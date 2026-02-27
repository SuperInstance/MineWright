# COPPER OPERATIONS CREW MANUAL
## MineWright Construction Division - Training Module 007

**Document Classification:** Public Training Material
**Last Updated:** 2026-02-27
**Instructor:** Senior Copper Operations Specialist
**Prerequisites:** Basic Mining Certification, Block Identification Module 001

---

## FOREWORD TO THE CREW

Listen up, MineWrights! You're about to enter the copper operations division. This isn't your average dig-and-dash operation. Copper is special—it changes over time, it conducts lightning like a champ, and it's the backbone of modern construction. Whether you're a Steve agent processing natural language commands or a human crew chief with a blueprints tablet, this manual will turn you from a rookie into a copper operations veteran.

**In this manual, you'll learn:**
- Where the copper ore hides and how to extract it efficiently
- The full copper block family and their transformation pathways
- How to work with oxidation (yes, it turns green, and that's beautiful)
- Lightning protection for your bases
- Precision observation tools
- Archaeology brushing techniques
- Aesthetic applications for showpiece builds
- Team coordination patterns for maximum yield

---

## MODULE 1: FINDING COPPER

### Natural Distribution Patterns

Copper ore generates in the underground layers of your Minecraft world. Here's what your task planning algorithms need to know:

**Y-Level Distribution:**
- **Primary Range:** Y = -16 to Y = 112
- **Peak Concentration:** Y = 48
- **Maximum Mining Depth:** Y = -16
- **Distribution Pattern:** Regular ore blobs with variance

**Biome Bonuses:**
Your LLM's context window should flag these biomes for bonus copper yield:

| Biome Type | Bonus Multiplier | Strategic Value |
|------------|------------------|-----------------|
| **Stony Peaks** | Standard | High density, exposed ore |
| **Windswept Hills** | Standard | Easy surface access |
| **Badlands** | Standard | Multiple resource overlap |
| **Dripstone Caves** | Standard | Large caverns for bulk mining |

**Ore Blob Characteristics:**
- Average blob size: 6-14 ore blocks
- Maximum potential: Up to 26 blocks in rare clusters
- Mining efficiency: Fortune III enchantment recommended
- Tool requirement: Stone pickaxe or higher (diamond preferred for operations)

### Automated Mining Protocols

When your Steve agents receive "mine copper" directives:

```java
// ActionRegistry reference
"COPPER_MINE" -> {
    "target_ore": "copper_ore",
    "optimal_y_range": [48, 64],  // Start near peak
    "mining_pattern": "branch",
    "tool_enchantment": "fortune_iii",
    "avoid_blocks": ["lava", "deepslate_copper_ore"]  // Separate strategy
}
```

---

> **CREW TALK #1**
>
> **Rookie:** "Hey Cap, the planning algorithm says we should strip-mine at Y=48. Is that optimal?"
>
> **Copperbeard (15-year veteran):** "Listen closer to what the LLM's telling you, kid. It said START at 48. Copper's spread over a huge vertical range—more than iron, less than coal. What you want is the SPOTTING pattern. Mine horizontal branches every 4-5 blocks vertically. You'll catch veins the single-level strip would miss. That's how we got 3 stacks in last shift—THINK PARALLEL."
>
> **Rookie:** "But won't that take longer?"
>
> **Copperbeard:** "Time's nothing if you're mining empty stone. Efficiency isn't speed—it's yield per effort. Now grab your pick and let the agents handle the planning. You focus on NOT falling into lava."

---

## MODULE 2: COPPER BLOCK TYPES

### Transformation Pathway

Copper processing follows a linear transformation chain. Your agents should memorize this sequence:

```
RAW COPPER → COPPER INGOT → COPPER BLOCK → CUT COPPER → [VARIANTS]
     ↓           ↓              ↓             ↓
   [Smelt]   [Craft 9]      [Craft 1]    [Stonecutter]
```

### Complete Block Reference

| Block Name | Crafting Recipe | Uses | Notes |
|------------|-----------------|------|-------|
| **Raw Copper** | N/A (drops from ore) | Smelting input | Must smelt before use |
| **Copper Ingot** | Smelt Raw Copper | Universal crafting unit | Base currency of copper ops |
| **Copper Block** | 9 Ingots (3×3) | Storage, lightning rods, crafting | Compressible for transport |
| **Cut Copper** | 4 Blocks (2×2) | Building material | First cutting stage |
| **Cut Copper Stairs** | 6 Cut Copper | Vertical access | Standard stair behavior |
| **Cut Copper Slab** | 6 Cut Copper | Thinner layers | Double-slab reversible |
| **Chiseled Copper** | 4 Cut Copper | Decorative, unique texture | Aesthetic specialty |
| **Copper Grate** | 4 Cut Copper | Semi-transparent, see-through | Light passes, mobs don't |
| **Copper Trapdoor** | 2 Cut Copper | Horizontal barrier | Redstone compatible |
| **Copper Door** | 3×2 Cut Copper | Vertical barrier | Redstone compatible |
| **Copper Bulb** | 1 Cut Copper + 1 Blaze Rod + 1 Redstone | Light source | Toggleable with redstone |

**Storage Optimization:**
- 1 Copper Block = 9 Ingots (compression ratio 9:1)
- For bulk transport: Always compress to blocks
- For crafting: Decompress as needed via crafting grid

### Agent Execution Pattern

```java
// Copper transformation action sequence
"COPPER_PROCESS" -> {
    "input": "raw_copper",
    "output_target": "copper_block",  // Compress for storage
    "intermediate": "copper_ingot",
    "smelting_priority": "immediate",
    "compression_threshold": 9  // Auto-compress at 9 ingots
}
```

---

## MODULE 3: OXIDATION MECHANICS

### Understanding the Patina Process

Copper blocks possess a unique property among Minecraft materials: **they oxidize over time** when exposed to air. This isn't a bug—it's a feature for architectural design.

**Oxidation Stages:**

| Stage | Block Name | Color Appearance | Time to Reach (approx.) |
|-------|------------|------------------|--------------------------|
| **Unoxidized** | Copper Block | Orange-red | Initial state |
| **Exposed** | Exposed Copper | Slightly browned | ~50-70 Minecraft days |
| **Weathered** | Weathered Copper | Green-brown mix | ~100-140 Minecraft days |
| **Oxidized** | Oxidized Copper | Full teal-green | ~150-200 Minecraft days |

**Game Mechanics Note:** Oxidation progresses through random block updates in loaded chunks. Unloaded chunks do not oxidize.

### Waxing: Preserving the Stage

**Honeycomb Application:**
- Right-click with honeycomb on any copper block variant
- Effectively "freezes" oxidation at current stage
- Useful for maintaining specific color schemes
- Reversible (see scraping below)

**Agent Integration:**
```java
"COPPER_WAX" -> {
    "target_stage": "exposed",  // Stop at this stage
    "tool": "honeycomb",
    "action": "right_click",
    "coverage": "full_surface"
}
```

### Scraping: Reversing Oxidation

**Axe Usage:**
- Right-click with axe on oxidized copper blocks
- Removes one oxidation stage per application
- Fully oxidized → weathered → exposed → unoxidized
- Does NOT remove wax (must axe waxed copper to dewax first)

**Design Pattern:**
```
DESIGN_GOAL: Gradient copper facade
EXECUTION:
1. Place copper blocks
2. Wait for full oxidation (or accelerate via commands)
3. Scrape sections to desired stages
4. Wax final state
5. Repeat for each gradient zone
```

---

> **CREW TALK #2**
>
> **Shift Supervisor:** "The client wants that green gradient on the cathedral roof. How's it coming?"
>
> **Apprentice Glazier:** "It's... uh... taking a while. The blocks are still orange."
>
> **Shift Supervisor:** "You're waiting for natural oxidation? On a job timeline?"
>
> **Apprentice Glazier:** "Well, yeah, that's how it works..."
>
> **Shift Supervisor:** *facepalms* "Kid, we've got Steve agents with command blocks. Use `/tick freeze` or setup a chunk loader with bonemeal-style aging hacks. Better yet—PRE-AGED SUPPLY. I've got three shulker boxes of pre-oxidized blocks from the last server reset. Match the gradient, WAX IT IN PLACE, and we're done by lunch. Natural aging is for survival builders—we're PROFESSIONALS."
>
> **Apprentice Glazier:** "There's a box of weathered copper in the supply chest?"
>
> **Shift Supervisor:** "Two boxes. oxidized AND exposed. We did the prep work last month. GRAB THE WAX."

---

## MODULE 4: LIGHTNING RODS

### The Critical Safety Component

Lightning rods are your base's first line of defense against electrical storms. One poorly placed strike can decimate flammable constructions and terrorize villagers.

**Crafting Recipe:**
```
[Copper Block] × 3
    ↓
[Lightning Rod] × 3
```
**Placement: 3 blocks vertically in center column of crafting grid**

### Technical Specifications

| Attribute | Value |
|-----------|-------|
| **Height** | 1 block |
| **Width** | 1 block (16×16 pixel footprint) |
| **Conduction Range** | 128 blocks (radius) in loaded chunks |
| **Strike Priority** | Highest point in conduction zone |
| **Blast Protection** | Diverted to rod (no block damage) |
| **Redstone Output** | Strong signal when struck |

### Installation Protocols

**Optimal Placement Logic:**

1. **Height Above Protected Area:**
   - Minimum: 1 block above highest flammable block
   - Recommended: 5-10 blocks above roofline
   - Calculate: `rod_y - max_flammable_y > 1`

2. **Coverage Area:**
   - One rod protects ~256×256 block area (128 radius)
   - For large builds: Grid pattern with overlapping coverage
   - Formula: `ceil(build_width / 256)` rods per row

3. **Agent Integration:**
```java
"LIGHTNING_PROTECTION" -> {
    "structure_bounds": "auto_detect",
    "rod_spacing": 200,  // Within 128 coverage
    "placement_height": "roof_y + 8",
    "material": "lightning_rod",
    "priority": "CRITICAL"
}
```

### Redstone Applications

**Signal Characteristics:**
- Emits strong redstone signal when struck
- Signal duration: Brief flash (~2-3 ticks)
- Pulse length: Sufficient for trigger-based systems

**Automated Response Systems:**
```
LIGHTNING STRIKE → Rod activation → Redstone pulse →
    ├─ Alert system (note blocks/alarms)
    ├─ Logging system (write to sign/narrator)
    ├─ Auxiliary systems unlock (emergency doors)
    └─ Data collection (strike counter)
```

---

> **CREW TALK #3**
>
> **Old Timer:** "Hey new guy, see that forest fire over the ridge? Bet you anything someone built a wooden roof without lightning protection."
>
> **New Guy:** "Should we... go help?"
>
> **Old Timer:** "Can't. Fire's in an unloaded chunk. That's the thing about lightning—your rod only works if you're THERE to attract it. Unloaded terrain? It's the Wild West of electrical storms."
>
> **New Guy:** "So how do we protect our outpost if we're away?"
>
> **Old Timer:** "Chunk loaders, kid. We anchor the chunks. The rod works, the base stays safe, and we come home to intact buildings instead of a crater. But here's the real secret—you put rods on EVERY peak, not just the highest one. Lightning's finicky. Give it OPTIONS and it'll pick your rod instead of your flammable roof. Redundancy isn't just for databases—it's for survival."
>
> **New Guy:** "How many rods on the main base?"
>
> **Old Timer:** "Twenty-seven. And we've taken zero structural hits in three years. You do the math."

---

## MODULE 5: SPYGASSES

### Precision Observation Tool

The spyglass revolutionized long-range observation for reconnaissance and navigation. Before its introduction, crew members had to craft observer towers or use spectator mode for detailed inspection.

**Crafting Recipe:**
```
[Copper Ingot] × 2
[Amethyst Shard] × 1
    ↓
[Spyglass] × 1
```
**Pattern:** Vertical line—Copper Ingot on top and bottom, Amethyst in center

### Technical Specifications

| Attribute | Value |
|-----------|-------|
| **Zoom Magnification** | ~8× (FOV reduction from default 70 to ~8.75) |
| **Animation Duration** | ~1 second (raise to eye) |
| **Movement Speed** | Reduced to sneaking speed while in use |
| **Duration** | Indefinite (until item unequipped) |
| **Stackable** | No (damage value system) |
| **Repairable** | Yes (with copper ingots in anvil) |

### Operational Protocols

**Usage Method:**
1. Equip spyglass in hotbar or off-hand
2. Hold right-click (or use hotkey)
3. Movement continues at reduced speed (sneaking behavior)
4. Release to stop viewing

**Agent Context Enhancement:**
```java
"SPYGLASS_RECON" -> {
    "objective": "distant_inspection",
    "action": "equip_and_use",
    "scan_pattern": "pan_left_to_right",
    "observation_duration": 100,  // ticks
    "data_collection": {
        "block_identification": true,
        "entity_detection": true,
        "coordinate_logging": true
    }
}
```

### Strategic Applications

**Use Case:**
- **Navigation:** Identify landmarks before committing to travel
- **Resource Spotting:** Confirm ore visibility from distance
- **Security:** Scan perimeter without approaching threats
- **Construction:** Verify alignment of distant build components
- **Documentation:** Capture screenshots for client approval

---

## MODULE 6: BRUSH USAGE

### Archaeology and Suspicious Blocks

The brush introduced copper into the archaeological toolkit, enabling recovery of items from suspicious sand and gravel without damage to artifacts.

**Crafting Recipe:**
```
[Copper Ingot] × 1
[Stick] × 1
    ↓
[Brush] × 1
```
**Pattern:** Horizontal—Copper Ingot left, Stick right

### Archaeology Sites

**Suspicious Block Locations:**

| Structure | Suspicious Block Type | Notable Loot |
|-----------|----------------------|--------------|
| **Desert Temple** | Suspicious Sand | TNT, diamonds, emeralds, gold |
| **Desert Well** | Suspicious Sand | Music discs, pottery shards |
| **Ocean Ruins (Warm)** | Suspicious Sand | Sniffer eggs, tools |
| **Ocean Ruins (Cold)** | Suspicious Gravel | Pottery shards, armor trims |
| **Trail Ruins** | Suspicious Gravel | Pottery sherds, banners, emeralds |

### Brushing Technique

**Correct Procedure:**
1. Identify suspicious block (slightly different texture)
2. Hold right-click with brush equipped
3. Maintain brush position through particle animation
4. Block breaks when brushing complete
5. Item drops (no experience)

**Important Notes:**
- Breaking suspicious block with any tool DESTROYS loot
- Brushing takes ~2-3 seconds per block
- Multiple items can drop from single block
- No tool enchantment affects brushing speed

**Agent Implementation:**
```java
"ARCHAEOLOGY_DIG" -> {
    "target_block": "suspicious_sand",
    "tool": "brush",
    "action": "hold_right_click",
    "avoid_alternative_break": true,  // CRITICAL
    "loot_collection": "auto_pickup",
    "site_mapping": true  // Track all suspicious blocks in structure
}
```

---

> **CREW TALK #4**
>
> **Site Foreman:** "Who broke the suspicious sand in the desert temple?"
>
> *Crickets in the comms channel*
>
> **Site Foreman:** "I'm checking the block logs. Someone used a diamond shovel. On a BRUSHING site. We just lost a music disc, a diamond, and three pottery sherds."
>
> **Nervous Apprentice:** "I... I thought it was regular sand? It looked the same from a distance."
>
> **Site Foreman:** "FROM A DISTANCE. Kid, rule number one of archaeology ops: IF YOU'RE NOT SURE, YOU BRUSH. The only block that should ever break in a ruin is gravel—and even then, only after you've brushed everything suspicious. We've got Steve agents with image recognition now. You could've ASKED the AI to flag suspicious blocks before you started swinging."
>
> **Nervous Apprentice:** "Sorry. It won't happen again."
>
> **Site Foreman:** "It better not. That disc was 'otherside'—rare drop. You just cost the crew 15 diamonds in resale value. Next time, let the reconnaissance agent do the scanning FIRST. That's what we built it for."

---

## MODULE 7: COPPER BUILDS

### Aesthetic Applications

The unique property of copper oxidation enables architects to create living, evolving structures. This isn't just about mechanics—it's about artistry.

### Color Palette Integration

**Oxidation Spectrum:**
```
Unoxidized (Orange-Red)  → Warm, energetic, fresh builds
Exposed (Brownish)       → Aged, rustic, traditional themes
Weathered (Green-Brown)  → Transitional, weathered looks
Oxidized (Teal-Green)    → Ancient, natural, overgrown aesthetics
```

### Architectural Patterns

**1. The Aging Temple:**
- Structure: Ancient ruins or religious building
- Technique: Mixed oxidation stages with mossy cobblestone
- Effect: Appears centuries old
- Agent pattern: Randomized block placement with weighted distribution

**2. Industrial Steamworks:**
- Structure: Factory, machinery, Victorian-era tech
- Technique: Unoxidized and exposed copper with iron
- Effect: Maintained, operational appearance
- Redstone integration: Exposed copper conduits for "pipes"

**3. Natural Integration:**
- Structure: Buildings merged with terrain
- Technique: Oxidized copper matching terrain, waxed in place
- Effect: Organic, grown-into-environment appearance
- Biome synergy: Matches swamp, jungle, and lush cave aesthetics

### Advanced Lighting Techniques

**Copper Bulb Applications:**
- **Toggle Mechanism:** Redstone pulse toggles on/off
- **Light Level:** Standard glowstone-equivalent
- **Aesthetic:** Industrial lighting fixtures
- **Agent Logic:**
```java
"COPPER_LIGHTING" -> {
    "fixture_type": "copper_bulb",
    "placement": "ceiling_mounted",
    "oxidation_stage": "exposed",  // Aged industrial look
    "wax": true,  // Preserve stage
    "redstone_control": "automatic",
    "trigger": "player_proximity"
}
```

---

## MODULE 8: TEAM COPPER

### Multi-Agent Coordination Patterns

When multiple Steve agents are deployed for copper operations, efficient coordination becomes critical. The MineWright framework provides several proven patterns.

### Spatial Partitioning

**Branch Mining Coordination:**
```
AGENT_1: Y=48, X=-100, Branch interval 4 blocks
AGENT_2: Y=52, X=-100, Branch interval 4 blocks
AGENT_3: Y=56, X=-100, Branch interval 4 blocks
AGENT_4: Y=60, X=-100, Branch interval 4 blocks

Result: Parallel coverage of peak copper range
```

**Collision Avoidance:**
- Each agent claims a branch before mining
- Minimum separation: 8 blocks horizontally
- Vertical separation: 4 blocks minimum
- Shared inventory access via central chest network

### Resource Collection Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│                    COPPER OPS FLOW                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  [MINING AGENTS]           [PROCESSING AGENT]               │
│       │                            │                        │
│  Raw Copper Ore        →   Smelting Furnace                │
│       │                            │                        │
│       │                      Copper Ingot                   │
│       │                            │                        │
│       │                    [STORAGE AGENT]                  │
│       │                            │                        │
│       └────────────────────→ Block Compression              │
│                                    │                        │
│                            [DISTRIBUTION]                   │
│                                    │                        │
│       ┌─────────────┬──────────────┼──────────────┐        │
│       ▼             ▼              ▼              ▼        │
│  [BUILD TEAM]  [REDSTONE]   [ARCHAEOLOGY]  [LIGHTING]     │
│       │             │              │              │        │
│  Construction  Component     Brush Supply   Bulb Install   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Chat Command Integration

**Player Leadership Commands:**
```
/steve spawn "Miner1" → "Mine copper at Y=48, 5x5 grid"
/steve spawn "Miner2" → "Mine copper at Y=52, 5x5 grid"
/steve spawn "Smelter" → "Auto-smelt all raw copper"
/steve spawn "Builder" → "Build copper roof using weathered blocks"
```

**Agent Autonomous Commands:**
```java
// Task Planner input
"Establish copper supply chain for cathedral roof construction"

// Auto-generated action sequence:
1. Spawn 3 mining agents at optimal Y-levels
2. Establish central smelting station
3. Route ingots to compression center
4. Pre-age blocks for aesthetic requirements
5. Coordinate placement with architecture agent
```

---

> **CREW TALK #5**
>
> **Logistics Coordinator:** "We've got a cathedral roof going in at spawn, a lightning grid for the wooden village outpost, AND an archaeology expedition to the desert temple. I've got five Steve agents and three human crew members. How do we prioritize?"
>
> **Crew Lead:** "Cathedral first—it's the client's showpiece. Two Steves on mining, one on smelting and compression. Human crew on placement and waxing the gradient."
>
> **Logistics Coordinator:** "What about the village? There's a storm cycle in 20 minutes."
>
> **Crew Lead:** "One Steve agent. Pre-fabricated lightning rods. Auto-placement routine. The agents can handle it—it's a grid pattern. Have the third Steve carry supplies and follow the placement algorithm."
>
> **Logistics Coordinator:** "And the archaeology?"
>
> **Crew Lead:** "Human crew. Archaeology needs judgment calls—brushing vs. natural gravel, loot prioritization, site documentation. Send our rookie with a brush and a supervisor. We don't want another 'diamond shovel' incident. Besides, the experience'll do them good."
>
> **Logistics Coordinator:** "So: Steves on mining, smelting, and lightning grid. Humans on cathedral roof and archaeology."
>
> **Crew Lead:** "Exactly. Play to the strengths. AI for predictable patterns, humans for adaptive judgment. Now get moving—that storm isn't waiting."

---

## MODULE 9: AUTOMATION POTENTIAL

### Large-Scale Copper Operations

For sustained construction projects or resource farming, automation becomes essential. The MineWright framework supports several proven architectures.

### Automated Mining Systems

**Dripstone Cave Copper Farms:**
- **Location:** Large dripstone cave systems with exposed copper
- **Mechanic:** World generation places ore on cave ceilings
- **Automation:** Agent-controlled collection with water streams
- **Yield:** High (exposed ore, no digging required)

**Design Pattern:**
```
1. Identify dripstone cave with copper ceiling
2. Build collection channel at floor level
3. Water stream carries mined blocks to central point
4. Agent mines ceiling systematically
5. Hopper minecart or agent collects from channel
```

### Dripstone Lava Casting

**Infinite Copper Generation:**
1. **Pointed Dripstone:** Place above cauldron
2. **Lava Source:** Position dripstone over lava
3. **Waiting:** Dripstone occasionally drips lava into cauldron
4. **Harvest:** Agent collects lava from cauldron
5. **Processing:** Lava → Stone → Mining (inefficient, not recommended)

**Note:** This method is slower than direct mining and primarily used for scientific interest rather than practical operations.

### Lightning Farm Integration

**Automated Rod Farming:**
```
┌───────────────────────────────────────────────────────────┐
│              LIGHTNING COPPER FARM DESIGN                 │
├───────────────────────────────────────────────────────────┤
│                                                           │
│     [Rod Grid] ← 128-block radius coverage overlap        │
│           ↓                                               │
│    [Redstone Pulse] ← Triggered by lightning strike       │
│           ↓                                               │
│     [Counter System] ← Log strikes for data              │
│           ↓                                               │
│    [Optional Converter] ← Zap-powered mob farms           │
│                                                           │
└───────────────────────────────────────────────────────────┘
```

**Agent Maintenance:**
- Rod durability check (infinite, but verify placement)
- Redstone pathway verification
- Chunk loading status monitoring
- Storm cycle prediction for resource planning

---

## QUICK REFERENCE CARD

**Copper Ore:**
- Y-level: -16 to 112, peak at 48
- Mining: Stone pickaxe minimum
- Fortune III recommended

**Block Compression:**
- 9 Ingots = 1 Block
- 1 Block = 9 Ingots
- Always compress for transport

**Oxidation Timeline:**
- Unoxidized → Exposed → Weathered → Oxidized
- Wax to preserve stage
- Axe to reverse stage

**Lightning Rod:**
- 128-block protection radius
- Place above flammable structures
- Emits redstone pulse when struck

**Spyglass:**
- Craft: 2 Copper + 1 Amethyst
- 8× zoom magnification
- Enables reconnaissance

**Brush:**
- Craft: 1 Copper + 1 Stick
- Excavates suspicious sand/gravel
- Never break suspicious blocks directly

---

## FINAL CHECKOUT EXAM

Before certification in Copper Operations, crew members must demonstrate competence in:

1. **Ore Identification:** Locate copper ore in loaded chunks
2. **Efficient Mining:** Execute branch mine pattern at optimal Y-level
3. **Block Processing:** Smelt, compress, and cut copper appropriately
4. **Oxidation Management:** Wax and scrape blocks to achieve target appearance
5. **Lightning Protection:** Design and install rod grid for structures
6. **Tool Usage:** Demonstrate proper spyglass and brush techniques
7. **Team Coordination:** Collaborate with agents and crew on multi-stage projects
8. **Automation Awareness:** Identify opportunities for automated systems

---

## APPENDIX: AGENT CODE REFERENCE

**Action Registry Snippets:**

```java
// Mining action
ActionRegistry.register("mine_copper", (steve, task, ctx) ->
    new BranchMineAction(steve, task, Material.COPPER_ORE, 48, 64));

// Processing action
ActionRegistry.register("process_copper", (steve, task, ctx) ->
    new SmeltAndCompressAction(steve, task, Material.COPPER_INGOT));

// Oxidation action
ActionRegistry.register("age_copper", (steve, task, ctx) ->
    new ApplyOxidationAction(steve, task, targetStage));

// Lightning protection
ActionRegistry.register("install_lightning_protection", (steve, task, ctx) ->
    new LightningGridAction(steve, task, detectBounds()));
```

---

**End of Manual**

**For questions or clarifications, consult your shift supervisor or query the MineWright knowledge base via in-game documentation terminal.**

*This manual is living documentation. Update as new copper applications are discovered.*

---

**MineWright Construction Division**
*Building the Future, One Block at a Time*
*Since Minecraft Alpha*