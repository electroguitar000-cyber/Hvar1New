package ru.dev.koramikon.hvar1new.listener;

import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import ru.dev.koramikon.hvar1new.Hvar1NewPlugin;
import ru.dev.koramikon.hvar1new.gate.GoalSide;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PostHitDetector {

    private final Hvar1NewPlugin plugin;
    private final Map<UUID, Long> lastStvorHit = new HashMap<>();

    public PostHitDetector(Hvar1NewPlugin plugin) {
        this.plugin = plugin;
        startStvorChecker();
    }

    private void startStvorChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                    for (org.bukkit.entity.Entity entity : world.getEntities()) {
                        if (!(entity instanceof Boat boat)) continue;

                        BoundingBox boatBox = boat.getBoundingBox();
                        UUID boatId = boat.getUniqueId();

                        for (int arenaId = 1; arenaId <= 50; arenaId++) {
                            if (checkStvor(boatBox, plugin.stvor1[arenaId])) {
                                addStvorHit(boatId, arenaId, "L");
                            }
                            if (checkStvor(boatBox, plugin.stvor3[arenaId])) {
                                addStvorHit(boatId, arenaId, "L");
                            }
                            if (checkStvor(boatBox, plugin.stvor2[arenaId])) {
                                addStvorHit(boatId, arenaId, "R");
                            }
                            if (checkStvor(boatBox, plugin.stvor4[arenaId])) {
                                addStvorHit(boatId, arenaId, "R");
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private boolean checkStvor(BoundingBox boatBox, GoalSide stvor) {
        if (stvor == null) return false;
        if (!stvor.hasBoth() && stvor.getPos1() != null) {
            Location loc = stvor.getPos1();
            BoundingBox stvorBB = new BoundingBox(
                    loc.getX() - 0.5, loc.getY(), loc.getZ() - 0.5,
                    loc.getX() + 0.5, loc.getY() + 3.0, loc.getZ() + 0.5
            );
            return boatBox.overlaps(stvorBB);
        }
        BoundingBox bb = stvor.getGoalBB();
        return bb != null && boatBox.overlaps(bb);
    }

    private void addStvorHit(UUID boatId, int arenaId, String side) {
        long now = System.currentTimeMillis();
        Long last = lastStvorHit.get(boatId);
        if (last == null || now - last > 1000) {
            plugin.statsManagers[arenaId].addStvor(side);
            lastStvorHit.put(boatId, now);
            // Лог только для отладки
            plugin.getLogger().info("[PostHit] створ в ворота " + side + " на арене " + arenaId);
        }
    }
}