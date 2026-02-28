package com.minewright.blackboard;

import java.util.UUID;

/**
 * Represents a single entry in the blackboard system for shared knowledge among agents.
 *
 * <p><b>Purpose:</b></p>
 * <p>BlackboardEntry encapsulates a piece of knowledge with metadata about its source,
 * confidence level, type, and timestamp. This allows agents to share information while
 * maintaining provenance and enabling reasoning about knowledge quality.</p>
 *
 * <p><b>Entry Types:</b></p>
 * <ul>
 *   <li><b>FACT:</b> Direct observation or verified information (e.g., "diamond at 100,64,200")</li>
 *   <li><b>HYPOTHESIS:</b> Inferred or uncertain information (e.g., "likely hostile nearby")</li>
 *   <li><b>GOAL:</b> Shared objective or target (e.g., "build storage at spawn")</li>
 *   <li><b>CONSTRAINT:</b> Rule or limitation (e.g., "don't build above y=256")</li>
 * </ul>
 *
 * <p><b>Confidence Levels:</b></p>
 * <ul>
 *   <li>1.0 = Absolute certainty (direct observation)</li>
 *   <li>0.7-0.9 = High confidence (inference from reliable data)</li>
 *   <li>0.4-0.6 = Moderate confidence (educated guess)</li>
 *   <li>0.1-0.3 = Low confidence (speculation)</li>
 *   <li>0.0 = No confidence (placeholder/default)</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is immutable and thread-safe. All fields are final and set during construction.
 * Entries can be safely shared between agents without synchronization.</p>
 *
 * @param <T> The type of the value stored in this entry
 * @see KnowledgeArea
 * @see Blackboard
 * @since 1.0.0
 */
public class BlackboardEntry<T> {
    /**
     * Unique key identifying this entry within a knowledge area.
     * Must be unique per area, but can be duplicated across different areas.
     */
    private final String key;

    /**
     * The actual knowledge value being stored.
     * Can be any type: strings, numbers, complex objects, NBT data, etc.
     */
    private final T value;

    /**
     * Timestamp when this entry was created, in milliseconds since epoch.
     * Used for staleness detection and cache eviction.
     */
    private final long timestamp;

    /**
     * UUID of the agent that created this entry.
     * Null if the entry was created by the system itself (e.g., server events).
     */
    private final UUID sourceAgent;

    /**
     * Confidence level of this knowledge (0.0 to 1.0).
     * Higher values indicate more reliable information.
     */
    private final double confidence;

    /**
     * Type classification of this knowledge entry.
     * Affects how agents interpret and use the information.
     */
    private final EntryType type;

    /**
     * Classification of knowledge entry types.
     * Determines how agents interpret and reason about the entry.
     */
    public enum EntryType {
        /**
         * Direct observation or verified information.
         * Highest reliability, used as ground truth for planning.
         */
        FACT,

        /**
         * Inferred or uncertain information.
         * Requires confirmation before critical use.
         */
        HYPOTHESIS,

        /**
         * Shared objective or target state.
         * Guides agent behavior and task allocation.
         */
        GOAL,

        /**
         * Rule or limitation on behavior.
         * Must be respected during planning and execution.
         */
        CONSTRAINT
    }

    /**
     * Creates a new blackboard entry with full metadata.
     *
     * @param key Unique identifier for this entry within its area
     * @param value The knowledge value to store
     * @param timestamp Creation timestamp (milliseconds since epoch)
     * @param sourceAgent UUID of the agent creating this entry (null for system entries)
     * @param confidence Confidence level (0.0 to 1.0)
     * @param type Classification of this entry
     * @throws IllegalArgumentException if key is null/empty or confidence is out of range
     */
    public BlackboardEntry(String key, T value, long timestamp, UUID sourceAgent,
                          double confidence, EntryType type) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Entry key cannot be null or empty");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0, got: " + confidence);
        }
        if (type == null) {
            throw new IllegalArgumentException("EntryType cannot be null");
        }

        this.key = key;
        this.value = value;
        this.timestamp = timestamp;
        this.sourceAgent = sourceAgent;
        this.confidence = confidence;
        this.type = type;
    }

    /**
     * Creates a new blackboard entry with current timestamp.
     *
     * @param key Unique identifier for this entry within its area
     * @param value The knowledge value to store
     * @param sourceAgent UUID of the agent creating this entry (null for system entries)
     * @param confidence Confidence level (0.0 to 1.0)
     * @param type Classification of this entry
     */
    public BlackboardEntry(String key, T value, UUID sourceAgent,
                          double confidence, EntryType type) {
        this(key, value, System.currentTimeMillis(), sourceAgent, confidence, type);
    }

    /**
     * Creates a FACT entry with high confidence (1.0) and current timestamp.
     * Convenience method for direct observations.
     *
     * @param key Unique identifier for this entry within its area
     * @param value The knowledge value to store
     * @param sourceAgent UUID of the agent creating this entry
     * @return A new FACT entry with confidence 1.0
     */
    public static <T> BlackboardEntry<T> createFact(String key, T value, UUID sourceAgent) {
        return new BlackboardEntry<>(key, value, sourceAgent, 1.0, EntryType.FACT);
    }

    /**
     * Creates a HYPOTHESIS entry with moderate confidence (0.5) and current timestamp.
     * Convenience method for uncertain information.
     *
     * @param key Unique identifier for this entry within its area
     * @param value The knowledge value to store
     * @param sourceAgent UUID of the agent creating this entry
     * @param confidence Confidence level (default 0.5 if not specified)
     * @return A new HYPOTHESIS entry
     */
    public static <T> BlackboardEntry<T> createHypothesis(String key, T value, UUID sourceAgent,
                                                          double confidence) {
        return new BlackboardEntry<>(key, value, sourceAgent, confidence, EntryType.HYPOTHESIS);
    }

    /**
     * Creates a GOAL entry with high confidence (0.9) and current timestamp.
     * Convenience method for shared objectives.
     *
     * @param key Unique identifier for this entry within its area
     * @param value The goal description
     * @param sourceAgent UUID of the agent creating this goal
     * @return A new GOAL entry with confidence 0.9
     */
    public static <T> BlackboardEntry<T> createGoal(String key, T value, UUID sourceAgent) {
        return new BlackboardEntry<>(key, value, sourceAgent, 0.9, EntryType.GOAL);
    }

    /**
     * Creates a CONSTRAINT entry with maximum confidence (1.0).
     * Convenience method for rules and limitations.
     *
     * @param key Unique identifier for this entry within its area
     * @param value The constraint description
     * @return A new CONSTRAINT entry with confidence 1.0, null source agent
     */
    public static <T> BlackboardEntry<T> createConstraint(String key, T value) {
        return new BlackboardEntry<>(key, value, null, 1.0, EntryType.CONSTRAINT);
    }

    /**
     * Gets the unique key for this entry.
     *
     * @return Entry key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the value stored in this entry.
     *
     * @return Knowledge value
     */
    public T getValue() {
        return value;
    }

    /**
     * Gets the creation timestamp.
     *
     * @return Timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the UUID of the agent that created this entry.
     *
     * @return Agent UUID, or null if created by the system
     */
    public UUID getSourceAgent() {
        return sourceAgent;
    }

    /**
     * Gets the confidence level of this entry.
     *
     * @return Confidence (0.0 to 1.0)
     */
    public double getConfidence() {
        return confidence;
    }

    /**
     * Gets the type classification of this entry.
     *
     * @return Entry type
     */
    public EntryType getType() {
        return type;
    }

    /**
     * Checks if this entry is stale based on the given maximum age.
     *
     * @param maxAgeMs Maximum allowed age in milliseconds
     * @return true if this entry is older than maxAgeMs
     */
    public boolean isStale(long maxAgeMs) {
        long age = System.currentTimeMillis() - timestamp;
        return age > maxAgeMs;
    }

    /**
     * Gets the age of this entry.
     *
     * @return Age in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - timestamp;
    }

    @Override
    public String toString() {
        return "BlackboardEntry{" +
                "key='" + key + '\'' +
                ", value=" + value +
                ", timestamp=" + timestamp +
                ", sourceAgent=" + (sourceAgent != null ? sourceAgent.toString().substring(0, 8) : "system") +
                ", confidence=" + confidence +
                ", type=" + type +
                '}';
    }

    /**
     * Checks if this entry is equal to another object.
     * Two entries are equal if they have the same key and knowledge area.
     * The value and other metadata are not considered for equality.
     *
     * @param obj Object to compare with
     * @return true if entries have the same key
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BlackboardEntry<?> that = (BlackboardEntry<?>) obj;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
