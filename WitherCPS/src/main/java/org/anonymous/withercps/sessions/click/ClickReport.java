package org.anonymous.withercps.sessions.click;

import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ClickReport(
        UUID uuid,
        String nickname,
        Type type,
        int total,
        double cps,
        double seconds,
        Set<ClickSession.ViolationData> violatedItems,
        Set<PotionEffectType> violatedEffects,
        float speed,
        double mspt,
        int ping,
        boolean gamemode
) {

    public enum Type {
        NETWORK,
        SERVER
    }

    public boolean isNetwork() {
        return type == Type.NETWORK;
    }

    public boolean isServer() {
        return type == Type.SERVER;
    }
}