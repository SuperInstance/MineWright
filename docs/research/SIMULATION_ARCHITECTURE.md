# Mental Simulation Architecture for MineWright

**Version:** 1.0
**Date:** 2026-02-27
**Status:** Design Proposal
**Author:** Architecture Team

---

## Executive Summary

This document outlines the architecture for integrating mental simulation capabilities into MineWright, enabling AI agents to "imagine" action outcomes before executing them in the real Minecraft world. The simulation system leverages a persistent Python kernel (via Jupyter Kernel Gateway) for flexible physics simulation, risk assessment, and planning validation.

### Key Benefits

- **Risk Mitigation:** Preview dangerous actions (lava, falling, mob encounters) before execution
- **Resource Optimization:** Validate building plans and material requirements virtually
- **Intelligent Fallback:** Generate alternative approaches when initial plans fail
- **Learning:** Store simulation outcomes for improved future planning

---

## 1. High-Level Architecture

### 1.1 System Overview

```
+---------------------------------------------------------------------+
|                         MineWright Mod (Java)                       |
+---------------------------------------------------------------------+
                              |
                              | HTTP/WebSocket
                              v
+---------------------------------------------------------------------+
|                    Jupyter Kernel Gateway                           |
|  (Persistent Python Kernels for Simulation Execution)               |
+---------------------------------------------------------------------+
                              |
        +---------------------+---------------------+
        |                     |                     |
        v                     v                     v
+---------------+    +---------------+    +---------------+
| Physics Kernel|    |  World Kernel |    | Combat Kernel|
| - Block       |    | - Voxel Map   |    | - Mob AI      |
|   mechanics   |    | - Chunk Load  |    | - Damage calc |
| - Gravity     |    | - Biome Data  |    | - Pathfinding |
| - Fluid Flow  |    | - Block States|    | - Threat eval |
+---------------+    +---------------+    +---------------+
        |                     |                     |
        +---------------------+---------------------+
                              |
                              v
                    +-------------------+
                    |  Simulation Store |
                    |  - Outcome Cache  |
                    |  - Risk Database  |
                    |  - Plan History   |
                    +-------------------+
```

### 1.2 Component Relationships

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Agent Decision Flow                        │
└─────────────────────────────────────────────────────────────────────┘

    User Command
           │
           v
    ┌──────────────┐
    │ TaskPlanner  │ ───────> Generate initial task sequence
    └──────────────┘
           │
           v
    ┌──────────────┐
    │ Action       │ ───────> Before executing, check simulation
    │ Executor     │
    └──────────────┘
           │
           v
    ┌──────────────────────────────────────────────────────┐
    │              Simulation Interceptor                  │
    │  (New interceptor in existing InterceptorChain)      │
    └──────────────────────────────────────────────────────┘
           │
           ├─────────────────┬─────────────────┬────────────┐
           v                 v                 v            v
    ┌──────────┐     ┌──────────┐     ┌──────────┐  ┌──────────┐
    │ Voxel    │     │ Risk     │     │ Resource │  │ Combat   │
    │ Snapshot │     │ Analysis │     │ Check    │  │ Sim      │
    └──────────┘     └──────────┘     └──────────┘  └──────────┘
           │                 │                 │            │
           └─────────────────┴─────────────────┴────────────┘
                             │
                             v
                    ┌────────────────┐
                    │ Kernel Gateway │ ───> Python simulation
                    └────────────────┘
                             │
                             v
                    ┌────────────────┐
                    │  Simulation    │
                    │  Result        │
                    │  - Success/Fail│
                    │  - Risk Score  │
                    │  - Alt Plans   │
                    └────────────────┘
                             │
                             v
                    ┌────────────────┐
                    │  Decision      │
                    │  - Proceed     │
                    │  - Modify      │
                    │  - Abort       │
                    └────────────────┘
```

---

## 2. Core Components

### 2.1 Simulation Interceptor

**Package:** `com.minewright.simulation.interceptor`

**Purpose:** Pre-execution hook that intercepts actions and runs simulations before real-world execution.

**Key Features:**
- Integrate into existing `InterceptorChain`
- Configurable simulation depth (quick check vs. deep analysis)
- Caching of simulation results
- Async simulation execution to avoid blocking game thread

**Integration Point:**
```java
// In ActionExecutor constructor
interceptorChain.addInterceptor(new SimulationInterceptor(
    kernelGateway,
    simulationConfig,
    cache
));
```

**Responsibilities:**
1. Extract action parameters (position, block type, quantity)
2. Create voxel snapshot of relevant area
3. Dispatch simulation request to kernel gateway
4. Parse simulation result
5. Decide: proceed, modify, or abort action
6. Publish simulation events for monitoring

### 2.2 Voxel World Memory

**Package:** `com.minewright.simulation.world`

**Purpose:** Maintain a lightweight, queryable representation of the Minecraft world for simulation.

**Key Features:**
- Chunk-based voxel storage (similar to Minecraft but optimized for queries)
- Efficient spatial indexing (3D sparse octree or chunk hashmap)
- Delta tracking (what changed since last snapshot)
- Lazy loading (only simulate relevant areas)

**Data Structure:**
```java
public class VoxelWorldSnapshot {
    private final String worldId;
    private final long timestamp;
    private final Map<ChunkPos, VoxelChunk> chunks;
    private final BlockPos origin;
    private final int radius;

    public BlockState getBlockState(BlockPos pos);
    public List<BlockPos> findBlocks(Block block, int searchRadius);
    public boolean isSafe(BlockPos pos);  // Check for hazards
    public boolean canPlace(BlockPos pos, Block block);
    public boolean canReach(BlockPos from, BlockPos to);
}
```

**Integration with Existing WorldKnowledge:**
```java
// Extend WorldKnowledge to provide voxel snapshots
public class WorldKnowledge {
    public VoxelWorldSnapshot createSnapshot(int radius) {
        // Efficiently scan and cache voxels
    }

    public VoxelWorldSnapshot createSnapshotForAction(Task task) {
        // Create minimal snapshot needed for specific action
    }
}
```

### 2.3 Kernel Gateway Client

**Package:** `com.minewright.simulation.gateway`

**Purpose:** HTTP/WebSocket client for communicating with Jupyter Kernel Gateway.

**Key Features:**
- Connection pooling (multiple kernels for parallel simulations)
- Request queuing and throttling
- Kernel health monitoring and auto-restart
- Support for both sync and async requests

**API Interface:**
```java
public interface KernelGatewayClient {
    // Spawn new kernel
    CompletableFuture<String> spawnKernel(String kernelType);

    // Execute simulation code
    CompletableFuture<SimulationResult> executeSimulation(
        String kernelId,
        SimulationRequest request
    );

    // Stream simulation results (for long-running sims)
    void executeSimulationStream(
        String kernelId,
        SimulationRequest request,
        Consumer<SimulationEvent> callback
    );

    // Terminate kernel
    void terminateKernel(String kernelId);

    // Health check
    boolean isKernelHealthy(String kernelId);
}
```

**Simulation Request Format (JSON):**
```json
{
    "type": "block_placement",
    "worldSnapshot": {
        "chunks": {/* voxel data */},
        "origin": [x, y, z],
        "radius": 16
    },
    "action": {
        "type": "place",
        "block": "minecraft:lava",
        "position": [x, y, z],
        "entity": {"position": [x, y, z], "inventory": []}
    },
    "options": {
        "maxTicks": 100,
        "checkCollisions": true,
        "checkHazards": true,
        "predictMobBehavior": true
    }
}
```

**Simulation Result Format (JSON):**
```json
{
    "success": false,
    "riskScore": 0.95,
    "failureReason": "lava_will_spread_and_kill_agent",
    "predictedOutcome": {
        "agentHealth": 0,
        "blocksAffected": 45,
        "entitiesThreatened": ["agent"]
    },
    "alternatives": [
        {
            "action": "place",
            "block": "minecraft:stone",
            "position": [x, y, z],
            "riskScore": 0.0
        }
    ],
    "ticksSimulated": 47
}
```

### 2.4 Risk Assessment Engine

**Package:** `com.minewright.simulation.risk`

**Purpose:** Analyze simulation results and provide actionable risk assessments.

**Risk Categories:**
1. **Lethal Risk:** Agent will die
2. **High Risk:** Significant damage, resource loss
3. **Medium Risk:** Minor damage, plan interruption
4. **Low Risk:** Recoverable issues
5. **No Risk:** Safe to proceed

**Configuration:**
```java
public class RiskAssessmentConfig {
    private double lethalThreshold = 0.8;      // Abort if >=
    private double highRiskThreshold = 0.5;    // Require confirmation
    private double mediumRiskThreshold = 0.2;  // Warn but proceed
    private boolean requireUserApprovalForHighRisk = true;
    private boolean autoGenerateAlternatives = true;
    private int maxAlternativePlans = 3;
}
```

**Decision Logic:**
```java
public enum SimulationDecision {
    PROCEED,           // Action is safe
    PROCEED_WITH_WARNING, // Minor risks, notify user
    REQUIRE_CONFIRMATION, // Moderate risks, ask user
    MODIFY_ACTION,     // Use simulation's suggested alternative
    ABORT              // Too dangerous, stop
}

public SimulationDecision decide(
    SimulationResult result,
    Task originalTask,
    RiskAssessmentConfig config
) {
    // Analyze risk score and context
    // Return appropriate decision
}
```

### 2.5 Simulation Cache

**Package:** `com.minewright.simulation.cache`

**Purpose:** Cache simulation results to avoid redundant computations.

**Cache Key Components:**
- World state hash (of relevant area)
- Action type and parameters
- Simulation options

**Implementation:**
```java
public class SimulationCache {
    private final Cache<CacheKey, SimulationResult> cache;

    public Optional<SimulationResult> getIfValid(
        VoxelWorldSnapshot world,
        Task task,
        SimulationOptions options
    );

    public void put(
        VoxelWorldSnapshot world,
        Task task,
        SimulationOptions options,
        SimulationResult result
    );

    // Invalidate cache when world changes
    public void invalidateArea(BlockPos center, int radius);
}
```

### 2.6 Simulation Event System

**Package:** `com.minewright.simulation.event`

**Purpose:** Publish simulation lifecycle events for monitoring and UI updates.

**Event Types:**
```java
public class SimulationStartedEvent {
    private final String agentId;
    private final Task task;
    private final String simulationId;
}

public class SimulationCompletedEvent {
    private final String simulationId;
    private final SimulationResult result;
    private final long durationMs;
}

public class SimulationFailedEvent {
    private final String simulationId;
    private final String error;
    private final boolean isRecoverable;
}

public class RiskAssessmentEvent {
    private final String agentId;
    private final Task task;
    private final double riskScore;
    private final SimulationDecision decision;
}
```

---

## 3. Data Flow

### 3.1 Happy Path: Safe Action Execution

```
┌─────────────┐
│ User Command│ "Build a stone house at [100, 64, 200]"
└──────┬──────┘
       │
       v
┌─────────────────────┐
│ TaskPlanner         │ Generates tasks:
│                     │ 1. Pathfind to location
└──────┬──────────────┘ 2. Place cobblestone walls
       │                3. Place wooden door
       v
┌─────────────────────┐
│ ActionExecutor      │ Dequeues first task: Pathfind
└──────┬──────────────┘
       │
       v
┌─────────────────────────────────────────────────┐
│ InterceptorChain.executeBeforeAction()          │
│                                                  │
│ 1. LoggingInterceptor                            │
│ 2. MetricsInterceptor                           │
│ 3. SimulationInterceptor <──── NEW!             │
│    - Check cache (MISS)                          │
│    - Create voxel snapshot (radius: 32)          │
│    - Send to Kernel Gateway                      │
│    - Wait for result (async, non-blocking)       │
└──────┬──────────────────────────────────────────┘
       │
       v
┌─────────────────────┐
│ Kernel Gateway      │ Routes to Physics Kernel
└──────┬──────────────┘
       │
       v
┌─────────────────────┐
│ Physics Kernel      │ Simulates pathfinding:
│ (Python)            │ - Checks terrain traversability
└──────┬──────────────┘ - Detects hazards (lava, cliffs)
       │ - Predicts mob encounters
       v
┌─────────────────────┐
│ Simulation Result   │ {
│                     │   "success": true,
│ returned            │   "riskScore": 0.1,
└──────┬──────────────┘   "ticksSimulated": 245
       │                 }
       v
┌─────────────────────────────────────────────────┐
│ SimulationInterceptor                           │
│ - Receives result                               │
│ - Risk assessment: LOW (0.1)                    │
│ - Decision: PROCEED                             │
│ - Publishes SimulationCompletedEvent            │
│ - Returns true (allow action)                   │
└──────┬──────────────────────────────────────────┘
       │
       v
┌─────────────────────┐
│ PathfindAction.start() │ Action executes normally
└─────────────────────┘
```

### 3.2 Risk Path: Dangerous Action Intercepted

```
┌─────────────────────┐
│ Task: Place lava    │ at player's feet
└──────┬──────────────┘
       │
       v
┌─────────────────────────────────────────────────┐
│ SimulationInterceptor                           │
│ - Cache MISS                                    │
│ - Create voxel snapshot                          │
│ - Send simulation request                        │
└──────┬──────────────────────────────────────────┘
       │
       v
┌─────────────────────┐
│ Physics Kernel      │ Simulates lava flow:
│                     │ - Lava spreads instantly
└──────┬──────────────┘ - Agent caught in flow
       │ - Fire damage ticks
       v
┌─────────────────────┐
│ Simulation Result   │ {
│                     │   "success": false,
│ returned            │   "riskScore": 0.95,
└──────┬──────────────┘   "failureReason": "lava_immolation",
       │                 "alternatives": [
       v                   {"block": "stone", "risk": 0.0}
┌─────────────────────────────────────────────────┐                      ]
│ Risk Assessment Engine                          │
│ - Risk: LETHAL (0.95 >= 0.8)                    │
│ - Decision: ABORT                               │
│ - Generate alternative: Place stone instead      │
└──────┬──────────────────────────────────────────┘
       │
       v
┌─────────────────────┐
│ User Notification   │ "⚠️ DANGER: Placing lava will"
│                     │ "kill you! Suggested: stone"
└──────┬──────────────┘
       │
       v
┌─────────────────────┐
│ Action Cancelled    │ Agent stays safe
└─────────────────────┘
```

### 3.3 Learning Path: Simulation Feedback

```
┌─────────────────────┐
│ Real Action         │ Agent places torch
│ Executed            │ Unexpected: Creeper nearby
└──────┬──────────────┘
       │
       v
┌─────────────────────┐
│ Action Result       │ SUCCESS but with note:
│                     │ "Creeper detonated"
└──────┬──────────────┘
       │
       v
┌─────────────────────────────────────────────────┐
│ Simulation Learning System                      │
│ - Compare predicted vs actual outcome           │
│ - Simulation said: Safe (risk: 0.1)             │
│ - Reality: Creeper explosion                    │
│ - UPDATE: Increase mob threat weighting         │
│ - Store in learning database                    │
└──────┬──────────────────────────────────────────┘
       │
       v
┌─────────────────────┐
│ Next Simulation     │ Improved prediction:
│                     │ Risk score: 0.7 (HIGH)
└─────────────────────┘
```

---

## 4. Integration with Existing MineWright Code

### 4.1 ActionExecutor Integration

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`

**Changes Required:**

```java
public class ActionExecutor {
    // Existing fields...
    private final SimulationInterceptor simulationInterceptor;

    public ActionExecutor(ForemanEntity foreman) {
        // ... existing initialization ...

        // NEW: Create simulation components
        SimulationConfig config = SimulationConfig.load();
        KernelGatewayClient gateway = new KernelGatewayClient(
            config.getGatewayUrl(),
            config.getMaxKernels()
        );
        SimulationCache cache = new SimulationCache(config.getCacheSize());

        // NEW: Add to interceptor chain
        this.simulationInterceptor = new SimulationInterceptor(
            gateway, cache, config
        );
        interceptorChain.addInterceptor(simulationInterceptor);

        // Existing interceptors added after simulation
        interceptorChain.addInterceptor(new LoggingInterceptor());
        interceptorChain.addInterceptor(new MetricsInterceptor());
        interceptorChain.addInterceptor(new EventPublishingInterceptor(
            eventBus, foreman.getSteveName()
        ));
    }

    private void executeTask(Task task) {
        // NEW: Check if simulation is required
        if (simulationInterceptor.requiresSimulation(task)) {
            // Async simulation check
            CompletableFuture<SimulationDecision> future =
                simulationInterceptor.simulateAsync(task, foreman);

            // Non-blocking: will check in tick()
            // Store pending action for later execution
            pendingSimulationTasks.put(task, future);
            return;
        }

        // Existing: Create and execute action immediately
        currentAction = createAction(task);
        if (currentAction != null) {
            currentAction.start();
        }
    }

    @Override
    public void tick() {
        // ... existing tick logic ...

        // NEW: Check pending simulations
        checkPendingSimulations();

        // ... rest of existing tick logic ...
    }

    private void checkPendingSimulations() {
        Iterator<Map.Entry<Task, CompletableFuture<SimulationDecision>>> it =
            pendingSimulationTasks.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<Task, CompletableFuture<SimulationDecision>> entry = it.next();
            Task task = entry.getKey();
            CompletableFuture<SimulationDecision> future = entry.getValue();

            if (future.isDone()) {
                it.remove();
                try {
                    SimulationDecision decision = future.get();
                    handleSimulationDecision(task, decision);
                } catch (Exception e) {
                    MineWrightMod.LOGGER.error("Simulation error", e);
                    // Fallback: proceed without simulation
                    executeTaskDirectly(task);
                }
            }
        }
    }
}
```

### 4.2 BaseAction Integration

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BaseAction.java`

**Add Simulation Metadata:**

```java
public abstract class BaseAction {
    // Existing fields...

    // NEW: Simulation result that approved this action
    private SimulationResult approvedSimulation;

    // NEW: Override for actions that require simulation
    public boolean requiresSimulation() {
        return false; // Default: no simulation required
    }

    // NEW: Actions can provide simulation hints
    public SimulationHint getSimulationHint() {
        return SimulationHint.DEFAULT;
    }

    // NEW: Post-execution feedback for learning
    public void reportActualOutcome(ActionResult result) {
        if (approvedSimulation != null) {
            SimulationLearningSystem.getInstance().reportOutcome(
                approvedSimulation, result
            );
        }
    }
}

public enum SimulationHint {
    DEFAULT,           // Standard simulation
    QUICK_CHECK,       // Fast, less accurate
    DEEP_ANALYSIS,     // Thorough, slower
    COMBAT_FOCUS,      // Prioritize mob threats
    STRUCTURAL_FOCUS,  // Check physics/stability
    RESOURCE_FOCUSED   // Verify material availability
}
```

**Action-Specific Overrides:**

```java
public class PlaceBlockAction extends BaseAction {
    @Override
    public boolean requiresSimulation() {
        // Require simulation for dangerous blocks
        String blockType = task.getParameter("block");
        return isDangerousBlock(blockType);
    }

    @Override
    public SimulationHint getSimulationHint() {
        String blockType = task.getParameter("block");
        if (isFluidBlock(blockType)) {
            return SimulationHint.DEEP_ANALYSIS;
        }
        return SimulationHint.QUICK_CHECK;
    }

    private boolean isDangerousBlock(String blockType) {
        return blockType.contains("lava")
            || blockType.contains("fire")
            || blockType.contains("tnt");
    }
}

public class MineBlockAction extends BaseAction {
    @Override
    public boolean requiresSimulation() {
        // Simulate if mining potentially dangerous blocks
        String blockType = task.getParameter("block");
        return isUnstableBlock(blockType);
    }

    @Override
    public SimulationHint getSimulationHint() {
        return SimulationHint.STRUCTURAL_FOCUS;
    }
}
```

### 4.3 PromptBuilder Integration

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\PromptBuilder.java`

**Add Simulation Context:**

```java
public class PromptBuilder {
    public static String buildSystemPrompt() {
        return """
            You are Steve, a Minecraft foreman AI.

            # Available Actions
            ...

            # Simulation Capabilities
            You have access to mental simulation to preview actions:
            - Use simulation for risky or complex actions
            - Simulation checks for hazards, structural integrity, and threats
            - If simulation indicates danger, explain why and suggest alternatives

            # Risk Assessment
            Always consider:
            1. Is this action potentially dangerous? (lava, heights, mobs)
            2. Do I have enough information to proceed safely?
            3. Could this action damage existing structures?

            When uncertain, request simulation or propose safer alternatives.
            """;
    }

    public static String buildUserPrompt(ForemanEntity foreman, String command,
                                          WorldKnowledge worldKnowledge) {
        StringBuilder prompt = new StringBuilder();
        // ... existing prompt building ...

        // NEW: Add simulation status
        prompt.append("\n# Simulation Status\n");
        prompt.append("- Available: ").append(
            SimulationSystem.getInstance().isAvailable()
        ).append("\n");
        prompt.append("- Recent Simulations: ").append(
            SimulationSystem.getInstance().getRecentSimulationCount()
        ).append("\n");

        return prompt.toString();
    }
}
```

### 4.4 AgentStateMachine Integration

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\execution\AgentStateMachine.java`

**Add SIMULATING State:**

```java
public enum AgentState {
    IDLE,
    PLANNING,
    SIMULATING,  // NEW: Agent is simulating actions
    EXECUTING,
    PAUSED,
    COMPLETED,
    FAILED;
}

// In AgentStateMachine class
private static final Map<AgentState, Set<AgentState>> VALID_TRANSITIONS;
static {
    // ... existing transitions ...

    // PLANNING can now go to SIMULATING
    VALID_TRANSITIONS.put(AgentState.PLANNING,
        EnumSet.of(AgentState.SIMULATING, AgentState.EXECUTING,
                   AgentState.FAILED, AgentState.IDLE));

    // SIMULATING can go to EXECUTING or back to PLANNING
    VALID_TRANSITIONS.put(AgentState.SIMULATING,
        EnumSet.of(AgentState.EXECUTING, AgentState.PLANNING,
                   AgentState.FAILED));
}
```

### 4.5 EventBus Integration

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\event\EventBus.java`

**Add Simulation Event Types:**

```java
// New event classes in com.minewright.simulation.event package
public class SimulationStartedEvent {
    private final String agentId;
    private final String simulationId;
    private final Task task;
    private final Instant timestamp;
}

public class SimulationCompletedEvent {
    private final String simulationId;
    private final SimulationResult result;
    private final Duration duration;
    private final boolean wasCached;
}

public class SimulationAbortedEvent {
    private final String simulationId;
    private final String reason;
    private final Optional<Task> alternativeTask;
}
```

---

## 5. Python Kernel Architecture

### 5.1 Kernel Types

#### Physics Kernel
```python
class PhysicsKernel:
    """Handles block mechanics, gravity, fluids, and structural integrity"""

    def simulate_action(self, world_snapshot, action, max_ticks=100):
        """
        Simulates an action and returns predicted outcome.

        Returns:
            SimulationResult with:
            - success: bool
            - risk_score: float (0-1)
            - affected_blocks: List[BlockPos]
            - entity_damage: Dict[str, int]
        """
        pass

    def check_structural_integrity(self, world_snapshot, center, radius):
        """Check if blocks will fall or collapse"""
        pass

    def simulate_fluid_flow(self, world_snapshot, fluid_pos, ticks):
        """Predict lava/water flow patterns"""
        pass
```

#### World Kernel
```python
class WorldKernel:
    """Handles voxel queries, chunk loading, and world state"""

    def load_area(self, chunk_coords):
        """Load voxel data for specified chunks"""
        pass

    def find_path(self, start, goal, world_snapshot):
        """A* pathfinding with hazard avoidance"""
        pass

    def check_reachability(self, entity_pos, target_pos, world_snapshot):
        """Can entity reach target (accounting for obstacles)"""
        pass

    def estimate_resources(self, world_snapshot, area):
        """Count available resources in area"""
        pass
```

#### Combat Kernel
```python
class CombatKernel:
    """Handles mob AI, damage calculations, and threat assessment"""

    def predict_mob_behavior(self, world_snapshot, mob_type, mob_pos, ticks):
        """
        Predict mob actions for next N ticks.

        Returns:
            List of predicted positions/actions
        """
        pass

    def calculate_combat_risk(self, entity, mobs, world_snapshot):
        """
        Assess combat danger.

        Returns:
            Risk score 0-1 based on:
            - Mob type and count
            - Entity equipment
            - Environmental advantages
        """
        pass

    def simulate_combat(self, entity, target, world_snapshot, max_ticks=50):
        """Simulate combat encounter outcome"""
        pass
```

### 5.2 Jupyter Kernel Gateway Setup

**Installation:**
```bash
pip install jupyter_kernel_gateway
pip install numpy  # For voxel operations
```

**Start Gateway:**
```bash
jupyter kernelgateway \
  --KernelManager.kernel_cmd=['python', '-m', 'ipykernel_launcher', '-f', '{connection_file}'] \
  --IP 0.0.0.0 \
  --Port 8888 \
  --api='jupyter-websocket' \
  --token='YOUR_SECURE_TOKEN'
```

**Docker Deployment (Recommended):**
```dockerfile
FROM python:3.11-slim

RUN pip install jupyter_kernel_gateway numpy

COPY simulation_kernels/ /app/kernels/

WORKDIR /app
EXPOSE 8888

CMD ["jupyter", "kernelgateway", \
     "--KernelManager.kernel_cmd=['python', '-m', 'ipykernel_launcher', '-f', '{connection_file}']", \
     "--IP=0.0.0.0", \
     "--Port=8888", \
     "--api=jupyter-websocket", \
     "--token=${GATEWAY_TOKEN}"]
```

### 5.3 Kernel Communication Protocol

**Request Format:**
```json
{
  "kernel_id": "kernel_abc123",
  "code": "simulate_placement(world_data, action_data)",
  "data": {
    "world_snapshot": {
      "chunks": {...},
      "entities": [...]
    },
    "action": {
      "type": "place_block",
      "block": "minecraft:lava",
      "position": [100, 64, 200]
    }
  }
}
```

**Response Format:**
```json
{
  "status": "ok",
  "result": {
    "success": false,
    "risk_score": 0.95,
    "failure_reason": "lava_flow_will_immolate_agent",
    "predicted_ticks": 47,
    "affected_positions": [
      [100, 64, 200],
      [100, 64, 201],
      [101, 64, 200]
    ],
    "alternatives": [
      {
        "block": "minecraft:stone",
        "risk_score": 0.0
      }
    ]
  },
  "execution_time_ms": 23
}
```

---

## 6. Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Goal:** Basic infrastructure and simple simulations

**Tasks:**
1. Create simulation package structure
   - `com.minewright.simulation.*`

2. Implement VoxelWorldSnapshot
   - Efficient voxel storage
   - Chunk-based loading
   - Basic query API

3. Implement SimulationCache
   - In-memory caching (Caffeine)
   - Cache key generation

4. Setup Jupyter Kernel Gateway
   - Docker deployment
   - Basic Python kernel
   - Health check endpoint

**Deliverables:**
- Voxel snapshot can be created from Minecraft world
- Cache stores and retrieves results
- Gateway responds to ping

**Testing:**
```java
@Test
void testVoxelSnapshotCreation() {
    VoxelWorldSnapshot snapshot = worldKnowledge.createSnapshot(16);
    assertNotNull(snapshot);
    assertEquals(Blocks.STONE, snapshot.getBlockState(new BlockPos(0, 60, 0)).getBlock());
}

@Test
void testCacheHit() {
    SimulationRequest request = createTestRequest();
    SimulationResult result = createTestResult();

    cache.put(request, result);
    Optional<SimulationResult> cached = cache.get(request);

    assertTrue(cached.isPresent());
    assertEquals(result, cached.get());
}
```

### Phase 2: Simulation Interceptor (Week 3-4)

**Goal:** Integrate simulation into action execution

**Tasks:**
1. Implement SimulationInterceptor
   - Integrate into InterceptorChain
   - Async simulation execution
   - Decision logic

2. Implement KernelGatewayClient
   - HTTP client for gateway communication
   - WebSocket support for streaming
   - Connection pooling

3. Add AgentState.SIMULATING
   - State machine updates
   - Transition logic

4. Implement basic risk assessment
   - Risk thresholds
   - Decision enums

**Deliverables:**
- Actions are intercepted before execution
- Simulation requests sent to gateway
- Basic risk decisions work

**Testing:**
```java
@Test
void testInterceptorBlocksDangerousAction() {
    Task lavaTask = new Task("place", Map.of("block", "lava", ...));

    SimulationInterceptor interceptor = new SimulationInterceptor(...);
    boolean approved = interceptor.beforeAction(
        new PlaceBlockAction(foreman, lavaTask),
        context
    );

    assertFalse(approved); // Should block lava placement
}
```

### Phase 3: Python Kernels (Week 5-6)

**Goal:** Implement actual physics and world simulations

**Tasks:**
1. Physics Kernel
   - Block placement/removal
   - Gravity simulation
   - Fluid flow (lava/water)

2. World Kernel
   - Pathfinding (A*)
   - Reachability checks
   - Resource estimation

3. Combat Kernel
   - Basic mob AI
   - Damage calculation
   - Threat assessment

**Deliverables:**
- Kernels can simulate basic actions
- Risk scores are calculated
- Alternatives are generated

**Testing:**
```python
def test_lava_placement_risk():
    world = create_test_world()
    action = PlaceBlockAction(block="lava", position=(100, 64, 200))

    result = physics_kernel.simulate_action(world, action)

    assert result.success == False
    assert result.risk_score > 0.8
    assert "lava" in result.failure_reason
```

### Phase 4: Advanced Features (Week 7-8)

**Goal:** Enhance simulation quality and performance

**Tasks:**
1. Multi-step simulation
   - Simulate entire task sequences
   - Identify failure points in plans

2. Alternative plan generation
   - When simulation fails, suggest fixes
   - Rank alternatives by risk

3. Learning system
   - Compare predictions vs reality
   - Adjust risk weights

4. Performance optimization
   - Lazy snapshot loading
   - Parallel simulation (multiple kernels)
   - Incremental simulation

**Deliverables:**
- Complex plans can be fully simulated
- Alternatives suggested for dangerous actions
- Simulation improves over time

### Phase 5: Polish & Production Readiness (Week 9-10)

**Goal:** Make system production-ready

**Tasks:**
1. Error handling
   - Kernel crash recovery
   - Fallback when simulation unavailable
   - Graceful degradation

2. Monitoring & debugging
   - Simulation metrics (latency, hit rate)
   - Visualization tools (show simulation results)
   - Debug logging

3. Documentation
   - API docs
   - Configuration guide
   - Troubleshooting

4. Testing
   - Integration tests
   - Load tests
   - User acceptance testing

**Deliverables:**
- Production-ready simulation system
- Complete documentation
- Test coverage > 80%

---

## 7. Performance Considerations

### 7.1 Latency Budget

| Component | Target Latency | Optimization Strategy |
|-----------|---------------|----------------------|
| Voxel Snapshot Creation | < 50ms | Chunk caching, lazy loading |
| Kernel Gateway Request | < 100ms | Connection pooling, keep-alive |
| Python Simulation Execution | < 500ms | Numpy vectorization, C++ extensions |
| Risk Assessment | < 10ms | Pre-computed thresholds |
| **Total Simulation Time** | **< 660ms** | Parallel where possible |

### 7.2 Caching Strategy

**Cache Hit Targets:**
- Repeated actions in same area: > 80% hit rate
- Similar world states: > 40% hit rate
- Overall: > 50% hit rate

**Cache Invalidation:**
- Time-based: TTL of 5 minutes
- Event-based: Invalidate on block changes
- Area-based: Invalidate chunks where actions occur

**Implementation:**
```java
public class SimulationCache {
    private final Cache<CacheKey, SimulationResult> cache;

    public SimulationCache() {
        this.cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats() // For monitoring
            .build();
    }

    public CacheStats getStats() {
        return cache.stats();
    }
}
```

### 7.3 Scalability

**Kernel Pooling:**
- Start with 2-3 kernels per server
- Scale up based on load
- Max kernels: 10 (configurable)

**Load Balancing:**
```java
public class KernelPool {
    private final Queue<KernelGatewayClient> availableKernels;

    public CompletableFuture<SimulationResult> submitSimulation(
        SimulationRequest request
    ) {
        KernelGatewayClient kernel = acquireKernel();

        return kernel.executeSimulation(request)
            .whenComplete((result, error) -> releaseKernel(kernel));
    }
}
```

**Resource Limits:**
- Max concurrent simulations: 20
- Max simulation time: 2 seconds
- Max world snapshot size: 64x64x64 blocks

### 7.4 Memory Management

**Voxel Storage:**
- Use primitive arrays (not objects)
- Sparse storage for air blocks
- Compress chunk data

**Snapshot Lifecycle:**
```java
public class VoxelWorldSnapshot implements AutoCloseable {
    private final ByteBuffer voxelData;

    @Override
    public void close() {
        // Explicitly release native memory
        UnsafeAccess.freeMemory(voxelDataAddress);
    }
}

// Use try-with-resources
try (VoxelWorldSnapshot snapshot = createSnapshot()) {
    // Use snapshot
    // Automatically freed
}
```

### 7.5 Async Execution

**Non-blocking Simulation:**
```java
public class SimulationInterceptor implements ActionInterceptor {
    private final ExecutorService simulationExecutor;

    @Override
    public boolean beforeAction(BaseAction action, ActionContext context) {
        if (!action.requiresSimulation()) {
            return true; // Skip simulation
        }

        // Start simulation asynchronously
        CompletableFuture<SimulationDecision> future =
            simulateAsync(action, context);

        // Store future, check later in tick()
        context.pendingSimulations.put(action, future);

        return false; // Don't execute yet, wait for simulation
    }

    private CompletableFuture<SimulationDecision> simulateAsync(
        BaseAction action,
        ActionContext context
    ) {
        return CompletableFuture.supplyAsync(() -> {
            VoxelWorldSnapshot snapshot = createSnapshot(action);
            SimulationResult result = gateway.executeSimulation(snapshot);
            return riskAssessor.decide(result);
        }, simulationExecutor);
    }
}
```

---

## 8. Configuration

### 8.1 Config Options

**File:** `config/minewright-common.toml`

```toml
[simulation]
# Enable/disable simulation system
enabled = true

# Jupyter Kernel Gateway URL
gateway_url = "http://localhost:8888"

# Gateway authentication token
gateway_token = "YOUR_SECURE_TOKEN"

# Number of persistent kernels to maintain
kernel_pool_size = 3

# Maximum simulation time (seconds)
max_simulation_time = 2

# Maximum snapshot radius (blocks)
max_snapshot_radius = 64

[simulation.cache]
# Enable result caching
enabled = true

# Maximum cache size
max_size = 10000

# Cache TTL (minutes)
ttl_minutes = 5

[simulation.risk]
# Risk thresholds (0.0 - 1.0)
lethal_threshold = 0.8
high_risk_threshold = 0.5
medium_risk_threshold = 0.2

# Require user approval for high-risk actions
require_approval_high_risk = true

# Auto-generate alternatives
auto_generate_alternatives = true

# Maximum alternative plans to generate
max_alternatives = 3

[simulation.performance]
# Maximum concurrent simulations
max_concurrent = 20

# Simulation executor thread pool size
executor_threads = 4

# Enable parallel kernel execution
parallel_kernels = true
```

### 8.2 Runtime Controls

**In-Game Commands:**
```
/minewright simulation enable
/minewright simulation disable
/minewright simulation status
/minewright simulation cache clear
/minewright simulation kernel restart
```

**GUI Controls:**
- Toggle simulation in Foreman Office GUI
- View simulation results overlay
- Configure risk thresholds per-agent

---

## 9. Monitoring & Debugging

### 9.1 Metrics to Track

```java
public class SimulationMetrics {
    private final Counter simulationsRun;
    private final Counter simulationsCached;
    private final Timer simulationLatency;
    private final Histogram riskScores;
    private final Counter dangerousActionsBlocked;

    public void recordSimulation(SimulationResult result, boolean wasCached) {
        if (wasCached) {
            simulationsCached.increment();
        } else {
            simulationsRun.increment();
        }

        simulationLatency.record(result.getDurationMs(), TimeUnit.MILLISECONDS);
        riskScores.record(result.getRiskScore());

        if (result.getRiskScore() >= lethalThreshold) {
            dangerousActionsBlocked.increment();
        }
    }
}
```

### 9.2 Debug Visualization

**Overlay showing:**
- Currently simulated area (bounding box)
- Simulation result (color-coded: green=red)
- Risk score
- Alternative suggestions

**Implementation:**
```java
public class SimulationDebugRenderer {
    public void renderSimulationOverlay(
        PoseStack poseStack,
        SimulationResult result,
        VoxelWorldSnapshot snapshot
    ) {
        // Draw simulated area
        renderBoundingBox(poseStack, snapshot.getBounds());

        // Draw affected blocks
        for (BlockPos pos : result.getAffectedPositions()) {
            renderHighlight(poseStack, pos, getResultColor(result));
        }

        // Draw risk indicator
        renderRiskIndicator(poseStack, result.getRiskScore());
    }
}
```

### 9.3 Logging

```java
// Simulation lifecycle
LOGGER.info("[SIM-{}] Started simulation for action: {}",
    simulationId, task.getAction());

LOGGER.debug("[SIM-{}] Snapshot created: {} chunks, {} KB",
    simulationId, snapshot.getChunkCount(), snapshot.getSizeKB());

LOGGER.info("[SIM-{}] Completed: risk={}, duration={}ms, cached={}",
    simulationId, result.getRiskScore(), duration, wasCached);

// Decision logging
LOGGER.warn("[SIM-{}] Action BLOCKED: risk={} >= {}",
    simulationId, riskScore, lethalThreshold);

LOGGER.info("[SIM-{}] Suggested alternative: {} (risk={})",
    simulationId, alt.getAction(), alt.getRiskScore());
```

---

## 10. Security Considerations

### 10.1 Kernel Gateway Security

1. **Authentication:**
   - Use secure tokens (not default)
   - Rotate tokens regularly
   - IP whitelisting if possible

2. **Network Security:**
   - Run on localhost if possible
   - Use HTTPS/WSS in production
   - Firewall rules to limit access

3. **Code Execution:**
   - Validate all inputs before sending to kernel
   - Sanitize simulation code
   - Limit execution time and memory

### 10.2 Minecraft Mod Security

1. **World Data:**
   - Don't expose sensitive world data
   - Limit snapshot radius
   - Obfuscate coordinates if needed

2. **Action Validation:**
   - Always re-validate after simulation
   - Don't trust simulation blindly
   - Sanity check all parameters

---

## 11. Future Enhancements

### 11.1 Multi-Agent Simulation

- Simulate multiple agents working together
- Coordination planning
- Conflict detection

### 11.2 Machine Learning Integration

- Train models on simulation outcomes
- Predict risks without full simulation
- Learn from player feedback

### 11.3 Extended Physics

- Redstone circuit simulation
- Entity physics (minecarts, boats)
- Explosion propagation

### 11.4 Natural Language Explanations

- Generate human-readable simulation summaries
- Explain why an action is dangerous
- Describe alternatives in natural language

---

## 12. Glossary

| Term | Definition |
|------|------------|
| **Mental Simulation** | Virtual execution of actions before real-world execution |
| **Voxel Snapshot** | Lightweight copy of world state for simulation |
| **Kernel Gateway** | Server managing persistent Python kernels |
| **Risk Score** | Numerical value (0-1) indicating action danger |
| **Interceptor** | Plugin that hooks into action execution pipeline |
| **Simulation Cache** | Storage of simulation results to avoid recomputation |

---

## 13. References

### 13.1 Related Research

- **Baritone:** Minecraft pathfinding and simulation bot
- **MineRL:** Minecraft environment for RL research
- **Jupyter Kernel Gateway:** Web server for kernel access
- **Voxel-based World Representation:** Efficient spatial data structures

### 13.2 Internal Documents

- `ACTION_API.md` - Action system architecture
- `AGENT_STATE_MACHINE.md` - State machine design
- `LLM_TOOL_CALLING.md` - LLM integration patterns
- `MULTI_AGENT_COMMUNICATION.md` - Agent coordination

---

## Appendix A: Example Simulation Flows

### A.1 Safe Building Placement

```java
// User: "Build a cobblestone house at [100, 64, 200]"

Task task = new Task("build", Map.of(
    "structure", "house",
    "material", "cobblestone",
    "position", "100,64,200"
));

// Simulation checks:
// 1. Is area clear? YES
// 2. Can agent reach all positions? YES
// 3. Are materials available? YES (64 cobblestone in inventory)
// 4. Any mobs nearby? NO

SimulationResult result = SimulationResult.builder()
    .success(true)
    .riskScore(0.05) // LOW risk
    .ticksSimulated(1200)
    .build();

// Decision: PROCEED
// Action executes normally
```

### A.2 Dangerous Lava Placement

```java
// User: "Place lava at [100, 64, 200]"

Task task = new Task("place", Map.of(
    "block", "lava",
    "position", "100,64,200"
));

// Simulation checks:
// 1. Will lava flow? YES
// 2. Is agent in flow path? YES
// 3. Will agent take damage? YES (4 fire damage ticks)
// 4. Can agent escape? NO (surrounded by walls)

SimulationResult result = SimulationResult.builder()
    .success(false)
    .riskScore(0.95) // LETHAL
    .failureReason("Agent will be trapped in lava flow")
    .predictedDamage(Map.of("fire", 8.0))
    .alternatives(List.of(
        new Task("place", Map.of("block", "stone", ...))
    ))
    .build();

// Decision: ABORT
// Agent notified: "Danger! Lava will trap you. Try stone instead."
```

### A.3 Combat Encounter

```java
// User: "Attack the zombie"

Task task = new Task("attack", Map.of(
    "target", "zombie",
    "target_id", "entity_123"
));

// Simulation checks:
// 1. What's zombie's equipment? Iron sword
// 2. What's agent's equipment? Stone sword
// 3. Distance? 5 blocks
// 4. Agent health? 8/20 hearts
// 5. Predicted damage? Agent: 6 hearts, Zombie: 5 hearts

SimulationResult result = SimulationResult.builder()
    .success(true)
    .riskScore(0.6) // HIGH RISK but winnable
    .predictedOutcome(Map.of(
        "agent_health_remaining", 2.0,
        "zombie_killed", true
    ))
    .build();

// Decision: REQUIRE_CONFIRMATION
// Agent asks: "Zombie has iron sword! I'll win but take 6 hearts. Proceed?"
```

---

## Appendix B: Code Structure

```
src/main/java/com/minewright/simulation/
├── cache/
│   ├── SimulationCache.java
│   ├── CacheKey.java
│   └── CacheStats.java
├── event/
│   ├── SimulationStartedEvent.java
│   ├── SimulationCompletedEvent.java
│   ├── SimulationFailedEvent.java
│   └── RiskAssessmentEvent.java
├── gateway/
│   ├── KernelGatewayClient.java
│   ├── KernelConnection.java
│   ├── KernelPool.java
│   └── GatewayException.java
├── interceptor/
│   ├── SimulationInterceptor.java
│   └── SimulationConfig.java
├── risk/
│   ├── RiskAssessmentEngine.java
│   ├── RiskAssessmentConfig.java
│   ├── SimulationDecision.java
│   └── DecisionType.java
├── world/
│   ├── VoxelWorldSnapshot.java
│   ├── VoxelChunk.java
│   ├── ChunkPos.java
│   └── BlockState.java
├── learning/
│   ├── SimulationLearningSystem.java
│   ├── OutcomeComparison.java
│   └── RiskAdjustment.java
└── SimulationSystem.java (main facade)

simulation_kernels/
├── physics/
│   ├── __init__.py
│   ├── block_mechanics.py
│   ├── fluid_flow.py
│   └── structural_check.py
├── world/
│   ├── __init__.py
│   ├── voxel_loader.py
│   ├── pathfinding.py
│   └── resource_counter.py
├── combat/
│   ├── __init__.py
│   ├── mob_ai.py
│   ├── damage_calc.py
│   └── threat_assessment.py
└── kernels.py (kernel entry points)
```

---

**Document Status:** Ready for Review
**Next Steps:** Team review, feedback incorporation, begin Phase 1 implementation
