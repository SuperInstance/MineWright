# RTS and MMO Automation History: Patterns and Techniques

**Research Date:** 2025-02-28
**Researcher:** AI Investigation
**Purpose:** Document automation patterns from RTS and MMO games for potential application in AI agent systems

---

## Executive Summary

This document compiles research on automation patterns from Real-Time Strategy (RTS) games (StarCraft, Age of Empires) and MMORPGs. These patterns offer valuable insights for building autonomous AI agents that can execute complex, multi-step tasks with natural behavior patterns.

**Key Insight:** Game automation has evolved from simple macros to sophisticated systems that emulate human behavior through statistical modeling, machine learning, and advanced input simulation.

---

## Table of Contents

1. [RTS Automation Patterns](#1-rts-automation-patterns)
2. [MMO Bot Patterns](#2-mmo-bot-patterns)
3. [Core Automation Techniques](#3-core-automation-techniques)
4. [Macro Recording Systems](#4-macro-recording-systems)
5. [Humanization and Anti-Detection](#5-humanization-and-anti-detection)
6. [Architectural Patterns](#6-architectural-patterns)
7. [Lessons for AI Agent Development](#7-lessons-for-ai-agent-development)

---

## 1. RTS Automation Patterns

### 1.1 Build Order Scripts

Build orders are pre-planned sequences of actions that optimize early-game development. They represent the foundation of RTS automation.

#### StarCraft AI: UAlbertaBot
- **Competition Winner:** 2013 AIIDE StarCraft AI competition
- **Core Algorithm:** Depth-first branch and bound build-order search
- **Capability:** Produces professional-quality build orders in real-time
- **Customization:** Highly configurable through BWAPI integration
- **Source:** [UAlbertaBot on GitCode](https://gitcode.com/gh_mirrors/ua/ualbertabot)

#### CommandCenter (StarCraft II)
- **Architecture:** Built on UAlbertaBot foundation
- **Features:**
  - JSON-based configuration files for custom strategies
  - WorkerManager for resource gathering optimization
  - Building placement algorithm finding nearest buildable locations
  - Support for all three races (Terran, Protoss, Zerg)
- **Repository:** [CommandCenter GitHub](https://github.com/davechurchill/commandcenter)
- **Source:** [CommandCenter Documentation](https://www.jiqizhixin.com/articles/2017-08-11-2)

#### Age of Empires IV: AoE4BO Tool
- **Technology:** Image recognition for resource/population detection
- **Resolution:** Works at 1920x1080
- **Script Syntax:**
  ```
  7s: At 7 population
  20sc: When population cap is 20
  200f: At 200 food
  200w: At 200 wood
  200g: At 200 gold
  2t: At 2 seconds
  400f,200g: When both 400 food AND 200 gold
  ```
- **Repository:** [AoE4BO on GitHub](https://github.com/nordie92/AoE4BO)

#### Age of Empires II: AI Scripting Language
- **Syntax:** Rule-based `(defrule <conditions> => <actions>)`
- **Features:**
  - IF-THEN conditional statements
  - Commands like `train-unit` with quantity parameters
  - UserPatch 1.3 adds: variables, loops, conditional detection
- **Resources:** [Awesome AoE2 on GitHub](https://github.com/Arkanosis/awesome-aoe2)
- **Source:** [AoE AI Tutorial Research](https://m.zhangqiaokeyan.com/academic-conference-foreign_meeting-236192_thesis/020516251783.html)

### 1.2 Build Order Optimization Algorithms

#### Research Approaches
- **Heuristic Search:** Real-time build-order optimization
- **Genetic Algorithms:** Multi-objective optimization for concurrent action sequences
- **Production Constrained Build-order Optimization (PC-BO):** Maximizes early-game unit production under dependency and resource constraints
- **Imitation Learning:** Training on human replays to replicate professional strategies
- **Source:** [Build Order Research](https://m.zhangqiaokeyan.com/academic-conference-foreign_7th-world-multiconference-systemics-cybernetics-and-informatics_thesis_0705013049605.html)

#### Key Principles
1. **Dependency Management:** Buildings/units unlock other options
2. **Resource Timing:** Resources must be available when needed
3. **Concurrent Actions:** Multiple actions can occur simultaneously
4. **Critical Path:** Identify bottlenecks in production chains

### 1.3 Resource Gathering Automation

#### Full Automation Pattern
- **Concept:** Player assigns worker once, worker continues automatically
- **Implementation:**
  - Workers automatically move between resource nodes and drop-off points
  - Auto-reassignment when resources deplete
  - Intelligent worker behavior (flee when attacked)
- **Source:** [RTS Automation Design](https://baijiahao.baidu.com/s?id=1665631241958600529)

#### Queue-Based Resource Management
- **Task Queueing:** Workers follow sequential task lists
  - Example: Gather berries → Switch to farming when depleted
- **Resource Pre-deduction:** Resources deducted when queued vs. when production starts
- **Priority Queues:** Higher priority tasks processed first
- **Source:** [Resource Allocation Patterns](https://www.360doc.cn/article/4238731_66859700.html)

#### Advanced Pathfinding
- **Memetic Ant Colony System:** Adaptive resource gathering with path-finding optimization
- **Believable Behaviors:** Creating consistent, realistic AI movement patterns
- **Source:** [Resource Gathering Research](https://m.zhangqiaokeyan.com/academic-journal-foreign_detail_thesis_0204119274449.html)

### 1.4 Unit Production Queues

#### ProductionManager Architecture
- **Core Responsibilities:**
  - Building and unit production
  - Queue management and prioritization
  - Worker allocation
  - Building state tracking (ready, assigned, constructing, complete)

#### Production Rounds / Macro Rounds
- **Pattern:** Cycle through production facilities, queueing one unit per building
- **Benefit:** Units complete simultaneously for efficient production cycles
- **Example:**
  - Hellion build time: 30s
  - Thor build time: 60s
  - Strategy: Queue 2 Hellions per 1 Thor for synchronized completion

#### Queue Management Principles
1. **Don't Over-Queue:** Queued resources sit idle
2. **Don't Under-Produce:** Idle facilities waste time
3. **Optimal Timing:** Produce units right after previous completes
4. **Priority Systems:** Essential items take precedence

### 1.5 Auto-Scouting Patterns

#### Age of Empires Auto Scout
- **Mechanic:** AI-controlled unit automatically scouts map
- **Pattern:** Pre-determined yet semi-random path
- **Limitations:**
  - Not programmed to immediately find bases/resources
  - Doesn't understand resource grouping (e.g., herdable animals in pairs)
- **Supported Games:** AoE2:DE, AoE3:DE, Age of Mythology: Retold
- **Source:** [Auto Scout Wiki](https://ageofempires.fandom.com/wiki/Auto_Scout)

#### Scouting Strategy Principles
- **Critical Locations:**
  - Enemy barracks/production buildings
  - Enemy army staging areas
  - Enemy resource areas
- **Vision Control:** Scouting determines victory potential
- **Information Advantage:** Reveal enemy intentions for appropriate response
- **Source:** [RTS Vision Strategy](https://m.taptap.cn/moment/290596478705994086)

#### AI Research Platforms
- **ELF Platform's MiniRTS:**
  - Resource gathering, building construction
  - Scouting unknown territories
  - Attack/defend mechanics
- **Source:** [ELF Research Platform](https://gitee.com/xsongx/ELF)

### 1.6 Attack Timing and Composition

#### Combat Simulation
- **Unit Micromanagement:** Fuzzy integral + heuristic search for optimal control
- **Spatial Configuration:** Unit positioning and formation
- **Composition Balancing:** Rock-paper-scissors unit counter relationships
- **Source:** [Unit Micromanagement Research](https://m.zhangqiaokeyan.com/academic-conference-foreign_meeting-221589_thesis_0705016204108.html)

#### Imitation Learning for Combat
- **Approach:** Search human replay database for similar situations
- **Focus:** Combat considering spatial configuration and unit types
- **Success:** Defeated competitive AI opponents
- **Source:** [Combat Imitation Learning](https://m.zhangqiaokeyan.com/academic-conference-foreign_meeting-236192_thesis_020516251783.html)

---

## 2. MMO Bot Patterns

### 2.1 Grinding/Leveling Automation

#### Pattern Overview
Grinding automation involves repetitive combat or resource collection for experience points and items.

#### Technical Approaches
- **Pixel Detection:** Screen reading to identify enemies, resource nodes, UI elements
- **Memory Reading:** Direct access to game state variables (health, mana, position)
- **Input Injection:** Simulated keyboard/mouse commands
- **Timing Systems:** Cooldown management and ability sequencing

#### Commercial Example: MANIA MapleStory Bot
- **Repository:** [MANIA Bot on GitHub](https://github.com/Lyze96/MANIA-Maplestory-Bot)
- **Features:**
  - Automated grinding
  - Loot collection
  - Potion management
  - Anti-detection measures

#### Anti-Detection Considerations
- **Account Risk:** Botting typically violates Terms of Service
- **Consequences:** Account suspension, permanent bans, progress loss
- **Recommendation:** Use legitimate leveling guides, in-game boost items, group play

### 2.2 Resource Node Detection and Routing

#### Detection Methods
1. **Pixel-Based Detection:**
   - Color matching for resource nodes
   - Pattern recognition for minimap dots
   - Template matching for specific objects

2. **Memory Reading:**
   - Direct access to entity coordinates
   - Resource type and quantity information
   - Respawn timer tracking

3. **Computer Vision:**
   - Object detection models (YOLO, etc.)
   - Semantic segmentation for terrain analysis
   - Optical character recognition for UI text

#### Routing Algorithms
- **Traveling Salesperson Problem (TSP):** Optimal node visitation order
- **Pathfinding:** A*, Dijkstra for obstacle avoidance
- **Respawn Awareness:** Timing routes to node regeneration cycles
- **Dynamic Re-routing:** Adapting to player competition or node depletion

#### Example: Genshin Impact Auto-Mining (2025)
- **Technology:** Screen analysis and input automation
- **Features:**
  - Crystal node detection
  - Automated routing
  - Anti-detection techniques
- **Source:** [Genshin Auto-Mining Guide](https://www.sohu.com/a/926146289_121979110) (Chinese)

### 2.3 Combat Rotation Scripts

#### What is a Combat Rotation?
A rotation is the sequence of spells/abilities used during combat to maximize damage, healing, or survivability.
- **Source:** [Combat Rotation Definition](https://chimpeon.com/games/what-is-a-combat-rotation)

#### Rotation Types

##### 1. Fixed Rotation
```
Ability A → Ability B → Ability C → Ability D → Repeat
```
- **Use Case:** Simple classes with predictable cooldowns
- **Example:** Basic damage rotation

##### 2. Priority System
```
If Ability A available, use Ability A
Else if Ability B available, use Ability B
Else if Ability C available, use Ability C
Else use Basic Attack
```
- **Use Case:** Classes with proc-based mechanics
- **Example:** Priority-based damage dealing

#### Implementation Pattern (Lua-style from SMM++ Mud Client)
```lua
function combatLoop(enemy)
  while isEnemyAlive(enemy) do
    -- Resource management
    if player.hp < 50 then
      usePotion("health")
    end
    if player.mp < 20 then
      usePotion("mana")
    end

    -- Priority-based ability selection
    if Fireball:canCast() then
      Fireball:cast(enemy)
    else
      sendToMud("attack " .. enemy)
    end

    -- Recursive execution with delay
    delay(1000, combatLoop, enemy)
    break
  end
end
```
- **Source:** [SMM++ Mud Client Tutorial](https://blog.csdn.net/weixin_42527665/article/details/153837498)

#### Commercial Rotation Bot: Rice Rotations
- **Game:** World of Warcraft
- **Coverage:** All classes and specializations
- **Features:**
  - Magicbind system for simplified configuration
  - Real-time adaptive strategies
  - DPS, tanking, and healing support
  - Pixel-perfect execution precision
- **Website:** [Rice Rotations](https://www.ricerotations.com/)

#### WRobot Fight Classes
- **Game:** World of Warcraft The Burning Crusade
- **Features:**
  - Dynamic rotations based on nearby players
  - Humanization techniques
  - Frame-locking for precise timing
- **Forum:** [WRobot Fight Classes](https://wrobot.eu/files/category/133-fight-classes-tbc/)

#### Python Automation Case Study: Wakfu Bot
- **Source:** [Medium Article](https://medium.com/@martin.lees/how-i-made-a-python-bot-to-automate-a-tactical-mmorpg-9f6693350d10)
- **Challenges:**
  - 30-second turn limit
  - Anti-bot CAPTCHA challenges
- **Solutions:**
  - Grid-based movement
  - Spell range calculations
  - Turn optimization algorithms

### 2.4 Quest Following Automation

#### Pattern Components
1. **Quest Parsing:**
   - Extract quest objectives from UI or journal
   - Identify required locations, NPCs, items

2. **Navigation:**
   - Pathfinding to quest areas
   - Map integration for waypoint following
   - Transportation usage (flight paths, teleports)

3. **Objective Execution:**
   - Kill credit tracking
   - Item collection
   - NPC interaction sequences

4. **Quest Turn-In:**
   - Return to quest giver
   - Automated dialogue selection
   - Reward claiming

#### Technical Challenges
- **Quest State Tracking:** Multi-step objectives
- **Conditional Branching:** Quests with different paths
- **Group Quests:** Coordinating multiple characters
- **Phasing:** Zone changes based on quest progress

### 2.5 Anti-AFK Systems

#### Purpose
Prevent automatic disconnection from inactivity during passive activities (fishing, camping spawns, etc.).

#### Common Techniques
1. **Periodic Input:**
   - Jump every 5 minutes
   - Rotate camera view
   - Toggle UI elements

2. **Chat Monitoring:**
   - Auto-reply to tells
   - Guild chat response
   - Random emotes

3. **Activity Simulation:**
   - Movement in place
   - Inventory shuffling
   - Skill casting (if applicable)

#### Implementation Example
```python
def anti_afk_loop():
    while True:
        actions = [
            lambda: press_key(KEY_SPACE),      # Jump
            lambda: rotate_camera(45),          # Rotate view
            lambda: press_key(KEY_SHIFT),       # Toggle run
            lambda: cast_spell("Poke")          # Minor action
        ]

        # Random action every 3-5 minutes
        random.choice(actions)()
        sleep(random.randint(180, 300))
```

---

## 3. Core Automation Techniques

### 3.1 Pixel Detection for Triggers

#### Overview
Pixel detection involves reading screen pixels to identify game states, enemies, UI elements, or resource nodes without memory access.

#### Techniques

##### 1. Color Matching
- **Method:** Search for specific RGB/HSV color values
- **Use Case:** Health bar detection, enemy highlighting, resource node identification
- **Advantage:** Simple, fast
- **Limitation:** Sensitive to lighting changes, UI themes

##### 2. Pattern Matching
- **Method:** Template matching using OpenCV
- **Use Case:** Icon detection, UI element recognition
- **Advantage:** More robust than color alone
- **Limitation:** Requires template images, scale-sensitive

##### 3. OCR (Optical Character Recognition)
- **Method:** Extract text from game UI
- **Use Case:** Quest objectives, damage numbers, item names
- **Tools:** Tesseract, EasyOCR
- **Advantage:** Direct information extraction
- **Limitation:** Computationally expensive

#### PixelBot Technique
- **Definition:** Automation relying solely on screen analysis
- **Advantage:** No memory access required, harder to detect
- **Disadvantage:** Slower than memory reading
- **Source:** [Python Game Automation Tutorial](https://m.blog.csdn.net/qq_38783257/article/details/118714730)

### 3.2 Memory Reading for Game State

#### Overview
Memory reading directly accesses game memory to extract information not visible on screen.

#### Techniques

##### 1. Pointer Chains
- **Method:** Follow pointers from known base address to target data
- **Challenge:** ASLR (Address Space Layout Randomization)
- **Solution:** Pattern scanning to find base address each session

##### 2. Pattern Scanning
- **Method:** Search for byte patterns unique to target data
- **Advantage:** Resilient to memory address changes
- **Tools:** Cheat Engine, custom scanners

##### 3. Hooking
- **Method:** Intercept game functions to read/modify data
- **Risk:** Easily detected by anti-cheat
- **Use Case:** Advanced automation with game integration

#### Data Extraction Examples
- Player coordinates and facing direction
- Entity lists (enemies, NPCs, objects)
- Resource values (health, mana, energy)
- Cooldown states
- Inventory contents

#### Security Considerations
- **Detection Risk:** Memory access is easily monitored
- **Anti-Cheat:** EAC, BattlEye detect illegal memory access
- **Encryption:** Some games encrypt memory values
- **Source:** [Game Hacking Techniques](https://m.blog.csdn.net/ujm567890)

### 3.3 Input Injection Patterns

#### Windows APIs

##### 1. SendInput API
- **Method:** Standard Windows input simulation
- **Advantage:** Official API, wide compatibility
- **Detection:** Easily flagged as non-human input
```cpp
// Example: Send mouse click
INPUT input = {0};
input.type = INPUT_MOUSE;
input.mi.dx = x;
input.mi.dy = y;
input.mi.dwFlags = MOUSEEVENTF_LEFTDOWN;
SendInput(1, &input, sizeof(INPUT));
```

##### 2. keybd_event / mouse_event
- **Method:** Legacy APIs for input simulation
- **Status:** Deprecated but still functional
- **Use Case:** Simple keystrokes and clicks

#### Advanced Methods

##### 1. Kernel-Level Drivers
- **Method:** Create virtual HID devices
- **Advantage:** Appears as hardware input
- **Disadvantage:** Requires driver development

##### 2. Direct Function Calls
- **Method:** Hook into game's internal input handling
- **Advantage:** Bypasses input layer
- **Risk:** High detection risk

##### 3. Hardware Macros
- **Method:** Use mouse/keyboard macro features
- **Advantage:** Appears as legitimate hardware input
- **Limitation:** Limited logic capabilities

#### Detection Analysis
- **Raw Input Analysis:** Detects simulated vs. real hardware
- **Timing Patterns:** Inhuman consistency in input intervals
- **Source:** [Input Simulation Cheating Analysis](https://blog.csdn.net/qq_33060405/article/details/148358072)

### 3.4 Timing-Based Behaviors

#### Response Time Distributions

##### Human Reaction Time Characteristics
- **Distribution:** Log-Normal or Weibull (right-skewed)
- **Not Uniform:** `Sleep(rand() % 50)` is easily detected
- **Typical Range:** 200-400ms for visual stimuli
- **Outliers:** Occasional longer delays (attention drift)

##### Implementation Pattern
```python
import random
import numpy as np

# Realistic human reaction time
def human_delay(mean_ms=300, stddev_ms=50):
    # Gaussian distribution with jitter
    delay = random.gauss(mean_ms, stddev_ms)
    delay = max(100, min(delay, 1000))  # Clamp to reasonable range
    sleep(delay / 1000)
```

#### Cooldown Management

##### 1. Global Cooldown (GCD) Tracking
- Measure actual GCD from game
- Account for haste/l haste effects
- Queue next action just before GCD ends

##### 2. Ability-Specific Cooldowns
- Track each ability independently
- Handle cooldown reduction effects
- Predict availability windows

##### 3. Cast Bars
- Read cast progress from UI
- Time next action for cast completion
- Handle interrupt/cancel scenarios

#### Action Sequencing
```python
class ActionQueue:
    def __init__(self):
        self.queue = []
        self.next_action_time = 0

    def enqueue(self, action, delay):
        execute_time = max(time.time(), self.next_action_time) + delay
        self.queue.append((execute_time, action))
        self.queue.sort(key=lambda x: x[0])

    def execute_ready_actions(self):
        now = time.time()
        while self.queue and self.queue[0][0] <= now:
            _, action = self.queue.pop(0)
            action()
```

### 3.5 Randomization to Avoid Detection

#### Goals of Randomization
1. Break repetitive patterns
2. Simulate human inconsistency
3. Avoid statistical detection

#### Randomization Targets

##### 1. Timing Randomization
```python
# Bad: Uniform distribution
sleep(random.uniform(1.0, 2.0))

# Good: Gaussian distribution
sleep(random.gauss(1.5, 0.2))
```

##### 2. Coordinate Randomization
```python
def click_at(target_x, target_y):
    # Add ±2-3 pixel offset to simulate hand tremor
    offset_x = random.gauss(0, 2)
    offset_y = random.gauss(0, 2)
    mouse_click(target_x + offset_x, target_y + offset_y)
```

##### 3. Action Order Variation
```python
# Intentionally deviate from optimal order
def vary_rotation(base_rotation):
    if random.random() < 0.1:  # 10% deviation
        swap_adjacent(base_rotation)
    return base_rotation
```

##### 4. "Mistake" Injection
```python
def intentional_mistake():
    if random.random() < 0.02:  # 2% error rate
        # Click slightly off-target
        # Press wrong key briefly
        # Use suboptimal ability
        pass
```

#### Advanced Techniques
- **Bio-rhythm Models:** Time-of-day performance variation
- **Age-based Models:** Different reaction profiles
- **Fatigue Simulation:** Degraded performance over time
- **Multi-dimensional Fingerprinting:** Combine multiple behavior vectors

---

## 4. Macro Recording Systems

### 4.1 Recording and Replay

#### Software Overview

| Software | Type | Size | Best For |
|----------|------|------|----------|
| **TinyTask** | Free Ultra-lightweight | 33-36KB | Simple tasks, beginners |
| **AutoHotkey** | Free Open-source | ~10MB | Complex automation, scripting |
| **Pulover's Macro Creator** | Free Visual | ~5MB | Visual interface + power |
| **Macro Recorder** | Freemium | Variable | Enterprise, AI features |
| **Mouse Recorder Premium** | Paid | Variable | Professional testing |
| **KeymouseGo** | Free Open-source | Variable | Cross-platform needs |

#### TinyTask
- **Website:** https://www.tinytask.net/
- **Features:**
  - Ultra-lightweight (36KB)
  - 100% portable, no installation
  - Simple record & playback
- **Limitations:**
  - Windows only
  - No script editing
  - Limited features

#### AutoHotkey
- **Features:**
  - Script-based automation
  - Hotkey binding
  - GUI creation
  - Variables, loops, conditionals
- **Best For:** Complex automation
- **Learning Curve:** Steep but powerful

#### Pulover's Macro Creator
- **Built On:** AutoHotkey
- **Features:**
  - Visual interface
  - Record & edit graphically
  - Logic branches
  - Conditions, loops, variables
- **Best For:** Visual + powerful automation

### 4.2 Adding Conditions to Macros

#### Conditional Execution

##### 1. State-Based Conditions
```autohotkey
; AutoHotkey example
if (PixelGetColor(x, y) == 0xFF0000)
{
    ; If pixel is red, execute action
    Click, x, y
}
```

##### 2. Time-Based Conditions
```autohotkey
; Execute only during specific hours
if (A_Hour >= 9 && A_Hour <= 17)
{
    RunMacro()
}
```

##### 3. Resource-Based Conditions
```autohotkey
; Check if health is low (pixel-based)
if (HealthPercent() < 30)
{
    UsePotion()
}
```

#### Automa Browser Plugin
- **Variable Types:** Global key-value pairs, data columns, loops
- **Conditional Statements:** IF/ELSE logic
- **Loop Components:** "Repeat task" and "Loop Data"
- **Loop Conditions:** Numbers, custom data, Google Sheets
- **Source:** [Automa Tutorial](https://blog.csdn.net/this_is_mangog/article/details/122374350)

#### Power Automate
- **Variables:** Store and update data
- **Data Operations:** Filter array, Select, Join
- **Avoid Unnecessary Loops:** Optimize data processing
- **Source:** [Microsoft Learn](https://learn.microsoft.com/en-us/power-automate/guidance/coding-guidelines/use-data-operations)

### 4.3 Loop Structures in Macros

#### Loop Types

##### 1. Counted Loops
```autohotkey
; Repeat exactly N times
Loop, 10
{
    PerformAction()
}
```

##### 2. Conditional Loops
```autohotkey
; While condition is true
While (PixelGetColor(x, y) == 0x00FF00)
{
    GatherResource()
    Sleep, 1000
}
```

##### 3. Iterative Loops
```autohotkey
; Iterate through data
Loop, Parse, ItemList, `,
{
    ProcessItem(A_LoopField)
}
```

##### 4. Nested Loops
```autohotkey
; Wave-based spawning pattern
Loop, 5  ; 5 waves
{
    Loop, 10  ; 10 units per wave
    {
        SpawnUnit()
    }
    Sleep, WaveDelay
}
```
- **Source:** [Wave Spawning Tutorial](https://m.taptap.cn/moment/398973098587587099)

#### MakeCode Arcade Loops
- **Types:** For, While, Repeat, for-of
- **Variables:** Name, type, value
- **Educational Focus:** Teaching programming concepts
- **Source:** [Microsoft Learn](https://learn.microsoft.com/zh-cn/training/modules/support-student-with-makecode-arcade/use-makecode-arcade-teach-cs-concepts/)

### 4.4 Variable Insertion Points

#### Variable Types

##### 1. Local Variables
- **Scope:** Within macro execution
- **Use Case:** Temporary values, counters
```autohotkey
LocalCount := 0
Loop, 10
{
    LocalCount++
}
```

##### 2. Global Variables
- **Scope:** Across macro executions
- **Use Case:** Persistent state, configuration
```autohotkey
Global MaxRetries := 3
Global ResourceTarget := 1000
```

##### 3. Data Variables
- **Scope:** From external sources
- **Sources:** Files, databases, user input
- **Use Case:** Dynamic configuration

#### Variable Insertion Pattern
```
Macro Template:
1. Move to {X}, {Y}
2. Click
3. Wait {DELAY} ms
4. Repeat {COUNT} times

Runtime Substitution:
{X} = 500
{Y} = 300
{DELAY} = 1000
{COUNT} = 5
```

#### Advanced: Dynamic Variable Binding
```autohotkey
; Read variables from file
IniRead, TargetX, config.ini, Settings, TargetX
IniRead, TargetY, config.ini, Settings, TargetY

; Use in macro
MouseMove, TargetX, TargetY
```

---

## 5. Humanization and Anti-Detection

### 5.1 Mouse Movement Simulation

#### Why It Matters
- Straight lines at constant speed are unnatural
- Anti-bot systems detect non-human movement patterns
- Realistic movement is crucial for evasion

#### Bezier Curve Interpolation

##### Mathematical Foundation
- **Control Points:** Start, end, and intermediate points
- **Curve Type:** Cubic Bezier (most common)
- **Result:** Smooth, natural path

##### Quadratic Bezier Formula
```
B(t) = (1-t)²P₀ + 2(1-t)tP₁ + t²P₂
where t ∈ [0,1], P₀ is start, P₁ is control, P₂ is end
```

##### Cubic Bezier Formula
```
B(t) = (1-t)³P₀ + 3(1-t)²tP₁ + 3(1-t)t²P₂ + t³P₃
where t ∈ [0,1], P₀, P₁, P₂, P₃ are control points
```

#### Libraries and Tools

##### 1. pyclick (Python)
- **Repository:** [pyclick on GitHub](https://github.com/patrikoss/pyclick)
- **Features:**
  - Bezier curve-based movement
  - Customizable parameters
  - Shivering/distortion simulation
  - Acceleration/deceleration control
```python
from pyclick import HumanClicker

clicker = HumanClicker()
clicker.move((start_x, start_y), (end_x, end_y))
```

##### 2. human-mouse (Python)
- **PyPI:** [human-mouse 0.1.1](https://pypi.org/project/human-mouse/0.1.1/)
- **Features:**
  - Ultra-realistic movement
  - Spline-based paths
  - Natural acceleration
  - Cross-platform (Windows, macOS, Linux)

##### 3. ghost-cursor (Playwright/Puppeteer)
- **Repository:** [ghost-cursor-play](https://github.com/bn-l/ghost-cursor-play)
- **Features:**
  - Browser automation
  - Fitts's Law implementation
  - Multiple control points
  - Realistic timing

#### Key Characteristics of Human Movement

##### 1. Non-linear Paths
- Bezier curves, not straight lines
- Subtle curves even for direct paths

##### 2. Variable Speed
- "Fast-slow-fast" pattern
- Acceleration at start
- Deceleration near target
- Occasional speed fluctuations

##### 3. Natural Delays
- 100-300ms pause before clicks
- Variable delay based on distance

##### 4. Micro-adjustments
- Small corrections near target
- Overshoot and correction
- Final positioning tweaks

##### 5. Click Jitter
- ±1-2 pixel movement during click
- Simulates muscle tremors

#### Implementation Example
```python
import numpy as np
from scipy.interpolate import make_interp_spline

def generate_bezier_path(start, end, control_points, num_points=100):
    """Generate smooth Bezier curve path"""
    t = np.linspace(0, 1, num_points)

    # Cubic Bezier with two control points
    path = []
    for i in range(num_points):
        ti = t[i]
        x = (1-ti)**3 * start[0] + \
            3*(1-ti)**2 * ti * control_points[0][0] + \
            3*(1-ti) * ti**2 * control_points[1][0] + \
            ti**3 * end[0]
        y = (1-ti)**3 * start[1] + \
            3*(1-ti)**2 * ti * control_points[0][1] + \
            3*(1-ti) * ti**2 * control_points[1][1] + \
            ti**3 * end[1]
        path.append((x, y))

    return path

def move_with_acceleration(path, duration=1.0):
    """Move along path with realistic acceleration"""
    # Ease-in-out curve
    t_values = np.linspace(0, 1, len(path))
    eased_t = []
    for t in t_values:
        if t < 0.5:
            # Ease in (accelerate)
            eased_t.append(2 * t * t)
        else:
            # Ease out (decelerate)
            eased_t.append(-1 + (4 - 2 * t) * t)

    for i, point in enumerate(path):
        # Apply easing to timing
        delay = eased_t[i] * duration / len(path)
        mouse_move(point)
        sleep(delay)
```

#### Resources
- **CSDN Tutorial:** [Mouse Trajectory Algorithms](https://m.blog.csdn.net/m0_72310110/article/details/149246267)
- **Research:** [Curve Smoothing Study](https://blog.csdn.net/eiilpux17/article/details/126062661)

### 5.2 Fitts's Law Application

#### What is Fitts's Law?
A model of human movement predicting time required to move to a target area.

#### Formula
```
T = a + b * log₂(2D / W)
where:
  T = Time to complete movement
  a, b = Empirical constants
  D = Distance to target
  W = Width of target
```

#### Implications for Automation
1. **Larger targets:** Faster approach and acquisition
2. **Smaller targets:** More careful, slower approach
3. **Distance:** Longer distance = more time
4. **Not linear:** Time scales logarithmically

#### Implementation
```python
import math

def fitts_law_delay(start, end, target_size, a=0.1, b=0.1):
    """Calculate realistic delay based on Fitts's Law"""
    distance = math.sqrt((end[0] - start[0])**2 + (end[1] - start[1])**2)
    time_ms = (a + b * math.log2(2 * distance / target_size)) * 1000
    return max(100, min(time_ms, 2000))  # Clamp to reasonable range
```

### 5.3 Behavioral Patterns

#### Detection Signatures

##### 1. Mechanical Repetition
- **Pattern:** Identical click intervals
- **Detection:** Statistical analysis reveals inhuman consistency
- **Solution:** Add variance to all timings

##### 2. Pixel-Perfect Precision
- **Pattern:** Clicking exact same coordinates repeatedly
- **Detection:** Unrealistic for human hand tremor
- **Solution:** Add ±2-3 pixel random offset

##### 3. Perfect Rhythm
- **Pattern:** Keyboard inputs at exact intervals
- **Detection:** Humans have natural timing variability
- **Solution:** Gaussian distribution for delays

##### 4. Linear Movement
- **Pattern:** Straight-line mouse paths
- **Detection:** Humans naturally curve movements
- **Solution:** Bezier curve interpolation

#### Humanization Techniques

##### 1. Fatigue Simulation
```python
class FatigueModel:
    def __init__(self):
        self.session_start = time.time()
        self.actions_performed = 0

    def get_reaction_time(self):
        """Get reaction time based on fatigue"""
        elapsed = time.time() - self.session_start

        # Base reaction time increases with session duration
        base_time = 300  # 300ms
        fatigue_factor = min(elapsed / 3600, 1.0)  # Max fatigue after 1 hour
        added_delay = fatigue_factor * 200  # Add up to 200ms

        return random.gauss(base_time + added_delay, 50)
```

##### 2. Break Taking
```python
def should_take_break():
    """Randomly take breaks to simulate human behavior"""
    # 5% chance every 100 actions
    if random.random() < 0.05:
        break_duration = random.gauss(60, 30)  # 60s ± 30s
        break_duration = max(30, min(break_duration, 300))  # Clamp 30-300s
        return True, break_duration
    return False, 0
```

##### 3. Context-Aware Behavior
```python
def adjust_behavior_for_context(context):
    """Adjust behavior based on game context"""
    if context['in_combat']:
        # Faster reactions, more focused
        reaction_mean = 200
        reaction_stddev = 30
    elif context['exploring']:
        # More relaxed, variable attention
        reaction_mean = 400
        reaction_stddev = 100
    else:
        # Normal behavior
        reaction_mean = 300
        reaction_stddev = 50

    return random.gauss(reaction_mean, reaction_stddev)
```

### 5.4 Anti-Cheat Evasion

#### Detection Methods

##### 1. Raw Input Analysis
- **What:** Analyze input at hardware level
- **Detects:** Simulated input from APIs
- **Evasion:** Kernel-level drivers, hardware macros

##### 2. Behavioral Analysis
- **What:** Statistical analysis of actions
- **Detects:** Inhuman patterns
- **Evasion:** Randomization, humanization

##### 3. Process/Driver Scanning
- **What:** Scan for known cheat software
- **Detects:** Blacklisted processes
- **Evasion:** Private builds, obfuscation

##### 4. Heuristic Detection
- **What:** Machine learning models
- **Detects:** Suspicious behavior patterns
- **Evasion:** Training data poisoning, gradual behavior drift

#### Advanced Evasion Techniques

##### 1. Virtual Machine Introspection (VMI)
- **Paper:** [VIC: Evasive Video Game Cheating](http://arxiv.org/html/2502.12322v2)
- **Method:** Run automation in separate VM
- **Advantage:** Isolated from host anti-cheat
- **Complexity:** High setup cost

##### 2. Machine Learning Against ML
- **Method:** Train models on human data
- **Use:** Generate realistic behavior
- **Challenge:** Requires large datasets

##### 3. Gradual Adaptation
- **Method:** Slowly drift behavior over time
- **Purpose:** Avoid sudden pattern changes
- **Implementation:** Moving average of behavioral parameters

#### Resources
- **Academic Paper:** [VIC - VM-based Cheating](http://arxiv.org/html/2502.12322v2)
- **CSDN Guide:** [Game Hacking Techniques](https://m.blog.csdn.net/ujm567890)
- **CSDN Analysis:** [Input Simulation Cheating](https://blog.csdn.net/qq_33060405/article/details/148358072)

---

## 6. Architectural Patterns

### 6.1 State Machine Design

#### Agent State Machine (Steve AI Pattern)
From the Steve AI project (Cursor for Minecraft):

```
States: IDLE, PLANNING, EXECUTING, WAITING, ERROR

Transitions:
IDLE → PLANNING: User command received
PLANNING → EXECUTING: Plan generated successfully
PLANNING → ERROR: Plan generation failed
EXECUTING → WAITING: Waiting for condition (e.g., resource respawn)
WAITING → EXECUTING: Condition met
EXECUTING → IDLE: All tasks complete
EXECUTING → ERROR: Action failed unrecoverably
ERROR → IDLE: Error handled
```

#### Benefits
1. **Clear State Tracking:** Always know what agent is doing
2. **Debugging:** Easy to log state transitions
3. **Error Recovery:** Explicit error handling paths
4. **Pause/Resume:** Can pause at any state

### 6.2 Task Queue Management

#### Priority-Based Queue
```python
import heapq

class TaskQueue:
    def __init__(self):
        self.queue = []

    def enqueue(self, task, priority):
        heapq.heappush(self.queue, (priority, time.time(), task))

    def dequeue(self):
        if self.queue:
            _, _, task = heapq.heappop(self.queue)
            return task
        return None

    def peek(self):
        if self.queue:
            return self.queue[0][2]
        return None
```

#### Work Queue Pattern (from RTS)
- **Free Resource Queue:** Available workers/units
- **Busy Resource Map:** Currently allocated resources
- **Task Assignment:** Pull from task queue, assign to free resource
- **Source:** [Resource Management Pattern](https://www.360doc.cn/article/4238731_66859700.html)

### 6.3 Event-Driven Architecture

#### Event Bus Pattern
```python
class EventBus:
    def __init__(self):
        self.listeners = {}

    def subscribe(self, event_type, callback):
        if event_type not in self.listeners:
            self.listeners[event_type] = []
        self.listeners[event_type].append(callback)

    def publish(self, event_type, data):
        if event_type in self.listeners:
            for callback in self.listeners[event_type]:
                callback(data)
```

#### Common Events
- `resource_collected`
- `enemy_sighted`
- `health_low`
- `quest_completed`
- `combat_started`
- `combat_ended`

### 6.4 Plugin Architecture

#### Action Registry Pattern (from Steve AI)
```java
public interface ActionFactory {
    Action create(Steve steve, Task task, ActionContext context);
}

public class ActionRegistry {
    private Map<String, ActionFactory> factories = new HashMap<>();

    public void register(String name, ActionFactory factory) {
        factories.put(name, factory);
    }

    public Action create(String name, Steve steve, Task task, ActionContext context) {
        ActionFactory factory = factories.get(name);
        return factory.create(steve, task, context);
    }
}
```

#### Benefits
1. **Extensibility:** Add actions without modifying core
2. **Modularity:** Actions are independent
3. **Discovery:** SPI for plugin loading
4. **Testing:** Test actions in isolation

### 6.5 Async Execution with Tick-Based Processing

#### Non-Blocking Pattern (from Steve AI)
```java
public class TaskPlanner {
    public CompletableFuture<List<Task>> planTasksAsync(String command) {
        return CompletableFuture.supplyAsync(() -> {
            // LLM call (blocking)
            return llmClient.generatePlan(command);
        });
    }
}

public class ActionExecutor {
    private CompletableFuture<List<Task>> pendingPlan;

    public void tick() {
        // Check if async operation is complete
        if (pendingPlan != null && pendingPlan.isDone()) {
            List<Task> tasks = pendingPlan.join();
            executeTasks(tasks);
            pendingPlan = null;
        }

        // Execute current actions
        for (Action action : activeActions) {
            action.tick();
        }
    }
}
```

#### Benefits
1. **No Server Freezing:** LLM calls don't block game thread
2. **Responsive:** Game continues while planning
3. **Scalable:** Can have multiple pending operations
4. **Cancellation:** Can cancel pending futures

---

## 7. Lessons for AI Agent Development

### 7.1 Applicability to Autonomous Agents

#### From RTS to AI Agents
1. **Build Orders → Task Planning**
   - Pre-computed action sequences
   - Dependency management
   - Resource timing optimization

2. **Production Queues → Action Pipelines**
   - Multi-step task execution
   - Resource allocation
   - Priority management

3. **Auto-Scouting → Environment Exploration**
   - Systematic area coverage
   - Information gathering
   - Adaptive pathing

#### From MMO Bots to AI Agents
1. **Combat Rotations → Skill Sequencing**
   - Optimal action ordering
   - Cooldown management
   - Priority-based execution

2. **Resource Routes → Efficient Pathing**
   - Node-to-node optimization
   - Respawn awareness
   - Dynamic re-routing

3. **Anti-Detection → Natural Behavior**
   - Human-like movement
   - Realistic timing
   - Behavioral variation

### 7.2 Recommended Patterns for Implementation

#### 1. Hierarchical Planning
```
High-Level: Build house
  ↓
Mid-Level: Gather materials, clear site, place blocks
  ↓
Low-Level: Move to tree, mine wood, return to base
```

#### 2. State-Driven Behavior
```
States: IDLE, MOVING, GATHERING, BUILDING, WAITING
State transitions based on: Environment, Goals, Resources
```

#### 3. Event-Based Reactivity
```
Events: Enemy detected, Resource depleted, Obstacle encountered
Reactions: Pause, Re-plan, Retry, Abort
```

#### 4. Async LLM Integration
```
User Command → Async Plan Generation → Tick-Based Execution
    ↓            ↓                        ↓
  Immediate   Non-blocking            Continuous
  Response    LLM call                Progress
```

#### 5. Action Registry Pattern
```
Plugin System
  ├── Core Actions (built-in)
  ├── Custom Actions (user-defined)
  └── Action Factories (creation logic)
```

### 7.3 Anti-Patterns to Avoid

#### 1. Blocking LLM Calls on Game Thread
- **Problem:** Freezes game during planning
- **Solution:** Async planning with CompletableFuture

#### 2. Hardcoded Sequences
- **Problem:** Inflexible, breaks on changes
- **Solution:** Dynamic planning with LLM

#### 3. No State Tracking
- **Problem:** Can't recover from errors
- **Solution:** State machine with error recovery

#### 4. Perfect Precision
- **Problem:** Appears robotic
- **Solution:** Randomization and humanization

#### 5. No Feedback Loop
- **Problem:** Can't adapt to failures
- **Solution:** Continuous re-planning based on results

### 7.4 Testing and Validation

#### Unit Testing
- Test individual actions in isolation
- Mock game state and LLM responses
- Verify state transitions

#### Integration Testing
- Test action sequences
- Verify resource management
- Check error recovery

#### Behavioral Testing
- Verify human-like movement
- Check timing distributions
- Validate natural action patterns

#### Performance Testing
- Measure tick time impact
- Check memory usage
- Verify no thread blocking

### 7.5 Future Research Directions

#### 1. Imitation Learning
- Train on human gameplay replays
- Learn natural action sequences
- Discover optimal strategies

#### 2. Reinforcement Learning
- Learn from trial and error
- Optimize for specific goals
- Adapt to changing environments

#### 3. Multi-Agent Coordination
- Collaborative task execution
- Distributed planning
- Shared resource management

#### 4. Hierarchical RL
- High-level policy selection
- Low-level action execution
- Multiple time scales

#### 5. Language Model Integration
- Natural language task specification
- Context-aware planning
- Explainable decision-making

---

## 8. Conclusion

### Key Takeaways

1. **Game Automation is Rich Source of Patterns:** Decades of game automation have produced sophisticated techniques for autonomous behavior.

2. **Humanization is Critical:** Anti-cheat evolution drives increasingly realistic behavior simulation, which benefits legitimate AI agents.

3. **State Machines and Event Systems:** Proven architectures for managing complex, multi-step behaviors.

4. **Async Processing is Essential:** Long-running operations (LLM calls) must not block real-time systems.

5. **Plugin Architecture Enables Extensibility:** Action registry pattern allows adding capabilities without modifying core.

### Ethical Considerations

**Important:** Many techniques described in this document originate from game botting, which typically violates Terms of Service. This research is intended for:

- **Educational purposes:** Understanding automation patterns
- **Legitimate AI development:** Building autonomous agents for authorized applications
- **Game AI research:** Creating intelligent game opponents

**Not for:** Cheating in online games, violating ToS, unfair advantages in competitive play.

### Final Thoughts

The evolution of game automation from simple macros to sophisticated AI systems provides valuable lessons for building autonomous agents. By studying these patterns, we can create more natural, efficient, and capable AI systems while avoiding common pitfalls.

The future of autonomous agents lies in combining:
- **Planning:** LLM-powered high-level reasoning
- **Execution:** Tick-based low-level actions
- **Adaptation:** Event-driven reactivity
- **Naturalness:** Human-like behavior patterns

This synthesis of game automation wisdom with modern AI techniques promises agents that are both powerful and pleasant to interact with.

---

## Sources and References

### RTS Automation
- [UAlbertaBot on GitCode](https://gitcode.com/gh_mirrors/ua/ualbertabot)
- [CommandCenter GitHub](https://github.com/davechurchill/commandcenter)
- [CommandCenter Documentation (机PLUS)](https://www.jiqizhixin.com/articles/2017-08-11-2)
- [AoE4BO on GitHub](https://github.com/nordie92/AoE4BO)
- [Awesome AoE2 on GitHub](https://github.com/Arkanosis/awesome-aoe2)
- [Build Order Research (Zhangqiaokeyan)](https://m.zhangqiaokeyan.com/academic-conference-foreign_7th-world-multiconference-systemics-cybernetics-and-informatics_thesis_0705013049605.html)
- [Combat Imitation Learning (Zhangqiaokeyan)](https://m.zhangqiaokeyan.com/academic-conference-foreign_meeting-236192_thesis_020516251783.html)
- [RTS Automation Design (Baidu Baijiahao)](https://baijiahao.baidu.com/s?id=1665631241958600529)
- [Resource Allocation Patterns (360doc)](https://www.360doc.cn/article/4238731_66859700.html)
- [Resource Gathering Research (Zhangqiaokeyan)](https://m.zhangqiaokeyan.com/academic-journal-foreign_detail_thesis_0204119274449.html)
- [Auto Scout Wiki (Age of Empires Fandom)](https://ageofempires.fandom.com/wiki/Auto_Scout)
- [RTS Vision Strategy (TapTap)](https://m.taptap.cn/moment/290596478705994086)
- [ELF Research Platform (Gitee)](https://gitee.com/xsongx/ELF)
- [Unit Micromanagement Research (Zhangqiaokeyan)](https://m.zhangqiaokeyan.com/academic-conference-foreign_meeting-221589_thesis_0705016204108.html)

### MMO Bot Patterns
- [MANIA MapleStory Bot (GitHub)](https://github.com/Lyze96/MANIA-Maplestory-Bot)
- [Genshin Auto-Mining Guide (Sohu)](https://www.sohu.com/a/926146289_121979110)
- [What is a Combat Rotation (Chimpeon)](https://chimpeon.com/games/what-is-a-combat-rotation)
- [Rice Rotations](https://www.ricerotations.com/)
- [WRobot Fight Classes](https://wrobot.eu/files/category/133-fight-classes-tbc/)
- [How I Made a Python Bot (Medium)](https://medium.com/@martin.lees/how-i-made-a-python-bot-to-automate-a-tactical-mmorpg-9f6693350d10)
- [SMM++ Mud Client Tutorial (CSDN)](https://blog.csdn.net/weixin_42527665/article/details/153837498)

### Core Automation Techniques
- [Python Game Automation Tutorial (CSDN)](https://m.blog.csdn.net/qq_38783257/article/details/118714730)
- [Game Hacking Techniques (CSDN)](https://m.blog.csdn.net/ujm567890)
- [Input Simulation Cheating Analysis (CSDN)](https://blog.csdn.net/qq_33060405/article/details/148358072)
- [VIC: VM-based Cheating (arXiv)](http://arxiv.org/html/2502.12322v2)
- [Forensic Framework for Speedruns (arXiv)](https://arxiv.org/html/2509.10848v1)

### Macro Recording
- [TinyTask](https://www.tinytask.net/)
- [Automa Tutorial (CSDN)](https://blog.csdn.net/this_is_mangog/article/details/122374350)
- [Power Automate Data Operations (Microsoft Learn)](https://learn.microsoft.com/en-us/power-automate/guidance/coding-guidelines/use-data-operations)
- [Wave Spawning Tutorial (TapTap)](https://m.taptap.cn/moment/398973098587587099)
- [MakeCode Arcade (Microsoft Learn)](https://learn.microsoft.com/zh-cn/training/modules/support-student-with-makecode-arcade/use-makecode-arcade-teach-cs-concepts/)
- [Game Automation Script (CSDN)](https://m.blog.csdn.net/2301_78150559/article/details/144513996)

### Mouse Movement and Humanization
- [pyclick (GitHub)](https://github.com/patrikoss/pyclick)
- [human-mouse (PyPI)](https://pypi.org/project/human-mouse/0.1.1/)
- [ghost-cursor-play (GitHub)](https://github.com/bn-l/ghost-cursor-play)
- [Mouse Trajectory Algorithms (CSDN)](https://m.blog.csdn.net/m0_72310110/article/details/149246267)
- [Curve Smoothing Study (CSDN)](https://blog.csdn.net/eiilpux17/article/details/126062661)

---

**Document Version:** 1.0
**Last Updated:** 2025-02-28
**Classification:** Research Documentation
