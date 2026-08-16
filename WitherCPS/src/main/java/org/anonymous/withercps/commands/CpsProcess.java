package org.anonymous.withercps.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.anonymous.withercps.WitherCPS;
import org.anonymous.withercps.cache.ColorCache;
import org.anonymous.withercps.cache.ModeCache;
import org.anonymous.withercps.cache.PlayerCache;
import org.anonymous.withercps.netty.PacketInjector;
import org.anonymous.withercps.sessions.click.ClickService;
import org.anonymous.withercps.sessions.watcher.WatcherService;
import org.anonymous.withercps.sessions.watcher.WatcherSession;
import org.anonymous.withercps.utils.ConfigUtils;
import org.anonymous.withercps.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.*;

public record CpsProcess(WitherCPS plugin, WatcherService watcherService, ClickService clickService, PacketInjector injector, ConfigUtils config, ColorCache colors, ModeCache modes, PlayerCache players, TimeUtils timeUtils) implements CommandExecutor, TabCompleter {

    private static final List<String> PERSONAL_COMMANDS = List.of("add", "remove", "list");
    private static final List<String> ADMIN_COMMANDS = List.of("reload");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            if (!sender.hasPermission("Koramikon.cps")) {
                sender.sendMessage(config.getMessage("messages.commands.errors.no-permission"));
                return true;
            }
            if (sender instanceof Player player) {
                toggleMonitoring(player, true);
            } else {
                sender.sendMessage(config.getMessage("messages.commands.errors.only-player"));
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("Koramikon.cps.reload")) {
                    sender.sendMessage(config.getMessage("messages.commands.errors.no-permission"));
                    return true;
                }
                handleReload(sender);
            }

            case "help" -> {
                if (!sender.hasPermission("Koramikon.cps.help")) {
                    sender.sendMessage(config.getMessage("messages.commands.errors.no-permission"));
                    return true;
                }
                handleHelp(sender);
            }

            case "list" -> {
                if (!sender.hasPermission("Koramikon.cps.list")) {
                    sender.sendMessage(config.getMessage("messages.commands.errors.no-permission"));
                    return true;
                }
                sender.sendMessage(config.getMessage("messages.commands.list"));
            }

            case "mode" -> {
                if (!sender.hasPermission("Koramikon.cps.mode")) {
                    sender.sendMessage(config.getMessage("messages.commands.errors.no-permission"));
                    return true;
                }
                if (sender instanceof Player player) {
                    handleMode(player, args);
                } else {
                    sender.sendMessage(config.getMessage("messages.commands.errors.only-player"));
                }
            }

            case "mute" -> {
                if (!sender.hasPermission("Koramikon.cps.mute")) {
                    sender.sendMessage(config.getMessage("messages.commands.errors.no-permission"));
                    return true;
                }
                if (sender instanceof Player player) {
                    handleMute(player, args, true);
                } else {
                    sender.sendMessage(config.getMessage("messages.commands.errors.only-player"));
                }
            }

            case "unmute" -> {
                if (!sender.hasPermission("Koramikon.cps.unmute")) {
                    sender.sendMessage(config.getMessage("messages.commands.errors.no-permission"));
                    return true;
                }
                if (sender instanceof Player player) {
                    handleMute(player, args, false);
                } else {
                    sender.sendMessage(config.getMessage("messages.commands.errors.only-player"));
                }
            }

            case "mutelist" -> {
                if (!sender.hasPermission("Koramikon.cps.mutelist")) {
                    sender.sendMessage(config.getMessage("messages.commands.errors.no-permission"));
                    return true;
                }
                if (sender instanceof Player player) {
                    handleMutelist(player);
                } else {
                    sender.sendMessage(config.getMessage("messages.commands.errors.only-player"));
                }
            }

            case "personal" -> {
                if (!sender.hasPermission("Koramikon.cps.personal")) {
                    sender.sendMessage(config.getMessage("messages.commands.errors.no-permission"));
                    return true;
                }
                if (sender instanceof Player player) {
                    handlePersonal(player, args);
                } else {
                    sender.sendMessage(config.getMessage("messages.commands.errors.only-player"));
                }
            }

            case "netty" -> {
                if (!sender.hasPermission("Koramikon.cps.Netty")) {
                    sender.sendMessage(config.getMessage("messages.commands.errors.no-permission"));
                    return true;
                }
                if (sender instanceof Player player) {
                    toggleMonitoring(player, false);
                } else {
                    sender.sendMessage(config.getMessage("messages.commands.errors.only-player"));
                }
            }

            case "all" -> {
                if (!sender.hasPermission("Koramikon.cps.all")) {
                    sender.sendMessage(config.getMessage("messages.commands.errors.no-permission"));
                    return true;
                }
                if (sender instanceof Player player) {
                    toggleAll(player);
                } else {
                    sender.sendMessage(config.getMessage("messages.commands.errors.only-player"));
                }
            }

            case "network-thread", "network" -> {
                if (!sender.hasPermission("Koramikon.cps.Netty")) {
                    sender.sendMessage(config.getMessage("messages.commands.errors.no-permission"));
                    return true;
                }
                if (sender instanceof Player player) {
                    toggleMonitoring(player, false);
                } else {
                    sender.sendMessage(config.getMessage("messages.commands.errors.only-player"));
                }
            }

            case "server-thread", "server" -> {
                if (!sender.hasPermission("Koramikon.cps")) {
                    sender.sendMessage(config.getMessage("messages.commands.errors.no-permission"));
                    return true;
                }
                if (sender instanceof Player player) {
                    toggleMonitoring(player, true);
                } else {
                    sender.sendMessage(config.getMessage("messages.commands.errors.only-player"));
                }
            }

            default -> {
                sender.sendMessage(config.getMessage("messages.commands.errors.use-tab"));
            }
        }
        return true;
    }

    private void handleHelp(CommandSender sender) {
        sender.sendMessage(config.getMessage("messages.commands.help.header"));

        ConfigurationSection commandsSection = plugin.getConfig().getConfigurationSection("messages.commands.help.commands");
        if (commandsSection != null) {
            for (String key : commandsSection.getKeys(false)) {
                String description = commandsSection.getString(key, "");
                // Ключ для format: убираем угловые скобки для плейсхолдера
                sender.sendMessage(config.getMessage("messages.commands.help.format",
                        "command", key,
                        "description", description));
            }
        }
    }

    private void toggleMonitoring(Player player, boolean serverThread) {
        UUID uuid = player.getUniqueId();
        WatcherSession session = watcherService.getSession(uuid);

        if (session == null) {
            session = watcherService.start(uuid);
        }

        boolean enabled;

        if (serverThread) {
            enabled = !session.isServer();
            session.setServer(enabled);
        } else {
            enabled = !session.isNetwork();
            session.setNetwork(enabled);
        }

        player.sendMessage(getComponent(serverThread, enabled));
    }

    private void toggleAll(Player player) {
        UUID uuid = player.getUniqueId();
        WatcherSession session = watcherService.getSession(uuid);

        if (session == null) {
            session = watcherService.start(uuid);
        }

        boolean bothEnabled = session.isServer() && session.isNetwork();

        if (bothEnabled) {
            session.setServer(false);
            session.setNetwork(false);
            session.setMode(null);
            player.sendMessage(config.getMessage("messages.commands.monitoring.all.disable"));
        } else {
            session.setServer(true);
            session.setNetwork(true);
            // Включаем режим "hockey" по умолчанию при /cps all
            if (session.getMode() == null) {
                session.setMode("hockey");
            }
            player.sendMessage(config.getMessage("messages.commands.monitoring.all.enable"));
        }
    }

    private @NonNull Component getComponent(boolean serverThread, boolean enabled) {
        String thread = config.getString("messages.commands.monitoring.placeholders." + (serverThread ? "thread-server" : "thread-network"));
        String status = config.getString("messages.commands.monitoring.placeholders." + (enabled ? "status-enable" : "status-disable"));

        Component message = config.getMessage("messages.commands.monitoring.success", "thread", thread, "status", status);

        if (enabled) {
            Component details = Component.text(" §7(наведитесь)"
            ).hoverEvent(
                    HoverEvent.showText(
                            Component.text(
                                    serverThread
                                            ? "§fРежим Server-Thread использует серверные тики,\nпоэтому значения CPS могут быть неточными.\n\nНе воспринимайте данный CPS как истину."
                                            : "§fРежим Network-Thread не отслеживает серверные состояния\nигрока (gamemode, effects, speed и другие)."
                            )
                    )
            );

            message = message.append(details);
        }
        return message;
    }

    private void handleMode(Player player, String[] args) {
        WatcherSession session = watcherService.getSession(player.getUniqueId());
        if (session == null) {
            session = watcherService.start(player.getUniqueId());
        }

        if (!session.isEnabled()) {
            player.sendMessage(config.getMessage("messages.commands.errors.off-monitoring"));
            return;
        }

        if (args.length < 2) {
            // /cps mode — включает/выключает режим hockey
            String currentMode = session.getMode();
            if (currentMode == null) {
                session.setMode("hockey");
                player.sendMessage(config.getMessage("messages.commands.mode.success", "mode", "&bХоккей"));
            } else {
                session.setMode(null);
                player.sendMessage(config.getMessage("messages.commands.mode.reset"));
            }
            return;
        }

        String input = args[1].toLowerCase();
        if (input.equals("off")) {
            session.setMode(null);
            player.sendMessage(config.getMessage("messages.commands.mode.reset"));
            return;
        }

        ModeCache.Mode mode = modes.getMode(input);
        if (mode == null) {
            player.sendMessage(config.getMessage("messages.commands.mode.not", "mode", input));
            return;
        }

        session.setMode(input);
        player.sendMessage(config.getMessage("messages.commands.mode.success", "mode", mode.prefix()));
    }

    private void handleMute(Player player, String[] args, boolean mute) {
        if (args.length < 2) {
            player.sendMessage(config.getMessage("messages.commands." + (mute ? "mute" : "unmute") + ".usage"));
            return;
        }

        WatcherSession session = watcherService.getSession(player.getUniqueId());
        if (session == null) {
            player.sendMessage(config.getMessage("messages.commands.errors.off-monitoring"));
            return;
        }

        String last = args[args.length - 1];
        Optional<Duration> parsed = timeUtils.parse(last);
        long duration = parsed.map(Duration::toMillis).orElse(-1L);

        int end = parsed.isPresent() ? args.length - 1 : args.length;

        for (int i = 1; i < end; i++) {
            String targetName = args[i];
            UUID targetUuid = players.getUuid(targetName);

            if (targetUuid == null) {
                player.sendMessage(config.getMessage("messages.commands.errors.incorrect-player", "target", targetName));
                continue;
            }

            if (mute) {
                session.mute(targetUuid, duration);
                String time = parsed.isPresent() ? " на " + args[args.length - 1] : "";
                player.sendMessage(config.getMessage("messages.commands.mute.success", "target", targetName, "time", time));
                continue;
            }

            if (!session.isMuted(targetUuid)) {
                player.sendMessage(config.getMessage("messages.commands.unmute.not", "target", targetName));
                continue;
            }

            session.unmute(targetUuid);
            player.sendMessage(config.getMessage("messages.commands.unmute.success", "target", targetName));
        }
    }

    private void handleMutelist(Player player) {
        WatcherSession session = watcherService.getSession(player.getUniqueId());
        if (session == null) {
            player.sendMessage(config.getMessage("messages.commands.errors.off-monitoring"));
            return;
        }

        Map<UUID, Long> muted = session.getMuted();
        if (muted.isEmpty()) {
            player.sendMessage(config.getMessage("messages.commands.mutelist.empty"));
            return;
        }

        player.sendMessage(config.getMessage("messages.commands.mutelist.header"));
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, Long> entry : muted.entrySet()) {
            UUID uuid = entry.getKey();
            long expiry = entry.getValue();

            String formatted = expiry == -1L ? "" : timeUtils().format(expiry - now);
            player.sendMessage(config.getMessage("messages.commands.mutelist.format", "target", players.getName(uuid), "time", formatted));
        }
    }

    private void handlePersonal(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        WatcherSession session = watcherService.getSession(uuid);

        if (session == null || !session.isEnabled()) {
            player.sendMessage(config.getMessage("messages.commands.errors.off-monitoring"));
            return;
        }

        if (args.length < 2) {
            if (!session.getTargets().isEmpty()) {
                session.getTargets().clear();
                player.sendMessage(config.getMessage("messages.commands.monitoring.personal.exit"));
                return;
            }
            player.sendMessage(config.getMessage("messages.commands.monitoring.personal.usage"));
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "add" -> {
                if (!player.hasPermission("Koramikon.cps.personal.add")) {
                    player.sendMessage(config.getMessage("messages.commands.errors.no-permission"));
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(config.getMessage("messages.commands.monitoring.personal.add.usage"));
                    return;
                }
                for (int i = 2; i < args.length; i++) {
                    String targetName = args[i];
                    UUID targetUuid = players.getUuid(targetName);

                    if (targetUuid == null) {
                        player.sendMessage(config.getMessage("messages.commands.errors.incorrect-player", "target", targetName));
                        continue;
                    }

                    if (session.getTargets().contains(targetUuid)) {
                        player.sendMessage(config.getMessage("messages.commands.monitoring.personal.add.already", "target", targetName));
                        continue;
                    }

                    session.addTarget(targetUuid);
                    player.sendMessage(config.getMessage("messages.commands.monitoring.personal.add.success", "target", targetName));
                }
            }

            case "remove" -> {
                if (!player.hasPermission("Koramikon.cps.personal.remove")) {
                    player.sendMessage(config.getMessage("messages.commands.errors.no-permission"));
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(config.getMessage("messages.commands.monitoring.personal.remove.usage"));
                    return;
                }
                for (int i = 2; i < args.length; i++) {
                    String targetName = args[i];
                    UUID targetUuid = players.getUuid(targetName);

                    if (targetUuid == null) {
                        player.sendMessage(config.getMessage("messages.commands.errors.incorrect-player", "target", targetName));
                        continue;
                    }

                    if (!session.getTargets().contains(targetUuid)) {
                        player.sendMessage(config.getMessage("messages.commands.monitoring.personal.remove.not", "target", targetName));
                        continue;
                    }

                    session.removeTarget(targetUuid);
                    player.sendMessage(config.getMessage("messages.commands.monitoring.personal.remove.success", "target", targetName));
                }
            }

            case "list" -> {
                if (!player.hasPermission("Koramikon.cps.personal.list")) {
                    player.sendMessage(config.getMessage("messages.commands.errors.no-permission"));
                    return;
                }
                Set<UUID> targets = session.getTargets();
                if (targets.isEmpty()) {
                    player.sendMessage(config.getMessage("messages.commands.monitoring.personal.list.empty"));
                    return;
                }

                player.sendMessage(config.getMessage("messages.commands.monitoring.personal.list.header"));
                for (UUID targetUuid : targets) {
                    String name = players.getName(targetUuid);
                    if (name != null) {
                        player.sendMessage(config.getMessage("messages.commands.monitoring.personal.list.format", "target", name));
                    }
                }
            }

            default -> {
                player.sendMessage(config.getMessage("messages.commands.monitoring.personal.usage"));
            }
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        colors.load();
        modes.load();

        sender.sendMessage(config.getMessage("messages.commands.reload.success"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        String current = args[args.length - 1].toLowerCase();
        List<String> completions = new ArrayList<>();

        WatcherSession session = watcherService.getSession(player.getUniqueId());

        if (args.length == 1) {
            if (player.hasPermission("Koramikon.cps.Netty")) copyMatches(current, List.of("Netty"), completions);
            if (player.hasPermission("Koramikon.cps.all")) copyMatches(current, List.of("all"), completions);
            if (player.hasPermission("Koramikon.cps.reload")) copyMatches(current, ADMIN_COMMANDS, completions);
            if (player.hasPermission("Koramikon.cps.help")) copyMatches(current, List.of("help"), completions);
            if (player.hasPermission("Koramikon.cps.list")) copyMatches(current, List.of("list"), completions);

            if (session != null) {
                if (player.hasPermission("Koramikon.cps.mode")) copyMatches(current, List.of("mode"), completions);
                if (player.hasPermission("Koramikon.cps.mute")) copyMatches(current, List.of("mute"), completions);
                if (player.hasPermission("Koramikon.cps.unmute")) copyMatches(current, List.of("unmute"), completions);
                if (player.hasPermission("Koramikon.cps.mutelist")) copyMatches(current, List.of("mutelist"), completions);
                if (player.hasPermission("Koramikon.cps.personal")) copyMatches(current, List.of("personal"), completions);
            }

            return completions;
        }

        if (session == null) {
            return Collections.emptyList();
        }

        String sub = args[0].toLowerCase();
        Set<String> written = new HashSet<>(Arrays.asList(args).subList(1, args.length - 1));

        switch (sub) {
            case "mode" -> {
                if (args.length == 2 && player.hasPermission("Koramikon.cps.mode")) {
                    String currentMode = session.getMode();

                    for (ModeCache.Mode mode : modes.getModes()) {
                        String modeName = mode.name();

                        if (!modeName.equalsIgnoreCase(currentMode)) {
                            completions.add(modeName);
                        }
                    }
                    if (currentMode != null) {
                        completions.add("off");
                    }
                }
            }

            case "mute" -> {
                if (player.hasPermission("Koramikon.cps.mute")) {
                    fillPlayers(completions, player, current, written, false);
                }
            }

            case "unmute" -> {
                if (player.hasPermission("Koramikon.cps.unmute")) {
                    for (UUID uuid : session.getMuted().keySet()) {
                        String name = players.getName(uuid);

                        if (name != null && startWithIgnoreCase(name, current) && !written.contains(name)) {
                            completions.add(name);
                        }
                    }
                }
            }

            case "personal" -> {
                if (args.length == 2 && player.hasPermission("Koramikon.cps.personal")) {
                    copyMatches(current, PERSONAL_COMMANDS, completions);
                } else if (args.length >= 3) {
                    String personalSub = args[1].toLowerCase();
                    Set<UUID> targets = session.getTargets();

                    switch (personalSub) {
                        case "add" -> {
                            if (player.hasPermission("Koramikon.cps.personal.add")) {
                                fillPlayers(completions, player, current, written, true);
                            }
                        }
                        case "remove" -> {
                            if (player.hasPermission("Koramikon.cps.personal.remove")) {
                                for (UUID targetUuid : targets) {
                                    String name = players.getName(targetUuid);
                                    if (name != null && startWithIgnoreCase(name, current) && !written.contains(name)) {
                                        completions.add(name);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        var result = StringUtil.copyPartialMatches(current, completions, new ArrayList<>());
        Collections.sort(result);

        return result;
    }

    private void copyMatches(String prefix, Collection<String> source, List<String> target) {
        for (String string : source) {
            if (startWithIgnoreCase(string, prefix)) {
                target.add(string);
            }
        }
    }

    private boolean startWithIgnoreCase(String string, String prefix) {
        return string.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private void fillPlayers(List<String> completions, Player player, String current, Set<String> written, boolean excludeAlreadyAdded) {
        WatcherSession session = watcherService.getSession(player.getUniqueId());

        for (Player target : Bukkit.getOnlinePlayers()) {
            String name = target.getName();

            if (name.equalsIgnoreCase(player.getName())) {
                continue;
            }

            if (written.contains(name)) {
                continue;
            }

            if (excludeAlreadyAdded && session != null && session.getTargets().contains(target.getUniqueId())) {
                continue;
            }

            if (name.toLowerCase().startsWith(current)) {
                completions.add(name);
            }
        }
    }
}