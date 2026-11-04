package com.arthas.shieldrpg.internal.skill;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.arthas.shieldrpg.ShieldRPG;
import com.arthas.shieldrpg.api.ShieldApi;
import com.arthas.shieldrpg.api.ShieldDefinition;
import com.arthas.shieldrpg.api.ShieldSkillAction;
import com.arthas.shieldrpg.api.ShieldSkillContext;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ShieldSkillRunner {

    private static final Map<String, Long> COOLDOWN_END = new ConcurrentHashMap<>();
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    private ShieldSkillRunner() {}

    public static void run(ServerPlayer player, ItemStack stack, ShieldDefinition definition, String trigger,
                           @Nullable Entity directSource, @Nullable LivingEntity attacker, float amount,
                           Map<String, Object> extras) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        runSkills(player, level, stack, definition, trigger, directSource, attacker, amount, extras);
    }

    private static void runSkills(ServerPlayer player, ServerLevel level, ItemStack stack, ShieldDefinition definition, String trigger,
                                  @Nullable Entity directSource, @Nullable LivingEntity attacker, float amount,
                                  Map<String, Object> extras) {
        long now = level.getGameTime();
        var skills = definition.skills();
        for (int i = 0; i < skills.size(); i++) {
            ShieldDefinition.Skill skill = skills.get(i);
            if (!skill.trigger().equals(trigger)) {
                continue;
            }

            if (trigger.equals(ShieldTriggers.STABILITY_LOW)) {

                double ratioBefore = number(extras, "stability_ratio_before");
                double ratioAfter = number(extras, "stability_ratio");
                if (!(ratioBefore > skill.threshold() && ratioAfter <= skill.threshold())) {
                    continue;
                }
            }

            String cooldownKey = player.getUUID() + "|" + definition.itemId() + "|" + i;
            if (skill.cooldownTicks() > 0 && now < COOLDOWN_END.getOrDefault(cooldownKey, Long.MIN_VALUE)) {
                continue;
            }

            Long previousEnd = null;
            if (skill.cooldownTicks() > 0) {
                previousEnd = COOLDOWN_END.put(cooldownKey, now + skill.cooldownTicks());
            }
            boolean fired = false;
            for (ShieldDefinition.Step step : skill.steps()) {
                List<LivingEntity> targets = resolveTargets(step, player, level, directSource, attacker);
                if (step.maxRange() > 0.0) {
                    double maxSqr = step.maxRange() * step.maxRange();
                    targets = targets.stream().filter(target -> target.distanceToSqr(player) <= maxSqr).toList();
                }
                if (targets.isEmpty()) {
                    continue;
                }

                ShieldSkillAction action = ShieldApi.getSkillAction(step.action());
                if (action == null) {
                    reportOnce("unknown:" + definition.itemId() + ":" + step.action(),
                            "Shield " + definition.itemId() + " uses unknown skill action '" + step.action() + "'");
                    continue;
                }

                for (LivingEntity target : targets) {
                    try {
                        action.execute(new ShieldSkillContext(trigger, player, stack, directSource, attacker, target, amount, extras), step.params());
                    } catch (Throwable t) {
                        reportOnce("error:" + step.action(), "Skill action '" + step.action() + "' failed: " + t);
                        continue;
                    }
                    fired = true;
                    playParticle(level, target, step);
                }
            }
            if (!fired) {
                if (skill.cooldownTicks() > 0) {
                    if (previousEnd == null) {
                        COOLDOWN_END.remove(cooldownKey);
                    } else {
                        COOLDOWN_END.put(cooldownKey, previousEnd);
                    }
                }
                continue;
            }

            if (skill.cooldownTicks() > 0) {
                notifyCooldownListeners(player, stack, definition, i, skill);
            }
            playSound(level, player, skill);
        }
    }

    private static void notifyCooldownListeners(ServerPlayer player, ItemStack stack, ShieldDefinition definition, int skillIndex,
                                                ShieldDefinition.Skill skill) {
        if (stack.isEmpty() || !ShieldApi.isSkillCooldownDisplayEnabled(stack.getItem(), definition)) {
            return;
        }
        for (var listener : ShieldApi.getSkillCooldownListeners()) {
            try {
                listener.onSkillCooldownStarted(player, stack, skillIndex, skill, skill.cooldownTicks());
            } catch (Throwable t) {
                reportOnce("cooldown-listener:" + listener.id(), "Skill cooldown listener '" + listener.id() + "' failed: " + t);
            }
        }
    }

    private static double number(Map<String, Object> extras, String key) {
        return extras.get(key) instanceof Number n ? n.doubleValue() : 0.0;
    }

    public static void clearAll() {
        COOLDOWN_END.clear();
    }

    public static boolean isHostile(ServerPlayer player, LivingEntity entity) {
        if (entity == player || !entity.isAlive() || entity instanceof Player || entity instanceof ArmorStand) {
            return false;
        }
        return entity instanceof Enemy || (entity instanceof Mob mob && mob.getTarget() == player);
    }

    private static List<LivingEntity> resolveTargets(ShieldDefinition.Step skill, ServerPlayer player, ServerLevel level,
                                                      @Nullable Entity directSource, @Nullable LivingEntity attacker) {
        switch (skill.target()) {
            case "self":
                return List.of(player);
            case "attacker_melee":
                return directSource instanceof LivingEntity direct && direct.isAlive() && direct != player
                        ? List.of(direct) : List.of();
            case "nearby": {
                double radius = skill.radius();
                return level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                        entity -> isHostile(player, entity) && entity.distanceToSqr(player) <= radius * radius);
            }
            default: {
                LivingEntity target = directSource instanceof LivingEntity direct ? direct : attacker;
                return target != null && target.isAlive() && target != player ? List.of(target) : List.of();
            }
        }
    }

    private static void playParticle(ServerLevel level, LivingEntity target, ShieldDefinition.Step skill) {
        ShieldDefinition.ParticleFx fx = skill.particle();
        if (fx == null) {
            return;
        }
        try {
            var type = BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse(fx.type()));
            if (type instanceof SimpleParticleType simple) {
                level.sendParticles(simple, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        fx.count(), fx.spread(), fx.spread(), fx.spread(), fx.speed());
            } else {
                reportOnce("particle:" + fx.type(), "Skill particle '" + fx.type() + "' is unknown or not a simple particle");
            }
        } catch (RuntimeException e) {
            reportOnce("particle:" + fx.type(), "Skill particle '" + fx.type() + "' is invalid: " + e.getMessage());
        }
    }

    private static void playSound(ServerLevel level, ServerPlayer player, ShieldDefinition.Skill skill) {
        ShieldDefinition.SoundFx sound = skill.sound();
        if (sound == null) {
            return;
        }
        try {
            SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(Identifier.parse(sound.id()));
            if (event != null) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), event, SoundSource.PLAYERS,
                        sound.volume(), sound.pitch());
            } else {
                reportOnce("sound:" + sound.id(), "Skill sound '" + sound.id() + "' is unknown");
            }
        } catch (RuntimeException e) {
            reportOnce("sound:" + sound.id(), "Skill sound '" + sound.id() + "' is invalid: " + e.getMessage());
        }
    }

    private static void reportOnce(String key, String message) {
        if (REPORTED.add(key)) {
            ShieldRPG.LOGGER.warn(message);
        }
    }
}
