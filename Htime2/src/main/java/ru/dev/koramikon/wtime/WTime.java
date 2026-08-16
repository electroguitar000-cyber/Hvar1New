package ru.dev.koramikon.wtime;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Boat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class WTime extends JavaPlugin {

    private static WTime instance;
    private LanguageManager languageManager;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public Location pos1, pos2;
    public MatchTimer matchTimer;
    public MatchTimer breakTimer;
    public int currentPeriod = 0;
    public final Map<UUID, PenaltyPlayer> penalties = new HashMap<>();
    private final List<String> penaltyHistory = new ArrayList<>();
    public boolean timersRunning = true;
    private final Map<Integer, Location> boatPoints = new HashMap<>();
    private BukkitRunnable currentResetTask = null;
    public boolean isBreakMode = false; // true = перерыв, false = матч
    public boolean bullitMode = false;
    public boolean autoPilot = false; // Автопилот включен?
    public int currentAutoPeriod = 0; // Текущий период на автопилоте (1,2,3)

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        languageManager = new LanguageManager(this);
        languageManager.loadMessages();

        loadBoatPoints();
        loadPenaltyHistory();

        registerCommands();
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        startBoatDetectionTask();
        startActionBarTask();

        getLogger().info("Плагин HTime2" + getDescription().getVersion() + " включен!");
    }

    @Override
    public void onDisable() {
        saveBoatPoints();
        savePenaltyHistory();
        if (matchTimer != null) matchTimer.stop();
        if (breakTimer != null) breakTimer.stop();
    }

    private void registerCommands() {
        var htimeCmd = new WTimeCommand(this);
        var boatCmd = new BoatCommand(this);
        var tabCompleter = new TabCompleter(this);

        Objects.requireNonNull(getCommand("htime2")).setExecutor(htimeCmd);
        Objects.requireNonNull(getCommand("htime2")).setTabCompleter(tabCompleter);
        Objects.requireNonNull(getCommand("boat2")).setExecutor(boatCmd);
        Objects.requireNonNull(getCommand("boat2")).setTabCompleter(tabCompleter);
    }

    private void startBoatDetectionTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pos1 == null) return;

                //если bullit режим - игнорируем лодки
                if (bullitMode) {
                    if (timersRunning != false) {
                        timersRunning = false;
                        broadcastRadius(getMessage("match.paused"));
                    }
                    return;
                }

                boolean hasBoat = pos1.getWorld().getEntitiesByClass(Boat.class).stream()
                        .anyMatch(b -> b.getLocation().distance(pos1) <= 100);

                // Для матча - влияет, для перерыва - нет
                if (matchTimer != null && matchTimer.isRunning()) {
                    if (timersRunning != hasBoat) {
                        timersRunning = hasBoat;
                        String key = hasBoat ? "match.resumed" : "match.paused";
                        broadcastRadius(getMessage(key));
                    }
                } else if (breakTimer != null && breakTimer.isRunning()) {

                } else {
                    timersRunning = true; // Если нет матча - паузы нет
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateActionBar();
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    public void updateActionBar() {
        if (pos2 == null) return;

        var config = getConfig();
        boolean showTimer = config.getBoolean("actionbar.show-timer", true);
        boolean showPause = config.getBoolean("actionbar.show-pause", true);
        String separator = config.getString("actionbar.separator", " &8| ");
        int maxPenalties = config.getInt("actionbar.max-penalties", 5);

        for (Player p : pos2.getWorld().getPlayers()) {
            // Проверяем расстояние до pos1 (если pos1 есть)
            if (pos1 != null) {
                if (p.getLocation().distance(pos1) > 100) continue;
            } else {
                if (p.getLocation().distance(pos2) > 100) continue;
            }

            List<String> parts = new ArrayList<>();

            if (showTimer) {
                String timerText = getTimerText();
                if (!timerText.isEmpty()) parts.add(timerText);
            }

            String penaltiesText = getPenaltiesText(p, maxPenalties);
            if (!penaltiesText.isEmpty()) parts.add(penaltiesText);

            // Показываем паузу ТОЛЬКО если есть активный матч
            if (showPause && !timersRunning && matchTimer != null && matchTimer.isRunning()) {
                parts.add("&#FF5555&l[ПАУЗА]");
            }

            if (!parts.isEmpty()) {
                String result = String.join(separator, parts);
                sendActionBar(p, result);
            }
        }
    }

    private String getTimerText() {
        var config = getConfig();

        if (breakTimer != null && breakTimer.isRunning() && !breakTimer.isFinished()) {
            String format = config.getString("actionbar.timer-format.break", "&#55AAFF&lПЕРЕРЫВ");
            return format + " " + breakTimer.getFormattedTime();
            // Для перерыва не добавляем паузу
        }

        if (matchTimer != null && matchTimer.isRunning() && !matchTimer.isFinished()) {
            String format;
            if (currentPeriod > 0) {
                format = config.getString("actionbar.timer-format.period", "&#FFAA55&lПериод {period}");
                format = format.replace("{period}", String.valueOf(currentPeriod));
            } else {
                format = config.getString("actionbar.timer-format.match", "&#AAFFAA&lМАТЧ");
            }
            return format + " " + matchTimer.getFormattedTime();
        }

        return "";
    }

    private String getPenaltiesText(Player p, int maxPenalties) {
        var config = getConfig();
        List<String> penaltyParts = new ArrayList<>();

        if (p.hasPermission("wtime.match")) {
            String format = config.getString("actionbar.penalty-format.admin", "&#FF5555{player} &f{reason} &d{time}");
            int count = 0;

            for (PenaltyPlayer pp : penalties.values()) {
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
            PenaltyPlayer own = penalties.get(p.getUniqueId());
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

    // === ОСНОВНОЙ МЕТОД ДЛЯ HEX ===
    public Component parseHex(String message) {
        if (message == null) return Component.empty();

        // Конвертируем &#RRGGBB в &x&R&R&G&G&B&B
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

        // Конвертируем в Component
        return LegacyComponentSerializer.legacyAmpersand().deserialize(buffer.toString());
    }

    public void sendMessage(Player player, String message) {
        if (player == null || message == null) return;
        player.sendMessage(parseHex(message));
    }

    public void sendActionBar(Player player, String message) {
        if (player == null || message == null) return;
        player.sendActionBar(parseHex(message));
    }

    public void broadcastRadius(String message) {
        if (pos2 == null || message == null) return;
        Component component = parseHex(message);
        for (Player p : pos2.getWorld().getPlayers()) {
            if (p.getLocation().distance(pos2) <= 100) {
                p.sendMessage(component);
            }
        }
    }

    public String getMessage(String key) {
        return languageManager.getMessage(key);
    }

    public String getMessage(String key, String... replacements) {
        return languageManager.getMessage(key, replacements);
    }

    public List<String> getMessageList(String key) {
        return languageManager.getMessageList(key);
    }

    public void matchNaturallyEnded() {
        if (matchTimer != null && matchTimer.isRunning()) {
            // Это матч закончился
            getLogger().info("Матч закончился!");
            broadcastRadius(getMessage("match.natural-end"));
            broadcastRadius(getMessage("match.natural-end"));
            broadcastRadius(getMessage("match.natural-end"));

            // Автоматически очищаем лодки
            if (pos1 != null) {
                int removed = 0;
                for (var boat : pos1.getWorld().getEntitiesByClass(org.bukkit.entity.Boat.class)) {
                    if (boat.getLocation().distance(pos1) <= 100) {
                        boat.remove();
                        removed++;
                    }
                }
                broadcastRadius(getMessage("boat.clear-complete",
                        "{count}", String.valueOf(removed)));
            }

            // АВТОПИЛОТ: после периода запускаем перерыв (кроме 3 периода)
            if (autoPilot && currentPeriod < 3) {
                // БЕЗ СООБЩЕНИЙ
                isBreakMode = true;
                breakTimer = new MatchTimer(120, true);
                currentPeriod = 0;
                breakTimer.start();
            } else if (autoPilot && currentPeriod == 3) {
                // После 3 периода - матч окончен (БЕЗ СООБЩЕНИЙ)
                autoPilot = false;
                currentAutoPeriod = 0;
            }

            matchTimer = null;

        } else if (breakTimer != null && breakTimer.isRunning()) {
            // Это перерыв закончился
            getLogger().info("Перерыв закончился!");
            broadcastRadius(getMessage("break.ended"));
            broadcastRadius(getMessage("break.ended"));
            broadcastRadius(getMessage("break.ended"));

            // АВТОПИЛОТ: после перерыва запускаем следующий период (БЕЗ СООБЩЕНИЙ)
            if (autoPilot) {
                currentAutoPeriod = currentPeriod + 1;

                // Включаем ворота только для 1 периода (БЕЗ СООБЩЕНИЙ)
                if (currentAutoPeriod == 1) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hvar1 on");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hvar2 on");
                }

                isBreakMode = false;
                matchTimer = new MatchTimer(15 * 60, false);
                currentPeriod = currentAutoPeriod;
                matchTimer.start();
            }

            breakTimer = null;
        }
    }

    private void startAutoPeriod() {

        // Включаем ворота только для 1 периода
        if (currentAutoPeriod == 1) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hvar1 on");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hvar2 on");
        }

        isBreakMode = false;
        matchTimer = new MatchTimer(15 * 60, false);
        currentPeriod = currentAutoPeriod;
        matchTimer.start();
    }

    public void addPenaltyToHistory(String player, String reason, int minutes) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String entry = getMessage("penalty.history.entry")
                .replace("{player}", player)
                .replace("{reason}", reason)
                .replace("{time}", minutes + "m")
                .replace("{date}", date);

        penaltyHistory.add(0, entry);
        if (penaltyHistory.size() > 100) penaltyHistory.remove(penaltyHistory.size() - 1);
        savePenaltyHistory();
    }

    public List<String> getLast10History() {
        return penaltyHistory.size() <= 10 ? new ArrayList<>(penaltyHistory) : new ArrayList<>(penaltyHistory.subList(0, 10));
    }

    private void loadPenaltyHistory() {
        if (getConfig().contains("penalty-history")) {
            penaltyHistory.addAll(getConfig().getStringList("penalty-history"));
        }
    }

    private void savePenaltyHistory() {
        getConfig().set("penalty-history", penaltyHistory);
        saveConfig();
    }

    private void loadBoatPoints() {
        boatPoints.clear();
        for (int i = 1; i <= 9; i++) {
            if (getConfig().contains("boatpoints." + i)) {
                boatPoints.put(i, getConfig().getLocation("boatpoints." + i));
            }
        }
    }

    public void saveBoatPoints() {
        for (var e : boatPoints.entrySet()) {
            getConfig().set("boatpoints." + e.getKey(), e.getValue());
        }
        saveConfig();
    }

    public static WTime get() { return instance; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public Map<Integer, Location> getBoatPoints() { return boatPoints; }
    public BukkitRunnable getCurrentResetTask() { return currentResetTask; }
    public void setCurrentResetTask(BukkitRunnable task) { this.currentResetTask = task; }
}