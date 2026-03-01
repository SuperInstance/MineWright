package com.minewright.humanization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Manages session state and fatigue modeling for AI agents.
 *
 * <p>Human play sessions follow natural patterns: warm-up periods, peak performance,
 * fatigue, breaks, and session termination. This class simulates those patterns.</p>
 *
 * <h2>Session Phases</h2>
 * <ul>
 *   <li><b>WARMUP</b> - First 5-15 minutes: slower reactions, more mistakes</li>
 *   <li><b>PERFORMANCE</b> - Main gameplay: optimal reactions, lowest mistakes</li>
 *   <li><b>FATIGUE</b> - After 2+ hours: slower reactions, increased mistakes</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * SessionManager session = new SessionManager();
 *
 * // In agent tick() method:
 * session.update();
 * SessionPhase phase = session.getCurrentPhase();
 * double reactionMultiplier = session.getReactionMultiplier();
 *
 * // Check if should take break
 * if (session.shouldTakeBreak()) {
 *     session.startBreak();
 * }
 * }</pre>
 *
 * @see HumanizationUtils
 * @see MistakeSimulator
 * @see IdleBehaviorController
 * @since 2.2.0
 */
public class SessionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionManager.class);

    /**
     * Default warm-up duration in milliseconds (10 minutes).
     */
    private static final long DEFAULT_WARMUP_DURATION_MS = 10 * 60 * 1000;

    /**
     * Default fatigue onset time in milliseconds (60 minutes).
     */
    private static final long DEFAULT_FATIGUE_ONSET_MS = 60 * 60 * 1000;

    /**
     * Minimum break interval in milliseconds (30 minutes).
     */
    private static final long MIN_BREAK_INTERVAL_MS = 30 * 60 * 1000;

    /**
     * Maximum break interval in milliseconds (forced break after 2 hours).
     */
    private static final long MAX_BREAK_INTERVAL_MS = 2 * 60 * 60 * 1000;

    /**
     * Default break duration in milliseconds (2 minutes).
     */
    private static final long DEFAULT_BREAK_DURATION_MS = 2 * 60 * 1000;

    /**
     * Break chance when minimum interval has elapsed (10% per check).
     */
    private static final double BREAK_CHANCE = 0.10;

    /**
     * Random number generator for break scheduling.
     */
    private final Random random;

    /**
     * System time when session started.
     */
    private final long sessionStartTime;

    /**
     * Time of last break taken.
     */
    private long lastBreakTime;

    /**
     * Whether agent is currently on break.
     */
    private boolean onBreak;

    /**
     * Time when current break will end.
     */
    private long breakEndTime;

    /**
     * Whether session modeling is enabled.
     */
    private boolean enabled;

    /**
     * Custom warm-up duration.
     */
    private final long warmupDurationMs;

    /**
     * Custom fatigue onset time.
     */
    private final long fatigueOnsetMs;

    /**
     * Phases of a play session.
     */
    public enum SessionPhase {
        /**
         * Warm-up phase: slower reactions, more mistakes.
         * Reaction multiplier: 1.3 (30% slower)
         * Error multiplier: 1.5 (50% more mistakes)
         */
        WARMUP,

        /**
         * Performance phase: optimal performance.
         * Reaction multiplier: 1.0 (normal)
         * Error multiplier: 1.0 (normal)
         */
        PERFORMANCE,

        /**
         * Fatigue phase: degraded performance.
         * Reaction multiplier: 1.5 (50% slower)
         * Error multiplier: 2.0 (2x mistakes)
         */
        FATIGUE
    }

    /**
     * Creates a session manager with default timing values.
     */
    public SessionManager() {
        this(DEFAULT_WARMUP_DURATION_MS, DEFAULT_FATIGUE_ONSET_MS);
    }

    /**
     * Creates a session manager with custom timing values.
     *
     * @param warmupDurationMs Duration of warm-up phase in milliseconds
     * @param fatigueOnsetMs Time when fatigue begins in milliseconds
     */
    public SessionManager(long warmupDurationMs, long fatigueOnsetMs) {
        this.sessionStartTime = System.currentTimeMillis();
        this.lastBreakTime = sessionStartTime;
        this.onBreak = false;
        this.enabled = true;
        this.random = new Random();
        this.warmupDurationMs = warmupDurationMs;
        this.fatigueOnsetMs = fatigueOnsetMs;

        LOGGER.info("SessionManager created: warmup={}ms, fatigue_onset={}ms",
            warmupDurationMs, fatigueOnsetMs);
    }

    /**
     * Enables or disables session modeling.
     *
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.debug("Session modeling {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Checks if session modeling is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Updates session state (should be called periodically).
     *
     * <p>Checks if break should end and updates fatigue calculations.</p>
     */
    public void update() {
        if (!enabled) {
            return;
        }

        // Check if break should end
        if (onBreak && System.currentTimeMillis() >= breakEndTime) {
            endBreak();
        }
    }

    /**
     * Gets the current session phase.
     *
     * @return Current session phase
     */
    public SessionPhase getCurrentPhase() {
        if (!enabled) {
            return SessionPhase.PERFORMANCE;
        }

        long elapsed = System.currentTimeMillis() - sessionStartTime;

        if (elapsed < warmupDurationMs) {
            return SessionPhase.WARMUP;
        } else if (elapsed < fatigueOnsetMs) {
            return SessionPhase.PERFORMANCE;
        } else {
            return SessionPhase.FATIGUE;
        }
    }

    /**
     * Gets reaction time multiplier for current phase.
     *
     * <p>Multipliers:</p>
     * <ul>
     *   <li>WARMUP: 1.3 (30% slower)</li>
     *   <li>PERFORMANCE: 1.0 (normal)</li>
     *   <li>FATIGUE: 1.5 (50% slower)</li>
     * </ul>
     *
     * @return Reaction time multiplier
     */
    public double getReactionMultiplier() {
        if (!enabled || onBreak) {
            return 1.0;
        }

        return switch (getCurrentPhase()) {
            case WARMUP -> 1.3;
            case PERFORMANCE -> 1.0;
            case FATIGUE -> 1.5;
        };
    }

    /**
     * Gets error rate multiplier for current phase.
     *
     * <p>Multipliers:</p>
     * <ul>
     *   <li>WARMUP: 1.5 (50% more mistakes)</li>
     *   <li>PERFORMANCE: 1.0 (normal)</li>
     *   <li>FATIGUE: 2.0 (2x mistakes)</li>
     * </ul>
     *
     * @return Error rate multiplier
     */
    public double getErrorMultiplier() {
        if (!enabled || onBreak) {
            return 1.0;
        }

        return switch (getCurrentPhase()) {
            case WARMUP -> 1.5;
            case PERFORMANCE -> 1.0;
            case FATIGUE -> 2.0;
        };
    }

    /**
     * Gets fatigue level (0.0 to 1.0).
     *
     * <p>Fatigue increases linearly from fatigue onset to maximum at 3 hours.</p>
     *
     * @return Fatigue level (0.0 = rested, 1.0 = fully fatigued)
     */
    public double getFatigueLevel() {
        if (!enabled) {
            return 0.0;
        }

        long elapsed = System.currentTimeMillis() - sessionStartTime;

        if (elapsed < fatigueOnsetMs) {
            return 0.0;
        }

        // Fatigue increases from onset to maximum at 3 hours
        long timeSinceFatigueOnset = elapsed - fatigueOnsetMs;
        long maxFatigueTime = 3 * 60 * 60 * 1000; // 3 hours

        return Math.min(1.0, (double) timeSinceFatigueOnset / maxFatigueTime);
    }

    /**
     * Checks if agent should take a break.
     *
     * <p>Break logic:</p>
     * <ul>
     *   <li>Minimum 30 minutes between breaks</li>
     *   <li>10% chance per check after minimum interval</li>
     *   <li>Forced break after 2 hours</li>
     * </ul>
     *
     * @return true if agent should take a break
     */
    public boolean shouldTakeBreak() {
        if (!enabled || onBreak) {
            return false;
        }

        long timeSinceLastBreak = System.currentTimeMillis() - lastBreakTime;

        // Forced break after max interval
        if (timeSinceLastBreak >= MAX_BREAK_INTERVAL_MS) {
            LOGGER.info("Forced break triggered ({}ms since last break)", timeSinceLastBreak);
            return true;
        }

        // Random break after min interval
        if (timeSinceLastBreak >= MIN_BREAK_INTERVAL_MS) {
            if (random.nextDouble() < BREAK_CHANCE) {
                LOGGER.info("Random break triggered ({}ms since last break)", timeSinceLastBreak);
                return true;
            }
        }

        return false;
    }

    /**
     * Starts a break with default duration.
     */
    public void startBreak() {
        startBreak(DEFAULT_BREAK_DURATION_MS);
    }

    /**
     * Starts a break with specified duration.
     *
     * @param durationMs Break duration in milliseconds
     */
    public void startBreak(long durationMs) {
        if (onBreak) {
            LOGGER.warn("Already on break, ignoring startBreak() call");
            return;
        }

        onBreak = true;
        breakEndTime = System.currentTimeMillis() + durationMs;

        LOGGER.info("Break started for {}ms (ends at {})", durationMs, breakEndTime);
    }

    /**
     * Ends the current break.
     */
    private void endBreak() {
        if (!onBreak) {
            return;
        }

        onBreak = false;
        lastBreakTime = System.currentTimeMillis();

        LOGGER.info("Break ended (last break time updated to {})", lastBreakTime);
    }

    /**
     * Checks if agent is currently on break.
     *
     * @return true if on break
     */
    public boolean isOnBreak() {
        return onBreak;
    }

    /**
     * Gets remaining break time in milliseconds.
     *
     * @return Milliseconds until break ends (0 if not on break)
     */
    public long getBreakTimeRemaining() {
        if (!onBreak) {
            return 0;
        }

        long remaining = breakEndTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Gets session elapsed time in milliseconds.
     *
     * @return Milliseconds since session started
     */
    public long getSessionElapsedTime() {
        return System.currentTimeMillis() - sessionStartTime;
    }

    /**
     * Gets time since last break in milliseconds.
     *
     * @return Milliseconds since last break ended
     */
    public long getTimeSinceLastBreak() {
        return System.currentTimeMillis() - lastBreakTime;
    }

    /**
     * Gets a summary of session state for logging/debugging.
     *
     * @return Summary string of current session state
     */
    public String getSessionSummary() {
        if (!enabled) {
            return "SessionManager[disabled]";
        }

        return String.format(
            "SessionManager[phase=%s, fatigue=%.2f, on_break=%s, elapsed=%dms, since_last_break=%dms]",
            getCurrentPhase(),
            getFatigueLevel(),
            onBreak,
            getSessionElapsedTime(),
            getTimeSinceLastBreak()
        );
    }

    /**
     * Resets the session (simulates starting a new session).
     */
    public void resetSession() {
        // Can't actually reset session start time, but we can reset last break
        // to simulate a "refreshed" state
        lastBreakTime = System.currentTimeMillis();
        onBreak = false;

        LOGGER.info("Session state reset (simulating new session)");
    }

    /**
     * Sets the seed for the random number generator.
     *
     * <p>Useful for testing to get reproducible results.</p>
     *
     * @param seed Seed value for random number generation
     */
    public void setSeed(long seed) {
        random.setSeed(seed);
        LOGGER.debug("Random seed set to: {}", seed);
    }
}
