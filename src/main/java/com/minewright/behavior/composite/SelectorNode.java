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
 * Tries each child in order until one succeeds.
 *
 * <p><b>Purpose:</b></p>
 * <p>SelectorNode is the "OR" of behavior trees. It executes each child in order,
 * succeeding if any child succeeds. If a child returns RUNNING, it returns RUNNING.
 * If all children fail, it returns FAILURE.</p>
 *
 * <p><b>Execution Logic:</b></p>
 * <pre>
 * For each child in order:
 *   1. Tick the child
 *   2. If child returns SUCCESS, return SUCCESS immediately (found success)
 *   3. If child returns RUNNING, return RUNNING (continue next tick)
 *   4. If child returns FAILURE, try next child
 *
 * If all children fail, return FAILURE
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
 * // Example 1: Combat response selector (try best option first)
 * BTNode combatResponse = new SelectorNode(
 *     new SequenceNode(
 *         new ConditionNode(() -> hasWeapon()),
 *         new ActionNode(new AttackAction())
 *     ),
 *     new SequenceNode(
 *         new ConditionNode(() -> canFlee()),
 *         new ActionNode(new FleeAction())
 *     ),
 *     new ActionNode(new HideAction()) // Last resort
 * );
 *
 * // Example 2: Resource gathering selector (try closest first)
 * BTNode gatherResources = new SelectorNode(
 *     new SequenceNode(
 *         new ConditionNode(() -> isNearby("iron_ore")),
 *         new ActionNode(new MineAction("iron_ore"))
 *     ),
 *     new SequenceNode(
 *         new ConditionNode(() -> isNearby("coal_ore")),
 *         new ActionNode(new MineAction("coal_ore"))
 *     ),
 *     new ActionNode(new WanderAction()) // No resources nearby
 * );
 *
 * // Example 3: Dynamic selector
 * SelectorNode selector = new SelectorNode();
 * selector.addChild(new ActionNode(new AttackAction()));
 * selector.addChild(new ActionNode(new FleeAction()));
 * }</pre>
 *
 * <p><b>Behavior Diagram:</b></p>
 * <pre>
 *        SelectorNode
 *             │
 *     ┌───────┴───────┐
 *     │               │
 *   Child 1  ────>  Child 2  ────>  Child 3
 *   FAILURE          SUCCESS          (not executed)
 *        │               │
 *        └───────────────┤
 *                        │
 *                    SUCCESS
 *
 *        SelectorNode (all fail case)
 *             │
 *     ┌───────┴───────┐
 *     │               │
 *   Child 1  ────>  Child 2  ────>  Child 3
 *   FAILURE          FAILURE          FAILURE
 *        │               │               │
 *        └───────────────┴───────────────┘
 *                        │
 *                    FAILURE
 * }</pre>
 *
 * <p><b>Common Patterns:</b></p>
 * <ul>
 *   <li><b>Fallback Chain:</b> Try best option first, fall back to alternatives</li>
 *   <li><b>Priority Selection:</b> Higher priority options earlier in the list</li>
 *   <li><b>Conditional Execution:</b> Each child checks a condition before acting</li>
 *   <li><b>Error Recovery:</b> Try alternatives if first option fails</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Not thread-safe - assumes single-threaded game tick execution</li>
 *   <li>Should not be ticked concurrently from multiple threads</li>
 *   <li>Thread-safe access to blackboard is responsibility of child nodes</li>
 * </ul>
 *
 * @see BTNode
 * @see SequenceNode
 * @see ParallelNode
 * @see NodeStatus
 * @since 1.0.0
 */
public class SelectorNode implements BTNode {
    private static final Logger LOGGER = TestLogger.getLogger(SelectorNode.class);

    /**
     * Child nodes to try in order.
     * List is modifiable to support dynamic child addition.
     */
    private final List<BTNode> children;

    /**
     * Index of the currently executing child.
     * Resets to 0 on reset() call.
     */
    private int currentChild;

    /**
     * Flag indicating if this selector has completed.
     * Set to true when a child succeeds or all children fail.
     */
    private boolean completed;

    /**
     * The last result returned by this selector.
     * Used to return consistent result after completion.
     */
    private NodeStatus lastResult;

    /**
     * Optional name for this selector (useful for debugging).
     */
    private final String name;

    /**
     * Creates a selector node with the given children.
     *
     * @param children Child nodes to try in order
     * @throws IllegalArgumentException if children is null or empty
     */
    public SelectorNode(BTNode... children) {
        this(null, children);
    }

    /**
     * Creates a selector node with the given children and name.
     *
     * @param name The name for this selector (for debugging)
     * @param children Child nodes to try in order
     * @throws IllegalArgumentException if children is null or empty
     */
    public SelectorNode(String name, BTNode... children) {
        if (children == null || children.length == 0) {
            throw new IllegalArgumentException("SelectorNode requires at least one child");
        }
        this.name = name;
        this.children = new ArrayList<>(Arrays.asList(children));
        this.currentChild = 0;
        this.completed = false;
        this.lastResult = null;
        LOGGER.debug("Created SelectorNode '{}' with {} children",
            getName(), this.children.size());
    }

    /**
     * Creates a selector node with a list of children.
     *
     * @param children List of child nodes
     * @throws IllegalArgumentException if children is null or empty
     */
    public SelectorNode(List<BTNode> children) {
        this(null, children);
    }

    /**
     * Creates a selector node with a name and list of children.
     *
     * @param name The name for this selector (for debugging)
     * @param children List of child nodes
     * @throws IllegalArgumentException if children is null or empty
     */
    public SelectorNode(String name, List<BTNode> children) {
        if (children == null || children.isEmpty()) {
            throw new IllegalArgumentException("SelectorNode requires at least one child");
        }
        this.name = name;
        this.children = new ArrayList<>(children);
        this.currentChild = 0;
        this.completed = false;
        this.lastResult = null;
        LOGGER.debug("Created SelectorNode '{}' with {} children",
            getName(), this.children.size());
    }

    /**
     * Creates an empty selector node.
     *
     * <p>Children can be added later using {@link #addChild(BTNode)}.
     * At least one child must be added before ticking.</p>
     */
    public SelectorNode() {
        this.name = null;
        this.children = new ArrayList<>();
        this.currentChild = 0;
        this.completed = false;
        this.lastResult = null;
        LOGGER.debug("Created empty SelectorNode");
    }

    /**
     * Creates an empty selector node with a name.
     *
     * @param name The name for this selector
     */
    public SelectorNode(String name) {
        this.name = name;
        this.children = new ArrayList<>();
        this.currentChild = 0;
        this.completed = false;
        this.lastResult = null;
        LOGGER.debug("Created empty SelectorNode '{}'", getName());
    }

    @Override
    public NodeStatus tick(BTBlackboard blackboard) {
        if (children.isEmpty()) {
            LOGGER.warn("SelectorNode '{}' has no children, returning FAILURE",
                getName());
            return NodeStatus.FAILURE;
        }

        // If already completed, return cached result
        if (completed && lastResult != null) {
            LOGGER.trace("SelectorNode '{}' already completed with {}",
                getName(), lastResult);
            return lastResult;
        }

        // Try children from current index
        while (currentChild < children.size()) {
            BTNode child = children.get(currentChild);
            LOGGER.trace("[{}/{}] Ticking child: {}",
                currentChild + 1, children.size(), child.getName());

            NodeStatus status = child.tick(blackboard);

            switch (status) {
                case SUCCESS:
                    // Child succeeded - return success
                    LOGGER.debug("SelectorNode '{}' child {} ({}) succeeded, returning SUCCESS",
                        getName(), currentChild, child.getName());
                    completed = true;
                    lastResult = NodeStatus.SUCCESS;
                    return NodeStatus.SUCCESS;

                case RUNNING:
                    // Child still running - return RUNNING
                    LOGGER.trace("SelectorNode '{}' child {} ({}) still running",
                        getName(), currentChild, child.getName());
                    return NodeStatus.RUNNING;

                case FAILURE:
                    // Child failed - try next
                    currentChild++;
                    LOGGER.trace("SelectorNode '{}' child {} failed, trying next",
                        getName(), currentChild - 1);
                    break;
            }
        }

        // All children failed
        LOGGER.debug("SelectorNode '{}' all {} children failed, returning FAILURE",
            getName(), children.size());
        completed = true;
        lastResult = NodeStatus.FAILURE;
        return NodeStatus.FAILURE;
    }

    @Override
    public void reset() {
        int previousChild = currentChild;
        currentChild = 0;
        completed = false;
        lastResult = null;

        // Reset all children
        for (BTNode child : children) {
            child.reset();
        }

        LOGGER.debug("SelectorNode '{}' reset (was at child {})",
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
        sb.append("Selector[");
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) sb.append(" | ");
            if (i == currentChild) sb.append(">");
            sb.append(children.get(i).getName());
            if (i == currentChild) sb.append("<");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Adds a child node to this selector.
     *
     * <p>Children are tried in the order they are added.</p>
     *
     * @param child The child node to add
     * @throws IllegalArgumentException if child is null
     */
    public void addChild(BTNode child) {
        Objects.requireNonNull(child, "Child cannot be null");
        children.add(child);
        LOGGER.debug("SelectorNode '{}' added child: {} (total: {})",
            getName(), child.getName(), children.size());
    }

    /**
     * Inserts a child at a specific index.
     *
     * <p>Useful for prioritizing - insert at index 0 for highest priority.</p>
     *
     * @param index The index to insert at
     * @param child The child node to insert
     * @throws IllegalArgumentException if child is null
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public void insertChild(int index, BTNode child) {
        Objects.requireNonNull(child, "Child cannot be null");
        children.add(index, child);
        LOGGER.debug("SelectorNode '{}' inserted child '{}' at index {}",
            getName(), child.getName(), index);
    }

    /**
     * Removes a child from this selector.
     *
     * @param child The child to remove
     * @return true if the child was found and removed
     */
    public boolean removeChild(BTNode child) {
        boolean removed = children.remove(child);
        if (removed) {
            LOGGER.debug("SelectorNode '{}' removed child '{}'",
                getName(), child.getName());
        }
        return removed;
    }

    /**
     * Gets the number of children in this selector.
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
