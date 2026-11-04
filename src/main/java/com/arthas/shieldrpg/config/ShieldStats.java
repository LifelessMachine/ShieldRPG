package com.arthas.shieldrpg.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ShieldStats {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final class Num {
        private final ModConfigSpec.DoubleValue value;
        private final double fallback;

        private Num(ModConfigSpec.DoubleValue value, double fallback) {
            this.value = value;
            this.fallback = fallback;
        }

        public double get() {
            try {
                return value.get();
            } catch (RuntimeException notLoaded) {
                return fallback;
            }
        }
    }

    public static final class Whole {
        private final ModConfigSpec.IntValue value;
        private final int fallback;

        private Whole(ModConfigSpec.IntValue value, int fallback) {
            this.value = value;
            this.fallback = fallback;
        }

        public int get() {
            try {
                return value.get();
            } catch (RuntimeException notLoaded) {
                return fallback;
            }
        }
    }

    private static Num num(String path, double defaultValue, double min, double max) {
        return new Num(BUILDER.defineInRange(path, defaultValue, min, max), defaultValue);
    }

    private static Whole whole(String path, int defaultValue, int min, int max) {
        return new Whole(BUILDER.defineInRange(path, defaultValue, min, max), defaultValue);
    }

    public static final Whole GUARD_BREAK_COOLDOWN_TICKS;
    public static final Whole RECHARGE_DELAY_TICKS;
    public static final Whole EXHAUSTION_COOLDOWN_TICKS;
    public static final Whole PERFECT_BLOCK_TICKS;
    public static final Whole PERFECT_GUARD_COOLDOWN_TICKS;
    public static final Whole REPEAT_HIT_WINDOW_TICKS;
    public static final ModConfigSpec.ConfigValue<String> RESOURCE_PROVIDER;

    private static final ModConfigSpec.Builder STARTUP_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DISABLED_RECIPES;

    static {
        STARTUP_BUILDER.push("crafting");
        DISABLED_RECIPES = STARTUP_BUILDER
                .comment("Shield ids whose crafting recipe is switched off. Read before recipes load; /reload also applies changes.",
                        "The vanilla shield recipe is off by default: use shieldrpg:standard_shield instead.")
                .defineListAllowEmpty("disabled_recipes", java.util.List.of("minecraft:shield"), () -> "", o -> o instanceof String);
        STARTUP_BUILDER.pop();
    }

    public static final ModConfigSpec STARTUP_SPEC = STARTUP_BUILDER.build();

    public static java.util.Set<String> disabledRecipes() {
        try {
            return new java.util.HashSet<>(DISABLED_RECIPES.get());
        } catch (RuntimeException notLoaded) {
            return java.util.Set.of("minecraft:shield");
        }
    }

    static {
        BUILDER.push("mechanics");
        GUARD_BREAK_COOLDOWN_TICKS = whole("guard_break_cooldown_ticks", 200, 20, 1200);
        RECHARGE_DELAY_TICKS = whole("recharge_delay_ticks", 80, 0, 600);
        EXHAUSTION_COOLDOWN_TICKS = whole("exhaustion_cooldown_ticks", 30, 0, 200);
        PERFECT_BLOCK_TICKS = new Whole(BUILDER
                .comment("Perfect Guard window in ticks, counted from the moment the shield starts blocking",
                        "(a raised shield starts blocking after its block delay, 0.25 s).")
                .defineInRange("perfect_block_ticks", 10, 0, 40), 10);
        PERFECT_GUARD_COOLDOWN_TICKS = whole("perfect_guard_cooldown_ticks", 0, 0, 200);
        REPEAT_HIT_WINDOW_TICKS = whole("repeat_hit_window_ticks", 10, 0, 100);
        RESOURCE_PROVIDER = BUILDER
                .comment("Which resource pays for holding a shield. 'auto' = highest-priority registered provider")
                .define("resource_provider", "auto");
        BUILDER.pop();

    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ShieldStats() {}
}
