package com.minewright.skill;

import com.minewright.testutil.TestLogger;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implements the Voyager-style skill refinement loop with 3-4 iterations.
 *
 * <p><b>Refinement Process:</b></p>
 * <ol>
 *   <li>Execute skill and capture results</li>
 *   <li>Validate results with CriticAgent</li>
 *   <li>If validation fails, generate feedback</li>
 *   <li>Refine skill based on feedback</li>
 *   <li>Repeat until success or max iterations</li>
 * </ol>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>Thread-safe. Uses atomic counters and concurrent maps.</p>
 *
 * @see CriticAgent
 * @see SkillLibrary
 * @since 1.0.0
 */
public class SkillRefinementLoop {
    private static final Logger LOGGER = TestLogger.getLogger(SkillRefinementLoop.class);

    private static final int DEFAULT_MAX_ITERATIONS = 4;
    private static final long REFINEMENT_TIMEOUT_MS = 120000;

    private final SkillLibrary skillLibrary;
    private final CriticAgent criticAgent;
    private final int maxIterations;

    private final Map<String, Skill> refinedSkills;
    private final Map<String, RefinementState> currentRefinements;
    private final AtomicInteger totalRefinements;
    private final AtomicInteger totalSuccessfulRefinements;
    private final AtomicInteger totalFailedRefinements;
    private final AtomicInteger totalIterations;
    private final AtomicBoolean running;
    private final AtomicLong totalRefinementTime;

    private static volatile SkillRefinementLoop instance;

    /**
     * Creates a new SkillRefinementLoop.
     *
     * @param skillLibrary The skill library
     * @param maxIterations Maximum refinement iterations
     */
    public SkillRefinementLoop(SkillLibrary skillLibrary, int maxIterations) {
        this.skillLibrary = skillLibrary;
        this.criticAgent = CriticAgent.getInstance();
        this.maxIterations = Math.max(1, Math.min(maxIterations, 10));
        this.refinedSkills = new ConcurrentHashMap<>();
        this.currentRefinements = new ConcurrentHashMap<>();
        this.totalRefinements = new AtomicInteger(0);
        this.totalSuccessfulRefinements = new AtomicInteger(0);
        this.totalFailedRefinements = new AtomicInteger(0);
        this.totalIterations = new AtomicInteger(0);
        this.running = new AtomicBoolean(false);
        this.totalRefinementTime = new AtomicLong(0);

        criticAgent.setRefinementLoop(this);
    }

    /**
     * Creates a SkillRefinementLoop with default settings.
     *
     * @param skillLibrary The skill library
     */
    public SkillRefinementLoop(SkillLibrary skillLibrary) {
        this(skillLibrary, DEFAULT_MAX_ITERATIONS);
    }

    /**
     * Gets the singleton instance.
     *
     * @return Singleton instance
     */
    public static SkillRefinementLoop getInstance() {
        if (instance == null) {
            synchronized (SkillRefinementLoop.class) {
                if (instance == null) {
                    instance = new SkillRefinementLoop(SkillLibrary.getInstance());
                }
            }
        }
        return instance;
    }

    /**
     * Starts the refinement loop.
     */
    public void start() {
        running.set(true);
        LOGGER.info("Skill refinement loop started (max iterations: {})", maxIterations);
    }

    /**
     * Stops the refinement loop.
     */
    public void stop() {
        running.set(false);
        LOGGER.info("Skill refinement loop stopped");
    }

    /**
     * Checks if the loop is running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Refines a skill using the Voyager-style iterative process.
     *
     * @param skill The skill to refine
     * @param context The execution context
     * @return Refinement result
     */
    public CompletableFuture<RefinementResult> refineSkill(Skill skill, ValidationContext context) {
        if (skill == null || context == null) {
            return CompletableFuture.completedFuture(
                new RefinementResult(null, "Invalid skill or context", 0, false));
        }

        return CompletableFuture.supplyAsync(() -> {
            return doRefinementLoop(skill, context);
        });
    }

    /**
     * Performs the actual refinement loop.
     */
    private RefinementResult doRefinementLoop(Skill skill, ValidationContext context) {
        long startTime = System.currentTimeMillis();
        int iteration = 0;
        Skill currentSkill = skill;
        String lastError = null;

        totalRefinements.incrementAndGet();
        String skillName = skill.getName();
        currentRefinements.put(skillName, new RefinementState(skillName, 0, System.currentTimeMillis()));

        try {
            while (iteration < maxIterations) {
                iteration++;
                totalIterations.incrementAndGet();
                currentRefinements.put(skillName, new RefinementState(skillName, iteration, System.currentTimeMillis()));

                LOGGER.info("Refinement iteration {}/{} for skill '{}'", iteration, maxIterations, skillName);

                // Step 1: Validate the skill
                ValidationResult validationResult = criticAgent.validate(currentSkill, context);

                if (validationResult.isValid()) {
                    // Success!
                    long totalTime = System.currentTimeMillis() - startTime;
                    totalRefinementTime.addAndGet(totalTime);
                    totalSuccessfulRefinements.incrementAndGet();

                    if (currentSkill != skill) {
                        skillLibrary.addSkill(currentSkill);
                        refinedSkills.put(skillName, currentSkill);
                    }

                    currentRefinements.remove(skillName);
                    LOGGER.info("Skill '{}' refined successfully after {} iterations ({}ms)",
                        skillName, iteration, totalTime);

                    return new RefinementResult(currentSkill,
                        "Refinement successful after " + iteration + " iterations",
                        iteration, true);
                }

                // Step 2: Generate feedback based on validation failure
                lastError = validationResult.getReason();
                LOGGER.debug("Skill '{}' iteration {} failed: {}", skillName, iteration, lastError);

                // For now, we'll use a simple refinement approach
                // In a full implementation, this would call the LLM to generate refined code
                Skill refinedSkill = createRefinedSkill(currentSkill, iteration, lastError);
                if (refinedSkill == null) {
                    break;
                }
                currentSkill = refinedSkill;
            }

            // Failed after max iterations
            long totalTime = System.currentTimeMillis() - startTime;
            totalRefinementTime.addAndGet(totalTime);
            totalFailedRefinements.incrementAndGet();
            currentRefinements.remove(skillName);

            LOGGER.warn("Skill '{}' refinement failed after {} iterations: {}",
                skillName, maxIterations, lastError);

            return new RefinementResult(null,
                "Failed after " + maxIterations + " iterations: " + lastError,
                maxIterations, false);

        } catch (Exception e) {
            LOGGER.error("Error refining skill '{}'", skillName, e);
            totalFailedRefinements.incrementAndGet();
            currentRefinements.remove(skillName);
            return new RefinementResult(null, "Error: " + e.getMessage(), iteration, false);
        }
    }

    /**
     * Creates a refined version of a skill.
     * In a full implementation, this would use LLM to generate improved code.
     *
     * @param skill The original skill
     * @param iteration Current iteration number
     * @param feedback Feedback from validation
     * @return Refined skill or null if cannot refine
     */
    private Skill createRefinedSkill(Skill skill, int iteration, String feedback) {
        // Placeholder implementation
        // In production, this would call LLM to generate refined code
        LOGGER.debug("Creating refined skill for '{}' (iteration {})", skill.getName(), iteration);
        return skill; // Return same skill for now
    }

    /**
     * Submits a skill for refinement.
     *
     * @param skill The skill to refine
     * @param feedback The feedback for refinement
     */
    public void submitForRefinement(Skill skill, String feedback) {
        if (skill == null) {
            LOGGER.warn("Cannot submit null skill for refinement");
            return;
        }
        LOGGER.info("Skill '{}' submitted for refinement with feedback: {}",
            skill.getName(), feedback);
    }

    /**
     * Checks if a skill has been refined.
     *
     * @param skillName The name of the skill
     * @return true if the skill was successfully refined
     */
    public boolean wasRefined(String skillName) {
        return refinedSkills.containsKey(skillName);
    }

    /**
     * Gets the refined skill.
     *
     * @param skillName The name of the skill
     * @return The refined skill, or null if not found
     */
    public Skill getRefinedSkill(String skillName) {
        return refinedSkills.get(skillName);
    }

    /**
     * Gets the current refinement state for a skill.
     *
     * @param skillName The name of the skill
     * @return The current refinement state, or null if not being refined
     */
    public RefinementState getCurrentRefinement(String skillName) {
        return currentRefinements.get(skillName);
    }

    /**
     * Gets the total number of refinements.
     *
     * @return The total refinement count
     */
    public int getTotalRefinements() {
        return totalRefinements.get();
    }

    /**
     * Gets the number of successful refinements.
     *
     * @return The successful refinement count
     */
    public int getSuccessfulRefinements() {
        return totalSuccessfulRefinements.get();
    }

    /**
     * Gets the number of failed refinements.
     *
     * @return The failed refinement count
     */
    public int getFailedRefinements() {
        return totalFailedRefinements.get();
    }

    /**
     * Gets the total number of iterations used.
     *
     * @return The total iteration count
     */
    public int getTotalIterations() {
        return totalIterations.get();
    }

    /**
     * Gets the maximum number of iterations allowed.
     *
     * @return The maximum iteration count
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * Gets statistics about the refinement loop.
     *
     * @return Statistics map
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRefinements", totalRefinements.get());
        stats.put("successfulRefinements", totalSuccessfulRefinements.get());
        stats.put("failedRefinements", totalFailedRefinements.get());
        stats.put("totalIterations", totalIterations.get());
        stats.put("maxIterations", maxIterations);
        stats.put("running", running.get());
        stats.put("totalRefinementTimeMs", totalRefinementTime.get());
        stats.put("currentRefinements", currentRefinements.size());
        return stats;
    }

    /**
     * Clears all statistics.
     */
    public void resetStatistics() {
        totalRefinements.set(0);
        totalSuccessfulRefinements.set(0);
        totalFailedRefinements.set(0);
        totalIterations.set(0);
        totalRefinementTime.set(0);
        refinedSkills.clear();
        currentRefinements.clear();
        LOGGER.info("Refinement loop statistics reset");
    }

    /**
     * State of an ongoing refinement.
     */
    public static class RefinementState {
        private final String skillName;
        private final int iteration;
        private final long startTime;

        public RefinementState(String skillName, int iteration, long startTime) {
            this.skillName = skillName;
            this.iteration = iteration;
            this.startTime = startTime;
        }

        public String getSkillName() {
            return skillName;
        }

        public int getIteration() {
            return iteration;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
    }

    /**
     * Result of a refinement attempt.
     */
    public static class RefinementResult {
        private final Skill refinedSkill;
        private final String message;
        private final int iterations;
        private final boolean success;

        public RefinementResult(Skill refinedSkill, String message, int iterations, boolean success) {
            this.refinedSkill = refinedSkill;
            this.message = message;
            this.iterations = iterations;
            this.success = success;
        }

        public Skill getRefinedSkill() {
            return refinedSkill;
        }

        public String getMessage() {
            return message;
        }

        public int getIterations() {
            return iterations;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}
