package com.minewright.coordination;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an agent's bid for a task in the Contract Net Protocol.
 *
 * <p>Bids are submitted by agents in response to {@link TaskAnnouncement}.
 * The manager evaluates all bids and awards the contract to the best bidder.</p>
 *
 * <p><b>Bid Scoring:</b></p>
 * <p>Bids are compared using the formula:
 * <code>score * confidence / (estimatedTime / 1000.0)</code></p>
 *
 * <ul>
 *   <li><b>score</b> - How well the agent can perform this task (0.0-1.0)</li>
 *   <li><b>confidence</b> - Agent's confidence in success (0.0-1.0)</li>
 *   <li><b>estimatedTime</b> - Expected completion time in milliseconds</li>
 * </ul>
 *
 * <p><b>Capabilities Map:</b></p>
 * <ul>
 *   <li><code>proficiencies</code> - Map&lt;String, Double&gt; of skill levels</li>
 *   <li><code>tools</code> - Set&lt;String&gt; of available tools</li>
 *   <li><code>distance</code> - Double, distance to task location</li>
 *   <li><code>currentLoad</code> - Double, how busy the agent is (0.0-1.0)</li>
 * </ul>
 *
 * @param announcementId The announcement this bid responds to
 * @param bidderId UUID of the agent submitting the bid
 * @param score How well agent can do this task (0.0-1.0)
 * @param estimatedTime Estimated completion time in milliseconds
 * @param confidence Confidence in success (0.0-1.0)
 * @param capabilities Map of agent capabilities and context
 *
 * @see TaskAnnouncement
 * @see AgentCapability
 * @since 1.3.0
 */
public class TaskBid implements Comparable<TaskBid> {
    private final String announcementId;
    private final UUID bidderId;
    private final double score;
    private final long estimatedTime;
    private final double confidence;
    private final Map<String, Object> capabilities;

    /**
     * Creates a new task bid.
     *
     * @param announcementId The announcement this bid responds to
     * @param bidderId UUID of the agent submitting the bid
     * @param score Capability score (0.0-1.0)
     * @param estimatedTime Estimated time in milliseconds
     * @param confidence Confidence in success (0.0-1.0)
     * @param capabilities Agent capabilities and context
     */
    public TaskBid(
        String announcementId,
        UUID bidderId,
        double score,
        long estimatedTime,
        double confidence,
        Map<String, Object> capabilities
    ) {
        if (announcementId == null || announcementId.isBlank()) {
            throw new IllegalArgumentException("Announcement ID cannot be null or blank");
        }
        if (bidderId == null) {
            throw new IllegalArgumentException("Bidder ID cannot be null");
        }
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("Score must be between 0.0 and 1.0");
        }
        if (estimatedTime <= 0) {
            throw new IllegalArgumentException("Estimated time must be positive");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }

        this.announcementId = announcementId;
        this.bidderId = bidderId;
        this.score = score;
        this.estimatedTime = estimatedTime;
        this.confidence = confidence;
        this.capabilities = capabilities != null ? capabilities : Map.of();
    }

    public String announcementId() {
        return announcementId;
    }

    public UUID bidderId() {
        return bidderId;
    }

    public double score() {
        return score;
    }

    public long estimatedTime() {
        return estimatedTime;
    }

    public double confidence() {
        return confidence;
    }

    public Map<String, Object> capabilities() {
        return capabilities;
    }

    /**
     * Calculates the overall bid value for comparison.
     * Higher values indicate better bids.
     *
     * @return Calculated bid value
     */
    public double getBidValue() {
        double timeFactor = estimatedTime / 1000.0; // Convert to seconds
        if (timeFactor < 1.0) {
            timeFactor = 1.0; // Prevent division by very small numbers
        }
        return (score * confidence) / timeFactor;
    }

    /**
     * Gets the agent's distance to the task location.
     *
     * @return Distance in blocks, or -1 if not specified
     */
    public double getDistance() {
        Object dist = capabilities.get("distance");
        if (dist instanceof Number) {
            return ((Number) dist).doubleValue();
        }
        return -1.0;
    }

    /**
     * Gets the agent's current load.
     *
     * @return Load factor (0.0-1.0), or 0.0 if not specified
     */
    public double getCurrentLoad() {
        Object load = capabilities.get("currentLoad");
        if (load instanceof Number) {
            return ((Number) load).doubleValue();
        }
        return 0.0;
    }

    @Override
    public int compareTo(TaskBid other) {
        if (other == null) {
            return 1;
        }

        // Higher bid value is better
        double thisValue = this.getBidValue();
        double otherValue = other.getBidValue();

        int comparison = Double.compare(otherValue, thisValue);
        if (comparison != 0) {
            return comparison;
        }

        // Tie-breaker: lower estimated time is better
        comparison = Long.compare(this.estimatedTime, other.estimatedTime);
        if (comparison != 0) {
            return comparison;
        }

        // Final tie-breaker: bidder ID (for consistent ordering)
        return this.bidderId.compareTo(other.bidderId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskBid taskBid = (TaskBid) o;
        return Objects.equals(announcementId, taskBid.announcementId) &&
               Objects.equals(bidderId, taskBid.bidderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(announcementId, bidderId);
    }

    @Override
    public String toString() {
        return String.format("TaskBid[announcement=%s, bidder=%s, score=%.2f, time=%dms, conf=%.2f, value=%.4f]",
            announcementId.substring(0, Math.min(8, announcementId.length())),
            bidderId.toString().substring(0, 8),
            score, estimatedTime, confidence, getBidValue());
    }

    /**
     * Creates a builder for constructing task bids.
     *
     * @return New builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating TaskBid instances.
     */
    public static class Builder {
        private String announcementId;
        private UUID bidderId;
        private double score = 0.5;
        private long estimatedTime = 60000; // Default 1 minute
        private double confidence = 0.5;
        private final Map<String, Object> capabilities = new java.util.HashMap<>();

        private Builder() {}

        public Builder announcementId(String announcementId) {
            this.announcementId = announcementId;
            return this;
        }

        public Builder bidderId(UUID bidderId) {
            this.bidderId = bidderId;
            return this;
        }

        public Builder score(double score) {
            if (score < 0.0 || score > 1.0) {
                throw new IllegalArgumentException("Score must be between 0.0 and 1.0");
            }
            this.score = score;
            return this;
        }

        public Builder estimatedTime(long estimatedTime) {
            if (estimatedTime <= 0) {
                throw new IllegalArgumentException("Estimated time must be positive");
            }
            this.estimatedTime = estimatedTime;
            return this;
        }

        public Builder estimatedSeconds(double seconds) {
            return estimatedTime((long) (seconds * 1000));
        }

        public Builder confidence(double confidence) {
            if (confidence < 0.0 || confidence > 1.0) {
                throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
            }
            this.confidence = confidence;
            return this;
        }

        public Builder capability(String key, Object value) {
            capabilities.put(key, value);
            return this;
        }

        public Builder proficiencies(Map<String, Double> proficiencies) {
            capabilities.put("proficiencies", proficiencies);
            return this;
        }

        public Builder tools(java.util.Set<String> tools) {
            capabilities.put("tools", tools);
            return this;
        }

        public Builder distance(double distance) {
            capabilities.put("distance", distance);
            return this;
        }

        public Builder currentLoad(double load) {
            capabilities.put("currentLoad", load);
            return this;
        }

        public Builder capabilities(Map<String, Object> capabilities) {
            this.capabilities.putAll(capabilities);
            return this;
        }

        /**
         * Builds the task bid.
         *
         * @return New TaskBid instance
         * @throws IllegalStateException if required fields are missing
         */
        public TaskBid build() {
            if (announcementId == null) {
                throw new IllegalStateException("Announcement ID is required");
            }
            if (bidderId == null) {
                throw new IllegalStateException("Bidder ID is required");
            }
            return new TaskBid(announcementId, bidderId, score, estimatedTime, confidence, capabilities);
        }
    }
}
