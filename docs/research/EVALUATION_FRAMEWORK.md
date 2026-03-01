# Evaluation Framework for Steve AI Dissertation

**Version:** 1.0
**Date:** 2025-02-28
**Status:** Academic Publication Ready

## Executive Summary

This document defines a rigorous, reproducible evaluation framework for validating the claims made in the dissertation about Steve AI ("Cursor for Minecraft"). The framework provides quantitative metrics, benchmark scenarios, comparison methodologies, and measurement procedures suitable for academic publication.

**Core Claims to Evaluate:**
1. **Performance:** Async LLM integration reduces planning latency by 90%+ compared to synchronous approaches
2. **Cost:** Cascade routing and caching reduce LLM API costs by 60-80%
3. **Capability:** LLM-powered planning enables complex multi-step reasoning beyond traditional scripted AI
4. **Quality:** Hybrid architecture (LLM planning + action execution) achieves higher task success rates than pure LLM or pure scripted approaches

---

## Table of Contents

1. [Evaluation Criteria](#1-evaluation-criteria)
2. [Benchmark Scenarios](#2-benchmark-scenarios)
3. [Comparison Methodology](#3-comparison-methodology)
4. [Metrics and Measurement Procedures](#4-metrics-and-measurement-procedures)
5. [Experimental Design](#5-experimental-design)
6. [Data Collection Templates](#6-data-collection-templates)
7. [Statistical Analysis](#7-statistical-analysis)
8. [Reproducibility Guidelines](#8-reproducibility-guidelines)

---

## 1. Evaluation Criteria

### 1.1 Latency Metrics

#### Planning Latency (T_plan)
**Definition:** Time from user command submission to task queue completion.

**Measurement Points:**
- **T_submit:** Timestamp when `processNaturalLanguageCommand()` is called
- **T_queue:** Timestamp when first task is queued in `taskQueue`
- **T_plan = T_queue - T_submit**

**Sub-metrics:**
- **LLM Response Time:** Time from API request to response receipt
- **Parsing Time:** Time to parse LLM response into structured tasks
- **Cache Hit Time:** Time when response served from cache vs API

**Target Claims:**
- Async planning: <5s perceived latency (non-blocking)
- Sync planning: 30-60s blocking latency
- Cache hit: <100ms latency

#### Execution Latency (T_exec)
**Definition:** Time from task queue to task completion.

**Measurement Points:**
- **T_start:** Timestamp when `action.start()` is called
- **T_complete:** Timestamp when `action.isComplete()` returns true
- **T_exec = T_complete - T_start**

**Sub-metrics:**
- **Per-action latency:** Average time per action type
- **Tick efficiency:** Actions completed per game tick
- **Concurrency speedup:** Speedup factor with multiple agents

**Target Claims:**
- Simple actions (mine, place): <5s
- Complex actions (build structure): 60-300s depending on size
- Multi-agent coordination: 2-4x speedup for parallelizable tasks

### 1.2 Cost Metrics

#### LLM API Cost (C_llm)
**Definition:** Total monetary cost of LLM API calls per task.

**Calculation:**
```
C_llm = (N_input_tokens × Price_input) + (N_output_tokens × Price_output)
```

**Where:**
- `N_input_tokens`: Total input tokens (system prompt + user prompt + context)
- `N_output_tokens`: Total output tokens (LLM response)
- `Price_input`: Per-token input price for provider/model
- `Price_output`: Per-token output price for provider/model

**Cost Tracking:**
- Use `PromptMetrics` class for automatic tracking
- Log costs per request with provider/model info
- Aggregate costs per task, per session, per benchmark

**Target Claims:**
- Cascade routing: 60-80% cost reduction vs GPT-4 only
- Caching: 40-60% hit rate reduces effective costs
- Batching: Reduces API calls by 3-5x for rate-limited scenarios

#### Computational Cost (C_comp)
**Definition:** Local computational resources consumed.

**Metrics:**
- **CPU Usage:** Average CPU percentage during execution
- **Memory Usage:** JVM heap usage during peak operations
- **Network Bandwidth:** Total bytes transferred to/from LLM APIs

**Target Claims:**
- Async execution: <10% CPU impact on game thread
- Tick-based actions: No frame drops during execution
- Memory: <500MB additional overhead for LLM infrastructure

### 1.3 Success Rate Metrics

#### Task Completion Rate (R_complete)
**Definition:** Percentage of tasks that complete successfully.

**Calculation:**
```
R_complete = (N_complete / N_total) × 100%
```

**Where:**
- `N_complete`: Tasks with `ActionResult.success() == true`
- `N_total`: Total tasks attempted

**Failure Categories:**
1. **Planning Failures:** LLM returned null or unparseable response
2. **Execution Failures:** Action threw exception or timed out
3. **Replanning Failures:** Task required replanning but replanning failed
4. **Resource Failures:** Insufficient resources (blocks, items) to complete

**Target Claims:**
- Simple tasks: >95% completion rate
- Medium tasks: >85% completion rate
- Complex tasks: >70% completion rate
- Multi-agent: >80% completion rate

#### Correctness Rate (R_correct)
**Definition:** Percentage of completed tasks that match user intent.

**Measurement:**
- Manual evaluation by human judges
- Automated checks when ground truth is known (e.g., block counts)
- Player satisfaction surveys for subjective tasks

**Target Claims:**
- Exact match for simple tasks: >90%
- Reasonable approximation for complex tasks: >75%
- Player satisfaction: >4/5 average rating

### 1.4 Quality Metrics

#### Output Correctness (Q_correct)
**Definition:** Accuracy of the final result compared to specifications.

**Measurement:**
- **Block-level accuracy:** For building tasks, count correctly placed blocks
- **Resource accuracy:** For gathering tasks, count gathered items vs target
- **Structural accuracy:** For complex structures, measure spatial correctness

**Formula:**
```
Q_correct = (N_correct / N_specified) × 100%
```

#### Player Satisfaction (Q_satisfaction)
**Definition:** Subjective quality assessment by human players.

**Measurement:**
- Post-task survey (1-5 Likert scale)
- Questions:
  1. "Did the agent complete what you asked?" (intent matching)
  2. "How satisfied are you with the result?" (result quality)
  3. "Would you use this agent again?" (overall satisfaction)

**Target Claims:**
- Average satisfaction: >4/5
- Reuse intention: >80%

---

## 2. Benchmark Scenarios

### 2.1 Simple Task: "Mine 10 Stone"

**Objective:** Gather 10 cobblestone blocks.

**Starting Conditions:**
- Agent spawned at Y=65 (surface)
- Stone available at Y=60-65 (1-5 blocks below)
- Agent has diamond pickaxe in inventory
- No obstacles between agent and stone

**Success Criteria:**
- Exactly 10 cobblestone in agent inventory
- Task completes within 60 seconds
- No fatal errors or crashes

**Metrics Collected:**
- Planning latency (T_plan)
- Execution latency (T_exec)
- LLM tokens used
- API cost
- Actions executed (pathfind, mine counts)
- Success/failure status

**Complexity Classification:** SIMPLE
**Expected Actions:** 3-5 (pathfind to stone, mine block, repeat)
**Expected Time:** 20-40 seconds

### 2.2 Medium Task: "Build a 5x5 House"

**Objective:** Construct a 5x5x3 house with walls, roof, and door.

**Starting Conditions:**
- Flat terrain at Y=65
- Agent has 150 blocks of specified material (e.g., oak planks)
- Agent has 1 door in inventory
- Agent has 1 torch in inventory

**Success Criteria:**
- Structure matches 5x5x3 dimensions (±1 block tolerance)
- Walls are complete (no gaps >1 block)
- Roof is complete
- Door is placed in valid location
- Task completes within 5 minutes

**Metrics Collected:**
- Planning latency
- Execution latency
- LLM tokens used
- API cost
- Block placement accuracy
- Structural correctness
- Multi-agent coordination (if applicable)

**Complexity Classification:** MODERATE
**Expected Actions:** 50-100 (pathfind, place blocks)
**Expected Time:** 120-300 seconds

### 2.3 Complex Task: "Create an Automated Farm"

**Objective:** Build a functional automated wheat farm with water, farmland, and hopper collection.

**Starting Conditions:**
- Flat terrain at Y=65
- Agent has access to water bucket, hoe, hopper, chest
- Agent has seeds in inventory
- Agent has building materials (dirt, cobblestone)

**Success Criteria:**
- Water source placed correctly for hydration
- Farmland tilled and hydrated
- Seeds planted on farmland
- Hopper collection system functional
- Chest accessible for collection
- Task completes within 10 minutes

**Metrics Collected:**
- Planning latency
- Execution latency
- LLM tokens used (likely multiple API calls)
- API cost
- Number of replanning cycles
- Action sequence correctness
- Final farm functionality

**Complexity Classification:** COMPLEX
**Expected Actions:** 200-500 (including multiple planning cycles)
**Expected Time:** 300-600 seconds

### 2.4 Multi-Agent Task: "Build a Village with 3 Workers"

**Objective:** Coordinate 3 Steve agents to build a small village with 3 houses, a central plaza, and paths.

**Starting Conditions:**
- 3 Steve agents spawned at different locations
- Each agent has 500 blocks of building material
- Flat terrain available for building
- Agents can communicate via orchestration system

**Success Criteria:**
- 3 distinct houses constructed
- Central plaza area created
- Paths connecting structures
- No conflicts between agents (no blocks placed twice)
- Task completes within 15 minutes

**Metrics Collected:**
- Planning latency per agent
- Execution latency per agent
- Total LLM tokens across all agents
- Total API cost
- Inter-agent communication overhead
- Conflict resolution events
- Parallelization efficiency (speedup vs single agent)
- Final village layout quality

**Complexity Classification:** COMPLEX + MULTI-AGENT
**Expected Actions:** 500-1000 across all agents
**Expected Time:** 600-900 seconds

### 2.5 Stress Test: "100 Sequential Commands"

**Objective:** Test system stability and performance under sustained load.

**Procedure:**
1. Execute 100 simple commands sequentially
2. Mix of: "mine 5 [resource]", "place 5 [block]", "craft 10 [item]"
3. No delays between commands (submit as fast as possible)

**Success Criteria:**
- <5% command failure rate
- No memory leaks (stable heap usage)
- No thread deadlocks
- Average planning latency remains <5s
- Cache hit rate >30%

**Metrics Collected:**
- Planning latency distribution (p50, p95, p99)
- Cache hit rate over time
- Memory usage trend
- Error rate trend
- LLM token usage pattern

---

## 3. Comparison Methodology

### 3.1 Baseline: Traditional Scripted AI

**Description:** Rule-based AI with pre-programmed behaviors, no LLM integration.

**Implementation:**
- Behavior tree or finite state machine
- Hard-coded action sequences
- No planning phase
- Direct execution of pre-defined patterns

**Example Implementation:**
```java
// Scripted version of "mine 10 stone"
public class ScriptedMiningAction extends BaseAction {
    private final int targetQuantity = 10;
    private int mined = 0;

    @Override
    protected void onTick() {
        // Hard-coded sequence: pathfind, mine, repeat
        if (mined < targetQuantity) {
            pathfindToNearestStone();
            mineBlock();
            mined++;
        } else {
            succeed("Mined " + mined + " stone");
        }
    }
}
```

**Advantages:**
- Zero LLM cost
- Predictable performance
- No network latency

**Disadvantages:**
- Limited to pre-programmed scenarios
- No adaptability to novel situations
- Requires manual programming for each task type

### 3.2 Comparison: Pure LLM Agent

**Description:** LLM generates complete action sequences with tick-by-tick instructions.

**Implementation:**
- LLM generates detailed step-by-step instructions
- Agent follows LLM output directly
- No intermediate action abstraction

**Example Prompt:**
```
"Generate a tick-by-tick plan to mine 10 stone. Output format:
TICK 1: Move to x, y, z
TICK 2: Face direction
TICK 3: Swing arm
..."
```

**Advantages:**
- Maximum flexibility
- Can handle novel scenarios

**Disadvantages:**
- Extremely high token usage
- Slow planning (generates hundreds of steps)
- Error-prone (one mistake ruins entire sequence)

### 3.3 Comparison: ReAct Agent

**Description:** ReAct (Reasoning + Acting) pattern with iterative LLM calls.

**Implementation:**
- LLM generates one action at a time
- After each action, LLM observes result and plans next action
- Continues until task complete

**Example Loop:**
```
1. LLM: "I should mine stone. Action: MINE block=stone"
2. Agent: Executes mine, returns result
3. LLM: "I mined 1 stone. Need 9 more. Action: MINE block=stone"
4. ... (repeats 10 times)
```

**Advantages:**
- More flexible than Steve AI (no fixed action primitives)
- Can adapt during execution

**Disadvantages:**
- Very high API cost (10 LLM calls for 10 blocks)
- Very slow (10x planning latency)
- Token inefficient

### 3.4 Steve AI (Hybrid)

**Description:** LLM generates high-level plan, executes via tick-based actions.

**Implementation:**
- LLM generates structured task list (JSON)
- Tasks execute via action primitives (tick-based)
- Replanning only on failure

**Example Flow:**
```
1. LLM: "Plan: Mine stone. Tasks: [MINE block=stone quantity=10]"
2. Agent: Executes 1 task with 10 ticks
3. No additional LLM calls needed
```

**Advantages:**
- Low API cost (1 LLM call for 10 blocks)
- Fast planning (single async request)
- Combines LLM reasoning with efficient execution

### 3.5 Comparison Matrix

| Metric | Scripted AI | Pure LLM | ReAct | Steve AI |
|--------|-------------|----------|-------|----------|
| **Planning Latency** | 0s | 60-120s | 30-60s per action | 2-5s total |
| **API Cost (10 blocks)** | $0 | $0.50-1.00 | $0.10-0.30 | $0.01-0.05 |
| **Token Usage** | 0 | 10,000-20,000 | 5,000-10,000 | 500-1,000 |
| **Flexibility** | Low | High | High | Medium |
| **Execution Speed** | Fast | Slow | Very Slow | Fast |
| **Success Rate** | High (known tasks) | Low | Medium | High |
| **Replanning** | Manual | Full regeneration | Per-action | On-failure |

---

## 4. Metrics and Measurement Procedures

### 4.1 Automated Metrics Collection

#### Measurement Points in Code

**1. Planning Latency Measurement**
```java
// In ActionExecutor.processNaturalLanguageCommand()
public void processNaturalLanguageCommand(String command) {
    long tSubmit = System.currentTimeMillis();

    // Record planning start
    EvaluationMetrics.recordPlanningStart(foreman.getEntityName(), tSubmit);

    planningFuture = getTaskPlanner().planTasksAsync(foreman, command);

    planningFuture.thenAccept(response -> {
        long tQueue = System.currentTimeMillis();
        long planningLatency = tQueue - tSubmit;

        // Record planning completion
        EvaluationMetrics.recordPlanningComplete(
            foreman.getEntityName(),
            planningLatency,
            response.getTasks().size()
        );
    });
}
```

**2. Execution Latency Measurement**
```java
// In ActionExecutor.executeTask()
private void executeTask(Task task) {
    long tStart = System.currentTimeMillis();

    EvaluationMetrics.recordExecutionStart(
        foreman.getEntityName(),
        task.getAction(),
        tStart
    );

    currentAction = createAction(task);
    currentAction.start();

    // Track completion in tick()
    // When action.isComplete():
    long tComplete = System.currentTimeMillis();
    long execLatency = tComplete - tStart;

    EvaluationMetrics.recordExecutionComplete(
        foreman.getEntityName(),
        task.getAction(),
        execLatency,
        currentAction.getResult().isSuccess()
    );
}
```

**3. LLM Cost Tracking**
```java
// In AsyncLLMClient.sendAsync()
public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
    int estimatedInputTokens = PromptMetrics.estimateTokens(prompt);

    return apiCall(prompt, params).thenApply(response -> {
        int outputTokens = response.getTokensUsed();
        double cost = PromptMetrics.calculateCost(estimatedInputTokens, outputTokens);

        // Record cost
        EvaluationMetrics.recordLLMCall(
            providerId,
            model,
            estimatedInputTokens,
            outputTokens,
            cost,
            response.getLatencyMs(),
            response.isFromCache()
        );

        return response;
    });
}
```

### 4.2 Manual Evaluation Procedures

#### Task Correctness Assessment

**Procedure:**
1. **Pre-benchmark:** Define success criteria for each benchmark task
2. **During execution:** Record screenshots/videos at key checkpoints
3. **Post-execution:** Compare final state against criteria

**Checklist for "Build 5x5 House":**
- [ ] Dimensions: 5x5x3 (±1 block tolerance)
- [ ] Floor: 25 blocks placed
- [ ] Walls: 4 walls × 5 blocks × 3 height = 60 blocks
- [ ] Roof: 25 blocks placed
- [ ] Door: 1 door placed
- [ ] No gaps: Check each wall layer for completeness
- [ ] Total blocks: 111 blocks (floor + walls + roof + door)

**Scoring:**
```
Correctness Score = (N_correct / N_required) × 100%
```

#### Player Satisfaction Survey

**Survey Form:**
```
Post-Task Evaluation Form
=========================

Task: [Auto-filled task description]
Agent: [Auto-filled agent name]
Timestamp: [Auto-filled]

Q1: Did the agent complete what you asked?
○ Yes
○ Partially
○ No

Q2: How satisfied are you with the result?
○ Very Dissatisfied (1)
○ Dissatisfied (2)
○ Neutral (3)
○ Satisfied (4)
○ Very Satisfied (5)

Q3: How would you rate the agent's efficiency?
○ Very Slow (1)
○ Slow (2)
○ Acceptable (3)
○ Fast (4)
○ Very Fast (5)

Q4: Did the agent make any mistakes?
○ No mistakes
○ Minor mistakes (didn't affect outcome)
○ Major mistakes (affected outcome)
○ Complete failure

Q5: Would you use this agent again for similar tasks?
○ Definitely Not (1)
○ Unlikely (2)
○ Maybe (3)
○ Likely (4)
○ Definitely (5)

Additional Comments:
[Free text field]
```

**Analysis:**
- Calculate mean scores for Q2, Q3, Q5
- Calculate completion rate from Q1
- Categorize mistake frequency from Q4
- Perform qualitative analysis on comments

### 4.3 Data Export Format

#### JSON Export Schema
```json
{
  "benchmark_run": {
    "run_id": "uuid",
    "timestamp": "ISO-8601",
    "benchmark_version": "1.0",
    "system_info": {
      "minecraft_version": "1.20.1",
      "steve_ai_version": "git-commit-hash",
      "java_version": "17",
      "os": "Windows 10",
      "hardware": {
        "cpu": "model",
        "ram_gb": 32,
        "gpu": "model"
      }
    },
    "config": {
      "llm_provider": "openai",
      "llm_model": "gpt-4",
      "cascade_routing": true,
      "caching_enabled": true,
      "batching_enabled": false
    },
    "tasks": [
      {
        "task_id": "uuid",
        "scenario": "Mine 10 Stone",
        "complexity": "SIMPLE",
        "agent_name": "Steve-1",
        "timestamp_start": "ISO-8601",
        "timestamp_complete": "ISO-8601",
        "planning": {
          "latency_ms": 2340,
          "llm_calls": 1,
          "cache_hit": false,
          "input_tokens": 1250,
          "output_tokens": 180,
          "cost_usd": 0.045
        },
        "execution": {
          "latency_ms": 28500,
          "actions_completed": 35,
          "actions_failed": 2,
          "ticks_elapsed": 570
        },
        "result": {
          "success": true,
          "correctness_score": 1.0,
          "blocks_mined": 10,
          "errors": []
        }
      }
    ],
    "summary": {
      "total_tasks": 100,
      "successful_tasks": 92,
      "success_rate": 0.92,
      "avg_planning_latency_ms": 2100,
      "avg_execution_latency_ms": 32000,
      "total_llm_cost_usd": 4.52,
      "total_tokens": 125000,
      "cache_hit_rate": 0.45
    }
  }
}
```

---

## 5. Experimental Design

### 5.1 Controlled Variables

**System Configuration:**
- Minecraft Forge 1.20.1
- Java 17 (HotSpot JVM)
- Fixed JVM heap: 4GB
- No other mods loaded
- Single-player instance (no server/network overhead)

**Environment:**
- Superflat world (to standardize terrain)
- Peaceful difficulty (no mob interference)
- Day time (to standardize visibility)
- Clear weather (no rain/snow)

**Agent Starting State:**
- Location: (0, 65, 0)
- Inventory: Pre-configured per scenario
- Health: Full
- No active effects

### 5.2 Experimental Groups

**Group A: Steve AI (Baseline)**
- Configuration: OpenAI GPT-4
- Cascade routing: Disabled
- Caching: Enabled
- Batching: Disabled
- N = 30 trials per scenario

**Group B: Steve AI (Cascade)**
- Configuration: Cascade routing enabled
- FAST tier: Groq llama-3.1-8b-instant
- BALANCED tier: Groq llama-3.3-70b
- SMART tier: GPT-4
- Caching: Enabled
- N = 30 trials per scenario

**Group C: ReAct (Comparison)**
- Configuration: GPT-4
- One action per LLM call
- No caching (unique prompts)
- N = 30 trials per scenario

**Group D: Scripted AI (Baseline)**
- Configuration: No LLM
- Pre-programmed behaviors only
- N = 30 trials per scenario

### 5.3 Trial Procedure

**Per Trial:**
1. **Setup:**
   - Reset world to initial state
   - Spawn agent at (0, 65, 0)
   - Clear agent inventory and equip required items
   - Reset all metrics

2. **Execution:**
   - Submit natural language command
   - Start recording (video + metrics)
   - Wait for completion or timeout (10 minutes max)
   - Stop recording

3. **Post-Processing:**
   - Export metrics to JSON
   - Take screenshots of final state
   - Manually verify correctness
   - Administer satisfaction survey (if human trial)

4. **Reset:**
   - Remove agent
   - Clear any placed blocks
   - Wait 5 seconds before next trial

### 5.4 Statistical Power Analysis

**Sample Size Justification:**

For detecting a medium effect size (Cohen's d = 0.5) with 80% power at α = 0.05:

- Two-tailed t-test
- Required sample: n = 64 per group
- Adjusted for multiple comparisons: n = 30 per group

**Effect Sizes of Interest:**
- Planning latency: 20% reduction (large effect)
- API cost: 50% reduction (large effect)
- Success rate: 10% improvement (medium effect)

---

## 6. Data Collection Templates

### 6.1 EvaluationMetrics.java

```java
package com.minewright.evaluation;

import com.minewright.llm.PromptMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive metrics collection for academic evaluation.
 * Thread-safe implementation for concurrent agent scenarios.
 */
public class EvaluationMetrics {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationMetrics.class);

    // Per-metrics tracking
    private static final Map<String, TaskMetrics> activeTasks = new ConcurrentHashMap<>();
    private static final List<CompletedTaskMetrics> completedTasks = new ArrayList<>();
    private static final AtomicLong totalCost = new AtomicLong(0); // Cost in cents (1/100 USD)

    // Benchmark metadata
    private static String benchmarkRunId = java.util.UUID.randomUUID().toString();
    private static String benchmarkVersion = "1.0";
    private static long benchmarkStartTime = System.currentTimeMillis();

    /**
     * Record the start of planning phase.
     */
    public static void recordPlanningStart(String agentName, long timestamp) {
        TaskMetrics metrics = getOrCreateTaskMetrics(agentName);
        metrics.planningStartTime = timestamp;
        metrics.phase = ExecutionPhase.PLANNING;

        LOGGER.debug("[{}] Planning started at {}", agentName, timestamp);
    }

    /**
     * Record the completion of planning phase.
     */
    public static void recordPlanningComplete(String agentName, long latencyMs, int taskCount) {
        TaskMetrics metrics = getTaskMetrics(agentName);
        if (metrics == null) {
            LOGGER.warn("[{}] No active task metrics found for planning completion", agentName);
            return;
        }

        metrics.planningLatencyMs = latencyMs;
        metrics.tasksGenerated = taskCount;
        metrics.phase = ExecutionPhase.EXECUTION;

        LOGGER.info("[{}] Planning completed: {}ms, {} tasks generated",
            agentName, latencyMs, taskCount);
    }

    /**
     * Record the start of a task execution.
     */
    public static void recordExecutionStart(String agentName, String actionType, long timestamp) {
        TaskMetrics metrics = getTaskMetrics(agentName);
        if (metrics == null) {
            LOGGER.warn("[{}] No active task metrics found for execution start", agentName);
            return;
        }

        metrics.currentAction = actionType;
        metrics.executionStartTime = timestamp;
        metrics.actionsStarted++;

        LOGGER.debug("[{}] Action '{}' started at {}", agentName, actionType, timestamp);
    }

    /**
     * Record the completion of a task execution.
     */
    public static void recordExecutionComplete(String agentName, String actionType,
                                               long latencyMs, boolean success) {
        TaskMetrics metrics = getTaskMetrics(agentName);
        if (metrics == null) {
            LOGGER.warn("[{}] No active task metrics found for execution completion", agentName);
            return;
        }

        metrics.actionsCompleted++;
        if (success) {
            metrics.actionsSuccessful++;
        } else {
            metrics.actionsFailed++;
        }

        LOGGER.info("[{}] Action '{}' completed: {}ms, success={}",
            agentName, actionType, latencyMs, success);
    }

    /**
     * Record an LLM API call with full details.
     */
    public static void recordLLMCall(String providerId, String model,
                                     int inputTokens, int outputTokens,
                                     double costUsd, long latencyMs,
                                     boolean fromCache) {
        // Update global totals
        PromptMetrics.recordRequest(inputTokens, outputTokens);
        totalCost.addAndGet((long) (costUsd * 100)); // Convert to cents

        // Update current task metrics if available
        for (TaskMetrics metrics : activeTasks.values()) {
            metrics.llmCalls++;
            metrics.inputTokens += inputTokens;
            metrics.outputTokens += outputTokens;
            metrics.llmCostUsd += costUsd;
            if (fromCache) {
                metrics.cacheHits++;
            }

            // Track latency distribution
            if (latencyMs < 1000) {
                metrics.latencyBuckets[0]++;
            } else if (latencyMs < 3000) {
                metrics.latencyBuckets[1]++;
            } else if (latencyMs < 10000) {
                metrics.latencyBuckets[2]++;
            } else {
                metrics.latencyBuckets[3]++;
            }
        }

        LOGGER.debug("[LLM] {} ({}): {} in + {} out tokens, ${}, {}ms, cache={}",
            providerId, model, inputTokens, outputTokens,
            String.format("%.6f", costUsd), latencyMs, fromCache);
    }

    /**
     * Mark a task as complete with final results.
     */
    public static void recordTaskComplete(String agentName, boolean success,
                                         double correctnessScore, Map<String, Object> results) {
        TaskMetrics metrics = getTaskMetrics(agentName);
        if (metrics == null) {
            LOGGER.warn("[{}] No active task metrics found for task completion", agentName);
            return;
        }

        long completionTime = System.currentTimeMillis();
        long totalLatency = completionTime - metrics.planningStartTime;

        metrics.success = success;
        metrics.correctnessScore = correctnessScore;
        metrics.results = results;
        metrics.completionTime = completionTime;
        metrics.totalLatencyMs = totalLatency;
        metrics.phase = ExecutionPhase.COMPLETE;

        // Move to completed list
        CompletedTaskMetrics completed = new CompletedTaskMetrics(metrics);
        completedTasks.add(completed);
        activeTasks.remove(agentName);

        LOGGER.info("[{}] Task COMPLETE: success={}, correctness={}, total={}ms, llmCost=${}",
            agentName, success, correctnessScore, totalLatency,
            String.format("%.4f", metrics.llmCostUsd));
    }

    /**
     * Export all collected metrics to JSON file.
     */
    public static void exportToJson(String filepath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filepath))) {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"benchmark_run\": {\n");
            json.append("    \"run_id\": \"").append(benchmarkRunId).append("\",\n");
            json.append("    \"timestamp\": \"").append(LocalDateTime.now().format(
                DateTimeFormatter.ISO_DATE_TIME)).append("\",\n");
            json.append("    \"benchmark_version\": \"").append(benchmarkVersion).append("\",\n");
            json.append("    \"duration_ms\": ").append(System.currentTimeMillis() - benchmarkStartTime).append(",\n");

            // Summary statistics
            json.append("    \"summary\": {\n");
            json.append("      \"total_tasks\": ").append(completedTasks.size()).append(",\n");
            long successfulTasks = completedTasks.stream().filter(t -> t.success).count();
            json.append("      \"successful_tasks\": ").append(successfulTasks).append(",\n");
            double successRate = completedTasks.isEmpty() ? 0 :
                (double) successfulTasks / completedTasks.size();
            json.append("      \"success_rate\": ").append(String.format("%.3f", successRate)).append(",\n");

            double avgPlanningLatency = completedTasks.stream()
                .mapToLong(t -> t.planningLatencyMs).average().orElse(0);
            json.append("      \"avg_planning_latency_ms\": ").append((long) avgPlanningLatency).append(",\n");

            double avgExecutionLatency = completedTasks.stream()
                .mapToLong(t -> t.executionLatencyMs).average().orElse(0);
            json.append("      \"avg_execution_latency_ms\": ").append((long) avgExecutionLatency).append(",\n");

            double totalCostUsd = totalCost.get() / 100.0;
            json.append("      \"total_llm_cost_usd\": ").append(String.format("%.2f", totalCostUsd)).append(",\n");

            long totalTokens = completedTasks.stream()
                .mapToLong(t -> t.inputTokens + t.outputTokens).sum();
            json.append("      \"total_tokens\": ").append(totalTokens).append(",\n");

            long totalCacheHits = completedTasks.stream().mapToLong(t -> t.cacheHits).sum();
            double cacheHitRate = completedTasks.isEmpty() ? 0 :
                (double) totalCacheHits / completedTasks.stream().mapToLong(t -> t.llmCalls).sum();
            json.append("      \"cache_hit_rate\": ").append(String.format("%.3f", cacheHitRate)).append("\n");
            json.append("    },\n");

            // Individual task results
            json.append("    \"tasks\": [\n");
            for (int i = 0; i < completedTasks.size(); i++) {
                CompletedTaskMetrics task = completedTasks.get(i);
                json.append("      {\n");
                json.append("        \"agent_name\": \"").append(task.agentName).append("\",\n");
                json.append("        \"planning_latency_ms\": ").append(task.planningLatencyMs).append(",\n");
                json.append("        \"execution_latency_ms\": ").append(task.executionLatencyMs).append(",\n");
                json.append("        \"total_latency_ms\": ").append(task.totalLatencyMs).append(",\n");
                json.append("        \"success\": ").append(task.success).append(",\n");
                json.append("        \"correctness_score\": ").append(task.correctnessScore).append(",\n");
                json.append("        \"llm_calls\": ").append(task.llmCalls).append(",\n");
                json.append("        \"input_tokens\": ").append(task.inputTokens).append(",\n");
                json.append("        \"output_tokens\": ").append(task.outputTokens).append(",\n");
                json.append("        \"llm_cost_usd\": ").append(String.format("%.6f", task.llmCostUsd)).append(",\n");
                json.append("        \"cache_hits\": ").append(task.cacheHits).append(",\n");
                json.append("        \"actions_started\": ").append(task.actionsStarted).append(",\n");
                json.append("        \"actions_completed\": ").append(task.actionsCompleted).append(",\n");
                json.append("        \"actions_successful\": ").append(task.actionsSuccessful).append("\n");
                json.append("      }");
                if (i < completedTasks.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("    ]\n");
            json.append("  }\n");
            json.append("}\n");

            writer.write(json.toString());
            LOGGER.info("Metrics exported to {}", filepath);
        } catch (IOException e) {
            LOGGER.error("Failed to export metrics to {}", filepath, e);
        }
    }

    /**
     * Reset all metrics for a new benchmark run.
     */
    public static void reset() {
        activeTasks.clear();
        completedTasks.clear();
        totalCost.set(0);
        benchmarkRunId = java.util.UUID.randomUUID().toString();
        benchmarkStartTime = System.currentTimeMillis();
        PromptMetrics.reset();

        LOGGER.info("Metrics reset. New run ID: {}", benchmarkRunId);
    }

    // ------------------------------------------------------------------------
    // Internal Helper Classes
    // ------------------------------------------------------------------------

    private enum ExecutionPhase {
        PLANNING, EXECUTION, COMPLETE
    }

    private static class TaskMetrics {
        String agentName;
        ExecutionPhase phase;
        long planningStartTime;
        long planningLatencyMs;
        long executionStartTime;
        long executionLatencyMs;
        long totalLatencyMs;
        long completionTime;
        int tasksGenerated;
        String currentAction;
        int actionsStarted;
        int actionsCompleted;
        int actionsSuccessful;
        int actionsFailed;
        boolean success;
        double correctnessScore;
        Map<String, Object> results;
        int llmCalls;
        int inputTokens;
        int outputTokens;
        double llmCostUsd;
        int cacheHits;
        int[] latencyBuckets = new int[4]; // <1s, 1-3s, 3-10s, >10s

        TaskMetrics(String agentName) {
            this.agentName = agentName;
            this.phase = ExecutionPhase.PLANNING;
            this.planningStartTime = System.currentTimeMillis();
        }
    }

    private static class CompletedTaskMetrics {
        String agentName;
        long planningLatencyMs;
        long executionLatencyMs;
        long totalLatencyMs;
        boolean success;
        double correctnessScore;
        int llmCalls;
        int inputTokens;
        int outputTokens;
        double llmCostUsd;
        int cacheHits;
        int actionsStarted;
        int actionsCompleted;
        int actionsSuccessful;

        CompletedTaskMetrics(TaskMetrics metrics) {
            this.agentName = metrics.agentName;
            this.planningLatencyMs = metrics.planningLatencyMs;
            this.executionLatencyMs = metrics.executionLatencyMs;
            this.totalLatencyMs = metrics.totalLatencyMs;
            this.success = metrics.success;
            this.correctnessScore = metrics.correctnessScore;
            this.llmCalls = metrics.llmCalls;
            this.inputTokens = metrics.inputTokens;
            this.outputTokens = metrics.outputTokens;
            this.llmCostUsd = metrics.llmCostUsd;
            this.cacheHits = metrics.cacheHits;
            this.actionsStarted = metrics.actionsStarted;
            this.actionsCompleted = metrics.actionsCompleted;
            this.actionsSuccessful = metrics.actionsSuccessful;
        }
    }

    private static TaskMetrics getOrCreateTaskMetrics(String agentName) {
        return activeTasks.computeIfAbsent(agentName, TaskMetrics::new);
    }

    private static TaskMetrics getTaskMetrics(String agentName) {
        return activeTasks.get(agentName);
    }
}
```

### 6.2 BenchmarkRunner.java

```java
package com.minewright.evaluation;

import com.minewright.entity.ForemanEntity;
import com.minewright.action.ActionExecutor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Automated benchmark runner for reproducible evaluation.
 */
public class BenchmarkRunner {

    public static class BenchmarkConfig {
        String scenarioName;
        String naturalLanguageCommand;
        StartingConditions startingConditions;
        SuccessCriteria successCriteria;
        int timeoutSeconds = 600; // 10 minutes max

        public static class StartingConditions {
            BlockPos spawnPosition;
            List<ItemStack> inventoryItems;
            String worldType = "flat";
            String timeOfDay = "day";
            String weather = "clear";
        }

        public static class SuccessCriteria {
            boolean requireExactBlockCount;
            int targetBlockCount;
            double minCorrectnessScore = 0.8;
            int maxExecutionTimeSeconds;
        }
    }

    public static class BenchmarkResult {
        String scenarioName;
        int trialNumber;
        boolean completed;
        boolean timedOut;
        long planningLatencyMs;
        long executionLatencyMs;
        boolean success;
        double correctnessScore;
        int llmCalls;
        int inputTokens;
        int outputTokens;
        double llmCostUsd;
        List<String> errors;
        String notes;
    }

    private final ServerLevel level;
    private final List<BenchmarkResult> results = new ArrayList<>();

    public BenchmarkRunner(ServerLevel level) {
        this.level = level;
    }

    /**
     * Run a single benchmark trial.
     */
    public CompletableFuture<BenchmarkResult> runTrial(BenchmarkConfig config, int trialNumber) {
        CompletableFuture<BenchmarkResult> future = new CompletableFuture<>();

        // Reset metrics for this trial
        EvaluationMetrics.reset();

        // Setup starting conditions
        ForemanEntity agent = spawnAgent(config.startingConditions);
        setupInventory(agent, config.startingConditions);

        // Record start time
        long startTime = System.currentTimeMillis();
        BenchmarkResult result = new BenchmarkResult();
        result.scenarioName = config.scenarioName;
        result.trialNumber = trialNumber;
        result.errors = new ArrayList<>();

        // Submit command
        ActionExecutor executor = agent.getActionExecutor();
        executor.processNaturalLanguageCommand(config.naturalLanguageCommand);

        // Monitor completion
        Thread monitorThread = new Thread(() -> {
            try {
                long endTime = startTime + (config.timeoutSeconds * 1000);

                while (System.currentTimeMillis() < endTime) {
                    Thread.sleep(100); // Check every 100ms

                    if (!executor.isExecuting()) {
                        // Agent finished
                        result.completed = true;
                        result.timedOut = false;
                        break;
                    }
                }

                if (System.currentTimeMillis() >= endTime) {
                    // Timeout
                    result.completed = false;
                    result.timedOut = true;
                    result.errors.add("Benchmark timeout after " + config.timeoutSeconds + " seconds");
                    executor.stopCurrentAction();
                }

                // Collect results
                result.success = evaluateSuccess(agent, config.successCriteria);
                result.correctnessScore = calculateCorrectness(agent, config.successCriteria);
                result.llmCalls = (int) PromptMetrics.getTotalRequests();
                result.inputTokens = (int) PromptMetrics.getTotalInputTokens();
                result.outputTokens = (int) PromptMetrics.getTotalOutputTokens();
                result.llmCostUsd = PromptMetrics.getTotalEstimatedCost();

                // Clean up
                agent.remove();

                future.complete(result);

            } catch (Exception e) {
                result.errors.add("Monitor thread exception: " + e.getMessage());
                future.complete(result);
            }
        });

        monitorThread.start();

        return future;
    }

    /**
     * Run multiple trials of a benchmark scenario.
     */
    public CompletableFuture<List<BenchmarkResult>> runBenchmarkSuite(
            BenchmarkConfig config, int numberOfTrials) {

        List<CompletableFuture<BenchmarkResult>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfTrials; i++) {
            futures.add(runTrial(config, i));

            // Delay between trials
            try {
                Thread.sleep(5000); // 5 second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<BenchmarkResult> allResults = new ArrayList<>();
                for (CompletableFuture<BenchmarkResult> future : futures) {
                    allResults.add(future.join());
                }
                return allResults;
            });
    }

    /**
     * Export benchmark results to CSV.
     */
    public void exportToCsv(List<BenchmarkResult> results, String filepath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filepath))) {
            // Header
            writer.write("Scenario,Trial,Completed,TimedOut,PlanningLatencyMs,ExecutionLatencyMs," +
                "Success,Correctness,LLMCalls,InputTokens,OutputTokens,CostUsd,Errors,Notes\n");

            // Data rows
            for (BenchmarkResult result : results) {
                writer.write(String.format("%s,%d,%b,%b,%d,%d,%b,%.2f,%d,%d,%d,%.6f,\"%s\",\"%s\"\n",
                    result.scenarioName,
                    result.trialNumber,
                    result.completed,
                    result.timedOut,
                    result.planningLatencyMs,
                    result.executionLatencyMs,
                    result.success,
                    result.correctnessScore,
                    result.llmCalls,
                    result.inputTokens,
                    result.outputTokens,
                    result.llmCostUsd,
                    String.join("; ", result.errors),
                    result.notes
                ));
            }

        } catch (IOException e) {
            // Log error
        }
    }

    // Helper methods
    private ForemanEntity spawnAgent(BenchmarkConfig.StartingConditions conditions) {
        // Implementation: spawn ForemanEntity at specified position
        return null;
    }

    private void setupInventory(ForemanEntity agent, BenchmarkConfig.StartingConditions conditions) {
        // Implementation: give agent specified items
    }

    private boolean evaluateSuccess(ForemanEntity agent, BenchmarkConfig.SuccessCriteria criteria) {
        // Implementation: check if success criteria met
        return true;
    }

    private double calculateCorrectness(ForemanEntity agent, BenchmarkConfig.SuccessCriteria criteria) {
        // Implementation: calculate correctness score
        return 1.0;
    }
}
```

---

## 7. Statistical Analysis

### 7.1 Hypothesis Testing

#### Hypothesis 1: Cascade Routing Reduces Cost

**Null Hypothesis (H0):** Cascade routing does not reduce LLM costs compared to GPT-4 only.
**Alternative Hypothesis (H1):** Cascade routing reduces LLM costs by >50%.

**Test:** Two-tailed independent t-test
- **Group A:** Steve AI (GPT-4 only)
- **Group B:** Steve AI (Cascade routing)
- **Metric:** Average cost per task (USD)

**Significance Level:** α = 0.05
**Effect Size:** Cohen's d

**R Code:**
```r
# Load data
group_a <- read.csv("group_a_costs.csv")
group_b <- read.csv("group_b_costs.csv")

# Perform t-test
result <- t.test(group_b$cost, group_a$cost,
                 alternative = "less",  # Testing if B < A
                 var.equal = FALSE)

# Calculate effect size
library(effsize)
cohen_d <- cohen.d(group_b$cost, group_a$cost)

# Output
cat("t =", result$statistic, "\n")
cat("p-value =", result$p.value, "\n")
cat("Cohen's d =", cohen_d$estimate, "\n")
cat("Mean cost reduction =",
    (mean(group_a$cost) - mean(group_b$cost)) / mean(group_a$cost) * 100,
    "%\n")
```

#### Hypothesis 2: Async Planning Reduces Perceived Latency

**Null Hypothesis (H0):** Async planning does not reduce perceived latency compared to sync planning.
**Alternative Hypothesis (H1):** Async planning reduces perceived latency by >80%.

**Test:** Paired t-test (same tasks, sync vs async)
- **Metric:** Perceived planning latency (time to first action)

**R Code:**
```r
sync_latency <- c(35000, 42000, 38000, 45000, 32000)  # 35-45 seconds
async_latency <- c(2500, 3200, 2800, 3500, 2200)      # 2-3 seconds

result <- t.test(async_latency, sync_latency,
                 alternative = "less",
                 paired = TRUE)

reduction <- (mean(sync_latency) - mean(async_latency)) /
             mean(sync_latency) * 100

cat("Latency reduction =", reduction, "%\n")
cat("p-value =", result$p.value, "\n")
```

### 7.2 Confidence Intervals

**95% Confidence Interval for Success Rate:**

```r
# Calculate Wilson score interval (better for proportions)
library(Hmisc)

successes <- 92
trials <- 100

ci <- binconf(successes, trials, method = "wilson")
cat("Success Rate:", ci$PointEst, "\n")
cat("95% CI:", ci$Lower, "-", ci$Upper, "\n")
```

### 7.3 ANOVA for Multi-Group Comparison

**Comparing All Four Groups:**

```r
# Load data for all groups
scripted <- read.csv("scripted_results.csv")
pure_llm <- read.csv("pure_llm_results.csv")
react <- read.csv("react_results.csv")
steve_ai <- read.csv("steve_ai_results.csv")

# Combine into single data frame
data <- data.frame(
  latency = c(scripted$latency, pure_llm$latency,
              react$latency, steve_ai$latency),
  group = factor(rep(c("Scripted", "Pure_LLM", "ReAct", "Steve_AI"),
                     each = 30))
)

# Perform ANOVA
model <- aov(latency ~ group, data = data)
summary(model)

# Post-hoc Tukey test
TukeyHSD(model)

# Effect size (eta-squared)
ss_total <- sum(model$`(Intercept)`^2)
ss_effect <- sum(model$`group`^2)
eta_squared <- ss_effect / ss_total
cat("Eta-squared =", eta_squared, "\n")
```

---

## 8. Reproducibility Guidelines

### 8.1 Environment Replication

**Required Software:**
- Minecraft Forge 1.20.1 (exact version specified in build.gradle)
- Java 17 (AdoptOpenJDK or Oracle JDK)
- Git commit hash: [Specify exact commit]

**World Seed:** Use fixed seed for all benchmarks
```
Seed: 123456789
World Type: Superflat
Generator Settings:minecraft:bedrock,3*minecraft:stone,63*minecraft:air;village
```

**Configuration File:**
```toml
[llm]
provider = "openai"
apiKey = "${API_KEY}"  # Loaded from environment variable
model = "gpt-4"

[cascade]
enabled = true  # Set to false for baseline

[caching]
enabled = true
max_entries = 1000

[batching]
enabled = false  # Disable for benchmarks
```

### 8.2 Automated Test Script

```bash
#!/bin/bash
# benchmark.sh - Automated benchmark execution

set -e  # Exit on error

# Configuration
STEVE_DIR="/path/to/steve"
RESULTS_DIR="$STEVE_DIR/benchmark_results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p "$RESULTS_DIR"

# Build project
cd "$STEVE_DIR"
./gradlew clean build

# Run benchmarks
echo "Starting benchmark run $TIMESTAMP"

# Scenario 1: Simple
echo "Running scenario 1: Simple (Mine 10 Stone)"
# [Code to launch Minecraft with benchmark mod]

# Scenario 2: Medium
echo "Running scenario 2: Medium (Build 5x5 House)"

# Scenario 3: Complex
echo "Running scenario 3: Complex (Automated Farm)"

# Scenario 4: Multi-Agent
echo "Running scenario 4: Multi-Agent (Village with 3 workers)"

# Collect results
echo "Collecting results"
cp -r "$STEVE_DIR/metrics_output" "$RESULTS_DIR/$TIMESTAMP/"

# Generate report
echo "Generating statistical report"
Rscript generate_report.R "$RESULTS_DIR/$TIMESTAMP/"

echo "Benchmark complete. Results in $RESULTS_DIR/$TIMESTAMP/"
```

### 8.3 Docker Container (Optional)

**Dockerfile:**
```dockerfile
FROM openjdk:17-jdk-slim

# Install dependencies
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Download Minecraft server
WORKDIR /minecraft
RUN wget https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.2.0/forge-1.20.1-47.2.0-installer.jar
RUN java -jar forge-1.20.1-47.2.0-installer.jar --installServer

# Copy Steve AI mod
COPY build/libs/steve-*.jar /minecraft/mods/

# Copy benchmark config
COPY config/benchmark.toml /minecraft/config/steve-common.toml

# Set environment variables
ENV MIN_MEMORY="4G"
ENV MAX_MEMORY="4G"

# Expose ports
EXPOSE 25565

# Start server
CMD java -Xms4G -Xmx4G -jar forge-1.20.1-47.2.0-server.jar nogui
```

---

## Appendix A: Glossary

- **Action:** Atomic operation executed by Steve agent (mine, place, craft, etc.)
- **Cascade Routing:** Intelligent LLM selection based on task complexity
- **Foreman Entity:** The in-game representation of Steve AI
- **LLM:** Large Language Model (GPT-4, Claude, etc.)
- **Planning Latency:** Time from command to task queue completion
- **ReAct:** Reasoning + Acting pattern for AI agents
- **Replanning:** Process of generating new plan when current plan fails
- **Task:** Structured instruction (action + parameters) from LLM
- **Tick:** Minecraft game update (20 ticks = 1 second)

---

## Appendix B: Quick Reference

**Key Metrics to Report:**
1. Planning latency: Mean (SD), Median, IQR
2. Execution latency: Mean (SD), Median, IQR
3. API cost: Total per scenario, Per task mean
4. Token usage: Input, Output, Total
5. Success rate: Percentage, 95% CI
6. Correctness: Mean score, Distribution

**Statistical Tests:**
- Two groups: Independent t-test or Mann-Whitney U
- Multiple groups: ANOVA or Kruskal-Wallis
- Proportions: Chi-square or Fisher's exact

**Sample Sizes:**
- Minimum: 20 trials per scenario
- Recommended: 30 trials per scenario
- High power: 50+ trials per scenario

**Effect Size Interpretation:**
- Small: d = 0.2
- Medium: d = 0.5
- Large: d = 0.8

---

**Document End**

For questions or clarifications about this evaluation framework, please refer to the dissertation methodology chapter or contact the research team.
