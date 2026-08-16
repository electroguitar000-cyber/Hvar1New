package ru.dev.mikikor.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ru.dev.mikikor.ZapretBlok;

public class ZapretCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Проверка прав для всех команд
        if (!sender.hasPermission("zapret.admin") && !sender.isOp()) {
            sender.sendMessage(ZapretBlok.getInstance().getConfigManager().getMessage("messages.no_permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                ZapretBlok.getInstance().getConfigManager().reload();
                sender.sendMessage(ZapretBlok.getInstance().getConfigManager().getMessage("messages.reload_success"));
                sender.sendMessage(ZapretBlok.getInstance().getConfigManager().getMessage("messages.blocked_info")
                        .replace("{place_count}", String.valueOf(ZapretBlok.getInstance().getConfigManager().getBlockedPlaceBlocks().size()))
                        .replace("{break_count}", String.valueOf(ZapretBlok.getInstance().getConfigManager().getBlockedBreakBlocks().size())));
                break;

            case "on":
                if (ZapretBlok.getInstance().isProtectionEnabled()) {
                    sender.sendMessage(ZapretBlok.getInstance().getConfigManager().getMessage("messages.already_enabled"));
                } else {
                    ZapretBlok.getInstance().setProtectionEnabled(true);
                    sender.sendMessage(ZapretBlok.getInstance().getConfigManager().getMessage("messages.enabled"));
                }
                break;

            case "off":
                if (!ZapretBlok.getInstance().isProtectionEnabled()) {
                    sender.sendMessage(ZapretBlok.getInstance().getConfigManager().getMessage("messages.already_disabled"));
                } else {
                    ZapretBlok.getInstance().setProtectionEnabled(false);
                    sender.sendMessage(ZapretBlok.getInstance().getConfigManager().getMessage("messages.disabled"));
                }
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ZapretBlok.getInstance().getConfigManager().getMessage("commands.help"));
    }
}