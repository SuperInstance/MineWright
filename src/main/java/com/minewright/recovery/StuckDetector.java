package com.minewright.recovery;

import com.minewright.entity.ForemanEntity;
import com.minewright.execution.AgentStateMachine;
import com.minewright.execution.AgentState;
import com.minewright.action.ActionExecutor;
import com.minewright.action.actions.BaseAction;
import com.minewright.pathfinding.PathfindingContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Detects when an AI agent is stuck and categorizes the stuck condition.
 *
 * <h3>Detection Types:</h3>
 * <ul>
 *   <li><b>Position Stuck:</b> Agent hasn't moved from its position
 *       despite active movement attempts.</li>
 *   <li><b>Progress Stuck:</b> Agent is moving but task progress counter
 *       hasn't increased (walking in circles, wrong direction).</li>
 *   <li><b>State Stuck:</b> Agent state machine hasn't transitioned
 *       in unusually long time (logic deadlock).</li>
 *   <li><b>Path Stuck:</b> Pathfinding cannot find valid path to target.</li>
 *   <li><b>Resource Stuck:</b> Agent cannot acquire required resources.</li>
 * </ul>
 *
 * <h3>Usage Pattern:</h3>
 * <pre>{@code
 * StuckDetector detector = new StuckDetector(foreman);
 *
 * // Every tick
 * if (detector.tickAndDetect()) {
 *     StuckType type = detector.detectStuck(foreman);
 *     recoveryManager.attemptRecovery(type, foreman);
 * }
 * }</pre>
 *
 * <h3>Detection Thresholds:</h3>
 * <ul>
 *   <li>Position Stuck: 60 ticks (3 seconds) without movement</li>
 *   <li>Progress Stuck: 100 ticks (5 seconds) without progress increase</li>
 *   <li>State Stuck: 200 ticks (10 seconds) in same state</li>
 *   <li>Path Stuck: Immediate if pathfinding returns null</li>
 *   <li>Resource Stuck: Detected by action, not by detector</li>
 * </ul>
 *
 * <h3>Thread Safety:</h3>
 * <p>This class is not thread-safe. It should be used from a single
 * thread (typically the Minecraft server thread).</p>
 *
 * @since 1.1.0
 * @see StuckType
 * @see RecoveryStrategy
 * @see RecoveryManager
 */
public class StuckDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(StuckDetector.class);

    // ========== Detection Thresholds ==========

    /** Ticks without movement before considering position stuck (3 seconds). */
    private static final int POSITION_STUCK_TICKS = 60;

    /** Ticks without progress before considering progress stuck (5 seconds). */
    private static final int PROGRESS_STUCK_TICKS = 100;

    /** Ticks in same state before considering state stuck (10 seconds). */
    private static final int STATE_STUCK_TICKS = 200;

    /** Minimum distance movement to consider "not stuck" (blocks). */
    private static final double MIN_MOVEMENT_DISTANCE = 0.5;

    // ========== Tracking State ==========

    /** Entity being monitored. */
    private final ForemanEntity entity;

    /** Last recorded position. */
    private Vec3 lastPosition;

    /** Last recorded task progress. */
    private int lastProgress;

    /** Last recorded agent state. */
    private AgentState lastState;

    /** Ticks since last position change. */
    private int stuckPositionTicks = 0;

    /** Ticks since last progress increase. */
    private int stuckProgressTicks = 0;

    /** Ticks since last state transition. */
    private int stuckStateTicks = 0;

    /** Whether pathfinding is currently stuck. */
    private boolean pathStuck = false;

    /** Detection history for debugging. */
    private final Map<String, Integer> detectionHistory = new HashMap<>();

    /**
     * Creates a new stuck detector for the given entity.
     *
     * @param entity Entity to monitor
     */
    public StuckDetector(ForemanEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        this.entity = entity;
        this.lastPosition = entity.position();
        this.lastProgress = 0;
        this.lastState = AgentState.IDLE;
        reset();
    }

    // ========== Detection Methods ==========

    /**
     * Checks if entity is stuck at current position.
     *
     * @param tickThreshold Ticks without movement before stuck
     * @return true if position hasn't changed significantly
     */
    public boolean isPositionStuck(int tickThreshold) {
        return stuckPositionTicks >= tickThreshold;
    }

    /**
     * Checks if entity is stuck making progress.
     *
     * @param expectedProgress Expected progress value
     * @param actualProgress   Actual progress value
     * @param tickThreshold    Ticks without progress before stuck
     * @return true if progress hasn't increased
     */
    public boolean isProgressStuck(int expectedProgress, int actualProgress, int tickThreshold) {
        return stuckProgressTicks >= tickThreshold;
    }

    /**
     * Checks if entity state machine is stuck.
     *
     * @param state         Current state
     * @param tickThreshold Ticks in same state before stuck
     * @return true if state hasn't transitioned
     */
    public boolean isStateStuck(AgentState state, int tickThreshold) {
        return stuckStateTicks >= tickThreshold;
    }

    /**
     * Detects the type of stuck condition affecting the entity.
     *
     * <p>This method checks all stuck conditions and returns the most
     * severe one detected. Order of priority:</p>
     * <ol>
     *   <li>Path Stuck (most severe - cannot even plan movement)</li>
     *   <li>Position Stuck (physically blocked)</li>
     *   <li>Progress Stuck (moving but not advancing)</li>
     *   <li>State Stuck (logic deadlock)</li>
     *   <li>Resource Stuck (detected by action, not here)</li>
     * </ol>
     *
     * @return Detected stuck type, or null if not stuck
     */
    public StuckType detectStuck() {
        // Check path stuck first (most severe)
        if (pathStuck) {
            recordDetection("path");
            return StuckType.PATH_STUCK;
        }

        // Check position stuck
        if (isPositionStuck(POSITION_STUCK_TICKS)) {
            recordDetection("position");
            return StuckType.POSITION_STUCK;
        }

        // Check progress stuck
        if (isProgressStuck(0, entity.getActionExecutor().getCurrentActionProgress(),
                PROGRESS_STUCK_TICKS)) {
            recordDetection("progress");
            return StuckType.PROGRESS_STUCK;
        }

        // Check state stuck
        AgentState currentState = getState();
        if (isStateStuck(currentState, STATE_STUCK_TICKS)) {
            recordDetection("state");
            return StuckType.STATE_STUCK;
        }

        return null; // Not stuck
    }

    // ========== Tick Method ==========

    /**
     * Main tick method - call every game tick to update detection state.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Checks if entity position has changed</li>
     *   <li>Checks if task progress has increased</li>
     *   <li>Checks if agent state has transitioned</li>
     *   <li>Updates stuck counters accordingly</li>
     * </ol>
     *
     * @return true if any stuck condition was detected this tick
     */
    public boolean tickAndDetect() {
        Vec3 currentPosition = entity.position();
        int currentProgress = entity.getActionExecutor().getCurrentActionProgress();
        AgentState currentState = getState();

        boolean detectedStuck = false;

        // Check position movement
        double distanceMoved = lastPosition.distanceTo(currentPosition);
        if (distanceMoved < MIN_MOVEMENT_DISTANCE) {
            stuckPositionTicks++;
            if (stuckPositionTicks >= POSITION_STUCK_TICKS) {
                LOGGER.debug("[StuckDetector] {} position stuck for {} ticks at {}",
                    entity.getEntityName(), stuckPositionTicks, entity.blockPosition());
                detectedStuck = true;
            }
        } else {
            stuckPositionTicks = 0;
            lastPosition = currentPosition;
        }

        // Check progress increase
        if (currentProgress > lastProgress) {
            stuckProgressTicks = 0;
            lastProgress = currentProgress;
        } else {
            stuckProgressTicks++;
            if (stuckProgressTicks >= PROGRESS_STUCK_TICKS) {
                LOGGER.debug("[StuckDetector] {} progress stuck for {} ticks at {}%",
                    entity.getEntityName(), stuckProgressTicks, currentProgress);
                detectedStuck = true;
            }
        }

        // Check state transition
        if (currentState != lastState) {
            stuckStateTicks = 0;
            lastState = currentState;
        } else {
            stuckStateTicks++;
            if (stuckStateTicks >= STATE_STUCK_TICKS) {
                LOGGER.debug("[StuckDetector] {} state stuck in {} for {} ticks",
                    entity.getEntityName(), currentState, stuckStateTicks);
                detectedStuck = true;
            }
        }

        return detectedStuck;
    }

    // ========== State Management ==========

    /**
     * Resets all stuck detection counters.
     * Call this when agent successfully recovers or starts new task.
     */
    public void reset() {
        stuckPositionTicks = 0;
        stuckProgressTicks = 0;
        stuckStateTicks = 0;
        pathStuck = false;
        lastPosition = entity.position();
        lastProgress = 0;
        lastState = getState();
        detectionHistory.clear();
        LOGGER.debug("[StuckDetector] Reset detection for {}", entity.getEntityName());
    }

    /**
     * Marks pathfinding as stuck.
     * Call this when pathfinding returns null or invalid path.
     */
    public void markPathStuck() {
        pathStuck = true;
        recordDetection("path");
        LOGGER.warn("[StuckDetector] {} pathfinding stuck at {}",
            entity.getEntityName(), entity.blockPosition());
    }

    /**
     * Clears pathfinding stuck status.
     * Call this when a new valid path is found.
     */
    public void clearPathStuck() {
        pathStuck = false;
        LOGGER.debug("[StuckDetector] {} pathfinding cleared",
            entity.getEntityName());
    }

    // ========== Getters ==========

    /**
     * Gets the current stuck position tick count.
     *
     * @return Ticks without significant movement
     */
    public int getStuckPositionTicks() {
        return stuckPositionTicks;
    }

    /**
     * Gets the current stuck progress tick count.
     *
     * @return Ticks without progress increase
     */
    public int getStuckProgressTicks() {
        return stuckProgressTicks;
    }

    /**
     * Gets the current stuck state tick count.
     *
     * @return Ticks without state transition
     */
    public int getStuckStateTicks() {
        return stuckStateTicks;
    }

    /**
     * Checks if pathfinding is currently stuck.
     *
     * @return true if path stuck
     */
    public boolean isPathStuck() {
        return pathStuck;
    }

    /**
     * Gets detection history for debugging.
     *
     * @return Map of detection type to count
     */
    public Map<String, Integer> getDetectionHistory() {
        return new HashMap<>(detectionHistory);
    }

    // ========== Helper Methods ==========

    /**
     * Gets the current agent state from state machine.
     *
     * @return Current agent state
     */
    private AgentState getState() {
        ActionExecutor executor = entity.getActionExecutor();
        if (executor != null) {
            AgentStateMachine stateMachine = executor.getStateMachine();
            if (stateMachine != null) {
                return stateMachine.getCurrentState();
            }
        }
        return AgentState.IDLE;
    }

    /**
     * Records a detection event for history tracking.
     *
     * @param type Type of stuck detected
     */
    private void recordDetection(String type) {
        detectionHistory.put(type, detectionHistory.getOrDefault(type, 0) + 1);
    }

    /**
     * Gets a snapshot of current detection state.
     *
     * @return Detection state snapshot
     */
    public DetectionState getStateSnapshot() {
        return new DetectionState(
            stuckPositionTicks,
            stuckProgressTicks,
            stuckStateTicks,
            pathStuck,
            lastPosition,
            lastProgress,
            lastState,
            new HashMap<>(detectionHistory)
        );
    }

    /**
     * Snapshot of stuck detection state.
     */
    public record DetectionState(
        int stuckPositionTicks,
        int stuckProgressTicks,
        int stuckStateTicks,
        boolean pathStuck,
        Vec3 lastPosition,
        int lastProgress,
        AgentState lastState,
        Map<String, Integer> detectionHistory
    ) {
        @Override
        public String toString() {
            return String.format(
                "DetectionState[posStuck=%d, progStuck=%d, stateStuck=%d, pathStuck=%s, state=%s]",
                stuckPositionTicks, stuckProgressTicks, stuckStateTicks,
                pathStuck, lastState
            );
        }
    }
}
