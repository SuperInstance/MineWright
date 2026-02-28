# Multi-Agent Coordination - Developer Analysis

**Date:** 2026-02-28
**Author:** Senior Developer Analysis
**Status:** Implementation Recommendations

---

## Executive Summary

This document analyzes MineWright's current multi-agent coordination implementation and provides specific recommendations for adopting RTS-style macro/micro patterns. The goal is to transform agents from "idle workers waiting for commands" into "fellow players" with autonomous behaviors, natural conversation, and intelligent coordination.

**Key Finding:** MineWright has excellent foundational infrastructure (Contract Net, OrchestratorService, Blackboard, CommunicationBus) but lacks the **macro/micro separation** and **worker autonomy** that makes RTS AI feel alive and responsive.

---

## 1. Current Coordination Architecture

### 1.1 Existing Components

MineWright already has a sophisticated coordination infrastructure:

```
┌─────────────────────────────────────────────────────────────┐
│                    CURRENT ARCHITECTURE                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Human Player                                                │
│       │                                                      │
│       ▼                                                      │
│  ┌──────────────────┐                                       │
│  │  ForemanEntity   │ ← Receives K-key GUI commands         │
│  │  (Mace)          │                                       │
│  └────────┬─────────┘                                       │
│           │                                                 │
│           ▼                                                 │
│  ┌──────────────────────────────────────────────────┐      │
│  │         OrchestratorService                      │      │
│  │  - processHumanCommand()                        │      │
│  │  - distributeTasks() (round-robin)               │      │
│  │  - AgentCommunicationBus messaging              │      │
│  └────────┬─────────────────────────────────────────┘      │
│           │                                                 │
│      ┌────┴─────┬────────────┐                             │
│      ▼           ▼            ▼                             │
│  ┌────────┐ ┌────────┐  ┌────────┐                          │
│  │Worker  │ │Worker  │  │Worker  │ ← ForemanEntity          │
│  │Mine    │ │Mine    │  │Mine    │   instances              │
│  │Wright  │ │Wright  │  │Wright  │                          │
│  └────────┘ └────────┘  └────────┘                          │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Contract Net Protocol Implementation

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\coordination\ContractNetManager.java`

**Current Implementation:**
- Full Contract Net Protocol (announcement → bid → award → execute)
- Thread-safe negotiation tracking with `ContractNegotiation` state
- Deadline-based bid collection (30 second windows)
- Bid evaluation based on `TaskBid.getBidValue()`

**Strengths:**
- Excellent implementation of decentralized task allocation
- Proper listener pattern for events (`ContractListener`)
- Concurrent safety with `ConcurrentHashMap` and synchronized blocks

**Limitation:**
- Not integrated with worker autonomous behavior
- Workers don't auto-bid on tasks (requires explicit call to `submitBid()`)
- No capability-aware filtering (workers bid on everything)

### 1.3 OrchestratorService Analysis

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\orchestration\OrchestratorService.java`

**Current Behavior:**
1. Receives `ParsedResponse` from LLM (TaskPlanner output)
2. Creates `PlanExecution` with task list
3. Distributes tasks via round-robin to available workers
4. Sends `TASK_ASSIGNMENT` messages through `AgentCommunicationBus`
5. Monitors progress, handles failures, retries tasks

**Current Distribution Strategy:**
```java
// Lines 363-385: Round-robin with foreman fallback
int workerIndex = 0;
for (Task task : tasks) {
    if (availableWorkers.isEmpty()) {
        assignTaskToAgent(plan, task, foremanId); // Foreman takes remaining
    } else {
        ForemanEntity worker = availableWorkers.get(
            workerIndex % availableWorkers.size());
        assignTaskToAgent(plan, task, worker.getEntityName());
        workerIndex++;
    }
}
```

**Strengths:**
- Clear separation of concerns (orchestration vs execution)
- Proper retry mechanism (MAX_TASK_RETRIES = 2)
- Agent registration/unregistration with cleanup
- Plan progress tracking

**Limitations:**
- No macro planning (just distributes LLM tasks directly)
- No worker role specialization
- No worker idle behavior management
- No supply chain coordination (haulers, builders, miners working together)

### 1.4 Communication Infrastructure

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\communication\CommunicationBus.java`

**Features:**
- Direct and broadcast messaging
- Message queues per agent (max 100 messages)
- Request/response correlation with `CompletableFuture`
- Message history (last 1000 messages)
- Statistics tracking

**Message Types:** (from `AgentMessage.java`)
- `TASK_ASSIGNMENT` - Foreman → Worker
- `TASK_PROGRESS` - Worker → Foreman (percent complete)
- `TASK_COMPLETE` - Worker → Foreman
- `TASK_FAILED` - Worker → Foreman
- `HELP_REQUEST` - Worker → Foreman
- `STATUS_REPORT` - Worker → Foreman
- `PLAN_ANNOUNCEMENT` - Foreman → All
- `BROADCAST` - General announcements

**Strengths:**
- Production-ready message bus
- Proper error handling and statistics
- Thread-safe queues

**Limitations:**
- No agent-to-agent chat (only foreman ↔ worker)
- No coordination messages (worker → worker)
- No conversation context sharing
- No personality-driven message filtering

### 1.5 Blackboard System

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\blackboard\Blackboard.java`

**Knowledge Areas:**
- `WORLD_STATE` - Block positions, entities
- `AGENT_STATUS` - Agent states, locations
- `TASKS` - Active tasks, progress
- `THREATS` - Hostile mobs, dangers
- `RESOURCES` - Material locations
- `GOALS` - High-level objectives

**Features:**
- Typed entries (FACT, HYPOTHESIS, GOAL, CONSTRAINT)
- Confidence levels (0.0 - 1.0)
- Subscriber notifications on changes
- Staleness management (auto-eviction)
- Thread-safe with `ReadWriteLock`

**Strengths:**
- Excellent implementation of blackboard pattern
- Proper partitioning by knowledge area
- Reactive updates via subscriptions

**Current Underutilization:**
- Workers don't auto-post discoveries
- No task announcement blackboard (decentralized task claiming)
- No shared conversation context

---

## 2. Macro/Micro Separation Analysis

### 2.1 What We're Missing

RTS games have a clear separation that MineWright lacks:

| Aspect | RTS Games (StarCraft) | Current MineWright |
|--------|----------------------|-------------------|
| **Strategic Layer** | Player commands: "Build barracks", "Expand to natural" | Player types natural language → LLM generates tasks |
| **Tactical Layer** | Workers interpret: Find build site, gather resources, construct | Workers receive direct task assignments |
| **Worker AI** | Autonomous when idle: Auto-assign to minerals, return after building | Workers wait for foreman assignment |
| **Coordination** | Decentralized: Workers notice needs, request help | Centralized: All coordination through foreman |

### 2.2 Current Flow Analysis

**Current Human Command Flow:**
```
Player presses K → Types "Build a stone house" → TaskPlanner.planTasksAsync()
    ↓
LLM generates: ParsedResponse { plan: "Build stone house", tasks: [...] }
    ↓
OrchestratorService.processHumanCommand()
    ↓
PlanExecution created, tasks distributed round-robin
    ↓
Workers receive TASK_ASSIGNMENT messages
    ↓
Workers execute ActionExecutor.tick()
    ↓
Workers send TASK_COMPLETE when done
```

**Problem:** This is a flat hierarchy. The foreman doesn't do macro planning - it just distributes what the LLM gives it.

### 2.3 Recommended Macro/Micro Split

```
┌─────────────────────────────────────────────────────────────┐
│              RECOMMENDED RTS-STYLE ARCHITECTURE              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Human Player                                                │
│       │ "Build a castle"                                     │
│       ▼                                                      │
│  ┌──────────────────────────────────────────────────┐       │
│  │         MACRO LAYER (Foreman)                     │       │
│  │                                                   │       │
│  │  1. Analyze requirements                          │       │
│  │     - "Castle needs 5000 stone, 1000 wood"        │       │
│  │                                                   │       │
│  │  2. Create phases                                 │       │
│  │     - Phase 1: Gather materials (2 workers)       │       │
│  │     - Phase 2: Clear site (1 worker)              │       │
│  │     - Phase 3: Build foundation (2 workers)       │       │
│  │     - Phase 4: Construct walls (3 workers)        │       │
│  │                                                   │       │
│  │  3. Post tasks to Blackboard                      │       │
│  │     - High-level tasks with dependencies          │       │
│  └────────┬──────────────────────────────────────────┘       │
│           │ Tasks posted to Blackboard                        │
│           │                                                 │
│      ┌────┴──────────┬──────────────┬──────────────┐        │
│      ▼               ▼              ▼              ▼         │
│  ┌─────────┐    ┌─────────┐   ┌─────────┐   ┌─────────┐     │
│  │WORKER   │    │WORKER   │   │WORKER   │   │WORKER   │     │
│  │MINER    │    │BUILDER  │   │HAULER   │   │QUARRY   │     │
│  │         │    │         │   │         │   │         │     │
│  │MICRO AI │    │MICRO AI │   │MICRO AI │   │MICRO AI │     │
│  └────┬────┘    └────┬────┘   └────┬────┘   └────┬────┘     │
│       │              │             │             │           │
│       ▼              ▼             ▼             ▼           │
│  ┌─────────┐    ┌─────────┐   ┌─────────┐   ┌─────────┐     │
│  │"Mine    │    │"Claim   │   │"Move    │   │"Dig     │     │
│  │ stone"  │    │build    │   │items    │   │quarry"  │     │
│  │from     │    │task     │   │between  │   │layer    │     │
│  │blackboard│   │from     │   │chests"  │   │by       │     │
│  │         │    │blackboard│  │         │   │layer"   │     │
│  └─────────┘    └─────────┘   └─────────┘   └─────────┘     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. RTS Pattern Adoption Recommendations

### 3.1 Worker Roles and Specializations

**Recommendation:** Implement role-based worker AI similar to RTS unit types.

```java
// NEW FILE: src/main/java/com/minewright/entity/WorkerRole.java
package com.minewright.entity;

import com.minewright.action.Task;
import com.minewright.coordination.AgentCapability;

import java.util.Set;

/**
 * Worker roles with specialized capabilities and behaviors.
 * Based on RTS unit specializations (SCV, Probe, Drone).
 */
public enum WorkerRole {
    /**
     * Specializes in ore discovery and mining.
     * Idle behavior: Explores caves, searches for exposed ores.
     */
    MINER("Miner", Set.of(
        AgentCapability.MINING,
        AgentCapability.CAVE_EXPLORATION,
        AgentCapability.ORE_DETECTION
    )),

    /**
     * Specializes in structure construction.
     * Idle behavior: Looks for incomplete blueprints, claims build tasks.
     */
    BUILDER("Builder", Set.of(
        AgentCapability.BUILDING,
        AgentCapability.BLOCK_PLACEMENT,
        AgentCapability.BLUEPRINT_INTERPRETATION
    )),

    /**
     * Specializes in bulk resource extraction (quarries, farms).
     * Idle behavior: Returns to assigned work site, continues extraction.
     */
    QUARRY("Quarry Worker", Set.of(
        AgentCapability.BULK_MINING,
        AgentCapability.RESOURCE_PROCESSING
    )),

    /**
     * Specializes in item transport between storage locations.
     * Idle behavior: Finds full chests needing emptying, moves items.
     */
    HAULER("Hauler", Set.of(
        AgentCapability.ITEM_TRANSPORT,
        AgentCapability.CHEST_MANAGEMENT,
        AgentCapability.LOGISTICS
    )),

    /**
     * Generalist worker, can do any task but excels at none.
     * Idle behavior: Waits at foreman location for direct orders.
     */
    GENERAL("General Worker", Set.of(
        AgentCapability.MINING,
        AgentCapability.BUILDING,
        AgentCapability.GATHERING
    ));

    private final String displayName;
    private final Set<AgentCapability> capabilities;

    WorkerRole(String displayName, Set<AgentCapability> capabilities) {
        this.displayName = displayName;
        this.capabilities = capabilities;
    }

    public String getDisplayName() { return displayName; }
    public Set<AgentCapability> getCapabilities() { return capabilities; }

    public boolean canHandle(Task task) {
        return capabilities.stream().anyMatch(cap ->
            cap.matchesAction(task.getAction()));
    }

    /**
     * Get idle behavior for this role.
     * Returns what the worker should do when not assigned tasks.
     */
    public String getIdleBehaviorDescription() {
        return switch (this) {
            case MINER -> "Explore caves, search for ores";
            case BUILDER -> "Look for incomplete structures";
            case QUARRY -> "Return to assigned work site";
            case HAULER -> "Find chests needing transport";
            case GENERAL -> "Wait at foreman location";
        };
    }
}
```

**Implementation Location:** `C:\Users\casey\steve\src\main\java\com\minewright\entity\WorkerRole.java`

### 3.2 Worker Micro AI - Autonomous Behavior

**Recommendation:** Add micro AI to ForemanEntity's tick() method.

```java
// ADD TO: src/main/java/com/minewright/entity/ForemanEntity.java

/**
 * Micro AI behavior tick.
 * Called every game tick to handle autonomous worker behaviors.
 *
 * This implements the RTS pattern where workers have default behaviors
 * when not directly assigned tasks, rather than standing idle.
 */
private void tickMicroAI() {
    // Priority 1: Survival (always, even when assigned tasks)
    if (handleThreats()) {
        return; // Defer everything if in danger
    }

    // Priority 2: Current task execution
    if (hasAssignedTask() && !getCurrentTask().isComplete()) {
        executeCurrentTask();

        // Check for auto-interruptions (inventory full, tool broken)
        handleTaskInterruptions();
        return;
    }

    // Priority 3: Task complete - report and clear
    if (hasAssignedTask() && getCurrentTask().isComplete()) {
        reportTaskCompletion();
        clearCurrentTask();
    }

    // Priority 4: No task - perform idle behavior based on role
    executeIdleBehavior();
}

/**
 * Handles immediate threats (mobs, lava, cactus).
 * Pure survival instinct - no task is worth dying for.
 *
 * @return true if currently handling a threat (should defer other actions)
 */
private boolean handleThreats() {
    // Check for nearby hostile mobs
    Entity nearestHostile = findNearestHostileMob(8.0);
    if (nearestHostile != null) {
        setMicroState(MicroState.FLEEING);

        // Run to safety
        Vec3 safeLocation = findNearestSafeLocation();
        getNavigation().moveTo(safeLocation.x, safeLocation.y, safeLocation.z, 0.8);

        // Notify foreman via communication
        sendMessageToForeman("Fleeing from " + nearestHostile.getName().getString());
        return true;
    }

    // Check for environmental hazards
    if (isInLava() || isTouchingCactus()) {
        setMicroState(MicroState.EMERGENCY_ESCAPE);
        Vec3 safeLocation = findNearestSafeLocation();
        getNavigation().moveTo(safeLocation.x, safeLocation.y, safeLocation.z, 1.0);
        return true;
    }

    return false;
}

/**
 * Executes role-specific idle behavior.
 * This is where workers feel "alive" - they're always doing something relevant.
 */
private void executeIdleBehavior() {
    if (getWorkerRole() == null) {
        return; // No role assigned - wait at spawn
    }

    setMicroState(MicroState.IDLE);

    switch (getWorkerRole()) {
        case MINER -> executeMinerIdleBehavior();
        case BUILDER -> executeBuilderIdleBehavior();
        case QUARRY -> executeQuarryIdleBehavior();
        case HAULER -> executeHaulerIdleBehavior();
        case GENERAL -> executeGeneralIdleBehavior();
    }
}

/**
 * Miner idle behavior: Autonomous ore discovery and mining.
 *
 * When idle, miners should:
 * 1. Check for visible exposed ores
 * 2. If found, navigate and mine
 * 3. If not, explore nearby caves
 * 4. Post discoveries to blackboard for other miners
 */
private void executeMinerIdleBehavior() {
    // Check inventory - deposit if full
    if (getInventory().getFreeSlot() == 0) {
        setMicroState(MicroState.DEPOSITING);
        navigateToNearestStorage();
        if (isAtStorage()) {
            depositAllOres();
        }
        return;
    }

    // Look for exposed ores nearby
    BlockState targetOre = findNearestExposedOre(32.0);
    if (targetOre != null) {
        setMicroState(MicroState.WORKING);

        // Navigate to ore
        BlockPos orePos = getOrePosition(targetOre);
        if (!isAt(orePos)) {
            navigateTo(orePos);
            return;
        }

        // Mine the ore
        equipToolFor(targetOre.getBlock());
        mineBlock(orePos);

        // Post discovery to blackboard
        Blackboard.getInstance().post(
            KnowledgeArea.RESOURCES,
            "ore_" + orePos.getX() + "_" + orePos.getZ(),
            new OreDiscovery(targetOre, orePos),
            getUUID(),
            0.9,
            BlackboardEntry.EntryType.FACT
        );
        return;
    }

    // No ores visible - explore nearby cave
    if (isNearbyCave(16.0)) {
        BlockPos caveEntrance = findNearestCaveEntrance();
        navigateTo(caveEntrance);
        return;
    }

    // No cave - wander randomly looking for ores
    if (getNavigation().isDone()) {
        wanderRandomly(20.0);
    }
}

/**
 * Builder idle behavior: Autonomous structure completion.
 *
 * When idle, builders should:
 * 1. Check blackboard for incomplete blueprints
 * 2. Claim a section if capable
 * 3. Fetch materials from storage
 * 4. Place blocks according to blueprint
 */
private void executeBuilderIdleBehavior() {
    Blackboard blackboard = Blackboard.getInstance();

    // Look for incomplete blueprints on blackboard
    List<BlackboardEntry<BlueprintEntry>> incompleteBlueprints =
        blackboard.queryArea(KnowledgeArea.TASKS).stream()
            .filter(e -> e.getKey().startsWith("blueprint_"))
            .filter(e -> !e.getValue().isComplete())
            .filter(e -> e.getValue().canClaim(this))
            .map(e -> (BlackboardEntry<BlueprintEntry>) e)
            .toList();

    if (incompleteBlueprints.isEmpty()) {
        // No blueprints - wait at foreman
        ForemanEntity foreman = getCrewManager().getForeman();
        if (foreman != null && !isNear(foreman, 5.0)) {
            navigateTo(foreman.blockPosition());
        }
        return;
    }

    // Claim first available blueprint section
    BlackboardEntry<BlueprintEntry> blueprintEntry = incompleteBlueprints.get(0);
    BlueprintEntry blueprint = blueprintEntry.getValue();

    // Claim this section
    if (blueprint.claimSection(this)) {
        setCurrentTask(blueprint.asTask());
        setMicroState(MicroState.WORKING);

        // Notify blackboard of claim
        blackboard.post(
            KnowledgeArea.AGENT_STATUS,
            "worker_" + getEntityName(),
            new WorkerStatus(getEntityName(), WorkerStatus.State.WORKING, blueprint.getSectionId()),
            getUUID(),
            1.0,
            BlackboardEntry.EntryType.FACT
        );
    }
}

/**
 * General worker idle behavior: Wait at foreman location.
 *
 * General workers don't have autonomous specialties.
 * They wait at the foreman's location for direct assignment.
 */
private void executeGeneralIdleBehavior() {
    ForemanEntity foreman = getCrewManager().getForeman();
    if (foreman == null) {
        return; // No foreman - stay put
    }

    // Move to foreman if not nearby
    if (!isNear(foreman, 5.0)) {
        navigateTo(foreman.blockPosition());
    }
}

/**
 * Micro AI states for tracking worker behavior.
 */
public enum MicroState {
    /** No assigned task, performing idle behavior */
    IDLE,

    /** Executing assigned task or autonomous work */
    WORKING,

    /** Fleeing from hostile mob */
    FLEEING,

    /** Emergency escape from environmental hazard */
    EMERGENCY_ESCAPE,

    /** Returning items to storage */
    DEPOSITING,

    /** Fetching materials for building */
    FETCHING_MATERIALS,

    /** Moving to task location */
    TRAVELING
}
```

**Implementation:** Add these methods to `C:\Users\casey\steve\src\main\java\com\minewright\entity\ForemanEntity.java`

### 3.3 Foreman Macro Planning

**Recommendation:** Create a new `ForemanMacroController` class for strategic planning.

```java
// NEW FILE: src/main/java/com/minewright/macro/ForemanMacroController.java
package com.minewright.macro;

import com.minewright.action.Task;
import com.minewright.blackboard.Blackboard;
import com.minewright.blackboard.BlackboardEntry;
import com.minewright.blackboard.KnowledgeArea;
import com.minewright.entity.CrewManager;
import com.minewright.entity.ForemanEntity;
import com.minewright.entity.WorkerRole;
import com.minewright.llm.ResponseParser;

import java.util.*;

/**
 * Foreman macro-level planning and coordination.
 *
 * This class implements the "Foreman as strategic planner" pattern from RTS games.
 * The foreman receives high-level commands from the human player and decomposes
 * them into phases, posting tasks to the blackboard for workers to claim.
 *
 * This is different from OrchestratorService, which does direct task assignment.
 * Macro planning is about creating the right environment for workers to self-organize.
 */
public class ForemanMacroController {

    private final ForemanEntity foreman;
    private final CrewManager crewManager;
    private final Blackboard blackboard;

    // Active macro plans
    private final Map<String, MacroPlan> activePlans;

    public ForemanMacroController(ForemanEntity foreman) {
        this.foreman = foreman;
        this.crewManager = CrewManager.getInstance();
        this.blackboard = Blackboard.getInstance();
        this.activePlans = new HashMap<>();
    }

    /**
     * Process a high-level human command through macro planning.
     *
     * Unlike OrchestratorService.processHumanCommand() which directly assigns tasks,
     * this method creates a strategic plan with phases and posts tasks to the
     * blackboard for workers to claim autonomously.
     *
     * @param parsedResponse LLM parsed response
     * @return Macro plan for tracking
     */
    public MacroPlan planMacroExecution(ResponseParser.ParsedResponse parsedResponse) {
        String planId = "macro_" + UUID.randomUUID().toString().substring(0, 8);

        // Step 1: Analyze resource requirements
        ResourceAnalysis resources = analyzeResourceNeeds(parsedResponse);

        // Step 2: Create macro plan with phases
        MacroPlan plan = createMacroPlan(planId, parsedResponse, resources);

        // Step 3: Post phase 1 tasks to blackboard
        postPhaseTasks(plan, 0);

        // Step 4: Announce plan to crew
        announcePlan(plan);

        activePlans.put(planId, plan);

        return plan;
    }

    /**
     * Analyze the material needs for a plan.
     * The foreman is responsible for ensuring adequate resources.
     */
    private ResourceAnalysis analyzeResourceNeeds(ResponseParser.ParsedResponse response) {
        ResourceAnalysis analysis = new ResourceAnalysis();

        for (Task task : response.getTasks()) {
            String action = task.getAction();

            // Estimate material requirements based on action
            if (action.equals("build") || action.equals("construct")) {
                String blockType = task.getStringParameter("block", "stone");
                int quantity = task.getIntParameter("quantity", 1);
                analysis.addMaterial(blockType, quantity);
            } else if (action.equals("mine") || action.equals("gather")) {
                String resource = task.getStringParameter("resource", "stone");
                int quantity = task.getIntParameter("quantity", 64);
                analysis.addProduction(resource, quantity);
            }
        }

        return analysis;
    }

    /**
     * Create a macro plan with phases and dependencies.
     *
     * RTS-style planning breaks complex commands into sequential phases:
     * - Phase 0: Resource gathering
     * - Phase 1: Site preparation
     * - Phase 2: Construction
     * - Phase 3: Finishing
     */
    private MacroPlan createMacroPlan(String planId, ResponseParser.ParsedResponse response,
                                     ResourceAnalysis resources) {
        MacroPlan plan = new MacroPlan(planId, response.getPlan());

        // Phase 0: Resource gathering (if needed)
        if (resources.hasNeeds()) {
            MacroPhase gatheringPhase = new MacroPhase(
                "Gather Materials",
                0,
                createGatheringTasks(resources),
                Collections.emptyList() // No dependencies
            );
            plan.addPhase(gatheringPhase);
        }

        // Phase 1: Site preparation
        List<Task> prepTasks = findSitePreparationTasks(response);
        if (!prepTasks.isEmpty()) {
            MacroPhase prepPhase = new MacroPhase(
                "Prepare Site",
                1,
                prepTasks,
                resources.hasNeeds() ? List.of(0) : Collections.emptyList()
            );
            plan.addPhase(prepPhase);
        }

        // Phase 2: Main construction
        List<Task> buildTasks = filterBuildTasks(response.getTasks());
        if (!buildTasks.isEmpty()) {
            MacroPhase buildPhase = new MacroPhase(
                "Build Structure",
                2,
                buildTasks,
                List.of(1) // Depends on site prep
            );
            plan.addPhase(buildPhase);
        }

        return plan;
    }

    /**
     * Post tasks for a phase to the blackboard.
     * Workers will discover and claim these tasks autonomously.
     */
    private void postPhaseTasks(MacroPlan plan, int phaseIndex) {
        MacroPhase phase = plan.getPhases().get(phaseIndex);

        for (Task task : phase.getTasks()) {
            String taskId = plan.getPlanId() + "_phase" + phaseIndex + "_" +
                           phase.getTasks().indexOf(task);

            // Determine which role should prioritize this task
            WorkerRole preferredRole = determinePreferredRole(task);

            // Post to blackboard
            BlackboardEntry<Task> entry = BlackboardEntry.create(
                taskId,
                task,
                foreman.getUUID(),
                1.0,
                BlackboardEntry.EntryType.GOAL
            );

            blackboard.post(KnowledgeArea.TASKS, entry);

            // Log for debugging
            System.out.println("[ForemanMacro] Posted task to blackboard: " +
                task.getAction() + " (preferred: " + preferredRole + ")");
        }
    }

    /**
     * Announce the plan to all workers.
     * This uses AgentCommunicationBus for crew-wide notification.
     */
    private void announcePlan(MacroPlan plan) {
        String announcement = String.format(
            "New plan: %s. %d phases. Starting with: %s",
            plan.getDescription(),
            plan.getPhases().size(),
            plan.getPhases().get(0).getName()
        );

        foreman.say(announcement);

        // Also broadcast via communication bus
        // ...
    }

    /**
     * Determine which worker role should handle a task.
     */
    private WorkerRole determinePreferredRole(Task task) {
        String action = task.getAction();

        return switch (action) {
            case "mine", "gather" -> WorkerRole.MINER;
            case "build", "construct" -> WorkerRole.BUILDER;
            case "move", "transport" -> WorkerRole.HAULER;
            default -> WorkerRole.GENERAL;
        };
    }

    /**
     * Check if a phase is complete and advance to next phase.
     * Called periodically by ForemanEntity.tick()
     */
    public void checkPhaseProgress(MacroPlan plan) {
        if (plan.isComplete()) {
            return;
        }

        MacroPhase currentPhase = plan.getCurrentPhase();

        // Check if all tasks in current phase are complete
        boolean phaseComplete = currentPhase.getTasks().stream()
            .allMatch(task -> isTaskComplete(task, plan.getPlanId()));

        if (phaseComplete) {
            advanceToNextPhase(plan);
        }
    }

    /**
     * Advance to the next phase of the macro plan.
     */
    private void advanceToNextPhase(MacroPlan plan) {
        int nextPhaseIndex = plan.getCurrentPhaseIndex() + 1;

        if (nextPhaseIndex < plan.getPhases().size()) {
            plan.setCurrentPhaseIndex(nextPhaseIndex);

            // Post next phase tasks
            postPhaseTasks(plan, nextPhaseIndex);

            // Announce phase transition
            MacroPhase nextPhase = plan.getPhases().get(nextPhaseIndex);
            foreman.say("Phase complete. Starting: " + nextPhase.getName());
        } else {
            // All phases complete
            plan.setComplete(true);
            foreman.say("Plan complete: " + plan.getDescription());
        }
    }

    /**
     * Check if a specific task is complete.
     */
    private boolean isTaskComplete(Task task, String planId) {
        // Query blackboard for task completion status
        // This would check AGENT_STATUS area for completion reports
        return false; // Placeholder
    }

    /**
     * Resource analysis for macro planning.
     */
    public static class ResourceAnalysis {
        private final Map<String, Integer> materialNeeds = new HashMap<>();
        private final Map<String, Integer> productionTargets = new HashMap<>();

        public void addMaterial(String type, int quantity) {
            materialNeeds.merge(type, quantity, Integer::sum);
        }

        public void addProduction(String type, int quantity) {
            productionTargets.merge(type, quantity, Integer::sum);
        }

        public boolean hasNeeds() {
            return !materialNeeds.isEmpty();
        }

        public Map<String, Integer> getMaterialNeeds() {
            return Collections.unmodifiableMap(materialNeeds);
        }
    }

    /**
     * A macro-level plan with multiple phases.
     */
    public static class MacroPlan {
        private final String planId;
        private final String description;
        private final List<MacroPhase> phases;
        private int currentPhaseIndex = 0;
        private boolean complete = false;

        public MacroPlan(String planId, String description) {
            this.planId = planId;
            this.description = description;
            this.phases = new ArrayList<>();
        }

        public void addPhase(MacroPhase phase) {
            phases.add(phase);
        }

        public String getPlanId() { return planId; }
        public String getDescription() { return description; }
        public List<MacroPhase> getPhases() { return phases; }
        public MacroPhase getCurrentPhase() { return phases.get(currentPhaseIndex); }
        public int getCurrentPhaseIndex() { return currentPhaseIndex; }
        public void setCurrentPhaseIndex(int index) { this.currentPhaseIndex = index; }
        public boolean isComplete() { return complete; }
        public void setComplete(boolean complete) { this.complete = complete; }
    }

    /**
     * A phase of a macro plan.
     */
    public static class MacroPhase {
        private final String name;
        private final int phaseIndex;
        private final List<Task> tasks;
        private final List<Integer> dependencies; // Phase indices this depends on

        public MacroPhase(String name, int phaseIndex, List<Task> tasks,
                         List<Integer> dependencies) {
            this.name = name;
            this.phaseIndex = phaseIndex;
            this.tasks = tasks;
            this.dependencies = dependencies;
        }

        public String getName() { return name; }
        public int getPhaseIndex() { return phaseIndex; }
        public List<Task> getTasks() { return tasks; }
        public List<Integer> getDependencies() { return dependencies; }
    }
}
```

**Implementation Location:** `C:\Users\casey\steve\src\main\java\com\minewright\macro\ForemanMacroController.java`

---

## 4. Communication Improvements

### 4.1 Current Limitations

1. **No agent-to-agent chat:** Workers can't talk to each other directly
2. **Foreman as only interface:** All communication flows through foreman
3. **No conversation context:** Messages don't reference previous discussions
4. **No personality filtering:** All agents sound the same

### 4.2 Recommended Agent-to-Agent Messaging

```java
// ADD TO: src/main/java/com/minewright/communication/AgentMessage.java

/**
 * Additional message types for agent-to-agent coordination.
 */
public enum Type {
    // Existing types...
    TASK_ASSIGNMENT,
    TASK_PROGRESS,
    TASK_COMPLETE,
    TASK_FAILED,
    HELP_REQUEST,
    STATUS_REPORT,
    PLAN_ANNOUNCEMENT,
    BROADCAST,

    // NEW: Agent-to-agent communication types

    /**
     * Worker discovery announcement.
     * Example: "Found diamonds at 100, 64, -200"
     */
    DISCOVERY_ANNOUNCEMENT,

    /**
     * Worker coordination request.
     * Example: "Need help with east wall"
     */
    COORDINATION_REQUEST,

    /**
     * Worker coordination offer/response.
     * Example: "I can help", "On my way"
     */
    COORDINATION_OFFER,

    /**
     * Worker-to-worker banter/chat.
     * Example: "Race you to the corner!"
     */
    BANTER,

    /**
     * Worker encouragement.
     * Example: "Almost there!", "Looking good!"
     */
    ENCOURAGEMENT,

    /**
     * Team celebration.
     * Example: "Section complete!", "New record!"
     */
    CELEBRATION,

    /**
     * Warning broadcast.
     * Example: "Creeper nearby!", "Lava above!"
     */
    WARNING
}
```

### 4.3 Shared Conversation Context

```java
// NEW FILE: src/main/java/com/minewright/communication/SharedConversationContext.java
package com.minewright.communication;

import java.time.Instant;
import java.util.*;

/**
 * Shared context for agent conversations.
 *
 * This prevents repetitive conversations and enables contextual responses.
 * All agents can see what's been discussed recently.
 */
public class SharedConversationContext {

    private static final SharedConversationContext INSTANCE =
        new SharedConversationContext();

    private final Map<String, ConversationTopic> recentTopics =
        new ConcurrentHashMap<>();
    private final Set<String> exhaustedTopics = ConcurrentHashMap.newKeySet();
    private final Queue<String> recentComments = new ConcurrentLinkedQueue<>();

    public static SharedConversationContext getInstance() {
        return INSTANCE;
    }

    /**
     * Record a comment made by an agent.
     */
    public void recordComment(String agentName, String topic, String content) {
        ConversationTopic conversationTopic = new ConversationTopic(
            agentName, topic, content, Instant.now()
        );

        recentTopics.put(topic, conversationTopic);
        recentComments.offer(content);
        if (recentComments.size() > 20) {
            recentComments.poll();
        }

        // Mark topic as recently discussed
        exhaustedTopics.add(topic);

        // Clean up old topics after 5 minutes
        cleanupOldTopics();
    }

    /**
     * Check if a topic has been discussed recently.
     */
    public boolean isTopicExhausted(String topic) {
        return exhaustedTopics.contains(topic);
    }

    /**
     * Allow topics to be discussed again.
     * Call this periodically to refresh conversation topics.
     */
    public void refreshTopics() {
        exhaustedTopics.clear();
    }

    /**
     * Get recent comments for context.
     */
    public List<String> getRecentComments(int count) {
        List<String> result = new ArrayList<>();
        Iterator<String> iterator = recentComments.iterator();
        while (iterator.hasNext() && result.size() < count) {
            result.add(iterator.next());
        }
        return result;
    }

    private void cleanupOldTopics() {
        Instant fiveMinutesAgo = Instant.now().minusSeconds(300);
        recentTopics.entrySet().removeIf(entry ->
            entry.getValue().timestamp.isBefore(fiveMinutesAgo));
    }

    public static class ConversationTopic {
        public final String agentName;
        public final String topic;
        public final String content;
        public final Instant timestamp;

        public ConversationTopic(String agentName, String topic,
                                String content, Instant timestamp) {
            this.agentName = agentName;
            this.topic = topic;
            this.content = content;
            this.timestamp = timestamp;
        }
    }
}
```

### 4.4 Personality-Driven Dialogue Filtering

```java
// NEW FILE: src/main/java/com/minewright/communication/PersonalityDialogueFilter.java
package com.minewright.communication;

/**
 * Applies personality traits to dialogue.
 * Makes each agent have a unique voice.
 */
public class PersonalityDialogueFilter {

    /**
     * Apply personality traits to a message.
     */
    public static String applyPersonality(String baseMessage,
                                          PersonalityProfile personality) {
        String filtered = baseMessage;

        // Apply verbal tics
        if (personality.shouldUseVerbalTic()) {
            String tic = personality.getRandomVerbalTic();
            filtered = tic + " " + filtered;
        }

        // Adjust formality
        if (personality.formality > 70) {
            filtered = formalize(filtered);
        } else if (personality.formality < 30) {
            filtered = casualize(filtered);
        }

        // Adjust enthusiasm based on extraversion
        if (personality.extraversion > 70 && !filtered.endsWith("!")) {
            filtered = filtered + "!";
        }

        return filtered;
    }

    private static String casualize(String message) {
        return message
            .replace("I am", "I'm")
            .replace("you are", "you're")
            .replace("cannot", "can't")
            .replace("do not", "don't")
            .replace("Yes", "Yep")
            .replace("No", "Nope");
    }

    private static String formalize(String message) {
        return message
            .replace("I'm", "I am")
            .replace("you're", "you are")
            .replace("can't", "cannot")
            .replace("don't", "do not")
            .replace("Yep", "Yes")
            .replace("Nope", "No");
    }

    /**
     * Personality profile for dialogue customization.
     */
    public static class PersonalityProfile {
        public int openness;        // Creativity, adaptability
        public int conscientiousness; // Organization, responsibility
        public int extraversion;    // Sociability, enthusiasm
        public int agreeableness;   // Collaboration, helpfulness
        public int neuroticism;     // Anxiety, nervousness
        public int humor;           // Wit, joking
        public int encouragement;   // Supportiveness
        public int formality;       // Formal vs casual speech

        public boolean shouldUseVerbalTic() {
            return humor > 70 || neuroticism > 60;
        }

        public String getRandomVerbalTic() {
            if (humor > 70) {
                return List.of("Hey,", "So,", "Well,", "Listen,")
                    .get(new Random().nextInt(4));
            }
            if (neuroticism > 60) {
                return List.of("Um,", "Uh,", "I think,", "Maybe,")
                    .get(new Random().nextInt(4));
            }
            return "";
        }
    }
}
```

---

## 5. Script Coordination

### 5.1 Current Script System

MineWright doesn't currently have a script system, but the research mentions script coordination. Here's how scripts should fit into the coordination architecture:

**Recommendation:** Scripts are high-level behavior templates that workers can execute autonomously.

```
Script Examples:
- "quarry_layer.dig": Dig a 3x3x3 hole, place torches, collect drops
- "wall_section.build": Place blocks in a line pattern, stop when obstructed
- "farm_row.tend": Plant seeds, bonemeal, harvest when grown

Script Execution Flow:
1. Foreman posts script task to blackboard: "Execute quarry_layer.dig at (100, 64, 200)"
2. Miner worker claims task (matches role)
3. Worker loads script, executes tick-by-tick
4. Worker reports progress and completion
```

### 5.2 Script System Recommendation

```java
// NEW FILE: src/main/java/com/minewright/script/Script.java
package com.minewright.script;

import com.minewright.entity.ForemanEntity;

/**
 * A script is a reusable behavior template that workers can execute.
 *
 * Scripts are like mini-programs that define autonomous behaviors.
 * They differ from tasks in that they're pre-defined templates rather
 * than LLM-generated actions.
 */
public interface Script {

    /**
     * Called when the script starts.
     * Initialize any state needed for execution.
     */
    void onStart(ForemanEntity worker);

    /**
     * Called every game tick.
     * Execute one step of the script.
     *
     * @return true if script is complete, false if still running
     */
    boolean tick(ForemanEntity worker);

    /**
     * Called if the script is cancelled.
     * Clean up any resources or state.
     */
    void onCancel(ForemanEntity worker);

    /**
     * Get the current progress of this script (0.0 to 1.0).
     */
    double getProgress();

    /**
     * Get a human-readable description of what this script does.
     */
    String getDescription();
}
```

---

## 6. Mace's Role as Foreman

### 6.1 Current Situation

**Current Behavior:**
- Mace is just another `ForemanEntity` instance
- No special foreman capabilities
- Same role as any worker

**Problem:** Mace should be the "face" of the crew, not just another worker.

### 6.2 Recommended Foreman Enhancements

```java
// NEW FILE: src/main/java/com/minewright/entity/ForemanCapabilities.java
package com.minewright.entity;

/**
 * Capabilities unique to the foreman role.
 *
 * Only one agent should have these capabilities at a time.
 * This is what makes Mace special compared to workers.
 */
public interface ForemanCapabilities {

    /**
     * Plan macro-level execution of a human command.
     * This is the strategic layer - breaking commands into phases.
     */
    MacroPlan planMacroExecution(ResponseParser.ParsedResponse command);

    /**
     * Facilitate worker coordination.
     * When workers need to coordinate, the foreman mediates.
     */
    void facilitateCoordination(String topic, List<ForemanEntity> participants);

    /**
     * Report aggregate progress to the human player.
     * The foreman is the interface - workers don't talk to players directly.
     */
    void reportAggregateProgress(String planId);

    /**
     * Translate player intent to worker tasks.
     * Convert human natural language into worker-understandable commands.
     */
    void translateAndDelegate(String playerCommand, List<ForemanEntity> workers);

    /**
     * Relay worker messages to the player.
     * Important worker discoveries should be shared with the player.
     */
    void relayWorkerMessage(AgentMessage workerMessage);

    /**
     * Mediate worker disagreements.
     * When workers have conflicting goals, foreman decides.
     */
    void mediateDisagreement(ForemanEntity worker1, ForemanEntity worker2,
                           String issue);
}
```

### 6.3 Mace-Specific Behaviors

**Personality:** The Foreman
- Conscientious: 90 (organized, responsible)
- Extraversion: 80 (outspoken leader)
- Agreeableness: 70 (collaborative)
- Neuroticism: 30 (calm under pressure)
- Humor: 60 (occasionally witty)
- Encouragement: 85 (very supportive)
- Formality: 40 (casual but professional)

**Example Dialogue (Mace):**
- Plan announcement: "Alright team, here's the plan: we're building a starter house. Worker1, you're on the frame. Worker2, handle the flooring. I'll take the roof. Let's make it happen!"
- Progress update: "25% complete. Foundation's solid, materials flowing well."
- Worker coordination: "Worker2, can you spare some cobble for Worker1?"
- Celebration: "That's a wrap! Beautiful work team. Player's gonna love this."

---

## 7. Code Recommendations Summary

### 7.1 Immediate Changes (Week 1-2)

1. **Add WorkerRole enum** to `ForemanEntity`
   - Add `private WorkerRole role;` field
   - Add role to constructor parameters
   - Store role in agent data for persistence

2. **Add micro AI tick** to `ForemanEntity.tick()`
   - Call `tickMicroAI()` before `actionExecutor.tick()`
   - Implement `handleThreats()` for survival
   - Implement `executeIdleBehavior()` for role-based actions

3. **Create `ForemanMacroController`**
   - Implement macro planning with phases
   - Post tasks to blackboard instead of direct assignment
   - Announce plans to crew

4. **Add agent-to-agent message types**
   - Extend `AgentMessage.Type` enum
   - Add `DISCOVERY_ANNOUNCEMENT`, `COORDINATION_REQUEST`, `BANTER`, etc.

### 7.2 Medium-Term Changes (Week 3-4)

5. **Implement `SharedConversationContext`**
   - Track recent conversation topics
   - Prevent repetitive discussions
   - Enable contextual responses

6. **Add `PersonalityDialogueFilter`**
   - Create personality profiles for Mace and workers
   - Apply verbal tics and formality adjustments
   - Make each agent sound unique

7. **Enhance `OrchestratorService` integration**
   - Use `ForemanMacroController` for strategic planning
   - Keep `OrchestratorService` for tactical coordination
   - Post tasks to blackboard for decentralized claiming

### 7.3 Long-Term Changes (Week 5-6)

8. **Implement script system**
   - Create `Script` interface
   - Add built-in scripts (quarry, wall, farm)
   - Allow workers to execute scripts autonomously

9. **Add Mace-specific foreman UI**
   - Show macro plan progress
   - Display worker status and locations
   - Enable player to intervene in coordination

10. **Polish autonomous behaviors**
    - Tune miner cave exploration
    - Optimize builder blueprint claiming
    - Balance hauler logistics

---

## 8. Testing Recommendations

### 8.1 Unit Tests

```java
// TEST FILE: src/test/java/com/minewright/entity/WorkerMicroAITest.java

@Test
public void testMinerIdleBehavior_CavesExplored() {
    // Create a miner worker
    ForemanEntity miner = spawnTestWorker(WorkerRole.MINER);

    // Place exposed ore nearby
    placeOre(BlockPos.ZERO, Blocks.DIAMOND_ORE);

    // Tick micro AI
    miner.tickMicroAI();

    // Verify miner navigates to ore
    assertHeadingTowards(miner, BlockPos.ZERO);
}

@Test
public void testWorkerFleesFromThreats() {
    // Create a worker
    ForemanEntity worker = spawnTestWorker(WorkerRole.BUILDER);

    // Spawn creeper nearby
    spawnCreeper(worker.blockPosition().offset(5, 0, 0));

    // Tick micro AI
    worker.tickMicroAI();

    // Verify worker flees
    assertMovingAway(worker, creeper.getPosition());
}
```

### 8.2 Integration Tests

```java
// TEST FILE: src/test/java/com/minewright/macro/ForemanMacroControllerTest.java

@Test
public void testMacroPlanning_PhasesExecuted() {
    // Create foreman with macro controller
    ForemanEntity foreman = spawnTestForeman();
    ForemanMacroController macro = new ForemanMacroController(foreman);

    // Process "build a house" command
    ResponseParser.ParsedResponse response = parseCommand("build a stone house");
    MacroPlan plan = macro.planMacroExecution(response);

    // Verify phase 0 tasks posted to blackboard
    List<BlackboardEntry<?>> tasks = Blackboard.getInstance()
        .queryArea(KnowledgeArea.TASKS);

    assertFalse(tasks.isEmpty(), "Should have tasks on blackboard");
    assertTrue(tasks.get(0).getKey().startsWith(plan.getPlanId()));
}

@Test
public void testWorkerAutoClaimsTasks() {
    // Post tasks to blackboard
    Task buildTask = new Task("build", Map.of("block", "stone"));
    Blackboard.getInstance().post(KnowledgeArea.TASKS,
        BlackboardEntry.create("build_wall", buildTask, foremanId, 1.0,
                             BlackboardEntry.EntryType.GOAL));

    // Spawn builder worker
    ForemanEntity builder = spawnTestWorker(WorkerRole.BUILDER);

    // Tick micro AI
    builder.tickMicroAI();

    // Verify worker claimed task
    assertNotNull(builder.getCurrentTask());
    assertEquals("build", builder.getCurrentTask().getAction());
}
```

---

## 9. Performance Considerations

### 9.1 Tick-Based Optimization

**Current:** All logic in `ForemanEntity.tick()` happens every 20ms.

**Recommendation:** Use staggered updates for different systems.

```java
// ADD TO: ForemanEntity.java

private int tickCounter = 0;

@Override
public void tick() {
    tickCounter++;

    // Every tick: Survival, current task execution
    tickMicroAI();

    // Every 5 ticks (100ms): Check for new tasks on blackboard
    if (tickCounter % 5 == 0) {
        checkBlackboardForTasks();
    }

    // Every 20 ticks (400ms): Update conversation
    if (tickCounter % 20 == 0) {
        updateConversation();
    }

    // Every 100 ticks (2 seconds): Report status to foreman
    if (tickCounter % 100 == 0) {
        reportStatusToForeman();
    }
}
```

### 9.2 Blackboard Query Optimization

**Current:** `queryArea()` scans all entries in a knowledge area.

**Recommendation:** Use indexed queries for frequent lookups.

```java
// OPTIMIZATION: Add indexes to Blackboard

private final Map<KnowledgeArea, Map<String, BlackboardEntry<?>>> areas;
private final Map<KnowledgeArea, Map<Object, List<String>>> indexes; // NEW

public <T> void post(KnowledgeArea area, BlackboardEntry<T> entry) {
    // ... existing code ...

    // Update indexes
    Map<Object, List<String>> areaIndexes = indexes.get(area);
    Object indexKey = getIndexKey(entry); // e.g., entry.getSourceAgent()
    areaIndexes.computeIfAbsent(indexKey, k -> new ArrayList<>())
               .add(entry.getKey());
}

public <T> List<BlackboardEntry<T>> queryBySource(KnowledgeArea area, UUID source) {
    // Use index instead of scanning
    Map<Object, List<String>> areaIndexes = indexes.get(area);
    List<String> keys = areaIndexes.get(source);

    if (keys == null) return Collections.emptyList();

    return keys.stream()
        .map(key -> areas.get(area).get(key))
        .map(entry -> (BlackboardEntry<T>) entry)
        .toList();
}
```

---

## 10. Conclusion

### 10.1 Key Takeaways

1. **MineWright has excellent infrastructure** - Contract Net, OrchestratorService, Blackboard, CommunicationBus are all production-ready.

2. **Missing macro/micro separation** - Workers wait for assignments instead of having autonomous behaviors.

3. **Foreman needs strategic role** - Mace should be a "face" that plans macro execution, not just a task router.

4. **Agent-to-agent communication** - Workers should talk to each other, not just through the foreman.

5. **Personality-driven dialogue** - Each agent should have a unique voice based on traits.

### 10.2 Implementation Priority

**Phase 1 (Week 1-2): Worker Autonomy**
- Add `WorkerRole` enum
- Implement `tickMicroAI()` with survival and idle behaviors
- Add miner cave exploration and builder blueprint claiming

**Phase 2 (Week 3-4): Macro Planning**
- Create `ForemanMacroController`
- Implement phase-based planning with blackboard task posting
- Add plan progress tracking and phase transitions

**Phase 3 (Week 5-6): Communication & Personality**
- Implement `SharedConversationContext`
- Add `PersonalityDialogueFilter`
- Enable agent-to-agent messaging with new message types

**Phase 4 (Week 7-8): Polish & Testing**
- Tune autonomous behaviors
- Add Mace-specific foreman UI
- Comprehensive testing of coordination scenarios

### 10.3 Success Metrics

**Quantitative:**
- Workers spend < 10% of time idle (standing still)
- Task completion time improves by 30% (parallel execution)
- Player intervention reduces by 50% (autonomous problem-solving)

**Qualitative:**
- Agents feel like "fellow players" not bots
- Mace feels like a competent foreman
- Coordination feels natural and emergent
- Conversations are varied and context-aware

---

**Document Version:** 1.0
**Last Updated:** 2026-02-28
**Next Review:** After Phase 1 implementation (Week 2)
