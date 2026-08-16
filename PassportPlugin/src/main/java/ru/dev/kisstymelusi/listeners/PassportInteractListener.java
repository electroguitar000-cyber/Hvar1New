package ru.dev.kisstymelusi.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import ru.dev.kisstymelusi.PassportPlugin;
import ru.dev.kisstymelusi.utils.BookBuilder;

public class PassportInteractListener implements Listener {

    private final PassportPlugin plugin;

    public PassportInteractListener(PassportPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Проверяем, что это правый клик по предмету
        if (!event.getAction().toString().contains("RIGHT_CLICK")) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        // Проверяем, является ли предмет паспортом
        if (!BookBuilder.isPassportBook(item)) return;

        // Отменяем стандартное открытие книги
        event.setCancelled(true);

        // Выполняем команду /passport open от имени игрока
        Player player = event.getPlayer();
        player.performCommand("passport open");
    }
}