# GOAP Patterns: Modern Game AI Implementation Guide

**Author:** Orchestrator Research Team
**Date:** 2026-03-02
**Version:** 1.0
**Status:** Research Document
**Focus:** Practical GOAP patterns from 2024-2025 game development

---

## Table of Contents

1. [GOAP Fundamentals](#1-goap-fundamentals)
2. [How GOAP Differs from HTN and Behavior Trees](#2-how-goap-differs-from-htn-and-behavior-trees)
3. [World State Representation](#3-world-state-representation)
4. [Action Preconditions and Effects](#4-action-preconditions-and-effects)
5. [Goal State Definition](#5-goal-state-definition)
6. [Implementation Patterns](#6-implementation-patterns)
7. [Case Studies](#7-case-studies)
8. [Application to Minecraft AI](#8-application-to-minecraft-ai)
9. [Code Patterns to Steal](#9-code-patterns-to-steal)
10. [References](#10-references)

---

## 1. GOAP Fundamentals

### 1.1 What is GOAP?

**Goal-Oriented Action Planning (GOAP)** is a goal-based AI planning system that:
- Uses **backward chaining** from goals to current state
- Employs **A* search** through action space
- Enables **emergent behavior** through flexible action composition
- Originated in **F.E.A.R. (2005)** by Jeff Orkin at Monolith Productions

### 1.2 Core Philosophy

GOAP is built on three fundamental principles:

1. **Goals Over Behaviors**: Define what you want, not how to achieve it
2. **Backward Planning**: Start from goal state, work backward to current state
3. **Decoupled Goals and Actions**: Same goal can be achieved multiple ways

```
Traditional FSM:        GOAP:
┌─────────────┐         ┌──────────────┐
│ State A     │         │ Goal: Kill   │
│     │       │         │ Enemy        │
│     ▼       │         └──────┬───────┘
│ State B     │                │
│     │       │         ┌──────▼───────┐
│     ▼       │         │ Planner finds │
│ State C     │         │ action seq:  │
└─────────────┘         │ Reload→Shoot │
                        └──────────────┘
```

### 1.3 GOAP Planning Cycle

```
                    ┌─────────────────┐
                    │  Current State  │
                    │  (Observation)  │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  Select Goal    │◄────┐
                    │  (Priority)     │     │
                    └────────┬────────┘     │
                             │              │
                             ▼              │
                    ┌─────────────────┐     │
                    │  Plan Actions   │     │
                    │  (A* Search)    │     │
                    └────────┬────────┘     │
                             │              │
                             ▼              │
                    ┌─────────────────┐     │
                    │  Execute Plan   │     │
                    │  (Action Seq)   │     │
                    └────────┬────────┘     │
                             │              │
                             ▼              │
                    ┌─────────────────┐     │
                    │  Monitor State  │─────┘
                    │  (Replan if     │
                    │   Changed)      │
                    └─────────────────┘
```

---

## 2. How GOAP Differs from HTN and Behavior Trees

### 2.1 Comparison Matrix

| Aspect | GOAP | HTN (Hierarchical Task Network) | Behavior Trees (BT) |
|--------|------|--------------------------------|---------------------|
| **Planning Direction** | Backward (Goal → Current) | Forward (Current → Goal) | None (Reactive) |
| **Goal Definition** | Explicit world state | Implicit in task hierarchy | Hardcoded in tree structure |
| **Search Algorithm** | A* through state space | Recursive decomposition | Tree traversal |
| **Flexibility** | High - emergent behavior | Medium - task variants | Low - fixed paths |
| **Predictability** | Low - can surprise | High - follows methods | Very High - deterministic |
| **Designer Control** | Low - goals define behavior | High - methods encode knowledge | Very High - explicit trees |
| **Computational Cost** | High (search) | Medium (decomposition) | Low (evaluation) |
| **Learning Curve** | Steep | Moderate | Easy |
| **Best For** | Dynamic, emergent gameplay | Story-driven, predictable AI | Simple reactive behaviors |
| **Example Games** | F.E.A.R., Shadow of Mordor | KILLZONE, Civilization | Almost all modern games |

### 2.2 Decision Tree: When to Use Which

```
                    ┌──────────────────┐
                    │   Need AI for?   │
                    └─────────┬────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Is world        │
                    │  dynamic/unpred- │
                    │  ictable?        │
                    └─────────┬────────┘
                       │           │
                     Yes          No
                       │           │
                       ▼           ▼
              ┌─────────────┐ ┌─────────────┐
              │ Emergent    │ │ Predictable │
              │ behavior    │ │ behavior    │
              │ desired?    │ │ needed?     │
              └──────┬──────┘ └──────┬──────┘
                 │         │        │      │
                Yes        No      Yes     No
                 │         │        │      │
                 ▼         ▼        ▼      ▼
           ┌─────────┐ ┌─────────┐ ┌──────┐ ┌─────────┐
           │  GOAP   │ │   HTN   │ │  HTN │ │    BT   │
           │+Utility │ │Utility  │ │      │ │         │
           └─────────┘ └─────────┘ └──────┘ └─────────┘
```

### 2.3 Why GOAP for Minecraft?

Minecraft is an ideal GOAP use case because:

| Minecraft Characteristic | GOAP Advantage |
|--------------------------|----------------|
| **Open-ended goals** ("build a house") | Goals can be achieved many ways |
| **Dynamic world** (day/night, mobs, players) | Replanning adapts to changes |
| **Resource constraints** (inventory, tools) | Preconditions model resources naturally |
| **Multi-step tasks** (gather → craft → build) | A* finds optimal sequences |
| **Emergent gameplay** (unexpected solutions) | GOAP surprises even developers |
| **Modding community** | Easy to add new actions without breaking existing AI |

---

## 3. World State Representation

### 3.1 Bitmask-Based State (High Performance)

**Used in:** F.E.A.R., modern Unity implementations

```java
/**
 * Bitmask-based world state for maximum performance.
 * Each boolean state is a single bit in a long (64 states per long).
 */
public class BitmaskWorldState {
    // State variable indices (compile-time constants)
    public static final int HAS_WEAPON = 0;
    public static final int HAS_AMMO = 1;
    public static final int ENEMY_VISIBLE = 2;
    public static final int IN_COVER = 3;
    public static final int IS_RELOADING = 4;
    public static final int ENEMY_IN_RANGE = 5;
    // ... up to 63 boolean states

    private long booleanState = 0L;
    private final int[] intState;  // For integer-valued properties

    public BitmaskWorldState(int intStateSize) {
        this.intState = new int[intStateSize];
    }

    /**
     * Set boolean state (bit manipulation).
     */
    public void setBoolean(int index, boolean value) {
        if (value) {
            booleanState |= (1L << index);
        } else {
            booleanState &= ~(1L << index);
        }
    }

    /**
     * Get boolean state.
     */
    public boolean getBoolean(int index) {
        return (booleanState & (1L << index)) != 0;
    }

    /**
     * Check if multiple states are true (AND operation).
     * Used for precondition checking.
     */
    public boolean hasAllStates(long stateMask) {
        return (booleanState & stateMask) == stateMask;
    }

    /**
     * Check if any states are true (OR operation).
     */
    public boolean hasAnyState(long stateMask) {
        return (booleanState & stateMask) != 0;
    }

    /**
     * Calculate Hamming distance (number of differing bits).
     * Used as heuristic in A* search.
     */
    public int calculateBitDistance(BitmaskWorldState other) {
        long diff = this.booleanState ^ other.booleanState;
        return Long.bitCount(diff);
    }

    /**
     * Fast copy operation.
     */
    public BitmaskWorldState copy() {
        BitmaskWorldState copy = new BitmaskWorldState(intState.length);
        copy.booleanState = this.booleanState;
        System.arraycopy(this.intState, 0, copy.intState, 0, intState.length);
        return copy;
    }
}
```

**Advantages:**
- Extremely fast precondition checking (bitwise operations)
- Efficient state difference calculation
- Low memory footprint
- CPU cache friendly

**Disadvantages:**
- Limited to 64 boolean states (can extend with multiple longs)
- Not as readable/debuggable
- Requires state index management

### 3.2 Dictionary-Based State (Flexible)

**Used in:** Shadow of Mordor, modern indie games

```java
/**
 * Dictionary-based world state for maximum flexibility.
 */
public class DictionaryWorldState {
    private final Map<String, Object> properties;
    private final transient Integer hashCodeCache;

    public DictionaryWorldState() {
        this.properties = new ConcurrentHashMap<>();
    }

    /**
     * Set any property value.
     */
    public void set(String key, Object value) {
        properties.put(key, value);
        hashCodeCache = null;  // Invalidate cache
    }

    /**
     * Get property with default.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object value = properties.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Check if this state satisfies a target state.
     * All key-value pairs in target must match.
     */
    public boolean satisfies(DictionaryWorldState target) {
        for (Map.Entry<String, Object> entry : target.properties.entrySet()) {
            Object ourValue = properties.get(entry.getKey());
            if (!Objects.equals(entry.getValue(), ourValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculate state difference for heuristic.
     */
    public int calculateDifference(DictionaryWorldState other) {
        int differences = 0;

        // Count mismatched values
        for (Map.Entry<String, Object> entry : other.properties.entrySet()) {
            Object ourValue = properties.get(entry.getKey());
            if (!Objects.equals(entry.getValue(), ourValue)) {
                differences++;
            }
        }

        return differences;
    }

    /**
     * Create immutable snapshot for planning.
     */
    public DictionaryWorldState snapshot() {
        DictionaryWorldState snapshot = new DictionaryWorldState();
        snapshot.properties.putAll(this.properties);
        snapshot.properties = Collections.unmodifiableMap(snapshot.properties);
        return snapshot;
    }

    /**
     * Merge another state into this one.
     * Other state's values override ours.
     */
    public void merge(DictionaryWorldState other) {
        properties.putAll(other.properties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DictionaryWorldState)) return false;
        return properties.equals(((DictionaryWorldState) o).properties);
    }

    @Override
    public int hashCode() {
        if (hashCodeCache == null) {
            hashCodeCache = properties.hashCode();
        }
        return hashCodeCache;
    }
}
```

**Advantages:**
- Unlimited state variables
- Readable property names
- Easy to extend
- Supports any value type

**Disadvantages:**
- Slower than bitmask (hash map operations)
- Higher memory overhead
- String key overhead

### 3.3 Hybrid Approach (Best of Both)

```java
/**
 * Hybrid world state using bitmask for common booleans,
 * dictionary for complex properties.
 */
public class HybridWorldState {
    // Fast boolean states (up to 64)
    private long booleanStates;

    // Integer states (indexed by enum)
    private final int[] intStates;

    // Complex states (dictionary)
    private final Map<String, Object> complexStates;

    public enum IntState {
        HEALTH, AMMO, WOOD_COUNT, STONE_COUNT,
        POSITION_X, POSITION_Y, POSITION_Z,
        TARGET_DISTANCE
    }

    public HybridWorldState() {
        this.intStates = new int[IntState.values().length];
        this.complexStates = new HashMap<>();
    }

    /**
     * Fast boolean access.
     */
    public boolean getBoolean(int index) {
        return (booleanStates & (1L << index)) != 0;
    }

    public void setBoolean(int index, boolean value) {
        if (value) {
            booleanStates |= (1L << index);
        } else {
            booleanStates &= ~(1L << index);
        }
    }

    /**
     * Fast integer access.
     */
    public int getInt(IntState state) {
        return intStates[state.ordinal()];
    }

    public void setInt(IntState state, int value) {
        intStates[state.ordinal()] = value;
    }

    /**
     * Complex property access.
     */
    public <T> T get(String key, T defaultValue) {
        Object value = complexStates.get(key);
        return value != null ? (T) value : defaultValue;
    }

    public void set(String key, Object value) {
        complexStates.put(key, value);
    }

    /**
     * Optimized precondition checking.
     */
    public boolean satisfies(HybridWorldState target) {
        // Check boolean states first (fast path)
        if ((this.booleanStates & target.booleanStates) != target.booleanStates) {
            return false;
        }

        // Check integer states
        for (IntState state : IntState.values()) {
            int targetValue = target.intStates[state.ordinal()];
            if (targetValue != 0 && this.intStates[state.ordinal()] != targetValue) {
                return false;
            }
        }

        // Check complex states
        for (Map.Entry<String, Object> entry : target.complexStates.entrySet()) {
            Object ourValue = complexStates.get(entry.getKey());
            if (!Objects.equals(entry.getValue(), ourValue)) {
                return false;
            }
        }

        return true;
    }
}
```

---

## 4. Action Preconditions and Effects

### 4.1 Action Interface (Modern Design)

```java
/**
 * Modern GOAP action interface with support for:
 * - Dynamic preconditions
 * - Probabilistic effects
 * - Duration estimation
 * - Cost functions
 */
public interface GoapAction {

    /**
     * Checks if action can execute in current state.
     * May consider dynamic factors (distance, resources).
     */
    boolean canExecute(WorldState state);

    /**
     * Simulates action effects on state copy.
     * Used during planning (not actual execution).
     */
    WorldState simulateEffects(WorldState state);

    /**
     * Executes the action in real world.
     * Returns true if execution started successfully.
     */
    boolean execute(WorldState state, ActionContext context);

    /**
     * Gets action cost for A* search.
     * Can be static or dynamic.
     */
    int getCost(WorldState state);

    /**
     * Gets preconditions as state template.
     */
    WorldState getPreconditions();

    /**
     * Gets effects as state template.
     */
    WorldState getEffects();

    /**
     * Checks if action is still running.
     */
    boolean isRunning();

    /**
     * Cancels running action.
     */
    void cancel();

    /**
     * Estimated execution duration (ticks).
     * Used for action scheduling.
     */
    default int getEstimatedDuration() {
        return 1;  // Default: instant
    }

    /**
     * Action name for debugging.
     */
    String getName();

    /**
     * Action type for categorization.
     */
    default ActionType getType() {
        return ActionType.GENERIC;
    }

    enum ActionType {
        COMBAT, MOVEMENT, GATHERING, CRAFTING,
        BUILDING, SURVIVAL, SOCIAL, GENERIC
    }
}
```

### 4.2 Base Action Implementation

```java
/**
 * Base action with common functionality.
 */
public abstract class BaseGoapAction implements GoapAction {

    protected final String name;
    protected final WorldState preconditions;
    protected final WorldState effects;
    protected final int baseCost;
    protected boolean isRunning;

    public BaseGoapAction(String name, int baseCost) {
        this.name = name;
        this.baseCost = baseCost;
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
        newState.merge(effects);
        return newState;
    }

    @Override
    public int getCost(WorldState state) {
        return baseCost;  // Can override for dynamic cost
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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("%s(cost=%d)", name, baseCost);
    }
}
```

### 4.3 Concrete Action Examples

#### Combat Actions

```java
/**
 * Shoot action with ammo management.
 */
public class ShootAction extends BaseGoapAction {

    private static final int AMMO_COST = 1;

    public ShootAction() {
        super("Shoot", 1);

        // Preconditions
        preconditions.set("hasWeapon", true);
        preconditions.set("ammoCount", AMMO_COST);
        preconditions.set("enemyVisible", true);
        preconditions.set("weaponReady", true);

        // Effects
        effects.set("ammoCount", -AMMO_COST);  // Decrements by 1
        effects.set("enemyHealth", -10);       // Decrements by 10
        effects.set("enemyAlerted", true);
    }

    @Override
    public boolean canExecute(WorldState state) {
        // Additional dynamic check
        int distance = state.getInt("enemyDistance", 100);
        if (distance > 50) {
            return false;  // Out of range
        }

        return super.canExecute(state);
    }

    @Override
    public int getCost(WorldState state) {
        // Dynamic cost based on ammo scarcity
        int ammo = state.getInt("ammoCount", 0);
        if (ammo < 10) {
            return baseCost * 3;  // Triple cost when low on ammo
        }
        return baseCost;
    }

    @Override
    public boolean execute(WorldState state, ActionContext context) {
        isRunning = true;

        // Consume ammo
        int currentAmmo = state.getInt("ammoCount", 0);
        state.set("ammoCount", currentAmmo - AMMO_COST);

        // Apply damage (would be in game code)
        // context.getEnemy().takeDamage(10);

        isRunning = false;
        return true;
    }

    @Override
    public int getEstimatedDuration() {
        return 5;  // 5 ticks (~250ms)
    }
}

/**
 * Reload action with duration.
 */
public class ReloadAction extends BaseGoapAction {

    private final int reloadAmount;
    private int ticksRemaining;

    public ReloadAction(int reloadAmount) {
        super("Reload", 5);
        this.reloadAmount = reloadAmount;

        preconditions.set("hasWeapon", true);
        preconditions.set("hasAmmoReserve", true);

        effects.set("ammoCount", reloadAmount);
        effects.set("weaponReady", false);  // Not ready during reload
    }

    @Override
    public boolean execute(WorldState state, ActionContext context) {
        if (!isRunning) {
            // Start reload
            isRunning = true;
            ticksRemaining = 40;  // 2 seconds at 20 TPS
            return true;
        }

        // Continue reload
        ticksRemaining--;

        if (ticksRemaining <= 0) {
            // Reload complete
            state.set("ammoCount", reloadAmount);
            state.set("weaponReady", true);
            isRunning = false;
        }

        return true;
    }

    @Override
    public int getEstimatedDuration() {
        return 40;  // 40 ticks
    }

    @Override
    public ActionType getType() {
        return ActionType.COMBAT;
    }
}

/**
 * Take cover action with pathfinding.
 */
public class TakeCoverAction extends BaseGoapAction {

    private BlockPos targetCover;
    private boolean isMoving;

    public TakeCoverAction() {
        super("TakeCover", 3);

        preconditions.set("enemyVisible", true);
        preconditions.set("inCover", false);
        preconditions.set("hasPathToCover", true);

        effects.set("inCover", true);
        effects.set("enemyVisible", false);  // Can't see from cover
    }

    @Override
    public boolean canExecute(WorldState state) {
        // Find cover if not already found
        if (targetCover == null) {
            targetCover = findNearestCover(state);
            if (targetCover == null) {
                return false;  // No cover available
            }
            state.set("hasPathToCover", true);
        }

        return super.canExecute(state);
    }

    @Override
    public boolean execute(WorldState state, ActionContext context) {
        if (!isRunning) {
            isRunning = true;
            isMoving = true;

            // Start pathfinding to cover
            // context.getNavigation().moveTo(targetCover);
            return true;
        }

        // Check if arrived
        if (isMoving && hasArrived(context)) {
            isMoving = false;
            state.set("inCover", true);
            state.set("enemyVisible", false);
            isRunning = false;
        }

        return true;
    }

    private BlockPos findNearestCover(WorldState state) {
        // In real implementation, search for cover blocks
        // For now, return null (not implemented)
        return null;
    }

    private boolean hasArrived(ActionContext context) {
        // Check if entity is at target position
        // return context.getNavigation().isAtTarget();
        return false;
    }

    @Override
    public void cancel() {
        isRunning = false;
        isMoving = false;
        targetCover = null;
    }

    @Override
    public ActionType getType() {
        return ActionType.COMBAT;
    }
}
```

#### Minecraft Actions

```java
/**
 * Mine block action for Minecraft.
 */
public class MineBlockAction extends BaseGoapAction {

    private final String blockType;
    private BlockPos targetBlock;
    private boolean isMoving;
    private boolean isMining;
    private int miningTicks;

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
    public boolean canExecute(WorldState state) {
        // Find target block if not set
        if (targetBlock == null) {
            targetBlock = findNearestBlock(state, blockType);
            if (targetBlock == null) {
                return false;  // No block found
            }
            state.set("near_" + blockType, true);
        }

        // Check inventory space
        if (state.getBoolean("inventoryFull", false)) {
            return false;
        }

        return super.canExecute(state);
    }

    @Override
    public boolean execute(WorldState state, ActionContext context) {
        if (!isRunning) {
            isRunning = true;
            isMoving = true;

            // Pathfind to block
            // context.getNavigation().moveTo(targetBlock);
            return true;
        }

        if (isMoving) {
            // Check if arrived at block
            if (hasArrived(context)) {
                isMoving = false;
                isMining = true;
                miningTicks = calculateMiningTime(state);
            }
        } else if (isMining) {
            // Continue mining
            miningTicks--;

            if (miningTicks <= 0) {
                // Mining complete
                // context.breakBlock(targetBlock);

                state.set("has_" + blockType, true);
                state.set("near_" + blockType, false);

                // Check inventory
                // if (context.getInventory().isFull()) {
                //     state.set("inventoryFull", true);
                // }

                isRunning = false;
                isMining = false;
            }
        }

        return true;
    }

    private BlockPos findNearestBlock(WorldState state, String blockType) {
        // In real implementation, search nearby chunks
        // For now, return null
        return null;
    }

    private int calculateMiningTime(WorldState state) {
        // Calculate mining time based on tool and block
        String tool = state.getString("toolType", "wooden_pickaxe");
        int hardness = getBlockHardness(blockType);
        int toolSpeed = getToolSpeed(tool);

        return hardness / toolSpeed;
    }

    private int getBlockHardness(String blockType) {
        // Block hardness values
        return switch (blockType) {
            case "stone" -> 30;
            case "iron_ore" -> 40;
            case "diamond_ore" -> 50;
            default -> 20;
        };
    }

    private int getToolSpeed(String toolType) {
        // Tool mining speeds
        return switch (toolType) {
            case "wooden_pickaxe" -> 2;
            case "stone_pickaxe" -> 4;
            case "iron_pickaxe" -> 6;
            case "diamond_pickaxe" -> 8;
            default -> 1;
        };
    }

    private boolean hasArrived(ActionContext context) {
        // Check distance to target
        // return context.getNavigation().isAtTarget();
        return false;
    }

    @Override
    public void cancel() {
        isRunning = false;
        isMoving = false;
        isMining = false;
        targetBlock = null;
    }

    @Override
    public ActionType getType() {
        return ActionType.GATHERING;
    }
}

/**
 * Craft item action.
 */
public class CraftItemAction extends BaseGoapAction {

    private final String outputItem;
    private final Map<String, Integer> requiredMaterials;
    private boolean isMoving;
    private boolean isCrafting;

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
    public boolean canExecute(WorldState state) {
        // Check crafting table proximity
        if (!state.getBoolean("near_craftingTable", false)) {
            return false;
        }

        // Check all materials
        for (String material : requiredMaterials.keySet()) {
            if (!state.getBoolean("has_" + material, false)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean execute(WorldState state, ActionContext context) {
        if (!isRunning) {
            isRunning = true;
            isCrafting = true;

            // Consume materials
            for (Map.Entry<String, Integer> material : requiredMaterials.entrySet()) {
                // context.getInventory().remove(material.getKey(), material.getValue());
                state.set("has_" + material.getKey(), false);
            }

            // Start crafting
            // context.startCrafting(outputItem);
            return true;
        }

        if (isCrafting) {
            // Check if crafting complete
            // if (context.isCraftingComplete()) {
                // Add crafted item
                // context.getInventory().add(outputItem);
                state.set("has_" + outputItem, true);

                isRunning = false;
                isCrafting = false;
            // }
        }

        return true;
    }

    @Override
    public ActionType getType() {
        return ActionType.CRAFTING;
    }
}
```

---

## 5. Goal State Definition

### 5.1 Goal Interface

```java
/**
 * GOAP goal interface with priority and validation.
 */
public interface GoapGoal {

    /**
     * Gets target world state (what we want to achieve).
     */
    WorldState getTargetState();

    /**
     * Gets priority (higher = more important).
     * Can be dynamic based on current state.
     */
    int getPriority(WorldState currentState);

    /**
     * Checks if goal is achieved.
     */
    boolean isAchieved(WorldState state);

    /**
     * Goal name for debugging.
     */
    String getName();

    /**
     * Optional: Invalidate condition (when to abandon goal).
     */
    default boolean shouldInvalidate(WorldState state) {
        return false;  // Default: never invalidate
    }

    /**
     * Goal category for priority arbitration.
     */
    default GoalCategory getCategory() {
        return GoalCategory.GENERAL;
    }

    enum GoalCategory {
        SURVIVAL,      // Health, hunger, safety
        COMBAT,        // Fighting enemies
        GATHERING,     // Collecting resources
        CRAFTING,      // Making items
        BUILDING,      // Construction
        EXPLORATION,   // Discovering
        SOCIAL,        // Interacting with players/agents
        GENERAL        // Default
    }
}
```

### 5.2 Base Goal Implementation

```java
/**
 * Base goal with common functionality.
 */
public abstract class BaseGoapGoal implements GoapGoal {

    protected final String name;
    protected final WorldState targetState;
    protected int basePriority;
    protected final GoalCategory category;

    public BaseGoapGoal(String name, int basePriority) {
        this(name, basePriority, GoalCategory.GENERAL);
    }

    public BaseGoapGoal(String name, int basePriority, GoalCategory category) {
        this.name = name;
        this.basePriority = basePriority;
        this.category = category;
        this.targetState = new WorldState();
    }

    @Override
    public WorldState getTargetState() {
        return targetState;
    }

    @Override
    public int getPriority(WorldState currentState) {
        return basePriority;  // Can override for dynamic priority
    }

    @Override
    public boolean isAchieved(WorldState state) {
        return state.satisfies(targetState);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public GoalCategory getCategory() {
        return category;
    }

    /**
     * Helper to add target condition.
     */
    protected void addTarget(String key, Object value) {
        targetState.set(key, value);
    }

    @Override
    public String toString() {
        return String.format("%s(priority=%d, category=%s)",
            name, basePriority, category);
    }
}
```

### 5.3 Dynamic Priority Goals

```java
/**
 * Survival goal with urgency-based priority.
 */
public class SurvivalGoal extends BaseGoapGoal {

    public SurvivalGoal() {
        super("Survival", 10, GoalCategory.SURVIVAL);
        addTarget("health", 100);
        addTarget("isSafe", true);
    }

    @Override
    public int getPriority(WorldState state) {
        int health = state.getInt("health", 100);
        boolean isSafe = state.getBoolean("isSafe", false);

        // Priority skyrockets when health critical
        if (health < 20) {
            return 1000;  // Emergency
        } else if (health < 50) {
            return 100;  // High priority
        }

        // Increase priority when in danger
        if (!isSafe) {
            return 50;
        }

        return basePriority;  // Low priority when healthy and safe
    }

    @Override
    public boolean isAchieved(WorldState state) {
        int health = state.getInt("health", 0);
        boolean isSafe = state.getBoolean("isSafe", false);
        return health >= 80 && isSafe;
    }

    @Override
    public boolean shouldInvalidate(WorldState state) {
        // Never invalidate survival goal
        return false;
    }
}

/**
 * Combat goal with engagement-based priority.
 */
public class KillEnemyGoal extends BaseGoapGoal {

    public KillEnemyGoal() {
        super("KillEnemy", 20, GoalCategory.COMBAT);
        addTarget("enemyHealth", 0);
    }

    @Override
    public int getPriority(WorldState state) {
        boolean enemyVisible = state.getBoolean("enemyVisible", false);
        int enemyDistance = state.getInt("enemyDistance", 100);
        int playerHealth = state.getInt("health", 100);

        // Very high priority when enemy can see us
        if (enemyVisible && enemyDistance < 20) {
            return 200;  // Immediate threat
        }

        // Decrease priority if we're low on health
        if (playerHealth < 30) {
            return 5;  // Survival first
        }

        // Medium priority when enemy is visible
        if (enemyVisible) {
            return 50;
        }

        return basePriority;
    }

    @Override
    public boolean shouldInvalidate(WorldState state) {
        // Abandon if enemy too far or we're dying
        int enemyDistance = state.getInt("enemyDistance", 100);
        int health = state.getInt("health", 100);

        return enemyDistance > 100 || health < 20;
    }
}

/**
 * Resource gathering goal with threshold-based priority.
 */
public class GatherResourcesGoal extends BaseGoapGoal {

    private final String resourceType;
    private final int targetQuantity;
    private final int criticalThreshold;

    public GatherResourcesGoal(String resourceType, int targetQuantity) {
        super("Gather_" + resourceType, 5, GoalCategory.GATHERING);
        this.resourceType = resourceType;
        this.targetQuantity = targetQuantity;
        this.criticalThreshold = targetQuantity / 4;  // 25% = critical

        addTarget("has_" + resourceType, true);
    }

    @Override
    public int getPriority(WorldState state) {
        int currentCount = state.getInt(resourceType + "_count", 0);

        if (currentCount < criticalThreshold) {
            return 50;  // High priority when critical
        } else if (currentCount < targetQuantity / 2) {
            return 20;  // Medium priority when low
        }

        return basePriority;
    }

    @Override
    public boolean isAchieved(WorldState state) {
        int currentCount = state.getInt(resourceType + "_count", 0);
        return currentCount >= targetQuantity;
    }
}
```

---

## 6. Implementation Patterns

### 6.1 A* Search Through Action Space

**GOAP uses backward A* search:**
- Start from goal state
- Work backward to current state
- Each node is a world state
- Each edge is an action (effects → preconditions)

```java
/**
 * A* planner for GOAP using backward search.
 */
public class GoapPlanner {

    private static final int MAX_ITERATIONS = 1000;
    private static final int MAX_PLAN_LENGTH = 50;

    /**
     * Plans action sequence from current state to goal.
     * Uses backward A* search.
     */
    public List<GoapAction> plan(WorldState currentState,
                                  GoapGoal goal,
                                  List<GoapAction> availableActions) {

        WorldState goalState = goal.getTargetState();

        // Goal already achieved
        if (currentState.satisfies(goalState)) {
            return Collections.emptyList();
        }

        // A* initialization
        PriorityQueue<PlanNode> openSet = new PriorityQueue<>();
        Set<WorldState> closedSet = new HashSet<>();

        // Start from goal (backward planning)
        PlanNode startNode = new PlanNode(
            goalState, null, null,
            0, calculateHeuristic(goalState, currentState)
        );
        openSet.add(startNode);

        int iterations = 0;

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;

            // Get node with lowest f-cost
            PlanNode current = openSet.poll();

            // Check if we've reached current state
            if (currentState.satisfies(current.getState())) {
                List<GoapAction> plan = current.getActionPath();

                if (plan.size() <= MAX_PLAN_LENGTH) {
                    return plan;
                } else {
                    return null;  // Plan too long
                }
            }

            closedSet.add(current.getState());

            // Expand node: find actions that could lead to this state
            for (GoapAction action : availableActions) {
                // In backward planning, we look for actions whose
                // effects could help achieve current state
                WorldState predecessorState = calculatePredecessorState(
                    current.getState(), action, currentState
                );

                if (predecessorState == null) {
                    continue;  // Action not relevant
                }

                if (closedSet.contains(predecessorState)) {
                    continue;  // Already evaluated
                }

                // Calculate costs
                int tentativeGCost = current.getGCost() + action.getCost(currentState);
                int hCost = calculateHeuristic(predecessorState, currentState);

                PlanNode neighbor = new PlanNode(
                    predecessorState, action, current,
                    tentativeGCost, hCost
                );

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

    /**
     * Calculates predecessor state for backward planning.
     * Returns state needed BEFORE applying action to reach target.
     */
    private WorldState calculatePredecessorState(WorldState targetState,
                                                  GoapAction action,
                                                  WorldState currentState) {
        WorldState effects = action.getEffects();
        WorldState preconditions = action.getPreconditions();

        // Check if action's effects are compatible with target
        for (String key : effects.getAllKeys()) {
            Object targetValue = targetState.get(key);
            Object effectValue = effects.get(key);

            if (targetValue != null && !targetValue.equals(effectValue)) {
                // Incompatible - this action can't help reach target
                return null;
            }
        }

        // Build predecessor state
        WorldState predecessor = targetState.copy();

        // Add preconditions as requirements
        for (String key : preconditions.getAllKeys()) {
            predecessor.set(key, preconditions.get(key));
        }

        return predecessor;
    }

    /**
     * Heuristic function for A*.
     * Uses state difference as admissible heuristic.
     */
    private int calculateHeuristic(WorldState from, WorldState to) {
        int differences = 0;

        for (String key : to.getAllKeys()) {
            if (!Objects.equals(from.get(key), to.get(key))) {
                differences++;
            }
        }

        return differences;
    }
}
```

### 6.2 Action Cost Functions

**Static Cost:**

```java
public class StaticCostFunction {
    public int calculateCost(GoapAction action) {
        return action.getCost();  // Fixed cost
    }
}
```

**Distance-Based Cost:**

```java
public class DistanceCostFunction {
    public int calculateCost(GoapAction action, WorldState state) {
        int baseCost = action.getCost();

        if (action instanceof MovementAction) {
            BlockPos target = ((MovementAction) action).getTarget(state);
            BlockPos current = getCurrentPosition(state);

            double distance = Math.sqrt(
                Math.pow(target.getX() - current.getX(), 2) +
                Math.pow(target.getY() - current.getY(), 2) +
                Math.pow(target.getZ() - current.getZ(), 2)
            );

            return baseCost + (int) distance;
        }

        return baseCost;
    }
}
```

**Resource-Aware Cost:**

```java
public class ResourceAwareCostFunction {
    public int calculateCost(GoapAction action, WorldState state) {
        int baseCost = action.getCost();

        // Increase cost when resources scarce
        if (action instanceof ConsumeResourceAction) {
            String resource = ((ConsumeResourceAction) action).getResource();
            int current = state.getInt(resource, 0);

            if (current < 10) {
                return baseCost * 3;
            } else if (current < 20) {
                return baseCost * 2;
            }
        }

        return baseCost;
    }
}
```

**Time-of-Day Cost:**

```java
public class TimeOfDayCostFunction {
    public int calculateCost(GoapAction action, WorldState state) {
        int baseCost = action.getCost();
        boolean isNight = state.getBoolean("isNight", false);

        // Combat riskier at night
        if (isNight && action instanceof CombatAction) {
            return baseCost * 2;
        }

        // Stealth easier at night
        if (isNight && action instanceof StealthAction) {
            return baseCost / 2;
        }

        return baseCost;
    }
}
```

### 6.3 Replanning Triggers

```java
/**
 * Monitors world state and triggers replanning when needed.
 */
public class ReplanningMonitor {

    private WorldState lastPlannedState;
    private static final int STATE_DIFFERENCE_THRESHOLD = 3;

    /**
     * Checks if replanning is needed.
     */
    public boolean shouldReplan(WorldState currentState,
                                List<GoapAction> currentPlan) {

        // No plan - need to plan
        if (currentPlan == null || currentPlan.isEmpty()) {
            return true;
        }

        // State changed significantly
        if (lastPlannedState != null) {
            int differences = calculateStateDifference(currentState, lastPlannedState);
            if (differences > STATE_DIFFERENCE_THRESHOLD) {
                return true;
            }
        }

        // Plan invalid (preconditions no longer met)
        for (GoapAction action : currentPlan) {
            if (!action.canExecute(currentState)) {
                return true;
            }
        }

        return false;
    }

    private int calculateStateDifference(WorldState s1, WorldState s2) {
        int differences = 0;

        for (String key : s1.getAllKeys()) {
            if (!Objects.equals(s1.get(key), s2.get(key))) {
                differences++;
            }
        }

        return differences;
    }

    public void setLastPlannedState(WorldState state) {
        this.lastPlannedState = state.copy();
    }
}
```

### 6.4 Action Executor

```java
/**
 * Executes GOAP action plans.
 */
public class ActionExecutor {

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
     * Sets new plan to execute.
     */
    public void setPlan(List<GoapAction> plan) {
        if (plan == null || plan.isEmpty()) {
            return;
        }

        // Cancel current action
        if (currentAction != null && currentAction.isRunning()) {
            currentAction.cancel();
        }

        this.actionQueue = new LinkedList<>(plan);
        this.currentAction = null;
    }

    /**
     * Tick update - call once per game tick.
     * Returns true when plan complete.
     */
    public boolean tick() {
        // Update world state
        updateWorldState();

        // Start next action if needed
        if (currentAction == null || !currentAction.isRunning()) {
            if (!actionQueue.isEmpty()) {
                currentAction = actionQueue.poll();

                if (currentAction.canExecute(worldState)) {
                    currentAction.execute(worldState, context);
                } else {
                    return false;  // Plan invalid
                }
            } else {
                return true;  // All actions complete
            }
        }

        return false;  // Still executing
    }

    private void updateWorldState() {
        // Sync world state with game
    }

    public void cancel() {
        if (currentAction != null && currentAction.isRunning()) {
            currentAction.cancel();
        }
        actionQueue.clear();
        currentAction = null;
    }
}
```

---

## 7. Case Studies

### 7.1 F.E.A.R. (2005) - Original GOAP Implementation

**Key Innovations:**
- First mainstream use of GOAP
- Backward chaining from goals
- A* search through action space
- Emergent tactical behavior

**Action Examples:**
```
Action: Grenade
- Preconditions: hasGrenade=true, enemyVisible=true, inCover=false
- Effects: hasGrenade=false, enemyHealth=-50, enemyAlerted=true
- Cost: 10

Action: FlankLeft
- Preconditions: enemyVisible=true, leftFlankOpen=true
- Effects: playerPosition=leftFlank, enemySuppressed=false
- Cost: 5
```

**Results:**
- Enemies appeared intelligent and unpredictable
- Players praised AI for "human-like" tactics
- Set new standard for FPS AI

### 7.2 Shadow of Mordor (2014) - Nemesis System

**GOAP Enhancements:**
- **Layered GOAP:** High-level strategic goals, low-level tactical actions
- **Social Relationships:** Goals based on nemesis hierarchy
- **Memory System:** Actions affected by past encounters
- **Dynamic Goals:** Goals created from story events

**Architecture:**
```
High-Level Planner (GOAP)
├── Strategic Goals: "Kill Player", "Recruit Follower", "Ambush"
└── Outputs: Tactical objectives

Mid-Level System (Behavior Trees)
├── Executes tactical objectives
└── Outputs: Immediate actions

Low-Level Systems (FSM)
├── Animation, movement, combat
└── Executes immediate actions
```

**Key Insight:**
> "GOAP handled the 'what' and 'why', behavior trees handled the 'how'."

### 7.3 Kenshi (2018) - Open-World Squad AI

**GOAP for Survival:**
- **Needs-Based Goals:** Hunger, health, money
- **Dynamic Priorities:** Starving > fighting > working
- **Squad Coordination:** Goals based on squad role

**Goal Prioritization:**
```
Critical Health (priority: 1000)
→ Find healing, flee combat

Starving (priority: 500)
→ Find food, hunt, steal

No Money (priority: 100)
→ Work, trade, beg

Under Attack (priority: 200)
→ Fight, flee, call allies
```

### 7.4 Modern Implementations (2024-2025)

**Unity GOAP Libraries:**
- Multi-threaded planners
- Visual debugging tools
- Hot-reload of actions
- Editor integration

**Trends:**
1. **Hybrid Systems:** GOAP + Utility AI + Behavior Trees
2. **Learning:** RL for action cost tuning
3. **Tools:** Visual planners, debuggers
4. **Multi-Agent:** Shared planning, coordination

---

## 8. Application to Minecraft AI

### 8.1 Why GOAP for Steve/MineWright?

| Steve AI Feature | GOAP Benefit |
|-----------------|--------------|
| **Natural Language Commands** | Goals can be generated from LLM |
| **Dynamic World** | Replanning adapts to changes |
| **Resource Management** | Preconditions model inventory |
| **Multi-Step Tasks** | A* finds optimal sequences |
| **Emergent Behavior** | Surprising solutions |
| **Multi-Agent** | Shared action library |

### 8.2 Integration with Existing HTN Planner

**Hybrid Approach:**

```java
/**
 * Hybrid planner using HTN for high-level, GOAP for low-level.
 */
public class HybridPlanner {

    private final HTNPlanner htnPlanner;
    private final GoapPlanner goapPlanner;

    /**
     * Plans using HTN for strategy, GOAP for tactics.
     */
    public List<GoapAction> plan(WorldState state, GoapGoal goal) {
        // HTN decomposes goal into abstract tasks
        List<HTNTask> tasks = htnPlanner.decompose(goalToTask(goal), state);

        List<GoapAction> fullPlan = new ArrayList<>();

        // GOAP plans execution for each task
        for (HTNTask task : tasks) {
            GoapGoal taskGoal = taskToGoal(task);
            List<GoapAction> taskPlan = goapPlanner.plan(
                state, taskGoal, getActionsForTask(task)
            );

            if (taskPlan == null) {
                return null;  // Task unachievable
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

### 8.3 Minecraft-Specific World State

```java
/**
 * Minecraft world state for GOAP.
 */
public class MinecraftWorldState extends HybridWorldState {

    // Boolean states
    public static final int HAS_TOOL = 0;
    public static final int HAS_WEAPON = 1;
    public static final int IN_DANGER = 2;
    public static final int IS_NIGHT = 3;
    public static final int NEAR_WATER = 4;
    public static final int NEAR_LAVA = 5;
    public static final int INVENTORY_FULL = 6;
    public static final int HAS_FOOD = 7;
    public static final int UNDERGROUND = 8;
    public static final int RAINING = 9;

    // Integer states
    public enum IntState {
        HEALTH, FOOD_LEVEL, OXYGEN,
        WOOD_COUNT, STONE_COUNT, IRON_COUNT,
        X, Y, Z,
        TARGET_X, TARGET_Y, TARGET_Z,
        EXPERIENCE_LEVEL
    }

    // Complex states
    public void setBiome(String biome) {
        set("biome", biome);
    }

    public void setDimension(String dimension) {
        set("dimension", dimension);
    }

    public void setNearbyBlocks(List<String> blocks) {
        set("nearbyBlocks", blocks);
    }

    public void setInventoryContents(Map<String, Integer> items) {
        set("inventory", items);
    }

    public void setKnownLocations(Map<String, BlockPos> locations) {
        set("locations", locations);
    }
}
```

### 8.4 Minecraft Action Library

**Movement Actions:**
```java
// MoveToBlock (target: BlockPos)
// FollowEntity (target: Entity)
// Wander
// Flee (from: Entity)
// Patrol (points: List<BlockPos>)
```

**Gathering Actions:**
```java
// MineBlock (blockType: String)
// ChopTree
// HarvestCrop
// CollectItem (itemType: String)
```

**Crafting Actions:**
```java
// CraftItem (recipe: Recipe)
// SmeltOre (oreType: String)
// EnchantItem
// RepairItem
```

**Building Actions:**
```java
// PlaceBlock (blockType: String, position: BlockPos)
// BuildStructure (structure: Structure)
// ClearArea (size: int)
```

**Survival Actions:**
```java
// EatFood
// Heal
// Sleep
// EquipArmor
```

### 8.5 Example: "Build a House" Goal

**Goal Definition:**
```java
WorldState goalState = new WorldState();
goalState.set("has_shelter", true);
goalState.set("shelter_position", targetPos);
goalState.set("shelter_type", "wooden_house");

GoapGoal buildHouse = new BaseGoapGoal("BuildHouse", 15) {
    @Override
    public WorldState getTargetState() {
        return goalState;
    }
};
```

**Generated Plan (Example):**
```
1. MoveToForest (find trees)
2. ChopTree x16 (get wood)
3. CraftPlanks x64 (wood -> planks)
4. CraftSticks x16 (planks -> sticks)
5. CraftCraftingTable (planks)
6. PlaceCraftingTable (at site)
7. MoveToSite (to build location)
8. PlaceBlocks (build walls)
9. CraftDoor (planks + sticks)
10. PlaceDoor (add door)
11. PlaceTorches (lighting)
```

### 8.6 Integration with ProcessManager

```java
/**
 * GOAP-based behavior process for ProcessManager.
 */
public class GoapProcess implements BehaviorProcess {

    private final GoapAgent goapAgent;
    private int priority = 50;

    public GoapProcess(ForemanEntity foreman) {
        this.goapAgent = new GoapAgent(foreman);
    }

    @Override
    public boolean canRun() {
        // Can run if we have achievable goals
        WorldState state = goapAgent.getWorldState();
        return goapAgent.getGoalSelector().hasAchievableGoals(state);
    }

    @Override
    public int getPriority() {
        // Dynamic priority based on urgency
        GoapGoal currentGoal = goapAgent.getCurrentGoal();
        if (currentGoal != null) {
            return currentGoal.getPriority(goapAgent.getWorldState());
        }
        return priority;
    }

    @Override
    public void tick() {
        goapAgent.tick();
    }

    @Override
    public void onActivate() {
        // Called when process gains control
    }

    @Override
    public void onDeactivate() {
        // Cancel current plan
        goapAgent.cancel();
    }

    @Override
    public String getName() {
        return "GOAP";
    }
}
```

---

## 9. Code Patterns to Steal

### 9.1 ReGoap (C# Unity) - Best Open Source GOAP

**URL:** https://github.com/sploreg/goap

**Key Features:**
- Multi-threaded planning
- Component-based architecture
- Visual debugger
- Hot-reload support

**Patterns to Steal:**

1. **Action Component:**
```csharp
public class GoapAction : MonoBehaviour {
    public HashSet<KeyValuePair<string, object>> preconditions;
    public HashSet<KeyValuePair<string, object>> effects;

    public virtual bool PrePerform(GameObject agent) { }
    public virtual bool PostPerform(GameObject agent) { }
}
```

2. **Sensor System:**
```csharp
public interface IGoapSensor {
    void UpdateSensor(GameObject agent);
    HashSet<KeyValuePair<string, object>> GetWorldState();
}
```

3. **Data Provider:**
```csharp
public interface IGoapDataProvider {
    IDictionary<string, object> GetWorldState();
    void PlanFound(IDictionary<string, object> goal, Queue<GoapAction> actions);
    void PlanFailed(IDictionary<string, object> goal);
    void ActionsFinished();
    void PlanAborted(GoapAction aborter);
}
```

### 9.2 CrashKonijn.GOAP (Unity 2024)

**URL:** https://crashkonijn.com/goap

**Key Features:**
- Modern async/await patterns
- Compiled actions (better performance)
- Visual editor

**Patterns to Steal:**

1. **Action Builder:**
```csharp
public class ActionBuilder {
    public ActionBuilder(string name) {
        Name = name;
        Preconditions = new WorldState(true);
        Effects = new WorldState(true);
    }

    public ActionBuilder AddEffect(string key, bool value) {
        Effects.Set(key, value);
        return this;
    }

    public IAction Build() {
        return new CompiledAction(Name, Preconditions, Effects, Cost);
    }
}
```

2. **Goal Inspector:**
```csharp
public class GoalInspector {
    public void Inspect(IAction[] actions, IGoal[] goals) {
        foreach (var goal in goals) {
            var isValid = IsValid(goal, actions);
            Debug.Log($"{goal.Name}: {(isValid ? "Valid" : "Invalid")}");
        }
    }
}
```

### 9.3 Embabel (Java) - OODA Loop Integration

**URL:** https://segmentfault.com/p/1210000046614005

**Key Features:**
- GOAP + OODA (Observe-Orient-Decide-Act)
- Reactive planning
- Event-driven updates

**Patterns to Steal:**

1. **OODA-Goap Integration:**
```java
public class OodaGoapAgent {
    public void observe() {
        // Update world state from sensors
        worldState.update();
    }

    public void orient() {
        // Assess situation, adjust goal priorities
        goalSelector.reprioritize(worldState);
    }

    public void decide() {
        // Generate plan
        currentPlan = planner.plan(worldState, currentGoal);
    }

    public void act() {
        // Execute plan
        executor.execute(currentPlan);
    }
}
```

### 9.4 Action Validation Pattern

```java
/**
 * Validates action definitions at startup.
 */
public class ActionValidator {

    public ValidationResult validate(GoapAction action) {
        ValidationResult result = new ValidationResult();

        // Check preconditions not empty
        if (action.getPreconditions().isEmpty()) {
            result.addWarning("Action has no preconditions");
        }

        // Check effects not empty
        if (action.getEffects().isEmpty()) {
            result.addError("Action has no effects");
        }

        // Check cost positive
        if (action.getCost(null) <= 0) {
            result.addWarning("Action has zero or negative cost");
        }

        // Check for circular effects
        if (hasCircularEffects(action)) {
            result.addError("Action has circular effects");
        }

        return result;
    }

    private boolean hasCircularEffects(GoapAction action) {
        WorldState preconditions = action.getPreconditions();
        WorldState effects = action.getEffects();

        // Check if effects negate preconditions
        for (String key : effects.getAllKeys()) {
            if (preconditions.hasProperty(key)) {
                Object preValue = preconditions.get(key);
                Object effectValue = effects.get(key);

                if (Objects.equals(preValue, effectValue)) {
                    return true;  // Circular
                }
            }
        }

        return false;
    }
}
```

### 9.5 Planner Optimization Techniques

**1. Action Caching:**
```java
/**
 * Cache applicable actions for each state pattern.
 */
public class ActionCache {
    private final Map<WorldState, List<GoapAction>> cache;

    public List<GoapAction> getApplicableActions(WorldState state) {
        return cache.computeIfAbsent(state, this::findApplicableActions);
    }

    private List<GoapAction> findApplicableActions(WorldState state) {
        // Filter actions by preconditions
    }
}
```

**2. Hierarchical Planning:**
```java
/**
 * Plan high-level first, then refine each step.
 */
public class HierarchicalPlanner {

    public List<GoapAction> plan(WorldState state, GoapGoal goal) {
        // High-level plan (abstract actions)
        List<GoapAction> highLevel = planHighLevel(state, goal);

        List<GoapAction> detailed = new ArrayList<>();

        // Refine each abstract action
        for (GoapAction abstractAction : highLevel) {
            List<GoapAction> refined = refineAction(abstractAction, state);
            detailed.addAll(refined);
            state = applyActions(state, refined);
        }

        return detailed;
    }
}
```

**3. Incremental Replanning:**
```java
/**
 * Replan only from current action, not from scratch.
 */
public class IncrementalPlanner {

    public List<GoapAction> replan(WorldState state,
                                   List<GoapAction> oldPlan,
                                   GoapAction currentAction) {

        // Build new plan starting from current action
        List<GoapAction> newPlan = new ArrayList<>();
        newPlan.add(currentAction);

        // Plan remaining actions
        WorldState projectedState = currentAction.simulateEffects(state);
        List<GoapAction> remaining = plan(projectedState, originalGoal);

        newPlan.addAll(remaining);
        return newPlan;
    }
}
```

---

## 10. References

### 10.1 Academic Papers

1. **Orkin, J. (2004)** - "Applying Goal-Oriented Action Planning to Games" - *AI Game Programming Wisdom 2*
2. **Higgins, D. (2017)** - "Goal-Oriented Action Planning in Dynamic Environments" - *AIIDE*
3. **Champandard, A. J. (2022)** - "Neuro-Symbolic AI for Game Agents" - *IEEE Transactions on Games*

### 10.2 Game Industry Resources

**GDC Talks:**
- Jeff Orkin: "AI Architecture: GOAP Planning in F.E.A.R." (2006)
- Kevin Maxham: "GOAP for Killzone 2" (2009)
- Brian Schwab: "AI Game Architecture" (2015)

**Open Source Implementations:**
- [ReGoap (C# Unity)](https://github.com/sploreg/goap) - Battle-tested, production-ready
- [CrashKonijn.GOAP](https://crashkonijn.com/goap) - Modern Unity implementation
- [Embabel (Java)](https://segmentfault.com/p/1210000046614005) - OODA integration
- [GOAP for Unity (GitCode)](https://gitcode.net/mirrors/goap/goap) - Multi-threaded

**Articles & Tutorials:**
- [GAMES104 - Advanced AI](https://it.en369.cn/jiaocheng/1754692047a2775114.html) - 2024 course notes
- [GOAP技术要点 (CSDN)](https://www.cnblogs.com/FlyingZiming/articles/17274602.html) - Chinese deep dive
- [游戏AI实现-GOAP](https://m.blog.csdn.net/weixin_50702814/article/details/144515041) - Implementation guide

### 10.3 Online Resources

**Documentation:**
- [CrashKonijn GOAP Docs](https://crashkonijn.com/goap/docs)
- [Unity GOAP Tutorial](https://developer.unity.cn/projects/659013b0edbc2a97d81632eb)

**Community:**
- [Game AI Pro](https://www.gameaipro.com) - Article series
- [AI Game Programming Wisdom](https://www.crcpress.com/AI-Game-Programming-Wisdom-Series/book-series/IGPWS)

**Video Courses:**
- [Udemy: Goal-Oriented Action Planning](https://www.udemy.com/course/ai_with_goap/) - Penny de Byl
- [Bilibili: GOAP Course](https://m.bilibili.com/video/BV1AF4m1u74s) - Chinese language

### 10.4 Related Technologies

- **PDDL** - Planning Domain Definition Language (standard)
- **STRIPS** - Classical planning (GOAP inspiration)
- **Utility AI** - Alternative decision system
- **Behavior Trees** - Reactive alternative

---

## Appendix: Quick Reference

### GOAP Planning Checklist

- [ ] Define world state properties
- [ ] Create action library (preconditions + effects)
- [ ] Define goals with target states
- [ ] Implement A* planner (backward search)
- [ ] Add action cost functions
- [ ] Implement replanning triggers
- [ ] Create action executor
- [ ] Add goal priority system
- [ ] Integrate with game loop
- [ ] Test and tune

### Action Template

```java
public class MyAction extends BaseGoapAction {
    public MyAction() {
        super("MyAction", 5);  // name, base cost

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
        super("MyGoal", 10);  // name, base priority
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
**Last Updated:** 2026-03-02
**Related Documents:**
- `GOAP_DEEP_DIVE.md` - Comprehensive GOAP guide
- `ARCHITECTURE_D_GOAP.md` - GOAP architecture patterns
- `PRE_LLM_GAME_AUTOMATION.md` - Historical context

**Next Steps:**
1. Implement GOAP planner in `src/main/java/com/minewright/goap/`
2. Create Minecraft-specific action library
3. Integrate with existing HTN planner (hybrid approach)
4. Add LLM goal generation
5. Benchmark against HTN-only approach
