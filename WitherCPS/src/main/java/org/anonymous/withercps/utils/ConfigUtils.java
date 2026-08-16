package org.anonymous.withercps.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.anonymous.withercps.WitherCPS;

public record ConfigUtils(WitherCPS plugin) {

    public Component getMessage(String path, Object... args) {
        return color(getString(path, args));
    }

    public String getString(String path, Object... args) {
        String text = plugin.getConfig().getString(path, path);

        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 < args.length) {
                text = text.replace("{" + args[i] + "}", String.valueOf(args[i + 1]));
            }
        }

        if (text.contains("{prefix}")) {
            text = text.replace("{prefix}", getPrefix());
        }

        return text;
    }

    public double getInteger(String path) {
        return plugin.getConfig().getInt(path, 0);
    }

    public double getDouble(String path) {
        return plugin.getConfig().getDouble(path, 0.0);
    }

    private String getPrefix() {
        return plugin.getConfig().getString("messages.prefix", "&#FFCC75[WitherCPS]");
    }

    private Component color(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}