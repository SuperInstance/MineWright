package com.minewright.recovery;

import com.minewright.entity.ForemanEntity;

/**
 * Strategy for recovering an agent from a stuck condition.
 *
 * <p><b>Recovery Strategy Pattern:</b></p>
 * <p>Each strategy implements a specific approach to getting an agent
 * unstuck. The RecoveryManager selects the appropriate strategy based
 * on the stuck type and escalation chain.</p>
 *
 * <p><b>Strategy Lifecycle:</b></p>
 * <ol>
 *   <li>RecoveryManager calls {@code canRecover()} to check applicability</li>
 *   <li>If applicable, calls {@code execute()} to perform recovery</li>
 *   <li>RecoveryResult indicates success, retry, abort, or escalation</li>
 *   <li>If ESCALATE, RecoveryManager tries next strategy</li>
 *   <li>If RETRY, RecoveryManager may retry same strategy</li>
 * </ol>
 *
 * <p><b>Thread Safety:</b> Implementations must be thread-safe as
 * recovery may be called from the main game tick thread.</p>
 *
 * <p><b>Best Practices:</b></p>
 * <ul>
 *   <li>Keep recovery fast - don't block the game thread</li>
 *   <li>Log all recovery attempts for debugging</li>
 *   <li>Clean up resources in execute() even if recovery fails</li>
 *   <li>Notify user via chat for significant recovery events</li>
 *   <li>Return appropriate RecoveryResult to guide next steps</li>
 * </ul>
 *
 * <h3>Example Implementation:</h3>
 * <pre>{@code
 * public class ExampleRecoveryStrategy implements RecoveryStrategy {
 *     @Override
 *     public boolean canRecover(StuckType type, ForemanEntity entity) {
 *         return type == StuckType.POSITION_STUCK;
 *     }
 *
 *     @Override
 *     public RecoveryResult execute(ForemanEntity entity) {
 *         try {
 *             // Attempt recovery
 *             if (recoverySuccessful) {
 *                 entity.sendChatMessage("Got unstuck!");
 *                 return RecoveryResult.SUCCESS;
 *             }
 *             return RecoveryResult.ESCALATE;
 *         } catch (Exception e) {
 *             LOGGER.error("Recovery failed", e);
 *             return RecoveryResult.ABORT;
 *         }
 *     }
 *
 *     @Override
 *     public int getMaxAttempts() {
 *         return 3;
 *     }
 * }
 * }</pre>
 *
 * @since 1.1.0
 * @see StuckType
 * @see RecoveryResult
 * @see RecoveryManager
 */
public interface RecoveryStrategy {

    /**
     * Checks if this strategy can recover from the given stuck type.
     *
     * <p>Called by RecoveryManager to determine if this strategy
     * is applicable to the current situation.</p>
     *
     * @param type   The type of stuck condition
     * @param entity The stuck agent
     * @return true if this strategy can attempt recovery
     */
    boolean canRecover(StuckType type, ForemanEntity entity);

    /**
     * Executes the recovery strategy.
     *
     * <p>Performs the actual recovery actions to get the agent unstuck.
     * This method should be fast and non-blocking.</p>
     *
     * <p><b>Post-conditions:</b></p>
     * <ul>
     *   <li>If SUCCESS: Agent should be in a state to resume execution</li>
     *   <li>If RETRY: Agent state unchanged, recovery will be retried</li>
     *   <li>If ABORT: Agent task stopped, user notified</li>
     *   <li>If ESCALATE: Agent state unchanged, next strategy will be tried</li>
     * </ul>
     *
     * @param entity The stuck agent
     * @return Recovery result indicating outcome
     */
    RecoveryResult execute(ForemanEntity entity);

    /**
     * Returns the maximum number of recovery attempts.
     *
     * <p>RecoveryManager will retry this strategy up to this many times
     * before escalating to the next strategy.</p>
     *
     * @return Maximum attempt count (0 = no retry, 1 = single attempt)
     */
    int getMaxAttempts();

    /**
     * Returns the strategy name for logging.
     *
     * @return Strategy name
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
