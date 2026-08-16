package ru.dev.koramikon.wtime.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.dev.koramikon.wtime.WTime;

public class BoatCommand implements CommandExecutor {

    private final WTime plugin;

    public BoatCommand(WTime plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.getMessage("common.player-only"));
            return true;
        }

        if (!checkPerm(p)) return true;

        if (args.length == 0) {
            sendHelp(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "pos" -> handlePos(p, args);
            case "reset" -> handleReset(p, args);
            case "del" -> handleDel(p, args);
            case "clear" -> handleClear(p);
            default -> sendHelp(p);
        }

        return true;
    }

    private boolean checkPerm(Player p) {
        // Меняем wtime.boat на htime.boat
        if (!p.hasPermission("htime2.boat") && !p.isOp()) {
            plugin.sendMessage(p, plugin.getMessage("common.no-permission"));
            return false;
        }
        return true;
    }

    private void handlePos(Player p, String[] args) {
        if (args.length != 2 || !isNumber(args[1])) {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax",
                    "{usage}", "/boat2 pos <1-9>"));
            return;
        }

        int num = Integer.parseInt(args[1]);
        if (num < 1 || num > 9) {
            plugin.sendMessage(p, plugin.getMessage("boat.point-invalid"));
            return;
        }

        Location loc = p.getLocation().clone();
        loc.setYaw(90f);
        loc.setPitch(0f);

        plugin.getBoatPoints().put(num, loc);
        plugin.saveBoatPoints();

        plugin.sendMessage(p, plugin.getMessage("boat.point-set",
                "{number}", String.valueOf(num)));
    }

    private void handleReset(Player p, String[] args) {
        Location loc = null;
        int num = -1;

        if (args.length == 2 && isNumber(args[1])) {
            num = Integer.parseInt(args[1]);
            loc = plugin.getBoatPoints().get(num);

            if (loc == null) {
                plugin.sendMessage(p, plugin.getMessage("boat.point-not-exist",
                        "{number}", String.valueOf(num)));
                return;
            }
        } else {
            double bestDist = Double.MAX_VALUE;
            for (var entry : plugin.getBoatPoints().entrySet()) {
                double dist = p.getLocation().distance(entry.getValue());
                if (dist < bestDist) {
                    bestDist = dist;
                    loc = entry.getValue();
                    num = entry.getKey();
                }
            }

            if (loc == null) {
                plugin.sendMessage(p, plugin.getMessage("boat.point-not-found"));
                return;
            }
        }

        startEpicReset(p, loc, num);
    }

    private void handleDel(Player p, String[] args) {
        if (args.length == 1) {
            plugin.getBoatPoints().clear();
            plugin.saveBoatPoints();
            plugin.sendMessage(p, plugin.getMessage("boat.point-deleted-all"));

        } else if (args.length == 2 && isNumber(args[1])) {
            int num = Integer.parseInt(args[1]);

            if (plugin.getBoatPoints().remove(num) != null) {
                plugin.saveBoatPoints();
                plugin.sendMessage(p, plugin.getMessage("boat.point-deleted",
                        "{number}", String.valueOf(num)));
            } else {
                plugin.sendMessage(p, plugin.getMessage("boat.point-not-exist",
                        "{number}", String.valueOf(num)));
            }
        } else {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax",
                    "{usage}", "/boat2 del или /boat2 del <номер>"));
        }
    }

    private void handleClear(Player p) {
        if (plugin.pos1 == null) {
            plugin.sendMessage(p, plugin.getMessage("boat.clear-no-pos1"));
            return;
        }

        int removed = 0;
        for (var boat : plugin.pos1.getWorld().getEntitiesByClass(org.bukkit.entity.Boat.class)) {
            if (boat.getLocation().distance(plugin.pos1) <= 100) {
                boat.remove();
                removed++;
            }
        }

        String msg = plugin.getMessage("boat.clear-complete",
                "{count}", String.valueOf(removed));

        plugin.sendMessage(p, msg);

        for (var player : plugin.pos1.getWorld().getPlayers()) {
            if (player.getLocation().distance(plugin.pos1) <= 100) {
                plugin.sendMessage(player, msg);
            }
        }
    }

    private void startEpicReset(Player initiator, Location loc, int num) {
        var world = loc.getWorld();
        var nearby = world.getPlayers().stream()
                .filter(p -> p.getLocation().distance(loc) <= 70)
                .toList();

        plugin.sendMessage(initiator, plugin.getMessage("boat.reset-started",
                "{number}", String.valueOf(num)));

        if (plugin.getCurrentResetTask() != null && !plugin.getCurrentResetTask().isCancelled()) {
            plugin.getCurrentResetTask().cancel();
        }

        var task = new org.bukkit.scheduler.BukkitRunnable() {
            int countdown = 3;
            Location spawnLoc = loc.clone().add(0, 1, 0);

            @Override
            public void run() {
                if (countdown == 0) {
                    world.spawnEntity(loc, org.bukkit.entity.EntityType.DARK_OAK_BOAT);
                    world.spawnParticle(org.bukkit.Particle.EXPLOSION, loc, 15, 1.5, 1.5, 1.5, 0);
                    world.spawnParticle(org.bukkit.Particle.LARGE_SMOKE, loc, 30, 3, 3, 3, 0.1);
                    world.spawnParticle(org.bukkit.Particle.SNOWFLAKE, loc, 50, 2, 2, 2, 0);

                    String msg = plugin.getMessage("boat.reset-complete");
                    for (var p : nearby) {
                        plugin.sendMessage(p, msg);
                        p.playSound(loc, org.bukkit.Sound.ENTITY_GENERIC_SPLASH, 2f, 0.8f);
                    }

                    cancel();
                    return;
                }

                String msg = plugin.getMessage("boat.reset-countdown",
                        "{count}", String.valueOf(countdown));

                for (var p : nearby) {
                    plugin.sendMessage(p, msg);
                }

                org.bukkit.Sound sound;
                org.bukkit.Particle particle;

                switch (countdown) {
                    case 1 -> {
                        sound = org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING;
                        particle = org.bukkit.Particle.END_ROD;
                    }
                    case 2 -> {
                        sound = org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME;
                        particle = org.bukkit.Particle.ENCHANT;
                    }
                    default -> {
                        sound = org.bukkit.Sound.BLOCK_NOTE_BLOCK_HARP;
                        particle = org.bukkit.Particle.HAPPY_VILLAGER;
                    }
                }

                world.playSound(spawnLoc, sound, 1.5f, 0.5f);
                world.spawnParticle(particle, spawnLoc, countdown == 1 ? 4 : 5, 1, 1, 1, 0);
                world.spawnParticle(org.bukkit.Particle.ITEM_SNOWBALL, spawnLoc, 10, 1.5, 1.5, 1.5, 0.1);
                world.spawnParticle(org.bukkit.Particle.SNOWFLAKE, spawnLoc, 5, 1, 1, 1, 0);

                countdown--;
            }
        };

        task.runTaskTimer(plugin, 0L, 20L);
        plugin.setCurrentResetTask(task);
    }

    private boolean isNumber(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void sendHelp(Player p) {
        for (String line : plugin.getMessageList("help.boat")) {
            plugin.sendMessage(p, line);
        }
    }
}