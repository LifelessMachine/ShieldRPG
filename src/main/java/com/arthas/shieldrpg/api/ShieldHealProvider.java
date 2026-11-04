package com.arthas.shieldrpg.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public interface ShieldHealProvider extends ShieldProvider {

    void heal(Player caster, LivingEntity target, double amount);
}
