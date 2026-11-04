package com.arthas.shieldrpg.internal.skill;

import com.arthas.shieldrpg.ShieldRPG;
import com.arthas.shieldrpg.api.ShieldSkillAction;
import com.arthas.shieldrpg.api.ShieldSkillContext;
import com.arthas.shieldrpg.api.ShieldSkillParams;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class PullAction implements ShieldSkillAction {
    public static final String ID = ShieldRPG.MODID + ":pull";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(ShieldSkillContext context, ShieldSkillParams params) {
        ServerPlayer player = context.player();
        LivingEntity target = context.target();
        if (target == null || target == player || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        target.knockback(params.getDouble("strength", 1.5), target.getX() - player.getX(), target.getZ() - player.getZ(),
                level.damageSources().playerAttack(player), context.amount());
    }
}
