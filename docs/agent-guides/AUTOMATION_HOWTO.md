# AUTOMATION HANDBOOK
## MineWright Crew Training Manual - Volume 7

**Foreman's Note:** "Listen up, crew. Automation's what separates the apprentices from the master builders. Anyone can place blocks by hand - takes a real craftsman to build a system that places 'em for you. Read this manual, practice the builds, and you'll be running operations that make the solo builders scratch their heads."

---

## TABLE OF CONTENTS

1. [Why Automate: The Foreman's Case](#why-automate)
2. [Auto-Smelters: Foundation of Production](#auto-smelters)
3. [Auto-Farms: The Harvest Pipeline](#auto-farms)
4. [Item Sorting: Warehouse Logic](#item-sorting)
5. [Storage Systems: The Supply Chain](#storage-systems)
6. [Super Smelters: Heavy Industry](#super-smelters)
7. [Villager Workstations: Trade Automation](#villager-workstations)
8. [Redstone Clocks: Timing Everything](#redstone-clocks)
9. [When NOT to Automate: Knowing Your Limits](#when-not-to-automate)
10. [Crew Talk: Field Dialogues](#crew-talk)

---

## WHY AUTOMATE

### The Time Savings Equation

**Manual vs. Automated Production:**

| Task | Manual (per hour) | Automated (per hour) | ROI Time |
|------|-------------------|----------------------|----------|
| Smelting cobblestone | 480 stacks | 2,400+ stacks | 2 hours |
| Harvesting wheat | 120 stacks | 600+ stacks | 3 hours |
| Sorting items | 40 stacks | 800+ stacks | 1 hour |
| Tree farming | 80 stacks | 400+ stacks | 2 hours |

**Crew Wisdom:** "Every hour you spend building automation saves you ten hours of manual labor. Do the math - would you rather harvest wheat for a week, or spend a day building a farm that does it forever?"

### Consistency: The Quality Advantage

Manual work varies based on:
- Fatigue levels
- Attention span
- inventory management
- Chunk loading issues

Automated systems deliver:
- Predictable output rates
- Consistent quality
- 24/7 operation capability
- Zero fatigue errors

**The Reliability Factor:** "A tired crew member misses a harvest. A well-built hopper system never sleeps. That's the difference between 'good enough' and 'professional grade'."

### Scaling: Multi-Crew Operations

When you've got multiple Steves working on a project:
- **Manual:** Each worker adds linear output
- **Automated:** Shared systems multiply everyone's effectiveness

**Crew Coordination Example:**
- 3 manual workers: 3x output
- 3 workers + auto-smelter: 15x output (smelter handles 5 workers each)
- 3 workers + sorting system: 30x output (no inventory management time)

**Foreman's Rule:** "Build automation before you hire more crew. One good system beats ten extra bodies any day."

---

## AUTO-SMELTERS

### The Standard Design

**Components Required:**
- 1 Furnace (the smelter)
- 1 Hopper above (input buffer)
- 1 Hopper below (output collection)
- 1 Chest (final storage)

**Build Sequence:**
1. Place chest on ground
2. Place hopper pointing into chest
3. Place furnace on top of hopper
4. Place hopper on top of furnace
5. Fill hopper with fuel (coal/charcoal)

**Throughput Calculation:**
- Furnace speed: 10 seconds per smelt
- Hopper transfer: 8 ticks (0.4 seconds)
- Max theoretical: 360 items/hour
- Practical limit: 300 items/hour (chunk loading considerations)

**Fuel Efficiency Matrix:**

| Fuel Type | Items per Unit | Burn Time | Efficiency |
|-----------|----------------|-----------|------------|
| Coal/Charcoal | 8 items | 80 seconds | Standard |
| Coal Block | 80 items | 800 seconds | 10% bonus |
| Lava Bucket | 100 items | 1000 seconds | Portable |
| Dried Kelp | 4 items | 40 seconds | Renewable |
| Blaze Rod | 12 items | 120 seconds | Nether-only |

**Crew Tip:** "Charcoal's your friend. Build a tree farm, burn the logs in a manual furnace, feed the charcoal to your auto-smelter. Zero fuel cost, infinite smelting."

### Multi-Furnace Banks

**The 4-Bank Layout:**
```
[Hopper] [Hopper] [Hopper] [Hopper]
[Furnace][Furnace][Furnace][Furnace]
[Hopper] [Hopper] [Hopper] [Hopper]
[ Chest ] [ Chest ] [ Chest ] [ Chest ]
```

**Advantages:**
- 4x throughput (1,200 items/hour)
- Centralized fueling (hoppers connect sideways)
- Parallel processing (different materials simultaneously)

**Fuel Distribution Hack:**
Place hoppers between furnaces pointing sideways:
```
[Hopper] → [Furnace] → [Hopper] → [Furnace]
   ↓           ↓           ↓           ↓
[Chest ]   [Chest ]   [Chest ]   [Chest ]
```

**Crew Wisdom:** "Don't chain more than 4 furnaces in a row. Hopper updates get weird after that. Keep your banks small, build more banks instead of longer ones."

### The Auto-Smelter Feeder System

**Automatic Ore Processing:**
```
[Mining Output] → [Hopper Filter] → [Ore Chest]
                                         ↓
                                    [Smelter Bank]
                                         ↓
                                    [Ingot Chest]
```

**Hopper Filter Setup:**
1. Place hopper under mining output
2. Put 1 of each ore type in hopper slots
3. Leave 1 slot empty for junk
4. Hopper sorts: ores go down, junk stays put

**Throughput Optimization:**
- Don't overfill filters (keeps 1 item slot open)
- Use overflow chests for non-ore items
- Test with a stack before committing to full automation

**Field Note:** "I spent three hours fixing a jammed filter because I filled all the slots. Rule of thumb: always leave at least one empty slot in sorting hoppers. The flow needs room to breathe."

---

## AUTO-FARMS

### Crop Farms: The Water Canal Method

**Basic Wheat Farm Design:**

**Layout (9x9):**
```
[W] = Water, [C] = Crops, [D] = Dirt, [H] = Hoppers

[D][C][C][C][C][C][C][D][D]
[D][C][C][C][C][C][C][D][D]
[D][C][C][C][C][C][C][D][D]
[D][C][C][W][W][C][C][D][D]
[D][C][C][W][W][C][C][D][D]
[D][C][C][C][C][C][C][D][D]
[D][C][C][C][C][C][C][D][D]
[H][H][H][H][H][H][H][H][H]
[C][C][C][C][C][C][C][C][C]
```

**Key Specifications:**
- Water hydrates 4 blocks in all directions
- Crops must be within 4 blocks of water
- Hoppers collect drops without breaking crops
- Observer + piston breaker for automated harvest

**Automated Harvest Circuit:**
1. Observer watches crops grow
2. When fully grown, sends redstone pulse
3. Piston breaks crop row
4. Hoppers collect drops
5. Water flows items to collection point

**Yield Calculations:**
- Single crop: 1-3 wheat + 0-3 seeds per harvest
- 80-crop farm: 80-240 wheat per harvest cycle
- Growth time: 31-68 minutes (random, average 50)
- Hourly yield: ~100-200 wheat

**Crew Optimization:** "Plant in rows, not patches. Makes the piston harvesters simpler. One pulse clears an entire row instead of knocking your head against a checkerboard pattern."

### Tree Farms: The Chamber Method

**Simple Tree Farm (4x4 internal):**

**Construction:**
1. Build 4x4 room, 8 blocks high
2. Place dirt floor (4x4)
3. Plant saplings in center (2x2)
4. Leave 1 block air gap above saplings
5. Place glass ceiling (containment)
6. Add observer at ceiling height
7. Place piston breaker mechanism

**Automation Flow:**
```
[Saplings Grow] → [Observer Detects] → [Piston Breaks]
                                    ↓
                            [Hopper Collection]
                                    ↓
                            [Sapling Sorter]
                                    ↓
                    [Saplings replant via dispenser]
```

**Replanting System:**
1. Hoppers collect drops (logs + saplings)
2. Item sorter separates saplings from logs
3. Saplings go to dispenser beneath dirt
4. Dispenser plants new saplings on pulse
5. Logs go to storage/smelter

**Species Considerations:**
- **Oak:** Fast, smallest, good for beginners
- **Birch:** Compact, consistent height
- **Spruce:** Massive, requires headroom
- **Acacia:** Irregular shape, avoid for automation
- **Dark Oak:** 2x2 planting only, complex breaker
- **Jungle:** Huge, complex branching

**Yield by Species:**
- Oak: 4-6 logs per tree
- Birch: 5-7 logs per tree
- Spruce: 8-15 logs per tree
- Jungle: 20-50 logs per tree (space-intensive)

**Crew Warning:** "Jungle trees look impressive until you spend four hours building a breaker that reaches all the branches. Stick to birch for your first auto-farm - predictable height means simple mechanics."

### Sugar Cane: The Tick-Based Harvester

**Flying Machine Design:**

**Track Layout (long axis):**
```
[       Sugar Cane Row       ]
[Powered Rail][Regular Rail]x
[       Minecart with        ]
[      Hopper/Dispenser      ]
```

**Harvesting Mechanism:**
1. Observer detects fully grown cane (height 3)
2. Activates flying machine circuit
3. Machine breaks cane, collects drops
4. Returns to start position
5. Cycle repeats

**Alternative: Piston Harvest**
```
[Observer] → [Piston] → [Sugar Cane]
              ↓
         [Water Collection]
              ↓
          [Hopper Chain]
```

**Pros and Cons:**

| Method | Speed | Complexity | Reliability |
|--------|-------|------------|-------------|
| Flying Machine | Fast | High | Medium |
| Piston Harvest | Medium | Low | High |
| Manual | Slow | Minimal | 100% |

**Throughput Numbers:**
- Growth time: 8-16 minutes (random)
- Average yield: 1-2 cane per harvest
- Recommended: 20-plant minimum for meaningful automation

**Crew Tip:** "Sugar cane's all about the water setup. One block of water hydrates 4 cane in a straight line. Build 'em in rows off a central canal - saves water blocks and keeps the harvesting clean."

### Bamboo: The Explosive Producer

**Why Bamboo's Special:**
- Growth rate: 1-3 blocks per minute (fastest crop)
- Height potential: Up to 12-16 blocks
- Smelting fuel: 0.25 items per bamboo (4x better than logs)
- Crafting resource: Scaffolding, sticks

**High-Density Farm Design:**

**Growing Chamber (9x9 floor):**
1. Fill floor with dirt/podzol
2. Plant bamboo in grid pattern (every other block)
3. Glass ceiling at height 16
4. Observer at height 15
5. Flying machine breaker at ceiling

**Collection System:**
```
[Bamboo Breaks] → [Item Flow]
                     ↓
               [Water Canal]
                     ↓
               [Hopper Line]
                     ↓
            [Sorting/Storage]
```

**Fuel Conversion Pipeline:**
1. Bamboo → Smelter (auto)
2. Smelted bamboo → Bamboo Blocks
3. Bamboo Blocks → More smelting (25% fuel bonus)
4. Excess → Crafting tables, sticks, scaffolding

**Yield Reality:**
- 40-plant farm: 1-3 stacks/hour
- Fuel equivalent: 4-12 coal/hour
- Self-sustaining: Use 25% of output to fuel the smelter

**Crew Bragging Rights:** "Bamboo's the only crop that pays for its own processing. Build a 40-plant farm, feed a quarter back to the smelter, and you've got infinite fuel forever. That's real automation - zero input, infinite output."

---

## ITEM SORTING

### Hopper Filter Fundamentals

**The Sorting Mechanism:**

Hoppers check items from top to bottom, left to right:
1. Item matches filter slot → goes into that slot
2. No match → moves to next hopper/container
3. All slots full → stops moving (jam!)

**Basic Sorter Template:**
```
           [Input Chest]
               ↓
          [Sorting Hopper]
          /  |   |   |   \
    [Item][Item][Item][Item][Overflow]
     Chest Chest Chest Chest  Chest
```

**Filter Setup Procedure:**
1. Place hopper under input
2. Put 1 example item in each filter slot
3. Leave at least 1 slot empty
4. Connect output chests to sides
5. Test with mixed items before relying on it

**Filter Stacking Hack:**
Put 1 item in each of 4 slots → sorts up to 4 items/tick
Put stacks (64) in each slot → same sorting, longer before refill

**Crew Warning:** "Never fill all 5 slots in a sorting hopper. Always leave one empty or you'll wonder why the flow stopped. Jams are the silent killer of automation systems."

### Multi-Layer Sorting Systems

**The Funnel Architecture:**

**Layer 1: Category Sort**
```
[Mixed Input] → [Hopper Line]
                   ↓
    ┌──────────────┴──────────────┐
    ↓         ↓         ↓         ↓
[Ores]    [Wood]    [Food]   [Misc]
```

**Layer 2: Item-Specific Sort**
```
[Ores] → [Hopper Chain]
            ↓
    ┌───────┴─────────┐
    ↓       ↓         ↓
[Iron][Gold][Copper][Coal]
```

**Throughput Optimization:**
- Single hopper: 2.5 items/second
- 5-hopper line: 12.5 items/second
- Parallel lines: Multiply accordingly

**Overflow Protection:**
```
[Main Sorter] → [Filtered Items]
                  ↓
           [Overflow Detector]
                  ↓
         [Backup Storage Chest]
                  ↓
            [Alarm System]
```

**Redstone Overflow Alarm:**
1. Comparator on backup storage
2. Detects items present
3. Activates note block/redstone lamp
4. Crew gets notified before system jams

**Field Story:** "We lost 20 stacks of iron because an overflow chest filled up and jammed the whole sorter. Now every system has an alarm and three tiers of overflow protection. Cheap insurance for expensive materials."

### Diamond-Piping Complex Sorts

**When Hoppers Aren't Enough:**

Hopper limitations:
- 5 filter slots max
- Slow transfer rate
- Can't route based on NBT data

**Diamond Pipe Alternatives:**
- Modded pipes (if available)
- Minecart with hopper routing
- Shulker box buffering systems

**Minecart Routing Method:**
```
[Mixed Items] → [Hopper] → [Minecart]
                           ↓
                     [Detector Rail]
                      /    |    \
                [Route A][Route B][Route C]
```

**Crew Philosophy:** "Work with vanilla mechanics before reaching for complex routing. A well-designed hopper system beats a complicated pipe network every time. Simple breaks less, and when it does break, anyone can fix it."

---

## STORAGE SYSTEMS

### The Silo Concept

**Vertical Storage Design:**

**Standard Silo (Double Chests):**
```
[Chest] ←→ [Chest] ←→ [Chest] ←→ [Chest]
   ↓         ↓         ↓         ↓
[Hopper] [Hopper] [Hopper] [Hopper]
   ↓         ↓         ↓         ↓
         [Central Collection]
```

**Chest Connection Rules:**
- Large chests merge (2 blocks)
- Max 54 slots per double chest
- Up to 27 stacks of 64 items = 1,728 items per chest
- Always build in pairs for maximum density

**Storage Density Math:**
- Single chest volume: 1m³
- Double chest volume: 2m³
- Items stored: 1,728 per double chest
- Density: 864 items/m³

**Shulker Box Storage:**
- Shulker box: 27 slots
- Inside chest: 27 shulker boxes
- Total storage: 729 slots × 64 = 46,656 items
- Density: 23,328 items/m³ (27x better!)

**Crew Recommendation:** "Start with regular chests for common materials. Upgrade to shulker box storage only for high-volume, high-value items. The setup time isn't worth it for dirt and cobblestone."

### Retrieval Systems

**Manual Retrieval (Chest Access):**
- Simple, reliable
- Crew walks to chest
- Limited by walking time
- Good for low-frequency access

**Hopper Feed Systems:**
```
[Storage] → [Hopper] → [Item Pipe] → [Workstation]
```

**Automated Feeder Design:**
1. Hopper pulls from storage chest
2. Feeds item into dropper/dispenser
3. Redstone clock activates dropper
4. Items feed into crafting system
5. Excess returns to storage

**Minecart Retrieval:**
```
[Storage Silo] → [Hopper] → [Minecart] → [Processing]
                                 ↑
                         [Redstone Clock]
```

**Pros and Cons:**

| Method | Speed | Complexity | Range |
|--------|-------|------------|-------|
| Manual | Slow | Low | Walking distance |
| Hopper | Medium | Medium | 1 block radius |
| Minecart | Fast | High | Unlimited with rails |

**Crew Wisdom:** "The fastest retrieval system is the one you don't have to build. Put your storage next to where you use the materials. Site planning beats mechanical complexity every time."

### Organization Protocols

**The Category System:**

**Zone 1: Raw Materials**
- Ores (unsmelted)
- Raw wood (unplanked)
- Crops (uncrafted)

**Zone 2: Processed Materials**
- Ingots (smelted)
- Planks (crafted)
- Food items (prepared)

**Zone 3: Building Blocks**
- Stone variants
- Wood variants
- Manufactured blocks (terracotta, concrete, etc.)

**Zone 4: Specialized Items**
- Redstone components
- Enchanted gear
- Rare drops

**Labeling System:**
1. Item frames on chest fronts
2. Signs above chest rows
3. Glow item frames for high-visibility
4. Color-coded wool/terracotta backgrounds

**Inventory Management Rules:**
1. Never mix categories in a chest
2. Keep 1 empty slot per chest (overflow buffer)
3. Stack items before storing (hopper jam prevention)
4. Never store garbage (lava item disposal nearby)

**Field Note:** "I spent two hours looking for redstone because someone put it in the 'building blocks' zone instead of 'specialized items.' Save yourself the headache - label everything, and discipline the crew to follow the system. A disorganized storage system is worse than no storage at all."

---

## SUPER SMELTERS

### The Nether Portal Design

**Super Smelter Concept:**
Multiple furnaces fed by synchronized hopper systems, processing thousands of items per hour.

**Basic Requirements:**
- 16-64 furnaces (typical: 32)
- Centralized fuel distribution
- Input buffer system
- Output collection system
- Compact design (portal builds)

**The 32-Furnace Classic:**

**Layout (16x2 furnace bank):**
```
[Input Chest] [Input Chest]
     ↓             ↓
[Hopper Line] [Hopper Line]
     ↓             ↓
[Furnace][Furnace]...x16
[Furnace][Furnace]...x16
     ↓             ↓
[Hopper Line] [Hopper Line]
     ↓             ↓
[Output][Output][Output][Output]
```

**Fuel Distribution:**
```
[Fuel Chest] → [Central Hopper]
                   ↓
        [Side Hoppers to Furnaces]
```

**Throughput Calculations:**
- 32 furnaces × 300 items/hour = 9,600 items/hour
- Practical limit: ~7,000 items/hour (hopper speed limits)
- Time to smelt 64 stacks: ~45 minutes

**Build Requirements:**
- 32 furnaces
- 96 hoppers (32 input, 32 fuel, 32 output)
- 4 double chests (input, fuel, output × 2)
- Building materials: ~1 stack stone, 1 stack glass

**Crew Achievement:** "Built my first super smelter in a weekend. Spent the next month smelting everything I could find. Three weeks later, I had more iron than I'd ever use. That's when I learned: automation creates its own problems - now I had to automate storage too."

### The Portal Smelter Variation

**Why Use Nether Portals:**
- Instant item transport (portal mechanics)
- Separation of processing and storage
- Compact nether builds
- Aesthetic appeal

**Portal Transfer System:**

**Overworld Side:**
```
[Super Smelter] → [Output Chest]
                      ↓
                 [Nether Portal]
```

**Nether Side:**
```
          [Nether Portal]
                    ↓
            [Collection Hopper]
                    ↓
             [Storage System]
```

**Item Transport Physics:**
- Items thrown through portal teleport instantly
- 5-second cooldown per portal
- Works both directions
- Minecarts transport items too

**Throughput Considerations:**
- Portal cooldown: 5 seconds
- Items per trip: Unlimited (if using minecarts)
- Effective rate: 12 trips/minute per portal
- Recommendation: 2-4 portals for high-volume systems

**Crew Warning:** "Portal smelters look cool until you realize you're crossing dimensions every time you need fuel. Build your fuel system on both sides or you'll spend more time traveling than smelting."

### Fuel Management for Super Smelters

**Fuel Consumption Math:**
- 32 furnaces × 1 item/10 seconds = 192 items/minute
- Coal (8 items/fuel): 24 coal/minute
- Charcoal: 24 charcoal/minute
- Lava bucket (100 items/fuel): 1.92 buckets/minute

**Charcoal Production System:**
```
[Tree Farm] → [Log Storage] → [Charcoal Smelter]
                                        ↓
                                  [Charcoal Storage]
                                        ↓
                                  [Super Smelter Fuel]
```

**Sustainability Requirements:**
- 32 furnaces need ~2,000 charcoal/hour
- 1 log = 4 planks = 8 charcoal (smelted planks)
- Need ~250 logs/hour to fuel smelter
- 4-tree farm produces ~200 logs/hour
- Solution: 8-tree farm + charcoal log feeding

**Lava Automation:**
```
[Nether Portal] → [Lava Collection]
                       ↓
                [Lava Transport]
                       ↓
                [Super Smelter]
```

**Lava Sources:**
- Point drain farms (limited in 1.20+)
- Guardian farms (iron farm byproduct)
- Nether basalt delta (manual bucketing)

**Fuel Efficiency Ranking:**
1. Bamboo/scaffolding (renewable, self-sustaining)
2. Charcoal (renewable, requires tree farm)
3. Coal (non-renewable, abundant)
4. Lava buckets (renewable, labor-intensive)
5. Dried kelp blocks (renewable, low yield)

**Foreman's Advice:** "Don't build a super smelter until you've automated fuel production. There's nothing sadder than a 32-furnace bank sitting idle because you're out of charcoal. Build the fuel farm first, then scale up."

---

## VILLAGER WORKSTATIONS

### Automated Crop Farming

**Villager Mechanics:**
- Farmers tend crops within claimed workstation radius
- Harvest and replant automatically
- Deposit excess crops into inventory
- Can be automated with hopper minecarts

**The Farmer Workstation:**

**Requirements:**
1. Composter (workstation)
2. Farmland (growing space)
3. Bed (villager binding)
4. Minecart with hopper (collection)

**Layout Example:**
```
[Farmer Villager]
        ↓
   [Composter]
        ↓
[Farmland Plot] 9x9 max
        ↓
[Minecart Track under crops]
        ↓
[Hopper Minecart + Powered Rail]
        ↓
[Collection Chest]
```

**Crop Preferences (by farmer):**
- Wheat (primary)
- Carrots
- Potatoes
- Beetroots
- Pumpkins/melons (not harvested by farmers)

**Automation Flow:**
1. Farmer plants, tends, harvests
2. Excess crops dropped on ground
3. Minecart with hopper collects
4. Powered rail keeps minecart moving
5. Items delivered to chest

**Yield Expectations:**
- Single farmer: ~30-50 crops/hour
- Multi-farmer setup: Linear scaling
- Limit: 128 villager cap per server

**Crew Tip:** "Bind your farmer to a bed nearby. If they lose their workstation, they stop farming. Nothing worse than finding your farm idle because the villager wandered off during the night."

### Trading Hall Automation

**The Trading Hall Concept:**
Organized villager workstations with automated trading and restocking.

**Hall Layout (2 rows of stalls):**
```
[Bed] [Workstation] [Villager]
   ↓       ↓              ↓
[Trading Interface]
   ↓
[Item Input/Output]
```

**Workstation Types:**

| Profession | Workstation | Common Trades |
|------------|-------------|---------------|
| Farmer | Composter | Crops, food |
| Fletcher | Fletching Table | Arrows, bows |
| Toolsmith | Smithing Table | Tools, enchantments |
| Armorer | Blast Furnace | Armor, bells |
| Weaponsmith | Grindstone | Weapons |
| Cleric | Brewing Stand | Magical items |
| Librarian | Lectern | Enchanted books |
| Cartographer | Cartography Table | Maps, banners |

**Automated Trading Mechanics:**

**Curing System:**
1. Zombie villager + weakness potion
2. Golden apple → villager
3. Permanent discount on trades
4. Repeatable for better prices

**Refresh Mechanics:**
- Trades refresh when villager works at workstation
- Need to break and replace workstation for forced refresh
- Time-based refresh also works (slower)

**Bulk Trading System:**
```
[Item Storage] → [Hopper] → [Dispenser]
                              ↓
                         [Trading Villager]
                              ↓
                    [Output Collection]
```

**Automation Challenges:**
- Villagers can't be fully automated (requires player interaction)
- Trade offers randomize
- Supply restocking manual
- Best for semi-automation, not full automation

**Crew Wisdom:** "Don't expect full automation from villagers. They're force multipliers, not replacements. Use 'em for bulk processing, but plan on manual trading for the high-value stuff. Mending books aren't gonna farm themselves."

### Iron Farm Automation

**Golem Spawning Mechanics:**
- Spawn condition: Villagers gossip + scare
- Spawn location: Within 10x10x10 of villagers
- Limit: 1 golem per 10 villagers (loose limit)
- Iron drops: 3-4 per golem (affected by Looting)

**The Iron Tower Design:**

**Component Stack:**
```
[Spawn Platform] (Iron Golem spawns here)
      ↓
[Water Stream] (Flows golems to kill chamber)
      ↓
[Lava/Weapon Trap] (Kills golem)
      ↓
[Hopper Collection] (Collects iron)
      ↓
[Iron Storage]
```

**Villager Pods:**
- 10-20 villagers per pod
- Zombie sightline (scare mechanic)
- Beds + workstations (villager retention)
- Protected from golems (golems attack zombies)

**Yield Calculations:**
- 20 villagers: ~60 iron/hour (theoretical)
- Practical limit: ~40-50 iron/hour
- Scaling: Multiple pods add linearly

**Space Requirements:**
- Single pod: 10x10x10 minimum
- Multi-pod: 30x30x30 for 4 pods
- Height: Above build limit (Y=200+)

**Maintenance:**
- Villages need protection (lighting)
- Zombie placement (trapdoor mechanic)
- Lava height adjustment (kill efficiency)
- Hopper clearance (collection reliability)

**Crew Achievement:** "Built an iron farm that paid for itself in a week. Now I'm drowning in iron - built an iron golem farm just to deal with the excess. First world problems, but at least I'll never run into the Nether unprepared again."

---

## REDSTONE CLOCKS

### Clock Circuit Fundamentals

**The Clock Concept:**
Redstone circuits that generate repeating pulses - the heartbeat of automation.

**Clock Types by Speed:**

| Clock Type | Pulse Speed | Complexity | Use Case |
|------------|-------------|------------|----------|
| Torch Clock | Slow (4-10 ticks) | Low | Slow automation |
| Hopper Clock | Adjustable | Medium | Timing circuits |
| Comparator Clock | Fast (1-2 ticks) | High | High-speed systems |
| Observer Clock | Fast (1 tick) | Low | Item transport |
| Minecart Clock | Adjustable | High | Precise timing |

**The Torch Clock (Simplest):**
```
[Redstone Torch] → [Redstone Dust]
       ↑                   ↓
       └──────[Block] ← [Repeater]
```

**Pulse Speed:** 4 ticks minimum
**Uses:** Slow smelting feeds, furnace cycling
**Pros:** Simple, cheap, reliable
**Cons:** Fixed speed, limited to slow applications

**Crew Warning:** "Torch clocks burn out if you pulse too fast. Keep it above 4 ticks or the torch goes dark and your automation dies. Nothing worse than troubleshooting a system that stopped working because of a clock burnout."

### Pulse Generators

**The Monostable Circuit:**
Converts a long signal into a short pulse - essential for precise timing.

**Design 1: Repeater Pulse:**
```
[Input] → [Repeater(1tick)] → [Output]
              ↓
         [Redstone Dust]
              ↓
          [Torch Inverter]
```

**Pulse Length:** 1 tick
**Use Case:** Triggering single actions from long signals

**Design 2: Comparator Pulse:**
```
[Input] → [Comparator] → [Output]
            ↓
       [Container Item]
```

**Pulse Length:** Adjustable by item count
**Use Case:** Variable pulse lengths (2-5 ticks typical)

**Field Application:**
```
[Observer Signal] → [Pulse Generator] → [Piston]
                                           ↓
                                   [Single-Tick Action]
```

**Crew Tip:** "Pulse generators are the difference between 'it works' and 'it works perfectly.' Use 'em everywhere you need precise timing. A piston that stays extended too long jams your system - a pulse generator guarantees it retracts."

### Timing Circuits

**The Hopper Clock (Adjustable):**

**Construction:**
```
[Hopper] ←→ [Hopper]
   ↓           ↓
[Comparator][Comparator]
   ↓           ↓
[Redstone] ← [Redstone]
```

**Speed Adjustment:**
- Add items to hoppers (1-64 per hopper)
- More items = slower clock
- 1 item = fastest (0.4 seconds)
- 64 items = slowest (25.6 seconds)

**Applications:**
- Furnace cycling (10-second intervals)
- Restock timing (villager trading)
- Automated harvesting intervals
- Backup power cycling

**Clock Multiplier:**
```
[Slow Clock] → [T-Flip Flop] → [Half Speed]
                               → [Half Speed Again]
```

**Use Case:** Convert fast clock to slow intervals without adding items to hoppers

**The Comparator Clock (Fast):**

**Design:**
```
[Redstone Block] → [Comparator] → [Output]
                       ↓
                 [Dust Loop]
```

**Pulse Speed:** 1-2 ticks
**Use Cases:**
- High-speed item transport
- Rapid-fire dispensers
- Flying machine activators

**Crew Warning:** "Fast clocks lag servers. Keep your 1-tick clocks contained to single chunks and turn 'em off when not in use. Nothing kills a server faster than a redstone clock that never stops."

### Practical Clock Applications

**Smelter Timing:**
```
[10-Second Clock] → [Hopper Feed]
                        ↓
                   [Furnace]
```

**Farm Harvest Timing:**
```
[50-Minute Clock] → [Piston Breaker]
                           ↓
                    [Crop Harvest]
```

**How to Build Long Timers:**
1. Hopper clock with 64 items (32 seconds)
2. Chain multiple hopper clocks
3. Multiply intervals: 32s × 2 × 2 × 2 = 256 seconds
4. Add comparator adjustments for fine-tuning

**Auto-Shutoff Circuits:**
```
[Clock] → [AND Gate] → [Output]
           ↑
      [Master Switch]
```

**Use Case:** Turn off all automation with one switch
**Crew Essential:** "Always build a master shutoff. When your server starts lagging and you need to kill the clocks fast, you'll be glad you planned ahead. Redstone doesn't respect server TPS - sometimes you gotta pull the plug."

---

## WHEN NOT TO AUTOMATE

### The Complexity Trap

**Signs Your Automation's Too Complex:**
- Takes longer to build than manual work would take
- Requires 3+ hours of troubleshooting
- Only you understand how it works
- Breaks when you look at it wrong
- Crew can't fix it without you

**Complexity Metrics:**

| System | Acceptable Build Time | Warning Signs |
|--------|----------------------|---------------|
| Simple smelter | 10 minutes | If >30 min |
| Auto-farm | 30 minutes | If >2 hours |
| Super smelter | 2 hours | If >1 day |
| Iron farm | 4 hours | If >2 days |

**The Apprenticeship Rule:**
"Don't build it if you can't teach it in 10 minutes. Automation's worthless if the crew can't maintain it when you're not around."

### Diminishing Returns

**The Efficiency Curve:**

**Tier 1 Automation (Basic):**
- Build time: 10-30 minutes
- Efficiency gain: 5-10x
- Maintenance: Low
- **ROI:** Excellent

**Tier 2 Automation (Advanced):**
- Build time: 1-2 hours
- Efficiency gain: 10-20x
- Maintenance: Medium
- **ROI:** Good

**Tier 3 Automation (Super Systems):**
- Build time: 4-8 hours
- Efficiency gain: 20-50x
- Maintenance: High
- **ROI:** Conditional

**Tier 4 Automation (Megaprojects):**
- Build time: Days to weeks
- Efficiency gain: 50-100x
- Maintenance: Very High
- **ROI:** Questionable

**When to Stop:**
- When Tier 3 takes as long as Tier 4 would save
- When maintenance exceeds manual labor time
- when system complexity exceeds crew capability

**Crew Wisdom:** "I spent a week building a fully automated smelter that sorted, smelted, and stored 20 different ore types. Beautiful machine. Worked for three days before a chunk loading issue jammed a hopper and broke the whole thing. Took two days to fix. Manual smelting would've been faster. Know when good enough is good enough."

### The Maintenance Burden

**Hidden Costs of Automation:**

**Jams:**
- Hopper overflow
- Item frame destruction
- Minecart derailment
- Chunk unloading glitches

**Debugging Time:**
- Finding the break point
- Understanding the failure mode
- Fixing without breaking other systems
- Testing the fix

**Update Risks:**
- Minecraft updates break redstone mechanics
- Chunk loading changes
- Entity behavior changes
- Plugin/mod conflicts

**Maintenance Schedule:**

| System Type | Check Frequency | Common Issues |
|-------------|-----------------|---------------|
| Smelters | Daily | Fuel, overflow |
| Farms | Weekly | Breaker jams |
| Iron Farms | Monthly | Villager loss |
| Super Smelters | Weekly | Multi-point failures |

**Crew Philosophy:** "Build it, test it, document it, then hand it off. If you can't walk away from an automation system for a month and have it still work, it's not done. Maintenance is part of the build budget, not an afterthought."

### Alternative Strategies

**Manual + Semi-Automated:**
- Manual harvesting, automatic collection
- Manual sorting, automatic smelting
- Manual farming, automatic processing

**Crew Coordination:**
- 3 crew members + basic automation = massive output
- Beats complex automation alone
- Scales with crew size
- More fun, less debugging

**Phased Automation:**
1. Start manual
2. Add collection system
3. Add processing automation
4. Add input automation last

**When Manual Wins:**
- Low-volume tasks (<1 stack/day)
- Complex item interactions
- Infrequent operations
- Prototype/experimental builds

**Foreman's Final Rule:**
"The best automation is the automation that actually works. A simple, reliable system beats a complex, brilliant one every time. Build for your future self - they'll thank you when they're not debugging a 50-stage redstone monstrosity at 2 AM."

---

## CREW TALK

### Dialogue 1: The Smelter Discussion

**Foreman:** "New guy built a manual smelter yesterday. Spent all day feeding coal and iron."

**Apprentice:** "Why didn't he automate it?"

**Journeyman:** "Tried. Got the hopper directions wrong, burned through 64 coal before we realized the furnace wasn't even on."

**Foreman:** "Hoppy's classic mistake. The hopper arrow points where the items go. Arrow down, items go down. Arrow sideways, items go sideways. Simple."

**Apprentice:** "So he just needed to flip the hopper?"

**Journeyman:** "Three hours later, yeah. But the lesson stuck - now he draws arrows on everything before he places it."

**Foreman:** "That's how you learn. One lost afternoon of coal, lifetime of proper hopper placement. Cheap tuition."

---

### Dialogue 2: Farm Automation Debate

**Builder:** "Thinking about a flying machine sugarcane farm."

**Redstone Tech:** "Don't. Too many moving parts, too many failure modes."

**Builder:** "But it's faster than piston harvest."

**Redstone Tech:** "Until it isn't. Flying machines derailing, chunk loading issues, teleportation glitches. I've spent more hours fixing flying machines than I've ever saved by having them."

**Builder:** "So piston harvest?"

**Redstone Tech:** "Piston harvest. Slower, but it works when you're not looking at it. That's the automation test - does it run when you're 500 blocks away mining diamonds?"

**Foreman:** "Tech's right. Perfect is the enemy of working. Build the piston farm today, use it for a month, then think about upgrades if you actually need more speed."

---

### Dialogue 3: The Iron Gambit

**Apprentice:** "Heard you built an iron farm in the sky."

**Veteran:** "Y=256, 20 villagers, golems spawning like crazy."

**Apprentice:** "Worth it?"

**Veteran:** "First week? No. Three days of building, two days of debugging zombie mechanics, another day fixing the lava kill chamber. Thought I'd wasted a week."

**Apprentice:** "And now?"

**Veteran:** "Now? I don't think about iron anymore. Build whatever I want, never run out. Got an iron golem farm just to deal with the excess."

**Apprentice:** "So you'd do it again?"

**Veteran:** "In a heartbeat. But I'd build the zombie platform differently. Spent two hours figuring out the trapdoor trick - should've looked it up first."

**Foreman:** "That's the iron farm in a nutshell. Brutal learning curve, infinite payoff. Just make sure you're committed before you start stacking those villagers into the sky."

---

### Dialogue 4: Storage System Wars

**Organizer:** "We need a new storage system. The current chest wall is a mess."

**Builder:** "Shulker box storage. 27 boxes per chest, massive density."

**Organizer:** "Crew can't find anything in shulker boxes. Too many clicks."

**Redstone Tech:** "Hopper sorting system then. Auto-sort everything."

**Organizer:** "Last time we built a sorter, someone put diamonds in the 'misc' category and we spent three hours looking for them."

**Foreman:** "Chest wall. Labeled. Double chests. Category system. No fancy mechanics, just organization."

**Builder:** "But shulker boxes are cooler."

**Foreman:** "Cool doesn't stack. Organized chest walls let anyone find anything in 30 seconds. Shulker boxes require you to remember what's in which box. We're building for the crew, not for your redstone portfolio."

**Organizer:** "Chest wall it is. I'll make the signs."

---

### Dialogue 5: The Super Smelter Ambition

**Apprentice:** "Saw a video on a 128-furnace super smelter. We should build one."

**Journeyman:** "We don't smelt enough to need 128 furnaces."

**Apprentice:** "But we could. Imagine the throughput."

**Redstone Tech:** "Imagine the fuel consumption. 128 furnaces running flat out would burn through a double chest of charcoal in an hour."

**Apprentice:** "So we build a bigger tree farm."

**Foreman:** "Now you're building systems to feed the systems you built to feed the systems you built. That's not automation, that's redstone addiction."

**Apprentice:** "So no super smelter?"

**Foreman:** "Build a 16-furnace bank. If you're filling that constantly, come back and we'll talk about expansion. Don't build for what you might do someday, build for what you're doing right now."

**Journeyman:** "First rule of automation: scale to demand, not ambition."

**Apprentice:** "Sixteen furnaces it is. For now."

---

## FOREMAN'S CLOSING NOTES

**The Automation Mindset:**

Every automation system should answer three questions:
1. What problem am I solving?
2. Is this the simplest solution?
3. Can someone else fix this when I'm gone?

**The Crew Automation Manifesto:**

- **Build what you use, not what you see in videos**
- **Test in creative, deploy in survival**
- **Document your systems**
- **Teach two other people how it works**
- **Turn it off when you're not using it**
- **Accept that it will break, plan accordingly**
- **Celebrate failures as learning, not embarrassment**

**Final Wisdom:**

"The best automation is the automation you forget is there. It just works, day after day, while you focus on building, mining, exploring. If you're thinking about your redstone systems more than your builds, you've overcomplicated it. Simple systems, reliable operation, happy crew. That's the MineWright way."

---

**Appendix: Quick Reference Charts**

**Material Flow Priority:**
1. Diamonds → Priority 1 (highest value)
2. Iron/Gold → Priority 2 (construction critical)
3. Redstone → Priority 3 (automation dependency)
4. Food → Priority 4 (survival essential)
5. Common materials → Priority 5 (abundant)

**Automation Build Order (New Base):**
1. Shelter + basic storage
2. Charcoal source (manual → auto)
3. Crop farm (manual → auto)
4. Tree farm (manual → auto)
5. Auto-smelter (basic)
6. Item sorting (basic)
7. Iron farm (if needed)
8. Super smelter (if volume demands)

**Redstone Component Stockpile:**
- Redstone dust: 3 stacks minimum
- Redstone torches: 2 stacks minimum
- Repeaters: 1 stack minimum
- Comparators: 32 minimum
- Hoppers: 2 stacks minimum (you'll always need more)
- Pistons: 1 stack minimum
- Slimeballs: For sticky pistons as needed

---

**End of Manual - Version 1.0 - MineWright Training Series**

**For questions, consult your local redstone technician or refer to:**
- VOLUME 3: REDSTONE FUNDAMENTALS
- VOLUME 5: CREW ORGANIZATION
- VOLUME 9: MEGAPROJECT MANAGEMENT

**Remember:** Automation serves the crew, not the other way around. Build smart, build simple, build together.
