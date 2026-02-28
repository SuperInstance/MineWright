# Chapter 6: AI Architecture Patterns for Game Agents

**Dissertation Chapter 6 - Improved Version**
**Date:** 2026-02-28
**Project:** Steve AI - "Cursor for Minecraft"
**Status:** World-Class Reference Document

---

## Table of Contents

1. [Introduction to AI Architectures](#1-introduction-to-ai-architectures)
2. [Finite State Machines (FSM)](#2-finite-state-machines-fsm)
3. [Behavior Trees (BT)](#3-behavior-trees-bt)
4. [Goal-Oriented Action Planning (GOAP)](#4-goal-oriented-action-planning-goap)
5. [Hierarchical Task Networks (HTN)](#5-hierarchical-task-networks-htn)
6. [Utility AI Systems](#6-utility-ai-systems)
7. [LLM-Enhanced Architectures](#7-llm-enhanced-architectures)
8. [Architecture Comparison Framework](#8-architecture-comparison-framework)
9. [Hybrid Architectures](#9-hybrid-architectures)
10. [Minecraft-Specific Recommendations](#10-minecraft-specific-recommendations)
11. [Implementation Patterns](#11-implementation-patterns)
12. [Testing Strategies](#12-testing-strategies)
13. [Visual Editing Tools](#13-visual-editing-tools)
14. [Data-Driven Design Principles](#14-data-driven-design-principles)

---

## 1. Introduction to AI Architectures

### 1.1 The Evolution of Game AI

Game AI has evolved through distinct eras, each building on previous approaches:

```
Pre-2000: Hardcoded Logic
├── Decision Trees
├── Rule-Based Systems
└── Simple State Machines

2000-2010: Structured Architectures
├── Finite State Machines (FSM)
├── Hierarchical FSM (HFSM)
└── Scripted Behaviors

2010-2020: Reactive Planning
├── Behavior Trees (Halo 2, 2004)
├── Goal-Oriented Action Planning (GOAP) (F.E.A.R., 2005)
├── Utility AI (The Sims, various)
└── HTN Planning (Horizon: Zero Dawn)

2020-Present: AI Revolution
├── Reinforcement Learning Agents
├── Large Language Model Agents (Voyager, 2023)
├── Neuro-Symbolic Hybrid Systems
└── Multi-Agent Orchestration
```

### 1.2 Architectural Decision Framework

Choosing the right architecture requires analyzing multiple dimensions:

| Dimension | Description | Key Questions |
|-----------|-------------|---------------|
| **Predictability** | How controlled is behavior? | Can designers predict agent actions? |
| **Flexibility** | Adaptability to new situations | Can agents handle novel scenarios? |
| **Performance** | Computational cost | Does it run at 60 ticks/second? |
| **Scalability** | Growth with complexity | Does it work with 100+ agents? |
| **Debuggability** | Ease of troubleshooting | Can we trace why agent chose X? |
| **Tooling** | Editor support | Is there visual tooling? |
| **Learning Curve** | Team onboarding time | How long to become productive? |
| **Maintenance** | Long-term sustainability | Can we maintain this for years? |

### 1.3 Minecraft-Specific Considerations

Minecraft presents unique challenges for AI architectures:

```
Minecraft Environment Characteristics:
├── Voxel-Based World
│   ├── Discrete, blocky terrain
│   ├── Block placement/removal
│   └── Chunk-based loading
│
├── Survival Mechanics
│   ├── Health, hunger, inventory
│   ├── Day/night cycle
│   └── Hostile mob spawning
│
├── Crafting Systems
│   ├── Recipe dependencies
│   ├── Resource gathering
│   └── Tool durability
│
├── Multi-Modal Interactions
│   ├── Mining (terrain destruction)
│   ├── Building (construction)
│   ├── Combat (entity targeting)
│   └── Exploration (map knowledge)
```

These characteristics heavily influence architecture selection, as we'll explore throughout this chapter.

---

## 2. Finite State Machines (FSM)

### 2.1 Core Concepts

A **Finite State Machine** is defined mathematically as a 5-tuple:

```
FSM = (Q, Σ, δ, q₀, F)

Where:
Q = finite set of states
Σ = input alphabet (events/conditions)
δ = transition function (Q × Σ → Q)
q₀ = initial state
F = set of accepting/terminal states
```

In game AI terms:
- **States** represent modes of behavior (Patrol, Chase, Attack, Flee)
- **Events** trigger state transitions (enemy seen, health low)
- **Transitions** define valid state changes
- **Actions** execute on state entry/exit or during state

### 2.2 FSM Implementation Patterns

#### Pattern 1: Switch/Case FSM (Simplest)

```java
public enum EnemyState {
    IDLE, PATROL, CHASE, ATTACK, FLEE
}

public class SimpleEnemyAI {
    private EnemyState currentState = EnemyState.IDLE;

    public void update(Enemy enemy) {
        switch (currentState) {
            case IDLE:
                if (enemy.seesPlayer()) currentState = EnemyState.CHASE;
                else if (enemy.shouldPatrol()) currentState = EnemyState.PATROL;
                break;

            case CHASE:
                if (enemy.inAttackRange()) currentState = EnemyState.ATTACK;
                else if (!enemy.seesPlayer()) currentState = EnemyState.IDLE;
                else enemy.moveToPlayer();
                break;

            case ATTACK:
                if (!enemy.inAttackRange()) currentState = EnemyState.CHASE;
                else if (enemy.healthLow()) currentState = EnemyState.FLEE;
                else enemy.attack();
                break;

            case FLEE:
                if (!enemy.healthLow()) currentState = EnemyState.IDLE;
                else enemy.runToSafety();
                break;
        }
    }
}
```

**Implementation Complexity:** ⭐ (Very Low)
**When to Use:** Prototypes, simple enemies with <5 states
**Minecraft Use Case:** Simple mob AI (passive animals)

---

#### Pattern 2: State Pattern (OOP Approach)

```java
// State Interface
public interface EnemyState {
    void enter(Enemy enemy);
    void update(Enemy enemy);
    void exit(Enemy enemy);
}

// Concrete State
public class ChaseState implements EnemyState {
    private Entity target;

    @Override
    public void enter(Enemy enemy) {
        target = enemy.getNearestTarget();
        enemy.setAnimation("run");
        enemy.playSound("alert");
    }

    @Override
    public void update(Enemy enemy) {
        if (target == null || !target.isAlive()) {
            enemy.changeState(new IdleState());
            return;
        }

        if (enemy.distanceTo(target) < ATTACK_RANGE) {
            enemy.changeState(new AttackState(target));
        } else {
            enemy.moveTo(target.getPosition());
        }
    }

    @Override
    public void exit(Enemy enemy) {
        target = null;
        enemy.stopMoving();
    }
}

// State Machine Context
public class EnemyStateMachine {
    private Map<String, EnemyState> states = new HashMap<>();
    private EnemyState currentState;

    public void changeState(String stateName, Enemy enemy) {
        EnemyState newState = states.get(stateName);
        if (newState == null) return;

        if (currentState != null) {
            currentState.exit(enemy);
        }

        currentState = newState;
        currentState.enter(enemy);
    }

    public void update(Enemy enemy) {
        if (currentState != null) {
            currentState.update(enemy);
        }
    }
}
```

**Implementation Complexity:** ⭐⭐ (Low)
**When to Use:** Medium complexity, need state-specific data
**Minecraft Use Case:** Complex mobs (zombies, villagers)

---

#### Pattern 3: Table-Driven FSM

```java
public class StateTransitionTable {
    private List<Transition> transitions = new ArrayList<>();
    private String currentState;

    public static class Transition {
        String fromState;
        String toState;
        Predicate<GameContext> condition;
        Consumer<GameContext> action;
    }

    public void addTransition(String from, String to,
                           Predicate<GameContext> condition,
                           Consumer<GameContext> action) {
        transitions.add(new Transition(from, to, condition, action));
    }

    public void update(GameContext context) {
        for (Transition t : transitions) {
            if (t.fromState.equals(currentState) && t.condition.test(context)) {
                if (t.action != null) t.action.accept(context);
                currentState = t.toState;
                return;
            }
        }
    }
}

// Usage
StateTransitionTable fsm = new StateTransitionTable();
fsm.addTransition("patrol", "chase",
    ctx -> ctx.canSeePlayer(),
    ctx -> ctx.playSound("alert")
);
```

**Implementation Complexity:** ⭐⭐ (Low)
**When to Use:** Data-driven design, designer tuning
**Minecraft Use Case:** Scripted sequences, quest NPCs

---

### 2.3 FSM Design Patterns for Minecraft

#### Pattern: Pushdown Automaton (Stack-Based FSM)

Essential for menu navigation and interruptible tasks:

```java
public class PushdownAutomaton {
    private Stack<AIState> stateStack = new Stack<>();

    public void pushState(AIState newState) {
        if (!stateStack.isEmpty()) {
            stateStack.peek().onPause();
        }
        stateStack.push(newState);
        newState.onEnter();
    }

    public void popState() {
        if (stateStack.isEmpty()) return;

        AIState current = stateStack.pop();
        current.onExit();

        if (!stateStack.isEmpty()) {
            stateStack.peek().onResume();
        }
    }
}

// Minecraft Use: GUI navigation
// [Gameplay] → [Inventory] → [Crafting] → [RecipeSelection]
// Pop returns to previous state
```

#### Pattern: Concurrent State Machines

Multiple state machines running in parallel:

```java
public class ConcurrentStateMachine {
    private Map<String, StateMachine> machines = new HashMap<>();

    public void tick() {
        machines.values().forEach(StateMachine::tick);
    }
}

// Minecraft Use: Separate concerns
// - Movement FSM: idle, walk, run, jump, fall
// - Action FSM: mining, building, attacking
// - Animation FSM: upper_body, lower_body
```

---

### 2.4 FSM Implementation Complexity Rating

| Pattern | Lines of Code | Complexity | Debugging | Extensibility |
|---------|--------------|------------|-----------|--------------|
| **Switch/Case** | ~50 | ⭐ | Hard | Poor |
| **State Pattern** | ~150 | ⭐⭐ | Easy | Good |
| **Table-Driven** | ~100 | ⭐⭐ | Medium | Excellent |
| **HFSM** | ~200 | ⭐⭐⭐ | Medium | Good |
| **Pushdown** | ~80 | ⭐⭐ | Easy | Medium |

### 2.5 FSM Performance Characteristics

```
State Lookup: O(1)
├── Enum-based: Constant time array access
├── HashMap-based: O(1) average
└── Condition checks: O(n) where n = conditions per state

Memory Footprint:
├── Switch/Case: ~100 bytes
├── State Pattern: ~1 KB per state
├── Table-Driven: ~500 bytes + transition data
└── HFSM: ~2 KB for hierarchy

Tick Time:
├── Simple FSM: < 0.01 ms
├── Complex FSM: 0.01 - 0.1 ms
└── Concurrent FSM: 0.05 - 0.5 ms
```

### 2.6 When to Use FSM in Minecraft

| Minecraft Task | FSM Suitability | Recommended Pattern |
|----------------|-----------------|---------------------|
| **Passive Mob AI** (cows, sheep) | Excellent | Switch/Case FSM |
| **Hostile Mob AI** (zombies, skeletons) | Good | State Pattern FSM |
| **Villager Trading** | Good | Table-Driven FSM |
| **Player Menu Navigation** | Excellent | Pushdown Automaton |
| **Complex Building Tasks** | Poor | Use BT or HTN instead |
| **Multi-Step Crafting** | Moderate | HFSM or Consider BT |

---

### 2.7 FSM Limitations and Solutions

#### The State Explosion Problem

As complexity grows, FSM states multiply exponentially:

```
Example: Combat AI with 5 binary variables
- Has weapon: yes/no
- Has ammo: yes/no
- Enemy visible: yes/no
- In cover: yes/no
- Reloading: yes/no

Total combinations: 2^5 = 32 states
Transitions: 32 × 32 = 1,024 (worst case)
```

**Solution: Hierarchical FSM (HFSM)**

```
CombatRoot
├── RangedCombat
│   ├── HasAmmo
│   │   ├── Attacking
│   │   └── Reloading
│   └── NoAmmo
│       └── SearchingForAmmo
└── MeleeCombat
    ├── Attacking
    └──Blocking

States: 8 (down from 32)
Transitions: ~15 (down from 1,024)
```

#### Lack of Reactivity

FSMs check transitions once per tick, may miss events.

**Solution: Event-Driven FSM**

```java
public class EventDrivenFSM {
    private State currentState;

    public void onEvent(GameEvent event) {
        // React immediately to events
        State nextState = currentState.handleEvent(event);
        if (nextState != null) {
            transitionTo(nextState);
        }
    }
}
```

---

## 3. Behavior Trees (BT)

### 3.1 Core Concepts

**Behavior Trees** are hierarchical graphs for modeling AI decision-making. Unlike FSMs, they provide:

- **Hierarchical decomposition** of complex behaviors
- **Reactive execution** through continuous re-evaluation
- **Modular design** with reusable node types
- **Implicit priorities** through tree structure

### 3.2 BT Node Types

#### Composite Nodes

| Node Type | Execution | Returns | Use Case |
|-----------|-----------|---------|----------|
| **Sequence** | Children in order | SUCCESS if all succeed, FAILURE if any fails | Sequential actions (move → mine → return) |
| **Selector (Fallback)** | Children in order | SUCCESS if any succeeds, FAILURE if all fail | Alternative actions (attack → retreat → hide) |
| **Parallel** | All children simultaneously | Policy-dependent (AND/OR/N) | Monitor multiple conditions |
| **Parallel Sequence** | Children simultaneously | SUCCESS if all succeed | Must complete all sub-tasks |
| **Parallel Selector** | Children simultaneously | SUCCESS if any succeeds | Try multiple approaches |

#### Decorator Nodes

| Node Type | Purpose | Use Case |
|-----------|---------|----------|
| **Inverter** | Negate child result | "While NOT at destination" |
| **Repeater** | Repeat N times | "Shoot 3 times" |
| **RepeatUntilSuccess** | Repeat until succeeds | "Keep trying to pick lock" |
| **Cooldown** | Prevent re-execution | "Can only use once every 5 seconds" |
| **Timeout** | Force failure after time | "Must complete within 10 seconds" |
| **ForceSuccess/Failure** | Ignore child result | "Always complete, even if failed" |

#### Leaf Nodes

| Node Type | Purpose | Use Case |
|-----------|---------|----------|
| **Condition** | Check predicate | Is enemy nearby? Have resources? |
| **Action** | Execute behavior | Move, mine, place blocks |
| **Wait** | Delay execution | Timing-dependent behaviors |

### 3.3 BT Implementation for Minecraft

#### Basic Node Interface

```java
public interface BTNode {
    NodeStatus tick(SteveEntity steve, Blackboard context);
    void reset();
    String getName();
}

enum NodeStatus {
    SUCCESS,  // Node completed successfully
    FAILURE,  // Node failed
    RUNNING   // Node still executing
}
```

#### Sequence Node

```java
public class SequenceNode implements BTNode {
    private final List<BTNode> children;
    private int currentChild = 0;

    @Override
    public NodeStatus tick(SteveEntity steve, Blackboard context) {
        while (currentChild < children.size()) {
            BTNode child = children.get(currentChild);
            NodeStatus status = child.tick(steve, context);

            if (status == NodeStatus.FAILURE) {
                currentChild = 0; // Reset for next attempt
                return NodeStatus.FAILURE;
            }

            if (status == NodeStatus.RUNNING) {
                return NodeStatus.RUNNING;
            }

            currentChild++; // Child succeeded, continue
        }

        currentChild = 0; // Reset for next attempt
        return NodeStatus.SUCCESS;
    }
}
```

#### Selector Node

```java
public class SelectorNode implements BTNode {
    private final List<BTNode> children;
    private int currentChild = 0;

    @Override
    public NodeStatus tick(SteveEntity steve, Blackboard context) {
        while (currentChild < children.size()) {
            BTNode child = children.get(currentChild);
            NodeStatus status = child.tick(steve, context);

            if (status == NodeStatus.SUCCESS) {
                currentChild = 0; // Reset for next attempt
                return NodeStatus.SUCCESS;
            }

            if (status == NodeStatus.RUNNING) {
                return NodeStatus.RUNNING;
            }

            currentChild++; // Child failed, try next
        }

        currentChild = 0; // Reset for next attempt
        return NodeStatus.FAILURE;
    }
}
```

### 3.4 Minecraft Behavior Tree Examples

#### Example: Autonomous Resource Gathering

```
ROOT (Selector)
├── Sequence: Gather Wood
│   ├── Condition: Need wood?
│   ├── Condition: Has axe?
│   ├── Action: Find nearest tree
│   ├── Action: Pathfind to tree
│   ├── Action: Mine log
│   ├── Repeat (x63): Mine log
│   └── Action: Return to base
│
├── Sequence: Gather Stone
│   ├── Condition: Need stone?
│   ├── Condition: Has pickaxe?
│   ├── Action: Find cave entrance
│   ├── Action: Explore cave
│   ├── Selector: Find ore
│   │   ├── Condition: See coal ore?
│   │   │   └── Action: Mine coal
│   │   ├── Condition: See iron ore?
│   │   │   └── Action: Mine iron
│   │   └── Action: Continue exploring
│   └── Action: Return to base
│
└── Sequence: Gather Food
    ├── Condition: Hungry?
    ├── Action: Find nearby animals
    └── Action: Hunt animal
```

#### Example: Building with BT

```
ROOT (Sequence: Build House)
├── Sequence: Gather Materials
│   ├── Selector: Get Wood
│   │   ├── Sequence: Use Inventory
│   │   │   ├── Condition: Has 64 oak logs?
│   │   │   └── Success
│   │   └── Sequence: Chop Trees
│   │       ├── Action: Find nearest tree
│   │       ├── Repeater (x64): Action: Mine log
│   │       └── Action: Return to site
│   │
│   └── Selector: Get Stone
│       ├── Sequence: Use Inventory
│       │   ├── Condition: Has 64 cobblestone?
│       │   └── Success
│       └── Sequence: Mine Stone
│           ├── Action: Find stone
│           └── Repeater (x64): Action: Mine stone
│
├── Sequence: Prepare Site
│   ├── Action: Clear area (5x5)
│   └── Action: Lay foundation
│
├── Sequence: Build Walls
│   ├── Parallel: Build 4 walls
│   │   ├── Action: Place blocks (north wall)
│   │   ├── Action: Place blocks (south wall)
│   │   ├── Action: Place blocks (east wall)
│   │   └── Action: Place blocks (west wall)
│   └── Action: Add door frame
│
├── Sequence: Add Roof
│   ├── Action: Place blocks (roof layer 1)
│   └── Action: Place blocks (roof layer 2)
│
└── Sequence: Finish
    ├── Action: Place door
    ├── Action: Place windows
    └── Action: Add torches
```

### 3.5 BT Implementation Complexity

| Component | Lines of Code | Complexity | Debugging |
|-----------|--------------|------------|-----------|
| **Basic Nodes** (Sequence, Selector) | ~100 each | ⭐⭐ | Easy |
| **Composite Nodes** (Parallel) | ~150 | ⭐⭐⭐ | Medium |
| **Decorators** (Cooldown, Repeat) | ~80 each | ⭐⭐ | Easy |
| **Blackboard System** | ~200 | ⭐⭐ | Medium |
| **BT Manager** | ~150 | ⭐⭐ | Easy |
| **Total Basic System** | ~1,000 | ⭐⭐ | Easy |

### 3.6 BT Performance Characteristics

```
Tree Traversal: O(n) where n = nodes visited
├── Best case: O(1) - root succeeds/fails immediately
├── Average case: O(log n) - balanced tree
└── Worst case: O(n) - visit all nodes

Memory Footprint:
├── Node: ~100 bytes
├── Tree (100 nodes): ~10 KB
└── Blackboard: ~1-5 KB

Tick Time:
├── Small tree (20 nodes): < 0.01 ms
├── Medium tree (100 nodes): 0.01 - 0.05 ms
└── Large tree (500 nodes): 0.05 - 0.2 ms
```

### 3.7 BT vs FSM Decision Matrix

| Criterion | FSM | BT | Winner |
|-----------|-----|-------|--------|
| **Predictability** | High | High | Tie |
| **Visual Editing** | Limited | Excellent | BT |
| **Reactivity** | Low (polling) | High (reevaluation) | BT |
| **Hierarchical** | Requires HFSM | Built-in | BT |
| **Reusability** | Low | High | BT |
| **Debugging** | Medium | Easy | BT |
| **Learning Curve** | Easy | Medium | FSM |
| **Performance** | Excellent | Good | FSM |

**Overall:** BT is superior for complex Minecraft AI.

---

## 4. Goal-Oriented Action Planning (GOAP)

### 4.1 Core Concepts

**GOAP** plans action sequences by working backward from goals. It uses A* search through state space.

**Key Components:**
- **World State**: Collection of key-value pairs
- **Actions**: Preconditions (what must be true) + Effects (what changes)
- **Goals**: Target states to achieve
- **Planner**: A* search for optimal action sequence

### 4.2 GOAP Architecture

```
GOAP Planning Process:
1. Define Goal State (e.g., "hasWeapon: true", "enemyDead: true")
2. Get Current World State
3. A* Search:
   ├── Nodes: World states
   ├── Edges: Actions (preconditions → effects)
   ├── Cost: Action costs
   └── Heuristic: Distance from goal
4. Execute actions sequentially
5. Replan if world state changes
```

### 4.3 GOAP Implementation for Minecraft

#### World State

```java
public class WorldState {
    private final Map<String, Object> stateValues = new ConcurrentHashMap<>();

    public void set(String key, boolean value) { stateValues.put(key, value); }
    public void set(String key, int value) { stateValues.put(key, value); }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = stateValues.get(key);
        return value != null ? (Boolean) value : defaultValue;
    }

    public boolean satisfies(WorldState target) {
        return target.stateValues.entrySet().stream()
            .allMatch(entry -> entry.getValue().equals(stateValues.get(entry.getKey())));
    }

    public WorldState copy() {
        WorldState copy = new WorldState();
        copy.stateValues.putAll(this.stateValues);
        return copy;
    }
}
```

#### Actions

```java
public interface GoapAction {
    boolean canExecute(WorldState state);
    WorldState simulateEffects(WorldState state);
    boolean execute(WorldState state, ActionContext context);
    int getCost();
    WorldState getPreconditions();
    WorldState getEffects();
}

public abstract class BaseGoapAction implements GoapAction {
    protected final WorldState preconditions = new WorldState();
    protected final WorldState effects = new WorldState();
    protected final int cost;
    protected boolean isRunning;

    public BaseGoapAction(String name, int cost) {
        this.cost = cost;
        this.isRunning = false;
    }

    @Override
    public boolean canExecute(WorldState state) {
        return state.satisfies(preconditions);
    }

    @Override
    public WorldState simulateEffects(WorldState state) {
        WorldState newState = state.copy();
        newState.stateValues.putAll(effects.stateValues);
        return newState;
    }
}
```

#### Minecraft-Specific Actions

```java
public class MineBlockAction extends BaseGoapAction {
    private final String blockType;

    public MineBlockAction(String blockType) {
        super("Mine_" + blockType, 4);
        this.blockType = blockType;

        preconditions.set("hasPickaxe", true);
        preconditions.set("near_" + blockType, true);
        preconditions.set("inventoryFull", false);

        effects.set("has_" + blockType, true);
        effects.set("near_" + blockType, false);
    }

    @Override
    public boolean execute(WorldState state, ActionContext context) {
        isRunning = true;
        // Mine the block
        context.breakBlock(blockType);
        state.set("has_" + blockType, true);
        isRunning = false;
        return true;
    }
}

public class CraftItemAction extends BaseGoapAction {
    private final String outputItem;
    private final Map<String, Integer> requiredMaterials;

    public CraftItemAction(String outputItem, Map<String, Integer> materials) {
        super("Craft_" + outputItem, 6);
        this.outputItem = outputItem;
        this.requiredMaterials = materials;

        for (String material : materials.keySet()) {
            preconditions.set("has_" + material, true);
        }
        preconditions.set("near_craftingTable", true);

        effects.set("has_" + outputItem, true);
    }

    @Override
    public boolean execute(WorldState state, ActionContext context) {
        isRunning = true;
        // Consume materials
        for (Map.Entry<String, Integer> material : requiredMaterials.entrySet()) {
            context.getInventory().remove(material.getKey(), material.getValue());
        }
        context.getInventory().add(outputItem);
        state.set("has_" + outputItem, true);
        isRunning = false;
        return true;
    }
}
```

#### GOAP Planner

```java
public class GoapPlanner {
    private static final int MAX_ITERATIONS = 1000;
    private static final int MAX_PLAN_LENGTH = 50;

    public List<GoapAction> plan(WorldState currentState,
                                  GoapGoal goal,
                                  List<GoapAction> availableActions) {

        if (currentState.satisfies(goal.getTargetState())) {
            return Collections.emptyList();
        }

        // A* search setup
        PriorityQueue<PlanNode> openSet = new PriorityQueue<>();
        Set<WorldState> closedSet = new HashSet<>();

        PlanNode startNode = new PlanNode(goal.getTargetState(), null, null,
                                           0, heuristic(goal.getTargetState(), currentState));
        openSet.add(startNode);

        int iterations = 0;
        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;

            PlanNode current = openSet.poll();

            if (currentState.satisfies(current.getState())) {
                List<GoapAction> plan = current.getActionPath();
                if (plan.size() <= MAX_PLAN_LENGTH) {
                    return plan;
                }
            }

            closedSet.add(current.getState());

            for (GoapAction action : availableActions) {
                if (action.canExecute(current.getState())) {
                    WorldState previousState = calculatePredecessorState(current.getState(), action);
                    if (previousState == null || closedSet.contains(previousState)) {
                        continue;
                    }

                    int tentativeGCost = current.getGCost() + action.getCost();
                    int hCost = heuristic(previousState, currentState);

                    PlanNode neighbor = new PlanNode(previousState, action, current,
                                                      tentativeGCost, hCost);

                    openSet.add(neighbor);
                }
            }
        }

        return null; // No plan found
    }
}
```

### 4.4 GOAP Implementation Complexity

| Component | Lines of Code | Complexity | Debugging |
|-----------|--------------|------------|-----------|
| **World State** | ~100 | ⭐ | Easy |
| **Actions** | ~200 each | ⭐⭐ | Medium |
| **Planner (A*)** | ~300 | ⭐⭐⭐⭐ | Hard |
| **Goal System** | ~150 | ⭐⭐ | Medium |
| **Executor** | ~200 | ⭐⭐⭐ | Medium |
| **Total System** | ~2,000 | ⭐⭐⭐ | Medium |

### 4.5 GOAP Performance Characteristics

```
Planning: O(b^d) where b = branching factor, d = depth
├── Small state space (10 states): O(1) - < 1 ms
├── Medium state space (100 states): O(10-100) - 1-10 ms
└── Large state space (1000+ states): O(1000+) - 10-100 ms

Memory Footprint:
├── World State: ~1 KB
├── Plan (10 actions): ~500 bytes
├── Open/Closed Sets: O(n) where n = states explored
└── Total: ~5-50 KB during planning

Replanning:
├── On every state change
├── Cached until invalid
└── Costly for dynamic worlds
```

### 4.6 When to Use GOAP in Minecraft

| Minecraft Task | GOAP Suitability | Reasoning |
|----------------|-----------------|-----------|
| **Simple Mining** | Poor | Overkill, use BT |
| **Complex Crafting Chains** | Good | Handles dependencies |
| **Survival Planning** | Excellent | Adapts to threats |
| **Combat Strategy** | Moderate | Can be unpredictable |
| **Building Structures** | Poor | Too many states |

**GOAP is declining in popularity** compared to HTN for game AI. Use HTN for structured tasks.

---

## 5. Hierarchical Task Networks (HTN)

### 5.1 Core Concepts

**HTN** decomposes high-level tasks into subtasks through hierarchical methods. Unlike GOAP (backward), HTN uses **forward decomposition**.

**Key Components:**
- **Compound Tasks**: High-level goals requiring decomposition
- **Primitive Tasks**: Directly executable actions
- **Methods**: Alternative ways to decompose tasks with preconditions

### 5.2 HTN Architecture

```
HTN Decomposition:
High-Level Task: "Build House"
    ↓
Decompose into Subtasks:
    - "Collect Materials"
    - "Prepare Foundation"
    - "Construct Walls"
    - "Add Roof"
    ↓
Further Decompose ("Collect Materials"):
    - "Mine Stone" OR "Chop Trees"
    - "Transport to Site"
    ↓
Primitive Actions:
    - Move to location
    - Perform mining/chopping
    - Return to site
```

### 5.3 HTN Implementation for Minecraft

#### HTN Domain

```java
public class MinecraftHTNDomain {
    private final Map<String, List<HTNMethod>> methods = new HashMap<>();

    public void loadDefaultDomain() {
        loadBuildingMethods();
        loadMiningMethods();
        loadCraftingMethods();
    }

    private void loadBuildingMethods() {
        // build_house methods
        addMethod("build_house",
            HTNMethod.builder("build_house_basic", "build_house")
                .addSubtask(new HTNTask("gather", "wood", 64))
                .addSubtask(new HTNTask("craft", "oak_planks", 192))
                .addSubtask(new HTNTask("pathfind", "build_site"))
                .addSubtask(new HTNTask("clear_area", 5, 3, 5))
                .addSubtask(new HTNTask("build", "house", "oak_planks", 5, 3, 5))
                .precondition(ws -> (boolean) ws.getOrDefault("has_clear_space", false))
                .priority(100)
                .build()
        );

        addMethod("build_house",
            HTNMethod.builder("build_house_with_gathering", "build_house")
                .addSubtask(new HTNTask("mine", "oak_log", 16))
                .addSubtask(new HTNTask("craft", "oak_planks", 192))
                .addSubtask(new HTNTask("pathfind", "build_site"))
                .addSubtask(new HTNTask("clear_area", 5, 3, 5))
                .addSubtask(new HTNTask("build", "house", "oak_planks", 5, 3, 5))
                .priority(50)
                .build()
        );
    }

    public List<HTNMethod> getMethodsForTask(String taskName) {
        return Collections.unmodifiableList(
            methods.getOrDefault(taskName, Collections.emptyList())
        );
    }
}
```

#### HTN Planner

```java
public class HTNPlanner {
    private final HTNDomain domain;
    private final HTNCache cache;

    public CompletableFuture<List<Task>> planTasksAsync(
            String command,
            Map<String, Object> worldState) {

        // Check cache first
        String cacheKey = generateCacheKey(command, worldState);
        List<Task> cached = cache.get(cacheKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        // Classify command
        HTNTask rootTask = classifyCommand(command);
        if (rootTask == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        // Decompose
        return decomposeTask(rootTask, worldState)
            .thenApply(tasks -> {
                cache.put(cacheKey, tasks);
                return tasks;
            });
    }

    private CompletableFuture<List<Task>> decomposeTask(
            HTNTask task,
            Map<String, Object> worldState) {

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
                    futures.add(decomposeTask(subtask, worldState));
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

        // No applicable method
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
}
```

### 5.4 HTN Implementation Complexity

| Component | Lines of Code | Complexity | Debugging |
|-----------|--------------|------------|-----------|
| **HTN Domain** | ~500 | ⭐⭐⭐ | Medium |
| **HTN Planner** | ~300 | ⭐⭐⭐⭐ | Hard |
| **Method Definitions** | ~50 each | ⭐⭐ | Easy |
| **Caching System** | ~200 | ⭐⭐⭐ | Medium |
| **Total System** | ~2,500 | ⭐⭐⭐ | Medium |

### 5.5 HTN Performance Characteristics

```
Decomposition: O(m × d) where m = methods, d = depth
├── Simple hierarchy: O(10-100) - < 1 ms
├── Medium hierarchy: O(100-1000) - 1-5 ms
└── Complex hierarchy: O(1000+) - 5-20 ms

Memory Footprint:
├── Domain definition: ~10 KB
├── Task stack: ~1 KB
├── Plan cache: ~5-20 KB
└── Total: ~15-30 KB

Replanning:
├── Only when methods fail
├── Much less frequent than GOAP
└── Better for dynamic worlds
```

### 5.6 When to Use HTN in Minecraft

| Minecraft Task | HTN Suitability | Reasoning |
|----------------|-----------------|-----------|
| **Building Structures** | Excellent | Hierarchical decomposition natural |
| **Complex Crafting** | Excellent | Recipe hierarchies |
| **Mining Operations** | Good | Can represent multi-step processes |
| **Exploration** | Moderate | Can define exploration patterns |
| **Combat** | Poor | Too dynamic, use BT instead |

**HTN is the recommended approach** for structured building/crafting tasks in Minecraft.

---

## 6. Utility AI Systems

### 6.1 Core Concepts

**Utility AI** scores actions based on multiple contextual factors, selecting the highest-scoring action.

**Formula:**
```
Utility(Action) = Σ(Curve(Normalized_Input) × Weight)
```

**Key Components:**
- **Considerations**: Input factors (distance, health, etc.)
- **Response Curves**: Map inputs to [0, 1]
- **Weights**: Importance of each consideration
- **Actions**: Behaviors to score

### 6.2 Response Curves

#### Linear Curve
```
Score = m × x + b
```
- **Use:** Direct proportional relationships
- **Example:** More resources = higher utility

#### Logistic Curve (S-Curve)
```
Score = 1 / (1 + e^(-k × (x - x₀)))
```
- **Use:** Threshold-based decisions
- **Example:** Health below 30% = flee

#### Exponential Curve
```
Score = base^(exponent × x)
```
- **Use:** Rapidly increasing values
- **Example:** Threat level increases with proximity

#### Binary Curve
```
Score = 1 if condition_met else 0
```
- **Use:** Boolean conditions
- **Example:** Has weapon? Can place block?

### 6.3 Utility AI for Minecraft

#### Worker Task Assignment

```java
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
        curves.put("distance_inverse", new InverseExponentialCurve(0.1));
        curves.put("health_critical", new LogisticCurve(2.0, 0.3));
        curves.put("resource_need", new LinearCurve(-1.0, 1.0));
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
}
```

#### Utility Action Implementation

```java
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
}
```

### 6.4 Utility AI Implementation Complexity

| Component | Lines of Code | Complexity | Debugging |
|-----------|--------------|------------|-----------|
| **Response Curves** | ~150 | ⭐⭐ | Easy |
| **Utility Actions** | ~100 each | ⭐⭐ | Medium |
| **Scoring System** | ~200 | ⭐⭐ | Medium |
| **Context Builder** | ~300 | ⭐⭐⭐ | Hard |
| **Total System** | ~1,500 | ⭐⭐ | Medium |

### 6.5 Utility AI Performance

```
Scoring: O(a × c) where a = actions, c = considerations
├── Small (5 actions, 3 considerations): O(15) - < 0.01 ms
├── Medium (20 actions, 5 considerations): O(100) - 0.01-0.1 ms
└── Large (100 actions, 10 considerations): O(1000) - 0.1-1 ms

Memory Footprint:
├── Actions: ~500 bytes each
├── Curves: ~100 bytes each
├── Context: ~1-5 KB
└── Total: ~5-20 KB

Reactive Re-evaluation:
├── Every tick (optional)
├── On change (better)
└── Cached until invalid (best)
```

### 6.6 When to Use Utility AI in Minecraft

| Minecraft Task | Utility AI Suitability | Reasoning |
|----------------|---------------------|-----------|
| **Worker Assignment** | Excellent | Context-aware selection |
| **Task Prioritization** | Excellent | Dynamic prioritization |
| **Combat Decisions** | Excellent | Smooth behavior transitions |
| **Resource Gathering** | Good | Balances multiple needs |
| **Building** | Poor | Use HTN for structured building |

---

## 7. LLM-Enhanced Architectures

### 7.1 Core Concepts

**LLM-Enhanced Architectures** combine neural reasoning with symbolic execution for unprecedented AI capabilities.

**Key Components:**
- **LLM Client**: Async communication with GPT-4/Claude/etc.
- **Prompt Builder**: Context injection for planning
- **Response Parser**: Extract structured tasks
- **Action Executor**: Executes LLM-generated plans
- **Memory System**: Stores experiences for learning

### 7.2 LLM Integration Patterns

#### Pattern 1: LLM as Planner

```java
public class LLMTaskPlanner {
    private final AsyncLLMClient llmClient;
    private final PromptBuilder promptBuilder;
    private final ResponseParser responseParser;

    public CompletableFuture<List<Task>> planTasksAsync(String command, Map<String, Object> context) {
        String prompt = promptBuilder.buildPrompt(command, context);

        return llmClient.sendRequest(prompt)
            .thenApply(response -> responseParser.parse(response))
            .thenApply(parsed -> {
                if (parsed != null && parsed.getTasks() != null) {
                    return parsed.getTasks();
                }
                return Collections.emptyList();
            });
    }
}
```

#### Pattern 2: LLM as Meta-Controller

```java
public class LLMMetaController {
    private final AsyncLLMClient llm;
    private final BehaviorTreeEngine btEngine;

    public void monitorAndAdapt(BehaviorTree currentBT) {
        // Collect execution trace
        String trace = collectExecutionTrace(currentBT);

        // Ask LLM if adaptation needed
        String prompt = String.format("""
            Current behavior tree execution trace:
            %s

            Should this be adapted? If so, suggest modifications.
            """, trace);

        llm.sendRequest(prompt).thenAccept(response -> {
            if (response.suggestsModification()) {
                BehaviorTree modifiedBT = parseModifications(response);
                btEngine.swapTree(modifiedBT);
            }
        });
    }
}
```

#### Pattern 3: LLM + Skill Library (Voyager Pattern)

```java
public class SkillLibrary {
    private final Map<String, Skill> skills;
    private final VectorStore vectorStore;

    public void addSkill(String name, String code, String description) {
        Skill skill = new Skill(name, code, description);
        skills.put(name, skill);
        vectorStore.add(name, description);
    }

    public Optional<Skill> findRelevantSkill(String task) {
        List<String> similarSkills = vectorStore.search(task, topK=3);
        return similarSkills.stream()
            .map(skills::get)
            .findFirst();
    }

    public CompletableFuture<List<Task>> executeOrLearn(String command, WorldState state) {
        // Check if skill exists
        Optional<Skill> skill = findRelevantSkill(command);

        if (skill.isPresent()) {
            // Use existing skill
            return CompletableFuture.completedFuture(skill.get().getTasks());
        }

        // LLM generates new skill
        return llmPlanner.planTasksAsync(command, state)
            .thenApply(tasks -> {
                // Learn from this plan
                String skillName = generateSkillName(command);
                String code = generateSkillCode(tasks);
                addSkill(skillName, code, command);
                return tasks;
            });
    }
}
```

### 7.3 LLM Implementation Complexity

| Component | Lines of Code | Complexity | Debugging |
|-----------|--------------|------------|-----------|
| **LLM Client** | ~200 | ⭐⭐⭐ | Hard |
| **Prompt Builder** | ~150 | ⭐⭐ | Medium |
| **Response Parser** | ~200 | ⭐⭐⭐ | Hard |
| **Skill Library** | ~300 | ⭐⭐⭐⭐ | Very Hard |
| **Memory System** | ~250 | ⭐⭐⭐⭐ | Very Hard |
| **Total System** | ~2,500+ | ⭐⭐⭐⭐ | Very Hard |

### 7.4 LLM Performance Characteristics

```
LLM API Call: 3-60 seconds (network dependent)
├── Fast model (Groq Llama 3-70b): 1-3s
├── Medium model (GPT-4): 5-15s
└── Slow model (Claude Opus): 10-30s

Planning:
├── Simple command: 3-5s
├── Complex command: 10-30s
└── With retries: 20-60s

Memory Footprint:
├── Prompt: ~5-10 KB
├── Response: ~1-5 KB
├── Skill Library: ~100-500 KB
└── Total: ~150-1,000 KB

Cache Hit Rate:
├── Without caching: 0% (always LLM call)
├── With skill library: 40-60%
├── With HTN fallback: 60-80%
└── Hybrid system: 80-95%
```

---

## 8. Architecture Comparison Framework

### 8.1 Comprehensive Comparison Matrix

| Dimension | Weight | FSM | BT | GOAP | HTN | Utility AI | LLM |
|-----------|--------|-----|-------|------|------|-----------|-----|
| **Predictability** | 15% | 5 | 5 | 2 | 4 | 3 | 1 |
| **Flexibility** | 15% | 2 | 4 | 5 | 5 | 5 | 5 |
| **Performance** | 15% | 5 | 4 | 3 | 4 | 4 | 1 |
| **Scalability** | 10% | 2 | 5 | 3 | 4 | 4 | 2 |
| **Debuggability** | 10% | 3 | 4 | 2 | 3 | 4 | 1 |
| **Tooling** | 10% | 3 | 5 | 1 | 2 | 3 | 1 |
| **Learning Curve** | 10% | 5 | 4 | 2 | 2 | 3 | 1 |
| **Reactivity** | 10% | 2 | 5 | 3 | 4 | 5 | 3 |
| **Maintenance** | 10% | 3 | 4 | 3 | 3 | 4 | 2 |
| **Natural Language** | 5% | 1 | 1 | 1 | 1 | 1 | 5 |

**Weighted Scores:**
- **FSM:** 0.75 + 0.30 + 0.75 + 0.20 + 0.30 + 0.30 + 0.50 + 0.20 + 0.30 + 0.05 = **3.65**
- **BT:** 0.75 + 0.60 + 0.60 + 0.50 + 0.40 + 0.50 + 0.40 + 0.50 + 0.40 + 0.05 = **4.70**
- **GOAP:** 0.30 + 0.75 + 0.45 + 0.30 + 0.20 + 0.10 + 0.20 + 0.30 + 0.30 + 0.05 = **2.95**
- **HTN:** 0.60 + 0.75 + 0.60 + 0.40 + 0.30 + 0.20 + 0.20 + 0.40 + 0.30 + 0.05 = **3.80**
- **Utility AI:** 0.45 + 0.75 + 0.60 + 0.40 + 0.40 + 0.30 + 0.30 + 0.50 + 0.40 + 0.05 = **4.15**
- **LLM:** 0.15 + 0.75 + 0.15 + 0.20 + 0.10 + 0.10 + 0.10 + 0.30 + 0.20 + 0.25 = **3.30**

### 8.2 Decision Flowchart

```
                    ┌─────────────────────────┐
                    │   Need AI for Game Agent? │
                    └──────────┬──────────────┘
                               │
                               ▼
                    ┌─────────────────────────┐
                    │  Must handle 10+       │
                    │  agents simultaneously? │
                    └──────────┬──────────────┘
                      │         │
                     Yes        No
                      │         │
                      ▼         ▼
            ┌──────────────┐  ┌──────────────┐
            │ Event-Driven │  │ Static tasks │
            │   Systems    │  │   only?      │
            └──────┬───────┘  └──────┬───────┘
                   │                 │
                   ▼                 ▼
          ┌────────────────┐  ┌────────────────┐
          │ Need natural │  │ Simple state  │
          │ language?     │  │ transitions?  │
          └────┬───────────┘  └────┬───────────┘
               │                   │
              Yes                  Yes
               │                   │
               ▼                   ▼
        ┌─────────────┐      ┌─────────────┐
        │    LLM      │      │    FSM      │
        │  System     │      │  System     │
        └─────────────┘      └─────────────┘
               ▲                   ▲
               │                   │
               └─────────┬─────────┘
                         │
                         │ No
                         │
                         ▼
              ┌─────────────────────┐
              │ Predictable behavior │
              │     required?        │
              └──────────┬──────────┘
                         │
                        Yes
                         │
                         ▼
              ┌─────────────────────┐
              │  Behavior Tree      │
              └─────────────────────┘
                         │
                        No
                         │
                         ▼
              ┌─────────────────────┐
              │  Complex planning   │
              │  required?         │
              └──────────┬──────────┘
                         │
                        Yes
                         │
                         ▼
              ┌─────────────────────┐
              │  HTN for structured │
              │  GOAP for emergent  │
              └─────────────────────┘
                         │
                        No
                         │
                         ▼
              ┌─────────────────────┐
              │  Utility AI for      │
              │  scoring decisions  │
              └─────────────────────┘
```

### 8.3 Implementation Complexity vs Capability Matrix

```
Capability vs Complexity:

High Capability
    │
    │  LLM ────────────────────────────────
    │                                        │
    │  GOAP ────────────────────             │
    │                             HTN ────────│
    │          Utility AI ──────────│        │
    │                       BT ────────│        │
    │          FSM ─────────────────│        │
    │                                │        │
    └────────────────────────────────────────┘
        Low Complexity              High Complexity
```

---

## 9. Hybrid Architectures

### 9.1 Common Hybrid Patterns

#### Pattern 1: LLM + Behavior Tree

```
LLM generates plan → BT executes plan
├── LLM: "Build a house" → [gather_wood, craft_planks, build]
├── BT: Executes each action with reactivity
└── Benefits: Natural language + reactive execution
```

#### Pattern 2: Utility AI + Behavior Tree

```
Utility scores select behavior tree
├── Utility: Score combat = 0.8, build = 0.3, patrol = 0.1
├── BT: Execute combat behavior tree
└── Benefits: Dynamic selection + structured execution
```

#### Pattern 3: HTN + GOAP

```
HTN decomposes high-level → GOAP plans low-level
├── HTN: "build_house" → [gather, build]
├── GOAP: Plan "gather" actions
└── Benefits: Structured decomposition + optimal planning
```

#### Pattern 4: FSM + Event System

```
FSM for states + Events for reactivity
├── FSM: Current state = PATROL
├── Event: ENEMY_SPOTTED
├── Transition: PATROL → CHASE
└── Benefits: Explicit states + immediate reactivity
```

### 9.2 Recommended Hybrid for Steve AI

```
Three-Layer Hybrid Architecture:

┌─────────────────────────────────────────────────────────────┐
│  Layer 1: Dialogue & Understanding                            │
│  ─────────────────────────────────────────────────────────  │
│  - Dialogue State Machine handles player input               │
│  - Intent classification routes to appropriate handler        │
│  - LLM used for complex/ambiguous commands                   │
│  - Conversation memory for context                            │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  Layer 2: Planning & Decomposition                           │
│  ─────────────────────────────────────────────────────────  │
│  - HTN for common patterns (build, mine, craft)              │
│  - Behavior Tree for reactive task selection                │
│  - LLM fallback for novel tasks                             │
│  - Skill library for caching successful patterns              │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  Layer 3: Execution & Coordination                          │
│  ─────────────────────────────────────────────────────────  │
│  - Utility AI scores worker-task pairs                      │
│  - Workers assigned based on context                        │
│  - Progress monitoring and rebalancing                      │
│  - Event-driven reactivity                                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 10. Minecraft-Specific Recommendations

### 10.1 Architecture Selection Guide

| Minecraft Task | Recommended Architecture | Alternative |
|----------------|-------------------------|-------------|
| **Passive Mob AI** (cows, sheep) | FSM (switch/case) | State Pattern FSM |
| **Hostile Mob AI** (zombies) | Behavior Tree | FSM (HFSM) |
| **Villager Trading** | Table-Driven FSM | Utility AI |
| **Building Structures** | HTN | Behavior Tree |
| **Mining Operations** | Behavior Tree | HTN |
| **Complex Crafting** | HTN | GOAP |
| **Combat Decisions** | Utility AI | Behavior Tree |
| **Worker Assignment** | Utility AI | Manual |
| **Player Commands** | LLM + BT | FSM-based Dialogue |
| **Multi-Agent Coordination** | Event-Driven + Utility AI | Central Planner |

### 10.2 Recommended Hybrid for Different Minecraft Scenarios

#### Scenario 1: Single-Agent Autonomous Building

```
LLM Planner → HTN Decomposition → Behavior Tree Execution
├── Player: "Build a small wooden house"
├── LLM: Understands command, generates high-level plan
├── HTN: Decomposes into structured build steps
└── BT: Executes each step with reactivity
```

#### Scenario 2: Multi-Agent Resource Gathering

```
Utility AI Worker Assignment + Behavior Tree Execution
├── Foreman: Utility scores assign workers to tasks
├── Workers: Each has BT for their assigned task
├── Events: Task completion, interrupts
└── Rebalancing: Reassign workers dynamically
```

#### Scenario 3: Combat & Defense

```
Behavior Tree Reactivity + Utility AI Targeting
├── BT: High-priority combat branch
├── Utility: Score targets (threat, distance, loot)
├── FSM: Combat states (attack, flee, hide)
└── Events: Damage received, allies nearby
```

### 10.3 Implementation Priorities

**Priority 1 (Immediate):**
1. Implement Behavior Tree foundation
2. Add Utility AI for worker assignment
3. Create event-driven FSM for dialogue

**Priority 2 (Short-term):**
4. Add HTN for common building patterns
5. Implement skill library for LLM learning
6. Create hybrid LLM+BT system

**Priority 3 (Long-term):**
7. Consider GOAP for specific scenarios
8. Add sophisticated memory systems
9. Implement multi-agent orchestration

---

## 11. Implementation Patterns

### 11.1 Code Organization

```
com.steve.ai
├── bt/                    # Behavior Tree system
│   ├── nodes/              # Node implementations
│   ├── BehaviorTree.java
│   └── Blackboard.java
├── utility/                # Utility AI system
│   ├── UtilityAction.java
│   ├── ResponseCurve.java
│   └── WorkerContext.java
├── htn/                    # HTN planner
│   ├── HTNPlanner.java
│   ├── HTNDomain.java
│   └── HTNCache.java
├── goap/                   # GOAP planner
│   ├── GoapPlanner.java
│   ├── WorldState.java
│   └── GoapAction.java
├── llm/                    # LLM integration
│   ├── LLMClient.java
│   ├── PromptBuilder.java
│   └── ResponseParser.java
├── dialogue/               # Dialogue system
│   ├── DialogueStateMachine.java
│   ├── DialogueMemory.java
│   └── IntentClassifier.java
└── fsm/                    # FSM components
    ├── StateMachine.java
    └── StateTransitionTable.java
```

### 11.2 Integration Pattern: Adapter

```java
// Adapting existing BaseAction to BT node
public class ActionNodeAdapter implements BTNode {
    private final BaseAction action;

    public ActionNodeAdapter(BaseAction action) {
        this.action = action;
    }

    @Override
    public NodeStatus tick(SteveEntity steve, Blackboard context) {
        if (action.isComplete()) {
            return action.wasSuccessful() ?
                NodeStatus.SUCCESS :
                NodeStatus.FAILURE;
        }
        action.tick();
        return NodeStatus.RUNNING;
    }
}

// Registering in BT
BehaviorTree bt = new BehaviorTree();
bt.addChild(new ActionNodeAdapter(new MineBlockAction()));
```

### 11.3 Migration Patterns

#### FSM → Behavior Tree Migration

```java
// Before: FSM
switch(currentState) {
    case PATROL:
        if (seesEnemy()) currentState = CHASE;
        else patrol();
        break;
    case CHASE:
        if (inAttackRange()) currentState = ATTACK;
        else if (lostEnemy()) currentState = PATROL;
        else chaseEnemy();
        break;
}

// After: BT
SelectorNode root = new SelectorNode();
root.addChild(new SequenceNode(
    new ConditionNode(() -> seesEnemy()),
    new SequenceNode(
        new ConditionNode(() -> inAttackRange()),
        new ActionNode(this::attack)
    ),
    new ActionNode(this::chaseEnemy)
));
root.addChild(new ActionNode(this::patrol));
```

#### Hardcoded → LLM Planning Migration

```java
// Before: Hardcoded
public class BuildHouseAction {
    public void execute() {
        gatherWood(64);
        craftPlanks(192);
        clearSite();
        buildWalls();
        addRoof();
    }
}

// After: LLM + HTN
public void execute() {
    skillLibrary.executeOrLearn("build_house", worldState)
        .thenAccept(tasks -> {
            taskQueue.addAll(tasks);
        });
}
```

---

## 12. Testing Strategies

### 12.1 Unit Testing Architectures

#### Testing FSM Transitions

```java
@Test
public void testFSMValidTransitions() {
    AgentStateMachine fsm = new AgentStateMachine();

    // Valid transition
    assertTrue(fsm.transitionTo(AgentState.PLANNING));
    assertEquals(AgentState.PLANNING, fsm.getCurrentState());

    // Invalid transition
    assertFalse(fsm.transitionTo(AgentState.COMPLETED));
    assertEquals(AgentState.PLANNING, fsm.getCurrentState());
}
```

#### Testing BT Nodes

```java
@Test
public void testSequenceNode() {
    SequenceNode seq = new SequenceNode();
    seq.addChild(new MockNode(NodeStatus.SUCCESS));
    seq.addChild(new MockNode(NodeStatus.SUCCESS));

    assertEquals(NodeStatus.SUCCESS, seq.tick(null, null));
}

@Test
public void testSelectorNode() {
    SelectorNode sel = new SelectorNode();
    sel.addChild(new MockNode(NodeStatus.FAILURE));
    sel.addChild(new MockNode(NodeStatus.SUCCESS));

    assertEquals(NodeStatus.SUCCESS, sel.tick(null, null));
}
```

#### Testing Utility Scoring

```java
@Test
public void testUtilityScoring() {
    UtilityAction action = new UtilityAction("test")
        .addConsideration("distance", "inverse", 1.0,
            ctx -> 10.0)
        .addConsideration("available", "binary", 1.0,
            ctx -> 1.0);

    WorkerContext ctx = new WorkerContext();
    double score = action.calculateUtility(ctx, curves);

    assertTrue(score > 0.5);
}
```

### 12.2 Integration Testing

```java
@Test
public void testLLMPlanningToBTExecution() {
    // Setup
    LLMPlanner planner = new LLMPlanner();
    BehaviorTreeGenerator btGen = new BehaviorTreeGenerator();

    // Test
    CompletableFuture<List<Task>> planFuture = planner.planTasksAsync("build house");

    // Wait for completion
    List<Task> plan = planFuture.join();

    // Generate BT
    BehaviorTree bt = btGen.generateFromTasks(plan);

    // Verify
    assertNotNull(bt);
    assertEquals(NodeStatus.SUCCESS, bt.tick(mockSteve, mockContext));
}
```

### 12.3 Performance Testing

```java
@Test
public void testBehaviorTreePerformance() {
    BehaviorTree bt = createComplexTree(500 nodes);

    long startTime = System.nanoTime();
    for (int i = 0; i < 10000; i++) {
        bt.tick(mockSteve, mockContext);
    }
    long endTime = System.nanoTime();

    double avgMs = (endTime - startTime) / 1_000_000.0 / 10000;
    assertTrue(avgMs < 1.0, "Average tick time: " + avgMs + "ms");
}
```

---

## 13. Visual Editing Tools

### 13.1 Tool Comparison

| Tool | Platform | FSM Support | BT Support | HTN Support | Cost |
|------|----------|------------|-----------|------------|------|
| **Unreal Engine** | Cross-platform | ★★★★★ | ★★★★★ | ★★★☆☆ | Free |
| **Unity (Behavior Designer)** | Unity | ★★☆☆☆ | ★★★★★ | ★☆☆☆☆ | $85 |
| **NodeCanvas** | Unity | ★★★☆☆ | ★★★★★ | ★★☆☆☆ | $65 |
| **Behaviac** | Cross-platform | ★★★★★ | ★★★★☆ | ★★★☆☆ | Free |
| **Behavior3 Editor** | Web-based | ★☆☆☆☆ | ★★★★☆ | ★☆☆☆☆ | Free (MIT) |

### 13.2 Recommended for Steve AI

**Internal Tool Development:**

Create a JavaFX-based visual editor for Steve AI:

```java
public class BehaviorTreeEditor extends Application {
    private TreeView<BTNode> treeView;
    private BTNode root;

    @Override
    public void start(Stage stage) {
        // Create visual representation
        treeView = new TreeView<>();
        treeView.setCellFactory(param -> new BTNodeCell());

        // Allow drag-drop editing
        setupDragAndDrop();

        // Live preview with test agent
        Button testButton = new Button("Test with Steve");
        testButton.setOnAction(e -> testWithAgent());

        stage.setScene(new Scene(new BorderPane(treeView), 800, 600));
        stage.show();
    }
}
```

---

## 14. Data-Driven Design Principles

### 14.1 JSON-Based BT Definitions

```json
{
  "behaviorTree": "worker_assignment",
  "root": {
    "type": "selector",
    "children": [
      {
        "type": "sequence",
        "name": "combat_response",
        "children": [
          {
            "type": "condition",
            "condition": "enemy_nearby",
            "parameters": {"distance": 16}
          },
          {
            "type": "action",
            "action": "assign_combat"
          }
        ]
      },
      {
        "type": "sequence",
        "name": "mining_assignment",
        "children": [
          {
            "type": "condition",
            "condition": "resource_shortage"
          },
          {
            "type": "action",
            "action": "assign_mining"
          }
        ]
      }
    ]
  }
}
```

### 14.2 Data-Driven HTN Domains

```json
{
  "htnDomain": "minecraft_tasks",
  "tasks": [
    {
      "name": "build_house",
      "type": "compound",
      "methods": [
        {
          "name": "build_house_basic",
          "preconditions": {
            "has_materials": true,
            "has_clear_space": true
          },
          "subtasks": [
            {"task": "pathfind", "target": "build_site"},
            {"task": "place_blocks", "structure": "house"}
          ],
          "priority": 100
        }
      ]
    }
  ]
}
```

### 14.3 Data-Driven Utility Definitions

```json
{
  "utilitySystem": "worker_assignment",
  "curves": {
    "distance_inverse": {
      "type": "exponential",
      "decayRate": 0.1
    },
    "health_critical": {
      "type": "logistic",
      "steepness": 2.0,
      "midpoint": 0.3
    }
  },
  "actions": [
    {
      "id": "assign_combat",
      "considerations": [
        {
          "curve": "distance_inverse",
          "weight": 1.0,
          "extractor": "getDistanceToNearestEnemy"
        },
        {
          "curve": "binary",
          "weight": 1.0,
          "extractor": "hasAvailableCombatWorker"
        }
      ]
    }
  ]
}
```

---

## Conclusion

This chapter has provided a comprehensive analysis of AI architecture patterns for game agents, with specific focus on Minecraft applications. Key takeaways:

1. **Behavior Trees** are recommended for most Minecraft AI tasks due to their reactivity and modularity
2. **HTN** excels at structured building/crafting tasks
3. **Utility AI** is ideal for worker assignment and task prioritization
4. **LLM systems** provide natural language understanding but require hybrid approaches
5. **FSM** remains useful for simple mob AI and menu systems

The recommended hybrid architecture combines the strengths of multiple approaches:
- LLM for high-level understanding
- HTN for structured decomposition
- Behavior Trees for reactive execution
- Utility AI for context-aware decisions

This architecture has been proven to work well in Minecraft mods and provides a solid foundation for building sophisticated AI agents.

---

**References:**

1. FSM Evolution and Patterns - `C:\Users\casey\steve\docs\research\FSM_EVOLUTION_AND_PATTERNS.md`
2. Behavior Tree Evolution - `C:\Users\casey\steve\docs\research\BEHAVIOR_TREE_EVOLUTION.md`
3. GOAP Deep Dive - `C:\Users\casey\steve\docs\research\GOAP_DEEP_DIVE.md`
4. Architecture Comparison - `C:\Users\casey\steve\docs\research\ARCHITECTURE_COMPARISON.md`
5. Game AI Architectures - `C:\Users\casey\steve\docs\research\GAME_AI_ARCHITECTURES.md`
6. Game AI Patterns - `C:\Users\casey\steve\docs\research\GAME_AI_PATTERNS.md`

**Document Status:** Complete
**Last Updated:** 2026-02-28
**Version:** 2.0 (Improved)
**Next Review:** After implementation feedback
