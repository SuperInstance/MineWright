package com.minewright.htn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link HTNMethod}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Builder pattern for method creation</li>
 *   <li>Precondition evaluation</li>
 *   <li>Subtask management</li>
 *   <li>Priority handling</li>
 *   <li>Method-task association</li>
 *   <li>Equality and hash code</li>
 *   <li>Edge cases and error handling</li>
 * </ul>
 */
@DisplayName("HTNMethod Tests")
class HTNMethodTest {

    private HTNWorldState basicState;

    @BeforeEach
    void setUp() {
        basicState = HTNWorldState.builder()
            .property("hasWood", true)
            .property("woodCount", 64)
            .property("hasAxe", true)
            .build();
    }

    @Test
    @DisplayName("Builder creates method with name and task name")
    void testBuilderCreatesMethod() {
        HTNMethod method = HTNMethod.builder("gather_wood_with_axe", "gather_wood")
            .build();

        assertEquals("gather_wood_with_axe", method.getMethodName());
        assertEquals("gather_wood", method.getTaskName());
    }

    @Test
    @DisplayName("Convenience builder method works")
    void testBuilderConvenienceMethod() {
        HTNMethod.Builder builder = HTNMethod.builder("test_method", "test_task");

        assertNotNull(builder);
    }

    @Test
    @DisplayName("Builder throws on null or empty method name")
    void testBuilderThrowsOnInvalidMethodName() {
        assertThrows(IllegalArgumentException.class,
            () -> HTNMethod.builder(null, "test_task")
                .subtask(HTNTask.primitive("mine").build())
                .build(),
            "Should throw when method name is null");

        assertThrows(IllegalArgumentException.class,
            () -> HTNMethod.builder("", "test_task")
                .subtask(HTNTask.primitive("mine").build())
                .build(),
            "Should throw when method name is empty");
    }

    @Test
    @DisplayName("Builder throws on null or empty task name")
    void testBuilderThrowsOnInvalidTaskName() {
        assertThrows(IllegalArgumentException.class,
            () -> HTNMethod.builder("test_method", null)
                .subtask(HTNTask.primitive("mine").build())
                .build(),
            "Should throw when task name is null");

        assertThrows(IllegalArgumentException.class,
            () -> HTNMethod.builder("test_method", "")
                .subtask(HTNTask.primitive("mine").build())
                .build(),
            "Should throw when task name is empty");
    }

    @Test
    @DisplayName("Builder throws when no subtasks defined")
    void testBuilderThrowsOnNoSubtasks() {
        assertThrows(IllegalArgumentException.class,
            () -> HTNMethod.builder("test_method", "test_task").build(),
            "Should throw when method has no subtasks");
    }

    @Test
    @DisplayName("Builder adds single subtask")
    void testBuilderAddsSingleSubtask() {
        HTNTask subtask = HTNTask.primitive("mine").build();

        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(subtask)
            .build();

        assertEquals(1, method.getSubtaskCount());
        assertEquals(subtask, method.getSubtasks().get(0));
    }

    @Test
    @DisplayName("Builder ignores null subtask")
    void testBuilderIgnoresNullSubtask() {
        HTNTask subtask = HTNTask.primitive("mine").build();

        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(null)
            .subtask(subtask)
            .subtask(null)
            .build();

        assertEquals(1, method.getSubtaskCount());
    }

    @Test
    @DisplayName("Builder adds multiple subtasks")
    void testBuilderAddsMultipleSubtasks() {
        HTNTask subtask1 = HTNTask.primitive("pathfind").build();
        HTNTask subtask2 = HTNTask.primitive("mine").build();
        HTNTask subtask3 = HTNTask.primitive("return").build();

        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(subtask1)
            .subtask(subtask2)
            .subtask(subtask3)
            .build();

        assertEquals(3, method.getSubtaskCount());
        assertEquals(subtask1, method.getSubtasks().get(0));
        assertEquals(subtask2, method.getSubtasks().get(1));
        assertEquals(subtask3, method.getSubtasks().get(2));
    }

    @Test
    @DisplayName("Builder adds subtasks from list")
    void testBuilderAddsSubtasksFromList() {
        HTNTask subtask1 = HTNTask.primitive("pathfind").build();
        HTNTask subtask2 = HTNTask.primitive("mine").build();

        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtasks(List.of(subtask1, subtask2))
            .build();

        assertEquals(2, method.getSubtaskCount());
    }

    @Test
    @DisplayName("Builder ignores null subtasks list")
    void testBuilderIgnoresNullSubtasksList() {
        HTNTask subtask = HTNTask.primitive("mine").build();

        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtasks(null)
            .subtask(subtask)
            .build();

        assertEquals(1, method.getSubtaskCount());
    }

    @Test
    @DisplayName("Subtasks list is immutable")
    void testSubtasksImmutable() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        List<HTNTask> subtasks = method.getSubtasks();

        assertThrows(UnsupportedOperationException.class,
            () -> subtasks.add(HTNTask.primitive("craft").build()),
            "Subtasks list should be immutable");
    }

    @Test
    @DisplayName("Builder sets priority")
    void testBuilderSetsPriority() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .priority(100)
            .build();

        assertEquals(100, method.getPriority());
    }

    @Test
    @DisplayName("Default priority is 0")
    void testDefaultPriority() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertEquals(0, method.getPriority());
    }

    @Test
    @DisplayName("Builder sets description")
    void testBuilderSetsDescription() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .description("Gathers wood using an axe")
            .build();

        assertEquals("Gathers wood using an axe", method.getDescription());
    }

    @Test
    @DisplayName("Description can be null")
    void testDescriptionNull() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertNull(method.getDescription());
    }

    @Test
    @DisplayName("CheckPreconditions with no preconditions returns true")
    void testCheckPreconditionsNone() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertTrue(method.checkPreconditions(basicState));
    }

    @Test
    @DisplayName("CheckPreconditions with satisfied predicate returns true")
    void testCheckPreconditionsSatisfied() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .precondition(state -> state.getBoolean("hasWood"))
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertTrue(method.checkPreconditions(basicState));
    }

    @Test
    @DisplayName("CheckPreconditions with unsatisfied predicate returns false")
    void testCheckPreconditionsUnsatisfied() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .precondition(state -> state.getBoolean("hasStone"))
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertFalse(method.checkPreconditions(basicState));
    }

    @Test
    @DisplayName("CheckPreconditions with null state returns false")
    void testCheckPreconditionsNullState() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .precondition(state -> true)
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertFalse(method.checkPreconditions(null));
    }

    @Test
    @DisplayName("CheckPreconditions handles exceptions gracefully")
    void testCheckPreconditionsException() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .precondition(state -> {
                throw new RuntimeException("Test exception");
            })
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertFalse(method.checkPreconditions(basicState));
    }

    @Test
    @DisplayName("Builder with null precondition uses default")
    void testBuilderNullPrecondition() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .precondition((java.util.function.Predicate<HTNWorldState>) null)
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertTrue(method.checkPreconditions(basicState));
    }

    @Test
    @DisplayName("Builder with simple property precondition")
    void testBuilderSimplePrecondition() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .precondition("hasWood", true)
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertTrue(method.checkPreconditions(basicState));

        HTNWorldState stateWithoutWood = HTNWorldState.withProperty("hasWood", false);
        assertFalse(method.checkPreconditions(stateWithoutWood));
    }

    @Test
    @DisplayName("Complex precondition with multiple checks")
    void testComplexPrecondition() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .precondition(state ->
                state.getBoolean("hasWood") &&
                state.getInt("woodCount") >= 64 &&
                state.getBoolean("hasAxe"))
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertTrue(method.checkPreconditions(basicState));

        HTNWorldState insufficientWood = HTNWorldState.builder()
            .property("hasWood", true)
            .property("woodCount", 32)
            .property("hasAxe", true)
            .build();
        assertFalse(method.checkPreconditions(insufficientWood));
    }

    @Test
    @DisplayName("HasPreconditions returns true when preconditions defined")
    void testHasPreconditionsTrue() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .precondition(state -> state.getBoolean("hasWood"))
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertTrue(method.hasPreconditions());
    }

    @Test
    @DisplayName("HasPreconditions returns false when no preconditions")
    void testHasPreconditionsFalse() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertFalse(method.hasPreconditions());
    }

    @Test
    @DisplayName("GetSubtaskCount returns correct count")
    void testGetSubtaskCount() {
        HTNMethod method1 = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertEquals(1, method1.getSubtaskCount());

        HTNMethod method3 = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("pathfind").build())
            .subtask(HTNTask.primitive("mine").build())
            .subtask(HTNTask.primitive("return").build())
            .build();

        assertEquals(3, method3.getSubtaskCount());
    }

    @Test
    @DisplayName("Equality based on method name and task name")
    void testEquality() {
        HTNMethod method1 = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        HTNMethod method2 = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertEquals(method1, method2);
        assertEquals(method1.hashCode(), method2.hashCode());
    }

    @Test
    @DisplayName("Inequality when method name differs")
    void testInequalityMethodName() {
        HTNMethod method1 = HTNMethod.builder("method1", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        HTNMethod method2 = HTNMethod.builder("method2", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertNotEquals(method1, method2);
    }

    @Test
    @DisplayName("Inequality when task name differs")
    void testInequalityTaskName() {
        HTNMethod method1 = HTNMethod.builder("test_method", "task1")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        HTNMethod method2 = HTNMethod.builder("test_method", "task2")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertNotEquals(method1, method2);
    }

    @Test
    @DisplayName("Equality ignores priority and subtasks")
    void testEqualityIgnoresOtherFields() {
        HTNMethod method1 = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .priority(100)
            .build();

        HTNMethod method2 = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("craft").build())
            .subtask(HTNTask.primitive("place").build())
            .priority(50)
            .build();

        assertEquals(method1, method2,
            "Only method name and task name should affect equality");
    }

    @Test
    @DisplayName("ToString contains relevant information")
    void testToString() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .subtask(HTNTask.primitive("craft").build())
            .priority(100)
            .description("Test method")
            .build();

        String str = method.toString();

        assertTrue(str.contains("test_method"));
        assertTrue(str.contains("test_task"));
        assertTrue(str.contains("100"));
        assertTrue(str.contains("2"));
        assertTrue(str.contains("Test method"));
    }

    @Test
    @DisplayName("ToDetailedString includes subtask information")
    void testToDetailedString() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("pathfind").build())
            .subtask(HTNTask.compound("gather_wood").build())
            .subtask(HTNTask.primitive("return").build())
            .build();

        String str = method.toDetailedString();

        assertTrue(str.contains("test_method"));
        assertTrue(str.contains("test_task"));
        assertTrue(str.contains("pathfind"));
        assertTrue(str.contains("PRIMITIVE"));
        assertTrue(str.contains("gather_wood"));
        assertTrue(str.contains("COMPOUND"));
        assertTrue(str.contains("return"));
    }

    @Test
    @DisplayName("Method with both primitive and compound subtasks")
    void testMixedSubtaskTypes() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("pathfind").build())
            .subtask(HTNTask.compound("gather_wood").build())
            .subtask(HTNTask.primitive("craft").build())
            .subtask(HTNTask.compound("build_house").build())
            .build();

        List<HTNTask> subtasks = method.getSubtasks();

        assertEquals(4, subtasks.size());
        assertTrue(subtasks.get(0).isPrimitive());
        assertTrue(subtasks.get(1).isCompound());
        assertTrue(subtasks.get(2).isPrimitive());
        assertTrue(subtasks.get(3).isCompound());
    }

    @Test
    @DisplayName("Method with complex subtask parameters")
    void testComplexSubtaskParameters() {
        HTNTask subtask = HTNTask.primitive("mine")
            .parameter("blockType", "oak_log")
            .parameter("count", 64)
            .parameter("useTool", true)
            .build();

        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(subtask)
            .build();

        HTNTask retrieved = method.getSubtasks().get(0);

        assertEquals("oak_log", retrieved.getParameter("blockType"));
        assertEquals(64, retrieved.getParameter("count"));
        assertEquals(true, retrieved.getParameter("useTool"));
    }

    @Test
    @DisplayName("Negative priority is allowed")
    void testNegativePriority() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .priority(-10)
            .build();

        assertEquals(-10, method.getPriority());
    }

    @Test
    @DisplayName("Precondition can check multiple state properties")
    void testPreconditionMultipleProperties() {
        HTNWorldState state = HTNWorldState.builder()
            .property("hasAxe", true)
            .property("hasPickaxe", false)
            .property("hasWood", true)
            .property("hasStone", false)
            .build();

        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .precondition(s -> s.getBoolean("hasAxe") && !s.getBoolean("hasPickaxe"))
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertTrue(method.checkPreconditions(state));
    }

    @Test
    @DisplayName("Precondition with numeric comparisons")
    void testPreconditionNumericComparison() {
        HTNWorldState state = HTNWorldState.builder()
            .property("woodCount", 75)
            .property("requiredCount", 64)
            .build();

        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .precondition(s -> s.getInt("woodCount") >= s.getInt("requiredCount"))
            .subtask(HTNTask.primitive("craft").build())
            .build();

        assertTrue(method.checkPreconditions(state));
    }

    @Test
    @DisplayName("Method with single subtask is valid")
    void testSingleSubtask() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertEquals(1, method.getSubtaskCount());
        assertFalse(method.getSubtasks().isEmpty());
    }

    @Test
    @DisplayName("Method description can be empty string")
    void testEmptyDescription() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .description("")
            .build();

        assertEquals("", method.getDescription());
    }

    @Test
    @DisplayName("Builder methods can be chained")
    void testBuilderChaining() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .description("Test")
            .precondition(state -> true)
            .subtask(HTNTask.primitive("mine").build())
            .priority(100)
            .build();

        assertNotNull(method);
        assertEquals("test_method", method.getMethodName());
        assertEquals("Test", method.getDescription());
        assertEquals(100, method.getPriority());
    }

    @Test
    @DisplayName("Subtasks maintain insertion order")
    void testSubtasksOrder() {
        HTNTask task1 = HTNTask.primitive("task1").build();
        HTNTask task2 = HTNTask.primitive("task2").build();
        HTNTask task3 = HTNTask.primitive("task3").build();

        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(task1)
            .subtask(task2)
            .subtask(task3)
            .build();

        List<HTNTask> subtasks = method.getSubtasks();

        assertEquals(task1, subtasks.get(0));
        assertEquals(task2, subtasks.get(1));
        assertEquals(task3, subtasks.get(2));
    }

    @Test
    @DisplayName("Precondition that always returns true")
    void testAlwaysTruePrecondition() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .precondition(state -> true)
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertTrue(method.checkPreconditions(basicState));
        assertTrue(method.checkPreconditions(HTNWorldState.createMutable()));
    }

    @Test
    @DisplayName("Precondition that always returns false")
    void testAlwaysFalsePrecondition() {
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .precondition(state -> false)
            .subtask(HTNTask.primitive("mine").build())
            .build();

        assertFalse(method.checkPreconditions(basicState));
    }
}
