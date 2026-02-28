package com.minewright.decision;

import com.minewright.action.Task;

/**
 * A functional interface for calculating utility factor values for tasks.
 *
 * <p><b>Purpose:</b></p>
 * <p>Utility factors are individual scoring components that evaluate a specific
 * aspect of a task's desirability. Each factor returns a value from 0.0 to 1.0,
 * where higher values indicate more favorable conditions.</p>
 *
 * <p><b>Factor Examples:</b></p>
 * <ul>
 *   <li><b>Urgency:</b> How time-sensitive is this task?</li>
 *   <li><b>Safety:</b> How safe is it to perform this task now?</li>
 *   <li><b>Efficiency:</b> How efficiently can this task be completed?</li>
 *   <li><b>Proximity:</b> How close are the required resources?</li>
 * </ul>
 *
 * <p><b>Scoring Range:</b></p>
 * <ul>
 *   <li>0.0 = Very unfavorable condition</li>
 *   <li>0.5 = Neutral condition</li>
 *   <li>1.0 = Very favorable condition</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>Implementations must be thread-safe as factors may be called from
 * multiple threads when scoring tasks concurrently.</p>
 *
 * <p><b>Implementation Example:</b></p>
 * <pre>{@code
 * public class UrgencyFactor implements UtilityFactor {
 *     &#64;Override
 *     public double calculate(Task task, DecisionContext context) {
 *         // Tasks with deadlines get higher urgency
 *         long deadline = task.getLongParameter("deadline", 0);
 *         if (deadline == 0) return 0.5; // Neutral
 *
 *         long remaining = deadline - context.getGameTime();
 *         if (remaining < 100) return 1.0;  // Very urgent
 *         if (remaining < 1000) return 0.7; // Moderately urgent
 *         return 0.3; // Not urgent
 *     }
 *
 *     &#64;Override
 *     public String getName() {
 *         return "urgency";
 *     }
 * }
 * }</pre>
 *
 * @see UtilityScore
 * @see TaskPrioritizer
 * @see UtilityFactors
 * @since 1.0.0
 */
public interface UtilityFactor {

    /**
     * Calculates this factor's contribution to a task's utility score.
     *
     * <p>The returned value must be between 0.0 and 1.0. Values outside this
     * range will be clamped by the scoring system.</p>
     *
     * <p><b>Guidelines:</b></p>
     * <ul>
     *   <li>Return 0.0 for strongly unfavorable conditions</li>
     *   <li>Return 0.5 for neutral or unknown conditions</li>
     *   <li>Return 1.0 for strongly favorable conditions</li>
     *   <li>Use smooth gradients for nuanced scoring</li>
     *   <li>Avoid binary 0.0/1.0 unless the condition is truly binary</li>
     * </ul>
     *
     * @param task    The task being evaluated
     * @param context The decision context providing world and agent state
     * @return A value from 0.0 to 1.0 representing this factor's contribution
     * @throws NullPointerException if task or context is null (implementations
     *                              may choose to handle this gracefully)
     */
    double calculate(Task task, DecisionContext context);

    /**
     * Returns the name of this factor for identification and logging.
     *
     * <p>Factor names should be lowercase and use underscores for multi-word
     * names (e.g., "resource_proximity", "safety_factor"). This ensures
     * consistent formatting in score breakdowns and explanations.</p>
     *
     * <p>The name is used when:</p>
     * <ul>
     *   <li>Building factor maps in UtilityScore</li>
     *   <li>Generating decision explanations</li>
     *   <li>Logging and debugging output</li>
     *   <li>Configuring factor weights</li>
     * </ul>
     *
     * @return The factor name (e.g., "urgency", "safety", "efficiency")
     */
    String getName();

    /**
     * Returns the default weight for this factor when not explicitly configured.
     *
     * <p>Default weights allow factors to be self-documenting about their
     * relative importance. The standard range is 0.0 to 2.0:</p>
     * <ul>
     *   <li>0.0 = Factor is disabled by default</li>
     *   <li>0.5 = Low importance factor</li>
     *   <li>1.0 = Standard importance (default)</li>
     *   <li>1.5 = High importance factor</li>
     *   <li>2.0 = Critical factor</li>
     * </ul>
     *
     * <p>This default method can be overridden by factors that need different
     * default weights.</p>
     *
     * @return The default weight (default is 1.0)
     */
    default double getDefaultWeight() {
        return 1.0;
    }

    /**
     * Returns whether this factor should be applied to the given task.
     *
     * <p>Some factors only apply to specific task types. For example, a
     * "tool_readiness" factor only applies to tasks that require tools.
     * This method allows factors to opt-out of irrelevant tasks.</p>
     *
     * <p>The default implementation returns true for all tasks. Override this
     * to provide task-specific filtering logic.</p>
     *
     * @param task The task to check
     * @return true if this factor should be applied to the task
     */
    default boolean appliesTo(Task task) {
        return true;
    }

    /**
     * Returns a human-readable description of what this factor evaluates.
     *
     * <p>Descriptions are used in decision explanations and debugging output.
     * They should clearly explain what the factor measures and how it affects
     * the final score.</p>
     *
     * <p>Example descriptions:</p>
     * <ul>
     *   <li>"Evaluates time pressure and deadlines"</li>
     *   <li>"Measures safety based on nearby threats"</li>
     *   <li>"Assesses tool availability for the task"</li>
     * </ul>
     *
     * @return A description of this factor, or empty if not provided
     */
    default java.util.Optional<String> getDescription() {
        return java.util.Optional.empty();
    }
}
