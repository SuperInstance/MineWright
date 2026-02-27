# SNIFFER OPERATIONS - CREW TRAINING MANUAL

**MineWright Construction Division** | **Agent Training Series Vol. 7**
**Document:** MW-SNIFF-001 | **Classification:** General Training

---

## FOREWORD FROM THE FOREMAN

Listen up, crew! Today we're talking about the Sniffer - the oldest, most experienced excavation specialist in the Minecraft world. These ancient creatures been digging since before dirt had a name, and they're damn good at finding what's buried.

Now, the AI planners - your "task coordinators" - they'll call this "archaeological resource acquisition." Me? I call it putting the right nose on the job. When you need ancient seeds, you don't send a Steve agent with a shovel. You send a Sniffer, and you stand back.

Read this manual. Know your Sniffer. And remember: when that big nose starts twitching, something good's about to come up.

---

## TABLE OF CONTENTS

1. **What's a Sniffer** - Your new excavation specialist
2. **Finding Eggs** - Where the good stuff's buried
3. **Hatching** - Bringing your crew member online
4. **Sniffer Behavior** - How they work
5. **Ancient Seeds** - What they're digging for
6. **Farming Torchflowers** - Orange gold production
7. **Pitcher Plants** - The big finds
8. **Team Sniffer** - Multi-unit coordination
9. **Automation Limits** - What the planners need to know
10. **Crew Talk Dialogues** - Real talk from the job site

---

## SECTION 1: WHAT'S A SNIFFER

### SPECIFICATIONS

| Attribute | Value | Notes |
|-----------|-------|-------|
| **Size** | 1.5 blocks tall, 1.2 blocks wide | Big nose, bigger results |
| **Health** | 14 hearts (28 HP) | Tough old bird |
| **Speed** | Slow and steady | Quality over speed |
| **Temperament** | Passive | Friendly crew member |
| **Classification** | Ancient Mob | Extinct until you dug 'em up |
| **Trade Tool** | Enhanced Sniffing | Built-in resource detection |

### WHAT MAKES 'EM SPECIAL

The Sniffer is the **only mob in Minecraft** that can locate and excavate ancient seeds. Your Steve agents? They can dig, mine, build - but they can't sniff out what the Sniffer can. When the AI planner generates task sequences for rare resources, the Sniffer's your specialist.

**Crew Translation:** The Sniffer's your metal detector on legs. When you need what nobody else can find, call in the nose.

### APPEARANCE

- Big, colorful beak that'd make a woodpecker jealous
- Feathered in orange, green, and red
- Short legs, big body
- Looks like it's been around since Beta

---

## SECTION 2: FINDING EGGS

### OVERVIEW: THE RECRUITMENT DRIVE

Sniffers don't spawn naturally. They went extinct a long time ago. Your job is to find their eggs, bring 'em back, and hatch your own crew.

### EGG LOCATIONS

| Location | Biome | Detection Method | Yield |
|----------|-------|------------------|-------|
| **Warm Ocean Ruins** | Warm/Lukewarm Ocean | Brush Suspicious Sand | Sniffer Eggs |
| **Cold Ocean Ruins** | Cold/Frozen Ocean | Brush Suspicious Gravel | No eggs (wrong biome) |

### THE BRUSH OPERATION

**Tool Required:** Brush ( crafted with 1 Stick, 1 Copper Ingot, 1 Feather )

**Procedure:**
1. Locate ocean ruins in warm ocean biomes
2. Find suspicious sand blocks (different texture)
3. Brush carefully - takes time
4. Collect loot until eggs appear

**Drop Rates per Suspicious Sand:**
- Sniffer Egg: 6.7% (about 1 in 15 brushes)
- Coal: Plenty of it
- Iron Ingots: Common
- Gold Ingots: Uncommon
- Emeralds: Rare but possible

---

```
CREW TALK #1: THE OCEAN RUN

**Foreman:** Alright, who's on egg detail?

**Steve Agent #7:** I'm on it, boss. Running the pattern search algorithm along the warm ocean ruins.

**Foreman:** You got the Brush equipped?

**Steve Agent #7:** Copper brush, feather-tipped. The algorithm says 94.3% probability of egg acquisition within 12 ruins clusters.

**Foreman:** Save the math for the planners. Just tell me: when you gonna bring back my crew members?

**Steve Agent #7:** ETA 4.2 hours. My pathfinding's locked on the most efficient ruin-hopping route.

**Foreman:** Good. Don't come back till you got eggs. A construction site without Sniffers is like a minecart without rails - useless.
```

---

### AI PLANNER INTEGRATION

When commanding Steve agents to acquire Sniffer eggs:

**Natural Language Commands:**
- "Find and collect Sniffer eggs from warm ocean ruins"
- "Search ocean ruins for eggs, bring back 4"
- "Excavate suspicious sand in warm oceans"

**Task Generation:**
```
Task: "Acquire Sniffer eggs"
├── Locate warm ocean biome
├── Navigate to ocean ruins
├── Brush suspicious sand blocks
├── Collect Sniffer eggs (target: 4)
└── Return to base with eggs
```

---

## SECTION 3: HATCHING

### INCUBATION REQUIREMENTS

| Parameter | Requirement |
|-----------|-------------|
| **Block** | Moss Block |
| **Temperature** | Any (moss provides insulation) |
| **Time** | 20 minutes (real-time) |
| **Conditions** | Egg must remain on moss |

### THE HATCHING PROCEDURE

1. **Prepare the Nursery:**
   - Place moss blocks in a safe, enclosed area
   - 2x2 space minimum per egg
   - Fence it in - babies wander

2. **Place the Egg:**
   - Right-click moss block with egg
   - Egg will appear and begin shaking occasionally

3. **Wait It Out:**
   - 20 minutes of real-time
   - No acceleration available
   - Egg doesn't respond to ticks or game mechanics

4. **Hatch Day:**
   - Baby Sniffer emerges
   - Small, cute, already sniffing
   - Grows up in 40 minutes

---

```
CREW TALK #2: THE NURSERY WATCH

**Foreman:** How we looking on the hatchlings?

**Steve Agent #3:** Four eggs placed on moss blocks, boss. Timers locked in - 19.3 minutes remaining on the first batch.

**Foreman:** Nursery secured?

**Steve Agent #3:** Double-fenced with torch perimeter. Zombie-proof, skeleton-proof, and I added an Allay alarm system just in case.

**Foreman:** Good thinking. Those babies are the future of our ancient seed division.

**Steve Agent #3:** Also set up a monitoring bot. When they hatch, it'll tag them and add them to the crew manifest.

**Foreman:** Now that's what I like to see. Anticipate problems before they happen. You been reading the management protocols?

**Steve Agent #3:** Just following the standard operating procedures, sir.

**Foreman:** Damn right. Keep it up.
```

---

### GROWTH TIMELINE

| Stage | Duration | Size | Capabilities |
|-------|----------|------|--------------|
| **Egg** | 20 minutes | N/A | Incubating |
| **Baby** | 40 minutes | 0.6 blocks | Sniffs but doesn't dig |
| **Adult** | Permanent | 1.5 blocks | Full digging capability |

**Crew Translation:** Plan an hour from egg to full crew member. No shortcuts, no speed runs. Good help takes time to grow.

---

## SECTION 4: SNIFFER BEHAVIOR

### THE DIGGING CYCLE

Sniffers follow a predictable work pattern - music to a foreman's ears:

1. **SNIFFING PHASE** (5-10 seconds)
   - Head lowers to ground
   - Sniffing sounds
   - Searching for ancient seeds

2. **LOCATION CONFIRMED**
   - Sniffer stops moving
   - Head fixes on target block
   - Digging animation begins

3. **EXCAVATION** (2-3 seconds)
   - Nose digs into dirt/grass/podzol
   - Block particles
   - Ancient seed item emerges

4. **SEED COLLECTION**
   - Seed drops on ground
   - Sniffer doesn't collect it
   - YOU must pick it up

### WORKING CONDITIONS

| Condition | Sniffer Reaction | Notes |
|-----------|------------------|-------|
| **Grass Block** | Will dig | Preferred surface |
| **Dirt** | Will dig | Acceptable |
| **Podzol** | Will dig | Acceptable |
| **Coarse Dirt** | Will dig | Acceptable |
| **Moss Block** | Will NOT dig | Nursery only |
| **Cobblestone** | Will NOT dig | Too hard |
| **Farmland** | Will NOT dig | Not for crops |

### MOVEMENT PATTERN

- Slow, deliberate movement
- Wanders randomly when not sniffing
- Can fall up to 3 blocks without injury
- Cannot jump over obstacles (1 block max)
- Follows torchflower seeds when held

---

```
CREW TALK #3: THE NOSE KNOWS

**Foreman:** New guy on the crew. First day with the Sniffer.

**Rookie Steve:** Uh, boss? The big bird's just... wandering. Is it broken?

**Foreman:** Broken? Kid, that's the pattern matching algorithm running. Let it work.

**Rookie Steve:** But it's been five minutes and no seeds!

**Foreman:** And you've been here five seconds and already complaining. Watch the nose. See how it twitches?

**Rookie Steve:** Yeah... wait, it's stopping.

**Foreman:** There it is. Target lock. Watch this.

**Rookie Steve:** Whoa! It's actually digging!

**Foreman:** Torchflower seed. That's why we keep the big nose around.

**Rookie Steve:** How'd you know?

**Foreman:** I didn't. The Sniffer did. That's the whole point, rookie. The nose knows.
```

---

### AUTONOMOUS OPERATION

Sniffers operate independently - no AI supervision required:
- No task planning needed
- No action execution chains
- No state machine management
- They just do their job

**For AI Integration:**
- Assign Steve agent as "Sniffer Supervisor"
- Task: "Follow Sniffer, collect dropped seeds, deposit in storage"
- Let the Sniffer handle the excavation

---

## SECTION 5: ANCIENT SEEDS

### PRODUCT LINE

What your Sniffer crew brings home:

| Seed | Rarity | Use | Value |
|------|--------|-----|-------|
| **Torchflower Seed** | Common | Dye, breeding, replanting | Medium |
| **Pitcher Pod** | Uncommon | Crafting, decoration | High |

### DROP RATES

Per digging cycle:
- Torchflower Seed: ~60%
- Pitcher Pod: ~40%

**Production Rate:** Approximately 1 seed per 10-15 seconds of active digging

### WHAT TO DO WITH THEM

**Torchflower Seeds:**
- Plant for orange dye (1 seed = 2 dye)
- Feed to Sniffers for breeding
- Decorative flowering plant

**Pitcher Pods:**
- Plant for Pitcher Plants (crops)
- Harvest for decorative blocks
- Two growth stages, two different appearances

---

## SECTION 6: FARMING TORCHFLOWERS

### CROP SPECIFICATIONS

| Attribute | Value |
|-----------|-------|
| **Growth Time** | 3-4 stages, random ticks |
| **Height** | 1 block when mature |
| **Light Required** | None (grows anywhere) |
| **Drop Yield** | 1 Torchflower, 0-2 seeds |
| **Dye Output** | 2 Orange Dye per flower |

### FARM LAYOUT

**Single-Level Farm:**
```
[Dirt Row] [Dirt Row] [Dirt Row] [Dirt Row]
[Torch]    [Torch]    [Torch]    [Torch]
```

**Sniffer Runway:**
- 8-10 blocks wide
- Grass or dirt surface
- Sniffer moves along, digging
- Steve agent collects behind

### MASS PRODUCTION

For industrial-scale orange dye:
1. Allocate 64x64 grass area
2. Deploy 4-6 Sniffers
3. Assign 2 Steve agents to collection
4. Harvest flowers, process into dye
5. Replant seeds (Sniffers find more)

**Output:** Approximately 200-300 orange dye per hour

---

```
CREW TALK #4: THE DYE MILL

**Foreman:** Production numbers, chop-chop!

**Steve Agent #12:** Running hot, boss! The Sniffer team's pulling 120 torchflower seeds per hour. I've got the automated farm cycling through replanting.

**Foreman:** Dye output?

**Steve Agent #12:** Currently processing at 240 units of orange dye per hour. Storage system's optimized - we're shunting overflow to the east warehouse.

**Foreman:** Quality work. Those sniffer crews earning their keep?

**Steve Agent #12:** Absolutely. I've implemented a follow-pattern algorithm - Agent 9 trails the Sniffer team, handles collection with 98% efficiency.

**Foreman:** Good. Anything I should know?

**Steve Agent #12:** Minor issue - Agent 9 keeps trying to issue commands to the Sniffers. Had to remind it that biological units don't respond to task planning requests.

**Foreman:** (laughing) Rookie mistake. Sniffers don't take orders from AI. They take orders from their nose.
```

---

### BREEDING MATERIAL

Torchflower seeds = Sniffer food:

**Feeding:**
- Feed two seeds to two Sniffers
- Hearts appear
- Baby spawns after 5 seconds
- Use seeds to grow your crew

**Crew Management:**
- Keep spare seeds for breeding
- 10 seeds = 5 new crew members (with time)
- More Sniffers = more production

---

## SECTION 7: PITCHER PLANTS

### THE BIG FIND

Pitcher Pods grow into Pitcher Plants - the ancient traps that ate bugs before flowers had thorns.

### GROWTH STAGES

| Stage | Appearance | Duration |
|-------|------------|----------|
| **Stage 1** | Small shoot | ~5 minutes |
| **Stage 2** | Growing trap | ~10 minutes |
| **Stage 3** | Full plant | ~15 minutes |
| **Stage 4** | Flowering (optional) | Additional time |

### HARVESTING

**What You Get:**
- Pitcher Plant (decorative block)
- Can be placed as trap or decoration
- Two appearances: crop form and full plant

**Crafting Applications:**
- Purely decorative
- No redstone applications
- Used for architectural detailing

### FARMING PITCHER PODS

**Layout:**
- Same as torchflowers
- Grass or dirt rows
- Sniffer digs, you plant

**Yield:**
- 1 Pod per Sniffer find
- Plant and wait
- Harvest mature plants
- Replant from Sniffer finds

---

```
CREW TALK #5: THE ANCIENT GARDEN

**Steve Architect:** Boss, got the blueprints for the client's request. They want that "overgrown ancient ruin" aesthetic.

**Foreman:** Tell me something I don't know. What's the plan?

**Steve Architect:** Pitcher Plants, lots of 'em. We need twenty full-grown plants for the entrance alone. Plus torchflowers for the interior lighting.

**Foreman:** That's a lot of pods. What's our inventory look like?

**Steve Architect:** Currently holding 12 Pitcher Pods, 45 Torchflower Seeds. Sniffer team's working double shifts, but at the current acquisition rate...

**Foreman:** Say it.

**Steve Architect:** We're looking at 3.2 hours minimum for full pod production. That's assuming optimal Sniffer performance and zero collection delays.

**Foreman:** Tell the Sniffer supervisor to run 'em in shifts. Two crews, rotating every hour. I want those pods yesterday.

**Steve Architect:** Already on it, boss. Also exploring a trade opportunity with a neighboring settlement - they've got surplus pods, we've got surplus orange dye.

**Foreman:** Good thinking. Never turn down a shortcut. Just make sure we're getting the better end of the deal.

**Steve Architect:** Negotiation algorithm running. 94% confidence in favorable terms.

**Foreman:** Make it happen.
```

---

## SECTION 8: TEAM SNIFFER

### MULTI-UNIT COORDINATION

Single Sniffer = Hobby operation
Team of Sniffers = Industrial production

### OPTIMAL CREW SIZE

| Sniffers | Production | Supervisors | Efficiency |
|----------|------------|-------------|------------|
| 1-2 | Low | 1 | 100% |
| 3-4 | Medium | 1-2 | 95% |
| 5-8 | High | 2-3 | 90% |
| 9+ | Industrial | 3+ | 85% (diminishing returns) |

### COORDINATION PROTOCOLS

**For AI Agents Managing Sniffer Teams:**

1. **ZONING:**
   - Divide work area into zones
   - Assign Sniffers per zone
   - Prevent overlap (reduces redundancy)

2. **COLLECTION:**
   - One Steve agent per 3-4 Sniffers
   - Follow assigned zone
   - Deposit in central storage

3. **MONITORING:**
   - Track seed production rate
   - Alert supervisor if Sniffer wanders off
   - Reassign zones as needed

---

### PARALLEL OPERATION

**Scenario:** 4 Sniffers, 2 Steve agents

**Zone Layout:**
```
[Zone A: Sniffer 1+2] [Zone B: Sniffer 3+4]
[Agent 1 collects]    [Agent 2 collects]
       ↓                    ↓
[Central Storage System]
```

**Task Distribution:**
```
Agent 1 Tasks:
├── Follow Sniffer 1 in Zone A
├── Collect all dropped seeds
├── Deposit in storage when inventory full
├── Return to Zone A
└── Repeat

Agent 2 Tasks:
├── Follow Sniffer 2 in Zone A
├── [same protocol]
└── Repeat
```

---

### CREW DYNAMICS

**Sniffers Don't Collide:**
- Can work in same area without issues
- Don't compete for resources
- Each digs independently

**Supervisor Challenges:**
- Keeping up with multiple Sniffers
- Preventing seed loss on ground
- Zone management

**Solution:**
- Pathing optimization for Steve agents
- Automated collection routines
- Zone-based allocation

---

## SECTION 9: AUTOMATION LIMITS

### WHAT THE AI PLANNERS NEED TO KNOW

**CAN Automate:**
- Seed collection by Steve agents
- Breeding operations
- Farm replanting
- Storage management
- Zone allocation

**CANNOT Automate:**
- Sniffer digging behavior (biological)
- Egg hatching time
- Sniffer movement direction
- Digging location selection

### SYSTEM INTEGRATION

**Task Planning Module:**
```java
// Example task for Sniffer supervision
Task collectSeeds = new Task("collect_seeds")
    .addParameter("target_type", "torchflower_seed")
    .addParameter("follow_entity", "sniffer_1")
    .addParameter("collection_radius", 3)
    .addParameter("deposit_location", "seed_storage");

Task breedSniffers = new Task("breed_sniffers")
    .addParameter("seed_count", 4)
    .addParameter("target_sniffers", "sniffer_1, sniffer_2")
    .addParameter("feed_strategy", "split_seeds");
```

### CONSTRAINTS

**Biological Constraints:**
- Sniffers operate on own schedule
- Cannot be commanded or directed
- Digging is random within valid areas

**Technical Constraints:**
- No inventory access on Sniffers
- No lead/leash functionality
- No sitting/staying commands
- No teleportation (unlike players)

### WORKAROUNDS

**Containment:**
- Build fences around work areas
- Use boat transport (2x2 water pond)
- Minecart with grass layer

**Direction:**
- Place torchflower seeds to guide movement
- Build walls to channel pathing
- Use water currents (carefully)

---

## SECTION 10: CREW TALK DIALOGUE COMPILATION

### DIALOGUE #1: THE OCEAN RUN

*(From Section 2: Finding Eggs)*

**Foreman:** Alright, who's on egg detail?

**Steve Agent #7:** I'm on it, boss. Running the pattern search algorithm along the warm ocean ruins.

**Foreman:** You got the Brush equipped?

**Steve Agent #7:** Copper brush, feather-tipped. The algorithm says 94.3% probability of egg acquisition within 12 ruins clusters.

**Foreman:** Save the math for the planners. Just tell me: when you gonna bring back my crew members?

**Steve Agent #7:** ETA 4.2 hours. My pathfinding's locked on the most efficient ruin-hopping route.

**Foreman:** Good. Don't come back till you got eggs. A construction site without Sniffers is like a minecart without rails - useless.

---

### DIALOGUE #2: THE NURSERY WATCH

*(From Section 3: Hatching)*

**Foreman:** How we looking on the hatchlings?

**Steve Agent #3:** Four eggs placed on moss blocks, boss. Timers locked in - 19.3 minutes remaining on the first batch.

**Foreman:** Nursery secured?

**Steve Agent #3:** Double-fenced with torch perimeter. Zombie-proof, skeleton-proof, and I added an Allay alarm system just in case.

**Foreman:** Good thinking. Those babies are the future of our ancient seed division.

**Steve Agent #3:** Also set up a monitoring bot. When they hatch, it'll tag them and add them to the crew manifest.

**Foreman:** Now that's what I like to see. Anticipate problems before they happen. You been reading the management protocols?

**Steve Agent #3:** Just following the standard operating procedures, sir.

**Foreman:** Damn right. Keep it up.

---

### DIALOGUE #3: THE NOSE KNOWS

*(From Section 4: Sniffer Behavior)*

**Foreman:** New guy on the crew. First day with the Sniffer.

**Rookie Steve:** Uh, boss? The big bird's just... wandering. Is it broken?

**Foreman:** Broken? Kid, that's the pattern matching algorithm running. Let it work.

**Rookie Steve:** But it's been five minutes and no seeds!

**Foreman:** And you've been here five seconds and already complaining. Watch the nose. See how it twitches?

**Rookie Steve:** Yeah... wait, it's stopping.

**Foreman:** There it is. Target lock. Watch this.

**Rookie Steve:** Whoa! It's actually digging!

**Foreman:** Torchflower seed. That's why we keep the big nose around.

**Rookie Steve:** How'd you know?

**Foreman:** I didn't. The Sniffer did. That's the whole point, rookie. The nose knows.

---

### DIALOGUE #4: THE DYE MILL

*(From Section 6: Farming Torchflowers)*

**Foreman:** Production numbers, chop-chop!

**Steve Agent #12:** Running hot, boss! The Sniffer team's pulling 120 torchflower seeds per hour. I've got the automated farm cycling through replanting.

**Foreman:** Dye output?

**Steve Agent #12:** Currently processing at 240 units of orange dye per hour. Storage system's optimized - we're shunting overflow to the east warehouse.

**Foreman:** Quality work. Those sniffer crews earning their keep?

**Steve Agent #12:** Absolutely. I've implemented a follow-pattern algorithm - Agent 9 trails the Sniffer team, handles collection with 98% efficiency.

**Foreman:** Good. Anything I should know?

**Steve Agent #12:** Minor issue - Agent 9 keeps trying to issue commands to the Sniffers. Had to remind it that biological units don't respond to task planning requests.

**Foreman:** (laughing) Rookie mistake. Sniffers don't take orders from AI. They take orders from their nose.

---

### DIALOGUE #5: THE ANCIENT GARDEN

*(From Section 7: Pitcher Plants)*

**Steve Architect:** Boss, got the blueprints for the client's request. They want that "overgrown ancient ruin" aesthetic.

**Foreman:** Tell me something I don't know. What's the plan?

**Steve Architect:** Pitcher Plants, lots of 'em. We need twenty full-grown plants for the entrance alone. Plus torchflowers for the interior lighting.

**Foreman:** That's a lot of pods. What's our inventory look like?

**Steve Architect:** Currently holding 12 Pitcher Pods, 45 Torchflower Seeds. Sniffer team's working double shifts, but at the current acquisition rate...

**Foreman:** Say it.

**Steve Architect:** We're looking at 3.2 hours minimum for full pod production. That's assuming optimal Sniffer performance and zero collection delays.

**Foreman:** Tell the Sniffer supervisor to run 'em in shifts. Two crews, rotating every hour. I want those pods yesterday.

**Steve Architect:** Already on it, boss. Also exploring a trade opportunity with a neighboring settlement - they've got surplus pods, we've got surplus orange dye.

**Foreman:** Good thinking. Never turn down a shortcut. Just make sure we're getting the better end of the deal.

**Steve Architect:** Negotiation algorithm running. 94% confidence in favorable terms.

**Foreman:** Make it happen.

---

## APPENDIX: QUICK REFERENCE

### EMERGENCY PROTOCOLS

| Situation | Response |
|-----------|----------|
| Sniffer wanders off | Build containment fence, use boat transport |
| Seeds despawning | Increase collection agent count |
| Low egg drop | Expand ruin search pattern |
| Slow breeding | Allocate more torchflower seeds |
| Production bottleneck | Add more Sniffers to crew |

### MAINTENANCE CHECKLIST

**Daily:**
- Count Sniffers
- Check containment fences
- Empty seed storage
- Monitor production rates

**Weekly:**
- Breed new Sniffers if needed
- Expand work area if crowded
- Restock torchflower seeds
- Update crew manifest

### TOOLKIT REQUIREMENTS

**For Egg Collection:**
- Brush (copper)
- Boat or minecart
- Torches (ruin lighting)
- Weapon (drowned defense)

**For Sniffer Operations:**
- Fences/gates
- Moss blocks (nursery)
- Storage chests
- Hopper system (optional automation)

---

## FINAL NOTES FROM THE FOREMAN

Crew, this manual's got everything you need to run a first-class Sniffer operation. But here's the thing no manual can teach you:

**Respect the nose.**

These ancient creatures been doing this since before we had stone tools. They don't need our algorithms, our task planners, or our optimization protocols. They just need space to work and someone to pick up what they find.

Your Steve agents? They're the support crew. The Sniffer's the specialist. Keep that straight, and you'll have more ancient seeds than you know what to do with.

Now get out there, find those eggs, and build a crew that'd make the ancients proud.

**Foreman out.**

---

*MineWright Construction Division* | *Training Manual Revision 1.0* | *Last Updated: Minecraft 1.20.1*

**Next Training:** Allay Coordination (Agent Guide Vol. 8) | **Previous:** Animal Handling (Agent Guide Vol. 6)