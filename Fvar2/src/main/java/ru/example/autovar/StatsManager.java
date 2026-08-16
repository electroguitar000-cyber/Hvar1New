package ru.example.autovar;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class StatsManager {

    private final AutoVarHockey plugin;
    private final Map<String, String> teamNames = new HashMap<>();
    private final Map<String, Set<UUID>> teamPlayers = new HashMap<>();
    private final Map<UUID, int[]> playerStats = new HashMap<>();
    private final Map<UUID, String> playerColors = new HashMap<>();

    public StatsManager(AutoVarHockey plugin) {
        this.plugin = plugin;
        teamNames.put("L", "Левая команда");
        teamNames.put("R", "Правая команда");
        teamPlayers.put("L", new HashSet<>());
        teamPlayers.put("R", new HashSet<>());

        Bukkit.getScheduler().runTaskTimer(plugin, this::updatePlayerColors, 200L, 200L);
    }

    public void setPlayerColor(String side, String playerName, String color) {
        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(playerName);
        if (op == null || !op.hasPlayedBefore()) return;
        Set<UUID> set = teamPlayers.get(side.toUpperCase());
        if (set == null || !set.contains(op.getUniqueId())) return;
        playerColors.put(op.getUniqueId(), color);
    }

    private void updatePlayerColors() {
        for (String side : List.of("L", "R")) {
            Location center = side.equals("L") ? plugin.leftGate.getPos1() : plugin.rightGate.getPos1();
            if (center == null) continue;

            for (UUID uuid : teamPlayers.getOrDefault(side, Collections.emptySet())) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.getWorld().equals(center.getWorld()) && p.getLocation().distance(center) <= 100) {
                    playerColors.put(uuid, "green");
                }
            }
        }
    }

    public boolean addPlayerToTeam(String side, String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(playerName);
        if (op == null || !op.hasPlayedBefore()) return false;
        teamPlayers.computeIfAbsent(side.toUpperCase(), k -> new HashSet<>()).add(op.getUniqueId());
        playerStats.putIfAbsent(op.getUniqueId(), new int[]{0, 0});
        playerColors.put(op.getUniqueId(), "gray");
        return true;
    }

    public boolean removePlayerFromTeam(String side, String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(playerName);
        if (op == null) return false;
        Set<UUID> set = teamPlayers.get(side.toUpperCase());
        if (set != null) {
            set.remove(op.getUniqueId());
            playerStats.remove(op.getUniqueId());
            playerColors.remove(op.getUniqueId());
            return true;
        }
        return false;
    }

    public void clearAll() {
        teamNames.clear();
        teamNames.put("L", "Левая команда");
        teamNames.put("R", "Правая команда");
        teamPlayers.clear();
        teamPlayers.put("L", new HashSet<>());
        teamPlayers.put("R", new HashSet<>());
        playerStats.clear();
        playerColors.clear();
    }

    public void addGoal(UUID uuid) {
        playerStats.computeIfAbsent(uuid, k -> new int[]{0, 0})[0]++;
    }

    public void addAssist(UUID uuid) {
        playerStats.computeIfAbsent(uuid, k -> new int[]{0, 0})[1]++;
    }

    public void removeGoal(UUID uuid) {
        int[] s = playerStats.getOrDefault(uuid, new int[]{0, 0});
        if (s[0] > 0) s[0]--;
    }

    public void removeAssist(UUID uuid) {
        int[] s = playerStats.getOrDefault(uuid, new int[]{0, 0});
        if (s[1] > 0) s[1]--;
    }

    public String getTeamSide(UUID uuid) {
        if (teamPlayers.get("L").contains(uuid)) return "L";
        if (teamPlayers.get("R").contains(uuid)) return "R";
        return null;
    }

    public Set<UUID> getTeamPlayers(String side) {
        return teamPlayers.getOrDefault(side.toUpperCase(), Collections.emptySet());
    }

    public String getStatsList() {
        StringBuilder sb = new StringBuilder();
        sb.append(MessageManager.colorize(plugin.messageManager.getMessage("stats.header"))).append("\n");

        // Получаем общий счет
        int[] totalScore = getTotalScore();
        String leftName = getTeamName("L");
        String rightName = getTeamName("R");

        // Выводим счет
        sb.append(MessageManager.colorize("§6§l" + rightName + " §e" + totalScore[1] + "§f:§e" + totalScore[0] + " §6§l" + leftName)).append("\n");
        sb.append(MessageManager.colorize("§7§m----------------------------------------")).append("\n");

        for (String side : List.of("L", "R")) {
            String name = getTeamName(side);
            sb.append("§6§l").append(name).append("\n");
            Set<UUID> players = teamPlayers.getOrDefault(side, Collections.emptySet());
            if (players.isEmpty()) {
                sb.append(MessageManager.colorize(plugin.messageManager.getMessage("stats.no_players"))).append("\n");
            } else {
                for (UUID uuid : players) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    String nick = op.getName() != null ? op.getName() : "Неизвестный";
                    String color = playerColors.getOrDefault(uuid, "gray");
                    String colorCode = color.equals("green") ? "§a" : color.equals("red") ? "§c" : "§7";
                    int[] s = playerStats.getOrDefault(uuid, new int[]{0, 0});

                    String line = plugin.messageManager.getMessage("stats.player_line")
                            .replace("{color}", colorCode)
                            .replace("{player}", nick)
                            .replace("{goals}", String.valueOf(s[0]))
                            .replace("{assists}", String.valueOf(s[1]));
                    sb.append(MessageManager.colorize(line)).append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public String getTeamName(String side) {
        return teamNames.getOrDefault(side.toUpperCase(), side.toUpperCase() + " команда");
    }

    public void setTeamName(String side, String name) {
        teamNames.put(side.toUpperCase(), name);
    }

    public void saveToConfig(FileConfiguration config) {
        config.set("team-names.L", teamNames.get("L"));
        config.set("team-names.R", teamNames.get("R"));

        for (String side : teamPlayers.keySet()) {
            List<String> names = new ArrayList<>();
            for (UUID uuid : teamPlayers.get(side)) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                if (op.getName() != null) names.add(op.getName());
            }
            config.set("teams." + side, names);
        }

        for (UUID uuid : playerStats.keySet()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (op.getName() != null) {
                int[] s = playerStats.get(uuid);
                config.set("stats." + op.getName() + ".goals", s[0]);
                config.set("stats." + op.getName() + ".assists", s[1]);
            }
        }

        for (UUID uuid : playerColors.keySet()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (op.getName() != null) {
                config.set("colors." + op.getName(), playerColors.get(uuid));
            }
        }
    }

    public int[] getTotalScore() {
        int leftGoals = 0;
        int rightGoals = 0;

        for (UUID uuid : teamPlayers.getOrDefault("L", Collections.emptySet())) {
            int[] stats = playerStats.getOrDefault(uuid, new int[]{0, 0});
            leftGoals += stats[0];
        }

        for (UUID uuid : teamPlayers.getOrDefault("R", Collections.emptySet())) {
            int[] stats = playerStats.getOrDefault(uuid, new int[]{0, 0});
            rightGoals += stats[0];
        }

        return new int[]{leftGoals, rightGoals};
    }

    public void loadFromConfig(FileConfiguration config) {
        if (config.contains("team-names.L")) teamNames.put("L", config.getString("team-names.L"));
        if (config.contains("team-names.R")) teamNames.put("R", config.getString("team-names.R"));

        for (String side : List.of("L", "R")) {
            if (config.contains("teams." + side)) {
                for (String name : config.getStringList("teams." + side)) {
                    addPlayerToTeam(side, name);
                }
            }
        }

        if (config.contains("stats")) {
            for (String key : config.getConfigurationSection("stats").getKeys(false)) {
                OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(key);
                if (op != null && op.hasPlayedBefore()) {
                    int goals = config.getInt("stats." + key + ".goals", 0);
                    int assists = config.getInt("stats." + key + ".assists", 0);
                    playerStats.put(op.getUniqueId(), new int[]{goals, assists});
                }
            }
        }

        if (config.contains("colors")) {
            for (String key : config.getConfigurationSection("colors").getKeys(false)) {
                OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(key);
                if (op != null) {
                    playerColors.put(op.getUniqueId(), config.getString("colors." + key));
                }
            }
        }
    }
}