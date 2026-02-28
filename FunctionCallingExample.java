package com.minewright.llm;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/**
 * Simplified Function Calling Example for Dissertation Appendix.
 *
 * This demonstrates the core concepts of function calling with minimal complexity,
 * suitable for educational purposes and code examples in academic writing.
 */
public class FunctionCallingExample {

    public static class MinecraftTool {
        private final String name;
        private final String description;
        private final String jsonSchema;

        public MinecraftTool(String name, String description, String jsonSchema) {
            this.name = name;
            this.description = description;
            this.jsonSchema = jsonSchema;
        }

        public JsonObject toOpenAIFunction() {
            JsonObject function = new JsonObject();
            function.addProperty("name", name);
            function.addProperty("description", description);

            JsonObject parameters = JsonParser.parseString(jsonSchema).getAsJsonObject();
            function.add("parameters", parameters);

            return function;
        }
    }

    /**
     * Example: Defining Minecraft actions as function calling tools.
     */
    public static List<MinecraftTool> getMinecraftTools() {
        List<MinecraftTool> tools = new ArrayList<>();

        // Mine Block Tool
        tools.add(new MinecraftTool(
            "mine_block",
            "Mine a specific block type in the Minecraft world",
            """
            {
                "type": "object",
                "properties": {
                    "block_type": {
                        "type": "string",
                        "enum": ["iron_ore", "coal_ore", "diamond_ore", "copper_ore"],
                        "description": "Type of block to mine"
                    },
                    "quantity": {
                        "type": "integer",
                        "minimum": 1,
                        "maximum": 64,
                        "description": "Number of blocks to mine"
                    }
                },
                "required": ["block_type", "quantity"]
            }
            """
        ));

        // Place Block Tool
        tools.add(new MinecraftTool(
            "place_block",
            "Place a block at specified world coordinates",
            """
            {
                "type": "object",
                "properties": {
                    "block_type": {
                        "type": "string",
                        "enum": ["oak_planks", "stone_bricks", "cobblestone", "glass"],
                        "description": "Minecraft block ID"
                    },
                    "position": {
                        "type": "object",
                        "properties": {
                            "x": {"type": "integer"},
                            "y": {"type": "integer"},
                            "z": {"type": "integer"}
                        },
                        "required": ["x", "y", "z"]
                    }
                },
                "required": ["block_type", "position"]
            }
            """
        ));

        // Pathfind Tool
        tools.add(new MinecraftTool(
            "pathfind",
            "Navigate to specified world coordinates",
            """
            {
                "type": "object",
                "properties": {
                    "target": {
                        "type": "object",
                        "properties": {
                            "x": {"type": "integer"},
                            "y": {"type": "integer"},
                            "z": {"type": "integer"}
                        },
                        "required": ["x", "y", "z"]
                    }
                },
                "required": ["target"]
            }
            """
        ));

        return tools;
    }

    /**
     * Example: Sending a function calling request to OpenAI.
     */
    public static String sendFunctionCallRequest(String apiKey, String userMessage) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // Build the request with tool definitions
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4");
        requestBody.addProperty("temperature", 0.7);

        // Add messages
        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content",
            "You are a Minecraft AI assistant. Break down player commands into specific actions " +
            "using the available function calls.");
        messages.add(systemMessage);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        // Add tool definitions
        JsonArray tools = new JsonArray();
        for (MinecraftTool tool : getMinecraftTools()) {
            JsonObject toolWrapper = new JsonObject();
            toolWrapper.addProperty("type", "function");
            toolWrapper.add("function", tool.toOpenAIFunction());
            tools.add(toolWrapper);
        }
        requestBody.add("tools", tools);
        requestBody.addProperty("tool_choice", "auto");

        // Send request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    /**
     * Example: Parsing function calls from OpenAI response.
     */
    public static List<GameAction> parseFunctionCalls(String responseJson) {
        List<GameAction> actions = new ArrayList<>();

        JsonObject response = JsonParser.parseString(responseJson).getAsJsonObject();
        JsonArray choices = response.getAsJsonArray("choices");

        if (choices != null && choices.size() > 0) {
            JsonObject message = choices.get(0).getAsJsonObject()
                .getAsJsonObject("message");
            JsonArray toolCalls = message.getAsJsonArray("tool_calls");

            if (toolCalls != null) {
                for (JsonElement toolCall : toolCalls) {
                    JsonObject tc = toolCall.getAsJsonObject();
                    JsonObject function = tc.getAsJsonObject("function");

                    String functionName = function.get("name").getAsString();
                    String argumentsStr = function.get("arguments").getAsString();
                    JsonObject arguments = JsonParser.parseString(argumentsStr).getAsJsonObject();

                    actions.add(new GameAction(functionName, arguments));
                }
            }
        }

        return actions;
    }

    /**
     * Example: Executing parsed function calls as game actions.
     */
    public static void executeGameActions(List<GameAction> actions) {
        for (GameAction action : actions) {
            System.out.println("Executing action: " + action.name);

            switch (action.name) {
                case "mine_block" -> {
                    String blockType = action.arguments.get("block_type").getAsString();
                    int quantity = action.arguments.get("quantity").getAsInt();
                    System.out.println("  → Mining " + quantity + "x " + blockType);
                    // Execute mining logic here
                }

                case "place_block" -> {
                    String blockType = action.arguments.get("block_type").getAsString();
                    JsonObject position = action.arguments.getAsJsonObject("position");
                    int x = position.get("x").getAsInt();
                    int y = position.get("y").getAsInt();
                    int z = position.get("z").getAsInt();
                    System.out.println("  → Placing " + blockType + " at (" + x + ", " + y + ", " + z + ")");
                    // Execute placement logic here
                }

                case "pathfind" -> {
                    JsonObject target = action.arguments.getAsJsonObject("target");
                    int x = target.get("x").getAsInt();
                    int y = target.get("y").getAsInt();
                    int z = target.get("z").getAsInt();
                    System.out.println("  → Pathfinding to (" + x + ", " + y + ", " + z + ")");
                    // Execute pathfinding logic here
                }

                default -> System.out.println("  → Unknown action: " + action.name);
            }
        }
    }

    /**
     * Simple data structure for parsed game actions.
     */
    public static class GameAction {
        public final String name;
        public final JsonObject arguments;

        public GameAction(String name, JsonObject arguments) {
            this.name = name;
            this.arguments = arguments;
        }
    }

    /**
     * Complete usage example.
     */
    public static void main(String[] args) {
        try {
            // Example: Process a natural language command
            String playerCommand = "Mine 10 iron ore blocks";

            System.out.println("Player command: " + playerCommand);
            System.out.println("Sending to OpenAI with function calling...\n");

            // In real usage, use your actual API key
            String apiKey = "your-api-key-here";
            String response = sendFunctionCallRequest(apiKey, playerCommand);

            System.out.println("LLM Response:");
            System.out.println(response);
            System.out.println();

            // Parse function calls
            List<GameAction> actions = parseFunctionCalls(response);

            // Execute actions
            System.out.println("Executing " + actions.size() + " actions:\n");
            executeGameActions(actions);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
