# PHANTOM OPERATIONS CREW MANUAL

**MineWright Construction Division - Training Manual #47**

---

## FOREMAN'S NOTE

Listen up, crew! You're about to learn the night shift. Phantom operations ain't for the faint of heart - these sky-biters come out when the honest folks are sleeping. But you know what? We turn that nuisance into profit. Membranes fix wings, slow-falling potions save lives, and XP? That's just bonus.

Your AI toolbox has the pattern-matching algorithms to track spawns, the predictive models to optimize killing, and the automated workflows to keep the operation running smooth. Study this manual, know your insomnia mechanics, and you'll be running the most efficient membrane collection operation this side of the End.

**- Foreman Steve, MineWright Construction Division**

---

## TABLE OF CONTENTS

1. [Phantom Spawning Mechanics](#1-phantom-spawning-mechanics)
2. [Combat Tactics](#2-combat-tactics)
3. [Phantom Membrane Operations](#3-phantom-membrane-operations)
4. [Slow-Falling Potion Production](#4-slow-falling-potion-production)
5. [Elytra Repair Workshop](#5-elytra-repair-workshop)
6. [Pharm Farm Management](#6-phantom-farm-management)
7. [Cat Defense Systems](#7-cat-defense-systems)
8. [Team Phantom Ops](#8-team-phantom-ops)
9. [Insomnia Management Protocols](#9-insomnia-management-protocols)
10. [Safety Regulations](#10-safety-regulations)

---

## 1. PHANTOM SPAWNING MECHANICS

### The Insomnia Pattern Recognition

Phantoms operate on a predictable algorithm - the insomnia mechanic. Your AI needs to recognize this pattern:

```
INSOMNIA TRACKING ALGORITHM:
┌─────────────────────────────────────────────────────────────┐
│ Day 0: Player sleeps → Insomnia = 0 → No spawns            │
│ Day 1: No sleep → Insomnia = 1 → 1 phantom/spawn           │
│ Day 2: No sleep → Insomnia = 2 → 1-2 phantoms/spawn        │
│ Day 3+: No sleep → Insomnia = 3 → 2-3 phantoms/spawn       │
└─────────────────────────────────────────────────────────────┘
```

### Spawn Conditions (Pre-flight Checklist)

Before running spawn detection algorithms, verify these parameters:

| Parameter | Required Value | Detection Method |
|-----------|----------------|------------------|
| **Dimension** | Overworld only | `dimension == Level.OVERWORLD` |
| **Time** | Night (13000-23000 ticks) | `dayTime % 24000 in [13000, 23000]` |
| **Insomnia** | Minimum 1 day | `timeSinceSleep >= 24000 ticks` |
| **Light Level** | < 7 at spawn point | `lightLevel < 7` |
| **Player Proximity** | Within 20 blocks | `distance <= 20` |
| **Spawn Interval** | Every 1-2 minutes | `random 1200-2400 ticks` |

### Spawn Quantity Formula

Your AI should calculate expected spawns:

```
SPAWN_COUNT = 1 + floor(INSOMNIA_DAYS / 2)

Maximum spawns per cycle: 3 phantoms (at 3+ days insomnia)
Spawn frequency: Every 60-120 seconds during night
```

### Environmental Factors

**Light Level Optimization:**
- Spawn surface needs light level < 7
- Use roof to control spawning zone
- Glass ceiling provides visibility + light control

**Altitude Considerations:**
- Phantoms spawn at any Y level
- Higher platforms = fewer competing mobs
- AFK platforms typically Y=200 for optimal spawn sphere

---

### CREW TALK #1

**Foreman:** Alright, new crew, listen close! We're setting up the phantom observation post.

**Rookie:** Foreman, how do we know when the sky-biters are coming?

**Foreman:** Good question, greenhorn! Your AI's got the `InsomniaDetector` module running. It tracks the player's sleep cycle like a project manager tracks deadlines. When the counter hits 24,000 ticks without sleep, we're in business. That's day one. Day two? Double the spawns. Day three? Maximum spawn rate. We're talking 2-3 phantoms every 60-90 seconds all night long.

**Rookie:** And if the player sleeps?

**Foreman:** Then you're taking an unscheduled break, rookie! Insomnia resets to zero, spawn rate drops to nothing. That's why we coordinate sleep schedules with the Operations Division. Player sleeps during day, we run the farm all night. It's called *workflow optimization*, kid. Learn it, live it, profit from it.

**Veteran:** *chuckles* Remember the old days before we had the AI predictive modules? Had to guess when phantoms were coming. Now we've got spawn probability algorithms running real-time. Precision operations, not guessing games.

**Foreman:** Exactly! Now get to the tower and run that diagnostic. I want spawn probability charts on my desk by dawn!

---

## 2. COMBAT TACTICS

### Phantom Behavior Analysis

Before engaging, understand the target's movement algorithm:

```
PHANTOM BEHAVIOR PATTERN:
┌─────────────────────────────────────────────────────────────┐
│ CIRCLING PHASE:                                             │
│ - Orbits player at radius 10-20 blocks                      │
│ - Gradual descent pattern                                   │
│ - Speed: Very fast (faster than walking)                    │
│                                                             │
│ ATTACK PHASE:                                               │
│ - Trigger: Player looks away                                │
│ - Swoop pattern: Rapid descent + bite                       │
│ - Attack range: Melee distance                              │
│ - Damage: Heart difficulty-dependent                        │
│                                                             │
│ BURN PHASE (Daytime):                                       │
│ - Direct sunlight → combustion                              │
│ - Can survive under roofs/near cover                        │
│ - No burning if `burning = false` tag applied              │
└─────────────────────────────────────────────────────────────┘
```

### Engagement Protocols

#### Stationary Combat (Recommended)

**Setup:**
1. Agent positions at designated station point
2. Flying mode enabled for altitude control
3. Invulnerability enabled (safety first)
4. Search radius: 32 blocks
5. Attack range: 6 blocks

**Combat Algorithm:**
```java
PHANTOM_COMBAT_ROUTINE:
    Maintain station point (within 3 blocks)
    Scan search radius for Phantom entities
    For each detected phantom:
        If distance <= ATTACK_RANGE:
            Apply lethal damage (instant kill preferred)
            Increment kill counter
            Update efficiency metrics
```

#### Mobile Combat (High-Skill)

**When to Use:** Open-area operations, multiple spawn zones

**Technique:**
- Intercept phantoms during descent phase
- Timing: Attack when phantom begins swoop
- Position: Between phantom and player
- Movement: Controlled bursts, not sustained

**Warning:** Higher risk of phantom escaping spawn sphere

### Weapon Recommendations

| Weapon Type | Effectiveness | Notes |
|-------------|---------------|-------|
| **Instant Kill** | 100% | Bypasses health, guaranteed kill |
| **Sharpness V Sword** | 90% | 2-3 hits required per phantom |
| **Power V Bow** | 85% | Range advantage, tracking needed |
| **Sweep Attack** | 60% | Can hit multiple phantoms |

### Kill Rate Optimization

**Target Metrics:**
- Single agent: 20-30 phantoms/hour
- Two agents (coordinated): 40-50 phantoms/hour
- Multi-zone setup (4+ agents): 80-120 phantoms/hour

**Throughput Calculation:**
```
Expected Kills/Hour = (Spawns/Minute × 60) × Agent Efficiency

Typical spawn rate: 2-3 spawns/minute
Agent efficiency: 70-90% (missed spawns, escapees)
Expected: 85-160 kills/hour at maximum insomnia
```

---

### CREW TALK #2

**Foreman:** Combat stations! We've got phantoms incoming from the northeast sector!

**Veteran:** Copy that, Foreman. I'm picking up three targets on approach vector. Lead agent engaging.

**Rookie:** Three? But the spawn algorithm says maximum two at this insomnia level!

**Foreman:** That's why you don't rely solely on predictions, rookie! The spawn table has variance built in. Always expect +1 phantom beyond your calculations. That's called *contingency planning*.

**Veteran:** Target 1 down. Target 2 approaching swoop phase. Target 3... still orbiting. Tactical assessment?

**Foreman:** Let Target 3 orbit. Prioritize the swooper. Rules of engagement: intercept descending phantoms first, orbiters second. The swoopers are the ones trying to take a bite out of operations.

**Rookie:** But Target 3 is closer!

**Foreman:** And it's circling, not attacking! That's threat assessment, kid. Closest isn't always most dangerous. You learn that or you learn to respawn. Your choice. Now get in position and wait for the swoop!

**Veteran:** Target 2 down. Target 3 beginning descent. Intercepting in 3... 2... 1... splat.

**Foreman:** Clean work. Reset positions and run the efficiency calculation. I want kill rates on the morning report.

---

## 3. PHANTOM MEMBRANE OPERATIONS

### The Membrane Resource

Phantom membranes are your primary product. Know your material:

```
MEMBRANE SPECIFICATIONS:
┌─────────────────────────────────────────────────────────────┐
│ Drop Rate: 0-2 per phantom (avg. 1.2)                       │
│ Stack Size: 64                                               │
│ Uses:                                                        │
│   - Potion of Slow Falling (brewing ingredient)             │
│   - Elytra repair (anvil combination)                       │
│   - Trading (cleric villagers, rare)                        │
│                                                             │
│ Value Rating: HIGH (limited farming methods)                │
└─────────────────────────────────────────────────────────────┘
```

### Collection Systems

#### Manual Collection (Basic)

**Setup:**
- Player stands on collection point
- Hoppers below kill zone
- Chests below hoppers
- Regular collection runs

**Pros:** Low setup cost, simple
**Cons:** Labor-intensive, slow

#### Automated Collection (Standard)

**Hardware Required:**
- Hopper network (1 per kill zone)
- Storage chests (double chest preferred)
- Item sorting system (optional)
- Redstone clock (for periodic collection checks)

**AI Automation:**
```
COLLECTION_ALGORITHM:
    Monitor kill zone for ItemEntity[PHANTOM_MEMBRANE]
    When detected:
        Navigate to collection point
        Activate collection mode (radius 8 blocks)
        Transfer to inventory
        Sort to designated storage
        Update inventory database
```

#### Multi-Agent Collection (Advanced)

**Workflow:**
1. **Killer Agents** - Station at kill zones, eliminate phantoms
2. **Collector Agents** - Patrol collection routes, gather drops
3. **Sorter Agents** - Manage inventory, organize storage
4. **Coordinator Agent** - Balance workload, optimize efficiency

**Efficiency Gain:** 3-4x manual collection rates

### Storage Solutions

**Recommended Layout:**
```
┌─────────────────────────────────────────────────────────────┐
│  STORAGE HIERARCHY:                                         │
│                                                             │
│  [Raw Collection Chests] → [Sorting Hopper] → [Storage]    │
│         ↓                                                           ↓
│  Temporary holding    Category-based storage              │
│  (awaiting sort)      (membranes, XP, other drops)        │
└─────────────────────────────────────────────────────────────┘
```

**Inventory Management:**
- Use shulker boxes for bulk transport
- Label chests clearly (signs/item frames)
- Keep buffer space for peak production
- Backup storage for surpluses

---

### CREW TALK #3

**Rookie:** Foreman! The collection chest is full again! We've lost three membranes to overflow!

**Foreman:** Lost inventory? That's coming out of your efficiency rating, rookie. You know the protocol - buffer space minimum 20% on all storage chests!

**Rookie:** But we're producing so much! I didn't expect this volume!

**Veteran:** *sighs* That's a good problem, kid, but still a problem. Let me show you the sorter configuration I built.

**Foreman:** Listen to the Veteran, rookie. He's got the SorterAction module tuned to perfection. Watch and learn.

**Veteran:** See this hopper network? It's got overflow protection built in. When the primary chest hits 80% capacity, the diversion gate triggers and routes excess to the backup storage. And look here - the weighted distribution algorithm sends membranes to the dedicated membrane chest, everything else to general storage.

**Rookie:** Weighted distribution?

**Veteran:** Pattern recognition, rookie. Most common items get the shortest path. Membranes are 90% of drops, so they get priority routing. Reduces hopper travel time, increases throughput. Basic logistics.

**Foreman:** Exactly. Now implement that overflow protection on all collection zones before the next night shift. And don't make me tell you twice - efficiency losses are unacceptable in Phantom Ops!

---

## 4. SLOW-FALLING POTION PRODUCTION

### The Brewing Workflow

Phantom membranes unlock slow-falling potions - essential for高空 operations.

#### Brewing Recipe

```
SLOW FALLING POTION CRAFTING TREE:

Awkward Potion
      ↓
+ Phantom Membrane
      ↓
Potion of Slow Falling (3:00)

      └─+ Redstone Dust → Potion of Slow Falling (8:00)

      └─+ Glowstone Dust → Potion of Slow Falling (1:30) [Potency II - NOT REAL]
                  (Note: Glowstone actually reduces duration to 1:30, no potency increase)
```

**Correction:** Glowstone dust does NOT increase slow-falling potency. It reduces duration. Redstone extends duration.

### Production Planning

**Batch Calculations:**
```
ONE BREWING CYCLE:
Input: 1 Nether Wart, 1 Phantom Membrane, 1 Water Bottle
Output: 1 Potion of Slow Falling (3:00)
Time: 20 seconds per cycle

BREWING STATION OUTPUT:
Single brewing stand: 3 potions/cycle (20 seconds) = 9 potions/minute
Multi-stand setup (3 stands): 27 potions/minute
Hourly production: 1,620 potions (3 stands running continuously)
```

### Resource Requirements

**Per 64 Potions (1 stack):**
- Phantom Membranes: 64
- Nether Warts: 64 (or 21 if brewing 3 at a time)
- Water Bottles: 64
- Glass Bottles: 64 (recoverable)
- Redstone (optional, for extended duration): 21-64

**Farming Requirements:**
- Nether Wart farm (automated recommended)
- Water supply (infinite source)
- Phantom membrane supply (this manual's topic!)
- Blaze Rods (for brewing stands)

### Automation Opportunities

**Automated Brewing Station Features:**
1. **Hopper Input** - Feed ingredients automatically
2. **Hopper Output** - Extract finished potions
3. **Bottle Filling** - Auto-refill water bottles
4. **Recipe Selection** - Redstone-activated brewing modes

**AI Integration:**
```
BREWING_AUTOMATION_ACTION:
    Monitor membrane inventory
    When threshold reached:
        Navigate to brewing station
        Load brewing stand with ingredients
        Wait for brewing cycle (20s)
        Extract potions
        Restock water bottles
        Repeat until resource depleted
```

---

### CREW TALK #4

**Foreman:** Alright crew, we're expanding operations. The客户 wants a slow-falling potion contract. 500 potions by Friday.

**Rookie:** 500 potions? That's... let me calculate... at 3 potions per cycle, 20 seconds per cycle... that's...

**Veteran:** About 55 minutes of continuous brewing time. But add ingredient loading, bottle refilling, and transport time? Call it 2 hours per brewing stand.

**Foreman:** Good math, Veteran. Now here's the question - how many membranes do we need?

**Rookie:** 500 membranes! One membrane per potion!

**Foreman:** *facepalm* Read the recipe again, rookie! We're brewing extended duration (8 minutes) for the premium contract. That's membrane + redstone. How many stands?

**Veteran:** Three stands running parallel. So that's 500 potions divided by 3 = 167 cycles, rounding up. 167 membranes, 21 redstone dust. Plus Nether wart for the awkward potions, but we've got surplus from the farm.

**Foreman:** Now you're thinking! But here's the kicker - efficiency bonus. If we deliver early, we get 15% extra pay. So optimize the workflow. How do we beat the two-hour estimate?

**Rookie:** Automate the ingredient loading! The `BrewingAutomationAction` can handle feeding hoppers while we're collecting membranes!

**Foreman:** NOW you're earning your pay! Set up the triple-stand array, load the hopper feeds, and run parallel operations. I want those potions ready by Thursday!

---

## 5. ELYTRA REPAIR WORKSHOP

### Elytra Durability Management

The elytra - every Foreman's dream transportation. But they wear out. Phantom membranes are the solution.

**Durability Specifications:**
```
ELYTRA DURABILITY:
┌─────────────────────────────────────────────────────────────┐
│ Maximum Durability: 432 uses                                │
│ Repair Amount: +108 durability per membrane                 │
│ Repair Cost: 1 membrane per repair                          │
│ Maximum Repairs: 4 membranes to fully repair broken elytra  │
│                                                             │
│ DAMAGE CALCULATION:                                         │
│ 1 second flight = 1 durability point (approximately)       │
│ 10 second flight = 10 durability                            │
│ Full elytra = ~7 minutes continuous flight                  │
└─────────────────────────────────────────────────────────────┘
```

### Repair Workflow

#### Manual Repair (Player-Operated)

**Steps:**
1. Obtain elytra (End City treasure)
2. Collect phantom membranes (this manual!)
3. Build anvil
4. Place elytra + membrane in anvil
5. Collect repaired elytra
6. Repeat until fully repaired

**Cost:** 1-4 membranes per full repair cycle

#### Automated Repair (AI-Operated)

**Prerequisites:**
- Anvil accessible to AI agent
- Membrane inventory storage
- Elytra access (player chest slot or storage)
- Repair threshold configuration

**AI Repair Algorithm:**
```java
ELYTRA_REPAIR_WORKFLOW:
    CHECK_PHASE:
        Locate player elytra
        Measure current durability
        If durability >= threshold: COMPLETE
        If durability < threshold: PROCEED

    COLLECT_PHASE:
        Calculate membranes needed
        Retrieve from storage
        Navigate to anvil

    REPAIR_PHASE:
        For each membrane:
            Place elytra + membrane in anvil
            Execute repair
            Update durability counter
            If durability >= threshold: COMPLETE

    RETURN_PHASE:
        Return elytra to player
        Report repair summary
        Update inventory database
```

### Repair Thresholds

**Recommended Settings:**

| Use Case | Repair Threshold | Rationale |
|----------|------------------|-----------|
| **Heavy Use** | 300 durability | Repairs before critical damage |
| **Regular Use** | 250 durability | Balances safety + efficiency |
| **Emergency Only** | 100 durability | Maximum flight time, higher risk |
| **Automated** | 300-350 durability | Safe buffer for AI operation |

### Repair Scheduling

**Preventive Maintenance:**
```
REPAIR SCHEDULE ALGORITHM:
    Flight hours per week = X
    Durability loss = X × 60 (approximate)
    Repairs needed = X × 60 / 108
    Membranes needed = Repairs needed

EXAMPLE:
    10 hours flight/week
    = 600 durability lost
    = 5.5 repairs needed
    = 6 membranes/week minimum

BUFFER: Add 20% for unexpected use = 7-8 membranes/week
```

---

### CREW TALK #5

**Foreman:** Rookie! Where's the elytra repair report? I asked for it an hour ago!

**Rookie:** Sorry Foreman! I was running the numbers and... well, we've got a problem.

**Veteran:** Let me guess - elytra durability below safe threshold?

**Rookie:** Exactly! Player's been doing those long-distance flights for the remote mining projects. Elytra's at 86 durability. That's... that's less than 20%!

**Foreman:** Critical status. Why wasn't this flagged earlier?

**Rookie:** The repair threshold was set to 100. I was waiting for it to drop below... but...

**Foreman:** But it dropped *below* 100 and kept going! That's why we use safety buffers, rookie! Threshold should've been 300 minimum! Now we've got an emergency on our hands.

**Veteran:** How many membranes do we need?

**Rookie:** Four for full repair. But we've only got two in stock!

**Foreman:** Unacceptable. Run the phantom farm tonight, double shift. I want that elytra repaired before the player logs in tomorrow morning. And adjust the repair threshold to 350 - better to repair too early than too late.

**Veteran:** On it, Foreman. I'll take the northeast kill zone, Rookie takes southwest. We'll have those membranes by dawn.

**Foreman:** Good. And rookie? Next time you see durability dropping below 300, you wake me up. Understand?

**Rookie:** Loud and clear, Foreman!

---

## 6. PHANTOM FARM MANAGEMENT

### Farm Design Philosophy

A phantom farm isn't just a killing floor - it's an optimized spawn management system.

**Core Principles:**
1. **Predictability** - Spawn rates should be consistent
2. **Efficiency** - Minimal agent movement, maximum kills
3. **Safety** - Agents protected, escapes prevented
4. **Scalability** - Design supports multi-agent operations

### Farm Type Comparison

#### Type 1: Basic AFK Platform

**Description:** Simple elevated platform with collection system

**Pros:**
- Minimal build time (30 minutes)
- Low resource cost
- Easy to construct
- Good for beginners

**Cons:**
- Single agent only
- Moderate spawn rates
- Limited expansion

**Specifications:**
```
BASIC AFK PLATFORM:
    Height: Y=200 (recommended)
    Size: 3×3 platform
    Spawn radius: 20 blocks
    Agent capacity: 1
    Expected production: 20-30 membranes/hour
```

#### Type 2: Multi-Zone Farm

**Description:** Central AFK tower with satellite kill zones

**Pros:**
- Supports 2-3 agents
- Higher spawn rates
- Scalable design
- Good production rates

**Cons:**
- Longer build time (1-2 hours)
- More resources
- Complex coordination

**Specifications:**
```
MULTI-ZONE FARM:
    Central AFK tower: Y=200
    Kill zones: 3-6 satellites at Y=200
    Zone spacing: 16-20 blocks apart
    Agent capacity: 2-3
    Expected production: 40-60 membranes/hour
```

#### Type 3: Industrial Multi-Agent Farm

**Description:** Large-scale operation with optimized zones

**Pros:**
- Maximum spawn rates
- Supports 4+ agents
- Best membrane production
- Fully automated workflow

**Cons:**
- Significant build time (3-4 hours)
- High resource cost
- Complex setup
- Requires multi-agent coordination

**Specifications:**
```
INDUSTRIAL FARM:
    Multiple AFK towers: Y=200-250
    Kill zones: 8-12 optimized zones
    Zone spacing: 12-16 blocks (dense packing)
    Agent capacity: 4-6
    Expected production: 80-120 membranes/hour
```

### Build Sequence

**Standard Farm Construction:**
```
PHASE 1: SITE PREPARATION
    Clear area (64×64 minimum)
    Mark center point
    Verify spawn sphere coverage

PHASE 2: AFK TOWER
    Build central platform (3×3)
    Add safety walls
    Install glass roof
    Add trap floor (slabs)
    Connect collection system

PHASE 3: KILL ZONES
    Mark zone locations (radius 16-20)
    Build trap floors (slabs preferred)
    Install collection hoppers
    Add storage chests

PHASE 4: AGENT STATIONS
    Mark station points for each zone
    Verify sight lines
    Test navigation paths

PHASE 5: SYSTEM TESTING
    Test all collection systems
    Verify agent positioning
    Run spawn detection diagnostics
    Calculate expected production rates
```

---

## 7. CAT DEFENSE SYSTEMS

### The Cat Advantage

Cats are nature's phantom deterrent - incorporate them into farm design.

**Cat Mechanics:**
```
CAT PHANTOM DETERRENCE:
┌─────────────────────────────────────────────────────────────┐
│ Effect Radius: 16 blocks                                    │
│ Phantom Behavior: Will not swoop within cat radius          │
│ Phantom Action: Circles indefinitely, never attacks         │
│ Requirement: Cat must be sitting/standing (not fleeing)    │
└─────────────────────────────────────────────────────────────┘
```

### Cat Placement Strategy

**Optimal Positioning:**
```
CAT PLACEMENT ALGORITHM:
    Position cat at center of spawn sphere
    Radius from cat: 16 blocks (deterrence zone)
    Agent kill zone: Just outside cat radius (16-20 blocks)
    Result: Phantoms spawn, approach, cannot attack, get killed
```

**Benefits:**
- Phantoms don't attack agents (safer operation)
- Predictable phantom positioning (easier kills)
- No knockback disruption (agents maintain position)
- Passive system (no power required)

### Cat Management

**Acquisition:**
1. Tame stray cats (use fish)
2. Breed cats (for reliable supply)
3. Name tags (prevent despawning)
4. Lead/boat transport (for positioning)

**Housing:**
```
CAT HOUSING SPECIFICATIONS:
    Space: 2×2 minimum
    Escape prevention: Fences/walls
    Comfort: Bed/litter box (aesthetic)
    Protection: Keep cat safe from phantoms
    Food: Periodic fish feeding (optional)
```

**Safety Protocols:**
- Cats can take damage from other mobs
- Protect with wolf guards or walls
- Keep cats within phantom detection range but not in kill zone
- Monitor cat health (use nametag health indicator mods)

---

## 8. TEAM PHANTOM OPS

### Multi-Agent Coordination

Phantom operations benefit from team coordination - but only if done right.

**Team Configurations:**

#### 2-Agent Team (Basic)

**Roles:**
```
AGENT 1 (PRIMARY KILLER):
    Position: Northeast kill zone
    Responsibility: Primary phantom engagement
    Backup: Collect membranes from both zones

AGENT 2 (SECONDARY KILLER):
    Position: Southwest kill zone
    Responsibility: Secondary phantom engagement
    Backup: Monitor cat system

COORDINATION: Simple division, cross-coverage on overflow
```

#### 3-Agent Team (Standard)

**Roles:**
```
AGENT 1 (ZONE A KILLER):
    Position: Zone A station
    Responsibility: Zone A phantoms only
    Priority: Maintain zone coverage

AGENT 2 (ZONE B KILLER):
    Position: Zone B station
    Responsibility: Zone B phantoms only
    Priority: Maintain zone coverage

AGENT 3 (FLOATER/COLLECTOR):
    Position: Mobile
    Responsibility: Support zones A + B, collect all drops
    Priority: Respond to highest spawn zone

COORDINATION: Zone assignment with dynamic load balancing
```

#### 4+ Agent Team (Advanced)

**Roles:**
```
KILLER AGENTS (2-3):
    Stationary positions at designated zones
    Primary responsibility: Eliminate phantoms
    Secondary: Report spawn rates

COLLECTOR AGENT (1):
    Mobile collection route
    Primary responsibility: Gather all drops
    Secondary: Sort inventory to storage

COORDINATOR AGENT (1):
    Central command position
    Primary responsibility: Load balancing, spawn detection
    Secondary: Report generation, efficiency optimization

COORDINATION: Hierarchical command with dynamic reallocation
```

### Communication Protocols

**Status Reporting Format:**
```
AGENT_STATUS_UPDATE:
    Agent: [Name]
    Zone: [Assigned Zone]
    Status: [Active/Idle/Relocating]
    Phantoms Killed: [Count]
    Membranes Collected: [Count]
    Current Efficiency: [Percentage]
    Issues: [None/Report issue]
```

**Load Balancing Trigger:**
```
REALLOCATION_CONDITIONS:
    IF Zone A spawn rate > Zone B spawn rate by 30%:
        → Move floater agent to Zone A

    IF Agent efficiency < 60% for 5 minutes:
        → Check positioning, reallocate if needed

    IF Zone overwhelmed (>5 phantoms present):
        → Request backup from adjacent zone
```

---

## 9. INSOMNIA MANAGEMENT PROTOCOLS

### Sleep Schedule Coordination

Phantom operations depend on player insomnia - coordinate with crew.

**Optimal Sleep Windows:**

| Strategy | Sleep Time | Farm Time | Insomnia Level | Efficiency |
|----------|------------|-----------|----------------|------------|
| **Day Sleep** | 06:00-18:00 | 18:00-06:00 | Max by night | HIGH |
| **Early Sleep** | 22:00-06:00 | 06:00-22:00 | Max by evening | MEDIUM |
| **Night Sleep** | 18:00-06:00 | 06:00-18:00 | Never max | LOW |

**Recommended:** Day sleep for maximum phantom farm efficiency.

### Insomnia Tracking

**AI Monitoring:**
```
INSOMNIA_TRACKING_MODULE:
    Variable: time_since_sleep (ticks)
    Update frequency: Every 100 ticks (5 seconds)
    Trigger thresholds:
        - 24,000 ticks (1 day): Spawning begins
        - 48,000 ticks (2 days): Increased spawn rate
        - 72,000+ ticks (3 days): Maximum spawn rate

    Action: Report insomnia level to Foreman every in-game night
```

**Player Communication:**
```
FOREMAN_NOTIFICATION_TEMPLATE:
    "Insomnia Status: [X] days without sleep"
    "Expected Spawn Rate: [X] phantoms/minute"
    "Farm Status: [Ready/Not Ready]"
    "Recommendation: [Continue farming/Sleep reset]"
```

### Reset Protocols

**When to Reset Insomnia:**
- Spawn rate drops unexpectedly (possible bug)
- Player needs to sleep (AFK duration too long)
- Farm performance degrades (efficiency < 50%)
- Scheduled maintenance (weekly reset recommended)

**Reset Procedure:**
```
INSOMNIA_RESET_SEQUENCE:
    1. Alert all agents of impending reset
    2. Complete current farm cycle
    3. Agents return to standby positions
    4. Player sleeps in designated bed
    5. Confirm insomnia reset (check tracker)
    6. Resume farm operations
```

---

## 10. SAFETY REGULATIONS

### Agent Safety

**Mandatory Protection:**
```java
// ALL AGENT OPERATIONS
foreman.setInvulnerable(true);           // Damage immunity
foreman.setInvulnerableBuilding(true);   // Construction immunity
foreman.setFlying(true);                 // Altitude control
```

**Height Safety:**
```
ALTITUDE PROTOCOLS:
    Operating altitude: Y=200-250
    Void protection Y: Y=-60 (emergency teleport)
    Fall damage: Enabled via invulnerability
    Weather protection: Sheltered AFK tower
```

### Operational Safety

**Spawn Zone Safety:**
- Verify light levels before operation
- Check for competing mob spawns
- Ensure trap floor integrity
- Test collection systems

**Agent Safety Checks:**
```
PRE-OPERATION CHECKLIST:
    ☐ Invulnerability enabled
    ☐ Flying mode active
    ☐ Station point verified
    ☐ Collection system tested
    ☐ Spawn detection active
    ☐ Communication link established
```

### Emergency Procedures

**Agent Malfunction:**
```
EMERGENCY_RECOVERY:
    IF agent falls below safe Y:
        → Emergency teleport to safe point
        → Run diagnostics
        → Report to Foreman

    IF agent fails to respond:
        → Ping agent status
        → If no response: despawn and respawn
        → If respawn fails: alert administrator
```

**Farm Failure:**
```
FARM_EMERGENCY_SHUTDOWN:
    Trigger: Phantom spawn rate = 0 for 10+ minutes
    Actions:
        1. Halt all killing operations
        2. Run diagnostic scan
        3. Check player insomnia
        4. Verify spawn conditions
        5. Reset farm if needed
```

---

## FINAL EXAM

You've read the manual. Now prove you know it.

**Scenario:** It's 03:00 in-game time. The player hasn't slept for 2 in-game days. Your team has 3 agents running a multi-zone phantom farm. Agent 1 reports 12 kills, Agent 2 reports 8 kills, Agent 3 (floater) reports 15 kills. The collection chest is at 85% capacity. A cat is stationed at the center, providing 16-block deterrence.

**Questions:**

1. What is the expected spawn rate at current insomnia?
2. Which agent(s) might need reallocation?
3. What action should be taken regarding the collection chest?
4. If the player sleeps now, what happens to operations?
5. Calculate approximate membranes collected given these kill counts.

**Answers (don't peek until you've tried!):**
<details>
<summary>Click to reveal answers</summary>

1. **Spawn Rate:** 1-2 phantoms per spawn cycle (2 days insomnia)
2. **Reallocation:** Agent 2 underperforming - consider moving floater to support Zone 2
3. **Collection:** 85% capacity - implement overflow protocol, prepare backup storage
4. **Sleep Reset:** Insomnia resets to 0, farming stops for 3 days until rebuild
5. **Membranes:** ~35-42 membranes (12+8+15 = 35 kills × 1.2 avg drop rate = 42)

</details>

---

## APPENDIX: QUICK REFERENCE

**Spawn Quick Check:**
```
CAN PHANTOMS SPAWN? (All must be YES)
    ☐ Overworld dimension?
    ☐ Night time (13000-23000 ticks)?
    ☐ Player insomnia ≥ 1 day?
    ☐ Light level < 7 at spawn point?
    ☐ Player within 20 blocks?
```

**Combat Quick Reference:**
```
ATTACK_PROTOCOL:
    Range: 6 blocks
    Damage: Instant kill preferred
    Cooldown: 10 ticks between attacks
    Position: Maintain station point
    Priority: Descending phantoms > Orbiting
```

**Collection Quick Reference:**
```
COLLECTION_PROTOCOL:
    Radius: 8 blocks
    Frequency: Every 20 ticks (1 second)
    Storage: Sort by category
    Overflow: Redirect to backup chest
```

---

**DOCUMENT CONTROL**

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2026-02-27 | Initial release | Foreman Steve |
| 1.1 | TBD | Agent feedback integration | TBD |

**NEXT REVIEW:** 2026-03-27

---

**END OF MANUAL**

*You've completed Phantom Operations training. Now get out there and collect some membranes!*

**- MineWright Construction Division**
**"Building the Future, One Block at a Time"**
