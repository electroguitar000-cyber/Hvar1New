package ru.dev.kisstymelusi.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.dev.kisstymelusi.PassportPlugin;
import ru.dev.kisstymelusi.utils.PassportTransferManager;

public class PassportCommand implements CommandExecutor {

    private final PassportPlugin plugin;

    public PassportCommand(PassportPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessageManager().getMessage("invalid-usage"));
            return true;
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "create":
                if (!sender.hasPermission("passport.create")) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("invalid-usage"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("player-not-found"));
                    return true;
                }
                plugin.getDialogManager().startDialog(target, sender);
                break;

            case "refresh":
                if (!sender.hasPermission("passport.refresh")) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Только для игроков.");
                    return true;
                }
                Player playerRefresh = (Player) sender;
                if (!plugin.getPassportManager().hasPassport(playerRefresh.getUniqueId())) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("no-passport"));
                    return true;
                }
                if (!plugin.getPassportManager().isExpired(playerRefresh.getUniqueId())) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("not-expired"));
                    return true;
                }
                // Удаляем старый паспорт из инвентаря
                plugin.removePassportFromInventory(playerRefresh);
                // Обновляем дату выдачи
                plugin.getPassportManager().renewPassport(playerRefresh.getUniqueId());
                // Выдаём новый
                plugin.giveBook(playerRefresh);
                sender.sendMessage(plugin.getMessageManager().getMessage("refresh-success"));
                break;

            case "open":
                if (!sender.hasPermission("passport.open")) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Только для игроков.");
                    return true;
                }
                Player playerOpen = (Player) sender;
                if (!plugin.getPassportManager().hasPassport(playerOpen.getUniqueId())) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("no-passport"));
                    return true;
                }
                plugin.openBook(playerOpen);
                sender.sendMessage(plugin.getMessageManager().getMessage("passport-opened"));
                break;

            case "reload":
                if (!sender.hasPermission("passport.reload")) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(plugin.getMessageManager().getMessage("reloaded"));
                break;

            case "give":
                if (!sender.hasPermission("passport.give")) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Только для игроков.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("&cИспользуйте: /passport give <игрок>");
                    return true;
                }
                Player giver = (Player) sender;
                Player receiver = Bukkit.getPlayer(args[1]);
                if (receiver == null) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("player-not-found"));
                    return true;
                }
                if (!plugin.getPassportManager().hasPassport(giver.getUniqueId())) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("no-passport"));
                    return true;
                }
                // Отправляем запрос
                plugin.getTransferManager().createRequest(giver, receiver);
                sender.sendMessage(plugin.getMessageManager().getMessage("give-request-sent")
                        .replace("{target}", receiver.getName()));

                // Отправляем получателю сообщение с кнопками
                String acceptCmd = "/passport accept " + giver.getName();
                String denyCmd = "/passport deny " + giver.getName();
                TextComponent message = new TextComponent(
                        plugin.getMessageManager().getMessage("give-request-received")
                                .replace("{player}", giver.getName()) + " ");
                TextComponent acceptBtn = new TextComponent("[Согласиться]");
                acceptBtn.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                acceptBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, acceptCmd));
                acceptBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("Нажмите, чтобы принять").create()));
                TextComponent denyBtn = new TextComponent(" [Отказаться]");
                denyBtn.setColor(net.md_5.bungee.api.ChatColor.RED);
                denyBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, denyCmd));
                denyBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("Нажмите, чтобы отказаться").create()));
                message.addExtra(acceptBtn);
                message.addExtra(denyBtn);
                receiver.spigot().sendMessage(message);
                break;

            case "accept":
                if (!sender.hasPermission("passport.accept")) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Только для игроков.");
                    return true;
                }
                Player accepter = (Player) sender;
                if (args.length >= 2) {
                    // Если передан ник отправителя
                    Player giverAccept = Bukkit.getPlayer(args[1]);
                    if (giverAccept == null) {
                        sender.sendMessage(plugin.getMessageManager().getMessage("player-not-found"));
                        return true;
                    }
                    if (plugin.getTransferManager().acceptRequest(accepter, giverAccept)) {
                        plugin.openBookFor(accepter, giverAccept.getUniqueId());
                        accepter.sendMessage(plugin.getMessageManager().getMessage("give-accept"));
                    } else {
                        accepter.sendMessage(plugin.getMessageManager().getMessage("give-no-request"));
                    }
                } else {
                    accepter.sendMessage("&cИспользуйте: /passport accept <ник_отправителя>");
                }
                break;

            case "deny":
                if (!sender.hasPermission("passport.deny")) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Только для игроков.");
                    return true;
                }
                Player denier = (Player) sender;
                if (args.length >= 2) {
                    Player giverDeny = Bukkit.getPlayer(args[1]);
                    if (giverDeny == null) {
                        sender.sendMessage(plugin.getMessageManager().getMessage("player-not-found"));
                        return true;
                    }
                    if (plugin.getTransferManager().denyRequest(denier, giverDeny)) {
                        denier.sendMessage(plugin.getMessageManager().getMessage("give-deny"));
                    } else {
                        denier.sendMessage(plugin.getMessageManager().getMessage("give-no-request"));
                    }
                } else {
                    denier.sendMessage("&cИспользуйте: /passport deny <ник_отправителя>");
                }
                break;

            case "edit":
                if (!sender.hasPermission("passport.edit")) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Только для игроков.");
                    return true;
                }
                Player editor = (Player) sender;
                if (!plugin.getPassportManager().hasPassport(editor.getUniqueId())) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("no-passport"));
                    return true;
                }
                // Запускаем диалог редактирования
                plugin.getDialogManager().startEditDialog(editor);
                break;

            default:
                sender.sendMessage(plugin.getMessageManager().getMessage("invalid-usage"));
                break;
        }
        return true;
    }
}