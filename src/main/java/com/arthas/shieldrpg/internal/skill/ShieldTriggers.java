package com.arthas.shieldrpg.internal.skill;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.arthas.shieldrpg.ShieldRPG;
import com.arthas.shieldrpg.api.ShieldApi;
import com.arthas.shieldrpg.api.ShieldSkillContext;
import com.arthas.shieldrpg.api.ShieldTriggerIds;
import com.arthas.shieldrpg.item.ShieldBehavior;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class ShieldTriggers {

    public static final String PERFECT_GUARD = ShieldTriggerIds.PERFECT_GUARD;
    public static final String BLOCKED_HIT = ShieldTriggerIds.BLOCKED_HIT;
    public static final String BLOCKED_MELEE = ShieldTriggerIds.BLOCKED_MELEE;
    public static final String BLOCKED_PROJECTILE = ShieldTriggerIds.BLOCKED_PROJECTILE;
    public static final String PROJECTILE_INCOMING = ShieldTriggerIds.PROJECTILE_INCOMING;
    public static final String HIT_UNBLOCKED = ShieldTriggerIds.HIT_UNBLOCKED;
    public static final String SHIELD_BASH = ShieldTriggerIds.SHIELD_BASH;
    public static final String STABILITY_LOW = ShieldTriggerIds.STABILITY_LOW;
    public static final String STABILITY_FULL = ShieldTriggerIds.STABILITY_FULL;
    public static final String GUARD_BREAK = ShieldTriggerIds.GUARD_BREAK;
    public static final String SHIELD_RAISED = ShieldTriggerIds.SHIELD_RAISED;
    public static final String SHIELD_LOWERED = ShieldTriggerIds.SHIELD_LOWERED;
    public static final String GUARD_RECOVERED = ShieldTriggerIds.GUARD_RECOVERED;
    public static final String RESOURCE_EXHAUSTED = ShieldTriggerIds.RESOURCE_EXHAUSTED;
    public static final String SHIELD_EQUIPPED = ShieldTriggerIds.SHIELD_EQUIPPED;
    public static final String SHIELD_UNEQUIPPED = ShieldTriggerIds.SHIELD_UNEQUIPPED;
    public static final String SHIELD_BROKEN_ITEM = ShieldTriggerIds.SHIELD_BROKEN_ITEM;

    public static final double DEFAULT_LOW_THRESHOLD = 0.25;

    private static boolean errorLogged;
    private static final int MAX_DEPTH = 4;
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private ShieldTriggers() {}

    public static void fire(ServerPlayer player, ItemStack stack, ShieldBehavior shield, String trigger,
                            @Nullable Entity directSource, @Nullable LivingEntity attacker, float amount) {
        fire(player, stack, shield, trigger, directSource, attacker, amount, Map.of(), true);
    }

    public static void fire(ServerPlayer player, ItemStack stack, ShieldBehavior shield, String trigger,
                            @Nullable Entity directSource, @Nullable LivingEntity attacker, float amount,
                            Map<String, Object> extras) {
        fire(player, stack, shield, trigger, directSource, attacker, amount, extras, true);
    }

    private static void fire(ServerPlayer player, ItemStack stack, ShieldBehavior shield, String trigger,
                             @Nullable Entity directSource, @Nullable LivingEntity attacker, float amount,
                             Map<String, Object> extras, boolean notifyListeners) {
        int[] depth = DEPTH.get();
        if (depth[0] >= MAX_DEPTH) {
            return;
        }
        depth[0]++;
        try {
            dispatch(player, stack, shield, trigger, directSource, attacker, amount, extras, notifyListeners);
        } finally {
            depth[0]--;
        }
    }

    private static void dispatch(ServerPlayer player, ItemStack stack, ShieldBehavior shield, String trigger,
                                 @Nullable Entity directSource, @Nullable LivingEntity attacker, float amount,
                                 Map<String, Object> extras, boolean notifyListeners) {
        if (notifyListeners) {
            ShieldSkillContext context = new ShieldSkillContext(trigger, player, stack, directSource, attacker, null, amount, extras);
            for (var registration : ShieldApi.getTriggerListeners()) {
                try {
                    registration.provider().onTrigger(context);
                } catch (Throwable t) {
                    if (!errorLogged) {
                        errorLogged = true;
                        ShieldRPG.LOGGER.error("A shield trigger listener failed; it is skipped for this event", t);
                    }
                }
            }
        }
        var definition = shield.getShieldDefinition();
        if (definition != null) {
            ShieldSkillRunner.run(player, stack, definition, trigger, directSource, attacker, amount, extras);
        }
    }

    public static void fireStabilityChange(ServerPlayer player, ItemStack stack, ShieldBehavior shield,
                                           double before, double after, double max) {
        if (max <= 0.0) {
            return;
        }
        double ratioBefore = before / max;
        double ratioAfter = after / max;
        Map<String, Object> extras = Map.of(
                "stability_before", before, "stability", after, "max_stability", max,
                "stability_ratio_before", ratioBefore, "stability_ratio", ratioAfter);
        if (ratioAfter < ratioBefore) {

            boolean crossedDefault = ratioBefore > DEFAULT_LOW_THRESHOLD && ratioAfter <= DEFAULT_LOW_THRESHOLD;
            fire(player, stack, shield, STABILITY_LOW, null, null, 0.0F, extras, crossedDefault);
        } else if (ratioAfter >= 0.999 && ratioBefore < 0.999) {
            fire(player, stack, shield, STABILITY_FULL, null, null, 0.0F, extras, true);
        }
    }
}
