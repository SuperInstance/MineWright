package com.minewright.mentorship;

import com.minewright.mentorship.MentorshipModels.SkillLevel;

/**
 * Estimates task difficulty based on context.
 *
 * <p>This class analyzes task context to determine how difficult
 * a task is, allowing for appropriate teaching and scaffolding.</p>
 *
 * @since 1.5.0
 */
public class TaskDifficultyEstimator {

    /**
     * Estimates task difficulty based on context.
     *
     * @param context The task context description
     * @return Estimated skill level required
     */
    public static SkillLevel estimateDifficulty(String context) {
        String lower = context.toLowerCase();

        // Complex tasks
        if (lower.contains("complex") || lower.contains("redstone") ||
            lower.contains("circuit") || lower.contains("mechanism") ||
            lower.contains("automation")) {
            return SkillLevel.PROFICIENT;
        }

        // Intermediate tasks
        if (lower.contains("build") || lower.contains("create") ||
            lower.contains("farm") || lower.contains("structure")) {
            return SkillLevel.COMPETENT;
        }

        // Basic tasks
        return SkillLevel.BEGINNER;
    }
}
