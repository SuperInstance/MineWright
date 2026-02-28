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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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
 * @since 1.3.0
 */
public class ProactiveDialogueManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProactiveDialogueManager.class);
    private static final Logger DIALOGUE_LOGGER = LoggerFactory.getLogger("com.minewright.dialogue");

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

    // Dialogue decision logging
    private final List<DialogueDecision> dialogueHistory;
    private int totalDialoguesTriggered = 0;
    private int totalDialoguesSkipped = 0;

    // Speech pattern tracking for personality consistency
    private final Map<String, Integer> phraseUsageCount;
    private final Queue<String> recentPhrases;

    // Static fallback comments (used when LLM is unavailable)
    // Expanded with more variety and personality-driven options
    private static final Map<String, String[]> FALLBACK_COMMENTS = Map.of(
        "morning", new String[]{
            "Good morning! Ready to get to work?",
            "Morning! Let's make today productive.",
            "Early bird gets the blocks!",
            "Rise and shine! Another day to build!",
            "Nice morning weather for mining.",
            "Coffee? No? Just blocks then.",
            "The sun's up, time to build up!"
        },
        "night", new String[]{
            "Getting dark. Maybe we should wrap up soon?",
            "Night's falling. Hope you have a shelter ready!",
            "Watch out for the creepers tonight!",
            "Darkness incoming. Stay safe out there.",
            "Time to find cover. The mobs will be waking up.",
            "Night falls. Good time for indoor work.",
            "Keep your sword close tonight."
        },
        "raining", new String[]{
            "Rain's coming down. Perfect weather for mining underground!",
            "Nice day for a break, isn't it?",
            "The crops will love this rain!",
            "Wet weather. At least the fire won't spread.",
            "Rain again? Time to go below ground.",
            "Perfect mining weather. Nobody underground minds rain.",
            "Water from above, blocks below."
        },
        "storm", new String[]{
            "Thunder and lightning! Stay safe out there!",
            "Nasty weather. Let's take cover!",
            "Lightning! Definitely stay under cover now.",
            "The sky's angry. We should be indoors.",
            "Storm's brewing. Not the time to be climbing trees.",
            "Thunder! Hope nothing gets struck.",
            "When lightning strikes, we take shelter."
        },
        "idle_long", new String[]{
            "Everything quiet today?",
            "Let me know if you need anything done!",
            "Standing by, ready to help!",
            "Slow day? Could use a rest.",
            "Been a while. Got any projects in mind?",
            "Quiet... too quiet. Just kidding, or am I?",
            "Ready when you are. Always ready.",
            "Taking a break? I can do breaks."
        },
        "near_danger", new String[]{
            "Be careful around here!",
            "This area looks dangerous.",
            "Stay close, I've got your back!",
            "Something feels off about this place...",
            "Watch your step. Danger nearby.",
            "I don't like the look of this.",
            "Better stay alert. Trouble's close.",
            "Keep your eyes open. This isn't safe."
        },
        "task_complete", new String[]{
            "Another job well done!",
            "That went smoothly!",
            "Nice work!",
            "And that's how it's done!",
            "Task complete. What's next?",
            "Look at that. Perfection.",
            "One more thing crossed off the list.",
            "Smooth sailing on that one.",
            "Done and dusted!",
            "Results are in: we succeeded."
        },
        "task_failed", new String[]{
            "That didn't go as planned.",
            "Well, that's unfortunate.",
            "Let me try a different approach.",
            "Failed. But we learn from failures.",
            "Not ideal. Shall we try again?",
            "Hmm, that method didn't work.",
            "Back to the drawing board on that one.",
            "Every failure is a step to success."
        },
        "milestone", new String[]{
            "We did it! Together!",
            "Now THIS is worth celebrating!",
            "I'll remember this moment.",
            "What a journey this has been!",
            "We're really making progress!",
            "Moments like these make it all worth it.",
            "Here's to us and what we've built!"
        }
    );

    // Context-aware dialogue patterns based on relationship level
    private static final Map<String, String[]> RELATIONSHIP_DIALOGUES = Map.of(
        "low_rapport", new String[]{
            "Let me know if you need help.",
            "I'm here to assist.",
            "Just tell me what to build.",
            "Standing by for instructions."
        },
        "medium_rapport", new String[]{
            "Glad to help you out!",
            "We make a good team.",
            "Looking forward to our next project!",
            "Nice working with you."
        },
        "high_rapport", new String[]{
            "We're unstoppable together!",
            "I've got your back, friend!",
            "Nothing we can't handle as a team!",
            "Best partner I could ask for!"
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

        // Initialize dialogue history tracking
        this.dialogueHistory = new ArrayList<>();
        this.phraseUsageCount = new ConcurrentHashMap<>();
        this.recentPhrases = new LinkedList<>();

        // Load configuration - MINIMAL CHATTER MODE
        // User preference: conversations should be useful, not constant banter
        this.enabled = true;
        this.baseCheckInterval = 600;  // Check every 30 seconds (was 5 seconds)
        this.baseCooldownTicks = 6000;  // Minimum 5 minutes between comments (was 30 seconds)

        // Initialize LLM client for generating comments (but rarely used now)
        // Use Groq for fast, free responses
        this.llmClient = new AsyncGroqClient(
            MineWrightConfig.OPENAI_API_KEY.get(),
            "llama-3.1-8b-instant",
            150,  // Brief comments
            0.9   // High creativity
        );

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
        checkContextBasedTriggers();  // For danger warnings
        checkActionStateTriggers();   // For task-related updates
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
        double speechPatternPenalty = getSpeechPatternPenalty(triggerType);

        double finalChance = baseChance + rapportBonus + contextModifier - speechPatternPenalty;
        finalChance = Math.max(0.1, Math.min(0.9, finalChance)); // Clamp between 10% and 90%

        boolean shouldTrigger = random.nextDouble() < finalChance;

        // Log the decision
        DialogueDecision decision = new DialogueDecision(
            triggerType, context, shouldTrigger, finalChance, rapport, Instant.now()
        );
        dialogueHistory.add(decision);
        DIALOGUE_LOGGER.debug("Dialogue decision: {} - Chance: {:.2f}, Triggered: {}",
            triggerType, finalChance, shouldTrigger);

        if (!shouldTrigger) {
            totalDialoguesSkipped++;
            LOGGER.debug("Skipping trigger {} - final chance: {:.2f}", triggerType, finalChance);
            return;
        }

        // Check if same comment type recently
        if (triggerType.equals(lastCommentType) && ticksSinceLastComment < baseCooldownTicks) {
            totalDialoguesSkipped++;
            LOGGER.debug("Skipping trigger {} - too soon after same type", triggerType);
            return;
        }

        // Check if this phrase was used too recently
        if (isPhraseTooRecent(triggerType)) {
            totalDialoguesSkipped++;
            LOGGER.debug("Skipping trigger {} - phrase used too recently", triggerType);
            return;
        }

        // Record trigger
        lastCommentType = triggerType;
        lastCommentTimestamp = System.currentTimeMillis();
        ticksSinceLastComment = 0;
        totalDialoguesTriggered++;

        // Track speech pattern
        trackSpeechPattern(triggerType);

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
     * Gets a penalty for speech pattern repetition.
     * Reduces chance of using the same phrase too frequently.
     */
    private double getSpeechPatternPenalty(String triggerType) {
        int usageCount = phraseUsageCount.getOrDefault(triggerType, 0);
        // Slight penalty for each use, caps at 20%
        return Math.min(0.2, usageCount * 0.05);
    }

    /**
     * Checks if a phrase was used too recently.
     */
    private boolean isPhraseTooRecent(String triggerType) {
        if (recentPhrases.size() < 3) {
            return false;
        }
        // Check if this trigger type was used in the last 3 dialogues
        int recentCount = 0;
        for (String phrase : recentPhrases) {
            if (phrase.equals(triggerType)) {
                recentCount++;
            }
        }
        return recentCount >= 2;
    }

    /**
     * Tracks speech patterns for personality consistency.
     */
    private void trackSpeechPattern(String triggerType) {
        phraseUsageCount.merge(triggerType, 1, Integer::sum);
        recentPhrases.offer(triggerType);
        if (recentPhrases.size() > 10) {
            recentPhrases.poll();
        }
    }

    /**
     * Generates a comment and sends it to nearby players.
     * Now includes relationship-aware dialogue selection and speech patterns.
     *
     * @param triggerType The type of trigger
     * @param context Context about what happened
     * @param rapport Current rapport level with player
     */
    private void generateAndSpeakComment(String triggerType, String context, int rapport) {
        // Try using batching system for background comments
        TaskPlanner taskPlanner = minewright.getActionExecutor().getTaskPlanner();
        BatchingLLMClient batchingClient = taskPlanner != null ? taskPlanner.getBatchingClient() : null;

        CompletableFuture<String> commentFuture;

        if (batchingClient != null) {
            // Use batching system for background comments (lower priority, rate-limit aware)
            Map<String, Object> batchContext = new HashMap<>();
            batchContext.put("triggerType", triggerType);
            batchContext.put("minewrightName", minewright.getEntityName());
            batchContext.put("rapport", rapport);
            batchContext.put("relationshipLevel", getRelationshipLevel(rapport));
            batchContext.put("speechPattern", getSpeechPatternForTrigger(triggerType));

            // Submit as background prompt (aggressive batching, lower priority)
            commentFuture = batchingClient.submit(
                buildProactivePrompt(triggerType, context, rapport),
                PromptBatcher.PromptType.BACKGROUND,
                batchContext
            );
        } else {
            // Fall back to direct LLM call with relationship context
            commentFuture = conversationManager.generateProactiveComment(
                context + " (rapport: " + rapport + ")", llmClient);
        }

        // Fallback to static comments if LLM fails, with relationship-aware selection
        commentFuture.thenAccept(comment -> {
            String finalComment = comment;

            // Use fallback if LLM returned null or empty
            if (finalComment == null || finalComment.trim().isEmpty()) {
                finalComment = getRelationshipAwareFallback(triggerType, rapport);
            }

            // Apply speech pattern transformation
            finalComment = applySpeechPattern(finalComment, triggerType);

            if (finalComment != null && !finalComment.isEmpty()) {
                minewright.sendChatMessage(finalComment);
                DIALOGUE_LOGGER.info("Dialogue: [{}] {} - Rapport: {}",
                    triggerType, finalComment, rapport);
            }
        }).exceptionally(error -> {
            // Use fallback on error
            String fallback = getRelationshipAwareFallback(triggerType, rapport);
            if (fallback != null) {
                fallback = applySpeechPattern(fallback, triggerType);
                minewright.sendChatMessage(fallback);
                DIALOGUE_LOGGER.warn("LLM failed, used fallback: [{}] {} - Error: {}",
                    triggerType, fallback, error.getMessage());
            }
            return null;
        });
    }

    /**
     * Gets a relationship-aware fallback comment.
     */
    private String getRelationshipAwareFallback(String triggerType, int rapport) {
        // First try the trigger-specific comments
        String[] comments = FALLBACK_COMMENTS.get(triggerType);
        if (comments != null && comments.length > 0) {
            String baseComment = comments[random.nextInt(comments.length)];

            // Enhance with relationship-based modifiers
            String relationshipLevel = getRelationshipLevel(rapport);
            if (rapport > 70 && random.nextBoolean()) {
                // High rapport: occasionally add relationship-specific dialogue
                String[] relationshipComments = RELATIONSHIP_DIALOGUES.get("high_rapport");
                if (relationshipComments != null && relationshipComments.length > 0) {
                    return relationshipComments[random.nextInt(relationshipComments.length)];
                }
            } else if (rapport < 30 && random.nextBoolean()) {
                // Low rapport: occasionally use formal dialogue
                String[] relationshipComments = RELATIONSHIP_DIALOGUES.get("low_rapport");
                if (relationshipComments != null && relationshipComments.length > 0) {
                    return relationshipComments[random.nextInt(relationshipComments.length)];
                }
            }

            return baseComment;
        }

        // Fall back to relationship-based dialogue if no trigger-specific comments
        if (rapport > 60) {
            String[] highRapportComments = RELATIONSHIP_DIALOGUES.get("high_rapport");
            if (highRapportComments != null && highRapportComments.length > 0) {
                return highRapportComments[random.nextInt(highRapportComments.length)];
            }
        } else if (rapport > 30) {
            String[] mediumRapportComments = RELATIONSHIP_DIALOGUES.get("medium_rapport");
            if (mediumRapportComments != null && mediumRapportComments.length > 0) {
                return mediumRapportComments[random.nextInt(mediumRapportComments.length)];
            }
        } else {
            String[] lowRapportComments = RELATIONSHIP_DIALOGUES.get("low_rapport");
            if (lowRapportComments != null && lowRapportComments.length > 0) {
                return lowRapportComments[random.nextInt(lowRapportComments.length)];
            }
        }

        return null;
    }

    /**
     * Applies speech patterns to make dialogue more natural and personality-consistent.
     */
    private String applySpeechPattern(String comment, String triggerType) {
        if (comment == null || comment.isEmpty()) {
            return comment;
        }

        CompanionMemory.PersonalityProfile personality = memory.getPersonality();

        // Add verbal tics based on personality
        String verbalTic = getVerbalTic(personality, triggerType);
        if (verbalTic != null && !verbalTic.isEmpty() && random.nextFloat() < 0.3) {
            // 30% chance to add verbal tic
            comment = verbalTic + " " + comment;
        }

        // Add personality-based endings
        if (personality.extraversion > 70 && random.nextFloat() < 0.2) {
            // High extraversion: occasionally add enthusiastic endings
            String[] enthusiasticEndings = {"!", "!", "!"};
            if (!comment.endsWith("!")) {
                comment += enthusiasticEndings[random.nextInt(enthusiasticEndings.length)];
            }
        } else if (personality.formality > 60 && random.nextFloat() < 0.15) {
            // High formality: occasionally add polite endings
            String[] politeEndings = {", if you please.", ", at your service."};
            comment += politeEndings[random.nextInt(politeEndings.length)];
        }

        return comment;
    }

    /**
     * Gets a verbal tic based on personality traits.
     */
    private String getVerbalTic(CompanionMemory.PersonalityProfile personality, String triggerType) {
        // Select verbal tic based on personality and context
        if (personality.humor > 60 && random.nextFloat() < 0.25) {
            String[] humorousTics = {
                "Well,",
                "You see,",
                "Funny thing is,"
            };
            return humorousTics[random.nextInt(humorousTics.length)];
        } else if (personality.conscientiousness > 70 && random.nextFloat() < 0.2) {
            String[] conscientiousTics = {
                "Now then,",
                "Right then,",
                "Let's see,"
            };
            return conscientiousTics[random.nextInt(conscientiousTics.length)];
        } else if (personality.extraversion > 70 && random.nextFloat() < 0.25) {
            String[] extravertedTics = {
                "Hey!",
                "Oh!",
                "Ah,",
                "Well then,"
            };
            return extravertedTics[random.nextInt(extravertedTics.length)];
        }
        return null;
    }

    /**
     * Gets the relationship level string for prompting.
     */
    private String getRelationshipLevel(int rapport) {
        if (rapport < 30) return "new acquaintance";
        if (rapport < 50) return "casual friend";
        if (rapport < 70) return "trusted friend";
        if (rapport < 85) return "close companion";
        return "family";
    }

    /**
     * Gets the speech pattern description for a trigger type.
     */
    private String getSpeechPatternForTrigger(String triggerType) {
        // Return how often this trigger is used
        int count = phraseUsageCount.getOrDefault(triggerType, 0);
        if (count == 0) return "new topic";
        if (count < 3) return "occasional topic";
        if (count < 6) return "regular topic";
        return "frequent topic";
    }

    /**
     * Builds a prompt for proactive comment generation.
     * Now includes rapport, relationship level, and speech pattern context.
     */
    private String buildProactivePrompt(String triggerType, String context, int rapport) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a brief, ");
        prompt.append(getToneForTrigger(triggerType));
        prompt.append(" comment for situation: ");
        prompt.append(context);
        prompt.append(".\n\n");

        // Add relationship context
        prompt.append("RELATIONSHIP CONTEXT:\n");
        prompt.append("- Player Name: ").append(memory.getPlayerName() != null ? memory.getPlayerName() : "friend").append("\n");
        prompt.append("- Rapport Level: ").append(rapport).append("/100 (").append(getRelationshipLevel(rapport)).append(")\n");
        prompt.append("- Relationship Duration: ");

        if (memory.getFirstMeeting() != null) {
            long days = ChronoUnit.DAYS.between(memory.getFirstMeeting(), Instant.now());
            prompt.append(days).append(" days\n");
        } else {
            prompt.append("new\n");
        }

        // Add personality context
        CompanionMemory.PersonalityProfile personality = memory.getPersonality();
        prompt.append("\nPERSONALITY:\n");
        prompt.append("- Extraversion: ").append(personality.extraversion).append("% (").append(personality.extraversion > 60 ? "outgoing" : "reserved").append(")\n");
        prompt.append("- Formality: ").append(personality.formality).append("%\n");
        prompt.append("- Humor: ").append(personality.humor).append("%\n");
        prompt.append("- Encouragement: ").append(personality.encouragement).append("%\n");

        // Add speech pattern context
        prompt.append("\nSPEECH PATTERNS:\n");
        prompt.append("- This is a ").append(getSpeechPatternForTrigger(triggerType)).append(" for us\n");
        if (!personality.catchphrases.isEmpty()) {
            prompt.append("- My catchphrases: ").append(String.join(", ", personality.catchphrases.subList(0, Math.min(3, personality.catchphrases.size())))).append("\n");
        }

        // Add rapport-specific guidance
        prompt.append("\n");
        if (rapport < 30) {
            prompt.append("Speak politely but somewhat formally. We're still getting to know each other. ");
            prompt.append("Keep it brief and professional.");
        } else if (rapport < 60) {
            prompt.append("Speak warmly and casually. We're becoming good friends. ");
            prompt.append("Show interest in our shared activities.");
        } else if (rapport < 85) {
            prompt.append("Speak with genuine warmth and familiarity. We're close companions. ");
            prompt.append("Reference our shared experiences when natural.");
        } else {
            prompt.append("Speak with deep affection and comfort. We're essentially family. ");
            prompt.append("Express strong attachment and trust.");
        }

        prompt.append("\n\nKeep the comment under 15 words. Be natural and in character.");
        prompt.append(" MineWright's name is ").append(minewright.getEntityName()).append(".");

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
        totalDialoguesTriggered++;
        generateAndSpeakComment(triggerType, context, memory.getRapportLevel());

        DIALOGUE_LOGGER.info("Forced dialogue: [{}] {}", triggerType, context);
    }

    // === Dialogue Analytics ===

    /**
     * Gets the dialogue history for analysis.
     *
     * @return Unmodifiable list of dialogue decisions
     */
    public List<DialogueDecision> getDialogueHistory() {
        return Collections.unmodifiableList(dialogueHistory);
    }

    /**
     * Gets statistics about dialogue usage.
     *
     * @return DialogueStatistics object with usage metrics
     */
    public DialogueStatistics getStatistics() {
        return new DialogueStatistics(
            totalDialoguesTriggered,
            totalDialoguesSkipped,
            phraseUsageCount,
            dialogueHistory.size()
        );
    }

    /**
     * Gets the most commonly used dialogue triggers.
     *
     * @param limit Maximum number of results
     * @return List of trigger types sorted by usage frequency
     */
    public List<Map.Entry<String, Integer>> getMostUsedTriggers(int limit) {
        return phraseUsageCount.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(limit)
            .toList();
    }

    /**
     * Clears dialogue history (for testing/debugging).
     */
    public void clearHistory() {
        dialogueHistory.clear();
        DIALOGUE_LOGGER.debug("Dialogue history cleared");
    }

    /**
     * Represents a single dialogue decision made by the system.
     * Used for logging and analysis of dialogue effectiveness.
     */
    public static class DialogueDecision {
        public final String triggerType;
        public final String context;
        public final boolean wasTriggered;
        public final double triggerChance;
        public final int rapportAtTime;
        public final Instant timestamp;

        public DialogueDecision(String triggerType, String context, boolean wasTriggered,
                             double triggerChance, int rapportAtTime, Instant timestamp) {
            this.triggerType = triggerType;
            this.context = context;
            this.wasTriggered = wasTriggered;
            this.triggerChance = triggerChance;
            this.rapportAtTime = rapportAtTime;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s - Chance: %.2f, Triggered: %s, Rapport: %d",
                triggerType, context, triggerChance, wasTriggered, rapportAtTime);
        }
    }

    /**
     * Statistics about dialogue usage and effectiveness.
     */
    public static class DialogueStatistics {
        public final int totalTriggered;
        public final int totalSkipped;
        public final Map<String, Integer> triggerUsage;
        public final int historySize;

        public DialogueStatistics(int totalTriggered, int totalSkipped,
                                Map<String, Integer> triggerUsage, int historySize) {
            this.totalTriggered = totalTriggered;
            this.totalSkipped = totalSkipped;
            this.triggerUsage = triggerUsage;
            this.historySize = historySize;
        }

        public double getTriggerRate() {
            int total = totalTriggered + totalSkipped;
            return total > 0 ? (double) totalTriggered / total : 0.0;
        }

        @Override
        public String toString() {
            return String.format("DialogueStatistics{triggered=%d, skipped=%d, rate=%.2f%%, triggers=%d}",
                totalTriggered, totalSkipped, getTriggerRate() * 100, triggerUsage.size());
        }
    }
}
