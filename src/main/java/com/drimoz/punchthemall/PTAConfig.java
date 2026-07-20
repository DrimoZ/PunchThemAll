package com.drimoz.punchthemall;

import net.neoforged.neoforge.common.ModConfigSpec;

public class PTAConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec COMMON_CONFIG;

    public static final InteractionConfig INTERACTIONS;
    public static final PlayerConfig PLAYERS;
    public static final DropConfig DROPS;
    public static final DebugConfig DEBUG;

    static {
        BUILDER.comment(
                "PunchThemAll common configuration.",
                "The config is split by responsibility so pack makers can tune gameplay, automation and drops separately.",
                "Interactions themselves are defined by datapacks at data/<namespace>/pta/interaction/*.json (see the docs)."
        );
        BUILDER.push("PunchThemAll");

        INTERACTIONS = new InteractionConfig(BUILDER);
        PLAYERS = new PlayerConfig(BUILDER);
        DROPS = new DropConfig(BUILDER);
        DEBUG = new DebugConfig(BUILDER);

        BUILDER.pop();

        COMMON_CONFIG = BUILDER.build();
    }

    public static class InteractionConfig {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.IntValue cooldownTicks;
        public final ModConfigSpec.IntValue maxMatchesPerClick;
        public final ModConfigSpec.BooleanValue cancelVanillaInteraction;
        public final ModConfigSpec.BooleanValue allowLeftClick;
        public final ModConfigSpec.BooleanValue allowRightClick;
        public final ModConfigSpec.BooleanValue allowBlockInteractions;
        public final ModConfigSpec.BooleanValue allowAirInteractions;
        public final ModConfigSpec.BooleanValue allowFluidInteractions;
        public final ModConfigSpec.BooleanValue allowTransformations;

        private InteractionConfig(ModConfigSpec.Builder builder) {
            builder.push("Interactions");
            enabled = builder
                    .comment("Master switch for every configured PunchThemAll interaction.")
                    .define("enabled", true);
            cooldownTicks = builder
                    .comment("Minimum delay, in ticks, between two successful interactions for the same player.", "20 ticks = 1 second. Set to 0 to disable player cooldowns.")
                    .defineInRange("cooldown_ticks", 1, 0, 10000);
            maxMatchesPerClick = builder
                    .comment("Maximum number of matching interactions processed per click.", "Use 1 for predictable recipes, higher values for intentional chained outputs. Transformations still happen at most once per click.")
                    .defineInRange("max_matches_per_click", 64, 1, 1024);
            cancelVanillaInteraction = builder
                    .comment("Cancel the vanilla click event after at least one PunchThemAll interaction succeeds.", "Keep enabled to prevent duplicate vanilla handling; disable only for advanced compatibility packs.")
                    .define("cancel_vanilla_interaction", true);
            allowLeftClick = builder
                    .comment("Allow configured left-click interactions.")
                    .define("allow_left_click", true);
            allowRightClick = builder
                    .comment("Allow configured right-click interactions.")
                    .define("allow_right_click", true);
            allowBlockInteractions = builder
                    .comment("Allow interactions targeting blocks.")
                    .define("allow_block_interactions", true);
            allowAirInteractions = builder
                    .comment("Allow interactions configured with an air target.")
                    .define("allow_air_interactions", true);
            allowFluidInteractions = builder
                    .comment("Allow interactions targeting source fluids through ray tracing.")
                    .define("allow_fluid_interactions", true);
            allowTransformations = builder
                    .comment("Allow interactions to transform blocks or fluids after a successful drop roll.")
                    .define("allow_transformations", true);
            builder.pop();
        }
    }

    public static class PlayerConfig {
        public final ModConfigSpec.BooleanValue allowFakePlayers;
        public final ModConfigSpec.BooleanValue applyCooldownToFakePlayers;
        public final ModConfigSpec.BooleanValue applyPlayerEffectsToFakePlayers;
        public final ModConfigSpec.BooleanValue allowPlayerDamage;
        public final ModConfigSpec.BooleanValue allowFoodConsumption;

        private PlayerConfig(ModConfigSpec.Builder builder) {
            builder.push("Players");
            allowFakePlayers = builder
                    .comment("Allow Fake Players and machines to perform PunchThemAll interactions.")
                    .define("allow_fake_players", true);
            applyCooldownToFakePlayers = builder
                    .comment("Apply Interactions.cooldown_ticks to Fake Players too.", "Useful to throttle automated users from mods such as Click Machine.")
                    .define("apply_cooldown_to_fake_players", false);
            applyPlayerEffectsToFakePlayers = builder
                    .comment("Apply player-only side effects such as swing animation, damage and hunger to Fake Players.", "Most automation setups should keep this disabled.")
                    .define("apply_player_effects_to_fake_players", false);
            allowPlayerDamage = builder
                    .comment("Allow interaction entries to hurt real players.")
                    .define("allow_player_damage", true);
            allowFoodConsumption = builder
                    .comment("Allow interaction entries to consume real player saturation/food.")
                    .define("allow_food_consumption", true);
            builder.pop();
        }
    }

    public static class DropConfig {
        public final ModConfigSpec.BooleanValue placeInInventory;
        public final ModConfigSpec.BooleanValue placeFakePlayerDropsInInventory;
        public final ModConfigSpec.DoubleValue dropOffset;
        public final ModConfigSpec.DoubleValue dropVelocity;

        private DropConfig(ModConfigSpec.Builder builder) {
            builder.push("Drops");
            placeInInventory = builder
                    .comment("Try to place interaction result items directly into the player inventory.", "If the inventory is full, remaining items are dropped in the world.")
                    .define("place_in_inventory", true);
            placeFakePlayerDropsInInventory = builder
                    .comment("Try to place interaction result items directly into a Fake Player or machine inventory.", "Disable when automation should always eject items into the world.")
                    .define("place_fake_player_drops_in_inventory", true);
            dropOffset = builder
                    .comment("Distance from the interacted block face where world drops spawn.")
                    .defineInRange("world_drop_offset", 0.75D, 0.0D, 4.0D);
            dropVelocity = builder
                    .comment("Velocity multiplier applied to world drops in the clicked face direction.")
                    .defineInRange("world_drop_velocity", 0.10D, 0.0D, 2.0D);
            builder.pop();
        }
    }

    public static class DebugConfig {
        public final ModConfigSpec.BooleanValue logLoadedInteractions;
        public final ModConfigSpec.BooleanValue logSkippedInteractions;

        private DebugConfig(ModConfigSpec.Builder builder) {
            builder.push("Debug");
            logLoadedInteractions = builder
                    .comment("Log every interaction id loaded from datapacks.")
                    .define("log_loaded_interactions", false);
            logSkippedInteractions = builder
                    .comment("Log why runtime interactions are skipped by global config gates.")
                    .define("log_skipped_interactions", false);
            builder.pop();
        }
    }
}
