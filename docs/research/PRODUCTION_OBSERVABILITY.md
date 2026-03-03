# Production Observability Strategy for Steve AI

**Project:** Steve AI - "Cursor for Minecraft"
**Date:** March 3, 2026
**Version:** 1.0
**Status:** Design Document

---

## Executive Summary

This document defines the comprehensive observability strategy for Steve AI in production environments. As a complex multi-agent system with LLM integration, real-time game execution, and autonomous learning, Steve AI requires sophisticated monitoring to ensure reliability, performance, and cost-effectiveness.

**Key Components:**
- Distributed tracing across agent interactions
- Real-time metrics collection and dashboards
- LLM cost and performance tracking
- Skill library analytics
- Multi-agent coordination observability
- Integration with LangSmith and OpenTelemetry

**Target Metrics:**
- 99.5% uptime for agent services
- < 2s average planning latency
- < $0.10 per task in LLM costs
- 90%+ task success rate
- < 5% agent idle time

---

## Table of Contents

1. [Observability Architecture](#1-observability-architecture)
2. [Tracing Strategy](#2-tracing-strategy)
3. [Metrics Collection](#3-metrics-collection)
4. [Dashboard Design](#4-dashboard-design)
5. [LLM Observability](#5-llm-observability)
6. [Agent Coordination Tracking](#6-agent-coordination-tracking)
7. [Skill Analytics](#7-skill-analytics)
8. [Integration Patterns](#8-integration-patterns)
9. [Alert Configuration](#9-alert-configuration)
10. [Implementation Roadmap](#10-implementation-roadmap)

---

## 1. Observability Architecture

### 1.1 Overall Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    OBSERVABILITY STACK                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  APPLICATION LAYER                                      │   │
│  │  ├─ Agent State Machines                               │   │
│  │  ├─ Skill Execution                                    │   │
│  │  ├─ Multi-Agent Coordination                           │   │
│  │  └─ LLM Integration                                    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                             ↓                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  INSTRUMENTATION LAYER                                  │   │
│  │  ├─ OpenTelemetry Tracing                              │   │
│  │  ├─ Micrometer Metrics                                 │   │
│  │  ├─ Structured Logging                                 │   │
│  │  └─ Event Publishing                                   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                             ↓                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  COLLECTION LAYER                                       │   │
│  │  ├─ OpenTelemetry Collector                            │   │
│  │  ├─ Prometheus Metrics Server                          │   │
│  │  ├─ Loki/ELK Log Aggregation                          │   │
│  │  └─ LangSmith Tracing Backend                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                             ↓                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  VISUALIZATION LAYER                                     │   │
│  │  ├─ Grafana Dashboards                                 │   │
│  │  ├─ LangSmith UI                                       │   │
│  │  ├─ Kibana Log Search                                  │   │
│  │  └─ Custom Alerting                                    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 Data Flow

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Steve AI   │────▶│  Collector   │────▶│   Storage    │
│   Agents     │     │  (OTel)      │     │  (Prometheus) │
└──────────────┘     └──────────────┘     └──────────────┘
       │                    │                     │
       │                    ▼                     ▼
       │            ┌──────────────┐     ┌──────────────┐
       └───────────▶│  LangSmith   │     │   Grafana    │
                    │  (Traces)    │     │ (Dashboards) │
                    └──────────────┘     └──────────────┘
```

### 1.3 Component Responsibilities

| Component | Purpose | Technology |
|-----------|---------|------------|
| **Tracing** | Request flow across services | OpenTelemetry + LangSmith |
| **Metrics** | Numerical time-series data | Micrometer + Prometheus |
| **Logging** | Structured event records | SLF4J + Loki |
| **Events** | Domain-specific notifications | Event Bus |
| **Dashboards** | Real-time visualization | Grafana |
| **Alerting** | Threshold-based notifications | AlertManager |

---

## 2. Tracing Strategy

### 2.1 Distributed Tracing Overview

Steve AI uses distributed tracing to follow requests through the multi-agent system:

```
┌─────────────────────────────────────────────────────────────────┐
│                    TRACE HIERARCHY                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  TRACE: User Command "Build a house"                           │
│  │                                                              │
│  ├─ SPAN 1: Task Planning (LLM Call)                          │
│  │  ├─ Tag: model = "gpt-4"                                   │
│  │  ├─ Tag: tokens_in = 150                                   │
│  │  ├─ Tag: tokens_out = 800                                  │
│  │  └─ Event: tasks_generated = 5                             │
│  │                                                              │
│  ├─ SPAN 2: Contract Net Negotiation                          │
│  │  ├─ SPAN 2.1: Task Announcement                            │
│  │  ├─ SPAN 2.2: Bid Collection (3 bids)                      │
│  │  └─ SPAN 2.3: Award Selection                              │
│  │                                                              │
│  ├─ SPAN 3: Agent Execution (Steve-1)                         │
│  │  ├─ SPAN 3.1: Skill Lookup                                │
│  │  ├─ SPAN 3.2: Skill Execution                              │
│  │  └─ SPAN 3.3: Result Validation                            │
│  │                                                              │
│  ├─ SPAN 4: Agent Execution (Steve-2)                         │
│  │  └─ ...                                                     │
│  │                                                              │
│  └─ SPAN 5: Result Aggregation                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Span Attributes

Standard attributes for all spans:

```java
// Common attributes
put("service.name", "steve-ai")
put("service.version", "1.0.0")
put("agent.id", agentId.toString())
put("agent.name", agentName)
put("task.id", taskId)
put("task.type", taskType)

// LLM-specific attributes
put("llm.provider", "openai")
put("llm.model", "gpt-4")
put("llm.tokens.in", inputTokens)
put("llm.tokens.out", outputTokens)
put("llm.cost.usd", cost)
put("llm.latency.ms", latency)

// Skill-specific attributes
put("skill.name", skillName)
put("skill.category", category)
put("skill.execution_time.ms", executionTime)
put("skill.success", success)
```

### 2.3 Trace Context Propagation

```java
public class TracingUtils {
    private static final Tracer tracer = OpenTelemetry.getGlobalTracer("steve-ai");

    public static Span startSpan(String name, Map<String, String> attributes) {
        SpanBuilder builder = tracer.spanBuilder(name)
            .setSpanKind(SpanKind.INTERNAL);

        attributes.forEach(builder::setAttribute);
        return builder.startSpan();
    }

    public static void propagateContext(Context context, Runnable task) {
        try (Scope scope = context.makeCurrent()) {
            task.run();
        }
    }
}
```

### 2.4 OpenTelemetry Integration

```java
// In build.gradle
implementation 'io.opentelemetry:opentelemetry-api:1.32.0'
implementation 'io.opentelemetry:opentelemetry-sdk:1.32.0'
implementation 'io.opentelemetry:opentelemetry-exporter-otlp:1.32.0'

// Configuration
OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
    .setTracerProvider(
        SdkTracerProvider.builder()
            .addSpanProcessor(
                BatchSpanProcessor.builder(
                    OtlpGrpcSpanExporter.builder()
                        .setEndpoint("http://otel-collector:4317")
                        .build()
                ).build()
            )
            .build()
    )
    .build();
```

---

## 3. Metrics Collection

### 3.1 Metric Categories

Steve AI tracks five categories of metrics:

| Category | Purpose | Examples |
|----------|---------|----------|
| **Agent Metrics** | Agent state and performance | uptime, idle_time, tasks_completed |
| **LLM Metrics** | LLM usage and costs | tokens_used, cost_usd, latency_ms |
| **Skill Metrics** | Skill execution stats | executions, success_rate, avg_duration |
| **Coordination Metrics** | Multi-agent coordination | negotiations_started, bids_received, awards_granted |
| **System Metrics** | Resource utilization | cpu_usage, memory_usage, thread_count |

### 3.2 Key Metrics Definitions

```java
// Agent Metrics
private final Counter tasksCompleted = Counter.builder("agent.tasks.completed")
    .description("Total tasks completed by agent")
    .tag("agent", "unknown")
    .register(meterRegistry);

private final Gauge agentLoad = Gauge.builder("agent.load", agent, a -> a.getCurrentLoad())
    .description("Current load factor for agent")
    .register(meterRegistry);

// LLM Metrics
private final Counter llmTokensUsed = Counter.builder("llm.tokens.total")
    .description("Total tokens consumed")
    .tag("provider", "unknown")
    .tag("model", "unknown")
    .register(meterRegistry);

private final DistributionSummary llmLatency = DistributionSummary.builder("llm.latency")
    .description("LLM request latency")
    .baseUnit("milliseconds")
    .publishPercentiles(0.5, 0.95, 0.99)
    .register(meterRegistry);

// Skill Metrics
private final Counter skillExecutions = Counter.builder("skill.executions")
    .description("Total skill executions")
    .tag("skill", "unknown")
    .tag("category", "unknown")
    .register(meterRegistry);

private final Gauge skillSuccessRate = Gauge.builder("skill.success_rate",
    skillLibrary, sl -> sl.getOverallSuccessRate())
    .description("Skill library success rate")
    .register(meterRegistry);

// Coordination Metrics
private final Counter negotiationsStarted = Counter.builder("coordination.negotiations")
    .description("Contract net negotiations started")
    .register(meterRegistry);

private final Counter bidsReceived = Counter.builder("coordination.bids")
    .description("Bids received in negotiations")
    .register(meterRegistry);
```

### 3.3 Metrics Collection Points

```java
// In AgentStateMachine
public void transitionTo(State newState) {
    Span span = TracingUtils.startSpan("state.transition", Map.of(
        "from.state", currentState.name(),
        "to.state", newState.name()
    ));

    try (Scope scope = span.makeCurrent()) {
        // Record state transition
        stateTransitions.increment(
            Tags.of(
                "agent", agentId.toString(),
                "from", currentState.name(),
                "to", newState.name()
            )
        );

        currentState = newState;
        span.setStatus(StatusCode.OK);
    } catch (Exception e) {
        span.recordException(e);
        span.setStatus(StatusCode.ERROR, e.getMessage());
        throw e;
    } finally {
        span.end();
    }
}

// In TaskPlanner
public List<Task> planTasks(String command) {
    Span span = TracingUtils.startSpan("llm.planning", Map.of(
        "command", command,
        "model", config.getModel()
    ));

    try (Scope scope = span.makeCurrent()) {
        long startTime = System.nanoTime();

        // Call LLM
        List<Task> tasks = llmClient.plan(command);

        long duration = System.nanoTime() - startTime;

        // Record metrics
        llmLatency.record(duration / 1_000_000.0);
        llmTokensUsed.increment(
            Tags.of("provider", "openai", "model", "gpt-4")
        );

        span.setAttribute("tasks.generated", tasks.size());
        span.setStatus(StatusCode.OK);

        return tasks;
    } finally {
        span.end();
    }
}
```

### 3.4 EvaluationMetrics Integration

The existing `EvaluationMetrics` class provides comprehensive tracking:

```java
// Record planning phase
EvaluationMetrics.recordPlanningStart(agentName, timestamp);
EvaluationMetrics.recordPlanningComplete(agentName, latencyMs, taskCount);

// Record LLM call
EvaluationMetrics.recordLLMCall(
    providerId, model, inputTokens, outputTokens,
    costUsd, latencyMs, fromCache
);

// Record task completion
EvaluationMetrics.recordTaskComplete(
    agentName, success, correctnessScore, results
);

// Export for analysis
EvaluationMetrics.exportToJson("/path/to/metrics.json");
```

---

## 4. Dashboard Design

### 4.1 Main Dashboard Layout

```
┌─────────────────────────────────────────────────────────────────┐
│                    STEVE AI OVERVIEW                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │  AGENT STATUS   │  │  TASK METRICS   │  │  LLM COSTS      │ │
│  ├─────────────────┤  ├─────────────────┤  ├─────────────────┤ │
│  │ Active: 5/5     │  │ Completed: 127  │  │ Today: $12.45   │ │
│  │ Idle: 1         │  │ Failed: 8       │  │ This Week: $67  │ │
│  │ Busy: 3         │  │ In Progress: 3  │  │ Avg/Task: $0.09 │ │
│  │ Error: 1        │  │ Success: 94%    │  │ Tokens: 145K    │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              TASK SUCCESS RATE (24h)                     │   │
│  │  ████████████████████████████████████░░░ 94%            │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              AGENT LOAD DISTRIBUTION                     │   │
│  │  Steve-1: ████████████░░░░░░░░ 60%                      │   │
│  │  Steve-2: ████░░░░░░░░░░░░░░░░ 20%                      │   │
│  │  Steve-3: ██░░░░░░░░░░░░░░░░░░ 10%                      │   │
│  │  Steve-4: ████░░░░░░░░░░░░░░░░ 20%                      │   │
│  │  Steve-5: ░░░░░░░░░░░░░░░░░░░░ 0%                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 LLM Performance Dashboard

```
┌─────────────────────────────────────────────────────────────────┐
│                    LLM PERFORMANCE                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  REQUESTS PER MINUTE                                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  50 │                                                     │   │
│  │  40 │    ████                                             │   │
│  │  30 │    ████  ████                                       │   │
│  │  20 │    ████  ████  ████                                 │   │
│  │  10 │    ████  ████  ████  ████                           │   │
│  │   0 └──────────────────────────────────────────────────│   │
│  │       10:00  10:05  10:10  10:15  10:20                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  LATENCY DISTRIBUTION (ms)                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  10000 │                                                 │   │
│  │   5000 │    ████                                         │   │
│  │   1000 │    ████  ████  ████                             │   │
│  │    500 │    ████  ████  ████  ████                       │   │
│  │      0 └────────────────────────────────────────────────│   │
│  │          p50    p95    p99    max                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  COST BREAKDOWN                                                 │
│  ├─ GPT-4:    $45.00 (67%)                                    │
│  ├─ Groq:     $15.00 (22%)                                    │
│  └─ Gemini:   $7.50  (11%)                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.3 Skill Library Dashboard

```
┌─────────────────────────────────────────────────────────────────┐
│                    SKILL LIBRARY                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  TOP 5 SKILLS (by executions)                                  │
│  ├─ stripMine: 145 executions (98.6% success)                  │
│  ├─ buildShelter: 89 executions (95.5% success)                │
│  ├─ craftItem: 67 executions (99.0% success)                  │
│  ├─ collectDrops: 54 executions (97.2% success)                │
│  └─ followPlayer: 32 executions (100% success)                 │
│                                                                 │
│  SKILL CATEGORY DISTRIBUTION                                   │
│  ├─ Mining: 35%                                               │
│  ├─ Building: 25%                                             │
│  ├─ Crafting: 20%                                             │
│  ├─ Movement: 15%                                             │
│  └─ Combat: 5%                                                │
│                                                                 │
│  RECENT SKILL REFINEMENTS                                      │
│  ├─ mineIronDiamond: Iteration 2 → 3 (improving)              │
│  ├─ buildNetherPortal: Iteration 4 → SUCCESS                  │
│  └─ autoFarm: Iteration 1 → 2 (in progress)                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.4 Coordination Dashboard

```
┌─────────────────────────────────────────────────────────────────┐
│                MULTI-AGENT COORDINATION                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  CONTRACT NET PROTOCOL                                          │
│  ├─ Negotiations Started: 45                                   │
│  ├─ Bids Received: 127 (avg 2.8 per negotiation)              │
│  ├─ Awards Granted: 45 (100% success rate)                    │
│  └─ Conflicts Resolved: 7 (15.6% conflict rate)               │
│                                                                 │
│  AWARD SELECTION                                                │
│  ├─ Avg Selection Time: 1.2ms                                 │
│  ├─ Avg Score: 0.76                                           │
│  └─ Top Winner: Steve-1 (12 awards)                           │
│                                                                 │
│  WORKLOAD DISTRIBUTION                                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Steve-1: ████████████████░░░░░░░░░ 65% (12 tasks)      │   │
│  │  Steve-2: ████████░░░░░░░░░░░░░░░░ 35% (6 tasks)        │   │
│  │  Steve-3: ██████████████░░░░░░░░░░░ 55% (10 tasks)       │   │
│  │  Steve-4: ████░░░░░░░░░░░░░░░░░░░░ 20% (3 tasks)        │   │
│  │  Steve-5: ████████░░░░░░░░░░░░░░░░ 35% (6 tasks)        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. LLM Observability

### 5.1 LangSmith Integration

LangSmith provides end-to-end tracing for LLM applications:

```java
// Configuration
LangSmithConfig config = LangSmithConfig.builder()
    .apiKey(System.getenv("LANGSMITH_API_KEY"))
    .projectName("steve-ai-production")
    .samplingRate(1.0)  // Trace all requests
    .build();

// Wrap LLM calls
LangSmithTracer tracer = new LangSmithTracer(config);

public List<Task> planTasks(String command) {
    return tracer.trace("llm.planning", () -> {
        return llmClient.plan(command);
    }, Map.of(
        "command", command,
        "model", config.getModel(),
        "agent", agentId.toString()
    ));
}
```

### 5.2 LLM Call Metrics

Track for every LLM call:

```java
public class LLMCallMetrics {
    private final String requestId;
    private final String provider;
    private final String model;
    private final int inputTokens;
    private final int outputTokens;
    private final double costUsd;
    private final long latencyMs;
    private final boolean cached;
    private final boolean success;
    private final String errorMessage;

    // Export to monitoring
    public void record() {
        // Counter
        llmCalls.increment(
            Tags.of(
                "provider", provider,
                "model", model,
                "status", success ? "success" : "error"
            )
        );

        // Histogram
        llmLatency.record(latency);

        // Gauge
        llmCost.increment(costUsd);

        // LangSmith
        langSmith.record(this);
    }
}
```

### 5.3 Cost Tracking

```java
public class LLMCostTracker {
    private final Map<String, CostAccumulator> costs = new ConcurrentHashMap<>();

    public void recordCall(String model, int inputTokens, int outputTokens) {
        costs.computeIfAbsent(model, m -> new CostAccumulator(m))
            .add(inputTokens, outputTokens);
    }

    public CostReport getReport(TimeRange range) {
        return costs.values().stream()
            .map(CostAccumulator::getReport)
            .reduce(CostReport::combine)
            .orElse(CostReport.empty());
    }
}
```

### 5.4 Performance Targets

| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| Planning latency | < 2s | > 5s |
| Token usage per task | < 1000 | > 2000 |
| Cost per task | < $0.10 | > $0.20 |
| Cache hit rate | > 30% | < 10% |
| Error rate | < 1% | > 5% |

---

## 6. Agent Coordination Tracking

### 6.1 Contract Net Protocol Metrics

```java
// In ContractNetManager
public void recordNegotiationMetrics() {
    // Track negotiation flow
    negotiationsStarted.increment();

    for (TaskBid bid : bids) {
        bidsReceived.increment(
            Tags.of("agent", bid.bidderId().toString())
        );
    }

    if (winner.isPresent()) {
        awardsGranted.increment(
            Tags.of("winner", winner.get().bidderId().toString())
        );
    }
}

public void recordConflictResolution(ResolutionStrategy strategy) {
    conflictsResolved.increment(
        Tags.of("strategy", strategy.name())
    );
}
```

### 6.2 Workload Tracking

```java
// In WorkloadTracker
public void recordAgentMetrics(UUID agentId) {
    AgentWorkload workload = getWorkload(agentId);

    agentLoad.set(workload.getCurrentLoad());

    activeTasks.set(workload.getActiveTaskCount());

    agentSuccessRate.set(workload.getSuccessRate());

    agentAverageCompletionTime.set(workload.getAverageCompletionTime());
}
```

### 6.3 Coordination Dashboards

Key coordination metrics to visualize:

- **Negotiation Success Rate**: % of negotiations resulting in awards
- **Bid Participation**: Avg number of bids per negotiation
- **Conflict Rate**: % of negotiations requiring tie-breaking
- **Agent Utilization**: % of agents actively working vs idle
- **Load Balancing**: Variance in agent load factors

---

## 7. Skill Analytics

### 7.1 Skill Execution Metrics

```java
public class SkillAnalytics {
    private final Map<String, SkillMetrics> skillMetrics;

    public void recordExecution(String skillName, boolean success, long duration) {
        SkillMetrics metrics = skillMetrics.computeIfAbsent(
            skillName, k -> new SkillMetrics(k)
        );

        metrics.recordExecution(success, duration);

        // Update global metrics
        skillExecutions.increment(Tags.of("skill", skillName));
        skillLatency.record(duration);

        if (success) {
            skillSuccesses.increment(Tags.of("skill", skillName));
        } else {
            skillFailures.increment(Tags.of("skill", skillName));
        }
    }

    public SkillReport getReport(String skillName) {
        SkillMetrics metrics = skillMetrics.get(skillName);
        return metrics != null ? metrics.getReport() : SkillReport.empty();
    }
}
```

### 7.2 Skill Composition Tracking

```java
public class CompositionAnalytics {
    public void recordComposition(ComposedSkill composition, CompositionResult result) {
        String signature = composition.getSignature();

        compositionsAttempted.increment(Tags.of("signature", signature));

        if (result.isSuccess()) {
            compositionsSucceeded.increment(Tags.of("signature", signature));
        } else {
            compositionsFailed.increment(Tags.of(
                "signature", signature,
                "failure_step", String.valueOf(result.getCompletedSteps())
            ));
        }

        compositionTime.record(result.getTotalDuration());
    }
}
```

### 7.3 Refinement Tracking

```java
public class RefinementAnalytics {
    public void recordRefinement(RefinementResult result) {
        refinementsAttempted.increment();

        if (result.isSuccess()) {
            refinementsSucceeded.increment(
                Tags.of("iterations", String.valueOf(result.getIterations()))
            );
        } else {
            refinementsFailed.increment();
        }

        refinementIterations.record(result.getIterations());
    }

    public RefinementReport getReport() {
        return RefinementReport.builder()
            .totalAttempts(refinementsAttempted.get())
            .successRate(calculateSuccessRate())
            .averageIterations(calculateAvgIterations())
            .build();
    }
}
```

---

## 8. Integration Patterns

### 8.1 Prometheus Integration

```java
// In build.gradle
implementation 'io.micrometer:micrometer-registry-prometheus:1.11.0'

// Configuration
PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

// Expose metrics endpoint
MetricsEndpoint endpoint = new MetricsEndpoint(registry);

// Scrape config (prometheus.yml)
scrape_configs:
  - job_name: 'steve-ai'
    scrape_interval: 15s
    static_configs:
      - targets: ['localhost:9090']
```

### 8.2 Grafana Dashboard JSON

```json
{
  "dashboard": {
    "title": "Steve AI Overview",
    "panels": [
      {
        "title": "Task Success Rate",
        "targets": [
          {
            "expr": "rate(agent_tasks_completed_total[5m]) / rate(agent_tasks_total[5m])"
          }
        ]
      },
      {
        "title": "LLM Cost per Hour",
        "targets": [
          {
            "expr": "sum_increase(llm_cost_usd[1h])"
          }
        ]
      }
    ]
  }
}
```

### 8.3 AlertManager Configuration

```yaml
# alertmanager.yml
groups:
  - name: steve-ai-alerts
    rules:
      - alert: HighTaskFailureRate
        expr: rate(agent_tasks_failed_total[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High task failure rate detected"
          description: "{{ $value }} tasks failing per second"

      - alert: HighLLMCost
        expr: sum_increase(llm_cost_usd[1h]) > 10
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "LLM cost exceeding $10/hour"
          description: "Current hourly cost: {{ $value }}"

      - alert: AgentNotResponding
        expr: up{job="steve-ai"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Agent instance down"
          description: "Agent {{ $labels.instance }} is not responding"
```

---

## 9. Alert Configuration

### 9.1 Alert Thresholds

| Alert | Condition | Duration | Severity |
|-------|-----------|----------|----------|
| HighTaskFailureRate | failure rate > 10% | 5 min | Warning |
| HighLLMCost | cost > $10/hour | 1 min | Warning |
| AgentDown | instance not up | 2 min | Critical |
| HighLatency | p99 latency > 10s | 5 min | Warning |
| LowCacheHitRate | cache hit rate < 10% | 15 min | Info |
| AgentIdle | idle time > 30 min | 30 min | Info |

### 9.2 Alert Actions

```java
public class AlertManager {
    public void handleAlert(Alert alert) {
        switch (alert.getSeverity()) {
            case CRITICAL:
                // Page on-call engineer
                pagerService.notify(alert);
                // Attempt automatic recovery
                recoveryService.attemptRecovery(alert);
                break;

            case WARNING:
                // Send to Slack
                slackService.send(alert);
                // Create incident ticket
                incidentService.createTicket(alert);
                break;

            case INFO:
                // Log only
                logger.info("Info alert: {}", alert);
                break;
        }
    }
}
```

---

## 10. Implementation Roadmap

### 10.1 Phase 1: Foundation (Week 1-2)

- [ ] Set up OpenTelemetry collector
- [ ] Instrument core agent classes
- [ ] Configure Prometheus metrics
- [ ] Set up Grafana dashboards
- [ ] Configure basic alerts

### 10.2 Phase 2: LLM Observability (Week 3-4)

- [ ] Integrate LangSmith
- [ ] Instrument all LLM calls
- [ ] Set up cost tracking
- [ ] Create LLM performance dashboards
- [ ] Configure cost alerts

### 10.3 Phase 3: Skill Analytics (Week 5-6)

- [ ] Instrument skill execution
- [ ] Track composition metrics
- [ ] Monitor refinement process
- [ ] Create skill library dashboards
- [ ] Set up skill performance alerts

### 10.4 Phase 4: Coordination Observability (Week 7-8)

- [ ] Instrument Contract Net Protocol
- [ ] Track workload distribution
- [ ] Monitor agent interactions
- [ ] Create coordination dashboards
- [ ] Set up coordination alerts

### 10.5 Phase 5: Advanced Features (Week 9-10)

- [ ] Implement distributed tracing
- [ ] Set up log aggregation (Loki)
- [ ] Create custom alerting rules
- [ ] Implement A/B testing metrics
- [ ] Set up anomaly detection

---

## 11. Conclusion

Production observability is critical for Steve AI's reliability and cost-effectiveness. This strategy provides:

**Comprehensive Coverage:**
- Distributed tracing across all agent interactions
- Real-time metrics for performance and costs
- LLM-specific observability via LangSmith
- Skill library analytics
- Multi-agent coordination tracking

**Actionable Insights:**
- Real-time dashboards for operators
- Automated alerting for issues
- Cost optimization guidance
- Performance bottleneck identification
- Skill effectiveness tracking

**Production Readiness:**
- 99.5% uptime targets
- < 2s planning latency
- < $0.10 per task costs
- 90%+ task success rate

The observability strategy ensures Steve AI can operate reliably in production while providing the insights needed for continuous improvement.

---

**Document Version:** 1.0
**Last Updated:** March 3, 2026
**Related Documents:**
- `SKILL_REFINEMENT_RESEARCH.md` - Skill system observability
- `MULTI_AGENT_COORDINATION.md` - Coordination metrics
- `EVALATION_METRICS.java` - Metrics collection implementation
