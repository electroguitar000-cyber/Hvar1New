package org.anonymous.withercps.listeners;

import org.anonymous.withercps.cache.PlayerCache;
import org.anonymous.withercps.netty.PacketInjector;
import org.anonymous.withercps.sessions.click.ClickService;
import org.anonymous.withercps.sessions.watcher.WatcherService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public record PlayerListener(ClickService clickService, WatcherService watcherService, PacketInjector injector, PlayerCache players) implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        injector.inject(player);
        players.add(player.getUniqueId(), player.getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        injector.uninject(player);
        clickService.getSessions().remove(player.getUniqueId());
        watcherService.terminate(player.getUniqueId());
    }
}