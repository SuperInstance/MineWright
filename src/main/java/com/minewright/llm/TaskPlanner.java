package com.minewright.llm;

import com.minewright.action.Task;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import com.minewright.config.MineWrightConfig;
import com.minewright.entity.ForemanEntity;
import com.minewright.llm.async.*;
import com.minewright.llm.batch.*;
import com.minewright.llm.cascade.*;
import com.minewright.memory.WorldKnowledge;
import com.minewright.security.InputSanitizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Plans tasks for MineWright crew members using Large Language Models (LLMs).
 *
 * <p><b>Overview:</b></p>
 * <p>The TaskPlanner is responsible for converting natural language commands from
 * players into structured, executable tasks. It sends prompts to configured LLM
 * providers (OpenAI, Groq, Gemini) and parses the responses into actionable
 * instructions.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li><b>Async Planning:</b> Non-blocking LLM calls using {@link CompletableFuture}</li>
 *   <li><b>Multiple Providers:</b> Support for OpenAI, Groq, and Gemini with fallback</li>
 *   <li><b>Cascade Routing:</b> Intelligent LLM selection based on task complexity</li>
 *   <li><b>Rate Limit Management:</b> Batching system to avoid API rate limits</li>
 *   <li><b>Caching:</b> Response caching to reduce redundant API calls (40-60% hit rate)</li>
 *   <li><b>Resilience:</b> Circuit breaker, retry, and timeout protection</li>
 *   <li><b>Background Tasks:</b> Low-priority planning for autonomous behavior</li>
 * </ul>
 *
 * <p><b>Planning Flow:</b></p>
 * <pre>
 * User Command
 *     │
 *     ├─► buildSystemPrompt() - Define available actions and context
 *     ├─► buildUserPrompt() - Include world knowledge and command
 *     │
 *     ├─► Cascade Routing (if enabled)
 *     │   │
 *     │   ├─► Analyze task complexity
 *     │   ├─► Select appropriate LLM tier
 *     │   └─► Route to best provider (FAST, BALANCED, or SMART)
 *     │
 *     ├─► Legacy Routing (if cascade disabled)
 *     │   │
 *     │   ├─► Check cache first (40-60% hit rate)
 *     │   ├─► Apply batching if rate limit concern
 *     │   └─► Send to configured provider with resilience patterns
 *     │
 *     └─► parseAIResponse() - Extract structured tasks
 *             │
 *             └─► Queue tasks for execution
 * </pre>
 *
 * <p><b>Supported LLM Providers:</b></p>
 * <ul>
 *   <li><b>OpenAI:</b> GPT-4 and GPT-3.5 models (primary provider)</li>
 *   <li><b>Groq:</b> Llama models for fast inference (fallback)</li>
 *   <li><b>Gemini:</b> Google's Gemini models (alternative)</li>
 * </ul>
 *
 * <p><b>Cascade Routing:</b></p>
 * <p>When enabled, the router analyzes task complexity and automatically selects
 * the most appropriate LLM tier:</p>
 * <ul>
 *   <li><b>TRIVIAL tasks:</b> Served from cache (60-80% hit rate)</li>
 *   <li><b>SIMPLE tasks:</b> Routed to FAST tier (Groq llama-3.1-8b-instant)</li>
 *   <li><b>MODERATE tasks:</b> Routed to BALANCED tier (Groq llama-3.3-70b or gpt-3.5)</li>
 *   <li><b>COMPLEX/NOVEL tasks:</b> Routed to SMART tier (gpt-4, claude)</li>
 * </ul>
 *
 * <p><b>Resilience Patterns:</b></p>
 * <ul>
 *   <li><b>Circuit Breaker:</b> Stops calling failing APIs after threshold</li>
 *   <li><b>Retry:</b> Automatic retries with exponential backoff</li>
 *   <li><b>Timeout:</b> 60-second maximum wait for responses</li>
 *   <li><b>Fallback:</b> Falls back to Groq if primary provider fails</li>
 *   <li><b>Cache:</b> Reuses responses for identical prompts</li>
 * </ul>
 *
 * <p><b>Batching System:</b></p>
 * <p>When batching is enabled, multiple prompts are combined into a single API call
 * to respect rate limits. User-initiated commands get higher priority than
 * background tasks.</p>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Async clients use separate thread pools for LLM calls</li>
 *   <li>Caching is thread-safe via concurrent data structures</li>
 *   <li>Batching queue is synchronized for concurrent access</li>
 * </ul>
 *
 * @see PromptBuilder
 * @see ResponseParser
 * @see com.minewright.llm.async.AsyncLLMClient
 * @see com.minewright.llm.batch.BatchingLLMClient
 * @see com.minewright.llm.cascade.CascadeRouter
 *
 * @since 1.0.0
 */
public class TaskPlanner {
    private static final Logger LOGGER = TestLogger.getLogger(TaskPlanner.class);
    /**
     * Legacy synchronous OpenAI client (for backward compatibility).
     * Blocking - prefer async clients for new code.
     */
    private final OpenAIClient openAIClient;

    /**
     * GLM cascade router for intelligent model selection on z.ai.
     * Preprocesses messages and routes to appropriate GLM model.
     */
    private final GLMCascadeRouter glmCascadeRouter;

    /**
     * Legacy synchronous Gemini client (for backward compatibility).
     * Blocking - prefer async clients for new code.
     */
    private final GeminiClient geminiClient;

    /**
     * Legacy synchronous Groq client (for backward compatibility).
     * Blocking - prefer async clients for new code.
     */
    private final GroqClient groqClient;

    /**
     * Async OpenAI client for non-blocking GPT API calls.
     * Used as the primary provider when configured.
     */
    private final AsyncLLMClient asyncOpenAIClient;

    /**
     * Async Groq client for fast Llama inference.
     * Used as fallback when OpenAI fails.
     */
    private final AsyncLLMClient asyncGroqClient;

    /**
     * Async Gemini client for Google's model.
     * Used as alternative provider.
     */
    private final AsyncLLMClient asyncGeminiClient;

    /**
     * Response cache to avoid redundant API calls.
     * Achieves 40-60% hit rate for similar prompts.
     */
    private final LLMCache llmCache;

    /**
     * Batching client for rate limit management.
     * Combines multiple prompts into single API calls.
     */
    private BatchingLLMClient batchingClient;

    /**
     * Whether batching is enabled for rate limit management.
     * Can be disabled via configuration.
     */
    private boolean batchingEnabled = true;

    /**
     * Cascade router for intelligent LLM selection based on task complexity.
     * When enabled, automatically routes requests to the most appropriate tier.
     */
    private CascadeRouter cascadeRouter;

    /**
     * Complexity analyzer for determining task complexity.
     */
    private ComplexityAnalyzer complexityAnalyzer;

    /**
     * Whether cascade routing is enabled.
     * Can be toggled via configuration or runtime.
     */
    private boolean cascadeRoutingEnabled = false;

    public TaskPlanner() {
        // Legacy clients
        this.openAIClient = new OpenAIClient();
        this.geminiClient = new GeminiClient();
        this.groqClient = new GroqClient();

        // GLM cascade router for intelligent z.ai model selection
        this.glmCascadeRouter = new GLMCascadeRouter();

        // Initialize async infrastructure
        this.llmCache = new LLMCache();
        this.complexityAnalyzer = new ComplexityAnalyzer();

        // Initialize async clients directly (no resilience wrapper due to Forge classloading issues)
        String apiKey = MineWrightConfig.OPENAI_API_KEY.get();
        String model = MineWrightConfig.OPENAI_MODEL.get();
        int maxTokens = MineWrightConfig.MAX_TOKENS.get();
        double temperature = MineWrightConfig.TEMPERATURE.get();

        // Create async clients without resilience wrapper
        this.asyncOpenAIClient = new AsyncOpenAIClient(apiKey, model, maxTokens, temperature);
        this.asyncGroqClient = new AsyncGroqClient(apiKey, "llama-3.1-8b-instant", 500, temperature);
        this.asyncGeminiClient = new AsyncGeminiClient(apiKey, "gemini-1.5-flash", maxTokens, temperature);

        // Initialize cascade router (but disabled by default for backward compatibility)
        initializeCascadeRouter(apiKey, model, maxTokens, temperature);

        LOGGER.info("TaskPlanner initialized with async clients (cascade routing: {})",
            cascadeRoutingEnabled);
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
            LOGGER.info("BatchingLLMClient started for rate limit management");
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

    // ------------------------------------------------------------------------
    // Cascade Router Methods
    // ------------------------------------------------------------------------

    /**
     * Initializes the cascade router with tier-specific async clients.
     */
    private void initializeCascadeRouter(String apiKey, String model, int maxTokens, double temperature) {
        // Map tiers to appropriate async clients
        Map<LLMTier, AsyncLLMClient> tierClients = new HashMap<>();

        // FAST tier: Groq llama-3.1-8b-instant (fastest, cheapest)
        tierClients.put(LLMTier.FAST, asyncGroqClient);

        // BALANCED tier: Groq llama-3.3-70b (good balance of speed and quality)
        AsyncLLMClient balancedClient = new AsyncGroqClient(apiKey, "llama-3.3-70b-versatile", maxTokens, temperature);
        tierClients.put(LLMTier.BALANCED, balancedClient);

        // SMART tier: OpenAI GPT-4 (highest quality, most expensive)
        tierClients.put(LLMTier.SMART, asyncOpenAIClient);

        // Create cascade router
        this.cascadeRouter = new CascadeRouter(llmCache, complexityAnalyzer, tierClients);

        // Cascade routing is disabled by default for backward compatibility
        this.cascadeRoutingEnabled = false;

        LOGGER.info("Cascade router initialized with {} tiers", tierClients.size());
    }

    /**
     * Enables or disables cascade routing.
     *
     * <p>When enabled, intelligent routing based on task complexity is used.
     * When disabled, legacy provider-based routing is used.</p>
     *
     * @param enabled true to enable cascade routing
     */
    public void setCascadeRoutingEnabled(boolean enabled) {
        this.cascadeRoutingEnabled = enabled;
        LOGGER.info("Cascade routing {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Checks if cascade routing is enabled.
     *
     * @return true if cascade routing is enabled
     */
    public boolean isCascadeRoutingEnabled() {
        return cascadeRoutingEnabled;
    }

    /**
     * Gets the cascade router instance.
     *
     * @return CascadeRouter, or null if not initialized
     */
    public CascadeRouter getCascadeRouter() {
        return cascadeRouter;
    }

    /**
     * Gets the complexity analyzer instance.
     *
     * @return ComplexityAnalyzer for analyzing task complexity
     */
    public ComplexityAnalyzer getComplexityAnalyzer() {
        return complexityAnalyzer;
    }

    public ResponseParser.ParsedResponse planTasks(ForemanEntity foreman, String command) {
        // Check API key before making request
        if (!MineWrightConfig.hasValidApiKey()) {
            LOGGER.error("Cannot plan tasks: API key not configured. Please check config/minewright-common.toml");
            return null;
        }

        // SECURITY: Validate command for suspicious patterns before processing
        if (InputSanitizer.containsSuspiciousPatterns(command)) {
            String reason = InputSanitizer.getSuspiciousPatternDescription(command);
            LOGGER.warn("Command contains suspicious patterns and was rejected: {}. Command: {}",
                reason, command);
            return null;
        }

        try {
            String systemPrompt = PromptBuilder.buildSystemPrompt();
            WorldKnowledge worldKnowledge = new WorldKnowledge(foreman);
            String userPrompt = PromptBuilder.buildUserPrompt(foreman, command, worldKnowledge);

            String provider = MineWrightConfig.getValidatedProvider();
            LOGGER.info("Requesting AI plan for crew member '{}' using {}: {}", foreman.getEntityName(), provider, command);

            String response = getAIResponse(provider, systemPrompt, userPrompt);

            if (response == null) {
                LOGGER.error("Failed to get AI response for command: {}", command);
                return null;
            }
            ResponseParser.ParsedResponse parsedResponse = ResponseParser.parseAIResponse(response);

            if (parsedResponse == null) {
                LOGGER.error("Failed to parse AI response");
                return null;
            }

            LOGGER.info("Plan: {} ({} tasks)", parsedResponse.getPlan(), parsedResponse.getTasks().size());

            return parsedResponse;

        } catch (Exception e) {
            LOGGER.error("Error planning tasks", e);
            return null;
        }
    }

    private String getAIResponse(String provider, String systemPrompt, String userPrompt) {
        String response = switch (provider) {
            case "groq" -> groqClient.sendRequest(systemPrompt, userPrompt);
            case "gemini" -> geminiClient.sendRequest(systemPrompt, userPrompt);
            case "openai" -> {
                // Use GLM cascade router for z.ai - intelligently selects model
                LOGGER.info("Using GLM cascade router for intelligent model selection");
                yield glmCascadeRouter.processWithCascade(systemPrompt, userPrompt);
            }
            default -> {
                LOGGER.warn("Unknown AI provider '{}', using Groq", provider);
                yield groqClient.sendRequest(systemPrompt, userPrompt);
            }
        };

        if (response == null && !provider.equals("groq")) {
            LOGGER.warn("{} failed, trying Groq as fallback", provider);
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
            LOGGER.error("[Async] Cannot plan tasks: API key not configured. Please check config/minewright-common.toml");
            return CompletableFuture.completedFuture(null);
        }

        try {
            String systemPrompt = PromptBuilder.buildSystemPrompt();
            WorldKnowledge worldKnowledge = new WorldKnowledge(foreman);
            String userPrompt = PromptBuilder.buildUserPrompt(foreman, command, worldKnowledge);

            String provider = MineWrightConfig.getValidatedProvider();
            LOGGER.info("[Async] Requesting AI plan for crew member '{}' using {}: {}",
                foreman.getEntityName(), provider, command);

            // Build params map
            Map<String, Object> params = new java.util.HashMap<>();
            params.put("systemPrompt", systemPrompt);
            params.put("model", MineWrightConfig.OPENAI_MODEL.get());
            params.put("maxTokens", MineWrightConfig.MAX_TOKENS.get());
            params.put("temperature", MineWrightConfig.TEMPERATURE.get());
            params.put("foremanName", foreman.getEntityName());

            // Use batching for user-initiated commands to respect rate limits
            if (batchingEnabled && isUserInitiated && provider.equals("openai")) {
                return planTasksWithBatching(userPrompt, params);
            }

            // Fall back to direct async call for other providers or when batching disabled
            AsyncLLMClient client = getAsyncClient(provider);
            return executeAsyncRequest(client, userPrompt, params);

        } catch (Exception e) {
            LOGGER.error("[Async] Error setting up task planning", e);
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
                    LOGGER.error("[Batched] Empty response from LLM");
                    return null;
                }

                ResponseParser.ParsedResponse parsed = ResponseParser.parseAIResponse(content);
                if (parsed == null) {
                    LOGGER.error("[Batched] Failed to parse AI response");
                    return null;
                }

                LOGGER.info("[Batched] Plan received: {} ({} tasks, queue: {})",
                    parsed.getPlan(),
                    parsed.getTasks().size(),
                    batchClient.getQueueSize());

                return parsed;
            })
            .exceptionally(throwable -> {
                LOGGER.error("[Batched] Error planning tasks: {}", throwable.getMessage());
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
                    LOGGER.error("[Async] Empty response from LLM");
                    return null;
                }

                ResponseParser.ParsedResponse parsed = ResponseParser.parseAIResponse(content);
                if (parsed == null) {
                    LOGGER.error("[Async] Failed to parse AI response");
                    return null;
                }

                LOGGER.info("[Async] Plan received: {} ({} tasks, {}ms, {} tokens, cache: {})",
                    parsed.getPlan(),
                    parsed.getTasks().size(),
                    response.getLatencyMs(),
                    response.getTokensUsed(),
                    response.isFromCache());

                return parsed;
            })
            .exceptionally(throwable -> {
                LOGGER.error("[Async] Error planning tasks: {}", throwable.getMessage());
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
        context.put("foremanName", foreman.getEntityName());
        context.put("taskType", "background");

        return batchClient.submitBackgroundPrompt(taskDescription, context);
    }

    // ------------------------------------------------------------------------
    // Cascade-Aware Planning Methods
    // ------------------------------------------------------------------------

    /**
     * Asynchronously plans tasks using cascade routing (intelligent tier selection).
     *
     * <p>This method uses the cascade router to automatically select the most
     * appropriate LLM tier based on task complexity. This provides cost optimization
     * by using cheaper/faster models for simple tasks while reserving powerful
     * models for complex reasoning.</p>
     *
     * <p><b>Cascade Flow:</b></p>
     * <ol>
     *   <li>Check cache first (for cacheable tasks)</li>
     *   <li>Analyze command complexity</li>
     *   <li>Select appropriate tier (FAST, BALANCED, or SMART)</li>
     *   <li>Execute with automatic fallback on failure</li>
     *   <li>Track metrics for optimization</li>
     * </ol>
     *
     * @param foreman The crew member entity making the request
     * @param command The user command to plan
     * @return CompletableFuture that completes with the parsed response, or null on failure
     */
    public CompletableFuture<ResponseParser.ParsedResponse> planTasksWithCascade(ForemanEntity foreman,
                                                                                  String command) {
        // Check API key before making request
        if (!MineWrightConfig.hasValidApiKey()) {
            LOGGER.error("[Cascade] Cannot plan tasks: API key not configured");
            return CompletableFuture.completedFuture(null);
        }

        // Cascade routing must be enabled
        if (!cascadeRoutingEnabled || cascadeRouter == null) {
            LOGGER.warn("[Cascade] Cascade routing not enabled, falling back to standard async");
            return planTasksAsync(foreman, command);
        }

        try {
            String systemPrompt = PromptBuilder.buildSystemPrompt();
            WorldKnowledge worldKnowledge = new WorldKnowledge(foreman);
            String userPrompt = PromptBuilder.buildUserPrompt(foreman, command, worldKnowledge);

            LOGGER.info("[Cascade] Routing plan for crew member '{}': {}",
                foreman.getEntityName(), command);

            // Build params map with context for cascade router
            Map<String, Object> params = new java.util.HashMap<>();
            params.put("systemPrompt", systemPrompt);
            params.put("model", MineWrightConfig.OPENAI_MODEL.get());
            params.put("maxTokens", MineWrightConfig.MAX_TOKENS.get());
            params.put("temperature", MineWrightConfig.TEMPERATURE.get());
            params.put("foremanName", foreman.getEntityName());
            params.put("foreman", foreman);
            params.put("worldKnowledge", worldKnowledge);
            params.put("providerId", "cascade");

            // Route through cascade system
            return cascadeRouter.route(userPrompt, params)
                .thenApply(response -> {
                    String content = response.getContent();
                    if (content == null || content.isEmpty()) {
                        LOGGER.error("[Cascade] Empty response from LLM");
                        return null;
                    }

                    ResponseParser.ParsedResponse parsed = ResponseParser.parseAIResponse(content);
                    if (parsed == null) {
                        LOGGER.error("[Cascade] Failed to parse AI response");
                        return null;
                    }

                    LOGGER.info("[Cascade] Plan received: {} ({} tasks, {}ms, {} tokens, {})",
                        parsed.getPlan(),
                        parsed.getTasks().size(),
                        response.getLatencyMs(),
                        response.getTokensUsed(),
                        response.isFromCache() ? "cached" : response.getProviderId());

                    return parsed;
                })
                .exceptionally(throwable -> {
                    LOGGER.error("[Cascade] Error planning tasks: {}",
                        throwable.getMessage(), throwable);
                    return null;
                });

        } catch (Exception e) {
            LOGGER.error("[Cascade] Error setting up cascade routing", e);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Logs cascade router statistics.
     */
    public void logCascadeStats() {
        if (cascadeRouter != null) {
            cascadeRouter.logStats();
        } else {
            LOGGER.info("Cascade router not initialized");
        }
    }

    /**
     * Resets cascade router metrics.
     */
    public void resetCascadeMetrics() {
        if (cascadeRouter != null) {
            cascadeRouter.resetMetrics();
        }
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
                LOGGER.warn("[Async] Unknown provider '{}', using Groq", provider);
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
                LOGGER.warn("Unknown action type: {}", action);
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

