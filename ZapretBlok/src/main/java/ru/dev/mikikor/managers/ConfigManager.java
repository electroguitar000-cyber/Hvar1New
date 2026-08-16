package ru.dev.mikikor.managers;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.dev.mikikor.utils.ColorUtils;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration placeConfig;
    private FileConfiguration breakConfig;
    private FileConfiguration messagesConfig;

    private File placeFile;
    private File breakFile;
    private File messagesFile;

    private Set<Material> blockedPlaceBlocks;
    private Set<Material> blockedBreakBlocks;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.blockedPlaceBlocks = new HashSet<>();
        this.blockedBreakBlocks = new HashSet<>();

        createConfigs();
        loadConfigs();
        loadBlockLists();
    }

    private void createConfigs() {
        // Конфиг для блоков, которые нельзя ставить
        placeFile = new File(plugin.getDataFolder(), "blocked_place.yml");
        if (!placeFile.exists()) {
            plugin.saveResource("blocked_place.yml", false);
        }
        placeConfig = YamlConfiguration.loadConfiguration(placeFile);

        // Конфиг для блоков, которые нельзя ломать
        breakFile = new File(plugin.getDataFolder(), "blocked_break.yml");
        if (!breakFile.exists()) {
            plugin.saveResource("blocked_break.yml", false);
        }
        breakConfig = YamlConfiguration.loadConfiguration(breakFile);

        // Конфиг для текстов
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void loadConfigs() {
        try {
            placeConfig.load(placeFile);
            breakConfig.load(breakFile);
            messagesConfig.load(messagesFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Не удалось загрузить конфиги!");
            e.printStackTrace();
        }
    }

    private void loadBlockLists() {
        // Загрузка блоков, которые нельзя ставить
        List<String> placeBlocks = placeConfig.getStringList("blocked_blocks");
        blockedPlaceBlocks.clear();
        for (String blockName : placeBlocks) {
            Material material = Material.getMaterial(blockName.toUpperCase());
            if (material != null) {
                blockedPlaceBlocks.add(material);
            } else {
                plugin.getLogger().warning("Неизвестный блок в blocked_place.yml: " + blockName);
            }
        }

        // Загрузка блоков, которые нельзя ломать
        List<String> breakBlocks = breakConfig.getStringList("blocked_blocks");
        blockedBreakBlocks.clear();
        for (String blockName : breakBlocks) {
            Material material = Material.getMaterial(blockName.toUpperCase());
            if (material != null) {
                blockedBreakBlocks.add(material);
            } else {
                plugin.getLogger().warning("Неизвестный блок в blocked_break.yml: " + blockName);
            }
        }
    }

    public void reload() {
        createConfigs();
        loadConfigs();
        loadBlockLists();
    }

    public boolean isBlockedToPlace(Material material) {
        return blockedPlaceBlocks.contains(material);
    }

    public boolean isBlockedToBreak(Material material) {
        return blockedBreakBlocks.contains(material);
    }

    public String getMessage(String path) {
        String message = messagesConfig.getString(path);
        if (message == null) {
            return "§cСообщение не найдено: " + path;
        }
        return ColorUtils.colorize(message);
    }

    public Set<Material> getBlockedPlaceBlocks() {
        return new HashSet<>(blockedPlaceBlocks);
    }

    public Set<Material> getBlockedBreakBlocks() {
        return new HashSet<>(blockedBreakBlocks);
    }
}