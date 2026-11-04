package com.arthas.shieldrpg.api;

import net.minecraft.world.item.ItemStack;

public record ShieldGuardContext(ItemStack shield, double holdDrainPerSecond) {}
