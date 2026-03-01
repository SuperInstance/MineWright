package com.minewright.behavior;

import com.minewright.behavior.leaf.ActionNode;
import com.minewright.behavior.leaf.ConditionNode;
import com.minewright.action.ActionResult;
import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.action.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.lang.reflect.Field;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for behavior tree leaf nodes.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>{@link ConditionNode} - evaluates conditions</li>
 *   <li>{@link ActionNode} - wraps BaseAction for BT execution</li>
 * </ul>
 */
@DisplayName("Leaf Node Tests")
class LeafNodeTest {

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
    // ConditionNode Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("ConditionNode: true condition returns SUCCESS")
    void testConditionTrueReturnsSuccess() {
        ConditionNode condition = new ConditionNode(() -> true);

        NodeStatus result = condition.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "True condition should return SUCCESS");
    }

    @Test
    @DisplayName("ConditionNode: false condition returns FAILURE")
    void testConditionFalseReturnsFailure() {
        ConditionNode condition = new ConditionNode(() -> false);

        NodeStatus result = condition.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "False condition should return FAILURE");
    }

    @Test
    @DisplayName("ConditionNode: with blackboard predicate")
    void testConditionWithBlackboard() {
        blackboard.put("test_value", 42);
        ConditionNode condition = new ConditionNode(
            bb -> bb.getInt("test_value", 0) > 10
        );

        NodeStatus result = condition.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "Condition with blackboard access should evaluate correctly");
    }

    @Test
    @DisplayName("ConditionNode: reset is no-op")
    void testConditionReset() {
        ConditionNode condition = new ConditionNode(() -> true);

        assertDoesNotThrow(() -> condition.reset(),
            "Condition reset should be a no-op");
    }

    @Test
    @DisplayName("ConditionNode: isComplete always returns true")
    void testConditionIsComplete() {
        ConditionNode condition = new ConditionNode(() -> true);

        assertTrue(condition.isComplete(),
            "Conditions are always complete (never return RUNNING)");
    }

    @Test
    @DisplayName("ConditionNode: constructor throws on null supplier")
    void testConditionThrowsOnNullSupplier() {
        assertThrows(NullPointerException.class,
            () -> new ConditionNode((BooleanSupplier) null),
            "Should throw on null supplier");
    }

    @Test
    @DisplayName("ConditionNode: constructor throws on null predicate")
    void testConditionThrowsOnNullPredicate() {
        assertThrows(NullPointerException.class,
            () -> new ConditionNode((Predicate<BTBlackboard>) null),
            "Should throw on null predicate");
    }

    @Test
    @DisplayName("ConditionNode: named condition has correct name")
    void testConditionNamed() {
        ConditionNode condition = new ConditionNode("TestCondition", () -> true);

        assertEquals("TestCondition", condition.getName(),
            "Named condition should have correct name");
    }

    @Test
    @DisplayName("ConditionNode: test method evaluates without affecting state")
    void testConditionTestMethod() {
        ConditionNode condition = new ConditionNode("test", () -> true);

        boolean result = condition.test(blackboard);

        assertTrue(result,
            "test() should evaluate condition");
        // Can tick again without state change
        assertEquals(NodeStatus.SUCCESS, condition.tick(blackboard));
    }

    // ------------------------------------------------------------------------
    // ConditionNode Static Factory Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("ConditionNode.hasKey: returns SUCCESS when key exists")
    void testConditionHasKey() {
        blackboard.put("target", "value");

        ConditionNode condition = ConditionNode.hasKey("target");

        assertEquals(NodeStatus.SUCCESS, condition.tick(blackboard));
    }

    @Test
    @DisplayName("ConditionNode.hasKey: returns FAILURE when key missing")
    void testConditionHasKeyMissing() {
        ConditionNode condition = ConditionNode.hasKey("missing");

        assertEquals(NodeStatus.FAILURE, condition.tick(blackboard));
    }

    @Test
    @DisplayName("ConditionNode.isTrue: returns SUCCESS when boolean is true")
    void testConditionIsTrue() {
        blackboard.put("enabled", true);

        ConditionNode condition = ConditionNode.isTrue("enabled");

        assertEquals(NodeStatus.SUCCESS, condition.tick(blackboard));
    }

    @Test
    @DisplayName("ConditionNode.isFalse: returns SUCCESS when boolean is false")
    void testConditionIsFalse() {
        blackboard.put("disabled", false);

        ConditionNode condition = ConditionNode.isFalse("disabled");

        assertEquals(NodeStatus.SUCCESS, condition.tick(blackboard));
    }

    @Test
    @DisplayName("ConditionNode.greaterThan: checks numeric threshold")
    void testConditionGreaterThan() {
        blackboard.put("count", 10);

        ConditionNode condition = ConditionNode.greaterThan("count", 5);

        assertEquals(NodeStatus.SUCCESS, condition.tick(blackboard));
    }

    @Test
    @DisplayName("ConditionNode.lessThan: checks numeric threshold")
    void testConditionLessThan() {
        blackboard.put("temperature", 50);

        ConditionNode condition = ConditionNode.lessThan("temperature", 100);

        assertEquals(NodeStatus.SUCCESS, condition.tick(blackboard));
    }

    @Test
    @DisplayName("ConditionNode.equals: checks numeric equality")
    void testConditionEquals() {
        blackboard.put("value", 42);

        ConditionNode condition = ConditionNode.equals("value", 42);

        assertEquals(NodeStatus.SUCCESS, condition.tick(blackboard));
    }

    @Test
    @DisplayName("ConditionNode.not: negates condition")
    void testConditionNot() {
        blackboard.put("flag", true);

        ConditionNode original = ConditionNode.isTrue("flag");
        ConditionNode negated = ConditionNode.not(original);

        assertEquals(NodeStatus.FAILURE, negated.tick(blackboard),
            "Negated true condition should fail");
    }

    @Test
    @DisplayName("ConditionNode.and: all must succeed")
    void testConditionAnd() {
        blackboard.put("a", true);
        blackboard.put("b", true);

        ConditionNode condition = ConditionNode.and(
            ConditionNode.isTrue("a"),
            ConditionNode.isTrue("b")
        );

        assertEquals(NodeStatus.SUCCESS, condition.tick(blackboard));
    }

    @Test
    @DisplayName("ConditionNode.and: fails if any fails")
    void testConditionAndFails() {
        blackboard.put("a", true);
        blackboard.put("b", false);

        ConditionNode condition = ConditionNode.and(
            ConditionNode.isTrue("a"),
            ConditionNode.isTrue("b")
        );

        assertEquals(NodeStatus.FAILURE, condition.tick(blackboard));
    }

    @Test
    @DisplayName("ConditionNode.or: any can succeed")
    void testConditionOr() {
        blackboard.put("a", true);
        blackboard.put("b", false);

        ConditionNode condition = ConditionNode.or(
            ConditionNode.isTrue("a"),
            ConditionNode.isTrue("b")
        );

        assertEquals(NodeStatus.SUCCESS, condition.tick(blackboard));
    }

    @Test
    @DisplayName("ConditionNode.or: fails if all fail")
    void testConditionOrFails() {
        blackboard.put("a", false);
        blackboard.put("b", false);

        ConditionNode condition = ConditionNode.or(
            ConditionNode.isTrue("a"),
            ConditionNode.isTrue("b")
        );

        assertEquals(NodeStatus.FAILURE, condition.tick(blackboard));
    }

    // ------------------------------------------------------------------------
    // ActionNode Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("ActionNode: successful action returns SUCCESS")
    void testActionNodeSuccess() {
        MockAction action = new MockAction(true);
        action.setComplete(true);
        ActionNode actionNode = new ActionNode(action);

        NodeStatus result = actionNode.tick(blackboard);

        assertEquals(NodeStatus.SUCCESS, result,
            "Successful action should return SUCCESS");
        assertTrue(action.isStarted(),
            "Action should be started");
        assertTrue(actionNode.isComplete(),
            "Action node should be complete");
    }

    @Test
    @DisplayName("ActionNode: failing action returns FAILURE")
    void testActionNodeFailure() {
        MockAction action = new MockAction(false);
        action.setComplete(true);
        ActionNode actionNode = new ActionNode(action);

        NodeStatus result = actionNode.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "Failed action should return FAILURE");
    }

    @Test
    @DisplayName("ActionNode: running action returns RUNNING")
    void testActionNodeRunning() {
        MockAction action = new MockAction(true);
        ActionNode actionNode = new ActionNode(action);

        // First tick: running
        action.setComplete(false);
        NodeStatus result1 = actionNode.tick(blackboard);
        assertEquals(NodeStatus.RUNNING, result1,
            "Running action should return RUNNING");
        assertTrue(action.isStarted(),
            "Action should be started");

        // Second tick: complete
        action.setComplete(true);
        NodeStatus result2 = actionNode.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result2,
            "Completed action should return SUCCESS");
    }

    @Test
    @DisplayName("ActionNode: reset cancels in-progress action")
    void testActionNodeReset() {
        MockAction action = new MockAction(true);
        action.setComplete(false);
        ActionNode actionNode = new ActionNode(action);

        actionNode.tick(blackboard);
        assertTrue(action.isStarted(),
            "Action should be started");

        actionNode.reset();

        assertFalse(actionNode.isStarted(),
            "Action node should reset started flag");
        assertTrue(action.wasCancelled(),
            "In-progress action should be cancelled");
    }

    @Test
    @DisplayName("ActionNode: completed action returns cached result")
    void testActionNodeCachedResult() {
        MockAction action = new MockAction(true);
        action.setComplete(true);
        ActionNode actionNode = new ActionNode(action);

        // First tick
        actionNode.tick(blackboard);
        assertEquals(1, action.getTickCount());

        // Second tick (should use cached result)
        NodeStatus result = actionNode.tick(blackboard);
        assertEquals(NodeStatus.SUCCESS, result);
        assertEquals(1, action.getTickCount(),
            "Action should not be ticked again after completion");
    }

    @Test
    @DisplayName("ActionNode: constructor throws on null action")
    void testActionNodeThrowsOnNullAction() {
        assertThrows(IllegalArgumentException.class,
            () -> new ActionNode(null),
            "Should throw on null action");
    }

    @Test
    @DisplayName("ActionNode: named action has correct name")
    void testActionNodeNamed() {
        MockAction action = new MockAction(true);
        action.setComplete(true);
        ActionNode actionNode = new ActionNode("TestAction", action);

        assertEquals("TestAction", actionNode.getName(),
            "Named action node should have correct name");
    }

    @Test
    @DisplayName("ActionNode: getAction returns wrapped action")
    void testActionNodeGetAction() {
        MockAction action = new MockAction(true);
        action.setComplete(true);
        ActionNode actionNode = new ActionNode(action);

        assertEquals(action, actionNode.getAction(),
            "Should return the wrapped action");
    }

    @Test
    @DisplayName("ActionNode: isStarted returns correct state")
    void testActionNodeIsStarted() {
        MockAction action = new MockAction(true);
        action.setComplete(true);
        ActionNode actionNode = new ActionNode(action);

        assertFalse(actionNode.isStarted(),
            "Should not be started initially");

        actionNode.tick(blackboard);

        assertTrue(actionNode.isStarted(),
            "Should be started after first tick");
    }

    @Test
    @DisplayName("ActionNode: getActionResult returns result when complete")
    void testActionNodeGetActionResult() {
        MockAction action = new MockAction(true);
        action.setComplete(true);
        ActionNode actionNode = new ActionNode(action);

        assertNull(actionNode.getActionResult(),
            "Should be null before completion");

        actionNode.tick(blackboard);

        assertNotNull(actionNode.getActionResult(),
            "Should return action result after completion");
        assertTrue(actionNode.getActionResult().isSuccess(),
            "Result should be successful");
    }

    @Test
    @DisplayName("ActionNode: from factory method")
    void testActionNodeFromFactory() {
        MockAction action = new MockAction(true);
        action.setComplete(true);
        ActionNode actionNode = ActionNode.from(() -> action);

        assertNotNull(actionNode,
            "Factory should create action node");
        assertEquals(action, actionNode.getAction(),
            "Should wrap created action");
    }

    @Test
    @DisplayName("ActionNode: from factory method with name")
    void testActionNodeFromFactoryWithName() {
        MockAction action = new MockAction(true);
        action.setComplete(true);
        ActionNode actionNode = ActionNode.from("FactoryAction", () -> action);

        assertEquals("FactoryAction", actionNode.getName(),
            "Factory should set name");
    }

    @Test
    @DisplayName("ActionNode: action with null result returns FAILURE")
    void testActionNodeNullResult() {
        MockAction action = new MockAction(true);
        action.setResult(null); // Set null result
        action.setComplete(true);

        ActionNode actionNode = new ActionNode(action);

        NodeStatus result = actionNode.tick(blackboard);

        assertEquals(NodeStatus.FAILURE, result,
            "Null result should return FAILURE");
    }

    // ------------------------------------------------------------------------
    // Mock Action Implementation
    // ------------------------------------------------------------------------

    /**
     * Mock BaseAction for testing ActionNode.
     */
    private static class MockAction extends BaseAction {
        private final boolean success;
        private final boolean hasResult;
        private boolean complete = false;
        private boolean startedLocal = false;
        private boolean cancelledLocal = false;
        private int tickCount = 0;
        private ActionResult result;

        MockAction(boolean success) {
            this(success, true);
        }

        MockAction(boolean success, boolean hasResult) {
            super(null, null); // Mock without real entity/task
            this.success = success;
            this.hasResult = hasResult;
            this.result = ActionResult.success("Mock action completed");
        }

        @Override
        protected void onStart() {
            startedLocal = true;
            if (!success && !hasResult) {
                // Will fail on completion
            } else if (success) {
                this.result = ActionResult.success("Mock action completed");
            }
        }

        @Override
        protected void onTick() {
            tickCount++;
            if (complete) {
                if (success) {
                    this.result = ActionResult.success("Mock action completed");
                } else {
                    this.result = ActionResult.failure("Mock action failed", false);
                }
            }
        }

        @Override
        protected void onCancel() {
            cancelledLocal = true;
            this.result = ActionResult.failure("Mock action cancelled", false);
        }

        @Override
        public boolean isComplete() {
            return complete || result != null;
        }

        @Override
        public ActionResult getResult() {
            return result;
        }

        @Override
        public String getDescription() {
            return "MockAction";
        }

        // Test control methods
        void setComplete(boolean complete) {
            this.complete = complete;
        }

        void setResult(ActionResult result) {
            this.result = result;
        }

        boolean isStarted() {
            return startedLocal;
        }

        boolean wasCancelled() {
            return cancelledLocal;
        }

        int getTickCount() {
            return tickCount;
        }
    }
}
