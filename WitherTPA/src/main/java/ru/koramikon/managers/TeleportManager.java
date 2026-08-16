package ru.koramikon.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.koramikon.WitherTPA;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {

    private final WitherTPA plugin;
    private final Map<UUID, TeleportRequest> requests = new HashMap<>();

    public TeleportManager(WitherTPA plugin) {
        this.plugin = plugin;
    }

    public void createRequest(Player requester, Player target, boolean isHere) {
        // Remove old request if exists
        requests.remove(requester.getUniqueId());

        TeleportRequest request = new TeleportRequest(requester, target, isHere);
        requests.put(requester.getUniqueId(), request);

        // Schedule timeout
        int timeout = plugin.getConfig().getInt("settings.request-timeout", 30);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (requests.containsKey(requester.getUniqueId()) &&
                    requests.get(requester.getUniqueId()).equals(request)) {
                requests.remove(requester.getUniqueId());

                if (requester.isOnline()) {
                    plugin.getMessageUtils().sendMessage(requester, "tpa-request-expired");
                }

                // Also notify target if online
                if (target.isOnline()) {
                    plugin.getMessageUtils().sendMessage(target, "tpa-request-expired");
                }
            }
        }, timeout * 20L);
    }

    public TeleportRequest getRequest(UUID requesterUuid) {
        return requests.get(requesterUuid);
    }

    public TeleportRequest getRequestByTarget(UUID targetUuid) {
        return requests.values().stream()
                .filter(req -> req.getTargetUuid().equals(targetUuid))
                .findFirst()
                .orElse(null);
    }

    public void removeRequest(UUID requesterUuid) {
        requests.remove(requesterUuid);
    }

    public void acceptRequest(UUID targetUuid) {
        TeleportRequest request = getRequestByTarget(targetUuid);
        if (request != null) {
            Player requester = Bukkit.getPlayer(request.getRequesterUuid());
            Player target = Bukkit.getPlayer(targetUuid);

            if (requester != null && target != null) {
                // Check if target hasn't disabled teleports
                if (request.isHere()) {
                    // tpahere - target teleports to requester
                    if (!plugin.getPlayerSettingsManager().canTpTo(request.getRequesterUuid())) {
                        plugin.getMessageUtils().sendMessage(requester, "tp-target-off", "player", target.getName());
                        return;
                    }
                    target.teleport(requester);

                    // Отправка оповещения всем у кого включены tpanotice
                    plugin.getMessageUtils().sendTpaNotice(target, requester);

                } else {
                    // tpa - requester teleports to target
                    if (!plugin.getPlayerSettingsManager().canTpTo(targetUuid)) {
                        plugin.getMessageUtils().sendMessage(requester, "tp-target-off", "player", target.getName());
                        return;
                    }
                    requester.teleport(target);

                    // Отправка оповещения всем у кого включены tpanotice
                    plugin.getMessageUtils().sendTpaNotice(requester, target);
                }

                plugin.getMessageUtils().sendMessage(requester, "request-accepted-target", "player", target.getName());
                plugin.getMessageUtils().sendMessage(target, "request-accepted");

                // Apply cooldown
                plugin.getCooldownManager().setCooldown(requester.getUniqueId());
            }

            removeRequest(request.getRequesterUuid());
        }
    }

    public void denyRequest(UUID targetUuid) {
        TeleportRequest request = getRequestByTarget(targetUuid);
        if (request != null) {
            Player requester = Bukkit.getPlayer(request.getRequesterUuid());
            Player target = Bukkit.getPlayer(targetUuid);

            if (requester != null && target != null) {
                plugin.getMessageUtils().sendMessage(requester, "request-denied-target", "player", target.getName());
                plugin.getMessageUtils().sendMessage(target, "request-denied");
            }

            removeRequest(request.getRequesterUuid());
        }
    }

    public static class TeleportRequest {
        private final UUID requesterUuid;
        private final String requesterName;
        private final UUID targetUuid;
        private final String targetName;
        private final boolean isHere;
        private final long timestamp;

        public TeleportRequest(Player requester, Player target, boolean isHere) {
            this.requesterUuid = requester.getUniqueId();
            this.requesterName = requester.getName();
            this.targetUuid = target.getUniqueId();
            this.targetName = target.getName();
            this.isHere = isHere;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getRequesterUuid() {
            return requesterUuid;
        }

        public String getRequesterName() {
            return requesterName;
        }

        public Player getRequester() {
            return Bukkit.getPlayer(requesterUuid);
        }

        public UUID getTargetUuid() {
            return targetUuid;
        }

        public String getTargetName() {
            return targetName;
        }

        public Player getTarget() {
            return Bukkit.getPlayer(targetUuid);
        }

        public boolean isHere() {
            return isHere;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}