package com.arthas.shieldrpg.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ShieldApi {

    public static final int PRIORITY_FALLBACK = -1000;

    public static final int PRIORITY_BUILT_IN = 0;

    public static final int PRIORITY_DEFAULT = 100;

    public record Registration<T extends ShieldProvider>(T provider, int priority) {}

    public record ShieldEntry(Item item, Supplier<ShieldDescriptor> descriptor) {}

    private static final List<Registration<ShieldResourceProvider>> RESOURCE_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<Registration<ShieldStunProvider>> STUN_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<Registration<ShieldHealProvider>> HEAL_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<Registration<ShieldPerfectGuardListener>> PERFECT_GUARD_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<Registration<ShieldTriggerListener>> TRIGGER_LISTENERS = new CopyOnWriteArrayList<>();
    private static final Map<String, ShieldSkillAction> SKILL_ACTIONS = new ConcurrentHashMap<>();
    private static final List<ShieldEntry> SHIELDS = new CopyOnWriteArrayList<>();
    private static final Map<Item, ShieldDefinition> DEFINITIONS = new ConcurrentHashMap<>();
    private static final Set<String> CUSTOM_TRIGGERS = ConcurrentHashMap.newKeySet();
    private static final Map<Item, Boolean> SKILL_COOLDOWN_DISPLAY = new ConcurrentHashMap<>();
    private static final List<ShieldSkillCooldownListener> SKILL_COOLDOWN_LISTENERS = new CopyOnWriteArrayList<>();
    private static final Map<String, Boolean> CRAFTING_OVERRIDES = new ConcurrentHashMap<>();
    private static volatile ShieldTriggerDispatcher triggerDispatcher;
    private static volatile Predicate<String> craftingConfig = id -> true;

    private static volatile String externalItemStatsOwner;

    private ShieldApi() {}

    public static void declareExternalItemStats(String ownerId) {
        externalItemStatsOwner = ownerId;
    }

    public static boolean hasExternalItemStats() {
        return externalItemStatsOwner != null;
    }

    public static boolean registerResourceProvider(ShieldResourceProvider provider, int priority) {
        return register(RESOURCE_PROVIDERS, provider, priority);
    }

    public static boolean registerStunProvider(ShieldStunProvider provider, int priority) {
        return register(STUN_PROVIDERS, provider, priority);
    }

    public static boolean registerHealProvider(ShieldHealProvider provider, int priority) {
        return register(HEAL_PROVIDERS, provider, priority);
    }

    public static boolean registerPerfectGuardListener(ShieldPerfectGuardListener listener, int priority) {
        return register(PERFECT_GUARD_LISTENERS, listener, priority);
    }

    public static List<Registration<ShieldPerfectGuardListener>> getPerfectGuardListeners() {
        return sorted(PERFECT_GUARD_LISTENERS);
    }

    public static boolean registerTriggerListener(ShieldTriggerListener listener, int priority) {
        return register(TRIGGER_LISTENERS, listener, priority);
    }

    public static List<Registration<ShieldTriggerListener>> getTriggerListeners() {
        return sorted(TRIGGER_LISTENERS);
    }

    public static void registerShieldDefinition(Item item, ShieldDefinition definition) {
        DEFINITIONS.put(item, definition);
    }

    public static @Nullable ShieldDefinition getShieldDefinition(Item item) {
        return DEFINITIONS.get(item);
    }

    public static synchronized boolean registerSkillCooldownListener(ShieldSkillCooldownListener listener) {
        for (ShieldSkillCooldownListener existing : SKILL_COOLDOWN_LISTENERS) {
            if (existing.id().equals(listener.id())) {
                return false;
            }
        }
        return SKILL_COOLDOWN_LISTENERS.add(listener);
    }

    public static List<ShieldSkillCooldownListener> getSkillCooldownListeners() {
        return List.copyOf(SKILL_COOLDOWN_LISTENERS);
    }

    public static void setSkillCooldownDisplay(Item item, boolean enabled) {
        SKILL_COOLDOWN_DISPLAY.put(item, enabled);
    }

    public static boolean isSkillCooldownDisplayEnabled(Item item, @Nullable ShieldDefinition definition) {
        Boolean override = SKILL_COOLDOWN_DISPLAY.get(item);
        if (override != null) {
            return override;
        }
        return definition == null || definition.showSkillCooldown();
    }

    public static boolean registerTrigger(String triggerId) {
        if (triggerId == null || triggerId.isBlank() || ShieldTriggerIds.ALL.contains(triggerId)) {
            return false;
        }
        return CUSTOM_TRIGGERS.add(triggerId);
    }

    public static boolean isKnownTrigger(String triggerId) {
        return ShieldTriggerIds.ALL.contains(triggerId) || CUSTOM_TRIGGERS.contains(triggerId);
    }

    public static void fireTrigger(ServerPlayer player, ItemStack shield, String triggerId, @Nullable Entity directSource,
                                   @Nullable LivingEntity attacker, float amount, Map<String, Object> extras) {
        ShieldTriggerDispatcher dispatcher = triggerDispatcher;
        if (dispatcher != null && player != null && shield != null && triggerId != null) {
            dispatcher.fire(player, shield, triggerId, directSource, attacker, amount, extras == null ? Map.of() : extras);
        }
    }

    public static void installTriggerDispatcher(ShieldTriggerDispatcher dispatcher) {
        triggerDispatcher = dispatcher;
    }

    public static boolean registerSkillAction(ShieldSkillAction action) {
        return SKILL_ACTIONS.putIfAbsent(action.id(), action) == null;
    }

    @Nullable
    public static ShieldSkillAction getSkillAction(String id) {
        return SKILL_ACTIONS.get(id);
    }

    public static List<Registration<ShieldResourceProvider>> getResourceProviders() {
        return sorted(RESOURCE_PROVIDERS);
    }

    public static List<Registration<ShieldStunProvider>> getStunProviders() {
        return sorted(STUN_PROVIDERS);
    }

    public static List<Registration<ShieldHealProvider>> getHealProviders() {
        return sorted(HEAL_PROVIDERS);
    }

    public static synchronized boolean registerShield(Item item, Supplier<ShieldDescriptor> descriptor) {
        for (ShieldEntry existing : SHIELDS) {
            if (existing.item() == item) {
                return false;
            }
        }
        SHIELDS.add(new ShieldEntry(item, descriptor));
        return true;
    }

    public static List<ShieldEntry> getShields() {
        return List.copyOf(SHIELDS);
    }

    private static synchronized <T extends ShieldProvider> boolean register(List<Registration<T>> list, T provider, int priority) {
        for (Registration<T> existing : list) {
            if (existing.provider().id().equals(provider.id())) {
                return false;
            }
        }
        list.add(new Registration<>(provider, priority));
        return true;
    }

    private static <T extends ShieldProvider> List<Registration<T>> sorted(List<Registration<T>> list) {
        return list.stream().sorted(Comparator.comparingInt((Registration<T> r) -> r.priority()).reversed()).toList();
    }

    public static void installCraftingConfig(Predicate<String> enabledByConfig) {
        craftingConfig = enabledByConfig;
    }

    public static void setCraftingEnabled(String shieldId, boolean enabled) {
        CRAFTING_OVERRIDES.put(shieldId, enabled);
    }

    public static void clearCraftingOverride(String shieldId) {
        CRAFTING_OVERRIDES.remove(shieldId);
    }

    public static boolean isCraftingEnabled(String shieldId) {
        Boolean override = CRAFTING_OVERRIDES.get(shieldId);
        if (override != null) {
            return override;
        }
        return craftingConfig.test(shieldId);
    }

    public static List<String> getShieldIds() {
        List<String> ids = new ArrayList<>();
        for (ShieldEntry entry : SHIELDS) {
            Identifier key = BuiltInRegistries.ITEM.getKey(entry.item());
            if (key != null) {
                ids.add(key.toString());
            }
        }
        ids.sort(Comparator.naturalOrder());
        return ids;
    }

    public static @Nullable Item getShieldItem(String shieldId) {
        Identifier id = Identifier.tryParse(shieldId);
        if (id == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null || item == Items.AIR) {
            return null;
        }
        for (ShieldEntry entry : SHIELDS) {
            if (entry.item() == item) {
                return item;
            }
        }
        return null;
    }

    public static ItemStack createShieldStack(String shieldId) {
        Item item = getShieldItem(shieldId);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public static @Nullable ItemEntity dropShield(Level level, Vec3 position, String shieldId) {
        ItemStack stack = createShieldStack(shieldId);
        if (stack.isEmpty() || level.isClientSide()) {
            return null;
        }
        ItemEntity entity = new ItemEntity(level, position.x, position.y, position.z, stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
        return entity;
    }

    public static boolean giveShield(Player player, String shieldId) {
        ItemStack stack = createShieldStack(shieldId);
        if (stack.isEmpty() || player.level().isClientSide()) {
            return false;
        }
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), stack);
            entity.setDefaultPickUpDelay();
            player.level().addFreshEntity(entity);
        }
        return true;
    }
}
