package com.drimoz.punchthemall;

import net.minecraftforge.common.ForgeConfigSpec;

public class PTAConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec COMMON_CONFIG;
    public static final ForgeConfigSpec CLIENT_CONFIG;

    public static final ClientConfig CLIENT;

    public static final InteractionConfig INTERACTIONS;
    public static final PlayerConfig PLAYERS;
    public static final DropConfig DROPS;
    public static final LoaderConfig LOADER;
    public static final DebugConfig DEBUG;

    static {
        // The recipe viewers are a client concern, so their settings live in a client file. A
        // server has no business deciding which key a player holds to read a tooltip.
        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        clientBuilder.comment("PunchThemAll client-side settings. These affect what you see, never what the mod does.");
        clientBuilder.push("PunchThemAll");
        CLIENT = new ClientConfig(clientBuilder);
        clientBuilder.pop();
        CLIENT_CONFIG = clientBuilder.build();

        BUILDER.comment(
                "PunchThemAll common configuration.",
                "The config is split by responsibility so pack makers can tune gameplay, automation, drops and JSON loading separately.",
                "Most gameplay values are read live; JSON loader options apply on /reload or world load."
        );
        BUILDER.push("PunchThemAll");

        INTERACTIONS = new InteractionConfig(BUILDER);
        PLAYERS = new PlayerConfig(BUILDER);
        DROPS = new DropConfig(BUILDER);
        LOADER = new LoaderConfig(BUILDER);
        DEBUG = new DebugConfig(BUILDER);

        BUILDER.pop();

        COMMON_CONFIG = BUILDER.build();
    }

    /**
     * Read a config value, falling back to its declared default when no config file is attached yet.
     *
     * <p>{@code ForgeConfigSpec.ConfigValue.get()} throws while the spec is unloaded. That is the
     * right behaviour for gameplay switches, which are only ever read mid-game, but interaction
     * loading also consults the debug flags, and a logging toggle must not be able to abort a
     * datapack reload. It is also what makes the config-reading code reachable from unit tests.</p>
     */
    public static <T> T valueOrDefault(ForgeConfigSpec.ConfigValue<T> value) {
        return COMMON_CONFIG.isLoaded() ? value.get() : value.getDefault();
    }

    /** The client counterpart of {@link #valueOrDefault}, for the client spec. */
    public static <T> T clientValueOrDefault(ForgeConfigSpec.ConfigValue<T> value) {
        return CLIENT_CONFIG.isLoaded() ? value.get() : value.getDefault();
    }

    public static class ClientConfig {
        /** Which key expands a recipe tooltip from its summary to the full breakdown. */
        public final ForgeConfigSpec.ConfigValue<String> tooltipDetailKey;

        /** How many rows of drops a recipe box shows before the rest are shared between slots. */
        public final ForgeConfigSpec.IntValue maxDropRows;

        private ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.push("Tooltips");
            tooltipDetailKey = builder
                    .comment(
                            "Which key to hold to expand an interaction tooltip in JEI.",
                            "A tooltip that shows everything at once is unreadable on a busy interaction, and one",
                            "that shows a summary only is useless when you need the detail — so the detail is behind",
                            "a key, and this is that key.",
                            "shift, control, alt: hold it to expand.",
                            "always: never summarise, always show everything.",
                            "never: never expand, summary only."
                    )
                    // Arrays.asList, not List.of: the config spec is validated by testing a null
                    // value against the allowed list, and an immutable list throws on contains(null)
                    // rather than answering false. That crashes config loading before the game starts.
                    .defineInList("detail_key", "shift", java.util.Arrays.asList("shift", "control", "alt", "always", "never"));
            maxDropRows = builder
                    .comment(
                            "How many rows of drops an interaction shows in JEI before the rest share slots.",
                            "JEI sizes a category rather than a recipe, so the widest interaction in the pack decides",
                            "how tall every other one is drawn. This caps that: one interaction dropping thirty things",
                            "no longer makes the other sixty three rows tall.",
                            "Drops past the cap share a slot, which JEI cycles through on its own.",
                            "A pack whose interactions all fit in fewer rows is unaffected either way."
                    )
                    .defineInRange("max_drop_rows", 3, 1, 6);
            builder.pop();
        }
    }

    public static class InteractionConfig {
        public final ForgeConfigSpec.BooleanValue enabled;
        public final ForgeConfigSpec.IntValue cooldownTicks;
        public final ForgeConfigSpec.IntValue maxMatchesPerClick;
        public final ForgeConfigSpec.BooleanValue cancelVanillaInteraction;
        public final ForgeConfigSpec.BooleanValue allowLeftClick;
        public final ForgeConfigSpec.BooleanValue allowRightClick;
        public final ForgeConfigSpec.BooleanValue allowBlockInteractions;
        public final ForgeConfigSpec.BooleanValue allowAirInteractions;
        public final ForgeConfigSpec.BooleanValue allowFluidInteractions;
        public final ForgeConfigSpec.BooleanValue allowTransformations;
        public final ForgeConfigSpec.BooleanValue allowOffsetTransformations;
        public final ForgeConfigSpec.IntValue maxTransformationOffset;
        public final ForgeConfigSpec.IntValue maxTransformationsPerInteraction;
        public final ForgeConfigSpec.BooleanValue fireProtectionEvents;

        private InteractionConfig(ForgeConfigSpec.Builder builder) {
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
        public final ForgeConfigSpec.BooleanValue allowFakePlayers;
        public final ForgeConfigSpec.BooleanValue applyCooldownToFakePlayers;
        public final ForgeConfigSpec.BooleanValue applyPlayerEffectsToFakePlayers;
        public final ForgeConfigSpec.BooleanValue allowPlayerDamage;
        public final ForgeConfigSpec.BooleanValue allowFoodConsumption;

        private PlayerConfig(ForgeConfigSpec.Builder builder) {
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
                    .comment("Allow interaction JSON entries to hurt real players.")
                    .define("allow_player_damage", true);
            allowFoodConsumption = builder
                    .comment("Allow interaction JSON entries to consume real player saturation/food.")
                    .define("allow_food_consumption", true);
            builder.pop();
        }
    }

    public static class DropConfig {
        public final ForgeConfigSpec.BooleanValue placeInInventory;
        public final ForgeConfigSpec.BooleanValue placeFakePlayerDropsInInventory;
        public final ForgeConfigSpec.DoubleValue dropOffset;
        public final ForgeConfigSpec.DoubleValue dropVelocity;

        private DropConfig(ForgeConfigSpec.Builder builder) {
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

    public static class LoaderConfig {
        public final ForgeConfigSpec.BooleanValue recursiveDiscovery;
        public final ForgeConfigSpec.BooleanValue failFast;
        public final ForgeConfigSpec.BooleanValue lowerCaseGeneratedIds;
        public final ForgeConfigSpec.BooleanValue loadFromDatapacks;

        private LoaderConfig(ForgeConfigSpec.Builder builder) {
            builder.push("Loader");
            loadFromDatapacks = builder
                    .comment(
                            "Also load interactions from datapacks at data/<namespace>/pta/interaction/*.json, on top of config files.",
                            "Datapack files are read on /reload and are synchronised to clients by vanilla; they support schema_version 1 and 2.",
                            "A datapack interaction overrides a config interaction sharing the same id."
                    )
                    .define("load_from_datapacks", false);
            recursiveDiscovery = builder
                    .comment("Discover interaction JSON files recursively inside config/punchthemall/interactions.", "Disable to load only files directly in the interactions folder.")
                    .define("recursive_discovery", true);
            failFast = builder
                    .comment("Stop loading remaining interactions after the first invalid JSON file.", "Useful while developing packs; keep disabled for production packs so one bad file does not disable all others.")
                    .define("fail_fast", false);
            lowerCaseGeneratedIds = builder
                    .comment("Lowercase generated interaction ids based on file names.", "Minecraft resource locations require lowercase paths; keep enabled unless all file names are already valid.")
                    .define("lowercase_generated_ids", true);
            builder.pop();
        }
    }

    public static class DebugConfig {
        public final ForgeConfigSpec.BooleanValue logLoadedInteractions;
        public final ForgeConfigSpec.BooleanValue logSkippedInteractions;

        private DebugConfig(ForgeConfigSpec.Builder builder) {
            builder.push("Debug");
            logLoadedInteractions = builder
                    .comment("Log every interaction id loaded from config.")
                    .define("log_loaded_interactions", false);
            logSkippedInteractions = builder
                    .comment("Log why runtime interactions are skipped by global config gates.")
                    .define("log_skipped_interactions", false);
            builder.pop();
        }
    }
}
