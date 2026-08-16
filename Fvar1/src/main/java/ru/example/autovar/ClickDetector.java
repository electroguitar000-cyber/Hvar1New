package ru.example.autovar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClickDetector implements Listener {

    private final Map<UUID, ClickSession> sessions = new HashMap<>();
    private final AutoVarHockey plugin;
    private final BlockData lightBlock;

    public ClickDetector(AutoVarHockey plugin) {
        this.plugin = plugin;
        BlockData data = Material.LIGHT.createBlockData();
        if (data instanceof Lightable lightable) {
            lightable.setLit(true);
        }
        this.lightBlock = data;
    }

    @EventHandler
    public void onBoatHit(VehicleDamageEvent e) {
        if (!(e.getVehicle() instanceof Boat)) return;
        if (!(e.getAttacker() instanceof Player player)) return;

        plugin.registerHit(player);

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        ClickSession session = sessions.get(uuid);

        if (session == null) {
            session = new ClickSession(now);
            sessions.put(uuid, session);

            final Player p = player;
            final ClickSession s = session;

            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    endSession(p, s);
                }
            };
            task.runTaskLater(plugin, 20L);
            session.task = task;

        } else {
            session.clicks++;
            session.lastClickTime = now;
            session.task.cancel();

            final Player p = player;
            final ClickSession s = session;

            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    endSession(p, s);
                }
            };
            task.runTaskLater(plugin, 20L);
            session.task = task;
        }
    }

    private void endSession(Player player, ClickSession session) {
        sessions.remove(player.getUniqueId());

        double totalTimeSec = (session.lastClickTime - session.firstClickTime) / 1000.0;
        double adjustedTime = Math.max(totalTimeSec - 1.0, 0.01);
        double cps = session.clicks / adjustedTime;

        String team = AutoVarHockey.getInstance().statsManager.getTeamSide(player.getUniqueId());
        String teamName = team != null ? AutoVarHockey.getInstance().statsManager.getTeamName(team) : "-";

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("clicks", String.valueOf(session.clicks));
        placeholders.put("time", String.format("%.2f", adjustedTime));
        placeholders.put("cps", String.format("%.2f", cps));
        placeholders.put("team", teamName);

        String message = plugin.messageManager.getMessage("cps.message", placeholders);

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (plugin.clickCommand.isViewer(online)) {
                online.sendMessage(message);
            }
        }
    }

    private static class ClickSession {
        long firstClickTime;
        long lastClickTime;
        int clicks = 1;
        BukkitRunnable task;

        ClickSession(long time) {
            this.firstClickTime = time;
            this.lastClickTime = time;
        }
    }
}