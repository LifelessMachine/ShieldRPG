package com.arthas.shieldrpg.api;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public record ShieldDefinition(
        String itemId,
        boolean existingItem,
        String name,
        int nameFromRgb,
        int nameToRgb,
        String tier,
        String loreKey,
        int durability,
        double maxStability,
        double stabilityRegen,
        double holdResourceDrain,
        double damageReductionPercent,
        double projectileDamageReductionPercent,
        double perfectGuardKnockback,
        int perfectGuardWindowTicks,
        Map<ShieldDescriptor.Stat, Double> stats,
        List<Skill> skills,
        boolean showSkillCooldown,
        double raisedMoveSpeed) {

    public ShieldDefinition {
        EnumMap<ShieldDescriptor.Stat, Double> statCopy = new EnumMap<>(ShieldDescriptor.Stat.class);
        if (stats != null) {
            statCopy.putAll(stats);
        }
        stats = Collections.unmodifiableMap(statCopy);
        skills = skills == null ? List.of() : List.copyOf(skills);
    }

    public record Skill(
            String trigger,
            int cooldownTicks,
            double threshold,
            List<Step> steps,
            @Nullable SoundFx sound,
            @Nullable String textKey) {

        public Skill {
            steps = steps == null ? List.of() : List.copyOf(steps);
        }
    }

    public record Step(
            String action,
            String target,
            double radius,
            double maxRange,
            ShieldSkillParams params,
            @Nullable ParticleFx particle) {

        public Step {
            params = params == null ? ShieldSkillParams.EMPTY : params;
        }
    }

    public record ParticleFx(String type, int count, double spread, double speed) {}

    public record SoundFx(String id, float volume, float pitch) {}
}
