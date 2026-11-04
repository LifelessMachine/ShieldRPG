package com.arthas.shieldrpg.item;

import com.arthas.shieldrpg.api.ShieldDefinition;
import com.arthas.shieldrpg.api.ShieldDescriptor;
import com.arthas.shieldrpg.internal.ShieldDescriptors;

public final class ForeignShield implements ShieldBehavior {

    private final ShieldDefinition definition;

    public ForeignShield(ShieldDefinition definition) {
        this.definition = definition;
    }

    @Override
    public double getMaxStability() {
        return definition.maxStability();
    }

    @Override
    public double getStabilityRegenRate() {
        return definition.stabilityRegen();
    }

    @Override
    public double getHoldStaminaDrain() {
        return definition.holdResourceDrain();
    }

    @Override
    public double getPerfectGuardKnockback() {
        return definition.perfectGuardKnockback();
    }

    @Override
    public int getPerfectGuardWindowTicks() {
        return definition.perfectGuardWindowTicks();
    }

    @Override
    public double getProjectileDamageReductionPercent() {
        return definition.projectileDamageReductionPercent();
    }

    @Override
    public ShieldDescriptor getDescriptor() {
        return ShieldDescriptors.of(definition);
    }

    @Override
    public ShieldDefinition getShieldDefinition() {
        return definition;
    }
}
