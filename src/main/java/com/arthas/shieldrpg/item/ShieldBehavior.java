package com.arthas.shieldrpg.item;

import org.jetbrains.annotations.Nullable;

import com.arthas.shieldrpg.api.ShieldDefinition;
import com.arthas.shieldrpg.api.ShieldDescriptor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public interface ShieldBehavior {

    double getMaxStability();

    double getStabilityRegenRate();

    double getHoldStaminaDrain();

    default double getPerfectGuardKnockback() {
        return 0.0;
    }

    default int getPerfectGuardWindowTicks() {
        return -1;
    }

    default double getProjectileDamageReductionPercent() {
        return -1;
    }

    ShieldDescriptor getDescriptor();

    @Nullable
    default ShieldDefinition getShieldDefinition() {
        return null;
    }

    default void onBlockedAttack(ServerPlayer player, @Nullable LivingEntity attacker, float damage) {}

    default void onPerfectBlock(ServerPlayer player, @Nullable LivingEntity attacker, float damage) {}
}
