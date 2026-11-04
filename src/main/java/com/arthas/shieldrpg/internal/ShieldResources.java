package com.arthas.shieldrpg.internal;

import com.arthas.shieldrpg.ShieldRPG;
import com.arthas.shieldrpg.api.DrainResult;
import com.arthas.shieldrpg.api.ShieldApi;
import com.arthas.shieldrpg.api.ShieldGuardContext;
import com.arthas.shieldrpg.api.ShieldResourceProvider;
import com.arthas.shieldrpg.config.ShieldStats;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ShieldResources {

    public static final String AUTO = "auto";

    private static final ShieldResourceProvider FREE = new NoResourceProvider();
    private static volatile boolean errorLogged;

    private ShieldResources() {}

    public static ShieldResourceProvider active() {
        String wanted;
        try {
            wanted = ShieldStats.RESOURCE_PROVIDER.get();
        } catch (RuntimeException configNotLoaded) {
            wanted = AUTO;
        }
        var all = ShieldApi.getResourceProviders();
        if (!AUTO.equalsIgnoreCase(wanted)) {
            for (var reg : all) {
                if (reg.provider().id().equals(wanted)) {
                    return reg.provider();
                }
            }
        }
        return all.isEmpty() ? FREE : all.get(0).provider();
    }

    public static boolean canRaiseGuard(Player player, ShieldGuardContext ctx) {
        try {
            return active().canRaiseGuard(player, ctx);
        } catch (Throwable t) {
            logOnce(t);
            return true;
        }
    }

    public static DrainResult drain(ServerPlayer player, double cost, ShieldGuardContext ctx) {
        try {
            return active().drain(player, cost, ctx);
        } catch (Throwable t) {
            logOnce(t);
            return DrainResult.OK;
        }
    }

    public static Component depletedMessage() {
        try {
            return active().depletedMessage();
        } catch (Throwable t) {
            logOnce(t);
            return FREE.depletedMessage();
        }
    }

    private static void logOnce(Throwable t) {
        if (!errorLogged) {
            errorLogged = true;
            ShieldRPG.LOGGER.error("Shield resource provider failed; treating the guard as free", t);
        }
    }

    private static final class NoResourceProvider implements ShieldResourceProvider {
        @Override public String id() { return "shieldrpg:none"; }
        @Override public boolean canRaiseGuard(Player player, ShieldGuardContext context) { return true; }
        @Override public DrainResult drain(ServerPlayer player, double cost, ShieldGuardContext context) { return DrainResult.OK; }
    }
}
