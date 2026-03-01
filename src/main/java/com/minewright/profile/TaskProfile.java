package com.minewright.profile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a task profile inspired by Honorbuddy's profile system.
 *
 * <p>A profile is a declarative definition of a sequence of tasks that an agent
 * should execute. Profiles can be:</p>
 * <ul>
 *   <li>Loaded from JSON configuration files</li>
 *   <li>Created programmatically by LLMs</li>
 *   <li>Shared between agents</li>
 *   <li>Converted to Scripts for execution</li>
 * </ul>
 *
 * <p><b>Profile Structure:</b></p>
 * <pre>
 * TaskProfile {
 *   id: String (UUID)
 *   name: String
 *   description: String
 *   author: String
 *   version: String
 *   tasks: List&lt;ProfileTask&gt;
 *   settings: ProfileSettings
 *   tags: List&lt;String&gt;
 *   createdAt: Instant
 * }
 * </pre>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * TaskProfile profile = TaskProfile.builder()
 *     .name("Mining Iron")
 *     .description("Mine iron ore and smelt to ingots")
 *     .addTask(ProfileTask.builder()
 *         .type(TaskType.MINE)
 *         .target("iron_ore")
 *         .quantity(64)
 *         .build())
 *     .addTask(ProfileTask.builder()
 *         .type(TaskType.CRAFT)
 *         .target("iron_ingot")
 *         .quantity(64)
 *         .build())
 *     .build();
 * }</pre>
 *
 * @see ProfileTask
 * @see ProfileParser
 * @see ProfileExecutor
 * @since 1.4.0
 */
public class TaskProfile {

    private final String id;
    private final String name;
    private final String description;
    private final String author;
    private final String version;
    private final List<ProfileTask> tasks;
    private final ProfileSettings settings;
    private final List<String> tags;
    private final Instant createdAt;
    private final Map<String, Object> metadata;

    private TaskProfile(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString().substring(0, 8);
        this.name = builder.name;
        this.description = builder.description;
        this.author = builder.author != null ? builder.author : "Unknown";
        this.version = builder.version != null ? builder.version : "1.0.0";
        this.tasks = Collections.unmodifiableList(new ArrayList<>(builder.tasks));
        this.settings = builder.settings != null ? builder.settings : ProfileSettings.builder().build();
        this.tags = Collections.unmodifiableList(new ArrayList<>(builder.tags));
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
    }

    /**
     * Creates a new builder for constructing task profiles.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new builder initialized from this profile.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public String getVersion() { return version; }
    public List<ProfileTask> getTasks() { return tasks; }
    public ProfileSettings getSettings() { return settings; }
    public List<String> getTags() { return tags; }
    public Instant getCreatedAt() { return createdAt; }
    public Map<String, Object> getMetadata() { return metadata; }

    /**
     * Gets the total number of tasks in this profile.
     */
    public int getTaskCount() {
        return tasks.size();
    }

    /**
     * Checks if this profile has any tasks.
     */
    public boolean hasTasks() {
        return !tasks.isEmpty();
    }

    /**
     * Gets metadata value by key.
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Creates a copy of this profile.
     */
    public TaskProfile copy() {
        return this.toBuilder().build();
    }

    /**
     * Converts this profile to JSON format.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"id\": \"").append(id).append("\",\n");
        sb.append("  \"name\": \"").append(escapeJson(name)).append("\",\n");
        sb.append("  \"description\": \"").append(escapeJson(description)).append("\",\n");
        sb.append("  \"author\": \"").append(escapeJson(author)).append("\",\n");
        sb.append("  \"version\": \"").append(version).append("\",\n");

        if (!tags.isEmpty()) {
            sb.append("  \"tags\": [");
            for (int i = 0; i < tags.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(escapeJson(tags.get(i))).append("\"");
            }
            sb.append("],\n");
        }

        sb.append("  \"tasks\": [\n");
        for (int i = 0; i < tasks.size(); i++) {
            sb.append("    ").append(tasks.get(i).toJson());
            if (i < tasks.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");

        if (settings != null && !settings.isEmpty()) {
            sb.append(",\n  \"settings\": ").append(settings.toJson()).append("\n");
        }

        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Builder for constructing TaskProfile instances.
     */
    public static class Builder {
        private String id;
        private String name;
        private String description;
        private String author;
        private String version;
        private List<ProfileTask> tasks = new ArrayList<>();
        private ProfileSettings settings;
        private List<String> tags = new ArrayList<>();
        private Instant createdAt;
        private Map<String, Object> metadata = new HashMap<>();

        private Builder() {}

        private Builder(TaskProfile existing) {
            this.id = existing.id;
            this.name = existing.name;
            this.description = existing.description;
            this.author = existing.author;
            this.version = existing.version;
            this.tasks = new ArrayList<>(existing.tasks);
            this.settings = existing.settings;
            this.tags = new ArrayList<>(existing.tags);
            this.createdAt = existing.createdAt;
            this.metadata = new HashMap<>(existing.metadata);
        }

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder author(String author) { this.author = author; return this; }
        public Builder version(String version) { this.version = version; return this; }

        public Builder addTask(ProfileTask task) {
            this.tasks.add(task);
            return this;
        }

        public Builder tasks(List<ProfileTask> tasks) {
            this.tasks = new ArrayList<>(tasks);
            return this;
        }

        public Builder settings(ProfileSettings settings) {
            this.settings = settings;
            return this;
        }

        public Builder addTag(String tag) {
            this.tags.add(tag);
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = new ArrayList<>(tags);
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder putMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        public TaskProfile build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalStateException("Profile name cannot be null or empty");
            }
            return new TaskProfile(this);
        }
    }

    /**
     * Profile settings that control execution behavior.
     */
    public static class ProfileSettings {
        private final boolean repeat;
        private final int repeatCount;
        private final boolean stopOnError;
        private final int maxRetries;
        private final long retryDelayMs;
        private final int priority;
        private final Map<String, Object> customSettings;

        private ProfileSettings(Builder builder) {
            this.repeat = builder.repeat;
            this.repeatCount = builder.repeatCount;
            this.stopOnError = builder.stopOnError;
            this.maxRetries = builder.maxRetries;
            this.retryDelayMs = builder.retryDelayMs;
            this.priority = builder.priority;
            this.customSettings = Collections.unmodifiableMap(new HashMap<>(builder.customSettings));
        }

        public static Builder builder() {
            return new Builder();
        }

        public boolean isRepeat() { return repeat; }
        public int getRepeatCount() { return repeatCount; }
        public boolean isStopOnError() { return stopOnError; }
        public int getMaxRetries() { return maxRetries; }
        public long getRetryDelayMs() { return retryDelayMs; }
        public int getPriority() { return priority; }
        public Map<String, Object> getCustomSettings() { return customSettings; }

        public boolean isEmpty() {
            return !repeat && repeatCount == 0 && !stopOnError && maxRetries == 0
                    && retryDelayMs == 0 && priority == 0 && customSettings.isEmpty();
        }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            if (repeat) sb.append("\"repeat\": true");
            if (repeatCount > 0) sb.append(sb.length() > 1 ? ", " : "").append("\"repeatCount\": ").append(repeatCount);
            if (stopOnError) sb.append(sb.length() > 1 ? ", " : "").append("\"stopOnError\": true");
            if (maxRetries > 0) sb.append(sb.length() > 1 ? ", " : "").append("\"maxRetries\": ").append(maxRetries);
            if (retryDelayMs > 0) sb.append(sb.length() > 1 ? ", " : "").append("\"retryDelayMs\": ").append(retryDelayMs);
            if (priority != 0) sb.append(sb.length() > 1 ? ", " : "").append("\"priority\": ").append(priority);
            sb.append("}");
            return sb.toString();
        }

        public static class Builder {
            private boolean repeat = false;
            private int repeatCount = 1;
            private boolean stopOnError = true;
            private int maxRetries = 3;
            private long retryDelayMs = 5000;
            private int priority = 5;
            private Map<String, Object> customSettings = new HashMap<>();

            public Builder repeat(boolean repeat) { this.repeat = repeat; return this; }
            public Builder repeatCount(int count) { this.repeatCount = count; return this; }
            public Builder stopOnError(boolean stop) { this.stopOnError = stop; return this; }
            public Builder maxRetries(int retries) { this.maxRetries = retries; return this; }
            public Builder retryDelayMs(long delay) { this.retryDelayMs = delay; return this; }
            public Builder priority(int priority) { this.priority = priority; return this; }

            public Builder putSetting(String key, Object value) {
                this.customSettings.put(key, value);
                return this;
            }

            public ProfileSettings build() {
                return new ProfileSettings(this);
            }
        }
    }
}
