package com.minewright.behavior.decorator;

import com.minewright.behavior.BTBlackboard;
import com.minewright.behavior.BTNode;
import com.minewright.behavior.NodeStatus;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

/**
 * Limits execution frequency by enforcing a cooldown period.
 *
 * <p><b>Purpose:</b></p>
 * <p>CooldownNode prevents its child from executing too frequently. After the child
 * executes (whether success or failure), the node enters a cooldown period during
 * which it returns FAILURE without ticking the child. Once the cooldown expires,
 * the child can execute again.</p>
 *
 * <p><b>Execution Logic:</b></p>
 * <pre>
 * On first tick or after cooldown expires:
 *   1. Tick child
 *   2. Record execution time
 *   3. Return child's status
 *
 * During cooldown period:
 *   1. Do NOT tick child
 *   2. Return FAILURE immediately
 *   3. Check if cooldown has expired on next tick
 * </pre>
 *
 * <p><b>State Management:</b></p>
 * <ul>
 *   <li>Tracks last execution time (in milliseconds)</li>
 *   <li>Cooldown duration is configured in milliseconds</li>
 *   <li>Uses system time via {@link System#currentTimeMillis()}</li>
 *   <li>Cooldown resets when reset() is called</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Example 1: Limit ability usage (5 second cooldown)
 * BTNode specialAbility = new CooldownNode(
 *     new ActionNode(new SpecialAttackAction()),
 *     5000 // 5 seconds
 * );
 *
 * // Example 2: Throttle speech (10 second cooldown)
 * BTNode speech = new CooldownNode(
 *     new ActionNode(new SaySomethingAction()),
 *     10000 // 10 seconds
 * );
 *
 * // Example 3: Prevent spam (1 second cooldown)
 * BTNode noSpam = new CooldownNode(
 *     new ActionNode(new SendMessageAction()),
 *     1000 // 1 second
 * );
 * }</pre>
 *
 * <p><b>Behavior Diagram:</b></p>
 * <pre>
 * Time: 0ms     1000ms  2000ms  3000ms  4000ms
 *       │         │       │       │       │
 *       Child     █       █       █       Child
 *       SUCCESS   █       █       █       SUCCESS
 *                 █       █       █
 *       Cooldown  ████████████████
 *
 * █ = FAILURE (cooldown active)
 * </pre>
 *
 * <p><b>Common Patterns:</b></p>
 * <ul>
 *   <li><b>Ability Cooldowns:</b> Limit special attacks, spells, abilities</li>
 *   <li><b>Throttling:</b> Prevent rapid message sending, scanning, etc.</li>
 *   <li><b>Rate Limiting:</b> Control frequency of expensive operations</li>
 *   <li><b>Debounce:</b> Prevent repeated execution of same action</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Not thread-safe - assumes single-threaded game tick execution</li>
 *   <li>Uses system time which may vary across threads</li>
 *   <li>Child node is responsible for its own thread safety</li>
 * </ul>
 *
 * @see BTNode
 * @see InverterNode
 * @see RepeaterNode
 * @see NodeStatus
 * @since 1.0.0
 */
public class CooldownNode implements BTNode {
    private static final Logger LOGGER = TestLogger.getLogger(CooldownNode.class);

    /**
     * The child node to execute with cooldown.
     */
    private final BTNode child;

    /**
     * The cooldown duration in milliseconds.
     */
    private final long cooldownMs;

    /**
     * The time (ms) when the child was last executed.
     * 0 means never executed.
     */
    private long lastExecutionTime;

    /**
     * Optional name for this cooldown node (useful for debugging).
     */
    private final String name;

    /**
     * Creates a cooldown node for the given child and duration.
     *
     * @param child The child node to execute with cooldown
     * @param cooldownMs The cooldown duration in milliseconds
     * @throws IllegalArgumentException if child is null or cooldownMs < 0
     */
    public CooldownNode(BTNode child, long cooldownMs) {
        this(null, child, cooldownMs);
    }

    /**
     * Creates a cooldown node with a name.
     *
     * @param name The name for this cooldown node (for debugging)
     * @param child The child node to execute with cooldown
     * @param cooldownMs The cooldown duration in milliseconds
     * @throws IllegalArgumentException if child is null or cooldownMs < 0
     */
    public CooldownNode(String name, BTNode child, long cooldownMs) {
        if (child == null) {
            throw new IllegalArgumentException("Child cannot be null");
        }
        if (cooldownMs < 0) {
            throw new IllegalArgumentException("Cooldown duration cannot be negative");
        }
        this.name = name;
        this.child = child;
        this.cooldownMs = cooldownMs;
        this.lastExecutionTime = 0;
        LOGGER.debug("Created CooldownNode '{}' for child '{}' with cooldown {}ms",
            getName(), child.getName(), cooldownMs);
    }

    @Override
    public NodeStatus tick(BTBlackboard blackboard) {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastExecution = currentTime - lastExecutionTime;

        // Check if we're in cooldown
        if (lastExecutionTime > 0 && timeSinceLastExecution < cooldownMs) {
            long remainingMs = cooldownMs - timeSinceLastExecution;
            LOGGER.trace("CooldownNode '{}' in cooldown, {}ms remaining",
                getName(), remainingMs);
            return NodeStatus.FAILURE;
        }

        // Cooldown has expired (or first execution), tick child
        LOGGER.trace("CooldownNode '{}' cooldown expired (or first execution), ticking child '{}'",
            getName(), child.getName());

        NodeStatus childStatus = child.tick(blackboard);

        // Record execution time
        lastExecutionTime = currentTime;

        LOGGER.debug("CooldownNode '{}' executed child, result: {}, cooldown starts now ({}ms)",
            getName(), childStatus, cooldownMs);

        return childStatus;
    }

    @Override
    public void reset() {
        long previousTime = lastExecutionTime;
        lastExecutionTime = 0;
        child.reset();
        LOGGER.debug("CooldownNode '{}' reset (last execution was {}ms ago)",
            getName(), previousTime > 0 ? System.currentTimeMillis() - previousTime : "never");
    }

    @Override
    public boolean isComplete() {
        // Cooldown node is never complete - it's a recurring decorator
        return false;
    }

    @Override
    public String getName() {
        return name != null ? name : getDefaultName();
    }

    @Override
    public String getDescription() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastExecution = lastExecutionTime > 0
            ? currentTime - lastExecutionTime
            : Long.MAX_VALUE;
        long remainingMs = Math.max(0, cooldownMs - timeSinceLastExecution);

        String status;
        if (lastExecutionTime == 0) {
            status = "ready";
        } else if (remainingMs > 0) {
            status = String.format("cooldown: %dms", remainingMs);
        } else {
            status = "ready";
        }

        return String.format("Cooldown[%s, %dms] (%s)", child.getName(), cooldownMs, status);
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
     * Gets the cooldown duration.
     *
     * @return The cooldown duration in milliseconds
     */
    public long getCooldownMs() {
        return cooldownMs;
    }

    /**
     * Gets the time when the child was last executed.
     *
     * @return The last execution time in milliseconds since epoch, or 0 if never executed
     */
    public long getLastExecutionTime() {
        return lastExecutionTime;
    }

    /**
     * Gets the remaining cooldown time.
     *
     * @return The remaining cooldown in milliseconds, or 0 if not in cooldown
     */
    public long getRemainingCooldown() {
        if (lastExecutionTime == 0) {
            return 0;
        }
        long timeSinceLastExecution = System.currentTimeMillis() - lastExecutionTime;
        return Math.max(0, cooldownMs - timeSinceLastExecution);
    }

    /**
     * Checks if the node is currently in cooldown.
     *
     * @return true if in cooldown, false if ready to execute
     */
    public boolean isInCooldown() {
        return getRemainingCooldown() > 0;
    }

    /**
     * Manually sets the last execution time.
     *
     * <p>Useful for testing or synchronizing cooldowns with external events.</p>
     *
     * @param time The time to set as last execution (ms since epoch)
     */
    public void setLastExecutionTime(long time) {
        this.lastExecutionTime = time;
    }
}
