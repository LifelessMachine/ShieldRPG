package com.arthas.shieldrpg.internal;

import com.arthas.shieldrpg.api.ShieldApi;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.neoforged.neoforge.common.conditions.ICondition;

public record CraftingEnabledCondition(String shield) implements ICondition {

    public static final MapCodec<CraftingEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("shield").forGetter(CraftingEnabledCondition::shield)
    ).apply(instance, CraftingEnabledCondition::new));

    @Override
    public boolean test(IContext context) {
        return ShieldApi.isCraftingEnabled(shield);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
