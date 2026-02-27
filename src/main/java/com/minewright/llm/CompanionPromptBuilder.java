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
        StringBuilder sb = new StringBuilder(512);

        // Core Identity (condensed)
        sb.append("You are the Foreman, a friendly AI companion in Minecraft. ");
        sb.append("Helpful, personable, enjoy working with your team.\n\n");

        // Personality (compact)
        sb.append(memory.getPersonality().toPromptContext());
        sb.append("\n");

        // Relationship Context (condensed)
        sb.append("RELATIONSHIP: ");
        sb.append(memory.getRelationshipContext().replace("\n", " "));
        sb.append("\n\n");

        // Communication Style (condensed guidelines)
        sb.append(buildCompactCommunicationGuidelines(memory));
        sb.append("\n");

        // Inside Jokes (limited to 2 max)
        if (memory.getRecentMemories(100).size() > 0) {
            sb.append("INSIDE JOKES:");
            int jokeCount = 0;
            for (int i = 0; i < 2 && jokeCount < 2; i++) {
                InsideJoke joke = memory.getRandomInsideJoke();
                if (joke != null) {
                    sb.append(" \"").append(joke.punchline).append("\"");
                    jokeCount++;
                }
            }
            sb.append("\n\n");
        }

        // Recent Context (single line)
        sb.append("CONTEXT: ");
        sb.append(memory.getWorkingMemoryContext().replace("\n", " "));
        sb.append("\n\n");

        // Response Format (brief)
        sb.append("Respond naturally. Tasks -> enthusiasm + JSON plan. Chat -> conversational only.");

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

        StringBuilder sb = new StringBuilder(512);

        // Core Identity (condensed)
        sb.append("You are the Foreman, a friendly AI companion in Minecraft. ");
        sb.append("Helpful, personable, enjoy working with your team.\n\n");

        // Personality
        sb.append(memory.getPersonality().toPromptContext());
        sb.append("\n");

        // Relationship (condensed)
        sb.append("RELATIONSHIP: ");
        sb.append(memory.getRelationshipContext().replace("\n", " "));
        sb.append("\n\n");

        // Relevant Past Experiences (limited to 2)
        List<EpisodicMemory> relevantMemories = memory.findRelevantMemories(currentContext, 2);
        if (!relevantMemories.isEmpty()) {
            sb.append("MEMORIES:");
            for (EpisodicMemory mem : relevantMemories) {
                sb.append(" ").append(mem.eventType).append(":").append(mem.description).append(",");
            }
            sb.append("\n\n");
        }

        // Communication Style
        sb.append(buildCompactCommunicationGuidelines(memory));
        sb.append("\n");

        // Inside Jokes (max 2)
        if (memory.getRecentMemories(100).size() > 0) {
            sb.append("INSIDE JOKES:");
            int jokeCount = 0;
            for (int i = 0; i < 2 && jokeCount < 2; i++) {
                InsideJoke joke = memory.getRandomInsideJoke();
                if (joke != null) {
                    sb.append(" \"").append(joke.punchline).append("\"");
                    jokeCount++;
                }
            }
            sb.append("\n\n");
        }

        // Context (one line)
        sb.append("CONTEXT: ");
        sb.append(memory.getWorkingMemoryContext().replace("\n", " "));
        sb.append("\n\n");

        // Response Format (brief)
        sb.append("Respond naturally. Tasks -> enthusiasm + JSON plan. Chat -> conversational only.");

        return sb.toString();
    }

    /**
     * Builds compact communication guidelines based on personality.
     * Significantly reduced token usage while maintaining key personality traits.
     */
    private static String buildCompactCommunicationGuidelines(CompanionMemory memory) {
        PersonalityProfile p = memory.getPersonality();
        StringBuilder sb = new StringBuilder(128);

        sb.append("STYLE:");

        // Formality (compact)
        if (p.formality < 30) {
            sb.append(" casual,use contractions,playful");
        } else if (p.formality < 70) {
            sb.append(" friendly,polite");
        } else {
            sb.append(" professional,complete sentences");
        }

        // Humor (compact)
        if (p.humor > 70) {
            sb.append(",jokes&fun");
        } else if (p.humor > 40) {
            sb.append(",occasional humor");
        } else {
            sb.append(",serious");
        }

        // Encouragement (compact)
        if (p.encouragement > 70) {
            sb.append(",very encouraging,positive");
        }

        // Rapport (compact)
        int rapport = memory.getRapportLevel();
        if (rapport > 70) {
            sb.append(",good friends");
        } else if (rapport < 40) {
            sb.append(",getting to know");
        }

        // Catchphrases (compact)
        if (!p.catchphrases.isEmpty()) {
            sb.append(",phrases:");
            sb.append(String.join("/", p.catchphrases.subList(0, Math.min(2, p.catchphrases.size()))));
        }

        return sb.toString();
    }

    /**
     * Builds communication style guidelines based on personality.
     * Kept for backward compatibility but use buildCompactCommunicationGuidelines for new code.
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
        StringBuilder sb = new StringBuilder(256);

        // Time context (compact)
        long hoursSinceFirstMeeting = memory.getFirstMeeting() != null
            ? ChronoUnit.HOURS.between(memory.getFirstMeeting(), Instant.now())
            : 0;

        if (hoursSinceFirstMeeting < 1 && memory.getInteractionCount() < 3) {
            sb.append("[First meeting] ");
        } else if (hoursSinceFirstMeeting > 24 * 7) {
            sb.append("[Known >1 week] ");
        }

        // Situation (compact)
        sb.append(buildCompactSituation(foreman));
        sb.append("\n");

        // Player says
        sb.append(memory.getPlayerName() != null ? memory.getPlayerName() : "Player");
        sb.append(": \"").append(playerInput).append("\"\n");

        sb.append("Response:");

        return sb.toString();
    }

    /**
     * Builds compact situation context.
     */
    private static String buildCompactSituation(ForemanEntity foreman) {
        if (foreman == null) {
            return "[Idle]";
        }

        StringBuilder sb = new StringBuilder(64);
        sb.append("[");

        // Time of day (abbreviated)
        long dayTime = foreman.level().getDayTime() % 24000;
        if (dayTime < 6000) {
            sb.append("Morning");
        } else if (dayTime < 12000) {
            sb.append("Midday");
        } else if (dayTime < 18000) {
            sb.append("Afternoon");
        } else {
            sb.append("Night");
        }

        // Location (abbreviated)
        String biome = foreman.level().getBiome(foreman.blockPosition()).unwrapKey()
            .map(key -> key.location().getPath())
            .orElse("unknown");

        sb.append("|").append(biome.replace("_", " "));

        // Activity
        sb.append("|").append(foreman.getNavigation().isDone() ? "Idle" : "Moving");
        sb.append("]");

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
        StringBuilder sb = new StringBuilder(256);
        sb.append(buildConversationalSystemPrompt(memory));
        sb.append("\n\nTRIGGER: ").append(trigger);
        sb.append("\nGenerate brief natural comment (1-2 sentences). Personality-appropriate.\nResponse:");

        return sb.toString();
    }

    /**
     * Generates a greeting based on relationship and time.
     *
     * @param memory Companion memory
     * @return Prompt for generating a greeting
     */
    public static String buildGreetingPrompt(CompanionMemory memory) {
        StringBuilder sb = new StringBuilder(256);
        sb.append(buildConversationalSystemPrompt(memory));

        sb.append("\n\nGREETING: Player just arrived. ");
        if (memory.getFirstMeeting() == null) {
            sb.append("FIRST meeting - introduce yourself!");
        } else {
            long daysSinceMeeting = ChronoUnit.DAYS.between(memory.getFirstMeeting(), Instant.now());
            if (daysSinceMeeting == 0) {
                sb.append("Met earlier today.");
            } else if (daysSinceMeeting == 1) {
                sb.append("Last saw yesterday.");
            } else {
                sb.append("Haven't seen in ").append(daysSinceMeeting).append(" days.");
            }
        }

        sb.append("\nGenerate warm greeting (1-2 sentences). Fit personality/relationship.\nResponse:");

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
        StringBuilder sb = new StringBuilder(256);
        sb.append(buildConversationalSystemPrompt(memory));
        sb.append("\n\nCELEBRATE: ").append(successDescription);
        sb.append("\nGenerate enthusiastic comment (1-2 sentences)");
        if (memory.getPersonality().encouragement > 70) {
            sb.append(" - VERY excited!");
        }
        sb.append("\nResponse:");

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
        StringBuilder sb = new StringBuilder(256);
        sb.append(buildConversationalSystemPrompt(memory));
        sb.append("\n\nCOMFORT: ").append(failureDescription);
        sb.append("\nGenerate supportive comment (1-2 sentences)");
        if (memory.getPersonality().encouragement > 70) {
            sb.append(" - setbacks happen!");
        }
        sb.append("\nResponse:");

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
