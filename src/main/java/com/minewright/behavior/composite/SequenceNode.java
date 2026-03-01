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

/**
 * Executes child nodes in sequence until one fails.
 *
 * <p><b>Purpose:</b></p>
 * <p>SequenceNode is the "AND" of behavior trees. It executes each child in order,
 * succeeding only if all children succeed. If any child fails, the sequence stops
 * immediately and returns FAILURE.</p>
 *
 * <p><b>Execution Logic:</b></p>
 * <pre>
 * For each child in order:
 *   1. Tick the child
 *   2. If child returns FAILURE, return FAILURE immediately (fail-fast)
 *   3. If child returns RUNNING, return RUNNING (continue next tick)
 *   4. If child returns SUCCESS, move to next child
 *
 * If all children succeed, return SUCCESS
 * </pre>
 *
 * <p><b>State Management:</b></p>
 * <ul>
 *   <li>Tracks current child index to resume from on next tick</li>
 *   <li>Child index resets to 0 when reset() is called</li>
 *   <li>Returns RUNNING if current child is still executing</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Example 1: Gather wood sequence
 * BTNode gatherWood = new SequenceNode(
 *     new ConditionNode(() -> hasAxe()),
 *     new ActionNode(new MoveToTreeAction()),
 *     new ActionNode(new MineBlockAction()),
 *     new RepeaterNode(new ActionNode(new MineBlockAction()), 9)
 * );
 *
 * // Example 2: Build structure sequence
 * BTNode buildHouse = new SequenceNode(
 *     new ActionNode(new GatherMaterialsAction()),
 *     new ActionNode(new ClearSiteAction()),
 *     new ActionNode(new BuildWallsAction()),
 *     new ActionNode(new BuildRoofAction())
 * );
 *
 * // Example 3: Dynamic sequence
 * SequenceNode sequence = new SequenceNode();
 * sequence.addChild(new ConditionNode(() -> isSafe()));
 * sequence.addChild(new ActionNode(new PerformTaskAction()));
 * }</pre>
 *
 * <p><b>Behavior Diagram:</b></p>
 * <pre>
 *        SequenceNode
 *             │
 *     ┌───────┴───────┐
 *     │               │
 *   Child 1  ────>  Child 2  ────>  Child 3
 *   SUCCESS          SUCCESS          SUCCESS
 *        │               │               │
 *        └───────────────┴───────────────┘
 *                        │
 *                    SUCCESS
 *
 *        SequenceNode (failure case)
 *             │
 *     ┌───────┴───────┐
 *     │               │
 *   Child 1  ────>  Child 2  ────>  Child 3
 *   SUCCESS          FAILURE          (not executed)
 *        │               │
 *        └───────────────┤
 *                        │
 *                    FAILURE
 * }</pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Not thread-safe - assumes single-threaded game tick execution</li>
 *   <li>Should not be ticked concurrently from multiple threads</li>
 *   <li>Thread-safe access to blackboard is responsibility of child nodes</li>
 * </ul>
 *
 * @see BTNode
 * @see SelectorNode
 * @see ParallelNode
 * @see NodeStatus
 * @since 1.0.0
 */
public class SequenceNode implements BTNode {
    private static final Logger LOGGER = TestLogger.getLogger(SequenceNode.class);

    /**
     * Child nodes to execute in sequence.
     * List is modifiable to support dynamic child addition.
     */
    private final List<BTNode> children;

    /**
     * Index of the currently executing child.
     * Resets to 0 on reset() call.
     */
    private int currentChild;

    /**
     * Flag indicating if this sequence has completed.
     * Set to true when all children succeed or any fails.
     */
    private boolean completed;

    /**
     * Optional name for this sequence (useful for debugging).
     */
    private final String name;

    /**
     * Creates a sequence node with the given children.
     *
     * @param children Child nodes to execute in sequence
     * @throws IllegalArgumentException if children is null or empty
     */
    public SequenceNode(BTNode... children) {
        this(null, children);
    }

    /**
     * Creates a sequence node with the given children and name.
     *
     * @param name The name for this sequence (for debugging)
     * @param children Child nodes to execute in sequence
     * @throws IllegalArgumentException if children is null or empty
     */
    public SequenceNode(String name, BTNode... children) {
        if (children == null || children.length == 0) {
            throw new IllegalArgumentException("SequenceNode requires at least one child");
        }
        this.name = name;
        this.children = new ArrayList<>(Arrays.asList(children));
        this.currentChild = 0;
        this.completed = false;
        LOGGER.debug("Created SequenceNode '{}' with {} children",
            getName(), this.children.size());
    }

    /**
     * Creates a sequence node with a list of children.
     *
     * @param children List of child nodes
     * @throws IllegalArgumentException if children is null or empty
     */
    public SequenceNode(List<BTNode> children) {
        this(null, children);
    }

    /**
     * Creates a sequence node with a name and list of children.
     *
     * @param name The name for this sequence (for debugging)
     * @param children List of child nodes
     * @throws IllegalArgumentException if children is null or empty
     */
    public SequenceNode(String name, List<BTNode> children) {
        if (children == null || children.isEmpty()) {
            throw new IllegalArgumentException("SequenceNode requires at least one child");
        }
        this.name = name;
        this.children = new ArrayList<>(children);
        this.currentChild = 0;
        this.completed = false;
        LOGGER.debug("Created SequenceNode '{}' with {} children",
            getName(), this.children.size());
    }

    /**
     * Creates an empty sequence node.
     *
     * <p>Children can be added later using {@link #addChild(BTNode)}.
     * At least one child must be added before ticking.</p>
     */
    public SequenceNode() {
        this.name = null;
        this.children = new ArrayList<>();
        this.currentChild = 0;
        this.completed = false;
        LOGGER.debug("Created empty SequenceNode");
    }

    /**
     * Creates an empty sequence node with a name.
     *
     * @param name The name for this sequence
     */
    public SequenceNode(String name) {
        this.name = name;
        this.children = new ArrayList<>();
        this.currentChild = 0;
        this.completed = false;
        LOGGER.debug("Created empty SequenceNode '{}'", getName());
    }

    @Override
    public NodeStatus tick(BTBlackboard blackboard) {
        if (children.isEmpty()) {
            LOGGER.warn("SequenceNode '{}' has no children, returning FAILURE",
                getName());
            return NodeStatus.FAILURE;
        }

        // If already completed, return cached result
        if (completed) {
            LOGGER.trace("SequenceNode '{}' already completed", getName());
            return NodeStatus.SUCCESS; // All children succeeded
        }

        // Execute children from current index
        while (currentChild < children.size()) {
            BTNode child = children.get(currentChild);
            LOGGER.trace("[{}/{}] Ticking child: {}",
                currentChild + 1, children.size(), child.getName());

            NodeStatus status = child.tick(blackboard);

            switch (status) {
                case FAILURE:
                    // Child failed - reset and return failure
                    LOGGER.debug("SequenceNode '{}' child {} ({}) failed, resetting and returning FAILURE",
                        getName(), currentChild, child.getName());
                    reset();
                    return NodeStatus.FAILURE;

                case RUNNING:
                    // Child still running - return RUNNING
                    LOGGER.trace("SequenceNode '{}' child {} ({}) still running",
                        getName(), currentChild, child.getName());
                    return NodeStatus.RUNNING;

                case SUCCESS:
                    // Child succeeded - move to next
                    currentChild++;
                    LOGGER.trace("SequenceNode '{}' child {} succeeded, moving to next",
                        getName(), currentChild - 1);
                    break;
            }
        }

        // All children succeeded
        LOGGER.debug("SequenceNode '{}' all {} children succeeded",
            getName(), children.size());
        completed = true;
        return NodeStatus.SUCCESS;
    }

    @Override
    public void reset() {
        int previousChild = currentChild;
        currentChild = 0;
        completed = false;

        // Reset all children
        for (BTNode child : children) {
            child.reset();
        }

        LOGGER.debug("SequenceNode '{}' reset (was at child {})",
            getName(), previousChild);
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
        sb.append("Sequence[");
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) sb.append(", ");
            if (i == currentChild) sb.append(">");
            sb.append(children.get(i).getName());
            if (i == currentChild) sb.append("<");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Adds a child node to this sequence.
     *
     * <p>Children are executed in the order they are added.</p>
     *
     * @param child The child node to add
     * @throws IllegalArgumentException if child is null
     */
    public void addChild(BTNode child) {
        Objects.requireNonNull(child, "Child cannot be null");
        children.add(child);
        LOGGER.debug("SequenceNode '{}' added child: {} (total: {})",
            getName(), child.getName(), children.size());
    }

    /**
     * Inserts a child at a specific index.
     *
     * @param index The index to insert at
     * @param child The child node to insert
     * @throws IllegalArgumentException if child is null
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public void insertChild(int index, BTNode child) {
        Objects.requireNonNull(child, "Child cannot be null");
        children.add(index, child);
        LOGGER.debug("SequenceNode '{}' inserted child '{}' at index {}",
            getName(), child.getName(), index);
    }

    /**
     * Removes a child from this sequence.
     *
     * @param child The child to remove
     * @return true if the child was found and removed
     */
    public boolean removeChild(BTNode child) {
        boolean removed = children.remove(child);
        if (removed) {
            LOGGER.debug("SequenceNode '{}' removed child '{}'",
                getName(), child.getName());
        }
        return removed;
    }

    /**
     * Gets the number of children in this sequence.
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
     * Gets the index of the currently executing child.
     *
     * @return Current child index (0 to child count - 1)
     */
    public int getCurrentChildIndex() {
        return currentChild;
    }
}
