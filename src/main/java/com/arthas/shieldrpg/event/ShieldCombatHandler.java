package com.arthas.shieldrpg.event;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.arthas.shieldrpg.ShieldRPG;
import com.arthas.shieldrpg.api.PerfectGuardContext;
import com.arthas.shieldrpg.api.DrainResult;
import com.arthas.shieldrpg.api.ShieldGuardContext;
import com.arthas.shieldrpg.config.ShieldStats;
import com.arthas.shieldrpg.internal.ShieldEffects;
import com.arthas.shieldrpg.internal.ShieldResources;
import com.arthas.shieldrpg.internal.ShieldTasks;
import com.arthas.shieldrpg.internal.skill.ShieldSkillRunner;
import com.arthas.shieldrpg.internal.skill.ShieldTriggers;
import com.arthas.shieldrpg.item.NorseShieldItem;
import com.arthas.shieldrpg.item.ShieldBehavior;
import com.arthas.shieldrpg.item.ShieldBehaviors;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = ShieldRPG.MODID)
public final class ShieldCombatHandler {

    private static final Map<UUID, Integer> BLOCKING_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, ItemStack> RAISED_STACK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> REGEN_DELAY_TICKS = new ConcurrentHashMap<>();

    private static final Map<UUID, Map<UUID, HitMark>> LAST_STABILITY_HIT = new ConcurrentHashMap<>();

    private static final Map<UUID, Long> LAST_PERFECT_GUARD = new ConcurrentHashMap<>();
    private static final Map<UUID, Incoming> INCOMING_AMOUNT = new ConcurrentHashMap<>();

    private static final Map<UUID, Long> UNBLOCKED_CANDIDATE = new ConcurrentHashMap<>();

    private record HitMark(long time, boolean perfect) {}

    private record Incoming(float amount, long time) {}

    private ShieldCombatHandler() {}

    public static double getStability(Player player, ShieldBehavior shield) {
        ItemStack held = getHeldNorseShield(player);
        if (held != null) {
            return NorseShieldItem.getStability(held, shield.getMaxStability());
        }
        return shield.getMaxStability();
    }

    public static ItemStack getHeldNorseShield(Player player) {
        ItemStack off = player.getOffhandItem();
        if (ShieldBehaviors.of(off) != null) {
            return off;
        }
        ItemStack main = player.getMainHandItem();
        if (ShieldBehaviors.of(main) != null) {
            return main;
        }
        return null;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID uuid = player.getUUID();
        ItemStack useItem = player.getUseItem();

        updateRaisedMoveSpeed(player, useItem);

        REGEN_DELAY_TICKS.computeIfPresent(uuid, (k, v) -> v > 0 ? v - 1 : 0);

        if (player.isUsingItem() && ShieldBehaviors.of(useItem) instanceof ShieldBehavior shield) {
            int ticks = BLOCKING_TICKS.merge(uuid, 1, Integer::sum);
            if (ticks == 1) {
                RAISED_STACK.put(uuid, useItem);
                ShieldTriggers.fire(player, useItem, shield, ShieldTriggers.SHIELD_RAISED, null, null, 0.0F);
            }

            if (ticks % 10 == 0 && !player.getAbilities().instabuild) {
                double drainCost = shield.getHoldStaminaDrain() * 0.5;
                var guard = new ShieldGuardContext(useItem, shield.getHoldStaminaDrain());
                if (ShieldResources.drain(player, drainCost, guard) == DrainResult.DEPLETED) {

                    player.stopUsingItem();
                    BLOCKING_TICKS.remove(uuid);
                    RAISED_STACK.remove(uuid);
                    ShieldTriggers.fire(player, useItem, shield, ShieldTriggers.RESOURCE_EXHAUSTED, null, null, 0.0F);
                    ShieldTriggers.fire(player, useItem, shield, ShieldTriggers.SHIELD_LOWERED, null, null, 0.0F);

                    int cooldown = ShieldStats.EXHAUSTION_COOLDOWN_TICKS.get();
                    player.getCooldowns().addCooldown(useItem, cooldown);
                    player.sendOverlayMessage(ShieldResources.depletedMessage());
                }
            }
        } else {
            ItemStack raised = RAISED_STACK.remove(uuid);
            if (BLOCKING_TICKS.remove(uuid) != null && raised != null && !raised.isEmpty()
                    && ShieldBehaviors.of(raised) instanceof ShieldBehavior lowered) {
                ShieldTriggers.fire(player, raised, lowered, ShieldTriggers.SHIELD_LOWERED, null, null, 0.0F);
            }

            if (player.tickCount % 4 == 0) {
                processShieldStability(player, player.getOffhandItem());
                if (player.getMainHandItem() != player.getOffhandItem()) {
                    processShieldStability(player, player.getMainHandItem());
                }
            }
        }
    }

    private static final net.minecraft.resources.Identifier RAISED_SPEED_ID =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(com.arthas.shieldrpg.ShieldRPG.MODID, "raised_move_speed");

    private static void updateRaisedMoveSpeed(ServerPlayer player, ItemStack useItem) {
        var attribute = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        double wanted = 0.0;
        if (player.isUsingItem() && ShieldBehaviors.of(useItem) != null) {
            var definition = com.arthas.shieldrpg.api.ShieldApi.getShieldDefinition(useItem.getItem());
            if (definition != null) {
                wanted = definition.raisedMoveSpeed();
            }
        }
        var current = attribute.getModifier(RAISED_SPEED_ID);
        if (wanted == 0.0) {
            if (current != null) {
                attribute.removeModifier(RAISED_SPEED_ID);
            }
        } else if (current == null || current.amount() != wanted) {
            attribute.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    RAISED_SPEED_ID, wanted, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    private static void processShieldStability(ServerPlayer player, ItemStack stack) {
        if (stack != null && !stack.isEmpty() && ShieldBehaviors.of(stack) instanceof ShieldBehavior shield) {
            double max = shield.getMaxStability();

            if (NorseShieldItem.isGuardBroken(stack)) {
                if (player.getCooldowns().isOnCooldown(stack)) {
                    return;
                }

                NorseShieldItem.resetGuardBreak(stack, max);
                syncShieldToClient(player, stack);
                ShieldTriggers.fire(player, stack, shield, ShieldTriggers.GUARD_RECOVERED, null, null, 0.0F);
                return;
            }

            int delay = REGEN_DELAY_TICKS.getOrDefault(player.getUUID(), 0);
            if (delay > 0) {
                return;
            }

            double current = NorseShieldItem.getStability(stack, max);
            if (current < max - 0.001) {
                double regen = shield.getStabilityRegenRate() * 0.2;
                double newStab = Math.min(max, current + regen);
                NorseShieldItem.setStability(stack, newStab, max);
                syncShieldToClient(player, stack);
                ShieldTriggers.fireStabilityChange(player, stack, shield, current, newStab, max);
            }
        }
    }

    public static void syncShieldToClient(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || player.connection == null) {
            return;
        }
        int targetSlot = -1;
        for (int i = 0; i < player.inventoryMenu.slots.size(); i++) {
            ItemStack slotItem = player.inventoryMenu.slots.get(i).getItem();
            if (slotItem == stack) {
                targetSlot = i;
                break;
            }
        }
        if (targetSlot != -1) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                    player.inventoryMenu.containerId,
                    player.inventoryMenu.getStateId(),
                    targetSlot,
                    stack
            ));
        }
    }

    private static void pushParticles(ServerLevel level, ServerPlayer player, LivingEntity target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-4) {
            return;
        }
        dx /= len;
        dz /= len;
        double chestY = player.getY() + player.getBbHeight() * 0.6;
        double reach = Math.min(len, 3.0);
        for (double d = 0.8; d <= reach; d += 0.45) {
            level.sendParticles(ParticleTypes.CLOUD, player.getX() + dx * d, chestY, player.getZ() + dz * d,
                    0, dx, 0.02, dz, 0.5);
        }
        double hitY = target.getY() + target.getBbHeight() * 0.5;
        level.sendParticles(ParticleTypes.POOF, target.getX() - dx * target.getBbWidth() * 0.4, hitY,
                target.getZ() - dz * target.getBbWidth() * 0.4, 0, dx, 0.05, dz, 0.35);
        level.sendParticles(ParticleTypes.POOF, target.getX(), hitY + 0.2, target.getZ(), 0, dx, 0.1, dz, 0.3);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isUsingItem()
                && ShieldBehaviors.of(player.getUseItem()) != null
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)
                && event.getSource().getSourcePosition() != null) {
            INCOMING_AMOUNT.put(player.getUUID(), new Incoming(event.getAmount(), player.level().getGameTime()));
            UNBLOCKED_CANDIDATE.put(player.getUUID(), player.level().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        INCOMING_AMOUNT.remove(player.getUUID());
        Long candidateTime = UNBLOCKED_CANDIDATE.remove(player.getUUID());
        if (candidateTime == null || candidateTime != player.level().getGameTime()) {
            return;
        }
        ItemStack useItem = player.getUseItem();
        if (player.isUsingItem() && ShieldBehaviors.of(useItem) instanceof ShieldBehavior shield) {
            Entity direct = event.getSource().getDirectEntity();
            Entity owner = event.getSource().getEntity();
            ShieldTriggers.fire(player, useItem, shield, ShieldTriggers.HIT_UNBLOCKED, direct,
                    owner instanceof LivingEntity living ? living : null, event.getInflictedDamage());
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack useItem = player.getUseItem();
        if (player.isUsingItem() && ShieldBehaviors.of(useItem) instanceof ShieldBehavior shield) {
            Projectile projectile = event.getProjectile();
            Entity owner = projectile.getOwner();
            ShieldTriggers.fire(player, useItem, shield, ShieldTriggers.PROJECTILE_INCOMING, projectile,
                    owner instanceof LivingEntity living ? living : null, 0.0F);
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getAttackStrengthScale(0.5F) < 0.9F) {
            return;
        }
        ItemStack held = getHeldNorseShield(player);
        if (held == null || !(ShieldBehaviors.of(held) instanceof ShieldBehavior shield)) {
            return;
        }
        Entity victim = event.getTarget();
        float attackDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);

        ShieldTasks.runAfterThisTick(() -> {
            if (victim.isAlive() && player.isAlive()) {
                ShieldTriggers.fire(player, held, shield, ShieldTriggers.SHIELD_BASH, victim,
                        victim instanceof LivingEntity living ? living : null, attackDamage);
            }
        });
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || (event.getSlot() != EquipmentSlot.MAINHAND && event.getSlot() != EquipmentSlot.OFFHAND)) {
            return;
        }
        ItemStack from = event.getFrom();
        ItemStack to = event.getTo();
        boolean sameItem = from.is(to.getItem());
        Map<String, Object> extras = Map.of("slot", event.getSlot().getName());
        if (ShieldBehaviors.of(to) instanceof ShieldBehavior shield && !sameItem) {
            ShieldTriggers.fire(player, to, shield, ShieldTriggers.SHIELD_EQUIPPED, null, null, 0.0F, extras);
        }
        if (ShieldBehaviors.of(from) instanceof ShieldBehavior shield && !sameItem) {
            ShieldTriggers.fire(player, from, shield, ShieldTriggers.SHIELD_UNEQUIPPED, null, null, 0.0F, extras);
        }
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getAbilities().instabuild) {
            return;
        }
        ItemStack stack = event.getItem();
        if (stack.getItem() instanceof NorseShieldItem) {
            return;
        }
        ShieldBehavior shield = ShieldBehaviors.of(stack);
        if (shield != null && !ShieldResources.canRaiseGuard(player, new ShieldGuardContext(stack, shield.getHoldStaminaDrain()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemDestroyed(PlayerDestroyItemEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ShieldBehaviors.of(event.getOriginal()) instanceof ShieldBehavior shield) {
            ShieldTriggers.fire(player, event.getOriginal(), shield, ShieldTriggers.SHIELD_BROKEN_ITEM, null, null, 0.0F);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ShieldTasks.drain();
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack useItem = player.getUseItem();
        if (!(ShieldBehaviors.of(useItem) instanceof ShieldBehavior shield)) {
            return;
        }

        if (!event.getBlocked() || event.getOriginalBlockedDamage() <= 0.0F) {
            return;
        }

        UUID uuid = player.getUUID();
        float damage = event.getBlockedDamage();
        Entity attackerEntity = event.getDamageSource().getEntity();
        LivingEntity attacker = attackerEntity instanceof LivingEntity le ? le : null;

        UNBLOCKED_CANDIDATE.remove(uuid);
        Incoming recorded = INCOMING_AMOUNT.remove(uuid);
        long tickNow = player.level().getGameTime();
        float incoming = recorded != null && recorded.time() == tickNow ? Math.max(recorded.amount(), damage) : damage;

        Entity source = event.getDamageSource().getDirectEntity();
        int windowTicks = ShieldStats.REPEAT_HIT_WINDOW_TICKS.get();
        long now = player.level().getGameTime();
        Map<UUID, HitMark> hits = null;
        if (source != null && windowTicks > 0) {
            hits = LAST_STABILITY_HIT.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
            hits.values().removeIf(m -> now - m.time() >= windowTicks);
            HitMark last = hits.get(source.getUUID());
            if (last != null) {

                event.setShieldDamage(0);
                if (last.perfect()) {
                    event.setBlockedDamage(incoming);
                }
                REGEN_DELAY_TICKS.put(uuid, ShieldStats.RECHARGE_DELAY_TICKS.get());
                return;
            }
        }

        int perfectBlockTicks = shield.getPerfectGuardWindowTicks() >= 0
                ? shield.getPerfectGuardWindowTicks() : ShieldStats.PERFECT_BLOCK_TICKS.get();
        var blocksAttacks = useItem.get(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS);
        int blockDelay = blocksAttacks != null ? blocksAttacks.blockDelayTicks() : 0;
        int blockingTicks = player.getTicksUsingItem() - blockDelay;
        long lastPerfect = LAST_PERFECT_GUARD.getOrDefault(uuid, Long.MIN_VALUE / 2);
        boolean perfectReady = now - lastPerfect >= ShieldStats.PERFECT_GUARD_COOLDOWN_TICKS.get();
        if (blockingTicks <= perfectBlockTicks && perfectReady) {
            LAST_PERFECT_GUARD.put(uuid, now);

            event.setBlockedDamage(incoming);
            event.setShieldDamage(0);
            if (hits != null) {
                hits.put(source.getUUID(), new HitMark(now, true));
            }

            PerfectGuardContext context = new PerfectGuardContext(player, useItem, event.getDamageSource(),
                    incoming, shield.getPerfectGuardKnockback());
            ShieldEffects.firePerfectGuard(context);
            ShieldTriggers.fire(player, useItem, shield, ShieldTriggers.PERFECT_GUARD, source, attacker, incoming);
            ShieldTriggers.fire(player, useItem, shield, ShieldTriggers.BLOCKED_HIT, source, attacker, incoming);
            ShieldTriggers.fire(player, useItem, shield,
                    source instanceof Projectile ? ShieldTriggers.BLOCKED_PROJECTILE : ShieldTriggers.BLOCKED_MELEE,
                    source, attacker, incoming);

            if (!context.defaultEffectsSuppressed()) {

                double knockback = context.knockback();
                if (knockback > 0.0 && source instanceof LivingEntity target && target.isAlive()
                        && player.level() instanceof ServerLevel serverLevel) {
                    target.knockback(knockback, player.getX() - target.getX(), player.getZ() - target.getZ(),
                            serverLevel.damageSources().playerAttack(player), incoming);
                    pushParticles(serverLevel, player, target);
                }

                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            player.getX(), player.getY() + 1.0, player.getZ(), 14, 0.35, 0.35, 0.35, 0.12);
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 1.5F);
            }

            shield.onPerfectBlock(player, attacker, damage);
            return;
        }
        if (hits != null) {
            hits.put(source.getUUID(), new HitMark(now, false));
        }

        double projectileReduction = shield.getProjectileDamageReductionPercent();
        if (source instanceof Projectile && projectileReduction >= 0.0) {
            damage = (float) Math.min(incoming, incoming * projectileReduction / 100.0);
            event.setBlockedDamage(damage);
        }

        double currentStability = NorseShieldItem.getStability(useItem, shield.getMaxStability());

        if (currentStability <= damage) {

            NorseShieldItem.setStability(useItem, 0.0, shield.getMaxStability());
            triggerGuardBreak(player, shield, useItem);

            event.setBlockedDamage((float) Math.max(0.0, currentStability));
        } else {

            NorseShieldItem.setStability(useItem, currentStability - damage, shield.getMaxStability());
            ShieldTriggers.fireStabilityChange(player, useItem, shield, currentStability, currentStability - damage,
                    shield.getMaxStability());
            event.setShieldDamage(Math.max(1, (int) (damage / 4.0F)));
            shield.onBlockedAttack(player, attacker, damage);
            ShieldTriggers.fire(player, useItem, shield, ShieldTriggers.BLOCKED_HIT, source, attacker, damage);
            ShieldTriggers.fire(player, useItem, shield,
                    source instanceof Projectile ? ShieldTriggers.BLOCKED_PROJECTILE : ShieldTriggers.BLOCKED_MELEE,
                    source, attacker, damage);
            syncShieldToClient(player, useItem);

            int delayTicks = ShieldStats.RECHARGE_DELAY_TICKS.get();
            REGEN_DELAY_TICKS.put(uuid, delayTicks);
        }
    }

    public static void triggerGuardBreak(ServerPlayer player, ShieldBehavior shield) {
        ItemStack held = getHeldNorseShield(player);
        triggerGuardBreak(player, shield, held);
    }

    public static void triggerGuardBreak(ServerPlayer player, ShieldBehavior shield, ItemStack shieldStack) {

        player.stopUsingItem();

        int cdTicks = ShieldStats.GUARD_BREAK_COOLDOWN_TICKS.get();
        if (shieldStack != null && !shieldStack.isEmpty()) {
            player.getCooldowns().addCooldown(shieldStack, cdTicks);
            NorseShieldItem.setGuardBroken(shieldStack);
            syncShieldToClient(player, shieldStack);
        }

        ItemStack brokenStack = shieldStack != null ? shieldStack : ItemStack.EMPTY;
        ShieldTriggers.fire(player, brokenStack, shield, ShieldTriggers.GUARD_BREAK, null, null, 0.0F);

        ShieldEffects.stun(player, 16);

        BLOCKING_TICKS.remove(player.getUUID());
        RAISED_STACK.remove(player.getUUID());

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.2F, 0.85F);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    player.getX(), player.getY() + 1.1, player.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
        }

        player.sendOverlayMessage(
                Component.literal("[GUARD BROKEN!]").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
        );
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        BLOCKING_TICKS.remove(event.getEntity().getUUID());
        RAISED_STACK.remove(event.getEntity().getUUID());
        REGEN_DELAY_TICKS.remove(event.getEntity().getUUID());
        LAST_STABILITY_HIT.remove(event.getEntity().getUUID());
        INCOMING_AMOUNT.remove(event.getEntity().getUUID());
        LAST_PERFECT_GUARD.remove(event.getEntity().getUUID());
        UNBLOCKED_CANDIDATE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BLOCKING_TICKS.remove(player.getUUID());
            RAISED_STACK.remove(player.getUUID());
            REGEN_DELAY_TICKS.remove(player.getUUID());
            LAST_STABILITY_HIT.remove(player.getUUID());
            INCOMING_AMOUNT.remove(player.getUUID());
            LAST_PERFECT_GUARD.remove(player.getUUID());
            UNBLOCKED_CANDIDATE.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BLOCKING_TICKS.clear();
        RAISED_STACK.clear();
        REGEN_DELAY_TICKS.clear();
        LAST_STABILITY_HIT.clear();
        INCOMING_AMOUNT.clear();
        LAST_PERFECT_GUARD.clear();
        UNBLOCKED_CANDIDATE.clear();
        ShieldSkillRunner.clearAll();
    }
}
