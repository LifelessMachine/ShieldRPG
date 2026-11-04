package com.arthas.shieldrpg.item;

import java.util.List;

import com.arthas.shieldrpg.api.ShieldDefinition;
import com.arthas.shieldrpg.api.ShieldDescriptor;
import com.arthas.shieldrpg.util.ShieldTier;

import net.minecraft.world.item.Item;

public class DataShieldItem extends NorseShieldItem {

    private final ShieldDefinition definition;
    private final ShieldTier tier;

    public DataShieldItem(Item.Properties properties, ShieldDefinition definition) {
        super(properties, definition.name(), definition.nameFromRgb(), definition.nameToRgb());
        this.definition = definition;
        this.tier = ShieldTier.byName(definition.tier());
        com.arthas.shieldrpg.api.ShieldApi.registerShieldDefinition(this, definition);
    }

    public static Item.Properties configure(Item.Properties props, ShieldDefinition definition) {
        return shieldProps(props, definition.durability(), ShieldTier.byName(definition.tier()),
                (float) definition.damageReductionPercent());
    }

    public ShieldDefinition getDefinition() {
        return definition;
    }

    @Override
    public ShieldDefinition getShieldDefinition() {
        return definition;
    }

    @Override
    public ShieldDescriptor getDescriptor() {
        ShieldDescriptor descriptor = describe(definition.loreKey(), definition.damageReductionPercent());
        for (var stat : definition.stats().entrySet()) {
            descriptor = descriptor.with(stat.getKey(), stat.getValue());
        }
        return descriptor;
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
    public ShieldTier getTier() {
        return tier;
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
    public List<String> getSkillTextKeys() {
        return definition.skills().stream().map(ShieldDefinition.Skill::textKey).filter(key -> key != null).toList();
    }
}
