package com.minewright.personality.response;

import com.minewright.personality.FailureResponseGenerator;
import com.minewright.personality.PersonalityTraits;

/**
 * Generates learning statements and recovery plans for failures.
 *
 * <p>This class handles the analytical side of failure responses,
 * creating plans for improvement and acknowledging lessons learned.</p>
 */
public class LearningAndRecoveryGenerator {

    /**
     * Generates a learning statement based on the failure.
     *
     * @param context The failure context
     * @return A statement about what was learned
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
     * Generates a recovery plan based on the failure context.
     *
     * @param context The failure context
     * @return A recovery plan
     */
    public static String generateRecoveryPlan(FailureResponseGenerator.FailureContext context) {
        int severity = context.getSeverity();
        PersonalityTraits p = context.getPersonality();

        if (severity <= 40) {
            if (p.getConscientiousness() >= 70) {
                return String.format(
                    "Short term: %s. Long term: Implement verification step " +
                    "to prevent recurrence.",
                    getImmediateFix(context.getFailureType())
                );
            } else {
                return String.format(
                    "I'll %s and be more careful next time.",
                    getImmediateFix(context.getFailureType())
                );
            }
        } else {
            if (p.getConscientiousness() >= 70) {
                return String.format(
                    "Phase 1: %s. Phase 2: Review and update all related " +
                    "procedures. Phase 3: Implement verification protocols. " +
                    "I'll be ready to resume normal operations shortly.",
                    getImmediateFix(context.getFailureType())
                );
            } else {
                return String.format(
                    "I'm going to %s, take some time to reflect, and come " +
                    "back with a better approach. I'll make this right.",
                    getImmediateFix(context.getFailureType())
                );
            }
        }
    }

    /**
     * Determines if the player needs reassurance after this failure.
     *
     * @param context The failure context
     * @return true if player reassurance is needed
     */
    public static boolean needsPlayerReassurance(FailureResponseGenerator.FailureContext context) {
        // Need reassurance if:
        // 1. High severity failure
        // 2. Repeated failures
        // 3. Personality is high in agreeableness (worries about player feelings)
        // 4. Personality is high in neuroticism (projects own anxiety)

        return context.getSeverity() >= 60 ||
               context.getPreviousFailureCount() >= 3 ||
               context.getPersonality().getAgreeableness() >= 80 ||
               (context.getPersonality().getNeuroticism() >= 70 &&
                context.getSeverity() >= 40);
    }

    private static String getImmediateFix(FailureResponseGenerator.FailureType type) {
        switch (type) {
            case RESOURCE_WASTE:
                return "replace the wasted resources";
            case TOOL_BREAKAGE:
                return "craft a replacement tool";
            case NAVIGATION_ERROR:
                return "return to a known location";
            case STRUCTURAL_FAILURE:
                return "clear the debris and rebuild properly";
            case ITEM_LOSS:
                return "attempt to recover or replace the items";
            case TASK_FAILURE:
                return "restart the task with better preparation";
            case COMMUNICATION_ERROR:
                return "clarify my understanding";
            case SAFETY_VIOLATION:
                return "move to a safe location";
            case REPETITIVE_MISTAKE:
                return "analyze and address the root cause";
            case EMBARRASSING_MOMENT:
                return "collect myself and continue";
            default:
                return "address the issue";
        }
    }
}
