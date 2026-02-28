# Baritone & Mineflayer Architecture Analysis
## Research Report for "One Abstraction Away" System

**Date:** 2026-02-28
**Project:** Steve AI - "Cursor for Minecraft"
**Objective:** Learn from established Minecraft automation systems to design LLM-refinable behaviors

---

## Executive Summary

This research analyzes **Baritone** (Java pathfinding bot) and **Mineflayer** (JavaScript bot framework) to extract architectural patterns applicable to Steve AI's "One Abstraction Away" system. Key findings:

- **Baritone** excels at goal-oriented pathfinding with composite goals, efficient A* optimization, and chunk caching
- **Mineflayer** provides a flexible plugin architecture with behavior trees and state machines
- **Voyager** demonstrates successful LLM integration with Mineflayer for task refinement
- Both systems emphasize interruptibility, state persistence, and modular behavior composition

---

## Table of Contents

1. [Baritone Architecture Deep Dive](#1-baritone-architecture-deep-dive)
2. [Mineflayer Architecture Deep Dive](#2-mineflayer-architecture-deep-dive)
3. [LLM Integration Patterns (Voyager)](#3-llm-integration-patterns-voyager)
4. [Lessons for MineWright/Steve AI](#4-lessons-for-minewrightsteve-ai)
5. [Code Patterns to Steal](#5-code-patterns-to-steal)
6. [Recommended Architecture](#6-recommended-architecture)

---

## 1. Baritone Architecture Deep Dive

### 1.1 Core Pathfinding System

**Algorithm:** Modified A* with 30x performance improvement over MineBot

#### Key Components

```
baritone/src/main/java/baritone/
├── pathing/
│   ├── calc/
│   │   ├── PathNode.java           # Node representation with costs
│   │   ├── AStarPathFinder.java    # Modified A* implementation
│   │   └── openset/
│   │       └── BinaryHeapOpenSet.java  # Priority queue (10x faster)
│   ├── goals/
│   │   ├── Goal.java               # Goal interface
│   │   ├── GoalBlock.java          # Specific block position
│   │   ├── GoalXZ.java             # X/Z coordinates (long-distance)
│   │   ├── GoalYLevel.java         # Target Y level
│   │   └── GoalComposite.java      # Multi-target goals
│   ├── movement/
│   │   ├── Movement.java           # Movement action interface
│   │   ├── MovementHelper.java     # Movement cost calculations
│   │   └── ActionCosts.java        # Cost constants
│   └── path/
│       ├── SplicedPath.java        # Dynamic path concatenation
│       └── PathExecutor.java       # Path execution with cancellation
├── behavior/
│   ├── IBehavior.java              # Behavior interface
│   ├── PathingBehavior.java        # Pathfinding behavior
│   └── LookBehavior.java           # Camera control
├── process/
│   ├── IProcess.java               # Process interface (cancellable)
│   └── CustomGoalProcess.java      # Goal setting process
└── cache/
    └── CachedWorld.java            # 2-bit chunk caching
```

### 1.2 Goal System Design

#### Goal Interface

```java
public interface Goal {
    // Check if position satisfies goal
    boolean isInGoal(int x, int y, int z);

    // Heuristic cost estimate (for A*)
    double heuristic(int x, int y, int z);
}
```

#### Concrete Goal Implementations

**GoalBlock** - Target exact position:
```java
public class GoalBlock implements Goal {
    public final int x, y, z;

    @Override
    public boolean isInGoal(int x, int y, int z) {
        return x == this.x && y == this.y && z == this.z;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        int dx = x - this.x;
        int dy = y - this.y;
        int dz = z - this.z;
        // Weight Y-axis more (vertical movement is harder)
        return GoalYLevel.calculate(0, dy) + GoalXZ.calculate(dx, dz);
    }
}
```

**GoalComposite** - Multiple acceptable targets:
```java
public class GoalComposite implements Goal {
    private final List<Goal> goals;

    public GoalComposite(Goal... goals) {
        this.goals = Arrays.asList(goals);
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        return goals.stream().anyMatch(g -> g.isInGoal(x, y, z));
    }

    @Override
    public double heuristic(int x, int y, int z) {
        // Use minimum distance to any goal
        return goals.stream()
            .mapToDouble(g -> g.heuristic(x, y, z))
            .min()
            .orElse(Double.MAX_VALUE);
    }
}
```

### 1.3 Movement Costs & Action System

#### ActionCosts Constants

```java
public class ActionCosts {
    public static final double WALK_ONE_BLOCK_COST = 1.0;
    public static final double WALK_ONE_IN_WATER_COST = 2.5;
    public static final double JUMP_ONE_BLOCK_COST = 2.0;
    public static final double PLACE_ONE_BLOCK_COST = 1.5;
    public static final double BREAK_ONE_BLOCK_COST = varyByTool();
    public static final double FALL_ONE_BLOCK_COST = 1.5;
    public static final double COST_INF = Double.MAX_VALUE;
}
```

#### Movement Interface

```java
public interface Movement {
    // Calculate cost of this movement
    double getCost(CalculationContext context);

    // Execute this movement
    void execute(PathingBehavior behavior);

    // Check if movement is valid
    boolean isValid(CalculationContext context);

    // Get resulting position
    BetterBlockPos getDest();
}
```

### 1.4 Path Caching & Optimization

#### 2-Bit Chunk Representation

```java
public class CachedWorld {
    // Compress blocks to 2 bits: 00=AIR, 01=SOLID, 10=WATER, 11=AVOID
    private final Long2ObjectOpenHashMap<ChunkData> cachedChunks;

    public boolean isCached(int x, int z) {
        return cachedChunks.containsKey(ChunkPos.toLong(x, z));
    }

    public BlockState getBlock(int x, int y, int z) {
        ChunkData chunk = cachedChunks.get(ChunkPos.toLong(x >> 4, z >> 4));
        if (chunk == null) return BlockState.UNKNOWN;
        return chunk.get(x & 15, y, z & 15);
    }
}
```

#### SplicedPath for Memory Efficiency

```java
public class SplicedPath implements IPath {
    private final List<IPath> segments;

    public SplicedPath(List<IPath> segments) {
        this.segments = segments;
    }

    // Only load needed segments into memory
    @Override
    public Movement get(int index) {
        int segmentIndex = findSegment(index);
        int localIndex = index - segmentOffsets[segmentIndex];
        return segments.get(segmentIndex).get(localIndex);
    }
}
```

### 1.5 Interruption & Cancellation

#### IProcess Interface

```java
public interface IProcess {
    boolean cancel();
    boolean isCancelled();
    void onLostControl(CancelEvent event);
}
```

#### Path Cancellation Detection

```java
public class PathExecutor {
    public void tick() {
        if (cancelled) return;

        // Check if path is still valid
        double currentCost = recalculateCost(nextMovement);
        if (currentCost >= ActionCosts.COST_INF && canCancel) {
            cancel("Movement cost infinite");
            return;
        }

        // Check if stuck
        if (positionTicks > stuckThreshold) {
            cancel("Stuck for too long");
            return;
        }

        executeNextMovement();
    }

    public boolean cancel(String reason) {
        cancelled = true;
        onFailure(reason);
        return true;
    }
}
```

### 1.6 Process-Based Behaviors

**Farm Process** - Automated farming:
```java
public class FarmProcess implements IProcess {
    private final List<Task> taskQueue = new ArrayList<>();

    public void farm(int range, BlockPos origin) {
        // Scan for mature crops
        List<BlockPos> crops = scanForMatureCrops(origin, range);

        // Prioritize tasks
        for (BlockPos crop : crops) {
            taskQueue.add(new HarvestTask(crop));
            taskQueue.add(new ReplantTask(crop));
        }
    }

    @Override
    public void tick() {
        if (taskQueue.isEmpty()) {
            onComplete();
            return;
        }

        Task current = taskQueue.get(0);
        if (current.isComplete()) {
            taskQueue.remove(0);
            return;
        }

        current.tick();
    }

    @Override
    public boolean cancel() {
        taskQueue.clear();
        return true;
    }
}
```

---

## 2. Mineflayer Architecture Deep Dive

### 2.1 Core Architecture

**Stack:** Node.js + Minecraft protocol layer + Plugin system

```
mineflayer/
├── lib/
│   ├── plugins/                    # Core plugins
│   │   ├── bed.js                 # Bed interaction
│   │   ├── block_actions.js       # Block break/place
│   │   ├── chest.js               # Chest management
│   │   ├── inventory.js           # Inventory operations
│   │   └── physics.js             # Physics simulation
│   └── bot.js                     # Main bot class
└── examples/
```

### 2.2 Plugin System

```javascript
const mineflayer = require('mineflayer')

const bot = mineflayer.createBot({
  host: 'localhost',
  username: 'SteveBot'
})

// Load plugins
bot.loadPlugin(require('mineflayer-pathfinder').pathfinder)
bot.loadPlugin(require('mineflayer-collectblock').plugin)
bot.loadPlugin(require('mineflayer-tool').tool)
```

#### Plugin Pattern

```javascript
function myBotPlugin(bot, options) {
  // Extend bot with new functionality
  bot.myFeature = {
    doSomething: () => {
      // Implementation
    }
  }

  // Listen to events
  bot.on('chat', (username, message) => {
    if (message === '!doSomething') {
      bot.myFeature.doSomething()
    }
  })
}

module.exports = myBotPlugin
```

### 2.3 Pathfinder Plugin

**Inspired by Baritone's goal system**

```javascript
const { pathfinder, Movements, goals } = require('mineflayer-pathfinder')

bot.loadPlugin(pathfinder)

bot.once('spawn', () => {
  // Configure movement costs
  const defaultMove = new Movements(bot)
  defaultMove.canDig = true
  defaultMove.digCost = 1
  defaultMove.maxDropDown = 4
  defaultMove.allowParkour = true
  bot.pathfinder.setMovements(defaultMove)

  // Set navigation goal
  const goal = new goals.GoalBlock(100, 64, -200)
  bot.pathfinder.setGoal(goal)
})
```

#### Goal Types

```javascript
// Specific block position
new goals.GoalBlock(x, y, z)

// X/Z coordinates (long distance)
new goals.GoalXZ(x, z)

// Near a position (within radius)
new goals.GoalNear(x, y, z, range)

// Y level (any X/Z)
new goals.GoalYLevel(y)

// Composite goals (any satisfies)
new goals.GoalCompositeAny(
  new goals.GoalBlock(x1, y1, z1),
  new goals.GoalBlock(x2, y2, z2)
)

// Composite goals (all must satisfy in sequence)
new goals.GoalCompositeAll(
  new goals.GoalBlock(x1, y1, z1),
  new goals.GoalBlock(x2, y2, z2)
)
```

### 2.4 Movements Configuration

```javascript
const { Movements } = require('mineflayer-pathfinder')

const movements = new Movements(bot, mcData)

// Movement options
movements.canDig = true              // Allow breaking blocks
movements.digCost = 1                // Cost multiplier for breaking
movements.placeCost = 1              // Cost multiplier for placing
movements.maxDropDown = 4            // Max fall distance
movements.allow1by1towers = true     // Allow pillaring up
movements.allowFreeMotion = false    // Straight line if clear
movements.dontCreateFlow = true      // Don't break liquid-adjacent
movements.allowParkour = false       // Allow jump parkour
movements.allowSprint = true         // Sprint when possible

// Per-block costs
movements.blocksCostAvoid['minecraft:lava'] = Infinity
movements.blocksCostAvoid['minecraft:fire'] = Infinity
```

### 2.5 State Machine Plugin

**mineflayer-statemachine** for complex AI behaviors:

```javascript
const { StateMachine, State, Transition } = require('mineflayer-statemachine')

// Define states
const idleState = new State(() => {
  bot.chat('Idling...')
})

const moveToTargetState = new State(() => {
  const target = bot.target
  if (target) {
    bot.pathfinder.setGoal(new goals.GoalNear(target.position))
  }
})

// Define transitions
const transitionToMove = new Transition(() => {
  return bot.target !== null
}, moveToTargetState)

const transitionToIdle = new Transition(() => {
  return !bot.pathfinder.isMoving() && !bot.target
}, idleState)

// Build state machine
const behavior = new StateMachine(idleState)
idleState.addTransition(transitionToMove)
moveToTargetState.addTransition(transitionToIdle)

// Execute
bot.on('physicTick', () => {
  behavior.tick()
})
```

### 2.6 Behavior Modules

**Pre-built behaviors from mineflayer-statemachine:**

```javascript
// behaviorMoveTo - Navigate to position
const moveToBehavior = new BehaviorMoveTo(bot, targetPos)

// behaviorMineBlock - Break specific blocks
const mineBehavior = new BehaviorMineBlock(bot, 'diamond_ore')

// behaviorFollowEntity - Follow another entity
const followBehavior = new BehaviorFollowEntity(bot, targetEntity)

// behaviorPlaceBlock - Place blocks
const placeBehavior = new BehaviorPlaceBlock(bot, blockType, pos)

// behaviorIdle - Do nothing
const idleBehavior = new BehaviorIdle()
```

#### Composite Behaviors

```javascript
// Sequential execution
const sequence = new BehaviorSequence(bot, [
  new BehaviorMoveTo(bot, chestPos),
  new BehaviorOpenChest(bot, chestPos),
  new BehaviorDeposit(bot, items),
  new BehaviorCloseChest(bot)
])

// Parallel execution
const parallel = new BehaviorParallel(bot, [
  new BehaviorMineBlock(bot, 'iron_ore'),
  new BehaviorFollowEntity(bot, player)
])

// Selector (try until one succeeds)
const selector = new BehaviorSelector(bot, [
  new BehaviorUseSword(bot),
  new BehaviorUseBow(bot),
  new BehaviorFlee(bot)
])
```

### 2.7 Interrupt Handling

```javascript
// Stuck detection
bot.on('physicTick', () => {
  if (bot.pathfinder.isMoving()) {
    bot.stuckCounter++

    if (bot.stuckCounter >= 100) {
      // Bot is stuck
      bot.pathfinder.stop()

      // Recalculate path
      setTimeout(() => {
        bot.pathfinder.setGoal(currentGoal)
      }, 500)

      bot.stuckCounter = 0
    }
  } else {
    bot.stuckCounter = 0
  }
})

// Cancel on event
bot.on('chat', (username, message) => {
  if (message === 'stop') {
    bot.pathfinder.stop()
  }
})
```

### 2.8 Error Handling & Recovery

```javascript
bot.pathfinder.on('goal_reached', (goal) => {
  console.log('Reached goal!')
})

bot.pathfinder.on('path_update', (r) => {
  if (r.status === 'noPath') {
    console.log('Cannot reach goal!')
    // Try alternative or cancel
  }
})

bot.pathfinder.on('path_stop', (reason) => {
  console.log('Path stopped:', reason)
  // Handle cancellation
})
```

---

## 3. LLM Integration Patterns (Voyager)

### 3.1 Voyager Architecture

**Voyager** (Wang et al., 2024) - First LLM-powered lifelong learning agent for Minecraft

```
Voyager Architecture:
┌─────────────────────────────────────────────────────────┐
│                   GPT-4 Language Model                  │
└───────────────────┬─────────────────────────────────────┘
                    │
    ┌───────────────┼───────────────┐
    │               │               │
    ▼               ▼               ▼
┌─────────┐   ┌──────────┐   ┌──────────────┐
│ Auto    │   │ Reusable │   │ Iterative    │
│ Curriculum │ Skill  │   │ Prompting    │
│         │   │ Library  │   │ Mechanism   │
└─────────┘   └──────────┘   └──────────────┘
                    │
                    ▼
            ┌───────────────┐
            │  Mineflayer   │
            │   Bot API     │
            └───────────────┘
                    │
                    ▼
            ┌───────────────┐
            │   Minecraft   │
            │   Game        │
            └───────────────┘
```

### 3.2 Three Core Components

#### 1. Automatic Curriculum

GPT-4 proposes progressively harder tasks based on:
- Current inventory/equipment
- Biome/environment
- Task completion history
- Discovered items/locations

```python
# Curriculum generation prompt
curriculum_prompt = f"""
You are a Minecraft task generator. Generate the next task
for the agent based on current state:

Current State:
- Items: {inventory}
- Location: {biome}
- Completed Tasks: {task_history}
- Skills Available: {skills}

Generate a challenging but achievable task.
Format: TASK: <description>
"""
```

#### 2. Reusable Skill Library

Skills stored as executable JavaScript code:

```javascript
// Skill: craftIronPickaxe
const craftIronPickaxe = {
  name: "craftIronPickaxe",
  code: `
async function craftIronPickaxe(bot) {
  // Check if we have materials
  const iron = bot.inventory.items().find(i => i.name === 'iron_ingot')
  const sticks = bot.inventory.items().find(i => i.name === 'stick')

  if (!iron || iron.count < 3 || !sticks || sticks.count < 2) {
    return false // Cannot craft
  }

  // Find crafting table
  const table = bot.findBlock({
    matching: bot.mcData.blocksByName.crafting_table.id
  })

  if (!table) {
    return await bot.learn('placeCraftingTable')
  }

  // Craft the pickaxe
  await bot.craft(bot.mcData.itemsByName.iron_pickaxe.id, 1)
  return true
}
  `,
  dependencies: ["mineIronOre", "craftSticks"],
  embedding: [...], // Vector embedding for retrieval
  examples: [
    {context: "have iron, need pickaxe", usage: "craftIronPickaxe"},
    {context: "upgrade from stone", usage: "craftIronPickaxe"}
  ]
}
```

#### 3. Iterative Prompting Mechanism

Self-correction using three feedback types:

```javascript
// Execution with refinement
async function executeWithRefinement(code, maxAttempts = 3) {
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    try {
      // Try to execute
      const result = await eval(code)

      // Verify success
      if (await verifyTaskComplete(result)) {
        // Success - add to skill library
        addToSkillLibrary(code)
        return result
      }
    } catch (error) {
      // Get feedback from GPT-4
      const feedback = await getFeedback(code, error, gamestate)
      code = feedback.refinedCode
    }
  }
  throw new Error('Failed after max attempts')
}

// Feedback prompt
feedback_prompt = f"""
Your code failed to complete the task.

Code:
{code}

Error:
{error}

Game State:
{gamestate}

Task:
{task}

Refine the code to handle this error. Return only the
improved JavaScript function.
"""
```

### 3.3 Task Decomposition Pattern

Voyager breaks high-level tasks into subtasks:

```
High-Level Task: "Build a house"
├── Subtask 1: "Gather wood"
│   └── Uses skill: chopTree
├── Subtask 2: "Craft planks"
│   └── Uses skill: craftPlanks
├── Subtask 3: "Place foundation"
│   └── Uses skill: placeBlocks
├── Subtask 4: "Build walls"
│   └── Uses skill: buildWall
└── Subtask 5: "Add roof"
    └── Uses skill: buildRoof
```

### 3.4 Skill Retrieval

When new tasks arrive, relevant skills are retrieved:

```javascript
// Find relevant skills using semantic search
async function findRelevantSkills(task) {
  const taskEmbedding = await embed(task)

  const similarities = skillLibrary.map(skill => ({
    skill,
    similarity: cosineSimilarity(taskEmbedding, skill.embedding)
  }))

  return similarities
    .sort((a, b) => b.similarity - a.similarity)
    .slice(0, 5) // Top 5
    .map(s => s.skill)
}
```

---

## 4. Lessons for MineWright/Steve AI

### 4.1 What to Adopt from Baritone

| Feature | Why It's Useful | How to Apply |
|---------|----------------|--------------|
| **Goal System** | Clean abstraction for targets | Implement Goal interface with composite goals |
| **Composite Goals** | Handle multiple acceptable outcomes | GoalCompositeAny/All for flexible planning |
| **Movement Costs** | Explicit action difficulty modeling | ActionCosts constants for different block types |
| **Chunk Caching** | Performance for long-distance paths | Cache world state in compressed format |
| **SplicedPath** | Memory efficiency for long paths | Segment paths and load on-demand |
| **Process Interface** | Standardized cancellation | IProcess with cancel(), isCancelled() |
| **Stuck Detection** | Recovery from failures | Track position ticks, recalculate when stuck |

### 4.2 What to Adopt from Mineflayer

| Feature | Why It's Useful | How to Apply |
|---------|----------------|--------------|
| **Plugin System** | Extensible architecture | Plugin loading with dependency injection |
| **Movements Config** | Tunable behavior per situation | Movement profiles (mining, building, combat) |
| **State Machine** | Complex behavior orchestration | State machines for agent states |
| **Behavior Modules** | Reusable action patterns | Composable behavior classes |
| **Interrupt Handling** | Responsive to events | Event-driven cancellation |
| **Error Recovery** | Robustness in dynamic worlds | Retry with recalculated paths |

### 4.3 What to Do Differently

| Aspect | Baritone/Mineflayer | Our Approach |
|--------|-------------------|--------------|
| **Planning** | Manual commands | LLM-generated task plans |
| **Behaviors** | Hardcoded skills | LLM-refinable skills |
| **Context** | Static world state | Dynamic memory with conversation history |
| **Adaptation** | Fixed strategies | Self-improving via feedback |
| **Coordination** | Single agent | Multi-agent collaboration |
| **Natural Language** | Chat commands | Full natural language understanding |

### 4.4 LLM Integration Strategy

**Key Insight:** Voyager demonstrates that LLMs can successfully:
- Generate executable code for Minecraft tasks
- Refine behaviors based on feedback
- Compose skills in novel ways
- Learn from experience without weight updates

**Our Advantage:**
- Java-based (better performance than JS)
- Multi-agent coordination (Voyager is single-agent)
- Built-in memory system for context
- Direct mod integration (deeper game access)

---

## 5. Code Patterns to Steal

### 5.1 Goal System for Steve AI

**Interface Definition:**

```java
package com.steve.ai.goals;

/**
 * A goal represents a target state the Steve agent wants to achieve.
 * Goals are used by the task planner to generate action sequences.
 */
public interface Goal {
    /**
     * Check if the given position satisfies this goal.
     */
    boolean isSatisfied(BlockPos pos, ServerLevel level);

    /**
     * Estimate the heuristic cost to reach this goal from the given position.
     * Used for pathfinding and task prioritization.
     */
    double heuristic(BlockPos from);

    /**
     * Get a human-readable description of this goal.
     */
    String getDescription();

    /**
     * Check if this goal is still achievable given current world state.
     */
    default boolean isAchievable(ServerLevel level) {
        return true;
    }
}
```

**Concrete Implementations:**

```java
package com.steve.ai.goals;

/**
 * Reach a specific block position.
 */
public class BlockGoal implements Goal {
    private final BlockPos target;
    private final double tolerance;

    public BlockGoal(BlockPos target, double tolerance) {
        this.target = target;
        this.tolerance = tolerance;
    }

    @Override
    public boolean isSatisfied(BlockPos pos, ServerLevel level) {
        return pos.distSqr(target) <= tolerance * tolerance;
    }

    @Override
    public double heuristic(BlockPos from) {
        // Weight Y-axis movement more heavily
        int dx = target.getX() - from.getX();
        int dy = Math.abs(target.getY() - from.getY());
        int dz = target.getZ() - from.getZ();

        return Math.sqrt(dx * dx + dz * dz) + (dy * 2.0);
    }

    @Override
    public String getDescription() {
        return "reach " + target;
    }
}

/**
 * Find and mine a specific block type.
 */
public class MineGoal implements Goal {
    private final Block block;
    private final int count;
    private final BlockPos center;
    private final int range;

    public MineGoal(Block block, int count, BlockPos center, int range) {
        this.block = block;
        this.count = count;
        this.center = center;
        this.range = range;
    }

    @Override
    public boolean isSatisfied(BlockPos pos, ServerLevel level) {
        // Check if we have enough in inventory
        // This requires access to Steve's inventory
        return false; // TODO
    }

    @Override
    public double heuristic(BlockPos from) {
        // Find nearest ore
        BlockPos nearest = findNearestBlock(center, range);
        if (nearest == null) return Double.MAX_VALUE;
        return from.distSqr(nearest);
    }

    private BlockPos findNearestBlock(BlockPos center, int range) {
        // Scan for block in range
        return null; // TODO
    }

    @Override
    public String getDescription() {
        return "mine " + count + " " + block.getDescriptionId();
    }
}

/**
 * Composite goal that is satisfied when any sub-goal is satisfied.
 */
public class AnyOfGoal implements Goal {
    private final List<Goal> goals;

    public AnyOfGoal(Goal... goals) {
        this.goals = List.of(goals);
    }

    @Override
    public boolean isSatisfied(BlockPos pos, ServerLevel level) {
        return goals.stream().anyMatch(g -> g.isSatisfied(pos, level));
    }

    @Override
    public double heuristic(BlockPos from) {
        return goals.stream()
            .mapToDouble(g -> g.heuristic(from))
            .min()
            .orElse(Double.MAX_VALUE);
    }

    @Override
    public String getDescription() {
        return "any of: " + goals.stream()
            .map(Goal::getDescription)
            .collect(Collectors.joining(", "));
    }
}
```

### 5.2 Movement Cost System

```java
package com.steve.ai.pathing;

/**
 * Cost constants for different movement types.
 * Based on Baritone's ActionCosts system.
 */
public final class MovementCosts {
    // Basic movements
    public static final double WALK = 1.0;
    public static final double WALK_IN_WATER = 2.5;
    public static final double JUMP = 2.0;
    public static final double FALL = 1.5;

    // Block interactions
    public static final double BREAK_BLOCK = 10.0; // Will be tool-dependent
    public static final double PLACE_BLOCK = 1.5;
    public static final double PILLAR_UP = 3.0;

    // Special movements
    public static final double PARKOUR = 5.0;
    public static final double LADDER = 0.5;
    public static final double VINE = 0.7;

    // Avoidance
    public static final double AVOID = Double.MAX_VALUE;
    public static final double INF = Double.MAX_VALUE;

    /**
     * Calculate break cost based on tool and block.
     */
    public static double getBreakCost(BlockState block, ItemStack tool) {
        // Hardness multiplier
        double hardness = block.getDestroySpeed(null, null);

        // Tool multiplier (better tools = faster)
        double toolMultiplier = 1.0;
        if (!tool.isEmpty()) {
            toolMultiplier = getToolSpeed(tool, block);
        }

        return BREAK_BLOCK * hardness / toolMultiplier;
    }

    private static double getToolSpeed(ItemStack tool, BlockState block) {
        // Simplified tool speed calculation
        float speed = tool.getDestroySpeed(block);
        return Math.max(speed, 1.0f);
    }
}
```

### 5.3 Interruptible Process System

```java
package com.steve.ai.process;

/**
 * A cancellable process that can be executed tick-by-tick.
 * Inspired by Baritone's IProcess interface.
 */
public interface IProcess {
    /**
     * Called each game tick. Implementations should do small amounts
     * of work and return immediately.
     *
     * @return true if the process is complete
     */
    boolean tick();

    /**
     * Cancel this process. Called when the process needs to stop
     * prematurely (e.g., new command, unsafe condition).
     *
     * @return true if cancellation was successful
     */
    boolean cancel();

    /**
     * Check if this process has been cancelled.
     */
    boolean isCancelled();

    /**
     * Called when the process completes successfully.
     */
    default void onComplete() {}

    /**
     * Called when the process is cancelled.
     */
    default void onCancel(String reason) {}

    /**
     * Called when the process encounters an error.
     */
    default void onError(Throwable error) {}
}
```

**Base Implementation:**

```java
package com.steve.ai.process;

/**
 * Base class for processes with common cancellation logic.
 */
public abstract class BaseProcess implements IProcess {
    protected volatile boolean cancelled = false;
    protected volatile boolean complete = false;

    @Override
    public boolean tick() {
        if (cancelled || complete) {
            return true;
        }

        try {
            complete = executeTick();
            if (complete) {
                onComplete();
            }
        } catch (Throwable t) {
            cancel();
            onError(t);
        }

        return complete;
    }

    /**
     * Subclasses implement this with their tick logic.
     */
    protected abstract boolean executeTick();

    @Override
    public boolean cancel() {
        if (complete) return false;
        cancelled = true;
        onCancel("Process cancelled");
        return true;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
```

### 5.4 Task Queue with Priorities

```java
package com.steve.ai.execution;

/**
 * A prioritized task queue that supports interruption and re-planning.
 */
public class TaskQueue {
    private final PriorityQueue<Task> queue;
    private Task currentTask;
    private final Object lock = new Object();

    public TaskQueue() {
        this.queue = new PriorityQueue<>(Comparator
            .comparingInt(Task::getPriority)
            .reversed()
            .thenComparingLong(Task::getCreationTime));
    }

    /**
     * Add a task to the queue.
     */
    public void enqueue(Task task) {
        synchronized (lock) {
            queue.add(task);

            // If this is high priority, interrupt current task
            if (task.getPriority() > currentPriority()) {
                interruptCurrent("Higher priority task queued");
            }
        }
    }

    /**
     * Get the next task to execute.
     */
    public Task getNext() {
        synchronized (lock) {
            if (currentTask != null && !currentTask.isComplete()) {
                return currentTask;
            }

            currentTask = queue.poll();
            return currentTask;
        }
    }

    /**
     * Interrupt the currently executing task.
     */
    public void interruptCurrent(String reason) {
        synchronized (lock) {
            if (currentTask != null && !currentTask.isComplete()) {
                Task interrupted = currentTask;
                currentTask = null;

                // Save state for potential resume
                interrupted.suspend();

                // Re-queue if it's resumable
                if (interrupted.isResumable()) {
                    queue.add(interrupted);
                }
            }
        }
    }

    /**
     * Clear all pending tasks.
     */
    public void clear() {
        synchronized (lock) {
            interruptCurrent("Queue cleared");
            queue.clear();
        }
    }

    private int currentPriority() {
        return currentTask != null ? currentTask.getPriority() : Integer.MIN_VALUE;
    }
}
```

### 5.5 Stuck Detection & Recovery

```java
package com.steve.ai.execution;

/**
 * Detects when the agent is stuck and triggers recovery.
 */
public class StuckDetector {
    private static final int STUCK_THRESHOLD = 100; // ticks
    private static final double POSITION_EPSILON = 0.1;

    private final SteveSteve steve;
    private int stuckTicks = 0;
    private Vec3 lastPosition;

    public StuckDetector(SteveSteve steve) {
        this.steve = steve;
    }

    /**
     * Check if the agent is stuck. Should be called each tick.
     */
    public boolean check() {
        Vec3 currentPos = steve.position();

        if (lastPosition != null) {
            double distance = currentPos.distanceTo(lastPosition);

            if (distance < POSITION_EPSILON) {
                stuckTicks++;

                if (stuckTicks >= STUCK_THRESHOLD) {
                    onStuckDetected();
                    return true;
                }
            } else {
                // Moving normally
                stuckTicks = 0;
            }
        }

        lastPosition = currentPos;
        return false;
    }

    /**
     * Called when the agent is detected as stuck.
     */
    private void onStuckDetected() {
        // Try to recover
        RecoveryStrategy strategy = determineRecoveryStrategy();
        strategy.execute(steve);
        stuckTicks = 0;
    }

    private RecoveryStrategy determineRecoveryStrategy() {
        // Analyze situation and choose recovery
        BlockPos pos = steve.blockPosition();
        BlockState below = steve.level().getBlockState(pos.below());

        // If in water, try to swim up
        if (steve.isInWater()) {
            return new SwimUpStrategy();
        }

        // If stuck on block, try to jump
        if (below.isAir() || below.is(Blocks.WATER)) {
            return new JumpStrategy();
        }

        // Default: break obstacle
        return new BreakObstacleStrategy();
    }

    interface RecoveryStrategy {
        void execute(SteveSteve steve);
    }

    record JumpStrategy() implements RecoveryStrategy {
        @Override
        public void execute(SteveSteve steve) {
            steve.setJumping(true);
        }
    }

    record SwimUpStrategy() implements RecoveryStrategy {
        @Override
        public void execute(SteveSteve steve) {
            steve.setDeltaMovement(steve.getDeltaMovement().add(0, 0.1, 0));
        }
    }

    record BreakObstacleStrategy() implements RecoveryStrategy {
        @Override
        public void execute(SteveSteve steve) {
            // Try to break blocking blocks
            // This would integrate with the mining action
        }
    }
}
```

### 5.6 Path Splicing for Efficiency

```java
package com.steve.ai.pathing;

/**
 * Combines multiple path segments for memory efficiency.
 * Long paths are split into segments and loaded on-demand.
 */
public class SplicedPath {
    private final List<PathSegment> segments;
    private final int totalLength;

    public SplicedPath(List<Path> paths) {
        this.segments = new ArrayList<>();

        int offset = 0;
        for (Path path : paths) {
            segments.add(new PathSegment(path, offset));
            offset += path.length();
        }

        this.totalLength = offset;
    }

    /**
     * Get the movement at the given index.
     */
    public Movement get(int index) {
        // Find the segment containing this index
        for (PathSegment segment : segments) {
            if (segment.contains(index)) {
                return segment.path.get(index - segment.offset);
            }
        }
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    /**
     * Get total path length.
     */
    public int length() {
        return totalLength;
    }

    private static record PathSegment(Path path, int offset) {
        boolean contains(int index) {
            return index >= offset && index < offset + path.length();
        }
    }
}
```

---

## 6. Recommended Architecture

### 6.1 Hybrid Approach

**Combine the best of both systems:**

```
Steve AI "One Abstraction Away" Architecture:

                    User Natural Language
                            │
                            ▼
┌──────────────────────────────────────────────────────────┐
│                    Task Planner (LLM)                     │
│  - Decomposes natural language into goals                │
│  - Generates action sequences                            │
│  - Refines plans based on feedback                       │
└───────────────┬──────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────┐
│                  Goal System (Baritone-style)             │
│  - GoalBlock, GoalXZ, GoalMine, GoalComposite            │
│  - Heuristic evaluation for planning                     │
└───────────────┬──────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────┐
│               Behavior Orchestrator (Mineflayer-style)    │
│  - State machine for agent states                        │
│  - Behavior tree for action selection                    │
│  - Composable behaviors                                  │
└───────────────┬──────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────┐
│                   Action Executor                         │
│  - Tick-based execution (non-blocking)                   │
│  - Interruptible processes                               │
│  - Stuck detection & recovery                            │
└───────────────┬──────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────┐
│                   Pathfinder (Baritone-inspired)          │
│  - A* pathfinding with movement costs                    │
│  - Chunk caching for performance                         │
│  - Spliced paths for memory efficiency                   │
└───────────────┬──────────────────────────────────────────┘
                │
                ▼
              Minecraft World
```

### 6.2 Integration Points

#### LLM to Goal System

```java
// LLM parses natural language and creates goals
public class LLMGoalParser {
    private final OpenAIClient llm;

    public List<Goal> parseGoals(String naturalLanguage, WorldContext context) {
        String prompt = buildPrompt(naturalLanguage, context);
        String response = llm.complete(prompt);

        // Parse LLM response into Goal objects
        return parseGoalsFromJson(response);
    }

    private String buildPrompt(String request, WorldContext context) {
        return String.format("""
            You are a Minecraft task planner. Convert this request
            into structured goals.

            Request: %s

            Current Context:
            - Position: %s
            - Inventory: %s
            - Nearby blocks: %s

            Respond with JSON array of goals:
            [
              {
                "type": "block" | "mine" | "composite",
                "params": {...}
              }
            ]
            """, request, context.position(), context.inventory(),
            context.nearbyBlocks());
    }
}
```

#### Goal to Action System

```java
// Goals are decomposed into actions using existing action system
public class GoalDecomposer {
    private final ActionRegistry registry;

    public List<Action> decompose(Goal goal, SteveSteve steve) {
        List<Action> actions = new ArrayList<>();

        if (goal instanceof BlockGoal) {
            BlockGoal bg = (BlockGoal) goal;
            Path path = pathfindTo(steve.blockPosition(), bg.getTarget());
            actions.addAll(createMovementActions(path));
        } else if (goal instanceof MineGoal) {
            MineGoal mg = (MineGoal) goal;
            actions.add(new FindBlockAction(mg.getBlock(), mg.getRange()));
            actions.add(new MineAction(mg.getBlock(), mg.getCount()));
        } else if (goal instanceof CompositeGoal) {
            // Decompose each sub-goal
            for (Goal sub : ((CompositeGoal) goal).getGoals()) {
                actions.addAll(decompose(sub, steve));
            }
        }

        return actions;
    }
}
```

#### Behavior Refinement Loop

```java
// Actions can be refined by LLM based on feedback
public class BehaviorRefiner {
    private final OpenAIClient llm;
    private final SteveMemory memory;

    public Action refineAction(Action failedAction, ExecutionFeedback feedback) {
        String prompt = String.format("""
            This action failed. Refine it to handle this situation.

            Original Action: %s

            Failure Reason: %s
            Current State: %s

            Respond with improved action specification:
            {
              "type": "...",
              "params": {...},
              "reasoning": "..."
            }
            """,
            failedAction.getDescription(),
            feedback.getFailureReason(),
            feedback.getCurrentState()
        );

        String response = llm.complete(prompt);
        return parseActionFromJson(response);
    }
}
```

### 6.3 Multi-Agent Coordination

```java
// Coordinate multiple Steves using composite goals and task queues
public class MultiAgentCoordinator {
    private final List<SteveSteve> agents;
    private final Map<SteveSteve, TaskQueue> taskQueues;

    public void executeGoal(Goal goal) {
        // Decompose goal into sub-goals
        List<Goal> subGoals = decomposeGoal(goal);

        // Assign sub-goals to agents based on capability/location
        for (Goal subGoal : subGoals) {
            SteveSteve bestAgent = selectBestAgent(subGoal);
            taskQueues.get(bestAgent).enqueue(createTask(subGoal));
        }
    }

    private SteveSteve selectBestAgent(Goal goal) {
        // Choose agent based on:
        // - Distance to goal
        // - Current workload
        // - Required capabilities (tools, etc.)
        return agents.stream()
            .min(Comparator.comparingDouble(a ->
                goal.heuristic(a.blockPosition())))
            .orElse(agents.get(0));
    }
}
```

### 6.4 Memory & Context Integration

```java
// Rich context for LLM planning
public class WorldContext {
    private final SteveSteve steve;
    private final SteveMemory memory;

    public String getContextForLLM() {
        return String.format("""
            Current Agent State:
            - Name: %s
            - Position: %s
            - Health: %.1f/%.1f
            - Hunger: %d/20
            - Inventory: %s

            Recent Actions:
            %s

            Nearby Blocks:
            %s

            Nearby Entities:
            %s

            Known Locations:
            %s

            Conversation Summary:
            %s
            """,
            steve.getName().getString(),
            steve.blockPosition(),
            steve.getHealth(), steve.getMaxHealth(),
            steve.getFoodData().getFoodLevel(),
            formatInventory(),
            formatRecentActions(),
            formatNearbyBlocks(),
            formatNearbyEntities(),
            formatKnownLocations(),
            memory.getConversationSummary()
        );
    }

    private String formatInventory() {
        return steve.getInventory().items.stream()
            .map(item -> String.format("%sx %s",
                item.getCount(),
                item.getDescriptionId()))
            .collect(Collectors.joining(", "));
    }
}
```

### 6.5 Example Flow

```
User: "Steve, build me a house at 100, 64, -200"
        │
        ▼
Task Planner receives request
        │
        ▼
LLM decomposes into goals:
  - Goal: goto(100, 64, -200)
  - Goal: gather(wood, 64)
  - Goal: craft(planks, 256)
  - Goal: build(house, 5x5x4)
        │
        ▼
Each goal is planned:
  goto(100, 64, -200) → Pathfind to location
  gather(wood, 64) → Find trees, mine logs
  craft(planks, 256) → Use crafting table
  build(house) → Execute building actions
        │
        ▼
Actions executed tick-by-tick:
  - Movement actions navigate to target
  - Mining actions gather resources
  - Crafting actions convert materials
  - Building actions place blocks
        │
        ▼
Progress reported to LLM:
  - "Moved 500/1000 blocks"
  - "Gathered 32/64 logs"
  - "Built 2/4 walls"
        │
        ▼
LLM can refine plan based on feedback:
  - If stuck: "Try alternative route"
  - If no wood: "Find trees first"
  - If night: "Wait for day or place torches"
        │
        ▼
Completion and summary added to memory
```

---

## 7. Implementation Roadmap

### Phase 1: Core Goal System (Week 1-2)
- [ ] Implement Goal interface and basic implementations
- [ ] Add GoalBlock, GoalXZ, GoalMine, GoalComposite
- [ ] Integrate with existing action system
- [ ] Add goal-based pathfinding integration

### Phase 2: Process & Cancellation (Week 2-3)
- [ ] Implement IProcess interface
- [ ] Add BaseProcess with cancellation support
- [ ] Integrate with ActionExecutor
- [ ] Add stuck detection and recovery

### Phase 3: Pathfinder Improvements (Week 3-4)
- [ ] Add movement cost system
- [ ] Implement chunk caching
- [ ] Add spliced path support
- [ ] Optimize A* with better heuristics

### Phase 4: Behavior System (Week 4-5)
- [ ] Add behavior tree/state machine support
- [ ] Implement composable behaviors
- [ ] Add behavior modules for common tasks
- [ ] Integrate with existing action registry

### Phase 5: LLM Integration (Week 5-6)
- [ ] Add goal parsing from natural language
- [ ] Implement behavior refinement loop
- [ ] Add rich context gathering
- [ ] Integrate with memory system

### Phase 6: Multi-Agent Coordination (Week 6-7)
- [ ] Add task queue with priorities
- [ ] Implement agent selection logic
- [ ] Add collaborative building
- [ ] Test with multiple agents

---

## 8. Key Takeaways

1. **Goals over Actions:** Baritone's goal system provides a clean abstraction that's more LLM-friendly than raw actions. Goals can be composed and reasoned about.

2. **Interruptibility is Key:** Both systems emphasize clean cancellation. Our IProcess interface follows this pattern.

3. **Performance Matters:** Baritone's 30x improvement comes from smart optimizations (binary heap, chunk caching, spliced paths). We should adopt similar patterns.

4. **Composability:** Mineflayer's plugin system and behavior trees enable great flexibility. Our action registry is a good start, but behaviors add composition.

5. **LLM as Planner, not Executor:** Voyager shows LLMs excel at high-level planning and refinement, not low-level control. Keep the LLM at the planning layer.

6. **Feedback Loops:** Iterative refinement with feedback is more effective than perfect one-shot generation. Build in self-correction.

7. **Context is King:** Rich context (world state, memory, conversation) enables better planning. Our memory system is a competitive advantage.

8. **Multi-Agent is Our Edge:** Neither Baritone nor Mineflayer emphasize coordination. Our collaborative building could be unique.

---

## 9. Sources

### Baritone
- [Official Repository](https://github.com/cabaletta/baritone)
- [Baritone Architecture Overview](https://m.blog.csdn.net/gitblog_01087/article/details/157828480)
- [Baritone Source Code Structure](https://m.blog.csdn.net/gitblog_00430/article/details/154051501)
- [Goal System Design](https://blog.csdn.net/gitblog_00148/article/details/154051093)
- [Multi-Target Path Planning](https://m.blog.csdn.net/gitblog_00896/article/details/154051456)
- [Automated Farming](https://m.blog.csdn.net/gitblog_00831/article/details/154051613)
- [Fluid Path Planning](https://m.blog.csdn.net/gitblog_00304/article/details/154050803)

### Mineflayer
- [Official Repository](https://github.com/PrismarineJS/mineflayer)
- [Mineflayer Plugin System](https://blog.csdn.net/gitblog_00813/article/details/151099716)
- [Pathfinder Plugin](https://blog.csdn.net/gitblog_00336/article/details/142247147)
- [State Machine Plugin](https://blog.csdn.net/gitblog_00651/article/details/147169753)
- [Core API Guide](https://blog.csdn.net/gitblog_00366/article/details/151305703)

### Voyager
- [Voyager: An Open-Ended Embodied Agent](https://ar5iv.labs.arxiv.org/html/2305.16291)
- [Voyager Overview (CSDN)](https://blog.csdn.net/shibing624/article/details/157401873)
- [MineLand Multi-Agent Framework](https://arxiv.org/html/2403.19267v1)

### General LLM Agent Research
- [LLM-Planner](https://m.blog.csdn.net/s_m_c/article/details/141650017)
- [Task-Oriented AI Agents](https://m.blog.csdn.net/universsky2015/article/details/146168671)
- [LLM-Modulo Framework](https://hub.baai.ac.cn/p/25ca631b-9161-4147-b2db-b2ef0da5cb66)

---

## Appendix: Quick Reference

### Baritone Commands vs Steve AI

| Baritone Command | Steve AI Equivalent | LLM Interface |
|-----------------|---------------------|---------------|
| `#goto 100 500` | "Navigate to x=100, z=500" | `naveto 100 64 500` |
| `#mine diamond_ore` | "Mine diamonds" | `find and mine diamond ore` |
| `#this` | "Build structure at schematic" | `build a house here` |
| `#follow @p` | "Follow me" | `follow the nearest player` |
| `#farm` | "Farm crops in area" | `harvest and replant crops nearby` |

### Movement Cost Comparison

| Movement | Baritone Cost | Mineflayer Default | Steve AI Proposed |
|----------|--------------|-------------------|-------------------|
| Walk | 1.0 | 1.0 | 1.0 |
| Walk in water | 2.5 | 1.5 | 2.5 |
| Jump | 2.0 | 1.5 | 2.0 |
| Fall 1 block | 1.5 | 1.0 | 1.5 |
| Break block | tool-dependent | configurable | tool-dependent |
| Place block | 1.5 | 1.0 | 1.5 |
| Pillar up | 3.0 | configurable | 3.0 |

### State Comparison

| Aspect | Baritone | Mineflayer | Steve AI |
|--------|----------|------------|----------|
| Implementation | Java | JavaScript | Java |
| Pathfinding | A* (optimized) | A* (pathfinder) | A* (to be enhanced) |
| Goals | Strong system | Strong system | To be implemented |
| Behaviors | Process-based | Plugin + State machine | To be implemented |
| LLM Integration | None | Voyager (research) | Core feature |
| Multi-Agent | No | No | Yes (unique) |
| Memory | None | None | Yes (unique) |

---

**End of Analysis Report**

For questions or clarifications, refer to the original repositories and papers cited in Section 9.
