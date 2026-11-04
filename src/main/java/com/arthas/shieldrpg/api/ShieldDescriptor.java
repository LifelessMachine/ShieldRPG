package com.arthas.shieldrpg.api;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public record ShieldDescriptor(
        @Nullable String tierTranslationKey,
        int tierRgb,
        boolean tierGlow,
        @Nullable String loreKey,
        double maxStability,
        double stabilityRegen,
        double holdResourceDrain,
        double damageReductionPercent,
        Map<Stat, Double> stats) {

    public ShieldDescriptor {
        EnumMap<Stat, Double> copy = new EnumMap<>(Stat.class);
        if (stats != null) {
            copy.putAll(stats);
        }
        stats = java.util.Collections.unmodifiableMap(copy);
    }

    public enum Stat {
        ARMOR,
        MAX_HEALTH,
        KNOCKBACK_RESIST,
        MOVE_SPEED
    }

    public static ShieldDescriptor of(String tierTranslationKey, int tierRgb, boolean tierGlow, String loreKey,
                                      double maxStability, double stabilityRegen, double holdResourceDrain,
                                      double damageReductionPercent) {
        return new ShieldDescriptor(tierTranslationKey, tierRgb, tierGlow, loreKey, maxStability, stabilityRegen,
                holdResourceDrain, damageReductionPercent, new EnumMap<>(Stat.class));
    }

    public static ShieldDescriptor vanillaLike(double maxStability, double stabilityRegen, double holdResourceDrain,
                                               double damageReductionPercent) {
        return new ShieldDescriptor(null, 0, false, null, maxStability, stabilityRegen, holdResourceDrain,
                damageReductionPercent, new EnumMap<>(Stat.class));
    }

    public ShieldDescriptor with(Stat stat, double value) {
        Map<Stat, Double> copy = new EnumMap<>(Stat.class);
        copy.putAll(stats);
        copy.put(stat, value);
        return new ShieldDescriptor(tierTranslationKey, tierRgb, tierGlow, loreKey, maxStability, stabilityRegen,
                holdResourceDrain, damageReductionPercent, copy);
    }

    public boolean hasItemDetails() {
        return loreKey != null && tierTranslationKey != null;
    }
}
