# RTS Macro/Micro Control Patterns Research
## Applicability to MineWright Minecraft AI Crew

**Date:** 2026-02-28
**Research Focus:** How RTS games separate strategic (macro) from tactical (micro) control, and how these patterns apply to autonomous Minecraft AI agents.

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Macro vs Micro: Definitions](#macro-vs-micro-definitions)
3. [RTS Macro Control Patterns](#rts-macro-control-patterns)
4. [RTS Micro Automation Examples](#rts-micro-automation-examples)
5. [Hierarchical AI Architectures](#hierarchical-ai-architectures)
6. [Applicability to MineWright](#applicability-to-minewright)
7. [Proposed Behavior System](#proposed-behavior-system)
8. [Code Examples](#code-examples)
9. [Implementation Roadmap](#implementation-roadmap)
10. [References](#references)

---

## Executive Summary

Real-Time Strategy (RTS) games like StarCraft and Age of Empires have evolved sophisticated patterns for separating **strategic macro-management** (economy, build orders, tech trees) from **tactical micro-control** (unit positioning, combat maneuvers, ability usage). This separation enables:

- **Scalable command:** One player controls hundreds of units through high-level orders
- **Intelligent autonomy:** Units handle low-level details (pathfinding, resource gathering, combat) automatically
- **Emergent complexity:** Simple macro commands produce sophisticated coordinated behavior

For MineWright's autonomous Minecraft AI crew, these patterns offer a blueprint for scaling from single-agent commands to multi-agent coordination where a "Foreman" issues macro commands that "Worker" agents interpret into micro behaviors.

---

## Macro vs Micro: Definitions

### Macro Management (Strategic)

| Aspect | Description | RTS Examples | MineWright Analog |
|--------|-------------|--------------|-------------------|
| **Scope** | Long-term, economy-wide decisions | Build orders, expansion timing, tech tree progress | Project planning, resource allocation, crew coordination |
| **Time Scale** | Minutes to hours | "Build 3 barracks by 5:00", "Expand to natural" | "Build castle by sunset", "Establish mining outpost" |
| **Player Actions** | Few, high-impact decisions | Select building, place location, set rally point | Type "build a house", "gather 64 cobblestone" |
| **Success Metrics** | Economy rate, tech level, army size | Resources per minute, supply cap, upgrades | Blocks placed, resources gathered, structures completed |
| **Cognitive Load** | Strategic planning, game knowledge | Memorize build orders, understand unit counters | Understand project requirements, material needs |

### Micro Management (Tactical)

| Aspect | Description | RTS Examples | MineWright Analog |
|--------|-------------|--------------|-------------------|
| **Scope** | Short-term, individual unit actions | Unit kiting, focus fire, spell timing | Block placement precision, combat positioning, tool selection |
| **Time Scale** | Seconds to milliseconds | "Stim and retreat", "Force field now" | "Jump over gap", "switch to pickaxe", "shield block" |
| **Player Actions** | Many, rapid-fire commands | Click individual units, issue move commands | Manual override of specific actions |
| **Success Metrics** | Battle outcomes, unit preservation | "Won with 0 losses", "Perfect surround" | "Built without errors", "Survived skeleton ambush" |
| **Cognitive Load** | APM (actions per minute), reflexes | 300+ APM pro players | Not applicable (AI handles micro) |

---

## RTS Macro Control Patterns

### 1. High-Level Command Systems

#### StarCraft II Command System

```
Player Input
    ↓
┌─────────────────────────────────────┐
│  MACRO LAYER (Player Decisions)      │
│  - "Build supply depot"              │
│  - "Set rally point to minerals"     │
│  - "Research stimpack"               │
└─────────────┬───────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  INTERPRETATION LAYER (Game AI)      │
│  - Find build location               │
│  - Assign worker to construction     │
│  - Deduct resources                  │
└─────────────┬───────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  EXECUTION LAYER (Unit AI)           │
│  - Worker navigates to site          │
│  - Worker constructs (progress bar)  │
│  - Worker auto-returns to work       │
└─────────────────────────────────────┘
```

#### Command Types

1. **Build Commands**
   - Player selects building type
   - Player clicks placement location
   - AI assigns worker, deducts resources
   - Worker handles navigation, construction, resumption

2. **Production Commands**
   - Player sets rally points
   - Player queues units
   - AI handles unit spawning, routing to destination
   - Units auto-form or move to rally

3. **Research Commands**
   - Player selects upgrade
   - AI deducts resources, starts timer
   - AI applies upgrade globally when complete

4. **Attack/Move Commands**
   - Player selects units, right-clicks destination
   - AI calculates path for each unit
   - Units handle collision avoidance, formation

### 2. Worker AI Automation

#### Resource Gathering (StarCraft II)

```java
// Pseudo-code: StarCraft II worker AI
class WorkerAI {
    enum State {
        IDLE,           // Waiting for orders
        MOVING_TO_MINERALS,  // Walking to resource
        MINING,         // Gathering animation
        RETURNING,      // Carrying to base
        BUILDING,       // Constructing structure
        REPAIRING       // Fixing mechanical units
    }

    State currentState = State.IDLE;
    ResourceNode assignedResource;
    BaseEntity assignedBase;

    void onTick() {
        switch (currentState) {
            case IDLE:
                // Auto-assign to nearest unoccupied mineral patch
                assignedResource = findNearestUnoccupiedResource();
                if (assignedResource != null) {
                    setPathTo(assignedResource.position);
                    currentState = State.MOVING_TO_MINERALS;
                }
                break;

            case MOVING_TO_MINERALS:
                if (hasReached(assignedResource.position)) {
                    startMiningAnimation();
                    currentState = State.MINING;
                }
                break;

            case MINING:
                if (miningTimer.complete()) {
                    pickUpResource();
                    assignedBase = findNearestFriendlyBase();
                    setPathTo(assignedBase.position);
                    currentState = State.RETURNING;
                }
                break;

            case RETURNING:
                if (hasReached(assignedBase.position)) {
                    depositResource();
                    assignedBase = null;
                    currentState = State.IDLE;  // Loop back
                }
                break;
        }
    }

    // Player can interrupt at any time
    void onPlayerCommand(Command cmd) {
        if (cmd.isBuildCommand()) {
            // Interrupt mining to build
            currentState = State.BUILDING;
            startConstruction(cmd.buildingType, cmd.location);
        } else if (cmd.isRepairCommand()) {
            currentState = State.REPAIRING;
            startRepair(cmd.targetUnit);
        }
    }
}
```

#### Key Behaviors

1. **Auto-Assignment:** Idle workers automatically find tasks
2. **Resource Optimization:** Workers distribute evenly across mineral patches
3. **Persistent Tasks:** Workers return to mining after building
4. **Interruptible:** Player commands override auto-behavior
5. **Smart Resumption:** After interruption, workers resume previous task

### 3. Combat AI Automation

#### Auto-Attack Behavior

```java
// Pseudo-code: StarCraft II combat unit AI
class CombatUnitAI {
    enum Stance {
        AGGRESSIVE,  // Attack enemies in range
        DEFENSIVE,   // Only attack when attacked
        HOLD_POSITION,  // Don't move, but attack
        PATROL       // Move between waypoints
    }

    Stance currentStance = Stance.AGGRESSIVE;
    Unit currentTarget;

    void onTick() {
        switch (currentStance) {
            case AGGRESSIVE:
                // Auto-acquire targets in range
                currentTarget = findNearestEnemyInRange(weaponRange);
                if (currentTarget != null) {
                    if (inRange(currentTarget)) {
                        attack(currentTarget);
                    } else {
                        moveTowards(currentTarget);
                    }
                }
                break;

            case DEFENSIVE:
                // Only attack if being attacked
                if (isUnderAttack()) {
                    currentTarget = getAttacker();
                    attack(currentTarget);
                }
                break;

            case HOLD_POSITION:
                // Don't move, but attack enemies in range
                currentTarget = findNearestEnemyInRange(weaponRange);
                if (currentTarget != null) {
                    attack(currentTarget);
                }
                break;
        }
    }

    // Kiting behavior: shoot while retreating
    void kite(Unit enemy) {
        if (weaponCooldownReady() && inRange(enemy)) {
            attack(enemy);
        } else {
            moveAwayFrom(enemy);
        }
    }
}
```

#### Micro Techniques

1. **Focus Fire:** Multiple units target same enemy for quick kills
2. **Kiting:** Shoot while retreating (range units vs melee)
3. **Surround:** Units position to prevent enemy escape
4. **Spell Timing:** Abilities used at optimal moments
5. **Unit Preservation:** Low-health units retreat to safety

### 4. Building Production AI

#### Rally Point System

```java
// Pseudo-code: Building production with rally points
class ProductionBuilding {
    Vector3 rallyPoint;
    Queue<UnitType> productionQueue;

    void onUnitProduced(Unit unit) {
        // Newly produced units auto-move to rally point
        if (rallyPoint != null) {
            unit.moveTo(rallyPoint);

            // Set unit's default behavior based on building type
            if (this instanceof Barracks) {
                unit.setStance(Stance.AGGRESSIVE);
            } else if (this instanceof Factory) {
                unit.setStance(Stance.DEFENSIVE);
            }
        }
    }

    // Worker rally points: auto-assign to minerals
    void setRallyPointToMinerals(MineralPatch patch) {
        this.rallyPoint = patch.position;
        this.workerRallyMode = true;
    }

    void onWorkerProduced(SCV worker) {
        super.onUnitProduced(worker);
        if (workerRallyMode) {
            worker.assignedResource = findNearestMineralPatch(rallyPoint);
            worker.currentState = WorkerAI.State.MOVING_TO_MINERALS;
        }
    }
}
```

---

## RTS Micro Automation Examples

### 1. Worker AI: Complete Behavior Tree

```
┌─────────────────────────────────────────────────────────┐
│                    WORKER BEHAVIOR TREE                  │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
              ┌────────────────────────┐
              │  Do I have player       │──Yes──▶ Execute Player Command
              │  command pending?       │
              └────────┬───────────────┘
                       │ No
                       ▼
              ┌────────────────────────┐
              │  Am I carrying         │──Yes──▶ Return to Base
              │  resources?            │
              └────────┬───────────────┘
                       │ No
                       ▼
              ┌────────────────────────┐
              │  Is my assigned        │──Yes──▶ Move to Resources
              │  resource nearby?      │
              └────────┬───────────────┘
                       │ No
                       ▼
              ┌────────────────────────┐
              │  Find new unoccupied   │
              │  resource patch        │
              └────────────────────────┘
```

### 2. Combat AI: State Machine

```
┌─────────────────────────────────────────────────────────┐
│                   COMBAT UNIT STATE MACHINE              │
└─────────────────────────────────────────────────────────┘

    IDLE
     │
     │ (enemy in range)
     ▼
  ATTACKING ◄─────────┐
     │                 │
     │ (enemy dies)    │ (weapon cooling)
     │                 │
     ▼                 │
  RELOADING ──────────┘
     │
     │ (reload complete)
     ▼
  ATTACKING

    RETREATING (when health < 30%)
     │
     │ (health restored)
     ▼
    IDLE
```

### 3. Building Construction AI

```java
// Pseudo-code: Building construction progression
class BuildingConstruction {
    float progress = 0.0f;  // 0.0 to 1.0
    int requiredWorkerSlots = 1;
    List<Worker> assignedWorkers;

    void onTick() {
        // Progress based on number of workers
        float constructionRate = 0.01f * assignedWorkers.size();
        progress += constructionRate;

        // Visual feedback: building appears incrementally
        updateVisualModel(progress);

        if (progress >= 1.0f) {
            completeConstruction();
        }
    }

    // Workers can be added/removed dynamically
    void addWorker(Worker worker) {
        if (assignedWorkers.size() < requiredWorkerSlots) {
            assignedWorkers.add(worker);
            worker.currentState = WorkerAI.State.BUILDING;
        }
    }
}
```

---

## Hierarchical AI Architectures

### 1. Three-Layer RTS AI Architecture

Based on research from [三国志策略游戏AI分层架构设计](https://m.blog.csdn.net), modern RTS games use a three-layer hierarchy:

```
┌─────────────────────────────────────────────────────────┐
│  STRATEGIC LAYER (5-10 second cycles)                   │
│  Input: Cities, resources, tech progress, diplomacy     │
│  Output: Attack/Defend/Develop commands                 │
│  Responsibilities:                                      │
│  - Economic development decisions                       │
│  - Technology tree progression                          │
│  - Army composition goals                               │
│  - Expansion timing                                     │
└─────────────────────┬───────────────────────────────────┘
                      │ Commands
                      ▼
┌─────────────────────────────────────────────────────────┐
│  TACTICAL LAYER (Before battles/key moments)            │
│  Input: Force distribution, terrain, objectives         │
│  Output: Deployment plans, attack routes                │
│  Responsibilities:                                      │
│  - Unit group assignments                              │
│  - Attack route planning                               │
│  - Formation selection                                  │
│  - Priority targeting                                   │
└─────────────────────┬───────────────────────────────────┘
                      │ Assignments
                      ▼
┌─────────────────────────────────────────────────────────┐
│  EXECUTION LAYER (Per-frame updates)                    │
│  Input: Current coordinates, obstacles, targets          │
│  Output: Movement paths, attack targets                 │
│  Responsibilities:                                      │
│  - Pathfinding (A* navigation)                          │
│  - Collision avoidance                                  │
│  - Auto-targeting                                       │
│  - Ability execution                                    │
└─────────────────────────────────────────────────────────┘
```

### 2. Command Hierarchy (Feudal Reinforcement Learning)

From [Room Clearance with Feudal Hierarchical RL](https://xueshu.baidu.com/usercenter/paper/show?paperid=c34ff866f7d346f7def9d7c2dbb254c9):

```
                COMMANDER (High-Level)
                       │
        ┌──────────────┼──────────────┐
        │              │              │
    SQUAD A        SQUAD B        SQUAD C
    (Lieutenant)  (Lieutenant)  (Lieutenant)
        │              │              │
    ┌───┴───┐      ┌───┴───┐      ┌───┴───┐
   S1 S2 S3      S4 S5 S6      S7 S8 S9
  (Soldiers)    (Soldiers)    (Soldiers)

Command Flow:
1. Commander: "Secure the north building"
2. Lieutenants: Split into rooms, assign squads
3. Soldiers: Execute room-by-room clearance

Information Flow (Bottom-Up):
1. Soldiers: Report enemy contact, obstacles
2. Lieutenants: Adjust squad assignments, request support
3. Commander: Reallocate resources based on reports
```

### 3. Contract Net Protocol (Task Allocation)

From [Decentralized Task Allocation Research](https://www.nature.com/articles/s41598-025-21709-9):

```
Task Announcement Phase:
┌─────────────┐                    ┌─────────────┐
│  MANAGER    │                    │   WORKER 1  │
│ (Foreman)   │                    │             │
└──────┬──────┘                    └──────┬──────┘
       │                                  │
       │  ┌──────────────────────────┐    │
       │  │ "Task: Gather 64 cobble" │    │
       │  │ Location: (100, 64, 200) │    │
       │  │ Deadline: 5 minutes      │    │
       │  └──────────────────────────┘    │
       │           ANNOUNCE               │
       ├─────────────────────────────────▶│
       │                                  │
       │                          ┌───────┴────────┐
       │                          │ Evaluate task  │
       │                          │ - Can I do it? │
       │                          │ - How long?    │
       │                          │ - Capability?  │
       │                          └───────┬────────┘
       │                                  │
       │  BID ◄───────────────────────────┤
       │  "I can do it in 3 minutes"      │
       │                                  │
       │                          ┌───────┴────────┐
       │                          │ Wait for award │
       │                          └────────────────┘

Award Phase:
       │                                  │
       │  AWARD ──────────────────────────▶│
       │  "You got the job!"              │
       │                                  │
       │                          ┌───────┴────────┐
       │                          │ Start task     │
       │                          └────────────────┘
```

---

## Applicability to MineWright

### Current MineWright Architecture

Based on code analysis, MineWright already has foundational components:

```
Player Input (K key GUI)
    ↓
┌─────────────────────────────────────┐
│  TASK PLANNER (LLM)                 │
│  - Receives natural language command│
│  - Generates structured tasks       │
│  - Returns ParsedResponse           │
└─────────────┬───────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  ORCHESTRATOR SERVICE               │
│  - Foreman/Worker roles             │
│  - Task distribution (round-robin)  │
│  - Progress monitoring              │
│  - Retry on failure                 │
└─────────────┬───────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  ACTION EXECUTOR (Per Agent)         │
│  - Tick-based execution             │
│  - State machine (IDLE→PLANNING...) │
│  - Individual action implementations│
└─────────────────────────────────────┘
```

**Existing Strengths:**
- AgentStateMachine for state tracking
- OrchestratorService for multi-agent coordination
- AgentCommunicationBus for messaging
- Plugin architecture for actions
- Contract Net Protocol (TaskBid, TaskAnnouncement)

**Missing Macro/Micro Separation:**
- No clear distinction between strategic and tactical behaviors
- Workers don't have autonomous "idle" behaviors
- No behavior trees or hierarchical task decomposition
- Limited worker AI (actions are direct, not interpreted)

### Proposed Foreman/Worker Model

```
┌─────────────────────────────────────────────────────────┐
│                      HUMAN PLAYER                        │
│                    "Build a castle"                      │
└────────────────────────────┬────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│  FOREMAN (Strategic Layer)                              │
│  - Receives high-level command                          │
│  - Plans resource allocation                           │
│  - Decomposes into worker tasks                        │
│  - Monitors overall progress                           │
│                                                          │
│  Example Tasks:                                         │
│  - "Clear 100x100 area"                                 │
│  - "Gather 500 cobblestone"                             │
│  - "Build foundation"                                   │
│  - "Construct walls"                                    │
└────────────────────────────┬────────────────────────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  WORKER A    │    │  WORKER B    │    │  WORKER C    │
│  (Miner)     │    │  (Builder)   │    │  (Quarry)   │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  MICRO AI    │    │  MICRO AI    │    │  MICRO AI   │
│  (Tactical)  │    │  (Tactical)  │    │  (Tactical) │
│              │    │              │    │             │
│  - Pathfind  │    │  - Navigate │    │  - Find ore │
│  - Avoid mobs│    │  - Place    │    │  - Mine     │
│  - Return    │    │    blocks   │    │  - Deposit  │
└──────────────┘    └──────────────┘    └──────────────┘
```

---

## Proposed Behavior System

### 1. Worker Behavior Tree

```
                    ROOT
                      │
        ┌─────────────┴─────────────┐
        │   Has Player Command?     │──Yes──▶ EXECUTE_PLAYER_COMMAND
        └─────────────┬─────────────┘
                      │ No
                      ▼
        ┌─────────────────────────┐
        │   Assigned Task?        │──Yes──▶ EXECUTE_ASSIGNED_TASK
        └─────────────┬───────────┘
                      │ No
                      ▼
        ┌─────────────────────────┐
        │   Inventory Full?       │──Yes──▶ DEPOSIT_TO_STORAGE
        └─────────────┬───────────┘
                      │ No
                      ▼
        ┌─────────────────────────┐
        │   Can Auto-Assign?      │──Yes──▶ FIND_NEXT_TASK
        └─────────────┬───────────┘
                      │ No
                      ▼
                    IDLE
```

### 2. Role-Based Specializations

```java
// Worker roles with different micro behaviors
enum WorkerRole {
    MINER,      // Specializes in finding and mining ores
    BUILDER,    // Specializes in structure construction
    QUARRY,     // Specializes in bulk resource extraction
    FARMER,     // Specializes in crop cultivation
    LOGGER,     // Specializes in tree harvesting
    HAULER,     // Specializes in transport between chests
    GENERAL     // Can do any task (Jack of all trades)
}

// Role-specific behavior parameters
class RoleProfile {
    WorkerRole role;

    // Skill proficiencies (0.0 - 1.0)
    Map<String, Double> proficiencies;

    // Preferred tasks
    List<String> preferredTasks;

    // Default behavior when idle
    Consumer<WorkerEntity> idleBehavior;
}
```

### 3. Autonomous Task Queue

```java
// Workers maintain their own task queue
class WorkerTaskQueue {
    Queue<Task> personalQueue;
    Task currentTask;

    void onTick() {
        if (currentTask == null || currentTask.isComplete()) {
            if (!personalQueue.isEmpty()) {
                currentTask = personalQueue.poll();
                startTask(currentTask);
            } else {
                // No assigned tasks - check for auto-assign
                attemptAutoAssign();
            }
        }

        if (currentTask != null) {
            currentTask.tick();
        }
    }

    void attemptAutoAssign() {
        // Look for tasks in shared blackboard
        Task availableTask = blackboard.findTaskMatching(
            this.role,
            this.location,
            this.inventory
        );

        if (availableTask != null) {
            personalQueue.add(availableTask);
        }
    }
}
```

---

## Code Examples

### Example 1: Foreman Macro Command

```java
package com.minewright.macro;

import com.minewright.llm.ResponseParser;
import com.minewright.orchestration.OrchestratorService;
import com.minewright.entity.ForemanEntity;

/**
 * Foreman issues high-level macro commands.
 *
 * Unlike direct action execution, the Foreman:
 * 1. Plans resource requirements
 * 2. Decomposes into worker tasks
 * 3. Assigns tasks based on worker roles
 * 4. Monitors overall progress
 */
public class ForemanMacroController {

    private final OrchestratorService orchestrator;
    private final ForemanEntity foreman;

    public ForemanMacroController(OrchestratorService orchestrator,
                                   ForemanEntity foreman) {
        this.orchestrator = orchestrator;
        this.foreman = foreman;
    }

    /**
     * Process a high-level command from the human player.
     *
     * Example: "Build a stone castle with 4 towers"
     *
     * The Foreman will:
     * 1. Analyze the structure requirements
     * 2. Calculate material needs
     * 3. Create a task plan
     * 4. Distribute to workers
     */
    public MacroPlan planMacroCommand(String humanCommand) {
        // Step 1: Use LLM to understand requirements
        ResponseParser.ParsedResponse response =
            foreman.getTaskPlanner().planTasksAsync(humanCommand).join();

        // Step 2: Analyze resource requirements
        ResourceAnalysis resources = analyzeResourceNeeds(response);

        // Step 3: Create macro plan with dependencies
        MacroPlan plan = new MacroPlan(response.getPlan());

        // Phase 1: Resource gathering
        if (resources.needsCobblestone > 0) {
            plan.addPhase(new ResourcePhase(
                "Gather Materials",
                Map.of("cobblestone", resources.needsCobblestone,
                       "wood", resources.needsWood)
            ));
        }

        // Phase 2: Site preparation
        plan.addPhase(new PreparationPhase(
            "Clear and Level Site",
            response.getPlan().getBounds()
        ));

        // Phase 3: Construction
        plan.addPhase(new ConstructionPhase(
            "Build Structure",
            response.getTasks()
        ));

        // Step 4: Execute through orchestrator
        orchestrator.processHumanCommand(response,
            foreman.getAvailableWorkers());

        return plan;
    }

    /**
     * Analyze the material needs for a plan.
     * The Foreman is responsible for ensuring adequate resources.
     */
    private ResourceAnalysis analyzeResourceNeeds(
            ResponseParser.ParsedResponse response) {

        ResourceAnalysis analysis = new ResourceAnalysis();

        for (Task task : response.getTasks()) {
            switch (task.getAction()) {
                case "build":
                case "construct":
                    String blockType = task.getStringParameter("block", "stone");
                    int quantity = task.getIntParameter("quantity", 1);
                    analysis.addMaterial(blockType, quantity);
                    break;

                case "mine":
                case "gather":
                    // These produce, not consume
                    break;
            }
        }

        return analysis;
    }
}
```

### Example 2: Worker Micro AI

```java
package com.minewright.micro;

import com.minewright.entity.WorkerEntity;
import com.minewright.action.Task;
import com.minewright.blackboard.BlackboardSystem;

/**
 * Worker micro AI handles low-level autonomous behaviors.
 *
 * Key responsibilities:
 * - Pathfinding and navigation
 * - Combat self-preservation
 * - Resource gathering automation
 * - Idle behavior (auto-assign tasks)
 */
public class WorkerMicroAI {

    private final WorkerEntity worker;
    private final WorkerRole role;
    private final WorkerTaskQueue taskQueue;
    private final BlackboardSystem blackboard;

    // Current micro state
    private MicroState microState = MicroState.IDLE;
    private Task currentTask;

    public WorkerMicroAI(WorkerEntity worker, WorkerRole role) {
        this.worker = worker;
        this.role = role;
        this.taskQueue = new WorkerTaskQueue();
        this.blackboard = BlackboardSystem.getInstance();
    }

    /**
     * Called every game tick (20 times per second).
     * This is where autonomous micro behaviors live.
     */
    public void onTick() {
        // Priority 1: Survival (avoid mobs, lava, etc.)
        if (handleThreats()) {
            return;  // Defer everything if in danger
        }

        // Priority 2: Player override command
        if (worker.hasPlayerOverrideCommand()) {
            executePlayerCommand();
            return;
        }

        // Priority 3: Assigned task from Foreman
        if (currentTask != null && !currentTask.isComplete()) {
            executeCurrentTask();
            return;
        }

        // Priority 4: Auto-assign new task
        if (currentTask == null) {
            attemptAutoAssign();
        }

        // Priority 5: Role-specific idle behavior
        if (currentTask == null) {
            executeIdleBehavior();
        }
    }

    /**
     * Handle immediate threats (mobs, environmental damage).
     * This is pure survival instinct - no task is worth dying for.
     */
    private boolean handleThreats() {
        // Check for nearby hostile mobs
        if (worker.getNearestHostileMob() < 8.0) {
            microState = MicroState.FLEEING;

            // Run away to safety
            worker.navigateTo(worker.getNearestSafeLocation());

            // Notify foreman that we're interrupted
            sendMessageToForeman("Fleeing from hostile mob!");

            return true;
        }

        // Check for environmental hazards
        if (worker.isInLava() || worker.isInCactus()) {
            microState = MicroState.EMERGENCY_ESCAPE;
            worker.navigateTo(worker.getNearestSafeLocation());
            return true;
        }

        return false;
    }

    /**
     * Execute the current assigned task.
     * The worker interprets high-level tasks into low-level actions.
     */
    private void executeCurrentTask() {
        switch (currentTask.getAction()) {
            case "mine":
                executeMiningTask(currentTask);
                break;

            case "build":
                executeBuildingTask(currentTask);
                break;

            case "gather":
                executeGatheringTask(currentTask);
                break;

            case "move":
                executeMoveTask(currentTask);
                break;
        }
    }

    /**
     * Mining task micro behavior.
     *
     * High-level: "Mine 64 cobblestone"
     * Micro interpretation:
     * 1. Find nearest stone
     * 2. Path to it
     * 3. Equip pickaxe
     * 4. Mine block
     * 5. Repeat until inventory full or quota met
     */
    private void executeMiningTask(Task task) {
        String resourceType = task.getStringParameter("resource");
        int targetQuantity = task.getIntParameter("quantity", 64);
        int currentQuantity = worker.countInventoryItem(resourceType);

        if (currentQuantity >= targetQuantity) {
            // Task complete!
            currentTask.complete();
            reportTaskCompletion();
            return;
        }

        // Check if inventory full
        if (worker.getInventoryFreeSlots() == 0) {
            // Need to deposit
            microState = MicroState.DEPOSITING;
            worker.navigateToNearestStorage();

            if (worker.isAtStorage()) {
                worker.depositAll(resourceType);
                microState = MicroState.WORKING;
            }
            return;
        }

        // Find nearest target block
        if (!worker.hasTargetBlock()) {
            worker.findNearestBlock(resourceType);
        }

        // Navigate to target
        if (!worker.isAtTarget()) {
            worker.navigateToTarget();
            return;
        }

        // Mine the block
        worker.equipToolFor(resourceType);
        worker.mineTarget();

        // Update progress
        task.updateProgress(currentQuantity, targetQuantity);
    }

    /**
     * Building task micro behavior.
     *
     * High-level: "Build wall at (100, 64, 100)"
     * Micro interpretation:
     * 1. Check if have required blocks
     * 2. If not, fetch from storage
     * 3. Navigate to build site
     * 4. Place blocks according to blueprint
     * 5. Handle obstacles (mobs, terrain)
     */
    private void executeBuildingTask(Task task) {
        Blueprint blueprint = task.getBlueprint();
        BlockPos nextBlock = blueprint.getNextBlock();

        // Check if have required materials
        String requiredBlock = blueprint.getBlockType(nextBlock);
        if (!worker.hasInInventory(requiredBlock)) {
            // Fetch from storage
            microState = MicroState.FETCHING_MATERIALS;
            worker.fetchFromStorage(requiredBlock);
            return;
        }

        // Navigate to build location
        if (!worker.isInRange(nextBlock, 5.0)) {
            worker.navigateTo(nextBlock);
            return;
        }

        // Place the block
        worker.placeBlock(nextBlock, requiredBlock);
        blueprint.markBlockPlaced(nextBlock);

        // Update progress
        task.updateProgress(bluemark.getPlacedCount(),
                           blueprint.getTotalCount());

        if (blueprint.isComplete()) {
            currentTask.complete();
            reportTaskCompletion();
        }
    }

    /**
     * Attempt to auto-assign a task from the shared blackboard.
     *
     * This is where workers proactively look for work instead of
     * waiting for the Foreman to assign everything.
     */
    private void attemptAutoAssign() {
        // Check blackboard for tasks matching my role
        Task availableTask = blackboard.findTask(
            task -> task.matchesRole(role)
                 && task.isInRange(worker.getPosition(), 50)
                 && worker.hasCapabilitiesFor(task)
        );

        if (availableTask != null) {
            // Announce intent to other workers (prevent duplicate work)
            if (blackboard.claimTask(availableTask.getId(), worker.getId())) {
                currentTask = availableTask;
                microState = MicroState.WORKING;

                // Notify foreman
                sendMessageToForeman("Auto-assigned task: " +
                    availableTask.getDescription());
            }
        }
    }

    /**
     * Execute role-specific idle behavior.
     *
     * When a worker has no assigned tasks, they don't just stand still.
     * They perform role-appropriate autonomous behaviors.
     */
    private void executeIdleBehavior() {
        microState = MicroState.IDLE;

        switch (role) {
            case MINER:
                // Miners automatically look for exposed ores
                if (worker.canSeeOre()) {
                    worker.navigateTo(worker.getNearestOre());
                } else {
                    // Miners explore caves when idle
                    worker.exploreNearbyCave();
                }
                break;

            case BUILDER:
                // Builders look for incomplete structures
                Blueprint incomplete = blackboard.findIncompleteBlueprint();
                if (incomplete != null) {
                    currentTask = incomplete.asTask();
                }
                break;

            case QUARRY:
                // Quarry workers always return to designated quarry
                worker.navigateTo(worker.getAssignedQuarryLocation());
                break;

            case FARMER:
                // Farmers tend to nearby crops
                worker.tendNearbyCrops();
                break;

            case HAULER:
                // Haulers look for full chests needing emptying
                worker.findAndMoveItems();
                break;

            case GENERAL:
                // General workers wait at foreman's location
                worker.navigateTo(worker.getForeman().getPosition());
                break;
        }
    }

    /**
     * Send a message to the Foreman.
     * Used for status updates, help requests, completion reports.
     */
    private void sendMessageToForeman(String message) {
        AgentMessage msg = AgentMessage.builder()
            .type(AgentMessage.Type.STATUS_REPORT)
            .sender(worker.getId(), worker.getName())
            .recipient(worker.getForeman().getId())
            .content(message)
            .priority(AgentMessage.Priority.NORMAL)
            .build();

        worker.getCommunicationBus().publish(msg);
    }

    private void reportTaskCompletion() {
        TaskCompletionReport report = new TaskCompletionReport(
            worker.getId(),
            currentTask.getId(),
            currentTask.getResult()
        );

        sendMessageToForeman("Task complete: " + report.getSummary());
    }

    enum MicroState {
        IDLE,           // No task, performing idle behavior
        WORKING,        // Executing assigned task
        FLEEING,        // Running from threat
        EMERGENCY_ESCAPE,  // Environmental hazard
        DEPOSITING,     // Returning items to storage
        FETCHING_MATERIALS,  // Getting materials for building
        TRAVELING       // Moving to task location
    }
}
```

### Example 3: Behavior Tree Node

```java
package com.minewright.behavior;

import com.minewright.entity.WorkerEntity;

/**
 * Base class for behavior tree nodes.
 *
 * Behavior trees provide a hierarchical way to organize AI decisions.
 * Each node returns a status: SUCCESS, FAILURE, or RUNNING.
 */
public abstract class BehaviorNode {

    public enum Status {
        SUCCESS,   // Node completed successfully
        FAILURE,   // Node failed, try sibling
        RUNNING    // Node still executing
    }

    /**
     * Execute this behavior node.
     * Called every tick until the node returns SUCCESS or FAILURE.
     */
    public abstract Status execute(WorkerEntity worker);

    /**
     * Reset this node's internal state.
     * Called when the behavior tree is restarted.
     */
    public void reset() {
        // Default: no state to reset
    }
}

/**
 * Selector node: tries children in order until one succeeds.
 *
 * Pattern: "Try this, else try that, else try that..."
 */
class SelectorNode extends BehaviorNode {
    private final List<BehaviorNode> children;
    private int currentChild = 0;

    @SafeVarargs
    public SelectorNode(BehaviorNode... children) {
        this.children = Arrays.asList(children);
    }

    @Override
    public Status execute(WorkerEntity worker) {
        while (currentChild < children.size()) {
            Status status = children.get(currentChild).execute(worker);

            switch (status) {
                case RUNNING:
                    return Status.RUNNING;  // Still executing

                case SUCCESS:
                    reset();  // Reset for next time
                    return Status.SUCCESS;

                case FAILURE:
                    currentChild++;  // Try next child
                    break;
            }
        }

        reset();  // All children failed
        return Status.FAILURE;
    }

    @Override
    public void reset() {
        currentChild = 0;
        children.forEach(BehaviorNode::reset);
    }
}

/**
 * Sequence node: executes children in order until one fails.
 *
 * Pattern: "Do this, then do that, then do that..."
 */
class SequenceNode extends BehaviorNode {
    private final List<BehaviorNode> children;
    private int currentChild = 0;

    @SafeVarargs
    public SequenceNode(BehaviorNode... children) {
        this.children = Arrays.asList(children);
    }

    @Override
    public Status execute(WorkerEntity worker) {
        while (currentChild < children.size()) {
            Status status = children.get(currentChild).execute(worker);

            switch (status) {
                case RUNNING:
                    return Status.RUNNING;  // Still executing

                case FAILURE:
                    reset();  // Reset for next time
                    return Status.FAILURE;

                case SUCCESS:
                    currentChild++;  // Move to next child
                    break;
            }
        }

        reset();  // All children succeeded
        return Status.SUCCESS;
    }

    @Override
    public void reset() {
        currentChild = 0;
        children.forEach(BehaviorNode::reset);
    }
}

/**
 * Condition node: checks a condition, returns SUCCESS/FAILURE.
 */
class ConditionNode extends BehaviorNode {
    private final Predicate<WorkerEntity> condition;

    public ConditionNode(Predicate<WorkerEntity> condition) {
        this.condition = condition;
    }

    @Override
    public Status execute(WorkerEntity worker) {
        return condition.test(worker) ? Status.SUCCESS : Status.FAILURE;
    }
}

/**
 * Action node: executes an action, returns RUNNING until complete.
 */
class ActionNode extends BehaviorNode {
    private final Consumer<WorkerEntity> action;
    private final Predicate<WorkerEntity> isComplete;

    public ActionNode(Consumer<WorkerEntity> action,
                     Predicate<WorkerEntity> isComplete) {
        this.action = action;
        this.isComplete = isComplete;
    }

    @Override
    public Status execute(WorkerEntity worker) {
        action.accept(worker);
        return isComplete.test(worker) ? Status.SUCCESS : Status.RUNNING;
    }
}

/**
 * Complete worker behavior tree using the node types above.
 */
class WorkerBehaviorTree {
    private final BehaviorNode root;

    public WorkerBehaviorTree(WorkerEntity worker) {
        this.root = new SelectorNode(
            // Priority 1: Survival
            new SequenceNode(
                new ConditionNode(w -> w.isInDanger(),
                new ActionNode(
                    w -> w.fleeToSafety(),
                    w -> !w.isInDanger()
                )
            ),

            // Priority 2: Player command
            new SequenceNode(
                new ConditionNode(w -> w.hasPlayerCommand()),
                new ActionNode(
                    w -> w.executePlayerCommand(),
                    w -> !w.hasPlayerCommand()
                )
            ),

            // Priority 3: Assigned task
            new SequenceNode(
                new ConditionNode(w -> w.hasAssignedTask()),
                new ActionNode(
                    w -> w.executeAssignedTask(),
                    w -> !w.hasAssignedTask()
                )
            ),

            // Priority 4: Idle behavior
            new ActionNode(
                w -> w.performIdleBehavior(),
                w -> w.hasAssignedTask()  // Stop when assigned
            )
        );
    }

    public void tick(WorkerEntity worker) {
        root.execute(worker);
    }
}
```

### Example 4: Task Blackboard

```java
package com.minewright.blackboard;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared blackboard for task announcement and discovery.
 *
 * Workers can post tasks they discover (e.g., "Found coal deposit!")
 * and browse tasks that need doing.
 *
 * This enables decentralized task allocation without the Foreman
 * micromanaging every decision.
 */
public class TaskBlackboard {

    private static final TaskBlackboard INSTANCE = new TaskBlackboard();

    public static TaskBlackboard getInstance() {
        return INSTANCE;
    }

    // Tasks available for claiming
    private final Map<String, BlackboardTask> availableTasks;

    // Tasks claimed by specific workers
    private final Map<String, String> taskClaims;  // taskId -> workerId

    // Tasks organized by category for faster lookup
    private final Map<TaskCategory, List<BlackboardTask>> tasksByCategory;

    private TaskBlackboard() {
        this.availableTasks = new ConcurrentHashMap<>();
        this.taskClaims = new ConcurrentHashMap<>();
        this.tasksByCategory = new ConcurrentHashMap<>();

        for (TaskCategory category : TaskCategory.values()) {
            tasksByCategory.put(category, new ArrayList<>());
        }
    }

    /**
     * Post a task to the blackboard.
     *
     * Anyone can post tasks: Foreman, workers, even external systems.
     *
     * Examples:
     * - Foreman: "Build wall at (100, 64, 100)"
     * - Worker: "Found diamond at (50, 12, 200)"
     * - System: "Refuel furnace at (-10, 64, 50)"
     */
    public void postTask(BlackboardTask task) {
        availableTasks.put(task.getId(), task);
        tasksByCategory.get(task.getCategory()).add(task);

        LOGGER.debug("Task posted to blackboard: {} ({})",
            task.getId(), task.getTitle());
    }

    /**
     * Claim a task from the blackboard.
     *
     * Uses atomic compare-and-set to prevent multiple workers
     * from claiming the same task.
     *
     * @return true if claim successful, false if already claimed
     */
    public boolean claimTask(String taskId, String workerId) {
        BlackboardTask task = availableTasks.get(taskId);
        if (task == null) {
            return false;  // Task doesn't exist
        }

        // Atomic claim
        String previousClaim = taskClaims.putIfAbsent(taskId, workerId);
        if (previousClaim != null) {
            return false;  // Already claimed by another worker
        }

        task.setClaimedBy(workerId);
        task.setClaimTime(Instant.now());

        LOGGER.debug("Task {} claimed by worker {}", taskId, workerId);
        return true;
    }

    /**
     * Release a task claim (e.g., worker can't complete it).
     */
    public void releaseTask(String taskId, String workerId) {
        if (workerId.equals(taskClaims.get(taskId))) {
            taskClaims.remove(taskId);

            BlackboardTask task = availableTasks.get(taskId);
            if (task != null) {
                task.setClaimedBy(null);
                task.incrementRetryCount();
            }

            LOGGER.debug("Task {} released by worker {}", taskId, workerId);
        }
    }

    /**
     * Mark a task as complete.
     */
    public void completeTask(String taskId, String workerId, TaskResult result) {
        if (workerId.equals(taskClaims.get(taskId))) {
            BlackboardTask task = availableTasks.remove(taskId);
            taskClaims.remove(taskId);

            if (task != null) {
                tasksByCategory.get(task.getCategory()).remove(task);

                LOGGER.info("Task {} completed by worker {}: {}",
                    taskId, workerId, result.getSummary());
            }
        }
    }

    /**
     * Find tasks matching given criteria.
     *
     * Workers use this to auto-assign tasks that match their role
     * and capabilities.
     */
    public List<BlackboardTask> findTasks(TaskFilter filter) {
        return availableTasks.values().stream()
            .filter(filter::matches)
            .sorted(Comparator.comparing(BlackboardTask::getPriority).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Get the best task for a specific worker.
     *
     * Considers:
     * - Worker's role
     * - Worker's capabilities
     * - Worker's current location
     * - Task priority
     * - Task deadline
     */
    public BlackboardTask findBestTaskFor(WorkerProfile profile) {
        return availableTasks.values().stream()
            .filter(task -> task.matchesRole(profile.getRole()))
            .filter(task -> profile.hasCapabilitiesFor(task))
            .filter(task -> task.isInRange(profile.getLocation(), 100))
            .max(Comparator.comparing(task ->
                calculateTaskScore(task, profile)))
            .orElse(null);
    }

    private double calculateTaskScore(BlackboardTask task,
                                     WorkerProfile profile) {
        double score = 0.0;

        // Priority is most important
        score += task.getPriority().getValue() * 1000;

        // Distance matters (closer is better)
        double distance = task.getLocation().distanceTo(profile.getLocation());
        score -= distance * 10;

        // Deadline urgency
        if (task.getDeadline() != null) {
            long timeUntilDeadline = ChronoUnit.MINUTES.between(
                Instant.now(), task.getDeadline());
            score += (1000.0 / Math.max(1, timeUntilDeadline));
        }

        // Role match bonus
        if (task.getPreferredRole() == profile.getRole()) {
            score += 500;
        }

        return score;
    }
}

/**
 * A task on the blackboard.
 */
public class BlackboardTask {
    private final String id;
    private final String title;
    private final TaskCategory category;
    private final BlockPos location;
    private final Map<String, Object> requirements;
    private final TaskPriority priority;
    private final Instant deadline;

    private String claimedBy;
    private Instant claimTime;
    private int retryCount;

    public BlackboardTask(String title, TaskCategory category,
                         BlockPos location, TaskPriority priority) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.category = category;
        this.location = location;
        this.priority = priority;
        this.requirements = new HashMap<>();
        this.deadline = null;
    }

    // Getters and setters...

    public boolean matchesRole(WorkerRole role) {
        WorkerRole preferred = (WorkerRole) requirements.get("preferredRole");
        return preferred == null || preferred == role;
    }

    public boolean isInRange(BlockPos pos, double maxDistance) {
        return location.distanceTo(pos) <= maxDistance;
    }
}

enum TaskCategory {
    MINING,     // Ore extraction
    BUILDING,   // Structure construction
    GATHERING,  // Resource collection
    CRAFTING,   // Item crafting
    FARMING,    // Crop cultivation
    LOGGING,    // Tree harvesting
    TRANSPORT,  // Item movement
    MAINTENANCE // Machine refueling, repair
}

enum TaskPriority {
    CRITICAL(100),  // Blocking progress
    HIGH(75),       // Important but not blocking
    NORMAL(50),     // Standard task
    LOW(25),        // Nice to have
    BACKGROUND(10); // Fallback task

    private final int value;
    TaskPriority(int value) { this.value = value; }
    public int getValue() { return value; }
}
```

---

## Implementation Roadmap

### Phase 1: Foundational Behaviors (Week 1-2)

**Goal:** Workers can perform autonomous actions without Foreman micromanagement.

- [ ] Implement `WorkerMicroAI` class
- [ ] Add threat detection and evasion
- [ ] Add inventory-full detection and auto-deposit
- [ ] Add role-based idle behaviors
- [ ] Implement basic behavior tree nodes

**Testing:**
- Spawn worker in dangerous area, verify it flees
- Fill worker inventory, verify it finds chest
- Leave miner idle, verify it explores caves

### Phase 2: Task Blackboard (Week 2-3)

**Goal:** Decentralized task discovery and allocation.

- [ ] Implement `TaskBlackboard` system
- [ ] Add task posting API (Foreman and workers)
- [ ] Add task claiming with atomic operations
- [ ] Add task filtering by role, location, capabilities
- [ ] Add task scoring for auto-assignment

**Testing:**
- Foreman posts 10 tasks, verify workers distribute evenly
- Worker posts "found diamond", verify another worker claims it
- Test concurrent claiming (verify no duplicates)

### Phase 3: Foreman Macro Planning (Week 3-4)

**Goal:** Foreman decomposes high-level commands into worker tasks.

- [ ] Implement `ForemanMacroController`
- [ ] Add resource requirement analysis
- [ ] Add multi-phase planning (gather → build → finish)
- [ ] Add dependency tracking (can't build until materials gathered)
- [ ] Add progress monitoring and rebalancing

**Testing:**
- Command "build a house", verify 3-phase execution
- Command "gather 64 iron", verify workers coordinate
- Remove worker mid-task, verify rebalancing

### Phase 4: Role Specialization (Week 4-5)

**Goal:** Workers excel at specific tasks based on role.

- [ ] Implement `WorkerRole` enum and profiles
- [ ] Add role-specific skill proficiencies
- [ ] Add preferred task lists per role
- [ ] Add idle behaviors per role
- [ ] Add role-based task routing

**Testing:**
- Miners automatically find and mine ores
- Builders prioritize incomplete structures
- Farmers automatically tend crops

### Phase 5: Advanced Coordination (Week 5-6)

**Goal:** Multiple workers collaborate on complex projects.

- [ ] Implement spatial partitioning for large builds
- [ ] Add collaborative building (workers on different sections)
- [ ] Add supply chain (haulers bring materials to builders)
- [ ] Add dynamic rebalancing based on worker speed
- [ ] Add help request system

**Testing:**
- Build large structure with 5 workers, verify parallel execution
- Block builder's path, verify hauler brings materials
- Worker stuck, verify help request and assistance

---

## References

### RTS Game Research

1. **[Macro vs Micro in RTS Games](http://www.kekenet.com/lesson/19170-728756)**
   - Defines macro as economy/technology management
   - Defines micro as unit control in battles
   - Discusses the balance between both for competitive play

2. **[AlphaStar: Mastering StarCraft II](https://deepmind.google/discover/blog/alphastar-mastering-the-real-time-strategy-game-starcraft-ii/)**
   - DeepMind's breakthrough in RTS AI
   - Highlights the challenge of balancing macro and micro
   - Uses hierarchical agent architecture

3. **[Project MUSE: StarCraft Analysis](https://muse.jhu.edu/pub/166/oa_monograph/chapter/3765257)**
   - Academic analysis of StarCraft strategy
   - Economic advantage (macro) ensures long-term victory
   - Unit control (micro) crucial for battle outcomes

### Hierarchical AI Architectures

4. **[三国志策略游戏AI分层架构设计](https://m.blog.csdn.net)**
   - Three-layer AI architecture for strategy games
   - Strategic layer (5-10 turn cycles)
   - Tactical layer (battle planning)
   - Execution layer (per-frame updates)

5. **[Hierarchical Adversarial Search for StarCraft](https://ojs.aaai.org/index.php/AIIDE/article/view/12811)**
   - Military chain-of-command hierarchy
   - Outperforms flat search algorithms
   - Tested with up to 72 units under 40ms constraints

6. **[Feudal Hierarchical Reinforcement Learning](https://xueshu.baidu.com/usercenter/paper/show?paperid=c34ff866f7d346f7def9d7c2dbb254c9)**
   - Commander → Lieutenant → Soldier hierarchy
   - Efficient for multi-agent coordination
   - Outperforms standard RL algorithms

### Task Allocation

7. **[Decentralized Task Allocation for Dynamic Multi-Agent Systems](https://www.nature.com/articles/s41598-025-21709-9)**
   - Contract Net Protocol implementation
   - Scalable without centralized coordination
   - Auction-based task negotiation

8. **[PySC2 Resource Collection Optimization](https://m.blog.csdn.net/gitblog_00038/article/details/154051572)**
   - Dynamic priority scheduling for resource gathering
   - Multi-unit coordination for collection
   - Path planning to avoid conflicts

### Behavior Trees and State Machines

9. **[Unity Game AI Programming](https://cnblogs.com)**
   - Finite State Machines (FSM) basics
   - Hierarchical FSM for complex AI
   - Behavior trees for modular AI design

10. **[Game AI Architecture](https://www.douban.com/group/topic/78004392/)**
    - Movement, Decision, and Strategic layers
    - Combined with animation and physics systems
    - Production-grade implementations for Unity/Unreal

### Worker AI and Automation

11. **[CommandCenter SC2 AI Bot](https://m.blog.csdn.net/gitblog_00400/article/details/142115688)**
    - WorkerManager for resource gathering
    - Build order execution system
    - Building placement algorithms

12. **[StarCraft 2 Mining Optimization](https://m.duote.com/tech/sc2/6114.html)**
    - Worker distribution techniques
    - 2 workers per mineral patch for optimal efficiency
    - Rally point strategies

### Pathfinding and Combat AI

13. **[SparCraft Combat Simulator](https://xueshu.baidu.com/usercenter/paper/show?paperid=c34ff866f7d346f7def9d7c2dbb254c9)**
    - Millions of unit actions per second
    - Portfolio Greedy Search for combat
    - Real-time constraints

14. **[Amit's Game Programming Info](http://www-cs-students.stanford.edu/~amitp/gameprog.html)**
    - Coordinated unit movement
    - Formation control
    - Potential fields for navigation

---

## Conclusion

RTS games have perfected the art of balancing **strategic oversight** with **tactical autonomy**. By separating macro-level decisions (economy, production, strategy) from micro-level behaviors (pathfinding, combat automation, resource gathering), they enable players to control hundreds of units through intuitive high-level commands.

For MineWright, adopting these patterns offers a clear path to scalable multi-agent coordination:

1. **Foreman** as the strategic layer: Receives human commands, plans resource allocation, decomposes into worker tasks
2. **Workers** as the tactical layer: Interpret tasks into low-level actions, handle autonomous behaviors, collaborate via blackboard
3. **Behavior trees** for decision logic: Priority-based action selection, interruptible by threats, idle behaviors when unassigned
4. **Task blackboard** for decentralization: Workers can discover and claim tasks without Foreman micromanagement
5. **Role specialization** for efficiency: Miners, builders, haulers excel at specific tasks with tailored idle behaviors

The proposed architecture maintains MineWright's existing strengths (OrchestratorService, AgentStateMachine, plugin architecture) while adding the macro/micro separation that makes RTS AI both powerful and manageable.

**Next Steps:**
1. Review this research document with the development team
2. Prioritize features based on development capacity
3. Begin implementation with Phase 1 (Foundational Behaviors)
4. Iterate based on testing and community feedback

---

**Document Version:** 1.0
**Last Updated:** 2026-02-28
**Author:** Research synthesis from web sources and MineWright codebase analysis
