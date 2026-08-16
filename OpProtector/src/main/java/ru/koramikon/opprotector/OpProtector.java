package ru.koramikon.opprotector;

import org.bukkit.plugin.java.JavaPlugin;
import ru.koramikon.opprotector.commands.ListOpCommand;
import ru.koramikon.opprotector.commands.LopCommand;
import ru.koramikon.opprotector.listeners.OpProtectionListener;
import ru.koramikon.opprotector.managers.ConfigManager;
import ru.koramikon.opprotector.utils.MessageUtils;

import java.util.Objects;

public class OpProtector extends JavaPlugin {

    private static OpProtector instance;
    private ConfigManager configManager;
    private MessageUtils messageUtils;
    private OpProtectionListener protectionListener;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.messageUtils = new MessageUtils(this);

        // Register commands
        ListOpCommand listOpCommand = new ListOpCommand(this);
        Objects.requireNonNull(getCommand("listop")).setExecutor(listOpCommand);
        Objects.requireNonNull(getCommand("listop")).setTabCompleter(listOpCommand);

        LopCommand lopCommand = new LopCommand(this);
        Objects.requireNonNull(getCommand("lop")).setExecutor(lopCommand);
        Objects.requireNonNull(getCommand("lop")).setTabCompleter(lopCommand);

        // Register listener
        this.protectionListener = new OpProtectionListener(this);
        getServer().getPluginManager().registerEvents(protectionListener, this);

        // Check all online players after startup
        getServer().getScheduler().runTaskLater(this, () -> {
            protectionListener.checkAllPlayers();
        }, 20L);

        getLogger().info("==========================================");
        getLogger().info("OpProtector enabled!");
        getLogger().info("==========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("OpProtector disabled!");
    }

    public static OpProtector getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageUtils getMessageUtils() {
        return messageUtils;
    }

    public void reloadPlugin() {
        reloadConfig();
        configManager = new ConfigManager(this);
        messageUtils = new MessageUtils(this);

        if (protectionListener != null) {
            protectionListener.checkAllPlayers();
        }

        getLogger().info("OpProtector reloaded!");
    }
}