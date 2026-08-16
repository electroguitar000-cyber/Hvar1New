package ru.dev.koramikon.wtime;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.dev.koramikon.wtime.commands.BoatCommand;
import ru.dev.koramikon.wtime.commands.TabCompleter;
import ru.dev.koramikon.wtime.commands.WTimeCommand;
import ru.dev.koramikon.wtime.data.MatchTimer;
import ru.dev.koramikon.wtime.data.PenaltyPlayer;
import ru.dev.koramikon.wtime.listeners.PlayerListener;
import ru.dev.koramikon.wtime.managers.LanguageManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WTime extends JavaPlugin {

    private static WTime instance;
    private LanguageManager languageManager;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static class TimerInstance {
        public Location pos1, pos2;
        public MatchTimer matchTimer;
        public MatchTimer breakTimer;
        public int currentPeriod = 0;
        public boolean timersRunning = true;
        public boolean autoMatch = false;
        public int nextPeriod = 1;
        public BossBar breakBossBar = null;
        public boolean bullitMode = false;
        public boolean isBreakMode = false;
        public final Map<UUID, PenaltyPlayer> penalties = new HashMap<>();
        private final List<String> penaltyHistory = new ArrayList<>();
        public final Map<Integer, Location> boatPoints = new HashMap<>();
        public BukkitRunnable currentResetTask = null;

        public List<String> getPenaltyHistory() { return penaltyHistory; }
    }

    public final TimerInstance[] timers = new TimerInstance[51];
    public int autoPeriodDuration = 15;
    public int autoBreakDuration = 120;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        for (int i = 1; i <= 50; i++) {
            timers[i] = new TimerInstance();
        }

        loadAutoMatchConfig();

        languageManager = new LanguageManager(this);
        languageManager.loadMessages();

        registerCommands();
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        startBoatDetectionTask();
        startActionBarTask();

        getLogger().info("Плагин Htime1 v" + getDescription().getVersion() + " включен с поддержкой 50 арен!");
    }

    @Override
    public void onDisable() {
        for (int i = 1; i <= 50; i++) {
            TimerInstance t = timers[i];
            if (t.matchTimer != null) t.matchTimer.stop();
            if (t.breakTimer != null) t.breakTimer.stop();
            if (t.breakBossBar != null) {
                t.breakBossBar.removeAll();
                t.breakBossBar = null;
            }
        }
        saveAllBoatPoints();
    }

    public void loadAutoMatchConfig() {
        autoPeriodDuration = getConfig().getInt("auto-match.period-duration", 15);
        autoBreakDuration = getConfig().getInt("auto-match.break-duration", 120);
    }

    private void registerCommands() {
        var wtimeCmd = new WTimeCommand(this);
        var boatCmd = new BoatCommand(this);
        var tabCompleter = new TabCompleter();

        // Глобальная команда /htime
        var htimeCmd = getCommand("htime");
        if (htimeCmd != null) {
            htimeCmd.setExecutor(wtimeCmd);
            htimeCmd.setTabCompleter(tabCompleter);
        }

        for (int i = 1; i <= 50; i++) {
            String cmdName = "htime" + i;
            var cmd = getCommand(cmdName);
            if (cmd != null) {
                cmd.setExecutor(wtimeCmd);
                cmd.setTabCompleter(tabCompleter);
            }
            String boatName = "boat" + i;
            var boat = getCommand(boatName);
            if (boat != null) {
                boat.setExecutor(boatCmd);
                boat.setTabCompleter(tabCompleter);
            }
        }
    }

    private void startBoatDetectionTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 1; i <= 50; i++) {
                    TimerInstance t = timers[i];
                    if (t.pos1 == null) continue;

                    if (t.bullitMode) {
                        if (t.timersRunning) {
                            t.timersRunning = false;
                            broadcastRadius(i, getMessage("match.paused", i));
                        }
                        continue;
                    }

                    boolean hasBoat = t.pos1.getWorld().getEntitiesByClass(Boat.class).stream()
                            .anyMatch(b -> b.getLocation().distance(t.pos1) <= 100);

                    if (t.matchTimer != null && t.matchTimer.isRunning()) {
                        if (t.timersRunning != hasBoat) {
                            t.timersRunning = hasBoat;
                            String key = hasBoat ? "match.resumed" : "match.paused";
                            broadcastRadius(i, getMessage(key, i));
                        }
                    } else {
                        t.timersRunning = true;
                    }
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 1; i <= 50; i++) {
                    updateActionBar(i);
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    public void updateActionBar(int arenaId) {
        TimerInstance t = timers[arenaId];
        if (t.pos2 == null) return;

        var config = getConfig();
        String separator = config.getString("actionbar.separator", " &8| ");
        int maxPenalties = config.getInt("actionbar.max-penalties", 4);

        for (Player p : t.pos2.getWorld().getPlayers()) {
            if (t.pos1 != null) {
                if (p.getLocation().distance(t.pos1) > 100) continue;
            } else {
                if (p.getLocation().distance(t.pos2) > 100) continue;
            }

            List<String> parts = new ArrayList<>();

            if (t.matchTimer != null && !t.matchTimer.isFinished() && t.currentPeriod > 0) {
                String format = "&#FFAA55&lПериод {period}";
                format = format.replace("{period}", String.valueOf(t.currentPeriod));
                parts.add(format + " " + t.matchTimer.getFormattedTime());
            }

            String penaltiesText = getPenaltiesText(arenaId, p, maxPenalties);
            if (!penaltiesText.isEmpty()) parts.add(penaltiesText);

            if (!parts.isEmpty()) {
                String result = String.join(separator, parts);
                sendActionBar(p, result);
            }
        }
    }

    private String getPenaltiesText(int arenaId, Player p, int maxPenalties) {
        TimerInstance t = timers[arenaId];
        var config = getConfig();
        List<String> penaltyParts = new ArrayList<>();

        if (p.hasPermission("htime" + arenaId + ".match")) {
            String format = config.getString("actionbar.penalty-format.admin", "&#FF5555{player} &f{reason} &d{time}");
            int count = 0;

            for (PenaltyPlayer pp : t.penalties.values()) {
                if (!pp.isActive()) continue;
                if (count >= maxPenalties) {
                    penaltyParts.add("§7...");
                    break;
                }

                String text = format
                        .replace("{player}", pp.getName())
                        .replace("{reason}", pp.reason)
                        .replace("{time}", pp.getFormattedRemaining());
                penaltyParts.add(text);
                count++;
            }
        } else {
            PenaltyPlayer own = t.penalties.get(p.getUniqueId());
            if (own != null && own.isActive()) {
                String format = config.getString("actionbar.penalty-format.player", "&f{reason} &d{time}");
                String text = format
                        .replace("{reason}", own.reason)
                        .replace("{time}", own.getFormattedRemaining());
                penaltyParts.add(text);
            }
        }

        return String.join(" ", penaltyParts);
    }

    // === ЦВЕТА ===
    public Component parseLegacyHex(String message) {
        if (message == null) return Component.empty();
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (char c : hex.toCharArray()) {
                replacement.append('&').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(buffer.toString());
    }

    public String convertHexToLegacy(String message) {
        if (message == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    // === ОТПРАВКА СООБЩЕНИЙ ===
    public void sendMessage(Player player, String message) {
        if (player == null || message == null) return;
        player.sendMessage(parseLegacyHex(message));
    }

    public void sendActionBar(Player player, String message) {
        if (player == null || message == null) return;
        player.sendActionBar(parseLegacyHex(message));
    }

    public void broadcastRadius(int arenaId, String message) {
        TimerInstance t = timers[arenaId];
        if (t.pos2 == null || message == null) return;
        Component component = parseLegacyHex(message);
        for (Player p : t.pos2.getWorld().getPlayers()) {
            if (p.getLocation().distance(t.pos2) <= 100) {
                p.sendMessage(component);
            }
        }
    }

    // === ЯЗЫКОВОЙ МЕНЕДЖЕР ===
    public String getMessage(String key) {
        return languageManager.getMessage(key, 1);
    }

    public String getMessage(String key, int arenaId) {
        return languageManager.getMessage(key, arenaId);
    }

    public String getMessage(String key, int arenaId, String... replacements) {
        return languageManager.getMessage(key, arenaId, replacements);
    }

    public List<String> getMessageList(String key) {
        return languageManager.getMessageList(key);
    }

    // === ИСТОРИЯ ШТРАФОВ ===
    public void addPenaltyToHistory(int arenaId, String player, String reason, int minutes) {
        TimerInstance t = timers[arenaId];
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String entry = getMessage("penalty.history.entry", arenaId)
                .replace("{player}", player)
                .replace("{reason}", reason)
                .replace("{time}", minutes + "m")
                .replace("{date}", date);
        t.getPenaltyHistory().add(0, entry);
        if (t.getPenaltyHistory().size() > 100) t.getPenaltyHistory().remove(t.getPenaltyHistory().size() - 1);
        savePenaltyHistory(arenaId);
    }

    public List<String> getLast10History(int arenaId) {
        TimerInstance t = timers[arenaId];
        return t.getPenaltyHistory().size() <= 10 ? new ArrayList<>(t.getPenaltyHistory()) : new ArrayList<>(t.getPenaltyHistory().subList(0, 10));
    }

    private void savePenaltyHistory(int arenaId) {
        TimerInstance t = timers[arenaId];
        getConfig().set("arena" + arenaId + ".penalty-history", t.getPenaltyHistory());
        saveConfig();
    }

    // === ТОЧКИ ЛОДОК ===
    public void saveBoatPoints(int arenaId) {
        TimerInstance t = timers[arenaId];
        for (var e : t.boatPoints.entrySet()) {
            getConfig().set("arena" + arenaId + ".boatpoints." + e.getKey(), e.getValue());
        }
        saveConfig();
    }

    public void saveAllBoatPoints() {
        for (int i = 1; i <= 50; i++) {
            saveBoatPoints(i);
        }
    }

    // === ГЕТТЕРЫ ===
    public static WTime get() { return instance; }
    public LanguageManager getLanguageManager() { return languageManager; }
}