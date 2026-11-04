package com.arthas.shieldrpg.item;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.arthas.shieldrpg.api.ShieldApi;
import com.arthas.shieldrpg.api.ShieldDefinition;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ShieldBehaviors {

    private static final Map<ShieldDefinition, ForeignShield> FOREIGN = new ConcurrentHashMap<>();

    private ShieldBehaviors() {}

    @Nullable
    public static ShieldBehavior of(@Nullable ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : of(stack.getItem());
    }

    @Nullable
    public static ShieldBehavior of(Item item) {
        if (item instanceof ShieldBehavior behavior) {
            return behavior;
        }
        ShieldDefinition definition = ShieldApi.getShieldDefinition(item);
        return definition == null ? null : FOREIGN.computeIfAbsent(definition, ForeignShield::new);
    }
}
