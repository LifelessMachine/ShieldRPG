# Adding a shield with JSON

[Index](API.md) | Previous: [Getting started](Getting_Started_API.md) | Next: [Shield definition reference](Definition_Reference_API.md)

**On this page**

- [The files](#the-files)
- [Turn an existing item into a shield](#turn-an-existing-item-into-a-shield)
- [Override a shield from config](#override-a-shield-from-config)

---

ShieldRPG scans every mod jar for `data/<namespace>/shieldrpg/shields/*.json`, registers one item per file, and makes it a shield. You don't register the item yourself.

## The files

A shield needs these files (`yourmod:ember_buckler` as the example):

| File | What |
|---|---|
| `data/yourmod/shieldrpg/shields/ember_buckler.json` | the definition |
| `assets/yourmod/items/ember_buckler.json` | item model selector (raised vs. lowered) |
| `assets/yourmod/models/item/ember_buckler.json` | model, shield not raised |
| `assets/yourmod/models/item/ember_buckler_blocking.json` | model, shield raised |
| `assets/yourmod/textures/item/ember_buckler.png` | texture |
| `assets/yourmod/lang/en_us.json` | name, lore, skill text |

Definition:

```json
{
  "item_id": "yourmod:ember_buckler",
  "name": "Ember Buckler",
  "name_gradient": ["#F5B041", "#FDEBD0"],
  "tier": "rare",
  "lore_key": "tooltip.yourmod.ember_buckler.lore.1",
  "durability": 336,
  "guard": {
    "max_stability": 60,
    "regen_per_second": 10,
    "hold_drain_per_second": 4,
    "damage_reduction_percent": 65,
    "projectile_damage_reduction_percent": 80,
    "raised_move_speed": -0.05
  },
  "perfect_guard": { "knockback": 0.8, "window_ticks": 10 },
  "stats": { "armor": 5, "move_speed": -0.02 },
  "skills": [
    {
      "trigger": "perfect_guard",
      "cooldown_ticks": 120,
      "steps": [
        { "action": "shieldrpg:damage", "target": "nearby", "radius": 5,
          "params": { "amount": 8 },
          "particle": { "type": "minecraft:flame", "count": 20, "spread": 0.5, "speed": 0.05 } },
        { "action": "shieldrpg:knockback", "target": "nearby", "radius": 5,
          "params": { "strength": 1.2 } }
      ],
      "sound": { "id": "minecraft:entity.blaze.shoot", "volume": 0.8, "pitch": 1.0 },
      "text_key": "tooltip.yourmod.ember_buckler.skill.flare"
    }
  ]
}
```

`items/ember_buckler.json`:

```json
{
  "model": {
    "type": "minecraft:condition",
    "property": "minecraft:using_item",
    "on_false": { "type": "minecraft:model", "model": "yourmod:item/ember_buckler" },
    "on_true":  { "type": "minecraft:model", "model": "yourmod:item/ember_buckler_blocking" }
  }
}
```

The two models only set a parent and the texture. The parents carry the hand and block display transforms:

```json
{ "parent": "shieldrpg:item/template_shield",          "textures": { "layer0": "yourmod:item/ember_buckler" } }
{ "parent": "shieldrpg:item/template_shield_blocking", "textures": { "layer0": "yourmod:item/ember_buckler" } }
```

Lang:

```json
{
  "item.yourmod.ember_buckler": "Ember Buckler",
  "tooltip.yourmod.ember_buckler.lore.1": "Still warm.",
  "tooltip.yourmod.ember_buckler.skill.flare": "Flare",
  "tooltip.yourmod.ember_buckler.skill.flare.desc": "A Perfect Guard burns nearby enemies for %2$s damage and knocks them back (every %1$s seconds)."
}
```

Tags and creative tabs are normal vanilla/NeoForge: add the item to a tab in `displayItems`, and to `c:tools/shields` if you want other mods to treat it as a shield. Crafting is covered in [Crafting and drops](#crafting-and-drops).

## Turn an existing item into a shield

Register the item yourself, then set `"existing_item": true` in the definition. ShieldRPG then skips registration and attaches the shield behaviour to that item. Works with any mod's item.

```json
{ "item_id": "yourmod:tavern_door", "existing_item": true, "name": "Tavern Door", "tier": "rare", ... }
```

## Crafting and drops

Recipes are ordinary recipe JSON in `data/yourmod/recipe/`. Add the ShieldRPG condition if you want server owners to be able to switch the recipe off:

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["PPP", "PSP", " P "],
  "key": { "P": "#minecraft:planks", "S": "minecraft:stick" },
  "result": { "id": "yourmod:ember_buckler", "count": 1 },
  "neoforge:conditions": [ { "type": "shieldrpg:crafting_enabled", "shield": "yourmod:ember_buckler" } ]
}
```

* Switch a recipe off: list the id in `crafting.disabled_recipes` in `config/shieldrpg-startup.toml`, or call `ShieldApi.setCraftingEnabled("yourmod:ember_buckler", false)` before the datapacks load (a `/reload` applies it later). `ShieldApi.isCraftingEnabled(id)` reads the result.
* Change a recipe: a datapack or a file in your mod with the same path replaces it (`data/shieldrpg/recipe/wooden_buckler.json`).
* The vanilla shield: `data/minecraft/recipe/shield.json` is shipped with the condition and is off by default. `shieldrpg:standard_shield` has the same recipe. To get the vanilla recipe back, remove `minecraft:shield` from `crafting.disabled_recipes`.

Giving or dropping shields from code:

```java
List<String> ids = ShieldApi.getShieldIds();
ItemStack stack = ShieldApi.createShieldStack("shieldrpg:wooden_buckler");
ShieldApi.dropShield(level, mob.position(), "shieldrpg:wooden_buckler");
ShieldApi.giveShield(player, "shieldrpg:wooden_buckler");
```

For loot tables just use the shield as a normal item entry.

## Override a shield from config

Put a definition with the same `item_id` in `config/shieldrpg/shields/` (the mod creates this folder on first launch). It replaces the one from the jar. A server owner can rebalance any shield this way. To start from an existing shield, copy its file from `config/shieldrpg/defaults/<namespace>/`. That folder holds the current definition of every shield found in the mod jars and is rewritten on every launch, so editing it does nothing. Restart the game after editing, and keep the files identical on the server and the client.

Every field of the definition is listed in [Definition_Reference_API.md](Definition_Reference_API.md); triggers are in [Triggers_API.md](Triggers_API.md).

---

[Index](API.md) | Previous: [Getting started](Getting_Started_API.md) | Next: [Shield definition reference](Definition_Reference_API.md)
