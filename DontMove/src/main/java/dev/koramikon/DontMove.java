package dev.koramikon.dontmove;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.util.BoundingBox;

public class DontMove extends JavaPlugin implements Listener {
    private Region[] regions = new Region[4];
    private boolean blockingEnabled = false;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        for (int i = 0; i < 4; i++) {
            regions[i] = new Region();
        }
        loadConfig();
        getCommand("dontmove").setExecutor(new CommandHandler(this));
        getCommand("dontmove").setTabCompleter(new CommandHandler(this));
        getServer().getPluginManager().registerEvents(this, this);
        messageManager = new MessageManager(this);
        getLogger().info("DontMove enabled!");
    }

    @Override
    public void onDisable() {
        saveConfig();
        getLogger().info("DontMove disabled!");
    }

    public void loadConfig() {
        FileConfiguration config = getConfig();
        blockingEnabled = config.getBoolean("enabled", false);
        for (int i = 1; i <= 4; i++) {
            String path = "regions." + i + ".";
            if (config.contains(path + "pos1")) {
                Location pos1 = (Location) config.get(path + "pos1");
                Location pos2 = (Location) config.get(path + "pos2");
                if (pos1 != null && pos2 != null) {
                    regions[i - 1].setPos1(pos1);
                    regions[i - 1].setPos2(pos2);
                }
            }
        }
    }

    public void saveConfig() {
        FileConfiguration config = getConfig();
        config.set("enabled", blockingEnabled);
        for (int i = 1; i <= 4; i++) {
            Region region = regions[i - 1];
            if (region.isSet()) {
                config.set("regions." + i + ".pos1", region.getPos1());
                config.set("regions." + i + ".pos2", region.getPos2());
            } else {
                config.set("regions." + i, null);
            }
        }
        super.saveConfig();
    }

    public Region getRegion(int index) {
        if (index < 0 || index >= 4) return null;
        return regions[index];
    }

    public boolean isBlockingEnabled() {
        return blockingEnabled;
    }

    public void setBlockingEnabled(boolean enabled) {
        this.blockingEnabled = enabled;
        saveConfig();
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!blockingEnabled) return;
        Entity vehicle = event.getVehicle();
        if (!(vehicle instanceof Boat)) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        BoundingBox boatBox = vehicle.getBoundingBox();
        Vector shift = to.toVector().subtract(vehicle.getLocation().toVector());
        BoundingBox boxAtTo = boatBox.clone().shift(shift);

        for (Region region : regions) {
            if (!region.isSet()) continue;

            if (region.isInside(from) && !region.isInside(to)) continue;

            if (region.intersects(boxAtTo)) {
                float yaw = vehicle.getLocation().getYaw();
                float pitch = vehicle.getLocation().getPitch();
                Location back = from.clone();
                back.setYaw(yaw);
                back.setPitch(pitch);
                vehicle.teleport(back);

                Boat boat = (Boat) vehicle;
                Vector vel = boat.getVelocity();
                Location center = region.getPos1().clone().add(region.getPos2()).multiply(0.5);
                Vector outDir = boat.getLocation().toVector().subtract(center.toVector()).normalize();
                double proj = vel.dot(outDir);
                if (proj < 0) {
                    vel.subtract(outDir.clone().multiply(proj));
                    boat.setVelocity(vel);
                }
                return;
            }
        }
    }
}