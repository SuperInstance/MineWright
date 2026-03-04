package com.minewright.mentorship;

import com.minewright.mentorship.MentorshipModels.TaskError;

/**
 * Analyzes worker errors to determine type and solution.
 *
 * <p>This class examines error context to categorize mistakes and
 * suggest appropriate corrections.</p>
 *
 * @since 1.5.0
 */
public class ErrorAnalyzer {

    /**
     * Analyzes error to determine type and solution.
     *
     * @param worker The worker who made the error
     * @param context The error context
     * @return TaskError with type and suggested solution
     */
    public static TaskError analyzeError(WorkerProfile worker, String context) {
        String lower = context.toLowerCase();

        // Analyze error type based on context
        if (lower.contains("block") || lower.contains("material")) {
            return new TaskError(TaskError.ErrorType.WRONG_MATERIAL,
                "that material", "a more suitable one");
        }

        if (lower.contains("missing") || lower.contains("forgot")) {
            return new TaskError(TaskError.ErrorType.MISSING_STEP,
                "a step", "the complete process");
        }

        if (lower.contains("dangerous") || lower.contains("unsafe") ||
            lower.contains("lava") || lower.contains("fall")) {
            return new TaskError(TaskError.ErrorType.SAFETY_ISSUE,
                "a safety issue", "a safer approach");
        }

        if (lower.contains("slow") || lower.contains("inefficient")) {
            return new TaskError(TaskError.ErrorType.EFFICIENCY_ISSUE,
                "a slower method", "a more efficient technique");
        }

        // Default
        return new TaskError(TaskError.ErrorType.EFFICIENCY_ISSUE,
            "something", "a better way");
    }
}
