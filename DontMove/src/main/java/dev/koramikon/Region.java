package dev.koramikon.dontmove;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.util.BoundingBox;

public class Region {
    private Location pos1;
    private Location pos2;
    private boolean set = false;

    public void setPos1(Location loc) {
        this.pos1 = loc.clone();
        checkSet();
    }

    public void setPos2(Location loc) {
        this.pos2 = loc.clone();
        checkSet();
    }

    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }

    private void checkSet() {
        set = (pos1 != null && pos2 != null);
    }

    public boolean isSet() { return set; }

    public BoundingBox getBoundingBox() {
        if (!set) return null;
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean intersects(BoundingBox boatBox) {
        if (!set) return false;
        BoundingBox regionBox = getBoundingBox();
        return regionBox.overlaps(boatBox);
    }

    public Vector getPushOutVector(BoundingBox boatBox) {
        if (!set) return null;
        BoundingBox regionBox = getBoundingBox();
        if (!regionBox.overlaps(boatBox)) return null;

        Vector regionCenter = regionBox.getCenter();
        Vector boatCenter = boatBox.getCenter();
        Vector dir = boatCenter.clone().subtract(regionCenter).normalize();

        double dx = 0, dy = 0, dz = 0;
        if (boatBox.getMaxX() > regionBox.getMinX() && boatBox.getMinX() < regionBox.getMaxX()) {
            double overlapX = Math.min(boatBox.getMaxX() - regionBox.getMinX(), regionBox.getMaxX() - boatBox.getMinX());
            if (dir.getX() > 0) {
                dx = overlapX;
            } else if (dir.getX() < 0) {
                dx = -overlapX;
            } else {
                dx = overlapX;
            }
        }
        if (boatBox.getMaxY() > regionBox.getMinY() && boatBox.getMinY() < regionBox.getMaxY()) {
            double overlapY = Math.min(boatBox.getMaxY() - regionBox.getMinY(), regionBox.getMaxY() - boatBox.getMinY());
            if (dir.getY() > 0) {
                dy = overlapY;
            } else if (dir.getY() < 0) {
                dy = -overlapY;
            } else {
                dy = overlapY;
            }
        }
        if (boatBox.getMaxZ() > regionBox.getMinZ() && boatBox.getMinZ() < regionBox.getMaxZ()) {
            double overlapZ = Math.min(boatBox.getMaxZ() - regionBox.getMinZ(), regionBox.getMaxZ() - boatBox.getMinZ());
            if (dir.getZ() > 0) {
                dz = overlapZ;
            } else if (dir.getZ() < 0) {
                dz = -overlapZ;
            } else {
                dz = overlapZ;
            }
        }

        return new Vector(dx, dy, dz);
    }

    public boolean isInside(Location loc) {
        if (!set) return false;
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public Location getIntersection(Location from, Location to) {
        if (!set) return null;
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        Vector origin = from.toVector();
        Vector dir = to.toVector().subtract(origin);

        double tMin = 0.0;
        double tMax = 1.0;

        if (Math.abs(dir.getX()) < 1e-8) {
            if (origin.getX() < minX || origin.getX() > maxX) return null;
        } else {
            double t1 = (minX - origin.getX()) / dir.getX();
            double t2 = (maxX - origin.getX()) / dir.getX();
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return null;
        }
        if (Math.abs(dir.getY()) < 1e-8) {
            if (origin.getY() < minY || origin.getY() > maxY) return null;
        } else {
            double t1 = (minY - origin.getY()) / dir.getY();
            double t2 = (maxY - origin.getY()) / dir.getY();
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return null;
        }
        if (Math.abs(dir.getZ()) < 1e-8) {
            if (origin.getZ() < minZ || origin.getZ() > maxZ) return null;
        } else {
            double t1 = (minZ - origin.getZ()) / dir.getZ();
            double t2 = (maxZ - origin.getZ()) / dir.getZ();
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return null;
        }
        if (tMin < 0) tMin = 0;
        if (tMin > 1) return null;
        Vector hit = origin.clone().add(dir.clone().multiply(tMin));
        return new Location(from.getWorld(), hit.getX(), hit.getY(), hit.getZ());
    }
}