# LLM Tool/Function Calling Research & Best Practices

**Research Date:** 2026-02-27
**Target:** Improve ResponseParser and Action Execution in Steve AI
**Focus:** Schema design, error handling, multi-step execution, and feedback loops

---

## Executive Summary

This document compiles research on LLM function/tool calling patterns from major providers (OpenAI, Claude, Gemini) and provides actionable recommendations for improving Steve AI's ResponseParser and action execution system. Key findings emphasize structured output validation, progressive error handling, and multi-step orchestration patterns.

**Key Takeaway:** Modern tool calling has evolved from simple JSON extraction to sophisticated multi-agent orchestration with strict schema validation, parallel execution, and adaptive error recovery.

---

## 1. Current Implementation Analysis

### 1.1 Existing ResponseParser Architecture

Located at: `C:\Users\casey\steve\src\main\java\com\minewright\llm\ResponseParser.java`

**Current Approach:**
- Manual JSON extraction with regex-based cleanup
- Basic type conversion (primitives, arrays)
- Single-shot parsing with null returns on failure
- No schema validation
- Minimal error context

**Strengths:**
- Simple and straightforward
- Handles common JSON formatting issues (missing commas, markdown blocks)
- Type-aware parameter extraction

**Weaknesses:**
- No validation against expected schemas
- Silent failures (returns null without details)
- No retry mechanism with partial results
- No support for multi-step tool calling
- Missing structured output enforcement
- Limited error context for debugging

### 1.2 Current Prompt Strategy

Located at: `C:\Users\casey\steve\src\main\java\com\minewright\llm\PromptBuilder.java`

**Current Approach:**
- Text-based JSON format instructions
- Hardcoded action definitions in system prompt
- No formal schema definitions
- Relies on examples (few-shot learning)

**Opportunities:**
- Migrate to JSON Schema definitions
- Add strict mode enforcement (OpenAI)
- Implement tool/function calling APIs
- Add parameter validation schemas

---

## 2. Provider-Specific Best Practices

### 2.1 OpenAI Function Calling 2.0

**Sources:**
- [Microsoft Azure - Function Calling Guide](https://learn.microsoft.com/en-us/azure/ai-foundry/openai/how-to/function-calling)
- [OpenAI Function Calling 2.0 Guide](https://devpress.csdn.net/v1/article/detail/145210500)

#### Key Updates (2024+)

**Deprecated Parameters:**
- `functions` → Use `tools` instead
- `function_call` → Use `tool_choice` instead

**New Features:**
```json
{
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "build_structure",
        "description": "Build a structure in Minecraft",
        "parameters": {
          "type": "object",
          "properties": {
            "structure": {
              "type": "string",
              "enum": ["house", "castle", "tower"],
              "description": "Type of structure to build"
            },
            "blocks": {
              "type": "array",
              "items": {"type": "string"},
              "description": "List of block types to use"
            }
          },
          "required": ["structure", "blocks"]
        }
      }
    }
  ],
  "tool_choice": "auto"
}
```

**Strict Mode (New):**
```json
{
  "type": "function",
  "function": {
    "name": "mine_block",
    "strict": true,
    "parameters": {
      "type": "object",
      "properties": {...},
      "additionalProperties": false
    }
  }
}
```

#### Best Practices

1. **Clear Descriptions**
   - Describe purpose, format, and expected output
   - Include examples in descriptions
   - Specify constraints explicitly

2. **Software Engineering Principles**
   - Make functions intuitive (principle of least surprise)
   - Use enums to prevent invalid states
   - Pass the "intern test" - can someone use it correctly with only the schema?

3. **Reduce Model Burden**
   - Don't have the model fill in known parameters
   - Merge functions always called consecutively
   - Keep function count under 20 for accuracy

4. **Security**
   - Principle of least privilege
   - User confirmation for actions
   - Validate tool call sources
   - Encrypt sensitive data

### 2.2 Claude Tool Use Patterns

**Sources:**
- [Claude Advanced Tool Use](https://m.blog.csdn.net/comedyking/article/details/156086530)
- [Claude Tool Use Case Studies](https://juejin.cn/post/7508648111486304271)

#### Key Patterns

**Traditional Pattern:**
```
User → Claude calls tool → Wait → Claude processes → Next tool
```
- 20+ tool calls for complex queries
- All intermediate data in context
- Sequential execution

**Advanced Pattern (PTC - Programmatic Tool Calling):**
```
User → Claude chains tools programmatically → Batch results
```
- Reduced round-trips
- Less context pollution
- Better for multi-step workflows

#### Tool Search Tool
- Built-in tool discovery mechanism
- Dynamically finds 3-5 relevant tools
- Enables scalable tool ecosystems

#### Usage Statistics (2024-2025)
- 36% of usage for programming/code
- Tool call complexity increased 116% (9.8 → 21.2 calls)
- Human intervention decreased 33% (6.2 → 4.1 turns)
- 49.1% of interactions now automated

### 2.3 Gemini Function Calling

**Sources:**
- [Firebase Gemini Function Calling](https://firebase.google.cn/docs/ai-logic/function-calling)
- [Gemini Function Calling Guide](https://m.blog.csdn.net/sinat_37574187/article/details/149472668)

#### Key Features

**Function Declaration Format:**
```json
{
  "name": "fetch_weather",
  "description": "Get weather for a location",
  "parameters": {
    "type": "object",
    "properties": {
      "location": {
        "type": "object",
        "properties": {
          "city": {"type": "string"},
          "state": {"type": "string"}
        }
      },
      "date": {"type": "string", "format": "date"}
    },
    "required": ["location", "date"]
  }
}
```

**Supported Properties:**
- `type`, `nullable`, `required`, `format`, `description`
- `properties`, `items`, `enum`

**Not Supported:**
- `default`, `optional`, `maximum`, `oneOf`

#### Capabilities
- Up to 128 function declarations
- Gemini 1.5+ and 2.0 support
- Multimodal (text, image, audio, video)
- OpenAPI-compatible schema format

---

## 3. Structured Output Techniques

### 3.1 Three Main Methods (Ranked by Reliability)

**Sources:**
- [ThoughtWorks - Structured Output from LLMs](https://www.thoughtworks.cn/radar/techniques/structured-output-from-llms)
- [LangChain Structured Output Guide](https://m.csdn.net/qq_45583713/article/details/157330723)

| Method | Reliability | Description |
|--------|-------------|-------------|
| **JSON Schema** | ⭐⭐⭐ Highest | Native `response_format` with JSON Schema; OpenAI, Azure, Gemini, Mistral, Ollama |
| **JSON Mode + Prompting** | ⭐⭐ Medium | `responseMimeType=JSON` + prompt instructions |
| **Prompting Only** | ⭐ Lowest | Relies solely on prompt instructions |

### 3.2 OpenAI Structured Outputs

**Implementation:**
```java
JsonObject responseFormat = new JsonObject();
responseFormat.addProperty("type", "json_schema");

JsonObject jsonSchema = new JsonObject();
jsonSchema.addProperty("name", "minecraft_action");
jsonSchema.addProperty("strict", true);
jsonSchema.addProperty("schema", actionSchema);

responseFormat.add("json_schema", jsonSchema);

requestBody.add("response_format", responseFormat);
```

**Benefits:**
- Guarantees schema compliance
- Reduces hallucinations
- Better developer experience
- Production-ready

### 3.3 LangChain4j Integration

**Sources:**
- [LangChain4j Structured Output](https://m.blog.csdn.net/linwu_2006_2006/article/details/155737214)

**Two-Level Support:**
1. **ChatModel API** - Direct structured output
2. **AI Service API** - Declarative method mapping

---

## 4. Schema Design for Action Parameters

### 4.1 Current Action System

Located at: `C:\Users\casey\steve\src\main\java\com\minewright\action\Task.java`

**Current Approach:**
- Generic `Map<String, Object>` for parameters
- Type-safe getter methods with defaults
- No schema enforcement

### 4.2 Recommended Schema Approach

**Define Action Schemas:**
```java
public class ActionSchema {
    private final String name;
    private final String description;
    private final JsonObject parameterSchema;
    private final List<String> requiredParameters;

    // For OpenAI/Gemini function calling
    public JsonObject toFunctionDeclaration() {
        JsonObject function = new JsonObject();
        function.addProperty("name", name);
        function.addProperty("description", description);

        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");
        parameters.add("properties", parameterSchema);
        parameters.add("required", Gson().toJsonTree(requiredParameters));

        function.add("parameters", parameters);
        return function;
    }
}
```

**Example Schemas:**

```json
{
  "name": "build_structure",
  "description": "Construct a building structure in Minecraft",
  "parameters": {
    "type": "object",
    "properties": {
      "structure": {
        "type": "string",
        "enum": ["house", "castle", "tower", "barn", "modern"],
        "description": "Predefined structure type"
      },
      "blocks": {
        "type": "array",
        "items": {
          "type": "string",
          "enum": ["oak_planks", "cobblestone", "glass_pane", ...]
        },
        "minItems": 2,
        "maxItems": 5,
        "description": "Block types to use in construction"
      },
      "dimensions": {
        "type": "array",
        "items": {"type": "integer"},
        "minItems": 3,
        "maxItems": 3,
        "description": "[width, height, depth] dimensions"
      }
    },
    "required": ["structure", "blocks"],
    "additionalProperties": false
  }
}
```

```json
{
  "name": "mine_block",
  "description": "Mine specific block types in the world",
  "parameters": {
    "type": "object",
    "properties": {
      "block": {
        "type": "string",
        "enum": ["iron", "diamond", "coal", "gold", "copper", "redstone", "emerald"],
        "description": "Resource type to mine"
      },
      "quantity": {
        "type": "integer",
        "minimum": 1,
        "maximum": 64,
        "default": 8,
        "description": "Number of blocks to mine"
      }
    },
    "required": ["block"],
    "additionalProperties": false
  }
}
```

### 4.3 Schema Registry Pattern

**Centralized Schema Management:**
```java
public class ActionSchemaRegistry {
    private final Map<String, ActionSchema> schemas = new ConcurrentHashMap<>();

    public void register(String actionName, ActionSchema schema) {
        schemas.put(actionName, schema);
    }

    public ActionSchema getSchema(String actionName) {
        return schemas.get(actionName);
    }

    public JsonArray getAllFunctionDeclarations() {
        JsonArray declarations = new JsonArray();
        schemas.values().forEach(schema ->
            declarations.add(schema.toFunctionDeclaration())
        );
        return declarations;
    }

    public ValidationResult validate(String actionName, Map<String, Object> parameters) {
        ActionSchema schema = getSchema(actionName);
        if (schema == null) {
            return ValidationResult.error("Unknown action: " + actionName);
        }
        return schema.validate(parameters);
    }
}
```

---

## 5. Error Handling for Malformed Responses

### 5.1 Progressive Error Recovery

**Three-Tier Strategy:**

1. **Tier 1: Schema Validation**
   - Validate structure before execution
   - Check required parameters
   - Verify types and ranges
   - Return specific error messages

2. **Tier 2: Automatic Repair**
   - Fix common JSON issues (current approach)
   - Inject missing defaults
   - Normalize enum values
   - Attempt parameter inference

3. **Tier 3: LLM Refinement**
   - Feed error back to LLM
   - Request correction with context
   - Limit retry attempts (2-3 max)
   - Cache common repair patterns

### 5.2 Error Response Structure

```java
public class ParseResult {
    private final boolean success;
    private final List<Task> tasks;
    private final List<ParseError> errors;
    private final String rawResponse;
    private final int repairAttempts;

    public static ParseResult success(List<Task> tasks) {
        return new ParseResult(true, tasks, List.of(), null, 0);
    }

    public static ParseResult failure(List<ParseError> errors, String raw) {
        return new ParseResult(false, List.of(), errors, raw, 0);
    }

    public static ParseResult partial(List<Task> tasks, List<ParseError> errors) {
        return new ParseResult(false, tasks, errors, null, 0);
    }
}

public class ParseError {
    private final ErrorType type;
    private final String action;
    private final String parameter;
    private final String message;
    private final String suggestion;

    public enum ErrorType {
        MISSING_REQUIRED,
        INVALID_TYPE,
        VALUE_OUT_OF_RANGE,
        UNKNOWN_ACTION,
        INVALID_ENUM,
        MALFORMED_JSON
    }
}
```

### 5.3 Error Feedback to LLM

**Constructive Error Messages:**
```
Your response had issues. Please fix:

ERROR 1: Unknown action "crafting"
  - Available actions: build, mine, attack, follow, pathfind
  - Did you mean "craft_item"?

ERROR 2: Invalid block type "wood"
  - Must use specific block names from the provided list
  - Examples: oak_log, spruce_planks, cobblestone

ERROR 3: Missing required parameter "quantity" for action "mine"
  - Format: {"action": "mine", "parameters": {"block": "iron", "quantity": 8}}

Please retry with corrections.
```

### 5.4 Circuit Breaker Pattern

**Prevent Retry Loops:**
```java
public class ResponseParserWithRetry {
    private static final int MAX_REPAIR_ATTEMPTS = 3;
    private static final int MAX_LLM_RETRIES = 2;

    private final CircuitBreaker circuitBreaker;
    private final Map<String, Integer> errorCounts = new ConcurrentHashMap<>();

    public ParseResult parseWithRecovery(String response, String originalCommand) {
        // Track error patterns
        String errorKey = extractErrorPattern(response);
        errorCounts.merge(errorKey, 1, Integer::sum);

        // Circuit breaker if same error repeats
        if (errorCounts.getOrDefault(errorKey, 0) > 3) {
            return ParseResult.failure(List.of(
                ParseError.repeatedError(errorKey)
            ), response);
        }

        // Attempt repair
        for (int attempt = 0; attempt < MAX_REPAIR_ATTEMPTS; attempt++) {
            try {
                ParseResult result = parse(response);
                if (result.isSuccess()) {
                    errorCounts.remove(errorKey); // Reset on success
                    return result;
                }

                // Try automatic repair
                String repaired = attemptRepair(response, result.getErrors());
                if (repaired != null) {
                    response = repaired;
                    continue;
                }

                break;
            } catch (Exception e) {
                // Log and continue
            }
        }

        // Final fallback - ask LLM for correction
        if (canRetryLLM()) {
            return requestLLMCorrection(originalCommand, response);
        }

        return ParseResult.failure(List.of(
            ParseError.finalFailure()
        ), response);
    }
}
```

---

## 6. Multi-Step Tool Execution Patterns

### 6.1 Sequential vs Parallel Execution

**Sequential Pattern:**
```
User Command → LLM Plan → [Task 1 → Task 2 → Task 3]
```
- Dependencies between tasks
- Previous results inform next actions
- Order matters

**Parallel Pattern:**
```
User Command → LLM Plan → [Task 1, Task 2, Task 3] (concurrent)
```
- Independent tasks
- No shared state
- Faster completion

### 6.2 Hybrid Orchestration

**DAG-Based Execution:**
```java
public class TaskOrchestrator {
    private final ExecutorService executor;

    public CompletableFuture<ExecutionResult> executePlan(ParsedResponse plan) {
        List<Task> tasks = plan.getTasks();

        // Build dependency graph
        TaskDAG dag = buildDAG(tasks);

        // Execute parallel layers
        CompletableFuture<List<TaskResult>> future = CompletableFuture.completedFuture(List.of());

        for (List<Task> layer : dag.getParallelLayers()) {
            future = future.thenCompose(previousResults ->
                executeParallelLayer(layer, previousResults)
            );
        }

        return future.thenApply(allResults ->
            new ExecutionResult(allResults, plan.getReasoning())
        );
    }

    private CompletableFuture<List<TaskResult>> executeParallelLayer(
        List<Task> layer,
        List<TaskResult> previousResults
    ) {
        List<CompletableFuture<TaskResult>> futures = layer.stream()
            .map(task -> executeTaskAsync(task, previousResults))
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }
}
```

### 6.3 Tool Result Feedback Loops

**Pattern from Gemini/Claude:**
```
1. Execute tool
2. Capture result
3. Feed result back to LLM
4. LLM decides next action based on result
5. Repeat until goal achieved
```

**Implementation:**
```java
public class AdaptiveExecutionLoop {
    private final TaskPlanner planner;
    private final ActionExecutor executor;
    private final SteveMemory memory;

    public CompletableFuture<Void> executeWithFeedback(
        String command,
        ForemanEntity foreman
    ) {
        return CompletableFuture.supplyAsync(() -> {
            ExecutionContext context = new ExecutionContext(foreman, memory);

            for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
                // Plan next actions based on current state
                ParsedResponse plan = planner.planNextStep(
                    command,
                    context.getCurrentState(),
                    context.getPreviousResults()
                );

                if (plan.isComplete()) {
                    break;
                }

                // Execute planned actions
                List<TaskResult> results = executor.executeAll(plan.getTasks(), context);

                // Update context with results
                context.addResults(results);

                // Check if goal achieved
                if (context.isGoalAchieved()) {
                    break;
                }

                // Check for blocking errors
                if (context.hasBlockingErrors()) {
                    // Ask LLM for recovery strategy
                    plan = planner.planRecovery(context.getErrors());
                }
            }

            return null;
        });
    }
}
```

### 6.4 Multi-Agent Coordination

**Collaborative Building Pattern:**
```
1. Decompose structure into sections
2. Agents claim sections (atomic)
3. Execute in parallel
4. Dynamic rebalancing
```

**Current Implementation:**
- `CollaborativeBuildManager` handles spatial partitioning
- `ConcurrentHashMap` for thread-safe claims
- Similar to multi-agent tool calling in Claude/Gemini

---

## 7. Recommendations for Steve AI

### 7.1 Immediate Improvements (Priority 1)

**1. Enhanced ResponseParser**

```java
public class EnhancedResponseParser {
    private final ActionSchemaRegistry schemaRegistry;
    private final LLMClient llmClient; // For corrections

    public ParseResult parseWithValidation(String response) {
        try {
            // Extract JSON
            String jsonString = extractJSON(response);
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

            // Parse reasoning and plan
            String reasoning = getString(json, "reasoning", "");
            String plan = getString(json, "plan", "");

            // Parse tasks with validation
            List<Task> validTasks = new ArrayList<>();
            List<ParseError> errors = new ArrayList<>();

            if (json.has("tasks")) {
                for (JsonElement taskElement : json.getAsJsonArray("tasks")) {
                    JsonObject taskObj = taskElement.getAsJsonObject();

                    // Validate against schema
                    String action = getString(taskObj, "action", "");
                    ValidationResult validation = schemaRegistry.validate(action, taskObj);

                    if (validation.isValid()) {
                        Task task = parseTask(taskObj);
                        validTasks.add(task);
                    } else {
                        errors.add(ParseError.schemaError(action, validation));
                    }
                }
            }

            // Return result
            if (validTasks.isEmpty()) {
                return ParseResult.failure(errors, response);
            } else if (!errors.isEmpty()) {
                return ParseResult.partial(validTasks, errors);
            } else {
                return ParseResult.success(validTasks);
            }

        } catch (Exception e) {
            return ParseResult.failure(
                List.of(ParseError.exception(e)),
                response
            );
        }
    }

    public ParseResult parseOrCorrect(String response, String originalCommand) {
        ParseResult result = parseWithValidation(response);

        if (!result.isSuccess() && canAttemptCorrection()) {
            // Ask LLM to fix errors
            String correctionPrompt = buildCorrectionPrompt(
                originalCommand,
                response,
                result.getErrors()
            );

            String corrected = llmClient.sendRequest(
                getSystemPrompt(),
                correctionPrompt
            );

            return parseWithValidation(corrected);
        }

        return result;
    }
}
```

**2. Action Schema Registry**

```java
public class MinecraftActionRegistry {
    private final ActionSchemaRegistry registry;

    public void registerCoreActions() {
        // Build action
        registry.register("build", ActionSchema.builder()
            .name("build")
            .description("Construct a structure in Minecraft")
            .addParameter("structure", String.class)
                .withEnum("house", "castle", "tower", "barn", "modern")
                .required()
            .addParameter("blocks", String[].class)
                .withEnum(getValidBlockTypes())
                .minItems(2)
                .maxItems(5)
                .required()
            .addParameter("dimensions", Integer[].class)
                .pattern("\\[\\d+,\\s*\\d+,\\s*\\d+\\]")
                .optional()
            .build());

        // Mine action
        registry.register("mine", ActionSchema.builder()
            .name("mine")
            .description("Mine specific block types")
            .addParameter("block", String.class)
                .withEnum("iron", "diamond", "coal", "gold", "copper", "redstone", "emerald")
                .required()
            .addParameter("quantity", Integer.class)
                .minValue(1)
                .maxValue(64)
                .defaultValue(8)
                .optional()
            .build());

        // Attack action
        registry.register("attack", ActionSchema.builder()
            .name("attack")
            .description("Attack hostile mobs")
            .addParameter("target", String.class)
                .withEnum("hostile", "zombie", "skeleton", "creeper", "spider")
                .defaultValue("hostile")
                .required()
            .build());

        // Follow action
        registry.register("follow", ActionSchema.builder()
            .name("follow")
            .description("Follow a player")
            .addParameter("player", String.class)
                .description("Player name to follow")
                .required()
            .build());

        // Pathfind action
        registry.register("pathfind", ActionSchema.builder()
            .name("pathfind")
            .description("Navigate to coordinates")
            .addParameter("x", Integer.class)
                .required()
            .addParameter("y", Integer.class)
                .required()
            .addParameter("z", Integer.class)
                .required()
            .build());
    }
}
```

**3. Update PromptBuilder**

```java
public class EnhancedPromptBuilder {
    private final ActionSchemaRegistry schemaRegistry;

    public String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a Minecraft AI agent. ");
        prompt.append("Respond ONLY with valid JSON matching the provided schema.\n\n");

        prompt.append("AVAILABLE ACTIONS:\n");
        for (ActionSchema schema : schemaRegistry.getAllSchemas()) {
            prompt.append(formatSchema(schema));
        }

        prompt.append("\nRESPONSE FORMAT:\n");
        prompt.append(schemaRegistry.getResponseSchema());

        prompt.append("\n\nRULES:\n");
        prompt.append("- Use exact enum values for block types and structures\n");
        prompt.append("- Provide coordinates as integers\n");
        prompt.append("- Keep reasoning under 15 words\n");

        return prompt.toString();
    }

    private String formatSchema(ActionSchema schema) {
        // Generate human-readable schema description
        return String.format(
            "- %s: %s\n  Parameters: %s\n",
            schema.getName(),
            schema.getDescription(),
            schema.getParameterDescriptions()
        );
    }
}
```

### 7.2 Medium-Term Improvements (Priority 2)

**4. Migrate to Native Function Calling**

```java
public class FunctionCallingLLMClient implements AsyncLLMClient {
    private final ActionSchemaRegistry schemaRegistry;
    private final OpenAIClient openAIClient;

    @Override
    public CompletableFuture<LLMResponse> chatAsync(
        List<ChatMessage> messages,
        LLMOptions options
    ) {
        JsonObject requestBody = buildFunctionCallingRequest(messages);

        return openAIClient.sendAsync(requestBody)
            .thenApply(this::parseFunctionCallResponse);
    }

    private JsonObject buildFunctionCallingRequest(List<ChatMessage> messages) {
        JsonObject body = new JsonObject();
        body.addProperty("model", config.getModel());

        // Add messages
        body.add("messages", serializeMessages(messages));

        // Add function declarations
        body.add("tools", schemaRegistry.getAllFunctionDeclarations());

        // Set tool choice
        body.addProperty("tool_choice", "required"); // Always call functions

        return body;
    }

    private ParsedResponse parseFunctionCallResponse(JsonObject response) {
        JsonArray choices = response.getAsJsonArray("choices");
        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        JsonObject message = firstChoice.getAsJsonObject("message");

        // Extract tool calls
        JsonArray toolCalls = message.getAsJsonArray("tool_calls");

        List<Task> tasks = new ArrayList<>();
        for (JsonElement toolCall : toolCalls) {
            JsonObject tc = toolCall.getAsJsonObject();
            JsonObject function = tc.getAsJsonObject("function");

            String functionName = function.get("name").getAsString();
            String arguments = function.get("arguments").getAsString();

            JsonObject argsJson = JsonParser.parseString(arguments).getAsJsonObject();
            Task task = parseTaskFromFunctionCall(functionName, argsJson);
            tasks.add(task);
        }

        return new ParsedResponse("", "", tasks);
    }
}
```

**5. Add Structured Output Mode**

```java
public class StructuredOutputLLMClient {
    private LLMResponse sendWithStructuredOutput(
        String systemPrompt,
        String userPrompt,
        JsonObject responseSchema
    ) {
        JsonObject body = new JsonObject();
        body.addProperty("model", "gpt-4o-2024-08-06"); // Supports structured output

        // Add messages
        body.add("messages", buildMessages(systemPrompt, userPrompt));

        // Set response format with schema
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_schema");

        JsonObject jsonSchema = new JsonObject();
        jsonSchema.addProperty("name", "minecraft_action_response");
        jsonSchema.addProperty("strict", true);
        jsonSchema.add("schema", responseSchema);

        responseFormat.add("json_schema", jsonSchema);
        body.add("response_format", responseFormat);

        return sendRequest(body);
    }
}
```

### 7.3 Advanced Improvements (Priority 3)

**6. Implement Multi-Step Orchestration**

```java
public class MultiStepTaskOrchestrator {
    private final TaskPlanner planner;
    private final ActionExecutor executor;
    private final SteveMemory memory;

    public CompletableFuture<Void> executeComplexCommand(
        String command,
        ForemanEntity foreman
    ) {
        return CompletableFuture.supplyAsync(() -> {
            ExecutionContext context = new ExecutionContext(foreman, memory);

            // Phase 1: Initial planning
            ParsedResponse initialPlan = planner.planTasks(command, foreman);

            // Phase 2: Execute with feedback loop
            while (!context.isGoalAchieved() && context.getIteration() < MAX_ITERATIONS) {
                // Execute current tasks
                List<TaskResult> results = executor.executeAll(
                    context.getCurrentTasks(),
                    context
                );

                // Update context
                context.addResults(results);

                // Check for errors
                if (context.hasErrors()) {
                    // Request recovery plan
                    ParsedResponse recoveryPlan = planner.planRecovery(
                        context.getErrors(),
                        context
                    );
                    context.mergeTasks(recoveryPlan.getTasks());
                    continue;
                }

                // Check if goal achieved
                if (context.isGoalAchieved()) {
                    break;
                }

                // Plan next step based on results
                ParsedResponse nextPlan = planner.planNextStep(
                    command,
                    context.getPreviousResults(),
                    context
                );

                context.mergeTasks(nextPlan.getTasks());
            }

            return null;
        });
    }
}
```

**7. Add Parallel Task Execution**

```java
public class ParallelTaskExecutor {
    private final ForkJoinPool executor;

    public List<TaskResult> executeParallel(List<Task> tasks, ExecutionContext context) {
        // Build dependency graph
        TaskDependencyGraph graph = buildDependencyGraph(tasks);

        // Execute parallel layers
        List<TaskResult> allResults = new ArrayList<>();

        for (List<Task> layer : graph.getParallelLayers()) {
            // Execute layer in parallel
            List<TaskResult> layerResults = layer.parallelStream()
                .map(task -> executeTask(task, context, allResults))
                .toList();

            allResults.addAll(layerResults);

            // Check for failures
            if (layerResults.stream().anyMatch(r -> !r.isSuccess())) {
                break;
            }
        }

        return allResults;
    }
}
```

---

## 8. Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
- [ ] Create `ActionSchemaRegistry` class
- [ ] Define schemas for all core actions
- [ ] Update `PromptBuilder` with schema descriptions
- [ ] Add basic validation to `ResponseParser`

### Phase 2: Error Handling (Week 3-4)
- [ ] Implement `ParseResult` with error details
- [ ] Add automatic repair for common issues
- [ ] Implement LLM correction feedback loop
- [ ] Add circuit breaker pattern

### Phase 3: Function Calling (Week 5-6)
- [ ] Migrate to OpenAI function calling API
- [ ] Add structured output mode
- [ ] Update all LLM clients (Groq, Gemini)
- [ ] Test with all providers

### Phase 4: Orchestration (Week 7-8)
- [ ] Implement multi-step execution loop
- [ ] Add parallel task execution
- [ ] Implement task dependency graph
- [ ] Add tool result feedback loops

### Phase 5: Optimization (Week 9-10)
- [ ] Add caching for repeated queries
- [ ] Optimize prompt size
- [ ] Add performance monitoring
- [ ] Implement A/B testing for strategies

---

## 9. Testing Strategy

### 9.1 Unit Tests

```java
class ResponseParserTest {
    @Test
    void testValidResponse() {
        String response = """
            {"reasoning": "Test", "plan": "Test plan", "tasks": [
                {"action": "mine", "parameters": {"block": "iron", "quantity": 8}}
            ]}
            """;

        ParseResult result = parser.parseWithValidation(response);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getTasks().size());
    }

    @Test
    void testInvalidAction() {
        String response = """
            {"reasoning": "Test", "plan": "Test plan", "tasks": [
                {"action": "invalid_action", "parameters": {}}
            ]}
            """;

        ParseResult result = parser.parseWithValidation(response);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().get(0).getMessage().contains("Unknown action"));
    }

    @Test
    void testPartialSuccess() {
        String response = """
            {"reasoning": "Test", "plan": "Test plan", "tasks": [
                {"action": "mine", "parameters": {"block": "iron", "quantity": 8}},
                {"action": "build", "parameters": {"structure": "house"}}
            ]}
            """;

        ParseResult result = parser.parseWithValidation(response);
        assertFalse(result.isSuccess()); // Has errors
        assertEquals(1, result.getTasks().size()); // One valid task
        assertEquals(1, result.getErrors().size()); // One error
    }
}
```

### 9.2 Integration Tests

```java
class LLMIntegrationTest {
    @Test
    void testEndToEndCommand() {
        String command = "Build a house";

        // Plan
        ParsedResponse plan = planner.planTasks(command, foreman);
        assertNotNull(plan);
        assertFalse(plan.getTasks().isEmpty());

        // Execute
        List<TaskResult> results = executor.executeAll(plan.getTasks(), context);

        // Verify
        assertTrue(results.stream().allMatch(TaskResult::isSuccess));
    }

    @Test
    void testErrorRecovery() {
        String command = "Build with invalid blocks";

        // Initial plan (will have errors)
        ParsedResponse plan = planner.planTasks(command, foreman);

        // Parse with validation
        ParseResult result = parser.parseOrCorrection(
            plan.getRawResponse(),
            command
        );

        // Should attempt correction
        assertTrue(result.getRepairAttempts() > 0);
    }
}
```

### 9.3 Performance Tests

```java
class PerformanceTest {
    @Test
    void testParallelExecution() {
        List<Task> tasks = generateTasks(100);

        long startTime = System.currentTimeMillis();
        executor.executeParallel(tasks, context);
        long parallelTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        executor.executeSequential(tasks, context);
        long sequentialTime = System.currentTimeMillis() - startTime;

        assertTrue(parallelTime < sequentialTime / 2); // At least 2x faster
    }
}
```

---

## 10. Key Metrics to Track

### 10.1 Quality Metrics
- **Parse Success Rate**: % of responses that parse successfully
- **Validation Pass Rate**: % of parsed responses that pass schema validation
- **Correction Success Rate**: % of corrected responses that become valid
- **Execution Success Rate**: % of tasks that execute without errors

### 10.2 Performance Metrics
- **Average Parse Time**: Time to parse and validate response
- **Correction Attempts**: Average number of correction attempts
- **LLM Round-Trips**: Average number of LLM calls per command
- **End-to-End Latency**: Time from command to completion

### 10.3 Error Metrics
- **Error Type Distribution**: Most common validation errors
- **Recovery Success Rate**: % of errors that are recovered
- **Circuit Breaker Trips**: How often circuit breaker activates

---

## 11. Migration Checklist

### Pre-Migration
- [ ] Back up current ResponseParser implementation
- [ ] Create feature branch
- [ ] Set up comprehensive test coverage
- [ ] Document current behavior

### Migration Steps
- [ ] Implement ActionSchemaRegistry
- [ ] Add schemas for all actions
- [ ] Update ResponseParser with validation
- [ ] Add error handling infrastructure
- [ ] Update PromptBuilder
- [ ] Test with all LLM providers
- [ ] Run integration tests
- [ ] Performance testing
- [ ] Documentation updates

### Post-Migration
- [ ] Monitor metrics for 1 week
- [ ] Gather user feedback
- [ ] Fine-tune prompts
- [ ] Optimize based on metrics
- [ ] Rollback plan if needed

---

## 12. Conclusion

The research shows that modern LLM tool calling has evolved significantly beyond simple JSON parsing. Key improvements for Steve AI include:

1. **Schema-Driven Validation**: Move from ad-hoc parsing to formal JSON Schema definitions
2. **Progressive Error Handling**: Implement tiered recovery from automatic repair to LLM correction
3. **Native Function Calling**: Leverage provider APIs for structured output
4. **Multi-Step Orchestration**: Implement feedback loops and adaptive execution
5. **Parallel Execution**: Execute independent tasks concurrently

These improvements will make Steve AI more reliable, efficient, and maintainable while reducing the burden on prompt engineering.

---

## Sources

### OpenAI Function Calling
- [Microsoft Azure - Function Calling Guide](https://learn.microsoft.com/en-us/azure/ai-foundry/openai/how-to/function-calling)
- [OpenAI Function Calling 2.0 Guide](https://devpress.csdn.net/v1/article/detail/145210500)
- [Function Calling Best Practices Checklist](https://blog.csdn.net/gitblog_00579/article/details/151949815)

### Claude Tool Use
- [Claude Advanced Tool Use](https://m.blog.csdn.net/comedyking/article/details/156086530)
- [Claude Tool Use Case Studies](https://juejin.cn/post/7508648111486304271)
- [Claude MCP/Skills/Agents/Plugins](https://www.cnblogs.com/treasury-manager/p/19166145)

### Gemini Function Calling
- [Firebase Gemini Function Calling](https://firebase.google.cn/docs/ai-logic/function-calling)
- [Gemini Function Calling Guide](https://m.blog.csdn.net/sinat_37574187/article/details/149472668)

### Structured Output
- [ThoughtWorks - Structured Output from LLMs](https://www.thoughtworks.cn/radar/techniques/structured-output-from-llms)
- [LangChain Structured Output Guide](https://m.csdn.net/qq_45583713/article/details/157330723)
- [LangChain4j Structured Output](https://m.blog.csdn.net/linwu_2006_2006/article/details/155737214)

### Additional Resources
- [vLLM Structured Output Implementation](https://blog.csdn.net/lxcxjxhx/article/details/157612683)
- [DeepSeek Tool Calling Guide](https://juejin.cn/post/7609151073308475443)
- [Large Model Tool/Function Calling Principles](https://juejin.cn/post/7542705382137905198)

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Next Review:** 2026-03-27
