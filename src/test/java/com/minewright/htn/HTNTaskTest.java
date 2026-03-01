package com.minewright.htn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link HTNTask}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Builder pattern for task creation</li>
 *   <li>Primitive and compound task types</li>
 *   <li>Parameter handling</li>
 *   <li>Task ID generation</li>
 *   <li>Cloning and parameter merging</li>
 *   <li>Conversion to action tasks</li>
 *   <li>Equality and hash code</li>
 *   <li>Edge cases and error handling</li>
 * </ul>
 */
@DisplayName("HTNTask Tests")
class HTNTaskTest {

    @Test
    @DisplayName("Builder creates primitive task with name")
    void testBuilderCreatesPrimitiveTask() {
        HTNTask task = new HTNTask.Builder()
            .name("mine")
            .type(HTNTask.Type.PRIMITIVE)
            .build();

        assertEquals("mine", task.getName());
        assertEquals(HTNTask.Type.PRIMITIVE, task.getType());
        assertTrue(task.isPrimitive());
        assertFalse(task.isCompound());
    }

    @Test
    @DisplayName("Builder creates compound task with name")
    void testBuilderCreatesCompoundTask() {
        HTNTask task = new HTNTask.Builder()
            .name("build_house")
            .type(HTNTask.Type.COMPOUND)
            .build();

        assertEquals("build_house", task.getName());
        assertEquals(HTNTask.Type.COMPOUND, task.getType());
        assertTrue(task.isCompound());
        assertFalse(task.isPrimitive());
    }

    @Test
    @DisplayName("Builder default type is COMPOUND")
    void testBuilderDefaultType() {
        HTNTask task = new HTNTask.Builder()
            .name("gather_wood")
            .build();

        assertEquals(HTNTask.Type.COMPOUND, task.getType());
        assertTrue(task.isCompound());
    }

    @Test
    @DisplayName("Primitive convenience method creates primitive task")
    void testPrimitiveConvenienceMethod() {
        HTNTask task = HTNTask.primitive("pathfind")
            .parameter("target", "base")
            .build();

        assertEquals("pathfind", task.getName());
        assertEquals(HTNTask.Type.PRIMITIVE, task.getType());
        assertTrue(task.isPrimitive());
    }

    @Test
    @DisplayName("Compound convenience method creates compound task")
    void testCompoundConvenienceMethod() {
        HTNTask task = HTNTask.compound("craft_item")
            .parameter("output", "stick")
            .parameter("count", 4)
            .build();

        assertEquals("craft_item", task.getName());
        assertEquals(HTNTask.Type.COMPOUND, task.getType());
        assertTrue(task.isCompound());
    }

    @Test
    @DisplayName("Builder throws on null or empty name")
    void testBuilderThrowsOnInvalidName() {
        assertThrows(IllegalArgumentException.class,
            () -> new HTNTask.Builder().build(),
            "Should throw when name is null");

        assertThrows(IllegalArgumentException.class,
            () -> new HTNTask.Builder().name("").build(),
            "Should throw when name is empty");

        assertThrows(IllegalArgumentException.class,
            () -> new HTNTask.Builder().name("   ").build(),
            "Should throw when name is whitespace");
    }

    @Test
    @DisplayName("Builder adds single parameter")
    void testBuilderAddsSingleParameter() {
        HTNTask task = HTNTask.primitive("mine")
            .parameter("blockType", "oak_log")
            .build();

        assertTrue(task.hasParameter("blockType"));
        assertEquals("oak_log", task.getParameter("blockType"));
    }

    @Test
    @DisplayName("Builder adds multiple parameters")
    void testBuilderAddsMultipleParameters() {
        HTNTask task = HTNTask.primitive("mine")
            .parameter("blockType", "stone")
            .parameter("count", 64)
            .parameter("useTool", true)
            .build();

        assertEquals(3, task.getParameters().size());
        assertEquals("stone", task.getParameter("blockType"));
        assertEquals(64, task.getParameter("count"));
        assertEquals(true, task.getParameter("useTool"));
    }

    @Test
    @DisplayName("Builder adds parameters from map")
    void testBuilderAddsParametersFromMap() {
        Map<String, Object> params = Map.of(
            "blockType", "oak_log",
            "count", 32,
            "byHand", false
        );

        HTNTask task = HTNTask.primitive("mine")
            .parameters(params)
            .build();

        assertEquals(3, task.getParameters().size());
        assertEquals("oak_log", task.getParameter("blockType"));
        assertEquals(32, task.getParameter("count"));
        assertEquals(false, task.getParameter("byHand"));
    }

    @Test
    @DisplayName("Builder merges parameters from multiple calls")
    void testBuilderMergesParameters() {
        HTNTask task = HTNTask.primitive("craft")
            .parameter("output", "planks")
            .parameters(Map.of("count", 4, "material", "oak"))
            .parameter("pattern", "standard")
            .build();

        assertEquals(4, task.getParameters().size());
        assertEquals("planks", task.getParameter("output"));
        assertEquals(4, task.getParameter("count"));
        assertEquals("oak", task.getParameter("material"));
        assertEquals("standard", task.getParameter("pattern"));
    }

    @Test
    @DisplayName("Parameters map is immutable")
    void testParametersMapIsImmutable() {
        HTNTask task = HTNTask.primitive("mine")
            .parameter("blockType", "stone")
            .build();

        Map<String, Object> params = task.getParameters();

        assertThrows(UnsupportedOperationException.class,
            () -> params.put("newParam", "value"),
            "Parameters map should be immutable");
    }

    @Test
    @DisplayName("Task ID is generated automatically")
    void testTaskIdGenerated() {
        HTNTask task1 = HTNTask.primitive("mine").build();
        HTNTask task2 = HTNTask.primitive("mine").build();

        assertNotNull(task1.getTaskId());
        assertNotNull(task2.getTaskId());
        assertNotEquals(task1.getTaskId(), task2.getTaskId(),
            "Each task should have unique ID");
    }

    @Test
    @DisplayName("Task ID can be set explicitly")
    void testTaskIdSetExplicitly() {
        HTNTask task = HTNTask.primitive("mine")
            .taskId("custom_task_123")
            .build();

        assertEquals("custom_task_123", task.getTaskId());
    }

    @Test
    @DisplayName("getStringParameter returns string value")
    void testGetStringParameter() {
        HTNTask task = HTNTask.primitive("mine")
            .parameter("blockType", "oak_log")
            .build();

        assertEquals("oak_log", task.getStringParameter("blockType", "default"));
    }

    @Test
    @DisplayName("getStringParameter returns default for missing key")
    void testGetStringParameterDefault() {
        HTNTask task = HTNTask.primitive("mine").build();

        assertEquals("default", task.getStringParameter("missing", "default"));
        assertNull(task.getStringParameter("missing", null));
    }

    @Test
    @DisplayName("getStringParameter converts non-string to string")
    void testGetStringParameterConversion() {
        HTNTask task = HTNTask.primitive("mine")
            .parameter("count", 64)
            .parameter("enabled", true)
            .build();

        assertEquals("64", task.getStringParameter("count", "0"));
        assertEquals("true", task.getStringParameter("enabled", "false"));
    }

    @Test
    @DisplayName("getIntParameter returns int value")
    void testGetIntParameter() {
        HTNTask task = HTNTask.primitive("mine")
            .parameter("count", 64)
            .parameter("width", 5)
            .build();

        assertEquals(64, task.getIntParameter("count", 0));
        assertEquals(5, task.getIntParameter("width", 0));
    }

    @Test
    @DisplayName("getIntParameter returns default for missing key")
    void testGetIntParameterDefault() {
        HTNTask task = HTNTask.primitive("mine").build();

        assertEquals(0, task.getIntParameter("missing", 0));
        assertEquals(100, task.getIntParameter("missing", 100));
    }

    @Test
    @DisplayName("getIntParameter converts number types")
    void testGetIntParameterConversion() {
        HTNTask task = HTNTask.primitive("mine")
            .parameter("count", 64L)
            .parameter("ratio", 0.5)
            .build();

        assertEquals(64, task.getIntParameter("count", 0));
        assertEquals(0, task.getIntParameter("ratio", 0));
    }

    @Test
    @DisplayName("getBooleanParameter returns boolean value")
    void testGetBooleanParameter() {
        HTNTask task = HTNTask.primitive("mine")
            .parameter("useTool", true)
            .parameter("byHand", false)
            .build();

        assertTrue(task.getBooleanParameter("useTool", false));
        assertFalse(task.getBooleanParameter("byHand", true));
    }

    @Test
    @DisplayName("getBooleanParameter returns default for missing key")
    void testGetBooleanParameterDefault() {
        HTNTask task = HTNTask.primitive("mine").build();

        assertTrue(task.getBooleanParameter("missing", true));
        assertFalse(task.getBooleanParameter("missing", false));
    }

    @Test
    @DisplayName("hasParameter returns true for existing parameters")
    void testHasParameter() {
        HTNTask task = HTNTask.primitive("mine")
            .parameter("blockType", "stone")
            .parameter("count", 64)
            .build();

        assertTrue(task.hasParameter("blockType"));
        assertTrue(task.hasParameter("count"));
        assertFalse(task.hasParameter("missing"));
    }

    @Test
    @DisplayName("Clone creates independent copy")
    void testCloneCreatesIndependentCopy() {
        HTNTask original = HTNTask.primitive("mine")
            .parameter("blockType", "stone")
            .build();

        HTNTask cloned = original.clone();

        assertEquals(original.getName(), cloned.getName());
        assertEquals(original.getType(), cloned.getType());
        assertEquals(original.getParameters(), cloned.getParameters());
        assertNotEquals(original.getTaskId(), cloned.getTaskId(),
            "Cloned task should have new ID");
    }

    @Test
    @DisplayName("Clone creates independent parameters map")
    void testCloneIndependentParameters() {
        HTNTask original = HTNTask.primitive("mine")
            .parameter("blockType", "stone")
            .build();

        HTNTask cloned = original.clone();

        // Original's parameters are copied but independent
        assertEquals("stone", cloned.getParameter("blockType"));
    }

    @Test
    @DisplayName("withParameters adds new parameters")
    void testWithParametersAddsNew() {
        HTNTask original = HTNTask.primitive("mine")
            .parameter("blockType", "stone")
            .build();

        HTNTask modified = original.withParameters(Map.of("count", 64));

        assertEquals("stone", modified.getParameter("blockType"));
        assertEquals(64, modified.getParameter("count"));
    }

    @Test
    @DisplayName("withParameters overrides existing parameters")
    void testWithParametersOverrides() {
        HTNTask original = HTNTask.primitive("mine")
            .parameter("blockType", "stone")
            .parameter("count", 32)
            .build();

        HTNTask modified = original.withParameters(Map.of("count", 64));

        assertEquals(64, modified.getParameter("count"));
        assertEquals("stone", modified.getParameter("blockType"));
    }

    @Test
    @DisplayName("withParameters creates new task instance")
    void testWithParametersCreatesNewInstance() {
        HTNTask original = HTNTask.primitive("mine")
            .parameter("blockType", "stone")
            .build();

        HTNTask modified = original.withParameters(Map.of("count", 64));

        assertNotEquals(original.getTaskId(), modified.getTaskId());
        assertEquals("stone", original.getParameter("blockType"));
        assertFalse(original.hasParameter("count"));
    }

    @Test
    @DisplayName("toActionTask succeeds for primitive task")
    void testToActionTaskForPrimitive() {
        HTNTask task = HTNTask.primitive("mine")
            .parameter("blockType", "oak_log")
            .parameter("count", 64)
            .build();

        com.minewright.action.Task actionTask = task.toActionTask();

        assertNotNull(actionTask);
        assertEquals("mine", actionTask.getAction());
        assertEquals("oak_log", actionTask.getParameter("blockType"));
        assertEquals(64, actionTask.getParameter("count"));
    }

    @Test
    @DisplayName("toActionTask throws for compound task")
    void testToActionTaskThrowsForCompound() {
        HTNTask task = HTNTask.compound("build_house")
            .parameter("material", "oak_planks")
            .build();

        assertThrows(IllegalStateException.class,
            task::toActionTask,
            "Cannot convert compound task to action task");
    }

    @Test
    @DisplayName("Equality based on name, type, and parameters")
    void testEquality() {
        HTNTask task1 = HTNTask.primitive("mine")
            .parameter("blockType", "stone")
            .parameter("count", 64)
            .build();

        HTNTask task2 = HTNTask.primitive("mine")
            .parameter("blockType", "stone")
            .parameter("count", 64)
            .build();

        assertEquals(task1, task2);
        assertEquals(task1.hashCode(), task2.hashCode());
    }

    @Test
    @DisplayName("Inequality when name differs")
    void testInequalityName() {
        HTNTask task1 = HTNTask.primitive("mine").build();
        HTNTask task2 = HTNTask.primitive("craft").build();

        assertNotEquals(task1, task2);
    }

    @Test
    @DisplayName("Inequality when type differs")
    void testInequalityType() {
        HTNTask primitive = HTNTask.primitive("gather").build();
        HTNTask compound = HTNTask.compound("gather").build();

        assertNotEquals(primitive, compound);
    }

    @Test
    @DisplayName("Inequality when parameters differ")
    void testInequalityParameters() {
        HTNTask task1 = HTNTask.primitive("mine")
            .parameter("blockType", "stone")
            .build();

        HTNTask task2 = HTNTask.primitive("mine")
            .parameter("blockType", "oak_log")
            .build();

        assertNotEquals(task1, task2);
    }

    @Test
    @DisplayName("Inequality when task ID differs but other fields same")
    void testEqualityIgnoresTaskId() {
        HTNTask task1 = HTNTask.primitive("mine")
            .taskId("id1")
            .parameter("blockType", "stone")
            .build();

        HTNTask task2 = HTNTask.primitive("mine")
            .taskId("id2")
            .parameter("blockType", "stone")
            .build();

        assertEquals(task1, task2,
            "Task ID should not affect equality");
    }

    @Test
    @DisplayName("toString contains all relevant information")
    void testToString() {
        HTNTask task = HTNTask.primitive("mine")
            .parameter("blockType", "stone")
            .taskId("test_id")
            .build();

        String str = task.toString();

        assertTrue(str.contains("mine"));
        assertTrue(str.contains("PRIMITIVE"));
        assertTrue(str.contains("blockType"));
        assertTrue(str.contains("stone"));
        assertTrue(str.contains("test_id"));
    }

    @Test
    @DisplayName("Empty parameters map is valid")
    void testEmptyParameters() {
        HTNTask task = HTNTask.primitive("mine").build();

        assertTrue(task.getParameters().isEmpty());
        assertFalse(task.hasParameter("anything"));
        assertNull(task.getParameter("anything"));
    }

    @Test
    @DisplayName("Null parameter value is stored")
    void testNullParameterValue() {
        HTNTask task = HTNTask.primitive("mine")
            .parameter("optional", null)
            .build();

        assertTrue(task.hasParameter("optional"));
        assertNull(task.getParameter("optional"));
        assertNull(task.getStringParameter("optional", "default"));
    }

    @Test
    @DisplayName("Builder with null parameters map does nothing")
    void testBuilderWithNullParametersMap() {
        HTNTask task = HTNTask.primitive("mine")
            .parameter("blockType", "stone")
            .parameters(null)
            .build();

        assertEquals("stone", task.getParameter("blockType"));
        assertEquals(1, task.getParameters().size());
    }

    @Test
    @DisplayName("Complex parameter types are stored")
    void testComplexParameterTypes() {
        Map<String, Object> complexMap = Map.of("nested", "value");

        HTNTask task = HTNTask.primitive("complex")
            .parameter("mapValue", complexMap)
            .parameter("listValue", java.util.List.of("a", "b", "c"))
            .build();

        assertEquals(complexMap, task.getParameter("mapValue"));
        assertEquals(java.util.List.of("a", "b", "c"), task.getParameter("listValue"));
    }

    @Test
    @DisplayName("Parameter value can be any object type")
    void testParameterAnyObjectType() {
        class CustomObject {
            final String value;
            CustomObject(String value) { this.value = value; }
        }

        CustomObject custom = new CustomObject("test");

        HTNTask task = HTNTask.primitive("custom")
            .parameter("custom", custom)
            .build();

        assertSame(custom, task.getParameter("custom"));
    }

    @Test
    @DisplayName("Multiple modifications via withParameters accumulate")
    void testMultipleWithParametersAccumulate() {
        HTNTask original = HTNTask.primitive("mine")
            .parameter("blockType", "stone")
            .build();

        HTNTask step1 = original.withParameters(Map.of("count", 32));
        HTNTask step2 = step1.withParameters(Map.of("useTool", true));

        assertEquals("stone", step2.getParameter("blockType"));
        assertEquals(32, step2.getParameter("count"));
        assertEquals(true, step2.getParameter("useTool"));
    }
}
