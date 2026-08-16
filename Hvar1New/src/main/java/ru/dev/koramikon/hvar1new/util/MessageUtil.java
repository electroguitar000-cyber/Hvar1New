package ru.dev.koramikon.hvar1new.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.dev.koramikon.hvar1new.Hvar1NewPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилита для сообщений.
 * Поддерживает:
 *   - Стандартные & коды (&a, &b, &l, &r и т.д.)
 *   - HEX цвета в формате &#RRGGBB
 */
public final class MessageUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9A-Fa-f]{6})");

    private static Hvar1NewPlugin plugin;
    private static FileConfiguration messages;
    private static String prefix;

    private MessageUtil() {}

    public static void init(Hvar1NewPlugin pl) {
        plugin = pl;
        reloadMessages();
    }

    public static void reloadMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);

        // Подгружаем defaults из jar
        InputStream defStream = plugin.getResource("messages.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            messages.setDefaults(defConfig);
        }

        prefix = colorize(messages.getString("prefix", "&8[&bHvar1&8] "));
    }

    /**
     * Получить строку из messages.yml, уже покрашенную.
     */
    public static String get(String key) {
        String raw = messages.getString(key, "&c[MISSING: " + key + "]");
        return prefix + colorize(raw);
    }

    /**
     * Получить строку БЕЗ префикса.
     */
    public static String getRaw(String key) {
        String raw = messages.getString(key, "&c[MISSING: " + key + "]");
        return colorize(raw);
    }

    /**
     * Покрасить строку: сначала HEX &#RRGGBB → §x§R§R§G§G§B§B, потом & → §
     */
    public static String colorize(String text) {
        if (text == null) return "";

        // Заменяем &#RRGGBB → §x§R§R§G§G§B§B
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);

        // Заменяем & на §
        return sb.toString().replace('&', '§');
    }

    /**
     * Получить список строк из messages.yml.
     */
    public static java.util.List<String> getList(String key) {
        return messages.getStringList(key);
    }

    /**
     * Получить компонент Adventure из строки (для sendMessage).
     */
    public static Component component(String key) {
        return LegacyComponentSerializer.legacySection().deserialize(get(key));
    }

    /**
     * Компонент из уже готовой (покрашенной) строки.
     */
    public static Component fromColored(String colored) {
        return LegacyComponentSerializer.legacySection().deserialize(colored);
    }
}
