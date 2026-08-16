package ru.koramikon.opprotector.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import ru.koramikon.opprotector.OpProtector;

public class MessageUtils {

    private final OpProtector plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    public MessageUtils(OpProtector plugin) {
        this.plugin = plugin;
    }

    public void sendMessage(CommandSender sender, String path) {
        String message = plugin.getConfigManager().getMessage(path);
        sender.sendMessage(serializer.deserialize(message));
    }

    public void sendMessage(CommandSender sender, String path, String... replacements) {
        String message = plugin.getConfigManager().getMessage(path);

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }

        sender.sendMessage(serializer.deserialize(message));
    }

    public void sendColoredMessage(CommandSender sender, String message) {
        sender.sendMessage(serializer.deserialize(message));
    }

    public String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}