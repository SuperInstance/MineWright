package com.minewright.dialogue;

import com.minewright.config.MineWrightConfig;
import com.minewright.entity.ForemanEntity;
import com.minewright.memory.CompanionMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Manages proactive dialogue for MineWright entities, making them feel alive
 * by commenting on game events without being prompted.
 *
 * <p>This system tracks various triggers and generates contextual comments
 * based on the environment, time, relationship with the player, and recent
 * events.</p>
 *
 * <p><b>Design Principles:</b></p>
 * <ul>
 *   <li>Non-blocking: Uses async LLM calls</li>
 *   <li>Respects cooldowns: Doesn't spam comments</li>
 *   <li>Context-aware: Comments fit the situation</li>
 *   <li>Personality-driven: Uses CompanionMemory for consistent voice</li>
 *   <li>Relationship-scaled: Higher rapport = more chatty</li>
 *   <li>Logged: All dialogue decisions tracked for analysis</li>
 * </ul>
 *
 * <p><b>Refactored Architecture (Wave 48):</b></p>
 * <ul>
 *   <li>{@link DialogueTriggerChecker} - Checks for dialogue triggers</li>
 *   <li>{@link DialogueCommentGenerator} - Generates dialogue content</li>
 *   <li>{@link DialogueSpeechPatternManager} - Manages speech patterns</li>
 *   <li>{@link DialogueAnalytics} - Tracks analytics and statistics</li>
 * </ul>
 *
 * @since 1.3.0
 */
public class ProactiveDialogueManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProactiveDialogueManager.class);
    private static final Logger DIALOGUE_LOGGER = LoggerFactory.getLogger("com.minewright.dialogue");

    private final ForemanEntity minewright;
    private final CompanionMemory memory;

    // Delegated components
    private final DialogueTriggerChecker triggerChecker;
    private final DialogueCommentGenerator commentGenerator;
    private final DialogueSpeechPatternManager speechPatternManager;
    private final DialogueAnalytics analytics;

    private final Random random;

    // Configuration
    private final boolean enabled;
    private final int baseCheckInterval;  // Ticks between trigger checks
    private final int baseCooldownTicks;  // Minimum ticks between comments

    // State tracking
    private int ticksSinceLastCheck = 0;
    private int ticksSinceLastComment = 0;
    private long lastCommentTimestamp = 0;
    private String lastCommentType = null;

    /**
     * Creates a new ProactiveDialogueManager for a MineWright entity.
     *
     * @param minewright The MineWright entity
     */
    public ProactiveDialogueManager(ForemanEntity minewright) {
        this.minewright = minewright;
        this.memory = minewright.getCompanionMemory();
        this.random = new Random();

        // Initialize delegated components
        this.triggerChecker = new DialogueTriggerChecker(minewright, memory);
        this.commentGenerator = new DialogueCommentGenerator(minewright, memory);
        this.speechPatternManager = new DialogueSpeechPatternManager(memory);
        this.analytics = new DialogueAnalytics();

        // Load configuration - MINIMAL CHATTER MODE
        // User preference: conversations should be useful, not constant banter
        this.enabled = true;
        this.baseCheckInterval = 600;  // Check every 30 seconds (was 5 seconds)
        this.baseCooldownTicks = 6000;  // Minimum 5 minutes between comments (was 30 seconds)

        LOGGER.info("ProactiveDialogueManager initialized (minimal chatter mode) for '{}'",
            minewright.getEntityName());
    }

    /**
     * Called every tick to check for proactive dialogue triggers.
     * This is lightweight and only does expensive work periodically.
     *
     * REDUCED CHATTER: Only checks important triggers now.
     */
    public void tick() {
        if (!enabled) {
            return;
        }

        // Only run checks periodically (not every tick)
        ticksSinceLastCheck++;
        ticksSinceLastComment++;

        if (ticksSinceLastCheck < baseCheckInterval) {
            return;
        }

        ticksSinceLastCheck = 0;

        // ONLY check IMPORTANT triggers - no idle chatter
        // Removed: checkTimeBasedTriggers(), checkWeatherTriggers(), checkPlayerProximityTriggers()
        // These just add noise. Keep only danger and action completion triggers.
        DialogueTriggerChecker.TriggerCheckResult result = triggerChecker.checkContextBasedTriggers();
        if (result.shouldTrigger) {
            triggerComment(result.triggerType, result.context);
        }
    }

    /**
     * Triggers a proactive comment based on the given trigger type.
     * Now includes context-aware selection, relationship-based dialogue,
     * speech pattern tracking, and comprehensive logging.
     *
     * @param triggerType The type of trigger
     * @param context Additional context about the trigger
     */
    private void triggerComment(String triggerType, String context) {
        if (!enabled) {
            return;
        }

        // Get relationship context for decision making
        int rapport = memory.getRapportLevel();
        int rapportFactor = rapport / 25;  // 0-4, higher = more chatty

        // Calculate trigger chance based on multiple factors
        double baseChance = 0.3;
        double rapportBonus = rapportFactor * 0.1;  // Up to +40% for high rapport
        double contextModifier = getContextModifier(triggerType, context);
        double speechPatternPenalty = speechPatternManager.getSpeechPatternPenalty(triggerType);

        double finalChance = baseChance + rapportBonus + contextModifier - speechPatternPenalty;
        finalChance = Math.max(0.1, Math.min(0.9, finalChance)); // Clamp between 10% and 90%

        boolean shouldTrigger = random.nextDouble() < finalChance;

        // Log the decision
        DialogueAnalytics.DialogueDecision decision = new DialogueAnalytics.DialogueDecision(
            triggerType, context, shouldTrigger, finalChance, rapport, Instant.now()
        );
        analytics.recordDecision(decision);
        DIALOGUE_LOGGER.debug("Dialogue decision: {} - Chance: {:.2f}, Triggered: {}",
            triggerType, finalChance, shouldTrigger);

        if (!shouldTrigger) {
            analytics.recordSkipped();
            LOGGER.debug("Skipping trigger {} - final chance: {:.2f}", triggerType, finalChance);
            return;
        }

        // Check if same comment type recently
        if (triggerType.equals(lastCommentType) && ticksSinceLastComment < baseCooldownTicks) {
            analytics.recordSkipped();
            LOGGER.debug("Skipping trigger {} - too soon after same type", triggerType);
            return;
        }

        // Check if this phrase was used too recently
        if (speechPatternManager.isPhraseTooRecent(triggerType)) {
            analytics.recordSkipped();
            LOGGER.debug("Skipping trigger {} - phrase used too recently", triggerType);
            return;
        }

        // Record trigger
        lastCommentType = triggerType;
        lastCommentTimestamp = System.currentTimeMillis();
        ticksSinceLastComment = 0;
        analytics.recordTriggered();

        // Track speech pattern
        speechPatternManager.trackSpeechPattern(triggerType);

        // Generate comment with relationship-aware context
        generateAndSpeakComment(triggerType, context, rapport);
    }

    /**
     * Gets a context modifier for the trigger chance based on the situation.
     */
    private double getContextModifier(String triggerType, String context) {
        // Important triggers get higher priority
        if (triggerType.equals("milestone") || triggerType.equals("low_health")) {
            return 0.3; // +30% chance
        }
        if (triggerType.equals("player_approach") || triggerType.equals("task_complete")) {
            return 0.15; // +15% chance
        }
        return 0.0;
    }

    /**
     * Generates a comment and sends it to nearby players.
     * Uses delegated components for comment generation and speech patterns.
     *
     * @param triggerType The type of trigger
     * @param context Context about what happened
     * @param rapport Current rapport level with player
     */
    private void generateAndSpeakComment(String triggerType, String context, int rapport) {
        CompletableFuture<String> commentFuture = commentGenerator.generateComment(triggerType, context, rapport);

        // Fallback to static comments if LLM fails, with relationship-aware selection
        commentFuture.thenAccept(comment -> {
            String finalComment = comment;

            // Use fallback if LLM returned null or empty
            if (finalComment == null || finalComment.trim().isEmpty()) {
                finalComment = commentGenerator.getRelationshipAwareFallback(triggerType, rapport);
            }

            // Apply speech pattern transformation
            finalComment = speechPatternManager.applySpeechPattern(finalComment, triggerType);

            if (finalComment != null && !finalComment.isEmpty()) {
                minewright.sendChatMessage(finalComment);
                DIALOGUE_LOGGER.info("Dialogue: [{}] {} - Rapport: {}",
                    triggerType, finalComment, rapport);
            }
        }).exceptionally(error -> {
            // Use fallback on error
            String fallback = commentGenerator.getRelationshipAwareFallback(triggerType, rapport);
            if (fallback != null) {
                fallback = speechPatternManager.applySpeechPattern(fallback, triggerType);
                minewright.sendChatMessage(fallback);
                DIALOGUE_LOGGER.warn("LLM failed, used fallback: [{}] {} - Error: {}",
                    triggerType, fallback, error.getMessage());
            }
            return null;
        });
    }

    /**
     * Called when a task is completed.
     *
     * @param taskDescription What task was completed
     */
    public void onTaskCompleted(String taskDescription) {
        if (triggerChecker.canTrigger("task_complete", 400)) {
            triggerChecker.recordTrigger("task_complete");
            triggerComment("task_complete", "Completed: " + taskDescription);
        }
    }

    /**
     * Called when a task fails.
     *
     * @param taskDescription What task failed
     * @param reason Why it failed
     */
    public void onTaskFailed(String taskDescription, String reason) {
        if (triggerChecker.canTrigger("task_failed", 600)) {
            triggerChecker.recordTrigger("task_failed");
            triggerComment("task_failed", "Failed: " + taskDescription + " - " + reason);
        }
    }

    /**
     * Called when MineWright gets stuck during a task.
     *
     * @param taskDescription The task MineWright is stuck on
     */
    public void onTaskStuck(String taskDescription) {
        if (triggerChecker.canTrigger("task_stuck", 800)) {
            triggerChecker.recordTrigger("task_stuck");
            triggerComment("task_stuck", "Stuck on: " + taskDescription);
        }
    }

    /**
     * Called when a milestone is reached.
     *
     * @param milestone The milestone description
     */
    public void onMilestoneReached(String milestone) {
        if (triggerChecker.canTrigger("milestone", 2000)) {
            triggerChecker.recordTrigger("milestone");
            triggerComment("milestone", "Milestone: " + milestone);
        }
    }

    /**
     * Gets the number of ticks since the last comment.
     *
     * @return Ticks since last comment
     */
    public int getTicksSinceLastComment() {
        return ticksSinceLastComment;
    }

    /**
     * Checks if proactive dialogue is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Forces a comment to be sent immediately (bypasses cooldowns).
     * Used for important events that should always be commented on.
     *
     * @param triggerType The type of trigger
     * @param context Context about the event
     */
    public void forceComment(String triggerType, String context) {
        lastCommentType = triggerType;
        lastCommentTimestamp = System.currentTimeMillis();
        ticksSinceLastComment = 0;
        analytics.recordTriggered();
        speechPatternManager.trackSpeechPattern(triggerType);

        generateAndSpeakComment(triggerType, context, memory.getRapportLevel());

        DIALOGUE_LOGGER.info("Forced dialogue: [{}] {}", triggerType, context);
    }

    // === Dialogue Analytics ===

    /**
     * Gets the dialogue history for analysis.
     *
     * @return Unmodifiable list of dialogue decisions
     */
    public List<DialogueAnalytics.DialogueDecision> getDialogueHistory() {
        return analytics.getDialogueHistory();
    }

    /**
     * Gets statistics about dialogue usage.
     *
     * @return DialogueStatistics object with usage metrics
     */
    public DialogueAnalytics.DialogueStatistics getStatistics() {
        return analytics.getStatistics(speechPatternManager.getPhraseUsageCount());
    }

    /**
     * Gets the most commonly used dialogue triggers.
     *
     * @param limit Maximum number of results
     * @return List of trigger types sorted by usage frequency
     */
    public List<Map.Entry<String, Integer>> getMostUsedTriggers(int limit) {
        return speechPatternManager.getPhraseUsageCount().entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(limit)
            .toList();
    }

    /**
     * Clears dialogue history (for testing/debugging).
     */
    public void clearHistory() {
        analytics.clearHistory();
        speechPatternManager.clearHistory();
        DIALOGUE_LOGGER.debug("Dialogue history cleared");
    }
}
