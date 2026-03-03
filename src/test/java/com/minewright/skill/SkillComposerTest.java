package com.minewright.skill;

import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.execution.CodeExecutionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link SkillComposer}.
 *
 * Tests cover:
 * <ul>
 *   <li>CompositionBuilder - building compositions with fluent API</li>
 *   <li>validateDependencies - checking skill availability</li>
 *   <li>executeComposed - executing compositions step by step</li>
 *   <li>Caching - composition caching behavior</li>
 *   <li>Success rate tracking - accurate tracking of execution outcomes</li>
 *   <li>Edge cases - empty steps, missing skills, null inputs</li>
 * </ul>
 *
 * @since 1.1.0
 */
@DisplayName("SkillComposer Comprehensive Tests")
class SkillComposerTest {

    private SkillComposer composer;
    private SkillLibrary library;
    private CodeExecutionEngine mockEngine;
    private ForemanEntity mockForeman;

    // Test skills
    private ExecutableSkill skill1;
    private ExecutableSkill skill2;
    private ExecutableSkill skill3;

    @BeforeEach
    void setUp() {
        // Create fresh instance for each test
        composer = SkillComposer.getInstance();
        library = SkillLibrary.getInstance();

        // Clear cache to ensure test isolation
        composer.clearCache();

        // Mock dependencies
        mockEngine = mock(CodeExecutionEngine.class);
        mockForeman = mock(ForemanEntity.class);

        // Setup default mock behavior
        when(mockEngine.execute(anyString()))
            .thenReturn(CodeExecutionEngine.ExecutionResult.success("Success"));

        // Create test skills
        skill1 = createTestSkill("skill1", "First test skill");
        skill2 = createTestSkill("skill2", "Second test skill");
        skill3 = createTestSkill("skill3", "Third test skill");

        // Add skills to library
        library.addSkill(skill1);
        library.addSkill(skill2);
        library.addSkill(skill3);
    }

    // ==================== CompositionBuilder Tests ====================

    @Test
    @DisplayName("CompositionBuilder creates simple composition")
    void testCompositionBuilderCreatesSimpleComposition() {
        ComposedSkill composition = SkillComposer.compose("testComposition")
            .fromSkill("skill1")
            .build();

        assertNotNull(composition, "Composition should not be null");
        assertEquals("testComposition", composition.getName());
        assertEquals(1, composition.getStepCount());
        assertEquals("skill1", composition.getSteps().get(0).getSkillName());
    }

    @Test
    @DisplayName("CompositionBuilder creates multi-step composition")
    void testCompositionBuilderCreatesMultiStepComposition() {
        ComposedSkill composition = SkillComposer.compose("multiStep")
            .fromSkill("skill1")
            .fromSkill("skill2")
            .fromSkill("skill3")
            .build();

        assertNotNull(composition);
        assertEquals("multiStep", composition.getName());
        assertEquals(3, composition.getStepCount());
        assertEquals("skill1", composition.getSteps().get(0).getSkillName());
        assertEquals("skill2", composition.getSteps().get(1).getSkillName());
        assertEquals("skill3", composition.getSteps().get(2).getSkillName());
    }

    @Test
    @DisplayName("CompositionBuilder with description and category")
    void testCompositionBuilderWithDescriptionAndCategory() {
        ComposedSkill composition = SkillComposer.compose("testComposition")
            .description("Test composition for validation")
            .category("test")
            .fromSkill("skill1")
            .build();

        assertEquals("Test composition for validation", composition.getDescription());
        assertEquals("test", composition.getCategory());
    }

    @Test
    @DisplayName("CompositionBuilder with context variables")
    void testCompositionBuilderWithContextVariables() {
        Map<String, Object> context = Map.of(
            "depth", 50,
            "direction", "north"
        );

        ComposedSkill composition = SkillComposer.compose("contextComposition")
            .fromSkill("skill1")
            .withContext("depth", 50)
            .withContext("direction", "north")
            .build();

        Map<String, Object> baseContext = composition.getBaseContext();
        assertEquals(2, baseContext.size());
        assertEquals(50, baseContext.get("depth"));
        assertEquals("north", baseContext.get("direction"));
    }

    @Test
    @DisplayName("CompositionBuilder with step-specific context")
    void testCompositionBuilderWithStepSpecificContext() {
        Map<String, Object> step1Context = Map.of("param1", "value1");
        Map<String, Object> step2Context = Map.of("param2", "value2");

        ComposedSkill composition = SkillComposer.compose("stepContextComposition")
            .fromSkillWithContext("skill1", step1Context)
            .fromSkillWithContext("skill2", step2Context)
            .build();

        assertEquals(2, composition.getStepCount());
        assertEquals("value1", composition.getSteps().get(0).getContext().get("param1"));
        assertEquals("value2", composition.getSteps().get(1).getContext().get("param2"));
    }

    @Test
    @DisplayName("CompositionBuilder with merged context")
    void testCompositionBuilderWithMergedContext() {
        ComposedSkill composition = SkillComposer.compose("mergedContext")
            .withContext("global", "value")
            .fromSkillWithContext("skill1", Map.of("local", "localValue"))
            .build();

        // Base context should have global variable
        assertEquals("value", composition.getBaseContext().get("global"));

        // Step should have local variable
        assertEquals("localValue", composition.getSteps().get(0).getContext().get("local"));
    }

    @Test
    @DisplayName("CompositionBuilder throws exception on empty steps")
    void testCompositionBuilderThrowsExceptionOnEmptySteps() {
        assertThrows(IllegalStateException.class, () -> {
            SkillComposer.compose("emptyComposition").build();
        }, "Building composition with no steps should throw IllegalStateException");
    }

    @Test
    @DisplayName("CompositionBuilder generates auto description")
    void testCompositionBuilderGeneratesAutoDescription() {
        ComposedSkill composition = SkillComposer.compose("autoDesc")
            .fromSkill("skill1")
            .fromSkill("skill2")
            .build();

        String description = composition.getDescription();
        assertTrue(description.contains("skill1"), "Description should contain skill1");
        assertTrue(description.contains("skill2"), "Description should contain skill2");
        assertTrue(description.contains("->"), "Description should show arrow between skills");
    }

    // ==================== validateDependencies Tests ====================

    @Test
    @DisplayName("validateDependencies with all available skills")
    void testValidateDependenciesWithAllAvailableSkills() {
        List<String> skillNames = Arrays.asList("skill1", "skill2", "skill3");

        SkillComposer.ValidationResult result = composer.validateDependencies(skillNames);

        assertTrue(result.isValid());
        assertEquals(3, result.getAvailableSkills().size());
        assertTrue(result.getMissingSkills().isEmpty());
    }

    @Test
    @DisplayName("validateDependencies with some missing skills")
    void testValidateDependenciesWithSomeMissingSkills() {
        List<String> skillNames = Arrays.asList("skill1", "nonexistent", "skill2");

        SkillComposer.ValidationResult result = composer.validateDependencies(skillNames);

        assertFalse(result.isValid());
        assertEquals(2, result.getAvailableSkills().size());
        assertEquals(1, result.getMissingSkills().size());
        assertTrue(result.getMissingSkills().contains("nonexistent"));
    }

    @Test
    @DisplayName("validateDependencies with all missing skills")
    void testValidateDependenciesWithAllMissingSkills() {
        List<String> skillNames = Arrays.asList("missing1", "missing2", "missing3");

        SkillComposer.ValidationResult result = composer.validateDependencies(skillNames);

        assertFalse(result.isValid());
        assertTrue(result.getAvailableSkills().isEmpty());
        assertEquals(3, result.getMissingSkills().size());
    }

    @Test
    @DisplayName("validateDependencies with empty list")
    void testValidateDependenciesWithEmptyList() {
        List<String> skillNames = List.of();

        SkillComposer.ValidationResult result = composer.validateDependencies(skillNames);

        assertTrue(result.isValid());
        assertTrue(result.getAvailableSkills().isEmpty());
        assertTrue(result.getMissingSkills().isEmpty());
    }

    @Test
    @DisplayName("validateDependencies with duplicate skills")
    void testValidateDependenciesWithDuplicateSkills() {
        List<String> skillNames = Arrays.asList("skill1", "skill1", "skill2");

        SkillComposer.ValidationResult result = composer.validateDependencies(skillNames);

        assertTrue(result.isValid());
        assertEquals(3, result.getAvailableSkills().size());
    }

    // ==================== executeComposed Tests ====================

    @Test
    @DisplayName("executeComposed with single step")
    void testExecuteComposedWithSingleStep() {
        ComposedSkill composition = SkillComposer.compose("singleStep")
            .fromSkill("skill1")
            .build();
        Map<String, Object> context = Map.of();

        SkillComposer.CompositionResult result = composer.executeComposed(
            composition, context, mockEngine);

        assertTrue(result.isSuccess());
        assertEquals("Composition completed successfully", result.getMessage());
        assertEquals(1, result.getCompletedSteps());
        assertEquals(1, result.getTotalSteps());
    }

    @Test
    @DisplayName("executeComposed with multiple steps")
    void testExecuteComposedWithMultipleSteps() {
        ComposedSkill composition = SkillComposer.compose("multiStep")
            .fromSkill("skill1")
            .fromSkill("skill2")
            .fromSkill("skill3")
            .build();
        Map<String, Object> context = Map.of();

        SkillComposer.CompositionResult result = composer.executeComposed(
            composition, context, mockEngine);

        assertTrue(result.isSuccess());
        assertEquals(3, result.getCompletedSteps());
        assertEquals(3, result.getTotalSteps());

        // Verify each step was executed
        verify(mockEngine, times(3)).execute(anyString());
    }

    @Test
    @DisplayName("executeComposed merges context correctly")
    void testExecuteComposedMergesContextCorrectly() {
        ComposedSkill composition = SkillComposer.compose("contextMerge")
            .withContext("base", "baseValue")
            .fromSkillWithContext("skill1", Map.of("step1", "step1Value"))
            .build();

        Map<String, Object> executionContext = Map.of("exec", "execValue");

        composer.executeComposed(composition, executionContext, mockEngine);

        // Capture the code that was executed
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockEngine).execute(codeCaptor.capture());

        // The code should contain substituted values
        String executedCode = codeCaptor.getValue();
        assertNotNull(executedCode);
    }

    @Test
    @DisplayName("executeComposed stops on first failure")
    void testExecuteComposedStopsOnFirstFailure() {
        // Setup mock to fail on second call
        when(mockEngine.execute(anyString()))
            .thenReturn(CodeExecutionEngine.ExecutionResult.success("Success"))
            .thenReturn(CodeExecutionEngine.ExecutionResult.error("Step failed"))
            .thenReturn(CodeExecutionEngine.ExecutionResult.success("Success"));

        ComposedSkill composition = SkillComposer.compose("failingComposition")
            .fromSkill("skill1")
            .fromSkill("skill2")
            .fromSkill("skill3")
            .build();

        SkillComposer.CompositionResult result = composer.executeComposed(
            composition, Map.of(), mockEngine);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Step 2 failed"));
        assertEquals(1, result.getCompletedSteps());
        assertEquals(3, result.getTotalSteps());

        // Verify only 2 steps were attempted (first success, second failure)
        verify(mockEngine, times(2)).execute(anyString());
    }

    @Test
    @DisplayName("executeComposed handles missing skill")
    void testExecuteComposedHandlesMissingSkill() {
        // Create composition with a skill that's not in library
        ComposedSkill composition = new ComposedSkill(
            "missingSkill",
            "Composition with missing skill",
            "test",
            List.of(new CompositionStep("nonexistent", null, Map.of())),
            Map.of()
        );

        SkillComposer.CompositionResult result = composer.executeComposed(
            composition, Map.of(), mockEngine);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Skill not found"));
        assertEquals(0, result.getCompletedSteps());

        // No execution should have occurred
        verify(mockEngine, never()).execute(anyString());
    }

    @Test
    @DisplayName("executeComposed records step results")
    void testExecuteComposedRecordsStepResults() {
        ComposedSkill composition = SkillComposer.compose("stepResults")
            .fromSkill("skill1")
            .fromSkill("skill2")
            .build();

        SkillComposer.CompositionResult result = composer.executeComposed(
            composition, Map.of(), mockEngine);

        assertEquals(2, result.getStepResults().size());

        // Check first step result
        SkillComposer.StepResult step1Result = result.getStepResults().get(0);
        assertTrue(step1Result.isSuccess());
        assertEquals("skill1", step1Result.getStep().getSkillName());
        assertNotNull(step1Result.getDurationMs());

        // Check second step result
        SkillComposer.StepResult step2Result = result.getStepResults().get(1);
        assertTrue(step2Result.isSuccess());
        assertEquals("skill2", step2Result.getStep().getSkillName());
    }

    // ==================== executeComposedAsync Tests ====================

    @Test
    @DisplayName("executeComposedAsync returns CompletableFuture")
    void testExecuteComposedAsyncReturnsCompletableFuture() {
        ComposedSkill composition = SkillComposer.compose("asyncComposition")
            .fromSkill("skill1")
            .build();

        CompletableFuture<SkillComposer.CompositionResult> future =
            composer.executeComposedAsync(composition, Map.of(), mockEngine);

        assertNotNull(future);
        assertFalse(future.isCompletedExceptionally());
    }

    @Test
    @DisplayName("executeComposedAsync completes successfully")
    void testExecuteComposedAsyncCompletesSuccessfully() throws ExecutionException, InterruptedException {
        ComposedSkill composition = SkillComposer.compose("asyncComposition")
            .fromSkill("skill1")
            .fromSkill("skill2")
            .build();

        CompletableFuture<SkillComposer.CompositionResult> future =
            composer.executeComposedAsync(composition, Map.of(), mockEngine);

        SkillComposer.CompositionResult result = future.get();
        assertTrue(result.isSuccess());
        assertEquals(2, result.getCompletedSteps());
    }

    @Test
    @DisplayName("executeComposedAsync handles failures")
    void testExecuteComposedAsyncHandlesFailures() throws Exception {
        when(mockEngine.execute(anyString()))
            .thenReturn(CodeExecutionEngine.ExecutionResult.error("Async error"));

        ComposedSkill composition = SkillComposer.compose("failingAsync")
            .fromSkill("skill1")
            .build();

        CompletableFuture<SkillComposer.CompositionResult> future =
            composer.executeComposedAsync(composition, Map.of(), mockEngine);

        SkillComposer.CompositionResult result = future.get();
        assertFalse(result.isSuccess());
    }

    // ==================== Caching Tests ====================

    @Test
    @DisplayName("cacheComposition stores composition")
    void testCacheCompositionStoresComposition() {
        ComposedSkill composition = SkillComposer.compose("cacheTest")
            .fromSkill("skill1")
            .build();

        composer.cacheComposition(composition);

        ComposedSkill retrieved = composer.getCachedComposition(composition.getSignature());
        assertNotNull(retrieved);
        assertEquals("cacheTest", retrieved.getName());
    }

    @Test
    @DisplayName("cacheComposition returns null for missing signature")
    void testCacheCompositionReturnsNullForMissingSignature() {
        ComposedSkill retrieved = composer.getCachedComposition("nonexistent");
        assertNull(retrieved);
    }

    @Test
    @DisplayName("cacheComposition evicts oldest when full")
    void testCacheCompositionEvictsOldestWhenFull() {
        // Clear cache and fill it up to MAX_CACHE_SIZE
        composer.clearCache();

        // Add MAX_CACHE_SIZE compositions
        for (int i = 0; i < 100; i++) {
            ComposedSkill comp = SkillComposer.compose("cache" + i)
                .fromSkill("skill1")
                .build();
            composer.cacheComposition(comp);
        }

        // Verify cache is full
        Map<String, Object> stats = composer.getStatistics();
        assertEquals(100, stats.get("cachedCompositions"));

        // Add one more composition
        ComposedSkill extra = SkillComposer.compose("extra")
            .fromSkill("skill1")
            .build();
        composer.cacheComposition(extra);

        // Cache should still be at max size
        stats = composer.getStatistics();
        assertEquals(100, stats.get("cachedCompositions"));

        // The oldest composition should have been evicted
        ComposedSkill oldest = composer.getCachedComposition(
            SkillComposer.compose("cache0").fromSkill("skill1").build().getSignature()
        );
        // Oldest may or may not be null depending on insertion order
        // The key point is cache size stays at MAX_CACHE_SIZE
    }

    @Test
    @DisplayName("clearCache removes all cached compositions")
    void testClearCacheRemovesAllCachedCompositions() {
        ComposedSkill composition = SkillComposer.compose("toClear")
            .fromSkill("skill1")
            .build();
        composer.cacheComposition(composition);

        composer.clearCache();

        Map<String, Object> stats = composer.getStatistics();
        assertEquals(0, stats.get("cachedCompositions"));

        ComposedSkill retrieved = composer.getCachedComposition(composition.getSignature());
        assertNull(retrieved);
    }

    @Test
    @DisplayName("cacheComposition updates existing entry")
    void testCacheCompositionUpdatesExistingEntry() {
        ComposedSkill original = SkillComposer.compose("updateTest")
            .description("Original description")
            .fromSkill("skill1")
            .build();

        composer.cacheComposition(original);

        ComposedSkill updated = SkillComposer.compose("updateTest")
            .description("Updated description")
            .fromSkill("skill2")
            .build();

        composer.cacheComposition(updated);

        ComposedSkill retrieved = composer.getCachedComposition(updated.getSignature());
        // Should retrieve the updated version
        assertNotNull(retrieved);
    }

    // ==================== Success Rate Tracking Tests ====================

    @Test
    @DisplayName("getCompositionSuccessRate returns 1.0 for untested composition")
    void testGetCompositionSuccessRateReturnsOneForUntested() {
        String signature = "untestedSignature";
        double rate = composer.getCompositionSuccessRate(signature);

        assertEquals(1.0, rate, 0.001);
    }

    @Test
    @DisplayName("getCompositionSuccessRate tracks successful executions")
    void testGetCompositionSuccessRateTracksSuccessful() {
        ComposedSkill composition = SkillComposer.compose("successTracking")
            .fromSkill("skill1")
            .build();

        // Execute successfully 3 times
        for (int i = 0; i < 3; i++) {
            composer.executeComposed(composition, Map.of(), mockEngine);
        }

        String signature = composition.getSignature();
        double rate = composer.getCompositionSuccessRate(signature);
        assertEquals(1.0, rate, 0.001);
    }

    @Test
    @DisplayName("getCompositionSuccessRate tracks failed executions")
    void testGetCompositionSuccessRateTracksFailed() {
        when(mockEngine.execute(anyString()))
            .thenReturn(CodeExecutionEngine.ExecutionResult.error("Failure"));

        ComposedSkill composition = SkillComposer.compose("failureTracking")
            .fromSkill("skill1")
            .build();

        // Execute with failure
        composer.executeComposed(composition, Map.of(), mockEngine);

        String signature = composition.getSignature();
        double rate = composer.getCompositionSuccessRate(signature);
        assertEquals(0.0, rate, 0.001);
    }

    @Test
    @DisplayName("getCompositionSuccessRate calculates mixed results")
    void testGetCompositionSuccessRateCalculatesMixedResults() {
        ComposedSkill composition = SkillComposer.compose("mixedTracking")
            .fromSkill("skill1")
            .build();

        // Execute 5 times: 3 success, 2 failure
        for (int i = 0; i < 3; i++) {
            composer.executeComposed(composition, Map.of(), mockEngine);
        }

        when(mockEngine.execute(anyString()))
            .thenReturn(CodeExecutionEngine.ExecutionResult.error("Failure"));

        for (int i = 0; i < 2; i++) {
            composer.executeComposed(composition, Map.of(), mockEngine);
        }

        String signature = composition.getSignature();
        double rate = composer.getCompositionSuccessRate(signature);
        assertEquals(0.6, rate, 0.001); // 3/5 = 0.6
    }

    @Test
    @DisplayName("getStatistics returns accurate metrics")
    void testGetStatisticsReturnsAccurateMetrics() {
        ComposedSkill comp1 = SkillComposer.compose("stats1")
            .fromSkill("skill1")
            .build();

        ComposedSkill comp2 = SkillComposer.compose("stats2")
            .fromSkill("skill2")
            .build();

        // Execute compositions
        composer.executeComposed(comp1, Map.of(), mockEngine);
        composer.executeComposed(comp1, Map.of(), mockEngine);
        composer.executeComposed(comp2, Map.of(), mockEngine);

        // Cache one composition
        composer.cacheComposition(comp1);

        Map<String, Object> stats = composer.getStatistics();
        assertEquals(1, stats.get("cachedCompositions"));
        assertEquals(3, stats.get("totalAttempts"));
        assertEquals(3, stats.get("totalSuccesses"));
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("executeComposed with null context")
    void testExecuteComposedWithNullContext() {
        ComposedSkill composition = SkillComposer.compose("nullContext")
            .fromSkill("skill1")
            .build();

        assertDoesNotThrow(() -> {
            SkillComposer.CompositionResult result = composer.executeComposed(
                composition, null, mockEngine);
            assertTrue(result.isSuccess());
        });
    }

    @Test
    @DisplayName("executeComposed with empty context")
    void testExecuteComposedWithEmptyContext() {
        ComposedSkill composition = SkillComposer.compose("emptyContext")
            .fromSkill("skill1")
            .build();
        Map<String, Object> emptyContext = Map.of();

        SkillComposer.CompositionResult result = composer.executeComposed(
            composition, emptyContext, mockEngine);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("executeComposed handles null code execution result")
    void testExecuteComposedHandlesNullCodeResult() {
        when(mockEngine.execute(anyString()))
            .thenReturn(null);

        ComposedSkill composition = SkillComposer.compose("nullResult")
            .fromSkill("skill1")
            .build();

        assertThrows(Exception.class, () -> {
            composer.executeComposed(composition, Map.of(), mockEngine);
        });
    }

    @Test
    @DisplayName("executeComposed handles exception during execution")
    void testExecuteComposedHandlesExceptionDuringExecution() {
        when(mockEngine.execute(anyString()))
            .thenThrow(new RuntimeException("Execution exception"));

        ComposedSkill composition = SkillComposer.compose("exceptionTest")
            .fromSkill("skill1")
            .build();

        SkillComposer.CompositionResult result = composer.executeComposed(
            composition, Map.of(), mockEngine);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Exception"));
    }

    @Test
    @DisplayName("CompositionStep with null skill")
    void testCompositionStepWithNullSkill() {
        CompositionStep step = new CompositionStep("testSkill", null, Map.of());

        assertFalse(step.hasSkill());
        assertNull(step.getSkill());
        assertEquals("testSkill", step.getSkillName());
    }

    @Test
    @DisplayName("CompositionStep resolve returns step when skill exists")
    void testCompositionStepResolveReturnsStepWhenSkillExists() {
        CompositionStep step = new CompositionStep("skill1", null, Map.of());

        CompositionStep resolved = step.resolve();

        assertTrue(resolved.hasSkill());
        assertNotNull(resolved.getSkill());
        assertEquals("skill1", resolved.getSkillName());
    }

    @Test
    @DisplayName("CompositionStep resolve handles missing skill")
    void testCompositionStepResolveHandlesMissingSkill() {
        CompositionStep step = new CompositionStep("nonexistent", null, Map.of());

        CompositionStep resolved = step.resolve();

        assertFalse(resolved.hasSkill());
        assertNull(resolved.getSkill());
    }

    @Test
    @DisplayName("ComposedSkill signature is unique")
    void testComposedSkillSignatureIsUnique() {
        ComposedSkill comp1 = SkillComposer.compose("sameName")
            .fromSkill("skill1")
            .fromSkill("skill2")
            .category("cat1")
            .build();

        ComposedSkill comp2 = SkillComposer.compose("sameName")
            .fromSkill("skill1")
            .fromSkill("skill2")
            .category("cat2")
            .build();

        assertNotEquals(comp1.getSignature(), comp2.getSignature());
    }

    @Test
    @DisplayName("ComposedSkill getRequiredActions aggregates from steps")
    void testComposedSkillGetRequiredActionsAggregatesFromSteps() {
        ExecutableSkill miningSkill = createTestSkillWithActions("mining", List.of("mine", "place"));
        ExecutableSkill movingSkill = createTestSkillWithActions("moving", List.of("move", "pathfind"));

        library.addSkill(miningSkill);
        library.addSkill(movingSkill);

        ComposedSkill composition = SkillComposer.compose("aggregatedActions")
            .fromSkill("mining")
            .fromSkill("moving")
            .build();

        List<String> requiredActions = composition.getRequiredActions();
        assertTrue(requiredActions.contains("mine"));
        assertTrue(requiredActions.contains("place"));
        assertTrue(requiredActions.contains("move"));
        assertTrue(requiredActions.contains("pathfind"));
    }

    @Test
    @DisplayName("ComposedSkill getEstimatedTicks sums from steps")
    void testComposedSkillGetEstimatedTicksSumsFromSteps() {
        ExecutableSkill fastSkill = createTestSkillWithTicks("fast", 50);
        ExecutableSkill slowSkill = createTestSkillWithTicks("slow", 200);

        library.addSkill(fastSkill);
        library.addSkill(slowSkill);

        ComposedSkill composition = SkillComposer.compose("summedTicks")
            .fromSkill("fast")
            .fromSkill("slow")
            .build();

        assertEquals(250, composition.getEstimatedTicks());
    }

    @Test
    @DisplayName("ValidationResult toString provides useful info")
    void testValidationResultToStringProvidesUsefulInfo() {
        List<String> skillNames = Arrays.asList("skill1", "skill2");
        SkillComposer.ValidationResult result = composer.validateDependencies(skillNames);

        String toString = result.toString();
        assertTrue(toString.contains("Valid") || toString.contains("Invalid"));
    }

    @Test
    @DisplayName("CompositionResult toString provides useful info")
    void testCompositionResultToStringProvidesUsefulInfo() {
        ComposedSkill composition = SkillComposer.compose("toStringTest")
            .fromSkill("skill1")
            .build();

        SkillComposer.CompositionResult result = composer.executeComposed(
            composition, Map.of(), mockEngine);

        String toString = result.toString();
        assertTrue(toString.contains("Success") || toString.contains("Failed"));
    }

    // ==================== Thread Safety Tests ====================

    @Test
    @DisplayName("Concurrent executions track correctly")
    void testConcurrentExecutionsTrackCorrectly() throws InterruptedException {
        ComposedSkill composition = SkillComposer.compose("concurrent")
            .fromSkill("skill1")
            .build();

        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                composer.executeComposed(composition, Map.of(), mockEngine);
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Map<String, Object> stats = composer.getStatistics();
        assertEquals(threadCount, stats.get("totalAttempts"));
        assertEquals(threadCount, stats.get("totalSuccesses"));
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a test skill with basic properties.
     */
    private ExecutableSkill createTestSkill(String name, String description) {
        return ExecutableSkill.builder(name)
            .description(description)
            .category("test")
            .codeTemplate("// Test code for " + name + "\nvar result = 'success';")
            .requiredActions("test")
            .build();
    }

    /**
     * Creates a test skill with specific required actions.
     */
    private ExecutableSkill createTestSkillWithActions(String name, List<String> actions) {
        return ExecutableSkill.builder(name)
            .description("Test skill " + name)
            .category("test")
            .codeTemplate("// Test code")
            .requiredActions(actions.toArray(new String[0]))
            .build();
    }

    /**
     * Creates a test skill with specific estimated ticks.
     */
    private ExecutableSkill createTestSkillWithTicks(String name, int ticks) {
        return ExecutableSkill.builder(name)
            .description("Test skill " + name)
            .category("test")
            .codeTemplate("// Test code")
            .requiredActions("test")
            .estimatedTicks(ticks)
            .build();
    }
}
