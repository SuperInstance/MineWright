package com.minewright.llm;

import com.minewright.entity.ForemanEntity;
import com.minewright.memory.WorldKnowledge;
import com.minewright.security.InputSanitizer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Builds prompts for LLM interactions with performance optimizations.
 * Phase 2: Caches static system prompt and optimizes StringBuilder usage.
 */
public class PromptBuilder {

    // Phase 2 optimization: Cache the system prompt since it never changes
    private static final String CACHED_SYSTEM_PROMPT = buildSystemPromptInternal();

    /**
     * Returns the cached system prompt.
     * Phase 2 optimization: Static prompt is cached to avoid repeated string construction.
     */
    public static String buildSystemPrompt() {
        return CACHED_SYSTEM_PROMPT;
    }

    /**
     * Internal method to build the system prompt (called once at class initialization).
     */
    private static String buildSystemPromptInternal() {
        return """
            You are a Minecraft AI. Respond ONLY with valid JSON.

            FORMAT: {"reasoning":"brief","plan":"action","tasks":[{"action":"type","parameters":{}}]}

            ACTIONS:
            - attack:{"target":"hostile"|specific_mob}
            - build:{"structure":"house|oldhouse|powerplant|castle|tower|barn|modern","blocks":["block1","block2"],"dimensions":[x,y,z]}
            - mine:{"block":"iron|diamond|coal|gold|copper|redstone|emerald","quantity":N}
            - follow:{"player":"NAME"}
            - pathfind:{"x":0,"y":0,"z":0}

            BLOCKS (use EXACT names):
            oak/spruce/birch/jungle/acacia/dark_oak/mangrove/cherry: log, planks, stairs, slab, fence
            stone variants: stone, cobblestone, stone_bricks, mossy_cobblestone, mossy/cracked/chiseled_stone_bricks
            ore variants: coal/iron/copper/gold/diamond/emerald/redstone/lapis_ore, deepslate_*
            terrain: dirt, grass_block, gravel, sand, red_sand, sandstone, red_sandstone, smooth_sandstone
            build: glass, glass_pane, bricks, brick_stairs, brick_slab
            wool: white/light_gray/gray/black/red/orange/yellow/lime/green/cyan/light_blue/blue/purple/magenta/pink/brown_wool
            terracotta: white/light_gray/gray/black/red/orange/yellow_terracotta
            decor: lantern, soul_lantern, torch, soul_torch, iron_bars, chain, gold/iron/diamond/emerald_block
            fluids: water, lava
            nether: netherrack, nether_bricks, crimson/warped_nylium, blackstone, basalt
            end: end_stone, purpur_block, purpur_pillar

            RULES:
            1. house/oldhouse/powerplant=auto-size; castle(14x10x14),tower(6x6x16),barn(12x8x14),modern
            2. Use 2-3 specific block types (NEVER generic "wood"/"stone"/"ore")
            3. No extra pathfind unless requested
            4. Reasoning <10 words
            5. Collaborative building: multiple foremen can work together

            EXAMPLES:
            "build a house" -> {"reasoning":"Building house","plan":"Construct","tasks":[{"action":"build","parameters":{"structure":"house","blocks":["oak_planks","cobblestone","glass_pane"],"dimensions":[9,6,9]}}]}
            "get iron" -> {"reasoning":"Mining iron","plan":"Mine","tasks":[{"action":"mine","parameters":{"block":"iron","quantity":16}}]}
            "kill mobs" -> {"reasoning":"Attacking hostiles","plan":"Attack","tasks":[{"action":"attack","parameters":{"target":"hostile"}}]}

            OUTPUT ONLY VALID JSON. No markdown or explanations.
            """;
    }

    public static String buildUserPrompt(ForemanEntity foreman, String command, WorldKnowledge worldKnowledge) {
        // SECURITY: Sanitize user command to prevent prompt injection attacks
        String sanitizedCommand = InputSanitizer.forCommand(command);

        // Phase 2 optimization: Increased capacity to reduce allocations
        StringBuilder prompt = new StringBuilder(384);

        // Compact situation report - only relevant info
        prompt.append("POS:[");

        // Phase 2 optimization: Inline position formatting to avoid String.format() overhead
        BlockPos pos = foreman.blockPosition();
        prompt.append(pos.getX()).append(',').append(pos.getY()).append(',').append(pos.getZ()).append(']');

        String players = worldKnowledge.getNearbyPlayerNames();
        if (!"none".equals(players)) {
            prompt.append(" | PLAYERS:").append(players);
        }

        String entities = worldKnowledge.getNearbyEntitiesSummary();
        if (!"none".equals(entities)) {
            prompt.append(" | ENTITIES:").append(entities);
        }

        String blocks = worldKnowledge.getNearbyBlocksSummary();
        if (!"none".equals(blocks)) {
            prompt.append(" | BLOCKS:").append(blocks);
        }

        prompt.append(" | BIOME:").append(worldKnowledge.getBiomeName());
        prompt.append("\nCMD:\"").append(sanitizedCommand).append("\"");

        return prompt.toString();
    }

    private static String formatInventory(ForemanEntity foreman) {
        return "[empty]";
    }

    // Token estimation for monitoring
    public static int estimateSystemPromptTokens() {
        // Approximate: 1 token ≈ 4 characters for code-like text
        return buildSystemPrompt().length() / 4;
    }

    public static int estimateUserPromptTokens(ForemanEntity foreman, String command, WorldKnowledge worldKnowledge) {
        return buildUserPrompt(foreman, command, worldKnowledge).length() / 4;
    }
}
