package ru.koramikon.opprotector.commands;

import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.koramikon.opprotector.OpProtector;
import ru.koramikon.opprotector.managers.ConfigManager;
import ru.koramikon.opprotector.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class LopCommand implements CommandExecutor, TabCompleter {

    private final OpProtector plugin;
    private final ConfigManager configManager;
    private final MessageUtils messageUtils;

    public LopCommand(OpProtector plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageUtils = plugin.getMessageUtils();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // Базовая команда /lop - ТОЛЬКО ДЛЯ OP
        if (args.length == 0) {
            // Проверяем, является ли отправитель оператором
            if (!sender.isOp()) {
                messageUtils.sendMessage(sender, "no-permission");
                return true;
            }

            // Show help
            messageUtils.sendMessage(sender, "lop-header");

            List<String> commands = configManager.getMessageList("lop-commands");
            for (String cmd : commands) {
                messageUtils.sendColoredMessage(sender, cmd);
            }
            return true;
        }

        // Команда /lop reload - требует специальный пермишен
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            // Проверяем специальный пермишен для reload
            if (!sender.hasPermission("opprotector.lop.reload")) {
                messageUtils.sendMessage(sender, "no-permission");
                return true;
            }

            plugin.reloadPlugin();
            messageUtils.sendMessage(sender, "lop-reloaded");
            return true;
        }

        messageUtils.sendColoredMessage(sender, "&cUnknown command. Use /lop");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String currentArg = args[0].toLowerCase();

            // Add reload только если есть специальный пермишен
            if (sender.hasPermission("opprotector.lop.reload")) {
                if ("reload".startsWith(currentArg)) {
                    completions.add("reload");
                }
            }

            return completions;
        }

        return new ArrayList<>();
    }
}