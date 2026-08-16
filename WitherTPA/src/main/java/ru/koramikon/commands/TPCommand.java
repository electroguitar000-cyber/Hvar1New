package ru.koramikon.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.koramikon.WitherTPA;
import ru.koramikon.utils.ConfigManager;

public class TPCommand implements CommandExecutor {

    private final WitherTPA plugin;

    public TPCommand(WitherTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Без аргументов - показать помощь
        if (args.length == 0) {
            showTpHelp(sender);
            return true;
        }

        // Обработка /tp off <ник> и /tp on <ник>
        if (args.length == 2 && (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("on"))) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }
            return handlePlayerBlock(player, args[0].equalsIgnoreCase("off"), args[1]);
        }

        // Если первый аргумент - "off" или "on", но нет второго аргумента
        if (args.length == 1 && args[0].equalsIgnoreCase("off")) {
            plugin.getMessageUtils().sendMessage(sender, "invalid-usage");
            plugin.getMessageUtils().sendRawMessage(sender, plugin.getConfigManager().getMessage("tp-off-usage"));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("on")) {
            plugin.getMessageUtils().sendMessage(sender, "invalid-usage");
            plugin.getMessageUtils().sendRawMessage(sender, plugin.getConfigManager().getMessage("tp-on-usage"));
            return true;
        }

        // Проверка на права для использования команды
        if (!sender.hasPermission("withertpa.tp")) {
            plugin.getMessageUtils().sendMessage(sender, "no-permission");
            return true;
        }

        // /tp <игрок> - телепортироваться самому
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can teleport themselves!");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                plugin.getMessageUtils().sendMessage(sender, "player-not-found");
                return true;
            }

            if (player.equals(target)) {
                plugin.getMessageUtils().sendMessage(sender, "tp-self");
                return true;
            }

            // Проверка на блокировку от цели
            if (plugin.getPlayerBlockManager().isBlocked(target.getUniqueId(), player.getUniqueId())) {
                if (player.hasPermission("withertpa.tp.others")) {
                    plugin.getMessageUtils().sendRawMessage(player,
                            "&#FFAA55<bold>⚠</bold> <reset>&#FFAA55Игрок " + target.getName() +
                                    " запретил вам телепортацию, но вы имеете право обойти это! >:)");
                } else {
                    plugin.getMessageUtils().sendRawMessage(player,
                            "&#FF5555<bold>❌</bold> <reset>&#FF5555Игрок " + target.getName() +
                                    " запретил вам телепортироваться к себе!");
                    return true;
                }
            }

            player.teleport(target);
            plugin.getMessageUtils().sendMessage(player, "tp-success", "player", target.getName());

            // Отправка оповещения всем у кого включены tpnotice
            plugin.getMessageUtils().sendTpNotice(player, target);

            return true;
        }

        // /tp <игрок1> <игрок2> - телепортировать игрока1 к игроку2
        if (args.length == 2) {
            if (!sender.hasPermission("withertpa.tp.others")) {
                plugin.getMessageUtils().sendMessage(sender, "no-permission");
                return true;
            }

            Player player1 = Bukkit.getPlayer(args[0]);
            Player player2 = Bukkit.getPlayer(args[1]);

            if (player1 == null || player2 == null) {
                plugin.getMessageUtils().sendMessage(sender, "player-not-found");
                return true;
            }

            if (player1.equals(player2)) {
                plugin.getMessageUtils().sendMessage(sender, "tp-self");
                return true;
            }

            if (plugin.getPlayerBlockManager().isBlocked(player2.getUniqueId(), player1.getUniqueId())) {
                plugin.getMessageUtils().sendRawMessage(sender,
                        "&#FFAA55<bold>⚠</bold> <reset>&#FFAA55Игрок " + player2.getName() +
                                " заблокировал " + player1.getName() + ", но вы имеете право обойти это! >:)");
            }

            player1.teleport(player2);

            String senderName = sender instanceof Player ? ((Player) sender).getName() : "Console";
            plugin.getMessageUtils().sendMessage(sender, "tp-success-other",
                    "player1", player1.getName(),
                    "player2", player2.getName());

            if (!sender.equals(player1)) {
                plugin.getMessageUtils().sendRawMessage(player1,
                        "&#FFAA55<bold>➡</bold> <reset>&#FFAA55Вы были телепортированы к игроку " + player2.getName() + " игроком " + senderName);
            }

            // Отправка оповещения всем у кого включены tpnotice
            plugin.getMessageUtils().sendTpNotice(player1, player2);

            return true;
        }

        plugin.getMessageUtils().sendMessage(sender, "invalid-usage");
        showTpHelp(sender);
        return true;
    }

    private boolean handlePlayerBlock(Player player, boolean block, String targetName) {
        if (!player.hasPermission(block ? "withertpa.off" : "withertpa.on")) {
            plugin.getMessageUtils().sendMessage(player, "no-permission");
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            plugin.getMessageUtils().sendMessage(player, "player-not-found");
            return true;
        }

        if (player.equals(target)) {
            plugin.getMessageUtils().sendRawMessage(player,
                    "&#FF5555<bold>❌</bold> <reset>&#FF5555Вы не можете заблокировать самого себя!");
            return true;
        }

        if (block) {
            plugin.getPlayerBlockManager().blockPlayer(player.getUniqueId(), target.getUniqueId());
            plugin.getMessageUtils().sendRawMessage(player,
                    "&#FF5555<bold>🚫</bold> <reset>&#FF5555Вы запретили игроку " + target.getName() + " телепортироваться к вам!");
        } else {
            plugin.getPlayerBlockManager().unblockPlayer(player.getUniqueId(), target.getUniqueId());
            plugin.getMessageUtils().sendRawMessage(player,
                    "&#55FF55<bold>✅</bold> <reset>&#55FF55Вы разрешили игроку " + target.getName() + " телепортироваться к вам!");
        }

        return true;
    }

    private void showTpHelp(CommandSender sender) {
        ConfigManager cfg = plugin.getConfigManager();
        plugin.getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tp-header"));
        plugin.getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tp"));
        if (sender.hasPermission("withertpa.off")) {
            plugin.getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tp-off"));
        }
        if (sender.hasPermission("withertpa.on")) {
            plugin.getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tp-on"));
        }
        if (sender.hasPermission("withertpa.tp.others")) {
            plugin.getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tp-other"));
        }
        plugin.getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("footer"));
    }
}