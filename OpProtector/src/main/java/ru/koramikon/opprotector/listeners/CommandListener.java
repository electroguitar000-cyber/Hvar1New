package ru.koramikon.opprotector.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ru.koramikon.opprotector.OpProtector;
import ru.koramikon.opprotector.managers.ConfigManager;
import ru.koramikon.opprotector.utils.MessageUtils;

public class CommandListener implements Listener {

    private final OpProtector plugin;
    private final ConfigManager configManager;
    private final MessageUtils messageUtils;

    public CommandListener(OpProtector plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageUtils = plugin.getMessageUtils();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player sender = event.getPlayer();
        String message = event.getMessage();
        String[] args = message.split(" ");

        // Проверяем команду /op
        if (args[0].equalsIgnoreCase("/op") && args.length >= 2) {
            String targetName = args[1];
            Player target = Bukkit.getPlayer(targetName);

            // Если игрок онлайн
            if (target != null) {
                // Проверяем, есть ли целевой игрок в списке допускаемых
                if (!configManager.isPlayerAllowed(target.getName())) {
                    event.setCancelled(true);
                    messageUtils.sendMessage(sender, "op-target-not-allowed", "%player%", target.getName());
                    return;
                }
            } else {
                // Если игрок оффлайн, проверяем по нику в конфиге
                if (!configManager.isPlayerAllowed(targetName)) {
                    event.setCancelled(true);
                    messageUtils.sendMessage(sender, "op-target-not-allowed", "%player%", targetName);
                    return;
                }
            }
        }

        // Проверяем команду /deop
        if (args[0].equalsIgnoreCase("/deop") && args.length >= 2) {
            String targetName = args[1];
            Player target = Bukkit.getPlayer(targetName);

            // Для /deop тоже проверяем
            if (target != null) {
                if (!configManager.isPlayerAllowed(target.getName())) {
                    event.setCancelled(true);
                    messageUtils.sendMessage(sender, "deop-target-not-allowed", "%player%", target.getName());
                    return;
                }
            } else {
                if (!configManager.isPlayerAllowed(targetName)) {
                    event.setCancelled(true);
                    messageUtils.sendMessage(sender, "deop-target-not-allowed", "%player%", targetName);
                    return;
                }
            }
        }
    }
}