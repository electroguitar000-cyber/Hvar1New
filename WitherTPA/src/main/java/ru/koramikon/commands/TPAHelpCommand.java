package ru.koramikon.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ru.koramikon.WitherTPA;

public class TPAHelpCommand implements CommandExecutor {

    private final WitherTPA plugin;

    public TPAHelpCommand(WitherTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Показываем полную справку
        plugin.sendFullHelp(sender);
        return true;
    }
}