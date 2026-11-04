package com.arthas.shieldrpg.item;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.arthas.shieldrpg.api.ShieldApi;
import com.arthas.shieldrpg.api.ShieldDescriptor;
import com.arthas.shieldrpg.api.ShieldGuardContext;
import com.arthas.shieldrpg.internal.ShieldResources;
import com.arthas.shieldrpg.util.ShieldText;
import com.arthas.shieldrpg.util.ShieldTier;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public abstract class NorseShieldItem extends ShieldItem implements ShieldBehavior {

    private static final float BLOCK_ANGLE_DEGREES = 90.0f;

    private final String fullName;
    private final int nameFrom;
    private final int nameTo;

    protected NorseShieldItem(Item.Properties properties, String fullName, int nameFrom, int nameTo) {
        super(properties);
        this.fullName = fullName;
        this.nameFrom = nameFrom;
        this.nameTo = nameTo;
        ShieldApi.registerShield(this, this::getDescriptor);
    }

    public static Item.Properties shieldProps(Item.Properties props, int durability, ShieldTier tier, float damageReductionPercent) {
        Item.Properties p = props.stacksTo(1)
                .durability(durability)
                .rarity(tier.vanillaRarity())
                .equippableUnswappable(EquipmentSlot.OFFHAND);

        p.delayedComponent(DataComponents.BLOCKS_ATTACKS, provider -> blocksAttacks(provider, damageReductionPercent));

        return p;
    }

    public static BlocksAttacks blocksAttacks(HolderLookup.Provider provider, float damageReductionPercent) {
        return new BlocksAttacks(
                0.25f,
                1.0f,
                List.of(new BlocksAttacks.DamageReduction(BLOCK_ANGLE_DEGREES, Optional.empty(), 0.0f, damageReductionPercent / 100.0f)),
                new BlocksAttacks.ItemDamageFunction(3.0f, 1.0f, 1.0f),
                Optional.of(provider.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                Optional.of(SoundEvents.SHIELD_BLOCK),
                Optional.of(SoundEvents.SHIELD_BREAK)
        );
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild
                && !ShieldResources.canRaiseGuard(player, new ShieldGuardContext(stack, getHoldStaminaDrain()))) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public Component getName(ItemStack stack) {
        return ShieldText.gradient(fullName, nameFrom, nameTo);
    }

    public double getStability(ItemStack stack) {
        return getStability(stack, getMaxStability());
    }

    public static double getStability(ItemStack stack, double maxStability) {
        if (stack == null || stack.isEmpty()) {
            return maxStability;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.getDoubleOr("guard_stability", maxStability);
        }
        return maxStability;
    }

    public static void setStability(ItemStack stack, double stability, double maxStability) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (stability >= maxStability - 0.001) {
                tag.remove("guard_stability");
                tag.remove("guard_broken");
            } else {
                tag.putDouble("guard_stability", Math.max(0.0, stability));
            }
        });
    }

    public static void setGuardBroken(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putDouble("guard_stability", 0.0);
            tag.putBoolean("guard_broken", true);
        });
    }

    public static boolean isGuardBroken(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.getBooleanOr("guard_broken", false);
        }
        return false;
    }

    public static void resetGuardBreak(ItemStack stack, double maxStability) {
        setStability(stack, maxStability, maxStability);
    }

    public abstract double getMaxStability();
    public abstract double getStabilityRegenRate();
    public abstract double getHoldStaminaDrain();
    public abstract ShieldTier getTier();

    public double getPerfectGuardKnockback() {
        return 0.0;
    }

    public double getProjectileDamageReductionPercent() {
        return -1;
    }

    public int getPerfectGuardWindowTicks() {
        return -1;
    }

    public List<String> getSkillTextKeys() {
        return List.of();
    }

    public abstract ShieldDescriptor getDescriptor();

    protected final ShieldDescriptor describe(String loreKey, double damageReductionPercent) {
        ShieldTier tier = getTier();
        return ShieldDescriptor.of(tier.translationKey(), tier.rgb(), tier.isGlow(), loreKey,
                getMaxStability(), getStabilityRegenRate(), getHoldStaminaDrain(), damageReductionPercent);
    }

    public void onBlockedAttack(ServerPlayer player, @Nullable LivingEntity attacker, float damage) {}
    public void onPerfectBlock(ServerPlayer player, @Nullable LivingEntity attacker, float damage) {}
}
