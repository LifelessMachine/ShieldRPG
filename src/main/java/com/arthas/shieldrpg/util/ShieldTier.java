package com.arthas.shieldrpg.util;

import net.minecraft.world.item.Rarity;

public enum ShieldTier {
    COMMON("tooltip.shieldrpg.tier.common", 0xAAAAAA, false, Rarity.COMMON),
    RARE("tooltip.shieldrpg.tier.rare", 0x55AAFF, false, Rarity.RARE),
    EPIC("tooltip.shieldrpg.tier.epic", 0xB04CFF, true, Rarity.EPIC),
    LEGENDARY("tooltip.shieldrpg.tier.legendary", 0x7CFF8A, true, Rarity.EPIC);

    private final String translationKey;
    private final int rgb;
    private final boolean glow;
    private final Rarity vanillaRarity;

    ShieldTier(String translationKey, int rgb, boolean glow, Rarity vanillaRarity) {
        this.translationKey = translationKey;
        this.rgb = rgb;
        this.glow = glow;
        this.vanillaRarity = vanillaRarity;
    }

    public static ShieldTier byName(String name) {
        for (ShieldTier tier : values()) {
            if (tier.name().equalsIgnoreCase(name)) {
                return tier;
            }
        }
        return COMMON;
    }

    public String translationKey() {
        return translationKey;
    }

    public int rgb() {
        return rgb;
    }

    public boolean isGlow() {
        return glow;
    }

    public Rarity vanillaRarity() {
        return vanillaRarity;
    }
}
