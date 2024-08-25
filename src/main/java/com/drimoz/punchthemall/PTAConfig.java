package com.drimoz.punchthemall;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class PTAConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec COMMON_CONFIG;

    public static final ForgeConfigSpec.ConfigValue<Boolean> allowFakePlayers;
    public static final ForgeConfigSpec.ConfigValue<Integer> interactionCooldown;

    static {
        BUILDER.comment("PunchThemAll Common Configs");
        BUILDER.push("PunchThemAll");

        BUILDER.push("Fake_Players");
        allowFakePlayers = BUILDER
                .comment("Define whether Fake Players are allowed to perform the Interactions.")
                .define("allowFakePlayers", true);
        BUILDER.pop();

        BUILDER.push("Interaction_Delay");
        interactionCooldown = BUILDER
                .comment("Define the minimum interval of time (in tick) between two Interactions.")
                .defineInRange("min_interaction_delay", 1, 1, 10000);
        BUILDER.pop();

        BUILDER.pop();

        COMMON_CONFIG = BUILDER.build();
    }
}
