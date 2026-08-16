package ru.dev.kisstymelusi.managers;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.dev.kisstymelusi.PassportPlugin;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {

    private final PassportPlugin plugin;
    private FileConfiguration messagesConfig;
    private String prefix;

    public MessageManager(PassportPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        prefix = messagesConfig.getString("prefix", "&6[Passport] &r");
    }

    public String getMessage(String key) {
        String msg = messagesConfig.getString("messages." + key);
        if (msg == null) return "Сообщение не найдено: " + key;
        return colorize(prefix + msg);
    }

    public String getRawMessage(String key) {
        return messagesConfig.getString("messages." + key);
    }

    public List<String> getRawMessageList(String key) {
        return messagesConfig.getStringList("messages." + key);
    }

    public static String colorize(String message) {
        if (message == null) return "";

        // HEX &#RRGGBB
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.COLOR_CHAR + "x"
                    + ChatColor.COLOR_CHAR + hex.charAt(0) + ChatColor.COLOR_CHAR + hex.charAt(1)
                    + ChatColor.COLOR_CHAR + hex.charAt(2) + ChatColor.COLOR_CHAR + hex.charAt(3)
                    + ChatColor.COLOR_CHAR + hex.charAt(4) + ChatColor.COLOR_CHAR + hex.charAt(5));
        }
        matcher.appendTail(buffer);

        // Обычные цвета
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}