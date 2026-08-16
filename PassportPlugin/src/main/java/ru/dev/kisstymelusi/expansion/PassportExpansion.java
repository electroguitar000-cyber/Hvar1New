package ru.dev.kisstymelusi.expansion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.dev.kisstymelusi.PassportPlugin;
import ru.dev.kisstymelusi.managers.PassportManager;
import ru.dev.kisstymelusi.models.PassportData;

public class PassportExpansion extends PlaceholderExpansion {

    private final PassportPlugin plugin;
    private final PassportManager passportManager;

    public PassportExpansion(PassportPlugin plugin) {
        this.plugin = plugin;
        this.passportManager = plugin.getPassportManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "passport";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Kisstymelusi";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return "Нет данных";
        }
        Player player = offlinePlayer.getPlayer();
        if (player == null) return "Нет данных";

        if (!passportManager.hasPassport(player.getUniqueId())) {
            return "Нет паспорта";
        }

        PassportData data = passportManager.getPassport(player.getUniqueId());
        if (data == null) return "Ошибка";

        switch (params.toLowerCase()) {
            case "name":
                return data.getFullName();
            case "age":
                return String.valueOf(data.getAge());
            case "gender":
                return data.getGender();
            case "city":
                return data.getCity();
            case "married":
                return data.getMarried();
            case "player":
                return data.getPlayerName();
            case "issuedate":
                return passportManager.getIssueDateString(player.getUniqueId(), plugin.getConfigManager().getTimezone());
            case "expirydate":
                return passportManager.getExpiryDateString(player.getUniqueId(), plugin.getConfigManager().getTimezone());
            case "status":
                return passportManager.isExpired(player.getUniqueId()) ? "Просрочен" : "Действителен";
            default:
                return null;
        }
    }
}