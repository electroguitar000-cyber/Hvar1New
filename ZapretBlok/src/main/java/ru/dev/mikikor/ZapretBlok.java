package ru.dev.mikikor;

import org.bukkit.plugin.java.JavaPlugin;
import ru.dev.mikikor.commands.ZapretCommand;
import ru.dev.mikikor.listeners.BlockBreakListener;
import ru.dev.mikikor.listeners.BlockPlaceListener;
import ru.dev.mikikor.managers.ConfigManager;

public class ZapretBlok extends JavaPlugin {

    private static ZapretBlok instance;
    private ConfigManager configManager;
    private boolean protectionEnabled;

    @Override
    public void onEnable() {
        instance = this;
        this.protectionEnabled = true;

        this.configManager = new ConfigManager(this);

        getServer().getPluginManager().registerEvents(new BlockBreakListener(), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(), this);

        getCommand("zapret").setExecutor(new ZapretCommand());

        getLogger().info("§aZapretBlok успешно загружен! Защита активна.");
    }

    @Override
    public void onDisable() {
        getLogger().info("§cZapretBlok выключен!");
    }

    public static ZapretBlok getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public boolean isProtectionEnabled() {
        return protectionEnabled;
    }

    public void setProtectionEnabled(boolean enabled) {
        this.protectionEnabled = enabled;
        String status = enabled ? "включена" : "выключена";
        getLogger().info("Защита " + status);

        getServer().getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("zapret.admin") || p.isOp())
                .forEach(p -> p.sendMessage(configManager.getMessage("messages.protection_status")
                        .replace("{status}", configManager.getMessage(enabled ? "messages.status_on" : "messages.status_off"))));
    }
}