package org.anonymous.withercps.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import org.anonymous.withercps.sessions.click.ClickService;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;

public record EntityListener(ClickService clickService) implements Listener {

    @EventHandler
    public void onEntityAdd(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof Boat) {
            clickService.getBoats().add(event.getEntity().getEntityId());
        }
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        if (event.getEntity() instanceof Boat) {
            clickService.getBoats().remove(event.getEntity().getEntityId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(VehicleDamageEvent event) {
        if (event.getVehicle() instanceof Boat && event.getAttacker() instanceof Player player) {
            clickService.register(player.getUniqueId(), false);
        } else if (event.getVehicle() instanceof Boat) {
            event.setCancelled(true);
        }
    }

//    @EventHandler
//    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
//        if (event.getRightClicked() instanceof Boat) {
//            clickService.register(event.getPlayer().getUniqueId(), false);
//        }
//    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDestroy(VehicleDestroyEvent event) {
        if (event.getVehicle() instanceof Boat && !(event.getAttacker() instanceof Player)) {
            event.setCancelled(true);
        }
    }
}