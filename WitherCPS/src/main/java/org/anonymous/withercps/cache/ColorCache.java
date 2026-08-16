package org.anonymous.withercps.cache;

import org.anonymous.withercps.WitherCPS;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public final class ColorCache {

    private final ColorResult ZERO = new ColorResult("&7", true);
    private final ColorResult NONE = new ColorResult(null, false);

    private final WitherCPS plugin;
    private Entry[] entries = new Entry[0];
    private FastEntry[] fastEntries = new FastEntry[0];

    private int minClicks = 0;
    private double minTime = 0.0;

    public ColorCache(WitherCPS plugin) {
        this.plugin = plugin;
    }

    public void load() {
        var config = plugin.getConfig();

        minClicks = config.getInt("settings.zero-cps.min-clicks", 4);
        minTime = config.getDouble("settings.zero-cps.min-time", 0.05);

        ConfigurationSection section = config.getConfigurationSection("colors");
        if (section == null) {
            plugin.getLogger().warning("Config section \"colors\" not found.");
            entries = new Entry[0];
        } else {
            List<Entry> entries = new ArrayList<>();

            for (String totalRange : section.getKeys(false)) {
                ConfigurationSection totalSection = section.getConfigurationSection(totalRange);
                if (totalSection == null) continue;

                int dash = totalRange.indexOf('-');
                int minTotal = Integer.parseInt((dash == -1 ? totalRange : totalRange.substring(0, dash)).trim());
                int maxTotal = dash == -1 ? minTotal : Integer.parseInt(totalRange.substring(dash + 1).trim());

                for (String color : totalSection.getKeys(false)) {
                    String cpsRange = totalSection.getString(color);
                    if (cpsRange == null || cpsRange.isBlank()) continue;

                    int cpsDash = cpsRange.indexOf('-');
                    double minCps = Double.parseDouble(cpsRange.substring(0, cpsDash).trim());
                    double maxCps = Double.parseDouble(cpsRange.substring(cpsDash + 1).trim());

                    entries.add(new Entry(minTotal, maxTotal, minCps, maxCps, color));
                }
            }

            this.entries = entries.toArray(Entry[]::new);
        }

        ConfigurationSection fastSection = config.getConfigurationSection("fast-colors");
        if (fastSection == null) {
            fastEntries = new FastEntry[0];
        } else {
            List<FastEntry> fastEntries = new ArrayList<>();

            for (String totalRange : fastSection.getKeys(false)) {
                ConfigurationSection totalSection = fastSection.getConfigurationSection(totalRange);
                if (totalSection == null) continue;

                int dash = totalRange.indexOf('-');
                int minTotal = Integer.parseInt((dash == -1 ? totalRange : totalRange.substring(0, dash)).trim());
                int maxTotal = dash == -1 ? minTotal : Integer.parseInt(totalRange.substring(dash + 1).trim());

                for (String color : totalSection.getKeys(false)) {
                    String timeRange = totalSection.getString(color);
                    if (timeRange == null || timeRange.isBlank()) continue;

                    int timeDash = timeRange.indexOf('-');
                    double minTime = Double.parseDouble(timeRange.substring(0, timeDash).trim());
                    double maxTime = Double.parseDouble(timeRange.substring(timeDash + 1).trim());

                    fastEntries.add(new FastEntry(minTotal, maxTotal, minTime, maxTime, color));
                }
            }

            this.fastEntries = fastEntries.toArray(FastEntry[]::new);
        }
    }


    public ColorResult getColor(int total, double cps, double time) {
        for (FastEntry entry : fastEntries) {
            if (total >= entry.minTotal && total <= entry.maxTotal) {
                if (time >= entry.minTime && time <= entry.maxTime) {
                    return new ColorResult(entry.color, false);
                }
            }
        }

        if (total < minClicks || time < minTime) return ZERO;

        for (Entry entry : entries) {
            if (total < entry.minTotal || total > entry.maxTotal) continue;
            if (cps >= entry.minCps && cps <= entry.maxCps) return new ColorResult(entry.color, false);
        }

        return NONE;
    }

    public record ColorResult(String color, boolean isZero) {
    }

    private record Entry(int minTotal, int maxTotal, double minCps, double maxCps, String color) {
    }

    private record FastEntry(int minTotal, int maxTotal, double minTime, double maxTime, String color) {
    }
}