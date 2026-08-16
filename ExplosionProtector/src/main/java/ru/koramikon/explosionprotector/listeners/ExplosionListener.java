package ru.koramikon.explosionprotector.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import ru.koramikon.explosionprotector.ExplosionProtector;

import java.util.List;

public class ExplosionListener implements Listener {

    private final ExplosionProtector plugin;

    public ExplosionListener(ExplosionProtector plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        ItemStack item = event.getItem();

        if (clickedBlock != null
                && clickedBlock.getType() == Material.RESPAWN_ANCHOR
                && item != null
                && item.getType() == Material.GLOWSTONE) {

            String configName = "minecraft:respawn_anchor";

            if (shouldCancel(configName)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        EntityType type = event.getEntityType();
        String entityName = type.name();
        String configName = "minecraft:" + entityName.toLowerCase();

        if (shouldCancel(configName)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Block block = event.getBlock();
        Material blockType = block.getType();
        String blockName = blockType.name();
        String configName = "minecraft:" + blockName.toLowerCase();

        if (shouldCancel(configName)) {
            event.setCancelled(true);
        }
    }

    private boolean shouldCancel(String configName) {
        List<String> blockList = plugin.getBlockList();
        String mode = plugin.getMode();

        boolean isInList = blockList.stream()
                .anyMatch(item -> item.equalsIgnoreCase(configName));

        if (mode.equals("BLACKLIST")) {
            return isInList;
        } else {
            return !isInList;
        }
    }
}