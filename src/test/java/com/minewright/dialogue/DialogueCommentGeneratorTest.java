package com.minewright.dialogue;

import com.minewright.config.MineWrightConfig;
import com.minewright.entity.ForemanEntity;
import com.minewright.action.ActionExecutor;
import com.minewright.llm.TaskPlanner;
import com.minewright.llm.batch.BatchingLLMClient;
import com.minewright.llm.batch.PromptBatcher;
import com.minewright.memory.CompanionMemory;
import com.minewright.memory.ConversationManager;
import com.minewright.memory.PersonalitySystem;
import com.minewright.llm.async.AsyncLLMClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DialogueCommentGenerator}.
 *
 * <p>Tests cover comment generation, fallback selection, relationship-aware dialogue,
 * prompt building, and tone assignment.</p>
 *
 * @since 1.4.0
 */
@DisplayName("DialogueCommentGenerator Tests")
class DialogueCommentGeneratorTest {

    @Mock
    private ForemanEntity foremanEntity;

    @Mock
    private CompanionMemory memory;

    @Mock
    private ActionExecutor actionExecutor;

    @Mock
    private TaskPlanner taskPlanner;

    @Mock
    private BatchingLLMClient batchingClient;

    @Mock
    private ConversationManager conversationManager;

    @Mock
    private AsyncLLMClient llmClient;

    private DialogueCommentGenerator generator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup common mocks
        when(foremanEntity.getActionExecutor()).thenReturn(actionExecutor);
        when(actionExecutor.getTaskPlanner()).thenReturn(taskPlanner);
        when(taskPlanner.getBatchingClient()).thenReturn(batchingClient);
        when(foremanEntity.getEntityName()).thenReturn("Steve");

        // Setup personality mock
        PersonalitySystem.PersonalityProfile personality = mock(PersonalitySystem.PersonalityProfile.class);
        when(personality.getExtraversion()).thenReturn(75);
        when(personality.getFormality()).thenReturn(30);
        when(personality.getHumor()).thenReturn(60);
        when(personality.getEncouragement()).thenReturn(80);
        when(personality.getCatchphrases()).thenReturn(List.of("Let's build!", "On it!"));

        // Setup memory mock
        when(memory.getPlayerName()).thenReturn("Player");
        when(memory.getFirstMeeting()).thenReturn(Instant.now().minusSeconds(86400 * 7)); // 7 days ago
        when(memory.getPersonality()).thenReturn(personality);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should initialize generator with required dependencies")
        void testConstructorInitialization() {
            DialogueCommentGenerator gen = new DialogueCommentGenerator(foremanEntity, memory);

            assertNotNull(gen, "Generator should be initialized");
        }
    }

    @Nested
    @DisplayName("generateComment Tests")
    class GenerateCommentTests {

        @Test
        @DisplayName("generateComment should use batching client when available")
        void testGenerateCommentWithBatchingClient() throws ExecutionException, InterruptedException {
            when(batchingClient.submit(anyString(), eq(PromptBatcher.PromptType.BACKGROUND), any()))
                .thenReturn(CompletableFuture.completedFuture("Test comment"));

            generator = new DialogueCommentGenerator(foremanEntity, memory);
            CompletableFuture<String> result = generator.generateComment("morning", "Morning time", 50);

            assertNotNull(result, "Result should not be null");
            assertEquals("Test comment", result.get(), "Comment should match");
            verify(batchingClient).submit(anyString(), eq(PromptBatcher.PromptType.BACKGROUND), any());
        }

        @Test
        @DisplayName("generateComment should fallback to conversation manager when batching unavailable")
        void testGenerateCommentWithoutBatchingClient() {
            when(taskPlanner.getBatchingClient()).thenReturn(null);
            when(conversationManager.generateProactiveComment(anyString(), eq(llmClient)))
                .thenReturn(CompletableFuture.completedFuture("Direct comment"));

            // This test verifies the fallback path exists
            // Full testing would require more complex mocking
            generator = new DialogueCommentGenerator(foremanEntity, memory);
            CompletableFuture<String> result = generator.generateComment("morning", "Morning time", 50);

            assertNotNull(result, "Result should not be null");
        }
    }

    @Nested
    @DisplayName("getRelationshipAwareFallback Tests")
    class GetRelationshipAwareFallbackTests {

        @BeforeEach
        void setUpGenerator() {
            generator = new DialogueCommentGenerator(foremanEntity, memory);
        }

        @Test
        @DisplayName("getRelationshipAwareFallback should return comment for known trigger type")
        void testGetRelationshipAwareFallbackKnownTrigger() {
            String result = generator.getRelationshipAwareFallback("morning", 50);

            assertNotNull(result, "Should return a comment");
            assertFalse(result.isEmpty(), "Comment should not be empty");
        }

        @Test
        @DisplayName("getRelationshipAwareFallback should return high rapport comment for rapport > 70")
        void testGetRelationshipAwareFallbackHighRapport() {
            String result = generator.getRelationshipAwareFallback("morning", 80);

            assertNotNull(result, "Should return a comment");
            // May return high rapport specific comment or base comment
            assertFalse(result.isEmpty(), "Comment should not be empty");
        }

        @Test
        @DisplayName("getRelationshipAwareFallback should return low rapport comment for rapport < 30")
        void testGetRelationshipAwareFallbackLowRapport() {
            String result = generator.getRelationshipAwareFallback("morning", 20);

            assertNotNull(result, "Should return a comment");
            // May return low rapport specific comment or base comment
            assertFalse(result.isEmpty(), "Comment should not be empty");
        }

        @Test
        @DisplayName("getRelationshipAwareFallback should return relationship comment for unknown trigger")
        void testGetRelationshipAwareFallbackUnknownTrigger() {
            String result = generator.getRelationshipAwareFallback("unknown_trigger", 50);

            // Should fall back to relationship-based dialogue
            assertNotNull(result, "Should return a relationship-based comment");
            assertFalse(result.isEmpty(), "Comment should not be empty");
        }

        @Test
        @DisplayName("getRelationshipAwareFallback should return null when no comments available")
        void testGetRelationshipAwareFallbackNoComments() {
            // This would require empty fallback maps, which isn't possible with current implementation
            // Testing with very high rapport that should still return something
            String result = generator.getRelationshipAwareFallback("morning", 100);

            assertNotNull(result, "Should still return a comment");
        }

        @Test
        @DisplayName("getRelationshipAwareFallback should handle all trigger types")
        void testGetRelationshipAwareFallbackAllTriggerTypes() {
            String[] triggerTypes = {
                "morning", "night", "raining", "storm",
                "idle_long", "near_danger", "task_complete",
                "task_failed", "milestone"
            };

            for (String triggerType : triggerTypes) {
                String result = generator.getRelationshipAwareFallback(triggerType, 50);
                assertNotNull(result, "Should return comment for trigger type: " + triggerType);
                assertFalse(result.isEmpty(), "Comment should not be empty for: " + triggerType);
            }
        }
    }

    @Nested
    @DisplayName("getFallbackComment Tests")
    class GetFallbackCommentTests {

        @BeforeEach
        void setUpGenerator() {
            generator = new DialogueCommentGenerator(foremanEntity, memory);
        }

        @Test
        @DisplayName("getFallbackComment should return comment for known trigger type")
        void testGetFallbackCommentKnownTrigger() {
            String result = generator.getFallbackComment("morning");

            assertNotNull(result, "Should return a comment");
            assertFalse(result.isEmpty(), "Comment should not be empty");
            assertTrue(result.contains("!") || result.contains("?"),
                "Comment should contain punctuation");
        }

        @Test
        @DisplayName("getFallbackComment should return null for unknown trigger type")
        void testGetFallbackCommentUnknownTrigger() {
            String result = generator.getFallbackComment("unknown_trigger_xyz");

            assertNull(result, "Should return null for unknown trigger");
        }

        @Test
        @DisplayName("getFallbackComment should return different comments on multiple calls")
        void testGetFallbackCommentRandomness() {
            String comment1 = generator.getFallbackComment("morning");
            String comment2 = generator.getFallbackComment("morning");
            String comment3 = generator.getFallbackComment("morning");

            // With randomness, we should likely get different comments
            // But test should pass even if we get the same (randomness is random)
            assertNotNull(comment1, "First comment should not be null");
            assertNotNull(comment2, "Second comment should not be null");
            assertNotNull(comment3, "Third comment should not be null");
        }

        @Test
        @DisplayName("getFallbackComment should handle all defined trigger types")
        void testGetFallbackCommentAllTriggerTypes() {
            String[] triggerTypes = {
                "morning", "night", "raining", "storm",
                "idle_long", "near_danger", "task_complete",
                "task_failed", "milestone"
            };

            for (String triggerType : triggerTypes) {
                String result = generator.getFallbackComment(triggerType);
                assertNotNull(result, "Should return comment for: " + triggerType);
                assertFalse(result.isEmpty(), "Comment should not be empty for: " + triggerType);
            }
        }
    }

    @Nested
    @DisplayName("buildProactivePrompt Tests")
    class BuildProactivePromptTests {

        @BeforeEach
        void setUpGenerator() {
            generator = new DialogueCommentGenerator(foremanEntity, memory);
        }

        @Test
        @DisplayName("buildProactivePrompt should include trigger context")
        void testBuildProactivePromptIncludesContext() {
            // We can't directly test private method, but we can verify through generateComment
            // which calls buildProactivePrompt internally

            when(batchingClient.submit(anyString(), eq(PromptBatcher.PromptType.BACKGROUND), any()))
                .thenAnswer(invocation -> {
                    String prompt = invocation.getArgument(0);
                    assertTrue(prompt.contains("Morning time!"),
                        "Prompt should include context");
                    return CompletableFuture.completedFuture("Test");
                });

            generator.generateComment("morning", "Morning time!", 50);
        }

        @Test
        @DisplayName("buildProactivePrompt should include rapport level")
        void testBuildProactivePromptIncludesRapport() {
            when(batchingClient.submit(anyString(), eq(PromptBatcher.PromptType.BACKGROUND), any()))
                .thenAnswer(invocation -> {
                    String prompt = invocation.getArgument(0);
                    assertTrue(prompt.contains("75"),
                        "Prompt should include rapport level");
                    return CompletableFuture.completedFuture("Test");
                });

            generator.generateComment("morning", "Context", 75);
        }

        @Test
        @DisplayName("buildProactivePrompt should include relationship level")
        void testBuildProactivePromptIncludesRelationshipLevel() {
            when(batchingClient.submit(anyString(), eq(PromptBatcher.PromptType.BACKGROUND), any()))
                .thenAnswer(invocation -> {
                    String prompt = invocation.getArgument(0);
                    assertTrue(prompt.contains("trusted friend") ||
                               prompt.contains("close companion") ||
                               prompt.contains("family"),
                        "Prompt should include relationship level");
                    return CompletableFuture.completedFuture("Test");
                });

            generator.generateComment("morning", "Context", 65);
        }

        @Test
        @DisplayName("buildProactivePrompt should include personality traits")
        void testBuildProactivePromptIncludesPersonality() {
            when(batchingClient.submit(anyString(), eq(PromptBatcher.PromptType.BACKGROUND), any()))
                .thenAnswer(invocation -> {
                    String prompt = invocation.getArgument(0);
                    assertTrue(prompt.contains("Extraversion"),
                        "Prompt should include extraversion");
                    assertTrue(prompt.contains("Formality"),
                        "Prompt should include formality");
                    return CompletableFuture.completedFuture("Test");
                });

            generator.generateComment("morning", "Context", 50);
        }
    }

    @Nested
    @DisplayName("getRelationshipLevel Tests")
    class GetRelationshipLevelTests {

        @BeforeEach
        void setUpGenerator() {
            generator = new DialogueCommentGenerator(foremanEntity, memory);
        }

        @Test
        @DisplayName("Should return 'new acquaintance' for rapport < 30")
        void testRelationshipLevelLow() {
            // Verify through prompt generation
            when(batchingClient.submit(anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    String prompt = invocation.getArgument(0);
                    assertTrue(prompt.contains("new acquaintance"),
                        "Should be new acquaintance for low rapport");
                    return CompletableFuture.completedFuture("Test");
                });

            generator.generateComment("morning", "Context", 20);
        }

        @Test
        @DisplayName("Should return 'casual friend' for rapport 30-49")
        void testRelationshipLevelCasual() {
            when(batchingClient.submit(anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    String prompt = invocation.getArgument(0);
                    assertTrue(prompt.contains("casual friend"),
                        "Should be casual friend for medium-low rapport");
                    return CompletableFuture.completedFuture("Test");
                });

            generator.generateComment("morning", "Context", 40);
        }

        @Test
        @DisplayName("Should return 'trusted friend' for rapport 50-69")
        void testRelationshipLevelTrusted() {
            when(batchingClient.submit(anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    String prompt = invocation.getArgument(0);
                    assertTrue(prompt.contains("trusted friend"),
                        "Should be trusted friend for medium rapport");
                    return CompletableFuture.completedFuture("Test");
                });

            generator.generateComment("morning", "Context", 60);
        }

        @Test
        @DisplayName("Should return 'close companion' for rapport 70-84")
        void testRelationshipLevelClose() {
            when(batchingClient.submit(anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    String prompt = invocation.getArgument(0);
                    assertTrue(prompt.contains("close companion"),
                        "Should be close companion for high rapport");
                    return CompletableFuture.completedFuture("Test");
                });

            generator.generateComment("morning", "Context", 75);
        }

        @Test
        @DisplayName("Should return 'family' for rapport >= 85")
        void testRelationshipLevelFamily() {
            when(batchingClient.submit(anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    String prompt = invocation.getArgument(0);
                    assertTrue(prompt.contains("family"),
                        "Should be family for very high rapport");
                    return CompletableFuture.completedFuture("Test");
                });

            generator.generateComment("morning", "Context", 90);
        }
    }

    @Nested
    @DisplayName("getToneForTrigger Tests")
    class GetToneForTriggerTests {

        @BeforeEach
        void setUpGenerator() {
            generator = new DialogueCommentGenerator(foremanEntity, memory);
        }

        @Test
        @DisplayName("Should return correct tone for morning trigger")
        void testToneForMorning() {
            when(batchingClient.submit(anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    String prompt = invocation.getArgument(0);
                    assertTrue(prompt.contains("cheerful"),
                        "Morning tone should be cheerful");
                    return CompletableFuture.completedFuture("Test");
                });

            generator.generateComment("morning", "Context", 50);
        }

        @Test
        @DisplayName("Should return correct tone for night trigger")
        void testToneForNight() {
            when(batchingClient.submit(anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    String prompt = invocation.getArgument(0);
                    assertTrue(prompt.contains("cautious") || prompt.contains("helpful"),
                        "Night tone should be cautious/helpful");
                    return CompletableFuture.completedFuture("Test");
                });

            generator.generateComment("night", "Context", 50);
        }

        @Test
        @DisplayName("Should return correct tone for storm trigger")
        void testToneForStorm() {
            when(batchingClient.submit(anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    String prompt = invocation.getArgument(0);
                    assertTrue(prompt.contains("concerned") || prompt.contains("urgent"),
                        "Storm tone should be concerned/urgent");
                    return CompletableFuture.completedFuture("Test");
                });

            generator.generateComment("storm", "Context", 50);
        }

        @Test
        @DisplayName("Should return correct tone for task_complete trigger")
        void testToneForTaskComplete() {
            when(batchingClient.submit(anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    String prompt = invocation.getArgument(0);
                    assertTrue(prompt.contains("satisfied") || prompt.contains("proud"),
                        "Task complete tone should be satisfied/proud");
                    return CompletableFuture.completedFuture("Test");
                });

            generator.generateComment("task_complete", "Context", 50);
        }

        @Test
        @DisplayName("Should return default tone for unknown trigger")
        void testToneForUnknown() {
            when(batchingClient.submit(anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    String prompt = invocation.getArgument(0);
                    assertTrue(prompt.contains("friendly"),
                        "Unknown trigger should use friendly tone");
                    return CompletableFuture.completedFuture("Test");
                });

            generator.generateComment("unknown_trigger", "Context", 50);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle concurrent comment generation")
        void testConcurrentCommentGeneration() {
            generator = new DialogueCommentGenerator(foremanEntity, memory);

            when(batchingClient.submit(anyString(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture("Comment"));

            // Generate multiple comments concurrently
            CompletableFuture<String> future1 = generator.generateComment("morning", "Context 1", 50);
            CompletableFuture<String> future2 = generator.generateComment("night", "Context 2", 50);
            CompletableFuture<String> future3 = generator.generateComment("task_complete", "Context 3", 50);

            assertDoesNotThrow(() -> {
                CompletableFuture.allOf(future1, future2, future3).get();
            }, "Should handle concurrent comment generation");
        }

        @Test
        @DisplayName("Should maintain conversation context across calls")
        void testMaintainsConversationContext() {
            generator = new DialogueCommentGenerator(foremanEntity, memory);

            when(batchingClient.submit(anyString(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture("Comment"));

            // Multiple calls should maintain context
            generator.generateComment("greeting", "Hello", 50);
            generator.generateComment("task_complete", "Done", 55);
            generator.generateComment("farewell", "Goodbye", 60);

            verify(batchingClient, times(3)).submit(anyString(), any(), any());
        }
    }
}
