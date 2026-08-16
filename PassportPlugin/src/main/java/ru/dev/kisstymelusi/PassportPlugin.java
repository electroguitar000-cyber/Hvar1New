package ru.dev.kisstymelusi;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.dev.kisstymelusi.commands.PassportCommand;
import ru.dev.kisstymelusi.commands.PassportTabCompleter;
import ru.dev.kisstymelusi.expansion.PassportExpansion;
import ru.dev.kisstymelusi.listeners.ChatListener;
import ru.dev.kisstymelusi.listeners.PassportDropListener;
import ru.dev.kisstymelusi.managers.*;
import ru.dev.kisstymelusi.utils.BookBuilder;
import ru.dev.kisstymelusi.utils.PassportTransferManager;
import ru.dev.kisstymelusi.listeners.PassportInteractListener;

import java.util.UUID; // <-- добавлен импорт

public class PassportPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private PassportManager passportManager;
    private DialogManager dialogManager;
    private PassportTransferManager transferManager;
    private int taskId;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        passportManager = new PassportManager(this);
        dialogManager = new DialogManager(this);
        transferManager = new PassportTransferManager(this);

        getCommand("passport").setExecutor(new PassportCommand(this));
        getCommand("passport").setTabCompleter(new PassportTabCompleter());

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PassportDropListener(this), this);
        getServer().getPluginManager().registerEvents(new PassportInteractListener(this), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PassportExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        startExpiryNotificationTask();
    }

    @Override
    public void onDisable() {
        if (taskId != 0) {
            getServer().getScheduler().cancelTask(taskId);
        }
        transferManager.clearAll();
    }

    public void reloadPlugin() {
        configManager.reload();
        messageManager.reload();
        passportManager.reload();
    }

    public void giveBook(Player player) {
        ItemStack book = new BookBuilder(this, player.getUniqueId()).build();
        if (book != null) {
            player.getInventory().addItem(book);
        } else {
            player.sendMessage(messageManager.getMessage("no-passport"));
        }
    }

    public void openBook(Player player) {
        ItemStack book = new BookBuilder(this, player.getUniqueId()).build();
        if (book != null) {
            player.openBook(book);
        } else {
            player.sendMessage(messageManager.getMessage("no-passport"));
        }
    }

    public void openBookFor(Player viewer, UUID passportOwnerUUID) {
        ItemStack book = new BookBuilder(this, passportOwnerUUID).build();
        if (book != null) {
            viewer.openBook(book);
        } else {
            viewer.sendMessage(messageManager.getMessage("no-passport"));
        }
    }

    public void removePassportFromInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && BookBuilder.isPassportBook(item)) {
                player.getInventory().remove(item);
            }
        }
    }

    private void startExpiryNotificationTask() {
        int interval = configManager.getNotifyInterval();
        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    if (passportManager.hasPassport(player.getUniqueId()) && passportManager.isExpired(player.getUniqueId())) {
                        player.sendMessage(messageManager.getMessage("expired-notification"));
                    }
                }
            }
        }.runTaskTimerAsynchronously(this, 20L * interval, 20L * interval).getTaskId();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public PassportManager getPassportManager() {
        return passportManager;
    }

    public DialogManager getDialogManager() {
        return dialogManager;
    }

    public PassportTransferManager getTransferManager() {
        return transferManager;
    }
}