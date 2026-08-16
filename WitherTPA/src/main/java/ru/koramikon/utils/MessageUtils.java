package ru.koramikon.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.koramikon.WitherTPA;

import java.util.regex.Pattern;

public class MessageUtils {

    private final WitherTPA plugin;
    private final Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageUtils(WitherTPA plugin) {
        this.plugin = plugin;
    }

    public String getMessage(String path) {
        return plugin.getConfigManager().getMessage(path);
    }

    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return message;
    }

    public Component format(String message) {
        String converted = hexPattern.matcher(message).replaceAll("<#$1>");
        converted = converted.replace("&l", "<bold>");
        converted = converted.replace("&r", "</bold>");
        converted = converted.replace("&0", "<black>");
        converted = converted.replace("&1", "<dark_blue>");
        converted = converted.replace("&2", "<dark_green>");
        converted = converted.replace("&3", "<dark_aqua>");
        converted = converted.replace("&4", "<dark_red>");
        converted = converted.replace("&5", "<dark_purple>");
        converted = converted.replace("&6", "<gold>");
        converted = converted.replace("&7", "<gray>");
        converted = converted.replace("&8", "<dark_gray>");
        converted = converted.replace("&9", "<blue>");
        converted = converted.replace("&a", "<green>");
        converted = converted.replace("&b", "<aqua>");
        converted = converted.replace("&c", "<red>");
        converted = converted.replace("&d", "<light_purple>");
        converted = converted.replace("&e", "<yellow>");
        converted = converted.replace("&f", "<white>");
        converted = converted.replace("&r", "<reset>");
        return miniMessage.deserialize(converted);
    }

    public void sendMessage(CommandSender sender, String path) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        sender.sendMessage(format(prefix + getMessage(path)));
    }

    public void sendMessage(CommandSender sender, String path, String... replacements) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        sender.sendMessage(format(prefix + getMessage(path, replacements)));
    }

    public void sendRawMessage(CommandSender sender, String message) {
        sender.sendMessage(format(message));
    }

    public void sendRawMessage(CommandSender sender, Component component) {
        sender.sendMessage(component);
    }

    public Component getButton(String text, String command, String buttonType) {
        String hover;
        if (buttonType.equalsIgnoreCase("accept")) {
            hover = plugin.getConfigManager().getMessage("accept-button-hover");
        } else {
            hover = plugin.getConfigManager().getMessage("deny-button-hover");
        }
        String button = "<click:run_command:'" + command + "'><hover:show_text:'" + hover + "'>" + text + "</hover></click>";
        return format(button);
    }

    public Component getPrefix() {
        return format(plugin.getConfig().getString("messages.prefix", ""));
    }

    public void sendTpNotice(Player teleported, Player target) {
        String message = plugin.getConfigManager().getMessage("tpnotice-message")
                .replace("{player}", teleported.getName())
                .replace("{target}", target.getName());

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (plugin.getPlayerSettingsManager().isTpNoticeEnabled(online.getUniqueId())) {
                online.sendMessage(format(message));
            }
        }
    }

    public void sendTpaNotice(Player teleported, Player target) {
        String message = plugin.getConfigManager().getMessage("tpanotice-message")
                .replace("{player}", teleported.getName())
                .replace("{target}", target.getName());

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (plugin.getPlayerSettingsManager().isTpaNoticeEnabled(online.getUniqueId())) {
                online.sendMessage(format(message));
            }
        }
    }
}