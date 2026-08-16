package ru.dev.koramikon.ftime.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.dev.koramikon.ftime.FTime;
import ru.dev.koramikon.ftime.timer.BreakTimer;
import ru.dev.koramikon.ftime.timer.MatchTimer;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FTimeCommand implements CommandExecutor, TabCompleter {

    private final FTime plugin;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FTimeCommand(FTime plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.sendMessage(sender, plugin.getMessage("common.player-only"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help":
                sendHelp(player);
                return true;
            case "reload":
                return handleReload(player);
            case "1":
            case "2":
                return handlePeriod(player, args);
            case "del":
                return handleDelete(player);
            case "dop":
                return handleDop(player, args);
            case "penka":
                return handlePenka(player);
            case "time":
                return handleTime(player);
            case "status":
                return handleStatus(player, args);
            case "pereriv":
                return handleBreak(player, args);
            case "boat":
                return handleBoat(player, args);
            case "yellow":
            case "red":
                return handleCard(player, args, sub);
            case "history":
                return handleHistory(player, args);
            default:
                plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                        "usage", "/ftime help"));
                return true;
        }
    }

    private void sendHelp(Player player) {
        for (String line : plugin.getHelpMessages()) {
            plugin.sendMessage(player, line);
        }
    }

    private boolean handleReload(Player player) {
        if (!player.hasPermission("ftime.admin") && !player.isOp()) {
            plugin.sendMessage(player, plugin.getMessage("common.no-permission"));
            return true;
        }

        plugin.reloadConfig();
        plugin.sendMessage(player, plugin.getMessage("common.reloaded"));
        return true;
    }

    // НОВАЯ КОМАНДА: /ftime penka
    private boolean handlePenka(Player player) {
        if (!player.hasPermission("ftime.match") && !player.isOp()) {
            plugin.sendMessage(player, plugin.getMessage("common.no-permission"));
            return true;
        }

        MatchTimer timer = plugin.getMatchTimer();
        if (timer == null) {
            plugin.sendMessage(player, plugin.getMessage("match.not-started"));
            return true;
        }

        timer.setPenkaMode();
        return true;
    }

    // НОВАЯ КОМАНДА: /ftime time
    private boolean handleTime(Player player) {
        if (!player.hasPermission("ftime.match") && !player.isOp()) {
            plugin.sendMessage(player, plugin.getMessage("common.no-permission"));
            return true;
        }

        MatchTimer timer = plugin.getMatchTimer();
        if (timer == null) {
            plugin.sendMessage(player, plugin.getMessage("match.not-started"));
            return true;
        }

        timer.setTimeMode();
        return true;
    }

    private boolean handlePeriod(Player player, String[] args) {
        if (!player.hasPermission("ftime.match") && !player.isOp()) {
            plugin.sendMessage(player, plugin.getMessage("common.no-permission"));
            return true;
        }

        if (args.length < 2) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime " + args[0] + " <время>m"));
            return true;
        }

        int period = Integer.parseInt(args[0]);
        String timeArg = args[1].toLowerCase();

        if (!timeArg.endsWith("m")) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime " + period + " <время>m"));
            return true;
        }

        try {
            int minutes = Integer.parseInt(timeArg.replace("m", ""));
            if (minutes <= 0) throw new NumberFormatException();

            if (plugin.getBreakTimer() != null) {
                plugin.getBreakTimer().stop();
                plugin.setBreakTimer(null);
            }

            plugin.setBreakMode(false);
            MatchTimer timer = new MatchTimer(plugin, minutes * 60);
            timer.setStartLocation(player.getLocation());
            plugin.setMatchTimer(timer);
            plugin.setCurrentPeriod(period);
            timer.start();

            String message = plugin.getMessage("match.started",
                    "period", String.valueOf(period),
                    "minutes", String.valueOf(minutes));

            plugin.broadcastRadius(player.getLocation(), plugin.getMatchRadius(), message);

        } catch (NumberFormatException e) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime " + period + " <время>m"));
        }
        return true;
    }

    private boolean handleDelete(Player player) {
        if (!player.hasPermission("ftime.match") && !player.isOp()) {
            plugin.sendMessage(player, plugin.getMessage("common.no-permission"));
            return true;
        }

        boolean stopped = false;

        if (plugin.getMatchTimer() != null) {
            plugin.getMatchTimer().stop();
            plugin.setMatchTimer(null);
            stopped = true;
        }

        if (plugin.getBreakTimer() != null) {
            plugin.getBreakTimer().stop();
            plugin.setBreakTimer(null);
            stopped = true;
        }

        plugin.setCurrentPeriod(0);
        plugin.setBreakMode(false);

        if (stopped) {
            plugin.broadcastRadius(player.getLocation(), plugin.getMatchRadius(),
                    plugin.getMessage("match.force-ended"));
        } else {
            plugin.sendMessage(player, plugin.getMessage("match.not-started"));
        }
        return true;
    }

    private boolean handleDop(Player player, String[] args) {
        if (!player.hasPermission("ftime.match") && !player.isOp()) {
            plugin.sendMessage(player, plugin.getMessage("common.no-permission"));
            return true;
        }

        if (args.length < 2) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime dop <время>m / remove / set"));
            return true;
        }

        if (args[1].equalsIgnoreCase("remove")) {
            return handleDopRemove(player, args);
        } else if (args[1].equalsIgnoreCase("set")) {
            return handleDopSet(player, args);
        } else {
            return handleDopAdd(player, args);
        }
    }

    private boolean handleDopAdd(Player player, String[] args) {
        String timeArg = args[1].toLowerCase();
        if (!timeArg.endsWith("m")) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime dop <время>m"));
            return true;
        }

        try {
            int minutes = Integer.parseInt(timeArg.replace("m", ""));
            if (minutes <= 0) throw new NumberFormatException();

            if (plugin.getMatchTimer() != null) {
                plugin.getMatchTimer().addExtraTime(minutes * 60, player.getName());
            }

            plugin.sendMessage(player, plugin.getMessage("dop.added",
                    "minutes", String.valueOf(minutes)));

        } catch (NumberFormatException e) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime dop <время>m"));
        }
        return true;
    }

    private boolean handleDopRemove(Player player, String[] args) {
        if (args.length < 3) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime dop remove <время>m"));
            return true;
        }

        String timeArg = args[2].toLowerCase();
        if (!timeArg.endsWith("m")) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime dop remove <время>m"));
            return true;
        }

        try {
            int minutes = Integer.parseInt(timeArg.replace("m", ""));
            if (minutes <= 0) throw new NumberFormatException();

            if (plugin.getMatchTimer() != null) {
                plugin.getMatchTimer().removeExtraTime(minutes * 60);
            }

            plugin.sendMessage(player, plugin.getMessage("dop.removed",
                    "minutes", String.valueOf(minutes)));

        } catch (NumberFormatException e) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime dop remove <время>m"));
        }
        return true;
    }

    private boolean handleDopSet(Player player, String[] args) {
        if (args.length < 3) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime dop set <время>m"));
            return true;
        }

        String timeArg = args[2].toLowerCase();
        if (!timeArg.endsWith("m")) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime dop set <время>m"));
            return true;
        }

        try {
            int minutes = Integer.parseInt(timeArg.replace("m", ""));
            if (minutes < 0) throw new NumberFormatException();

            if (plugin.getMatchTimer() != null) {
                plugin.getMatchTimer().setExtraTime(minutes * 60, player.getName());
            }

            plugin.sendMessage(player, plugin.getMessage("dop.set",
                    "minutes", String.valueOf(minutes),
                    "player", player.getName()));

        } catch (NumberFormatException e) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime1 dop set <время>m"));
        }
        return true;
    }

    private boolean handleStatus(Player player, String[] args) {
        if (!player.hasPermission("ftime1.match") && !player.isOp()) {
            plugin.sendMessage(player, plugin.getMessage("common.no-permission"));
            return true;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("dop")) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime1 status dop"));
            return true;
        }

        MatchTimer timer = plugin.getMatchTimer();
        if (timer == null || !timer.hasExtraTime()) {
            plugin.sendMessage(player, plugin.getMessage("dop.no-data"));
            return true;
        }

        int minutes = timer.getExtraSeconds() / 60;
        String date = ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timer.getExtraAddedTimestamp()),
                ZoneId.of("Europe/Moscow")
        ).format(DATE_FORMAT);

        plugin.sendMessage(player, plugin.getMessage("dop.status",
                "player", timer.getExtraAddedBy(),
                "minutes", String.valueOf(minutes),
                "date", date));
        return true;
    }

    private boolean handleBreak(Player player, String[] args) {
        if (!player.hasPermission("ftime1.match") && !player.isOp()) {
            plugin.sendMessage(player, plugin.getMessage("common.no-permission"));
            return true;
        }

        if (args.length == 1) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime1 pereriv <сек>s start / del"));
            return true;
        }

        if (args[1].equalsIgnoreCase("del")) {
            if (plugin.getBreakTimer() == null) {
                plugin.sendMessage(player, plugin.getMessage("break.not-running"));
                return true;
            }

            plugin.getBreakTimer().stop();
            plugin.setBreakTimer(null);
            plugin.setCurrentPeriod(0);

            plugin.broadcastRadius(player.getLocation(), plugin.getMatchRadius(),
                    plugin.getMessage("break.force-ended"));
            return true;
        }

        if (args.length < 3 || !args[2].equalsIgnoreCase("start")) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime1 pereriv <сек>s start"));
            return true;
        }

        String timeStr = args[1].toLowerCase();
        if (!timeStr.endsWith("s")) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime1 pereriv <сек>s start"));
            return true;
        }

        try {
            int seconds = Integer.parseInt(timeStr.replace("s", ""));

            if (plugin.getMatchTimer() != null) {
                plugin.getMatchTimer().stop();
                plugin.setMatchTimer(null);
            }

            plugin.setBreakMode(true);
            BreakTimer breakTimer = new BreakTimer(plugin, seconds);
            breakTimer.setStartLocation(player.getLocation());
            plugin.setBreakTimer(breakTimer);
            plugin.setCurrentPeriod(0);
            breakTimer.start();

            plugin.broadcastRadius(player.getLocation(), plugin.getMatchRadius(),
                    plugin.getMessage("break.started", "seconds", String.valueOf(seconds)));

        } catch (NumberFormatException e) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime1 pereriv <сек>s start"));
        }
        return true;
    }

    private boolean handleBoat(Player player, String[] args) {
        if (!player.hasPermission("ftime1.boat") && !player.isOp()) {
            plugin.sendMessage(player, plugin.getMessage("common.no-permission"));
            return true;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("clear")) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime1 boat clear"));
            return true;
        }

        int cleared = 0;
        double radius = plugin.getBoatRadius();

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof org.bukkit.entity.Boat) {
                entity.remove();
                cleared++;
            }
        }

        if (cleared == 0) {
            plugin.sendMessage(player, plugin.getMessage("boat.not-found",
                    "radius", String.valueOf((int) radius)));
        } else {
            plugin.sendMessage(player, plugin.getMessage("boat.cleared",
                    "count", String.valueOf(cleared),
                    "radius", String.valueOf((int) radius)));
        }
        return true;
    }

    private boolean handleCard(Player player, String[] args, String type) {
        if (!player.hasPermission("ftime1.card") && !player.isOp()) {
            plugin.sendMessage(player, plugin.getMessage("common.no-permission"));
            return true;
        }

        if (args.length < 2) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime1 " + type + " <ник> <причина...> или del"));
            return true;
        }

        String targetName = args[1];

        if (args.length == 3 && args[2].equalsIgnoreCase("del")) {
            plugin.getStorage().removeCard(targetName, type);
            String cardColor = plugin.getMessage("card." + type);
            plugin.sendMessage(player, plugin.getMessage("card.removed",
                    "color", cardColor,
                    "player", targetName));
            return true;
        }

        if (args.length < 3) {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime1 " + type + " <ник> <причина...>"));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
        if (target == null || target.getName() == null) {
            plugin.sendMessage(player, plugin.getMessage("card.player-not-found",
                    "player", targetName));
            return true;
        }

        plugin.getStorage().addCard(targetName, type, reason, player.getName());

        String cardColor = plugin.getMessage("card." + type);

        plugin.sendMessage(player, plugin.getMessage("card.issued",
                "color", cardColor,
                "player", targetName,
                "reason", reason));

        Player onlineTarget = Bukkit.getPlayer(targetName);
        if (onlineTarget != null && onlineTarget.isOnline()) {
            plugin.sendMessage(onlineTarget, plugin.getMessage("card.to-player",
                    "color", cardColor,
                    "reason", reason));
        }

        return true;
    }

    private boolean handleHistory(Player player, String[] args) {
        if (!player.hasPermission("ftime1.history") && !player.isOp()) {
            plugin.sendMessage(player, plugin.getMessage("common.no-permission"));
            return true;
        }

        if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase("list"))) {
            List<String> history = plugin.getHistory("cards");

            plugin.sendMessage(player, plugin.getMessage("history.header"));

            if (history.isEmpty()) {
                plugin.sendMessage(player, plugin.getMessage("history.empty"));
            } else {
                for (String entry : history) {
                    plugin.sendMessage(player, entry);
                }
            }
        } else {
            plugin.sendMessage(player, plugin.getMessage("common.invalid-syntax",
                    "usage", "/ftime1 history list"));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) return new ArrayList<>();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("help", "reload", "1", "2", "del", "dop", "status",
                    "pereriv", "boat", "yellow", "red", "history", "penka", "time"));
        } else if (args.length == 2) {
            String first = args[0].toLowerCase();
            switch (first) {
                case "1", "2" -> completions.addAll(List.of("5m", "10m", "15m", "30m", "45m", "60m", "90m"));
                case "dop" -> completions.addAll(List.of("remove", "set", "1m", "2m", "3m", "5m", "10m"));
                case "status" -> completions.add("dop");
                case "pereriv" -> completions.addAll(List.of("del", "30s", "60s", "90s", "120s", "180s"));
                case "boat" -> completions.add("clear");
                case "history" -> completions.add("list");
                case "yellow", "red" -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        completions.add(p.getName());
                    }
                }
            }
        } else if (args.length == 3) {
            String first = args[0].toLowerCase();
            if (first.equals("dop") && (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("set"))) {
                completions.addAll(List.of("1m", "2m", "3m", "5m", "10m"));
            } else if (first.equals("pereriv") && !args[1].equalsIgnoreCase("del")) {
                completions.add("start");
            }
        }

        String current = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(current))
                .sorted()
                .toList();
    }
}