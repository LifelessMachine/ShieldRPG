# ShieldRPG developer API

Docs for people building on ShieldRPG. Read them in order, or jump to what you need.

1. **[Getting started](Getting_Started_API.md)**: Dependency setup and what ShieldRPG is.
    - [Setup](Getting_Started_API.md#setup)
    - [Working example](Getting_Started_API.md#working-example)
2. **[Adding a shield with JSON](Shield_JSON_API.md)**: The files a shield needs, a full example, existing items, config overrides.
    - [The files](Shield_JSON_API.md#the-files)
    - [Turn an existing item into a shield](Shield_JSON_API.md#turn-an-existing-item-into-a-shield)
    - [Crafting and drops](Shield_JSON_API.md#crafting-and-drops)
    - [Override a shield from config](Shield_JSON_API.md#override-a-shield-from-config)
3. **[Shield definition reference](Definition_Reference_API.md)**: Every JSON field with its default and range.
    - [Top level](Definition_Reference_API.md#top-level)
    - [`guard`](Definition_Reference_API.md#guard)
    - [`perfect_guard`](Definition_Reference_API.md#perfect_guard)
    - [`stats`](Definition_Reference_API.md#stats)
    - [`skills[]`](Definition_Reference_API.md#skills)
4. **[Triggers](Triggers_API.md)**: The 17 built-in triggers and custom ones.
    - [Built-in triggers](Triggers_API.md#built-in-triggers)
    - [Custom triggers](Triggers_API.md#custom-triggers)
5. **[Skill actions and skill text](Skill_Actions_API.md)**: Built-in actions, writing your own, description text.
    - [Built-in actions](Skill_Actions_API.md#built-in-actions)
    - [Custom skill action](Skill_Actions_API.md#custom-skill-action)
    - [Skill text](Skill_Actions_API.md#skill-text)
6. **[Providers and listeners](Providers_API.md)**: Resource cost, heal, stun, perfect guard, cooldown display.
    - [Resource provider (what holding a shield costs)](Providers_API.md#resource-provider-what-holding-a-shield-costs)
    - [Heal and stun providers](Providers_API.md#heal-and-stun-providers)
    - [Perfect guard listener](Providers_API.md#perfect-guard-listener)
    - [Skill cooldown display](Providers_API.md#skill-cooldown-display)
7. **[Java API basics](Java_API.md)**: Registering things, shields built in code, lookups.
    - [Shields built in code](Java_API.md#shields-built-in-code)
    - [Lookups](Java_API.md#lookups)
8. **[Config](Config_API.md)**: All config keys.

## Quick paths

- Just want a new shield: [Adding a shield with JSON](Shield_JSON_API.md), then [Definition reference](Definition_Reference_API.md).
- Want a new skill effect: [Skill actions](Skill_Actions_API.md).
- Want a new trigger: [Triggers](Triggers_API.md#custom-triggers).
- Want shields to cost stamina or mana: [Providers](Providers_API.md#resource-provider-what-holding-a-shield-costs).
