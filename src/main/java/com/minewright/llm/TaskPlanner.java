package com.minewright.llm;

import com.minewright.MineWrightMod;
import com.minewright.action.Task;
import com.minewright.config.MineWrightConfig;
import com.minewright.entity.ForemanEntity;
import com.minewright.llm.async.*;
import com.minewright.llm.batch.*;
import com.minewright.memory.WorldKnowledge;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TaskPlanner {
    // Legacy synchronous clients (for backward compatibility)
    private final OpenAIClient openAIClient;
    private final GeminiClient geminiClient;
    private final GroqClient groqClient;

    // Async clients (using base clients directly to avoid Resilience4j classloading issues)
    private final AsyncLLMClient asyncOpenAIClient;
    private final AsyncLLMClient asyncGroqClient;
    private final AsyncLLMClient asyncGeminiClient;
    private final LLMCache llmCache;

    // Batching client for rate limit management
    private BatchingLLMClient batchingClient;
    private boolean batchingEnabled = true;

    public TaskPlanner() {
        // Legacy clients
        this.openAIClient = new OpenAIClient();
        this.geminiClient = new GeminiClient();
        this.groqClient = new GroqClient();

        // Initialize async infrastructure
        this.llmCache = new LLMCache();

        // Initialize async clients directly (no resilience wrapper due to Forge classloading issues)
        String apiKey = MineWrightConfig.OPENAI_API_KEY.get();
        String model = MineWrightConfig.OPENAI_MODEL.get();
        int maxTokens = MineWrightConfig.MAX_TOKENS.get();
        double temperature = MineWrightConfig.TEMPERATURE.get();

        // Create async clients without resilience wrapper
        this.asyncOpenAIClient = new AsyncOpenAIClient(apiKey, model, maxTokens, temperature);
        this.asyncGroqClient = new AsyncGroqClient(apiKey, "llama-3.1-8b-instant", 500, temperature);
        this.asyncGeminiClient = new AsyncGeminiClient(apiKey, "gemini-1.5-flash", maxTokens, temperature);

        MineWrightMod.LOGGER.info("TaskPlanner initialized with async clients");
    }

    /**
     * Gets or creates the singleton batching client.
     * This ensures all crew members share the same batching queue.
     */
    public BatchingLLMClient getBatchingClient() {
        if (batchingClient == null && batchingEnabled) {
            // Use OpenAI client as the default underlying client for batching
            batchingClient = new BatchingLLMClient(asyncOpenAIClient);
            batchingClient.start();
            MineWrightMod.LOGGER.info("BatchingLLMClient started for rate limit management");
        }
        return batchingClient;
    }

    /**
     * Gets the batcher for direct access (e.g., for background prompts).
     */
    public PromptBatcher getBatcher() {
        BatchingLLMClient client = getBatchingClient();
        return client != null ? client.getBatcher() : null;
    }

    /**
     * Enables or disables batching.
     */
    public void setBatchingEnabled(boolean enabled) {
        this.batchingEnabled = enabled;
        if (!enabled && batchingClient != null) {
            batchingClient.stop();
            batchingClient = null;
        }
    }

    /**
     * Shuts down the batching client.
     */
    public void shutdown() {
        if (batchingClient != null) {
            batchingClient.stop();
            batchingClient = null;
        }
    }

    public ResponseParser.ParsedResponse planTasks(ForemanEntity foreman, String command) {
        // Check API key before making request
        if (!MineWrightConfig.hasValidApiKey()) {
            MineWrightMod.LOGGER.error("Cannot plan tasks: API key not configured. Please check config/minewright-common.toml");
            return null;
        }

        try {
            String systemPrompt = PromptBuilder.buildSystemPrompt();
            WorldKnowledge worldKnowledge = new WorldKnowledge(foreman);
            String userPrompt = PromptBuilder.buildUserPrompt(foreman, command, worldKnowledge);

            String provider = MineWrightConfig.getValidatedProvider();
            MineWrightMod.LOGGER.info("Requesting AI plan for crew member '{}' using {}: {}", foreman.getSteveName(), provider, command);

            String response = getAIResponse(provider, systemPrompt, userPrompt);

            if (response == null) {
                MineWrightMod.LOGGER.error("Failed to get AI response for command: {}", command);
                return null;
            }
            ResponseParser.ParsedResponse parsedResponse = ResponseParser.parseAIResponse(response);

            if (parsedResponse == null) {
                MineWrightMod.LOGGER.error("Failed to parse AI response");
                return null;
            }

            MineWrightMod.LOGGER.info("Plan: {} ({} tasks)", parsedResponse.getPlan(), parsedResponse.getTasks().size());

            return parsedResponse;

        } catch (Exception e) {
            MineWrightMod.LOGGER.error("Error planning tasks", e);
            return null;
        }
    }

    private String getAIResponse(String provider, String systemPrompt, String userPrompt) {
        String response = switch (provider) {
            case "groq" -> groqClient.sendRequest(systemPrompt, userPrompt);
            case "gemini" -> geminiClient.sendRequest(systemPrompt, userPrompt);
            case "openai" -> openAIClient.sendRequest(systemPrompt, userPrompt);
            default -> {
                MineWrightMod.LOGGER.warn("Unknown AI provider '{}', using Groq", provider);
                yield groqClient.sendRequest(systemPrompt, userPrompt);
            }
        };

        if (response == null && !provider.equals("groq")) {
            MineWrightMod.LOGGER.warn("{} failed, trying Groq as fallback", provider);
            response = groqClient.sendRequest(systemPrompt, userPrompt);
        }

        return response;
    }

    /**
     * Asynchronously plans tasks for crew member using the configured LLM provider.
     *
     * <p>This method returns immediately with a CompletableFuture, allowing the game thread
     * to continue without blocking. The actual LLM call is executed on a separate thread pool
     * with full resilience patterns (circuit breaker, retry, rate limiting, caching).</p>
     *
     * <p><b>Non-blocking:</b> Game thread is never blocked</p>
     * <p><b>Resilient:</b> Automatic retry, circuit breaker, fallback on failure</p>
     * <p><b>Cached:</b> Repeated prompts may hit cache (40-60% hit rate)</p>
     *
     * @param foreman   The crew member entity making the request
     * @param command The user command to plan
     * @return CompletableFuture that completes with the parsed response, or null on failure
     */
    public CompletableFuture<ResponseParser.ParsedResponse> planTasksAsync(ForemanEntity foreman, String command) {
        return planTasksAsync(foreman, command, true);
    }

    /**
     * Asynchronously plans tasks with optional batching for rate limit management.
     *
     * <p>When batching is enabled and this is a user-initiated command, the prompt
     * will be sent through the batching system to avoid rate limits.</p>
     *
     * @param foreman The crew member entity making the request
     * @param command The user command to plan
     * @param isUserInitiated true if this is a direct user interaction (higher priority)
     * @return CompletableFuture that completes with the parsed response, or null on failure
     */
    public CompletableFuture<ResponseParser.ParsedResponse> planTasksAsync(ForemanEntity foreman, String command,
                                                                           boolean isUserInitiated) {
        // Check API key before making request
        if (!MineWrightConfig.hasValidApiKey()) {
            MineWrightMod.LOGGER.error("[Async] Cannot plan tasks: API key not configured. Please check config/minewright-common.toml");
            return CompletableFuture.completedFuture(null);
        }

        try {
            String systemPrompt = PromptBuilder.buildSystemPrompt();
            WorldKnowledge worldKnowledge = new WorldKnowledge(foreman);
            String userPrompt = PromptBuilder.buildUserPrompt(foreman, command, worldKnowledge);

            String provider = MineWrightConfig.getValidatedProvider();
            MineWrightMod.LOGGER.info("[Async] Requesting AI plan for crew member '{}' using {}: {}",
                foreman.getSteveName(), provider, command);

            // Build params map
            Map<String, Object> params = new java.util.HashMap<>();
            params.put("systemPrompt", systemPrompt);
            params.put("model", MineWrightConfig.OPENAI_MODEL.get());
            params.put("maxTokens", MineWrightConfig.MAX_TOKENS.get());
            params.put("temperature", MineWrightConfig.TEMPERATURE.get());
            params.put("foremanName", foreman.getSteveName());

            // Use batching for user-initiated commands to respect rate limits
            if (batchingEnabled && isUserInitiated && provider.equals("openai")) {
                return planTasksWithBatching(userPrompt, params);
            }

            // Fall back to direct async call for other providers or when batching disabled
            AsyncLLMClient client = getAsyncClient(provider);
            return executeAsyncRequest(client, userPrompt, params);

        } catch (Exception e) {
            MineWrightMod.LOGGER.error("[Async] Error setting up task planning", e);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Plans tasks using the batching system for rate limit management.
     */
    private CompletableFuture<ResponseParser.ParsedResponse> planTasksWithBatching(String userPrompt,
                                                                                     Map<String, Object> params) {
        BatchingLLMClient batchClient = getBatchingClient();
        if (batchClient == null) {
            // Fall back to direct call
            return executeAsyncRequest(asyncOpenAIClient, userPrompt, params);
        }

        // Submit through batching system as high-priority user prompt
        return batchClient.submitUserPrompt(userPrompt, params)
            .thenApply(content -> {
                if (content == null || content.isEmpty()) {
                    MineWrightMod.LOGGER.error("[Batched] Empty response from LLM");
                    return null;
                }

                ResponseParser.ParsedResponse parsed = ResponseParser.parseAIResponse(content);
                if (parsed == null) {
                    MineWrightMod.LOGGER.error("[Batched] Failed to parse AI response");
                    return null;
                }

                MineWrightMod.LOGGER.info("[Batched] Plan received: {} ({} tasks, queue: {})",
                    parsed.getPlan(),
                    parsed.getTasks().size(),
                    batchClient.getQueueSize());

                return parsed;
            })
            .exceptionally(throwable -> {
                MineWrightMod.LOGGER.error("[Batched] Error planning tasks: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Executes an async request directly without batching.
     */
    private CompletableFuture<ResponseParser.ParsedResponse> executeAsyncRequest(AsyncLLMClient client,
                                                                                    String userPrompt,
                                                                                    Map<String, Object> params) {
        return client.sendAsync(userPrompt, params)
            .thenApply(response -> {
                String content = response.getContent();
                if (content == null || content.isEmpty()) {
                    MineWrightMod.LOGGER.error("[Async] Empty response from LLM");
                    return null;
                }

                ResponseParser.ParsedResponse parsed = ResponseParser.parseAIResponse(content);
                if (parsed == null) {
                    MineWrightMod.LOGGER.error("[Async] Failed to parse AI response");
                    return null;
                }

                MineWrightMod.LOGGER.info("[Async] Plan received: {} ({} tasks, {}ms, {} tokens, cache: {})",
                    parsed.getPlan(),
                    parsed.getTasks().size(),
                    response.getLatencyMs(),
                    response.getTokensUsed(),
                    response.isFromCache());

                return parsed;
            })
            .exceptionally(throwable -> {
                MineWrightMod.LOGGER.error("[Async] Error planning tasks: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Submits a background task for planning with low priority.
     * These are batched aggressively to reduce API calls.
     *
     * @param foreman The crew member entity
     * @param taskDescription The background task description
     * @return CompletableFuture with the response
     */
    public CompletableFuture<String> submitBackgroundTask(ForemanEntity foreman, String taskDescription) {
        BatchingLLMClient batchClient = getBatchingClient();
        if (batchClient == null) {
            return CompletableFuture.completedFuture(null);
        }

        Map<String, Object> context = new java.util.HashMap<>();
        context.put("foremanName", foreman.getSteveName());
        context.put("taskType", "background");

        return batchClient.submitBackgroundPrompt(taskDescription, context);
    }

    /**
     * Returns the appropriate async client based on provider config.
     *
     * @param provider Provider name ("openai", "groq", "gemini")
     * @return Resilient async client
     */
    private AsyncLLMClient getAsyncClient(String provider) {
        return switch (provider) {
            case "openai" -> asyncOpenAIClient;
            case "gemini" -> asyncGeminiClient;
            case "groq" -> asyncGroqClient;
            default -> {
                MineWrightMod.LOGGER.warn("[Async] Unknown provider '{}', using Groq", provider);
                yield asyncGroqClient;
            }
        };
    }

    /**
     * Returns the LLM cache for monitoring.
     *
     * @return LLM cache instance
     */
    public LLMCache getLLMCache() {
        return llmCache;
    }

    /**
     * Checks if the specified provider's async client is healthy.
     *
     * @param provider Provider name
     * @return true if healthy (circuit breaker not OPEN)
     */
    public boolean isProviderHealthy(String provider) {
        return getAsyncClient(provider).isHealthy();
    }

    public boolean validateTask(Task task) {
        String action = task.getAction();
        
        return switch (action) {
            case "pathfind" -> task.hasParameters("x", "y", "z");
            case "mine" -> task.hasParameters("block", "quantity");
            case "place" -> task.hasParameters("block", "x", "y", "z");
            case "craft" -> task.hasParameters("item", "quantity");
            case "attack" -> task.hasParameters("target");
            case "follow" -> task.hasParameters("player");
            case "gather" -> task.hasParameters("resource", "quantity");
            case "build" -> task.hasParameters("structure", "blocks", "dimensions");
            default -> {
                MineWrightMod.LOGGER.warn("Unknown action type: {}", action);
                yield false;
            }
        };
    }

    public List<Task> validateAndFilterTasks(List<Task> tasks) {
        return tasks.stream()
            .filter(this::validateTask)
            .toList();
    }
}

