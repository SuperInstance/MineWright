package com.minewright.mentorship;

import com.minewright.mentorship.MentorshipModels.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Detects teaching moments based on worker behavior and triggers.
 *
 * <p>This class analyzes worker behavior and context to determine
 * when teaching opportunities arise. It implements the teaching moment
 * detection logic with cooldowns and worker state checks.</p>
 *
 * @since 1.5.0
 */
public class TeachingMomentDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeachingMomentDetector.class);

    private final Map<String, Instant> lastTeachingMoment;
    private final Map<String, Integer> consecutiveSuccesses;

    public TeachingMomentDetector() {
        this.lastTeachingMoment = new java.util.concurrent.ConcurrentHashMap<>();
        this.consecutiveSuccesses = new java.util.concurrent.ConcurrentHashMap<>();
    }

    /**
     * Detects if a teaching moment should occur based on worker behavior.
     *
     * <p>Teaching moments are opportunities to transfer knowledge that arise naturally
     * from the situation. They should be relevant, timely, actionable, and appropriate
     * for the worker's skill level.</p>
     *
     * @param worker The worker to check
     * @param triggerType What triggered the potential teaching moment
     * @param context Context about the situation
     * @param hintGenerator Hint generator for creating moments
     * @param dialogueGenerator Dialogue generator for creating moments
     * @return Teaching moment if detected, null otherwise
     */
    public TeachingMoment detectTeachingMoment(WorkerProfile worker,
            TeachingMomentTrigger triggerType, String context,
            HintGenerator hintGenerator, DialogueGenerator dialogueGenerator) {

        // Check if worker is receptive to teaching
        if (!isWorkerTeachable(worker)) {
            LOGGER.debug("Worker '{}' is not teachable right now (focus: {}, stress: {})",
                worker.getName(), worker.getFocusLevel(), worker.getStressLevel());
            return null;
        }

        // Check cooldown
        Instant lastTaught = lastTeachingMoment.get(worker.getName());
        if (lastTaught != null) {
            long millisSince = Instant.now().toEpochMilli() - lastTaught.toEpochMilli();
            long cooldownMillis = getCooldownForTrigger(triggerType);
            if (millisSince < cooldownMillis) {
                LOGGER.debug("Teaching moment for '{}' blocked by cooldown ({}ms remaining)",
                    worker.getName(), cooldownMillis - millisSince);
                return null;
            }
        }

        // Determine if this is a teaching moment
        TeachingMoment moment = evaluateTeachingMoment(worker, triggerType, context,
            hintGenerator, dialogueGenerator);
        if (moment != null) {
            lastTeachingMoment.put(worker.getName(), Instant.now());
            LOGGER.info("Teaching moment detected for '{}' (trigger: {}, type: {})",
                worker.getName(), triggerType, moment.getType());
        }

        return moment;
    }

    /**
     * Evaluates if a situation constitutes a teaching moment and creates the moment.
     */
    private TeachingMoment evaluateTeachingMoment(WorkerProfile worker,
            TeachingMomentTrigger trigger, String context,
            HintGenerator hintGenerator, DialogueGenerator dialogueGenerator) {

        return switch (trigger) {
            case WORKER_STUCK -> createStuckTeachingMoment(worker, context, hintGenerator);
            case WORKER_MISTAKE -> createMistakeTeachingMoment(worker, context, dialogueGenerator);
            case WORKER_SUCCESS_SUBOPTIMAL -> createImprovementTeachingMoment(worker, context, dialogueGenerator);
            case WORKER_QUESTION -> createAnswerTeachingMoment(worker, context, dialogueGenerator);
            case NEW_CHALLENGE -> createChallengeTeachingMoment(worker, context, dialogueGenerator);
            case SKILL_MILESTONE -> createCelebrationMoment(worker, context, dialogueGenerator);
            case PATTERN_RECOGNITION -> createPatternTeachingMoment(worker, context, dialogueGenerator);
        };
    }

    /**
     * Creates a teaching moment when a worker is stuck.
     * Provides hands-on help or hints based on skill gap.
     */
    private TeachingMoment createStuckTeachingMoment(WorkerProfile worker, String context,
            HintGenerator hintGenerator) {
        // Don't teach if worker is frustrated
        if (worker.getStressLevel() > 0.7) {
            LOGGER.debug("Worker '{}' too stressed for teaching (stress: {})",
                worker.getName(), worker.getStressLevel());
            return null;
        }

        SkillLevel taskLevel = TaskDifficultyEstimator.estimateDifficulty(context);
        SkillLevel workerLevel = worker.getSkillLevel(context);

        if (taskLevel.ordinal() > workerLevel.ordinal() + 1) {
            // Task is too hard - demonstrate
            return TeachingMoment.handsOn(context,
                "This is tricky, let me show you how it's done...");
        }

        // Provide hint
        return TeachingMoment.hint(context, hintGenerator.generateHint(worker, context, 1));
    }

    /**
     * Creates a teaching moment when a worker makes a mistake.
     * Differentiates between repeated mistakes and first-time errors.
     */
    private TeachingMoment createMistakeTeachingMoment(WorkerProfile worker, String context,
            DialogueGenerator dialogueGenerator) {
        if (worker.hasRepeatedMistake(context)) {
            // Pattern - need more direct teaching
            LOGGER.debug("Repeated mistake detected for worker '{}' on '{}'",
                worker.getName(), context);
            return TeachingMoment.correction(context, dialogueGenerator.generateCorrection(worker, context));
        }

        // Track as a potential repeated mistake
        worker.trackMistake(context);

        // First time mistake - gentle guidance
        return TeachingMoment.gentleGuidance(context,
            dialogueGenerator.generateGentleCorrection(worker, context));
    }

    /**
     * Creates a teaching moment when worker succeeds but inefficiently.
     */
    private TeachingMoment createImprovementTeachingMoment(WorkerProfile worker, String context,
            DialogueGenerator dialogueGenerator) {
        return TeachingMoment.suggestion(context, dialogueGenerator.generateSuggestion(worker, context));
    }

    /**
     * Creates a teaching moment when a worker asks a question.
     * Uses Socratic questioning to guide to the answer.
     */
    private TeachingMoment createAnswerTeachingMoment(WorkerProfile worker, String context,
            DialogueGenerator dialogueGenerator) {
        List<String> questions = dialogueGenerator.generateSocraticQuestions(context);
        String dialogue = questions.get(0);  // Start with first question

        return TeachingMoment.socratic(context, dialogue);
    }

    /**
     * Creates a collaborative teaching moment for novel situations.
     */
    private TeachingMoment createChallengeTeachingMoment(WorkerProfile worker, String context,
            DialogueGenerator dialogueGenerator) {
        String foremanInput = dialogueGenerator.generateForemanVulnerability(context);

        if (foremanInput != null) {
            return TeachingMoment.collaborative(context, foremanInput);
        }

        return TeachingMoment.collaborative(context,
            "I haven't seen this exact situation before. " +
            "Let's figure it out together. What do you think is the best approach?");
    }

    /**
     * Creates a celebration teaching moment for skill milestones.
     */
    private TeachingMoment createCelebrationMoment(WorkerProfile worker, String context,
            DialogueGenerator dialogueGenerator) {
        String celebration = dialogueGenerator.generateCelebration(worker, context, consecutiveSuccesses);
        return TeachingMoment.celebration(context, celebration);
    }

    /**
     * Creates a teaching moment based on observed patterns.
     */
    private TeachingMoment createPatternTeachingMoment(WorkerProfile worker, String context,
            DialogueGenerator dialogueGenerator) {
        return TeachingMoment.insight(context,
            "I've noticed something interesting. " + dialogueGenerator.generateInsight(worker, context));
    }

    /**
     * Checks if worker is in a teachable state.
     */
    private boolean isWorkerTeachable(WorkerProfile worker) {
        return worker.getFocusLevel() > 0.6
            && worker.getStressLevel() < 0.7
            && worker.getRapportLevel() > 20;
    }

    /**
     * Gets cooldown duration for trigger type.
     */
    private long getCooldownForTrigger(TeachingMomentTrigger trigger) {
        return switch (trigger) {
            case WORKER_STUCK -> 2 * 60 * 1000L;  // 2 minutes
            case WORKER_MISTAKE -> 5 * 60 * 1000L;  // 5 minutes
            case WORKER_SUCCESS_SUBOPTIMAL -> 3 * 60 * 1000L;  // 3 minutes
            case WORKER_QUESTION -> 0;  // Always answer questions
            case NEW_CHALLENGE -> 10 * 60 * 1000L;  // 10 minutes
            case SKILL_MILESTONE -> 0;  // Always celebrate
            case PATTERN_RECOGNITION -> 5 * 60 * 1000L;  // 5 minutes
        };
    }

    /**
     * Interface for generating hints.
     */
    public interface HintGenerator {
        String generateHint(WorkerProfile worker, String context, int hintLevel);
    }

    /**
     * Interface for generating dialogue.
     */
    public interface DialogueGenerator {
        String generateCorrection(WorkerProfile worker, String context);
        String generateGentleCorrection(WorkerProfile worker, String context);
        String generateSuggestion(WorkerProfile worker, String context);
        java.util.List<String> generateSocraticQuestions(String context);
        String generateForemanVulnerability(String context);
        String generateCelebration(WorkerProfile worker, String context, java.util.Map<String, Integer> consecutiveSuccesses);
        String generateInsight(WorkerProfile worker, String context);
    }
}
