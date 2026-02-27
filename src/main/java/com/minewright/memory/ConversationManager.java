package com.minewright.memory;

import com.minewright.entity.ForemanEntity;
import com.minewright.llm.CompanionPromptBuilder;
import com.minewright.llm.async.AsyncLLMClient;
import com.minewright.llm.async.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Manages conversational interactions with the companion AI.
 *
 * <p>Detects whether player input is a task command or casual chat,
 * and routes appropriately. For conversation, uses the companion
 * prompt builder to generate contextual, personality-driven responses.</p>
 *
 * <p><b>Flow:</b></p>
 * <ol>
 *   <li>Player sends message via /foreman tell or GUI</li>
 *   <li>ConversationManager detects task vs chat</li>
 *   <li>If task: Route to ActionExecutor</li>
 *   <li>If chat: Generate conversational response via LLM</li>
 *   <li>Update companion memory with interaction</li>
 * </ol>
 *
 * @since 1.2.0
 */
public class ConversationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationManager.class);

    private final ForemanEntity minewright;
    private final CompanionMemory memory;

    /**
     * Creates a new ConversationManager for a MineWright entity.
     *
     * @param minewright The MineWright entity
     */
    public ConversationManager(ForemanEntity minewright) {
        this.minewright = minewright;
        this.memory = minewright.getCompanionMemory();
    }

    /**
     * Processes a player message, detecting whether it's a task or conversation.
     *
     * @param playerName The player's name
     * @param message The message content
     * @param llmClient Async LLM client for generating responses
     * @return CompletableFuture with the response (if conversational)
     */
    public CompletableFuture<String> processMessage(String playerName, String message, AsyncLLMClient llmClient) {
        // Initialize relationship on first interaction
        if (memory.getPlayerName() == null) {
            memory.initializeRelationship(playerName);
        }

        // Detect task vs conversation
        boolean isTask = CompanionPromptBuilder.isTaskCommand(message);

        if (isTask) {
            // This is a task command - let ActionExecutor handle it
            LOGGER.debug("Detected task command from {}: {}", playerName, message);
            memory.addToWorkingMemory("player_command", message);
            memory.recordPlaystyleMetric("tasks_given", 1);

            // Return empty future - task execution happens via ActionExecutor
            return CompletableFuture.completedFuture(null);
        }

        // This is conversation - generate response
        LOGGER.debug("Detected conversation from {}: {}", playerName, message);
        return generateConversationalResponse(playerName, message, llmClient);
    }

    /**
     * Generates a conversational response using the LLM.
     *
     * @param playerName The player's name
     * @param message The player's message
     * @param llmClient Async LLM client
     * @return CompletableFuture with the response
     */
    private CompletableFuture<String> generateConversationalResponse(String playerName, String message,
                                                                      AsyncLLMClient llmClient) {
        // Build conversational prompts
        String systemPrompt = CompanionPromptBuilder.buildConversationalSystemPrompt(memory);
        String userPrompt = CompanionPromptBuilder.buildConversationalUserPrompt(message, memory, minewright);

        // Add to working memory
        memory.addToWorkingMemory("player_message", message);
        memory.addToWorkingMemory("conversation_topic", extractTopic(message));

        // Track conversation
        memory.getConversationalMemory().addDiscussedTopic(extractTopic(message));

        // Call LLM asynchronously
        Map<String, Object> params = Map.of(
            "systemPrompt", systemPrompt,
            "maxTokens", 300,  // Shorter responses for chat
            "temperature", 0.8  // Higher temperature for more personality
        );

        return llmClient.sendAsync(userPrompt, params)
            .thenApply(response -> {
                String responseText = response.getContent().trim();

                // Update interaction metrics
                memory.incrementInteractionCount();
                memory.adjustRapport(1); // Small rapport boost for conversation

                // Track phrase usage for variety
                trackPhraseUsage(responseText);

                LOGGER.debug("Generated conversational response for {}: {}",
                    playerName, truncate(responseText, 100));

                return responseText;
            })
            .exceptionally(error -> {
                LOGGER.error("Failed to generate conversational response", error);
                return "I'm having trouble thinking of a response right now.";
            });
    }

    /**
     * Generates a proactive comment based on an event.
     *
     * @param trigger What triggered the comment
     * @param llmClient Async LLM client
     * @return CompletableFuture with the comment
     */
    public CompletableFuture<String> generateProactiveComment(String trigger, AsyncLLMClient llmClient) {
        String prompt = CompanionPromptBuilder.buildProactiveCommentPrompt(memory, trigger);

        Map<String, Object> params = Map.of(
            "maxTokens", 150,  // Brief comments
            "temperature", 0.9  // High creativity
        );

        return llmClient.sendAsync(prompt, params)
            .thenApply(response -> response.getContent().trim())
            .exceptionally(error -> {
                LOGGER.debug("Failed to generate proactive comment: {}", error.getMessage());
                return null;  // Silently fail for proactive comments
            });
    }

    /**
     * Generates a greeting when the player approaches.
     *
     * @param llmClient Async LLM client
     * @return CompletableFuture with the greeting
     */
    public CompletableFuture<String> generateGreeting(AsyncLLMClient llmClient) {
        String prompt = CompanionPromptBuilder.buildGreetingPrompt(memory);

        Map<String, Object> params = Map.of(
            "maxTokens", 100,
            "temperature", 0.85
        );

        return llmClient.sendAsync(prompt, params)
            .thenApply(response -> response.getContent().trim())
            .exceptionally(error -> {
                LOGGER.debug("Failed to generate greeting: {}", error.getMessage());
                return "Hey there!";
            });
    }

    /**
     * Generates a celebration message for a success.
     *
     * @param successDescription What succeeded
     * @param llmClient Async LLM client
     * @return CompletableFuture with the celebration
     */
    public CompletableFuture<String> generateCelebration(String successDescription, AsyncLLMClient llmClient) {
        String prompt = CompanionPromptBuilder.buildCelebrationPrompt(memory, successDescription);

        Map<String, Object> params = Map.of(
            "maxTokens", 150,
            "temperature", 0.9
        );

        return llmClient.sendAsync(prompt, params)
            .thenApply(response -> {
                String celebration = response.getContent().trim();
                memory.recordSharedSuccess(successDescription);
                return celebration;
            })
            .exceptionally(error -> {
                LOGGER.debug("Failed to generate celebration: {}", error.getMessage());
                memory.recordSharedSuccess(successDescription);
                return "Great job!";
            });
    }

    /**
     * Generates a comforting message for a failure.
     *
     * @param failureDescription What failed
     * @param llmClient Async LLM client
     * @return CompletableFuture with the comfort message
     */
    public CompletableFuture<String> generateComfort(String failureDescription, AsyncLLMClient llmClient) {
        String prompt = CompanionPromptBuilder.buildComfortPrompt(memory, failureDescription);

        Map<String, Object> params = Map.of(
            "maxTokens", 150,
            "temperature", 0.8
        );

        return llmClient.sendAsync(prompt, params)
            .thenApply(response -> {
                String comfort = response.getContent().trim();
                memory.recordSharedFailure(failureDescription, "attempt failed");
                return comfort;
            })
            .exceptionally(error -> {
                LOGGER.debug("Failed to generate comfort: {}", error.getMessage());
                memory.recordSharedFailure(failureDescription, "attempt failed");
                return "Don't worry, we'll get it next time!";
            });
    }

    /**
     * Extracts a simple topic from a message for tracking.
     */
    private String extractTopic(String message) {
        // Simple keyword extraction - could be enhanced with NLP
        String[] words = message.toLowerCase().split("\\s+");
        if (words.length > 0) {
            return words[0];  // Just use first word as topic
        }
        return "general";
    }

    /**
     * Tracks phrase usage to avoid repetition.
     */
    private void trackPhraseUsage(String response) {
        // Simple approach: track first few words
        String[] words = response.split("\\s+");
        if (words.length >= 2) {
            String phrase = words[0] + " " + words[1];
            memory.getConversationalMemory().recordPhraseUsage(phrase);
        }
    }

    /**
     * Truncates a string for logging.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "[null]";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }
}
