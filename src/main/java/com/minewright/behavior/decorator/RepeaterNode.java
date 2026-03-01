package com.minewright.behavior.decorator;

import com.minewright.behavior.BTBlackboard;
import com.minewright.behavior.BTNode;
import com.minewright.behavior.NodeStatus;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

/**
 * Repeats its child node a specified number of times.
 *
 * <p><b>Purpose:</b></p>
 * <p>RepeaterNode executes its child multiple times, returning SUCCESS only if
 * the child succeeds the specified number of times. If the child fails at any
 * point, the repeater fails immediately.</p>
 *
 * <p><b>Execution Logic:</b></p>
 * <pre>
 * Initialize repeat count to 0
 * While repeat count < target count:
 *   1. Tick child
 *   2. If child returns FAILURE, return FAILURE immediately
 *   3. If child returns RUNNING, return RUNNING (continue next tick)
 *   4. If child returns SUCCESS, increment count, reset child, continue
 *
 * When target count reached, return SUCCESS
 * </pre>
 *
 * <p><b>State Management:</b></p>
 * <ul>
 *   <li>Tracks current repeat count</li>
 *   <li>Resets child after each successful repetition</li>
 *   <li>Count resets to 0 when reset() is called</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Example 1: Mine 10 logs
 * BTNode gatherWood = new RepeaterNode(
 *     new ActionNode(new MineBlockAction()),
 *     10
 * );
 *
 * // Example 2: Attack 3 times
 * BTNode tripleAttack = new RepeaterNode(
 *     new ActionNode(new AttackAction()),
 *     3
 * );
 *
 * // Example 3: Repeat indefinitely until failure
 * BTNode repeatUntilFail = new RepeaterNode(
 *     new ConditionNode(() -> isStillHealthy()),
 *     RepeaterNode.INDEFINITELY
 * );
 * }</pre>
 *
 * <p><b>Behavior Diagram:</b></p>
 * <pre>
 *        RepeaterNode (count=3)
 *             │
 *             ├──> Repeat 1: Child SUCCESS
 *             │    count = 1
 *             │
 *             ├──> Repeat 2: Child SUCCESS
 *             │    count = 2
 *             │
 *             ├──> Repeat 3: Child SUCCESS
 *             │    count = 3 (target reached)
 *             │
 *             └────> SUCCESS
 *
 *
 *        RepeaterNode (count=3, failure case)
 *             │
 *             ├──> Repeat 1: Child SUCCESS
 *             │    count = 1
 *             │
 *             ├──> Repeat 2: Child FAILURE
 *             │
 *             └────> FAILURE (immediate)
 * }</pre>
 *
 * <p><b>Common Patterns:</b></p>
 * <ul>
 *   <li><b>Bounded Loops:</b> "Do this N times" - gather N resources, attack N times</li>
 *   <li><b>Retry Logic:</b> "Keep trying N times" - attempt to pick lock multiple times</li>
 *   <li><b>Indefinite Loops:</b> "Keep doing until fails" - keep mining while can see ore</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Not thread-safe - assumes single-threaded game tick execution</li>
 *   <li>Child node is responsible for its own thread safety</li>
 * </ul>
 *
 * @see BTNode
 * @see InverterNode
 * @see CooldownNode
 * @see NodeStatus
 * @since 1.0.0
 */
public class RepeaterNode implements BTNode {
    private static final Logger LOGGER = TestLogger.getLogger(RepeaterNode.class);

    /**
     * Special value for repeating indefinitely until failure.
     * The repeater will continue executing as long as the child succeeds.
     */
    public static final int INDEFINITELY = -1;

    /**
     * The child node to repeat.
     */
    private final BTNode child;

    /**
     * The target number of successful repetitions.
     * Use {@link #INDEFINITELY} to repeat until failure.
     */
    private final int targetCount;

    /**
     * Current number of successful repetitions.
     */
    private int currentCount;

    /**
     * Optional name for this repeater (useful for debugging).
     */
    private final String name;

    /**
     * Creates a repeater that executes the child a specific number of times.
     *
     * @param child The child node to repeat
     * @param count The number of times to repeat (or INDEFINITELY)
     * @throws IllegalArgumentException if child is null or count < -1
     */
    public RepeaterNode(BTNode child, int count) {
        this(null, child, count);
    }

    /**
     * Creates a repeater with a name.
     *
     * @param name The name for this repeater (for debugging)
     * @param child The child node to repeat
     * @param count The number of times to repeat (or INDEFINITELY)
     * @throws IllegalArgumentException if child is null or count < -1
     */
    public RepeaterNode(String name, BTNode child, int count) {
        if (child == null) {
            throw new IllegalArgumentException("Child cannot be null");
        }
        if (count < INDEFINITELY) {
            throw new IllegalArgumentException("Count must be >= -1 (INDEFINITELY)");
        }
        this.name = name;
        this.child = child;
        this.targetCount = count;
        this.currentCount = 0;
        LOGGER.debug("Created RepeaterNode '{}' for child '{}' with count {}",
            getName(), child.getName(), count == INDEFINITELY ? "INDEFINITELY" : count);
    }

    @Override
    public NodeStatus tick(BTBlackboard blackboard) {
        // Check if we've already reached the target
        if (targetCount != INDEFINITELY && currentCount >= targetCount) {
            LOGGER.debug("RepeaterNode '{}' already reached target count {}",
                getName(), targetCount);
            return NodeStatus.SUCCESS;
        }

        // Tick the child
        LOGGER.trace("RepeaterNode '{}' (repeat {}/{}) ticking child '{}'",
            getName(), currentCount + 1, targetCount == INDEFINITELY ? "∞" : targetCount, child.getName());

        NodeStatus childStatus = child.tick(blackboard);

        switch (childStatus) {
            case FAILURE:
                // Child failed - reset count and return failure
                LOGGER.debug("RepeaterNode '{}' child failed at repeat {}, resetting and returning FAILURE",
                    getName(), currentCount + 1);
                currentCount = 0;
                return NodeStatus.FAILURE;

            case RUNNING:
                // Child still running
                LOGGER.trace("RepeaterNode '{}' child still running at repeat {}",
                    getName(), currentCount + 1);
                return NodeStatus.RUNNING;

            case SUCCESS:
                // Child succeeded - increment count
                currentCount++;
                LOGGER.trace("RepeaterNode '{}' child succeeded, count now {}",
                    getName(), currentCount);

                // Reset child for next repetition
                child.reset();

                // Check if target reached
                if (targetCount != INDEFINITELY && currentCount >= targetCount) {
                    LOGGER.debug("RepeaterNode '{}' reached target count {}, returning SUCCESS",
                        getName(), targetCount);
                    return NodeStatus.SUCCESS;
                }

                // Continue repeating
                return NodeStatus.RUNNING;
        }

        // Should never reach here
        return NodeStatus.FAILURE;
    }

    @Override
    public void reset() {
        int previousCount = currentCount;
        currentCount = 0;
        child.reset();
        LOGGER.debug("RepeaterNode '{}' reset (was at count {})",
            getName(), previousCount);
    }

    @Override
    public boolean isComplete() {
        // Complete only if we've reached the target count
        return targetCount != INDEFINITELY && currentCount >= targetCount;
    }

    @Override
    public String getName() {
        return name != null ? name : getDefaultName();
    }

    @Override
    public String getDescription() {
        String targetStr = targetCount == INDEFINITELY ? "∞" : String.valueOf(targetCount);
        return String.format("Repeater[%s] %d/%s", child.getName(), currentCount, targetStr);
    }

    /**
     * Gets the child node.
     *
     * @return The child node
     */
    public BTNode getChild() {
        return child;
    }

    /**
     * Gets the target repeat count.
     *
     * @return The target count, or INDEFINITELY if repeating forever
     */
    public int getTargetCount() {
        return targetCount;
    }

    /**
     * Gets the current repeat count.
     *
     * @return The number of successful repetitions so far
     */
    public int getCurrentCount() {
        return currentCount;
    }

    /**
     * Checks if this repeater repeats indefinitely.
     *
     * @return true if repeating indefinitely (until failure)
     */
    public boolean isIndefinite() {
        return targetCount == INDEFINITELY;
    }
}
