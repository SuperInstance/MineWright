# LLM Tool Use and Function Calling Patterns

**Research Date:** 2026-03-02
**Version:** 1.0
**Status:** Comprehensive Research & Best Practices
**Target:** Enhance MineWright AI's FunctionCallingTool system

---

## Executive Summary

This document provides comprehensive research on LLM tool use and function calling patterns across major providers (OpenAI, Anthropic, Google) and frameworks (LangChain, AutoGPT, MCP). It covers tool definition schemas, selection strategies, error handling patterns, and framework-specific implementations with direct application to MineWright AI's existing `FunctionCallingTool` infrastructure.

**Key Findings:**
- **Function Calling** has evolved from provider-specific implementations to standardized patterns
- **MCP (Model Context Protocol)** is emerging as the "USB-C for AI" - universal tool interface standard
- **LangGraph** represents the 2025 shift from "prompt engineering" to "flow engineering"
- **Structured Output** with JSON Schema provides highest reliability (vs. prompt-only approaches)
- MineWright AI's existing `FunctionCallingTool` interface aligns well with modern patterns

---

## Table of Contents

1. [Tool Definition Patterns](#1-tool-definition-patterns)
2. [Tool Selection Strategies](#2-tool-selection-strategies)
3. [Error Handling Patterns](#3-error-handling-patterns)
4. [Framework Patterns](#4-framework-patterns)
5. [Application to MineWright](#5-application-to-minewright)
6. [Implementation Roadmap](#6-implementation-roadmap)
7. [Sources](#7-sources)

---

## 1. Tool Definition Patterns

### 1.1 JSON Schema for Parameters

Tool definitions use JSON Schema to describe parameters, enabling LLMs to understand and generate valid function calls.

#### Basic Structure

```json
{
  "name": "mine_block",
  "description": "Mine blocks of a specific type within a search radius",
  "parameters": {
    "type": "object",
    "properties": {
      "block_type": {
        "type": "string",
        "description": "The type of block to mine (e.g., 'stone', 'iron_ore', 'diamond_ore')"
      },
      "quantity": {
        "type": "integer",
        "description": "Number of blocks to mine",
        "minimum": 1,
        "maximum": 64,
        "default": 8
      },
      "radius": {
        "type": "integer",
        "description": "Search radius in blocks",
        "minimum": 1,
        "maximum": 32,
        "default": 5
      }
    },
    "required": ["block_type"],
    "additionalProperties": false
  }
}
```

#### Schema Property Types

| Type | Description | Example Use |
|------|-------------|-------------|
| `string` | Text values | Block names, entity types |
| `integer` | Whole numbers | Quantities, coordinates, counts |
| `number` | Decimal numbers | Durability, percentages |
| `boolean` | True/false | Toggle flags, state switches |
| `array` | Ordered lists | Item lists, coordinate sequences |
| `object` | Nested structures | Complex configurations |

#### Advanced Schema Features

**Enum Constraints:**
```json
{
  "block_type": {
    "type": "string",
    "enum": ["oak_log", "spruce_log", "birch_log", "stone", "cobblestone"],
    "description": "Valid block types"
  }
}
```

**Array Constraints:**
```json
{
  "blocks": {
    "type": "array",
    "items": {
      "type": "string",
      "enum": ["oak_planks", "cobblestone", "glass_pane"]
    },
    "minItems": 2,
    "maxItems": 5,
    "description": "Block types for building"
  }
}
```

**Nested Objects:**
```json
{
  "location": {
    "type": "object",
    "properties": {
      "x": {"type": "integer"},
      "y": {"type": "integer"},
      "z": {"type": "integer"},
      "dimension": {
        "type": "string",
        "enum": ["overworld", "nether", "end"]
      }
    },
    "required": ["x", "y", "z"]
  }
}
```

### 1.2 Tool Descriptions

Research shows that rich tool descriptions significantly improve LLM selection accuracy (20-30% improvement in benchmarks).

#### Description Best Practices

**DO:**
- Start with a clear one-line summary
- Provide concrete examples with expected outputs
- Explicitly state constraints and limitations
- Include prerequisites when relevant
- Use enum values to prevent invalid states

**DON'T:**
- Use vague or generic descriptions
- Overload with too much information
- Assume the model knows context
- Use inconsistent terminology

#### Example: Effective Tool Description

```json
{
  "name": "craft_item",
  "description": "Craft a specific Minecraft item using available resources.\n\nExamples:\n- craft_item(item=\"iron_pickaxe\", count=1) -> Crafts one iron pickaxe\n- craft_item(item=\"oak_planks\", count=16) -> Converts 1 log to 4 planks\n\nConstraints:\n- Maximum count: 64\n- Must have required materials in inventory\n- Some items require specific tool tiers\n\nPrerequisites:\n- Must be near a crafting table for complex recipes\n- Basic recipes (sticks, planks) can be crafted anywhere"
}
```

### 1.3 Required vs Optional Parameters

**Required Parameters:**
- Essential for tool execution
- Must be present in function call
- Listed in `"required"` array
- LLM will always prompt user if missing

**Optional Parameters:**
- Have sensible defaults
- Listed in `"properties"` but not `"required"`
- LLM may omit or use default values
- Use `"default"` property to specify

**Example:**
```json
{
  "parameters": {
    "type": "object",
    "properties": {
      "target": {
        "type": "string",
        "description": "Entity or block to target"
      },
      "action": {
        "type": "string",
        "enum": ["attack", "defend", "follow"],
        "default": "attack",
        "description": "Combat action to perform"
      },
      "distance": {
        "type": "integer",
        "default": 5,
        "minimum": 1,
        "maximum": 32,
        "description": "Maximum engagement distance"
      }
    },
    "required": ["target"]
  }
}
```

### 1.4 Nested Objects and Complex Parameters

#### Hierarchical Parameter Design

```json
{
  "name": "build_structure",
  "parameters": {
    "type": "object",
    "properties": {
      "structure_type": {
        "type": "string",
        "enum": ["house", "castle", "tower", "farm"]
      },
      "blueprint": {
        "type": "object",
        "description": "Structure blueprint configuration",
        "properties": {
          "dimensions": {
            "type": "object",
            "properties": {
              "width": {"type": "integer", "minimum": 1},
              "height": {"type": "integer", "minimum": 1},
              "depth": {"type": "integer", "minimum": 1}
            },
            "required": ["width", "height", "depth"]
          },
          "materials": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "block": {"type": "string"},
                "quantity": {"type": "integer"}
              }
            }
          }
        }
      }
    }
  }
}
```

---

## 2. Tool Selection Strategies

### 2.1 Single Tool Selection

**Use Case:** Simple, unambiguous commands where one tool suffices.

**Pattern:**
```
User Command → LLM Analysis → Single Tool Call → Execute → Result
```

**Example:**
```json
{
  "user_command": "Mine 10 iron ore",
  "tool_calls": [
    {
      "id": "call_abc123",
      "type": "function",
      "function": {
        "name": "mine_block",
        "arguments": "{\"block_type\": \"iron_ore\", \"quantity\": 10}"
      }
    }
  ]
}
```

### 2.2 Parallel Tool Calls

**Use Case:** Multiple independent operations that can execute simultaneously.

**Pattern:**
```
User Command → LLM Analysis → Multiple Tool Calls → Parallel Execution → Aggregate Results
```

**Example:**
```json
{
  "user_command": "Gather wood and stone while finding water",
  "tool_calls": [
    {
      "id": "call_abc123",
      "type": "function",
      "function": {
        "name": "mine_block",
        "arguments": "{\"block_type\": \"oak_log\", \"quantity\": 5}"
      }
    },
    {
      "id": "call_def456",
      "type": "function",
      "function": {
        "name": "mine_block",
        "arguments": "{\"block_type\": \"cobblestone\", \"quantity\": 10}"
      }
    },
    {
      "id": "call_ghi789",
      "type": "function",
      "function": {
        "name": "find_nearest",
        "arguments": "{\"target_type\": \"water\"}"
      }
    }
  ]
}
```

**Key Benefits:**
- Reduces latency (parallel execution)
- Maintains task independence
- Better resource utilization

### 2.3 Tool Chaining

**Use Case:** Sequential operations where output of one tool informs the next.

**Pattern:**
```
Command → Tool 1 → Result → Tool 2 → Result → Tool 3 → Final Result
```

**Example:**
```javascript
// Chain: Find blocks → Move to location → Mine blocks
Step 1: find_nearest("diamond_ore")
  → Result: {location: {x: 100, y: 12, z: -50}}

Step 2: pathfind({x: 100, y: 12, z: -50})
  → Result: {success: true, distance: 45}

Step 3: mine_block({block_type: "diamond_ore", quantity: 5})
  → Result: {mined: 5, total: 5}
```

**Implementation Pattern:**
```java
public class ToolChainExecutor {
    public CompletableFuture<ToolResult> executeChain(
        List<ToolCall> chain,
        ExecutionContext context
    ) {
        CompletableFuture<ToolResult> future = CompletableFuture.completedFuture(null);

        for (ToolCall call : chain) {
            future = future.thenCompose(previousResult -> {
                // Update context with previous result
                if (previousResult != null) {
                    context.addResult(previousResult);
                }

                // Execute next tool
                return executeTool(call, context);
            });
        }

        return future;
    }
}
```

### 2.4 Conditional Tool Use

**Use Case:** Tool selection based on runtime conditions or previous results.

**Pattern:**
```
Execute Tool → Check Result → Branch to Different Tool
```

**Example:**
```javascript
// Mining with tool durability check
mine_block({block: "iron_ore"})
  → Result: {success: false, reason: "pickaxe_broken"}
  → Conditional: craft_item({item: "iron_pickaxe"})
  → Retry: mine_block({block: "iron_ore"})
```

**Implementation:**
```java
public class ConditionalToolExecutor {
    public ToolResult executeWithRecovery(
        String toolName,
        JsonObject params,
        ExecutionContext context
    ) {
        ToolResult result = executeTool(toolName, params);

        // Check if recovery needed
        if (result.hasRecoveryCondition()) {
            String recoveryTool = result.getRecoveryTool();
            JsonObject recoveryParams = result.getRecoveryParameters();

            // Execute recovery
            ToolResult recoveryResult = executeTool(recoveryTool, recoveryParams);

            if (recoveryResult.isSuccess()) {
                // Retry original tool
                return executeTool(toolName, params);
            }
        }

        return result;
    }
}
```

---

## 3. Error Handling Patterns

### 3.1 Invalid Parameters

**Detection:** Schema validation before execution

**Pattern:**
```
Parse Tool Call → Validate Schema → Return Error or Execute
```

**Error Response Format:**
```json
{
  "success": false,
  "error_type": "INVALID_PARAMETERS",
  "errors": [
    {
      "parameter": "quantity",
      "issue": "VALUE_OUT_OF_RANGE",
      "message": "Value 100 exceeds maximum of 64",
      "suggestion": "Use a value between 1 and 64"
    },
    {
      "parameter": "block_type",
      "issue": "INVALID_ENUM",
      "message": "Value 'wood' is not a valid block type",
      "valid_options": ["oak_log", "spruce_log", "birch_log"],
      "suggestion": "Use 'oak_log' instead of 'wood'"
    }
  ]
}
```

**Validation Implementation:**
```java
public class SchemaValidator {
    public ValidationResult validate(String toolName, JsonObject parameters) {
        ActionSchema schema = registry.getSchema(toolName);
        List<ValidationError> errors = new ArrayList<>();

        // Check required parameters
        for (String required : schema.getRequiredParameters()) {
            if (!parameters.has(required)) {
                errors.add(ValidationError.missing(required));
            }
        }

        // Validate types
        for (Map.Entry<String, JsonElement> entry : parameters.entrySet()) {
            String param = entry.getKey();
            JsonElement value = entry.getValue();

            ParameterSchema paramSchema = schema.getParameter(param);
            if (!paramSchema.validateType(value)) {
                errors.add(ValidationError.typeMismatch(
                    param,
                    paramSchema.getType(),
                    value
                ));
            }
        }

        return errors.isEmpty()
            ? ValidationResult.success()
            : ValidationResult.failure(errors);
    }
}
```

### 3.2 Tool Execution Failures

**Categories:**
1. **Transient Failures** - Retryable (network, temporary locks)
2. **Permanent Failures** - Not retryable (invalid state, permissions)
3. **Partial Failures** - Some objectives met

**Error Response with Recovery:**
```json
{
  "success": false,
  "error_type": "EXECUTION_FAILED",
  "tool": "mine_block",
  "error_message": "No pickaxe available",
  "retryable": false,
  "recovery_hint": {
    "suggested_action": "craft_item",
    "parameters": {"item": "wooden_pickaxe", "count": 1},
    "reasoning": "You need a pickaxe to mine stone blocks"
  },
  "alternatives": [
    {
      "tool": "find_nearest",
      "parameters": {"target": "pickaxe"},
      "description": "Search for nearby pickaxe"
    }
  ]
}
```

**MineWright Integration:**
```java
// Using existing ToolResult and ToolExecutionException
public ToolResult execute(JsonObject parameters) {
    try {
        // Check prerequisites
        if (!hasRequiredTool()) {
            return ToolResult.failure("No pickaxe equipped")
                .withRecoveryHint("Equip a pickaxe using equip_item or craft one using craft_item");
        }

        // Execute logic
        int mined = performMining(parameters);
        return ToolResult.success("Mined " + mined + " blocks");

    } catch (ToolExecutionException e) {
        if (e.isRetryable()) {
            // Schedule retry with backoff
            return scheduleRetry(parameters);
        }
        return ToolResult.failure(e.getMessage(), e);
    }
}
```

### 3.3 Retry Strategies

#### Exponential Backoff

```java
public class RetryExecutor {
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000;

    public ToolResult executeWithRetry(
        String toolName,
        JsonObject params
    ) {
        int attempt = 0;
        long delay = BASE_DELAY_MS;

        while (attempt < MAX_RETRIES) {
            try {
                return executeTool(toolName, params);
            } catch (ToolExecutionException e) {
                if (!e.isRetryable() || attempt == MAX_RETRIES - 1) {
                    return ToolResult.failure(e.getMessage(), e);
                }

                attempt++;
                try {
                    Thread.sleep(delay);
                    delay *= 2; // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return ToolResult.failure("Interrupted during retry", ie);
                }
            }
        }

        return ToolResult.failure("Max retries exceeded");
    }
}
```

#### Circuit Breaker Pattern

```java
public class CircuitBreakerExecutor {
    private final Map<String, CircuitState> circuitStates = new ConcurrentHashMap<>();

    public ToolResult execute(String toolName, JsonObject params) {
        CircuitState state = circuitStates.get(toolName);

        // Check if circuit is open
        if (state != null && state.isOpen()) {
            if (state.shouldAttemptReset()) {
                state.attemptReset();
            } else {
                return ToolResult.failure("Circuit breaker open for: " + toolName)
                    .withRecoveryHint("Tool temporarily unavailable due to repeated failures");
            }
        }

        try {
            ToolResult result = executeTool(toolName, params);

            // Reset circuit on success
            if (state != null) {
                state.recordSuccess();
            }

            return result;

        } catch (ToolExecutionException e) {
            // Record failure
            circuitStates.computeIfAbsent(toolName, k -> new CircuitState())
                .recordFailure();

            return ToolResult.failure(e.getMessage(), e);
        }
    }
}
```

### 3.4 Fallback Behaviors

**Graceful Degradation:**
```java
public class FallbackExecutor {
    public ToolResult executeWithFallbacks(
        String primaryTool,
        JsonObject params,
        List<Fallback> fallbacks
    ) {
        // Try primary tool
        try {
            ToolResult result = executeTool(primaryTool, params);
            if (result.isSuccess()) {
                return result;
            }
        } catch (ToolExecutionException e) {
            // Log and continue to fallbacks
            LOGGER.warn("Primary tool failed: {}", e.getMessage());
        }

        // Try fallbacks in order
        for (Fallback fallback : fallbacks) {
            try {
                ToolResult result = executeTool(fallback.toolName, fallback.params);
                if (result.isSuccess()) {
                    return result.withFallbackNote(fallback.description);
                }
            } catch (ToolExecutionException e) {
                LOGGER.debug("Fallback {} failed: {}",
                    fallback.toolName, e.getMessage());
            }
        }

        return ToolResult.failure("All tools failed")
            .withRecoveryHint("Try alternative approach or manual intervention");
    }
}
```

---

## 4. Framework Patterns

### 4.1 OpenAI Function Calling

**Current Version:** Function Calling 2.0 (2024+)

**Request Format:**
```json
{
  "model": "gpt-4o",
  "messages": [
    {
      "role": "system",
      "content": "You are a Minecraft AI assistant..."
    },
    {
      "role": "user",
      "content": "Build a house"
    }
  ],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "build_structure",
        "description": "Build a structure in Minecraft",
        "parameters": {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "type": "object",
          "properties": {
            "structure_type": {
              "type": "string",
              "enum": ["house", "castle", "tower"]
            },
            "materials": {
              "type": "array",
              "items": {"type": "string"}
            }
          },
          "required": ["structure_type"]
        }
      }
    }
  ],
  "tool_choice": "auto"
}
```

**Strict Mode (New in 2024):**
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

**Response Format:**
```json
{
  "choices": [
    {
      "message": {
        "role": "assistant",
        "tool_calls": [
          {
            "id": "call_abc123",
            "type": "function",
            "function": {
              "name": "build_structure",
              "arguments": "{\"structure_type\":\"house\",\"materials\":[\"oak_planks\",\"cobblestone\"]}"
            }
          }
        ]
      }
    }
  ]
}
```

### 4.2 Anthropic Tool Use

**Key Differences from OpenAI:**
- Uses `tools` parameter (same as OpenAI)
- Content blocks for complex tool interactions
- Built-in tool search mechanism for large tool ecosystems

**Request Format:**
```json
{
  "model": "claude-3-5-sonnet-20241022",
  "max_tokens": 1024,
  "tools": [
    {
      "name": "pathfind",
      "description": "Navigate to coordinates",
      "input_schema": {
        "type": "object",
        "properties": {
          "x": {"type": "integer"},
          "y": {"type": "integer"},
          "z": {"type": "integer"}
        },
        "required": ["x", "y", "z"]
      }
    }
  ],
  "messages": [
    {
      "role": "user",
      "content": "Go to coordinates 100, 64, -200"
    }
  ]
}
```

**Response with Tool Use:**
```json
{
  "id": "msg_abc123",
  "content": [
    {
      "type": "text",
      "text": "I'll navigate to those coordinates."
    },
    {
      "type": "tool_use",
      "id": "toolu_abc123",
      "name": "pathfind",
      "input": {
        "x": 100,
        "y": 64,
        "z": -200
      }
    }
  ]
}
```

### 4.3 LangChain Tools

**Tool Interface:**
```python
from langchain.tools import BaseTool
from typing import Optional, Type
from pydantic import BaseModel, Field

class MineBlockInput(BaseModel):
    """Input schema for mine_block tool."""
    block_type: str = Field(..., description="Type of block to mine")
    quantity: int = Field(default=8, ge=1, le=64, description="Number of blocks")
    radius: int = Field(default=5, ge=1, le=32, description="Search radius")

class MineBlockTool(BaseTool):
    name = "mine_block"
    description = "Mine blocks of a specific type within a radius"
    args_schema: Type[BaseModel] = MineBlockInput

    def _run(self, block_type: str, quantity: int = 8, radius: int = 5) -> str:
        """Execute the mining operation."""
        # Mining logic here
        return f"Mined {quantity} {block_type} blocks"

    async def _arun(self, block_type: str, quantity: int = 8, radius: int = 5) -> str:
        """Async execution."""
        return self._run(block_type, quantity, radius)
```

**LangGraph Pattern (2025 Trend):**
```python
from langgraph.graph import StateGraph
from typing import TypedDict

class AgentState(TypedDict):
    messages: list
    tools_results: list
    next_action: str

def tool_router(state: AgentState) -> str:
    """Route to appropriate tool based on state."""
    last_message = state["messages"][-1]
    if last_message.tool_calls:
        return "tools"
    return "end"

def tool_executor(state: AgentState):
    """Execute tool calls."""
    results = []
    for tool_call in last_message.tool_calls:
        result = tools[tool_call.name].invoke(tool_call.args)
        results.append(result)
    return {"tools_results": results}

# Build graph
workflow = StateGraph(AgentState)
workflow.add_node("agent", agent_node)
workflow.add_node("tools", tool_executor)
workflow.add_conditional_edges("agent", tool_router)
workflow.set_entry_point("agent")
workflow.set_finish_point("end")
```

### 4.4 AutoGPT Patterns

**Task Decomposition:**
```python
class AutoGPTAgent:
    def __init__(self, tools: List[Tool]):
        self.tools = {tool.name: tool for tool in tools}
        self.memory = []

    def execute_command(self, command: str) -> str:
        # Step 1: Plan
        plan = self.plan_subtasks(command)

        # Step 2: Execute each subtask
        results = []
        for subtask in plan:
            # Select tool
            tool_name = self.select_tool(subtask)
            tool = self.tools[tool_name]

            # Execute
            result = tool.execute(subtask.parameters)
            results.append(result)

            # Update memory
            self.memory.append({
                "subtask": subtask,
                "result": result
            })

        # Step 3: Synthesize
        return self.synthesize_results(results)
```

### 4.5 MCP (Model Context Protocol)

**Architecture:**
```
AI Client (MCP Client) ←→ HTTP/SSE ←→ MCP Server (Tool Provider)
```

**Tool Manifest:**
```json
{
  "protocol_version": "2024-11-05",
  "server": {
    "name": "minecraft-tools",
    "version": "1.0.0"
  },
  "tools": [
    {
      "name": "minecraft.pathfind",
      "description": "Calculate path to coordinates",
      "inputSchema": {
        "type": "object",
        "properties": {
          "x": {"type": "integer"},
          "y": {"type": "integer"},
          "z": {"type": "integer"}
        },
        "required": ["x", "y", "z"]
      }
    }
  ],
  "resources": [
    {
      "uri": "minecraft://world/blocks",
      "name": "World Blocks",
      "description": "Query blocks in the world"
    }
  ]
}
```

**Tool Invocation:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "minecraft.pathfind",
    "arguments": {
      "x": 100,
      "y": 64,
      "z": -200
    }
  }
}
```

---

## 5. Application to MineWright

### 5.1 Current Implementation Assessment

**Existing Infrastructure:**
- `FunctionCallingTool` interface (235 lines, well-documented)
- `ToolMetadata` annotation for declarative tool definitions
- `ToolParam` annotation for parameter schemas
- `ToolResult` class for structured outcomes
- `ToolExecutionException` for error handling

**Strengths:**
- Aligns with modern function calling patterns
- Supports JSON Schema parameter definitions
- Provides rich error context and recovery hints
- Supports parallel execution flag
- Includes cost estimation for model routing

**Gaps:**
- No schema registry for validation
- No tool chaining/orchestration
- No retry logic
- No MCP integration
- Limited error recovery automation

### 5.2 Enhancing FunctionCallingTool

#### Add Schema Registry

```java
// src/main/java/com/minewright/action/ToolSchemaRegistry.java
public class ToolSchemaRegistry {
    private final Map<String, FunctionCallingTool> tools = new ConcurrentHashMap<>();
    private final Map<String, JsonObject> schemas = new ConcurrentHashMap<>();

    public void register(FunctionCallingTool tool) {
        String name = tool.getName();
        tools.put(name, tool);

        JsonObject schema = new JsonObject();
        schema.addProperty("name", name);
        schema.addProperty("description", tool.getDescription());
        schema.add("parameters", tool.getParameterSchema());

        schemas.put(name, schema);
    }

    public JsonArray getAllToolDeclarations() {
        JsonArray declarations = new JsonArray();
        schemas.values().forEach(declarations::add);
        return declarations;
    }

    public ValidationResult validateCall(
        String toolName,
        JsonObject parameters
    ) {
        FunctionCallingTool tool = tools.get(toolName);
        if (tool == null) {
            return ValidationResult.error("Unknown tool: " + toolName);
        }

        // Validate against schema
        return validateAgainstSchema(
            tool.getParameterSchema(),
            parameters
        );
    }
}
```

#### Add Tool Chain Executor

```java
// src/main/java/com/minewright/action/ToolChainExecutor.java
public class ToolChainExecutor {
    private final ToolSchemaRegistry registry;
    private final ActionExecutor actionExecutor;

    public CompletableFuture<ToolResult> executeChain(
        List<ToolCall> chain,
        ForemanEntity foreman
    ) {
        CompletableFuture<ToolResult> future = CompletableFuture.completedFuture(null);
        ExecutionContext context = new ExecutionContext(foreman);

        for (ToolCall call : chain) {
            future = future.thenCompose(previousResult -> {
                // Add previous result to context
                if (previousResult != null) {
                    context.addResult(previousResult);
                }

                // Validate call
                ValidationResult validation = registry.validateCall(
                    call.getToolName(),
                    call.getParameters()
                );

                if (!validation.isValid()) {
                    return CompletableFuture.completedFuture(
                        ToolResult.failure("Invalid parameters: " + validation.getErrors())
                    );
                }

                // Execute tool
                FunctionCallingTool tool = registry.getTool(call.getToolName());
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        return tool.execute(call.getParameters());
                    } catch (ToolExecutionException e) {
                        return ToolResult.failure(e.getMessage(), e);
                    }
                });
            });
        }

        return future;
    }
}
```

### 5.3 Minecraft-Specific Tools

#### Enhanced Tool Definitions

```java
@ToolMetadata(
    name = "pathfind_to",
    description = "Navigate to specific coordinates or entity.\n\n" +
                  "Examples:\n" +
                  "- pathfind_to(target={x: 100, y: 64, z: -200}) -> Navigate to coordinates\n" +
                  "- pathfind_to(target={entity: \"player\"}) -> Follow nearest player\n" +
                  "- pathfind_to(target={block: \"chest\"}) -> Go to nearest chest\n\n" +
                  "Constraints:\n" +
                  "- Maximum distance: 1000 blocks\n" +
                  "- Avoids dangerous blocks (lava, cactus)\n" +
                  "- Uses existing pathfinding infrastructure",
    category = "movement",
    estimatedTickCost = 50,
    parameters = {
        @ToolParam(
            name = "target",
            type = "object",
            description = "Target destination (coordinates, entity, or block type)"
        ),
        @ToolParam(
            name = "timeout",
            type = "integer",
            description = "Maximum ticks to attempt pathfinding",
            required = false,
            defaultValue = "600",
            minValue = "100",
            maxValue = "3600"
        )
    }
)
public class PathfindToTool implements FunctionCallingTool {
    @Override
    public ToolResult execute(JsonObject parameters) throws ToolExecutionException {
        JsonObject target = parameters.getAsJsonObject("target");

        if (target.has("x") && target.has("y") && target.has("z")) {
            // Coordinate-based pathfinding
            int x = target.get("x").getAsInt();
            int y = target.get("y").getAsInt();
            int z = target.get("z").getAsInt();

            return executePathfind(x, y, z);
        } else if (target.has("entity")) {
            // Entity-based pathfinding
            String entity = target.get("entity").getAsString();
            return executePathfindToEntity(entity);
        } else if (target.has("block")) {
            // Block-based pathfinding
            String block = target.get("block").getAsString();
            return executePathfindToBlock(block);
        }

        return ToolResult.failure("Invalid target specification")
            .withRecoveryHint("Target must include coordinates (x, y, z), entity name, or block type");
    }

    // ... implementation methods
}
```

#### Resource Gathering Tool

```java
@ToolMetadata(
    name = "gather_resource",
    description = "Gather specific resources from the environment.\n\n" +
                  "Examples:\n" +
                  "- gather_resource(resource=\"iron_ore\", quantity=10) -> Mine 10 iron ore\n" +
                  "- gather_resource(resource=\"oak_log\", quantity=5, method=\"chop\") -> Chop trees\n" +
                  "- gather_resource(resource=\"wheat\", quantity=20, method=\"farm\") -> Harvest crops\n\n" +
                  "Constraints:\n" +
                  "- Maximum quantity: 64\n" +
                  "- Requires appropriate tools\n" +
                  "- Auto-detects nearest resources",
    category = "mining",
    estimatedTickCost = 30,
    parameters = {
        @ToolParam(
            name = "resource",
            type = "string",
            description = "Resource type to gather",
            enumValues = {"coal_ore", "iron_ore", "gold_ore", "diamond_ore",
                         "oak_log", "spruce_log", "birch_log", "wheat", "carrot"},
            required = true
        ),
        @ToolParam(
            name = "quantity",
            type = "integer",
            description = "Amount to gather",
            minValue = "1",
            maxValue = "64",
            defaultValue = "8",
            required = false
        ),
        @ToolParam(
            name = "method",
            type = "string",
            description = "Gathering method",
            enumValues = {"auto", "mine", "chop", "farm", "dig"},
            defaultValue = "auto",
            required = false
        ),
        @ToolParam(
            name = "radius",
            type = "integer",
            description = "Search radius",
            minValue = "5",
            maxValue = "64",
            defaultValue = "32",
            required = false
        )
    }
)
public class GatherResourceTool implements FunctionCallingTool {
    @Override
    public ToolResult execute(JsonObject parameters) throws ToolExecutionException {
        String resource = parameters.get("resource").getAsString();
        int quantity = parameters.has("quantity")
            ? parameters.get("quantity").getAsInt()
            : 8;
        String method = parameters.has("method")
            ? parameters.get("method").getAsString()
            : "auto";
        int radius = parameters.has("radius")
            ? parameters.get("radius").getAsInt()
            : 32;

        // Check prerequisites
        if (!hasRequiredTool(resource, method)) {
            String neededTool = getRequiredTool(resource, method);
            return ToolResult.failure("Missing required tool: " + neededTool)
                .withRecoveryHint("Craft or equip " + neededTool + " before gathering " + resource);
        }

        // Find resources
        List<BlockPos> resourceLocations = findResources(resource, radius);
        if (resourceLocations.isEmpty()) {
            return ToolResult.failure("No " + resource + " found within " + radius + " blocks")
                .withRecoveryHint("Expand search radius or move to different biome");
        }

        // Gather resources
        int gathered = 0;
        for (BlockPos pos : resourceLocations) {
            if (gathered >= quantity) break;

            ToolResult result = gatherAt(pos, resource);
            if (result.isSuccess()) {
                gathered++;
            }
        }

        if (gathered < quantity) {
            return ToolResult.partial(gathered, quantity,
                "Only found " + gathered + " of " + quantity + " " + resource);
        }

        return ToolResult.success("Gathered " + gathered + " " + resource);
    }

    // ... implementation methods
}
```

### 5.4 Tool Result Formatting

#### Structured JSON Output

```java
public class ToolResultFormatter {
    public JsonObject formatForLLM(ToolResult result) {
        JsonObject json = new JsonObject();

        // Status
        json.addProperty("status", result.getStatus().name().toLowerCase());

        // Message
        json.addProperty("message", result.getMessage());

        // Data (if any)
        if (result.getData() != null) {
            json.add("data", result.getData());
        }

        // Recovery hint (for failures)
        if (result.getRecoveryHint() != null) {
            JsonObject recovery = new JsonObject();
            recovery.addProperty("hint", result.getRecoveryHint());

            // Suggest alternative tools
            if (result.isFailure()) {
                JsonArray alternatives = suggestAlternatives(result);
                recovery.add("alternative_tools", alternatives);
            }

            json.add("recovery", recovery);
        }

        // Progress (for partial results)
        if (result.isPartial()) {
            JsonObject progress = new JsonObject();
            progress.addProperty("completed", result.getCompletedSteps());
            progress.addProperty("total", result.getTotalSteps());
            progress.addProperty("percentage", result.getCompletionPercentage());
            json.add("progress", progress);
        }

        // Error details (if applicable)
        if (result.getError() != null) {
            JsonObject error = new JsonObject();
            error.addProperty("type", result.getError().getClass().getSimpleName());
            error.addProperty("message", result.getError().getMessage());
            json.add("error", error);
        }

        return json;
    }

    private JsonArray suggestAlternatives(ToolResult result) {
        JsonArray alternatives = new JsonArray();

        // Analyze failure to suggest alternatives
        String message = result.getMessage().toLowerCase();

        if (message.contains("no pickaxe")) {
            alternatives.add("craft_item");
            alternatives.add("find_nearest");
            alternatives.add("equip_item");
        } else if (message.contains("not found")) {
            alternatives.add("pathfind_to");
            alternatives.add("explore");
        } else if (message.contains("inventory full")) {
            alternatives.add("deposit_item");
            alternatives.add("drop_item");
        }

        return alternatives;
    }
}
```

---

## 6. Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Tasks:**
- [ ] Create `ToolSchemaRegistry` class
- [ ] Implement schema validation
- [ ] Add unit tests for validation
- [ ] Document existing `FunctionCallingTool` usage

**Deliverables:**
- Working schema registry
- Validation tests
- Documentation updates

### Phase 2: Error Handling (Week 3-4)

**Tasks:**
- [ ] Implement retry logic with exponential backoff
- [ ] Add circuit breaker pattern
- [ ] Create fallback executor
- [ ] Add error recovery tests

**Deliverables:**
- Resilient tool execution
- Error recovery system
- Comprehensive error tests

### Phase 3: Orchestration (Week 5-6)

**Tasks:**
- [ ] Create `ToolChainExecutor`
- [ ] Implement parallel tool execution
- [ ] Add conditional tool use
- [ ] Create execution context for state passing

**Deliverables:**
- Tool chaining system
- Parallel execution support
- Orchestration tests

### Phase 4: Minecraft Tools (Week 7-8)

**Tasks:**
- [ ] Define schemas for all core actions
- [ ] Create Minecraft-specific tools
- [ ] Add tool result formatting
- [ ] Integrate with existing `ActionExecutor`

**Deliverables:**
- Complete tool definitions
- Tool result formatter
- Integration tests

### Phase 5: MCP Integration (Week 9-10)

**Tasks:**
- [ ] Implement MCP client
- [ ] Create tool manifest generator
- [ ] Add MCP server for external tool access
- [ ] Test with MCP-compatible clients

**Deliverables:**
- MCP client implementation
- Tool manifest system
- MCP integration tests

---

## 7. Sources

### Web Research Sources

1. [AI智能体的两种扩展哲学MCP与Agent Skill](https://juejin.cn/post/7588028859540324378) - Comparison of MCP and Agent Skill approaches (2025-12-25)

2. [Function Calling and Tool Use: Turning LLMs into Action-Taking Agents](https://dev.to/qvfagundes/function-calling-and-tool-use-turning-llms-into-action-taking-agents-30ca) - Comprehensive function calling guide (2025-12-03)

3. [LangGraph工作流与智能体核心模式总结](https://blog.csdn.net/W3508559248/article/details/153787431) - LangGraph workflow patterns (2025-10-23)

4. [MCP vs Function Calling：谁才是AI应用开发的未来？](https://juejin.im/post/7499317287716241417) - MCP vs Function Calling comparison (2025-04-30)

5. [大模型应用进阶指南（二）打造具备自主工具使用能力的智能推理链](https://developer.aliyun.com/article/1705518) - Advanced LLM application patterns

6. [AI Agent常见策略范式](https://blog.csdn.net/imaginehero/article/details/149395719) - Common AI Agent strategy patterns

7. [OpenAI的Function calling 和 LangChain的Search Agent](https://devpress.csdn.net/v1/article/detail/131776061) - OpenAI and LangChain comparison

8. [2025-2026年LLM智能体研究报告：原理演进](https://blog.csdn.net/qq_43743777/article/details/157129356) - LLM Agent evolution 2025-2026

9. [大模型Function Calling实战指南](https://m.blog.csdn.net/trb701012/article/details/156872219) - Function Calling practical guide

10. [AI MCP与Agent智能体最新热点整理](https://m.blog.csdn.net/universsky2015/article/details/155912783) - MCP and Agent latest trends (2025)

11. [Model Context Protocol（MCP）详解](https://k.sina.cn/article_1887312183_m707e193703301dxee.html) - MCP detailed explanation

12. [MCP、Function Calling、A2A三大核心技术详解](https://blog.csdn.net/androiddddd/article/details/153735477) - MCP, Function Calling, A2A comparison

### Internal Sources

13. `C:\Users\casey\steve\src\main\java\com\minewright\action\FunctionCallingTool.java` - Existing tool calling interface

14. `C:\Users\casey\steve\src\main\java\com\minewright\action\ToolMetadata.java` - Tool metadata annotation

15. `C:\Users\casey\steve\src\main\java\com\minewright\action\ToolParam.java` - Parameter schema annotation

16. `C:\Users\casey\steve\src\main\java\com\minewright\action\ToolResult.java` - Tool result class

17. `C:\Users\casey\steve\src\main\java\com\minewright\action\ToolExecutionException.java` - Tool exception class

18. `C:\Users\casey\steve\docs\research\LLM_TOOL_CALLING.md` - Previous tool calling research

19. `C:\Users\casey\steve\docs\research\LLM_TOOL_CALLING_QUICKREF.md` - Tool calling quick reference

---

**Document Version:** 1.0
**Last Updated:** 2026-03-02
**Next Review:** 2026-04-02
**Maintained By:** MineWright AI Research Team

---

## Appendix: Quick Reference

### Tool Definition Checklist

- [ ] Name uses snake_case
- [ ] Description includes examples
- [ ] Description states constraints
- [ ] All parameters have types
- [ ] Required parameters listed
- [ ] Optional parameters have defaults
- [ ] Enums use exact values
- [ ] Ranges have min/max
- [ ] Nested objects properly structured
- [ ] `additionalProperties: false` set

### Error Response Checklist

- [ ] Clear error type
- [ ] Specific error message
- [ ] Parameter that caused error
- [ ] Valid values/suggestions
- [ ] Recovery hint provided
- [ ] Alternative actions suggested
- [ ] Retry flag set correctly
- [ ] Error context included

### Testing Checklist

- [ ] Valid parameters succeed
- [ ] Invalid parameters rejected with helpful error
- [ ] Missing required parameters caught
- [ ] Out-of-range values rejected
- [ ] Invalid enum values rejected
- [ ] Retry logic works
- [ ] Circuit breaker triggers
- [ ] Fallback tools work
- [ ] Parallel execution safe
- [ ] Tool chains execute correctly
