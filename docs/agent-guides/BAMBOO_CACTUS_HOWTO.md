# MINEWRIGHT CREW MANUAL
## BAMBOO & CACTUS FARMING OPERATIONS

---

**FOREMAN'S DESK:**
Listen up, crew! Today you're learning the green thumbs of the trade - bamboo and cactus farming. These ain't just pretty plants, they're the engine rooms of any major construction operation. Bamboos grow fast and give us sticks, scaffolding, and fuel. Cacti? They're spiny little gold mines for green dye and XP smelting.

Pay attention to the safety protocols (especially around cacti - trust me), and you'll be running a Tier 1 farm operation by shift's end.

**REQUIRED TOOLS:** Shovels, Silk Touch, Hoppers, Observers, Redstone
**SKILL LEVEL:** Apprentice
**ESTIMATED TIME:** 2 shifts for basic setup, 1 week for Tier 1 automation

---

## TABLE OF CONTENTS

1. [Bamboo Basics](#1-bamboo-basics)
2. [Bamboo Uses](#2-bamboo-uses)
3. [Bamboo Farm Designs](#3-bamboo-farm-designs)
4. [Cactus Basics](#4-cactus-basics)
5. [Cactus Farm Designs](#5-cactus-farm-designs)
6. [Cactus Uses](#6-cactus-uses)
7. [Automatic Collection](#7-automatic-collection)
8. [Team Farming Operations](#8-team-farming-operations)
9. [Efficiency Protocols](#9-efficiency-protocols)
10. [Safety & Troubleshooting](#10-safety--troubleshooting)

---

## 1. BAMBOO BASICS

### Natural Habitat
Bamboo generates in two biomes only, crew:
- **Jungle Biomes** - Common, scattered in clusters
- **Bamboo Jungle Variants** - Dense patches, look for the podzol ground

**PRO TIP:** When the LLM pathfinder sends you on a "find_bamboo" task, it'll prioritize bamboo jungles. Bring a boat - jungles are big and you'll want to travel the rivers.

### Growth Mechanics
This is where bamboo gets special:

| Growth Stage | Height | Time (Random Tick) | Notes |
|--------------|--------|-------------------|-------|
| Sprout | 1 block | ~1 game day | Starts small |
| Small | 2-3 blocks | ~2 game days | Growing |
| Medium | 4-11 blocks | ~3 game days | Produces more |
| Giant | 12-16+ blocks | ~4+ game days | Maximum height |

**Growth Conditions:**
- Requires: Light level 9+ (torchlight works)
- Block below: Dirt, grass, podzol, gravel, mycelium, rooted dirt, moss
- Growth rate: 1 block per 204.8 game ticks (random tick)
- Bone meal: Accelerates growth by 1-2 stages per application
- Max height: 12-16 blocks naturally (can go higher with bone meal)

**AI PLANNING NOTE:** The TaskPlanner breaks bamboo farming into:
1. `locate_bamboo_source` - Pathfind to jungle
2. `harvest_bamboo` - Collect initial stock
3. `prepare_farm_site` - Level ground, place dirt
4. `plant_bamboo` - Place shoots
5. `setup_observers` - Redstone automation (if Tier 1+)

### Planting Protocol

```
ALGORITHM: PLANT_BAMBOO_GRID
---
1. Claim 16x16 work zone (spatial partitioning)
2. Clear vegetation: Execute CLEAR_AREA action
3. Place dirt blocks in checkerboard pattern (alternating rows)
4. Plant bamboo on every other dirt block
5. Skip blocks for observer placement (for automation)
6. Return planting coordinates to SteveMemory
```

**CREW TALK #1**
> **GRIZZLED FOREMAN BAMBOO BILL:** "Alright, listen up you greenhorns! When you're planting bamboo, give it room to BREATHE! I've seen too many apprentices pack 'em tight like sardines and wonder why they get quarter yields. Use the checkerboard pattern - dirt, bamboo, empty, dirt. That empty space? That's for your observer when you upgrade to automatic. And another thing - bamboo likes height. Don't build a ceiling lower than 20 blocks or you're clipping its wings, plain and simple!"

---

## 2. BAMBOO USES

### Primary Products

**Sticks (6 per bamboo):**
- Crafting: Torches, tools, fences, ladders
- Fuel source: Smelt 4 items per stick
- Construction: Scaffolding base material

**Scaffolding (6 bamboo + 1 string = 6 scaffolding):**
- Tier 1 vertical access
- Stackable: Build up to 256 blocks without support
- Time-saver: Essential for large builds
- Auto-descend: Sneak to climb down safely

**Bamboo Planks (1 bamboo = 2 planks):**
- Alternative wood source
- Identical properties to oak planks
- Useful when wood is scarce

**Other Items:**
- Stick: 1 bamboo ( furnace smelting)
- Bamboo Block: 9 bamboo (decorative)
- Bamboo Slab/Stairs: Crafting variations

### Fuel Efficiency

| Item | Smelt Count | Efficiency |
|------|-------------|------------|
| Bamboo (raw) | 0.25 items | Low |
| Stick | 4 items | Medium |
| Scaffolding | 4 items | Medium |
| Bamboo Block | 0.25 items | Low |

**FUEL PRO TIP:** Always convert bamboo to sticks before smelting. 4 bamboo = 4 sticks = 16 items smelted. Raw bamboo? Terrible efficiency.

### Special Uses

**Panda Breeding:**
- Pandas eat bamboo (must be within 8 blocks)
- Required for: Breeding, speeding up baby growth
- Only spawns in: Jungle biomes

**Decorative:**
- Bamboo blocks for Asian-themed builds
- Stripped bamboo (axe) for texture variation

**Creeper Defense:**
- Dense bamboo creates a barrier
- Creeper explosions destroy bamboo, not your builds
- Cheap, renewable sacrificial layer

---

## 3. BAMBOO FARM DESIGNS

### Tier 0: Manual Farm

**Layout:**
```
Ground view (top-down):
[D = Dirt, B = Bamboo, _ = Empty for observer]

Row 1: D B _ D B _ D B _ D B
Row 2: _ D B _ D B _ D B _ D
Row 3: D B _ D B _ D B _ D B
Row 4: _ D B _ D B _ D B _ D
...
```

**Construction Steps:**
1. Mark 16x16 area (256 bamboo max)
2. Place dirt in checkerboard
3. Plant bamboo on every other dirt
4. Leave 1-block gaps for future automation
5. Add torches (light level 14)

**Yield:** 128 bamboo per harvest cycle
**Efficiency:** Manual, ~10 minutes harvest time
**Upgrade Path:** Add observers for Tier 1

### Tier 1: Observer-Based Auto-Harvest

**Mechanism:**
```
Side view:
[A = Air, O = Observer, P = Piston, F = Furnace Minecart, H = Hopper]

Height 16: B (bamboo top)
Height 15: B
...
Height 3:  B
Height 2:  B
Height 1:  O (facing bamboo) -> P (facing bamboo)
Ground:    F (on hopper line) -> H -> Chest
```

**How It Works:**
1. Observer detects bamboo growth
2. Triggers piston to break bamboo
3. Bamboo falls onto hopper minecart track
4. Minecart delivers to collection system
5. Rinse and repeat

**Redstone Circuit:**
```
Observer -> Piston (1-tick pulse)
Observer should be at height 1-2 (detects base growth)
Piston pushes upward, breaks entire stalk
```

**Material List (per 16x16 section):**
- Observers: 128 (every other column)
- Pistons: 128
- Hoppers: 16 (collection line)
- Powered rails: 8 (minecart acceleration)
- Redstone: 2 stacks
- Chests: 4 (storage)

**Yield:** ~2,000 bamboo/hour (theoretical max)
**Actual:** ~800-1,200 bamboo/hour (chunk loading limits)

**CREW TALK #2**
> **SHIFT LEAD "REDSTONE" ROSIE:** "Alright crew, listen close! Observers ain't miracle workers. They only update when the CHUNK LOADS. You build a Tier 1 farm 500 blocks from spawn and wander off? You're getting zero yield, zilch, nada! The LLM pathfinder marks chunk boundaries in SteveMemory for a reason. Keep your farms within 128 blocks of spawn or an AFK pool, or you're wasting everyone's time. And another thing - observer orientation matters! The face watches the bamboo, the back sends the pulse. Get it backwards and you've got yourself a very expensive decorative block. Test with redstone lamps before you commit the build!"

### Tier 2: Zero-Tick Farm (1.20.1 Limited)

**IMPORTANT:** In Minecraft 1.20.1, zero-tick farms are PATCHED. Random tick updates are required for bamboo growth.

**Alternative: Flying Machine Farm**
```
Design:
- Flying machine moves along track above bamboo
- Pistons mounted below machine break bamboo as it passes
- Minecart with hopper collects items
- Machine cycles back and forth continuously
```

**Yield:** ~1,500-2,000 bamboo/hour
**Complexity:** High (requires flying machine expertise)
**Maintenance:** Low (once tuned)

### Tier 3: Multi-Agent Collaborative

**Spatial Partitioning Strategy:**
```
[A = Agent 1 zone, B = Agent 2 zone, C = Agent 3 zone]

Zone A (64x64): Agent 1 maintains harvest
Zone B (64x64): Agent 2 maintains harvest
Zone C (64x64): Agent 3 maintains harvest
Central Hub: Storage, processing, distribution
```

**Protocol:**
1. Each agent claims 64x64 zone (atomic operation via `ConcurrentHashMap`)
2. Agent executes `harvest_bamboo` action (tick-based, non-blocking)
3. Agent deposits in central storage
4. Dynamic rebalancing: If Zone A completes, Agent 1 claims Zone D

**Coordination Pattern:**
```
EVENT_BUS.publish(new BambooHarvestCompleteEvent(agentId, zoneId));
-> CollaborativeBuildManager.rebalanceZones();
-> Idle agents claim pending zones
```

**Yield:** Scales with agent count (3 agents = ~3,600 bamboo/hour)

---

## 4. CACTUS BASICS

### Natural Habitat
Cacti spawn naturally in:
- **Desert Biomes** - Abundant
- **Badlands** - Less common
- **Eroded Badlands** - Rare patches

**PRO TIP:** Desert temples often have cactus gardens. Perfect starter stock.

### Growth Mechanics

| Growth Stage | Height | Time (Random Tick) |
|--------------|--------|-------------------|
| Small | 1 block | Starts immediately |
| Medium | 2-3 blocks | ~1 game day |
| Large | 3+ blocks | ~2+ game days |

**Growth Conditions:**
- Requires: Sand or red sand (bottom block only)
- Light: Any level (grows in darkness)
- Space: All 4 side blocks must be empty air
- Max height: 3 blocks naturally
- Bone meal: Does NOT work on cactus (important!)

**Growth Rate Formula:**
```
Random tick chance: 1/8 per game tick per chunk section
Growth attempt: If random tick hits, cactus has 1/16 chance to grow
Expected time: ~10-20 minutes per growth stage (depends on chunk loading)
```

**DAMAGE MECHANIC:**
- Contact damage: 1 heart every 0.5 seconds
- Touching any side block (not bottom) triggers damage
- Knocks back slightly
- Does NOT damage through armor (well, minimal)

**SAFETY PROTOCOL:** Always use `NavigateAction` with `avoid_cactus=true` parameter. The pathfinder automatically routes around cactus patches.

### Planting Protocol

```
ALGORITHM: PLANT_CACTUS_GRID
---
1. Claim 16x16 work zone
2. Place sand in 1-block gaps (4-block minimum spacing)
3. Plant cactus on sand
4. Leave 3 blocks air on all sides (critical!)
5. Add light sources (not required, but good practice)
6. Build collection system below
7. Return coordinates to SteveMemory
```

**DO NOT PLACE:**
- Adjacent blocks (cactus breaks)
- Above cactus (cactus breaks)
- Water nearby (cactus breaks - odd but true)

---

## 5. CACTUS FARM DESIGNS

### Tier 0: Manual Farm

**Layout (Top-Down):**
```
[S = Sand, C = Cactus, _ = Air gap]

Row 1: C _ _ _ C _ _ _ C _ _ _
Row 2: _ _ _ _ _ _ _ _ _ _ _ _
Row 3: _ _ _ C _ _ _ C _ _ _ C
Row 4: _ _ _ _ _ _ _ _ _ _ _ _
...
```

**Spacing:**
- Minimum: 4 blocks between cacti (including air gap)
- Recommended: 5 blocks for easier harvest
- Pattern: Checkerboard with 4-block gaps

**Yield:** 64 cactus per harvest (16x16 grid)
**Harvest Time:** ~5 minutes (be careful!)
**Danger:** Medium (1 heart damage if careless)

**CREW TALK #3**
> **OLD TIMER "SPINELESS" SAM:** "Let me tell you about the Great Cactus Massacre of '23. We had three apprentices - fresh spawns, straight from the theoretical learning module. They built a beautiful cactus farm, dense as jungle, packed tight for efficiency. Looked good on paper. Then harvest time came. First kid walks in, gets clipped by three cactus at once. Knocked sideways into another patch. It was a chain reaction, domino effect of disaster! Lost three inventories full of diamonds. Moral of the story: SPACE YOUR CACTUS! Give 'em room. Use the 4-block rule. And wear armor - even leather saves you from the 'ouch, I dropped my stuff' scenario. Been farming cacti for 500 hours and I still keep golden apples on my hotbar. Learn from our mistakes, crew!"

### Tier 1: Water Stream Collection

**Mechanism:**
```
Top view (water layer, height 2):
[W = Water flow, F = Flow direction]

Row 1: C W W W C W W W C W W W
Row 2: ^ W W W ^ W W W ^ W W W  (^ = cactus above)
Row 3: W W W W W W W W W W W W
Row 4: C W W W C W W W C W W W
...
```

**Side View:**
```
Height 3:  C (cactus top)
Height 2:  C (cactus middle) -> grows here
Height 1:  C (cactus bottom) -> planted on sand
Ground:    S (sand) on top of glass
Below:     Water stream (flowing to collection)
```

**How It Works:**
1. Cactus grows to height 3
2. Top block (height 3) breaks automatically (4th growth attempt)
3. Broken cactus falls onto adjacent glass
4. Glass slopes into water stream
5. Water flows to central collection point
6. Hoppers catch items, deposit in chest

**Material List (16x16):**
- Sand: 64 blocks
- Cactus: 16 (starter)
- Glass: 128 (slopes)
- Water buckets: 4
- Hoppers: 8
- Chests: 2
- Building blocks: 256 (structure)

**Yield:** ~400 cactus/hour
**Maintenance:** Zero (passive collection)
**Efficiency:** High (once built)

**Construction Steps:**
1. Build 16x16 platform at Y=64
2. Place water streams in 4 channels
3. Place glass diagonally to direct items to water
4. Place sand on glass (sand does NOT block water)
5. Plant cactus on sand
6. Wait for automatic growth

**Important Notes:**
- Cactus grows through flowing water (odd but true)
- Top block breaks when 4th growth attempts
- No observers needed (passive design)
- Bone meal useless (don't waste it)

### Tier 2: Observer Piston Farm

**Mechanism:**
```
Side view:
[O = Observer, P = Piston, H = Hopper, _ = Air]

Height 3:  C (cactus top)
Height 2:  C (cactus)
Height 1:  C (cactus bottom) on sand
Ground:    S (sand) on glass
Below:     O -> P -> H -> Chest
```

**How It Works:**
1. Observer detects cactus growth at height 2
2. Triggers piston to push cactus
3. Cactus breaks (pushing breaks it)
4. Broken cactus falls into hopper
5. Hopper deposits in chest

**Redstone Circuit:**
```
Observer (facing cactus) -> Piston (facing cactus)
Piston should be 1 block away (push distance 1)
Pulse length: 1 tick (observer default)
```

**Material List (16x16):**
- Observers: 64 (every other column)
- Pistons: 64
- Hoppers: 16
- Redstone: 1 stack
- Sand: 64
- Glass: 64

**Yield:** ~600 cactus/hour (faster than water)
**Maintenance:** Low (pistons rarely break)
**Efficiency:** High (forced harvest cycle)

**CREW TALK #4**
> **AUTOMATION SPECIALIST "PISTON" PETE:** "Listen up, automation engineers! I've seen too many observer-piston cactus farms fail because of one simple mistake: observer placement. Put your observer facing the cactus at GROUND LEVEL, not at the top! Why? Because cactus grows from the BOTTOM, not the top. Observer at the top? You're watching the wrong block. At ground level, you detect the height-2 growth immediately. That's when you strike - piston push, break the cactus, reset the cycle. Timing is everything. Also, use sticky pistons if you want to push sand blocks for reconfiguration, but regular pistons are fine for just breaking cactus. Save your slimeballs for the honey farm - that's a whole different manual. Test with a single cactus before you commit to 64. One working prototype is worth a dozen broken farms!"

### Tier 3: Sub-Block Stack Farm (Advanced)

**Mechanism:**
```
Exploit: Cactus can be placed on:
- Sand
- Red sand
- Other cactus (with item frame trick)

NOT directly exploitable in 1.20.1 (patched)
```

**NOTE:** Sub-block stacking cactus farms were patched in 1.16+. Do NOT attempt - you'll waste materials.

**Alternative: Multi-Layer Farm**
```
Vertical stacking (3 layers):
Layer 3 (Y=68): Cactus row (water collection)
Layer 2 (Y=66): Cactus row (water collection)
Layer 1 (Y=64): Cactus row (water collection)
```

**Space 3 layers vertically** (2 blocks between layers for growth and collection)

**Yield:** 1,200 cactus/hour (3x Tier 2)
**Complexity:** Medium (vertical stacking)
**Maintenance:** Low (independent layers)

---

## 6. CACTUS USES

### Primary Products

**Green Dye (1 cactus = 1 dye):**
- Wool dyeing: Green wool
- Concrete powder: Green concrete (water -> solid)
- Terracotta: Green terracotta
- Glass: Green stained glass
- Candle: Green candle
- Firework stars: Green trails
- Shulker box: Green dye

**Smelting (1 cactus = 0.2 XP):**
- Smelt cactus in: Furnace, smoker, blast furnace
- Product: Green dye (same as crafting)
- XP yield: 0.2 per cactus
- Time: 200 ticks (10 seconds)

**XP FARMING:**
```
1,000 cactus = 200 XP (approx level 10)
10,000 cactus = 2,000 XP (approx level 30)
```

**Defense:**
- Mob damage: 1 heart per 0.5 seconds
- Player damage: Same (use with caution!)
- PvP: Barrier around bases
- Mob trap: Cactus wall (zombies can't pathfind through)

**Decorative:**
- Desert theme builds
- Spikes and barriers
- Fake desert landscaping
- Cactus fences (expensive but cool)

---

## 7. AUTOMATIC COLLECTION

### Hopper Systems

**Basic Collection Line:**
```
Layout:
[O = Item output, H = Hopper, C = Chest]

Farm drop point -> H -> H -> H -> C
                   |    |    |
                  (all hoppers point toward chest)
```

**Hopper Chain Rules:**
1. Point toward chest (direction matters!)
2. Space every 2 blocks (hopper range = 1 block)
3. Use hopper minecarts for long distances
4. Filter with comparator for specific items

**Item Filtering:**
```
Design:
[1 = Item slot 1, 2 = Item slot 2, etc.]

Hopper 1: Locked slot 1 with bamboo (only bamboo passes)
Hopper 2: Locked slot 1 with cactus (only cactus passes)
Hopper 3: Empty (catches everything else)
```

**Redstone Filter:**
```
Comparator -> Redstone torch -> Hopper (disable when full)
Prevents hopper overflow and item loss
```

### Minecart Collection (Long Distance)

**Hopper Minecart System:**
```
Layout:
[F = Furnace minecart (pusher), H = Hopper minecart (collector)]

Farm drop -> [H] on track -> [F] pushes -> Storage chest
             |
             (collects items continuously while moving)
```

**Advantages:**
- Collects over long distances (unlimited range)
- Continuous operation (while chunk is loaded)
- Sorting possible with detector rails

**Disadvantages:**
- Requires rails (expensive)
- Chunk loading dependency
- Slower than hopper lines (short distance)

### Storage Management

**Automatic Smelting:**
```
Layout:
[C = Cactus chest, F = Furnace, H = Hopper]

C -> H (extracts cactus) -> F (smelts) -> H (extracts dye) -> Dye Chest
```

**Smelter Automation:**
- Hopper below cactus chest (extracts)
- Hopper above furnace (inputs)
- Hopper below furnace (outputs)
- Requires fuel system (charcoal, bamboo, etc.)

**Storage Organization:**
```
Barrel 1: Bamboo raw
Barrel 2: Sticks (crafted)
Barrel 3: Scaffolding (crafted)
Barrel 4: Cactus raw
Barrel 5: Green dye (smelted/crafted)
Barrel 6: Fuel source
```

**CREW TALK #5**
> **WAREHOUSE MANAGER "STACK" STANLEY:** "Newbies always make the same mistake: they build a beautiful farm, collect 10,000 bamboo, then dump it all in one double chest. Two days later, they're crying because they can't find anything and the inventory system is lagging the server. ORGANIZE YOUR OUTPUT! I run the 'Six-Barrel System' - bamboo, sticks, scaffolding, cactus, dye, fuel. Label your barrels. Use item frames on the front. And for Steve's sake, use hoppers to auto-sort! It takes 10 minutes to set up a sorting system and saves you hours of headache. The LLM inventory optimizer can auto-sort, but only if you've set up the infrastructure. Do it right the first time - I've dismantled too many messy farms to count!"

---

## 8. TEAM FARMING OPERATIONS

### Multi-Agent Coordination

**Role Distribution:**
```
Agent 1: Primary Harvester (bamboo zone)
Agent 2: Primary Harvester (cactus zone)
Agent 3: Logistics (transport, storage, processing)
Agent 4: Maintenance (redstone, hopper repairs)
```

**Spatial Partitioning:**
```
Map layout (256x256 total):

Zone 1 (NW): Bamboo Farm (64x64) - Agent 1
Zone 2 (NE): Cactus Farm (64x64) - Agent 2
Zone 3 (SW): Storage Hub (64x64) - Agent 3
Zone 4 (SE): Processing (64x64) - Agent 4
```

**Protocol:**
```
1. Event Bus broadcasts: FarmMaintenanceRequiredEvent
2. Available agents claim zones (atomic operation)
3. Agent executes assigned action (non-blocking tick-based)
4. On complete: publish ZoneCompletedEvent
5. CollaborativeBuildManager rebalances if needed
```

### Communication Protocol

**Agent-to-Agent Chat:**
```
Agent 1: "Bamboo Zone A harvest complete. 2,400 items collected."
Agent 3: "Logistics dispatched. ETA: 200 ticks."
Agent 2: "Cactus Zone B requires maintenance. Observer at [32, 64, 128] malfunction."
Agent 4: "Maintenance crew dispatched. ETA: 150 ticks."
```

**Event Bus Messages:**
```java
// Internal events (LLM integration)
FarmHarvestEvent(zoneId, agentId, itemCount)
FarmMaintenanceEvent(zoneId, issueType, urgency)
StorageFullEvent(storageId, itemType, currentCount)

// Agent responses
AgentClaimZoneEvent(agentId, zoneId)
AgentCompleteTaskEvent(agentId, taskId, result)
```

### Large-Scale Operations

**Industrial Bamboo Complex (Tier 10):**
```
Layout (10 zones total):
- 8 bamboo farms (64x64 each)
- 1 central storage hub
- 1 processing facility

Capacity: ~80,000 bamboo/hour
Staffing: 10 agents (8 harvesters, 2 logistics)
Output: 320,000 sticks/hour (fuel empire)
```

**Industrial Cactus Complex (Tier 10):**
```
Layout (10 zones total):
- 8 cactus farms (64x64 each)
- 1 central smelting hall
- 1 dye storage facility

Capacity: ~48,000 cactus/hour
Staffing: 10 agents (8 harvesters, 2 smelters)
Output: 9,600 XP/hour (level 30 every 20 minutes)
```

**Coordination Commands:**
```
/minewright assign <agent> <zone> - Manually assign zone
/minewright status - View all agent zones
/minewright rebalance - Redistribute workload
/minewright harvest <bamboo|cactus> - Manual harvest trigger
```

---

## 9. EFFICIENCY PROTOCOLS

### Growth Rate Optimization

**Bamboo:**
```
Base rate: 1 block per 204.8 ticks (random tick)
Optimized: 2-3 bone meal applications -> instant height 12
Time savings: 4 minutes -> 10 seconds
Cost: 2 bone meal per bamboo (reasonable trade)
```

**Cactus:**
```
Base rate: 1 growth stage per ~10 minutes (random tick)
Bone meal: Does NOT work (don't waste!)
Optimization: Zero (relies on random tick only)
Strategy: Build more farms (scale out, not up)
```

### Collection Efficiency

**Hopper vs. Minecart:**
```
Distance < 32 blocks: Use hoppers (faster, cheaper)
Distance 32-128 blocks: Use hopper + minecart hybrid
Distance > 128 blocks: Use hopper minecart exclusively
```

**Hopper Timing:**
```
Item transfer rate: 8 items per 2.5 seconds (8 ticks)
Transfer time: 2.5 seconds per stack
Optimization: Use multiple parallel hopper lines
```

### Layout Optimization

**Bamboo:**
```
Optimal spacing: Checkerboard (every other block)
Yield per chunk: 128 bamboo (16x16 grid)
Growth area: 256 blocks (16x16)
Efficiency: 50% coverage (balance of growth and access)
```

**Cactus:**
```
Optimal spacing: 4-block gaps (including cactus)
Yield per chunk: 64 cactus (16x16 grid with gaps)
Growth area: 256 blocks (16x16)
Efficiency: 25% coverage (safety requirement)
```

**Vertical Farming:**
```
Bamboo: Single layer only (height growth is vertical)
Cactus: Up to 3 layers (2-block separation)
Total yield per chunk (cactus): 192 cactus (3 layers x 64)
```

### Production Calculations

**Bamboo Production:**
```
Time to max height: ~4 game days (80 minutes)
Yield per bamboo: 12-16 bamboo
16x16 farm (128 bamboo): 1,536-2,048 bamboo per cycle
Cycles per hour: ~0.75 (with random tick variance)
Hourly yield: ~1,200 bamboo
Daily yield: ~28,800 bamboo
```

**Cactus Production:**
```
Time to height 3: ~2 game days (40 minutes)
Yield per cactus: 3 cactus (top breaks automatically)
16x16 farm (64 cactus): 192 cactus per cycle
Cycles per hour: ~1.5 (faster than bamboo)
Hourly yield: ~400 cactus
Daily yield: ~9,600 cactus
```

**Fuel Production (Bamboo -> Sticks):**
```
1 bamboo -> 6 sticks
1 stick -> 4 items smelted
1,200 bamboo/hour -> 7,200 sticks/hour -> 28,800 items smelted/hour
Efficiency: Excellent (better than coal farms at this scale)
```

**XP Production (Cactus -> Smelting):**
```
1 cactus -> 0.2 XP
400 cactus/hour -> 80 XP/hour
Time to level 30: ~10 hours (passive, zero effort)
Smelting time: 200 ticks per cactus (10 seconds x 400 = 4,000 ticks = 3.33 minutes smelting time total)
```

---

## 10. SAFETY & TROUBLESHOOTING

### Safety Protocols

**Cactus Handling:**
1. Always wear armor (leather minimum)
2. Approach from below (cactus only damages sides)
3. Use sticks to break (distance bonus + reach)
4. Never place blocks adjacent (breaks cactus)
5. Keep golden apples on hotbar (emergency healing)

**Bamboo Handling:**
1. No damage risk (safe)
2. Watch for pandas (aggressive if attacked)
3. Jungle mobs (ocelots, parrots, creepers)
4. Bring torches (canopy = dark)
5. Clear vegetation before building

**Redstone Safety:**
1. Test circuits with redstone lamps first
2. Use repeaters to extend signal (no signal loss)
3. Observer orientation matters (face = watches, back = pulses)
4. Piston timing: 1 tick pulse usually sufficient
5. Always have backup materials (redstone fails sometimes)

### Common Issues

**Bamboo Not Growing:**
```
Possible causes:
1. Light level too low (< 9)
2. Wrong soil (requires dirt, grass, podzol, gravel, mycelium, rooted dirt, moss)
3. Ceiling too low (bamboo needs 12+ blocks vertical)
4. Chunk not loaded (AFK pool needed)
5. Bone meal not working (bamboo has growth limit)

Solutions:
- Add torches (light level 14)
- Replace soil with dirt
- Raise ceiling to 20 blocks
- Stay within 128 blocks of spawn
- Use bone meal sparingly (2-3 applications max)
```

**Cactus Not Growing:**
```
Possible causes:
1. Adjacent blocks (cactus breaks if blocked)
2. Wrong soil (requires sand or red sand only)
3. Chunk not loaded (same as bamboo)
4. Random tick variance (normal, wait longer)
5. Water nearby (cactus breaks if watered)

Solutions:
- Remove adjacent blocks (4-block rule)
- Replace with sand
- Stay within 128 blocks of spawn
- Wait 20+ minutes (random tick variance)
- Keep water 2+ blocks away
```

**Observer Not Triggering:**
```
Possible causes:
1. Observer facing wrong direction
2. Observer out of range (only detects adjacent block)
3. Chunk not loaded
4. Redstone signal blocked
5. Observer waiting for different update type

Solutions:
- Face observer toward watched block
- Place observer adjacent (1 block away)
- Stay within 128 blocks of spawn
- Check redstone path (use repeaters)
- Observer detects block updates, not state changes
```

**Hopper Not Collecting:**
```
Possible causes:
1. Hopper facing wrong direction
2. Hopper disabled (redstone signal)
3. Hopper full (capacity 5 stacks)
4. Items not landing in hopper range (1 block)
5. Chunk not loaded

Solutions:
- Point hopper toward collection point
- Remove redstone signal
- Empty hopper regularly (add more hoppers)
- Adjust drop point (water stream helps)
- Stay within 128 blocks of spawn
```

**Yield Lower Than Expected:**
```
Possible causes:
1. Chunk loading issues (most common)
2. Entity cramming (too many items)
3. Spawn chunks not loaded
4. Observer/piston timing issues
5. Farm design inefficiencies

Solutions:
- Add AFK pool (always loading chunks)
- Add more hoppers (increase collection rate)
- Verify spawn chunks (mark with coordinates)
- Test single observer before replicating
- Optimize layout (refer to efficiency section)
```

### Emergency Procedures

**Inventory Overflow:**
```
Symptoms: Lag, items disappearing
Causes: Too many items in loaded chunks
Solutions:
1. Add more hoppers/chests (increase storage)
2. Add item frames (despawn timer)
3. Use composters (dispose excess)
4. Build additional storage (scale up)
```

**Redstone Circuit Burnout:**
```
Symptoms: Pistons not firing, observers not triggering
Causes: Redstone signal loops, timing issues
Solutions:
1. Break circuit (remove power source)
2. Add repeaters (prevent signal loops)
3. Adjust timing (use repeater delays)
4. Test single component first
```

**Chunk Loading Failures:**
```
Symptoms: Farm not producing when away
Causes: Distance from spawn, no AFK pool
Solutions:
1. Build AFK pool (always load chunks)
2. Move farm closer to spawn (<128 blocks)
3. Use chunk loaders (if server allows)
4. Agent patrol (keep area loaded)
```

---

## APPENDIX: QUICK REFERENCE

### Command Reference
```
/minewright spawn <name> - Spawn bamboo/cactus specialist agent
/minewright assign <agent> <bamboo|cactus> - Assign farm type
/minewright status - View farm production rates
/minewright harvest - Manual harvest trigger
/minewright replant - Replant harvested area
```

### Material Checklist (Tier 1 Farm)

**Bamboo Farm (16x16):**
- Bamboo: 128 (starter)
- Dirt: 64
- Observers: 128
- Pistons: 128
- Hoppers: 16
- Chests: 4
- Redstone: 2 stacks
- Torches: 1 stack

**Cactus Farm (16x16):**
- Cactus: 16 (starter)
- Sand: 64
- Glass: 128
- Water buckets: 4
- Hoppers: 8
- Chests: 2
- Building blocks: 256

### Growth Timers

| Crop | Time to Max Height | Yield per Plant | Bone Meal Effective? |
|------|-------------------|-----------------|---------------------|
| Bamboo | 4 game days (80 min) | 12-16 | Yes (2-3 apps) |
| Cactus | 2 game days (40 min) | 3 | No |

### Production Rates (16x16 Farm)

| Farm Type | Hourly Yield | Daily Yield | Uses |
|-----------|-------------|-------------|------|
| Bamboo | 1,200 | 28,800 | Sticks, fuel, scaffolding |
| Cactus | 400 | 9,600 | Green dye, XP, defense |

---

**FOREMAN'S CLOSING:**

There you have it, crew - the complete bamboo and cactus farming playbook. Start with Tier 0, learn the mechanics, then upgrade as you gain experience. These crops are the backbone of any industrial operation - cheap, fast-growing, and infinitely renewable.

Remember: Space your cactus, face your observers, and keep those chunks loaded. A well-designed Tier 2 farm pays for itself ten times over in the first week.

Now get out there and start planting! The construction crew isn't going to wait for their scaffolding.

**FOREMAN BAMBOO BILL**
**MineWright Construction Crew**
**Date: 2026-02-27**

---

*"Bamboo grows fast, cactus grows slow. But both build fortunes."* - MineWright Proverb

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Maintained By:** MineWright Training Division
**Next Review:** 2026-03-27

**Related Manuals:**
- AUTOMATIC_FARMING.md (General automation principles)
- REDSTONE_ENGINEERING.md (Circuit design)
- WAREHOUSE_MANAGEMENT.md (Storage systems)
- XP_FARMING.md (Advanced XP production)
- CONSTRUCTION_SCAFFOLDING.md (Scaffolding applications)

---

**END OF MANUAL**
