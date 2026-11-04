package com.arthas.shieldrpg.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.arthas.shieldrpg.api.ShieldDefinition;
import com.arthas.shieldrpg.internal.ShieldDefinitionLoader;
import com.arthas.shieldrpg.item.DataShieldItem;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModShieldItems {
    private ModShieldItems() {}

    private static final Map<String, DeferredRegister.Items> REGISTERS = new LinkedHashMap<>();
    private static final List<DeferredItem<? extends Item>> ALL = new ArrayList<>();

    public static void register(IEventBus bus) {
        for (ShieldDefinition definition : ShieldDefinitionLoader.loadAll()) {
            if (definition.existingItem()) {
                com.arthas.shieldrpg.internal.ExistingItemShields.add(definition);
                continue;
            }
            Identifier id = Identifier.parse(definition.itemId());
            DeferredRegister.Items register = REGISTERS.computeIfAbsent(id.getNamespace(), ns -> DeferredRegister.createItems(ns));
            ALL.add(register.registerItem(id.getPath(),
                    properties -> new DataShieldItem(properties, definition),
                    properties -> DataShieldItem.configure(properties, definition)));
        }
        for (DeferredRegister.Items register : REGISTERS.values()) {
            register.register(bus);
        }
    }

    public static List<DeferredItem<? extends Item>> all() {
        return List.copyOf(ALL);
    }

    public static Item iconItem() {
        for (DeferredItem<? extends Item> item : ALL) {
            if (item.getId().getPath().equals("viking_roundshield")) {
                return item.get();
            }
        }
        return ALL.isEmpty() ? net.minecraft.world.item.Items.SHIELD : ALL.get(0).get();
    }
}
