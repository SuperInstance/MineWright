# Cascade Router System

## Overview

The Cascade Router is an intelligent LLM selection system that optimizes cost and latency by routing requests to appropriate model tiers based on task complexity. Inspired by cascade routing patterns from SmartCRDT and Aequor, it enables dramatic cost savings (70-90%) while maintaining quality for complex tasks.

### Key Benefits

- **Cost Optimization**: 70-90% reduction in LLM API costs through intelligent tier selection
- **Low Latency**: Fast responses for simple tasks using lightweight models
- **Quality Preservation**: Complex tasks still use powerful models (GPT-4, Claude)
- **Cache Optimization**: Pre-computed responses for common patterns
- **Transparent Routing**: Clear decision making with detailed logging

## Architecture

```
User Command
      |
      v
ComplexityAnalyzer
      |
      +---> TRIVIAL ----> CACHE (Instant)
      |
      +---> SIMPLE ----> FAST (Groq llama-3.1-8b) ~$0.00001/1K tokens
      |
      +---> MODERATE ----> BALANCED (Groq llama-3.3-70b/gpt-3.5) ~$0.00020/1K tokens
      |
      +---> COMPLEX ----> SMART (gpt-4/claude) ~$0.01000/1K tokens
      |
      +---> NOVEL ----> SMART + Maximum Context
```

## Task Complexity Levels

### TRIVIAL

**Definition**: Single action, well-known patterns

**Examples**:
- "mine 10 stone"
- "follow me"
- "stop"
- "wait here"
- "status"

**Routing**: CACHE or LOCAL

**Expected Cache Hit Rate**: 60-80%

**Cost**: $0.00 (cached)

### SIMPLE

**Definition**: 1-2 actions, straightforward execution

**Examples**:
- "mine 10 stone and craft a furnace"
- "go to x,y,z and place torch"
- "attack zombie"
- "walk to 100,64,200"

**Routing**: FAST (Groq llama-3.1-8b-instant)

**Expected Cache Hit Rate**: 30-50%

**Cost**: ~$0.00001/1K tokens

**Latency**: ~100-300ms

### MODERATE

**Definition**: 3-5 actions, some reasoning required

**Examples**:
- "build a small house"
- "gather resources for crafting"
- "create a storage room"
- "setup a farm"

**Routing**: BALANCED (Groq llama-3.3-70b or gpt-3.5-turbo)

**Expected Cache Hit Rate**: 10-20%

**Cost**: ~$0.00020/1K tokens

**Latency**: ~300-800ms

### COMPLEX

**Definition**: Multiple actions, coordination between agents

**Examples**:
- "coordinate crew to build a castle"
- "setup automated farm system"
- "organize mining operation"
- "manage multiple agents"

**Routing**: SMART (gpt-4, claude) with full reasoning

**Expected Cache Hit Rate**: 5-10%

**Cost**: ~$0.01000/1K tokens

**Latency**: ~1000-3000ms

### NOVEL

**Definition**: Never seen before, maximum reasoning needed

**Examples**:
- Creative problem solving
- Complex strategic planning
- Multi-stage operations
- Novel combinations

**Routing**: SMART with maximum context, low temperature

**Expected Cache Hit Rate**: 0%

**Cost**: ~$0.01000/1K tokens

**Latency**: ~2000-4000ms

## Routing Logic

### ComplexityAnalyzer

Analyzes commands using multiple strategies:

```java
public class ComplexityAnalyzer {
    public TaskComplexity analyze(String command, ForemanEntity foreman, WorldKnowledge worldKnowledge) {
        // 1. Pattern matching (highest confidence)
        if (matchesPattern(command, TRIVIAL_PATTERNS)) return TRIVIAL;
        if (matchesPattern(command, SIMPLE_PATTERNS)) return SIMPLE;
        if (matchesPattern(command, MODERATE_PATTERNS)) return MODERATE;
        if (matchesPattern(command, COMPLEX_PATTERNS)) return COMPLEX;

        // 2. Length and structure analysis
        TaskComplexity lengthResult = analyzeByLength(command);
        if (lengthResult != null) return lengthResult;

        // 3. Keyword analysis
        TaskComplexity keywordResult = analyzeByKeywords(command);
        if (keywordResult != null) return keywordResult;

        // 4. Context complexity
        TaskComplexity contextResult = analyzeByContext(command, foreman, worldKnowledge);
        if (contextResult != null) return contextResult;

        // 5. Historical frequency (novelty detection)
        return adjustForHistory(command, initialComplexity);
    }
}
```

### Pattern Examples

**TRIVIAL Patterns**:
```java
Pattern.compile("^\\s*stop\\s*$")
Pattern.compile("^\\s*follow\\s+me\\s*$")
Pattern.compile("^\\s*status\\s*$")
```

**SIMPLE Patterns**:
```java
Pattern.compile("^mine\\s+\\d+\\s+\\w+")
Pattern.compile("^craft\\s+\\d+\\s+\\w+")
Pattern.compile("^go\\s+to\\s+")
```

**COMPLEX Patterns**:
```java
Pattern.compile("^coordinate\\s+")
Pattern.compile("^work\\s+together\\s+to")
Pattern.compile("^all\\s+agents\\s+")
```

### Historical Adjustment

```java
private TaskComplexity adjustForHistory(String command, TaskComplexity initialComplexity) {
    int executionCount = commandHistory.getOrDefault(signature, 0);

    if (executionCount == 0) {
        // Never seen -> NOVEL (unless trivial)
        return initialComplexity == TRIVIAL ? TRIVIAL : NOVEL;
    }

    if (executionCount >= 5) {
        // Frequently seen -> may downgrade for cache
        if (initialComplexity == SIMPLE) return TRIVIAL;
        if (initialComplexity == MODERATE && executionCount >= 10) return SIMPLE;
    }

    return initialComplexity;
}
```

## Cost Optimization

### Expected Savings Breakdown

**Scenario**: 1000 commands per day

| Complexity | Distribution | Without Cascade | With Cascade | Savings |
|------------|--------------|-----------------|--------------|---------|
| TRIVIAL | 30% (300) | $3.00 | $0.00 | $3.00 |
| SIMPLE | 40% (400) | $8.00 | $0.40 | $7.60 |
| MODERATE | 20% (200) | $4.00 | $0.80 | $3.20 |
| COMPLEX | 8% (80) | $2.40 | $1.60 | $0.80 |
| NOVEL | 2% (20) | $0.40 | $0.40 | $0.00 |
| **Total** | **100%** | **$17.80** | **$3.20** | **$14.60 (82%)** |

### Token Usage Comparison

**Typical Task**: "build a small house"

| Model | Input Tokens | Output Tokens | Cost | Latency |
|-------|--------------|---------------|------|---------|
| GPT-4 | 500 | 300 | $0.008 | 2000ms |
| Claude 3 Opus | 500 | 300 | $0.009 | 2500ms |
| Groq 70B | 500 | 300 | $0.00016 | 400ms |
| Groq 8B | 500 | 300 | $0.000008 | 200ms |
| **Cascade (FAST)** | 500 | 300 | **$0.000008** | **200ms** |

## LLM Tiers

### CACHE

**Model**: None (pre-computed)

**Cost**: $0.00/1K tokens

**Latency**: ~1ms

**Use Case**: Previously seen commands

**Hit Rate**: 40-60% overall

### LOCAL

**Model**: Future: Ollama (llama-3.1-8b)

**Cost**: $0.00 (local compute)

**Latency**: ~50-200ms

**Use Case**: Simple tasks with GPU available

**Status**: Not yet implemented

### FAST

**Model**: Groq llama-3.1-8b-instant

**Cost**: ~$0.00001/1K tokens

**Latency**: ~100-300ms

**Use Case**: SIMPLE tasks

**Provider**: Groq

### BALANCED

**Model**: Groq llama-3.3-70b or OpenAI gpt-3.5-turbo

**Cost**: ~$0.00020/1K tokens

**Latency**: ~300-800ms

**Use Case**: MODERATE tasks

**Provider**: Groq/OpenAI

### SMART

**Model**: OpenAI gpt-4 or Anthropic claude-3-opus

**Cost**: ~$0.01000/1K tokens

**Latency**: ~1000-3000ms

**Use Case**: COMPLEX and NOVEL tasks

**Provider**: OpenAI/Anthropic

## Configuration

### Config File: `config/steve-common.toml`

```toml
[llm.cascade]
# Enable cascade routing
enabled = true

# Default tier for unrecognized tasks
defaultTier = "BALANCED"

# Enable caching for TRIVIAL tasks
enableCache = true

# Cache TTL (time-to-live)
cacheTTL = "1h"

# Maximum cache size
maxCacheEntries = 1000

# Complexity thresholds
[llm.cascade.thresholds]
# Maximum word count for TRIVIAL
trivialMaxWords = 3

# Maximum word count for SIMPLE
simpleMaxWords = 10

# Maximum word count for MODERATE
moderateMaxWords = 25

# Minimum execution count to downgrade from NOVEL
novelThreshold = 5

# Tier-specific settings
[llm.cascade.tiers]
# FAST tier model
fastModel = "llama-3.1-8b-instant"
fastProvider = "groq"

# BALANCED tier model
balancedModel = "llama-3.3-70b"
balancedProvider = "groq"

# SMART tier model
smartModel = "gpt-4"
smartProvider = "openai"
```

### Java Configuration

```java
public class CascadeConfig {
    private boolean enabled = true;
    private LLMTier defaultTier = LLMTier.BALANCED;
    private boolean enableCache = true;
    private Duration cacheTTL = Duration.ofHours(1);
    private int maxCacheEntries = 1000;

    // Complexity thresholds
    private int trivialMaxWords = 3;
    private int simpleMaxWords = 10;
    private int moderateMaxWords = 25;
    private int novelThreshold = 5;
}
```

## Usage Example

### Basic Routing

```java
// Create analyzer
ComplexityAnalyzer analyzer = new ComplexityAnalyzer();

// Analyze command
TaskComplexity complexity = analyzer.analyze(
    "build a small house",
    foreman,
    worldKnowledge
);

// Select tier based on complexity
LLMTier tier = selectTierForComplexity(complexity);

// Route to appropriate LLM
LLMResponse response = llmRouter.route(tier, prompt);
```

### Custom Routing Logic

```java
public class CustomRouter {
    public LLMTier selectTier(Task task, ForemanEntity foreman) {
        // Check cache first
        if (cache.hasResponse(task)) {
            return LLMTier.CACHE;
        }

        // Analyze complexity
        TaskComplexity complexity = analyzer.analyze(
            task.getCommand(),
            foreman,
            worldKnowledge
        );

        // Apply custom rules
        if (task.requiresCreativeThinking()) {
            return LLMTier.SMART; // Always use smart for creative tasks
        }

        if (complexity == TaskComplexity.COMPLEX && foreman.getBudgetRemaining() < 0.10) {
            return LLMTier.BALANCED; // Downgrade if low budget
        }

        // Standard routing
        return complexityToTier(complexity);
    }
}
```

## Monitoring and Metrics

### Key Metrics

```java
public class CascadeMetrics {
    // Request counts by tier
    private Map<LLMTier, AtomicLong> requestsByTier;

    // Cache hit/miss rates
    private double cacheHitRate;

    // Average latency by tier
    private Map<LLMTier, Double> averageLatency;

    // Cost tracking
    private double totalCost;
    private double costWithoutCascade;

    // Complexity distribution
    private Map<TaskComplexity, Integer> complexityDistribution;
}
```

### Logging

```java
MineWrightMod.LOGGER.info("Cascade routing: command='{}' complexity={} tier={} cost=${}",
    command,
    complexity,
    tier,
    tier.estimateCost(inputTokens + outputTokens)
);
```

## Best Practices

1. **Start Conservative**: Begin with higher tiers and optimize down
2. **Monitor Quality**: Track success rates by tier
3. **Cache Aggressively**: TRIVIAL tasks should have 60%+ hit rate
4. **Set Budget Limits**: Prevent runaway costs on NOVEL tasks
5. **A/B Test**: Compare cascade vs. always-smart for quality
6. **Update Patterns**: Regularly refine pattern matching based on usage

## Troubleshooting

### Too Many NOVEL Classifications

**Symptom**: High cost, many tasks routed to SMART tier

**Solutions**:
1. Increase pattern specificity
2. Lower `novelThreshold` for faster downgrade
3. Add more TRIVIAL/SIMPLE patterns
4. Improve historical tracking

### Poor Cache Hit Rate

**Symptom**: TRIVIAL tasks still hitting LLM

**Solutions**:
1. Increase `cacheTTL`
2. Add more TRIVIAL patterns
3. Check cache key generation
4. Verify cache is being checked before routing

### Quality Issues on FAST Tier

**Symptom**: SIMPLE tasks producing poor results

**Solutions**:
1. Upgrade SIMPLE tasks to BALANCED tier
2. Improve prompt engineering for fast models
3. Add fallback to SMART on retry
4. Adjust complexity thresholds

## Future Enhancements

### Planned Features

1. **LOCAL Tier**: Ollama integration for zero-cost inference
2. **Adaptive Thresholds**: ML-based complexity prediction
3. **Performance Tracking**: Automatic tier adjustment based on success rates
4. **Budget Awareness**: Dynamic tier selection based on remaining budget
5. **Multi-Model Fallback**: Automatic retry with higher tier on failure

### Research Directions

1. **Semantic Caching**: Cache based on meaning, not exact match
2. **Ensemble Routing**: Use multiple models and combine results
3. **User Feedback**: Learn from user corrections
4. **Cost Prediction**: Pre-execution cost estimation

## References

- **Inspired By**: SmartCRDT/Aequor cascade routing patterns
- **Related**: `TaskComplexity`, `LLMTier`, `ComplexityAnalyzer`
- **See Also**: `LLMCache`, `ResilientLLMClient`
