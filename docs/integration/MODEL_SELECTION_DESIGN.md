# Model Selection System for Foreman and Workers

**Project:** MineWright - Minecraft Autonomous Agents
**Component:** Intelligent LLM Model Selection and Routing
**Version:** 1.0
**Date:** 2026-02-27
**Status:** Design Document

---

## Executive Summary

This document outlines a comprehensive model selection system that intelligently routes LLM requests to different GLM models based on agent role (Foreman vs Worker) and task complexity. The system optimizes for cost-efficiency by using lighter models for simple tasks while escalating to more capable models for complex reasoning.

**Key Design Principles:**
1. **Role-Based Routing:** Foreman always uses the most capable model (glm-5)
2. **Complexity Detection:** Workers analyze task parameters to determine complexity
3. **Automatic Escalation:** Workers seamlessly upgrade to glm-5 when needed
4. **Cost Optimization:** Prefer glm-4.7-air for simple tasks (3-5x cheaper)
5. **Performance Transparency:** Track model usage, latency, and costs

**Expected Cost Savings:** 40-60% reduction in LLM costs through intelligent routing

---

## Table of Contents

1. [Model Architecture Overview](#model-architecture-overview)
2. [Complexity Scoring Algorithm](#complexity-scoring-algorithm)
3. [Task Type Classification](#task-type-classification)
4. [Model Selection Decision Tree](#model-selection-decision-tree)
5. [Configuration Schema](#configuration-schema)
6. [Implementation Plan](#implementation-plan)
7. [Performance and Cost Analysis](#performance-and-cost-analysis)
8. [Monitoring and Metrics](#monitoring-and-metrics)
9. [Testing Strategy](#testing-strategy)

---

## Model Architecture Overview

### Available GLM Models

| Model | Context | Speed | Cost | Best For |
|-------|---------|-------|------|----------|
| **glm-5** | 128K | Slow | High | Complex reasoning, planning, coordination |
| **glm-4.7-air** | 128K | Fast | Low | Simple tasks, basic actions, quick responses |
| **glm-4.7-flash** | 128K | Very Fast | Very Low | Classification, routing, embeddings |
| **glm-4.7** | 128K | Medium | Medium | Balanced performance and cost |

### Agent-Model Assignment Matrix

| Agent Role | Default Model | Escalation Model | Decision Criteria |
|------------|--------------|------------------|-------------------|
| **Foreman** | glm-5 | N/A | Always uses glm-5 (no escalation) |
| **Worker (Simple)** | glm-4.7-air | glm-5 | Complexity score < 50 |
| **Worker (Complex)** | glm-5 | N/A | Complexity score >= 50 |

### Model Selection Flow

```
                    ┌─────────────┐
                    │   Request   │
                    └──────┬──────┘
                           │
                           ▼
                    ┌─────────────┐
                    │ Get Agent   │
                    │    Role     │
                    └──────┬──────┘
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
      ┌───────────────┐         ┌───────────────┐
      │   FOREMAN     │         │   WORKER      │
      └───────┬───────┘         └───────┬───────┘
              │                         │
              │                         ▼
              │                ┌────────────────┐
              │                │ Calculate      │
              │                │ Complexity     │
              │                │    Score       │
              │                └────────┬───────┘
              │                         │
              │                ┌────────┴────────┐
              │                │                 │
              │                ▼                 ▼
              │        ┌─────────────┐   ┌─────────────┐
              │        │ Score < 50  │   │ Score >= 50 │
              │        └──────┬──────┘   └──────┬──────┘
              │               │                 │
              │               ▼                 ▼
              │        ┌─────────────┐   ┌─────────────┐
              │        │glm-4.7-air  │   │   glm-5     │
              │        └─────────────┘   └─────────────┘
              │                             │
              └─────────────┬───────────────┘
                            │
                            ▼
                    ┌─────────────┐
                    │Execute with │
                    │Selected     │
                    │   Model     │
                    └─────────────┘
```

---

## Complexity Scoring Algorithm

The complexity scoring algorithm evaluates multiple dimensions of a task to determine whether it requires advanced reasoning.

### Score Components

```java
/**
 * Calculates a complexity score (0-100) for a task.
 * Higher scores indicate more complex tasks requiring glm-5.
 */
public class TaskComplexityScorer {

    // Base scores for different action types
    private static final Map<String, Integer> ACTION_BASE_SCORES = Map.ofEntries(
        // Simple actions (0-20 points)
        Map.entry("pathfind", 5),
        Map.entry("follow", 5),
        Map.entry("wait", 0),
        Map.entry("say", 5),

        // Moderate actions (20-40 points)
        Map.entry("mine", 25),
        Map.entry("place", 20),
        Map.entry("gather", 25),
        Map.entry("attack", 30),

        // Complex actions (40-60 points)
        Map.entry("build", 50),
        Map.entry("craft", 45),
        Map.entry("construct", 55),

        // Very complex actions (60-80 points)
        Map.entry("coordinate", 70),
        Map.entry("plan", 75),
        Map.entry("optimize", 65),

        // Unknown actions (conservative high score)
        Map.entry("unknown", 80)
    );

    /**
     * Calculates the total complexity score for a task.
     */
    public int calculateComplexityScore(Task task) {
        int score = 0;

        // 1. Base action type score (0-80 points)
        score += getActionBaseScore(task.getAction());

        // 2. Parameter complexity (0-15 points)
        score += calculateParameterComplexity(task);

        // 3. Quantity scale (0-5 points)
        score += calculateQuantityScale(task);

        // 4. Contextual complexity (0-10 points)
        score += calculateContextualComplexity(task);

        return Math.min(100, score); // Cap at 100
    }

    /**
     * Gets the base score for an action type.
     */
    private int getActionBaseScore(String action) {
        return ACTION_BASE_SCORES.getOrDefault(action.toLowerCase(), 80);
    }

    /**
     * Calculates parameter complexity based on parameter count and types.
     */
    private int calculateParameterComplexity(Task task) {
        int paramCount = task.getParameters().size();
        int complexity = 0;

        // Parameter count complexity
        if (paramCount > 5) complexity += 10;
        else if (paramCount > 3) complexity += 5;
        else if (paramCount > 1) complexity += 2;

        // Nested structure complexity
        for (Map.Entry<String, Object> entry : task.getParameters().entrySet()) {
            if (entry.getValue() instanceof Map) {
                complexity += 5; // Nested parameters
            }
            if (entry.getValue() instanceof Collection) {
                complexity += 3; // List/array parameters
            }
        }

        return Math.min(15, complexity);
    }

    /**
     * Calculates complexity based on quantity scale.
     */
    private int calculateQuantityScale(Task task) {
        // Check for quantity/count parameters
        int quantity = task.getIntParameter("quantity", 1);
        int count = task.getIntParameter("count", 1);
        int num = task.getIntParameter("num", 1);
        int max = Math.max(quantity, Math.max(count, num));

        if (max > 100) return 5;
        if (max > 50) return 3;
        if (max > 10) return 1;
        return 0;
    }

    /**
     * Calculates contextual complexity based on task context.
     */
    private int calculateContextualComplexity(Task task) {
        int complexity = 0;

        // Multi-step planning
        String action = task.getAction().toLowerCase();
        if (action.contains("then") || action.contains("after")) {
            complexity += 5;
        }

        // Conditional logic
        if (action.contains("if") || action.contains("unless") || action.contains("when")) {
            complexity += 5;
        }

        // Coordination requirements
        if (task.hasParameters("with", "together", "coordinate")) {
            complexity += 5;
        }

        return complexity;
    }

    /**
     * Determines if a task is complex enough to require glm-5.
     */
    public boolean isComplexTask(Task task) {
        return calculateComplexityScore(task) >= COMPLEXITY_THRESHOLD;
    }
}
```

### Complexity Thresholds

```java
/**
 * Configuration for complexity-based model selection.
 */
public class ModelSelectionConfig {

    /**
     * Threshold above which workers use glm-5 instead of glm-4.7-air.
     * Default: 50 (on a 0-100 scale)
     */
    public static final int COMPLEXITY_THRESHOLD = 50;

    /**
     * Threshold above which tasks are considered "very complex" and get
     * additional monitoring.
     * Default: 75
     */
    public static final int VERY_COMPLEX_THRESHOLD = 75;

    /**
     * Maximum complexity score (cap).
     */
    public static final int MAX_COMPLEXITY_SCORE = 100;
}
```

---

## Task Type Classification

### Task Categories

```java
/**
 * Classifies tasks into complexity categories for model selection.
 */
public enum TaskComplexity {

    /**
     * SIMPLE - Basic actions with clear, direct execution paths.
     * Model: glm-4.7-air
     * Examples: pathfind, follow, wait, say
     */
    SIMPLE(0, 30, "glm-4.7-air"),

    /**
     * MODERATE - Standard actions with some decision-making.
     * Model: glm-4.7-air
     * Examples: mine, place, gather, attack
     */
    MODERATE(31, 49, "glm-4.7-air"),

    /**
     * COMPLEX - Actions requiring multi-step planning or reasoning.
     * Model: glm-5
     * Examples: build structures, craft with recipes, coordinate workers
     */
    COMPLEX(50, 74, "glm-5"),

    /**
     * VERY_COMPLEX - Tasks requiring advanced reasoning or optimization.
     * Model: glm-5
     * Examples: multi-step planning, complex coordination, problem-solving
     */
    VERY_COMPLEX(75, 100, "glm-5");

    private final int minScore;
    private final int maxScore;
    private final String recommendedModel;

    TaskComplexity(int minScore, int maxScore, String recommendedModel) {
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.recommendedModel = recommendedModel;
    }

    /**
     * Determines the task complexity category from a score.
     */
    public static TaskComplexity fromScore(int score) {
        for (TaskComplexity category : values()) {
            if (score >= category.minScore && score <= category.maxScore) {
                return category;
            }
        }
        return VERY_COMPLEX; // Default to most capable
    }

    /**
     * Gets the recommended model for this complexity level.
     */
    public String getRecommendedModel() {
        return recommendedModel;
    }

    /**
     * Checks if this complexity level requires glm-5.
     */
    public boolean requiresAdvancedModel() {
        return this == COMPLEX || this == VERY_COMPLEX;
    }
}
```

### Action Type to Complexity Mapping

```java
/**
 * Predefined complexity mappings for common actions.
 */
public class ActionComplexityRegistry {

    private static final Map<String, ActionComplexityProfile> PROFILES;

    static {
        PROFILES = Map.ofEntries(

            // === SIMPLE ACTIONS (0-30) ===
            entry("pathfind", new ActionComplexityProfile(
                5, "glm-4.7-air",
                "Direct navigation to coordinates",
                List.of("x", "y", "z")
            )),
            entry("follow", new ActionComplexityProfile(
                5, "glm-4.7-air",
                "Follow a target entity",
                List.of("target")
            )),
            entry("wait", new ActionComplexityProfile(
                0, "glm-4.7-air",
                "Wait for a duration",
                List.of("duration")
            )),

            // === MODERATE ACTIONS (31-49) ===
            entry("mine", new ActionComplexityProfile(
                25, "glm-4.7-air",
                "Mine blocks of a specific type",
                List.of("block", "quantity")
            )),
            entry("place", new ActionComplexityProfile(
                20, "glm-4.7-air",
                "Place blocks at specific locations",
                List.of("block", "x", "y", "z")
            )),
            entry("gather", new ActionComplexityProfile(
                25, "glm-4.7-air",
                "Gather resources from the environment",
                List.of("resource", "quantity")
            )),
            entry("attack", new ActionComplexityProfile(
                30, "glm-4.7-air",
                "Attack hostile entities",
                List.of("target")
            )),

            // === COMPLEX ACTIONS (50-74) ===
            entry("build", new ActionComplexityProfile(
                50, "glm-5",
                "Construct structures from blueprints",
                List.of("structure", "location", "materials")
            )),
            entry("craft", new ActionComplexityProfile(
                45, "glm-5",
                "Craft items using recipes",
                List.of("item", "quantity", "recipe")
            )),
            entry("construct", new ActionComplexityProfile(
                55, "glm-5",
                "Multi-step construction with planning",
                List.of("blueprint", "steps")
            )),

            // === VERY COMPLEX ACTIONS (75-100) ===
            entry("coordinate", new ActionComplexityProfile(
                70, "glm-5",
                "Coordinate multiple workers",
                List.of("workers", "tasks", "strategy")
            )),
            entry("plan", new ActionComplexityProfile(
                75, "glm-5",
                "Create multi-step plans",
                List.of("goal", "constraints", "resources")
            )),
            entry("optimize", new ActionComplexityProfile(
                65, "glm-5",
                "Optimize existing workflows",
                List.of("workflow", "metrics", "constraints")
            ))
        );
    }

    /**
     * Gets the complexity profile for an action type.
     */
    public static ActionComplexityProfile getProfile(String action) {
        return PROFILES.getOrDefault(action.toLowerCase(),
            new ActionComplexityProfile(80, "glm-5", "Unknown action", List.of()));
    }

    /**
     * Profile for an action type.
     */
    public record ActionComplexityProfile(
        int baseScore,
        String recommendedModel,
        String description,
        List<String> requiredParameters
    ) {}
}
```

---

## Model Selection Decision Tree

### Decision Tree Algorithm

```java
/**
 * Decision tree for model selection based on agent role and task characteristics.
 */
public class ModelSelectionDecisionTree {

    private final TaskComplexityScorer complexityScorer;
    private final ModelSelectionConfig config;

    /**
     * Selects the appropriate model for a request.
     */
    public String selectModel(AgentRole role, Task task, ModelSelectionContext context) {
        // Decision tree traversal
        return traverseTree(role, task, context);
    }

    /**
     * Traverses the decision tree to select a model.
     */
    private String traverseTree(AgentRole role, Task task, ModelSelectionContext context) {

        // === ROOT NODE: Agent Role ===
        if (role == AgentRole.FOREMAN) {
            // Foreman always uses the most capable model
            return config.getForemanModel();
        }

        // === WORKER NODES ===
        if (role == AgentRole.WORKER || role == AgentRole.SPECIALIST) {

            // Calculate complexity
            int complexityScore = complexityScorer.calculateComplexityScore(task);
            TaskComplexity complexity = TaskComplexity.fromScore(complexityScore);

            // === COMPLEXITY CHECK ===
            if (complexity.requiresAdvancedModel()) {
                // Complex task: Use glm-5
                logModelSelection(task, "glm-5", "complexity=" + complexityScore);
                return config.getWorkerComplexModel();
            }

            // === PARAMETER COMPLEXITY CHECK ===
            if (hasComplexParameters(task)) {
                // Nested or complex parameters: Escalate to glm-5
                logModelSelection(task, "glm-5", "complex_parameters");
                return config.getWorkerComplexModel();
            }

            // === ERROR RECOVERY CHECK ===
            if (context.hasRecentErrors()) {
                // Previous failures: Escalate to glm-5
                logModelSelection(task, "glm-5", "error_recovery");
                return config.getWorkerComplexModel();
            }

            // === CONTEXT LENGTH CHECK ===
            if (context.estimatedTokenCount() > config.getContextThreshold()) {
                // Long context: Use glm-5 (better reasoning)
                logModelSelection(task, "glm-5", "long_context");
                return config.getWorkerComplexModel();
            }

            // === DEFAULT: Simple task ===
            logModelSelection(task, "glm-4.7-air", "simple_task");
            return config.getWorkerSimpleModel();
        }

        // === SOLO MODE ===
        if (role == AgentRole.SOLO) {
            // Solo agents use glm-5 for full capability
            return config.getForemanModel();
        }

        // Fallback
        return config.getWorkerSimpleModel();
    }

    /**
     * Checks if a task has complex nested parameters.
     */
    private boolean hasComplexParameters(Task task) {
        for (Object value : task.getParameters().values()) {
            // Check for nested structures
            if (value instanceof Map && ((Map<?, ?>) value).size() > 3) {
                return true;
            }
            // Check for large lists
            if (value instanceof Collection && ((Collection<?>) value).size() > 10) {
                return true;
            }
        }
        return false;
    }

    /**
     * Logs model selection decisions for monitoring.
     */
    private void logModelSelection(Task task, String model, String reason) {
        MineWrightMod.LOGGER.info("[ModelSelection] Task: {} -> Model: {} (Reason: {})",
            task.getAction(), model, reason);
    }
}
```

### Decision Tree Visualization

```
┌─────────────────────────────────────────────────────────────────┐
│                        MODEL SELECTION                          │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
                    ┌────────────────┐
                    │  Agent Role?   │
                    └───────┬────────┘
                            │
          ┌─────────────────┼─────────────────┐
          │                 │                 │
          ▼                 ▼                 ▼
    ┌───────────┐    ┌───────────┐    ┌───────────┐
    │ FOREMAN   │    │  WORKER   │    │   SOLO    │
    └─────┬─────┘    └─────┬─────┘    └─────┬─────┘
          │                │                │
          │                │                │
          │                ▼                │
          │    ┌──────────────────────┐     │
          │    │ Complexity Score?    │     │
          │    └──────────┬───────────┘     │
          │               │                  │
          │     ┌─────────┴─────────┐        │
          │     │                   │        │
          │     ▼                   ▼        │
          │  >= 50               < 50        │
          │     │                   │        │
          │     │                   │        │
          │     ▼                   ▼        │
          │ ┌─────────┐        ┌─────────┐   │
          │ │ glm-5   │        │Check    │   │
          │ └────┬────┘        │Params?  │   │
          │      │             └────┬────┘   │
          │      │                  │        │
          │      │           ┌──────┴──────┐ │
          │      │           │             │ │
          │      │           ▼             ▼ │
          │      │      Complex      Simple  │
          │      │      │             │      │
          │      │      ▼             ▼      │
          │      │   ┌───────┐   ┌─────────┐│
          │      │   │glm-5  │   │Check    ││
          │      │   └───────┘   │Errors?  ││
          │      │               └────┬────┘│
          │      │                    │     │
          │      │             ┌──────┴────┐│
          │      │             │           ││
          │      │             ▼           ▼│
          │      │         Has          No  │
          │      │         Errors       Errors
          │      │           │           ││
          │      │           ▼           ▼│
          │      │        ┌─────┐   ┌─────┐│
          │      │        │glm-5│   │Check││
          │      │        └──┬──┘   │CTX? ││
          │      │           │      └──┬──┘│
          │      │           │         │   │
          │      │           │    ┌────┴───┐│
          │      │           │    │        ││
          │      │           │    ▼        ▼│
          │      │           │ Long     Short
          │      │           │  │        ││
          │      │           │  ▼        ▼│
          │      │           │glm-5   glm-4.7-air
          │      │           │         │
          │      └───────────┴─────────┘
          │
          ▼
    ┌─────────┐
    │  glm-5  │
    └─────────┘
```

---

## Configuration Schema

### Config File Format (minewright-common.toml)

```toml
[ModelSelection]
# Enable intelligent model selection based on task complexity
enabled = true

# Default complexity threshold (0-100)
# Tasks with score >= this value use glm-5
complexity_threshold = 50

# Enable automatic escalation to glm-5 on errors
auto_escalate_on_error = true

# Number of retries before escalating
escalation_retry_threshold = 2

[ModelSelection.Models]
# Model assignments for different agent roles

# Foreman always uses the most capable model
foreman_model = "glm-5"

# Worker simple task model (fast, cost-effective)
worker_simple_model = "glm-4.7-air"

# Worker complex task model (capable reasoning)
worker_complex_model = "glm-5"

# Solo agent model (full capability)
solo_model = "glm-5"

[ModelSelection.Complexity]
# Action type base scores (0-100)

# Simple actions (0-30)
simple_actions = [
    {action = "pathfind", score = 5},
    {action = "follow", score = 5},
    {action = "wait", score = 0},
    {action = "say", score = 5}
]

# Moderate actions (31-49)
moderate_actions = [
    {action = "mine", score = 25},
    {action = "place", score = 20},
    {action = "gather", score = 25},
    {action = "attack", score = 30}
]

# Complex actions (50-74)
complex_actions = [
    {action = "build", score = 50},
    {action = "craft", score = 45},
    {action = "construct", score = 55}
]

# Very complex actions (75-100)
very_complex_actions = [
    {action = "coordinate", score = 70},
    {action = "plan", score = 75},
    {action = "optimize", score = 65}
]

[ModelSelection.Adjustments]
# Score adjustments for specific factors

# Parameter count complexity
param_count_bonus = 2  # Points per parameter over 3
max_param_bonus = 15   # Maximum parameter bonus

# Quantity scale
quantity_threshold_1 = 10   # +1 point for quantities > 10
quantity_threshold_2 = 50   # +3 points for quantities > 50
quantity_threshold_3 = 100  # +5 points for quantities > 100

# Context length threshold (tokens)
context_length_threshold = 4000  # Escalate to glm-5 if context exceeds

# Nested structure bonus
nested_structure_bonus = 5   # Points for nested Map parameters
list_structure_bonus = 3     # Points for List/array parameters

[ModelSelection.Monitoring]
# Enable model selection metrics tracking
enable_metrics = true

# Log model selection decisions
log_selections = true

# Track cost savings
track_cost_savings = true

# Generate weekly reports
generate_reports = true

[ModelSelection.CostEstimates]
# Estimated costs per 1M tokens (for tracking)

# glm-5 pricing (example)
glm_5_input_cost = 2.0
glm_5_output_cost = 6.0

# glm-4.7-air pricing (example)
glm_4_7_air_input_cost = 0.5
glm_4_7_air_output_cost = 1.5

# Currency for cost tracking
currency = "USD"
```

### Java Configuration Class

```java
/**
 * Configuration for model selection system.
 */
public class ModelSelectionConfig {

    // Enable/disable model selection
    private boolean enabled = true;

    // Complexity threshold (0-100)
    private int complexityThreshold = 50;

    // Auto-escalate on errors
    private boolean autoEscalateOnError = true;

    // Retry threshold before escalation
    private int escalationRetryThreshold = 2;

    // Model assignments
    private String foremanModel = "glm-5";
    private String workerSimpleModel = "glm-4.7-air";
    private String workerComplexModel = "glm-5";
    private String soloModel = "glm-5";

    // Monitoring
    private boolean enableMetrics = true;
    private boolean logSelections = true;
    private boolean trackCostSavings = true;

    // Cost estimates (per 1M tokens)
    private double glm5InputCost = 2.0;
    private double glm5OutputCost = 6.0;
    private double glm47AirInputCost = 0.5;
    private double glm47AirOutputCost = 1.5;

    // Context length threshold
    private int contextLengthThreshold = 4000;

    // Getters
    public boolean isEnabled() { return enabled; }
    public int getComplexityThreshold() { return complexityThreshold; }
    public boolean shouldAutoEscalateOnError() { return autoEscalateOnError; }
    public int getEscalationRetryThreshold() { return escalationRetryThreshold; }
    public String getForemanModel() { return foremanModel; }
    public String getWorkerSimpleModel() { return workerSimpleModel; }
    public String getWorkerComplexModel() { return workerComplexModel; }
    public String getSoloModel() { return soloModel; }
    public boolean isMetricsEnabled() { return enableMetrics; }
    public boolean shouldLogSelections() { return logSelections; }
    public boolean shouldTrackCostSavings() { return trackCostSavings; }
    public double getGlm5InputCost() { return glm5InputCost; }
    public double getGlm5OutputCost() { return glm5OutputCost; }
    public double getGlm47AirInputCost() { return glm47AirInputCost; }
    public double getGlm47AirOutputCost() { return glm47AirOutputCost; }
    public int getContextThreshold() { return contextLengthThreshold; }

    /**
     * Loads configuration from Forge config spec.
     */
    public static ModelSelectionConfig fromForgeConfig(ForgeConfigSpec spec) {
        ModelSelectionConfig config = new ModelSelectionConfig();

        // Load values from config spec
        // (implementation depends on actual config structure)

        return config;
    }
}
```

---

## Implementation Plan

### Phase 1: Core Model Selection Service (Week 1)

**Files to Create:**

```java
// File: src/main/java/com/minewright/llm/model/TaskComplexityScorer.java
package com.minewright.llm.model;

import com.minewright.action.Task;
import java.util.Map;

/**
 * Calculates complexity scores for tasks to determine model selection.
 */
public class TaskComplexityScorer {

    private static final Map<String, Integer> ACTION_BASE_SCORES = Map.of(
        "pathfind", 5,
        "follow", 5,
        "wait", 0,
        "say", 5,
        "mine", 25,
        "place", 20,
        "gather", 25,
        "attack", 30,
        "build", 50,
        "craft", 45,
        "construct", 55,
        "coordinate", 70,
        "plan", 75,
        "optimize", 65
    );

    private final ModelSelectionConfig config;

    public TaskComplexityScorer(ModelSelectionConfig config) {
        this.config = config;
    }

    /**
     * Calculates complexity score (0-100) for a task.
     */
    public int calculateComplexityScore(Task task) {
        int score = getActionBaseScore(task.getAction());
        score += calculateParameterComplexity(task);
        score += calculateQuantityScale(task);
        score += calculateContextualComplexity(task);
        return Math.min(100, score);
    }

    /**
     * Checks if a task requires glm-5.
     */
    public boolean isComplexTask(Task task) {
        return calculateComplexityScore(task) >= config.getComplexityThreshold();
    }

    private int getActionBaseScore(String action) {
        return ACTION_BASE_SCORES.getOrDefault(action.toLowerCase(), 80);
    }

    private int calculateParameterComplexity(Task task) {
        int paramCount = task.getParameters().size();
        int complexity = 0;

        if (paramCount > 5) complexity += 10;
        else if (paramCount > 3) complexity += 5;
        else if (paramCount > 1) complexity += 2;

        for (Object value : task.getParameters().values()) {
            if (value instanceof Map) complexity += 5;
            if (value instanceof Collection) complexity += 3;
        }

        return Math.min(15, complexity);
    }

    private int calculateQuantityScale(Task task) {
        int quantity = task.getIntParameter("quantity", 1);
        int count = task.getIntParameter("count", 1);
        int max = Math.max(quantity, count);

        if (max > 100) return 5;
        if (max > 50) return 3;
        if (max > 10) return 1;
        return 0;
    }

    private int calculateContextualComplexity(Task task) {
        int complexity = 0;
        String action = task.getAction().toLowerCase();

        if (action.contains("then") || action.contains("after")) complexity += 5;
        if (action.contains("if") || action.contains("when")) complexity += 5;
        if (task.hasParameters("with", "together", "coordinate")) complexity += 5;

        return complexity;
    }
}
```

```java
// File: src/main/java/com/minewright/llm/model/ModelSelector.java
package com.minewright.llm.model;

import com.minewright.MineWrightMod;
import com.minewright.action.Task;
import com.minewright.orchestration.AgentRole;

/**
 * Selects the appropriate LLM model based on agent role and task complexity.
 */
public class ModelSelector {

    private final TaskComplexityScorer complexityScorer;
    private final ModelSelectionConfig config;
    private final ModelSelectionMetrics metrics;

    public ModelSelector(ModelSelectionConfig config) {
        this.config = config;
        this.complexityScorer = new TaskComplexityScorer(config);
        this.metrics = new ModelSelectionMetrics();
    }

    /**
     * Selects the appropriate model for a request.
     */
    public String selectModel(AgentRole role, Task task, ModelSelectionContext context) {
        if (!config.isEnabled()) {
            return config.getForemanModel(); // Fallback to default
        }

        String selectedModel;

        if (role == AgentRole.FOREMAN) {
            // Foreman always uses glm-5
            selectedModel = config.getForemanModel();
            logSelection(task, selectedModel, "foreman_role");

        } else if (role == AgentRole.WORKER || role == AgentRole.SPECIALIST) {
            // Worker: Check complexity
            int complexityScore = complexityScorer.calculateComplexityScore(task);

            if (complexityScore >= config.getComplexityThreshold()) {
                selectedModel = config.getWorkerComplexModel();
                logSelection(task, selectedModel, "complexity=" + complexityScore);
            } else if (context.hasRecentErrors()) {
                selectedModel = config.getWorkerComplexModel();
                logSelection(task, selectedModel, "error_recovery");
            } else {
                selectedModel = config.getWorkerSimpleModel();
                logSelection(task, selectedModel, "simple_task");
            }

        } else {
            // SOLO or unknown
            selectedModel = config.getSoloModel();
            logSelection(task, selectedModel, "solo_mode");
        }

        // Track metrics
        metrics.recordSelection(role, task.getAction(), selectedModel);

        return selectedModel;
    }

    private void logSelection(Task task, String model, String reason) {
        if (config.shouldLogSelections()) {
            MineWrightMod.LOGGER.info("[ModelSelection] {} -> {} ({})",
                task.getAction(), model, reason);
        }
    }

    /**
     * Gets the metrics tracker.
     */
    public ModelSelectionMetrics getMetrics() {
        return metrics;
    }
}
```

### Phase 2: Integration with TaskPlanner (Week 1-2)

```java
// File: src/main/java/com/minewright/llm/ModelAwareTaskPlanner.java
package com.minewright.llm;

import com.minewright.llm.model.*;
import com.minewright.orchestration.AgentRole;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced TaskPlanner with model selection based on agent role and task complexity.
 */
public class ModelAwareTaskPlanner extends TaskPlanner {

    private final ModelSelector modelSelector;
    private final Map<String, AsyncLLMClient> modelClients;

    public ModelAwareTaskPlanner() {
        super();

        // Initialize model selector
        ModelSelectionConfig config = ModelSelectionConfig.fromForgeConfig(
            MineWrightConfig.SPEC
        );
        this.modelSelector = new ModelSelector(config);

        // Initialize multiple model clients
        this.modelClients = new ConcurrentHashMap<>();
        modelClients.put("glm-5", new AsyncOpenAIClient(
            MineWrightConfig.OPENAI_API_KEY.get(),
            "glm-5",
            MineWrightConfig.MAX_TOKENS.get(),
            MineWrightConfig.TEMPERATURE.get()
        ));
        modelClients.put("glm-4.7-air", new AsyncOpenAIClient(
            MineWrightConfig.OPENAI_API_KEY.get(),
            "glm-4.7-air",
            MineWrightConfig.MAX_TOKENS.get(),
            MineWrightConfig.TEMPERATURE.get()
        ));
    }

    /**
     * Plans tasks with automatic model selection.
     */
    @Override
    public CompletableFuture<ParsedResponse> planTasksAsync(
        ForemanEntity foreman,
        String command
    ) {
        // Get agent role
        AgentRole role = foreman.getRole();

        // Determine context
        ModelSelectionContext context = ModelSelectionContext.builder()
            .agentRole(role)
            .commandLength(command.length())
            .recentErrors(getRecentErrorCount(foreman))
            .build();

        // For now, we need to plan tasks first to get the task list
        // In production, we could analyze the command directly

        // Use default model for planning
        return super.planTasksAsync(foreman, command).thenApply(parsedResponse -> {
            // Re-plan individual tasks with model selection
            if (parsedResponse != null && !parsedResponse.getTasks().isEmpty()) {
                return planTasksWithModelSelection(foreman, parsedResponse.getTasks());
            }
            return parsedResponse;
        });
    }

    /**
     * Plans each task with appropriate model selection.
     */
    private ParsedResponse planTasksWithModelSelection(
        ForemanEntity foreman,
        List<Task> tasks
    ) {
        // Group tasks by model
        Map<String, List<Task>> tasksByModel = new HashMap<>();

        for (Task task : tasks) {
            String model = modelSelector.selectModel(
                foreman.getRole(),
                task,
                ModelSelectionContext.defaultContext()
            );
            tasksByModel.computeIfAbsent(model, k -> new ArrayList<>()).add(task);
        }

        // Execute tasks by model batch
        ParsedResponse combinedResponse = new ParsedResponse();
        for (Map.Entry<String, List<Task>> entry : tasksByModel.entrySet()) {
            String model = entry.getKey();
            List<Task> modelTasks = entry.getValue();

            // Get appropriate client
            AsyncLLMClient client = modelClients.get(model);
            if (client != null) {
                // Process tasks with this model
                // (Implementation depends on specific task planning logic)
            }
        }

        return combinedResponse;
    }
}
```

### Phase 3: Metrics and Monitoring (Week 2)

```java
// File: src/main/java/com/minewright/llm/model/ModelSelectionMetrics.java
package com.minewright.llm.model;

import com.minewright.orchestration.AgentRole;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks model selection metrics and cost savings.
 */
public class ModelSelectionMetrics {

    // Selection counts by model
    private final Map<String, AtomicLong> selectionCounts = new ConcurrentHashMap<>();

    // Selection counts by role and model
    private final Map<String, Map<String, AtomicLong>> selectionsByRole = new ConcurrentHashMap<>();

    // Token counts by model
    private final Map<String, AtomicLong> inputTokens = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> outputTokens = new ConcurrentHashMap<>();

    // Cost tracking
    private final AtomicLong totalEstimatedCost = new AtomicLong(0);

    // Escalation tracking
    private final AtomicLong escalationCount = new AtomicLong(0);

    public ModelSelectionMetrics() {
        // Initialize counters
        selectionCounts.put("glm-5", new AtomicLong(0));
        selectionCounts.put("glm-4.7-air", new AtomicLong(0));
    }

    /**
     * Records a model selection decision.
     */
    public void recordSelection(AgentRole role, String action, String model) {
        selectionCounts.getOrDefault(model, new AtomicLong(0)).incrementAndGet();

        selectionsByRole
            .computeIfAbsent(role.name(), k -> new ConcurrentHashMap<>())
            .computeIfAbsent(model, k -> new AtomicLong(0))
            .incrementAndGet();
    }

    /**
     * Records token usage for a model.
     */
    public void recordTokens(String model, int inputTokens, int outputTokens) {
        this.inputTokens.getOrDefault(model, new AtomicLong(0)).addAndGet(inputTokens);
        this.outputTokens.getOrDefault(model, new AtomicLong(0)).addAndGet(outputTokens);
    }

    /**
     * Records an escalation event.
     */
    public void recordEscalation() {
        escalationCount.incrementAndGet();
    }

    /**
     * Calculates estimated cost savings.
     */
    public double calculateSavings(ModelSelectionConfig config) {
        long glm5Tokens = inputTokens.getOrDefault("glm-5", new AtomicLong(0)).get() +
                         outputTokens.getOrDefault("glm-5", new AtomicLong(0)).get();

        long glm47AirTokens = inputTokens.getOrDefault("glm-4.7-air", new AtomicLong(0)).get() +
                             outputTokens.getOrDefault("glm-4.7-air", new AtomicLong(0)).get();

        // Cost if all used glm-5
        double allGlm5Cost = (glm5Tokens + glm47AirTokens) / 1_000_000.0 *
                           ((config.getGlm5InputCost() + config.getGlm5OutputCost()) / 2);

        // Actual cost with mixed models
        double actualCost = (glm5Tokens / 1_000_000.0) *
                          ((config.getGlm5InputCost() + config.getGlm5OutputCost()) / 2) +
                          (glm47AirTokens / 1_000_000.0) *
                          ((config.getGlm47AirInputCost() + config.getGlm47AirOutputCost()) / 2);

        return allGlm5Cost - actualCost;
    }

    /**
     * Generates a metrics report.
     */
    public String generateReport(ModelSelectionConfig config) {
        StringBuilder report = new StringBuilder();
        report.append("=== Model Selection Metrics ===\n");

        // Selection counts
        report.append("\nSelection Counts:\n");
        for (Map.Entry<String, AtomicLong> entry : selectionCounts.entrySet()) {
            report.append(String.format("  %s: %d\n", entry.getKey(), entry.getValue().get()));
        }

        // Token counts
        report.append("\nToken Usage:\n");
        for (String model : selectionCounts.keySet()) {
            long inTokens = inputTokens.getOrDefault(model, new AtomicLong(0)).get();
            long outTokens = outputTokens.getOrDefault(model, new AtomicLong(0)).get();
            report.append(String.format("  %s: %d input, %d output\n", model, inTokens, outTokens));
        }

        // Escalations
        report.append(String.format("\nEscalations: %d\n", escalationCount.get()));

        // Cost savings
        double savings = calculateSavings(config);
        report.append(String.format("\nEstimated Cost Savings: $%.4f %s\n",
            savings, config.getCurrency()));

        return report.toString();
    }

    // Getters
    public long getSelectionCount(String model) {
        return selectionCounts.getOrDefault(model, new AtomicLong(0)).get();
    }

    public long getEscalationCount() {
        return escalationCount.get();
    }
}
```

### Phase 4: Configuration Integration (Week 2)

```java
// File: src/main/java/com/minewright/config/ModelSelectionConfig.java
package com.minewright.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class MineWrightConfig {
    // ... existing config ...

    // Model selection configuration
    public static final ForgeConfigSpec.BooleanValue MODEL_SELECTION_ENABLED;
    public static final ForgeConfigSpec.IntValue COMPLEXITY_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<String> FOREMAN_MODEL;
    public static final ForgeConfigSpec.ConfigValue<String> WORKER_SIMPLE_MODEL;
    public static final ForgeConfigSpec.ConfigValue<String> WORKER_COMPLEX_MODEL;
    public static final ForgeConfigSpec.ConfigValue<String> SOLO_MODEL;
    public static final ForgeConfigSpec.BooleanValue AUTO_ESCALATE_ON_ERROR;
    public static final ForgeConfigSpec.IntValue ESCALATION_RETRY_THRESHOLD;

    static {
        // ... existing config builder ...

        // Model selection configuration
        builder.comment("Intelligent Model Selection").push("modelSelection");

        MODEL_SELECTION_ENABLED = builder
            .comment("Enable intelligent model selection based on task complexity")
            .define("enabled", true);

        COMPLEXITY_THRESHOLD = builder
            .comment("Complexity threshold (0-100). Tasks with score >= this use glm-5")
            .defineInRange("complexityThreshold", 50, 0, 100);

        FOREMAN_MODEL = builder
            .comment("Model for Foreman agent (always most capable)")
            .define("foremanModel", "glm-5");

        WORKER_SIMPLE_MODEL = builder
            .comment("Model for Worker simple tasks (fast, cost-effective)")
            .define("workerSimpleModel", "glm-4.7-air");

        WORKER_COMPLEX_MODEL = builder
            .comment("Model for Worker complex tasks (capable reasoning)")
            .define("workerComplexModel", "glm-5");

        SOLO_MODEL = builder
            .comment("Model for Solo agent (full capability)")
            .define("soloModel", "glm-5");

        AUTO_ESCALATE_ON_ERROR = builder
            .comment("Automatically escalate to glm-5 on errors")
            .define("autoEscalateOnError", true);

        ESCALATION_RETRY_THRESHOLD = builder
            .comment("Number of retries before escalating to glm-5")
            .defineInRange("escalationRetryThreshold", 2, 1, 5);

        builder.pop();
    }
}
```

### Phase 5: Testing (Week 3)

```java
// File: src/test/java/com/minewright/llm/model/ModelSelectionTest.java
package com.minewright.llm.model;

import com.minewright.action.Task;
import com.minewright.orchestration.AgentRole;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for model selection system.
 */
class ModelSelectionTest {

    @Test
    void testForemanAlwaysUsesGlm5() {
        ModelSelector selector = new ModelSelector(testConfig());
        Task task = new Task("pathfind", Map.of("x", 0, "y", 64, "z", 0));

        String model = selector.selectModel(
            AgentRole.FOREMAN,
            task,
            ModelSelectionContext.defaultContext()
        );

        assertEquals("glm-5", model);
    }

    @Test
    void testSimpleTaskUsesAirModel() {
        ModelSelector selector = new ModelSelector(testConfig());
        Task task = new Task("pathfind", Map.of("x", 0, "y", 64, "z", 0));

        String model = selector.selectModel(
            AgentRole.WORKER,
            task,
            ModelSelectionContext.defaultContext()
        );

        assertEquals("glm-4.7-air", model);
    }

    @Test
    void testComplexTaskUsesGlm5() {
        ModelSelector selector = new ModelSelector(testConfig());
        Task task = new Task("build", Map.of(
            "structure", "castle",
            "location", Map.of("x", 0, "y", 64, "z", 0),
            "materials", List.of("stone", "wood", "iron")
        ));

        String model = selector.selectModel(
            AgentRole.WORKER,
            task,
            ModelSelectionContext.defaultContext()
        );

        assertEquals("glm-5", model);
    }

    @Test
    void testComplexityScoreCalculation() {
        TaskComplexityScorer scorer = new TaskComplexityScorer(testConfig());

        // Simple task
        Task simpleTask = new Task("pathfind", Map.of("x", 0, "y", 64, "z", 0));
        int simpleScore = scorer.calculateComplexityScore(simpleTask);
        assertTrue(simpleScore < 50, "Simple task should have low complexity");

        // Complex task
        Task complexTask = new Task("build", Map.of(
            "structure", "castle",
            "blueprint", Map.of("rooms", List.of("bedroom", "kitchen", "hall"))
        ));
        int complexScore = scorer.calculateComplexityScore(complexTask);
        assertTrue(complexScore >= 50, "Complex task should have high complexity");
    }

    @Test
    void testEscalationOnError() {
        ModelSelector selector = new ModelSelector(testConfig());
        Task task = new Task("mine", Map.of("block", "stone", "quantity", 10));

        ModelSelectionContext context = ModelSelectionContext.builder()
            .recentErrors(3)
            .build();

        String model = selector.selectModel(AgentRole.WORKER, task, context);

        assertEquals("glm-5", model, "Should escalate on errors");
    }

    private ModelSelectionConfig testConfig() {
        ModelSelectionConfig config = new ModelSelectionConfig();
        config.setEnabled(true);
        config.setComplexityThreshold(50);
        config.setForemanModel("glm-5");
        config.setWorkerSimpleModel("glm-4.7-air");
        config.setWorkerComplexModel("glm-5");
        return config;
    }
}
```

---

## Performance and Cost Analysis

### Cost Comparison Matrix

| Scenario | Without Model Selection | With Model Selection | Savings |
|----------|------------------------|----------------------|---------|
| **Simple Tasks (40%)** | glm-5: $2.40 | glm-4.7-air: $0.60 | **75%** |
| **Moderate Tasks (30%)** | glm-5: $1.80 | glm-4.7-air: $0.45 | **75%** |
| **Complex Tasks (30%)** | glm-5: $1.80 | glm-5: $1.80 | 0% |
| **TOTAL** | **$6.00** | **$2.85** | **52.5%** |

*Assumptions: 1000 tasks per day, 1000 tokens per task, glm-5 at $6/M tokens, glm-4.7-air at $1.5/M tokens*

### Latency Comparison

| Model | Avg Response Time | 95th Percentile | Throughput |
|-------|-------------------|-----------------|------------|
| glm-5 | 1200ms | 1800ms | ~50 req/min |
| glm-4.7-air | 600ms | 900ms | ~100 req/min |

**Expected System Latency Improvement:** 35-40% faster for simple tasks

### Annual Cost Projections

| Usage Level | Tasks/Year | Cost (All glm-5) | Cost (Mixed) | Annual Savings |
|-------------|-----------|------------------|--------------|----------------|
| **Development** | 100K | $600 | $285 | $315 (52.5%) |
| **Production (Low)** | 1M | $6,000 | $2,850 | $3,150 (52.5%) |
| **Production (High)** | 10M | $60,000 | $28,500 | $31,500 (52.5%) |

### Break-Even Analysis

**Implementation Cost:** ~40 hours development
**Hourly Rate:** $100/hour
**Total Investment:** $4,000

**Break-Even Point:**
- Low usage (100K tasks/year): 1.3 years
- Medium usage (1M tasks/year): 1.5 months
- High usage (10M tasks/year): 5 days

---

## Monitoring and Metrics

### Key Performance Indicators (KPIs)

```java
/**
 * KPIs for model selection system.
 */
public class ModelSelectionKPIs {

    /**
     * Cost savings percentage.
     * Target: > 40%
     */
    public double calculateCostSavingsPercentage() {
        double actualCost = metrics.getTotalCost();
        double projectedCost = metrics.getCostIfAllGlm5();
        return ((projectedCost - actualCost) / projectedCost) * 100;
    }

    /**
     * Simple task routing rate.
     * Target: > 60% (indicates good cost optimization)
     */
    public double calculateSimpleTaskRoutingRate() {
        long simpleTasks = metrics.getSelectionCount("glm-4.7-air");
        long totalTasks = metrics.getTotalSelections();
        return ((double) simpleTasks / totalTasks) * 100;
    }

    /**
     * Escalation rate.
     * Target: < 10% (indicates good complexity detection)
     */
    public double calculateEscalationRate() {
        long escalations = metrics.getEscalationCount();
        long totalRequests = metrics.getTotalSelections();
        return ((double) escalations / totalRequests) * 100;
    }

    /**
     * Average latency improvement.
     * Target: > 30% faster for simple tasks
     */
    public double calculateLatencyImprovement() {
        double glm5Latency = metrics.getAverageLatency("glm-5");
        double airLatency = metrics.getAverageLatency("glm-4.7-air");
        return ((glm5Latency - airLatency) / glm5Latency) * 100;
    }

    /**
     * Error rate by model.
     * Target: < 5% for all models
     */
    public Map<String, Double> calculateErrorRates() {
        Map<String, Double> errorRates = new HashMap<>();
        for (String model : List.of("glm-5", "glm-4.7-air")) {
            long errors = metrics.getErrorCount(model);
            long total = metrics.getSelectionCount(model);
            errorRates.put(model, (total > 0) ? ((double) errors / total) * 100 : 0);
        }
        return errorRates;
    }
}
```

### Dashboard Metrics

```
┌────────────────────────────────────────────────────────────────┐
│                    MODEL SELECTION DASHBOARD                  │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  📊 COST SAVINGS                                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  ████████████████████░░░░░░░░░░░░░  52.5% ($31,500/yr)  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  📈 MODEL DISTRIBUTION                                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  glm-4.7-air  ████████████████████████████  70%        │   │
│  │  glm-5        ████████                        30%        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ⚡ LATENCY COMPARISON                                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  glm-4.7-air  600ms  ████████████████████              │   │
│  │  glm-5        1200ms ████████████████████████████████  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  🔧 SYSTEM HEALTH                                               │
│  • Escalation Rate: 8.2% (Target: < 10%) ✓                    │
│  • Error Rate (glm-5): 2.1% ✓                                 │
│  • Error Rate (glm-4.7-air): 3.4% ✓                           │
│  • Cache Hit Rate: 45.2%                                      │
│                                                                 │
│  📅 LAST 7 DAYS                                                │
│  • Total Requests: 45,231                                     │
│  • Cost Savings: $612.30                                      │
│  • Avg Latency Improvement: 38.2%                             │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

### Alert Thresholds

```java
/**
 * Alert configuration for model selection system.
 */
public class ModelSelectionAlerts {

    // Cost savings dropped below threshold
    private static final double MIN_COST_SAVINGS_PERCENTAGE = 30.0;

    // Escalation rate too high (indicates poor complexity detection)
    private static final double MAX_ESCALATION_RATE = 15.0;

    // Error rate too high for a model
    private static final double MAX_ERROR_RATE = 10.0;

    // Latency degradation
    private static final double MAX_LATENCY_MS = 1500;

    /**
     * Checks if any alerts should be triggered.
     */
    public List<Alert> checkAlerts(ModelSelectionMetrics metrics) {
        List<Alert> alerts = new ArrayList<>();

        // Check cost savings
        if (metrics.calculateCostSavingsPercentage() < MIN_COST_SAVINGS_PERCENTAGE) {
            alerts.add(new Alert(
                Alert.Level.WARNING,
                "Low cost savings",
                String.format("Cost savings dropped to %.1f%% (target: >%.1f%%)",
                    metrics.calculateCostSavingsPercentage(),
                    MIN_COST_SAVINGS_PERCENTAGE)
            ));
        }

        // Check escalation rate
        if (metrics.calculateEscalationRate() > MAX_ESCALATION_RATE) {
            alerts.add(new Alert(
                Alert.Level.WARNING,
                "High escalation rate",
                String.format("Escalation rate is %.1f%% (target: <%.1f%%)",
                    metrics.calculateEscalationRate(),
                    MAX_ESCALATION_RATE)
            ));
        }

        // Check error rates
        for (Map.Entry<String, Double> entry : metrics.calculateErrorRates().entrySet()) {
            if (entry.getValue() > MAX_ERROR_RATE) {
                alerts.add(new Alert(
                    Alert.Level.ERROR,
                    "High error rate",
                    String.format("Error rate for %s is %.1f%% (target: <%.1f%%)",
                        entry.getKey(), entry.getValue(), MAX_ERROR_RATE)
                ));
            }
        }

        return alerts;
    }

    public record Alert(Level level, String title, String message) {
        enum Level { INFO, WARNING, ERROR }
    }
}
```

---

## Testing Strategy

### Unit Tests

```java
/**
 * Test suite for model selection system.
 */
@ExtendWith(MockitoExtension.class)
class ModelSelectionTestSuite {

    @Test
    @DisplayName("Foreman should always use glm-5 regardless of task complexity")
    void testForemanAlwaysUsesGlm5() {
        // Implementation shown earlier
    }

    @Test
    @DisplayName("Workers should use glm-4.7-air for simple pathfinding")
    void testWorkerUsesAirForSimplePathfinding() {
        Task task = new Task("pathfind", Map.of("x", 100, "y", 64, "z", -200));
        ModelSelector selector = new ModelSelector(testConfig());

        String model = selector.selectModel(
            AgentRole.WORKER,
            task,
            ModelSelectionContext.defaultContext()
        );

        assertEquals("glm-4.7-air", model);
    }

    @Test
    @DisplayName("Workers should escalate to glm-5 for complex building")
    void testWorkerEscalatesForComplexBuilding() {
        Task task = new Task("build", Map.of(
            "structure", "underground_base",
            "blueprint", Map.of(
                "rooms", List.of("bedroom", "storage", "farm", "enchanting"),
                "depth", 50,
                "materials", Map.of(
                    "stone", 5000,
                    "iron", 500,
                    "redstone", 100
                )
            )
        ));

        String model = selector.selectModel(
            AgentRole.WORKER,
            task,
            ModelSelectionContext.defaultContext()
        );

        assertEquals("glm-5", model);
    }

    @Test
    @DisplayName("Complexity score should calculate correctly for multi-step tasks")
    void testComplexityScoreForMultiStepTasks() {
        Task task = new Task("craft", Map.of(
            "item", "diamond_pickaxe",
            "steps", List.of(
                "smelt iron_ore",
                "craft sticks",
                "craft crafting_table",
                "combine ingredients"
            )
        ));

        int score = new TaskComplexityScorer(testConfig())
            .calculateComplexityScore(task);

        assertTrue(score >= 50, "Multi-step task should be complex");
    }

    @Test
    @DisplayName("Escalation should occur after retry threshold")
    void testEscalationAfterRetries() {
        ModelSelectionContext context = ModelSelectionContext.builder()
            .recentErrors(3)
            .build();

        Task task = new Task("mine", Map.of("block", "stone", "quantity", 64));
        String model = new ModelSelector(testConfig())
            .selectModel(AgentRole.WORKER, task, context);

        assertEquals("glm-5", model);
    }

    @Test
    @DisplayName("Metrics should track cost savings accurately")
    void testMetricsTrackCostSavings() {
        ModelSelectionMetrics metrics = new ModelSelectionMetrics();

        // Simulate usage
        for (int i = 0; i < 100; i++) {
            metrics.recordTokens("glm-4.7-air", 500, 500);
            if (i < 30) {
                metrics.recordTokens("glm-5", 1000, 1000);
            }
        }

        double savings = metrics.calculateSavings(testConfig());
        assertTrue(savings > 0, "Should show cost savings");
    }
}
```

### Integration Tests

```java
/**
 * Integration tests for model selection with full system.
 */
@ExtendWith(MinecraftExtension.class)
class ModelSelectionIntegrationTest {

    @Test
    @DisplayName("End-to-end test: Worker executes simple task with glm-4.7-air")
    void testSimpleTaskExecution( MinecraftServer server) {
        // Setup
        ForemanEntity worker = spawnWorker(server);
        Task task = new Task("pathfind", Map.of("x", 100, "y", 64, "z", -200));

        // Execute
        CompletableFuture<ActionResult> future = worker.executeTask(task);

        // Verify
        ActionResult result = future.join();
        assertEquals(ActionResult.Status.SUCCESS, result.getStatus());
        assertEquals("glm-4.7-air", result.getModelUsed());
    }

    @Test
    @DisplayName("End-to-end test: Worker executes complex task with glm-5")
    void testComplexTaskExecution(MinecraftServer server) {
        // Setup
        ForemanEntity worker = spawnWorker(server);
        Task task = new Task("build", Map.of(
            "structure", "castle",
            "blueprint", getComplexBlueprint()
        ));

        // Execute
        CompletableFuture<ActionResult> future = worker.executeTask(task);

        // Verify
        ActionResult result = future.join();
        assertEquals(ActionResult.Status.SUCCESS, result.getStatus());
        assertEquals("glm-5", result.getModelUsed());
    }

    @Test
    @DisplayName("Stress test: 100 concurrent tasks with model selection")
    void testConcurrentTaskExecution(MinecraftServer server) {
        // Setup
        List<ForemanEntity> workers = spawnWorkers(server, 10);
        List<Task> tasks = generateMixedTasks(100);

        // Execute
        List<CompletableFuture<ActionResult>> futures = new ArrayList<>();
        for (Task task : tasks) {
            ForemanEntity worker = selectWorker(workers);
            futures.add(worker.executeTask(task));
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Verify
        long successCount = futures.stream()
            .map(CompletableFuture::join)
            .filter(r -> r.getStatus() == ActionResult.Status.SUCCESS)
            .count();

        assertTrue(successCount >= 95, "At least 95% of tasks should succeed");
    }
}
```

### Performance Tests

```java
/**
 * Performance benchmarks for model selection system.
 */
class ModelSelectionPerformanceTest {

    @Test
    @DisplayName("Benchmark: Model selection overhead")
    void benchmarkModelSelectionOverhead() {
        ModelSelector selector = new ModelSelector(testConfig());
        Task task = new Task("pathfind", Map.of("x", 100, "y", 64, "z", -200));

        long startTime = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            selector.selectModel(AgentRole.WORKER, task,
                ModelSelectionContext.defaultContext());
        }
        long endTime = System.nanoTime();

        long avgTimeNs = (endTime - startTime) / 10_000;
        assertTrue(avgTimeNs < 1_000_000, // < 1ms
            "Model selection should be fast: " + avgTimeNs + "ns");
    }

    @Test
    @DisplayName("Benchmark: Complexity scoring overhead")
    void benchmarkComplexityScoring() {
        TaskComplexityScorer scorer = new TaskComplexityScorer(testConfig());
        Task task = new Task("build", Map.of("structure", "house"));

        long startTime = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            scorer.calculateComplexityScore(task);
        }
        long endTime = System.nanoTime();

        long avgTimeNs = (endTime - startTime) / 10_000;
        assertTrue(avgTimeNs < 500_000, // < 0.5ms
            "Complexity scoring should be fast: " + avgTimeNs + "ns");
    }
}
```

---

## Conclusion

This model selection system provides a comprehensive solution for intelligently routing LLM requests to appropriate GLM models based on agent role and task complexity. The key benefits are:

**Cost Optimization:**
- 40-60% reduction in LLM costs through intelligent routing
- Simple tasks use cost-effective glm-4.7-air
- Complex tasks still get full glm-5 capability

**Performance Improvements:**
- 35-40% faster response times for simple tasks
- Reduced load on premium model capacity
- Better resource utilization

**Intelligence:**
- Automatic complexity detection
- Dynamic escalation based on task characteristics
- Error recovery with automatic model upgrade

**Transparency:**
- Comprehensive metrics and monitoring
- Cost savings tracking
- Performance dashboards

**Reliability:**
- Graceful fallback to capable models
- Configurable thresholds
- Alert system for anomalies

The implementation plan provides a clear path from basic model selection to full system integration, with comprehensive testing at each phase.

---

## Next Steps

1. **Review and approve** this design document
2. **Implement Phase 1** (Core model selection service)
3. **Write unit tests** for complexity scoring
4. **Integrate with TaskPlanner** (Phase 2)
5. **Add metrics and monitoring** (Phase 3)
6. **Configure in production** (Phase 4)
7. **Run comprehensive tests** (Phase 5)
8. **Monitor and optimize** based on production metrics

---

**Document Version:** 1.0
**Author:** MineWright Development Team
**Status:** Design Complete - Ready for Implementation
**Last Updated:** 2026-02-27
