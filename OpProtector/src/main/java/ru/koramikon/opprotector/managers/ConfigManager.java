package ru.koramikon.opprotector.managers;

import org.bukkit.configuration.file.FileConfiguration;
import ru.koramikon.opprotector.OpProtector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConfigManager {

    private final OpProtector plugin;
    private List<String> allowedPlayers;

    public ConfigManager(OpProtector plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        allowedPlayers = config.getStringList("allowed-players");
        if (allowedPlayers == null) {
            allowedPlayers = new ArrayList<>();
        }
    }

    public List<String> getAllowedPlayers() {
        return new ArrayList<>(allowedPlayers);
    }

    public boolean isPlayerAllowed(String playerName) {
        return allowedPlayers.contains(playerName.toLowerCase());
    }

    public boolean addPlayer(String playerName) {
        String lowerName = playerName.toLowerCase();
        if (!allowedPlayers.contains(lowerName)) {
            allowedPlayers.add(lowerName);
            saveAllowedPlayers();
            return true;
        }
        return false;
    }

    public boolean removePlayer(String playerName) {
        String lowerName = playerName.toLowerCase();
        boolean removed = allowedPlayers.remove(lowerName);
        if (removed) {
            saveAllowedPlayers();
        }
        return removed;
    }

    private void saveAllowedPlayers() {
        plugin.getConfig().set("allowed-players", allowedPlayers);
        plugin.saveConfig();
    }

    public String getMessage(String path) {
        return plugin.getConfig().getString("messages." + path, "&cMessage not found: " + path);
    }

    public List<String> getMessageList(String path) {
        return plugin.getConfig().getStringList("messages." + path);
    }
}