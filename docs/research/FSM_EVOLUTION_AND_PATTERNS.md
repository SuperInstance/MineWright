# Finite State Machines in Game AI: Evolution and Patterns

**Research Document**
**Date:** 2026-02-28
**Project:** Steve AI - "Cursor for Minecraft"
**Author:** Orchestrator Research Team

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Classic FSM Patterns](#2-classic-fsm-patterns)
3. [FSM Implementation Patterns](#3-fsm-implementation-patterns)
4. [FSM Limitations and Solutions](#4-fsm-limitations-and-solutions)
5. [FSM + LLM Integration](#5-fsm--llm-integration)
6. [Case Study: Steve AI Implementation](#6-case-study-steve-ai-implementation)
7. [Best Practices and Recommendations](#7-best-practices-and-recommendations)
8. [References and Further Reading](#8-references-and-further-reading)

---

## 1. Introduction

Finite State Machines (FSMs) have been a cornerstone of game AI development for decades. From the early days of arcade games to modern AAA titles, FSMs provide a simple yet powerful way to model agent behavior. This document explores the evolution of FSM patterns in game AI, their implementation strategies, limitations, and modern integration with Large Language Models (LLMs).

### What is a Finite State Machine?

A Finite State Machine is a mathematical model of computation defined as:

```
FSM = (Q, Σ, δ, q₀, F)
```

Where:
- **Q** = finite set of states
- **Σ** = input symbols (events/conditions)
- **δ** = transition function (Q × Σ → Q)
- **q₀** = initial state
- **F** = set of final/accepting states

In game AI terms:
- **States** = behaviors (Patrol, Chase, Attack, Flee)
- **Events** = game conditions (player spotted, low health, enemy dead)
- **Transitions** = rules for changing behavior

### Why FSMs Dominated Game AI

| Advantage | Explanation |
|-----------|-------------|
| **Simplicity** | Easy to understand, visualize, and debug |
| **Predictability** | Deterministic behavior is easier to test |
| **Performance** | Low overhead, O(1) state lookup |
| **Communication** | Non-programmers can understand state diagrams |
| **Tooling** | Excellent visual editor support |

---

## 2. Classic FSM Patterns

### 2.1 Standard Game AI States

Classic game AI typically uses these fundamental states:

#### Combat AI States
```java
public enum CombatState {
    IDLE,           // Waiting, looking around
    PATROL,         // Following predetermined path
    ALERT,          // Suspicious, investigating
    CHASE,          // Pursuing detected target
    ATTACK,         // Engaging target in combat
    FLEE,           // Running away (low health/fear)
    DEAD,           // Dead/inactive
    STUNNED         // Temporarily incapacitated
}
```

#### Platformer Character States
```java
public enum PlatformerState {
    IDLE,
    WALKING,
    RUNNING,
    JUMPING,
    FALLING,
    LANDING,
    CROUCHING,
    SLIDING,
    HANGING,        // On ledge
    CLIMBING,
    DASHING,
    ATTACKING,
    HURT,
    DYING
}
```

### 2.2 State Transition Conditions

Transitions are triggered by conditions checked each frame:

```java
// Classic guard AI transition logic
switch (currentState) {
    case PATROL:
        if (canSeePlayer()) {
            transitionTo(ALERT);
        }
        if (patrolComplete()) {
            transitionTo(IDLE);
        }
        break;

    case ALERT:
        if (canSeePlayer()) {
            transitionTo(CHASE);
        } else if (alertTimeout()) {
            transitionTo(PATROL);
        }
        break;

    case CHASE:
        if (inAttackRange()) {
            transitionTo(ATTACK);
        } else if (lostPlayer()) {
            transitionTo(ALERT);
        } else if (lowHealth()) {
            transitionTo(FLEE);
        }
        break;

    case ATTACK:
        if (targetDead()) {
            transitionTo(PATROL);
        } else if (outOfRange()) {
            transitionTo(CHASE);
        } else if (lowHealth()) {
            transitionTo(FLEE);
        }
        break;

    case FLEE:
        if (healthRestored() || reachedSafeZone()) {
            transitionTo(PATROL);
        }
        break;
}
```

### 2.3 State Entry/Exit Actions

Each state can define actions when entering or exiting:

```java
public interface State {
    void onEnter();
    void onUpdate(float deltaTime);
    void onExit();
}

public class ChaseState implements State {
    private Entity target;

    @Override
    public void onEnter() {
        // Play alert sound
        audio.playSound("alert");
        // Update UI to show pursuit
        ui.showAlertIcon();
        // Store target reference
        target = sensor.getNearestEnemy();
    }

    @Override
    public void onUpdate(float deltaTime) {
        if (target != null) {
            movement.moveTowards(target.getPosition());
            weapon.aimAt(target);
        }
    }

    @Override
    public void onExit() {
        // Clear target
        target = null;
        // Hide alert UI
        ui.hideAlertIcon();
    }
}
```

### 2.4 Hierarchical FSMs (HFSM)

Hierarchical FSMs allow states to contain sub-states, reducing code duplication:

``                            ┌─────────────────────┐
                            │    COMBAT_ROOT      │
                            │  (can transition    │
                            │   to any attack)    │
                            └──────────┬──────────┘
                                       │
            ┌──────────────────────────┼──────────────────────────┐
            │                          │                          │
    ┌───────▼───────┐        ┌────────▼────────┐        ┌────────▼────────┐
    │   MELEE_COMBAT │        │  RANGED_COMBAT  │        │  SUPPORT_COMBAT │
    │               │        │                 │        │                 │
    │ ┌───────────┐ │        │ ┌─────────────┐ │        │ ┌─────────────┐ │
    │ │   LIGHT   │ │        │ │    SNIPING  │ │        │ │   HEALING   │ │
    │ │   ATTACK  │ │        │ │             │ │        │ │             │ │
    │ └───────────┘ │        │ └─────────────┘ │        │ └─────────────┘ │
    │ ┌───────────┐ │        │ ┌─────────────┐ │        │ ┌─────────────┐ │
    │ │   HEAVY   │ │        │ │   SUPPRESS  │ │        │ │   BUFFING   │ │
    │ │   ATTACK  │ │        │ │             │ │        │ │             │ │
    │ └───────────┘ │        │ └─────────────┘ │        │ └─────────────┘ │
    │ ┌───────────┐ │        │ ┌─────────────┐ │        │ ┌─────────────┐ │
    │ │   GRAPPLE │ │        │ │   GRENADE   │ │        │ │   REVIVING  │ │
    │ └───────────┘ │        │ └─────────────┘ │        │ └─────────────┘ │
    └───────────────┘        └─────────────────┘        └─────────────────┘
```

**Example Implementation:**

```java
// Parent state
public abstract class CombatState implements State {
    protected void checkCombatTransitions() {
        if (shouldFlee()) {
            transitionTo(new FleeState());
        } else if (shouldSwitchWeapon()) {
            switchToAppropriateCombatStyle();
        }
    }
}

// Child states inherit shared behavior
public class MeleeLightAttackState extends CombatState {
    @Override
    public void onUpdate(float deltaTime) {
        performLightAttack();
        checkCombatTransitions(); // Inherited from parent
    }
}

public class RangedSnipingState extends CombatState {
    @Override
    public void onUpdate(float deltaTime) {
        aimAtTarget();
        fireWhenReady();
        checkCombatTransitions(); // Inherited from parent
    }
}
```

**Benefits of HFSM:**
- Shared behavior at parent level
- Easier to add new attack types
- Cleaner transitions between combat styles
- Reduces state explosion

---

## 3. FSM Implementation Patterns

### 3.1 Switch/Case Implementation (Simple FSM)

The most basic approach using enums and switch statements:

```java
public enum State {
    IDLE, PATROL, CHASE, ATTACK, FLEE
}

public class SimpleEnemyAI {
    private State currentState = State.IDLE;

    public void update(float deltaTime) {
        switch (currentState) {
            case IDLE:
                handleIdle(deltaTime);
                break;
            case PATROL:
                handlePatrol(deltaTime);
                break;
            case CHASE:
                handleChase(deltaTime);
                break;
            case ATTACK:
                handleAttack(deltaTime);
                break;
            case FLEE:
                handleFlee(deltaTime);
                break;
        }
    }

    private void handleIdle(float deltaTime) {
        if (seesPlayer()) {
            currentState = State.CHASE;
        } else if (shouldPatrol()) {
            currentState = State.PATROL;
        }
    }

    private void handlePatrol(float deltaTime) {
        followPatrolPath();
        if (seesPlayer()) {
            currentState = State.CHASE;
        }
    }

    // ... other handlers
}
```

**Pros:**
- Quick to implement
- Low overhead
- Easy to understand for simple cases

**Cons:**
- Becomes unwieldy with many states
- Hard to maintain
- Violates Open/Closed Principle
- Difficult to add entry/exit actions

### 3.2 State Pattern (OOP Approach)

Using polymorphism with state classes:

```java
// State interface
public interface State {
    void onEnter(Entity entity);
    void onUpdate(Entity entity, float deltaTime);
    void onExit(Entity entity);
}

// Context class
public class StateMachine {
    private Map<String, State> states = new HashMap<>();
    private State currentState;

    public void addState(String name, State state) {
        states.put(name, state);
    }

    public void changeState(String stateName, Entity entity) {
        State newState = states.get(stateName);
        if (newState == null) return;

        if (currentState != null) {
            currentState.onExit(entity);
        }

        currentState = newState;
        currentState.onEnter(entity);
    }

    public void update(Entity entity, float deltaTime) {
        if (currentState != null) {
            currentState.onUpdate(entity, deltaTime);
        }
    }
}

// Concrete state implementation
public class PatrolState implements State {
    private List<Vector3> waypoints;
    private int currentWaypoint = 0;

    @Override
    public void onEnter(Entity entity) {
        entity.setAnimation("walk");
        entity.playSound("patrol_start");
    }

    @Override
    public void onUpdate(Entity entity, float deltaTime) {
        Vector3 target = waypoints.get(currentWaypoint);
        entity.moveTo(target, deltaTime);

        if (entity.reachedDestination()) {
            currentWaypoint = (currentWaypoint + 1) % waypoints.size();
        }

        if (entity.canSeePlayer()) {
            entity.getStateMachine().changeState("chase", entity);
        }
    }

    @Override
    public void onExit(Entity entity) {
        entity.stopMovement();
    }
}
```

**Pros:**
- Clean separation of concerns
- Easy to add new states
- Supports complex state-specific data
- Testable in isolation
- Follows SOLID principles

**Cons:**
- More boilerplate code
- Slight performance overhead from virtual calls
- Higher initial complexity

### 3.3 Table-Driven FSM

Using data tables to define states and transitions:

```java
public class StateTransitionTable {
    public static class Transition {
        String fromState;
        String toState;
        Predicate<GameContext> condition;
        Consumer<GameContext> action;

        Transition(String from, String to, Predicate<GameContext> cond, Consumer<GameContext> action) {
            this.fromState = from;
            this.toState = to;
            this.condition = cond;
            this.action = action;
        }
    }

    private List<Transition> transitions = new ArrayList<>();
    private String currentState;

    public void addTransition(String from, String to, Predicate<GameContext> condition, Consumer<GameContext> action) {
        transitions.add(new Transition(from, to, condition, action));
    }

    public void update(GameContext context) {
        for (Transition t : transitions) {
            if (t.fromState.equals(currentState) && t.condition.test(context)) {
                if (t.action != null) {
                    t.action.accept(context);
                }
                currentState = t.toState;
                return;
            }
        }
    }
}

// Usage
StateTransitionTable fsm = new StateTransitionTable();
fsm.addTransition(
    "patrol",
    "chase",
    ctx -> ctx.canSeePlayer(),
    ctx -> ctx.playSound("alert")
);
fsm.addTransition(
    "chase",
    "attack",
    ctx -> ctx.isInRange(ctx.PLAYER_ATTACK_RANGE),
    null
);
fsm.addTransition(
    "attack",
    "flee",
    ctx -> ctx.getHealth() < 30,
    ctx -> ctx.playSound("retreat")
);
```

### 3.4 Visual State Machine Editors

Modern game engines provide visual FSM editors:

#### Unity Tools

| Tool | Description |
|------|-------------|
| **PlayMaker** | Popular visual scripting with FSM |
| **State Machine 2** | Part of Game Creator 2 |
| **FlowCanvas** | Node-based visual scripting |
| **Animator Controller** | Built-in animation state machine |

#### Unreal Engine

| Feature | Description |
|---------|-------------|
| **Blueprints** | Visual scripting with state machine support |
| **Animation Blueprints** | State machines for animation |
| **Behavior Trees** | Built-in AI behavior tree system |
| **State Tree** | New hierarchical state machine system (UE 5.1+) |

**Example: Unreal State Tree**

```
State Tree: EnemyAI
├── Root State
    ├── Selector (Parallel)
    ├── Movement State Machine
    │   ├── Idle
    │   ├── Patrol
    │   ├── Chase
    │   └── Flee
    ├── Combat State Machine
    │   ├── NotFighting
    │   ├── MeleeAttack
    │   └── RangedAttack
    └── Animation State Machine
        ├── Locomotion
        ├── Combat
        └── Emotes
```

---

## 4. FSM Limitations and Solutions

### 4.1 The State Explosion Problem

As AI complexity grows, FSMs suffer from exponential growth:

**Example: Simple Guard AI**

| Variables | Values | State Combinations |
|-----------|--------|-------------------|
| Health | High, Low, Critical | 3 |
| Ammo | Full, Low, Empty | 3 |
| Enemy Seen | Yes, No | 2 |
| Weapon | Melee, Ranged | 2 |
| Cover Available | Yes, No | 2 |
| **Total** | | **3 × 3 × 2 × 2 × 2 = 72 states** |

With 10 binary conditions: **2^10 = 1,024 states**

**Transitions:** With 72 states, you could need up to **72 × 72 = 5,184 transitions**

### 4.2 How Behavior Trees Solve This

Behavior Trees eliminate explicit transitions, using hierarchical control flow instead:

**FSM Approach (Explicit Transitions):**
```java
// FSM requires defining every transition
switch(currentState) {
    case PATROL:
        if (seesEnemy()) transitionTo(CHASE);
        if (hearsNoise()) transitionTo(INVESTIGATE);
        if (lowHealth()) transitionTo(FLEE);
        if (nightTime()) transitionTo(CAMP);
        if (rainy()) transitionTo(FIND_SHELTER);
        // ... more transitions
        break;
    case CHASE:
        if (lostEnemy()) transitionTo(PATROL);
        if (inRange()) transitionTo(ATTACK);
        if (lowHealth()) transitionTo(FLEE);
        if (outnumbered()) transitionTo(CALL_BACKUP);
        // ... more transitions
        break;
    // ... many more states
}
```

**Behavior Tree Approach (Implicit Flow):**
```
Root (Selector)
├── Combat (Sequence)
│   ├── Condition: Is in combat?
│   ├── Condition: Has line of sight?
│   ├── Selector
│   │   ├── Sequence: Melee Attack
│   │   │   ├── Condition: In melee range
│   │   │   └── Action: Attack
│   │   └── Sequence: Ranged Attack
│   │       ├── Condition: Has ammo
│   │       └── Action: Shoot
│   └── Condition: Low health?
│       └── Action: Flee
├── Patrol (Sequence)
│   ├── Condition: Not in combat
│   └── Action: Follow waypoint path
└── Idle
    └── Action: Wait
```

**Comparison:**

| Aspect | FSM | Behavior Tree |
|--------|-----|---------------|
| **Transitions** | Explicit, O(n²) | Implicit, hierarchical |
| **Adding Behavior** | May require modifying multiple states | Add node/subtree |
| **Reusability** | Hard | Easy (subtrees) |
| **Scalability** | Exponential | Linear |
| **Visualization** | Complex graph | Clear tree |
| **Modularity** | Low | High |

### 4.3 Pushdown Automata (FSM with Stack)

Pushdown Automata solve the "return to previous state" problem:

**Use Case:** Menu navigation, pause screens, cutscenes

```java
public class PushdownAutomaton {
    private Stack<State> stateStack = new Stack<>();

    public void pushState(State newState) {
        if (!stateStack.isEmpty()) {
            stateStack.peek().onPause();
        }
        stateStack.push(newState);
        newState.onEnter();
    }

    public void popState() {
        if (stateStack.isEmpty()) return;

        State current = stateStack.pop();
        current.onExit();

        if (!stateStack.isEmpty()) {
            State previous = stateStack.peek();
            previous.onResume();
        }
    }

    public void update(float deltaTime) {
        if (!stateStack.isEmpty()) {
            stateStack.peek().onUpdate(deltaTime);
        }
    }
}

// Usage example
PushdownAutomaton gameFSM = new PushdownAutomaton();

// Normal gameplay
gameFSM.pushState(new GameplayState());

// Player opens menu - gameplay paused
gameFSM.pushState(new MenuState());

// Player opens settings - menu paused
gameFSM.pushState(new SettingsState());

// Close settings - return to menu
gameFSM.popState();

// Close menu - resume gameplay
gameFSM.popState();
```

**Real Example:** Complex menu systems

```
Stack (top at right):
[Gameplay] [MainMenu] [OptionsMenu] [KeyBindings] [ConfirmDialog]

Closing ConfirmDialog → [Gameplay] [MainMenu] [OptionsMenu] [KeyBindings]
Closing KeyBindings → [Gameplay] [MainMenu] [OptionsMenu]
Closing OptionsMenu → [Gameplay] [MainMenu]
Closing MainMenu → [Gameplay]
```

### 4.4 Concurrent State Machines

Allow multiple independent state machines to run simultaneously:

**Use Case:** Character animation + game logic + weapon state

```java
public class ConcurrentStateMachine {
    private Map<String, StateMachine> machines = new HashMap<>();

    public void addMachine(String name, StateMachine machine) {
        machines.put(name, machine);
    }

    public void update(float deltaTime) {
        for (StateMachine machine : machines.values()) {
            machine.update(deltaTime);
        }
    }

    public StateMachine getMachine(String name) {
        return machines.get(name);
    }
}

// Usage
ConcurrentStateMachine characterAI = new ConcurrentStateMachine();

// Movement state machine
StateMachine movementFSM = new StateMachine();
movementFSM.addState("idle", new IdleState());
movementFSM.addState("walk", new WalkState());
movementFSM.addState("run", new RunState());
movementFSM.addState("jump", new JumpState());

// Combat state machine
StateMachine combatFSM = new StateMachine();
combatFSM.addState("peaceful", new PeacefulState());
combatFSM.addState("aiming", new AimingState());
combatFSM.addState("firing", new FiringState());
combatFSM.addState("reloading", new ReloadingState());

// Animation state machine
StateMachine animationFSM = new StateMachine();
animationFSM.addState("upper_idle", new UpperIdleState());
animationFSM.addState("upper_attack", new UpperAttackState());
animationFSM.addState("lower_idle", new LowerIdleState());
animationFSM.addState("lower_walk", new LowerWalkState());

characterAI.addMachine("movement", movementFSM);
characterAI.addMachine("combat", combatFSM);
characterAI.addMachine("animation", animationFSM);

// Now can walk while aiming, run while reloading, etc.
```

**Benefits:**
- Separation of concerns
- Independent behavior systems
- More realistic character behavior
- Easier to maintain

**Challenges:**
- Cross-machine coordination
- Debugging complexity
- Conflicting actions (e.g., can't reload while stunned)

---

## 5. FSM + LLM Integration

### 5.1 How LLMs Can Dynamically Create States

Traditional FSMs have fixed states defined at compile-time. LLMs enable runtime state generation:

**Traditional FSM:**
```java
// States hardcoded at compile time
enum State { PATROL, CHASE, ATTACK, FLEE }
```

**LLM-Enhanced FSM:**
```java
public class DynamicStateMachine {
    private Map<String, DynamicState> states = new HashMap<>();
    private OpenAIClient llmClient;

    public async void generateStates(String taskDescription) {
        // Ask LLM to generate appropriate states
        String prompt = """
            Given the task: %s

            Generate a list of states needed to accomplish this task.
            For each state, provide:
            - State name
            - Entry conditions
            - Exit conditions
            - Behavior description

            Return as JSON.
            """.formatted(taskDescription);

        String response = await llmClient.complete(prompt);
        StateDefinition[] definitions = parseStates(response);

        for (StateDefinition def : definitions) {
            DynamicState state = createRuntimeState(def);
            states.put(def.name, state);
        }
    }
}
```

**Example LLM Response:**

```json
{
  "states": [
    {
      "name": "GATHER_RESOURCES",
      "entry_condition": "need_resources && has_tool",
      "exit_condition": "inventory_full || task_complete",
      "behavior": "Locate nearest resource node, move to it, extract resource"
    },
    {
      "name": "CRAFT_ITEM",
      "entry_condition": "has_required_materials && near_crafting_table",
      "exit_condition": "item_crafted || crafting_failed",
      "behavior": "Place materials in crafting grid, execute recipe"
    },
    {
      "name": "TRADE_WITH_VILLAGER",
      "entry_condition": "has_trade_goods && found_villager",
      "exit_condition": "trade_complete || villager_gone",
      "behavior": "Open trading interface, select trade, confirm"
    }
  ]
}
```

### 5.2 LLM as State Transition Decider

Instead of hardcoded transition rules, LLM can evaluate context and decide transitions:

```java
public class LLMTransitionDecider {
    private OpenAIClient llm;
    private ConversationMemory memory;

    public StateTransition decideTransition(
        String currentState,
        GameContext context,
        State[] possibleStates
    ) {
        // Build context from game state
        String situation = buildSituationReport(context);

        String prompt = """
            Current state: %s
            Situation: %s

            Possible transitions: %s

            Based on the current situation, which state should we transition to?
            Consider:
            1. Current objective
            2. Immediate threats
            3. Resource availability
            4. Recent history

            Return state name and reasoning (one sentence).
            """.formatted(
                currentState,
                situation,
                Arrays.toString(possibleStates)
            );

        String response = llm.complete(prompt, memory);
        return parseTransition(response);
    }

    private String buildSituationReport(GameContext ctx) {
        return """
            Health: %.1f/%.1f
            Hunger: %.1f/%.1f
            Nearby enemies: %s
            Current objective: %s
            Recent events: %s
            """.formatted(
                ctx.getHealth(), ctx.getMaxHealth(),
                ctx.getHunger(), ctx.getMaxHunger(),
                ctx.getNearbyEnemies(),
                ctx.getCurrentObjective(),
                memory.getRecentEvents(5)
            );
    }
}
```

### 5.3 Natural Language State Definitions

Define states in natural language, let LLM translate to code:

**User Prompt:**
```
"Create a guard AI that patrols during day, guards the entrance at night,
 and chases intruders but returns to post if they escape"
```

**LLM Generates:**

```java
// LLM-generated code
public class GuardAI {
    enum State {
        DAY_PATROL,
        NIGHT_GUARD,
        CHASE_INTRUDER,
        RETURN_TO_POST,
        ALERT_ALLIES
    }

    private State currentState = State.DAY_PATROL;

    public void update() {
        boolean isNight = world.getTimeOfDay() > 0.7;
        boolean hasIntruder = sensors.detectEnemy();
        boolean intruderEscaped = !hasIntruder &&
                                 currentState == State.CHASE_INTRUDER &&
                                 timeSinceLastSighting > 5000;

        switch(currentState) {
            case DAY_PATROL:
                if (isNight) {
                    transitionTo(State.NIGHT_GUARD, "Night fell");
                } else if (hasIntruder) {
                    transitionTo(State.CHASE_INTRUDER, "Intruder detected");
                } else {
                    patrolAlongRoute();
                }
                break;

            case NIGHT_GUARD:
                if (!isNight) {
                    transitionTo(State.DAY_PATROL, "Day broke");
                } else if (hasIntruder) {
                    transitionTo(State.CHASE_INTRUDER, "Intruder detected");
                } else {
                    guardEntrance();
                }
                break;

            case CHASE_INTRUDER:
                if (intruderEscaped) {
                    transitionTo(State.RETURN_TO_POST, "Target lost");
                } else {
                    chaseTarget();
                    if (hasAlliesNearby()) {
                        alertAllies();
                    }
                }
                break;

            case RETURN_TO_POST:
                if (hasIntruder) {
                    transitionTo(State.CHASE_INTRUDER, "Rediscovered intruder");
                } else if (reachedPost()) {
                    transitionTo(isNight ? State.NIGHT_GUARD : State.DAY_PATROL,
                                 "Returned to post");
                } else {
                    moveTo(post);
                }
                break;
        }
    }
}
```

### 5.4 Hybrid Architecture: FSM + LLM

The best approach combines structured FSM with LLM flexibility:

```
┌─────────────────────────────────────────────────────────────┐
│                     High-Level LLM Planning                 │
│  (Generates task sequences, handles complex reasoning)      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ Generates sub-goals
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    Hierarchical FSM                         │
│  (Manages execution states, ensures reliability)           │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │  IDLE    │→ │ PLANNING │→ │EXECUTING │→ │COMPLETED │   │
│  └──────────┘  └──────────┘  └─────┬────┘  └──────────┘   │
│                                    │                        │
│                    ┌───────────────┴───────────┐           │
│                    │  Action Queue (FSM)       │           │
│                    │  ┌────────────────────┐   │           │
│                    │  │ Move → Mine → Craft│   │           │
│                    │  └────────────────────┘   │           │
│                    └───────────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
                         │
                         │ Context & Results
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      Memory System                          │
│  (Stores experiences, learns from outcomes)                 │
└─────────────────────────────────────────────────────────────┘
```

**Implementation Example:**

```java
public class HybridAIController {
    private AgentStateMachine stateMachine;  // Structured FSM
    private TaskPlanner llmPlanner;          // LLM for planning
    private ActionExecutor actionExecutor;   // Executes actions
    private SteveMemory memory;              // Learning

    public void processCommand(String userCommand) {
        // FSM controls the flow
        if (!stateMachine.transitionTo(AgentState.PLANNING)) {
            log.warn("Cannot start planning, current state: {}",
                    stateMachine.getCurrentState());
            return;
        }

        // LLM generates the plan
        CompletableFuture<List<Task>> planFuture = llmPlanner.planTasksAsync(
            userCommand,
            memory.buildContext()
        );

        planFuture.thenAccept(tasks -> {
            // FSM validates state transition
            if (stateMachine.getCurrentState() == AgentState.PLANNING) {
                stateMachine.transitionTo(AgentState.EXECUTING,
                                        "Plan generated with " + tasks.size() + " tasks");
                actionExecutor.executeTasks(tasks);
            }
        }).exceptionally(ex -> {
            stateMachine.transitionTo(AgentState.FAILED, "Planning failed: " + ex.getMessage());
            return null;
        });
    }

    public void onTaskComplete(Task task) {
        memory.recordResult(task, true);

        // FSM decides next action
        if (actionExecutor.hasMoreTasks()) {
            actionExecutor.executeNextTask();
        } else {
            stateMachine.transitionTo(AgentState.COMPLETED, "All tasks finished");
            // Transition back to IDLE automatically
            stateMachine.transitionTo(AgentState.IDLE, "Ready for next command");
        }
    }

    public void onTaskFailed(Task task, String reason) {
        memory.recordResult(task, false);

        // FSM handles failure - can retry or abort
        if (task.getRetryCount() < MAX_RETRIES) {
            actionExecutor.retryTask(task);
        } else {
            stateMachine.transitionTo(AgentState.FAILED,
                                    "Task failed after retries: " + reason);
        }
    }
}
```

---

## 6. Case Study: Steve AI Implementation

### 6.1 Project Overview

**Steve AI** is "Cursor for Minecraft" - an autonomous AI agent that plays Minecraft with users. The project uses a hybrid FSM + LLM architecture.

### 6.2 State Machine Design

Steve AI implements a clean FSM pattern in `AgentStateMachine.java`:

**States Defined:**
```java
public enum AgentState {
    IDLE,       // Waiting for commands
    PLANNING,   // Processing with AI
    EXECUTING,  // Performing actions
    PAUSED,     // Temporarily suspended
    COMPLETED,  // Success state
    FAILED      // Error state
}
```

**State Transition Diagram:**
```
                    ┌─────────────────────────────────────┐
                    │                                     │
                    ▼                                     │
  ┌──────────┐   ┌──────────┐   ┌───────────┐   ┌───────────┐
  │   IDLE   │──▶│ PLANNING │──▶│ EXECUTING │──▶│ COMPLETED │
  └──────────┘   └──────────┘   └───────────┘   └───────────┘
       ▲              │              │               │
       │              │              │               │
       │              ▼              ▼               │
       │         ┌──────────┐   ┌──────────┐        │
       │         │  FAILED  │   │  PAUSED  │        │
       │         └──────────┘   └──────────┘        │
       │              │              │               │
       └──────────────┴──────────────┴───────────────┘
```

### 6.3 Transition Validation

The state machine validates transitions to prevent invalid state changes:

```java
private static final Map<AgentState, Set<AgentState>> VALID_TRANSITIONS;

static {
    VALID_TRANSITIONS = new EnumMap<>(AgentState.class);

    // IDLE can go to PLANNING (new command)
    VALID_TRANSITIONS.put(AgentState.IDLE,
        EnumSet.of(AgentState.PLANNING));

    // PLANNING can go to EXECUTING (success) or FAILED (error)
    VALID_TRANSITIONS.put(AgentState.PLANNING,
        EnumSet.of(AgentState.EXECUTING, AgentState.FAILED, AgentState.IDLE));

    // EXECUTING can complete, fail, or pause
    VALID_TRANSITIONS.put(AgentState.EXECUTING,
        EnumSet.of(AgentState.COMPLETED, AgentState.FAILED, AgentState.PAUSED));

    // PAUSED can resume or cancel
    VALID_TRANSITIONS.put(AgentState.PAUSED,
        EnumSet.of(AgentState.EXECUTING, AgentState.IDLE));

    // COMPLETED goes back to IDLE
    VALID_TRANSITIONS.put(AgentState.COMPLETED,
        EnumSet.of(AgentState.IDLE));

    // FAILED can go back to IDLE (reset)
    VALID_TRANSITIONS.put(AgentState.FAILED,
        EnumSet.of(AgentState.IDLE));
}
```

### 6.4 Thread Safety

The implementation uses `AtomicReference` for thread-safe state updates:

```java
public boolean transitionTo(AgentState targetState, String reason) {
    AgentState fromState = currentState.get();

    // Check if transition is valid
    if (!canTransitionTo(targetState)) {
        LOGGER.warn("[{}] Invalid state transition: {} → {}",
            agentId, fromState, targetState);
        return false;
    }

    // Atomic compare-and-set for thread safety
    if (currentState.compareAndSet(fromState, targetState)) {
        LOGGER.info("[{}] State transition: {} → {} (reason: {})",
            agentId, fromState, targetState, reason);

        // Publish event
        if (eventBus != null) {
            eventBus.publish(new StateTransitionEvent(
                agentId, fromState, targetState, reason));
        }
        return true;
    }
    return false;
}
```

### 6.5 Event Bus Integration

State transitions are published as events for decoupled observation:

```java
public class StateTransitionEvent {
    private final String agentId;
    private final AgentState fromState;
    private final AgentState toState;
    private final String reason;

    // Other components can listen to state changes
    // Example: Update UI, log analytics, trigger actions
}
```

### 6.6 LLM Integration

The FSM coordinates with LLM-based planning:

```java
public void processCommand(String command) {
    // FSM transition
    if (!stateMachine.transitionTo(AgentState.PLANNING)) {
        return;
    }

    // Async LLM call (non-blocking)
    CompletableFuture<List<Task>> plan = llmClient.planTasksAsync(
        command,
        memory.buildContext()
    );

    plan.thenAccept(tasks -> {
        // Transition to EXECUTING when plan is ready
        stateMachine.transitionTo(AgentState.EXECUTING, "Plan ready");
        executor.executeTasks(tasks);
    });
}
```

### 6.7 Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Enum-based states** | Type safety, compile-time validation |
| **Explicit transitions** | Prevents invalid state changes |
| **AtomicReference** | Thread-safe for async LLM calls |
| **Event publishing** | Decouples state machine from observers |
| **State helper methods** | `canAcceptCommands()`, `isActive()` for convenience |

---

## 7. Best Practices and Recommendations

### 7.1 When to Use FSM

**Use FSM when:**
- Behavior is well-defined and predictable
- Number of states is small (< 10)
- Performance is critical
- Team has FSM experience
- Behavior doesn't change frequently

**Avoid FSM when:**
- Behavior requires complex reasoning
- Many interacting states (> 15)
- Frequent behavior changes expected
- Need natural language understanding
- Learning from experience is required

### 7.2 FSM Design Guidelines

1. **Keep states simple and focused**
   ```java
   // Good: Single responsibility
   class PatrolState { }
   class ChaseState { }

   // Bad: Multiple responsibilities
   class PatrolAndChaseAndAttackState { }
   ```

2. **Use hierarchical FSMs for complexity**
   ```java
   CombatRoot
   ├── MeleeCombat
   │   ├── LightAttack
   │   ├── HeavyAttack
   │   └── Grapple
   └── RangedCombat
       ├── Sniping
       ├── Suppressing
       └── Grenades
   ```

3. **Prefer composition over inheritance**
   ```java
   // Attach behaviors dynamically
   stateMachine.addBehavior(new HealBehavior());
   stateMachine.addBehavior(new BuffBehavior());
   ```

4. **Centralize transition logic**
   ```java
   // Define all valid transitions in one place
   TRANSITIONS.put(State.A, [State.B, State.C]);
   ```

5. **Use events for cross-system communication**
   ```java
   eventBus.publish(new StateTransitionEvent(from, to));
   ```

### 7.3 Testing FSMs

```java
@Test
public void testValidTransitions() {
    AgentStateMachine sm = new AgentStateMachine(eventBus);

    assertTrue(sm.canTransitionTo(AgentState.PLANNING));
    assertTrue(sm.transitionTo(AgentState.PLANNING));
    assertEquals(AgentState.PLANNING, sm.getCurrentState());
}

@Test
public void testInvalidTransitions() {
    AgentStateMachine sm = new AgentStateMachine(eventBus);

    // Cannot go from IDLE to EXECUTING directly
    assertFalse(sm.canTransitionTo(AgentState.EXECUTING));
    assertFalse(sm.transitionTo(AgentState.EXECUTING));
}

@Test
public void testConcurrentTransitions() {
    AgentStateMachine sm = new AgentStateMachine(eventBus);

    // Simulate concurrent state changes
    CompletableFuture<Void> t1 = CompletableFuture.runAsync(() -> {
        sm.transitionTo(AgentState.PLANNING);
    });

    CompletableFuture<Void> t2 = CompletableFuture.runAsync(() -> {
        sm.transitionTo(AgentState.EXECUTING);
    });

    CompletableFuture.allOf(t1, t2).join();

    // Only one transition should succeed
    assertNotEquals(AgentState.IDLE, sm.getCurrentState());
}
```

### 7.4 Hybrid Architecture Recommendation

For complex AI projects like Steve AI:

```
┌───────────────────────────────────────────────────────────────┐
│                    Recommended Architecture                    │
│                                                                │
│  LLM Layer: High-level planning, natural language, reasoning  │
│      │                                                         │
│      ├─ Generate task sequences                               │
│      ├─ Handle edge cases                                     │
│      └─ Provide explanations                                  │
│                                                                │
│  FSM Layer: Execution control, state management, safety       │
│      │                                                         │
│      ├─ Validate state transitions                            │
│      ├─ Enforce execution rules                               │
│      └─ Handle timeouts and errors                            │
│                                                                │
│  Action Layer: Low-level game operations                      │
│      │                                                         │
│      ├─ Movement, mining, building                             │
│      ├─ Collision detection                                    │
│      └─ Animation control                                      │
│                                                                │
│  Memory Layer: Context, learning, persistence                 │
│                                                                │
│      ├─ Store execution history                                │
│      ├─ Build context for LLM                                  │
│      └─ Learn from outcomes                                   │
└───────────────────────────────────────────────────────────────┘
```

---

## 8. References and Further Reading

### Academic Papers

1. **"Statecharts: A Visual Formalism for Complex Systems"** - David Harel (1987)
   - Introduced hierarchical state machines

2. **"Behavior Trees: A New AI Architecture for Games"** - Isla (2005)
   - Comparison of FSM vs Behavior Trees

3. **"Planning and Acting using Concurrent Hierarchical Finite State Machines"** - Benson (1996)
   - Concurrent state machine research

### Books

1. **"Programming Game AI by Example"** - Mat Buckland
   - Classic FSM implementation patterns

2. **"Artificial Intelligence for Games"** - Ian Millington
   - Comprehensive game AI techniques

3. **"Game AI Pro"** Series - Various Authors
   - Modern game AI practices

### Online Resources

1. **Game Programming Patterns** - Robert Nystrom
   - [State Pattern](https://gpp.tkchu.me/state.html)

2. **Unity State Machine Basics**
   - [Unity Documentation](https://docs.unity.cn/2021.3/Documentation/Manual/StateMachineBasics.html)

3. **Unreal Engine State Tree**
   - [UE5 Documentation](https://docs.unrealengine.com/5.1/en-US/)

### Open Source Projects

1. **HFSM2** - High-performance C++ hierarchical state machine framework
2. **Boost.Statechart** - C++ state machine library
3. **Stateful** - Java state machine library

### Community Resources

1. **GDC Talks** - Game Developers Conference presentations on AI
2. **AI Game Dev** - Blog and forums for game AI
3. **r/gamedev** - Reddit community discussions

---

## Appendix A: Complete FSM Example

Complete implementation of a guard AI using the State Pattern:

```java
// State interface
public interface GuardState {
    void enter(Guard guard);
    void update(Guard guard, float deltaTime);
    void exit(Guard guard);
}

// Patrol state
public class PatrolState implements GuardState {
    private List<Vector3> waypoints;
    private int currentWaypoint = 0;

    public PatrolState(List<Vector3> waypoints) {
        this.waypoints = waypoints;
    }

    @Override
    public void enter(Guard guard) {
        guard.setAnimation("walk");
        guard.playSound("patrol_start");
    }

    @Override
    public void update(Guard guard, float deltaTime) {
        // Check for intruders
        if (guard.canSeePlayer()) {
            guard.changeState(new ChaseState(guard.getPlayer()));
            return;
        }

        // Follow patrol path
        Vector3 target = waypoints.get(currentWaypoint);
        guard.moveTo(target, deltaTime);

        if (guard.reachedDestination()) {
            currentWaypoint = (currentWaypoint + 1) % waypoints.size();
        }

        // Check time of day
        if (guard.isNightTime()) {
            guard.changeState(new GuardPostState(guard.getPost()));
        }
    }

    @Override
    public void exit(Guard guard) {
        guard.stopMovement();
    }
}

// Chase state
public class ChaseState implements GuardState {
    private Entity target;
    private float chaseTimeout = 10.0f;
    private float timeSinceLastSighting = 0.0f;

    public ChaseState(Entity target) {
        this.target = target;
    }

    @Override
    public void enter(Guard guard) {
        guard.setAnimation("run");
        guard.playSound("alert");
        guard.callAllies();
    }

    @Override
    public void update(Guard guard, float deltaTime) {
        // Check if target still visible
        if (guard.canSee(target)) {
            timeSinceLastSighting = 0.0f;
            guard.moveTo(target.getPosition(), deltaTime);

            // Attack if in range
            if (guard.isInRange(target, GUARD_ATTACK_RANGE)) {
                guard.changeState(new AttackState(target));
            }
        } else {
            timeSinceLastSighting += deltaTime;

            // Lost target
            if (timeSinceLastSighting > chaseTimeout) {
                guard.changeState(new SearchState(target.getLastKnownPosition()));
            }
        }

        // Check health
        if (guard.getHealthPercent() < 30) {
            guard.changeState(new FleeState());
        }
    }

    @Override
    public void exit(Guard guard) {
        target = null;
    }
}

// Attack state
public class AttackState implements GuardState {
    private Entity target;
    private float attackCooldown = 1.5f;
    private float timeSinceAttack = 0.0f;

    public AttackState(Entity target) {
        this.target = target;
    }

    @Override
    public void enter(Guard guard) {
        guard.setAnimation("combat");
        guard.aimAt(target);
    }

    @Override
    public void update(Guard guard, float deltaTime) {
        // Check if target dead
        if (!target.isAlive()) {
            guard.changeState(new PatrolState(guard.getWaypoints()));
            return;
        }

        // Maintain attack range
        float distance = guard.distanceTo(target);
        if (distance > GUARD_ATTACK_RANGE) {
            guard.changeState(new ChaseState(target));
            return;
        }

        // Attack when ready
        timeSinceAttack += deltaTime;
        if (timeSinceAttack >= attackCooldown) {
            guard.attack(target);
            timeSinceAttack = 0.0f;
        }

        // Face target
        guard.aimAt(target);
    }

    @Override
    public void exit(Guard guard) {
        guard.lowerWeapon();
    }
}

// Guard entity with state machine
public class Guard extends Entity {
    private GuardState currentState;
    private Map<String, List<Vector3>> patrolRoutes;

    public void changeState(GuardState newState) {
        if (currentState != null) {
            currentState.exit(this);
        }
        currentState = newState;
        currentState.enter(this);
    }

    @Override
    public void update(float deltaTime) {
        if (currentState != null) {
            currentState.update(this, deltaTime);
        }
    }
}
```

---

## Appendix B: FSM vs Behavior Trees Comparison

### Code Comparison: Same Behavior in FSM and BT

**FSM Implementation:**
```java
switch (currentState) {
    case PATROL:
        if (seeEnemy()) {
            currentState = CHASE;
        } else if (hearNoise()) {
            currentState = INVESTIGATE;
        } else {
            followPatrolRoute();
        }
        break;

    case CHASE:
        if (inAttackRange()) {
            currentState = ATTACK;
        } else if (lostEnemy()) {
            currentState = INVESTIGATE;
        } else if (lowHealth()) {
            currentState = FLEE;
        } else {
            chaseEnemy();
        }
        break;

    case ATTACK:
        if (!inAttackRange()) {
            currentState = CHASE;
        } else if (enemyDead()) {
            currentState = PATROL;
        } else if (lowHealth()) {
            currentState = FLEE;
        } else {
            attackEnemy();
        }
        break;

    case INVESTIGATE:
        if (seeEnemy()) {
            currentState = CHASE;
        } else if (investigationComplete()) {
            currentState = PATROL;
        } else {
            moveToNoiseSource();
        }
        break;

    case FLEE:
        if (safe()) {
            currentState = PATROL;
        } else {
            runToSafety();
        }
        break;
}
```

**Behavior Tree Implementation:**
```
Selector
├── Sequence (Emergency)
│   ├── Condition: Low health?
│   └── Action: Flee
├── Sequence (Combat)
│   ├── Condition: Can see enemy?
│   ├── Selector
│   │   ├── Sequence (Attack)
│   │   │   ├── Condition: In attack range?
│   │   │   └── Action: Attack
│   │   └── Action: Chase
│   └── Decorator: Repeat
├── Sequence (Investigate)
│   ├── Condition: Hear noise?
│   ├── Action: Move to noise source
│   └── Decorator: Run once
└── Action: Patrol
```

**Comparison:**

| Metric | FSM | Behavior Tree |
|--------|-----|---------------|
| Lines of Code | ~60 | ~15 (nodes) |
| Transitions to Define | 15 | 0 (implicit) |
| Adding New Behavior | Modify multiple states | Add new branch |
| Readability | Linear, but long | Hierarchical, clear |
| Debugging | Follow switch cases | Trace tree path |
| Reusability | Low | High (subtrees) |

---

**Document Version:** 1.0
**Last Updated:** 2026-02-28
**Maintained By:** Steve AI Development Team

---

**Sources:**
- [游戏AI程序设计实战 - Baidu Encyclopedia](https://baike.baidu.com/item/%E6%B8%B8%E6%88%8FAI%E7%A8%8B%E5%BA%8F%E8%AE%BE%E8%AE%A1%E5%AE%9E%E6%88%98/49935688)
- [游戏AI状态机与行为树 - CSDN](https://download.csdn.net/download/b3n4m5q6w7/92407104)
- [FSM、HFSM、BT对比 - CSDN](https://blog.csdn.net/larry_zeng1/article/details/80353132)
- [State Pattern - Game Programming Patterns](https://gpp.tkchu.me/state.html)
- [State Machine Transition Overview - ScienceDirect](https://www.sciencedirect.com/topics/computer-science/state-machine-transition)
- [Unity State Machine Basics - Unity Documentation](https://docs.unity.cn/2021.3/Documentation/Manual/StateMachineBasics.html)
- [Game AI State Machine - 百度文库](https://wk.baidu.com/view/a9b659c9142ded630b1c59eef8c75fbfc77d94c5)
- [Parallel State Machines - CSDN](https://m.blog.csdn.net/senlin2684/article/details/7973163)
- [LLM Agents History and Trends - CSDN](https://m.blog.csdn.net/weixin_30666943/article/details/99108255)
- [Pushdown Automaton - Game Programming Patterns](https://www.cnblogs.com/yusjoel/articles/7125018.html)
