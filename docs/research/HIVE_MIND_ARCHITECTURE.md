# Hive Mind Architecture

**Version:** 1.0
**Date:** 2026-02-27
**Status:** Design Specification
**Author:** Architecture Team

---

## Executive Summary

The **Hive Mind** is a hybrid distributed AI architecture that combines local strategic planning (Jupyter Kernel Gateway) with edge-based tactical reflexes (Cloudflare Workers). This dual-layer approach enables Minecraft AI agents to achieve sub-10ms response times for critical combat/navigation decisions while maintaining sophisticated multi-agent coordination and world simulation capabilities.

### Key Benefits

| Benefit | Description | Impact |
|---------|-------------|--------|
| **Ultra-Low Latency** | Tactical decisions at edge (<10ms) | Combat reflexes, emergency avoidance |
| **Scalability** | Stateless workers auto-scale | Support 100+ agents simultaneously |
| **Observability** | Centralized logging via AI Gateway | Real-time debugging, performance monitoring |
| **Resilience** | Local fallback when edge unavailable | Graceful degradation, always works |
| **Cost Efficiency** | Pay-per-use edge computing | Only pay for actual agent activity |

### Why Hybrid Local+Edge?

**Pure Local** (Current State):
- ✅ No network dependency
- ❌ Limited scalability (single machine)
- ❌ No shared state between servers
- ❌ Expensive for light workloads

**Pure Edge**:
- ✅ Infinite scalability
- ✅ Built-in shared state
- ❌ Network latency (50-200ms)
- ❌ No offline capability

**Hybrid Hive Mind**:
- ✅ Best of both worlds
- ✅ Strategic planning locally (Jupyter)
- ✅ Tactical reflexes at edge (Workers)
- ✅ Fallback to local when needed

---

## Architecture Overview

### System Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         HIVE MIND ARCHITECTURE                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                           LOCAL FOREMAN LAYER                              │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                    Jupyter Kernel Gateway                             │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐     │  │
│  │  │ Physics Kernel  │  │  World Kernel   │  │  Combat Kernel  │     │  │
│  │  │ - Block physics │  │ - Pathfinding   │  │ - Mob AI        │     │  │
│  │  │ - Fluid flow    │  │ - Spatial idx   │  │ - Damage calc   │     │  │
│  │  │ - Structures    │  │ - Resources     │  │ - Threat assess │     │  │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘     │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                      │                                      │
│  ┌───────────────────────────────────┼──────────────────────────────────┐  │
│  │              Strategic Planning & World Simulation                   │  │
│  │  • Mental simulation (what-if scenarios)                             │  │
│  │  • Multi-agent coordination (Contract Net, Blackboard)               │  │
│  │  • Complex optimization (build sequences, resource allocation)       │  │
│  │  • Long-term memory (learned patterns, world knowledge)              │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ HTTP/WebSocket (100-500ms acceptable)
                                      │ (strategic sync, mission dispatch)
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CLOUDFLARE EDGE LAYER                               │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                      Cloudflare Workers                               │  │
│  │  ┌────────────────────────────────────────────────────────────────┐ │  │
│  │  │  Tactical Reflex Layer (Sub-10ms responses)                     │ │  │
│  │  │  • Emergency avoidance (lava, cliffs, mobs)                      │ │  │
│  │  │  • Quick decisions (block placement, movement)                   │ │  │
│  │  │  • Workers AI for fast inference                                │ │  │
│  │  └────────────────────────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                      │                                      │
│  ┌───────────────────────────────────┼──────────────────────────────────┐  │
│  │                        Edge Computing Services                       │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────┐  │  │
│  │  │   Durable   │  │  Vectorize  │  │    KV       │  │    AI     │  │  │
│  │  │   Objects   │  │  (RAG)      │  │   Cache     │  │  Gateway  │  │  │
│  │  │             │  │             │  │             │  │           │  │  │
│  │  │ Per-agent   │  │ Semantic    │  │ Mission     │  │ Observ-   │  │  │
│  │  │ state       │  │ search      │  │ cache       │  │ ability   │  │  │
│  │  │             │  │             │  │             │  │           │  │  │
│  │  │ • Position  │  │ • Resource  │  │ • Completed │  │ • Metrics │  │  │
│  │  │ • Health    │  │   locations │  │ • Pending   │  │ • Logs    │  │  │
│  │  │ • Inventory │  │ • World     │  │ • Failed    │  │ • Costs   │  │  │
│  │  │ • Mission   │  │   knowledge│  │ • Alternat- │  │ • Errors  │  │  │
│  │  │ • Status    │  │ • Build     │  │   ives      │  │           │  │  │
│  │  │             │  │   plans     │  │             │  │           │  │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └───────────┘  │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ WebSocket (<50ms to most regions)
                                      │ (tactical commands, sensor data)
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          MINECRAFT CLIENT/SERVER                           │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                    Minecraft Forge Mod (Java)                        │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐     │  │
│  │  │  ForemanEntity  │  │ ActionExecutor  │  │   CrewManager   │     │  │
│  │  │  - Strategic    │  │  - Tactical     │  │  - Multi-agent  │     │  │
│  │  │    planning    │  │    execution    │  │    coordination │     │  │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘     │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│  Think-Act Loop:                                                             │
│  1. SENSE → Minecraft world events                                           │
│  2. THINK → Local Jupyter (strategic) OR Edge Worker (tactical)             │
│  3. ACT  → Minecraft action execution                                        │
│  4. LEARN → Update local + edge memory                                       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Think-Act Loop Flow

```
┌─────────┐    ┌──────────┐    ┌─────────────────┐    ┌─────────┐
│  SENSE  │───▶│  FILTER  │───▶│      THINK      │───▶│   ACT   │
│ (MC)    │    │ (Urgency)│    │  (Decision Layer)│    │ (MC)    │
└─────────┘    └──────────┘    └─────────────────┘    └─────────┘
                   │                   │                     │
                   │                   │                     │
                   ▼                   ▼                     ▼
            ┌─────────────┐     ┌───────────────┐     ┌─────────────┐
            │ HIGH (>0.7) │     │  Choose Route │     │ Execute     │
            │             │     │  based on:    │     │ Action      │
            │ → EDGE      │     │  • Urgency     │     │ + Update    │
            │   (tactical)│     │  • Complexity  │     │ State       │
            │             │     │  • Connectivity│     │             │
            └─────────────┘     └───────────────┘     └─────────────┘
                   │                   │
                   │                   ▼
                   │            ┌──────────────────┐
                   │            │  Decision Routes │
                   │            │  ┌────────────┐  │
                   │            │  │ HIGH       │──▶ Edge Worker
                   │            │  │ urgency    │     (<10ms)
                   │            │  └────────────┘  │
                   │            │  ┌────────────┐  │
                   │            │  │ MEDIUM     │──▶ Local (Jupyter)
                   │            │  │ complexity │     (100-500ms)
                   │            │  └────────────┘  │
                   │            │  ┌────────────┐  │
                   │            │  │ OFFLINE    │──▶ Local Only
                   │            │  │ mode      │     (fallback)
                   │            │  └────────────┘  │
                   │            └──────────────────┘
                   │
                   ▼
            ┌─────────────────────────────────────────────────────────────┐
            │                    TACTICAL REFLEX                            │
            │  • Emergency stop (lava, cliff)                             │
            │  • Quick combat dodge                                       │
            │  • Block placement verification                             │
            │  • Simple pathfinding adjustments                           │
            │  • Resource count checks                                    │
            └─────────────────────────────────────────────────────────────┘
```

---

## Component Breakdown

### 1. Local Foreman (Jupyter Kernel Gateway)

**Location:** Runs on same machine as Minecraft server
**Technology:** Python 3.11+, Jupyter Kernel Gateway, NumPy, SciPy

#### Purpose

The Local Foreman handles **strategic, computationally intensive** tasks that:
- Require complex world simulation
- Involve multi-agent coordination
- Need access to full world state
- Can tolerate 100-500ms latency
- Benefit from Python's scientific ecosystem

#### Core Responsibilities

```python
class LocalForeman:
    """
    Strategic planning coordinator running locally.
    Handles complex decisions that benefit from full world context.
    """

    def __init__(self):
        self.physics_kernel = PhysicsKernel()
        self.world_kernel = WorldKernel()
        self.combat_kernel = CombatKernel()
        self.coordinator = MultiAgentCoordinator()

    async def plan_complex_build(self, blueprint: StructureBlueprint) -> BuildPlan:
        """
        Simulate entire build sequence before execution.

        Uses:
        - Physics kernel for structural integrity
        - World kernel for resource estimation
        - Combat kernel for threat assessment
        """
        # Check structural feasibility
        if not self.physics_kernel.check_stability(blueprint):
            return BuildPlan(error="Structure will collapse")

        # Estimate required resources
        resources = self.world_kernel.estimate_resources(blueprint)

        # Optimize build sequence (spatial, temporal)
        sequence = self.optimize_build_sequence(blueprint, resources)

        # Coordinate multiple agents
        assignments = self.coordinator.assign_tasks(sequence, available_agents)

        return BuildPlan(assignments=assignments, sequence=sequence)

    async def simulate_combat_encounter(self, agents: List[Agent], mobs: List[Mob]) -> CombatPlan:
        """
        Simulate combat outcome before engagement.

        Returns:
        - Predicted damage to each agent
        - Recommended tactics
        - Alternative strategies (flee, call backup)
        """
        # Predict mob behavior
        mob_actions = self.combat_kernel.predict_mob_behavior(mobs, ticks=100)

        # Simulate combat rounds
        outcome = self.combat_kernel.simulate_combat(agents, mobs, max_ticks=50)

        # Generate combat plan
        if outcome.agent_survival_rate < 0.5:
            return CombatPlan(retreat=True, reason="High casualties predicted")
        else:
            return CombatPlan(attack=True, tactics=outcome.optimal_tactics)

    async def coordinate_mining_operation(self, resource_type: str, quantity: int) -> MiningPlan:
        """
        Coordinate multiple agents for efficient resource gathering.

        Uses Contract Net Protocol for task allocation.
        """
        # Find nearest deposits
        deposits = self.world_kernel.find_nearest_deposits(resource_type, agent_positions)

        # Auction-based task allocation
        auction = ResourceAuction(deposits, agents)
        assignments = auction.allocate_tasks()

        return MiningPlan(assignments=assignments, estimated_yield=quantity)
```

#### Key Kernels

**Physics Kernel** (`physics_kernel.py`):
```python
class PhysicsKernel:
    """Block mechanics, gravity, fluids, structural integrity"""

    def simulate_action(self, world_snapshot, action) -> SimulationResult:
        """Predict outcome of placing/breaking blocks"""
        pass

    def check_structural_integrity(self, world_snapshot, build_plan) -> bool:
        """Will structure stand or collapse?"""
        pass

    def simulate_fluid_flow(self, world_snapshot, fluid_pos, ticks) -> FlowPattern:
        """Predict lava/water spread"""
        pass
```

**World Kernel** (`world_kernel.py`):
```python
class WorldKernel:
    """Voxel queries, pathfinding, resource estimation"""

    def find_path(self, start, goal, world_snapshot) -> Path:
        """A* pathfinding with hazard avoidance"""
        pass

    def estimate_resources(self, world_snapshot, area) -> ResourceReport:
        """Count available resources in area"""
        pass

    def check_reachability(self, entity_pos, target_pos) -> bool:
        """Can entity reach target?"""
        pass
```

**Combat Kernel** (`combat_kernel.py`):
```python
class CombatKernel:
    """Mob AI, damage calculations, threat assessment"""

    def predict_mob_behavior(self, mobs, ticks) -> List[MobAction]:
        """Predict mob actions for next N ticks"""
        pass

    def simulate_combat(self, agents, mobs, max_ticks) -> CombatOutcome:
        """Simulate combat encounter"""
        pass

    def calculate_combat_risk(self, entity, mobs) -> float:
        """Risk score 0-1 based on equipment, numbers, environment"""
        pass
```

#### Data Flow

```
Minecraft Mod (Java)
        │
        │ HTTP POST /simulate
        │ {
        │   "type": "build_plan",
        │   "blueprint": {...},
        │   "world_state": {...}
        │ }
        ▼
┌───────────────────┐
│ Jupyter Gateway   │
│   :8888/rest      │
└─────────┬─────────┘
          │
          │ Execute Python Code
          ▼
┌──────────────────────────────────────────────────────────────┐
│                      Local Foreman                           │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐             │
│  │   Physics  │  │   World    │  │  Combat    │             │
│  │   Kernel   │  │   Kernel   │  │  Kernel    │             │
│  └────────────┘  └────────────┘  └────────────┘             │
└──────────────────────────────────────────────────────────────┘
          │
          │ JSON Response
          │ {
          │   "success": true,
          │   "plan": [...],
          │   "risk_score": 0.2,
          │   "estimated_time": 450
          │ }
          ▼
Minecraft Mod executes plan
```

#### When to Use Local Foreman

| Task Type | Use Local Because... |
|-----------|---------------------|
| **Complex Building** | Need structural simulation, resource optimization |
| **Multi-Agent Coord** | Contract Net, Blackboard patterns require full state |
| **Long-Term Planning** | Simulate 1000+ ticks into future |
| **Pathfinding** | A* on full world state, avoid hazards |
| **Resource Estimation** | Scan large areas, count blocks |
| **Combat Strategy** | Predict mob behavior, simulate encounter |

---

### 2. Cloudflare Workers (Tactical Edge)

**Location:** Cloudflare global network (300+ locations)
**Technology:** Cloudflare Workers (Python), Workers AI, Durable Objects

#### Purpose

Cloudflare Workers handle **tactical, time-critical** decisions that:
- Require sub-10ms response time
- Are simple/rule-based
- Benefit from global distribution
- Don't need full world context
- Can be stateless or use minimal state

#### Core Responsibilities

```python
# cloudflare/src/index.py

from cloudflareWorkers import *
from ai import Ai_Binding

# Workers AI binding for fast inference
AI = Ai_Binding()

# Durable Object for per-agent state
class AgentState(DurableObject):
    def __init__(self, env, state):
        self.env = env
        self.storage = state.storage
        # Load or initialize agent state
        self.agent_id = state.id
        self.position = (0, 64, 0)
        self.health = 20
        self.inventory = {}
        self.current_mission = None

    async def fetch(self, request):
        """Handle incoming requests from Minecraft client"""
        url = request.url
        path = url.path

        if path.startswith("/tactical/"):
            return await self.handle_tactical(request)
        elif path.startswith("/sync/"):
            return await self.handle_sync(request)
        elif path.startswith("/mission/"):
            return await self.handle_mission(request)

    async def handle_tactical(self, request):
        """
        Handle tactical decision requests (sub-10ms).
        """
        # Parse request
        data = await request.json()
        action = data.get("action")
        context = data.get("context", {})

        # Route to appropriate handler
        if action == "check_emergency":
            return await self.check_emergency(context)
        elif action == "quick_place":
            return await self.quick_place_block(context)
        elif action == "quick_move":
            return await self.quick_move(context)
        elif action == "combat_reflex":
            return await self.combat_reflex(context)

    async def check_emergency(self, context):
        """
        EMERGENCY AVOIDANCE - Highest priority reflex.

        Returns immediately if danger detected.
        """
        position = context.get("position")
        blocks = context.get("nearby_blocks", [])
        entities = context.get("nearby_entities", [])

        # Lava check
        for block in blocks:
            if block.get("type") == "lava" and is_adjacent(position, block["pos"]):
                return Response.json({
                    "danger": True,
                    "type": "lava",
                    "action": "stop",
                    "reason": "Lava adjacent",
                    "latency_ms": 5
                })

        # Cliff check
        for block in blocks:
            if block.get("type") == "air" and is_below(position, block["pos"]):
                if block["pos"][1] < position[1] - 3:
                    return Response.json({
                        "danger": True,
                        "type": "cliff",
                        "action": "stop",
                        "reason": "Cliff detected",
                        "latency_ms": 6
                    })

        # Hostile mob check
        for entity in entities:
            if entity.get("type") in ["zombie", "skeleton", "creeper", "spider"]:
                dist = distance(position, entity["pos"])
                if dist < 3:
                    return Response.json({
                        "danger": True,
                        "type": "mob",
                        "action": "retreat",
                        "entity": entity,
                        "reason": f"Hostile mob at {dist} blocks",
                        "latency_ms": 4
                    })

        return Response.json({
            "danger": False,
            "action": "proceed",
            "latency_ms": 3
        })

    async def combat_reflex(self, context):
        """
        COMBAT REFLEX - Quick combat decisions using Workers AI.

        Uses Workers AI for fast inference on combat scenarios.
        """
        agent_health = context.get("health", 20)
        nearby_mobs = context.get("nearby_mobs", [])
        agent_equipment = context.get("equipment", {})

        if not nearby_mobs:
            return Response.json({
                "action": "idle",
                "reason": "No threats"
            })

        # Use Workers AI for combat decision
        prompt = f"""
        Minecraft agent combat situation:
        - Agent health: {agent_health}/20
        - Equipment: {agent_equipment}
        - Nearby threats: {nearby_mobs}

        Recommend action (attack/retreat/dodge) with brief reason.
        Respond in JSON format: {{"action": "...", "reason": "..."}}
        """

        ai_response = await AI.run(prompt, model="@cf/meta/llama-3.1-8b-instruct")

        try:
            decision = json.loads(ai_response)
            return Response.json({
                "action": decision.get("action"),
                "reason": decision.get("reason"),
                "ai_model": "llama-3.1-8b-instruct",
                "latency_ms": 8
            })
        except:
            # Fallback to rule-based
            if agent_health < 6:
                return Response.json({
                    "action": "retreat",
                    "reason": "Low health (fallback)",
                    "latency_ms": 2
                })
            else:
                return Response.json({
                    "action": "attack",
                    "reason": "Engage threat (fallback)",
                    "latency_ms": 2
                })

    async def quick_place_block(self, context):
        """
        QUICK BLOCK PLACEMENT - Validate block placement.
        """
        block_type = context.get("block_type")
        position = context.get("position")
        nearby_blocks = context.get("nearby_blocks", [])

        # Check if position is occupied
        for block in nearby_blocks:
            if block["pos"] == position:
                return Response.json({
                    "allowed": False,
                    "reason": "Position occupied",
                    "existing_block": block["type"]
                })

        # Check for dangerous placements
        if block_type == "lava":
            # Check if would trap agent
            if would_trap_agent(position, nearby_blocks):
                return Response.json({
                    "allowed": False,
                    "reason": "Lava would trap agent"
                })

        return Response.json({
            "allowed": True,
            "reason": "Safe to place"
        })

    async def handle_sync(self, request):
        """
        SYNC - Synchronize agent state with Local Foreman.

        Called periodically by Minecraft mod to keep edge state fresh.
        """
        # Update local state from request
        data = await request.json()
        self.position = tuple(data.get("position", self.position))
        self.health = data.get("health", self.health)
        self.inventory = data.get("inventory", self.inventory)
        self.current_mission = data.get("mission")

        # Persist to Durable Object storage
        await self.storage.put({
            "position": self.position,
            "health": self.health,
            "inventory": self.inventory,
            "mission": self.current_mission,
            "last_sync": datetime.now().isoformat()
        })

        # Return current mission if any
        return Response.json({
            "synced": True,
            "mission": self.current_mission
        })

    async def handle_mission(self, request):
        """
        MISSION - Receive mission from Local Foreman.

        Stores mission in Durable Object for fast access during tactical decisions.
        """
        mission = await request.json()
        self.current_mission = mission

        # Store mission with TTL
        await self.storage.put("mission", mission, expiration_ttl=300)  # 5 minutes

        return Response.json({
            "accepted": True,
            "mission_id": mission.get("id")
        })
```

#### Decision Routing Logic

```python
async def route_decision(context):
    """
    Decide whether to handle locally (Worker) or escalate to Foreman.
    """
    urgency = calculate_urgency(context)
    complexity = estimate_complexity(context)
    has_connectivity = check_foreman_available()

    # Urgent tactical decisions → Handle immediately at edge
    if urgency > 0.7:
        return "EDGE_IMMEDIATE"

    # Medium complexity, Foreman available → Delegate to Foreman
    if complexity > 0.5 and has_connectivity:
        return "FOREMAN_DELEGATE"

    # Low complexity, can cache → Handle at edge with caching
    if complexity < 0.3:
        return "EDGE_CACHED"

    # Offline mode → Handle locally with limited capability
    if not has_connectivity:
        return "EDGE_FALLBACK"

    # Default → Edge
    return "EDGE_DEFAULT"
```

#### When to Use Cloudflare Workers

| Task Type | Use Workers Because... |
|-----------|----------------------|
| **Emergency Avoidance** | Sub-10ms reflex, don't wait for network |
| **Quick Decisions** | Simple rules, fast inference with Workers AI |
| **Combat Reflexes** | Dodge, attack, retreat based on immediate threat |
| **Block Validation** | Check if placement is safe/valid |
| **Simple Movement** | Avoid obvious obstacles, short paths |
| **State Storage** | Durable Objects for per-agent memory |

---

### 3. Durable Objects (Working Memory)

**Purpose:** Persistent per-agent state stored at edge

#### Schema

```python
class AgentStateSchema:
    """
    Schema for agent state stored in Durable Objects.

    Stored as JSON in Durable Object storage.
    """

    # Identity
    agent_id: str
    agent_name: str
    created_at: datetime

    # Position & Status
    position: tuple[int, int, int]  # (x, y, z)
    dimension: str  # "overworld", "nether", "end"
    health: float  # 0-20
    food: float  # 0-20

    # Inventory
    inventory: dict[str, int]  # {"minecraft:stone": 64, ...}

    # Current Mission
    mission_id: Optional[str]
    mission_type: Optional[str]  # "mine", "build", "transport", ...
    mission_target: Optional[tuple[int, int, int]]
    mission_progress: float  # 0.0 - 1.0

    # Capabilities
    capabilities: list[str]  # ["mine", "build", "fight", ...]
    specialization: Optional[str]  # "miner", "builder", "scout"

    # Learning
    success_count: int
    failure_count: int
    last_actions: list[dict]  # Last 10 actions with outcomes

    # Timestamps
    last_sync: datetime
    last_action: datetime
    last_seen: datetime
```

#### API

```python
# Worker script for managing agent state

async def get_agent_state(agent_id: str) -> dict:
    """
    Get agent state from Durable Object.

    Called by Minecraft mod to sync state.
    """
    # Get Durable Object stub
    stub = env.AGENT_STATE.get(id=agent_id)

    # Fetch state
    response = await stub.fetch(Request.new(
        "http://agent-state/get",
        method="GET"
    ))

    return await response.json()

async def update_agent_position(agent_id: str, position: tuple):
    """
    Update agent position in Durable Object.

    Called after every move action (batch recommended).
    """
    stub = env.AGENT_STATE.get(id=agent_id)

    await stub.fetch(Request.new(
        "http://agent-state/update",
        method="POST",
        body=json.dumps({"position": position})
    ))

async def complete_mission(agent_id: str, mission_id: str, outcome: str):
    """
    Mark mission as complete, update learning stats.

    Called when mission succeeds or fails.
    """
    stub = env.AGENT_STATE.get(id=agent_id)

    await stub.fetch(Request.new(
        "http://agent-state/complete",
        method="POST",
        body=json.dumps({
            "mission_id": mission_id,
            "outcome": outcome,
            "timestamp": datetime.now().isoformat()
        })
    ))
```

#### Data Lifecycle

```
┌─────────────────────────────────────────────────────────────┐
│                   Durable Object Lifecycle                   │
└─────────────────────────────────────────────────────────────┘

  Minecraft              Cloudflare              Local Foreman
      │                       │                        │
      │ 1. Agent spawned      │                        │
      │───────────────────────>│                        │
      │   POST /agent/create   │                        │
      │                       │ 2. Create DO           │
      │                       │<───────────────────────│
      │                       │   3. Init state        │
      │                       │                        │
      │ 4. Sync every 5s      │                        │
      │<──────────────────────>|                        │
      │   POST /sync          │   5. Persist to DO    │
      │                       │                        │
      │ 6. Mission dispatch  │                        │
      │                       │<───────────────────────│
      │   GET /mission        │   7. Store mission    │
      │                       │                        │
      │ 8. Mission complete  │                        │
      │───────────────────────>│                        │
      │   POST /complete      │   9. Update stats     │
      │                       │───────────────────────>│
      │                       │  10. Archive to       │
      │                       │     Vectorize         │
      │                       │                        │
      │ 11. Agent despawned   │                        │
      │───────────────────────>│                        │
      │   DELETE /agent       │ 12. Delete DO         │
```

---

### 4. Vectorize (Long-term Memory)

**Purpose:** Semantic search for world knowledge, build plans, learned patterns

#### Vector Collections

```python
# Collections in Vectorize

class VectorCollections:
    """
    Vector collections for semantic search.

    Each collection stores embeddings for different knowledge types.
    """

    # Resource Locations
    RESOURCE_DEPOSITS = "minecraft-resources"
    # Vectors: location embeddings
    # Metadata: {"resource_type": "diamond", "position": [x,y,z], "quantity": 5}

    # Build Plans
    BUILD_PLANS = "minecraft-builds"
    # Vectors: blueprint description embeddings
    # Metadata: {"blueprint_id": "...", "blocks": [...], "success_rate": 0.95}

    # Combat Encounters
    COMBAT_HISTORY = "minecraft-combat"
    # Vectors: situation description embeddings
    # Metadata: {"mobs": [...], "outcome": "victory", "tactics": [...]}

    # Agent Behaviors
    AGENT_PATTERNS = "minecraft-behaviors"
    # Vectors: behavior description embeddings
    # Metadata: {"agent_id": "...", "action": "...", "result": "success"}
```

#### Usage Patterns

```python
async def find_nearest_resources(resource_type: str, position: tuple, limit: int = 5) -> list:
    """
    Find nearest resource deposits using semantic search + distance filtering.

    Benefits:
    - Semantic matching (e.g., "diamond ore" ≈ "deepslate diamonds")
    - Fast retrieval (vector search optimized)
    - Learned patterns (find what was successful before)
    """
    # Generate query embedding
    query_text = f"{resource_type} deposit near {position}"
    query_vector = await env.VECTORIZE.embed(query_text)

    # Vector search
    results = await env.VECTORIZE.query(
        index_name="minecraft-resources",
        vector=query_vector,
        top_k=limit * 3,  # Get more, filter by distance
        namespace="resources",
        filter={"resource_type": resource_type}
    )

    # Filter by distance and return
    nearby = []
    for result in results:
        dist_pos = result["metadata"]["position"]
        distance = calculate_distance(position, dist_pos)

        if distance < 500:  # Within 500 blocks
            nearby.append({
                "position": dist_pos,
                "distance": distance,
                "quantity": result["metadata"]["quantity"],
                "confidence": result["score"]
            })

        if len(nearby) >= limit:
            break

    # Sort by distance
    nearby.sort(key=lambda x: x["distance"])

    return nearby[:limit]

async def search_similar_builds(description: str) -> list:
    """
    Find similar build plans using semantic search.

    Uses:
    - Find what worked before
    - Adapt successful strategies
    - Learn from failures
    """
    query_vector = await env.VECTORIZE.embed(description)

    results = await env.VECTORIZE.query(
        index_name="minecraft-builds",
        vector=query_vector,
        top_k=5,
        namespace="builds"
    )

    return [
        {
            "blueprint_id": r["metadata"]["blueprint_id"],
            "similarity": r["score"],
            "success_rate": r["metadata"]["success_rate"],
            "blocks": r["metadata"]["blocks"]
        }
        for r in results
    ]

async def learn_from_combat(situation: dict, outcome: str, tactics: list):
    """
    Store combat encounter in vector database for future learning.

    Enables:
    - "What did we do last time in this situation?"
    - Pattern recognition (e.g., "always retreat when 3+ skeletons")
    - Tactical recommendations
    """
    # Generate embedding from situation description
    description = f"""
    Combat situation:
    - Agent health: {situation['health']}/20
    - Nearby mobs: {situation['mobs']}
    - Equipment: {situation['equipment']}
    - Environment: {situation['environment']}
    """

    embedding = await env.VECTORIZE.embed(description)

    # Store with metadata
    await env.VECTORIZE.upsert(
        index_name="minecraft-combat",
        vectors=[{
            "id": f"combat-{uuid.uuid4()}",
            "values": embedding,
            "metadata": {
                "timestamp": datetime.now().isoformat(),
                "situation": situation,
                "outcome": outcome,
                "tactics": tactics
            }
        }]
    )
```

#### When to Use Vectorize

| Use Case | Query Type | Benefit |
|----------|-----------|---------|
| **Find Resources** | "diamond deposits near spawn" | Semantic match + location filter |
| **Similar Builds** | "houses like the one at [100,64,200]" | Reuse successful patterns |
| **Combat History** | "last time we fought 3 skeletons" | Learn from past encounters |
| **Agent Specialization** | "agents good at mining" | Match tasks to capabilities |

---

### 5. AI Gateway (Observability)

**Purpose:** Centralized logging, metrics, and debugging for all AI decisions

#### Metrics Collected

```python
class AIMetrics:
    """
    Metrics tracked by AI Gateway.

    Enables:
    - Performance monitoring
    - Cost tracking
    - Error debugging
    - Usage analytics
    """

    # Decision Metrics
    decisions_total: Counter           # Total decisions made
    decisions_edge: Counter            # Decisions handled by Workers
    decisions_local: Counter           # Decisions handled by Local Foreman
    decisions_cached: Counter          # Decisions served from cache

    # Latency Metrics
    latency_edge: Histogram            # Edge decision latency (ms)
    latency_local: Histogram           # Local Foreman latency (ms)
    latency_network: Histogram         # Network round-trip (ms)

    # Cost Metrics
    cost_workers_ai: Gauge             # Workers AI API costs
    cost_local_compute: Gauge          # Local compute costs
    cost_storage: Gauge                # Storage costs (DO + Vectorize)

    # Error Metrics
    errors_total: Counter              # Total errors
    errors_timeout: Counter            # Timeout errors
    errors_fallback: Counter           # Fallback to local mode

    # Agent Metrics
    agents_active: Gauge               # Currently active agents
    agents_total: Gauge                # Total agents ever created
    missions_active: Gauge             # Currently active missions
    missions_completed: Counter        # Missions completed successfully

    # Learning Metrics
    cache_hit_rate: Gauge              # Cache effectiveness
    prediction_accuracy: Gauge         # Simulation vs reality
    learning_rate: Gauge               # How fast agents improve
```

#### Dashboard Queries

```sql
-- Average decision latency by layer
SELECT
    layer,
    AVG(latency_ms) as avg_latency,
    COUNT(*) as decisions
FROM metrics.decisions
WHERE timestamp > NOW() - INTERVAL '1 hour'
GROUP BY layer;

-- Cache hit rate over time
SELECT
    timestamp,
    cache_hits / (cache_hits + cache_misses) as hit_rate
FROM metrics.performance
WHERE timestamp > NOW() - INTERVAL '24 hours'
GROUP BY timestamp;

-- Top 10 agents by mission completion
SELECT
    agent_id,
    COUNT(*) as missions_completed,
    AVG(completion_time_ms) as avg_time
FROM metrics.missions
WHERE status = 'completed'
GROUP BY agent_id
ORDER BY missions_completed DESC
LIMIT 10;

-- Cost breakdown
SELECT
    service,
    SUM(cost_usd) as total_cost
FROM metrics.costs
WHERE timestamp > NOW() - INTERVAL '30 days'
GROUP BY service;
```

---

## Communication Patterns

### Message Types

```typescript
// Message format for all Hive Mind communication

interface HiveMessage {
  // Standard fields
  id: string;                    // Unique message ID
  type: MessageType;             // Message type
  timestamp: string;             // ISO 8601 timestamp
  source: MessageSource;         // Who sent it

  // Routing
  agent_id: string;              // Target agent ID (if applicable)
  correlation_id?: string;        // For request/response correlation

  // Payload
  payload: Record<string, any>;  // Message-specific data

  // Quality of Service
  priority: Priority;            // LOW, NORMAL, HIGH, URGENT
  ttl?: number;                  // Time-to-live (seconds)
  requires_ack?: boolean;        // Require acknowledgment
}

enum MessageType {
  // Minecraft → Worker (tactical)
  TACTICAL_REQUEST = "tactical_request",
  EMERGENCY_CHECK = "emergency_check",
  COMBAT_REFLEX = "combat_reflex",

  // Worker → Foreman (sync)
  STATE_SYNC = "state_sync",
  MISSION_COMPLETE = "mission_complete",
  ERROR_REPORT = "error_report",

  // Foreman → Worker (mission dispatch)
  MISSION_ASSIGN = "mission_assign",
  MISSION_UPDATE = "mission_update",
  COORDINATION_REQUEST = "coordination_request",

  // Bidirectional
  HEARTBEAT = "heartbeat",
  PING = "ping",
  PONG = "pong"
}

enum MessageSource {
  MINECRAFT_MOD = "minecraft_mod",
  LOCAL_FOREMAN = "local_foreman",
  CLOUDFLARE_WORKER = "cloudflare_worker",
  AI_GATEWAY = "ai_gateway"
}

enum Priority {
  LOW = 1,        // Telemetry, logging
  NORMAL = 5,     // Regular messages
  HIGH = 10,      // Mission updates
  URGENT = 20     // Emergencies
}
```

### Communication Flows

#### 1. Minecraft → Worker (Tactical)

```
┌─────────────────┐                  ┌─────────────────┐
│  Minecraft Mod  │                  │  Cloudflare     │
│                 │                  │  Worker         │
└────────┬────────┘                  └────────┬────────┘
         │                                    │
         │ 1. POST /tactical/check_emergency  │
         │    {                                │
         │      "position": [100, 64, 200],    │
         │      "nearby_blocks": [...],        │
         │      "nearby_entities": [...]       │
         │    }                                │
         │────────────────────────────────────>│
         │                                    │
         │                         2. Process at edge
         │                         (<10ms)
         │                                    │
         │ 3. Response                        │
         │    {                                │
         │      "danger": true,                │
         │      "type": "lava",                │
         │      "action": "stop",              │
         │      "latency_ms": 5                │
         │    }                                │
         │<────────────────────────────────────│
         │                                    │
         │ 4. Execute STOP action             │
```

#### 2. Worker → Foreman (Sync)

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Cloudflare     │    │  Minecraft Mod  │    │  Local Foreman  │
│  Worker         │    │                 │    │  (Jupyter)       │
└────────┬────────┘    └────────┬────────┘    └────────┬────────┘
         │                     │                     │
         │ 1. Agent state    │                     │
         │    changed         │                     │
         │                     │                     │
         │ 2. POST /sync     │                     │
         │    {              │                     │
         │      "position":  │                     │
         │        [101,64,200],                   │
         │      "health": 18,                     │
         │      "inventory": {...}                │
         │    }             │                     │
         │────────────────────>│                     │
         │                     │                     │
         │                     │ 3. Batch sync     │
         │                     │    every 5s        │
         │                     │────────────────────>│
         │                     │                     │
         │                     │            4. Update world model
         │                     │                     │
         │                     │ 5. Ack            │
         │                     │<────────────────────│
         │ 6. 200 OK         │                     │
         │<────────────────────│                     │
```

#### 3. Foreman → Worker (Mission Dispatch)

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Local Foreman  │    │  Minecraft Mod  │    │  Cloudflare     │
│  (Jupyter)       │    │                 │    │  Worker         │
└────────┬────────┘    └────────┬────────┘    └────────┬────────┘
         │                     │                     │
         │ 1. Complex build  │                     │
         │    plan ready      │                     │
         │                     │                     │
         │ 2. Generate tasks  │                     │
         │                     │                     │
         │ 3. POST /mission  │                     │
         │    {              │                     │
         │      "agent_id": "steve_1",            │
         │      "type": "build",                  │
         │      "blueprint": {...},               │
         │      "priority": 10                    │
         │    }             │                     │
         │─────────────────────────────────────────>│
         │                     │                     │
         │                     │                     │
         │                     │            4. Store in DO
         │                     │                     │
         │ 5. 201 Created    │                     │
         │<─────────────────────────────────────────│
         │                     │                     │
         │ 6. Notify mod     │                     │
         │────────────────────>│                     │
         │                     │                     │
         │                     │ 7. GET /mission   │
         │                     │────────────────────>│
         │                     │                     │
         │                     │ 8. Mission data   │
         │                     │<────────────────────│
```

### Protocols

#### WebSocket Protocol (Real-time)

```javascript
// WebSocket message format
{
  "id": "msg_123",
  "type": "tactical_request",
  "timestamp": "2026-02-27T10:30:00Z",
  "source": "minecraft_mod",
  "agent_id": "steve_1",
  "payload": {
    "action": "check_emergency",
    "position": [100, 64, 200],
    "context": {...}
  },
  "priority": 20,
  "requires_ack": true
}

// Acknowledgment format
{
  "id": "msg_123_ack",
  "type": "ack",
  "timestamp": "2026-02-27T10:30:00.010Z",
  "source": "cloudflare_worker",
  "ack_for": "msg_123",
  "received_at": "2026-02-27T10:30:00.005Z"
}
```

#### HTTP REST API (Batch operations)

```http
# Sync agent state (batch)
POST /api/v1/agents/batch-sync HTTP/1.1
Host: workers.your-domain.com
Content-Type: application/json

{
  "agents": [
    {
      "id": "steve_1",
      "position": [100, 64, 200],
      "health": 18,
      "inventory": {"minecraft:stone": 64}
    },
    {
      "id": "steve_2",
      "position": [105, 64, 202],
      "health": 20,
      "inventory": {"minecraft:oak_log": 32}
    }
  ]
}

# Response
HTTP/1.1 200 OK
Content-Type: application/json

{
  "synced": 2,
  "failed": 0,
  "errors": []
}
```

---

## Latency Analysis

### Decision Latency Breakdown

| Decision Type | Path | Latency | Components |
|--------------|------|---------|------------|
| **Emergency Check** | MC → Worker | 5-10ms | Network (2-3ms) + Worker compute (2-7ms) |
| **Combat Reflex** | MC → Worker (AI) | 8-15ms | Network + Workers AI inference |
| **Quick Move** | MC → Worker | 5-10ms | Network + rule-based check |
| **Block Validation** | MC → Worker | 5-10ms | Network + spatial query |
| **Build Planning** | MC → Local Foreman | 200-500ms | Local simulation |
| **Multi-Agent Coord** | MC → Local → Workers | 300-800ms | Planning + dispatch |
| **Pathfinding** | MC → Local Foreman | 100-300ms | A* on world state |
| **Resource Search** | MC → Local (Vectorize) | 150-400ms | Vector search + retrieval |

### Network Latency by Region

| Region | Latency to Worker | Latency to Local Foreman |
|--------|------------------|-------------------------|
| **Same City** | 5-10ms | N/A (local) |
| **Same Continent** | 20-50ms | N/A (local) |
| **Cross-Continent** | 100-200ms | N/A (local) |
| **Offline Mode** | N/A | N/A (local only) |

### Optimization Strategies

**1. Edge Decision Cache**
```python
# Cache frequently-made tactical decisions
@cache(ttl=60)  # Cache for 60 seconds
async def check_emergency_cached(agent_id, position_hash):
    """Check emergency with cached result"""
    return await check_emergency(position_hash)
```

**2. Prefetch State**
```python
# Prefetch likely-needed state
async def prefetch_neighbor_positions(agent_id):
    """Prefetch positions of nearby agents"""
    for neighbor_id in get_nearby_agents(agent_id, radius=50):
        await prefetch_durable_object(neighbor_id)
```

**3. Batch Updates**
```python
# Batch multiple updates into single request
async def batch_sync_agents(agent_states: list):
    """Sync multiple agents in one request"""
    return await worker_api.post("/agents/batch-sync", json={
        "agents": agent_states
    })
```

**4. Predictive Loading**
```python
# Predict what agent will need next
async def predict_and_prefetch(agent_id):
    """Based on current mission, prefetch needed data"""
    mission = await get_current_mission(agent_id)

    if mission.type == "mine":
        # Prefetch resource locations
        await vectorize_search(mission.resource_type, limit=10)
    elif mission.type == "build":
        # Prefetch similar build plans
        await vectorize_search(mission.blueprint_id, limit=5)
```

---

## Deployment Architecture

### Local Deployment

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Local Machine (Game Server)                    │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  Minecraft Forge Server                                             │
│  ├─ MineWright Mod (Java)                                           │
│  │  ├─ ForemanEntity                                               │
│  │  ├─ ActionExecutor                                              │
│  │  └─ CrewManager                                                │
│  └─ Port 25565 (default)                                           │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  │ HTTP/WebSocket (localhost)
                                  │
┌─────────────────────────────────────────────────────────────────────┐
│  Jupyter Kernel Gateway                                             │
│  ├─ Port 8888                                                      │
│  ├─ Python 3.11+ Kernels                                            │
│  │  ├─ Physics Kernel                                              │
│  │  ├─ World Kernel                                                │
│  │  └─ Combat Kernel                                               │
│  └─ WebSocket + REST API                                           │
└─────────────────────────────────────────────────────────────────────┘

Deployment:
```bash
# Install Jupyter Kernel Gateway
pip install jupyter_kernel_gateway

# Start gateway
jupyter kernelgateway \
  --KernelManager.kernel_cmd=['python', '-m', 'ipykernel_launcher', '-f', '{connection_file}'] \
  --IP 0.0.0.0 \
  --Port 8888 \
  --api='jupyter-websocket' \
  --token='YOUR_SECURE_TOKEN'

# Or use Docker
docker run -d \
  -p 8888:8888 \
  -e JUPYTER_TOKEN='YOUR_SECURE_TOKEN' \
  -v $(pwd)/kernels:/app/kernels \
  jupyter-kernel-gateway:latest
```

Configuration (`config/steve-common.toml`):
```toml
[jupyter]
enabled = true
gateway_url = "http://localhost:8888"
gateway_token = "YOUR_SECURE_TOKEN"
kernel_name = "python3"
timeout_ms = 5000  # Timeout for simulation requests

[jupyter.kernels]
# Number of persistent kernels to maintain
physics = 1
world = 2
combat = 1
```

### Cloudflare Deployment

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Cloudflare Global Network                        │
│                    (300+ locations worldwide)                       │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  Cloudflare Services                                                 │
│  ├─ Workers (Python)                                                │
│  │  └─ src/index.py (Tactical reflex)                               │
│  ├─ Durable Objects (Per-agent state)                               │
│  ├─ Vectorize (Semantic search)                                     │
│  ├─ KV (Mission cache)                                              │
│  └─ Workers AI (Fast inference)                                     │
└─────────────────────────────────────────────────────────────────────┘
```

Deployment:
```bash
# Install Wrangler CLI
npm install -g wrangler

# Login to Cloudflare
wrangler login

# Deploy to Cloudflare
cd cloudflare
wrangler deploy

# Set secrets
wrangler secret put JUPITER_GATEWAY_URL
wrangler secret put JUPITER_GATEWAY_TOKEN

# View logs
wrangler tail

# View metrics
wrangler analytics --format=json
```

Configuration (`cloudflare/wrangler.toml`):
```toml
name = "minecraft-agent-reflex"
main = "src/index.py"
compatibility_date = "2024-01-01"
type = "python"

[vars]
ENVIRONMENT = "production"
LOG_LEVEL = "info"
JUPITER_GATEWAY_URL = "http://localhost:8888"  # Local Foreman URL

[[durable_objects.bindings]]
name = "AGENT_STATE"
class_name = "AgentState"

[ai]
binding = "AI"

[[vectorize.bindings]]
name = "VECTORIZE"
index_name = "minecraft-knowledge"

[[kv_namespaces]]
binding = "MISSION_CACHE"
id = "mission_cache_id"

[triggers]
crons = ["*/5 * * * *"]  # Heartbeat every 5 minutes
```

### Network Topology

```
┌───────────────────────────────────────────────────────────────────┐
│                         Global Network                              │
└───────────────────────────────────────────────────────────────────┘

  Local Server                Cloudflare Edge              Minecraft Clients
       │                            │                             │
       │ 5-10ms                    │ 5-10ms                      │
       ▼                            ▼                             │
  ┌─────────┐              ┌─────────────┐                   │
  │ Jupyter │─────────────▶│  Worker     │─────────────────────▶│
  │ Gateway │              │  (Nearest   │                   │
  │         │◀──────────────│   Edge)     │◀──────────────────────│
  └─────────┘  100-500ms   └─────────────┘     5-50ms
       │                            │
       │                            │
       ▼                            ▼
  ┌─────────┐              ┌─────────────┐
  │ Python  │              │   Workers   │
  │ Kernels │              │  Services   │
  │         │              │  - DO       │
  │         │              │  - AI       │
  │         │              │  - Vectorize│
  └─────────┘              └─────────────┘
```

---

## Cost Analysis

### Cloudflare Workers Pricing

**Workers (Python):**
- $5/month for first 100k requests
- $0.50 per million requests thereafter
- Free tier: 100k requests/day

**Durable Objects:**
- $0.20 per GB-month storage
- $0.0004 per read request
- $0.002 per write request
- $0.004 per delete request

**Workers AI:**
- Llama 3.1 8B: $0.005 per 1M input tokens, $0.015 per 1M output tokens
- Mixtral 8x7B: $0.015 per 1M input tokens, $0.045 per 1M output tokens

**Vectorize:**
- $0.20 per million vector dimensions stored/month
- $0.50 per million embeddings searched

**KV Storage:**
- $0.50 per GB-month
- $0.005 per 10k reads
- $0.05 per 10k writes

### Cost Estimation for 10 Agents

**Assumptions:**
- 10 active agents
- 8 hours gameplay/day
- 60 tactical decisions/agent/hour
- 5 strategic decisions/agent/hour
- 1 mission completion/agent/hour

**Calculation:**

```
Tactical Decisions (Workers):
- 10 agents × 8 hours × 60 decisions = 4,800 decisions/day
- 4,800 × 30 days = 144,000 decisions/month
- Cost: 144k × $0.50/1M = $0.07/month

Strategic Decisions (Local Foreman):
- No direct cost (runs locally)

Durable Objects (State):
- 10 agents × 1 KB state × 30 days = 300 KB-month
- Writes: 10 agents × 100 writes/day = 3,000 writes/month
- Reads: 10 agents × 500 reads/day = 15,000 reads/month
- Storage: 300KB × $0.20/GB = $0.00006
- Writes: 3,000 × $0.002 = $6.00
- Reads: 15,000 × $0.0004 = $6.00
- Total DO: ~$12.00/month

Workers AI (Combat Reflex):
- 10 agents × 5 AI decisions/hour × 8 hours = 400 AI decisions/day
- 400 × 30 = 12,000 decisions/month
- Avg 100 tokens × 12,000 = 1.2M tokens
- Cost: 1.2M × $0.005/1M = $0.006/month

Vectorize (Knowledge):
- 100 resources + 50 builds + 100 encounters = 250 vectors
- 250 × 1536 dimensions = 384,000 dimensions
- Storage: 384K × $0.20/1M = $0.08/month
- Searches: 100 × 30 = 3,000 searches/month
- Cost: 3,000 × $0.50/1M = $0.0015/month

KV Cache (Missions):
- 100 missions × 1 KB = 100 KB
- Storage: 100KB × $0.50/GB = $0.00005/month
- Reads/Writes: Negligible (<$0.01)

TOTAL ESTIMATED COST: $12.16/month
```

### Cost Optimization

**1. Batch State Updates**
```python
# Instead of updating every action (10s), batch updates every minute
# Reduces Durable Object writes by 6x
await batch_sync_agents(agent_states)  # 500 writes → 80 writes
```

**2. Cache Tactical Decisions**
```python
# Cache emergency checks for same position
# Reduces Workers requests by 50%+
@cache(ttl=60)
async def check_emergency(agent_id, position_hash):
    ...
```

**3. Use Local for Strategic**
```python
# Keep complex planning local (no cost)
# Only use edge for time-critical decisions
if urgency > 0.7:
    use_cloudflare_worker()  # $0.000001/decision
else:
    use_local_foreman()  # Free (runs locally)
```

**4. Optimize AI Usage**
```python
# Use rule-based decisions when possible
# Only use Workers AI for complex combat
if combat_complexity > 0.7:
    use_workers_ai()  # $0.005/1M tokens
else:
    use_rules()  # Free
```

---

## Implementation Phases

### Phase 1: Basic Worker for Tactical (Week 1-2)

**Goal:** Deploy first Cloudflare Worker for emergency checks

**Tasks:**
1. Create Cloudflare Worker project
2. Implement `/tactical/check_emergency` endpoint
3. Add emergency detection (lava, cliff, mob)
4. Deploy to Cloudflare
5. Test from Minecraft mod

**Deliverables:**
- Working Cloudflare Worker
- Sub-10ms emergency checks
- Basic observability logging

**Testing:**
```java
@Test
void testEmergencyCheck() {
    EmergencyCheckRequest request = new EmergencyCheckRequest(
        new BlockPos(100, 64, 200),
        List.of(new BlockState("lava", new BlockPos(101, 64, 200))),
        List.of()
    );

    EmergencyCheckResponse response = workerClient.checkEmergency(request);

    assertTrue(response.isDanger());
    assertEquals("lava", response.getDangerType());
    assertTrue(response.getLatencyMs() < 20);
}
```

### Phase 2: Durable Objects for State (Week 3-4)

**Goal:** Store agent state in Durable Objects

**Tasks:**
1. Define AgentState schema
2. Implement Durable Object class
3. Add state sync endpoints
4. Implement periodic sync (every 5s)
5. Add mission storage

**Deliverables:**
- Per-agent state in edge DO
- State sync working
- Mission assignment working

### Phase 3: Vectorize for Memory (Week 5-6)

**Goal:** Vector database for world knowledge

**Tasks:**
1. Create Vectorize collections
2. Implement resource location search
3. Add build plan similarity search
4. Store combat encounters
5. Implement learning loop

**Deliverables:**
- Vector collections created
- Semantic search working
- Learning from past decisions

### Phase 4: Full Hive Mind Integration (Week 7-8)

**Goal:** Complete Hive Mind system with decision routing

**Tasks:**
1. Implement decision routing logic
2. Add fallback to local mode
3. Implement batch operations
4. Add observability (AI Gateway)
5. Performance optimization

**Deliverables:**
- Complete Hive Mind system
- Decision routing working
- Observability dashboard
- Production ready

---

## Fallback Scenarios

### 1. Cloudflare Unavailable → Local-Only Mode

```java
public class HiveMindClient {
    private final CloudflareWorkerClient workerClient;
    private final LocalForemanClient localClient;

    public DecisionResponse makeDecision(DecisionRequest request) {
        try {
            // Try edge first (fast)
            return workerClient.makeDecision(request);
        } catch (CloudflareUnavailableException e) {
            // Fallback to local
            MineWrightMod.LOGGER.warn("Cloudflare unavailable, using local mode");
            return localClient.makeDecision(request);
        }
    }
}
```

### 2. High Latency → Increase Tick Interval

```python
async def handle_high_latency():
    """When latency > 100ms, reduce decision frequency"""
    latency = await measure_latency()

    if latency > 100:
        # Reduce tactical check frequency
        global TACTICAL_CHECK_INTERVAL
        TACTICAL_CHECK_INTERVAL = 2  # Check every 2 ticks instead of 1

        # Cache decisions longer
        cache_ttl = 120  # 2 minutes instead of 1
```

### 3. Rate Limited → Queue and Batch

```python
class RequestQueue:
    """Queue requests when rate limited"""

    async def submit(self, request):
        if self.is_rate_limited():
            await self.queue.put(request)
            return

        # Process immediately
        return await self.process(request)

    async def flush_queue(self):
        """Flush queued requests in batch"""
        if self.queue.empty():
            return

        requests = await self.queue.get_batch(size=100)
        return await self.batch_process(requests)
```

### 4. Worker Error → Graceful Degradation

```python
async def handle_worker_error(error):
    """Degrade gracefully when Worker fails"""
    if isinstance(error, WorkerTimeoutError):
        # Use cached decision
        return await get_cached_decision()

    elif isinstance(error, WorkerRateLimitError):
        # Queue and retry later
        await queue_decision()
        return DEFAULT_DECISION

    else:
        # Full fallback to local
        return await local_foreman.decide()
```

---

## Reference Links

### Related Research Documents

- **Multi-Agent Coordination:** `docs/research/MULTI_AGENT_COORDINATION.md`
  - Contract Net Protocol implementation
  - Blackboard pattern for shared state
  - Swarm optimization for task allocation

- **Mental Simulation:** `docs/research/JUPYTER_MENTAL_SIMULATION.md`
  - Jupyter Kernel Gateway integration
  - World model architecture
  - Simulation cache strategies

- **Simulation Architecture:** `docs/research/SIMULATION_ARCHITECTURE.md`
  - Voxel world snapshots
  - Risk assessment engine
  - Alternative plan generation

### Cloudflare Documentation

- **Workers:** https://developers.cloudflare.com/workers/
- **Durable Objects:** https://developers.cloudflare.com/durable-objects/
- **Vectorize:** https://developers.cloudflare.com/vectorize/
- **Workers AI:** https://developers.cloudflare.com/workers-ai/

### Code Examples

- **Jupyter Client:** `C:\Users\casey\steve\docs\research\JUPYTER_MENTAL_SIMULATION.md`
  - Complete Java WebSocket client
  - Message protocol implementation
  - Mental simulation service

- **Multi-Agent Patterns:** `C:\Users\casey\steve\docs\research\MULTI_AGENT_COORDINATION.md`
  - Contract Net implementation
  - Auction-based task allocation
  - Blackboard pattern examples

### Configuration Files

- **Cloudflare Wrangler:** `C:\Users\casey\steve\cloudflare\wrangler.toml`
- **Minecraft Config:** `C:\Users\casey\steve\config\steve-common.toml`

---

## Appendix A: Example Flows

### A.1 Complete Combat Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          COMBAT SCENARIO                               │
└─────────────────────────────────────────────────────────────────────────┘

1. SENSE
   Minecraft: Agent detects hostile mob (zombie) at 5 blocks distance

2. FILTER (Urgency Check)
   Mod: urgency = calculate_urgency(mob_distance=5, mob_type="zombie")
       = 0.75 (HIGH)

3. THINK - Decision Route
   Mod: Since urgency > 0.7 → Use Cloudflare Worker

4. ACT - Worker Request
   Mod → Worker: POST /tactical/combat_reflex
   {
     "health": 15,
     "equipment": {"weapon": "stone_sword"},
     "nearby_mobs": [{"type": "zombie", "distance": 5}]
   }

5. Worker Processing
   Worker: Check rules (simple case)
   - Health OK, weapon OK → Use rule-based
   - Decision: ATTACK
   - Latency: 6ms

6. Worker Response
   Worker → Mod: {
     "action": "attack",
     "reason": "Engage zombie (favorable)",
     "latency_ms": 6
   }

7. ACT - Minecraft
   Mod: Execute attack action
   - Pathfind to zombie
   - Hit with sword
   - Monitor health during combat

8. LEARN
   Mod → Worker: POST /complete (after combat)
   Worker: Update agent stats, store in Vectorize
   - Outcome: Victory
   - Tactics: Aggressive melee
   - Learned: "Can solo zombie with stone sword"
```

### A.2 Complex Building Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     COMPLEX BUILD SCENARIO                              │
└─────────────────────────────────────────────────────────────────────────┘

1. SENSE
   Player: "Build a stone house at [100, 64, 200]"

2. FILTER
   Mod: complexity = estimate_complexity("build stone house")
       = 0.85 (HIGH)
       urgency = 0.1 (LOW)

3. THINK - Decision Route
   Mod: High complexity, low urgency → Use Local Foreman

4. ACT - Foreman Request
   Mod → Foreman: POST /simulate/build_plan
   {
     "blueprint": {
       "type": "house",
       "material": "stone",
       "position": [100, 64, 200],
       "size": [10, 6, 10]
     },
     "world_state": {...}
   }

5. Foreman Processing (Jupyter)
   Physics Kernel: Check structural integrity
   - Result: Stable (risk_score: 0.05)

   World Kernel: Estimate resources
   - Stone needed: 960 blocks
   - Available: 64 blocks
   - Gap: 896 blocks

   World Kernel: Optimize build sequence
   - Generate 120 placement actions
   - Optimize for minimal movement

   Coordinator: Assign tasks to 3 agents
   - Agent 1: Walls (north, west)
   - Agent 2: Walls (south, east)
   - Agent 3: Roof

6. Foreman Response
   Foreman → Mod: {
     "success": true,
     "plan": {
       "agents": 3,
       "actions": 120,
       "resources_needed": {"stone": 896},
       "estimated_time": 450,  // seconds
       "risk_score": 0.05
     },
     "latency_ms": 380
   }

7. ACT - Dispatch to Workers
   Foreman → Workers: POST /mission for each agent
   Worker: Store mission in Durable Objects

8. ACT - Minecraft
   Mod: Agents start building
   - Each agent fetches mission from Worker
   - Executes actions tick-by-tick
   - Reports progress every 5s

9. LEARN
   Mod → Foreman: Build completed in 420 seconds
   Foreman: Update Vectorize
   - "Stone house at [100,64,200] - Success"
   - Store for future reference
```

### A.3 Emergency Avoidance Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      EMERGENCY AVOIDANCE SCENARIO                        │
└─────────────────────────────────────────────────────────────────────────┘

1. SENSE
   Minecraft: Agent walking, about to step into lava

2. FILTER (Emergency Detection)
   Mod: detect_emergency() → true
       urgency = 1.0 (CRITICAL)

3. THINK - Immediate Edge Route
   Mod: CRITICAL → Use Cloudflare Worker immediately

4. ACT - Worker Request (sub-10ms)
   Mod → Worker: POST /tactical/check_emergency
   {
     "position": [100, 63, 200],  # About to step here
     "nearby_blocks": [
       {"type": "lava", "pos": [100, 63, 201]}
     ],
     "nearby_entities": []
   }

5. Worker Processing (FAST)
   Worker: Check adjacency
   - Lava at [100, 63, 201] is adjacent
   - DANGER: True

   Worker: Generate response
   - Action: STOP
   - Reason: "Lava adjacent"
   - Latency: 4ms

6. Worker Response
   Worker → Mod: {
     "danger": true,
     "type": "lava",
     "action": "stop",
     "reason": "Lava adjacent",
     "latency_ms": 4
   }

7. ACT - Immediate Stop
   Mod: Stop agent immediately
   - Cancel movement
   - Agent stays safe

8. LEARN
   Mod: Update local memory
   - "Lava at [100, 63, 201] - AVOID"

   Worker: No sync needed (emergency)
```

---

## Glossary

| Term | Definition |
|------|------------|
| **Hive Mind** | Hybrid AI architecture combining local strategic planning with edge tactical reflexes |
| **Local Foreman** | Jupyter Kernel Gateway running locally for strategic planning |
| **Tactical Edge** | Cloudflare Workers for sub-10ms tactical decisions |
| **Durable Objects** | Cloudflare's per-object storage for agent state |
| **Vectorize** | Cloudflare's vector database for semantic search |
| **Working Memory** | Short-term state stored in Durable Objects |
| **Long-term Memory** | Knowledge stored in Vectorize |
| **Think-Act Loop** | Sense → Think → Act → Learn cycle |
| **Urgency** | 0-1 score indicating time criticality |
| **Complexity** | 0-1 score indicating computational difficulty |
| **Fallback** | Graceful degradation when services unavailable |

---

**Document Status:** Design Complete
**Next Steps:** Phase 1 Implementation
**Version:** 1.0
**Last Updated:** 2026-02-27
