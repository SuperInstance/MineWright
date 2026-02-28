package com.minewright.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minewright.action.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Parses AI responses containing structured task data in JSON format.
 * Handles Gson's LazilyParsedNumber by converting to proper Integer/Double types.
 */
public class ResponseParser {

    // Optional logger for error reporting (can be set in tests or production)
    private static volatile Consumer<String> errorLogger = null;

    /**
     * Sets an optional error logger for parsing failures.
     * Useful for test environments where the main logger may not be available.
     */
    public static void setErrorLogger(Consumer<String> logger) {
        errorLogger = logger;
    }

    private static void logError(String message, Throwable cause) {
        Consumer<String> logger = errorLogger;
        if (logger != null) {
            logger.accept(message + ": " + cause.getMessage());
        }
    }

    public static ParsedResponse parseAIResponse(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        try {
            String jsonString = extractJSON(response);

            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

            String reasoning = json.has("reasoning") ? json.get("reasoning").getAsString() : "";
            String plan = json.has("plan") ? json.get("plan").getAsString() : "";
            List<Task> tasks = new ArrayList<>();

            if (json.has("tasks") && json.get("tasks").isJsonArray()) {
                JsonArray tasksArray = json.getAsJsonArray("tasks");

                for (JsonElement taskElement : tasksArray) {
                    if (taskElement.isJsonObject()) {
                        JsonObject taskObj = taskElement.getAsJsonObject();
                        Task task = parseTask(taskObj);
                        if (task != null) {
                            tasks.add(task);
                        }
                    }
                }
            }

            return new ParsedResponse(reasoning, plan, tasks);

        } catch (Exception e) {
            logError("Failed to parse AI response: " + response, e);
            return null;
        }
    }

    private static String extractJSON(String response) {
        String cleaned = response.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        cleaned = cleaned.trim();

        // Fix common JSON formatting issues
        cleaned = cleaned.replaceAll("\\n\\s*", " ");

        // Fix missing commas between array/object elements (common AI mistake)
        cleaned = cleaned.replaceAll("}\\s+\\{", "},{");
        cleaned = cleaned.replaceAll("}\\s+\\[", "},[");
        cleaned = cleaned.replaceAll("]\\s+\\{", "],{");
        cleaned = cleaned.replaceAll("]\\s+\\[", "],[");

        return cleaned;
    }

    /**
     * Converts a Gson JsonElement to a proper Java Object.
     * Handles LazilyParsedNumber by converting to Integer or Double.
     * Recursively handles nested objects and arrays.
     */
    private static Object convertJsonElement(JsonElement element) {
        if (element.isJsonNull()) {
            return null;
        } else if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            } else if (element.getAsJsonPrimitive().isNumber()) {
                // Convert LazilyParsedNumber to proper Integer/Double
                Number number = element.getAsNumber();
                // Check if it's a whole number that fits in an integer
                double doubleValue = number.doubleValue();
                if (doubleValue == number.intValue() &&
                    doubleValue >= Integer.MIN_VALUE &&
                    doubleValue <= Integer.MAX_VALUE) {
                    return number.intValue();
                }
                return number.doubleValue();
            } else {
                return element.getAsString();
            }
        } else if (element.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                list.add(convertJsonElement(item));
            }
            return list;
        } else if (element.isJsonObject()) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), convertJsonElement(entry.getValue()));
            }
            return map;
        }
        return null;
    }

    private static Task parseTask(JsonObject taskObj) {
        if (!taskObj.has("action")) {
            return null;
        }

        String action = taskObj.get("action").getAsString();
        Map<String, Object> parameters = new HashMap<>();

        if (taskObj.has("parameters") && taskObj.get("parameters").isJsonObject()) {
            JsonObject paramsObj = taskObj.getAsJsonObject("parameters");

            for (String key : paramsObj.keySet()) {
                JsonElement value = paramsObj.get(key);
                parameters.put(key, convertJsonElement(value));
            }
        }

        return new Task(action, parameters);
    }

    public static class ParsedResponse {
        private final String reasoning;
        private final String plan;
        private final List<Task> tasks;

        public ParsedResponse(String reasoning, String plan, List<Task> tasks) {
            this.reasoning = reasoning;
            this.plan = plan;
            this.tasks = tasks;
        }

        public String getReasoning() {
            return reasoning;
        }

        public String getPlan() {
            return plan;
        }

        public List<Task> getTasks() {
            return tasks;
        }
    }
}
