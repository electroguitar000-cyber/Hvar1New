package ru.koramikon;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import ru.koramikon.commands.*;
import ru.koramikon.listeners.TeleportListener;
import ru.koramikon.managers.CooldownManager;
import ru.koramikon.managers.PlayerBlockManager;
import ru.koramikon.managers.PlayerSettingsManager;
import ru.koramikon.managers.TeleportManager;
import ru.koramikon.utils.ConfigManager;
import ru.koramikon.utils.MessageUtils;

import java.util.Objects;

public class WitherTPA extends JavaPlugin {

    private static WitherTPA instance;
    private ConfigManager configManager;
    private TeleportManager teleportManager;
    private CooldownManager cooldownManager;
    private PlayerSettingsManager playerSettingsManager;
    private PlayerBlockManager playerBlockManager;
    private MessageUtils messageUtils;

    @Override
    public void onEnable() {
        instance = this;

        // Save default configs
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("aliases.yml", false);

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.messageUtils = new MessageUtils(this);
        this.teleportManager = new TeleportManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.playerSettingsManager = new PlayerSettingsManager();
        this.playerBlockManager = new PlayerBlockManager();

        // Register commands
        registerCommands();

        // Register listeners
        getServer().getPluginManager().registerEvents(new TeleportListener(this), this);

        getLogger().info("WitherTPA has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("WitherTPA has been disabled!");
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("tpa")).setExecutor(new TPACommand(this));
        Objects.requireNonNull(getCommand("tp")).setExecutor(new TPCommand(this));
        Objects.requireNonNull(getCommand("tpaccept")).setExecutor(new TPAcceptCommand(this));
        Objects.requireNonNull(getCommand("tpdeny")).setExecutor(new TPDenyCommand(this));
        Objects.requireNonNull(getCommand("tpahere")).setExecutor(new TPHereCommand(this));
        Objects.requireNonNull(getCommand("tpreload")).setExecutor(new TPReloadCommand(this));
        Objects.requireNonNull(getCommand("tpahelp")).setExecutor(new TPAHelpCommand(this));
        Objects.requireNonNull(getCommand("tpanotice")).setExecutor(new TpaNoticeCommand(this));
        Objects.requireNonNull(getCommand("tpnotice")).setExecutor(new TpNoticeCommand(this));
    }

    public static WitherTPA getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public PlayerSettingsManager getPlayerSettingsManager() {
        return playerSettingsManager;
    }

    public PlayerBlockManager getPlayerBlockManager() {
        return playerBlockManager;
    }

    public MessageUtils getMessageUtils() {
        return messageUtils;
    }

    public void sendFullHelp(CommandSender sender) {
        ConfigManager cfg = getConfigManager();

        getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("header"));

        if (sender.hasPermission("withertpa.tpa")) {
            getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tpa"));
        }
        if (sender.hasPermission("withertpa.tp")) {
            getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tp"));
            if (sender.hasPermission("withertpa.off")) {
                getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tp-off"));
            }
            if (sender.hasPermission("withertpa.on")) {
                getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tp-on"));
            }
            if (sender.hasPermission("withertpa.tp.others")) {
                getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tp-other"));
            }
        }
        if (sender.hasPermission("withertpa.tpahere")) {
            getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tpahere"));
        }
        if (sender.hasPermission("withertpa.tpaccept")) {
            getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tpaccept"));
        }
        if (sender.hasPermission("withertpa.tpdeny")) {
            getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tpdeny"));
        }
        if (sender.hasPermission("withertpa.off")) {
            getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tpa-off"));
        }
        if (sender.hasPermission("withertpa.on")) {
            getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tpa-on"));
        }
        if (sender.hasPermission("withertpa.reload")) {
            getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("tpreload"));
        }
        if (sender.hasPermission("withertpa.notice")) {
            getMessageUtils().sendRawMessage(sender, "&#55FFFF/tpanotice on/off <reset>&f- включить/выключить оповещения");
        }

        getMessageUtils().sendRawMessage(sender, cfg.getHelpMessage("footer"));
    }
}