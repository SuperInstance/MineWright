package com.minewright.action;

import com.minewright.testutil.TaskBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Task}.
 *
 * Tests cover:
 * <ul>
 *   <li>Task creation with action and parameters</li>
 *   <li>Parameter extraction methods</li>
 *   <li>Type-safe parameter getters</li>
 *   <li>Parameter presence checking</li>
 *   <li>toString output</li>
 * </ul>
 */
@DisplayName("Task Tests")
class TaskTest {

    @Test
    @DisplayName("Task creation with action and parameters")
    void testTaskCreation() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("quantity", 10);

        Task task = new Task("mine", params);

        assertEquals("mine", task.getAction(), "Action should match constructor argument");
        assertEquals(params, task.getParameters(), "Parameters should match constructor argument");
    }

    @Test
    @DisplayName("Task creation with empty parameters")
    void testTaskCreationWithEmptyParameters() {
        Task task = new Task("move", new HashMap<>());

        assertEquals("move", task.getAction());
        assertTrue(task.getParameters().isEmpty(), "Parameters map should be empty");
    }

    @Test
    @DisplayName("Get parameter returns correct value")
    void testGetParameter() {
        Map<String, Object> params = new HashMap<>();
        params.put("target", "zombie");
        params.put("count", 5);

        Task task = new Task("attack", params);

        assertEquals("zombie", task.getParameter("target"));
        assertEquals(5, task.getParameter("count"));
    }

    @Test
    @DisplayName("Get parameter returns null for missing key")
    void testGetParameterMissingKey() {
        Map<String, Object> params = new HashMap<>();
        params.put("existing", "value");

        Task task = new Task("action", params);

        assertNull(task.getParameter("nonexistent"),
                "Should return null for missing parameter");
    }

    @Test
    @DisplayName("GetStringParameter returns string value")
    void testGetStringParameter() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "Steve");
        params.put("block", "oak_log");

        Task task = new Task("build", params);

        assertEquals("Steve", task.getStringParameter("name"));
        assertEquals("oak_log", task.getStringParameter("block"));
    }

    @Test
    @DisplayName("GetStringParameter converts non-string to string")
    void testGetStringParameterConversion() {
        Map<String, Object> params = new HashMap<>();
        params.put("number", 42);
        params.put("bool", true);

        Task task = new Task("craft", params);

        assertEquals("42", task.getStringParameter("number"),
                "Should convert number to string");
        assertEquals("true", task.getStringParameter("bool"),
                "Should convert boolean to string");
    }

    @Test
    @DisplayName("GetStringParameter returns null for missing key")
    void testGetStringParameterNull() {
        Map<String, Object> params = new HashMap<>();
        Task task = new Task("action", params);

        assertNull(task.getStringParameter("missing"),
                "Should return null for missing parameter");
    }

    @Test
    @DisplayName("GetStringParameterWithDefault returns default for missing key")
    void testGetStringParameterWithDefault() {
        Map<String, Object> params = new HashMap<>();
        Task task = new Task("action", params);

        String defaultValue = "default_value";
        assertEquals(defaultValue, task.getStringParameter("missing", defaultValue),
                "Should return default value for missing parameter");
    }

    @Test
    @DisplayName("GetStringParameterWithDefault returns value if present")
    void testGetStringParameterWithDefaultValuePresent() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "dirt");

        Task task = new Task("action", params);

        assertEquals("dirt", task.getStringParameter("block", "stone"),
                "Should return actual value, not default");
    }

    @Test
    @DisplayName("GetStringParameterWithDefault handles null values")
    void testGetStringParameterWithDefaultNullValue() {
        Map<String, Object> params = new HashMap<>();
        params.put("nullable", null);

        Task task = new Task("action", params);

        assertEquals("default", task.getStringParameter("nullable", "default"),
                "Should return default when value is null");
    }

    @Test
    @DisplayName("GetIntParameter returns integer value")
    void testGetIntParameter() {
        Map<String, Object> params = new HashMap<>();
        params.put("count", 42);
        params.put("x", 100);
        params.put("y", -5);

        Task task = new Task("place", params);

        assertEquals(42, task.getIntParameter("count", 0));
        assertEquals(100, task.getIntParameter("x", 0));
        assertEquals(-5, task.getIntParameter("y", 0));
    }

    @Test
    @DisplayName("GetIntParameter converts Number to int")
    void testGetIntParameterConversion() {
        Map<String, Object> params = new HashMap<>();
        params.put("doubleValue", 3.14);
        params.put("longValue", 100000L);
        params.put("shortValue", (short) 10);

        Task task = new Task("action", params);

        assertEquals(3, task.getIntParameter("doubleValue", 0),
                "Should convert double to int (truncated)");
        assertEquals(100000, task.getIntParameter("longValue", 0),
                "Should convert long to int");
        assertEquals(10, task.getIntParameter("shortValue", 0),
                "Should convert short to int");
    }

    @Test
    @DisplayName("GetIntParameter returns default for non-number values")
    void testGetIntParameterNonNumber() {
        Map<String, Object> params = new HashMap<>();
        params.put("string", "not a number");
        params.put("bool", true);

        Task task = new Task("action", params);

        assertEquals(99, task.getIntParameter("string", 99),
                "Should return default for string value");
        assertEquals(88, task.getIntParameter("bool", 88),
                "Should return default for boolean value");
    }

    @Test
    @DisplayName("GetIntParameter returns default for missing key")
    void testGetIntParameterMissing() {
        Map<String, Object> params = new HashMap<>();
        Task task = new Task("action", params);

        assertEquals(42, task.getIntParameter("missing", 42),
                "Should return default for missing parameter");
    }

    @Test
    @DisplayName("HasParameters returns true when all parameters present")
    void testHasParametersAllPresent() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 10);
        params.put("y", 20);
        params.put("z", 30);
        params.put("block", "stone");

        Task task = new Task("action", params);

        assertTrue(task.hasParameters("x", "y", "z"),
                "Should return true when all parameters are present");
    }

    @Test
    @DisplayName("HasParameters returns false when any parameter missing")
    void testHasParametersOneMissing() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 10);
        params.put("y", 20);

        Task task = new Task("action", params);

        assertFalse(task.hasParameters("x", "y", "z"),
                "Should return false when 'z' is missing");
    }

    @Test
    @DisplayName("HasParameters returns false when all parameters missing")
    void testHasParametersAllMissing() {
        Map<String, Object> params = new HashMap<>();
        Task task = new Task("action", params);

        assertFalse(task.hasParameters("x", "y", "z"),
                "Should return false when no parameters are present");
    }

    @Test
    @DisplayName("HasParameters with single parameter")
    void testHasParametersSingle() {
        Map<String, Object> params = new HashMap<>();
        params.put("action", "mine");

        Task task = new Task("action", params);

        assertTrue(task.hasParameters("action"),
                "Should return true for single present parameter");
        assertFalse(task.hasParameters("missing"),
                "Should return false for single missing parameter");
    }

    @Test
    @DisplayName("HasParameters with no arguments returns true")
    void testHasParametersNoArguments() {
        Map<String, Object> params = new HashMap<>();
        Task task = new Task("action", params);

        assertTrue(task.hasParameters(),
                "Should return true when checking no parameters");
    }

    @Test
    @DisplayName("ToString contains action and parameters")
    void testToString() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("quantity", 5);

        Task task = new Task("mine", params);
        String result = task.toString();

        assertTrue(result.contains("mine"),
                "toString should contain the action");
        assertTrue(result.contains("block"),
                "toString should contain parameter keys");
        assertTrue(result.contains("stone"),
                "toString should contain parameter values");
        assertTrue(result.contains("Task{"),
                "toString should follow format");
    }

    @Test
    @DisplayName("Task with null parameter value")
    void testTaskWithNullParameterValue() {
        Map<String, Object> params = new HashMap<>();
        params.put("present", "value");
        params.put("nullable", null);

        Task task = new Task("action", params);

        assertTrue(task.hasParameters("present"),
                "Should have present parameter");
        assertTrue(task.hasParameters("nullable"),
                "Should have nullable parameter (even if null)");
        assertNull(task.getParameter("nullable"),
                "Null parameter should return null");
        assertEquals("default", task.getStringParameter("nullable", "default"),
                "Null parameter should use default");
    }

    @Test
    @DisplayName("Task with complex parameter types")
    void testTaskWithComplexParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("stringList", java.util.List.of("a", "b", "c"));
        params.put("intArray", new int[]{1, 2, 3});
        params.put("nestedMap", Map.of("key", "value"));

        Task task = new Task("complex", params);

        assertEquals(java.util.List.of("a", "b", "c"), task.getParameter("stringList"));
        assertArrayEquals(new int[]{1, 2, 3}, (int[]) task.getParameter("intArray"));
        assertEquals(Map.of("key", "value"), task.getParameter("nestedMap"));
    }

    @Test
    @DisplayName("Task preserves parameter map reference")
    void testTaskPreservesParameterReference() {
        Map<String, Object> params = new HashMap<>();
        params.put("value", 1);

        Task task = new Task("action", params);

        // Modifying original map should affect task's parameters
        // (they share the same reference)
        params.put("value", 2);

        assertEquals(2, task.getParameter("value"),
                "Task should use the same map reference");
    }

    @Test
    @DisplayName("Task using TaskBuilder from testutil")
    void testTaskWithBuilder() {
        Task task = TaskBuilder.aTask("build")
                .withBlock("oak_planks")
                .withQuantity(100)
                .withPosition(10, 64, 20)
                .build();

        assertEquals("build", task.getAction());
        assertEquals("oak_planks", task.getParameter("block"));
        assertEquals(100, task.getParameter("quantity"));
        assertEquals(10, task.getParameter("x"));
        assertEquals(64, task.getParameter("y"));
        assertEquals(20, task.getParameter("z"));
    }

    @Test
    @DisplayName("Task using TaskBuilder Presets")
    void testTaskWithBuilderPresets() {
        Task miningTask = TaskBuilder.Presets.mineStone(64);
        assertEquals("mine", miningTask.getAction());
        assertEquals("stone", miningTask.getParameter("block"));
        assertEquals(64, miningTask.getParameter("quantity"));

        Task placeTask = TaskBuilder.Presets.placeBlock("dirt", 100, 64, -50);
        assertEquals("place", placeTask.getAction());
        assertEquals("dirt", placeTask.getParameter("block"));
        assertEquals(100, placeTask.getParameter("x"));
        assertEquals(64, placeTask.getParameter("y"));
        assertEquals(-50, placeTask.getParameter("z"));

        Task pathfindTask = TaskBuilder.Presets.pathfindTo(0, 64, 0);
        assertEquals("pathfind", pathfindTask.getAction());
        assertEquals(0, pathfindTask.getParameter("x"));
        assertEquals(64, pathfindTask.getParameter("y"));
        assertEquals(0, pathfindTask.getParameter("z"));
    }

    @Test
    @DisplayName("Edge case: Zero and negative integer parameters")
    void testEdgeCaseIntegerParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("zero", 0);
        params.put("negative", -100);

        Task task = new Task("action", params);

        assertEquals(0, task.getIntParameter("zero", 999));
        assertEquals(-100, task.getIntParameter("negative", 999));
    }

    @Test
    @DisplayName("Edge case: Empty string parameters")
    void testEdgeCaseEmptyStringParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("empty", "");

        Task task = new Task("action", params);

        assertEquals("", task.getStringParameter("empty"));
        assertEquals("", task.getStringParameter("empty", "default"));
    }

    @Test
    @DisplayName("Multiple tasks with same action but different parameters")
    void testMultipleTasksDifferentParameters() {
        Task task1 = new Task("move", Map.of("direction", "north", "distance", 10));
        Task task2 = new Task("move", Map.of("direction", "south", "distance", 5));

        assertEquals(task1.getAction(), task2.getAction(),
                "Actions should be the same");
        assertNotEquals(task1.getParameters(), task2.getParameters(),
                "Parameters should be different");
        assertEquals("north", task1.getParameter("direction"));
        assertEquals("south", task2.getParameter("direction"));
    }
}
