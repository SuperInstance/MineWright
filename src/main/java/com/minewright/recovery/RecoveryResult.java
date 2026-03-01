package com.minewright.recovery;

/**
 * Result of a recovery strategy execution.
 *
 * <p><b>Recovery Outcomes:</b></p>
 * <ul>
 *   <li><b>SUCCESS:</b> Recovery completed successfully. Agent can resume
 *       normal operation.</li>
 *   <li><b>RETRY:</b> Recovery strategy should be attempted again.
 *       Transient condition may resolve on next attempt.</li>
 *   <li><b>ABORT:</b> Recovery failed and task should be aborted.
 *       Task cannot be completed with current resources/state.</li>
 *   <li><b>ESCALATE:</b> Recovery failed and should escalate to next strategy.
 *       Current strategy insufficient, try alternative approach.</li>
 * </ul>
 *
 * <p><b>Recovery Flow:</b></p>
 * <pre>
 * Stuck Detected → Try Strategy 1
 *                    ├── RETRY → Try Strategy 1 again
 *                    ├── ESCALATE → Try Strategy 2
 *                    ├── ABORT → Give up, notify user
 *                    └── SUCCESS → Resume execution
 * </pre>
 *
 * @since 1.1.0
 * @see RecoveryStrategy
 * @see RecoveryManager
 */
public enum RecoveryResult {

    /**
     * Recovery completed successfully.
     * Agent can resume normal operation immediately.
     */
    SUCCESS("Success", "Recovery completed, agent can resume"),

    /**
     * Recovery should be retried.
     * Transient condition may resolve on next attempt.
     * RecoveryManager may retry with same strategy after delay.
     */
    RETRY("Retry", "Recovery should be attempted again"),

    /**
     * Recovery failed permanently.
     * Task should be aborted and user notified.
     * No further recovery attempts will be made.
     */
    ABORT("Abort", "Recovery failed, task should be aborted"),

    /**
     * Recovery failed but alternative strategy may succeed.
     * RecoveryManager should try next strategy in chain.
     */
    ESCALATE("Escalate", "Recovery failed, try next strategy");

    private final String displayName;
    private final String description;

    RecoveryResult(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Returns the human-readable display name.
     *
     * @return Display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the recovery result description.
     *
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this result indicates successful recovery.
     *
     * @return true if recovery was successful
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * Checks if this result indicates recovery failure.
     *
     * @return true if recovery failed
     */
    public boolean isFailure() {
        return this == ABORT || this == ESCALATE;
    }

    /**
     * Checks if this result warrants a retry attempt.
     *
     * @return true if retry is appropriate
     */
    public boolean shouldRetry() {
        return this == RETRY;
    }

    /**
     * Checks if this result warrants escalation to next strategy.
     *
     * @return true if escalation is appropriate
     */
    public boolean shouldEscalate() {
        return this == ESCALATE;
    }

    /**
     * Checks if this result warrants task abortion.
     *
     * @return true if abortion is appropriate
     */
    public boolean shouldAbort() {
        return this == ABORT;
    }

    @Override
    public String toString() {
        return displayName + ": " + description;
    }
}
