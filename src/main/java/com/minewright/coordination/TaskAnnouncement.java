package com.minewright.coordination;

import com.minewright.action.Task;

import java.util.Map;
import java.util.UUID;

/**
 * Announcement broadcast when a task needs allocation through the Contract Net Protocol.
 *
 * <p><b>Contract Net Protocol Flow:</b></p>
 * <ol>
 *   <li>Manager announces task via {@link TaskAnnouncement}</li>
 *   <li>Agents evaluate and submit {@link TaskBid}</li>
 *   <li>Manager selects best bid and awards contract</li>
 *   <li>Agent executes and reports completion</li>
 * </ol>
 *
 * <p><b>Requirements Map:</b></p>
 * <ul>
 *   <li><code>skills</code> - Set&lt;String&gt; of required skills</li>
 *   <li><code>maxDistance</code> - Double, maximum distance from task location</li>
 *   <li><code>tools</code> - Set&lt;String&gt; of required tools</li>
 *   <li><code>minProficiency</code> - Double, minimum skill level (0.0-1.0)</li>
 *   <li><code>priority</code> - Task priority for sorting</li>
 * </ul>
 *
 * @param announcementId Unique identifier for this announcement
 * @param task The task to be allocated
 * @param requesterId UUID of the agent/manager requesting the task
 * @param deadline System time deadline for bid submission (milliseconds)
 * @param requirements Map of capability requirements (skills, distance, tools, etc.)
 *
 * @see TaskBid
 * @see ContractNetManager
 * @since 1.3.0
 */
public record TaskAnnouncement(
    String announcementId,
    Task task,
    UUID requesterId,
    long deadline,
    Map<String, Object> requirements
) {
    /**
     * Creates a new task announcement with a generated unique ID.
     *
     * @param task The task to be allocated
     * @param requesterId UUID of the agent requesting the task
     * @param deadlineMs Deadline for bid submission (milliseconds from epoch)
     * @param requirements Capability requirements map
     */
    public TaskAnnouncement {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (requesterId == null) {
            throw new IllegalArgumentException("Requester ID cannot be null");
        }
        if (deadline <= System.currentTimeMillis()) {
            throw new IllegalArgumentException("Deadline must be in the future");
        }
        if (announcementId == null || announcementId.isBlank()) {
            announcementId = "announce_" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    /**
     * Checks if this announcement has expired.
     *
     * @return true if the current time is past the deadline
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > deadline;
    }

    /**
     * Gets the remaining time before deadline.
     *
     * @return Remaining milliseconds (0 if expired)
     */
    public long getRemainingTime() {
        long remaining = deadline - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Gets a required skill from the requirements map.
     *
     * @param skillName The skill to check for
     * @return true if the skill is required
     */
    @SuppressWarnings("unchecked")
    public boolean requiresSkill(String skillName) {
        Object skills = requirements.get("skills");
        if (skills instanceof Iterable<?> skillSet) {
            for (Object skill : skillSet) {
                if (skillName.equals(skill)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets the maximum distance constraint.
     *
     * @return Maximum distance, or Double.MAX_VALUE if not specified
     */
    public double getMaxDistance() {
        Object dist = requirements.get("maxDistance");
        if (dist instanceof Number) {
            return ((Number) dist).doubleValue();
        }
        return Double.MAX_VALUE;
    }

    /**
     * Gets the minimum proficiency requirement.
     *
     * @return Minimum proficiency (0.0-1.0), or 0.0 if not specified
     */
    public double getMinProficiency() {
        Object prof = requirements.get("minProficiency");
        if (prof instanceof Number) {
            return ((Number) prof).doubleValue();
        }
        return 0.0;
    }

    /**
     * Gets the task priority for sorting.
     *
     * @return Priority value (higher = more important), default 5
     */
    public int getPriority() {
        Object priority = requirements.get("priority");
        if (priority instanceof Number) {
            return ((Number) priority).intValue();
        }
        return 5;
    }

    /**
     * Creates a builder for constructing task announcements.
     *
     * @return New builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating TaskAnnouncement instances.
     */
    public static class Builder {
        private String announcementId;
        private Task task;
        private UUID requesterId;
        private long deadline;
        private final Map<String, Object> requirements = new java.util.HashMap<>();

        private Builder() {}

        public Builder announcementId(String id) {
            this.announcementId = id;
            return this;
        }

        public Builder task(Task task) {
            this.task = task;
            return this;
        }

        public Builder requesterId(UUID requesterId) {
            this.requesterId = requesterId;
            return this;
        }

        public Builder deadline(long deadlineMs) {
            this.deadline = deadlineMs;
            return this;
        }

        public Builder deadlineAfter(long durationMs) {
            this.deadline = System.currentTimeMillis() + durationMs;
            return this;
        }

        public Builder requireSkill(String skill) {
            requirements.computeIfAbsent("skills", k -> new java.util.HashSet<String>());
            @SuppressWarnings("unchecked")
            var skills = (java.util.Set<String>) requirements.get("skills");
            skills.add(skill);
            return this;
        }

        public Builder skills(java.util.Set<String> skills) {
            requirements.put("skills", skills);
            return this;
        }

        public Builder maxDistance(double distance) {
            requirements.put("maxDistance", distance);
            return this;
        }

        public Builder tools(java.util.Set<String> tools) {
            requirements.put("tools", tools);
            return this;
        }

        public Builder minProficiency(double proficiency) {
            requirements.put("minProficiency", proficiency);
            return this;
        }

        public Builder priority(int priority) {
            requirements.put("priority", priority);
            return this;
        }

        public Builder requirement(String key, Object value) {
            requirements.put(key, value);
            return this;
        }

        public Builder requirements(Map<String, Object> requirements) {
            this.requirements.putAll(requirements);
            return this;
        }

        /**
         * Builds the task announcement.
         *
         * @return New TaskAnnouncement instance
         * @throws IllegalStateException if required fields are missing
         */
        public TaskAnnouncement build() {
            if (task == null) {
                throw new IllegalStateException("Task is required");
            }
            if (requesterId == null) {
                throw new IllegalStateException("Requester ID is required");
            }
            if (deadline == 0) {
                deadline = System.currentTimeMillis() + 30000; // Default 30 second deadline
            }
            return new TaskAnnouncement(announcementId, task, requesterId, deadline, requirements);
        }
    }
}
