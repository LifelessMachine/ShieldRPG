package com.arthas.shieldrpg.stamina;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.arthas.shieldrpg.ShieldRPG;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ShieldRPG.MODID)
public final class StaminaEvents {
    private static final Map<UUID, Float> LAST_SENT = new ConcurrentHashMap<>();

    private StaminaEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!StaminaProvider.isActive()) {
            return;
        }
        double value = StaminaState.get(player);
        if (!player.isBlocking() && value < StaminaState.MAX) {
            value = Math.min(StaminaState.MAX, value + StaminaState.REGEN_PER_SECOND / 20.0);
            StaminaState.set(player.getUUID(), value);
        }
        if (player.tickCount % 4 == 0) {
            sendIfChanged(player, (float) value);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refill(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refill(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        StaminaState.forget(event.getEntity().getUUID());
        LAST_SENT.remove(event.getEntity().getUUID());
    }

    private static void refill(ServerPlayer player) {
        StaminaState.set(player.getUUID(), StaminaState.MAX);
        LAST_SENT.remove(player.getUUID());
        sendIfChanged(player, (float) StaminaState.MAX);
    }

    private static void sendIfChanged(ServerPlayer player, float value) {
        Float last = LAST_SENT.get(player.getUUID());
        if (last != null && Math.abs(last - value) < 0.05F) {
            return;
        }
        LAST_SENT.put(player.getUUID(), value);
        PacketDistributor.sendToPlayer(player, new StaminaSyncPayload(value, (float) StaminaState.MAX));
    }
}
