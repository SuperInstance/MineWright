# Game AI Decision-Making Systems Research Report 2025

**Project:** MineWright Action Execution System
**Date:** February 27, 2025
**Author:** Orchestrator Research Team
**Version:** 1.0

---

## Executive Summary

This report presents comprehensive research on modern game AI behavior trees and decision-making systems, with specific recommendations for enhancing the MineWright action execution system. The research covers five major paradigms: Behavior Trees, Goal-Oriented Action Planning (GOAP), Hierarchical Task Networks (HTN), Utility AI, and Finite State Machines (FSM), along with integration patterns for hybrid architectures.

**Key Finding:** The MineWright project already implements a solid foundation with a state machine, plugin architecture, and async LLM integration. However, it can significantly benefit from incorporating Utility AI for action selection, HTN for hierarchical planning, and event-driven behavior trees for reactive decision-making.

---

## Table of Contents

1. [Behavior Trees](#1-behavior-trees)
2. [GOAP and HTN Planning](#2-goap-and-htn-planning)
3. [Utility AI](#3-utility-ai)
4. [Finite State Machines](#4-finite-state-machines)
5. [Integration Patterns](#5-integration-patterns)
6. [MineWright Analysis](#6-minewright-current-system-analysis)
7. [Recommendations](#7-recommendations-for-minewright)
8. [Implementation Roadmap](#8-implementation-roadmap)
9. [References](#9-references)

---

## 1. Behavior Trees

### 1.1 Core Concepts

Behavior Trees (BTs) are hierarchical, tree-structured graphs that model decision-making and action execution. They consist of:

- **Nodes**: Leaf nodes (actions, conditions) and composite nodes (sequences, selectors, parallels)
- **Traversal**: Tick-based evaluation from root to leaves
- **Status**: Each node returns SUCCESS, FAILURE, or RUNNING
- **Reactive**: Continuously re-evaluated each frame/tick

### 1.2 Modern BT Patterns (2024-2025)

#### 1.2.1 Event-Driven Behavior Trees

Traditional BTs tick continuously, which is inefficient. Modern implementations use event-driven patterns:

```java
// Event-driven BT node
public interface EventDrivenNode {
    void onEvent(Event event);
    Status tick();
    void subscribe(EventBus eventBus);
}

// Example: React to player proximity
public class ProximityCondition extends ConditionNode {
    @Override
    public void onEvent(Event event) {
        if (event instanceof PlayerProximityEvent) {
            this.lastProximity = ((PlayerProximityEvent) event).getDistance();
        }
    }

    @Override
    public Status tick() {
        return lastProximity < 10.0 ? Status.SUCCESS : Status.FAILURE;
    }
}
```

**Benefits:**
- Reduced CPU usage (no continuous ticking)
- Faster reaction to critical events
- Better for multi-agent systems

#### 1.2.2 Dynamic Tree Modification

Modern BTs support runtime modifications:

```java
// Dynamic BT builder
public class DynamicBehaviorTree {
    public void insertNode(NodePath path, BehaviorNode node);
    public void removeNode(NodePath path);
    public void replaceNode(NodePath path, BehaviorNode newNode);
    public void enableSubtree(String subtreeId);
    public void disableSubtree(String subtreeId);
}

// Example: Add new behavior at runtime
tree.insertNode(
    NodePath.parse("root.combat"),
    new ShieldAction()
);
```

**Use Cases:**
- Learning from player interactions
- Adaptive difficulty
- Runtime behavior expansion

#### 1.2.3 Utility-Decorated Behavior Trees

Combining BT structure with utility scoring:

```java
public class UtilitySelector extends SelectorNode {
    private List<UtilityScore> childScores;

    @Override
    protected BehaviorNode selectChild() {
        // Sort children by utility score instead of fixed order
        return children.stream()
            .max(Comparator.comparing(this::calculateUtility))
            .orElse(null);
    }

    private double calculateUtility(BehaviorNode node) {
        // Contextual utility calculation
        return node.getUtilityScore(context);
    }
}
```

### 1.3 Hierarchical Task Networks in BTs

HTN-style decomposition within BT nodes:

```java
public class TaskDecomposerNode extends DecoratorNode {
    @Override
    public Status tick() {
        if (child == null) {
            // Decompose high-level task into sub-tasks
            List<Task> subtasks = decomposite(task);
            child = new SequenceNode(subtasks);
        }
        return child.tick();
    }
}
```

### 1.4 BT Implementation Recommendations for MineWright

**Priority: HIGH**

The current action queue system could be enhanced with a behavior tree layer:

1. **Replace linear task queue with BT-based selection**
   - Current: Queue-based sequential execution
   - Proposed: BT with reactive re-evaluation

2. **Add event-driven BT nodes**
   - React to combat events immediately
   - Respond to resource changes without waiting for current action

3. **Implement utility-decorated selectors**
   - Score multiple available actions
   - Select highest-utility action dynamically

---

## 2. GOAP and HTN Planning

### 2.1 Goal-Oriented Action Planning (GOAP)

#### 2.1.1 Core GOAP Architecture

```
World State → Goals → Actions → Planner → Execution
     ↓           ↓         ↓         ↓          ↓
  Beliefs    Desired Preconditions A* Plan    Actions
              State                Search
```

**Components:**
- **World State**: Agent's belief about the world (key-value pairs)
- **Goals**: Desired states (e.g., `has_weapon: true`)
- **Actions**: Preconditions and effects with costs
- **Planner**: A* or greedy DFS to find action sequence

#### 2.1.2 Modern GOAP Implementations

**Unity GOAP Framework (2024):**
```csharp
public class GoapAction {
    public HashSet<KeyValuePair<string, object>> Preconditions;
    public HashSet<KeyValuePair<string, object>> Effects;
    public float Cost;  // Action cost for pathfinding

    public virtual bool Perform(GameObject agent);
    public virtual bool IsProcced();
}
```

**Key Features:**
- Action cost optimization for path planning
- NavMesh integration for spatial reasoning
- Dynamic replanning when world state changes

#### 2.1.3 Plan Caching Strategies

```java
public class PlanCache {
    private Map<StateSignature, List<Action>> cache;
    private int maxCacheSize = 100;

    public List<Action> plan(WorldState currentState, Goal goal) {
        StateSignature signature = computeSignature(currentState, goal);

        return cache.computeIfAbsent(signature, sig -> {
            // Run actual planner
            return aStarPlan(currentState, goal);
        });
    }

    private StateSignature computeSignature(WorldState state, Goal goal) {
        // Hash relevant state variables
        return StateSignature.of(state.getRelevantKeys(), goal.getKey());
    }
}
```

**Cache Invalidation:**
- Time-based expiration
- World state delta threshold
- Goal completion

#### 2.1.4 Replanning Strategies

1. **Lazy Replanning**: Replan only when current action fails
2. **Periodic Replanning**: Replan every N ticks
3. **Event-Driven Replanning**: Replan on significant world changes
4. **Anytime Planning**: Return best plan found so far when interrupted

### 2.2 Hierarchical Task Networks (HTN)

#### 2.2.1 HTN vs GOAP Comparison

| Aspect | GOAP | HTN |
|--------|------|-----|
| Planning Direction | Backward chaining | Forward decomposition |
| Control | Emergent, unpredictable | Designer-controlled |
| Best For | Open-ended gameplay | Narrative, structured missions |
| Complexity | Simpler implementation | More complex hierarchies |
| Industry Usage | Declining (too unpredictable) | Growing (Horizon Zero Dawn) |

#### 2.2.2 Modern HTN Advances

**HTN + Multi-Agent Reinforcement Learning (pyHIPOP+, 2025):**
```python
class HybridHTNPlanner:
    def __init__(self):
        self.symbolic_planner = HTNPlanner()
        self.neural_policy = MARLPolicy()

    def plan(self, state, task):
        # Use neural network to guide decomposition
        method = self.neural_policy.predict_method(state, task)
        return self.symbolic_planner.decompose(state, task, method)
```

**HTN + Monte Carlo Tree Search:**
```java
public class MCTSHTNPlanner {
    public Plan plan(CompoundTask task, WorldState state) {
        // Pre-exploration phase with MCTS
        Method bestMethod = mctsSelectMethod(task, state);

        // Decompose using selected method
        return decompose(task, state, bestMethod);
    }
}
```

**LLM-Powered HTN (2024-2025):**
- Tree-of-Thought (ToT) for multi-path expansion
- Self-projection during planning phase
- Tool-aware decomposition (considers available actions)

#### 2.2.3 HTN Structure Example

```java
public interface Task {
    boolean isPrimitive();
}

public class CompoundTask implements Task {
    private List<Method> methods;  // Alternative ways to accomplish task

    @Override
    public boolean isPrimitive() {
        return false;
    }
}

public class Method {
    private List<Task> subtasks;
    private Predicate<WorldState> precondition;

    public List<Task> decompose(WorldState state) {
        return precondition.test(state) ? subtasks : null;
    }
}

public class PrimitiveTask implements Task {
    private Action action;

    @Override
    public boolean isPrimitive() {
        return true;
    }
}
```

### 2.3 GOAP/HTN Recommendations for MineWright

**Priority: MEDIUM-HIGH**

The current LLM-based planning is powerful but slow. HTN could provide:

1. **Hybrid LLM + HTN Planning**
   ```java
   public class HybridPlanner {
       public List<Task> plan(String command) {
           // Try HTN first (fast, deterministic)
           List<Task> tasks = htnPlanner.plan(command);
           if (tasks != null) return tasks;

           // Fallback to LLM (slow, flexible)
           return llmPlanner.planAsync(command);
       }
   }
   ```

2. **Hierarchical Action Decomposition**
   - High-level LLM planning ("build a house")
   - Mid-level HTN decomposition ("gather materials", "construct walls")
   - Low-level BT execution ("move to block", "place block")

3. **Plan Caching for Common Commands**
   - Cache LLM responses for repeated commands
   - Signature-based cache keys
   - Invalidate on world state changes

---

## 3. Utility AI

### 3.1 Core Utility AI Concepts

Utility AI scores actions based on multiple considerations:

```
Utility = f(consideration1, consideration2, ..., considerationN)
```

**Key Features:**
- No rigid state transitions
- Smooth behavior blending
- Easy to tune and debug
- Highly context-aware

### 3.2 Curve-Based Evaluation

#### 3.2.1 Response Curves

```java
public interface ResponseCurve {
    double evaluate(double input);
}

public class LinearCurve implements ResponseCurve {
    private final double m;  // slope
    private final double b;  // y-intercept

    @Override
    public double evaluate(double x) {
        return m * x + b;
    }
}

public class ExponentialCurve implements ResponseCurve {
    private final double base;
    private final double exponent;

    @Override
    public double evaluate(double x) {
        return Math.pow(base, exponent * x);
    }
}

public class LogisticCurve implements ResponseCurve {
    private final double k;
    private final double x0;

    @Override
    public double evaluate(double x) {
        return 1.0 / (1.0 + Math.exp(-k * (x - x0)));
    }
}
```

#### 3.2.2 Consideration System

```java
public class Consideration {
    private final String name;
    private final Function<Context, Double> inputExtractor;
    private final ResponseCurve curve;
    private final double weight;

    public double score(Context context) {
        double input = inputExtractor.apply(context);
        double normalized = curve.evaluate(input);
        return normalized * weight;
    }
}

public class UtilityAction {
    private final String actionId;
    private final List<Consideration> considerations;

    public double calculateUtility(Context context) {
        return considerations.stream()
            .mapToDouble(c -> c.score(context))
            .average()
            .orElse(0.0);
    }
}
```

### 3.3 Contextual Scoring

```java
public class Context {
    private final WorldState worldState;
    private final AgentState agentState;
    private final Map<String, Object> variables;

    public double getDistanceToTarget() {
        return (double) variables.getOrDefault("distance_to_target", 0.0);
    }

    public double getHealthPercentage() {
        return agentState.getHealth() / agentState.getMaxHealth();
    }

    public boolean hasResource(String resource) {
        return worldState.hasResource(resource, agentState);
    }
}
```

### 3.4 Multi-Consideration Decision Example

```java
// Combat action selection
public class CombatUtilitySystem {
    private final List<UtilityAction> actions = List.of(
        new UtilityAction("attack")
            .addConsideration("enemy_distance", new LogisticCurve(2.0, 5.0), 1.0)
            .addConsideration("weapon_ready", new BinaryCurve(), 1.0)
            .addConsideration("health_low", new InverseLinearCurve(), 0.5),

        new UtilityAction("retreat")
            .addConsideration("health_low", new LinearCurve(2.0, 0.0), 1.0)
            .addConsideration("enemy_distance", new InverseExponentialCurve(), 0.8)
            .addConsideration("has_escape_route", new BinaryCurve(), 1.0),

        new UtilityAction("heal")
            .addConsideration("health_low", new LinearCurve(3.0, -0.5), 1.0)
            .addConsideration("has_health_potion", new BinaryCurve(), 1.0)
            .addConsideration("in_combat", new InverseLinearCurve(), 0.7)
    );

    public String selectBestAction(Context context) {
        return actions.stream()
            .max(Comparator.comparing(a -> a.calculateUtility(context)))
            .map(UtilityAction::getId)
            .orElse("idle");
    }
}
```

### 3.5 Weight Tuning Strategies

1. **Manual Tuning**: Designer adjusts weights based on observation
2. **Genetic Algorithms**: Evolve weights based on success metrics
3. **Reinforcement Learning**: Learn optimal weights through trial and error
4. **Online Learning**: Adapt weights based on recent performance

### 3.6 Utility AI Recommendations for MineWright

**Priority: HIGH**

The current action selection (LLM-generated sequential tasks) lacks dynamic reactivity:

1. **Add Utility AI Layer for Reactive Decisions**
   ```java
   public class UtilityActionSelector {
       public Task selectReactiveAction(Context context) {
           // When interrupted, score alternative actions
           if (context.isUnderAttack()) {
               return evaluateCombatActions(context);
           }
           if (context.isBlockageDetected()) {
               return evaluateNavigationActions(context);
           }
           return null;  // Continue current task
       }
   }
   ```

2. **Context-Aware Task Prioritization**
   ```java
   public class TaskQueueManager {
       private final Queue<Task> queue;
       private final UtilityScorer scorer;

       public Task getNextTask(Context context) {
           // Re-score tasks based on current context
           return queue.stream()
               .max(Comparator.comparing(t -> scorer.score(t, context)))
               .orElse(null);
       }
   }
   ```

3. **Curve-Based Resource Management**
   - Distance to target evaluation
   - Health/low-priority thresholds
   - Time-of-day considerations

---

## 4. Finite State Machines

### 4.1 Modern FSM Variations

#### 4.1.1 Hierarchical FSM (HFSM)

```java
public class HierarchicalState {
    private final String name;
    private HierarchicalState parent;
    private final List<HierarchicalState> children;
    private final Map<Event, HierarchicalState> transitions;

    public void handleEvent(Event event) {
        // Try local transitions first
        HierarchicalState target = transitions.get(event);
        if (target != null) {
            transitionTo(target);
            return;
        }

        // Bubble up to parent
        if (parent != null) {
            parent.handleEvent(event);
        }
    }
}
```

**Example: Combat HFSM**
```
Combat (parent)
├── MeleeAttack
│   ├── LightAttack
│   └── HeavyAttack
├── RangedAttack
│   ├── BowAttack
│   └── SpellAttack
└── Defensive
    ├── Block
    └── Dodge
```

#### 4.1.2 Pushdown Automata (Stack-Based FSM)

```java
public class PushdownAutomaton {
    private final Stack<State> stateStack;

    public void pushState(State state) {
        if (currentState != null) {
            currentState.onExit();
            stateStack.push(currentState);
        }
        currentState = state;
        currentState.onEnter();
    }

    public void popState() {
        currentState.onExit();
        currentState = stateStack.pop();
        currentState.onEnter();
    }

    public void handleEvent(Event event) {
        // Interrupt with push, resume with pop
        if (event.type == INTERRUPT) {
            pushState(event.interruptState);
        }
    }
}
```

**Use Cases:**
- Interruptible actions (combat interrupts mining)
- Nested dialogues
- State history tracking

#### 4.1.3 Concurrent States

```java
public class ConcurrentStateMachine {
    private final List<StateMachine> parallelStates;

    public void tick() {
        // All states tick in parallel
        parallelStates.forEach(StateMachine::tick);
    }

    public void handleEvent(Event event) {
        // Event broadcasted to all states
        parallelStates.forEach(sm -> sm.handleEvent(event));
    }
}
```

**Example: Movement + Action**
```java
ConcurrentStateMachine sm = new ConcurrentStateMachine();
sm.addStateMachine(new MovementStateMachine());  // Controls movement
sm.addStateMachine(new ActionStateMachine());    // Controls actions
```

### 4.2 Event-Driven Transitions

```java
public interface State {
    void onEnter();
    void onExit();
    void onUpdate();
    void onEvent(Event event);
}

public class EventDrivenStateMachine {
    private final Map<State, Map<Event, State>> transitionTable;
    private State currentState;

    public void publishEvent(Event event) {
        State targetState = transitionTable
            .getOrDefault(currentState, Collections.emptyMap())
            .get(event);

        if (targetState != null) {
            currentState.onExit();
            currentState.onEvent(event);
            targetState.onEnter();
            currentState = targetState;
        }
    }
}
```

### 4.3 FSM Analysis of MineWright

**Current Implementation:**
- AgentStateMachine with explicit states (IDLE, PLANNING, EXECUTING, etc.)
- Thread-safe state transitions using AtomicReference
- Event bus integration
- Valid transition enforcement

**Strengths:**
- Well-structured state definition
- Thread-safe implementation
- Event publishing
- Transition validation

**Weaknesses:**
- No hierarchical states
- No concurrent states
- Limited interrupt handling
- No state history (no push/pop)

### 4.4 FSM Recommendations for MineWright

**Priority: MEDIUM**

1. **Add Hierarchical States**
   ```java
   public enum ExtendedAgentState {
       WORKING(
           new AgentState[] {MINING, BUILDING, CRAFTING}
       ),
       COMBAT(
           new AgentState[] {ATTACKING, DEFENDING, RETREATING}
       );

       private final AgentState[] substates;
   }
   ```

2. **Implement Pushdown Automaton for Interrupts**
   ```java
   public class InterruptibleActionExecutor {
       public void handleInterrupt(Task interruptTask) {
           // Push current state
           stateMachine.pushState(currentState);

           // Execute interrupt
           executeTask(interruptTask);

           // Pop to resume
           stateMachine.popState();
       }
   }
   ```

3. **Concurrent Movement + Action States**
   ```java
   public class ParallelActionExecutor {
       private final StateMachine movementState;
       private final StateMachine actionState;

       public void tick() {
           movementState.tick();  // Always moving
           actionState.tick();    // Performing action
       }
   }
   ```

---

## 5. Integration Patterns

### 5.1 Behavior Tree + Utility AI

```java
public class UtilityBehaviorTree extends BehaviorTree {
    @Override
    protected BehaviorNode selectChild(SelectorNode selector) {
        // Replace fixed-order selection with utility scoring
        if (selector instanceof UtilitySelector) {
            return ((UtilitySelector) selector).selectByUtility(context);
        }
        return super.selectChild(selector);
    }
}
```

**Benefits:**
- BT provides structure and hierarchy
- Utility AI provides dynamic selection
- Best for: Complex decision-making with multiple valid options

### 5.2 FSM + GOAP

```java
public class HybridFSMGOAP {
    private StateMachine fsm;
    private GOAPPlanner goap;

    public void onEnterState(State state) {
        if (state.requiresPlanning()) {
            // Use GOAP to plan actions for this state
            List<Action> plan = goap.plan(currentState, state.getGoal());
            executePlan(plan);
        }
    }

    public void onPlanComplete() {
        // Transition to next state based on plan results
        fsm.transitionTo(nextState);
    }
}
```

**Benefits:**
- FSM manages high-level state flow
- GOAP handles action sequences within states
- Best for: Mission-based gameplay with structured phases

### 5.3 GDC 2025: GOAP Hybrid Systems

**Kingdom Come: Deliverance 2 Approach:**
- GOAP provides flexible action selection
- Designer constraints prevent "too uncontrollable" behavior
- Hybrid system maintains narrative control

```java
public class ConstrainedGOAP {
    private final Set<Action> designerActions;  // Mandatory actions
    private final Set<Action> goapActions;      // Flexible actions

    public List<Action> plan(WorldState state, Goal goal) {
        // Always include designer-specified actions
        List<Action> plan = new ArrayList<>(designerActions);

        // Fill gaps with GOAP-planned actions
        plan.addAll(goapPlan(state, goal));

        return validatePlan(plan);
    }
}
```

### 5.4 Neuro-Symbolic Integration

**LLM + Symbolic Planning:**

```java
public class NeuroSymbolicPlanner {
    private final LLMClient llm;
    private final HTNPlanner htn;

    public List<Task> plan(String command, WorldState world) {
        // LLM interprets command and extracts intent
        CommandIntent intent = llm.extractIntent(command);

        // Symbolic planner handles concrete execution
        List<Task> tasks = htn.plan(intent, world);

        // LLM validates and refines plan
        return llm.refinePlan(tasks, command, world);
    }
}
```

**Benefits:**
- LLM provides natural language understanding
- Symbolic planner provides guaranteed execution
- Best for: Complex, vague user commands

### 5.5 Recommended Architecture for MineWright

**Three-Layer Hybrid System:**

```
┌─────────────────────────────────────────────────┐
│         Layer 1: LLM Intent Understanding        │
│  (Process natural language, extract high-level   │
│   goals, handle ambiguity)                       │
└─────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────┐
│         Layer 2: HTN Task Decomposition          │
│  (Break goals into subtasks, cache plans,        │
│   handle common patterns)                        │
└─────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────┐
│       Layer 3: Utility + BT Execution            │
│  (React to events, score actions, execute        │
│   tick-by-tick with interrupts)                  │
└─────────────────────────────────────────────────┘
```

---

## 6. MineWright Current System Analysis

### 6.1 Architecture Overview

**Existing Components:**
1. **ActionExecutor**: Tick-based task execution with async LLM planning
2. **AgentStateMachine**: Explicit state management (IDLE, PLANNING, EXECUTING, etc.)
3. **ActionRegistry**: Plugin-based action factory system
4. **InterceptorChain**: Cross-cutting concerns (logging, metrics, events)
5. **CollaborativeBuildManager**: Multi-agent spatial coordination
6. **Async LLM Infrastructure**: Non-blocking LLM calls with resilience

### 6.2 Strengths

1. **Async LLM Integration**: Non-blocking planning prevents game freezes
2. **Plugin Architecture**: Extensible action system via ActionRegistry
3. **State Machine**: Explicit state management with validation
4. **Interceptor Pattern**: Clean separation of cross-cutting concerns
5. **Multi-Agent Support**: Collaborative building with spatial partitioning
6. **Resilience Patterns**: Circuit breakers, retries, fallback handlers

### 6.3 Weaknesses and Gaps

1. **No Dynamic Action Selection**
   - Actions executed in fixed queue order
   - No reactivity to world changes
   - Cannot interrupt for urgent tasks

2. **No Hierarchical Planning**
   - LLM generates flat task lists
   - No task decomposition abstraction
   - Limited plan reusability

3. **No Utility-Based Decisions**
   - No scoring system for action selection
   - No context-aware prioritization
   - Fixed behavior patterns

4. **Limited Interrupt Handling**
   - State machine has PAUSED state but underutilized
   - No push/pop state history
   - Concurrent states not supported

5. **No Plan Caching**
   - Every command goes to LLM
   - Repeated commands re-planned
   - No optimization for common patterns

6. **FSM Limitations**
   - No hierarchical states
   - No concurrent states
   - Limited event-driven transitions

### 6.4 Current Execution Flow

```
User Command (K key)
    ↓
Async LLM Call (TaskPlanner.planTasksAsync)
    ↓
Wait for CompletableFuture
    ↓
Queue Tasks (Sequential)
    ↓
Execute Actions (Tick-based)
    ↓
Result → Memory
```

**Problem:** Linear, non-reactive, no dynamic replanning

---

## 7. Recommendations for MineWright

### 7.1 Priority 1: Utility AI for Reactive Decision-Making

**Impact:** HIGH | **Effort:** MEDIUM | **Urgency:** HIGH

**Implementation:**

```java
// File: src/main/java/com/minewright/ai/utility/UtilitySystem.java
package com.minewright.ai.utility;

import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.execution.ActionContext;
import java.util.*;
import java.util.function.Function;

/**
 * Utility AI system for scoring and selecting actions based on context.
 */
public class UtilitySystem {
    private final List<UtilityAction> actions;
    private final Map<String, ResponseCurve> curves;

    public UtilitySystem() {
        this.actions = new ArrayList<>();
        this.curves = new HashMap<>();
        initializeCurves();
        initializeActions();
    }

    private void initializeCurves() {
        curves.put("distance_logistic", new LogisticCurve(0.5, 10.0));
        curves.put("health_linear", new LinearCurve(-2.0, 1.0));
        curves.put("binary", new BinaryCurve());
        curves.put("inverse_exponential", new InverseExponentialCurve(0.1));
    }

    private void initializeActions() {
        // Combat actions
        actions.add(new UtilityAction("attack")
            .addConsideration("enemy_distance", "distance_logistic", 1.0,
                ctx -> ctx.getDistanceToNearestEnemy())
            .addConsideration("weapon_ready", "binary", 1.0,
                ctx -> ctx.hasWeapon() ? 1.0 : 0.0)
            .addConsideration("health_critical", "inverse_exponential", 0.5,
                ctx -> ctx.getHealthPercentage()));

        // Retreat actions
        actions.add(new UtilityAction("retreat")
            .addConsideration("health_critical", "health_linear", 1.0,
                ctx -> ctx.getHealthPercentage())
            .addConsideration("has_escape_route", "binary", 0.8,
                ctx -> ctx.hasEscapeRoute() ? 1.0 : 0.0));

        // Gather actions
        actions.add(new UtilityAction("gather")
            .addConsideration("resource_nearby", "distance_logistic", 1.0,
                ctx -> 20.0 - ctx.getDistanceToNearestResource())
            .addConsideration("inventory_space", "binary", 0.7,
                ctx -> ctx.getInventorySpacePercentage()));
    }

    public UtilityAction selectBestAction(UtilityContext context) {
        return actions.stream()
            .max(Comparator.comparing(a -> a.calculateUtility(context, curves)))
            .orElse(null);
    }

    public Task createTaskFromAction(UtilityAction action, UtilityContext context) {
        // Convert utility action to MineWright task
        return Task.builder()
            .action(action.getId())
            .parameters(context.toParameterMap())
            .build();
    }
}

// File: src/main/java/com/minewright/ai/utility/UtilityAction.java
package com.minewright.ai.utility;

import java.util.*;

public class UtilityAction {
    private final String id;
    private final List<Consideration> considerations;

    public UtilityAction(String id) {
        this.id = id;
        this.considerations = new ArrayList<>();
    }

    public UtilityAction addConsideration(String name, String curveId,
                                          double weight, Function<UtilityContext, Double> extractor) {
        considerations.add(new Consideration(name, curveId, weight, extractor));
        return this;
    }

    public double calculateUtility(UtilityContext context, Map<String, ResponseCurve> curves) {
        if (considerations.isEmpty()) return 0.0;

        return considerations.stream()
            .mapToDouble(c -> {
                double input = c.extractor.apply(context);
                ResponseCurve curve = curves.get(c.curveId);
                double score = curve != null ? curve.evaluate(input) : input;
                return score * c.weight;
            })
            .average()
            .orElse(0.0);
    }

    public String getId() { return id; }

    private static class Consideration {
        final String name;
        final String curveId;
        final double weight;
        final Function<UtilityContext, Double> extractor;

        Consideration(String name, String curveId, double weight,
                     Function<UtilityContext, Double> extractor) {
            this.name = name;
            this.curveId = curveId;
            this.weight = weight;
            this.extractor = extractor;
        }
    }
}

// File: src/main/java/com/minewright/ai/utility/UtilityContext.java
package com.minewright.ai.utility;

import com.minewright.entity.ForemanEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class UtilityContext {
    private final ForemanEntity foreman;
    private final Map<String, Object> cache;

    public UtilityContext(ForemanEntity foreman) {
        this.foreman = foreman;
        this.cache = new HashMap<>();
    }

    public double getDistanceToNearestEnemy() {
        return (double) cache.computeIfAbsent("nearest_enemy_dist",
            k -> foreman.level().getEntitiesOfClass(Entity.class,
                foreman.getBoundingBox().inflate(20)).stream()
                .filter(e -> e instanceof net.minecraft.world.entity.monster.Monster)
                .mapToDouble(e -> foreman.position().distanceTo(e.position()))
                .min()
                .orElse(100.0));
    }

    public double getHealthPercentage() {
        return foreman.getHealth() / foreman.getMaxHealth();
    }

    public boolean hasWeapon() {
        return foreman.getMainHandItem().canPerformAction(
            net.minecraft.world.item.Items.IRON_SWORD.getItem(),
            net.minecraft.world.item.itemaction.ItemAction.ATTACK);
    }

    public boolean hasEscapeRoute() {
        // Simple check: is there space behind?
        BlockPos behind = foreman.blockPosition().relative(
            foreman.getDirection().getOpposite());
        return foreman.level().getBlockState(behind).isAir();
    }

    public double getDistanceToNearestResource() {
        return (double) cache.computeIfAbsent("nearest_resource_dist",
            k -> 15.0);  // Placeholder: actual implementation would scan
    }

    public double getInventorySpacePercentage() {
        return 0.8;  // Placeholder
    }

    public Map<String, Object> toParameterMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("entity_uuid", foreman.getUUID().toString());
        return params;
    }
}
```

**Integration with ActionExecutor:**

```java
// In ActionExecutor.java
private final UtilitySystem utilitySystem;

public ActionExecutor(ForemanEntity foreman) {
    // ... existing initialization ...
    this.utilitySystem = new UtilitySystem();
}

public void tick() {
    // Check for reactive interrupts
    UtilityContext context = new UtilityContext(foreman);

    // If under attack, use utility system
    if (context.getDistanceToNearestEnemy() < 5.0) {
        UtilityAction bestAction = utilitySystem.selectBestAction(context);
        if (bestAction != null) {
            Task interruptTask = utilitySystem.createTaskFromAction(bestAction, context);

            // Push current state
            stateMachine.pushState(currentState);

            // Execute interrupt
            executeTask(interruptTask);

            // Will pop state after interrupt completes
            return;
        }
    }

    // ... rest of existing tick logic ...
}
```

### 7.2 Priority 2: Plan Caching for Common Commands

**Impact:** HIGH | **Effort:** LOW | **Urgency:** HIGH

**Implementation:**

```java
// File: src/main/java/com/minewright/llm/PlanCache.java
package com.minewright.llm;

import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cache for LLM-generated plans to avoid redundant API calls.
 */
public class PlanCache {
    private final Cache<PlanSignature, List<Task>> cache;

    public PlanCache() {
        this.cache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    }

    public List<Task> get(String command, ForemanEntity foreman) {
        PlanSignature signature = new PlanSignature(command, foreman);
        return cache.getIfPresent(signature);
    }

    public void put(String command, ForemanEntity foreman, List<Task> tasks) {
        PlanSignature signature = new PlanSignature(command, foreman);
        cache.put(signature, tasks);
    }

    public void invalidate() {
        cache.invalidateAll();
    }

    public int size() {
        return (int) cache.estimatedSize();
    }

    /**
     * Signature for plan cache keys.
     */
    private static class PlanSignature {
        private final String command;
        private final String location;
        private final String health;
        private final String inventory;

        public PlanSignature(String command, ForemanEntity foreman) {
            this.command = command.toLowerCase().trim();
            this.location = foreman.blockPosition().toString();
            this.health = String.valueOf((int) foreman.getHealth());
            this.inventory = String.valueOf(foreman.getInventory().getContainerSize());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PlanSignature)) return false;
            PlanSignature that = (PlanSignature) o;
            return command.equals(that.command) &&
                   location.equals(that.location);
        }

        @Override
        public int hashCode() {
            return command.hashCode() * 31 + location.hashCode();
        }
    }
}
```

**Integration:**

```java
// In ActionExecutor.java
private final PlanCache planCache;

public void processNaturalLanguageCommand(String command) {
    // Check cache first
    List<Task> cachedTasks = planCache.get(command, foreman);
    if (cachedTasks != null) {
        MineWrightMod.LOGGER.info("Using cached plan for command: {}", command);
        taskQueue.clear();
        taskQueue.addAll(cachedTasks);
        sendToGUI(foreman.getSteveName(), "Okay! (cached)");
        return;
    }

    // ... existing async LLM logic ...
}

// When planning completes
if (response != null) {
    List<Task> tasks = response.getTasks();

    // Cache the result
    planCache.put(command, foreman, tasks);

    taskQueue.clear();
    taskQueue.addAll(tasks);
    // ...
}
```

### 7.3 Priority 3: Event-Driven Behavior Tree Layer

**Impact:** MEDIUM-HIGH | **Effort:** HIGH | **Urgency:** MEDIUM

**Implementation:**

```java
// File: src/main/java/com/minewright/ai/bt/BehaviorTree.java
package com.minewright.ai.bt;

import com.minewright.event.EventBus;
import java.util.*;

/**
 * Event-driven behavior tree for reactive decision-making.
 */
public class BehaviorTree {
    private final BehaviorNode root;
    private final EventBus eventBus;
    private final Map<Class<?>, List<BehaviorNode>> eventSubscribers;

    public BehaviorTree(BehaviorNode root, EventBus eventBus) {
        this.root = root;
        this.eventBus = eventBus;
        this.eventSubscribers = new HashMap<>();
        registerEventHandlers(root);
    }

    public NodeStatus tick() {
        return root.tick();
    }

    public void onEvent(Object event) {
        List<BehaviorNode> subscribers = eventSubscribers.get(event.getClass());
        if (subscribers != null) {
            subscribers.forEach(node -> node.onEvent(event));
        }
    }

    private void registerEventHandlers(BehaviorNode node) {
        if (node instanceof EventSubscriber) {
            EventSubscriber subscriber = (EventSubscriber) node;
            subscriber.getSubscribedEvents().forEach(eventClass -> {
                eventSubscribers.computeIfAbsent(eventClass, k -> new ArrayList<>())
                    .add(node);
            });
        }

        if (node instanceof CompositeNode) {
            ((CompositeNode) node).getChildren().forEach(this::registerEventHandlers);
        }
    }
}

// File: src/main/java/com/minewright/ai/bt/BehaviorNode.java
package com.minewright.ai.bt;

public interface BehaviorNode {
    NodeStatus tick();
    void onEvent(Object event);
    void reset();
}

// File: src/main/java/com/minewright/ai/bt/NodeStatus.java
package com.minewright.ai.bt;

public enum NodeStatus {
    SUCCESS,
    FAILURE,
    RUNNING
}

// File: src/main/java/com/minewright/ai/bt/SelectorNode.java
package com.minewright.ai.bt;

import java.util.*;

/**
 * Selector node: tries children in order, returns first success.
 */
public class SelectorNode extends CompositeNode {
    private int currentChildIndex = 0;

    @Override
    public NodeStatus tick() {
        while (currentChildIndex < children.size()) {
            BehaviorNode child = children.get(currentChildIndex);
            NodeStatus status = child.tick();

            switch (status) {
                case SUCCESS:
                    reset();
                    return NodeStatus.SUCCESS;
                case RUNNING:
                    return NodeStatus.RUNNING;
                case FAILURE:
                    currentChildIndex++;
                    break;
            }
        }

        reset();
        return NodeStatus.FAILURE;
    }

    @Override
    public void reset() {
        currentChildIndex = 0;
        children.forEach(BehaviorNode::reset);
    }
}

// File: src/main/java/com/minewright/ai/bt/SequenceNode.java
package com.minewright.ai.bt;

import java.util.*;

/**
 * Sequence node: runs children in order, fails if any child fails.
 */
public class SequenceNode extends CompositeNode {
    private int currentChildIndex = 0;

    @Override
    public NodeStatus tick() {
        while (currentChildIndex < children.size()) {
            BehaviorNode child = children.get(currentChildIndex);
            NodeStatus status = child.tick();

            switch (status) {
                case FAILURE:
                    reset();
                    return NodeStatus.FAILURE;
                case RUNNING:
                    return NodeStatus.RUNNING;
                case SUCCESS:
                    currentChildIndex++;
                    break;
            }
        }

        reset();
        return NodeStatus.SUCCESS;
    }

    @Override
    public void reset() {
        currentChildIndex = 0;
        children.forEach(BehaviorNode::reset);
    }
}

// File: src/main/java/com/minewright/ai/bt/EventCondition.java
package com.minewright.ai.bt;

import com.minewright.event.CombatEvent;
import java.util.*;

/**
 * Condition node that reacts to events.
 */
public class EventCondition implements BehaviorNode, EventSubscriber {
    private boolean triggered = false;
    private final Set<Class<?>> subscribedEvents;

    public EventCondition(Class<?>... events) {
        this.subscribedEvents = new HashSet<>(Arrays.asList(events));
    }

    @Override
    public NodeStatus tick() {
        return triggered ? NodeStatus.SUCCESS : NodeStatus.FAILURE;
    }

    @Override
    public void onEvent(Object event) {
        if (subscribedEvents.contains(event.getClass())) {
            triggered = true;
        }
    }

    @Override
    public void reset() {
        triggered = false;
    }

    @Override
    public Set<Class<?>> getSubscribedEvents() {
        return subscribedEvents;
    }
}

// File: src/main/java/com/minewright/ai/bt/ActionAdapter.java
package com.minewright.ai.bt;

import com.minewright.action.actions.BaseAction;

/**
 * Adapts MineWright BaseAction to BehaviorNode interface.
 */
public class ActionAdapter implements BehaviorNode {
    private final BaseAction action;

    public ActionAdapter(BaseAction action) {
        this.action = action;
    }

    @Override
    public NodeStatus tick() {
        if (!action.isComplete()) {
            action.tick();
            return NodeStatus.RUNNING;
        }
        return action.getResult().isSuccess() ?
            NodeStatus.SUCCESS : NodeStatus.FAILURE;
    }

    @Override
    public void onEvent(Object event) {
        // Actions don't typically handle events
    }

    @Override
    public void reset() {
        action.cancel();
    }
}
```

**Integration with ActionExecutor:**

```java
// In ActionExecutor.java
private BehaviorTree reactiveBehaviorTree;

public ActionExecutor(ForemanEntity foreman) {
    // ... existing initialization ...

    // Build reactive BT
    this.reactiveBehaviorTree = buildReactiveBehaviorTree();
}

private BehaviorTree buildReactiveBehaviorTree() {
    // Priority: Combat > Blockage > queued tasks
    SelectorNode root = new SelectorNode();

    // Combat branch
    SequenceNode combatSequence = new SequenceNode();
    combatSequence.addChild(new EventCondition(CombatEvent.class));
    combatSequence.addChild(new ActionAdapter(new CombatAction(foreman)));
    root.addChild(combatSequence);

    // Blockage branch
    SequenceNode blockageSequence = new SequenceNode();
    blockageSequence.addChild(new BlockageCondition(foreman));
    blockageSequence.addChild(new ActionAdapter(new PathfindAction(foreman)));
    root.addChild(blockageSequence);

    // Default: process task queue
    root.addChild(new TaskQueueExecutor(this));

    return new BehaviorTree(root, eventBus);
}

public void tick() {
    // Check reactive BT first
    NodeStatus status = reactiveBehaviorTree.tick();
    if (status == NodeStatus.RUNNING) {
        return;  // Reactive action in progress
    }

    // Fall through to normal task queue
    // ... existing logic ...
}
```

### 7.4 Priority 4: HTN for Hierarchical Planning

**Impact:** MEDIUM | **Effort:** HIGH | **Urgency:** LOW

**Implementation:**

```java
// File: src/main/java/com/minewright/ai/htn/HTNPlanner.java
package com.minewright.ai.htn;

import com.minewright.action.Task;
import com.minewright.llm.TaskPlanner;
import java.util.*;

/**
 * Hierarchical Task Network planner for MineWright.
 */
public class HTNPlanner {
    private final Map<String, CompoundTask> taskLibrary;
    private final TaskPlanner llmPlanner;

    public HTNPlanner(TaskPlanner llmPlanner) {
        this.taskLibrary = new HashMap<>();
        this.llmPlanner = llmPlanner;
        initializeTaskLibrary();
    }

    private void initializeTaskLibrary() {
        // Build structure task
        CompoundTask build = new CompoundTask("build");
        build.addMethod(new Method(
            List.of(new PrimitiveTask("gather_materials")),
            new PrimitiveTask("construct_structure")
        ));
        taskLibrary.put("build", build);

        // Gather resources task
        CompoundTask gather = new CompoundTask("gather");
        gather.addMethod(new Method(
            List.of(
                new PrimitiveTask("find_resource"),
                new PrimitiveTask("mine"),
                new PrimitiveTask("store_inventory")
            )
        ));
        taskLibrary.put("gather", gather);
    }

    public List<Task> plan(String highLevelCommand) {
        // Check HTN library first
        CompoundTask rootTask = taskLibrary.get(highLevelCommand);
        if (rootTask != null) {
            return decompose(rootTask);
        }

        // Fallback to LLM for unknown commands
        return null;  // Signal to use LLM
    }

    private List<Task> decompose(CompoundTask task) {
        List<Task> result = new ArrayList<>();
        Queue<Task> queue = new LinkedList<>();

        queue.add(new Task(task.getName(), Map.of()));

        while (!queue.isEmpty()) {
            Task current = queue.poll();

            if (taskLibrary.containsKey(current.getAction())) {
                // Decompose further
                CompoundTask compound = taskLibrary.get(current.getAction());
                Method method = compound.selectBestMethod();
                if (method != null) {
                    method.getSubtasks().forEach(queue::add);
                }
            } else {
                // Primitive task, add to result
                result.add(current);
            }
        }

        return result;
    }
}

// File: src/main/java/com/minewright/ai/htn/CompoundTask.java
package com.minewright.ai.htn;

import java.util.*;

public class CompoundTask {
    private final String name;
    private final List<Method> methods;

    public CompoundTask(String name) {
        this.name = name;
        this.methods = new ArrayList<>();
    }

    public void addMethod(Method method) {
        methods.add(method);
    }

    public String getName() { return name; }

    public Method selectBestMethod() {
        // Simple: return first method
        // Advanced: score methods based on world state
        return methods.isEmpty() ? null : methods.get(0);
    }
}

// File: src/main/java/com/minewright/ai/htn/Method.java
package com.minewright.ai.htn;

import java.util.*;

public class Method {
    private final List<Task> subtasks;

    public Method(Task... subtasks) {
        this.subtasks = Arrays.asList(subtasks);
    }

    public List<Task> getSubtasks() {
        return subtasks;
    }
}

// File: src/main/java/com/minewright/ai/htn/PrimitiveTask.java
package com.minewright.ai.htn;

public class PrimitiveTask extends com.minewright.action.Task {
    public PrimitiveTask(String action) {
        super(action, Map.of());
    }
}
```

### 7.5 Priority 5: Enhanced FSM with Pushdown Support

**Impact:** MEDIUM | **Effort:** MEDIUM | **Urgency:** LOW

**Implementation:**

```java
// File: src/main/java/com/minewright/execution/PushdownStateMachine.java
package com.minewright.execution;

import com.minewright.event.EventBus;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Stack;

/**
 * Pushdown automaton state machine with interrupt support.
 */
public class PushdownStateMachine extends AgentStateMachine {
    private final Stack<AgentState> stateStack;
    private final AtomicReference<AgentState> suspendedState;

    public PushdownStateMachine(EventBus eventBus, String agentId) {
        super(eventBus, agentId);
        this.stateStack = new Stack<>();
        this.suspendedState = new AtomicReference<>();
    }

    /**
     * Interrupt current state and push onto stack.
     */
    public boolean interrupt(AgentState interruptState, String reason) {
        AgentState current = getCurrentState();

        if (current == interruptState) {
            return false;  // Already in this state
        }

        // Push current state
        stateStack.push(current);
        suspendedState.set(current);

        // Transition to interrupt state
        boolean success = transitionTo(interruptState,
            "INTERRUPT: " + reason);

        if (!success) {
            // Rollback
            stateStack.pop();
            suspendedState.set(null);
        }

        return success;
    }

    /**
     * Pop suspended state and resume.
     */
    public boolean resume() {
        if (stateStack.isEmpty()) {
            return false;
        }

        AgentState previousState = stateStack.pop();
        suspendedState.set(null);

        return transitionTo(previousState, "RESUME from interrupt");
    }

    /**
     * Check if there's a suspended state to resume to.
     */
    public boolean hasSuspendedState() {
        return !stateStack.isEmpty();
    }

    public AgentState getSuspendedState() {
        return suspendedState.get();
    }
}
```

**Integration:**

```java
// In ActionExecutor.java
private final PushdownStateMachine stateMachine;

public void handleCombatInterrupt(CombatEvent event) {
    if (stateMachine.getCurrentState() != AgentState.COMBAT) {
        // Interrupt and push current state
        stateMachine.interrupt(AgentState.COMBAT, "Under attack!");

        // Execute combat action
        Task combatTask = new Task("attack", Map.of(
            "target_uuid", event.getAttackerUUID().toString()
        ));
        executeTask(combatTask);
    }
}

public void tick() {
    // Check if combat complete
    if (stateMachine.getCurrentState() == AgentState.COMBAT &&
        currentAction != null && currentAction.isComplete()) {

        if (stateMachine.hasSuspendedState()) {
            // Resume previous activity
            stateMachine.resume();
        } else {
            stateMachine.transitionTo(AgentState.IDLE);
        }
    }

    // ... rest of tick logic ...
}
```

---

## 8. Implementation Roadmap

### Phase 1: Quick Wins (2-3 weeks)

**Priority 1: Plan Caching**
- Week 1: Implement PlanCache with Caffeine
- Week 1: Integrate with ActionExecutor
- Week 2: Add cache invalidation logic
- Week 2: Add metrics/monitoring

**Priority 2: Basic Utility AI**
- Week 1: Implement ResponseCurve classes
- Week 2: Implement Consideration and UtilityAction
- Week 3: Create combat utility actions
- Week 3: Integrate with ActionExecutor for interrupts

**Deliverables:**
- 50% reduction in LLM API calls for repeated commands
- Reactive combat behavior without manual commands

### Phase 2: Event-Driven Behavior Trees (4-6 weeks)

**Week 1-2: BT Core**
- Implement BehaviorNode interface
- Implement SelectorNode and SequenceNode
- Implement EventCondition and ActionAdapter

**Week 3-4: Integration**
- Build reactive behavior tree for MineWright
- Integrate with existing EventBus
- Add event subscriptions

**Week 5-6: Testing & Tuning**
- Create test scenarios
- Tune behavior priorities
- Profile performance

**Deliverables:**
- Reactive BT layer integrated
- Event-driven combat/navigation
- Performance profiling results

### Phase 3: Enhanced State Machine (2-3 weeks)

**Week 1: Pushdown FSM**
- Implement PushdownStateMachine
- Add interrupt/resume methods
- Add state history tracking

**Week 2-3: Integration**
- Replace AgentStateMachine with PushdownStateMachine
- Update ActionExecutor to use interrupts
- Add concurrent states (movement + action)

**Deliverables:**
- Interruptible actions
- State history tracking
- Concurrent movement and action states

### Phase 4: HTN Planning (4-6 weeks)

**Week 1-2: HTN Core**
- Implement CompoundTask and Method
- Implement HTNPlanner with decomposition
- Create task library for common commands

**Week 3-4: Hybrid Planning**
- Implement LLM + HTN hybrid planner
- Add plan caching for HTN results
- Handle HTN failures gracefully

**Week 5-6: Testing & Expansion**
- Create test scenarios
- Expand task library
- Benchmark against pure LLM

**Deliverables:**
- HTN planner for common patterns
- 70% faster planning for cached commands
- Hybrid LLM + HTN system

### Phase 5: Advanced Features (6-8 weeks)

**Week 1-2: Learning System**
- Implement utility weight tuning
- Add feedback from action results
- Online weight adjustment

**Week 3-4: Multi-Agent Coordination**
- Extend CollaborativeBuildManager
- Add agent-specific utility scores
- Implement dynamic task allocation

**Week 5-6: Neuro-Symbolic Integration**
- LLM intent extraction
- Symbolic plan refinement
- Plan validation

**Week 7-8: Polish & Optimization**
- Performance profiling
- Memory optimization
- Documentation

**Deliverables:**
- Self-tuning utility system
- Enhanced multi-agent coordination
- Neuro-symbolic planning system

### Timeline Summary

| Phase | Duration | Priority | Impact |
|-------|----------|----------|--------|
| Phase 1: Quick Wins | 2-3 weeks | HIGH | High |
| Phase 2: BT Layer | 4-6 weeks | MEDIUM-HIGH | Medium-High |
| Phase 3: Enhanced FSM | 2-3 weeks | MEDIUM | Medium |
| Phase 4: HTN Planning | 4-6 weeks | MEDIUM | Medium |
| Phase 5: Advanced | 6-8 weeks | LOW | Low |

**Total: 18-26 weeks (4.5-6.5 months)**

### Risk Mitigation

1. **Performance Concerns**
   - Profile each phase before moving to next
   - Set performance budgets (e.g., BT tick < 1ms)
   - Use async where possible

2. **Complexity Management**
   - Keep existing LLM system as fallback
   - Gradual migration, not rewrite
   - Extensive testing at each phase

3. **LLM Dependency**
   - Cache aggressively
   - Use HTN for common patterns
   - Implement graceful degradation

---

## 9. References

### 9.1 Behavior Trees

- **AI Tree for Unity** (v1.11.1) - Modern behavior tree tool with perception systems
- **Behaviac Framework** - Supports BT, FSM, and HTN in unified system

### 9.2 GOAP and HTN

- **Unity GOAP Framework** (GitCode) - Open-source GOAP with action cost optimization
- **Udemy Course: "Goal-Oriented Action Planning - Advanced AI For Games"** - C# GOAP implementation
- **pyHIPOP+** (2025) - Enhanced HTN planner with MARL integration
- **Horizon Zero Dawn** - HTN usage in AAA production
- **Transformers: Fall of Cybertron** - HTN for NPC behavior

### 9.3 Utility AI

- **GameWorld Score System** (Skywork AI) - Comprehensive evaluation framework
- **Newcastle University Research** - AI long-term strategy evaluation

### 9.4 Industry Trends

- **GDC 2025 AI Summit** - Latest in game AI research and practices
- **Kingdom Come: Deliverance 2** - GOAP hybrid systems for NPC behavior
- **Tencent AI Teammate System** - Natural language command for teammates
- **Google DeepMind SIMA** - Universal game agent (600+ tasks)

### 9.5 Academic Research

- **IEEE Conference on Games (CoG) 2024** - "Generative AI with GOAP for fast-paced dynamic decision-making"
- **AAAI Conference on Artificial Intelligence and Interactive Digital Entertainment**
- **Artificial Intelligence journal (Elsevier)** - Recent HTN and neuro-symbolic papers

---

## Appendix A: Code Structure

### Proposed Package Structure

```
src/main/java/com/minewright/
├── ai/
│   ├── utility/
│   │   ├── UtilitySystem.java
│   │   ├── UtilityAction.java
│   │   ├── UtilityContext.java
│   │   ├── ResponseCurve.java
│   │   ├── LinearCurve.java
│   │   ├── LogisticCurve.java
│   │   ├── ExponentialCurve.java
│   │   └── BinaryCurve.java
│   ├── bt/
│   │   ├── BehaviorTree.java
│   │   ├── BehaviorNode.java
│   │   ├── NodeStatus.java
│   │   ├── SelectorNode.java
│   │   ├── SequenceNode.java
│   │   ├── ParallelNode.java
│   │   ├── DecoratorNode.java
│   │   ├── ConditionNode.java
│   │   ├── EventCondition.java
│   │   ├── ActionAdapter.java
│   │   └── CompositeNode.java
│   ├── htn/
│   │   ├── HTNPlanner.java
│   │   ├── CompoundTask.java
│   │   ├── PrimitiveTask.java
│   │   ├── Method.java
│   │   ├── TaskLibrary.java
│   │   └── HybridPlanner.java
│   └── learning/
│       ├── WeightTuner.java
│       ├── FeedbackCollector.java
│       └── OnlineLearning.java
├── execution/
│   ├── AgentStateMachine.java (existing)
│   ├── PushdownStateMachine.java (new)
│   ├── HierarchicalStateMachine.java (new)
│   ├── ActionContext.java (existing)
│   └── ... (existing)
├── llm/
│   ├── TaskPlanner.java (existing)
│   ├── PlanCache.java (new)
│   └── ... (existing)
└── action/
    ├── ActionExecutor.java (modify)
    ├── Task.java (existing)
    └── ... (existing)
```

---

## Appendix B: Performance Considerations

### B.1 Budget Targets

| System | Budget per Tick | Notes |
|--------|----------------|-------|
| Utility Scoring | < 0.5ms | Score 5-10 actions |
| Behavior Tree | < 1ms | Full tree traversal |
| HTN Planning | < 5ms | Common commands |
| LLM Planning | N/A | Async, non-blocking |
| State Machine | < 0.1ms | Simple transition |

### B.2 Optimization Strategies

1. **Utility AI**
   - Cache consideration inputs
   - Lazy evaluation
   - Parallel scoring

2. **Behavior Trees**
   - Event-driven (no continuous tick)
   - Node memoization
   - Subtree pruning

3. **HTN Planning**
   - Plan caching
   - Method pre-selection
   - Decomposition memoization

4. **State Machine**
   - Transition lookup tables
   - Minimal state objects
   - Atomic operations

---

## Appendix C: Testing Strategy

### C.1 Unit Tests

```java
@Test
public void testUtilityScoring() {
    UtilitySystem system = new UtilitySystem();
    UtilityContext context = createContext(
        5.0,  // enemy distance
        0.5,  // health percentage
        true  // has weapon
    );

    UtilityAction action = system.selectBestAction(context);
    assertEquals("attack", action.getId());
}

@Test
public void testBehaviorTree() {
    BehaviorTree tree = createTestTree();
    assertEquals(NodeStatus.SUCCESS, tree.tick());
}

@Test
public void testPlanCache() {
    PlanCache cache = new PlanCache();
    List<Task> tasks = createTestTasks();

    cache.put("test", foreman, tasks);
    assertSame(tasks, cache.get("test", foreman));
}

@Test
public void testHTNDecomposition() {
    HTNPlanner planner = new HTNPlanner(null);
    List<Task> tasks = planner.plan("build");

    assertNotNull(tasks);
    assertTrue(tasks.size() > 1);
}
```

### C.2 Integration Tests

```java
@Test
public void testReactiveCombatInterrupt() {
    ActionExecutor executor = new ActionExecutor(foreman);
    executor.processNaturalLanguageCommand("build a house");

    // Wait for planning to start
    waitForPlanning(executor);

    // Simulate combat event
    CombatEvent event = new CombatEvent(attackerUUID);
    executor.handleCombatInterrupt(event);

    // Verify combat state
    assertEquals(AgentState.COMBAT, executor.getState());
}

@Test
public void testPlanCacheHit() {
    ActionExecutor executor = new ActionExecutor(foreman);

    long start1 = System.currentTimeMillis();
    executor.processNaturalLanguageCommand("gather 10 oak logs");
    waitForCompletion(executor);
    long duration1 = System.currentTimeMillis() - start1;

    long start2 = System.currentTimeMillis();
    executor.processNaturalLanguageCommand("gather 10 oak logs");
    waitForCompletion(executor);
    long duration2 = System.currentTimeMillis() - start2;

    // Second call should be much faster (cache hit)
    assertTrue(duration2 < duration1 / 10);
}
```

### C.3 Performance Benchmarks

```java
@Test
public void benchmarkUtilityScoring() {
    UtilitySystem system = new UtilitySystem();
    UtilityContext context = createTestContext();

    long start = System.nanoTime();
    for (int i = 0; i < 10000; i++) {
        system.selectBestAction(context);
    }
    long duration = System.nanoTime() - start;

    double avgMicros = duration / 10000.0 / 1000.0;
    assertTrue(avgMicros < 0.5, "Average scoring time: " + avgMicros + "μs");
}

@Test
public void benchmarkBehaviorTree() {
    BehaviorTree tree = createComplexTree();

    long start = System.nanoTime();
    for (int i = 0; i < 10000; i++) {
        tree.tick();
    }
    long duration = System.nanoTime() - start;

    double avgMicros = duration / 10000.0 / 1000.0;
    assertTrue(avgMicros < 1.0, "Average BT tick time: " + avgMicros + "μs");
}
```

---

## Conclusion

The MineWright project has a solid foundation with async LLM integration, a plugin architecture, and state machine management. By incorporating modern game AI techniques, it can evolve from a simple command-execution system into a sophisticated, reactive AI agent.

**Recommended Priority:**
1. **Utility AI** - Enable reactive decision-making
2. **Plan Caching** - Reduce LLM API calls
3. **Event-Driven BT** - Add reactive behavior layer
4. **Enhanced FSM** - Support interrupts and state history
5. **HTN Planning** - Optimize common command patterns

This roadmap provides a gradual path to enhanced AI capabilities without disrupting existing functionality. Each phase can be implemented incrementally, with clear deliverables and measurable improvements.

---

**End of Report**
