package com.arthas.shieldrpg.internal;

import com.arthas.shieldrpg.api.ShieldDefinition;
import com.arthas.shieldrpg.api.ShieldDescriptor;
import com.arthas.shieldrpg.util.ShieldTier;

public final class ShieldDescriptors {

    private ShieldDescriptors() {}

    public static ShieldDescriptor of(ShieldDefinition definition) {
        ShieldTier tier = ShieldTier.byName(definition.tier());
        ShieldDescriptor descriptor = ShieldDescriptor.of(tier.translationKey(), tier.rgb(), tier.isGlow(), definition.loreKey(),
                definition.maxStability(), definition.stabilityRegen(), definition.holdResourceDrain(),
                definition.damageReductionPercent());
        for (var stat : definition.stats().entrySet()) {
            descriptor = descriptor.with(stat.getKey(), stat.getValue());
        }
        return descriptor;
    }
}
