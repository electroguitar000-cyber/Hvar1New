package ru.koramikon.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.koramikon.WitherTPA;
import ru.koramikon.managers.TeleportManager;

public class TeleportListener implements Listener {

    private final WitherTPA plugin;

    public TeleportListener(WitherTPA plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Remove requests where player is requester
        TeleportManager.TeleportRequest request = plugin.getTeleportManager().getRequest(player.getUniqueId());
        if (request != null) {
            // Notify target that requester left
            Player target = request.getTarget();
            if (target != null && target.isOnline()) {
                plugin.getMessageUtils().sendMessage(target, "tpa-request-expired");
            }
            plugin.getTeleportManager().removeRequest(player.getUniqueId());
        }

        // Remove requests where player is target
        request = plugin.getTeleportManager().getRequestByTarget(player.getUniqueId());
        if (request != null) {
            // Notify requester that target left
            Player requester = request.getRequester();
            if (requester != null && requester.isOnline()) {
                plugin.getMessageUtils().sendMessage(requester, "tpa-request-expired");
            }
            plugin.getTeleportManager().removeRequest(request.getRequesterUuid());
        }
    }
}