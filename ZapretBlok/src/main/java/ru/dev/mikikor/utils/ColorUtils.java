package ru.dev.mikikor.utils;

import net.md_5.bungee.api.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String colorize(String message) {
        if (message == null) return "";

        // Обработка HEX цветов
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexColor = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hexColor).toString());
        }
        matcher.appendTail(buffer);

        // Обработка стандартных цветов (&a, &l, и т.д.)
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}