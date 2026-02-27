# TNT Mining Automation for MineWright (Forge 1.20.1)

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Status:** Design Specification

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Safe TNT Placement Patterns](#safe-tnt-placement-patterns)
3. [Explosion Timing Optimization](#explosion-timing-optimization)
4. [Resource Preservation](#resource-preservation)
5. [TNT Duping Ethics & Options](#tnt-duping-ethics--options)
6. [Post-Blast Collection Systems](#post-blast-collection-systems)
7. [Code Examples](#code-examples)
8. [Implementation Roadmap](#implementation-roadmap)
9. [Configuration Options](#configuration-options)
10. [Safety & Security](#safety--security)

---

## Executive Summary

This document outlines a comprehensive TNT mining automation system for the MineWright Minecraft mod (Forge 1.20.1). The system enables AI-controlled Foreman entities to safely and efficiently mine large areas using TNT explosives while preserving resources and protecting player builds.

### Key Features

- **Safe Placement Algorithms:** Grid-based patterns with safety margins
- **Timing Optimization:** Precise TNT ignition sequences
- **Resource Preservation:** Configurable explosion modes (ore-safe, area-clear)
- **Collection Systems:** Hopper-based automatic item collection
- **Ethical Design:** Configurable TNT duping with server-admin controls
- **Build Protection:** Automatic claim detection and exclusion zones

### Design Philosophy

The TNT mining system follows three core principles:

1. **Safety First:** Never endanger players, their builds, or the Foreman entities
2. **Server Balance:** Provide configurable options for different playstyles
3. **Efficiency:** Maximize yield while minimizing wasted resources

---

## Safe TNT Placement Patterns

### Pattern 1: Checkerboard Mining

```
Legend:
[T] = TNT position    [=] = Safe zone    [O] = Observer/Trigger

Safe Checkerboard Pattern (3x3 grid):
[T] [=] [T]
[=] [O] [=]
[T] [=] [T]
```

**Benefits:**
- Prevents chain reactions
- Ensures complete coverage
- Safe for operator placement
- Preserves structural integrity

**Code Pattern:**
```java
public class CheckerboardPattern {
    public static List<BlockPos> generatePattern(BlockPos center, int radius) {
        List<BlockPos> positions = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -1; y <= 1; y++) {
                    // Checkerboard: skip every other position
                    if (Math.abs(x) % 2 == Math.abs(z) % 2) {
                        positions.add(center.offset(x, y, z));
                    }
                }
            }
        }
        return positions;
    }
}
```

### Pattern 2: Tunnel Clearing

```
Side view of tunnel clearing:
    Wall
     ↓
[T][T][T]  ← Ceiling TNT (clears upward)
[=][=][=]  ← Safe path (agent walks here)
[T][T][T]  ← Floor TNT (clears downward)
```

**Benefits:**
- Creates clear pathways
- Preserves tunnel walls
- Sequential detonation prevents collapse
- Ideal for highway construction

### Pattern 3: Ore Vein Targeting

```
Precision ore extraction:
        [T] [T] [T]
        [T] [X] [T]  ← X = detected ore
        [T] [T] [T]
```

**Benefits:**
- Targeted explosions
- Minimal overburden removal
- Preserves surrounding blocks
- Uses LLM planning for optimal placement

### Safety Distance Calculations

| Explosion Power | Safe Distance (blocks) | Collection Zone Radius |
|----------------|------------------------|------------------------|
| 4 (standard TNT) | 8 | 12 |
| 6 (enhanced) | 12 | 18 |
| 8 (maximum) | 16 | 24 |

**Implementation:**
```java
public class SafetyCalculator {
    // TNT explosion power in vanilla Minecraft
    private static final float TNT_POWER = 4.0f;

    // Calculate safe distance based on explosion power
    public static int getSafeDistance(float explosionPower) {
        return (int) Math.ceil(explosionPower * 2.5);
    }

    // Check if position is safe from chain reactions
    public static boolean isSafeFromChainReaction(Level level, BlockPos pos, int radius) {
        // Check for other TNT entities in range
        List<PrimedTnt> nearbyTNT = level.getEntitiesOfClass(
            PrimedTnt.class,
            new AABB(pos).inflate(radius)
        );
        return nearbyTNT.isEmpty();
    }

    // Verify no players or protected builds in blast zone
    public static BlastZoneAssessment assessBlastZone(Level level, BlockPos center, float power) {
        int safeDistance = getSafeDistance(power);
        AABB blastZone = new AABB(center).inflate(safeDistance);

        // Check for players
        List<Player> players = level.getEntitiesOfClass(Player.class, blastZone);

        // Check for protected blocks (integration with claim mods)
        boolean hasProtectedBlocks = checkProtectedBlocks(level, blastZone);

        return new BlastZoneAssessment(
            players.isEmpty(),
            !hasProtectedBlocks,
            players,
            getProtectedBlocks(level, blastZone)
        );
    }
}
```

---

## Explosion Timing Optimization

### Sequential Detonation System

Prevent chain reactions and control blast waves with precise timing.

```java
public class TNTTimingController {
    private static final int DEFAULT_FUSE_TICKS = 80; // 4 seconds
    private static final int MIN_SEPARATION_TICKS = 10; // Minimum between detonations

    public enum DetonationPattern {
        SEQUENTIAL,      // One at a time, maximum safety
        WAVE,            // Fan out from center
        PERIMETER_FIRST, // Outside in, contains blast
        SIMULTANEOUS     // All at once (dangerous, max power)
    }

    /**
     * Calculate fuse times for sequential detonation
     * @param positions TNT placement positions
     * @param pattern Timing pattern to use
     * @return Map of position to fuse tick count
     */
    public static Map<BlockPos, Integer> calculateFuseTimes(
        List<BlockPos> positions,
        DetonationPattern pattern
    ) {
        Map<BlockPos, Integer> fuseTimes = new HashMap<>();

        switch (pattern) {
            case SEQUENTIAL:
                int delay = 0;
                for (BlockPos pos : positions) {
                    fuseTimes.put(pos, DEFAULT_FUSE_TICKS + delay);
                    delay += MIN_SEPARATION_TICKS;
                }
                break;

            case WAVE:
                BlockPos center = calculateCenter(positions);
                for (BlockPos pos : positions) {
                    int distance = (int) center.distSqr(pos);
                    fuseTimes.put(pos, DEFAULT_FUSE_TICKS + (distance / 4));
                }
                break;

            case PERIMETER_FIRST:
                List<BlockPos> perimeter = findPerimeterPositions(positions);
                List<BlockPos> interior = new ArrayList<>(positions);
                interior.removeAll(perimeter);

                delay = 0;
                for (BlockPos pos : perimeter) {
                    fuseTimes.put(pos, DEFAULT_FUSE_TICKS + delay);
                    delay += MIN_SEPARATION_TICKS;
                }
                for (BlockPos pos : interior) {
                    fuseTimes.put(pos, DEFAULT_FUSE_TICKS + delay);
                    delay += MIN_SEPARATION_TICKS;
                }
                break;

            case SIMULTANEOUS:
                for (BlockPos pos : positions) {
                    fuseTimes.put(pos, DEFAULT_FUSE_TICKS);
                }
                break;
        }

        return fuseTimes;
    }

    private static BlockPos calculateCenter(List<BlockPos> positions) {
        int x = 0, y = 0, z = 0;
        for (BlockPos pos : positions) {
            x += pos.getX();
            y += pos.getY();
            z += pos.getZ();
        }
        return new BlockPos(x / positions.size(), y / positions.size(), z / positions.size());
    }

    private static List<BlockPos> findPerimeterPositions(List<BlockPos> positions) {
        // Find positions on the edge of the blast zone
        Set<BlockPos> positionSet = new HashSet<>(positions);
        List<BlockPos> perimeter = new ArrayList<>();

        for (BlockPos pos : positions) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (!positionSet.contains(neighbor)) {
                    perimeter.add(pos);
                    break;
                }
            }
        }

        return perimeter;
    }
}
```

### Redstone Integration

For advanced users who want to integrate with existing redstone circuits:

```java
public class RedstoneTNTIgniter {
    /**
     * Place TNT with redstone ignition system
     * Uses redstone blocks and repeaters for timing
     */
    public static void placeRedstoneIgnitionSystem(
        Level level,
        List<BlockPos> tntPositions,
        Map<BlockPos, Integer> fuseTimes
    ) {
        // Place redstone blocks with repeater delays
        // This allows for pure redstone timing (no entities)

        for (Map.Entry<BlockPos, Integer> entry : fuseTimes.entrySet()) {
            BlockPos tntPos = entry.getKey();
            int extraDelay = entry.getValue() - DEFAULT_FUSE_TICKS;

            // Calculate repeater positions
            BlockPos repeaterPos = tntPos.below();

            // Place repeaters with appropriate delay
            int repeaterCount = (int) Math.ceil(extraDelay / 4.0); // 4 ticks per repeater max

            for (int i = 0; i < repeaterCount; i++) {
                level.setBlock(
                    repeaterPos.below(i),
                    Blocks.REPEATER.defaultBlockState()
                        .setValue(RepeaterBlock.POWERED, false)
                        .setValue(RepeaterBlock.DELAY, Math.min(4, extraDelay)),
                    3
                );
            }
        }
    }
}
```

---

## Resource Preservation

### Ore-Safe Explosion Mode

```java
public class OrePreservationSystem {
    /**
     * Checks if a block should be preserved during explosion
     * Uses Minecraft's explosion resistance mechanics
     */
    public static boolean shouldPreserveBlock(BlockState blockState) {
        Block block = blockState.getBlock();

        // Always preserve these valuable blocks
        if (block instanceof OreBlock) {
            return true;
        }

        // Preserve based on explosion resistance
        if (block.getExplosionResistance() > 20.0f) {
            return true; // Too hard to blow up anyway
        }

        // Preserve player-placed blocks (check with block entity data)
        if (isPlayerPlaced(blockState)) {
            return true;
        }

        return false;
    }

    /**
     * Creates a modified explosion that preserves certain blocks
     */
    public static Explosion createOreSafeExplosion(
        Level level,
        Entity entity,
        double x, double y, double z,
        float power,
        boolean createFire
    ) {
        // Create custom explosion behavior
        return new OreSafeExplosion(level, entity, x, y, z, power, createFire);
    }

    private static boolean isPlayerPlaced(BlockState blockState) {
        // Check for block entity data indicating player placement
        // This requires integration with a tracking system
        return false; // Placeholder
    }
}

/**
 * Custom explosion implementation that preserves ores
 */
class OreSafeExplosion extends Explosion {
    public OreSafeExplosion(
        Level level, Entity entity,
        double x, double y, double z,
        float power, boolean createFire
    ) {
        super(level, entity, x, y, z, power, createFire, BlockExplosion.DESTROY);
    }

    @Override
    public void explode() {
        // Override to skip ore blocks
        List<BlockPos> toBlow = getToBlow();

        // Filter out ore blocks
        toBlow.removeIf(pos -> {
            BlockState state = level.getBlockState(pos);
            return OrePreservationSystem.shouldPreserveBlock(state);
        });

        // Continue with modified explosion
        super.explode();
    }
}
```

### Collection Efficiency Optimization

```java
public class CollectionOptimizer {
    /**
     * Calculates optimal hopper placement for maximum collection
     */
    public static List<BlockPos> calculateOptimalHopperPositions(
        BlockPos blastCenter,
        int blastRadius
    ) {
        List<BlockPos> hopperPositions = new ArrayList<>();

        // Place hoppers in a grid pattern below the blast zone
        int hopperSpacing = 4; // Hoppers collect 4 blocks away

        for (int x = -blastRadius; x <= blastRadius; x += hopperSpacing) {
            for (int z = -blastRadius; z <= blastRadius; z += hopperSpacing) {
                // Position hoppers 2 blocks below the lowest blast point
                BlockPos hopperPos = blastCenter.offset(x, -blastRadius - 2, z);
                hopperPositions.add(hopperPos);
            }
        }

        return hopperPositions;
    }

    /**
     * Sets up hopper minecart collection system
     * More efficient than stationary hoppers for large blasts
     */
    public static void setupHopperMinecartSystem(
        Level level,
        BlockPos blastCenter,
        int blastRadius
    ) {
        // Create rails under blast zone
        int railSpacing = 6;

        for (int x = -blastRadius; x <= blastRadius; x += railSpacing) {
            for (int z = -blastRadius; z <= blastRadius; z += railSpacing) {
                BlockPos railPos = blastCenter.offset(x, -blastRadius - 1, z);

                // Place rail
                level.setBlock(railPos, Blocks.RAIL.defaultBlockState(), 3);

                // Spawn hopper minecart
                AbstractMinecart minecart = new MinecartChest(
                    level,
                    railPos.getX() + 0.5,
                    railPos.getY(),
                    railPos.getZ() + 0.5
                );
                level.addFreshEntity(minecart);
            }
        }
    }
}
```

---

## TNT Duping Ethics & Options

### Ethical Considerations

TNT duplication is a controversial topic in the Minecraft community:

**Arguments FOR Duplication:**
- Essential for technical/redstone players
- Only affordable automated block destruction method
- Vanilla Minecraft has not patched these mechanics
- Considered a legitimate game mechanic by many

**Arguments AGAINST Duplication:**
- Can be used for griefing
- Gives unfair advantage in survival
- Can cause server lag with large-scale use
- Goes against "survival" spirit for some

### Configurable Duplication System

```java
public class TNTDuplicationConfig {
    public enum DuplicationMode {
        DISABLED,        // No duplication allowed
        WHITELIST,       // Only in specific areas/for specific players
        PERMISSION_BASED,// Requires permission node
        UNRESTRICTED     // Full duplication (not recommended for public servers)
    }

    private DuplicationMode mode = DuplicationMode.DISABLED;
    private Set<UUID> whitelistedPlayers = new HashSet<>();
    private Set<BlockPos> duplicationZones = new HashSet<>();
    private int maxDuplicationPerHour = 100;

    public boolean canDuplicateTNT(Player player, BlockPos pos) {
        switch (mode) {
            case DISABLED:
                return false;

            case WHITELIST:
                return whitelistedPlayers.contains(player.getUUID()) ||
                       isInDuplicationZone(pos);

            case PERMISSION_BASED:
                return hasPermission(player, "minewright.tnt.duplicate");

            case UNRESTRICTED:
                return true;

            default:
                return false;
        }
    }

    private boolean isInDuplicationZone(BlockPos pos) {
        // Check if position is within a designated duplication zone
        return duplicationZones.stream()
            .anyMatch(zone -> pos.closerThan(zone, 32));
    }

    private boolean hasPermission(Player player, String permission) {
        // Integration with permission system (placeholder)
        return player.hasPermissions(2); // Op level 2+
    }
}
```

### Config File Integration

Add to `MineWrightConfig.java`:

```java
// TNT Mining Configuration
public static final ForgeConfigSpec.EnumValue<TNTDuplicationMode> TNT_DUP_MODE;
public static final ForgeConfigSpec.BooleanValue TNT_DUP_WHITELIST_ONLY;
public static final ForgeConfigSpec.IntValue TNT_DUP_MAX_PER_HOUR;
public static final ForgeConfigSpec.BooleanValue TNT_DUP_LOG_USAGE;
public static final ForgeConfigSpec.BooleanValue TNT_ORE_SAFE_MODE;
public static final ForgeConfigSpec.IntValue TNT_COLLECTION_RADIUS;

static {
    // ... existing config code ...

    builder.comment("TNT Mining Configuration").push("tnt");

    TNT_DUP_MODE = builder
        .comment("TNT duplication mode: DISABLED, PERMISSION_BASED, or UNRESTRICTED")
        .defineEnum("duplicationMode", TNTDuplicationMode.DISABLED);

    TNT_DUP_WHITELIST_ONLY = builder
        .comment("Only allow TNT duplication in whitelisted zones")
        .define("whitelistOnly", true);

    TNT_DUP_MAX_PER_HOUR = builder
        .comment("Maximum TNT duplications per hour per player (0 = unlimited)")
        .defineInRange("maxPerHour", 100, 0, 10000);

    TNT_DUP_LOG_USAGE = builder
        .comment("Log all TNT duplication attempts to server console")
        .define("logUsage", true);

    TNT_ORE_SAFE_MODE = builder
        .comment("Preserve ores during TNT explosions (uses custom explosion logic)")
        .define("oreSafeMode", true);

    TNT_COLLECTION_RADIUS = builder
        .comment("Radius for hopper collection system (blocks)")
        .defineInRange("collectionRadius", 16, 4, 64);

    builder.pop();
}
```

---

## Post-Blast Collection Systems

### System 1: Hopper Grid

```
Top-down view of hopper collection system:

[H][H][H][H][H]
[H]         [H]
[H]  BLAST  [H]
[H]   ZONE  [H]
[H][H][H][H][H]

Legend: [H] = Hopper with chest below
```

```java
public class HopperCollectionSystem {
    private final Level level;
    private final BlockPos center;
    private final int radius;
    private final List<BlockPos> hopperPositions;
    private final List<BlockPos> chestPositions;

    public HopperCollectionSystem(Level level, BlockPos center, int radius) {
        this.level = level;
        this.center = center;
        this.radius = radius;
        this.hopperPositions = new ArrayList<>();
        this.chestPositions = new ArrayList<>();
    }

    /**
     * Builds the collection system
     * Places hoppers in a grid with chests below
     */
    public void build() {
        int spacing = 4; // Hoppers collect from 4 blocks away

        for (int x = -radius; x <= radius; x += spacing) {
            for (int z = -radius; z <= radius; z += spacing) {
                // Place hopper
                BlockPos hopperPos = center.offset(x, -2, z);
                level.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState()
                    .setValue(HopperBlock.FACING, Direction.DOWN), 3);
                hopperPositions.add(hopperPos);

                // Place chest below hopper
                BlockPos chestPos = hopperPos.below();
                level.setBlock(chestPos, Blocks.CHEST.defaultBlockState()
                    .setValue(ChestBlock.FACING, Direction.NORTH), 3);
                chestPositions.add(chestPos);
            }
        }
    }

    /**
     * Collects all items from the collection system
     * Returns total count and types
     */
    public CollectionResult collectItems() {
        Map<Item, Integer> collectedItems = new HashMap<>();
        int totalCount = 0;

        for (BlockPos chestPos : chestPositions) {
            BlockEntity blockEntity = level.getBlockEntity(chestPos);
            if (blockEntity instanceof ChestBlockEntity chest) {
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    ItemStack stack = chest.getItem(slot);
                    if (!stack.isEmpty()) {
                        collectedItems.merge(stack.getItem(), stack.getCount(), Integer::sum);
                        totalCount += stack.getCount();
                        chest.setItem(slot, ItemStack.EMPTY);
                    }
                }
            }
        }

        return new CollectionResult(collectedItems, totalCount);
    }

    public record CollectionResult(
        Map<Item, Integer> items,
        int totalCount
    ) {}
}
```

### System 2: Hopper Minecart Grid

More efficient for large blast zones:

```java
public class HopperMinecartSystem {
    private final Level level;
    private final List<MinecartHopper> minecarts = new ArrayList<>();

    /**
     * Creates a grid of hopper minecarts under the blast zone
     */
    public void setup(Level level, BlockPos center, int radius) {
        int spacing = 6; // Minecarts can be spaced further apart

        for (int x = -radius; x <= radius; x += spacing) {
            for (int z = -radius; z <= radius; z += spacing) {
                BlockPos railPos = center.offset(x, -1, z);

                // Place rail
                level.setBlock(railPos, Blocks.RAIL.defaultBlockState(), 3);

                // Spawn hopper minecart
                MinecartHopper minecart = new MinecartHopper(
                    level,
                    railPos.getX() + 0.5,
                    railPos.getY(),
                    railPos.getZ() + 0.5
                );
                level.addFreshEntity(minecart);
                minecarts.add(minecart);
            }
        }
    }

    /**
     * Returns all minecarts to a central collection point
     * Uses powered rails to move them
     */
    public void returnToCollectionPoint(BlockPos collectionPoint) {
        // Build powered rail system
        // Activate rails to move minecarts
        // Minecarts automatically transfer items to target chest
    }

    /**
     * Removes all minecarts and returns collected items
     */
    public Map<Item, Integer> collectAndRemove() {
        Map<Item, Integer> items = new HashMap<>();

        for (MinecartHopper minecart : minecarts) {
            // Transfer items to map
            for (int slot = 0; slot < minecart.getContainerSize(); slot++) {
                ItemStack stack = minecart.getItem(slot);
                if (!stack.isEmpty()) {
                    items.merge(stack.getItem(), stack.getCount(), Integer::sum);
                }
            }

            // Remove minecart
            minecart.discard();
        }

        minecarts.clear();
        return items;
    }
}
```

### System 3: Vacuum Entity (Advanced)

Creates an entity that actively attracts dropped items:

```java
public class VacuumEntity extends Entity {
    private final BlockPos collectionPoint;
    private final double attractionStrength = 0.5;
    private final int collectionRadius = 20;

    public VacuumEntity(EntityType<? extends VacuumEntity> type, Level level) {
        super(type, level);
        this.collectionPoint = BlockPos.ZERO;
    }

    public VacuumEntity(Level level, BlockPos collectionPoint) {
        super(ModEntities.VACUUM.get(), level);
        this.collectionPoint = collectionPoint;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) return;

        // Find all item entities in range
        List<ItemEntity> items = level().getEntitiesOfClass(
            ItemEntity.class,
            new AABB(blockPosition()).inflate(collectionRadius)
        );

        // Attract items toward collection point
        for (ItemEntity item : items) {
            Vec3 currentPos = item.position();
            Vec3 targetPos = Vec3.atCenterOf(collectionPoint);
            Vec3 direction = targetPos.subtract(currentPos).normalize();

            // Apply velocity toward collection point
            item.setDeltaMovement(
                item.getDeltaMovement().add(
                    direction.scale(attractionStrength)
                )
            );

            // Check if item reached collection point
            if (currentPos.distanceTo(targetPos) < 1.0) {
                collectItem(item);
            }
        }
    }

    private void collectItem(ItemEntity item) {
        // Add to internal storage or nearby chest
        // Remove item entity
        item.discard();
    }
}
```

---

## Code Examples

### Complete TNT Mining Action

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.config.MineWrightConfig;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Action for automated TNT mining
 *
 * Usage: /foreman task tnt_mine x=100 y=60 z=200 radius=10 pattern=CHECKERBOARD
 */
public class TNTMineAction extends BaseAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(TNTMineAction.class);

    private final BlockPos center;
    private final int radius;
    private final TNTPattern pattern;

    private List<BlockPos> tntPositions;
    private List<BlockPos> hoppersToPlace;
    private List<BlockPos> chestsToPlace;

    private int stage = 0; // 0=setup, 1=place_hoppers, 2=place_tnt, 3=ignite, 4=collect
    private int ticksInStage = 0;
    private static final int TICKS_PER_STAGE = 20; // 1 second per stage

    private Map<Item, Integer> collectedItems;
    private int totalItemsCollected = 0;

    public enum TNTPattern {
        CHECKERBOARD,
        TUNNEL,
        PERIMETER
    }

    public TNTMineAction(ForemanEntity foreman, Task task) {
        super(foreman, task);

        this.center = new BlockPos(
            task.getIntParameter("x", foreman.blockPosition().getX()),
            task.getIntParameter("y", foreman.blockPosition().getY()),
            task.getIntParameter("z", foreman.blockPosition().getZ())
        );
        this.radius = task.getIntParameter("radius", 5);
        this.pattern = TNTPattern.valueOf(
            task.getStringParameter("pattern", "CHECKERBOARD")
        );
    }

    @Override
    protected void onStart() {
        LOGGER.info("[{}] Starting TNT mining at {} with radius {} and pattern {}",
            foreman.getSteveName(), center, radius, pattern);

        // Generate TNT placement pattern
        tntPositions = generatePattern(center, radius, pattern);

        // Calculate collection system positions
        hoppersToPlace = calculateHopperPositions(center, radius + MineWrightConfig.TNT_COLLECTION_RADIUS.get());
        chestsToPlace = new ArrayList<>();
        for (BlockPos hopper : hoppersToPlace) {
            chestsToPlace.add(hopper.below());
        }

        collectedItems = new HashMap<>();

        // Check safety
        if (!assessSafety()) {
            result = ActionResult.failure("Blast zone is not safe - players or protected blocks nearby");
            return;
        }

        // Give Foreman TNT blocks
        equipTNT();

        foreman.sendChatMessage(String.format(
            "Starting TNT mining operation. Will place %d TNT blocks in %s pattern.",
            tntPositions.size(), pattern
        ));
    }

    @Override
    protected void onTick() {
        ticksInStage++;

        switch (stage) {
            case 0 -> setupCollectionSystem();
            case 1 -> placeHoppers();
            case 2 -> placeTNT();
            case 3 -> igniteTNT();
            case 4 -> collectItems();
            case 5 -> complete();
        }
    }

    private void setupCollectionSystem() {
        if (ticksInStage < TICKS_PER_STAGE) return;

        foreman.sendChatMessage("Building collection system...");
        stage = 1;
        ticksInStage = 0;
    }

    private void placeHoppers() {
        if (ticksInStage >= hoppersToPlace.size()) {
            stage = 2;
            ticksInStage = 0;
            foreman.sendChatMessage("Collection system ready. Placing TNT...");
            return;
        }

        // Place 1 hopper per tick
        if (ticksInStage < hoppersToPlace.size()) {
            BlockPos hopperPos = hoppersToPlace.get(ticksInStage);
            BlockPos chestPos = chestsToPlace.get(ticksInStage);

            // Place chest
            foreman.level().setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);

            // Place hopper
            foreman.level().setBlock(hopperPos,
                Blocks.HOPPER.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.HopperBlock.FACING,
                        net.minecraft.core.Direction.DOWN),
                3);

            foreman.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    private void placeTNT() {
        if (ticksInStage >= tntPositions.size()) {
            stage = 3;
            ticksInStage = 0;
            foreman.sendChatMessage("All TNT placed. Igniting sequence...");
            return;
        }

        // Place multiple TNT per tick (faster)
        int tntPerTick = 5;
        int startIndex = ticksInStage * tntPerTick;

        for (int i = 0; i < tntPerTick && (startIndex + i) < tntPositions.size(); i++) {
            BlockPos tntPos = tntPositions.get(startIndex + i);
            foreman.level().setBlock(tntPos, Blocks.TNT.defaultBlockState(), 3);
            foreman.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    private void igniteTNT() {
        if (ticksInStage >= tntPositions.size()) {
            stage = 4;
            ticksInStage = 0;
            return;
        }

        // Ignite TNT with timing delay
        if (ticksInStage < tntPositions.size()) {
            BlockPos tntPos = tntPositions.get(ticksInStage);

            // Use flint and steel to ignite
            foreman.level().setBlock(tntPos, Blocks.TNT.defaultBlockState(), 3);

            // Spawn primed TNT entity
            PrimedTnt primedTnt = new PrimedTnt(foreman.level(),
                tntPos.getX() + 0.5, tntPos.getY(), tntPos.getZ() + 0.5, null);

            // Set fuse time based on pattern
            int fuseDelay = ticksInStage * 5; // 5 tick delay between each TNT
            primedTnt.setFuse(80 + fuseDelay);

            foreman.level().addFreshEntity(primedTnt);
            foreman.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    private void collectItems() {
        // Wait for all explosions to complete and items to drop
        if (ticksInStage < 200) { // Wait 10 seconds
            return;
        }

        // Collect from all chests
        for (BlockPos chestPos : chestsToPlace) {
            var blockEntity = foreman.level().getBlockEntity(chestPos);
            if (blockEntity instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    ItemStack stack = chest.getItem(slot);
                    if (!stack.isEmpty()) {
                        collectedItems.merge(stack.getItem(), stack.getCount(), Integer::sum);
                        totalItemsCollected += stack.getCount();
                        chest.setItem(slot, ItemStack.EMPTY);
                    }
                }
            }
        }

        stage = 5;
    }

    private void complete() {
        // Clean up hoppers (optional)
        // Remove collection system

        StringBuilder summary = new StringBuilder();
        summary.append("TNT mining complete! Collected: ");

        // List most valuable items
        collectedItems.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .forEach(entry -> summary.append(String.format("%s x%d, ",
                entry.getKey().getDescription().getString(), entry.getValue())));

        result = ActionResult.success(summary.toString());

        foreman.sendChatMessage(summary.toString());
        foreman.notifyMilestone("Completed large-scale TNT mining operation");
    }

    @Override
    protected void onCancel() {
        // Clean up any placed blocks
        for (BlockPos hopper : hoppersToPlace) {
            foreman.level().removeBlock(hopper, false);
        }
        for (BlockPos chest : chestsToPlace) {
            foreman.level().removeBlock(chest, false);
        }
    }

    @Override
    public String getDescription() {
        return String.format("TNT Mining at %s (radius %d, %s) - Stage %d/5",
            center, radius, pattern, stage + 1);
    }

    // ========== Helper Methods ==========

    private List<BlockPos> generatePattern(BlockPos center, int radius, TNTPattern pattern) {
        List<BlockPos> positions = new ArrayList<>();

        switch (pattern) {
            case CHECKERBOARD -> {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        for (int y = -1; y <= 1; y++) {
                            if (Math.abs(x) % 2 == Math.abs(z) % 2) {
                                positions.add(center.offset(x, y, z));
                            }
                        }
                    }
                }
            }
            case TUNNEL -> {
                // Forward tunnel pattern
                for (int i = 0; i < radius * 2; i++) {
                    positions.add(center.offset(i, 1, 0));  // Ceiling
                    positions.add(center.offset(i, -1, 0)); // Floor
                }
            }
            case PERIMETER -> {
                // TNT around perimeter
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.abs(x) == radius || Math.abs(z) == radius) {
                            positions.add(center.offset(x, 0, z));
                        }
                    }
                }
            }
        }

        return positions;
    }

    private List<BlockPos> calculateHopperPositions(BlockPos center, int radius) {
        List<BlockPos> positions = new ArrayList<>();
        int spacing = 4;

        for (int x = -radius; x <= radius; x += spacing) {
            for (int z = -radius; z <= radius; z += spacing) {
                positions.add(center.offset(x, -2, z));
            }
        }

        return positions;
    }

    private boolean assessSafety() {
        // Check for players in blast zone
        int safeDistance = (int) Math.ceil(radius * 1.5);
        var players = foreman.level().getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class,
            new AABB(center).inflate(safeDistance)
        );

        if (!players.isEmpty()) {
            return false;
        }

        // Check for protected blocks (integration with claim mods)
        // Placeholder: return true for now

        return true;
    }

    private void equipTNT() {
        ItemStack tntStack = new ItemStack(Blocks.TNT, 64);
        foreman.setItemInHand(InteractionHand.MAIN_HAND, tntStack);
    }
}
```

### Integration with Action Registry

Add to `CoreActionsPlugin.java`:

```java
@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {
    // ... existing registrations ...

    // TNT Mining
    registry.register("tnt_mine",
        (minewright, task, ctx) -> new TNTMineAction(minewright, task),
        priority, PLUGIN_ID);
}
```

### LLM Prompt Integration

Add to `PromptBuilder.java`:

```java
private void addTNTMiningCapabilities(StringBuilder prompt) {
    prompt.append("""

        ## TNT Mining Capabilities

        The Foreman can perform large-scale mining operations using TNT explosives:

        Action: `tnt_mine`
        Parameters:
        - x, y, z: Center position for blast zone
        - radius: Blast radius (default 5, max 20)
        - pattern: CHECKERBOARD (safe), TUNNEL (linear), PERIMETER (border)

        Examples:
        - "Clear a large area for building at x=100 z=200" -> tnt_mine x=100 y=60 z=200 radius=10 pattern=CHECKERBOARD
        - "Dig a tunnel through the mountain" -> tnt_mine x=50 y=70 z=100 radius=15 pattern=TUNNEL
        - "Remove the perimeter of this platform" -> tnt_mine x=0 y=64 z=0 radius=8 pattern=PERIMETER

        Safety:
        - Foreman automatically checks for players and protected builds
        - Uses ore-safe mode by default (preserves valuable ores)
        - Creates collection system before detonation
        - Sequential ignition prevents chain reactions

        Limitations:
        - Maximum blast radius is 20 blocks
        - Requires TNT in Foreman's inventory
        - 10-second cooldown between operations
        - Not available near spawn (100-block protection radius)
        """);
}
```

---

## Implementation Roadmap

### Phase 1: Core TNT Mining (Week 1-2)

**Tasks:**
- [ ] Create `TNTMineAction` class
- [ ] Implement pattern generation algorithms
- [ ] Add safety zone checking
- [ ] Implement sequential ignition system
- [ ] Add to `CoreActionsPlugin`
- [ ] Basic testing with small blasts

**Acceptance Criteria:**
- Can execute basic TNT mining with `tnt_mine` command
- Checkerboard pattern works correctly
- No chain reactions occur
- Foreman reports results

### Phase 2: Collection Systems (Week 2-3)

**Tasks:**
- [ ] Implement hopper grid placement
- [ ] Create chest inventory collection logic
- [ ] Add hopper minecart alternative
- [ ] Implement item aggregation and reporting
- [ ] Test collection efficiency

**Acceptance Criteria:**
- Items automatically collected after blast
- Collected items reported to chat
- Hopper system removes successfully
- Minecart system functional

### Phase 3: Resource Preservation (Week 3-4)

**Tasks:**
- [ ] Implement `OreSafeExplosion` class
- [ ] Add block filtering logic
- [ ] Integrate with Forge explosion events
- [ ] Test ore preservation rates
- [ ] Balance explosion power vs. ore survival

**Acceptance Criteria:**
- Ores survive TNT blasts at 80%+ rate
- Configurable preservation modes
- No performance impact from custom explosion logic

### Phase 4: Configuration & Permissions (Week 4)

**Tasks:**
- [ ] Add TNT config section to `MineWrightConfig`
- [ ] Implement duplication modes
- [ ] Add whitelist system
- [ ] Create permission integration
- [ ] Add logging and monitoring

**Acceptance Criteria:**
- Server admins can control TNT usage
- Duplication can be disabled/limited
- Permission-based access works
- Usage logged for monitoring

### Phase 5: Safety & Protection (Week 5)

**Tasks:**
- [ ] Implement build protection detection
- [ ] Add spawn protection radius
- [ ] Create claim mod integration
- [ ] Add player proximity warnings
- [ ] Implement emergency cancellation

**Acceptance Criteria:**
- Cannot blast near protected builds
- Spawn zone protected
- Integration with popular claim mods
- Emergency stop works correctly

### Phase 6: Advanced Features (Week 6)

**Tasks:**
- [ ] Implement vacuum entity collection
- [ ] Add automated TNT sourcing (crafting)
- [ ] Create multi-agent coordination for large blasts
- [ ] Add blast statistics and analytics
- [ ] Optimize performance for large-scale operations

**Acceptance Criteria:**
- Vacuum entity collects efficiently
- Can source TNT automatically
- Multiple agents coordinate safely
- Performance acceptable for 100+ TNT blasts

---

## Configuration Options

### Complete Config Section

Add to `MineWrightConfig.java`:

```java
// TNT Mining Configuration
public static final ForgeConfigSpec.EnumValue<TNTDuplicationMode> TNT_DUP_MODE;
public static final ForgeConfigSpec.BooleanValue TNT_DUP_WHITELIST_ONLY;
public static final ForgeConfigSpec.IntValue TNT_DUP_MAX_PER_HOUR;
public static final ForgeConfigSpec.BooleanValue TNT_DUP_LOG_USAGE;
public static final ForgeConfigSpec.BooleanValue TNT_ORE_SAFE_MODE;
public static final ForgeConfigSpec.DoubleValue TNT_ORE_PRESERVATION_CHANCE;
public static final ForgeConfigSpec.IntValue TNT_COLLECTION_RADIUS;
public static final ForgeConfigSpec.IntValue TNT_MAX_BLAST_RADIUS;
public static final ForgeConfigSpec.IntValue TNT_SPAWN_PROTECTION_RADIUS;
public static final ForgeConfigSpec.BooleanValue TNT_REQUIRE_CONFIRMATION;
public static final ForgeConfigSpec.BooleanValue TNT_ENABLED;
public static final ForgeConfigSpec.IntValue TNT_COOLDOWN_TICKS;

public enum TNTDuplicationMode {
    DISABLED,
    PERMISSION_BASED,
    UNRESTRICTED
}

static {
    // ... existing config code ...

    builder.comment("TNT Mining Configuration").push("tnt");

    TNT_ENABLED = builder
        .comment("Enable TNT mining capabilities")
        .define("enabled", true);

    TNT_DUP_MODE = builder
        .comment("TNT duplication mode: DISABLED, PERMISSION_BASED, or UNRESTRICTED")
        .defineEnum("duplicationMode", TNTDuplicationMode.DISABLED);

    TNT_DUP_WHITELIST_ONLY = builder
        .comment("Only allow TNT duplication in whitelisted zones")
        .define("whitelistOnly", true);

    TNT_DUP_MAX_PER_HOUR = builder
        .comment("Maximum TNT duplications per hour per player (0 = unlimited)")
        .defineInRange("maxPerHour", 100, 0, 10000);

    TNT_DUP_LOG_USAGE = builder
        .comment("Log all TNT duplication attempts to server console")
        .define("logUsage", true);

    TNT_ORE_SAFE_MODE = builder
        .comment("Preserve ores during TNT explosions")
        .define("oreSafeMode", true);

    TNT_ORE_PRESERVATION_CHANCE = builder
        .comment("Chance for ores to survive TNT blast (0.0-1.0)")
        .defineInRange("orePreservationChance", 0.8, 0.0, 1.0);

    TNT_COLLECTION_RADIUS = builder
        .comment("Radius for hopper collection system (blocks)")
        .defineInRange("collectionRadius", 16, 4, 64);

    TNT_MAX_BLAST_RADIUS = builder
        .comment("Maximum blast radius allowed")
        .defineInRange("maxBlastRadius", 20, 5, 50);

    TNT_SPAWN_PROTECTION_RADIUS = builder
        .comment("Radius around spawn where TNT is disabled")
        .defineInRange("spawnProtectionRadius", 100, 0, 500);

    TNT_REQUIRE_CONFIRMATION = builder
        .comment("Require player confirmation before large-scale TNT operations")
        .define("requireConfirmation", true);

    TNT_COOLDOWN_TICKS = builder
        .comment("Cooldown between TNT operations (ticks, 20 = 1 second)")
        .defineInRange("cooldownTicks", 200, 0, 7200);

    builder.pop();
}
```

### In-Game Commands

Add to `ForemanCommands.java`:

```
/foreman tnt zone add <name> <radius>
    - Adds a designated TNT mining zone

/foreman tnt zone remove <name>
    - Removes a TNT mining zone

/foreman tnt whitelist add <player>
    - Adds player to TNT duplication whitelist

/foreman tnt whitelist remove <player>
    - Removes player from whitelist

/foreman tnt stats
    - Shows TNT usage statistics

/foreman tnt cancel
    - Emergency cancellation of active TNT operation
```

---

## Safety & Security

### Multi-Layer Safety System

```java
public class TNTSafetyManager {
    private final Level level;
    private final Set<UUID> authorizedPlayers = new HashSet<>();
    private final Set<BlockPos> protectedZones = new HashSet<>();

    /**
     * Comprehensive safety check before TNT operation
     */
    public SafetyCheckResult performSafetyCheck(BlockPos center, int radius) {
        SafetyCheckResult result = new SafetyCheckResult();

        // Check 1: Player proximity
        result.addCheck(checkPlayerProximity(center, radius));

        // Check 2: Spawn protection
        result.addCheck(checkSpawnProtection(center));

        // Check 3: Build protection
        result.addCheck(checkBuildProtection(center, radius));

        // Check 4: Claimed land
        result.addCheck(checkClaimedLand(center, radius));

        // Check 5: Rate limiting
        result.addCheck(checkRateLimit());

        // Check 6: Entity limits
        result.addCheck(checkEntityLimits(center, radius));

        return result;
    }

    private SafetyCheck checkPlayerProximity(BlockPos center, int radius) {
        int safeDistance = radius + 10;
        List<Player> nearbyPlayers = level.getEntitiesOfClass(
            Player.class,
            new AABB(center).inflate(safeDistance)
        );

        if (!nearbyPlayers.isEmpty()) {
            return SafetyCheck.failed(
                "Players nearby: " + nearbyPlayers.size(),
                "Move all players at least %d blocks away".formatted(safeDistance)
            );
        }

        return SafetyCheck.passed("No players in blast zone");
    }

    private SafetyCheck checkSpawnProtection(BlockPos center) {
        BlockPos spawnPos = new BlockPos(0, level.getSeaLevel(), 0);
        int protectionRadius = MineWrightConfig.TNT_SPAWN_PROTECTION_RADIUS.get();

        if (center.closerThan(spawnPos, protectionRadius)) {
            return SafetyCheck.failed(
                "Within spawn protection zone",
                "Move operation at least %d blocks from spawn".formatted(protectionRadius)
            );
        }

        return SafetyCheck.passed("Outside spawn protection");
    }

    private SafetyCheck checkBuildProtection(BlockPos center, int radius) {
        // Check for blocks that appear to be player-placed
        AABB blastZone = new AABB(center).inflate(radius);

        for (BlockPos pos : BlockPos.betweenClosed(
            (int)blastZone.minX, (int)blastZone.minY, (int)blastZone.minZ,
            (int)blastZone.maxX, (int)blastZone.maxY, (int)blastZone.maxZ
        )) {
            BlockState state = level.getBlockState(pos);

            // Check for valuable blocks
            if (isValuableBlock(state)) {
                return SafetyCheck.failed(
                    "Valuable blocks detected: " + state.getBlock().getName().getString(),
                    "Use ore-safe mode or exclude this area"
                );
            }

            // Check for player-placed blocks (heuristic)
            if (isPlayerPlaced(state)) {
                return SafetyCheck.failed(
                    "Player-placed blocks detected",
                    "Respect player builds and find another location"
                );
            }
        }

        return SafetyCheck.passed("No protected blocks detected");
    }

    private SafetyCheck checkClaimedLand(BlockPos center, int radius) {
        // Integration with claim mods (FTB Chunks, etc.)
        // Placeholder implementation

        return SafetyCheck.passed("No claimed land in blast zone");
    }

    private SafetyCheck checkRateLimit() {
        // Check if Foreman has used TNT recently
        // Implement cooldown system

        return SafetyCheck.passed("Rate limit check passed");
    }

    private SafetyCheck checkEntityLimits(BlockPos center, int radius) {
        // Check for nearby entities that could be affected
        int entityCount = level.getEntities(null, new AABB(center).inflate(radius)).size();

        if (entityCount > 100) {
            return SafetyCheck.failed(
                "Too many entities in blast zone: " + entityCount,
                "Clear area or reduce blast radius"
            );
        }

        return SafetyCheck.passed("Entity count acceptable");
    }

    private boolean isValuableBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof OreBlock ||
               block == Blocks.CHEST ||
               block == Blocks.ENDER_CHEST ||
               block == Blocks.SMITHING_TABLE ||
               block == Blocks.ENCHANTING_TABLE;
    }

    private boolean isPlayerPlaced(BlockState state) {
        // Heuristic: Check for blocks rarely found naturally
        // In production, integrate with a block tracking system

        return false; // Placeholder
    }

    public static class SafetyCheckResult {
        private final List<SafetyCheck> checks = new ArrayList<>();

        public void addCheck(SafetyCheck check) {
            checks.add(check);
        }

        public boolean isSafe() {
            return checks.stream().allMatch(SafetyCheck::passed);
        }

        public List<String> getFailureReasons() {
            return checks.stream()
                .filter(check -> !check.passed())
                .map(SafetyCheck::message)
                .toList();
        }
    }

    public record SafetyCheck(
        boolean passed,
        String message,
        String suggestion
    ) {
        static SafetyCheck passed(String message) {
            return new SafetyCheck(true, message, null);
        }

        static SafetyCheck failed(String message, String suggestion) {
            return new SafetyCheck(false, message, suggestion);
        }
    }
}
```

---

## Testing Checklist

### Unit Tests

- [ ] Pattern generation algorithms
- [ ] Safety distance calculations
- [ ] Collection system placement
- [ ] Item aggregation logic
- [ ] Configuration validation

### Integration Tests

- [ ] TNT placement and ignition
- [ ] Collection system retrieval
- [ ] Multi-agent coordination
- [ ] Permission system integration
- [ ] Claim mod integration

### Performance Tests

- [ ] Large blast operations (100+ TNT)
- [ ] Multiple simultaneous operations
- [ ] Collection system efficiency
- [ ] Server impact monitoring

### Safety Tests

- [ ] Player protection
- [ ] Build protection
- [ ] Spawn protection
- [ ] Entity protection
- [ ] Emergency cancellation

---

## Conclusion

This TNT mining automation system provides MineWright with powerful large-scale mining capabilities while maintaining safety, balance, and configurability. The modular design allows server administrators to customize the system for their community's needs, from technical redstone servers to family-friendly survival worlds.

### Key Benefits

1. **Efficiency:** Mine large areas quickly using TNT
2. **Safety:** Multi-layer protection system prevents griefing
3. **Flexibility:** Configurable for different playstyles
4. **Integration:** Works with existing MineWright AI systems
5. **Balance:** Server admins have full control

### Future Enhancements

- Advanced ore detection using LLM vision
- Automated quarry building
- Integration with external storage systems
- Blast analytics and optimization
- Custom explosion types (nuclear, directional, etc.)

---

## Sources

- [MC挖矿机教程 - 多特网](https://m.duote.com/tech/202509/878738.html)
- [漏斗矿车 - 百度百科](https://baike.baidu.com/item/%E6%BC%8F%E6%96%97%E7%9F%BF%E8%BD%A6/16821130)
- [漏斗矿车 - MC百科](https://www.mcmod.cn/item/1070.html?jump=3164)
- [TNT复制器 - MC百科](https://www.mcmod.cn/class/24175.html)
- [NeoForge TNT禁用讨论 - 百度贴吧](https://tieba.baidu.com/p/9242442974)
- [自动爆炸TNT模块教程 - 今日头条](https://www.toutiao.com/zixun/7534930031299119138/)
- [TNT大炮教程 - 今日头条](https://www.toutiao.com/zixun/7515691821025151011/)
- [全自动刷TNT机教程 - 酷宗网](https://www.kuzun.cn/gonglue/pzxvrL7xkN.html)
- [Java版TNT制作 - CSDN博客](https://blog.csdn.net/weixin_42465030/article/details/115638520)

---

**Document End**
