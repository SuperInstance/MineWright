# Game AI Patterns Research: Behavior Trees, Utility AI, GOAP, HTN & Dialogue Systems

**Date:** February 27, 2026
**Research Focus:** Game AI Decision-Making Patterns for MineWright AI
**Application:** Foreman/Worker System Improvements

---

## Executive Summary

This research document synthesizes 2024-2025 advances in game AI patterns with specific applications for MineWright AI's Foreman/Worker coordination system. The analysis covers Behavior Trees, Utility AI, Goal-Oriented Action Planning (GOAP), Hierarchical Task Networks (HTN), and NPC Dialogue Systems.

### Key Findings for MineWright AI

| Pattern | Maturity | Integration Effort | Priority for MineWright AI |
|---------|----------|-------------------|----------------------|
| **Utility AI** | Production-Ready | Medium | **HIGH** - For worker task assignment |
| **Behavior Trees** | Production-Ready | High | **HIGH** - For reactive decision making |
| **HTN Planning** | Growing | High | **MEDIUM** - For hierarchical task decomposition |
| **GOAP** | Declining | Medium | **LOW** - HTN is preferred for structured tasks |
| **Dialogue State Machines** | Production-Ready | Medium | **MEDIUM** - For player communication |

---

## Table of Contents

1. [Behavior Tree Patterns](#1-behavior-tree-patterns)
2. [Utility AI Systems](#2-utility-ai-systems)
3. [GOAP Implementation](#3-goap-implementation)
4. [HTN Decomposition Strategies](#4-htn-decomposition-strategies)
5. [NPC Dialogue State Machines](#5-npc-dialogue-state-machines)
6. [Foreman/Worker System Analysis](#6-foremanworker-system-analysis)
7. [Specific Improvements](#7-specific-improvements-for-foremanworker-system)
8. [Implementation Roadmap](#8-implementation-roadmap)

---

## 1. Behavior Tree Patterns

### 1.1 Core Behavior Tree Concepts (2024-2025)

Behavior Trees (BTs) are hierarchical graphs for modeling AI decision-making. Unlike finite state machines, BTs provide:

- **Hierarchical decomposition** of complex behaviors
- **Reactive execution** through continuous re-evaluation
- **Modular design** with reusable node types
- **Event-driven patterns** for modern implementations

### 1.2 Node Types

#### Composite Nodes

| Node Type | Description | Use Case |
|-----------|-------------|----------|
| **Sequence** | Executes children in order, fails if any child fails | Sequential actions (move → mine → return) |
| **Selector (Fallback)** | Tries children in order until one succeeds | Alternative actions (attack → retreat → hide) |
| **Parallel** | Executes all children simultaneously | Monitor multiple conditions |
| **Decorator** | Wraps child to modify behavior | Repeat, invert, timeout behaviors |

#### Leaf Nodes

| Node Type | Description | Use Case |
|-----------|-------------|----------|
| **Action** | Performs a game action | Move, mine, place blocks |
| **Condition** | Checks a state predicate | Is enemy nearby? Have resources? |
| **Wait** | Delays execution | Timing-dependent behaviors |

### 1.3 Modern Event-Driven Behavior Trees (2024-2025 Trend)

Traditional BTs tick continuously, wasting CPU cycles. Modern implementations use **event-driven patterns**:

```java
// Event-driven behavior tree node
public interface EventDrivenNode {
    void onEvent(Event event);
    NodeStatus tick();
    void subscribe(EventBus eventBus);
}

// Example: React to worker completion events
public class WorkerCompletionCondition extends ConditionNode {
    private boolean workerCompleted = false;

    @Override
    public void onEvent(Event event) {
        if (event instanceof WorkerTaskCompleteEvent) {
            this.workerCompleted = true;
        }
    }

    @Override
    public NodeStatus tick() {
        return workerCompleted ? NodeStatus.SUCCESS : NodeStatus.FAILURE;
    }
}
```

**Benefits for MineWright AI:**
- Reduced CPU usage (no continuous polling for worker status)
- Immediate response to task completion events
- Better scalability for multi-worker scenarios

### 1.4 Behavior Tree Node Status

Every BT node returns one of three statuses:

```java
public enum NodeStatus {
    SUCCESS,  // Node completed successfully
    FAILURE,  // Node failed
    RUNNING   // Node still executing (multi-tick action)
}
```

### 1.5 Example: Worker Selection Behavior Tree

```
ROOT (Selector)
├── Sequence: Assign Combat Worker
│   ├── Condition: Is Enemy Nearby?
│   ├── Condition: Has Available Combat Worker?
│   └── Action: Assign Worker to Combat
│
├── Sequence: Assign Mining Worker
│   ├── Condition: Need Resources?
│   ├── Condition: Has Available Worker?
│   └── Action: Assign Worker to Mining
│
└── Sequence: Assign Building Worker
    ├── Condition: Has Queued Build Task?
    ├── Condition: Worker Has Materials?
    └── Action: Assign Worker to Building
```

### 1.6 BT Implementation for MineWright AI

```java
package com.steve.ai.bt;

import com.steve.execution.EventBus;
import java.util.*;

/**
 * Behavior tree for reactive worker assignment decisions.
 */
public class WorkerAssignmentTree {
    private final BehaviorNode root;
    private final EventBus eventBus;
    private final Map<Class<?>, List<BehaviorNode>> eventSubscribers;

    public WorkerAssignmentTree(EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventSubscribers = new HashMap<>();
        this.root = buildTree();
        registerEventHandlers(root);
    }

    private BehaviorNode buildTree() {
        SelectorNode root = new SelectorNode();

        // Combat branch (highest priority)
        SequenceNode combatBranch = new SequenceNode();
        combatBranch.addChild(new EnemyProximityCondition());
        combatBranch.addChild(new AvailableWorkerCondition("combat"));
        combatBranch.addChild(new AssignCombatAction());
        root.addChild(combatBranch);

        // Mining branch
        SequenceNode miningBranch = new SequenceNode();
        miningBranch.addChild(new ResourceNeedCondition());
        miningBranch.addChild(new AvailableWorkerCondition("mining"));
        miningBranch.addChild(new AssignMiningAction());
        root.addChild(miningBranch);

        // Building branch
        SequenceNode buildingBranch = new SequenceNode();
        buildingBranch.addChild(new QueuedBuildTaskCondition());
        buildingBranch.addChild(new WorkerHasMaterialsCondition());
        buildingBranch.addChild(new AssignBuildAction());
        root.addChild(buildingBranch);

        return root;
    }

    public void tick() {
        root.tick();
    }

    public void onEvent(Event event) {
        List<BehaviorNode> subscribers = eventSubscribers.get(event.getClass());
        if (subscribers != null) {
            subscribers.forEach(node -> node.onEvent(event));
        }
    }

    private void registerEventHandlers(BehaviorNode node) {
        if (node instanceof EventSubscriber) {
            EventSubscriber subscriber = (EventSubscriber) node;
            subscriber.getSubscribedEvents().forEach(eventClass -> {
                eventSubscribers.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(node);
            });
        }

        if (node instanceof CompositeNode) {
            ((CompositeNode) node).getChildren().forEach(this::registerEventHandlers);
        }
    }
}
```

---

## 2. Utility AI Systems

### 2.1 Core Utility AI Concepts (2024-2025)

Utility AI scores actions based on multiple contextual considerations, selecting the action with the highest score. Unlike rigid state machines, utility systems provide:

- **Smooth behavior blending** - No hard state transitions
- **Context-aware decisions** - Actions evaluated based on current situation
- **Easy tuning** - Adjust weights and curves without restructuring
- **Predictable scoring** - Mathematical curves for consistent behavior

### 2.2 Utility Scoring Formula

```
Utility(Action) = f(Consideration₁, Consideration₂, ..., Considerationₙ)

Where each Consideration:
Score = Curve(Normalized_Input) × Weight
```

### 2.3 Response Curves (Critical Component)

Response curves map input values to normalized scores [0, 1]. Different curve shapes create different behaviors:

#### Linear Curve
```
Score = m × x + b
```
- **Use Case:** Direct proportional relationships (more resources = higher utility)

#### Logistic Curve
```
Score = 1 / (1 + e^(-k × (x - x₀)))
```
- **Use Case:** Threshold-based decisions (health below 30% = flee)

#### Exponential Curve
```
Score = base^(exponent × x)
```
- **Use Case:** Rapidly increasing/decreasing values

#### Binary Curve
```
Score = 1 if condition_met else 0
```
- **Use Case:** Boolean conditions (has weapon? can place block?)

### 2.4 Utility AI for Worker Task Assignment

```java
package com.steve.ai.utility;

import com.steve.entity.WorkerEntity;
import com.steve.action.Task;
import java.util.*;
import java.util.function.Function;

/**
 * Utility AI system for scoring and assigning worker tasks.
 */
public class WorkerUtilitySystem {
    private final List<UtilityAction> actions;
    private final Map<String, ResponseCurve> curves;

    public WorkerUtilitySystem() {
        this.actions = new ArrayList<>();
        this.curves = new HashMap<>();
        initializeCurves();
        initializeActions();
    }

    private void initializeCurves() {
        // Distance curve: closer = higher utility
        curves.put("distance_inverse", new InverseExponentialCurve(0.1));

        // Health curve: critical health = very high flee utility
        curves.put("health_critical", new LogisticCurve(2.0, 0.3));

        // Resource need curve: less resources = higher gather utility
        curves.put("resource_need", new LinearCurve(-1.0, 1.0));

        // Worker availability: more available = higher assign utility
        curves.put("worker_availability", new LinearCurve(1.0, 0.0));
    }

    private void initializeActions() {
        // Combat action
        actions.add(new UtilityAction("assign_combat")
            .addConsideration("enemy_distance", "distance_inverse", 1.0,
                ctx -> ctx.getDistanceToNearestEnemy())
            .addConsideration("has_combat_worker", "binary", 1.0,
                ctx -> ctx.hasAvailableWorker("combat") ? 1.0 : 0.0)
            .addConsideration("enemy_threat", "linear", 0.8,
                ctx -> ctx.getEnemyThreatLevel()));

        // Mining action
        actions.add(new UtilityAction("assign_mining")
            .addConsideration("resource_need", "resource_need", 1.0,
                ctx -> 1.0 - ctx.getResourcePercentage("iron"))
            .addConsideration("has_miner", "worker_availability", 1.0,
                ctx -> ctx.getAvailableWorkerCount("miner") / 10.0)
            .addConsideration("distance_to_ore", "distance_inverse", 0.7,
                ctx -> ctx.getDistanceToNearestOre()));

        // Building action
        actions.add(new UtilityAction("assign_building")
            .addConsideration("queued_build_tasks", "linear", 1.0,
                ctx -> ctx.getQueuedBuildTaskCount() / 20.0)
            .addConsideration("has_builder", "worker_availability", 1.0,
                ctx -> ctx.getAvailableWorkerCount("builder") / 10.0)
            .addConsideration("has_materials", "binary", 0.9,
                ctx -> ctx.hasBuildingMaterials() ? 1.0 : 0.0));
    }

    public UtilityAction selectBestAction(WorkerContext context) {
        return actions.stream()
            .max(Comparator.comparing(a -> a.calculateUtility(context, curves)))
            .orElse(null);
    }

    public Task createTaskFromAction(UtilityAction action, WorkerContext context) {
        return Task.builder()
            .action(action.getId())
            .parameters(context.toParameterMap())
            .build();
    }
}

/**
 * Represents an action that can be scored by the utility system.
 */
public class UtilityAction {
    private final String id;
    private final List<Consideration> considerations;

    public UtilityAction(String id) {
        this.id = id;
        this.considerations = new ArrayList<>();
    }

    public UtilityAction addConsideration(String name, String curveId,
                                          double weight,
                                          Function<WorkerContext, Double> extractor) {
        considerations.add(new Consideration(name, curveId, weight, extractor));
        return this;
    }

    public double calculateUtility(WorkerContext context, Map<String, ResponseCurve> curves) {
        if (considerations.isEmpty()) return 0.0;

        // Weighted average of all considerations
        double totalScore = 0.0;
        double totalWeight = 0.0;

        for (Consideration c : considerations) {
            double input = c.extractor.apply(context);
            ResponseCurve curve = curves.get(c.curveId);
            double normalizedScore = curve != null ? curve.evaluate(input) : input;

            totalScore += normalizedScore * c.weight;
            totalWeight += c.weight;
        }

        return totalWeight > 0 ? totalScore / totalWeight : 0.0;
    }

    public String getId() { return id; }

    private static class Consideration {
        final String name;
        final String curveId;
        final double weight;
        final Function<WorkerContext, Double> extractor;

        Consideration(String name, String curveId, double weight,
                     Function<WorkerContext, Double> extractor) {
            this.name = name;
            this.curveId = curveId;
            this.weight = weight;
            this.extractor = extractor;
        }
    }
}
```

### 2.5 Response Curve Implementations

```java
package com.steve.ai.utility;

/**
 * Response curves for utility score normalization.
 */
public interface ResponseCurve {
    double evaluate(double input);
}

class LinearCurve implements ResponseCurve {
    private final double slope;
    private final double intercept;

    public LinearCurve(double slope, double intercept) {
        this.slope = slope;
        this.intercept = intercept;
    }

    @Override
    public double evaluate(double x) {
        return Math.max(0, Math.min(1, slope * x + intercept));
    }
}

class LogisticCurve implements ResponseCurve {
    private final double k;      // Steepness
    private final double x0;     // Midpoint

    public LogisticCurve(double k, double x0) {
        this.k = k;
        this.x0 = x0;
    }

    @Override
    public double evaluate(double x) {
        return 1.0 / (1.0 + Math.exp(-k * (x - x0)));
    }
}

class InverseExponentialCurve implements ResponseCurve {
    private final double decayRate;

    public InverseExponentialCurve(double decayRate) {
        this.decayRate = decayRate;
    }

    @Override
    public double evaluate(double x) {
        return Math.exp(-decayRate * x);
    }
}

class BinaryCurve implements ResponseCurve {
    @Override
    public double evaluate(double x) {
        return x > 0 ? 1.0 : 0.0;
    }
}
```

### 2.6 Worker Context for Utility Evaluation

```java
package com.steve.ai.utility;

import com.steve.entity.ForemanEntity;
import com.steve.entity.WorkerEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Context object for utility score calculations.
 */
public class WorkerContext {
    private final ForemanEntity foreman;
    private final Level level;
    private final Map<String, Object> cache;

    public WorkerContext(ForemanEntity foreman) {
        this.foreman = foreman;
        this.level = foreman.level();
        this.cache = new HashMap<>();
    }

    public double getDistanceToNearestEnemy() {
        return (double) cache.computeIfAbsent("nearest_enemy_dist",
            k -> level.getEntitiesOfClass(Entity.class,
                foreman.getBoundingBox().inflate(32)).stream()
                .filter(e -> e instanceof net.minecraft.world.entity.monster.Monster)
                .mapToDouble(e -> foreman.position().distanceTo(e.position()))
                .min()
                .orElse(100.0));
    }

    public double getDistanceToNearestOre() {
        return (double) cache.computeIfAbsent("nearest_ore_dist",
            k -> {
                // Scan nearby chunks for ore
                return 15.0; // Placeholder: actual implementation
            });
    }

    public double getResourcePercentage(String resource) {
        return (double) cache.computeIfAbsent("resource_" + resource,
            k -> {
                int count = foreman.getInventory().getItemCount(resource);
                return Math.min(1.0, count / 64.0); // Normalize to stack size
            });
    }

    public boolean hasAvailableWorker(String workerType) {
        return getAvailableWorkerCount(workerType) > 0;
    }

    public double getAvailableWorkerCount(String workerType) {
        return (double) cache.computeIfAbsent("workers_" + workerType,
            k -> foreman.getWorkerManager().getAvailableWorkers(workerType).size());
    }

    public int getQueuedBuildTaskCount() {
        return (int) cache.computeIfAbsent("queued_build_tasks",
            k -> foreman.getBuildManager().getQueuedTaskCount());
    }

    public boolean hasBuildingMaterials() {
        return getResourcePercentage("oak_planks") > 0.1 &&
               getResourcePercentage("cobblestone") > 0.1;
    }

    public double getEnemyThreatLevel() {
        // 0-1 scale based on enemy type and count
        return (double) cache.computeIfAbsent("enemy_threat",
            k -> {
                long enemyCount = level.getEntitiesOfClass(
                    net.minecraft.world.entity.monster.Monster.class,
                    foreman.getBoundingBox().inflate(32)
                ).size();
                return Math.min(1.0, enemyCount / 5.0);
            });
    }

    public Map<String, Object> toParameterMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("foreman_uuid", foreman.getUUID().toString());
        return params;
    }
}
```

---

## 3. GOAP Implementation

### 3.1 Goal-Oriented Action Planning Overview

GOAP is an AI planning technique where agents plan sequences of actions to achieve goals based on world state. It uses **backward chaining** from goals to preconditions.

**Current Status (2024-2025):** GOAP is declining in popularity compared to HTN for game AI, as it can produce unpredictable behaviors. However, it remains useful for certain scenarios.

### 3.2 GOAP vs HTN Decision Matrix

| Factor | GOAP | HTN | Recommendation for MineWright AI |
|--------|------|-----|------------------------------|
| **Control** | Emergent, unpredictable | Designer-controlled | **HTN** - Need predictable builds |
| **Planning Direction** | Backward chaining | Forward decomposition | **HTN** - Natural task breakdown |
| **Complexity** | Simpler to implement | More complex hierarchies | **GOAP** - Quick prototyping |
| **Industry Usage** | Declining | Growing (Horizon Zero Dawn) | **HTN** - Future-proof |
| **Multi-Agent** | Difficult to coordinate | Natural coordination | **HTN** - Worker system |

### 3.3 GOAP Core Components

```java
package com.steve.ai.goap;

import java.util.*;

/**
 * GOAP Action with preconditions and effects.
 */
public class GoapAction {
    private final String name;
    private final Map<String, Boolean> preconditions;
    private final Map<String, Boolean> effects;
    private final double cost;

    public GoapAction(String name, double cost) {
        this.name = name;
        this.preconditions = new HashMap<>();
        this.effects = new HashMap<>();
        this.cost = cost;
    }

    public GoapAction addPrecondition(String key, boolean value) {
        preconditions.put(key, value);
        return this;
    }

    public GoapAction addEffect(String key, boolean value) {
        effects.put(key, value);
        return this;
    }

    public boolean isProcced(Map<String, Boolean> worldState) {
        return preconditions.entrySet().stream()
            .allMatch(entry -> worldState.getOrDefault(entry.getKey(), !entry.getValue()) == entry.getValue());
    }

    // Getters
    public String getName() { return name; }
    public Map<String, Boolean> getPreconditions() { return preconditions; }
    public Map<String, Boolean> getEffects() { return effects; }
    public double getCost() { return cost; }
}

/**
 * GOAP Goal with desired world state.
 */
public class GoapGoal {
    private final String name;
    private final Map<String, Boolean> desiredState;

    public GoapGoal(String name) {
        this.name = name;
        this.desiredState = new HashMap<>();
    }

    public GoapGoal addGoalState(String key, boolean value) {
        desiredState.put(key, value);
        return this;
    }

    public boolean isAchieved(Map<String, Boolean> worldState) {
        return desiredState.entrySet().stream()
            .allMatch(entry -> worldState.getOrDefault(entry.getKey(), !entry.getValue()) == entry.getValue());
    }

    // Getters
    public String getName() { return name; }
    public Map<String, Boolean> getDesiredState() { return desiredState; }
}
```

### 3.4 GOAP Planner (A* Implementation)

```java
package com.steve.ai.goap;

import java.util.*;

/**
 * GOAP Planner using A* search for action sequence planning.
 */
public class GoapPlanner {
    private final List<GoapAction> availableActions;

    public GoapPlanner(List<GoapAction> availableActions) {
        this.availableActions = availableActions;
    }

    public Queue<GoapAction> plan(Map<String, Boolean> worldState, GoapGoal goal) {
        // Check if goal already achieved
        if (goal.isAchieved(worldState)) {
            return new LinkedList<>();
        }

        // A* search
        PriorityQueue<Node> openSet = new PriorityQueue<>(
            Comparator.comparingDouble(n -> n.f)
        );
        Set<Map<String, Boolean>> closedSet = new HashSet<>();

        Node startNode = new Node(worldState, null, null, 0, heuristic(worldState, goal));
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            // Check if goal achieved
            if (goal.isAchieved(current.state)) {
                return buildPath(current);
            }

            closedSet.add(current.state);

            // Expand neighbors
            for (GoapAction action : availableActions) {
                if (!action.isProcced(current.state)) {
                    continue;
                }

                // Apply action effects
                Map<String, Boolean> newState = applyAction(current.state, action);

                if (closedSet.contains(newState)) {
                    continue;
                }

                double gScore = current.g + action.getCost();
                double fScore = gScore + heuristic(newState, goal);

                Node neighbor = new Node(newState, action, current, gScore, fScore);
                openSet.add(neighbor);
            }
        }

        // No plan found
        return null;
    }

    private Map<String, Boolean> applyAction(Map<String, Boolean> state, GoapAction action) {
        Map<String, Boolean> newState = new HashMap<>(state);
        newState.putAll(action.getEffects());
        return newState;
    }

    private double heuristic(Map<String, Boolean> state, GoapGoal goal) {
        // Count unsatisfied goals
        return goal.getDesiredState().entrySet().stream()
            .filter(entry -> state.getOrDefault(entry.getKey(), !entry.getValue()) != entry.getValue())
            .count();
    }

    private Queue<GoapAction> buildPath(Node node) {
        LinkedList<GoapAction> path = new LinkedList<>();
        while (node.action != null) {
            path.addFirst(node.action);
            node = node.parent;
        }
        return path;
    }

    private static class Node {
        final Map<String, Boolean> state;
        final GoapAction action;
        final Node parent;
        final double g;
        final double f;

        Node(Map<String, Boolean> state, GoapAction action, Node parent, double g, double f) {
            this.state = state;
            this.action = action;
            this.parent = parent;
            this.g = g;
            this.f = f;
        }
    }
}
```

### 3.5 GOAP for Worker Coordination Example

```java
package com.steve.ai.goap;

import com.steve.entity.WorkerEntity;
import java.util.*;

/**
 * Example: GOAP for assigning workers to tasks.
 */
public class WorkerGoapExample {
    private GoapPlanner planner;

    public void initialize() {
        List<GoapAction> actions = Arrays.asList(
            new GoapAction("assign_miner", 1.0)
                .addPrecondition("has_available_worker", true)
                .addPrecondition("needs_resources", true)
                .addEffect("worker_assigned", true)
                .addEffect("resources_being_gathered", true),

            new GoapAction("assign_builder", 1.0)
                .addPrecondition("has_available_worker", true)
                .addPrecondition("has_build_task", true)
                .addPrecondition("has_materials", true)
                .addEffect("worker_assigned", true)
                .addEffect("construction_started", true),

            new GoapAction("wait_for_worker", 0.1)
                .addPrecondition("worker_assigned", true)
                .addEffect("worker_busy", false)
        );

        this.planner = new GoapPlanner(actions);
    }

    public Queue<GoapAction> planWorkerAssignment(WorkerEntity worker) {
        // Current world state
        Map<String, Boolean> worldState = new HashMap<>();
        worldState.put("has_available_worker", worker.isAvailable());
        worldState.put("needs_resources", needsResources());
        worldState.put("has_build_task", hasQueuedBuildTasks());
        worldState.put("has_materials", hasBuildingMaterials());

        // Goal
        GoapGoal goal = new GoapGoal("worker_utilized")
            .addGoalState("worker_assigned", true);

        return planner.plan(worldState, goal);
    }

    private boolean needsResources() {
        // Check resource levels
        return true;
    }

    private boolean hasQueuedBuildTasks() {
        // Check build queue
        return true;
    }

    private boolean hasBuildingMaterials() {
        // Check materials
        return true;
    }
}
```

---

## 4. HTN Decomposition Strategies

### 4.1 Hierarchical Task Network Overview

HTN planning decomposes high-level tasks into subtasks through hierarchical methods. It uses **forward decomposition** rather than GOAP's backward chaining.

**Key Advantage for MineWright AI:** Designer control over task decomposition ensures predictable, reproducible building plans.

### 4.2 HTN Core Concepts

```
Compound Task: High-level goal that requires decomposition
Primitive Task: Directly executable action (maps to MineWright AI actions)
Method: Alternative way to decompose a compound task with preconditions
```

### 4.3 HTN Decomposition Example for Building

```
build_house (Compound Task)
├── Method: build_house_basic
│   ├── Preconditions: has_materials, has_clear_space
│   └── Subtasks:
│       ├── gather_materials (Compound)
│       │   ├── Method: gather_from_nearby
│       │   │   ├── Subtasks:
│       │   │   │   ├── pathfind {target: "forest"}
│       │   │   │   ├── mine {block: "oak_log", quantity: 32}
│       │   │   │   └── return_to_site
│       │   │   └── Preconditions: forest_nearby
│       │   └── Method: gather_from_storage
│       │       └── Subtasks: take_from_chest
│       ├── clear_area (Primitive)
│       ├── lay_foundation (Primitive)
│       ├── build_walls (Primitive)
│       └── add_roof (Primitive)
│
└── Method: build_house_advanced
    ├── Preconditions: has_materials, has_decorations
    └── Subtasks: [includes glass panes, doors, etc.]
```

### 4.4 HTN Planner Integration for MineWright AI

```java
package com.steve.ai.htn;

import com.steve.action.Task;
import com.steve.llm.TaskPlanner;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * HTN Planner for MineWright AI with LLM fallback.
 */
public class SteveHTNPlanner {
    private final HTNDomain domain;
    private final TaskPlanner llmPlanner;
    private final HTNCache cache;

    public SteveHTNPlanner(TaskPlanner llmPlanner) {
        this.domain = new HTNDomain();
        this.domain.loadDefaultDomain();
        this.llmPlanner = llmPlanner;
        this.cache = new HTNCache();
    }

    public CompletableFuture<List<Task>> planTasksAsync(
            String command,
            Map<String, Object> worldState) {

        // Check cache first
        String cacheKey = generateCacheKey(command, worldState);
        List<Task> cached = cache.get(cacheKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        // Try HTN decomposition
        HTNTask rootTask = classifyCommand(command);
        if (rootTask != null && domain.hasMethodsForTask(rootTask.getName())) {
            return decomposeTask(rootTask, worldState, command)
                .thenApply(tasks -> {
                    cache.put(cacheKey, tasks);
                    return tasks;
                });
        }

        // Fall back to LLM for novel tasks
        return llmPlanner.planTasksAsync(command)
            .thenApply(response -> {
                if (response != null) {
                    List<Task> tasks = response.getTasks();
                    learnFromPlan(command, tasks);
                    cache.put(cacheKey, tasks);
                    return tasks;
                }
                return Collections.emptyList();
            });
    }

    private HTNTask classifyCommand(String command) {
        String lower = command.toLowerCase().trim();

        if (lower.matches("build\\s+(house|tower|castle).*")) {
            String type = extractType(lower, "house", "tower", "castle");
            return new HTNTask("build_" + type, HTNTask.Type.COMPOUND);
        }

        if (lower.matches("mine\\s+(iron|diamond|coal|gold).*")) {
            String type = extractType(lower, "iron", "diamond", "coal", "gold");
            return new HTNTask("mine_" + type, HTNTask.Type.COMPOUND);
        }

        return null;
    }

    private CompletableFuture<List<Task>> decomposeTask(
            HTNTask task,
            Map<String, Object> worldState,
            String originalCommand) {

        if (task.getType() == HTNTask.Type.PRIMITIVE) {
            Task steveTask = convertToSteveTask(task);
            return CompletableFuture.completedFuture(Collections.singletonList(steveTask));
        }

        // Find applicable methods
        List<HTNMethod> methods = domain.getMethodsForTask(task.getName());

        for (HTNMethod method : methods) {
            if (method.checkPreconditions(worldState)) {
                // Decompose using this method
                List<CompletableFuture<List<Task>>> futures = new ArrayList<>();

                for (HTNTask subtask : method.getSubtasks()) {
                    futures.add(decomposeTask(subtask, worldState, originalCommand));
                }

                // Combine all subtasks
                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<Task> allTasks = new ArrayList<>();
                        futures.forEach(f -> allTasks.addAll(f.join()));
                        return allTasks;
                    });
            }
        }

        // No applicable method - fall back to LLM
        return llmPlanner.planTasksAsync(originalCommand)
            .thenApply(response -> response != null ? response.getTasks() : Collections.emptyList());
    }

    private Task convertToSteveTask(HTNTask htnTask) {
        return Task.builder()
            .action(htnTask.getName())
            .parameters(htnTask.getParameters())
            .build();
    }

    private void learnFromPlan(String command, List<Task> tasks) {
        HTNTask rootTask = classifyCommand(command);
        if (rootTask != null) {
            HTNMethod learnedMethod = HTNMethod.fromTaskList(
                "learned_" + command.hashCode(),
                tasks
            );
            domain.addMethod(rootTask.getName(), learnedMethod);
        }
    }

    private String generateCacheKey(String command, Map<String, Object> worldState) {
        return command.toLowerCase().trim();
    }

    private String extractType(String command, String... types) {
        for (String type : types) {
            if (command.contains(type)) {
                return type;
            }
        }
        return types[0];
    }
}
```

### 4.5 HTN Domain for Building Tasks

```java
package com.steve.ai.htn;

import java.util.*;

/**
 * HTN Domain for MineWright AI building tasks.
 */
public class HTNDomain {
    private final Map<String, List<HTNMethod>> methods;

    public HTNDomain() {
        this.methods = new HashMap<>();
    }

    public void loadDefaultDomain() {
        registerBuildingMethods();
        registerMiningMethods();
        registerCraftingMethods();
    }

    private void registerBuildingMethods() {
        // build_house methods
        addMethod("build_house",
            HTNMethod.builder("build_house_basic", "build_house")
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE,
                    Map.of("target", "build_site")))
                .addSubtask(new HTNTask("place_blocks", HTNTask.Type.PRIMITIVE,
                    Map.of("structure", "house", "material", "oak_planks")))
                .precondition(ws -> (boolean) ws.getOrDefault("has_materials", false))
                .priority(100)
                .build()
        );

        addMethod("build_house",
            HTNMethod.builder("build_house_with_gathering", "build_house")
                .addSubtask(new HTNTask("mine", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "oak_log", "quantity", 32)))
                .addSubtask(new HTNTask("craft", HTNTask.Type.PRIMITIVE,
                    Map.of("item", "oak_planks", "quantity", 128)))
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE,
                    Map.of("target", "build_site")))
                .addSubtask(new HTNTask("place_blocks", HTNTask.Type.PRIMITIVE,
                    Map.of("structure", "house", "material", "oak_planks")))
                .priority(50)
                .build()
        );

        // build_tower methods
        addMethod("build_tower",
            HTNMethod.builder("build_tower_basic", "build_tower")
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE))
                .addSubtask(new HTNTask("place_blocks", HTNTask.Type.PRIMITIVE,
                    Map.of("structure", "tower", "material", "cobblestone")))
                .priority(100)
                .build()
        );
    }

    private void registerMiningMethods() {
        // mine_diamond method
        addMethod("mine_diamond",
            HTNMethod.builder("mine_diamond_optimal", "mine_diamond")
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE,
                    Map.of("y", -59))) // Diamond optimal level
                .addSubtask(new HTNTask("mine", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "diamond_ore", "quantity", 8)))
                .priority(100)
                .build()
        );
    }

    private void registerCraftingMethods() {
        // craft_pickaxe methods
        addMethod("craft_pickaxe",
            HTNMethod.builder("craft_stone_pickaxe", "craft_pickaxe")
                .addSubtask(new HTNTask("mine", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "cobblestone", "quantity", 3)))
                .addSubtask(new HTNTask("craft", HTNTask.Type.PRIMITIVE,
                    Map.of("item", "stone_pickaxe", "quantity", 1)))
                .priority(100)
                .build()
        );
    }

    public void addMethod(String taskName, HTNMethod method) {
        methods.computeIfAbsent(taskName, k -> new ArrayList<>()).add(method);
        methods.get(taskName).sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }

    public List<HTNMethod> getMethodsForTask(String taskName) {
        return Collections.unmodifiableList(
            methods.getOrDefault(taskName, Collections.emptyList())
        );
    }

    public boolean hasMethodsForTask(String taskName) {
        return methods.containsKey(taskName) && !methods.get(taskName).isEmpty();
    }
}
```

---

## 5. NPC Dialogue State Machines

### 5.1 Dialogue System Overview (2024-2025)

Modern NPC dialogue systems have evolved from simple decision trees to sophisticated AI-driven conversations. Key trends:

- **Hybrid FSM + LLM**: State machines for dialogue flow, LLM for content generation
- **Emotional modeling**: NPCs with mood states (trust, patience, curiosity)
- **Long-term memory**: Characters remember past interactions (Convai: 3+ months)
- **Intent classification**: NLP-based understanding of player input
- **Context-aware responses**: Using conversation history and world state

### 5.2 Dialogue State Machine for MineWright AI

```java
package com.steve.ai.dialogue;

import com.steve.entity.SteveEntity;
import com.steve.execution.EventBus;
import java.util.*;

/**
 * Dialogue state machine for MineWright AI player communication.
 */
public class DialogueStateMachine {
    private final SteveEntity steve;
    private final EventBus eventBus;
    private DialogueState currentState;
    private final Deque<DialogueState> stateStack;
    private final DialogueMemory memory;

    public DialogueStateMachine(SteveEntity steve, EventBus eventBus) {
        this.steve = steve;
        this.eventBus = eventBus;
        this.currentState = DialogueState.IDLE;
        this.stateStack = new ArrayDeque<>();
        this.memory = new DialogueMemory();
    }

    public void handlePlayerInput(String input) {
        // Classify player intent
        PlayerIntent intent = classifyIntent(input);

        // Transition based on current state and intent
        DialogueState nextState = currentState.getNextState(intent);

        if (nextState != null) {
            transitionTo(nextState, input);
        }
    }

    private void transitionTo(DialogueState newState, String input) {
        // Push current state if interruptible
        if (currentState.isInterruptible()) {
            stateStack.push(currentState);
        }

        // Exit current state
        currentState.onExit(input);

        // Enter new state
        currentState = newState;
        currentState.onEnter(input, steve, memory);

        // Publish event
        eventBus.publish(new DialogueStateChangeEvent(steve.getUUID(), newState));
    }

    public void returnToPreviousState() {
        if (!stateStack.isEmpty()) {
            DialogueState previousState = stateStack.pop();
            currentState.onExit("return");
            currentState = previousState;
            currentState.onEnter("return", steve, memory);
        }
    }

    private PlayerIntent classifyIntent(String input) {
        String lower = input.toLowerCase();

        // Simple keyword-based classification
        // Advanced version would use NLP/LLM
        if (lower.contains("build") || lower.contains("construct") || lower.contains("make")) {
            return PlayerIntent.BUILD_REQUEST;
        }
        if (lower.contains("mine") || lower.contains("gather") || lower.contains("get")) {
            return PlayerIntent.GATHER_REQUEST;
        }
        if (lower.contains("status") || lower.contains("how are you") || lower.contains("what doing")) {
            return PlayerIntent.STATUS_QUERY;
        }
        if (lower.contains("stop") || lower.contains("cancel") || lower.contains("never mind")) {
            return PlayerIntent.CANCEL;
        }
        if (lower.contains("help") || lower.contains("what can you do")) {
            return PlayerIntent.HELP_REQUEST;
        }

        return PlayerIntent.GENERAL_QUERY;
    }

    public DialogueState getCurrentState() {
        return currentState;
    }
}

/**
 * Player intent classification.
 */
enum PlayerIntent {
    BUILD_REQUEST,
    GATHER_REQUEST,
    STATUS_QUERY,
    CANCEL,
    HELP_REQUEST,
    GENERAL_QUERY
}

/**
 * Dialogue states for MineWright AI.
 */
enum DialogueState {
    IDLE(false) {
        @Override
        public DialogueState getNextState(PlayerIntent intent) {
            return switch (intent) {
                case BUILD_REQUEST -> BUILD_PLANNING;
                case GATHER_REQUEST -> GATHER_PLANNING;
                case STATUS_QUERY -> REPORTING_STATUS;
                case HELP_REQUEST -> PROVIDING_HELP;
                default -> IDLE;
            };
        }

        @Override
        public void onEnter(String input, SteveEntity steve, DialogueMemory memory) {
            // Send greeting message
            sendToPlayer(steve, "Ready for your command!");
        }
    },

    BUILD_PLANNING(true) {
        @Override
        public DialogueState getNextState(PlayerIntent intent) {
            return switch (intent) {
                case CANCEL -> IDLE;
                case BUILD_REQUEST -> BUILD_PLANNING; // Refine plan
                default -> EXECUTING_TASK;
            };
        }

        @Override
        public void onEnter(String input, SteveEntity steve, DialogueMemory memory) {
            sendToPlayer(steve, "Planning build task: " + input);
            // Trigger LLM planning
            memory.setPendingTask(input);
        }
    },

    GATHER_PLANNING(true) {
        @Override
        public DialogueState getNextState(PlayerIntent intent) {
            return switch (intent) {
                case CANCEL -> IDLE;
                case GATHER_REQUEST -> GATHER_PLANNING; // Refine plan
                default -> EXECUTING_TASK;
            };
        }

        @Override
        public void onEnter(String input, SteveEntity steve, DialogueMemory memory) {
            sendToPlayer(steve, "Planning gather task: " + input);
            memory.setPendingTask(input);
        }
    },

    EXECUTING_TASK(true) {
        @Override
        public DialogueState getNextState(PlayerIntent intent) {
            return switch (intent) {
                case CANCEL -> IDLE;
                case STATUS_QUERY -> REPORTING_STATUS;
                default -> EXECUTING_TASK;
            };
        }

        @Override
        public void onEnter(String input, SteveEntity steve, DialogueMemory memory) {
            sendToPlayer(steve, "Working on it!");
            // Start task execution
        }
    },

    REPORTING_STATUS(true) {
        @Override
        public DialogueState getNextState(PlayerIntent intent) {
            return switch (intent) {
                case CANCEL -> IDLE;
                case BUILD_REQUEST -> BUILD_PLANNING;
                case GATHER_REQUEST -> GATHER_PLANNING;
                default -> IDLE;
            };
        }

        @Override
        public void onEnter(String input, SteveEntity steve, DialogueMemory memory) {
            String status = generateStatusReport(steve, memory);
            sendToPlayer(steve, status);
        }
    },

    PROVIDING_HELP(true) {
        @Override
        public DialogueState getNextState(PlayerIntent intent) {
            return IDLE;
        }

        @Override
        public void onEnter(String input, SteveEntity steve, DialogueMemory memory) {
            sendToPlayer(steve, """
                I can help you:
                - Build: "Build a house", "Make a tower"
                - Gather: "Mine 64 iron", "Get 32 oak logs"
                - Status: "What are you doing?", "Report status"
                - Cancel: "Stop", "Cancel task"
                """);
        }
    };

    private final boolean interruptible;

    DialogueState(boolean interruptible) {
        this.interruptible = interruptible;
    }

    public boolean isInterruptible() {
        return interruptible;
    }

    public abstract DialogueState getNextState(PlayerIntent intent);

    public abstract void onEnter(String input, SteveEntity steve, DialogueMemory memory);

    public void onExit(String input) {
        // Default: no action
    }

    protected void sendToPlayer(SteveEntity steve, String message) {
        // Send message to player's GUI
        steve.sendChatMessage(message);
    }

    protected String generateStatusReport(SteveEntity steve, DialogueMemory memory) {
        return String.format("""
            Status Report:
            - Current Task: %s
            - Progress: %d%%
            - Workers Active: %d
            - Resources: %s
            """,
            memory.getCurrentTask(),
            memory.getTaskProgress(),
            steve.getActiveWorkerCount(),
            getResourceSummary(steve)
        );
    }

    private String getResourceSummary(SteveEntity steve) {
        return "Iron: 32, Wood: 64, Stone: 128";
    }
}
```

### 5.3 Dialogue Memory System

```java
package com.steve.ai.dialogue;

import java.util.*;

/**
 * Memory system for dialogue context and history.
 */
public class DialogueMemory {
    private final List<String> conversationHistory;
    private final Map<String, Object> context;
    private final Map<String, Double> sentimentScores;

    private String currentTask;
    private int taskProgress;

    public DialogueMemory() {
        this.conversationHistory = new ArrayList<>();
        this.context = new HashMap<>();
        this.sentimentScores = new HashMap<>();
        this.taskProgress = 0;
    }

    public void addMessage(String role, String message) {
        conversationHistory.add(role + ": " + message);
        // Keep last 20 messages
        if (conversationHistory.size() > 20) {
            conversationHistory.remove(0);
        }
    }

    public List<String> getRecentHistory(int count) {
        int start = Math.max(0, conversationHistory.size() - count);
        return new ArrayList<>(conversationHistory.subList(start, conversationHistory.size()));
    }

    public void setContext(String key, Object value) {
        context.put(key, value);
    }

    public Object getContext(String key) {
        return context.get(key);
    }

    public void setSentiment(String category, double score) {
        sentimentScores.put(category, Math.max(0, Math.min(1, score)));
    }

    public double getSentiment(String category) {
        return sentimentScores.getOrDefault(category, 0.5);
    }

    public void setCurrentTask(String task) {
        this.currentTask = task;
        this.taskProgress = 0;
    }

    public void setPendingTask(String task) {
        this.currentTask = task;
    }

    public String getCurrentTask() {
        return currentTask != null ? currentTask : "No active task";
    }

    public void updateTaskProgress(int progress) {
        this.taskProgress = Math.max(0, Math.min(100, progress));
    }

    public int getTaskProgress() {
        return taskProgress;
    }

    /**
     * Generates conversation context for LLM prompt.
     */
    public String toLLMContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("Conversation History:\n");
        getRecentHistory(5).forEach(msg -> sb.append("  ").append(msg).append("\n"));
        sb.append("\n");
        sb.append("Current Task: ").append(getCurrentTask()).append("\n");
        sb.append("Task Progress: ").append(getTaskProgress()).append("%\n");
        return sb.toString();
    }
}
```

---

## 6. Foreman/Worker System Analysis

### 6.1 Current System Architecture

Based on the existing codebase, MineWright AI has:

```
ForemanEntity (Player-facing agent)
    ├── TaskPlanner (LLM-based planning)
    ├── ActionExecutor (Task execution)
    ├── AgentStateMachine (State management)
    └── CollaborativeBuildManager (Multi-agent coordination)

WorkerEntity (Task executor)
    ├── Action execution
    ├── Pathfinding
    └── Resource gathering
```

### 6.2 Identified Gaps

| Gap | Current State | Desired State | Impact |
|-----|--------------|---------------|--------|
| **Worker Assignment** | Manual/LLM-driven | Utility-based scoring | Workers assigned optimally based on context |
| **Task Prioritization** | Queue-based (FIFO) | Behavior tree selection | Reactive to urgent tasks (combat, resource shortage) |
| **Hierarchical Planning** | Flat task lists | HTN decomposition | Reusable building patterns |
| **Dialogue** | Simple GUI | State machine + memory | Natural conversations with context |
| **Coordination** | Spatial partitioning | Utility-based coordination | Dynamic worker reallocation |

### 6.3 Integration Points

```
┌────────────────────────────────────────────────────────────┐
│                    Foreman Entity                          │
├────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │        Utility-Based Worker Assignment               │  │
│  │  - Score each available worker for each task         │  │
│  │  - Consider: skill, proximity, inventory, load       │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │        Behavior Tree: Task Selection                 │  │
│  │  - Combat branch (highest priority)                  │  │
│  │  - Resource gathering branch                         │  │
│  │  - Building branch                                   │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │        HTN Planner: Task Decomposition               │  │
│  │  - "build house" → gather + clear + construct         │  │
│  │  - "mine iron" → explore + extract + return           │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │        Dialogue State Machine                        │  │
│  │  - Handle player input naturally                     │  │
│  │  - Remember conversation context                     │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │        Worker Assignment                             │  │
│  │  - Assign tasks based on utility scores              │  │
│  │  - Monitor worker progress                           │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
└────────────────────────────────────────────────────────────┘
```

---

## 7. Specific Improvements for Foreman/Worker System

### 7.1 Priority 1: Utility-Based Worker Assignment

**Problem:** Workers are assigned tasks without considering context (proximity, skill, current load).

**Solution:** Implement utility scoring for worker-task matching.

```java
package com.steve.ai.workers;

import com.steve.entity.WorkerEntity;
import com.steve.action.Task;
import java.util.*;

/**
 * Utility-based worker assignment system.
 */
public class WorkerAssignmentSystem {
    private final List<UtilityAction> assignmentActions;
    private final Map<String, ResponseCurve> curves;

    public WorkerEntity selectBestWorker(Task task, List<WorkerEntity> availableWorkers) {
        if (availableWorkers.isEmpty()) {
            return null;
        }

        return availableWorkers.stream()
            .max(Comparator.comparing(w -> scoreWorkerForTask(w, task)))
            .orElse(null);
    }

    private double scoreWorkerForTask(WorkerEntity worker, Task task) {
        double score = 0.0;
        double totalWeight = 0.0;

        // Distance consideration (closer is better)
        double distance = worker.position().distanceTo(task.getTargetPosition());
        double distanceScore = curves.get("distance_inverse").evaluate(distance);
        score += distanceScore * 1.0;
        totalWeight += 1.0;

        // Skill matching
        double skillScore = evaluateSkillMatch(worker, task);
        score += skillScore * 0.8;
        totalWeight += 0.8;

        // Current load (less loaded is better)
        double loadScore = 1.0 - worker.getCurrentLoad();
        score += loadScore * 0.6;
        totalWeight += 0.6;

        // Inventory consideration
        double inventoryScore = evaluateInventory(worker, task);
        score += inventoryScore * 0.5;
        totalWeight += 0.5;

        return totalWeight > 0 ? score / totalWeight : 0.0;
    }

    private double evaluateSkillMatch(WorkerEntity worker, Task task) {
        String taskType = task.getAction();
        String workerRole = worker.getRole();

        // Exact match = 1.0, partial = 0.5, mismatch = 0.0
        if (workerRole.equals(taskType)) {
            return 1.0;
        }
        if (isRelatedRole(workerRole, taskType)) {
            return 0.5;
        }
        return 0.0;
    }

    private double evaluateInventory(WorkerEntity worker, Task task) {
        // Check if worker has required materials
        Map<String, Integer> required = task.getRequiredMaterials();
        if (required.isEmpty()) {
            return 1.0; // No materials needed
        }

        int hasRequired = 0;
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            if (worker.getInventory().getItemCount(entry.getKey()) >= entry.getValue()) {
                hasRequired++;
            }
        }

        return (double) hasRequired / required.size();
    }

    private boolean isRelatedRole(String role, String taskType) {
        // Define role-task relationships
        Map<String, Set<String>> relatedRoles = Map.of(
            "builder", Set.of("place", "construct", "build"),
            "miner", Set.of("mine", "gather", "extract"),
            "combat", Set.of("attack", "defend", "patrol")
        );

        return relatedRoles.getOrDefault(role, Collections.emptySet())
            .contains(taskType);
    }
}
```

### 7.2 Priority 2: Reactive Behavior Tree for Task Selection

**Problem:** Foreman doesn't react to urgent events (mob attacks, resource shortage).

**Solution:** Event-driven behavior tree for reactive task prioritization.

```java
package com.steve.ai.bt;

import com.steve.entity.ForemanEntity;
import com.steve.event.*;
import java.util.*;

/**
 * Reactive behavior tree for foreman task selection.
 */
public class ForemanBehaviorTree {
    private final BehaviorNode root;
    private final EventBus eventBus;

    public ForemanBehaviorTree(ForemanEntity foreman, EventBus eventBus) {
        this.eventBus = eventBus;
        this.root = buildTree(foreman);
        registerEventHandlers();
    }

    private BehaviorNode buildTree(ForemanEntity foreman) {
        SelectorNode root = new SelectorNode();

        // 1. Combat response (highest priority)
        SequenceNode combatBranch = new SequenceNode();
        combatBranch.addChild(new EnemyProximityCondition(foreman, 16.0));
        combatBranch.addChild(new AvailableCombatWorkerCondition());
        combatBranch.addChild(new AssignCombatAction(foreman));
        root.addChild(combatBranch);

        // 2. Critical resource shortage
        SequenceNode resourceBranch = new SequenceNode();
        resourceBranch.addChild(new ResourceShortageCondition(foreman));
        resourceBranch.addChild(new AvailableWorkerCondition());
        resourceBranch.addChild(new AssignGatheringAction(foreman));
        root.addChild(resourceBranch);

        // 3. Player command (via dialogue)
        SequenceNode commandBranch = new SequenceNode();
        commandBranch.addChild(new PendingCommandCondition(foreman));
        commandBranch.addChild(new ProcessCommandAction(foreman));
        root.addChild(commandBranch);

        // 4. Queued build tasks
        SequenceNode buildBranch = new SequenceNode();
        buildBranch.addChild(new QueuedBuildTaskCondition(foreman));
        buildBranch.addChild(new MaterialsAvailableCondition(foreman));
        buildBranch.addChild(new AssignBuildAction(foreman));
        root.addChild(buildBranch);

        return root;
    }

    public void tick() {
        root.tick();
    }

    private void registerEventHandlers() {
        // Subscribe to combat events
        eventBus.subscribe(CombatEvent.class, event -> {
            // Trigger immediate re-evaluation
            tick();
        });

        // Subscribe to resource events
        eventBus.subscribe(ResourceChangeEvent.class, event -> {
            if (event.isShortage()) {
                tick();
            }
        });

        // Subscribe to command events
        eventBus.subscribe(PlayerCommandEvent.class, event -> {
            tick();
        });
    }
}
```

### 7.3 Priority 3: Dialogue State Machine for Player Communication

**Problem:** Limited communication with player (simple GUI input).

**Solution:** State machine-based dialogue with memory and context.

```java
package com.steve.ai.dialogue;

import com.steve.entity.ForemanEntity;
import com.steve.llm.OpenAIClient;
import java.util.*;

/**
 * Enhanced dialogue system for natural player communication.
 */
public class ForemanDialogueSystem {
    private final ForemanEntity foreman;
    private final DialogueStateMachine stateMachine;
    private final DialogueMemory memory;
    private final OpenAIClient llmClient;

    public ForemanDialogueSystem(ForemanEntity foreman, OpenAIClient llmClient) {
        this.foreman = foreman;
        this.llmClient = llmClient;
        this.memory = new DialogueMemory();
        this.stateMachine = new DialogueStateMachine(foreman, foreman.getEventBus());
    }

    public void handlePlayerMessage(String message) {
        // Add to conversation history
        memory.addMessage("player", message);

        // Update sentiment
        updateSentiment(message);

        // Process through state machine
        stateMachine.handlePlayerInput(message);

        // For complex queries, use LLM
        if (shouldUseLLM(message)) {
            String response = generateLLMResponse(message);
            sendToPlayer(response);
            memory.addMessage("foreman", response);
        }
    }

    private boolean shouldUseLLM(String message) {
        // Use LLM for:
        // - Complex questions
        // - Ambiguous commands
        // - Status summaries
        // - Help requests
        String lower = message.toLowerCase();
        return lower.contains("explain") ||
               lower.contains("why") ||
               lower.contains("how") ||
               lower.contains("status") ||
               lower.contains("summary") ||
               lower.contains("help");
    }

    private String generateLLMResponse(String message) {
        // Build prompt with context
        String prompt = buildPromptWithContext(message);

        // Call LLM
        return llmClient.generateResponse(prompt);
    }

    private String buildPromptWithContext(String message) {
        return String.format("""
            You are Steve, a Minecraft AI assistant. You coordinate worker units to build, mine, and gather resources.

            Current Context:
            %s

            Player says: "%s"

            Respond naturally and concisely (max 2 sentences).
            """,
            memory.toLLMContext(),
            message
        );
    }

    private void updateSentiment(String message) {
        // Simple sentiment analysis
        // Advanced version would use NLP model
        String lower = message.toLowerCase();

        if (lower.contains("thank") || lower.contains("good") || lower.contains("great")) {
            memory.setSentiment("satisfaction", 1.0);
        } else if (lower.contains("bad") || lower.contains("wrong") || lower.contains("stop")) {
            memory.setSentiment("satisfaction", 0.0);
        }
    }

    private void sendToPlayer(String message) {
        foreman.sendChatMessage(message);
    }

    public void reportStatus() {
        String status = generateStatusReport();
        sendToPlayer(status);
        memory.addMessage("foreman", status);
    }

    private String generateStatusReport() {
        return String.format("""
            [Status Report]
            Active Workers: %d / %d
            Current Tasks: %s
            Resources: %s
            """,
            foreman.getActiveWorkerCount(),
            foreman.getTotalWorkerCount(),
            memory.getCurrentTask(),
            getResourceSummary()
        );
    }

    private String getResourceSummary() {
        return String.format("Wood: %d, Stone: %d, Iron: %d",
            foreman.getResourceCount("wood"),
            foreman.getResourceCount("stone"),
            foreman.getResourceCount("iron")
        );
    }
}
```

### 7.4 Priority 4: HTN for Common Task Patterns

**Problem:** Every "build a house" command requires LLM planning (expensive).

**Solution:** HTN domain for common building patterns.

```java
package com.steve.ai.htn;

import com.steve.action.Task;
import java.util.*;

/**
 * HTN domain for common Minecraft tasks.
 */
public class MinecraftHTNDomain extends HTNDomain {
    @Override
    public void loadDefaultDomain() {
        super.loadDefaultDomain();
        loadBuildingPatterns();
        loadMiningPatterns();
    }

    private void loadBuildingPatterns() {
        // House pattern
        addMethod("build_house",
            HTNMethod.builder("house_5x5_basic", "build_house")
                .addSubtask(createGatherTask("oak_log", 16))
                .addSubtask(createCraftTask("oak_planks", 64))
                .addSubtask(createPathfindTask("build_site"))
                .addSubtask(createClearTask(5, 3, 5))
                .addSubtask(createBuildTask("house", "oak_planks", 5, 3, 5))
                .priority(100)
                .build()
        );

        // Tower pattern
        addMethod("build_tower",
            HTNMethod.builder("tower_3x10_stone", "build_tower")
                .addSubtask(createGatherTask("cobblestone", 90))
                .addSubtask(createPathfindTask("build_site"))
                .addSubtask(createBuildTask("tower", "cobblestone", 3, 10, 3))
                .priority(100)
                .build()
        );

        // Farm pattern
        addMethod("build_farm",
            HTNMethod.builder("farm_8x8_wheat", "build_farm")
                .addSubtask(createGatherTask("dirt", 64))
                .addSubtask(createGatherTask("water_bucket", 1))
                .addSubtask(createPathfindTask("farm_site"))
                .addSubtask(createBuildTask("farm", "dirt", 8, 1, 8))
                .addSubtask(createPlaceTask("water", 0, 0, 0))
                .addSubtask(createPlantTask("wheat_seeds", 64))
                .priority(100)
                .build()
        );
    }

    private void loadMiningPatterns() {
        // Strip mining pattern
        addMethod("mine_coal",
            HTNMethod.builder("strip_mine_coal", "mine_coal")
                .addSubtask(createPathfindTask("y_90"))
                .addSubtask(createMineTask("coal_ore", 32))
                .addSubtask(createReturnTask())
                .priority(100)
                .build()
        );

        // Cave mining pattern
        addMethod("mine_iron",
            HTNMethod.builder("cave_mine_iron", "mine_iron")
                .addSubtask(createPathfindTask("cave_entrance"))
                .addSubtask(createExploreTask("cave"))
                .addSubtask(createMineTask("iron_ore", 16))
                .addSubtask(createReturnTask())
                .priority(90)
                .build()
        );
    }

    // Helper methods for creating tasks
    private HTNTask createGatherTask(String block, int quantity) {
        return new HTNTask("gather", HTNTask.Type.PRIMITIVE,
            Map.of("block", block, "quantity", quantity));
    }

    private HTNTask createCraftTask(String item, int quantity) {
        return new HTNTask("craft", HTNTask.Type.PRIMITIVE,
            Map.of("item", item, "quantity", quantity));
    }

    private HTNTask createPathfindTask(String target) {
        return new HTNTask("pathfind", HTNTask.Type.PRIMITIVE,
            Map.of("target", target));
    }

    private HTNTask createClearTask(int width, int height, int depth) {
        return new HTNTask("clear_area", HTNTask.Type.PRIMITIVE,
            Map.of("width", width, "height", height, "depth", depth));
    }

    private HTNTask createBuildTask(String structure, String material, int w, int h, int d) {
        return new HTNTask("build", HTNTask.Type.PRIMITIVE,
            Map.of("structure", structure, "material", material,
                   "width", w, "height", h, "depth", d));
    }

    private HTNTask createPlaceTask(String block, int x, int y, int z) {
        return new HTNTask("place", HTNTask.Type.PRIMITIVE,
            Map.of("block", block, "x", x, "y", y, "z", z));
    }

    private HTNTask createPlantTask(String seed, int quantity) {
        return new HTNTask("plant", HTNTask.Type.PRIMITIVE,
            Map.of("seed", seed, "quantity", quantity));
    }

    private HTNTask createMineTask(String ore, int quantity) {
        return new HTNTask("mine", HTNTask.Type.PRIMITIVE,
            Map.of("block", ore, "quantity", quantity));
    }

    private HTNTask createExploreTask(String location) {
        return new HTNTask("explore", HTNTask.Type.PRIMITIVE,
            Map.of("location", location));
    }

    private HTNTask createReturnTask() {
        return new HTNTask("return", HTNTask.Type.PRIMITIVE, Map.of());
    }
}
```

---

## 8. Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Goals:** Implement core behavior tree and utility AI infrastructure.

#### Week 1: Behavior Tree Foundation
- [ ] Create `ai.bt` package
- [ ] Implement core BT nodes (Selector, Sequence, Condition, Action)
- [ ] Implement event-driven BT base classes
- [ ] Create basic BT for worker assignment
- [ ] Write unit tests for BT nodes

#### Week 2: Utility AI Foundation
- [ ] Create `ai.utility` package
- [ ] Implement response curves (Linear, Logistic, Exponential)
- [ ] Implement UtilityAction and Consideration classes
- [ ] Create WorkerContext for utility evaluation
- [ ] Implement WorkerUtilitySystem
- [ ] Write unit tests for utility scoring

**Deliverables:**
- Working behavior tree execution
- Utility scoring system
- Basic worker assignment based on utility

### Phase 2: Worker Assignment (Week 3-4)

**Goals:** Replace manual worker assignment with utility-based selection.

#### Week 3: Worker Assignment
- [ ] Implement WorkerAssignmentSystem with utility scoring
- [ ] Add worker proximity consideration
- [ ] Add skill matching logic
- [ ] Add inventory consideration
- [ ] Integrate with existing WorkerManager

#### Week 4: Testing & Tuning
- [ ] Create test scenarios for worker assignment
- [ ] Tune utility weights for optimal assignment
- [ ] Add metrics/monitoring for assignment quality
- [ ] Performance profiling

**Deliverables:**
- Context-aware worker assignment
- 30% improvement in worker efficiency
- Metrics dashboard

### Phase 3: Reactive Behavior Tree (Week 5-6)

**Goals:** Add event-driven BT for reactive task selection.

#### Week 5: BT Integration
- [ ] Implement ForemanBehaviorTree
- [ ] Add combat response branch
- [ ] Add resource shortage branch
- [ ] Add build task branch
- [ ] Integrate with existing EventBus

#### Week 6: Event Handling
- [ ] Implement event-driven BT nodes
- [ ] Add combat event subscription
- [ ] Add resource change event subscription
- [ ] Add player command event subscription
- [ ] Testing reactive behavior

**Deliverables:**
- Reactive foreman that responds to events
- Combat interrupts lower-priority tasks
- Resource shortage triggers gathering

### Phase 4: Dialogue System (Week 7-8)

**Goals:** Implement state machine-based dialogue with memory.

#### Week 7: Dialogue State Machine
- [ ] Create `ai.dialogue` package
- [ ] Implement DialogueStateMachine
- [ ] Define dialogue states (IDLE, PLANNING, EXECUTING, REPORTING)
- [ ] Implement intent classification
- [ ] Add DialogueMemory for conversation history

#### Week 8: LLM Integration
- [ ] Integrate LLM for complex queries
- [ ] Implement context building for LLM prompts
- [ ] Add status report generation
- [ ] Add sentiment tracking
- [ ] Testing dialogue flows

**Deliverables:**
- Natural dialogue with player
- Conversation memory and context
- Status reporting and help

### Phase 5: HTN Planning (Week 9-11)

**Goals:** Add HTN planner for common task patterns.

#### Week 9: HTN Core
- [ ] Create `ai.htn` package
- [ ] Implement HTNTask, HTNMethod, HTNDomain
- [ ] Implement HTN decomposition logic
- [ ] Add method preconditions checking

#### Week 10: Minecraft Domain
- [ ] Implement MinecraftHTNDomain
- [ ] Add building patterns (house, tower, farm)
- [ ] Add mining patterns (strip mine, cave mine)
- [ ] Add crafting patterns

#### Week 11: Integration & Testing
- [ ] Integrate with existing TaskPlanner
- [ ] Add LLM fallback for novel tasks
- [ ] Implement plan caching
- [ ] Testing and refinement

**Deliverables:**
- HTN planner for common tasks
- 40-60% reduction in LLM API calls
- Reusable task patterns

### Phase 6: Polish & Optimization (Week 12-13)

**Goals:** Performance optimization and polish.

#### Week 12: Optimization
- [ ] Profile utility scoring performance
- [ ] Optimize BT traversal
- [ ] Add caching for expensive calculations
- [ ] Implement lazy evaluation

#### Week 13: Polish
- [ ] Add comprehensive logging
- [ ] Create debugging tools
- [ ] Write documentation
- [ ] User testing and feedback

**Deliverables:**
- Optimized performance (<1ms per tick)
- Comprehensive documentation
- Debugging tools

---

## Summary & Key Takeaways

### Recommended Implementation Priority

| Priority | System | Impact | Effort | Timeline |
|----------|--------|--------|--------|----------|
| **1** | Utility AI for Worker Assignment | High | Medium | 2 weeks |
| **2** | Reactive Behavior Tree | High | Medium | 2 weeks |
| **3** | Dialogue State Machine | Medium | Medium | 2 weeks |
| **4** | HTN Planning | Medium | High | 3 weeks |
| **5** | GOAP (Alternative to HTN) | Low | Medium | - |

### Critical Success Factors

1. **Start with Utility AI** - Highest ROI for worker assignment
2. **Add Event-Driven BT** - Essential for reactive behavior
3. **Skip GOAP** - HTN is better fit for structured building tasks
4. **Enhance Dialogue** - State machine + LLM hybrid is best approach
5. **Cache Aggressively** - Reduce LLM calls with HTN and plan caching

### Architecture Recommendation

```
Three-Layer Architecture:
┌──────────────────────────────────────────────────────────┐
│  Layer 1: Dialogue & Command Understanding              │
│  - Dialogue State Machine handles player input          │
│  - Intent classification routes to appropriate handler  │
│  - LLM used for complex/ambiguous commands              │
└──────────────────────────────────────────────────────────┘
                           ↓
┌──────────────────────────────────────────────────────────┐
│  Layer 2: Task Planning & Prioritization                │
│  - HTN for common patterns (build, mine, craft)         │
│  - Behavior Tree for reactive selection                 │
│  - LLM fallback for novel tasks                         │
└──────────────────────────────────────────────────────────┘
                           ↓
┌──────────────────────────────────────────────────────────┐
│  Layer 3: Worker Assignment & Execution                 │
│  - Utility AI scores worker-task pairs                   │
│  - Workers assigned based on context                    │
│  - Progress monitoring and reassignment                 │
└──────────────────────────────────────────────────────────┘
```

### Expected Improvements

| Metric | Current | With Improvements | Gain |
|--------|---------|-------------------|------|
| LLM API Calls | 100% | 40-60% | 40-60% reduction |
| Worker Efficiency | Baseline | 130% | 30% improvement |
| Response Time | 3-5s | <1s (cached) | 5x faster |
| Reactivity | Low | High | Event-driven |
| Dialogue Quality | Basic | Natural | State machine + LLM |

---

## Sources

### Behavior Trees
- [Python Behavior Tree Programming Complete Guide](https://m.blog.csdn.net/gitblog_00397/article/details/156750426)
- [Using Behavior Trees to Implement Game AI](https://m.blog.csdn.net/zhwei_87/article/details/37818039)
- [Behaviac Framework: Complete Game AI Solution](https://m.blog.csdn.net/gitblog_00292/article/details/155280101)

### Utility AI
- [Open-world Enemy AI in Mafia III - Game AI Pro 2](https://www.gamedeveloper.com/programming/open-world-enemy-ai-in-mafia-iii)
- [Common Issues with Behavior Trees - Epic Games](https://dev.epicgames.com/documentation/en-us/unreal-engine)
- [Utility AI: Weight-Based Game AI](https://blog.csdn.net/qq_34556414/article/details/108384960)

### GOAP
- [GOAP Complete Tutorial in Unity](https://m.blog.csdn.net/gitblog_00933/article/details/141454404)
- [GOAP AI Implementation in Unity](https://m.blog.csdn.net/weixin_50702814/article/details/144515041)
- [GOAP for Unity (Open Source)](https://gitee.com/bin384401056/GOAP)

### HTN
- [Task Planning and Reasoning: HTN](https://m.blog.csdn.net/qq_43625558/article/details/155274884)
- [Game AI: From FSM to HTN](https://m.blog.csdn.net/qq_36460660/article/details/145621543)
- [Game AI Technology Comparison: FSM, HFSM, BT, GOAP, HTN](https://www.cnblogs.com/moonhigh/p/17999544)

### Dialogue Systems
- [Tongyi Qianwen 2.5-7B Game NPC Dialogue System](https://m.blog.csdn.net/qq_55787617/article/details/145378921)
- [NPC Dynamic Decision Making and Emotion Simulation](https://m.blog.csdn.net/qq_67882122/article/details/145425566)
- [C++ Genshin Impact Dialogue System Development](https://m.blog.csdn.net/qq_62432825/article/details/178742819)
- [AI in Games: Enriching Game Content](https://www.researchgate.net/publication/383177041)

---

**Document Version:** 1.0
**Last Updated:** February 27, 2026
**Author:** Claude Research Agent
**Project:** MineWright AI (MineWright)
