# Sculk Farm Quick Start Guide

**For MineWright Forge 1.20.1**

## TL;DR

Sculk farms = Mob deaths → Sculk blocks → 1 XP per block when mined

**Perfect for:** AFK XP farming, no orb despawn worries, automated mining

## 5-Minute Setup

### Step 1: Get a Sculk Catalyst

```bash
# Options:
1. Find Ancient City in Deep Dark (16% chest drop)
2. Kill Warden (guaranteed drop)
3. Trade with Foreman (add to config)
```

### Step 2: Find Deep Dark

```java
// In your Foreman commands:
/foreman task locate_deep_dark

// Or programmatically:
BlockPos deepDark = DeepDarkDetector.findNearestDeepDark(level, playerPos, 2000);
```

### Step 3: Build Basic Farm

```
Minimal Design:
┌─────────────┐
│ Spawn Pad   │ (Y=-20, dark room)
│   ↓ ↓ ↓     │
├─────────────┤
│ Catalyst    │ (Y=-30)
│ [SCULK]     │
│             │
│ Stone floor │ (radius 8)
└─────────────┘

Mobs fall 22+ blocks → die → sculk spreads
```

### Step 4: Mine & Profit

```bash
# Foreman command:
/foreman task mine_sculk

# Expected yield:
# - 100 blocks = 100 XP = ~8 levels (0-100)
# - With Mending: infinite tool repair
```

## Foreman Commands

```bash
# Locate Deep Dark
/foreman "Find the nearest deep dark biome and navigate there"

# Build Farm
/foreman "Build a sculk farm with spawning platform and killing chamber"

# Check Safety
/foreman "Check for wardens and ensure safe path"

# Mine XP
/foreman "Mine all sculk blocks within 8 blocks of the catalyst"
```

## Code Snippets

### Check for Sculk Catalyst

```java
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;

// Check if position has catalyst
if (level.getBlockState(pos).is(Blocks.SCULK_CATALYST)) {
    // Found one!
}
```

### Count Sculk Blocks

```java
int sculkCount = 0;
int radius = 8;

for (BlockPos pos : BlockPos.betweenClosed(
    center.offset(-radius, -1, -radius),
    center.offset(radius, 1, radius)
)) {
    if (level.getBlockState(pos).is(Blocks.SCULK)) {
        sculkCount++;
    }
}

// Each block = 1 XP
int totalXP = sculkCount;
```

### Warden Safety Check

```java
List<Warden> wardens = level.getEntitiesOfClass(
    Warden.class,
    new AABB(center).inflate(32)
);

if (!wardens.isEmpty()) {
    // WARNING: Wardens nearby
    // 1. Sneak
    // 2. Avoid vibrations
    // 3. Use wool carpet
    // 4. Retreat if needed
}
```

## Farm Configurations

### Compact (18x18x30)

```
┌─────────────────┐
│   Dark Room     │ ← Spawning (Y=0)
│   18x18x3       │
└────────┬────────┘
         │ 22 block drop
┌────────┴────────┐
│   Catalyst      │ ← Y=-25
│   [SCULK]       │
│                 │
│   Stone Floor   │
└─────────────────┘
```

**Space:** 18x18 surface, 30 blocks tall
**Yield:** 100-200 XP per hour
**Build time:** ~30 minutes

### Efficient (32x32x40)

```
┌─────────────────────┐
│   Spawning Platform │ ← 32x32, multiple levels
│   Water streams     │
└──────────┬──────────┘
           │
┌──────────┴──────────┐
│   Killing Chamber   │
│   Iron Golems       │ ← More XP per kill
└──────────┬──────────┘
           │
┌──────────┴──────────┐
│   Catalyst Grid     │ ← 2x2 catalysts
│   [C][C][C][C]      │
└─────────────────────┘
```

**Space:** 32x32 surface, 40 blocks tall
**Yield:** 500-1000 XP per hour
**Build time:** ~2 hours

## Warden Survival Guide

### DO's
✓ Sneak at all times
✓ Use wool carpet for flooring
✓ Stay 20+ blocks from Wardens
✓ Destroy Sculk Shriekers first
✓ Retreat if detected

### DON'Ts
✗ Run or walk normally
✗ Place/break blocks near Warden
✗ Use projectiles (snowballs, arrows)
✗ Activate Sculk Shriekers
✗ Open chests nearby

## Troubleshooting

**Catalyst not blooming?**
- Check: Mob died within 8 blocks?
- Fix: Move killing chamber closer

**No sculk spreading?**
- Check: Blocks are replaceable (stone, dirt)?
- Fix: Replace non-convertible blocks

**Warden keeps spawning?**
- Check: Nearby Sculk Shriekers?
- Fix: Destroy all shriekers in area

**Low XP yield?**
- Check: Using high-XP mobs?
- Fix: Target Endermen (5 XP) or Blazes (10 XP)

## Next Steps

1. Read full documentation: `SCULK_FARM_AUTOMATION.md`
2. Test in creative world first
3. Implement Foreman actions
4. Add LLM prompts for sculk farming
5. Optimize for your server

## API Reference

```java
// Detection
DeepDarkDetector.isInDeepDark(Level, BlockPos)
DeepDarkDetector.findNearestDeepDark(Level, BlockPos, int)

// Catalyst
SculkCatalystHelper.getCoverageArea(BlockPos)
SculkCatalystHelper.countSculkBlocks(Level, BlockPos)

// Safety
WardenSafetySystem.detectWardens(Level, BlockPos, int)
WardenSafetySystem.isSafeFromWarden(Level, BlockPos)
```

---

**Questions?** See `SCULK_FARM_AUTOMATION.md` for complete documentation
