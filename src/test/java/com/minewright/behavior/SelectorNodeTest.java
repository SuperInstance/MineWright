package com.minewright.behavior;

import com.minewright.behavior.composite.SelectorNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SelectorNode}.
 *
 * <p>Tests verify the "OR" behavior of selector nodes:
 * <ul>
 *   <li>Selector succeeds if any child succeeds (first success wins)</li>
 *   <li>Selector fails only if all children fail</li>
 *   <li>Running state: selector returns RUNNING while child is executing</li>
 *   <li>Fallback behavior: tries children in order until one succeeds</li>
 * </ul>
 */
@DisplayName("SelectorNode Tests")
class SelectorNodeTest {

    private BTBlackboard blackboard;
    private MockNode successNode;
    private MockNode failureNode;
    private MockNode runningNode;

    @BeforeEach
    void setUp() {
        // Create a mock blackboard that doesn't require a real entity
        blackboard = createMockBlackboard();

        successNode = new MockNode(NodeStatus.SUCCESS);
        failureNode = new MockNode(NodeStatus.FAILURE);
        runningNode = new MockNode(NodeStatus.RUNNING);
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

    @Test
    @DisplayName("Single success child returns SUCCESS")
    void testSingleSuccessChild() {
        SelectorNode selector = new SelectorNode(successNode);

        NodeStatus result = selector.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "Selector with single success child should return SUCCESS");
        assertEquals(1, successNode.getTickCount(),
            "Child should be ticked once");
        assertTrue(selector.isComplete(),
            "Selector should be complete after child succeeds");
    }

    @Test
    @DisplayName("Single failure child returns FAILURE")
    void testSingleFailureChild() {
        SelectorNode selector = new SelectorNode(failureNode);

        NodeStatus result = selector.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "Selector with single failure child should return FAILURE");
        assertEquals(1, failureNode.getTickCount(),
            "Child should be ticked once");
        assertTrue(selector.isComplete(),
            "Selector should be complete after all children fail");
    }

    @Test
    @DisplayName("First success stops execution")
    void testFirstSuccessStopsExecution() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        SelectorNode selector = new SelectorNode(child1, child2, child3);

        NodeStatus result = selector.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "Selector should succeed when first child succeeds");
        assertEquals(1, child1.getTickCount(),
            "First child should be ticked");
        assertEquals(0, child2.getTickCount(),
            "Second child should NOT be ticked");
        assertEquals(0, child3.getTickCount(),
            "Third child should NOT be ticked");
    }

    @Test
    @DisplayName("Skips failures and tries next child")
    void testSkipsFailures() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        SelectorNode selector = new SelectorNode(child1, child2, child3);

        NodeStatus result = selector.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "Selector should succeed when second child succeeds");
        assertEquals(1, child1.getTickCount(),
            "First child should be ticked");
        assertEquals(1, child2.getTickCount(),
            "Second child should be ticked");
        assertEquals(0, child3.getTickCount(),
            "Third child should NOT be ticked");
    }

    @Test
    @DisplayName("All failures returns FAILURE")
    void testAllFailures() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.FAILURE);
        MockNode child3 = new MockNode(NodeStatus.FAILURE);

        SelectorNode selector = new SelectorNode(child1, child2, child3);

        NodeStatus result = selector.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "Selector should fail when all children fail");
        assertEquals(1, child1.getTickCount(),
            "First child should be ticked");
        assertEquals(1, child2.getTickCount(),
            "Second child should be ticked");
        assertEquals(1, child3.getTickCount(),
            "Third child should be ticked");
        assertTrue(selector.isComplete(),
            "Selector should be complete");
    }

    @Test
    @DisplayName("Running child returns RUNNING")
    void testRunningChild() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.RUNNING);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        SelectorNode selector = new SelectorNode(child1, child2, child3);

        NodeStatus result = selector.tick(blackboard);

        assertEquals(NodeStatus.RUNNING, result,
            "Selector should return RUNNING when child is running");
        assertEquals(1, child1.getTickCount(),
            "First child should be ticked");
        assertEquals(1, child2.getTickCount(),
            "Second child should be ticked");
        assertEquals(0, child3.getTickCount(),
            "Third child should NOT be ticked yet");
        assertEquals(1, selector.getCurrentChildIndex(),
            "Current child index should be 1");
    }

    @Test
    @DisplayName("Multi-tick: RUNNING then SUCCESS")
    void testMultiTickRunningThenSuccess() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.RUNNING, NodeStatus.SUCCESS);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        SelectorNode selector = new SelectorNode(child1, child2, child3);

        // First tick: child2 runs
        NodeStatus result1 = selector.tick(blackboard);
        assertEquals(NodeStatus.RUNNING, result1,
            "First tick should return RUNNING");
        assertEquals(1, child1.getTickCount());
        assertEquals(1, child2.getTickCount());

        // Second tick: child2 succeeds
        NodeStatus result2 = selector.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result2,
            "Second tick should return SUCCESS");
        assertEquals(1, child1.getTickCount(),
            "Child1 should not be re-ticked");
        assertEquals(2, child2.getTickCount(),
            "Child2 should be ticked twice");
        assertEquals(0, child3.getTickCount(),
            "Child3 should not be ticked");
        assertTrue(selector.isComplete(),
            "Selector should be complete");
    }

    @Test
    @DisplayName("Multi-tick: all failures then SUCCESS on retry")
    void testMultiTickFailureThenSuccess() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.FAILURE);
        MockNode child3 = new MockNode(NodeStatus.FAILURE);

        SelectorNode selector = new SelectorNode(child1, child2, child3);

        // First tick: all fail
        NodeStatus result1 = selector.tick(blackboard);
        assertEquals(NodeStatus.FAILURE, result1,
            "First tick should return FAILURE (all failed)");
        assertEquals(1, child1.getTickCount());
        assertEquals(1, child2.getTickCount());
        assertEquals(1, child3.getTickCount());

        // Reset and change child3 to succeed
        selector.reset();
        child3.setNextStatus(NodeStatus.SUCCESS);

        // Second tick: now succeeds on child3
        NodeStatus result2 = selector.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result2);
        assertEquals(2, child3.getTickCount(),
            "Child3 should be ticked again and succeed (total 2 times)");
    }

    @Test
    @DisplayName("Multi-tick: continue trying after RUNNING fails")
    void testMultiTickRunningFailsThenNextSucceeds() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.RUNNING, NodeStatus.FAILURE);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        SelectorNode selector = new SelectorNode(child1, child2, child3);

        // First tick: child2 runs
        NodeStatus result1 = selector.tick(blackboard);
        assertEquals(NodeStatus.RUNNING, result1);

        // Second tick: child2 fails, selector continues to child3
        NodeStatus result2 = selector.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result2,
            "After child2 fails, should try child3 and succeed");
        assertEquals(1, child3.getTickCount(),
            "Child3 should be ticked");
    }

    @Test
    @DisplayName("Reset clears state and resets children")
    void testReset() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.RUNNING);

        SelectorNode selector = new SelectorNode(child1, child2);

        // Tick to set state
        selector.tick(blackboard);
        assertEquals(1, selector.getCurrentChildIndex(),
            "Current child should be 1");

        // Reset
        selector.reset();

        assertEquals(0, selector.getCurrentChildIndex(),
            "Current child should reset to 0");
        assertFalse(selector.isComplete(),
            "Selector should not be complete after reset");
        assertTrue(child1.wasReset(),
            "Child1 should be reset");
        assertTrue(child2.wasReset(),
            "Child2 should be reset");
    }

    @Test
    @DisplayName("Reset after failure allows re-execution")
    void testResetAfterFailure() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.FAILURE);
        MockNode child3 = new MockNode(NodeStatus.FAILURE);

        SelectorNode selector = new SelectorNode(child1, child2, child3);

        // First execution fails
        NodeStatus result1 = selector.tick(blackboard);
        assertEquals(NodeStatus.FAILURE, result1);

        // Reset and change child2 to succeed
        selector.reset();
        child2.setNextStatus(NodeStatus.SUCCESS);

        // Re-execute
        NodeStatus result2 = selector.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result2,
            "After reset, selector should succeed on child2");
    }

    @Test
    @DisplayName("Reset after success allows re-execution")
    void testResetAfterSuccess() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);

        SelectorNode selector = new SelectorNode(child1, child2);

        // First execution succeeds on child1
        NodeStatus result1 = selector.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result1);

        // Reset and change child1 to fail
        selector.reset();
        child1.setNextStatus(NodeStatus.FAILURE);

        // Re-execute: should now succeed on child2
        NodeStatus result2 = selector.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result2,
            "After reset, should try child1, fail, then succeed on child2");
        assertEquals(1, child2.getTickCount(),
            "Child2 should be ticked");
    }

    @Test
    @DisplayName("Empty selector returns FAILURE")
    void testEmptySelector() {
        SelectorNode selector = new SelectorNode();

        NodeStatus result = selector.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "Empty selector should return FAILURE");
    }

    @Test
    @DisplayName("Named selector has correct name")
    void testNamedSelector() {
        SelectorNode selector = new SelectorNode("TestSelector", successNode);

        assertEquals("TestSelector", selector.getName(),
            "Named selector should have correct name");
    }

    @Test
    @DisplayName("Add child dynamically")
    void testAddChild() {
        SelectorNode selector = new SelectorNode();
        MockNode child = new MockNode(NodeStatus.SUCCESS);

        selector.addChild(child);

        assertEquals(1, selector.getChildCount(),
            "Should have 1 child after adding");

        NodeStatus result = selector.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result,
            "Dynamically added child should execute");
    }

    @Test
    @DisplayName("Insert child at front for priority")
    void testInsertChild() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);

        SelectorNode selector = new SelectorNode(child1);
        selector.insertChild(0, child2);

        NodeStatus result = selector.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "Inserted child should be tried first");
        assertEquals(0, child1.getTickCount(),
            "Original child should not be ticked");
        assertEquals(1, child2.getTickCount(),
            "Inserted child should be ticked");
    }

    @Test
    @DisplayName("Remove child")
    void testRemoveChild() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);
        SelectorNode selector = new SelectorNode(child1, child2);

        boolean removed = selector.removeChild(child2);

        assertTrue(removed,
            "Remove should return true");
        assertEquals(1, selector.getChildCount(),
            "Should have 1 child after removal");
    }

    @Test
    @DisplayName("Get children returns unmodifiable list")
    void testGetChildren() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);
        SelectorNode selector = new SelectorNode(child1, child2);

        var children = selector.getChildren();

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
