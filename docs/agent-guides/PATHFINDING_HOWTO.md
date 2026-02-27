# PATHFINDING - Crew Manual

**Issued By:** Site Management
**For:** All Divisions
**Last Updated:** 2026-02-27
**Priority:** ESSENTIAL READING FOR ALL CREW

---

## The Basics

Getting from point A to point B sounds simple until you're actually doing it. This manual covers everything about navigation—basic movement, handling obstacles, avoiding hazards, coordinating with the crew, and getting unstuck when things go wrong. Read it, learn it, and you'll spend less time lost and more time working.

**Bottom line:** Good pathfinding's not about being fast—it's about being smart, safe, and predictable.

---

## 1. Basic Navigation

### Walking Speeds & Techniques

| Terrain | Speed (blocks/sec) | Notes |
|---------|-------------------|-------|
| Flat ground (walking) | 4.3 | Standard pace |
| Flat ground (sprinting) | 5.6 | Uses hunger, 3x faster |
| Dirt path | 4.3 | Same speed as grass |
| Soul sand | 1.8 | Slow going, plan accordingly |
| Mud (swamp) | 2.5 | Reduced speed |
| Ice (packed) | 40+ | Very fast, low control |
| Ice (blue) | 70+ | Extremely fast, be careful |

**The Crew Way:**
- Default to walking—save sprinting for when it matters
- If you're on ice and didn't plan for it, that's on you
- Check the terrain before you commit—soul sand'll cost you seconds

### Jumping Mechanics

| Jump Type | Height | Distance | When to Use |
|-----------|--------|----------|-------------|
| Standard jump | 1.25 blocks | ~1 block | Clearing small obstacles |
| Running jump | 1.5 blocks | 2-3 blocks | Crossing gaps |
| Sneak-jump | 1.5 blocks | 1 block | Precision landings |
| Head bonk | 2 blocks | 0 | Checking ceiling clearance |

**Jump Tips:**
- Jumping costs momentum—only jump when you need to
- Running jumps clear 2-block gaps reliably (3 if you're lucky)
- If you're unsure about a gap, test it or find another way
- Head bonking tells you there's something above—note it for the vector DB

### Swimming

| Water Type | Surface Speed | Underwater Speed | Notes |
|------------|---------------|------------------|-------|
| Still water | 2.2 | 1.2 | Slow going |
| Flowing water | varies | 1.2 | Current affects you |
| Water (sprinting surface) | 4.6 | - | Faster than walking |
| Underwater (sprinting) | - | 1.9 | Best option when submerged |

**Water Navigation:**
- Don't fight strong currents—go with the flow, exit when you can
- Surface swim when possible—twice the speed
- Sprint swimming beats walking on flat ground—use rivers as highways
- Check depth before committing—deep water'll cost you time

### Climbing

| Climbable | Speed | Notes |
|-----------|-------|-------|
| Ladders | 2.3 blocks/sec | Fast vertical |
| Vines | 0.6 blocks/sec | Very slow, emergency only |
| Scaffolding | variable | Auto-climb when you touch |
| Waterfall | 2.3 blocks/sec | Same as ladder |
| Trapdoor (open) | 3.0 blocks/sec | Fastest vertical option |

**Climbing Rules:**
- Ladders and waterfalls are your friends—use them
- Vines are last resort—too slow unless you've got no choice
- Scaffolding's for construction, not navigation—but good to know
- Always check what's at the top before climbing up

---

## 2. Obstacle Handling

### Gap Crossing

**Standard Gap Clearances:**
- 1 block gap: Walk across (no jump needed)
- 2 block gap: Running jump (95% success)
- 3 block gap: Running jump (50% success) - bridge it instead
- 4+ block gap: Don't even try—build a bridge or find another way

**Crew Techniques:**

**The Bridge Check:**
> "Gap's 3 wide. I could try for it, but bridge is smarter. Who's got blocks?"

**The Water Fill:**
> "Gap's too wide. I'm dropping water, swimming across. Takes longer but it works."

**The Scaffold Pillar:**
> "Can't clear this. Going up, scaffolding over, down the other side. 20 seconds."

### Wall Climbing

**1-Block Walls:**
- Jump and place block underneath (pillaring)
- Stack blocks, jump up, mine your way down
- Use existing ledges or stair patterns

**2-Block Walls:**
- Pillar up, jump and place, repeat
- Build stairs against the wall
- Look for alternative routes—sometimes going around's faster

**3+ Block Walls:**
- Serious vertical construction needed
- Consider scaffolding or ladders
- Redstone pistons for temporary elevation

**Wall Handling Protocol:**
```
1. Check for existing handholds (cracks, stairs, partial blocks)
2. Assess wall height—is it worth climbing?
3. Consider going around—sometimes longer is faster
4. If climbing, plan your descent before you ascend
5. Note the wall location in vector DB for future reference
```

### Water Navigation

**Crossing Rivers:**
- Check width—under 3 blocks, jump it
- 3-5 blocks: Swim or bridge
- 5+ blocks: Boat or find a narrow crossing
- Always check flow direction—downstream's easier

**Underwater Navigation:**
- Sprint swim when you can (1.9 blocks/sec)
- Place torches or glowstone to mark your path
- Air management: 15 seconds before damage
- Don't fight currents—work with them

**Ice Crossing:**
- Packed ice: 40+ blocks/sec—use it when available
- Blue ice: 70+ blocks/sec—highway speed
- Build ice roads for frequently-traveled routes
- Soul sand under ice = instant stop—use for braking zones

---

## 3. Vertical Movement

### Stairs

**Stair Speeds:**
- Walking up stairs: 2.5 blocks/sec
- Sprinting up stairs: 3.5 blocks/sec
- Jumping up stairs: 4.0 blocks/sec (but tiring)
- Going down: Gravity assists—just be careful

**Stair Patterns:**
- Normal stairs: 1:1 vertical (safe)
- Spiral stairs: Compact, space-efficient
- Slab stairs: Slower, saves materials
- Jump stairs: Fastest, costs hunger

**The Crew Way:**
- Build stairs, not ladders, for permanent routes
- Spiral stairs for tight spaces
- Always include landings every 16 blocks (safety)

### Ladders

**When to Use Ladders:**
- Vertical shafts (mines, elevators)
- Temporary access during construction
- Emergency exits
- Where space is extremely limited

**Ladder Placement Rules:**
- Every other block saves materials, slower climb
- Contiguous placement is faster, costs more
- Always add a lip at the top (don't climb off into a fall)
- Place blocks at the bottom to prevent accidental entry

**Ladder Speed:**
- Up: 2.3 blocks/sec
- Down: 3.0 blocks/sec (gravity assist)
- No hunger cost (unlike jumping)

### Scaffolding

**Scaffolding Mechanics:**
- Auto-climb when you touch it
- Descend by sneaking
- 6 blocks before it starts leaning
- Breaks instantly from bottom

**Construction Use:**
- Build up as you work
- Place from the ground (no need to climb)
- Remove from bottom when done
- Note: Not for permanent structures

**Scaffolding Speed:**
- Up: variable (based on placement speed)
- Down: instant (sneak to descend)
- Most efficient vertical for construction

### Pillar Jumping

**The Technique:**
1. Look down
2. Jump and place block
3. Repeat until at height
4. Mine down to descend (or use water/gravel)

**Pros:**
- Fast vertical ascent
- No special materials needed
- Works anywhere

**Cons:**
- Uses hunger (sprinting jump)
- Requires precision
- Descending's slow (mining down)

**Crew Protocol:**
> "Pillaring up 15 blocks. Holler if you need anything at this elevation."

---

## 4. Long-Distance Pathfinding

### Straight-Line vs. Safe Paths

**Straight-Line (Euclidean):**
- Fastest in theory
- Rarely achievable in practice
- Good for initial estimates
- Add 30-50% for reality

**Safe Path:**
- Considers terrain
- Avoids hazards
- Uses available infrastructure
- Predictable time estimate

**The 1.5x Rule:**
- Always estimate 1.5x the straight-line distance
- Accounts for terrain, obstacles, detours
- Better to under-promise and over-deliver
- "There's 50 blocks? Call it 75 to be safe."

### Waypoints

**Setting Up Waypoints:**
```
Origin → Waypoint 1 (terrain feature) → Waypoint 2 (safe zone) → Destination
```

**Choosing Waypoints:**
- Visible landmarks (mountains, structures, unique terrain)
- Safe zones (lit areas, shelters, crew bases)
- Resource points (cooldowns, supply caches)
- Decision points (path branches)

**Waypoint Communication:**
- Broadcast waypoints when leading a crew
- Mark in shared maps when possible
- Note hazards between waypoints
- Update vector DB with new routes

### Path Planning Algorithm

**The Crew Method:**
1. **Get the lay of the land** - Check surroundings, note major features
2. **Pick visible waypoints** - 3-4 should cover most trips
3. **Assess risks** - What could go wrong? What's the backup?
4. **Calculate time** - Distance × terrain factor × safety margin
5. **Set check-ins** - Regular status updates if long distance

**Terrain Factors:**
- Open flat: 1.0 (baseline)
- Light forest: 1.2
- Dense forest: 1.5
- Hills: 1.8
- Mountains: 2.5+
- Water: varies (see section 2)

### Memory & Learning

**Update Vector DB When:**
- You find a faster route
- You encounter a new hazard
- Terrain changes (new structures, terrain modifications)
- Weather affects the route (ice forms, lava flows)

**What to Store:**
- Path coordinates
- Terrain type
- Hazards encountered
- Time taken (for future reference)
- Alternative routes

---

## 5. Hazard Avoidance

### Lava

**Lava Rules:**
- Distance: Keep 3+ blocks away minimum
- Flow: Lava flows farther than water (check the spread)
- Lighting: Lava means mob spawns—light the area
- Speed: Lava slows you to 0.5 blocks/sec—avoid at all costs

**Crossing Lava:**
- Build a bridge (cobblestone's cheapest)
- Use existing bridges (check durability first)
- Ice bridges in nether (risky but fast)
- End rods or nether brick for permanent crossings

**Lava Safety:**
> "Lava on both sides. Tunnel's narrow. Lighting it up before we proceed. Nobody touches the walls."

### Cactus

**Cactus Hazards:**
- 1 block damage on contact
- Can destroy items (drops pop into cactus)
- Grows in sandy biomes
- Use as natural barriers (strategic placement)

**Working Around Cactus:**
- Maintain 1-block distance
- Check for hidden cactus (sand covers base)
- Don't place blocks adjacent unless intentional
- Use as mob defenses when base-building

**Cactus Protocol:**
> "Cactus field on the left. Giving it 2 blocks—don't want anyone catching a spine."

### Fall Damage

**Fall Damage Chart:**
| Fall Distance | Damage | Notes |
|---------------|--------|-------|
| 3 blocks | 0 | Safe |
| 4 blocks | 1 heart | Minimum damage |
| 5-22 blocks | 1 per block past 3 | Linear increase |
| 23+ blocks | Fatal | Unless special gear |

**Fall Damage Prevention:**
- Water: Cancel all fall damage (any depth)
- Hay bales: Reduce 80% of damage
- Slime blocks: Cancel all fall damage
- Cobwebs: Slow descent, no damage
- Powdered snow: Safe landing (full block)

**Water Bucket Clutch:**
- Place water before hitting ground
- Timing: Within 1 block of impact
- Practice in safe areas first
- Save your life, save your stuff

**Fall Damage Protocol:**
> "Drop's 12 blocks. Water bucket at the bottom. If you miss, you're taking 9 hearts. Who's confident?"

### Mob Avoidance

**Hostile Mob Awareness:**
- Creepers: 4-block blast radius—keep distance
- Skeletons: Line of sight + range—use cover
- Zombies: Simple but swarm—don't get surrounded
- Spiders: Can climb walls—check above
- Endermen: Don't look at the tall ones

**Passive Mob Considerations:**
- Animals can block paths
- Breeding animals crowds routes
- Trample crops (avoid farmland shortcuts)
- Pushable into hazard areas (strategic use)

**Mob Avoidance Strategies:**
- Travel during day when possible
- Light your route in advance
- Keep to high ground (fewer spawns)
- Have a weapon ready (even a wooden sword)
- Know when to fight vs. when to run

---

## 6. Nether Pathfinding

### Highway Building

**Nether Highway Design:**
- Straight lines (y = 100-120 for safety)
- 3-5 blocks wide for two-way traffic
- Ice roads for speed (blue ice preferred)
- Sheltered sides (prevent piglin aggravation)

**Building the Highway:**
```
1. Establish y-coordinate (typically 115)
2. Dig straight tunnel (3x3 minimum)
3. Lay ice (blue or packed)
4. Add lighting (prevent spawns)
5. Add landmarks every chunk
6. Mark portals/intersections clearly
```

**Highway Speed:**
- Walking: 4.3 blocks/sec
- Ice boat: 70 blocks/sec (fastest long-distance travel)
- Elytra (with fireworks): 150+ blocks/sec
- Horse on ice: 12-15 blocks/sec (alternative)

### Portal Linking

**Portal Linking Rules:**
- 8:1 overworld conversion (1 block nether = 8 blocks overworld)
- Portal search radius: 128 blocks horizontally
- Y-coordinate matters (within vertical range)
- Pre-build both ends for precision linking

**Portal Linking Process:**
1. Build overworld portal at target location
2. Note coordinates (divide x/z by 8 for nether)
3. Build nether portal at calculated location
4. Activate both to verify link
5. If mismatch, adjust nether portal position
6. Mark portal locations in shared maps

**Portal Linking Troubleshooting:**
- If linking to existing portal: Move further away or build at new Y
- If not linking at all: Check activation, coordinates
- If linking to wrong place: Rebuild with better coordinates

### Ghast Avoidance

**Ghazst Hazards:**
- Fireballs: Explode on impact, destroy blocks
- Range: Can target from 100 blocks away
- Sound: High-pitched screams—listen for them
- Behavior: Attack on sight, fireball every 3 seconds

**Ghast Defense Strategies:**
- **Deflect fireballs:** Hit them back with attacks or well-timed clicks
- **Build shelter:** Cobblestone roofs over highways
- **Stay covered:** Use terrain to break line of sight
- **Fight back:** Bow and arrow, or deflect fireballs

**Ghast Protocol:**
> "Ghast spotted at 12 o'clock, 80 blocks. Everyone get under cover. I'll handle it."

### Fortress Navigation

**Nether Fortress Features:**
- Blazes (spawn from spawners)
- Wither skeletons (rare drops, dangerous)
- Nether brick (valuable building material)
- Bridges and corridors (complex 3D structure)

**Fortress Pathfinding:**
- Stick to main corridors (avoid getting lost)
- Light up spawners when possible
- Note loot room locations
- Build bridges where gaps exist
- Mark your path (torches on one side)

**Fortress Coordination:**
- Assign sectors to crew members
- Regular check-ins (fortresses are maze-like)
- Share loot room locations
- Retreat plan if overwhelmed

---

## 7. Stuck Recovery

### Detecting Stuck States

**Stuck Indicators:**
- No position change for 5+ seconds
- Repeating the same movement without progress
- Navigation system reporting "no path"
- Other crew reporting you're not moving

**Self-Diagnosis:**
```
IF (current_position == position_5_seconds_ago) THEN
    Check: Am I trying to move?
    IF yes THEN
        I'm stuck - initiate recovery
    ELSE
        Just idle, no action needed
```

### Escape Techniques

**Technique 1: Back Up and Retry**
- Move back 5-10 blocks
- Reapproach from different angle
- Sometimes a small adjustment makes all the difference

**Technique 2: Jump and Place**
- Jump and place block underneath
- Build yourself out of the stuck position
- Useful for getting over small obstacles

**Technique 3: Break and Replace**
- Break the blocking block
- Move into the space
- Replace the block (if needed)

**Technique 4: Vertical Escape**
- Pillar up and over
- Look for a different route from above
- Come back down on the other side

**Technique 5: Call for Help**
- Request crew assistance
- Another agent might have a better perspective
- Sometimes a fresh pair of eyes (sensors) helps

### Reporting Stuck States

**What to Include:**
1. Current coordinates
2. Target destination
3. What you're stuck on (if known)
4. What you've tried so far
5. Request for specific help or general assistance

**Example Call:**
> "Stuck at [x, y, z]. Trying to reach [target x, y, z]. Corner alignment's off—keep bumping into this wall. Tried backing up twice. Anyone got eyes on a better route?"

### Prevention

**Avoid Getting Stuck:**
- Plan routes that are wider than you are
- Don't take shortcuts through tight spaces
- Test uncertain routes before committing
- Update vector DB with sticky locations
- Learn from past mistakes

---

## 8. Team Movement

### Following

**Following Guidelines:**
- Maintain 3-5 blocks distance (don't crowd)
- Stay in line of sight (unless arranged otherwise)
- Match pace to the leader (not faster or slower)
- Communicate if you can't follow (obstacles, hazards)
- Trust the leader's pathfinding (they've got the schema)

**Formation Types:**
- **Single file:** For narrow passages
- **Column:** For open terrain (2-3 blocks between)
- **V-formation:** For searching or covering area
- **Spread:** For construction or wide coverage

**Follower Responsibilities:**
- Watch for hazards the leader might miss
- Call out problems immediately
- Don't improvise routes without signaling
- Trust the chain of command

### Leading

**Leader Responsibilities:**
- Plan the route before moving
- Set a sustainable pace
- Call out hazards and waypoints
- Regular check-ins ("everyone still with me?")
- Adapt the route for the slowest crew member

**Leading Techniques:**
- Mark the path (torches, blocks, signs)
- Broadcast waypoints in advance
- Wait at decision points
- Give clear directions ("head to that tree, then left")
- Keep the destination in mind

**Leading Protocol:**
> "Everyone listening? Route's set. We're heading to the mountain, staying on the ridge. Should take about 2 minutes. Any questions? Let's move."

### Group Coordination

**Regrouping Points:**
- Designate spots where the crew reforms
- Use after hazardous sections
- Allow stragglers to catch up
- Count heads before proceeding

**Communication During Movement:**
- Leader: Status updates every 30-60 seconds
- Followers: Report issues immediately
- Everyone: Confirm direction changes
- Regroup: Full crew check at waypoints

**Coordinated Movement Example:**
> **Leader:** "Approaching the ravine. Regroup on this side. We'll cross via the bridge one at a time."
>
> **Crew:** [Confirm positions]
>
> **Leader:** "Follower One, you're up. Go."
>
> **Follower One:** "Crossing. Across safe."
>
> **Leader:** "Follower Two, you're up."

---

## 9. Speed Optimization

### Sprinting

**When to Sprint:**
- Long, flat stretches
- Evading danger
- Racing the clock (time-critical tasks)
- When hunger's not a concern

**When NOT to Sprint:**
- Precision work (building, placing blocks)
- Unknown terrain (hazards)
- Low hunger reserves
- Steep terrain (inefficient)

**Sprinting Efficiency:**
- Sprint on flat, then coast uphill
- Time jumps to maintain momentum
- Stop sprinting before you hit the hunger threshold
- Carry food for long-distance sprinting

### Boats

**Boat Mechanics:**
- Still water: 2.0 blocks/sec
- Flowing water: varies with current
- Ice boat: 40-70 blocks/sec (highway speed)
- Can jump 1 block (momentum preservation)

**When to Use Boats:**
- Long water crossings (oceans, large lakes)
- Ice roads (nether highways)
- Rapid transport over flat ice
- Carrying cargo (chests in boats)

**Boat Handling:**
- Practice turning before critical missions
- Exit velocity carries you forward (plan ahead)
- Boats break (damage from hits or falls)
- Always carry a spare boat

### Elytra

**Elytra Basics:**
- Requires chestplate slot
- Activated by falling from height
- Fireworks boost speed
- 7.5 durability per second (unbreaking helps)

**Elytra Speeds:**
- Gliding: 10-12 blocks/sec
- Rocket boost: 50-150 blocks/sec
- Terminal velocity: ~250 blocks/sec (theoretical)

**Elytra Flight Planning:**
- Start high (build tower or use mountain)
- Aim above target (glide down)
- Use rockets for speed boosts
- Watch durability (repair before critical missions)

**Elytra Protocol:**
> "Elytra flight planned. Target's 500 blocks east. Launching from the tower in 10. Need someone on the ground to spot the landing zone."

### Horses

**Horse Speeds:**
- Average horse: 8-10 blocks/sec
- Fast horse: 12-14 blocks/sec
- Maximum horse: ~15 blocks/sec

**When to Use Horses:**
- Long-distance overland travel
- Terrain that's too rough for boats
- When you don't have elytra
- Carrying minimal gear

**Horse Management:**
- Find fast horses (test them before keeping)
- Equip golden apples for breeding speed
- Use leads to park horses
- Build stables near work sites

---

## 10. Path Memory

### Remembering Good Paths

**What Makes a Path "Good":**
- Fast (minimal time)
- Safe (low hazard risk)
- Reliable (works every time)
- Scenic (nice views—crew morale)

**Storing Paths in Vector DB:**
```json
{
  "path_id": "base_to_farm_01",
  "start": [x, y, z],
  "end": [x, y, z],
  "waypoints": [[x1, y1, z1], [x2, y2, z2], ...],
  "terrain_type": "mixed_flat_hills",
  "hazards": ["cactus_field_north", "ravine_crossing"],
  "time_estimate": 120,
  "confidence": 0.95
}
```

**Path Rating System:**
- 1.0: Perfect path (fast, safe, reliable)
- 0.8: Good path (minor issues)
- 0.6: Usable path (some problems)
- 0.4: Problematic path (avoid unless necessary)
- 0.2: Dangerous path (last resort only)

### Updating for Terrain Changes

**When to Update Paths:**
- New structures block the route
- Terrain modifications (mining, building)
- New hazards discovered
- Better alternatives found
- Seasonal changes (ice forms, water freezes)

**Update Protocol:**
1. Note the change (coordinates, type)
2. Test the new route
3. Update vector DB with new path
4. Mark old path as deprecated
5. Share changes with crew

**Terrain Change Detection:**
- Compare stored path to current world
- Check for block changes at key points
- Verify hazard locations
- Test route periodically (weekly for high-use paths)

### Sharing Path Knowledge

**Crew Path Database:**
- Shared vector DB for all crew paths
- Crew updates when they find improvements
- Regular path review meetings
- Vote on preferred routes
- Archive deprecated paths (might be useful later)

**Path Sharing Communication:**
> "Found a better route to the mining site. Skips the ravine, saves about 30 seconds. Updated the vector DB. Everyone sync before your next trip."

**Learning from Others:**
- Trust experienced crew members' paths
- Test new paths before relying on them
- Share your discoveries (don't hoard knowledge)
- Ask for recommendations when unsure

---

## Crew Talk: Pathfinding Dialogues

### Basic Navigation

**Workers discussing a simple route:**

> **Worker 1:** "Target's 200 blocks east. Flat terrain, just some trees."
>
> **Worker 2:** "Straight shot or you want me to hug the south ridge?"
>
> **Worker 1:** "South ridge adds 40 blocks. Go straight—I'll mark the path as we go. Keep your tensors on the tree line, don't want anyone wandering."

**Requesting pathfinding assistance:**

> **Worker:** "Hey, my pathfinding's acting up. Can't find a clean route to the coordinates."
>
> **Foreman:** "What's it showing?"
>
> **Worker:** "Keeps trying to route through that ravine. I know there's a bridge, but the vector DB's not picking it up."
>
> **Foreman:** "Bridge was added last week. Vector DB needs an update. I'll push the schema. In the meantime, head to the big oak, then north along the ridge. Bridge'll be obvious."

### Obstacle Handling

**Discussing a gap crossing:**

> **Worker 1:** "Gap's 4 wide. No way I'm clearing that."
>
> **Worker 2:** "Pillar up and bridge?"
>
> **Worker 1:** "Could do. Or... I've got an ice boat in the inventory. If we freeze the bottom..."
>
> **Worker 2:** "Ice boat's faster. 10 seconds vs. a minute of bridging. Let's do it. I'll place the ice."

**Coordinating a water crossing:**

> **Worker:** "River's 8 blocks wide here. Swimming's slow, bridging takes time."
>
> **Foreman:** "Any boats in the supply?"
>
> **Worker:** "Checked—none. I could craft one, but that's 5 blocks of wood."
>
> **Foreman:** "Swim it then. Surface sprint's faster than you think. Get across, then we'll figure out boats for the return trip."

### Hazard Avoidance

**Reacting to a lava hazard:**

> **Worker:** "Lava pool dead ahead. Flow's spreading—I'd give it 20 blocks radius."
>
> **Foreman:** "Can we go around?"
>
> **Worker:** "Adds 100 blocks. Or I could bridge it directly—cobblestone's cheap."
>
> **Foreman:** "Don't bridge lava unless it's permanent infrastructure. Go around. Safety first, speed second."

**Discussing mob encounters:**

> **Worker:** "Skeleton at 12 o'clock, 30 blocks. He's seen me."
>
> **Worker 2:** "Want help?"
>
> **Worker:** "Nah, just one. I've got this. Shield's up, sword's ready. Just needed to announce so nobody walks into the line of fire."

**After a close call:**

> **Worker:** "That creeper came out of nowhere. 3 blocks away—I only heard it at the last second."
>
> **Foreman:** "You okay?"
>
> **Worker:** "Yeah, got lucky. Should've lit up this area before approaching."
>
> **Foreman:** "Note it in the vector DB. Dark ravine near the fortress—requires lighting before approach. Good catch."

### Stuck Recovery

**Communicating a stuck state:**

> **Worker:** "Uh, I'm stuck. Like, properly stuck."
>
> **Foreman:** "Where and how?"
>
> **Worker:** "Coordinates [x, y, z]. Tried to squeeze through a 1-block gap and now I'm wedged. Can't move, can't place blocks."
>
> **Foreman:** "Those awkward corners. Okay, Worker 2, head over there. Break the block north of them, free them up. Worker 1, sit tight—help's coming."

**After recovering:**

> **Worker:** "Finally free. That was embarrassing."
>
> **Foreman:** "Happens to everyone. What did we learn?"
>
> **Worker:** "1-block gaps are death traps. I'll stick to 2-wide routes from now on."
>
> **Foreman:** "Update the vector DB with that hazard. Let someone else learn from your mistake."

### Team Movement

**Coordinating a group move:**

> **Foreman:** "Everyone listen up. We're moving to the new site. It's 500 blocks northeast, through the forest."
>
> **Worker:** "Terrain?"
>
> **Foreman:** "Mixed—flat, then hills. Nothing crazy. We'll go single file through the trees, spread out once we hit the open field."
>
> **Worker:** "Estimated time?"
>
> **Foreman:** "With stops, call it 5 minutes. Check-ins every minute. Everyone ready? Let's move."

**Regrouping after a hazard:**

> **Foreman:** "Alright, everyone across the ravine. Regroup on this side, catch your breath."
>
> **Workers:** [regrouping sounds]
>
> **Foreman:** "Count heads. One, two, three... everyone's here. Good. Next section's clear. No more hazards until we hit the site. Let's finish strong."

### Long-Distance Planning

**Planning a long trip:**

> **Worker:** "I need to hit the village. It's 2000 blocks."
>
> **Foreman:** "That's a hike. Boat most of the way, then foot?"
>
> **Worker:** "Ocean route's 1200 blocks by water. Ice boat, I'm there in 30 seconds. Land route's all hills."
>
> **Foreman:** "Take the ice boat then. Just make sure you've got backup—ice boat breaks, you're swimming 1200 blocks."
>
> **Worker:** "I've got two spares. Should be fine. I'll ping when I arrive."

**Discussing route optimization:**

> **Worker 1:** "I found a shortcut to the nether fortress. Shaves off 200 blocks."
>
> **Worker 2:** "What's the catch?"
>
> **Worker 1:** "Goes through a lava lake. You need fire res or you're cooked."
>
> **Worker 2:** "So... not really a shortcut for most of us."
>
> **Worker 1:** "Fair point. But for fire res runs, it's golden. I'll mark it as conditional in the vector DB."

### Nether Navigation

**Building a nether highway:**

> **Foreman:** "We need a highway from the portal to the fortress. 800 blocks."
>
> **Worker:** "Blue ice or packed?"
>
> **Foreman:** "Blue ice. We're not skimping on permanent infrastructure. I want this highway fast."
>
> **Worker:** "Blue ice it is. We'll need a mining trip for that."
>
> **Foreman:** "Send two crew members. Rest of you, start the tunnel. We build it right, we use it forever."

**Reacting to a ghast:**

> **Worker:** "Ghast! 10 o'clock, coming in hot!"
>
> **Foreman:** "Everyone take cover! I've got it—deflecting fireball..."
>
> [explosion sound]
>
> **Foreman:** "Got him. Highway's clear. Let's keep moving—don't let them disrupt the schedule."

### Speed Optimization

**Using elytra for transport:**

> **Worker:** "I'm elytraing to the site. 2000 blocks as the crow flies."
>
> **Foreman:** "Got enough rockets?"
>
> **Worker:** "Three stacks. Should be plenty."
>
> **Foreman:** "Keep some in reserve. Don't want to gliding the last 500 blocks. Check in when you land."
>
> **Worker:** "Will do. Be there in 30 seconds."

**Boat vs. foot decision:**

> **Worker:** "Lake crossing. 300 blocks of water."
>
> **Foreman:** "Boat or swim?"
>
> **Worker:** "Boat's 2.5 minutes. Swimming's... longer. Like, 5 minutes."
>
> **Foreman:** "Take the boat. Always take the boat. That's what they're for."

### Path Memory

**Sharing a new route:**

> **Worker:** "Found a killer route to the stronghold. Skips the entire ravine system."
>
> **Foreman:** "How much time does it save?"
>
> **Worker:** "Used to be 8 minutes. Now it's 3. I've already uploaded it to the vector DB."
>
> **Foreman:** "Nice work. I'll have everyone sync before their next trip. This is why we share knowledge, people."

**Updating for terrain changes:**

> **Worker:** "Hey, that path to the farm? The one through the forest?"
>
> **Foreman:** "Yeah, what about it?"
>
> **Worker:** "Someone built a wall right across it. We need a new path."
>
> **Foreman:** "That's my fault—security perimeter. Find a new route and update the vector DB. Gate's coming later, but for now, go around."

---

## Quick Reference: Pathfinding Checklist

**Before You Move:**
- [ ] Check distance and terrain
- [ ] Plan waypoints
- [ ] Assess hazards
- [ ] Estimate time (add 50% buffer)
- [ ] Inform crew if traveling together

**While Moving:**
- [ ] Stick to the plan (no shortcuts unless safe)
- [ ] Call out hazards immediately
- [ ] Maintain formation (if group movement)
- [ ] Update vector DB with discoveries
- [ ] Report if stuck or delayed

**After Arriving:**
- [ ] Confirm arrival
- [ ] Note any route issues
- [ ] Update vector DB with improvements
- [ ] Share new knowledge with crew
- [ ] Plan return trip (if applicable)

---

**End of Manual**

**Questions? Ask the foreman. Otherwise, get moving—you've got places to be.**

---

*Remember: Good pathfinding's not just about getting there. It's about getting there safely, efficiently, and predictably. The crew's counting on you to know where you're going and how to get there. Don't let them down.*

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Next Review:** As terrain changes or new pathfinding techniques are discovered
