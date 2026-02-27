# Gold Farm Architecture - Technical Deep Dive

## System Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     MineWright Gold Farm System                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────┐      ┌──────────────┐      ┌─────────────┐   │
│  │   Player    │─────▶│  Foreman GUI │─────▶│ Foreman API │   │
│  └─────────────┘      └──────────────┘      └──────┬──────┘   │
│                                                     │           │
│                                                     ▼           │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │              ActionExecutor (Task Queue)                 │  │
│  ├─────────────────────────────────────────────────────────┤  │
│  │                                                           │  │
│  │  Queue: [BuildGoldFarm] → [OperateGoldFarm] → [Collect] │  │
│  │                                                           │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                     │           │
│                                                     ▼           │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                   Gold Farm Actions                       │  │
│  ├─────────────────────────────────────────────────────────┤  │
│  │                                                           │  │
│  │  ┌────────────────┐  ┌────────────────┐                 │  │
│  │  │ BuildGoldFarm  │  │ MonitorGoldFarm│                 │  │
│  │  │   Action       │  │    Action      │                 │  │
│  │  └────────────────┘  └────────────────┘                 │  │
│  │                                                           │  │
│  │  ┌────────────────┐  ┌────────────────┐                 │  │
│  │  │OperateGoldFarm │  │ CollectGold    │                 │  │
│  │  │    Action      │  │    Action      │                 │  │
│  │  └────────────────┘  └────────────────┘                 │  │
│  │                                                           │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                     │           │
│                                                     ▼           │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                   Infrastructure                          │  │
│  ├─────────────────────────────────────────────────────────┤  │
│  │                                                           │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │  │
│  │  │WorldKnowledge│  │PortalDetector│  │GoldFarmGen   │  │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  │  │
│  │                                                           │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Flow

### Command Flow

```
Player Input
    │
    ▼
Natural Language Command ("build a gold farm")
    │
    ▼
TaskPlanner.planTasksAsync()
    │
    ├─▶ PromptBuilder.buildSystemPrompt()
    ├─▶ LLM Request (OpenAI/Groq/Gemini)
    ├─▶ ResponseParser.parseResponse()
    │
    ▼
ActionExecutor.queueTask()
    │
    ▼
BuildGoldFarmAction.onStart()
    │
    ├─▶ PortalDetector.findPortals()
    ├─▶ GoldFarmGenerators.generatePortalTrap()
    ├─▶ CollaborativeBuildManager.assignSections()
    │
    ▼
BuildGoldFarmAction.onTick()
    │
    ├─▶ Place blocks (5 per tick)
    ├─▶ Report progress (every 100 blocks)
    ├─▶ Complete when finished
    │
    ▼
ActionResult.success()
    │
    ▼
Player Notification
```

---

## State Machine

### Gold Farm Operation States

```
┌─────────┐
│  IDLE   │◀────────────────────────────────────┐
└────┬────┘                                     │
     │                                          │
     │ Player Command                           │
     ▼                                          │
┌─────────┐                                   │
│DETECTING│                                   │
│ PORTAL  │                                   │
└────┬────┘                                   │
     │                                          │
     │ Portal Found                             │
     ▼                                          │
┌─────────┐                                   │
│BUILDING │                                   │
│  TRAP   │                                   │
└────┬────┘                                   │
     │                                          │
     │ Build Complete                          │
     ▼                                          │
┌─────────┐                                   │
│OPERATING│                                   │
│  FARM   │                                   │
└────┬────┘                                   │
     │                                          │
     │  ┌────────────────┐                     │
     │  │                │                     │
     │  ▼                ▼                     │
     │ ┌──────┐      ┌──────┐                 │
     │ │KILLING│      │COLLECT│               │
     │ │PIGLINS│      │ ITEMS │               │
     │ └──────┘      └──────┘                 │
     │  │                │                     │
     └──┴────────────────┘                     │
     │                                          │
     │ Session Complete / Error                 │
     ▼                                          │
┌─────────┐                                   │
│COMPLETE │                                   │
└─────────┘                                   │
     │                                          │
     └──────────────────────────────────────────┘
```

---

## Class Diagram

### Core Classes

```
┌─────────────────────────────────────────────────────────────┐
│                        BaseAction                           │
├─────────────────────────────────────────────────────────────┤
│ - foreman: ForemanEntity                                    │
│ - task: Task                                                │
│ - result: ActionResult                                      │
│                                                             │
│ + onStart(): void                                           │
│ + onTick(): void                                            │
│ + onCancel(): void                                          │
│ + isComplete(): boolean                                     │
└─────────────────────────────────────────────────────────────┘
                            △
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────┴────────┐  ┌───────┴────────┐  ┌──────┴───────┐
│BuildGoldFarm  │  │OperateGoldFarm │  │CollectGold   │
│    Action     │  │    Action      │  │   Action     │
├───────────────┤  ├────────────────┤  ├──────────────┤
│ - phase: enum │  │ - killChamber: │  │ - goldCount: │
│ - buildPlan:  │  │   BlockPos     │  │   int        │
│   List<>      │  │ - target:      │  │ - ticks: int │
│ - blocks: int │  │   Zombified... │  │              │
├───────────────┤  ├────────────────┤  ├──────────────┤
│ + detect...() │  │ + triggerAggro()│  │ + collect()  │
│ + design...() │  │ + killPiglins()│  │ + report()   │
│ + construct() │  │ + collect()    │  │              │
└───────────────┘  └────────────────┘  └──────────────┘
```

### Utility Classes

```
┌─────────────────────────────────────────────────────────────┐
│                     PortalDetector                           │
├─────────────────────────────────────────────────────────────┤
│ - level: Level                                              │
│ - center: BlockPos                                          │
│ - searchRadius: int                                         │
│                                                             │
│ + findPortals(): List<PortalInfo>                           │
│ - analyzePortal(BlockPos): PortalInfo                       │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ uses
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      PortalInfo                              │
├─────────────────────────────────────────────────────────────┤
│ - location: BlockPos                                        │
│ - width: int                                                │
│ - height: int                                               │
│ - isXAxis: boolean                                          │
│                                                             │
│ + canSpawnPiglins(): boolean                                │
│ + getOptimalTrapPos(): BlockPos                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   GoldFarmGenerators                         │
├─────────────────────────────────────────────────────────────┤
│ + generatePortalTrap(): List<BlockPlacement>                │
│ + generateSimpleTrap(): List<BlockPlacement>                │
│ - buildKillChamber(): List<BlockPlacement>                  │
│ - buildSpawnPlatforms(): List<BlockPlacement>               │
│ - buildCollectionSystem(): List<BlockPlacement>             │
└─────────────────────────────────────────────────────────────┘
```

---

## Spawn Mechanics

### Zombified Piglin Spawn Algorithm

```
┌─────────────────────────────────────────────────────────────┐
│              Zombified Piglin Spawn Logic                    │
└─────────────────────────────────────────────────────────────┘

Every tick (20 times per second):
    │
    ├─ For each portal in loaded chunks:
    │   │
    │   ├─ For each portal frame block:
    │   │   │
    │   │   ├─ Random spawn attempt (0.05% chance)
    │   │   │
    │   │   └─ If successful:
    │   │       │
    │   │       ├─ Check spawn conditions:
    │   │       │   ├─ Is it dark enough? (light ≤ 7)
    │   │       │   ├─ Is there space? (2x2x2 area)
    │   │       │   └─ Is player within 16 blocks?
    │   │       │
    │   │       └─ Spawn zombified piglin:
    │   │           ├─ 95% adult
    │   │           ├─ 5% baby (faster, 1x1 gap)
    │   │           └─ Position: random near portal
    │   │
    │   └─ Repeat for each frame block
    │
    └─ Result: More portal blocks = more spawn attempts

Example 2x3 portal (21 blocks):
    21 spawn attempts per tick
    ~1 spawn every 2-3 seconds
    ~20-30 spawns per minute

Example 4x5 portal (62 blocks):
    62 spawn attempts per tick
    ~3 spawns per second
    ~100-150 spawns per minute
```

### Spawn Platform Optimization

```
Optimal Spawn Platform Design:
    │
    ├─ Distance: 2-4 blocks from portal frame
    ├─ Size: 5x5 minimum (allows for 2x2 spawn area)
    ├─ Height: 2 blocks above portal (to catch spawns)
    ├─ Material: Non-full blocks (allows spawn through)
    │   └─ Use: Nether brick fence, glass panes, iron bars
    │
    └─ Hole: 1x1 center hole for piglins to fall

┌─────────────────────────────┐
│                             │
│   [Portal]                  │
│                             │
│   ┌───────────────────┐     │
│   │  # # # # # # #    │     │
│   │  #             #    │     │
│   │  #    ▼       #    │     │  ▼ = Fall hole
│   │  #             #    │     │  # = Fence/barrier
│   │  # # # # # # #    │     │
│   └───────────────────┘     │
│           │                 │
│           ▼                 │
│   ┌───────────┐             │
│   │ Kill      │             │
│   │ Chamber   │             │
│   └───────────┘             │
└─────────────────────────────┘
```

---

## Kill Chamber Design

### Standard Kill Chamber

```
Top View (3x3):
┌─────┬─────┬─────┐
│     │ H   │     │
├─────┼─────┼─────┤
│ H   │ H   │ H   │    H = Hopper (points to chest)
├─────┼─────┼─────┤    C = Chest (under center hopper)
│     │ H/C │     │
└─────┴─────┴─────┘

Side View:
    ┌─────┐
    │Glass│         (3 blocks high, walls)
    ├─────┤
    │Glass│
    ├─────┤
    │Glass│
    ├═══╦═┤
    ║H═╦═╦║         (Hopper floor)
    ╠═══╬══╣
    ║ C ║             (Chest)
    └───┘
    ┌───┐
    ║ H ║             (Overflow hopper)
    └───┘
    ┌───┐
    ║ C ║             (Overflow chest)
    └───┘
```

### Advanced Kill Chamber (5x5)

```
Top View (5x5):
┌───┬───┬───┬───┬───┐
│   │ G │ G │ G │   │
├───┼───┼───┼───┼───┤
│ G │   │   │   │ G │
├───┼───┼───┼───┼───┤
│ G │   │   │   │ G │    G = Glass (walls)
├───┼───┼───┼───┼───┤    H = Hopper
│ G │   │   │   │ G │    S = Stone (floor)
├───┼───┼───┼───┼───┤    . = Air (open)
│   │ G │ G │ G │   │
└───┴───┴───┴───┴───┘

Floor Layout:
┌───┬───┬───┬───┬───┐
│ S │ H │ H │ H │ S │
├───┼───┼───┼───┼───┤
│ H │ . │ . │ . │ H │
├───┼───┼───┼───┼───┤    Hoppers in + pattern
│ H │ . │ . │ . │ H │    Center 3x3 open for agent
├───┼───┼───┼───┼───┤
│ H │ . │ . │ . │ H │
├───┼───┼───┼───┼───┤
│ S │ H │ H │ H │ S │
└───┴───┴───┴───┴───┘
```

---

## Multi-Agent Coordination

### Role Distribution

```
┌─────────────────────────────────────────────────────────────┐
│                   OrchestratorService                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  Foreman     │  │  Builder     │  │  Operator    │     │
│  │  Agent       │  │  Agent       │  │  Agent       │     │
│  ├──────────────┤  ├──────────────┤  ├──────────────┤     │
│  │ - Plans      │  │ - Builds     │  │ - Kills      │     │
│  │ - Coordinates│  │   trap       │  │   piglins    │     │
│  │ - Monitors   │  │ - Maintains  │  │ - Collects   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│         │                  │                  │             │
│         └──────────────────┴──────────────────┘             │
│                     │                                       │
│                     ▼                                       │
│            AgentCommunicationBus                            │
│            (Message Queue)                                  │
└─────────────────────────────────────────────────────────────┘
```

### Task Assignment Flow

```
Player: "build a gold farm"
    │
    ▼
Foreman Agent:
    ├─ Scans for portal
    ├─ Generates build plan
    ├─ Divides into sections
    │
    ├─ Message: TASK_ASSIGNMENT → Builder Agent
    │   Content: "Build north platform"
    │
    ├─ Message: TASK_ASSIGNMENT → Builder Agent 2
    │   Content: "Build south platform"
    │
    └─ Message: TASK_ASSIGNMENT → Builder Agent 3
        Content: "Build kill chamber"
    │
    ▼
Builder Agents:
    ├─ Receive tasks
    ├─ Build in parallel
    ├─ Report progress
    │
    └─ Message: TASK_COMPLETE → Foreman
    │
    ▼
Foreman Agent:
    ├─ Assembles sections
    ├─ Validates build
    ├─ Assigns operation task
    │
    └─ Message: TASK_ASSIGNMENT → Operator Agent
        Content: "Operate farm for 10 minutes"
    │
    ▼
Operator Agent:
    ├─ Moves to kill chamber
    ├─ Begins operation
    ├─ Reports statistics
    │
    └─ Message: TASK_COMPLETE → Foreman
        Content: "Killed 450 piglins, 60 gold ingots"
```

---

## Performance Metrics

### Spawn Rate Calculator

```
Piglin Spawn Rate = PortalBlocks × 0.0005 × 20 ticks/sec × 60 sec

Example Calculations:

2x3 Portal (21 blocks):
    21 × 0.0005 × 20 × 60 = 12.6 spawns/min
    Realistic: 20-30 spawns/min (with platform optimization)

3x4 Portal (34 blocks):
    34 × 0.0005 × 20 × 60 = 20.4 spawns/min
    Realistic: 40-60 spawns/min

4x5 Portal (50 blocks):
    50 × 0.0005 × 20 × 60 = 30 spawns/min
    Realistic: 80-120 spawns/min

5x5 Portal (62 blocks):
    62 × 0.0005 × 20 × 60 = 37.2 spawns/min
    Realistic: 100-150 spawns/min
```

### Gold Income Calculator

```
Gold Income = SpawnsPerMin × AvgGoldPerPiglin × 60 min

Average Drops per Piglin:
    - Gold Nuggets: 0-1 (40% chance) = 0.4 nuggets avg
    - Golden Sword: 0-1 (8.5% chance) = 0.085 swords avg
    - Gold Ingot: 0-1 (rare, with Looting) = 0.01 ingots avg

Total Gold per Piglin:
    (0.4 / 9) + (0.085 × 0.33) + 0.01 = ~0.06 ingots

Example Income (30 spawns/min):
    30 × 0.06 × 60 = 108 ingots/hour

Example Income (100 spawns/min):
    100 × 0.06 × 60 = 360 ingots/hour
```

---

## Error Handling

### Common Failure Scenarios

```
┌─────────────────────────────────────────────────────────────┐
│                    Error Scenarios                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Scenario 1: No Portal Found                               │
│  ├─ Detection: PortalDetector returns empty list           │
│  ├─ Action: Cancel task, notify player                     │
│  └─ Suggestion: "Build a nether portal within 32 blocks"   │
│                                                             │
│  Scenario 2: Portal Too Small                              │
│  ├─ Detection: PortalInfo.width < 2 or height < 3         │
│  ├─ Action: Warn player, continue anyway                   │
│  └─ Result: Reduced spawn rate                             │
│                                                             │
│  Scenario 3: Build Interrupted                             │
│  ├─ Detection: Task cancelled mid-build                    │
│  ├─ Action: Save progress, pause construction              │
│  └─ Recovery: Resume from last placed block                │
│                                                             │
│  Scenario 4: Low Spawn Rate                                │
│  ├─ Detection: < 10 spawns/min after 5 min                 │
│  ├─ Action: Suggest platform improvements                  │
│  └─ Auto-fix: Add more spawn platforms                     │
│                                                             │
│  Scenario 5: Agent Stuck                                   │
│  ├─ Detection: No movement for 40 ticks                    │
│  ├─ Action: Teleport to target position                    │
│  └─ Fallback: Cancel operation, report error               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Testing Checklist

### Unit Tests

```java
@Test
public void testPortalDetector() {
    // Test portal detection
    // Test size calculation
    // Test orientation detection
}

@Test
public void testSpawnRateCalculation() {
    // Test spawn rate math
    // Verify gold income estimates
}

@Test
public void testKillChamberBuild() {
    // Test block placement
    // Verify hopper connections
    // Check chest accessibility
}
```

### Integration Tests

```java
@Test
public void testFullGoldFarmCycle() {
    // 1. Spawn agent
    // 2. Build farm
    // 3. Operate for 5 min
    // 4. Verify collection
    // 5. Check stats
}

@Test
public void testMultiAgentCoordination() {
    // 1. Spawn 3 agents
    // 2. Assign roles
    // 3. Build in parallel
    // 4. Verify no conflicts
}
```

### In-Game Testing

```
Manual Testing Checklist:
    □ Build farm with 2x3 portal
    □ Build farm with 4x5 portal
    □ Test spawn rate (count piglins)
    □ Verify item collection
    □ Test chest overflow
    □ Test operation cancellation
    □ Test agent recovery (stuck scenario)
    □ Measure actual gold income
    □ Test with multiple agents
    □ Verify parallel building
```

---

## Configuration

### Config Options

```toml
[gold_farm]
# Maximum operation time per session (minutes)
max_session_time = 30

# Target piglin count before killing
target_piglin_count = 30

# Auto-restart operation when complete
auto_restart = true

# Kill chamber dimensions
kill_chamber_width = 5
kill_chamber_height = 3
kill_chamber_depth = 5

# Spawn platform design
spawn_platform_size = 5
spawn_platform_distance = 2

# Collection system
use_hopper_system = true
overflow_chests = true
sort_gold_items = false

# Performance
blocks_per_tick = 5
kill_batch_size = 10
report_interval = 300
```

---

## Future Enhancements

### Planned Features

```
Phase 5: Advanced Features (Future)
    ├─ Automatic portal construction
    ├─ Dynamic spawn platform optimization
    ├─ Piglin trading system
    ├─ AFK operation mode
    ├─ Remote monitoring (GUI)
    ├─ Performance analytics
    └─ Gold smelting automation
```

### Experimental Ideas

```
Experimental Concepts:
    ├─ Nether hub network (multiple portals)
    ├─ Portal frame optimization (larger portals)
    ├─ Baby piglin handling (faster spawns)
    ├─ XP collection system
    ├─ Automatic gold block crafting
    └─ Integration with villager trading
```

---

## Conclusion

This architecture provides a robust, scalable foundation for gold farm automation in MineWright. The modular design allows for easy extension and optimization, while the multi-agent coordination enables efficient parallel operations.

Key strengths:
- **Modular**: Each component is independent and testable
- **Scalable**: Supports farms of any size
- **Robust**: Comprehensive error handling and recovery
- **Efficient**: Optimized for high spawn rates and collection
- **Extensible**: Easy to add new features and improvements
