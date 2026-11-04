package com.arthas.shieldrpg;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import com.arthas.shieldrpg.api.ShieldApi;
import com.arthas.shieldrpg.config.ShieldStats;
import com.arthas.shieldrpg.internal.ShieldNativeStats;
import com.arthas.shieldrpg.registry.ModShieldCreativeTabs;
import com.arthas.shieldrpg.registry.ModShieldItems;

import net.minecraft.core.component.DataComponents;
import com.mojang.serialization.MapCodec;
import com.arthas.shieldrpg.internal.CraftingEnabledCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

@Mod(ShieldRPG.MODID)
public class ShieldRPG {
    public static final String MODID = "shieldrpg";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS, MODID);
    static {
        CONDITIONS.register("crafting_enabled", () -> CraftingEnabledCondition.CODEC);
    }

    public ShieldRPG(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing ShieldRPG (Standalone Shield Engine)...");

        modContainer.registerConfig(ModConfig.Type.SYNCED, ShieldStats.SPEC);
        modContainer.registerConfig(ModConfig.Type.STARTUP, ShieldStats.STARTUP_SPEC);

        ModShieldItems.register(modEventBus);
        ModShieldCreativeTabs.register(modEventBus);

        CONDITIONS.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);

        ShieldApi.registerSkillCooldownListener(new com.arthas.shieldrpg.internal.SkillCooldownTracker());

        modEventBus.addListener(this::modifyDefaultComponents);

        com.arthas.shieldrpg.internal.skill.BuiltInSkillActions.register();
        ShieldApi.registerSkillAction(new com.arthas.shieldrpg.internal.skill.BeamAction());
        ShieldApi.registerSkillAction(new com.arthas.shieldrpg.internal.skill.BlastAction());
        ShieldApi.registerSkillAction(new com.arthas.shieldrpg.internal.skill.IgniteAction());
        ShieldApi.registerSkillAction(new com.arthas.shieldrpg.internal.skill.PullAction());
        ShieldApi.registerSkillAction(new com.arthas.shieldrpg.internal.skill.CleanseAction());
        ShieldApi.registerTrigger(com.arthas.shieldrpg.internal.skill.PerfectComboTracker.TRIGGER_ID);
        ShieldApi.registerTriggerListener(new com.arthas.shieldrpg.internal.skill.PerfectComboTracker(), ShieldApi.PRIORITY_DEFAULT);
        ShieldApi.registerResourceProvider(new com.arthas.shieldrpg.stamina.StaminaProvider(), ShieldApi.PRIORITY_FALLBACK);

        ShieldApi.installCraftingConfig(id -> !ShieldStats.disabledRecipes().contains(id));

        ShieldApi.installTriggerDispatcher((player, stack, trigger, directSource, attacker, amount, extras) -> {
            if (com.arthas.shieldrpg.item.ShieldBehaviors.of(stack) instanceof com.arthas.shieldrpg.item.ShieldBehavior shield) {
                com.arthas.shieldrpg.internal.skill.ShieldTriggers.fire(player, stack, shield, trigger, directSource, attacker, amount, extras);
            }
        });
        modEventBus.addListener(this::loadComplete);

        LOGGER.info("ShieldRPG successfully initialized!");
    }

    private void registerPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(com.arthas.shieldrpg.stamina.StaminaSyncPayload.TYPE,
                com.arthas.shieldrpg.stamina.StaminaSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> com.arthas.shieldrpg.stamina.StaminaState.setClient(payload.value(), payload.max())));
    }

    private void loadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            for (var item : ModShieldItems.all()) {
                if (item.get() instanceof com.arthas.shieldrpg.item.DataShieldItem shield) {
                    checkTriggers(shield.getDefinition());
                }
            }
            for (var item : com.arthas.shieldrpg.internal.ExistingItemShields.items()) {
                var definition = ShieldApi.getShieldDefinition(item);
                if (definition != null) {
                    checkTriggers(definition);
                }
            }
        });
    }

    private static void checkTriggers(com.arthas.shieldrpg.api.ShieldDefinition definition) {
        for (var skill : definition.skills()) {
            if (!ShieldApi.isKnownTrigger(skill.trigger())) {
                LOGGER.warn("Shield {}: skill trigger '{}' is not a ShieldRPG trigger and no mod declared it with ShieldApi.registerTrigger",
                        definition.itemId(), skill.trigger());
            }
        }
    }

    private void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {

        com.arthas.shieldrpg.internal.ExistingItemShields.apply(event);
        if (ShieldApi.hasExternalItemStats()) {
            return;
        }
        for (ShieldApi.ShieldEntry entry : ShieldApi.getShields()) {
            event.modify(entry.item(), (builder, provider, item) -> {
                var descriptor = entry.descriptor().get();
                if (descriptor.hasItemDetails()) {
                    builder.set(DataComponents.ATTRIBUTE_MODIFIERS, ShieldNativeStats.attributes(descriptor));
                }
            });
        }
    }
}
