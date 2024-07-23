package com.drimoz.punchthemall.core.registry;

public class RegistryConstants {

    public static final String SAME_STATE = "copy_state_value";

    public static final String INCORRECT_FORMAT = "Incorrect Json format";
    public static final String SEPARATOR = ".";

    public static final String STRING_TYPE = "type";
    public static final String STRING_HUNGER = "hunger";
    public static final String STRING_DAMAGE = "damage";
    public static final String STRING_HAND = "hand";
    public static final String STRING_BLOCK = "block";
    public static final String STRING_TRANSFORMATION = "transformation";
    public static final String STRING_POOL = "pool";
    public static final String STRING_BIOME = "biome";

    // Hunger
    public static final String STRING_HUNGER_CHANCE = "chance";
    public static final String STRING_HUNGER_CHANCE_FULL = STRING_HUNGER + SEPARATOR + STRING_HUNGER_CHANCE;
    public static final String STRING_HUNGER_COUNT = "count";
    public static final String STRING_HUNGER_COUNT_FULL = STRING_HUNGER + SEPARATOR + STRING_HUNGER_COUNT;
    public static final String STRING_HUNGER_MIN = "min";
    public static final String STRING_HUNGER_MIN_FULL = STRING_HUNGER + SEPARATOR + STRING_HUNGER_MIN;
    public static final String STRING_HUNGER_MAX = "max";
    public static final String STRING_HUNGER_MAX_FULL = STRING_HUNGER + SEPARATOR + STRING_HUNGER_MAX;

    // Damage
    public static final String STRING_DAMAGE_CHANCE = "chance";
    public static final String STRING_DAMAGE_CHANCE_FULL = STRING_DAMAGE + SEPARATOR + STRING_DAMAGE_CHANCE;
    public static final String STRING_DAMAGE_COUNT = "count";
    public static final String STRING_DAMAGE_COUNT_FULL = STRING_DAMAGE + SEPARATOR + STRING_DAMAGE_COUNT;
    public static final String STRING_DAMAGE_MIN = "min";
    public static final String STRING_DAMAGE_MIN_FULL = STRING_DAMAGE + SEPARATOR + STRING_DAMAGE_MIN;
    public static final String STRING_DAMAGE_MAX = "max";
    public static final String STRING_DAMAGE_MAX_FULL = STRING_DAMAGE + SEPARATOR + STRING_DAMAGE_MAX;

    // Hand
    public static final String STRING_HAND_HAND = "hand";
    public static final String STRING_HAND_HAND_FULL = STRING_HAND + SEPARATOR + STRING_HAND_HAND;
    public static final String STRING_HAND_ITEM = "item";
    public static final String STRING_HAND_ITEM_FULL = STRING_HAND + SEPARATOR + STRING_HAND_ITEM;
    public static final String STRING_HAND_ITEM_ITEM = "item";
    public static final String STRING_HAND_ITEM_ITEM_FULL = STRING_HAND_ITEM_FULL + SEPARATOR + STRING_HAND_ITEM_ITEM;
    public static final String STRING_HAND_ITEM_TAG = "tag";
    public static final String STRING_HAND_ITEM_TAG_FULL = STRING_HAND_ITEM_FULL + SEPARATOR + STRING_HAND_ITEM_TAG;
    public static final String STRING_HAND_ITEM_NBT = "nbt";
    public static final String STRING_HAND_ITEM_NBT_FULL = STRING_HAND_ITEM_FULL + SEPARATOR + STRING_HAND_ITEM_NBT;
    public static final String STRING_HAND_ITEM_NBT_WHITELIST = "whitelist";
    public static final String STRING_HAND_ITEM_NBT_WHITELIST_FULL = STRING_HAND_ITEM_NBT_FULL + SEPARATOR + STRING_HAND_ITEM_NBT_WHITELIST;
    public static final String STRING_HAND_ITEM_NBT_BLACKLIST = "blacklist";
    public static final String STRING_HAND_ITEM_NBT_BLACKLIST_FULL = STRING_HAND_ITEM_NBT_FULL + SEPARATOR + STRING_HAND_ITEM_NBT_BLACKLIST;
    public static final String STRING_HAND_ITEM_DAMAGEABLE = "damageable";
    public static final String STRING_HAND_ITEM_DAMAGEABLE_FULL = STRING_HAND + SEPARATOR + STRING_HAND_ITEM_DAMAGEABLE;
    public static final String STRING_HAND_ITEM_CONSUMABLE = "consumable";
    public static final String STRING_HAND_ITEM_CONSUMABLE_FULL = STRING_HAND + SEPARATOR + STRING_HAND_ITEM_CONSUMABLE;
    public static final String STRING_HAND_ITEM_CHANCE = "chance";
    public static final String STRING_HAND_ITEM_CHANCE_FULL = STRING_HAND + SEPARATOR + STRING_HAND_ITEM_CHANCE;

    // Block
    public static final String STRING_BLOCK_BLOCK = "block";
    public static final String STRING_BLOCK_BLOCK_FULL = STRING_BLOCK + SEPARATOR + STRING_BLOCK_BLOCK;
    public static final String STRING_BLOCK_FLUID = "fluid";
    public static final String STRING_BLOCK_FLUID_FULL = STRING_BLOCK + SEPARATOR + STRING_BLOCK_FLUID;
    public static final String STRING_BLOCK_TAG = "tag";
    public static final String STRING_BLOCK_TAG_FULL = STRING_BLOCK + SEPARATOR + STRING_BLOCK_TAG;
    public static final String STRING_BLOCK_NBT = "nbt";
    public static final String STRING_BLOCK_NBT_FULL = STRING_BLOCK + SEPARATOR + STRING_BLOCK_NBT;
    public static final String STRING_BLOCK_NBT_WHITELIST = "whitelist";
    public static final String STRING_BLOCK_NBT_WHITELIST_FULL = STRING_BLOCK_NBT_FULL + SEPARATOR + STRING_BLOCK_NBT_WHITELIST;
    public static final String STRING_BLOCK_NBT_BLACKLIST = "blacklist";
    public static final String STRING_BLOCK_NBT_BLACKLIST_FULL = STRING_BLOCK_NBT_FULL + SEPARATOR + STRING_BLOCK_NBT_BLACKLIST;
    public static final String STRING_BLOCK_STATE = "state";
    public static final String STRING_BLOCK_STATE_FULL = STRING_BLOCK + SEPARATOR + STRING_BLOCK_STATE;
    public static final String STRING_BLOCK_STATE_WHITELIST = "whitelist";
    public static final String STRING_BLOCK_STATE_WHITELIST_FULL = STRING_BLOCK_STATE_FULL + SEPARATOR + STRING_BLOCK_STATE_WHITELIST;
    public static final String STRING_BLOCK_STATE_BLACKLIST = "blacklist";
    public static final String STRING_BLOCK_STATE_BLACKLIST_FULL = STRING_BLOCK_STATE_FULL + SEPARATOR + STRING_BLOCK_STATE_BLACKLIST;

    // Transformation
    public static final String STRING_TRANSFORMATION_CHANCE = "chance";
    public static final String STRING_TRANSFORMATION_CHANCE_FULL = STRING_TRANSFORMATION + SEPARATOR + STRING_TRANSFORMATION_CHANCE;
    public static final String STRING_TRANSFORMATION_BLOCK = "block";
    public static final String STRING_TRANSFORMATION_BLOCK_FULL = STRING_TRANSFORMATION + SEPARATOR + STRING_TRANSFORMATION_BLOCK;
    public static final String STRING_TRANSFORMATION_FLUID = "fluid";
    public static final String STRING_TRANSFORMATION_FLUID_FULL = STRING_TRANSFORMATION + SEPARATOR + STRING_TRANSFORMATION_FLUID;
    public static final String STRING_TRANSFORMATION_NBT = "fluid";
    public static final String STRING_TRANSFORMATION_NBT_FULL = STRING_TRANSFORMATION + SEPARATOR + STRING_TRANSFORMATION_NBT;
    public static final String STRING_TRANSFORMATION_STATE = "state";
    public static final String STRING_TRANSFORMATION_STATE_FULL = STRING_TRANSFORMATION + SEPARATOR + STRING_TRANSFORMATION_STATE;
    public static final String STRING_TRANSFORMATION_SOUND = "sound";
    public static final String STRING_TRANSFORMATION_SOUND_FULL = STRING_TRANSFORMATION + SEPARATOR + STRING_TRANSFORMATION_SOUND;
    public static final String STRING_TRANSFORMATION_PARTICLES = "particles";
    public static final String STRING_TRANSFORMATION_PARTICLES_FULL = STRING_TRANSFORMATION + SEPARATOR + STRING_TRANSFORMATION_PARTICLES;

    // Pool
    public static final String STRING_POOL_ENTRY = "[]";
    public static final String STRING_POOL_ENTRY_FULL = STRING_POOL + SEPARATOR + STRING_POOL_ENTRY;
    public static final String STRING_POOL_ITEM = "item";
    public static final String STRING_POOL_ITEM_FULL = STRING_POOL_ENTRY_FULL + SEPARATOR + STRING_POOL_ITEM;
    public static final String STRING_POOL_NBT = "nbt";
    public static final String STRING_POOL_NBT_FULL = STRING_POOL_ENTRY_FULL + SEPARATOR + STRING_POOL_NBT;
    public static final String STRING_POOL_TAG = "tag";
    public static final String STRING_POOL_TAG_FULL = STRING_POOL_ENTRY_FULL + SEPARATOR + STRING_POOL_TAG;
    public static final String STRING_POOL_COUNT = "count";
    public static final String STRING_POOL_COUNT_FULL = STRING_POOL_ENTRY_FULL + SEPARATOR + STRING_POOL_COUNT;
    public static final String STRING_POOL_MIN = "min";
    public static final String STRING_POOL_MIN_FULL = STRING_POOL_ENTRY_FULL + SEPARATOR + STRING_POOL_MIN;
    public static final String STRING_POOL_MAX = "max";
    public static final String STRING_POOL_MAX_FULL = STRING_POOL_ENTRY_FULL + SEPARATOR + STRING_POOL_MAX;
    public static final String STRING_POOL_CHANCE = "chance";
    public static final String STRING_POOL_CHANCE_FULL = STRING_POOL_ENTRY_FULL + SEPARATOR + STRING_POOL_CHANCE;

    // BIOME
    public static final String STRING_BIOME_WHITELIST = "whitelist";
    public static final String STRING_BIOME_WHITELIST_FULL = STRING_BIOME + SEPARATOR + STRING_BIOME_WHITELIST;
    public static final String STRING_BIOME_BLACKLIST = "blacklist";
    public static final String STRING_BIOME_BLACKLIST_FULL = STRING_BIOME + SEPARATOR + STRING_BIOME_BLACKLIST;
}
