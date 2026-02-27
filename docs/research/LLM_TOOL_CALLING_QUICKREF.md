# LLM Tool Calling - Quick Reference

**Purpose:** Fast reference for implementing tool calling improvements in Steve AI
**Full Document:** See `LLM_TOOL_CALLING.md` for detailed research

---

## Priority Actions

### 1. Schema Registry (DO THIS FIRST)
```java
// C:\Users\casey\steve\src\main\java\com\minewright\llm\ActionSchemaRegistry.java
public class ActionSchemaRegistry {
    void register(String actionName, ActionSchema schema);
    ValidationResult validate(String actionName, JsonObject params);
    JsonArray getAllFunctionDeclarations(); // For OpenAI/Gemini
}
```

### 2. Enhanced ResponseParser
```java
// C:\Users\casey\steve\src\main\java\com\minewright\llm\ResponseParser.java
public class EnhancedResponseParser {
    ParseResult parseWithValidation(String response);
    ParseResult parseOrCorrect(String response, String originalCommand);
}

// New return type
class ParseResult {
    boolean isSuccess();
    List<Task> getTasks();
    List<ParseError> getErrors();
}
```

### 3. Migrate to Function Calling
```java
// Update OpenAIClient to use tools parameter
JsonObject requestBody = new JsonObject();
requestBody.add("tools", schemaRegistry.getAllFunctionDeclarations());
requestBody.addProperty("tool_choice", "required");
```

---

## Schema Examples

### Build Action Schema
```json
{
  "name": "build",
  "description": "Construct a structure in Minecraft",
  "parameters": {
    "type": "object",
    "properties": {
      "structure": {
        "type": "string",
        "enum": ["house", "castle", "tower", "barn", "modern"]
      },
      "blocks": {
        "type": "array",
        "items": {"type": "string"},
        "minItems": 2,
        "maxItems": 5
      },
      "dimensions": {
        "type": "array",
        "items": {"type": "integer"},
        "minItems": 3,
        "maxItems": 3
      }
    },
    "required": ["structure", "blocks"],
    "additionalProperties": false
  }
}
```

### Mine Action Schema
```json
{
  "name": "mine",
  "description": "Mine specific block types in the world",
  "parameters": {
    "type": "object",
    "properties": {
      "block": {
        "type": "string",
        "enum": ["iron", "diamond", "coal", "gold", "copper", "redstone", "emerald"]
      },
      "quantity": {
        "type": "integer",
        "minimum": 1,
        "maximum": 64,
        "default": 8
      }
    },
    "required": ["block"],
    "additionalProperties": false
  }
}
```

---

## Error Handling Strategy

### Three-Tier Recovery
1. **Validate** - Check schema before execution
2. **Repair** - Fix common issues automatically
3. **Correct** - Ask LLM to fix errors (max 2 retries)

### Error Response Format
```
ERROR 1: Unknown action "crafting"
  - Available: build, mine, attack, follow, pathfind
  - Did you mean "craft_item"?

ERROR 2: Invalid block type "wood"
  - Use specific names: oak_log, spruce_planks, etc.

Please retry with corrections.
```

---

## Multi-Step Execution

### Sequential Pattern
```
Command → Plan → [Task 1 → Task 2 → Task 3]
```

### Parallel Pattern
```
Command → Plan → [Task 1, Task 2, Task 3] (concurrent)
```

### Feedback Loop
```
Execute Tool → Capture Result → Feed to LLM → Plan Next Action
```

---

## Key Metrics

| Metric | Target | Current |
|--------|--------|---------|
| Parse Success Rate | >95% | Unknown |
| Validation Pass Rate | >90% | Unknown |
| Correction Success Rate | >70% | N/A |
| LLM Round-Trips | <3 per command | Unknown |

---

## Implementation Checklist

- [ ] Create ActionSchemaRegistry class
- [ ] Define schemas for all actions
- [ ] Update ResponseParser with validation
- [ ] Add ParseResult error details
- [ ] Implement automatic repair
- [ ] Add LLM correction feedback
- [ ] Update PromptBuilder with schemas
- [ ] Migrate to function calling API
- [ ] Add parallel execution
- [ ] Implement feedback loops
- [ ] Add monitoring/metrics
- [ ] Document and test

---

## Quick Commands

```bash
# Run tests
./gradlew test

# Run with debug logging
./gradlew runClient --debug

# View logs
tail -f logs/latest.log
```

---

## File Locations

| Component | Path |
|-----------|------|
| ResponseParser | `C:\Users\casey\steve\src\main\java\com\minewright\llm\ResponseParser.java` |
| PromptBuilder | `C:\Users\casey\steve\src\main\java\com\minewright\llm\PromptBuilder.java` |
| OpenAIClient | `C:\Users\casey\steve\src\main\java\com\minewright\llm\OpenAIClient.java` |
| Task Class | `C:\Users\casey\steve\src\main\java\com\minewright\action\Task.java` |
| Actions | `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\*.java` |

---

**Version:** 1.0
**Updated:** 2026-02-27
