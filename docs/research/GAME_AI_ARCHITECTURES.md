# Game AI Architectures Research Document

**Research Date:** 2025-02-27
**Project:** MineWright AI (MineWright Mod)
**Purpose:** Comprehensive analysis of modern game AI agent architectures for informing system design

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [GOAP (Goal-Oriented Action Planning)](#goap-goal-oriented-action-planning)
3. [HTN (Hierarchical Task Networks)]#htn-hierarchical-task-networks)
4. [Behavior Trees](#behavior-trees)
5. [Utility AI Systems](#utility-ai-systems)
6. [LLM-Powered Game Agents](#llm-powered-game-agents)
7. [Architecture Comparison](#architecture-comparison)
8. [Hybrid Approaches](#hybrid-approaches)
9. [Recommendations for MineWright AI](#recommendations-for-steve-ai)
10. [References](#references)

---

## Executive Summary

This document provides a comprehensive analysis of five major game AI architectures:

| Architecture | Type | Best For | Complexity | Performance |
|--------------|------|----------|------------|-------------|
| **GOAP** | Planning (Backward) | Emergent behavior, open worlds | High | Medium |
| **HTN** | Planning (Forward) | Strategic games, complex tasks | High | Good |
| **Behavior Trees** | Control | Predictable behavior, designer control | Low | Excellent |
| **Utility AI** | Scoring | Emotional AI, realistic NPCs | Medium | Good |
| **LLM Agents** | Neural | Natural language, complex reasoning | Very High | Variable |

**Key Finding:** Modern game development increasingly favors **hybrid approaches** that combine the strengths of multiple architectures. The MineWright AI project currently uses a custom LLM-driven planner with action execution, which could benefit from incorporating Utility AI concepts for emotional responses and Behavior Tree patterns for action composition.

---

## GOAP (Goal-Oriented Action Planning)

### Overview

**GOAP** is a planning-based AI system that works backward from goals to determine the sequence of actions needed to achieve them. It was first popularized in the game *F.E.A.R.* (2005) and has since been used in *The Sims* series, *Fallout 4*, and other open-world games.

### How It Works

```
1. Define Goal State (e.g., "HasWeapon: true", "EnemyDead: true")
2. Get Current World State (e.g., "HasWeapon: false", "HasAxe: true")
3. Use A* Search to Find Action Sequence:
   - Nodes: World states
   - Edges: Actions with preconditions/effects
   - Heuristic: Distance from goal state
4. Execute actions sequentially
5. Replan if world state changes
```

### Core Components

#### 1. World State
A collection of key-value pairs representing the current game state:

```java
Map<String, Object> worldState = new HashMap<>();
worldState.put("hasWeapon", false);
worldState.put("hasWood", 5);
worldState.put("enemyVisible", true);
worldState.put("health", 75);
```

#### 2. Actions
Actions define:
- **Preconditions:** What must be true to execute
- **Effects:** What changes after execution
- **Cost:** How expensive the action is

```java
public class AttackAction implements GOAPAction {
    @Override
    public boolean checkPreconditions(Map<String, Object> state) {
        return state.get("hasWeapon").equals(true) &&
               state.get("enemyVisible").equals(true);
    }

    @Override
    public Map<String, Object> getEffects() {
        Map<String, Object> effects = new HashMap<>();
        effects.put("enemyHealth", -10);
        return effects;
    }

    @Override
    public float getCost() {
        return 1.0f;
    }

    @Override
    public void execute() {
        // Attack logic
    }
}
```

#### 3. Goals
Goals define target states the agent wants to achieve:

```java
public class SurviveGoal implements GOAPGoal {
    @Override
    public Map<String, Object> getTargetState() {
        Map<String, Object> target = new HashMap<>();
        target.put("enemyDead", true);
        target.put("health", ">50");
        return target;
    }

    @Override
    public float getPriority() {
        return 10.0f; // High priority
    }
}
```

#### 4. Planner (A* Implementation)

```java
public class GOAPPlanner {
    public Queue<GOAPAction> plan(Map<String, Object> currentState,
                                   Map<String, Object> goalState,
                                   List<GOAPAction> availableActions) {
        // A* search implementation
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<Map<String, Object>> closedSet = new HashSet<>();

        Node startNode = new Node(currentState, null, null, 0, heuristic(currentState, goalState));
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (matchesGoal(current.state, goalState)) {
                return reconstructPath(current);
            }

            closedSet.add(current.state);

            for (GOAPAction action : availableActions) {
                if (action.checkPreconditions(current.state)) {
                    Map<String, Object> newState = applyAction(current.state, action);
                    if (!closedSet.contains(newState)) {
                        float newCost = current.gCost + action.getCost();
                        float hCost = heuristic(newState, goalState);
                        openSet.add(new Node(newState, action, current, newCost, hCost));
                    }
                }
            }
        }

        return null; // No plan found
    }

    private float heuristic(Map<String, Object> state, Map<String, Object> goal) {
        // Count mismatched keys
        return (float) goal.entrySet().stream()
            .filter(e -> !e.getValue().equals(state.get(e.getKey())))
            .count();
    }
}
```

### Strengths

1. **Emergent Behavior:** Can produce unexpected, intelligent solutions
2. **Flexible:** Easy to add new actions without restructuring
3. **Dynamic:** Adapts to changing world conditions
4. **Goal-Driven:** Agent behavior is purposeful and directed
5. **Reusable Actions:** Same actions can achieve different goals

### Weaknesses

1. **Computational Cost:** A* search can be expensive with large state spaces
2. **Unpredictable:** Hard to control for narrative scenarios
3. **Complex Debugging:** Difficult to understand why agent chose specific actions
4. **State Management:** Requires careful world state representation
5. **Replanning Overhead:** Must recalculate when world changes

### When to Use GOAP

| Scenario | Suitability |
|----------|-------------|
| Open-world survival games | Excellent |
| RTS games with many strategies | Excellent |
| Narrative-driven games | Poor |
| Simple enemy AI | Overkill |
| Real-time constraints | Careful optimization needed |

### Code Example: Complete GOAP System

```java
// Agent using GOAP
public class GOAPAgent {
    private Map<String, Object> worldState;
    private List<GOAPAction> availableActions;
    private List<GOAPGoal> goals;
    private GOAPPlanner planner;

    public void update() {
        // Update world state from perception
        updateWorldState();

        // Find highest priority goal
        GOAPGoal currentGoal = goals.stream()
            .max(Comparator.comparing(GOAPGoal::getPriority))
            .orElse(null);

        if (currentGoal != null) {
            // Plan actions to achieve goal
            Queue<GOAPAction> plan = planner.plan(
                worldState,
                currentGoal.getTargetState(),
                availableActions
            );

            if (plan != null && !plan.isEmpty()) {
                // Execute first action in plan
                GOAPAction nextAction = plan.peek();
                if (nextAction.checkPreconditions(worldState)) {
                    nextAction.execute();
                    worldState = applyEffects(worldState, nextAction.getEffects());
                    plan.poll();
                }
            }
        }
    }
}
```

### Real-World Implementations

1. **F.E.A.R.** (2005) - First major use of GOAP
2. **The Sims Series** - NPC needs and behaviors
3. **Fallout 4** - NPC survival behaviors
4. **Kenshi** - Squad AI and autonomy
5. **Embabel Agent Framework** - JVM-based GOAP implementation

---

## HTN (Hierarchical Task Networks)

### Overview

**HTN** is a forward-planning approach that decomposes high-level tasks into smaller subtasks recursively until reaching executable primitive actions. Unlike GOAP (backward planning), HTN plans from the current state toward the goal. It's used in *Horizon: Zero Dawn*, *Transformers: Fall of Cybertron*, and many strategy games.

### How It Works

```
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

### Core Components

#### 1. Tasks

Tasks come in two types:

```java
// Compound task (can be decomposed)
public interface CompoundTask {
    List<Method> getMethods(WorldState state);
}

// Primitive task (executable action)
public interface PrimitiveTask {
    boolean checkPreconditions(WorldState state);
    void execute(WorldState state);
    WorldState getEffects();
}
```

#### 2. Methods

Methods define alternative ways to accomplish a compound task:

```java
public class BuildHouseMethod implements Method {
    @Override
    public boolean isValid(WorldState state) {
        return state.hasResources() && state.hasLocation();
    }

    @Override
    public List<Task> getSubtasks() {
        return Arrays.asList(
            new CollectMaterialsTask(),
            new PrepareFoundationTask(),
            new ConstructWallsTask(),
            new AddRoofTask()
        );
    }
}
```

#### 3. HTN Planner

```java
public class HTNPlanner {
    public List<PrimitiveTask> plan(Task rootTask, WorldState initialState) {
        List<PrimitiveTask> plan = new ArrayList<>();
        List<Task> taskStack = new ArrayList<>();
        taskStack.add(rootTask);

        while (!taskStack.isEmpty()) {
            Task current = taskStack.remove(taskStack.size() - 1);

            if (current instanceof PrimitiveTask) {
                PrimitiveTask primitive = (PrimitiveTask) current;
                if (primitive.checkPreconditions(currentState)) {
                    plan.add(primitive);
                    currentState = primitive.getEffects();
                } else {
                    return null; // Plan failed
                }
            } else if (current instanceof CompoundTask) {
                CompoundTask compound = (CompoundTask) current;
                List<Method> methods = compound.getMethods(currentState);
                boolean methodFound = false;

                for (Method method : methods) {
                    if (method.isValid(currentState)) {
                        List<Task> subtasks = method.getSubtasks();
                        // Add subtasks in reverse order (stack LIFO)
                        for (int i = subtasks.size() - 1; i >= 0; i--) {
                            taskStack.add(subtasks.get(i));
                        }
                        methodFound = true;
                        break;
                    }
                }

                if (!methodFound) {
                    return null; // No valid method
                }
            }
        }

        return plan;
    }
}
```

### Strengths

1. **Human-Like Planning:** Mirrors how humans decompose problems
2. **Predictable:** Designer can control available methods
3. **Efficient:** Predefined decomposition reduces search space
4. **Hierarchical:** Natural representation of complex behaviors
5. **Domain Knowledge:** Leverages expert knowledge

### Weaknesses

1. **Domain Dependent:** Requires extensive task library design
2. **Inflexible:** Cannot discover novel solutions outside predefined methods
3. **Complex Setup:** High initial design cost
4. **Brittleness:** Fails if no method matches current situation
5. **Maintenance:** Task hierarchy can become complex

### When to Use HTN

| Scenario | Suitability |
|----------|-------------|
| Strategy games (Civilization) | Excellent |
| Complex crafting/building | Excellent |
| Story-driven games with branching paths | Excellent |
| Simple reactive AI | Overkill |
| Highly dynamic environments | Moderate |

### Code Example: HTN in Action

```java
// Domain definition
public class CraftingDomain {
    public static class CraftItemTask implements CompoundTask {
        @Override
        public List<Method> getMethods(WorldState state) {
            List<Method> methods = new ArrayList<>();

            // Method 1: Craft if have materials
            methods.add(new Method() {
                @Override
                public boolean isValid(WorldState state) {
                    return state.hasMaterials(item);
                }

                @Override
                public List<Task> getSubtasks() {
                    return Arrays.asList(
                        new MoveToCraftingStationTask(),
                        new CraftPrimitiveTask()
                    );
                }
            });

            // Method 2: Gather materials then craft
            methods.add(new Method() {
                @Override
                public boolean isValid(WorldState state) {
                    return state.canGatherMaterials(item);
                }

                @Override
                public List<Task> getSubtasks() {
                    return Arrays.asList(
                        new GatherMaterialsTask(item),
                        new MoveToCraftingStationTask(),
                        new CraftPrimitiveTask()
                    );
                }
            });

            return methods;
        }
    }
}

// Agent using HTN
public class HTNAgent {
    private HTNPlanner planner;
    private WorldState currentState;

    public void executeTask(String taskName) {
        Task rootTask = new Task(taskName);
        List<PrimitiveTask> plan = planner.plan(rootTask, currentState);

        if (plan != null) {
            for (PrimitiveTask task : plan) {
                task.execute(currentState);
                currentState = task.getEffects();
            }
        } else {
            System.out.println("Failed to find plan for: " + taskName);
        }
    }
}
```

### Real-World Implementations

1. **Horizon: Zero Dawn** - NPC combat and exploration AI (Guerrilla Games Decima Engine)
2. **Transformers: Fall of Cybertron** - Boss AI and mission planning
3. **Civilization Series** - AI strategy and diplomacy
4. **UE4/UE5 HTN Plugin** - Third-party plugin for Unreal Engine
5. **Fluid HTN (C#)** - Popular Unity HTN implementation

---

## Behavior Trees

### Overview

**Behavior Trees** are a control-based AI architecture that uses a hierarchical tree structure to determine agent behavior. They have become the industry standard for game AI due to their predictability and excellent tooling support in major engines (Unreal Engine, Unity).

### How It Works

Behavior trees consist of nodes that execute in a specific pattern. The tree is evaluated from the root down to leaf nodes.

```
                    [Selector]
                   /    |    \
              [Seq]   [Seq]   [Seq]
             / | \    / | \    / | \
        Cond Act  Act Cond Act  Act Cond Act Cond

Selector: Execute children until one succeeds
Sequence: Execute children until one fails
Condition: Check if condition is true
Action: Execute behavior
```

### Core Node Types

#### 1. Composite Nodes

```java
// Sequence: Executes children until one fails
public class SequenceNode extends BehaviorNode {
    private List<BehaviorNode> children;
    private int currentChild = 0;

    @Override
    public NodeStatus tick() {
        while (currentChild < children.size()) {
            NodeStatus status = children.get(currentChild).tick();

            if (status == NodeStatus.RUNNING) {
                return NodeStatus.RUNNING;
            }

            if (status == NodeStatus.FAILURE) {
                currentChild = 0; // Reset for next tick
                return NodeStatus.FAILURE;
            }

            currentChild++; // Success, continue to next
        }

        currentChild = 0; // Reset for next tick
        return NodeStatus.SUCCESS;
    }
}

// Selector: Executes children until one succeeds
public class SelectorNode extends BehaviorNode {
    private List<BehaviorNode> children;
    private int currentChild = 0;

    @Override
    public NodeStatus tick() {
        while (currentChild < children.size()) {
            NodeStatus status = children.get(currentChild).tick();

            if (status == NodeStatus.RUNNING) {
                return NodeStatus.RUNNING;
            }

            if (status == NodeStatus.SUCCESS) {
                currentChild = 0; // Reset for next tick
                return NodeStatus.SUCCESS;
            }

            currentChild++; // Failure, try next
        }

        currentChild = 0; // Reset for next tick
        return NodeStatus.FAILURE;
    }
}

// Parallel: Executes all children simultaneously
public class ParallelNode extends BehaviorNode {
    private List<BehaviorNode> children;
    private int successThreshold; // Number of children needed for success

    @Override
    public NodeStatus tick() {
        int successCount = 0;
        int failureCount = 0;

        for (BehaviorNode child : children) {
            NodeStatus status = child.tick();

            if (status == NodeStatus.SUCCESS) successCount++;
            if (status == NodeStatus.FAILURE) failureCount++;

            if (status == NodeStatus.RUNNING) {
                return NodeStatus.RUNNING;
            }
        }

        if (successCount >= successThreshold) {
            return NodeStatus.SUCCESS;
        }
        if (failureCount == children.size()) {
            return NodeStatus.FAILURE;
        }
        return NodeStatus.RUNNING;
    }
}
```

#### 2. Decorator Nodes

```java
// Inverter: Inverts child's result
public class InverterNode extends DecoratorNode {
    @Override
    public NodeStatus tick() {
        NodeStatus status = child.tick();

        if (status == NodeStatus.SUCCESS) return NodeStatus.FAILURE;
        if (status == NodeStatus.FAILURE) return NodeStatus.SUCCESS;
        return NodeStatus.RUNNING;
    }
}

// Repeater: Repeats child N times
public class RepeaterNode extends DecoratorNode {
    private int repeatCount;
    private int currentCount = 0;

    @Override
    public NodeStatus tick() {
        while (currentCount < repeatCount) {
            NodeStatus status = child.tick();
            if (status == NodeStatus.RUNNING) return NodeStatus.RUNNING;
            currentCount++;
        }
        currentCount = 0;
        return NodeStatus.SUCCESS;
    }
}

// Cooldown: Adds cooldown between executions
public class CooldownNode extends DecoratorNode {
    private long cooldownMs;
    private long lastExecution = 0;

    @Override
    public NodeStatus tick() {
        long now = System.currentTimeMillis();
        if (now - lastExecution < cooldownMs) {
            return NodeStatus.FAILURE;
        }

        NodeStatus status = child.tick();
        if (status != NodeStatus.RUNNING) {
            lastExecution = now;
        }
        return status;
    }
}
```

#### 3. Leaf Nodes

```java
// Condition: Checks if a condition is true
public class ConditionNode extends BehaviorNode {
    private Predicate<WorldState> condition;

    @Override
    public NodeStatus tick() {
        return condition.test(worldState) ? NodeStatus.SUCCESS : NodeStatus.FAILURE;
    }
}

// Action: Executes a behavior
public class ActionNode extends BehaviorNode {
    private Runnable action;
    private boolean isRunning = false;

    @Override
    public NodeStatus tick() {
        if (!isRunning) {
            action.run();
            isRunning = true;
        }

        if (isActionComplete()) {
            isRunning = false;
            return NodeStatus.SUCCESS;
        }
        return NodeStatus.RUNNING;
    }
}
```

### Strengths

1. **Predictable:** Designer has full control over behavior flow
2. **Visual:** Excellent tooling and visual editors
3. **Modular:** Easy to create reusable behavior components
4. **Performant:** Very fast execution
5. **Debuggable:** Clear visualization of active behavior path
6. **Standard:** Built into major engines (Unreal, Unity)

### Weaknesses

1. **Rigid:** Behavior structure is hardcoded
2. **No Planning:** Cannot discover new solutions
3. **Complexity:** Trees can become unwieldy for complex behaviors
4. **Maintenance:** Changes require restructuring tree
5. **Limited Flexibility:** Hard to adapt to dynamic situations

### When to Use Behavior Trees

| Scenario | Suitability |
|----------|-------------|
| Enemy combat AI | Excellent |
| Character animation control | Excellent |
| UI and menu navigation | Excellent |
| Complex strategic planning | Poor |
| Highly dynamic environments | Moderate |
| Simple state-based behavior | Excellent |

### Code Example: Complete Behavior Tree

```java
public class EnemyBehaviorTree {
    private BehaviorNode rootNode;

    public EnemyBehaviorTree() {
        // Build tree structure
        rootNode = new SelectorNode(
            // Priority 1: Combat behavior
            new SequenceNode(
                new ConditionNode(state -> state.isEnemyVisible()),
                new ConditionNode(state -> state.hasAmmo()),
                new SequenceNode(
                    new ActionNode(this::aimAtEnemy),
                    new ActionNode(this::fireWeapon)
                )
            ),

            // Priority 2: Reload if needed
            new SequenceNode(
                new ConditionNode(state -> !state.hasAmmo()),
                new ActionNode(this::reloadWeapon)
            ),

            // Priority 3: Patrol behavior
            new SequenceNode(
                new ConditionNode(state -> !state.isEnemyVisible()),
                new SelectorNode(
                    new SequenceNode(
                        new ConditionNode(state -> state.reachedWaypoint()),
                        new ActionNode(this::selectNextWaypoint)
                    ),
                    new ActionNode(this::moveToWaypoint)
                )
            ),

            // Priority 4: Idle
            new ActionNode(this::playIdleAnimation)
        );
    }

    public void update(Enemy enemy) {
        rootNode.tick(enemy.getWorldState());
    }
}
```

### Real-World Implementations

1. **Unreal Engine** - Built-in behavior tree system with visual editor
2. **Unity** - Behavior Designer, NodeCanvas, and other plugins
3. **Behavior3 Editor** - Open-source visual editor (MIT license)
4. **Godot (Beehave)** - Behavior tree plugin for Godot Engine
5. **Behaviac** - Supports BT, FSM, and HTN in one framework

---

## Utility AI Systems

### Overview

**Utility AI** is a scoring-based decision system where each possible action is evaluated against multiple factors, and the action with the highest score (utility) is selected. It excels at creating realistic, context-aware behavior and is particularly effective for emotional AI and tactical reasoning.

### How It Works

```
For each possible action:
    1. Gather input factors (health, distance, threat level, etc.)
    2. Normalize factors to [0, 1] range
    3. Apply response curves (linear, exponential, custom)
    4. Calculate weighted sum of scores
    5. Select action with highest utility score
```

### Core Components

#### 1. Input Factors

Factors represent game state values that influence decisions:

```java
public interface UtilityFactor {
    float evaluate(Agent agent, WorldState state);
}

// Example factors
public class HealthFactor implements UtilityFactor {
    @Override
    public float evaluate(Agent agent, WorldState state) {
        float health = agent.getHealth();
        float maxHealth = agent.getMaxHealth();
        return health / maxHealth; // Normalize to [0, 1]
    }
}

public class DistanceToEnemyFactor implements UtilityFactor {
    @Override
    public float evaluate(Agent agent, WorldState state) {
        Entity enemy = state.getNearestEnemy();
        float distance = agent.distanceTo(enemy);
        // Closer = higher score (inverse relationship)
        return 1.0f - Math.min(distance / 100.0f, 1.0f);
    }
}
```

#### 2. Response Curves

Response curves transform raw input values into utility scores:

```java
public interface ResponseCurve {
    float transform(float inputValue); // Input [0,1] → Output [0,1]
}

// Linear curve (direct mapping)
public class LinearCurve implements ResponseCurve {
    @Override
    public float transform(float input) {
        return input;
    }
}

// Exponential curve (amplifies high values)
public class ExponentialCurve implements ResponseCurve {
    private float exponent;

    public ExponentialCurve(float exponent) {
        this.exponent = exponent;
    }

    @Override
    public float transform(float input) {
        return (float) Math.pow(input, exponent);
    }
}

// Logistic curve (S-curve)
public class LogisticCurve implements ResponseCurve {
    private float steepness;
    private float midpoint;

    @Override
    public float transform(float input) {
        return 1.0f / (1.0f + (float) Math.exp(-steepness * (input - midpoint)));
    }
}
```

#### 3. Utility Actions

Actions define how factors are combined to produce a score:

```java
public class UtilityAction {
    private String name;
    private List<FactorWeight> factors;
    private Runnable executor;

    public float calculateUtility(Agent agent, WorldState state) {
        float totalScore = 0.0f;
        float totalWeight = 0.0f;

        for (FactorWeight fw : factors) {
            float rawValue = fw.factor.evaluate(agent, state);
            float transformedValue = fw.curve.transform(rawValue);
            totalScore += transformedValue * fw.weight;
            totalWeight += fw.weight;
        }

        return totalWeight > 0 ? totalScore / totalWeight : 0.0f;
    }

    public void execute() {
        executor.run();
    }

    private static class FactorWeight {
        UtilityFactor factor;
        ResponseCurve curve;
        float weight;
    }
}
```

#### 4. Utility AI System

```java
public class UtilityAISystem {
    private List<UtilityAction> availableActions;

    public UtilityAction selectBestAction(Agent agent, WorldState state) {
        UtilityAction bestAction = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        for (UtilityAction action : availableActions) {
            float score = action.calculateUtility(agent, state);

            if (score > bestScore) {
                bestScore = score;
                bestAction = action;
            }
        }

        return bestAction;
    }

    public void update(Agent agent, WorldState state) {
        UtilityAction bestAction = selectBestAction(agent, state);
        if (bestAction != null) {
            bestAction.execute();
        }
    }
}
```

### Strengths

1. **Realistic:** Produces natural, context-aware behavior
2. **Flexible:** Easy to add/remove actions and factors
3. **Tunable:** Designers can adjust weights and curves
4. **Emotional:** Excellent for simulating emotional states
5. **No Fixed Structure:** Actions compete independently
6. **Smooth Transitions:** Natural behavior changes

### Weaknesses

1. **Balancing:** Difficult to tune weights and curves
2. **Unpredictable:** Hard to guarantee specific behaviors
3. **No Planning:** Makes decisions based only on current state
4. **Computation:** Evaluates all actions each frame (can be optimized)
5. **Designer Skill:** Requires intuition to tune effectively

### When to Use Utility AI

| Scenario | Suitability |
|----------|-------------|
| Emotional AI systems | Excellent |
| Tactical combat AI | Excellent |
| Realistic NPC behavior | Excellent |
| Simple binary decisions | Overkill |
| Narrative scripting | Poor |
| Real-time with many actions | Careful optimization needed |

### Code Example: Combat Utility AI

```java
public class CombatUtilityAI {
    private UtilityAISystem aiSystem;

    public CombatUtilityAI() {
        aiSystem = new UtilityAISystem();

        // Attack action
        UtilityAction attack = new UtilityAction("Attack");
        attack.addFactor(new HealthFactor(), 0.3f, new LinearCurve());
        attack.addFactor(new DistanceToEnemyFactor(), 0.5f, new ExponentialCurve(2));
        attack.addFactor(new AmmoFactor(), 0.2f, new LogisticCurve(5, 0.5f));
        attack.setExecutor(() -> performAttack());

        // Retreat action
        UtilityAction retreat = new UtilityAction("Retreat");
        retreat.addFactor(new InverseHealthFactor(), 0.6f, new ExponentialCurve(3));
        retreat.addFactor(new DistanceToCoverFactor(), 0.4f, new LinearCurve());
        retreat.setExecutor(() -> performRetreat());

        // Reload action
        UtilityAction reload = new UtilityAction("Reload");
        reload.addFactor(new LowAmmoFactor(), 0.7f, new ExponentialCurve(2));
        reload.addFactor(new IsSafeFactor(), 0.3f, new LinearCurve());
        reload.setExecutor(() -> performReload());

        aiSystem.addAction(attack);
        aiSystem.addAction(retreat);
        aiSystem.addAction(reload);
    }

    public void updateCombat(Agent agent, WorldState state) {
        aiSystem.update(agent, state);
    }
}
```

### Real-World Implementations

1. **Ready Or Not** - Tactical shooter with 50+ AI behavior models
2. **The Sims Series** - Need satisfaction and decision making
3. **Total War Series** - Strategic and tactical AI decisions
4. **Grand Theft Auto V** - NPC behavior and driver AI
5. **Intel Game Dev AI Toolkit** - Utility AI framework for developers

---

## LLM-Powered Game Agents

### Overview

**LLM-Powered Agents** represent the cutting edge of game AI, using large language models (GPT-4, Claude, etc.) to understand natural language commands, reason about complex tasks, and generate executable code. Projects like NVIDIA's Voyager demonstrate unprecedented capabilities for autonomous exploration and learning in game environments like Minecraft.

### How It Works

```
1. User Input: Natural language command ("Build a wooden house")
2. LLM Processing:
   - Parse command into structured tasks
   - Generate code/commands for game execution
   - Handle errors and self-correct
3. Execution:
   - Run generated code in game environment
   - Observe results
4. Learning:
   - Store successful patterns in skill library
   - Update world knowledge
   - Improve future responses
```

### Core Components

#### 1. LLM Client

```java
public interface AsyncLLMClient {
    CompletableFuture<LLMResponse> chatAsync(List<Message> messages);
}

public class OpenAIClient implements AsyncLLMClient {
    private String apiKey;
    private String model;
    private ExecutorService executor;

    @Override
    public CompletableFuture<LLMResponse> chatAsync(List<Message> messages) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // API call to OpenAI
                HttpResponse<String> response = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(buildRequestBody(messages, model))
                    .build()
                    .send(HttpResponse.BodyHandlers.ofString());

                return parseResponse(response.body());
            } catch (Exception e) {
                throw new LLMException("LLM call failed", e);
            }
        }, executor);
    }
}
```

#### 2. Prompt Builder

```java
public class PromptBuilder {
    private WorldKnowledge worldKnowledge;
    private List<ActionDefinition> availableActions;

    public String buildPlanningPrompt(String userCommand, WorldState currentState) {
        StringBuilder prompt = new StringBuilder();

        // System prompt
        prompt.append("You are an AI agent in a Minecraft-like world.\n\n");

        // Available actions
        prompt.append("Available Actions:\n");
        for (ActionDefinition action : availableActions) {
            prompt.append(String.format("- %s: %s\n",
                action.getName(),
                action.getDescription()));
        }

        // Current world state
        prompt.append("\nCurrent World State:\n");
        prompt.append(formatWorldState(currentState));

        // Relevant memories
        prompt.append("\nRelevant Past Experiences:\n");
        List<Memory> relevantMemories = worldKnowledge.searchRelevantMemories(userCommand);
        for (Memory memory : relevantMemories) {
            prompt.append(String.format("- %s\n", memory.getContent()));
        }

        // User command
        prompt.append(String.format("\nUser Command: %s\n", userCommand));
        prompt.append("\nGenerate a sequence of actions to achieve this goal.\n");
        prompt.append("Response format: JSON with 'plan' (description) and 'tasks' (array of action objects).\n");

        return prompt.toString();
    }
}
```

#### 3. Response Parser

```java
public class ResponseParser {
    public ParsedResponse parse(String llmResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(llmResponse);

            String plan = root.get("plan").asText();
            List<Task> tasks = new ArrayList<>();

            JsonNode tasksNode = root.get("tasks");
            for (JsonNode taskNode : tasksNode) {
                Task task = new Task(
                    taskNode.get("action").asText(),
                    taskNode.get("params")
                );
                tasks.add(task);
            }

            return new ParsedResponse(plan, tasks);
        } catch (Exception e) {
            // Fallback parsing or error recovery
            return handleParseError(llmResponse, e);
        }
    }
}
```

#### 4. Skill Library (Voyager Pattern)

```java
public class SkillLibrary {
    private Map<String, Skill> skills;
    private VectorStore vectorStore; // For semantic search

    public void addSkill(String name, String code, String description) {
        Skill skill = new Skill(name, code, description);
        skills.put(name, skill);
        vectorStore.add(name, description);
    }

    public List<Skill> findRelevantSkills(String task) {
        // Semantic search for relevant skills
        List<String> skillNames = vectorStore.search(task, topK=5);
        return skillNames.stream()
            .map(skills::get)
            .collect(Collectors.toList());
    }

    public String executeSkill(String name, Map<String, Object> params) {
        Skill skill = skills.get(name);
        if (skill != null) {
            // Execute skill code (e.g., JavaScript via GraalVM)
            return codeExecutionEngine.execute(skill.getCode(), params);
        }
        throw new IllegalArgumentException("Skill not found: " + name);
    }
}
```

### Architecture Patterns

#### 1. ReAct Pattern (Reasoning + Acting)

```java
public class ReActAgent {
    private AsyncLLMClient llmClient;
    private CodeExecutionEngine codeEngine;

    public AgentResponse execute(String userCommand) {
        List<Message> conversation = new ArrayList<>();
        conversation.add(new Message("user", userCommand));

        while (true) {
            // Get LLM response (reasoning + action)
            LLMResponse llmResponse = llmClient.chatAsync(conversation).join();

            // Check if LLM wants to use a tool
            if (llmResponse.hasAction()) {
                Action action = llmResponse.getAction();

                // Execute action
                ActionResult result = codeEngine.execute(action);

                // Add result back to conversation
                conversation.add(new Message("assistant", llmResponse.getText()));
                conversation.add(new Message("user", "Observation: " + result));

                // Continue if not done
                if (result.isComplete()) {
                    break;
                }
            } else {
                // LLM gave final answer
                return new AgentResponse(llmResponse.getText());
            }
        }
    }
}
```

#### 2. Multi-Agent Orchestration

```java
public class OrchestratorService {
    private List<ForemanEntity> agents;
    private AgentCommunicationBus commBus;

    public void executeTask(String task) {
        // Decompose task into subtasks
        List<SubTask> subtasks = decomposeTask(task);

        // Assign to agents based on capability
        for (SubTask subtask : subtasks) {
            ForemanEntity bestAgent = findBestAgent(subtask);
            bestAgent.queueTask(subtask);
        }

        // Monitor progress and rebalance if needed
        monitorProgress();
    }

    private ForemanEntity findBestAgent(SubTask task) {
        // Find idle agent or agent with shortest queue
        return agents.stream()
            .filter(a -> a.canAcceptTask(task))
            .min(Comparator.comparing(ForemanEntity::getQueueLength))
            .orElse(null);
    }
}
```

### Strengths

1. **Natural Language:** Understands complex commands
2. **Flexibility:** No hardcoded behaviors
3. **Learning:** Can acquire new skills over time
4. **Reasoning:** Can handle novel situations
5. **Code Generation:** Can write executable code
6. **Context Awareness:** Uses memory for intelligent decisions

### Weaknesses

1. **Latency:** API calls can be slow (30-60 seconds)
2. **Cost:** LLM API costs can be significant
3. **Reliability:** Responses can be inconsistent
4. **Complexity:** Requires sophisticated error handling
5. **Resource Intensive:** High memory and CPU usage
6. **Debugging:** Difficult to trace decision process

### When to Use LLM Agents

| Scenario | Suitability |
|----------|-------------|
| Natural language interfaces | Excellent |
| Complex planning tasks | Excellent |
| Research prototypes | Excellent |
| Real-time combat actions | Poor (too slow) |
| Simple repetitive tasks | Overkill |
| Production game with budget constraints | Careful cost analysis needed |

### Real-World Implementations

1. **Voyager (NVIDIA, 2023)** - First lifelong learning agent in Minecraft
   - 3.3× more items discovered
   - 15× faster diamond tool unlocking
   - GPT-4 powered with skill library

2. **MineDojo (NVIDIA, 2022)** - NeurIPS award winner
   - 730,000+ Minecraft videos
   - 7,000+ wiki pages
   - Custom MineCLIP model

3. **Steve Mod** - Multi-agent Minecraft modification
   - Natural language commands
   - Multi-agent coordination
   - Mining, building, defense

4. **Mindcraft** - Open-source framework
   - 20+ LLM API support
   - JavaScript code generation
   - Visual understanding

### Code Example: Complete LLM Agent

```java
public class LLMGameAgent {
    private AsyncLLMClient llmClient;
    private PromptBuilder promptBuilder;
    private ResponseParser responseParser;
    private SkillLibrary skillLibrary;
    private WorldKnowledge worldKnowledge;
    private CodeExecutionEngine codeEngine;

    private CompletableFuture<ParsedResponse> planningFuture;
    private Queue<Task> taskQueue = new LinkedBlockingQueue<>();
    private BaseAction currentAction;

    public void processCommand(String command) {
        // Build prompt with context
        String prompt = promptBuilder.buildPlanningPrompt(
            command,
            getWorldState()
        );

        // Start async LLM call
        planningFuture = llmClient.chatAsync(prompt)
            .thenApply(response -> {
                ParsedResponse parsed = responseParser.parse(response);
                taskQueue.addAll(parsed.getTasks());
                return parsed;
            });
    }

    public void tick() {
        // Check if planning is complete
        if (planningFuture != null && planningFuture.isDone()) {
            try {
                ParsedResponse result = planningFuture.get();
                // Tasks already queued in thenApply
            } catch (Exception e) {
                handleError(e);
            }
            planningFuture = null;
        }

        // Execute current action
        if (currentAction == null || currentAction.isComplete()) {
            if (!taskQueue.isEmpty()) {
                Task task = taskQueue.poll();
                currentAction = createAction(task);
                currentAction.start();
            }
        } else {
            currentAction.tick();
        }
    }

    private BaseAction createAction(Task task) {
        // Check if skill exists for this task
        List<Skill> relevantSkills = skillLibrary.findRelevantSkills(task.getAction());

        if (!relevantSkills.isEmpty()) {
            // Use existing skill
            return new SkillAction(relevantSkills.get(0), task);
        } else {
            // Ask LLM to generate new skill code
            String code = generateSkillCode(task);
            skillLibrary.addSkill(task.getAction(), code, task.getDescription());
            return new SkillAction(skillLibrary.getSkill(task.getAction()), task);
        }
    }
}
```

---

## Architecture Comparison

### Decision Matrix

| Criterion | GOAP | HTN | Behavior Trees | Utility AI | LLM Agents |
|-----------|------|-----|----------------|------------|------------|
| **Predictability** | Low | Medium | High | Medium | Low |
| **Flexibility** | High | Medium | Low | High | Very High |
| **Performance** | Medium | Good | Excellent | Good | Poor |
| **Setup Complexity** | High | Very High | Low | Medium | Very High |
| **Tuning Difficulty** | Medium | High | Low | High | Low |
| **Debugging** | Hard | Medium | Easy | Medium | Very Hard |
| **Tool Support** | Poor | Medium | Excellent | Medium | Poor |
| **Emergent Behavior** | Excellent | Poor | None | Good | Excellent |
| **Natural Language** | None | None | None | None | Excellent |
| **Learning Capability** | None | None | None | None | Excellent |

### When to Choose Each Architecture

#### Choose GOAP when:
- You need emergent, unexpected intelligent behavior
- The agent has many possible actions and goals
- World state changes frequently
- You can afford the computational cost
- Examples: Open-world survival games, sandbox games

#### Choose HTN when:
- You have expert domain knowledge
- Tasks have clear hierarchical structure
- You want predictable but complex behavior
- Planning needs to incorporate foresight
- Examples: Strategy games, complex crafting systems

#### Choose Behavior Trees when:
- You need precise control over behavior
- You have good visual editing tools
- Performance is critical
- Behavior patterns are relatively fixed
- Examples: Enemy combat AI, animation control, UI behavior

#### Choose Utility AI when:
- You want realistic, context-aware decisions
- Emotional AI or personality is important
- Tactical reasoning is needed
- Designer wants to tune weights rather than structure
- Examples: NPC daily routines, combat tactics, social simulations

#### Choose LLM Agents when:
- Natural language input is required
- Tasks are complex and varied
- Learning and adaptation are important
- You can tolerate latency and cost
- Examples: Research prototypes, AI companions, creative tools

### Hybrid Approaches

Many modern games combine multiple architectures:

1. **Behavior Tree + GOAP**
   - Behavior tree handles low-level actions
   - GOAP handles high-level planning
   - Example: Combat games with strategic positioning

2. **Utility AI + Behavior Trees**
   - Utility scores select behavior tree
   - Behavior tree executes selected behavior
   - Example: NPC daily life simulations

3. **LLM + Behavior Trees**
   - LLM generates plan or interprets commands
   - Behavior tree executes individual actions
   - Example: Current MineWright AI implementation

4. **HTN + Utility AI**
   - HTN decomposes high-level tasks
   - Utility AI selects between alternative methods
   - Example: Strategic games with tactical combat

---

## Hybrid Approaches

### 1. Behavior Tree + Utility AI

Use utility scoring to select between behavior trees:

```java
public class UtilityBehaviorSelector {
    private Map<String, BehaviorTree> behaviors;
    private UtilityScorer scorer;

    public void update(Agent agent) {
        // Score each behavior tree
        String bestBehavior = behaviors.entrySet().stream()
            .max(Comparator.comparing(e -> scorer.scoreBehavior(e.getKey(), agent)))
            .map(Map.Entry::getKey)
            .orElse("idle");

        // Execute selected behavior tree
        behaviors.get(bestBehavior).tick(agent);
    }
}
```

### 2. LLM Planner + Behavior Tree Execution

```java
public class HybridAgent {
    private LLMPlanner planner;
    private BehaviorTreeExecutor executor;

    public void processCommand(String command) {
        // LLM generates high-level plan
        List<String> plan = planner.generatePlan(command);

        // Convert plan to behavior tree
        BehaviorTree bt = executor.buildTreeFromPlan(plan);

        // Execute behavior tree
        executor.execute(bt);
    }
}
```

### 3. GOAP + HTN

Use HTN for task decomposition and GOAP for low-level planning:

```java
public class HybridPlanner {
    private HTNPlanner htnPlanner;
    private GOAPPlanner goapPlanner;

    public List<Action> plan(String highLevelTask, WorldState state) {
        // Decompose high-level task into subtasks
        List<Task> subtasks = htnPlanner.decompose(highLevelTask, state);

        List<Action> plan = new ArrayList<>();
        for (Task subtask : subtasks) {
            if (subtask.isPrimitive()) {
                plan.add(subtask.asAction());
            } else {
                // Use GOAP to plan subtask
                plan.addAll(goapPlanner.plan(subtask, state));
            }
        }
        return plan;
    }
}
```

### 4. Multi-Level Architecture

```java
public class MultiLevelAI {
    // Level 1: LLM for high-level understanding
    private LLMClient llmClient;

    // Level 2: HTN for task decomposition
    private HTNPlanner htnPlanner;

    // Level 3: Utility AI for action selection
    private UtilitySystem utilitySystem;

    // Level 4: Behavior trees for execution
    private BehaviorTreeExecutor btExecutor;

    public void execute(String userCommand) {
        // Level 1: Understand command
        Intent intent = llmClient.parseIntent(userCommand);

        // Level 2: Decompose into tasks
        List<Task> tasks = htnPlanner.decompose(intent);

        // Level 3: Select best action for each task
        for (Task task : tasks) {
            Action action = utilitySystem.selectBestAction(task);
            // Level 4: Execute via behavior tree
            btExecutor.execute(action.getBehaviorTree());
        }
    }
}
```

---

## Recommendations for MineWright AI

### Current Architecture Analysis

The MineWright AI (MineWright mod) currently uses:

1. **LLM-Based Planning** (OpenAI/Groq/Gemini)
2. **Plugin-Based Action System** (ActionRegistry, ActionFactory)
3. **State Machine** (AgentStateMachine)
4. **Event Bus** (EventBus, InterceptorChain)
5. **Tick-Based Execution** (BaseAction.tick())

### Strengths of Current Design

- Natural language command understanding
- Modular, extensible action system
- Clean separation of concerns
- Async planning prevents game freezing
- Multi-agent orchestration support

### Potential Improvements

#### 1. Add Utility AI for Action Selection

**Problem:** Current system executes LLM-generated tasks sequentially without considering current context.

**Solution:** Add utility scoring to prioritize and adapt tasks:

```java
public class UtilityTaskPrioritizer {
    public float scoreTask(Task task, ForemanEntity steve, WorldState state) {
        float score = 0.0f;

        // Factor: Distance to task location
        float distance = steve.distanceTo(task.getLocation());
        score += Math.max(0, 1.0f - distance / 100.0f) * 0.3f;

        // Factor: Resource availability
        if (state.hasResources(task.getRequiredResources())) {
            score += 0.4f;
        }

        // Factor: Time of day (construction during day)
        if (task.isConstruction() && state.isDay()) {
            score += 0.2f;
        }

        // Factor: Danger level
        if (!state.isLocationSafe(task.getLocation())) {
            score -= 0.3f;
        }

        return score;
    }
}
```

#### 2. Implement Behavior Tree for Action Composition

**Problem:** Complex actions (like building structures) are monolithic.

**Solution:** Use behavior trees to compose complex actions from primitives:

```java
public class BuildStructureBehaviorTree extends BehaviorTree {
    public BuildStructureBehaviorTree(StructureTemplate template) {
        this.root = new SequenceNode(
            // Phase 1: Gather materials
            new SelectorNode(
                new SequenceNode(
                    new ConditionNode(s -> s.hasMaterials(template)),
                    new SuccessNode()
                ),
                new GatherMaterialsBehavior(template.getMaterials())
            ),

            // Phase 2: Move to site
            new MoveToLocationBehavior(template.getLocation()),

            // Phase 3: Build structure
            new ParallelNode(
                new PlaceBlocksBehavior(template.getBlocks()),
                new MonitorForObstaclesBehavior()
            ),

            // Phase 4: Verify completion
            new VerifyStructureBehavior(template)
        );
    }
}
```

#### 3. Add HTN for Complex Task Decomposition

**Problem:** LLM sometimes generates inefficient task sequences.

**Solution:** Use HTN for predictable decomposition of common tasks:

```java
public class CraftingHTN {
    public static class CraftItemTask implements CompoundTask {
        @Override
        public List<Method> getMethods(WorldState state) {
            return Arrays.asList(
                // Method 1: Already have materials
                new Method() {
                    public boolean isValid(WorldState s) {
                        return s.hasMaterials(currentItem.getRecipe());
                    }
                    public List<Task> getSubtasks() {
                        return Arrays.asList(
                            new Task("move", Map.of("target", "crafting_table")),
                            new Task("craft", Map.of("item", currentItem))
                        );
                    }
                },

                // Method 2: Need to gather materials
                new Method() {
                    public boolean isValid(WorldState s) {
                        return s.canGatherMaterials(currentItem.getRecipe());
                    }
                    public List<Task> getSubtasks() {
                        return Arrays.asList(
                            new GatherMaterialsTask(currentItem.getRecipe()),
                            new Task("move", Map.of("target", "crafting_table")),
                            new Task("craft", Map.of("item", currentItem))
                        );
                    }
                }
            );
        }
    }
}
```

#### 4. Implement Skill Library (Voyager Pattern)

**Problem:** Successful patterns aren't reused across sessions.

**Solution:** Implement a skill library with semantic search:

```java
public class SteveSkillLibrary {
    private VectorStore skillVectorStore;
    private Map<String, JavaScriptSkill> skills;

    public void learnFromExecution(String command, List<Task> tasks, boolean success) {
        if (success) {
            // Create skill from successful execution
            String skillName = generateSkillName(command);
            String code = generateSkillCode(tasks);
            String description = command;

            JavaScriptSkill skill = new JavaScriptSkill(skillName, code, description);
            skills.put(skillName, skill);
            skillVectorStore.add(skillName, description);
        }
    }

    public Optional<JavaScriptSkill> findRelevantSkill(String command) {
        List<String> similarSkills = skillVectorStore.search(command, topK=3);
        return similarSkills.stream()
            .map(skills::get)
            .findFirst();
    }

    private String generateSkillCode(List<Task> tasks) {
        // Generate JavaScript code that executes the task sequence
        StringBuilder code = new StringBuilder();
        code.append("async function execute(bot, params) {\n");
        for (Task task : tasks) {
            code.append(generateCodeForTask(task));
        }
        code.append("}\n");
        return code.toString();
    }
}
```

#### 5. Add Emotional Layer with Utility AI

**Problem:** Steve lacks personality and emotional responses.

**Solution:** Add emotion system with utility-based responses:

```java
public class SteveEmotionalSystem {
    private enum Emotion { HAPPY, FRUSTRATED, TIRED, EXCITED, BORED }
    private Map<Emotion, Float> emotionalState;
    private UtilityResponseGenerator responseGenerator;

    public void onTaskComplete(boolean success, Task task) {
        if (success) {
            emotionalState.merge(Emotion.HAPPY, 0.2f, Float::sum);
            emotionalState.merge(Emotion.EXCITED, 0.1f, Float::sum);
        } else {
            emotionalState.merge(Emotion.FRUSTRATED, 0.3f, Float::sum);
        }

        // Generate response based on emotional state
        String response = responseGenerator.generateResponse(
            task,
            getDominantEmotion(),
            emotionalState
        );

        steve.sendChatMessage(response);
    }

    public Emotion getDominantEmotion() {
        return emotionalState.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(Emotion.BORED);
    }
}
```

### Recommended Architecture Evolution

```
Current:
User Command → LLM Planner → Task Queue → Action Execution

Recommended Evolution:
User Command
    ↓
[Skill Library Check] ← If skill exists, use it
    ↓ (no skill)
[LLM Planner]
    ↓
[HTN Decomposition] ← Structured decomposition for common tasks
    ↓
[Utility Prioritization] ← Score and reorder based on context
    ↓
[Behavior Tree Execution] ← Execute complex actions
    ↓
[Emotional Response] ← Add personality to responses
    ↓
[Skill Learning] ← Store successful patterns
```

### Implementation Priority

1. **Phase 1: Skill Library** (High Value, Medium Effort)
   - Implement vector store for skill storage
   - Add semantic search for relevant skills
   - Generate JavaScript code from task sequences
   - This is the biggest win for reusability

2. **Phase 2: Utility Prioritization** (Medium Value, Low Effort)
   - Add scoring factors for task prioritization
   - Reorder task queue based on utility scores
   - Simple but effective improvement

3. **Phase 3: Emotional System** (Medium Value, Medium Effort)
   - Add emotional state tracking
   - Implement utility-based response generation
   - Makes Steve feel more alive

4. **Phase 4: Behavior Tree Composition** (Low Value, High Effort)
   - Useful for very complex actions
   - Can be added incrementally
   - Nice to have but not critical

5. **Phase 5: HTN Decomposition** (Low Value, High Effort)
   - HTN is less useful when you have LLM planning
   - Consider only if LLM latency becomes problematic

---

## References

### Academic Papers

1. **Voyager: An Open-Ended Embodied Agent with Large Language Models**
   - Wang et al., NVIDIA, 2023
   - [Paper](https://arxiv.org/abs/2305.16291)

2. **MineDojo: Building Open-Ended Embodied Agents with Internet-Scale Knowledge**
   - Fan et al., NVIDIA, NeurIPS 2022
   - [Paper](https://arxiv.org/abs/2211.12581)

3. **Goal-Oriented Action Planning (GOAP)**
   - Orkin, J., MIT, 2004
   - [AI Game Programming Wisdom 1]

4. **HTN Planning: Complexity and Prospects**
   - Erol, K., Hendler, J., & Nau, D.S., 1994
   - University of Maryland

5. **Behavior Trees in Robotics and AI**
   - Colledanchise, A. & Ogren, P., 2018
   - CRC Press

### Open Source Projects

1. **Embabel Agent Framework**
   - JVM-based GOAP implementation
   - [GitHub](https://github.com/embabel/embabel-agent)

2. **Behavior3 Editor**
   - Visual behavior tree editor (MIT License)
   - [GitHub](https://github.com/behavior3/behavior3editor)

3. **PyTrees**
   - Python behavior tree library
   - [GitHub](https://github.com/splintered-reality/py_trees)

4. **Fluid HTN**
   - C# HTN planner for Unity
   - [GitHub](https://github.com/ashblue/fluid-behavior-tree)

5. **Mindcraft**
   - Open-source LLM Minecraft agent
   - [GitHub](https://github.com/Mindcraft)

### Game Implementations

1. **F.E.A.R.** (2005) - First major GOAP implementation
2. **Horizon: Zero Dawn** - HTN planning in Decima Engine
3. **The Sims Series** - Utility-based need system
4. **Fallout 4** - GOAP for NPC survival
5. **Ready Or Not** - Utility AI with 50+ behavior models

### Online Resources

1. **GOAP Tutorials**
   - [游戏AI行为决策——GOAP](https://www.cnblogs.com/OwlCat/Undeclared/17936809.html)
   - [Udemy GOAP Course](https://www.udemy.com/course/ai_with_goap/)

2. **HTN Resources**
   - [游戏AI行为决策——HTN](https://www.cnblogs.com/OwlCat/p/17910900.html)
   - [Unreal HTN Plugin Docs](https://maksmaisak.github.io/htn/)

3. **Behavior Tree Implementations**
   - [C++ Behavior Tree Framework](https://m.blog.csdn.net/m0_75015600/article/details/153280061)
   - [Python Behavior Tree Tutorial](https://m.php.cn/faq/1351692.html)
   - [Behaviac Framework](https://github.com/Tencent/behaviac)

4. **Utility AI**
   - [谈一谈游戏AI - Utility](https://juejin.cn/post/7214041943297818685)
   - [Utility AI: A Weight-Based Game AI](https://blog.csdn.net/wawa1203/article/details/109337835)

5. **LLM Agents**
   - [LangGraph Multi-Agent Tutorial](https://www.cnblogs.com/crazymakercircle/p/19412858)
   - [ReAct Framework Guide](https://blog.csdn.net/zuozewei/)
   - [Voyager Architecture](https://m.blog.csdn.net/gitblog_00533/article/details/151387307)

### Frameworks and Libraries

1. **LangChain** - LLM application framework
2. **LangGraph** - Stateful agent framework
3. **AutoGen** - Microsoft's multi-agent framework
4. **Semantic Kernel** - Microsoft's agent SDK
5. **Unity ML-Agents** - Game AI toolkit
6. **Intel Game Dev AI Toolkit** - AI integration for games

---

## Conclusion

Modern game AI development offers a rich landscape of architectures, each with distinct strengths and weaknesses. The choice of architecture depends heavily on the game's requirements:

- **Predictable, controlled behavior** → Behavior Trees
- **Complex strategic planning** → HTN
- **Emergent, dynamic behavior** → GOAP
- **Realistic, emotional AI** → Utility AI
- **Natural language interfaces** → LLM Agents

For **MineWright AI**, the current LLM-based approach is well-suited for the core functionality. The recommended improvements focus on:

1. **Skill Library** - Learning and reusing successful patterns
2. **Utility Prioritization** - Context-aware task ordering
3. **Emotional System** - Adding personality and engagement
4. **Behavior Tree Composition** - Structuring complex actions

These enhancements maintain the strengths of the LLM-based approach while addressing its weaknesses (latency, cost, reliability) through caching, prioritization, and structured execution patterns.

The future of game AI lies in **hybrid architectures** that combine the reasoning capabilities of LLMs with the reliability and performance of traditional AI systems. MineWright AI is well-positioned to evolve in this direction.

---

**Document Version:** 1.0
**Last Updated:** 2025-02-27
**Maintained By:** MineWright AI Development Team
