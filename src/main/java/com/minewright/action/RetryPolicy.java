package com.minewright.action;

import java.util.concurrent.TimeUnit;

/**
 * Retry policy for handling transient action failures.
 *
 * <p>This class defines how actions should be retried when they fail
 * due to transient errors (network issues, temporary blocks, etc.).</p>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Configurable max attempts and delays</li>
 *   <li>Exponential backoff for retries</li>
 *   <li>Jitter to prevent retry storms</li>
 *   <li>Time-limited retries</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class RetryPolicy {
    private final int maxAttempts;
    private final long initialDelayMs;
    private final long maxDelayMs;
    private final double backoffMultiplier;
    private final boolean addJitter;
    private final long maxTotalDurationMs;

    private RetryPolicy(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.initialDelayMs = builder.initialDelayMs;
        this.maxDelayMs = builder.maxDelayMs;
        this.backoffMultiplier = builder.backoffMultiplier;
        this.addJitter = builder.addJitter;
        this.maxTotalDurationMs = builder.maxTotalDurationMs;
    }

    /**
     * Gets the maximum number of retry attempts.
     *
     * @return Max attempts (1 = no retries)
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Calculates the delay before the next retry attempt.
     *
     * @param attemptNumber The attempt number (0-indexed)
     * @return Delay in milliseconds
     */
    public long getDelayMs(int attemptNumber) {
        if (attemptNumber <= 0) {
            return 0;
        }

        // Calculate exponential backoff
        long delay = (long) (initialDelayMs * Math.pow(backoffMultiplier, attemptNumber - 1));
        delay = Math.min(delay, maxDelayMs);

        // Add jitter if enabled (random +/- 25%)
        if (addJitter) {
            double jitter = 0.5 + Math.random(); // 0.5 to 1.5
            delay = (long) (delay * jitter);
        }

        return delay;
    }

    /**
     * Checks if a retry is allowed based on elapsed time.
     *
     * @param elapsedMs Time elapsed since first attempt
     * @return true if retry is allowed
     */
    public boolean canRetry(long elapsedMs) {
        return elapsedMs < maxTotalDurationMs;
    }

    /**
     * Creates a new builder for RetryPolicy.
     *
     * @return New builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for RetryPolicy.
     */
    public static class Builder {
        private int maxAttempts = 3;
        private long initialDelayMs = 1000; // 1 second
        private long maxDelayMs = 30000; // 30 seconds
        private double backoffMultiplier = 2.0;
        private boolean addJitter = true;
        private long maxTotalDurationMs = 60000; // 1 minute

        /**
         * Sets the maximum number of retry attempts.
         *
         * @param maxAttempts Max attempts (1 = no retries)
         * @return This builder
         */
        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = Math.max(1, maxAttempts);
            return this;
        }

        /**
         * Sets the initial delay before the first retry.
         *
         * @param delay Delay duration
         * @param unit Time unit
         * @return This builder
         */
        public Builder initialDelay(long delay, TimeUnit unit) {
            this.initialDelayMs = unit.toMillis(delay);
            return this;
        }

        /**
         * Sets the maximum delay between retries.
         *
         * @param delay Max delay
         * @param unit Time unit
         * @return This builder
         */
        public Builder maxDelay(long delay, TimeUnit unit) {
            this.maxDelayMs = unit.toMillis(delay);
            return this;
        }

        /**
         * Sets the exponential backoff multiplier.
         *
         * @param multiplier Backoff multiplier (default: 2.0)
         * @return This builder
         */
        public Builder backoffMultiplier(double multiplier) {
            this.backoffMultiplier = Math.max(1.0, multiplier);
            return this;
        }

        /**
         * Enables or disables jitter (random variation) in retry delays.
         *
         * @param enable true to enable jitter
         * @return This builder
         */
        public Builder addJitter(boolean enable) {
            this.addJitter = enable;
            return this;
        }

        /**
         * Sets the maximum total duration for all retry attempts.
         *
         * @param duration Max duration
         * @param unit Time unit
         * @return This builder
         */
        public Builder maxTotalDuration(long duration, TimeUnit unit) {
            this.maxTotalDurationMs = unit.toMillis(duration);
            return this;
        }

        /**
         * Builds the RetryPolicy.
         *
         * @return New RetryPolicy instance
         */
        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }

    // Predefined retry policies

    /** No retries - fail immediately. */
    public static final RetryPolicy NO_RETRIES = builder().maxAttempts(1).build();

    /** Quick retries for fast, transient issues. */
    public static final RetryPolicy QUICK_RETRY = builder()
        .maxAttempts(3)
        .initialDelay(500, TimeUnit.MILLISECONDS)
        .maxDelay(5, TimeUnit.SECONDS)
        .maxTotalDuration(10, TimeUnit.SECONDS)
        .build();

    /** Standard retries for most actions. */
    public static final RetryPolicy STANDARD = builder()
        .maxAttempts(3)
        .initialDelay(1, TimeUnit.SECONDS)
        .maxDelay(10, TimeUnit.SECONDS)
        .maxTotalDuration(30, TimeUnit.SECONDS)
        .build();

    /** Aggressive retries for important actions. */
    public static final RetryPolicy AGGRESSIVE = builder()
        .maxAttempts(5)
        .initialDelay(1, TimeUnit.SECONDS)
        .maxDelay(30, TimeUnit.SECONDS)
        .maxTotalDuration(2, TimeUnit.MINUTES)
        .build();

    /** Patient retries for long-running operations. */
    public static final RetryPolicy PATIENT = builder()
        .maxAttempts(10)
        .initialDelay(2, TimeUnit.SECONDS)
        .maxDelay(60, TimeUnit.SECONDS)
        .maxTotalDuration(5, TimeUnit.MINUTES)
        .build();
}
