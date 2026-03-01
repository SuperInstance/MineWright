package com.minewright.behavior.leaf;

import com.minewright.behavior.BTBlackboard;
import com.minewright.behavior.BTNode;
import com.minewright.behavior.NodeStatus;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Leaf node that evaluates a condition predicate.
 *
 * <p><b>Purpose:</b></p>
 * <p>ConditionNode checks a condition and returns SUCCESS if the condition is true,
 * FAILURE if false. It never returns RUNNING since conditions are evaluated
 * instantaneously. This makes condition nodes ideal for guards and decision points
 * in behavior trees.</p>
 *
 * <p><b>Execution Logic:</b></p>
 * <pre>
 * Evaluate condition:
 *   - If condition returns true, return SUCCESS
 *   - If condition returns false, return FAILURE
 * </pre>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Example 1: Check if has weapon
 * BTNode hasWeapon = new ConditionNode(
 *     () -> blackboard.getEntity().getMainHandItem().isWeapon()
 * );
 *
 * // Example 2: Check if entity is in range
 * BTNode inRange = new ConditionNode(
 *     blackboard -> {
 *         double distance = blackboard.getEntity().distanceTo(target);
 *         return distance < 10.0;
 *     }
 * );
 *
 * // Example 3: Combined with sequence for guard
 * BTNode attackIfArmed = new SequenceNode(
 *     new ConditionNode(() -> hasWeapon()),
 *     new ActionNode(new AttackAction())
 * );
 *
 * // Example 4: Check inventory
 * BTNode hasWood = new ConditionNode(
 *     () -> blackboard.getInt("inventory.wood", 0) > 0
 * );
 * }</pre>
 *
 * <p><b>Common Patterns:</b></p>
 * <ul>
 *   <li><b>Guards:</b> Prevent execution unless conditions are met</li>
 *   <li><b>Decision Points:</b> Branch behavior tree based on world state</li>
 *   <li><b>Preconditions:</b> Check requirements before actions</li>
 *   <li><b>State Queries:</b> Check entity state, inventory, position, etc.</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Condition is evaluated on the calling thread</li>
 *   <li>Condition function must be thread-safe if used in parallel nodes</li>
 *   <li>Blackboard access is thread-safe via ConcurrentHashMap</li>
 * </ul>
 *
 * @see BTNode
 * @see ActionNode
 * @see com.minewright.behavior.composite.SequenceNode
 * @see com.minewright.behavior.composite.SelectorNode
 * @see NodeStatus
 * @since 1.0.0
 */
public class ConditionNode implements BTNode {
    private static final Logger LOGGER = TestLogger.getLogger(ConditionNode.class);

    /**
     * Supplier that evaluates the condition.
     * Returns true if condition is met (SUCCESS), false otherwise (FAILURE).
     */
    private final BooleanSupplier condition;

    /**
     * Predicate that evaluates the condition with blackboard access.
     * Takes BTBlackboard as parameter for more complex conditions.
     */
    private final Predicate<BTBlackboard> conditionWithBlackboard;

    /**
     * Optional name for this condition (useful for debugging).
     */
    private final String name;

    /**
     * Creates a condition node using a BooleanSupplier.
     *
     * <p>The supplier cannot access the blackboard directly. Use the
     * {@link #ConditionNode(String, Predicate)} constructor for blackboard access.</p>
     *
     * @param condition The condition to evaluate
     * @throws IllegalArgumentException if condition is null
     */
    public ConditionNode(BooleanSupplier condition) {
        this(null, condition);
    }

    /**
     * Creates a named condition node using a BooleanSupplier.
     *
     * @param name The name for this condition (for debugging)
     * @param condition The condition to evaluate
     * @throws IllegalArgumentException if condition is null
     */
    public ConditionNode(String name, BooleanSupplier condition) {
        this(name, condition != null
            ? (bb) -> condition.getAsBoolean()
            : null);
    }

    /**
     * Creates a condition node using a Predicate with blackboard access.
     *
     * <p>The predicate receives the blackboard, enabling access to entity,
     * world state, and stored values.</p>
     *
     * @param condition The condition to evaluate
     * @throws IllegalArgumentException if condition is null
     */
    public ConditionNode(Predicate<BTBlackboard> condition) {
        this(null, condition);
    }

    /**
     * Creates a named condition node using a Predicate with blackboard access.
     *
     * @param name The name for this condition (for debugging)
     * @param condition The condition to evaluate
     * @throws IllegalArgumentException if condition is null
     */
    public ConditionNode(String name, Predicate<BTBlackboard> condition) {
        this.conditionWithBlackboard = Objects.requireNonNull(condition, "Condition cannot be null");
        this.condition = null; // Not used when Predicate is provided
        this.name = name;
        LOGGER.debug("Created ConditionNode '{}'", getName());
    }

    @Override
    public NodeStatus tick(BTBlackboard blackboard) {
        boolean result;

        if (conditionWithBlackboard != null) {
            result = conditionWithBlackboard.test(blackboard);
        } else if (condition != null) {
            result = condition.getAsBoolean();
        } else {
            LOGGER.error("ConditionNode '{}' has no condition set", getName());
            return NodeStatus.FAILURE;
        }

        NodeStatus status = result ? NodeStatus.SUCCESS : NodeStatus.FAILURE;

        LOGGER.trace("ConditionNode '{}' evaluated to {} -> {}",
            getName(), result, status);

        return status;
    }

    @Override
    public void reset() {
        // No state to reset - conditions are stateless
        LOGGER.trace("ConditionNode '{}' reset (no-op)", getName());
    }

    @Override
    public boolean isComplete() {
        // Conditions are always "complete" - they never return RUNNING
        return true;
    }

    @Override
    public String getName() {
        return name != null ? name : getDefaultName();
    }

    @Override
    public String getDescription() {
        return String.format("Condition[%s]", getName());
    }

    /**
     * Tests this condition without affecting behavior tree state.
     *
     * <p>Useful for logging, debugging, or checking conditions before
     * adding them to a behavior tree.</p>
     *
     * @param blackboard The blackboard to test with
     * @return true if condition would succeed, false otherwise
     */
    public boolean test(BTBlackboard blackboard) {
        if (conditionWithBlackboard != null) {
            return conditionWithBlackboard.test(blackboard);
        } else if (condition != null) {
            return condition.getAsBoolean();
        }
        return false;
    }

    // Static factory methods for common conditions

    /**
     * Creates a condition that checks if a blackboard key exists.
     *
     * @param key The key to check
     * @return A new ConditionNode
     */
    public static ConditionNode hasKey(String key) {
        return new ConditionNode("hasKey:" + key, bb -> bb.containsKey(key));
    }

    /**
     * Creates a condition that checks if a blackboard boolean value is true.
     *
     * @param key The key to check
     * @return A new ConditionNode
     */
    public static ConditionNode isTrue(String key) {
        return new ConditionNode("isTrue:" + key, bb -> bb.getBoolean(key, false));
    }

    /**
     * Creates a condition that checks if a blackboard boolean value is false.
     *
     * @param key The key to check
     * @return A new ConditionNode
     */
    public static ConditionNode isFalse(String key) {
        return new ConditionNode("isFalse:" + key, bb -> !bb.getBoolean(key, true));
    }

    /**
     * Creates a condition that checks if a numeric value is greater than a threshold.
     *
     * @param key The key to check
     * @param threshold The threshold value
     * @return A new ConditionNode
     */
    public static ConditionNode greaterThan(String key, int threshold) {
        return new ConditionNode("greaterThan:" + key, bb -> bb.getInt(key, 0) > threshold);
    }

    /**
     * Creates a condition that checks if a numeric value is less than a threshold.
     *
     * @param key The key to check
     * @param threshold The threshold value
     * @return A new ConditionNode
     */
    public static ConditionNode lessThan(String key, int threshold) {
        return new ConditionNode("lessThan:" + key, bb -> bb.getInt(key, Integer.MAX_VALUE) < threshold);
    }

    /**
     * Creates a condition that checks if a numeric value equals a value.
     *
     * @param key The key to check
     * @param value The expected value
     * @return A new ConditionNode
     */
    public static ConditionNode equals(String key, int value) {
        return new ConditionNode("equals:" + key, bb -> bb.getInt(key, Integer.MIN_VALUE) == value);
    }

    /**
     * Creates a negated condition (logical NOT).
     *
     * @param condition The condition to negate
     * @return A new ConditionNode that returns the opposite
     */
    public static ConditionNode not(ConditionNode condition) {
        return new ConditionNode("not:" + condition.getName(),
            bb -> !condition.test(bb));
    }

    /**
     * Creates a combined condition (logical AND).
     *
     * <p>Returns SUCCESS only if all conditions succeed.</p>
     *
     * @param conditions Conditions to combine
     * @return A new ConditionNode
     */
    public static ConditionNode and(ConditionNode... conditions) {
        return new ConditionNode("and",
            bb -> {
                for (ConditionNode cond : conditions) {
                    if (!cond.test(bb)) return false;
                }
                return true;
            });
    }

    /**
     * Creates a combined condition (logical OR).
     *
     * <p>Returns SUCCESS if any condition succeeds.</p>
     *
     * @param conditions Conditions to combine
     * @return A new ConditionNode
     */
    public static ConditionNode or(ConditionNode... conditions) {
        return new ConditionNode("or",
            bb -> {
                for (ConditionNode cond : conditions) {
                    if (cond.test(bb)) return true;
                }
                return false;
            });
    }
}
