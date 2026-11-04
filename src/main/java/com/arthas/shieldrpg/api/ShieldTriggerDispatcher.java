package com.arthas.shieldrpg.api;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface ShieldTriggerDispatcher {

    void fire(ServerPlayer player, ItemStack shield, String trigger, @Nullable Entity directSource,
              @Nullable LivingEntity attacker, float amount, Map<String, Object> extras);
}
