package ru.dev.koramikon.ftime.data;

import java.util.ArrayList;
import java.util.List;

public class PlayerCards {

    private final List<CardRecord> records = new ArrayList<>();

    public void addCard(String type, String reason, String issuer) {
        records.add(new CardRecord(type, "", reason, issuer, System.currentTimeMillis()));
    }

    public void removeCard(String type) {
        records.removeIf(r -> r.type().equals(type));
    }

    public List<CardRecord> getRecords() {
        return new ArrayList<>(records);
    }
}