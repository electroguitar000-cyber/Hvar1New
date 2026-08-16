package ru.dev.koramikon.ftime.data;

public record CardRecord(
        String type,
        String playerName,
        String reason,
        String issuer,
        long timestamp
) {
}