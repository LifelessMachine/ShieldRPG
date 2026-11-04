package com.arthas.shieldrpg.stamina;

import com.arthas.shieldrpg.ShieldRPG;
import com.arthas.shieldrpg.api.DrainResult;
import com.arthas.shieldrpg.api.ShieldGuardContext;
import com.arthas.shieldrpg.api.ShieldResourceProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class StaminaProvider implements ShieldResourceProvider {
    public static final String ID = ShieldRPG.MODID + ":stamina";

    public static boolean isActive() {
        return ID.equals(com.arthas.shieldrpg.internal.ShieldResources.active().id());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean canRaiseGuard(Player player, ShieldGuardContext context) {
        return StaminaState.get(player) >= context.holdDrainPerSecond();
    }

    @Override
    public DrainResult drain(ServerPlayer player, double cost, ShieldGuardContext context) {
        double value = StaminaState.get(player);
        if (value < cost) {
            StaminaState.set(player.getUUID(), 0.0);
            return DrainResult.DEPLETED;
        }
        StaminaState.set(player.getUUID(), value - cost);
        return DrainResult.OK;
    }

    @Override
    public Component depletedMessage() {
        return Component.literal("[OUT OF STAMINA]").withStyle(ChatFormatting.YELLOW);
    }
}
