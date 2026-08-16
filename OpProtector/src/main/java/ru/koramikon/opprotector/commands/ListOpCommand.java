package ru.koramikon.opprotector.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.koramikon.opprotector.OpProtector;
import ru.koramikon.opprotector.managers.ConfigManager;
import ru.koramikon.opprotector.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ListOpCommand implements CommandExecutor, TabCompleter {

    private final OpProtector plugin;
    private final ConfigManager configManager;
    private final MessageUtils messageUtils;

    public ListOpCommand(OpProtector plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageUtils = plugin.getMessageUtils();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // Базовая команда /listop (просмотр) - ТОЛЬКО ДЛЯ OP
        if (args.length == 0) {
            // Проверяем, является ли отправитель оператором (игрок с OP или консоль)
            if (!sender.isOp()) {
                messageUtils.sendMessage(sender, "no-permission");
                return true;
            }

            // Show list of allowed players
            List<String> allowedPlayers = configManager.getAllowedPlayers();

            messageUtils.sendMessage(sender, "listop-header");

            if (allowedPlayers.isEmpty()) {
                messageUtils.sendMessage(sender, "listop-empty");
            } else {
                for (String player : allowedPlayers) {
                    messageUtils.sendColoredMessage(sender, "&e- &f" + player);
                }
            }
            return true;
        }

        // Команды с аргументами
        if (args.length >= 2) {
            String action = args[0].toLowerCase();
            String targetPlayer = args[1];

            switch (action) {
                case "add":
                    // Проверяем специальный пермишен для add
                    if (!sender.hasPermission("opprotector.listop.add")) {
                        messageUtils.sendMessage(sender, "no-permission");
                        return true;
                    }

                    if (configManager.addPlayer(targetPlayer)) {
                        messageUtils.sendMessage(sender, "listop-added", "%player%", targetPlayer);
                    } else {
                        messageUtils.sendColoredMessage(sender, "&cИгрок " + targetPlayer + " уже в списке!");
                    }
                    break;

                case "del":
                case "remove":
                    // Проверяем специальный пермишен для del
                    if (!sender.hasPermission("opprotector.listop.del")) {
                        messageUtils.sendMessage(sender, "no-permission");
                        return true;
                    }

                    if (configManager.removePlayer(targetPlayer)) {
                        messageUtils.sendMessage(sender, "listop-removed", "%player%", targetPlayer);
                    } else {
                        messageUtils.sendMessage(sender, "listop-not-found", "%player%", targetPlayer);
                    }
                    break;

                default:
                    messageUtils.sendMessage(sender, "listop-usage");
                    break;
            }
            return true;
        }

        messageUtils.sendMessage(sender, "listop-usage");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // Для табкомплита тоже проверяем права
        if (!sender.isOp() && !sender.hasPermission("opprotector.listop.add") && !sender.hasPermission("opprotector.listop.del")) {
            return completions;
        }

        if (args.length == 1) {
            // Tab complete for first argument (add/del)
            List<String> actions = new ArrayList<>();

            // Показываем add только если есть пермишен
            if (sender.hasPermission("opprotector.listop.add")) {
                actions.add("add");
            }
            // Показываем del только если есть пермишен
            if (sender.hasPermission("opprotector.listop.del")) {
                actions.add("del");
            }

            // Filter based on what user typed
            String currentArg = args[0].toLowerCase();
            completions = actions.stream()
                    .filter(action -> action.startsWith(currentArg))
                    .collect(Collectors.toList());

        } else if (args.length == 2) {
            String action = args[0].toLowerCase();
            String currentArg = args[1].toLowerCase();

            if (action.equals("add") && sender.hasPermission("opprotector.listop.add")) {
                // Suggest online players for add
                completions = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(currentArg))
                        .collect(Collectors.toList());

            } else if ((action.equals("del") || action.equals("remove")) && sender.hasPermission("opprotector.listop.del")) {
                // Suggest players from allowed list for removal
                completions = configManager.getAllowedPlayers().stream()
                        .filter(name -> name.toLowerCase().startsWith(currentArg))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }
}