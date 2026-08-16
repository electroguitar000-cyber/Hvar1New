package ru.dev.koramikon.wtime.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.dev.koramikon.wtime.WTime;
import ru.dev.koramikon.wtime.data.MatchTimer;
import ru.dev.koramikon.wtime.data.PenaltyPlayer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class WTimeCommand implements CommandExecutor {

    private final WTime plugin;

    public WTimeCommand(WTime plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.getMessage("common.player-only"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "pos1" -> handlePos1(p);
            case "pos2" -> handlePos2(p);
            case "pos" -> handlePosDel(p, args);
            case "1", "2", "3" -> handlePeriod(p, Integer.parseInt(args[0]), args);
            case "pereriv" -> handleBreak(p, args);
            case "del" -> handleDelete(p);
            case "history" -> handleHistory(p, args);
            case "reload" -> handleReload(p);
            case "per" -> handlePer(p);
            case "bullit" -> handleBullit(p);
            default -> handlePenalty(p, args);
        }

        return true;
    }

    private boolean checkPerm(Player p, String perm) {
        String fullPerm = "htime2." + perm;
        if (!p.hasPermission(fullPerm) && !p.isOp()) {
            plugin.sendMessage(p, plugin.getMessage("common.no-permission"));
            return false;
        }
        return true;
    }

    private void handlePos1(Player p) {
        if (!checkPerm(p, "pos")) return;
        plugin.pos1 = p.getLocation();
        plugin.sendMessage(p, plugin.getMessage("pos.pos1-set"));
    }

    private void handlePos2(Player p) {
        if (!checkPerm(p, "pos")) return;
        plugin.pos2 = p.getLocation();
        plugin.sendMessage(p, plugin.getMessage("pos.pos2-set"));
    }

    private void handlePosDel(Player p, String[] args) {
        if (!checkPerm(p, "pos")) return;

        if (args.length == 2 && args[1].equalsIgnoreCase("del")) {
            plugin.pos1 = null;
            plugin.pos2 = null;
            plugin.sendMessage(p, plugin.getMessage("pos.pos-deleted"));
        } else {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax",
                    "{usage}", "/htime2 pos del"));
        }
    }

    private void handlePeriod(Player p, int period, String[] args) {
        if (!checkPerm(p, "match")) return;

        if (args.length != 3 || !args[2].equalsIgnoreCase("start")) {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax",
                    "{usage}", "/htime2 " + period + " <время>m start"));
            return;
        }

        String timeStr = args[1].toLowerCase();
        if (!timeStr.endsWith("m")) {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax",
                    "{usage}", "/htime2 " + period + " <время>m start"));
            return;
        }

        try {
            int minutes = Integer.parseInt(timeStr.replace("m", ""));

            if (plugin.breakTimer != null) {
                plugin.breakTimer.stop();
                plugin.breakTimer = null;
            }

            // Если запускаем 1 период - включаем автопилот
            if (period == 1 && !plugin.autoPilot) {
                plugin.autoPilot = true;
                plugin.currentAutoPeriod = 1;
            }

            plugin.isBreakMode = false;
            plugin.matchTimer = new MatchTimer(minutes * 60, false);
            plugin.currentPeriod = period;
            plugin.matchTimer.start();

            plugin.broadcastRadius(plugin.getMessage("match.started",
                    "{period}", String.valueOf(period),
                    "{minutes}", minutes + " минут"));

        } catch (NumberFormatException e) {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax",
                    "{usage}", "/htime2 " + period + " <время>m start"));
        }
    }

    private void handleBullit(Player p) {
        if (!checkPerm(p, "match")) return;

        plugin.bullitMode = true;
        plugin.timersRunning = false;

        plugin.broadcastRadius("&7BULLITMODE - on (пизда вратарю...)");
        plugin.sendMessage(p, "&8BULLITMODE - on");
    }

    private void handlePer(Player p) {
        if (!checkPerm(p,"match")) return;

        plugin.bullitMode = false;

        //Проверям наличие лодки чтобы восстановить timersRunning
        if (plugin.pos1 != null) {
            boolean hasBoat = plugin.pos1.getWorld().getEntitiesByClass(org.bukkit.entity.Boat.class).stream()
                    .anyMatch(b -> b.getLocation().distance(plugin.pos1) <= 100);
            plugin.timersRunning = hasBoat;
        } else {
            plugin.timersRunning = true;
        }

        plugin.broadcastRadius("PERMODE - on");
        plugin.sendMessage(p,"PERMODE - on (время будет идти как обычно");
    }

    private void handleBreak(Player p, String[] args) {
        if (!checkPerm(p, "match")) return;

        if (args.length == 1) {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax",
                    "{usage}", "/htime2 pereriv <сек>s start / del"));
            return;
        }

        if (args[1].equalsIgnoreCase("del")) {
            if (plugin.breakTimer == null) {
                plugin.sendMessage(p, plugin.getMessage("break.not-running"));
                return;
            }

            plugin.breakTimer.stop();
            plugin.breakTimer = null;
            plugin.currentPeriod = 0;
            plugin.broadcastRadius(plugin.getMessage("break.force-ended"));
            return;
        }

        if (args.length != 3 || !args[2].equalsIgnoreCase("start")) {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax",
                    "{usage}", "/htime2 pereriv <сек>s start"));
            return;
        }

        String timeStr = args[1].toLowerCase();
        if (!timeStr.endsWith("s")) {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax",
                    "{usage}", "/htime2 pereriv <сек>s start"));
            return;
        }

        try {
            int seconds = Integer.parseInt(timeStr.replace("s", ""));

            if (plugin.matchTimer != null) {
                plugin.matchTimer.stop();
                plugin.matchTimer = null;
            }

            plugin.isBreakMode = true;
            plugin.breakTimer = new MatchTimer(seconds, true);
            plugin.currentPeriod = 0;
            plugin.breakTimer.start();

            plugin.broadcastRadius(plugin.getMessage("break.started",
                    "{seconds}", String.valueOf(seconds)));

        } catch (NumberFormatException e) {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax",
                    "{usage}", "/htime2 pereriv <сек>s start"));
        }
    }

    private void handleDelete(Player p) {
        if (!checkPerm(p, "match")) return;

        boolean stopped = false;

        if (plugin.matchTimer != null) {
            plugin.matchTimer.stop();
            plugin.matchTimer = null;
            stopped = true;
        }

        if (plugin.breakTimer != null) {
            plugin.breakTimer.stop();
            plugin.breakTimer = null;
            stopped = true;
        }

        plugin.currentPeriod = 0;

        if (stopped) {
            plugin.broadcastRadius(plugin.getMessage("match.force-ended"));
        } else {
            plugin.sendMessage(p, plugin.getMessage("match.not-started"));
        }
    }

    private void handlePenalty(Player p, String[] args) {
        if (!checkPerm(p, "match")) return;

        if (args.length == 2 && args[1].equalsIgnoreCase("del")) {
            removePenalty(p, args[0]);
            return;
        }

        if (args.length >= 4 && args[args.length - 1].equalsIgnoreCase("start")) {
            addPenalty(p, args);
            return;
        }

        sendHelp(p);
    }

    private void addPenalty(Player p, String[] args) {
        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);

        if (target == null || target.getName() == null) {
            plugin.sendMessage(p, plugin.getMessage("penalty.player-not-found",
                    "{player}", targetName));
            return;
        }

        String[] reasonParts = Arrays.copyOfRange(args, 1, args.length - 2);
        String reason = String.join(" ", reasonParts);
        String timeStr = args[args.length - 2];

        if (!timeStr.endsWith("m")) {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax",
                    "{usage}", "/htime2 <ник> <причина> <время>m start"));
            return;
        }

        try {
            int minutes = Integer.parseInt(timeStr.replace("m", ""));

            PenaltyPlayer pp = new PenaltyPlayer(target.getUniqueId(), reason, minutes, plugin);
            plugin.penalties.put(target.getUniqueId(), pp);
            pp.start();

            String name = target.getName() != null ? target.getName() : targetName;
            plugin.broadcastRadius(plugin.getMessage("penalty.added",
                    "{player}", name,
                    "{reason}", reason,
                    "{minutes}", String.valueOf(minutes)));

            plugin.addPenaltyToHistory(name, reason, minutes);

        } catch (NumberFormatException e) {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax",
                    "{usage}", "/htime2 <ник> <причина> <время>m start"));
        }
    }

    private void removePenalty(Player p, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);

        if (target == null || target.getName() == null) {
            plugin.sendMessage(p, plugin.getMessage("penalty.player-not-found",
                    "{player}", targetName));
            return;
        }

        PenaltyPlayer pp = plugin.penalties.remove(target.getUniqueId());

        if (pp == null) {
            plugin.sendMessage(p, plugin.getMessage("penalty.not-found",
                    "{player}", target.getName()));
            return;
        }

        pp.remainingSeconds = 0;
        String playerName = target.getName() != null ? target.getName() : targetName;
        plugin.broadcastRadius(plugin.getMessage("penalty.removed",
                "{player}", playerName));
    }

    private void handleHistory(Player p, String[] args) {
        if (!checkPerm(p, "match")) return;

        if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase("list"))) {
            List<String> history = plugin.getLast10History();

            if (history.isEmpty()) {
                plugin.sendMessage(p, plugin.getMessage("penalty.history-empty"));
            } else {
                plugin.sendMessage(p, plugin.getMessage("penalty.history-header"));
                for (String entry : history) {
                    plugin.sendMessage(p, entry);
                }
            }
        } else {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax",
                    "{usage}", "/htime2 history list"));
        }
    }

    private void handleReload(Player p) {
        if (!checkPerm(p, "match")) return;

        plugin.reloadConfig();
        plugin.getLanguageManager().loadMessages();
        plugin.sendMessage(p, plugin.getMessage("common.reloaded"));
    }

    private void sendHelp(Player p) {
        for (String line : plugin.getMessageList("help.main")) {
            plugin.sendMessage(p, line);
        }
    }
}