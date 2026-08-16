package ru.dev.kisstymelusi.managers;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.dev.kisstymelusi.PassportPlugin;
import ru.dev.kisstymelusi.models.DialogState;
import ru.dev.kisstymelusi.models.DialogStep;
import ru.dev.kisstymelusi.models.PassportData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DialogManager {

    private final PassportPlugin plugin;
    private final Map<UUID, DialogState> dialogs;

    public DialogManager(PassportPlugin plugin) {
        this.plugin = plugin;
        this.dialogs = new HashMap<>();
    }

    public void startDialog(Player target, CommandSender sender) {
        if (plugin.getPassportManager().hasPassport(target.getUniqueId())) {
            sender.sendMessage(plugin.getMessageManager().getMessage("already-has-passport"));
            return;
        }

        DialogState state = new DialogState();
        state.setPlayer(target);
        state.setStep(DialogStep.ASK_NAME);
        state.setEditMode(false);
        dialogs.put(target.getUniqueId(), state);

        target.sendMessage(plugin.getMessageManager().getMessage("ask-name"));
        sender.sendMessage(plugin.getMessageManager().getMessage("create-started").replace("{player}", target.getName()));
    }

    public void startEditDialog(Player player) {
        if (!plugin.getPassportManager().hasPassport(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-passport"));
            return;
        }

        DialogState state = new DialogState();
        state.setPlayer(player);
        state.setStep(DialogStep.ASK_NAME);
        state.setEditMode(true);
        // Сохраняем старые данные для возможного отката?
        dialogs.put(player.getUniqueId(), state);

        player.sendMessage(plugin.getMessageManager().getMessage("edit-started"));
        player.sendMessage(plugin.getMessageManager().getMessage("ask-name"));
    }

    public boolean isInDialog(Player player) {
        return dialogs.containsKey(player.getUniqueId());
    }

    public void handleAnswer(Player player, String message) {
        DialogState state = dialogs.get(player.getUniqueId());
        if (state == null) return;

        switch (state.getStep()) {
            case ASK_NAME:
                handleName(player, message, state);
                break;
            case ASK_AGE:
                handleAge(player, message, state);
                break;
            case ASK_GENDER:
                handleGender(player, message, state);
                break;
            case ASK_MARRIED:
                handleMarried(player, message, state);
                break;
            default:
                break;
        }
    }

    private void handleName(Player player, String msg, DialogState state) {
        boolean allowEnglish = plugin.getConfigManager().isAllowEnglishName();
        String regex = allowEnglish ? "^[a-zA-Zа-яА-ЯёЁ_]+$" : "^[а-яА-ЯёЁ_]+$";

        if (!msg.matches(regex) || !msg.contains("_")) {
            player.sendMessage(plugin.getMessageManager().getMessage("invalid-name"));
            return;
        }

        String fullName = msg.replace('_', ' ');
        state.setFullName(fullName);
        state.setStep(DialogStep.ASK_AGE);
        player.sendMessage(plugin.getMessageManager().getMessage("ask-age"));
    }

    private void handleAge(Player player, String msg, DialogState state) {
        if (!msg.matches("\\d+")) {
            player.sendMessage(plugin.getMessageManager().getMessage("invalid-age"));
            return;
        }

        int age = Integer.parseInt(msg);
        String limit = plugin.getConfigManager().getAgeLimit();
        String[] parts = limit.split("-");
        if (parts.length != 2) parts = new String[]{"1", "99"};

        try {
            int min = Integer.parseInt(parts[0]);
            int max = Integer.parseInt(parts[1]);
            if (age < min || age > max) {
                player.sendMessage(plugin.getMessageManager().getMessage("invalid-age"));
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getMessage("invalid-age"));
            return;
        }

        state.setAge(age);
        state.setStep(DialogStep.ASK_GENDER);
        player.sendMessage(plugin.getMessageManager().getMessage("ask-gender"));
    }

    private void handleGender(Player player, String msg, DialogState state) {
        String[] options = plugin.getConfigManager().getGenderOptions().split(" ");
        boolean valid = false;
        for (String opt : options) {
            if (opt.equalsIgnoreCase(msg)) {
                valid = true;
                state.setGender(opt);
                break;
            }
        }
        if (!valid) {
            player.sendMessage(plugin.getMessageManager().getMessage("invalid-gender")
                    .replace("{options}", plugin.getConfigManager().getGenderOptions()));
            return;
        }

        state.setStep(DialogStep.ASK_MARRIED);
        player.sendMessage(plugin.getMessageManager().getMessage("ask-married"));
    }

    private void handleMarried(Player player, String msg, DialogState state) {
        String[] options = plugin.getConfigManager().getMarriedOptions().split(" ");
        boolean valid = false;
        for (String opt : options) {
            if (opt.equalsIgnoreCase(msg)) {
                valid = true;
                state.setMarried(opt);
                break;
            }
        }
        if (!valid) {
            player.sendMessage(plugin.getMessageManager().getMessage("invalid-married")
                    .replace("{options}", plugin.getConfigManager().getMarriedOptions()));
            return;
        }

        // Завершаем диалог
        if (state.isEditMode()) {
            updatePassport(player, state);
        } else {
            createPassport(player, state);
        }
    }

    private void createPassport(Player player, DialogState state) {
        PassportData data = new PassportData();
        data.setPlayerName(player.getName());
        data.setFullName(state.getFullName());
        data.setAge(state.getAge());
        data.setGender(state.getGender());
        data.setCity(plugin.getConfigManager().getDefaultCity());
        data.setMarried(state.getMarried());
        data.setIssueDate(System.currentTimeMillis());
        data.setExpiryDays(plugin.getConfigManager().getExpiryDays());

        plugin.getPassportManager().savePassport(player.getUniqueId(), data);
        plugin.giveBook(player);
        player.sendMessage(plugin.getMessageManager().getMessage("passport-created"));

        dialogs.remove(player.getUniqueId());
    }

    private void updatePassport(Player player, DialogState state) {
        PassportData oldData = plugin.getPassportManager().getPassport(player.getUniqueId());
        if (oldData == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-passport"));
            dialogs.remove(player.getUniqueId());
            return;
        }

        // Обновляем только поля, оставляем дату и срок
        oldData.setFullName(state.getFullName());
        oldData.setAge(state.getAge());
        oldData.setGender(state.getGender());
        oldData.setMarried(state.getMarried());
        // Город не меняем (берётся из конфига, но можно оставить старый)
        // Можно разрешить менять город через конфиг, но по ТЗ не требуется.

        plugin.getPassportManager().savePassport(player.getUniqueId(), oldData);
        // Удаляем старую книгу из инвентаря
        plugin.removePassportFromInventory(player);
        // Выдаём новую
        plugin.giveBook(player);
        player.sendMessage(plugin.getMessageManager().getMessage("edit-success"));

        dialogs.remove(player.getUniqueId());
    }

    public void cancelDialog(Player player) {
        dialogs.remove(player.getUniqueId());
    }
}