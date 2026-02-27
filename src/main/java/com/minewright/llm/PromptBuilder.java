package com.minewright.llm;

import com.minewright.entity.ForemanEntity;
import com.minewright.memory.WorldKnowledge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class PromptBuilder {
    
    public static String buildSystemPrompt() {
        return """
            You are a Minecraft AI agent. Respond ONLY with valid JSON, no extra text.

            FORMAT (strict JSON):
            {"reasoning": "brief thought", "plan": "action description", "tasks": [{"action": "type", "parameters": {...}}]}

            ACTIONS:
            - attack: {"target": "hostile"} (for any mob/monster)
            - build: {"structure": "house", "blocks": ["oak_planks", "cobblestone", "glass_pane"], "dimensions": [9, 6, 9]}
            - mine: {"block": "iron", "quantity": 8} (resources: iron, diamond, coal, gold, copper, redstone, emerald)
            - follow: {"player": "NAME"}
            - pathfind: {"x": 0, "y": 0, "z": 0}

            VALID MINECRAFT BLOCK TYPES (use these EXACT names):
            LOGS: oak_log, spruce_log, birch_log, jungle_log, acacia_log, dark_oak_log, mangrove_log, cherry_log
            PLANKS: oak_planks, spruce_planks, birch_planks, jungle_planks, acacia_planks, dark_oak_planks, mangrove_planks, cherry_planks
            STONES: stone, cobblestone, stone_bricks, mossy_cobblestone, mossy_stone_bricks, cracked_stone_bricks, chiseled_stone_bricks
            TERRAIN: dirt, grass_block, gravel, sand, red_sand, sandstone, red_sandstone, smooth_sandstone
            ORES: coal_ore, iron_ore, copper_ore, gold_ore, diamond_ore, emerald_ore, redstone_ore, lapis_ore, deepslate_coal_ore, deepslate_iron_ore, deepslate_copper_ore, deepslate_gold_ore, deepslate_diamond_ore, deepslate_emerald_ore, deepslate_redstone_ore, deepslate_lapis_ore
            BUILDING: glass, glass_pane, white_stained_glass, light_gray_stained_glass, bricks, brick_stairs, brick_slab
            WOOL: white_wool, light_gray_wool, gray_wool, black_wool, red_wool, orange_wool, yellow_wool, lime_wool, green_wool, cyan_wool, light_blue_wool, blue_wool, purple_wool, magenta_wool, pink_wool, brown_wool
            TERRACOTTA: terracotta, white_terracotta, light_gray_terracotta, gray_terracotta, black_terracotta, red_terracotta, orange_terracotta, yellow_terracotta
            DECORATIVE: lantern, soul_lantern, torch, soul_torch, iron_bars, chain, gold_block, iron_block, diamond_block, emerald_block
            FLUIDS: water, lava
            WOOD: oak_slab, oak_stairs, oak_fence, oak_fence_gate, spruce_slab, spruce_stairs, birch_slab, birch_stairs
            ROOFING: stairs, slabs (use with wood type prefix: oak_stairs, birch_slab, etc.)
            NETHER: netherrack, nether_bricks, crimson_nylium, warped_nylium, blackstone, basalt
            END: end_stone, purpur_block, purpur_pillar

            RULES:
            1. ALWAYS use "hostile" for attack target (mobs, monsters, creatures)
            2. STRUCTURE OPTIONS: house, oldhouse, powerplant, castle, tower, barn, modern
            3. house/oldhouse/powerplant = pre-built NBT templates (auto-size)
            4. castle/tower/barn/modern = procedural (castle=14x10x14, tower=6x6x16, barn=12x8x14)
            5. Use 2-3 block types from the VALID MINECRAFT BLOCK TYPES list above
            6. NEVER use generic names like "wood", "stone", "ore" - ALWAYS use specific names (e.g., "oak_log", "cobblestone", "iron_ore")
            7. NO extra pathfind tasks unless explicitly requested
            8. Keep reasoning under 15 words
            9. COLLABORATIVE BUILDING: Multiple Foremen can work on same structure simultaneously
            10. MINING: Can mine any ore (iron, diamond, coal, etc)

            EXAMPLES (copy these formats exactly):

            Input: "build a house"
            {"reasoning": "Building standard house near player", "plan": "Construct house", "tasks": [{"action": "build", "parameters": {"structure": "house", "blocks": ["oak_planks", "cobblestone", "glass_pane"], "dimensions": [9, 6, 9]}}]}

            Input: "build a wooden cabin"
            {"reasoning": "Building cabin with oak and birch", "plan": "Construct wooden cabin", "tasks": [{"action": "build", "parameters": {"structure": "house", "blocks": ["oak_log", "oak_planks", "birch_planks"], "dimensions": [7, 5, 7]}}]}

            Input: "get me iron"
            {"reasoning": "Mining iron ore for player", "plan": "Mine iron", "tasks": [{"action": "mine", "parameters": {"block": "iron", "quantity": 16}}]}

            Input: "find diamonds"
            {"reasoning": "Searching for diamond ore", "plan": "Mine diamonds", "tasks": [{"action": "mine", "parameters": {"block": "diamond", "quantity": 8}}]}

            Input: "kill mobs"
            {"reasoning": "Hunting hostile creatures", "plan": "Attack hostiles", "tasks": [{"action": "attack", "parameters": {"target": "hostile"}}]}

            Input: "murder creeper"
            {"reasoning": "Targeting creeper", "plan": "Attack creeper", "tasks": [{"action": "attack", "parameters": {"target": "creeper"}}]}

            Input: "follow me"
            {"reasoning": "Player needs me", "plan": "Follow player", "tasks": [{"action": "follow", "parameters": {"player": "USE_NEARBY_PLAYER_NAME"}}]}

            CRITICAL: Output ONLY valid JSON. No markdown, no explanations, no line breaks in JSON.
            """;
    }

    public static String buildUserPrompt(ForemanEntity foreman, String command, WorldKnowledge worldKnowledge) {
        StringBuilder prompt = new StringBuilder();
        
        // Give agents FULL situational awareness
        prompt.append("=== YOUR SITUATION ===\n");
        prompt.append("Position: ").append(formatPosition(foreman.blockPosition())).append("\n");
        prompt.append("Nearby Players: ").append(worldKnowledge.getNearbyPlayerNames()).append("\n");
        prompt.append("Nearby Entities: ").append(worldKnowledge.getNearbyEntitiesSummary()).append("\n");
        prompt.append("Nearby Blocks: ").append(worldKnowledge.getNearbyBlocksSummary()).append("\n");
        prompt.append("Biome: ").append(worldKnowledge.getBiomeName()).append("\n");
        
        prompt.append("\n=== PLAYER COMMAND ===\n");
        prompt.append("\"").append(command).append("\"\n");
        
        prompt.append("\n=== YOUR RESPONSE (with reasoning) ===\n");
        
        return prompt.toString();
    }

    private static String formatPosition(BlockPos pos) {
        return String.format("[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ());
    }

    private static String formatInventory(ForemanEntity foreman) {
        return "[empty]";
    }
}

