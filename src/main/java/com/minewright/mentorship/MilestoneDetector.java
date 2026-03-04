package com.minewright.mentorship;

import com.minewright.mentorship.MentorshipModels.SkillMilestone;

/**
 * Detects skill milestones for worker progress.
 *
 * <p>This class analyzes worker performance to identify significant
 * achievements and improvements in their capabilities.</p>
 *
 * @since 1.5.0
 */
public class MilestoneDetector {

    /**
     * Detects what milestone the worker has achieved.
     *
     * @param worker The worker to check
     * @param context The task context
     * @return The detected milestone
     */
    public static SkillMilestone detectMilestone(WorkerProfile worker, String context) {
        int successCount = worker.getSuccessCount(context);

        if (successCount == 1) return SkillMilestone.FIRST_SUCCESS;
        if (successCount == 5) return SkillMilestone.CONSISTENT_PERFORMANCE;
        if (worker.hasImprovedSpeed(context)) return SkillMilestone.SPEED_IMPROVEMENT;
        if (worker.hasImprovedQuality(context)) return SkillMilestone.QUALITY_LEAP;
        if (worker.recentlyCompletedWithoutHelp(context)) return SkillMilestone.INDEPENDENCE;
        if (worker.usedCreativeApproach(context)) return SkillMilestone.INNOVATION;

        return SkillMilestone.FIRST_SUCCESS;  // Default
    }
}
