package com.arthas.shieldrpg.internal;

import com.arthas.shieldrpg.ShieldRPG;
import com.arthas.shieldrpg.api.PerfectGuardContext;
import com.arthas.shieldrpg.api.ShieldApi;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class ShieldEffects {

    private static volatile boolean errorLogged;

    private ShieldEffects() {}

    public static void stun(ServerPlayer player, int ticks) {
        var providers = ShieldApi.getStunProviders();
        if (!providers.isEmpty()) {
            try {
                providers.get(0).provider().applyStun(player, ticks);
                return;
            } catch (Throwable t) {
                logOnce(t);
            }
        }
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 6, false, false, true));
    }

    public static void heal(Player caster, LivingEntity target, double amount) {
        var providers = ShieldApi.getHealProviders();
        if (!providers.isEmpty()) {
            try {
                providers.get(0).provider().heal(caster, target, amount);
                return;
            } catch (Throwable t) {
                logOnce(t);
            }
        }
        target.heal((float) amount);
    }

    public static void firePerfectGuard(PerfectGuardContext context) {
        for (var registration : ShieldApi.getPerfectGuardListeners()) {
            try {
                registration.provider().onPerfectGuard(context);
            } catch (Throwable t) {
                logOnce(t);
            }
        }
    }

    private static void logOnce(Throwable t) {
        if (!errorLogged) {
            errorLogged = true;
            ShieldRPG.LOGGER.error("Shield stun/heal provider failed; using the vanilla fallback", t);
        }
    }
}
