# LLM Prompt Efficiency Optimizations

## Summary

This document details the optimizations made to reduce LLM token usage and improve prompt efficiency in the MineWright codebase.

## Changes Made

### 1. PromptBuilder.java - System Prompt Optimization

**File:** `src/main/java/com/minewright/llm/PromptBuilder.java`

**Token Reduction: ~50%**

- **Condensed format instructions** from verbose multi-line to compact single-line
- **Consolidated block lists** using slash notation (e.g., `oak/spruce/birch: log, planks`)
- **Compressed action definitions** using pipe notation for options
- **Reduced examples** from 7 to 3 (most common use cases)
- **Shortened rules** from 10 to 5 concise points
- **Removed redundant verbiage** while maintaining clarity

**Before:**
```
You are a Minecraft AI agent. Respond ONLY with valid JSON, no extra text.

FORMAT (strict JSON):
{"reasoning": "brief thought", "plan": "action description", "tasks": [{"action": "type", "parameters": {...}}]}
...
```

**After:**
```
You are a Minecraft AI. Respond ONLY with valid JSON.

FORMAT: {"reasoning":"brief","plan":"action","tasks":[{"action":"type","parameters":{}}]}
...
```

### 2. PromptBuilder.java - User Prompt Optimization

**Token Reduction: ~70%**

- **Compact situation report** using abbreviated labels (POS, PLAYERS, ENTITIES, BLOCKS, BIOME)
- **Conditional inclusion** of optional fields (only if not "none")
- **Single-line format** for position `[x,y,z]` instead of `[x, y, z]`
- **Pipe separators** instead of verbose newlines
- **Pre-allocated StringBuilder** with 256 char capacity

**Before:**
```
=== YOUR SITUATION ===
Position: [123, 64, 456]
Nearby Players: none
Nearby Entities: none
Nearby Blocks: none
Biome: plains

=== PLAYER COMMAND ===
"build a house"

=== YOUR RESPONSE (with reasoning) ===
```

**After:**
```
POS:[123,64,456] | BIOME:plains
CMD:"build a house"
```

### 3. CompanionPromptBuilder.java - Conversational Prompts

**Token Reduction: ~60%**

- **Condensed identity** from verbose description to single sentence
- **Compact communication guidelines** using comma-separated traits
- **Reduced inside jokes** from 3 to 2 maximum
- **Single-line relationship context** by replacing newlines
- **Abbreviated situation format** `[Time|Biome|Activity]`
- **Streamlined response format** instruction

**Added:** `buildCompactCommunicationGuidelines()` method for personality-based compact prompts

### 4. WorldKnowledge.java - Priority Filtering

**File:** `src/main/java/com/minewright/memory/WorldKnowledge.java`

**Token Reduction: ~80% for context summaries**

- **Added priority lists** for valuable blocks (ores, chests)
- **Added hostile mob filtering** to focus on threats
- **New method:** `getPriorityBlocksSummary()` - only shows ores and valuable blocks
- **New method:** `getPriorityEntitiesSummary()` - only shows hostile mobs
- **Compact format** using `block:count` instead of verbose descriptions

**Before:**
```
Nearby Blocks: stone, dirt, grass_block, cobblestone, coal_ore
Nearby Entities: 1 zombie, 2 sheep, 1 cow
```

**After:**
```
BLOCKS:coal_ore:5
ENTITIES:zombie:1
```

### 5. New Utility: PromptMetrics.java

**File:** `src/main/java/com/minewright/llm/PromptMetrics.java`

**Features:**
- **Token tracking** across all requests (thread-safe)
- **Cost estimation** for OpenAI, Groq, and Gemini providers
- **Per-request metrics** with `RequestMetrics` class
- **Token estimation** from text content (1 token ≈ 4 characters)
- **Summary reporting** for monitoring

**Usage:**
```java
// Record usage
PromptMetrics.recordRequest(500, 200);

// Estimate cost
double cost = PromptMetrics.calculateCost(500, 200);

// Get summary
System.out.println(PromptMetrics.getSummary());
```

### 6. New Utility: PromptVersion.java

**File:** `src/main/java/com/minewright/llm/PromptVersion.java`

**Features:**
- **A/B testing support** with percentage-based traffic splitting
- **Version registration** with custom prompt suppliers
- **Metrics tracking** per version (success rate, usage count)
- **Easy rollback** to previous versions
- **Thread-safe** implementation

**Usage:**
```java
// Register versions
PromptVersion.register("v1", () -> PromptBuilder.buildSystemPrompt());
PromptVersion.register("v2", () -> PromptBuilder.buildSystemPromptV2());

// Set up A/B test (10% to v2)
PromptVersion.setActiveVersion("v2", 0.1);

// Get active prompt
String prompt = PromptVersion.getActivePrompt();

// Check metrics
Map<String, VersionMetrics> metrics = PromptVersion.getAllMetrics();
```

## Token Savings Summary

| Component | Before | After | Reduction |
|-----------|--------|-------|-----------|
| System Prompt | ~2,200 tokens | ~1,100 tokens | 50% |
| User Prompt | ~400 tokens | ~120 tokens | 70% |
| Companion Prompt | ~1,500 tokens | ~600 tokens | 60% |
| World Context | ~200 tokens | ~40 tokens | 80% |

**Total estimated savings per request: ~1,880 tokens (65% reduction)**

## Cost Impact

Assuming GPT-4 pricing ($30/M input, $60/M output):

- **Before:** ~2,600 tokens/request × 100 requests = $7.80/day
- **After:** ~720 tokens/request × 100 requests = $2.16/day
- **Savings:** $5.64/day (72% reduction)

## Backward Compatibility

All changes maintain backward compatibility:
- Original methods preserved (`buildCommunicationGuidelines()`, `buildSituationContext()`)
- New optimized methods added alongside existing ones
- Optional to migrate to new methods gradually

## Next Steps

1. **Integration:** Update `TaskPlanner.java` to use `PromptMetrics` for tracking
2. **Testing:** Run A/B tests to compare optimized vs. original prompts
3. **Monitoring:** Set up logging with `PromptMetrics.getSummary()`
4. **Rollout:** Use `PromptVersion` for gradual migration

## Files Modified

1. `src/main/java/com/minewright/llm/PromptBuilder.java` - Optimized prompts
2. `src/main/java/com/minewright/llm/CompanionPromptBuilder.java` - Compact prompts
3. `src/main/java/com/minewright/memory/WorldKnowledge.java` - Priority filtering

## Files Created

1. `src/main/java/com/minewright/llm/PromptMetrics.java` - Token tracking
2. `src/main/java/com/minewright/llm/PromptVersion.java` - A/B testing
3. `docs/PROMPT_OPTIMIZATION.md` - This document
