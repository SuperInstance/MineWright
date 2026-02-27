package com.minewright.llm.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Batches LLM prompts to reduce API calls and respect rate limits.
 *
 * <p>This system accumulates prompts over time and sends them in smarter,
 * larger batches rather than many small individual requests. This helps:</p>
 *
 * <ul>
 *   <li>Avoid API rate limits (429 errors)</li>
 *   <li>Reduce total API calls</li>
 *   <li>Combine related prompts into single smarter requests</li>
 *   <li>Prioritize direct user interactions</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │                    PromptBatcher                            │
 * ├─────────────────────────────────────────────────────────────┤
 * │                                                              │
 * │  ┌──────────────┐     ┌──────────────┐     ┌─────────────┐ │
 * │  │   Incoming   │────►│   Priority   │────►│   Batch     │ │
 * │  │   Prompts    │     │    Queue     │     │   Buffer    │ │
 * │  └──────────────┘     └──────────────┘     └─────────────┘ │
 * │                                                    │        │
 * │                         ┌──────────────────────────┘        │
 * │                         ▼                                   │
 * │  ┌──────────────────────────────────────────────────────┐  │
 * │  │              Batch Compilation                        │  │
 * │  │  - Merge related prompts                              │  │
 * │  │  - Add context from local preprocessor                │  │
 * │  │  - Optimize for single API call                       │  │
 * │  └──────────────────────────────────────────────────────┘  │
 * │                         │                                   │
 * │                         ▼                                   │
 * │  ┌──────────────────────────────────────────────────────┐  │
 * │  │              Rate Limit Manager                       │  │
 * │  │  - Track API usage                                    │  │
 * │  │  - Enforce minimum intervals                          │  │
 * │  │  - Back off on 429 errors                             │  │
 * │  └──────────────────────────────────────────────────────┘  │
 * │                                                              │
 * └─────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @since 1.3.0
 */
public class PromptBatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptBatcher.class);

    // === Configuration ===

    /**
     * Minimum time between batch sends (milliseconds).
     * Adjust based on z.ai rate limits.
     */
    private static final long MIN_BATCH_INTERVAL_MS = 2000; // 2 seconds

    /**
     * Maximum time to hold prompts before forcing a batch send.
     */
    private static final long MAX_BATCH_WAIT_MS = 10000; // 10 seconds

    /**
     * Maximum prompts to include in a single batch.
     */
    private static final int MAX_BATCH_SIZE = 5;

    /**
     * Minimum prompts before considering a batch (unless urgent).
     */
    private static final int MIN_BATCH_SIZE = 2;

    // === State ===

    /**
     * Priority queue for pending prompts.
     */
    private final PriorityBlockingQueue<BatchedPrompt> promptQueue;

    /**
     * Current batch being assembled.
     */
    private final List<BatchedPrompt> currentBatch;

    /**
     * Tracks last API call time for rate limiting.
     */
    private volatile Instant lastSendTime;

    /**
     * Number of consecutive rate limit errors.
     */
    private final AtomicInteger consecutiveRateLimitErrors;

    /**
     * Current backoff multiplier for rate limiting.
     */
    private volatile double backoffMultiplier;

    /**
     * Callback for when a batch is ready to send.
     */
    private final Consumer<CompiledBatch> sendCallback;

    /**
     * Executor for background processing.
     */
    private final ScheduledExecutorService scheduler;

    /**
     * Local preprocessor for compiling prompts.
     */
    private final LocalPreprocessor preprocessor;

    /**
     * Whether the batcher is running.
     */
    private volatile boolean running;

    /**
     * Creates a new PromptBatcher.
     *
     * @param sendCallback Callback when batch is ready to send
     */
    public PromptBatcher(Consumer<CompiledBatch> sendCallback) {
        this.promptQueue = new PriorityBlockingQueue<>();
        this.currentBatch = new ArrayList<>();
        this.lastSendTime = Instant.MIN;
        this.consecutiveRateLimitErrors = new AtomicInteger(0);
        this.backoffMultiplier = 1.0;
        this.sendCallback = sendCallback;
        this.preprocessor = new LocalPreprocessor();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "PromptBatcher-Scheduler")
        );
        this.running = false;

        LOGGER.info("PromptBatcher initialized (min interval: {}ms, max batch: {})",
            MIN_BATCH_INTERVAL_MS, MAX_BATCH_SIZE);
    }

    /**
     * Starts the batch processing loop.
     */
    public void start() {
        if (running) return;
        running = true;

        // Schedule periodic batch processing
        scheduler.scheduleAtFixedRate(
            this::processBatch,
            100, // Initial delay
            500, // Check every 500ms
            TimeUnit.MILLISECONDS
        );

        LOGGER.info("PromptBatcher started");
    }

    /**
     * Stops the batch processing loop.
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Flush remaining prompts
        flushBatch();

        LOGGER.info("PromptBatcher stopped");
    }

    // === Public API ===

    /**
     * Submits a prompt for batched processing.
     *
     * @param prompt The prompt text
     * @param type The prompt type
     * @param context Additional context
     * @return A future that will complete with the response
     */
    public CompletableFuture<String> submit(String prompt, PromptType type, Map<String, Object> context) {
        BatchedPrompt batched = new BatchedPrompt(prompt, type, context);
        promptQueue.offer(batched);

        LOGGER.debug("Submitted prompt (type={}, queue={})", type, promptQueue.size());

        // If urgent/immediate, trigger quick processing
        if (type == PromptType.DIRECT_USER || type == PromptType.URGENT) {
            scheduler.execute(this::processUrgent);
        }

        return batched.getFuture();
    }

    /**
     * Submits a high-priority user interaction prompt.
     * These bypass normal batching and are sent quickly.
     *
     * @param prompt The prompt text
     * @param context Additional context
     * @return A future that will complete with the response
     */
    public CompletableFuture<String> submitUserPrompt(String prompt, Map<String, Object> context) {
        return submit(prompt, PromptType.DIRECT_USER, context);
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
        return submit(prompt, PromptType.BACKGROUND, context);
    }

    /**
     * Called when a rate limit error is received.
     * Implements exponential backoff.
     */
    public void onRateLimitError() {
        int errors = consecutiveRateLimitErrors.incrementAndGet();
        backoffMultiplier = Math.min(10.0, Math.pow(1.5, errors));

        LOGGER.warn("Rate limit hit (consecutive: {}, backoff: {}x)",
            errors, backoffMultiplier);
    }

    /**
     * Called when a successful API call completes.
     * Resets backoff.
     */
    public void onSuccess() {
        consecutiveRateLimitErrors.set(0);
        backoffMultiplier = 1.0;
        lastSendTime = Instant.now();
    }

    // === Batch Processing ===

    /**
     * Main batch processing loop - called periodically.
     */
    private void processBatch() {
        if (!running || promptQueue.isEmpty()) {
            return;
        }

        // Check rate limit
        if (!canSendNow()) {
            LOGGER.debug("Rate limited, waiting (backoff: {}x)", backoffMultiplier);
            return;
        }

        // Check if we have enough prompts or waited long enough
        boolean hasEnoughPrompts = promptQueue.size() >= MIN_BATCH_SIZE;
        boolean waitedLongEnough = hasWaitedLongEnough();

        if (!hasEnoughPrompts && !waitedLongEnough) {
            return;
        }

        // Collect prompts for batch
        List<BatchedPrompt> toProcess = collectBatch();

        if (toProcess.isEmpty()) {
            return;
        }

        // Compile and send
        CompiledBatch batch = compileBatch(toProcess);

        LOGGER.info("Sending batch: {} prompts (types: {})",
            toProcess.size(),
            toProcess.stream().map(p -> p.type.toString()).toList());

        sendCallback.accept(batch);
    }

    /**
     * Processes urgent prompts immediately.
     */
    private void processUrgent() {
        // Collect all urgent/direct user prompts
        List<BatchedPrompt> urgent = new ArrayList<>();
        List<BatchedPrompt> all = new ArrayList<>();
        promptQueue.drainTo(all);

        // Filter to get urgent ones, put others back
        for (BatchedPrompt p : all) {
            if (p.type == PromptType.DIRECT_USER || p.type == PromptType.URGENT) {
                urgent.add(p);
            } else {
                promptQueue.offer(p);
            }
        }

        if (urgent.isEmpty()) {
            return;
        }

        // Wait minimum interval if needed
        waitForMinInterval();

        // Compile and send
        CompiledBatch batch = compileBatch(urgent);

        LOGGER.info("Sending urgent batch: {} prompts", urgent.size());

        sendCallback.accept(batch);
    }

    /**
     * Flushes the current batch immediately.
     */
    private void flushBatch() {
        if (promptQueue.isEmpty()) {
            return;
        }

        List<BatchedPrompt> all = new ArrayList<>();
        promptQueue.drainTo(all);

        if (!all.isEmpty()) {
            CompiledBatch batch = compileBatch(all);
            sendCallback.accept(batch);
        }
    }

    /**
     * Collects prompts for the next batch.
     */
    private List<BatchedPrompt> collectBatch() {
        List<BatchedPrompt> batch = new ArrayList<>();

        // Take up to MAX_BATCH_SIZE prompts
        promptQueue.drainTo(batch, MAX_BATCH_SIZE);

        return batch;
    }

    /**
     * Compiles multiple prompts into a single smart batch.
     */
    private CompiledBatch compileBatch(List<BatchedPrompt> prompts) {
        // Use local preprocessor to optimize
        CompiledBatch compiled = preprocessor.compileBatch(prompts);

        return compiled;
    }

    // === Rate Limiting ===

    /**
     * Checks if we can send now based on rate limits.
     */
    private boolean canSendNow() {
        if (lastSendTime == Instant.MIN) {
            return true;
        }

        long adjustedInterval = (long) (MIN_BATCH_INTERVAL_MS * backoffMultiplier);
        long elapsed = java.time.Duration.between(lastSendTime, Instant.now()).toMillis();

        return elapsed >= adjustedInterval;
    }

    /**
     * Checks if we've waited long enough to force a send.
     */
    private boolean hasWaitedLongEnough() {
        if (lastSendTime == Instant.MIN) {
            return false;
        }

        long elapsed = java.time.Duration.between(lastSendTime, Instant.now()).toMillis();
        return elapsed >= MAX_BATCH_WAIT_MS;
    }

    /**
     * Waits for minimum interval if needed.
     */
    private void waitForMinInterval() {
        if (lastSendTime == Instant.MIN) {
            return;
        }

        long adjustedInterval = (long) (MIN_BATCH_INTERVAL_MS * backoffMultiplier);
        long elapsed = java.time.Duration.between(lastSendTime, Instant.now()).toMillis();
        long remaining = adjustedInterval - elapsed;

        if (remaining > 0) {
            try {
                Thread.sleep(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // === Getters ===

    public int getQueueSize() {
        return promptQueue.size();
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public boolean isRunning() {
        return running;
    }

    // === Inner Classes ===

    /**
     * Types of prompts with different batching priorities.
     */
    public enum PromptType {
        /**
         * Direct user interaction - highest priority, minimal batching.
         */
        DIRECT_USER(100),

        /**
         * Urgent system prompt - high priority.
         */
        URGENT(80),

        /**
         * Normal priority prompt.
         */
        NORMAL(50),

        /**
         * Background task - low priority, aggressive batching.
         */
        BACKGROUND(20),

        /**
         * Deferrable analytics/logging - lowest priority.
         */
        DEFERRABLE(10);

        private final int priority;

        PromptType(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    /**
     * A single prompt awaiting batch processing.
     */
    public static class BatchedPrompt implements Comparable<BatchedPrompt> {
        public final String id;
        public final String prompt;
        public final PromptType type;
        public final Map<String, Object> context;
        public final Instant createdAt;
        public final CompletableFuture<String> future;

        public BatchedPrompt(String prompt, PromptType type, Map<String, Object> context) {
            this.id = UUID.randomUUID().toString().substring(0, 8);
            this.prompt = prompt;
            this.type = type;
            this.context = context != null ? context : new HashMap<>();
            this.createdAt = Instant.now();
            this.future = new CompletableFuture<>();
        }

        public CompletableFuture<String> getFuture() {
            return future;
        }

        @Override
        public int compareTo(BatchedPrompt other) {
            // Higher priority first
            return Integer.compare(other.type.getPriority(), this.type.getPriority());
        }
    }

    /**
     * A compiled batch ready to send.
     */
    public static class CompiledBatch {
        public final String id;
        public final String systemPrompt;
        public final String userPrompt;
        public final List<BatchedPrompt> originalPrompts;
        public final Map<String, Object> params;
        public final Instant compiledAt;

        public CompiledBatch(String systemPrompt, String userPrompt,
                           List<BatchedPrompt> originalPrompts, Map<String, Object> params) {
            this.id = UUID.randomUUID().toString().substring(0, 8);
            this.systemPrompt = systemPrompt;
            this.userPrompt = userPrompt;
            this.originalPrompts = new ArrayList<>(originalPrompts);
            this.params = params != null ? params : new HashMap<>();
            this.compiledAt = Instant.now();
        }

        /**
         * Completes all futures in this batch with the response.
         */
        public void completeAll(String response) {
            for (BatchedPrompt prompt : originalPrompts) {
                prompt.future.complete(response);
            }
        }

        /**
         * Completes all futures in this batch with an exception.
         */
        public void completeExceptionally(Throwable error) {
            for (BatchedPrompt prompt : originalPrompts) {
                prompt.future.completeExceptionally(error);
            }
        }

        /**
         * Completes a specific prompt's future.
         */
        public void completePrompt(String promptId, String response) {
            for (BatchedPrompt prompt : originalPrompts) {
                if (prompt.id.equals(promptId)) {
                    prompt.future.complete(response);
                    return;
                }
            }
        }
    }
}
