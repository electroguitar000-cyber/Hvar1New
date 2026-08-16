package ru.koramikon.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.koramikon.WitherTPA;
import ru.koramikon.utils.ConfigManager;

public class TPHereCommand implements CommandExecutor {

    private final WitherTPA plugin;

    public TPHereCommand(WitherTPA plugin) {
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
            showTpaHereHelp(sender);
            return true;
        }

        if (!player.hasPermission("withertpa.tpahere")) {
            plugin.getMessageUtils().sendMessage(player, "no-permission");
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
        plugin.getTeleportManager().createRequest(player, target, true);

        // Send messages
        plugin.getMessageUtils().sendMessage(player, "tpahere-request-sent", "player", target.getName());

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
                plugin.getMessageUtils().getMessage("tpahere-request-received", "player", player.getName())
        ).append(Component.text(" ")).append(acceptButton).append(Component.text(" ")).append(denyButton);

        plugin.getMessageUtils().sendRawMessage(target, requestMessage);

        return true;
    }

    private void showTpaHereHelp(CommandSender sender) {
        ConfigManager cfg = plugin.getConfigManager();
        plugin.getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tpahere-header"));
        plugin.getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tpahere"));
        plugin.getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("footer"));
    }
}