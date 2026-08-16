package ru.dev.kisstymelusi.utils;

import org.bukkit.entity.Player;
import ru.dev.kisstymelusi.PassportPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PassportTransferManager {

    private final PassportPlugin plugin;
    private final Map<UUID, UUID> requests; // receiver -> giver

    public PassportTransferManager(PassportPlugin plugin) {
        this.plugin = plugin;
        this.requests = new HashMap<>();
    }

    public void createRequest(Player giver, Player receiver) {
        requests.put(receiver.getUniqueId(), giver.getUniqueId());
    }

    public boolean acceptRequest(Player receiver, Player giver) {
        UUID giverUUID = requests.get(receiver.getUniqueId());
        if (giverUUID == null || !giverUUID.equals(giver.getUniqueId())) {
            return false;
        }
        requests.remove(receiver.getUniqueId());
        return true;
    }

    public boolean denyRequest(Player receiver, Player giver) {
        UUID giverUUID = requests.get(receiver.getUniqueId());
        if (giverUUID == null || !giverUUID.equals(giver.getUniqueId())) {
            return false;
        }
        requests.remove(receiver.getUniqueId());
        return true;
    }

    public void clearAll() {
        requests.clear();
    }
}