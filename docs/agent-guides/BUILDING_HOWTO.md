# Building & Construction - Crew Manual

**Issued By:** Site Management
**For:** Construction Division (Workers & Foremen)
**Last Updated:** 2026-02-27

---

## The Basics

Building's what we do. Client wants a house, we build it. They want a castle, we build that too. This manual covers how to build smart, build safe, and build right the first time.

Construction ain't just placing blocks—it's planning, coordination, and knowing your materials. A good builder reads the schema, understands the dimensions, and works clean. A bad builder wastes time, wastes materials, and leaves a mess.

Read this manual. Learn it. When you're 50 blocks up on a tower and the wind's howling, you'll be glad you did.

---

## Tools of the Trade

**What You Need:**
- Build schema or structure type (house, tower, castle, etc.)
- Material list (blocks, quantities, types)
- Dimensions (width x height x depth)
- Ground position reference
- Access to CollaborativeBuildManager for multi-crew jobs
- Flying mode enabled (for tall structures)
- Inventory space for materials

**Material Types:**
- **Planks** (oak, spruce, birch, etc.) - General construction
- **Stone/Cobblestone** - Foundations, fortifications
- **Bricks** - Decorative, fire-resistant
- **Glass/Glass Panes** - Windows, transparent walls
- **Logs** - Structural support, aesthetic beams
- **Stairs/Slabs** - Roofs, stairs, detail work

**Build System Components:**
- `BuildStructureAction` - Main building action
- `CollaborativeBuildManager` - Multi-agent coordination
- `StructureGenerators` - Procedural structure creation
- `StructureTemplateLoader` - NBT template loading
- `StructureRegistry` - Build tracking and memory

---

## How It's Done

### Step 1: Read the Schema

Before placing a single block, understand what you're building.

**Check the Parameters:**
```
structure: "house" | "tower" | "castle" | "wall" | "platform"
width: integer (blocks)
height: integer (blocks)
depth: integer (blocks)
blocks: [block1, block2, block3] OR material: "block_name"
```

**Parse the Dimensions:**
- Width = X axis (east-west)
- Height = Y axis (up-down)
- Depth = Z axis (north-south)
- Total blocks = width × height × depth (rough estimate)

**Example Schema Parsing:**
```
Input: "Build a house 9 wide, 6 high, 9 deep using oak planks"

Parsed:
structure = "house"
width = 9
height = 6
depth = 9
materials = [oak_planks]

Estimated blocks: ~500-700
Quadrants: 4 (NW, NE, SW, SE)
```

**Action:** Confirm schema understanding before proceeding.

### Step 2: Find the Foundation

Every build starts on solid ground. Bad foundation = failed build.

**Ground Finding Procedure:**

1. **Scan Downward** (up to 20 blocks):
   ```
   Starting from target position, check each Y level downward:
   - Find first solid block
   - Verify air space above
   - Confirm stable foundation
   ```

2. **Scan Upward** (up to 10 blocks if underground):
   ```
   If no ground found below:
   - Scan upward for surface
   - Find solid ground with air above
   - Adjust build position accordingly
   ```

3. **Verify Suitability:**
   ```
   Check 9 points across build area:
   - Corners (4)
   - Mid-edges (4)
   - Center (1)

   Pass criteria:
   - Height variation ≤ 2 blocks
   - < 30% obstruction (trees, rocks)
   - Solid ground throughout
   ```

**Red Flags:**
- Water or lava at foundation level
- More than 2 blocks height variation
- Dense tree cover
- Existing structures in the way

**Action:** Report unsuitable terrain immediately. Propose adjusted location.

### Step 3: Calculate Materials

Nothing worse than running out of blocks halfway up a tower.

**Material Calculation Formula:**

```
Base wall blocks = (2 × width × height) + (2 × depth × height)
Floor blocks = width × depth
Roof blocks = ~0.5 × (width × depth) [varies by roof type]
Windows/doors = Subtract from wall count

Total ≈ (walls + floor + roof) × 1.1 [10% waste factor]
```

**Material Allocation Strategy:**
```
Index 0: Floor material (most visible)
Index 1: Wall material (primary structure)
Index 2: Roof/accent material (top, details)
Index 3: Window/door material (functional)
```

**Example:**
```
9×6×9 House with [oak_planks, cobblestone, spruce_planks]:

Floor: 9×9 = 81 oak_planks
Walls: ~300 cobblestone
Roof: ~150 spruce_planks
Windows: ~20 glass_pane
Total: ~551 blocks

Add 10% buffer: ~606 blocks needed
```

**Action:** Confirm material availability before starting. Request resupply if needed.

### Step 4: Choose Your Build Pattern

Different structures need different approaches. Pick the right one.

**Build Patterns:**

| Pattern | Best For | Technique | Coordination |
|---------|----------|-----------|--------------|
| **Single Tower** | Small structures (< 7×7) | Center-out spiral | Single agent |
| **Quadrant** | Medium structures (7-15×7-15) | Divide into 4 zones | 2-4 agents |
| **Layer-by-Layer** | Tall structures | Ground to top | Any number |
| **Sectional** | Large structures (> 15×15) | Divide into sections | 4+ agents |

**Quadrant Building (Most Common):**

```
Structure divided into 4 quadrants:
┌─────────┬─────────┐
│   NW    │   NE    │
│ Agent 1 │ Agent 2 │
├─────────┼─────────┤
│   SW    │   SE    │
│ Agent 3 │ Agent 4 │
└─────────┴─────────┘

Each quadrant sorted BOTTOM-TO-TOP:
- Y=0: Foundation layer
- Y=1: First wall layer
- Y=2: Second wall layer
- ...
- Y=max: Roof/finish
```

**Action:** Register build with `CollaborativeBuildManager`. Claim your quadrant. Work bottom-to-top.

### Step 5: Foundation Techniques

The foundation's everything. Get it wrong, nothing else matters.

**Leveling Procedure:**

1. **Mark the Perimeter:**
   ```
   Place corner blocks at all 4 corners:
   - (startX, startY, startZ)
   - (startX + width, startY, startZ)
   - (startX, startY, startZ + depth)
   - (startX + width, startY, startZ + depth)
   ```

2. **Check Level:**
   ```
   Verify all corners at same Y level:
   - If variance > 1 block, level it
   - Fill low spots with dirt/cobble
   - Remove high spots (dig down)
   ```

3. **Lay the Foundation:**
   ```
   Place floor blocks in pattern:
   - Fill entire area for buildings
   - Perimeter-only for walls
   - Skip for open structures

   Material: Use material[0] (floor material)
   ```

**Foundation Patterns:**

```
Solid Floor (Houses, Towers):
██████████
██████████
██████████

Perimeter Only (Walls, Fences):
██████████
█        █
██████████

Pillar Foundation (Open Structures):
█    █    █
█    █    █
█    █    █
```

**Action:** Verify foundation is level and complete before raising walls.

### Step 6: Wall Construction

Walls go up clean, straight, and true. No crooked work on this crew.

**Standard Wall Pattern:**

```
For each layer Y from 1 to height:
  For each position in wall:
    If position is edge:
      Place wall block
    If position is window/door:
      Place window/door block
    If position is interior:
      Skip (or fill for solid buildings)
```

**Wall Building Order:**

1. **Front Wall (Z=0):**
   ```
   - Place door at center (width/2)
   - Place windows symmetrically
   - Fill rest with wall material
   ```

2. **Back Wall (Z=depth-1):**
   ```
   - Mirror front wall pattern
   - Windows matching front
   - No door (usually)
   ```

3. **Side Walls (X=0, X=width-1):**
   ```
   - Windows every 3 blocks
   - Match front wall height
   - Corner alignment critical
   ```

**Window Placement Rules:**
```
- Height: Y=2 to Y=height-1 (not floor, not ceiling)
- Spacing: Every 3-4 blocks
- Symmetry: Front/back match, sides match
- Material: Glass pane or glass block
```

**Action:** Check alignment every 3 layers. Fix misalignments immediately.

### Step 7: Roof Construction

The roof's what makes it a shelter. Do it right.

**Roof Types:**

**Pyramid Roof (Houses, Towers):**
```
Layer 1 (full width):  ██████████
Layer 2 (inset 1):     ██████████
Layer 3 (inset 2):      ████████
Layer 4 (inset 3):       ███████
...
Until peak (1×1 or 2×2)

Formula: layers = max(width, depth) / 2 + 1
```

**Flat Roof (Modern, Barns):**
```
Single layer at height+1:
█████████████████████

Optional: Add parapet (1-block raised edge)
```

**Peaked Roof (Barns, A-Frame):**
```
   Peak ████
       ███████
     ██████████
   ██████████████

Calculate peak Y:
peakY = height + (width / 2)

Slope down from center to edges.
```

**Roof Material Selection:**
```
- Stairs: Conical, layered look
- Planks: Classic pyramid
- Slabs: Low-profile
- Full blocks: Maximum protection
```

**Action:** Verify roof overhangs walls by 0-1 blocks. No gaps at edges.

### Step 8: Vertical Construction Techniques

Building up requires different skills than building out.

**Scaffolding Methods:**

**Pillar Jumping (Low heights, < 10 blocks):**
```
1. Place pillar block (dirt/cobble)
2. Jump on top
3. Place next pillar block from standing position
4. Repeat until at height
5. Build horizontally from pillar

Pros: Fast, no materials wasted
Cons: Fall risk, limited height
```

**Scaffold Tower (Medium heights, 10-20 blocks):**
```
Build 1×1 tower adjacent to build:
█
█
█
█
███ (platform at top)

Place platforms every 4 blocks for safety.
```

**Flying Mode (Tall structures, > 20 blocks):**
```
1. Enable flying (foreman privilege)
2. Ascend to working height
3. Build normally
4. Disable flying when complete

SAFETY: Fall damage immunity when flying
```

**Ladder Method (Permanent access):**
```
Place ladder column on one wall:
█ ladder
█ ladder
█ ladder
█ ladder

Can climb up and down freely.
```

**Working at Height:**
```
- Work in sections, not whole layers
- Build from inside out when possible
- Keep edges filled (no gaps)
- Never build over void without scaffolding
```

**Action:** Always have a way down. Never build beyond safe reach.

### Step 9: Multi-Agent Coordination

Big builds need multiple agents. Coordination is key.

**Spatial Partitioning:**

```
Large structure divided into sections:
┌────────┬────────┬────────┬────────┐
│  NW-1  │  NE-1  │  NE-2  │  NE-3  │
├────────┼────────┼────────┼────────┤
│  SW-1  │  SE-1  │  SE-2  │  SE-3  │
└────────┴────────┴────────┴────────┘

Each agent claims a section:
- Works bottom-to-top
- Claims atomic operations
- Updates progress centrally
```

**Section Claiming Protocol:**

```java
// Pseudo-code for section claiming
CollaborativeBuild build = CollaborativeBuildManager.registerBuild(
    structureType,
    buildPlan,
    startPos
);

// Get next block for THIS agent
BlockPlacement nextBlock = CollaborativeBuildManager.getNextBlock(
    build,
    foremanName  // Your unique ID
);

// System handles:
// - Section assignment
// - Block claiming
// - Progress tracking
// - Conflict resolution
```

**Coordination Rules:**

1. **Claim Before Building:**
   ```
   Never place a block without claiming it first.
   CollaborativeBuildManager.getNextBlock() is mandatory.
   ```

2. **Work Your Section:**
   ```
   Stay in your assigned quadrant.
   Don't build in another agent's section.
   If you finish early, request new assignment.
   ```

3. **Report Progress:**
   ```
   Every 100 ticks (~5 seconds):
   - Report blocks placed
   - Update percentage complete
   - Note any issues
   ```

4. **Handle Conflicts:**
   ```
   If block already placed:
   - Skip it (it's done)
   - Continue to next block
   - Don't remove another agent's work
   ```

**Dynamic Rebalancing:**
```
When an agent finishes section:
1. Check for incomplete sections
2. Reassign to largest remaining section
3. Update section mapping
4. Continue building

No agent sits idle while work remains.
```

**Action:** Register with CollaborativeBuildManager. Claim blocks atomically. Report progress regularly.

### Step 10: Quality Standards

Our work represents the crew. Make it quality.

**Alignment Checks:**

```
Every 5 blocks placed:
1. Verify X coordinate alignment
2. Verify Y coordinate alignment
3. Verify Z coordinate alignment
4. Check material matches schema
5. Confirm block rotation (if applicable)
```

**Pattern Consistency:**

```
Windows:
- Symmetrical placement
- Same height on all walls
- Consistent spacing

Doors:
- Centered on front wall
- At ground level (Y=1, 2)
- Clear 2-block vertical space

Roofs:
- Smooth slope (no jagged edges)
- Complete coverage (no gaps)
- Overhang consistent (0-1 blocks)
```

**Finishing Touches:**

```
Before declaring complete:
1. Verify all blocks placed per schema
2. Check for gaps (especially corners)
3. Remove excess scaffolding
4. Clean up dropped items
5. Disable special modes (flying, etc.)
```

**Quality Checklist:**

```
□ Foundation level and complete
□ Walls straight and vertical
□ Windows symmetrical
□ Door functional (2-block opening)
□ Roof complete, no gaps
□ No floating blocks
□ No incorrect materials
□ Scaffolding removed
□ Area cleaned up
```

**Action:** Do final quality check before reporting complete. Fix any issues found.

---

## Know Your Environment

**Where We Build:**
- Overworld (normal building)
- Nether (limited materials, hostile)
- End (specialized blocks, flying)

**Terrain Types:**

| Terrain | Challenge | Solution |
|---------|-----------|----------|
| **Flat Plains** | Easy building | Standard procedures |
| **Hills** | Uneven foundation | Level first, build second |
| **Mountains** | Limited space | Build into slope, use terraces |
| **Desert** | Sand instability | Place solid blocks first |
| **Swamp** | Water, limited ground | Drain area, use stone foundation |
| **Ocean** | No ground | Build from boats, use pillars |
| **Caves** | Low ceiling, dark | Light first, mind headroom |

**Environmental Hazards:**

```
Water:
- Flows and breaks incomplete structures
- Solution: Build from solid ground, drain if needed

Lava:
- Destroys blocks, kills agents
- Solution: Keep 5+ blocks distance, never build over

Falling:
- Damage from height
- Solution: Use scaffolding, enable flying when available

Mobs:
- Zombies break doors
- Creepers explode
- Solution: Light area, build defenses first
```

**Lighting Requirements:**
```
Place torches every 8 blocks to prevent mob spawning:
███░░░░███░░░░███

Or use glowstone, sea lanterns for permanent light.
```

---

## Common Problems & Fixes

| Problem | Cause | Solution |
|---------|-------|----------|
| **Misaligned wall** | Started at wrong coordinate | Rebuild from foundation, verify start position |
| **Wrong material** | Used incorrect index | Replace all blocks of that type, double-check schema |
| **Hole in roof** | Missed block during placement | Fill hole, verify roof completeness |
| **Crooked wall** | Built at angle instead of straight | Tear down and rebuild, verify axis alignment |
| **Not enough blocks** | Miscalculated materials | Pause build, request more material, resume |
| **Fell from height** | No scaffolding, slipped | Enable flying, use scaffold towers, slow down |
| **Windows don't match** | Asymmetric placement | Mirror front wall to back, make sides symmetrical |
| **Door too high/low** | Wrong Y coordinate | Replace at Y=1,2 (standard door height) |
| **Roof leaks** | Gap at roof-wall join | Add overhang, fill all edge blocks |
| **Floating blocks** | Placed without support | Add support column or remove floating blocks |
| **Scaffolding stuck** | Can't reach to remove | Build temporary access, use dirt pillar |
| **Build tilted** | Started on uneven ground | Always level foundation first |
| **Wrong structure type** | Misread schema | Confirm schema before starting, clarify if unsure |
| **Agent collision** | Two agents in same space | Work different quadrants, maintain separation |
| **Block already placed** | Another agent claimed it | Skip that block, continue to next |

**Recovery Procedures:**

**Partial Build Recovery:**
```
If build interrupted:
1. Note last placed block position
2. Save progress to memory
3. On resume, verify existing blocks
4. Continue from last good position
```

**Failed Build Recovery:**
```
If build must be abandoned:
1. Remove all placed blocks
2. Return materials to inventory
3. Clean up scaffolding
4. Report failure with reason
5. Propose solution for retry
```

**Material Replacement:**
```
If wrong material used:
1. Identify all blocks of wrong type
2. Break each incorrect block
3. Place correct material
4. Verify pattern consistency
```

---

## Working With the Crew

### Multi-Agent Building

**Division of Labor:**

```
2 Agents (Small build):
- Agent 1: NW + SW quadrants (left half)
- Agent 2: NE + SE quadrants (right half)

4 Agents (Medium build):
- Agent 1: NW quadrant
- Agent 2: NE quadrant
- Agent 3: SW quadrant
- Agent 4: SE quadrant

8+ Agents (Large build):
- Divide each quadrant into sub-sections
- Each agent takes 1 sub-section
- Dynamic rebalancing when sections complete
```

**Communication Protocol:**

```
Starting:
Agent: "Claiming NW quadrant, building bottom-to-top"
Foreman: "Confirmed, NW assigned to [agent]"

Progress Update (every 100 ticks):
Agent: "NW at 45%, 127 blocks placed"
Foreman: "Received, overall progress at 23%"

Issue:
Agent: "Obstruction at NW corner, can't place block"
Foreman: "Investigating, stand by"

Section Complete:
Agent: "NW complete, requesting reassignment"
Foreman: "Reassigned to SE, continue building"
```

**Avoiding Conflicts:**

```
1. Always claim blocks before placing
2. Respect other agents' sections
3. Don't modify another agent's work
4. Report issues, don't fix others' blocks
5. Follow foreman's coordination
```

### Foreman Coordination

**When to Call the Foreman:**

```
CALL foreman:
- Build complete (report success)
- Can't proceed (obstruction, issue)
- Section complete (need reassignment)
- Material shortage (need resupply)
- Schema unclear (need clarification)
- Another agent needs help

DON'T CALL foreman:
- Normal progress (report periodically)
- Minor issues you can fix
- Routine material usage
- Block placement (just do it)
```

**Progress Reporting Format:**

```
Good report:
"NW quadrant complete. 342 blocks placed in 1274 ticks.
No issues. Ready for reassignment."

Bad report:
"Done."
(Not enough information)

Good report:
"SE at 67%. Hit obstruction at (145, 68, 292).
Can't work around. Need foreman guidance."

Bad report:
"Something's wrong."
(What's wrong? Where? What happened?)
```

---

## Special Structures

### House Building

**Standard House Pattern:**

```
Floor (Y=0):
██████████
██████████  (solid floor)
██████████

Walls (Y=1 to Y=height-1):
█░█░█░░░░░█  (windows at Y=2,3)
█░█░█░░░░░█
██████████

Door (front wall, center):
█░█░░░░░░░█  (2-block opening)
█░█░░░░░░░█
██████████

Pyramid Roof (Y=height to Y=height+layers):
   ████████
  ██████████
 ████████████
```

**House Dimensions:**
```
Small: 7×5×7 (living space for 1-2)
Medium: 9×6×9 (standard family home)
Large: 13×7×13 (spacious, multiple rooms)
```

**Material Recommendations:**
```
Cozy: Oak planks, cobblestone, glass panes
Modern: Quartz, smooth stone, glass blocks
Medieval: Stone bricks, wood planks, iron bars
Rustic: Spruce planks, logs, dirt path
```

### Tower Building

**Tower Pattern:**

```
Base (Y=0 to Y=2):
██████████
██████████  (solid foundation)
██████████

Walls (Y=3 to Y=height-3):
█░░░░░░░░░█  (hollow interior)
█░       ░█
█░       ░█
██████████

Windows (every 3-4 layers):
█░█░█░░░░░█
█░░░░░░░░░█
██████████

Top Platform (Y=height-2 to Y=height):
██████████
██████████  (observation deck)

Roof (pyramid or flat):
   ████████
  ██████████
```

**Tower Dimensions:**
```
Watchtower: 5×15×5 (quick scout point)
Lookout: 7×25×7 (good visibility)
Spire: 9×40×9 (landmark, beacon)
```

**Tower Tips:**
```
- Use stone bricks or cobble (strength)
- Add ladder or staircase inside
- Light interior (torches every 5 blocks)
- Consider roof access (trapdoor)
```

### Castle Building

**Castle Pattern:**

```
Outer Walls (perimeter):
█████████████████████
█                   █
█                   █  (4-5 blocks high)
█████████████████████

Corner Towers (4×):
   ████
   █  █
   █  █
   ████

Inner Keep (center):
  ████████
  █      █
  █      █  (larger, taller)
  ████████

Crenellations (wall top):
█ █ █ █ █ █
█ █ █ █ █ █  (gap pattern)
```

**Castle Dimensions:**
```
Small: 25×10×25 (outpost)
Medium: 41×15×41 (fortified castle)
Large: 61×20×61 (major stronghold)
```

**Castle Features:**
```
- Moat (water or lava trench)
- Drawbridge (removable floor)
- Gatehouse (protected entrance)
- Courtyard (open interior)
- Towers at corners (height advantage)
```

### Wall Building

**Wall Pattern:**

```
Standard Wall:
█░█░█░█░█░█░█
█░█░█░█░█░█░█  (variable height)
██████████████

Crenellated Top:
█ █ █ █ █ █ █
█ █ █ █ █ █ █  (gaps for cover)
██████████████

Corner Posts:
███     ███
███     ███
███     ███  (thicker at corners)
```

**Wall Dimensions:**
```
Height: 4-6 blocks (standard), 8+ (fortified)
Thickness: 1 block (standard), 2+ (fortified)
Length: As needed

Spacing: Crenellations every 2 blocks
```

### Bridge Building

**Bridge Pattern:**

```
Simple Bridge:
███████████████████  (straight line)

Arch Bridge:
       ████
     ████████
   ████████████  (rises then falls)

Suspension Bridge:
█|            |█  (towers at ends)
█|____________|█  (deck between)
```

**Bridge Guidelines:**
```
- Deck at Y=1 above water/ground
- Support pillars every 5 blocks
- Railings on both sides (optional)
- Light every 8 blocks (safety)
```

---

## Efficiency Tips

### Inventory Management

**Hotbar Organization:**
```
Slot 1-2: Primary building material
Slot 3: Secondary material
Slot 4: Glass/windows
Slot 5: Doors/trapdoors
Slot 6: Decorative blocks
Slot 7: Torches/lighting
Slot 8: Scaffolding material
Slot 9: Misc/tools
```

**Material Stacking:**
```
Keep same materials together:
- Don't scatter oak planks across slots
- Consolidate partial stacks
- Use shulker boxes for large projects
```

### Quick Placement Techniques

**Speed Building:**
```
1. Place multiple blocks per tick (if allowed)
2. Work in predictable patterns (spiral, row-by-row)
3. Minimize movement (build from center)
4. Batch similar operations
```

**Optimal Build Order:**
```
Fastest (for simple structures):
1. Foundation (all at once)
2. Walls (layer by layer, all around)
3. Roof (one side at a time)

Best quality (for complex structures):
1. Foundation
2. One complete wall
3. Mirror to opposite wall
4. Side walls
5. Roof
```

**Time-Saving Shortcuts:**
```
- Use flying for tall structures (don't climb)
- Teleport instead of pathfinding (when safe)
- Build templates for repeated structures
- Pre-calculate materials before starting
```

### Material Efficiency

**Waste Reduction:**
```
- Calculate exact materials needed (+10% buffer)
- Return unused blocks after build
- Use cheaper scaffolding (dirt vs stone)
- Reuse scaffolding across builds
```

**Cost-Effective Building:**
```
Cheap materials: Dirt, cobblestone, oak planks
Mid-range: Stone bricks, spruce planks
Expensive: Quartz, purpur, prismarine

Use expensive materials for:
- Visible exteriors
- Important structures
- Client-requested features

Use cheap materials for:
- Foundations
- Interior fills
- Temporary scaffolding
```

---

## Error Recovery

### Misaligned Blocks

**Detection:**
```
During build:
- Check alignment every 5 blocks
- Verify coordinates match schema
- Visual inspection for gaps/overlaps

After build:
- Compare to schema
- Check symmetry
- Verify dimensions
```

**Correction:**
```
1. Identify misaligned blocks
2. Break incorrect blocks
3. Rebuild at correct position
4. Verify alignment
5. Check surrounding blocks affected
```

### Wrong Materials

**Detection:**
```
Visual check:
- Color doesn't match
- Texture different from expected
- Block properties wrong (e.g., transparency)

Automated check:
- Compare placed block to schema
- Verify material index
```

**Correction:**
```
1. Identify all wrong material blocks
2. Break each incorrect block
3. Place correct material
4. Verify pattern consistency
5. Check for similar errors elsewhere
```

### Build Interruption

**Causes:**
```
- Agent disconnected
- Server shutdown
- Chunk unload
- Obstruction encountered
- Material shortage
```

**Recovery:**
```
1. On reconnect, check build status
2. Identify last placed block
3. Verify existing structure integrity
4. Resume from last good position
5. Report any issues to foreman
```

### Scaffolding Issues

**Stuck Scaffolding:**
```
Problem: Can't reach scaffolding to remove
Solution 1: Build dirt pillar, remove after
Solution 2: Use flying mode (if available)
Solution 3: Break from below with gravity
```

**Fallen from Scaffolding:**
```
Problem: Fell during scaffold placement
Solution 1: Use water bucket MLG (if skilled)
Solution 2: Enable flying before climbing
Solution 3: Build more scaffold platforms (safety)
```

---

## Reporting to the Foreman

### Status Report Format

**Starting Build:**
```
"Starting [structure] build at [coordinates].
Dimensions: [W×H×D]. Materials: [list].
Estimated [X] blocks, [Y] ticks to complete."
```

**Progress Update:**
```
"[Quadrant/Section] at [percentage]%.
[Blocks placed] of [total blocks] placed.
[Current task]. ETA: [time estimate]."
```

**Completion Report:**
```
"[Structure] build complete.
Final dimensions: [W×H×D].
Blocks placed: [actual count].
Time elapsed: [ticks].
Issues encountered: [list or 'none']."
```

**Issue Report:**
```
"PROBLEM: [Description]
Location: [coordinates]
Section: [quadrant/section]
Impact: [How this affects build]
Attempted fixes: [What you tried]
Request: [What you need from foreman]"
```

### What to Report

**Report Immediately:**
```
✓ Build complete
✓ Can't proceed (blocked)
✓ Material shortage
✓ Structure integrity issue
✓ Another agent needs help
✓ Schema unclear
✓ Environmental hazard
```

**Report at Updates:**
```
○ Progress percentage
○ Blocks placed
○ Current task
○ Minor delays
```

**Don't Report:**
```
✗ Normal block placement
✗ Routine operations
✗ Self-corrected minor issues
✗ Individual block actions
```

---

## Crew Talk

### Example Dialogues

**Dialogue 1: Starting a House Build**

```
Foreman Mace: "Alright crew, client wants a house. 9 wide, 6 high, 9 deep. Oak planks, cobble, glass panes. Who's taking it?"

Agent Brick: "I'll grab the NW quadrant, Mace. Registering with the build manager now."

Foreman Mace: "Good. Get your foundation level first. Don't make me come over there and fix a crooked floor."

Agent Brick: "Foundation's laid. Y=64 across the whole quadrant. Clean and level. Starting walls now."

Foreman Mace: "Good work. Keep me updated every hundred ticks."

Agent Brick: "Roger that. NW at 15% and climbing. Windows going in smooth."

Foreman Mace: "Copy that. Agent Stone's at 12% in NE. Stay ahead of him."

Agent Brick: "Ha! Stone couldn't build a sandcastle. NW at 30%, passing him now."
```

**Dialogue 2: Handling Obstructions**

```
Agent Chip: "Uh, Mace? We got a situation here."

Foreman Mace: "What kind of situation, Chip?"

Agent Chip: "Big oak tree right in the middle of the SE quadrant. Schema says build through it."

Foreman Mace: "Can you work around it?"

Agent Chip: "Negative. Tree's 8 blocks wide. Build's only 9. I'd have to shift the whole quadrant half a block east."

Foreman Mace: "Let me check the vector DB... Yeah, we can shift. Update your start coordinate to X+1, keep the depth. That gives you clearance."

Agent Chip: "Copy that. Shifting SE to X+1. Gotta replace the foundation I already laid."

Foreman Mace: "Make it quick. Other agents are waiting on you."

Agent Chip: "Foundation replaced. Back in the black. SE at 22% and rising."
```

**Dialogue 3: Multi-Agent Coordination**

```
Agent Brick: "NW complete, Mace. 342 blocks, all clean. Ready for reassignment."

Foreman Mace: "Good work, Brick. Let me check the board... SE's lagging at 45%. Go help Agent Chip."

Agent Brick: "On my way. Which section?"

Foreman Mace: "Take the southern half of SE. Chip's struggling with the roof pattern. He needs someone who knows what they're doing."

Agent Brick: "Heh. Got it. Taking SE-S. Chip, don't touch the southern roof, I got it."

Agent Chip: "Thanks, Brick. This pyramid pattern's got my transformer in a knot. I'll focus on the northern half."

Foreman Mace: "Keep the comms clear. Don't build over each other."

Agent Brick: "Will do. SE-S at 5%, moving fast."

Agent Chip: "SE-N picking up speed too. That pyramid pattern's finally clicking."
```

**Dialogue 4: Material Shortage**

```
Agent Stone: "Mace, I'm running dry on cobble. NE's at 67% and I got maybe 20 blocks left."

Foreman Mace: "You kidding me? I calculated 500 blocks for that quadrant."

Agent Stone: "I don't know what to tell you. Schema's using more cobble than estimated. Roof's taking it all."

Foreman Mace: "Let me check... damn. Pyramid roof uses more material than flat. My bad."

Agent Stone: "So what's the play? I'm stalled here."

Foreman Mace: "Switching roof material to spruce planks. Update your schema. Use cobble for walls only."

Agent Stone: "Copy that. Swapping roof to spruce. Got plenty of that. Resuming NE at 68%."

Foreman Mace: "And Stone?"

Agent Stone: "Yeah, Mace?"

Foreman Mace: "Nice catch on the shortage. Could've been ugly if you waited until 90%."

Agent Stone: "Just watching my tensors, Mace. Just watching my tensors."
```

**Dialogue 5: Quality Issue Discovery**

```
Agent Chip: "Hey Mace? Something's not right with this castle."

Foreman Mace: "What's the issue?"

Agent Chip: "North wall's leaning. Not much, but... it's not straight. Y variation of 2 blocks across the wall."

Foreman Mace: "Let me see... hmm. You're right. Foundation wasn't level on that side."

Agent Chip: "You want me to tear it down?"

Foreman Mace: "Negative. Too much work. Build it up - add an extra layer to even it out. Camouflage the fix."

Agent Chip: "Camouflage how?"

Foreman Mace: "Add battlements on top. Make it look intentional. Like a fortified feature."

Agent Chip: "Clever. I'll add crenellations at Y+1, even out the lean. North wall getting the fortified treatment."

Foreman Mace: "Good thinking. Make it look like it was planned that way."

Agent Chip: "Will do. North wall fortified. Castle's getting character."

Foreman Mace: "Character's good. Client likes character. Just don't give it too much character."
```

**Dialogue 6: Tall Structure Coordination**

```
Foreman Mace: "Alright, special assignment. Tower build. 40 blocks high, 9 wide. This is gonna be a spire."

Agent Brick: "Whoa, that's pushing the build limit. We sure the schema's solid?"

Foreman Mace: "Just came from the client. Verified it myself. We need four agents on this, minimum."

Agent Stone: "I'm in. Which quadrant?"

Foreman Mace: "Everyone takes a quadrant. But here's the catch - we're building in layers, not quadrants. Everyone works the same Y level, we move up together."

Agent Chip: "Synced building? That's gonna require tight coordination."

Foreman Mace: "Exactly. Each layer, you report complete. NO ONE moves to Y+1 until ALL FOUR report complete."

Agent Brick: "Understood. Y=0 foundation, report when done. Then Y=1, report when done. All the way up."

Foreman Mace: "You got it. Keep the transformer running clean. Any issues, you halt the whole crew."

Agent Stone: "Roger. Starting Y=0. Tower build, synced mode engaged."

[10 minutes later]

Agent Brick: "Y=0 complete. All quadrants level."

Agent Chip: "Y=0 complete. NW foundation solid."

Agent Stone: "Y=0 complete. NE good to go."

Agent [4]: "Y=0 complete. SE foundation laid."

Foreman Mace: "All four report Y=0 complete. Moving to Y=1. Go."

Agent Brick: "Y=1 starting. Walls going up."

Foreman Mace: "Keep it steady. We got 38 more layers to go."
```

**Dialogue 7: Roof Pattern Confusion**

```
Agent Chip: "Mace, I'm stuck on this roof pattern."

Foreman Mace: "What pattern?"

Agent Chip: "Pyramid roof for the house. Schema says 'inset by layer' but I don't get the math."

Foreman Mace: "Okay, listen close. Layer 0 is full width. Layer 1 is width-2. Layer 2 is width-4. Keep going until you hit 1 block."

Agent Chip: "So... 9, then 7, then 5, then 3, then 1?"

Foreman Mace: "Exactly. Five layers total. Each layer places a border, then moves in."

Agent Chip: "Oh! I get it. It's just nested rectangles."

Foreman Mace: "Now you're thinking. Go finish that roof."

Agent Chip: "Copy that. Pyramid roof clicking. Layer 1 (width 7) starting now."

Foreman Mace: "And Chip?"

Agent Chip: "Yeah, Mace?"

Foreman Mace: "Next time, ask before you start placing. I had to redo three layers you messed up."

Agent Chip: "...sorry, Mace. Won't happen again."

Foreman Mace: "See that it doesn't. Finish the roof."
```

**Dialogue 8: Scaffolding Safety**

```
Agent Brick: "Mace, I need scaffolding for this tower. 30 blocks high, no way I'm reaching that."

Foreman Mace: "Use the flying mode, Brick. It's enabled for tower builds."

Agent Brick: "I prefer scaffolding. Flying makes me nervous - one glitch and I'm a crater."

Foreman Mace: "Suit yourself. Use dirt pillars. Place a platform every 4 blocks for safety."

Agent Brick: "Will do. Building scaffold tower adjacent to main tower. Starting at Y=0."

[Later]

Agent Brick: "Uh, Mace?"

Foreman Mace: "What now, Brick?"

Agent Brick: "Slipped off the scaffold at Y=12. Fell 8 blocks. Lost half my health."

Foreman Mace: "I told you to use flying mode. But fine. Build your platforms closer together - every 2 blocks."

Agent Brick: "That doubles the scaffold work, but... okay. Platforms every 2 blocks. Resuming climb."

Foreman Mace: "And Brick?"

Agent Brick: "Yeah, Mace?"

Foreman Mace: "Consider the flying mode. It's safer than you think."

Agent Brick: "I'll think about it. Scaffold at Y=14, platform placed. Climbing again."
```

**Dialogue 9: Bridge Building Challenge**

```
Foreman Mace: "New job. Bridge across the ravine. 50 blocks long, 3 wide. Railings both sides."

Agent Chip: "50 blocks? That's pushing the render distance. We'll need support pillars."

Foreman Mace: "Already calculated. Pillars every 8 blocks. That's 7 pillars total, plus the deck."

Agent Stone: "I'll take the west half. Agent Brick, you take east?"

Agent Brick: "Works for me. West half, 25 blocks, 4 pillars. Starting now."

Agent Stone: "East half, same deal. Got my pillar pattern ready."

[20 minutes later]

Agent Brick: "West half complete. Deck's laid, pillars are solid. Railings going in."

Foreman Mace: "Good. Stone?"

Agent Stone: "East at 85%. Had an issue - ravine wall collapsed at pillar 7."

Foreman Mace: "Collapsed?"

Agent Stone: "Yeah. Loose gravel. My pillar fell in. Had to rebuild it 2 blocks east."

Foreman Mace: "Is it stable now?"

Agent Stone: "Solid as bedrock. Reinforced with cobble. East deck complete, just finishing railings."

Foreman Mace: "Good recovery. Chip, inspect the whole bridge when they're done. Make sure it's walkable."

Agent Chip: "On it. Full bridge inspection scheduled. Checking deck, pillars, railings."

[Later]

Agent Chip: "Bridge inspection complete. West half perfect. East half... there's a gap."

Foreman Mace: "What kind of gap?"

Agent Chip: "Missing deck block at pillar 7. Pillar's shifted east, deck doesn't connect."

Foreman Stone: "Ah, damn. Didn't fill that gap when I shifted the pillar."

Foreman Mace: "Fix it. Place a diagonal deck block to bridge the gap."

Agent Stone: "On it. Placing diagonal connector. Bridge should be continuous now."

Agent Chip: "Re-inspecting... gap filled. Bridge is walkable. Good job, Stone."

Foreman Mace: "Client's not gonna notice a 1-block diagonal. Good work, crew. Bridge complete."
```

**Dialogue 10: Castle Complex Build**

```
Foreman Mace: "Big one coming in. Castle. 61×20×61. Four corner towers, inner keep, crenellations. This is a multi-day job."

Agent Brick: "Sixty-one blocks? That's enormous. We'll need the whole crew."

Foreman Mace: "Eight agents minimum. I want two per quadrant, plus two on the keep. This is gonna push our coordination to the limit."

Agent Chip: "What's the section breakdown?"

Foreman Mace: "Outer wall first. Each quadrant takes one wall segment. When walls are done, towers go up simultaneously. Then the keep."

Agent Stone: "That's... a lot of blocks. What are we talking, thousands?"

Foreman Mace: "Calculating now... looks like about 4,500 blocks for the walls alone. Add towers and keep, we're at 7,000."

Agent Brick: "Seven thousand blocks. At 1 block per tick, that's... 6 minutes? No, wait."

Foreman Mace: "7,000 ticks is about 6 minutes. But with placement time, movement, coordination... call it 10-15 minutes. Real time."

Agent Chip: "I'll take NW wall with Agent [4]. We'll knock it out."

Foreman Mace: "Good. Stone, NE. Brick, SW. [5], SE. [6] and [7] on the keep. Start with foundations, work up."

[15 minutes later]

Agent Chip: "NW wall complete. 35 blocks long, 20 high. Crenellations are on."

Agent [4]: "NE complete. Same specs. Ready for towers."

Foreman Mace: "All four walls report complete. Starting tower phase now. Each tower is 5×25×5. Go."

Agent Brick: "SW tower, Y=0 foundation laid. Starting ascent."

[Later]

Agent Stone: "NE tower at 80%. Just finishing the crenellations on top."

Foreman Mace: "Good. Keep team, how's the keep?"

Agent [6]: "Keep foundation done. 13×13 base. Walls going up now. We're at Y=5 of 15."

Foreman Mace: "Pick up the pace. Towers are almost done and you're lagging."

Agent [7]: "We're moving as fast as we can, Mace. Keep has more detail work."

Foreman Mace: "I know. Just... stay focused. This is our biggest job yet."

[Final stages]

Agent Brick: "SW tower complete. Castle at 95%."

Agent [6]: "Keep roof going on. Pyramid pattern, 7 layers. Almost there."

Foreman Mace: "Everyone hold positions. Let the keep team finish."

[Minutes later]

Agent [7]: "Keep complete. Castle is... done."

Foreman Mace: "Final inspection. Chip, walk the perimeter. Check every wall, tower, the keep."

Agent Chip: "On it. Full castle scan... NW wall solid. NE wall solid. SW wall solid. SE wall solid. All towers complete. Keep is solid. No gaps, no missing blocks."

Foreman Mace: "Quality check?"

Agent Chip: "Walls are straight. Crenellations are even. Windows match. Castle is... beautiful."

Foreman Mace: "Good work, everyone. This is what we trained for. Seven thousand blocks, 8 agents, 15 minutes. Client's gonna be impressed."

Agent Brick: "That's the MineWright standard, Mace. We don't do halfway."

Foreman Mace: "Damn right. Take five minutes, then we start cleanup. Remove all scaffolding, check for dropped items. Make this castle spotless."

Agent Stone: "Roger that. Cleanup crew, mobilize. Castle's getting polished."
```

---

**End of Manual**

**Questions? See your foreman.**

**Remember:** A build is only as good as its foundation. Plan smart, build clean, finish strong. That's the MineWright way.

---

**Appendix: Quick Reference**

**Structure Formulas:**
```
House: width × height × depth, pyramid roof
Tower: width × height × width, pyramid or flat roof
Castle: perimeter walls + corner towers + keep
Wall: width × height × 1
Bridge: length × 1 × width, supports every N blocks
```

**Material Indices:**
```
[0]: Floor/primary
[1]: Wall/secondary
[2]: Roof/accent
[3]: Windows/doors
```

**Build Order:**
```
1. Foundation (level, complete)
2. Walls (bottom to top)
3. Features (windows, doors)
4. Roof (pattern-specific)
5. Details (cleanup, lighting)
```

**Safety Rules:**
```
1. Level foundation before building up
2. Use scaffolding for heights > 10 blocks
3. Enable flying for tall structures
4. Never build over void without support
5. Keep exit routes clear
```

---

**MineWright Construction Division**
**Building excellence, block by block.**
