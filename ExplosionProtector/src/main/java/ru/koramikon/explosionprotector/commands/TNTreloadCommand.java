package ru.koramikon.explosionprotector.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.koramikon.explosionprotector.ExplosionProtector;

public class TNTreloadCommand implements CommandExecutor {

    private final ExplosionProtector plugin;

    public TNTreloadCommand(ExplosionProtector plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("explosionprotector.reload")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды!");
            return true;
        }

        try {
            plugin.loadConfigValues();
            sender.sendMessage(ChatColor.GREEN + "Конфиг ExplosionProtector успешно перезагружен!");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Ошибка при перезагрузке конфига. Проверь консоль.");
            e.printStackTrace();
        }

        return true;
    }
}