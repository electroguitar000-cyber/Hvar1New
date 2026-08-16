package ru.dev.koramikon.ftime;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.dev.koramikon.ftime.command.FTimeCommand;
import ru.dev.koramikon.ftime.data.Storage;
import ru.dev.koramikon.ftime.timer.BreakTimer;
import ru.dev.koramikon.ftime.timer.MatchTimer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FTime extends JavaPlugin {

    private static FTime instance;
    private Storage storage;
    private MatchTimer matchTimer;
    private BreakTimer breakTimer;
    private int currentPeriod = 0;
    private boolean isBreakMode = false;
    private Location pos1;
    private Location pos2;
    private final Map<String, List<String>> history = new HashMap<>();

    // Настройки
    private int matchRadius = 100;
    private int boatRadius = 150;
    private int actionbarRadius = 100;
    private int maxHistory = 20;

    // Сообщения (кэш)
    private final Map<String, String> messages = new HashMap<>();
    private List<String> helpMessages = new ArrayList<>();

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        reloadConfig();

        storage = new Storage(this);

        var cmd = getCommand("ftime1");
        if (cmd != null) {
            var executor = new FTimeCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getLogger().info("FTime загружен!");
        getLogger().info("Радиус матча: " + matchRadius + " блоков");
        getLogger().info("Радиус удаления лодок: " + boatRadius + " блоков");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        loadConfig();
        loadMessages();
        getLogger().info("Конфигурация перезагружена!");
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();

        matchRadius = config.getInt("settings.radius.match", 100);
        boatRadius = config.getInt("settings.radius.boat", 150);
        actionbarRadius = config.getInt("settings.radius.actionbar", 100);
        maxHistory = config.getInt("settings.max-history", 20);
    }

    private void loadMessages() {
        messages.clear();
        FileConfiguration config = getConfig();

        // Загружаем все сообщения из секции messages, сохраняя путь без "messages."
        if (config.contains("messages")) {
            loadMessagesSection("", config.getConfigurationSection("messages"));
        }

        // Загружаем help как список
        helpMessages = config.getStringList("messages.help");
        if (helpMessages.isEmpty()) {
            helpMessages = List.of(
                    "&l✨ &lF&lT&lɪ&lᴍ&lᴇ &l2&l.&l0 ✨",
                    "&m─────────────────────",
                    "&e/ftime1 help &7— это сообщение",
                    "&e/ftime1 <1|2> <время>m &7— начать тайм"
            );
        }
    }

    private void loadMessagesSection(String prefix, org.bukkit.configuration.ConfigurationSection section) {
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (section.isConfigurationSection(key)) {
                loadMessagesSection(path, section.getConfigurationSection(key));
            } else {
                messages.put(path, section.getString(key));
            }
        }
    }

    public String getMessage(String path, String... replacements) {
        String message = messages.get(path);

        if (message == null) {
            getLogger().warning("Не найдено сообщение: " + path);
            return "&cMessage not found: " + path;
        }

        if (replacements.length > 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) {
                    message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
                }
            }
        }

        return message;
    }

    public List<String> getHelpMessages() {
        return helpMessages;
    }

    public void sendMessage(org.bukkit.command.CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;

        Component component = miniMessage.deserialize(convertToMiniMessage(message));
        sender.sendMessage(component);
    }

    public void broadcastRadius(Location center, double radius, String message) {
        if (message == null || message.isEmpty() || center == null) return;

        Component component = miniMessage.deserialize(convertToMiniMessage(message));

        double radiusSquared = radius * radius;
        for (org.bukkit.entity.Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= radiusSquared) {
                player.sendMessage(component);
            }
        }
    }

    public void broadcastActionBar(Location center, double radius, String message) {
        if (message == null || message.isEmpty() || center == null) return;

        Component component = miniMessage.deserialize(convertToMiniMessage(message));

        double radiusSquared = radius * radius;
        for (org.bukkit.entity.Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= radiusSquared) {
                player.sendActionBar(component);
            }
        }
    }

    private String convertToMiniMessage(String message) {
        String result = message;

        // 1. Конвертируем HEX цвета &#RRGGBB
        result = result.replaceAll("&#([A-Fa-f0-9]{6})", "<color:#$1>");

        // 2. Конвертируем цвета
        result = result.replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>");

        // 3. Конвертируем форматирование
        result = result.replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");

        return result;
    }

    @Override
    public void onDisable() {
        if (matchTimer != null) matchTimer.stop();
        if (breakTimer != null) breakTimer.stop();
        if (storage != null) storage.save();
        getLogger().info("FTime1 выгружен!");
    }

    public static FTime getInstance() {
        return instance;
    }

    public Storage getStorage() {
        return storage;
    }

    public MatchTimer getMatchTimer() {
        return matchTimer;
    }

    public void setMatchTimer(MatchTimer matchTimer) {
        this.matchTimer = matchTimer;
    }

    public BreakTimer getBreakTimer() {
        return breakTimer;
    }

    public void setBreakTimer(BreakTimer breakTimer) {
        this.breakTimer = breakTimer;
    }

    public int getCurrentPeriod() {
        return currentPeriod;
    }

    public void setCurrentPeriod(int currentPeriod) {
        this.currentPeriod = currentPeriod;
    }

    public boolean isBreakMode() {
        return isBreakMode;
    }

    public void setBreakMode(boolean breakMode) {
        isBreakMode = breakMode;
    }

    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public int getMatchRadius() {
        return matchRadius;
    }

    public int getBoatRadius() {
        return boatRadius;
    }

    public int getActionbarRadius() {
        return actionbarRadius;
    }

    public int getMaxHistory() {
        return maxHistory;
    }

    public void addHistory(String category, String entry) {
        history.computeIfAbsent(category, k -> new ArrayList<>()).add(0, entry);
        List<String> list = history.get(category);
        while (list.size() > maxHistory) list.remove(list.size() - 1);
    }

    public List<String> getHistory(String category) {
        return history.getOrDefault(category, new ArrayList<>());
    }
}