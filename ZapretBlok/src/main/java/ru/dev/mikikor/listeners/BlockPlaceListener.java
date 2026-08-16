package ru.dev.mikikor.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import ru.dev.mikikor.ZapretBlok;

public class BlockPlaceListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Если защита выключена - разрешаем всё
        if (!ZapretBlok.getInstance().isProtectionEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        // Проверка на bypass (опционально)
        // if (player.hasPermission("zapret.bypass") || player.isOp()) {
        //     return;
        // }

        if (ZapretBlok.getInstance().getConfigManager().isBlockedToPlace(event.getBlock().getType())) {
            event.setCancelled(true);
            player.sendMessage(ZapretBlok.getInstance().getConfigManager().getMessage("messages.cannot_place")
                    .replace("{block}", event.getBlock().getType().toString().toLowerCase()));
        }
    }
}