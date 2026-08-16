package ru.dev.koramikon.wtime.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import ru.dev.koramikon.wtime.WTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabCompleter implements org.bukkit.command.TabCompleter {

    private final WTime plugin;
    private static final List<String> REASONS = Arrays.asList(
            "Превышение CPS", "Неправильная отдача", "Заступ за линию ворот", "Численное неравенство", "Залезание на голову", "Спам", "Некорректная форма", "Эффекты", "Неспортивное поведение", "/lay /sit /crawl", "Помеха сбрасыванию", "AutoClicker", "Неправильный gamemode", "Speed выше 1.0\n", "Использование читов", "Телепортации на матче"
    );
    private static final List<String> TIMES = Arrays.asList("1m", "2m", "3m", "4m", "5m", "10m");
    private static final List<String> SECONDS = Arrays.asList("10s", "15s", "20s", "30s", "45s", "60s");

    public TabCompleter(WTime plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player p)) return null;

        List<String> completions = new ArrayList<>();
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("htime2") || cmd.equals("ht2")) {
            handleWTimeTab(p, args, completions);
        } else if (cmd.equals("boat2")) {
            handleBoatTab(p, args, completions);
        }

        return completions;
    }

    private void handleWTimeTab(Player p, String[] args, List<String> completions) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();

            if (p.hasPermission("htime2.pos")) {
                suggestions.addAll(Arrays.asList("pos1", "pos2", "pos"));
            }

            if (p.hasPermission("htime2.match")) {
                suggestions.addAll(Arrays.asList("1", "2", "3", "pereriv", "history", "reload"));
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

            } else if (args[0].equalsIgnoreCase("history")) {
                StringUtil.copyPartialMatches(args[1], List.of("list"), completions);

            } else if (List.of("1", "2", "3").contains(args[0].toLowerCase())) {
                StringUtil.copyPartialMatches(args[1], TIMES, completions);

            } else if (Bukkit.getPlayerExact(args[0]) != null) {
                StringUtil.copyPartialMatches(args[1], REASONS, completions);
            }

        } else if (args.length == 3) {
            if (List.of("1", "2", "3").contains(args[0].toLowerCase())) {
                StringUtil.copyPartialMatches(args[2], List.of("start"), completions);

            } else if (args[0].equalsIgnoreCase("pereriv")) {
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

    private void handleBoatTab(Player p, String[] args, List<String> completions) {
        if (!p.hasPermission("htime2.boat")) return;

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0],
                    Arrays.asList("pos", "reset", "del", "clear"), completions);

        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("pos") || args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("del")) {
                List<String> numbers = new ArrayList<>();
                for (int i = 1; i <= 9; i++) numbers.add(String.valueOf(i));
                StringUtil.copyPartialMatches(args[1], numbers, completions);
            }
        }
    }
}