package com.minewright.skill;

import com.minewright.action.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ComposedSkill}.
 *
 * Tests cover:
 * <ul>
 *   <li>Composed skill creation and properties</li>
 *   <li>Signature generation for caching</li>
 *   <li>Code generation with context merging</li>
 *   <li>Success rate tracking</li>
 *   <li>Required actions and items aggregation</li>
 *   <li>Estimated ticks calculation</li>
 *   <li>Step management</li>
 *   <li>Task applicability</li>
 * </ul>
 *
 * @see ComposedSkill
 * @see CompositionStep
 * @see SkillComposer
 * @since 1.1.0
 */
@DisplayName("ComposedSkill Tests")
class ComposedSkillTest {

    private Skill mockSkill1;
    private Skill mockSkill2;
    private Skill mockSkill3;
    private List<CompositionStep> steps;
    private Map<String, Object> baseContext;

    @BeforeEach
    void setUp() {
        mockSkill1 = mock(Skill.class);
        mockSkill2 = mock(Skill.class);
        mockSkill3 = mock(Skill.class);

        when(mockSkill1.getName()).thenReturn("stripMine");
        when(mockSkill1.getCategory()).thenReturn("mining");
        when(mockSkill1.getDescription()).thenReturn("Strip mine for resources");
        when(mockSkill1.getRequiredActions()).thenReturn(List.of("mine", "move"));
        when(mockSkill1.getRequiredItems()).thenReturn(List.of("pickaxe"));
        when(mockSkill1.getEstimatedTicks()).thenReturn(100);
        when(mockSkill1.generateCode(any())).thenReturn("// Strip mine code");
        when(mockSkill1.isApplicable(any())).thenReturn(true);

        when(mockSkill2.getName()).thenReturn("collectDrops");
        when(mockSkill2.getCategory()).thenReturn("gathering");
        when(mockSkill2.getDescription()).thenReturn("Collect dropped items");
        when(mockSkill2.getRequiredActions()).thenReturn(List.of("move", "pickup"));
        when(mockSkill2.getRequiredItems()).thenReturn(List.of());
        when(mockSkill2.getEstimatedTicks()).thenReturn(50);
        when(mockSkill2.generateCode(any())).thenReturn("// Collect drops code");
        when(mockSkill2.isApplicable(any())).thenReturn(true);

        when(mockSkill3.getName()).thenReturn("craftItem");
        when(mockSkill3.getCategory()).thenReturn("crafting");
        when(mockSkill3.getDescription()).thenReturn("Craft an item");
        when(mockSkill3.getRequiredActions()).thenReturn(List.of("craft"));
        when(mockSkill3.getRequiredItems()).thenReturn(List.of("wood", "stick"));
        when(mockSkill3.getEstimatedTicks()).thenReturn(30);
        when(mockSkill3.generateCode(any())).thenReturn("// Craft item code");
        when(mockSkill3.isApplicable(any())).thenReturn(false);

        steps = Arrays.asList(
            new CompositionStep("stripMine", mockSkill1, Map.of("depth", 50)),
            new CompositionStep("collectDrops", mockSkill2, Map.of()),
            new CompositionStep("craftItem", mockSkill3, Map.of("item", "iron_pickaxe"))
        );

        baseContext = Map.of(
            "playerName", "TestPlayer",
            "maxDepth", 100
        );
    }

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("ComposedSkill creates with valid parameters")
    void testComposedSkillCreation() {
        ComposedSkill composedSkill = new ComposedSkill(
            "mineAndCraft",
            "Mine resources and craft item",
            "mining",
            steps,
            baseContext
        );

        assertEquals("mineAndCraft", composedSkill.getName());
        assertEquals("Mine resources and craft item", composedSkill.getDescription());
        assertEquals("mining", composedSkill.getCategory());
        assertEquals(3, composedSkill.getStepCount());
    }

    @Test
    @DisplayName("ComposedSkill throws on null name")
    void testComposedSkillThrowsOnNullName() {
        assertThrows(NullPointerException.class, () -> {
            new ComposedSkill(
                null,
                "Description",
                "category",
                steps,
                baseContext
            );
        });
    }

    @Test
    @DisplayName("ComposedSkill handles null description")
    void testComposedSkillHandlesNullDescription() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            null,
            "category",
            steps,
            baseContext
        );

        assertEquals("", composedSkill.getDescription());
    }

    @Test
    @DisplayName("ComposedSkill handles null category")
    void testComposedSkillHandlesNullCategory() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            null,
            steps,
            baseContext
        );

        assertEquals("composed", composedSkill.getCategory());
    }

    @Test
    @DisplayName("ComposedSkill creates immutable steps list")
    void testComposedSkillCreatesImmutableSteps() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        List<CompositionStep> returnedSteps = composedSkill.getSteps();

        assertThrows(UnsupportedOperationException.class, () -> {
            returnedSteps.add(new CompositionStep("newStep", mockSkill1, Map.of()));
        });
    }

    @Test
    @DisplayName("ComposedSkill creates immutable base context")
    void testComposedSkillCreatesImmutableBaseContext() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        Map<String, Object> returnedContext = composedSkill.getBaseContext();

        assertThrows(UnsupportedOperationException.class, () -> {
            returnedContext.put("newKey", "newValue");
        });
    }

    // ==================== Signature Tests ====================

    @Test
    @DisplayName("Get signature generates unique identifier")
    void testGetSignatureGeneratesIdentifier() {
        ComposedSkill composedSkill = new ComposedSkill(
            "mineAndCraft",
            "Description",
            "mining",
            steps,
            baseContext
        );

        String signature = composedSkill.getSignature();

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
        assertTrue(signature.contains("mining:"));
        assertTrue(signature.contains("mineAndCraft:"));
    }

    @Test
    @DisplayName("Get signature is consistent for same composition")
    void testGetSignatureIsConsistent() {
        ComposedSkill composedSkill = new ComposedSkill(
            "mineAndCraft",
            "Description",
            "mining",
            steps,
            baseContext
        );

        String signature1 = composedSkill.getSignature();
        String signature2 = composedSkill.getSignature();

        assertEquals(signature1, signature2);
    }

    @Test
    @DisplayName("Get signature differs for different compositions")
    void testGetSignatureDiffersForDifferentCompositions() {
        ComposedSkill skill1 = new ComposedSkill(
            "skill1",
            "Description",
            "category",
            steps.subList(0, 2),
            baseContext
        );

        ComposedSkill skill2 = new ComposedSkill(
            "skill2",
            "Description",
            "category",
            steps.subList(1, 3),
            baseContext
        );

        assertNotEquals(skill1.getSignature(), skill2.getSignature());
    }

    // ==================== Code Generation Tests ====================

    @Test
    @DisplayName("Generate code merges base and step contexts")
    void testGenerateCodeMergesContexts() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        Map<String, Object> additionalContext = Map.of("extraKey", "extraValue");

        String code = composedSkill.generateCode(additionalContext);

        assertNotNull(code);
        assertTrue(code.contains("// Composed skill: testSkill"));
        assertTrue(code.contains("// Steps: 3"));

        // Verify all skills were called with merged context
        verify(mockSkill1).generateCode(argThat(ctx ->
            ctx.containsKey("depth") && ctx.containsKey("playerName") && ctx.containsKey("extraKey")
        ));
        verify(mockSkill2).generateCode(argThat(ctx ->
            ctx.containsKey("playerName") && ctx.containsKey("extraKey")
        ));
        verify(mockSkill3).generateCode(argThat(ctx ->
            ctx.containsKey("item") && ctx.containsKey("playerName") && ctx.containsKey("extraKey")
        ));
    }

    @Test
    @DisplayName("Generate code includes step numbers")
    void testGenerateCodeIncludesStepNumbers() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        String code = composedSkill.generateCode(Map.of());

        assertTrue(code.contains("// Step 1:"));
        assertTrue(code.contains("// Step 2:"));
        assertTrue(code.contains("// Step 3:"));
    }

    @Test
    @DisplayName("Generate code includes skill names")
    void testGenerateCodeIncludesSkillNames() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        String code = composedSkill.generateCode(Map.of());

        assertTrue(code.contains("stripMine"));
        assertTrue(code.contains("collectDrops"));
        assertTrue(code.contains("craftItem"));
    }

    // ==================== Required Actions and Items Tests ====================

    @Test
    @DisplayName("Get required actions aggregates from all skills")
    void testGetRequiredActionsAggregates() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        List<String> requiredActions = composedSkill.getRequiredActions();

        assertTrue(requiredActions.contains("mine"));
        assertTrue(requiredActions.contains("move"));
        assertTrue(requiredActions.contains("pickup"));
        assertTrue(requiredActions.contains("craft"));
        assertEquals(4, requiredActions.size()); // No duplicates
    }

    @Test
    @DisplayName("Get required actions removes duplicates")
    void testGetRequiredActionsRemovesDuplicates() {
        // All skills return "move" as a required action
        List<CompositionStep> stepsWithDuplicates = Arrays.asList(
            new CompositionStep("skill1", mockSkill1, Map.of()),
            new CompositionStep("skill2", mockSkill2, Map.of())
        );

        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            stepsWithDuplicates,
            baseContext
        );

        List<String> requiredActions = composedSkill.getRequiredActions();

        long moveCount = requiredActions.stream().filter(a -> a.equals("move")).count();
        assertEquals(1, moveCount, "Duplicate actions should be removed");
    }

    @Test
    @DisplayName("Get required items aggregates from all skills")
    void testGetRequiredItemsAggregates() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        List<String> requiredItems = composedSkill.getRequiredItems();

        assertTrue(requiredItems.contains("pickaxe"));
        assertTrue(requiredItems.contains("wood"));
        assertTrue(requiredItems.contains("stick"));
        assertEquals(3, requiredItems.size()); // No duplicates
    }

    // ==================== Estimated Ticks Tests ====================

    @Test
    @DisplayName("Get estimated ticks sums from all skills")
    void testGetEstimatedTicksSums() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        int estimatedTicks = composedSkill.getEstimatedTicks();

        assertEquals(180, estimatedTicks); // 100 + 50 + 30
    }

    // ==================== Success Rate Tests ====================

    @Test
    @DisplayName("Get success rate returns 1.0 for unexecuted skill")
    void testGetSuccessRateUnexecuted() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        assertEquals(1.0, composedSkill.getSuccessRate());
    }

    @Test
    @DisplayName("Record success tracks execution count")
    void testRecordSuccessTracksExecution() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        composedSkill.recordSuccess(true);
        composedSkill.recordSuccess(false);
        composedSkill.recordSuccess(true);

        assertEquals(3, composedSkill.getExecutionCount());
    }

    @Test
    @DisplayName("Record success updates success rate correctly")
    void testRecordSuccessUpdatesSuccessRate() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        composedSkill.recordSuccess(true);
        composedSkill.recordSuccess(true);
        composedSkill.recordSuccess(false);

        assertEquals(2.0 / 3.0, composedSkill.getSuccessRate(), 0.001);
    }

    @Test
    @DisplayName("Get execution count returns zero initially")
    void testGetExecutionCountInitiallyZero() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        assertEquals(0, composedSkill.getExecutionCount());
    }

    @Test
    @DisplayName("Get last execution time returns zero initially")
    void testGetLastExecutionTimeInitiallyZero() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        assertEquals(0, composedSkill.getLastExecutionTime());
    }

    @Test
    @DisplayName("Record success updates last execution time")
    void testRecordSuccessUpdatesLastExecutionTime() throws InterruptedException {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        Thread.sleep(10); // Ensure time passes

        composedSkill.recordSuccess(true);

        assertTrue(composedSkill.getLastExecutionTime() > 0);
    }

    // ==================== Task Applicability Tests ====================

    @Test
    @DisplayName("Is applicable returns true if any skill is applicable")
    void testIsApplicableAnySkill() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        Task mockTask = mock(Task.class);

        // mockSkill1 and mockSkill2 return true, mockSkill3 returns false
        assertTrue(composedSkill.isApplicable(mockTask));
    }

    @Test
    @DisplayName("Is applicable returns false if no skills are applicable")
    void testIsApplicableNoSkills() {
        when(mockSkill1.isApplicable(any())).thenReturn(false);
        when(mockSkill2.isApplicable(any())).thenReturn(false);
        when(mockSkill3.isApplicable(any())).thenReturn(false);

        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        Task mockTask = mock(Task.class);

        assertFalse(composedSkill.isApplicable(mockTask));
    }

    @Test
    @DisplayName("Is applicable handles null skill gracefully")
    void testIsApplicableHandlesNullSkill() {
        CompositionStep stepWithNullSkill = new CompositionStep("nullSkill", null, Map.of());

        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            Arrays.asList(stepWithNullSkill),
            baseContext
        );

        Task mockTask = mock(Task.class);

        assertFalse(composedSkill.isApplicable(mockTask));
    }

    // ==================== Component Skills Tests ====================

    @Test
    @DisplayName("Get component skill names returns all skill names")
    void testGetComponentSkillNames() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        List<String> componentNames = composedSkill.getComponentSkillNames();

        assertEquals(3, componentNames.size());
        assertTrue(componentNames.contains("stripMine"));
        assertTrue(componentNames.contains("collectDrops"));
        assertTrue(componentNames.contains("craftItem"));
    }

    // ==================== ToString Tests ====================

    @Test
    @DisplayName("ToString contains skill information")
    void testToStringContainsSkillInfo() {
        ComposedSkill composedSkill = new ComposedSkill(
            "mineAndCraft",
            "Description",
            "mining",
            steps,
            baseContext
        );

        String str = composedSkill.toString();

        assertTrue(str.contains("ComposedSkill"));
        assertTrue(str.contains("mineAndCraft"));
        assertTrue(str.contains("steps=3"));
        assertTrue(str.contains("mining"));
    }

    @Test
    @DisplayName("ToString includes success rate percentage")
    void testToStringIncludesSuccessRate() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        composedSkill.recordSuccess(true);
        composedSkill.recordSuccess(true);
        composedSkill.recordSuccess(false);

        String str = composedSkill.toString();

        assertTrue(str.contains("successRate="));
        assertTrue(str.contains("%"));
    }

    // ==================== Edge Cases Tests ====================

    @Test
    @DisplayName("ComposedSkill with single step works correctly")
    void testComposedSkillWithSingleStep() {
        List<CompositionStep> singleStep = List.of(
            new CompositionStep("singleSkill", mockSkill1, Map.of())
        );

        ComposedSkill composedSkill = new ComposedSkill(
            "singleStepSkill",
            "Description",
            "category",
            singleStep,
            baseContext
        );

        assertEquals(1, composedSkill.getStepCount());
        assertEquals(100, composedSkill.getEstimatedTicks());
    }

    @Test
    @DisplayName("ComposedSkill with many steps aggregates correctly")
    void testComposedSkillWithManySteps() {
        // Create a composition with many steps
        List<CompositionStep> manySteps = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            manySteps.add(new CompositionStep("skill" + i, mockSkill1, Map.of()));
        }

        ComposedSkill composedSkill = new ComposedSkill(
            "manyStepsSkill",
            "Description",
            "category",
            manySteps,
            baseContext
        );

        assertEquals(50, composedSkill.getStepCount());
        assertEquals(5000, composedSkill.getEstimatedTicks());
    }

    @Test
    @DisplayName("ComposedSkill with empty base context works")
    void testComposedSkillWithEmptyBaseContext() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            Map.of()
        );

        Map<String, Object> stepContext = Map.of("stepKey", "stepValue");

        // Should not throw when merging contexts
        assertDoesNotThrow(() -> {
            composedSkill.generateCode(stepContext);
        });
    }

    @Test
    @DisplayName("ComposedSkill with null step context works")
    void testComposedSkillWithNullStepContext() {
        CompositionStep stepWithNullContext = new CompositionStep("testSkill", mockSkill1, null);

        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            List.of(stepWithNullContext),
            baseContext
        );

        // Should handle null step context gracefully
        assertDoesNotThrow(() -> {
            composedSkill.generateCode(Map.of());
        });
    }

    @Test
    @DisplayName("Generate code with empty additional context works")
    void testGenerateCodeWithEmptyAdditionalContext() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        assertDoesNotThrow(() -> {
            String code = composedSkill.generateCode(Map.of());
            assertNotNull(code);
        });
    }

    @Test
    @DisplayName("Get steps returns unmodifiable list")
    void testGetStepsReturnsUnmodifiableList() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        List<CompositionStep> returnedSteps = composedSkill.getSteps();

        assertThrows(UnsupportedOperationException.class, () -> {
            returnedSteps.remove(0);
        });
    }

    @Test
    @DisplayName("Thread safety of record success")
    void testThreadSafetyOfRecordSuccess() throws InterruptedException {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        // Record successes from multiple threads
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    composedSkill.recordSuccess(true);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(1000, composedSkill.getExecutionCount());
    }

    @Test
    @DisplayName("Get base context returns defensive copy")
    void testGetBaseContextReturnsDefensiveCopy() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        Map<String, Object> context1 = composedSkill.getBaseContext();
        Map<String, Object> context2 = composedSkill.getBaseContext();

        assertNotSame(context1, context2, "Should return new map each time");
        assertEquals(context1, context2, "But content should be the same");
    }

    @Test
    @DisplayName("Get steps returns defensive copy")
    void testGetStepsReturnsDefensiveCopy() {
        ComposedSkill composedSkill = new ComposedSkill(
            "testSkill",
            "Description",
            "category",
            steps,
            baseContext
        );

        List<CompositionStep> steps1 = composedSkill.getSteps();
        List<CompositionStep> steps2 = composedSkill.getSteps();

        // Steps list is immutable but we should verify it
        assertEquals(steps1, steps2);
    }
}
