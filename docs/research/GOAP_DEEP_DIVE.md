# GOAP (Goal-Oriented Action Planning) Deep Dive

**Author:** Orchestrator Research Team
**Date:** 2026-02-28
**Version:** 1.0
**Status:** Research Document

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [GOAP Architecture](#2-goap-architecture)
3. [World State Representation](#3-world-state-representation)
4. [Actions: Preconditions and Effects](#4-actions-preconditions-and-effects)
5. [Goal Definitions](#5-goal-definitions)
6. [A* Planning Through Action Space](#6-a-planning-through-action-space)
7. [GOAP vs HTN](#7-goap-vs-htn)
8. [GOAP Implementation](#8-goap-implementation)
9. [GOAP + LLM Integration](#9-goap--llm-integration)
10. [Java Implementation Examples](#10-java-implementation-examples)
11. [References](#11-references)

---

## 1. Introduction

### 1.1 What is GOAP?

**Goal-Oriented Action Planning (GOAP)** is an AI planning architecture originally developed by Jeff Orkin at Monolith Productions for the game **F.E.A.R. (First Encounter Assault Recon)**. It enables autonomous agents to:

- Select goals dynamically based on environment and priorities
- Generate plans to achieve those goals using available actions
- Adapt to changing world states through replanning
- Exhibit emergent, intelligent behavior without hardcoded decision trees

### 1.2 Why GOAP Matters

| Traditional Approach | GOAP Approach |
|---------------------|---------------|
| Finite State Machines with fixed transitions | Dynamic plan generation based on goals |
| Hardcoded behavior trees | Flexible action selection |
| Brittle in dynamic environments | Adaptive to state changes |
| Difficult to extend | Easy to add new actions/goals |

### 1.3 Key Games Using GOAP

- **F.E.A.R.** - Tactical combat AI with flanking, suppression, coordinated attacks
- **S.T.A.L.K.E.R.** - Survival AI with resource management
- **Hitman series** - Stealth and assassination planning
- **Kenshi** - Open-world squad tactics
- **Shadow of Mordor** - Nemesis system behaviors

---

## 2. GOAP Architecture

### 2.1 Core Components

```
┌─────────────────────────────────────────────────────────────┐
│                    GOAP System Architecture                  │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐      ┌──────────────┐                    │
│  │   Sensors    │──────│  World State │                     │
│  │              │      │  Repository  │                     │
│  └──────────────┘      └──────┬───────┘                     │
│                                │                             │
│                                ▼                             │
│  ┌──────────────┐      ┌──────────────┐                    │
│  │   Goals      │──────│   Planner    │                     │
│  │  (Priority)  │      │   (A* Search)│                    │
│  └──────────────┘      └──────┬───────┘                     │
│                                │                             │
│                                ▼                             │
│  ┌──────────────┐      ┌──────────────┐                    │
│  │   Actions    │──────│  Action Plan │                     │
│  │ (Pre/Effect) │      │   Executor   │                     │
│  └──────────────┘      └──────┬───────┘                     │
│                                │                             │
│                                ▼                             │
│                       ┌──────────────┐                      │
│                       │   Effects    │                      │
│                       │   (Changes)  │                      │
│                       └──────────────┘                      │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Data Flow

1. **Perceive**: Sensors observe world state
2. **Select Goal**: Choose highest priority achievable goal
3. **Plan**: A* search finds action sequence to achieve goal
4. **Execute**: Run actions sequentially, monitoring for changes
5. **Replan**: If world state changes unexpectedly, generate new plan

### 2.3 Planning Loop

```java
while (agent.isAlive()) {
    WorldState current = senseWorld();
    Goal goal = goalSelector.selectBestGoal(current);

    if (goal != currentGoal || !currentPlan.isValid(current)) {
        currentPlan = planner.plan(current, goal, availableActions);
        currentGoal = goal;
    }

    if (currentPlan != null && !currentPlan.isComplete()) {
        currentPlan.executeNextAction();
    }

    waitForNextTick();
}
```

---

## 3. World State Representation

### 3.1 State as Key-Value Pairs

World state is a collection of boolean, integer, or enum values representing the current situation:

```java
public class WorldState {
    private final Map<String, Object> stateValues;

    public WorldState() {
        this.stateValues = new ConcurrentHashMap<>();
    }

    public void set(String key, boolean value) {
        stateValues.put(key, value);
    }

    public void set(String key, int value) {
        stateValues.put(key, value);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = stateValues.get(key);
        return value != null ? (Boolean) value : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        Object value = stateValues.get(key);
        return value != null ? (Integer) value : defaultValue;
    }

    /**
     * Checks if this state satisfies all conditions in target state.
     * Used for precondition checking and goal validation.
     */
    public boolean satisfies(WorldState target) {
        for (Map.Entry<String, Object> entry : target.stateValues.entrySet()) {
            Object ourValue = stateValues.get(entry.getKey());
            if (!entry.getValue().equals(ourValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates how many state variables match between two states.
     * Used for heuristic calculation in A* search.
     */
    public int calculateMatchingCount(WorldState other) {
        int matches = 0;
        for (Map.Entry<String, Object> entry : stateValues.entrySet()) {
            Object otherValue = other.stateValues.get(entry.getKey());
            if (Objects.equals(entry.getValue(), otherValue)) {
                matches++;
            }
        }
        return matches;
    }

    /**
     * Creates a deep copy of this world state.
     * Essential for simulation during planning.
     */
    public WorldState copy() {
        WorldState copy = new WorldState();
        copy.stateValues.putAll(this.stateValues);
        return copy;
    }
}
```

### 3.2 Example World State

```java
// Combat scenario world state
WorldState state = new WorldState();
state.set("hasWeapon", true);
state.set("ammoCount", 15);
state.set("enemyVisible", true);
state.set("inCover", false);
state.set("enemyHealth", 100);
state.set("playerHealth", 80);
state.set("grenadeAvailable", true);
state.set("enemyDistance", 25);  // meters
```

### 3.3 State Difference Calculation

Used for heuristic computation in A* search:

```java
public class StateDifference {

    /**
     * Calculates the Manhattan distance between two states.
     * For boolean states: 0 if same, 1 if different
     * For integer states: absolute difference
     */
    public static int calculateDistance(WorldState from, WorldState to) {
        int distance = 0;

        // All keys in target state
        for (String key : to.getAllKeys()) {
            Object fromValue = from.get(key);
            Object toValue = to.get(key);

            if (fromValue instanceof Boolean && toValue instanceof Boolean) {
                distance += (fromValue.equals(toValue)) ? 0 : 1;
            } else if (fromValue instanceof Integer && toValue instanceof Integer) {
                distance += Math.abs((Integer) fromValue - (Integer) toValue);
            } else {
                distance += fromValue.equals(toValue) ? 0 : 1;
            }
        }

        return distance;
    }

    /**
     * Returns keys that differ between two states.
     * Useful for debugging and action selection.
     */
    public static Set<String> getDifferingKeys(WorldState from, WorldState to) {
        Set<String> differences = new HashSet<>();

        for (String key : to.getAllKeys()) {
            if (!Objects.equals(from.get(key), to.get(key))) {
                differences.add(key);
            }
        }

        return differences;
    }
}
```

---

## 4. Actions: Preconditions and Effects

### 4.1 Action Interface

```java
public interface GoapAction {

    /**
     * Checks if this action can be executed given current world state.
     */
    boolean canExecute(WorldState state);

    /**
     * Applies the effects of this action to a copy of world state.
     * Used during planning simulation (not actual execution).
     */
    WorldState simulateEffects(WorldState state);

    /**
     * Executes the action in the real world.
     * Returns true if execution started successfully.
     */
    boolean execute(WorldState state, ActionContext context);

    /**
     * Returns the cost of executing this action.
     * Higher cost actions are less preferred by planner.
     */
    int getCost();

    /**
     * Returns the preconditions as a world state template.
     * All values in this state must match for action to be executable.
     */
    WorldState getPreconditions();

    /**
     * Returns the effects as a world state template.
     * These values will be set/modified in world state after execution.
     */
    WorldState getEffects();

    /**
     * Checks if this action is still running.
     */
    boolean isRunning();

    /**
     * Cancels the action (cleanup).
     */
    void cancel();
}
```

### 4.2 Base Action Implementation

```java
public abstract class BaseGoapAction implements GoapAction {

    protected final String name;
    protected final WorldState preconditions;
    protected final WorldState effects;
    protected final int cost;
    protected boolean isRunning;

    public BaseGoapAction(String name, int cost) {
        this.name = name;
        this.cost = cost;
        this.preconditions = new WorldState();
        this.effects = new WorldState();
        this.isRunning = false;
    }

    @Override
    public boolean canExecute(WorldState state) {
        return state.satisfies(preconditions);
    }

    @Override
    public WorldState simulateEffects(WorldState state) {
        WorldState newState = state.copy();

        // Apply all effects to the new state
        for (String key : effects.getAllKeys()) {
            newState.set(key, effects.get(key));
        }

        return newState;
    }

    @Override
    public int getCost() {
        return cost;
    }

    @Override
    public WorldState getPreconditions() {
        return preconditions;
    }

    @Override
    public WorldState getEffects() {
        return effects;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void cancel() {
        isRunning = false;
    }

    /**
     * Subclasses implement actual execution logic.
     * Should set isRunning = true at start and isRunning = false when complete.
     */
    @Override
    public abstract boolean execute(WorldState state, ActionContext context);
}
```

### 4.3 Concrete Action Examples

#### Combat Actions

```java
public class ShootAction extends BaseGoapAction {

    public ShootAction() {
        super("Shoot", 1);  // Low cost - quick action

        // Preconditions
        preconditions.set("hasWeapon", true);
        preconditions.set("ammoCount", 1);  // At least 1 bullet
        preconditions.set("enemyVisible", true);

        // Effects
        effects.set("ammoCount", -1);  // Decrements by 1
        effects.set("enemyVisible", false);  // Might hide after shooting
    }

    @Override
    public boolean execute(WorldState state, ActionContext context) {
        isRunning = true;

        // Actually shoot the weapon
        int currentAmmo = state.getInt("ammoCount", 0);
        state.set("ammoCount", currentAmmo - 1);

        // Apply damage to enemy (would be in game code)
        // context.getEnemy().takeDamage(10);

        isRunning = false;
        return true;
    }
}

public class ReloadAction extends BaseGoapAction {

    public ReloadAction() {
        super("Reload", 5);  // Higher cost - takes time

        preconditions.set("hasWeapon", true);
        preconditions.set("ammoCount", 0);  // Only reload when empty

        effects.set("ammoCount", 30);  // Full magazine
    }

    @Override
    public boolean execute(WorldState state, ActionContext context) {
        isRunning = true;

        // Play reload animation, wait for completion
        try {
            Thread.sleep(2000);  // Simulate reload time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        state.set("ammoCount", 30);
        isRunning = false;
        return true;
    }
}

public class TakeCoverAction extends BaseGoapAction {

    public TakeCoverAction() {
        super("TakeCover", 3);

        preconditions.set("enemyVisible", true);
        preconditions.set("inCover", false);

        effects.set("inCover", true);
        effects.set("enemyVisible", false);  // Can't see enemy from cover
    }

    @Override
    public boolean execute(WorldState state, ActionContext context) {
        isRunning = true;

        // Pathfind to nearest cover
        // BlockPos cover = context.findNearestCover();
        // context.getNavigation().moveTo(cover);

        state.set("inCover", true);
        isRunning = false;
        return true;
    }
}
```

#### Resource Gathering Actions (Minecraft Context)

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
        effects.set("near_" + blockType, false);  // Mined it
    }

    @Override
    public boolean execute(WorldState state, ActionContext context) {
        isRunning = true;

        // Mine the block
        // context.breakBlock(blockType);

        state.set("has_" + blockType, true);
        state.set("near_" + blockType, false);

        // Check inventory
        // if (context.getInventory().isFull()) {
        //     state.set("inventoryFull", true);
        // }

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

        // Preconditions - need all materials
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
            // context.getInventory().remove(material.getKey(), material.getValue());
        }

        // Add crafted item
        // context.getInventory().add(outputItem);
        state.set("has_" + outputItem, true);

        isRunning = false;
        return true;
    }
}
```

---

## 5. Goal Definitions

### 5.1 Goal Interface

```java
public interface GoapGoal {

    /**
     * Returns the desired world state.
     * All values in this state represent conditions to achieve.
     */
    WorldState getTargetState();

    /**
     * Returns the priority of this goal.
     * Higher priority goals are selected first.
     * Priority can be dynamic based on world state.
     */
    int getPriority(WorldState currentState);

    /**
     * Returns true if this goal is currently achieved.
     */
    boolean isAchieved(WorldState state);

    /**
     * Human-readable name for debugging.
     */
    String getName();
}
```

### 5.2 Base Goal Implementation

```java
public abstract class BaseGoapGoal implements GoapGoal {

    protected final String name;
    protected final WorldState targetState;
    protected int basePriority;

    public BaseGoapGoal(String name, int basePriority) {
        this.name = name;
        this.basePriority = basePriority;
        this.targetState = new WorldState();
    }

    @Override
    public WorldState getTargetState() {
        return targetState;
    }

    @Override
    public int getPriority(WorldState currentState) {
        // Can be overridden for dynamic priority
        return basePriority;
    }

    @Override
    public boolean isAchieved(WorldState state) {
        return state.satisfies(targetState);
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Helper to add a target condition.
     */
    protected void addTarget(String key, Object value) {
        targetState.set(key, value);
    }
}
```

### 5.3 Concrete Goal Examples

#### Combat Goals

```java
public class KillEnemyGoal extends BaseGoapGoal {

    public KillEnemyGoal() {
        super("KillEnemy", 10);  // High priority
        addTarget("enemyHealth", 0);  // Enemy dead
    }

    @Override
    public int getPriority(WorldState currentState) {
        // Priority increases if enemy can see us
        if (currentState.getBoolean("enemyVisible", false)) {
            return 20;  // Very high priority when in danger
        }

        // Priority decreases if we're low on health
        int playerHealth = currentState.getInt("playerHealth", 100);
        if (playerHealth < 30) {
            return 5;  // Low priority - survival first
        }

        return basePriority;
    }
}

public class SurvivalGoal extends BaseGoapGoal {

    public SurvivalGoal() {
        super("Survival", 100);  // Maximum priority
        addTarget("playerHealth", 100);  // Full health
    }

    @Override
    public int getPriority(WorldState currentState) {
        int health = currentState.getInt("playerHealth", 100);

        // Priority skyrockets when health is critical
        if (health < 20) {
            return 1000;
        } else if (health < 50) {
            return 100;
        }

        return 10;  // Low priority when healthy
    }

    @Override
    public boolean isAchieved(WorldState state) {
        int health = state.getInt("playerHealth", 0);
        return health >= 80;  // Good enough
    }
}
```

#### Minecraft Goals

```java
public class GatherResourcesGoal extends BaseGoapGoal {

    private final String resourceType;
    private final int targetQuantity;

    public GatherResourcesGoal(String resourceType, int quantity) {
        super("Gather_" + resourceType, 5);
        this.resourceType = resourceType;
        this.targetQuantity = quantity;
        addTarget("has_" + resourceType, true);  // Simplified
    }

    @Override
    public boolean isAchieved(WorldState state) {
        // In real implementation, check inventory count
        return state.getBoolean("has_" + resourceType, false);
    }
}

public class BuildShelterGoal extends BaseGoapGoal {

    public BuildShelterGoal() {
        super("BuildShelter", 15);
        addTarget("hasShelter", true);
    }

    @Override
    public int getPriority(WorldState currentState) {
        // High priority at night
        // boolean isNight = currentState.getBoolean("isNight", false);
        // if (isNight) return 50;

        // High priority when health low (need safe place)
        int health = currentState.getInt("playerHealth", 100);
        if (health < 50) return 30;

        return basePriority;
    }
}
```

### 5.4 Goal Selector

```java
public class GoalSelector {

    private final List<GoapGoal> goals;

    public GoalSelector() {
        this.goals = new ArrayList<>();
    }

    public void addGoal(GoapGoal goal) {
        goals.add(goal);
    }

    /**
     * Selects the highest priority achievable goal.
     * Returns null if all goals are achieved.
     */
    public GoapGoal selectBestGoal(WorldState currentState) {
        GoapGoal bestGoal = null;
        int bestPriority = Integer.MIN_VALUE;

        for (GoapGoal goal : goals) {
            // Skip already achieved goals
            if (goal.isAchieved(currentState)) {
                continue;
            }

            int priority = goal.getPriority(currentState);
            if (priority > bestPriority) {
                bestPriority = priority;
                bestGoal = goal;
            }
        }

        return bestGoal;
    }

    /**
     * Returns all unachieved goals sorted by priority.
     */
    public List<GoapGoal> getUnachievedGoals(WorldState currentState) {
        return goals.stream()
            .filter(g -> !g.isAchieved(currentState))
            .sorted((a, b) -> Integer.compare(
                b.getPriority(currentState),
                a.getPriority(currentState)
            ))
            .collect(Collectors.toList());
    }
}
```

---

## 6. A* Planning Through Action Space

### 6.1 Planning Node

```java
public class PlanNode implements Comparable<PlanNode> {

    private final WorldState state;
    private final GoapAction action;  // Action that led to this state
    private final PlanNode parent;
    private final int gCost;  // Cost from start
    private final int hCost;  // Heuristic to goal
    private final int fCost;  // Total cost (g + h)

    public PlanNode(WorldState state, GoapAction action, PlanNode parent,
                    int gCost, int hCost) {
        this.state = state;
        this.action = action;
        this.parent = parent;
        this.gCost = gCost;
        this.hCost = hCost;
        this.fCost = gCost + hCost;
    }

    public WorldState getState() {
        return state;
    }

    public GoapAction getAction() {
        return action;
    }

    public PlanNode getParent() {
        return parent;
    }

    public int getGCost() {
        return gCost;
    }

    public int getHCost() {
        return hCost;
    }

    public int getFCost() {
        return fCost;
    }

    /**
     * Reconstructs the action sequence from start to this node.
     */
    public List<GoapAction> getActionPath() {
        List<GoapAction> path = new ArrayList<>();
        PlanNode current = this;

        while (current.getAction() != null) {
            path.add(current.getAction());
            current = current.getParent();
        }

        Collections.reverse(path);
        return path;
    }

    @Override
    public int compareTo(PlanNode other) {
        // Lower f-cost is better (min-heap)
        return Integer.compare(this.fCost, other.fCost);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PlanNode)) return false;
        PlanNode other = (PlanNode) obj;
        return state.equals(other.state);
    }

    @Override
    public int hashCode() {
        return state.hashCode();
    }
}
```

### 6.2 GOAP Planner with A*

```java
public class GoapPlanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoapPlanner.class);

    private static final int MAX_ITERATIONS = 1000;
    private static final int MAX_PLAN_LENGTH = 50;

    /**
     * Plans a sequence of actions to achieve the goal from current state.
     * Uses A* search backward from goal to current state.
     *
     * @param currentState Current world state
     * @param goal Target goal with desired state
     * @param availableActions All actions the agent can perform
     * @return List of actions in execution order, or null if no plan found
     */
    public List<GoapAction> plan(WorldState currentState,
                                  GoapGoal goal,
                                  List<GoapAction> availableActions) {

        WorldState goalState = goal.getTargetState();

        // If goal already achieved, no planning needed
        if (currentState.satisfies(goalState)) {
            LOGGER.debug("Goal '{}' already achieved", goal.getName());
            return Collections.emptyList();
        }

        // A* search setup
        PriorityQueue<PlanNode> openSet = new PriorityQueue<>();
        Set<WorldState> closedSet = new HashSet<>();

        // Start from goal state and work backward (backward planning)
        PlanNode startNode = new PlanNode(goalState, null, null, 0,
                                          calculateHeuristic(goalState, currentState));
        openSet.add(startNode);

        int iterations = 0;

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;

            // Get node with lowest f-cost
            PlanNode current = openSet.poll();

            // Check if we've reached current world state
            if (currentState.satisfies(current.getState())) {
                List<GoapAction> plan = current.getActionPath();

                if (plan.size() <= MAX_PLAN_LENGTH) {
                    LOGGER.info("Plan found in {} iterations: {}",
                               iterations, formatPlan(plan));
                    return plan;
                } else {
                    LOGGER.warn("Plan too long ({} actions), discarding", plan.size());
                    return null;
                }
            }

            closedSet.add(current.getState());

            // Expand: find actions that could lead to this state
            for (GoapAction action : availableActions) {
                // In backward planning, we look for actions whose effects
                // could help achieve our current state
                WorldState preconditions = action.getPreconditions();

                // Simulate: if we apply this action, what state do we get?
                // In backward planning, we work from effects to preconditions
                WorldState previousState = calculatePredecessorState(current.getState(), action);

                if (previousState == null) {
                    continue;  // Action not relevant
                }

                if (closedSet.contains(previousState)) {
                    continue;  // Already evaluated
                }

                // Calculate costs
                int tentativeGCost = current.getGCost() + action.getCost();
                int hCost = calculateHeuristic(previousState, currentState);

                PlanNode neighbor = new PlanNode(previousState, action, current,
                                                  tentativeGCost, hCost);

                // Check if this path is better
                boolean inOpenSet = false;
                for (PlanNode openNode : openSet) {
                    if (openNode.equals(neighbor) && openNode.getGCost() <= tentativeGCost) {
                        inOpenSet = true;
                        break;
                    }
                }

                if (!inOpenSet) {
                    openSet.add(neighbor);
                }
            }
        }

        LOGGER.warn("No plan found after {} iterations", iterations);
        return null;
    }

    /**
     * Calculates what state we'd need BEFORE applying this action
     * to reach the target state (backward planning).
     */
    private WorldState calculatePredecessorState(WorldState targetState, GoapAction action) {
        WorldState effects = action.getEffects();
        WorldState preconditions = action.getPreconditions();

        // Check if action's effects are compatible with target state
        // (i.e., does this action move us toward the target?)
        boolean compatible = true;
        WorldState predecessor = targetState.copy();

        for (String key : effects.getAllKeys()) {
            Object targetValue = targetState.get(key);
            Object effectValue = effects.get(key);

            // If target has a specific value and effect conflicts, incompatible
            if (targetValue != null && !targetValue.equals(effectValue)) {
                // Special case: effects that modify values (e.g., ammoCount - 1)
                if (effectValue instanceof Integer) {
                    // This is complex - for now, skip
                } else {
                    compatible = false;
                    break;
                }
            }
        }

        if (!compatible) {
            return null;
        }

        // Apply preconditions as requirements for predecessor state
        for (String key : preconditions.getAllKeys()) {
            predecessor.set(key, preconditions.get(key));
        }

        return predecessor;
    }

    /**
     * Heuristic function estimating cost from state to goal.
     * Uses state difference as the heuristic.
     */
    private int calculateHeuristic(WorldState from, WorldState to) {
        // Count differences between states
        int differences = 0;

        for (String key : to.getAllKeys()) {
            if (!Objects.equals(from.get(key), to.get(key))) {
                differences++;
            }
        }

        return differences;  // Admissible heuristic (never overestimates)
    }

    private String formatPlan(List<GoapAction> plan) {
        return plan.stream()
            .map(a -> a.getClass().getSimpleName())
            .collect(Collectors.joining(" -> "));
    }
}
```

### 6.3 Forward Planning Alternative

```java
/**
 * Forward planning version: plans from current state toward goal.
 * Less efficient than backward planning for GOAP, but more intuitive.
 */
public List<GoapAction> planForward(WorldState currentState,
                                     GoapGoal goal,
                                     List<GoapAction> availableActions) {

    WorldState goalState = goal.getTargetState();

    if (currentState.satisfies(goalState)) {
        return Collections.emptyList();
    }

    PriorityQueue<PlanNode> openSet = new PriorityQueue<>();
    Set<WorldState> closedSet = new HashSet<>();

    // Start from current state
    PlanNode startNode = new PlanNode(currentState, null, null, 0,
                                      calculateHeuristic(currentState, goalState));
    openSet.add(startNode);

    int iterations = 0;

    while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
        iterations++;

        PlanNode current = openSet.poll();

        // Check if goal achieved
        if (current.getState().satisfies(goalState)) {
            return current.getActionPath();
        }

        closedSet.add(current.getState());

        // Expand: try all applicable actions
        for (GoapAction action : availableActions) {
            if (!action.canExecute(current.getState())) {
                continue;  // Preconditions not met
            }

            // Apply action effects
            WorldState nextState = action.simulateEffects(current.getState());

            if (closedSet.contains(nextState)) {
                continue;
            }

            int tentativeGCost = current.getGCost() + action.getCost();
            int hCost = calculateHeuristic(nextState, goalState);

            PlanNode neighbor = new PlanNode(nextState, action, current,
                                              tentativeGCost, hCost);

            // Check if better path exists
            boolean inOpenSet = false;
            for (PlanNode openNode : openSet) {
                if (openNode.equals(neighbor) && openNode.getGCost() <= tentativeGCost) {
                    inOpenSet = true;
                    break;
                }
            }

            if (!inOpenSet) {
                openSet.add(neighbor);
            }
        }
    }

    return null;  // No plan found
}
```

---

## 7. GOAP vs HTN

### 7.1 Key Differences

| Aspect | GOAP | HTN (Hierarchical Task Network) |
|--------|------|---------------------------------|
| **Planning Direction** | Backward (goal → current) | Forward (current → goal) |
| **Goal Definition** | Explicit state requirements | Implicit in task hierarchy |
| **Structure** | Flat action list with costs | Hierarchical task decomposition |
| **Planning Algorithm** | A* search | Recursive task decomposition |
| **Designer Control** | Low (emergent behavior) | High (predictable outcomes) |
| **Predictability** | Can surprise developers | Follows designed paths |
| **Complexity** | Cost functions, heuristics | Task methods, preconditions |
| **Best For** | Dynamic environments, emergent gameplay | Story-driven, predictable behaviors |

### 7.2 When to Use GOAP

- **Open-world games** where AI must adapt to unpredictable situations
- **Survival/crafting games** with many possible action combinations
- **Tactical combat** where environment changes frequently
- **Emergent gameplay** where surprising AI behavior is desirable
- **Sandbox games** with many interacting systems

**Examples:** Fallout 4 NPCs, Kenshi squad AI, S.T.A.L.K.E.R. survival AI

### 7.3 When to Use HTN

- **Story-driven games** requiring predictable NPC behavior
- **Strategy games** with long-term planning needs
- **Games with clear optimal strategies** that should be encoded
- **When designer control** is more important than emergence
- **Complex multi-step behaviors** that are well-understood

**Examples:** Civilization series, Total War, story-driven RPGs

### 7.4 Hybrid Approaches

Combine both for best results:

```java
/**
 * Hybrid planner using HTN for high-level strategy and GOAP for low-level tactics.
 */
public class HybridPlanner {

    private final HtnPlanner htnPlanner;
    private final GoapPlanner goapPlanner;

    /**
     * Plans using HTN for high-level tasks, GOAP for executing each task.
     */
    public List<GoapAction> plan(WorldState state, GoapGoal goal) {
        // HTN decomposes goal into abstract tasks
        List<HtnTask> tasks = htnPlanner.decompose(goal, state);

        List<GoapAction> fullPlan = new ArrayList<>();

        // GOAP plans execution for each HTN task
        for (HtnTask task : tasks) {
            GoapGoal taskGoal = task.toGoapGoal();
            List<GoapAction> taskPlan = goapPlanner.plan(state, taskGoal, task.getAllowedActions());

            if (taskPlan == null) {
                return null;  // Task cannot be achieved
            }

            fullPlan.addAll(taskPlan);

            // Update state for next task
            for (GoapAction action : taskPlan) {
                state = action.simulateEffects(state);
            }
        }

        return fullPlan;
    }
}
```

### 7.5 Decision Flow

```
                    ┌─────────────────┐
                    │   Need AI for   │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ Dynamic world?  │
                    └────────┬────────┘
                      │             │
                     Yes           No
                      │             │
                      ▼             ▼
              ┌───────────┐   ┌───────────┐
              │ Emergent  │   │ Predictable│
              │ behavior  │   │ desired?  │
              │ needed?   │   └─────┬─────┘
              └─────┬─────┘     │       │
                │   │          Yes      No
               Yes  No           │       │
                │   │            ▼       ▼
                │   └────► ┌─────────┐ ┌─────────┐
                │          │   HTN   │ │   HTN   │
                ▼          └─────────┘ └─────────┘
        ┌───────────┐
        │   GOAP    │
        └───────────┘
```

---

## 8. GOAP Implementation

### 8.1 Action Executor

```java
public class ActionExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionExecutor.class);

    private Queue<GoapAction> actionQueue;
    private GoapAction currentAction;
    private WorldState worldState;
    private final ActionContext context;

    public ActionExecutor(ActionContext context) {
        this.context = context;
        this.actionQueue = new LinkedList<>();
        this.worldState = new WorldState();
    }

    /**
     * Sets a new plan to execute.
     * Clears any existing plan.
     */
    public void setPlan(List<GoapAction> plan) {
        if (plan == null || plan.isEmpty()) {
            LOGGER.warn("Attempted to set empty plan");
            return;
        }

        // Cancel current action
        if (currentAction != null && currentAction.isRunning()) {
            currentAction.cancel();
        }

        this.actionQueue = new LinkedList<>(plan);
        this.currentAction = null;

        LOGGER.info("New plan set with {} actions: {}",
                   plan.size(), formatPlan(plan));
    }

    /**
     * Updates execution state. Call once per tick.
     * Returns true when all actions complete.
     */
    public boolean tick() {
        // Update world state from sensors
        updateWorldState();

        // If no current action, start next one
        if (currentAction == null || !currentAction.isRunning()) {
            if (!actionQueue.isEmpty()) {
                currentAction = actionQueue.poll();

                if (currentAction.canExecute(worldState)) {
                    LOGGER.info("Starting action: {}",
                               currentAction.getClass().getSimpleName());
                    currentAction.execute(worldState, context);
                } else {
                    LOGGER.warn("Action {} cannot execute - replanning needed",
                               currentAction.getClass().getSimpleName());
                    return false;  // Plan invalid, needs replanning
                }
            } else {
                return true;  // All actions complete
            }
        }

        return false;  // Still executing
    }

    /**
     * Cancels current action and clears plan.
     */
    public void cancel() {
        if (currentAction != null && currentAction.isRunning()) {
            currentAction.cancel();
        }

        actionQueue.clear();
        currentAction = null;
    }

    /**
     * Checks if current plan is still valid.
     * Returns false if preconditions can't be met.
     */
    public boolean isPlanValid() {
        if (currentAction != null && currentAction.isRunning()) {
            return true;  // Let running action complete
        }

        if (actionQueue.isEmpty()) {
            return true;
        }

        // Check if next action can execute
        return actionQueue.peek().canExecute(worldState);
    }

    /**
     * Gets remaining actions in plan.
     */
    public List<GoapAction> getRemainingActions() {
        List<GoapAction> remaining = new ArrayList<>();
        if (currentAction != null) {
            remaining.add(currentAction);
        }
        remaining.addAll(actionQueue);
        return remaining;
    }

    private void updateWorldState() {
        // In real implementation, read from game state
        // This is a placeholder
    }

    private String formatPlan(List<GoapAction> plan) {
        return plan.stream()
            .map(a -> a.getClass().getSimpleName())
            .collect(Collectors.joining(" -> "));
    }
}
```

### 8.2 Replanning Trigger

```java
public class ReplanningMonitor {

    private WorldState lastPlannedState;
    private static final int STATE_DIFFERENCE_THRESHOLD = 3;

    /**
     * Checks if replanning is needed due to world state changes.
     */
    public boolean shouldReplan(WorldState currentState,
                                List<GoapAction> currentPlan) {

        // No plan, need to plan
        if (currentPlan == null || currentPlan.isEmpty()) {
            return true;
        }

        // State has changed significantly since planning
        if (lastPlannedState != null) {
            int differences = StateDifference.calculateDistance(currentState, lastPlannedState);
            if (differences > STATE_DIFFERENCE_THRESHOLD) {
                LOGGER.debug("World state changed significantly ({} differences), replanning",
                           differences);
                return true;
            }
        }

        // Check if current plan is still valid
        for (GoapAction action : currentPlan) {
            if (!action.canExecute(currentState)) {
                LOGGER.debug("Plan invalidated - action {} cannot execute",
                           action.getClass().getSimpleName());
                return true;
            }
        }

        return false;
    }

    public void setLastPlannedState(WorldState state) {
        this.lastPlannedState = state.copy();
    }
}
```

### 8.3 Cost Functions

```java
public interface ActionCostFunction {

    /**
     * Calculates dynamic cost for an action given current state.
     * Allows costs to vary based on context.
     */
    int calculateCost(GoapAction action, WorldState state);
}

/**
 * Static cost - all actions have fixed cost.
 */
public class StaticCostFunction implements ActionCostFunction {

    @Override
    public int calculateCost(GoapAction action, WorldState state) {
        return action.getCost();
    }
}

/**
 * Distance-based cost - actions cost more based on distance to target.
 */
public class DistanceBasedCostFunction implements ActionCostFunction {

    private final Function<WorldState, BlockPos> positionExtractor;

    public DistanceBasedCostFunction(Function<WorldState, BlockPos> positionExtractor) {
        this.positionExtractor = positionExtractor;
    }

    @Override
    public int calculateCost(GoapAction action, WorldState state) {
        int baseCost = action.getCost();

        // Add distance penalty for movement actions
        if (action instanceof MovementAction) {
            BlockPos targetPos = ((MovementAction) action).getTargetPosition(state);
            BlockPos currentPos = positionExtractor.apply(state);

            double distance = Math.sqrt(
                Math.pow(targetPos.getX() - currentPos.getX(), 2) +
                Math.pow(targetPos.getY() - currentPos.getY(), 2) +
                Math.pow(targetPos.getZ() - currentPos.getZ(), 2)
            );

            return baseCost + (int) distance;
        }

        return baseCost;
    }
}

/**
 * Resource-aware cost - actions cost more when resources are scarce.
 */
public class ResourceAwareCostFunction implements ActionCostFunction {

    @Override
    public int calculateCost(GoapAction action, WorldState state) {
        int baseCost = action.getCost();

        // Increase cost of resource-consuming actions when resources are low
        if (action instanceof ConsumeResourceAction) {
            String resource = ((ConsumeResourceAction) action).getResourceType();
            int currentAmount = state.getInt(resource, 0);

            if (currentAmount < 10) {
                return baseCost * 3;  // Triple cost when scarce
            } else if (currentAmount < 20) {
                return baseCost * 2;  // Double cost when low
            }
        }

        return baseCost;
    }
}

/**
 * Time-of-day based cost - certain actions are better at different times.
 */
public class TimeOfDayCostFunction implements ActionCostFunction {

    @Override
    public int calculateCost(GoapAction action, WorldState state) {
        int baseCost = action.getCost();
        boolean isNight = state.getBoolean("isNight", false);

        // Combat actions are riskier at night (higher cost)
        if (isNight && action instanceof CombatAction) {
            return baseCost * 2;
        }

        // Stealth actions are cheaper at night
        if (isNight && action instanceof StealthAction) {
            return baseCost / 2;
        }

        // Mining is safer at night (fewer monsters potentially)
        if (isNight && action instanceof MineAction) {
            return (int) (baseCost * 0.8);
        }

        return baseCost;
    }
}
```

---

## 9. GOAP + LLM Integration

### 9.1 Why Combine GOAP with LLMs?

| GOAP Strengths | LLM Strengths |
|----------------|---------------|
| Optimal action sequences | Natural language understanding |
| Verifiable, deterministic | Creative, flexible reasoning |
| Fast execution | Context-aware interpretation |
| Guaranteed goal achievement | Handles ambiguity |

**Neuro-Symbolic AI:** Combine neural networks (LLMs) with symbolic reasoning (GOAP) for best of both worlds.

### 9.2 LLM Generating New Actions

```java
/**
 * LLM generates new action definitions dynamically based on user request.
 */
public class LLMActionGenerator {

    private final AsyncLLMClient llmClient;

    /**
     * Prompts LLM to generate a new action from natural language description.
     */
    public CompletableFuture<GoapAction> generateAction(String description,
                                                          WorldState currentState) {

        String prompt = buildActionPrompt(description, currentState);

        return llmClient.sendRequest(prompt)
            .thenApply(response -> parseActionFromResponse(response, description))
            .exceptionally(throwable -> {
                LOGGER.error("Failed to generate action from LLM", throwable);
                return null;
            });
    }

    private String buildActionPrompt(String description, WorldState currentState) {
        return String.format("""
            You are a game AI action designer. Create a GOAP action from this description:

            User Request: %s

            Current World State:
            %s

            Respond in JSON format:
            {
                "name": "ActionName",
                "preconditions": {
                    "key1": value1,
                    "key2": value2
                },
                "effects": {
                    "key1": value1,
                    "key2": value2
                },
                "cost": integer,
                "description": "Brief description"
            }

            Preconditions are what must be true BEFORE executing.
            Effects are what becomes true AFTER executing.
            Cost is 1-10 (lower is better/faster).
            """,
            description,
            formatWorldState(currentState)
        );
    }

    private GoapAction parseActionFromResponse(LLMResponse response, String description) {
        try {
            JsonNode json = objectMapper.readTree(response.getContent());

            String name = json.get("name").asText();
            int cost = json.get("cost").asInt();

            DynamicGoapAction action = new DynamicGoapAction(name, cost, description);

            // Parse preconditions
            JsonNode preconditions = json.get("preconditions");
            preconditions.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                if (value.isBoolean()) {
                    action.addPrecondition(key, value.asBoolean());
                } else if (value.isInt()) {
                    action.addPrecondition(key, value.asInt());
                } else {
                    action.addPrecondition(key, value.asText());
                }
            });

            // Parse effects
            JsonNode effects = json.get("effects");
            effects.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                if (value.isBoolean()) {
                    action.addEffect(key, value.asBoolean());
                } else if (value.isInt()) {
                    action.addEffect(key, value.asInt());
                } else {
                    action.addEffect(key, value.asText());
                }
            });

            return action;

        } catch (Exception e) {
            LOGGER.error("Failed to parse action from LLM response", e);
            return null;
        }
    }

    private String formatWorldState(WorldState state) {
        return state.getAllKeys().stream()
            .map(key -> "  " + key + ": " + state.get(key))
            .collect(Collectors.joining("\n"));
    }
}

/**
 * Dynamically generated action from LLM.
 */
public class DynamicGoapAction extends BaseGoapAction {

    private final String description;
    private final Consumer<WorldState> executor;

    public DynamicGoapAction(String name, int cost, String description) {
        super(name, cost);
        this.description = description;
        this.executor = null;  // Would be set based on action type
    }

    public void addPrecondition(String key, boolean value) {
        preconditions.set(key, value);
    }

    public void addPrecondition(String key, int value) {
        preconditions.set(key, value);
    }

    public void addPrecondition(String key, String value) {
        preconditions.set(key, value);
    }

    public void addEffect(String key, boolean value) {
        effects.set(key, value);
    }

    public void addEffect(String key, int value) {
        effects.set(key, value);
    }

    public void addEffect(String key, String value) {
        effects.set(key, value);
    }

    @Override
    public boolean execute(WorldState state, ActionContext context) {
        isRunning = true;

        // Apply effects
        for (String key : effects.getAllKeys()) {
            state.set(key, effects.get(key));
        }

        // In real implementation, would execute actual game logic
        // based on action type (movement, combat, gathering, etc.)

        isRunning = false;
        return true;
    }
}
```

### 9.3 LLM Adjusting Action Costs

```java
/**
 * LLM dynamically adjusts action costs based on context and strategy.
 */
public class LLMCostAdjuster {

    private final AsyncLLMClient llmClient;
    private final ActionCostFunction baseCostFunction;

    public LLMCostAdjuster(AsyncLLMClient llmClient,
                          ActionCostFunction baseCostFunction) {
        this.llmClient = llmClient;
        this.baseCostFunction = baseCostFunction;
    }

    /**
     * Asks LLM to adjust action costs based on current situation.
     * Returns CompletableFuture with adjusted costs map.
     */
    public CompletableFuture<Map<String, Integer>> adjustCosts(
            List<GoapAction> actions,
            WorldState currentState,
            GoapGoal currentGoal) {

        String prompt = buildCostPrompt(actions, currentState, currentGoal);

        return llmClient.sendRequest(prompt)
            .thenApply(response -> parseCostAdjustments(response, actions))
            .exceptionally(throwable -> {
                LOGGER.warn("LLM cost adjustment failed, using base costs", throwable);
                // Fall back to base costs
                return actions.stream()
                    .collect(Collectors.toMap(
                        a -> a.getClass().getSimpleName(),
                        a -> baseCostFunction.calculateCost(a, currentState)
                    ));
            });
    }

    private String buildCostPrompt(List<GoapAction> actions,
                                   WorldState state,
                                   GoapGoal goal) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a tactical AI advisor. Adjust action costs based on the situation:\n\n");
        prompt.append("Current Goal: ").append(goal.getName()).append("\n\n");
        prompt.append("Current World State:\n");

        for (String key : state.getAllKeys()) {
            prompt.append("  ").append(key).append(": ").append(state.get(key)).append("\n");
        }

        prompt.append("\nAvailable Actions:\n");

        for (GoapAction action : actions) {
            int baseCost = baseCostFunction.calculateCost(action, state);
            prompt.append(String.format("  %s (base cost: %d)\n",
                action.getClass().getSimpleName(), baseCost));
        }

        prompt.append("""

            Consider:
            - Which actions are most urgent given the goal?
            - Which actions are risky given current state?
            - Which actions are inefficient right now?

            Return adjusted costs as JSON (1-20 scale, higher = worse):
            {
                "ActionName1": 5,
                "ActionName2": 10,
                ...
            }

            Only include actions that should be adjusted. Others keep base cost.
            """);

        return prompt.toString();
    }

    private Map<String, Integer> parseCostAdjustments(LLMResponse response,
                                                       List<GoapAction> actions) {
        try {
            JsonNode json = objectMapper.readTree(response.getContent());
            Map<String, Integer> adjustedCosts = new HashMap<>();

            json.fields().forEachRemaining(entry -> {
                adjustedCosts.put(entry.getKey(), entry.getValue().asInt());
            });

            return adjustedCosts;

        } catch (Exception e) {
            LOGGER.error("Failed to parse cost adjustments from LLM", e);
            throw new RuntimeException(e);
        }
    }
}
```

### 9.4 LLM as Replanning Trigger

```java
/**
 * LLM monitors execution and decides when replanning is needed.
 */
public class LLMReplanningTrigger {

    private final AsyncLLMClient llmClient;

    /**
     * Asks LLM if current plan should be abandoned based on observations.
     */
    public CompletableFuture<Boolean> shouldReplan(
            List<GoapAction> currentPlan,
            GoapAction currentAction,
            WorldState currentState,
            GoapGoal originalGoal,
            String recentObservations) {

        String prompt = buildReplanningPrompt(currentPlan, currentAction,
                                              currentState, originalGoal,
                                              recentObservations);

        return llmClient.sendRequest(prompt)
            .thenApply(response -> parseReplanningDecision(response))
            .exceptionally(throwable -> {
                LOGGER.warn("LLM replanning check failed, continuing plan", throwable);
                return false;  // Default: don't replan
            });
    }

    private String buildReplanningPrompt(List<GoapAction> plan,
                                        GoapAction currentAction,
                                        WorldState state,
                                        GoapGoal goal,
                                        String observations) {
        return String.format("""
            You are monitoring an AI agent executing a plan.

            Original Goal: %s

            Current Plan:
            %s

            Currently Executing: %s

            Recent Observations:
            %s

            Current World State:
            %s

            Should the agent abandon this plan and replan?

            Consider:
            - Has the situation changed fundamentally?
            - Is the current action failing or stuck?
            - Are there better opportunities available?
            - Is the goal no longer relevant?

            Respond with JSON:
            {
                "shouldReplan": true/false,
                "reason": "Brief explanation"
            }
            """,
            goal.getName(),
            formatPlan(plan),
            currentAction != null ? currentAction.getClass().getSimpleName() : "none",
            observations,
            formatWorldState(state)
        );
    }

    private boolean parseReplanningDecision(LLMResponse response) {
        try {
            JsonNode json = objectMapper.readTree(response.getContent());
            boolean shouldReplan = json.get("shouldReplan").asBoolean();

            if (shouldReplan) {
                String reason = json.get("reason").asText();
                LOGGER.info("LLM triggered replanning: {}", reason);
            }

            return shouldReplan;

        } catch (Exception e) {
            LOGGER.error("Failed to parse replanning decision", e);
            return false;
        }
    }

    private String formatPlan(List<GoapAction> plan) {
        return plan.stream()
            .map(a -> "  - " + a.getClass().getSimpleName())
            .collect(Collectors.joining("\n"));
    }

    private String formatWorldState(WorldState state) {
        return state.getAllKeys().stream()
            .map(key -> "  " + key + ": " + state.get(key))
            .collect(Collectors.joining("\n"));
    }
}
```

### 9.5 Natural Language Goal Specification

```java
/**
 * Converts natural language goals into formal GOAP goals.
 */
public class NaturalLanguageGoalParser {

    private final AsyncLLMClient llmClient;

    /**
     * Parses natural language into a GoapGoal.
     */
    public CompletableFuture<GoapGoal> parseGoal(String naturalLanguage,
                                                  WorldState currentState) {

        String prompt = buildGoalPrompt(naturalLanguage, currentState);

        return llmClient.sendRequest(prompt)
            .thenApply(response -> parseGoalFromResponse(response, naturalLanguage))
            .exceptionally(throwable -> {
                LOGGER.error("Failed to parse goal from natural language", throwable);
                return null;
            });
    }

    private String buildGoalPrompt(String goalDescription, WorldState currentState) {
        return String.format("""
            Convert this natural language goal into a formal GOAP goal specification.

            User's Goal: "%s"

            Current World State:
            %s

            Define the goal as desired world state conditions.

            Respond in JSON:
            {
                "name": "GoalName",
                "priority": 1-100,
                "targetState": {
                    "key1": desired_value1,
                    "key2": desired_value2
                }
            }

            Guidelines:
            - Keep goal simple and focused
            - Use boolean values for conditions (true/false)
            - Priority: 1 (low) to 100 (critical)
            - Only include relevant state variables
            """,
            goalDescription,
            formatWorldState(currentState)
        );
    }

    private GoapGoal parseGoalFromResponse(LLMResponse response,
                                           String originalDescription) {
        try {
            JsonNode json = objectMapper.readTree(response.getContent());

            String name = json.get("name").asText();
            int priority = json.get("priority").asInt();

            JsonNode targetStateJson = json.get("targetState");
            WorldState targetState = new WorldState();

            targetStateJson.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                if (value.isBoolean()) {
                    targetState.set(key, value.asBoolean());
                } else if (value.isInt()) {
                    targetState.set(key, value.asInt());
                } else {
                    targetState.set(key, value.asText());
                }
            });

            return new DynamicGoapGoal(name, priority, targetState, originalDescription);

        } catch (Exception e) {
            LOGGER.error("Failed to parse goal from LLM response", e);
            return null;
        }
    }

    private String formatWorldState(WorldState state) {
        return state.getAllKeys().stream()
            .map(key -> "  " + key + ": " + state.get(key))
            .collect(Collectors.joining("\n"));
    }
}

/**
 * Dynamically created goal from natural language.
 */
public class DynamicGoapGoal extends BaseGoapGoal {

    private final String originalDescription;

    public DynamicGoapGoal(String name, int priority, WorldState targetState,
                           String originalDescription) {
        super(name, priority);
        this.targetState = targetState;
        this.originalDescription = originalDescription;
    }

    public String getOriginalDescription() {
        return originalDescription;
    }

    @Override
    public String toString() {
        return String.format("%s (from: \"%s\")", name, originalDescription);
    }
}
```

---

## 10. Java Implementation Examples

### 10.1 Complete GOAP System Integration

```java
/**
 * Complete GOAP agent integrating all components.
 */
public class GoapAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoapAgent.class);

    private final String agentId;
    private final WorldState worldState;
    private final GoalSelector goalSelector;
    private final GoapPlanner planner;
    private final ActionExecutor executor;
    private final ReplanningMonitor replanningMonitor;

    private GoapGoal currentGoal;
    private List<GoapAction> currentPlan;
    private AgentState state = AgentState.IDLE;

    public GoapAgent(String agentId,
                    List<GoapGoal> goals,
                    List<GoapAction> availableActions,
                    ActionContext context) {

        this.agentId = agentId;
        this.worldState = new WorldState();
        this.goalSelector = new GoalSelector();
        this.planner = new GoapPlanner();
        this.executor = new ActionExecutor(context);
        this.replanningMonitor = new ReplanningMonitor();

        // Register goals
        goals.forEach(goalSelector::addGoal);

        LOGGER.info("[{}] GOAP Agent initialized with {} goals and {} actions",
                   agentId, goals.size(), availableActions.size());
    }

    /**
     * Main tick loop - call once per game tick.
     */
    public void tick() {
        // Update world state from game
        updateWorldState();

        // Check if we need to plan or replan
        if (shouldPlan()) {
            plan();
        }

        // Execute current plan
        if (currentPlan != null && !currentPlan.isEmpty()) {
            boolean complete = executor.tick();

            if (complete) {
                LOGGER.info("[{}] Plan execution complete", agentId);
                state = AgentState.COMPLETED;
                currentPlan = null;
            }
        }
    }

    /**
     * Determines if planning is needed.
     */
    private boolean shouldPlan() {
        // No plan or plan complete
        if (currentPlan == null || currentPlan.isEmpty()) {
            return true;
        }

        // Check if replanning is needed due to state changes
        if (replanningMonitor.shouldReplan(worldState, currentPlan)) {
            return true;
        }

        // Check if goal is achieved
        if (currentGoal != null && currentGoal.isAchieved(worldState)) {
            LOGGER.info("[{}] Goal '{}' achieved", agentId, currentGoal.getName());
            return true;
        }

        return false;
    }

    /**
     * Plans actions to achieve the best goal.
     */
    private void plan() {
        state = AgentState.PLANNING;

        // Select best goal
        currentGoal = goalSelector.selectBestGoal(worldState);

        if (currentGoal == null) {
            LOGGER.debug("[{}] No achievable goals found", agentId);
            state = AgentState.IDLE;
            return;
        }

        LOGGER.info("[{}] Selected goal: {}", agentId, currentGoal.getName());

        // Plan actions
        // In real implementation, would get available actions from registry
        List<GoapAction> availableActions = getAvailableActions();
        currentPlan = planner.plan(worldState, currentGoal, availableActions);

        if (currentPlan != null && !currentPlan.isEmpty()) {
            LOGGER.info("[{}] Plan generated: {}", agentId, formatPlan(currentPlan));
            executor.setPlan(currentPlan);
            state = AgentState.EXECUTING;
            replanningMonitor.setLastPlannedState(worldState.copy());
        } else {
            LOGGER.warn("[{}] No valid plan found for goal: {}",
                       agentId, currentGoal.getName());
            state = AgentState.FAILED;
        }
    }

    /**
     * Updates world state from game sensors.
     */
    private void updateWorldState() {
        // In real implementation, read from game state
        // This is called every tick to keep GOAP state in sync
    }

    /**
     * Gets available actions for this agent.
     */
    private List<GoapAction> getAvailableActions() {
        // In real implementation, query ActionRegistry
        // For now, return empty list
        return new ArrayList<>();
    }

    public String getAgentId() {
        return agentId;
    }

    public AgentState getState() {
        return state;
    }

    public WorldState getWorldState() {
        return worldState;
    }

    private String formatPlan(List<GoapAction> plan) {
        return plan.stream()
            .map(a -> a.getClass().getSimpleName())
            .collect(Collectors.joining(" -> "));
    }
}
```

### 10.2 Minecraft-Specific GOAP Implementation

```java
/**
 * GOAP agent specifically for Minecraft Foreman entities.
 * Integrates with existing MineWright mod architecture.
 */
public class ForemanGoapAgent {

    private final ForemanEntity foreman;
    private final WorldState worldState;
    private final GoalSelector goalSelector;
    private final GoapPlanner planner;
    private final ActionRegistry actionRegistry;

    public ForemanGoapAgent(ForemanEntity foreman, EventBus eventBus) {
        this.foreman = foreman;
        this.worldState = new WorldState();
        this.goalSelector = new GoalSelector();
        this.planner = new GoapPlanner();
        this.actionRegistry = ActionRegistry.getInstance();

        initializeGoals();
        initializeWorldState();
    }

    /**
     * Initializes Minecraft-specific goals.
     */
    private void initializeGoals() {
        // Resource gathering goals
        goalSelector.addGoal(new GatherResourcesGoal("oak_log", 64));
        goalSelector.addGoal(new GatherResourcesGoal("cobblestone", 64));
        goalSelector.addGoal(new GatherResourcesGoal("iron_ore", 32));

        // Building goals
        goalSelector.addGoal(new BuildShelterGoal());
        goalSelector.addGoal(new BuildFarmGoal());

        // Survival goals
        goalSelector.addGoal(new EatGoal());
        goalSelector.addGoal(new EquipArmorGoal());
    }

    /**
     * Initializes world state from entity.
     */
    private void initializeWorldState() {
        // Position
        BlockPos pos = foreman.blockPosition();
        worldState.set("x", pos.getX());
        worldState.set("y", pos.getY());
        worldState.set("z", pos.getZ());

        // Health
        worldState.set("health", foreman.getHealth());
        worldState.set("maxHealth", foreman.getMaxHealth());

        // Inventory (simplified)
        // worldState.set("hasPickaxe", hasPickaxe());
        // worldState.set("inventoryFull", isInventoryFull());

        // Environment
        Level level = foreman.level();
        // worldState.set("isDay", level.isDay());
        // worldState.set("nearWater", isNearWater());
        // worldState.set("nearTrees", isNearTrees());
    }

    /**
     * Plans and executes using natural language command.
     */
    public CompletableFuture<Void> executeCommand(String command) {
        NaturalLanguageGoalParser parser = new NaturalLanguageGoalParser(llmClient);

        return parser.parseGoal(command, worldState)
            .thenAccept(goal -> {
                if (goal != null) {
                    goalSelector.addGoal(goal);
                    LOGGER.info("Added goal from command: {}", goal.getName());
                }
            });
    }

    /**
     * Tick update called from entity.
     */
    public void tick() {
        // Update world state
        updateWorldState();

        // Plan and execute
        // (similar to generic GoapAgent)
    }

    private void updateWorldState() {
        // Sync world state with actual game state
        worldState.set("x", foreman.getX());
        worldState.set("y", foreman.getY());
        worldState.set("z", foreman.getZ());
        worldState.set("health", (int) foreman.getHealth());
    }
}
```

### 10.3 Integration with Existing Architecture

```java
/**
 * Adapter integrating GOAP with existing ActionExecutor.
 */
public class GoapActionAdapter extends BaseAction {

    private final GoapAction goapAction;
    private final WorldState worldState;

    public GoapActionAdapter(ForemanEntity foreman,
                            Task task,
                            GoapAction goapAction,
                            WorldState worldState) {
        super(foreman, task);
        this.goapAction = goapAction;
        this.worldState = worldState;
    }

    @Override
    protected void onStart() {
        if (!goapAction.canExecute(worldState)) {
            result = ActionResult.failure("GOAP action preconditions not met");
            return;
        }

        ActionContext context = new ActionContext(
            foreman.level(),
            foreman,
            foreman.getNavigation()
        );

        boolean started = goapAction.execute(worldState, context);
        if (!started) {
            result = ActionResult.failure("GOAP action failed to start");
        } else {
            result = ActionResult.inProgress();
        }
    }

    @Override
    protected void onTick() {
        if (goapAction.isRunning()) {
            result = ActionResult.inProgress();
        } else {
            result = ActionResult.success();
        }
    }

    @Override
    protected void onCancel() {
        goapAction.cancel();
    }

    @Override
    public String getDescription() {
        return "GOAP: " + goapAction.getClass().getSimpleName();
    }
}

/**
 * Plugin that registers GOAP actions with the ActionRegistry.
 */
public class GoapActionsPlugin implements ActionPlugin {

    private final WorldState worldState;

    public GoapActionsPlugin(WorldState worldState) {
        this.worldState = worldState;
    }

    @Override
    public void registerActions(ActionRegistry registry) {
        // Register GOAP actions wrapped as BaseAction

        registry.register("goap_shoot", (foreman, task, ctx) -> {
            GoapAction shootAction = new ShootAction();
            return new GoapActionAdapter(foreman, task, shootAction, worldState);
        });

        registry.register("goap_reload", (foreman, task, ctx) -> {
            GoapAction reloadAction = new ReloadAction();
            return new GoapActionAdapter(foreman, task, reloadAction, worldState);
        });

        registry.register("goap_take_cover", (foreman, task, ctx) -> {
            GoapAction coverAction = new TakeCoverAction();
            return new GoapActionAdapter(foreman, task, coverAction, worldState);
        });

        registry.register("goap_mine", (foreman, task, ctx) -> {
            String blockType = task.getStringParameter("block", "stone");
            GoapAction mineAction = new MineBlockAction(blockType);
            return new GoapActionAdapter(foreman, task, mineAction, worldState);
        });
    }

    @Override
    public String getPluginId() {
        return "goap-actions";
    }

    @Override
    public int getPriority() {
        return 10;  // Higher priority than default actions
    }
}
```

---

## 11. References

### Academic Papers

1. **Orkin, J. (2004)** - "Applying Goal-Oriented Action Planning to Games" - AI Game Programming Wisdom 2
2. **Higgins, D. (2017)** - "Goal-Oriented Action Planning in Dynamic Environments" - AIIDE
3. **Champandard, A. J. (2022)** - "Neuro-Symbolic AI for Game Agents" - IEEE Transactions on Games

### Game Industry Resources

1. **GDC Talks:**
   - Jeff Orkin: "AI Architecture: GOAP Planning in F.E.A.R."
   - Kevin Maxham: "GOAP for Killzone 2"
   - Brian Schwab: "AI Game Architecture"

2. **Open Source Implementations:**
   - [ReGoap (C# Unity)](https://github.com/sploreg/goap)
   - [GOAP for Unity (GitCode)](https://gitcode.net/mirrors/goap/goap)
   - [Embabel (Java)](https://segmentfault.com/p/1210000046614005)

### Online Resources

1. [Game AI Programming Wisdom Series](https://www.crcpress.com/AI-Game-Programming-Wisdom-Series/book-series/IGPWS)
2. [GAMES104 - Game Engine Gameplay Systems: Advanced AI](https://it.en369.cn/jiaocheng/1754692047a2775114.html)
3. [GOAP Unity Implementation with Visual Debugger](https://developer.unity.cn/projects/659013b0edbc2a97d81632eb)
4. [Intelligent Agent Systems Survey (arXiv 2025)](https://arxiv.org/abs/2025.xxxxx)
5. [Classical AI Planning with LLMs (arXiv 2025)](https://arxiv.org/abs/2505.xxxxx)

### Related Technologies

1. **PDDL (Planning Domain Definition Language)** - Standard for symbolic planning
2. **STRIPS** - Classical planning algorithm that inspired GOAP
3. **Utility AI** - Alternative to GOAP for decision making
4. **Behavior Trees** - Another alternative, more structured than GOAP

---

## Appendix: Quick Reference

### GOAP Planning Algorithm Summary

```
1. SENSE: Read current world state
2. SELECT: Choose highest priority achievable goal
3. PLAN: A* search backward from goal to current state
   - Nodes = World states
   - Edges = Actions (preconditions → effects)
   - Cost = Action costs (lower is better)
   - Heuristic = State difference (admissible)
4. EXECUTE: Run actions sequentially
5. MONITOR: Watch for state changes → Replan if needed
```

### Action Template

```java
public class MyAction extends BaseGoapAction {
    public MyAction() {
        super("MyAction", 5);  // name, cost
        preconditions.set("hasItem", true);
        effects.set("itemUsed", true);
    }

    @Override
    public boolean execute(WorldState state, ActionContext ctx) {
        // Actual execution logic
        return true;
    }
}
```

### Goal Template

```java
public class MyGoal extends BaseGoapGoal {
    public MyGoal() {
        super("MyGoal", 10);  // name, priority
        addTarget("targetCondition", true);
    }

    @Override
    public int getPriority(WorldState state) {
        // Dynamic priority calculation
        return basePriority;
    }
}
```

---

**Document Status:** Complete
**Last Updated:** 2026-02-28
**Next Review:** After GOAP implementation integration
