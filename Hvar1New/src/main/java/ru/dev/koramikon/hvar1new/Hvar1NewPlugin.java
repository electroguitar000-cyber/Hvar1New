package ru.dev.koramikon.hvar1new;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import ru.dev.koramikon.hvar1new.command.ClickCommand;
import ru.dev.koramikon.hvar1new.command.HvarCommand;
import ru.dev.koramikon.hvar1new.gate.GoalListener;
import ru.dev.koramikon.hvar1new.gate.GoalSide;
import ru.dev.koramikon.hvar1new.listener.BoatControlManager;
import ru.dev.koramikon.hvar1new.listener.BoatRemoveListener;
import ru.dev.koramikon.hvar1new.listener.ClickDetector;
import ru.dev.koramikon.hvar1new.stats.StatsManager;
import ru.dev.koramikon.hvar1new.util.MessageUtil;

import java.util.*;

public class Hvar1NewPlugin extends JavaPlugin implements Listener {

    private static Hvar1NewPlugin instance;

    public GoalSide leftGate;
    public GoalSide rightGate;
    private boolean gatesEnabled;

    public final GoalSide[] leftGates = new GoalSide[51];
    public final GoalSide[] rightGates = new GoalSide[51];
    public final GoalSide[] stvor1 = new GoalSide[51];
    public final GoalSide[] stvor2 = new GoalSide[51];
    public final GoalSide[] stvor3 = new GoalSide[51];
    public final GoalSide[] stvor4 = new GoalSide[51];
    public final GoalSide[] saveL = new GoalSide[51];
    public final GoalSide[] saveR = new GoalSide[51];
    public final StatsManager[] statsManagers = new StatsManager[51];
    public final boolean[] gatesEnabledArr = new boolean[51];
    public final long[] gateDelayTicks = new long[51];
    public final Location[] centerLocations = new Location[51];

    public final TextDisplay[] placeholders = new TextDisplay[51];
    public final Location[] placeholderLocations = new Location[51];

    public final BoundingBox[] zone1 = new BoundingBox[51];
    public final BoundingBox[] zone2 = new BoundingBox[51];
    public final BoundingBox[] zone3 = new BoundingBox[51];
    public final Location[][] zonePoints = new Location[51][6];

    private final Deque<HitRecord>[] lastHits = new Deque[51];
    private final HitRecord[] lastGoal = new HitRecord[51];

    private ClickCommand clickCommand;
    private BoatControlManager boatControlManager;
    private int placeholderUpdateTask = -1;

    private String goalMusicFolder;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        MessageUtil.init(this);

        for (int i = 1; i <= 50; i++) {
            leftGates[i] = new GoalSide();
            rightGates[i] = new GoalSide();
            stvor1[i] = new GoalSide();
            stvor2[i] = new GoalSide();
            stvor3[i] = new GoalSide();
            stvor4[i] = new GoalSide();
            saveL[i] = new GoalSide();
            saveR[i] = new GoalSide();
            statsManagers[i] = new StatsManager(i);
            gatesEnabledArr[i] = false;
            gateDelayTicks[i] = 8L;
            lastHits[i] = new ArrayDeque<>(10);
            lastGoal[i] = null;
            loadArenaFromConfig(i);
        }

        leftGate = leftGates[1];
        rightGate = rightGates[1];
        gatesEnabled = gatesEnabledArr[1];

        goalMusicFolder = getConfig().getString("goal-random-music-folder", "");
        getLogger().info("goal-random-music-folder = '" + goalMusicFolder + "'");

        boatControlManager = new BoatControlManager(this);

        clickCommand = new ClickCommand(this);
        var hclickCmd = getCommand("hclick");
        if (hclickCmd != null) hclickCmd.setExecutor(clickCommand);

        HvarCommand hvarExecutor = new HvarCommand(this);
        var oldCmd = getCommand("Hvar1");
        if (oldCmd != null) {
            oldCmd.setExecutor(hvarExecutor);
            oldCmd.setTabCompleter(hvarExecutor);
        }
        for (int i = 1; i <= 50; i++) {
            String name = "hvar" + i;
            var cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(hvarExecutor);
                cmd.setTabCompleter(hvarExecutor);
            }
        }

        getServer().getPluginManager().registerEvents(new GoalListener(this), this);
        getServer().getPluginManager().registerEvents(new ClickDetector(this), this);
        getServer().getPluginManager().registerEvents(new BoatRemoveListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (int i = 1; i <= 50; i++) updatePlayerColors(i);
        }, 0L, 100L);

        placeholderUpdateTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (int i = 1; i <= 50; i++) {
                if (placeholders[i] != null && placeholders[i].isValid()) {
                    updatePlaceholderText(i);
                }
            }
        }, 0L, 200L);

        getLogger().info("Hvar1New включён!");
    }

    @Override
    public void onDisable() {
        if (placeholderUpdateTask != -1) {
            Bukkit.getScheduler().cancelTask(placeholderUpdateTask);
        }
        for (int i = 1; i <= 50; i++) {
            if (placeholders[i] != null && placeholders[i].isValid()) {
                placeholders[i].remove();
            }
        }
        for (int i = 1; i <= 50; i++) saveArenaToConfig(i);
        saveConfig();
    }

    private void loadArenaFromConfig(int id) {
        String base = "arena" + id + ".";
        leftGates[id].loadFromConfig(getConfig(), base + "left");
        rightGates[id].loadFromConfig(getConfig(), base + "right");
        stvor1[id].loadFromConfig(getConfig(), base + "stvor1");
        stvor2[id].loadFromConfig(getConfig(), base + "stvor2");
        stvor3[id].loadFromConfig(getConfig(), base + "stvor3");
        stvor4[id].loadFromConfig(getConfig(), base + "stvor4");
        saveL[id].loadFromConfig(getConfig(), base + "saveL");
        saveR[id].loadFromConfig(getConfig(), base + "saveR");
        gatesEnabledArr[id] = getConfig().getBoolean(base + "enabled", false);
        gateDelayTicks[id] = getConfig().getLong(base + "gate-delay-ticks", 8L);
        statsManagers[id].loadFromConfig(getConfig(), base);
        zonePoints[id][0] = getConfig().getLocation(base + "zone1.pos1");
        zonePoints[id][1] = getConfig().getLocation(base + "zone1.pos2");
        zonePoints[id][2] = getConfig().getLocation(base + "zone2.pos1");
        zonePoints[id][3] = getConfig().getLocation(base + "zone2.pos2");
        zonePoints[id][4] = getConfig().getLocation(base + "zone3.pos1");
        zonePoints[id][5] = getConfig().getLocation(base + "zone3.pos2");
        updateZoneBB(id);
        updateCenter(id);

        Location loc = getConfig().getLocation(base + "placeholder.location");
        if (loc != null) {
            placeholderLocations[id] = loc;
            createPlaceholder(id, loc);
        }
    }

    private void saveArenaToConfig(int id) {
        String base = "arena" + id + ".";
        leftGates[id].saveToConfig(getConfig(), base + "left");
        rightGates[id].saveToConfig(getConfig(), base + "right");
        stvor1[id].saveToConfig(getConfig(), base + "stvor1");
        stvor2[id].saveToConfig(getConfig(), base + "stvor2");
        stvor3[id].saveToConfig(getConfig(), base + "stvor3");
        stvor4[id].saveToConfig(getConfig(), base + "stvor4");
        saveL[id].saveToConfig(getConfig(), base + "saveL");
        saveR[id].saveToConfig(getConfig(), base + "saveR");
        getConfig().set(base + "enabled", gatesEnabledArr[id]);
        getConfig().set(base + "gate-delay-ticks", gateDelayTicks[id]);
        if (centerLocations[id] != null) getConfig().set(base + "center", centerLocations[id]);
        statsManagers[id].saveToConfig(getConfig(), base);
        if (zonePoints[id][0] != null) getConfig().set(base + "zone1.pos1", zonePoints[id][0]);
        if (zonePoints[id][1] != null) getConfig().set(base + "zone1.pos2", zonePoints[id][1]);
        if (zonePoints[id][2] != null) getConfig().set(base + "zone2.pos1", zonePoints[id][2]);
        if (zonePoints[id][3] != null) getConfig().set(base + "zone2.pos2", zonePoints[id][3]);
        if (zonePoints[id][4] != null) getConfig().set(base + "zone3.pos1", zonePoints[id][4]);
        if (zonePoints[id][5] != null) getConfig().set(base + "zone3.pos2", zonePoints[id][5]);

        if (placeholderLocations[id] != null) {
            getConfig().set(base + "placeholder.location", placeholderLocations[id]);
        } else {
            getConfig().set(base + "placeholder.location", null);
        }
    }

    public void updateZoneBB(int id) {
        zone1[id] = buildBB(zonePoints[id][0], zonePoints[id][1]);
        zone2[id] = buildBB(zonePoints[id][2], zonePoints[id][3]);
        zone3[id] = buildBB(zonePoints[id][4], zonePoints[id][5]);
    }

    private BoundingBox buildBB(Location p1, Location p2) {
        if (p1 == null || p2 == null) return null;
        double minX = Math.min(p1.getX(), p2.getX());
        double maxX = Math.max(p1.getX(), p2.getX());
        double minY = Math.min(p1.getY(), p2.getY());
        double maxY = Math.max(p1.getY(), p2.getY());
        double minZ = Math.min(p1.getZ(), p2.getZ());
        double maxZ = Math.max(p1.getZ(), p2.getZ());
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public void updateCenter(int arenaId) {
        if (leftGates[arenaId].hasBoth() && rightGates[arenaId].hasBoth()) {
            Location leftPos = leftGates[arenaId].getPos1();
            Location rightPos = rightGates[arenaId].getPos1();
            if (leftPos != null && rightPos != null && leftPos.getWorld().equals(rightPos.getWorld())) {
                centerLocations[arenaId] = new Location(
                        leftPos.getWorld(),
                        (leftPos.getX() + rightPos.getX()) / 2,
                        (leftPos.getY() + rightPos.getY()) / 2,
                        (leftPos.getZ() + rightPos.getZ()) / 2
                );
            }
        }
    }

    public void registerHit(int arenaId, Player player) {
        lastHits[arenaId].addFirst(new HitRecord(player.getUniqueId(), System.currentTimeMillis()));
        if (lastHits[arenaId].size() > 10) lastHits[arenaId].removeLast();
    }

    public void handleGoal(int arenaId, Boat boat, String scoredAgainst) {
        String scoringTeam = scoredAgainst.equals("L") ? "R" : "L";
        UUID goalScorer = null;
        UUID assist = null;

        List<HitRecord> hits = new ArrayList<>(lastHits[arenaId]);
        for (int i = 0; i < hits.size(); i++) {
            HitRecord hit = hits.get(i);
            String team = statsManagers[arenaId].getTeamSide(hit.player);
            if (team != null && team.equals(scoringTeam)) {
                goalScorer = hit.player;
                for (int j = i + 1; j < hits.size(); j++) {
                    HitRecord next = hits.get(j);
                    String nextTeam = statsManagers[arenaId].getTeamSide(next.player);
                    if (nextTeam != null && nextTeam.equals(scoringTeam) && !next.player.equals(goalScorer)) {
                        assist = next.player;
                        break;
                    }
                }
                break;
            }
        }

        if (goalScorer != null) {
            lastGoal[arenaId] = new HitRecord(goalScorer, assist, System.currentTimeMillis());
            statsManagers[arenaId].addGoal(goalScorer);
            if (assist != null) statsManagers[arenaId].addAssist(assist);
        }

        String goalMsgRaw = MessageUtil.getRaw("goal-msg")
                .replace("{arena}", String.valueOf(arenaId))
                .replace("{side}", scoredAgainst);
        String goalMsgColored = MessageUtil.colorize(goalMsgRaw);
        var goalComponent = MessageUtil.fromColored(goalMsgColored);

        String authorMsgColored = null;
        if (goalScorer != null) {
            String scorerName = Bukkit.getOfflinePlayer(goalScorer).getName();
            String assistText = (assist != null) ?
                    MessageUtil.getRaw("goal-assist-format").replace("{player}", Bukkit.getOfflinePlayer(assist).getName()) :
                    MessageUtil.getRaw("goal-assist-default");
            String authorMsgRaw = MessageUtil.getRaw("goal-author")
                    .replace("{scorer}", scorerName)
                    .replace("{assist}", assistText);
            authorMsgColored = MessageUtil.colorize(authorMsgRaw);
        }

        Location center = centerLocations[arenaId];
        if (center == null) center = boat.getLocation();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(center.getWorld())) continue;
            if (p.getLocation().distance(center) <= 100) {
                p.sendMessage(goalComponent);
                if (authorMsgColored != null) p.sendMessage(MessageUtil.fromColored(authorMsgColored));
            }
        }

        lastHits[arenaId].clear();
        boat.getWorld().strikeLightningEffect(boat.getLocation());
        boat.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, boat.getLocation(), 200, 1, 1, 1, 0.1);
        boat.getWorld().playSound(boat.getLocation(), org.bukkit.Sound.BLOCK_CANDLE_EXTINGUISH, 10, 1);
        if (boat.isValid()) boat.remove();

        // ===== ВОСПРОИЗВОДИМ СЛУЧАЙНУЮ МУЗЫКУ ГОЛА =====
        if (!goalMusicFolder.isEmpty()) {
            boolean cmdResult = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "music play random A " + goalMusicFolder);
            getLogger().info("Команда музыки гола выполнена: " + cmdResult + " (папка: " + goalMusicFolder + ")");
        }
    }

    public void cancelLastGoal(int arenaId) {
        if (lastGoal[arenaId] == null) return;
        statsManagers[arenaId].removeGoal(lastGoal[arenaId].player);
        if (lastGoal[arenaId].assist != null) statsManagers[arenaId].removeAssist(lastGoal[arenaId].assist);
        lastGoal[arenaId] = null;
        lastHits[arenaId].clear();
        Bukkit.broadcastMessage(MessageUtil.get("goal-canceled").replace("{arena}", String.valueOf(arenaId)));
    }

    private void updatePlayerColors(int arenaId) {
        Location center = centerLocations[arenaId];
        if (center == null) return;

        for (String side : List.of("L", "R")) {
            for (UUID uuid : statsManagers[arenaId].getTeamPlayers(side)) {
                Player p = Bukkit.getPlayer(uuid);
                String color = "gray";
                if (p != null && p.isOnline() && p.getWorld().equals(center.getWorld())) {
                    if (p.getLocation().distance(center) <= 150) {
                        color = "green";
                    } else {
                        color = "red";
                    }
                }
                statsManagers[arenaId].setPlayerColorRaw(uuid, color);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        double distance = Math.sqrt(
                Math.pow(to.getX() - from.getX(), 2) +
                        Math.pow(to.getY() - from.getY(), 2) +
                        Math.pow(to.getZ() - from.getZ(), 2)
        );

        if (distance < 0.01) return;

        for (int i = 1; i <= 50; i++) {
            if (gatesEnabledArr[i] && statsManagers[i].getTeamSide(uuid) != null) {
                statsManagers[i].addBlocks(uuid, distance);
            }
        }
    }

    public void createPlaceholder(int arenaId, Location loc) {
        deletePlaceholder(arenaId);
        var world = loc.getWorld();
        if (world == null) return;

        TextDisplay display = world.spawn(loc, TextDisplay.class);
        display.setText(MessageUtil.colorize(statsManagers[arenaId].getPlaceholderStats(arenaId)));
        display.setLineWidth(1000);
        display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(2.0f, 2.0f, 2.0f),
                new AxisAngle4f(0, 0, 0, 1)
        ));
        display.setViewRange(150);
        display.setDefaultBackground(false);
        display.setShadowed(true);

        placeholders[arenaId] = display;
        placeholderLocations[arenaId] = loc.clone();
        getConfig().set("arena" + arenaId + ".placeholder.location", loc);
        saveConfig();
    }

    public void updatePlaceholderText(int arenaId) {
        TextDisplay display = placeholders[arenaId];
        if (display == null || !display.isValid()) return;
        String text = MessageUtil.colorize(statsManagers[arenaId].getPlaceholderStats(arenaId));
        display.setText(text);
    }

    public void deletePlaceholder(int arenaId) {
        if (placeholders[arenaId] != null && placeholders[arenaId].isValid()) {
            placeholders[arenaId].remove();
        }
        placeholders[arenaId] = null;
        placeholderLocations[arenaId] = null;
        getConfig().set("arena" + arenaId + ".placeholder.location", null);
        saveConfig();
    }

    public void reloadPlugin() {
        reloadConfig();
        for (int i = 1; i <= 50; i++) {
            if (placeholders[i] != null && placeholders[i].isValid()) {
                placeholders[i].remove();
            }
            placeholders[i] = null;
            placeholderLocations[i] = null;
            loadArenaFromConfig(i);
        }
        leftGate = leftGates[1];
        rightGate = rightGates[1];
        gatesEnabled = gatesEnabledArr[1];
        MessageUtil.reloadMessages();
        for (int i = 1; i <= 50; i++) {
            updateCenter(i);
            updateZoneBB(i);
        }
        getLogger().info("Плагин перезагружен!");
    }

    public static Hvar1NewPlugin getInstance() { return instance; }
    public ClickCommand getClickCommand() { return clickCommand; }
    public BoatControlManager getBoatControlManager() { return boatControlManager; }

    public void registerHit(Player player) { registerHit(1, player); }
    public void handleGoal(Boat boat, String scoredAgainst) { handleGoal(1, boat, scoredAgainst); }
    public void cancelLastGoal() { cancelLastGoal(1); }
    public boolean areGatesEnabled() { return gatesEnabledArr[1]; }
    public void setGatesEnabled(boolean enabled) { gatesEnabledArr[1] = enabled; }
    public static long getGateDelayTicks() { return instance.gateDelayTicks[1]; }

    private static class HitRecord {
        final UUID player;
        final UUID assist;
        final long time;
        HitRecord(UUID player, long time) { this(player, null, time); }
        HitRecord(UUID player, UUID assist, long time) {
            this.player = player;
            this.assist = assist;
            this.time = time;
        }
    }
}