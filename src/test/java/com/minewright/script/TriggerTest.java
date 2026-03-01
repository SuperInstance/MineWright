package com.minewright.script;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Trigger class.
 */
public class TriggerTest {

    @Test
    public void testEventTriggerBuilder() {
        Trigger trigger = Trigger.builder()
            .type(Trigger.TriggerType.EVENT)
            .condition("block_broken == \"oak_log\"")
            .cooldown(100)
            .build();

        assertEquals(Trigger.TriggerType.EVENT, trigger.getType());
        assertEquals("block_broken == \"oak_log\"", trigger.getCondition());
        assertEquals(100, trigger.getCooldown());
        assertNull(trigger.getDelay());
        assertTrue(trigger.hasCooldown());
        assertFalse(trigger.hasDelay());
        assertTrue(trigger.isValid());
    }

    @Test
    public void testConditionTriggerBuilder() {
        Trigger trigger = Trigger.builder()
            .type(Trigger.TriggerType.CONDITION)
            .condition("inventory_count(\"oak_log\") < 10")
            .delay(20)
            .cooldown(200)
            .description("Trigger when low on oak logs")
            .build();

        assertEquals(Trigger.TriggerType.CONDITION, trigger.getType());
        assertEquals("inventory_count(\"oak_log\") < 10", trigger.getCondition());
        assertEquals(20, trigger.getDelay());
        assertEquals(200, trigger.getCooldown());
        assertEquals("Trigger when low on oak logs", trigger.getDescription());
        assertTrue(trigger.hasDelay());
        assertTrue(trigger.hasCooldown());
        assertTrue(trigger.isValid());
    }

    @Test
    public void testTimeTriggerBuilder() {
        Trigger trigger = Trigger.builder()
            .type(Trigger.TriggerType.TIME)
            .condition("time_of_day() == \"day\"")
            .delay(100)
            .build();

        assertEquals(Trigger.TriggerType.TIME, trigger.getType());
        assertEquals("time_of_day() == \"day\"", trigger.getCondition());
        assertEquals(100, trigger.getDelay());
        assertNull(trigger.getCooldown());
        assertTrue(trigger.hasDelay());
        assertFalse(trigger.hasCooldown());
        assertTrue(trigger.isValid());
    }

    @Test
    public void testPlayerActionTriggerBuilder() {
        Trigger trigger = Trigger.builder()
            .type(Trigger.TriggerType.PLAYER_ACTION)
            .condition("player_action == \"broke_block\"")
            .cooldown(50)
            .build();

        assertEquals(Trigger.TriggerType.PLAYER_ACTION, trigger.getType());
        assertEquals("player_action == \"broke_block\"", trigger.getCondition());
        assertEquals(50, trigger.getCooldown());
        assertTrue(trigger.isValid());
    }

    @Test
    public void testTriggerValidation() {
        // Valid trigger
        Trigger valid = Trigger.builder()
            .type(Trigger.TriggerType.EVENT)
            .condition("test == true")
            .build();
        assertTrue(valid.isValid());

        // Invalid trigger (no type)
        assertThrows(IllegalStateException.class, () -> {
            Trigger.builder()
                .condition("test == true")
                .build();
        });

        // Invalid trigger (no condition)
        assertThrows(IllegalStateException.class, () -> {
            Trigger.builder()
                .type(Trigger.TriggerType.EVENT)
                .build();
        });

        // Invalid trigger (empty condition)
        assertThrows(IllegalStateException.class, () -> {
            Trigger.builder()
                .type(Trigger.TriggerType.EVENT)
                .condition("")
                .build();
        });
    }

    @Test
    public void testOptionalFields() {
        // Trigger with all optional fields null
        Trigger trigger = Trigger.builder()
            .type(Trigger.TriggerType.CONDITION)
            .condition("x == 5")
            .build();

        assertEquals(Trigger.TriggerType.CONDITION, trigger.getType());
        assertEquals("x == 5", trigger.getCondition());
        assertNull(trigger.getDelay());
        assertNull(trigger.getCooldown());
        assertNull(trigger.getDescription());
        assertFalse(trigger.hasDelay());
        assertFalse(trigger.hasCooldown());
    }

    @Test
    public void testTriggerCopy() {
        Trigger original = Trigger.builder()
            .type(Trigger.TriggerType.EVENT)
            .condition("event == \"test\"")
            .delay(10)
            .cooldown(50)
            .description("Test trigger")
            .build();

        Trigger copy = original.toBuilder().build();

        assertEquals(original.getType(), copy.getType());
        assertEquals(original.getCondition(), copy.getCondition());
        assertEquals(original.getDelay(), copy.getDelay());
        assertEquals(original.getCooldown(), copy.getCooldown());
        assertEquals(original.getDescription(), copy.getDescription());
        assertEquals(original, copy);
    }

    @Test
    public void testTriggerModification() {
        Trigger original = Trigger.builder()
            .type(Trigger.TriggerType.EVENT)
            .condition("x == 1")
            .cooldown(100)
            .build();

        Trigger modified = original.toBuilder()
            .condition("x == 2")
            .delay(20)
            .build();

        assertEquals("x == 2", modified.getCondition());
        assertEquals(20, modified.getDelay());
        assertEquals(100, modified.getCooldown()); // preserved
    }

    @Test
    public void testToDSL() {
        Trigger trigger = Trigger.builder()
            .type(Trigger.TriggerType.CONDITION)
            .condition("inventory_count(\"oak_log\") < 10")
            .delay(20)
            .cooldown(200)
            .description("Low on oak logs")
            .build();

        String dsl = trigger.toDSL();

        assertTrue(dsl.contains("type: \"CONDITION\""));
        assertTrue(dsl.contains("condition: \"inventory_count(\\\"oak_log\\\") < 10\""));
        assertTrue(dsl.contains("delay: 20"));
        assertTrue(dsl.contains("cooldown: 200"));
        assertTrue(dsl.contains("description: \"Low on oak logs\""));
    }

    @Test
    public void testToDSLMinimal() {
        Trigger trigger = Trigger.builder()
            .type(Trigger.TriggerType.EVENT)
            .condition("event_fired")
            .build();

        String dsl = trigger.toDSL();

        assertTrue(dsl.contains("type: \"EVENT\""));
        assertTrue(dsl.contains("condition: \"event_fired\""));
        assertFalse(dsl.contains("delay:"));
        assertFalse(dsl.contains("cooldown:"));
    }

    @Test
    public void testEqualsAndHashCode() {
        Trigger trigger1 = Trigger.builder()
            .type(Trigger.TriggerType.CONDITION)
            .condition("x == 5")
            .cooldown(100)
            .build();

        Trigger trigger2 = Trigger.builder()
            .type(Trigger.TriggerType.CONDITION)
            .condition("x == 5")
            .cooldown(100)
            .build();

        Trigger trigger3 = Trigger.builder()
            .type(Trigger.TriggerType.CONDITION)
            .condition("x == 10")
            .cooldown(100)
            .build();

        assertEquals(trigger1, trigger2);
        assertEquals(trigger1.hashCode(), trigger2.hashCode());
        assertNotEquals(trigger1, trigger3);
    }

    @Test
    public void testToString() {
        Trigger trigger = Trigger.builder()
            .type(Trigger.TriggerType.EVENT)
            .condition("test_event")
            .delay(10)
            .cooldown(50)
            .build();

        String str = trigger.toString();

        assertTrue(str.contains("EVENT"));
        assertTrue(str.contains("test_event"));
        assertTrue(str.contains("delay=10"));
        assertTrue(str.contains("cooldown=50"));
    }

    @Test
    public void testAllTriggerTypes() {
        assertEquals(4, Trigger.TriggerType.values().length);
        assertEquals(Trigger.TriggerType.EVENT, Trigger.TriggerType.valueOf("EVENT"));
        assertEquals(Trigger.TriggerType.CONDITION, Trigger.TriggerType.valueOf("CONDITION"));
        assertEquals(Trigger.TriggerType.TIME, Trigger.TriggerType.valueOf("TIME"));
        assertEquals(Trigger.TriggerType.PLAYER_ACTION, Trigger.TriggerType.valueOf("PLAYER_ACTION"));
    }
}
