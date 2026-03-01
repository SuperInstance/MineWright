package com.minewright.behavior.leaf;

import com.minewright.action.ActionResult;
import com.minewright.action.actions.BaseAction;
import com.minewright.behavior.BTBlackboard;
import com.minewright.behavior.BTNode;
import com.minewright.behavior.NodeStatus;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

/**
 * Leaf node that wraps a {@link BaseAction} for behavior tree execution.
 *
 * <p><b>Purpose:</b></p>
 * <p>ActionNode adapts the existing {@link BaseAction} system to behavior trees.
 * It ticks the wrapped action each frame, converting the action's result to
 * behavior tree status. This enables seamless integration of BT control flow
 * with existing game actions.</p>
 *
 * <p><b>Execution Logic:</b></p>
 * <pre>
 * If action not started:
 *   1. Call action.start()
 *   2. Mark as started
 *
 * Each tick:
 *   1. Call action.tick()
 *   2. Check action.isComplete()
 *   3. If complete:
 *      - Get action result
 *      - Return SUCCESS if result success, FAILURE otherwise
 *   4. If not complete:
 *      - Return RUNNING
 * </pre>
 *
 * <p><b>Result Mapping:</b></p>
 * <table border="1">
 *   <tr><th>ActionResult</th><th>NodeStatus</th></tr>
 *   <tr><td>success</td><td>SUCCESS</td></tr>
 *   <tr><td>failure (non-recoverable)</td><td>FAILURE</td></tr>
 *   <tr><td>failure (requires replanning)</td><td>FAILURE</td></tr>
 *   <tr><td>running</td><td>RUNNING</td></tr>
 * </table>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Example 1: Wrap an existing action
 * BaseAction mineAction = new MineBlockAction(foreman, task);
 * BTNode mineNode = new ActionNode(mineAction);
 *
 * // Example 2: Use in sequence
 * BTNode gatherWood = new SequenceNode(
 *     new ConditionNode(() -> hasAxe()),
 *     new ActionNode(new MoveToTreeAction(foreman, moveTask)),
 *     new ActionNode(new MineBlockAction(foreman, mineTask))
 * );
 *
 * // Example 3: Use with decorators
 * BTNode timedAttack = new CooldownNode(
 *     new ActionNode(new AttackAction(foreman, attackTask)),
 *     1000 // 1 second cooldown
 * );
 * }</pre>
 *
 * <p><b>Integration with BaseAction:</b></p>
 * <p>ActionNode bridges the behavior tree and action systems:</p>
 * <ul>
 *   <li>Calls {@link BaseAction#start()} on first tick</li>
 *   <li>Calls {@link BaseAction#tick()} each subsequent tick</li>
 *   <li>Calls {@link BaseAction#cancel()} if node is reset before completion</li>
 *   <li>Checks {@link BaseAction#isComplete()} for termination</li>
 *   <li>Maps {@link ActionResult} to {@link NodeStatus}</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Not thread-safe - assumes single-threaded game tick execution</li>
 *   <li>BaseAction is designed for single-threaded tick execution</li>
 *   <li>Blackboard access is thread-safe but action should not be ticked concurrently</li>
 * </ul>
 *
 * @see BTNode
 * @see ConditionNode
 * @see BaseAction
 * @see ActionResult
 * @see com.minewright.behavior.composite.SequenceNode
 * @see NodeStatus
 * @since 1.0.0
 */
public class ActionNode implements BTNode {
    private static final Logger LOGGER = TestLogger.getLogger(ActionNode.class);

    /**
     * The wrapped action to execute.
     */
    private final BaseAction action;

    /**
     * Flag indicating if the action has been started.
     */
    private boolean started;

    /**
     * Flag indicating if the action has completed.
     */
    private boolean completed;

    /**
     * Cached result after completion.
     */
    private NodeStatus cachedResult;

    /**
     * Optional name for this action node (useful for debugging).
     */
    private final String name;

    /**
     * Creates an action node wrapping the given action.
     *
     * @param action The action to wrap
     * @throws IllegalArgumentException if action is null
     */
    public ActionNode(BaseAction action) {
        this(null, action);
    }

    /**
     * Creates a named action node wrapping the given action.
     *
     * @param name The name for this action node (for debugging)
     * @param action The action to wrap
     * @throws IllegalArgumentException if action is null
     */
    public ActionNode(String name, BaseAction action) {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null");
        }
        this.name = name;
        this.action = action;
        this.started = false;
        this.completed = false;
        this.cachedResult = null;
        LOGGER.debug("Created ActionNode '{}' for action '{}'",
            getName(), action.getDescription());
    }

    @Override
    public NodeStatus tick(BTBlackboard blackboard) {
        // Return cached result if already completed
        if (completed && cachedResult != null) {
            LOGGER.trace("ActionNode '{}' already completed with {}",
                getName(), cachedResult);
            return cachedResult;
        }

        // Start action on first tick
        if (!started) {
            LOGGER.debug("ActionNode '{}' starting action '{}'",
                getName(), action.getDescription());
            action.start();
            started = true;
        }

        // Tick the action
        action.tick();

        // Check completion
        if (action.isComplete()) {
            completed = true;

            ActionResult result = action.getResult();
            if (result != null && result.isSuccess()) {
                cachedResult = NodeStatus.SUCCESS;
                LOGGER.debug("ActionNode '{}' action completed successfully: {}",
                    getName(), result.getMessage());
            } else {
                cachedResult = NodeStatus.FAILURE;
                LOGGER.debug("ActionNode '{}' action failed: {}",
                    getName(),
                    result != null ? result.getMessage() : "no result");
            }

            return cachedResult;
        }

        // Action still running
        LOGGER.trace("ActionNode '{}' action still running: {}",
            getName(), action.getDescription());
        return NodeStatus.RUNNING;
    }

    @Override
    public void reset() {
        if (started && !completed) {
            // Action was in progress - cancel it
            LOGGER.debug("ActionNode '{}' cancelling in-progress action '{}'",
                getName(), action.getDescription());
            action.cancel();
        }

        boolean wasStarted = started;
        boolean wasCompleted = completed;

        started = false;
        completed = false;
        cachedResult = null;

        LOGGER.debug("ActionNode '{}' reset (was: started={}, completed={})",
            getName(), wasStarted, wasCompleted);
    }

    @Override
    public boolean isComplete() {
        return completed;
    }

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }
        // Use action description as default name
        String desc = action.getDescription();
        // Simplify description for readability
        if (desc != null && desc.length() > 30) {
            return desc.substring(0, 30) + "...";
        }
        return desc != null ? desc : getDefaultName();
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Action[");
        sb.append(getName());
        if (started) {
            sb.append(completed ? " ✓" : " …");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Gets the wrapped action.
     *
     * @return The BaseAction being wrapped
     */
    public BaseAction getAction() {
        return action;
    }

    /**
     * Checks if the action has been started.
     *
     * @return true if action.start() has been called
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Gets the action result if complete.
     *
     * @return The ActionResult, or null if not yet complete
     */
    public ActionResult getActionResult() {
        return completed ? action.getResult() : null;
    }

    /**
     * Creates an action node from an action factory.
     *
     * <p>Useful for creating action nodes dynamically within behavior trees.</p>
     *
     * @param <T> The action type
     * @param factory Factory function that creates the action
     * @return A new ActionNode
     */
    public static <T extends BaseAction> ActionNode from(java.util.function.Supplier<T> factory) {
        return new ActionNode(factory.get());
    }

    /**
     * Creates an action node with a custom name from an action factory.
     *
     * @param <T> The action type
     * @param name The name for the action node
     * @param factory Factory function that creates the action
     * @return A new ActionNode
     */
    public static <T extends BaseAction> ActionNode from(String name, java.util.function.Supplier<T> factory) {
        return new ActionNode(name, factory.get());
    }
}
