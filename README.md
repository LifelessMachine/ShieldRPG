# ShieldRPG

**RPG shield combat overhaul featuring 54 unique shields with custom abilities.** Guard Stability, perfect guards, guard breaks and a skill shield.

## Features

- **Shield Damage Reduction**: Blocking no longer negates 100% of incoming damage; shields now mitigate damage by a set percentage.
- **Guard Stability.** Blocking drains the shield's stability. At zero the guard breaks: the shield goes on cooldown and you are stunned for a moment. Stability refills a few seconds after the last hit.
- **Perfect Guard.** Block within a short window after the shield comes up to take no damage, trigger shield ability or something.
- **54 shields.** come with ability and stat.
- **Stamina.** Holding a shield up costs stamina, shown on its own HUD bar. If another mod provides a resource, it takes over automatically.
- **Vanilla shield remake.** No Op vanilla shield.
- **Crafting.** Every shield is craftable from vanilla items, All recipe in crafting book.

## Configuration

| File                              | Contents                                                                                         |
| --------------------------------- | ------------------------------------------------------------------------------------------------ |
| `config/shieldrpg-startup.toml`   | `crafting.disabled_recipes`: shield recipes to switch off                                        |
| `config/shieldrpg-synced.toml`    | Guard break cooldown, recharge delay, perfect guard window, resource provider. Synced to clients |
| `config/shieldrpg/shields/*.json` | Add, change or replace any shield. Copy a file from `config/shieldrpg/defaults/` to start. Restart the game after editing. Keep identical on server and client |

## Compatibility

- Any item from any mod can be turned into a shield with a JSON file.
- Other mods can ship their own shields by including JSON files under `data/<namespace>/shieldrpg/shields/` in their jar. Shields are read once at game start, so world datapacks are not supported.

## For developers

The developer docs start at **[docs/API.md](docs/API.md)**:

- [Getting started](docs/Getting_Started_API.md)
- [Adding a shield with JSON](docs/Shield_JSON_API.md)
- [Shield definition reference](docs/Definition_Reference_API.md)
- [Triggers](docs/Triggers_API.md)
- [Skill actions and skill text](docs/Skill_Actions_API.md)
- [Providers and listeners](docs/Providers_API.md)
- [Java API](docs/Java_API.md)
- [Config](docs/Config_API.md)

## Changelog

### 1.0.0

- 54 shields with a skill each, and custom vanilla shield.
- Guard Stability, Perfect Guard, guard break and stamina for holding shield.
- Crafting recipes for every shield, with a config option to disable.
- Developer API: JSON shields, custom skill actions, custom triggers, resource/heal/stun providers.
- some bug to fix later.

## Reporting bugs

Open an issue with your Minecraft, ModLoaders and ShieldRPG versions, and attach the log or crash report. ( [mclo.gs](https://mclo.gs))

## License

[MIT](LICENSE) © 2026 FrostyFox
