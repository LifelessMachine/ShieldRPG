package com.arthas.shieldrpg.client;

import java.util.List;

import com.arthas.shieldrpg.ShieldRPG;
import com.arthas.shieldrpg.api.ShieldDefinition;
import com.arthas.shieldrpg.api.ShieldSkillText;
import com.arthas.shieldrpg.item.ShieldBehavior;
import com.arthas.shieldrpg.item.ShieldBehaviors;
import com.arthas.shieldrpg.api.ShieldApi;
import com.arthas.shieldrpg.api.ShieldDescriptor;
import com.arthas.shieldrpg.item.NorseShieldItem;
import com.arthas.shieldrpg.registry.ModShieldItems;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = ShieldRPG.MODID, value = Dist.CLIENT)
public final class ShieldClientEvents {
    private ShieldClientEvents() {}

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        IItemDecorator shieldOverlayDecorator = (guiGraphics, font, stack, xOffset, yOffset) -> {
            if (!(ShieldBehaviors.of(stack) instanceof ShieldBehavior shield)) {
                return false;
            }

            double max = shield.getMaxStability();
            double current = NorseShieldItem.getStability(stack, max);
            if (current >= max - 0.001) {
                return false;
            }

            float ratio = (float) Math.max(0.0, Math.min(1.0, current / max));
            float missingRatio = 1.0F - ratio;
            int overlayHeight = Math.round(16.0F * missingRatio);
            if (overlayHeight <= 0) {
                return false;
            }

            int color;
            if (ratio > 0.60F) {

                color = 0x7700E5FF;
            } else if (ratio >= 0.30F) {

                color = 0x77FFAA00;
            } else {

                color = 0x88FF2222;
            }

            int startY = yOffset + 16 - overlayHeight;
            int endY = yOffset + 16;
            guiGraphics.fill(xOffset, startY, xOffset + 16, endY, color);
            return true;
        };

        for (var item : ModShieldItems.all()) {
            event.register(item.get(), shieldOverlayDecorator);
        }
        for (var item : com.arthas.shieldrpg.internal.ExistingItemShields.items()) {
            event.register(item, shieldOverlayDecorator);
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (ShieldApi.hasExternalItemStats() || !(ShieldBehaviors.of(event.getItemStack()) instanceof ShieldBehavior shield)) {
            return;
        }
        ShieldDescriptor d = shield.getDescriptor();
        List<Component> tooltip = event.getToolTip();
        int at = Math.min(1, tooltip.size());

        tooltip.add(at++, Component.translatable(d.tierTranslationKey()).withStyle(Style.EMPTY.withColor(d.tierRgb())));
        tooltip.add(at++, Component.translatable(d.loreKey()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        ShieldDefinition definition = shield.getShieldDefinition();
        if (definition != null) {
            boolean first = true;
            for (ShieldDefinition.Skill skill : definition.skills()) {
                if (skill.textKey() == null) {
                    continue;
                }
                if (first) {
                    tooltip.add(at++, Component.empty());
                    first = false;
                }

                tooltip.add(at++, Component.translatable(skill.textKey()).withStyle(ChatFormatting.GOLD));
                String descKey = skill.textKey() + ".desc";
                if (Language.getInstance().has(descKey)) {
                    tooltip.add(at++, Component.translatable(descKey, ShieldSkillText.descriptionArgs(skill).toArray())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        }
        tooltip.add(at++, Component.empty());
        tooltip.add(at++, line("tooltip.shieldrpg.guard.stability", fmt(d.maxStability()), ChatFormatting.AQUA));
        tooltip.add(at++, line("tooltip.shieldrpg.guard.regen", fmt(d.stabilityRegen()), ChatFormatting.AQUA));
        tooltip.add(at++, line("tooltip.shieldrpg.guard.drain", fmt(d.holdResourceDrain()), ChatFormatting.AQUA));
        tooltip.add(at++, line("tooltip.shieldrpg.guard.reduction", fmt(d.damageReductionPercent()) + "%", ChatFormatting.AQUA));
        double projectileReduction = shield.getProjectileDamageReductionPercent();
        if (projectileReduction >= 0 && projectileReduction != d.damageReductionPercent()) {
            tooltip.add(at++, line("tooltip.shieldrpg.guard.reduction_projectile", fmt(projectileReduction) + "%", ChatFormatting.AQUA));
        }
        if (definition != null && definition.raisedMoveSpeed() != 0.0) {
            double pct = definition.raisedMoveSpeed() * 100.0;
            tooltip.add(at++, line("tooltip.shieldrpg.guard.raised_speed", (pct > 0 ? "+" : "") + fmt(pct) + "%",
                    pct > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        }

    }

    private static Component line(String key, String value, ChatFormatting color) {
        return Component.translatable(key, value).withStyle(color);
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? Long.toString((long) v) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }
}
