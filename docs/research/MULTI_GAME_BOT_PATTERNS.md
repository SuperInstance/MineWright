# Multi-Game Bot Architecture Patterns Research

**Research Date:** 2026-03-01
**Purpose:** Academic research into game automation architectures across multiple games
**Focus:** Extract reusable patterns for AI agent development

---

## Executive Summary

This document analyzes automation tools and bot architectures from diverse game ecosystems to identify common patterns, architectural innovations, and best practices. Research covers:

1. **Diablo Series** (Demonbuddy, Koolo) - Item management, run systems
2. **OSRS** (Powerbot, Dreambot, Tribot) - Random events, scripting APIs
3. **Guild Wars 2** - Dynamic event handling, navigation
4. **EVE Online** (TinyMiner) - Economy automation, complex state management
5. **Ultima Online** (Razor, UOSteam) - Macro scripting, early automation patterns
6. **MUD Clients** (TinTin++, ZMud) - Trigger/alias foundations

**Key Finding:** Despite spanning 30 years of game automation history, successful bot architectures converge on a small set of core patterns: **event-driven reactive systems**, **hierarchical state management**, **rule-based decision making**, and **graceful error recovery**.

---

## Table of Contents

1. [Individual Bot Analyses](#individual-bot-analyses)
2. [Cross-Game Common Patterns](#cross-game-common-patterns)
3. [Unique Innovations by Game](#unique-innovations-by-game)
4. [Architectural Recommendations](#architectural-recommendations)
5. [Anti-Detection Insights](#anti-detection-insights)
6. [References](#references)

---

## 1. Individual Bot Analyses

### 1.1 Diablo Series Bots

#### **Demonbuddy (Diablo 3)**

**Architecture Overview:**
- Plugin-based system similar to Honorbuddy (World of Warcraft)
- C#/.NET framework with extensibility points
- Object-based game state reading (memory injection/reflection)

**Item Management System:**
- **Pickit System:** Rule-based item filtering using priority queues
- **NIP Files:** Configuration files defining item pickup rules (format: `[Name] == [Item] && [Stat] >= [Value]`)
- **Automatic Stashing:** Sorts items into categories (weapons, armor, gems)
- **Vendor Logic:** Identifies items to sell vs. keep based on rules

**Run System:**
- **Sequential Runs:** Predefined boss runs (Mephisto, Baal, etc.)
- **Checkpoint System:** Saves game state at waypoints for efficiency
- **Death Recovery:** Automatic corpse retrieval and equipment re-equip
- **Statistics Tracking:** Runs per hour, items per hour, XP efficiency

**Error Handling:**
- Stuck detection with timeout thresholds
- Game crash detection and restart
- Inventory full handling (return to town, sell, stash, resume)

#### **Koolo (Diablo II Resurrected)**

**Technology Stack:**
- Written in Go (modernized architecture)
- NIP file-based pickit system in `config/{character}/pickit`
- Discord/Telegram integration for remote monitoring

**Item Management Features:**
- Item identification automation
- Inventory slot locking for important gear
- Automatic gambling with gold management
- Cube recipe automation

**Multi-Bot Coordination:**
- "Companion mode" for party coordination
- Shared pickit priorities across bots
- Leader/follower patterns for efficient runs

---

### 1.2 OSRS Bots

#### **DreamBot**

**Architecture:**
- Java-based client with custom API
- Simple scripting API for rapid development
- Active community with 40+ script repository

**Scripting API Patterns:**
```java
// State-based task execution
public int onLoop() {
    if (getInventory().isFull()) {
        return bank();
    } else if (atResource()) {
        return mine();
    } else {
        return walkToResource();
    }
}
```

**Random Event Handling:**
- Dedicated solvers for each random event
- Pattern recognition for event detection
- Interruptible main loop for immediate event response
- State preservation across event handling

**Anti-Detection Features:**
- Human-like mouse movement curves
- Variable reaction times
- Randomized click positions within targets
- Break handler mimicking human play sessions

#### **OSRSBot/RSB (RuneLite-based)**

**Architecture:**
- RuneLite extension (open-source client modification)
- Minimal API modification for stealth
- Multiple run modes: `--bot`, `--bot-runelite`, `--runelite`

**Detection Methods Used:**
- Color-based game object recognition (OpenCV)
- Text recognition (Tesseract OCR) for UI reading
- Mouse/keyboard simulation (PyAutoGUI)

**Script Components:**
1. **Event Listeners:** Monitor character movement, item interactions
2. **Exception Handlers:** Graceful error recovery
3. **Anti-Detection Modules:** Randomization of behavior
4. **Multithreading:** Parallel task execution

---

### 1.3 Guild Wars 2 Automation

#### **Dynamic Events Challenge**

**Why GW2 is Different:**
- Events replace traditional quest system
- No NPC "quest givers" - events happen organically
- Events scale based on participant count
- Chain reactions: completing one triggers next
- No binary success/failure - outcomes change environment

**Navigation Approaches:**
- NavMesh-based waypoint following
- Event coordinate detection via API
- Path recalculation when events move

**Scout Tools:**
- **Guild Wars 2 Scout:** Mobile app using official APIs
- Tracks dynamic event status in real-time
- WvW battle status monitoring
- Provides JSON data for automation integration

---

### 1.4 EVE Online Bots

#### **TinyMiner - Mining Bot**

**Architecture Overview:**
- Multi-account support (Rorqual/Orca drone coordination)
- Local chat monitoring for anti-detection
- Email notification system for alerts

**Economy Automation:**
- **Ore Hold Monitoring:** Automatic cargo management
- **Belt Rat Detection:** Combat response during mining
- **Ice Belt Finding:** Automatic ice anomaly location
- **Auto-Restart:** Game crash recovery and reconnect

**TinyTrader - Market Bot:**
- Automated buy/sell order updates
- Item selling across multiple stations
- Asset hauling using waypoint navigation
- Price difference arbitrage

**State Management:**
- Complex state machine for mining cycle:
  ```
  IDLE → UNDOCK → WARP_TO_BELT → TARGET_ASTEROID →
  MINING → FULL_CARGO → WARP_TO_STATION → DOCK →
  UNLOAD → IDLE
  ```

**Anti-Detection Features:**
- Local chat monitoring (respond to players)
- Keyword alerts (GM, admin, etc.)
- Randomized activity patterns
- Break scheduling

---

### 1.5 Ultima Online Automation

#### **Razor**

**Historical Significance:**
- Pioneering macro tool (pre-2000)
- Foundation for modern automation patterns
- Open-source with active GitHub community

**Macro Recording:**
- Record and playback sequences
- Command-based scripting language (v1.6.4.2+)
- Hotkey system for skill/spell/item binding

**Scripting Features:**
- **Aliases:** Create custom commands
- **Hotkeys:** Bind complex sequences to single keys
- **Agents:** Automated responses to game events
- **Loop/If/Else:** Basic program flow control

**Example Script:**
```
; Simple mining loop
hotkey F1
  overhead "Mining..."
  useitem "Pickaxe"
  targettile offset 0 0
  wait 2000
loop
```

#### **UOSteam**

**Script Categories:**
- `general/` - Utility scripts
- `organize/` - Item/loot management
- `pvmpvp/` - Combat automation
- `resource/` - Gathering/crafting
- `skill/` - Skill training

**Architecture:**
- Modular script organization
- Profile-based configuration
- Python support in Razor Enhanced

#### **Razor Enhanced**

**Python API Advantages:**
- Full programming language capabilities
- External library support
- REPL for testing
- Submodules for code organization

**Example Python Script:**
```python
# Animal taming training
from System import *
def train_taming():
    while True:
        target = Mobiles.Find('animal')
        if target:
            Spells.Cast('Animal Taming')
            Target.WaitForTarget(2000)
            Target.TargetExecute(target)
            Misc.Pause(3000)
```

---

### 1.6 MUD Automation (TinTin++, ZMud)

#### **Historical Context**

MUD (Multi-User Dungeon) automation in the 1990s prefigured all modern bot patterns. Text-based nature made automation natural and powerful.

#### **TinTin++**

**Core Features:**
- Cross-platform MUD client
- Scripting language with triggers, aliases, automapping
- Split-screen interface

**Triggers:**
```
#action {%1 tells you 'hello'} {say hello %1}
#action {You are hungry} {get bread bag;eat bread}
```

**Aliases:**
```
#alias {gonorth} {send go north}
#alias {heal} {cast 'heal' %0}
```

**Variables:**
```
#variable {hp} {100}
#math {hp} {$hp - 10}
```

**Automation Patterns:**
- **Response Automation:** Auto-reply to players (AFK bot)
- **Combat Automation:** Trigger on enemy attack patterns
- **Pathfinding:** Automated movement through zones
- **Loot Collection:** Trigger on item drop messages

#### **ZMud**

**Features:**
- Triggers (pattern → action)
- Aliases (custom commands)
- Macros (key bindings)
- Variables and databases
- Mapping system
- Button creation for GUI

**Trigger Patterns:**
```
#AC {^Your English name:} {river}
#AC {^Please enter password:} {12345}
#trigger {Looks like (%x) wants to kill you} {halt;#disconnect}
```

**Multi-Command Execution:**
```
du book for 50;out;fadai
```
(Semicolon-separated sequential execution)

**Advanced Features:**
- Pattern matching with wildcards (%w for words, %d for numbers)
- Conditional logic (#if, #switch)
- Looping (#loop, #while)
- Database storage for persistent state

---

## 2. Cross-Game Common Patterns

### 2.1 Architectural Patterns

#### **Pattern 1: Hierarchical State Machines (HFSM)**

**Found In:** Almost all bots

**Structure:**
```
Bot State
├── Combat Layer
│   ├── Attack State
│   ├── Defend State
│   └── Flee State
├── Navigation Layer
│   ├── Patrol State
│   ├── Chase State
│   └── Return State
└── Resource Layer
    ├── Gather State
    ├── Craft State
    └── Sell State
```

**Benefits:**
- Manages complexity through layering
- Each layer handles specific concerns
- Easy to add new states without affecting others
- Natural representation of game behaviors

**Implementation:**
```java
enum HighLevelState { COMBAT, NAVIGATION, RESOURCE }
enum CombatState { ATTACK, DEFEND, FLEE }
enum NavigationState { PATROL, CHASE, RETURN }

class BotState {
    HighLevelState highLevel;
    CombatState combatState;

    void update() {
        switch(highLevel) {
            case COMBAT: updateCombat(); break;
            case NAVIGATION: updateNavigation(); break;
        }
    }
}
```

---

#### **Pattern 2: Event-Driven Reactive System**

**Found In:** MUD clients, OSRS bots, GW2 tools

**Core Principle:** Don't poll continuously - react to game events

**Architecture:**
```
Game Output → Parser → Event Bus → Handlers → Actions
```

**Example:**
```
Game: "You see a genie appear!"
Parser: Detects random event
Event Bus: Publishes RandomEventStarted
Handlers:
  - Mining: Pause mining
  - Combat: Engage genie
  - Movement: Stop movement
```

**Benefits:**
- Efficient (no wasteful polling)
- Responsive (immediate reaction)
- Modular (easy to add new handlers)
- Natural fit for game event systems

**Implementation Pattern:**
```java
interface GameEventHandler {
    boolean canHandle(String gameOutput);
    void handle(String gameOutput, BotContext context);
}

class RandomEventHandler implements GameEventHandler {
    public boolean canHandle(String output) {
        return output.contains("genie appear");
    }

    public void handle(String output, BotContext context) {
        context.setState(State.HANDLING_RANDOM);
        // Handle event...
    }
}
```

---

#### **Pattern 3: Rule-Based Decision Making**

**Found In:** All bots with item management, combat targeting, skill selection

**Implementation Approaches:**

**A. Pickit Rules (Diablo-style):**
```
IF [ItemType] == "Sword" AND [Level] >= 70 AND [Damage] > 100 THEN KEEP
IF [ItemType] == "Potion" AND [Quantity] < 10 THEN PICKUP
```

**B. Priority Queues:**
```java
class TargetSelector {
    List<TargetingRule> rules = [
        new AggroRule(),           // Attack what attacks me
        new LowHealthRule(),       // Finish wounded targets
        new HighValueRule(),       // Target valuable enemies
        new ClosestRule()          // Default to nearest
    ];

    Entity selectTarget(List<Entity> enemies) {
        for (rule : rules) {
            Entity target = rule.apply(enemies);
            if (target != null) return target;
        }
        return null;
    }
}
```

**C. Utility Scoring:**
```java
class ActionScorer {
    Action selectAction() {
        Map<Action, Double> scores = new HashMap<>();

        for (action : availableActions) {
            scores.put(action, calculateUtility(action));
        }

        return maxByScore(scores);
    }

    double calculateUtility(Action action) {
        double score = 0;
        score += action.getBenefit() * BENEFIT_WEIGHT;
        score -= action.getCost() * COST_WEIGHT;
        score += action.getRisk() * RISK_WEIGHT;
        return score;
    }
}
```

---

#### **Pattern 4: Task Queue with Priorities**

**Found In:** EVE Online bots, raid helpers

**Structure:**
```java
class TaskQueue {
    PriorityQueue<Task> queue;

    void addTask(Task task, Priority priority) {
        task.priority = priority;
        queue.add(task);
    }

    void execute() {
        while (!queue.isEmpty()) {
            Task task = queue.poll();
            if (task.isStillValid()) {
                task.execute();
            }
        }
    }
}

// Interruptible tasks
interface InterruptibleTask {
    boolean shouldInterrupt(Task newTask);
    void onInterrupt();
}
```

**Use Cases:**
- Mining: Queue asteroid targets, interrupt if attacked
- Combat: Queue abilities, interrupt for emergency heals
- Crafting: Queue items, interrupt for combat

---

#### **Pattern 5: Finite State Machine + Behavior Tree Hybrid**

**Found In:** Modern game AI (Uncharted, God of War, RDR2)

**Architecture:**
```
High-Level FSM (What we're doing)
├── Combat State → BT_CombatBehavior
├── Exploration State → BT_ExplorationBehavior
└── Social State → BT_SocialBehavior

Behavior Trees (How we do it)
├── BT_CombatBehavior
│   ├── Selector (Choose approach)
│   │   ├── Sequence (Melee combat)
│   │   │   ├── IsInRange
│   │   │   ├── CanAttack
│   │   │   └── AttackAction
│   │   └── Sequence (Ranged combat)
│   │       ├── HasAmmo
│   │       ├── IsClearLineOfSight
│   │       └── RangedAttackAction
```

**Benefits:**
- FSM handles high-level mode switches
- BT handles complex behavior logic
- Best of both worlds

---

### 2.2 State Management Patterns

#### **Pattern 6: Persistent Context State**

**Found In:** All successful long-running bots

**Components:**
```java
class BotContext {
    // Game State
    GameState gameState;

    // Agent State
    AgentStats stats;
    Position position;
    Inventory inventory;

    // Task State
    Task currentTask;
    List<Task> taskQueue;

    // Learning State
    Map<String, Double> successRates;
    List<String> successfulPatterns;

    // Social State
    Set<String> friends;
    Set<String> enemies;
    ConversationHistory conversations;
}
```

**Persistence:**
- Save state on shutdown
- Load state on startup
- Periodic checkpoints
- Rollback on critical failure

---

#### **Pattern 7: Blackboard Pattern**

**Found In:** Behavior tree systems, multi-agent coordination

**Architecture:**
```
┌─────────────────────────────────────┐
│         Blackboard                  │
│  ┌─────────────────────────────┐   │
│  │ Shared Knowledge            │   │
│  │ - PlayerPosition            │   │
│  │ - LastSeenTime              │   │
│  │ - ResourceLocations         │   │
│  │ - ThreatLevel               │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
          ▲         ▲         ▲
          │         │         │
    Behavior  Behavior  Behavior
       Node      Node      Node
```

**Implementation:**
```java
class Blackboard {
    Map<String, Object> data = new ConcurrentHashMap<>();

    void put(String key, Object value) {
        data.put(key, value);
    }

    <T> T get(String key, Class<T> type) {
        return type.cast(data.get(key));
    }

    boolean has(String key) {
        return data.containsKey(key);
    }
}

// Usage in behavior nodes
class CanAttackNode extends BehaviorNode {
    public Status execute(Blackboard bb) {
        Position playerPos = bb.get("PlayerPosition", Position.class);
        Position myPos = bb.get("MyPosition", Position.class);

        if (playerPos.distanceTo(myPos) < ATTACK_RANGE) {
            return Status.SUCCESS;
        }
        return Status.FAILURE;
    }
}
```

---

### 2.3 Navigation Patterns

#### **Pattern 8: Waypoint Network**

**Found In:** PODBot (CS 1.6), classic MMO pathfinding

**Structure:**
```
Waypoint Graph:
Node(id=1, position=(10,20,30), type=PATROL)
Node(id=2, position=(15,25,35), type=CAMP)
Node(id=3, position=(20,30,40), type=OBJECTIVE)

Edges:
1 → 2 (weight: 5.0)
2 → 3 (weight: 7.5)
1 → 3 (weight: 12.0, jump_required=true)
```

**Pathfinding:**
- Dijkstra for shortest path
- A* with heuristics for speed
- Precompute common routes

---

#### **Pattern 9: NavMesh Navigation**

**Found In:** Modern games (Guild Wars 2, Unity games)

**Advantages:**
- Efficient representation
- Natural movement
- Dynamic obstacle support

**Implementation:**
- Polygon mesh of walkable surfaces
- A* over polygon centers
- String pulling for smooth paths

---

#### **Pattern 10: Hierarchical Pathfinding**

**Found In:** Open world games (Minecraft, EVE)

**Concept:**
```
Global Layer (Low Resolution):
┌───┬───┬───┬───┐
│ A │ B │ C │ D │
├───┼───┼───┼───┤
│ E │ F │ G │ H │
└───┴───┴───┴───┘

Local Layer (High Resolution):
Each cell contains detailed pathfinding
```

**Benefits:**
- Fast long-distance planning
- Detailed local navigation
- Scales to large worlds

---

### 2.4 Error Recovery Patterns

#### **Pattern 11: Stuck Detection**

**Found In:** OnmyojiAutoScript, game automation frameworks

**Detection Methods:**
1. **Position-Based:** No movement in N seconds
2. **State-Based:** Same state for N ticks
3. **Health-Based:** Health not regenerating
4. **Progress-Based:** Quest progress stalled

**Implementation:**
```java
class StuckDetector {
    private Position lastPosition;
    private int stuckTicks = 0;
    private static final int STUCK_THRESHOLD = 100; // 5 seconds at 20 TPS

    public void update(Position currentPosition) {
        if (currentPosition.equals(lastPosition)) {
            stuckTicks++;
            if (stuckTicks > STUCK_THRESHOLD) {
                throw new StuckException("Agent hasn't moved");
            }
        } else {
            stuckTicks = 0;
            lastPosition = currentPosition;
        }
    }
}
```

---

#### **Pattern 12: Retry with Exponential Backoff**

**Found In:** Network automation, API interactions

**Pattern:**
```
Attempt 1: Immediate retry
Attempt 2: Wait 1 second
Attempt 3: Wait 2 seconds
Attempt 4: Wait 4 seconds
Attempt 5: Wait 8 seconds (max)
```

**Implementation:**
```java
class RetryHandler {
    public <T> T retry(Supplier<T> action, int maxAttempts) {
        int attempt = 0;
        long delay = 1000; // Start with 1 second

        while (attempt < maxAttempts) {
            try {
                return action.get();
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxAttempts) {
                    throw e;
                }
                Thread.sleep(delay);
                delay = Math.min(delay * 2, MAX_DELAY);
            }
        }
        return null;
    }
}
```

---

#### **Pattern 13: Graceful Degradation**

**Found In:** EVE Online bots, raid automation

**Concept:** Reduce functionality instead of failing completely

**Levels:**
```
Full Operation:
- All features active
- Optimized paths
- Advanced strategies

Degraded Level 1:
- Disable optional features
- Use simpler paths
- Basic strategies

Degraded Level 2:
- Minimum viable features
- Direct paths only
- Reactive only (no planning)

Safe Mode:
- Movement only
- Defensive actions
- Await human intervention
```

**Implementation:**
```java
class DegradationHandler {
    private DegradationLevel level = DegradationLevel.FULL;

    public void handleRepeatedFailures() {
        switch (level) {
            case FULL:
                level = DegradationLevel.DEGRADED_1;
                disableOptionalFeatures();
                break;
            case DEGRADED_1:
                level = DegradationLevel.DEGRADED_2;
                enableBasicMode();
                break;
            case DEGRADED_2:
                level = DegradationLevel.SAFE;
                enableSafeMode();
                notifyHuman();
                break;
        }
    }
}
```

---

#### **Pattern 14: State Rollback**

**Found In:** Transaction systems, critical automation

**Concept:** Undo to last known good state on failure

**Implementation:**
```java
class StateManager {
    private Stack<BotState> stateStack = new Stack<>();

    public void saveState() {
        stateStack.push(currentState.copy());
    }

    public void rollback() {
        if (!stateStack.isEmpty()) {
            currentState = stateStack.pop();
        }
    }

    public <T> T executeWithRollback(Supplier<T> action) {
        saveState();
        try {
            T result = action.get();
            return result;
        } catch (Exception e) {
            rollback();
            throw e;
        }
    }
}
```

---

### 2.5 Scripting Patterns

#### **Pattern 15: DSL (Domain-Specific Language)**

**Found In:** MUD clients (TinTin++, ZMud), Razor

**Examples:**

**TinTin++:**
```
#action {You are hungry} {get bread bag;eat bread}
#alias {heal} {cast 'heal' %0}
```

**Razor:**
```
hotkey F1
  useitem 'Pickaxe'
  targettile offset 0 0
  wait 2000
loop
```

**Benefits:**
- Easy for non-programmers
- Declarative (what, not how)
- Game-specific abstractions
- Rapid iteration

---

#### **Pattern 16: Lua Scripting**

**Found In:** Dota 2 bots, Logitech G Hub, game mods

**Why Lua?**
- Fast, lightweight
- Easy to embed
- Safe (sandboxed)
- Popular in games

**Example (Dota 2):**
```lua
function Think()
    local npcBot = GetBot()

    if npcBot:GetHealth() < 0.3 then
        npcBot:Action_MoveToLocation(GetLocation(ancient))
    elseif npcBot:GetMana() > 100 then
        local ability = npcBot:GetAbilityByName("fireball")
        npcBot:Action_UseAbility(ability)
    end
end
```

---

#### **Pattern 17: Configuration-Driven Behavior**

**Found In:** Diablo pickit systems, OSRS bot profiles

**Format:**
```toml
[combat]
aggressive_mode = true
retreat_health_percent = 20
target_priority = ["lowest_hp", "closest"]

[mining]
ore_types = ["iron", "gold", "diamond"]
inventory_keep_slots = 5
bank_when_full = true

[pickit]
# Keep items with these properties
[[pickit.rules]]
item_type = "weapon"
min_level = 70
min_damage = 100
action = "keep"

[[pickit.rules]]
item_type = "potion"
action = "sell"
```

---

## 3. Unique Innovations by Game

### 3.1 Diablo Series: Item Rule Engine

**Innovation:** NIP (Named Item Pattern) files for declarative item filtering

**Example:**
```
# High-level weapons
[Name] == ColossusBlade && [Quality] == Unique && [Flag] == Ethereal # Keep
[Name] == PhaseBlade && [Quality] == Unique # Keep

# Magic Find gear
[Type] == Armor && [Quality] == Magic && [MagicFind] >= 30 # Keep
```

**Why It's Clever:**
- Declarative (what to keep, not how to decide)
- Easy to modify without code changes
- Community sharing of pickit files
- Semantic (reads like English)

---

### 3.2 OSRS: Random Event Solvers

**Innovation:** Dedicated subsystem for unexpected game events

**Architecture:**
```
Main Loop:
  ├─ Task: Mining
  ├─ Task: Banking
  └─ Task: Combat

Random Event System (interrupts everything):
  ├─ Detector: Pattern matches "Strange teleports you"
  ├─ Classifier: Identifies event type (Genie, Mime, etc.)
  ├─ Solver: Specific solution for event
  └─ Resumer: Returns to previous task
```

**Key Insight:** Random events are first-class concerns, not afterthoughts

---

### 3.3 Guild Wars 2: Dynamic Event Chaining

**Innovation:** Events trigger other events (no quest givers)

**Challenge for Bots:**
- Traditional: Talk to NPC → Get Quest → Complete → Return
- GW2: Walk into area → Event starts → Complete → Next event starts

**Bot Adaptation:**
```
EventTracker:
  currentEvent: WatchEvent
  eventChain: [Defend, CounterAttack, Rescue]

Response:
  IF currentEvent.success THEN
    currentEvent = eventChain.next()
    NavigateTo(currentEvent.location)
  ELSE
    Wait for next event in area
```

**Key Insight:** World state drives behavior, not quest logs

---

### 3.4 EVE Online: Multi-Account Orchestration

**Innovation:** "Companion mode" for bot fleets

**Pattern:**
```
Leader Bot:
  - Makes decisions
  - Targets enemies
  - Navigates

Follower Bots:
  - Assist leader
  - Target leader's target
  - Follow leader's movement
  - Maintain formation

Coordination:
  - Shared state via IPC/network
  - Role assignment (healer, DPS, hauler)
  - Distributed resource processing
```

**Key Insight:** Specialized roles > generalized bots

---

### 3.5 Ultima Online: Macro Recording

**Innovation:** Record → Edit → Replay workflow

**Benefits:**
- Non-programmers can create automation
- Learning by example
- Easy debugging (watch replay)
- Community sharing

**Evolution:**
```
1. Simple Recording (Early Razor)
   - Record keystrokes
   - Replay exactly

2. Script Recording (Razor Enhanced)
   - Record → Generate script
   - Edit script (add loops, conditions)
   - Replay modified script

3. Full Programming (UOSteam, Python)
   - Program from scratch
   - Recording as starting point
```

---

### 3.6 MUD Clients: Trigger/Alias Foundation

**Innovation:** Text-based pattern matching (prefigured all modern automation)

**Why It Matters:**
- Text is structured and parseable
- Regular expressions are powerful
- Stateless interactions (easy to automate)
- Foundation for all modern patterns

**Legacy in Modern Systems:**
```
MUD Trigger → Event Listener
MUD Alias → Command Registration
MUD Variable → Context State
MUD Loop → Behavior Tree
```

---

## 4. Architectural Recommendations

### 4.1 Core Architecture for Steve AI

Based on research across 30 years of game automation, recommended architecture:

```
┌─────────────────────────────────────────────────────────────────┐
│                    BRAIN LAYER (Strategic)                      │
│                                                                 │
│   Components:                                                   │
│   • LLM Task Planner (what to do)                              │
│   • High-Level FSM (mode: combat, build, gather)               │
│   • Goal Prioritization (utility scoring)                       │
│   • Multi-Agent Coordination (foreman/worker pattern)          │
│                                                                 │
│   Update Rate: Event-driven (every 30-60 seconds)              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Generates Tasks & Policies
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SCRIPT LAYER (Tactical)                       │
│                                                                 │
│   Components:                                                   │
│   • Behavior Trees (how to do tasks)                           │
│   • HTN Planner (decompose complex tasks)                      │
│   • Rule-Based Decision Making (targeting, items)              │
│   • Waypoint Navigation (A* with smoothing)                    │
│                                                                 │
│   Update Rate: Every tick (20 TPS)                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Issues Actions
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   PHYSICAL LAYER (Action)                       │
│                                                                 │
│   Components:                                                   │
│   • Action Executor (tick-based execution)                     │
│   • Interceptor Chain (logging, metrics, events)               │
│   • Error Recovery (retry, rollback, degradation)              │
│   • Minecraft API (blocks, entities, inventory)                │
│                                                                 │
│   Update Rate: Every tick (20 TPS)                             │
└─────────────────────────────────────────────────────────────────┘
```

---

### 4.2 Specific Recommendations

#### **Recommendation 1: Implement Pickit-Style Item Rules**

**Why:** Diablo's NIP system is the gold standard for declarative item management

**Implementation:**
```java
public interface ItemRule {
    boolean matches(Item item);
    Action getAction(Item item); // KEEP, SELL, DROP, STASH
}

public class ItemRuleEngine {
    private List<ItemRule> rules;

    public Action decide(Item item) {
        for (ItemRule rule : rules) {
            if (rule.matches(item)) {
                return rule.getAction(item);
            }
        }
        return Action.DROP;
    }
}

// Configuration
rules:
  - type: DIAMOND
    action: KEEP
  - type: DIRT
    action: DROP
  - type: LOG
    condition: count < 64
    action: KEEP
```

---

#### **Recommendation 2: Add Random Event Handlers**

**Why:** OSRS bots treat random events as first-class concerns

**Implementation:**
```java
public class RandomEventSystem {
    private List<RandomEventHandler> handlers;
    private Task originalTask;

    public void onGameOutput(String output) {
        for (RandomEventHandler handler : handlers) {
            if (handler.detects(output)) {
                interruptCurrentTask();
                handler.handle(output);
                resumeOriginalTask();
                return;
            }
        }
    }
}

// Example handler
public class ZombieSiegeHandler implements RandomEventHandler {
    public boolean detects(String output) {
        return output.contains("Zombie siege started");
    }

    public void handle(String output) {
        // Run to safety
        // Equip weapons
        // Wait for siege to end
    }
}
```

---

#### **Recommendation 3: Implement Exponential Backoff Retry**

**Why:** Network operations and LLM calls benefit from smart retry

**Implementation:**
```java
public class RetryPolicy {
    private int maxAttempts = 3;
    private long baseDelay = 1000; // 1 second
    private long maxDelay = 30000; // 30 seconds

    public <T> T executeWithRetry(Callable<T> operation) {
        int attempt = 0;
        long delay = baseDelay;

        while (attempt < maxAttempts) {
            try {
                return operation.call();
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxAttempts) {
                    throw new RuntimeException("Max retries exceeded", e);
                }
                Thread.sleep(delay);
                delay = Math.min(delay * 2, maxDelay);
            }
        }
        return null;
    }
}
```

---

#### **Recommendation 4: Add Stuck Detection**

**Why:** OnmyojiAutoScript and game automation frameworks rely on this

**Implementation:**
```java
public class StuckDetector {
    private Position lastPosition;
    private int stuckTicks = 0;
    private static final int STUCK_THRESHOLD = 100; // 5 seconds

    public void update(Position currentPosition) {
        if (currentPosition.equals(lastPosition)) {
            stuckTicks++;
            if (stuckTicks > STUCK_THRESHOLD) {
                triggerRecovery();
            }
        } else {
            stuckTicks = 0;
            lastPosition = currentPosition;
        }
    }

    private void triggerRecovery() {
        // Try random movement
        // Repath to destination
        // Teleport if desperate
        // Request human help if all fails
    }
}
```

---

#### **Recommendation 5: Create DSL for Common Tasks**

**Why:** MUD clients and Razor show DSLs enable rapid iteration

**Example DSL for Minecraft:**
```
# Mining macro
mine "iron_ore" {
    radius = 32
    keep_inventory_space = 5
    drop_garbage = true
    tools = ["iron_pickaxe", "diamond_pickaxe"]

    on_inventory_full {
        path_to_chest = "storage"
        deposit_items = "iron_ore"
    }
}

# Building macro
build "house" {
    template = "simple_house.schematic"
    offset = relative_to_player
    verify_resources = true
    missing_resources_action = "queue_gathering"
}
```

---

#### **Recommendation 6: Implement Multi-Agent Role Specialization**

**Why:** EVE Online companion mode shows specialized roles work better

**Roles:**
```
Foreman:
  - Plans and delegates
  - Coordinates workers
  - Monitors progress
  - Reports to player

Miner:
  - Gathers resources
  - Reports finds
  - Follows foreman orders

Builder:
  - Places blocks
  - Follows blueprints
  - Reports completion

Guard:
  - Protects area
  - Responds to threats
  - Alerts others
```

---

#### **Recommendation 7: Add Blackboard for Shared State**

**Why:** Behavior trees and multi-agent systems use this pattern

**Implementation:**
```java
public class AgentBlackboard {
    private Map<String, Object> data = new ConcurrentHashMap<>();

    // Zone-specific state
    public void recordResourceLocation(String type, Position pos) {
        data.put("resource_" + type + "_last_seen", pos);
    }

    public Position getLastResourceLocation(String type) {
        return (Position) data.get("resource_" + type + "_last_seen");
    }

    // Threat assessment
    public void setThreatLevel(ThreatLevel level) {
        data.put("current_threat", level);
    }

    public ThreatLevel getThreatLevel() {
        return (ThreatLevel) data.getOrDefault("current_threat", ThreatLevel.NONE);
    }
}
```

---

### 4.3 Anti-Detection Strategies (If Applicable)

**Note:** This section is for academic research into how bots avoid detection.

#### **Pattern: Human-Like Behavior**

**Techniques:**
1. **Randomized Delays:** Never fixed intervals
   ```java
   // Bad
   Thread.sleep(1000);

   // Good
   Thread.sleep(800 + random.nextFloat() * 400);
   ```

2. **Mouse Movement Curves:** Not straight lines
   ```java
   // Generate Bezier curve from A to B
   List<Position> path = generateMouseCurve(start, end);
   for (Position pos : path) {
       mouseMove(pos);
       sleep(10);
   }
   ```

3. **Mistake Simulation:** Occasional errors
   ```java
   if (random.nextFloat() < 0.05) { // 5% error rate
       // Mis-click slightly
       // Then correct
   }
   ```

4. **Break Scheduling:** Human-like play sessions
   ```java
   sessionDuration = 60 + random.nextFloat() * 30; // 60-90 minutes
   breakDuration = 5 + random.nextFloat() * 10; // 5-15 minutes
   ```

5. **Varied Action Order:** Don't always do things same way
   ```java
   // Shuffle tasks instead of fixed order
   Collections.shuffle(taskQueue);
   ```

---

#### **Pattern: Behavioral Signature Avoidance**

**Detection Methods Games Use:**
- Click patterns (repetitive sequences)
- Timing patterns (fixed intervals)
- Skill usage (always same rotation)
- Movement (always optimal path)

**Countermeasures:**
- Add noise to all behaviors
- Vary decision-making
- Suboptimal choices occasionally
- React to "distractions"

---

## 5. Anti-Detection Insights

### 5.1 How Games Detect Bots

#### **Detection Method 1: Behavioral Analysis**

**What Games Track:**
- Click timestamps (too regular = bot)
- Click positions (exact center = bot)
- Action sequences (same order = bot)
- Reaction times (too fast = bot)
- Session duration (too long = bot)

**Academic Research:**
- User Behavior Analysis achieves 95.92% accuracy
- NGUARD framework combines supervised + unsupervised learning
- Focuses on social activities (bots don't socialize)

---

#### **Detection Method 2: Client-Side Detection**

**Techniques:**
- Hook detection (Is DLL injected?)
- Process scanning (Is known bot running?)
- Memory reading (Is external process reading game memory?)
- API monitoring (Are calls too fast/regular?)

---

#### **Detection Method 3: Server-Side Analysis**

**Techniques:**
- Action timing analysis
- Path efficiency (too optimal = bot)
- Resource gathering (too perfect = bot)
- Economic patterns (always selling at max = bot)
- Social patterns (never talks = bot)

---

### 5.2 Anti-Detection Best Practices

**From OSRS Bot Color Project:**
- Random click distribution algorithms
- Color-based detection (less detectable than memory reading)
- Community-developed anti-detection patterns

**From EVE Online Bots:**
- Local chat monitoring (respond to players)
- Break scheduling (don't run 24/7)
- Randomized activity patterns

**From Game Automation Research:**
- Variable delays (never fixed intervals)
- Non-sequential skill usage
- Avoid perfect optimization
- Add "human errors"

---

## 6. Architectural Comparisons

### 6.1 State Machine vs Behavior Tree vs HTN

| Aspect | FSM | Behavior Tree | HTN |
|--------|-----|---------------|-----|
| **Best For** | Simple state-based behavior | Complex reactive behavior | Goal-oriented planning |
| **Complexity** | Low | Medium | High |
| **Scalability** | Poor (state explosion) | Good (modular) | Excellent (hierarchical) |
| **Reactivity** | High | Very High | Medium |
| **Planning** | None | Low (utility scoring) | High (task decomposition) |
| **Learning** | Difficult | Possible | Possible |
| **Examples** | PODBot, Game AI | Uncharted, God of War | React, GOAP systems |

**Recommendation for Steve AI:** Hybrid approach
- FSM for high-level states (COMBAT, BUILDING, IDLE)
- Behavior Trees for reactive behaviors (dodge, attack, flee)
- HTN for complex task planning (build house → gather materials → construct)

---

### 6.2 Polling vs Event-Driven

| Aspect | Polling | Event-Driven |
|--------|---------|--------------|
| **CPU Usage** | High (constant checks) | Low (only on events) |
| **Responsiveness** | Delay (poll interval) | Immediate |
| **Complexity** | Simple | Complex (need event bus) |
| **Predictability** | High (check every N ticks) | Low (depends on events) |
| **Best For** | Continuous monitoring | Discrete game events |

**Recommendation:** Hybrid
- Poll for continuous state (health, position)
- Event-driven for discrete events (chat, commands, combat)

---

### 6.3 Centralized vs Distributed Control

| Aspect | Centralized | Distributed |
|--------|-------------|-------------|
| **Coordination** | Easy | Hard |
| **Scalability** | Poor (bottleneck) | Excellent |
| **Fault Tolerance** | Low (single point) | High (redundant) |
| **Complexity** | Low | High |
| **Best For** | Small agent counts | Large multi-agent systems |

**Recommendation for Steve AI:** Hierarchical
- Centralized planning (foreman)
- Distributed execution (workers)
- Emergent behavior through simple rules

---

## 7. Key Takeaways

### 7.1 Universal Patterns

**Present in almost all successful bots:**

1. **Hierarchical State Management:** Break complexity into layers
2. **Event-Driven Reactivity:** Respond to game events, don't just poll
3. **Rule-Based Decisions:** Declarative rules for common decisions
4. **Error Recovery:** Retry, rollback, degrade gracefully
5. **Stuck Detection:** Detect and recover from failure states
6. **Configuration-Driven:** Externalize behavior to config files

---

### 7.2 Historical Evolution

**1990s (MUD Automation):**
- Text-based triggers and aliases
- Simple script languages
- Foundation for modern patterns

**2000s (MMO Bots - WoW, UO, Diablo):**
- GUI automation (color detection, mouse clicks)
- Memory reading (direct state access)
- Complex state machines
- Pickit systems for items

**2010s (Modern MMO Bots - OSRS, GW2, EVE):**
- Behavior trees
- Advanced pathfinding (NavMesh, A*)
- Multi-agent coordination
- Anti-detection focus

**2020s (LLM-Enhanced Agents):**
- Natural language understanding
- Task planning and reasoning
- Learning from experience
- Multi-modal communication

**Trend:** Increasing abstraction and capability, but core patterns remain consistent

---

### 7.3 Most Transferable Patterns

**For Steve AI Development:**

1. **Pickit-Style Item Rules** (Diablo): Declarative item management
2. **Random Event Handlers** (OSRS): First-class interrupt system
3. **Stuck Detection** (Game Automation): Essential reliability feature
4. **Behavior Trees** (Modern Games): Modular reactive behaviors
5. **Multi-Agent Roles** (EVE): Specialization > generalization
6. **Event Bus Pattern** (All successful bots): Decoupled communication
7. **Retry with Backoff** (Network/Automation): Resilient operations
8. **Blackboard Pattern** (Multi-agent): Shared state management

---

## 8. Implementation Roadmap

### Phase 1: Core Patterns (Immediate)
- [ ] Implement item rule engine (pickit-style)
- [ ] Add stuck detection with recovery
- [ ] Implement retry with exponential backoff
- [ ] Create event bus for agent communication

### Phase 2: Enhanced AI (Short-term)
- [ ] Behavior tree runtime for reactive behaviors
- [ ] HTN planner for complex task decomposition
- [ ] Blackboard for shared agent state
- [ ] Multi-agent role specialization

### Phase 3: Advanced Features (Long-term)
- [ ] DSL for common tasks (mining, building)
- [ ] Skill library (Voyager-style learning)
- [ ] Dynamic event system (like GW2)
- [ ] Multi-agent orchestration (like EVE)

---

## 9. References

### Academic Papers
- [User Behavior Analysis for Online Game Bot Detection](https://xueshu.baidu.com/usercenter/paper/show?paperid=dfe5b9259a0eeaa3d4b0d148c14deaaa) - 95.92% detection accuracy
- [NGUARD: A Game Bot Detection Framework](https://m.zhangqiaokeyan.com/academic-journal-foreign_detail_thesis/0704024251738.html) - Supervised + unsupervised methods
- [A Survey of Behavior Trees in Robotics and AI](https://xueshu.baidu.com/usercenter/paper/show?paperid=1x4e0jc0um3n0200jy1v0p609d493068) - BT theory

### Bot Projects & Documentation
- [Koolo - Diablo II Resurrected Bot](https://github.com/hectorgimenez/koolo) - NIP pickit system
- [DreamBot](http://www.dreambot.org/) - OSRS bot with simple API
- [OSRS-Bot-COLOR](https://github.com/ThatOneGuyScripts/OSRS-Bot-COLOR) - Color-based detection
- [TinyMiner](https://www.tinyminer.com/) - EVE Online mining bot
- [Razor](https://www.razorce.com) - Ultima Online macro tool
- [TinTin++](https://sourceforge.net/projects/tintin/) - MUD client automation

### Game AI Resources
- [Dota 2 Bot Scripting](https://developer.valvesoftware.com/wiki/Dota_Bot_Scripting) - Lua API
- [GAMES104: Game Engine AI](https://m.blog.csdn.net/yx314636922/article/details/142365525) - Pathfinding overview
- [PODBOT Analysis](http://www.ruibaoantu.com/article/detail.html?id=5854476) - Waypoint navigation
- [BehaviorTree.CPP](https://m.blog.csdn.net/stallion5632/article/details/139879512) - C++ BT implementation

### Automation & Error Recovery
- [Agent Exception Handling & Recovery](https://jimmysong.io/zh/book/agentic-design-patterns/12-exception-handling-and-recovery/) - Modern patterns
- [AutoGPT Error Recovery](https://blog.csdn.net/weixin_26850469/article/details/155929091) - AI agent failures
- [OnmyojiAutoScript Stuck Detection](https://m.blog.csdn.net/gitblog_07264/article/details/148296920) - Game-specific solutions
- [Retry Pattern in Automation Testing](https://m.blog.csdn.net/okcross0/article/details/142953243) - Retry strategies

### MUD Automation History
- [TinTin++ Deep Dive](https://www.showapi.com/news/article/66c3e7134ddd79f11a01024) - Scripting capabilities
- [ZMud Trigger Tutorial](https://tieba.baidu.com/p/8982020570) - Conditional reflex automation
- [ZMud Command Reference](https://wk.baidu.com/view/96ffa6ddd15abe2342f8d81) - Command documentation

---

## Appendix: Code Examples

### A. Complete Bot Loop (OSRS-Style)

```java
public class MiningBot {
    private State currentState = State.IDLE;
    private Position rockPosition;
    private int stuckTicks = 0;

    public int onLoop() {
        // Check for random events first
        if (detectRandomEvent()) {
            return handleRandomEvent();
        }

        // Check if stuck
        if (isStuck()) {
            return handleStuck();
        }

        // Main state machine
        switch (currentState) {
            case IDLE:
                return startMining();

            case MINING:
                if (inventoryFull()) {
                    currentState = State.BANKING;
                    return walkToBank();
                } else if (!atRock()) {
                    return walkToRock();
                } else {
                    return mine();
                }

            case BANKING:
                if (atBank()) {
                    deposit();
                    currentState = State.IDLE;
                    return 1000; // Wait 1 second
                } else {
                    return walkToBank();
                }
        }

        return 1000; // Default delay
    }

    private boolean isStuck() {
        Position current = getPosition();
        if (current.equals(lastPosition)) {
            stuckTicks++;
            return stuckTicks > 100; // 5 seconds
        }
        stuckTicks = 0;
        lastPosition = current;
        return false;
    }
}
```

### B. Pickit Rule Engine (Diablo-Style)

```java
public class PickitEngine {
    private List<PickitRule> rules = loadRules("config/pickit.nip");

    public ItemAction evaluate(Item item) {
        for (PickitRule rule : rules) {
            if (rule.matches(item)) {
                return rule.getAction();
            }
        }
        return ItemAction.DROP;
    }
}

// Rule format: [Name] == ColossusBlade && [Quality] == Unique # [KEEP]
public class PickitRule {
    private String namePattern;
    private Quality quality;
    private ItemAction action;

    public boolean matches(Item item) {
        if (!item.getName().matches(namePattern)) return false;
        if (item.getQuality() != quality) return false;
        return true;
    }
}
```

### C. Behavior Tree Example (Modern Game AI)

```java
// Combat behavior tree
BehaviorTree combatTree = new BehaviorTree(
    new SelectorNode(
        // Flee if low health
        new SequenceNode(
            new HealthCheckNode(0.3f),
            new FleeAction()
        ),
        // Attack if enemy in range
        new SequenceNode(
            new EnemyInRangeNode(ATTACK_RANGE),
            new HasAmmoNode(),
            new AttackAction()
        ),
        // Chase enemy
        new SequenceNode(
            new EnemyVisibleNode(),
            new ChaseAction()
        ),
        // Patrol
        new PatrolAction()
    )
);

// Usage
combatTree.tick(blackboard);
```

### D. Event-Driven Architecture (MUD-Style)

```java
public class EventBus {
    private Map<Class<?>, List<EventHandler>> handlers = new HashMap<>();

    public <T> void subscribe(Class<T> eventType, EventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    public void publish(Object event) {
        List<EventHandler> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (EventHandler handler : eventHandlers) {
                handler.handle(event);
            }
        }
    }
}

// Usage
eventBus.subscribe(ChatMessageEvent.class, event -> {
    if (event.getMessage().contains("genie")) {
        botState.setHandlingRandomEvent(true);
        randomEventSolver.solve();
    }
});
```

---

**Document Version:** 1.0
**Last Updated:** 2026-03-01
**Research Completed By:** Claude (Research Agent)
**Status:** Complete - Ready for integration into dissertation and implementation planning
