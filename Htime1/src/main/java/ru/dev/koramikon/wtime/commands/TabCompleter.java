package ru.dev.koramikon.wtime.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabCompleter implements org.bukkit.command.TabCompleter {

    private static final List<String> REASONS = Arrays.asList(
            "Превышение CPS", "Неправильная отдача", "Заступ за линию ворот", "Численное неравенство", "Залезание на голову", "Спам", "Некорректная форма", "Эффекты", "Неспортивное поведение", "/lay /sit /crawl", "Помеха сбрасыванию", "AutoClicker", "Неправильный gamemode", "Speed выше 1.0\n", "Использование читов", "Телепортации на матче"
    );
    private static final List<String> TIMES = Arrays.asList("1m", "2m", "3m", "4m", "5m", "10m", "15m", "20m", "30m");
    private static final List<String> SECONDS = Arrays.asList("10s", "15s", "20s", "25s", "30s", "45s", "60s", "120s", "180s", "300s");
    private static final List<String> TIME_FORMATS = Arrays.asList("1m", "5m", "10m", "15m", "20m", "15:00", "15:30");

    public TabCompleter() {}

    private int getArenaId(Command command) {
        String name = command.getName().toLowerCase();
        if (name.startsWith("htime")) {
            try { return Integer.parseInt(name.substring(5)); } catch (NumberFormatException e) { return -1; }
        }
        if (name.startsWith("boat")) {
            try { return Integer.parseInt(name.substring(4)); } catch (NumberFormatException e) { return -1; }
        }
        return -1;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player p)) return null;

        String cmdName = command.getName().toLowerCase();

        // === ГЛОБАЛЬНАЯ КОМАНДА /htime ===
        if (cmdName.equals("htime") || cmdName.equals("ht")) {
            if (args.length == 1) {
                return StringUtil.copyPartialMatches(args[0], List.of("reload"), new ArrayList<>());
            }
            return null;
        }

        int arenaId = getArenaId(command);
        if (arenaId < 1 || arenaId > 50) return null;

        List<String> completions = new ArrayList<>();

        if (cmdName.startsWith("htime") || cmdName.startsWith("ht")) {
            handleWTimeTab(p, arenaId, args, completions);
        } else if (cmdName.startsWith("boat") || cmdName.startsWith("b")) {
            handleBoatTab(p, arenaId, args, completions);
        }

        return completions;
    }

    private void handleWTimeTab(Player p, int arenaId, String[] args, List<String> completions) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            if (p.hasPermission("htime" + arenaId + ".pos")) {
                suggestions.addAll(Arrays.asList("pos1", "pos2", "pos"));
            }
            if (p.hasPermission("htime" + arenaId + ".match")) {
                suggestions.addAll(Arrays.asList("1", "2", "3", "pereriv", "bullit", "per")); // reload и timeout удалены
                for (Player player : Bukkit.getOnlinePlayers()) {
                    suggestions.add(player.getName());
                }
            }
            StringUtil.copyPartialMatches(args[0], suggestions, completions);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("pos")) {
                StringUtil.copyPartialMatches(args[1], List.of("del"), completions);
            } else if (args[0].equalsIgnoreCase("pereriv")) {
                StringUtil.copyPartialMatches(args[1], SECONDS, completions);
            } else if (List.of("1", "2", "3").contains(args[0].toLowerCase())) {
                StringUtil.copyPartialMatches(args[1], TIME_FORMATS, completions);
            } else if (Bukkit.getPlayerExact(args[0]) != null) {
                StringUtil.copyPartialMatches(args[1], REASONS, completions);
            }
        } else if (args.length == 3) {
            if (List.of("1", "2", "3").contains(args[0].toLowerCase())) {
                StringUtil.copyPartialMatches(args[2], List.of("start"), completions);
            } else if (Bukkit.getPlayerExact(args[0]) != null && args.length >= 3) {
                StringUtil.copyPartialMatches(args[args.length - 1], TIMES, completions);
            }
        } else if (args.length >= 4 && Bukkit.getPlayerExact(args[0]) != null) {
            if (args[args.length - 1].isEmpty()) {
                StringUtil.copyPartialMatches("", List.of("start"), completions);
            }
        }
    }

    private void handleBoatTab(Player p, int arenaId, String[] args, List<String> completions) {
        if (!p.hasPermission("htime" + arenaId + ".boat")) return;

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("pos", "reset", "del", "clear"), completions);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("pos") || args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("del")) {
                List<String> numbers = new ArrayList<>();
                for (int i = 1; i <= 9; i++) numbers.add(String.valueOf(i));
                StringUtil.copyPartialMatches(args[1], numbers, completions);
            }
        }
    }
}