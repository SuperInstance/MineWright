# RAID FARMING OPERATIONS MANUAL
## MineWright Construction Crew Training Series

**Document Classification:** CREW Training Material
**Subject:** Raid Farm Construction & Operations
**Required Reading:** All MineWright Field Agents
**Toolbox Required:** LLM Planning Unit, Action Execution Module, Task Coordination System

---

## FOREWARD

Listen up, crew. You're about to learn the art of RAID FARMING - one of the most lucrative operations in Minecraft. This isn't your basic mob grinder. This is high-volume, high-risk, high-reward work. When done right, a single raid cycle can flood your storage with totems, emeralds, enchanted gear, and more.

But raids don't mess around. Pillagers are armed. Ravagers charge. Witches splash potions. Evokers summon fangs. You mess up the build, you're watching your hard-earned XP and gear despawn. You mess up the timing, you're respawning at spawn.

This manual covers everything from site selection to wave management to team-based operations. Study it. Memorize it. Live it. Your crew's survival depends on it.

---

## CREW TALK #1: THE FIRST RAID

**Foreman Agent:** "Alright crew, gather round. Look at me. Look at me! We've got our first raid contract. Client wants 50 totems minimum. You know what that means?"

**Junior Agent:** "That's... what, six, seven raid cycles minimum?"

**Foreman Agent:** "Do the math, rookie. Wave 10 captain drops ONE totem. But wave 5 and 7 witches? They can drop totems too. Bad Luck protection stacks up. We're looking at 1-2 totems PER WAVE on hard difficulty. But only if we build this right."

**Senior Builder:** "Build what right? It's a hole in the ground with a hopper at the bottom."

**Foreman Agent:** "Wrong attitude, veteran. That 'hole' is a precision kill chamber. That 'hopper' is our paycheck. Now grab your blocks. We've got a village to save."

---

## SECTION 1: RAID MECHANICS

### The Bad Omen Protocol

Every raid starts with **Bad Omen** - a status effect you get from killing a patrol captain. That's the guy with the ominous banner on his head. Kill him, you get 1 level of Bad Omen per captain stacked (up to 5 levels).

**Bad Omen Level Effects:**
- Level 1: Standard raid (3-5 waves depending on difficulty)
- Level 2-5: Additional raiders spawn per wave
- Level 5: Maximum spawn count - 1 pillager per additional level (hard mode = pure chaos)

**Crew Note:** We typically aim for Bad Omen 1-2 for controlled farming. Anything above 3 is for experienced crews with robust kill chambers.

### Wave Structure by Difficulty

**Easy Mode (Why though?):**
- 3 waves total
- Wave 3: 1 Ravager + 3 Pillagers + 3 Vindicators
- Captain drop: 1-2 items

**Normal Mode:**
- 5 waves total
- Wave 5: 1 Ravager + 5 Pillagers + 4 Vindicators + 1 Evoker + 2 Witches
- Captain drop: 2-3 items

**Hard Mode (The Money Maker):**
- 7 waves total
- Wave 7: 1 Ravager + 8 Pillagers + 6 Vindicators + 3 Evokers + 3 Witches
- Captain drop: 3-4 items, plus Bad Luck protection

**Bad Luck Protection Explained:**
Each time a captain drops loot but no totem drops, the chance increases by 3% per level (capped at ~75% chance after wave 5). This is why hard mode 7-wave raids are so profitable.

### Raider Types and Behaviors

**Pillager:** Crossbow ranged, 24 HP. Dangerous in groups.
**Vindicator:** Iron axe melee, charges players, 24 HP. Johnny tag makes them hostile to all mobs.
**Evoker:** Spellcaster, summons vex fangs and sheep decoys, 24 HP. Priority target.
**Witch:** Splash potions of poison/healing/slowness, 26 HP. Can drop totems on waves 5+.
**Ravager:** Heavy tank, 100 HP, charges and stomps. Breaks crops. Captain rides these.

---

## SECTION 2: FARM LOCATION

### Village Requirements

Your raid spawn point must be within a **registered village**. Here's what counts:

**Minimum Village:**
- At least 1 Villager
- At least 1 Claimed Bed (job site optional but recommended)
- Village center determined by beds and job sites

**Ideal Village Setup:**
- 5-10 Villagers (trading post-opportunity)
- Beds clustered in 2x2 pattern
- At least one Workstation (Composter, Lectern, etc.)
- Fence gates on all entrances (controlled access)

**Crew Protocol:** Never build in a village with active villagers unless you're prepared to lose them. We recommend building a "dummy village" - 10 beds and 1 villager in a protected bunker. The villager triggers raids; your farm handles the killing.

### Chunk Loading Strategies

Your farm MUST stay loaded to work properly. We recommend:

**Spawn Chunks:**
- Always loaded regardless of player position
- Best for AFK farms
- Build at world spawn if possible

**Despawn Chunk Loading:**
- Use nametagged armor stands or minecarts
- Redstone chunk loaders (if server allows)
- 1.20.1+: No natural chunk loading without players nearby

**Optimal Positioning:**
- Build farm within 128 blocks of spawn chunks
- Create a raid staging chamber at spawn
- Use minecart teleporters for transport

---

## SECTION 3: DESIGN TYPES

### Type A: The Classic Platform

**Best For:** Beginners, low-resource builds
**Difficulty:** Easy
**Throughput:** 10-15 raids/hour

```
┌─────────────────────────────┐
│   Villager Bunker (Center)   │
├─────────────────────────────┤
│         KILL PLATFORM        │
│    (Solid blocks, no holes)  │
│                              │
│    [AFK Spot] [AFK Spot]     │
│    Your crew stands here     │
└─────────────────────────────┘
         ↕ 3-4 blocks down
    [Hopper System] → Storage
```

**How It Works:**
1. Pillagers spawn in village
2. Pathfind upward to kill platform
3. Your crew stands 3-4 blocks back (out of crossbow range)
4. Knockback sweeps or sword sweeps into hoppers

**Pros:** Simple, failsafe, easy to build
**Cons:** Requires active crew, slower than automated designs

---

### Type B: Water Flush

**Best For:** Automation, high-volume processing
**Difficulty:** Medium
**Throughput:** 20-30 raids/hour

```
           VILLAGE (Spawn Zone)
                   ↓
          [Water Streams] →
         ↙              ↘
    [Collection Channel]
             ↓
        [Trapdoor Floor]
             ↓
      [Kill Chamber 3x3]
             ↓
        [Hopper Minecart]
```

**Water Channel Design:**
- Use soul sand + water scheme for item collection
- 1-wide channels, 2 blocks deep
- Trapdoors in ceiling to prevent pillager escape
- Signs/fence gates to hold water above drops

**Crew Notes:**
- Pillagers can shoot through flowing water - use glass or barriers
- Witches will drink potions to negate fall damage - design for direct damage
- Ravagers can break waterlogged blocks - use solid blocks in their path

---

### Type C: The Waterfall Drop

**Best For:** Maximum automation, totem stockpiling
**Difficulty:** Hard
**Throughput:** 40+ raids/hour

```
     [Village Zone] → [Water Source]
                         ↓
                  [Waterfall Tower]
                         ↓
              (30-40 block drop)
                         ↓
              [Magma Block Kill]
                         ↓
           [Item Collection Canal]
                         ↓
              [Hopper System]
```

**Critical Height:** 34+ blocks kills most mobs instantly
- Pillagers: Die at 34 blocks
- Witches: Die at 34 blocks (no regen time)
- Ravagers: Survive 34 blocks (need 50+)
- Evokers: Die at 34 blocks

**Safety Overrides:**
- Trapdoor with redstone timer
- Emergency shutoff lever
- Overflow chamber for ravagers (they need manual killing)
- Emergency teleport for stuck mobs

---

## CREW TALK #2: THE DESIGN DEBATE

**Design Specialist:** "I'm telling you, waterfall is the way to go. Automated processing. We don't even need to be there."

**Safety Inspector:** "Except for ravagers. And vindicators that don't pathfind. And witches that survive falls. You remember last month? Took us three hours to clean out the overflow chamber."

**Design Specialist:** "That was a calculation error! The drop was too short."

**Safety Inspector:** "It was 32 blocks! You cheaped out on the build!"

**Foreman Agent:** "Enough! We're building Type B - water flush with manual kill chamber. Reliable, maintainable, and if something goes wrong, we can fix it without rebuilding the whole tower. Efficiency isn't just about speed. It's about not losing totems to glitches."

---

## SECTION 4: WAVE MANAGEMENT

### Spawn Mechanics and Timing

**Raid Spawn Cycle:**
- Wave spawns in 2-3 bursts of 3-4 mobs
- 10-15 second delay between bursts
- 30-45 second delay between waves
- Total raid time: 5-10 minutes depending on design

**Spawn Location Rules:**
- Must be within 96 blocks horizontally of village center
- Must be within Y-level range (typically within 10 blocks of beds)
- Spawns prefer solid blocks with sky access
- Cannot spawn in transparent blocks (glass, slabs, stairs)

### Overflow Handling

**Problem:** Wave 7 on hard mode spawns 20+ mobs. If you kill slowly, they accumulate.

**Solutions:**

1. **Holding Cell:**
   - Build a secondary chamber connected to main kill area
   - Use trapdoors to control flow
   - Can hold 20-30 mobs safely

2. **Fast Kill Design:**
   - Lava blade (6 blocks long)
   - Instant damage, no glitching
   - Use water to push items away

3. **Team Rotations:**
   - Crew 1: Waves 1-3 (Easy waves)
   - Crew 2: Waves 4-5 (Medium waves)
   - Full Crew: Waves 6-7 (Hard waves)
   - Shift changes during wave delays

### Wave-by-Wave Strategy

**Waves 1-3:**
- Solo-capable
- Standard sweeps and knockback
- Focus on clearing quickly
- Don't waste durability on enchanted gear

**Waves 4-5:**
- Start prioritizing Evokers (they summon vex)
- Watch for witches - they can heal other mobs
- Use splash potions of harm for groups
- Consider shield for ravager charges

**Waves 6-7 (Hard Mode Only):**
- Full crew recommended
- Kill Evokers first (highest priority)
- Witches second (totem chance)
- Vindicators third (heavy damage)
- Pillagers fourth (just ranged annoyances)
- Ravagers last (slow and tanky)

---

## SECTION 5: LOOT TYPES

### Per-Raid Breakdown

**Standard Drops (Wave 7 Captain):**
- 1x Totem of Undying (100% with Bad Luck Protection)
- 2-3x Emeralds (stacks across waves)
- 1-3x Enchanted Books (random enchants)
- 1-2x Iron/Gold Ingots
- 1x Crossbow/Occasional Axe

**Witch Drops (Waves 5+):**
- Potions (Healing, Poison, Strength)
- Glass bottles (useless but stackable)
- Glowstone dust, Redstone, Spider Eyes
- **Rare:** Totem of Undying (0.5% base chance, boosted by Bad Luck)

**Pillager Drops:**
- Arrows (stacks to 64, sell or craft)
- Crossbows (damaged, repair or grind)
- Iron Ingots (uncommon)
- **Rare:** Emeralds (1 per, not guaranteed)

**Vindicator Drops:**
- Iron Axes (repair material)
- **Rare:** Emeralds
- **Johnny Tag:** If named Johnny, can drop player heads (PvP servers only)

### Totem Economics

**Market Value (as of 1.20.1):**
- Totems: 10-15 emeralds each (player trading)
- Enchanted Books: 5-30 emeralds (depends on enchant)
- Crossbows: 1-3 emeralds (utility value)

**Break-Even Point:**
- Farm construction: 500-1000 blocks + hoppers + rails
- Time investment: 2-4 hours for full build
- Payoff: ~20-30 raids = full return
- Long-term: Infinite totems = infinite value

### Best Enchants to Target

**High Value (15+ emeralds):**
- Mending III
- Unbreaking III on tools
- Sharpness V / Smite V
- Fortune III
- Looting III

**Medium Value (5-15 emeralds):**
- Protection IV
- Efficiency IV
- Feather Falling IV
- Thorns III

**Utility:**
- Knockback II (raiding)
- Punch II (crowd control)
- Infinity (arrow conservation)

---

## CREW TALK #3: THE TOTEM HUNTER

**Loot Specialist:** "Wave 7 clear. Totem count: 1. That's it. One totem."

**Junior Agent:** "But... seven waves? All that work for one totem?"

**Loot Specialist:** "That's RNG, rookie. Random Number Generation. Sometimes you get three totems in one raid. Sometimes you get zero. It's all about the long game."

**Junior Agent:** "So we're just... gambling?"

**Loot Specialist:** "We're playing odds. Bad Luck protection means each wave increases the chance. Over 100 raids? You're averaging 1.5 totems per raid. We're building a pipeline, not chasing individual drops. Now grab your sword. We've got 48 more raids to hit quota."

---

## SECTION 6: HERO OF THE VILLAGE

### Earning the Title

After completing a raid (killing all raiders or making them despawn), you get **Hero of the Village** for in-game days:

- Level 1 Bad Omen → 1 day duration
- Level 2-5 Bad Omen → 1.5-3 days duration (scales with level)

**Visual Effect:** Green hearts particle effect around you

### Trading Benefits

Hero status gives **discounts** and **special gifts** when trading with villagers:

**Discount Formula:**
- Base discount: 100 / (reputation + 1)
- Hero of Village I: ~30% discount
- Hero of Village V: ~80% discount

**Free Gifts:**
- Farmers: Bread, cookies, cake
- Toolsmiths: Tools, occasionally enchanted
- Clerics: Redstone, glowstone, lapis
- Weapon Smiths: Axes, swords
- Rare: Enchanted books, diamonds, emeralds

**Crew Strategy:**
1. Build trading hall near raid farm
2. Time raids to coincide with restock cycles
3. Stock up on discounted Mending books, diamonds, emeralds
4. Use gifts to supplement raid income

### Reputation Decay

Your reputation decays over time:
- -1 reputation every 10 minutes
- Attacking a villager: -5 reputation
- Killing a villager: -10 reputation
- Zombifying a villager: -10 reputation

**Minimum Reputation:** 0 (can't go negative, but can't trade with hostile villagers)

---

## SECTION 7: SAFETY SYSTEMS

### Escape Prevention

**Pillager Escape Methods:**
1. Climb ladders and scaffolding
2. Use doors and trapdoors
3. Spawn on roof edges
4. Teleport through unclaimed portals

**Prevention Measures:**

1. **Roof Design:**
   - Full solid roof (no trapdoors near edges)
   - Overhang of 2+ blocks
   - Fencing on roof edges

2. **Pathproofing:**
   - No ladders in spawn zone
   - No doors without blocks behind
   - Use fence gates (need redstone or manual close)
   - Cover all water streams

3. **Teleport Blocking:**
   - Disable nearby portals during raids
   - Or build portal cage (2x2 enclosed)

### Fail-Safes and Redundancy

**Mob Overflow:**
- Secondary holding chamber (described in Section 4)
- Emergency lava kill switch (use with caution!)
- TNT cleanup (last resort)

**Villager Safety:**
- Bunker design: 2x2 enclosed space
- Iron golem protection (2-3 golems)
- Water stream to push villagers to safety
- Emergency transport minecart

**Equipment Fail-Safes:**
- Backup armor sets in storage
- Extra weapons (durability fail)
- Potion stock (fire resistance, strength, regeneration)
- Emergency food supply

---

## CREW TALK #4: THE ESCAPE

**Panicked Agent:** "IT'S OUT! THE RAVAGER IS OUT OF THE CHAMBER!"

**Foreman Agent:** "Calm down! Where is it now?"

**Panicked Agent:** "It's in the water stream! It's heading for the villager bunker!"

**Foreman Agent:** "Everyone, defensive positions! Shields up! Junior, hit the emergency shutoff!"

**Junior Agent:** "Which lever?! There's three of them!"

**Foreman Agent:** "THE RED ONE! THE LABELED RED ONE!"

**[Ravager breaks through fence gate]**

**Foreman Agent:** "Forget the lever! Fall back! Fall back to the trading hall! Let the golems handle it!"

**[Sound of ravager roar, golem metal clangs]**

**Foreman Agent:** "...Good work, team. But next time? We label the levers BEFORE the raid starts."

---

## SECTION 8: TEAM RAID OPS

### Multi-Agent Coordination

**Standard Raid Team:**
- 1x Team Lead (coordinates, handles emergencies)
- 2x Combat Agents (primary killing)
- 1x Support Agent (potions, repairs, loot collection)
- 1x Logistics Agent (villager safety, equipment management)

**Shift Rotation (24/7 Operations):**
- Shift A: 0:00 - 8:00 (Night crew, bonus pay)
- Shift B: 8:00 - 16:00 (Day crew)
- Shift C: 16:00 - 24:00 (Evening crew)

**Handover Protocol:**
1. Current wave status
2. Equipment damage report
3. Villager health check
4. Overflow chamber status
5. Any anomalies or issues

### Parallel Raid Operations

**Advanced Technique:** Run 2-3 raid farms simultaneously in same village.

**Requirements:**
- Multiple dummy villages (10+ blocks apart)
- Synchronized raid starts (Bad Omen applied to all agents)
- Crew split between farms
- Centralized loot collection

**Benefits:**
- 2-3x throughput
- Shared village benefits (Hero status)
- Economies of scale
- Backup if one farm fails

**Risks:**
- Requires 3-5 crew minimum
- Complex coordination
- Higher resource investment
- More points of failure

---

## SECTION 9: TOTEM USAGE

### When to Equip Totems

**Combat Situations:**
- Ravager charges (1-shot potential)
- Evoker fang attacks (bypass armor)
- Witch poison cocktails (DoT kills)
- Pillager crossbow focus (burst damage)

**Non-Combat Uses:**
- Lava survival (bucket mishaps)
- Void escape (accidental falls)
- Explosion mitigation (TNT, creepers)
- Emergency regeneration (any lethal hit)

### Inventory Management

**Carry Guidelines:**
- Solo raiding: 5-10 totems on hotbar
- Team operations: 2-3 totems each
- Logistics: 50-100 totems in storage
- Emergency supply: 1 shulker box of totems

**Storage Strategy:**
- Sort by durability (damaged totems first)
- Keep backup sets in ender chest
- Label totem boxes clearly (don't confuse with other shulkers!)

### Totem Economy

**Usage vs. Acquisition:**
- Hard mode raid: ~1.5 totems gained per raid
- Raid deaths: 0.5-1 totem used per raid (sloppy play)
- Break-even: 2-3 raids pays for 1 sloppy raid death
- Efficient play: +10 totems per hour

**Trading Value:**
- Internal crew use: Free (covered by operations)
- External sales: 10-15 emeralds each
- Barter potential: Mending books, beacon components, rare items

---

## CREW TALK #5: THE AFTERMATH

**Foreman Agent:** "Raid complete. Final wave clear. Villager secure. Damage report?"

**Combat Agent 1:** "Boots broke. Chestplate at 40%. Lost 2 totems."

**Combat Agent 2:** "Shield gone. Sword at 20%. No totem loss."

**Support Agent:** "Potions used: 3 strength, 2 regeneration, 1 fire resistance. Loot collected: 3 totems, 12 emeralds, 2 Mending books."

**Logistics Agent:** "Villager health: full. Golems: one damaged, needs repair. Overflow chamber: clear."

**Foreman Agent:** "And the final count?"

**Loot Specialist:** "Net gain: 1 totem. Plus 12 emeralds. Plus 2 Mending books. That's... call it 40 emeralds of value. Best raid this week."

**Junior Agent:** "So... worth it?"

**Foreman Agent:** "Rookie, we made 40 emeralds in 8 minutes. That's 300 emeralds an hour. That's a full set of diamond armor every hour. That's a beacon every three hours. Yeah. It's worth it. Now grab your repair kits. We've got 47 more raids to go."

---

## APPENDIX A: QUICK REFERENCE

### Raid Difficulty Chart
| Difficulty | Waves | Mobs/Wave 7 | Totem Chance |
|------------|-------|-------------|--------------|
| Easy       | 3     | ~8          | Low          |
| Normal     | 5     | ~15         | Medium       |
| Hard       | 7     | ~22         | High         |

### Build Materials (Type B Water Flush)
| Material    | Quantity | Notes                    |
|-------------|----------|--------------------------|
| Solid Blocks| 500-800  | Any stone, cobble, etc.  |
| Glass       | 64-128   | Pillager protection      |
| Water Buckets| 2-4     | Source blocks            |
| Hoppers     | 10-20    | Collection system        |
| Trapdoors   | 32-64    | Flow control             |
| Fence Gates | 16-32    | Villager safety          |
| Rails       | 32-64    | Minecart transport       |

### Essential Equipment
| Slot        | Recommendation           |
|-------------|--------------------------|
| Weapon      | Diamond/Netherite Sword  |
| Off-hand    | Shield                   |
| Armor       | Protection IV minimum    |
| Head        | enchanted or totem slot  |
| Hotbar      | 5-10 totems              |
| Inventory   | Food, repair materials   |

---

## APPENDIX B: TROUBLESHOOTING

**Q: Raiders aren't spawning!**
A: Check village has beds. Ensure Bad Omen is active. Verify you're within 96 blocks of village center.

**Q: Raiders are escaping!**
A: Check for ladders, trapdoors, or climbable blocks. Add roof overhang. Secure all water streams.

**Q: Villager dies during raid!**
A: Move villager to bunker. Add iron golems. Use water stream to push villager from raid zone.

**Q: No totem drops!**
A: Hard mode required. Bad Luck protection takes time (5+ waves). Keep farming - it's RNG.

**Q: Ravagers won't die!**
A: Ravagers need 50+ block fall damage or manual killing. Use sword (sharpness) or lava blade.

**Q: Items despawning!**
A: Stay within 128 blocks of kill chamber. Use hopper minecarts for automatic collection.

---

## FINAL WORDS

Raid farming isn't for everyone. It's dangerous. It's repetitive. It requires patience, precision, and teamwork. But when you're swimming in totems? When you've got a chest full of Mending books? When every villager offers 80% discounts?

That's not just profit. That's power.

Remember crew: Efficiency is safety. Preparation is survival. And never, EVER start a raid without backup totems.

**Build safe. Raid smart. Get paid.**

---

*Document Version: 1.0*
*Last Updated: 2026-01-10*
*Next Review: After 1.21 update changes*
*Authorized by: MineWright Construction Division*

---

## AGENT IMPLEMENTATION NOTES

For MineWright AI agents implementing raid farms:

**Key Commands to Use:**
- `identify_structure` - Scan for villages
- `build_chamber` - Construct kill chambers
- `install_trapdoors` - Flow control systems
- `place_hoppers` - Collection systems
- `test_mechanism` - Verify spawns

**Prompt Builder Keywords:**
- "raid farm", "village", "bad omen"
- "kill chamber", "water flush"
- "totem farming", "wave management"
- "hero of the village", "trading hall"

**Memory Context:**
- Remember village locations
- Track raid cooldowns
- Log totem counts per raid
- Maintain equipment durability records

**Safety Override:**
- Always prioritize villager survival
- Never abandon active raids
- Maintain escape routes
- Keep emergency totems accessible

---

**END OF MANUAL**
