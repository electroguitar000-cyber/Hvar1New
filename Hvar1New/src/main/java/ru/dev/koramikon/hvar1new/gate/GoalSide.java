package ru.dev.koramikon.hvar1new.gate;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.BoundingBox;

/**
 * Одна сторона ворот (левые или правые).
 * Автор: Koramikon (на основе ru.example.autovar)
 */
public class GoalSide {

    private Location pos1;
    private Location pos2;
    private BoundingBox cachedBB;

    public void setPos1(Location loc) {
        pos1 = loc;
        updateCache();
    }

    public void setPos2(Location loc) {
        pos2 = loc;
        updateCache();
    }

    private void updateCache() {
        if (pos1 == null || pos2 == null) {
            cachedBB = null;
            return;
        }

        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = minY + 4.0;
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        double thickness = 2.0;

        cachedBB = new BoundingBox(
                minX - thickness / 2, minY, minZ - thickness / 2,
                maxX + thickness / 2, maxY, maxZ + thickness / 2
        );
    }

    public BoundingBox getGoalBB() {
        return cachedBB;
    }

    public boolean hasBoth() {
        return pos1 != null && pos2 != null;
    }

    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }

    public void loadFromConfig(FileConfiguration config, String path) {
        pos1 = config.getLocation(path + ".pos1");
        pos2 = config.getLocation(path + ".pos2");
        updateCache();
    }

    public void saveToConfig(FileConfiguration config, String path) {
        if (pos1 != null) config.set(path + ".pos1", pos1);
        if (pos2 != null) config.set(path + ".pos2", pos2);
    }

    public void delete() {
        pos1 = null;
        pos2 = null;
        cachedBB = null;
    }
}
