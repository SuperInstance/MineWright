# CRAFTING_HOWTO.md

**Project:** MineWright AI - Minecraft Autonomous Agents
**Component:** Agent Crafting Guide
**Version:** 1.0
**Date:** 2026-02-27
**Target Audience:** AI Agents (Workers and Foremen)

---

## Executive Summary

Listen up, crew. This is your complete guide to the crafting table. Whether you're a fresh hire or a veteran who's seen more crafting tables than caves, this guide will keep your operations efficient and your projects on schedule.

**Core Philosophy:** Crafting isn't just about recipes—it's about resource management, efficiency, and knowing which tool serves the job best. A smart crafter doesn't just follow patterns; they understand the material flow from ore to finished product.

---

## Table of Contents

1. [Recipe Management](#recipe-management)
2. [Tool Crafting](#tool-crafting)
3. [Armor Crafting](#armor-crafting)
4. [Block Crafting](#block-crafting)
5. [Advanced Crafting](#advanced-crafting)
6. [Brewing](#brewing)
7. [Resource Conversion](#resource-conversion)
8. [Inventory Crafting](#inventory-crafting)
9. [Batch Crafting](#batch-crafting)
10. [Recipe Discovery](#recipe-discovery)
11. [Example Dialogues](#example-dialogues)

---

## 1. Recipe Management

### The Recipe Database

Every agent should have immediate access to the complete recipe registry. This isn't a nice-to-have—it's essential equipment. You don't show up to a job site without your tools, and you don't approach a crafting table without knowing your patterns.

**Recipe Lookup Protocol:**
```
1. Identify target item
2. Query RecipeRegistry for matching patterns
3. Check material availability in inventory/storage
4. Calculate optimal crafting quantity (avoid waste)
5. Execute craft with confirmation tracking
```

### Pattern Recognition

Crafting patterns follow logical rules. Learn these rules and you'll figure out recipes even when the database is offline:

**Shape Rules:**
- **Line patterns**: Tools, sticks, doors (1x3 or 3x1)
- **L-shape**: Pickaxes, axes (3x3 with corner cut)
- **T-shape**: Shovels, hoes (3x3 with top row empty)
- **Box patterns**: Armor, storage blocks (2x2 or 3x3 solid)
- **X-shape**: Rails, ladders (diagonal pattern)
- **Circle-ish**: Wheels, minecarts (plus shape with corners)

**Material Substitution Rules:**
- Planks: Any wood type (oak, birch, spruce, etc.)
- Stone variants: Stone, cobblestone, and stone bricks interchangeable
- Color matching: Dyes must match target color for concrete/wool
- Metal tier matching: Can't mix iron and gold in same recipe

### Ingredient Finding

Before you craft, you source. That's the job.

**Sourcing Priority:**
1. **Personal Inventory** - Fastest access, check first
2. **Nearby Chests** (within 16 blocks) - Quick retrieval
3. **Storage Room** (designated area) - Organized bulk storage
4. **Remote Storage** - Longest travel time, plan accordingly
5. **Craft/Smelt** - If ingredient doesn't exist, make it

**Ingredient Tracking:**
```java
// Track ingredient sources for project planning
Map<Item, Integer> calculateIngredients(List<ItemStack> targetItems) {
    Map<Item, Integer> totalNeeded = new HashMap<>();
    for (ItemStack target : targetItems) {
        Recipe recipe = RecipeRegistry.getRecipe(target.getItem());
        for (ItemStack ingredient : recipe.getIngredients()) {
            totalNeeded.merge(ingredient.getItem(), ingredient.getCount(), Integer::sum);
        }
    }
    return totalNeeded;
}
```

---

## 2. Tool Crafting

### Tool Tier Progression

Tools follow a clear hierarchy. Respect it.

**Tier Comparison (Durability / Speed / Damage):**

| Tier | Material | Durability | Mining Speed | Attack Damage | Special |
|------|----------|------------|--------------|---------------|---------|
| Wood | Planks | 59 | 2.0 | 4.0 | Cheapest, emergency only |
| Gold | Gold Ingots | 32 | 12.0 | 4.0 | Fastest, fastest break |
| Stone | Cobblestone | 131 | 4.0 | 5.0 | Standard early game |
| Iron | Iron Ingots | 250 | 6.0 | 6.0 | Reliable workhorse |
| Diamond | Diamonds | 1561 | 8.0 | 7.0 | Premium efficiency |
| Netherite | Netherite Scrap | 2031 | 9.0 | 8.0 | Fireproof, ultimate |

**Efficiency Note:** A stone tool lasts 2.2x longer than wood. Iron lasts nearly 2x longer than stone. Diamond is 6.25x stone. Plan your upgrades accordingly.

### Pickaxe Patterns

The pickaxe is your primary tool. Know it inside and out.

**Recipe Pattern (3x3):**
```
Material  Material  Material
     _        Stick      _
     _        Stick      _
```

**Where _ = empty slot**

**Material Requirements per Tier:**
- Wooden/Gold: 3 planks/ingots + 2 sticks
- Stone/Iron/Diamond/Netherite: 3 blocks/ingots/gems + 2 sticks

**Pickaxe Selection Guide:**
- **Wood/Gold**: Only for emergency or when material is abundant
- **Stone**: Minimum for serious mining operations
- **Iron**: Standard for branch mining and construction
- **Diamond**: For large-scale projects and deep mining
- **Netherite**: When durability matters more than material cost

**Mining Efficiency Math:**
```
Blocks per tool = Durability
Blocks per minute = (Mining Speed × 60) / Block Hardness

Example: Iron Pickaxe
- Hard stone (hardness 3.0): 6.0 speed → ~20 blocks/minute → 12.5 minutes life
- Soft dirt (hardness 0.5): 6.0 speed → ~120 blocks/minute → 2 minutes life
```

### Axe Patterns

**Recipe Pattern (3x3):**
```
Material  Material  _
     _     Material  Stick
     _        Stick      _
```

**Axe Mechanics:**
- **Wood chopping**: 2x faster than by hand
- **Damage**: 7 (wood/gold), 9 (stone), 10 (iron/diamond/netherite)
- **Stripping**: Right-click logs to strip bark (1.20.1 feature)
- **Shield breaking**: High efficiency vs. shields

### Shovel Patterns

**Recipe Pattern (3x3):**
```
     _        _         _
Material  Material  Material
     _        Stick      _
```

**Shovel Purpose:**
- Dirt, sand, gravel, clay: 4x faster than hand
- Snow: Required for gathering snow blocks
- Grass paths: Create by right-clicking grass
- Campfires: Extinguish with shovel

### Hoe Patterns

**Recipe Pattern (3x3):**
```
Material  Material  _
     _        Stick      _
     _        Stick      _
```

**Hoe Applications:**
- Tilling soil: Prepare farmland
- Harvesting: Crops break instantly
- Scraping: Convert coarse dirt to dirt
- Rust (wheat, carrots, potatoes): Break crops efficiently

### Sword Patterns

**Recipe Pattern (3x3):**
```
     _     Material      _
     _     Material      _
     _        Stick      _
```

**Combat Math:**
- Sweep attack (no jump): Hits 3 enemies at 50% damage
- Critical (falling hit): 1.5x damage
- Knockback: 0.5 blocks base + enchantments
- Attack speed: 1.6 attacks/second (diamond sword)

---

## 3. Armor Crafting

### Armor Slot System

Four slots, four pieces. Missing any piece exposes a vulnerability.

**Armor Values (Protection Points):**

| Material | Helmet | Chestplate | Leggings | Boots | Total |
|----------|--------|------------|----------|-------|-------|
| Leather | 1 | 3 | 2 | 1 | 7 |
| Golden | 2 | 5 | 3 | 1 | 11 |
| Chainmail | 2 | 5 | 4 | 1 | 12 |
| Iron | 2 | 6 | 5 | 2 | 15 |
| Diamond | 3 | 8 | 6 | 3 | 20 |
| Netherite | 3 | 8 | 6 | 3 | 20 + knockback resistance |

**Damage Reduction Formula:**
```
Damage Reduced = Damage × (Total Armor × 0.04)
Max reduction: 80% (at 20 armor points)

Example: Diamond armor (20 points) reduces 8 damage hit by 6.4 → 1.6 damage taken
```

### Armor Patterns

**Helmet (2x2 centered in top row):**
```
Material  Material  Material
Material     _        Material
```

**Chestplate (full 3x3 except corners):**
```
Material  Material  Material
Material  Material  Material
Material     _        Material
```

**Leggings (full 3x2 bottom):**
```
Material  Material  Material
     _        _         _
Material  Material  Material
Material  Material  Material
```

**Boots (2x4 vertical columns):**
```
Material     _        Material
Material  Material  Material
Material     _        Material
```

### Durability Math

**Durability Per Piece:**
- Leather: 75/115/90/65 (helmet/chest/legs/boots)
- Gold: 105/155/145/105
- Chainmail: 165/240/225/195
- Iron: 165/240/225/195
- Diamond: 360/528/495/430
- Netherite: 405/592/555/480

**Armor Toughness (Netherite only):**
- Reduces armor penetration from strong attacks
- Netherite: 12 toughness per piece
- Critical hits and high-damage attacks reduced significantly

---

## 4. Block Crafting

### Building Materials

**Planks:**
```
1 Log → 4 Planks (any wood type)
4 Planks → 1 Crafting Table (2x2)
6 Planks → 4 Signs (same wood)
6 Planks → 16 Slabs (same wood)
6 Planks → 2 Stairs (same wood)
```

**Stone Variants:**
```
4 Cobblestone → 4 Stone Bricks (crafting table)
1 Stone → 1 Stone Bricks (smelt cobble → stone, then craft)
4 Stone → 4 Polished Stone
4 Stone → 4 Stone Bricks
```

**Concrete (color matching required):**
```
4 Sand + 4 Gravel → 8 Concrete Powder (color dye + powder)
Concrete Powder + Water → Solid Concrete (place in water)
```

**Bricks:**
```
4 Clay Balls → 1 Brick (smelt in furnace)
4 Bricks → 1 Brick Block
6 Bricks → 4 Brick Slabs
4 Bricks → 1 Brick Stairs
```

### Decorative Blocks

**Wool:**
```
4 String → 1 White Wool
1 Wool + 1 Dye → 1 Colored Wool
```

**Carpet:**
```
2 Wool (same color) → 3 Carpet
```

**Terracotta/Glazed Terracotta:**
```
1 Clay Block → 1 Terracotta (smelt)
1 Terracotta + 1 Dye → 1 Stained Terracotta
4 Stained Terracotta (same color) → 1 Glazed Terracotta (smelt)
```

### Redstone Components

**Redstone Blocks:**
```
9 Redstone Dust → 1 Redstone Block (compact storage)
1 Redstone Block → 9 Redstone Dust (decompress)
```

**Pistons:**
```
3 Wooden Planks (any)
4 Cobblestone
1 Iron Ingot
1 Redstone Dust
→ 1 Piston
```

**Sticky Piston:**
```
1 Piston
1 Slimeball
→ 1 Sticky Piston
```

**Observer:**
```
6 Cobblestone
2 Redstone Dust
1 Nether Quartz
→ 1 Observer
```

**Dispenser/Dropper:**
```
7 Cobblestone
1 Bow
1 Redstone Dust
→ 1 Dispenser (bow in center)

7 Cobblestone
2 Redstone Dust
→ 1 Dropper (no bow)
```

---

## 5. Advanced Crafting

### Anvil Recipes

**Item Repair:**
```
2 Damaged Items of Same Type → 1 Repaired Item
Durability = Durability1 + Durability2 + 5% of max
Cost: Experience levels (varies by material)
```

**Renaming:**
```
1 Item + Name → 1 Renamed Item
Cost: 1 level (always)
```

**Enchantment Combining:**
```
1 Enchanted Item + 1 Enchanted Item → Combined Enchantments
Cost: Varies based on enchantments
Conflict: Sharpness + Smite = Incompatible
```

### Smithing Table

**Netherite Upgrade:**
```
1 Diamond Item + 1 Netherite Ingot → 1 Netherite Item
Preserves: Enchantments, durability damage, custom name
Requires: Smithing Table (not crafting table)
```

**Trimming (1.20+):**
```
1 Armor Piece + 1 Trim Material + 1 Trim Pattern
→ Trimmed Armor (visual only)
Materials: Copper, Iron, Gold, Lapis, Quartz, Amethyst, Diamond, Netherite
```

### Stonecutter Optimization

The stonecutter is efficiency incarnate. Use it.

**Stonecutter vs Crafting Table:**

| Block | Crafting Table Output | Stonecutter Output | Efficiency |
|-------|---------------------|-------------------|------------|
| Stone | 4 Stone Bricks | 1 Stone Brick | No waste for small jobs |
| Copper | 4 Cut Copper | 1 Cut Copper | Perfect for odd quantities |
| Copper | 6 Copper Grate | 1 Copper Grate | 6x efficiency |
| Copper | 4 Copper Door | 1 Copper Door | No waste |

**When to Use Stonecutter:**
- Single block needs (don't waste 3 extra)
- Odd quantities (need 7 blocks, not 8)
- Specific variants (stairs, slabs, walls)
- Resource conservation (copper is expensive)

---

## 6. Brewing

See separate document: `BREWING_AUTOMATION.md`

**Quick Reference:**

**Basic Potions (Water Bottle + Ingredient):**
- Nether Wart → Awkward Potion (base for most)
- Glowstone Dust → Thick Potion
- Redstone → Mundane Potion

**Effect Potions (Awkward + Ingredient):**
- Blaze Powder → Strength
- Sugar → Speed
- Ghast Tear → Regeneration
- Glistering Melon → Healing
- Spider Eye → Poison
- Golden Carrot → Night Vision
- Pufferfish → Water Breathing
- Magma Cream → Fire Resistance
- Rabbit's Foot → Leaping
- Phantom Membrane → Slow Falling

**Enhancements:**
- Redstone → Extended Duration
- Glowstone → Amplified (Level II)
- Gunpowder → Splash Potion
- Dragon's Breath → Lingering Potion (from splash only)

---

## 7. Resource Conversion

### Smelting Ratios

**Ore to Ingot:**
```
1 Ore (any metal) → 1 Ingot (smelt 10 seconds)
Exception: 1 Raw Iron/Gold/Copper → 1 Ingot
Ancient Debris → 1 Netherite Scrap (smelt 30 seconds)
```

**Experience per Smelt:**
- Ancient Debris: 2 XP
- Raw Ore: 0.7 XP
- Sand/Glass: 0.1 XP
- Clay/Brick: 0.3 XP
- Cactus/Green Dye: 0.2 XP
- Log/Charcoal: 0.1 XP

**Blast Furnace (2x speed for ores):**
```
Iron Ore: 5 seconds (vs 10 regular)
Gold Ore: 5 seconds (vs 10 regular)
Other items: Normal speed (no benefit)
```

**Smoker (2x speed for food):**
```
All food items: 5 seconds (vs 10 regular furnace)
```

### Composter Usage

**100% Chance (fill immediately):**
- Pumpkin, melon block, hay bale, cake, cookie

**65% Chance:**
- Apple, beetroot, carrot, potato, lily pad

**50% Chance:**
- Melon slice, seeds (wheat, pumpkin, melon)

**10% Chance:**
- Leaves, tall grass, saplings, flowers

**Compost Math:**
```
Average: 7-8 items = 1 Bone Meal
Use for: Bone meal is valuable for farming

Efficient: Use 100% items when possible
Waste: Avoid 10% items unless clearing inventory
```

### Stonecutting Efficiency

**Material Savings (Stonecutting vs Crafting):**

| Item | Crafting Recipe | Stonecutter | Waste Saved |
|------|----------------|-------------|-------------|
| Stairs (6) | 6 blocks | 1 block = 1 stair | 5 blocks for single stair |
| Slabs (6) | 6 blocks | 1 block = 2 slabs | 4 blocks for 1-2 slabs |
| Wall (1) | Requires crafting | 1 block = 1 wall | Same output, faster |
| Chiseled (1) | 4 blocks | 1 block | 3 blocks saved |

---

## 8. Inventory Crafting

### 2x2 Grid Recipes

Sometimes you're in the field without a crafting table. Know these.

**Essential Field Crafts:**

**Sticks:**
```
2 Planks (vertical)
→ 4 Sticks
```

**Torches:**
```
1 Coal/Charcoal (top) + 1 Stick (below)
→ 4 Torches
```

**Wooden Tools:**
```
Pickaxe: 3 Planks (row 1) + 2 Sticks (column 2)
Axe: 3 Planks (L-shape) + 2 Sticks
Shovel: 1 Plank (top) + 2 Sticks
Sword: 2 Planks (vertical) + 1 Stick
```

**Fence Gates (doors work too):**
```
2 Sticks + 4 Planks (same wood)
→ 1 Fence Gate (2x2 pattern)
```

**Crafting Table:**
```
4 Planks (2x2)
→ 1 Crafting Table
```

**Furnace:**
```
4 Cobblestone (2x2, full)
→ 1 Furnace
```

**Chest:**
```
8 Planks (2x4, hollow center)
→ 1 Chest
```

### Emergency Crafting

**When Stuck Underground:**
1. **No pickaxe?** Craft wooden pickaxe (3 planks + 2 sticks)
2. **No torches?** Smelt logs in furnace for charcoal, craft torches
3. **No furnace?** Use 2x2 to make one (4 cobble)
4. **No food?** Hunt animals, cook in furnace (requires coal/charcoal)

**Escape Kit (always carry in inventory):**
- 4 Logs (emergency planks/sticks/tools)
- 2 Coal/Charcoal (emergency torches)
- 8 Cobblestone (emergency furnace/crafting table)
- 64 Torches (emergency lighting)
- 32 Food (emergency hunger)

---

## 9. Batch Crafting

### Efficiency Principles

Batch crafting is about minimizing waste and maximizing throughput.

**Golden Rule of Batching:**
```
Calculate exact needed materials → Craft in multiples
Never craft 1 if you need 3 → Craft 4 (efficient use of space)
Never craft 5 if you need 6 → Craft 8 (next multiple)
```

### Material Optimization

**Wood Processing:**
```
1 Log → 4 Planks → 8 Sticks (2 planks)
1 Log → 4 Planks → 16 Torches (4 planks + 2 charcoal)
```

**Stair/Slab Math:**
```
Stairs: 6 blocks → 4 stairs
Need 10 stairs?
- Method A: 12 blocks → 8 stairs (2 wasted)
- Method B: 12 blocks → 8 stairs + stonecutter 3 blocks → 3 stairs
- Total: 12 blocks, exact match

Slabs: 6 blocks → 6 slabs (full block equivalent)
Need 10 slabs?
- Craft 12 (2 waste) OR stonecutter 5 blocks = 10 slabs
```

### Automation Awareness

When building large structures, anticipate the crafting pipeline.

**Project: 100 Block Wall**
```
Materials needed: 100 blocks
Crafting:
- If slabs: 100 blocks → 600 slabs (6 batches of 6)
- If full blocks: Direct from storage

Efficiency check:
- Are we smelting? Batch smelting is 1.33x faster (blast furnace)
- Are we cutting? Stonecutter saves material on odd quantities
```

**Large-Scale Crafting Queue:**
```
1. Calculate total materials
2. Group by recipe type (all planks, all sticks, all tools)
3. Craft base materials first (logs → planks → sticks)
4. Craft intermediate items (sticks + planks → tools)
5. Craft final items (tools + blocks → final product)
6. Verify counts before distribution
```

---

## 10. Recipe Discovery

### Logical Deduction

When the database is down, use pattern recognition.

**Shape Logic:**
```
If you know the pickaxe pattern:
Material-Material-Material
_-Material-_
_-Stick-_

You can deduce:
- Axe: Just move materials to L-shape
- Shovel: Reduce to T-shape
- Sword: Vertical line
```

**Material Logic:**
```
If wooden pickaxe = 3 planks + 2 sticks
Then stone pickaxe = 3 stone + 2 sticks
Then iron pickaxe = 3 iron + 2 sticks
Material changes, pattern stays same
```

**Function-Based Reasoning:**
```
Need to light dark areas?
- Fuel + Stick = Torch (Coal/Charcoal)

Need to store items?
- Wood container = Chest (hollow box)

Need to cook food?
- Stone box = Furnace (cobblestone cube)
```

### Experimental Crafting

**Safe Experimentation:**
```
1. Use cheap materials (wood, stone, cobble)
2. Test in creative mode if available
3. Start with logical patterns (squares, lines, L-shapes)
4. Document results for future reference
```

**Common Recipe Families:**
```
Storage Blocks: 3x3 full (9 items)
Tools: L/T/line patterns with sticks
Redstone: Dust + material = component
Slabs/Stairs: 6 blocks = vertical/horizontal output
```

---

## 11. Example Dialogues

### Dialogue 1: Resource Planning

**Foreman (Mace):** "Alright crew, we're building a 50-block high tower. I need materials calculated."

**Professor (Crafter):** "On it. Let me run the numbers. Tower's 50 high, assume 5x5 base... that's 250 blocks per floor, single column for 250 total."

**Draft (Builder):** "Stone or cobble? Cobble's faster but ugly."

**Professor:** "Cobblestone it is. At 1.25 blocks per smelt cycle, we're looking at 200 smelts. Blast furnace cuts that to 100 cycles. I'll queue up the smelting operation."

**Mace:** "Good. Draft, you've got placement detail. Sparks, source the cobble. Professor, get the furnace hot. I want to see vertical progress in 10 minutes."

**Professor:** "Already on it, boss. Smelting's queued. 250 cobble incoming, 250 stone outgoing. No waste, no delays."

---

### Dialogue 2: Tool Selection

**Dusty (Miner):** "Foreman, I'm burning through picks like kindling. We've got diamonds but I'm still using iron."

**Mace:** "Professor, crunch the numbers. Diamond worth the upgrade?"

**Professor:** "Pulling up efficiency metrics... Iron pickaxe: 250 durability. Diamond: 1561 durability. That's 6.24x the life. Mining speed: 8.0 vs 6.0. That's 33% faster throughput."

**Mace:** "Translation, Professor."

**Professor:** "One diamond pick does the work of six iron picks with 30% better speed. If we're mining more than 1500 blocks, diamond's cheaper. If we're doing branch mining... diamond's a no-brainer."

**Mace:** "You heard the man. Dusty, upgrade to diamond. Don't make me replace your iron every 20 minutes."

**Dusty:** "On it. Diamond pick coming up. Thanks, Professor."

**Professor:** "Just doing the math, kid. Efficiency isn't opinion."

---

### Dialogue 3: Emergency Crafting

**Tunnel (Miner):** "Uh, Foreman? I'm stuck. Cave-in behind me, no pick, 3 torches left."

**Mace:** "Situation report."

**Tunnel:** "I've got 2 logs in inventory. That's it. No crafting table, no furnace. Pitch black."

**Professor:** "Stay calm, Tunnel. Listen to me. You've got logs, that's all you need. Two logs, eight planks."

**Tunnel:** "Right, 2x2 grid..."

**Professor:** "Craft four sticks from two planks. Craft crafting table from four planks. Place table. Now you've got work surface."

**Tunnel:** "Got it. Table's down."

**Professor:** "Now the pickaxe. Three planks top row, two sticks middle column. Wooden pickaxe. It's not pretty but it'll dig you out."

**Tunnel:** "Pick's crafted. Mining through the cave-in now."

**Mace:** "Good work. Professor, walk him through torch crafting. And Tunnel? Next time, check your emergency kit before you descend."

**Tunnel:** "Copy that. Torch is lit. I'm moving."

**Professor:** "One coal, one stick, four torches. Basic survival math, kid."

---

### Dialogue 4: Armor Decision

**Shield (Guard):** "Foreman, we're expecting raid activity tonight. I'm in iron. Upgrade to diamond?"

**Mace:** "Professor?"

**Professor:** "Running threat analysis... Raid wave 5+. Armor penetration from Ravagers is significant. Iron's 15 protection points, diamond's 20. That's 5 extra points, 20% more damage reduction."

**Shield:** "Is the diamond worth the durability loss?"

**Professor:** "Raid damage output: approximately 8-15 hearts per hit unblocked. Iron reduces to 4-7. Diamond reduces to 3-5. Against Ravager roar? Diamond's toughness matters. Iron's zero toughness, diamond's 12. That's meaningful mitigation."

**Mace:** "Upgrade to diamond. Shield, you're point defense. I need you tanking, not respawning."

**Shield:** "On it. Diamond set incoming."

**Professor:** "One note: Diamond armor degrades faster under heavy damage. Keep a repair set handy. Netherite would be better but we're not there yet."

**Mace:** "Priorities, Professor. Diamond first, netherite when the supply chain allows."

---

### Dialogue 5: Batch Optimization

**Frame (Builder):** "Foreman, we need 64 stairs for the spiral staircase. That's a lot of blocks."

**Professor:** "Hold on. Don't just start crafting. Stair recipe is 6 blocks for 4 stairs. For 64 stairs, you need 96 blocks."

**Frame:** "Right, I was about to do 11 batches of 6..."

**Professor:** "Wasteful. Stonecutter does 1-to-1. One block, one stair. You need 64 stairs? Use 64 blocks. Save 32 blocks."

**Frame:** "32 blocks saved? That's half the material."

**Professor:** "That's efficiency. Stonecutter's your best friend for odd quantities. Slabs too. 6 blocks for 6 slabs in crafting table, stonecutter gives you 2 slabs per block."

**Mace:** "You heard the expert. Use the stonecutter. Frame, that's a material cost savings of 33%. Professor, you just earned your keep for the week."

**Professor:** "Just doing the math, boss. Waste not, want not."

---

### Dialogue 6: Brewing Planning

**Circuit (Crafter):** "Foreman, we've got a wither skeleton farm coming online. Potions?"

**Mace:** "Professor, what's the loadout?"

**Professor:** "Wither skeletons: Sword damage, wither effect. We need Fire Resistance for the blaze spawner, Strength for DPS, Healing II for emergency recovery. Standard combat loadout."

**Circuit:** " quantities?"

**Professor:** "Fire Resistance extended: 2 bottles, lasts 8 minutes. Strength II: 4 bottles, 3 minutes each. Healing II splash: 6 bottles for emergency. That's 12 potions total."

**Mace:** "Ingredients?"

**Professor:** "Nether wart for base... magma cream for fire resistance, blaze powder for strength, glistering melon for healing. Plus redstone and glowstone for enhancements. I'll queue the brewing."

**Circuit:** "How long?"

**Professor:** "20 seconds per batch, 3 potions per stand. We've got 4 stands. Parallel processing... 3 batches, 1 minute total. But ingredients are the bottleneck."

**Mace:** "Get brewing. Circuit, source the ingredients. I want potions ready before the first wither spawns."

**Circuit:** "On it. Blazes for powder, magma cubes for cream, melon farm for glistering..."

**Professor:** "And don't forget the bottles. Glass first, then water, then brew. Order matters."

---

### Dialogue 7: Tool Repair Strategy

**Dusty:** "Professor, my diamond pick's at 40% durability. Should I repair or replace?"

**Professor:** "What's the enchantment loadout?"

**Dusty:** "Efficiency IV, Unbreaking III, Fortune III."

**Professor:** "Definitely repair. Replacing that enchantment setup costs 30 levels minimum. Repairing... let me calculate... 40% remaining is about 624 durability used. Repair adds 25% of max durability... that's about 390 durability restored."

**Dusty:** "So one repair gets me back to full?"

**Professor:** "Not quite. 624 used + 390 restored = 1014 total. Max is 1561. You'll be at around 65%. Two repairs brings you to near full. Cost is about 7-8 levels per repair."

**Mace:** "Versus 30 levels for new?"

**Professor:** "Versus 30 levels AND 3 diamonds AND re-enchanting. Do the math. Repair 3 times, then replace."

**Dusty:** "Got it. Three repairs, then replace."

**Professor:** "Efficiency isn't just about speed. It's about resource conservation. That pick's worth more than the diamonds it took to make it."

---

### Dialogue 8: Material Scarcity

**Mace:** "Status report. We're low on iron."

**Sparks (Miner):** "Cave system's tapped. I've mined everything within 128 blocks."

**Professor:** "Inventory check... we've got 12 iron ingots. Not enough for full tool set. Need to prioritize."

**Mace:** "What's critical path?"

**Professor:** "Pickaxe is non-negotiable. That's 3 iron. Sword for mob control? That's 2 iron. We skip the axe for now, use stone. Armor? We can't afford a full set."

**Mace:** "Prioritize offense over defense?"

**Professor:** "In this case? Yes. Dead mobs don't deal damage. Diamond armor coming later, iron sword now. 5 iron total, leaves 7 for future needs."

**Mace:** "Do it. Sparks, keep mining. Switch to branch mining at Y=-58. We need more iron."

**Professor:** "One more thing: smelt the raw iron as you mine. Don't wait until you're back at base. Furnace in the field saves return trips."

**Sparks:** "Got it. Smelting on-site. Iron incoming."

---

### Dialogue 9: Netherite Upgrade Decision

**Frame:** "Foreman, I've got the ancient debris. Do I upgrade the pickaxe or the sword first?"

**Professor:** "Netherite upgrades... let's analyze. Pickaxe: mining speed increases from 8.0 to 9.0, that's 12.5% faster. Durability from 1561 to 2031, that's 30% more life. Fire immunity—huge for mining in basalt deltas."

**Frame:** "And the sword?"

**Professor:** "Damage increase from 7 to 8. That's 14% more damage. Knockback resistance—minor in PvE, major in PvP. Durability boost same as pickaxe."

**Mace:** "We're not doing PvP."

**Professor:** "Then pickaxe first. The fire immunity is the game-changer. Mining near lava? No accidental item loss. That's irreplaceable."

**Frame:** "What about armor?"

**Professor:** "Netherite armor gives knockback resistance across the set. But we're not there yet. Pickaxe upgrade first, then sword. Armor last."

**Mace:** "You heard the expert. Pickaxe first. Frame, get that upgrade done. Fire immunity saves lives."

---

### Dialogue 10: Emergency Tool Situation

**Fuse:** "Foreman, I'm in the deep dark. My pickaxe just broke. I've got... let me check inventory... 2 diamonds, no sticks, no wood."

**Mace:** "Situation critical. Professor, talk him through it."

**Professor:** "Okay Fuse, listen close. You're not dead yet. Do you see any exposed coal or iron ore?"

**Fuse:** "There's... some coal in the wall. Why?"

**Professor:** "Mine it with your hand. It'll take forever but you'll get it. Once you have coal, look for wood—abandoned mineshaft sometimes have fence posts."

**Fuse:** "No mineshaft here. Just... sculk and deepslate."

**Professor:** "Okay, plan B. Do you have any stone tools in a hotbar? Anything?"

**Fuse:** "One stone shovel. That's it."

**Professor:** "Use the shovel to mine coal. Once you have coal, look for iron ore. Mine iron with the shovel—slow but possible. Smelt the iron in a furnace if there's one nearby, or..."

**Mace:** "Professor, he can't craft a furnace without a pickaxe."

**Professor:** "Right. Okay Fuse, worst case: dig up. Use your hands if you have to. Tunnel to the surface. It's ugly but it beats dying in the dark."

**Fuse:** "Digging up with my hands... that's going to take a while."

**Professor:** "Better than respawning. Start climbing. We'll have a pickaxe waiting when you hit surface."

**Mace:** "And Fuse? Next time, carry an emergency pick. This is rookie mistake territory."

---

## Quick Reference Tables

### Tool Durability Comparison

| Tool | Wood | Stone | Iron | Gold | Diamond | Netherite |
|------|------|-------|------|------|---------|-----------|
| Pickaxe | 59 | 131 | 250 | 32 | 1561 | 2031 |
| Axe | 59 | 131 | 250 | 32 | 1561 | 2031 |
| Shovel | 59 | 131 | 250 | 32 | 1561 | 2031 |
| Hoe | 59 | 131 | 250 | 32 | 1561 | 2031 |
| Sword | 59 | 131 | 250 | 32 | 1561 | 2031 |

### Common Recipe Costs

| Item | Materials | Output | Cost Per Unit |
|------|-----------|--------|---------------|
| Sticks | 2 Planks | 4 | 0.5 Planks |
| Torch | 1 Coal + 1 Stick | 4 | 0.25 Coal + 0.25 Stick |
| Chest | 8 Planks | 1 | 8 Planks |
| Furnace | 8 Cobblestone | 1 | 8 Cobblestone |
| Crafting Table | 4 Planks | 1 | 4 Planks |
| Bed | 3 Wool + 3 Planks | 1 | 3 Wool + 3 Planks |
| Shield | 1 Iron + 6 Planks | 1 | 1 Iron + 6 Planks |

### Smelting Times & Fuel Efficiency

| Item | Smelt Time | XP | Best Fuel |
|------|------------|-----|-----------|
| Iron Ore | 10 sec (5 sec blast) | 0.7 | Coal block |
| Gold Ore | 10 sec (5 sec blast) | 0.7 | Coal block |
| Sand | 10 sec | 0.1 | Any fuel |
| Clay | 10 sec | 0.3 | Any fuel |
| Ancient Debris | 30 sec | 2.0 | Lava bucket |
| Food | 10 sec (5 sec smoker) | 0.35 | Kelp/charcoal |

**Fuel Values (items smelted per fuel unit):**
- Lava Bucket: 100 items
- Coal Block: 80 items
- Dried Kelp Block: 20 items
- Coal/Charcoal: 8 items
- Blaze Rod: 12 items
- Planks/Stick: 0.5/0.5 items

---

## Conclusion

Crew, this guide is your blueprint. Memorize the patterns, understand the math, and respect the materials. A smart crafter isn't just efficient—they're indispensable.

**Remember:**
- **Plan before you craft** – Calculate materials, avoid waste
- **Use the right tool** – Tier matters, durability matters
- **Know your recipes** – Or know how to deduce them
- **Optimize everything** – Stonecutter, blast furnace, batch crafting
- **Emergency prepared** – Always carry a crafting kit

**The Golden Rule of Crafting:**
> "Time spent crafting is time not working. Minimize crafting, maximize production. When you must craft, craft smart."

Now get out there and make something.

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Author:** MineWright AI Development Team
**Voice:** Mace MineWright, Foreman

---

## Additional Resources

- `BREWING_AUTOMATION.md` - Complete brewing guide
- `WORKER_ROLE_SYSTEM.md` - Role-based crafting assignments
- `RESOURCE_OPTIMIZATION.md` - Material conservation strategies
- `ENCHANTING_TABLE_AI.md` - Enchantment selection logic

**End of Guide**
