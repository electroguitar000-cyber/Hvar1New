package ru.koramikon.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.koramikon.WitherTPA;
import ru.koramikon.managers.TeleportManager;
import ru.koramikon.utils.ConfigManager;

public class TPACommand implements CommandExecutor {

    private final WitherTPA plugin;

    public TPACommand(WitherTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        // Без аргументов - показать помощь
        if (args.length == 0) {
            showTpaHelp(sender);
            return true;
        }

        if (!player.hasPermission("withertpa.tpa")) {
            plugin.getMessageUtils().sendMessage(player, "no-permission");
            return true;
        }

        // Handle /tpa off/on
        if (args.length == 1 && args[0].equalsIgnoreCase("off")) {
            handleToggle(player, true);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("on")) {
            handleToggle(player, false);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageUtils().sendMessage(player, "player-not-found");
            return true;
        }

        if (player.equals(target)) {
            plugin.getMessageUtils().sendMessage(player, "tpa-self");
            return true;
        }

        // Check cooldown
        if (plugin.getCooldownManager().hasCooldown(player.getUniqueId())) {
            int remaining = plugin.getCooldownManager().getRemainingCooldown(player.getUniqueId());
            plugin.getMessageUtils().sendMessage(player, "tpa-cooldown", "time", String.valueOf(remaining));
            return true;
        }

        // Check if target has auto-deny enabled
        if (plugin.getPlayerSettingsManager().isAutoDeny(target.getUniqueId())) {
            plugin.getMessageUtils().sendMessage(player, "tpa-target-off", "player", target.getName());
            return true;
        }

        // Create request
        plugin.getTeleportManager().createRequest(player, target, false);

        // Send messages
        plugin.getMessageUtils().sendMessage(player, "tpa-request-sent", "player", target.getName());

        // Send request message with buttons to target
        Component acceptButton = plugin.getMessageUtils().getButton(
                plugin.getMessageUtils().getMessage("accept-button"),
                "/tpaccept",
                "accept"
        );
        Component denyButton = plugin.getMessageUtils().getButton(
                plugin.getMessageUtils().getMessage("deny-button"),
                "/tpdeny",
                "deny"
        );

        Component requestMessage = plugin.getMessageUtils().format(
                plugin.getMessageUtils().getMessage("tpa-request-received", "player", player.getName())
        ).append(Component.text(" ")).append(acceptButton).append(Component.text(" ")).append(denyButton);

        plugin.getMessageUtils().sendRawMessage(target, requestMessage);

        return true;
    }

    private void handleToggle(Player player, boolean disable) {
        if (!player.hasPermission(disable ? "withertpa.off" : "withertpa.on")) {
            plugin.getMessageUtils().sendMessage(player, "no-permission");
            return;
        }

        plugin.getPlayerSettingsManager().setAutoDeny(player.getUniqueId(), disable);

        if (disable) {
            plugin.getMessageUtils().sendMessage(player, "tpa-off-enabled");
        } else {
            plugin.getMessageUtils().sendMessage(player, "tpa-off-disabled");
        }
    }

    private void showTpaHelp(CommandSender sender) {
        ConfigManager cfg = plugin.getConfigManager();
        plugin.getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tpa-header"));
        plugin.getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tpa"));
        if (sender.hasPermission("withertpa.off")) {
            plugin.getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tpa-off"));
        }
        if (sender.hasPermission("withertpa.on")) {
            plugin.getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tpa-on"));
        }
        plugin.getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("footer"));
    }
}