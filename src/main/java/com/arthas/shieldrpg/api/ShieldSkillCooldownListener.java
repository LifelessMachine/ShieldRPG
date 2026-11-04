package com.arthas.shieldrpg.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface ShieldSkillCooldownListener extends ShieldProvider {

    void onSkillCooldownStarted(ServerPlayer player, ItemStack shield, int skillIndex, ShieldDefinition.Skill skill, int ticks);
}
