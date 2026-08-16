package ru.dev.kisstymelusi.managers;

import org.bukkit.configuration.file.FileConfiguration;
import ru.dev.kisstymelusi.PassportPlugin;

public class ConfigManager {

    private final PassportPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(PassportPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public int getExpiryDays() {
        return config.getInt("passport.expiry-days", 30);
    }

    public String getAgeLimit() {
        return config.getString("passport.age-limit", "1-99");
    }

    public boolean isAllowEnglishName() {
        return config.getBoolean("passport.allow-english-name", false);
    }

    public String getDefaultCity() {
        return config.getString("passport.default-city", "Пин-сити");
    }

    public String getGenderOptions() {
        return config.getString("passport.gender-options", "Мужской Женский");
    }

    public String getMarriedOptions() {
        return config.getString("passport.married-options", "Да Нет");
    }

    public int getNotifyInterval() {
        return config.getInt("passport.notify-interval", 360);
    }

    public String getTimezone() {
        return config.getString("timezone", "Europe/Moscow");
    }
}