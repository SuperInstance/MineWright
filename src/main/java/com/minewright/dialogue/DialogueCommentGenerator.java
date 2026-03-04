package com.minewright.dialogue;

import com.minewright.config.MineWrightConfig;
import com.minewright.entity.ForemanEntity;
import com.minewright.llm.TaskPlanner;
import com.minewright.llm.batch.BatchingLLMClient;
import com.minewright.llm.batch.PromptBatcher;
import com.minewright.memory.CompanionMemory;
import com.minewright.memory.ConversationManager;
import com.minewright.memory.PersonalitySystem;
import com.minewright.llm.async.AsyncGroqClient;
import com.minewright.llm.async.AsyncLLMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Generates dialogue comments for proactive dialogue.
 * <p>
 * Responsible for:
 * <ul>
 *   <li>Building prompts for LLM-based comment generation</li>
 *   <li>Fallback comment selection when LLM is unavailable</li>
 *   <li>Applying speech patterns and personality to comments</li>
 *   <li>Relationship-aware comment selection</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class DialogueCommentGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DialogueCommentGenerator.class);

    private final ForemanEntity minewright;
    private final CompanionMemory memory;
    private final ConversationManager conversationManager;
    private final AsyncLLMClient llmClient;
    private final Random random;

    // Static fallback comments (used when LLM is unavailable)
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

    public DialogueCommentGenerator(ForemanEntity minewright, CompanionMemory memory) {
        this.minewright = minewright;
        this.memory = memory;
        this.conversationManager = new ConversationManager(minewright);
        this.random = new Random();

        // Initialize LLM client for generating comments
        // Use Groq for fast, free responses
        this.llmClient = new AsyncGroqClient(
            MineWrightConfig.OPENAI_API_KEY.get(),
            "llama-3.1-8b-instant",
            150,  // Brief comments
            0.9   // High creativity
        );
    }

    /**
     * Generates a comment and sends it to nearby players.
     * Includes relationship-aware dialogue selection and speech patterns.
     *
     * @param triggerType The type of trigger
     * @param context Context about what happened
     * @param rapport Current rapport level with player
     * @return CompletableFuture that completes when comment is generated
     */
    public CompletableFuture<String> generateComment(String triggerType, String context, int rapport) {
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

        return commentFuture;
    }

    /**
     * Gets a relationship-aware fallback comment.
     */
    public String getRelationshipAwareFallback(String triggerType, int rapport) {
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
     * Gets a fallback comment from the static pool.
     */
    public String getFallbackComment(String triggerType) {
        String[] comments = FALLBACK_COMMENTS.get(triggerType);
        if (comments != null && comments.length > 0) {
            return comments[random.nextInt(comments.length)];
        }
        return null;
    }

    /**
     * Builds a prompt for proactive comment generation.
     * Includes rapport, relationship level, and speech pattern context.
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
        PersonalitySystem.PersonalityProfile personality = memory.getPersonality();
        prompt.append("\nPERSONALITY:\n");
        int extraversion = personality.getExtraversion();
        prompt.append("- Extraversion: ").append(extraversion).append("% (").append(extraversion > 60 ? "outgoing" : "reserved").append(")\n");
        prompt.append("- Formality: ").append(personality.getFormality()).append("%\n");
        prompt.append("- Humor: ").append(personality.getHumor()).append("%\n");
        prompt.append("- Encouragement: ").append(personality.getEncouragement()).append("%\n");

        // Add speech pattern context
        prompt.append("\nSPEECH PATTERNS:\n");
        prompt.append("- This is a ").append(getSpeechPatternForTrigger(triggerType)).append(" for us\n");
        List<String> catchphrases = personality.getCatchphrases();
        if (!catchphrases.isEmpty()) {
            prompt.append("- My catchphrases: ").append(String.join(", ", catchphrases.subList(0, Math.min(3, catchphrases.size())))).append("\n");
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
        // This would be populated by speech pattern tracking
        return "occasional topic";
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
}
