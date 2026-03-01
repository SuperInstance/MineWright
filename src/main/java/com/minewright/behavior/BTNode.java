package com.minewright.behavior;

/**
 * Interface for all behavior tree nodes.
 *
 * <p><b>Purpose:</b></p>
 * <p>Behavior tree nodes are the fundamental building blocks of behavior trees.
 * Each node represents a decision, action, or control flow structure. Nodes
 * are organized hierarchically into trees that define complex AI behaviors.</p>
 *
 * <p><b>Node Lifecycle:</b></p>
 * <ol>
 *   <li>Node is created and added to a behavior tree</li>
 *   <li>Behavior tree tick() method calls this node's tick()</li>
 *   <li>Node executes and returns a status (SUCCESS, FAILURE, RUNNING)</li>
 *   <li>If RUNNING, node will be ticked again on next frame</li>
 *   <li>If terminal (SUCCESS/FAILURE), node may be reset() for reuse</li>
 * </ol>
 *
 * <p><b>Node Types:</b></p>
 * <ul>
 *   <li><b>Composite Nodes:</b> Control flow - Sequence, Selector, Parallel</li>
 *   <li><b>Decorator Nodes:</b> Modify child result - Inverter, Repeater, Cooldown</li>
 *   <li><b>Leaf Nodes:</b> Perform actions or check conditions - Action, Condition</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Create a behavior tree for gathering wood
 * BTNode gatherWoodTree = new SequenceNode(
 *     new ConditionNode(() -> hasItem("axe")),
 *     new ActionNode(new MoveToNearestTreeAction()),
 *     new ActionNode(new MineBlockAction()),
 *     new RepeaterNode(new ActionNode(new MineBlockAction()), 9) // 10 total logs
 * );
 *
 * // Tick the tree each game tick
 * BTBlackboard blackboard = new BTBlackboard(foremanEntity);
 * NodeStatus status = gatherWoodTree.tick(blackboard);
 *
 * // Reset tree when switching tasks
 * if (status.isTerminal()) {
 *     gatherWoodTree.reset();
 * }
 * }</pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Nodes are ticked on the main game thread (single-threaded)</li>
 *   <li>Implementations should be thread-safe if accessed from multiple threads</li>
 *   <li>BTBlackboard provides thread-safe context storage</li>
 * </ul>
 *
 * <p><b>Implementation Guidelines:</b></p>
 * <ul>
 *   <li>Nodes should be stateless where possible (store state in blackboard)</li>
 *   <li>Stateful nodes must implement reset() to clear internal state</li>
 *   <li>Nodes should never block - return RUNNING for long-running operations</li>
 *   <li>Nodes should handle null blackboard gracefully</li>
 *   <li>Nodes should log important state changes for debugging</li>
 * </ul>
 *
 * @see NodeStatus
 * @see BTBlackboard
 * @see com.minewright.behavior.composite.CompositeNode
 * @see com.minewright.behavior.decorator.DecoratorNode
 * @see com.minewright.behavior.leaf.LeafNode
 * @since 1.0.0
 */
public interface BTNode {
    /**
     * Executes this node's behavior.
     *
     * <p>This method is called each game tick (or each behavior tree update).
     * The node should perform its logic and return a status indicating:
     * <ul>
     *   <li>SUCCESS - Node completed successfully</li>
     *   <li>FAILURE - Node failed</li>
     *   <li>RUNNING - Node is still executing</li>
     * </ul></p>
     *
     * <p><b>Implementation Notes:</b></p>
     * <ul>
     *   <li>Should never block - all operations must be non-blocking</li>
     *   <li>Should be idempotent - multiple ticks with same state should be safe</li>
     *   <li>Should use blackboard for storing state between ticks</li>
     *   <li>Should return RUNNING for multi-tick operations</li>
     *   <li>Should validate blackboard is not null before accessing</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * public NodeStatus tick(BTBlackboard blackboard) {
     *     if (blackboard == null) {
     *         return NodeStatus.FAILURE;
     *     }
     *
     *     ForemanEntity entity = blackboard.getEntity();
     *     if (entity == null) {
     *         return NodeStatus.FAILURE;
     *     }
     *
     *     // Check if already completed
     *     if (blackboard.getBoolean("action_complete", false)) {
     *         return NodeStatus.SUCCESS;
     *     }
     *
     *     // Perform action
     *     boolean success = performAction(entity);
     *     if (success) {
     *         blackboard.put("action_complete", true);
     *         return NodeStatus.SUCCESS;
     *     }
     *
     *     // Still working
     *     return NodeStatus.RUNNING;
     * }
     * }</pre>
     *
     * @param blackboard The shared context data for this behavior tree execution
     * @return The status of this node after execution
     * @throws NullPointerException if blackboard is null (implementation dependent)
     */
    NodeStatus tick(BTBlackboard blackboard);

    /**
     * Resets this node to its initial state.
     *
     * <p>This method is called when a behavior tree is reused or when
     * execution should restart from the beginning. Nodes should clear
     * any internal state and reset child nodes recursively.</p>
     *
     * <p><b>Implementation Notes:</b></p>
     * <ul>
     *   <li>Should reset all internal state variables</li>
     *   <li>Should call reset() on all child nodes (for composites/decorators)</li>
     *   <li>Should clear any state stored in blackboard by this node</li>
     *   <li>Should be idempotent - safe to call multiple times</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * public void reset() {
     *     this.currentIndex = 0;
     *     this.completed = false;
     *     for (BTNode child : children) {
     *         child.reset();
     *     }
     * }
     * }</pre>
     */
    void reset();

    /**
     * Checks if this node has completed execution.
     *
     * <p>A node is complete if it has returned a terminal status (SUCCESS or FAILURE)
     * and will not return RUNNING unless reset. This is a useful optimization
     * to avoid ticking completed nodes.</p>
     *
     * <p><b>Default Implementation:</b></p>
     * <p>Returns false by default. Stateful nodes should override this method
     * to track completion state.</p>
     *
     * @return true if this node has completed and will not return RUNNING
     */
    default boolean isComplete() {
        return false;
    }

    /**
     * Gets the name of this node for debugging and logging.
     *
     * <p><b>Default Implementation:</b></p>
     * <p>Returns the simple class name by default. Nodes should override
     * to provide more descriptive names.</p>
     *
     * @return The name of this node
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Gets the default name for a node (simple class name).
     * Provided for implementations to call when they have a custom name field.
     *
     * @return The simple class name
     */
    default String getDefaultName() {
        return getClass().getSimpleName();
    }

    /**
     * Gets a description of this node for debugging purposes.
     *
     * <p><b>Default Implementation:</b></p>
     * <p>Returns the node name by default. Nodes should override to provide
     * additional context (e.g., "Sequence: [Move, Mine, Return]").</p>
     *
     * @return A description of this node
     */
    default String getDescription() {
        return getName();
    }
}
