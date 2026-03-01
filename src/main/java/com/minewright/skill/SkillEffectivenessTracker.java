package com.minewright.skill;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks the effectiveness of skills over time to guide usage recommendations.
 *
 * <p><b>Purpose:</b></p>
 * <p>SkillEffectivenessTracker monitors skill execution outcomes to provide
 * data-driven recommendations about when to use (or avoid) specific skills.
 * This enables continuous improvement of the skill library.</p>
 *
 * <p><b>Tracked Metrics:</b></p>
 * <ul>
 *   <li><b>Execution Count:</b> Total times skill has been used</li>
 *   <li><b>Success Count:</b> Number of successful executions</li>
 *   <li><b>Total Execution Time:</b> Cumulative time spent executing</li>
 *   <li><b>Recent Success Rate:</b> Success rate over last N executions</li>
 *   <li><b>Trend:</b> Improving, declining, or stable</li>
 * </ul>
 *
 * <p><b>Recommendations:</b></p>
 * <p>Based on tracked metrics, the tracker can recommend:</p>
 * <ul>
 *   <li><b>Use:</b> High success rate, fast execution</li>
 *   <li><b>Avoid:</b> Low success rate, slow execution</li>
 *   <li><b>Refine:</b> Declining success rate</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>All operations are thread-safe using atomic counters and concurrent maps.</p>
 *
 * @see Skill
 * @see ExecutableSkill
 * @see SkillLibrary
 * @since 1.0.0
 */
public class SkillEffectivenessTracker {
    private static final Logger LOGGER = TestLogger.getLogger(SkillEffectivenessTracker.class);

    /**
     * Singleton instance for global access.
     */
    private static volatile SkillEffectivenessTracker instance;

    /**
     * Per-skill tracking data.
     */
    private final Map<String, SkillStats> statsMap;

    /**
     * Number of recent executions to consider for trend analysis.
     */
    private static final int RECENT_WINDOW = 10;

    private SkillEffectivenessTracker() {
        this.statsMap = new ConcurrentHashMap<>();
        LOGGER.info("SkillEffectivenessTracker initialized");
    }

    /**
     * Gets the singleton SkillEffectivenessTracker instance.
     *
     * @return Global instance
     */
    public static SkillEffectivenessTracker getInstance() {
        if (instance == null) {
            synchronized (SkillEffectivenessTracker.class) {
                if (instance == null) {
                    instance = new SkillEffectivenessTracker();
                }
            }
        }
        return instance;
    }

    /**
     * Records the outcome of a skill execution.
     *
     * @param skillId       The skill that was executed
     * @param success       Whether execution was successful
     * @param executionTime Time taken in milliseconds
     */
    public void recordUse(String skillId, boolean success, long executionTime) {
        SkillStats stats = statsMap.computeIfAbsent(skillId, k -> new SkillStats());

        stats.recordExecution(success, executionTime);

        LOGGER.debug("Recorded skill execution: {} (success: {}, time: {}ms)",
            skillId, success, executionTime);
    }

    /**
     * Gets the overall effectiveness score for a skill (0.0 to 1.0).
     * Combines success rate and execution speed.
     *
     * @param skillId The skill to score
     * @return Effectiveness score, or 0.0 if no data
     */
    public double getEffectivenessScore(String skillId) {
        SkillStats stats = statsMap.get(skillId);
        if (stats == null) {
            return 0.0;
        }

        int totalExecutions = stats.getTotalExecutions();
        if (totalExecutions == 0) {
            return 0.0;
        }

        // Base score from success rate (70% weight)
        double successRate = stats.getSuccessRate();
        double score = successRate * 0.7;

        // Add speed bonus (30% weight) - faster is better
        // Normalize execution time: assume 1000ms is "fast"
        double avgTime = stats.getAverageExecutionTime();
        double speedScore = Math.max(0, 1.0 - (avgTime / 5000.0)); // 5 seconds = 0 score
        score += speedScore * 0.3;

        return Math.min(score, 1.0);
    }

    /**
     * Gets the average execution time for a skill.
     *
     * @param skillId The skill to query
     * @return Average time in milliseconds, or 0 if no data
     */
    public long getAverageExecutionTime(String skillId) {
        SkillStats stats = statsMap.get(skillId);
        return stats != null ? stats.getAverageExecutionTime() : 0;
    }

    /**
     * Gets the success rate for a skill.
     *
     * @param skillId The skill to query
     * @return Success rate (0.0 to 1.0), or 0.0 if no data
     */
    public double getSuccessRate(String skillId) {
        SkillStats stats = statsMap.get(skillId);
        return stats != null ? stats.getSuccessRate() : 0.0;
    }

    /**
     * Gets the trend direction for a skill's success rate.
     *
     * @param skillId The skill to query
     * @return Trend direction
     */
    public Trend getTrend(String skillId) {
        SkillStats stats = statsMap.get(skillId);
        return stats != null ? stats.getTrend() : Trend.UNKNOWN;
    }

    /**
     * Generates a usage recommendation for a skill.
     *
     * @param skillId The skill to recommend
     * @return Recommendation enum
     */
    public Recommendation getRecommendation(String skillId) {
        SkillStats stats = statsMap.get(skillId);
        if (stats == null) {
            return Recommendation.UNKNOWN;
        }

        int totalExecutions = stats.getTotalExecutions();
        if (totalExecutions < 3) {
            return Recommendation.UNKNOWN; // Not enough data
        }

        double successRate = stats.getSuccessRate();
        double effectiveness = getEffectivenessScore(skillId);
        Trend trend = stats.getTrend();

        // High effectiveness with stable or improving trend
        if (effectiveness >= 0.7 && (trend == Trend.STABLE || trend == Trend.IMPROVING)) {
            return Recommendation.USE;
        }

        // Low effectiveness or declining trend
        if (effectiveness < 0.4 || trend == Trend.DECLINING) {
            return Recommendation.AVOID;
        }

        // Medium effectiveness
        if (effectiveness >= 0.5) {
            return Recommendation.CONSIDER;
        }

        // Needs refinement
        return Recommendation.REFINE;
    }

    /**
     * Gets a human-readable explanation of the recommendation.
     *
     * @param skillId The skill to explain
     * @return Explanation string
     */
    public String getRecommendationExplanation(String skillId) {
        Recommendation recommendation = getRecommendation(skillId);
        SkillStats stats = statsMap.get(skillId);

        if (stats == null || recommendation == Recommendation.UNKNOWN) {
            return "Insufficient data to make recommendation";
        }

        double successRate = stats.getSuccessRate();
        double avgTime = stats.getAverageExecutionTime();
        int totalExecutions = stats.getTotalExecutions();

        return switch (recommendation) {
            case USE -> String.format(
                "Recommended: High success rate (%.1f%%) and good speed (%dms avg over %d uses)",
                successRate * 100, avgTime, totalExecutions
            );
            case AVOID -> String.format(
                "Avoid: Low success rate (%.1f%%) or poor performance (%dms avg over %d uses)",
                successRate * 100, avgTime, totalExecutions
            );
            case CONSIDER -> String.format(
                "Use with caution: Moderate success rate (%.1f%%, %dms avg over %d uses)",
                successRate * 100, avgTime, totalExecutions
            );
            case REFINE -> String.format(
                "Needs refinement: Declining performance (%.1f%% success, %dms avg over %d uses)",
                successRate * 100, avgTime, totalExecutions
            );
            default -> "Unable to generate recommendation";
        };
    }

    /**
     * Resets statistics for a skill.
     * Useful when a skill is significantly updated.
     *
     * @param skillId The skill to reset
     */
    public void resetSkill(String skillId) {
        statsMap.remove(skillId);
        LOGGER.info("Reset statistics for skill: {}", skillId);
    }

    /**
     * Clears all statistics.
     */
    public void clear() {
        statsMap.clear();
        LOGGER.info("Cleared all skill effectiveness statistics");
    }

    /**
     * Gets statistics for all skills.
     *
     * @return Map of skill ID to stats
     */
    public Map<String, SkillStats> getAllStats() {
        return Map.copyOf(statsMap);
    }

    /**
     * Per-skill tracking statistics.
     */
    public static class SkillStats {
        private final AtomicInteger totalExecutions = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final java.util.Queue<Boolean> recentResults = new java.util.LinkedList<>();

        public void recordExecution(boolean success, long executionTime) {
            totalExecutions.incrementAndGet();
            if (success) {
                successCount.incrementAndGet();
            }
            totalExecutionTime.addAndGet(executionTime);

            // Track recent results for trend analysis
            recentResults.offer(success);
            if (recentResults.size() > RECENT_WINDOW) {
                recentResults.poll();
            }
        }

        public int getTotalExecutions() {
            return totalExecutions.get();
        }

        public int getSuccessCount() {
            return successCount.get();
        }

        public double getSuccessRate() {
            int total = totalExecutions.get();
            if (total == 0) {
                return 0.0;
            }
            return (double) successCount.get() / total;
        }

        public long getAverageExecutionTime() {
            int total = totalExecutions.get();
            if (total == 0) {
                return 0;
            }
            return totalExecutionTime.get() / total;
        }

        public Trend getTrend() {
            if (recentResults.size() < RECENT_WINDOW) {
                return Trend.UNKNOWN;
            }

            // Calculate recent success rate
            long recentSuccesses = recentResults.stream().filter(Boolean::booleanValue).count();
            double recentRate = (double) recentSuccesses / recentResults.size();

            // Compare to overall rate
            double overallRate = getSuccessRate();

            if (recentRate > overallRate + 0.1) {
                return Trend.IMPROVING;
            } else if (recentRate < overallRate - 0.1) {
                return Trend.DECLINING;
            } else {
                return Trend.STABLE;
            }
        }
    }

    /**
     * Trend direction for skill effectiveness.
     */
    public enum Trend {
        IMPROVING,
        STABLE,
        DECLINING,
        UNKNOWN
    }

    /**
     * Usage recommendation for skills.
     */
    public enum Recommendation {
        /**
         * Skill should be used - high effectiveness.
         */
        USE,

        /**
         * Skill should be avoided - low effectiveness.
         */
        AVOID,

        /**
         * Use with caution - medium effectiveness.
         */
        CONSIDER,

        /**
         * Skill needs refinement - declining effectiveness.
         */
        REFINE,

        /**
         * Insufficient data for recommendation.
         */
        UNKNOWN
    }
}
