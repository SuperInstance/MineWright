# MOB FARMS CREW MANUAL
## MineWright Training Series - Volume 7

**Foreman Note:** Listen up crew. Mob farms aren't just about watching zombies fall to their doom. They're about understanding spawn mechanics, manipulating conditions, and building systems that work while you sleep. This manual covers everything from simple drop towers to Nether operations. Read it, practice it, and you'll be turning hostile mobs into resources like a pro.

---

## TABLE OF CONTENTS
1. Spawn Mechanics - Understanding the Rules
2. Farm Types - Pick Your Approach
3. Common Designs - Proven Blueprints
4. Mob-Specific Farms - Target Your Quarry
5. Nether Operations - Hellish Harvests
6. Collection Systems - Gathering the Goods
7. Kill Mechanisms - The Final Stage
8. Team Building - Large-Scale Operations
9. Efficiency Tips - Maximizing Output
10. Crew Talk - Field Dialogues

---

## CHAPTER 1: SPAWN MECHANICS

### THE SPAWNING EQUATION

Mobs don't just appear randomly - they follow rules. Learn these rules, you control the flow.

**Surface Requirements (Natural Spawning):**
- Light level 7 or lower for hostile mobs
- Solid block beneath (no glass, no slabs unless bottom-half)
- 2x2x2.5 space minimum (different mobs have different heights)
- Player within 24 blocks (spherical distance)
- Player within 128 blocks for despawning mechanics

**Spawn Rates by Location:**
- Overworld surface: Up to 70 hostile mobs naturally
- Cave systems: Same cap, but distributed across dark spaces
- Spawners: Ignore most rules, operate on their own timer

**The Spawn Cycle:**
- Game attempts spawning every game tick (20 times/second)
- Each attempt rolls for spawn locations
- Success depends on available spawnable spaces
- More spawnable blocks = more spawn attempts = more mobs

**Crew Tip:** The planning algorithm loves this stuff. Ask it to calculate spawn probabilities based on your build location. It'll crunch the numbers and tell you exactly how many spawnable blocks you're working with.

---

## CHAPTER 2: FARM TYPES

### SPAWNER-BASED FARMS

**Pros:**
- Predictable spawn rates
- Compact footprint
- Specific mob types
- No light management needed

**Cons:**
- Limited by spawner location
- Requires finding/breaking spawners
- Lower max output than natural farms

**Best For:** Early game resources, specific mob needs ( Blaze rods for brewing, Ender pearls from Nether spawners)

### NATURAL SPAWNING FARMS

**Pros:**
- Unlimited spawn potential
- Scalable design
- Works anywhere dark spaces exist
- Can target multiple mob types

**Cons:**
- Requires spawning platforms (2-3 layers minimum)
- Light management critical
- Player positioning affects rates
- Larger construction footprint

**Best For:** Mass resource production, XP farms, general mob drops

### RAID FARMS

**Pros:**
- Highest XP rates in game
- Bonus drops (Bad Omen, Totems)
- Automated via villagers and llamas

**Cons:**
- Requires village setup
- Complex redstone for automation
- Limited by raid mechanics
- Needs player intervention to start

**Best For:** Late-game XP, Totem farming, high-value drops

---

## CHAPTER 3: COMMON DESIGNS

### SIMPLE DROP TOWER

**The Basics:**
- 23-block fall damage = exactly 1 HP remaining
- Punch or sweep attack for kill + full XP
- No redstone required
- Works on all non-flying mobs

**Construction Specs:**
```
Layer 1: Spawn platform (dark, 2x2 minimum per spawn point)
Layer 2-3: Additional spawn platforms (stack vertically)
Central: 2x2 drop shaft through all layers
Bottom: Water collection to 1x1 kill zone
Height: 23 blocks from spawn to collection
```

**Crew Note:** Simple doesn't mean weak. A well-built tower with 8 spawn platforms can process 2000+ mobs per hour. Add AFK platform at spawn level for maximum efficiency.

### WATER FLUSH SYSTEM

**The Concept:**
- Mobs spawn on dry platforms
- Periodic water flush pushes them to collection
- Can be timed or manual
- Gentler than drop towers (preserves rare drops)

**Building It:**
- Use signs or trapdoors to hold water
- Flood each platform level simultaneously
- Water streams merge into central channel
- Final water delivery to kill zone

**Pro Tip:** The autonomous agent excels at building these repetitive systems. Task it with "build water flush farm layer by layer" and watch it work through the night.

### LAVA BLADE

**How It Works:**
- Thin lava layer (1 block deep, using signs/trapdoors)
- Mobs pushed through by water
- Lava damages but doesn't destroy items
- Fast kills, automatic collection

**Warning:** Lava blades destroy XP. Use for pure resource farming, not XP. Blade height is critical - too high destroys drops, too low doesn't kill. Test with sacrificial zombies before committing to production.

---

## CHAPTER 4: MOB-SIFIC FARMS

### ZOMBIE FARMS

**Drop Focus:** Rotten flesh (compost/carpet), iron ingots (rare), carrots, potatoes

**Special Considerations:**
- Zombies can pick up items (use trapdoors to prevent)
- Baby zombies have small hitbox (use carpet or trapdoor flooring)
- Can be converted to drowned for copper drops

**Design Adaptation:**
- 1.5-block high ceiling to exclude spiders
- Carpet layer to catch baby zombies
- Water system to flush drowned into deeper water for conversion

### SKELETON FARMS

**Drop Focus:** Bones (bonemeal), arrows, bows (rare enchanted), equipment

**Special Considerations:**
- Skeletons have ranged attacks (stay behind cover)
- Can't swim (water traps work well)
- Burn in sunlight (roof your farm)

**Design Adaptation:**
- Use water streams to deliver
- Full enclosure to prevent sun death
- Collection chamber with 1x1 sight lines to prevent shooting

### CREEPER FARMS

**Drop Focus:** Gunpowder (TNT, fireworks), music discs (if skeleton kill)

**Special Considerations:**
- Explosion radius = big problems
- Cats/ocelots prevent spawning
- Charged creepers rare but devastating

**Design Adaptation:**
- 3-block high corridors (creepers are tall)
- Water delivery essential (don't let them get close)
- Build away from main base (explosions happen)
- Consider TNT duplication setup ( exploits physics glitches)

### SPIDER FARMS

**Drop Focus:** String (bows, wool), spider eyes (potions)

**Special Considerations:**
- Can climb walls (need overhangs or ceiling)
- 1x1 spawn space (smaller footprint)
- Don't trigger trapdoors (unlike zombies)

**Design Adaptation:**
- Use full-block floors (no carpet)
- Ceiling overhangs or cover on walls
- Wider spawning area (exploits 1x1 spawn)
- Can't be separated from cave spiders in natural farms

### ENDERMAN FARMS

**Drop Focus:** Ender pearls (eyes, teleporting, crafting)

**Special Considerations:**
- teleport when hit (need enclosed chamber)
- Aggro from player gaze (don't look directly at them)
- Water damages them (unique weakness)
- Spawn in dark, but prefer higher spawn blocks

**Design Adaptation:**
- Build in End (best spawn rates)
- 2.5-block high minimum (they're tall)
- Water ceiling or flowing water for damage
- Snow golems to knock them into kill zone
- Player must look away during operation

---

## CHAPTER 5: NETHER FARMS

### BLAZE FORTRESS FARMS

**Target:** Blaze rods (brewing, fuel, End portal activation)

**Location:** Nether Fortresses only

**Spawn Requirements:**
- Fortress spawn rules override general Nether
- Spawners exist but natural spawning is better
- Light level irrelevant (they spawn anywhere in fortress)

**Construction:**
- Build spawn platforms around fortress
- Funnel blazes to central drop
- 23-block fall (blaze have 20 HP)
- Punch for XP or automatic kill for rods only

**Crew Alert:** Blazes shoot fireballs. Build with fire-resistant materials or design for range. The action registry has blaze-resistant materials if you need them.

### WITHER SKELETON FARMS

**Target:** Coal, bones, stone swords, Wither Skeleton skulls (3 for boss)

**Location:** Lower floors of Nether Fortresses

**Spawn Requirements:**
- Fortress spawn mechanics
- Prefer lower fortress areas
- Light level irrelevant

**Construction:**
- Same platform system as blaze farms
- Wider platforms (wither skeletons are tall)
- Watch for normal skeleton spawns (they'll clog the system)
- Skull drop is 2.5% chance - needs patience or AFK time

**Team Strategy:** Multiple crew members building spawn platforms in parallel can complete a fortress farm in an hour. Coordinate via the event bus - assign each worker a section.

### PIGLIN BARtering FARMS

**Target:** Gold nuggets, Ender pearls (rare), potions, books, quartz

**Location:** Nether wastes, near bastions (not inside)

**Mechanic:**
- Gold ingots thrown to piglins
- 8-12 second cooldown
- Random drops in return
- Zombie piglin variant in overworld = gold drops

**Design:**
- Create containment cell for piglins
- Hopper system to deliver gold ingots
- Collection system for returned items
- Manual or automated gold feeding

**Warning:** Adult piglins attack if you open containers. Use hoppers or droppers only. No chests, no furnaces in their line of sight.

---

## CHAPTER 6: COLLECTION SYSTEMS

### HOPPER NETWORKS

**Direct Collection:**
- Hoppers under kill zone catch drops
- Chain hoppers to storage system
- Slow transfer rate (2.5 items per second per hopper)
- Can clog on high-output farms

**Best For:** Small-scale farms, specific item filtering, incorporation into larger sorting systems

**Crew Tip:** The task executor can build hopper lines automatically. Just define the endpoints and let the agents lay the pipe.

### MINECART COLLECTION

**Hopper Minecarts:**
- Faster collection than static hoppers
- Can collect from full block (not just above)
- Rail-based transport to storage
- Powered rails for automatic return

**Setup:**
- Place hopper minecart on rail under kill zone
- Rail line loops to storage
- Detector rail activates system when full
- Unload into hoppers/chests at destination

**Best For:** High-output farms, remote storage integration, large-scale operations

### WATER STREAMS

**Basic Principle:**
- Mobs and items both flow with water
- Single water source block flows 8 blocks
- Ice or packed ice for fastest transport
- Signs/trapdoors hold water without flow interruption

**Construction Pattern:**
```
[Water Source] -> [Flow Channel] -> [Item Collection] -> [Hopper Storage]
     |               |                  |                    |
  (blocks)      (ice for speed)   (trapdoor floor)   (sorter system)
```

**Efficiency Hack:** Use soul sand bubble columns for vertical transport. Mobs go up, items go down. Fast, automatic, redstone-free.

---

## CHAPTER 7: KILL MECHANISMS

### FALL DAMAGE

**The Math:**
- Fall distance - 3 = damage dealt
- Each block = 1 HP (half heart)
- 23-block fall = 20 damage (leaves 1 HP on most mobs)
- Different HP = different distances (test before production)

**Advantages:**
- Preserves XP
- No resource consumption
- Silent operation
- Works on all non-flying mobs

**Disadvantages:**
- Requires precise height calculation
- Some mobs have different HP
- Flying mobs immune
- Doesn't work in Nether (ceiling too low)

**Crew Note:** The vector DB has fall damage charts for every mob. Query it before building - one wrong block means either dead mobs (no XP) or survivors (clogged system).

### LAVA KILLS

**Design Types:**
- Lava blade (thin layer, flowing water pushes through)
- Lava bath (full immersion, destroys drops)
- Lava carpet (signs hold lava, mobs walk over)

**Advantages:**
- Fast kills
- Automatic operation
- Compact design
- No player intervention needed

**Disadvantages:**
- Destroys XP
- Can destroy drops if too thick
- Fire risk (build with non-flammable blocks)
- Doesn't work on Nether mobs (fire immunity)

### PLAYER KILLS

**Sweep Attack Farms:**
- Mobs delivered at 1 HP
- Player stands in safe spot
- Sweeps kill multiple mobs
- Full XP collection

**Manual Kill Chambers:**
- Mobs trapped in 1x1 space
- Player attacks through gap
- Critical hits for faster kills
- Can use looting sword for bonus drops

**Advantages:**
- Full XP drops
- Looting enchantment works
- No automation needed
- Therapeutic (stab many zombies)

**Disadvantages:**
- Requires player attention
- Slower than automatic kills
- Boring after 10,000 kills
- Armor wear and tear

---

## CHAPTER 8: TEAM BUILDING

### COORDINATED CONSTRUCTION

**Divide and Conquer:**
- Spawn platforms (parallel construction)
- Water systems (linear assembly)
- Collection systems (specialist task)
- Kill chambers (precision work, best builder)

**Communication Protocol:**
```
Worker 1: "Spawn layer 3 complete, moving to water delivery"
Worker 2: "Copy, building collection chamber now"
Worker 3: "Starting AFK platform, need spawn height confirmed"
Foreman: "Height is 128 blocks relative to bedrock, proceed"
```

**Parallel Building Patterns:**
- Vertical partitioning: Each worker takes a layer
- Horizontal partitioning: Each worker takes a quadrant
- System partitioning: Each worker takes a subsystem (spawn/transport/kill)

### ROLE SPECIALIZATION

**Platform Builders:**
- Fast material placement
- Consistent spacing
- Light management expertise
- Efficiency optimization (2x2 vs 1x1 patterns)

**System Engineers:**
- Water flow mechanics
- Redstone timing
- Hopper logic
- Minecart routing

**Quality Control:**
- Verify spawn conditions
- Test water flows
- Check kill distances
- AFK position validation

**The Foreman's Job:**
Keep everyone working, prevent bottlenecks, maintain quality standards. The agent state machine tracks crew member states - use it to identify who's idle and who's blocked.

---

## CHAPTER 9: EFFICIENCY TIPS

### SPAWN RATE OPTIMIZATION

**Maximize Spawnable Blocks:**
- 2x2 platforms give 4 spawn attempts per platform
- 21-block spacing between platforms (spawn cycles)
- Up to 8 layers maximum (hard spawn cap)
- Remove competing dark spaces (light up nearby caves)

**Player Positioning:**
- 24-block radius for spawn activation
- Center your AFK spot among all platforms
- 128-block maximum distance for despawning
- Use boat/n Portal trick to stay active without mobs spawning on you

**AFK Tricks:**
- Hold F3 + walk slowly (reduces chunk loading, increases spawns)
- Use auto-clicker for sweep attacks (check server rules)
- Watch Netflix, read manual, coordinate other builds

### SCALING CONSIDERATIONS

**Vertical Scaling:**
- Build up, not out (saves horizontal space)
- Use Nether portal transport between layers
- Water elevators for mob movement
- Ender pearl teleport for player access

**Horizontal Scaling:**
- Multiple separate farms
- Centralized storage
- Item sorting for combined drops
- Red dust wires for status signals

**Diminishing Returns:**
- 4 layers = 80% efficiency
- 6 layers = 95% efficiency
- 8 layers = 99% efficiency (same effort for 4% gain)

**Crew Math:** Sometimes two small farms outperform one mega-farm. The collaborative build manager can simulate spawn rates before you commit to construction. Query it, compare options, pick the winner.

### TROUBLESHOOTING

**No Mobs Spawning:**
- Light level too high
- Player too far
- Spawn cap reached
- Wrong block type (transparent blocks don't work)

**Mobs Not Dying:**
- Fall distance wrong
- Lava too thick/thin
- Wrong mob type (HP variation)
- Water pushing mobs away from hazard

**Collection Issues:**
- Hoppers full
- Minecart stuck
- Items despawning (5-minute timer)
- Wrong collection point (drops going elsewhere)

**Performance Problems:**
- Too many entities (clear with kill command)
- Redstone clock too fast (lag)
- Chunk loading issues (spawn chunks)
- Mob counting error (despawning not happening)

---

## CHAPTER 10: CREW TALK

### DIALOGUE 1: PLANNING PHASE

**Foreman:** "Alright crew, we're building a mob farm on this ridge. High elevation, nearby cave systems we'll need to light up. What's the spawn analysis?"

**Builder 1:** "Ran the numbers through the planning algorithm. We've got solid stone at Y=120, natural caves 30 blocks down. If we build 6 spawn layers, we're looking at roughly 2,400 mobs per hour at optimal rates."

**Foreman:** "And the competing dark spaces?"

**Builder 1:** "Already calculated. We'll need to torch out three cave systems and ravine. That's 8,000 blocks of lighting work. About two hours with three workers on torch placement."

**Foreman:** "Do it. Builder 2, you're on spawn platforms. Builder 3, water delivery system. I want to see mobs flowing within three hours. Move."

### DIALOGUE 2: CONSTRUCTION ISSUES

**Builder 1:** "We got a problem on Layer 4. Water's not flowing right. I've got two tiles where the stream stops and just sits there."

**System Engineer:** "Let me check. ...Yeah, you've got ice in the flow line. Ice speeds it up, but you mixed regular ice with packed ice. Different flow rates. The water's trying to sync and getting stuck."

**Builder 1:** "So replace the packed ice?"

**System Engineer:** "No, replace the regular ice. Go all packed ice or all regular ice. Mixed doesn't work. Also, check your signs - if any of them broke, water won't flow over them."

**Foreman:** "You heard the engineer. Layer 4 needs ice replacement and sign inspection. Get it done before we finish Layer 5. We don't want to rebuild this whole system."

### DIALOGUE 3: EFFICIENCY DEBATE

**Builder 1:** "Eight layers. That's what the manual says. Maximum spawn efficiency."

**Builder 2:** "But six layers is 95% efficiency. Why build two more layers for 4% more spawns? We're talking 200 extra blocks of material, 2 hours of work, for 100 mobs per hour."

**System Engineer:** "Depends on what you're farming. If this is for XP, that 4% matters over long AFK sessions. If it's for string and bones, you're right - diminishing returns hit hard."

**Foreman:** "It's a general farm. We're not specializing. Build six layers, reallocate that effort to the raid farm we're planning next week. Efficiency isn't just about spawn rates - it's about construction time too."

### DIALOGUE 4: KILL MECHANISM TROUBLESHOOTING

**Builder 1:** "Zombies are surviving the fall. I timed it - 23 blocks exactly, but some are at half health, some are at quarter health. Inconsistent."

**System Engineer:** "Let me check your shaft. ...Yeah, see this? You've got water flowing down the shaft. Mobs are falling through water - that reduces fall damage. They're not taking the full 23-block equivalent."

**Builder 1:** "How'd water get in there?"

**System Engineer:** "Probably the spawn platforms. You've got a leak somewhere, water's dripping down the shaft. Either seal the platforms or extend the shaft by three blocks to compensate."

**Foreman:** "Extend the shaft. Sealing the platforms takes longer. Test it with sacrificial zombies before you call it done. I want zero survivors - we don't want the kill chamber clogging up."

### DIALOGUE 5: TEAM COORDINATION

**Foreman:** "Status check. We've got one hour until nightfall. Where are we?"

**Builder 1:** "Spawn platforms done. All six layers placed and darkened. Moving to water delivery now."

**Builder 2:** "Halfway through water system. The spiral shaft is tricky - had to rebuild it twice. Should be done in 30 minutes."

**System Engineer:** "Collection chamber is ready. Hoppers are in place, sorting system is wired, just waiting on the water delivery to connect."

**Foreman:** "Builder 3, you're on AFK platform and final touches. Once the water's flowing, we test. I want zombies falling before the sun comes up. Anyone need extra hands?"

**Builder 2:** "I could use help on the spiral. The corners keep leaking."

**Foreman:** "Builder 1, you're done with platforms. Go assist Builder 2. Two workers on the spiral, we'll cut that time in half. System Engineer, prep the kill chamber while we wait. Let's move like we've got a purpose."

---

## FOREMAN'S FINAL NOTES

Mob farms are about understanding spawn mechanics and exploiting them. Give mobs the darkness they need, funnel them into a system, and collect the drops. Simple in concept, takes some doing in practice.

The vector database's got plenty of designs if you need reference. The planning algorithm can calculate spawn rates. The autonomous agents can build the repetitive parts. But the real work - the coordination, the problem-solving, the optimization - that's on you, crew.

Build it right, build it once. A good mob farm pays dividends for the life of the world. A bad one gets torn down and rebuilt.

**Now get to work. Those zombie skeletons aren't gonna kill themselves.**

---

*MineWright Training Manual - Volume 7: Mob Farms*
*Generated for the MineWright AI Project - Autonomous Minecraft Construction Crews*
*Last Updated: 2026-01-10*
