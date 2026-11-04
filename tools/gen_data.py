#!/usr/bin/env python3
"""Generates the JSON, lang, item definitions, recipes and models of the built-in shields. Run from the project root:
python3 tools/gen_data.py  (textures: python3 tools/gen_textures.py)"""
import json, os

GRAD = {"epic": ["#9B59B6", "#D7BDE2"], "legendary": ["#F5B041", "#FDEBD0"]}


def fx(t, count=8, spread=0.3, speed=0.05):
    return {"type": "minecraft:" + t, "count": count, "spread": spread, "speed": speed}


# Short skill descriptions (no knockback strength, radius or range: just what the skill does). Argument order: %1$s = cooldown seconds,
# then every numeric step param in JSON order (`*_ticks` shown as seconds; an effect's amplifier counts as a number).
DESC = {
    "rim_strike": "A fully charged attack with this shield hits the target for %2$s extra damage (every %1$s seconds).",
    "quick_step": "When a projectile is about to hit you, you gain speed for %2$s seconds (every %1$s seconds).",
    "thorns": "Melee attackers you block take %2$s damage (every %1$s seconds).",
    "chilling_return": "Blocking a projectile slows its shooter for %2$s seconds (every %1$s seconds).",
    "thunderclap": "A Perfect Guard strikes nearby enemies for %2$s damage, knocks them back and weakens them for %4$s seconds (every %1$s seconds).",
    "second_wind": "When the guard is nearly broken you heal %2$s%% of your max health and gain Absorption for %3$s seconds (every %1$s seconds).",
    "crushing_bash": "A fully charged attack with this shield deals %2$s extra damage and knocks the target back (every %1$s seconds).",
    "stand_fast": "When the guard recovers you gain Resistance for %2$s seconds.",
    "beacon_beam": "Two Perfect Guards in a row fire a beam at your attacker for %2$s damage (every %1$s seconds).",
    "steady_resolve": "Blocking a hit grants Regeneration for %2$s seconds (every %1$s seconds).",
    "shake_it_off": "A hit that gets past your shield's blocking angle grants you Absorption for %2$s seconds (every %1$s seconds).",
    "battle_stance": "Raising the shield grants Strength for %2$s seconds (every %1$s seconds).",
    "quick_recovery": "Lowering the shield grants Haste for %2$s seconds (every %1$s seconds).",
    "ready_to_march": "Taking up the shield grants Speed for %2$s seconds (every %1$s seconds).",
    "light_step": "Putting the shield away grants Jump Boost for %2$s seconds (every %1$s seconds).",
    "last_reserve": "When your stamina runs out you gain strong Absorption for %2$s seconds (every %1$s seconds).",
    "full_charge": "When the guard is fully restored you heal %2$s%% of your max health (every %1$s seconds).",
    "rebirth": "When the shield breaks you heal %2$s%% of your max health, gain Regeneration for %3$s seconds and knock nearby enemies back.",
    "last_stand": "When the guard breaks, nearby enemies are knocked back and you gain Absorption for %3$s seconds (every %1$s seconds).",
    "sss_boom": "A Perfect Guard makes the shield explode: nearby enemies take %3$s damage and are knocked back, blocks stay intact (every %1$s seconds).",
    "boing": "Melee attackers you block are bounced away (every %1$s seconds).",
    "prickly": "Melee attackers you block take %2$s damage (every %1$s seconds).",
    "withering_gaze": "Melee attackers you block wither for %2$s seconds (every %1$s seconds).",
    "fright_night": "When the guard breaks, nearby enemies are blinded for %2$s seconds and weakened (every %1$s seconds).",
    "piston_slam": "A fully charged attack with this shield launches the target back and into the air (every %1$s seconds).",
    "clang": "A Perfect Guard rings the pan against the attacker for %2$s damage and weakens it for %3$s seconds (every %1$s seconds).",
}


def boost_fx(step):
    """Makes the particle burst of a step bigger, and gives steps without one a default so every skill is easy to see."""
    p = step.get("particle")
    if p is None:
        action, target = step["action"], step.get("target", "self")
        if action == "shieldrpg:knockback":
            step["particle"] = {"type": "minecraft:cloud", "count": 18, "spread": 0.5, "speed": 0.12}
        elif action == "shieldrpg:apply_effect":
            good = target == "self"
            step["particle"] = {"type": "minecraft:happy_villager" if good else "minecraft:smoke", "count": 16, "spread": 0.5, "speed": 0.05}
        elif action == "shieldrpg:damage":
            step["particle"] = {"type": "minecraft:crit", "count": 18, "spread": 0.5, "speed": 0.2}
        elif action == "shieldrpg:heal":
            step["particle"] = {"type": "minecraft:heart", "count": 10, "spread": 0.5, "speed": 0.05}
        return
    if p["type"] == "minecraft:sweep_attack":
        p.update(count=3, spread=0.8)
    else:
        p["count"] = min(40, max(14, p.get("count", 1) * 3))
        p["spread"] = round(max(0.5, p.get("spread", 0.0) * 1.5), 2)


# id, name, tier, guard, perfect_guard, stats, skills(list of dict), lang(name-lore), skill_lang{key:(name,desc)}
SHIELDS = [
    dict(id="wooden_buckler", name="Wooden Buckler", tier="common",
         guard=dict(max_stability=80, regen_per_second=22, hold_drain_per_second=3, damage_reduction_percent=40),
         stats=dict(armor=2),
         skills=[dict(trigger="perfect_guard", cooldown_ticks=100, action="shieldrpg:apply_effect", target="attacker_melee",
                      params=dict(effect="minecraft:glowing", duration_ticks=160, amplifier=0), particle=fx("end_rod", 8, 0.3, 0.05),
                      text="tell_tale", name="Tell-Tale",
                      desc="A Perfect Guard marks the attacker so it glows for %2$s seconds (every %1$s seconds).")],
         lore="A small round shield of seasoned oak. Light and quick, but it will not stop much."),
    dict(id="iron_heater_shield", name="Iron Heater Shield", tier="common",
         guard=dict(max_stability=120, regen_per_second=20, hold_drain_per_second=4, damage_reduction_percent=50,
                    projectile_damage_reduction_percent=70),
         stats=dict(armor=5),
         skills=[dict(trigger="shield_bash", cooldown_ticks=60, action="shieldrpg:knockback", target="attacker",
                      params=dict(strength=2.2), particle=fx("poof", 8, 0.3, 0.1), text="iron_shove", name="Iron Shove",
                      desc="A fully charged attack with this shield shoves the target back (every %1$s seconds).")],
         lore="A classic iron-bound shield. It turns arrows better than it turns swords."),
    dict(id="copper_roundel", name="Copper Roundel", tier="common",
         guard=dict(max_stability=100, regen_per_second=20, hold_drain_per_second=4, damage_reduction_percent=50),
         stats=dict(armor=4),
         skills=[dict(trigger="shield_bash", cooldown_ticks=20, action="shieldrpg:damage", target="attacker",
                      params=dict(amount=2, ignore_invulnerability=True), particle=fx("crit", 6, 0.3, 0.1), text="rim_strike",
                      name="Rim Strike", desc="When you land a fully charged attack with this shield in either hand, its rim hits the target for %2$s extra damage (every %1$s seconds).")],
         lore="Beaten copper with a sharpened rim."),
    dict(id="skirmisher_pelta", name="Skirmisher Pelta", tier="rare",
         guard=dict(max_stability=90, regen_per_second=24, hold_drain_per_second=3, damage_reduction_percent=45),
         stats=dict(armor=4, move_speed=0.05),
         skills=[dict(trigger="projectile_incoming", cooldown_ticks=200, action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:speed", duration_ticks=60, amplifier=0), particle=fx("cloud", 6, 0.3, 0.05),
                      text="quick_step", name="Quick Step",
                      desc="When a projectile is about to hit you, you gain speed for %2$s seconds (every %1$s seconds).")],
         lore="A crescent shield carried by light skirmishers who never stand still."),
    dict(id="thornback_scutum", name="Thornback Scutum", tier="rare",
         guard=dict(max_stability=160, regen_per_second=18, hold_drain_per_second=5, damage_reduction_percent=60),
         stats=dict(armor=7),
         skills=[dict(trigger="blocked_melee", cooldown_ticks=20, action="shieldrpg:damage", target="attacker_melee", max_range=5,
                      params=dict(amount=3), particle=fx("damage_indicator", 3, 0.2, 0.1), text="thorns", name="Thorns",
                      desc="Melee attackers you block take %2$s damage (every %1$s seconds).")],
         lore="Studded with iron spikes. Hitting it hurts."),
    dict(id="frostguard_pavise", name="Frostguard Pavise", tier="rare",
         guard=dict(max_stability=180, regen_per_second=16, hold_drain_per_second=5, damage_reduction_percent=55,
                    projectile_damage_reduction_percent=100),
         stats=dict(armor=8, knockback_resist=0.2, move_speed=-0.05),
         skills=[dict(trigger="blocked_projectile", cooldown_ticks=100, action="shieldrpg:apply_effect", target="attacker", max_range=30,
                      params=dict(effect="minecraft:slowness", duration_ticks=60, amplifier=1), particle=fx("snowflake", 10, 0.4, 0.05),
                      text="chilling_return", name="Chilling Return",
                      desc="The shooter of a projectile you block is slowed for %2$s seconds, however far away (every %1$s seconds).")],
         lore="A tall archer's wall rimed with frost that never melts."),
    dict(id="stormcaller_aspis", name="Stormcaller Aspis", tier="epic",
         guard=dict(max_stability=200, regen_per_second=18, hold_drain_per_second=5, damage_reduction_percent=60),
         perfect_guard=dict(knockback=0.6),
         stats=dict(armor=9),
         skills=[dict(trigger="perfect_guard", cooldown_ticks=100, sound=dict(id="minecraft:entity.generic.explode", volume=0.5, pitch=1.5),
                      steps=[dict(action="shieldrpg:damage", target="nearby", radius=5, params=dict(amount=12), particle=fx("electric_spark", 8, 0.3, 0.1)),
                             dict(action="shieldrpg:knockback", target="nearby", radius=5, params=dict(strength=1.5), particle=fx("cloud", 8, 0.3, 0.1)),
                             dict(action="shieldrpg:apply_effect", target="nearby", radius=5,
                                  params=dict(effect="minecraft:weakness", duration_ticks=100, amplifier=0))],
                      text="thunderclap", name="Thunderclap",
                      desc="A Perfect Guard strikes every enemy within 5 blocks with lightning for %2$s damage, throws them back with force %3$s and weakens them for %4$s seconds (every %1$s seconds).")],
         lore="Bronze etched with a storm. It answers a perfect parry with thunder."),
    dict(id="sunforged_bulwark", name="Sunforged Bulwark", tier="epic",
         guard=dict(max_stability=240, regen_per_second=16, hold_drain_per_second=6, damage_reduction_percent=65),
         stats=dict(armor=12, max_health=4, move_speed=-0.08),
         skills=[dict(trigger="stability_low", threshold=0.3, cooldown_ticks=1200, sound=dict(id="minecraft:block.beacon.activate", volume=0.8, pitch=1.4),
                      steps=[dict(action="shieldrpg:heal", target="self", params=dict(percent_max_health=20), particle=fx("happy_villager", 12, 0.4, 0.1)),
                             dict(action="shieldrpg:apply_effect", target="self",
                                  params=dict(effect="minecraft:absorption", duration_ticks=200, amplifier=0), particle=fx("end_rod", 8, 0.3, 0.05))],
                      text="second_wind", name="Second Wind",
                      desc="When the guard falls to 30%% of its stability you heal %2$s%% of your max health and gain Absorption for %3$s seconds (once every %1$s seconds).")],
         lore="Gold that holds the heat of the sun, and gives it back when you are about to fall."),
    dict(id="wardens_greatshield", name="Warden's Greatshield", tier="legendary",
         guard=dict(max_stability=320, regen_per_second=14, hold_drain_per_second=7, damage_reduction_percent=75,
                    projectile_damage_reduction_percent=85),
         perfect_guard=dict(knockback=1.0),
         stats=dict(armor=14, max_health=8, knockback_resist=0.4, move_speed=-0.1),
         skills=[dict(trigger="shield_bash", cooldown_ticks=60,
                      steps=[dict(action="shieldrpg:damage", target="attacker", params=dict(amount=6, ignore_invulnerability=True), particle=fx("sweep_attack", 1, 0.0, 0.0)),
                             dict(action="shieldrpg:knockback", target="attacker", params=dict(strength=1.5))],
                      text="crushing_bash", name="Crushing Bash",
                      desc="When you land a fully charged attack with this shield in either hand, you smash the target for %2$s extra damage and knock it back (every %1$s seconds)."),
                 dict(trigger="guard_recovered", action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:resistance", duration_ticks=100, amplifier=0), particle=fx("happy_villager", 8, 0.4, 0.1),
                      text="stand_fast", name="Stand Fast",
                      desc="When the guard recovers after breaking you gain Resistance for %2$s seconds.")],
         lore="Forged for the wardens who held the pass. It has never been carried by anyone who ran."),
    dict(id="beacon_aegis", name="Beacon Aegis", tier="legendary",
         guard=dict(max_stability=260, regen_per_second=18, hold_drain_per_second=6, damage_reduction_percent=65),
         stats=dict(armor=10, max_health=10),
         skills=[dict(trigger="shieldrpg:perfect_combo", cooldown_ticks=100, action="shieldrpg:beam", target="attacker", max_range=24,
                      sound=dict(id="minecraft:block.beacon.power_select", volume=0.9, pitch=1.6),
                      params=dict(amount=20), text="beacon_beam", name="Beacon Beam",
                      desc="Three Perfect Guards within 10 seconds fire a beam from the shield that hits your attacker for %2$s damage from up to 24 blocks away (every %1$s seconds).")],
         lore="A shield that stores light. Two perfect guards in a row let it out."),

    dict(id="vanguard_kite", name="Vanguard Kite Shield", tier="rare",
         guard=dict(max_stability=130, regen_per_second=20, hold_drain_per_second=4, damage_reduction_percent=55),
         stats=dict(armor=6),
         skills=[dict(trigger="blocked_hit", cooldown_ticks=160, action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:regeneration", duration_ticks=60, amplifier=0), particle=fx("heart", 4, 0.3, 0.05),
                      text="steady_resolve", name="Steady Resolve",
                      desc="Blocking a hit grants Regeneration for %2$s seconds (every %1$s seconds).")],
         lore="Long and pointed, built to be planted in the ground while the line holds."),
    dict(id="ironhide_targe", name="Ironhide Targe", tier="common",
         guard=dict(max_stability=110, regen_per_second=20, hold_drain_per_second=4, damage_reduction_percent=50),
         stats=dict(armor=5),
         skills=[dict(trigger="hit_unblocked", cooldown_ticks=300, action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:absorption", duration_ticks=100, amplifier=0), particle=fx("crit", 6, 0.3, 0.05),
                      text="shake_it_off", name="Shake It Off",
                      desc="While your shield is raised, a hit that comes from outside its blocking angle (for example from behind) is not blocked, but grants you Absorption for %2$s seconds (every %1$s seconds).")],
         lore="Tanned thick. The blows that get past it feel like less."),
    dict(id="duelists_parma", name="Duelist's Parma", tier="rare",
         guard=dict(max_stability=95, regen_per_second=24, hold_drain_per_second=3, damage_reduction_percent=45),
         stats=dict(armor=4, move_speed=0.03),
         skills=[dict(trigger="shield_raised", cooldown_ticks=200, action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:strength", duration_ticks=60, amplifier=0), particle=fx("crit", 5, 0.3, 0.05),
                      text="battle_stance", name="Battle Stance",
                      desc="Raising the shield grants Strength for %2$s seconds (every %1$s seconds).")],
         lore="Raise it to commit. A duelist's shield is half of the dance."),
    dict(id="duelists_buckler", name="Duelist's Buckler", tier="rare",
         guard=dict(max_stability=90, regen_per_second=24, hold_drain_per_second=3, damage_reduction_percent=45),
         stats=dict(armor=4, move_speed=0.03),
         skills=[dict(trigger="shield_lowered", cooldown_ticks=200, action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:haste", duration_ticks=60, amplifier=0), particle=fx("cloud", 4, 0.3, 0.05),
                      text="quick_recovery", name="Quick Recovery",
                      desc="Lowering the shield grants Haste for %2$s seconds (every %1$s seconds).")],
         lore="Drop it to finish. The other half of the dance."),
    dict(id="wayfarers_roundshield", name="Wayfarer's Roundshield", tier="rare",
         guard=dict(max_stability=100, regen_per_second=22, hold_drain_per_second=4, damage_reduction_percent=48),
         stats=dict(armor=4, move_speed=0.02),
         skills=[dict(trigger="shield_equipped", cooldown_ticks=600, action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:speed", duration_ticks=200, amplifier=0), particle=fx("cloud", 6, 0.3, 0.05),
                      text="ready_to_march", name="Ready to March",
                      desc="Taking up the shield grants Speed for %2$s seconds (every %1$s seconds).")],
         lore="Swung to the arm when the road turns bad."),
    dict(id="pilgrims_roundshield", name="Pilgrim's Roundshield", tier="rare",
         guard=dict(max_stability=100, regen_per_second=22, hold_drain_per_second=4, damage_reduction_percent=48),
         stats=dict(armor=4, move_speed=0.02),
         skills=[dict(trigger="shield_unequipped", cooldown_ticks=600, action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:jump_boost", duration_ticks=100, amplifier=0), particle=fx("cloud", 4, 0.3, 0.05),
                      text="light_step", name="Light Step",
                      desc="Putting the shield away grants Jump Boost for %2$s seconds (every %1$s seconds).")],
         lore="Slung on the back for the long road."),
    dict(id="reservoir_bulwark", name="Reservoir Bulwark", tier="epic",
         guard=dict(max_stability=220, regen_per_second=19, hold_drain_per_second=6, damage_reduction_percent=62),
         stats=dict(armor=9, max_health=2),
         skills=[dict(trigger="resource_exhausted", cooldown_ticks=600, action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:absorption", duration_ticks=200, amplifier=1), particle=fx("end_rod", 8, 0.3, 0.05),
                      text="last_reserve", name="Last Reserve",
                      desc="When your guard resource runs dry you gain strong Absorption for %2$s seconds (every %1$s seconds).")],
         lore="Built around a deep well of stamina. When the well runs dry, it still gives."),
    dict(id="wellspring_bulwark", name="Wellspring Bulwark", tier="epic",
         guard=dict(max_stability=220, regen_per_second=19, hold_drain_per_second=6, damage_reduction_percent=62),
         stats=dict(armor=9, max_health=2),
         skills=[dict(trigger="stability_full", cooldown_ticks=400, action="shieldrpg:heal", target="self",
                      params=dict(percent_max_health=5), particle=fx("happy_villager", 6, 0.4, 0.1),
                      text="full_charge", name="Full Charge",
                      desc="When the guard is fully restored you heal %2$s%% of your max health (every %1$s seconds).")],
         lore="A spring that refills the moment it is whole."),
    dict(id="phoenix_heart_shield", name="Phoenix Heart Shield", tier="legendary",
         durability=300,
         guard=dict(max_stability=260, regen_per_second=17, hold_drain_per_second=6, damage_reduction_percent=100,
                    projectile_damage_reduction_percent=100),
         perfect_guard=dict(knockback=0.8),
         stats=dict(armor=11, max_health=6, knockback_resist=0.2),
         skills=[dict(trigger="shield_broken_item", cooldown_ticks=0, sound=dict(id="minecraft:entity.blaze.shoot", volume=0.8, pitch=0.7),
                      steps=[dict(action="shieldrpg:heal", target="self", params=dict(percent_max_health=100), particle=fx("flame", 16, 0.5, 0.1)),
                             dict(action="shieldrpg:apply_effect", target="self",
                                  params=dict(effect="minecraft:regeneration", duration_ticks=100, amplifier=1)),
                             dict(action="shieldrpg:knockback", target="nearby", radius=6, params=dict(strength=1.6), particle=fx("lava", 8, 0.4, 0.1))],
                      text="rebirth", name="Rebirth",
                      desc="When the shield finally breaks it bursts into flame: you heal %2$s%% of your max health, gain Regeneration for %3$s seconds, and enemies within 6 blocks are thrown back with force %5$s.")],
         lore="It burns out only once. What rises from it keeps you alive."),
    dict(id="shieldmaidens_last_stand", name="Shieldmaiden's Last Stand", tier="legendary",
         guard=dict(max_stability=300, regen_per_second=25, hold_drain_per_second=5, damage_reduction_percent=80),
         perfect_guard=dict(knockback=0.6),
         stats=dict(armor=12, max_health=10),
         skills=[dict(trigger="guard_break", cooldown_ticks=1200, sound=dict(id="minecraft:entity.generic.explode", volume=0.6, pitch=1.3),
                      steps=[dict(action="shieldrpg:knockback", target="nearby", radius=6, params=dict(strength=2), particle=fx("cloud", 10, 0.3, 0.1)),
                             dict(action="shieldrpg:apply_effect", target="self",
                                  params=dict(effect="minecraft:absorption", duration_ticks=400, amplifier=1), particle=fx("happy_villager", 10, 0.4, 0.1))],
                      text="last_stand", name="Last Stand",
                      desc="When the guard breaks, nearby enemies are thrown back and you gain Absorption for %3$s seconds (once every %1$s seconds).")],
         lore="Carried by the shieldmaidens who held the line when the Aegis itself was cracked."),
    dict(id="creeper_face", name="Creeper Face", tier="rare",
         guard=dict(max_stability=100, regen_per_second=20, hold_drain_per_second=4, damage_reduction_percent=50),
         stats=dict(armor=4),
         skills=[dict(trigger="perfect_guard", cooldown_ticks=120, action="shieldrpg:blast", target="self",
                      sound=dict(id="minecraft:entity.generic.explode", volume=0.9, pitch=1.0),
                      params=dict(radius=4, amount=15, strength=1.6), text="sss_boom", name="Sss... Boom",
                      desc="A Perfect Guard makes the shield explode: enemies within %2$s blocks take %3$s damage and are thrown back with force %4$s. No blocks are harmed (every %1$s seconds).")],
         lore="Stolen from a creeper that got too close. It still wants to hug you."),
    dict(id="slime_bouncer", name="Slime Bouncer", tier="common",
         guard=dict(max_stability=90, regen_per_second=22, hold_drain_per_second=3, damage_reduction_percent=45),
         stats=dict(armor=3),
         skills=[dict(trigger="blocked_melee", cooldown_ticks=40, action="shieldrpg:knockback", target="attacker_melee", max_range=5,
                      params=dict(strength=1.8), particle=fx("item_slime", 8, 0.3, 0.1), text="boing", name="Boing",
                      desc="Melee attackers you block are bounced away with force %2$s (every %1$s seconds).")],
         lore="Wobbly, sticky and surprisingly effective."),
    dict(id="cactus_buckler", name="Cactus Buckler", tier="common",
         guard=dict(max_stability=80, regen_per_second=22, hold_drain_per_second=3, damage_reduction_percent=40),
         stats=dict(armor=2),
         skills=[dict(trigger="blocked_hit", cooldown_ticks=80, action="shieldrpg:damage", target="nearby", radius=3,
                      params=dict(amount=3), particle=fx("damage_indicator", 4, 0.3, 0.1), text="spiny_bloom", name="Spiny Bloom",
                      desc="Blocking a hit pricks every enemy around you for %2$s damage (every %1$s seconds).")],
         lore="Do not hug. Do not hit. Do not touch."),
    dict(id="wither_skull_buckler", name="Wither Skull Buckler", tier="epic",
         guard=dict(max_stability=150, regen_per_second=18, hold_drain_per_second=5, damage_reduction_percent=58),
         stats=dict(armor=7),
         skills=[dict(trigger="blocked_melee", cooldown_ticks=100, action="shieldrpg:apply_effect", target="attacker_melee", max_range=5,
                      params=dict(effect="minecraft:wither", duration_ticks=80, amplifier=0), particle=fx("smoke", 8, 0.3, 0.05),
                      sound=dict(id="minecraft:entity.wither.ambient", volume=0.4, pitch=1.4), text="withering_gaze", name="Withering Gaze",
                      desc="Melee attackers you block wither for %2$s seconds (every %1$s seconds).")],
         lore="Its empty sockets watch whoever hits it. Nobody enjoys it."),
    dict(id="jack_o_shield", name="Jack o' Shield", tier="rare",
         guard=dict(max_stability=120, regen_per_second=20, hold_drain_per_second=4, damage_reduction_percent=50),
         stats=dict(armor=4),
         skills=[dict(trigger="guard_break", cooldown_ticks=400, sound=dict(id="minecraft:entity.ghast.warn", volume=0.6, pitch=1.3),
                      steps=[dict(action="shieldrpg:apply_effect", target="nearby", radius=5,
                                  params=dict(effect="minecraft:blindness", duration_ticks=80, amplifier=0), particle=fx("flame", 6, 0.3, 0.05)),
                             dict(action="shieldrpg:apply_effect", target="nearby", radius=5,
                                  params=dict(effect="minecraft:weakness", duration_ticks=100, amplifier=0))],
                      text="fright_night", name="Fright Night",
                      desc="When the guard breaks, enemies within 5 blocks are blinded for %2$s seconds and weakened for %4$s seconds (every %1$s seconds).")],
         lore="Carved with a very rude face. The grin glows when it is about to give way."),
    dict(id="piston_ram", name="Piston Ram", tier="epic",
         guard=dict(max_stability=170, regen_per_second=17, hold_drain_per_second=5, damage_reduction_percent=60),
         stats=dict(armor=8, move_speed=-0.04),
         skills=[dict(trigger="shield_bash", cooldown_ticks=60,
                      steps=[dict(action="shieldrpg:knockback", target="attacker", params=dict(strength=2.6), particle=fx("poof", 6, 0.3, 0.1)),
                             dict(action="shieldrpg:apply_effect", target="attacker", params=dict(effect="minecraft:levitation", duration_ticks=20, amplifier=0))],
                      sound=dict(id="minecraft:block.piston.extend", volume=0.9, pitch=1.0), text="piston_slam", name="Piston Slam",
                      desc="When you land a fully charged attack with this shield in either hand, you ram the target for %2$s extra damage and launch it back with force %3$s (every %1$s seconds).")],
         lore="Sticky piston, bolted to a door. It pushes back harder than it was built to."),
]

# existing items turned into shields (the items are registered in Java, the definition says "existing_item")
ITEMS = [
    dict(id="tavern_door", name="Tavern Door", tier="rare",
         guard=dict(max_stability=140, regen_per_second=18, hold_drain_per_second=5, damage_reduction_percent=55),
         perfect_guard=dict(knockback=1.2), durability=300, stats=dict(armor=6),
         skills=[dict(trigger="perfect_guard", cooldown_ticks=160, action="shieldrpg:knockback", target="nearby", radius=4,
                      params=dict(strength=2.5), particle=fx("cloud", 10, 0.4, 0.1),
                      sound=dict(id="minecraft:block.wooden_door.close", volume=1.0, pitch=0.8), text="bouncer", name="Bouncer",
                      desc="A Perfect Guard throws every enemy around you out the door (every %1$s seconds).")],
         lore="Torn off its hinges. Surprisingly good at shoving drunks, and enemies."),
    dict(id="cast_iron_pan", name="Cast-Iron Pan", tier="common",
         guard=dict(max_stability=90, regen_per_second=20, hold_drain_per_second=4, damage_reduction_percent=45),
         perfect_guard=dict(knockback=0.5), durability=250, stats=dict(armor=3),
         skills=[dict(trigger="perfect_guard", cooldown_ticks=80,
                      steps=[dict(action="shieldrpg:damage", target="attacker_melee", params=dict(amount=8), particle=fx("crit", 6, 0.3, 0.1)),
                             dict(action="shieldrpg:apply_effect", target="attacker_melee",
                                  params=dict(effect="minecraft:weakness", duration_ticks=80, amplifier=0))],
                      sound=dict(id="minecraft:block.anvil.land", volume=0.5, pitch=1.8), text="clang", name="Clang",
                      desc="A Perfect Guard rings the pan against the attacker for %2$s damage and weakens it for %3$s seconds (every %1$s seconds).")],
         lore="Dinner and defence. Mostly defence."),
    dict(id="barrel_lid", name="Barrel Lid", tier="common",
         guard=dict(max_stability=60, regen_per_second=24, hold_drain_per_second=3, damage_reduction_percent=35),
         durability=150, stats=dict(armor=2),
         skills=[dict(trigger="blocked_projectile", cooldown_ticks=300, action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:saturation", duration_ticks=20, amplifier=0), particle=fx("happy_villager", 6, 0.4, 0.1),
                      text="snack_break", name="Snack Break",
                      desc="Blocking a projectile fills your hunger bar (every %1$s seconds).")],
         lore="Better than nothing, which is what the last owner had."),
]


NEW_SHIELDS = [
    dict(id="pufferfish_buckler", name="Pufferfish Buckler", tier="common",
         guard=dict(max_stability=80, regen_per_second=22, hold_drain_per_second=3, damage_reduction_percent=50),
         stats=dict(armor=3),
         skills=[dict(trigger="blocked_melee", cooldown_ticks=100, action="shieldrpg:apply_effect", target="attacker_melee", max_range=5,
                      params=dict(effect="minecraft:poison", duration_ticks=100, amplifier=0), particle=fx("item_slime", 8, 0.3, 0.1),
                      text="toxic_spines", name="Toxic Spines",
                      desc="Melee attackers you block are poisoned for %2$s seconds (every %1$s seconds).")],
         lore="Still puffed up. Do not poke."),
    dict(id="turtle_shell_shield", name="Turtle Shell Shield", tier="rare",
         guard=dict(max_stability=200, regen_per_second=16, hold_drain_per_second=5, damage_reduction_percent=50),
         stats=dict(armor=8, knockback_resist=0.2),
         skills=[dict(trigger="stability_low", threshold=0.3, cooldown_ticks=1200, action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:resistance", duration_ticks=100, amplifier=2), particle=fx("happy_villager", 10, 0.4, 0.1),
                      sound=dict(id="minecraft:item.armor.equip_turtle", volume=1.0, pitch=0.9), text="turtle_up", name="Turtle Up",
                      desc="When the guard is nearly broken you gain strong Resistance for %2$s seconds (every %1$s seconds).")],
         lore="Slow, heavy and very hard to crack."),
    dict(id="ender_eye_shield", name="Eye of Ender Shield", tier="epic",
         guard=dict(max_stability=140, regen_per_second=18, hold_drain_per_second=5, damage_reduction_percent=50),
         stats=dict(armor=6, max_health=2),
         skills=[dict(trigger="blocked_projectile", cooldown_ticks=120, action="shieldrpg:apply_effect", target="attacker", max_range=30,
                      params=dict(effect="minecraft:blindness", duration_ticks=80, amplifier=0), particle=fx("portal", 14, 0.4, 0.3),
                      sound=dict(id="minecraft:entity.enderman.teleport", volume=0.6, pitch=1.2), text="evil_eye", name="Evil Eye",
                      desc="The shooter of a projectile you block is blinded for %2$s seconds, however far away (every %1$s seconds).")],
         lore="It looks back."),
    dict(id="blaze_shield", name="Blaze Shield", tier="rare",
         guard=dict(max_stability=120, regen_per_second=20, hold_drain_per_second=4, damage_reduction_percent=50),
         stats=dict(armor=5),
         skills=[dict(trigger="blocked_melee", cooldown_ticks=100, action="shieldrpg:ignite", target="attacker_melee", max_range=5,
                      params=dict(seconds=5), particle=fx("flame", 10, 0.3, 0.05),
                      sound=dict(id="minecraft:item.firecharge.use", volume=0.7, pitch=1.0), text="scorch", name="Scorch",
                      desc="Melee attackers you block are set on fire for %2$s seconds (every %1$s seconds).")],
         lore="Bolted together from blaze rods. Warm to the touch."),
    dict(id="prismarine_shield", name="Prismarine Shield", tier="rare",
         guard=dict(max_stability=130, regen_per_second=19, hold_drain_per_second=4, damage_reduction_percent=50),
         stats=dict(armor=5, move_speed=0.02),
         skills=[dict(trigger="shield_unequipped", cooldown_ticks=600, action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:dolphins_grace", duration_ticks=200, amplifier=0), particle=fx("bubble", 12, 0.4, 0.1),
                      text="sea_legs", name="Sea Legs",
                      desc="Putting the shield away grants Dolphin's Grace for %2$s seconds (every %1$s seconds).")],
         lore="Cut from an ocean monument. It still smells of salt."),
    dict(id="phantom_wing_shield", name="Phantom Wing Shield", tier="epic",
         guard=dict(max_stability=100, regen_per_second=22, hold_drain_per_second=3, damage_reduction_percent=50),
         stats=dict(armor=4, move_speed=0.06),
         skills=[dict(trigger="guard_break", cooldown_ticks=600, action="shieldrpg:apply_effect", target="nearby", radius=5,
                      params=dict(effect="minecraft:levitation", duration_ticks=40, amplifier=0), particle=fx("cloud", 10, 0.4, 0.1),
                      sound=dict(id="minecraft:entity.phantom.flap", volume=0.9, pitch=1.0), text="updraft", name="Updraft",
                      desc="When the guard breaks, nearby enemies are lifted into the air for %2$s seconds (every %1$s seconds).")],
         lore="Membrane stretched over a bone frame. It is almost weightless."),
    dict(id="cobweb_shield", name="Cobweb Shield", tier="rare",
         guard=dict(max_stability=90, regen_per_second=22, hold_drain_per_second=3, damage_reduction_percent=50),
         stats=dict(armor=3),
         skills=[dict(trigger="blocked_projectile", cooldown_ticks=160, action="shieldrpg:pull", target="attacker", max_range=24,
                      params=dict(strength=1.6), particle=fx("cloud", 8, 0.3, 0.05), text="reel_in", name="Reel In",
                      desc="The shooter of a projectile you block is yanked toward you (every %1$s seconds).")],
         lore="Spun by something much bigger than a spider."),
    dict(id="golden_apple_shield", name="Golden Apple Shield", tier="epic",
         guard=dict(max_stability=150, regen_per_second=18, hold_drain_per_second=5, damage_reduction_percent=50),
         stats=dict(armor=8, max_health=4),
         skills=[dict(trigger="guard_recovered", cooldown_ticks=900, action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:health_boost", duration_ticks=400, amplifier=1), particle=fx("happy_villager", 10, 0.4, 0.1),
                      sound=dict(id="minecraft:entity.player.levelup", volume=0.6, pitch=1.2), text="golden_vigor", name="Golden Vigor",
                      desc="When the guard recovers you gain Health Boost for %2$s seconds (every %1$s seconds).")],
         lore="Too shiny to eat. It is also too hard."),
    dict(id="bell_shield", name="Bell Shield", tier="rare",
         guard=dict(max_stability=140, regen_per_second=18, hold_drain_per_second=5, damage_reduction_percent=50),
         stats=dict(armor=6),
         skills=[dict(trigger="shield_raised", cooldown_ticks=600, action="shieldrpg:cleanse", target="self",
                      params=dict(), particle=fx("note", 8, 0.5, 0.1),
                      sound=dict(id="minecraft:block.bell.use", volume=1.0, pitch=1.2), text="clear_chime", name="Clear Chime",
                      desc="Raising the shield rings the bell and removes every harmful effect from you (every %1$s seconds)."),
                 ],
         lore="Taken from the village square. The villagers want it back."),
    dict(id="lantern_shield", name="Lantern Shield", tier="common",
         guard=dict(max_stability=110, regen_per_second=20, hold_drain_per_second=4, damage_reduction_percent=50),
         stats=dict(armor=4),
         skills=[dict(trigger="shield_equipped", cooldown_ticks=600, action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:night_vision", duration_ticks=600, amplifier=0), particle=fx("end_rod", 8, 0.4, 0.05),
                      text="lantern_light", name="Lantern Light",
                      desc="Taking up the shield grants Night Vision for %2$s seconds (every %1$s seconds).")],
         lore="Hold it up and the dark backs off a little."),
]
SHIELDS += NEW_SHIELDS
SHIELDS.append(dict(id="standard_shield", name="Standard Shield", tier="common", durability=336,
         guard=dict(max_stability=80, regen_per_second=20, hold_drain_per_second=4, damage_reduction_percent=70),
         stats=dict(armor=2),
         skills=[dict(trigger="perfect_guard", cooldown_ticks=100, action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:speed", duration_ticks=60, amplifier=0), particle=fx("cloud", 6, 0.3, 0.05),
                      text="riposte_step", name="Riposte Step",
                      desc="A Perfect Guard gives you speed for %2$s seconds (every %1$s seconds).")],
         lore="The plain wooden shield every soldier knows. It replaces the vanilla recipe."))



def _g(ms, regen, drain, **extra):
    return dict(max_stability=ms, regen_per_second=regen, hold_drain_per_second=drain, damage_reduction_percent=60, **extra)


SHIELDS += [
    dict(id="bastion_shield", name="Bastion Shield", tier="common", guard=_g(115, 20, 4), stats=dict(armor=4),
         skills=[dict(trigger="blocked_melee", cooldown_ticks=200, action="shieldrpg:apply_effect", target="attacker_melee",
                      params=dict(effect="minecraft:weakness", duration_ticks=120, amplifier=0), particle=fx("smoke", 8, 0.3, 0.05),
                      text="dull_blow", name="Dull Blow",
                      desc="Melee attackers you block are weakened for %2$s seconds (every %1$s seconds).")],
         lore="Five straight sides and no clever ideas. It holds the line."),
    dict(id="bannermans_standard", name="Bannerman's Standard", tier="common", guard=_g(100, 21, 4), stats=dict(armor=3),
         skills=[dict(trigger="shield_bash", cooldown_ticks=160, action="shieldrpg:apply_effect", target="attacker",
                      params=dict(effect="minecraft:blindness", duration_ticks=60, amplifier=0), particle=fx("cloud", 8, 0.3, 0.05),
                      text="dazzle", name="Dazzle",
                      desc="A fully charged attack with this shield blinds the target for %2$s seconds (every %1$s seconds).")],
         lore="Cloth snaps in the face of whoever stands too close."),
    dict(id="wicker_oval", name="Wicker Oval", tier="common", guard=_g(95, 22, 3), stats=dict(armor=3),
         skills=[dict(trigger="blocked_projectile", cooldown_ticks=100, action="shieldrpg:heal", target="self",
                      params=dict(amount=2), particle=fx("happy_villager", 6, 0.4, 0.1),
                      text="arrow_catcher", name="Arrow Catcher",
                      desc="Blocking a projectile heals you for %2$s health (every %1$s seconds).")],
         lore="Woven tight enough that arrows get stuck instead of bouncing."),
    dict(id="crusaders_cross", name="Crusader's Cross", tier="rare", guard=_g(140, 19, 4), stats=dict(armor=6),
         skills=[dict(trigger="hit_unblocked", cooldown_ticks=400, action="shieldrpg:heal", target="self",
                      params=dict(amount=4), particle=fx("heart", 5, 0.4, 0.05),
                      text="mend", name="Mend",
                      desc="When a hit gets past your shield you heal %2$s health (every %1$s seconds).")],
         lore="A cross-shaped plate. What it misses, it forgives."),
    dict(id="spearpoint_shield", name="Spearpoint Shield", tier="rare", guard=_g(130, 20, 4), stats=dict(armor=5),
         skills=[dict(trigger="shield_bash", cooldown_ticks=140,
                      steps=[dict(action="shieldrpg:damage", target="attacker", params=dict(amount=4, ignore_invulnerability=True), particle=fx("crit", 8, 0.3, 0.1)),
                             dict(action="shieldrpg:apply_effect", target="attacker", params=dict(effect="minecraft:slowness", duration_ticks=100, amplifier=1))],
                      text="hamstring", name="Hamstring",
                      desc="A fully charged attack with this shield hits the target for %2$s damage and slows it for %3$s seconds (every %1$s seconds).")],
         lore="Pointed down like a blade. It goes for the legs."),
    dict(id="gatehouse_shield", name="Gatehouse Shield", tier="rare", guard=_g(145, 18, 5), stats=dict(armor=6),
         skills=[dict(trigger="stability_low", cooldown_ticks=500,
                      steps=[dict(action="shieldrpg:knockback", target="nearby", radius=4, params=dict(strength=1.2), particle=fx("poof", 8, 0.3, 0.1)),
                             dict(action="shieldrpg:apply_effect", target="nearby", radius=4, params=dict(effect="minecraft:slowness", duration_ticks=80, amplifier=1))],
                      text="hold_the_gate", name="Hold the Gate",
                      desc="When the guard is nearly broken, nearby enemies are shoved back and slowed for %3$s seconds (every %1$s seconds).")],
         lore="Flat on top, like a portcullis."),
    dict(id="forge_twins_roundel", name="Forge Twins Roundel", tier="rare", guard=_g(120, 20, 4), stats=dict(armor=5),
         skills=[dict(trigger="stability_full", cooldown_ticks=1200, action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:fire_resistance", duration_ticks=300, amplifier=0), particle=fx("flame", 10, 0.4, 0.05),
                      text="heat_ward", name="Heat Ward",
                      desc="When the guard is fully charged you gain Fire Resistance for %2$s seconds (every %1$s seconds).")],
         lore="Two discs hammered out together. Neither one has ever been cold."),
    dict(id="leaf_shield", name="Leaf Shield", tier="rare", guard=_g(105, 23, 3), stats=dict(armor=4),
         skills=[dict(trigger="perfect_guard", cooldown_ticks=300, action="shieldrpg:apply_effect", target="self",
                      params=dict(effect="minecraft:regeneration", duration_ticks=100, amplifier=1), particle=fx("happy_villager", 8, 0.4, 0.1),
                      text="verdant_pulse", name="Verdant Pulse",
                      desc="A Perfect Guard grants Regeneration for %2$s seconds (every %1$s seconds).")],
         lore="Moss-grown and light. It likes being struck at just the right moment."),
    dict(id="sunburst_targe", name="Sunburst Targe", tier="epic", guard=_g(170, 18, 5), stats=dict(armor=8),
         skills=[dict(trigger="blocked_projectile", cooldown_ticks=200, action="shieldrpg:damage", target="nearby", radius=5,
                      params=dict(amount=8), particle=fx("flash", 1, 0.0, 0.0) if False else fx("end_rod", 10, 0.5, 0.15),
                      text="shrapnel", name="Shrapnel",
                      desc="Blocking a projectile hurts every nearby enemy for %2$s damage (every %1$s seconds).")],
         lore="Eight points, all of them aimed outward."),
    dict(id="cathedral_aegis", name="Cathedral Aegis", tier="epic", guard=_g(190, 17, 5), stats=dict(armor=9, max_health=2),
         skills=[dict(trigger="perfect_guard", cooldown_ticks=400,
                      steps=[dict(action="shieldrpg:apply_effect", target="self", params=dict(effect="minecraft:resistance", duration_ticks=80, amplifier=1), particle=fx("end_rod", 10, 0.5, 0.1)),
                             dict(action="shieldrpg:apply_effect", target="self", params=dict(effect="minecraft:regeneration", duration_ticks=100, amplifier=1))],
                      text="sanctuary", name="Sanctuary",
                      desc="A Perfect Guard grants Resistance for %2$s seconds and Regeneration for %4$s seconds (every %1$s seconds).")],
         lore="Pointed like a window of stained glass. Inside it, nothing touches you."),
]

# Damage reduction is 50-100 for every shield. The weaker the block, the more the shield gives back while raised (a faster
# walk speed, plus a bit of held speed in stats); the stronger the block, the slower you walk while it is raised.
# id: (damage_reduction_percent, projectile_damage_reduction_percent or None, raised_move_speed, extra stats)
BALANCE = {
    "wooden_buckler":        (50, None, 0.20, dict(move_speed=0.06)),
    "pufferfish_buckler":    (52, None, 0.12, dict(move_speed=0.03)),
    "cobweb_shield":         (54, None, 0.10, dict()),
    "phantom_wing_shield":   (78, None, -0.08, dict()),
    "lantern_shield":        (58, None, 0.05, dict()),
    "prismarine_shield":     (62, None, 0.0, dict()),
    "blaze_shield":          (64, None, 0.0, dict()),
    "ender_eye_shield":      (66, None, 0.0, dict()),
    "bell_shield":           (68, None, -0.05, dict()),
    "golden_apple_shield":   (70, None, -0.05, dict()),
    "standard_shield":       (70, None, -0.05, dict()),
    "bastion_shield":        (62, None, 0.0, dict()),
    "bannermans_standard":   (55, None, 0.10, dict()),
    "wicker_oval":           (52, None, 0.15, dict(move_speed=0.03)),
    "crusaders_cross":       (68, None, -0.05, dict()),
    "spearpoint_shield":     (64, None, 0.0, dict()),
    "gatehouse_shield":      (88, None, -0.2, dict()),
    "forge_twins_roundel":   (60, None, 0.05, dict()),
    "leaf_shield":           (56, None, 0.10, dict(move_speed=0.03)),
    "sunburst_targe":        (82, None, -0.15, dict()),
    "cathedral_aegis":       (86, None, -0.20, dict()),
    "turtle_shell_shield":   (100, None, -0.30, dict()),
    "cactus_buckler":        (50, None, 0.15, dict(move_speed=0.04)),
    "barrel_lid":            (50, None, 0.15, dict(move_speed=0.04)),
    "slime_bouncer":         (55, None, 0.10, dict(move_speed=0.03)),
    "copper_roundel":        (55, None, 0.10, dict(move_speed=0.03)),
    "cast_iron_pan":         (55, None, 0.10, dict(move_speed=0.03)),
    "skirmisher_pelta":      (55, None, 0.20, dict(move_speed=0.08)),
    "duelists_parma":        (58, None, 0.10, dict()),
    "duelists_buckler":      (58, None, 0.10, dict()),
    "wayfarers_roundshield": (58, None, 0.10, dict()),
    "pilgrims_roundshield":  (58, None, 0.10, dict()),
    "creeper_face":          (60, None, 0.05, dict()),
    "jack_o_shield":         (80, None, -0.1, dict()),
    "ironhide_targe":        (62, None, 0.0, dict()),
    "iron_heater_shield":    (65, 80, 0.0, dict()),
    "vanguard_kite":         (68, None, 0.0, dict()),
    "wither_skull_buckler":  (70, None, 0.0, dict()),
    "tavern_door":           (72, None, -0.05, dict()),
    "thornback_scutum":      (72, None, -0.05, dict()),
    "reservoir_bulwark":     (75, None, -0.10, dict()),
    "wellspring_bulwark":    (75, None, -0.10, dict()),
    "stormcaller_aspis":     (75, None, -0.10, dict()),
    "frostguard_pavise":     (78, 100, -0.15, dict()),
    "piston_ram":            (80, None, -0.15, dict()),
    "beacon_aegis":          (80, None, -0.15, dict()),
    "sunforged_bulwark":     (92, None, -0.22, dict()),
    "shieldmaidens_last_stand": (95, None, -0.25, dict()),
    "wardens_greatshield":   (90, 95, -0.25, dict()),
    "phoenix_heart_shield":  (100, 100, -0.30, dict()),
}
# Shields with a skill that fires when the guard is nearly broken or breaks: fragile guard, very high damage reduction
FRAGILE = ["sunforged_bulwark", "phoenix_heart_shield", "shieldmaidens_last_stand", "jack_o_shield", "turtle_shell_shield",
           "phantom_wing_shield", "gatehouse_shield"]
FRAGILE_STABILITY = 0.5
FRAGILE_MIN = 40
FRAGILE_FIXED = {"turtle_shell_shield": 50}
for _s in SHIELDS + ITEMS:
    _dr, _pr, _rs, _st = BALANCE[_s["id"]]
    _s["guard"]["damage_reduction_percent"] = _dr
    if _pr is None:
        _s["guard"].pop("projectile_damage_reduction_percent", None)
    else:
        _s["guard"]["projectile_damage_reduction_percent"] = _pr
    if _rs:
        _s["guard"]["raised_move_speed"] = _rs
    _s["stats"] = {**_s["stats"], **_st}

# Which project each shield lives in. CORE shields are written into the ShieldRPG project (namespace shieldrpg, shown in the
# ShieldRPG creative tab).
CORE_IDS = ["standard_shield", "bastion_shield", "bannermans_standard", "wicker_oval", "crusaders_cross", "spearpoint_shield", "gatehouse_shield", "forge_twins_roundel", "leaf_shield", "sunburst_targe", "cathedral_aegis", "wooden_buckler", "iron_heater_shield", "copper_roundel", "skirmisher_pelta", "thornback_scutum",
            "frostguard_pavise", "stormcaller_aspis", "sunforged_bulwark", "wardens_greatshield", "vanguard_kite",
            "ironhide_targe", "duelists_parma", "duelists_buckler", "wayfarers_roundshield", "pilgrims_roundshield",
            "reservoir_bulwark", "wellspring_bulwark", "phoenix_heart_shield", "shieldmaidens_last_stand"]
# Crafting recipes (all from items that already exist in the game). "S" = vanilla shield: those recipes are upgrades of it.
# id: ("shaped", pattern, {key: item}) or ("shapeless", [items], count)
M = "minecraft:"
SHIELD = [M + "shield", "shieldrpg:standard_shield"]
RECIPES = {
    "bastion_shield": ("shaped", ["BIB", "BBB", " B "], {"B": M + "stone_bricks", "I": M + "iron_ingot"}),
    "bannermans_standard": ("shaped", ["WWW", "WIW", " S "], {"W": M + "white_wool", "I": M + "iron_ingot", "S": M + "stick"}),
    "wicker_oval": ("shaped", ["SSS", "STS", "SSS"], {"S": M + "stick", "T": M + "string"}),
    "crusaders_cross": ("shaped", [" G ", "GIG", " G "], {"G": M + "gold_ingot", "I": M + "iron_ingot"}),
    "spearpoint_shield": ("shaped", ["FIF", "IPI", " I "], {"F": M + "flint", "I": M + "iron_ingot", "P": "#minecraft:planks"}),
    "gatehouse_shield": ("shaped", ["CIC", "CCC", " I "], {"C": M + "cobblestone", "I": M + "iron_ingot"}),
    "forge_twins_roundel": ("shaped", ["MIM", "ICI", " I "], {"M": M + "magma_cream", "I": M + "iron_ingot", "C": M + "copper_ingot"}),
    "leaf_shield": ("shaped", ["MMM", "MIM", " M "], {"M": M + "moss_block", "I": M + "iron_ingot"}),
    "sunburst_targe": ("shaped", ["GDG", "BIB", "GDG"], {"G": M + "gold_ingot", "D": M + "diamond", "B": M + "glowstone_dust", "I": M + "iron_ingot"}),
    "cathedral_aegis": ("shaped", ["QGQ", "QDQ", " Q "], {"Q": M + "quartz", "G": M + "gold_ingot", "D": M + "diamond"}),
    "standard_shield": ("shaped", ["PIP", "PPP", " P "], {"P": "#minecraft:planks", "I": M + "iron_ingot"}),
    # Only four shields are upgrades of the vanilla shield ("V"); everything else is built from raw materials.
    # rank 1: wood and scraps
    "wooden_buckler": ("shaped", ["PPP", "PSP", " P "], {"P": "#minecraft:planks", "S": M + "stick"}),
    "cactus_buckler": ("shaped", ["CCC", "CSC", " C "], {"C": M + "cactus", "S": M + "stick"}),
    "tavern_door": ("shapeless", [M + "oak_door", M + "iron_nugget", M + "iron_nugget"], 1),
    "barrel_lid": ("shapeless", [M + "barrel"], 2),
    # rank 2: iron, copper, leather
    "cast_iron_pan": ("shaped", ["III", "ICI", " S "], {"I": M + "iron_ingot", "C": M + "coal", "S": M + "stick"}),
    "viking_roundshield": ("shaped", ["PIP", "PPP", "PIP"], {"I": M + "iron_ingot", "P": "#minecraft:planks"}),
    "iron_heater_shield": ("shaped", ["III", "IPI", " I "], {"I": M + "iron_ingot", "P": "#minecraft:planks"}),
    "copper_roundel": ("shaped", ["CCC", "CNC", " C "], {"C": M + "copper_ingot", "N": M + "iron_nugget"}),
    "ironhide_targe": ("shaped", ["LIL", "LPL", "LIL"], {"L": M + "leather", "I": M + "iron_ingot", "P": "#minecraft:planks"}),
    "slime_bouncer": ("shaped", ["BBB", "BPB", " P "], {"B": M + "slime_ball", "P": "#minecraft:planks"}),
    # rank 3: iron body plus a themed item
    "skirmisher_pelta": ("shaped", ["FFF", "LIL", " L "], {"F": M + "feather", "L": M + "leather", "I": M + "iron_ingot"}),
    "thornback_scutum": ("shaped", ["DID", "IPI", "DID"], {"D": M + "pointed_dripstone", "I": M + "iron_ingot", "P": "#minecraft:planks"}),
    "frostguard_pavise": ("shaped", ["PIP", "PIP", " I "], {"P": M + "packed_ice", "I": M + "iron_ingot"}),
    "duelists_parma": ("shaped", ["ISI", "PPP", " P "], {"I": M + "iron_ingot", "S": M + "iron_sword", "P": "#minecraft:planks"}),
    "duelists_buckler": ("shaped", ["GIG", "PPP", " P "], {"G": M + "gold_ingot", "I": M + "iron_ingot", "P": "#minecraft:planks"}),
    "wayfarers_roundshield": ("shaped", ["LCL", "PPP", " P "], {"L": M + "leather", "C": M + "compass", "P": "#minecraft:planks"}),
    "pilgrims_roundshield": ("shaped", ["LNL", "PPP", " P "], {"L": M + "leather", "N": M + "lead", "P": "#minecraft:planks"}),
    "svalinn_frost_oak_shield": ("shaped", ["SIS", "SSS", " I "], {"S": M + "snow_block", "I": M + "iron_ingot"}),
    "jack_o_shield": ("shaped", ["PCP", "PPP", " T "], {"P": "#minecraft:planks", "C": M + "carved_pumpkin", "T": M + "torch"}),
    "stormcaller_aspis": ("shaped", ["CLC", "CCC", " C "], {"C": M + "copper_ingot", "L": M + "lightning_rod"}),
    "vanguard_kite": ("shaped", ["WWW", "IVI"], {"W": M + "red_wool", "I": M + "iron_ingot", "V": SHIELD}),  # upgrade
    # rank 4: gems, rare drops
    "creeper_face": ("shaped", ["GGG", "GHG", " I "], {"G": M + "gunpowder", "H": M + "creeper_head", "I": M + "iron_ingot"}),
    "reservoir_bulwark": ("shaped", ["LEL", "III", " I "], {"L": M + "lapis_block", "E": M + "experience_bottle", "I": M + "iron_ingot"}),
    "wellspring_bulwark": ("shaped", ["PWP", "DID", " I "], {"P": M + "prismarine_shard", "W": M + "water_bucket", "D": M + "diamond", "I": M + "iron_ingot"}),
    "wither_skull_buckler": ("shaped", ["OSO", "OOO", " O "], {"O": M + "obsidian", "S": M + "wither_skeleton_skull"}),
    "piston_ram": ("shaped", ["IPI", "IDI", " I "], {"I": M + "iron_ingot", "P": M + "sticky_piston", "D": M + "iron_door"}),
    "ironwood_tower_shield": ("shaped", ["IDI", "PIP", " I "], {"I": M + "iron_ingot", "D": M + "diamond", "P": "#minecraft:planks"}),
    "sunforged_bulwark": ("shaped", ["GBG", "GVG"], {"G": M + "gold_ingot", "B": M + "glowstone", "V": SHIELD}),  # upgrade
    # rank 5: legendary
    "pufferfish_buckler": ("shaped", ["PFP", "PPP", " P "], {"P": "#minecraft:planks", "F": M + "pufferfish"}),
    "turtle_shell_shield": ("shaped", ["SSS", "SIS", " S "], {"S": M + "turtle_scute", "I": M + "iron_ingot"}),
    "ender_eye_shield": ("shaped", ["EEE", "IOI", " I "], {"E": M + "ender_eye", "I": M + "iron_ingot", "O": M + "obsidian"}),
    "blaze_shield": ("shaped", ["RBR", "III", " I "], {"R": M + "blaze_rod", "B": M + "blaze_powder", "I": M + "iron_ingot"}),
    "prismarine_shield": ("shaped", ["PCP", "PIP", " I "], {"P": M + "prismarine_shard", "C": M + "prismarine_crystals", "I": M + "iron_ingot"}),
    "phantom_wing_shield": ("shaped", ["MDM", "MMM", " I "], {"M": M + "phantom_membrane", "D": M + "diamond", "I": M + "iron_ingot"}),
    "cobweb_shield": ("shaped", ["CSC", "CCC", " S "], {"C": M + "cobweb", "S": M + "string"}),
    "golden_apple_shield": ("shaped", ["GAG", "GGG", " G "], {"G": M + "gold_ingot", "A": M + "golden_apple"}),
    "bell_shield": ("shaped", ["IBI", "GGG", " G "], {"I": M + "iron_ingot", "B": M + "bell", "G": M + "gold_ingot"}),
    "lantern_shield": ("shaped", ["ILI", "PPP", " P "], {"I": M + "iron_ingot", "L": M + "lantern", "P": "#minecraft:planks"}),
    "beacon_aegis": ("shaped", ["DBD", "OOO", " O "], {"D": M + "diamond", "B": M + "beacon", "O": M + "obsidian"}),
    "shieldmaidens_last_stand": ("shaped", ["DTD", "GGG", " G "], {"D": M + "diamond", "T": M + "totem_of_undying", "G": M + "gold_ingot"}),
    "aegis_of_the_shieldmaiden": ("shaped", ["DHD", "NNN", " N "], {"D": M + "diamond", "H": M + "heart_of_the_sea", "N": M + "netherite_ingot"}),
    "wardens_greatshield": ("shaped", ["EEE", "NVN"], {"E": M + "echo_shard", "N": M + "netherite_scrap", "V": SHIELD}),  # upgrade
    "phoenix_heart_shield": ("shaped", ["RTR", "RVR"], {"R": M + "blaze_rod", "T": M + "totem_of_undying", "V": SHIELD}),  # upgrade
}

# How hard the recipe is (1 = scraps ... 5 = netherite / rare drops). The harder the recipe, the better the shield:
# durability (unless the shield sets its own), Guard Stability and armor are scaled by CRAFT_SCALE[rank].
DIFFICULTY = {
    "wicker_oval": 1, "bannermans_standard": 2, "bastion_shield": 2, "crusaders_cross": 3, "spearpoint_shield": 3, "gatehouse_shield": 3,
    "forge_twins_roundel": 3, "leaf_shield": 3, "sunburst_targe": 4, "cathedral_aegis": 4,
    "standard_shield": 1, "wooden_buckler": 1, "cactus_buckler": 1, "tavern_door": 1, "barrel_lid": 1,
    "cast_iron_pan": 2, "viking_roundshield": 2, "iron_heater_shield": 2, "copper_roundel": 2, "ironhide_targe": 2, "slime_bouncer": 2,
    "skirmisher_pelta": 3, "thornback_scutum": 3, "frostguard_pavise": 3, "duelists_parma": 3, "duelists_buckler": 3,
    "wayfarers_roundshield": 3, "pilgrims_roundshield": 3, "svalinn_frost_oak_shield": 3, "jack_o_shield": 3,
    "stormcaller_aspis": 3, "vanguard_kite": 3,
    "creeper_face": 4, "reservoir_bulwark": 4, "wellspring_bulwark": 4, "wither_skull_buckler": 4, "piston_ram": 4,
    "ironwood_tower_shield": 4, "sunforged_bulwark": 4,
    "pufferfish_buckler": 2, "lantern_shield": 2, "turtle_shell_shield": 3, "blaze_shield": 3, "prismarine_shield": 3,
    "cobweb_shield": 3, "bell_shield": 3, "ender_eye_shield": 4, "phantom_wing_shield": 4, "golden_apple_shield": 4,
    "beacon_aegis": 5, "shieldmaidens_last_stand": 5, "aegis_of_the_shieldmaiden": 5, "wardens_greatshield": 5, "phoenix_heart_shield": 5,
}
CRAFT_SCALE = {1: 0.85, 2: 0.95, 3: 1.0, 4: 1.1, 5: 1.2}
# Norse shields are hand-made JSON files; their base numbers (before the craft scale) live here so the script can rescale them
NORSE_BASE = {  # id: (durability, max_stability, armor)
    "viking_roundshield": (250, 40, 4), "svalinn_frost_oak_shield": (336, 64, 7),
    "ironwood_tower_shield": (450, 120, 11), "aegis_of_the_shieldmaiden": (600, 120, 13),
}
NORSE_IDS = ["viking_roundshield", "svalinn_frost_oak_shield", "ironwood_tower_shield", "aegis_of_the_shieldmaiden"]


def recipe_json(ns, i):
    kind, a, b = RECIPES[i]
    out = {"type": "minecraft:crafting_" + kind, "category": "equipment"}
    if kind == "shaped":
        out.update(pattern=a, key=b)
        count = 1
    else:
        out.update(ingredients=a)
        count = b
    out["result"] = {"id": f"{ns}:{i}", "count": count}
    out["neoforge:conditions"] = [{"type": "shieldrpg:crafting_enabled", "shield": f"{ns}:{i}"}]
    return out


TARGETS = {
    "core": dict(ns="shieldrpg", res="src/main/resources", shields=[s["id"] for s in SHIELDS + ITEMS], items=[]),
}


STABILITY_SCALE = 0.4
# Durability follows the vanilla shield (336): common a bit below it, rare = vanilla, epic and legendary above it
DURABILITY_BY_TIER = {"common": 250, "rare": 336, "epic": 450, "legendary": 600}


def definition(s, existing, ns, lang):
    d = {"item_id": f"{ns}:{s['id']}"}
    if existing:
        d["existing_item"] = True
    d["name"] = s["name"]
    if s["tier"] in GRAD:
        d["name_gradient"] = GRAD[s["tier"]]
    d["tier"] = s["tier"]
    d["lore_key"] = f"tooltip.{ns}.{s['id']}.lore.1"
    k = CRAFT_SCALE[DIFFICULTY[s["id"]]]
    d["durability"] = s["durability"] if "durability" in s else round(DURABILITY_BY_TIER[s["tier"]] * k)
    guard = dict(s["guard"])
    # Guard Stability is 60% lower than the original design; regeneration scales with it so a full refill takes the same time
    guard["max_stability"] = max(10, round(guard["max_stability"] * STABILITY_SCALE * k))
    if s["id"] in FRAGILE:
        guard["max_stability"] = FRAGILE_FIXED.get(s["id"], max(FRAGILE_MIN, round(guard["max_stability"] * FRAGILE_STABILITY)))
    regen = round(guard["regen_per_second"] * STABILITY_SCALE, 1)
    guard["regen_per_second"] = int(regen) if regen == int(regen) else regen
    d["guard"] = guard
    if "perfect_guard" in s:
        d["perfect_guard"] = s["perfect_guard"]
    d["stats"] = dict(s["stats"])
    if d["stats"].get("armor"):
        d["stats"]["armor"] = max(1, round(d["stats"]["armor"] * k))
    skills = []
    for sk in s["skills"]:
        out = {"trigger": sk["trigger"]}
        if "threshold" in sk:
            out["threshold"] = sk["threshold"]
        if sk.get("cooldown_ticks"):
            out["cooldown_ticks"] = sk["cooldown_ticks"]
        if "steps" in sk:
            out["steps"] = sk["steps"]
        else:
            step = {"action": sk["action"], "target": sk["target"]}
            if "max_range" in sk:
                step["max_range"] = sk["max_range"]
            step["params"] = sk["params"]
            if "particle" in sk:
                step["particle"] = sk["particle"]
            out.update(step)
        if "sound" in sk:
            out["sound"] = sk["sound"]
        key = f"tooltip.{ns}.{s['id']}.skill.{sk['text']}"
        out["text_key"] = key
        lang[key] = sk["name"]
        lang[key + ".desc"] = DESC.get(sk["text"], sk["desc"])
        for step in (out["steps"] if "steps" in out else [out]):
            boost_fx(step)
        skills.append(out)
    d["skills"] = skills
    lang[f"item.{ns}.{s['id']}"] = s["name"]
    lang[f"tooltip.{ns}.{s['id']}.lore.1"] = s["lore"]
    return d


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


BY_ID = {s["id"]: s for s in SHIELDS + ITEMS}
for name, t in TARGETS.items():
    ns, res = t["ns"], t["res"]
    lang_path = f"{res}/assets/{ns}/lang/en_us.json"
    if name == "core" and os.path.exists(lang_path):  # core already has a lang file: merge into it
        with open(lang_path, encoding="utf-8") as f:
            lang = json.load(f)
    else:
        lang = {}
    for i in t["shields"] + t["items"]:
        s = BY_ID[i]
        write(f"{res}/data/{ns}/shieldrpg/shields/{i}.json", definition(s, i in t["items"], ns, lang))
        write(f"{res}/assets/{ns}/items/{i}.json", {"model": {
            "type": "minecraft:condition", "property": "minecraft:using_item",
            "on_false": {"type": "minecraft:model", "model": f"{ns}:item/{i}"},
            "on_true": {"type": "minecraft:model", "model": f"{ns}:item/{i}_blocking"}}})
        write(f"{res}/assets/{ns}/models/item/{i}.json", {"parent": "shieldrpg:item/template_shield", "textures": {"layer0": f"{ns}:item/{i}"}})
        write(f"{res}/assets/{ns}/models/item/{i}_blocking.json", {"parent": "shieldrpg:item/template_shield_blocking", "textures": {"layer0": f"{ns}:item/{i}"}})
    for i in t["shields"] + t["items"] + (NORSE_IDS if name == "core" else []):
        write(f"{res}/data/{ns}/recipe/{i}.json", recipe_json(ns, i))
    if name == "core":  # hand-made Norse shields: rescale from their base numbers
        for i, (dur, stab, armor) in NORSE_BASE.items():
            path = f"{res}/data/{ns}/shieldrpg/shields/{i}.json"
            with open(path, encoding="utf-8") as f:
                nd = json.load(f)
            k = CRAFT_SCALE[DIFFICULTY[i]]
            nd["durability"] = round(dur * k)
            nd["guard"]["max_stability"] = max(10, round(stab * k))
            nd["stats"]["armor"] = max(1, round(armor * k))
            for sk in nd["skills"]:
                step = sk if "action" in sk else None
                if step is None:
                    continue
                if i == "viking_roundshield":
                    step["particle"] = {"type": "minecraft:sweep_attack", "count": 3, "spread": 0.8}
                elif i == "svalinn_frost_oak_shield":
                    step["particle"] = {"type": "minecraft:snowflake", "count": 24, "spread": 0.6, "speed": 0.08}
                elif i == "ironwood_tower_shield":
                    step["particle"] = {"type": "minecraft:crit", "count": 18, "spread": 0.6, "speed": 0.25}
                elif i == "aegis_of_the_shieldmaiden":
                    step["particle"] = {"type": "minecraft:happy_villager", "count": 30, "spread": 0.6, "speed": 0.1}
            write(path, nd)
    if name == "core":
        write(f"{res}/data/minecraft/recipe/shield.json", {
            "type": "minecraft:crafting_shaped", "category": "equipment",
            "pattern": ["WoW", "WWW", " W "],
            "key": {"W": "#minecraft:planks", "o": "minecraft:iron_ingot"},
            "result": {"id": "minecraft:shield", "count": 1},
            "neoforge:conditions": [{"type": "shieldrpg:crafting_enabled", "shield": "minecraft:shield"}]})
    # all recipes are unlocked in the recipe book from the start (an advancement that rewards them on the first tick)
    recipe_ids = t["shields"] + t["items"] + (NORSE_IDS if name == "core" else [])
    write(f"{res}/data/{ns}/advancement/unlock_shield_recipes.json", {
        "criteria": {"start": {"trigger": "minecraft:tick"}},
        "rewards": {"recipes": [f"{ns}:{i}" for i in recipe_ids]}})
    write(lang_path, lang)
    print(name, len(t["shields"]), "shields,", len(t["items"]), "existing items,", len(lang), "lang keys")

# Tag files so PSM / other mods see the shields (existing tag files are extended, not replaced)
for name, t in TARGETS.items():
    ids = [f"{t['ns']}:{i}" for i in t["shields"] + t["items"]]
    for tag in ("c/tags/item/tools/shields.json", "c/tags/item/weapons/shields.json", "playerstatmod/tags/item/weapon/shield.json",
                "playerstatmod/tags/item/weapon/one_handed.json"):
        path = f"{t['res']}/data/{tag}"
        values = []
        if os.path.exists(path):
            with open(path, encoding="utf-8") as f:
                values = json.load(f)["values"]
        for v in ids:
            if v not in values:
                values.append(v)
        write(path, {"replace": False, "values": values})
