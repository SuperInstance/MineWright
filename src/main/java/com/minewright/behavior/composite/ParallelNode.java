package com.minewright.behavior.composite;

import com.minewright.behavior.BTBlackboard;
import com.minewright.behavior.BTNode;
import com.minewright.behavior.NodeStatus;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes all children simultaneously with configurable success policy.
 *
 * <p><b>Purpose:</b></p>
 * <p>ParallelNode executes all children each tick, enabling concurrent behaviors
 * like monitoring multiple conditions or performing independent actions simultaneously.
 * The final result is determined by the configured {@link ParallelPolicy}.</p>
 *
 * <p><b>Execution Logic:</b></p>
 * <pre>
 * On each tick:
 *   1. Tick all children (in order, not truly parallel threads)
 *   2. Collect all child statuses
 *   3. Apply policy to determine final result
 *   4. Return final result based on policy
 *
 * Policies:
 *   - SUCCESS_IF_ALL_SUCCEED: Return SUCCESS only if all succeed
 *   - SUCCESS_IF_ANY_SUCCEEDS: Return SUCCESS if any succeeds
 *   - FAILURE_IF_ANY_FAILS: Return FAILURE if any fails
 *   - FAILURE_IF_ALL_FAIL: Return FAILURE only if all fail
 * </pre>
 *
 * <p><b>Important Notes:</b></p>
 * <ul>
 *   <li><b>Not Thread-Parallel:</b> Children are executed sequentially on the same thread.
 *       This is "logical parallelism" - all children run each tick, not true concurrency.</li>
 *   <li><b>All Children Tick:</b> Every child is ticked every time, regardless of status.
 *       Children with RUNNING status continue; terminal children are ticked again (idempotent).</li>
 *   <li><b>Completion:</b> The parallel node completes when the policy condition is met.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Example 1: Monitor multiple conditions (all must succeed)
 * BTNode safetyCheck = new ParallelNode(ParallelPolicy.SUCCESS_IF_ALL_SUCCEED,
 *     new ConditionNode(() -> !isNearThreat()),
 *     new ConditionNode(() -> hasHealth()),
 *     new ConditionNode(() -> hasAmmo())
 * );
 *
 * // Example 2: Try multiple approaches (any success is good)
 * BTNode searchForTarget = new ParallelNode(ParallelPolicy.SUCCESS_IF_ANY_SUCCEEDS,
 *     new ActionNode(new ScanVisualAction()),
 *     new ActionNode(new ScanAudioAction()),
 *     new ActionNode(new ScanMemoryAction())
 * );
 *
 * // Example 3: Multi-task building (fail if any fails)
 * BTNode parallelBuild = new ParallelNode(ParallelPolicy.FAILURE_IF_ANY_FAILS,
 *     new ActionNode(new BuildNorthWallAction()),
 *     new ActionNode(new BuildSouthWallAction()),
 *     new ActionNode(new BuildEastWallAction())
 * );
 * }</pre>
 *
 * <p><b>Behavior Diagram:</b></p>
 * <pre>
 *        ParallelNode (SUCCESS_IF_ALL_SUCCEED)
 *             │
 *     ┌───────┼───────┐
 *     │       │       │
 *   Child 1 Child 2 Child 3
 *   SUCCESS RUNNING SUCCESS
 *        │       │       │
 *        └───────┴───────┘
 *                │
 *            RUNNING (waiting for Child 2)
 *
 *        ParallelNode (SUCCESS_IF_ALL_SUCCEED)
 *             │
 *     ┌───────┼───────┐
 *     │       │       │
 *   Child 1 Child 2 Child 3
 *   SUCCESS SUCCESS SUCCESS
 *        │       │       │
 *        └───────┴───────┘
 *                │
 *            SUCCESS (all succeeded)
 * }</pre>
 *
 * <p><b>Policy Reference:</b></p>
 * <table border="1">
 *   <tr><th>Policy</th><th>Success Condition</th><th>Failure Condition</th><th>Running Condition</th></tr>
 *   <tr><td>SUCCESS_IF_ALL_SUCCEED</td><td>All children SUCCESS</td><td>Any child FAILURE</td><td>Any child RUNNING</td></tr>
 *   <tr><td>SUCCESS_IF_ANY_SUCCEEDS</td><td>Any child SUCCESS</td><td>All children FAILURE</td><td>No SUCCESS and any RUNNING</td></tr>
 *   <tr><td>FAILURE_IF_ANY_FAILS</td><td>All children SUCCESS</td><td>Any child FAILURE</td><td>Any child RUNNING</td></tr>
 *   <tr><td>FAILURE_IF_ALL_FAIL</td><td>Any child SUCCESS</td><td>All children FAILURE</td><td>No FAILURE and any RUNNING</td></tr>
 * </table>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Not thread-safe - assumes single-threaded game tick execution</li>
 *   <li>Children are ticked sequentially, not concurrently</li>
 *   <li>Thread-safe access to blackboard is responsibility of child nodes</li>
 * </ul>
 *
 * @see BTNode
 * @see SequenceNode
 * @see SelectorNode
 * @see ParallelPolicy
 * @see NodeStatus
 * @since 1.0.0
 */
public class ParallelNode implements BTNode {
    private static final Logger LOGGER = TestLogger.getLogger(ParallelNode.class);

    /**
     * Defines how the parallel node determines its final status.
     */
    public enum ParallelPolicy {
        /**
         * Returns SUCCESS only if all children succeed.
         * Returns FAILURE immediately if any child fails.
         * Returns RUNNING if any child is running (no failures yet).
         *
         * <p>Use for: Strict requirements where all conditions must be met.</p>
         */
        SUCCESS_IF_ALL_SUCCEED,

        /**
         * Returns SUCCESS as soon as any child succeeds.
         * Returns FAILURE only if all children fail.
         * Returns RUNNING while waiting for success (if any child is running).
         *
         * <p>Use for: Trying multiple approaches, first success wins.</p>
         */
        SUCCESS_IF_ANY_SUCCEEDS,

        /**
         * Returns FAILURE as soon as any child fails.
         * Returns SUCCESS only if all children succeed.
         * Returns RUNNING if any child is running (no failures yet).
         *
         * <p>Use for: Critical tasks where any failure is unacceptable.</p>
         */
        FAILURE_IF_ANY_FAILS,

        /**
         * Returns FAILURE only if all children fail.
         * Returns SUCCESS as soon as any child succeeds.
         * Returns RUNNING while waiting (if no failure yet and any child is running).
         *
         * <p>Use for: Optimistic execution where any success is acceptable.</p>
         */
        FAILURE_IF_ALL_FAIL
    }

    /**
     * Child nodes to execute in parallel.
     * List is modifiable to support dynamic child addition.
     */
    private final List<BTNode> children;

    /**
     * The policy for determining final status.
     */
    private final ParallelPolicy policy;

    /**
     * Last known status of each child.
     * Used for logging and policy evaluation.
     */
    private final ConcurrentHashMap<BTNode, NodeStatus> lastStatuses;

    /**
     * Flag indicating if this parallel node has completed.
     */
    private boolean completed;

    /**
     * The final result once completed.
     */
    private NodeStatus finalResult;

    /**
     * Optional name for this parallel node (useful for debugging).
     */
    private final String name;

    /**
     * Creates a parallel node with the given policy and children.
     *
     * @param policy The success/failure policy
     * @param children Child nodes to execute in parallel
     * @throws IllegalArgumentException if policy is null, children is null or empty
     */
    public ParallelNode(ParallelPolicy policy, BTNode... children) {
        this(null, policy, children);
    }

    /**
     * Creates a parallel node with the given name, policy, and children.
     *
     * @param name The name for this parallel node (for debugging)
     * @param policy The success/failure policy
     * @param children Child nodes to execute in parallel
     * @throws IllegalArgumentException if policy is null, children is null or empty
     */
    public ParallelNode(String name, ParallelPolicy policy, BTNode... children) {
        if (policy == null) {
            throw new IllegalArgumentException("ParallelPolicy cannot be null");
        }
        if (children == null || children.length == 0) {
            throw new IllegalArgumentException("ParallelNode requires at least one child");
        }
        this.name = name;
        this.policy = policy;
        this.children = new ArrayList<>(Arrays.asList(children));
        this.lastStatuses = new ConcurrentHashMap<>();
        this.completed = false;
        this.finalResult = null;
        LOGGER.debug("Created ParallelNode '{}' with policy '{}' and {} children",
            getName(), policy, this.children.size());
    }

    /**
     * Creates a parallel node with the given policy and list of children.
     *
     * @param policy The success/failure policy
     * @param children List of child nodes
     * @throws IllegalArgumentException if policy is null, children is null or empty
     */
    public ParallelNode(ParallelPolicy policy, List<BTNode> children) {
        this(null, policy, children);
    }

    /**
     * Creates a parallel node with a name, policy, and list of children.
     *
     * @param name The name for this parallel node (for debugging)
     * @param policy The success/failure policy
     * @param children List of child nodes
     * @throws IllegalArgumentException if policy is null, children is null or empty
     */
    public ParallelNode(String name, ParallelPolicy policy, List<BTNode> children) {
        if (policy == null) {
            throw new IllegalArgumentException("ParallelPolicy cannot be null");
        }
        if (children == null || children.isEmpty()) {
            throw new IllegalArgumentException("ParallelNode requires at least one child");
        }
        this.name = name;
        this.policy = policy;
        this.children = new ArrayList<>(children);
        this.lastStatuses = new ConcurrentHashMap<>();
        this.completed = false;
        this.finalResult = null;
        LOGGER.debug("Created ParallelNode '{}' with policy '{}' and {} children",
            getName(), policy, this.children.size());
    }

    @Override
    public NodeStatus tick(BTBlackboard blackboard) {
        if (children.isEmpty()) {
            LOGGER.warn("ParallelNode '{}' has no children, returning FAILURE",
                getName());
            return NodeStatus.FAILURE;
        }

        // If already completed, return cached result
        if (completed && finalResult != null) {
            LOGGER.trace("ParallelNode '{}' already completed with {}",
                getName(), finalResult);
            return finalResult;
        }

        // Tick all children and collect statuses
        int successCount = 0;
        int failureCount = 0;
        int runningCount = 0;

        for (BTNode child : children) {
            NodeStatus status = child.tick(blackboard);
            lastStatuses.put(child, status);

            switch (status) {
                case SUCCESS -> successCount++;
                case FAILURE -> failureCount++;
                case RUNNING -> runningCount++;
            }
        }

        LOGGER.trace("ParallelNode '{}' child statuses: {} SUCCESS, {} FAILURE, {} RUNNING",
            getName(), successCount, failureCount, runningCount);

        // Determine result based on policy
        NodeStatus result = applyPolicy(successCount, failureCount, runningCount);

        if (result.isTerminal()) {
            completed = true;
            finalResult = result;
            LOGGER.debug("ParallelNode '{}' completed with {} (policy: {})",
                getName(), result, policy);
        }

        return result;
    }

    /**
     * Applies the parallel policy to determine the final status.
     *
     * @param successCount Number of children returning SUCCESS
     * @param failureCount Number of children returning FAILURE
     * @param runningCount Number of children returning RUNNING
     * @return The determined status
     */
    private NodeStatus applyPolicy(int successCount, int failureCount, int runningCount) {
        int totalChildren = children.size();

        return switch (policy) {
            case SUCCESS_IF_ALL_SUCCEED -> {
                // Fail immediately if any child failed
                if (failureCount > 0) yield NodeStatus.FAILURE;
                // All succeeded
                if (successCount == totalChildren) yield NodeStatus.SUCCESS;
                // Still running
                yield NodeStatus.RUNNING;
            }

            case SUCCESS_IF_ANY_SUCCEEDS -> {
                // Success if any child succeeded
                if (successCount > 0) yield NodeStatus.SUCCESS;
                // All failed
                if (failureCount == totalChildren) yield NodeStatus.FAILURE;
                // Still waiting
                yield NodeStatus.RUNNING;
            }

            case FAILURE_IF_ANY_FAILS -> {
                // Fail immediately if any child failed
                if (failureCount > 0) yield NodeStatus.FAILURE;
                // All succeeded
                if (successCount == totalChildren) yield NodeStatus.SUCCESS;
                // Still running
                yield NodeStatus.RUNNING;
            }

            case FAILURE_IF_ALL_FAIL -> {
                // Success if any child succeeded
                if (successCount > 0) yield NodeStatus.SUCCESS;
                // All failed
                if (failureCount == totalChildren) yield NodeStatus.FAILURE;
                // Still waiting
                yield NodeStatus.RUNNING;
            }
        };
    }

    @Override
    public void reset() {
        completed = false;
        finalResult = null;
        lastStatuses.clear();

        // Reset all children
        for (BTNode child : children) {
            child.reset();
        }

        LOGGER.debug("ParallelNode '{}' reset", getName());
    }

    @Override
    public boolean isComplete() {
        return completed;
    }

    @Override
    public String getName() {
        return name != null ? name : getDefaultName();
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Parallel[").append(policy).append("]: ");
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) sb.append(", ");
            BTNode child = children.get(i);
            NodeStatus status = lastStatuses.get(child);
            sb.append(child.getName());
            if (status != null) {
                sb.append("(").append(status).append(")");
            }
        }
        return sb.toString();
    }

    /**
     * Adds a child node to this parallel node.
     *
     * @param child The child node to add
     * @throws IllegalArgumentException if child is null
     */
    public void addChild(BTNode child) {
        Objects.requireNonNull(child, "Child cannot be null");
        children.add(child);
        LOGGER.debug("ParallelNode '{}' added child: {} (total: {})",
            getName(), child.getName(), children.size());
    }

    /**
     * Removes a child from this parallel node.
     *
     * @param child The child to remove
     * @return true if the child was found and removed
     */
    public boolean removeChild(BTNode child) {
        boolean removed = children.remove(child);
        if (removed) {
            lastStatuses.remove(child);
            LOGGER.debug("ParallelNode '{}' removed child '{}'",
                getName(), child.getName());
        }
        return removed;
    }

    /**
     * Gets the parallel policy.
     *
     * @return The policy for determining final status
     */
    public ParallelPolicy getPolicy() {
        return policy;
    }

    /**
     * Gets the number of children in this parallel node.
     *
     * @return Child count
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * Gets the list of children (unmodifiable).
     *
     * @return Unmodifiable list of children
     */
    public List<BTNode> getChildren() {
        return List.copyOf(children);
    }

    /**
     * Gets the last known status of a child.
     *
     * @param child The child to query
     * @return The last status, or null if not yet ticked
     */
    public NodeStatus getChildStatus(BTNode child) {
        return lastStatuses.get(child);
    }
}
