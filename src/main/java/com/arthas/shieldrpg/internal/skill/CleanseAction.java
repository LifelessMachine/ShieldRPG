package com.arthas.shieldrpg.internal.skill;

import com.arthas.shieldrpg.ShieldRPG;
import java.util.ArrayList;

import com.arthas.shieldrpg.api.ShieldSkillAction;
import com.arthas.shieldrpg.api.ShieldSkillContext;
import com.arthas.shieldrpg.api.ShieldSkillParams;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class CleanseAction implements ShieldSkillAction {
    public static final String ID = ShieldRPG.MODID + ":cleanse";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(ShieldSkillContext context, ShieldSkillParams params) {
        LivingEntity target = context.target();
        if (target == null) {
            return;
        }
        for (MobEffectInstance effect : new ArrayList<>(target.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                target.removeEffect(effect.getEffect());
            }
        }
    }
}
