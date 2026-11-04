package com.arthas.shieldrpg.stamina;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.player.Player;

public final class StaminaState {
    public static final double MAX = 100.0;

    public static final double REGEN_PER_SECOND = 10.0;

    private static final Map<UUID, Double> SERVER = new ConcurrentHashMap<>();
    private static volatile double clientValue = MAX;
    private static volatile double clientMax = MAX;

    private StaminaState() {}

    public static double get(Player player) {
        return player.level().isClientSide() ? clientValue : SERVER.getOrDefault(player.getUUID(), MAX);
    }

    public static double max() {
        return clientMax;
    }

    public static void set(UUID player, double value) {
        SERVER.put(player, Math.max(0.0, Math.min(MAX, value)));
    }

    public static void forget(UUID player) {
        SERVER.remove(player);
    }

    public static void setClient(double value, double max) {
        clientValue = value;
        clientMax = max;
    }
}
