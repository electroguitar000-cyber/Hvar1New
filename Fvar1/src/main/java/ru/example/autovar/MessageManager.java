package ru.example.autovar;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {

    private final JavaPlugin plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        loadConfig();
    }

    public void loadConfig() {
        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reload() {
        loadConfig();
    }

    public String getMessage(String key) {
        if (messagesConfig == null) {
            loadConfig();
        }

        String msg = messagesConfig.getString(key);

        if (msg == null) {
            plugin.getLogger().warning("Message not found: " + key);
            return "§cMessage not found: " + key;
        }

        return colorize(msg);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public List<String> getMessageList(String key) {
        if (messagesConfig == null) {
            loadConfig();
        }
        return messagesConfig.getStringList(key);
    }

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) return "";

        String result = message;

        Matcher matcher = HEX_PATTERN.matcher(result);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder();
            replacement.append(ChatColor.COLOR_CHAR).append('x');
            for (char c : hex.toCharArray()) {
                replacement.append(ChatColor.COLOR_CHAR).append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        result = buffer.toString();

        result = ChatColor.translateAlternateColorCodes('&', result);

        return result;
    }
}