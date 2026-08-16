package dev.koramikon.dontmove;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final DontMove plugin;

    public CommandHandler(DontMove plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда доступна только игрокам.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("help"));
            return true;
        }

        String arg1 = args[0].toLowerCase();

        if (arg1.equals("help")) {
            player.sendMessage(plugin.getMessageManager().getMessage("help"));
            return true;
        }

        if (arg1.equals("status")) {
            boolean enabled = plugin.isBlockingEnabled();
            String status = enabled ? "&#55FF55&lВКЛЮЧЕНА" : "&#FF5555&lВЫКЛЮЧЕНА";
            player.sendMessage(plugin.getMessageManager().getMessage("status", "status", status));
            return true;
        }

        if (arg1.equals("on") || arg1.equals("off")) {
            boolean enable = arg1.equals("on");
            plugin.setBlockingEnabled(enable);
            String key = enable ? "mode-on" : "mode-off";
            player.sendMessage(plugin.getMessageManager().getMessage(key));
            String status = enable ? "&#55FF55&lВКЛЮЧЕНА" : "&#FF5555&lВЫКЛЮЧЕНА";
            player.sendMessage(plugin.getMessageManager().getMessage("status", "status", status));
            return true;
        }

        if (args.length == 2) {
            try {
                int regionIndex = Integer.parseInt(arg1);
                if (regionIndex < 1 || regionIndex > 4) {
                    player.sendMessage(plugin.getMessageManager().getMessage("invalid-number"));
                    return true;
                }
                String posType = args[1].toLowerCase();
                if (!posType.equals("pos1") && !posType.equals("pos2")) {
                    player.sendMessage(plugin.getMessageManager().getMessage("invalid-arg"));
                    return true;
                }

                Location blockLoc = player.getLocation().subtract(0, 1, 0).getBlock().getLocation();
                Region region = plugin.getRegion(regionIndex - 1);
                if (posType.equals("pos1")) {
                    region.setPos1(blockLoc);
                } else {
                    region.setPos2(blockLoc);
                }
                plugin.saveConfig();

                String posName = posType.toUpperCase();
                String locStr = blockLoc.getBlockX() + ", " + blockLoc.getBlockY() + ", " + blockLoc.getBlockZ();
                String msg = plugin.getMessageManager().getMessage("pos-set",
                        "region", String.valueOf(regionIndex),
                        "pos", posName,
                        "location", locStr);
                player.sendMessage(msg);
                return true;

            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessageManager().getMessage("invalid-arg"));
                return true;
            }
        }

        player.sendMessage(plugin.getMessageManager().getMessage("invalid-arg"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String arg = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>(Arrays.asList("help", "on", "off", "status", "1", "2", "3", "4"));
            List<String> result = new ArrayList<>();
            for (String s : suggestions) {
                if (s.startsWith(arg)) result.add(s);
            }
            return result;
        } else if (args.length == 2) {
            String arg1 = args[0];
            String arg2 = args[1].toLowerCase();
            if (arg1.matches("[1-4]")) {
                List<String> pos = Arrays.asList("pos1", "pos2");
                List<String> result = new ArrayList<>();
                for (String s : pos) {
                    if (s.startsWith(arg2)) result.add(s);
                }
                return result;
            }
        }
        return new ArrayList<>();
    }
}