package ru.dev.kisstymelusi.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import ru.dev.kisstymelusi.PassportPlugin;
import ru.dev.kisstymelusi.managers.MessageManager;
import ru.dev.kisstymelusi.models.PassportData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BookBuilder {

    private final PassportPlugin plugin;
    private final UUID playerUUID;

    public BookBuilder(PassportPlugin plugin, UUID playerUUID) {
        this.plugin = plugin;
        this.playerUUID = playerUUID;
    }

    public ItemStack build() {
        PassportData data = plugin.getPassportManager().getPassport(playerUUID);
        if (data == null) return null;

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return null;

        String title = MessageManager.colorize(plugin.getMessageManager().getRawMessage("book-title"));
        String author = MessageManager.colorize(plugin.getMessageManager().getRawMessage("book-author"));
        meta.setTitle(title != null ? title : "Паспорт");
        meta.setAuthor(author != null ? author : "Passport System");

        List<String> contentLines = plugin.getMessageManager().getRawMessageList("book-content");
        if (contentLines == null || contentLines.isEmpty()) {
            contentLines = new ArrayList<>();
            contentLines.add("Имя: {fullName}");
            contentLines.add("Возраст: {age}");
            contentLines.add("Пол: {gender}");
            contentLines.add("Город: {city}");
            contentLines.add("Женат/Замужем: {married}");
            contentLines.add("Владелец: {playerName}");
            contentLines.add("Дата: {issueDate}");
            contentLines.add("Срок: {expiryDays} дней");
            contentLines.add("Статус: {status}");
        }

        String timezone = plugin.getConfigManager().getTimezone();
        String issueDate = plugin.getPassportManager().getIssueDateString(playerUUID, timezone);
        boolean expired = plugin.getPassportManager().isExpired(playerUUID);
        String status = expired ? "Просрочен" : "Действителен";

        List<String> formattedLines = new ArrayList<>();
        for (String line : contentLines) {
            line = line.replace("{fullName}", data.getFullName())
                    .replace("{age}", String.valueOf(data.getAge()))
                    .replace("{gender}", data.getGender())
                    .replace("{city}", data.getCity())
                    .replace("{married}", data.getMarried())
                    .replace("{playerName}", data.getPlayerName())
                    .replace("{issueDate}", issueDate)
                    .replace("{expiryDays}", String.valueOf(data.getExpiryDays()))
                    .replace("{status}", status);
            formattedLines.add(MessageManager.colorize(line));
        }

        String fullText = String.join("\n", formattedLines);
        List<String> pages = new ArrayList<>();
        if (fullText.length() <= 200) {
            pages.add(fullText);
        } else {
            StringBuilder page = new StringBuilder();
            for (String line : formattedLines) {
                if (page.length() + line.length() + 1 > 200) {
                    pages.add(page.toString());
                    page = new StringBuilder(line);
                } else {
                    if (page.length() > 0) page.append("\n");
                    page.append(line);
                }
            }
            if (page.length() > 0) pages.add(page.toString());
        }

        meta.setPages(pages);
        book.setItemMeta(meta);
        return book;
    }

    public static boolean isPassportBook(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK) return false;
        BookMeta meta = (BookMeta) item.getItemMeta();
        if (meta == null) return false;
        // Проверяем по автору или названию
        String author = meta.getAuthor();
        String title = meta.getTitle();
        return (author != null && author.contains("Passport")) ||
                (title != null && title.equalsIgnoreCase("Паспорт"));
    }
}