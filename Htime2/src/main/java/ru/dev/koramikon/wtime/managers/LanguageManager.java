package ru.dev.koramikon.wtime.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.dev.koramikon.wtime.WTime;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageManager {

    private final WTime plugin;
    private FileConfiguration messages;
    private final Map<String, String> cache = new HashMap<>();

    public LanguageManager(WTime plugin) {
        this.plugin = plugin;
    }

    public void loadMessages() {
        cache.clear();

        String locale = plugin.getConfig().getString("locale", "ru");
        File langFile = new File(plugin.getDataFolder(), "messages_" + locale + ".yml");

        if (!langFile.exists()) {
            plugin.saveResource("messages_" + locale + ".yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(langFile);

        for (String key : messages.getKeys(true)) {
            if (messages.isString(key)) {
                cache.put(key, messages.getString(key));
            }
        }

        plugin.getLogger().info("Загружена локаль: " + locale);
    }

    public String getMessage(String key) {
        String msg = cache.get(key);
        if (msg == null) {
            msg = messages.getString(key);
            if (msg == null) {
                plugin.getLogger().warning("Не найдено сообщение: " + key);
                return "§cMissing message: " + key;
            }
            cache.put(key, msg);
        }

        if (!key.startsWith("help.") && !msg.startsWith("&#E64C4C&l[WTime]")) {
            String prefix = cache.getOrDefault("prefix", "&#E64C4C&l[WTime] &r");
            msg = prefix + msg;
        }

        return msg;
    }

    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                msg = msg.replace(replacements[i], replacements[i + 1]);
            }
        }

        return msg;
    }

    public List<String> getMessageList(String key) {
        List<String> list = messages.getStringList(key);
        if (list.isEmpty() && messages.contains(key)) {
            String single = messages.getString(key);
            if (single != null) {
                list = List.of(single);
            }
        }
        return list;
    }
}