package ru.dev.koramikon.hvar1new.listener;

import org.bukkit.entity.Boat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import ru.dev.koramikon.hvar1new.Hvar1NewPlugin;

public class BoatRemoveListener implements Listener {

    private final Hvar1NewPlugin plugin;

    public BoatRemoveListener(Hvar1NewPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBoatRemove(EntityRemoveEvent event) {
        if (!(event.getEntity() instanceof Boat boat)) return;
        // Определяем арену по центру (можно по воротам, но проще по центру)
        int arenaId = -1;
        for (int i = 1; i <= 50; i++) {
            if (plugin.centerLocations[i] != null && plugin.centerLocations[i].getWorld().equals(boat.getWorld())) {
                if (boat.getLocation().distance(plugin.centerLocations[i]) <= 200) {
                    arenaId = i;
                    break;
                }
            }
        }
        if (arenaId != -1) {
            plugin.getBoatControlManager().onBoatRemove(boat.getUniqueId(), arenaId);
        }
    }
}