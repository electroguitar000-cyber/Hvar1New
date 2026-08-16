package ru.koramikon.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.koramikon.WitherTPA;

public class TpaNoticeCommand implements CommandExecutor {

    private final WitherTPA plugin;

    public TpaNoticeCommand(WitherTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (!player.hasPermission("withertpa.notice")) {
            plugin.getMessageUtils().sendMessage(player, "no-permission");
            return true;
        }

        if (args.length != 1) {
            plugin.getMessageUtils().sendMessage(player, "invalid-usage");
            plugin.getMessageUtils().sendRawMessage(player, plugin.getConfigManager().getMessage("tpanotice-usage"));
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            plugin.getPlayerSettingsManager().setTpaNoticeEnabled(player.getUniqueId(), true);
            plugin.getMessageUtils().sendRawMessage(player, plugin.getConfigManager().getMessage("tpanotice-on"));
            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {
            plugin.getPlayerSettingsManager().setTpaNoticeEnabled(player.getUniqueId(), false);
            plugin.getMessageUtils().sendRawMessage(player, plugin.getConfigManager().getMessage("tpanotice-off"));
            return true;
        }

        plugin.getMessageUtils().sendMessage(player, "invalid-usage");
        plugin.getMessageUtils().sendRawMessage(player, plugin.getConfigManager().getMessage("tpanotice-usage"));
        return true;
    }
}