package org.anonymous.withercps.cache;

import org.anonymous.withercps.WitherCPS;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public final class ModeCache {

    private final WitherCPS plugin;
    private Map<String, Mode> modes = Collections.emptyMap();

    public ModeCache(WitherCPS plugin) {
        this.plugin = plugin;
    }

    public void load() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("modes");
        if (section == null) {
            plugin.getLogger().warning("No modes section found in config!");
            modes = Collections.emptyMap();
            return;
        }

        Map<String, Mode> loaded = new HashMap<>();
        for (String name : section.getKeys(false)) {
            ConfigurationSection modeSection = section.getConfigurationSection(name);
            if (modeSection == null) {
                plugin.getLogger().warning("Invalid mode section: " + name);
                continue;
            }

            try {
                loaded.put(name.toLowerCase(), loadMode(name, modeSection));
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Failed to load mode \"" + name + "\": " + e.getMessage());
            }
        }

        modes = Collections.unmodifiableMap(loaded);
    }

    public Mode getMode(String name) {
        if (name == null || name.isBlank()) return null;
        return modes.get(name.toLowerCase());
    }

    public Collection<Mode> getModes() {
        return modes.values();
    }

    private Mode loadMode(String name, ConfigurationSection section) {
        String prefix = section.getString("prefix", "");
        int knockback = section.getInt("knockback", 0);

        // Роли
        List<Role> roles = new ArrayList<>();
        ConfigurationSection rolesSection = section.getConfigurationSection("roles");
        if (rolesSection != null) {
            for (String roleName : rolesSection.getKeys(false)) {
                ConfigurationSection roleSection = rolesSection.getConfigurationSection(roleName);
                if (roleSection == null) continue;

                String rolePrefix = roleSection.getString("prefix", roleName);

                // Атрибуты предметов
                AttributeMode attributeMode;
                List<AttributeEntry> allowedAttributes = List.of();

                if (roleSection.isBoolean("attributes")) {
                    boolean value = roleSection.getBoolean("attributes");
                    attributeMode = value ? AttributeMode.ALL : AttributeMode.NONE;
                } else if (roleSection.isList("attributes")) {
                    attributeMode = AttributeMode.WHITELIST;
                    List<String> rawAttributes = roleSection.getStringList("attributes");
                    List<AttributeEntry> parsed = new ArrayList<>();
                    for (String entry : rawAttributes) {
                        String[] parts = entry.trim().split("\\s+", 2);
                        if (parts.length == 2) {
                            try {
                                double val = Double.parseDouble(parts[1]);
                                parsed.add(new AttributeEntry(parts[0], val));
                            } catch (NumberFormatException ignored) {
                                plugin.getLogger().warning("Invalid attribute value in role \"" + roleName + "\": " + entry);
                            }
                        }
                    }
                    allowedAttributes = Collections.unmodifiableList(parsed);
                } else {
                    attributeMode = AttributeMode.ALL;
                }

                // Базовые атрибуты игрока
                Map<String, List<Double>> basePlayerAttributes = new HashMap<>();
                ConfigurationSection baseAttrSection = roleSection.getConfigurationSection("base-player-attributes");
                if (baseAttrSection != null) {
                    for (String key : baseAttrSection.getKeys(false)) {
                        if (baseAttrSection.isList(key)) {
                            List<Double> values = new ArrayList<>();
                            for (Object val : baseAttrSection.getList(key)) {
                                if (val instanceof Number num) {
                                    values.add(num.doubleValue());
                                }
                            }
                            basePlayerAttributes.put(key.toLowerCase(), Collections.unmodifiableList(values));
                        } else if (baseAttrSection.isDouble(key) || baseAttrSection.isInt(key)) {
                            basePlayerAttributes.put(key.toLowerCase(), List.of(baseAttrSection.getDouble(key)));
                        }
                    }
                }

                double speed = roleSection.getDouble("speed", -1.0);
                List<String> effects = Collections.unmodifiableList(roleSection.getStringList("effects"));

                roles.add(new Role(roleName, rolePrefix, attributeMode, allowedAttributes, basePlayerAttributes, speed, effects));
            }
        }

        return new Mode(name, prefix, knockback, Collections.unmodifiableList(roles));
    }

    public enum AttributeMode {
        ALL,
        NONE,
        WHITELIST
    }

    public record AttributeEntry(String name, double value) {
    }

    public record Role(String name, String prefix, AttributeMode attributeMode,
                       List<AttributeEntry> allowedAttributes, Map<String, List<Double>> basePlayerAttributes,
                       double speed, List<String> effects) {

        private static final double EPSILON = 0.01;

        public boolean isAttributeAllowed(String attributeName, double value) {
            return switch (attributeMode) {
                case ALL -> true;
                case NONE -> false;
                case WHITELIST -> {
                    boolean foundInWhitelist = false;
                    for (AttributeEntry entry : allowedAttributes) {
                        if (entry.name().equalsIgnoreCase(attributeName)) {
                            foundInWhitelist = true;
                            if (Math.abs(entry.value() - value) < EPSILON) {
                                yield true;
                            }
                        }
                    }
                    yield !foundInWhitelist;
                }
            };
        }

        public boolean isPlayerAttributeAllowed(String attributeName, double value) {
            List<Double> allowedValues = basePlayerAttributes.get(attributeName.toLowerCase());
            if (allowedValues == null) {
                return true;
            }
            for (Double allowed : allowedValues) {
                if (Math.abs(allowed - value) < EPSILON) {
                    return true;
                }
            }
            return false;
        }

        public boolean isSpeedAllowed(float speed) {
            if (this.speed < 0) return true;
            double roundedSpeed = Math.round(speed * 1000.0) / 1000.0;
            double roundedAllowed = Math.round(this.speed * 1000.0) / 1000.0;
            return Math.abs(roundedAllowed - roundedSpeed) < 0.001;
        }
    }

    public record Mode(String name, String prefix, int knockback, List<Role> roles) {

        public Role findRoleByPlayerScale(double playerScale) {
            for (Role role : roles) {
                if (role.isPlayerAttributeAllowed("scale", playerScale)) {
                    return role;
                }
            }
            return null;
        }
    }
}