package com.arthas.shieldrpg.api;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public record ShieldSkillContext(
        String trigger,
        ServerPlayer player,
        ItemStack shield,
        @Nullable Entity directSource,
        @Nullable LivingEntity attacker,
        @Nullable LivingEntity target,
        float amount,
        Map<String, Object> extras) {

    public ShieldSkillContext {
        extras = extras == null ? Map.of() : extras;
    }

    public ShieldSkillContext(String trigger, ServerPlayer player, ItemStack shield, @Nullable Entity directSource,
                              @Nullable LivingEntity attacker, @Nullable LivingEntity target, float amount) {
        this(trigger, player, shield, directSource, attacker, target, amount, Map.of());
    }

    public double extraDouble(String key, double fallback) {
        return extras.get(key) instanceof Number number ? number.doubleValue() : fallback;
    }

    public String extraString(String key, String fallback) {
        Object value = extras.get(key);
        return value == null ? fallback : value.toString();
    }
}
