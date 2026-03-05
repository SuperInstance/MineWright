package com.minewright.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link StructureGenerators}.
 *
 * Tests cover:
 * <ul>
 *   <li>All structure types: house, castle, tower, wall, platform, barn, modern, box</li>
 *   <li>Material selection and fallback</li>
 *   <li>Size validation and minimum dimensions</li>
 *   <li>Structure block counts and composition</li>
 *   <li>Edge cases: empty materials, zero dimensions</li>
 *   <li>Position offset calculations</li>
 *   <li>Default structure type handling</li>
 *   <li>Case-insensitive structure type matching</li>
 *   <li>Typo tolerance in structure names</li>
 *   <li>Material cycling</li>
 * </ul>
 */
@DisplayName("StructureGenerators Tests")
class StructureGeneratorsTest {

    private BlockPos originPos;
    private BlockPos elevatedPos;
    private List<Block> stoneMaterials;
    private List<Block> woodMaterials;
    private List<Block> emptyMaterials;
    private List<Block> glassMaterials;

    @BeforeEach
    void setUp() {
        originPos = new BlockPos(0, 0, 0);
        elevatedPos = new BlockPos(100, 64, 100);
        stoneMaterials = List.of(Blocks.STONE, Blocks.COBBLESTONE, Blocks.STONE_BRICKS);
        woodMaterials = List.of(Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.SPRUCE_PLANKS);
        emptyMaterials = new ArrayList<>();
        glassMaterials = List.of(Blocks.GLASS, Blocks.GLASS_PANE);
    }

    // ==================== General Structure Tests ====================

    @Test
    @DisplayName("generate() returns non-empty list for valid structure type")
    void testGenerateReturnsNonEmptyList() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "house", originPos, 7, 5, 7, woodMaterials);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result should contain blocks");
    }

    @Test
    @DisplayName("generate() handles unknown structure type with defaults")
    void testGenerateUnknownStructureType() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "unknown_structure_xyz", originPos, 7, 5, 7, woodMaterials);

        assertNotNull(result, "Unknown structure type should return default house");
        assertFalse(result.isEmpty(), "Default structure should have blocks");
    }

    @Test
    @DisplayName("generate() is case-insensitive")
    void testGenerateCaseInsensitive() {
        List<BlockPlacement> lowercase = StructureGenerators.generate(
            "house", originPos, 7, 5, 7, woodMaterials);
        List<BlockPlacement> uppercase = StructureGenerators.generate(
            "HOUSE", originPos, 7, 5, 7, woodMaterials);
        List<BlockPlacement> mixedCase = StructureGenerators.generate(
            "HoUsE", originPos, 7, 5, 7, woodMaterials);

        assertEquals(lowercase.size(), uppercase.size(),
            "Case should not affect block count");
        assertEquals(lowercase.size(), mixedCase.size(),
            "Mixed case should produce same result");
    }

    @Test
    @DisplayName("generate() uses starting position correctly")
    void testGenerateUsesStartingPosition() {
        List<BlockPlacement> originResult = StructureGenerators.generate(
            "platform", originPos, 5, 0, 5, woodMaterials);
        List<BlockPlacement> elevatedResult = StructureGenerators.generate(
            "platform", elevatedPos, 5, 0, 5, woodMaterials);

        // Find min Y coordinate for each structure
        int originMinY = originResult.stream()
            .mapToInt(b -> b.pos.getY())
            .min()
            .orElse(0);
        int elevatedMinY = elevatedResult.stream()
            .mapToInt(b -> b.pos.getY())
            .min()
            .orElse(0);

        assertTrue(elevatedMinY > originMinY,
            "Elevated structure should have higher Y coordinates");
    }

    @Test
    @DisplayName("generate() handles empty materials list")
    void testGenerateEmptyMaterials() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "box", originPos, 3, 3, 3, emptyMaterials);

        assertNotNull(result, "Should not crash with empty materials");
        assertFalse(result.isEmpty(), "Should use default materials");
    }

    // ==================== House Tests ====================

    @Test
    @DisplayName("generate() creates house with floor")
    void testHouseHasFloor() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "house", originPos, 7, 5, 7, woodMaterials);

        // Count blocks at Y=0 (floor level)
        long floorBlocks = result.stream()
            .filter(b -> b.pos.getY() == 0)
            .count();

        assertTrue(floorBlocks > 0, "House should have floor blocks");
        assertEquals(7 * 7, floorBlocks, "Floor should be width x depth");
    }

    @Test
    @DisplayName("generate() creates house with walls")
    void testHouseHasWalls() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "house", originPos, 7, 5, 7, woodMaterials);

        // Count wall blocks (Y > 0 and Y < height+1)
        long wallBlocks = result.stream()
            .filter(b -> b.pos.getY() > 0 && b.pos.getY() <= 5)
            .count();

        assertTrue(wallBlocks > 0, "House should have wall blocks");
    }

    @Test
    @DisplayName("generate() creates house with roof")
    void testHouseHasRoof() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "house", originPos, 7, 5, 7, woodMaterials);

        // Find maximum Y coordinate
        int maxY = result.stream()
            .mapToInt(b -> b.pos.getY())
            .max()
            .orElse(0);

        assertTrue(maxY > 5, "Roof should extend above wall height");
    }

    @Test
    @DisplayName("generate() creates house with windows")
    void testHouseHasWindows() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "house", originPos, 7, 5, 7, woodMaterials);

        long windowBlocks = result.stream()
            .filter(b -> b.block == Blocks.GLASS_PANE)
            .count();

        assertTrue(windowBlocks > 0, "House should have windows");
    }

    @Test
    @DisplayName("generate() creates house with door")
    void testHouseHasDoor() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "house", originPos, 7, 5, 7, woodMaterials);

        long doorBlocks = result.stream()
            .filter(b -> b.block == Blocks.OAK_DOOR)
            .count();

        assertTrue(doorBlocks > 0, "House should have door blocks");
    }

    @Test
    @DisplayName("generate() accepts 'home' alias for house")
    void testHomeAlias() {
        List<BlockPlacement> house = StructureGenerators.generate(
            "house", originPos, 7, 5, 7, woodMaterials);
        List<BlockPlacement> home = StructureGenerators.generate(
            "home", originPos, 7, 5, 7, woodMaterials);

        assertEquals(house.size(), home.size(),
            "Home alias should produce same block count as house");
    }

    @Test
    @DisplayName("generate() enforces minimum dimensions for house")
    void testHouseMinimumDimensions() {
        // Request house smaller than minimum
        List<BlockPlacement> result = StructureGenerators.generate(
            "house", originPos, 1, 1, 1, woodMaterials);

        // Should use minimum dimensions (5x4x5)
        assertTrue(result.size() > 5 * 4 * 5,
            "Should use minimum dimensions, not requested 1x1x1");
    }

    // ==================== Castle Tests ====================

    @Test
    @DisplayName("generate() creates castle with corner towers")
    void testCastleHasCornerTowers() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "castle", originPos, 15, 10, 15, stoneMaterials);

        // Find blocks at corner positions (tower locations)
        Set<BlockPos> corners = Set.of(
            new BlockPos(0, 0, 0),
            new BlockPos(0, 0, 14),
            new BlockPos(14, 0, 0),
            new BlockPos(14, 0, 14)
        );

        boolean hasCornerBlocks = result.stream()
            .anyMatch(b -> corners.contains(b.pos));

        assertTrue(hasCornerBlocks, "Castle should have blocks at corners");
    }

    @Test
    @DisplayName("generate() creates castle taller than main structure")
    void testCastleTowerHeight() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "castle", originPos, 15, 10, 15, stoneMaterials);

        int maxY = result.stream()
            .mapToInt(b -> b.pos.getY())
            .max()
            .orElse(0);

        assertTrue(maxY > 10 + 5, "Towers should extend above main walls");
    }

    @Test
    @DisplayName("generate() creates castle with crenellations")
    void testCastleHasCrenellations() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "castle", originPos, 15, 10, 15, stoneMaterials);

        // Crenellations are at height + 1 and height + 2
        long topBlocks = result.stream()
            .filter(b -> b.pos.getY() >= 10)
            .count();

        assertTrue(topBlocks > 0, "Castle should have top features (crenellations)");
    }

    @Test
    @DisplayName("generate() accepts 'fort' and 'catle' (typo) aliases")
    void testCastleAliases() {
        List<BlockPlacement> castle = StructureGenerators.generate(
            "castle", originPos, 15, 10, 15, stoneMaterials);
        List<BlockPlacement> fort = StructureGenerators.generate(
            "fort", originPos, 15, 10, 15, stoneMaterials);
        List<BlockPlacement> catle = StructureGenerators.generate(
            "catle", originPos, 15, 10, 15, stoneMaterials);

        assertEquals(castle.size(), fort.size(), "Fort alias should match castle");
        assertEquals(castle.size(), catle.size(), "Typo tolerance should work");
    }

    // ==================== Tower Tests ====================

    @Test
    @DisplayName("generate() creates tower with square footprint")
    void testTowerSquareFootprint() {
        int width = 7;
        List<BlockPlacement> result = StructureGenerators.generate(
            "tower", originPos, width, 10, 0, stoneMaterials);

        // Count floor blocks (should be width * width)
        long floorBlocks = result.stream()
            .filter(b -> b.pos.getY() == 0)
            .count();

        assertTrue(floorBlocks >= width * width,
            "Tower floor should be square (width x width)");
    }

    @Test
    @DisplayName("generate() creates tower with pyramidal roof")
    void testTowerPyramidalRoof() {
        int width = 7;
        List<BlockPlacement> result = StructureGenerators.generate(
            "tower", originPos, width, 10, 0, stoneMaterials);

        int maxY = result.stream()
            .mapToInt(b -> b.pos.getY())
            .max()
            .orElse(0);

        assertTrue(maxY > 10, "Tower roof should extend above body");
    }

    @Test
    @DisplayName("generate() creates tower with stone materials")
    void testTowerMaterials() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "tower", originPos, 7, 10, 0, stoneMaterials);

        boolean hasStoneBlocks = result.stream()
            .anyMatch(b -> b.block == Blocks.STONE_BRICKS ||
                           b.block == Blocks.CHISELED_STONE_BRICKS);

        assertTrue(hasStoneBlocks, "Tower should use stone materials");
    }

    // ==================== Wall Tests ====================

    @Test
    @DisplayName("generate() creates wall as single line of blocks")
    void testWallSingleLine() {
        int width = 10;
        int height = 5;
        List<BlockPlacement> result = StructureGenerators.generate(
            "wall", originPos, width, height, 0, stoneMaterials);

        assertEquals(width * height, result.size(),
            "Wall should have exactly width x height blocks");
    }

    @Test
    @DisplayName("generate() creates wall in X direction")
    void testWallXDirection() {
        int width = 5;
        List<BlockPlacement> result = StructureGenerators.generate(
            "wall", originPos, width, 3, 0, stoneMaterials);

        // Check X coordinates vary
        Set<Integer> xCoords = new HashSet<>();
        result.forEach(b -> xCoords.add(b.pos.getX()));

        assertTrue(xCoords.size() == width, "Wall should span all X coordinates");
    }

    @Test
    @DisplayName("generate() creates wall at Z=0")
    void testWallZCoordinate() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "wall", originPos, 5, 3, 0, stoneMaterials);

        boolean allAtZZero = result.stream()
            .allMatch(b -> b.pos.getZ() == 0);

        assertTrue(allAtZZero, "All wall blocks should be at Z=0");
    }

    // ==================== Platform Tests ====================

    @Test
    @DisplayName("generate() creates flat platform")
    void testPlatformFlat() {
        int width = 10;
        int depth = 10;
        List<BlockPlacement> result = StructureGenerators.generate(
            "platform", originPos, width, 0, depth, woodMaterials);

        assertEquals(width * depth, result.size(),
            "Platform should have exactly width x depth blocks");
    }

    @Test
    @DisplayName("generate() creates platform at Y=0")
    void testPlatformYCoordinate() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "platform", originPos, 10, 0, 10, woodMaterials);

        boolean allAtYZero = result.stream()
            .allMatch(b -> b.pos.getY() == 0);

        assertTrue(allAtYZero, "All platform blocks should be at Y=0");
    }

    @Test
    @DisplayName("generate() creates platform with correct dimensions")
    void testPlatformDimensions() {
        int width = 7;
        int depth = 5;
        List<BlockPlacement> result = StructureGenerators.generate(
            "platform", originPos, width, 0, depth, woodMaterials);

        Set<Integer> xCoords = new HashSet<>();
        Set<Integer> zCoords = new HashSet<>();
        result.forEach(b -> {
            xCoords.add(b.pos.getX());
            zCoords.add(b.pos.getZ());
        });

        assertTrue(xCoords.size() == width, "Platform should span width X coordinates");
        assertTrue(zCoords.size() == depth, "Platform should span depth Z coordinates");
    }

    // ==================== Barn Tests ====================

    @Test
    @DisplayName("generate() creates barn with peaked roof")
    void testBarnPeakedRoof() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "barn", originPos, 11, 6, 9, woodMaterials);

        int maxY = result.stream()
            .mapToInt(b -> b.pos.getY())
            .max()
            .orElse(0);

        assertTrue(maxY > 6, "Barn roof should peak above wall height");
    }

    @Test
    @DisplayName("generate() creates barn with door opening")
    void testBarnDoorOpening() {
        int width = 11;
        List<BlockPlacement> result = StructureGenerators.generate(
            "barn", originPos, width, 6, 9, woodMaterials);

        // Count blocks at front wall (Z=0) between 1/3 and 2/3 width
        long frontDoorBlocks = result.stream()
            .filter(b -> b.pos.getZ() == 0 &&
                        b.pos.getY() <= 2 &&
                        b.pos.getX() >= width / 3 &&
                        b.pos.getX() <= 2 * width / 3)
            .count();

        // Should have fewer blocks here (door opening)
        assertTrue(frontDoorBlocks < 3 * 3, "Door area should have fewer blocks");
    }

    @Test
    @DisplayName("generate() accepts 'shed' alias for barn")
    void testShedAlias() {
        List<BlockPlacement> barn = StructureGenerators.generate(
            "barn", originPos, 11, 6, 9, woodMaterials);
        List<BlockPlacement> shed = StructureGenerators.generate(
            "shed", originPos, 11, 6, 9, woodMaterials);

        assertEquals(barn.size(), shed.size(), "Shed alias should match barn");
    }

    // ==================== Modern House Tests ====================

    @Test
    @DisplayName("generate() creates modern house with glass walls")
    void testModernHouseGlassWalls() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "modern", originPos, 9, 5, 9, woodMaterials);

        long glassBlocks = result.stream()
            .filter(b -> b.block == Blocks.GLASS || b.block == Blocks.GLASS_PANE)
            .count();

        assertTrue(glassBlocks > 0, "Modern house should have glass blocks");
    }

    @Test
    @DisplayName("generate() creates modern house with flat roof")
    void testModernHouseFlatRoof() {
        int height = 5;
        List<BlockPlacement> result = StructureGenerators.generate(
            "modern_house", originPos, 9, height, 9, woodMaterials);

        // Find max Y - should be close to height (flat roof)
        int maxY = result.stream()
            .mapToInt(b -> b.pos.getY())
            .max()
            .orElse(0);

        assertTrue(maxY <= height + 1, "Modern house should have flat roof");
    }

    @Test
    @DisplayName("generate() creates modern house with quartz walls")
    void testModernHouseQuartzWalls() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "modern", originPos, 9, 5, 9, woodMaterials);

        boolean hasQuartz = result.stream()
            .anyMatch(b -> b.block == Blocks.QUARTZ_BLOCK);

        assertTrue(hasQuartz, "Modern house should use quartz blocks");
    }

    @Test
    @DisplayName("generate() accepts 'modern_house' alias")
    void testModernHouseAlias() {
        List<BlockPlacement> modern = StructureGenerators.generate(
            "modern", originPos, 9, 5, 9, woodMaterials);
        List<BlockPlacement> modernHouse = StructureGenerators.generate(
            "modern_house", originPos, 9, 5, 9, woodMaterials);

        assertEquals(modern.size(), modernHouse.size(),
            "Aliases should produce same result");
    }

    // ==================== Box Tests ====================

    @Test
    @DisplayName("generate() creates solid box")
    void testBoxSolid() {
        int width = 5;
        int height = 5;
        int depth = 5;
        List<BlockPlacement> result = StructureGenerators.generate(
            "box", originPos, width, height, depth, stoneMaterials);

        assertEquals(width * height * depth, result.size(),
            "Box should be completely solid (width x height x depth)");
    }

    @Test
    @DisplayName("generate() accepts 'cube' alias for box")
    void testCubeAlias() {
        List<BlockPlacement> box = StructureGenerators.generate(
            "box", originPos, 5, 5, 5, stoneMaterials);
        List<BlockPlacement> cube = StructureGenerators.generate(
            "cube", originPos, 5, 5, 5, stoneMaterials);

        assertEquals(box.size(), cube.size(), "Cube alias should match box");
    }

    // ==================== Material Tests ====================

    @Test
    @DisplayName("generate() cycles through materials list")
    void testMaterialCycling() {
        List<Block> materials = List.of(
            Blocks.STONE, Blocks.DIRT, Blocks.COBBLESTONE,
            Blocks.SAND, Blocks.GRAVEL, Blocks.ANDESITE
        );

        List<BlockPlacement> result = StructureGenerators.generate(
            "box", originPos, 6, 1, 6, materials);

        Set<Block> usedBlocks = new HashSet<>();
        result.forEach(b -> usedBlocks.add(b.block));

        assertTrue(usedBlocks.size() > 1, "Should use multiple materials");
        assertTrue(usedBlocks.size() <= materials.size(),
            "Should not use more materials than provided");
    }

    @Test
    @DisplayName("generate() uses oak planks as default material")
    void testDefaultMaterial() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "box", originPos, 3, 3, 3, emptyMaterials);

        boolean hasOakPlanks = result.stream()
            .anyMatch(b -> b.block == Blocks.OAK_PLANKS);

        assertTrue(hasOakPlanks, "Should use oak planks as default");
    }

    @Test
    @DisplayName("generate() does not use glass as roof material")
    void testGlassRoofFallback() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "house", originPos, 7, 5, 7, glassMaterials);

        // Find roof blocks (Y > 5)
        boolean roofHasGlass = result.stream()
            .filter(b -> b.pos.getY() > 5)
            .anyMatch(b -> b.block == Blocks.GLASS || b.block == Blocks.GLASS_PANE);

        assertFalse(roofHasGlass, "Should not use glass as roof material");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("generate() handles zero dimensions")
    void testZeroDimensions() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "platform", originPos, 0, 0, 0, woodMaterials);

        // Platform with 0 dimensions should be empty
        assertEquals(0, result.size(), "Zero dimensions should produce no blocks");
    }

    @Test
    @DisplayName("generate() handles negative dimensions (treated as zero)")
    void testNegativeDimensions() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "platform", originPos, -5, 0, -5, woodMaterials);

        assertEquals(0, result.size(), "Negative dimensions should produce no blocks");
    }

    @Test
    @DisplayName("generate() handles very large dimensions")
    void testLargeDimensions() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "platform", originPos, 100, 0, 100, woodMaterials);

        assertEquals(10000, result.size(), "Should handle large platforms");
    }

    @Test
    @DisplayName("generate() handles single block dimensions")
    void testSingleBlockDimensions() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "box", originPos, 1, 1, 1, stoneMaterials);

        assertEquals(1, result.size(), "Single block dimension should produce 1 block");
    }

    @Test
    @DisplayName("generate() creates structure at negative coordinates")
    void testNegativeCoordinates() {
        BlockPos negativePos = new BlockPos(-50, -30, -50);
        List<BlockPlacement> result = StructureGenerators.generate(
            "platform", negativePos, 5, 0, 5, woodMaterials);

        assertFalse(result.isEmpty(), "Should work with negative coordinates");

        // Check that blocks are at negative coordinates
        boolean hasNegativeCoords = result.stream()
            .anyMatch(b -> b.pos.getX() < 0 || b.pos.getY() < 0 || b.pos.getZ() < 0);

        assertTrue(hasNegativeCoords, "Blocks should be at negative coordinates");
    }

    @Test
    @DisplayName("generate() prevents block overlap in structure")
    void testNoBlockOverlap() {
        List<BlockPlacement> result = StructureGenerators.generate(
            "box", originPos, 3, 3, 3, stoneMaterials);

        // Check for duplicate positions
        Set<BlockPos> uniquePositions = new HashSet<>();
        for (BlockPlacement placement : result) {
            assertFalse(uniquePositions.contains(placement.pos),
                "Should not have overlapping blocks at " + placement.pos);
            uniquePositions.add(placement.pos);
        }
    }

    @Test
    @DisplayName("generate() produces deterministic results")
    void testDeterministicResults() {
        List<BlockPlacement> result1 = StructureGenerators.generate(
            "house", originPos, 7, 5, 7, woodMaterials);
        List<BlockPlacement> result2 = StructureGenerators.generate(
            "house", originPos, 7, 5, 7, woodMaterials);

        assertEquals(result1.size(), result2.size(),
            "Same inputs should produce same block count");
    }

    @Test
    @DisplayName("generate() handles null materials gracefully")
    void testNullMaterialsHandling() {
        // This should not crash, but behavior depends on implementation
        // Either uses default materials or returns empty
        assertDoesNotThrow(() -> {
            List<BlockPlacement> result = StructureGenerators.generate(
                "box", originPos, 3, 3, 3, null);
            assertNotNull(result);
        }, "Should not throw exception with null materials");
    }
}
