package ru.dev.koramikon.hvar1new.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.dev.koramikon.hvar1new.Hvar1NewPlugin;
import ru.dev.koramikon.hvar1new.util.MessageUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ClickCommand implements CommandExecutor {

    private final Hvar1NewPlugin plugin;
    private final Set<UUID> viewers = new HashSet<>();

    public ClickCommand(Hvar1NewPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.component("only-player"));
            return true;
        }
        UUID uuid = player.getUniqueId();
        if (viewers.contains(uuid)) {
            viewers.remove(uuid);
            player.sendMessage(MessageUtil.component("cps-disabled"));
        } else {
            viewers.add(uuid);
            player.sendMessage(MessageUtil.component("cps-enabled"));
        }
        return true;
    }

    public boolean isViewer(Player player) {
        return viewers.contains(player.getUniqueId());
    }
}