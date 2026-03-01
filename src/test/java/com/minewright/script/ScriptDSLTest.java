package com.minewright.script;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

/**
 * Test suite for ScriptDSL class.
 */
public class ScriptDSLTest {

    @Test
    public void testTriggerTypeValues() {
        assertEquals(4, ScriptDSL.TriggerType.values().length);
        assertEquals(ScriptDSL.TriggerType.EVENT, ScriptDSL.TriggerType.valueOf("EVENT"));
        assertEquals(ScriptDSL.TriggerType.CONDITION, ScriptDSL.TriggerType.valueOf("CONDITION"));
        assertEquals(ScriptDSL.TriggerType.TIME, ScriptDSL.TriggerType.valueOf("TIME"));
        assertEquals(ScriptDSL.TriggerType.PLAYER_ACTION, ScriptDSL.TriggerType.valueOf("PLAYER_ACTION"));
    }

    @Test
    public void testActionTypeValues() {
        assertEquals(5, ScriptDSL.ActionType.values().length);
        assertEquals(ScriptDSL.ActionType.SEQUENCE, ScriptDSL.ActionType.valueOf("SEQUENCE"));
        assertEquals(ScriptDSL.ActionType.PARALLEL, ScriptDSL.ActionType.valueOf("PARALLEL"));
        assertEquals(ScriptDSL.ActionType.CONDITIONAL, ScriptDSL.ActionType.valueOf("CONDITIONAL"));
        assertEquals(ScriptDSL.ActionType.LOOP, ScriptDSL.ActionType.valueOf("LOOP"));
        assertEquals(ScriptDSL.ActionType.ATOMIC, ScriptDSL.ActionType.valueOf("ATOMIC"));
    }

    @Test
    public void testBuiltinFunctions() {
        assertEquals(16, ScriptDSL.BuiltinFunction.values().length);

        // Test inventory functions
        assertTrue(ScriptDSL.BuiltinFunction.INVENTORY_COUNT.getFunctionName().equals("inventory_count"));
        assertTrue(ScriptDSL.BuiltinFunction.INVENTORY_HAS.getFunctionName().equals("inventory_has"));

        // Test distance functions
        assertTrue(ScriptDSL.BuiltinFunction.DISTANCE_TO.getFunctionName().equals("distance_to"));
        assertTrue(ScriptDSL.BuiltinFunction.DISTANCE_TO_NEAREST.getFunctionName().equals("distance_to_nearest"));

        // Test entity functions
        assertTrue(ScriptDSL.BuiltinFunction.HEALTH_PERCENT.getFunctionName().equals("health_percent"));
    }

    @Test
    public void testAtomicCommands() {
        assertEquals(16, ScriptDSL.AtomicCommand.values().length);

        // Test block interaction commands
        assertTrue(ScriptDSL.AtomicCommand.fromString("mine").isPresent());
        assertTrue(ScriptDSL.AtomicCommand.fromString("place").isPresent());

        // Test case insensitivity
        assertTrue(ScriptDSL.AtomicCommand.fromString("MINE").isPresent());
        assertTrue(ScriptDSL.AtomicCommand.fromString("Mine").isPresent());

        // Test invalid command
        assertTrue(ScriptDSL.AtomicCommand.fromString("invalid_command").isEmpty());
    }

    @Test
    public void testSchemaValidation() {
        // Test value type validation
        assertTrue(ScriptDSL.Schema.isValidValueType("string"));
        assertTrue(ScriptDSL.Schema.isValidValueType(123));
        assertTrue(ScriptDSL.Schema.isValidValueType(true));
        assertFalse(ScriptDSL.Schema.isValidValueType(new Object()));

        // Test string length validation
        assertTrue(ScriptDSL.Schema.isValidStringLength("short"));
        assertTrue(ScriptDSL.Schema.isValidStringLength(""));
        assertFalse(ScriptDSL.Schema.isValidStringLength("x".repeat(1001)));

        // Test iteration count validation
        assertTrue(ScriptDSL.Schema.isValidIterationCount(10));
        assertTrue(ScriptDSL.Schema.isValidIterationCount(1));
        assertFalse(ScriptDSL.Schema.isValidIterationCount(0));
        assertFalse(ScriptDSL.Schema.isValidIterationCount(-1));
        assertFalse(ScriptDSL.Schema.isValidIterationCount(1001));
        assertFalse(ScriptDSL.Schema.isValidIterationCount("not a number"));
    }

    @Test
    public void testSchemaConstants() {
        assertEquals(20, ScriptDSL.Schema.getMaxDepth());
        assertEquals(500, ScriptDSL.Schema.getMaxNodes());
        assertEquals(1000, ScriptDSL.Schema.getMaxLoopIterations());
        assertEquals(1000, ScriptDSL.Schema.getMaxLength());
    }

    @Test
    public void testReservedWords() {
        assertTrue(ScriptDSL.RESERVED_WORDS.contains("true"));
        assertTrue(ScriptDSL.RESERVED_WORDS.contains("false"));
        assertTrue(ScriptDSL.RESERVED_WORDS.contains("if"));
        assertTrue(ScriptDSL.RESERVED_WORDS.contains("while"));

        assertFalse(ScriptDSL.RESERVED_WORDS.contains("myVariable"));
        assertFalse(ScriptDSL.RESERVED_WORDS.contains("valid_name"));
    }

    @Test
    public void testValidOperators() {
        assertTrue(ScriptDSL.VALID_OPERATORS.contains("=="));
        assertTrue(ScriptDSL.VALID_OPERATORS.contains("!="));
        assertTrue(ScriptDSL.VALID_OPERATORS.contains("<"));
        assertTrue(ScriptDSL.VALID_OPERATORS.contains("and"));
        assertTrue(ScriptDSL.VALID_OPERATORS.contains("or"));

        assertFalse(ScriptDSL.VALID_OPERATORS.contains("invalid"));
        assertFalse(ScriptDSL.VALID_OPERATORS.contains("xor"));
    }

    @Test
    public void testVariableNameValidation() {
        // Valid names
        assertTrue(ScriptDSL.isValidVariableName("myVar"));
        assertTrue(ScriptDSL.isValidVariableName("_private"));
        assertTrue(ScriptDSL.isValidVariableName("camelCase"));
        assertTrue(ScriptDSL.isValidVariableName("var123"));
        assertTrue(ScriptDSL.isValidVariableName("_"));
        assertTrue(ScriptDSL.isValidVariableName("a"));

        // Invalid names
        assertFalse(ScriptDSL.isValidVariableName(null));
        assertFalse(ScriptDSL.isValidVariableName(""));
        assertFalse(ScriptDSL.isValidVariableName("123var")); // starts with number
        assertFalse(ScriptDSL.isValidVariableName("my-var")); // contains dash
        assertFalse(ScriptDSL.isValidVariableName("my var")); // contains space
        assertFalse(ScriptDSL.isValidVariableName("true")); // reserved word
        assertFalse(ScriptDSL.isValidVariableName("if")); // reserved word
    }

    @Test
    public void testConditionExpressionValidation() {
        // Valid expressions
        assertTrue(ScriptDSL.isValidConditionExpression("x == 5"));
        assertTrue(ScriptDSL.isValidConditionExpression("inventory_count(\"oak_log\") > 10"));
        assertTrue(ScriptDSL.isValidConditionExpression("(a and b) or c"));
        assertTrue(ScriptDSL.isValidConditionExpression("((a))"));

        // Invalid expressions
        assertFalse(ScriptDSL.isValidConditionExpression(null));
        assertFalse(ScriptDSL.isValidConditionExpression(""));
        assertFalse(ScriptDSL.isValidConditionExpression("(a == b")); // unbalanced
        assertFalse(ScriptDSL.isValidConditionExpression("a == b)")); // unbalanced
    }

    @Test
    public void testCreateTemplate() {
        Map<String, Object> template = ScriptDSL.createTemplate();

        assertNotNull(template);
        assertEquals("script_template", template.get("id"));
        assertEquals("Script Template", template.get("name"));
        assertEquals("A template for creating scripts", template.get("description"));
        assertEquals("1.0.0", template.get("version"));
        assertNotNull(template.get("triggers"));
        assertNotNull(template.get("variables"));
        assertNotNull(template.get("actions"));
    }
}
