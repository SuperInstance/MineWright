# VILLAGER OPERATIONS - CREW MANUAL

**MineWright Construction Training Series**

---

## CREW BRIEFING

Listen up, crew. Villager operations ain't just trading—it's economic engineering. You're building resource generation pipelines, optimizing supply chains, and establishing automated trading systems. Think of villagers as specialized processing nodes with their own neural pathways. They got inputs, outputs, and a whole lot of stochastic behavior you need to tame.

This manual covers the full lifecycle: acquisition, job assignment, trading optimization, breeding programs, and industrial-scale villager hall operations. Follow these protocols and you'll never face resource scarcity again. Ignore them, and you'll be stuck with a bunch of useless nitwits and wasted emeralds.

---

## TABLE OF CONTENTS

1. [Villager Basics](#1-villager-basics)
2. [Job Sites & Professions](#2-job-sites--professions)
3. [Trading Systems](#3-trading-systems)
4. [Breeding Programs](#4-breeding-programs)
5. [Villager Hall Design](#5-villager-hall-design)
6. [Special Villagers](#6-special-villagers)
7. [Curing Protocols](#7-curing-protocols)
8. [Iron Farms](#8-iron-farms)
9. [Team Operations](#9-team-operations)
10. [Crew Talk Dialogues](#10-crew-talk)

---

## 1. VILLAGER BASICS

### ACQUISITION PROTOCOLS

**Finding Villagers:**
- Villages spawn in plains, desert, savanna, taiga, and snowy taiga biomes
- Abandoned villages have no villagers but valid housing—prime real estate
- Igloos (rare) may contain basement villager—check for brewing stands

**Transport Methods:**

*Water Transport (Preferred):*
```
1. Build water channel from village to destination
2. Use boats for long-distance transport
3. Minecart with boat works for rail systems
4. Soul sand bubble elevators for vertical transport
```

*Lead Transport:*
```
1. Craft leads (4 string + 1 slimeball)
2. Right-click villager to attach
3. Slow walk—villagers get stuck on blocks
4. Works best for short distances
```

*Boat Methods:*
```
1. Push boat toward villager
2. Villager enters boat when touched
3. Break boat to release at destination
4. Ice boat highways for rapid transit
```

**Containment:**
- Use boats as temporary holding cells
- Nether portal transport works with boats
- Workstations anchor villagers to specific locations
- 2x1 holes prevent wandering but allow interaction

**CREW TIP:** Never transport a villager you haven't screened. Check their trades before investing the time. Useless trades in the village = useless trades in your hall.

---

## 2. JOB SITES & PROFESSIONS

### PROFESSION ASSIGNMENT PROTOCOL

Villagers are like uninitialized neural networks—they need a training example (job site) to develop their weights (trades). Get the workstation placement wrong and you're training on garbage data.

**Job Site Blocks:**

| Profession | Job Site Block | Trade Focus |
|------------|----------------|-------------|
| Armorer | Blast Furnace | Armor, chainmail |
| Butcher | Smoker | Food, rabbit stew |
| Cartographer | Cartography Table | Maps, banners, compasses |
| Cleric | Brewing Stand | Potions, enchanting items, magic items |
| Farmer | Composter | Crops, bread, cookies |
| Fisherman | Barrel | Fish, fishing rods |
| Fletcher | Fletching Table | Bows, arrows, crossbows |
| Leatherworker | Cauldron | Leather, saddles, horse armor |
| Librarian | Lectern | Enchanted books, bookshelves |
| Mason | Stonecutter | Stone, terracotta, bricks |
| Nitwit | None | None (default unemployed) |
| Shepherd | Loom | Wool, banners, dyed items |
| Toolsmith | Smithing Table | Tools, mineral trades |
| Weaponsmith | Grindstone | Weapons, armor |

**Assignment Rules:**

1. **Workstation Detection Range:** 16 blocks horizontally
2. **Claim Priority:** Nearest unclaimed workstation
3. **Profession Lock:** Villager claims workstation → profession assigned
4. **Reset Mechanism:** Break workstation → villager becomes unemployed
5. **New Profession:** Place different workstation → villager takes new job

**CRITICAL PROTOCOL - Librarian Cycle:**

Librarians are your high-value targets. The training protocol:

```
1. Place lectern near unemployed villager
2. Villager claims lectern → becomes librarian
3. Check trades immediately
4. If bad trades: break lectern, wait, replace lectern
5. Repeat until desired trades appear
6. LOCK IT IN: Trade once to cement the profession
```

**Embeddings Reset Warning:** Breaking a job site doesn't just reset trades—it rerolls the entire trade schema. Each cycle is independent. No correlation between cycles. You're training from scratch every time.

**CREW TIP:** Want a mason instead of a farmer? Break the composter and replace with a stonecutter. Simple. But time it right—villager must be unemployed to take the new job.

---

## 3. TRADING SYSTEMS

### TRADE OPTIMIZATION ENGINE

Trading ain't commerce—it's resource multiplication. Each villager is a function that converts inputs to emeralds and emeralds to outputs. Your job: optimize the function parameters.

**Trade Mechanics:**

*Restocking:*
- Villagers restock twice per day
- Must access job site to restock
- Stock shown in trade UI (max varies per trade)
- Restock duration: ~10 seconds per restock

*Leveling Up:*
- Villagers have 5 levels: Novice, Apprentice, Journeyman, Expert, Master
- Each level unlocks new trades
- XP earned per trade (varies by trade)
- Level requires multiple trades to advance

*Price Mechanics:*
- Base price determined at profession assignment
- Demand increases price with repeated use
- Demand decays over time
- Hero of the Village reduces prices
- Curing zombie villager gives permanent discount

**Good Trade Patterns:**

*Emerald Generators (Input → Emerald):*
- Paper (24 paper = 1 emerald) - Farmer villager
- String (15 string = 1 emerald) - Fletcher
- Iron Ingots (4-6 iron = 1 emerald) - Weaponsmith/Toolsmith
- Coal (16 coal = 1 emerald) - Armorer/Toolsmith
- Wheat (20 wheat = 1 emerald) - Farmer

*Resource Sinks (Emerald → Output):*
- Mending books - Librarian (Master level)
- Unbreaking books - Librarian
- Efficiency books - Librarian
- Fortune books - Librarian
- Silk Touch books - Librarian
- Bottles o' Enchanting - Cleric
- Name Tags - Librarian (Master level)
- Saddle - Leatherworker
- Ender Pearls - Cleric

**CREW TIP:** Trade clockwise. Use all trades at a level before leveling up. Some high-level trades are garbage. Mending at Master only? You're cycling that librarian again.

---

## 4. BREEDING PROGRAMS

### POPULATION GROWTH PROTOCOLS

Think of breeding as horizontal scaling for your villager infrastructure. You're expanding your processing capacity. But remember: every villager needs a job site and a bed. No exceptions.

**Requirements:**

1. **Beds:** One bed per villager (including baby)
2. **Food:** Villagers must be "willing"
3. **Space:** 2 blocks overhead for babies
4. **Time:** Breeding isn't instant

**Willingness Mechanics:**

*Food Items (per 12 food = 1 villager willing):*
- Bread: 3 willing per villager
- Carrots: 3 willing per villager
- Potatoes: 3 willing per villager
- Beetroots: 3 willing per villager

*Best Food Source:*
- Carrot farm (fast growth, stackable)
- Potato farm (fast growth, poisonous potatoes)
- Automated wheat → bread converter

**Breeding Chamber Design:**

```
Ideal Setup:
- 3x3 platform per pair
- Beds on upper level or adjacent
- Workstations nearby (for post-breeding assignment)
- Water stream to move babies to sorting area
- Trapdoors prevent escape while allowing interaction
```

**Population Cap Formula:**
```
Max Villagers = Number of Beds in village
```

Place extra beds to increase cap. Beds don't need to be accessible—just loaded.

**Baby Villager Growth:**
- 20 minutes to adulthood
- Inherits nothing from parents
- Profession assigned at adulthood via job site

**CREW TIP:** Breed in batches. 10 pairs = 20 babies. Sort them by profession. Don't assign professions until adulthood—job sites are wasted on kids.

---

## 5. VILLAGER HALL DESIGN

### TRADING CENTER ARCHITECTURE

Your villager hall is the central processing unit of your economic engine. Bad design = inefficient operations, trapped villagers, lost profits.

**Layout Principles:**

*Cell-Based Design:*
- 1 villager per cell (2x2 minimum)
- Workstation visible and accessible
- Trading slot (trapdoor or fence gate)
- Bed nearby (within 48 blocks)

*Horizontal Layout:*
```
[Bed][Villager][Workstation]
   |         |          |
[Walkway for player access]
```

*Vertical Layout:*
```
Level 1: Trading cells with workstations
Level 2: Beds (above or below)
```

**Workstation Placement:**

*Critical Rules:*
- Workstation must be within 16 blocks of villager
- Line of sight NOT required
- Only one workstation per villager type in detection range
- Multiple workstations of same type = villager switches randomly

**Traffic Flow:**

*Efficient Circulation:*
- Central hallway with cells on both sides
- Trading slots face hallway
- Signs label each villager's profession
- Redstone lamps indicate availability

**Protection Systems:**

*Zombie Defense:*
- Iron doors with buttons
- Trapdoors for trading (zombies can't enter)
- Fence gates (villagers can't open)
- Overhead lighting (no hostile spawns)
- Golem patrol area

**CREW TIP:** Put your high-value traders (Mending librarians) closest to spawn. Time is money. Running 50 blocks for one Mending trade is inefficient ops.

---

## 6. SPECIAL VILLAGERS

### HIGH-PRIORITY OPERATORS

Some villagers are force multipliers. They deserve extra investment and protection.

**LIBRARIAN - The Enchant Provider**

*Target Trades:*
- Mending (essential)
- Unbreaking III
- Efficiency V
- Fortune III
- Silk Touch
- Sharpness V
- Looting III
- Protection IV

*Cycling Protocol:*
```
1. Build cell with lectern
2. Place unemployed villager
3. Place lectern → librarian assigned
4. Check trades immediately
5. Break lectern if bad trades
6. Wait for green particles (profession reset)
7. Place lectern again
8. Repeat until Mending appears
```

*Expected Cycles:* 5-50 attempts. Mending is rare.
*Cost:* ~64-128 bookshelves worth of cycles
*ROI:* One Mending book pays for the entire operation

**TOOLSMITH - The Equipment Supplier**

*Value Trades:*
- Diamond pickaxe, axe, shovel, hoe
- Enchanted diamond tools (Fortune, Efficiency, Mending)
- Coal and iron emerald sinks

*Setup:* Smithing table, diamond supply
*Use Case:* Early-game diamond access, backup tool production

**WEAPONSMITH - The Combat Supplier**

*Value Trades:*
- Diamond sword (enchanted)
- Diamond axe (enchanted)
- Emerald → iron exchange

*Setup:* Grindstone
*Use Case:* Diamond weapon access without mining

**CLERIC - The Magic Supplier**

*Value Trades:*
- Bottles o' Enchanting (XP farm supplement)
- Redstone dust
- Glowstone dust
- Nether wart
- Ender pearls
- Name Tag (rare, Master level)

*Setup:* Brewing stand
*Use Case:* Potions, enchanting, ender pearl access

**CREW TIP:** Prioritize Librarian above all. Mending is the single most valuable enchant. Once you have Mending, your equipment becomes permanent. That's infinite tool durability.

---

## 7. CURING PROTOCOLS

### ZOMBIE VILLAGER CONVERSION

Curing zombie villagers is your path to discounted trades and niche profession setup. It's investment banking with guaranteed returns.

**Requirements:**
- Weakness Potion (Splash or Lingering)
- Golden Apple
- Zombie Villager
- Safe containment

**Curing Process:**

1. **Capture Zombie Villager**
   - Boat transport
   - Minecart containment
   - Nether portal trap

2. **Apply Weakness**
   - Splash potion of Weakness (1:30 duration)
   - Lingering potion (area effect)
   - Witches can cast weakness (advanced technique)

3. **Feed Golden Apple**
   - Must be applied while weakness is active
   - Right-click with golden apple
   - Villager begins shaking

4. **Wait for Conversion**
   - Duration: ~3-5 minutes
   - Villager emits red particles during conversion
   - Strength I buff during conversion
   - Success: villager converts with permanent discount

**Discount Mechanics:**
- Permanent price reduction on all trades
- Stacks with Hero of the Village
- 1 cure = ~5-10% discount
- Multiple cures don't stack further

**Economic Analysis:**
```
Cost per Cure:
- Golden Apple: 8 gold ingots = 72 golden nuggets
- Weakness Potion: 1 inventory slot
- Time: 5 minutes

Benefit:
- Permanent discount: 5-10%
- ROI: 100+ trades to break even
- Niche use: Specific profession setup in remote locations
```

**CREW TIP:** Cure only when you need a specific villager in a specific location. Transporting existing villagers is usually faster. Curing is for when the transport distance exceeds 1,000 blocks.

---

## 8. IRON FARMS

### AUTOMATED IRON PRODUCTION

Iron farms are the ultimate villager operation. You're leveraging golem spawning mechanics to generate infinite iron. It's industrial-level production.

**Golem Spawning Mechanics:**

*Spawn Conditions:*
- Villager gossip (scared by zombie)
- Minimum 3 villagers
- Villagers must be within 10 blocks of each other
- Spawn attempt: every 30 seconds (approximate)
- Spawn location: within 16x16x6 area around villagers

*Spawning Algorithm:*
1. Villager sees zombie and gossips
2. Golem spawn chance increases with each gossip
3. At threshold, golem spawns
4. Golem attacks zombie threat
5. Iron ingots + poppies dropped on death

**Farm Designs:**

*Classic Design (Mumbo Jumbo style):*
- 10 villagers in holding pen
- Zombie visible but unreachable (behind fence)
- Spawn platform 3 blocks above villagers
- Water flushing system to kill golems
- Collection system for iron drops

*Modular Design:*
- 3-villager cells
- Multiple cells for linear scaling
- Each cell独立的 spawning zone
- Redstone controls for on/off

*Efficiency Metrics:*
- 1 golem = 3-6 iron ingots
- 10 villagers = ~60 iron/hour (optimized)
- 100 villagers = ~600 iron/hour

**Optimization Protocols:**

*Maximizing Spawn Rate:*
- Keep villagers terrified but safe
- Zombie must be visible but not pathable
- Use trapdoors to block zombie movement
- Remove competing spawnable spaces

*Collection System:*
- Water streams push drops
- Hopper minecart collection
- Item sorting system
- Storage silo with shulker boxes

**CREW TIP:** Iron farms are late-game operations. Build one after you have your trading hall established. Iron is useful, but emeralds buy everything. Prioritize the trading engine first.

---

## 9. TEAM OPERATIONS

### MULTI-AGENT VILLAGER MANAGEMENT

Large-scale villager operations require team coordination. This is where the MineWright crew shines. Multiple agents, parallel processing, synchronized operations.

**Role Distribution:**

*Acquisition Team:*
- Agent 1: Scouting villages
- Agent 2: Building transport infrastructure
- Agent 3: Operating boats/minecarts

*Trading Hall Setup:*
- Agent 1: Building cells and workstations
- Agent 2: Assigning professions
- Agent 3: Cycling for optimal trades

*Breeding Operations:*
- Agent 1: Managing food supply
- Agent 2: Monitoring bed capacity
- Agent 3: Sorting by profession

**Parallel Processing:**

*Simultaneous Trading:*
- Each agent assigned a villager section
- Optimal restock management
- Distributed resource processing

*Construction Division:*
- Agent 1: Builds housing
- Agent 2: Installs workstations
- Agent 3: Sets up trading infrastructure

**Communication Protocols:**

*Trade Updates:*
```
Agent 1: "Got Mending on Librarian Cell 4"
Agent 2: "Confirmed, updating inventory manifest"
Agent 3: "Redirecting resource flow to Cell 4"
```

*Restock Coordination:*
```
Agent 1: "Farmer workstations need restock check"
Agent 2: "Handling all agricultural workstations"
Agent 3: "Industrial workstations clear"
```

**CREW TIP:** Assign each agent a specialty. One agent manages all librarians (high complexity). Another handles bulk trades (farmers, clerics). Third agent manages breeding. Specialization beats generalization every time.

---

## 10. CREW TALK

### FIELD DIALOGUES FROM ACTIVE OPERATIONS

---

**Dialogue 1: The Librarian Cycle**

> **Agent Miller:** "How many cycles on that librarian?"
>
> **Agent Smith:** "Forty-two and counting. Still no Mending. The embedding weights are stubborn today."
>
> **Miller:** "You're using the right block, right? Lectern, not bookshelf."
>
> **Smith:** "Lectern, confirmed. Position optimized. Detection range clear. This is just stochastic variance at work."
>
> **Miller:** "Keep cycling. We got two hundred bookshelves in storage. The convergence is coming."
>
> **Smith:** "Copy that. Breaking lectern. Resetting weights. Attempting cycle forty-three."

---

**Dialogue 2: Transport Operations**

> **Agent Davis:** "Village located at coordinates -850, 320. Population twelve. Professions mixed."
>
> **Agent Taylor:** "Constructing transport channel. Water source placement in progress. Estimated completion: two minutes."
>
> **Davis:** "I've identified three promising candidates. One farmer with paper trades. One cleric with ender pearl access. One librarian—trades unknown."
>
> **Taylor:** "Channel operational. Loading villager into transport craft."
>
> **Davis:** "Scanning librarian trades. No Mending. Confirmed: cycling required."
>
> **Taylor:** "Transport initiated. ETA to base: 6 minutes. Prepare processing cells."

---

**Dialogue 3: Breeding Program**

> **Agent Rivera:** "Beds installed. Capacity increased to twenty-four. Food supply standing by."
>
> **Agent Chen:** "Carrot farm operational. Production rate: 120 carrots per minute. Sufficient for full population willing."
>
> **Rivera:** "Initiating breeding protocol. Monitoring population growth."
>
> **Chen:** "Villagers willing. Breeding cycle initiated. Time to adulthood: twenty minutes."
>
> **Rivera:** "Job site distribution prepared. Professions assigned at adulthood."
>
> **Chen:** "Processing pipeline confirmed. Economic engine expanding."

---

**Dialogue 4: Iron Farm Operations**

> **Agent Lee:** "Golem spawn rate dropping. Something's blocking the detection zone."
>
> **Agent Patel:** "Scanning sector. Zombie positioning... hold. Zombie has pathing to villagers. They're not terrified."
>
> **Lee:** "Gossip levels insufficient. Spawn probability approaching zero."
>
> **Patel:** "Applying fix. Trapdoor installation blocking zombie movement. Villagers still have line of sight."
>
> **Lee:** "Terror levels rising. Gossip increasing. Spawn rate normalizing."
>
> **Patel:** "Production restored. Iron flow resumed."

---

**Dialogue 5: Trading Hall Optimization**

> **Agent Garcia:** "Section 3 trades complete. Emerald balance: four hundred twelve. Resource inventory: full."
>
> **Agent Martinez:** "Section 4 requires restock. Workstations blocked. Need physical intervention."
>
> **Garcia:** "Acknowledged. Rerouting to Section 4. Clearing workstation access."
>
> **Martinez:** "Section 5 showing demand pricing on diamond trades. Recommend switching to Section 7 backup."
>
> **Garcia:** "Section 7 confirmed. Trading active. Demand prices reset on Section 5."
>
> **Martinez:** "Coordinated trading prevents demand spikes. Optimal operations maintained."

---

## FINAL CREW BRIEFING

Villager operations separate the novice miners from the true MineWright crew. Anyone can dig a hole. It takes skill to build an economic engine.

**Key Takeaways:**

1. **Patience pays** - Good librarians take dozens of cycles
2. **Design matters** - Bad layouts kill efficiency
3. **Scale intelligently** - Breed only when you have infrastructure
4. **Specialize** - Not every villager needs to be a librarian
5. **Protect your assets** - Golem defense, zombie-proofing, redundant backups

Your trading hall is the heart of your operation. Your villagers are the processing nodes. Your job is to optimize the entire pipeline.

Now get out there, find those villages, and build something efficient.

**MineWright Construction Crew - Villager Operations Division**

*Manual Version 1.0*
*Last Updated: Cycle 2026-01-10*
