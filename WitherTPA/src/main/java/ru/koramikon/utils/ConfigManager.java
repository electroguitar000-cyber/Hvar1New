package ru.koramikon.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.koramikon.WitherTPA;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final WitherTPA plugin;
    private FileConfiguration messagesConfig;
    private FileConfiguration aliasesConfig;
    private final Map<String, List<String>> customAliases = new HashMap<>();

    public ConfigManager(WitherTPA plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();

        // Load messages.yml
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Load aliases.yml
        File aliasesFile = new File(plugin.getDataFolder(), "aliases.yml");
        aliasesConfig = YamlConfiguration.loadConfiguration(aliasesFile);

        // Load custom aliases
        if (aliasesConfig.contains("aliases")) {
            for (String key : aliasesConfig.getConfigurationSection("aliases").getKeys(false)) {
                List<String> aliases = aliasesConfig.getStringList("aliases." + key);
                customAliases.put(key, aliases);
            }
        }
    }

    public String getMessage(String path) {
        return messagesConfig.getString(path, "&cMessage not found: " + path);
    }

    // Добавь в ConfigManager.java
    public String getHelpMessage(String path) {
        return messagesConfig.getString("help-" + path, "&cMessage not found: " + path);
    }

    public List<String> getAliases(String command) {
        return customAliases.getOrDefault(command, List.of());
    }
}