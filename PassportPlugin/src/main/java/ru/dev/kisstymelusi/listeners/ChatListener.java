package ru.dev.kisstymelusi.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ru.dev.kisstymelusi.PassportPlugin;

public class ChatListener implements Listener {

    private final PassportPlugin plugin;

    public ChatListener(PassportPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getDialogManager().isInDialog(player)) {
            event.setCancelled(true);
            String message = event.getMessage();
            plugin.getDialogManager().handleAnswer(player, message);
        }
    }
}