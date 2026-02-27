package com.minewright.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for normalizing common block name aliases to valid Minecraft registry names.
 * Handles common names that LLMs might return that don't match Minecraft's block registry.
 */
public class BlockNameMapper {

    private static final Map<String, String> ALIAS_MAP = new HashMap<>();

    static {
        // Wood types and logs
        ALIAS_MAP.put("wood", "oak_log");
        ALIAS_MAP.put("log", "oak_log");
        ALIAS_MAP.put("oak_wood", "oak_log");
        ALIAS_MAP.put("birch_wood", "birch_log");
        ALIAS_MAP.put("spruce_wood", "spruce_log");
        ALIAS_MAP.put("jungle_wood", "jungle_log");
        ALIAS_MAP.put("acacia_wood", "acacia_log");
        ALIAS_MAP.put("dark_oak_wood", "dark_oak_log");
        ALIAS_MAP.put("mangrove_wood", "mangrove_log");
        ALIAS_MAP.put("cherry_wood", "cherry_log");
        ALIAS_MAP.put("crimson_wood", "crimson_stem");
        ALIAS_MAP.put("warped_wood", "warped_stem");

        // Planks
        ALIAS_MAP.put("plank", "oak_planks");
        ALIAS_MAP.put("planks", "oak_planks");
        ALIAS_MAP.put("oak_plank", "oak_planks");
        ALIAS_MAP.put("birch_plank", "birch_planks");
        ALIAS_MAP.put("spruce_plank", "spruce_planks");
        ALIAS_MAP.put("jungle_plank", "jungle_planks");
        ALIAS_MAP.put("acacia_plank", "acacia_planks");
        ALIAS_MAP.put("dark_oak_plank", "dark_oak_planks");
        ALIAS_MAP.put("mangrove_plank", "mangrove_planks");
        ALIAS_MAP.put("cherry_plank", "cherry_planks");
        ALIAS_MAP.put("bamboo_plank", "bamboo_planks");
        ALIAS_MAP.put("crimson_plank", "crimson_planks");
        ALIAS_MAP.put("warped_plank", "warped_planks");

        // Stone variants
        ALIAS_MAP.put("cobble", "cobblestone");
        ALIAS_MAP.put("cobblestone_wall", "cobblestone_wall");
        ALIAS_MAP.put("stone_bricks", "stone_bricks");
        ALIAS_MAP.put("stone_brick", "stone_bricks");
        ALIAS_MAP.put("smooth_stone", "smooth_stone");
        ALIAS_MAP.put("polished_andesite", "polished_andesite");
        ALIAS_MAP.put("andesite", "andesite");
        ALIAS_MAP.put("granite", "granite");
        ALIAS_MAP.put("diorite", "diorite");
        ALIAS_MAP.put("polished_granite", "polished_granite");
        ALIAS_MAP.put("polished_diorite", "polished_diorite");

        // Ores
        ALIAS_MAP.put("iron", "iron_ore");
        ALIAS_MAP.put("iron_ore_block", "iron_ore");
        ALIAS_MAP.put("coal", "coal_ore");
        ALIAS_MAP.put("coal_ore_block", "coal_ore");
        ALIAS_MAP.put("diamond", "diamond_ore");
        ALIAS_MAP.put("diamond_ore_block", "diamond_ore");
        ALIAS_MAP.put("gold", "gold_ore");
        ALIAS_MAP.put("gold_ore_block", "gold_ore");
        ALIAS_MAP.put("copper", "copper_ore");
        ALIAS_MAP.put("copper_ore_block", "copper_ore");
        ALIAS_MAP.put("redstone", "redstone_ore");
        ALIAS_MAP.put("redstone_ore_block", "redstone_ore");
        ALIAS_MAP.put("lapis", "lapis_ore");
        ALIAS_MAP.put("lapis_lazuli", "lapis_ore");
        ALIAS_MAP.put("lapis_ore_block", "lapis_ore");
        ALIAS_MAP.put("emerald", "emerald_ore");
        ALIAS_MAP.put("emerald_ore_block", "emerald_ore");

        // Deepslate ores
        ALIAS_MAP.put("deepslate_iron", "deepslate_iron_ore");
        ALIAS_MAP.put("deepslate_coal", "deepslate_coal_ore");
        ALIAS_MAP.put("deepslate_diamond", "deepslate_diamond_ore");
        ALIAS_MAP.put("deepslate_gold", "deepslate_gold_ore");
        ALIAS_MAP.put("deepslate_copper", "deepslate_copper_ore");
        ALIAS_MAP.put("deepslate_redstone", "deepslate_redstone_ore");
        ALIAS_MAP.put("deepslate_lapis", "deepslate_lapis_ore");
        ALIAS_MAP.put("deepslate_emerald", "deepslate_emerald_ore");

        // Dirt and soil
        ALIAS_MAP.put("dirt", "dirt");
        ALIAS_MAP.put("grass", "grass_block");
        ALIAS_MAP.put("grass_block", "grass_block");
        ALIAS_MAP.put("farmland", "farmland");
        ALIAS_MAP.put("podzol", "podzol");
        ALIAS_MAP.put("coarse_dirt", "coarse_dirt");
        ALIAS_MAP.put("mycelium", "mycelium");

        // Sand variants
        ALIAS_MAP.put("sand", "sand");
        ALIAS_MAP.put("red_sand", "red_sand");
        ALIAS_MAP.put("gravel", "gravel");

        // Glass
        ALIAS_MAP.put("glass", "glass");
        ALIAS_MAP.put("glass_pane", "glass_pane");

        // Building materials
        ALIAS_MAP.put("brick", "bricks");
        ALIAS_MAP.put("bricks", "bricks");
        ALIAS_MAP.put("brick_block", "bricks");
        ALIAS_MAP.put("clay", "clay");
        ALIAS_MAP.put("terracotta", "terracotta");

        // Wool colors
        ALIAS_MAP.put("wool", "white_wool");
        ALIAS_MAP.put("white_wool", "white_wool");
        ALIAS_MAP.put("black_wool", "black_wool");
        ALIAS_MAP.put("red_wool", "red_wool");
        ALIAS_MAP.put("blue_wool", "blue_wool");
        ALIAS_MAP.put("green_wool", "green_wool");
        ALIAS_MAP.put("yellow_wool", "yellow_wool");
        ALIAS_MAP.put("orange_wool", "orange_wool");
        ALIAS_MAP.put("purple_wool", "purple_wool");
        ALIAS_MAP.put("cyan_wool", "cyan_wool");
        ALIAS_MAP.put("magenta_wool", "magenta_wool");
        ALIAS_MAP.put("light_blue_wool", "light_blue_wool");
        ALIAS_MAP.put("light_gray_wool", "light_gray_wool");
        ALIAS_MAP.put("lime_wool", "lime_wool");
        ALIAS_MAP.put("pink_wool", "pink_wool");
        ALIAS_MAP.put("gray_wool", "gray_wool");
        ALIAS_MAP.put("brown_wool", "brown_wool");

        // Concrete
        ALIAS_MAP.put("concrete", "white_concrete");
        ALIAS_MAP.put("white_concrete", "white_concrete");
        ALIAS_MAP.put("black_concrete", "black_concrete");
        ALIAS_MAP.put("red_concrete", "red_concrete");
        ALIAS_MAP.put("blue_concrete", "blue_concrete");
        ALIAS_MAP.put("green_concrete", "green_concrete");
        ALIAS_MAP.put("yellow_concrete", "yellow_concrete");
        ALIAS_MAP.put("orange_concrete", "orange_concrete");
        ALIAS_MAP.put("purple_concrete", "purple_concrete");
        ALIAS_MAP.put("cyan_concrete", "cyan_concrete");
        ALIAS_MAP.put("magenta_concrete", "magenta_concrete");
        ALIAS_MAP.put("light_blue_concrete", "light_blue_concrete");
        ALIAS_MAP.put("light_gray_concrete", "light_gray_concrete");
        ALIAS_MAP.put("lime_concrete", "lime_concrete");
        ALIAS_MAP.put("pink_concrete", "pink_concrete");
        ALIAS_MAP.put("gray_concrete", "gray_concrete");
        ALIAS_MAP.put("brown_concrete", "brown_concrete");

        // Metals and storage blocks
        ALIAS_MAP.put("iron_block", "iron_block");
        ALIAS_MAP.put("gold_block", "gold_block");
        ALIAS_MAP.put("copper_block", "copper_block");
        ALIAS_MAP.put("diamond_block", "diamond_block");
        ALIAS_MAP.put("emerald_block", "emerald_block");
        ALIAS_MAP.put("lapis_block", "lapis_block");
        ALIAS_MAP.put("redstone_block", "redstone_block");
        ALIAS_MAP.put("coal_block", "coal_block");

        // Nether blocks
        ALIAS_MAP.put("netherrack", "netherrack");
        ALIAS_MAP.put("nether_bricks", "nether_bricks");
        ALIAS_MAP.put("nether_brick", "nether_bricks");
        ALIAS_MAP.put("crimson_nylium", "crimson_nylium");
        ALIAS_MAP.put("warped_nylium", "warped_nylium");
        ALIAS_MAP.put("basalt", "basalt");
        ALIAS_MAP.put("blackstone", "blackstone");

        // End blocks
        ALIAS_MAP.put("end_stone", "end_stone");
        ALIAS_MAP.put("purpur_block", "purpur_block");
        ALIAS_MAP.put("purpur", "purpur_block");
        ALIAS_MAP.put("obsidian", "obsidian");
        ALIAS_MAP.put("crying_obsidian", "crying_obsidian");
        ALIAS_MAP.put("ender_chest", "ender_chest");

        // Leaves
        ALIAS_MAP.put("leaves", "oak_leaves");
        ALIAS_MAP.put("oak_leaves", "oak_leaves");
        ALIAS_MAP.put("birch_leaves", "birch_leaves");
        ALIAS_MAP.put("spruce_leaves", "spruce_leaves");
        ALIAS_MAP.put("jungle_leaves", "jungle_leaves");
        ALIAS_MAP.put("acacia_leaves", "acacia_leaves");
        ALIAS_MAP.put("dark_oak_leaves", "dark_oak_leaves");
        ALIAS_MAP.put("mangrove_leaves", "mangrove_leaves");
        ALIAS_MAP.put("cherry_leaves", "cherry_leaves");
        ALIAS_MAP.put("azalea_leaves", "azalea_leaves");
        ALIAS_MAP.put("flowering_azalea_leaves", "flowering_azalea_leaves");

        // Saplings
        ALIAS_MAP.put("sapling", "oak_sapling");
        ALIAS_MAP.put("oak_sapling", "oak_sapling");
        ALIAS_MAP.put("birch_sapling", "birch_sapling");
        ALIAS_MAP.put("spruce_sapling", "spruce_sapling");
        ALIAS_MAP.put("jungle_sapling", "jungle_sapling");
        ALIAS_MAP.put("acacia_sapling", "acacia_sapling");
        ALIAS_MAP.put("dark_oak_sapling", "dark_oak_sapling");
        ALIAS_MAP.put("mangrove_sapling", "mangrove_propagule");
        ALIAS_MAP.put("cherry_sapling", "cherry_sapling");

        // Flowers and plants
        ALIAS_MAP.put("flower", "poppy");
        ALIAS_MAP.put("red_flower", "poppy");
        ALIAS_MAP.put("yellow_flower", "dandelion");
        ALIAS_MAP.put("tall_grass", "tall_grass");
        ALIAS_MAP.put("grass_plant", "grass");
        ALIAS_MAP.put("fern", "fern");
        ALIAS_MAP.put("lily_pad", "lily_pad");
        ALIAS_MAP.put("vine", "vines");
        ALIAS_MAP.put("vines", "vines");

        // Crops
        ALIAS_MAP.put("wheat", "wheat");
        ALIAS_MAP.put("carrot", "carrots");
        ALIAS_MAP.put("carrots", "carrots");
        ALIAS_MAP.put("potato", "potatoes");
        ALIAS_MAP.put("potatoes", "potatoes");
        ALIAS_MAP.put("beetroot", "beetroots");
        ALIAS_MAP.put("sugar_cane", "sugar_cane");
        ALIAS_MAP.put("pumpkin", "pumpkin");
        ALIAS_MAP.put("melon", "melon");
        ALIAS_MAP.put("cactus", "cactus");
        ALIAS_MAP.put("bamboo", "bamboo");

        // Special blocks
        ALIAS_MAP.put("bookshelf", "bookshelf");
        ALIAS_MAP.put("crafting_table", "crafting_table");
        ALIAS_MAP.put("workbench", "crafting_table");
        ALIAS_MAP.put("furnace", "furnace");
        ALIAS_MAP.put("chest", "chest");
        ALIAS_MAP.put("barrel", "barrel");
        ALIAS_MAP.put("bed", "red_bed");
        ALIAS_MAP.put("torch", "torch");
        ALIAS_MAP.put("lantern", "lantern");
        ALIAS_MAP.put("soul_lantern", "soul_lantern");
        ALIAS_MAP.put("glowstone", "glowstone");
        ALIAS_MAP.put("sea_lantern", "sea_lantern");
        ALIAS_MAP.put("jack_o_lantern", "jack_o_lantern");

        // Slabs and stairs (common variants)
        ALIAS_MAP.put("slab", "oak_slab");
        ALIAS_MAP.put("stairs", "oak_stairs");
        ALIAS_MAP.put("stone_slab", "stone_slab");
        ALIAS_MAP.put("stone_stairs", "stone_stairs");
        ALIAS_MAP.put("cobblestone_slab", "cobblestone_slab");
        ALIAS_MAP.put("cobblestone_stairs", "cobblestone_stairs");
        ALIAS_MAP.put("brick_slab", "brick_slab");
        ALIAS_MAP.put("brick_stairs", "brick_stairs");

        // Fence variants
        ALIAS_MAP.put("fence", "oak_fence");
        ALIAS_MAP.put("fence_gate", "oak_fence_gate");
        ALIAS_MAP.put("wood_fence", "oak_fence");

        // Door variants
        ALIAS_MAP.put("door", "oak_door");
        ALIAS_MAP.put("wood_door", "oak_door");

        // Trapdoor variants
        ALIAS_MAP.put("trapdoor", "oak_trapdoor");
        ALIAS_MAP.put("hatch", "oak_trapdoor");

        // Pressure plates and buttons
        ALIAS_MAP.put("pressure_plate", "stone_pressure_plate");
        ALIAS_MAP.put("button", "stone_button");

        // Rails
        ALIAS_MAP.put("rail", "rail");
        ALIAS_MAP.put("rails", "rail");
        ALIAS_MAP.put("powered_rail", "powered_rail");
        ALIAS_MAP.put("detector_rail", "detector_rail");
        ALIAS_MAP.put("activator_rail", "activator_rail");

        // Redstone components
        ALIAS_MAP.put("redstone_wire", "redstone_wire");
        ALIAS_MAP.put("repeater", "repeater");
        ALIAS_MAP.put("comparator", "comparator");
        ALIAS_MAP.put("piston", "piston");
        ALIAS_MAP.put("sticky_piston", "sticky_piston");
        ALIAS_MAP.put("observer", "observer");
        ALIAS_MAP.put("hopper", "hopper");
        ALIAS_MAP.put("dropper", "dropper");
        ALIAS_MAP.put("dispenser", "dispenser");

        // Snow and ice
        ALIAS_MAP.put("snow", "snow_block");
        ALIAS_MAP.put("snow_block", "snow_block");
        ALIAS_MAP.put("ice", "ice");
        ALIAS_MAP.put("packed_ice", "packed_ice");
        ALIAS_MAP.put("blue_ice", "blue_ice");

        // Misc
        ALIAS_MAP.put("sponge", "sponge");
        ALIAS_MAP.put("wet_sponge", "wet_sponge");
        ALIAS_MAP.put("slime_block", "slime_block");
        ALIAS_MAP.put("honey_block", "honey_block");
        ALIAS_MAP.put("bone_block", "bone_block");
        ALIAS_MAP.put("prismarine", "prismarine");
        ALIAS_MAP.put("sea_pickle", "sea_pickle");
        ALIAS_MAP.put("turtle_egg", "turtle_egg");
        ALIAS_MAP.put("sniffer_egg", "sniffer_egg");
    }

    /**
     * Normalizes a block name alias to a valid Minecraft registry name.
     * This method is case-insensitive and handles spaces by converting them to underscores.
     *
     * @param blockName The block name to normalize (can be alias, with spaces, mixed case, etc.)
     * @return The normalized Minecraft registry name, or the original name if no mapping is found
     */
    public static String normalize(String blockName) {
        if (blockName == null || blockName.isEmpty()) {
            return blockName;
        }

        // Normalize input: lowercase and replace spaces with underscores
        String normalized = blockName.toLowerCase().trim().replace(" ", "_");

        // Look up in alias map
        String mapped = ALIAS_MAP.get(normalized);
        if (mapped != null) {
            return mapped;
        }

        // Return original normalized name if no mapping found
        return normalized;
    }

    /**
     * Checks if a block name is a known alias that needs mapping.
     *
     * @param blockName The block name to check
     * @return true if the name is a known alias, false otherwise
     */
    public static boolean isKnownAlias(String blockName) {
        if (blockName == null || blockName.isEmpty()) {
            return false;
        }

        String normalized = blockName.toLowerCase().trim().replace(" ", "_");
        return ALIAS_MAP.containsKey(normalized);
    }

    /**
     * Adds a custom alias mapping at runtime.
     * Useful for plugins or dynamic configurations.
     *
     * @param alias The alias to map from
     * @param registryName The Minecraft registry name to map to
     */
    public static void addAlias(String alias, String registryName) {
        if (alias != null && registryName != null) {
            ALIAS_MAP.put(alias.toLowerCase().trim().replace(" ", "_"), registryName);
        }
    }

    /**
     * Gets the total number of registered aliases.
     * Useful for debugging and statistics.
     *
     * @return The number of registered aliases
     */
    public static int getAliasCount() {
        return ALIAS_MAP.size();
    }
}
