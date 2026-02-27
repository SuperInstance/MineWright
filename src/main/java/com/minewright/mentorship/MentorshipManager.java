package com.minewright.mentorship;

import com.minewright.entity.ForemanEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages mentorship dynamics between Foreman and Worker entities.
 *
 * <p>This system tracks learning progress, adjusts dialogue based on skill levels,
 * provides scaffolding for skill development, and creates genuine teaching moments.</p>
 *
 * <p><b>Core Features:</b></p>
 * <ul>
 *   <li>Teaching moment detection based on worker behavior</li>
 *   <li>Explanation depth adjustment by skill level</li>
 *   <li>Scaffolding dialogue that fades as competence grows</li>
 *   <li>Genuine praise that feels earned and specific</li>
 *   <li>Non-condescending correction patterns</li>
 *   <li>Progress celebration and milestone tracking</li>
 *   <li>Foreman vulnerability and asking for help</li>
 * </ul>
 *
 * <p><b>Research Sources:</b></p>
 * <ul>
 *   <li>Star Wars mentorship patterns (testing character, philosophy over technique)</li>
 *   <li>Socratic teaching method (questioning techniques)</li>
 *   <li>Instructional scaffolding (Vygotsky's Zone of Proximal Development)</li>
 *   <li>Workplace coaching (CSS Framework: Clear, Specific, Supportive)</li>
 *   <li>Master craftsman traditions (observation, demonstration, practice)</li>
 * </ul>
 *
 * @since 1.5.0
 * @see <a href="https://www.studysmarter.co.uk/explanations/greek/greek-history/greek-socratic-method/">Socratic Method</a>
 * @see <a href="https://www.edutopia.org/article/powerful-scaffolding-strategies-support-learning/">Scaffolding Strategies</a>
 */
public class MentorshipManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MentorshipManager.class);

    private final ForemanEntity foreman;
    private final Map<String, WorkerProfile> workers;
    private final MentorshipPersonality foremanPersonality;

    // Teaching state
    private final Map<String, Instant> lastTeachingMoment;
    private final Map<String, Integer> consecutiveSuccesses;
    private final Map<String, Set<String>> taughtConcepts;

    /**
     * Creates a new MentorshipManager for the given foreman.
     *
     * @param foreman The foreman entity who will be the mentor
     */
    public MentorshipManager(ForemanEntity foreman) {
        this.foreman = foreman;
        this.workers = new ConcurrentHashMap<>();
        this.foremanPersonality = new MentorshipPersonality();
        this.lastTeachingMoment = new ConcurrentHashMap<>();
        this.consecutiveSuccesses = new ConcurrentHashMap<>();
        this.taughtConcepts = new ConcurrentHashMap<>();

        LOGGER.info("MentorshipManager initialized for foreman '{}'",
            foreman.getEntityName());
    }

    // ========== Worker Registration ==========

    /**
     * Registers a worker for mentorship tracking.
     *
     * @param workerName Unique name of the worker
     * @param workerRole Role of the worker (e.g., "builder", "miner")
     */
    public void registerWorker(String workerName, String workerRole) {
        workers.put(workerName, new WorkerProfile(workerName, workerRole));
        taughtConcepts.put(workerName, ConcurrentHashMap.newKeySet());
        consecutiveSuccesses.put(workerName, 0);

        LOGGER.info("Registered worker '{}' for mentorship (role: {})",
            workerName, workerRole);
    }

    /**
     * Unregisters a worker from mentorship tracking.
     *
     * @param workerName Name of the worker to unregister
     */
    public void unregisterWorker(String workerName) {
        workers.remove(workerName);
        taughtConcepts.remove(workerName);
        consecutiveSuccesses.remove(workerName);
        lastTeachingMoment.remove(workerName);

        LOGGER.info("Unregistered worker '{}' from mentorship", workerName);
    }

    // ========== Teaching Moment Detection ==========

    /**
     * Detects if a teaching moment should occur based on worker behavior.
     *
     * <p>Teaching moments are opportunities to transfer knowledge that arise naturally
     * from the situation. They should be relevant, timely, actionable, and appropriate
     * for the worker's skill level.</p>
     *
     * @param workerName The worker to check
     * @param triggerType What triggered the potential teaching moment
     * @param context Context about the situation
     * @return Teaching moment if detected, null otherwise
     */
    public TeachingMoment detectTeachingMoment(String workerName,
            TeachingMomentTrigger triggerType, String context) {

        WorkerProfile worker = workers.get(workerName);
        if (worker == null) {
            LOGGER.warn("Cannot detect teaching moment for unknown worker: {}", workerName);
            return null;
        }

        // Check if worker is receptive to teaching
        if (!isWorkerTeachable(worker)) {
            LOGGER.debug("Worker '{}' is not teachable right now (focus: {}, stress: {})",
                workerName, worker.getFocusLevel(), worker.getStressLevel());
            return null;
        }

        // Check cooldown
        Instant lastTaught = lastTeachingMoment.get(workerName);
        if (lastTaught != null) {
            long millisSince = Instant.now().toEpochMilli() - lastTaught.toEpochMilli();
            long cooldownMillis = getCooldownForTrigger(triggerType);
            if (millisSince < cooldownMillis) {
                LOGGER.debug("Teaching moment for '{}' blocked by cooldown ({}ms remaining)",
                    workerName, cooldownMillis - millisSince);
                return null;
            }
        }

        // Determine if this is a teaching moment
        TeachingMoment moment = evaluateTeachingMoment(worker, triggerType, context);
        if (moment != null) {
            lastTeachingMoment.put(workerName, Instant.now());
            LOGGER.info("Teaching moment detected for '{}' (trigger: {}, type: {})",
                workerName, triggerType, moment.getType());
        }

        return moment;
    }

    /**
     * Evaluates if a situation constitutes a teaching moment and creates the moment.
     */
    private TeachingMoment evaluateTeachingMoment(WorkerProfile worker,
            TeachingMomentTrigger trigger, String context) {

        return switch (trigger) {
            case WORKER_STUCK -> createStuckTeachingMoment(worker, context);
            case WORKER_MISTAKE -> createMistakeTeachingMoment(worker, context);
            case WORKER_SUCCESS_SUBOPTIMAL -> createImprovementTeachingMoment(worker, context);
            case WORKER_QUESTION -> createAnswerTeachingMoment(worker, context);
            case NEW_CHALLENGE -> createChallengeTeachingMoment(worker, context);
            case SKILL_MILESTONE -> createCelebrationMoment(worker, context);
            case PATTERN_RECOGNITION -> createPatternTeachingMoment(worker, context);
        };
    }

    /**
     * Creates a teaching moment when a worker is stuck.
     * Provides hands-on help or hints based on skill gap.
     */
    private TeachingMoment createStuckTeachingMoment(WorkerProfile worker, String context) {
        // Don't teach if worker is frustrated
        if (worker.getStressLevel() > 0.7) {
            LOGGER.debug("Worker '{}' too stressed for teaching (stress: {})",
                worker.getName(), worker.getStressLevel());
            return null;
        }

        SkillLevel taskLevel = estimateTaskDifficulty(context);
        SkillLevel workerLevel = worker.getSkillLevel(context);

        if (taskLevel.ordinal() > workerLevel.ordinal() + 1) {
            // Task is too hard - demonstrate
            return TeachingMoment.handsOn(context,
                "This is tricky, let me show you how it's done...");
        }

        // Provide hint
        return TeachingMoment.hint(context, generateHint(worker, context, 1));
    }

    /**
     * Creates a teaching moment when a worker makes a mistake.
     * Differentiates between repeated mistakes and first-time errors.
     */
    private TeachingMoment createMistakeTeachingMoment(WorkerProfile worker, String context) {
        String errorKey = "error:" + context;

        if (worker.hasRepeatedMistake(context)) {
            // Pattern - need more direct teaching
            LOGGER.debug("Repeated mistake detected for worker '{}' on '{}'",
                worker.getName(), context);
            return TeachingMoment.correction(context, generateCorrection(worker, context));
        }

        // Track as a potential repeated mistake
        worker.trackMistake(context);

        // First time mistake - gentle guidance
        return TeachingMoment.gentleGuidance(context,
            generateGentleCorrection(worker, context));
    }

    /**
     * Creates a teaching moment when worker succeeds but inefficiently.
     */
    private TeachingMoment createImprovementTeachingMoment(WorkerProfile worker, String context) {
        return TeachingMoment.suggestion(context, generateSuggestion(worker, context));
    }

    /**
     * Creates a teaching moment when a worker asks a question.
     * Uses Socratic questioning to guide to the answer.
     */
    private TeachingMoment createAnswerTeachingMoment(WorkerProfile worker, String context) {
        List<String> questions = generateSocraticQuestions(context);
        String dialogue = questions.get(0);  // Start with first question

        return TeachingMoment.socratic(context, dialogue);
    }

    /**
     * Creates a collaborative teaching moment for novel situations.
     */
    private TeachingMoment createChallengeTeachingMoment(WorkerProfile worker, String context) {
        String foremanInput = generateForemanVulnerability(context);

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
    private TeachingMoment createCelebrationMoment(WorkerProfile worker, String context) {
        String celebration = generateCelebration(worker, context);
        return TeachingMoment.celebration(context, celebration);
    }

    /**
     * Creates a teaching moment based on observed patterns.
     */
    private TeachingMoment createPatternTeachingMoment(WorkerProfile worker, String context) {
        return TeachingMoment.insight(context,
            "I've noticed something interesting. " + generateInsight(worker, context));
    }

    // ========== Explanation Depth Adjustment ==========

    /**
     * Determines the appropriate explanation depth for a worker.
     *
     * <p>Based on Vygotsky's Zone of Proximal Development, this adjusts
     * the level of support based on the gap between task difficulty
     * and worker skill level.</p>
     *
     * @param workerName The worker to assess
     * @param taskContext Context of the task
     * @return Appropriate explanation depth
     */
    public ExplanationDepth getExplanationDepth(String workerName, String taskContext) {
        WorkerProfile worker = workers.get(workerName);
        if (worker == null) {
            return ExplanationDepth.DETAILED;  // Default to detailed for unknown workers
        }

        SkillLevel workerLevel = worker.getSkillLevel(taskContext);
        SkillLevel taskLevel = estimateTaskDifficulty(taskContext);

        int gap = taskLevel.ordinal() - workerLevel.ordinal();

        // Adjust based on rapport and stress
        if (worker.getStressLevel() > 0.6) {
            // Stressed workers need more support
            LOGGER.debug("Worker '{}' is stressed, providing more support", workerName);
            return ExplanationDepth.DETAILED;
        }

        if (worker.getRapportLevel() > 60 && gap <= 0) {
            // High rapport + easier task = minimal guidance
            return ExplanationDepth.CONFIRMATION;
        }

        return getDepthForGap(gap);
    }

    /**
     * Maps skill gap to explanation depth.
     */
    private ExplanationDepth getDepthForGap(int gap) {
        return switch (gap) {
            case -3, -2 -> ExplanationDepth.MINIMAL;      // Task much easier than skill
            case -1 -> ExplanationDepth.CONFIRMATION;     // Task slightly easier
            case 0 -> ExplanationDepth.HINTS;             // Task matches skill
            case 1 -> ExplanationDepth.SCAFFOLDED;        // Task slightly harder
            case 2 -> ExplanationDepth.DETAILED;          // Task significantly harder
            default -> ExplanationDepth.HANDS_ON;          // Task far beyond skill
        };
    }

    // ========== Scaffolding Dialogue Generation ==========

    /**
     * Generates a hint without giving the solution.
     *
     * <p>Uses a three-tier hint system:</p>
     * <ol>
     *   <li>Conceptual hints (think about principles)</li>
     *   <li>Process hints (step-by-step thinking)</li>
     *   <li>Specific hints (direct guidance)</li>
     * </ol>
     *
     * @param worker The worker to generate hint for
     * @param context The task context
     * @param hintLevel Which level of hint (1-3)
     * @return The hint dialogue
     */
    public String generateHint(WorkerProfile worker, String context, int hintLevel) {
        return switch (hintLevel) {
            case 1 -> getConceptualHint(context);
            case 2 -> getProcessHint(context);
            case 3 -> getSpecificHint(context);
            default -> "What do you think the first step is?";
        };
    }

    /**
     * Generates conceptual hints about principles.
     */
    private String getConceptualHint(String context) {
        String lower = context.toLowerCase();

        if (lower.contains("wall") || lower.contains("build") || lower.contains("structure")) {
            return "Think about what makes structures stable and level.";
        }
        if (lower.contains("redstone") || lower.contains("circuit") || lower.contains("wire")) {
            return "Consider how the signal flows and what powers it.";
        }
        if (lower.contains("water") || lower.contains("flow") || lower.contains("liquid")) {
            return "Think about how water moves in Minecraft and what blocks it can pass through.";
        }
        if (lower.contains("farm") || lower.contains("crop") || lower.contains("grow")) {
            return "What do crops need to grow successfully?";
        }
        if (lower.contains("mine") || lower.contains("ore") || lower.contains("dig")) {
            return "Think about the most efficient way to gather resources.";
        }

        return "What's the core principle we need to consider here?";
    }

    /**
     * Generates process hints about approach.
     */
    private String getProcessHint(String context) {
        String lower = context.toLowerCase();

        if (lower.contains("wall") || lower.contains("build")) {
            return "Start with a solid foundation, then build up in layers.";
        }
        if (lower.contains("redstone") || lower.contains("circuit")) {
            return "Trace the path from power source to output. Where's the signal stopping?";
        }
        if (lower.contains("water") || lower.contains("flow")) {
            return "Water flows and spreads. Check if there's a path for it to follow.";
        }
        if (lower.contains("farm") || lower.contains("crop")) {
            return "Consider the layout for water reach and light levels.";
        }
        if (lower.contains("mine") || lower.contains("ore")) {
            return "Think about branch mining patterns and cave exploration efficiency.";
        }

        return "Break it down into smaller, manageable steps.";
    }

    /**
     * Generates specific hints with direct guidance.
     */
    private String getSpecificHint(String context) {
        String lower = context.toLowerCase();

        if (lower.contains("wall") || lower.contains("build")) {
            return "Try starting with the corners to frame the structure.";
        }
        if (lower.contains("redstone") || lower.contains("circuit")) {
            return "A repeater might help boost the signal. What direction should it face?";
        }
        if (lower.contains("water") || lower.contains("flow")) {
            return "Water hydrates soil up to 4 blocks away. Check your spacing.";
        }
        if (lower.contains("farm") || lower.contains("crop")) {
            return "Crops need light level 9 or higher. Is your farm well-lit?";
        }
        if (lower.contains("mine")) {
            return "Try mining at Y=-58 for the best diamond distribution.";
        }

        return "Focus on one part at a time, then move to the next.";
    }

    /**
     * Generates Socratic questioning sequence.
     *
     * <p>Based on the Socratic method, these questions guide workers
     * to discover answers themselves rather than being told directly.</p>
     */
    public List<String> generateSocraticQuestions(String context) {
        List<String> questions = new ArrayList<>();

        questions.add("What's your goal here?");
        questions.add("What have you tried so far?");

        String lower = context.toLowerCase();

        if (lower.contains("build") || lower.contains("create")) {
            questions.add("What would happen if we tried a different arrangement?");
            questions.add("Is there a way to make this more efficient?");
        } else if (lower.contains("redstone") || lower.contains("mechanism")) {
            questions.add("Where is the signal flowing?");
            questions.add("What's powering this component?");
            questions.add("What happens if the signal path is blocked?");
        } else if (lower.contains("farm") || lower.contains("crop")) {
            questions.add("What do these crops need to thrive?");
            questions.add("How does the water reach them?");
        } else {
            questions.add("What do you think would happen if...?");
            questions.add("Is there another way to approach this?");
        }

        questions.add("What have you learned from trying this?");
        return questions;
    }

    // ========== Praise Generation ==========

    /**
     * Generates specific, genuine praise for worker achievement.
     *
     * <p>Uses the CSS Framework: Clear observation, Specific impact, Supportive tone.
     * Avoids generic praise like "good job" in favor of specific feedback.</p>
     *
     * @param workerName The worker being praised
     * @param completion Details about what was completed
     * @return Specific praise dialogue
     */
    public String generatePraise(String workerName, TaskCompletion completion) {
        WorkerProfile worker = workers.get(workerName);
        if (worker == null) {
            return "Well done!";
        }

        String observation = completion.getSpecificAction();
        String impact = completion.getOutcome();
        String trait = completion.getDemonstratedTrait();

        // Use different templates based on trait
        return switch (trait.toLowerCase()) {
            case "speed", "efficiency", "fast", "quick" -> String.format(
                "You finished that quickly, which gave us time for %s. Shows your efficiency.",
                impact
            );
            case "quality", "precision", "accuracy", "detail", "careful" -> String.format(
                "The %s is perfect. That attention to detail really stands out.",
                observation
            );
            case "creativity", "creative", "innovative", "clever", "smart" -> String.format(
                "I never would have thought to %s. That's a creative approach to %s.",
                observation, impact
            );
            case "perseverance", "determination", "persistent", "stuck with it" -> String.format(
                "You stuck with %s even when it was tricky. That determination is valuable.",
                observation
            );
            case "improvement", "improved", "growth", "progress" -> String.format(
                "This is much better than your previous attempts. You've really improved at %s.",
                trait
            );
            case "teamwork", "helping", "cooperative", "together" -> String.format(
                "Your %s helped with %s. That's what makes a good team.",
                observation, impact
            );
            default -> String.format(
                "I noticed %s. That really helped with %s. Shows your %s.",
                observation, impact, trait
            );
        };
    }

    // ========== Correction Generation ==========

    /**
     * Generates non-condescending correction using CSS framework.
     *
     * <p>Principles:</p>
     * <ul>
     *   <li>Use "I notice" instead of "You did"</li>
     *   <li>Use "Let's" instead of "You should"</li>
     *   <li>Focus on work, not worker</li>
     *   <li>Share responsibility when appropriate</li>
     *   <li>End with confidence</li>
     * </ul>
     */
    public String generateCorrection(WorkerProfile worker, String context) {
        TaskError error = analyzeError(worker, context);

        // Use CSS: Clear observation, Specific impact, Supportive solution
        return String.format(
            "I noticed %s. %s Let's %s.",
            getSpecificObservation(error),
            getImpactExplanation(error),
            getCollaborativeSolution(error)
        );
    }

    /**
     * Generates gentle correction for first-time mistakes.
     */
    private String generateGentleCorrection(WorkerProfile worker, String context) {
        TaskError error = analyzeError(worker, context);

        return String.format(
            "Hmm, let me show you something. %s Would you like to try a different approach?",
            getSpecificObservation(error)
        );
    }

    /**
     * Generates specific observation of the issue.
     */
    private String getSpecificObservation(TaskError error) {
        return switch (error.getType()) {
            case WRONG_MATERIAL -> String.format("you used %s here", error.getDetail());
            case MISSING_STEP -> String.format("this section is missing %s", error.getDetail());
            case SAFETY_ISSUE -> String.format("there's a %s concern", error.getDetail());
            case EFFICIENCY_ISSUE -> String.format("you're doing %s", error.getDetail());
            case STRUCTURAL_ISSUE -> String.format("the %s might not hold", error.getDetail());
        };
    }

    /**
     * Explains the impact of the issue.
     */
    private String getImpactExplanation(TaskError error) {
        return switch (error.getType()) {
            case WRONG_MATERIAL -> String.format("It might not work as well as %s would.",
                error.getSuggestedAlternative());
            case MISSING_STEP -> String.format("That could cause issues with %s.", error.getDetail());
            case SAFETY_ISSUE -> "That could be unsafe or cause problems.";
            case EFFICIENCY_ISSUE -> "That takes more time and effort than necessary.";
            case STRUCTURAL_ISSUE -> "That might lead to instability or collapse.";
        };
    }

    /**
     * Provides collaborative solution.
     */
    private String getCollaborativeSolution(TaskError error) {
        return switch (error.getType()) {
            case WRONG_MATERIAL -> String.format("try using %s instead", error.getSuggestedAlternative());
            case MISSING_STEP -> String.format("add %s here", error.getDetail());
            case SAFETY_ISSUE -> "move it to a safer location";
            case EFFICIENCY_ISSUE -> "try this more efficient approach";
            case STRUCTURAL_ISSUE -> "reinforce it with additional support";
        };
    }

    // ========== Celebration Generation ==========

    /**
     * Generates celebration for worker progress.
     *
     * <p>Celebrates growth by comparing to past performance, not absolute standards.
     * Focuses on the worker's journey and improvement.</p>
     */
    public String generateCelebration(WorkerProfile worker, String context) {
        SkillMilestone milestone = detectMilestone(worker, context);

        // Track success
        worker.recordSuccess(context);

        return switch (milestone) {
            case FIRST_SUCCESS -> String.format(
                "First time completing %s! Well done on getting through it!",
                context
            );
            case CONSISTENT_PERFORMANCE -> {
                int count = consecutiveSuccesses.merge(worker.getName(), 1, Integer::sum);
                yield String.format(
                    "That's the %d time in a row you've nailed %s. You've really got this down!",
                    count, context
                );
            }
            case SPEED_IMPROVEMENT -> String.format(
                "That was much faster than before. Your improvement with %s is noticeable!",
                context
            );
            case QUALITY_LEAP -> String.format(
                "This is noticeably better quality than your earlier attempts. Great progress on %s!",
                context
            );
            case INDEPENDENCE -> {
                worker.recordIndependentCompletion(context);
                yield "You completed that entirely on your own! That's real progress.";
            }
            case INNOVATION -> String.format(
                "I've never seen that approach to %s before. Creative thinking!",
                context
            );
            case TEACHING_OTHER -> String.format(
                "You're helping others learn %s now. You've come full circle!",
                context
            );
        };
    }

    /**
     * Detects what milestone the worker has achieved.
     */
    private SkillMilestone detectMilestone(WorkerProfile worker, String context) {
        int successCount = worker.getSuccessCount(context);

        if (successCount == 1) return SkillMilestone.FIRST_SUCCESS;
        if (successCount == 5) {
            consecutiveSuccesses.put(worker.getName(), 5);
            return SkillMilestone.CONSISTENT_PERFORMANCE;
        }
        if (worker.hasImprovedSpeed(context)) return SkillMilestone.SPEED_IMPROVEMENT;
        if (worker.hasImprovedQuality(context)) return SkillMilestone.QUALITY_LEAP;
        if (worker.recentlyCompletedWithoutHelp(context)) return SkillMilestone.INDEPENDENCE;
        if (worker.usedCreativeApproach(context)) return SkillMilestone.INNOVATION;

        return SkillMilestone.FIRST_SUCCESS;  // Default
    }

    // ========== Foreman Vulnerability ==========

    /**
     * Generates dialogue where foreman admits uncertainty or asks for help.
     *
     * <p>This makes the foreman more relatable and models that learning is ongoing.
     * Based on research showing mentors who acknowledge their own limitations
     * are more effective and trusted.</p>
     */
    public String generateForemanVulnerability(String context) {
        if (foremanPersonality.shouldAdmitUncertainty(context)) {
            return String.format(
                "You know, I haven't actually done %s before. What's your approach?",
                context
            );
        }

        if (foremanPersonality.shouldAskWorker(context)) {
            return String.format(
                "You've been working with %s more than I have lately. What would you suggest?",
                context
            );
        }

        return null;
    }

    /**
     * Generates suggestion for improvement.
     */
    private String generateSuggestion(WorkerProfile worker, String context) {
        return String.format(
            "You completed it, but there might be a more efficient way. Next time, %s",
            getProcessHint(context)
        );
    }

    /**
     * Generates insight about worker's progress.
     */
    private String generateInsight(WorkerProfile worker, String context) {
        return String.format(
            "you've been improving consistently with %s. Your technique is getting much smoother.",
            context
        );
    }

    // ========== Helper Methods ==========

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
     * Estimates task difficulty based on context.
     */
    private SkillLevel estimateTaskDifficulty(String context) {
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

    /**
     * Analyzes error to determine type and solution.
     */
    private TaskError analyzeError(WorkerProfile worker, String context) {
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

    // ========== Getters ==========

    public Map<String, WorkerProfile> getWorkers() {
        return Collections.unmodifiableMap(workers);
    }

    public WorkerProfile getWorker(String workerName) {
        return workers.get(workerName);
    }

    // ========== NBT Persistence ==========

    /**
     * Saves mentorship data to NBT for world save.
     */
    public void saveToNBT(CompoundTag tag) {
        CompoundTag mentorshipTag = new CompoundTag();

        // Save worker profiles
        for (Map.Entry<String, WorkerProfile> entry : workers.entrySet()) {
            CompoundTag workerTag = new CompoundTag();
            entry.getValue().saveToNBT(workerTag);
            mentorshipTag.put(entry.getKey(), workerTag);
        }

        tag.put("MentorshipData", mentorshipTag);

        LOGGER.debug("Saved mentorship data for {} workers", workers.size());
    }

    /**
     * Loads mentorship data from NBT.
     */
    public void loadFromNBT(CompoundTag tag) {
        if (!tag.contains("MentorshipData")) {
            return;
        }

        CompoundTag mentorshipTag = tag.getCompound("MentorshipData");

        for (String workerName : mentorshipTag.getAllKeys()) {
            CompoundTag workerTag = mentorshipTag.getCompound(workerName);
            WorkerProfile profile = WorkerProfile.loadFromNBT(workerTag);
            workers.put(workerName, profile);
            taughtConcepts.put(workerName, ConcurrentHashMap.newKeySet());
        }

        LOGGER.info("Loaded mentorship data for {} workers", workers.size());
    }

    // ========== Inner Classes ==========

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
     * Worker skill and progress tracking.
     */
    public static class WorkerProfile {
        private final String name;
        private final String role;
        private final Map<String, SkillLevel> skills;
        private final Map<String, Integer> taskHistory;
        private final Set<String> repeatedMistakes;
        private int rapportLevel;
        private double stressLevel;
        private double focusLevel;

        public WorkerProfile(String name, String role) {
            this.name = name;
            this.role = role;
            this.skills = new ConcurrentHashMap<>();
            this.taskHistory = new ConcurrentHashMap<>();
            this.repeatedMistakes = ConcurrentHashMap.newKeySet();
            this.rapportLevel = 10;
            this.stressLevel = 0.0;
            this.focusLevel = 1.0;
        }

        public SkillLevel getSkillLevel(String context) {
            // Extract skill category from context
            String category = extractSkillCategory(context);
            return skills.getOrDefault(category, SkillLevel.BEGINNER);
        }

        private String extractSkillCategory(String context) {
            String lower = context.toLowerCase();

            if (lower.contains("redstone") || lower.contains("circuit")) return "redstone";
            if (lower.contains("build") || lower.contains("construct")) return "building";
            if (lower.contains("mine") || lower.contains("dig")) return "mining";
            if (lower.contains("farm") || lower.contains("crop")) return "farming";
            if (lower.contains("craft")) return "crafting";

            return "general";
        }

        public void improveSkill(String context) {
            String category = extractSkillCategory(context);
            SkillLevel current = getSkillLevel(context);

            if (current.ordinal() < SkillLevel.values().length - 1) {
                skills.put(category, SkillLevel.values()[current.ordinal() + 1]);
                LOGGER.debug("Worker '{}' improved skill '{}' to {}",
                    name, category, skills.get(category));
            }
        }

        public void trackMistake(String context) {
            String key = "mistake:" + context;
            int count = taskHistory.getOrDefault(key, 0) + 1;

            if (count >= 3) {
                repeatedMistakes.add(context);
            }

            taskHistory.put(key, count);
        }

        public boolean hasRepeatedMistake(String context) {
            return repeatedMistakes.contains(context);
        }

        public void recordSuccess(String context) {
            String category = extractSkillCategory(context);
            String key = category + "_success";
            int count = taskHistory.getOrDefault(key, 0) + 1;
            taskHistory.put(key, count);

            // Improve skill every 3 successes
            if (count % 3 == 0) {
                improveSkill(category);
            }
        }

        public int getSuccessCount(String context) {
            String category = extractSkillCategory(context);
            return taskHistory.getOrDefault(category + "_success", 0);
        }

        public boolean hasImprovedSpeed(String context) {
            String key = extractSkillCategory(context) + "_fast";
            return taskHistory.getOrDefault(key, 0) > 1;
        }

        public boolean hasImprovedQuality(String context) {
            String key = extractSkillCategory(context) + "_quality";
            return taskHistory.getOrDefault(key, 0) > 1;
        }

        public boolean recentlyCompletedWithoutHelp(String context) {
            String key = extractSkillCategory(context) + "_independent";
            return taskHistory.getOrDefault(key, 0) > 0;
        }

        public void recordIndependentCompletion(String context) {
            String key = extractSkillCategory(context) + "_independent";
            taskHistory.put(key, taskHistory.getOrDefault(key, 0) + 1);
        }

        public boolean usedCreativeApproach(String context) {
            return taskHistory.containsKey(extractSkillCategory(context) + "_creative");
        }

        // Getters and setters
        public String getName() { return name; }
        public String getRole() { return role; }
        public int getRapportLevel() { return rapportLevel; }
        public void setRapportLevel(int level) { this.rapportLevel = Math.max(0, Math.min(100, level)); }
        public double getStressLevel() { return stressLevel; }
        public void setStressLevel(double level) { this.stressLevel = Math.max(0.0, Math.min(1.0, level)); }
        public double getFocusLevel() { return focusLevel; }
        public void setFocusLevel(double level) { this.focusLevel = Math.max(0.0, Math.min(1.0, level)); }

        public void saveToNBT(CompoundTag tag) {
            tag.putString("Name", name);
            tag.putString("Role", role);
            tag.putInt("Rapport", rapportLevel);
            tag.putDouble("Stress", stressLevel);
            tag.putDouble("Focus", focusLevel);

            // Save skills
            CompoundTag skillsTag = new CompoundTag();
            for (Map.Entry<String, SkillLevel> entry : skills.entrySet()) {
                skillsTag.putInt(entry.getKey(), entry.getValue().ordinal());
            }
            tag.put("Skills", skillsTag);

            // Save task history
            CompoundTag historyTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : taskHistory.entrySet()) {
                historyTag.putInt(entry.getKey(), entry.getValue());
            }
            tag.put("TaskHistory", historyTag);

            // Save repeated mistakes
            ListTag mistakesTag = new net.minecraft.nbt.ListTag();
            for (String mistake : repeatedMistakes) {
                mistakesTag.add(net.minecraft.nbt.StringTag.valueOf(mistake));
            }
            tag.put("RepeatedMistakes", mistakesTag);
        }

        public static WorkerProfile loadFromNBT(CompoundTag tag) {
            String name = tag.getString("Name");
            String role = tag.getString("Role");
            WorkerProfile profile = new WorkerProfile(name, role);

            profile.rapportLevel = tag.getInt("Rapport");
            profile.stressLevel = tag.getDouble("Stress");
            profile.focusLevel = tag.getDouble("Focus");

            // Load skills
            if (tag.contains("Skills")) {
                CompoundTag skillsTag = tag.getCompound("Skills");
                for (String key : skillsTag.getAllKeys()) {
                    int level = skillsTag.getInt(key);
                    profile.skills.put(key, SkillLevel.values()[level]);
                }
            }

            // Load task history
            if (tag.contains("TaskHistory")) {
                CompoundTag historyTag = tag.getCompound("TaskHistory");
                for (String key : historyTag.getAllKeys()) {
                    profile.taskHistory.put(key, historyTag.getInt(key));
                }
            }

            // Load repeated mistakes
            if (tag.contains("RepeatedMistakes")) {
                net.minecraft.nbt.ListTag mistakesTag = tag.getList("RepeatedMistakes", 8);
                for (int i = 0; i < mistakesTag.size(); i++) {
                    profile.repeatedMistakes.add(mistakesTag.getString(i));
                }
            }

            return profile;
        }
    }

    /**
     * Foreman's personality for teaching interactions.
     */
    public static class MentorshipPersonality {
        private boolean admitsUncertainty;
        private double willingnessToLearnFromWorkers;

        public MentorshipPersonality() {
            this.admitsUncertainty = true;
            this.willingnessToLearnFromWorkers = 0.7;
        }

        public boolean shouldAdmitUncertainty(String context) {
            return admitsUncertainty && (
                context.toLowerCase().contains("new") ||
                context.toLowerCase().contains("experimental") ||
                context.toLowerCase().contains("first time") ||
                context.toLowerCase().contains("never")
            );
        }

        public boolean shouldAskWorker(String context) {
            return willingnessToLearnFromWorkers > 0.5 &&
                Math.random() < willingnessToLearnFromWorkers;
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
