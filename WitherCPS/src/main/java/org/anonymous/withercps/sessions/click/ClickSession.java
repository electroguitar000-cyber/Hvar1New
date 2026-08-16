package org.anonymous.withercps.sessions.click;

import org.anonymous.withercps.WitherCPS;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ClickSession {

    private final double minus_cps;
    private final WitherCPS plugin;

    private long firstServer, lastServer, firstNetwork, lastNetwork;
    private int serverClicks, networkClicks;

    private final Set<ViolationData> violatedItems = new HashSet<>();
    private final Set<PotionEffectType> violatedEffects = new HashSet<>();

    private float speed = 0.2f;
    private double mspt;
    private int ping;
    private boolean gamemode;

    private static final Set<String> TRACKED_ATTRIBUTES = Set.of(
            "scale",
            "entity_interaction_range",
            "attack_knockback"
    );

    public ClickSession(double minus_cps, WitherCPS plugin) {
        this.minus_cps = minus_cps;
        this.plugin = plugin;
    }


    public void recordClick(boolean network, long timestamp) {
        if (network) {
            if (networkClicks++ == 0) firstNetwork = timestamp;
            lastNetwork = timestamp;
        } else {
            if (serverClicks++ == 0) firstServer = timestamp;
            lastServer = timestamp;
        }
    }


    public boolean hasNetwork() {
        return networkClicks > 0;
    }

    public boolean hasServer() {
        return serverClicks > 0;
    }

    public long getLastAny() {
        return Math.max(lastNetwork, lastServer);
    }


    public ClickReport createNetworkReport(UUID uuid, String nickname) {
        return createReport(uuid, nickname, true);
    }

    public ClickReport createServerReport(UUID uuid, String nickname) {
        return createReport(uuid, nickname, false);
    }

    private ClickReport createReport(UUID uuid, String nickname, boolean network) {
        int clicks = network ? networkClicks : serverClicks;
        double cps = network ? calculateCps(true) : calculateCps(false);
        double seconds = calculateDuration(network);

        return new ClickReport(uuid, nickname, network ? ClickReport.Type.NETWORK : ClickReport.Type.SERVER, clicks, cps, seconds, Set.copyOf(violatedItems), Set.copyOf(violatedEffects), speed, mspt, ping, gamemode);
    }


    private double calculateDuration(boolean network) {
        long duration = network ? lastNetwork - firstNetwork : lastServer - firstServer;
        return floor(duration / 1_000_000_000.0);
    }

    private double calculateCps(boolean network) {
        double seconds = calculateDuration(network);

        if (seconds <= 0.0) return 0.0;

        double clicks = (network ? networkClicks : serverClicks) - minus_cps;
        return floor(clicks / seconds);
    }

    private double floor(double value) {
        return Math.floor(value * 100) / 100.0;
    }


    public void updateStats(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            violatedEffects.add(effect.getType());
        }

        PlayerInventory inventory = player.getInventory();

        recordItem("Шлем", inventory.getHelmet());
        recordItem("Нагрудник", inventory.getChestplate());
        recordItem("Поножи", inventory.getLeggings());
        recordItem("Ботинки", inventory.getBoots());
        recordItem("Правая рука", inventory.getItemInMainHand());
        recordItem("Левая рука", inventory.getItemInOffHand());

        recordPlayerAttributes(player);

        gamemode = player.getGameMode() == GameMode.CREATIVE;
        ping = player.getPing();
    }

    public void updateSpeed(float walkSpeed) {
        this.speed = walkSpeed;
    }

    private void recordItem(String slot, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        List<ViolationData.AttributeData> attributes = new ArrayList<>();
        if (item.hasItemMeta() && item.getItemMeta().hasAttributeModifiers()) {
            var multimap = item.getItemMeta().getAttributeModifiers();
            if (multimap != null) {
                for (Map.Entry<Attribute, AttributeModifier> entry : multimap.entries()) {
                    String attributeName = entry.getKey().getKey().getKey();
                    double value = entry.getValue().getAmount();
                    attributes.add(new ViolationData.AttributeData(attributeName, value));
                }
            }
        }

        violatedItems.add(new ViolationData(slot, item.clone(), item.getEnchantmentLevel(Enchantment.KNOCKBACK), attributes));
    }

    private void recordPlayerAttributes(Player player) {
        List<ViolationData.AttributeData> attributes = new ArrayList<>();

        for (Attribute attribute : Attribute.values()) {
            String attributeName = attribute.getKey().getKey();

            if (!TRACKED_ATTRIBUTES.contains(attributeName)) continue;

            var instance = player.getAttribute(attribute);
            if (instance != null) {
                double baseValue = instance.getBaseValue();
                attributes.add(new ViolationData.AttributeData(attributeName, baseValue));
            }
        }

        if (!attributes.isEmpty()) {
            violatedItems.add(new ViolationData("Атрибуты игрока", null, 0, attributes));
        }
    }

    public void updateDurationTick(double durationTick) {
        this.mspt = durationTick;
    }


    public record ViolationData(String slot, ItemStack itemStack, int knockback, List<AttributeData> attributes) {

        public record AttributeData(String name, double value) {
        }
    }
}