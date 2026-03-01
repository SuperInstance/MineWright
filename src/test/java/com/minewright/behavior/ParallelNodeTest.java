package com.minewright.behavior;

import com.minewright.behavior.composite.ParallelNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ParallelNode}.
 *
 * <p>Tests verify the parallel execution behavior:
 * <ul>
 *   <li>All children are ticked each time</li>
 *   <li>Policy-based result determination</li>
 *   <li>State management: reset clears state and resets children</li>
 *   <li>Completion detection based on policy</li>
 * </ul>
 */
@DisplayName("ParallelNode Tests")
class ParallelNodeTest {

    private BTBlackboard blackboard;
    private MockNode successNode;
    private MockNode failureNode;
    private MockNode runningNode;

    @BeforeEach
    void setUp() {
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

    // ------------------------------------------------------------------------
    // SUCCESS_IF_ALL_SUCCEED Policy Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("SUCCESS_IF_ALL_SUCCEED: all success returns SUCCESS")
    void testSuccessIfAllSucceed_AllSuccess() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.SUCCESS_IF_ALL_SUCCEED,
            child1, child2, child3
        );

        NodeStatus result = parallel.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "All children succeeded should return SUCCESS");
        assertEquals(1, child1.getTickCount(),
            "Child1 should be ticked once");
        assertEquals(1, child2.getTickCount(),
            "Child2 should be ticked once");
        assertEquals(1, child3.getTickCount(),
            "Child3 should be ticked once");
        assertTrue(parallel.isComplete(),
            "Parallel should be complete");
    }

    @Test
    @DisplayName("SUCCESS_IF_ALL_SUCCEED: one failure returns FAILURE")
    void testSuccessIfAllSucceed_OneFailure() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.FAILURE);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.SUCCESS_IF_ALL_SUCCEED,
            child1, child2, child3
        );

        NodeStatus result = parallel.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "One child failed should return FAILURE");
        // All children should still be ticked
        assertEquals(1, child1.getTickCount());
        assertEquals(1, child2.getTickCount());
        assertEquals(1, child3.getTickCount());
    }

    @Test
    @DisplayName("SUCCESS_IF_ALL_SUCCEED: running child returns RUNNING")
    void testSuccessIfAllSucceed_RunningChild() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.RUNNING);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.SUCCESS_IF_ALL_SUCCEED,
            child1, child2, child3
        );

        NodeStatus result = parallel.tick(blackboard);

        assertEquals(NodeStatus.RUNNING, result,
            "Running child should return RUNNING");
        assertFalse(parallel.isComplete(),
            "Parallel should not be complete");
    }

    // ------------------------------------------------------------------------
    // SUCCESS_IF_ANY_SUCCEEDS Policy Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("SUCCESS_IF_ANY_SUCCEEDS: one success returns SUCCESS")
    void testSuccessIfAnySucceeds_OneSuccess() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);
        MockNode child3 = new MockNode(NodeStatus.FAILURE);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.SUCCESS_IF_ANY_SUCCEEDS,
            child1, child2, child3
        );

        NodeStatus result = parallel.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "One child succeeded should return SUCCESS");
        // All children should still be ticked
        assertEquals(1, child1.getTickCount());
        assertEquals(1, child2.getTickCount());
        assertEquals(1, child3.getTickCount());
        assertTrue(parallel.isComplete(),
            "Parallel should be complete");
    }

    @Test
    @DisplayName("SUCCESS_IF_ANY_SUCCEEDS: all failure returns FAILURE")
    void testSuccessIfAnySucceeds_AllFailure() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.FAILURE);
        MockNode child3 = new MockNode(NodeStatus.FAILURE);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.SUCCESS_IF_ANY_SUCCEEDS,
            child1, child2, child3
        );

        NodeStatus result = parallel.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "All children failed should return FAILURE");
        assertTrue(parallel.isComplete(),
            "Parallel should be complete");
    }

    @Test
    @DisplayName("SUCCESS_IF_ANY_SUCCEEDS: running and failure returns RUNNING")
    void testSuccessIfAnySucceeds_RunningAndFailure() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.RUNNING);
        MockNode child3 = new MockNode(NodeStatus.FAILURE);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.SUCCESS_IF_ANY_SUCCEEDS,
            child1, child2, child3
        );

        NodeStatus result = parallel.tick(blackboard);

        assertEquals(NodeStatus.RUNNING, result,
            "Running child should return RUNNING");
        assertFalse(parallel.isComplete(),
            "Parallel should not be complete");
    }

    // ------------------------------------------------------------------------
    // FAILURE_IF_ANY_FAILS Policy Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("FAILURE_IF_ANY_FAILS: one failure returns FAILURE")
    void testFailureIfAnyFails_OneFailure() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.FAILURE);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.FAILURE_IF_ANY_FAILS,
            child1, child2, child3
        );

        NodeStatus result = parallel.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "One child failed should return FAILURE");
    }

    @Test
    @DisplayName("FAILURE_IF_ANY_FAILS: all success returns SUCCESS")
    void testFailureIfAnyFails_AllSuccess() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.FAILURE_IF_ANY_FAILS,
            child1, child2, child3
        );

        NodeStatus result = parallel.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "All children succeeded should return SUCCESS");
        assertTrue(parallel.isComplete(),
            "Parallel should be complete");
    }

    @Test
    @DisplayName("FAILURE_IF_ANY_FAILS: running child returns RUNNING")
    void testFailureIfAnyFails_RunningChild() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.RUNNING);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.FAILURE_IF_ANY_FAILS,
            child1, child2, child3
        );

        NodeStatus result = parallel.tick(blackboard);

        assertEquals(NodeStatus.RUNNING, result,
            "Running child should return RUNNING");
    }

    // ------------------------------------------------------------------------
    // FAILURE_IF_ALL_FAIL Policy Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("FAILURE_IF_ALL_FAIL: one success returns SUCCESS")
    void testFailureIfAllFail_OneSuccess() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);
        MockNode child3 = new MockNode(NodeStatus.FAILURE);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.FAILURE_IF_ALL_FAIL,
            child1, child2, child3
        );

        NodeStatus result = parallel.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "One child succeeded should return SUCCESS");
        assertTrue(parallel.isComplete(),
            "Parallel should be complete");
    }

    @Test
    @DisplayName("FAILURE_IF_ALL_FAIL: all failure returns FAILURE")
    void testFailureIfAllFail_AllFailure() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.FAILURE);
        MockNode child3 = new MockNode(NodeStatus.FAILURE);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.FAILURE_IF_ALL_FAIL,
            child1, child2, child3
        );

        NodeStatus result = parallel.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "All children failed should return FAILURE");
        assertTrue(parallel.isComplete(),
            "Parallel should be complete");
    }

    @Test
    @DisplayName("FAILURE_IF_ALL_FAIL: running and failure returns RUNNING")
    void testFailureIfAllFail_RunningAndFailure() {
        MockNode child1 = new MockNode(NodeStatus.FAILURE);
        MockNode child2 = new MockNode(NodeStatus.RUNNING);
        MockNode child3 = new MockNode(NodeStatus.FAILURE);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.FAILURE_IF_ALL_FAIL,
            child1, child2, child3
        );

        NodeStatus result = parallel.tick(blackboard);

        assertEquals(NodeStatus.RUNNING, result,
            "Running child should return RUNNING");
        assertFalse(parallel.isComplete(),
            "Parallel should not be complete");
    }

    // ------------------------------------------------------------------------
    // Multi-tick Execution Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Multi-tick: RUNNING then SUCCESS")
    void testMultiTickRunningThenSuccess() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.RUNNING, NodeStatus.SUCCESS);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.SUCCESS_IF_ALL_SUCCEED,
            child1, child2, child3
        );

        // First tick: child2 runs
        NodeStatus result1 = parallel.tick(blackboard);
        assertEquals(NodeStatus.RUNNING, result1,
            "First tick should return RUNNING");

        // Second tick: all succeed
        NodeStatus result2 = parallel.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result2,
            "Second tick should return SUCCESS");
        assertTrue(parallel.isComplete(),
            "Parallel should be complete");
    }

    @Test
    @DisplayName("Multi-tick: RUNNING then FAILURE")
    void testMultiTickRunningThenFailure() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.RUNNING, NodeStatus.FAILURE);
        MockNode child3 = new MockNode(NodeStatus.SUCCESS);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.SUCCESS_IF_ALL_SUCCEED,
            child1, child2, child3
        );

        // First tick: child2 runs
        NodeStatus result1 = parallel.tick(blackboard);
        assertEquals(NodeStatus.RUNNING, result1);

        // Second tick: child2 fails
        NodeStatus result2 = parallel.tick(blackboard);
        assertEquals(NodeStatus.FAILURE, result2,
            "Second tick should return FAILURE");
        assertTrue(parallel.isComplete(),
            "Parallel should be complete");
    }

    @Test
    @DisplayName("All children ticked each time")
    void testAllChildrenTickedEachTime() {
        MockNode child1 = new MockNode(NodeStatus.RUNNING, NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.RUNNING, NodeStatus.SUCCESS);
        MockNode child3 = new MockNode(NodeStatus.RUNNING, NodeStatus.SUCCESS);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.SUCCESS_IF_ALL_SUCCEED,
            child1, child2, child3
        );

        // Tick multiple times
        parallel.tick(blackboard);
        parallel.tick(blackboard);

        assertEquals(2, child1.getTickCount(),
            "Child1 should be ticked twice");
        assertEquals(2, child2.getTickCount(),
            "Child2 should be ticked twice");
        assertEquals(2, child3.getTickCount(),
            "Child3 should be ticked twice");
    }

    // ------------------------------------------------------------------------
    // State Management Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Reset clears state and resets children")
    void testReset() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.RUNNING);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.SUCCESS_IF_ALL_SUCCEED,
            child1, child2
        );

        // Tick to set state
        parallel.tick(blackboard);
        assertFalse(parallel.isComplete(),
            "Should not be complete with running child");

        // Reset
        parallel.reset();

        assertFalse(parallel.isComplete(),
            "Should not be complete after reset");
        assertTrue(child1.wasReset(),
            "Child1 should be reset");
        assertTrue(child2.wasReset(),
            "Child2 should be reset");
    }

    @Test
    @DisplayName("Reset after completion allows re-execution")
    void testResetAfterCompletion() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.SUCCESS_IF_ALL_SUCCEED,
            child1, child2
        );

        // First execution succeeds
        NodeStatus result1 = parallel.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result1);

        // Reset and change child2 to fail
        parallel.reset();
        child2.setNextStatus(NodeStatus.FAILURE);

        // Re-execute
        NodeStatus result2 = parallel.tick(blackboard);
        assertEquals(NodeStatus.FAILURE, result2,
            "After reset, should return new result");
    }

    // ------------------------------------------------------------------------
    // Edge Cases and Error Handling
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Constructor throws on null policy")
    void testConstructorThrowsOnNullPolicy() {
        assertThrows(IllegalArgumentException.class,
            () -> new ParallelNode(null, successNode),
            "Should throw on null policy");
    }

    @Test
    @DisplayName("Constructor throws on null children")
    void testConstructorThrowsOnNullChildren() {
        assertThrows(IllegalArgumentException.class,
            () -> new ParallelNode(ParallelNode.ParallelPolicy.SUCCESS_IF_ALL_SUCCEED, (BTNode[]) null),
            "Should throw on null children");
    }

    @Test
    @DisplayName("Named parallel has correct name")
    void testNamedParallel() {
        ParallelNode parallel = new ParallelNode(
            "TestParallel",
            ParallelNode.ParallelPolicy.SUCCESS_IF_ALL_SUCCEED,
            successNode
        );

        assertEquals("TestParallel", parallel.getName(),
            "Named parallel should have correct name");
    }

    @Test
    @DisplayName("Get child status after tick")
    void testGetChildStatus() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.RUNNING);

        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.SUCCESS_IF_ALL_SUCCEED,
            child1, child2
        );

        parallel.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, parallel.getChildStatus(child1),
            "Child1 status should be SUCCESS");
        assertEquals(NodeStatus.RUNNING, parallel.getChildStatus(child2),
            "Child2 status should be RUNNING");
    }

    @Test
    @DisplayName("Add child dynamically")
    void testAddChild() {
        // Start with one child
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.SUCCESS_IF_ALL_SUCCEED,
            child1
        );
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);

        parallel.addChild(child2);

        assertEquals(2, parallel.getChildCount(),
            "Should have 2 children after adding");

        NodeStatus result = parallel.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result,
            "All children should execute");
    }

    @Test
    @DisplayName("Remove child")
    void testRemoveChild() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);
        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.SUCCESS_IF_ALL_SUCCEED,
            child1, child2
        );

        boolean removed = parallel.removeChild(child2);

        assertTrue(removed,
            "Remove should return true");
        assertEquals(1, parallel.getChildCount(),
            "Should have 1 child after removal");
    }

    @Test
    @DisplayName("Get children returns unmodifiable list")
    void testGetChildren() {
        MockNode child1 = new MockNode(NodeStatus.SUCCESS);
        MockNode child2 = new MockNode(NodeStatus.SUCCESS);
        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.SUCCESS_IF_ALL_SUCCEED,
            child1, child2
        );

        List<BTNode> children = parallel.getChildren();

        assertEquals(2, children.size(),
            "Should have 2 children");
        assertThrows(UnsupportedOperationException.class,
            () -> children.add(new MockNode(NodeStatus.SUCCESS)),
            "Children list should be unmodifiable");
    }

    @Test
    @DisplayName("Get policy returns correct policy")
    void testGetPolicy() {
        ParallelNode parallel = new ParallelNode(
            ParallelNode.ParallelPolicy.SUCCESS_IF_ANY_SUCCEEDS,
            successNode
        );

        assertEquals(ParallelNode.ParallelPolicy.SUCCESS_IF_ANY_SUCCEEDS,
            parallel.getPolicy(),
            "Should return correct policy");
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
