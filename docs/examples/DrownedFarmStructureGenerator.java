package com.minewright.structure;

import com.minewright.action.ActionResult;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for generating drowned farm structures.
 * Provides static methods for creating various farm designs.
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * List<BlockPlacement> farm = DrownedFarmStructureGenerator.buildAerialFarm(
 *     new BlockPos(x, y, z), 80, 3, "stream"
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @see BuildDrownedFarmAction
 */
public class DrownedFarmStructureGenerator {

    // Default dimensions and settings
    private static final int DEFAULT_PLATFORM_SIZE = 80;
    private static final int DEFAULT_LAYER_COUNT = 3;
    private static final int DEFAULT_LAYER_SPACING = 5;
    private static final int DEFAULT_DROP_SHAFT_HEIGHT = 45;
    private static final int DEFAULT_KILL_CHAMBER_OFFSET = 50;
    private static final int WATER_DEPTH = 2;
    private static final int HOLE_SPACING = 4;

    /**
     * Build an aerial drowned farm (best for tridents).
     *
     * @param basePos Starting position (corner of platform)
     * @param platformSize Size of platform (recommended 60-120)
     * @param layers Number of spawning layers (1-8)
     * @param collectionType Collection method: "drop", "bubble", or "stream"
     * @return List of block placements for the farm
     */
    public static List<BlockPlacement> buildAerialFarm(
            BlockPos basePos,
            int platformSize,
            int layers,
            String collectionType) {

        List<BlockPlacement> plan = new ArrayList<>();
        int halfSize = platformSize / 2;
        int centerX = basePos.getX() + halfSize;
        int centerZ = basePos.getZ() + halfSize;
        int baseY = basePos.getY();

        // Validate inputs
        if (platformSize < 20 || platformSize > 128) {
            throw new IllegalArgumentException("Platform size must be between 20 and 128");
        }
        if (layers < 1 || layers > 8) {
            throw new IllegalArgumentException("Layer count must be between 1 and 8");
        }

        // Build spawning layers
        for (int layer = 0; layer < layers; layer++) {
            int layerY = baseY - (layer * DEFAULT_LAYER_SPACING);
            buildSpawningLayer(plan, basePos.getX(), layerY, basePos.getZ(), platformSize);
        }

        // Build central drop shaft
        buildDropShaft(plan, centerX, baseY - (layers * DEFAULT_LAYER_SPACING), centerZ,
            DEFAULT_DROP_SHAFT_HEIGHT);

        // Build killing chamber
        buildKillingChamber(plan, centerX, baseY + DEFAULT_KILL_CHAMBER_OFFSET, centerZ);

        // Build AFK platform
        buildAFKPlatform(plan, centerX, baseY + DEFAULT_KILL_CHAMBER_OFFSET + 3, centerZ);

        // Build collection system
        switch (collectionType.toLowerCase()) {
            case "stream" -> buildWaterStreamCollection(plan, centerX,
                baseY - (layers * DEFAULT_LAYER_SPACING), centerZ, platformSize);
            case "bubble" -> buildBubbleColumnCollection(plan, centerX,
                baseY + DEFAULT_KILL_CHAMBER_OFFSET - 10, centerZ);
            case "drop" -> { /* Basic drop collection handled by killing chamber */ }
            default -> buildWaterStreamCollection(plan, centerX,
                baseY - (layers * DEFAULT_LAYER_SPACING), centerZ, platformSize);
        }

        return plan;
    }

    /**
     * Build a river conversion farm (simpler, no tridents).
     *
     * @param basePos Starting position
     * @param chamberSize Size of conversion chamber (recommended 9-15)
     * @return List of block placements
     */
    public static List<BlockPlacement> buildRiverFarm(BlockPos basePos, int chamberSize) {
        List<BlockPlacement> plan = new ArrayList<>();

        // Build conversion chamber
        buildConversionChamber(plan, basePos, chamberSize);

        // Build collection tunnel
        buildCollectionTunnel(plan, basePos.offset(chamberSize / 2, 1, chamberSize));

        // Build killing area
        buildSimpleKillingArea(plan, basePos.offset(chamberSize / 2, 1, chamberSize + 20));

        return plan;
    }

    /**
     * Build a compact drowned farm for tight spaces.
     *
     * @param basePos Starting position
     * @param size Size (must be at least 40)
     * @return List of block placements
     */
    public static List<BlockPlacement> buildCompactFarm(BlockPos basePos, int size) {
        return buildAerialFarm(basePos, size, 1, "drop");
    }

    /**
     * Build a spawning layer with water and holes.
     */
    private static void buildSpawningLayer(List<BlockPlacement> plan,
            int startX, int y, int startZ, int size) {

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                BlockPos basePos = new BlockPos(startX + x, y, startZ + z);

                // Check if this should be a hole
                boolean isHole = (x % HOLE_SPACING == 0) && (z % HOLE_SPACING == 0) &&
                    (x > 0 && x < size - 1) && (z > 0 && z < size - 1);

                if (isHole) {
                    // Create hole through platform
                    plan.add(new BlockPlacement(basePos, Blocks.AIR));

                    // Water funnel below
                    plan.add(new BlockPlacement(basePos.below(1), Blocks.WATER));
                    plan.add(new BlockPlacement(basePos.below(2), Blocks.WATER));
                } else {
                    // Solid base
                    plan.add(new BlockPlacement(basePos, Blocks.STONE));

                    // Water layers
                    plan.add(new BlockPlacement(basePos.above(1), Blocks.WATER));
                    plan.add(new BlockPlacement(basePos.above(2), Blocks.WATER));
                }
            }
        }
    }

    /**
     * Build central drop shaft with glass walls.
     */
    private static void buildDropShaft(List<BlockPlacement> plan,
            int centerX, int startY, int centerZ, int height) {

        for (int y = 0; y < height; y++) {
            BlockPos shaftPos = new BlockPos(centerX, startY + y, centerZ);

            // Air in center
            plan.add(new BlockPlacement(shaftPos, Blocks.AIR));

            // Glass walls
            plan.add(new BlockPlacement(shaftPos.east(), Blocks.GLASS));
            plan.add(new BlockPlacement(shaftPos.west(), Blocks.GLASS));
            plan.add(new BlockPlacement(shaftPos.south(), Blocks.GLASS));
            plan.add(new BlockPlacement(shaftPos.north(), Blocks.GLASS));
        }
    }

    /**
     * Build killing chamber with collection system.
     */
    private static void buildKillingChamber(List<BlockPlacement> plan,
            int centerX, int centerY, int centerZ) {

        BlockPos killFloor = new BlockPos(centerX, centerY - 2, centerZ);

        // Hopper floor (2x2)
        plan.add(new BlockPlacement(killFloor, Blocks.HOPPER));
        plan.add(new BlockPlacement(killFloor.east(), Blocks.HOPPER));
        plan.add(new BlockPlacement(killFloor.south(), Blocks.HOPPER));
        plan.add(new BlockPlacement(killFloor.south().east(), Blocks.HOPPER));

        // Chests below hoppers
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                BlockPos chestPos = killFloor.below().offset(dx, 0, dz);
                plan.add(new BlockPlacement(chestPos, Blocks.CHEST));
            }
        }

        // Glass walls around killing chamber
        for (int y = -1; y <= 1; y++) {
            for (Direction dir : Direction.values()) {
                if (dir.getAxis() == Direction.Axis.Y) continue;
                BlockPos wallPos = killFloor.offset(dir.getNormal()).above(y);
                plan.add(new BlockPlacement(wallPos, Blocks.GLASS));
            }
        }
    }

    /**
     * Build AFK platform above killing chamber.
     */
    private static void buildAFKPlatform(List<BlockPlacement> plan,
            int centerX, int centerY, int centerZ) {

        // 5x5 platform
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos pos = new BlockPos(centerX + dx, centerY, centerZ + dz);
                plan.add(new BlockPlacement(pos, Blocks.OAK_PLANKS));
            }
        }

        // Safety railing
        for (int i = -2; i <= 2; i++) {
            plan.add(new BlockPlacement(new BlockPos(centerX + i, centerY + 1, centerZ - 3),
                Blocks.OAK_FENCE));
            plan.add(new BlockPlacement(new BlockPos(centerX + i, centerY + 1, centerZ + 3),
                Blocks.OAK_FENCE));
            plan.add(new BlockPlacement(new BlockPos(centerX - 3, centerY + 1, centerZ + i),
                Blocks.OAK_FENCE));
            plan.add(new BlockPlacement(new BlockPos(centerX + 3, centerY + 1, centerZ + i),
                Blocks.OAK_FENCE));
        }
    }

    /**
     * Build water stream collection system.
     */
    private static void buildWaterStreamCollection(List<BlockPlacement> plan,
            int centerX, int centerY, int centerZ, int platformSize) {

        int streamLength = platformSize / 2;

        // Create 4 water streams flowing to center from each direction
        for (int direction = 0; direction < 4; direction++) {
            for (int dist = 2; dist < streamLength; dist++) {
                BlockPos streamPos;

                switch (direction) {
                    case 0 -> streamPos = new BlockPos(centerX + dist, centerY, centerZ); // East
                    case 1 -> streamPos = new BlockPos(centerX - dist, centerY, centerZ); // West
                    case 2 -> streamPos = new BlockPos(centerX, centerY, centerZ + dist); // South
                    case 3 -> streamPos = new BlockPos(centerX, centerY, centerZ - dist); // North
                    default -> continue;
                }

                // Place water source
                plan.add(new BlockPlacement(streamPos, Blocks.WATER));

                // Signs below to contain water
                plan.add(new BlockPlacement(streamPos.below(), Blocks.OAK_SIGN));
            }
        }
    }

    /**
     * Build bubble column collection system.
     */
    private static void buildBubbleColumnCollection(List<BlockPlacement> plan,
            int centerX, int centerY, int centerZ) {

        // Upward bubble column (soul sand)
        BlockPos soulSandPos = new BlockPos(centerX, centerY, centerZ);
        plan.add(new BlockPlacement(soulSandPos, Blocks.SOUL_SAND));

        // Water column above
        for (int y = 1; y <= 10; y++) {
            plan.add(new BlockPlacement(soulSandPos.above(y), Blocks.WATER));
        }

        // Collection platform at top
        BlockPos collectionY = soulSandPos.above(11);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos pos = collectionY.offset(dx, 0, dz);
                plan.add(new BlockPlacement(pos, Blocks.PRISMARINE));

                // Hoppers in center
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    plan.add(new BlockPlacement(pos, Blocks.HOPPER));
                }
            }
        }
    }

    /**
     * Build conversion chamber for river farms.
     */
    private static void buildConversionChamber(List<BlockPlacement> plan,
            BlockPos basePos, int size) {

        // Hollow chamber with stone walls
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    BlockPos pos = basePos.offset(x, y, z);
                    boolean isWall = (x == 0 || x == size - 1 ||
                                     y == 0 || y == size - 1 ||
                                     z == 0 || z == size - 1);

                    if (isWall) {
                        plan.add(new BlockPlacement(pos, Blocks.STONE_BRICKS));

                        // Add openings for water flow
                        if (y == 1 && (z == size / 2 || x == size / 2)) {
                            plan.add(new BlockPlacement(pos, Blocks.WATER));
                        }
                    } else if (y < 3) {
                        // Water at bottom
                        plan.add(new BlockPlacement(pos, Blocks.WATER));
                    } else {
                        // Air above
                        plan.add(new BlockPlacement(pos, Blocks.AIR));
                    }
                }
            }
        }
    }

    /**
     * Build collection tunnel for river farms.
     */
    private static void buildCollectionTunnel(List<BlockPlacement> plan, BlockPos start) {
        // 20-block long tunnel
        for (int z = 0; z < 20; z++) {
            BlockPos pos = start.offset(0, 0, z);

            // Water floor
            plan.add(new BlockPlacement(pos, Blocks.WATER));

            // Glass walls
            plan.add(new BlockPlacement(pos.east(), Blocks.GLASS));
            plan.add(new BlockPlacement(pos.west(), Blocks.GLASS));

            // Glass ceiling
            plan.add(new BlockPlacement(pos.above(), Blocks.GLASS));
        }
    }

    /**
     * Build simple killing area for river farms.
     */
    private static void buildSimpleKillingArea(List<BlockPlacement> plan, BlockPos pos) {
        // Hopper for collection
        plan.add(new BlockPlacement(pos.below(), Blocks.HOPPER));
        plan.add(new BlockPlacement(pos.below(2), Blocks.CHEST));

        // Glass enclosure
        for (int y = 0; y <= 2; y++) {
            plan.add(new BlockPlacement(pos.east().above(y), Blocks.GLASS));
            plan.add(new BlockPlacement(pos.west().above(y), Blocks.GLASS));
            plan.add(new BlockPlacement(pos.north().above(y), Blocks.GLASS));
            plan.add(new BlockPlacement(pos.south().above(y), Blocks.GLASS));
        }
    }

    /**
     * Estimate farm production rates.
     *
     * @param platformSize Size of spawning platform
     * @param layers Number of spawning layers
     * @param lootingLevel Looting enchantment level (0-3)
     * @return Production statistics
     */
    public static FarmStats estimateProduction(int platformSize, int layers, int lootingLevel) {
        // Base spawn rate: ~900 drowned/hour per 80x80 layer
        double baseRate = 900.0 * (platformSize / 80.0) * (platformSize / 80.0);

        // Layer efficiency (each layer is slightly less efficient)
        double layerEfficiency = 1.0 - (layers * 0.05);

        int drownedPerHour = (int) (baseRate * layers * layerEfficiency);

        // Trident calculations
        double tridentChance = 0.0375; // 15% spawn * 25% drop
        if (lootingLevel == 1) tridentChance = 0.045;
        if (lootingLevel == 2) tridentChance = 0.0525;
        if (lootingLevel == 3) tridentChance = 0.0555; // 15% spawn * 37% drop

        int tridentsPerHour = (int) (drownedPerHour * tridentChance);

        // Nautilus shell (8% base, +3.5% per looting)
        double shellChance = 0.08 + (lootingLevel * 0.035);
        int shellsPerHour = (int) (drownedPerHour * shellChance);

        // Copper (11% base, +4.25% per looting, natural drowned only)
        double copperChance = 0.11 + (lootingLevel * 0.0425);
        int copperPerHour = (int) (drownedPerHour * copperChance);

        return new FarmStats(drownedPerHour, tridentsPerHour, shellsPerHour, copperPerHour);
    }

    /**
     * Statistics for drowned farm production.
     */
    public record FarmStats(
        int drownedPerHour,
        int tridentsPerHour,
        int nautilusShellsPerHour,
        int copperPerHour
    ) {
        @Override
        public String toString() {
            return String.format("""
                Drowned Farm Production Estimates:
                - Drowned: %,d/hour
                - Tridents: %,d/hour (one every ~%d hours)
                - Nautilus Shells: %,d/hour
                - Copper Ingots: %,d/hour
                """,
                drownedPerHour,
                tridentsPerHour,
                tridentsPerHour > 0 ? 100 / tridentsPerHour : 999,
                nautilusShellsPerHour,
                copperPerHour
            );
        }
    }

    /**
     * Find optimal Y-level for drowned farm in current biome.
     *
     * @param level World level
     * @param pos Position to check
     * @return Optimal Y-level for spawning platform
     */
    public static int findOptimalYLevel(Level level, BlockPos pos) {
        var biome = level.getBiome(pos).unwrapKey().get().location().toString();

        // Ocean biomes: Y must be < 58 (6 blocks below sea level)
        if (biome.contains("ocean")) {
            return 50; // Safe depth
        }

        // River and frozen river: No height restriction
        // Build at sea level for convenience
        if (biome.contains("river")) {
            return 62; // Sea level
        }

        // Default: Build slightly above sea level
        return 60;
    }

    /**
     * Check if position is suitable for drowned farm.
     *
     * @param level World level
     * @param centerPos Center position for farm
     * @param size Farm size
     * @return Suitability check result
     */
    public static SuitabilityResult checkSuitability(Level level, BlockPos centerPos, int size) {
        var biome = level.getBiome(centerPos).unwrapKey().get().location().toString();
        boolean isOcean = biome.contains("ocean");
        boolean isRiver = biome.contains("river");

        if (!isOcean && !isRiver) {
            return new SuitabilityResult(false,
                "Not in a water biome (current: " + biome + ")",
                SuitabilityResult.Warning.WRONG_BIOME);
        }

        // Check space
        int halfSize = size / 2;
        BlockPos corner1 = centerPos.offset(-halfSize, 0, -halfSize);
        BlockPos corner2 = centerPos.offset(halfSize, 0, halfSize);

        for (BlockPos pos : BlockPos.betweenClosed(corner1, corner2)) {
            var block = level.getBlockState(pos).getBlock();
            if (block != Blocks.WATER && block != Blocks.AIR) {
                return new SuitabilityResult(false,
                    "Area not clear (found " + block + " at " + pos + ")",
                    SuitabilityResult.Warning.OBSTRUCTED);
            }
        }

        String warning = isOcean ?
            "Ocean biome - must build at Y < 58 for optimal spawning" :
            "River biome - no tridents from converted zombies";

        return new SuitabilityResult(true, warning, null);
    }

    /**
     * Result of suitability check.
     */
    public record SuitabilityResult(
        boolean suitable,
        String message,
        Warning warning
    ) {
        public enum Warning {
            WRONG_BIOME,
            OBSTRUCTED,
            HEIGHT_RESTRICTION
        }
    }
}
