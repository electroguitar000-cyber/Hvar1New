package ru.example.autovar;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GoalListener implements Listener {

    private final Map<UUID, BukkitTask> pendingGoals = new HashMap<>();
    private final Map<UUID, Long> lastGoalTime = new HashMap<>();

    private static final double BOAT_VOLUME = 1.375 * 0.5625 * 1.375;
    private static final double REQUIRED_VOLUME = (20.5 / 22.0) * BOAT_VOLUME;

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;

        AutoVarHockey plugin = AutoVarHockey.getInstance();

        if (!plugin.areGatesEnabled()) return;

        BoundingBox boatBB = boat.getBoundingBox();

        double maxInterVolume = 0.0;
        String scoredAgainst = null;

        BoundingBox leftBB = plugin.leftGate.getGoalBB();
        if (leftBB != null && boatBB.overlaps(leftBB)) {
            BoundingBox inter = boatBB.clone().intersection(leftBB);
            double vol = Math.max(0, inter.getVolume());
            if (vol > maxInterVolume) {
                maxInterVolume = vol;
                scoredAgainst = "L";
            }
        }

        BoundingBox rightBB = plugin.rightGate.getGoalBB();
        if (rightBB != null && boatBB.overlaps(rightBB)) {
            BoundingBox inter = boatBB.clone().intersection(rightBB);
            double vol = Math.max(0, inter.getVolume());
            if (vol > maxInterVolume) {
                maxInterVolume = vol;
                scoredAgainst = "R";
            }
        }

        boolean sufficientOverlap = maxInterVolume >= REQUIRED_VOLUME;

        UUID uid = boat.getUniqueId();
        long now = System.currentTimeMillis();

        if (sufficientOverlap) {
            if (!pendingGoals.containsKey(uid)) {
                if (now - lastGoalTime.getOrDefault(uid, 0L) < 5000) return;

                // Используем статический метод через класс, а не через экземпляр
                long delay = AutoVarHockey.getGateDelayTicks();

                BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!boat.isValid()) {
                        pendingGoals.remove(uid);
                        return;
                    }

                    BoundingBox currentBB = boat.getBoundingBox();
                    double currentMaxVol = 0.0;
                    String currentScoredAgainst = null;

                    if (leftBB != null && currentBB.overlaps(leftBB)) {
                        double vol = Math.max(0, currentBB.clone().intersection(leftBB).getVolume());
                        if (vol > currentMaxVol) {
                            currentMaxVol = vol;
                            currentScoredAgainst = "L";
                        }
                    }

                    if (rightBB != null && currentBB.overlaps(rightBB)) {
                        double vol = Math.max(0, currentBB.clone().intersection(rightBB).getVolume());
                        if (vol > currentMaxVol) {
                            currentMaxVol = vol;
                            currentScoredAgainst = "R";
                        }
                    }

                    if (currentMaxVol >= REQUIRED_VOLUME && currentScoredAgainst != null) {
                        plugin.handleGoal(boat, currentScoredAgainst);
                        lastGoalTime.put(uid, System.currentTimeMillis());

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (boat.isValid()) boat.remove();
                        }, 20L);
                    }
                    pendingGoals.remove(uid);
                }, delay);

                pendingGoals.put(uid, task);
            }
        } else {
            BukkitTask task = pendingGoals.remove(uid);
            if (task != null) task.cancel();
        }
    }
}