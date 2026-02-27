package com.minewright.llm.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Heartbeat scheduler for batching LLM prompts when user is inactive.
 *
 * <p>This system coordinates with PromptBatcher to send batches on a regular
 * "heartbeat" schedule when the user isn't directly interacting. This allows
 * accumulating prompts during idle time and sending them efficiently.</p>
 *
 * <h2>Schedule Strategy</h2>
 * <ul>
 *   <li><b>Active mode:</b> When user is interacting, send prompts quickly</li>
 *   <li><b>Idle mode:</b> When user is inactive, accumulate and send on heartbeat</li>
 *   <li><b>Adaptive:</b> Adjusts timing based on API feedback</li>
 * </ul>
 *
 * @since 1.3.0
 */
public class HeartbeatScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatScheduler.class);

    // === Configuration ===

    /**
     * Normal heartbeat interval when user is idle.
     */
    private static final long IDLE_HEARTBEAT_MS = 5000; // 5 seconds

    /**
     * Faster heartbeat when user is active.
     */
    private static final long ACTIVE_HEARTBEAT_MS = 1000; // 1 second

    /**
     * Time without user activity before switching to idle mode.
     */
    private static final long ACTIVITY_TIMEOUT_MS = 3000; // 3 seconds

    /**
     * Maximum time to hold prompts before forced send.
     */
    private static final long MAX_HOLD_TIME_MS = 15000; // 15 seconds

    // === State ===

    private final PromptBatcher batcher;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService callbackExecutor;

    private volatile Instant lastUserActivity;
    private volatile Instant lastHeartbeat;
    private final AtomicBoolean isIdle;
    private final AtomicInteger consecutiveErrors;
    private final AtomicLong currentIntervalMs;

    private volatile boolean running;
    private ScheduledFuture<?> heartbeatTask;

    /**
     * Creates a new HeartbeatScheduler.
     *
     * @param batcher The PromptBatcher to coordinate with
     */
    public HeartbeatScheduler(PromptBatcher batcher) {
        this.batcher = batcher;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "HeartbeatScheduler")
        );
        this.callbackExecutor = Executors.newCachedThreadPool(
            r -> {
                Thread t = new Thread(r, "Heartbeat-Callback");
                t.setDaemon(true);
                return t;
            }
        );

        this.lastUserActivity = Instant.now();
        this.lastHeartbeat = Instant.now();
        this.isIdle = new AtomicBoolean(true);
        this.consecutiveErrors = new AtomicInteger(0);
        this.currentIntervalMs = new AtomicLong(IDLE_HEARTBEAT_MS);
        this.running = false;

        LOGGER.info("HeartbeatScheduler created (idle: {}ms, active: {}ms)",
            IDLE_HEARTBEAT_MS, ACTIVE_HEARTBEAT_MS);
    }

    /**
     * Starts the heartbeat scheduler.
     */
    public synchronized void start() {
        if (running) return;
        running = true;

        // Start heartbeat task
        heartbeatTask = scheduler.scheduleAtFixedRate(
            this::onHeartbeat,
            100, // Initial delay
            500, // Check every 500ms
            TimeUnit.MILLISECONDS
        );

        LOGGER.info("HeartbeatScheduler started");
    }

    /**
     * Stops the heartbeat scheduler.
     */
    public synchronized void stop() {
        running = false;

        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }

        scheduler.shutdown();
        callbackExecutor.shutdown();

        try {
            scheduler.awaitTermination(2, TimeUnit.SECONDS);
            callbackExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        LOGGER.info("HeartbeatScheduler stopped");
    }

    // === User Activity Tracking ===

    /**
     * Called when user interacts with the system.
     * Switches to active mode.
     */
    public void onUserActivity() {
        lastUserActivity = Instant.now();

        if (isIdle.compareAndSet(true, false)) {
            // Switched from idle to active
            currentIntervalMs.set(ACTIVE_HEARTBEAT_MS);
            LOGGER.debug("Switched to ACTIVE mode");
        }
    }

    /**
     * Checks if user has been idle long enough to switch modes.
     */
    private void checkIdleState() {
        long idleTime = Duration.between(lastUserActivity, Instant.now()).toMillis();

        if (idleTime > ACTIVITY_TIMEOUT_MS && isIdle.compareAndSet(false, true)) {
            // Switched from active to idle
            currentIntervalMs.set(IDLE_HEARTBEAT_MS);
            LOGGER.debug("Switched to IDLE mode (inactive for {}ms)", idleTime);
        }
    }

    // === Heartbeat Processing ===

    /**
     * Called on each heartbeat interval.
     */
    private void onHeartbeat() {
        if (!running) return;

        try {
            // Update idle state
            checkIdleState();

            // Check if enough time has passed since last heartbeat
            long timeSinceLastHeartbeat = Duration.between(lastHeartbeat, Instant.now()).toMillis();
            long targetInterval = currentIntervalMs.get();

            // Apply backoff if we've had errors
            if (consecutiveErrors.get() > 0) {
                targetInterval = (long) (targetInterval * Math.pow(1.5, consecutiveErrors.get()));
            }

            if (timeSinceLastHeartbeat < targetInterval) {
                return; // Not time yet
            }

            // Check if there's anything to send
            if (batcher.getQueueSize() == 0) {
                return;
            }

            // Trigger batch processing
            LOGGER.debug("Heartbeat triggered (queue: {}, mode: {})",
                batcher.getQueueSize(), isIdle.get() ? "IDLE" : "ACTIVE");

            lastHeartbeat = Instant.now();

            // The PromptBatcher handles the actual sending

        } catch (Exception e) {
            LOGGER.error("Error in heartbeat processing", e);
        }
    }

    // === Error Handling ===

    /**
     * Called when an API error occurs.
     * Increases backoff.
     */
    public void onError(Exception error) {
        int errors = consecutiveErrors.incrementAndGet();
        LOGGER.warn("API error #{}: {}", errors, error.getMessage());
    }

    /**
     * Called when an API call succeeds.
     * Resets backoff.
     */
    public void onSuccess() {
        consecutiveErrors.set(0);
    }

    // === Statistics ===

    /**
     * Gets the current heartbeat interval in milliseconds.
     */
    public long getCurrentIntervalMs() {
        return currentIntervalMs.get();
    }

    /**
     * Checks if the scheduler is in idle mode.
     */
    public boolean isIdleMode() {
        return isIdle.get();
    }

    /**
     * Gets the time since last user activity in milliseconds.
     */
    public long getIdleTimeMs() {
        return Duration.between(lastUserActivity, Instant.now()).toMillis();
    }

    /**
     * Gets the number of consecutive errors.
     */
    public int getConsecutiveErrors() {
        return consecutiveErrors.get();
    }

    /**
     * Checks if the scheduler is running.
     */
    public boolean isRunning() {
        return running;
    }

    // === Debug Info ===

    /**
     * Gets a status summary for debugging.
     */
    public String getStatusSummary() {
        return String.format(
            "HeartbeatScheduler[mode=%s, interval=%dms, queue=%d, errors=%d, idle=%dms]",
            isIdle.get() ? "IDLE" : "ACTIVE",
            currentIntervalMs.get(),
            batcher.getQueueSize(),
            consecutiveErrors.get(),
            getIdleTimeMs()
        );
    }
}
