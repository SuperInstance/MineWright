package com.minewright.llm.batch;

import com.minewright.llm.async.AsyncLLMClient;
import com.minewright.llm.async.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * LLM client with intelligent batching to respect rate limits.
 *
 * <p>This is the main entry point for making LLM calls that automatically
 * batch prompts to avoid rate limits while maintaining responsiveness for
 * direct user interactions.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create the batching client
 * AsyncLLMClient underlyingClient = ...; // Your z.ai client
 * BatchingLLMClient batchClient = new BatchingLLMClient(underlyingClient);
 * batchClient.start();
 *
 * // Submit prompts
 * CompletableFuture<String> response = batchClient.submitUserPrompt(
 *     "Hello Steve!",
 *     Map.of("playerName", "Alex")
 * );
 *
 * // Background tasks are batched automatically
 * CompletableFuture<String> background = batchClient.submitBackgroundPrompt(
 *     "Analyze the area for resources",
 *     Map.of("area", "desert")
 * );
 * }</pre>
 *
 * <h2>Architecture</h2>
 * <pre>
 * User Request
 *      │
 *      ▼
 * ┌─────────────────────┐
│ BatchingLLMClient    │
 * ├─────────────────────┤
 * │                     │
 * │  ┌───────────────┐  │     ┌──────────────┐
 * │  │ PromptBatcher │  │────►│ z.ai API     │
 * │  └───────────────┘  │     │ (rate limit) │
 * │         │           │     └──────────────┘
 * │         ▼           │            │
 * │  ┌───────────────┐  │            │
 * │  │ Heartbeat     │◄─┼────────────┘
 * │  │ Scheduler     │  │   (feedback)
 * │  └───────────────┘  │
 * │                     │
 * └─────────────────────┘
 * </pre>
 *
 * @since 1.3.0
 */
public class BatchingLLMClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchingLLMClient.class);

    private final AsyncLLMClient underlyingClient;
    private final PromptBatcher batcher;
    private final HeartbeatScheduler heartbeat;

    private volatile boolean running;

    /**
     * Creates a new BatchingLLMClient.
     *
     * @param underlyingClient The underlying AsyncLLMClient to use for actual API calls
     */
    public BatchingLLMClient(AsyncLLMClient underlyingClient) {
        this.underlyingClient = underlyingClient;
        this.batcher = new PromptBatcher(this::sendBatch);
        this.heartbeat = new HeartbeatScheduler(batcher);
        this.running = false;

        LOGGER.info("BatchingLLMClient created");
    }

    /**
     * Starts the batching system.
     */
    public void start() {
        if (running) return;
        running = true;

        batcher.start();
        heartbeat.start();

        LOGGER.info("BatchingLLMClient started");
    }

    /**
     * Stops the batching system.
     */
    public void stop() {
        running = false;

        heartbeat.stop();
        batcher.stop();

        LOGGER.info("BatchingLLMClient stopped");
    }

    // === Public API ===

    /**
     * Submits a high-priority user interaction prompt.
     * These are sent quickly with minimal batching.
     *
     * @param prompt The prompt text
     * @param context Additional context
     * @return A future that will complete with the response
     */
    public CompletableFuture<String> submitUserPrompt(String prompt, Map<String, Object> context) {
        heartbeat.onUserActivity();
        return batcher.submitUserPrompt(prompt, context);
    }

    /**
     * Submits a prompt with specified type.
     *
     * @param prompt The prompt text
     * @param type The prompt type
     * @param context Additional context
     * @return A future that will complete with the response
     */
    public CompletableFuture<String> submit(String prompt, PromptBatcher.PromptType type,
                                           Map<String, Object> context) {
        if (type == PromptBatcher.PromptType.DIRECT_USER || type == PromptBatcher.PromptType.URGENT) {
            heartbeat.onUserActivity();
        }
        return batcher.submit(prompt, type, context);
    }

    /**
     * Submits a background task prompt.
     * These are batched aggressively to reduce API calls.
     *
     * @param prompt The prompt text
     * @param context Additional context
     * @return A future that will complete with the response
     */
    public CompletableFuture<String> submitBackgroundPrompt(String prompt, Map<String, Object> context) {
        return batcher.submitBackgroundPrompt(prompt, context);
    }

    /**
     * Submits a normal priority prompt.
     *
     * @param prompt The prompt text
     * @param context Additional context
     * @return A future that will complete with the response
     */
    public CompletableFuture<String> submitNormalPrompt(String prompt, Map<String, Object> context) {
        return batcher.submit(prompt, PromptBatcher.PromptType.NORMAL, context);
    }

    // === Batch Sending ===

    /**
     * Sends a compiled batch to the underlying LLM client.
     * This is the callback from PromptBatcher.
     */
    private void sendBatch(PromptBatcher.CompiledBatch batch) {
        LOGGER.debug("Sending batch {} with {} prompts", batch.id, batch.originalPrompts.size());

        // Prepare parameters
        Map<String, Object> params = new HashMap<>(batch.params);
        params.put("systemPrompt", batch.systemPrompt);

        // Send to underlying client
        underlyingClient.sendAsync(batch.userPrompt, params)
            .thenAccept(response -> handleSuccess(batch, response))
            .exceptionally(error -> {
                handleError(batch, error);
                return null;
            });
    }

    /**
     * Handles successful response from LLM.
     */
    private void handleSuccess(PromptBatcher.CompiledBatch batch, LLMResponse response) {
        String content = response.getContent();

        // Notify rate limit tracker
        batcher.onSuccess();
        heartbeat.onSuccess();

        // Parse batch response and complete individual futures
        if (batch.originalPrompts.size() == 1) {
            // Single prompt - direct response
            batch.completeAll(content);
        } else {
            // Multiple prompts - parse response
            Map<Integer, String> responses = parseBatchResponse(content);

            if (responses.isEmpty()) {
                // Couldn't parse - give full response to all
                batch.completeAll(content);
            } else {
                // Complete each prompt with its response
                int i = 1;
                for (PromptBatcher.BatchedPrompt prompt : batch.originalPrompts) {
                    String promptResponse = responses.getOrDefault(i, content);
                    batch.completePrompt(prompt.id, promptResponse);
                    i++;
                }
            }
        }

        LOGGER.debug("Batch {} completed successfully", batch.id);
    }

    /**
     * Handles error from LLM.
     */
    private void handleError(PromptBatcher.CompiledBatch batch, Throwable error) {
        LOGGER.error("Batch {} failed: {}", batch.id, error.getMessage());

        // Check for rate limit
        if (isRateLimitError(error)) {
            batcher.onRateLimitError();
            heartbeat.onError((Exception) error);

            // Could retry here if desired
        }

        // Complete futures exceptionally
        batch.completeExceptionally(error);
    }

    /**
     * Checks if an error is a rate limit error.
     */
    private boolean isRateLimitError(Throwable error) {
        String message = error.getMessage();
        return message != null && (
            message.contains("429") ||
            message.contains("rate limit") ||
            message.contains("Rate limit")
        );
    }

    /**
     * Parses a batch response into individual responses.
     *
     * <p>Expected format: [N] Response text</p>
     */
    private Map<Integer, String> parseBatchResponse(String content) {
        Map<Integer, String> responses = new HashMap<>();

        if (content == null || content.isEmpty()) {
            return responses;
        }

        // Split by [N] markers
        String[] lines = content.split("\n");
        StringBuilder currentResponse = new StringBuilder();
        int currentNum = 0;

        for (String line : lines) {
            // Check for [N] marker
            if (line.matches("^\\s*\\[\\d+\\].*")) {
                // Save previous response
                if (currentNum > 0 && currentResponse.length() > 0) {
                    responses.put(currentNum, currentResponse.toString().trim());
                }

                // Start new response
                currentNum = extractNumber(line);
                currentResponse = new StringBuilder();
                currentResponse.append(line.replaceFirst("^\\s*\\[\\d+\\]\\s*", ""));
            } else {
                currentResponse.append("\n").append(line);
            }
        }

        // Save last response
        if (currentNum > 0 && currentResponse.length() > 0) {
            responses.put(currentNum, currentResponse.toString().trim());
        }

        return responses;
    }

    /**
     * Extracts number from [N] format.
     */
    private int extractNumber(String line) {
        try {
            int start = line.indexOf('[');
            int end = line.indexOf(']');
            if (start >= 0 && end > start) {
                return Integer.parseInt(line.substring(start + 1, end).trim());
            }
        } catch (NumberFormatException e) {
            // Ignore
        }
        return 0;
    }

    // === Accessors ===

    /**
     * Gets the underlying PromptBatcher for direct access.
     * Use this for submitting background prompts.
     */
    public PromptBatcher getBatcher() {
        return batcher;
    }

    /**
     * Gets the HeartbeatScheduler.
     */
    public HeartbeatScheduler getHeartbeat() {
        return heartbeat;
    }

    // === Statistics ===

    public int getQueueSize() {
        return batcher.getQueueSize();
    }

    public boolean isIdleMode() {
        return heartbeat.isIdleMode();
    }

    public double getBackoffMultiplier() {
        return batcher.getBackoffMultiplier();
    }

    public boolean isRunning() {
        return running;
    }

    public String getStatusSummary() {
        return String.format(
            "BatchingLLMClient[queue=%d, mode=%s, backoff=%.1fx, running=%s]",
            getQueueSize(),
            isIdleMode() ? "IDLE" : "ACTIVE",
            getBackoffMultiplier(),
            running
        );
    }
}
