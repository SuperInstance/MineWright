package com.minewright.htn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link HTNPlanner}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Planner creation and configuration</li>
 *   <li>Simple decomposition (primitive tasks)</li>
 *   <li>Complex decomposition (compound tasks)</li>
 *   <li>Method selection based on preconditions</li>
 *   <li>Priority-based method ordering</li>
 *   <li>Depth limiting and loop detection</li>
 *   <li>Edge cases (no valid plan, circular decomposition)</li>
 *   <li>World state integration</li>
 *   <li>Task parameter propagation</li>
 * </ul>
 */
@DisplayName("HTNPlanner Tests")
class HTNPlannerTest {

    private HTNWorldState basicState;
    private HTNWorldState stateWithAxe;
    private HTNWorldState stateWithMaterials;
    private HTNWorldState emptyState;

    @BeforeEach
    void setUp() {
        basicState = HTNWorldState.createMutable();
        stateWithAxe = HTNWorldState.builder()
            .property("hasAxe", true)
            .property("hasWood", false)
            .build();
        stateWithMaterials = HTNWorldState.builder()
            .property("hasMaterials", true)
            .build();
        emptyState = HTNWorldState.createMutable();
    }

    @Test
    @DisplayName("Constructor with domain creates planner")
    void testConstructorWithDomain() {
        HTNDomain domain = new HTNDomain("test_domain");
        HTNPlanner planner = new HTNPlanner(domain);

        assertEquals(domain, planner.getDomain());
    }

    @Test
    @DisplayName("Constructor throws on null domain")
    void testConstructorThrowsOnNullDomain() {
        assertThrows(IllegalArgumentException.class,
            () -> new HTNPlanner(null),
            "Domain cannot be null");
    }

    @Test
    @DisplayName("Constructor with custom limits")
    void testConstructorWithCustomLimits() {
        HTNDomain domain = new HTNDomain("test_domain");
        HTNPlanner planner = new HTNPlanner(domain, 100, 5000);

        assertNotNull(planner);
        assertEquals(domain, planner.getDomain());
    }

    @Test
    @DisplayName("Decompose primitive task returns single task")
    void testDecomposePrimitiveTask() {
        HTNDomain domain = new HTNDomain("test_domain");
        HTNPlanner planner = new HTNPlanner(domain);

        HTNTask primitiveTask = HTNTask.primitive("mine")
            .parameter("blockType", "stone")
            .build();

        List<HTNTask> result = planner.decompose(primitiveTask, basicState);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("mine", result.get(0).getName());
        assertEquals("stone", result.get(0).getParameter("blockType"));
    }

    @Test
    @DisplayName("Decompose compound task with single primitive subtask")
    void testDecomposeCompoundWithSinglePrimitive() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method = HTNMethod.builder("simple_method", "simple_task")
            .subtask(HTNTask.primitive("mine")
                .parameter("blockType", "stone")
                .build())
            .build();

        domain.addMethod(method);

        HTNPlanner planner = new HTNPlanner(domain);
        HTNTask compoundTask = HTNTask.compound("simple_task").build();

        List<HTNTask> result = planner.decompose(compoundTask, basicState);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("mine", result.get(0).getName());
    }

    @Test
    @DisplayName("Decompose compound task with multiple primitive subtasks")
    void testDecomposeCompoundWithMultiplePrimitives() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method = HTNMethod.builder("gather_method", "gather_wood")
            .subtask(HTNTask.primitive("pathfind")
                .parameter("target", "tree")
                .build())
            .subtask(HTNTask.primitive("mine")
                .parameter("blockType", "oak_log")
                .parameter("count", 16)
                .build())
            .subtask(HTNTask.primitive("pathfind")
                .parameter("target", "base")
                .build())
            .build();

        domain.addMethod(method);

        HTNPlanner planner = new HTNPlanner(domain);
        HTNTask compoundTask = HTNTask.compound("gather_wood").build();

        List<HTNTask> result = planner.decompose(compoundTask, basicState);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("pathfind", result.get(0).getName());
        assertEquals("mine", result.get(1).getName());
        assertEquals("pathfind", result.get(2).getName());
    }

    @Test
    @DisplayName("Decompose nested compound tasks")
    void testDecomposeNestedCompoundTasks() {
        HTNDomain domain = new HTNDomain("test_domain");

        // Level 2: simple mining
        HTNMethod mineMethod = HTNMethod.builder("mine_method", "mine_ore")
            .subtask(HTNTask.primitive("pathfind")
                .parameter("target", "ore")
                .build())
            .subtask(HTNTask.primitive("mine")
                .parameter("blockType", "iron_ore")
                .build())
            .build();

        // Level 1: gather includes mining
        HTNMethod gatherMethod = HTNMethod.builder("gather_method", "gather_resources")
            .subtask(HTNTask.compound("mine_ore").build())
            .subtask(HTNTask.primitive("craft")
                .parameter("output", "iron_ingot")
                .build())
            .build();

        domain.addMethods(List.of(mineMethod, gatherMethod));

        HTNPlanner planner = new HTNPlanner(domain);
        HTNTask rootTask = HTNTask.compound("gather_resources").build();

        List<HTNTask> result = planner.decompose(rootTask, basicState);

        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals("pathfind", result.get(0).getName());
        assertEquals("mine", result.get(1).getName());
        assertEquals("craft", result.get(2).getName());
    }

    @Test
    @DisplayName("Method selection based on preconditions")
    void testMethodSelectionByPreconditions() {
        HTNDomain domain = new HTNDomain("test_domain");

        // High priority method: requires axe
        HTNMethod methodWithAxe = HTNMethod.builder("with_axe", "gather_wood")
            .precondition(state -> state.getBoolean("hasAxe"))
            .subtask(HTNTask.primitive("chop").build())
            .priority(100)
            .build();

        // Low priority method: fallback without axe
        HTNMethod methodWithoutAxe = HTNMethod.builder("without_axe", "gather_wood")
            .precondition(state -> true)
            .subtask(HTNTask.primitive("punch").build())
            .priority(50)
            .build();

        domain.addMethods(List.of(methodWithAxe, methodWithoutAxe));

        HTNPlanner planner = new HTNPlanner(domain);
        HTNTask task = HTNTask.compound("gather_wood").build();

        // With axe - should use high priority method
        List<HTNTask> resultWithAxe = planner.decompose(task, stateWithAxe);
        assertNotNull(resultWithAxe);
        assertEquals("chop", resultWithAxe.get(0).getName());

        // Without axe - should use fallback
        List<HTNTask> resultWithoutAxe = planner.decompose(task, emptyState);
        assertNotNull(resultWithoutAxe);
        assertEquals("punch", resultWithoutAxe.get(0).getName());
    }

    @Test
    @DisplayName("Decompose returns null when no applicable methods")
    void testDecomposeNoApplicableMethods() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method = HTNMethod.builder("impossible", "test_task")
            .precondition(state -> state.getBoolean("impossible_condition"))
            .subtask(HTNTask.primitive("mine").build())
            .build();

        domain.addMethod(method);

        HTNPlanner planner = new HTNPlanner(domain);
        HTNTask task = HTNTask.compound("test_task").build();

        List<HTNTask> result = planner.decompose(task, basicState);

        assertNull(result,
            "Should return null when no applicable methods");
    }

    @Test
    @DisplayName("Decompose returns null for unknown compound task")
    void testDecomposeUnknownCompoundTask() {
        HTNDomain domain = new HTNDomain("test_domain");
        HTNPlanner planner = new HTNPlanner(domain);

        HTNTask unknownTask = HTNTask.compound("unknown_task").build();

        List<HTNTask> result = planner.decompose(unknownTask, basicState);

        assertNull(result,
            "Should return null for unknown compound tasks");
    }

    @Test
    @DisplayName("Decompose returns null for null task")
    void testDecomposeNullTask() {
        HTNDomain domain = new HTNDomain("test_domain");
        HTNPlanner planner = new HTNPlanner(domain);

        List<HTNTask> result = planner.decompose(null, basicState);

        assertNull(result,
            "Should return null for null task");
    }

    @Test
    @DisplayName("Decompose returns null for null world state")
    void testDecomposeNullWorldState() {
        HTNDomain domain = new HTNDomain("test_domain");
        HTNPlanner planner = new HTNPlanner(domain);

        HTNTask task = HTNTask.primitive("mine").build();

        List<HTNTask> result = planner.decompose(task, null);

        assertNull(result,
            "Should return null for null world state");
    }

    @Test
    @DisplayName("Decompose with custom depth limit")
    void testDecomposeWithDepthLimit() {
        HTNDomain domain = new HTNDomain("test_domain");

        // Create deep nesting
        HTNMethod method1 = HTNMethod.builder("m1", "task1")
            .subtask(HTNTask.compound("task2").build())
            .build();

        HTNMethod method2 = HTNMethod.builder("m2", "task2")
            .subtask(HTNTask.compound("task3").build())
            .build();

        HTNMethod method3 = HTNMethod.builder("m3", "task3")
            .subtask(HTNTask.compound("task4").build())
            .build();

        HTNMethod method4 = HTNMethod.builder("m4", "task4")
            .subtask(HTNTask.primitive("action").build())
            .build();

        domain.addMethods(List.of(method1, method2, method3, method4));

        HTNPlanner planner = new HTNPlanner(domain);
        HTNTask rootTask = HTNTask.compound("task1").build();

        // Should succeed with sufficient depth
        List<HTNTask> result = planner.decompose(rootTask, basicState, 10);
        assertNotNull(result);

        // Should fail with insufficient depth
        List<HTNTask> resultLimited = planner.decompose(rootTask, basicState, 2);
        assertNull(resultLimited,
            "Should fail when depth limit exceeded");
    }

    @Test
    @DisplayName("Decompose handles method failure gracefully")
    void testDecomposeMethodFailure() {
        HTNDomain domain = new HTNDomain("test_domain");

        // Method that references undefined subtask
        HTNMethod failingMethod = HTNMethod.builder("failing", "test_task")
            .subtask(HTNTask.compound("undefined_subtask").build())
            .build();

        // Fallback method that works
        HTNMethod workingMethod = HTNMethod.builder("working", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .priority(50)
            .build();

        domain.addMethods(List.of(failingMethod, workingMethod));

        HTNPlanner planner = new HTNPlanner(domain);
        HTNTask task = HTNTask.compound("test_task").build();

        List<HTNTask> result = planner.decompose(task, basicState);

        assertNotNull(result,
            "Should fallback to working method when first fails");
        assertEquals("mine", result.get(0).getName());
    }

    @Test
    @DisplayName("CanDecompose checks task decomposability")
    void testCanDecompose() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method = HTNMethod.builder("method1", "compound_task")
            .precondition(state -> state.getBoolean("hasAxe"))
            .subtask(HTNTask.primitive("mine").build())
            .build();

        domain.addMethod(method);

        HTNPlanner planner = new HTNPlanner(domain);

        HTNTask primitiveTask = HTNTask.primitive("mine").build();
        HTNTask compoundTask = HTNTask.compound("compound_task").build();

        // Primitive tasks are always decomposable
        assertTrue(planner.canDecompose(primitiveTask, basicState));

        // Compound task with satisfied preconditions
        assertTrue(planner.canDecompose(compoundTask, stateWithAxe));

        // Compound task with unsatisfied preconditions
        assertFalse(planner.canDecompose(compoundTask, emptyState));
    }

    @Test
    @DisplayName("CanDecompose handles null inputs")
    void testCanDecomposeNullInputs() {
        HTNDomain domain = new HTNDomain("test_domain");
        HTNPlanner planner = new HTNPlanner(domain);

        HTNTask task = HTNTask.primitive("mine").build();

        assertFalse(planner.canDecompose(null, basicState));
        assertFalse(planner.canDecompose(task, null));
    }

    @Test
    @DisplayName("GetApplicableMethods returns applicable methods")
    void testGetApplicableMethods() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method1 = HTNMethod.builder("method1", "test_task")
            .precondition(state -> state.getBoolean("hasAxe"))
            .subtask(HTNTask.primitive("mine").build())
            .priority(100)
            .build();

        HTNMethod method2 = HTNMethod.builder("method2", "test_task")
            .precondition(state -> true)
            .subtask(HTNTask.primitive("craft").build())
            .priority(50)
            .build();

        domain.addMethods(List.of(method1, method2));

        HTNPlanner planner = new HTNPlanner(domain);
        HTNTask task = HTNTask.compound("test_task").build();

        // With axe - both methods applicable
        List<HTNMethod> methodsWithAxe = planner.getApplicableMethods(task, stateWithAxe);
        assertEquals(2, methodsWithAxe.size());
        assertEquals("method1", methodsWithAxe.get(0).getMethodName());

        // Without axe - only fallback
        List<HTNMethod> methodsWithoutAxe = planner.getApplicableMethods(task, emptyState);
        assertEquals(1, methodsWithoutAxe.size());
        assertEquals("method2", methodsWithoutAxe.get(0).getMethodName());
    }

    @Test
    @DisplayName("GetApplicableMethods returns empty for primitive task")
    void testGetApplicableMethodsPrimitiveTask() {
        HTNDomain domain = new HTNDomain("test_domain");
        HTNPlanner planner = new HTNPlanner(domain);

        HTNTask primitiveTask = HTNTask.primitive("mine").build();

        List<HTNMethod> methods = planner.getApplicableMethods(primitiveTask, basicState);

        assertNotNull(methods);
        assertTrue(methods.isEmpty());
    }

    @Test
    @DisplayName("GetApplicableMethods handles null inputs")
    void testGetApplicableMethodsNullInputs() {
        HTNDomain domain = new HTNDomain("test_domain");
        HTNPlanner planner = new HTNPlanner(domain);

        HTNTask task = HTNTask.compound("test_task").build();

        List<HTNMethod> nullTaskResult = planner.getApplicableMethods(null, basicState);
        List<HTNMethod> nullStateResult = planner.getApplicableMethods(task, null);

        assertNotNull(nullTaskResult);
        assertTrue(nullTaskResult.isEmpty());

        assertNotNull(nullStateResult);
        assertTrue(nullStateResult.isEmpty());
    }

    @Test
    @DisplayName("ToString contains planner information")
    void testToString() {
        HTNDomain domain = new HTNDomain("test_domain");
        HTNPlanner planner = new HTNPlanner(domain, 100, 5000);

        String str = planner.toString();

        assertTrue(str.contains("test_domain"));
        assertTrue(str.contains("maxDepth=100"));
        assertTrue(str.contains("maxIterations=5000"));
    }

    @Test
    @DisplayName("Decompose with parameter inheritance")
    void testDecomposeParameterInheritance() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method = HTNMethod.builder("method1", "build_structure")
            .subtask(HTNTask.primitive("place_block")
                .parameter("blockType", "stone")
                .build())
            .subtask(HTNTask.primitive("place_block")
                // No blockType parameter - should inherit from parent
                .build())
            .build();

        domain.addMethod(method);

        HTNPlanner planner = new HTNPlanner(domain);
        HTNTask parentTask = HTNTask.compound("build_structure")
            .parameter("blockType", "oak_planks")
            .build();

        List<HTNTask> result = planner.decompose(parentTask, basicState);

        assertNotNull(result);
        assertEquals(2, result.size());

        // First subtask has its own parameter
        assertEquals("stone", result.get(0).getParameter("blockType"));

        // Second subtask inherits from parent (no override)
        assertEquals("oak_planks", result.get(1).getParameter("blockType"));
    }

    @Test
    @DisplayName("Decompose complex multi-level hierarchy")
    void testDecomposeComplexHierarchy() {
        HTNDomain domain = new HTNDomain("test_domain");

        // Level 3: Final actions
        HTNMethod mineMethod = HTNMethod.builder("mine_ore", "mine_iron")
            .subtask(HTNTask.primitive("find_ore").build())
            .subtask(HTNTask.primitive("mine_block")
                .parameter("blockType", "iron_ore")
                .build())
            .build();

        // Level 2: Smelting
        HTNMethod smeltMethod = HTNMethod.builder("smelt_iron", "process_iron")
            .subtask(HTNTask.compound("mine_iron").build())
            .subtask(HTNTask.primitive("smelt")
                .parameter("input", "iron_ore")
                .parameter("output", "iron_ingot")
                .build())
            .build();

        // Level 1: Tool crafting
        HTNMethod craftMethod = HTNMethod.builder("craft_pickaxe", "make_tool")
            .subtask(HTNTask.compound("process_iron").build())
            .subtask(HTNTask.primitive("craft")
                .parameter("output", "iron_pickaxe")
                .build())
            .build();

        domain.addMethods(List.of(mineMethod, smeltMethod, craftMethod));

        HTNPlanner planner = new HTNPlanner(domain);
        HTNTask rootTask = HTNTask.compound("make_tool").build();

        List<HTNTask> result = planner.decompose(rootTask, basicState);

        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals("find_ore", result.get(0).getName());
        assertEquals("mine_block", result.get(1).getName());
        assertEquals("smelt", result.get(2).getName());
        assertEquals("craft", result.get(3).getName());
    }

    @Test
    @DisplayName("Decompose with alternative methods")
    void testDecomposeAlternativeMethods() {
        HTNDomain domain = new HTNDomain("test_domain");

        // Method 1: Direct approach
        HTNMethod directMethod = HTNMethod.builder("direct", "get_item")
            .precondition(state -> state.getBoolean("hasItem"))
            .subtask(HTNTask.primitive("use_item").build())
            .priority(100)
            .build();

        // Method 2: Crafting approach
        HTNMethod craftMethod = HTNMethod.builder("craft", "get_item")
            .precondition(state -> state.getBoolean("hasMaterials"))
            .subtask(HTNTask.primitive("craft_item").build())
            .priority(75)
            .build();

        // Method 3: Gathering approach (fallback)
        HTNMethod gatherMethod = HTNMethod.builder("gather", "get_item")
            .precondition(state -> true)
            .subtask(HTNTask.primitive("gather_item").build())
            .priority(50)
            .build();

        domain.addMethods(List.of(directMethod, craftMethod, gatherMethod));

        HTNPlanner planner = new HTNPlanner(domain);
        HTNTask task = HTNTask.compound("get_item").build();

        // Test with hasItem=true - should use direct
        HTNWorldState hasItem = HTNWorldState.withProperty("hasItem", true);
        List<HTNTask> result1 = planner.decompose(task, hasItem);
        assertEquals("use_item", result1.get(0).getName());

        // Test with hasMaterials=true - should use craft
        HTNWorldState hasMaterials = HTNWorldState.withProperty("hasMaterials", true);
        List<HTNTask> result2 = planner.decompose(task, hasMaterials);
        assertEquals("craft_item", result2.get(0).getName());

        // Test with neither - should use gather fallback
        List<HTNTask> result3 = planner.decompose(task, emptyState);
        assertEquals("gather_item", result3.get(0).getName());
    }

    @Test
    @DisplayName("Decompose maintains task order")
    void testDecomposeMaintainsTaskOrder() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method = HTNMethod.builder("method1", "multi_step")
            .subtask(HTNTask.primitive("step1").build())
            .subtask(HTNTask.primitive("step2").build())
            .subtask(HTNTask.primitive("step3").build())
            .subtask(HTNTask.primitive("step4").build())
            .subtask(HTNTask.primitive("step5").build())
            .build();

        domain.addMethod(method);

        HTNPlanner planner = new HTNPlanner(domain);
        HTNTask task = HTNTask.compound("multi_step").build();

        List<HTNTask> result = planner.decompose(task, basicState);

        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals("step1", result.get(0).getName());
        assertEquals("step2", result.get(1).getName());
        assertEquals("step3", result.get(2).getName());
        assertEquals("step4", result.get(3).getName());
        assertEquals("step5", result.get(4).getName());
    }

    @Test
    @DisplayName("Decompose empty method succeeds")
    void testDecomposeEmptyMethod() {
        HTNDomain domain = new HTNDomain("test_domain");

        // Method with subtasks that decompose to nothing
        HTNMethod method = HTNMethod.builder("empty", "empty_task")
            .subtask(HTNTask.primitive("noop").build())
            .build();

        domain.addMethod(method);

        HTNPlanner planner = new HTNPlanner(domain);
        HTNTask task = HTNTask.compound("empty_task").build();

        List<HTNTask> result = planner.decompose(task, basicState);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("noop", result.get(0).getName());
    }

    @Test
    @DisplayName("Planner with default domain")
    void testPlannerWithDefaultDomain() {
        HTNPlanner planner = new HTNPlanner(HTNDomain.createDefault());

        // Should successfully decompose a build_house task
        HTNTask buildHouse = HTNTask.compound("build_house")
            .parameter("material", "oak_planks")
            .build();

        List<HTNTask> result = planner.decompose(buildHouse, stateWithMaterials);

        assertNotNull(result,
            "Default domain should be able to decompose build_house");
        assertFalse(result.isEmpty(),
            "Decomposition should produce tasks");
    }

    @Test
    @DisplayName("Decompose handles complex preconditions")
    void testDecomposeComplexPreconditions() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod complexMethod = HTNMethod.builder("complex", "complex_task")
            .precondition(state ->
                state.getBoolean("hasAxe") &&
                state.getInt("woodCount") >= 50 &&
                !state.getBoolean("isNight"))
            .subtask(HTNTask.primitive("action").build())
            .build();

        domain.addMethod(complexMethod);

        HTNPlanner planner = new HTNPlanner(domain);
        HTNTask task = HTNTask.compound("complex_task").build();

        // State satisfying all conditions
        HTNWorldState validState = HTNWorldState.builder()
            .property("hasAxe", true)
            .property("woodCount", 64)
            .property("isNight", false)
            .build();

        List<HTNTask> result = planner.decompose(task, validState);
        assertNotNull(result);

        // State failing one condition
        HTNWorldState invalidState = HTNWorldState.builder()
            .property("hasAxe", true)
            .property("woodCount", 32)  // Not enough wood
            .property("isNight", false)
            .build();

        List<HTNTask> resultInvalid = planner.decompose(task, invalidState);
        assertNull(resultInvalid);
    }

    @Test
    @DisplayName("Multiple primitive tasks in sequence")
    void testMultiplePrimitiveTasksInSequence() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method = HTNMethod.builder("sequence", "sequence_task")
            .subtask(HTNTask.primitive("action1").build())
            .subtask(HTNTask.primitive("action2").build())
            .subtask(HTNTask.primitive("action3").build())
            .build();

        domain.addMethod(method);

        HTNPlanner planner = new HTNPlanner(domain);
        HTNTask task = HTNTask.compound("sequence_task").build();

        List<HTNTask> result = planner.decompose(task, basicState);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(HTNTask::isPrimitive));
    }

    @Test
    @DisplayName("Decompose with very deep hierarchy")
    void testDecomposeVeryDeepHierarchy() {
        HTNDomain domain = new HTNDomain("test_domain");

        // Create a chain of 10 levels
        for (int i = 1; i <= 10; i++) {
            String currentTask = "task" + i;
            String nextTask = i < 10 ? "task" + (i + 1) : null;

            HTNMethod.Builder builder = HTNMethod.builder("method" + i, currentTask)
                .subtask(HTNTask.primitive("action" + i).build());

            if (nextTask != null) {
                builder.subtask(HTNTask.compound(nextTask).build());
            }

            domain.addMethod(builder.build());
        }

        HTNPlanner planner = new HTNPlanner(domain, 50, 10000);
        HTNTask rootTask = HTNTask.compound("task1").build();

        List<HTNTask> result = planner.decompose(rootTask, basicState);

        assertNotNull(result);
        assertEquals(10, result.size());
    }

    @Test
    @DisplayName("Decompose with method selection priority")
    void testDecomposeMethodPrioritySelection() {
        HTNDomain domain = new HTNDomain("test_domain");

        // Three methods with different priorities
        HTNMethod lowPriority = HTNMethod.builder("low", "test_task")
            .precondition(state -> true)
            .subtask(HTNTask.primitive("low_action").build())
            .priority(10)
            .build();

        HTNMethod mediumPriority = HTNMethod.builder("medium", "test_task")
            .precondition(state -> state.getBoolean("condition"))
            .subtask(HTNTask.primitive("medium_action").build())
            .priority(50)
            .build();

        HTNMethod highPriority = HTNMethod.builder("high", "test_task")
            .precondition(state -> state.getBoolean("special"))
            .subtask(HTNTask.primitive("high_action").build())
            .priority(100)
            .build();

        domain.addMethods(List.of(lowPriority, mediumPriority, highPriority));

        HTNPlanner planner = new HTNPlanner(domain);
        HTNTask task = HTNTask.compound("test_task").build();

        // Special condition true - should use high priority
        HTNWorldState specialState = HTNWorldState.withProperty("special", true);
        List<HTNTask> result1 = planner.decompose(task, specialState);
        assertEquals("high_action", result1.get(0).getName());

        // Regular condition true - should use medium priority
        HTNWorldState regularState = HTNWorldState.withProperty("condition", true);
        List<HTNTask> result2 = planner.decompose(task, regularState);
        assertEquals("medium_action", result2.get(0).getName());

        // No conditions - should use low priority fallback
        List<HTNTask> result3 = planner.decompose(task, emptyState);
        assertEquals("low_action", result3.get(0).getName());
    }
}
