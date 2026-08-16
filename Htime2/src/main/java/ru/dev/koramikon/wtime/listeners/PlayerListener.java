package ru.dev.koramikon.wtime.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.dev.koramikon.wtime.WTime;
import ru.dev.koramikon.wtime.data.PenaltyPlayer;

public class PlayerListener implements Listener {

    private final WTime plugin;

    public PlayerListener(WTime plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PenaltyPlayer pp = plugin.penalties.get(p.getUniqueId());

        if (pp != null && pp.isActive() && plugin.pos2 != null) {
            Location tp = plugin.pos2.clone().add(0.5, 1, 0.5);
            Bukkit.getScheduler().runTaskLater(plugin, () -> p.teleport(tp), 10L);

            plugin.sendMessage(p, "§c§lТы всё ещё на штрафной за: §f" +
                    pp.reason + " §e" + pp.getFormattedRemaining());
        }
    }
}