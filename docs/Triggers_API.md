# Triggers

[Index](API.md) | Previous: [Shield definition reference](Definition_Reference_API.md) | Next: [Skill actions and skill text](Skill_Actions_API.md)

**On this page**

- [Built-in triggers](#built-in-triggers)
- [Custom triggers](#custom-triggers)

---

## Built-in triggers

| Id | Fires when |
|---|---|
| `perfect_guard` | you block inside the perfect guard window |
| `blocked_hit` | any blocked hit, perfect guards included |
| `blocked_melee` | a blocked melee hit |
| `blocked_projectile` | a blocked projectile |
| `projectile_incoming` | a projectile is about to hit you while the shield is up |
| `hit_unblocked` | the shield is up but an attack it could have blocked gets through: from outside the blocking angle, in the 0.25 s before the shield starts blocking, or a piercing arrow. Fire, poison, falling and other damage that shields never block don't count |
| `shield_bash` | a fully charged attack with the shield in either hand |
| `stability_low` | stability drops to or below the skill's `threshold` (default 0.25). Trigger listeners get it once, when stability crosses 25% |
| `stability_full` | stability is back to full |
| `guard_break` | the guard breaks |
| `guard_recovered` | the guard recovers after breaking |
| `shield_raised` | you raise the shield (right away, before the 0.25 s block delay) |
| `shield_lowered` | you lower the shield, or it drops because the resource ran out |
| `resource_exhausted` | the guard resource ran out and the shield dropped |
| `shield_equipped` | the shield moves into an equipment slot (`slot` is in the context extras) |
| `shield_unequipped` | the shield leaves an equipment slot |
| `shield_broken_item` | the item breaks (durability 0) |

## Custom triggers

ShieldRPG itself declares one: `shieldrpg:perfect_combo`, fired when the player lands two perfect guards within 10 seconds.

Declare the id, then fire it from your own code. A trigger fired with `fireTrigger` runs the matching skills of the shield in `shield`, so a JSON skill can use it like any built-in one.

```java
ShieldApi.registerTrigger("yourmod:combo");

ShieldApi.fireTrigger(player, shieldStack, "yourmod:combo",
        directSource, attacker, amount, Map.of("combo", 2));
```

To build triggers out of built-in ones, listen to all triggers with `ShieldTriggerListener`. Example, "two perfect guards within 10 seconds":

```java
public final class ComboTracker implements ShieldTriggerListener {
    private final Map<UUID, long[]> state = new ConcurrentHashMap<>(); // {count, lastTick}

    @Override public String id() { return "yourmod:combo_tracker"; }

    @Override
    public void onTrigger(ShieldSkillContext ctx) {
        if (!ShieldTriggerIds.PERFECT_GUARD.equals(ctx.trigger())) return;
        long now = ctx.player().level().getGameTime();
        long[] s = state.get(ctx.player().getUUID());
        long count = s != null && now - s[1] <= 200 ? s[0] + 1 : 1;
        if (count >= 2) {
            state.remove(ctx.player().getUUID());
            ShieldApi.fireTrigger(ctx.player(), ctx.shield(), "yourmod:combo",
                    ctx.directSource(), ctx.attacker(), ctx.amount(), Map.of());
        } else {
            state.put(ctx.player().getUUID(), new long[] { count, now });
        }
    }
}
```

`ShieldTriggerIds` has constants for all built-in ids.

The hit that breaks the guard fires `guard_break` only: no `blocked_*` and no `stability_low`. A guard break doesn't fire `shield_lowered` either.

Triggers fired from inside other triggers (a skill that deals damage which sets off another shield's skill, a listener that calls `fireTrigger`) are capped at 4 levels deep, so two shields can't bounce damage back and forth forever.

---

[Index](API.md) | Previous: [Shield definition reference](Definition_Reference_API.md) | Next: [Skill actions and skill text](Skill_Actions_API.md)
