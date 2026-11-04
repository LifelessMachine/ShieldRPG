# Config

[Index](API.md) | Previous: [Java API basics](Java_API.md)

---

Two files. `config/shieldrpg-startup.toml` is read at game start, before any recipe loads. The `mechanics` values live in `config/shieldrpg-synced.toml` (a copy in a world's `syncedconfig/` folder overrides it for that world) and are synced to clients.

| Key | Default | Range |
|---|---|---|
| `crafting.disabled_recipes` (startup file) | `["minecraft:shield"]` | list of item ids |
| `mechanics.guard_break_cooldown_ticks` | 200 | 20 to 1200 |
| `mechanics.recharge_delay_ticks` | 80 | 0 to 600 |
| `mechanics.exhaustion_cooldown_ticks` | 30 | 0 to 200 |
| `mechanics.perfect_block_ticks` | 10 | 0 to 40, counted from the moment the shield starts blocking |
| `mechanics.perfect_guard_cooldown_ticks` | 0 | 0 to 200 |
| `mechanics.repeat_hit_window_ticks` | 10 | 0 to 100 |
| `mechanics.resource_provider` | `auto` | `auto` or a provider id (ShieldRPG's own is `shieldrpg:stamina`) |

`crafting.disabled_recipes` switches shield recipes off. It applies from the first world load; after editing the file, `/reload` applies the change. The vanilla shield recipe is off by default; `shieldrpg:standard_shield` has the same recipe and replaces it.

The vanilla shield (`minecraft:shield`) is a normal ShieldRPG shield: its numbers, armor and skill come from `shieldrpg/shields/vanilla_shield.json` and can be overridden from `config/shieldrpg/shields/` like any other.

---

[Index](API.md) | Previous: [Java API basics](Java_API.md)
