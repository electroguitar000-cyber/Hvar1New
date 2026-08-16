package ru.dev.kisstymelusi.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import ru.dev.kisstymelusi.PassportPlugin;
import ru.dev.kisstymelusi.utils.BookBuilder;

public class PassportDropListener implements Listener {

    private final PassportPlugin plugin;

    public PassportDropListener(PassportPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (BookBuilder.isPassportBook(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getMessageManager().getMessage("cannot-drop-passport"));
        }
    }
}