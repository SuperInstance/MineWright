package com.minewright.behavior;

/**
 * Return status for behavior tree node execution.
 *
 * <p><b>Purpose:</b></p>
 * <p>Behavior tree nodes return one of three statuses to indicate their execution result.
 * These statuses drive the execution flow through composite and decorator nodes.</p>
 *
 * <p><b>Status Values:</b></p>
 * <ul>
 *   <li><b>SUCCESS:</b> The node completed successfully. Sequence nodes continue to the next child,
 *       selector nodes return success to parent, parallel nodes may continue based on policy.</li>
 *   <li><b>FAILURE:</b> The node failed. Sequence nodes return failure to parent (fail-fast),
 *       selector nodes try the next child, parallel nodes may continue based on policy.</li>
 *   <li><b>RUNNING:</b> The node is still executing and has not yet completed.
 *       The behavior tree will tick this node again on the next frame. Parent nodes
 *       return RUNNING to indicate they are waiting for child completion.</li>
 * </ul>
 *
 * <p><b>Flow Control Examples:</b></p>
 * <pre>{@code
 * // Sequence Node: Execute children in order, fail fast
 * SequenceNode sequence = new SequenceNode(
 *     new MoveToTargetAction(),
 *     new MineBlockAction(),
 *     new ReturnToBaseAction()
 * );
 * // If MineBlockAction returns FAILURE, sequence returns FAILURE immediately
 * // If MoveToTargetAction returns RUNNING, sequence returns RUNNING
 *
 * // Selector Node: Try children until one succeeds
 * SelectorNode selector = new SelectorNode(
 *     new AttackAction(),
 *     new FleeAction(),
 *     new HideAction()
 * );
 * // If AttackAction returns FAILURE, selector tries FleeAction
 * // If FleeAction returns SUCCESS, selector returns SUCCESS immediately
 * }</pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This enum is immutable and thread-safe.</p>
 *
 * @see BTNode
 * @see com.minewright.behavior.composite.SequenceNode
 * @see com.minewright.behavior.composite.SelectorNode
 * @since 1.0.0
 */
public enum NodeStatus {
    /**
     * The node completed successfully.
     *
     * <p>In a SequenceNode: Continue to next child (or return SUCCESS if all done).</p>
     * <p>In a SelectorNode: Return SUCCESS immediately.</p>
     * <p>In a ParallelNode: Depends on policy (e.g., SUCCESS_IF_ALL_SUCCEED).</p>
     */
    SUCCESS,

    /**
     * The node failed to complete.
     *
     * <p>In a SequenceNode: Return FAILURE immediately (fail-fast).</p>
     * <p>In a SelectorNode: Try next child (or return FAILURE if none left).</p>
     * <p>In a ParallelNode: Depends on policy (e.g., FAILURE_IF_ANY_FAILS).</p>
     */
    FAILURE,

    /**
     * The node is still executing.
     *
     * <p>The behavior tree will tick this node again on the next frame.
     * Parent nodes return RUNNING to indicate they are waiting for completion.</p>
     *
     * <p>In all composite nodes: Return RUNNING to parent immediately.</p>
     */
    RUNNING;

    /**
     * Checks if this status is a terminal state (SUCCESS or FAILURE).
     *
     * <p>Terminal states indicate that the node has completed execution
     * and will not be ticked again until reset.</p>
     *
     * @return true if this status is SUCCESS or FAILURE
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAILURE;
    }

    /**
     * Checks if this status is a success state.
     *
     * @return true if this status is SUCCESS
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * Checks if this status is a failure state.
     *
     * @return true if this status is FAILURE
     */
    public boolean isFailure() {
        return this == FAILURE;
    }

    /**
     * Checks if this status is a running state.
     *
     * @return true if this status is RUNNING
     */
    public boolean isRunning() {
        return this == RUNNING;
    }

    /**
     * Returns the logical NOT of this status for conditional logic.
     *
     * <p>Useful for inverter nodes and conditional checks.</p>
     *
     * <ul>
     *   <li>SUCCESS &rarr; FAILURE</li>
     *   <li>FAILURE &rarr; SUCCESS</li>
     *   <li>RUNNING &rarr; RUNNING</li>
     * </ul>
     *
     * @return The inverted status
     */
    public NodeStatus invert() {
        return switch (this) {
            case SUCCESS -> FAILURE;
            case FAILURE -> SUCCESS;
            case RUNNING -> RUNNING;
        };
    }

    /**
     * Combines this status with another according to sequence logic.
     *
     * <p>Used internally by SequenceNode to combine child results:</p>
     * <ul>
     *   <li>If this is RUNNING, return RUNNING (still executing current child)</li>
     *   <li>If other is FAILURE, return FAILURE (fail-fast)</li>
     *   <li>If other is RUNNING, return RUNNING (next child is running)</li>
     *   <li>Otherwise (both SUCCESS), return SUCCESS</li>
     * </ul>
     *
     * @param other The other status to combine with
     * @return The combined status according to sequence logic
     */
    public NodeStatus combineSequence(NodeStatus other) {
        if (this == RUNNING || other == RUNNING) {
            return RUNNING;
        }
        if (other == FAILURE) {
            return FAILURE;
        }
        return SUCCESS;
    }

    /**
     * Combines this status with another according to selector logic.
     *
     * <p>Used internally by SelectorNode to combine child results:</p>
     * <ul>
     *   <li>If this is SUCCESS or other is SUCCESS, return SUCCESS (found success)</li>
     *   <li>If this is RUNNING or other is RUNNING, return RUNNING</li>
     *   <li>Otherwise (both FAILURE), return FAILURE</li>
     * </ul>
     *
     * @param other The other status to combine with
     * @return The combined status according to selector logic
     */
    public NodeStatus combineSelector(NodeStatus other) {
        if (this == SUCCESS || other == SUCCESS) {
            return SUCCESS;
        }
        if (this == RUNNING || other == RUNNING) {
            return RUNNING;
        }
        return FAILURE;
    }
}
