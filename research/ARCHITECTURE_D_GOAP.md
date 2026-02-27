# GOAP (Goal-Oriented Action Planning) Architecture for MineWright

**Document Version:** 1.0
**Last Updated:** 2026-02-26
**Complexity Rating:** 8/10
**Status:** Theoretical Architecture - Not Yet Implemented

---

## Table of Contents

1. [Overview](#1-overview)
2. [How GOAP Works](#2-how-goap-works)
3. [Goal Hierarchy](#3-goal-hierarchy)
4. [Action Definitions](#4-action-definitions)
5. [World State Representation](#5-world-state-representation)
6. [A* Planner Implementation](#6-a-planner-implementation)
7. [Code Examples](#7-code-examples)
8. [Integration with Existing MineWright](#8-integration-with-existing-minewright)
9. [Pros and Cons](#9-pros-and-cons)
10. [Performance Considerations](#10-performance-considerations)
11. [Testing Strategy](#11-testing-strategy)
12. [Migration Path](#12-migration-path)

---

## 1. Overview

### 1.1 What is GOAP?

**Goal-Oriented Action Planning (GOAP)** is an AI planning technique used extensively in game AI (first popularized in F.E.A.R.) that enables agents to:

1. **Define high-level goals** (e.g., "keep player happy", "build shelter")
2. **Declare available actions** with preconditions and effects
3. **Automatically plan action sequences** to achieve goals using A* search
4. **React dynamically** to changing world states

### 1.2 Why GOAP for MineWright?

The current MineWright system uses **LLM-based planning** (`TaskPlanner`) which:
- **Pros**: Flexible, understands natural language, creative solutions
- **Cons**: Slow (30-60s), expensive API costs, non-deterministic, requires internet

GOAP provides a complementary approach:
- **Fast**: Millisecond-level planning
- **Deterministic**: Same inputs = same plans
- **Local**: No API calls needed
- **Transparent**: Easy to debug and visualize

### 1.3 Hybrid Architecture Vision

```
                      User Command
                           │
                           ▼
              ┌────────────────────────┐
              │   Command Classifier   │
              │  (LLM or Heuristics)   │
              └────────┬───────────────┘
                       │
           ┌───────────┴───────────┐
           │                       │
           ▼                       ▼
    ┌──────────────┐      ┌──────────────┐
    │   GOAP Core  │      │   LLM Fallback│
    │ (Fast Plans) │      │ (Complex Tasks)│
    └──────────────┘      └──────────────┘
           │                       │
           └───────────┬───────────┘
                       ▼
              ┌────────────────┐
              │ Action Executor│
              │  (Existing)    │
              └────────────────┘
```

---

## 2. How GOAP Works

### 2.1 Core Concepts

```
┌─────────────────────────────────────────────────────────────┐
│                    GOAP Planning Cycle                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. ASSESS: Scan world state → Build WorldState object     │
│                                                             │
│  2. SELECT: Choose highest priority goal from goal stack    │
│                                                             │
│  3. PLAN:   Run A* search to find action sequence          │
│            Start: Current WorldState                        │
│            Goal:  Goal.isSatisfied(WorldState)             │
│                                                             │
│  4. EXECUTE: Pop action from queue → Execute → Update state│
│                                                             │
│  5. REPEAT: If goal not satisfied or new goal arises        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Planning Example

**Goal:** "Build a shelter"

**World State (Current):**
```json
{
  "has_wood": 0,
  "has_stone": 0,
  "has_tools": false,
  "near_trees": true,
  "near_shelter": false
}
```

**GOAP Planner Output:**
```
Action Sequence:
1. GatherWoodAction(target: 10)
2. CraftPickaxeAction()
3. GatherStoneAction(target: 20)
4. CraftWallsAction()
5. CompleteGoal()
```

**World State (After Plan):**
```json
{
  "has_wood": 10,
  "has_stone": 20,
  "has_tools": true,
  "near_trees": true,
  "near_shelter": true
}
```

---

## 3. Goal Hierarchy

### 3.1 Goal Structure

Goals are organized in a priority hierarchy:

```
                    KEEP_PLAYER_ALIVE (Priority: 100)
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
  KEEP_PLAYER_HAPPY    KEEP_SELF_SAFE    COMPLETE_TASKS
    (Priority: 80)      (Priority: 90)     (Priority: 70)
        │                  │                  │
    ┌───┴────┐        ┌───┴────┐        ┌───┴────┐
    │        │        │        │        │        │
SOCIAL   COMBAT   HEALTH   SHELTER  BUILDING  MINING
    │        │        │        │        │        │
... (subgoals)
```

### 3.2 Goal Definitions

```java
/**
 * Base goal class for GOAP system
 */
public abstract class Goal {
    protected final String name;
    protected final int priority;
    protected final Map<String, Object> goalState;

    public Goal(String name, int priority) {
        this.name = name;
        this.priority = priority;
        this.goalState = new HashMap<>();
    }

    /**
     * Check if this goal is satisfied by the current world state
     */
    public abstract boolean isSatisfied(WorldState worldState);

    /**
     * Calculate urgency (0.0 to 1.0) based on current conditions
     * Higher urgency = should be prioritized
     */
    public abstract double calculateUrgency(WorldState worldState);

    /**
     * Get the target world state that satisfies this goal
     */
    public Map<String, Object> getTargetState() {
        return new HashMap<>(goalState);
    }

    public int getPriority() { return priority; }
    public String getName() { return name; }
}
```

### 3.3 Concrete Goal Examples

#### 3.3.1 KeepPlayerHappyGoal

```java
public class KeepPlayerHappyGoal extends Goal {
    private static final double HAPPY_THRESHOLD = 0.7;

    public KeepPlayerHappyGoal() {
        super("keep_player_happy", 80);
        goalState.put("player_happiness", 1.0);
        goalState.put("player_nearby", true);
    }

    @Override
    public boolean isSatisfied(WorldState worldState) {
        double happiness = worldState.getDouble("player_happiness", 0.0);
        boolean nearby = worldState.getBoolean("player_nearby", false);
        return happiness >= HAPPY_THRESHOLD && nearby;
    }

    @Override
    public double calculateUrgency(WorldState worldState) {
        double happiness = worldState.getDouble("player_happiness", 0.5);
        double lastInteraction = worldState.getDouble("seconds_since_player_interaction", 0.0);

        // Urgency increases as happiness decreases or time since interaction increases
        double happinessUrgency = 1.0 - happiness;
        double interactionUrgency = Math.min(lastInteraction / 300.0, 1.0); // 5 minutes = max urgency

        return Math.max(happinessUrgency, interactionUrgency);
    }
}
```

#### 3.3.2 CompleteTaskGoal

```java
public class CompleteTaskGoal extends Goal {
    private final String taskDescription;
    private final Map<String, Object> taskRequirements;

    public CompleteTaskGoal(String taskDescription, Map<String, Object> requirements) {
        super("complete_task", 70);
        this.taskDescription = taskDescription;
        this.taskRequirements = requirements;
        this.goalState.putAll(requirements);
    }

    @Override
    public boolean isSatisfied(WorldState worldState) {
        for (Map.Entry<String, Object> req : taskRequirements.entrySet()) {
            Object currentValue = worldState.get(req.getKey());
            if (!Objects.equals(currentValue, req.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public double calculateUrgency(WorldState worldState) {
        // Task urgency is based on how close we are to completion
        int satisfiedReqs = 0;
        for (Map.Entry<String, Object> req : taskRequirements.entrySet()) {
            if (Objects.equals(worldState.get(req.getKey()), req.getValue())) {
                satisfiedReqs++;
            }
        }
        double progress = (double) satisfiedReqs / taskRequirements.size();
        return 1.0 - progress; // Higher urgency when less progress
    }
}
```

#### 3.3.3 SelfPreservationGoal

```java
public class SelfPreservationGoal extends Goal {
    private static final double MIN_HEALTH = 0.3;

    public SelfPreservationGoal() {
        super("self_preservation", 90); // High priority
        goalState.put("health", 1.0);
        goalState.put("in_danger", false);
    }

    @Override
    public boolean isSatisfied(WorldState worldState) {
        double health = worldState.getDouble("health", 1.0);
        boolean inDanger = worldState.getBoolean("in_danger", false);
        return health >= MIN_HEALTH && !inDanger;
    }

    @Override
    public double calculateUrgency(WorldState worldState) {
        double health = worldState.getDouble("health", 1.0);
        boolean inDanger = worldState.getBoolean("in_danger", false);

        double healthUrgency = health < MIN_HEALTH ? 1.0 : 0.0;
        double dangerUrgency = inDanger ? 1.0 : 0.0;

        return Math.max(healthUrgency, dangerUrgency);
    }
}
```

---

## 4. Action Definitions

### 4.1 Action Structure

```java
/**
 * Base action class for GOAP
 * Actions have preconditions (what must be true) and effects (what becomes true)
 */
public abstract class GoapAction {
    protected final String name;
    protected final int cost; // Cost for A* heuristic (lower = preferred)
    protected final Map<String, Object> preconditions;
    protected final Map<String, Object> effects;

    protected GoapAction(String name, int cost) {
        this.name = name;
        this.cost = cost;
        this.preconditions = new HashMap<>();
        this.effects = new HashMap<>();
    }

    /**
     * Check if this action can be executed in the current world state
     */
    public boolean canExecute(WorldState worldState) {
        for (Map.Entry<String, Object> precond : preconditions.entrySet()) {
            Object worldValue = worldState.get(precond.getKey());
            if (!Objects.equals(worldValue, precond.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Apply this action's effects to a world state (returns NEW state)
     */
    public WorldState applyEffects(WorldState worldState) {
        WorldState newState = worldState.copy();
        for (Map.Entry<String, Object> effect : effects.entrySet()) {
            newState.set(effect.getKey(), effect.getValue());
        }
        return newState;
    }

    /**
     * Execute the action in the game world
     * Returns true when action is complete
     */
    public abstract boolean execute(ForemanEntity foreman, WorldState currentState);

    /**
     * Heuristic cost for A* (lower is better)
     */
    public int getCost() { return cost; }
    public String getName() { return name; }
    public Map<String, Object> getPreconditions() { return preconditions; }
    public Map<String, Object> getEffects() { return effects; }
}
```

### 4.2 Concrete Action Examples

#### 4.2.1 GatherWoodAction

```java
public class GatherWoodAction extends GoapAction {
    private static final int WOOD_PER_ACTION = 5;

    public GatherWoodAction() {
        super("gather_wood", 10); // Moderate cost
        preconditions.put("has_tools", true);
        preconditions.put("near_trees", true);
        preconditions.put("wood_in_inventory", 0); // Can gather if we have none
        effects.put("wood_in_inventory", WOOD_PER_ACTION);
    }

    @Override
    public boolean execute(ForemanEntity foreman, WorldState currentState) {
        // Find nearest tree
        BlockPos treePos = findNearestTree(foreman);
        if (treePos == null) {
            return false;
        }

        // Pathfind to tree
        foreman.getNavigation().moveTo(treePos.getX(), treePos.getY(), treePos.getZ(), 1.0);

        // Check if arrived
        if (foreman.blockPosition().distSqr(treePos) < 9) { // Within 3 blocks
            // Mine wood
            BlockState treeState = foreman.level().getBlockState(treePos);
            foreman.level().destroyBlock(treePos, true);

            // Update world state
            int currentWood = currentState.getInt("wood_in_inventory", 0);
            currentState.set("wood_in_inventory", currentWood + WOOD_PER_ACTION);

            return true; // Action complete
        }

        return false; // Still executing
    }

    private BlockPos findNearestTree(ForemanEntity foreman) {
        // Search for logs within 20 blocks
        BlockPos foremanPos = foreman.blockPosition();
        for (int x = -20; x <= 20; x++) {
            for (int y = -5; y <= 10; y++) {
                for (int z = -20; z <= 20; z++) {
                    BlockPos checkPos = foremanPos.offset(x, y, z);
                    BlockState state = foreman.level().getBlockState(checkPos);
                    if (state.is(BlockTags.LOGS)) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }
}
```

#### 4.2.2 CraftPickaxeAction

```java
public class CraftPickaxeAction extends GoapAction {
    private static final int WOOD_COST = 3;

    public CraftPickaxeAction() {
        super("craft_pickaxe", 15);
        preconditions.put("wood_in_inventory", WOOD_COST);
        preconditions.put("has_workbench", true);
        effects.put("has_pickaxe", true);
        effects.put("wood_in_inventory", 0); // Consumes wood
    }

    @Override
    public boolean execute(ForemanEntity foreman, WorldState currentState) {
        // Check if we already have a pickaxe in inventory
        if (hasPickaxeInInventory(foreman)) {
            currentState.set("has_pickaxe", true);
            currentState.set("has_tools", true);
            return true;
        }

        // Find workbench
        BlockPos workbench = findNearestWorkbench(foreman);
        if (workbench == null) {
            return false;
        }

        // Move to workbench
        foreman.getNavigation().moveTo(workbench.getX(), workbench.getY(), workbench.getZ(), 1.0);

        if (foreman.blockPosition().distSqr(workbench) < 9) {
            // Craft pickaxe (give item to Foreman)
            ItemStack pickaxe = new ItemStack(Items.WOODEN_PICKAXE);
            foreman.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);

            // Update state
            currentState.set("has_pickaxe", true);
            currentState.set("has_tools", true);
            int currentWood = currentState.getInt("wood_in_inventory", 0);
            currentState.set("wood_in_inventory", Math.max(0, currentWood - WOOD_COST));

            return true;
        }

        return false;
    }

    private boolean hasPickaxeInInventory(ForemanEntity foreman) {
        ItemStack mainHand = foreman.getMainHandItem();
        return mainHand.getItem() instanceof PickaxeItem;
    }

    private BlockPos findNearestWorkbench(ForemanEntity foreman) {
        BlockPos foremanPos = foreman.blockPosition();
        for (int x = -10; x <= 10; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -10; z <= 10; z++) {
                    BlockPos checkPos = foremanPos.offset(x, y, z);
                    BlockState state = foreman.level().getBlockState(checkPos);
                    if (state.is(Blocks.CRAFTING_TABLE)) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }
}
```

#### 4.2.3 BuildShelterAction

```java
public class BuildShelterAction extends GoapAction {
    private static final int WOOD_COST = 20;
    private List<BlockPos> buildPlan;
    private int currentBlockIndex = 0;

    public BuildShelterAction() {
        super("build_shelter", 50); // High cost
        preconditions.put("wood_in_inventory", WOOD_COST);
        preconditions.put("has_tools", true);
        effects.put("has_shelter", true);
        effects.put("near_shelter", true);
        effects.put("wood_in_inventory", 0); // Consumes wood
    }

    @Override
    public boolean execute(ForemanEntity foreman, WorldState currentState) {
        // Generate build plan on first execution
        if (buildPlan == null) {
            buildPlan = generateShelterPlan(foreman);
            if (buildPlan.isEmpty()) {
                return false; // Failed to generate plan
            }
        }

        // Place blocks one by one
        if (currentBlockIndex < buildPlan.size()) {
            BlockPos targetPos = buildPlan.get(currentBlockIndex);

            // Move to block position
            if (foreman.blockPosition().distSqr(targetPos) > 9) {
                foreman.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.0);
                return false;
            }

            // Place block
            foreman.level().setBlock(targetPos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
            foreman.swing(InteractionHand.MAIN_HAND, true);

            currentBlockIndex++;

            // Check if complete
            if (currentBlockIndex >= buildPlan.size()) {
                currentState.set("has_shelter", true);
                currentState.set("near_shelter", true);
                int currentWood = currentState.getInt("wood_in_inventory", 0);
                currentState.set("wood_in_inventory", Math.max(0, currentWood - WOOD_COST));
                return true;
            }
        }

        return false;
    }

    private List<BlockPos> generateShelterPlan(ForemanEntity foreman) {
        List<BlockPos> plan = new ArrayList<>();
        BlockPos start = foreman.blockPosition();
        int width = 5, height = 4, depth = 5;

        // Walls
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                plan.add(start.offset(x, y, 0)); // Front wall
                plan.add(start.offset(x, y, depth - 1)); // Back wall
            }
        }
        for (int z = 0; z < depth; z++) {
            for (int y = 0; y < height; y++) {
                plan.add(start.offset(0, y, z)); // Left wall
                plan.add(start.offset(width - 1, y, z)); // Right wall
            }
        }

        // Roof
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                plan.add(start.offset(x, height, z));
            }
        }

        return plan;
    }
}
```

#### 4.2.4 FollowPlayerAction

```java
public class FollowPlayerGoapAction extends GoapAction {
    private Player targetPlayer;
    private static final int FOLLOW_DISTANCE = 5;

    public FollowPlayerGoapAction() {
        super("follow_player", 5); // Low cost
        preconditions.put("player_nearby", false);
        effects.put("player_nearby", true);
        effects.put("player_happiness", 0.8); // Makes player happy
    }

    @Override
    public boolean execute(ForemanEntity foreman, WorldState currentState) {
        // Find nearest player
        if (targetPlayer == null || !targetPlayer.isAlive()) {
            targetPlayer = findNearestPlayer(foreman);
            if (targetPlayer == null) {
                return false;
            }
        }

        double distance = foreman.distanceTo(targetPlayer);

        if (distance > FOLLOW_DISTANCE) {
            // Move towards player
            foreman.getNavigation().moveTo(targetPlayer, 1.0);
            return false; // Still following
        } else {
            // Close enough
            currentState.set("player_nearby", true);
            currentState.set("seconds_since_player_interaction", 0.0);
            return true;
        }
    }

    private Player findNearestPlayer(ForemanEntity foreman) {
        return foreman.level().players().stream()
            .filter(p -> p.isAlive() && !p.isSpectator())
            .min(Comparator.comparingDouble(foreman::distanceTo))
            .orElse(null);
    }
}
```

---

## 5. World State Representation

### 5.1 WorldState Class

```java
/**
 * Represents the current state of the world for GOAP planning
 * Immutable snapshot of relevant game state
 */
public class WorldState {
    private final Map<String, Object> state;
    private final long timestamp;

    public WorldState() {
        this.state = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    private WorldState(Map<String, Object> state, long timestamp) {
        this.state = new HashMap<>(state);
        this.timestamp = timestamp;
    }

    public void set(String key, Object value) {
        state.put(key, value);
    }

    public Object get(String key) {
        return state.get(key);
    }

    public String getString(String key, String defaultValue) {
        Object value = state.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        Object value = state.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    public double getDouble(String key, double defaultValue) {
        Object value = state.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = state.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    public WorldState copy() {
        return new WorldState(this.state, this.timestamp);
    }

    public long getTimestamp() { return timestamp; }

    /**
     * Calculate difference between two world states
     * Used for heuristic calculations
     */
    public int distanceTo(WorldState other) {
        int differences = 0;
        for (Map.Entry<String, Object> entry : other.state.entrySet()) {
            if (!Objects.equals(this.state.get(entry.getKey()), entry.getValue())) {
                differences++;
            }
        }
        return differences;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldState that = (WorldState) o;
        return Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state);
    }

    @Override
    public String toString() {
        return "WorldState{" + state + "}";
    }
}
```

### 5.2 WorldStateScanner

```java
/**
 * Scans the game world and builds a WorldState object
 * Abstracts away Minecraft API details from GOAP system
 */
public class WorldStateScanner {
    private final ForemanEntity foreman;

    public WorldStateScanner(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Build a complete WorldState snapshot
     */
    public WorldState scan() {
        WorldState state = new WorldState();

        // Foreman's state
        scanForemanState(state);

        // Environment state
        scanEnvironment(state);

        // Player state
        scanPlayerState(state);

        // Inventory state
        scanInventory(state);

        // Task state
        scanTaskState(state);

        return state;
    }

    private void scanForemanState(WorldState state) {
        state.set("health", foreman.getHealth() / foreman.getMaxHealth());
        state.set("position", foreman.blockPosition());
        state.set("is_alive", foreman.isAlive());
        state.set("in_danger", isInDanger());

        // Check for tools
        ItemStack mainHand = foreman.getMainHandItem();
        state.set("has_pickaxe", mainHand.getItem() instanceof PickaxeItem);
        state.set("has_axe", mainHand.getItem() instanceof AxeItem);
        state.set("has_tools", state.getBoolean("has_pickaxe", false) ||
                               state.getBoolean("has_axe", false));
    }

    private void scanEnvironment(WorldState state) {
        BlockPos pos = foreman.blockPosition();

        // Check for nearby resources
        state.set("near_trees", hasNearbyBlocks(pos, 20, BlockTags.LOGS));
        state.set("near_stone", hasNearbyBlocks(pos, 20, BlockTags.BASE_STONE_OVERWORLD));
        state.set("near_iron", hasNearbyBlocks(pos, 30, Blocks.IRON_ORE));
        state.set("near_coal", hasNearbyBlocks(pos, 30, Blocks.COAL_ORE));

        // Check for shelter
        state.set("has_shelter", hasShelterNearby(pos, 10));
        state.set("near_shelter", hasShelterNearby(pos, 30));

        // Check for workbench
        state.set("has_workbench", hasNearbyBlocks(pos, 10, Blocks.CRAFTING_TABLE));

        // Check lighting level
        int lightLevel = foreman.level().getBrightness(LightLayer.BLOCK, pos);
        state.set("light_level", lightLevel);
        state.set("is_dark", lightLevel < 8);
    }

    private void scanPlayerState(WorldState state) {
        Player nearestPlayer = findNearestPlayer();

        if (nearestPlayer != null) {
            double distance = foreman.distanceTo(nearestPlayer);
            state.set("player_nearby", distance < 20);
            state.set("player_distance", distance);

            // Get player happiness from companion memory
            CompanionMemory memory = foreman.getCompanionMemory();
            state.set("player_happiness", memory.getRelationshipScore());

            long lastInteraction = System.currentTimeMillis() - memory.getLastInteractionTime();
            state.set("seconds_since_player_interaction", lastInteraction / 1000.0);
        } else {
            state.set("player_nearby", false);
            state.set("player_distance", Double.MAX_VALUE);
            state.set("player_happiness", 0.0);
            state.set("seconds_since_player_interaction", 9999.0);
        }
    }

    private void scanInventory(WorldState state) {
        // Count resources in inventory (if Foreman has one)
        int woodCount = countItemInInventory(ItemTags.LOGS);
        int stoneCount = countItemInInventory(ItemTags.COAL_ORES); // Rough approximation

        state.set("wood_in_inventory", woodCount);
        state.set("stone_in_inventory", stoneCount);
    }

    private void scanTaskState(WorldState state) {
        ActionExecutor executor = foreman.getActionExecutor();

        if (executor.getCurrentGoal() != null) {
            state.set("current_goal", executor.getCurrentGoal());
            state.set("has_active_task", true);
        } else {
            state.set("has_active_task", false);
        }

        state.set("task_queue_size", getTaskQueueSize());
    }

    private boolean isInDanger() {
        // Check for nearby hostile mobs
        return !foreman.level().getEntitiesOfClass(Monster.class,
            foreman.getBoundingBox().inflate(10)).isEmpty();
    }

    private boolean hasNearbyBlocks(BlockPos center, int radius, TagKey<Block> blockTag) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius/2; y <= radius/2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (foreman.level().getBlockState(pos).is(blockTag)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasNearbyBlocks(BlockPos center, int radius, Block block) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius/2; y <= radius/2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (foreman.level().getBlockState(pos).is(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasShelterNearby(BlockPos pos, int radius) {
        // Simple check: are there enough blocks nearby to form a shelter?
        int blockCount = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 5; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    if (!foreman.level().getBlockState(checkPos).isAir()) {
                        blockCount++;
                    }
                }
            }
        }
        return blockCount > 50; // Arbitrary threshold
    }

    private Player findNearestPlayer() {
        return foreman.level().players().stream()
            .filter(p -> p.isAlive() && !p.isSpectator())
            .min(Comparator.comparingDouble(foreman::distanceTo))
            .orElse(null);
    }

    private int countItemInInventory(TagKey<Item> itemTag) {
        // Simplified - would need actual inventory tracking
        return 0;
    }

    private int getTaskQueueSize() {
        // Would need access to task queue
        return 0;
    }
}
```

---

## 6. A* Planner Implementation

### 6.1 AStarPlanner

```java
/**
 * A* pathfinding algorithm for GOAP action planning
 * Finds the cheapest sequence of actions to achieve a goal
 */
public class AStarGoapPlanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(AStarGoapPlanner.class);
    private static final int MAX_ITERATIONS = 1000; // Prevent infinite loops
    private static final long MAX_PLANNING_TIME_MS = 100; // 100ms max

    private final List<GoapAction> availableActions;

    public AStarGoapPlanner(List<GoapAction> actions) {
        this.availableActions = new ArrayList<>(actions);
        LOGGER.debug("GOAP Planner initialized with {} actions", actions.size());
    }

    /**
     * Plan a sequence of actions to achieve the goal
     *
     * @param startState Current world state
     * @param goal       Goal to achieve
     * @return List of actions, or empty list if no plan found
     */
    public List<GoapAction> plan(WorldState startState, Goal goal) {
        long startTime = System.currentTimeMillis();

        // If goal already satisfied, no action needed
        if (goal.isSatisfied(startState)) {
            LOGGER.debug("Goal '{}' already satisfied", goal.getName());
            return Collections.emptyList();
        }

        // A* search
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<WorldState> closedSet = new HashSet<>();
        Map<WorldState, Node> allNodes = new HashMap<>();

        Node startNode = new Node(startState, null, null, 0, heuristic(startState, goal.getTargetState()));
        openSet.add(startNode);
        allNodes.put(startState, startNode);

        int iterations = 0;

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;

            // Check timeout
            if (System.currentTimeMillis() - startTime > MAX_PLANNING_TIME_MS) {
                LOGGER.warn("GOAP planning timeout after {}ms", System.currentTimeMillis() - startTime);
                return Collections.emptyList();
            }

            // Get node with lowest f-score
            Node current = openSet.poll();

            // Check if goal reached
            if (goal.isSatisfied(current.worldState)) {
                List<GoapAction> plan = reconstructPath(current);
                LOGGER.info("GOAP plan found in {} iterations ({}ms): {} actions",
                    iterations, System.currentTimeMillis() - startTime, plan.size());
                return plan;
            }

            closedSet.add(current.worldState);

            // Explore neighbors
            for (GoapAction action : availableActions) {
                // Check if action can execute
                if (!action.canExecute(current.worldState)) {
                    continue;
                }

                // Apply action effects
                WorldState neighborState = action.applyEffects(current.worldState);

                // Skip if already evaluated
                if (closedSet.contains(neighborState)) {
                    continue;
                }

                // Calculate costs
                int tentativeGScore = current.gScore + action.getCost();

                Node neighborNode = allNodes.get(neighborState);
                boolean isNew = neighborNode == null;

                if (isNew || tentativeGScore < neighborNode.gScore) {
                    neighborNode = new Node(
                        neighborState,
                        action,
                        current,
                        tentativeGScore,
                        tentativeGScore + heuristic(neighborState, goal.getTargetState())
                    );

                    allNodes.put(neighborState, neighborNode);

                    if (isNew) {
                        openSet.add(neighborNode);
                    }
                }
            }
        }

        LOGGER.warn("GOAP planning failed: no plan found after {} iterations", iterations);
        return Collections.emptyList();
    }

    /**
     * Heuristic function for A* (estimated cost to reach goal)
     * Uses Manhattan distance in state space
     */
    private int heuristic(WorldState current, Map<String, Object> goal) {
        int distance = 0;
        for (Map.Entry<String, Object> goalEntry : goal.entrySet()) {
            Object currentValue = current.get(goalEntry.getKey());
            if (!Objects.equals(currentValue, goalEntry.getValue())) {
                distance++;
            }
        }
        return distance;
    }

    /**
     * Reconstruct path from goal node back to start
     */
    private List<GoapAction> reconstructPath(Node goalNode) {
        LinkedList<GoapAction> path = new LinkedList<>();
        Node current = goalNode;

        while (current.action != null) {
            path.addFirst(current.action);
            current = current.parent;
        }

        return path;
    }

    /**
     * Node for A* search
     */
    private static class Node implements Comparable<Node> {
        final WorldState worldState;
        final GoapAction action; // Action that led to this state
        final Node parent;       // Parent node
        final int gScore;        // Cost from start
        final int fScore;        // Estimated total cost (g + h)

        Node(WorldState worldState, GoapAction action, Node parent, int gScore, int fScore) {
            this.worldState = worldState;
            this.action = action;
            this.parent = parent;
            this.gScore = gScore;
            this.fScore = fScore;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.fScore, other.fScore);
        }
    }
}
```

### 6.2 GoalManager

```java
/**
 * Manages the goal hierarchy and selects the most urgent goal
 */
public class GoalManager {
    private final List<Goal> goals;
    private Goal currentGoal;

    public GoalManager() {
        this.goals = new ArrayList<>();
        registerDefaultGoals();
    }

    private void registerDefaultGoals() {
        goals.add(new SelfPreservationGoal());
        goals.add(new KeepPlayerHappyGoal());
        goals.add(new StayNearPlayerGoal());
        // Add more goals as needed
    }

    /**
     * Select the most urgent goal based on current world state
     */
    public Goal selectGoal(WorldState worldState) {
        // If current goal is not satisfied, keep working on it
        if (currentGoal != null && !currentGoal.isSatisfied(worldState)) {
            return currentGoal;
        }

        // Otherwise, find new highest priority goal
        Goal bestGoal = null;
        double bestUrgency = -1.0;

        for (Goal goal : goals) {
            if (!goal.isSatisfied(worldState)) {
                double urgency = goal.calculateUrgency(worldState);
                double weightedUrgency = urgency * (goal.getPriority() / 100.0);

                if (weightedUrgency > bestUrgency) {
                    bestUrgency = weightedUrgency;
                    bestGoal = goal;
                }
            }
        }

        currentGoal = bestGoal;
        return bestGoal;
    }

    /**
     * Register a custom goal
     */
    public void registerGoal(Goal goal) {
        goals.add(goal);
        goals.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }

    /**
     * Add a task from the player as a high-priority goal
     */
    public void addTaskGoal(String taskDescription, Map<String, Object> requirements) {
        CompleteTaskGoal taskGoal = new CompleteTaskGoal(taskDescription, requirements);
        taskGoal.priority = 95; // Higher than most autonomous goals
        goals.add(0, taskGoal); // Insert at beginning
    }
}
```

---

## 7. Code Examples

### 7.1 Complete GoapSystem Integration

```java
/**
 * Main GOAP system that coordinates planning and execution
 */
public class GoapSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoapSystem.class);

    private final ForemanEntity foreman;
    private final WorldStateScanner scanner;
    private final GoalManager goalManager;
    private final AStarGoapPlanner planner;
    private final ActionExecutor actionExecutor;

    // Current plan execution state
    private Queue<GoapAction> currentPlan;
    private GoapAction currentAction;
    private WorldState lastWorldState;
    private Goal currentGoal;
    private long lastPlanningTime = 0;
    private static final long REPLAN_INTERVAL_MS = 5000; // Replan every 5 seconds

    public GoapSystem(ForemanEntity foreman) {
        this.foreman = foreman;
        this.scanner = new WorldStateScanner(foreman);
        this.goalManager = new GoalManager();
        this.planner = new AStarGoapPlanner(createActionRegistry());
        this.actionExecutor = foreman.getActionExecutor();

        LOGGER.info("GOAP System initialized for Foreman '{}'", foreman.getForemanName());
    }

    /**
     * Called every tick to update GOAP system
     */
    public void tick() {
        // Scan world state
        WorldState currentWorldState = scanner.scan();

        // Check if we need to replan
        boolean needsReplan = shouldReplan(currentWorldState);

        if (needsReplan) {
            replan(currentWorldState);
        }

        // Execute current action
        executeAction(currentWorldState);

        lastWorldState = currentWorldState;
    }

    private boolean shouldReplan(WorldState currentWorldState) {
        // Replan if:
        // 1. No current plan
        if (currentPlan == null || currentPlan.isEmpty()) {
            return true;
        }

        // 2. Current goal is satisfied
        if (currentGoal != null && currentGoal.isSatisfied(currentWorldState)) {
            return true;
        }

        // 3. World state changed significantly
        if (lastWorldState != null) {
            int stateDistance = lastWorldState.distanceTo(currentWorldState);
            if (stateDistance > 3) { // Significant change
                return true;
            }
        }

        // 4. Time elapsed since last planning
        long timeSinceLastPlan = System.currentTimeMillis() - lastPlanningTime;
        if (timeSinceLastPlan > REPLAN_INTERVAL_MS) {
            return true;
        }

        return false;
    }

    private void replan(WorldState currentWorldState) {
        LOGGER.debug("Replanning for Foreman '{}'", foreman.getForemanName());

        // Select goal
        currentGoal = goalManager.selectGoal(currentWorldState);

        if (currentGoal == null) {
            LOGGER.debug("No active goal for Foreman '{}', idling", foreman.getForemanName());
            currentPlan = new LinkedList<>();
            return;
        }

        // Plan actions
        List<GoapAction> actions = planner.plan(currentWorldState, currentGoal);

        if (actions.isEmpty()) {
            LOGGER.warn("Failed to find plan for goal '{}' for Foreman '{}'",
                currentGoal.getName(), foreman.getForemanName());
            currentPlan = new LinkedList<>();
            return;
        }

        // Convert to action queue
        currentPlan = new LinkedList<>(actions);
        currentAction = null;

        LOGGER.info("New plan for Foreman '{}': {} -> {} actions",
            foreman.getForemanName(), currentGoal.getName(), actions.size());

        lastPlanningTime = System.currentTimeMillis();
    }

    private void executeAction(WorldState currentWorldState) {
        // If no action, get next from plan
        if (currentAction == null) {
            if (currentPlan == null || currentPlan.isEmpty()) {
                return;
            }
            currentAction = currentPlan.poll();
            LOGGER.debug("Starting action '{}' for Foreman '{}'",
                currentAction.getName(), foreman.getForemanName());
        }

        // Execute action
        boolean complete = currentAction.execute(foreman, currentWorldState);

        if (complete) {
            LOGGER.debug("Action '{}' completed for Foreman '{}'",
                currentAction.getName(), foreman.getForemanName());
            currentAction = null;
        }
    }

    /**
     * Add a task from the player
     */
    public void addPlayerTask(String taskDescription, Map<String, Object> requirements) {
        goalManager.addTaskGoal(taskDescription, requirements);
        LOGGER.info("Added player task '{}' for Foreman '{}'",
            taskDescription, foreman.getForemanName());
    }

    /**
     * Create the registry of available GOAP actions
     */
    private List<GoapAction> createActionRegistry() {
        return List.of(
            new GatherWoodAction(),
            new GatherStoneAction(),
            new CraftPickaxeAction(),
            new CraftAxeAction(),
            new CraftWorkbenchAction(),
            new BuildShelterAction(),
            new FollowPlayerGoapAction(),
            new IdleAction(),
            new CombatAction()
        );
    }

    /**
     * Get current goal for debugging/display
     */
    public Goal getCurrentGoal() {
        return currentGoal;
    }

    /**
     * Get current plan for debugging/display
     */
    public List<String> getCurrentPlan() {
        if (currentPlan == null) {
            return Collections.emptyList();
        }
        return currentPlan.stream()
            .map(GoapAction::getName)
            .collect(Collectors.toList());
    }
}
```

### 7.2 Action Adapter (GOAP to Existing Actions)

```java
/**
 * Adapter to use existing BaseAction implementations in GOAP
 */
public class ActionAdapter extends GoapAction {
    private final Supplier<BaseAction> actionFactory;
    private BaseAction currentAction;

    public ActionAdapter(String name, int cost,
                        Map<String, Object> preconditions,
                        Map<String, Object> effects,
                        Supplier<BaseAction> actionFactory) {
        super(name, cost);
        this.preconditions.putAll(preconditions);
        this.effects.putAll(effects);
        this.actionFactory = actionFactory;
    }

    @Override
    public boolean execute(ForemanEntity foreman, WorldState currentState) {
        // Create action if needed
        if (currentAction == null) {
            Task task = new Task(name, new HashMap<>());
            ActionContext context = foreman.getActionExecutor().getActionContext();
            currentAction = actionFactory.get();
            currentAction.start();
        }

        // Tick action
        currentAction.tick();

        // Check if complete
        if (currentAction.isComplete()) {
            ActionResult result = currentAction.getResult();
            if (result != null && result.isSuccess()) {
                currentAction = null;
                return true;
            } else {
                // Action failed
                currentAction = null;
                return false;
            }
        }

        return false;
    }

    @Override
    protected void onCancel() {
        if (currentAction != null) {
            currentAction.cancel();
            currentAction = null;
        }
    }
}
```

---

## 8. Integration with Existing MineWright

### 8.1 Hybrid Planning Architecture

```
                           User Command
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │  CommandInterpreter    │
                    │  (Classify Intent)      │
                    └────────────┬────────────┘
                                 │
                ┌────────────────┼────────────────┐
                │                                    │
        Simple/Repetitive                   Complex/Novel
        (Build shelter)                      (Creative build)
                │                                    │
                ▼                                    ▼
        ┌──────────────┐                   ┌──────────────┐
        │  GOAP Engine │                   │  LLM Planner │
        │ (Fast, Local)│                   │ (Slow, API)  │
        └──────────────┘                   └──────────────┘
                │                                    │
                └────────────────┬──────────────────┘
                                  ▼
                         ┌────────────────┐
                         │ ActionExecutor │
                         │  (Existing)    │
                         └────────────────┘
```

### 8.2 Command Interpreter

```java
/**
 * Classifies user commands to determine which planner to use
 */
public class CommandInterpreter {
    private static final Set<String> GOAP_COMMANDS = Set.of(
        "build", "mine", "gather", "craft", "follow", "stay"
    );

    private static final Set<String> LLM_COMMANDS = Set.of(
        "create", "design", "imagine", "write", "compose",
        "unique", "custom", "special", "creative"
    );

    public PlanningMode classifyCommand(String command) {
        String lowerCmd = command.toLowerCase();

        // Check for LLM indicators
        for (String llmKeyword : LLM_COMMANDS) {
            if (lowerCmd.contains(llmKeyword)) {
                return PlanningMode.LLM;
            }
        }

        // Check for GOAP indicators
        for (String goapKeyword : GOAP_COMMANDS) {
            if (lowerCmd.contains(goapKeyword)) {
                return PlanningMode.GOAP;
            }
        }

        // Default to LLM for complex commands
        return command.split(" ").length > 10 ? PlanningMode.LLM : PlanningMode.GOAP;
    }

    enum PlanningMode {
        GOAP,  // Use GOAP planner
        LLM    // Use LLM planner
    }
}
```

### 8.3 Modified ActionExecutor

```java
/**
 * Enhanced ActionExecutor with GOAP integration
 */
public class HybridActionExecutor extends ActionExecutor {
    private GoapSystem goapSystem;
    private CommandInterpreter interpreter;

    public HybridActionExecutor(ForemanEntity foreman) {
        super(foreman);
        this.goapSystem = new GoapSystem(foreman);
        this.interpreter = new CommandInterpreter();
    }

    @Override
    public void processNaturalLanguageCommand(String command) {
        // Classify command
        CommandInterpreter.PlanningMode mode = interpreter.classifyCommand(command);

        if (mode == CommandInterpreter.PlanningMode.GOAP) {
            // Use GOAP for simple, repetitive tasks
            processWithGoap(command);
        } else {
            // Use LLM for complex, creative tasks
            super.processNaturalLanguageCommand(command);
        }
    }

    private void processWithGoap(String command) {
        // Parse command into goal requirements
        Map<String, Object> requirements = parseRequirements(command);

        // Add as goal to GOAP system
        goapSystem.addPlayerTask(command, requirements);

        MineWrightMod.LOGGER.info("Processing '{}' with GOAP planner", command);
    }

    private Map<String, Object> parseRequirements(String command) {
        // Simple parser - would be more sophisticated in production
        Map<String, Object> requirements = new HashMap<>();

        String lowerCmd = command.toLowerCase();

        if (lowerCmd.contains("shelter") || lowerCmd.contains("house")) {
            requirements.put("has_shelter", true);
        } else if (lowerCmd.contains("wood")) {
            requirements.put("wood_in_inventory", extractQuantity(lowerCmd));
        } else if (lowerCmd.contains("stone")) {
            requirements.put("stone_in_inventory", extractQuantity(lowerCmd));
        } else if (lowerCmd.contains("follow")) {
            requirements.put("player_nearby", true);
        }

        return requirements;
    }

    private int extractQuantity(String command) {
        // Extract number from command
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }
        return 10; // Default
    }

    @Override
    public void tick() {
        // Tick GOAP system
        goapSystem.tick();

        // Tick existing action execution
        super.tick();
    }
}
```

---

## 9. Pros and Cons

### 9.1 Advantages of GOAP

**1. Performance**
- **Millisecond-level planning** vs. 30-60 seconds for LLM
- No network latency
- No API costs
- Predictable performance

**2. Determinism**
- Same world state = same plan
- Easy to test and debug
- Reproducible behavior

**3. Transparency**
- Clear action sequences
- Easy to visualize and explain
- Understandable failure modes

**4. Autonomy**
- Can work offline
- No external dependencies
- Reliable in all conditions

**5. Scalability**
- Handles multiple agents efficiently
- Local computation only
- No rate limiting

### 9.2 Disadvantages of GOAP

**1. Limited Flexibility**
- Requires pre-defined actions
- Cannot handle novel situations
- No creative problem-solving

**2. High Development Cost**
- Must manually define all actions
- Complex to implement correctly
- Requires careful tuning

**3. Brittle Behavior**
- Fails on unexpected edge cases
- Requires comprehensive world state
- Can get stuck in local optima

**4. Maintenance Burden**
- Adding new actions requires planning knowledge
- Preconditions/effects must be accurate
- Testing complexity increases with action count

**5. Debugging Difficulty**
- Hard to visualize state space
- Action sequences can be long
- Interdependencies are complex

### 9.3 Comparison Table

| Feature               | GOAP                 | LLM (Current)        |
|-----------------------|----------------------|----------------------|
| **Planning Speed**    | ~10ms               | 30-60s               |
| **API Cost**          | Free                | $0.01-0.10 per plan  |
| **Flexibility**       | Low (predefined)    | High (generative)    |
| **Reliability**       | High (deterministic)| Medium (stochastic)  |
| **Offline Support**   | Yes                 | No                   |
| **Development Cost**  | High                | Low                  |
| **Creativity**        | None                | High                 |
| **Debugging**         | Complex             | Medium               |
| **Multi-Agent**       | Easy                | Complex              |

---

## 10. Performance Considerations

### 10.1 Performance Characteristics

```
Operation                | Time     | Frequency
--------------------------|----------|------------------
World State Scan          | 5-10ms   | Every tick (20/s)
Goal Selection            | <1ms     | Every replan (1/5s)
A* Planning (typical)     | 10-50ms  | Every replan (1/5s)
A* Planning (worst case)  | 100ms    | Rare (timeout)
Action Execution          | Varies   | Every tick
```

### 10.2 Optimization Strategies

**1. State Caching**
```java
private WorldState cachedState;
private long cacheTimestamp = 0;
private static final long CACHE_DURATION_MS = 1000;

public WorldState scan() {
    long now = System.currentTimeMillis();
    if (cachedState != null && (now - cacheTimestamp) < CACHE_DURATION_MS) {
        return cachedState; // Return cached state
    }
    cachedState = performScan();
    cacheTimestamp = now;
    return cachedState;
}
```

**2. Action Filtering**
```java
// Only consider actions whose preconditions are close to being met
private List<GoapAction> filterRelevantActions(WorldState state) {
    return availableActions.stream()
        .filter(action -> {
            int missingPreconds = 0;
            for (Map.Entry<String, Object> precond : action.getPreconditions().entrySet()) {
                if (!Objects.equals(state.get(precond.getKey()), precond.getValue())) {
                    missingPreconds++;
                }
            }
            return missingPreconds <= 2; // Only consider if 2+ preconds met
        })
        .collect(Collectors.toList());
}
```

**3. Hierarchical Planning**
```java
// Plan at higher level first, then refine
public List<GoapAction> hierarchicalPlan(WorldState start, Goal goal) {
    // Try high-level plan first
    List<GoapAction> highLevel = plan(start, goal, HIGH_LEVEL_ACTIONS);

    // If found, refine with low-level actions
    if (!highLevel.isEmpty()) {
        return refinePlan(start, highLevel);
    }

    // Fall back to full search
    return plan(start, goal, ALL_ACTIONS);
}
```

**4. Parallel Planning**
```java
// Plan for all goals in parallel
public CompletableFuture<Plan>[] planForAllGoals(WorldState state) {
    return goals.stream()
        .map(goal -> CompletableFuture.supplyAsync(() ->
            planner.plan(state, goal), executor))
        .toArray(CompletableFuture[]::new);
}
```

### 10.3 Memory Profiling

```
Component                | Memory    | Notes
-------------------------|-----------|------------------
WorldState               | 1-5 KB    | One snapshot
Action Registry          | 5-10 KB   | All action definitions
A* Open Set              | 10-100 KB | During planning
A* Closed Set            | 10-100 KB | During planning
Plan Cache               | 50-500 KB | Optional, for replanning
-------------------------|-----------|------------------
Total (typical)          | < 1 MB    |
Total (worst case)       | < 5 MB    |
```

---

## 11. Testing Strategy

### 11.1 Unit Testing

```java
@Test
public void testGatherWoodActionPreconditions() {
    GatherWoodAction action = new GatherWoodAction();

    WorldState validState = new WorldState();
    validState.set("has_tools", true);
    validState.set("near_trees", true);

    assertTrue(action.canExecute(validState));
}

@Test
public void testGatherWoodActionEffects() {
    GatherWoodAction action = new GatherWoodAction();

    WorldState before = new WorldState();
    before.set("wood_in_inventory", 0);

    WorldState after = action.applyEffects(before);

    assertEquals(5, after.getInt("wood_in_inventory", 0));
}

@Test
public void testGoalSatisfaction() {
    CompleteTaskGoal goal = new CompleteTaskGoal("test", Map.of("wood_in_inventory", 10));

    WorldState satisfiedState = new WorldState();
    satisfiedState.set("wood_in_inventory", 10);

    assertTrue(goal.isSatisfied(satisfiedState));

    WorldState unsatisfiedState = new WorldState();
    unsatisfiedState.set("wood_in_inventory", 5);

    assertFalse(goal.isSatisfied(unsatisfiedState));
}

@Test
public void testAStarPlanner() {
    List<GoapAction> actions = List.of(
        new GatherWoodAction(),
        new CraftPickaxeAction()
    );

    AStarGoapPlanner planner = new AStarGoapPlanner(actions);

    WorldState start = new WorldState();
    start.set("has_tools", false);
    start.set("near_trees", true);

    Goal goal = new CompleteTaskGoal("get tools", Map.of("has_tools", true));

    List<GoapAction> plan = planner.plan(start, goal);

    assertFalse(plan.isEmpty());
    assertEquals("gather_wood", plan.get(0).getName());
}
```

### 11.2 Integration Testing

```java
@Test
public void testGoapSystemFullCycle() {
    ForemanEntity foreman = createTestMineWright();
    GoapSystem goap = new GoapSystem(foreman);

    // Set up world state
    WorldState state = new WorldState();
    state.set("near_trees", true);
    state.set("has_tools", false);

    // Add goal
    goap.addPlayerTask("get tools", Map.of("has_tools", true));

    // Run planning
    goap.tick();

    // Verify plan created
    assertNotNull(goap.getCurrentGoal());
    assertFalse(goap.getCurrentPlan().isEmpty());
}

@Test
public void testReplanOnStateChange() {
    // Test that system replans when world state changes significantly
}
```

### 11.3 Performance Testing

```java
@Test
public void testPlanningPerformance() {
    List<GoapAction> actions = createLargeActionSet(50); // 50 actions
    AStarGoapPlanner planner = new AStarGoapPlanner(actions);

    WorldState start = createComplexWorldState();
    Goal goal = createComplexGoal();

    long startTime = System.currentTimeMillis();
    List<GoapAction> plan = planner.plan(start, goal);
    long duration = System.currentTimeMillis() - startTime;

    assertTrue("Planning took too long: " + duration + "ms", duration < 100);
}

@Test
public void testConcurrentPlanning() {
    // Test multiple agents planning simultaneously
    List<ForemanEntity> foremans = createTestAgents(10);
    List<GoapSystem> goapSystems = foremans.stream()
        .map(GoapSystem::new)
        .collect(Collectors.toList());

    long startTime = System.currentTimeMillis();

    goapSystems.parallelStream().forEach(goap -> {
        goap.tick();
    });

    long duration = System.currentTimeMillis() - startTime;

    assertTrue("Concurrent planning took too long: " + duration + "ms", duration < 200);
}
```

---

## 12. Migration Path

### 12.1 Phase 1: Foundation (Week 1-2)

**Goals:**
- Implement core GOAP classes
- Create basic actions (gather, craft, build)
- Set up testing infrastructure

**Deliverables:**
- `WorldState`, `WorldStateScanner`
- `Goal`, `GoapAction` base classes
- `AStarGoapPlanner`
- 3-5 basic actions
- Unit tests for core classes

### 12.2 Phase 2: Integration (Week 3-4)

**Goals:**
- Integrate GOAP with existing ActionExecutor
- Create action adapters
- Implement hybrid planning

**Deliverables:**
- `GoapSystem` coordinator
- `ActionAdapter` for existing actions
- `CommandInterpreter`
- `HybridActionExecutor`
- Integration tests

### 12.3 Phase 3: Expansion (Week 5-6)

**Goals:**
- Add comprehensive action library
- Implement advanced goals
- Optimize performance

**Deliverables:**
- 15-20 total actions
- Goal hierarchy with 8-10 goals
- Performance optimizations
- Stress tests

### 12.4 Phase 4: Production (Week 7-8)

**Goals:**
- Debug and polish
- Documentation
- Gradual rollout

**Deliverables:**
- Production-ready GOAP system
- Comprehensive documentation
- Gradual feature flag rollout
- Monitoring and metrics

### 12.5 Rollout Strategy

```
Week 1-2:  Development only, no user impact
Week 3-4:  Internal testing with debug mode
Week 5-6:  Beta testing with opt-in users
Week 7-8:  Gradual rollout (10% -> 50% -> 100%)
Week 9+:   Monitor, iterate, improve
```

### 12.6 Feature Flags

```java
public class GoapConfig {
    public static final ForgeConfig.BooleanValue USE_GOAP =
        Config.Builder
            .define("useGoapSystem", false)
            .comment("Enable GOAP planning system (experimental)");

    public static final ForgeConfig.BooleanValue GOAP_DEBUG =
        Config.Builder
            .define("goapDebug", false)
            .comment("Show GOAP planning debug info");

    public static final ForgeConfig.DoubleValue GOAP_LLM_THRESHOLD =
        Config.Builder
            .define("goapLlmThreshold", 0.5)
            .comment("Threshold for using LLM vs GOAP (0.0 = always GOAP, 1.0 = always LLM)");
}
```

---

## 13. Conclusion

GOAP provides a powerful complement to the existing LLM-based planning in MineWright:

- **Best of both worlds**: Fast local planning + creative LLM generation
- **Performance**: 1000x faster planning for common tasks
- **Reliability**: Works offline, no API costs
- **Scalability**: Better support for multi-agent scenarios

However, GOAP also introduces significant complexity:

- **High development cost**: Requires careful action design
- **Brittleness**: Limited to pre-defined scenarios
- **Maintenance**: Ongoing tuning required

**Recommendation:**

Implement GOAP as a **tier-1 planner** for common, repetitive tasks while keeping LLM as the **tier-2 fallback** for complex, creative, or novel situations. This hybrid approach maximizes both performance and flexibility while managing development complexity.

**Next Steps:**

1. Start with Phase 1 (Foundation) - implement core GOAP classes
2. Create 3-5 basic actions to prove the concept
3. Measure performance vs. LLM planner
4. Decide on full rollout based on results

---

**Document End**

For questions or feedback, please refer to:
- MineWright Architecture Team
- C:\Users\casey\minewright\research\
- CLAUDE.md for project overview
