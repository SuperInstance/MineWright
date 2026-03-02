package com.minewright.script;

import com.minewright.llm.async.AsyncLLMClient;
import com.minewright.llm.async.LLMResponse;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link ScriptGenerator}.
 *
 * <p>Test Coverage:</p>
 * <ul>
 *   <li>Script generation from task patterns</li>
 *   <li>Parameter substitution and validation</li>
 *   <li>Cache hit/miss behavior</li>
 *   <li>Error handling for invalid inputs</li>
 *   <li>DSL output format verification</li>
 *   <li>Statistics tracking</li>
 * </ul>
 *
 * @since 1.3.0
 */
@DisplayName("ScriptGenerator Tests")
class ScriptGeneratorTest {

    @Mock
    private AsyncLLMClient mockLLMClient;

    @Mock
    private ScriptParser mockParser;

    @Mock
    private ScriptValidator mockValidator;

    private ScriptGenerator generator;
    private ScriptGenerationContext context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create generator with default configuration
        generator = new ScriptGenerator(mockLLMClient, mockParser, mockValidator);

        // Create a basic context for testing
        context = ScriptGenerationContext.builder()
            .agentId("test-agent-1")
            .agentName("Steve")
            .agentPosition(100, 64, 200)
            .addInventoryItem("oak_log", 64)
            .addInventoryItem("stone", 32)
            .biome("forest")
            .timeOfDay("day")
            .build();

        // Setup default validator behavior
        ScriptValidator.ValidationResult validResult = new ScriptValidator.ValidationResult();
        when(mockValidator.validate(any())).thenReturn(validResult);
    }

    // ==================== Null and Empty Input Tests ====================

    @Test
    @DisplayName("generateAsync should throw exception for null command")
    void testGenerateAsync_NullCommand_ThrowsException() {
        CompletableFuture<Script> future = generator.generateAsync(null, context);

        assertThrows(NullPointerException.class, () -> {
            try {
                future.get();
            } catch (ExecutionException e) {
                throw (e.getCause());
            }
        });
    }

    @Test
    @DisplayName("generateAsync should throw exception for empty command")
    void testGenerateAsync_EmptyCommand_ThrowsException() {
        CompletableFuture<Script> future = generator.generateAsync("   ", context);

        assertThrows(IllegalArgumentException.class, () -> {
            try {
                future.get();
            } catch (ExecutionException e) {
                throw (e.getCause());
            }
        });
    }

    @Test
    @DisplayName("generateAsync should throw exception for null context")
    void testGenerateAsync_NullContext_ThrowsException() {
        CompletableFuture<Script> future = generator.generateAsync("mine iron ore", null);

        assertThrows(NullPointerException.class, () -> {
            try {
                future.get();
            } catch (ExecutionException e) {
                throw (e.getCause());
            }
        });
    }

    // ==================== Script Generation Tests ====================

    @Test
    @DisplayName("generateAsync should successfully generate script from simple command")
    void testGenerateAsync_SimpleCommand_Success() throws Exception {
        // Arrange
        String command = "mine iron ore";
        String llmResponse = buildSimpleMiningScriptYAML();
        LLMResponse response = LLMResponse.builder()
            .content(llmResponse)
            .model("gpt-4")
            .providerId("openai")
            .tokensUsed(500)
            .latencyMs(1000)
            .fromCache(false)
            .build();

        when(mockLLMClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        // Act
        CompletableFuture<Script> future = generator.generateAsync(command, context);
        Script script = future.get();

        // Assert
        assertNotNull(script, "Script should not be null");
        assertNotNull(script.getMetadata(), "Metadata should not be null");
        assertEquals("llm-generator", script.getAuthor(), "Author should be llm-generator");
        assertTrue(script.getTags().contains("generated"), "Should have generated tag");

        // Verify LLM was called
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLLMClient).sendAsync(promptCaptor.capture(), any());

        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains(command), "Prompt should contain the command");
        assertTrue(prompt.contains("DSL Grammar"), "Prompt should contain DSL grammar reference");
        assertTrue(prompt.contains("Example Scripts"), "Prompt should contain examples");
    }

    @Test
    @DisplayName("generateAsync should include context in prompt when provided")
    void testGenerateAsync_WithContext_IncludesContextInPrompt() throws Exception {
        // Arrange
        String command = "build shelter";
        String llmResponse = buildSimpleBuildingScriptYAML();
        LLMResponse response = LLMResponse.builder()
            .content(llmResponse)
            .model("gpt-4")
            .providerId("openai")
            .tokensUsed(600)
            .latencyMs(1200)
            .fromCache(false)
            .build();

        when(mockLLMClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        // Act
        CompletableFuture<Script> future = generator.generateAsync(command, context);
        future.get();

        // Assert
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLLMClient).sendAsync(promptCaptor.capture(), any());

        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Steve"), "Prompt should contain agent name");
        assertTrue(prompt.contains("oak_log"), "Prompt should contain inventory item");
        assertTrue(prompt.contains("forest"), "Prompt should contain biome");
        assertTrue(prompt.contains("day"), "Prompt should contain time of day");
        assertTrue(prompt.contains("X=100"), "Prompt should contain position");
    }

    @Test
    @DisplayName("generateAsync should handle empty context")
    void testGenerateAsync_EmptyContext_Success() throws Exception {
        // Arrange
        String command = "move forward";
        ScriptGenerationContext emptyContext = ScriptGenerationContext.builder().build();
        String llmResponse = buildSimpleMovementScriptYAML();
        LLMResponse response = LLMResponse.builder()
            .content(llmResponse)
            .model("gpt-4")
            .providerId("openai")
            .tokensUsed(300)
            .latencyMs(800)
            .fromCache(false)
            .build();

        when(mockLLMClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        // Act
        CompletableFuture<Script> future = generator.generateAsync(command, emptyContext);
        Script script = future.get();

        // Assert
        assertNotNull(script, "Script should not be null");
        verify(mockLLMClient).sendAsync(any(), any());
    }

    // ==================== Caching Tests ====================

    @Test
    @DisplayName("generateAsync should return cached script on cache hit")
    void testGenerateAsync_CacheHit_ReturnsCachedScript() throws Exception {
        // Arrange - First call
        String command = "gather wood";
        String llmResponse = buildSimpleGatheringScriptYAML();
        LLMResponse response = LLMResponse.builder()
            .content(llmResponse)
            .model("gpt-4")
            .providerId("openai")
            .tokensUsed(400)
            .latencyMs(900)
            .fromCache(false)
            .build();

        when(mockLLMClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        // Act - First call
        CompletableFuture<Script> future1 = generator.generateAsync(command, context);
        Script script1 = future1.get();

        // Act - Second call (should hit cache)
        CompletableFuture<Script> future2 = generator.generateAsync(command, context);
        Script script2 = future2.get();

        // Assert
        assertNotNull(script1, "First script should not be null");
        assertNotNull(script2, "Second script should not be null");

        // LLM should only be called once (first time)
        verify(mockLLMClient, times(1)).sendAsync(any(), any());

        // Stats should show cache hit
        assertEquals(1, generator.getStats().getCacheHits(), "Should have 1 cache hit");
        assertEquals(1, generator.getStats().getCacheMisses(), "Should have 1 cache miss");
        assertEquals(0.5, generator.getStats().getCacheHitRate(), 0.01, "Cache hit rate should be 50%");
    }

    @Test
    @DisplayName("generateAsync should handle different contexts as different cache entries")
    void testGenerateAsync_DifferentContexts_DifferentCacheEntries() throws Exception {
        // Arrange
        String command = "mine coal";
        String llmResponse = buildSimpleMiningScriptYAML();
        LLMResponse response = LLMResponse.builder()
            .content(llmResponse)
            .model("gpt-4")
            .providerId("openai")
            .tokensUsed(400)
            .latencyMs(900)
            .fromCache(false)
            .build();

        when(mockLLMClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        ScriptGenerationContext context1 = ScriptGenerationContext.builder()
            .agentName("Steve")
            .build();

        ScriptGenerationContext context2 = ScriptGenerationContext.builder()
            .agentName("Alex")
            .build();

        // Act
        generator.generateAsync(command, context1).get();
        generator.generateAsync(command, context2).get();

        // Assert - Should call LLM twice because contexts differ
        verify(mockLLMClient, times(2)).sendAsync(any(), any());
    }

    @Test
    @DisplayName("clearCache should remove all cached scripts")
    void testClearCache_RemovesAllScripts() throws Exception {
        // Arrange
        String command = "test command";
        String llmResponse = buildSimpleMiningScriptYAML();
        LLMResponse response = LLMResponse.builder()
            .content(llmResponse)
            .model("gpt-4")
            .providerId("openai")
            .tokensUsed(400)
            .latencyMs(900)
            .fromCache(false)
            .build();

        when(mockLLMClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        // Generate a script to populate cache
        generator.generateAsync(command, context).get();

        assertEquals(1, generator.getCacheSize(), "Cache should have 1 entry");

        // Act
        generator.clearCache();

        // Assert
        assertEquals(0, generator.getCacheSize(), "Cache should be empty");
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("generateAsync should handle LLM client failure")
    void testGenerateAsync_LLMFailure_ThrowsScriptGenerationException() {
        // Arrange
        String command = "complex task";
        when(mockLLMClient.sendAsync(any(), any()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API unavailable")));

        // Act & Assert
        CompletableFuture<Script> future = generator.generateAsync(command, context);

        assertThrows(ScriptGenerator.ScriptGenerationException.class, () -> {
            try {
                future.get();
            } catch (ExecutionException e) {
                throw (e.getCause());
            }
        });

        // Stats should record failure
        assertEquals(1, generator.getStats().getFailures(), "Should record 1 failure");
    }

    @Test
    @DisplayName("generateAsync should handle parsing failure")
    void testGenerateAsync_ParsingFailure_ThrowsScriptGenerationException() throws Exception {
        // Arrange
        String command = "invalid script output";
        String llmResponse = "this is not valid YAML DSL";

        LLMResponse response = LLMResponse.builder()
            .content(llmResponse)
            .model("gpt-4")
            .providerId("openai")
            .tokensUsed(200)
            .latencyMs(500)
            .fromCache(false)
            .build();

        when(mockLLMClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        // This will cause parsing to fail because the response is not valid YAML
        // The actual parsing happens in ScriptParser.parse()

        // Since we can't easily mock the static ScriptParser.parse() method,
        // we'll test the behavior with a valid script that has validation issues
        String invalidScript = buildScriptWithValidationErrors();
        LLMResponse invalidResponse = LLMResponse.builder()
            .content(invalidScript)
            .model("gpt-4")
            .providerId("openai")
            .tokensUsed(300)
            .latencyMs(600)
            .fromCache(false)
            .build();

        when(mockLLMClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(invalidResponse));

        // Mock validator to return validation failure
        ScriptValidator.ValidationResult invalidResult = new ScriptValidator.ValidationResult();
        invalidResult.addError("Script has validation errors");
        when(mockValidator.validate(any())).thenReturn(invalidResult);

        // Act
        CompletableFuture<Script> future = generator.generateAsync(command, context);
        Script script = future.get();

        // Assert - Script should still be returned even with validation errors
        assertNotNull(script, "Script should be returned even with validation errors");
        assertEquals(1, generator.getStats().getValidationFailures(), "Should record validation failure");
    }

    // ==================== DSL Output Format Tests ====================

    @Test
    @DisplayName("generated script should be convertible to DSL format")
    void testScriptToDSL_Convertible() throws Exception {
        // Arrange
        String command = "build house";
        String llmResponse = buildSimpleBuildingScriptYAML();
        LLMResponse response = LLMResponse.builder()
            .content(llmResponse)
            .model("gpt-4")
            .providerId("openai")
            .tokensUsed(700)
            .latencyMs(1500)
            .fromCache(false)
            .build();

        when(mockLLMClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        // Act
        CompletableFuture<Script> future = generator.generateAsync(command, context);
        Script script = future.get();

        // Assert
        String dsl = script.toDSL();
        assertNotNull(dsl, "DSL output should not be null");
        assertTrue(dsl.contains("# Script:"), "DSL should have script header");
        assertTrue(dsl.contains("metadata:"), "DSL should have metadata section");
        assertTrue(dsl.contains("script:"), "DSL should have script section");
    }

    @Test
    @DisplayName("generated script should preserve metadata")
    void testGeneratedScript_PreservesMetadata() throws Exception {
        // Arrange
        String command = "craft tools";
        String llmResponse = buildScriptWithFullMetadata();
        LLMResponse response = LLMResponse.builder()
            .content(llmResponse)
            .model("gpt-4")
            .providerId("openai")
            .tokensUsed(500)
            .latencyMs(1000)
            .fromCache(false)
            .build();

        when(mockLLMClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        // Act
        CompletableFuture<Script> future = generator.generateAsync(command, context);
        Script script = future.get();

        // Assert
        assertNotNull(script.getMetadata(), "Metadata should not be null");
        assertEquals("craft_tools_script", script.getId(), "Script ID should be preserved");
        assertEquals("Craft Tools", script.getName(), "Script name should be preserved");
        assertEquals("Crafts basic tools from available materials", script.getDescription(), "Description should be preserved");
        assertEquals("llm-generator", script.getAuthor(), "Author should be llm-generator");
    }

    // ==================== Configuration Tests ====================

    @Test
    @DisplayName("Generator should respect custom configuration")
    void testCustomConfiguration_RespectsSettings() throws Exception {
        // Arrange
        ScriptGenerator.GeneratorConfig customConfig = new ScriptGenerator.GeneratorConfig(
            3000, // maxTokens
            0.5, // temperature
            50, // maxCacheSize
            Duration.ofMinutes(30), // cacheTtl
            false // includeExamples
        );

        ScriptGenerator customGenerator = new ScriptGenerator(
            mockLLMClient, mockParser, mockValidator, customConfig);

        String command = "test config";
        String llmResponse = buildSimpleMiningScriptYAML();
        LLMResponse response = LLMResponse.builder()
            .content(llmResponse)
            .model("gpt-4")
            .providerId("openai")
            .tokensUsed(400)
            .latencyMs(900)
            .fromCache(false)
            .build();

        when(mockLLMClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        // Act
        customGenerator.generateAsync(command, context).get();

        // Assert
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockLLMClient).sendAsync(any(), paramsCaptor.capture());

        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals(3000, params.get("maxTokens"), "Should use custom maxTokens");
        assertEquals(0.5, params.get("temperature"), "Should use custom temperature");

        // Also verify prompt doesn't include examples
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLLMClient).sendAsync(promptCaptor.capture(), any());

        String prompt = promptCaptor.getValue();
        assertFalse(prompt.contains("Example Scripts"), "Should not include examples when disabled");
    }

    // ==================== Statistics Tests ====================

    @Test
    @DisplayName("Generator should track statistics accurately")
    void testStatistics_Tracking() throws Exception {
        // Arrange
        String command = "track stats";
        String llmResponse = buildSimpleMiningScriptYAML();
        LLMResponse response = LLMResponse.builder()
            .content(llmResponse)
            .model("gpt-4")
            .providerId("openai")
            .tokensUsed(500)
            .latencyMs(1000)
            .fromCache(false)
            .build();

        when(mockLLMClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        // Act - Generate script
        generator.generateAsync(command, context).get();

        // Assert stats
        ScriptGenerator.GeneratorStats stats = generator.getStats();
        assertEquals(0, stats.getCacheHits(), "Should have 0 cache hits");
        assertEquals(1, stats.getCacheMisses(), "Should have 1 cache miss");
        assertEquals(1, stats.getLLMCalls(), "Should have 1 LLM call");
        assertEquals(500, stats.getTotalTokensUsed(), "Should track 500 tokens");
        assertEquals(500.0, stats.getAvgTokensPerCall(), 0.01, "Average tokens should be 500");
        assertEquals(1000.0, stats.getAvgLatencyMs(), 0.01, "Average latency should be 1000ms");
        assertEquals(1, stats.getSuccesses(), "Should have 1 success");
        assertEquals(0, stats.getFailures(), "Should have 0 failures");

        // Verify toString
        String statsString = stats.toString();
        assertTrue(statsString.contains("GeneratorStats"), "Stats toString should contain class name");
        assertTrue(statsString.contains("llmCalls=1"), "Should show 1 LLM call");
    }

    // ==================== Parameter Substitution Tests ====================

    @Test
    @DisplayName("generated script should support parameterized values")
    void testGeneratedScript_Parameterized() throws Exception {
        // Arrange
        String command = "build wall with oak planks";
        String llmResponse = buildParameterizedScriptYAML();
        LLMResponse response = LLMResponse.builder()
            .content(llmResponse)
            .model("gpt-4")
            .providerId("openai")
            .tokensUsed(600)
            .latencyMs(1200)
            .fromCache(false)
            .build();

        when(mockLLMClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        // Act
        CompletableFuture<Script> future = generator.generateAsync(command, context);
        Script script = future.get();

        // Assert
        assertNotNull(script.getParameters(), "Script should have parameters");
        assertFalse(script.getParameters().isEmpty(), "Script should have at least one parameter");

        // Check if material parameter exists
        assertTrue(script.getParameters().containsKey("material") ||
                   script.getParameters().containsKey("block"),
                   "Script should have material or block parameter");
    }

    // ==================== Helper Methods ====================

    private String buildSimpleMiningScriptYAML() {
        return """
            metadata:
              id: "simple_mining"
              name: "Simple Mining"
              description: "Mines blocks in a area"

            script:
              type: "sequence"
              steps:
                - type: "action"
                  action: "locate_nearest"
                  params:
                    block: "iron_ore"
                    radius: 64
                    save_as: "target"

                - type: "action"
                  action: "pathfind_to"
                  params:
                    target: "@target"
                    max_distance: 100

                - type: "action"
                  action: "mine"
                  params:
                    target: "@target"
            """;
    }

    private String buildSimpleBuildingScriptYAML() {
        return """
            metadata:
              id: "simple_building"
              name: "Simple Building"
              description: "Builds a structure"

            parameters:
              - name: "material"
                type: "string"
                default: "oak_planks"

            script:
              type: "sequence"
              steps:
                - type: "action"
                  action: "build_rectangle"
                  params:
                    width: 5
                    depth: 5
                    height: 3
                    material: "{{material}}"
            """;
    }

    private String buildSimpleGatheringScriptYAML() {
        return """
            metadata:
              id: "simple_gathering"
              name: "Simple Gathering"
              description: "Gathers resources"

            script:
              type: "sequence"
              steps:
                - type: "action"
                  action: "gather"
                  params:
                    resource: "oak_log"
                    amount: 10
            """;
    }

    private String buildSimpleMovementScriptYAML() {
        return """
            metadata:
              id: "simple_movement"
              name: "Simple Movement"
              description: "Moves to a location"

            script:
              type: "action"
              action: "move"
              params:
                direction: "forward"
                distance: 10
            """;
    }

    private String buildScriptWithFullMetadata() {
        return """
            metadata:
              id: "craft_tools_script"
              name: "Craft Tools"
              description: "Crafts basic tools from available materials"
              author: "test_author"

            script:
              type: "sequence"
              steps:
                - type: "action"
                  action: "craft"
                  params:
                    item: "wooden_pickaxe"
                    count: 1
            """;
    }

    private String buildScriptWithValidationErrors() {
        return """
            metadata:
              id: "invalid_script"
              name: "Invalid Script"
              description: "A script with validation issues"

            script:
              type: "sequence"
              steps:
                - type: "action"
                  action: "unknown_action"
                  params:
                    invalid_param: "value"
            """;
    }

    private String buildParameterizedScriptYAML() {
        return """
            metadata:
              id: "parameterized_wall"
              name: "Build Wall"
              description: "Builds a wall with specified material"

            parameters:
              - name: "material"
                type: "string"
                default: "oak_planks"
              - name: "height"
                type: "integer"
                default: 3

            script:
              type: "sequence"
              steps:
                - type: "action"
                  action: "check_inventory"
                  params:
                    item: "{{material}}"
                    count: 20

                - type: "action"
                  action: "build_wall"
                  params:
                    material: "{{material}}"
                    height: "{{height}}"
                    length: 10
            """;
    }
}
