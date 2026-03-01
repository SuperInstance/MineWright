# WoW Glider (WoWGlider) - Technical Architecture Analysis
## A Comprehensive Study of Game Automation Design Patterns (2005-2009 Era)

**Document Version:** 1.0
**Date:** 2026-03-01
**Purpose:** Academic research for legitimate game AI development
**Focus:** Architecture patterns, design decisions, and technical approaches

---

## Executive Summary

WoW Glider (also known as WoWGlider or MMOGlider) was a commercial game automation bot developed by MDY Industries (Michael Donnelly) for World of Warcraft. Operating from approximately 2005-2009, it sold approximately 120,000 licenses and generated around $3.5 million in revenue before being shut down by legal action from Blizzard Entertainment.

**Note:** This analysis focuses on the technical architecture and design patterns for academic purposes. The software was ultimately ruled to violate copyright law and Blizzard's Terms of Service. This research is intended to inform legitimate game AI development and understand historical approaches to game automation.

---

## Table of Contents

1. [Historical Context](#1-historical-context)
2. [System Architecture](#2-system-architecture)
3. [Memory Reading & Game State Extraction](#3-memory-reading--game-state-extraction)
4. [Automation System Design](#4-automation-system-design)
5. [Humanization Techniques](#5-humanization-techniques)
6. [State Management & Decision Making](#6-state-management--decision-making)
7. [Movement & Pathfinding](#7-movement--pathfinding)
8. [Combat System](#8-combat-system)
9. [Failure Recovery](#9-failure-recovery)
10. [Anti-Detection Strategies](#10-anti-detection-strategies)
11. [Design Patterns Identified](#11-design-patterns-identified)
12. [Lessons for Legitimate Game AI](#12-lessons-for-legitimate-game-ai)
13. [References](#references)

---

## 1. Historical Context

### Timeline

| Period | Event |
|--------|-------|
| **2005** | WoW Glider development begins |
| **2005-2006** | Initial release and growth |
| **2007** | Peak popularity; Warden anti-cheat active |
| **2008** | MDY Industries v. Blizzard lawsuit filed |
| **2009** | Initial ruling against MDY; $6.5M damages |
| **2010** | Ninth Circuit appeal; permanent injunction issued |

### Technical Context

**Era Characteristics (2005-2009):**
- **32-bit Windows dominance** (XP, Vista)
- **Limited anti-cheat sophistication** compared to modern systems
- **Memory reading primary technique** (packet encryption too complex)
- **Static address discovery** via pattern scanning
- **Input simulation** via Windows API (SendInput, keybd_event, mouse_event)

**Competitive Landscape:**
- Other WoW bots used similar memory-reading approaches
- Packet manipulation bots existed but were more easily detected
- Macro/scripting bots (AutoIt, AutoHotkey) less sophisticated
- Glider distinguished itself through humanization and stability

---

## 2. System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        USER INTERFACE LAYER                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │ Configuration│  │   Status     │  │   Logs       │             │
│  │    Panel     │  │  Monitor     │  │  Viewer      │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ Commands/Configuration
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      ORCHESTRATION LAYER                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │   Task       │  │   State      │  │   Error      │             │
│  │  Scheduler   │  │  Machine     │  │  Handler     │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ Coordinates
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      GAME STATE LAYER                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │    Memory    │  │    Pattern   │  │    Object    │             │
│  │   Reader     │  │    Scanner   │  │    Manager   │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ Read Operations
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      WORLD OF WARCRAFT PROCESS                      │
│  • Player State (HP, MP, Position, Level)                          │
│  • Target Information (Health, Distance, Type)                     │
│  • Environment (Objects, NPCs, Mobs)                               │
│  • Inventory (Items, Equipment, Bag Slots)                         │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ Output
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      ACTION EXECUTION LAYER                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │    Input     │  │   Combat     │  │   Movement   │             │
│  │  Simulator   │  │   Module     │  │   Controller │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
└─────────────────────────────────────────────────────────────────────┘
```

### Component Breakdown

| Component | Responsibility | Technology |
|-----------|----------------|------------|
| **Memory Reader** | Read game process memory | ReadProcessMemory, pattern scanning |
| **Pattern Scanner** | Locate dynamic memory addresses | Byte pattern matching, pointer chains |
| **Object Manager** | Track game objects (units, items) | Linked list traversal, object parsing |
| **State Machine** | Manage bot behavior states | Finite State Machine (FSM) |
| **Combat Module** | Execute combat rotations | Priority queues, cooldown tracking |
| **Navigation System** | Handle movement and pathfinding | Waypoint graphs, A* algorithm |
| **Input Simulator** | Generate keyboard/mouse input | SendInput, keybd_event, mouse_event |
| **Humanization Engine** | Add realistic variation | Random delays, Bezier curves |
| **Error Recovery** | Handle stuck situations | Timeout detection, state reset |

---

## 3. Memory Reading & Game State Extraction

### Memory Reading Architecture

WoW Glider operated primarily as an **external memory-reading bot**, meaning it did not inject code into the WoW process but instead read memory from outside.

```
┌──────────────────────────────────────────────────────────────────┐
│                    WoW Glider Application                         │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Memory Reading Subsystem                    │   │
│  │                                                           │   │
│  │  1. Attach to WoW.exe process                            │   │
│  │  2. Locate base address via pattern scanning             │   │
│  │  3. Navigate pointer chains to dynamic addresses          │   │
│  │  4. Read structured data from memory                      │   │
│  │  5. Parse into usable game objects                       │   │
│  └─────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
                              │
                              │ ReadProcessMemory() API calls
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                    World of Warcraft Process                     │
│                     (WoW.exe)                                    │
│                                                                  │
│  .text segment (code)        ──► Pattern scanning target         │
│  .data segment (static data) ──► Static offsets                  │
│  .heap segment (dynamic)     ──► Player objects, game state      │
│  .stack segment              ──► Thread-local data               │
└──────────────────────────────────────────────────────────────────┘
```

### Pattern Scanning Technique

**The Challenge:** Memory addresses changed on each game restart due to ASLR (Address Space Layout Randomization) and dynamic allocation.

**The Solution:** Pattern scanning searches for unique byte sequences (signatures) in the code segment to locate functions, then calculates offsets to data.

```cpp
// Pseudocode illustrating pattern scanning approach
BYTE* patternScan(BYTE* baseAddress, SIZE_T size, BYTE* pattern, SIZE_T patternSize, BYTE mask) {
    for (SIZE_T i = 0; i < size - patternSize; i++) {
        bool match = true;
        for (SIZE_T j = 0; j < patternSize; j++) {
            if ((mask & (1 << j)) && (baseAddress[i + j] != pattern[j])) {
                match = false;
                break;
            }
        }
        if (match) {
            return &baseAddress[i];  // Found pattern
        }
    }
    return nullptr;
}

// Example: Find player pointer
// Pattern: 8B 0D ? ? ? ? 89 04 24 E8
// Scan .text segment for this function prologue
// Extract offset from ? ? ? ? bytes
```

**Key Memory Locations (Historical):**

| Data Type | Access Method | Example Offset Pattern |
|-----------|--------------|----------------------|
| **Player Base** | Static address → pointer chain | [[[[WoW.exe+0x123456] + 0x78] + 0xABC] + 0xDEF] |
| **Player Health** | Player base + offset | PlayerBase + 0x20 |
| **Player Position** | Player base + offset | PlayerBase + 0x58 (X), +0x5C (Y), +0x60 (Z) |
| **Target Pointer** | Player base + offset | PlayerBase + 0x104 |
| **Object Manager** | Static address | WoW.exe + 0x456789 |

### Pointer Chain Navigation

```cpp
// Pseudocode for pointer chain dereferencing
uintptr_t readPointerChain(uintptr_t baseAddress, std::vector<intptr_t> offsets) {
    uintptr_t current = baseAddress;

    for (size_t i = 0; i < offsets.size(); i++) {
        // Read pointer at current + offset
        current = *(uintptr_t*)(current + offsets[i]);

        if (current == 0 || current == (uintptr_t)-1) {
            // Invalid pointer, chain broken
            return 0;
        }
    }

    return current;
}

// Example: Get player health
// Chain: [[WoW.exe + 0x01234567] + 0x34] + 0x56]
// Offsets: [0x01234567, 0x34, 0x56]
```

### Object Manager Traversal

WoW uses a linked list structure to track all game objects (players, NPCs, items).

```
Object Manager (Base Address)
         │
         ├─► FirstObject [GUID, Type, Name, ...]
         │        │
         │        ├─► NextObject ──► [Unit Object]
         │        │                      │
         │        │                      ├─► NextObject ──► [Item Object]
         │        │                      │
         │        │                      └─► NextObject ──► [GameObject]
         │        │
         │        └─► NextObject ──► [Another Unit]
         │
         └─► CurrentObject ──► [Player's Object]
```

**Object Structure (Typical):**

| Offset | Field | Type | Description |
|--------|-------|------|-------------|
| +0x00 | GUID | uint64 | Unique identifier |
| +0x08 | Type | uint32 | Object type (Unit, Item, GameObject) |
| +0x10 | Next | ptr | Pointer to next object in list |
| +0x30 | Name | char[] | Object name |
| +0x58 | Position | float[3] | X, Y, Z coordinates |
| +0x20 | Health | int32 | Current health |
| +0x24 | MaxHealth | int32 | Maximum health |

---

## 4. Automation System Design

### Finite State Machine Architecture

WoW Glider used a **Finite State Machine (FSM)** as its core decision-making architecture.

```
                    ┌─────────────────────────────────────────┐
                    │         MAIN CONTROL LOOP               │
                    │         (ticks every ~100ms)            │
                    └─────────────────────────────────────────┘
                                       │
                                       │ Check conditions
                                       ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│    IDLE     │───▶│   SEARCH    │───▶│   COMBAT    │───▶│    LOOT     │
│   Waiting   │    │   Looking   │    │  Fighting   │    │Collecting   │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
      ▲                  │                  │                  │
      │                  │                  │                  │
      │                  ▼                  ▼                  ▼
      │           ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
      │           │   REST      │    │   FLEE      │    │   RETURN    │
      │           │ Recovering  │    │  Escaping   │    │To Waypoint  │
      │           └─────────────┘    └─────────────┘    └─────────────┘
      │                                                                          │
      └──────────────────────────────────────────────────────────────────────────┘
```

### State Definitions

| State | Trigger Conditions | Actions | Exit Conditions |
|-------|-------------------|---------|-----------------|
| **IDLE** | Bot started, no task | Wait for user input | User starts bot |
| **SEARCH** | No target in combat | Scan for enemies, move to next waypoint | Target found, hostile, in range |
| **COMBAT** | Hostile target acquired | Execute combat rotation | Target dead, player health low |
| **LOOT** | Target died, lootable | Move to corpse, loot | No lootable corpses nearby |
| **REST** | Health/Mana low | Sit, eat/drink, wait | Health/Mana restored |
| **FLEE** | Health critical | Run to safe location | Out of danger, health restored |
| **RETURN** | Task complete, far from home | Navigate to starting point | Near starting location |

### Task Scheduling

The bot supported multiple automation "profiles" or "tasks":

```cpp
enum TaskType {
    TASK_GRIND,      // Kill mobs in area for XP/loot
    TASK_FARM,       // Gather specific resources
    TASK_QUEST,      // Complete quest objectives
    TASK_LEVEL,      // Level character to target
    TASK_PVP,        // Battleground automation
    TASK_FISH        // Fishing minigame
};

struct TaskProfile {
    TaskType type;
    int priority;

    // Movement
    std::vector<Waypoint> waypoints;
    float searchRadius;

    // Combat
    std::string pullSpell;
    CombatRotation rotation;

    // Loot
    bool autoLoot;
    std::vector<ItemFilter> lootRules;

    // Rest
    int healthRestThreshold;
    int manaRestThreshold;
};
```

### Priority-Based Task Queue

```
Task Queue (Priority-based)
├─► [1] EMERGENCY_FLEE (Health < 20%)
├─► [2] REST (Health/Mana low)
├─► [3] LOOT (Corpses nearby)
├─► [4] COMBAT (Target acquired)
├─► [5] SEARCH (Looking for targets)
└─► [6] NAVIGATE (Moving to waypoint)
```

---

## 5. Humanization Techniques

### Random Delay Distribution

**Problem:** Bots with perfect timing are easily detected.

**Solution:** Add randomness to all actions using statistical distributions.

```cpp
// Humanized delay calculation
float calculateDelay(float baseDelay, HumanizationProfile profile) {
    // Add Gaussian noise (bell curve distribution)
    float jitter = randomGaussian(0, profile.stdDeviation);

    // Add fatigue factor (increases over time)
    float fatigue = 1.0 + (sessionTime * profile.fatigueRate);

    // Calculate final delay
    float delay = (baseDelay + jitter) * fatigue;

    // Clamp to reasonable bounds
    return clamp(delay, profile.minDelay, profile.maxDelay);
}

// Example usage
void humanizedClick(int x, int y) {
    float delay = calculateDelay(100.0, profile);  // Base 100ms
    Sleep(delay);
    simulateClick(x, y);
}
```

**Delay Tiers:**

| Action Type | Base Delay | Jitter | Range |
|------------|-----------|--------|-------|
| **Micro Actions** | 50ms | ±20ms | 30-70ms |
| **Spell Casts** | 200ms | ±50ms | 150-250ms |
| **Target Switch** | 300ms | ±100ms | 200-400ms |
| **Movement Commands** | 500ms | ±200ms | 300-700ms |
| **Combat Actions** | 1.0s | ±0.5s | 0.5-1.5s |
| **Long Actions** | 2.0s | ±1.0s | 1.0-3.0s |

### Mouse Movement Humanization

**Bezier Curve Trajectories:**

Humans don't move mice in straight lines. Glider used Bezier curves to simulate natural mouse movement.

```
Start Point (Current Mouse Position)
    │
    │ Control Point 1 (Random offset)
    │    │
    │    │ Control Point 2 (Random offset)
    │    │    │
    │    │    │
    ▼    ▼    ▼
    ╲    ╲  ●─── Target Point (Click destination)
     ╲    ╲
      ╲    ●
       ╲
        ●
```

```cpp
// Pseudocode for Bezier curve mouse movement
void humanizedMouseMove(POINT start, POINT end, int duration) {
    // Generate random control points
    POINT cp1 = {
        start.x + random(-100, 100),
        start.y + random(-100, 100)
    };
    POINT cp2 = {
        end.x + random(-100, 100),
        end.y + random(-100, 100)
    };

    // Animate along Bezier curve
    for (float t = 0; t <= 1.0; t += 0.01) {
        POINT pos = cubicBezier(start, cp1, cp2, end, t);
        setPosition(pos);

        // Variable speed (slower at start/end)
        float speed = easeInOut(t);
        Sleep(duration * speed * 0.01);
    }
}
```

### Behavioral Randomization

**Fatigue Simulation:**

```cpp
// Simulate human tiredness over time
struct FatigueSystem {
    float sessionStart;
    float currentFatigue;

    void update() {
        float sessionLength = getCurrentTime() - sessionStart;

        // Fatigue increases with session length
        currentFatigue = min(1.0, sessionLength / 4.0);  // Max after 4 hours

        // Increase delays, add pauses
        if (currentFatigue > 0.5) {
            // 5% chance of "distracted" pause
            if (random() < 0.05 * currentFatigue) {
                Sleep(random(5000, 30000));  // 5-30 second pause
            }
        }
    }
};
```

**Mistake Simulation:**

Humans occasionally make mistakes. Glider simulated this:

```cpp
// Simulate occasional human errors
void humanizedAction(Action action) {
    static int actionCount = 0;
    actionCount++;

    // 2% chance of "mistake"
    if (actionCount % 50 == 0 && random() < 0.02) {
        // Wrong target, then correction
        targetWrongEntity();
        Sleep(random(500, 2000));
        cancelTarget();
        targetCorrectEntity();
        return;
    }

    // Normal execution
    executeAction(action);
}
```

### Anti-Pattern Detection

**Timing Analysis Prevention:**

| Detection Vector | Countermeasure |
|------------------|----------------|
| Fixed intervals | Gaussian delay distribution |
| Instant reactions | Minimum 200ms reaction time |
| Perfect efficiency | 5-10% "mistake" rate |
| Linear mouse paths | Bezier curve interpolation |
| 24/7 operation | Scheduled breaks, session limits |

---

## 6. State Management & Decision Making

### Game State Tracking

```cpp
struct GameState {
    // Player state
    PlayerState player;

    // Target state
    UnitState* target;

    // Environment
    std::vector<UnitState> nearbyEnemies;
    std::vector<ItemState> nearbyItems;

    // Combat
    bool inCombat;
    int combatTime;

    // Status
    HealthStatus health;
    ManaStatus mana;
    BuffDebuffList buffs;

    // Position
    Position3D currentPosition;
    Position3D homePosition;

    // Task progress
    int mobsKilled;
    int itemsLooted;
    float experienceGained;
};
```

### Decision Tree Logic

```
                    START TICK
                        │
                        ├─► Is player alive?
                        │    ├─ No ──► RESPAWN STATE
                        │    └─ Yes ──► continue
                        │
                        ├─► Is health low?
                        │    ├─ Yes ──► REST STATE
                        │    └─ No ──► continue
                        │
                        ├─► Is mana low?
                        │    ├─ Yes ──► REST STATE
                        │    └─ No ──► continue
                        │
                        ├─► Have target?
                        │    ├─ No ──► SEARCH STATE
                        │    └─ Yes ──► continue
                        │
                        ├─► Is target hostile?
                        │    ├─ Yes ──► COMBAT STATE
                        │    └─ No ──► SEARCH STATE
                        │
                        └─► DEFAULT STATE
```

### State Transition Validation

```cpp
bool canTransition(State from, State to) {
    // Define valid transitions
    static std::map<State, std::set<State>> validTransitions = {
        {STATE_IDLE,     {STATE_SEARCH, STATE_NAVIGATE}},
        {STATE_SEARCH,   {STATE_COMBAT, STATE_NAVIGATE, STATE_REST}},
        {STATE_COMBAT,   {STATE_LOOT, STATE_FLEE, STATE_REST}},
        {STATE_LOOT,     {STATE_SEARCH, STATE_REST, STATE_NAVIGATE}},
        {STATE_REST,     {STATE_SEARCH, STATE_NAVIGATE}},
        {STATE_FLEE,     {STATE_REST, STATE_NAVIGATE}},
        {STATE_NAVIGATE, {STATE_SEARCH, STATE_REST}}
    };

    auto allowed = validTransitions[from];
    return allowed.find(to) != allowed.end();
}
```

### Event-Driven Updates

While the main loop ran on a timer, certain events triggered immediate state changes:

```cpp
enum GameEvent {
    EVENT_TARGET_DIED,
    EVENT_HEALTH_LOW,
    EVENT_MANA_LOW,
    EVENT_INVENTORY_FULL,
    EVENT_DIED,
    EVENT_RESPAWNED,
    EVENT_STUCK,
    EVENT_AGGRO_ADDITIONAL
};

void onEvent(GameEvent event, void* data) {
    switch (event) {
        case EVENT_TARGET_DIED:
            currentState = STATE_LOOT;
            break;

        case EVENT_HEALTH_LOW:
            if (player.health < 20) {
                currentState = STATE_FLEE;
            } else {
                currentState = STATE_REST;
            }
            break;

        case EVENT_STUCK:
            currentState = STATE_UNSTUCK;
            break;

        // ... more events
    }
}
```

---

## 7. Movement & Pathfinding

### Waypoint Navigation System

WoW Glider used **recorded waypoint paths** for navigation, not real-time pathfinding like A*.

```
Waypoint Path Structure:
┌──────────────────────────────────────────────────────────────────┐
│  Waypoint 0 ──► Waypoint 1 ──► Waypoint 2 ──► ... ──► Waypoint N │
│      │              │               │                    │       │
│      ▼              ▼               ▼                    ▼       │
│   [X,Y,Z]        [X,Y,Z]         [X,Y,Z]              [X,Y,Z]    │
│   action=        action=         action=             action=    │
│   START          MOVE            MOVE                 END        │
│                                                                 │
│  Each waypoint contains:                                        │
│  - 3D coordinates (float x, y, z)                              │
│  - Action type (MOVE, WAIT, INTERACT)                          │
│  - Optional parameters (target, delay, etc.)                   │
└──────────────────────────────────────────────────────────────────┘
```

```cpp
struct Waypoint {
    float x, y, z;
    WaypointAction action;
    float tolerance;  // How close to get before considering "arrived"
    int waitTime;     // Time to wait at this waypoint
};

enum WaypointAction {
    ACTION_MOVE,      // Move to this waypoint
    ACTION_WAIT,      // Wait for specified time
    ACTION_INTERACT,  // Interact with NPC/object
    ACTION_PULL,      // Pull mob (start combat)
    ACTION_END        // End of path
};
```

### Path Following Algorithm

```cpp
// Simple waypoint following with tolerance
void followWaypointPath(std::vector<Waypoint> path) {
    int currentWaypoint = 0;

    while (currentWaypoint < path.size()) {
        Waypoint target = path[currentWaypoint];

        // Calculate distance to target
        float distance = calculateDistance(player.position, target);

        if (distance < target.tolerance) {
            // Arrived at waypoint
            if (target.action == ACTION_WAIT) {
                Sleep(target.waitTime);
            } else if (target.action == ACTION_INTERACT) {
                interactWithTarget();
            }

            currentWaypoint++;
        } else {
            // Move towards waypoint
            moveTowards(target);
            Sleep(100);  // Tick rate
        }
    }
}
```

### Stuck Detection

**Frame Counter Method:**

```cpp
struct StuckDetector {
    Position3D lastPosition;
    int stuckFrameCount;
    static const int STUCK_THRESHOLD = 100;  // 5 seconds at 20 FPS

    void update() {
        Position3D currentPosition = player.position;
        float distanceMoved = calculateDistance(lastPosition, currentPosition);

        if (distanceMoved < 0.1) {  // Essentially not moving
            stuckFrameCount++;

            if (stuckFrameCount > STUCK_THRESHOLD) {
                // We're stuck!
                onStuck();
            }
        } else {
            // Moving normally, reset counter
            stuckFrameCount = 0;
            lastPosition = currentPosition;
        }
    }

    void onStuck() {
        // Recovery strategies:
        // 1. Try jumping
        // 2. Try random direction
        // 3. Return to last known good position
        // 4. Recalculate path

        LOG("Stuck detected, attempting recovery");

        switch (stuckFrameCount % 4) {
            case 0: jump(); break;
            case 1: moveRandomDirection(); break;
            case 2: returnToLastPosition(); break;
            case 3: recalculatePath(); break;
        }
    }
};
```

**Position History Method:**

```cpp
// Detect circular movement (stuck in loop)
struct LoopDetector {
    std::deque<Position3D> recentPositions;
    static const int HISTORY_SIZE = 20;

    void update(Position3D pos) {
        recentPositions.push_back(pos);
        if (recentPositions.size() > HISTORY_SIZE) {
            recentPositions.pop_front();
        }

        // Check if we're visiting same positions repeatedly
        int duplicateCount = 0;
        for (const auto& oldPos : recentPositions) {
            if (calculateDistance(pos, oldPos) < 1.0) {
                duplicateCount++;
            }
        }

        if (duplicateCount >= 3) {
            // Loop detected
            onLoopDetected();
        }
    }
};
```

### Movement Humanization

**Variable Speed:**

```cpp
float calculateMoveSpeed() {
    float baseSpeed = player.getBaseMoveSpeed();

    // Add random variation (±5%)
    float variation = 1.0 + random(-0.05, 0.05);

    // Apply terrain modifiers
    if (isInWater()) variation *= 0.5;
    if (isSwimming()) variation *= 0.7;

    return baseSpeed * variation;
}
```

**Path Smoothing:**

Instead of moving directly between waypoints, bots would add slight curves:

```cpp
Position3D getIntermediateTarget(Position3D current, Position3D waypoint) {
    // Add perpendicular offset to create slight curve
    Vector3D direction = normalize(waypoint - current);
    Vector3D perpendicular = getPerpendicular(direction);

    float offsetAmount = random(-2.0, 2.0);  // ±2 units
    Position3D offset = waypoint + (perpendicular * offsetAmount);

    return offset;
}
```

---

## 8. Combat System

### Combat State Machine

```
                    COMBAT STATE ENTERED
                            │
                            ▼
                    ┌───────────────┐
                    │   PULL PHASE  │
                    │  Initiate     │
                    │  Combat       │
                    └───────────────┘
                            │
                            │ Target in range
                            ▼
                    ┌───────────────┐
                    │ ROTATION LOOP │◄────┐
                    └───────────────┘     │
                            │             │
                            │ Check       │
                            ▼             │
              ┌──────────────────────────┤
              │                          │
              ▼                          ▼
    ┌─────────────────┐        ┌─────────────────┐
    │ ABILITY READY?  │        │ NEED TO MOVE?   │
    │ Cooldown check  │        │ Positioning     │
    └─────────────────┘        └─────────────────┘
              │                          │
         Yes │  │No                  Yes│  │No
              ▼  ▼                       ▼  ▼
    ┌─────────────┐              ┌─────────────┐
    │ CAST ABILITY│              │ REPOSITION  │
    │ (Priority)  │              │ (Kiting,    │
    └─────────────┘              │  LoS, etc.) │
         │                        └─────────────┘
         │                              │
         └──────────────┬───────────────┘
                        │
                        ▼
               ┌─────────────────┐
               │ TARGET ALIVE?   │
               └─────────────────┘
                        │
                   Yes  │  │ No
                        ▼  ▼
                   ┌─────────┐  ┌──────────┐
                   │ Continue│  │ EXIT     │
                   │ Loop    │  │ COMBAT   │
                   └─────────┘  └──────────┘
```

### Combat Rotation System

```cpp
struct CombatAction {
    std::string spellName;
    int priority;           // Higher = use first
    float minMana;          // Minimum mana required
    float maxRange;         // Maximum range
    float cooldown;         // Cooldown in seconds
    float lastCast;         // Last cast time
    HealthCondition health; // Health condition

    bool canCast(const Player& player, const Target& target) {
        if (player.mana < minMana) return false;
        if (target.distance > maxRange) return false;
        if (getCurrentTime() - lastCast < cooldown) return false;
        if (!health.meetsCondition(target.healthPercent)) return false;
        return true;
    }
};

class CombatRotation {
    std::vector<CombatAction> actions;

    void executeRotation(Player& player, Target& target) {
        while (target.isAlive()) {
            // Sort by priority
            std::sort(actions.begin(), actions.end(),
                [](const CombatAction& a, const CombatAction& b) {
                    return a.priority > b.priority;
                });

            // Find highest priority action we can cast
            for (auto& action : actions) {
                if (action.canCast(player, target)) {
                    castSpell(action.spellName);
                    action.lastCast = getCurrentTime();

                    // Humanized delay
                    Sleep(random(200, 500));
                    break;
                }
            }

            Sleep(100);  // Tick rate
        }
    }
};
```

### Target Selection

```cpp
struct TargetPriority {
    int level;              // Prefer same/higher level
    float distance;         // Prefer closer targets
    int currentHealth;      // Prefer lower health (execute)
    bool isElite;           // Avoid elites (usually)
    int aggroCount;         // Avoid targets with many friends
    float score;

    float calculateScore() {
        score = 0;

        // Level scoring (prefer ±2 levels)
        int levelDiff = abs(level - player.level);
        score += max(0, 10 - levelDiff);

        // Distance scoring (prefer closer)
        score += max(0, 10 - (distance / 5.0));

        // Health scoring (prefer low health for execute)
        score += (100 - currentHealth) / 10.0;

        // Elite penalty
        if (isElite) score -= 50;

        // Aggro penalty
        score -= aggroCount * 5;

        return score;
    }
};

Target selectBestTarget(std::vector<Unit> nearbyEnemies) {
    TargetPriority best;
    best.score = -999;

    for (auto& enemy : nearbyEnemies) {
        TargetPriority tp;
        tp.level = enemy.level;
        tp.distance = calculateDistance(player, enemy);
        tp.currentHealth = enemy.healthPercent;
        tp.isElite = enemy.isElite;
        tp.aggroCount = countAggroMobs(enemy);

        tp.calculateScore();

        if (tp.score > best.score) {
            best = tp;
        }
    }

    return getTargetFromPriority(best);
}
```

### Kiting and Positioning

```cpp
// Maintain optimal distance (kiting for ranged)
void maintainDistance(float optimalDistance) {
    float currentDistance = calculateDistance(player, target);

    if (currentDistance < optimalDistance * 0.8) {
        // Too close, move away
        Vector3D awayDirection = normalize(player.position - target.position);
        moveInDirection(awayDirection);
    } else if (currentDistance > optimalDistance * 1.2) {
        // Too far, move closer
        Vector3D towardDirection = normalize(target.position - player.position);
        moveInDirection(towardDirection);
    }
}
```

### Cooldown Management

```cpp
class CooldownTracker {
    std::map<std::string, float> cooldowns;
    float globalCooldown;  // WoW GCD (typically 1.5s)
    float lastGlobalCooldown;

    bool isOnCooldown(const std::string& spell) {
        if (cooldowns.find(spell) == cooldowns.end()) {
            return false;  // Not tracked, no cooldown
        }

        float elapsed = getCurrentTime() - cooldowns[spell];
        return elapsed < getSpellCooldown(spell);
    }

    bool isGlobalCooldownReady() {
        float elapsed = getCurrentTime() - lastGlobalCooldown;
        return elapsed >= globalCooldown;
    }

    void onCastSpell(const std::string& spell) {
        cooldowns[spell] = getCurrentTime();
        lastGlobalCooldown = getCurrentTime();
    }
};
```

---

## 9. Failure Recovery

### Error Detection

```cpp
enum ErrorType {
    ERROR_STUCK,              // Not moving despite commands
    ERROR_TARGET_LOST,        // Target disappeared/despawned
    ERROR_LINE_OF_SIGHT,      // Can't see target
    ERROR_OUT_OF_RANGE,       // Target moved out of range
    ERROR_INVENTORY_FULL,     // Can't loot
    ERROR_DEATH,              // Player died
    ERROR_DISCONNECTED,       // Network disconnect
    ERROR_UNKNOWN             // Catch-all
};

struct ErrorCondition {
    ErrorType type;
    int occurrenceCount;
    float firstOccurrence;
    float lastOccurrence;

    bool isRecurring() {
        return occurrenceCount > 3;
    }

    float getTimeSinceFirst() {
        return getCurrentTime() - firstOccurrence;
    }
};
```

### Recovery Strategies

**Stuck Recovery:**

```cpp
class StuckRecovery {
    int attemptCount;
    static const int MAX_ATTEMPTS = 5;

    void recoverFromStuck() {
        attemptCount++;

        switch (attemptCount) {
            case 1:
                // First attempt: Jump
                pressKey(KEY_SPACE);
                Sleep(500);
                break;

            case 2:
                // Second attempt: Move backwards slightly
                moveBackward(2.0);
                Sleep(1000);
                break;

            case 3:
                // Third attempt: Random direction
                turn(random(-180, 180));
                moveForward(3.0);
                Sleep(1500);
                break;

            case 4:
                // Fourth attempt: Return to last known good position
                returnToLastGoodPosition();
                break;

            case MAX_ATTEMPTS:
                // Last resort: Hearthstone / logout
                LOG("Unable to recover from stuck, hearthing");
                castSpell("Hearthstone");
                break;
        }
    }

    void reset() {
        attemptCount = 0;
    }
};
```

**Death Recovery:**

```cpp
class DeathRecovery {
    void onPlayerDeath() {
        LOG("Player died, running death recovery");

        // Release spirit
        releaseSpirit();
        Sleep(2000);

        // Find nearest graveyard
        Position3D graveyard = findNearestGraveyard();

        // Navigate to corpse
        navigateTo(graveyard);

        // Wait at corpse location
        waitForResurrection();

        // Resurrect
        resurrect();

        // Evaluate: Continue or hearth?
        if (shouldHearth()) {
            castSpell("Hearthstone");
            return;
        }

        // Continue with current task
        restoreState();
    }

    bool shouldHearth() {
        // Hearth if:
        // - Durability is low
        // - Far from task location
        // - Died multiple times recently
        return (getDurabilityPercent() < 20) ||
               (getDeathCount() > 3);
    }
};
```

**Network Disconnect Recovery:**

```cpp
class DisconnectRecovery {
    bool isConnected() {
        // Check if WoW process is still running
        // and responsive to memory reads
        return isProcessRunning("WoW.exe") &&
               canReadMemory();
    }

    void reconnect() {
        LOG("Disconnected, attempting reconnect");

        // Close WoW
        terminateProcess("WoW.exe");
        Sleep(5000);

        // Restart WoW
        launchWoW();
        Sleep(15000);  // Wait for load

        // Login
        enterCredentials();
        pressEnter();
        Sleep(5000);

        // Select character
        selectCharacter(characterName);
        pressEnter();
        Sleep(10000);

        // Restore state
        restoreSession();
    }
};
```

### State Persistence

```cpp
struct BotState {
    // Current task
    TaskType currentTask;
    int currentWaypoint;

    // Position
    Position3D lastKnownPosition;
    Position3D homePosition;

    // Statistics
    int mobsKilled;
    int itemsLooted;
    int deathCount;
    float sessionStart;

    // Error tracking
    std::map<ErrorType, ErrorCondition> errors;

    void save() {
        std::ofstream file("bot_state.json");
        json j;
        j["currentTask"] = currentTask;
        j["currentWaypoint"] = currentWaypoint;
        j["lastKnownPosition"] = lastKnownPosition.toJSON();
        j["mobsKilled"] = mobsKilled;
        // ... more fields
        file << j.dump();
    }

    void load() {
        std::ifstream file("bot_state.json");
        json j;
        file >> j;

        currentTask = j["currentTask"];
        currentWaypoint = j["currentWaypoint"];
        lastKnownPosition = Position3D::fromJSON(j["lastKnownPosition"]);
        mobsKilled = j["mobsKilled"];
        // ... more fields
    }
};
```

---

## 10. Anti-Detection Strategies

### Warden Anti-Cheat System

Blizzard's **Warden** client was active during WoW Glider's operation and scanned for:

1. **Memory signature scanning** - Detect known bot code patterns
2. **DLL injection detection** - Find injected libraries
3. **Process enumeration** - Check for known bot processes
4. **Window title reading** - Detect bot windows
5. **API hook detection** - Find hooked functions

### Detection Avoidance Techniques

**Code Obfuscation:**

```cpp
// Obfuscate memory reads to avoid signature detection
uintptr_t obfuscatedRead(uintptr_t address) {
    // Split read into multiple operations
    // Use different API calls
    // Add junk code

    // Method 1: Direct read (sometimes)
    if (random() < 0.3) {
        return *(uintptr_t*)address;
    }

    // Method 2: ReadProcessMemory
    else if (random() < 0.6) {
        uintptr_t value;
        ReadProcessMemory(hProcess, (LPCVOID)address, &value, sizeof(value), NULL);
        return value;
    }

    // Method 3: Custom read routine
    else {
        return customMemoryRead(address);
    }
}
```

**Process Hiding:**

```cpp
// Rename window to avoid detection
void hideWindow() {
    // Change window title from "WoW Glider" to something innocent
    SetWindowText(hWnd, "Notepad");

    // Change process name (requires executable name change)
    // "WoWGlider.exe" → "calc.exe"
}

// Avoid DLL detection by not injecting
// Glider was external, not injected
```

**Timing Obfuscation:**

```cpp
// Randomize tick rate to avoid detection
void mainLoop() {
    while (running) {
        float baseTickRate = 100.0;  // 100ms
        float jitter = random(-20, 20);  // ±20ms
        float tickRate = baseTickRate + jitter;

        tick();
        Sleep(tickRate);
    }
}
```

**Memory Pattern Evasion:**

```cpp
// Avoid static patterns in memory
void* dynamicAllocation(size_t size) {
    // Use different allocation methods
    if (random() < 0.5) {
        return malloc(size);
    } else {
        return VirtualAlloc(NULL, size, MEM_COMMIT, PAGE_READWRITE);
    }
}

// Encrypt sensitive strings
char* decryptString(const char* encrypted) {
    // Simple XOR cipher
    size_t len = strlen(encrypted);
    char* decrypted = new char[len + 1];

    for (size_t i = 0; i < len; i++) {
        decrypted[i] = encrypted[i] ^ 0x55;
    }
    decrypted[len] = '\0';

    return decrypted;
}
```

**Behavioral Evasion:**

```cpp
// Don't run 24/7
void scheduleBreaks() {
    float sessionLength = getCurrentTime() - sessionStart;

    // Take break every 2-4 hours
    if (sessionLength > random(7200, 14400)) {
        LOG("Taking scheduled break");
        logout();
        Sleep(random(1800, 3600));  // 30-60 minute break
        login();
    }
}

// Respond to whispers
void onWhisper(Player from, std::string message) {
    // Simple pre-set responses
    std::string responses[] = {
        "sorry, afk",
        "brb",
        "busy",
        "can't talk",
        "..."
    };

    std::string response = responses[random(0, 4)];
    sendWhisper(from, response);
}
```

---

## 11. Design Patterns Identified

### Architectural Patterns

| Pattern | Description | Application in WoW Glider |
|---------|-------------|--------------------------|
| **Finite State Machine** | System with discrete states and transitions | Core bot behavior (IDLE, SEARCH, COMBAT, etc.) |
| **Component-Based** | System composed of modular components | Separate modules for combat, navigation, looting |
| **Event-Driven** | Responds to events rather than polling | Game events triggering state changes |
| **Observer Pattern** | Objects subscribe to notifications | Combat log updates, position changes |
| **Strategy Pattern** | Interchangeable algorithms | Different combat rotations for different classes |
| **Factory Pattern** | Create objects without specifying exact class | Action creation based on configuration |
| **Singleton Pattern** | Single instance of critical resources | Memory reader, configuration manager |

### Behavioral Patterns

| Pattern | Description | Application |
|---------|-------------|-------------|
| **Command Pattern** | Encapsulate actions as objects | Input simulation (press key, click) |
| **Chain of Responsibility** | Pass requests along chain | Error handling, stuck recovery |
| **Template Method** | Skeleton algorithm with customizable steps | Combat rotation framework |
| **State Pattern** | Encapsulate state-specific behavior | Each FSM state as separate class |
| **Iterator Pattern** | Traverse collections without exposing structure | Object manager traversal |

### Creational Patterns

| Pattern | Description | Application |
|---------|-------------|-------------|
| **Builder Pattern** | Construct complex objects step-by-step | Waypoint path construction |
| **Prototype Pattern** | Clone existing objects | Mob template copying |

---

## 12. Lessons for Legitimate Game AI

### Applicable Patterns

**1. Finite State Machines for Bot Behavior**

FSMs are excellent for game AI due to their:
- Simplicity and debuggability
- Predictable behavior
- Low computational overhead
- Clear visual representation

**Application:** Use FSMs for NPC AI, companion behavior, tactical decisions.

**2. Separation of Brain and Script Layers**

WoW Glider's architecture anticipated modern "One Abstraction Away" philosophy:
- **Brain Layer:** High-level decision making (what to do)
- **Script Layer:** Low-level execution (how to do it)

**Application:** LLMs for planning, traditional AI for execution.

**3. Humanization Through Randomization**

Adding statistical variation to all actions creates more natural behavior:
- Gaussian delays instead of fixed timing
- Bezier curves for mouse movement
- Occasional "mistakes" and pauses
- Fatigue simulation over long sessions

**Application:** Make AI companions feel more characterful and less robotic.

**4. Robust Error Recovery**

Comprehensive failure handling improves reliability:
- Multiple recovery strategies (jump, reverse, random, return)
- State persistence for crash recovery
- Graceful degradation when stuck

**Application:** AI agents that recover from errors without manual intervention.

**5. Modular Component Design**

Separating concerns into independent components enables:
- Easier testing and debugging
- Code reuse across different bot types
- Independent optimization of subsystems

**Application:** Plugin architecture for extensible AI systems.

### Ethical Considerations

**What NOT to Do:**

1. **Don't violate Terms of Service** - Always respect game rules
2. **Don't use memory reading on live games** - This violates most EULAs
3. **Don't interfere with other players' experience** - Bots ruin games for others
4. **Don't compromise game security** - Detection evasion is unethical for legitimate AI

**What TO Do:**

1. **Build legitimate AI companions** - NPCs that enhance single-player or co-op experiences
2. **Study architecture patterns** - Learn from historical systems without copying exploits
3. **Contribute to research** - Advance game AI as an academic discipline
4. **Build for modding-friendly games** - Minecraft, Skyrim, etc. encourage AI mods

### Technical Takeaways for Steve AI

**From WoW Glider's Architecture:**

| Glider Feature | Steve AI Adaptation |
|----------------|-------------------|
| **Memory-based state reading** | Use Minecraft API instead (legitimate) |
| **External process bot** | Internal mod (Forge API integration) |
| **Combat rotation priority system** | Task priority queue for Steve agents |
| **Waypoint navigation** | Hierarchical pathfinding (A*) |
| **Stuck detection** | Movement validator in pathfinding system |
| **Humanization delays** | Natural timing for character interactions |
| **Finite State Machine** | AgentStateMachine for behavior management |
| **Object manager traversal** | Entity tracking via Minecraft API |
| **Death recovery** | Respawn handling for agent persistence |
| **Error recovery strategies** | ErrorRecoveryStrategy and RetryPolicy |

**Improvements Over Glider:**

| Area | Glider Approach | Steve AI Improvement |
|------|----------------|---------------------|
| **State Access** | External memory reading (fragile) | Official Forge API (stable) |
| **Decision Making** | Hardcoded rotations | LLM-powered planning (flexible) |
| **Learning** | None (static scripts) | Skill library with auto-generation |
| **Multi-Agent** | Single bot per account | True multi-agent coordination |
| **Conversation** | None (automated responses) | Rich dialogue with personality |
| **Architecture** | Monolithic FSM | Hybrid FSM + Behavior Trees + HTN |
| **Detection** | Warden anti-cheat | No detection needed (legitimate mod) |

---

## 13. References

### Primary Sources

1. **MDY Industries, LLC v. Blizzard Entertainment, Inc.** - Wikipedia
   - URL: https://en.wikipedia.org/wiki/MDY_Industries,_LLC_v._Blizzard_Entertainment,_Inc.

2. **Exploiting Online Games** (Chinese: 网络游戏安全揭密) - Greg Hoglund, Gary McGraw
   - Baidu Baike: https://baike.baidu.com/item/网络游戏安全揭密/3660760
   - Kongfz Book: https://mbook.kongfz.com/754391/8642105913/

3. **暴雪和黑客的战争系列** (The War Between Blizzard and Hackers) - CSDN (2008)
   - Series covering Warden vs. bot evolution

4. **魔兽世界Warden的简略分析** (WoW Warden Brief Analysis) - CSDN (2008)
   - Technical analysis of Warden's protocol and detection methods

5. **规避网络游戏的外挂检测机制** (Bypassing Online Game Anti-Cheat) - CSDN (2008)
   - Translation of Darawk's anti-Warden techniques from rootkit.com

### Game AI Architecture References

6. **Game AI Pro 3: Collected Wisdom of Game AI Professionals** (2017)
   - Section on Movement and Pathfinding
   - Section on Combat AI Architecture
   - URL: http://finelybook.com/game-ai-pro-3-collected-wisdom-of-game-ai-professionals/

7. **AI Game Programming Wisdom** (Series)
   - A* Pathfinding chapters
   - Combat AI architecture
   - URL: https://mbook.kongfz.com/241290/6977425913/

### Technical Pattern References

8. **Finite State Machine in Game AI** - CSDN
   - URL: https://blog.csdn.net/qq_33060405/article/details/148981492

9. **A* Pathfinding for 2D Grid-Based Platformers** - TutsPlus
   - URL: https://code.tutsplus.com/a-pathfinding-for-2d-grid-based-platformers-making-a-bot-follow-the-path--cms-24913t

10. **Left 4 Dead AI System** (Translated) - CSDN
    - URL: https://blog.csdn.net/u011643833/article/details/79684019

11. **Handling Dynamic Obstacles** - CNBlogs
    - URL: https://www.cnblogs.com/xuuold/articles/10397945.html

12. **Game-Bot Framework** - GitCode
    - Python game automation framework
    - URL: https://gitcode.com/gh_mirrors/ga/Game-Bot

13. **Awesome Game Security** - GitHub
    - Curated list of game security resources
    - URL: https://github.com/BlackTom900131/awesome-game-security

### Historical Context

14. **Cheat Engine Memory Scanning Documentation**
    - Pattern scanning techniques
    - URL: https://wiki.cheatengine.org/index.php?title=Cheat_Engine:Memory_Scanning

15. **Pointer Scanning for Game Hacking** - CNBlogs
    - Dynamic address location
    - URL: https://www.cnblogs.com/apachecn/p/19291511

---

## Appendix: Legal and Ethical Note

**Important:** This document is provided for **academic research purposes only**. WoW Glider was ruled to violate copyright law and Blizzard's Terms of Service. The analysis of its architecture is intended to:

1. **Document historical approaches** to game automation
2. **Extract design patterns** applicable to legitimate game AI
3. **Inform research** on companion AI systems
4. **Contribute to academic understanding** of game bot architecture

**For legitimate development:**
- Always respect game Terms of Service
- Use official APIs when available
- Build for modding-friendly games
- Don't interfere with other players' experiences
- Contribute to open-source AI research

**This analysis does not provide:**
- Specific memory addresses for current game versions
- Bypass techniques for modern anti-cheat systems
- Exploitation methods for cheating
- Tools for violating game rules

---

**Document End**

*For questions or clarifications about this research, please refer to the academic project guidelines and ethical AI development practices.*
