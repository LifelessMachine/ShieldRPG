package com.arthas.shieldrpg.api;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class PerfectGuardContext {

    private final ServerPlayer player;
    private final ItemStack shield;
    private final DamageSource damageSource;
    private final float blockedDamage;
    private double knockback;
    private boolean defaultEffectsSuppressed;

    public PerfectGuardContext(ServerPlayer player, ItemStack shield, DamageSource damageSource,
                               float blockedDamage, double knockback) {
        this.player = player;
        this.shield = shield;
        this.damageSource = damageSource;
        this.blockedDamage = blockedDamage;
        this.knockback = knockback;
    }

    public ServerPlayer player() {
        return player;
    }

    public ItemStack shield() {
        return shield;
    }

    public DamageSource damageSource() {
        return damageSource;
    }

    @Nullable
    public Entity directSource() {
        return damageSource.getDirectEntity();
    }

    @Nullable
    public LivingEntity attacker() {
        return damageSource.getEntity() instanceof LivingEntity living ? living : null;
    }

    public float blockedDamage() {
        return blockedDamage;
    }

    public double knockback() {
        return knockback;
    }

    public void setKnockback(double knockback) {
        this.knockback = knockback;
    }

    public boolean defaultEffectsSuppressed() {
        return defaultEffectsSuppressed;
    }

    public void suppressDefaultEffects() {
        this.defaultEffectsSuppressed = true;
    }
}
