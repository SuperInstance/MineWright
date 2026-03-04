package com.minewright.personality;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Analyzes failure types, severity levels, and contexts to determine
 * appropriate response strategies.
 *
 * <p>This class is responsible for:</p>
 * <ul>
 *   <li>Classifying failure severity into levels</li>
 *   <li>Determining if player reassurance is needed</li>
 *   <li>Analyzing failure patterns and contexts</li>
 * </ul>
 *
 * @see FailureResponseGenerator.SeverityLevel
 * @see FailureResponseGenerator.FailureType
 * @since 1.3.0
 */
public class FailureAnalyzer {

    /**
     * Determines if player reassurance is needed based on the failure context.
     *
     * <p>Reassurance is needed if:</p>
     * <ul>
     *   <li>High severity failure (>= 60)</li>
     *   <li>Repeated failures (>= 3 previous)</li>
     *   <li>Personality is high in agreeableness (worries about player feelings)</li>
     *   <li>Personality is high in neuroticism (projects own anxiety)</li>
 * </ul>
     *
     * @param context The failure context to analyze
     * @return true if player reassurance is needed, false otherwise
     */
    public static boolean needsPlayerReassurance(FailureResponseGenerator.FailureContext context) {
        return context.getSeverity() >= 60 ||
               context.getPreviousFailureCount() >= 3 ||
               context.getPersonality().getAgreeableness() >= 80 ||
               (context.getPersonality().getNeuroticism() >= 70 &&
                context.getSeverity() >= 40);
    }

    /**
     * Analyzes the failure type and personality to determine the archetype
     * that should be used for response generation.
     *
     * @param personality The personality traits to analyze
     * @return The archetype type for response generation
     */
    public static ResponseArchetype determineArchetype(PersonalityTraits personality) {
        if (personality.getConscientiousness() >= 80) {
            return ResponseArchetype.PERFECTIONIST;
        } else if (personality.getNeuroticism() >= 80) {
            return ResponseArchetype.WORRIER;
        } else if (personality.getNeuroticism() <= 20) {
            return ResponseArchetype.STOIC;
        } else if (personality.getExtraversion() >= 80) {
            return ResponseArchetype.ENTHUSIASTIC;
        } else if (personality.getExtraversion() <= 20) {
            return ResponseArchetype.INTROVERTED;
        } else if (personality.getAgreeableness() >= 80) {
            return ResponseArchetype.ACCOMMODATING;
        } else if (personality.getAgreeableness() <= 20) {
            return ResponseArchetype.DIRECT;
        } else if (personality.getOpenness() >= 80) {
            return ResponseArchetype.INNOVATOR;
        } else {
            return ResponseArchetype.BALANCED;
        }
    }

    /**
     * Gets the immediate fix for a specific failure type.
     *
     * @param type The failure type
     * @return Description of the immediate fix
     */
    public static String getImmediateFix(FailureResponseGenerator.FailureType type) {
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

    /**
     * Response archetype types for different personality patterns.
     */
    public enum ResponseArchetype {
        PERFECTIONIST,
        WORRIER,
        STOIC,
        ENTHUSIASTIC,
        INTROVERTED,
        ACCOMMODATING,
        DIRECT,
        INNOVATOR,
        BALANCED
    }

    /**
     * Selects a random option from the provided choices.
     *
     * @param options The options to choose from
     * @return A randomly selected option, or empty string if no options provided
     */
    public static String randomChoice(String... options) {
        if (options == null || options.length == 0) {
            return "";
        }
        return options[ThreadLocalRandom.current().nextInt(options.length)];
    }

    // Private constructor to prevent instantiation
    private FailureAnalyzer() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
