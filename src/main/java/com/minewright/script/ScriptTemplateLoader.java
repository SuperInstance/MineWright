package com.minewright.script;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads script templates from JSON files in the config/templates directory.
 *
 * <p><b>Template Loading:</b></p>
 * <ul>
 *   <li>Loads JSON templates from {@code config/templates/}</li>
 *   <li>Caches loaded templates for performance</li>
 *   <li>Validates template structure and required fields</li>
 *   <li>Provides hot-reload capability for development</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * // Load a specific template
 * ScriptTemplate template = ScriptTemplateLoader.load("mining_basic");
 *
 * // Get all available templates
 * List<ScriptTemplate> templates = ScriptTemplateLoader.loadAll();
 *
 * // Get template by category
 * List<ScriptTemplate> miningTemplates =
 *     ScriptTemplateLoader.loadByCategory("resource_gathering");
 *
 * // Reload templates (hot-reload for development)
 * ScriptTemplateLoader.reload();
 * }</pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is thread-safe. Templates can be loaded concurrently from multiple threads.</p>
 *
 * @see ScriptTemplate
 * @see Script
 * @since 1.3.0
 */
public class ScriptTemplateLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptTemplateLoader.class);

    /**
     * Default templates directory path.
     */
    private static final String DEFAULT_TEMPLATES_DIR = "config/templates";

    /**
     * Cached templates by ID.
     */
    private static final Map<String, ScriptTemplate> TEMPLATE_CACHE = new ConcurrentHashMap<>();

    /**
     * Gson instance with custom deserializers.
     */
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(ScriptNode.class, new ScriptNodeDeserializer())
        .registerTypeAdapter(new TypeToken<List<ScriptTemplate.TemplateParameter>>(){}.getType(),
                            new TemplateParameterListDeserializer())
        .create();

    /**
     * Templates directory path.
     */
    private static String templatesDir = DEFAULT_TEMPLATES_DIR;

    private ScriptTemplateLoader() {
        // Utility class - prevent instantiation
    }

    /**
     * Sets the templates directory path.
     *
     * @param dir Path to templates directory
     */
    public static void setTemplatesDirectory(String dir) {
        if (dir == null || dir.trim().isEmpty()) {
            throw new IllegalArgumentException("Templates directory cannot be null or empty");
        }
        templatesDir = dir;
        LOGGER.info("Templates directory set to: {}", templatesDir);
    }

    /**
     * Gets the current templates directory path.
     *
     * @return Templates directory path
     */
    public static String getTemplatesDirectory() {
        return templatesDir;
    }

    /**
     * Loads a script template by ID.
     *
     * <p>The template ID corresponds to the filename without the .json extension.</p>
     *
     * @param templateId Template ID (filename without .json extension)
     * @return The loaded template, or null if not found
     * @throws ScriptTemplateException if template loading fails
     */
    public static ScriptTemplate load(String templateId) throws ScriptTemplateException {
        if (templateId == null || templateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Template ID cannot be null or empty");
        }

        // Check cache first
        ScriptTemplate cached = TEMPLATE_CACHE.get(templateId);
        if (cached != null) {
            LOGGER.debug("Template '{}' loaded from cache", templateId);
            return cached;
        }

        // Load from file
        Path templatePath = Paths.get(templatesDir, templateId + ".json");

        if (!Files.exists(templatePath)) {
            LOGGER.warn("Template file not found: {}", templatePath);
            return null;
        }

        try {
            String json = Files.readString(templatePath);
            ScriptTemplate template = parseTemplate(json, templateId);

            // Cache the template
            TEMPLATE_CACHE.put(templateId, template);

            LOGGER.info("Loaded template '{}' from {}", templateId, templatePath);
            return template;

        } catch (IOException e) {
            LOGGER.error("Failed to read template file: {}", templatePath, e);
            throw new ScriptTemplateException("Failed to read template: " + templateId, e);
        } catch (Exception e) {
            LOGGER.error("Failed to parse template: {}", templateId, e);
            throw new ScriptTemplateException("Failed to parse template: " + templateId, e);
        }
    }

    /**
     * Loads all templates from the templates directory.
     *
     * @return List of all loaded templates
     */
    public static List<ScriptTemplate> loadAll() {
        List<ScriptTemplate> templates = new ArrayList<>();

        try {
            Path templatesPath = Paths.get(templatesDir);

            if (!Files.exists(templatesPath)) {
                LOGGER.warn("Templates directory does not exist: {}", templatesDir);
                return templates;
            }

            Files.list(templatesPath)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    String templateId = getTemplateId(path);
                    try {
                        ScriptTemplate template = load(templateId);
                        if (template != null) {
                            templates.add(template);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to load template: {}", templateId, e);
                    }
                });

            LOGGER.info("Loaded {} templates from {}", templates.size(), templatesDir);
            return templates;

        } catch (IOException e) {
            LOGGER.error("Failed to list templates directory: {}", templatesDir, e);
            return templates;
        }
    }

    /**
     * Loads templates by category.
     *
     * @param category Category to filter by
     * @return List of templates in the category
     */
    public static List<ScriptTemplate> loadByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return loadAll().stream()
            .filter(t -> category.equals(t.getMetadata().getCategory()))
            .toList();
    }

    /**
     * Gets a template from cache without loading from disk.
     *
     * @param templateId Template ID
     * @return Cached template, or null if not cached
     */
    public static ScriptTemplate getCached(String templateId) {
        return TEMPLATE_CACHE.get(templateId);
    }

    /**
     * Checks if a template exists in the cache.
     *
     * @param templateId Template ID
     * @return true if template is cached
     */
    public static boolean isCached(String templateId) {
        return TEMPLATE_CACHE.containsKey(templateId);
    }

    /**
     * Clears the template cache.
     */
    public static void clearCache() {
        TEMPLATE_CACHE.clear();
        LOGGER.info("Template cache cleared");
    }

    /**
     * Reloads all templates from disk.
     *
     * <p>Useful for development when templates are modified.</p>
     */
    public static void reload() {
        clearCache();
        loadAll();
        LOGGER.info("All templates reloaded");
    }

    /**
     * Reloads a specific template.
     *
     * @param templateId Template ID to reload
     * @return Reloaded template, or null if not found
     */
    public static ScriptTemplate reload(String templateId) {
        TEMPLATE_CACHE.remove(templateId);
        return load(templateId);
    }

    /**
     * Gets all cached template IDs.
     *
     * @return Set of cached template IDs
     */
    public static java.util.Set<String> getCachedTemplateIds() {
        return Collections.unmodifiableSet(TEMPLATE_CACHE.keySet());
    }

    /**
     * Parses a template from JSON string.
     */
    private static ScriptTemplate parseTemplate(String json, String templateId) {
        try {
            ScriptTemplate template = GSON.fromJson(json, ScriptTemplate.class);

            // Validate template
            validateTemplate(template, templateId);

            return template;

        } catch (Exception e) {
            throw new ScriptTemplateException("Failed to parse template JSON: " + templateId, e);
        }
    }

    /**
     * Validates a loaded template.
     */
    private static void validateTemplate(ScriptTemplate template, String templateId) {
        if (template.getMetadata() == null) {
            throw new ScriptTemplateException("Template missing metadata: " + templateId);
        }

        if (template.getMetadata().getId() == null ||
            template.getMetadata().getId().trim().isEmpty()) {
            throw new ScriptTemplateException("Template missing ID: " + templateId);
        }

        if (template.getRootNode() == null) {
            throw new ScriptTemplateException("Template missing root node: " + templateId);
        }

        // Validate parameters
        if (template.getParameters() != null) {
            for (ScriptTemplate.TemplateParameter param : template.getParameters()) {
                if (param.getName() == null || param.getName().trim().isEmpty()) {
                    throw new ScriptTemplateException(
                        "Template has parameter without name: " + templateId
                    );
                }
            }
        }
    }

    /**
     * Extracts template ID from file path.
     */
    private static String getTemplateId(Path path) {
        String filename = path.getFileName().toString();
        return filename.substring(0, filename.lastIndexOf('.'));
    }

    /**
     * Custom deserializer for ScriptNode.
     */
    private static class ScriptNodeDeserializer implements JsonDeserializer<ScriptNode> {

        @Override
        public ScriptNode deserialize(JsonElement json, Type typeOfT,
                                     JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();

            // Parse node type
            String typeStr = obj.get("type").getAsString();
            ScriptNode.NodeType nodeType;
            try {
                nodeType = ScriptNode.NodeType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("Unknown node type: " + typeStr);
            }

            ScriptNode.Builder builder = ScriptNode.builder().type(nodeType);

            // Parse action
            if (obj.has("action")) {
                builder.withAction(obj.get("action").getAsString());
            }

            // Parse condition
            if (obj.has("condition")) {
                builder.withCondition(obj.get("condition").getAsString());
            }

            // Parse parameters
            if (obj.has("parameters")) {
                JsonObject paramsObj = obj.get("parameters").getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : paramsObj.entrySet()) {
                    String value = entry.getValue().getAsString();
                    builder.addParameter(entry.getKey(), value);
                }
            }

            // Parse description (stored in metadata)
            if (obj.has("description")) {
                builder.addMetadata("description", obj.get("description").getAsString());
            }

            // Parse children
            if (obj.has("children")) {
                JsonElement childrenElement = obj.get("children");
                if (childrenElement.isJsonArray()) {
                    for (JsonElement childElement : childrenElement.getAsJsonArray()) {
                        ScriptNode child = context.deserialize(childElement, ScriptNode.class);
                        builder.addChild(child);
                    }
                }
            }

            // Parse steps (alias for children)
            if (obj.has("steps")) {
                JsonElement stepsElement = obj.get("steps");
                if (stepsElement.isJsonArray()) {
                    for (JsonElement stepElement : stepsElement.getAsJsonArray()) {
                        ScriptNode child = context.deserialize(stepElement, ScriptNode.class);
                        builder.addChild(child);
                    }
                }
            }

            // Parse then/else for IF nodes
            if (obj.has("then")) {
                ScriptNode thenNode = context.deserialize(obj.get("then"), ScriptNode.class);
                builder.addChild(thenNode);
            }

            if (obj.has("else")) {
                ScriptNode elseNode = context.deserialize(obj.get("else"), ScriptNode.class);
                builder.addChild(elseNode);
            }

            // Parse fallback for SELECTOR nodes
            if (obj.has("fallback")) {
                ScriptNode fallbackNode = context.deserialize(obj.get("fallback"), ScriptNode.class);
                builder.addChild(fallbackNode);
            }

            return builder.build();
        }
    }

    /**
     * Custom deserializer for TemplateParameter list.
     */
    private static class TemplateParameterListDeserializer
        implements JsonDeserializer<List<ScriptTemplate.TemplateParameter>> {

        @Override
        public List<ScriptTemplate.TemplateParameter> deserialize(
            JsonElement json, Type typeOfT, JsonDeserializationContext context
        ) throws JsonParseException {
            List<ScriptTemplate.TemplateParameter> parameters = new ArrayList<>();

            if (json.isJsonArray()) {
                for (JsonElement element : json.getAsJsonArray()) {
                    JsonObject paramObj = element.getAsJsonObject();

                    String name = paramObj.get("name").getAsString();
                    String type = paramObj.get("type").getAsString();
                    Object defaultValue = paramObj.has("default")
                        ? paramObj.get("default").getAsString()
                        : null;
                    boolean required = paramObj.has("required")
                        ? paramObj.get("required").getAsBoolean()
                        : false;
                    String description = paramObj.has("description")
                        ? paramObj.get("description").getAsString()
                        : null;

                    List<String> allowedValues = null;
                    if (paramObj.has("allowed_values")) {
                        allowedValues = new ArrayList<>();
                        for (JsonElement value : paramObj.get("allowed_values").getAsJsonArray()) {
                            allowedValues.add(value.getAsString());
                        }
                    }

                    parameters.add(new ScriptTemplate.TemplateParameter(
                        name, type, defaultValue, required, description, allowedValues
                    ));
                }
            }

            return parameters;
        }
    }

    /**
     * Exception thrown when template loading fails.
     */
    public static class ScriptTemplateException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ScriptTemplateException(String message) {
            super(message);
        }

        public ScriptTemplateException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
