package ru.example.autovar;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class AutoVarHockey extends JavaPlugin {

    private static AutoVarHockey instance;
    public GoalSide leftGate;
    public GoalSide rightGate;
    public StatsManager statsManager;
    public ClickCommand clickCommand;
    public ClickDetector clickDetector;
    public MessageManager messageManager;
    public boolean gatesEnabled = true;
    public static int gateDelayTicks = 8;  // static поле

    private final Deque<GoalHit> lastHits = new ArrayDeque<>(10);
    private GoalHit lastGoal = null;

    private FileConfiguration config;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        reloadConfig();
        config = getConfig();

        messageManager = new MessageManager(this);

        leftGate = new GoalSide();
        rightGate = new GoalSide();
        statsManager = new StatsManager(this);
        clickCommand = new ClickCommand();
        clickDetector = new ClickDetector(this);

        if (getCommand("Fvar2") != null) {
            getCommand("Fvar2").setExecutor(new VarCommand());
        }
        if (getCommand("Fclick2") != null) {
            getCommand("Fclick2").setExecutor(clickCommand);
        }

        getServer().getPluginManager().registerEvents(clickDetector, this);
        getServer().getPluginManager().registerEvents(new GoalListener(), this);

        leftGate.loadFromConfig(config, "gates.left");
        rightGate.loadFromConfig(config, "gates.right");
        statsManager.loadFromConfig(config);
        gatesEnabled = config.getBoolean("gates-enabled", true);
        gateDelayTicks = config.getInt("gate-delay-ticks", 8);

        getLogger().info("§aSqw +aura");
    }

    @Override
    public void onDisable() {
        leftGate.saveToConfig(config, "gates.left");
        rightGate.saveToConfig(config, "gates.right");
        statsManager.saveToConfig(config);
        config.set("gates-enabled", gatesEnabled);
        config.set("gate-delay-ticks", gateDelayTicks);
        saveConfig();

        getLogger().info("§cSqw -aura");
    }

    public void reload() {
        reloadConfig();
        config = getConfig();
        messageManager.reload();
        leftGate.loadFromConfig(config, "gates.left");
        rightGate.loadFromConfig(config, "gates.right");
        statsManager.loadFromConfig(config);
        gatesEnabled = config.getBoolean("gates-enabled", true);
        gateDelayTicks = config.getInt("gate-delay-ticks", 8);
    }

    public static AutoVarHockey getInstance() {
        return instance;
    }

    public static int getGateDelayTicks() {
        return gateDelayTicks;
    }

    public boolean areGatesEnabled() {
        return gatesEnabled;
    }

    public void setGatesEnabled(boolean enabled) {
        this.gatesEnabled = enabled;
    }

    public void registerHit(Player player) {
        lastHits.addFirst(new GoalHit(player.getUniqueId(), System.currentTimeMillis()));
        if (lastHits.size() > 10) lastHits.removeLast();
        //getLogger().info("Registered hit from: " + player.getName());
    }

    public void cancelLastGoal() {
        if (lastGoal == null) {
            getLogger().warning("No last goal to cancel!");
            return;
        }

        // Удаляем гол
        statsManager.removeGoal(lastGoal.getPlayer());
        getLogger().info("Removed goal from: " + lastGoal.getPlayer());

        // Удаляем пас если был
        if (lastGoal.getAssist() != null) {
            statsManager.removeAssist(lastGoal.getAssist());
            getLogger().info("Removed assist from: " + lastGoal.getAssist());
        }

        lastGoal = null;
        lastHits.clear();
    }

    public void handleGoal(Boat boat, String scoredAgainst) {
        String scoringTeam = scoredAgainst.equals("L") ? "R" : "L";
        Location loc = boat.getLocation();

        // Находим забившего и ассистента
        UUID goalScorer = null;
        UUID assist = null;

        for (GoalHit hit : lastHits) {
            String team = statsManager.getTeamSide(hit.getPlayer());
            if (team != null && team.equals(scoringTeam)) {
                if (goalScorer == null) {
                    goalScorer = hit.getPlayer();
                } else if (assist == null && !hit.getPlayer().equals(goalScorer)) {
                    assist = hit.getPlayer();
                    break;
                }
            }
        }

        // Сохраняем последний гол
        if (goalScorer != null) {
            lastGoal = new GoalHit(goalScorer, assist, System.currentTimeMillis());
        }

        // TITLES
        for (UUID uuid : statsManager.getTeamPlayers(scoringTeam)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.getWorld().equals(loc.getWorld()) && p.getLocation().distance(loc) <= 100) {
                p.sendTitle(messageManager.getMessage("goal.title_scored"), messageManager.getMessage("goal.subtitle"), 10, 70, 20);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
            }
        }

        for (UUID uuid : statsManager.getTeamPlayers(scoredAgainst)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.getWorld().equals(loc.getWorld()) && p.getLocation().distance(loc) <= 100) {
                p.sendTitle(messageManager.getMessage("goal.title_conceded"), messageManager.getMessage("goal.subtitle"), 10, 70, 20);
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
            }
        }

        String goalMsg = messageManager.getMessage("goal.message");
        String authorMsg = "";

        if (goalScorer != null) {
            Player scorer = Bukkit.getPlayer(goalScorer);
            String scorerName = scorer != null ? scorer.getName() : Bukkit.getOfflinePlayer(goalScorer).getName();
            statsManager.addGoal(goalScorer);

            String assistText = messageManager.getMessage("goal.assist_default");
            if (assist != null && !assist.equals(goalScorer)) {
                Player assistPlayer = Bukkit.getPlayer(assist);
                String assistName = assistPlayer != null ? assistPlayer.getName() : Bukkit.getOfflinePlayer(assist).getName();
                statsManager.addAssist(assist);
                assistText = messageManager.getMessage("goal.assist_format").replace("{player}", assistName);
            }

            authorMsg = messageManager.getMessage("goal.author")
                    .replace("{scorer}", scorerName)
                    .replace("{assist}", assistText);
        }

        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distance(loc) <= 100) {
                p.sendMessage(goalMsg);
                if (!authorMsg.isEmpty()) {
                    p.sendMessage(authorMsg);
                }
            }
        }

        lastHits.clear();

        loc.getWorld().strikeLightningEffect(loc);
        loc.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, loc, 200, 1, 1, 1, 0.1);
        loc.getWorld().playSound(loc, org.bukkit.Sound.BLOCK_CANDLE_EXTINGUISH, 10.0f, 1.0f);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (boat.isValid()) boat.remove();
        }, 20L);
    }

    // Внутренний класс для хранения информации о хите
    private static class GoalHit {
        private final UUID player;
        private final UUID assist;
        private final long time;

        GoalHit(UUID player, long time) {
            this.player = player;
            this.time = time;
            this.assist = null;
        }

        GoalHit(UUID scorer, UUID assist, long time) {
            this.player = scorer;
            this.assist = assist;
            this.time = time;
        }

        public UUID getPlayer() {
            return player;
        }

        public UUID getAssist() {
            return assist;
        }

        public long getTime() {
            return time;
        }
    }
}