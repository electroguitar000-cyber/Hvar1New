package org.anonymous.withercps.sessions.click;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.anonymous.withercps.cache.ModeCache;
import org.anonymous.withercps.utils.ConfigUtils;

import java.util.List;

public class ClickAttention {

    private final ConfigUtils config;
    private final ModeCache modes;

    public ClickAttention(ConfigUtils config, ModeCache modes) {
        this.config = config;
        this.modes = modes;
    }

    public Component buildAttention(ClickReport report, String modeName, String colorCode) {
        ModeCache.Mode mode = modes.getMode(modeName);
        if (mode == null) return Component.empty();

        // Находим роль игрока по его scale
        double playerScale = getPlayerScale(report);
        ModeCache.Role role = mode.findRoleByPlayerScale(playerScale);

        TextComponent.Builder builder = Component.text();

        addItemAttention(builder, report, role, mode);
        addEffectAttention(builder, report, role, mode);

        if (report.gamemode()) builder.append(config.getMessage("messages.monitoring.attention.gamemode"));

        if (role != null && !role.isSpeedAllowed(report.speed()))
            builder.append(config.getMessage("messages.monitoring.attention.speed", "level", report.speed() * 5));

        if ("&c".equals(colorCode) || "&4".equals(colorCode)) addConnectionAttention(builder, report);

        return builder.build();
    }

    private double getPlayerScale(ClickReport report) {
        for (ClickSession.ViolationData data : report.violatedItems()) {
            if (data.itemStack() == null && data.attributes() != null) {
                for (ClickSession.ViolationData.AttributeData attr : data.attributes()) {
                    if ("scale".equalsIgnoreCase(attr.name())) {
                        return attr.value();
                    }
                }
            }
        }
        return 1.0;
    }

    private void addItemAttention(TextComponent.Builder builder, ClickReport report, ModeCache.Role role, ModeCache.Mode mode) {
        report.violatedItems().forEach(data -> {
            boolean knockback = data.knockback() > mode.knockback();

            boolean attributes = false;
            if (data.attributes() != null && !data.attributes().isEmpty()) {
                for (ClickSession.ViolationData.AttributeData attr : data.attributes()) {
                    boolean allowed;
                    if (data.itemStack() == null) {
                        // Атрибуты игрока — проверяем по роли
                        allowed = role != null && role.isPlayerAttributeAllowed(attr.name(), attr.value());
                    } else {
                        // Атрибуты предмета — проверяем по роли
                        allowed = role != null && role.isAttributeAllowed(attr.name(), attr.value());
                    }

                    if (!allowed) {
                        attributes = true;
                        break;
                    }
                }
            }

            if (!knockback && !attributes) return;

            Component item = config.getMessage("messages.monitoring.attention.item", "slot", data.slot());

            if (data.itemStack() != null) {
                builder.append(item.hoverEvent(data.itemStack().asHoverEvent()));
            } else {
                Component hoverText = Component.text("§fАтрибуты игрока:");
                for (ClickSession.ViolationData.AttributeData attr : data.attributes()) {
                    String allowedStr = "";
                    if (role != null) {
                        List<Double> allowedValues = role.basePlayerAttributes().get(attr.name().toLowerCase());
                        if (allowedValues != null) {
                            allowedStr = " §7(разрешено для " + role.prefix() + "§7: §f" + allowedValues + "§7)";
                        }
                    }
                    hoverText = hoverText.append(Component.newline())
                            .append(Component.text("§7- §e" + attr.name() + " §f= §b" + attr.value() + allowedStr));
                }
                if (role != null) {
                    hoverText = hoverText.append(Component.newline())
                            .append(Component.text("§7Роль: " + role.prefix()));
                }
                builder.append(item.hoverEvent(hoverText));
            }
        });
    }

    private void addEffectAttention(TextComponent.Builder builder, ClickReport report, ModeCache.Role role, ModeCache.Mode mode) {
        if (role == null) return;
        report.violatedEffects().forEach(type -> {
            String effect = type.getKey().getKey().toLowerCase();
            if (role.effects().contains(effect)) return;

            Component line = Component.text("§8- §c" + capitalize(effect));
            builder.append(line).append(Component.newline());
        });
    }

    private void addConnectionAttention(TextComponent.Builder builder, ClickReport report) {
        if (report.mspt() > 50.0)
            builder.append(config.getMessage("messages.monitoring.attention.mspt", "mspt", report.mspt()));
        if (report.ping() > config.getInteger("settings.max-ping"))
            builder.append(config.getMessage("messages.monitoring.attention.ping", "ping", report.ping()));
    }

    private String capitalize(String string) {
        if (string.isEmpty()) return string;
        return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }
}