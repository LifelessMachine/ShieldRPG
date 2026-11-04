package com.arthas.shieldrpg.api;

import net.minecraft.server.level.ServerPlayer;

public interface ShieldStunProvider extends ShieldProvider {

    void applyStun(ServerPlayer target, int ticks);
}
