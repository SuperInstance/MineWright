package com.minewright.behavior.decorator;

import com.minewright.behavior.BTBlackboard;
import com.minewright.behavior.BTNode;
import com.minewright.behavior.NodeStatus;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

/**
 * Inverts the result of its child node.
 *
 * <p><b>Purpose:</b></p>
 * <p>InverterNode negates the child's status, converting SUCCESS to FAILURE
 * and FAILURE to SUCCESS. RUNNING status remains unchanged (as the child is
 * still executing and cannot be inverted).</p>
 *
 * <p><b>Execution Logic:</b></p>
 * <pre>
 * Tick child:
 *   - If child returns SUCCESS, return FAILURE
 *   - If child returns FAILURE, return SUCCESS
 *   - If child returns RUNNING, return RUNNING
 * </pre>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Example 1: "While NOT at destination"
 * BTNode moveToTarget = new SequenceNode(
 *     new InverterNode(
 *         new ConditionNode(() -> isAtDestination())
 *     ),
 *     new ActionNode(new MoveStepAction())
 * );
 *
 * // Example 2: "If NOT in combat"
 * BTNode nonCombatBehavior = new InverterNode(
 *     new ConditionNode(() -> isInCombat())
 * );
 *
 * // Example 3: "Keep trying until fails" (succeeds while child succeeds)
 * // Using inverter to check for failure condition
 * BTNode waitForFail = new SequenceNode(
 *     new InverterNode(
 *         new ConditionNode(() -> hasFailed())
 *     ),
 *     new ActionNode(new ContinueAction())
 * );
 * }</pre>
 *
 * <p><b>Behavior Diagram:</b></p>
 * <pre>
 *        InverterNode
 *             │
 *             ├──> Child
 *             │    │
 *             │    SUCCESS
 *             │    │
 *             │    │
 *             └────> FAILURE (inverted)
 *
 *
 *        InverterNode
 *             │
 *             ├──> Child
 *             │    │
 *             │    FAILURE
 *             │    │
 *             │    │
 *             └────> SUCCESS (inverted)
 *
 *
 *        InverterNode
 *             │
 *             ├──> Child
 *             │    │
 *             │    RUNNING
 *             │    │
 *             │    │
 *             └────> RUNNING (unchanged)
 * }</pre>
 *
 * <p><b>Common Patterns:</b></p>
 * <ul>
 *   <li><b>Negative Conditions:</b> "If NOT hungry", "While NOT at target"</li>
 *   <li><b>Failure Checking:</b> Detecting when something has NOT failed yet</li>
 *   <li><b>Guard Clauses:</b> Preventing execution when condition is NOT met</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Not thread-safe - assumes single-threaded game tick execution</li>
 *   <li>Child node is responsible for its own thread safety</li>
 * </ul>
 *
 * @see BTNode
 * @see com.minewright.behavior.composite.SequenceNode
 * @see com.minewright.behavior.composite.SelectorNode
 * @see RepeaterNode
 * @see NodeStatus
 * @since 1.0.0
 */
public class InverterNode implements BTNode {
    private static final Logger LOGGER = TestLogger.getLogger(InverterNode.class);

    /**
     * The child node whose result will be inverted.
     */
    private final BTNode child;

    /**
     * Optional name for this inverter (useful for debugging).
     */
    private final String name;

    /**
     * Creates an inverter node for the given child.
     *
     * @param child The child node to invert
     * @throws IllegalArgumentException if child is null
     */
    public InverterNode(BTNode child) {
        this(null, child);
    }

    /**
     * Creates an inverter node with a name for the given child.
     *
     * @param name The name for this inverter (for debugging)
     * @param child The child node to invert
     * @throws IllegalArgumentException if child is null
     */
    public InverterNode(String name, BTNode child) {
        if (child == null) {
            throw new IllegalArgumentException("Child cannot be null");
        }
        this.name = name;
        this.child = child;
        LOGGER.debug("Created InverterNode '{}' for child '{}'",
            getName(), child.getName());
    }

    @Override
    public NodeStatus tick(BTBlackboard blackboard) {
        LOGGER.trace("InverterNode '{}' ticking child '{}'",
            getName(), child.getName());

        NodeStatus childStatus = child.tick(blackboard);
        NodeStatus result = childStatus.invert();

        LOGGER.trace("InverterNode '{}' child returned {}, returning {}",
            getName(), childStatus, result);

        return result;
    }

    @Override
    public void reset() {
        child.reset();
        LOGGER.debug("InverterNode '{}' reset", getName());
    }

    @Override
    public boolean isComplete() {
        return child.isComplete();
    }

    @Override
    public String getName() {
        return name != null ? name : getDefaultName();
    }

    @Override
    public String getDescription() {
        return String.format("Inverter[%s]", child.getName());
    }

    /**
     * Gets the child node.
     *
     * @return The child node
     */
    public BTNode getChild() {
        return child;
    }
}
