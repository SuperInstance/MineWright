package com.minewright.mentorship;

import com.minewright.entity.ForemanEntity;
import com.minewright.mentorship.MentorshipModels.*;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
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
    private final Set<String> taughtConcepts;

    // Delegated components
    private final TeachingMomentDetector teachingMomentDetector;
    private final MentorshipDialogueGenerator dialogueGenerator;

    /**
     * Creates a new MentorshipManager for the given foreman.
     *
     * @param foreman The foreman entity who will be the mentor
     */
    public MentorshipManager(ForemanEntity foreman) {
        this.foreman = foreman;
        this.workers = new ConcurrentHashMap<>();
        this.taughtConcepts = ConcurrentHashMap.newKeySet();

        // Initialize delegated components
        this.dialogueGenerator = new MentorshipDialogueGenerator();
        this.teachingMomentDetector = new TeachingMomentDetector();

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
        taughtConcepts.add(workerName);

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

        return teachingMomentDetector.detectTeachingMoment(
            worker, triggerType, context, dialogueGenerator, dialogueGenerator);
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
        SkillLevel taskLevel = TaskDifficultyEstimator.estimateDifficulty(taskContext);

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

        return dialogueGenerator.generatePraise(workerName, completion);
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
            taughtConcepts.add(workerName);
        }

        LOGGER.info("Loaded mentorship data for {} workers", workers.size());
    }
}
