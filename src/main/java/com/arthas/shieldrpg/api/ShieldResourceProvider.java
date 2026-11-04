package com.arthas.shieldrpg.api;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public interface ShieldResourceProvider extends ShieldProvider {

    boolean canRaiseGuard(Player player, ShieldGuardContext context);

    DrainResult drain(ServerPlayer player, double cost, ShieldGuardContext context);

    default Component depletedMessage() {
        return Component.literal("[OUT OF RESOURCE]").withStyle(ChatFormatting.YELLOW);
    }
}
