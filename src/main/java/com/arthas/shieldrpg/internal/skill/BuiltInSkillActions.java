package com.arthas.shieldrpg.internal.skill;

import com.arthas.shieldrpg.api.ShieldApi;
import com.arthas.shieldrpg.api.ShieldSkillAction;
import com.arthas.shieldrpg.api.ShieldSkillContext;
import com.arthas.shieldrpg.api.ShieldSkillParams;
import com.arthas.shieldrpg.internal.ShieldEffects;
import com.arthas.shieldrpg.internal.ShieldTasks;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

public final class BuiltInSkillActions {

    private BuiltInSkillActions() {}

    public static void register() {
        add("shieldrpg:damage", BuiltInSkillActions::damage);
        add("shieldrpg:apply_effect", BuiltInSkillActions::applyEffect);
        add("shieldrpg:heal", BuiltInSkillActions::heal);
        add("shieldrpg:knockback", BuiltInSkillActions::knockback);
        add("shieldrpg:reflect_projectile", BuiltInSkillActions::reflectProjectile);
    }

    private interface Body {
        void run(ShieldSkillContext context, ShieldSkillParams params);
    }

    private static void add(String id, Body body) {
        ShieldApi.registerSkillAction(new ShieldSkillAction() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public void execute(ShieldSkillContext context, ShieldSkillParams params) {
                body.run(context, params);
            }
        });
    }

    private static void damage(ShieldSkillContext context, ShieldSkillParams params) {
        LivingEntity target = context.target();
        if (target != null && context.player().level() instanceof ServerLevel level) {
            if (params.getBoolean("ignore_invulnerability", false)) {
                target.damageCooldownTime = 0;
            }
            target.hurtServer(level, level.damageSources().playerAttack(context.player()), (float) params.getDouble("amount", 1.0));
        }
    }

    private static void applyEffect(ShieldSkillContext context, ShieldSkillParams params) {
        LivingEntity target = context.target();
        if (target == null) {
            return;
        }
        var effect = BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(params.getString("effect", "minecraft:slowness")));
        effect.ifPresent(holder -> target.addEffect(new MobEffectInstance(holder,
                params.getInt("duration_ticks", 60), params.getInt("amplifier", 0), false, true, true)));
    }

    private static void heal(ShieldSkillContext context, ShieldSkillParams params) {
        LivingEntity target = context.target();
        if (target == null) {
            return;
        }
        double amount = params.getDouble("amount", 0.0)
                + target.getMaxHealth() * params.getDouble("percent_max_health", 0.0) / 100.0;
        if (amount > 0.0) {
            ShieldEffects.heal(context.player(), target, amount);
        }
    }

    private static void knockback(ShieldSkillContext context, ShieldSkillParams params) {
        LivingEntity target = context.target();
        if (target != null && target != context.player() && context.player().level() instanceof ServerLevel level) {
            target.knockback(params.getDouble("strength", 0.5),
                    context.player().getX() - target.getX(), context.player().getZ() - target.getZ(),
                    level.damageSources().playerAttack(context.player()), context.amount());
        }
    }

    private static void reflectProjectile(ShieldSkillContext context, ShieldSkillParams params) {
        if (!(context.directSource() instanceof Projectile projectile) || projectile.isRemoved()) {
            return;
        }
        var player = context.player();
        Vec3 incoming = projectile.getDeltaMovement();
        double speed = incoming.length();
        if (speed <= 0.0) {
            return;
        }
        LivingEntity shooter = context.attacker();

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + player.getBbHeight() * 0.65, player.getZ(),
                    12, 0.3, 0.3, 0.3, 0.1);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        ShieldTasks.runAfterThisTick(() -> {
            if (projectile.isRemoved()) {
                return;
            }
            Vec3 chest = player.position().add(0.0, player.getBbHeight() * 0.65, 0.0);
            Vec3 direction;
            if (shooter != null && shooter.isAlive()) {
                direction = shooter.getBoundingBox().getCenter().subtract(chest).normalize();
            } else {
                direction = incoming.scale(-1.0).normalize();
            }

            Vec3 start = chest.add(direction.scale(player.getBbWidth() * 0.5 + projectile.getBbWidth() + 0.4));
            projectile.setPos(start.x, start.y, start.z);
            projectile.setDeltaMovement(direction.scale(speed));
            projectile.setOwner(player);
            projectile.needsSync = true;
        });
    }
}
