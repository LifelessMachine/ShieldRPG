package com.arthas.shieldrpg.stamina;

import com.arthas.shieldrpg.ShieldRPG;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class StaminaHud implements GuiLayer {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ShieldRPG.MODID, "stamina_hud");

    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 5;
    private static final int GREEN = 0xFFB6E84A;
    private static final int RED = 0xFFFF4040;

    @Override
    public void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator() || player.isCreative() || !StaminaProvider.isActive()) {
            return;
        }
        double value = StaminaState.get(player);
        double max = Math.max(1.0, StaminaState.max());
        double ratio = Math.max(0.0, Math.min(1.0, value / max));

        int left = g.guiWidth() / 2 + 10;
        int top = g.guiHeight() - 49 + 2;
        if (player.getAirSupply() < player.getMaxAirSupply()) {
            top -= 10;
        }
        int color = ratio < 0.2 ? RED : GREEN;

        g.fill(left - 1, top - 1, left + BAR_WIDTH + 1, top + BAR_HEIGHT + 1, 0xFF101010);
        g.fill(left, top, left + BAR_WIDTH, top + BAR_HEIGHT, 0xFF303830);
        int fill = (int) Math.round(BAR_WIDTH * ratio);
        if (fill > 0) {
            g.fill(left, top, left + fill, top + BAR_HEIGHT, color);
            g.fill(left, top, left + fill, top + 1, lighten(color));
        }
        Font font = mc.font;
        String number = Integer.toString((int) Math.floor(value));
        g.text(font, Component.literal(number), left - font.width(number) - 3, top - 2, color, false);
    }

    private static int lighten(int argb) {
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 50);
        int gr = Math.min(255, ((argb >> 8) & 0xFF) + 50);
        int b = Math.min(255, (argb & 0xFF) + 50);
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    @EventBusSubscriber(modid = ShieldRPG.MODID, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {}

        @SubscribeEvent
        public static void registerLayers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, ID, new StaminaHud());
        }
    }
}
