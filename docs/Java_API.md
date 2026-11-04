# Java API basics

[Index](API.md) | Previous: [Providers and listeners](Providers_API.md) | Next: [Config](Config_API.md)

**On this page**

- [Shields built in code](#shields-built-in-code)
- [Lookups](#lookups)

---

All registration goes through `ShieldApi`. Do it in `FMLCommonSetupEvent` inside `enqueueWork`:

```java
private void commonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
        ShieldApi.registerSkillAction(new BlastAction());
        ShieldApi.registerTrigger("yourmod:combo");
        ShieldApi.registerTriggerListener(new ComboTracker(), ShieldApi.PRIORITY_DEFAULT);
    });
}
```

Every provider implements `ShieldProvider#id()`. An id that is already registered is rejected (the register method returns `false`). Where several providers exist, the highest priority wins. `PRIORITY_DEFAULT` is 100, `PRIORITY_BUILT_IN` is 0, `PRIORITY_FALLBACK` is -1000 (used by ShieldRPG's own stamina, so any other provider wins).

## Shields built in code

Items that aren't made from JSON can still be declared. `registerShield` tells the API about the item and its tooltip/guard numbers:

```java
ShieldApi.registerShield(MY_ITEM.get(), () -> ShieldDescriptor.vanillaLike(80, 20, 4, 70));
```

`ShieldDescriptor.of(...)` is the full version with tier and lore. Most mods should use JSON instead.

## Lookups

```java
ShieldDefinition def = ShieldApi.getShieldDefinition(stack.getItem());   // null if not a data shield
ShieldApi.isKnownTrigger("perfect_guard");
ShieldApi.getSkillAction("shieldrpg:damage");
```

`ShieldDefinition` is a plain record of everything in the JSON.

## Crafting and drops

```java
ShieldApi.setCraftingEnabled("shieldrpg:wooden_buckler", false);
ShieldApi.isCraftingEnabled("shieldrpg:wooden_buckler");
ShieldApi.clearCraftingOverride("shieldrpg:wooden_buckler");
ShieldApi.getShieldIds();
ShieldApi.getShieldItem("shieldrpg:wooden_buckler");
ShieldApi.createShieldStack("shieldrpg:wooden_buckler");
ShieldApi.dropShield(level, position, "shieldrpg:wooden_buckler");
ShieldApi.giveShield(player, "shieldrpg:wooden_buckler");
```

`dropShield` and `giveShield` do nothing on the client. `ShieldDefinition` and `ShieldDescriptor` are immutable: their `stats()`, `skills()` and `steps()` can't be changed after creation.

The recipe condition `shieldrpg:crafting_enabled` is described in [Adding a shield with JSON](Shield_JSON_API.md#crafting-and-drops).

---

[Index](API.md) | Previous: [Providers and listeners](Providers_API.md) | Next: [Config](Config_API.md)
