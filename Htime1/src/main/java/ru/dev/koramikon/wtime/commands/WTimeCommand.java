package ru.dev.koramikon.wtime.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.dev.koramikon.wtime.WTime;
import ru.dev.koramikon.wtime.WTime.TimerInstance;
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

    private int getArenaId(Command command) {
        String name = command.getName().toLowerCase();
        if (name.equals("htime1") || name.equals("ht1")) return 1;
        if (name.startsWith("htime")) {
            try { return Integer.parseInt(name.substring(5)); } catch (NumberFormatException e) { return -1; }
        }
        if (name.startsWith("ht")) {
            try { return Integer.parseInt(name.substring(2)); } catch (NumberFormatException e) { return -1; }
        }
        return -1;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        // === ГЛОБАЛЬНАЯ КОМАНДА /htime reload ===
        if (cmdName.equals("htime") || cmdName.equals("ht")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.isOp()) {
                    sender.sendMessage("§cУ вас нет прав!");
                    return true;
                }
                plugin.reloadConfig();
                plugin.loadAutoMatchConfig();
                plugin.getLanguageManager().loadMessages();
                sender.sendMessage("§aПлагин Htime успешно перезагружен!");
                return true;
            }
            sender.sendMessage("§cИспользование: /htime reload");
            return true;
        }

        // === КОМАНДЫ ДЛЯ АРЕН htime1-50 ===
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.getMessage("common.player-only"));
            return true;
        }

        int arenaId = getArenaId(command);
        if (arenaId < 1 || arenaId > 50) {
            sender.sendMessage("§cНекорректный номер арены!");
            return true;
        }

        TimerInstance t = plugin.timers[arenaId];

        if (args.length == 0) {
            sendHelp(p, arenaId);
            return true;
        }

        String permPrefix = "htime" + arenaId;

        switch (args[0].toLowerCase()) {
            case "pos1" -> {
                if (!checkPerm(p, permPrefix + ".pos")) return true;
                t.pos1 = p.getLocation();
                plugin.sendMessage(p, plugin.getMessage("pos.pos1-set", arenaId));
            }
            case "pos2" -> {
                if (!checkPerm(p, permPrefix + ".pos")) return true;
                t.pos2 = p.getLocation();
                plugin.sendMessage(p, plugin.getMessage("pos.pos2-set", arenaId));
            }
            case "pos" -> {
                if (!checkPerm(p, permPrefix + ".pos")) return true;
                if (args.length == 2 && args[1].equalsIgnoreCase("del")) {
                    t.pos1 = null;
                    t.pos2 = null;
                    plugin.sendMessage(p, plugin.getMessage("pos.pos-deleted", arenaId));
                } else {
                    plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax", arenaId,
                            "{usage}", "/htime" + arenaId + " pos del"));
                }
            }
            case "1", "2", "3" -> handlePeriod(p, arenaId, t, Integer.parseInt(args[0]), args, permPrefix);
            case "pereriv" -> handleBreak(p, arenaId, t, args, permPrefix);
            case "del" -> handleDelete(p, arenaId, t, permPrefix);
            case "bullit" -> handleBullit(p, arenaId, t, permPrefix);
            case "per" -> handlePer(p, arenaId, t, permPrefix);
            default -> handlePenalty(p, arenaId, t, args, permPrefix);
        }

        return true;
    }

    private boolean checkPerm(Player p, String perm) {
        if (!p.hasPermission(perm) && !p.isOp()) {
            plugin.sendMessage(p, plugin.getMessage("common.no-permission"));
            return false;
        }
        return true;
    }

    private void handlePeriod(Player p, int arenaId, TimerInstance t, int period, String[] args, String permPrefix) {
        if (!checkPerm(p, permPrefix + ".match")) return;

        if (args.length < 2) {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax", arenaId,
                    "{usage}", "/htime" + arenaId + " " + period + " <время>"));
            return;
        }

        String timeStr = args[1];
        int totalSeconds = parseTimeString(timeStr);
        if (totalSeconds <= 0) {
            plugin.sendMessage(p, "&#FF5555Неверный формат времени! Используйте: 15m или 15:30");
            return;
        }

        clearTimers(t);

        if (period == 1) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hvar" + arenaId + " on");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hvar" + arenaId + " on");
        }

        t.autoMatch = true;
        t.nextPeriod = period + 1;
        t.isBreakMode = false;
        t.matchTimer = new MatchTimer(totalSeconds, false, arenaId);
        t.currentPeriod = period;
        t.matchTimer.start();

        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String timeDisplay = (seconds > 0) ? minutes + "м " + seconds + "с" : minutes + " минут";
        plugin.broadcastRadius(arenaId, plugin.getMessage("match.started", arenaId,
                "{period}", String.valueOf(period),
                "{minutes}", timeDisplay));
    }

    private void handleBreak(Player p, int arenaId, TimerInstance t, String[] args, String permPrefix) {
        if (!checkPerm(p, permPrefix + ".match")) return;

        if (args.length == 1) {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax", arenaId,
                    "{usage}", "/htime" + arenaId + " pereriv <сек>s / del"));
            return;
        }

        if (args[1].equalsIgnoreCase("del")) {
            if (t.breakTimer == null) {
                plugin.sendMessage(p, plugin.getMessage("break.not-running", arenaId));
                return;
            }
            if (t.breakBossBar != null) {
                t.breakBossBar.removeAll();
                t.breakBossBar = null;
            }
            t.breakTimer.stop();
            t.breakTimer = null;
            plugin.broadcastRadius(arenaId, plugin.getMessage("break.force-ended", arenaId));
            return;
        }

        int seconds = parseTimeStringSeconds(args[1]);
        if (seconds <= 0) {
            plugin.sendMessage(p, "&#FF5555Введите корректное количество секунд! Пример: /htime" + arenaId + " pereriv 30s");
            return;
        }

        if (t.breakTimer != null) {
            t.breakTimer.stop();
            t.breakTimer = null;
        }
        if (t.breakBossBar != null) {
            t.breakBossBar.removeAll();
            t.breakBossBar = null;
        }

        t.breakBossBar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SEGMENTED_20);
        String titleText = "&#55AAFF&lПЕРЕРЫВ &f" + String.format("%02d:%02d", seconds / 60, seconds % 60);
        t.breakBossBar.setTitle(plugin.convertHexToLegacy(titleText));

        if (t.pos2 != null) {
            for (Player player : t.pos2.getWorld().getPlayers()) {
                if (player.getLocation().distance(t.pos2) <= 100) {
                    t.breakBossBar.addPlayer(player);
                }
            }
        }

        t.isBreakMode = true;
        t.breakTimer = new MatchTimer(seconds, true, arenaId);
        t.breakTimer.start();

        plugin.broadcastRadius(arenaId, plugin.getMessage("break.started", arenaId,
                "{seconds}", String.valueOf(seconds)));
    }

    private int parseTimeString(String input) {
        if (input.endsWith("m")) {
            try {
                return Integer.parseInt(input.replace("m", "")) * 60;
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        if (input.contains(":")) {
            String[] parts = input.split(":");
            if (parts.length == 2) {
                try {
                    int minutes = Integer.parseInt(parts[0]);
                    int seconds = Integer.parseInt(parts[1]);
                    if (minutes > 99 || seconds > 59) return -1;
                    return minutes * 60 + seconds;
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private int parseTimeStringSeconds(String input) {
        if (input.endsWith("s")) {
            try {
                return Integer.parseInt(input.replace("s", ""));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private void clearTimers(TimerInstance t) {
        if (t.matchTimer != null) {
            t.matchTimer.stop();
            t.matchTimer = null;
        }
        if (t.breakTimer != null) {
            t.breakTimer.stop();
            t.breakTimer = null;
        }
        if (t.breakBossBar != null) {
            t.breakBossBar.removeAll();
            t.breakBossBar = null;
        }
        t.currentPeriod = 0;
        t.autoMatch = false;
    }

    private void handleDelete(Player p, int arenaId, TimerInstance t, String permPrefix) {
        if (!checkPerm(p, permPrefix + ".match")) return;
        clearTimers(t);
        plugin.broadcastRadius(arenaId, plugin.getMessage("match.force-ended", arenaId));
    }

    private void handleBullit(Player p, int arenaId, TimerInstance t, String permPrefix) {
        if (!checkPerm(p, permPrefix + ".match")) return;
        t.bullitMode = true;
        t.timersRunning = false;
        plugin.broadcastRadius(arenaId, "&#FF5555&l[BULLIT MODE] &fВремя остановлено навсегда!");
        plugin.sendMessage(p, "&#55FF55&lРежим BULLIT активирован!");
    }

    private void handlePer(Player p, int arenaId, TimerInstance t, String permPrefix) {
        if (!checkPerm(p, permPrefix + ".match")) return;
        t.bullitMode = false;
        if (t.pos1 != null) {
            boolean hasBoat = t.pos1.getWorld().getEntitiesByClass(org.bukkit.entity.Boat.class).stream()
                    .anyMatch(b -> b.getLocation().distance(t.pos1) <= 100);
            t.timersRunning = hasBoat;
        } else {
            t.timersRunning = true;
        }
        plugin.broadcastRadius(arenaId, "&#55FF55&l[PER MODE] &fОбычный режим активирован!");
        plugin.sendMessage(p, "&#55FF55&lОбычный режим активирован!");
    }

    private void handlePenalty(Player p, int arenaId, TimerInstance t, String[] args, String permPrefix) {
        if (!checkPerm(p, permPrefix + ".match")) return;

        if (args.length == 2 && args[1].equalsIgnoreCase("del")) {
            removePenalty(p, arenaId, t, args[0]);
            return;
        }

        if (args.length >= 4 && args[args.length - 1].equalsIgnoreCase("start")) {
            addPenalty(p, arenaId, t, args);
            return;
        }

        sendHelp(p, arenaId);
    }

    private void addPenalty(Player p, int arenaId, TimerInstance t, String[] args) {
        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
        if (target == null || target.getName() == null) {
            plugin.sendMessage(p, plugin.getMessage("penalty.player-not-found", arenaId,
                    "{player}", targetName));
            return;
        }

        String[] reasonParts = Arrays.copyOfRange(args, 1, args.length - 2);
        String reason = String.join(" ", reasonParts);
        String timeStr = args[args.length - 2];

        if (!timeStr.endsWith("m")) {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax", arenaId,
                    "{usage}", "/htime" + arenaId + " <ник> <причина> <время>m start"));
            return;
        }

        try {
            int minutes = Integer.parseInt(timeStr.replace("m", ""));
            PenaltyPlayer pp = new PenaltyPlayer(target.getUniqueId(), reason, minutes, plugin, arenaId);
            t.penalties.put(target.getUniqueId(), pp);
            pp.start();

            String name = target.getName() != null ? target.getName() : targetName;
            plugin.broadcastRadius(arenaId, plugin.getMessage("penalty.added", arenaId,
                    "{player}", name, "{reason}", reason, "{minutes}", String.valueOf(minutes)));

            plugin.addPenaltyToHistory(arenaId, name, reason, minutes);
        } catch (NumberFormatException e) {
            plugin.sendMessage(p, plugin.getMessage("common.invalid-syntax", arenaId,
                    "{usage}", "/htime" + arenaId + " <ник> <причина> <время>m start"));
        }
    }

    private void removePenalty(Player p, int arenaId, TimerInstance t, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
        if (target == null || target.getName() == null) {
            plugin.sendMessage(p, plugin.getMessage("penalty.player-not-found", arenaId,
                    "{player}", targetName));
            return;
        }

        PenaltyPlayer pp = t.penalties.remove(target.getUniqueId());
        if (pp == null) {
            plugin.sendMessage(p, plugin.getMessage("penalty.not-found", arenaId,
                    "{player}", target.getName()));
            return;
        }

        pp.remainingSeconds = 0;
        String playerName = target.getName() != null ? target.getName() : targetName;
        plugin.broadcastRadius(arenaId, plugin.getMessage("penalty.removed", arenaId,
                "{player}", playerName));
    }

    private void sendHelp(Player p, int arenaId) {
        for (String line : plugin.getMessageList("help.main")) {
            String colored = line.replace("{arena}", String.valueOf(arenaId));
            plugin.sendMessage(p, colored);
        }
    }
}