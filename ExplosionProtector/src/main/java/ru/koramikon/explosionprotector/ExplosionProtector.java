package ru.koramikon.explosionprotector;

import org.bukkit.plugin.java.JavaPlugin;
import ru.koramikon.explosionprotector.commands.TNTreloadCommand;
import ru.koramikon.explosionprotector.listeners.ExplosionListener;

import java.util.List;
import java.util.logging.Level;

public final class ExplosionProtector extends JavaPlugin {

    private static ExplosionProtector instance;
    private List<String> blockList;
    private String mode;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadConfigValues();

        // Регистрация команд
        getCommand("tntreload").setExecutor(new TNTreloadCommand(this));

        // Регистрация слушателя
        getServer().getPluginManager().registerEvents(new ExplosionListener(this), this);

        getLogger().info("Плагин ExplosionProtector успешно включен!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Плагин ExplosionProtector выключен.");
    }

    public void loadConfigValues() {
        reloadConfig();
        this.mode = getConfig().getString("mode", "BLACKLIST").toUpperCase();
        this.blockList = getConfig().getStringList("blocks");
        getLogger().info("Конфиг перезагружен. Режим: " + mode + ". Блоков в списке: " + blockList.size());
    }

    public List<String> getBlockList() {
        return blockList;
    }

    public String getMode() {
        return mode;
    }

    public static ExplosionProtector getInstance() {
        return instance;
    }
}