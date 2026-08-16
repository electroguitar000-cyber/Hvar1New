package ru.dev.koramikon.ftime.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.dev.koramikon.ftime.FTime;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Storage {

    private final FTime plugin;
    private final Map<String, PlayerCards> cards = new HashMap<>();
    private final List<CardRecord> history = new LinkedList<>();

    private File cardsFile;
    private FileConfiguration cardsConfig;

    public Storage(FTime plugin) {
        this.plugin = plugin;
        loadData();
    }

    public void addCard(String playerName, String type, String reason, String issuer) {
        PlayerCards pc = cards.computeIfAbsent(playerName.toLowerCase(), k -> new PlayerCards());
        pc.addCard(type, reason, issuer);

        CardRecord rec = new CardRecord(type, playerName, reason, issuer, System.currentTimeMillis());
        history.add(0, rec);
        while (history.size() > plugin.getMaxHistory()) history.remove(history.size() - 1);

        String cardColor = plugin.getMessage("card." + type);
        String entry = plugin.getMessage("history.entry",
                "color", cardColor,
                "type", type,
                "player", playerName,
                "reason", reason,
                "time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                "issuer", issuer);
        plugin.addHistory("cards", entry);

        saveData();
    }

    public void removeCard(String playerName, String type) {
        PlayerCards pc = cards.get(playerName.toLowerCase());
        if (pc != null) {
            pc.removeCard(type);
            saveData();
        }
    }

    private void loadData() {
        cardsFile = new File(plugin.getDataFolder(), "cards.yml");
        if (!cardsFile.exists()) {
            try {
                cardsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать cards.yml: " + e.getMessage());
            }
        }
        cardsConfig = YamlConfiguration.loadConfiguration(cardsFile);
    }

    public void save() {
        saveData();
    }

    public void saveData() {
        if (cardsConfig == null) {
            cardsConfig = new YamlConfiguration();
        }

        for (Map.Entry<String, PlayerCards> entry : cards.entrySet()) {
            String path = "cards." + entry.getKey();
            List<Map<String, Object>> cardList = new ArrayList<>();
            for (CardRecord record : entry.getValue().getRecords()) {
                Map<String, Object> cardData = new HashMap<>();
                cardData.put("type", record.type());
                cardData.put("reason", record.reason());
                cardData.put("issuer", record.issuer());
                cardData.put("timestamp", record.timestamp());
                cardList.add(cardData);
            }
            cardsConfig.set(path, cardList);
        }

        List<Map<String, Object>> historyList = new ArrayList<>();
        for (CardRecord record : history) {
            Map<String, Object> historyData = new HashMap<>();
            historyData.put("type", record.type());
            historyData.put("player", record.playerName());
            historyData.put("reason", record.reason());
            historyData.put("issuer", record.issuer());
            historyData.put("timestamp", record.timestamp());
            historyList.add(historyData);
        }
        cardsConfig.set("history", historyList);

        try {
            cardsConfig.save(cardsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить cards.yml: " + e.getMessage());
        }
    }
}