package com.minewright.recovery;

import com.minewright.entity.ForemanEntity;
import com.minewright.recovery.strategies.AbortStrategy;
import com.minewright.recovery.strategies.RepathStrategy;
import com.minewright.recovery.strategies.TeleportStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages recovery strategy selection and execution for stuck agents.
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li><b>Strategy Selection:</b> Chooses appropriate strategy for stuck type</li>
 *   <li><b>Escalation Chain:</b) Tries strategies in order until success</li>
 *   <li><b>Attempt Tracking:</b> Monitors retry counts per strategy</li>
 *   <li><b>Result Handling:</b> Processes recovery results and determines next action</li>
 *   <li><b>Statistics:</b> Tracks recovery success/failure rates</li>
 * </ul>
 *
 * <h3>Default Escalation Chain:</h3>
 * <pre>
 * For POSITION_STUCK or PATH_STUCK:
 *   1. RepathStrategy (try new path)
 *   2. TeleportStrategy (teleport to safe location)
 *   3. AbortStrategy (give up)
 *
 * For PROGRESS_STUCK:
 *   1. RepathStrategy (recalculate path)
 *   2. AbortStrategy (give up)
 *
 * For STATE_STUCK or RESOURCE_STUCK:
 *   1. AbortStrategy (direct to abort)
 * </pre>
 *
 * <h3>Usage Pattern:</h3>
 * <pre>{@code
 * RecoveryManager manager = new RecoveryManager(foreman);
 * StuckDetector detector = new StuckDetector(foreman);
 *
 * // Every tick
 * detector.tickAndDetect();
 * StuckType type = detector.detectStuck();
 * if (type != null) {
 *     RecoveryResult result = manager.attemptRecovery(type);
 *     if (result == RecoveryResult.SUCCESS) {
 *         detector.reset();
 *     }
 * }
 * }</pre>
 *
 * <h3>Thread Safety:</h3>
 * <p>This class is not thread-safe. It should be used from a single
 * thread (typically the Minecraft server thread).</p>
 *
 * @since 1.1.0
 * @see RecoveryStrategy
 * @see StuckType
 * @see RecoveryResult
 * @see StuckDetector
 */
public class RecoveryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryManager.class);

    /** Default recovery strategies in escalation order. */
    private static final List<RecoveryStrategy> DEFAULT_STRATEGIES = List.of(
        new RepathStrategy(),
        new TeleportStrategy(),
        new AbortStrategy()
    );

    /** Entity being managed. */
    private final ForemanEntity entity;

    /** Available recovery strategies. */
    private final List<RecoveryStrategy> strategies;

    /** Attempt count per strategy. */
    private final Map<String, Integer> attemptCounts;

    /** Total recovery attempts. */
    private int totalAttempts;

    /** Total successful recoveries. */
    private int successCount;

    /** Current strategy index in escalation chain. */
    private int currentStrategyIndex;

    /** Whether recovery is currently in progress. */
    private boolean isRecovering;

    /** Stuck type currently being recovered. */
    private StuckType currentStuckType;

    /**
     * Creates a recovery manager with default strategies.
     *
     * @param entity Entity to manage recovery for
     */
    public RecoveryManager(ForemanEntity entity) {
        this(entity, DEFAULT_STRATEGIES);
    }

    /**
     * Creates a recovery manager with custom strategies.
     *
     * @param entity     Entity to manage recovery for
     * @param strategies Recovery strategies in escalation order
     */
    public RecoveryManager(ForemanEntity entity, List<RecoveryStrategy> strategies) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if (strategies == null || strategies.isEmpty()) {
            throw new IllegalArgumentException("Strategies cannot be null or empty");
        }

        this.entity = entity;
        this.strategies = new ArrayList<>(strategies);
        this.attemptCounts = new HashMap<>();
        this.totalAttempts = 0;
        this.successCount = 0;
        this.currentStrategyIndex = 0;
        this.isRecovering = false;
    }

    /**
     * Attempts to recover from the given stuck type.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Finds applicable strategies for stuck type</li>
     *   <li>Tries current strategy in escalation chain</li>
     *   <li>Handles retry, escalation, or abort based on result</li>
     *   <li>Tracks statistics for monitoring</li>
     * </ol>
     *
     * @param stuckType Type of stuck condition
     * @return Recovery result
     */
    public RecoveryResult attemptRecovery(StuckType stuckType) {
        if (stuckType == null) {
            LOGGER.warn("[RecoveryManager] Cannot recover from null stuck type");
            return RecoveryResult.ABORT;
        }

        LOGGER.info("[RecoveryManager] Attempting recovery for {} from {}",
            entity.getEntityName(), stuckType);

        // Check if already recovering
        if (isRecovering && currentStuckType == stuckType) {
            LOGGER.debug("[RecoveryManager] Already recovering from {}, checking progress",
                stuckType);
            return continueRecovery();
        }

        // Start new recovery
        isRecovering = true;
        currentStuckType = stuckType;
        currentStrategyIndex = 0;

        return attemptNextStrategy();
    }

    /**
     * Continues current recovery attempt.
     * Called when recovery is already in progress.
     *
     * @return Recovery result
     */
    private RecoveryResult continueRecovery() {
        if (currentStrategyIndex >= strategies.size()) {
            // Exhausted all strategies
            LOGGER.warn("[RecoveryManager] Exhausted all strategies for {}",
                entity.getEntityName());
            finishRecovery(false);
            return RecoveryResult.ABORT;
        }

        return attemptNextStrategy();
    }

    /**
     * Attempts the next strategy in the escalation chain.
     *
     * @return Recovery result
     */
    private RecoveryResult attemptNextStrategy() {
        // Find next applicable strategy
        while (currentStrategyIndex < strategies.size()) {
            RecoveryStrategy strategy = strategies.get(currentStrategyIndex);
            String strategyName = strategy.getName();

            if (strategy.canRecover(currentStuckType, entity)) {
                // Found applicable strategy
                int currentAttempts = attemptCounts.getOrDefault(strategyName, 0);
                int maxAttempts = strategy.getMaxAttempts();

                if (currentAttempts < maxAttempts) {
                    // Attempt recovery
                    LOGGER.info("[RecoveryManager] Trying strategy {} (attempt {}/{})",
                        strategyName, currentAttempts + 1, maxAttempts);

                    RecoveryResult result = executeStrategy(strategy);

                    // Update attempt count
                    attemptCounts.put(strategyName, currentAttempts + 1);
                    totalAttempts++;

                    // Handle result
                    return handleResult(result, strategy);
                } else {
                    // Max attempts reached, escalate
                    LOGGER.info("[RecoveryManager] Strategy {} exhausted {} attempts, escalating",
                        strategyName, maxAttempts);
                    currentStrategyIndex++;
                    // Continue to next strategy
                }
            } else {
                // Strategy not applicable, try next
                currentStrategyIndex++;
            }
        }

        // No applicable strategies found
        LOGGER.warn("[RecoveryManager] No applicable strategies for {}",
            currentStuckType);
        finishRecovery(false);
        return RecoveryResult.ABORT;
    }

    /**
     * Executes a recovery strategy.
     *
     * @param strategy Strategy to execute
     * @return Recovery result
     */
    private RecoveryResult executeStrategy(RecoveryStrategy strategy) {
        try {
            return strategy.execute(entity);
        } catch (Exception e) {
            LOGGER.error("[RecoveryManager] Error executing strategy {}",
                strategy.getName(), e);
            return RecoveryResult.ESCALATE;
        }
    }

    /**
     * Handles recovery result and determines next action.
     *
     * @param result   Result from strategy execution
     * @param strategy Strategy that was executed
     * @return Final recovery result for caller
     */
    private RecoveryResult handleResult(RecoveryResult result, RecoveryStrategy strategy) {
        switch (result) {
            case SUCCESS:
                LOGGER.info("[RecoveryManager] Recovery successful with {}",
                    strategy.getName());
                successCount++;
                finishRecovery(true);
                return RecoveryResult.SUCCESS;

            case RETRY:
                LOGGER.debug("[RecoveryManager] Strategy {} requested retry",
                    strategy.getName());
                // Stay on current strategy, will retry next tick
                return RecoveryResult.RETRY;

            case ESCALATE:
                LOGGER.info("[RecoveryManager] Strategy {} requested escalation",
                    strategy.getName());
                // Move to next strategy
                currentStrategyIndex++;
                // Try next strategy immediately
                return attemptNextStrategy();

            case ABORT:
                LOGGER.warn("[RecoveryManager] Strategy {} requested abort",
                    strategy.getName());
                finishRecovery(false);
                return RecoveryResult.ABORT;

            default:
                LOGGER.warn("[RecoveryManager] Unknown recovery result: {}", result);
                finishRecovery(false);
                return RecoveryResult.ABORT;
        }
    }

    /**
     * Finishes recovery and resets state.
     *
     * @param successful Whether recovery was successful
     */
    private void finishRecovery(boolean successful) {
        isRecovering = false;
        currentStuckType = null;
        currentStrategyIndex = 0;
        attemptCounts.clear();
    }

    /**
     * Resets recovery state.
     * Call this when agent starts a new task.
     */
    public void reset() {
        isRecovering = false;
        currentStuckType = null;
        currentStrategyIndex = 0;
        attemptCounts.clear();
        LOGGER.debug("[RecoveryManager] Reset recovery state for {}",
            entity.getEntityName());
    }

    /**
     * Checks if recovery is currently in progress.
     *
     * @return true if recovering
     */
    public boolean isRecovering() {
        return isRecovering;
    }

    /**
     * Gets the current stuck type being recovered.
     *
     * @return Current stuck type, or null if not recovering
     */
    public StuckType getCurrentStuckType() {
        return currentStuckType;
    }

    /**
     * Gets total recovery attempts.
     *
     * @return Total attempts
     */
    public int getTotalAttempts() {
        return totalAttempts;
    }

    /**
     * Gets total successful recoveries.
     *
     * @return Success count
     */
    public int getSuccessCount() {
        return successCount;
    }

    /**
     * Gets success rate (0.0 to 1.0).
     *
     * @return Success rate, or 0 if no attempts
     */
    public double getSuccessRate() {
        if (totalAttempts == 0) {
            return 0.0;
        }
        return (double) successCount / totalAttempts;
    }

    /**
     * Gets recovery statistics for monitoring.
     *
     * @return Statistics snapshot
     */
    public RecoveryStats getStats() {
        return new RecoveryStats(
            totalAttempts,
            successCount,
            getSuccessRate(),
            isRecovering,
            currentStuckType,
            new HashMap<>(attemptCounts)
        );
    }

    /**
     * Snapshot of recovery statistics.
     */
    public record RecoveryStats(
        int totalAttempts,
        int successCount,
        double successRate,
        boolean isRecovering,
        StuckType currentStuckType,
        Map<String, Integer> attemptCounts
    ) {
        @Override
        public String toString() {
            return String.format(
                "RecoveryStats[attempts=%d, successes=%d, rate=%.1f%%, recovering=%s, type=%s]",
                totalAttempts, successCount, successRate * 100,
                isRecovering, currentStuckType
            );
        }
    }
}
