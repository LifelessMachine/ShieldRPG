package com.arthas.shieldrpg.internal.skill;

import com.arthas.shieldrpg.ShieldRPG;
import com.arthas.shieldrpg.api.ShieldSkillAction;
import com.arthas.shieldrpg.api.ShieldSkillContext;
import com.arthas.shieldrpg.api.ShieldSkillParams;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class BlastAction implements ShieldSkillAction {
    public static final String ID = ShieldRPG.MODID + ":blast";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(ShieldSkillContext context, ShieldSkillParams params) {
        ServerPlayer player = context.player();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double radius = Math.max(0.5, params.getDouble("radius", 4.0));
        float amount = (float) params.getDouble("amount", 6.0);
        double strength = params.getDouble("strength", 1.5);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);

        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 60, radius * 0.35, 0.6, radius * 0.35, 0.08);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 40, radius * 0.4, 0.5, radius * 0.4, 0.04);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.3, player.getZ(), 30, radius * 0.5, 0.1, radius * 0.5, 0.15);

        var damageSource = level.damageSources().playerAttack(player);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> ShieldSkillRunner.isHostile(player, e) && e.distanceToSqr(player) <= radius * radius)) {
            entity.hurtServer(level, damageSource, amount);
            entity.knockback(strength, player.getX() - entity.getX(), player.getZ() - entity.getZ(), damageSource, amount);
        }
    }
}
