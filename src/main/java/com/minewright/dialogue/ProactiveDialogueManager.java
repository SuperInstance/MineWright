package com.minewright.dialogue;

import com.minewright.config.MineWrightConfig;
import com.minewright.entity.ForemanEntity;
import com.minewright.llm.CompanionPromptBuilder;
import com.minewright.llm.TaskPlanner;
import com.minewright.llm.async.AsyncLLMClient;
import com.minewright.llm.async.AsyncGroqClient;
import com.minewright.llm.batch.BatchingLLMClient;
import com.minewright.llm.batch.PromptBatcher;
import com.minewright.memory.CompanionMemory;
import com.minewright.memory.ConversationManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
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
 * </ul>
 *
 * @since 1.3.0
 */
public class ProactiveDialogueManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProactiveDialogueManager.class);

    private final ForemanEntity minewright;
    private final CompanionMemory memory;
    private final ConversationManager conversationManager;
    private final AsyncLLMClient llmClient;
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
    private long lastGreetingTime = 0;
    private Player lastGreetedPlayer = null;
    private boolean wasRaining = false;
    private long lastWeatherCheckTime = 0;

    // Cooldown tracking per trigger type
    private final Map<String, Long> triggerCooldowns = new HashMap<>();

    // Static fallback comments (used when LLM is unavailable)
    private static final Map<String, String[]> FALLBACK_COMMENTS = Map.of(
        "morning", new String[]{
            "Good morning! Ready to get to work?",
            "Morning! Let's make today productive.",
            "Early bird gets the blocks!"
        },
        "night", new String[]{
            "Getting dark. Maybe we should wrap up soon?",
            "Night's falling. Hope you have a shelter ready!",
            "Watch out for the creepers tonight!"
        },
        "raining", new String[]{
            "Rain's coming down. Perfect weather for mining underground!",
            "Nice day for a break, isn't it?",
            "The crops will love this rain!"
        },
        "storm", new String[]{
            "Thunder and lightning! Stay safe out there!",
            "Nasty weather. Let's take cover!"
        },
        "idle_long", new String[]{
            "Everything quiet today?",
            "Let me know if you need anything done!",
            "Standing by, ready to help!"
        },
        "near_danger", new String[]{
            "Be careful around here!",
            "This area looks dangerous.",
            "Stay close, I've got your back!"
        },
        "task_complete", new String[]{
            "Another job well done!",
            "That went smoothly!",
            "Nice work!"
        }
    );

    /**
     * Creates a new ProactiveDialogueManager for a MineWright entity.
     *
     * @param minewright The MineWright entity
     */
    public ProactiveDialogueManager(ForemanEntity minewright) {
        this.minewright = minewright;
        this.memory = minewright.getCompanionMemory();
        this.conversationManager = new ConversationManager(minewright);
        this.random = new Random();

        // Load configuration
        this.enabled = true;  // Could add config option later
        this.baseCheckInterval = 100;  // Check every 5 seconds
        this.baseCooldownTicks = 600;  // Minimum 30 seconds between comments

        // Initialize LLM client for generating comments
        // Use Groq for fast, free responses
        this.llmClient = new AsyncGroqClient(
            MineWrightConfig.OPENAI_API_KEY.get(),
            "llama-3.1-8b-instant",
            150,  // Brief comments
            0.9   // High creativity
        );

        LOGGER.info("ProactiveDialogueManager initialized for MineWright '{}'",
            minewright.getEntityName());
    }

    /**
     * Called every tick to check for proactive dialogue triggers.
     * This is lightweight and only does expensive work periodically.
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

        // Check triggers
        checkTimeBasedTriggers();
        checkContextBasedTriggers();
        checkWeatherTriggers();
        checkPlayerProximityTriggers();
        checkActionStateTriggers();
    }

    /**
     * Checks time-based triggers (morning, night, idle too long).
     */
    private void checkTimeBasedTriggers() {
        Level level = minewright.level();
        long dayTime = level.getDayTime() % 24000;

        // Morning greeting (6:00 AM game time)
        if (dayTime >= 0 && dayTime < 2000) {
            if (canTrigger("morning", 24000)) {  // Once per day
                triggerComment("morning", "It's morning!");
            }
        }

        // Night warning (8:00 PM game time)
        if (dayTime >= 18000 && dayTime < 20000) {
            if (canTrigger("night", 12000)) {  // Once per evening
                triggerComment("night", "It's getting dark!");
            }
        }

        // Idle comment (if idle for too long)
        if (!minewright.getActionExecutor().isExecuting() && ticksSinceLastComment > 1200) {
            if (canTrigger("idle_long", 6000)) {  // Every 5 minutes
                triggerComment("idle_long", "Been idle a while");
            }
        }
    }

    /**
     * Checks context-based triggers (biome, location, danger).
     */
    private void checkContextBasedTriggers() {
        if (minewright.level().isClientSide) {
            return;
        }

        // Check biome
        try {
            String biome = minewright.level().getBiome(minewright.blockPosition()).unwrapKey()
                .map(key -> key.location().getPath())
                .orElse("unknown");

            // Special biomes
            if (biome.contains("nether")) {
                if (canTrigger("nether_biome", 3000)) {
                    triggerComment("nether_biome", "Entered the Nether");
                }
            } else if (biome.contains("end")) {
                if (canTrigger("end_biome", 3000)) {
                    triggerComment("end_biome", "Entered the End");
                }
            } else if (biome.contains("snow") || biome.contains("ice")) {
                if (canTrigger("cold_biome", 6000)) {
                    triggerComment("cold_biome", "Entered cold biome");
                }
            } else if (biome.contains("desert")) {
                if (canTrigger("desert_biome", 6000)) {
                    triggerComment("desert_biome", "Entered desert");
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error checking biome for proactive dialogue", e);
        }

        // Check for danger (low health, nearby hostile)
        if (minewright.getHealth() < minewright.getMaxHealth() * 0.3) {
            if (canTrigger("low_health", 2000)) {
                triggerComment("low_health", "Health is low!");
            }
        }
    }

    /**
     * Checks weather-related triggers.
     */
    private void checkWeatherTriggers() {
        Level level = minewright.level();

        // Check weather occasionally
        long now = System.currentTimeMillis();
        if (now - lastWeatherCheckTime < 5000) {
            return;
        }
        lastWeatherCheckTime = now;

        boolean isRaining = level.isRaining();
        boolean isThundering = level.isThundering();

        // Rain started
        if (isRaining && !wasRaining) {
            if (isThundering) {
                if (canTrigger("storm", 10000)) {
                    triggerComment("storm", "Storm started!");
                }
            } else {
                if (canTrigger("raining", 15000)) {
                    triggerComment("raining", "Rain started!");
                }
            }
        }

        wasRaining = isRaining;
    }

    /**
     * Checks player proximity triggers (greeting, approach).
     */
    private void checkPlayerProximityTriggers() {
        Level level = minewright.level();
        if (level.isClientSide) {
            return;
        }

        // Find nearest player
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : level.players()) {
            double dist = minewright.position().distanceTo(player.position());
            if (dist < nearestDistance) {
                nearestDistance = dist;
                nearestPlayer = player;
            }
        }

        if (nearestPlayer == null) {
            return;
        }

        // Greet when player approaches (within 10 blocks)
        if (nearestDistance < 10.0) {
            long now = System.currentTimeMillis();

            // Check if we should greet
            boolean shouldGreet = false;
            if (lastGreetedPlayer == null || lastGreetedPlayer != nearestPlayer) {
                shouldGreet = true;
            } else if (now - lastGreetingTime > 60000) {  // 1 minute cooldown
                shouldGreet = true;
            }

            if (shouldGreet) {
                lastGreetedPlayer = nearestPlayer;
                lastGreetingTime = now;

                // Initialize relationship if needed
                if (memory.getPlayerName() == null) {
                    memory.initializeRelationship(nearestPlayer.getName().getString());
                }

                triggerComment("player_approach", "Player approached");
            }
        }
    }

    /**
     * Checks action state triggers (task complete, stuck, etc.).
     */
    private void checkActionStateTriggers() {
        // This is called from ActionExecutor callbacks
        // See onTaskCompleted() and onTaskFailed() methods below
    }

    /**
     * Triggers a proactive comment based on the given trigger type.
     *
     * @param triggerType The type of trigger
     * @param context Additional context about the trigger
     */
    private void triggerComment(String triggerType, String context) {
        if (!enabled) {
            return;
        }

        // Check rapport-based frequency
        int rapport = memory.getRapportLevel();
        int rapportFactor = rapport / 25;  // 0-4, higher = more chatty

        // Random factor to avoid predictability
        double chance = 0.3 + (rapportFactor * 0.1);  // 30% to 70% base chance
        if (random.nextDouble() > chance) {
            LOGGER.debug("Skipping trigger {} due to random chance", triggerType);
            return;
        }

        // Check if same comment type recently
        if (triggerType.equals(lastCommentType) && ticksSinceLastComment < baseCooldownTicks) {
            LOGGER.debug("Skipping trigger {} - too soon after same type", triggerType);
            return;
        }

        // Record trigger
        lastCommentType = triggerType;
        lastCommentTimestamp = System.currentTimeMillis();
        ticksSinceLastComment = 0;

        // Generate comment
        generateAndSpeakComment(triggerType, context);
    }

    /**
     * Generates a comment and sends it to nearby players.
     *
     * @param triggerType The type of trigger
     * @param context Context about what happened
     */
    private void generateAndSpeakComment(String triggerType, String context) {
        // Try using batching system for background comments
        TaskPlanner taskPlanner = minewright.getActionExecutor().getTaskPlanner();
        BatchingLLMClient batchingClient = taskPlanner != null ? taskPlanner.getBatchingClient() : null;

        CompletableFuture<String> commentFuture;

        if (batchingClient != null) {
            // Use batching system for background comments (lower priority, rate-limit aware)
            Map<String, Object> batchContext = new HashMap<>();
            batchContext.put("triggerType", triggerType);
            batchContext.put("minewrightName", minewright.getEntityName());
            batchContext.put("rapport", memory.getRapportLevel());

            // Submit as background prompt (aggressive batching, lower priority)
            commentFuture = batchingClient.submit(
                buildProactivePrompt(triggerType, context),
                PromptBatcher.PromptType.BACKGROUND,
                batchContext
            );
        } else {
            // Fall back to direct LLM call
            commentFuture = conversationManager.generateProactiveComment(context, llmClient);
        }

        // Fallback to static comments if LLM fails
        commentFuture.thenAccept(comment -> {
            String finalComment = comment;

            // Use fallback if LLM returned null or empty
            if (finalComment == null || finalComment.trim().isEmpty()) {
                finalComment = getFallbackComment(triggerType);
            }

            if (finalComment != null && !finalComment.isEmpty()) {
                minewright.sendChatMessage(finalComment);
                LOGGER.debug("Proactive comment [{}]: {}", triggerType, finalComment);
            }
        }).exceptionally(error -> {
            // Use fallback on error
            String fallback = getFallbackComment(triggerType);
            if (fallback != null) {
                minewright.sendChatMessage(fallback);
            }
            LOGGER.debug("LLM comment generation failed, used fallback: {}", error.getMessage());
            return null;
        });
    }

    /**
     * Builds a prompt for proactive comment generation.
     */
    private String buildProactivePrompt(String triggerType, String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a brief, ");
        prompt.append(getToneForTrigger(triggerType));
        prompt.append(" comment for situation: ");
        prompt.append(context);
        prompt.append(". MineWright's name is ");
        prompt.append(minewright.getEntityName());
        prompt.append(". Keep it under 15 words. Be natural and ");
        prompt.append(memory.getPersonality().extraversion > 60 ? "outgoing" : "reserved");
        prompt.append(".");
        return prompt.toString();
    }

    /**
     * Gets the appropriate tone for a trigger type.
     */
    private String getToneForTrigger(String triggerType) {
        return switch (triggerType) {
            case "morning" -> "cheerful, morning-greeting";
            case "night" -> "cautious, helpful";
            case "raining" -> "observational, casual";
            case "storm" -> "concerned, urgent";
            case "idle_long" -> "helpful, slightly bored";
            case "near_danger" -> "alert, protective";
            case "task_complete" -> "satisfied, proud";
            case "task_failed" -> "encouraging, optimistic";
            case "milestone" -> "celebratory, warm";
            default -> "friendly";
        };
    }

    /**
     * Gets a fallback comment from the static pool.
     *
     * @param triggerType The type of trigger
     * @return A fallback comment, or null if none available
     */
    private String getFallbackComment(String triggerType) {
        String[] comments = FALLBACK_COMMENTS.get(triggerType);
        if (comments != null && comments.length > 0) {
            return comments[random.nextInt(comments.length)];
        }
        return null;
    }

    /**
     * Checks if a trigger can fire based on cooldown.
     *
     * @param triggerType The trigger to check
     * @param cooldownTicks Minimum ticks between triggers
     * @return true if the trigger can fire
     */
    private boolean canTrigger(String triggerType, int cooldownTicks) {
        Long lastTrigger = triggerCooldowns.get(triggerType);
        if (lastTrigger == null) {
            return true;
        }

        long ticksSinceTrigger = ticksSinceLastComment;
        return ticksSinceTrigger >= cooldownTicks;
    }

    /**
     * Records that a trigger has fired.
     *
     * @param triggerType The trigger that fired
     */
    private void recordTrigger(String triggerType) {
        triggerCooldowns.put(triggerType, System.currentTimeMillis());
    }

    /**
     * Called when a task is completed.
     *
     * @param taskDescription What task was completed
     */
    public void onTaskCompleted(String taskDescription) {
        if (canTrigger("task_complete", 400)) {
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
        if (canTrigger("task_failed", 600)) {
            triggerComment("task_failed", "Failed: " + taskDescription + " - " + reason);
        }
    }

    /**
     * Called when MineWright gets stuck during a task.
     *
     * @param taskDescription The task MineWright is stuck on
     */
    public void onTaskStuck(String taskDescription) {
        if (canTrigger("task_stuck", 800)) {
            triggerComment("task_stuck", "Stuck on: " + taskDescription);
        }
    }

    /**
     * Called when a milestone is reached.
     *
     * @param milestone The milestone description
     */
    public void onMilestoneReached(String milestone) {
        if (canTrigger("milestone", 2000)) {
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
        generateAndSpeakComment(triggerType, context);
    }
}
