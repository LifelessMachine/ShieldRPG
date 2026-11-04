package com.arthas.shieldrpg.internal;

import com.arthas.shieldrpg.ShieldRPG;
import com.arthas.shieldrpg.api.ShieldDefinition;
import com.arthas.shieldrpg.api.ShieldSkillCooldownListener;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class SkillCooldownTracker implements ShieldSkillCooldownListener {
    public static final String END_KEY = "shieldrpg_cd_end";
    public static final String LENGTH_KEY = "shieldrpg_cd_len";

    @Override
    public String id() {
        return ShieldRPG.MODID + ":cooldown_tracker";
    }

    @Override
    public void onSkillCooldownStarted(ServerPlayer player, ItemStack shield, int skillIndex, ShieldDefinition.Skill skill, int ticks) {
        if (shield.isEmpty()) {
            return;
        }
        double end = player.level().getGameTime() + ticks;
        CustomData.update(DataComponents.CUSTOM_DATA, shield, tag -> {
            tag.putDouble(END_KEY, end);
            tag.putDouble(LENGTH_KEY, ticks);
        });
    }
}
