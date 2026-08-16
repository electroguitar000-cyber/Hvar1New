package ru.koramikon.opprotector.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.koramikon.opprotector.OpProtector;
import ru.koramikon.opprotector.managers.ConfigManager;
import ru.koramikon.opprotector.utils.MessageUtils;

public class OpProtectionListener implements Listener {

    private final OpProtector plugin;
    private final ConfigManager configManager;
    private final MessageUtils messageUtils;

    public OpProtectionListener(OpProtector plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageUtils = plugin.getMessageUtils();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Автоматически снимаем OP если игрок не в списке
        if (player.isOp() && !configManager.isPlayerAllowed(player.getName())) {
            player.setOp(false);
            plugin.getLogger().warning("§c[OpProtector] Автоматически снят OP с " + player.getName() + " (нет в списке допускаемых)");

            // Уведомляем админов
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission("opprotector.admin") || admin.isOp()) {
                    admin.sendMessage("§c§l[OpProtector] §fАвтоматически снят OP с §e" + player.getName() + " §f(нет в списке)");
                }
            }
        }
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

            // Проверяем, есть ли целевой игрок в списке
            boolean isAllowed;
            if (target != null) {
                isAllowed = configManager.isPlayerAllowed(target.getName());
            } else {
                isAllowed = configManager.isPlayerAllowed(targetName);
            }

            if (!isAllowed) {
                event.setCancelled(true);
                messageUtils.sendMessage(sender, "op-target-not-allowed", "%player%", targetName);
                return;
            }
        }

        // Проверяем команду /deop
        if (args[0].equalsIgnoreCase("/deop") && args.length >= 2) {
            String targetName = args[1];
            Player target = Bukkit.getPlayer(targetName);

            // Проверяем, есть ли целевой игрок в списке
            boolean isAllowed;
            if (target != null) {
                isAllowed = configManager.isPlayerAllowed(target.getName());
            } else {
                isAllowed = configManager.isPlayerAllowed(targetName);
            }

            if (!isAllowed) {
                event.setCancelled(true);
                messageUtils.sendMessage(sender, "deop-target-not-allowed", "%player%", targetName);
                return;
            }
        }
    }

    // Метод для проверки всех игроков
    public void checkAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp() && !configManager.isPlayerAllowed(player.getName())) {
                player.setOp(false);
                plugin.getLogger().warning("§c[OpProtector] Принудительно снят OP с " + player.getName());
                messageUtils.sendMessage(player, "op-removed");
            }
        }
    }
}