package ru.dev.koramikon.hvar1new.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import ru.dev.koramikon.hvar1new.Hvar1NewPlugin;
import ru.dev.koramikon.hvar1new.gate.GoalSide;
import ru.dev.koramikon.hvar1new.stats.StatsManager;
import ru.dev.koramikon.hvar1new.util.MessageUtil;

import java.util.*;

public class HvarCommand implements CommandExecutor, TabCompleter {

    private final Hvar1NewPlugin plugin;

    public HvarCommand(Hvar1NewPlugin plugin) {
        this.plugin = plugin;
    }

    private int getArenaId(Command command) {
        String name = command.getName().toLowerCase();
        if (name.equals("hvar1")) return 1;
        if (name.startsWith("hvar")) {
            try { return Integer.parseInt(name.substring(4)); } catch (NumberFormatException e) { return -1; }
        }
        if (name.equalsIgnoreCase("Hvar1")) return 1;
        return -1;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // ========== ГЛОБАЛЬНАЯ КОМАНДА /hvar reload ==========
        if (command.getName().equalsIgnoreCase("hvar") && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.isOp()) {
                sender.sendMessage(MessageUtil.component("no-permission"));
                return true;
            }
            plugin.reloadPlugin();
            sender.sendMessage(MessageUtil.component("reload-success"));
            return true;
        }

        int arenaId = getArenaId(command);
        if (arenaId < 1 || arenaId > 50) {
            sender.sendMessage(MessageUtil.component("hvar-invalid-number"));
            return true;
        }

        if (args.length > 0 && !args[0].equalsIgnoreCase("help") && !sender.isOp()) {
            sender.sendMessage(MessageUtil.component("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, arenaId);
            return true;
        }

        String sub = args[0].toLowerCase();

        // ========== ПЛЕЙСХОЛДЕР ==========
        if (sub.equals("placeholder") && args.length >= 2) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(MessageUtil.component("only-player"));
                return true;
            }
            String action = args[1].toLowerCase();
            if (action.equals("place")) {
                Location loc = p.getLocation();
                plugin.createPlaceholder(arenaId, loc);
                p.sendMessage(MessageUtil.fromColored("&aПлейсхолдер для арены " + arenaId + " установлен!"));
                return true;
            } else if (action.equals("delete")) {
                plugin.deletePlaceholder(arenaId);
                p.sendMessage(MessageUtil.fromColored("&cПлейсхолдер для арены " + arenaId + " удалён."));
                return true;
            } else {
                p.sendMessage(MessageUtil.component("invalid-placeholder-arg"));
                return true;
            }
        }

        // ========== СЕЙВЫ ==========
        if ((sub.equals("l") || sub.equals("r")) && args.length >= 2 && args[1].equalsIgnoreCase("save")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(MessageUtil.component("only-player"));
                return true;
            }
            handleSavePos(p, arenaId, sub.charAt(0), args);
            return true;
        }

        // ========== СТВОРЫ (новая команда без pos1) ==========
        if (sub.equals("stvor1") || sub.equals("stvor2")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(MessageUtil.component("only-player"));
                return true;
            }
            // Определяем сторону по имени команды (если команда заканчивается на l или r)
            char side = 'r';
            if (label.toLowerCase().contains("l")) {
                side = 'l';
            }
            handleStvorPos(p, arenaId, side, sub, args);
            return true;
        }

        // ========== СТВОРЫ (старый вариант для совместимости) ==========
        if ((sub.equals("l") || sub.equals("r")) && args.length >= 2) {
            String second = args[1].toLowerCase();
            if (second.equals("stvor1") || second.equals("stvor2")) {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(MessageUtil.component("only-player"));
                    return true;
                }
                handleStvorPos(p, arenaId, sub.charAt(0), second, args);
                return true;
            }
            if (second.equals("del")) {
                handleDel(sender, arenaId, sub.charAt(0));
                return true;
            }
        }

        // ========== ОСТАЛЬНЫЕ КОМАНДЫ ==========
        switch (sub) {
            case "help":
                sendHelp(sender, arenaId);
                break;
            case "on":
                plugin.gatesEnabledArr[arenaId] = true;
                sender.sendMessage(MessageUtil.component("gates-enabled-msg")
                        .replaceText(b -> b.matchLiteral("{arena}").replacement(String.valueOf(arenaId))));
                break;
            case "off":
                plugin.gatesEnabledArr[arenaId] = false;
                sender.sendMessage(MessageUtil.component("gates-disabled-msg")
                        .replaceText(b -> b.matchLiteral("{arena}").replacement(String.valueOf(arenaId))));
                break;
            case "status":
                sendStatus(sender, arenaId);
                break;
            case "l":
            case "r":
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(MessageUtil.component("only-player"));
                    return true;
                }
                if (args.length >= 2 && !args[1].equalsIgnoreCase("del") && !args[1].equalsIgnoreCase("stvor1") && !args[1].equalsIgnoreCase("stvor2") && !args[1].equalsIgnoreCase("save") && !args[1].equalsIgnoreCase("placeholder")) {
                    handleGatePos(p, arenaId, sub.charAt(0), args);
                }
                break;
            case "stata":
                handleStata(sender, arenaId, args);
                break;
            default:
                sender.sendMessage(MessageUtil.component("unknown-command"));
        }
        return true;
    }

    // ========== УСТАНОВКА СЕЙВОВ ==========
    private void handleSavePos(Player player, int arenaId, char side, String[] args) {
        if (args.length < 3 || !args[2].equalsIgnoreCase("pos1") && !args[2].equalsIgnoreCase("pos2")) {
            player.sendMessage(MessageUtil.component("invalid-pos-arg"));
            return;
        }
        String posArg = args[2].toLowerCase();
        GoalSide saveGoal = (side == 'l') ? plugin.saveL[arenaId] : plugin.saveR[arenaId];
        Location loc = player.getLocation().clone();
        loc.setY(loc.getBlockY());
        if (posArg.equals("pos1")) saveGoal.setPos1(loc);
        else saveGoal.setPos2(loc);
        String cfgPath = "arena" + arenaId + "." + (side == 'l' ? "saveL" : "saveR");
        saveGoal.saveToConfig(plugin.getConfig(), cfgPath);
        plugin.saveConfig();
        String msg = MessageUtil.get("save-set")
                .replace("{arena}", String.valueOf(arenaId))
                .replace("{side}", String.valueOf(side).toUpperCase())
                .replace("{pos}", posArg)
                .replace("{x}", String.format("%.1f", loc.getX()))
                .replace("{y}", String.format("%.1f", loc.getY()))
                .replace("{z}", String.format("%.1f", loc.getZ()));
        player.sendMessage(MessageUtil.fromColored(msg));
    }

    // ========== УДАЛЕНИЕ КОМАНДЫ ==========
    private void handleDel(CommandSender sender, int arenaId, char side) {
        String sideName = String.valueOf(side).toUpperCase();
        StatsManager stats = plugin.statsManagers[arenaId];

        GoalSide gate = (side == 'l') ? plugin.leftGates[arenaId] : plugin.rightGates[arenaId];
        gate.delete();
        gate.saveToConfig(plugin.getConfig(), "arena" + arenaId + "." + (side == 'l' ? "left" : "right"));

        if (side == 'l') {
            plugin.stvor1[arenaId].delete();
            plugin.stvor3[arenaId].delete();
            plugin.stvor1[arenaId].saveToConfig(plugin.getConfig(), "arena" + arenaId + ".stvor1");
            plugin.stvor3[arenaId].saveToConfig(plugin.getConfig(), "arena" + arenaId + ".stvor3");
            plugin.saveL[arenaId].delete();
            plugin.saveL[arenaId].saveToConfig(plugin.getConfig(), "arena" + arenaId + ".saveL");
        } else {
            plugin.stvor2[arenaId].delete();
            plugin.stvor4[arenaId].delete();
            plugin.stvor2[arenaId].saveToConfig(plugin.getConfig(), "arena" + arenaId + ".stvor2");
            plugin.stvor4[arenaId].saveToConfig(plugin.getConfig(), "arena" + arenaId + ".stvor4");
            plugin.saveR[arenaId].delete();
            plugin.saveR[arenaId].saveToConfig(plugin.getConfig(), "arena" + arenaId + ".saveR");
        }

        stats.resetTeam(sideName);
        plugin.saveConfig();
        String msg = MessageUtil.get("team-deleted").replace("{side}", sideName).replace("{arena}", String.valueOf(arenaId));
        sender.sendMessage(MessageUtil.fromColored(msg));
    }

    // ========== УСТАНОВКА СТВОРОВ ==========
    private void handleStvorPos(Player player, int arenaId, char side, String stvorType, String[] args) {
        // Убираем проверку на pos1, всегда используем текущую позицию игрока
        GoalSide stvor;
        if (side == 'l') {
            stvor = stvorType.equals("stvor1") ? plugin.stvor1[arenaId] : plugin.stvor3[arenaId];
        } else {
            stvor = stvorType.equals("stvor1") ? plugin.stvor2[arenaId] : plugin.stvor4[arenaId];
        }
        var loc = player.getLocation().clone();
        loc.setY(loc.getBlockY());
        stvor.setPos1(loc);
        String cfgKey = "arena" + arenaId + "." + (stvor == plugin.stvor1[arenaId] ? "stvor1" :
                (stvor == plugin.stvor2[arenaId] ? "stvor2" :
                 (stvor == plugin.stvor3[arenaId] ? "stvor3" : "stvor4")));
        stvor.saveToConfig(plugin.getConfig(), cfgKey);
        plugin.saveConfig();
        String msg = MessageUtil.get("stvor-set")
                .replace("{arena}", String.valueOf(arenaId))
                .replace("{side}", String.valueOf(side).toUpperCase())
                .replace("{stvor}", stvorType)
                .replace("{x}", String.format("%.1f", loc.getX()))
                .replace("{y}", String.format("%.1f", loc.getY()))
                .replace("{z}", String.format("%.1f", loc.getZ()));
        player.sendMessage(MessageUtil.fromColored(msg));
    }

    // ========== УСТАНОВКА ВОРОТ ==========
    private void handleGatePos(Player player, int arenaId, char side, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtil.component("invalid-pos-arg"));
            return;
        }
        String posArg = args[1].toLowerCase();
        if (!posArg.equals("pos1") && !posArg.equals("pos2")) {
            player.sendMessage(MessageUtil.component("invalid-pos-arg"));
            return;
        }
        GoalSide gate = (side == 'l') ? plugin.leftGates[arenaId] : plugin.rightGates[arenaId];
        String sideName = String.valueOf(side).toUpperCase();
        var loc = player.getLocation().clone();
        loc.setY(loc.getBlockY());
        if (posArg.equals("pos1")) gate.setPos1(loc);
        else gate.setPos2(loc);
        gate.saveToConfig(plugin.getConfig(), "arena" + arenaId + "." + (side == 'l' ? "left" : "right"));
        plugin.saveConfig();
        plugin.updateCenter(arenaId);
        String msg = MessageUtil.get("pos-set")
                .replace("{arena}", String.valueOf(arenaId))
                .replace("{side}", sideName)
                .replace("{pos}", posArg)
                .replace("{x}", String.format("%.1f", loc.getX()))
                .replace("{y}", String.format("%.1f", loc.getY()))
                .replace("{z}", String.format("%.1f", loc.getZ()));
        player.sendMessage(MessageUtil.fromColored(msg));
    }

    // ========== СТАТИСТИКА ==========
    private void handleStata(CommandSender sender, int arenaId, String[] args) {
        if (args.length < 2) {
            String help = MessageUtil.getRaw("stata-help").replace("{arena}", String.valueOf(arenaId));
            sender.sendMessage(MessageUtil.fromColored(MessageUtil.colorize(help)));
            return;
        }
        String sub = args[1].toLowerCase();
        StatsManager stats = plugin.statsManagers[arenaId];

        switch (sub) {
            case "list":
                sender.sendMessage(MessageUtil.fromColored(stats.getStatsList(arenaId)));
                break;
            case "clear":
                stats.clearPlayerStats();
                sender.sendMessage(MessageUtil.fromColored("&aСтатистика арены " + arenaId + " очищена (игроки сохранены)"));
                break;
            case "add":
                if (args.length < 4) {
                    String usage = MessageUtil.getRaw("stata-add-usage").replace("{arena}", String.valueOf(arenaId));
                    sender.sendMessage(MessageUtil.fromColored(MessageUtil.colorize(usage)));
                    return;
                }
                String side = args[2].toUpperCase();
                if (!side.equals("L") && !side.equals("R")) {
                    sender.sendMessage(MessageUtil.component("invalid-side"));
                    return;
                }
                int added = 0, notFound = 0;
                for (int i = 3; i < args.length; i++) {
                    if (stats.addPlayerToTeam(side, args[i])) added++;
                    else notFound++;
                }
                String addedMsg = MessageUtil.get("stata-added").replace("{side}", side).replace("{count}", String.valueOf(added));
                sender.sendMessage(MessageUtil.fromColored(addedMsg));
                if (notFound > 0) {
                    String notFoundMsg = MessageUtil.get("stata-notfound").replace("{count}", String.valueOf(notFound));
                    sender.sendMessage(MessageUtil.fromColored(notFoundMsg));
                }
                break;
            case "del":
                if (args.length < 4) {
                    String usage = MessageUtil.getRaw("stata-del-usage").replace("{arena}", String.valueOf(arenaId));
                    sender.sendMessage(MessageUtil.fromColored(MessageUtil.colorize(usage)));
                    return;
                }
                side = args[2].toUpperCase();
                if (!side.equals("L") && !side.equals("R")) {
                    sender.sendMessage(MessageUtil.component("invalid-side"));
                    return;
                }
                String name = args[3];
                if (stats.removePlayerFromTeam(side, name)) {
                    String removedMsg = MessageUtil.get("stata-removed").replace("{player}", name).replace("{side}", side);
                    sender.sendMessage(MessageUtil.fromColored(removedMsg));
                } else {
                    sender.sendMessage(MessageUtil.component("player-not-in-team"));
                }
                break;
            case "canel":
                plugin.cancelLastGoal(arenaId);
                break;
            case "+gol":
                changeStat(sender, stats, args, 2, true, true);
                break;
            case "-gol":
                changeStat(sender, stats, args, 2, true, false);
                break;
            case "+pas":
                changeStat(sender, stats, args, 2, false, true);
                break;
            case "-pas":
                changeStat(sender, stats, args, 2, false, false);
                break;
            default:
                if (args.length >= 3 && (args[1].equalsIgnoreCase("L") || args[1].equalsIgnoreCase("R"))) {
                    side = args[1].toUpperCase();
                    String teamName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    stats.setTeamName(side, teamName);
                    String teamMsg = MessageUtil.get("team-name-set").replace("{side}", side).replace("{name}", teamName);
                    sender.sendMessage(MessageUtil.fromColored(teamMsg));
                } else {
                    String help = MessageUtil.getRaw("stata-help").replace("{arena}", String.valueOf(arenaId));
                    sender.sendMessage(MessageUtil.fromColored(MessageUtil.colorize(help)));
                }
        }
    }

    private void changeStat(CommandSender sender, StatsManager stats, String[] args, int targetIndex, boolean isGoal, boolean add) {
        if (args.length < targetIndex + 1) {
            sender.sendMessage(MessageUtil.component("need-nick"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[targetIndex]);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(MessageUtil.component("player-not-found"));
            return;
        }
        String type = isGoal ? "гол" : "пас";
        if (add) {
            if (isGoal) stats.addGoal(target.getUniqueId());
            else stats.addAssist(target.getUniqueId());
            String msg = MessageUtil.get("stat-changed-add").replace("{type}", type).replace("{player}", target.getName());
            sender.sendMessage(MessageUtil.fromColored(msg));
        } else {
            if (isGoal) stats.removeGoal(target.getUniqueId());
            else stats.removeAssist(target.getUniqueId());
            String msg = MessageUtil.get("stat-changed-remove").replace("{type}", type).replace("{player}", target.getName());
            sender.sendMessage(MessageUtil.fromColored(msg));
        }
    }

    // ========== HELP ==========
    private void sendHelp(CommandSender sender, int arenaId) {
        String header = MessageUtil.colorize(MessageUtil.getRaw("help-header").replace("{arena}", String.valueOf(arenaId)));
        sender.sendMessage(MessageUtil.fromColored(header));
        for (String line : MessageUtil.getList("help-lines")) {
            String colored = MessageUtil.colorize(line.replace("{arena}", String.valueOf(arenaId)));
            sender.sendMessage(MessageUtil.fromColored(colored));
        }
        String footer = MessageUtil.colorize(MessageUtil.getRaw("help-footer"));
        sender.sendMessage(MessageUtil.fromColored(footer));
    }

    private void sendStatus(CommandSender sender, int arenaId) {
        String header = MessageUtil.colorize(MessageUtil.getRaw("status-header").replace("{arena}", String.valueOf(arenaId)));
        sender.sendMessage(MessageUtil.fromColored(header));
        sender.sendMessage(MessageUtil.component(plugin.rightGates[arenaId].hasBoth() ? "status-gate-r-ready" : "status-gate-r-notready"));
        sender.sendMessage(MessageUtil.component(plugin.leftGates[arenaId].hasBoth() ? "status-gate-l-ready" : "status-gate-l-notready"));
        sender.sendMessage(MessageUtil.component(plugin.gatesEnabledArr[arenaId] ? "status-gates-on" : "status-gates-off"));
    }

    // ========== TABCOMPLETE ==========
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Обрабатываем глобальную команду /hvar
        if (command.getName().equalsIgnoreCase("hvar")) {
            if (args.length == 1) {
                return filterStartingWith(args[0], Arrays.asList("reload"));
            }
            return Collections.emptyList();
        }

        if (!sender.isOp()) {
            return Collections.emptyList();
        }

        int arenaId = getArenaId(command);
        if (arenaId == -1) return Collections.emptyList();

        if (args.length == 1) {
            List<String> base = new ArrayList<>(Arrays.asList("help", "on", "off", "status", "l", "r", "stata", "placeholder", "stvor1", "stvor2"));
            return filterStartingWith(args[0], base);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("placeholder")) {
            return filterStartingWith(args[1], Arrays.asList("place", "delete"));
        }
        if (args.length == 2) {
            String first = args[0].toLowerCase();
            if (first.equals("l") || first.equals("r")) {
                return filterStartingWith(args[1], Arrays.asList("stvor1", "stvor2", "save", "pos1", "pos2", "del"));
            }
            if (first.equals("stata")) {
                return filterStartingWith(args[1], Arrays.asList("list", "add", "del", "canel", "clear", "+gol", "-gol", "+pas", "-pas", "L", "R"));
            }
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("l") || args[0].equalsIgnoreCase("r"))) {
            String second = args[1].toLowerCase();
            if (second.equals("stvor1") || second.equals("stvor2")) {
                return filterStartingWith(args[2], Collections.singletonList("pos1"));
            }
            if (second.equals("save")) {
                return filterStartingWith(args[2], Arrays.asList("pos1", "pos2"));
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("stata") && args[1].equalsIgnoreCase("add")) {
            String sideArg = args[2].toUpperCase();
            if (!sideArg.equals("L") && !sideArg.equals("R")) {
                return filterStartingWith(args[2], Arrays.asList("L", "R"));
            } else {
                StatsManager stats = plugin.statsManagers[arenaId];
                List<String> players = new ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    String team = stats.getTeamSide(online.getUniqueId());
                    if (team == null) {
                        players.add(online.getName());
                    }
                }
                return filterStartingWith(args[args.length - 1], players);
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("stata") && args[1].equalsIgnoreCase("del")) {
            return filterStartingWith(args[2], Arrays.asList("L", "R"));
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("stata") && args[1].equalsIgnoreCase("del") && (args[2].equalsIgnoreCase("L") || args[2].equalsIgnoreCase("R"))) {
            String side = args[2].toUpperCase();
            StatsManager stats = plugin.statsManagers[arenaId];
            List<String> players = new ArrayList<>();
            for (UUID uuid : stats.getTeamPlayers(side)) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                if (op.getName() != null) {
                    players.add(op.getName());
                }
            }
            return filterStartingWith(args[3], players);
        }
        return Collections.emptyList();
    }

    private List<String> filterStartingWith(String prefix, Collection<String> options) {
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(opt);
            }
        }
        return result;
    }
}