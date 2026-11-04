package com.arthas.shieldrpg.internal;

import java.util.Map;

import com.arthas.shieldrpg.ShieldRPG;
import com.arthas.shieldrpg.api.ShieldDescriptor;
import com.arthas.shieldrpg.api.ShieldDescriptor.Stat;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class ShieldNativeStats {

    private ShieldNativeStats() {}

    public static ItemAttributeModifiers attributes(ShieldDescriptor d) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        for (Map.Entry<Stat, Double> e : d.stats().entrySet()) {
            double v = e.getValue();
            if (v == 0.0) {
                continue;
            }
            switch (e.getKey()) {
                case ARMOR -> add(builder, Attributes.ARMOR, e.getKey(), v,
                        AttributeModifier.Operation.ADD_VALUE);
                case MAX_HEALTH -> add(builder, Attributes.MAX_HEALTH, e.getKey(), v,
                        AttributeModifier.Operation.ADD_VALUE);
                case KNOCKBACK_RESIST -> add(builder, Attributes.KNOCKBACK_RESISTANCE, e.getKey(), v,
                        AttributeModifier.Operation.ADD_VALUE);
                case MOVE_SPEED -> add(builder, Attributes.MOVEMENT_SPEED, e.getKey(), v,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            }
        }
        return builder.build();
    }

    private static void add(ItemAttributeModifiers.Builder builder, Holder<Attribute> attribute, Stat stat,
                            double amount, AttributeModifier.Operation operation) {
        Identifier id = Identifier.fromNamespaceAndPath(ShieldRPG.MODID, "shield_stat." + stat.name().toLowerCase(java.util.Locale.ROOT));
        builder.add(attribute, new AttributeModifier(id, amount, operation), EquipmentSlotGroup.OFFHAND);
    }
}
