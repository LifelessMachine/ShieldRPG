# Shield definition reference

[Index](API.md) | Previous: [Adding a shield with JSON](Shield_JSON_API.md) | Next: [Triggers](Triggers_API.md)

**On this page**

- [Top level](#top-level)
- [`guard`](#guard)
- [`perfect_guard`](#perfect_guard)
- [`stats`](#stats)
- [`skills[]`](#skills)

---

Values outside the range are clamped.

## Top level

| Field | Default | Notes |
|---|---|---|
| `item_id` | required | `namespace:path` |
| `existing_item` | false | see [Shield_JSON_API.md](Shield_JSON_API.md#turn-an-existing-item-into-a-shield) |
| `name` | title-cased path | display name |
| `name_gradient` | white | `["#from", "#to"]` |
| `tier` | `common` | `common`, `rare`, `epic`, `legendary`. Sets tooltip colour and rarity |
| `lore_key` | `tooltip.<ns>.<path>.lore.1` | lang key |
| `durability` | 300 | 1 to 100000 |
| `show_skill_cooldown` | true | whether cooldown listeners are told about this shield's skill cooldowns |
| `guard`, `perfect_guard`, `stats`, `skills` | | below |

## `guard`

| Field | Default | Range | Notes |
|---|---|---|---|
| `max_stability` | 100 | 1 to 10000 | damage the guard absorbs before it breaks |
| `regen_per_second` | 20 | 0.1 to 1000 | |
| `hold_drain_per_second` | 4 | 0 to 1000 | resource cost of holding the shield up |
| `damage_reduction_percent` | 50 | 0 to 100 | |
| `projectile_damage_reduction_percent` | same as above | 0 to 100 | |
| `raised_move_speed` | 0 | -0.9 to 0.5 | movement speed change while raised. `-0.2` is 20% slower |

## `perfect_guard`

| Field | Default | Range | Notes |
|---|---|---|---|
| `knockback` | 0 | 0 to 5 | push on the attacker |
| `window_ticks` | global config | 0 to 40 | a hit within this many ticks after the shield starts blocking is a Perfect Guard. A raised shield starts blocking after 0.25 s (5 ticks), so the full time from raising is `window_ticks + 5` |

## `stats`

Vanilla attribute modifiers, active while the shield is in the off-hand: `armor`, `max_health`, `knockback_resist` (all added), `move_speed` (fraction of base speed, `0.05` is +5%). Other stats belong to the mod that owns them. A bridge mod can take over item stats with `ShieldApi.declareExternalItemStats`.

## `skills[]`

| Field | Default | Notes |
|---|---|---|
| `trigger` | required | a [trigger](Triggers_API.md) id |
| `cooldown_ticks` | 0 | per player, per shield, per skill. Shared by all steps |
| `threshold` | 0.25 | `stability_low` only: fraction of stability |
| `steps` | | list of steps. Or write one step's fields directly in the skill |
| `sound` | none | `{ "id", "volume" (0-10, 1), "pitch" (0.1-2, 1) }`, played at the player |
| `text_key` | none | lang key of the skill name. `<key>.desc` is the description |

A step:

| Field | Default | Notes |
|---|---|---|
| `action` | required | action id |
| `target` | `attacker` | `attacker`: the direct attacker, or whoever is responsible (an archer). `attacker_melee`: only a direct melee attacker. `self`. `nearby`: hostile mobs within `radius` (monsters, and any mob currently targeting you). Players, pets, villagers and armor stands are never hit |
| `radius` | 4 | 0.5 to 32, for `nearby` |
| `max_range` | 0 | 0 to 512. Skip targets farther than this from the player. 0 means no limit |
| `params` | | passed to the action |
| `particle` | none | `{ "type", "count" (1-200), "spread" (0-5), "speed" (0-5) }`. Simple particle types only. Spawned at each target |

All steps of a skill run together. If one step has no valid target, the others still run.

---

[Index](API.md) | Previous: [Adding a shield with JSON](Shield_JSON_API.md) | Next: [Triggers](Triggers_API.md)
