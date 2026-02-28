# Chapter 6: AI Architecture Patterns
## Game AI Automation Techniques That Don't Require LLMs

**Dissertation Chapter 6**
**Date:** February 28, 2026
**Series:** Game AI Automation Techniques
**Focus:** Non-LLM Architecture Patterns for Game Automation

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Architecture Evolution](#2-architecture-evolution)
3. [Pattern Catalog](#3-pattern-catalog)
4. [Rule-Based Systems](#4-rule-based-systems)
5. [Finite State Machines (FSM)](#5-finite-state-machines-fsm)
6. [Hierarchical FSM (HFSM)](#6-hierarchical-fsm-hfsm)
7. [Behavior Trees](#7-behavior-trees)
8. [Utility AI / Scoring Systems](#8-utility-ai--scoring-systems)
9. [GOAP (Goal-Oriented Action Planning)](#9-goap-goal-oriented-action-planning)
10. [HTN (Hierarchical Task Networks)](#10-htn-hierarchical-task-networks)
11. [Hybrid Approaches](#11-hybrid-approaches)
12. [Data-Driven AI](#12-data-driven-ai)
13. [Minecraft-Specific Recommendations](#13-minecraft-specific-recommendations)
14. [Implementation Decision Matrix](#14-implementation-decision-matrix)
15. [Conclusion](#15-conclusion)

---

## 1. Introduction

### 1.1 The Architecture Selection Problem

Game AI developers face a critical decision: which architecture pattern best serves their automation needs? This question has evolved from simple if-then rules in the 1980s to sophisticated planning systems in modern AAA titles. Yet despite the emergence of Large Language Models (LLMs), classical AI architectures remain the foundation of game automation due to their:

- **Deterministic behavior** - Same inputs produce same outputs
- **Performance** - Microsecond response times
- **Predictability** - Testable and debuggable
- **Resource efficiency** - No GPU or API requirements

### 1.2 Why Study Classical Patterns?

Before LLMs revolutionized AI, game developers pioneered architectures that achieved remarkable intelligence through carefully structured logic. Understanding these patterns is crucial because:

1. **They still work**: 90% of game AI uses classical architectures
2. **They're foundational**: LLMs often wrap these patterns
3. **They're predictable**: Critical for player experience
4. **They're efficient**: Run on minimal hardware

### 1.3 Chapter Overview

This chapter provides a comprehensive catalog of game AI architecture patterns, from the simplest rule-based systems to sophisticated hierarchical planners. Each pattern includes:

- When to use it
- Strengths and weaknesses
- Implementation complexity
- Example games that used it
- Code structure examples
- Performance characteristics

---

## 2. Architecture Evolution

### 2.1 Timeline of Game AI Architectures

```
1980s ────────┐
              │ Rule-Based Systems (if-then)
              │ Example: Pac-Man ghosts, Space Invaders
1990s ────────┤
              │ Finite State Machines
              │ Example: Doom enemies, RTS units
2000s ────────┤
              │ Behavior Trees + Utility AI
              │ Example: Halo, The Sims
2005 ─────────┤
              │ GOAP (F.E.A.R.)
              │ HTN (Total Warrior)
2010s ────────┤
              │ Hybrid architectures
              │ Example: Alien: Isolation (BT + Utility)
2020s ────────┤
              │ LLM-augmented systems
              │ Example: Inworld, Character.AI
2025+ ────────┘
              │ Classical + LLM hybrids
              │ Future: Predictable automation
```

### 2.2 Architecture Selection Factors

| Factor | Impact on Selection | Example |
|--------|-------------------|---------|
| **Complexity** | Simple tasks → Rules, Complex → BT/HTN | Door opening → Rules |
| **Determinism** | Critical systems → FSM, Creative → Utility | Platforming → FSM |
| **Reactivity** | Fast-paced → BT, Slow-paced → GOAP | FPS → BT |
| **Extensibility** | Evolving games → Utility/HTN | Live service → HTN |
| **Team Skills** | Junior-heavy → FSM, Senior → BT | Indie → FSM |
| **Performance** | Mobile → FSM, PC → GOAP | Mobile game → FSM |

### 2.3 Architecture Complexity Ladder

```
Complexity:    Low ────────────────────────────── High
               │                                    │
Implementation: └──────────────────────────────────┘
               Rule   FSM   HFSM   BT   Utility   GOAP   HTN
                           └─┐    └────┘
                    Hybrid Approaches
```

---

## 3. Pattern Catalog

### 3.1 Quick Reference Table

| Architecture | Best For | Complexity | Determinism | Performance |
|--------------|----------|-----------|-------------|-------------|
| **Rule-Based** | Simple, fixed behaviors | Very Low | High | Excellent |
| **FSM** | Clear state transitions | Low | High | Excellent |
| **HFSM** | Hierarchical behaviors | Medium | High | Very Good |
| **Behavior Tree** | Reactive, modular AI | Medium-High | Medium | Good |
| **Utility AI** | Dynamic, nuanced decisions | Medium | Low | Good |
| **GOAP** | Goal-oriented planning | High | Medium | Medium |
| **HTN** | Hierarchical task decomposition | High | High | Medium |

### 3.2 Decision Flowchart

```
                    ┌─────────────────────┐
                    │   Start: What does   │
                    │   your AI need to    │
                    │        do?           │
                    └──────────┬───────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
         ┌──────▼──────┐              ┌───────▼───────┐
         │ Simple,     │              │ Complex,      │
         │ fixed       │              │ adaptive      │
         │ behavior?   │              │ behavior?     │
         └──────┬──────┘              └───────┬───────┘
                │                             │
         ┌──────▼──────┐              ┌───────▼───────┐
         │ Rule-Based  │              │ Needs clear   │
         │ System      │              │ states?       │
         └─────────────┘              └───────┬───────┘
                                            │
                                     ┌──────▼──────┐
                                     │ YES         │ NO
                                     │             │
                              ┌──────▼──────┐ ┌──▼────────┐
                              │ FSM / HFSM  │ │ Utility   │
                              └─────────────┘ │ AI        │
                                             └────┬───────┘
                                                  │
                                           ┌──────▼──────┐
                                           │ Reactive?   │
                                           │             │
                                    ┌──────▼─────┐ ┌───▼────────┐
                                    │ YES        │ │ NO         │
                                    │            │ │             │
                             ┌──────▼─────┐ ┌──▼────────┐ ┌──▼──────┐
                             │ Behavior   │ │ GOAP      │ │ HTN     │
                             │ Tree       │ │           │ │         │
                             └────────────┘ └───────────┘ └─────────┘
```

---

## 4. Rule-Based Systems

### 4.1 Overview

**Rule-based systems** are the simplest AI architecture, consisting of explicit if-then conditionals that trigger behaviors. They're essentially hardcoded decision trees.

### 4.2 When to Use

- **Simple behaviors**: Door opening, basic pathfinding
- **Predictable outcomes**: Platforming, puzzle AI
- **Performance-critical**: Mobile games, massive hordes
- **Rapid prototyping**: Proof of concepts

### 4.3 Strengths and Weaknesses

| Strengths | Weaknesses |
|-----------|------------|
| Extremely simple to implement | Doesn't scale well |
| 100% predictable | Hard to maintain with many rules |
| Zero overhead | Brittle in edge cases |
| Easy to debug | No emergent behavior |
| Fast execution | Rules can conflict |

### 4.4 Implementation Complexity

**Very Low** - Basic programming knowledge required

```java
// Classic rule-based AI (1990s style)
public void updateRules(EnemyAI ai) {
    // Rule 1: Attack if in range
    if (ai.distanceToPlayer() < 10.0) {
        ai.attack();
        return;
    }

    // Rule 2: Chase if visible
    if (ai.canSeePlayer()) {
        ai.moveTo(ai.getPlayerPosition());
        return;
    }

    // Rule 3: Patrol
    ai.moveToNextPatrolPoint();
}
```

### 4.5 Example Games

| Game | Use Case | Year |
|------|----------|------|
| Pac-Man | Ghost movement patterns | 1980 |
| Space Invaders | Alien attack patterns | 1978 |
| Doom | Basic enemy behaviors | 1993 |
| Mario Platformers | Fixed enemy patterns | 1985-2024 |

### 4.6 Code Structure

```
RuleBasedAI
├── Rule class
│   ├── condition: Predicate<Context>
│   ├── action: Consumer<Context>
│   └── priority: int
├── RuleEngine class
│   ├── rules: List<Rule>
│   ├── evaluate(context): void
│   └── addRule(rule): void
└── Context class
    ├── game state
    ├── entity state
    └── world state
```

### 4.7 Advanced Rule Systems

**Priority-Based Rules**: Rules execute in priority order, first match wins.

```java
public class PriorityRuleEngine {
    private final List<Rule> rules = new ArrayList<>();

    public void addRule(Predicate<Context> condition,
                       Consumer<Context> action,
                       int priority) {
        rules.add(new Rule(condition, action, priority));
        rules.sort((a, b) -> Integer.compare(b.priority, a.priority));
    }

    public void evaluate(Context context) {
        for (Rule rule : rules) {
            if (rule.condition.test(context)) {
                rule.action.accept(context);
                return; // First match executes
            }
        }
    }
}
```

---

## 5. Finite State Machines (FSM)

### 5.1 Overview

**Finite State Machines** model AI behavior as a set of discrete states with explicit transitions between them. States represent modes of behavior (e.g., Idle, Attack, Flee), and transitions define when to switch.

### 5.2 When to Use

- **Clear behavioral modes**: Combat AI, platformer characters
- **Explicit transitions**: State changes are meaningful
- **Debugging focus**: Easy to visualize state flow
- **Simple behaviors**: Not too many states

### 5.3 Strengths and Weaknesses

| Strengths | Weaknesss |
|-----------|-----------|
| Simple to understand | Explosion of states with complexity |
| Easy to debug and visualize | Transitions become unmanageable |
| Predictable behavior | Hard to share logic between states |
| O(1) state lookup | Not very reusable |
| Great for tools | State explosion problem |

### 5.4 Implementation Complexity

**Low** - Basic state management, enum-based states

```java
// Classic FSM implementation
public enum EnemyState {
    IDLE,
    PATROL,
    CHASE,
    ATTACK,
    FLEE
}

public class EnemyFSM {
    private EnemyState currentState = EnemyState.IDLE;

    public void update() {
        switch (currentState) {
            case IDLE:
                updateIdle();
                break;
            case PATROL:
                updatePatrol();
                break;
            case CHASE:
                updateChase();
                break;
            case ATTACK:
                updateAttack();
                break;
            case FLEE:
                updateFlee();
                break;
        }
    }

    private void updateIdle() {
        if (canSeePlayer()) {
            transitionTo(EnemyState.CHASE);
        } else if (bored()) {
            transitionTo(EnemyState.PATROL);
        }
    }

    private void transitionTo(EnemyState newState) {
        onExit(currentState);
        currentState = newState;
        onEnter(newState);
    }
}
```

### 5.5 Example Games

| Game | Use Case | Year |
|------|----------|------|
| Half-Life | Marine squad AI | 1998 |
| Halo | Elite combat behaviors | 2001 |
| Zelda: Ocarina of Time | Enemy combat states | 1998 |
| Assassin's Creed | NPC behavior states | 2007 |

### 5.6 Code Structure

```
FSM Architecture
├── State enum
│   ├── IDLE
│   ├── PATROL
│   ├── CHASE
│   ├── ATTACK
│   └── FLEE
├── FSM class
│   ├── currentState: State
│   ├── update(): void
│   ├── transitionTo(state): void
│   ├── onEnter(state): void
│   └── onExit(state): void
└── State behaviors
    ├── updateIdle(): void
    ├── updatePatrol(): void
    ├── updateChase(): void
    └── updateAttack(): void
```

### 5.7 State Pattern Implementation

More flexible than switch-based FSMs:

```java
// State Pattern implementation
public interface EnemyState {
    void enter(Enemy enemy);
    void update(Enemy enemy);
    void exit(Enemy enemy);
}

public class IdleState implements EnemyState {
    @Override
    public void enter(Enemy enemy) {
        enemy.playAnimation("idle");
    }

    @Override
    public void update(Enemy enemy) {
        if (enemy.canSeePlayer()) {
            enemy.changeState(new ChaseState());
        }
    }

    @Override
    public void exit(Enemy enemy) {
        // Cleanup
    }
}

public class Enemy {
    private EnemyState currentState;

    public void changeState(EnemyState newState) {
        if (currentState != null) {
            currentState.exit(this);
        }
        currentState = newState;
        currentState.enter(this);
    }

    public void update() {
        currentState.update(this);
    }
}
```

### 5.8 Transition Tables

For complex FSMs, use transition tables:

```java
public class FSMTransitionTable {
    private final Map<State, Map<Trigger, State>> transitions = new HashMap<>();

    public void addTransition(State from, Trigger trigger, State to) {
        transitions.computeIfAbsent(from, k -> new HashMap<>())
                   .put(trigger, to);
    }

    public State getNextState(State current, Trigger trigger) {
        Map<Trigger, State> stateTransitions = transitions.get(current);
        if (stateTransitions != null) {
            return stateTransitions.get(trigger);
        }
        return null; // Invalid transition
    }
}

// Usage
FSMTransitionTable table = new FSMTransitionTable();
table.addTransition(EnemyState.IDLE, Trigger.SPOTTED_PLAYER, EnemyState.CHASE);
table.addTransition(EnemyState.CHASE, Trigger.LOST_PLAYER, EnemyState.PATROL);
table.addTransition(EnemyState.CHASE, Trigger.IN_RANGE, EnemyState.ATTACK);
```

---

## 6. Hierarchical FSM (HFSM)

### 6.1 Overview

**Hierarchical FSMs** extend FSMs by allowing states to contain sub-states. This creates a hierarchy where high-level states contain their own state machines, reducing state explosion.

### 6.2 When to Use

- **Complex state machines**: Would have too many flat states
- **Natural hierarchies**: Combat contains multiple sub-behaviors
- **Code reuse**: Sub-states shared across high-level states
- **Organizing complexity**: Many related states

### 6.3 Strengths and Weaknesses

| Strengths | Weaknesses |
|-----------|------------|
| Reduces state explosion | More complex than flat FSM |
| Natural decomposition | Harder to visualize |
| Code reuse across states | Tool support varies |
| Easier to maintain | Learning curve |
| Encapsulates sub-behaviors | Deep hierarchies get confusing |

### 6.4 Implementation Complexity

**Medium** - Requires understanding state nesting and delegation

```java
// HFSM implementation
public interface HFSMState {
    void onEnter();
    void onUpdate();
    void onExit();
    HFSMState[] getSubStates();
}

public class CombatState implements HFSMState {
    private HFSMState currentSubState;

    private final HFSMState[] subStates = {
        new MeleeCombatState(),
        new RangedCombatState(),
        new SpellCombatState()
    };

    @Override
    public void onEnter() {
        transitionTo(MeleeCombatState.class);
    }

    @Override
    public void onUpdate() {
        currentSubState.onUpdate();
    }

    @Override
    public void onExit() {
        currentSubState.onExit();
    }

    @Override
    public HFSMState[] getSubStates() {
        return subStates;
    }

    private void transitionTo(Class<? extends HFSMState> stateClass) {
        if (currentSubState != null) {
            currentSubState.onExit();
        }
        for (HFSMState state : subStates) {
            if (state.getClass().equals(stateClass)) {
                currentSubState = state;
                currentSubState.onEnter();
                break;
            }
        }
    }
}
```

### 6.5 Example Games

| Game | Use Case | Year |
|------|----------|------|
| The Sims | Character behavior states | 2000 |
| Grand Theft Auto | NPC activity hierarchies | 1997-2023 |
| Bioshock | Enemy combat states | 2007 |
| Skyrim | AI decision hierarchies | 2011 |

### 6.6 Code Structure

```
HFSM Hierarchy Example
│
├─ CombatState (High-level)
│  ├─ MeleeCombatState
│  │  ├─ Attacking
│  │  ├─ Blocking
│  │  └─ Dodging
│  ├─ RangedCombatState
│  │  ├─ Aiming
│  │  ├─ Reloading
│  │  └─ TakingCover
│  └─ SpellCombatState
│     ├─ Casting
│     ├─ Channeling
│     └─ Cooldown
│
├─ ExplorationState
│  ├─ Pathfinding
│  ├─ Investigating
│  └─ Searching
│
└─ IdleState
   ├─ Waiting
   ├─ Wandering
   └─ Observing
```

### 6.7 State History

HFSMs can maintain history for returning to previous sub-states:

```java
public class HistoryState implements HFSMState {
    private HFSMState lastSubState;

    @Override
    public void onEnter() {
        // Resume last sub-state instead of default
        if (lastSubState != null) {
            lastSubState.onEnter();
        }
    }

    @Override
    public void onExit() {
        lastSubState = currentSubState;
    }
}
```

---

## 7. Behavior Trees

### 7.1 Overview

**Behavior Trees** are hierarchical graphs that model AI decision-making through composable nodes. Unlike FSMs, BTs don't have explicit states - they continuously evaluate the tree from root to leaves each tick.

### 7.2 When to Use

- **Reactive behaviors**: AI must respond quickly to changes
- **Modular design**: Behaviors composed from reusable nodes
- **Team development**: Non-programmers can understand trees
- **Visual editing**: Designers need to tweak AI
- **Complex logic**: Many interacting conditions

### 7.3 Strengths and Weaknesses

| Strengths | Weaknesses |
|-----------|------------|
| Highly modular and reusable | Can be expensive (evaluates entire tree) |
| Easy to extend with new nodes | Tool dependence for complex trees |
| Great visual representation | Learning curve for node types |
| Reactive to world changes | Harder to debug than FSMs |
| Designer-friendly | Overkill for simple behaviors |

### 7.4 Implementation Complexity

**Medium-High** - Understanding node types and tree composition

```java
// Behavior Tree core interface
public interface BTNode {
    NodeStatus tick();
}

public enum NodeStatus {
    SUCCESS,
    FAILURE,
    RUNNING
}

// Composite nodes
public class SequenceNode implements BTNode {
    private final List<BTNode> children = new ArrayList<>();
    private int currentChild = 0;

    public void addChild(BTNode child) {
        children.add(child);
    }

    @Override
    public NodeStatus tick() {
        while (currentChild < children.size()) {
            NodeStatus status = children.get(currentChild).tick();

            if (status == NodeStatus.FAILURE) {
                currentChild = 0; // Reset for next tick
                return NodeStatus.FAILURE;
            }

            if (status == NodeStatus.RUNNING) {
                return NodeStatus.RUNNING; // Continue next tick
            }

            currentChild++; // This child succeeded, try next
        }

        currentChild = 0; // Reset for next tick
        return NodeStatus.SUCCESS;
    }
}

public class SelectorNode implements BTNode {
    private final List<BTNode> children = new ArrayList<>();
    private int currentChild = 0;

    @Override
    public NodeStatus tick() {
        while (currentChild < children.size()) {
            NodeStatus status = children.get(currentChild).tick();

            if (status == NodeStatus.SUCCESS) {
                currentChild = 0;
                return NodeStatus.SUCCESS;
            }

            if (status == NodeStatus.RUNNING) {
                return NodeStatus.RUNNING;
            }

            currentChild++;
        }

        currentChild = 0;
        return NodeStatus.FAILURE;
    }
}

// Leaf nodes
public class ConditionNode implements BTNode {
    private final Predicate<Context> condition;

    public ConditionNode(Predicate<Context> condition) {
        this.condition = condition;
    }

    @Override
    public NodeStatus tick() {
        return condition.test(getContext()) ?
               NodeStatus.SUCCESS : NodeStatus.FAILURE;
    }
}

public class ActionNode implements BTNode {
    private final Consumer<Context> action;

    public ActionNode(Consumer<Context> action) {
        this.action = action;
    }

    @Override
    public NodeStatus tick() {
        action.accept(getContext());
        return NodeStatus.SUCCESS;
    }
}
```

### 7.5 Example Games

| Game | Use Case | Year |
|------|----------|------|
| Halo series | Combat and squad AI | 2001-2021 |
| Assassin's Creed series | NPC behaviors | 2007-2023 |
| Batman: Arkham series | Enemy combat AI | 2009-2015 |
| Splinter Cell: Blacklist | Stealth AI | 2013 |
| Horizon Zero Dawn | Robot dinosaur AI | 2017 |

### 7.6 Code Structure

```
Behavior Tree Architecture
│
├─ Composite Nodes (Control flow)
│  ├─ Sequence (AND): All children must succeed
│  ├─ Selector (OR): One child must succeed
│  ├─ Parallel: All children run simultaneously
│  └─ Decorator: Wraps child to modify behavior
│     ├─ Inverter: Inverts child's result
│     ├─ Repeater: Repeats child N times
│     ├─ RepeatUntilFailure: Repeats until child fails
│     └─ Cooldown: Adds delay between executions
│
├─ Leaf Nodes (Terminal)
│  ├─ Condition: Checks a predicate
│  ├─ Action: Performs a behavior
│  └─ Wait: Delays for N seconds
│
└─ Context
   ├─ Blackboard: Shared data storage
   ├─ World state: Read-only world data
   └─ Agent state: Agent-specific data
```

### 7.7 Example Behavior Tree

```
Combat Selector (Choose best option)
├─ Sequence: Melee Attack
│  ├─ Condition: Enemy in melee range
│  ├─ Action: Equip melee weapon
│  ├─ Action: Attack enemy
│  └─ Action: Retreat slightly
├─ Sequence: Ranged Attack
│  ├─ Condition: Enemy in range
│  ├─ Condition: Have ammo
│  ├─ Action: Equip ranged weapon
│  ├─ Action: Aim at enemy
│  └─ Action: Fire
├─ Sequence: Find Cover
│  ├─ Condition: Under fire
│  ├─ Condition: Cover nearby
│  ├─ Action: Move to cover
│  └─ Decorator: Cooldown (5 seconds)
└─ Sequence: Patrol
   ├─ Action: Choose waypoint
   └─ Action: Move to waypoint
```

### 7.8 Decorator Nodes

Decorators wrap child nodes to modify their behavior:

```java
// Inverter: SUCCESS ↔ FAILURE
public class InverterNode implements BTNode {
    private final BTNode child;

    public InverterNode(BTNode child) {
        this.child = child;
    }

    @Override
    public NodeStatus tick() {
        NodeStatus status = child.tick();
        if (status == NodeStatus.SUCCESS) return NodeStatus.FAILURE;
        if (status == NodeStatus.FAILURE) return NodeStatus.SUCCESS;
        return status; // RUNNING stays RUNNING
    }
}

// Repeater: Run child N times
public class RepeaterNode implements BTNode {
    private final BTNode child;
    private final int count;
    private int iterations = 0;

    public RepeaterNode(BTNode child, int count) {
        this.child = child;
        this.count = count;
    }

    @Override
    public NodeStatus tick() {
        while (iterations < count) {
            NodeStatus status = child.tick();
            if (status == NodeStatus.RUNNING) {
                return NodeStatus.RUNNING;
            }
            iterations++;
        }
        iterations = 0;
        return NodeStatus.SUCCESS;
    }
}

// Cooldown: Delay between executions
public class CooldownNode implements BTNode {
    private final BTNode child;
    private final long cooldownMs;
    private long lastExecution = 0;

    public CooldownNode(BTNode child, long cooldownMs) {
        this.child = child;
        this.cooldownMs = cooldownMs;
    }

    @Override
    public NodeStatus tick() {
        long now = System.currentTimeMillis();
        if (now - lastExecution < cooldownMs) {
            return NodeStatus.FAILURE; // Not ready
        }
        NodeStatus status = child.tick();
        if (status == NodeStatus.SUCCESS) {
            lastExecution = now;
        }
        return status;
    }
}
```

### 7.9 Blackboard System

Behavior trees often use a blackboard for data sharing:

```java
public class Blackboard {
    private final Map<String, Object> data = new ConcurrentHashMap<>();

    public void set(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        return value != null ? (T) value : null;
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }
}

// Usage in BT nodes
public class HasAmmoCondition implements BTNode {
    private final Blackboard blackboard;

    public HasAmmoCondition(Blackboard blackboard) {
        this.blackboard = blackboard;
    }

    @Override
    public NodeStatus tick() {
        Integer ammo = blackboard.get("ammo", Integer.class);
        return ammo != null && ammo > 0 ?
               NodeStatus.SUCCESS : NodeStatus.FAILURE;
    }
}
```

---

## 8. Utility AI / Scoring Systems

### 8.1 Overview

**Utility AI** selects actions by scoring all available options based on weighted factors. The action with the highest score wins. This creates nuanced, context-dependent decisions without explicit rules or states.

### 8.2 When to Use

- **Nuanced decisions**: Many competing factors
- **Dynamic environments**: Context changes frequently
- **Emergent behavior**: Want surprising, intelligent choices
- **Player modeling**: Adapt to player playstyle
- **Sandbox games**: Unpredictable player actions

### 8.3 Strengths and Weaknesses

| Strengths | Weaknesses |
|-----------|------------|
| Natural, nuanced decisions | Hard to predict/debug |
| Easy to add new actions | Tuning weights is difficult |
| Handles complexity well | Performance can suffer with many actions |
| No state explosion | Requires good evaluation functions |
| Adapts to context | Less intuitive than BT/FSM |

### 8.4 Implementation Complexity

**Medium** - Requires designing scoring functions and weights

```java
// Utility AI core
public class UtilityAction {
    private final String name;
    private final List<UtilityFactor> factors;
    private double score;

    public UtilityAction(String name) {
        this.name = name;
        this.factors = new ArrayList<>();
    }

    public void addFactor(UtilityFactor factor) {
        factors.add(factor);
    }

    public double calculateScore(Context context) {
        double totalScore = 0.0;
        double totalWeight = 0.0;

        for (UtilityFactor factor : factors) {
            double weight = factor.getWeight();
            double value = factor.evaluate(context);

            totalScore += weight * value;
            totalWeight += weight;
        }

        this.score = totalWeight > 0 ? totalScore / totalWeight : 0.0;
        return this.score;
    }
}

public interface UtilityFactor {
    double evaluate(Context context);
    double getWeight();
    String getName();
}

// Example factors
public class HealthFactor implements UtilityFactor {
    @Override
    public double evaluate(Context context) {
        double health = context.getAgent().getHealth();
        double maxHealth = context.getAgent().getMaxHealth();
        return health / maxHealth; // 0.0 to 1.0
    }

    @Override
    public double getWeight() {
        return 1.0; // Medium importance
    }

    @Override
    public String getName() {
        return "health";
    }
}

public class DistanceToEnemyFactor implements UtilityFactor {
    @Override
    public double evaluate(Context context) {
        double distance = context.getDistanceToEnemy();
        // Score: 1.0 at close range, 0.0 at far range
        return Math.max(0.0, 1.0 - (distance / 50.0));
    }

    @Override
    public double getWeight() {
        return 2.0; // High importance
    }

    @Override
    public String getName() {
        return "distance_to_enemy";
    }
}

// Utility selector
public class UtilitySelector {
    private final List<UtilityAction> actions = new ArrayList<>();

    public void addAction(UtilityAction action) {
        actions.add(action);
    }

    public UtilityAction selectBest(Context context) {
        UtilityAction best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (UtilityAction action : actions) {
            double score = action.calculateScore(context);
            if (score > bestScore) {
                bestScore = score;
                best = action;
            }
        }

        return best;
    }
}
```

### 8.5 Example Games

| Game | Use Case | Year |
|------|----------|------|
| The Sims series | Need satisfaction, behavior selection | 2000-2024 |
| Galactic Civilizations II | Diplomacy decisions | 2006 |
| Civilization IV- VI | City management, diplomacy | 2005-2024 |
| Tom Clancy's Rainbow Six Siege | AI behavior selection | 2015 |
| Alien: Isolation | Alien behavior | 2014 |

### 8.6 Code Structure

```
Utility AI Architecture
│
├─ UtilityAction
│  ├─ name: String
│  ├─ factors: List<UtilityFactor>
│  ├─ score: double
│  └─ calculateScore(context): double
│
├─ UtilityFactor (interface)
│  ├─ evaluate(context): double (0.0 to 1.0)
│  ├─ getWeight(): double
│  └─ getName(): String
│
├─ Common Factors
│  ├─ DistanceFactor
│  ├─ HealthFactor
│  ├─ AmmoFactor
│  ├─ VisibilityFactor
│  ├─ TimeFactor
│  └─ ResourceFactor
│
├─ Scoring Curves
│  ├─ Linear: score = x
│  ├─ Quadratic: score = x^2
│  ├─ Exponential: score = e^(kx)
│  └─ Sigmoid: score = 1 / (1 + e^(-k(x - x0)))
│
└─ UtilitySelector
   ├─ actions: List<UtilityAction>
   └─ selectBest(context): UtilityAction
```

### 8.7 Scoring Curves

Raw values often need to be transformed into 0-1 scores:

```java
public interface ScoringCurve {
    double score(double value);
}

// Linear: Direct mapping
public class LinearCurve implements ScoringCurve {
    private final double min;
    private final double max;

    public LinearCurve(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public double score(double value) {
        return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
    }
}

// Quadratic: Emphasize high or low values
public class QuadraticCurve implements ScoringCurve {
    private final double exponent;

    public QuadraticCurve(double exponent) {
        this.exponent = exponent;
    }

    @Override
    public double score(double value) {
        double clamped = Math.max(0.0, Math.min(1.0, value));
        return Math.pow(clamped, exponent);
    }
}

// Sigmoid: Smooth S-curve
public class SigmoidCurve implements ScoringCurve {
    private final double center;
    private final double steepness;

    public SigmoidCurve(double center, double steepness) {
        this.center = center;
        this.steepness = steepness;
    }

    @Override
    public double score(double value) {
        return 1.0 / (1.0 + Math.exp(-steepness * (value - center)));
    }
}
```

### 8.8 Composite Factors

Combine multiple factors into one:

```java
public class CompositeFactor implements UtilityFactor {
    private final List<UtilityFactor> factors = new ArrayList<>();
    private final CombinationMethod method;

    public enum CombinationMethod {
        AVERAGE, WEIGHTED_AVERAGE, MIN, MAX, MULTIPLY
    }

    public CompositeFactor(CombinationMethod method) {
        this.method = method;
    }

    public void addFactor(UtilityFactor factor) {
        factors.add(factor);
    }

    @Override
    public double evaluate(Context context) {
        if (factors.isEmpty()) return 0.0;

        switch (method) {
            case AVERAGE:
                return factors.stream()
                    .mapToDouble(f -> f.evaluate(context))
                    .average()
                    .orElse(0.0);

            case WEIGHTED_AVERAGE:
                double sum = 0.0;
                double totalWeight = 0.0;
                for (UtilityFactor f : factors) {
                    double value = f.evaluate(context);
                    double weight = f.getWeight();
                    sum += value * weight;
                    totalWeight += weight;
                }
                return totalWeight > 0 ? sum / totalWeight : 0.0;

            case MIN:
                return factors.stream()
                    .mapToDouble(f -> f.evaluate(context))
                    .min()
                    .orElse(0.0);

            case MAX:
                return factors.stream()
                    .mapToDouble(f -> f.evaluate(context))
                    .max()
                    .orElse(0.0);

            case MULTIPLY:
                return factors.stream()
                    .mapToDouble(f -> f.evaluate(context))
                    .reduce(1.0, (a, b) -> a * b);

            default:
                return 0.0;
        }
    }

    @Override
    public double getWeight() {
        return 1.0;
    }

    @Override
    public String getName() {
        return "composite";
    }
}
```

---

## 9. GOAP (Goal-Oriented Action Planning)

### 9.1 Overview

**GOAP (Goal-Oriented Action Planning)** is a planning architecture where agents set goals and use A* search through action space to find sequences that achieve those goals. Each action has preconditions (world state requirements) and effects (changes to world state).

### 9.2 When to Use

- **Goal-driven behavior**: Agents have clear objectives
- **Dynamic environments**: World state changes unpredictably
- **Replanning needs**: Plans may fail mid-execution
- **Tactical AI**: Combat, stealth, resource management
- **Emergent planning**: Want agents to find creative solutions

### 9.3 Strengths and Weaknesses

| Strengths | Weaknesses |
|-----------|------------|
| Emergent, creative plans | Can be expensive (A* search) |
| Easy to add new actions | Requires good world state model |
| Handles complexity well | Hard to predict behavior |
| Dynamic replanning | Debugging is difficult |
| Goal-oriented design | Overkill for simple tasks |

### 9.4 Implementation Complexity

**High** - Requires A* planner, action definitions, world state model

```java
// GOAP core structures
public interface WorldState {
    boolean has(String key);
    Object get(String key);
    void set(String key, Object value);
    WorldState copy();
}

public interface GOAPAction {
    boolean canExecute(WorldState state);  // Preconditions
    WorldState execute(WorldState state);  // Effects
    double getCost();                      // Action cost
    String getName();
}

public interface Goal {
    boolean isSatisfied(WorldState state);
    double getPriority();
    String getName();
}

// A* planner for action sequences
public class GOAPPlanner {
    public List<GOAPAction> plan(
        WorldState startState,
        Goal goal,
        List<GOAPAction> availableActions
    ) {
        // A* search through action space
        PriorityQueue<PlanNode> openSet = new PriorityQueue<>();
        Map<WorldState, PlanNode> visited = new HashMap<>();

        PlanNode startNode = new PlanNode(startState, null, null, 0, heuristic(startState, goal));
        openSet.add(startNode);
        visited.put(startState, startNode);

        while (!openSet.isEmpty()) {
            PlanNode current = openSet.poll();

            // Goal reached?
            if (goal.isSatisfied(current.state)) {
                return reconstructPath(current);
            }

            // Try all actions
            for (GOAPAction action : availableActions) {
                if (!action.canExecute(current.state)) {
                    continue; // Preconditions not met
                }

                WorldState newState = action.execute(current.state.copy());
                double newCost = current.gCost + action.getCost();

                PlanNode existingNode = visited.get(newState);
                if (existingNode == null || newCost < existingNode.gCost) {
                    PlanNode newNode = new PlanNode(
                        newState,
                        action,
                        current,
                        newCost,
                        newCost + heuristic(newState, goal)
                    );

                    if (existingNode == null) {
                        openSet.add(newNode);
                        visited.put(newState, newNode);
                    } else {
                        existingNode.update(newNode);
                    }
                }
            }
        }

        return null; // No plan found
    }

    private double heuristic(WorldState state, Goal goal) {
        // Admissible heuristic: estimate remaining cost
        return goal.isSatisfied(state) ? 0.0 : 1.0;
    }

    private List<GOAPAction> reconstructPath(PlanNode node) {
        List<GOAPAction> path = new ArrayList<>();
        while (node.action != null) {
            path.add(0, node.action);
            node = node.parent;
        }
        return path;
    }
}

// Example action
public class AttackAction implements GOAPAction {
    @Override
    public boolean canExecute(WorldState state) {
        Boolean enemyInRange = state.get("enemy_in_range");
        Boolean hasWeapon = state.get("has_weapon");
        return Boolean.TRUE.equals(enemyInRange) && Boolean.TRUE.equals(hasWeapon);
    }

    @Override
    public WorldState execute(WorldState state) {
        WorldState newState = state.copy();
        newState.set("enemy_health", newState.get("enemy_health") - 10);
        newState.set("ammo", newState.get("ammo") - 1);
        return newState;
    }

    @Override
    public double getCost() {
        return 1.0;
    }

    @Override
    public String getName() {
        return "attack";
    }
}

// Example goal
public class EliminateEnemyGoal implements Goal {
    @Override
    public boolean isSatisfied(WorldState state) {
        Integer health = state.get("enemy_health");
        return health != null && health <= 0;
    }

    @Override
    public double getPriority() {
        return 10.0;
    }

    @Override
    public String getName() {
        return "eliminate_enemy";
    }
}
```

### 9.5 Example Games

| Game | Use Case | Year |
|------|----------|------|
| F.E.A.R. | Tactical combat AI | 2005 |
| S.T.A.L.K.E.R. | Survival planning | 2007 |
| Hitman series | Stealth planning | 2000-2021 |
| Kenshi | Squad tactics | 2018 |
| Shadow of Mordor | Nemesis system behaviors | 2014 |

### 9.6 Code Structure

```
GOAP Architecture
│
├─ WorldState
│  ├─ state: Map<String, Object>
│  ├─ has(key): boolean
│  ├─ get(key): Object
│  ├─ set(key, value): void
│  └─ copy(): WorldState
│
├─ GOAPAction (interface)
│  ├─ canExecute(state): boolean (Preconditions)
│  ├─ execute(state): WorldState (Effects)
│  ├─ getCost(): double
│  └─ getName(): String
│
├─ Goal (interface)
│  ├─ isSatisfied(state): boolean
│  ├─ getPriority(): double
│  └─ getName(): String
│
├─ GOAPPlanner
│  ├─ plan(start, goal, actions): List<GOAPAction>
│  ├─ heuristic(state, goal): double
│  └─ reconstructPath(node): List<GOAPAction>
│
└─ Action Executor
   ├─ currentPlan: List<GOAPAction>
   ├─ currentActionIndex: int
   ├─ execute(plan): void
   └─ replanIfNecessary(): void
```

### 9.7 Action Preconditions and Effects

```java
// Example actions with preconditions and effects
public class ReloadAction implements GOAPAction {
    @Override
    public boolean canExecute(WorldState state) {
        Boolean hasAmmo = state.get("has_reserve_ammo");
        Boolean needsReload = state.get("needs_reload");
        return Boolean.TRUE.equals(hasAmmo) && Boolean.TRUE.equals(needsReload);
    }

    @Override
    public WorldState execute(WorldState state) {
        WorldState newState = state.copy();
        newState.set("ammo", 30); // Full magazine
        newState.set("needs_reload", false);
        newState.set("reserve_ammo", state.get("reserve_ammo") - 30);
        return newState;
    }

    @Override
    public double getCost() {
        return 2.0; // Reloading takes time
    }

    @Override
    public String getName() {
        return "reload";
    }
}

public class TakeCoverAction implements GOAPAction {
    @Override
    public boolean canExecute(WorldState state) {
        Boolean inCombat = state.get("in_combat");
        Boolean coverNearby = state.get("cover_nearby");
        Boolean inCover = state.get("in_cover");
        return Boolean.TRUE.equals(inCombat) &&
               Boolean.TRUE.equals(coverNearby) &&
               !Boolean.TRUE.equals(inCover);
    }

    @Override
    public WorldState execute(WorldState state) {
        WorldState newState = state.copy();
        newState.set("in_cover", true);
        newState.set("exposure", 0.2); // Reduced exposure
        return newState;
    }

    @Override
    public double getCost() {
        return 1.5;
    }

    @Override
    public String getName() {
        return "take_cover";
    }
}
```

### 9.8 Replanning

GOAP systems must replan when world state changes unexpectedly:

```java
public class GOAPExecutor {
    private List<GOAPAction> currentPlan;
    private int currentActionIndex = 0;
    private WorldState expectedState;

    public void executePlan(List<GOAPAction> plan, WorldState startState) {
        this.currentPlan = plan;
        this.currentActionIndex = 0;
        this.expectedState = startState;
    }

    public void tick(WorldState actualState) {
        if (currentPlan == null || currentActionIndex >= currentPlan.size()) {
            return; // No plan or plan complete
        }

        // Check if state changed unexpectedly
        if (!statesMatch(actualState, expectedState)) {
            // Trigger replan
            requestReplan();
            return;
        }

        // Execute current action
        GOAPAction currentAction = currentPlan.get(currentActionIndex);
        if (currentAction.canExecute(actualState)) {
            expectedState = currentAction.execute(actualState.copy());
            currentActionIndex++;
        } else {
            // Can't execute action - replan
            requestReplan();
        }
    }

    private boolean statesMatch(WorldState a, WorldState b) {
        // Check if critical state matches expected
        // Implementation depends on what's important for your game
        return true;
    }

    private void requestReplan() {
        // Notify planner to generate new plan
        currentPlan = null;
        currentActionIndex = 0;
    }
}
```

---

## 10. HTN (Hierarchical Task Networks)

### 10.1 Overview

**HTN (Hierarchical Task Networks)** decomposes high-level tasks into primitive actions through hierarchical methods. Unlike GOAP's emergent planning, HTN uses designer-specified decomposition rules, making it more predictable and controllable.

### 10.2 When to Use

- **Structured tasks**: Clear hierarchies (build house → gather materials → construct)
- **Designer control**: Want predictable agent behavior
- **Multi-agent coordination**: Tasks can be distributed
- **Complex workflows**: Many steps with clear dependencies
- **Predictable outputs**: Same task = same plan

### 10.3 Strengths and Weaknesses

| Strengths | Weaknesses |
|-----------|------------|
| Designer-specified behavior | Requires upfront domain modeling |
| Very predictable | Can't discover novel solutions |
| Fast planning (no search) | Methods must be manually defined |
| Natural for structured tasks | Brittle if methods are incomplete |
| Great for multi-agent | Less flexible than GOAP |

### 10.4 Implementation Complexity

**High** - Requires designing task hierarchies and methods

```java
// HTN core structures
public interface Task {
    String getName();
    boolean isPrimitive();
}

public class PrimitiveTask implements Task {
    private final String name;
    private final Predicate<WorldState> precondition;
    private final Consumer<WorldState> effect;

    public PrimitiveTask(String name,
                        Predicate<WorldState> precondition,
                        Consumer<WorldState> effect) {
        this.name = name;
        this.precondition = precondition;
        this.effect = effect;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    public boolean canExecute(WorldState state) {
        return precondition.test(state);
    }

    public void execute(WorldState state) {
        effect.accept(state);
    }
}

public class CompoundTask implements Task {
    private final String name;
    private final List<Method> methods = new ArrayList<>();

    public CompoundTask(String name) {
        this.name = name;
    }

    public void addMethod(Method method) {
        methods.add(method);
    }

    public List<Method> getMethods() {
        return methods;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }
}

public class Method {
    private final String name;
    private final Predicate<WorldState> precondition;
    private final List<Task> subtasks;

    public Method(String name,
                 Predicate<WorldState> precondition,
                 List<Task> subtasks) {
        this.name = name;
        this.precondition = precondition;
        this.subtasks = subtasks;
    }

    public boolean canApply(WorldState state) {
        return precondition.test(state);
    }

    public List<Task> getSubtasks() {
        return subtasks;
    }

    public String getName() {
        return name;
    }
}

// HTN planner (recursive decomposition)
public class HTNPlanner {
    public List<PrimitiveTask> plan(Task rootTask, WorldState state) {
        List<PrimitiveTask> plan = new ArrayList<>();
        if (decompose(rootTask, state, plan)) {
            return plan;
        }
        return null; // Planning failed
    }

    private boolean decompose(Task task, WorldState state, List<PrimitiveTask> plan) {
        if (task.isPrimitive()) {
            PrimitiveTask primitive = (PrimitiveTask) task;
            if (!primitive.canExecute(state)) {
                return false; // Precondition failed
            }
            plan.add(primitive);
            return true;
        }

        CompoundTask compound = (CompoundTask) task;

        // Find first applicable method
        for (Method method : compound.getMethods()) {
            if (method.canApply(state)) {
                // Try to decompose all subtasks
                int planSize = plan.size();
                boolean success = true;

                for (Task subtask : method.getSubtasks()) {
                    if (!decompose(subtask, state, plan)) {
                        success = false;
                        break;
                    }
                }

                if (success) {
                    return true; // All subtasks decomposed
                }

                // Backtrack: remove added tasks
                while (plan.size() > planSize) {
                    plan.remove(plan.size() - 1);
                }
            }
        }

        return false; // No applicable method
    }
}
```

### 10.5 Example Games

| Game | Use Case | Year |
|------|----------|------|
| Horizon Zero Dawn | Robot dinosaur behaviors | 2017 |
| Total Warrior | Combat tactics | 2005 |
| MOBA games (LoL, Dota 2) | AI lane management | 2009-2024 |
| The Sims 4 | Complex social behaviors | 2014 |
| Red Dead Redemption 2 | NPC daily routines | 2018 |

### 10.6 Code Structure

```
HTN Architecture
│
├─ Task (interface)
│  ├─ PrimitiveTask (executable action)
│  │  ├─ name: String
│  │  ├─ precondition: Predicate<WorldState>
│  │  └─ effect: Consumer<WorldState>
│  │
│  └─ CompoundTask (decomposable task)
│     ├─ name: String
│     └─ methods: List<Method>
│
├─ Method
│  ├─ name: String
│  ├─ precondition: Predicate<WorldState>
│  └─ subtasks: List<Task>
│
├─ HTNPlanner
│  ├─ plan(rootTask, state): List<PrimitiveTask>
│  └─ decompose(task, state, plan): boolean
│
└─ Domain Example
   ├─ build_house (Compound)
   │  ├─ Method: build_with_wood
   │  │  ├─ Precondition: has_wood AND has_space
   │  │  └─ Subtasks: [gather_wood, clear_space, construct]
   │  └─ Method: build_with_stone
   │     ├─ Precondition: has_stone AND has_space
   │     └─ Subtasks: [gather_stone, clear_space, construct]
   │
   └─ gather_wood (Compound)
      ├─ Method: gather_from_forest
      │  ├─ Precondition: forest_nearby
      │  └─ Subtasks: [move_to_forest, chop_tree, return]
      └─ Method: gather_from_chest
         ├─ Precondition: chest_has_wood
         └─ Subtasks: [move_to_chest, take_wood]
```

### 10.7 Example HTN Domain

```java
// Minecraft building example
public class MinecraftHTNDomain {
    public CompoundTask createBuildHouseTask() {
        CompoundTask buildHouse = new CompoundTask("build_house");

        // Method 1: Build with cobblestone
        buildHouse.addMethod(new Method(
            "build_with_cobblestone",
            state -> {
                Integer cobble = state.get("cobblestone_count");
                return cobble != null && cobble >= 64;
            },
            Arrays.asList(
                new PrimitiveTask("clear_area",
                    state -> true,
                    state -> {/* Clear blocks */}),
                new PrimitiveTask("place_foundation",
                    state -> {
                        Integer cobble = state.get("cobblestone_count");
                        return cobble >= 16;
                    },
                    state -> {
                        int cobble = state.get("cobblestone_count");
                        state.set("cobblestone_count", cobble - 16);
                    }),
                new PrimitiveTask("place_walls",
                    state -> {
                        Integer cobble = state.get("cobblestone_count");
                        return cobble >= 32;
                    },
                    state -> {
                        int cobble = state.get("cobblestone_count");
                        state.set("cobblestone_count", cobble - 32);
                    }),
                new PrimitiveTask("place_roof",
                    state -> {
                        Integer cobble = state.get("cobblestone_count");
                        return cobble >= 16;
                    },
                    state -> {
                        int cobble = state.get("cobblestone_count");
                        state.set("cobblestone_count", cobble - 16);
                    })
            )
        ));

        // Method 2: Build with wood (fallback)
        buildHouse.addMethod(new Method(
            "build_with_wood",
            state -> {
                Integer wood = state.get("wood_count");
                return wood != null && wood >= 64;
            },
            Arrays.asList(
                // Similar structure with wood
            )
        ));

        return buildHouse;
    }
}
```

### 10.8 HTN vs GOAP Comparison

| Aspect | HTN | GOAP |
|--------|-----|------|
| **Planning Direction** | Forward (top-down) | Backward (goal-driven) |
| **Control** | Designer-specified methods | Emergent from actions |
| **Predictability** | High - same task = same plan | Low - depends on search |
| **Speed** | Fast (no search) | Slower (A* search) |
| **Flexibility** | Brittle if methods incomplete | Can discover novel plans |
| **Use Case** | Structured, known workflows | Open-ended problems |
| **Learning Curve** | Steeper (domain modeling) | Moderate (action effects) |

---

## 11. Hybrid Approaches

### 11.1 Overview

Modern game AI often combines multiple architectures to leverage their strengths. Hybrid architectures use the right tool for each job while mitigating individual weaknesses.

### 11.2 Common Hybrid Patterns

#### FSM + Behavior Tree

**Pattern**: FSM for high-level states, BT for state behaviors

```
FSM State: Combat
└─ Behavior Tree: Combat behaviors
   ├─ Selector: Choose combat action
   │  ├─ Sequence: Melee attack
   │  ├─ Sequence: Ranged attack
   │  └─ Sequence: Grenade throw

FSM State: Patrol
└─ Behavior Tree: Patrol behaviors
   ├─ Sequence: Follow waypoints
   └─ Sequence: Investigate sounds
```

**When to use**: Clear state transitions but complex state behaviors

```java
public class FSMBehaviorTree {
    private enum State {
        COMBAT,
        PATROL,
        IDLE
    }

    private State currentState = State.IDLE;
    private final Map<State, BehaviorTree> stateTrees = new HashMap<>();

    public FSMBehaviorTree() {
        stateTrees.put(State.COMBAT, createCombatTree());
        stateTrees.put(State.PATROL, createPatrolTree());
        stateTrees.put(State.IDLE, createIdleTree());
    }

    public void update(Context context) {
        // Check for state transitions (FSM)
        State newState = determineState(context);
        if (newState != currentState) {
            currentState = newState;
        }

        // Execute behavior tree for current state
        BehaviorTree tree = stateTrees.get(currentState);
        tree.tick(context);
    }

    private State determineState(Context context) {
        if (context.isUnderAttack()) {
            return State.COMBAT;
        } else if (context.hasPatrolRoute()) {
            return State.PATROL;
        }
        return State.IDLE;
    }
}
```

#### Utility + Behavior Tree

**Pattern**: Utility AI for action selection, BT for action execution

```
Utility System: Selects best action
└─ Highest-scoring action: Attack
   └─ Behavior Tree: Execute attack
      ├─ Sequence: Aim at enemy
      ├─ Sequence: Fire weapon
      └─ Sequence: Reload if empty
```

**When to use**: Many possible actions with complex execution logic

```java
public class UtilityBehaviorTree {
    private final UtilitySelector utilitySelector;
    private final Map<String, BehaviorTree> actionTrees;

    public void update(Context context) {
        // Use utility to select action
        UtilityAction bestAction = utilitySelector.selectBest(context);

        // Execute action's behavior tree
        BehaviorTree tree = actionTrees.get(bestAction.getName());
        tree.tick(context);
    }
}
```

#### GOAP + Utility

**Pattern**: Utility for goal selection, GOAP for planning

```
Utility System: Selects highest-priority goal
└─ Goal: Eliminate enemy (priority: 0.9)
   └─ GOAP Planner: Generates action sequence
      ├─ Take cover
      ├─ Reload
      ├─ Aim
      └─ Attack
```

**When to use**: Multiple competing goals with complex planning

```java
public class UtilityGOAP {
    private final List<Goal> goals;
    private final GOAPPlanner planner;

    public void update(Context context) {
        // Score all goals
        Goal bestGoal = goals.stream()
            .max(Comparator.comparingDouble(g -> g.getPriority()))
            .orElse(null);

        if (bestGoal != null && !bestGoal.isSatisfied(context.getWorldState())) {
            // Generate plan to achieve goal
            List<GOAPAction> plan = planner.plan(
                context.getWorldState(),
                bestGoal,
                context.getAvailableActions()
            );

            // Execute plan
            executePlan(plan, context);
        }
    }
}
```

#### FSM + Utility

**Pattern**: FSM for explicit states, Utility for action selection within states

```
FSM State: Combat
└─ Utility System: Choose combat action
   ├─ Attack (score: 0.8)
   ├─ Reload (score: 0.6)
   └─ Take cover (score: 0.4)
```

**When to use**: Clear states but nuanced action selection

### 11.3 When to Combine Architectures

| Combination | Use Case | Example |
|------------|----------|---------|
| FSM + BT | Complex state behaviors | Combat AI with many attack options |
| Utility + BT | Many actions, complex execution | Sandbox NPC behavior |
| GOAP + Utility | Multiple goals, needs planning | RTS unit AI |
| FSM + Utility | Clear states, nuanced choices | RPG companion AI |
| HTN + BT | Structured tasks, reactive execution | Building games |

### 11.4 Layered Architecture

A common pattern is layered AI with different architectures at each level:

```
                    ┌────────────────────────────────┐
                    │    Strategic Layer (Utility)   │
                    │    Select high-level goal      │
                    └────────────┬───────────────────┘
                                 │
                    ┌────────────▼───────────────────┐
                    │    Tactical Layer (GOAP/HTN)   │
                    │    Generate action plan        │
                    └────────────┬───────────────────┘
                                 │
                    ┌────────────▼───────────────────┐
                    │    Operational Layer (BT/FSM)  │
                    │    Execute actions reactively  │
                    └────────────────────────────────┘
```

**Example**: RTS AI

```java
public class LayeredAI {
    private UtilitySystem strategicLayer;
    private GOAPPlanner tacticalLayer;
    private BehaviorTree operationalLayer;

    public void update(Context context) {
        // Strategic: Choose objective
        Goal objective = strategicLayer.selectGoal(context);

        // Tactical: Plan to achieve objective
        List<GOAPAction> plan = tacticalLayer.plan(objective, context);

        // Operational: Execute reactively
        for (GOAPAction action : plan) {
            if (!executeActionWithBT(action, context)) {
                break; // Plan interrupted, replan next tick
            }
        }
    }

    private boolean executeActionWithBT(GOAPAction action, Context context) {
        // Wrap GOAP action in behavior tree for reactive execution
        BehaviorTree actionTree = createBTForAction(action);
        NodeStatus status = actionTree.tick(context);
        return status == NodeStatus.SUCCESS;
    }
}
```

---

## 12. Data-Driven AI

### 12.1 Overview

**Data-driven AI** externalizes behavior definition from code to configuration files. This allows designers to tune AI without recompiling and enables hot-reloading during development.

### 12.2 Benefits

| Benefit | Description |
|---------|-------------|
| **Rapid iteration** | Designers tweak AI without programmer help |
| **Hot-reloading** | Changes apply without restarting game |
| **Modding support** | Community can create new behaviors |
| **Designer empowerment** | Non-programmers control AI |
| **Faster iteration** | No compile-test cycle |

### 12.3 Configuration Formats

#### JSON

```json
{
  "behavior_tree": {
    "type": "selector",
    "children": [
      {
        "type": "sequence",
        "children": [
          {
            "type": "condition",
            "name": "enemy_visible"
          },
          {
            "type": "action",
            "name": "attack_enemy"
          }
        ]
      },
      {
        "type": "action",
        "name": "patrol"
      }
    ]
  }
}
```

#### YAML

```yaml
utility_actions:
  - name: attack
    factors:
      - name: distance_to_enemy
        weight: 2.0
        curve: linear
      - name: health
        weight: 1.0
        curve: quadratic
      - name: ammo
        weight: 1.5
        curve: exponential
```

#### Lua

```lua
-- FSM definition
states = {
    idle = {
        onEnter = function(self)
            self:playAnimation("idle")
        end,
        onUpdate = function(self)
            if self:canSeePlayer() then
                self:transitionTo("chase")
            end
        end
    },
    chase = {
        onEnter = function(self)
            self:setSpeed(10.0)
        end,
        onUpdate = function(self)
            self:moveTo(self:getPlayerPosition())
            if self:getDistanceToPlayer() < 5.0 then
                self:transitionTo("attack")
            end
        end
    }
}
```

### 12.4 Visual Editing Tools

Many engines provide visual editors for AI:

- **Unreal Engine**: Behavior Tree editor, Utility AI widgets
- **Unity**: Behavior Designer, NodeCanvas
- **Godot**: Visual Script for AI behaviors

### 12.5 Hot-Reloading Implementation

```java
public class AIConfigManager {
    private final Map<String, AIConfig> configs = new ConcurrentHashMap<>();
    private final WatchService watcher;

    public void startWatching(String configPath) {
        watcher = FileSystems.getDefault().newWatchService();
        Path path = Paths.get(configPath);
        path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);

        Thread watcherThread = new Thread(() -> {
            while (true) {
                WatchKey key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changedFile = (Path) event.context();
                    reloadConfig(changedFile.toString());
                }
                key.reset();
            }
        });
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    private void reloadConfig(String filename) {
        try {
            AIConfig newConfig = loadConfig(filename);
            configs.put(filename, newConfig);
            LOGGER.info("Reloaded AI config: {}", filename);
        } catch (Exception e) {
            LOGGER.error("Failed to reload config: {}", filename, e);
        }
    }

    public AIConfig getConfig(String filename) {
        return configs.get(filename);
    }
}
```

### 12.6 Validation System

Data-driven AI needs validation to catch errors before runtime:

```java
public class AIConfigValidator {
    public ValidationResult validate(BehaviorTreeConfig config) {
        ValidationResult result = new ValidationResult();

        validateNode(config.getRoot(), result);

        return result;
    }

    private void validateNode(NodeConfig node, ValidationResult result) {
        // Check node type exists
        if (!nodeTypes.containsKey(node.getType())) {
            result.addError("Unknown node type: " + node.getType());
        }

        // Check required parameters
        NodeType type = nodeTypes.get(node.getType());
        for (String param : type.getRequiredParams()) {
            if (!node.hasParam(param)) {
                result.addError("Missing required parameter: " + param);
            }
        }

        // Recursively validate children
        for (NodeConfig child : node.getChildren()) {
            validateNode(child, result);
        }
    }
}
```

---

## 13. Minecraft-Specific Recommendations

### 13.1 Task Complexity Mapping

| Minecraft Task | Recommended Architecture | Rationale |
|----------------|------------------------|-----------|
| **Pathfinding** | A* Algorithm | Well-researched, optimal paths |
| **Mining** | FSM + Utility | States: IDLE, MINING, RETURNING; Utility for ore selection |
| **Building** | HTN | Hierarchical decomposition fits construction |
| **Combat** | Behavior Tree | Reactive combat with many options |
| **Farming** | FSM + Trigger | Simple cycle with event triggers |
| **Trading** | Utility AI | Score trades by value and need |
| **Exploration** | Utility + BT | Utility selects locations, BT navigates |
| **Crafting** | HTN + Trigger | Hierarchical recipes with resource triggers |

### 13.2 Recommended Architecture for Steve AI

Based on the analysis of the codebase and requirements:

```
┌─────────────────────────────────────────────────────────────┐
│                    Steve AI Architecture                      │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  LLM Layer (Optional) - Strategic Goal Setting        │  │
│  │  Input: Natural language command                     │  │
│  │  Output: High-level goals with constraints           │  │
│  └────────────────────────┬─────────────────────────────┘  │
│                           │                                 │
│  ┌────────────────────────▼─────────────────────────────┐  │
│  │  State Machine - High-Level Agent States             │  │
│  │  States: IDLE, PLANNING, EXECUTING, COMPLETED,      │  │
│  │          FAILED, PAUSED                               │  │
│  │  Transition validation + event publishing            │  │
│  └────────────────────────┬─────────────────────────────┘  │
│                           │                                 │
│  ┌────────────────────────▼─────────────────────────────┐  │
│  │  Utility System - Task Priority & Selection          │  │
│  │  Factors: Distance, resources, player proximity,     │  │
│  │           danger level, time of day                  │  │
│  └────────────────────────┬─────────────────────────────┘  │
│                           │                                 │
│  ┌────────────────────────▼─────────────────────────────┐  │
│  │  HTN Planner - Task Decomposition                    │  │
│  │  Decompose: "Build house" → gather → construct       │  │
│  │  Fallback to LLM for novel tasks                     │  │
│  └────────────────────────┬─────────────────────────────┘  │
│                           │                                 │
│  ┌────────────────────────▼─────────────────────────────┐  │
│  │  Action Registry - Plugin Architecture               │  │
│  │  Actions registered via ActionFactory                │  │
│  │  Core actions: Mine, Build, Craft, Move, etc.        │  │
│  └────────────────────────┬─────────────────────────────┘  │
│                           │                                 │
│  ┌────────────────────────▼─────────────────────────────┐  │
│  │  BaseAction - Tick-Based Execution                   │  │
│  │  Each action: onStart(), onTick(), onCancel()        │  │
│  │  Non-blocking, incremental progress                  │  │
│  └────────────────────────┬─────────────────────────────┘  │
│                           │                                 │
│  ┌────────────────────────▼─────────────────────────────┐  │
│  │  Interceptor Chain - Cross-Cutting Concerns          │  │
│  │  Logging → Metrics → Event Publishing                │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### 13.3 Implementation Priorities

| Priority | Component | Architecture | Effort | Impact |
|----------|-----------|--------------|--------|--------|
| **1** | Action Registry | Plugin Pattern | Done | High |
| **2** | State Machine | FSM | Done | High |
| **3** | Utility Scoring | Utility AI | Medium | High |
| **4** | Task Decomposition | HTN | High | High |
| **5** | Reactive Behaviors | Behavior Tree | Medium | Medium |
| **6** | Goal Selection | GOAP (optional) | High | Low |
| **7** | LLM Integration | Fallback | Done | High |

### 13.4 Action System Architecture

The current codebase implements a solid foundation:

```java
// Current Action Registry pattern
ActionRegistry registry = ActionRegistry.getInstance();

// Register actions
registry.register("mine", (steve, task, ctx) -> new MineBlockAction(steve, task));
registry.register("build", (steve, task, ctx) -> new BuildStructureAction(steve, task));
registry.register("craft", (steve, task, ctx) -> new CraftItemAction(steve, task));

// Execute actions
BaseAction action = registry.createAction(actionName, steve, task, context);
action.start();
while (!action.isComplete()) {
    action.tick();
}
```

This **Plugin Architecture** is excellent for extensibility and aligns with modern practices.

### 13.5 State Machine Integration

The existing `AgentStateMachine` provides:

- **Explicit transitions**: Validated state changes
- **Event publishing**: Observers can react to state changes
- **Thread safety**: AtomicReference for concurrent access
- **Recovery**: Force transition for error recovery

This is a solid FSM foundation that can be extended with HFSM if needed.

### 13.6 Utility AI Integration Point

Add utility scoring for task prioritization:

```java
public class TaskPrioritizer {
    private final List<UtilityFactor> factors = new ArrayList<>();

    public void addFactor(UtilityFactor factor) {
        factors.add(factor);
    }

    public UtilityScore score(Task task, DecisionContext context) {
        double totalScore = 0.0;
        double totalWeight = 0.0;
        Map<String, Double> factorValues = new TreeMap<>();

        for (UtilityFactor factor : factors) {
            double value = factor.evaluate(task, context);
            double weight = factor.getWeight();

            factorValues.put(factor.getName(), value);
            totalScore += weight * value;
            totalWeight += weight;
        }

        double finalScore = totalWeight > 0 ? totalScore / totalWeight : 0.5;
        return new UtilityScore(0.5, factorValues, finalScore);
    }
}

// Example factors
public class DistanceFactor implements UtilityFactor {
    @Override
    public double evaluate(Task task, DecisionContext context) {
        BlockPos target = task.getTargetPosition();
        BlockPos current = context.getAgentPosition();
        double distance = Math.sqrt(current.distSqr(target));

        // Score: 1.0 at 0 blocks, 0.0 at 100+ blocks
        return Math.max(0.0, 1.0 - (distance / 100.0));
    }

    @Override
    public double getWeight() {
        return 1.5; // Distance is important
    }

    @Override
    public String getName() {
        return "distance";
    }
}
```

### 13.7 HTN for Common Tasks

Define HTN methods for frequently executed tasks:

```java
public class MinecraftHTNMethods {

    public CompoundTask createGatherResourceTask(String resource) {
        CompoundTask task = new CompoundTask("gather_" + resource);

        // Method 1: Gather from nearby
        task.addMethod(new Method(
            "gather_nearby_" + resource,
            state -> {
                Boolean nearby = state.get(resource + "_nearby");
                return Boolean.TRUE.equals(nearby);
            },
            Arrays.asList(
                new PrimitiveTask("move_to_resource", s -> true, s -> {}),
                new PrimitiveTask("mine_resource", s -> true, s -> {}),
                new PrimitiveTask("return_to_base", s -> true, s -> {})
            )
        ));

        // Method 2: Craft if have materials
        task.addMethod(new Method(
            "craft_" + resource,
            state -> {
                Boolean canCraft = state.get("can_craft_" + resource);
                return Boolean.TRUE.equals(canCraft);
            },
            Arrays.asList(
                new PrimitiveTask("craft_" + resource, s -> true, s -> {})
            )
        ));

        return task;
    }
}
```

---

## 14. Implementation Decision Matrix

### 14.1 Architecture Selection Flowchart

```
                    ┌─────────────────────────────────┐
                    │   What is your AI doing?        │
                    └─────────────┬───────────────────┘
                                  │
            ┌─────────────────────┼─────────────────────┐
            │                     │                     │
    ┌───────▼──────┐    ┌───────▼──────┐    ┌───────▼──────┐
    │ Simple,      │    │ Complex,     │    │ Goal-        │
    │ fixed        │    │ adaptive     │    │ oriented     │
    │ behavior?    │    │ behavior?    │    │ planning?    │
    └───────┬──────┘    └───────┬──────┘    └───────┬──────┘
            │                    │                    │
    ┌───────▼────────┐  ┌───────▼────────┐  ┌───────▼────────┐
    │ Rule-Based     │  │ Clear states?  │  │ Designer       │
    │ System         │  │                │  │ control?       │
    └────────────────┘  └───────┬────────┘  └───────┬────────┘
                                 │                    │
                         ┌───────▼────────┐  ┌───────▼────────┐
                    YES  │                │  │                │ NO
                    ┌────▼────┐    ┌─────▼─────┐    ┌─────▼─────┐
                    │ FSM/HFSM │    │ Utility   │    │ HTN       │
                    │          │    │ + BT      │    │           │
                    └────┬─────┘    └─────┬─────┘    └─────┬─────┘
                         │                │                │
                         │    Reactive?   │                │
                         │    ┌────┬─────┐ │                │
                         │    │ NO │ YES│ │                │
                         │    └─┬──┴─┬──┘ │                │
                         │      │     │   │                │
                         │   ┌──▼──┐ ┌▼───▼──┐             │
                         │   │Stay │ │ Add   │             │
                         │   │FSM  │ │ BT    │             │
                         │   └─────┘ └───────┘             │
                         │                                  │
                         └────────────┬─────────────────────┘
                                      │
                               ┌──────▼──────────┐
                               │ Consider        │
                               │ Hybrid          │
                               │ (FSM+BT,        │
                               │  Utility+BT)    │
                               └─────────────────┘
```

### 14.2 Minecraft Task Architecture Guide

| Task Type | Architecture | Rationale | Example |
|-----------|--------------|-----------|---------|
| **Pathfinding** | A* Algorithm | Optimal paths, well-researched | Navigate to position |
| **Mining** | FSM + Utility | States for phases, utility for ore selection | Mine iron ore |
| **Building** | HTN | Hierarchical task decomposition | Build house |
| **Combat** | Behavior Tree | Reactive combat with many options | Fight zombie |
| **Farming** | FSM + Triggers | Simple cycle with events | Harvest wheat |
| **Crafting** | HTN + Rules | Hierarchical recipes | Craft iron sword |
| **Exploration** | Utility + BT | Utility selects locations, BT navigates | Find village |
| **Trading** | Utility AI | Score trades by value | Trade with villager |
| **Multi-Agent** | HTN + Coordination | Decompose, distribute tasks | Build together |

### 14.3 Performance Comparison

| Architecture | Avg CPU (per agent) | Memory | Scalability |
|--------------|---------------------|--------|-------------|
| **Rule-Based** | ~0.01 ms | Low | Excellent |
| **FSM** | ~0.02 ms | Low | Excellent |
| **HFSM** | ~0.03 ms | Medium | Very Good |
| **Behavior Tree** | ~0.1-0.5 ms | Medium | Good |
| **Utility AI** | ~0.2-1.0 ms | Medium | Good |
| **GOAP** | ~1-10 ms | High | Medium |
| **HTN** | ~0.5-5 ms | High | Good |

### 14.4 Development Effort

| Architecture | Design | Implementation | Testing | Maintenance |
|--------------|--------|----------------|---------|-------------|
| **Rule-Based** | Low | Low | Low | Low |
| **FSM** | Low | Low | Low | Medium |
| **HFSM** | Medium | Medium | Medium | Medium |
| **Behavior Tree** | Medium | Medium-High | Medium | Low |
| **Utility AI** | Medium-High | Medium | Medium-High | Medium |
| **GOAP** | High | High | High | Medium |
| **HTN** | High | High | High | Low-Medium |

---

## 15. Conclusion

### 15.1 Key Takeaways

1. **No silver bullet**: Each architecture has strengths and weaknesses. Choose based on your specific needs.

2. **Start simple**: Begin with FSM or rules, evolve to BT/HTN as complexity grows.

3. **Hybrid is common**: Modern games often combine multiple architectures.

4. **Data-driven**: Externalize behavior for faster iteration.

5. **Performance matters**: Consider agent count and tick frequency when choosing.

6. **Tool support**: Behavior trees have excellent visual editors.

7. **Team skills**: Match architecture to team experience.

### 15.2 Minecraft-Specific Recommendations

For Steve AI (MineWright):

1. **Keep existing FSM**: `AgentStateMachine` is well-designed
2. **Add Utility scoring**: For task prioritization
3. **Implement HTN**: For common tasks (building, mining)
4. **Keep Plugin Architecture**: `ActionRegistry` is excellent
5. **LLM as fallback**: Use HTN for known tasks, LLM for novel ones

### 15.3 Future Trends

- **AI-augmented tools**: LLMs help configure classical architectures
- **Hybrid systems**: Classical + LLM becoming standard
- **Performance optimization**: More efficient BT and utility implementations
- **Visual tools**: Better AI editors for non-programmers
- **Multi-agent**: HTN and BT for coordination

### 15.4 Final Decision Matrix

```
Simplicity      │ Rule-Based → FSM → HFSM
Reactivity      │ FSM → Behavior Tree
Predictability  │ FSM → HFSM → HTN
Flexibility     │ Utility → GOAP
Performance     │ Rule-Based → FSM → HFSM → BT → HTN → Utility → GOAP
Team Skills     │ FSM → BT → Utility → HTN → GOAP
```

---

## References

### Academic Papers

1. Orkin, J. (2004). "Applying Goal-Oriented Action Planning to Games"
2. Champandard, A. J. (2007). "Behavior Tree AI and Gameplay Programming in 'Kung Fu Panda'"
3. Hernández, J. E. (2017). "HTN Planning for Games"

### Game AI Books

1. Millington, I. (2019). "AI for Games, 3rd Edition"
2. Rabin, S. (2015). "Game AI Pro" series
3. Champandard, A. J., & Díaz-Guerra, D. (2021). "Programming Game AI by Example"

### Online Resources

1. **Game AI Pro**: articles on all architectures
2. **GDC Vault**: AI talks from industry veterans
3. **aigamedev.com**: Research and case studies
4. **Recurse Center**: Behavior tree implementations
5. **Unreal Engine Documentation**: Behavior tree and utility AI guides

### Game Case Studies

1. **F.E.A.R.** (2005) - GOAP pioneer
2. **Halo series** - Behavior trees
3. **The Sims series** - Utility AI
4. **Horizon Zero Dawn** - HTN for robot behaviors
5. **Alien: Isolation** - Hybrid BT + Utility

---

**End of Chapter 6**

*Next chapters will cover implementation details, testing strategies, and LLM integration patterns.*