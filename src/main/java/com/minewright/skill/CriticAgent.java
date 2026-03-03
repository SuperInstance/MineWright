package com.minewright.skill;

import com.minewright.testutil.TestLogger;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CriticAgent validates skill execution success and generates feedback for refinement.
 *
 * <p><b>Voyager Pattern Integration:</b></p>
 * <p>This CriticAgent implements the core validation step from the Voyager skill learning pattern,
 * where skills are iteratively refined based on critic feedback before being
 * stored in the skill library.</p>
 *
 * <p><b>Validation Process:</b></p>
 * <ol>
 *   <li>Execute skill and observe results</li>
 *   <li>Check for task completed</li>
 *   <li>Validate execution success</li>
 *   <li>If failed, analyze what went wrong</li>
 *   <li>Generate refinement feedback</li>
 *   <li>Store successful skills in library</li>
 * </ol>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>Thread-safe. Uses atomic counters and concurrent access is safe for multi-agents.</p>
 *
 * @see SkillLibrary
 * @see SkillRefinementLoop
 * @since 1.0.0
 */
public class CriticAgent {
    private static final Logger LOGGER = TestLogger.getLogger(CriticAgent.class);

    private static final int MAX_ITERATIONS = 4;
    private static final long VALIDATION_TIMEOUT_MS = 30000;
    private static final long FEEDBACK_GENERATION_TIMEOUT_MS = 60000;

    private final SkillLibrary skillLibrary;
    private final AtomicInteger validationsPerformed = new AtomicInteger();
    private final AtomicInteger validationsPassed = new AtomicInteger();
    private final AtomicInteger validationsFailed = new AtomicInteger();
    private final AtomicLong totalValidationTime = new AtomicLong();
    private final AtomicLong totalFeedbackGenerationTime = new AtomicLong();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<CompletableFuture<ValidationResult>> currentValidation = new AtomicReference<>();

    private SkillRefinementLoop refinementLoop;

    // Singleton instance
    private static volatile CriticAgent instance;

    private CriticAgent(SkillLibrary skillLibrary) {
        this.skillLibrary = skillLibrary;
    }

    /**
     * Gets the singleton instance of the CriticAgent.
     *
     * @return Singleton instance
     */
    public static CriticAgent getInstance() {
        if (instance == null) {
            synchronized (CriticAgent.class) {
                if (instance == null) {
                    instance = new CriticAgent(SkillLibrary.getInstance());
                }
            }
        }
        return instance;
    }

    /**
     * Sets the refinement loop for submitting skills.
     *
     * @param loop The refinement loop
     */
    public void setRefinementLoop(SkillRefinementLoop loop) {
        this.refinementLoop = loop;
    }

    /**
     * Starts the critic agent.
     */
    public synchronized void start() {
        if (running.get()) {
            LOGGER.warn("CriticAgent is already running");
            return;
        }
        running.set(true);
        LOGGER.info("CriticAgent started");
    }

    /**
     * Stops the critic agent.
     */
    public synchronized void stop() {
        if (!running.get()) {
            return;
        }
        running.set(false);
        LOGGER.info("CriticAgent stopped");
    }

    /**
     * Checks if the critic agent is running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Validates a skill execution.
     *
     * @param skill The skill that was executed
     * @param executionContext Context with execution details
     * @return Validation result
     */
    public ValidationResult validate(Skill skill, ValidationContext executionContext) {
        long startTime = System.currentTimeMillis();
        validationsPerformed.incrementAndGet();

        try {
            // Check if task was completed
            boolean taskCompleted = executionContext.taskCompleted();

            if (!taskCompleted) {
                LOGGER.warn("Skill '{}' failed validation: task not completed", skill.getName());
                validationsFailed.incrementAndGet();
                return new ValidationResult(false, "Task not completed", 0);
            }

            // Check for errors
            if (executionContext.hasErrors()) {
                LOGGER.warn("Skill '{}' failed validation: errors occurred during execution", skill.getName());
                validationsFailed.incrementAndGet();
                return new ValidationResult(false, "Errors during execution: " + executionContext.getErrors(), 0);
            }

            // Check timing
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > VALIDATION_TIMEOUT_MS) {
                LOGGER.warn("Skill '{}' failed validation: timeout after {}ms", skill.getName(), elapsed);
                validationsFailed.incrementAndGet();
                return new ValidationResult(false, "Timeout", elapsed);
            }

            // Success
            long totalTime = System.currentTimeMillis() - startTime;
            totalValidationTime.addAndGet(totalTime);
            validationsPassed.incrementAndGet();
            LOGGER.info("Skill '{}' passed validation (time: {}ms)", skill.getName(), totalTime);
            return new ValidationResult(true, null, totalTime);

        } catch (Exception e) {
            LOGGER.error("Error validating skill '{}'", skill.getName(), e);
            validationsFailed.incrementAndGet();
            return new ValidationResult(false, "Error: " + e.getMessage(), 0);
        }
    }

    /**
     * Gets validation statistics.
     *
     * @return Statistics map
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("validationsPerformed", validationsPerformed.get());
        stats.put("validationsPassed", validationsPassed.get());
        stats.put("validationsFailed", validationsFailed.get());
        stats.put("successRate", calculateSuccessRate());
        stats.put("averageValidationTimeMs", calculateAverageValidationTime());
        stats.put("totalFeedbackGenerationTimeMs", totalFeedbackGenerationTime.get());
        stats.put("running", running.get());
        return stats;
    }

    private double calculateSuccessRate() {
        int total = validationsPerformed.get();
        return total > 0 ? (double) validationsPassed.get() / total : 1.0;
    }

    private long calculateAverageValidationTime() {
        int total = validationsPerformed.get();
        return total > 0 ? totalValidationTime.get() / total : 0L;
    }

    /**
     * Clears all statistics.
     */
    public void resetStatistics() {
        validationsPerformed.set(0);
        validationsPassed.set(0);
        validationsFailed.set(0);
        totalValidationTime.set(0L);
        totalFeedbackGenerationTime.set(0L);
    }

    /**
     * Gets the maximum number of iterations for refinement.
     *
     * @return Maximum iterations
     */
    public int getMaxIterations() {
        return MAX_ITERATIONS;
    }
}
