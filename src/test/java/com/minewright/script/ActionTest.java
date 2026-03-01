package com.minewright.script;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

/**
 * Test suite for Action class.
 */
public class ActionTest {

    @Test
    public void testAtomicActionBuilder() {
        Action action = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("mine")
            .parameter("block", "oak_log")
            .parameter("quantity", 10)
            .build();

        assertEquals(Action.ActionType.ATOMIC, action.getType());
        assertEquals("mine", action.getCommand());
        assertEquals("oak_log", action.getParameter("block"));
        assertEquals(10, action.getIntParameter("quantity", 0));
        assertEquals(2, action.getParameters().size());
        assertTrue(action.isAtomic());
        assertFalse(action.isComposite());
        assertTrue(action.isValid());
    }

    @Test
    public void testSequenceActionBuilder() {
        Action child1 = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("move")
            .parameter("x", 10)
            .build();

        Action child2 = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("mine")
            .parameter("block", "stone")
            .build();

        Action sequence = Action.builder()
            .type(Action.ActionType.SEQUENCE)
            .addChildren(child1, child2)
            .build();

        assertEquals(Action.ActionType.SEQUENCE, sequence.getType());
        assertEquals(2, sequence.getChildren().size());
        assertFalse(sequence.isAtomic());
        assertTrue(sequence.isComposite());
        assertTrue(sequence.isValid());
    }

    @Test
    public void testLoopActionBuilder() {
        Action body = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("mine")
            .build();

        Action loop = Action.builder()
            .type(Action.ActionType.LOOP)
            .iterations(10)
            .addChild(body)
            .build();

        assertEquals(Action.ActionType.LOOP, loop.getType());
        assertEquals(10, loop.getIterations());
        assertEquals(1, loop.getChildren().size());
        assertTrue(loop.isValid());
    }

    @Test
    public void testConditionalActionBuilder() {
        Action thenBranch = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("attack")
            .build();

        Action conditional = Action.builder()
            .type(Action.ActionType.CONDITIONAL)
            .condition("health_percent() < 50")
            .addChild(thenBranch)
            .build();

        assertEquals(Action.ActionType.CONDITIONAL, conditional.getType());
        assertEquals("health_percent() < 50", conditional.getCondition());
        assertEquals(1, conditional.getChildren().size());
        assertTrue(conditional.isValid());
    }

    @Test
    public void testParameterTypes() {
        Action action = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("test")
            .parameter("string", "value")
            .parameter("integer", 42)
            .parameter("boolean", true)
            .parameter("double", 3.14)
            .build();

        assertEquals("value", action.getStringParameter("string"));
        assertEquals(42, action.getIntParameter("integer", 0));
        assertEquals(0, action.getIntParameter("nonexistent", 0));
        assertTrue(action.getBooleanParameter("boolean", false));
        assertFalse(action.getBooleanParameter("nonexistent", false));
    }

    @Test
    public void testActionValidation() {
        // Valid atomic action
        Action validAtomic = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("mine")
            .build();
        assertTrue(validAtomic.isValid());

        // Invalid atomic action (no command)
        Action invalidAtomic = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .build();
        assertFalse(invalidAtomic.isValid());

        // Valid conditional action
        Action validConditional = Action.builder()
            .type(Action.ActionType.CONDITIONAL)
            .condition("true")
            .addChild(Action.builder()
                .type(Action.ActionType.ATOMIC)
                .command("test")
                .build())
            .build();
        assertTrue(validConditional.isValid());

        // Invalid conditional action (no condition)
        Action invalidConditional = Action.builder()
            .type(Action.ActionType.CONDITIONAL)
            .build();
        assertFalse(invalidConditional.isValid());

        // Invalid loop action (no iterations)
        Action invalidLoop = Action.builder()
            .type(Action.ActionType.LOOP)
            .addChild(Action.builder()
                .type(Action.ActionType.ATOMIC)
                .command("test")
                .build())
            .build();
        assertFalse(invalidLoop.isValid());
    }

    @Test
    public void testActionCopy() {
        Action original = Action.builder()
            .type(Action.ActionType.SEQUENCE)
            .parameter("key", "value")
            .addChild(Action.builder()
                .type(Action.ActionType.ATOMIC)
                .command("test")
                .build())
            .build();

        Action copy = original.copy();

        assertEquals(original.getType(), copy.getType());
        assertEquals(original.getParameters(), copy.getParameters());
        assertEquals(original.getChildren().size(), copy.getChildren().size());
        assertEquals(original, copy);
    }

    @Test
    public void testCountNodes() {
        Action leaf1 = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("action1")
            .build();

        Action leaf2 = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("action2")
            .build();

        Action sequence = Action.builder()
            .type(Action.ActionType.SEQUENCE)
            .addChildren(leaf1, leaf2)
            .build();

        assertEquals(1, leaf1.countNodes());
        assertEquals(3, sequence.countNodes()); // sequence + 2 children
    }

    @Test
    public void testCalculateDepth() {
        Action leaf = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("action")
            .build();

        Action nested = Action.builder()
            .type(Action.ActionType.SEQUENCE)
            .addChild(Action.builder()
                .type(Action.ActionType.PARALLEL)
                .addChild(leaf)
                .build())
            .build();

        assertEquals(1, leaf.calculateDepth());
        assertEquals(3, nested.calculateDepth()); // sequence -> parallel -> atomic
    }

    @Test
    public void testToDSL() {
        Action action = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("mine")
            .parameter("block", "oak_log")
            .parameter("quantity", 10)
            .build();

        String dsl = action.toDSL(0);

        assertTrue(dsl.contains("type: \"ATOMIC\""));
        assertTrue(dsl.contains("command: \"mine\""));
        assertTrue(dsl.contains("block:"));
        assertTrue(dsl.contains("quantity:"));
    }

    @Test
    public void testBuilderFromExisting() {
        Action original = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("mine")
            .parameter("block", "stone")
            .build();

        Action modified = original.toBuilder()
            .parameter("quantity", 5)
            .build();

        assertEquals("mine", modified.getCommand());
        assertEquals("stone", modified.getParameter("block"));
        assertEquals(5, modified.getParameter("quantity"));
    }

    @Test
    public void testAddChildrenVariadic() {
        Action child1 = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("a")
            .build();

        Action child2 = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("b")
            .build();

        Action child3 = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("c")
            .build();

        Action sequence = Action.builder()
            .type(Action.ActionType.SEQUENCE)
            .addChildren(child1, child2, child3)
            .build();

        assertEquals(3, sequence.getChildren().size());
    }

    @Test
    public void testUnmodifiableCollections() {
        Action action = Action.builder()
            .type(Action.ActionType.SEQUENCE)
            .parameter("key", "value")
            .addChild(Action.builder()
                .type(Action.ActionType.ATOMIC)
                .command("test")
                .build())
            .build();

        // Test that parameters map is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> {
            action.getParameters().put("new", "value");
        });

        // Test that children list is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> {
            action.getChildren().add(Action.builder()
                .type(Action.ActionType.ATOMIC)
                .command("another")
                .build());
        });
    }

    @Test
    public void testEqualsAndHashCode() {
        Action action1 = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("mine")
            .parameter("block", "stone")
            .build();

        Action action2 = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("mine")
            .parameter("block", "stone")
            .build();

        Action action3 = Action.builder()
            .type(Action.ActionType.ATOMIC)
            .command("place")
            .build();

        assertEquals(action1, action2);
        assertEquals(action1.hashCode(), action2.hashCode());
        assertNotEquals(action1, action3);
    }
}
