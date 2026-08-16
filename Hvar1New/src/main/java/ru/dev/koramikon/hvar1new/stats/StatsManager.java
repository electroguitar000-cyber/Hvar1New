package ru.dev.koramikon.hvar1new.stats;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import ru.dev.koramikon.hvar1new.util.MessageUtil;

import java.util.*;

public class StatsManager {

    private final int arenaId;
    private final Map<String, String> teamNames = new HashMap<>();
    private final Map<String, Set<UUID>> teamPlayers = new HashMap<>();
    private final Map<UUID, int[]> playerStats = new HashMap<>();
    private final Map<UUID, Integer> playerBlocks = new HashMap<>();
    private final Map<UUID, Long> playerPossession = new HashMap<>();
    private final Map<UUID, Integer> playerClicks = new HashMap<>();
    private final Map<UUID, String> playerColors = new HashMap<>();
    private final Map<String, Integer> teamSaves = new HashMap<>();
    private final Map<String, Integer> teamStvors = new HashMap<>();
    private final Map<String, Integer> teamShots = new HashMap<>(); // <-- НОВОЕ: броски
    private final Map<UUID, Double> blockAccumulator = new HashMap<>();

    public StatsManager(int arenaId) {
        this.arenaId = arenaId;
        teamNames.put("L", "Левая команда");
        teamNames.put("R", "Правая команда");
        teamPlayers.put("L", new HashSet<>());
        teamPlayers.put("R", new HashSet<>());
        teamSaves.put("L", 0);
        teamSaves.put("R", 0);
        teamStvors.put("L", 0);
        teamStvors.put("R", 0);
        teamShots.put("L", 0);   // <-- НОВОЕ
        teamShots.put("R", 0);   // <-- НОВОЕ
    }

    public int getArenaId() { return arenaId; }

    // ======== УПРАВЛЕНИЕ КОМАНДАМИ ========
    public boolean addPlayerToTeam(String side, String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(playerName);
        if (op == null || !op.hasPlayedBefore()) {
            Bukkit.getLogger().warning("[StatsManager] Игрок " + playerName + " не найден для арены " + arenaId);
            return false;
        }
        Set<UUID> set = teamPlayers.computeIfAbsent(side.toUpperCase(), k -> new HashSet<>());
        if (set.add(op.getUniqueId())) {
            playerStats.putIfAbsent(op.getUniqueId(), new int[]{0, 0});
            playerBlocks.putIfAbsent(op.getUniqueId(), 0);
            playerPossession.putIfAbsent(op.getUniqueId(), 0L);
            playerClicks.putIfAbsent(op.getUniqueId(), 0);
            // <-- ИЗМЕНЕНО: теперь сразу КРАСНЫЙ, а не серый
            playerColors.putIfAbsent(op.getUniqueId(), "red");
            Bukkit.getLogger().info("[StatsManager] Игрок " + playerName + " добавлен в команду " + side + " на арену " + arenaId + " (цвет: red)");
            return true;
        }
        return false;
    }

    public boolean removePlayerFromTeam(String side, String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(playerName);
        if (op == null) return false;
        Set<UUID> set = teamPlayers.get(side.toUpperCase());
        if (set != null && set.remove(op.getUniqueId())) {
            playerStats.remove(op.getUniqueId());
            playerBlocks.remove(op.getUniqueId());
            playerPossession.remove(op.getUniqueId());
            playerClicks.remove(op.getUniqueId());
            playerColors.remove(op.getUniqueId());
            return true;
        }
        return false;
    }

    public void clearAll() {
        teamPlayers.get("L").clear();
        teamPlayers.get("R").clear();
        playerStats.clear();
        playerBlocks.clear();
        blockAccumulator.clear();
        playerPossession.clear();
        playerClicks.clear();
        playerColors.clear();
        teamSaves.put("L", 0);
        teamSaves.put("R", 0);
        teamStvors.put("L", 0);
        teamStvors.put("R", 0);
        teamShots.put("L", 0);   // <-- НОВОЕ
        teamShots.put("R", 0);   // <-- НОВОЕ
    }

    // ======== НОВЫЙ МЕТОД: ОЧИСТКА СТАТИСТИКИ (ИГРОКИ ОСТАЮТСЯ) ========
    public void clearPlayerStats() {
        // Обнуляем статистику всех игроков
        for (UUID uuid : playerStats.keySet()) {
            playerStats.put(uuid, new int[]{0, 0});
        }
        // Обнуляем блоки
        for (UUID uuid : playerBlocks.keySet()) {
            playerBlocks.put(uuid, 0);
        }
        // Обнуляем владение
        for (UUID uuid : playerPossession.keySet()) {
            playerPossession.put(uuid, 0L);
        }
        // Обнуляем клики
        for (UUID uuid : playerClicks.keySet()) {
            playerClicks.put(uuid, 0);
        }
        // Обнуляем аккумулятор блоков
        blockAccumulator.clear();
        // Обнуляем командные сейвы, створы и броски
        teamSaves.put("L", 0);
        teamSaves.put("R", 0);
        teamStvors.put("L", 0);
        teamStvors.put("R", 0);
        teamShots.put("L", 0);
        teamShots.put("R", 0);
        // Цвета игроков остаются (они не сбрасываются)
        Bukkit.getLogger().info("[StatsManager] Статистика арены " + arenaId + " очищена (игроки сохранены)");
    }

    public void resetTeam(String side) {
        String key = side.toUpperCase();
        Set<UUID> players = new HashSet<>(teamPlayers.getOrDefault(key, Collections.emptySet()));
        for (UUID uuid : players) {
            removePlayerFromTeam(key, Bukkit.getOfflinePlayer(uuid).getName());
        }
        teamSaves.put(key, 0);
        teamStvors.put(key, 0);
        teamShots.put(key, 0);
    }

    // ======== ГЕТТЕРЫ ========
    public String getTeamName(String side) {
        return teamNames.getOrDefault(side.toUpperCase(), side.toUpperCase() + " команда");
    }

    public void setTeamName(String side, String name) {
        teamNames.put(side.toUpperCase(), name);
    }

    public Set<UUID> getTeamPlayers(String side) {
        return teamPlayers.getOrDefault(side.toUpperCase(), Collections.emptySet());
    }

    public String getTeamSide(UUID uuid) {
        if (teamPlayers.get("L").contains(uuid)) return "L";
        if (teamPlayers.get("R").contains(uuid)) return "R";
        return null;
    }

    // ======== СТАТИСТИКА ========
    public void addGoal(UUID uuid) {
        playerStats.computeIfAbsent(uuid, k -> new int[]{0, 0})[0]++;
    }

    public void addAssist(UUID uuid) {
        playerStats.computeIfAbsent(uuid, k -> new int[]{0, 0})[1]++;
    }

    public void removeGoal(UUID uuid) {
        int[] stats = playerStats.get(uuid);
        if (stats != null && stats[0] > 0) stats[0]--;
    }

    public void removeAssist(UUID uuid) {
        int[] stats = playerStats.get(uuid);
        if (stats != null && stats[1] > 0) stats[1]--;
    }

    public int[] getStats(UUID uuid) {
        return playerStats.getOrDefault(uuid, new int[]{0, 0});
    }

    // ======== БЛОКИ ========
    public void addBlocks(UUID uuid, double distance) {
        if (distance <= 0) return;
        double accumulated = blockAccumulator.getOrDefault(uuid, 0.0) + distance;
        int wholeBlocks = (int) Math.floor(accumulated);
        if (wholeBlocks > 0) {
            int current = playerBlocks.getOrDefault(uuid, 0);
            playerBlocks.put(uuid, current + wholeBlocks);
            blockAccumulator.put(uuid, accumulated - wholeBlocks);
        } else {
            blockAccumulator.put(uuid, accumulated);
        }
    }

    public int getBlocks(UUID uuid) {
        return playerBlocks.getOrDefault(uuid, 0);
    }

    // ======== ВРЕМЯ ВЛАДЕНИЯ ========
    public void addPossessionTime(UUID uuid, long millis) {
        if (millis <= 0) return;
        long current = playerPossession.getOrDefault(uuid, 0L);
        playerPossession.put(uuid, current + millis);
    }

    public long getPossessionTime(UUID uuid) {
        return playerPossession.getOrDefault(uuid, 0L);
    }

    public long getTeamTotalPossession(String side) {
        long total = 0;
        for (UUID uuid : teamPlayers.getOrDefault(side.toUpperCase(), Collections.emptySet())) {
            total += getPossessionTime(uuid);
        }
        return total;
    }

    // ======== КЛИКИ ========
    public void addClick(UUID uuid) {
        int current = playerClicks.getOrDefault(uuid, 0);
        playerClicks.put(uuid, current + 1);
    }

    public int getClicks(UUID uuid) {
        return playerClicks.getOrDefault(uuid, 0);
    }

    // ======== СЕЙВЫ ========
    public void addSave(String side) {
        String key = side.toUpperCase();
        teamSaves.put(key, teamSaves.getOrDefault(key, 0) + 1);
    }

    public int getSaves(String side) {
        return teamSaves.getOrDefault(side.toUpperCase(), 0);
    }

    // ======== СТВОРЫ ========
    public void addStvor(String side) {
        String key = side.toUpperCase();
        teamStvors.put(key, teamStvors.getOrDefault(key, 0) + 1);
    }

    public int getStvors(String side) {
        return teamStvors.getOrDefault(side.toUpperCase(), 0);
    }

    // ======== НОВОЕ: БРОСКИ (СТВОРЫ + СЕЙВЫ + ГОЛЫ) ========
    public void updateShots(String side) {
        String key = side.toUpperCase();
        int goals = 0;
        int saves = teamSaves.getOrDefault(key, 0);
        int stvors = teamStvors.getOrDefault(key, 0);
        // Считаем голы, забитые в эти ворота (противоположная команда)
        String opposite = key.equals("L") ? "R" : "L";
        for (UUID uuid : teamPlayers.getOrDefault(opposite, Collections.emptySet())) {
            goals += playerStats.getOrDefault(uuid, new int[]{0, 0})[0];
        }
        int total = goals + saves + stvors;
        teamShots.put(key, total);
    }

    public int getShots(String side) {
        return teamShots.getOrDefault(side.toUpperCase(), 0);
    }

    // ======== ЦВЕТА ========
    public void setPlayerColor(String side, String playerName, String color) {
        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(playerName);
        if (op == null) return;
        Set<UUID> set = teamPlayers.get(side.toUpperCase());
        if (set != null && set.contains(op.getUniqueId())) {
            playerColors.put(op.getUniqueId(), color.toLowerCase());
        }
    }

    public void setPlayerColorRaw(UUID uuid, String color) {
        playerColors.put(uuid, color);
    }

    public String getPlayerColor(UUID uuid) {
        return playerColors.getOrDefault(uuid, "gray");
    }

    // ======== ОБЩИЙ СЧЁТ ========
    public int[] getTotalScore() {
        int leftGoals = 0, rightGoals = 0;
        for (UUID uuid : teamPlayers.get("L")) {
            leftGoals += playerStats.getOrDefault(uuid, new int[]{0, 0})[0];
        }
        for (UUID uuid : teamPlayers.get("R")) {
            rightGoals += playerStats.getOrDefault(uuid, new int[]{0, 0})[0];
        }
        return new int[]{leftGoals, rightGoals};
    }

    // ======== СТАТИСТИКА ДЛЯ ЧАТА ========
    public String getStatsList(int arenaId) {
        if (this.arenaId != arenaId) {
            return "&cОшибка: статистика для арены " + arenaId + " не найдена!";
        }
        // Обновляем броски перед выводом
        updateShots("L");
        updateShots("R");

        StringBuilder sb = new StringBuilder();
        sb.append(MessageUtil.colorize(MessageUtil.getRaw("stats-list-header").replace("{arena}", String.valueOf(arenaId)))).append("\n");
        int[] score = getTotalScore();
        String leftName = getTeamName("L");
        String rightName = getTeamName("R");
        sb.append(MessageUtil.colorize(MessageUtil.getRaw("stats-list-score")
                .replace("{rightName}", rightName)
                .replace("{rightScore}", String.valueOf(score[1]))
                .replace("{leftScore}", String.valueOf(score[0]))
                .replace("{leftName}", leftName))).append("\n");
        sb.append(MessageUtil.colorize(MessageUtil.getRaw("stats-list-divider"))).append("\n");

        for (String side : List.of("L", "R")) {
            sb.append(MessageUtil.colorize(MessageUtil.getRaw("stats-list-team-header").replace("{teamName}", getTeamName(side)))).append("\n");
            Set<UUID> players = getTeamPlayers(side);
            if (players.isEmpty()) {
                sb.append(MessageUtil.colorize("  " + MessageUtil.getRaw("stats-list-no-players"))).append("\n");
            } else {
                long teamTotal = getTeamTotalPossession(side);
                for (UUID uuid : players) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    String name = op.getName() != null ? op.getName() : "???";
                    String colorCode = switch (getPlayerColor(uuid)) {
                        case "green" -> "&a";
                        case "red" -> "&c";
                        default -> "&7";
                    };
                    int[] stats = getStats(uuid);
                    int blocks = getBlocks(uuid);
                    long possession = getPossessionTime(uuid);
                    int percent = (teamTotal > 0) ? (int) Math.round((possession * 100.0) / teamTotal) : 0;
                    int clicks = getClicks(uuid);
                    String line = MessageUtil.getRaw("stats-list-player-format")
                            .replace("{color}", colorCode)
                            .replace("{player}", name)
                            .replace("{goals}", String.valueOf(stats[0]))
                            .replace("{assists}", String.valueOf(stats[1]))
                            .replace("{blocks}", String.valueOf(blocks))
                            .replace("{possession_percent}", String.valueOf(percent))
                            .replace("{clicks}", String.valueOf(clicks));
                    sb.append(MessageUtil.colorize("  " + line)).append("\n");
                }
                long teamSeconds = teamTotal / 1000;
                sb.append(MessageUtil.colorize("  " + MessageUtil.getRaw("stats-list-team-possession")
                        .replace("{side}", side)
                        .replace("{seconds}", String.valueOf(teamSeconds)))).append("\n");
            }
            // Сейвы
            int saves = getSaves(side);
            sb.append(MessageUtil.colorize("  " + MessageUtil.getRaw("stats-list-team-saves")
                    .replace("{side}", side)
                    .replace("{saves}", String.valueOf(saves)))).append("\n");
            // Створы
            int stvors = getStvors(side);
            sb.append(MessageUtil.colorize("  " + MessageUtil.getRaw("stats-list-team-stvors")
                    .replace("{side}", side)
                    .replace("{stvors}", String.valueOf(stvors)))).append("\n");
            // Броски (НОВОЕ)
            int shots = getShots(side);
            sb.append(MessageUtil.colorize("  " + MessageUtil.getRaw("stats-list-team-shots")
                    .replace("{side}", side)
                    .replace("{shots}", String.valueOf(shots)))).append("\n");
            sb.append("\n");
        }
        return sb.toString();
    }

    // ======== ПЛЕЙСХОЛДЕР (две колонки) ========
    public String getPlaceholderStats(int arenaId) {
        if (this.arenaId != arenaId) {
            return "&cОшибка: плейсхолдер для арены " + arenaId + " не найден!";
        }
        // Обновляем броски
        updateShots("L");
        updateShots("R");

        int[] score = getTotalScore();
        String leftName = getTeamName("L");
        String rightName = getTeamName("R");
        String scoreLine = MessageUtil.getRaw("stats-list-score")
                .replace("{rightName}", rightName)
                .replace("{rightScore}", String.valueOf(score[1]))
                .replace("{leftScore}", String.valueOf(score[0]))
                .replace("{leftName}", leftName);
        StringBuilder sb = new StringBuilder();
        sb.append(MessageUtil.colorize(scoreLine)).append("\n");

        List<String> leftLines = getTeamLines("R");
        List<String> rightLines = getTeamLines("L");

        int maxLeftWidth = 0;
        for (String line : leftLines) {
            int width = ChatColor.stripColor(line).length();
            if (width > maxLeftWidth) maxLeftWidth = width;
        }

        int maxLines = Math.max(leftLines.size(), rightLines.size());
        for (int i = 0; i < maxLines; i++) {
            String left = i < leftLines.size() ? leftLines.get(i) : "";
            String right = i < rightLines.size() ? rightLines.get(i) : "";
            int leftLen = ChatColor.stripColor(left).length();
            int padding = maxLeftWidth - leftLen;
            sb.append(left);
            for (int j = 0; j < padding; j++) sb.append(' ');
            sb.append("  ");
            sb.append(right);
            if (i < maxLines - 1) sb.append("\n");
        }
        return sb.toString();
    }

    private List<String> getTeamLines(String side) {
        List<String> lines = new ArrayList<>();
        String name = getTeamName(side);
        lines.add(MessageUtil.colorize("&6&l" + name));
        Set<UUID> players = getTeamPlayers(side);
        if (players.isEmpty()) {
            lines.add(MessageUtil.colorize("  &7Нет игроков"));
        } else {
            long teamTotal = getTeamTotalPossession(side);
            for (UUID uuid : players) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                String namePlayer = op.getName() != null ? op.getName() : "???";
                String colorCode = switch (getPlayerColor(uuid)) {
                    case "green" -> "&a";
                    case "red" -> "&c";
                    default -> "&7";
                };
                int[] stats = getStats(uuid);
                int blocks = getBlocks(uuid);
                long possession = getPossessionTime(uuid);
                int percent = (teamTotal > 0) ? (int) Math.round((possession * 100.0) / teamTotal) : 0;
                int clicks = getClicks(uuid);
                String line = MessageUtil.getRaw("stats-list-player-format")
                        .replace("{color}", colorCode)
                        .replace("{player}", namePlayer)
                        .replace("{goals}", String.valueOf(stats[0]))
                        .replace("{assists}", String.valueOf(stats[1]))
                        .replace("{blocks}", String.valueOf(blocks))
                        .replace("{possession_percent}", String.valueOf(percent))
                        .replace("{clicks}", String.valueOf(clicks));
                lines.add(MessageUtil.colorize(line));
            }
        }
        long teamTotal = getTeamTotalPossession(side);
        long teamSeconds = teamTotal / 1000;
        String teamTime = MessageUtil.getRaw("stats-list-team-possession")
                .replace("{side}", side)
                .replace("{seconds}", String.valueOf(teamSeconds));
        lines.add(MessageUtil.colorize(teamTime));
        int saves = getSaves(side);
        String savesLine = MessageUtil.getRaw("stats-list-team-saves")
                .replace("{side}", side)
                .replace("{saves}", String.valueOf(saves));
        lines.add(MessageUtil.colorize(savesLine));
        int stvors = getStvors(side);
        String stvorsLine = MessageUtil.getRaw("stats-list-team-stvors")
                .replace("{side}", side)
                .replace("{stvors}", String.valueOf(stvors));
        lines.add(MessageUtil.colorize(stvorsLine));
        int shots = getShots(side);
        String shotsLine = MessageUtil.getRaw("stats-list-team-shots")
                .replace("{side}", side)
                .replace("{shots}", String.valueOf(shots));
        lines.add(MessageUtil.colorize(shotsLine));
        return lines;
    }

    // ======== ЗАГРУЗКА / СОХРАНЕНИЕ ========
    public void loadFromConfig(ConfigurationSection config, String basePath) {
        if (config.contains(basePath + "team-names.L"))
            teamNames.put("L", config.getString(basePath + "team-names.L"));
        if (config.contains(basePath + "team-names.R"))
            teamNames.put("R", config.getString(basePath + "team-names.R"));
        for (String side : List.of("L", "R")) {
            if (config.contains(basePath + "teams." + side)) {
                for (String name : config.getStringList(basePath + "teams." + side)) {
                    addPlayerToTeam(side, name);
                }
            }
        }
        if (config.contains(basePath + "stats")) {
            var statSection = config.getConfigurationSection(basePath + "stats");
            if (statSection != null) {
                for (String key : statSection.getKeys(false)) {
                    OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(key);
                    if (op != null && op.hasPlayedBefore()) {
                        int goals = config.getInt(basePath + "stats." + key + ".goals", 0);
                        int assists = config.getInt(basePath + "stats." + key + ".assists", 0);
                        playerStats.put(op.getUniqueId(), new int[]{goals, assists});
                    }
                }
            }
        }
        if (config.contains(basePath + "blocks")) {
            var blockSection = config.getConfigurationSection(basePath + "blocks");
            if (blockSection != null) {
                for (String key : blockSection.getKeys(false)) {
                    OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(key);
                    if (op != null && op.hasPlayedBefore()) {
                        int blocks = config.getInt(basePath + "blocks." + key, 0);
                        playerBlocks.put(op.getUniqueId(), blocks);
                    }
                }
            }
        }
        if (config.contains(basePath + "possession")) {
            var possSection = config.getConfigurationSection(basePath + "possession");
            if (possSection != null) {
                for (String key : possSection.getKeys(false)) {
                    OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(key);
                    if (op != null && op.hasPlayedBefore()) {
                        long millis = config.getLong(basePath + "possession." + key, 0);
                        playerPossession.put(op.getUniqueId(), millis);
                    }
                }
            }
        }
        if (config.contains(basePath + "clicks")) {
            var clickSection = config.getConfigurationSection(basePath + "clicks");
            if (clickSection != null) {
                for (String key : clickSection.getKeys(false)) {
                    OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(key);
                    if (op != null && op.hasPlayedBefore()) {
                        int clicks = config.getInt(basePath + "clicks." + key, 0);
                        playerClicks.put(op.getUniqueId(), clicks);
                    }
                }
            }
        }
        if (config.contains(basePath + "saves")) {
            var savesSection = config.getConfigurationSection(basePath + "saves");
            if (savesSection != null) {
                if (savesSection.contains("L")) teamSaves.put("L", savesSection.getInt("L"));
                if (savesSection.contains("R")) teamSaves.put("R", savesSection.getInt("R"));
            }
        }
        if (config.contains(basePath + "stvors")) {
            var stvorsSection = config.getConfigurationSection(basePath + "stvors");
            if (stvorsSection != null) {
                if (stvorsSection.contains("L")) teamStvors.put("L", stvorsSection.getInt("L"));
                if (stvorsSection.contains("R")) teamStvors.put("R", stvorsSection.getInt("R"));
            }
        }
        if (config.contains(basePath + "shots")) {
            var shotsSection = config.getConfigurationSection(basePath + "shots");
            if (shotsSection != null) {
                if (shotsSection.contains("L")) teamShots.put("L", shotsSection.getInt("L"));
                if (shotsSection.contains("R")) teamShots.put("R", shotsSection.getInt("R"));
            }
        }
        if (config.contains(basePath + "colors")) {
            var colorSection = config.getConfigurationSection(basePath + "colors");
            if (colorSection != null) {
                for (String key : colorSection.getKeys(false)) {
                    OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(key);
                    if (op != null) {
                        playerColors.put(op.getUniqueId(), config.getString(basePath + "colors." + key));
                    }
                }
            }
        }
    }

    public void saveToConfig(ConfigurationSection config, String basePath) {
        config.set(basePath + "team-names.L", teamNames.get("L"));
        config.set(basePath + "team-names.R", teamNames.get("R"));
        for (String side : teamPlayers.keySet()) {
            List<String> names = new ArrayList<>();
            for (UUID uuid : teamPlayers.get(side)) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                if (op.getName() != null) names.add(op.getName());
            }
            config.set(basePath + "teams." + side, names);
        }
        for (Map.Entry<UUID, int[]> entry : playerStats.entrySet()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            if (op.getName() != null) {
                config.set(basePath + "stats." + op.getName() + ".goals", entry.getValue()[0]);
                config.set(basePath + "stats." + op.getName() + ".assists", entry.getValue()[1]);
            }
        }
        for (Map.Entry<UUID, Integer> entry : playerBlocks.entrySet()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            if (op.getName() != null) {
                config.set(basePath + "blocks." + op.getName(), entry.getValue());
            }
        }
        for (Map.Entry<UUID, Long> entry : playerPossession.entrySet()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            if (op.getName() != null) {
                config.set(basePath + "possession." + op.getName(), entry.getValue());
            }
        }
        for (Map.Entry<UUID, Integer> entry : playerClicks.entrySet()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            if (op.getName() != null) {
                config.set(basePath + "clicks." + op.getName(), entry.getValue());
            }
        }
        config.set(basePath + "saves.L", teamSaves.get("L"));
        config.set(basePath + "saves.R", teamSaves.get("R"));
        config.set(basePath + "stvors.L", teamStvors.get("L"));
        config.set(basePath + "stvors.R", teamStvors.get("R"));
        config.set(basePath + "shots.L", teamShots.get("L"));
        config.set(basePath + "shots.R", teamShots.get("R"));
        for (Map.Entry<UUID, String> entry : playerColors.entrySet()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            if (op.getName() != null) {
                config.set(basePath + "colors." + op.getName(), entry.getValue());
            }
        }
    }
}