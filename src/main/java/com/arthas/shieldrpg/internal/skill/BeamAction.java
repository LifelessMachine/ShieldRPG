package com.arthas.shieldrpg.internal.skill;

import com.arthas.shieldrpg.ShieldRPG;
import com.arthas.shieldrpg.api.ShieldSkillAction;
import com.arthas.shieldrpg.api.ShieldSkillContext;
import com.arthas.shieldrpg.api.ShieldSkillParams;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class BeamAction implements ShieldSkillAction {
    public static final String ID = ShieldRPG.MODID + ":beam";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(ShieldSkillContext context, ShieldSkillParams params) {
        LivingEntity target = context.target();
        if (target == null || target == context.player() || !(context.player().level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 from = context.player().getEyePosition().add(0.0, -0.25, 0.0);
        Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        Vec3 delta = to.subtract(from);
        int points = Math.max(2, (int) (delta.length() * 3.0));
        for (int i = 0; i <= points; i++) {
            Vec3 p = from.add(delta.scale(i / (double) points));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 3, 0.06, 0.06, 0.06, 0.01);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.05);
            }
        }
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, to.x, to.y, to.z, 30, 0.4, 0.4, 0.4, 0.3);
        target.hurtServer(level, level.damageSources().playerAttack(context.player()), (float) params.getDouble("amount", 4.0));
    }
}
