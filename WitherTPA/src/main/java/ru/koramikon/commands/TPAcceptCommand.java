package ru.koramikon.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.koramikon.WitherTPA;
import ru.koramikon.managers.TeleportManager;

public class TPAcceptCommand implements CommandExecutor {

    private final WitherTPA plugin;

    public TPAcceptCommand(WitherTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (!player.hasPermission("withertpa.tpaccept")) {
            plugin.getMessageUtils().sendMessage(player, "no-permission");
            return true;
        }

        TeleportManager.TeleportRequest request = plugin.getTeleportManager().getRequestByTarget(player.getUniqueId());

        if (request == null) {
            plugin.getMessageUtils().sendMessage(player, "no-requests");
            return true;
        }

        plugin.getTeleportManager().acceptRequest(player.getUniqueId());

        return true;
    }
}