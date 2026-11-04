package com.arthas.shieldrpg.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.arthas.shieldrpg.ShieldRPG;
import com.arthas.shieldrpg.api.ShieldApi;
import com.arthas.shieldrpg.api.ShieldDefinition;
import com.arthas.shieldrpg.item.NorseShieldItem;
import com.arthas.shieldrpg.util.ShieldTier;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

public final class ExistingItemShields {

    private static final List<ShieldDefinition> DEFINITIONS = new ArrayList<>();
    private static int count;
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    private static boolean applied;

    private ExistingItemShields() {}

    public static void add(ShieldDefinition definition) {
        DEFINITIONS.add(definition);
    }

    public static List<Item> items() {
        List<Item> result = new ArrayList<>();
        for (ShieldDefinition definition : DEFINITIONS) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(definition.itemId()));
            if (item != null && item != Items.AIR) {
                result.add(item);
            }
        }
        return result;
    }

    public static synchronized void apply(ModifyDefaultComponentsEvent event) {
        if (applied) {
            return;
        }
        applied = true;
        for (ShieldDefinition definition : DEFINITIONS) {
            Identifier id = Identifier.parse(definition.itemId());
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item == null || item == Items.AIR) {
                report(definition.itemId(), "Shield definition " + definition.itemId() + " has \"existing_item\": true but no such item exists");
                continue;
            }
            if (item instanceof NorseShieldItem || ShieldApi.getShieldDefinition(item) != null) {
                report(definition.itemId(), "Item " + definition.itemId() + " is already a ShieldRPG shield; the extra definition is ignored");
                continue;
            }
            ShieldApi.registerShieldDefinition(item, definition);
            ShieldApi.registerShield(item, () -> ShieldDescriptors.of(definition));
            count++;
            ShieldTier tier = ShieldTier.byName(definition.tier());
            event.modify(item, (builder, provider, target) -> {
                builder.set(DataComponents.BLOCKS_ATTACKS, NorseShieldItem.blocksAttacks(provider, (float) definition.damageReductionPercent()));
                builder.set(DataComponents.MAX_STACK_SIZE, 1);
                builder.set(DataComponents.MAX_DAMAGE, definition.durability());
                builder.set(DataComponents.RARITY, tier.vanillaRarity());
            });
        }
        if (count > 0) {
            ShieldRPG.LOGGER.info("{} existing item(s) turned into shields", count);
        }
    }

    private static void report(String key, String message) {
        if (REPORTED.add(key)) {
            ShieldRPG.LOGGER.warn(message);
        }
    }
}
