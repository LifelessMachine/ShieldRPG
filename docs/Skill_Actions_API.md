# Skill actions and skill text

[Index](API.md) | Previous: [Triggers](Triggers_API.md) | Next: [Providers and listeners](Providers_API.md)

**On this page**

- [Built-in actions](#built-in-actions)
- [Custom skill action](#custom-skill-action)
- [Skill text](#skill-text)

---

## Built-in actions

| Action | Params | Does |
|---|---|---|
| `shieldrpg:damage` | `amount` (1), `ignore_invulnerability` (false) | hurts the target as the player. `ignore_invulnerability` clears the hit immunity frames first, so the damage lands right after a normal hit |
| `shieldrpg:apply_effect` | `effect` (`minecraft:slowness`), `duration_ticks` (60), `amplifier` (0) | gives a mob effect |
| `shieldrpg:heal` | `amount` (0), `percent_max_health` (0) | heals the target, flat plus percent. Goes through the heal provider if one is registered |
| `shieldrpg:knockback` | `strength` (0.5) | pushes the target away from the player |
| `shieldrpg:reflect_projectile` | none | sends the blocked projectile back at its shooter at the speed it arrived with. Use with `blocked_projectile` |

Extra actions that ship with ShieldRPG:

| Action | Params | Does |
|---|---|---|
| `shieldrpg:beam` | `amount` (4) | particle beam from the player to the target, hurts it |
| `shieldrpg:blast` | `radius` (4), `amount` (6), `strength` (1.5) | explosion effect around the player (blocks stay intact): damages and knocks back nearby hostile mobs (same rule as the `nearby` target) |
| `shieldrpg:ignite` | `seconds` (5) | sets the target on fire |
| `shieldrpg:pull` | `strength` (1.5) | yanks the target toward the player |
| `shieldrpg:cleanse` | none | removes every harmful effect from the target |

## Custom skill action

Implement `ShieldSkillAction`, then use its id as `action` in JSON. `execute` runs once per target.

```java
public final class BlastAction implements ShieldSkillAction {
    @Override public String id() { return "yourmod:blast"; }

    @Override
    public void execute(ShieldSkillContext ctx, ShieldSkillParams params) {
        ServerPlayer player = ctx.player();
        if (!(player.level() instanceof ServerLevel level)) return;
        double radius = params.getDouble("radius", 4.0);
        float amount = (float) params.getDouble("amount", 6.0);
        var source = level.damageSources().playerAttack(player);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                x -> x != player && !(x instanceof Player))) {
            e.hurtServer(level, source, amount);
        }
    }
}
```

```json
{ "trigger": "perfect_guard", "cooldown_ticks": 120,
  "action": "yourmod:blast", "target": "self", "params": { "radius": 4, "amount": 15 } }
```

`ShieldSkillContext` has `trigger()`, `player()`, `shield()`, `directSource()`, `attacker()`, `target()` (the entity this call is for), `amount()` (damage involved) and `extras()` with `extraDouble(key, fallback)` / `extraString(key, fallback)`. `ShieldSkillParams` has `getDouble`, `getInt`, `getString`, `getBoolean`, `has`.

## Skill text

`<text_key>.desc` is a lang string filled with numbers from the skill:

- `%1$s` cooldown in seconds
- then every numeric param of every step, in JSON order. `*_ticks` params are shown in seconds. Strings and booleans are skipped

For the `Ember Buckler` above: `%1$s` = 6, `%2$s` = 8 (damage), `%3$s` = 1.2 (strength). Use `%%` for a literal percent sign.

---

[Index](API.md) | Previous: [Triggers](Triggers_API.md) | Next: [Providers and listeners](Providers_API.md)
