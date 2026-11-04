package com.arthas.shieldrpg.api;

import java.util.Set;

public final class ShieldTriggerIds {

    private ShieldTriggerIds() {}

    public static final String PERFECT_GUARD = "perfect_guard";

    public static final String BLOCKED_HIT = "blocked_hit";

    public static final String BLOCKED_MELEE = "blocked_melee";

    public static final String BLOCKED_PROJECTILE = "blocked_projectile";

    public static final String PROJECTILE_INCOMING = "projectile_incoming";

    public static final String HIT_UNBLOCKED = "hit_unblocked";

    public static final String SHIELD_BASH = "shield_bash";

    public static final String STABILITY_LOW = "stability_low";

    public static final String STABILITY_FULL = "stability_full";

    public static final String GUARD_BREAK = "guard_break";

    public static final String GUARD_RECOVERED = "guard_recovered";

    public static final String SHIELD_RAISED = "shield_raised";

    public static final String SHIELD_LOWERED = "shield_lowered";

    public static final String RESOURCE_EXHAUSTED = "resource_exhausted";

    public static final String SHIELD_EQUIPPED = "shield_equipped";

    public static final String SHIELD_UNEQUIPPED = "shield_unequipped";

    public static final String SHIELD_BROKEN_ITEM = "shield_broken_item";

    public static final Set<String> ALL = Set.of(
            PERFECT_GUARD, BLOCKED_HIT, BLOCKED_MELEE, BLOCKED_PROJECTILE, PROJECTILE_INCOMING, HIT_UNBLOCKED, SHIELD_BASH,
            STABILITY_LOW, STABILITY_FULL, GUARD_BREAK, GUARD_RECOVERED, SHIELD_RAISED, SHIELD_LOWERED, RESOURCE_EXHAUSTED,
            SHIELD_EQUIPPED, SHIELD_UNEQUIPPED, SHIELD_BROKEN_ITEM);
}
