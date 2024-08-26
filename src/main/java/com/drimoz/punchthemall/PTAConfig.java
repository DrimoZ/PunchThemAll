package com.drimoz.punchthemall;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class PTAConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec COMMON_CONFIG;

    public static final ForgeConfigSpec.ConfigValue<Boolean> allowFakePlayers;
    public static final ForgeConfigSpec.ConfigValue<Boolean> dropInInventory;
    public static final ForgeConfigSpec.ConfigValue<Integer> interactionCooldown;

    static {
        BUILDER.comment("PunchThemAll Common Configs");
        BUILDER.push("PunchThemAll");

        BUILDER.push("Interaction_Delay");
        interactionCooldown = BUILDER
                .comment("Define the minimum interval of time (in tick) between two Interactions.")
                .defineInRange("min_interaction_delay", 1, 1, 10000);
        BUILDER.pop();

        BUILDER.push("Fake_Players");
        allowFakePlayers = BUILDER
                .comment("Define whether Fake Players are allowed to perform the Interactions.")
                .define("allowFakePlayers", true);
        BUILDER.pop();

        BUILDER.push("Drop_or_Place_in_Inventory");
        dropInInventory = BUILDER
                .comment("Define whether the result items obtained from the Interactions should be directly placed in the player (machine) inventory or be dropped like vanilla Minecraft. \n(When this option is activated if there is no place in the player (machine) inventory the items will still be dropped in the world)")
                .define("place_in_inventory", true);
        BUILDER.pop();

        BUILDER.pop();

        COMMON_CONFIG = BUILDER.build();
    }
}
