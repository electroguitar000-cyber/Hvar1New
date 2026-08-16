package org.anonymous.withercps;

import org.anonymous.withercps.cache.ColorCache;
import org.anonymous.withercps.cache.ModeCache;
import org.anonymous.withercps.cache.PlayerCache;
import org.anonymous.withercps.commands.CpsProcess;
import org.anonymous.withercps.listeners.EntityListener;
import org.anonymous.withercps.listeners.PlayerListener;
import org.anonymous.withercps.netty.PacketInjector;
import org.anonymous.withercps.sessions.click.ClickAttention;
import org.anonymous.withercps.sessions.click.ClickService;
import org.anonymous.withercps.sessions.watcher.WatcherService;
import org.anonymous.withercps.utils.ConfigUtils;
import org.anonymous.withercps.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class WitherCPS extends JavaPlugin {

    private ConfigUtils config;
    private TimeUtils timeUtils;
    private ColorCache colors;
    private ModeCache modes;
    private PlayerCache players;
    private WatcherService watcherService;
    private ClickService clickService;
    private PacketInjector injector;
    private CpsProcess processor;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        registerProcesses();
        registerCommands();
        registerListeners();
    }

    private void registerProcesses() {
        config = new ConfigUtils(this);
        timeUtils = new TimeUtils();
        colors = new ColorCache(this);
        modes = new ModeCache(this);
        players = new PlayerCache();

        colors.load();
        modes.load();

        watcherService = new WatcherService();
        clickService = new ClickService(this, new ClickAttention(config, modes), watcherService, config, colors);
        clickService.start();
        clickService.registerBoats();

        injector = new PacketInjector(this, clickService);
        processor = new CpsProcess(this, watcherService, clickService, injector, config, colors, modes, players, timeUtils);
    }

    private void registerCommands() {
        PluginCommand cps = getCommand("withercps");
        if (cps != null) {
            cps.setExecutor(processor);
            cps.setTabCompleter(processor);
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new EntityListener(clickService), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(clickService, watcherService, injector, players), this);
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            injector.uninject(player);
        }
        clickService.getSessions().clear();
        clickService.getBoats().clear();
    }
}