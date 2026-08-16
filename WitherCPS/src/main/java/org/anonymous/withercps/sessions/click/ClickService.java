package org.anonymous.withercps.sessions.click;

import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.api.statistic.types.GenericStatistic;
import net.kyori.adventure.text.Component;
import org.anonymous.withercps.WitherCPS;
import org.anonymous.withercps.cache.ColorCache;
import org.anonymous.withercps.sessions.watcher.WatcherService;
import org.anonymous.withercps.sessions.watcher.WatcherSession;
import org.anonymous.withercps.utils.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ClickService {

    private final WitherCPS plugin;
    private final ClickAttention attentions;
    private final WatcherService watcher;
    private final ConfigUtils config;
    private final ColorCache colors;

    private final Map<UUID, ClickSession> sessions = new ConcurrentHashMap<>();
    private final Set<Integer> boats = ConcurrentHashMap.newKeySet();

    public ClickService(WitherCPS plugin, ClickAttention attentions, WatcherService watcher, ConfigUtils config, ColorCache colors) {
        this.plugin = plugin;
        this.attentions = attentions;
        this.watcher = watcher;
        this.config = config;
        this.colors = colors;
    }

    public void start() {
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> {
            long now = System.nanoTime();

            sessions.entrySet().removeIf(entry -> {
                ClickSession session = entry.getValue();

                if (now - session.getLastAny() < 200_000_000L) {
                    return false;
                }

                Bukkit.getGlobalRegionScheduler().execute(plugin, () -> flush(entry.getKey(), session));
                return true;
            });
        }, 50, 50, TimeUnit.MILLISECONDS);
    }

    private void flush(UUID uuid, ClickSession session) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;

        session.updateDurationTick(durationTick());
        session.updateSpeed(player.getWalkSpeed());

        if (session.hasServer()) {
            send(session.createServerReport(uuid, player.getName()));
        }

        if (session.hasNetwork()) {
            send(session.createNetworkReport(uuid, player.getName()));
        }
    }

    private double durationTick() {
        Spark spark = SparkProvider.get();
        GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> stat = spark.mspt();

        DoubleAverageInfo info = stat != null ? stat.poll(StatisticWindow.MillisPerTick.SECONDS_10) : null;
        return info != null ? info.max() : 0;
    }


    public void register(UUID uuid, boolean network) {
        long now = System.nanoTime();
        ClickSession session = sessions.computeIfAbsent(uuid, id -> new ClickSession(config.getDouble("settings.minus-cps"), plugin));

        session.recordClick(network, now);

        if (!network) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) session.updateStats(player);
        }
    }


    private void send(ClickReport report) {
        String path = report.isNetwork() ? "network" : "server";
        ColorCache.ColorResult colorResult = colors.getColor(report.total(), report.cps(), report.seconds());

        Component global = buildMessage(report, colorResult, path, true);
        Component personal = buildMessage(report, colorResult, path, false);

        watcher.getSessions().forEach((watcherUuid, session) -> {
            if (!canReceive(session, report.uuid())) return;
            if (report.isNetwork() && !session.isNetwork()) return;
            if (report.isServer() && !session.isServer()) return;

            Component message = session.getTargets().isEmpty() ? global : personal;

            if (report.isServer()) {
                Component attention = attentions.buildAttention(report, session.getMode(), colorResult.color());

                if (!attention.equals(Component.empty())) {
                    message = message.append(Component.space()).append(attention);
                }
            }

            Player watcher = Bukkit.getPlayer(watcherUuid);
            if (watcher != null && watcher.isOnline()) watcher.sendMessage(message);
        });
    }

    private boolean canReceive(WatcherSession session, UUID target) {
        return session.isEnabled() && !session.isMuted(target) && (session.getTargets().isEmpty() || session.getTargets().contains(target));
    }

    private Component buildMessage(ClickReport report, ColorCache.ColorResult result, String path, boolean isGlobal) {
        String type = isGlobal ? "global" : "personal";
        String prefix = config.getString("messages.monitoring.report.prefixes." + path + "." + type);

        boolean networkUseColors = plugin.getConfig().getBoolean("settings.network-use-colors", false);
        String networkRedColor = plugin.getConfig().getString("settings.network-red-color", "&c");

        String colorCode;
        if (path.equals("network")) {
            if (networkUseColors && "&c".equals(result.color())) {
                colorCode = networkRedColor;
            } else {
                colorCode = "&f";
            }
        } else {
            colorCode = result.color();
        }

        boolean hideStats = result.isZero();

        return config.getMessage(
                "messages.monitoring.report.format",
                "prefix", prefix,
                "target", report.nickname(),
                "color", colorCode,
                "total", report.total(),
                "time", hideStats ? 0.0 : report.seconds(),
                "cps", hideStats ? 0.0 : report.cps()
        );
    }


    public Map<UUID, ClickSession> getSessions() {
        return sessions;
    }

    public Set<Integer> getBoats() {
        return boats;
    }

    public void registerBoats() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Boat) boats.add(entity.getEntityId());
            }
        }
    }
}