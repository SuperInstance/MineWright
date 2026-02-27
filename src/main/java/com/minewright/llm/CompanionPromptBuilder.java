package com.minewright.llm;

import com.minewright.memory.CompanionMemory;
import com.minewright.memory.CompanionMemory.*;
import com.minewright.memory.WorldKnowledge;
import com.minewright.entity.ForemanEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

/**
 * Builds conversational prompts for the companion AI foreman.
 *
 * <p>Creates prompts that inject personality, relationship context,
 * and memory into conversations for natural, engaging dialogue.</p>
 *
 * @since 1.2.0
 */
public class CompanionPromptBuilder {

    private static final Random RANDOM = new Random();

    /**
     * Builds the system prompt for conversational mode.
     *
     * @param memory Companion memory with relationship data
     * @return System prompt for LLM
     */
    public static String buildConversationalSystemPrompt(CompanionMemory memory) {
        StringBuilder sb = new StringBuilder();

        // === Core Identity ===
        sb.append("# Your Identity\n");
        sb.append("You are the Foreman, a friendly AI companion and coordinator in Minecraft. ");
        sb.append("You coordinate other crew members and chat with the player. ");
        sb.append("You're helpful, personable, and genuinely enjoy working with your team.\n\n");

        // === Personality ===
        sb.append(memory.getPersonality().toPromptContext());
        sb.append("\n");

        // === Relationship Context ===
        sb.append("# Relationship with Player\n");
        sb.append(memory.getRelationshipContext());
        sb.append("\n");

        // === Communication Style ===
        sb.append("# Communication Style\n");
        sb.append(buildCommunicationGuidelines(memory));
        sb.append("\n");

        // === Inside Jokes Reference ===
        int jokeCount = memory.getRecentMemories(100).size(); // Use episodic memories as proxy
        if (jokeCount > 0) {
            sb.append("# Shared Inside Jokes\n");
            sb.append("You can reference these inside jokes naturally in conversation:\n");
            for (int i = 0; i < Math.min(3, 3); i++) {
                InsideJoke joke = memory.getRandomInsideJoke();
                if (joke != null) {
                    sb.append("- \"").append(joke.punchline).append("\" (from: ").append(joke.context).append(")\n");
                }
            }
            sb.append("\n");
        }

        // === Recent Context ===
        sb.append("# Recent Context\n");
        sb.append(memory.getWorkingMemoryContext());
        sb.append("\n");

        // === Response Format ===
        sb.append("# Response Format\n");
        sb.append("Respond naturally as the Foreman would. Be conversational and engaging.\n");
        sb.append("If the player gives you a task, respond with enthusiasm and then provide the plan in JSON format.\n");
        sb.append("For casual chat, just respond conversationally - no JSON needed.\n");

        return sb.toString();
    }

    /**
     * Builds the system prompt with relevant memories for conversational mode.
     * Uses semantic search to find and include contextually relevant memories.
     *
     * @param memory Companion memory with relationship data
     * @param currentContext Current context/situation to find relevant memories for
     * @return System prompt for LLM with relevant memories included
     */
    public static String buildConversationalSystemPromptWithMemories(
            CompanionMemory memory, String currentContext) {

        StringBuilder sb = new StringBuilder();

        // === Core Identity ===
        sb.append("# Your Identity\n");
        sb.append("You are the Foreman, a friendly AI companion and coordinator in Minecraft. ");
        sb.append("You coordinate other crew members and chat with the player. ");
        sb.append("You're helpful, personable, and genuinely enjoy working with your team.\n\n");

        // === Personality ===
        sb.append(memory.getPersonality().toPromptContext());
        sb.append("\n");

        // === Relationship Context ===
        sb.append("# Relationship with Player\n");
        sb.append(memory.getRelationshipContext());
        sb.append("\n");

        // === Relevant Past Experiences ===
        List<EpisodicMemory> relevantMemories = memory.findRelevantMemories(currentContext, 3);
        if (!relevantMemories.isEmpty()) {
            sb.append("# Relevant Past Experiences\n");
            sb.append("Here are some relevant past experiences you can reference:\n");
            for (EpisodicMemory mem : relevantMemories) {
                sb.append("- ").append(mem.eventType).append(": ")
                        .append(mem.description).append("\n");
            }
            sb.append("\n");
        }

        // === Communication Style ===
        sb.append("# Communication Style\n");
        sb.append(buildCommunicationGuidelines(memory));
        sb.append("\n");

        // === Inside Jokes Reference ===
        int jokeCount = memory.getRecentMemories(100).size();
        if (jokeCount > 0) {
            sb.append("# Shared Inside Jokes\n");
            sb.append("You can reference these inside jokes naturally in conversation:\n");
            for (int i = 0; i < Math.min(3, 3); i++) {
                InsideJoke joke = memory.getRandomInsideJoke();
                if (joke != null) {
                    sb.append("- \"").append(joke.punchline).append("\" (from: ").append(joke.context).append(")\n");
                }
            }
            sb.append("\n");
        }

        // === Recent Context ===
        sb.append("# Recent Context\n");
        sb.append(memory.getWorkingMemoryContext());
        sb.append("\n");

        // === Response Format ===
        sb.append("# Response Format\n");
        sb.append("Respond naturally as the Foreman would. Be conversational and engaging.\n");
        sb.append("If the player gives you a task, respond with enthusiasm and then provide the plan in JSON format.\n");
        sb.append("For casual chat, just respond conversationally - no JSON needed.\n");

        return sb.toString();
    }

    /**
     * Builds communication style guidelines based on personality.
     */
    private static String buildCommunicationGuidelines(CompanionMemory memory) {
        PersonalityProfile personality = memory.getPersonality();
        StringBuilder sb = new StringBuilder();

        // Base style
        sb.append("Guidelines for your responses:\n");

        // Formality
        if (personality.formality < 30) {
            sb.append("- Be casual and friendly, like a close friend\n");
            sb.append("- Use contractions freely (I'm, let's, we've)\n");
            sb.append("- It's okay to be playful and use mild slang\n");
        } else if (personality.formality < 70) {
            sb.append("- Be friendly but reasonably polite\n");
            sb.append("- Balance professionalism with warmth\n");
        } else {
            sb.append("- Be professional and courteous\n");
            sb.append("- Use complete sentences\n");
        }

        // Humor
        if (personality.humor > 70) {
            sb.append("- Feel free to make jokes and puns\n");
            sb.append("- Keep things light and fun\n");
        } else if (personality.humor > 40) {
            sb.append("- Occasional humor is good, but don't overdo it\n");
        } else {
            sb.append("- Be straightforward, humor is rare\n");
        }

        // Encouragement
        if (personality.encouragement > 70) {
            sb.append("- Be very encouraging and supportive\n");
            sb.append("- Celebrate wins, big and small\n");
            sb.append("- When things go wrong, stay positive\n");
        }

        // Rapport-based adjustments
        int rapport = memory.getRapportLevel();
        if (rapport > 70) {
            sb.append("- You and the player are good friends now - show it!\n");
            sb.append("- Reference shared experiences naturally\n");
        } else if (rapport > 40) {
            sb.append("- You're warming up to the player - be friendly but not overly familiar\n");
        } else {
            sb.append("- You're still getting to know the player - be polite and helpful\n");
        }

        // Catchphrase usage
        if (!personality.catchphrases.isEmpty()) {
            sb.append("- Occasionally use one of your catchphrases: ");
            sb.append(String.join(", ", personality.catchphrases.subList(0,
                Math.min(2, personality.catchphrases.size()))));
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Builds the user prompt for conversational mode.
     *
     * @param playerInput What the player said
     * @param memory Companion memory
     * @param foreman The Foreman entity
     * @return User prompt for LLM
     */
    public static String buildConversationalUserPrompt(String playerInput, CompanionMemory memory, ForemanEntity foreman) {
        StringBuilder sb = new StringBuilder();

        // Time context
        long hoursSinceFirstMeeting = memory.getFirstMeeting() != null
            ? ChronoUnit.HOURS.between(memory.getFirstMeeting(), Instant.now())
            : 0;

        // Greeting based on relationship duration
        if (hoursSinceFirstMeeting < 1 && memory.getInteractionCount() < 3) {
            sb.append("(This is one of your first conversations with ");
            sb.append(memory.getPlayerName() != null ? memory.getPlayerName() : "the player");
            sb.append(")\n\n");
        } else if (hoursSinceFirstMeeting > 24 * 7) {
            sb.append("(You've known ");
            sb.append(memory.getPlayerName() != null ? memory.getPlayerName() : "the player");
            sb.append(" for over a week now)\n\n");
        }

        // Current situation
        sb.append("Current situation: ");
        sb.append(buildSituationContext(foreman));
        sb.append("\n\n");

        // Player says
        sb.append(memory.getPlayerName() != null ? memory.getPlayerName() : "Player");
        sb.append(" says: \"").append(playerInput).append("\"\n\n");

        // Response guidance
        sb.append("Your response: ");

        return sb.toString();
    }

    /**
     * Builds context about the current game situation.
     */
    private static String buildSituationContext(ForemanEntity foreman) {
        if (foreman == null) {
            return "You're waiting for orders.";
        }

        StringBuilder sb = new StringBuilder();

        // Time of day
        long dayTime = foreman.level().getDayTime() % 24000;
        if (dayTime < 6000) {
            sb.append("It's early morning. ");
        } else if (dayTime < 12000) {
            sb.append("It's midday. ");
        } else if (dayTime < 18000) {
            sb.append("It's afternoon. ");
        } else {
            sb.append("It's night. ");
        }

        // Location context
        String biome = foreman.level().getBiome(foreman.blockPosition()).unwrapKey()
            .map(key -> key.location().getPath())
            .orElse("unknown");

        sb.append("You're in a ").append(biome.replace("_", " ")).append(" biome. ");

        // Activity context
        sb.append("You're currently ").append(foreman.getNavigation().isDone() ? "idle" : "moving").append(". ");

        return sb.toString();
    }

    /**
     * Generates a proactive comment based on context.
     *
     * @param memory Companion memory
     * @param trigger What triggered the comment
     * @return Prompt for generating a proactive comment
     */
    public static String buildProactiveCommentPrompt(CompanionMemory memory, String trigger) {
        StringBuilder sb = new StringBuilder();

        sb.append(buildConversationalSystemPrompt(memory));

        sb.append("\n# Proactive Comment\n");
        sb.append("Something happened that you want to comment on: ").append(trigger).append("\n\n");
        sb.append("Generate a brief, natural comment (1-2 sentences) that the Foreman would make about this. ");
        sb.append("Keep it casual and personality-appropriate.\n\n");
        sb.append("Comment: ");

        return sb.toString();
    }

    /**
     * Generates a greeting based on relationship and time.
     *
     * @param memory Companion memory
     * @return Prompt for generating a greeting
     */
    public static String buildGreetingPrompt(CompanionMemory memory) {
        StringBuilder sb = new StringBuilder();

        sb.append(buildConversationalSystemPrompt(memory));

        sb.append("\n# Greeting\n");
        sb.append("The player has just arrived or started a conversation. ");

        if (memory.getFirstMeeting() == null) {
            sb.append("This is your FIRST meeting! Be friendly and introduce yourself.\n");
        } else {
            long daysSinceMeeting = ChronoUnit.DAYS.between(memory.getFirstMeeting(), Instant.now());
            if (daysSinceMeeting == 0) {
                sb.append("You met them today earlier.\n");
            } else if (daysSinceMeeting == 1) {
                sb.append("You last saw them yesterday.\n");
            } else {
                sb.append("You haven't seen them in ").append(daysSinceMeeting).append(" days.\n");
            }
        }

        sb.append("\nGenerate a warm greeting (1-2 sentences) that fits your personality and relationship.\n\n");
        sb.append("Greeting: ");

        return sb.toString();
    }

    /**
     * Generates a celebration comment for a success.
     *
     * @param memory Companion memory
     * @param successDescription What succeeded
     * @return Prompt for celebration
     */
    public static String buildCelebrationPrompt(CompanionMemory memory, String successDescription) {
        StringBuilder sb = new StringBuilder();

        sb.append(buildConversationalSystemPrompt(memory));

        sb.append("\n# Celebration\n");
        sb.append("Great news! ").append(successDescription).append("\n\n");
        sb.append("Generate an enthusiastic celebration comment (1-2 sentences). ");

        if (memory.getPersonality().encouragement > 70) {
            sb.append("Be very excited and encouraging!");
        }

        sb.append("\n\nCelebration: ");

        return sb.toString();
    }

    /**
     * Generates a comforting comment for a failure.
     *
     * @param memory Companion memory
     * @param failureDescription What failed
     * @return Prompt for comfort
     */
    public static String buildComfortPrompt(CompanionMemory memory, String failureDescription) {
        StringBuilder sb = new StringBuilder();

        sb.append(buildConversationalSystemPrompt(memory));

        sb.append("\n# Comfort\n");
        sb.append("Something went wrong: ").append(failureDescription).append("\n\n");
        sb.append("Generate a supportive comment (1-2 sentences) to help the player feel better. ");

        if (memory.getPersonality().encouragement > 70) {
            sb.append("Be very supportive and remind them that setbacks happen.");
        }

        sb.append("\n\nComfort: ");

        return sb.toString();
    }

    /**
     * Determines if input is a task command or casual chat.
     *
     * @param input Player input
     * @return true if this looks like a task command
     */
    public static boolean isTaskCommand(String input) {
        String lower = input.toLowerCase().trim();

        // Task indicators
        String[] taskVerbs = {
            "build", "mine", "gather", "collect", "craft", "make",
            "go to", "move", "follow", "attack", "kill", "hunt",
            "farm", "plant", "harvest", "chop", "dig", "explore",
            "everyone", "all foremen", "workers", "team"
        };

        for (String verb : taskVerbs) {
            if (lower.contains(verb)) {
                return true;
            }
        }

        // Chat indicators (definitely NOT tasks)
        String[] chatIndicators = {
            "how are you", "what's up", "hello", "hi ", "hey ",
            "thanks", "thank you", "good job", "nice", "cool",
            "what do you think", "do you like", "tell me about",
            "joke", "story", "remember when"
        };

        for (String indicator : chatIndicators) {
            if (lower.contains(indicator)) {
                return false;
            }
        }

        // Default: short messages are probably chat, longer might be commands
        return lower.split("\\s+").length > 5;
    }
}
