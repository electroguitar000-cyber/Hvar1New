package ru.example.autovar;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;

public class ClickCommand implements CommandExecutor {

    private final Map<String, Boolean> viewers = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(AutoVarHockey.getInstance().messageManager.getMessage("errors.only_players"));
            return true;
        }

        // Проверка на право использовать команду
        if (!p.hasPermission("fvar1.use")) {
            p.sendMessage(AutoVarHockey.getInstance().messageManager.getMessage("errors.only_op"));
            return true;
        }

        String uuid = p.getUniqueId().toString();
        boolean current = viewers.getOrDefault(uuid, false);
        viewers.put(uuid, !current);

        String message = current ?
                AutoVarHockey.getInstance().messageManager.getMessage("cps.disabled") :
                AutoVarHockey.getInstance().messageManager.getMessage("cps.enabled");

        p.sendMessage(message);
        return true;
    }

    public boolean isViewer(Player p) {
        return viewers.getOrDefault(p.getUniqueId().toString(), false);
    }
}