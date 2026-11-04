package com.arthas.shieldrpg.internal.skill;

import com.arthas.shieldrpg.ShieldRPG;
import com.arthas.shieldrpg.api.ShieldSkillAction;
import com.arthas.shieldrpg.api.ShieldSkillContext;
import com.arthas.shieldrpg.api.ShieldSkillParams;

import net.minecraft.world.entity.LivingEntity;

public final class IgniteAction implements ShieldSkillAction {
    public static final String ID = ShieldRPG.MODID + ":ignite";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(ShieldSkillContext context, ShieldSkillParams params) {
        LivingEntity target = context.target();
        if (target != null && target != context.player()) {
            target.igniteForSeconds((float) params.getDouble("seconds", 5.0));
        }
    }
}
