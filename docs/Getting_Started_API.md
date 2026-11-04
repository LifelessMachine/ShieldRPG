# ShieldRPG API: getting started

[Index](API.md) | Next: [Adding a shield with JSON](Shield_JSON_API.md)

**On this page**

- [Setup](#setup)
- [Working example](#working-example)

---

ShieldRPG is a shield combat engine: guard stability, perfect guards, skills. Other mods add shields and hook into it. Most things need only JSON; Java is for custom skill actions, triggers and resource costs.

Minecraft 26.3, NeoForge 26.3.0.98, Java 25. API package: `com.arthas.shieldrpg.api`.

## Setup

Build ShieldRPG with `./gradlew jar`. Next to the mod jar you get `shieldrpg-api-<version>.jar`, which holds only the `api` package. Use it as a compile-only dependency:

```groovy
dependencies {
    compileOnly files('libs/shieldrpg-api-1.0.0.jar')
}
```

For testing, put the full `shieldrpg-<version>.jar` in your run folder's `mods`.

`neoforge.mods.toml`:

```toml
[[dependencies.yourmod]]
    modId="shieldrpg"
    type="required"
    versionRange="[1.0.0,)"
    ordering="AFTER"
    side="BOTH"
```

A JSON-only shield pack needs no Java at all, just a mod with data and assets.

## Working example

ShieldRPG itself is the example. Its shields are plain JSON under `data/shieldrpg/shieldrpg/shields/`. Its extra skill actions (`shieldrpg:beam`, `blast`, `ignite`, `pull`, `cleanse`), the `shieldrpg:perfect_combo` trigger, the stamina provider and the cooldown overlay all use the public API described in these pages.

---

[Index](API.md) | Next: [Adding a shield with JSON](Shield_JSON_API.md)
