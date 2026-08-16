package ru.dev.kisstymelusi.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy:HH:mm");

    public static String formatTimestamp(long timestamp, String timezoneId) {
        ZoneId zone = ZoneId.of(timezoneId);
        Instant instant = Instant.ofEpochMilli(timestamp);
        return FORMATTER.format(instant.atZone(zone));
    }
}