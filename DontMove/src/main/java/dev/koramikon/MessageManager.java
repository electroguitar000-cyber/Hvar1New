package dev.koramikon.dontmove;

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
    private final Map<String, String> messages = new HashMap<>();

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        List<String> helpLines = config.getStringList("help");
        if (helpLines.isEmpty()) {
            helpLines.add("&b/dontmove help &7— показать это меню");
            helpLines.add("");
            helpLines.add("&e— Регионы —");
            helpLines.add("&b/dontmove <1-4> pos1 &7— установить первую точку региона");
            helpLines.add("&b/dontmove <1-4> pos2 &7— установить вторую точку региона");
            helpLines.add("");
            helpLines.add("&e— Управление —");
            helpLines.add("&b/dontmove on &7— включить блокировку лодок");
            helpLines.add("&b/dontmove off &7— выключить блокировку лодок");
            helpLines.add("");
            helpLines.add("&e— Дополнительно —");
            helpLines.add("&b/dontmove help &7— показать эту справку");
        }
        messages.put("help", String.join("\n", helpLines));

        messages.put("pos-set", config.getString("pos-set", "&aRegion %region%: &e%pos% &aset to &b%location%"));
        messages.put("mode-on", config.getString("mode-on", "&aDontMove mode &2ENABLED"));
        messages.put("mode-off", config.getString("mode-off", "&cDontMove mode &4DISABLED"));
        messages.put("invalid-number", config.getString("invalid-number", "&cInvalid region number! Use 1-4."));
        messages.put("invalid-arg", config.getString("invalid-arg", "&cInvalid argument! Use help, on/off, or <1-4> pos1/pos2."));
        messages.put("no-region-set", config.getString("no-region-set", "&cRegion %region% is not fully set."));
        messages.put("no-permission", config.getString("no-permission", "&cYou don't have permission."));
    }

    private String translateHex(String message) {
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(message);
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
        return buffer.toString();
    }

    public String getMessage(String key) {
        String msg = messages.getOrDefault(key, "&cMissing message: " + key);
        msg = translateHex(msg);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            msg = msg.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return msg;
    }
}