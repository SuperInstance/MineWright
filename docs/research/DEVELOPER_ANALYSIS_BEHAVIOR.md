# Developer Analysis: Behavior Systems for MineWright

**Author:** Senior Developer Analysis
**Date:** 2026-02-28
**Version:** 1.0
**Status:** Implementation Guidance

---

## Executive Summary

This document analyzes MineWright's current behavior architecture against modern game AI patterns (Behavior Trees, FSM, GOAP, HTN, Utility AI) and provides specific recommendations for improvement. The analysis reveals that MineWright has a solid foundation with FSM for lifecycle management but lacks sophisticated reactive decision-making for runtime behavior.

**Key Finding:** MineWright should implement a **hybrid architecture** combining FSM (lifecycle) + Behavior Trees (reactive behaviors) + LLM (high-level planning). This matches patterns used in AAA games like Horizon: Zero Dawn and modern AI agents.

---

## Table of Contents

1. [Current State Analysis](#1-current-state-analysis)
2. [Research Gaps](#2-research-gaps)
3. [FSM vs Behavior Trees: Decision Matrix](#3-fsm-vs-behavior-trees-decision-matrix)
4. [Integration Points](#4-integration-points)
5. [Code Skeletons](#5-code-skeletons)
6. [Recommendations](#6-recommendations)

---

## 1. Current State Analysis

### 1.1 Existing Architecture

MineWright currently uses a **simple FSM-based architecture** with LLM planning:

```
Current Implementation (C:\Users\casey\steve\src\main\java\com\minewright\):

├── execution/
│   ├── AgentStateMachine.java      (FSM - lifecycle states)
│   ├── AgentState.java             (IDLE, PLANNING, EXECUTING, etc.)
│   ├── InterceptorChain.java       (cross-cutting concerns)
│   └── EventBus.java               (event system)
│
├── action/
│   ├── ActionExecutor.java         (task queue + execution)
│   ├── BaseAction.java             (tick-based actions)
│   └── actions/                    (individual actions)
│       ├── MineBlockAction.java
│       ├── PlaceBlockAction.java
│       └── ...
│
└── llm/
    ├── TaskPlanner.java            (LLM-based planning)
    └── ResponseParser.java         (extracts tasks from LLM)
```

### 1.2 Current Flow

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`

```java
// Current flow in ActionExecutor.tick():
public void tick() {
    // 1. Check if LLM planning complete (async)
    if (isPlanning.get() && planningFuture.isDone()) {
        processPlanningResults();
    }

    // 2. Execute current action
    if (currentAction != null) {
        if (currentAction.isComplete()) {
            handleActionComplete();
        } else {
            currentAction.tick();  // Tick-based execution
        }
    }

    // 3. Start next task from queue
    if (currentAction == null && !taskQueue.isEmpty()) {
        executeTask(taskQueue.poll());
    }

    // 4. Idle behavior
    if (taskQueue.isEmpty() && currentAction == null) {
        idleFollowAction.tick();
    }
}
```

### 1.3 State Machine

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\execution\AgentStateMachine.java`

```java
// Current FSM states:
public enum AgentState {
    IDLE,       // Ready for commands
    PLANNING,   // LLM processing
    EXECUTING,  // Performing tasks
    PAUSED,     // User paused
    COMPLETED,  // All tasks done
    FAILED      // Error occurred
}

// Valid transitions (hardcoded):
IDLE → PLANNING → EXECUTING → COMPLETED → IDLE
                └→ FAILED → IDLE
                └→ PAUSED → EXECUTING
```

### 1.4 Strengths of Current Implementation

✅ **Async LLM Planning**: Non-blocking CompletableFuture prevents server freeze
✅ **Tick-Based Execution**: Actions progress incrementally without blocking
✅ **Plugin Architecture**: ActionRegistry allows dynamic action registration
✅ **Event System**: EventBus enables loose coupling
✅ **Interceptor Chain**: Cross-cutting concerns (logging, metrics) separated
✅ **Thread Safety**: AtomicReference and BlockingQueue for concurrent access

### 1.5 Weaknesses of Current Implementation

❌ **No Reactive Decision-Making**: Cannot respond to changing world conditions during execution
❌ **Linear Task Execution**: Tasks execute in fixed sequence, no adaptation
❌ **No Parallel Behaviors**: Cannot do multiple things at once (e.g., move while watching for enemies)
❌ **Limited Error Recovery**: Failed actions just fail, no retry or alternative strategies
❌ **No Priority System**: All tasks equal priority, no urgency handling
❌ **Rigid Planning**: LLM generates static plan, cannot adjust mid-execution
❌ **Missing Subsumption Architecture**: No clear layering of behaviors (survival vs. task)

---

## 2. Research Gaps

### 2.1 Missing Behavior Tree System

**Research:** `C:\Users\casey\steve\docs\research\BEHAVIOR_TREES_DESIGN.md`

Behavior Trees provide:
- **Hierarchical decomposition** of complex behaviors
- **Reactive execution** through continuous re-evaluation
- **Parallel execution** via Parallel nodes
- **Event-driven patterns** for modern efficiency

**Gap:** MineWright has no Behavior Tree implementation.

### 2.2 Missing Utility AI

**Research:** `C:\Users\casey\steve\docs\research\GAME_AI_PATTERNS.md` (Section 2)

Utility AI provides:
- **Smooth behavior blending** via scoring functions
- **Context-aware decisions** based on multiple factors
- **Easy tuning** without restructuring code
- **Emotional AI** for relationship-based behaviors

**Gap:** MineWright has no utility scoring for decisions.

### 2.3 Missing GOAP System

**Research:** `C:\Users\casey\steve\docs\research\ARCHITECTURE_D_GOAP.md`

GOAP provides:
- **Fast local planning** (milliseconds vs. 30-60s for LLM)
- **Goal-oriented behavior** with action preconditions/effects
- **Deterministic replanning** when world changes
- **Offline capability** (no API needed)

**Gap:** MineWright relies entirely on LLM for planning, no fast local planner.

### 2.4 Missing Hierarchical Planning

**Research:** `C:\Users\casey\steve\docs\research\GAME_AI_ARCHITECTURES.md` (HTN section)

HTN provides:
- **Forward-planning** from current state
- **Task decomposition** into subtasks
- **Alternative methods** for accomplishing goals
- **Strategic reasoning** for complex scenarios

**Gap:** MineWright has no hierarchical task decomposition beyond LLM.

### 2.5 Missing Subsumption Architecture

**Research:** `C:\Users\casey\steve\docs\research\GAME_AI_PATTERNS.md` (Section 6)

Subsumption provides:
- **Layered behaviors** (survival > task > idle)
- **Priority-based override** (danger interrupts everything)
- **Fast reflexes** without high-level reasoning
- **Robust fallbacks** when higher layers fail

**Gap:** MineWright has no layered behavior system.

---

## 3. FSM vs Behavior Trees: Decision Matrix

### 3.1 Comparison Table

| Aspect | Finite State Machine (FSM) | Behavior Tree (BT) |
|--------|---------------------------|-------------------|
| **Current Use in MineWright** | ✅ AgentStateMachine | ❌ Not implemented |
| **Complexity** | O(n²) transitions | O(n) nodes |
| **Parallel Tasks** | Requires multiple FSMs | Native support |
| **Maintainability** | Difficult at scale | Easy to extend |
| **Debugging** | Log-based | Visual tree structure |
| **Reactivity** | Event-driven | Continuous evaluation |
| **Hierarchical** | Flat (or HFSM) | Naturally hierarchical |
| **Learning Curve** | Simple | Moderate |
| **Performance** | Very low overhead | Slightly higher |
| **Best For** | Simple, linear AI | Complex, hierarchical AI |

### 3.2 When to Use FSM

**Current MineWright Usage (✅ Appropriate):**
```java
// C:\Users\casey\steve\src\main\java\com\minewright\execution\AgentStateMachine.java

// Agent lifecycle management - PERFECT for FSM
IDLE → PLANNING → EXECUTING → COMPLETED → IDLE
```

**Keep FSM for:**
- High-level agent lifecycle (IDLE, PLANNING, EXECUTING, etc.)
- UI state management (GUI screens, overlays)
- Network connection states (connected, disconnected, reconnecting)
- Game session states (lobby, playing, paused)

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\execution\AgentStateMachine.java`
```java
// ✅ Keep this - FSM is perfect for lifecycle
public class AgentStateMachine {
    public boolean transitionTo(AgentState targetState, String reason) {
        // Validates and transitions between lifecycle states
        // This is NOT about behavior, it's about execution phase
    }
}
```

### 3.3 When to Use Behavior Trees

**Missing Implementation (❌ Should Add):**

**Add Behavior Trees for:**
- **Combat decisions**: Attack, flee, hide based on enemy type/distance
- **Resource gathering**: Choose what to gather based on needs/availability
- **Building behaviors**: Adapt building strategy based on terrain/materials
- **Emergency responses**: React to threats, accidents, failures
- **Social behaviors**: Follow player, chat, emote based on context

**Example: Combat Behavior Tree**
```
[Selector] Combat Root
├── [Sequence] Flee
│   ├── Condition: Health < 30%
│   ├── Condition: Enemy nearby
│   └── Action: Run to safe zone
├── [Sequence] Fight
│   ├── Condition: Has weapon
│   ├── Condition: Enemy in range
│   └── Action: Attack enemy
└── [Sequence] Hide
    ├── Condition: No weapon
    └── Action: Find shelter
```

### 3.4 Hybrid Approach (Recommended)

**Best of Both Worlds:**

```
┌─────────────────────────────────────────────────────────────┐
│                    ForemanEntity                             │
├─────────────────────────────────────────────────────────────┤
│  High-Level: FSM (AgentStateMachine)                         │
│  ├─ IDLE      → Waiting for commands                        │
│  ├─ PLANNING  → LLM generates behavior tree                 │
│  └─ EXECUTING → Behavior Tree controls actions              │
│                                                              │
│  Low-Level: Behavior Tree (NEW)                             │
│  ├─ Reactive decision-making each tick                      │
│  ├─ Parallel behavior execution                             │
│  └─ Dynamic adaptation to world changes                     │
│                                                              │
│  Integration:                                                │
│  └─ LLM → Generates BT JSON → NodeFactory → BTManager       │
└─────────────────────────────────────────────────────────────┘
```

**Implementation Location:**
```java
// New package: C:\Users\casey\steve\src\main\java\com\minewright\behavior\
├── BehaviorTreeManager.java      // Manages BT for an agent
├── Node.java                     // Base node interface
├── nodes/
│   ├── SequenceNode.java
│   ├── SelectorNode.java
│   ├── ParallelNode.java
│   └── ...
├── actions/
│   └── BTActionWrapper.java      // Wraps existing BaseAction
└── conditions/
    └── ConditionNode.java        // Checks world state
```

---

## 4. Integration Points

### 4.1 Point 1: Behavior Tree Manager Integration

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\execution\AgentStateMachine.java`

**Current:**
```java
// AgentStateMachine only tracks lifecycle
public enum AgentState {
    IDLE, PLANNING, EXECUTING, PAUSED, COMPLETED, FAILED
}
```

**Enhanced:**
```java
// Add BEHAVIOR_TREE state for BT-controlled execution
public enum AgentState {
    IDLE,
    PLANNING,           // LLM planning
    EXECUTING_TASK,     // Queue-based execution (current)
    EXECUTING_BT,       // Behavior Tree execution (NEW)
    PAUSED,
    COMPLETED,
    FAILED
}

// In ActionExecutor:
public void tick() {
    switch (stateMachine.getCurrentState()) {
        case EXECUTING_TASK:
            // Current: process task queue
            tickTaskExecution();
            break;
        case EXECUTING_BT:
            // NEW: tick behavior tree
            behaviorTreeManager.tick();
            break;
        // ... other states
    }
}
```

### 4.2 Point 2: LLM → Behavior Tree Pipeline

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\ResponseParser.java`

**Current:**
```java
// LLM returns list of tasks
public class ParsedResponse {
    private String plan;
    private List<Task> tasks;
}
```

**Enhanced:**
```java
// LLM returns tasks + behavior tree specification
public class ParsedResponse {
    private String plan;
    private List<Task> tasks;
    private BehaviorTreeSpec behaviorTree;  // NEW
}

// Behavior tree specification (JSON from LLM)
public class BehaviorTreeSpec {
    private String type;  // "sequence", "selector", "parallel"
    private List<BehaviorTreeSpec> children;
    private Map<String, Object> parameters;
}

// In ResponseParser:
private BehaviorTreeSpec parseBehaviorTree(JsonObject json) {
    // Extract BT structure from LLM JSON response
    // Example:
    // {
    //   "type": "selector",
    //   "children": [
    //     {"type": "sequence", "children": [...]},
    //     {"type": "sequence", "children": [...]}
    //   ]
    // }
}
```

### 4.3 Point 3: Blackboard Integration

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\behavior\blackboard\Blackboard.java`

```java
/**
 * Shared state object for behavior tree nodes.
 * Contains world state, agent state, and memory.
 */
public class Blackboard {
    // Agent state
    private BlockPos position;
    private double health;
    private Inventory inventory;

    // World state
    private Map<BlockPos, BlockState> nearbyBlocks;
    private List<Entity> nearbyEntities;
    private ThreatLevel threatLevel;

    // Memory
    private List<String> recentActions;
    private String currentGoal;
    private long lastPlayerInteraction;

    // Parameters (task-specific)
    private Map<String, Object> parameters;

    // Update from ForemanEntity each tick
    public void update(ForemanEntity foreman) {
        this.position = foreman.blockPosition();
        this.health = foreman.getHealth() / foreman.getMaxHealth();
        scanNearbyBlocks(foreman);
        scanNearbyEntities(foreman);
        // ...
    }
}
```

### 4.4 Point 4: Action Wrapper for BT

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\behavior\actions\BTActionWrapper.java`

```java
/**
 * Wraps existing BaseAction for use in Behavior Trees.
 * Bridges current action system with BT nodes.
 */
public class BTActionWrapper extends ActionNode {
    private final Supplier<BaseAction> actionFactory;
    private BaseAction currentAction;

    @Override
    public Status tick(Blackboard blackboard) {
        // Create action on first tick
        if (currentAction == null) {
            Task task = createTaskFromBlackboard(blackboard);
            currentAction = actionFactory.get();
            currentAction.start();
        }

        // Tick the action
        currentAction.tick();

        // Check completion
        if (currentAction.isComplete()) {
            ActionResult result = currentAction.getResult();
            currentAction = null;
            return result.isSuccess() ? Status.SUCCESS : Status.FAILURE;
        }

        return Status.RUNNING;
    }
}
```

### 4.5 Point 5: Event-Driven BT Nodes

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\behavior\nodes\EventDrivenCondition.java`

```java
/**
 * Event-driven condition node that subscribes to events.
 * More efficient than continuous polling.
 */
public class EventDrivenCondition extends ConditionNode {
    private volatile boolean conditionMet = false;
    private final Class<? extends Event> eventType;

    @Override
    public void subscribe(EventBus eventBus) {
        eventBus.subscribe(eventType, this::onEvent);
    }

    private void onEvent(Event event) {
        if (eventType.isInstance(event)) {
            // Update condition based on event
            conditionMet = evaluateEvent(event);
        }
    }

    @Override
    public Status tick(Blackboard blackboard) {
        // Return cached result from event handler
        return conditionMet ? Status.SUCCESS : Status.FAILURE;
    }
}
```

---

## 5. Code Skeletons

### 5.1 Core Behavior Tree Interfaces

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\behavior\Node.java`

```java
package com.minewright.behavior;

import com.minewright.behavior.blackboard.Blackboard;

/**
 * Base interface for all behavior tree nodes.
 */
public interface Node {
    enum Status {
        SUCCESS,  // Node completed successfully
        FAILURE,  // Node failed
        RUNNING   // Node still executing
    }

    /**
     * Called each game tick to evaluate the node.
     * Must return quickly to avoid server lag.
     */
    Status tick(Blackboard blackboard);

    /**
     * Reset node state (called when tree is restarted).
     */
    void reset();

    /**
     * Get node type for debugging/visualization.
     */
    String getType();
}
```

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\behavior\nodes\SequenceNode.java`

```java
package com.minewright.behavior.nodes;

import com.minewright.behavior.Node;
import com.minewright.behavior.blackboard.Blackboard;
import java.util.List;

/**
 * Executes children in order.
 * Returns SUCCESS only if ALL children succeed.
 * Returns FAILURE immediately if any child fails.
 */
public class SequenceNode implements Node {
    private final List<Node> children;
    private int currentChildIndex = 0;

    public SequenceNode(List<Node> children) {
        this.children = children;
    }

    @Override
    public Status tick(Blackboard blackboard) {
        // No children? Success!
        if (children.isEmpty()) {
            return Status.SUCCESS;
        }

        // Tick current child
        while (currentChildIndex < children.size()) {
            Node child = children.get(currentChildIndex);
            Status status = child.tick(blackboard);

            switch (status) {
                case RUNNING:
                    // Child still running, continue next tick
                    return Status.RUNNING;

                case FAILURE:
                    // Child failed, sequence fails
                    reset();
                    return Status.FAILURE;

                case SUCCESS:
                    // Child succeeded, move to next
                    currentChildIndex++;
                    break;
            }
        }

        // All children succeeded
        reset();
        return Status.SUCCESS;
    }

    @Override
    public void reset() {
        currentChildIndex = 0;
        children.forEach(Node::reset);
    }

    @Override
    public String getType() {
        return "SEQUENCE";
    }
}
```

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\behavior\nodes\SelectorNode.java`

```java
package com.minewright.behavior.nodes;

import com.minewright.behavior.Node;
import com.minewright.behavior.blackboard.Blackboard;
import java.util.List;

/**
 * Executes children in order.
 * Returns SUCCESS when ANY child succeeds.
 * Returns FAILURE only if ALL children fail.
 */
public class SelectorNode implements Node {
    private final List<Node> children;
    private int currentChildIndex = 0;

    public SelectorNode(List<Node> children) {
        this.children = children;
    }

    @Override
    public Status tick(Blackboard blackboard) {
        // No children? Failure!
        if (children.isEmpty()) {
            return Status.FAILURE;
        }

        // Try children in order
        while (currentChildIndex < children.size()) {
            Node child = children.get(currentChildIndex);
            Status status = child.tick(blackboard);

            switch (status) {
                case RUNNING:
                    // Child still running, continue next tick
                    return Status.RUNNING;

                case SUCCESS:
                    // Child succeeded, selector succeeds!
                    reset();
                    return Status.SUCCESS;

                case FAILURE:
                    // Child failed, try next
                    currentChildIndex++;
                    break;
            }
        }

        // All children failed
        reset();
        return Status.FAILURE;
    }

    @Override
    public void reset() {
        currentChildIndex = 0;
        children.forEach(Node::reset);
    }

    @Override
    public String getType() {
        return "SELECTOR";
    }
}
```

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\behavior\nodes\ParallelNode.java`

```java
package com.minewright.behavior.nodes;

import com.minewright.behavior.Node;
import com.minewright.behavior.blackboard.Blackboard;
import java.util.List;

/**
 * Executes ALL children simultaneously.
 * Policy: SUCCESS when ALL succeed, FAILURE when ONE fails.
 */
public class ParallelNode implements Node {
    public enum Policy {
        /** One failure = FAILURE, all success = SUCCESS */
        SEQUENCE,
        /** One success = SUCCESS, all failure = FAILURE */
        SELECTOR
    }

    private final List<Node> children;
    private final Policy policy;

    public ParallelNode(List<Node> children, Policy policy) {
        this.children = children;
        this.policy = policy;
    }

    @Override
    public Status tick(Blackboard blackboard) {
        int successCount = 0;
        int failureCount = 0;
        int runningCount = 0;

        // Tick all children
        for (Node child : children) {
            Status status = child.tick(blackboard);

            switch (status) {
                case SUCCESS:
                    successCount++;
                    break;
                case FAILURE:
                    failureCount++;
                    break;
                case RUNNING:
                    runningCount++;
                    break;
            }
        }

        // Apply policy
        if (policy == Policy.SEQUENCE) {
            // Fail on first failure
            if (failureCount > 0) {
                reset();
                return Status.FAILURE;
            }
            // Success when all succeed
            if (runningCount == 0 && successCount == children.size()) {
                reset();
                return Status.SUCCESS;
            }
        } else {
            // Success on first success
            if (successCount > 0) {
                reset();
                return Status.SUCCESS;
            }
            // Fail when all fail
            if (runningCount == 0 && failureCount == children.size()) {
                reset();
                return Status.FAILURE;
            }
        }

        return Status.RUNNING;
    }

    @Override
    public void reset() {
        children.forEach(Node::reset);
    }

    @Override
    public String getType() {
        return "PARALLEL_" + policy;
    }
}
```

### 5.2 Behavior Tree Manager

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\behavior\BehaviorTreeManager.java`

```java
package com.minewright.behavior;

import com.minewright.behavior.blackboard.Blackboard;
import com.minewright.entity.ForemanEntity;
import com.minewright.execution.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the behavior tree for a single agent.
 * Ticks the tree each game tick and manages blackboard.
 */
public class BehaviorTreeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BehaviorTreeManager.class);

    private final ForemanEntity foreman;
    private final Blackboard blackboard;
    private final EventBus eventBus;

    private BehaviorTree activeTree;
    private BehaviorTree defaultTree;

    public BehaviorTreeManager(ForemanEntity foreman, EventBus eventBus) {
        this.foreman = foreman;
        this.eventBus = eventBus;
        this.blackboard = new Blackboard(foreman);
        this.defaultTree = createDefaultTree();
        this.activeTree = defaultTree;
    }

    /**
     * Called every game tick.
     */
    public void tick() {
        // Update blackboard with current world state
        blackboard.update(foreman);

        // Tick the active behavior tree
        if (activeTree != null) {
            Node.Status status = activeTree.tick(blackboard);

            // Log interesting events
            if (status == Node.Status.FAILURE) {
                LOGGER.warn("Behavior tree failed for {}", foreman.getEntityName());
            }
        }
    }

    /**
     * Set a new behavior tree (from LLM or manual configuration).
     */
    public void setBehaviorTree(BehaviorTree tree) {
        if (tree == null) {
            LOGGER.warn("Attempted to set null behavior tree for {}", foreman.getEntityName());
            return;
        }

        LOGGER.info("Setting new behavior tree for {}: {}",
            foreman.getEntityName(), tree.getName());
        this.activeTree = tree;
        this.activeTree.reset();
    }

    /**
     * Create default idle/follow behavior tree.
     */
    private BehaviorTree createDefaultTree() {
        // [Selector]
        //   ├─ [Sequence] Survival
        //   │   ├─ Condition: Health < 30%
        //   │   └─ Action: Find safe zone
        //   └─ [Sequence] Follow Player
        //       ├─ Condition: Player nearby
        //       └─ Action: Follow player

        // Implementation would build this tree structure
        return new BehaviorTree("Default", null);
    }

    public Blackboard getBlackboard() {
        return blackboard;
    }

    public BehaviorTree getActiveTree() {
        return activeTree;
    }
}
```

### 5.3 Node Factory for LLM Integration

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\behavior\NodeFactory.java`

```java
package com.minewright.behavior;

import com.minewright.behavior.nodes.*;
import com.minewright.behavior.actions.BTActionWrapper;
import com.minewright.behavior.conditions.*;
import com.google.gson.JsonObject;
import java.util.Map;

/**
 * Creates behavior tree nodes from LLM-generated specifications.
 */
public class NodeFactory {
    private final ActionRegistry actionRegistry;
    private final EventBus eventBus;

    public NodeFactory(ActionRegistry actionRegistry, EventBus eventBus) {
        this.actionRegistry = actionRegistry;
        this.eventBus = eventBus;
    }

    /**
     * Create a node from JSON specification (from LLM).
     */
    public Node createNode(JsonObject spec) {
        String type = spec.get("type").getAsString();

        return switch (type) {
            case "sequence" -> createSequence(spec);
            case "selector" -> createSelector(spec);
            case "parallel" -> createParallel(spec);
            case "action" -> createAction(spec);
            case "condition" -> createCondition(spec);
            default -> throw new IllegalArgumentException("Unknown node type: " + type);
        };
    }

    private SequenceNode createSequence(JsonObject spec) {
        var children = spec.getAsJsonArray("children");
        List<Node> childNodes = new ArrayList<>();
        for (var child : children) {
            childNodes.add(createNode(child.getAsJsonObject()));
        }
        return new SequenceNode(childNodes);
    }

    private SelectorNode createSelector(JsonObject spec) {
        var children = spec.getAsJsonArray("children");
        List<Node> childNodes = new ArrayList<>();
        for (var child : children) {
            childNodes.add(createNode(child.getAsJsonObject()));
        }
        return new SelectorNode(childNodes);
    }

    private ParallelNode createParallel(JsonObject spec) {
        var children = spec.getAsJsonArray("children");
        List<Node> childNodes = new ArrayList<>();
        for (var child : children) {
            childNodes.add(createNode(child.getAsJsonObject()));
        }

        String policy = spec.has("policy")
            ? spec.get("policy").getAsString()
            : "sequence";
        return new ParallelNode(childNodes,
            Policy.valueOf(policy.toUpperCase()));
    }

    private Node createAction(JsonObject spec) {
        String actionType = spec.get("action").getAsString();
        Map<String, Object> params = parseParameters(spec);

        // Wrap existing BaseAction in BT node
        return new BTActionWrapper(() ->
            actionRegistry.createAction(actionType, foreman, task, context));
    }

    private Node createCondition(JsonObject spec) {
        String conditionType = spec.get("condition").getAsString();

        return switch (conditionType) {
            case "has_health_above" -> new HealthCondition(
                spec.get("threshold").getAsDouble());
            case "has_item" -> new HasItemCondition(
                spec.get("item").getAsString());
            case "enemy_nearby" -> new EnemyProximityCondition(
                spec.get("distance").getAsInt());
            default -> throw new IllegalArgumentException(
                "Unknown condition: " + conditionType);
        };
    }
}
```

### 5.4 Example: Combat Behavior Tree

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\behavior\trees\CombatBehaviorTree.java`

```java
package com.minewright.behavior.trees;

import com.minewright.behavior.*;
import com.minewright.behavior.nodes.*;
import com.minewright.behavior.actions.*;
import com.minewright.behavior.conditions.*;
import java.util.List;

/**
 * Behavior tree for combat situations.
 * Prioritizes survival, then fighting, then escape.
 */
public class CombatBehaviorTree extends BehaviorTree {

    public CombatBehaviorTree() {
        super("Combat", buildCombatTree());
    }

    private static Node buildCombatTree() {
        // [Selector] Combat Root
        //   ├─ [Sequence] Flee (critical health)
        //   │   ├─ Condition: Health < 30%
        //   │   ├─ Condition: Enemy nearby
        //   │   └─ Action: Run to safe zone
        //   │
        //   ├─ [Sequence] Fight (healthy and armed)
        //   │   ├─ Condition: Has weapon
        //   │   ├─ Condition: Health > 50%
        //   │   └─ Action: Attack enemy
        //   │
        //   └─ [Sequence] Hide (no weapon)
        //       ├─ Condition: No weapon
        //       ├─ Action: Find shelter
        //       └─ Action: Wait for enemy to leave

        return new SelectorNode(List.of(
            // Flee branch
            new SequenceNode(List.of(
                new HealthCondition(0.3, true),  // Health < 30%
                new EnemyProximityCondition(16),  // Enemy within 16 blocks
                new BTActionWrapper(() -> new FleeAction())
            )),

            // Fight branch
            new SequenceNode(List.of(
                new HasItemCondition("weapon"),
                new HealthCondition(0.5, false),  // Health > 50%
                new BTActionWrapper(() -> new AttackAction())
            )),

            // Hide branch
            new SequenceNode(List.of(
                new HasItemCondition("weapon", true),  // Does NOT have weapon
                new BTActionWrapper(() -> new FindShelterAction()),
                new BTActionWrapper(() -> new WaitAction(200))  // Wait 10 seconds
            ))
        ));
    }
}
```

### 5.5 Example: Mining Behavior Tree

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\behavior\trees\MiningBehaviorTree.java`

```java
package com.minewright.behavior.trees;

import com.minewright.behavior.*;
import com.minewright.behavior.nodes.*;
import com.minewright.behavior.actions.*;
import com.minewright.behavior.conditions.*;
import java.util.List;

/**
 * Behavior tree for resource gathering.
 * Adapts to available resources and inventory space.
 */
public class MiningBehaviorTree extends BehaviorTree {

    public MiningBehaviorTree() {
        super("Mining", buildMiningTree());
    }

    private static Node buildMiningTree() {
        // [Selector] Mining Root
        //   ├─ [Sequence] Mine Iron
        //   │   ├─ Condition: Need iron
        //   │   ├─ Condition: Has pickaxe
        //   │   ├─ Action: Find iron ore
        //   │   └─ Action: Mine ore
        //   │
        //   ├─ [Sequence] Mine Coal
        //   │   ├─ Condition: Need coal
        //   │   └─ Action: Mine coal
        //   │
        //   └─ [Sequence] Return to player
        //       ├─ Condition: Inventory full
        //       └─ Action: Path to player

        return new SelectorNode(List.of(
            // Iron branch
            new SequenceNode(List.of(
                new ResourceNeedCondition("iron_ore", 10),
                new HasItemCondition("pickaxe"),
                new BTActionWrapper(() -> new FindResourceAction("iron_ore")),
                new BTActionWrapper(() -> new MineBlockAction())
            )),

            // Coal branch
            new SequenceNode(List.of(
                new ResourceNeedCondition("coal", 20),
                new BTActionWrapper(() -> new FindResourceAction("coal")),
                new BTActionWrapper(() -> new MineBlockAction())
            )),

            // Return branch
            new SequenceNode(List.of(
                new InventoryFullCondition(),
                new BTActionWrapper(() -> new FollowPlayerAction())
            ))
        ));
    }
}
```

---

## 6. Recommendations

### 6.1 Priority 1: Implement Basic Behavior Tree System

**Effort:** 2-3 weeks
**Impact:** HIGH

**Tasks:**
1. Create `com.minewright.behavior` package
2. Implement core nodes (Sequence, Selector, Parallel)
3. Implement Blackboard for world state
4. Create BehaviorTreeManager
5. Write unit tests for node logic

**Files to Create:**
```
C:\Users\casey\steve\src\main\java\com\minewright\behavior\
├── Node.java
├── BehaviorTree.java
├── BehaviorTreeManager.java
├── blackboard\
│   └── Blackboard.java
├── nodes\
│   ├── SequenceNode.java
│   ├── SelectorNode.java
│   └── ParallelNode.java
└── conditions\
    └── ConditionNode.java
```

**Files to Modify:**
```
C:\Users\casey\steve\src\main\java\com\minewright\execution\AgentState.java
  Add EXECUTING_BT state

C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java
  Add behaviorTreeManager field
  Add BEHAVIOR_TREE case in tick()

C:\Users\casey\steve\src\main\java\com\minewright\entity\ForemanEntity.java
  Add getBehaviorTreeManager() method
```

### 6.2 Priority 2: LLM → Behavior Tree Generation

**Effort:** 1-2 weeks
**Impact:** HIGH

**Tasks:**
1. Extend PromptBuilder to request BT JSON
2. Extend ResponseParser to extract BT specification
3. Implement NodeFactory for JSON → Node conversion
4. Test LLM-generated behavior trees
5. Add validation and error handling

**Files to Modify:**
```
C:\Users\casey\steve\src\main\java\com\minewright\llm\PromptBuilder.java
  Add behavior tree template to prompt

C:\Users\casey\steve\src\main\java\com\minewright\llm\ResponseParser.java
  Add parseBehaviorTree() method

C:\Users\casey\steve\src\main\java\com\minewright\behavior\NodeFactory.java
  NEW: Create nodes from JSON
```

**Example Prompt Addition:**
```java
// In PromptBuilder.java
private String addBehaviorTreeTemplate() {
    return """
        When generating a plan, also provide a behavior tree specification:
        {
          "behaviorTree": {
            "type": "selector",
            "children": [
              {
                "type": "sequence",
                "children": [
                  {"type": "condition", "condition": "has_health_above", "threshold": 0.3},
                  {"type": "action", "action": "pathfind", "target": "safe_zone"}
                ]
              }
            ]
          }
        }
        """;
}
```

### 6.3 Priority 3: Predefined Behavior Trees

**Effort:** 1 week
**Impact:** MEDIUM

**Tasks:**
1. Create CombatBehaviorTree
2. Create MiningBehaviorTree
3. Create BuildingBehaviorTree
4. Create IdleFollowBehaviorTree
5. Add BT selection logic based on context

**Files to Create:**
```
C:\Users\casey\steve\src\main\java\com\minewright\behavior\trees\
├── CombatBehaviorTree.java
├── MiningBehaviorTree.java
├── BuildingBehaviorTree.java
└── IdleFollowBehaviorTree.java
```

### 6.4 Priority 4: GOAP System for Fast Planning

**Effort:** 3-4 weeks
**Impact:** MEDIUM

**Tasks:**
1. Implement WorldState representation
2. Implement GoapAction base class
3. Implement A* planner
4. Define basic actions (gather, craft, build)
5. Integrate with ActionExecutor

**Files to Create:**
```
C:\Users\casey\steve\src\main\java\com\minewright\goap\
├── WorldState.java
├── GoapAction.java
├── AStarPlanner.java
├── Goal.java
└── actions\
    ├── GatherWoodAction.java
    ├── CraftPickaxeAction.java
    └── BuildShelterAction.java
```

**Use Case:** Fast local planning for repetitive tasks without LLM calls.

### 6.5 Priority 5: Utility AI for Emotional Responses

**Effort:** 2 weeks
**Impact:** MEDIUM

**Tasks:**
1. Implement UtilityScorer with response curves
2. Define utility actions for social behaviors
3. Integrate with dialogue system
4. Add relationship-based utility modifiers

**Files to Create:**
```
C:\Users\casey\steve\src\main\java\com\minewright\utility\
├── UtilityScorer.java
├── ResponseCurve.java
├── UtilityAction.java
└── curves\
    ├── LinearCurve.java
    ├── LogisticCurve.java
    └── ExponentialCurve.java
```

### 6.6 Priority 6: Subsumption Architecture

**Effort:** 1 week
**Impact:** LOW (nice to have)

**Tasks:**
1. Implement behavior layers (survival, task, idle)
2. Add priority-based override
3. Integrate with BehaviorTreeManager
4. Add fast reflex behaviors

**Implementation:**
```java
// In BehaviorTreeManager.java
public class BehaviorTreeManager {
    private BehaviorTree survivalLayer;    // Highest priority
    private BehaviorTree taskLayer;         // Medium priority
    private BehaviorTree idleLayer;         // Lowest priority

    public void tick() {
        blackboard.update(foreman);

        // Subsumption: higher layers override lower layers
        if (survivalLayer.tick(blackboard) == Status.RUNNING) {
            return;  // Survival behavior active
        }

        if (taskLayer != null && taskLayer.tick(blackboard) == Status.RUNNING) {
            return;  // Task behavior active
        }

        // Fall through to idle
        idleLayer.tick(blackboard);
    }
}
```

---

## 7. Implementation Roadmap

### Phase 1: Foundation (Weeks 1-3)

**Goal:** Basic Behavior Tree System

- [ ] Create behavior package structure
- [ ] Implement Node interface and Status enum
- [ ] Implement SequenceNode, SelectorNode, ParallelNode
- [ ] Implement Blackboard for world state
- [ ] Create BehaviorTreeManager
- [ ] Add unit tests for core nodes

**Deliverables:**
- Working BT node hierarchy
- Blackboard system
- Basic tree execution

### Phase 2: Integration (Weeks 4-5)

**Goal:** Integrate with Existing System

- [ ] Add EXECUTING_BT state to AgentState
- [ ] Modify ActionExecutor to support BT execution
- [ ] Create BTActionWrapper to use existing actions
- [ ] Implement condition nodes (health, inventory, proximity)
- [ ] Create default idle/follow behavior tree

**Deliverables:**
- BT integrated into agent lifecycle
- Existing actions usable in BTs
- Default behaviors working

### Phase 3: LLM Integration (Weeks 6-7)

**Goal:** LLM Generates Behavior Trees

- [ ] Extend PromptBuilder with BT template
- [ ] Extend ResponseParser to extract BT JSON
- [ ] Implement NodeFactory for JSON → Node
- [ ] Add validation for generated BTs
- [ ] Test with various command types

**Deliverables:**
- LLM can generate behavior trees
- Generated trees execute correctly
- Error handling for invalid BTs

### Phase 4: Predefined Behaviors (Week 8)

**Goal:** Common Behavior Patterns

- [ ] Implement CombatBehaviorTree
- [ ] Implement MiningBehaviorTree
- [ ] Implement BuildingBehaviorTree
- [ ] Add BT selection logic
- [ ] Create BT library

**Deliverables:**
- Library of common behavior trees
- Context-aware BT selection
- Documentation for BT patterns

### Phase 5: Advanced Features (Weeks 9-12)

**Goal:** GOAP + Utility AI

- [ ] Implement GOAP system
- [ ] Implement Utility AI system
- [ ] Add subsumption architecture
- [ ] Performance optimization
- [ ] Comprehensive testing

**Deliverables:**
- Fast local planning (GOAP)
- Emotional responses (Utility)
- Layered behaviors (Subsumption)

---

## 8. Testing Strategy

### 8.1 Unit Tests

**File:** `C:\Users\casey\steve\src\test\java\com\minewright\behavior\NodeTest.java`

```java
@Test
public void testSequenceNode_AllChildrenSucceed() {
    MockNode child1 = new MockNode(Status.SUCCESS);
    MockNode child2 = new MockNode(Status.SUCCESS);
    SequenceNode sequence = new SequenceNode(List.of(child1, child2));

    Status result = sequence.tick(mockBlackboard);

    assertEquals(Status.SUCCESS, result);
    assertTrue(child1.wasTicked);
    assertTrue(child2.wasTicked);
}

@Test
public void testSequenceNode_ChildFails() {
    MockNode child1 = new MockNode(Status.SUCCESS);
    MockNode child2 = new MockNode(Status.FAILURE);
    MockNode child3 = new MockNode(Status.SUCCESS);  // Should not tick
    SequenceNode sequence = new SequenceNode(List.of(child1, child2, child3));

    Status result = sequence.tick(mockBlackboard);

    assertEquals(Status.FAILURE, result);
    assertTrue(child1.wasTicked);
    assertTrue(child2.wasTicked);
    assertFalse(child3.wasTicked);  // Never reached
}

@Test
public void testSelectorNode_OneChildSucceeds() {
    MockNode child1 = new MockNode(Status.FAILURE);
    MockNode child2 = new MockNode(Status.SUCCESS);
    MockNode child3 = new MockNode(Status.SUCCESS);  // Should not tick
    SelectorNode selector = new SelectorNode(List.of(child1, child2, child3));

    Status result = selector.tick(mockBlackboard);

    assertEquals(Status.SUCCESS, result);
    assertTrue(child1.wasTicked);
    assertTrue(child2.wasTicked);
    assertFalse(child3.wasTicked);  // Never reached
}

@Test
public void testParallelNode_AllSucceed() {
    MockNode child1 = new MockNode(Status.SUCCESS);
    MockNode child2 = new MockNode(Status.SUCCESS);
    ParallelNode parallel = new ParallelNode(
        List.of(child1, child2),
        ParallelNode.Policy.SEQUENCE
    );

    Status result = parallel.tick(mockBlackboard);

    assertEquals(Status.SUCCESS, result);
    assertTrue(child1.wasTicked);
    assertTrue(child2.wasTicked);
}
```

### 8.2 Integration Tests

**File:** `C:\Users\casey\steve\src\test\java\com\minewright\behavior\BehaviorTreeIntegrationTest.java`

```java
@Test
public void testBehaviorTreeManager_CombatScenario() {
    ForemanEntity foreman = createTestForeman();
    BehaviorTreeManager manager = new BehaviorTreeManager(foreman, eventBus);

    // Set up combat scenario
    foreman.setHealth(5);  // Low health
    spawnEnemyNear(foreman);

    // Tick behavior tree
    manager.tick();

    // Should flee (low health)
    assertTrue(foreman.isMoving());
    assertTrue(foreman.getTargetPosition().isSafeZone());
}

@Test
public void testBehaviorTreeManager_MiningScenario() {
    ForemanEntity foreman = createTestForeman();
    BehaviorTreeManager manager = new BehaviorTreeManager(foreman, eventBus);

    // Set up mining scenario
    foreman.getInventory().clear();
    spawnOreNear(foreman, "iron_ore");
    foreman.giveItem("pickaxe");

    // Tick behavior tree
    manager.tick();

    // Should mine ore
    assertTrue(foreman.isMining());
}

@Test
public void testLLMGeneratedBehaviorTree() {
    ForemanEntity foreman = createTestForeman();
    ActionExecutor executor = foreman.getActionExecutor();

    // Send command that triggers LLM planning
    executor.processNaturalLanguageCommand(
        "Build a house but run away if enemies come near"
    );

    // Wait for async planning
    waitForPlanningComplete(executor);

    // Check that behavior tree was generated
    BehaviorTreeManager btManager = executor.getBehaviorTreeManager();
    assertNotNull(btManager.getActiveTree());
    assertEquals("Generated", btManager.getActiveTree().getName());

    // Verify tree structure
    Node root = btManager.getActiveTree().getRoot();
    assertTrue(root instanceof SelectorNode);  // Should have priority branches
}
```

### 8.3 Performance Tests

**File:** `C:\Users\casey\steve\src\test\java\com\minewright\behavior\PerformanceTest.java`

```java
@Test
public void testBehaviorTreeTickPerformance() {
    BehaviorTree tree = createComplexTree(100);  // 100 nodes
    Blackboard blackboard = createMockBlackboard();

    long startTime = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
        tree.tick(blackboard);
    }
    long duration = System.nanoTime() - startTime;

    // Should tick 1000 times in less than 100ms
    assertTrue(duration < 100_000_000,
        "BT too slow: " + (duration / 1_000_000) + "ms for 1000 ticks");
}

@Test
public void testConcurrentBehaviorTrees() {
    List<ForemanEntity> foremen = createTestAgents(50);
    List<BehaviorTreeManager> managers = foremen.stream()
        .map(f -> new BehaviorTreeManager(f, eventBus))
        .collect(Collectors.toList());

    long startTime = System.nanoTime();
    managers.parallelStream().forEach(m -> m.tick());
    long duration = System.nanoTime() - startTime;

    // Should tick 50 agents in less than 50ms
    assertTrue(duration < 50_000_000,
        "Concurrent BT too slow: " + (duration / 1_000_000) + "ms");
}
```

---

## 9. Conclusion

### 9.1 Summary

MineWright has a **solid foundation** with FSM-based lifecycle management and async LLM planning. However, it lacks **reactive decision-making** capabilities that modern game AI requires.

**Key Recommendations:**

1. **Keep FSM** for lifecycle management (AgentStateMachine is perfect for this)
2. **Add Behavior Trees** for reactive decision-making during execution
3. **Use LLM** to generate behavior trees dynamically based on commands
4. **Add GOAP** for fast local planning of common tasks
5. **Consider Utility AI** for emotional/social responses

### 9.2 Expected Benefits

**Short-term (Phases 1-3):**
- ✅ Reactive behavior (respond to threats, opportunities)
- ✅ Parallel execution (move while watching for enemies)
- ✅ Dynamic planning (adjust strategies mid-execution)
- ✅ Better error recovery (try alternatives on failure)

**Long-term (Phases 4-5):**
- ✅ Fast local planning (no API calls for common tasks)
- ✅ Emotional AI (relationship-based behaviors)
- ✅ Layered behaviors (survival > task > idle)
- ✅ Scalable to many agents

### 9.3 Risk Mitigation

| Risk | Mitigation |
|------|------------|
| **Performance** | Keep BT ticking < 5ms, use event-driven nodes |
| **Complexity** | Start simple, add features incrementally |
| **LLM Quality** | Validate generated BTs, have predefined fallbacks |
| **Debugging** | Add BT visualization, extensive logging |
| **Integration** | Use wrapper pattern to avoid rewriting actions |

### 9.4 Success Metrics

- **Performance:** BT tick < 5ms, LLM planning still async
- **Reliability:** < 1% BT execution failures
- **Flexibility:** LLM can generate BTs for 80%+ of commands
- **Maintainability:** New behaviors added via composition, not code changes
- **User Experience:** Agents respond intelligently to changing conditions

---

## 10. References

### 10.1 Research Documents

- `C:\Users\casey\steve\docs\research\BEHAVIOR_TREES_DESIGN.md` - BT fundamentals
- `C:\Users\casey\steve\docs\research\GAME_AI_PATTERNS.md` - AI patterns overview
- `C:\Users\casey\steve\docs\research\GAME_AI_ARCHITECTURES.md` - Architecture comparison
- `C:\Users\casey\steve\docs\research\ARCHITECTURE_B_STATE_MACHINE.md` - FSM patterns
- `C:\Users\casey\steve\docs\research\ARCHITECTURE_D_GOAP.md` - GOAP implementation

### 10.2 Current Implementation

- `C:\Users\casey\steve\src\main\java\com\minewright\execution\AgentStateMachine.java` - FSM
- `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java` - Execution
- `C:\Users\casey\steve\src\main\java\com\minewright\llm\TaskPlanner.java` - LLM planning
- `C:\Users\casey\steve\CLAUDE.md` - Project overview

### 10.3 External Resources

- **Behavior Trees in Game AI:** Isla, M. and Blount, D. (2005)
- **Programming Game AI by Example:** Buckland, M. (2005)
- **Artificial Intelligence for Games:** Rabin, S. (2021)
- **Horizon: Zero Dawn AI Architecture:** Guerrilla Games GDC talk

---

**Document End**

*This analysis provides a roadmap for implementing modern behavior systems in MineWright while preserving the existing FSM-based lifecycle management and LLM planning system.*
