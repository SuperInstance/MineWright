package com.minewright.mentorship;

import java.time.Instant;

/**
 * Data models and enums for the mentorship system.
 *
 * <p>This class contains all the data structures used throughout
 * the mentorship system, including enums for skill levels, teaching
 * types, triggers, and milestones.</p>
 *
 * @since 1.5.0
 */
public class MentorshipModels {

    /**
     * Represents a detected teaching moment with appropriate dialogue.
     */
    public static class TeachingMoment {
        private final String context;
        private final String dialogue;
        private final TeachingType type;

        private TeachingMoment(String context, String dialogue, TeachingType type) {
            this.context = context;
            this.dialogue = dialogue;
            this.type = type;
        }

        public static TeachingMoment handsOn(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.HANDS_ON);
        }

        public static TeachingMoment hint(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.HINT);
        }

        public static TeachingMoment correction(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.CORRECTION);
        }

        public static TeachingMoment gentleGuidance(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.GENTLE_GUIDANCE);
        }

        public static TeachingMoment suggestion(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.SUGGESTION);
        }

        public static TeachingMoment socratic(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.SOCRATIC);
        }

        public static TeachingMoment collaborative(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.COLLABORATIVE);
        }

        public static TeachingMoment celebration(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.CELEBRATION);
        }

        public static TeachingMoment insight(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.INSIGHT);
        }

        public String getContext() { return context; }
        public String getDialogue() { return dialogue; }
        public TeachingType getType() { return type; }

        /**
         * Types of teaching interactions.
         */
        public enum TeachingType {
            HANDS_ON,        // Demonstration
            HINT,            // Guided hint
            CORRECTION,      // Direct correction for repeated mistakes
            GENTLE_GUIDANCE, // Gentle correction for first-time mistakes
            SUGGESTION,      // Improvement suggestion
            SOCRATIC,        // Questioning approach
            COLLABORATIVE,   // Working together
            CELEBRATION,     // Milestone celebration
            INSIGHT          // Pattern recognition
        }
    }

    /**
     * Skill levels for workers.
     * Based on Dreyfus model of skill acquisition.
     */
    public enum SkillLevel {
        NOVICE,      // No experience, needs rigid rules
        BEGINNER,    // Basic awareness, can do with help
        APPRENTICE,  // Can perform with some guidance
        COMPETENT,   // Independent, efficient
        PROFICIENT,  // Highly skilled, can handle complexity
        EXPERT       // Masterful, innovative
    }

    /**
     * Depth of explanation for teaching.
     * Adjusts based on worker skill and task difficulty.
     */
    public enum ExplanationDepth {
        HANDS_ON,      // Full demonstration
        DETAILED,      // Complete explanation
        SCAFFOLDED,    // Structured step-by-step support
        HINTS,         // Gentle guidance
        CONFIRMATION,  // Acknowledgment of capability
        MINIMAL        // No guidance needed
    }

    /**
     * Triggers for teaching moments.
     */
    public enum TeachingMomentTrigger {
        WORKER_STUCK,               // Worker cannot proceed
        WORKER_MISTAKE,             // Worker made an error
        WORKER_SUCCESS_SUBOPTIMAL,  // Worker succeeded inefficiently
        WORKER_QUESTION,            // Worker asked something
        NEW_CHALLENGE,              // Novel situation
        SKILL_MILESTONE,            // Worker improved
        PATTERN_RECOGNITION         // Foreman noticed pattern
    }

    /**
     * Skill achievement milestones.
     */
    public enum SkillMilestone {
        FIRST_SUCCESS,           // First time completing task
        CONSISTENT_PERFORMANCE,  // Multiple successes
        SPEED_IMPROVEMENT,       // Faster completion
        QUALITY_LEAP,            // Better quality
        INDEPENDENCE,            // Completed without help
        INNOVATION,              // Creative solution
        TEACHING_OTHER           // Helping others learn
    }

    /**
     * Error analysis for generating corrections.
     */
    public static class TaskError {
        private final ErrorType type;
        private final String detail;
        private final String suggestedAlternative;

        public TaskError(ErrorType type, String detail, String suggestedAlternative) {
            this.type = type;
            this.detail = detail;
            this.suggestedAlternative = suggestedAlternative;
        }

        public ErrorType getType() { return type; }
        public String getDetail() { return detail; }
        public String getSuggestedAlternative() { return suggestedAlternative; }

        public enum ErrorType {
            WRONG_MATERIAL,
            MISSING_STEP,
            SAFETY_ISSUE,
            EFFICIENCY_ISSUE,
            STRUCTURAL_ISSUE
        }
    }

    /**
     * Task completion details for generating praise.
     */
    public static class TaskCompletion {
        private final String specificAction;
        private final String outcome;
        private final String trait;

        public TaskCompletion(String specificAction, String outcome, String trait) {
            this.specificAction = specificAction;
            this.outcome = outcome;
            this.trait = trait;
        }

        public String getSpecificAction() { return specificAction; }
        public String getOutcome() { return outcome; }
        public String getDemonstratedTrait() { return trait; }
    }
}
