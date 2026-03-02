package com.minewright.script;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.util.*;

/**
 * Represents a script template that can be loaded from JSON files and instantiated.
 *
 * <p>Script templates are reusable blueprints for agent behaviors. They contain
 * parameterized behavior trees that can be customized with specific values when
 * instantiated.</p>
 *
 * <p><b>Template Structure:</b></p>
 * <pre>
 * ScriptTemplate {
 *   metadata: TemplateMetadata
 *   parameters: List[TemplateParameter]
 *   requirements: TemplateRequirements
 *   triggers: List[Trigger]
 *   root_node: ScriptNode
 *   error_handlers: Map[String, List[ScriptNode]]
 *   telemetry: TemplateTelemetry
 * }
 * </pre>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * // Load template
 * ScriptTemplate template = ScriptTemplateLoader.load("mining_basic");
 *
 * // Instantiate with parameters
 * Map<String, Object> params = new HashMap<>();
 * params.put("block_type", "diamond_ore");
 * params.put("target_quantity", 32);
 *
 * Script script = template.instantiate(params);
 * }</pre>
 *
 * @see ScriptNode
 * @see ScriptTemplateLoader
 * @see Script
 * @since 1.3.0
 */
public class ScriptTemplate {

    private final TemplateMetadata metadata;
    private final List<TemplateParameter> parameters;
    private final TemplateRequirements requirements;
    private final List<Trigger> triggers;
    private final ScriptNode rootNode;
    private final Map<String, List<ScriptNode>> errorHandlers;
    private final TemplateTelemetry telemetry;

    private ScriptTemplate(Builder builder) {
        this.metadata = builder.metadata;
        this.parameters = Collections.unmodifiableList(new ArrayList<>(builder.parameters));
        this.requirements = builder.requirements;
        this.triggers = Collections.unmodifiableList(new ArrayList<>(builder.triggers));
        this.rootNode = builder.rootNode;
        this.errorHandlers = Collections.unmodifiableMap(new HashMap<>(builder.errorHandlers));
        this.telemetry = builder.telemetry;
    }

    /**
     * Creates a new builder for constructing script templates.
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public TemplateMetadata getMetadata() { return metadata; }
    public List<TemplateParameter> getParameters() { return parameters; }
    public TemplateRequirements getRequirements() { return requirements; }
    public List<Trigger> getTriggers() { return triggers; }
    public ScriptNode getRootNode() { return rootNode; }
    public Map<String, List<ScriptNode>> getErrorHandlers() { return errorHandlers; }
    public TemplateTelemetry getTelemetry() { return telemetry; }

    /**
     * Gets the template ID.
     */
    public String getId() {
        return metadata != null ? metadata.getId() : null;
    }

    /**
     * Gets the template name.
     */
    public String getName() {
        return metadata != null ? metadata.getName() : null;
    }

    /**
     * Gets the template description.
     */
    public String getDescription() {
        return metadata != null ? metadata.getDescription() : null;
    }

    /**
     * Instantiates this template with the given parameters.
     *
     * <p>This creates a concrete Script by replacing template parameters
     * with their actual values. Parameters use ${param_name} syntax in the template.</p>
     *
     * @param parameters Parameter values to substitute
     * @return A new Script with parameters applied
     */
    public Script instantiate(Map<String, Object> parameters) {
        // Validate required parameters
        for (TemplateParameter param : this.parameters) {
            if (param.isRequired() && !parameters.containsKey(param.getName())) {
                throw new IllegalArgumentException(
                    "Missing required parameter: " + param.getName()
                );
            }
        }

        // Apply default values for missing optional parameters
        Map<String, Object> resolvedParams = new HashMap<>(parameters);
        for (TemplateParameter param : this.parameters) {
            if (!resolvedParams.containsKey(param.getName()) && param.getDefaultValue() != null) {
                resolvedParams.put(param.getName(), param.getDefaultValue());
            }
        }

        // Create script from template
        Script.Builder scriptBuilder = Script.builder();

        // Build metadata
        Script.ScriptMetadata metadata = Script.ScriptMetadata.builder()
            .id(this.metadata.getId() + "_" + UUID.randomUUID().toString().substring(0, 8))
            .name(this.metadata.getName())
            .description(this.metadata.getDescription())
            .author(this.metadata.getAuthor())
            .tags(this.metadata.getTags())
            .build();

        scriptBuilder.metadata(metadata);

        // Build parameters
        for (TemplateParameter param : this.parameters) {
            Object value = resolvedParams.get(param.getName());
            Script.Parameter scriptParam = new Script.Parameter(
                param.getName(),
                param.getType(),
                value,
                param.isRequired(),
                param.getDescription()
            );
            scriptBuilder.addParameter(param.getName(), scriptParam);
        }

        // Build requirements
        if (this.requirements != null) {
            Script.ScriptRequirements.Builder reqBuilder = Script.ScriptRequirements.builder();
            if (this.requirements.getInventory() != null) {
                for (ItemRequirement item : this.requirements.getInventory()) {
                    reqBuilder.addInventoryItem(item.getItem(), item.getQuantity());
                }
            }
            if (this.requirements.getTools() != null) {
                reqBuilder.tools(this.requirements.getTools());
            }
            reqBuilder
                .maxExecutionTime(this.requirements.getMaxExecutionTime())
                .maxDistance(this.requirements.getMaxDistance());
            scriptBuilder.requirements(reqBuilder.build());
        }

        // Clone and parameterize root node
        ScriptNode instantiatedRoot = parameterizeNode(rootNode, resolvedParams);
        scriptBuilder.scriptNode(instantiatedRoot);

        // Clone and parameterize error handlers
        for (Map.Entry<String, List<ScriptNode>> entry : errorHandlers.entrySet()) {
            for (ScriptNode handler : entry.getValue()) {
                ScriptNode instantiatedHandler = parameterizeNode(handler, resolvedParams);
                scriptBuilder.addErrorHandler(entry.getKey(), instantiatedHandler);
            }
        }

        // Build telemetry
        if (this.telemetry != null) {
            Script.ScriptTelemetry scriptTelemetry = new Script.ScriptTelemetry(
                this.telemetry.getLogLevel(),
                this.telemetry.getMetrics()
            );
            scriptBuilder.telemetry(scriptTelemetry);
        }

        return scriptBuilder.build();
    }

    /**
     * Creates an instance with default parameters.
     */
    public Script instantiate() {
        return instantiate(new HashMap<>());
    }

    /**
     * Recursively parameterizes a script node by replacing ${param} placeholders.
     */
    private ScriptNode parameterizeNode(ScriptNode node, Map<String, Object> parameters) {
        if (node == null) {
            return null;
        }

        ScriptNode.Builder builder = ScriptNode.builder()
            .type(node.getType())
            .withAction(node.getAction())
            .withCondition(node.getCondition())
            .metadata(node.getMetadata());

        // Parameterize node parameters
        for (Map.Entry<String, Object> entry : node.getParameters().entrySet()) {
            String value = parameterizeValue(String.valueOf(entry.getValue()), parameters);
            builder.addParameter(entry.getKey(), value);
        }

        // Recursively parameterize children
        for (ScriptNode child : node.getChildren()) {
            builder.addChild(parameterizeNode(child, parameters));
        }

        return builder.build();
    }

    /**
     * Replaces ${param} placeholders in a string value.
     */
    private String parameterizeValue(String value, Map<String, Object> parameters) {
        if (value == null) {
            return null;
        }

        String result = value;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }

        return result;
    }

    /**
     * Converts this template to JSON format.
     */
    public String toJson() {
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(ScriptNode.class, new ScriptNodeSerializer())
            .create();

        return gson.toJson(this);
    }

    /**
     * Builder for constructing ScriptTemplate instances.
     */
    public static class Builder {
        private TemplateMetadata metadata;
        private List<TemplateParameter> parameters = new ArrayList<>();
        private TemplateRequirements requirements;
        private List<Trigger> triggers = new ArrayList<>();
        private ScriptNode rootNode;
        private Map<String, List<ScriptNode>> errorHandlers = new HashMap<>();
        private TemplateTelemetry telemetry;

        public Builder metadata(TemplateMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder addParameter(TemplateParameter parameter) {
            this.parameters.add(parameter);
            return this;
        }

        public Builder parameters(List<TemplateParameter> parameters) {
            this.parameters = new ArrayList<>(parameters);
            return this;
        }

        public Builder requirements(TemplateRequirements requirements) {
            this.requirements = requirements;
            return this;
        }

        public Builder addTrigger(Trigger trigger) {
            this.triggers.add(trigger);
            return this;
        }

        public Builder triggers(List<Trigger> triggers) {
            this.triggers = new ArrayList<>(triggers);
            return this;
        }

        public Builder rootNode(ScriptNode rootNode) {
            this.rootNode = rootNode;
            return this;
        }

        public Builder addErrorHandler(String errorType, ScriptNode handler) {
            this.errorHandlers.computeIfAbsent(errorType, k -> new ArrayList<>()).add(handler);
            return this;
        }

        public Builder errorHandlers(Map<String, List<ScriptNode>> errorHandlers) {
            this.errorHandlers = new HashMap<>(errorHandlers);
            return this;
        }

        public Builder telemetry(TemplateTelemetry telemetry) {
            this.telemetry = telemetry;
            return this;
        }

        public ScriptTemplate build() {
            if (metadata == null) {
                throw new IllegalStateException("Metadata is required");
            }
            if (rootNode == null) {
                throw new IllegalStateException("Root node is required");
            }
            return new ScriptTemplate(this);
        }
    }

    /**
     * Template metadata.
     */
    public static class TemplateMetadata {
        @SerializedName("id")
        private final String id;

        @SerializedName("name")
        private final String name;

        @SerializedName("description")
        private final String description;

        @SerializedName("author")
        private final String author;

        @SerializedName("version")
        private final String version;

        @SerializedName("tags")
        private final List<String> tags;

        @SerializedName("category")
        private final String category;

        public TemplateMetadata(String id, String name, String description,
                              String author, String version, List<String> tags, String category) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.author = author;
            this.version = version;
            this.tags = tags != null ? Collections.unmodifiableList(new ArrayList<>(tags))
                                     : Collections.emptyList();
            this.category = category;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getAuthor() { return author; }
        public String getVersion() { return version; }
        public List<String> getTags() { return tags; }
        public String getCategory() { return category; }
    }

    /**
     * Template parameter definition.
     */
    public static class TemplateParameter {
        @SerializedName("name")
        private final String name;

        @SerializedName("type")
        private final String type;

        @SerializedName("default")
        private final Object defaultValue;

        @SerializedName("required")
        private final boolean required;

        @SerializedName("description")
        private final String description;

        @SerializedName("allowed_values")
        private final List<String> allowedValues;

        public TemplateParameter(String name, String type, Object defaultValue,
                               boolean required, String description, List<String> allowedValues) {
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
            this.required = required;
            this.description = description;
            this.allowedValues = allowedValues != null
                ? Collections.unmodifiableList(new ArrayList<>(allowedValues))
                : Collections.emptyList();
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public Object getDefaultValue() { return defaultValue; }
        public boolean isRequired() { return required; }
        public String getDescription() { return description; }
        public List<String> getAllowedValues() { return allowedValues; }
    }

    /**
     * Template requirements.
     */
    public static class TemplateRequirements {
        @SerializedName("inventory")
        private final List<ItemRequirement> inventory;

        @SerializedName("tools")
        private final List<String> tools;

        @SerializedName("maxExecutionTime")
        private final int maxExecutionTime;

        @SerializedName("maxDistance")
        private final int maxDistance;

        public TemplateRequirements(List<ItemRequirement> inventory, List<String> tools,
                                   int maxExecutionTime, int maxDistance) {
            this.inventory = inventory != null
                ? Collections.unmodifiableList(new ArrayList<>(inventory))
                : Collections.emptyList();
            this.tools = tools != null
                ? Collections.unmodifiableList(new ArrayList<>(tools))
                : Collections.emptyList();
            this.maxExecutionTime = maxExecutionTime;
            this.maxDistance = maxDistance;
        }

        public List<ItemRequirement> getInventory() { return inventory; }
        public List<String> getTools() { return tools; }
        public int getMaxExecutionTime() { return maxExecutionTime; }
        public int getMaxDistance() { return maxDistance; }
    }

    /**
     * Item requirement for templates.
     */
    public static class ItemRequirement {
        @SerializedName("item")
        private final String item;

        @SerializedName("quantity")
        private final int quantity;

        public ItemRequirement(String item, int quantity) {
            this.item = item;
            this.quantity = quantity;
        }

        public String getItem() { return item; }
        public int getQuantity() { return quantity; }
    }

    /**
     * Template telemetry configuration.
     */
    public static class TemplateTelemetry {
        @SerializedName("logLevel")
        private final String logLevel;

        @SerializedName("metrics")
        private final List<String> metrics;

        public TemplateTelemetry(String logLevel, List<String> metrics) {
            this.logLevel = logLevel;
            this.metrics = metrics != null
                ? Collections.unmodifiableList(new ArrayList<>(metrics))
                : Collections.emptyList();
        }

        public String getLogLevel() { return logLevel; }
        public List<String> getMetrics() { return metrics; }
    }

    /**
     * Gson serializer for ScriptNode in templates.
     */
    private static class ScriptNodeSerializer
        implements com.google.gson.JsonSerializer<ScriptNode> {

        @Override
        public com.google.gson.JsonElement serialize(ScriptNode node, java.lang.reflect.Type type,
                                                    com.google.gson.JsonSerializationContext context) {
            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
            obj.addProperty("type", node.getType().toString().toLowerCase());

            if (node.getAction() != null) {
                obj.addProperty("action", node.getAction());
            }

            if (node.getCondition() != null) {
                obj.addProperty("condition", node.getCondition());
            }

            if (!node.getParameters().isEmpty()) {
                com.google.gson.JsonObject params = new com.google.gson.JsonObject();
                for (Map.Entry<String, Object> entry : node.getParameters().entrySet()) {
                    params.addProperty(entry.getKey(), String.valueOf(entry.getValue()));
                }
                obj.add("parameters", params);
            }

            if (!node.getChildren().isEmpty()) {
                com.google.gson.JsonArray children = new com.google.gson.JsonArray();
                for (ScriptNode child : node.getChildren()) {
                    children.add(serialize(child, child.getClass(), context));
                }
                obj.add("children", children);
            }

            return obj;
        }
    }
}
