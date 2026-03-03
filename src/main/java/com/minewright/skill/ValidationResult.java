package com.minewright.skill;

/**
 * Result of a skill validation by the CriticAgent.
 *
 * <p><b>Purpose:</b></p>
 * <p>Contains the validation outcome including success status, reason for failure,
 * and timing information.</p>
 *
 * @see CriticAgent
 * @see ValidationContext
 * @since 1.0.0
 */
public class ValidationResult {
    private final boolean valid;
    private final String reason;
    private final long validationTimeMs;

    /**
     * Creates a new ValidationResult.
     *
     * @param valid Whether the validation passed
     * @param reason Reason for failure (null if valid)
     * @param validationTimeMs Time taken for validation in milliseconds
     */
    public ValidationResult(boolean valid, String reason, long validationTimeMs) {
        this.valid = valid;
        this.reason = reason;
        this.validationTimeMs = validationTimeMs;
    }

    /**
     * Checks if the validation passed.
     *
     * @return true if the skill execution was valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Gets the reason for validation failure.
     *
     * @return Failure reason, or null if valid
     */
    public String getReason() {
        return reason;
    }

    /**
     * Gets the time taken for validation.
     *
     * @return Validation time in milliseconds
     */
    public long getValidationTimeMs() {
        return validationTimeMs;
    }

    @Override
    public String toString() {
        if (valid) {
            return "ValidationResult[valid=true, time=" + validationTimeMs + "ms]";
        } else {
            return "ValidationResult[valid=false, reason=" + reason + ", time=" + validationTimeMs + "ms]";
        }
    }
}
