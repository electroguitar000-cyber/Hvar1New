package org.anonymous.withercps.utils;

import java.time.Duration;
import java.util.Optional;

public class TimeUtils {

    public Optional<Duration> parse(String input) {
        if (input == null || input.length() < 2) {
            return Optional.empty();
        }

        char unit = Character.toLowerCase(input.charAt(input.length() - 1));
        long value;

        try {
            value = Long.parseLong(input.substring(0, input.length() - 1));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        return switch (unit) {
            case 's' -> Optional.of(Duration.ofSeconds(value));
            case 'm' -> Optional.of(Duration.ofMinutes(value));
            case 'h' -> Optional.of(Duration.ofHours(value));
            case 'd' -> Optional.of(Duration.ofDays(value));
            default -> Optional.empty();
        };
    }

    public String format(long duration) {
        if (duration <= 0) {
            return "";
        }

        long totalSeconds = duration / 1000;

        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        StringBuilder builder = new StringBuilder(5);

        if (minutes < 10) builder.append("0");
        builder.append(minutes).append(":");

        if (seconds < 10) builder.append("0");
        builder.append(seconds);

        return builder.toString();
    }
}