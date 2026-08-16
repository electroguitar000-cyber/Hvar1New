package ru.koramikon.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ru.koramikon.WitherTPA;

public class TPReloadCommand implements CommandExecutor {

    private final WitherTPA plugin;

    public TPReloadCommand(WitherTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("withertpa.reload")) {
            plugin.getMessageUtils().sendMessage(sender, "no-permission");
            return true;
        }

        plugin.reloadConfig();
        plugin.getConfigManager().reload();
        plugin.getMessageUtils().sendMessage(sender, "reload-success");

        return true;
    }
}