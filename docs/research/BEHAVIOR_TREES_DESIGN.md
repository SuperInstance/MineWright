# Behavior Trees Design for MineWright

**Author:** Research Team
**Date:** 2026-02-28
**Version:** 1.0
**Status:** Design Document

---

## Table of Contents

1. [Overview](#overview)
2. [Behavior Tree Fundamentals](#behavior-tree-fundamentals)
3. [FSM vs Behavior Trees](#fsm-vs-behavior-trees)
4. [Architecture Design](#architecture-design)
5. [Implementation](#implementation)
6. [Example Behavior Trees](#example-behavior-trees)
7. [LLM Integration](#llm-integration)
8. [Minecraft-Specific Considerations](#minecraft-specific-considerations)
9. [Performance Optimization](#performance-optimization)
10. [Testing Strategy](#testing-strategy)

---

## Overview

### Purpose

This document outlines the design and implementation of a Behavior Tree system for MineWright, a Minecraft mod where AI agents (ForemanEntities) autonomously execute tasks. Behavior Trees will provide low-level reactive behaviors controlled by high-level LLM reasoning.

### Goals

- **Reactive Decision-Making**: Enable agents to respond quickly to changing world conditions
- **Hierarchical Organization**: Structure complex behaviors as composable trees
- **Dynamic Adaptation**: Allow LLM to modify behavior trees at runtime
- **Performance**: Maintain 20 TPS (ticks per second) with minimal overhead
- **Extensibility**: Easy to add new behaviors through node composition

### Integration with Existing Systems

The Behavior Tree system integrates with:
- **ActionExecutor**: Handles high-level task execution (current implementation)
- **AgentStateMachine**: Tracks agent states (IDLE, PLANNING, EXECUTING, etc.)
- **TacticalDecisionService**: Fast reflex decisions via Cloudflare Edge
- **LLM Planning**: High-level goal setting and behavior tree generation

---

## Behavior Tree Fundamentals

### What is a Behavior Tree?

A Behavior Tree is a hierarchical model for encoding decision-making and execution flows. Unlike Finite State Machines which use explicit state transitions, Behavior Trees use a tree of nodes that are evaluated each tick.

### Node Types

#### 1. Control Nodes (Composite)

Control nodes have multiple children and determine execution flow.

##### Sequence Node

Executes children in order. Returns **SUCCESS** only if ALL children succeed. Returns **FAILURE** immediately if any child fails.

```
[Sequence]
├── Child 1 (must succeed)
├── Child 2 (must succeed)
└── Child 3 (must succeed)
```

**Use Case**: "Find ore AND path to ore AND mine ore"

##### Selector Node

Executes children in order. Returns **SUCCESS** when ANY child succeeds. Returns **FAILURE** only if all children fail.

```
[Selector]
├── Child 1 (try first)
├── Child 2 (if 1 fails)
└── Child 3 (if 2 fails)
```

**Use Case**: "Attack enemy OR flee OR hide"

##### Parallel Node

Executes ALL children simultaneously. Return value depends on policy:

- **Parallel Selector**: One FAILURE → FAILURE; All SUCCESS → SUCCESS
- **Parallel Sequence**: One SUCCESS → SUCCESS; All FAILURE → FAILURE
- **Parallel Hybrid**: Returns based on threshold

```
[Parallel Sequence]
├── Look at player
├── Move toward goal
└── Avoid obstacles
```

**Use Case**: "Move while looking around while avoiding obstacles"

#### 2. Decorator Nodes

Decorator nodes modify their single child's behavior or result.

##### Inverter

Reverses the result (SUCCESS ↔ FAILURE).

```
[Inverter]
└── Is enemy nearby?
```

**Use Case**: "Continue while NOT at destination"

##### Repeater

Repeats child node N times or infinitely.

```
[Repeater: 3]
└── Attack enemy
```

**Use Case**: "Attack enemy 3 times then stop"

##### Succeeder

Always returns SUCCESS regardless of child result.

```
[Succeeder]
└── Log failure
```

**Use Case**: "Always log failures but don't fail the sequence"

##### Cooldown / Throttle

Limits execution frequency.

```
[Cooldown: 100 ticks]
└── Check for enemies
```

**Use Case**: "Check for threats every 5 seconds"

##### Timeout

Fails if child doesn't complete within time limit.

```
[Timeout: 200 ticks]
└── Path to destination
```

**Use Case**: "Give up on pathfinding after 10 seconds"

#### 3. Leaf Nodes

Leaf nodes perform actual work or check conditions.

##### Condition Node

Checks a condition, returns SUCCESS or FAILURE. No side effects.

```java
class HasOreCondition : ConditionNode {
    boolean evaluate() {
        return inventory.contains(IRON_ORE);
    }
}
```

##### Action Node

Executes an action. Returns RUNNING until complete, then SUCCESS or FAILURE.

```java
class MineBlockAction : ActionNode {
    NodeStatus tick() {
        if (isComplete()) return SUCCESS;
        if (canMine()) {
            mineBlock();
            return RUNNING;
        }
        return FAILURE;
    }
}
```

### Node Return States

| State | Description |
|-------|-------------|
| **SUCCESS** | Node completed successfully |
| **FAILURE** | Node failed |
| **RUNNING** | Node still executing (action nodes only) |

### How Behavior Trees Tick

```
Each Game Tick (20 times per second):
│
├── 1. Blackboard.update()  // Update world state
├── 2. root.tick()          // Evaluate tree from root
│       │
│       ├── If child returns SUCCESS → next child
│       ├── If child returns FAILURE → handle based on node type
│       └── If child returns RUNNING → stop, continue next tick
│
└── 3. Execute selected action
```

**Key Principles:**
- Trees are evaluated every tick (reactive)
- RUNNING nodes save state and continue next tick
- No long-running operations in tick() (non-blocking)
- Results are not cached (re-evaluate conditions each tick)

---

## FSM vs Behavior Trees

### Comparison Table

| Aspect | Finite State Machine (FSM) | Behavior Tree (BT) |
|--------|---------------------------|-------------------|
| **Complexity** | O(n²) transitions | O(n) nodes |
| **Parallel Tasks** | Requires multiple FSMs | Native support via Parallel nodes |
| **Maintainability** | Difficult at scale | Easy to extend (modular) |
| **Debugging** | Log-based | Visual tree structure |
| **Reactivity** | Event-driven (state changes) | Continuous evaluation (each tick) |
| **Hierarchical** | Flat (or HFSM) | Naturally hierarchical |
| **State Tracking** | Explicit states | Implicit (node traversal) |
| **Learning Curve** | Simple | Moderate |
| **Performance** | Very low overhead | Slightly higher (tree traversal) |
| **Best For** | Simple, linear AI | Complex, hierarchical AI |

### When to Use FSM

**Use FSM when:**
- Simple state transitions (IDLE → WALK → RUN)
- Performance-critical with many AI agents
- Linear, predictable behavior patterns
- Quick prototyping
- Event-driven behavior suffices

**Example in MineWright:**
```java
// AgentStateMachine.java (existing)
public enum AgentState {
    IDLE, PLANNING, EXECUTING, PAUSED, COMPLETED, FAILED
}
```
This high-level agent lifecycle is well-suited for FSM.

### When to Use Behavior Trees

**Use Behavior Trees when:**
- Complex NPC decision-making
- Need parallel behavior execution
- Frequently changing AI requirements
- Long-term maintainability matters
- Designer-friendly visual editing needed
- Reactive to continuous world state

**Example in MineWright:**
```java
// Low-level worker behaviors
[Selector]
├── [Sequence] Combat behavior
│   ├── Is threatened?
│   └── Fight or flee
├── [Sequence] Mining behavior
│   ├── Has tools?
│   ├── Find ore
│   └── Mine ore
└── [Sequence] Building behavior
    ├── Has materials?
    ├── Path to site
    └── Place blocks
```

### Hybrid Approach (Recommended for MineWright)

The **best approach** combines both:

```
High-Level: FSM (AgentStateMachine)
├── IDLE
├── PLANNING  (LLM generates behavior tree)
├── EXECUTING (Behavior Tree controls actions)
└── COMPLETED

Low-Level: Behavior Trees (reactive behaviors)
├── MiningWorkerBT
├── BuildingWorkerBT
└── CombatWorkerBT
```

**Benefits:**
- FSM manages lifecycle and high-level state
- Behavior Trees handle complex reactive behaviors
- Clear separation of concerns
- LLM generates behavior trees, FSM manages execution

### Implementation Strategy

```java
public class HybridAIController {
    private AgentStateMachine stateMachine;  // High-level
    private BehaviorTreeManager btManager;   // Low-level

    public void tick() {
        // FSM controls lifecycle
        switch (stateMachine.getCurrentState()) {
            case EXECUTING:
                // Behavior Tree handles reactive behaviors
                btManager.tick();
                break;
            // ... other states
        }
    }
}
```

---

## Architecture Design

### System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    ForemanEntity                             │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ AgentStateMachine│  │ActionExecutor    │ (High-level)   │
│  │  (Lifecycle)     │  │  (Task Queue)    │                │
│  └──────────────────┘  └──────────────────┘                │
│                                                              │
│  ┌──────────────────────────────────────────────────┐      │
│  │         BehaviorTreeManager (NEW)                 │      │
│  │  ┌────────────────────────────────────────────┐   │      │
│  │  │  BehaviorTree (Agent-specific)             │   │      │
│  │  │  ├─ Blackboard (World State)               │   │      │
│  │  │  └─ Root Node (Composite)                  │   │      │
│  │  │     ├─ Selector (High-level goals)         │   │      │
│  │  │     │  ├─ Sequence (Combat)                │   │      │
│  │  │     │  ├─ Sequence (Mining)                │   │      │
│  │  │     │  └─ Sequence (Building)              │   │      │
│  │  │     └─ Leaf Nodes (Actions/Conditions)     │   │      │
│  │  └────────────────────────────────────────────┘   │      │
│  └──────────────────────────────────────────────────┘      │
│                                                              │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ TacticalService  │  │     LLM          │                │
│  │  (Fast Reflexes) │  │  (Planner)       │                │
│  └──────────────────┘  └──────────────────┘                │
└─────────────────────────────────────────────────────────────┘
```

### Core Components

#### 1. BehaviorTreeManager

Manages the behavior tree for a single agent.

**Responsibilities:**
- Creates and maintains behavior trees
- Ticks the tree each game tick
- Manages blackboard (world state)
- Integrates with existing ActionExecutor

#### 2. BehaviorTree

Represents a complete behavior tree.

**Responsibilities:**
- Holds root node reference
- Manages tree lifecycle
- Provides tree visualization/debugging

#### 3. Node Hierarchy

```
Node (abstract)
├── CompositeNode
│   ├── SequenceNode
│   ├── SelectorNode
│   └── ParallelNode
├── DecoratorNode
│   ├── InverterNode
│   ├── RepeaterNode
│   ├── CooldownNode
│   └── TimeoutNode
└── LeafNode
    ├── ConditionNode
    └── ActionNode
```

#### 4. Blackboard

Shared state object containing world information.

**Contents:**
- Agent state (position, health, inventory)
- World state (nearby blocks, entities, threats)
- Memory (recent actions, goals)
- Parameters (task-specific data)

#### 5. NodeFactory

Creates nodes from LLM-generated specifications.

**Responsibilities:**
- Parses LLM JSON output
- Creates node instances
- Builds tree structure
- Validates tree integrity

### Data Flow

```
User Command
    ↓
LLM Planner (generates tasks + behavior tree JSON)
    ↓
ResponseParser (extracts behavior tree spec)
    ↓
NodeFactory (builds tree from JSON)
    ↓
BehaviorTreeManager (sets active tree)
    ↓
Each Tick:
    ↓
Blackboard.update() (refresh world state)
    ↓
RootNode.tick() (evaluate tree)
    ↓
ActionNode.execute() (if selected)
    ↓
ActionResult (success/failure)
```

---

## Implementation

### Base Node Interface

```java
package com.minewright.behavior;

import com.minewright.behavior.blackboard.Blackboard;

/**
 * Base interface for all behavior tree nodes.
 *
 * <p>All nodes must implement the tick() method which is called
 * each game tick. Nodes should be stateless where possible,
 * storing state in the Blackboard instead.</p>
 *
 * <p><b>Thread Safety:</b> Nodes are ticked on the main game thread
 * and should not be accessed from other threads without synchronization.</p>
 *
 * @since 1.2.0
 */
public interface Node {

    /**
     * Node status enumeration.
     */
    enum Status {
        /** Node completed successfully */
        SUCCESS,
        /** Node failed */
        FAILURE,
        /** Node still executing (action nodes only) */
        RUNNING
    }

    /**
     * Called each game tick to evaluate the node.
     *
     * <p>This method should return quickly to avoid server lag.
     * Long-running operations must be split across multiple ticks
     * using the RUNNING status.</p>
     *
     * @param blackboard The blackboard containing world state
     * @return The node's status
     */
    Status tick(Blackboard blackboard);

    /**
     * Resets the node's internal state.
     * Called when the behavior tree is restarted.
     */
    void reset();

    /**
     * Returns a human-readable description of this node.
     * Used for debugging and visualization.
     *
     * @return Node description
     */
    String getDescription();

    /**
     * Returns the node type for serialization.
     *
     * @return Node type string
     */
    String getNodeName();
}
```

### Abstract Base Classes

#### CompositeNode

```java
package com.minewright.behavior.nodes;

import com.minewright.behavior.Node;
import com.minewright.behavior.blackboard.Blackboard;
import java.util.List;

/**
 * Base class for composite nodes (nodes with children).
 *
 * <p>Composite nodes control the execution flow of their children.
 * Subclasses implement different execution strategies (sequence, selector, etc.).</p>
 */
public abstract class CompositeNode implements Node {

    protected final List<Node> children;
    protected int currentChild = 0;

    protected CompositeNode(List<Node> children) {
        this.children = List.copyOf(children); // Immutable
    }

    @Override
    public void reset() {
        currentChild = 0;
        for (Node child : children) {
            child.reset();
        }
    }

    /**
     * Returns the list of children (immutable).
     */
    public List<Node> getChildren() {
        return children;
    }

    /**
     * Returns the current child index being executed.
     */
    protected int getCurrentChildIndex() {
        return currentChild;
    }
}
```

#### DecoratorNode

```java
package com.minewright.behavior.nodes;

import com.minewright.behavior.Node;
import com.minewright.behavior.blackboard.Blackboard;

/**
 * Base class for decorator nodes (nodes with one child).
 *
 * <p>Decorator nodes modify their child's behavior or result.
 * Examples include Inverter, Repeater, Cooldown, etc.</p>
 */
public abstract class DecoratorNode implements Node {

    protected final Node child;

    protected DecoratorNode(Node child) {
        this.child = child;
    }

    @Override
    public void reset() {
        child.reset();
    }

    /**
     * Returns the decorated child node.
     */
    public Node getChild() {
        return child;
    }
}
```

### Composite Node Implementations

#### SequenceNode

```java
package com.minewright.behavior.nodes;

import com.minewright.behavior.Node;
import com.minewright.behavior.blackboard.Blackboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Executes children in sequence.
 *
 * <p>Returns SUCCESS if all children succeed.
 * Returns FAILURE immediately if any child fails.
 * Returns RUNNING if current child is still executing.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>
 * [Sequence]
 * ├── Find nearest ore
 * ├── Path to ore
 * └── Mine ore
 * </pre>
 *
 * <p>All actions must complete successfully for the sequence to succeed.</p>
 */
public class SequenceNode extends CompositeNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(SequenceNode.class);

    public SequenceNode(List<Node> children) {
        super(children);
    }

    @Override
    public Status tick(Blackboard blackboard) {
        if (children.isEmpty()) {
            return Status.SUCCESS;
        }

        // Tick current child
        Node child = children.get(currentChild);
        Status childStatus = child.tick(blackboard);

        switch (childStatus) {
            case RUNNING:
                // Child still executing, continue next tick
                return Status.RUNNING;

            case FAILURE:
                // Child failed, sequence fails
                LOGGER.debug("Sequence node failed at child {}: {}",
                    currentChild, child.getDescription());
                // Reset to allow retry
                currentChild = 0;
                return Status.FAILURE;

            case SUCCESS:
                // Child succeeded, move to next
                currentChild++;
                if (currentChild >= children.size()) {
                    // All children succeeded
                    currentChild = 0; // Reset for next run
                    return Status.SUCCESS;
                }
                // Continue to next child next tick
                return Status.RUNNING;

            default:
                return Status.FAILURE;
        }
    }

    @Override
    public String getDescription() {
        return "Sequence[" + children.size() + " children]";
    }

    @Override
    public String getNodeName() {
        return "sequence";
    }
}
```

#### SelectorNode

```java
package com.minewright.behavior.nodes;

import com.minewright.behavior.Node;
import com.minewright.behavior.blackboard.Blackboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Executes children in order until one succeeds.
 *
 * <p>Returns SUCCESS when any child succeeds.
 * Returns FAILURE only if all children fail.
 * Returns RUNNING if current child is still executing.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>
 * [Selector]
 * ├── Attack enemy (has weapon)
 * ├── Flee from enemy (no weapon)
 * └── Hide (no escape)
 * </pre>
 *
 * <p>First successful action wins. Try options in priority order.</p>
 */
public class SelectorNode extends CompositeNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(SelectorNode.class);

    public SelectorNode(List<Node> children) {
        super(children);
    }

    @Override
    public Status tick(Blackboard blackboard) {
        if (children.isEmpty()) {
            return Status.FAILURE;
        }

        // Tick current child
        Node child = children.get(currentChild);
        Status childStatus = child.tick(blackboard);

        switch (childStatus) {
            case RUNNING:
                // Child still executing
                return Status.RUNNING;

            case SUCCESS:
                // Child succeeded, selector succeeds
                LOGGER.debug("Selector node succeeded at child {}: {}",
                    currentChild, child.getDescription());
                currentChild = 0; // Reset for next run
                return Status.SUCCESS;

            case FAILURE:
                // Child failed, try next
                currentChild++;
                if (currentChild >= children.size()) {
                    // All children failed
                    currentChild = 0; // Reset for next run
                    return Status.FAILURE;
                }
                // Try next child next tick
                return Status.RUNNING;

            default:
                return Status.FAILURE;
        }
    }

    @Override
    public String getDescription() {
        return "Selector[" + children.size() + " children]";
    }

    @Override
    public String getNodeName() {
        return "selector";
    }
}
```

#### ParallelNode

```java
package com.minewright.behavior.nodes;

import com.minewright.behavior.Node;
import com.minewright.behavior.blackboard.Blackboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Executes all children simultaneously.
 *
 * <p>This implementation uses Parallel Sequence policy:
 * Returns SUCCESS when one child succeeds.
 * Returns FAILURE when all children fail.
 * Returns RUNNING until a decision is reached.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>
 * [Parallel Sequence]
 * ├── Move toward goal
 * ├── Look around for threats
 * └── Avoid obstacles
 * </pre>
 *
 * <p>All actions run simultaneously. Success if any completes.</p>
 */
public class ParallelNode extends CompositeNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelNode.class);

    /**
     * Policy for combining child results.
     */
    public enum Policy {
        /** One failure → FAILURE, All success → SUCCESS */
        SELECTOR,
        /** One success → SUCCESS, All failure → FAILURE */
        SEQUENCE,
        /** Threshold based on count */
        THRESHOLD
    }

    private final Policy policy;
    private final int threshold;

    public ParallelNode(List<Node> children, Policy policy) {
        this(children, policy, 0);
    }

    public ParallelNode(List<Node> children, Policy policy, int threshold) {
        super(children);
        this.policy = policy;
        this.threshold = threshold;
    }

    @Override
    public Status tick(Blackboard blackboard) {
        if (children.isEmpty()) {
            return Status.FAILURE;
        }

        int successCount = 0;
        int failureCount = 0;
        int runningCount = 0;

        // Tick all children
        for (Node child : children) {
            Status status = child.tick(blackboard);

            switch (status) {
                case SUCCESS -> successCount++;
                case FAILURE -> failureCount++;
                case RUNNING -> runningCount++;
            }
        }

        // Determine result based on policy
        return switch (policy) {
            case SELECTOR -> {
                // Fail if any child failed
                if (failureCount > 0) yield Status.FAILURE;
                // Success if all succeeded
                if (successCount == children.size()) yield Status.SUCCESS;
                // Still running
                yield Status.RUNNING;
            }
            case SEQUENCE -> {
                // Success if any child succeeded
                if (successCount > 0) yield Status.SUCCESS;
                // Fail if all failed
                if (failureCount == children.size()) yield Status.FAILURE;
                // Still running
                yield Status.RUNNING;
            }
            case THRESHOLD -> {
                int totalCount = successCount + failureCount;
                if (totalCount >= threshold) {
                    yield successCount >= threshold ? Status.SUCCESS : Status.FAILURE;
                }
                yield Status.RUNNING;
            }
        };
    }

    @Override
    public void reset() {
        super.reset();
    }

    @Override
    public String getDescription() {
        return "Parallel[" + policy + ", " + children.size() + " children]";
    }

    @Override
    public String getNodeName() {
        return "parallel";
    }
}
```

### Decorator Node Implementations

#### InverterNode

```java
package com.minewright.behavior.nodes;

import com.minewright.behavior.Node;
import com.minewright.behavior.blackboard.Blackboard;

/**
 * Inverts the result of its child.
 *
 * <p>SUCCESS → FAILURE<br>
 * FAILURE → SUCCESS<br>
 * RUNNING → RUNNING</p>
 *
 * <p><b>Example:</b></p>
 * <pre>
 * [Inverter]
 * └── Is inventory full?
 *
 * Returns SUCCESS if inventory is NOT full.
 * </pre>
 */
public class InverterNode extends DecoratorNode {

    public InverterNode(Node child) {
        super(child);
    }

    @Override
    public Status tick(Blackboard blackboard) {
        Status childStatus = child.tick(blackboard);

        return switch (childStatus) {
            case SUCCESS -> Status.FAILURE;
            case FAILURE -> Status.SUCCESS;
            case RUNNING -> Status.RUNNING;
        };
    }

    @Override
    public String getDescription() {
        return "Inverter[" + child.getDescription() + "]";
    }

    @Override
    public String getNodeName() {
        return "inverter";
    }
}
```

#### RepeaterNode

```java
package com.minewright.behavior.nodes;

import com.minewright.behavior.Node;
import com.minewright.behavior.blackboard.Blackboard;

/**
 * Repeats its child node a specified number of times.
 *
 * <p>Returns SUCCESS after child has succeeded N times.
 * Returns FAILURE immediately if child fails.
 * Returns RUNNING while repeating.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>
 * [Repeater: 3]
 * └── Attack enemy
 *
 * Attacks enemy 3 times then succeeds.
 * </pre>
 */
public class RepeaterNode extends DecoratorNode {

    private final int repeatCount;
    private int currentIteration = 0;

    /**
     * Creates a repeater that repeats forever.
     * Use with Cooldown or Timeout to prevent infinite loops.
     */
    public static RepeaterNode infinite(Node child) {
        return new RepeaterNode(child, -1);
    }

    public RepeaterNode(Node child, int repeatCount) {
        super(child);
        this.repeatCount = repeatCount;
    }

    @Override
    public Status tick(Blackboard blackboard) {
        Status childStatus = child.tick(blackboard);

        switch (childStatus) {
            case FAILURE:
                // Child failed, repeater fails
                currentIteration = 0;
                return Status.FAILURE;

            case SUCCESS:
                currentIteration++;
                if (repeatCount > 0 && currentIteration >= repeatCount) {
                    // Done repeating
                    currentIteration = 0;
                    return Status.SUCCESS;
                }
                // Reset child to repeat
                child.reset();
                return Status.RUNNING;

            case RUNNING:
                return Status.RUNNING;

            default:
                return Status.FAILURE;
        }
    }

    @Override
    public void reset() {
        super.reset();
        currentIteration = 0;
    }

    @Override
    public String getDescription() {
        return repeatCount > 0
            ? "Repeater[" + repeatCount + "x " + child.getDescription() + "]"
            : "Repeater[∞ " + child.getDescription() + "]";
    }

    @Override
    public String getNodeName() {
        return "repeater";
    }
}
```

#### CooldownNode

```java
package com.minewright.behavior.nodes;

import com.minewright.behavior.Node;
import com.minewright.behavior.blackboard.Blackboard;

/**
 * Adds a cooldown period between executions.
 *
 * <p>Returns FAILURE during cooldown.
 * Otherwise delegates to child.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>
 * [Cooldown: 100 ticks]
 * └── Check for enemies
 *
 * Only checks for enemies every 5 seconds.
 * </pre>
 */
public class CooldownNode extends DecoratorNode {

    private final int cooldownTicks;
    private long lastExecutionTick = -1;

    public CooldownNode(Node child, int cooldownTicks) {
        super(child);
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public Status tick(Blackboard blackboard) {
        long currentTick = blackboard.getGameTime();

        if (lastExecutionTick >= 0 &&
            currentTick - lastExecutionTick < cooldownTicks) {
            // Still in cooldown
            return Status.FAILURE;
        }

        // Not in cooldown, execute child
        Status result = child.tick(blackboard);

        if (result != Status.RUNNING) {
            // Child completed, start cooldown
            lastExecutionTick = currentTick;
        }

        return result;
    }

    @Override
    public void reset() {
        super.reset();
        lastExecutionTick = -1;
    }

    @Override
    public String getDescription() {
        return "Cooldown[" + cooldownTicks + "t, " + child.getDescription() + "]";
    }

    @Override
    public String getNodeName() {
        return "cooldown";
    }
}
```

### Leaf Node Implementations

#### ConditionNode (Abstract)

```java
package com.minewright.behavior.leaf;

import com.minewright.behavior.Node;
import com.minewright.behavior.blackboard.Blackboard;

/**
 * Base class for condition nodes.
 *
 * <p>Condition nodes check a condition and return SUCCESS or FAILURE.
 * They should not modify world state or have side effects.</p>
 */
public abstract class ConditionNode implements Node {

    @Override
    public final Status tick(Blackboard blackboard) {
        boolean result = evaluate(blackboard);
        return result ? Status.SUCCESS : Status.FAILURE;
    }

    /**
     * Evaluates the condition.
     *
     * @param blackboard The blackboard
     * @return true if condition is met
     */
    protected abstract boolean evaluate(Blackboard blackboard);

    @Override
    public void reset() {
        // Conditions have no state to reset
    }
}
```

#### ActionNode (Abstract)

```java
package com.minewright.behavior.leaf;

import com.minewright.behavior.Node;
import com.minewright.behavior.blackboard.Blackboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for action nodes.
 *
 * <p>Action nodes perform actions and return:
 * <ul>
 *   <li>RUNNING - while executing</li>
 *   <li>SUCCESS - when completed successfully</li>
 *   <li>FAILURE - if the action failed</li>
 * </ul></p>
 *
 * <p>Actions must be non-blocking. Long-running operations should
 * be split across multiple ticks using RUNNING status.</p>
 */
public abstract class ActionNode implements Node {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionNode.class);

    protected boolean started = false;

    @Override
    public final Status tick(Blackboard blackboard) {
        if (!started) {
            onStart(blackboard);
            started = true;
        }

        Status status = onTick(blackboard);

        if (status != Status.RUNNING) {
            // Action completed
            onComplete(blackboard, status);
            started = false;
        }

        return status;
    }

    /**
     * Called once when the action starts.
     */
    protected void onStart(Blackboard blackboard) {
        LOGGER.debug("Starting action: {}", getDescription());
    }

    /**
     * Called each tick while the action is running.
     *
     * @return The current status
     */
    protected abstract Status onTick(Blackboard blackboard);

    /**
     * Called when the action completes (success or failure).
     */
    protected void onComplete(Blackboard blackboard, Status status) {
        LOGGER.debug("Action completed: {} -> {}", getDescription(), status);
    }

    @Override
    public void reset() {
        started = false;
    }
}
```

### BehaviorTreeManager

```java
package com.minewright.behavior;

import com.minewright.behavior.blackboard.Blackboard;
import com.minewright.behavior.nodes.SelectorNode;
import com.minewright.behavior.nodes.SequenceNode;
import com.minewright.entity.ForemanEntity;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

/**
 * Manages behavior tree execution for a single agent.
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *   <li>Maintains the active behavior tree</li>
 *   <li>Updates blackboard with world state</li>
 *   <li>Ticks the tree each game tick</li>
 *   <li>Provides debugging and visualization</li>
 *   <li>Integrates with existing ActionExecutor</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * BehaviorTreeManager manager = new BehaviorTreeManager(foreman);
 * manager.setTree(miningWorkerTree);
 *
 * // In entity tick():
 * manager.tick();
 * </pre>
 *
 * @since 1.2.0
 */
public class BehaviorTreeManager {

    private static final Logger LOGGER = TestLogger.getLogger(BehaviorTreeManager.class);

    private final ForemanEntity foreman;
    private final Blackboard blackboard;
    private BehaviorTree activeTree;
    private boolean enabled = true;
    private long totalTicks = 0;
    private long successCount = 0;
    private long failureCount = 0;

    public BehaviorTreeManager(ForemanEntity foreman) {
        this.foreman = foreman;
        this.blackboard = new Blackboard(foreman);
    }

    /**
     * Sets the active behavior tree.
     *
     * @param tree The behavior tree to use (can be null to disable)
     */
    public void setTree(BehaviorTree tree) {
        if (this.activeTree != null) {
            this.activeTree.reset();
        }
        this.activeTree = tree;
        LOGGER.info("[{}] Behavior tree set: {}",
            foreman.getEntityName(),
            tree != null ? tree.getName() : "none");
    }

    /**
     * Gets the active behavior tree.
     */
    public BehaviorTree getTree() {
        return activeTree;
    }

    /**
     * Main update loop called each game tick.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Updates the blackboard with current world state</li>
     *   <li>Ticks the behavior tree if enabled</li>
     *   <li>Tracks statistics for debugging</li>
     * </ol>
     *
     * <p><b>Thread Safety:</b> Called on the main game thread only.</p>
     */
    public void tick() {
        if (!enabled || activeTree == null) {
            return;
        }

        try {
            // Update blackboard with current world state
            blackboard.update();

            // Tick the behavior tree
            Node.Status result = activeTree.tick(blackboard);

            // Track statistics
            totalTicks++;
            switch (result) {
                case SUCCESS -> successCount++;
                case FAILURE -> failureCount++;
            }

        } catch (Exception e) {
            LOGGER.error("[{}] Error ticking behavior tree",
                foreman.getEntityName(), e);
            // Don't crash the game, just log and continue
        }
    }

    /**
     * Resets the behavior tree and statistics.
     */
    public void reset() {
        if (activeTree != null) {
            activeTree.reset();
        }
        totalTicks = 0;
        successCount = 0;
        failureCount = 0;
    }

    /**
     * Gets the blackboard for external access.
     */
    public Blackboard getBlackboard() {
        return blackboard;
    }

    /**
     * Enables or disables behavior tree execution.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.info("[{}] Behavior tree execution: {}",
            foreman.getEntityName(),
            enabled ? "ENABLED" : "DISABLED");
    }

    /**
     * Returns whether behavior tree execution is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets statistics about tree execution.
     */
    public Statistics getStatistics() {
        return new Statistics(totalTicks, successCount, failureCount);
    }

    /**
     * Statistics record for debugging.
     */
    public record Statistics(long totalTicks, long successCount, long failureCount) {
        public double getSuccessRate() {
            return totalTicks > 0 ? (double) successCount / totalTicks : 0.0;
        }

        public double getFailureRate() {
            return totalTicks > 0 ? (double) failureCount / totalTicks : 0.0;
        }
    }
}
```

### Blackboard

```java
package com.minewright.behavior.blackboard;

import com.minewright.entity.ForemanEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * Shared state object for behavior tree execution.
 *
 * <p>The blackboard contains all information about the world,
 * agent, and task that behavior tree nodes may need to access.</p>
 *
 * <p><b>Contents:</b></p>
 * <ul>
 *   <li><b>Agent State:</b> Position, health, inventory, equipment</li>
 *   <li><b>World State:</b> Nearby blocks, entities, threats</li>
 *   <li><b>Task State:</b> Current goal, target, parameters</li>
 *   <li><b>Memory:</b> Recent actions, observations</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Updated on the main game thread only.
 * Behavior tree nodes should read but not modify the blackboard
 * during tick() execution.</p>
 *
 * @since 1.2.0
 */
public class Blackboard {

    private final ForemanEntity agent;
    private long gameTime = 0;

    // Agent State
    private BlockPos position;
    private float health;
    private float maxHealth;
    private Map<String, Integer> inventory;

    // World State
    private List<BlockPos> nearbyBlocks = new ArrayList<>();
    private List<Entity> nearbyEntities = new ArrayList<>();
    private List<BlockPos> threats = new ArrayList<>();
    private BlockPos nearestOre;
    private BlockPos nearestTree;
    private BlockPos nearestChest;

    // Task State
    private String currentGoal;
    private BlockPos targetPosition;
    private String targetBlock;
    private Map<String, Object> parameters = new HashMap<>();

    // Memory
    private Queue<String> recentActions = new LinkedList<>();
    private static final int MAX_RECENT_ACTIONS = 10;
    private Set<BlockPos> visitedLocations = new HashSet<>();

    public Blackboard(ForemanEntity agent) {
        this.agent = agent;
        update();
    }

    /**
     * Updates the blackboard with current world state.
     * Called at the start of each behavior tree tick.
     */
    public void update() {
        gameTime = agent.level().getGameTime();
        position = agent.blockPosition();
        health = agent.getHealth();
        maxHealth = agent.getMaxHealth();

        // Update world state periodically (not every tick for performance)
        if (gameTime % 20 == 0) {  // Once per second
            updateWorldState();
        }
    }

    private void updateWorldState() {
        Level level = agent.level();

        // Scan nearby blocks (16 block radius)
        nearbyBlocks.clear();
        BlockPos.betweenClosedStream(
            position.offset(-16, -16, -16),
            position.offset(16, 16, 16)
        ).forEach(bp -> {
            BlockState state = level.getBlockState(bp);
            if (!state.isAir()) {
                nearbyBlocks.add(bp.immutable());
            }
        });

        // Find nearby entities
        nearbyEntities = level.getEntitiesOfClass(
            Entity.class,
            new AABB(position).inflate(16.0)
        );

        // Find nearest ore
        nearestOre = findNearestBlock("ore");
        nearestTree = findNearestBlock("log");
        nearestChest = findNearestBlock("chest");

        // Find threats (hostile mobs, lava, etc.)
        threats.clear();
        nearbyEntities.forEach(e -> {
            if (isHostile(e)) {
                if (e.blockPosition() != null) {
                    threats.add(e.blockPosition());
                }
            }
        });
    }

    private BlockPos findNearestBlock(String type) {
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (BlockPos pos : nearbyBlocks) {
            BlockState state = agent.level().getBlockState(pos);
            String blockName = state.getBlock().toString().toLowerCase();

            if (blockName.contains(type.toLowerCase())) {
                double dist = pos.distSqr(position);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = pos;
                }
            }
        }

        return nearest;
    }

    private boolean isHostile(Entity entity) {
        String entityName = entity.getType().toString().toLowerCase();
        return entityName.contains("zombie") ||
               entityName.contains("skeleton") ||
               entityName.contains("creeper") ||
               entityName.contains("spider");
    }

    // Getters for behavior tree nodes

    public ForemanEntity getAgent() { return agent; }
    public long getGameTime() { return gameTime; }
    public BlockPos getPosition() { return position; }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public float getHealthPercentage() {
        return maxHealth > 0 ? (health / maxHealth) * 100 : 0;
    }

    public List<BlockPos> getNearbyBlocks() {
        return List.copyOf(nearbyBlocks);
    }

    public List<Entity> getNearbyEntities() {
        return List.copyOf(nearbyEntities);
    }

    public List<BlockPos> getThreats() {
        return List.copyOf(threats);
    }

    public boolean hasThreats() {
        return !threats.isEmpty();
    }

    public BlockPos getNearestOre() { return nearestOre; }
    public BlockPos getNearestTree() { return nearestTree; }
    public BlockPos getNearestChest() { return nearestChest; }

    public String getCurrentGoal() { return currentGoal; }
    public void setCurrentGoal(String goal) { this.currentGoal = goal; }

    public BlockPos getTargetPosition() { return targetPosition; }
    public void setTargetPosition(BlockPos pos) { this.targetPosition = pos; }

    public String getTargetBlock() { return targetBlock; }
    public void setTargetBlock(String block) { this.targetBlock = block; }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type, T defaultValue) {
        Object value = parameters.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }

    public void addRecentAction(String action) {
        recentActions.offer(action);
        if (recentActions.size() > MAX_RECENT_ACTIONS) {
            recentActions.poll();
        }
    }

    public Queue<String> getRecentActions() {
        return new LinkedList<>(recentActions);
    }

    public void markVisited(BlockPos pos) {
        visitedLocations.add(pos.immutable());
    }

    public boolean hasVisited(BlockPos pos) {
        return visitedLocations.contains(pos);
    }
}
```

### BehaviorTree

```java
package com.minewright.behavior;

import com.minewright.behavior.blackboard.Blackboard;
import com.minewright.behavior.nodes.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Represents a complete behavior tree.
 *
 * <p>A behavior tree consists of a root node (typically a Selector or Sequence)
 * which contains the entire behavior hierarchy.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>
 * BehaviorTree miningTree = new BehaviorTree.Builder()
 *     .name("mining_worker")
 *     .root(miningWorkerSelector)
 *     .build();
 * </pre>
 *
 * @since 1.2.0
 */
public class BehaviorTree {

    private static final Logger LOGGER = LoggerFactory.getLogger(BehaviorTree.class);

    private final String name;
    private final Node root;
    private final UUID id;
    private final long createdAt;

    private BehaviorTree(Builder builder) {
        this.name = builder.name;
        this.root = builder.root;
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Ticks the behavior tree.
     *
     * @param blackboard The blackboard
     * @return The root node's status
     */
    public Node.Status tick(Blackboard blackboard) {
        return root.tick(blackboard);
    }

    /**
     * Resets all nodes in the tree.
     */
    public void reset() {
        root.reset();
    }

    public String getName() { return name; }
    public Node getRoot() { return root; }
    public UUID getId() { return id; }
    public long getCreatedAt() { return createdAt; }

    /**
     * Builder for creating behavior trees.
     */
    public static class Builder {
        private String name = "unnamed";
        private Node root;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder root(Node root) {
            this.root = root;
            return this;
        }

        public BehaviorTree build() {
            if (root == null) {
                throw new IllegalArgumentException("Root node cannot be null");
            }
            return new BehaviorTree(this);
        }
    }

    /**
     * Creates a string representation of the tree structure.
     * Useful for debugging and visualization.
     */
    public String visualize() {
        StringBuilder sb = new StringBuilder();
        sb.append("Behavior Tree: ").append(name).append("\n");
        visualizeNode(root, "", sb);
        return sb.toString();
    }

    private void visualizeNode(Node node, String indent, StringBuilder sb) {
        sb.append(indent).append("├─ ").append(node.getDescription()).append("\n");

        if (node instanceof CompositeNode composite) {
            for (Node child : composite.getChildren()) {
                visualizeNode(child, indent + "│  ", sb);
            }
        } else if (node instanceof DecoratorNode decorator) {
            visualizeNode(decorator.getChild(), indent + "│  ", sb);
        }
    }
}
```

---

## Example Behavior Trees

### MiningWorker Behavior Tree

```java
package com.minewright.behavior.trees;

import com.minewright.behavior.*;
import com.minewright.behavior.leaf.*;
import com.minewright.behavior.nodes.*;
import com.minewright.behavior.conditions.*;
import com.minewright.behavior.actions.*;

import java.util.List;

/**
 * Behavior tree for a mining worker agent.
 *
 * <p><b>Behavior Hierarchy:</b></p>
 * <pre>
 * [Selector] MiningWorkerSelector
 * ├── [Sequence] HandleThreats
 * │   ├── IsThreatened?
 * │   └── [Selector] ThreatResponse
 * │       ├── HasWeapon? → Attack
 * │       ├── CanFlee? → Flee
 * │       └── Hide
 * ├── [Sequence] MiningBehavior
 * │   ├── HasTool? (pickaxe)
 * │   ├── FindNearestOre
 * │   ├── [Repeater: 3] → PathToTarget → MineBlock
 * │   └── ReturnToBase
 * └── [Sequence] IdleBehavior
 *     ├── [Cooldown: 100] → LookForWork
 *     └── FollowPlayer
 * </pre>
 */
public class MiningWorkerTree {

    public static BehaviorTree create() {
        return new BehaviorTree.Builder()
            .name("mining_worker")
            .root(createRoot())
            .build();
    }

    private static Node createRoot() {
        // Main selector: try options in priority order
        return new SelectorNode(List.of(
            handleThreatsSequence(),
            miningSequence(),
            idleSequence()
        ));
    }

    /**
     * Handle immediate threats before doing anything else.
     */
    private static Node handleThreatsSequence() {
        return new SequenceNode(List.of(
            new IsThreatenedCondition(),
            new SelectorNode(List.of(
                combatSequence(),
                fleeSequence(),
                hideSequence()
            ))
        ));
    }

    /**
     * Combat sequence if we have a weapon.
     */
    private static Node combatSequence() {
        return new SequenceNode(List.of(
            new HasWeaponCondition(),
            new CombatActionNode()
        ));
    }

    /**
     * Flee sequence if we can escape.
     */
    private static Node fleeSequence() {
        return new SequenceNode(List.of(
            new CanFleeCondition(),
            new FleeActionNode()
        ));
    }

    /**
     * Hide sequence as last resort.
     */
    private static Node hideSequence() {
        return new SequenceNode(List.of(
            new HideActionNode()
        ));
    }

    /**
     * Main mining behavior.
     */
    private static Node miningSequence() {
        return new SequenceNode(List.of(
            // Check if we have a pickaxe
            new HasToolCondition("pickaxe"),

            // Find nearest ore
            new FindNearestOreAction(),

            // Mine up to 3 blocks before returning to base
            new RepeaterNode(
                new SequenceNode(List.of(
                    new PathToTargetAction(),
                    new MineBlockAction()
                )),
                3
            ),

            // Return to base and deposit
            new ReturnToBaseAction(),
            new DepositInventoryAction()
        ));
    }

    /**
     * Idle behavior when no work to do.
     */
    private static Node idleSequence() {
        return new SequenceNode(List.of(
            // Only look for work every 5 seconds
            new CooldownNode(
                new LookForWorkAction(),
                100  // ticks
            ),

            // Otherwise follow nearest player
            new FollowPlayerActionNode()
        ));
    }
}
```

### Condition Nodes for Mining

```java
package com.minewright.behavior.conditions;

import com.minewright.behavior.leaf.ConditionNode;
import com.minewright.behavior.blackboard.Blackboard;

/**
 * Checks if the agent is currently threatened.
 */
public class IsThreatenedCondition extends ConditionNode {

    @Override
    protected boolean evaluate(Blackboard blackboard) {
        return blackboard.hasThreats();
    }

    @Override
    public String getDescription() {
        return "IsThreatened?";
    }

    @Override
    public String getNodeName() {
        return "is_threatened";
    }
}

/**
 * Checks if the agent has a weapon.
 */
public class HasWeaponCondition extends ConditionNode {

    @Override
    protected boolean evaluate(Blackboard blackboard) {
        return blackboard.getAgent()
            .getInventory()
            .hasAnyMatching(stack ->
                stack.getItem().toString().toLowerCase().contains("sword")
            );
    }

    @Override
    public String getDescription() {
        return "HasWeapon?";
    }

    @Override
    public String getNodeName() {
        return "has_weapon";
    }
}

/**
 * Checks if the agent can flee from threats.
 */
public class CanFleeCondition extends ConditionNode {

    @Override
    protected boolean evaluate(Blackboard blackboard) {
        // Can flee if health > 30%
        return blackboard.getHealthPercentage() > 30f;
    }

    @Override
    public String getDescription() {
        return "CanFlee?";
    }

    @Override
    public String getNodeName() {
        return "can_flee";
    }
}

/**
 * Checks if the agent has a specific tool.
 */
public class HasToolCondition extends ConditionNode {

    private final String toolType;

    public HasToolCondition(String toolType) {
        this.toolType = toolType;
    }

    @Override
    protected boolean evaluate(Blackboard blackboard) {
        return blackboard.getAgent()
            .getInventory()
            .hasAnyMatching(stack ->
                stack.getItem().toString().toLowerCase().contains(toolType)
            );
    }

    @Override
    public String getDescription() {
        return "Has" + toolType + "?";
    }

    @Override
    public String getNodeName() {
        return "has_tool";
    }
}
```

### Action Nodes for Mining

```java
package com.minewright.behavior.actions;

import com.minewright.behavior.leaf.ActionNode;
import com.minewright.behavior.blackboard.Blackboard;
import com.minewright.behavior.Node.Status;

/**
 * Action node for combat behavior.
 */
public class CombatActionNode extends ActionNode {

    @Override
    protected Status onTick(Blackboard blackboard) {
        // Get nearest threat
        var threats = blackboard.getThreats();
        if (threats.isEmpty()) {
            return Status.FAILURE;
        }

        // Target and attack
        // This would integrate with existing CombatAction
        // For now, just return success
        return Status.SUCCESS;
    }

    @Override
    public String getDescription() {
        return "AttackEnemy";
    }

    @Override
    public String getNodeName() {
        return "combat";
    }
}

/**
 * Action node for fleeing from threats.
 */
public class FleeActionNode extends ActionNode {

    @Override
    protected Status onTick(Blackboard blackboard) {
        // Find safe location and move away from threats
        // This would integrate with pathfinding system
        return Status.SUCCESS;
    }

    @Override
    public String getDescription() {
        return "FleeFromThreat";
    }

    @Override
    public String getNodeName() {
        return "flee";
    }
}

/**
 * Action node for finding the nearest ore.
 */
public class FindNearestOreAction extends ActionNode {

    @Override
    protected Status onTick(Blackboard blackboard) {
        BlockPos nearestOre = blackboard.getNearestOre();

        if (nearestOre == null) {
            return Status.FAILURE;  // No ore found
        }

        // Set as target
        blackboard.setTargetPosition(nearestOre);
        blackboard.setParameter("ore_type",
            blackboard.getAgent().level().getBlockState(nearestOre).getBlock().toString());

        return Status.SUCCESS;
    }

    @Override
    public String getDescription() {
        return "FindNearestOre";
    }

    @Override
    public String getNodeName() {
        return "find_ore";
    }
}
```

### BuildingWorker Behavior Tree

```java
package com.minewright.behavior.trees;

import com.minewright.behavior.*;
import com.minewright.behavior.nodes.*;
import com.minewright.behavior.actions.*;

import java.util.List;

/**
 * Behavior tree for a building worker agent.
 *
 * <p><b>Behavior Hierarchy:</b></p>
 * <pre>
 * [Selector] BuildingWorkerSelector
 * ├── [Sequence] HandleThreats (same as mining)
 * ├── [Sequence] BuildingBehavior
 * │   ├── HasMaterials?
 * │   ├── [Parallel] MoveAndPrepare
 * │   │   ├── PathToBuildSite
 * │   │   └── ClearArea
 * │   ├── [Repeater] → PlaceBlock
 * │   └── ReportProgress
 * └── [Sequence] GatherMaterials
 *     ├── FindChest
 *     ├── PathToChest
 *     └── TakeMaterials
 * </pre>
 */
public class BuildingWorkerTree {

    public static BehaviorTree create() {
        return new BehaviorTree.Builder()
            .name("building_worker")
            .root(createRoot())
            .build();
    }

    private static Node createRoot() {
        return new SelectorNode(List.of(
            handleThreatsSequence(),  // Reuse from mining
            buildingSequence(),
            gatherMaterialsSequence(),
            idleSequence()
        ));
    }

    private static Node handleThreatsSequence() {
        // Same as MiningWorkerTree
        return MiningWorkerTree.handleThreatsSequence();
    }

    /**
     * Main building behavior.
     */
    private static Node buildingSequence() {
        return new SequenceNode(List.of(
            new HasMaterialsCondition(),

            // Move to site and clear area in parallel
            new ParallelNode(List.of(
                new PathToBuildSiteAction(),
                new ClearAreaAction()
            ), ParallelNode.Policy.SEQUENCE),

            // Place blocks until structure is complete
            RepeaterNode.infinite(
                new SequenceNode(List.of(
                    new PlaceBlockAction(),
                    new ReportProgressAction()
                ))
            )
        ));
    }

    /**
     * Gather materials when inventory is empty.
     */
    private static Node gatherMaterialsSequence() {
        return new SequenceNode(List.of(
            new FindNearestChestAction(),
            new PathToTargetAction(),
            new TakeMaterialsAction()
        ));
    }

    private static Node idleSequence() {
        // Same as MiningWorkerTree
        return MiningWorkerTree.idleSequence();
    }
}
```

### CombatWorker Behavior Tree

```java
package com.minewright.behavior.trees;

import com.minewright.behavior.*;
import com.minewright.behavior.nodes.*;
import com.minewright.behavior.conditions.*;
import com.minewright.behavior.actions.*;

import java.util.List;

/**
 * Behavior tree for a combat-focused worker agent.
 *
 * <p><b>Behavior Hierarchy:</b></p>
 * <pre>
 * [Selector] CombatWorkerSelector
 * ├── [Sequence] CriticalCondition
 * │   ├── HealthBelow30%?
 * │   └── [Selector) EmergencyResponse
 * │       ├── CanHeal? → UseHealthPotion
 * │       ├── CanReachBase? → RetreatToBase
 * │       └── CallForHelp
 * ├── [Sequence] CombatBehavior
 * │   ├── IsThreatened?
 * │   ├── [Selector) TargetSelection
 * │   │   ├── NearestHostile? → TargetNearest
 * │   │   ├── LowestHealth? → TargetWeakest
 * │   │   └── TargetAny
 * │   ├── [Parallel) CombatActions
 * │   │   ├── AttackTarget
 * │   │   ├── StrafeMovement
 * │   │   └── BlockIfShield
 * │   └── [Cooldown: 20] → ReevaluateTarget
 * └── PatrolBehavior
 * </pre>
 */
public class CombatWorkerTree {

    public static BehaviorTree create() {
        return new BehaviorTree.Builder()
            .name("combat_worker")
            .root(createRoot())
            .build();
    }

    private static Node createRoot() {
        return new SelectorNode(List.of(
            criticalConditionSequence(),
            combatSequence(),
            patrolSequence()
        ));
    }

    /**
     * Handle critical health condition.
     */
    private static Node criticalConditionSequence() {
        return new SequenceNode(List.of(
            new HealthBelowThresholdCondition(30f),
            new SelectorNode(List.of(
                healSequence(),
                retreatSequence(),
                callForHelpSequence()
            ))
        ));
    }

    private static Node healSequence() {
        return new SequenceNode(List.of(
            new HasItemCondition("potion"),
            new UseHealthPotionAction()
        ));
    }

    private static Node retreatSequence() {
        return new SequenceNode(List.of(
            new CanReachBaseCondition(),
            new RetreatToBaseAction()
        ));
    }

    private static Node callForHelpSequence() {
        return new SequenceNode(List.of(
            new CallForHelpAction()
        ));
    }

    /**
     * Main combat behavior.
     */
    private static Node combatSequence() {
        return new SequenceNode(List.of(
            new IsThreatenedCondition(),

            // Select target
            new SelectorNode(List.of(
                targetNearestSequence(),
                targetWeakestSequence(),
                targetAnySequence()
            )),

            // Execute combat (parallel actions)
            new ParallelNode(List.of(
                new AttackTargetAction(),
                new StrafeMovementAction(),
                new BlockIfShieldAction()
            ), ParallelNode.Policy.SEQUENCE),

            // Reevaluate target periodically
            new CooldownNode(
                new ReevaluateTargetAction(),
                20  // ticks
            )
        ));
    }

    private static Node targetNearestSequence() {
        return new SequenceNode(List.of(
            new NearestHostileCondition(),
            new TargetNearestAction()
        ));
    }

    private static Node targetWeakestSequence() {
        return new SequenceNode(List.of(
            new WeakestTargetCondition(),
            new TargetWeakestAction()
        ));
    }

    private static Node targetAnySequence() {
        return new SequenceNode(List.of(
            new TargetAnyAction()
        ));
    }

    /**
     * Patrol behavior when no threats.
     */
    private static Node patrolSequence() {
        return new SequenceNode(List.of(
            new InverterNode(new IsThreatenedCondition()),
            new PatrolActionNode()
        ));
    }
}
```

---

## LLM Integration

### Dynamic Behavior Tree Generation

The LLM can generate behavior trees at runtime by outputting JSON specifications that the NodeFactory can parse.

#### LLM Prompt Template

```java
package com.minewright.behavior.llm;

import com.minewright.llm.PromptBuilder;

/**
 * Prompt builder for behavior tree generation.
 */
public class BehaviorTreePromptBuilder extends PromptBuilder {

    public String buildTreeGenerationPrompt(String agentRole, String taskDescription) {
        return """
            You are a game AI behavior tree designer. Create a behavior tree for a Minecraft agent.

            ## Agent Role
            {{AGENT_ROLE}}

            ## Task
            {{TASK_DESCRIPTION}}

            ## Available Node Types

            ### Composite Nodes
            - sequence: Execute children in order, all must succeed
            - selector: Execute children in order, first success wins
            - parallel: Execute all children simultaneously

            ### Decorator Nodes
            - inverter: Invert child result
            - repeater: Repeat child N times
            - cooldown: Add cooldown between executions
            - timeout: Fail if child doesn't complete in time

            ### Leaf Nodes
            - condition: Check world state (returns success/failure)
            - action: Execute action (returns running until complete)

            ## Available Conditions
            - is_threatened: Are there nearby threats?
            - has_health_below: Is health below threshold?
            - has_item: Do we have a specific item?
            - has_tool: Do we have a specific tool?
            - is_inventory_full: Is inventory full?
            - can_reach: Can we reach the target?
            - target_exists: Does the target exist?

            ## Available Actions
            - find_ore: Find nearest ore
            - find_tree: Find nearest tree
            - find_chest: Find nearest chest
            - path_to_target: Path to target position
            - mine_block: Mine a block
            - place_block: Place a block
            - attack_target: Attack target entity
            - flee: Flee from threats
            - retreat: Retreat to base
            - use_item: Use an item
            - follow_player: Follow nearest player

            ## Output Format

            Return a JSON behavior tree specification:

            ```json
            {
              "name": "behavior_tree_name",
              "description": "Human-readable description",
              "tree": {
                "type": "selector|sequence|parallel",
                "children": [
                  {
                    "type": "sequence",
                    "children": [
                      {
                        "type": "condition",
                        "name": "is_threatened"
                      },
                      {
                        "type": "action",
                        "name": "flee"
                      }
                    ]
                  }
                ]
              }
            }
            ```

            ## Guidelines

            1. Prioritize safety first (threat handling)
            2. Use selectors for fallback options
            3. Use sequences for required steps
            4. Use parallel for independent actions
            5. Add cooldowns to prevent spam
            6. Always include an idle/fallback behavior
            7. Keep trees shallow (max 5 levels deep)

            Generate a behavior tree for this task.
            """
            .replace("{{AGENT_ROLE}}", agentRole)
            .replace("{{TASK_DESCRIPTION}}", taskDescription);
    }
}
```

#### NodeFactory for LLM Integration

```java
package com.minewright.behavior.factory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewright.behavior.*;
import com.minewright.behavior.nodes.*;
import com.minewright.behavior.leaf.*;
import com.minewright.behavior.conditions.*;
import com.minewright.behavior.actions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating behavior trees from LLM-generated JSON.
 *
 * <p>Parses JSON output from LLM and builds the corresponding
 * behavior tree structure.</p>
 *
 * @since 1.2.0
 */
public class NodeFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeFactory.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Creates a behavior tree from JSON.
     *
     * @param json The JSON specification
     * @return The created behavior tree
     * @throws IllegalArgumentException if JSON is invalid
     */
    public static BehaviorTree fromJson(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);

            String name = root.path("name").asText("unnamed");
            String description = root.path("description").asText("");
            JsonNode treeNode = root.path("tree");

            Node rootNode = createNode(treeNode);

            return new BehaviorTree.Builder()
                .name(name)
                .root(rootNode)
                .build();

        } catch (Exception e) {
            LOGGER.error("Failed to create behavior tree from JSON", e);
            throw new IllegalArgumentException("Invalid behavior tree JSON", e);
        }
    }

    /**
     * Recursively creates a node from JSON.
     */
    private static Node createNode(JsonNode nodeJson) {
        String type = nodeJson.path("type").asText();

        return switch (type) {
            // Composite nodes
            case "sequence" -> createSequence(nodeJson);
            case "selector" -> createSelector(nodeJson);
            case "parallel" -> createParallel(nodeJson);

            // Decorator nodes
            case "inverter" -> createInverter(nodeJson);
            case "repeater" -> createRepeater(nodeJson);
            case "cooldown" -> createCooldown(nodeJson);
            case "timeout" -> createTimeout(nodeJson);

            // Leaf nodes
            case "condition" -> createCondition(nodeJson);
            case "action" -> createAction(nodeJson);

            default -> throw new IllegalArgumentException("Unknown node type: " + type);
        };
    }

    private static Node createSequence(JsonNode nodeJson) {
        List<Node> children = createChildren(nodeJson);
        return new SequenceNode(children);
    }

    private static Node createSelector(JsonNode nodeJson) {
        List<Node> children = createChildren(nodeJson);
        return new SelectorNode(children);
    }

    private static Node createParallel(JsonNode nodeJson) {
        List<Node> children = createChildren(nodeJson);
        String policy = nodeJson.path("policy").asText("sequence");
        ParallelNode.Policy p = ParallelNode.Policy.valueOf(policy.toUpperCase());
        return new ParallelNode(children, p);
    }

    private static Node createInverter(JsonNode nodeJson) {
        Node child = createSingleChild(nodeJson);
        return new InverterNode(child);
    }

    private static Node createRepeater(JsonNode nodeJson) {
        Node child = createSingleChild(nodeJson);
        int count = nodeJson.path("count").asInt(-1);
        return count > 0 ? new RepeaterNode(child, count) : RepeaterNode.infinite(child);
    }

    private static Node createCooldown(JsonNode nodeJson) {
        Node child = createSingleChild(nodeJson);
        int ticks = nodeJson.path("ticks").asInt(100);
        return new CooldownNode(child, ticks);
    }

    private static Node createTimeout(JsonNode nodeJson) {
        Node child = createSingleChild(nodeJson);
        int ticks = nodeJson.path("ticks").asInt(200);
        return new TimeoutNode(child, ticks);
    }

    private static Node createCondition(JsonNode nodeJson) {
        String name = nodeJson.path("name").asText();

        return switch (name) {
            case "is_threatened" -> new IsThreatenedCondition();
            case "has_health_below" -> {
                float threshold = (float) nodeJson.path("threshold").asDouble(30.0);
                yield new HealthBelowThresholdCondition(threshold);
            }
            case "has_item" -> {
                String item = nodeJson.path("item").asText();
                yield new HasItemCondition(item);
            }
            case "has_tool" -> {
                String tool = nodeJson.path("tool").asText();
                yield new HasToolCondition(tool);
            }
            case "is_inventory_full" -> new IsInventoryFullCondition();
            case "can_reach" -> new CanReachCondition();
            case "target_exists" -> new TargetExistsCondition();
            default -> throw new IllegalArgumentException("Unknown condition: " + name);
        };
    }

    private static Node createAction(JsonNode nodeJson) {
        String name = nodeJson.path("name").asText();

        return switch (name) {
            case "find_ore" -> new FindNearestOreAction();
            case "find_tree" -> new FindNearestTreeAction();
            case "find_chest" -> new FindNearestChestAction();
            case "path_to_target" -> new PathToTargetAction();
            case "mine_block" -> new MineBlockAction();
            case "place_block" -> new PlaceBlockAction();
            case "attack_target" -> new AttackTargetAction();
            case "flee" -> new FleeActionNode();
            case "retreat" -> new RetreatToBaseAction();
            case "use_item" -> {
                String item = nodeJson.path("item").asText();
                yield new UseItemAction(item);
            }
            case "follow_player" -> new FollowPlayerActionNode();
            default -> throw new IllegalArgumentException("Unknown action: " + name);
        };
    }

    private static List<Node> createChildren(JsonNode nodeJson) {
        JsonNode childrenJson = nodeJson.path("children");
        List<Node> children = new ArrayList<>();

        for (JsonNode childJson : childrenJson) {
            children.add(createNode(childJson));
        }

        return children;
    }

    private static Node createSingleChild(JsonNode nodeJson) {
        JsonNode childrenJson = nodeJson.path("children");
        if (childrenJson.isEmpty() || !childrenJson.isArray()) {
            throw new IllegalArgumentException("Decorator node must have exactly one child");
        }

        return createNode(childrenJson.get(0));
    }
}
```

### Integration with ActionExecutor

```java
package com.minewright.action;

import com.minewright.behavior.BehaviorTree;
import com.minewright.behavior.BehaviorTreeManager;
import com.minewright.behavior.factory.NodeFactory;
import com.minewright.llm.ResponseParser;
import com.minewright.entity.ForemanEntity;

// Add to ActionExecutor class

/**
 * Behavior tree manager for low-level reactive behaviors.
 */
private BehaviorTreeManager behaviorTreeManager;

/**
 * Initializes the behavior tree manager.
 */
private void initBehaviorTreeManager() {
    this.behaviorTreeManager = new BehaviorTreeManager(foreman);

    // Set default behavior tree based on role
    switch (foreman.getRole()) {
        case WORKER -> {
            // Check for assigned task type
            String taskType = foreman.getMemory().getCurrentTaskType();
            if (taskType != null) {
                setBehaviorTreeForTask(taskType);
            } else {
                // Default to mining worker
                behaviorTreeManager.setTree(com.minewright.behavior.trees.MiningWorkerTree.create());
            }
        }
        case FOREMAN -> {
            // Foreman uses higher-level orchestration, simpler BT
            behaviorTreeManager.setTree(com.minewright.behavior.trees.ForemanTree.create());
        }
        case SOLO -> {
            // Solo agent can do everything
            behaviorTreeManager.setTree(com.minewright.behavior.trees.SoloAgentTree.create());
        }
    }
}

/**
 * Sets behavior tree based on task type.
 */
private void setBehaviorTreeForTask(String taskType) {
    behaviorTreeManager.setTree(switch (taskType.toLowerCase()) {
        case "mining", "mine" -> com.minewright.behavior.trees.MiningWorkerTree.create();
        case "building", "build" -> com.minewright.behavior.trees.BuildingWorkerTree.create();
        case "combat", "guard", "defend" -> com.minewright.behavior.trees.CombatWorkerTree.create();
        default -> com.minewright.behavior.trees.MiningWorkerTree.create();
    });
}

/**
 * Processes LLM response that may include behavior tree specification.
 */
private void processLLMResponseWithBehaviorTree(ResponseParser.ParsedResponse response) {
    // Extract behavior tree JSON if present
    String behaviorTreeJson = response.getBehaviorTreeJson();

    if (behaviorTreeJson != null && !behaviorTreeJson.isBlank()) {
        try {
            // Parse and set behavior tree from LLM
            BehaviorTree tree = NodeFactory.fromJson(behaviorTreeJson);
            behaviorTreeManager.setTree(tree);
            LOGGER.info("Behavior tree updated from LLM: {}", tree.getName());
        } catch (Exception e) {
            LOGGER.warn("Failed to parse behavior tree from LLM, using default", e);
            initBehaviorTreeManager();
        }
    }

    // Rest of existing processing...
    currentGoal = response.getPlan();
    taskQueue.clear();
    taskQueue.addAll(response.getTasks());
}

/**
 * Modified tick() to include behavior tree.
 */
public void tick() {
    ticksSinceLastAction++;

    // ... existing async planning check ...

    // NEW: Tick behavior tree if enabled
    if (behaviorTreeManager != null && behaviorTreeManager.isEnabled()) {
        behaviorTreeManager.tick();
    }

    // ... rest of existing tick logic ...
}
```

---

## Minecraft-Specific Considerations

### Game Tick Integration

```java
package com.minewright.entity;

import com.minewright.behavior.BehaviorTreeManager;

// Add to ForemanEntity

/**
 * Behavior tree manager for low-level behaviors.
 */
private BehaviorTreeManager behaviorTreeManager;

/**
 * Initialize behavior tree manager in constructor.
 */
public ForemanEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
    super(entityType, level);
    // ... existing initialization ...

    this.behaviorTreeManager = new BehaviorTreeManager(this);
}

/**
 * Integrate behavior tree into entity tick().
 */
@Override
public void tick() {
    try {
        super.tick();
    } catch (Exception e) {
        LOGGER.error("[{}] Critical error in parent entity tick", entityName, e);
    }

    if (!this.level().isClientSide) {
        // ... existing tick logic ...

        // NEW: Tick behavior tree AFTER action executor
        // This allows behavior tree to provide reactive fallback behaviors
        try {
            if (behaviorTreeManager != null) {
                behaviorTreeManager.tick();
            }
        } catch (Exception e) {
            LOGGER.warn("[{}] Behavior tree error (continuing without BT)", entityName, e);
        }

        // ... rest of tick logic ...
    }
}

/**
 * Get behavior tree manager for external access.
 */
public BehaviorTreeManager getBehaviorTreeManager() {
    return behaviorTreeManager;
}
```

### Performance Considerations

1. **Don't Update Blackboard Every Tick**
   - Update world state every 20 ticks (once per second)
   - Cache expensive computations

2. **Avoid Deep Trees**
   - Max depth: 5 levels
   - Prefer wide, shallow trees

3. **Use Cooldowns**
   - Prevent rapid repeated condition checks
   - Default to 100 ticks (5 seconds) for expensive checks

4. **Profile and Optimize**
   ```java
   // Add timing to behavior tree tick
   public void tick() {
       long start = System.nanoTime();

       // ... tick logic ...

       long duration = System.nanoTime() - start;
       if (duration > 1_000_000) {  // > 1ms
           LOGGER.warn("Behavior tree tick took {} ms", duration / 1_000_000.0);
       }
   }
   ```

### Multiplayer Considerations

```java
/**
 * Thread-safe behavior tree updates for multiplayer.
 */
public class BehaviorTreeManager {

    private final Object treeLock = new Object();

    /**
     * Sets the active behavior tree (thread-safe).
     */
    public void setTree(BehaviorTree tree) {
        synchronized (treeLock) {
            if (this.activeTree != null) {
                this.activeTree.reset();
            }
            this.activeTree = tree;
        }
    }

    /**
     * Main update loop (no lock needed for read).
     */
    public void tick() {
        // Read reference without lock
        BehaviorTree current = activeTree;
        if (current != null && enabled) {
            // ... tick logic ...
        }
    }
}
```

---

## Testing Strategy

### Unit Tests

```java
package com.minewright.behavior;

import com.minewright.behavior.nodes.*;
import com.minewright.behavior.leaf.*;
import com.minewright.behavior.blackboard.Blackboard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for behavior tree nodes.
 */
class BehaviorTreeTest {

    @Test
    void testSequenceNode_AllSuccess() {
        // Create mock children that all succeed
        Node child1 = mockSuccessNode();
        Node child2 = mockSuccessNode();
        Node child3 = mockSuccessNode();

        SequenceNode sequence = new SequenceNode(List.of(child1, child2, child3));
        Blackboard blackboard = mock(Blackboard.class);

        // First tick
        Status result = sequence.tick(blackboard);
        assertEquals(Status.RUNNING, result);

        // Complete all children
        for (int i = 0; i < 3; i++) {
            result = sequence.tick(blackboard);
        }
        assertEquals(Status.SUCCESS, result);
    }

    @Test
    void testSequenceNode_ChildFails() {
        Node child1 = mockSuccessNode();
        Node child2 = mockFailureNode();
        Node child3 = mockSuccessNode();

        SequenceNode sequence = new SequenceNode(List.of(child1, child2, child3));
        Blackboard blackboard = mock(Blackboard.class);

        // First child succeeds
        Status result = sequence.tick(blackboard);
        assertEquals(Status.RUNNING, result);

        // Second child fails
        result = sequence.tick(blackboard);
        assertEquals(Status.FAILURE, result);

        // Third child should not execute
        verify(child3, never()).tick(any());
    }

    @Test
    void testSelectorNode_FirstSuccess() {
        Node child1 = mockSuccessNode();
        Node child2 = mockFailureNode();
        Node child3 = mockFailureNode();

        SelectorNode selector = new SelectorNode(List.of(child1, child2, child3));
        Blackboard blackboard = mock(Blackboard.class);

        Status result = selector.tick(blackboard);
        assertEquals(Status.SUCCESS, result);

        // Other children should not execute
        verify(child2, never()).tick(any());
        verify(child3, never()).tick(any());
    }

    @Test
    void testSelectorNode_AllFail() {
        Node child1 = mockFailureNode();
        Node child2 = mockFailureNode();
        Node child3 = mockFailureNode();

        SelectorNode selector = new SelectorNode(List.of(child1, child2, child3));
        Blackboard blackboard = mock(Blackboard.class);

        // All children fail
        for (int i = 0; i < 3; i++) {
            Status result = selector.tick(blackboard);
            assertEquals(Status.RUNNING, result);
        }

        // Final result is failure
        Status result = selector.tick(blackboard);
        assertEquals(Status.FAILURE, result);
    }

    @Test
    void testInverterNode() {
        Node successChild = mockSuccessNode();
        Node failureChild = mockFailureNode();

        InverterNode inverter1 = new InverterNode(successChild);
        InverterNode inverter2 = new InverterNode(failureChild);

        Blackboard blackboard = mock(Blackboard.class);

        assertEquals(Status.FAILURE, inverter1.tick(blackboard));
        assertEquals(Status.SUCCESS, inverter2.tick(blackboard));
    }

    @Test
    void testRepeaterNode() {
        Node child = mockSuccessNode();
        RepeaterNode repeater = new RepeaterNode(child, 3);

        Blackboard blackboard = mock(Blackboard.class);

        // Run 3 times
        for (int i = 0; i < 3; i++) {
            Status result = repeater.tick(blackboard);
            assertEquals(Status.RUNNING, result);
        }

        // 4th tick should succeed
        Status result = repeater.tick(blackboard);
        assertEquals(Status.SUCCESS, result);

        // Child should have been ticked 3 times
        verify(child, times(3)).tick(blackboard);
    }

    // Helper methods

    private Node mockSuccessNode() {
        Node node = mock(Node.class);
        when(node.tick(any())).thenReturn(Status.SUCCESS);
        return node;
    }

    private Node mockFailureNode() {
        Node node = mock(Node.class);
        when(node.tick(any())).thenReturn(Status.FAILURE);
        return node;
    }
}
```

### Integration Tests

```java
package com.minewright.behavior.integration;

import com.minewright.behavior.BehaviorTreeManager;
import com.minewright.behavior.trees.MiningWorkerTree;
import com.minewright.entity.ForemanEntity;
import com.minewright.MineWrightMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Integration tests for behavior trees in Minecraft.
 */
public class BehaviorTreeIntegrationTest {

    @GameTest(template = "empty")
    public void testMiningWorkerTree(GameTestHelper helper) {
        // Spawn a foreman
        ForemanEntity foreman = helper.spawnMob(
            MineWrightMod.FOREMAN_ENTITY.get(),
            1, 1, 1
        );

        // Set up behavior tree
        BehaviorTreeManager manager = foreman.getBehaviorTreeManager();
        manager.setTree(MiningWorkerTree.create());

        // Tick for 100 ticks (5 seconds)
        helper.runForTicks(100);

        // Behavior tree should have executed
        BehaviorTreeManager.Stats stats = manager.getStatistics();
        assertTrue(stats.totalTicks() > 0, "Behavior tree should have ticked");
    }

    @GameTest(template = "empty")
    public void testBehaviorTreeWithOre(GameTestHelper helper) {
        // Place some iron ore
        helper.setBlock(5, 1, 5, Blocks.IRON_ORE);

        // Spawn foreman
        ForemanEntity foreman = helper.spawnMob(
            MineWrightMod.FOREMAN_ENTITY.get(),
            1, 1, 1
        );

        // Set mining worker tree
        BehaviorTreeManager manager = foreman.getBehaviorTreeManager();
        manager.setTree(MiningWorkerTree.create());

        // Tick until ore is mined
        helper.runForTicks(200);

        // Ore should be mined
        assertTrue(helper.getBlock(5, 1, 5).isAir(), "Ore should be mined");
        helper.succeed();
    }
}
```

---

## Summary

This design document outlines a comprehensive Behavior Tree system for MineWright that:

1. **Integrates with existing systems** - Works alongside ActionExecutor and AgentStateMachine
2. **Provides reactive decision-making** - Agents respond quickly to world changes
3. **Supports LLM-generated trees** - Dynamic behavior tree creation from natural language
4. **Optimizes for performance** - Non-blocking execution, cached world state, cooldowns
5. **Includes comprehensive examples** - Mining, Building, and Combat worker trees
6. **Follows Minecraft best practices** - Tick-based execution, thread-safe design

### Next Steps

1. **Implement core nodes** - Sequence, Selector, Parallel, decorators
2. **Create condition nodes** - IsThreatened, HasItem, HealthBelow, etc.
3. **Create action nodes** - FindOre, PathToTarget, MineBlock, etc.
4. **Build example trees** - MiningWorker, BuildingWorker, CombatWorker
5. **Integrate with LLM** - Generate trees from natural language commands
6. **Profile and optimize** - Ensure minimal impact on server performance
7. **Add visualization** - Debug GUI for viewing tree state

---

## Sources

- [Java游戏AI算法设计与性能提升](https://docstore.docin.com/p-4865758863.html)
- [FSM、HFSM、BT的区别 - CSDN](https://m.blog.csdn.net/gao7009/article/details/80221163)
- [Behavior Tree-Based Intelligent Decision Systems for Games - CSDN](https://download.csdn.net/download/fq1986614/92252541)
- [Creating Intelligent AI Entities in Minecraft - CSDN](https://m.blog.csdn.net/a1033955099/article/details/144935466)
- [Unity行为树节点解析](https://blog.csdn.net/weixin_42216813/article/details/146218651)
- [行为树概念及Nav2应用](https://blog.csdn.net/fengnan50/article/details/149484160)
