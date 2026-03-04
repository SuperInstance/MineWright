package com.minewright.personality;

/**
 * Generates learning statements and recovery plans for failure responses.
 *
 * <p>This class is responsible for:</p>
 * <ul>
 *   <li>Creating personality-appropriate learning statements</li>
 *   <li>Generating recovery plans based on severity and personality</li>
 *   <li>Providing structured approaches to addressing failures</li>
 * </ul>
 *
 * @see FailureResponseGenerator
 * @since 1.3.0
 */
public class LearningAndRecoveryGenerator {

    /**
     * Generates a learning statement based on the failure context and personality.
     *
     * <p>Learning statements reflect how the character processes and internalizes
     * lessons from failures, with variations based on personality traits:</p>
     * <ul>
     *   <li>High conscientiousness: Procedural improvements</li>
     *   <li>High openness: Conceptual insights</li>
     *   <li>High neuroticism: Emotional reactions and promises</li>
     *   <li>Balanced: General learning acknowledgment</li>
     * </ul>
     *
     * @param context The failure context including type, severity, and personality
     * @return A learning statement appropriate to the personality
     */
    public static String generateLearningStatement(FailureResponseGenerator.FailureContext context) {
        PersonalityTraits p = context.getPersonality();
        FailureResponseGenerator.FailureType type = context.getFailureType();

        if (p.getConscientiousness() >= 70) {
            return String.format(
                "I've updated my procedures to prevent future %s failures. " +
                "The key lesson was to [specific protocol].",
                type.getDescription().toLowerCase()
            );
        } else if (p.getOpenness() >= 70) {
            return String.format(
                "This failure has taught me valuable lessons about %s. " +
                "I'm incorporating these insights into my mental models.",
                type.getDescription().toLowerCase()
            );
        } else if (p.getNeuroticism() >= 70) {
            return String.format(
                "I've learned that I need to be more careful with %s. " +
                "I'll try harder next time, I promise.",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "I've learned from this %s failure and will apply those " +
                "lessons going forward.",
                type.getDescription().toLowerCase()
            );
        }
    }

    /**
     * Generates a recovery plan based on the failure context and personality.
     *
     * <p>Recovery plans provide structured approaches to addressing failures,
     * with complexity varying based on severity and personality:</p>
     * <ul>
     *   <li>Low severity (≤40): Simple fixes with basic improvements</li>
     *   <li>High severity (>40): Multi-phase recovery with comprehensive measures</li>
     *   <li>High conscientiousness: Detailed, structured approaches</li>
     *   <li>Other personalities: Simpler, more general plans</li>
     * </ul>
     *
     * @param context The failure context including severity and personality
     * @return A recovery plan appropriate to the severity and personality
     */
    public static String generateRecoveryPlan(FailureResponseGenerator.FailureContext context) {
        int severity = context.getSeverity();
        PersonalityTraits p = context.getPersonality();

        if (severity <= 40) {
            if (p.getConscientiousness() >= 70) {
                return String.format(
                    "Short term: %s. Long term: Implement verification step " +
                    "to prevent recurrence.",
                    FailureAnalyzer.getImmediateFix(context.getFailureType())
                );
            } else {
                return String.format(
                    "I'll %s and be more careful next time.",
                    FailureAnalyzer.getImmediateFix(context.getFailureType())
                );
            }
        } else {
            if (p.getConscientiousness() >= 70) {
                return String.format(
                    "Phase 1: %s. Phase 2: Review and update all related " +
                    "procedures. Phase 3: Implement verification protocols. " +
                    "I'll be ready to resume normal operations shortly.",
                    FailureAnalyzer.getImmediateFix(context.getFailureType())
                );
            } else {
                return String.format(
                    "I'm going to %s, take some time to reflect, and come " +
                    "back with a better approach. I'll make this right.",
                    FailureAnalyzer.getImmediateFix(context.getFailureType())
                );
            }
        }
    }

    // Private constructor to prevent instantiation
    private LearningAndRecoveryGenerator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
