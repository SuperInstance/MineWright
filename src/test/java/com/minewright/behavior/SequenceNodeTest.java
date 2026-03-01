package com.minewright.behavior;

import com.minewright.behavior.composite.SequenceNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SequenceNode}.
 *
 * <p>Tests verify the "AND" behavior of sequence nodes:
 * <ul>
 *   <li>All children must succeed for sequence to succeed</li>
 *   <li>Fail-fast behavior: sequence fails immediately if any child fails</li>
 *   <li>Running state: sequence returns RUNNING while child is executing</li>
 *   <li>State management: reset clears state and resets all children</li>
 * </ul>
 */
@DisplayName("SequenceNode Tests")
class SequenceNodeTest {

    private BTBlackboard blackboard;
    private MockNode successNode;
    private MockNode failureNode;
    private MockNode runningNode;
    private MockNode multiTickNode;

    @BeforeEach
    void setUp() {
        // For unit testing, we create a test blackboard that doesn't need a real entity.
        // Our mock nodes don't actually use the blackboard for their core logic.
        blackboard = createTestBlackboard();

        // Create mock nodes with different behaviors
        successNode = new MockNode(NodeStatus.SUCCESS);
        failureNode = new MockNode(NodeStatus.FAILURE);
        runningNode = new MockNode(NodeStatus.RUNNING);
        multiTickNode = new MockNode(NodeStatus.RUNNING, NodeStatus.SUCCESS);
    }

    /**
     * Creates a test blackboard without needing a real ForemanEntity.
     * Uses sun.misc.Unsafe to allocate instance bypassing constructor.
     */
    private BTBlackboard createTestBlackboard() {
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

    @Test
    @DisplayName("Single success child returns SUCCESS")
    void testSingleSuccessChild() {
        SequenceNode sequence = new SequenceNode(successNode);

        NodeStatus result = sequence.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "Sequence with single success child should return SUCCESS");
        assertEquals(1, successNode.getTickCount(),
            "Child should be ticked once");
        assertTrue(sequence.isComplete(),
            "Sequence should be complete after all children succeed");
    }

    @Test
    @DisplayName("Single failure child returns FAILURE")
    void testSingleFailureChild() {
        SequenceNode sequence = new SequenceNode(failureNode);

        NodeStatus result = sequence.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "Sequence with single failure child should return FAILURE");
        assertEquals(1, failureNode.getTickCount(),
            "Child should be ticked once");
    }

    @Test
    @DisplayName("All success children returns SUCCESS")
    void testAllSuccessChildren() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        SequenceNode sequence = new SequenceNode(child1, child2, child3);

        NodeStatus result = sequence.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "Sequence with all success children should return SUCCESS");
        assertEquals(1, child1.getTickCount(),
            "First child should be ticked once");
        assertEquals(1, child2.getTickCount(),
            "Second child should be ticked once");
        assertEquals(1, child3.getTickCount(),
            "Third child should be ticked once");
        assertTrue(sequence.isComplete(),
            "Sequence should be complete");
    }

    @Test
    @DisplayName("Fail-fast: second child failure returns FAILURE")
    void testFailFastOnSecondChild() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.FAILURE);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        SequenceNode sequence = new SequenceNode(child1, child2, child3);

        NodeStatus result = sequence.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "Sequence should fail when second child fails");
        assertEquals(1, child1.getTickCount(),
            "First child should be ticked");
        assertEquals(1, child2.getTickCount(),
            "Second child should be ticked");
        assertEquals(0, child3.getTickCount(),
            "Third child should NOT be ticked (fail-fast)");
    }

    @Test
    @DisplayName("Fail-fast: first child failure returns FAILURE")
    void testFailFastOnFirstChild() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);

        SequenceNode sequence = new SequenceNode(child1, child2);

        NodeStatus result = sequence.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "Sequence should fail when first child fails");
        assertEquals(1, child1.getTickCount(),
            "First child should be ticked");
        assertEquals(0, child2.getTickCount(),
            "Second child should NOT be ticked");
    }

    @Test
    @DisplayName("Running child returns RUNNING")
    void testRunningChild() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.RUNNING);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        SequenceNode sequence = new SequenceNode(child1, child2, child3);

        NodeStatus result = sequence.tick(blackboard);

        assertEquals(NodeStatus.RUNNING, result,
            "Sequence should return RUNNING when child is running");
        assertEquals(1, child1.getTickCount(),
            "First child should be ticked");
        assertEquals(1, child2.getTickCount(),
            "Second child should be ticked");
        assertEquals(0, child3.getTickCount(),
            "Third child should NOT be ticked yet");
        assertEquals(1, sequence.getCurrentChildIndex(),
            "Current child index should be 1");
    }

    @Test
    @DisplayName("Multi-tick execution resumes after RUNNING")
    void testMultiTickExecution() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.RUNNING, NodeStatus.SUCCESS);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        SequenceNode sequence = new SequenceNode(child1, child2, child3);

        // First tick: child2 runs
        NodeStatus result1 = sequence.tick(blackboard);
        assertEquals(NodeStatus.RUNNING, result1,
            "First tick should return RUNNING");
        assertEquals(1, child1.getTickCount(),
            "Child1 should be ticked once");
        assertEquals(1, child2.getTickCount(),
            "Child2 should be ticked once");

        // Second tick: child2 succeeds, moves to child3
        NodeStatus result2 = sequence.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result2,
            "Second tick should return SUCCESS");
        assertEquals(1, child1.getTickCount(),
            "Child1 should still be ticked once");
        assertEquals(2, child2.getTickCount(),
            "Child2 should be ticked twice");
        assertEquals(1, child3.getTickCount(),
            "Child3 should be ticked once");
        assertTrue(sequence.isComplete(),
            "Sequence should be complete");
    }

    @Test
    @DisplayName("Reset clears state and resets children")
    void testReset() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.RUNNING);

        SequenceNode sequence = new SequenceNode(child1, child2);

        // Tick to set state
        sequence.tick(blackboard);
        assertEquals(1, sequence.getCurrentChildIndex(),
            "Current child should be 1");

        // Reset
        sequence.reset();

        assertEquals(0, sequence.getCurrentChildIndex(),
            "Current child should reset to 0");
        assertFalse(sequence.isComplete(),
            "Sequence should not be complete after reset");
        assertTrue(child1.wasReset(),
            "Child1 should be reset");
        assertTrue(child2.wasReset(),
            "Child2 should be reset");
    }

    @Test
    @DisplayName("Reset after failure allows re-execution")
    void testResetAfterFailure() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.FAILURE);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        SequenceNode sequence = new SequenceNode(child1, child2, child3);

        // First execution fails
        NodeStatus result1 = sequence.tick(blackboard);
        assertEquals(NodeStatus.FAILURE, result1);

        // Reset and change child2 to succeed
        sequence.reset();
        child2.setNextStatus(NodeStatus.SUCCESS);

        // Re-execute
        NodeStatus result2 = sequence.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result2,
            "After reset, sequence should succeed");
        assertEquals(1, child3.getTickCount(),
            "All children should execute");
    }

    @Test
    @DisplayName("Empty sequence returns FAILURE")
    void testEmptySequence() {
        SequenceNode sequence = new SequenceNode();

        NodeStatus result = sequence.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "Empty sequence should return FAILURE");
    }

    @Test
    @DisplayName("Named sequence has correct name")
    void testNamedSequence() {
        SequenceNode sequence = new SequenceNode("TestSequence", successNode);

        assertEquals("TestSequence", sequence.getName(),
            "Named sequence should have correct name");
    }

    @Test
    @DisplayName("Add child dynamically")
    void testAddChild() {
        SequenceNode sequence = new SequenceNode();
        MockNode child = new MockNode(NodeStatus.SUCCESS);

        sequence.addChild(child);

        assertEquals(1, sequence.getChildCount(),
            "Should have 1 child after adding");

        NodeStatus result = sequence.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result,
            "Dynamically added child should execute");
    }

    @Test
    @DisplayName("Remove child")
    void testRemoveChild() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);
        SequenceNode sequence = new SequenceNode(child1, child2);

        boolean removed = sequence.removeChild(child2);

        assertTrue(removed,
            "Remove should return true");
        assertEquals(1, sequence.getChildCount(),
            "Should have 1 child after removal");
    }

    @Test
    @DisplayName("Get children returns unmodifiable list")
    void testGetChildren() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);
        SequenceNode sequence = new SequenceNode(child1, child2);

        var children = sequence.getChildren();

        assertEquals(2, children.size(),
            "Should have 2 children");
        assertThrows(UnsupportedOperationException.class,
            () -> children.add(new MockNode(NodeStatus.SUCCESS)),
            "Children list should be unmodifiable");
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
            // If we run out of statuses, return the last one
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

        void setNextStatus(NodeStatus status) {
            tickIndex = 0;
            if (statuses.length > 0) {
                statuses[0] = status;
            }
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
