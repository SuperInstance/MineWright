package com.minewright.profile;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parser for TaskProfile JSON files.
 *
 * <p>This parser loads task profiles from JSON configuration files.
 * It validates the structure and content of profiles before creating
 * TaskProfile objects.</p>
 *
 * <p><b>JSON Format:</b></p>
 * <pre>{@code
 * {
 *   "name": "Mining Iron",
 *   "description": "Mine iron ore and smelt to ingots",
 *   "author": "Steve",
 *   "version": "1.0.0",
 *   "tags": ["mining", "iron", "smelting"],
 *   "settings": {
 *     "repeat": false,
 *     "stopOnError": true,
 *     "maxRetries": 3
 *   },
 *   "tasks": [
 *     {
 *       "type": "MINE",
 *       "target": "iron_ore",
 *       "quantity": 64,
 *       "parameters": {
 *         "radius": 32
 *       }
 *     },
 *     {
 *       "type": "TRAVEL",
 *       "target": "nearest_furnace"
 *     },
 *     {
 *       "type": "CRAFT",
 *       "target": "iron_ingot",
 *       "quantity": 64
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <p><b>Validation:</b></p>
 * <ul>
 *   <li>Required fields must be present</li>
 *   <li>Task types must be valid</li>
 *   <li>Quantities must be positive integers</li>
 *   <li>Conditions and parameters must be valid objects</li>
 * </ul>
 *
 * @see TaskProfile
 * @see ProfileTask
 * @since 1.4.0
 */
public class ProfileParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileParser.class);
    private static final Gson GSON = new Gson();

    /**
     * Parses a task profile from a JSON string.
     *
     * @param json The JSON string to parse
     * @return The parsed TaskProfile
     * @throws ProfileParseException If the JSON is invalid or validation fails
     */
    public TaskProfile parse(String json) throws ProfileParseException {
        if (json == null || json.trim().isEmpty()) {
            throw new ProfileParseException("JSON content cannot be null or empty");
        }

        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return parseFromJson(root);
        } catch (JsonSyntaxException e) {
            throw new ProfileParseException("Invalid JSON syntax: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ProfileParseException("Failed to parse profile: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a task profile from a file.
     *
     * @param path The path to the JSON file
     * @return The parsed TaskProfile
     * @throws ProfileParseException If the file cannot be read or parsing fails
     */
    public TaskProfile parseFile(Path path) throws ProfileParseException {
        if (path == null) {
            throw new ProfileParseException("File path cannot be null");
        }

        if (!Files.exists(path)) {
            throw new ProfileParseException("File does not exist: " + path);
        }

        if (!Files.isReadable(path)) {
            throw new ProfileParseException("File is not readable: " + path);
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            TaskProfile profile = parseFromJson(root);

            LOGGER.info("Loaded profile '{}' from file: {}", profile.getName(), path);
            return profile;
        } catch (JsonSyntaxException e) {
            throw new ProfileParseException("Invalid JSON syntax in file " + path + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ProfileParseException("Failed to read file " + path + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ProfileParseException("Failed to parse profile from file " + path + ": " + e.getMessage(), e);
        }
    }

    /**
     * Parses a task profile from a JSON object.
     */
    private TaskProfile parseFromJson(JsonObject root) throws ProfileParseException {
        // Validate and extract required fields
        String name = getStringRequired(root, "name", "Profile name");
        String description = getString(root, "description", "");
        String author = getString(root, "author", "Unknown");
        String version = getString(root, "version", "1.0.0");

        // Parse tags (optional)
        List<String> tags = new ArrayList<>();
        if (root.has("tags")) {
            JsonArray tagsArray = root.getAsJsonArray("tags");
            for (JsonElement tagElement : tagsArray) {
                tags.add(tagElement.getAsString());
            }
        }

        // Parse settings (optional)
        TaskProfile.ProfileSettings settings = null;
        if (root.has("settings")) {
            settings = parseSettings(root.getAsJsonObject("settings"));
        }

        // Parse tasks (required)
        if (!root.has("tasks")) {
            throw new ProfileParseException("Profile must have a 'tasks' array");
        }

        JsonArray tasksArray = root.getAsJsonArray("tasks");
        List<ProfileTask> tasks = new ArrayList<>();

        for (int i = 0; i < tasksArray.size(); i++) {
            try {
                JsonObject taskObject = tasksArray.get(i).getAsJsonObject();
                ProfileTask task = parseTask(taskObject);
                tasks.add(task);
            } catch (Exception e) {
                throw new ProfileParseException("Failed to parse task at index " + i + ": " + e.getMessage(), e);
            }
        }

        if (tasks.isEmpty()) {
            throw new ProfileParseException("Profile must have at least one task");
        }

        // Build profile
        return TaskProfile.builder()
                .name(name)
                .description(description)
                .author(author)
                .version(version)
                .tags(tags)
                .settings(settings)
                .tasks(tasks)
                .build();
    }

    /**
     * Parses profile settings from a JSON object.
     */
    private TaskProfile.ProfileSettings parseSettings(JsonObject settingsObject) {
        TaskProfile.ProfileSettings.Builder builder = TaskProfile.ProfileSettings.builder();

        if (settingsObject.has("repeat")) {
            builder.repeat(settingsObject.get("repeat").getAsBoolean());
        }

        if (settingsObject.has("repeatCount")) {
            builder.repeatCount(settingsObject.get("repeatCount").getAsInt());
        }

        if (settingsObject.has("stopOnError")) {
            builder.stopOnError(settingsObject.get("stopOnError").getAsBoolean());
        }

        if (settingsObject.has("maxRetries")) {
            builder.maxRetries(settingsObject.get("maxRetries").getAsInt());
        }

        if (settingsObject.has("retryDelayMs")) {
            builder.retryDelayMs(settingsObject.get("retryDelayMs").getAsLong());
        }

        if (settingsObject.has("priority")) {
            builder.priority(settingsObject.get("priority").getAsInt());
        }

        // Add custom settings
        for (Map.Entry<String, JsonElement> entry : settingsObject.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("repeat") && !key.equals("repeatCount") &&
                    !key.equals("stopOnError") && !key.equals("maxRetries") &&
                    !key.equals("retryDelayMs") && !key.equals("priority")) {
                builder.putSetting(key, jsonValueToObject(entry.getValue()));
            }
        }

        return builder.build();
    }

    /**
     * Parses a single task from a JSON object.
     */
    private ProfileTask parseTask(JsonObject taskObject) throws ProfileParseException {
        // Parse type (required)
        String typeString = getStringRequired(taskObject, "type", "Task type");
        TaskType type = TaskType.fromString(typeString);

        if (type == null) {
            throw new ProfileParseException("Invalid task type: " + typeString +
                    ". Valid types: " + java.util.Arrays.stream(TaskType.values())
                            .map(TaskType::name).toList());
        }

        ProfileTask.Builder builder = ProfileTask.builder()
                .type(type);

        // Parse target (required for most types)
        if (type.requiresTarget() && taskObject.has("target")) {
            builder.target(getString(taskObject, "target", null));
        }

        // Parse quantity (optional)
        if (taskObject.has("quantity")) {
            int quantity = taskObject.get("quantity").getAsInt();
            if (quantity < 0) {
                throw new ProfileParseException("Task quantity must be non-negative: " + quantity);
            }
            builder.quantity(quantity);
        }

        // Parse conditions (optional)
        if (taskObject.has("conditions")) {
            JsonObject conditionsObject = taskObject.getAsJsonObject("conditions");
            for (Map.Entry<String, JsonElement> entry : conditionsObject.entrySet()) {
                builder.addCondition(entry.getKey(), jsonValueToObject(entry.getValue()));
            }
        }

        // Parse parameters (optional)
        if (taskObject.has("parameters")) {
            JsonObject parametersObject = taskObject.getAsJsonObject("parameters");
            for (Map.Entry<String, JsonElement> entry : parametersObject.entrySet()) {
                builder.addParameter(entry.getKey(), jsonValueToObject(entry.getValue()));
            }
        }

        // Parse optional fields
        if (taskObject.has("description")) {
            builder.description(getString(taskObject, "description", null));
        }

        if (taskObject.has("optional")) {
            builder.optional(taskObject.get("optional").getAsBoolean());
        }

        return builder.build();
    }

    /**
     * Gets a string value from JSON object, throwing exception if not found.
     */
    private String getStringRequired(JsonObject object, String key, String label) throws ProfileParseException {
        if (!object.has(key)) {
            throw new ProfileParseException(label + " is required");
        }
        return object.get(key).getAsString();
    }

    /**
     * Gets a string value from JSON object, returning default if not found.
     */
    private String getString(JsonObject object, String key, String defaultValue) {
        if (object.has(key) && !object.get(key).isJsonNull()) {
            return object.get(key).getAsString();
        }
        return defaultValue;
    }

    /**
     * Converts a JsonElement to a Java object.
     */
    private Object jsonValueToObject(JsonElement element) {
        if (element.isJsonNull()) {
            return null;
        } else if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            } else if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsNumber();
            } else {
                return element.getAsString();
            }
        } else if (element.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                list.add(jsonValueToObject(item));
            }
            return list;
        } else if (element.isJsonObject()) {
            Map<String, Object> map = new java.util.HashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), jsonValueToObject(entry.getValue()));
            }
            return map;
        }
        return null;
    }

    /**
     * Validates a task profile without throwing exceptions.
     *
     * @param profile The profile to validate
     * @return List of validation errors (empty if valid)
     */
    public List<String> validate(TaskProfile profile) {
        List<String> errors = new ArrayList<>();

        if (profile == null) {
            errors.add("Profile cannot be null");
            return errors;
        }

        if (profile.getName() == null || profile.getName().trim().isEmpty()) {
            errors.add("Profile name cannot be empty");
        }

        if (!profile.hasTasks()) {
            errors.add("Profile must have at least one task");
        }

        // Validate each task
        for (int i = 0; i < profile.getTasks().size(); i++) {
            ProfileTask task = profile.getTasks().get(i);
            if (task.getType() == null) {
                errors.add("Task " + i + ": type cannot be null");
            }
            if (task.getType().requiresTarget() && (task.getTarget() == null || task.getTarget().isEmpty())) {
                errors.add("Task " + i + ": target is required for type " + task.getType());
            }
            if (task.getQuantity() < 0) {
                errors.add("Task " + i + ": quantity cannot be negative");
            }
        }

        return errors;
    }

    /**
     * Exception thrown when profile parsing fails.
     */
    public static class ProfileParseException extends Exception {
        public ProfileParseException(String message) {
            super(message);
        }

        public ProfileParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
