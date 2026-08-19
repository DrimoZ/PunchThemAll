package com.drimoz.punchthemall;

import net.neoforged.neoforge.common.ModConfigSpec;

public class PTAConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec COMMON_CONFIG;
    public static final ModConfigSpec CLIENT_CONFIG;

    public static final ClientConfig CLIENT;

    public static final InteractionConfig INTERACTIONS;
    public static final PlayerConfig PLAYERS;
    public static final DropConfig DROPS;
    public static final DebugConfig DEBUG;

    static {
        // The recipe viewers are a client concern, so their settings live in a client file. A
        // server has no business deciding which key a player holds to read a tooltip.
        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        clientBuilder.comment("PunchThemAll client-side settings. These affect what you see, never what the mod does.");
        clientBuilder.push("PunchThemAll");
        CLIENT = new ClientConfig(clientBuilder);
        clientBuilder.pop();
        CLIENT_CONFIG = clientBuilder.build();

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

    /**
     * Read a config value, falling back to its declared default when no config file is attached yet.
     *
     * <p>{@code ModConfigSpec.ConfigValue.get()} throws while the spec is unloaded. That is the right
     * behaviour for gameplay switches — they are only ever read mid-game — but interaction loading
     * also consults the debug flags, and it must not be possible for a logging toggle to abort a
     * datapack reload. It is also what makes the config-reading code reachable from unit tests.</p>
     */
    public static <T> T valueOrDefault(ModConfigSpec.ConfigValue<T> value) {
        return COMMON_CONFIG.isLoaded() ? value.get() : value.getDefault();
    }

    /** The client counterpart of {@link #valueOrDefault}, for the client spec. */
    public static <T> T clientValueOrDefault(ModConfigSpec.ConfigValue<T> value) {
        return CLIENT_CONFIG.isLoaded() ? value.get() : value.getDefault();
    }

    public static class ClientConfig {
        /** Which key expands a recipe tooltip from its summary to the full breakdown. */
        public final ModConfigSpec.ConfigValue<String> tooltipDetailKey;

        private ClientConfig(ModConfigSpec.Builder builder) {
            builder.push("Tooltips");
            tooltipDetailKey = builder
                    .comment(
                            "Which key to hold to expand an interaction tooltip in JEI/EMI.",
                            "A tooltip that shows everything at once is unreadable on a busy interaction, and one",
                            "that shows a summary only is useless when you need the detail — so the detail is behind",
                            "a key, and this is that key.",
                            "shift, control, alt: hold it to expand.",
                            "always: never summarise, always show everything.",
                            "never: never expand, summary only."
                    )
                    // Arrays.asList, not List.of: NeoForge validates the spec by testing a null
                    // value against the allowed list, and an immutable list throws on contains(null)
                    // rather than answering false. That crashes config loading before the game starts.
                    .defineInList("detail_key", "shift", java.util.Arrays.asList("shift", "control", "alt", "always", "never"));
            builder.pop();
        }
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
        public final ModConfigSpec.BooleanValue allowOffsetTransformations;
        public final ModConfigSpec.IntValue maxTransformationOffset;
        public final ModConfigSpec.IntValue maxTransformationsPerInteraction;
        public final ModConfigSpec.BooleanValue fireProtectionEvents;

        private InteractionConfig(ModConfigSpec.Builder builder) {
            builder.push("Interactions");
            enabled = builder
                    .comment("Master switch for every configured PunchThemAll interaction.")
                    .define("enabled", true);
            cooldownTicks = builder
                    .comment("Minimum delay, in ticks, between two successful interactions for the same player.", "20 ticks = 1 second. Set to 0 to disable player cooldowns.")
                    .defineInRange("cooldown_ticks", 1, 0, 10000);
            maxMatchesPerClick = builder
                    .comment("Maximum number of matching interactions processed per click.", "Use 1 for predictable recipes, higher values for intentional chained outputs. Any one block is still transformed at most once per click.")
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
            allowOffsetTransformations = builder
                    .comment(
                            "Allow transformations to act on a block other than the one that was interacted with.",
                            "Disabling this keeps every transformation on the clicked block, whatever the datapack asks for."
                    )
                    .define("allow_offset_transformations", true);
            maxTransformationOffset = builder
                    .comment(
                            "How far a transformation may reach from the interacted block, in blocks along the longest axis.",
                            "A transformation asking for more is skipped. This bounds what a datapack can touch from a single click."
                    )
                    .defineInRange("max_transformation_offset", 8, 0, 64);
            maxTransformationsPerInteraction = builder
                    .comment(
                            "Maximum number of blocks one interaction may transform per click.",
                            "Each one is a block update, so this bounds the cost of a single click on the server. A region counts every block it covers."
                    )
                    .defineInRange("max_transformations_per_interaction", 64, 1, 4096);
            fireProtectionEvents = builder
                    .comment(
                            "Post block break/place events for transformations, so claim and protection mods can veto them.",
                            "Leave enabled on any server that is not single player: without it, an offset transformation can reach inside a protected area."
                    )
                    .define("fire_protection_events", true);
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
