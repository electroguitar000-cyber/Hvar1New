package ru.example.autovar;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class VarCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(AutoVarHockey.getInstance().messageManager.getMessage("errors.only_players"));
            return true;
        }

        if (args.length == 0) {
            p.sendMessage(AutoVarHockey.getInstance().messageManager.getMessage("errors.need_help"));
            return true;
        }

        var plugin = AutoVarHockey.getInstance();

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(p);
            case "reload" -> {
                if (!p.hasPermission("fvar2.reload")) {
                    p.sendMessage(plugin.messageManager.getMessage("errors.only_op"));
                    return true;
                }
                handleReload(p, plugin);
            }
            case "status" -> {
                if (!p.hasPermission("fvar2.use")) {
                    p.sendMessage(plugin.messageManager.getMessage("errors.only_op"));
                    return true;
                }
                sendStatus(p, plugin);
            }
            case "l", "r" -> {
                if (!p.hasPermission("fvar2.use")) {
                    p.sendMessage(plugin.messageManager.getMessage("errors.only_op"));
                    return true;
                }
                handleGatePosOrDel(p, args, plugin);
            }
            case "stata" -> {
                if (!p.hasPermission("fvar2.use")) {
                    p.sendMessage(plugin.messageManager.getMessage("errors.only_op"));
                    return true;
                }
                handleStata(p, args, plugin);
            }
            case "on" -> {
                if (!p.hasPermission("fvar2.use")) {
                    p.sendMessage(plugin.messageManager.getMessage("errors.only_op"));
                    return true;
                }
                plugin.setGatesEnabled(true);
                p.sendMessage(plugin.messageManager.getMessage("gates.gates_enabled"));
            }
            case "off" -> {
                if (!p.hasPermission("fvar2.use")) {
                    p.sendMessage(plugin.messageManager.getMessage("errors.only_op"));
                    return true;
                }
                plugin.setGatesEnabled(false);
                p.sendMessage(plugin.messageManager.getMessage("gates.gates_disabled"));
            }
            default -> p.sendMessage(plugin.messageManager.getMessage("errors.unknown_command"));
        }
        return true;
    }

    private void handleReload(Player p, AutoVarHockey plugin) {
        if (!p.hasPermission("fvar2.reload")) {
            p.sendMessage(plugin.messageManager.getMessage("errors.only_op"));
            return;
        }
        try {
            plugin.reload();
            p.sendMessage(plugin.messageManager.getMessage("reload.success"));
        } catch (Exception e) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("error", e.getMessage());
            p.sendMessage(plugin.messageManager.getMessage("reload.error", placeholders));
        }
    }

    private void sendHelp(Player p) {
        AutoVarHockey plugin = AutoVarHockey.getInstance();

        p.sendMessage(plugin.messageManager.getMessage("help.header"));

        List<String> commands = plugin.messageManager.getMessageList("help.commands");
        for (String command : commands) {
            p.sendMessage(MessageManager.colorize(command));
        }

        p.sendMessage(plugin.messageManager.getMessage("help.footer"));
    }

    private void sendStatus(Player p, AutoVarHockey plugin) {
        String leftStatus = plugin.leftGate.hasBoth() ?
                plugin.messageManager.getMessage("status.ready") :
                plugin.messageManager.getMessage("status.not_ready");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("status", leftStatus);
        p.sendMessage(plugin.messageManager.getMessage("status.left_gates", placeholders));

        String rightStatus = plugin.rightGate.hasBoth() ?
                plugin.messageManager.getMessage("status.ready") :
                plugin.messageManager.getMessage("status.not_ready");
        placeholders.put("status", rightStatus);
        p.sendMessage(plugin.messageManager.getMessage("status.right_gates", placeholders));

        String detectStatus = plugin.areGatesEnabled() ?
                plugin.messageManager.getMessage("status.enabled") :
                plugin.messageManager.getMessage("status.disabled");
        placeholders.put("status", detectStatus);
        p.sendMessage(plugin.messageManager.getMessage("status.goal_detect", placeholders));
    }

    private void handleGatePosOrDel(Player p, String[] args, AutoVarHockey plugin) {
        String side = args[0].toUpperCase();
        GoalSide gate = side.equals("L") ? plugin.leftGate : plugin.rightGate;

        if (args.length < 2) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("side", side);
            p.sendMessage(plugin.messageManager.getMessage("gates.need_pos_or_del", placeholders));
            return;
        }

        if (args[1].equalsIgnoreCase("del")) {
            gate.delete();
            plugin.statsManager.clearAll();
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("side", side);
            p.sendMessage(plugin.messageManager.getMessage("gates.gates_deleted", placeholders));
            return;
        }

        if (args[1].equalsIgnoreCase("pos") && args.length == 3) {
            try {
                int num = Integer.parseInt(args[2]);
                if (num != 1 && num != 2) throw new Exception();
                Location loc = p.getLocation().clone();
                loc.setY(loc.getBlockY());
                if (num == 1) gate.setPos1(loc);
                else gate.setPos2(loc);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("num", String.valueOf(num));
                placeholders.put("side", side);
                p.sendMessage(plugin.messageManager.getMessage("gates.point_set", placeholders));
                if (gate.hasBoth()) {
                    p.sendMessage(plugin.messageManager.getMessage("gates.gates_ready", placeholders));
                }
            } catch (Exception e) {
                p.sendMessage(plugin.messageManager.getMessage("gates.only_1_or_2"));
            }
        }
    }

    private void handleStata(Player p, String[] args, AutoVarHockey plugin) {
        if (args.length == 1) {
            p.sendMessage(plugin.messageManager.getMessage("stata.commands_hint"));
            return;
        }

        if (args[1].equalsIgnoreCase("list")) {
            p.sendMessage(plugin.statsManager.getStatsList());
            return;
        }

        if (args[1].equalsIgnoreCase("add") && args.length >= 4) {
            String side = args[2].toUpperCase();
            if (!side.equals("L") && !side.equals("R")) {
                p.sendMessage(plugin.messageManager.getMessage("errors.need_side_lr"));
                return;
            }

            int added = 0;
            int notFound = 0;

            for (int i = 3; i < args.length; i++) {
                String nick = args[i];
                if (plugin.statsManager.addPlayerToTeam(side, nick)) {
                    added++;
                } else {
                    notFound++;
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("player", nick);
                    p.sendMessage(plugin.messageManager.getMessage("stata.player_not_found", placeholders));
                }
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("side", side);
            placeholders.put("count", String.valueOf(added));
            p.sendMessage(plugin.messageManager.getMessage("stata.added_to_team", placeholders));
            if (notFound > 0) {
                placeholders.put("count", String.valueOf(notFound));
                p.sendMessage(plugin.messageManager.getMessage("stata.not_found_players", placeholders));
            }
            return;
        }

        if (args.length == 4 && (args[2].equalsIgnoreCase("green") || args[2].equalsIgnoreCase("red"))) {
            String side = args[1].toUpperCase();
            if (!side.equals("L") && !side.equals("R")) {
                p.sendMessage(plugin.messageManager.getMessage("errors.need_side_lr"));
                return;
            }
            String color = args[2].toLowerCase();
            String nick = args[3];
            plugin.statsManager.setPlayerColor(side, nick, color);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", nick);
            placeholders.put("color", color.equals("green") ?
                    plugin.messageManager.getMessage("stata.color_green") :
                    plugin.messageManager.getMessage("stata.color_red"));
            placeholders.put("side", side);
            p.sendMessage(plugin.messageManager.getMessage("stata.player_color_changed", placeholders));
            return;
        }

        if (args.length >= 3 && (args[1].equalsIgnoreCase("L") || args[1].equalsIgnoreCase("R"))) {
            String side = args[1].toUpperCase();
            String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            plugin.statsManager.setTeamName(side, name);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("side", side);
            placeholders.put("name", name);
            p.sendMessage(plugin.messageManager.getMessage("stata.team_name_changed", placeholders));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "canel" -> {
                plugin.cancelLastGoal();
                p.sendMessage(plugin.messageManager.getMessage("stata.goal_canceled"));
            }
            case "+gol" -> changeStat(p, args, 2, true, true, plugin);
            case "-gol" -> changeStat(p, args, 2, true, false, plugin);
            case "+pas" -> changeStat(p, args, 2, false, true, plugin);
            case "-pas" -> changeStat(p, args, 2, false, false, plugin);
            case "del" -> {
                // Новый формат: /fvar2 stata del L/R ник
                if (args.length < 4) {
                    p.sendMessage("§c/Fvar2 stata del L/R ник");
                    return;
                }
                String side = args[2].toUpperCase();
                if (!side.equals("L") && !side.equals("R")) {
                    p.sendMessage(plugin.messageManager.getMessage("errors.need_side_lr"));
                    return;
                }
                String nick = args[3];
                OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(nick);
                if (target == null || !target.hasPlayedBefore()) {
                    p.sendMessage(plugin.messageManager.getMessage("errors.player_not_found"));
                    return;
                }

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", target.getName());
                placeholders.put("team", plugin.statsManager.getTeamName(side));

                if (plugin.statsManager.removePlayerFromTeam(side, target.getName())) {
                    p.sendMessage(plugin.messageManager.getMessage("stata.player_removed", placeholders));
                } else {
                    p.sendMessage(plugin.messageManager.getMessage("stata.player_not_in_team"));
                }
            }
            default -> p.sendMessage(plugin.messageManager.getMessage("errors.invalid_command"));
        }
    }

    private void changeStat(Player sender, String[] args, int index, boolean isGoal, boolean add, AutoVarHockey plugin) {
        if (args.length < index + 1) {
            sender.sendMessage(plugin.messageManager.getMessage("errors.need_nick"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[index]);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(plugin.messageManager.getMessage("errors.player_not_found"));
            return;
        }

        String type = isGoal ?
                plugin.messageManager.getMessage("stata.stat_type_goal") :
                plugin.messageManager.getMessage("stata.stat_type_assist");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("type", type);
        placeholders.put("player", target.getName());

        if (add) {
            if (isGoal) plugin.statsManager.addGoal(target.getUniqueId());
            else plugin.statsManager.addAssist(target.getUniqueId());
            sender.sendMessage(plugin.messageManager.getMessage("stata.stat_changed_add", placeholders));
        } else {
            if (isGoal) plugin.statsManager.removeGoal(target.getUniqueId());
            else plugin.statsManager.removeAssist(target.getUniqueId());
            sender.sendMessage(plugin.messageManager.getMessage("stata.stat_changed_remove", placeholders));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("help");
            completions.add("reload");
            completions.add("status");
            completions.add("l");
            completions.add("r");
            completions.add("stata");
            completions.add("on");
            completions.add("off");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("stata")) {
            completions.add("list");
            completions.add("add");
            completions.add("del");
            completions.add("canel");
            completions.add("L");
            completions.add("R");
            completions.add("+gol");
            completions.add("-gol");
            completions.add("+pas");
            completions.add("-pas");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("stata") && args[1].equalsIgnoreCase("add")) {
            completions.add("L");
            completions.add("R");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("stata") && args[1].equalsIgnoreCase("del")) {
            completions.add("L");
            completions.add("R");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("stata") && args[1].equalsIgnoreCase("del") && (args[2].equalsIgnoreCase("L") || args[2].equalsIgnoreCase("R"))) {
            // Табкомплит для ников при удалении
            String side = args[2].toUpperCase();
            for (UUID uuid : AutoVarHockey.getInstance().statsManager.getTeamPlayers(side)) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                if (op.getName() != null) {
                    completions.add(op.getName());
                }
            }
        } else if (args.length >= 4 && args[0].equalsIgnoreCase("stata") && args[1].equalsIgnoreCase("add") && (args[2].equalsIgnoreCase("L") || args[2].equalsIgnoreCase("R"))) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                completions.add(online.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("stata") && (args[1].equalsIgnoreCase("L") || args[1].equalsIgnoreCase("R"))) {
            completions.add("green");
            completions.add("red");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("stata") && (args[1].equalsIgnoreCase("L") || args[1].equalsIgnoreCase("R")) && (args[2].equalsIgnoreCase("green") || args[2].equalsIgnoreCase("red"))) {
            String side = args[1].toUpperCase();
            for (UUID uuid : AutoVarHockey.getInstance().statsManager.getTeamPlayers(side)) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                if (op.getName() != null) {
                    completions.add(op.getName());
                }
            }
        }

        return completions;
    }
}