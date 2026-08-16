package ru.dev.kisstymelusi.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.dev.kisstymelusi.PassportPlugin;
import ru.dev.kisstymelusi.models.PassportData;
import ru.dev.kisstymelusi.utils.DateUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PassportManager {

    private final PassportPlugin plugin;
    private final File passportsFile;
    private FileConfiguration passportsConfig;
    private final Map<UUID, PassportData> passportMap;

    public PassportManager(PassportPlugin plugin) {
        this.plugin = plugin;
        this.passportMap = new HashMap<>();
        this.passportsFile = new File(plugin.getDataFolder(), "passports.yml");
        if (!passportsFile.exists()) {
            plugin.saveResource("passports.yml", false);
        }
        reload();
    }

    public void reload() {
        passportsConfig = YamlConfiguration.loadConfiguration(passportsFile);
        passportMap.clear();
        if (passportsConfig.contains("passports")) {
            for (String key : passportsConfig.getConfigurationSection("passports").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                String playerName = passportsConfig.getString("passports." + key + ".playerName");
                String fullName = passportsConfig.getString("passports." + key + ".fullName");
                int age = passportsConfig.getInt("passports." + key + ".age");
                String gender = passportsConfig.getString("passports." + key + ".gender");
                String city = passportsConfig.getString("passports." + key + ".city");
                String married = passportsConfig.getString("passports." + key + ".married");
                long issueDate = passportsConfig.getLong("passports." + key + ".issueDate");
                int expiryDays = passportsConfig.getInt("passports." + key + ".expiryDays");

                PassportData data = new PassportData();
                data.setPlayerName(playerName);
                data.setFullName(fullName);
                data.setAge(age);
                data.setGender(gender);
                data.setCity(city);
                data.setMarried(married);
                data.setIssueDate(issueDate);
                data.setExpiryDays(expiryDays);
                passportMap.put(uuid, data);
            }
        }
    }

    public void savePassport(UUID uuid, PassportData data) {
        String path = "passports." + uuid.toString();
        passportsConfig.set(path + ".playerName", data.getPlayerName());
        passportsConfig.set(path + ".fullName", data.getFullName());
        passportsConfig.set(path + ".age", data.getAge());
        passportsConfig.set(path + ".gender", data.getGender());
        passportsConfig.set(path + ".city", data.getCity());
        passportsConfig.set(path + ".married", data.getMarried());
        passportsConfig.set(path + ".issueDate", data.getIssueDate());
        passportsConfig.set(path + ".expiryDays", data.getExpiryDays());

        try {
            passportsConfig.save(passportsFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Не удалось сохранить passports.yml: " + e.getMessage());
        }
        passportMap.put(uuid, data);
    }

    public PassportData getPassport(UUID uuid) {
        return passportMap.get(uuid);
    }

    public boolean hasPassport(UUID uuid) {
        return passportMap.containsKey(uuid);
    }

    public void removePassport(UUID uuid) {
        passportMap.remove(uuid);
        passportsConfig.set("passports." + uuid.toString(), null);
        try {
            passportsConfig.save(passportsFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Не удалось сохранить passports.yml: " + e.getMessage());
        }
    }

    public boolean isExpired(UUID uuid) {
        PassportData data = getPassport(uuid);
        if (data == null) return false;
        long expiryTime = data.getIssueDate() + data.getExpiryDays() * 24L * 60 * 60 * 1000;
        return System.currentTimeMillis() > expiryTime;
    }

    public void renewPassport(UUID uuid) {
        PassportData data = getPassport(uuid);
        if (data == null) return;
        data.setIssueDate(System.currentTimeMillis());
        savePassport(uuid, data);
    }

    public String getIssueDateString(UUID uuid, String timezone) {
        PassportData data = getPassport(uuid);
        if (data == null) return "";
        return DateUtils.formatTimestamp(data.getIssueDate(), timezone);
    }

    public String getExpiryDateString(UUID uuid, String timezone) {
        PassportData data = getPassport(uuid);
        if (data == null) return "";
        long expiryTime = data.getIssueDate() + data.getExpiryDays() * 24L * 60 * 60 * 1000;
        return DateUtils.formatTimestamp(expiryTime, timezone);
    }
}