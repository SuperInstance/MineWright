package com.minewright.skill;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestrates the complete learning loop for automatic skill improvement.
 *
 * <p><b>Purpose:</b></p>
 * <p>SkillLearningLoop ties together pattern extraction, skill generation, effectiveness tracking,
 * and iterative refinement into a continuous learning cycle inspired by the Voyager pattern.</p>
 *
 * <p><b>Learning Cycle:</b></p>
 * <ol>
 *   <li><b>Observe:</b> Track execution sequences and outcomes</li>
 *   <li><b>Extract:</b> Identify recurring patterns from successful executions</li>
 *   <li><b>Generate:</b> Create skills from high-confidence patterns</li>
 *   <li><b>Execute:</b> Use generated skills in appropriate situations</li>
 *   <li><b>Evaluate:</b> Track effectiveness and identify skills needing refinement</li>
 *   <li><b>Refine:</b> Improve underperforming skills through re-generation</li>
 * </ol>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is thread-safe. The learning loop runs asynchronously to avoid
 * blocking the main game thread.</p>
 *
 * @see PatternExtractor
 * @see SkillAutoGenerator
 * @see SkillEffectivenessTracker
 * @since 1.0.0
 */
public class SkillLearningLoop {
    private static final Logger LOGGER = TestLogger.getLogger(SkillLearningLoop.class);

    private static volatile SkillLearningLoop instance;

    private static final int MIN_EXECUTIONS_FOR_EVALUATION = 5;
    private static final int REFINEMENT_CHECK_INTERVAL = 10;
    private static final int LEARNING_INTERVAL_SECONDS = 30;

    private final ExecutorService learningExecutor;
    private volatile boolean running = false;
    private Future<?> learningTask;
    private SkillRefinedCallback refinementCallback;
    private final AtomicInteger checksPerformed = new AtomicInteger(0);

    @FunctionalInterface
    public interface SkillRefinedCallback {
        void onSkillRefined(String skillId, String reason);
    }

    private SkillLearningLoop() {
        this.learningExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SkillLearningLoop");
            t.setDaemon(true);
            return t;
        });
        LOGGER.info("SkillLearningLoop initialized");
    }

    public static SkillLearningLoop getInstance() {
        if (instance == null) {
            synchronized (SkillLearningLoop.class) {
                if (instance == null) {
                    instance = new SkillLearningLoop();
                }
            }
        }
        return instance;
    }

    public void setRefinementCallback(SkillRefinedCallback callback) {
        this.refinementCallback = callback;
    }

    public synchronized void start() {
        if (running) {
            LOGGER.warn("SkillLearningLoop is already running");
            return;
        }
        running = true;
        learningTask = learningExecutor.submit(this::runLearningLoop);
        LOGGER.info("SkillLearningLoop started");
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        if (learningTask != null) learningTask.cancel(true);
        LOGGER.info("SkillLearningLoop stopped");
    }

    /**
     * Main learning loop that runs asynchronously.
     * Periodically checks for new sequences and performs skill generation and refinement.
     */
    private void runLearningLoop() {
        LOGGER.info("Learning loop started");

        while (running) {
            try {
                // Wait for the learning interval
                Thread.sleep(LEARNING_INTERVAL_SECONDS * 1000L);

                if (!running) break;

                // Perform learning cycle
                performLearningCycle();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.error("Error in learning loop", e);
            }
        }

        LOGGER.info("Learning loop ended");
    }

    /**
     * Performs one complete learning cycle:
     * 1. Extract patterns from successful sequences
     * 2. Generate new skills from patterns
     * 3. Check for skills needing refinement
     */
    private void performLearningCycle() {
        LOGGER.debug("Performing learning cycle...");

        // Get successful sequences from ExecutionTracker
        ExecutionTracker tracker = ExecutionTracker.getInstance();
        List<ExecutionSequence> sequences = tracker.getSuccessfulSequences();

        if (sequences.isEmpty()) {
            LOGGER.debug("No successful sequences available for learning");
            return;
        }

        LOGGER.info("Learning from {} successful sequences", sequences.size());

        // Generate skills from sequences
        SkillAutoGenerator generator = SkillAutoGenerator.getInstance();
        int generated = generator.generateAndRegisterSkills(sequences);

        if (generated > 0) {
            LOGGER.info("Generated {} new skill(s) from {} sequences", generated, sequences.size());
        }

        // Periodically check for refinement
        int check = checksPerformed.incrementAndGet();
        if (check % REFINEMENT_CHECK_INTERVAL == 0) {
            checkForRefinement();
        }
    }

    /**
     * Checks for skills that need refinement based on effectiveness tracking.
     */
    private void checkForRefinement() {
        LOGGER.debug("Checking for skills needing refinement...");

        SkillEffectivenessTracker tracker = SkillEffectivenessTracker.getInstance();
        SkillLibrary library = SkillLibrary.getInstance();

        // Get all skills sorted by success rate
        List<Skill> skills = library.getSkillsBySuccessRate();

        int refined = 0;
        int removed = 0;

        for (Skill skill : skills) {
            String skillId = skill.getName();

            // Skip skills without enough data
            if (skill.getExecutionCount() < MIN_EXECUTIONS_FOR_EVALUATION) continue;

            // Get recommendation from effectiveness tracker
            SkillEffectivenessTracker.Recommendation recommendation = tracker.getRecommendation(skillId);

            if (recommendation == SkillEffectivenessTracker.Recommendation.REFINE) {
                String reason = tracker.getRecommendationExplanation(skillId);
                LOGGER.info("Skill '{}' needs refinement: {}", skillId, reason);

                if (refinementCallback != null) {
                    refinementCallback.onSkillRefined(skillId, reason);
                }
                refined++;

            } else if (recommendation == SkillEffectivenessTracker.Recommendation.AVOID) {
                LOGGER.warn("Removing low-performing skill: {}", skillId);
                library.removeSkill(skillId);
                removed++;
            }
        }

        if (refined > 0 || removed > 0) {
            LOGGER.info("Refinement check complete: {} skills flagged for refinement, {} skills removed", refined, removed);
        }
    }

    /**
     * Forces an immediate learning cycle.
     */
    public void forceLearningCycle() {
        LOGGER.info("Forcing immediate learning cycle");
        performLearningCycle();
    }

    public boolean isRunning() {
        return running;
    }

    public Map<String, Object> getStatusReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("running", running);
        report.put("checksPerformed", checksPerformed.get());
        report.put("skillLibrarySize", SkillLibrary.getInstance().getSkillCount());
        report.put("successfulSequences", ExecutionTracker.getInstance().getSuccessfulSequences().size());
        return report;
    }

    public void shutdown() {
        stop();
        learningExecutor.shutdown();
        LOGGER.info("SkillLearningLoop shutdown complete");
    }
}
