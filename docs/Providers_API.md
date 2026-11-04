# Providers and listeners

[Index](API.md) | Previous: [Skill actions and skill text](Skill_Actions_API.md) | Next: [Java API basics](Java_API.md)

**On this page**

- [Resource provider (what holding a shield costs)](#resource-provider-what-holding-a-shield-costs)
- [Heal and stun providers](#heal-and-stun-providers)
- [Perfect guard listener](#perfect-guard-listener)
- [Skill cooldown display](#skill-cooldown-display)

---

Hooks for mods that change how shields cost, heal, stun or show things. Register them as described in [Java API](Java_API.md).

## Resource provider (what holding a shield costs)

ShieldRPG has its own stamina bar, registered at `PRIORITY_FALLBACK` (-1000), below every other priority. It is used only when no other mod registers a provider, and its HUD hides itself as soon as another provider takes over. A `ShieldResourceProvider` makes holding a shield cost something else: stamina, mana, hunger, whatever you have.

```java
public final class StaminaProvider implements ShieldResourceProvider {
    @Override public String id() { return "yourmod:stamina"; }

    @Override
    public boolean canRaiseGuard(Player player, ShieldGuardContext ctx) {
        return Stamina.get(player) >= ctx.holdDrainPerSecond();
    }

    @Override
    public DrainResult drain(ServerPlayer player, double cost, ShieldGuardContext ctx) {
        double value = Stamina.get(player);
        if (value < cost) { Stamina.set(player, 0); return DrainResult.DEPLETED; }
        Stamina.set(player, value - cost);
        return DrainResult.OK;
    }

    @Override
    public Component depletedMessage() {
        return Component.literal("[OUT OF STAMINA]").withStyle(ChatFormatting.YELLOW);
    }
}

ShieldApi.registerResourceProvider(new StaminaProvider(), ShieldApi.PRIORITY_DEFAULT);
```

`canRaiseGuard` gates raising the shield. `drain` is called every half second while it is held, with half of `hold_drain_per_second` as `cost`. Return `DEPLETED` and the shield drops (this is not a guard break), the player gets a short cooldown and `depletedMessage()` is shown. `ShieldGuardContext` carries the shield stack and `holdDrainPerSecond`. Creative players are never drained.

The provider that gets used is the one named in config `resource_provider`, or the highest priority one when it is `auto`.

## Heal and stun providers

`ShieldHealProvider.heal(caster, target, amount)` is called by `shieldrpg:heal`. Register one if your mod changes how healing works (healing power, for instance). Without one, `LivingEntity.heal` is used.

`ShieldStunProvider.applyStun(player, ticks)` is called when a guard breaks (16 ticks). Without one the player gets Slowness VII.

```java
ShieldApi.registerHealProvider(new MyHeal(), ShieldApi.PRIORITY_DEFAULT);
ShieldApi.registerStunProvider(new MyStun(), ShieldApi.PRIORITY_DEFAULT);
```

## Perfect guard listener

Runs before the built-in perfect guard effects. You can change the knockback or suppress the defaults.

```java
ShieldApi.registerPerfectGuardListener(new ShieldPerfectGuardListener() {
    @Override public String id() { return "yourmod:pg"; }

    @Override
    public void onPerfectGuard(PerfectGuardContext ctx) {
        ctx.setKnockback(ctx.knockback() * 2);
        // ctx.suppressDefaultEffects();   // no default push and particles
    }
}, ShieldApi.PRIORITY_DEFAULT);
```

`PerfectGuardContext`: `player()`, `shield()`, `damageSource()`, `directSource()`, `attacker()`, `blockedDamage()`, `knockback()`.

## Skill cooldown display

The core enforces `cooldown_ticks` and draws a clockwise cooldown overlay on every shield item that has a skill cooldown (the shield can still be raised meanwhile). To show something more (HUD, sound, an external system), listen for it:

```java
ShieldApi.registerSkillCooldownListener(new ShieldSkillCooldownListener() {
    @Override public String id() { return "yourmod:cooldown_display"; }

    @Override
    public void onSkillCooldownStarted(ServerPlayer player, ItemStack shield, int skillIndex,
                                       ShieldDefinition.Skill skill, int ticks) {
        // send a packet, write to the stack's custom data, ...
    }
});
```

It is called on the server each time a skill with a cooldown fires. Turn it off for a shield with `"show_skill_cooldown": false` in JSON or `ShieldApi.setSkillCooldownDisplay(item, false)`.

---

[Index](API.md) | Previous: [Skill actions and skill text](Skill_Actions_API.md) | Next: [Java API basics](Java_API.md)
