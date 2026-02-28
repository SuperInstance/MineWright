package com.minewright.decision;

import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;

import java.util.Map;
import java.util.TreeMap;

/**
 * Represents the utility score calculated for a task based on various factors.
 *
 * <p><b>Purpose:</b></p>
 * <p>Utility AI scores tasks from 0.0 to 1.0 based on multiple weighted factors.
 * Higher scores indicate more desirable tasks. This record captures both the
 * final score and the breakdown of how each factor contributed.</p>
 *
 * <p><b>Scoring Model:</b></p>
 * <pre>
 * finalScore = baseValue + (factor1 * weight1) + (factor2 * weight2) + ...
 * </pre>
 *
 * <p><b>Factor Values:</b></p>
 * <ul>
 *   <li>Each factor contributes a value from 0.0 to 1.0</li>
 *   <li>Base value allows for initial priority adjustments</li>
 *   <li>Final score is clamped to [0.0, 1.0] range</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This record is immutable and thread-safe. Factors are stored in an
 * immutable TreeMap for consistent ordering.</p>
 *
 * @param baseValue   The base priority value before factors are applied
 * @param factors     Map of factor names to their calculated values (0.0-1.0)
 * @param finalScore  The final weighted score after combining all factors
 * @see UtilityFactor
 * @see TaskPrioritizer
 * @since 1.0.0
 */
public record UtilityScore(
    double baseValue,
    Map<String, Double> factors,
    double finalScore
) {
    /**
     * Minimum valid utility score.
     */
    public static final double MIN_SCORE = 0.0;

    /**
     * Maximum valid utility score.
     */
    public static final double MAX_SCORE = 1.0;

    /**
     * Calculates a utility score for a task given the decision context.
     *
     * <p>This factory method applies all registered factors from the context's
     * prioritizer, combining their weighted contributions to produce a final score.</p>
     *
     * @param task    The task to score
     * @param context The decision context providing world and agent state
     * @return A new UtilityScore with the calculated values
     * @throws IllegalArgumentException if task or context is null
     */
    public static UtilityScore calculate(Task task, DecisionContext context) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Decision context cannot be null");
        }

        TaskPrioritizer prioritizer = context.getPrioritizer();
        if (prioritizer == null) {
            // Default to neutral score if no prioritizer available
            return new UtilityScore(0.5, Map.of(), 0.5);
        }

        return prioritizer.score(task, context);
    }

    /**
     * Creates a new utility score with the given values.
     *
     * <p>The factors map is copied into an immutable TreeMap for consistent
     * iteration and thread safety.</p>
     *
     * @param baseValue  The base priority value
     * @param factors    Map of factor names to values
     * @param finalScore The final calculated score
     */
    public UtilityScore {
        // Validate score ranges
        if (baseValue < MIN_SCORE || baseValue > MAX_SCORE) {
            throw new IllegalArgumentException(
                String.format("Base value must be between %.1f and %.1f, got %.2f",
                    MIN_SCORE, MAX_SCORE, baseValue));
        }
        if (finalScore < MIN_SCORE || finalScore > MAX_SCORE) {
            throw new IllegalArgumentException(
                String.format("Final score must be between %.1f and %.1f, got %.2f",
                    MIN_SCORE, MAX_SCORE, finalScore));
        }

        // Create immutable sorted map for consistent ordering
        factors = Map.copyOf(new TreeMap<>(factors));

        // Validate all factor values
        for (Map.Entry<String, Double> entry : factors.entrySet()) {
            double value = entry.getValue();
            if (value < MIN_SCORE || value > MAX_SCORE) {
                throw new IllegalArgumentException(
                    String.format("Factor '%s' value must be between %.1f and %.1f, got %.2f",
                        entry.getKey(), MIN_SCORE, MAX_SCORE, value));
            }
        }
    }

    /**
     * Returns whether this score represents a high-priority task.
     *
     * <p>Tasks with scores above 0.7 are considered high priority.</p>
     *
     * @return true if score is 0.7 or higher
     */
    public boolean isHighPriority() {
        return finalScore >= 0.7;
    }

    /**
     * Returns whether this score represents a low-priority task.
     *
     * <p>Tasks with scores below 0.3 are considered low priority.</p>
     *
     * @return true if score is 0.3 or lower
     */
    public boolean isLowPriority() {
        return finalScore <= 0.3;
    }

    /**
     * Returns the value of a specific factor, or empty if the factor
     * was not applied to this score.
     *
     * @param factorName The name of the factor to retrieve
     * @return The factor's value, or empty if not present
     */
    public java.util.Optional<Double> getFactorValue(String factorName) {
        return java.util.Optional.ofNullable(factors.get(factorName));
    }

    /**
     * Returns a human-readable description of the score breakdown.
     *
     * <p>Useful for debugging and explaining decisions to players.</p>
     *
     * @return A formatted string showing all factor contributions
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("UtilityScore[%.2f]", finalScore));

        if (!factors.isEmpty()) {
            sb.append(" {");
            boolean first = true;
            for (Map.Entry<String, Double> entry : factors.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(String.format("%s=%.2f", entry.getKey(), entry.getValue()));
                first = false;
            }
            sb.append("}");
        }

        return sb.toString();
    }

    /**
     * Returns a comparison result against another score.
     *
     * @param other The other score to compare against
     * @return A negative value if this score is lower, zero if equal,
     *         positive if this score is higher
     */
    public int compareTo(UtilityScore other) {
        return Double.compare(this.finalScore, other.finalScore);
    }
}
