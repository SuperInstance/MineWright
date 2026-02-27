# STORAGE HOW-TO GUIDE
## Minecraft Storage Systems for AI Agents

**Target Audience:** AI Workers and Foremen
**Version:** 1.20.1
**Last Updated:** 2026-02-27

---

## TABLE OF CONTENTS

1. [Container Types](#1-container-types)
2. [Sorting Systems](#2-sorting-systems)
3. [Storage Room Design](#3-storage-room-design)
4. [Inventory Management](#4-inventory-management)
5. [Bulk Storage](#5-bulk-storage)
6. [Special Item Storage](#6-special-item-storage)
7. [Team Storage](#7-team-storage)
8. [Mobile Storage](#8-mobile-storage)
9. [Redstone Storage](#9-redstone-storage)
10. [Retrieval Efficiency](#10-retrieval-efficiency)
11. [Example Dialogues](#11-example-dialogues)

---

## 1. CONTAINER TYPES

### 1.1 Chests
**Best For:** General storage, accessible items, frequently used resources

**Pros:**
- Large capacity (27 slots)
- Easy to craft (8 planks)
- Can be placed adjacent to form double chests (54 slots)
- Compatible with hoppers and minecarts
- Transparent inventory view

**Cons:**
- Cannot be moved when full (breaks into individual items)
- Requires block space above for opening
- Opaque block (can't see through)

**When to Use:**
- Main item storage rooms
- Construction material caches
- Resource stockpiles near work sites
- Any storage needing quick access

**Best Practices:**
- Always leave walking space around double chests
- Place with trapdoors for compact access
- Use signs above for labeling
- Group related items in same chest area

---

### 1.2 Barrels
**Best For:** Compact storage, decorative builds, under-floor storage

**Pros:**
- Same capacity as chests (27 slots)
- Can be opened without space above
- Transparent block (light passes through)
- Can be placed in solid floors
- Smaller hitbox

**Cons:**
- Cannot form double containers
- Slightly more expensive (6 planks + 2 slabs)
- Less recognizable as storage

**When to Use:**
- Compact storage rooms with ceiling constraints
- Decorative storage (farms, houses)
- Under-floor or in-wall storage
- Situations where aesthetics matter

**Best Practices:**
- Use in rows for clean appearance
- Great for item sorters with tight spaces
- Perfect for underground farms
- Combine with different wood types for coding

---

### 1.3 Shulker Boxes
**Best For:** Portable storage, valuable items, inventory compression

**Pros:**
- Retains items when broken
- Can be dyed (16 colors for coding)
- Portable storage solution
- Works with item frames and glow item frames
- Can be placed in chests

**Cons:**
- Expensive (requires shulker shells - end game)
- Only 27 slots (same as single chest)
- Cannot be automated with hoppers when moving

**When to Use:**
- Mobile bases and inventory systems
- Toolkits for specific jobs
- Valuable item transport
- Building supply kits
- Emergency supply caches

**Color Coding Convention:**
- **Red:** Combat and emergency supplies
- **Orange:** Tools and equipment
- **Yellow:** Building materials (cobble, stone, dirt)
- **Lime:** Farming and seeds
- **Green:** Nature blocks (wood, leaves, saplings)
- **Cyan:** Redstone components
- **Light Blue:** Transportation (rails, minecarts, elytra)
- **Blue:** Minerals and ores
- **Purple:** Enchanted gear and books
- **Magenta:** Potions and brewing
- **Pink:** Decorative blocks
- **White:** Misc items and overflow
- **Light Gray:** Food and consumables
- **Gray:** Raw materials and crafting components
- **Black:** Trash and disposable items
- **Brown:** Nether and End materials

---

### 1.4 Ender Chests
**Best For:** Personal storage, valuable items, cross-base access

**Pros:**
- Shared inventory across all ender chests
- Only accessible by placer (in survival)
- Fire and lava proof
- Compact storage solution
- Perfect for valuable items

**Cons:**
- Very small (27 slots total)
- Expensive (obsidian + eye of ender)
- Cannot be expanded to double
- Not compatible with hoppers (into chest)

**When to Use:**
- Personal valuable storage
- Diamond/emerald/netherite hoarding
- Emergency backup gear
- Frequently needed items across bases
- Tool sharing between agents

**Best Practices:**
- Reserve for most valuable items only
- Use for enchanted tools and armor
- Store rare non-renewable resources
- Keep emergency gear (food, weapons, tools)
- Coordinate team access in multi-agent setups

---

## 2. SORTING SYSTEMS

### 2.1 Manual Sorting Categories

**Tier 1: Broad Categories (Basic)**
```
1. Tools & Weapons
2. Armor & Protection
3. Building Blocks
4. Natural Materials
5. Redstone & Rails
6. Food & Farming
7. Minerals & Ores
8. Misc & Decor
```

**Tier 2: Detailed Categories (Standard)**
```
Building Materials:
  - Stone variants (cobble, stone, andesite, etc.)
  - Wood variants (oak, spruce, birch, etc.)
  - Bricks & concrete
  - Glass & lighting
  - Terracotta & glazed

Natural Materials:
  - Dirt & sand variants
  - Gravel & raw stone
  - Nether blocks
  - End blocks
  - Coral & flora

Redstone:
  - Basic components (redstone, repeaters, comparators)
  - Pistons (regular, sticky, slime, honey)
  - Hoppers, droppers, dispensers
  - Rails & minecarts
  - Daylight detectors, observers

Farming:
  - Seeds & saplings
  - Flowers & crops
  - Farming tools (hoes, water buckets)
  - Compost & bonemeal
  - Animal drops (eggs, wool)

Minerals:
  - Raw ores (coal, iron, copper, etc.)
  - Raw precious metals (gold, ancient debris)
  - Gems (diamond, emerald, lapis, redstone)
  - Nether materials
  - End materials

Combat:
  - Weapons (swords, axes, bows, crossbows, tridents)
  - Armor (helmet, chestplate, leggings, boots)
  - Shields & totems
  - Arrows & projectiles
  - Enchanted books (combat)

Food:
  - Raw foods
  - Cooked foods
  - Prepared foods (bread, cake, cookies)
  - Potions
  - Golden foods

Tools:
  - Pickaxes (wood, stone, iron, diamond, netherite)
  - Axes, shovels, hoes
  - Shears, flint & steel
  - Fishing rods
  - Carrot on a stick, warped fungus on a stick

Decorative:
  - Wool & carpets
  - Stained glass & clay
  - Flowers & plants
  - Heads & banners
  - Amethyst & other crystals
```

**Tier 3: Hyper-Specific (Advanced)**
```
Each item type gets its own chest or dedicated section
Best used with auto-sorters and large storage rooms
```

---

### 2.2 Item Categorization Logic

**Primary Sort: By Function**
- What is the item USED FOR?
- Where will you NEED it?
- How OFTEN is it accessed?

**Secondary Sort: By Material**
- After function, group by material tier
- Wood > Stone > Iron > Gold > Diamond > Netherite

**Tertiary Sort: By Rarity**
- Common > Uncommon > Rare > Epic > Legendary
- Keep rare items more accessible

---

### 2.3 Label Conventions

**Sign Format:**
```
[SHORT NAME]
  - Category
  - Count/Status

Example:
[COBBLE]
  - Building
  - 12 DC
```

**Item Frame Labels:**
- Use the item itself as label
- Place on front of chest
- Add glow item frame for night visibility
- Rotate item to indicate category

**Color Coding:**
- Use wool blocks behind chests
- Carpet on top for quick identification
- Concrete for permanent marking
- Stained glass for transparency with color

**Naming Convention for Signs:**
```
Format: [CATEGORY] - [SUBCATEGORY] - [STATUS]

Examples:
[BUILD] - Stone - Full
[BUILD] - Wood - 3/4
[REDSTONE] - Basic - Overflow
[FOOD] - Raw - Empty
```

---

## 3. STORAGE ROOM DESIGN

### 3.1 Layout Patterns

**Pattern 1: Corridor Style**
```
┌─────────────────────────────────┐
│  [C][C]  [C][C]  [C][C]  [C][C]  │
│  [C][C]  [C][C]  [C][C]  [C][C]  │
│                                 │
│  [C][C]  [C][C]  [C][C]  [C][C]  │
│  [C][C]  [C][C]  [C][C]  [C][C]  │
└─────────────────────────────────┘
```
- **Best For:** Large, organized rooms
- **Advantages:** Easy expansion, clear categories
- **Disadvantages:** Requires large space

**Pattern 2: Room Style**
```
    ┌──┐ ┌──┐ ┌──┐ ┌──┐
    │CC│ │CC│ │CC│ │CC│
    │CC│ │CC│ │CC│ │CC│
└───┴──┴─┴──┴─┴──┴─┴──┴─┴──┐
│                           │
│    Walking/Crafting Area  │
│                           │
└───┬──┬─┬──┬─┬──┬─┬──┬─┴──┐
    │CC│ │CC│ │CC│ │CC│
    │CC│ │CC│ │CC│ │CC│
    └──┘ └──┘ └──┘ └──┘
```
- **Best For:** Compact multi-level storage
- **Advantages:** Central access, visual clarity
- **Disadvantages:** More walking between categories

**Pattern 3: Wall Style**
```
┌───────────────────────────────┐
│  [C][C][C][C][C][C][C][C]     │
│  [C][C][C][C][C][C][C][C]     │
│                               │
│                               │
│     Central Workspace         │
│                               │
│                               │
│  [C][C][C][C][C][C][C][C]     │
│  [C][C][C][C][C][C][C][C]     │
└───────────────────────────────┘
```
- **Best For:** Underground bases, tight spaces
- **Advantages:** Maximum density, efficient lighting
- **Disadvantages:** Harder to expand

---

### 3.2 Accessibility Standards

**Clearance Requirements:**
- **Single Chest:** 1 block walking space
- **Double Chest:** 2 blocks walking space recommended
- **Chest Rows:** 2 blocks between rows minimum
- **Ceiling Height:** 3 blocks minimum (2 for head, 1 for lighting)

**Access Patterns:**
- Place frequently used items at eye level
- Keep bulk storage low or high
- Leave center of room clear for crafting
- Use trapdoors for compact access patterns

**Agent-Specific Considerations:**
- Plan paths for automated agents
- Leave space for hopper systems
- Consider minecart access for large quantities
- Design for multiple agents accessing simultaneously

---

### 3.3 Expandability Planning

**Growth Space:**
- Always leave 20% empty capacity in main areas
- Plan for expansion tunnels/rooms
- Use modular sections that can be duplicated
- Keep sorting systems flexible

**Tiered Expansion:**
```
Tier 1: Basic Chests (0-100 items)
Tier 2: Organized Room (100-1000 items)
Tier 3: Multi-Room Complex (1000-10000 items)
Tier 4: Automated Sorting (10000+ items)
```

**Expansion Markers:**
- Mark planned expansion areas with temporary blocks
- Leave space walls between sections
- Plan redstone routes for future auto-sorters
- Document layout patterns for replication

---

### 3.4 Lighting and Atmosphere

**Lighting Levels:**
- Minimum: Light level 8 (prevent spawns)
- Recommended: Light level 12-14 (clear visibility)
- Premium: Light level 15 (full brightness)

**Lighting Patterns:**
- **Glowstone:** Central ceiling placement
- **Sea Lanterns:** Clean, modern look
- **Lanterns:** Classic atmosphere
- **Shroomlights:** Nature-themed areas
- **End Rods:** End-themed storage

**Anti-Spawn Measures:**
- Every corner must have light level 8+
- Use slabs or carpet on floors if needed
- Place half-slabs behind chests
- No 2x2 dark spaces anywhere

---

## 4. INVENTORY MANAGEMENT

### 4.1 Personal Inventory Organization

**Hotbar Setup (1-9):**
```
[1]  Primary Tool (Pickaxe)
[2]  Secondary Tool (Axe/Shovel)
[3]  Block/Place Block (Cobblestone/Torch)
[4]  Utility (Bucket/Water)
[5]  Food (Steak/Bread)
[6]  Weapon (Sword/Bow)
[7]  Special (Shears/Flint&Steel)
[8]  Block Type 2 (Dirt/Sand)
[9]  Quick Access Block (Workbench/Furnace)
```

**Inventory Rows (Bottom 27):**
```
Row 1 (27-22): Current project materials
Row 2 (21-16): Tools and backup equipment
Row 3 (15-10): Building supplies
Row 4 (9-4): Miscellaneous collected items
Offhand: Torch/Block/Shield
```

---

### 4.2 Hotbar Organization Strategies

**Strategy A: Task-Based**
- Mining setup (pick, torches, food, buckets)
- Building setup (blocks, placement tools)
- Combat setup (weapons, armor, potions)
- Farming setup (hoes, seeds, water)

**Strategy B: Tier-Based**
- [1-3]: Diamond/Netherite tools
- [4-6]: Iron/Gold tools
- [7-9]: Stone/Wood tools (disposables)

**Strategy C: Frequency-Based**
- [1]: Most used item
- [9]: Least used hotbar item
- Adjust based on current task

---

### 4.3 Overflow Handling

**Priority Dump Order:**
1. Stone/Cobblestone (most common)
2. Dirt/Gravel (common but useful)
3. Wooden items (renewable)
4. Common drops (string, sticks)
5. **KEEP:** Ores, gems, rare items

**Overflow Chests:**
- Place near workspace
- Label clearly "TO SORT"
- Empty daily into main storage
- Keep at least 2 available per work area

**Inventory Full Protocol:**
```
1. Check for trash items (drop)
2. Move valuable overflow to nearby chest
3. Return to main storage if necessary
4. Never drop valuable items
5. Use shulker boxes for extended capacity
```

---

### 4.4 Pickup Priorities

**Auto-Pickup Priority:**
1. **High Priority:** Rare drops, diamonds, enchanted items
2. **Medium Priority:** Iron, gold, redstone, lapis
3. **Low Priority:** Stone, dirt, cobble (full inventory = skip)
4. **No Pickup:** (When full) Common blocks, rotten flesh

**Smart Collection Rules:**
- Always pick up named items
- Always pick up enchanted items
- Always pick up ores
- Skip common blocks when inventory > 80% full
- Never leave diamond/tools on ground

---

## 5. BULK STORAGE

### 5.1 Stack Management

**Stack Size Reference:**
- Most items: 64 per stack
- Most tools/armor: 1 per item (no stack)
- Eggs, snowballs, etc.: 16 per stack
- Honey bottles, signs: 16 per stack
- Ender pearls: 16 per stack
- Buckets: 16 per stack (empty)
- Soup/stew: 1 per bowl

**Double Chest Capacity:**
- 54 slots × 64 items = 3,456 items (maximum)
- Realistic mixed storage: ~2,000 items
- Shulker boxes inside: 54 × 27 × 64 = 93,312 items (extreme)

---

### 5.2 Auto-Sorter Design Principles

**Basic Auto-Sorter Components:**
```
Chest → Hopper → Filter Hopper → Sort Chest
             ↓
          Junk Chest
```

**Filter Item Setup:**
- Place filter item in sorter hopper
- Use multiple hopper chain for complex sorting
- Timer-based for even distribution
- Minecart hopper systems for high volume

**Hopper Timing:**
- Items transfer every 8 ticks (0.4 seconds)
- Plan accordingly for high-volume systems
- Use redstone comparators to monitor fill level

---

### 5.3 Hopper Systems

**Single-Line Sorter:**
```
Input Chest → Hopper → [Filter] → Sorted Chest
                       ↓
                    Unsorted Chest
```

**Multi-Stage Sorter:**
```
Input → Filter 1 → Category A
     → Filter 2 → Category B
     → Filter 3 → Category C
     → Remainder → Unsorted
```

**Item Unloader:**
```
Minecart Hopper → Powered Rails → Unload Chest
                                    ↓
                              Hopper Chain → Storage
```

---

### 5.4 Storage Silos

**Vertical Silo Design:**
```
┌────────────────┐
│   Dropper      │
│   ↓            │
│   Chest        │
│   ↓            │
│   Hopper       │
│   ↓            │
│   Double Chest │  ← Main storage
│   Double Chest │
└────────────────┘
```

**Horizontal Silo Design:**
```
┌──┐┌──┐┌──┐┌──┐┌──┐
│DD││DD││DD││DD││DD│  ← Dropper line
└──┘└──┘└──┘└──┘└──┘
  ↓  ↓  ↓  ↓  ↓
┌─────────────────────┐
│   Chest Line        │  ← Storage access
└─────────────────────┘
```

**Advantages of Silos:**
- Massive single-item storage
- Compact footprint
- Easy item counting
- Simple redstone monitoring

---

## 6. SPECIAL ITEM STORAGE

### 6.1 Tools and Equipment

**Tool Storage Hierarchy:**
```
1. Currently In Use → Hotbar
2. Backup Tools → Inventory
3. Good Condition → Tool Chest
4. Damaged → Repair/Anvil
5. Disposable → Use until break
```

**Tool Chest Organization:**
```
Top Row:    Pickaxes (by tier)
Middle Row: Specialty tools (shovel, axe, hoe)
Bottom Row: Rare/Enchanted tools
```

**Durability Management:**
- Store tools by remaining durability
- Label: "TOOLS - < 50% USE"
- Keep one of each tier as backup
- Repair before critical failure

---

### 6.2 Armor Storage

**Armor Display Options:**
- Armor stands (decorative + functional)
- Item frames (space-saving)
- Dedicated armor chest (organized by set)

**Armor Sets:**
```
Full Set Example:
- Helmet (Diamond, Protection IV)
- Chestplate (Diamond, Protection IV)
- Leggings (Diamond, Protection IV)
- Boots (Diamond, Feather Falling IV)
```

**Storage by Use:**
- Combat armor → Ready access, near spawn
- Mining armor → Near mine entrance
- Farming armor → Near farms
- Special armor → Ender chest (valuable)

---

### 6.3 Potions and Brewing

**Potion Storage Considerations:**
- Potions stack to 1 (space intensive)
- Use splash vs. drinkable separation
- Label clearly with signs
- Keep brewing supplies nearby

**Brewing Station Setup:**
```
[Cauldron] [Brewing Stand] [Brewing Stand]
[Chest]    [Chest]         [Chest]
 Nether    Potions        Ingredients
 Warts     (Splash)       (Nether, etc.)
```

**Potion Categories:**
- **Healing:** Emergency, combat
- **Regeneration:** prolonged combat
- **Strength:** Major combat
- **Speed:** Travel, escape
- **Night Vision:** Caves, underwater
- **Fire Resistance:** Nether

---

### 6.4 Enchanted Books

**Organization by Enchantment:**
```
Combat: Sharpness, Smite, Bane of Arthropods
Protection: Protection, Fire Protection, Blast Protection
Tools: Efficiency, Fortune, Silk Touch, Unbreaking
Bow: Power, Punch, Flame, Infinity
Utility: Mending, Lure, Luck of the Sea
```

**Storage:**
- Separate by enchantment type
- Keep same-level enchantments together
- Use item frames for quick identification
- Store Mending books in ender chest

---

### 6.5 Rare and Valuable Items

**Classification:**
- **Common Renewable:** Dirt, cobble, wood
- **Uncommon Renewable:** Iron, gold, redstone (with farms)
- **Rare Renewable:** Ender pearls, slime, nautilus shells
- **Common Non-Renewable:** Coal, copper, lapiz (large but finite)
- **Rare Non-Renewable:** Diamonds, emeralds, ancient debris
- **Ultra-Rare:** Dragon head, elytra, enchanted golden apples

**Storage Guidelines:**
```
Common Renewable → Bulk storage, easy access
Rare Renewable → Organized storage, easy access
Non-Renewable → Secure storage, limited access
Ultra-Rare → Ender chest, maximum security
```

---

## 7. TEAM STORAGE

### 7.1 Shared Access Protocols

**Team Storage Rooms:**
- Mark clearly with signs: "TEAM STORAGE"
- Use consistent organization across team
- Lock valuable items (if modded)
- Document any changes to organization

**Access Levels:**
```
Level 1 - Public: Basic materials (dirt, stone, wood)
Level 2 - Team: Tools, armor, redstone
Level 3 - Restricted: Rare items, enchanted gear
Level 4 - Private: Personal valuable items
```

---

### 7.2 Resource Allocation

**Resource Distribution Strategy:**
```
Central Stockpile → Team Distribution → Personal Inventory
     (Bulk)           (Work Site)         (Active Use)
```

**Allocation Guidelines:**
- Take what you need, not what you want
- Leave 20% buffer for others
- Replenish what you use
- Return unused materials

**Foreman Responsibilities:**
- Monitor stock levels
- Distribute materials to workers
- Coordinate resupply runs
- Prevent resource hoarding

---

### 7.3 Conflict Prevention

**Sign-Out System (High-Value Items):**
```
[TOOL SIGN-OUT]
Item: Diamond Pickaxe
User: Worker_7
Time: 2026-02-27 14:30
Purpose: Mining trip to Y=-58
```

**Resource Reservation:**
- Use signs to reserve project materials
- Mark work areas clearly
- Communicate large withdrawals
- Return unused items promptly

**Conflict Resolution:**
1. Check if item is truly needed
2. Verify if alternative exists
3. Prioritize by urgency
4. Foreman makes final call

---

### 7.4 Multi-Agent Coordination

**Storage Coordination Patterns:**
```
Pattern A: Centralized
          ┌─────────┐
          │ Central │
          │ Storage │
          └────┬────┘
               │
    ┌──────────┼──────────┐
    ↓          ↓          ↓
[Agent 1]  [Agent 2]  [Agent 3]

Pattern B: Distributed
    ┌──────┐  ┌──────┐  ┌──────┐
    │Agent │  │Agent │  │Agent │
    │  1   │  │  2   │  │  3   │
    └───┬──┘  └───┬──┘  └───┬──┘
        │         │         │
        └────┬────┴────┬────┘
             ↓         ↓
        [Shared Cache] [Central Vault]
```

**Communication Protocols:**
- Announce large withdrawals
- Report low stock levels
- Share location of new caches
- Coordinate sorting efforts

---

## 8. MOBILE STORAGE

### 8.1 Donkey and Llama Storage

**Donkey/Mule Overview:**
- Inventory capacity: 15 slots
- Can wear chest
- Slower than horses but carry items
- Best for: Medium-distance transport

**Llama Overview:**
- Inventory capacity: 3, 6, 9, 12, or 15 slots (strength-based)
- Can form caravans
- Spit attack (self-defense)
- Best for: Long caravans, decor

**Equipment:**
```
Chest + Saddle/Horse Armor
Chest unlocks inventory slots
Saddle allows riding (donkeys/mules only)
```

**Best Practices:**
- Name animals with name tags
- Keep in secure pen when not in use
- Store at base for quick deployment
- Use for base-to-mine transport

---

### 8.2 Shulker Box Mobile Storage

**Backpack Configuration:**
```
Main Inventory
├─ Hotbar (9 slots)
├─ Main Inventory (27 slots)
└─ Offhand (1 slot)

With Shulker Box (in offhand):
└─ Shulker Box (27 slots accessible)

Maximum: 64 slots total (9 + 27 + 27 + 1)
```

**Carrying Strategies:**
- **Builder's Kit:** Shulker with building supplies
- **Mining Kit:** Shulker with rails, torches, buckets
- **Emergency Kit:** Shulker with food, armor, weapons
- **Sorter Kit:** Shulker with empty shulker boxes

**Inventory Tetris:**
```
Optimal packing for extended trips:
- 1 Shulker Box: Building materials
- 1 Shulker Box: Tools and equipment
- 1 Shulker Box: Food and supplies
- 1 Shulker Box: Empty boxes for loot
```

---

### 8.3 Ender Chest Base Network

**Network Setup:**
```
Home Base: Main Ender Chest (full access)
├─ Mining Outpost: Ender Chest (quick ore dump)
├─ Farming Station: Ender Chest (seed/tool access)
└─ Remote Base: Ender Chest (resource sharing)
```

**Items to Keep in Ender Chest:**
- Emergency diamond tools
- Backup armor
- Rare non-renewable resources
- Frequently needed items across bases
- Food for emergencies

**Network Advantages:**
- Instant access across bases
- No physical transport needed
- Perfect for multi-agent coordination
- Safe from most threats

---

### 8.4 Minecart Storage Systems

**Minecart with Chest:**
- 27 slots (same as single chest)
- Moves on rails
- Best for: Long-distance, high-volume transport

**Minecart with Hopper:**
- 5 slots
- Can pick up items
- Best for: Automatic collection, item transport

**System Design:**
```
Loading Station → Powered Rails → Unloading Station
     ↓                                  ↓
[Input Chest]                    [Output Chest]
     ↓                                  ↓
[Hopper to Minecart]             [Minecart to Hopper]
```

**Best Practices:**
- Use powered rails for speed
- Include stops at key stations
- Clear tracks before sending
- Label destination clearly

---

## 9. REDSTONE STORAGE

### 9.1 Item Sorting Systems

**Basic Sorter Design:**
```
Input Chest
     ↓
[Hopper with 1 filter item]
     ↓
[Comparator] → [Redstone] → [Item extracted to side]
     ↓
Remainder continues
```

**Multi-Item Sorter:**
```
                    ┌─→ [Diamond Chest]
Input → [Filter 1] ─┤
        (Diamond)   ├─→ [Iron Chest]
                    └─→ [Gold Chest]
```

**Compact Sorter (Wall-mounted):**
```
[Input] → [Hopper] → [Comparator] → [Dropper]
                            ↓
                      [Filtered Output]
```

---

### 9.2 Item Filters

**Hopper Filter:**
- Place one filter item in hopper
- All matching items go to hopper
- First slot acts as filter
- Remaining slots for overflow

**Minecart Hopper Filter:**
- Same principle as hopper
- Can move along collection line
- Good for farm collection

**Dropper Dispensing:**
- Random item from inventory
- Good for: Food dispensers, randomizers
- Use with comparator for detection

---

### 9.3 Automatic Storage Systems

**AFK Farm Collection:**
```
Farm → Water Stream → Hopper Line → Sorting System → Storage
```

**Mob Farm Storage:**
```
Spawner → Drop → Water → Hopper → [Sorter] → Rotten Flesh (Trash)
                                          → Bones (Store)
                                          → Gunpowder (Store)
                                          → Rare Drops (Special)
```

**Item Counter System:**
```
Chest → Hopper → Dropper → Counter
                      ↓
                 [Comparator Signal]
                      ↓
                 [Display/Redstone]
```

---

### 9.4 Redstone Silos

**Vertical Item Silo with Redstone:**
```
┌────────────────────────────┐
│     Dropper Tower          │
│     (Each dropper = 1 DC)  │
├────────────────────────────┤
│                            │
│  [Redstone Clock]          │
│  ← Controls extraction     │
└────────────────────────────┘
```

**Item Elevator:**
```
Bottom Chest → Hopper → Minecart → Powered Rail ↑
                                         ↓
                                  Top Chest
```

**Automatic Compacting:**
```
Raw Items → Crafting Table → Compact Items → Storage
(Cobble → Stone, Iron Blocks, etc.)
```

---

## 10. RETRIEVAL EFFICIENCY

### 10.1 Label Systems

**Visual Hierarchy:**
```
Level 1: Item Frames (immediate recognition)
Level 2: Signs with text (detailed info)
Level 3: Color Coding (category recognition)
Level 4: Map Coordinates (location reference)
```

**Label Templates:**
```
Simple: [ITEM NAME]
       Cobblestone

Detailed: [COBBLESTONE]
          Building - Common
          3.5 DC (Double Chests)

Full: [COBBLESTONE] [B-12]
      Category: Building - Stone
      Amount: ~3,400 blocks
      Last Restocked: 2026-02-27
      Reserved By: Worker_3
```

---

### 10.2 Location Finding

**Storage Map System:**
```
Create a map at each storage area:
- Mark rooms with banners
- Use different colors for categories
- Place maps in item frames at entrances
- Coordinate: X, Y, Z on signs
```

**Coordinate System:**
```
Format: [Building]-[Floor]-[Room]-[Row]-[Chest]

Example: MAIN-1-STONE-A-03
         Main Building, Floor 1, Stone Room, Row A, Chest 3
```

**Quick Reference Signs:**
```
┌─────────────────────────────┐
│ STORAGE DIRECTORY           │
├─────────────────────────────┤
│ [→] Stone: 10 blocks north  │
│ [↓] Dirt: Below             │
│ [↑] Wood: Floor 2           │
│ [←] Redstone: West Wing     │
└─────────────────────────────┘
```

---

### 10.3 Path Optimization

**Travel Time Calculation:**
- Walking: 4.3 blocks/second
- Sprinting: 5.6 blocks/second
- Minecart: 8 blocks/second (flat, powered)
- Ender Pearl: ~20 blocks/second (with cooldown)

**Layout Optimization:**
```
Most Used → Closest to spawn/work area
Less Used → Further away, still accessible
Rarely Used → Remote storage, bulk only
```

**Chest Access Time:**
- Opening chest: 0.25 seconds
- Taking item: Instant
- Closing chest: Instant
- Total per transaction: ~0.5-1 second

**Efficiency Tips:**
- Use shulker boxes to move bulk
- Carry multiple items in one trip
- Plan route before leaving
- Use minecarts for very large loads

---

### 10.4 Search Strategies

**Manual Search Method:**
```
1. Check item frames (if present)
2. Read signs (if labeled)
3. Check logical category location
4. Check overflow/unsorted chests
5. Ask other agents/team members
```

**Memory Aids:**
- Take photo/screenshot of storage layout
- Keep written notes in book and quill
- Create item location database (advanced)
- Use wool color memory tricks

**Common Item Locations (Convention):**
```
Spawn/Entrance → Tools, weapons, emergency
Main Floor → Common blocks, building materials
Basement → Mining, ores, redstone
Upper Floors → Farming, decorative, misc
Nether Portal → Nether materials
```

---

## 11. EXAMPLE DIALOGUES

### Dialogue 1: Setting Up a New Storage Room

**Foreman:** Alright team, we've got a new storage room to organize. I want this done systematic-like. Worker_7, you're on layout. What's the plan?

**Worker_7:** Boss, I'm thinking corridor style. Six rows of double chests, two-block walking paths between 'em. We'll sort by category - building, natural, redstone, farming, minerals, misc. Each row gets its own color coding on the floor.

**Worker_3:** I can handle the labeling. Item frames on the chests, signs above with category and count. We standardizing on the color code?

**Foreman:** You know it. Wool under the chests, orange carpet on top. Orange is building, we start there. Worker_2, you're placing the chests. Worker_7, once the layout's marked, fill 'em in order. Cobble, stone, dirt, gravel, sand - keep it alphabetical within category.

**Worker_2:** Got it, Boss. I'm laying down the orange wool now. How many double chests we planning per row?

**Worker_7:** Six per row gives us 324 slots per category. That's 20,736 items per category if we fill 'em. Should hold us through the next big mining op.

**Foreman:** Good math. Let's get to it. I want those glow item frames up by nightfall. We're not stumbling around in the dark looking for stone.

---

### Dialogue 2: Overflow Crisis

**Worker_2:** Foreman, we've got a situation. The cobblestone overflow is backing up into the unsorted chests. We're at capacity.

**Foreman:** How bad are we talking?

**Worker_2:** Three double chests full, and I've got two more in my inventory from the mining trip. No place to put it.

**Foreman:** Worker_3, grab a shulker box. Fill it with cobble, label it "COBBLE OVERFLOW - TEMP", stash it in the nether hub for now. We'll expand storage tomorrow.

**Worker_3:** On it, Boss. Should I dump the dirt overflow too?

**Foreman:** No, dirt goes in the silo. Worker_7, get on that hopper system. We need auto-sorting before we drown in cobble. This manual sorting is eating our efficiency.

**Worker_7:** Already sketched it out, Boss. Input chest, filter hopper line, six sorter chests. I can build it tonight.

**Foreman:** Make it so. Worker_2, finish your drop, then help Worker_3 with the shulker boxes. I don't want to see a single cobble block on the floor by sunset.

---

### Dialogue 3: Lost Items

**Worker_3:** Hey, anybody seen the diamond pickaxes? We're down two.

**Foreman:** Check the tool chest.

**Worker_3:** Did that. Only got three iron ones and a stone. The diamonds are gone.

**Worker_7:** I saw Worker_2 heading to the mines with one earlier. He might've taken the other by mistake.

**Foreman:** Worker_2, report in. Tool status.

**Worker_2:** Uh, yeah, about that. I've got one in my hotbar, other one's in the ender chest for safekeeping. Did a big run at Y=-58, didn't want to lose 'em.

**Foreman:** Good thinking on the ender chest, but communicate next time. We had Worker_3 searching for ten minutes. Protocol, people: sign out high-value tools in the logbook.

**Worker_2:** Won't happen again, Boss.

**Foreman:** See that it doesn't. Worker_7, make up a sign-out sheet. Name, tool, time, purpose. We're professionals, act like it.

---

### Dialogue 4: Organizing the Farming Supplies

**Worker_7:** Foreman, the farming chest is a mess. Seeds mixed with bone meal, hoes in with the wheat, I can't find nothing.

**Foreman:** Show me.

**Worker_7:** Look at this. Three stacks of seeds scattered, two hoes, a diamond axe in there somehow, and the bone meal's shoved in the back.

**Foreman:** That's not organization, that's a disaster. Worker_3, you're on detail. Empty it, sort it, restock it proper.

**Worker_3:** What's the order?

**Foreman:** Seeds first - wheat, carrots, potatoes, beetroot, melon, pumpkin, nether wart. One stack per type, consolidate the rest. Top row: bone meal. Middle row: hoes by tier - wood, stone, iron, diamond. Bottom row: buckets, water buckets, extra tools.

**Worker_7:** I'll make signs while Worker_3 sorts. "FARMING - SEEDS", "FARMING - TOOLS", "FARMING - SUPPLIES". Lime wool underneath for the green theme.

**Foreman:** Good teaming. While you're at it, check the wheat silo. We're running low on bread production.

**Worker_3:** On it, Boss. I'll do a full inventory count when we're done here.

---

### Dialogue 5: Redstone Storage Setup

**Worker_2:** Foreman, I'm building the auto-sorter, but I need redstone components. Where are we keeping 'em?

**Foreman:** Bottom floor, west wing. Look for the cyan carpet - that's redstone storage.

**Worker_2:** Found it. But we're running low on comparators. Only got six, and I need twelve for the full sorter.

**Foreman:** Check the nether chest. We've got a stash there for emergencies.

**Worker_2:** Got it. Hey Boss, while I'm here, can we reorganize this? Redstone dust, repeaters, comparators, pistons - it's all mixed.

**Foreman:** What's your plan?

**Worker_2:** Separation by function. Dust first, then transmission (repeaters, comparators), then movement (pistons, sticky pistons, slime, honey), then rails and minecarts, then misc (observers, daylight sensors).

**Foreman:** Logical. Do it. But finish the sorter first. We need that system online before the next mining op.

**Worker_2:** Can do. Should be done by dinner. Worker_7's helping me with the placement.

**Foreman:** Good. Don't forget to label everything. I don't want to be guessing which chest is which when I need redstone at midnight.

---

### Dialogue 6: Shulker Box Mobile Base

**Worker_3:** Foreman, I'm heading to the outpost for a week. Need mobile storage.

**Foreman:** How long you gone?

**Worker_3:** Seven days, minimum. Building a new bridge across the ravine.

**Foreman:** Take a shulker box. Fill it with building supplies - cobble, stone, dirt, gravel, wood. That's your materials box.

**Worker_3:** Got that. What about tools?

**Foreman:** Second shulker - tools only. Diamond pick, iron axe, shovel, two buckets of water, torches, flint and steel. Don't forget food - third shulker with steaks, bread, golden apples for emergencies.

**Worker_7:** I'll throw in an empty shulker for loot. Never know what you'll find out there.

**Worker_3:** That's four shulkers. I've only got so much inventory space.

**Foreman:** Keep three on you, stash the empty one in the ender chest. Pull it out when you need it. And Worker_3 - check in daily. If we don't hear from you, we send a search party.

**Worker_3:** Will do, Boss. Heading out at dawn.

---

### Dialogue 7: The Great Diamond Sort

**Worker_2:** Big news, Foreman. Worker_7 hit a vein of diamonds. We're talking four stacks.

**Foreman:** Four stacks? That's 256 diamonds. Where are we storing them?

**Worker_7:** Already in the diamond chest, Boss. But we should process some into gear.

**Foreman:** Agreed. Worker_3, count 'em up. We need to know exactly what we're working with.

**Worker_3:** 256 diamonds. That's enough for two full sets of diamond armor and tools, with leftovers.

**Foreman:** Let's be smart. One set for the team, rest goes in the ender chest for emergencies. Worker_7, you found 'em, you pick the gear set you want.

**Worker_7:** I'll take the standard - helmet, chestplate, leggings, boots, sword, pickaxe, axe.

**Foreman:** Worker_2, enchant that gear. Protection IV on the armor, Sharpness V on the sword, Fortune III on the pickaxe. We're not cutting corners.

**Worker_2:** On it. I've got 30 levels stored up. Should be enough for the full set.

**Foreman:** Remaining diamonds go in the ender chest. That's our rainy day fund. Nobody touches 'em without my say-so.

**Worker_7:** Understood, Boss. Anything left for me?

**Foreman:** Take a diamond block for your trouble. You earned it.

---

### Dialogue 8: Nether Hub Storage

**Worker_3:** Foreman, I'm doing a nether run. What storage we got there?

**Foreman:** We've got a small depot at the fortress. Double chest of netherrack, one of soul sand, half chest of quartz. Check the ender chest for gold and ancient debris.

**Worker_3:** How much ancient debris we expecting?

**Foreman:** Last run was three blocks. Should be enough for a netherite pick if we're careful. Don't waste it.

**Worker_7:** I'm going too. We can cover more ground. Two shulker boxes each - one for loot, one for supplies.

**Foreman:** Good plan. Worker_3, you take the gold route. Worker_7, you're on debris detail. Watch out for piglins - they've been agitated lately.

**Worker_3:** Got it. Fire resistance potions in the ender chest?

**Foreman:** Eight potions, splash type. Use 'em if you need 'em, but don't be wasteful. Those brews take time.

**Worker_7:** We'll be back by tomorrow night. With full shulkers if the fortress cooperates.

**Foreman:** Stay safe out there. And bring me back some nether wart - the farming stock is low.

---

### Dialogue 9: Winter Supply Check

**Foreman:** Winter's coming, team. We need to audit our supplies.

**Worker_2:** I'll check food stores. We've got three double chests of steak, two of bread, one of cooked pork.

**Foreman:** That's good for now. Worker_3, what's the coal situation?

**Worker_3:** Three double chests, nearly full. Should last us through winter, but we're not mining much.

**Foreman:** Keep a chest in reserve. We don't want to run out of smelting fuel. Worker_7, check the building materials. We got enough for indoor projects?

**Worker_7:** Cobblestone is overflowing, but we're low on wood. Only half a chest of oak, quarter chest of spruce.

**Foreman:** That's a problem. Worker_3, after your inventory check, take Worker_2 and hit the forest. We need three double chests of each wood type by the end of the week.

**Worker_3:** Can do. We'll replant as we go. No deforestation on our watch.

**Foreman:** Good. And keep an eye out for pumpkins - we're down to our last stack. Winter without pumpkin pie is no winter at all.

---

### Dialogue 10: Storage Room Expansion

**Worker_7:** Foreman, we're at capacity. The storage room's full, and we've got overflow in three different areas.

**Foreman:** How bad?

**Worker_7:** Building materials - full. Natural materials - full. We're storing dirt in the nether hub because we're out of space here.

**Foreman:** Time to expand. Worker_2, you're on demolition. Clear out the wall behind the redstone storage. We're going two rooms deeper.

**Worker_2:** How big we talking?

**Foreman:** Same layout as current. Corridor style, six rows, double chests. Color coding matches the existing system. Worker_3, you're on placement. Once the room's cleared, start laying chests.

**Worker_3:** Should we move anything during construction?

**Foreman:** No. Keep the storage live. We can't afford to shut down operations. Work around the chests, section by section.

**Worker_7:** I'll handle the relabeling. We need to update the signs to show Room A and Room B.

**Foreman:** Good thinking. Let's make it systematic - Room A is existing, Room B is expansion. Anyone asks, the signs tell the story.

**Worker_2:** Demolition starts at dawn. We should have the room cleared by nightfall.

**Foreman:** Excellent pace. Worker_3, get the materials ready. We're not stopping until we've got room for another 20,000 items.

---

## APPENDIX: QUICK REFERENCE

### Chest Capacity Quick Reference
- Single Chest: 27 slots
- Double Chest: 54 slots
- Shulker Box: 27 slots
- Ender Chest: 27 slots (shared)
- Donkey/Mule with Chest: 15 slots
- Minecart with Chest: 27 slots

### Stack Size Quick Reference
- 64 per stack: Most blocks and items
- 32 per stack: Some doors, slabs, signs (variants)
- 16 per stack: Eggs, snowballs, ender pearls, buckets, honey bottles
- 1 per item: Tools, weapons, armor, potions (most)

### Color Coding Quick Reference
- Red: Combat/Emergency
- Orange: Tools/Building
- Yellow: Basic Materials
- Green: Nature/Farming
- Blue: Minerals/Ores
- Purple: Enchanted/Rare
- Black: Trash/Disposable
- White: Misc/Overflow

### Container Use Cases Quick Reference
- Chest: General storage, accessible items
- Barrel: Compact storage, decorative
- Shulker Box: Portable storage, valuable items
- Ender Chest: Personal storage, cross-base access
- Donkey/Mule: Medium-distance transport
- Minecart: Long-distance transport

---

**END OF STORAGE HOW-TO GUIDE**

*Document Version: 1.0*
*Created for MineWright AI Agent System*
*Last Updated: 2026-02-27*

---

**NOTES:**

This guide is intended to be read by AI agents (workers and foremen) to help them make smarter decisions about storage in Minecraft. The construction crew dialogue style is intentional - it helps agents communicate in character while discussing storage systems.

When agents reference this guide, they should maintain the voice:
- Foremen: Directive, organizational, efficiency-focused
- Workers: Practical, execution-oriented, collaborative
- Shared understanding: Standardized terminology, clear protocols

The guide covers both basic and advanced storage techniques, ensuring agents at all knowledge levels can benefit from the information.
