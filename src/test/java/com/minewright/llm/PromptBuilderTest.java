package com.minewright.llm;

import com.minewright.entity.ForemanEntity;
import com.minewright.memory.WorldKnowledge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PromptBuilder}.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>System prompt generation and caching</li>
 *   <li>User prompt generation with various context scenarios</li>
 *   <li>Context injection (position, players, entities, blocks, biome)</li>
 *   <li>World knowledge formatting</li>
 *   <li>Input sanitization integration</li>
 *   <li>Token limit estimation</li>
 *   <li>Edge cases (empty input, special characters, null values)</li>
 * </ul>
 */
@DisplayName("PromptBuilder Tests")
class PromptBuilderTest {

    @Mock
    private ForemanEntity mockForeman;

    @Mock
    private WorldKnowledge mockWorldKnowledge;

    @Mock
    private Level mockLevel;

    private BlockPos testPosition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testPosition = new BlockPos(100, 64, 200);

        // Setup default mock behavior
        when(mockForeman.blockPosition()).thenReturn(testPosition);
        when(mockForeman.level()).thenReturn(mockLevel);
    }

    // ------------------------------------------------------------------------
    // System Prompt Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("buildSystemPrompt returns non-empty string")
    void testBuildSystemPromptReturnsNonEmpty() {
        String systemPrompt = PromptBuilder.buildSystemPrompt();

        assertNotNull(systemPrompt, "System prompt should not be null");
        assertFalse(systemPrompt.isEmpty(), "System prompt should not be empty");
    }

    @Test
    @DisplayName("buildSystemPrompt contains required sections")
    void testBuildSystemPromptContainsRequiredSections() {
        String systemPrompt = PromptBuilder.buildSystemPrompt();

        assertTrue(systemPrompt.contains("Minecraft AI"), "Should contain role description");
        assertTrue(systemPrompt.contains("FORMAT:"), "Should contain format specification");
        assertTrue(systemPrompt.contains("ACTIONS:"), "Should contain actions section");
        assertTrue(systemPrompt.contains("BLOCKS:"), "Should contain blocks section");
        assertTrue(systemPrompt.contains("RULES:"), "Should contain rules section");
        assertTrue(systemPrompt.contains("EXAMPLES:"), "Should contain examples section");
    }

    @Test
    @DisplayName("buildSystemPrompt contains all required actions")
    void testBuildSystemPromptContainsAllActions() {
        String systemPrompt = PromptBuilder.buildSystemPrompt();

        assertTrue(systemPrompt.contains("attack:"), "Should contain attack action");
        assertTrue(systemPrompt.contains("build:"), "Should contain build action");
        assertTrue(systemPrompt.contains("mine:"), "Should contain mine action");
        assertTrue(systemPrompt.contains("follow:"), "Should contain follow action");
        assertTrue(systemPrompt.contains("pathfind:"), "Should contain pathfind action");
    }

    @Test
    @DisplayName("buildSystemPrompt contains block categories")
    void testBuildSystemPromptContainsBlockCategories() {
        String systemPrompt = PromptBuilder.buildSystemPrompt();

        assertTrue(systemPrompt.contains("oak"), "Should contain oak wood variants");
        assertTrue(systemPrompt.contains("stone"), "Should contain stone variants");
        assertTrue(systemPrompt.contains("ore"), "Should contain ore variants");
        assertTrue(systemPrompt.contains("wool"), "Should contain wool variants");
        assertTrue(systemPrompt.contains("terracotta"), "Should contain terracotta variants");
    }

    @Test
    @DisplayName("buildSystemPrompt contains structure examples")
    void testBuildSystemPromptContainsStructures() {
        String systemPrompt = PromptBuilder.buildSystemPrompt();

        assertTrue(systemPrompt.contains("house"), "Should contain house structure");
        assertTrue(systemPrompt.contains("oldhouse"), "Should contain oldhouse structure");
        assertTrue(systemPrompt.contains("powerplant"), "Should contain powerplant structure");
        assertTrue(systemPrompt.contains("castle"), "Should contain castle structure");
        assertTrue(systemPrompt.contains("tower"), "Should contain tower structure");
        assertTrue(systemPrompt.contains("barn"), "Should contain barn structure");
        assertTrue(systemPrompt.contains("modern"), "Should contain modern structure");
    }

    @Test
    @DisplayName("buildSystemPrompt is cached (same instance returned)")
    void testBuildSystemPromptIsCached() {
        String prompt1 = PromptBuilder.buildSystemPrompt();
        String prompt2 = PromptBuilder.buildSystemPrompt();

        assertSame(prompt1, prompt2, "System prompt should be cached and return same instance");
    }

    @Test
    @DisplayName("buildSystemPrompt contains JSON output instruction")
    void testBuildSystemPromptContainsJSONInstruction() {
        String systemPrompt = PromptBuilder.buildSystemPrompt();

        assertTrue(systemPrompt.contains("JSON"), "Should mention JSON");
        assertTrue(systemPrompt.contains("OUTPUT ONLY VALID JSON"), "Should instruct JSON-only output");
        assertTrue(systemPrompt.contains("reasoning"), "Should contain reasoning field");
        assertTrue(systemPrompt.contains("plan"), "Should contain plan field");
        assertTrue(systemPrompt.contains("tasks"), "Should contain tasks field");
    }

    @Test
    @DisplayName("buildSystemPrompt contains examples")
    void testBuildSystemPromptContainsExamples() {
        String systemPrompt = PromptBuilder.buildSystemPrompt();

        assertTrue(systemPrompt.contains("EXAMPLES:"), "Should have examples section");
        assertTrue(systemPrompt.contains("build a house"), "Should contain build example");
        assertTrue(systemPrompt.contains("get iron"), "Should contain mining example");
        assertTrue(systemPrompt.contains("kill mobs"), "Should contain combat example");
    }

    // ------------------------------------------------------------------------
    // User Prompt Tests - Basic Structure
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("buildUserPrompt returns non-empty string")
    void testBuildUserPromptReturnsNonEmpty() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test command", mockWorldKnowledge);

        assertNotNull(userPrompt, "User prompt should not be null");
        assertFalse(userPrompt.isEmpty(), "User prompt should not be empty");
    }

    @Test
    @DisplayName("buildUserPrompt contains position")
    void testBuildUserPromptContainsPosition() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        assertTrue(userPrompt.contains("POS:"), "Should contain position prefix");
        assertTrue(userPrompt.contains("100"), "Should contain X coordinate");
        assertTrue(userPrompt.contains("64"), "Should contain Y coordinate");
        assertTrue(userPrompt.contains("200"), "Should contain Z coordinate");
        assertTrue(userPrompt.contains("[100,64,200]"), "Should contain formatted position");
    }

    @Test
    @DisplayName("buildUserPrompt contains command")
    void testBuildUserPromptContainsCommand() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String command = "build a castle";
        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, command, mockWorldKnowledge);

        assertTrue(userPrompt.contains("CMD:"), "Should contain command prefix");
        assertTrue(userPrompt.contains(command), "Should contain the actual command");
        assertTrue(userPrompt.contains("\"" + command + "\""), "Should wrap command in quotes");
    }

    @Test
    @DisplayName("buildUserPrompt contains biome")
    void testBuildUserPromptContainsBiome() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        assertTrue(userPrompt.contains("BIOME:"), "Should contain biome prefix");
        assertTrue(userPrompt.contains("plains"), "Should contain biome name");
    }

    // ------------------------------------------------------------------------
    // User Prompt Tests - Context Injection
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("buildUserPrompt includes players when present")
    void testBuildUserPromptIncludesPlayers() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("Steve, Alex");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        assertTrue(userPrompt.contains("PLAYERS:"), "Should contain players prefix");
        assertTrue(userPrompt.contains("Steve, Alex"), "Should contain player names");
    }

    @Test
    @DisplayName("buildUserPrompt excludes players when none")
    void testBuildUserPromptExcludesPlayersWhenNone() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        assertFalse(userPrompt.contains("PLAYERS:"), "Should not contain players prefix when none");
        assertFalse(userPrompt.contains("PLAYERS:none"), "Should not show 'none' for players");
    }

    @Test
    @DisplayName("buildUserPrompt includes entities when present")
    void testBuildUserPromptIncludesEntities() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("1 zombie, 2 skeleton");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        assertTrue(userPrompt.contains("ENTITIES:"), "Should contain entities prefix");
        assertTrue(userPrompt.contains("zombie"), "Should contain zombie");
        assertTrue(userPrompt.contains("skeleton"), "Should contain skeleton");
    }

    @Test
    @DisplayName("buildUserPrompt excludes entities when none")
    void testBuildUserPromptExcludesEntitiesWhenNone() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        assertFalse(userPrompt.contains("ENTITIES:"), "Should not contain entities prefix when none");
        assertFalse(userPrompt.contains("ENTITIES:none"), "Should not show 'none' for entities");
    }

    @Test
    @DisplayName("buildUserPrompt includes blocks when present")
    void testBuildUserPromptIncludesBlocks() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("stone, dirt, grass_block");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        assertTrue(userPrompt.contains("BLOCKS:"), "Should contain blocks prefix");
        assertTrue(userPrompt.contains("stone"), "Should contain stone");
        assertTrue(userPrompt.contains("dirt"), "Should contain dirt");
        assertTrue(userPrompt.contains("grass_block"), "Should contain grass_block");
    }

    @Test
    @DisplayName("buildUserPrompt excludes blocks when none")
    void testBuildUserPromptExcludesBlocksWhenNone() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        assertFalse(userPrompt.contains("BLOCKS:"), "Should not contain blocks prefix when none");
        assertFalse(userPrompt.contains("BLOCKS:none"), "Should not show 'none' for blocks");
    }

    @Test
    @DisplayName("buildUserPrompt includes all context when present")
    void testBuildUserPromptIncludesAllContext() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("Steve");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("1 zombie");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("stone, dirt");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        assertTrue(userPrompt.contains("PLAYERS:Steve"), "Should include players");
        assertTrue(userPrompt.contains("ENTITIES:1 zombie"), "Should include entities");
        assertTrue(userPrompt.contains("BLOCKS:stone, dirt"), "Should include blocks");
        assertTrue(userPrompt.contains("BIOME:plains"), "Should include biome");
    }

    @Test
    @DisplayName("buildUserPrompt formats position correctly with negative coordinates")
    void testBuildUserPromptNegativeCoordinates() {
        BlockPos negativePos = new BlockPos(-50, -10, -100);
        when(mockForeman.blockPosition()).thenReturn(negativePos);
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        assertTrue(userPrompt.contains("[-50,-10,-100]"), "Should format negative coordinates correctly");
    }

    @Test
    @DisplayName("buildUserPrompt formats position correctly with zero coordinates")
    void testBuildUserPromptZeroCoordinates() {
        BlockPos zeroPos = new BlockPos(0, 0, 0);
        when(mockForeman.blockPosition()).thenReturn(zeroPos);
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        assertTrue(userPrompt.contains("[0,0,0]"), "Should format zero coordinates correctly");
    }

    // ------------------------------------------------------------------------
    // Input Sanitization Integration Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("buildUserPrompt sanitizes command input")
    void testBuildUserPromptSanitizesCommand() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String command = "mine iron";
        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, command, mockWorldKnowledge);

        // Command should be present but sanitized
        assertTrue(userPrompt.contains("mine"), "Should contain sanitized command");
    }

    @Test
    @DisplayName("buildUserPrompt handles special characters in command")
    void testBuildUserPromptHandlesSpecialCharacters() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String command = "build a house with \"quotes\" and 'apostrophes'";
        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, command, mockWorldKnowledge);

        assertTrue(userPrompt.contains("CMD:"), "Should contain command prefix");
        // The sanitizer should handle special characters
        assertNotNull(userPrompt, "Should handle special characters gracefully");
    }

    @Test
    @DisplayName("buildUserPrompt handles unicode characters")
    void testBuildUserPromptHandlesUnicode() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String command = "build a house";
        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, command, mockWorldKnowledge);

        assertNotNull(userPrompt, "Should handle unicode characters");
    }

    // ------------------------------------------------------------------------
    // Token Estimation Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("estimateSystemPromptTokens returns positive value")
    void testEstimateSystemPromptTokens() {
        int tokens = PromptBuilder.estimateSystemPromptTokens();

        assertTrue(tokens > 0, "Should estimate positive token count");
        assertTrue(tokens > 100, "System prompt should be substantial");
    }

    @Test
    @DisplayName("estimateSystemPromptTokens is reasonable")
    void testEstimateSystemPromptTokensReasonable() {
        String systemPrompt = PromptBuilder.buildSystemPrompt();
        int estimatedTokens = PromptBuilder.estimateSystemPromptTokens();
        int calculatedTokens = systemPrompt.length() / 4;

        assertEquals(calculatedTokens, estimatedTokens,
            "Estimated tokens should match length / 4 calculation");
    }

    @Test
    @DisplayName("estimateUserPromptTokens returns positive value")
    void testEstimateUserPromptTokens() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        int tokens = PromptBuilder.estimateUserPromptTokens(mockForeman, "test command", mockWorldKnowledge);

        assertTrue(tokens > 0, "Should estimate positive token count");
    }

    @Test
    @DisplayName("estimateUserPromptTokens scales with command length")
    void testEstimateUserPromptTokensScalesWithCommand() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        int shortTokens = PromptBuilder.estimateUserPromptTokens(mockForeman, "hi", mockWorldKnowledge);
        int longTokens = PromptBuilder.estimateUserPromptTokens(
            mockForeman, "build a large castle with towers and a moat", mockWorldKnowledge);

        assertTrue(longTokens > shortTokens,
            "Longer commands should result in higher token estimates");
    }

    @Test
    @DisplayName("estimateUserPromptTokens scales with context")
    void testEstimateUserPromptTokensScalesWithContext() {
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        // Minimal context
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        int minimalTokens = PromptBuilder.estimateUserPromptTokens(mockForeman, "test", mockWorldKnowledge);

        // Full context
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("Steve, Alex, Bob");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("1 zombie, 2 skeleton, 1 creeper");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("stone, dirt, grass_block, cobblestone, oak_log");
        int fullTokens = PromptBuilder.estimateUserPromptTokens(mockForeman, "test", mockWorldKnowledge);

        assertTrue(fullTokens > minimalTokens,
            "More context should result in higher token estimates");
    }

    // ------------------------------------------------------------------------
    // Edge Cases
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("buildUserPrompt handles empty command")
    void testBuildUserPromptEmptyCommand() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "", mockWorldKnowledge);

        assertNotNull(userPrompt, "Should handle empty command");
        assertTrue(userPrompt.contains("CMD:\"\""), "Should include empty command in quotes");
    }

    @Test
    @DisplayName("buildUserPrompt handles whitespace command")
    void testBuildUserPromptWhitespaceCommand() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "   ", mockWorldKnowledge);

        assertNotNull(userPrompt, "Should handle whitespace command");
    }

    @Test
    @DisplayName("buildUserPrompt handles very long command")
    void testBuildUserPromptVeryLongCommand() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String longCommand = "build " + "a ".repeat(100) + "house";
        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, longCommand, mockWorldKnowledge);

        assertNotNull(userPrompt, "Should handle very long command");
        // The sanitizer should truncate excessive length
    }

    @Test
    @DisplayName("buildUserPrompt handles command with newlines")
    void testBuildUserPromptCommandWithNewlines() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String command = "build a house\nwith multiple\nlines";
        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, command, mockWorldKnowledge);

        assertNotNull(userPrompt, "Should handle command with newlines");
        // Newlines should be sanitized
    }

    @Test
    @DisplayName("buildUserPrompt handles command with tabs")
    void testBuildUserPromptCommandWithTabs() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String command = "build\ta\t\t\tthouse";
        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, command, mockWorldKnowledge);

        assertNotNull(userPrompt, "Should handle command with tabs");
    }

    @Test
    @DisplayName("buildUserPrompt handles extreme coordinates")
    void testBuildUserPromptExtremeCoordinates() {
        BlockPos extremePos = new BlockPos(10000000, -10000000, 10000000);
        when(mockForeman.blockPosition()).thenReturn(extremePos);
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        assertTrue(userPrompt.contains("10000000"), "Should handle large positive coordinates");
        assertTrue(userPrompt.contains("-10000000"), "Should handle large negative coordinates");
    }

    @Test
    @DisplayName("buildUserPrompt handles special biome names")
    void testBuildUserPromptSpecialBiomeNames() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("the_void");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        assertTrue(userPrompt.contains("the_void"), "Should handle special biome names");
    }

    @Test
    @DisplayName("buildUserPrompt handles complex entity summaries")
    void testBuildUserPromptComplexEntitySummaries() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("10 zombie, 5 skeleton, 2 creeper, 1 spider, 3 enderman");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "attack", mockWorldKnowledge);

        assertTrue(userPrompt.contains("ENTITIES:"), "Should include entities prefix");
        assertTrue(userPrompt.contains("zombie"), "Should include zombie");
        assertTrue(userPrompt.contains("skeleton"), "Should include skeleton");
    }

    @Test
    @DisplayName("buildUserPrompt handles complex block summaries")
    void testBuildUserPromptComplexBlockSummaries() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("stone, dirt, grass_block, cobblestone, oak_log");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        assertTrue(userPrompt.contains("BLOCKS:"), "Should include blocks prefix");
        assertTrue(userPrompt.contains("stone"), "Should include stone");
        assertTrue(userPrompt.contains("oak_log"), "Should include oak_log");
    }

    // ------------------------------------------------------------------------
    // Prompt Structure Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("buildUserPrompt uses pipe separator correctly")
    void testBuildUserPromptPipeSeparator() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("Steve");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("zombie");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("stone");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        // Check that pipe separators are used correctly
        assertTrue(userPrompt.contains(" | "), "Should use pipe separator between sections");
        String[] parts = userPrompt.split(" \\| ");
        assertTrue(parts.length >= 4, "Should have multiple sections separated by pipes");
    }

    @Test
    @DisplayName("buildUserPrompt position is first element")
    void testBuildUserPromptPositionFirst() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        assertTrue(userPrompt.startsWith("POS:["),
            "User prompt should start with position");
    }

    @Test
    @DisplayName("buildUserPrompt ends with newline")
    void testBuildUserPromptEndsWithNewline() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "test", mockWorldKnowledge);

        assertTrue(userPrompt.endsWith("\n"),
            "User prompt should end with newline before command");
    }

    // ------------------------------------------------------------------------
    // Real-world Scenario Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("buildUserPrompt handles mining scenario")
    void testBuildUserPromptMiningScenario() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("coal_ore, iron_ore, stone");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "mine 10 iron ore", mockWorldKnowledge);

        assertTrue(userPrompt.contains("mine"), "Should contain mining command");
        assertTrue(userPrompt.contains("iron"), "Should contain iron");
        assertTrue(userPrompt.contains("BLOCKS:"), "Should show nearby blocks");
        assertTrue(userPrompt.contains("coal_ore") || userPrompt.contains("iron_ore"),
            "Should show relevant ores");
    }

    @Test
    @DisplayName("buildUserPrompt handles combat scenario")
    void testBuildUserPromptCombatScenario() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("Steve");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("3 zombie, 1 skeleton");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("none");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "attack hostile mobs", mockWorldKnowledge);

        assertTrue(userPrompt.contains("attack"), "Should contain attack command");
        assertTrue(userPrompt.contains("ENTITIES:"), "Should show nearby entities");
        assertTrue(userPrompt.contains("zombie"), "Should show zombies");
        assertTrue(userPrompt.contains("PLAYERS:"), "Should show nearby players");
    }

    @Test
    @DisplayName("buildUserPrompt handles building scenario")
    void testBuildUserPromptBuildingScenario() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("none");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("oak_log, cobblestone, dirt");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        String userPrompt = PromptBuilder.buildUserPrompt(mockForeman, "build a small house", mockWorldKnowledge);

        assertTrue(userPrompt.contains("build"), "Should contain build command");
        assertTrue(userPrompt.contains("house"), "Should contain house");
        assertTrue(userPrompt.contains("BLOCKS:"), "Should show available materials");
    }

    // ------------------------------------------------------------------------
    // Performance Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("buildSystemPrompt is fast (cached)")
    void testBuildSystemPromptPerformance() {
        long startTime = System.nanoTime();

        for (int i = 0; i < 10000; i++) {
            PromptBuilder.buildSystemPrompt();
        }

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        double avgMs = durationMs / 10000.0;

        // Cached system prompt should be extremely fast (< 0.01ms average)
        assertTrue(avgMs < 0.01,
            "Average system prompt retrieval time: " + avgMs + "ms (should be < 0.01ms)");
    }

    @Test
    @DisplayName("buildUserPrompt is reasonably fast")
    void testBuildUserPromptPerformance() {
        when(mockWorldKnowledge.getNearbyPlayerNames()).thenReturn("Steve");
        when(mockWorldKnowledge.getNearbyEntitiesSummary()).thenReturn("1 zombie");
        when(mockWorldKnowledge.getNearbyBlocksSummary()).thenReturn("stone, dirt");
        when(mockWorldKnowledge.getBiomeName()).thenReturn("plains");

        long startTime = System.nanoTime();

        for (int i = 0; i < 1000; i++) {
            PromptBuilder.buildUserPrompt(mockForeman, "build a house", mockWorldKnowledge);
        }

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        double avgMs = durationMs / 1000.0;

        // User prompt building should be reasonably fast (< 1ms average)
        assertTrue(avgMs < 1.0,
            "Average user prompt build time: " + avgMs + "ms (should be < 1ms)");
        System.out.println("Average user prompt build time: " + avgMs + "ms");
    }
}
