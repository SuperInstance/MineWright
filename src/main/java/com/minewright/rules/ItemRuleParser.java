package com.minewright.rules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses item rules from JSON format.
 *
 * JSON format:
 * <pre>
 * {
 *   "name": "Keep diamonds",
 *   "conditions": [
 *     {"field": "item_type", "operator": "==", "value": "diamond"}
 *   ],
 *   "action": "KEEP",
 *   "priority": 100
 * }
 * </pre>
 */
public class ItemRuleParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemRuleParser.class);
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Parse a single rule from JSON string.
     */
    public static ItemRule parseRule(String json) {
        try {
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            return parseRule(jsonObject);
        } catch (Exception e) {
            LOGGER.error("Failed to parse rule from JSON: {}", json, e);
            return null;
        }
    }

    /**
     * Parse a single rule from JSON object.
     */
    public static ItemRule parseRule(JsonObject json) {
        try {
            String name = json.has("name") ? json.get("name").getAsString() : "Unnamed Rule";
            RuleAction action = RuleAction.valueOf(json.get("action").getAsString());
            int priority = json.has("priority") ? json.get("priority").getAsInt() : 0;
            boolean enabled = json.has("enabled") ? json.get("enabled").getAsBoolean() : true;

            List<RuleCondition> conditions = new ArrayList<>();
            if (json.has("conditions")) {
                JsonArray conditionsArray = json.getAsJsonArray("conditions");
                for (JsonElement conditionElement : conditionsArray) {
                    JsonObject conditionObj = conditionElement.getAsJsonObject();
                    String field = conditionObj.get("field").getAsString();
                    String operator = conditionObj.get("operator").getAsString();
                    Object value = parseValue(conditionObj.get("value"));
                    conditions.add(new RuleCondition(field, operator, value));
                }
            }

            ItemRule rule = new ItemRule(name, conditions, action, priority);
            rule.setEnabled(enabled);
            return rule;
        } catch (Exception e) {
            LOGGER.error("Failed to parse rule from JSON object", e);
            return null;
        }
    }

    /**
     * Parse a list of rules from a JSON file.
     */
    public static List<ItemRule> parseFile(Path path) {
        List<ItemRule> rules = new ArrayList<>();

        if (!Files.exists(path)) {
            LOGGER.warn("Rule file does not exist: {}", path);
            return rules;
        }

        try (Reader reader = new FileReader(path.toFile())) {
            JsonElement root = JsonParser.parseReader(reader);

            if (root.isJsonArray()) {
                // Array of rules
                JsonArray rulesArray = root.getAsJsonArray();
                for (JsonElement ruleElement : rulesArray) {
                    ItemRule rule = parseRule(ruleElement.getAsJsonObject());
                    if (rule != null) {
                        rules.add(rule);
                    }
                }
            } else if (root.isJsonObject()) {
                // Single rule or object with "rules" array
                JsonObject rootObj = root.getAsJsonObject();
                if (rootObj.has("rules")) {
                    JsonArray rulesArray = rootObj.getAsJsonArray("rules");
                    for (JsonElement ruleElement : rulesArray) {
                        ItemRule rule = parseRule(ruleElement.getAsJsonObject());
                        if (rule != null) {
                            rules.add(rule);
                        }
                    }
                } else {
                    ItemRule rule = parseRule(rootObj);
                    if (rule != null) {
                        rules.add(rule);
                    }
                }
            }

            LOGGER.info("Loaded {} rules from {}", rules.size(), path);
        } catch (IOException e) {
            LOGGER.error("Failed to read rule file: {}", path, e);
        }

        return rules;
    }

    /**
     * Parse a value from JSON, handling strings, numbers, and booleans.
     */
    private static Object parseValue(JsonElement element) {
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isString()) {
                return element.getAsString();
            } else if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsNumber();
            } else if (element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            }
        }
        return element.toString();
    }

    /**
     * Convert a rule to JSON string.
     */
    public static String toJson(ItemRule rule) {
        return GSON.toJson(rule);
    }

    /**
     * Convert a list of rules to JSON string.
     */
    public static String toJson(List<ItemRule> rules) {
        return GSON.toJson(rules);
    }
}
