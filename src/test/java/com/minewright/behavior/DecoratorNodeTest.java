package com.minewright.behavior;

import com.minewright.behavior.decorator.CooldownNode;
import com.minewright.behavior.decorator.InverterNode;
import com.minewright.behavior.decorator.RepeaterNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for behavior tree decorator nodes.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>{@link InverterNode} - inverts child result</li>
 *   <li>{@link RepeaterNode} - repeats child N times</li>
 *   <li>{@link CooldownNode} - limits execution frequency</li>
 * </ul>
 */
@DisplayName("Decorator Node Tests")
class DecoratorNodeTest {

    private BTBlackboard blackboard;

    @BeforeEach
    void setUp() {
        blackboard = createMockBlackboard();
    }

    /**
     * Creates a test blackboard without needing a real ForemanEntity.
     * Uses sun.misc.Unsafe to allocate instance bypassing constructor.
     */
    private BTBlackboard createMockBlackboard() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            Object unsafe = theUnsafeField.get(null);
            java.lang.reflect.Method allocateInstance =
                unsafeClass.getMethod("allocateInstance", Class.class);
            BTBlackboard bb = (BTBlackboard) allocateInstance.invoke(unsafe, BTBlackboard.class);

            // Initialize the data field directly
            Field dataField = BTBlackboard.class.getDeclaredField("data");
            dataField.setAccessible(true);
            dataField.set(bb, new java.util.concurrent.ConcurrentHashMap<>());

            return bb;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test blackboard using Unsafe", e);
        }
    }

    // ------------------------------------------------------------------------
    // InverterNode Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("InverterNode: SUCCESS becomes FAILURE")
    void testInverterSuccessToFailure() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        InverterNode inverter = new InverterNode(child);

        NodeStatus result = inverter.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "SUCCESS should be inverted to FAILURE");
    }

    @Test
    @DisplayName("InverterNode: FAILURE becomes SUCCESS")
    void testInverterFailureToSuccess() {
        MockNode child = new MockNode(NodeStatus.FAILURE);
        InverterNode inverter = new InverterNode(child);

        NodeStatus result = inverter.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "FAILURE should be inverted to SUCCESS");
    }

    @Test
    @DisplayName("InverterNode: RUNNING stays RUNNING")
    void testInverterRunningStaysRunning() {
        MockNode child = new MockNode(NodeStatus.RUNNING);
        InverterNode inverter = new InverterNode(child);

        NodeStatus result = inverter.tick(blackboard);

        assertEquals(NodeStatus.RUNNING, result,
            "RUNNING should stay RUNNING");
    }

    @Test
    @DisplayName("InverterNode: double inversion returns original")
    void testInverterDoubleInversion() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        InverterNode inverter1 = new InverterNode(child);
        InverterNode inverter2 = new InverterNode(inverter1);

        NodeStatus result = inverter2.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "Double inversion should return original status");
    }

    @Test
    @DisplayName("InverterNode: reset propagates to child")
    void testInverterReset() {
        MockNode child = new MockNode(NodeStatus.RUNNING);
        InverterNode inverter = new InverterNode(child);

        inverter.tick(blackboard);
        inverter.reset();

        assertTrue(child.wasReset(),
            "Child should be reset");
    }

    @Test
    @DisplayName("InverterNode: isComplete delegates to child")
    void testInverterIsComplete() {
        MockNode successChild = new MockNode(NodeStatus.SUCCESS);
        MockNode runningChild = new MockNode(NodeStatus.RUNNING);

        InverterNode inverter1 = new InverterNode(successChild);
        InverterNode inverter2 = new InverterNode(runningChild);

        inverter1.tick(blackboard);
        inverter2.tick(blackboard);

        assertTrue(inverter1.isComplete(),
            "Should be complete when child is complete");
        assertFalse(inverter2.isComplete(),
            "Should not be complete when child is running");
    }

    @Test
    @DisplayName("InverterNode: constructor throws on null child")
    void testInverterThrowsOnNullChild() {
        assertThrows(IllegalArgumentException.class,
            () -> new InverterNode(null),
            "Should throw on null child");
    }

    @Test
    @DisplayName("InverterNode: named inverter has correct name")
    void testInverterNamed() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        InverterNode inverter = new InverterNode("TestInverter", child);

        assertEquals("TestInverter", inverter.getName(),
            "Named inverter should have correct name");
    }

    @Test
    @DisplayName("InverterNode: getChild returns child")
    void testInverterGetChild() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        InverterNode inverter = new InverterNode(child);

        assertEquals(child, inverter.getChild(),
            "Should return the child node");
    }

    // ------------------------------------------------------------------------
    // RepeaterNode Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("RepeaterNode: repeat 3 times requires 3 successes")
    void testRepeaterThreeTimes() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        RepeaterNode repeater = new RepeaterNode(child, 3);

        // First tick
        NodeStatus result1 = repeater.tick(blackboard);
        assertEquals(NodeStatus.RUNNING, result1,
            "First success should return RUNNING (count 1/3)");
        assertEquals(1, repeater.getCurrentCount());

        // Second tick
        NodeStatus result2 = repeater.tick(blackboard);
        assertEquals(NodeStatus.RUNNING, result2,
            "Second success should return RUNNING (count 2/3)");
        assertEquals(2, repeater.getCurrentCount());

        // Third tick
        NodeStatus result3 = repeater.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result3,
            "Third success should return SUCCESS (count 3/3)");
        assertEquals(3, repeater.getCurrentCount());
        assertTrue(repeater.isComplete(),
            "Should be complete after reaching target");
    }

    @Test
    @DisplayName("RepeaterNode: failure resets count and returns FAILURE")
    void testRepeaterFailure() {
        MockNode child = new MockNode(NodeStatus.SUCCESS, NodeStatus.FAILURE);
        RepeaterNode repeater = new RepeaterNode(child, 3);

        // First tick succeeds
        NodeStatus result1 = repeater.tick(blackboard);
        assertEquals(NodeStatus.RUNNING, result1);
        assertEquals(1, repeater.getCurrentCount());

        // Second tick fails
        NodeStatus result2 = repeater.tick(blackboard);
        assertEquals(NodeStatus.FAILURE, result2,
            "Failure should return FAILURE");
        assertEquals(0, repeater.getCurrentCount(),
            "Count should reset to 0");
    }

    @Test
    @DisplayName("RepeaterNode: RUNNING child returns RUNNING")
    void testRepeaterRunningChild() {
        MockNode child = new MockNode(NodeStatus.RUNNING, NodeStatus.SUCCESS);
        RepeaterNode repeater = new RepeaterNode(child, 2);

        // First tick: child runs
        NodeStatus result1 = repeater.tick(blackboard);
        assertEquals(NodeStatus.RUNNING, result1);
        assertEquals(0, repeater.getCurrentCount(),
            "Count should not increment while child is running");

        // Second tick: child succeeds
        NodeStatus result2 = repeater.tick(blackboard);
        assertEquals(NodeStatus.RUNNING, result2,
            "Should continue running (count 1/2)");
        assertEquals(1, repeater.getCurrentCount());
    }

    @Test
    @DisplayName("RepeaterNode: reset clears count and resets child")
    void testRepeaterReset() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        RepeaterNode repeater = new RepeaterNode(child, 3);

        // Tick to advance count
        repeater.tick(blackboard);
        assertEquals(1, repeater.getCurrentCount());

        // Reset
        repeater.reset();

        assertEquals(0, repeater.getCurrentCount(),
            "Count should reset to 0");
        assertFalse(repeater.isComplete(),
            "Should not be complete after reset");
        assertTrue(child.wasReset(),
            "Child should be reset");
    }

    @Test
    @DisplayName("RepeaterNode: repeat 0 times succeeds immediately")
    void testRepeaterZeroTimes() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        RepeaterNode repeater = new RepeaterNode(child, 0);

        NodeStatus result = repeater.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "0 count should succeed immediately");
        assertEquals(0, child.getTickCount(),
            "Child should not be ticked");
    }

    @Test
    @DisplayName("RepeaterNode: repeat 1 time succeeds on first success")
    void testRepeaterOnce() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        RepeaterNode repeater = new RepeaterNode(child, 1);

        NodeStatus result = repeater.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "1 count should succeed on first success");
        assertEquals(1, repeater.getCurrentCount());
    }

    @Test
    @DisplayName("RepeaterNode: INDEFINITELY keeps repeating until failure")
    void testRepeaterIndefinitely() {
        MockNode child = new MockNode(
            NodeStatus.SUCCESS,
            NodeStatus.SUCCESS,
            NodeStatus.SUCCESS,
            NodeStatus.FAILURE
        );
        RepeaterNode repeater = new RepeaterNode(child, RepeaterNode.INDEFINITELY);

        assertTrue(repeater.isIndefinite(),
            "Should be marked as indefinite");

        // First three ticks succeed
        assertEquals(NodeStatus.RUNNING, repeater.tick(blackboard));
        assertEquals(NodeStatus.RUNNING, repeater.tick(blackboard));
        assertEquals(NodeStatus.RUNNING, repeater.tick(blackboard));

        // Fourth tick fails
        assertEquals(NodeStatus.FAILURE, repeater.tick(blackboard));
    }

    @Test
    @DisplayName("RepeaterNode: constructor throws on null child")
    void testRepeaterThrowsOnNullChild() {
        assertThrows(IllegalArgumentException.class,
            () -> new RepeaterNode(null, 3),
            "Should throw on null child");
    }

    @Test
    @DisplayName("RepeaterNode: constructor throws on invalid count")
    void testRepeaterThrowsOnInvalidCount() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        assertThrows(IllegalArgumentException.class,
            () -> new RepeaterNode(child, -2),
            "Should throw on count < -1");
    }

    @Test
    @DisplayName("RepeaterNode: named repeater has correct name")
    void testRepeaterNamed() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        RepeaterNode repeater = new RepeaterNode("TestRepeater", child, 3);

        assertEquals("TestRepeater", repeater.getName(),
            "Named repeater should have correct name");
    }

    @Test
    @DisplayName("RepeaterNode: getChild returns child")
    void testRepeaterGetChild() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        RepeaterNode repeater = new RepeaterNode(child, 3);

        assertEquals(child, repeater.getChild(),
            "Should return the child node");
    }

    @Test
    @DisplayName("RepeaterNode: getTargetCount returns target")
    void testRepeaterGetTargetCount() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        RepeaterNode repeater = new RepeaterNode(child, 5);

        assertEquals(5, repeater.getTargetCount(),
            "Should return target count");
    }

    // ------------------------------------------------------------------------
    // CooldownNode Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("CooldownNode: first execution succeeds")
    void testCooldownFirstExecution() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        CooldownNode cooldown = new CooldownNode(child, 1000);

        NodeStatus result = cooldown.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "First execution should succeed");
        assertEquals(1, child.getTickCount(),
            "Child should be ticked");
        assertTrue(cooldown.getLastExecutionTime() > 0,
            "Last execution time should be set");
    }

    @Test
    @DisplayName("CooldownNode: during cooldown returns FAILURE")
    void testCooldownDuringCooldown() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        CooldownNode cooldown = new CooldownNode(child, 1000);

        // First execution
        cooldown.tick(blackboard);

        // Immediate second tick during cooldown
        NodeStatus result = cooldown.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "During cooldown should return FAILURE");
        assertEquals(1, child.getTickCount(),
            "Child should not be ticked again");
        assertTrue(cooldown.isInCooldown(),
            "Should be in cooldown");
    }

    @Test
    @DisplayName("CooldownNode: after cooldown expires executes again")
    void testCooldownAfterExpiry() throws InterruptedException {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        CooldownNode cooldown = new CooldownNode(child, 100); // 100ms cooldown

        // First execution
        cooldown.tick(blackboard);

        // Wait for cooldown to expire
        Thread.sleep(150);

        // Second execution after cooldown
        NodeStatus result = cooldown.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "After cooldown should execute again");
        assertEquals(2, child.getTickCount(),
            "Child should be ticked twice");
        assertFalse(cooldown.isInCooldown(),
            "Should not be in cooldown");
    }

    @Test
    @DisplayName("CooldownNode: reset clears cooldown")
    void testCooldownReset() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        CooldownNode cooldown = new CooldownNode(child, 1000);

        // First execution
        cooldown.tick(blackboard);
        assertTrue(cooldown.isInCooldown(),
            "Should be in cooldown");

        // Reset
        cooldown.reset();

        assertEquals(0, cooldown.getLastExecutionTime(),
            "Last execution time should be reset");
        assertFalse(cooldown.isInCooldown(),
            "Should not be in cooldown after reset");
        assertTrue(child.wasReset(),
            "Child should be reset");
    }

    @Test
    @DisplayName("CooldownNode: isComplete always returns false")
    void testCooldownIsComplete() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        CooldownNode cooldown = new CooldownNode(child, 1000);

        cooldown.tick(blackboard);

        assertFalse(cooldown.isComplete(),
            "CooldownNode should never be complete");
    }

    @Test
    @DisplayName("CooldownNode: getRemainingCooldown returns correct time")
    void testCooldownGetRemainingCooldown() throws InterruptedException {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        CooldownNode cooldown = new CooldownNode(child, 1000);

        cooldown.tick(blackboard);

        Thread.sleep(100);
        long remaining = cooldown.getRemainingCooldown();

        assertTrue(remaining > 0 && remaining <= 900,
            "Remaining cooldown should be positive and less than total");
    }

    @Test
    @DisplayName("CooldownNode: zero cooldown allows consecutive execution")
    void testCooldownZeroDuration() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        CooldownNode cooldown = new CooldownNode(child, 0);

        // First execution
        NodeStatus result1 = cooldown.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result1);

        // Immediate second execution
        NodeStatus result2 = cooldown.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result2,
            "Zero cooldown should allow consecutive execution");
        assertEquals(2, child.getTickCount());
    }

    @Test
    @DisplayName("CooldownNode: constructor throws on null child")
    void testCooldownThrowsOnNullChild() {
        assertThrows(IllegalArgumentException.class,
            () -> new CooldownNode(null, 1000),
            "Should throw on null child");
    }

    @Test
    @DisplayName("CooldownNode: constructor throws on negative cooldown")
    void testCooldownThrowsOnNegativeCooldown() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        assertThrows(IllegalArgumentException.class,
            () -> new CooldownNode(child, -1),
            "Should throw on negative cooldown");
    }

    @Test
    @DisplayName("CooldownNode: named cooldown has correct name")
    void testCooldownNamed() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        CooldownNode cooldown = new CooldownNode("TestCooldown", child, 1000);

        assertEquals("TestCooldown", cooldown.getName(),
            "Named cooldown should have correct name");
    }

    @Test
    @DisplayName("CooldownNode: getChild returns child")
    void testCooldownGetChild() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        CooldownNode cooldown = new CooldownNode(child, 1000);

        assertEquals(child, cooldown.getChild(),
            "Should return the child node");
    }

    @Test
    @DisplayName("CooldownNode: getCooldownMs returns duration")
    void testCooldownGetCooldownMs() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        CooldownNode cooldown = new CooldownNode(child, 5000);

        assertEquals(5000, cooldown.getCooldownMs(),
            "Should return cooldown duration");
    }

    @Test
    @DisplayName("CooldownNode: setLastExecutionTime for testing")
    void testCooldownSetLastExecutionTime() {
        MockNode child = new MockNode(NodeStatus.SUCCESS);
        CooldownNode cooldown = new CooldownNode(child, 1000);

        long pastTime = System.currentTimeMillis() - 2000;
        cooldown.setLastExecutionTime(pastTime);

        assertFalse(cooldown.isInCooldown(),
            "Should not be in cooldown with old execution time");
    }

    @Test
    @DisplayName("CooldownNode: child failure during cooldown period")
    void testCooldownChildFailure() {
        MockNode child = new MockNode(NodeStatus.FAILURE);
        CooldownNode cooldown = new CooldownNode(child, 1000);

        NodeStatus result = cooldown.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "Child failure should be returned");
        assertTrue(cooldown.isInCooldown(),
            "Cooldown should start even on failure");
    }

    // ------------------------------------------------------------------------
    // Mock implementation for testing
    // ------------------------------------------------------------------------

    /**
     * Mock BTNode implementation for testing.
     * Returns predefined statuses and tracks tick/reset calls.
     */
    private static class MockNode implements BTNode {
        private final NodeStatus[] statuses;
        private int tickIndex = 0;
        private int tickCount = 0;
        private boolean resetCalled = false;
        private String name = "MockNode";

        MockNode(NodeStatus... statuses) {
            this.statuses = statuses;
        }

        @Override
        public NodeStatus tick(BTBlackboard blackboard) {
            tickCount++;
            if (tickIndex < statuses.length) {
                return statuses[tickIndex++];
            }
            return statuses[statuses.length - 1];
        }

        @Override
        public void reset() {
            resetCalled = true;
            tickIndex = 0;
        }

        @Override
        public boolean isComplete() {
            return tickIndex > 0 && tickIndex >= statuses.length &&
                   statuses[statuses.length - 1].isTerminal();
        }

        @Override
        public String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        int getTickCount() {
            return tickCount;
        }

        boolean wasReset() {
            return resetCalled;
        }
    }
}
