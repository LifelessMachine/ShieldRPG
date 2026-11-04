package com.arthas.shieldrpg.registry;

import com.arthas.shieldrpg.ShieldRPG;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModShieldCreativeTabs {
    private ModShieldCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ShieldRPG.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SHIELD_TAB =
            CREATIVE_MODE_TABS.register("norse_shields", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.shieldrpg"))
                    .icon(() -> new ItemStack(ModShieldItems.iconItem()))
                    .displayItems((parameters, output) -> {

                        for (DeferredItem<? extends Item> item : ModShieldItems.all()) {
                            if (ShieldRPG.MODID.equals(item.getId().getNamespace())) {
                                output.accept(item.get());
                            }
                        }
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
