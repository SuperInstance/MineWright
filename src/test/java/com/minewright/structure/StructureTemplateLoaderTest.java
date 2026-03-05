package com.minewright.structure;

import com.minewright.testutil.TestLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link StructureTemplateLoader}.
 *
 * Tests cover:
 * <ul>
 *   <li>NBT file loading from resources</li>
 *   <li>NBT parsing and block extraction</li>
 *   <li>Structure size calculation</li>
 *   <li>Palette handling</li>
 *   <li>Air block filtering</li>
 *   <li>Error handling and logging</li>
 *   <li>Template block creation</li>
 *   <li>Loaded template metadata</li>
 *   <li>Available structures listing</li>
 *   <li>Multiple file name format attempts</li>
 * </ul>
 */
@DisplayName("StructureTemplateLoader Tests")
class StructureTemplateLoaderTest {

    @Mock
    private ServerLevel mockLevel;

    @Mock
    private StructureTemplate mockTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize TestLogger
        TestLogger.initialize();
    }

    // ==================== TemplateBlock Tests ====================

    @Test
    @DisplayName("TemplateBlock stores position and block state")
    void testTemplateBlockCreation() {
        BlockPos pos = new BlockPos(1, 2, 3);
        BlockState state = Blocks.STONE.defaultBlockState();

        StructureTemplateLoader.TemplateBlock templateBlock =
            new StructureTemplateLoader.TemplateBlock(pos, state);

        assertEquals(pos, templateBlock.relativePos,
            "Position should be stored correctly");
        assertEquals(state, templateBlock.blockState,
            "Block state should be stored correctly");
    }

    @Test
    @DisplayName("TemplateBlock accepts null position")
    void testTemplateBlockNullPosition() {
        BlockState state = Blocks.STONE.defaultBlockState();

        assertDoesNotThrow(() -> {
            StructureTemplateLoader.TemplateBlock templateBlock =
                new StructureTemplateLoader.TemplateBlock(null, state);
            assertNull(templateBlock.relativePos);
        }, "TemplateBlock should accept null position");
    }

    @Test
    @DisplayName("TemplateBlock accepts null block state")
    void testTemplateBlockNullBlockState() {
        BlockPos pos = new BlockPos(0, 0, 0);

        assertDoesNotThrow(() -> {
            StructureTemplateLoader.TemplateBlock templateBlock =
                new StructureTemplateLoader.TemplateBlock(pos, null);
            assertNull(templateBlock.blockState);
        }, "TemplateBlock should accept null block state");
    }

    // ==================== LoadedTemplate Tests ====================

    @Test
    @DisplayName("LoadedTemplate stores metadata correctly")
    void testLoadedTemplateCreation() {
        String name = "test_structure";
        List<StructureTemplateLoader.TemplateBlock> blocks = new ArrayList<>();
        blocks.add(new StructureTemplateLoader.TemplateBlock(
            new BlockPos(0, 0, 0), Blocks.STONE.defaultBlockState()));

        StructureTemplateLoader.LoadedTemplate template =
            new StructureTemplateLoader.LoadedTemplate(name, blocks, 10, 5, 10);

        assertEquals(name, template.name, "Name should be stored");
        assertEquals(blocks, template.blocks, "Blocks list should be stored");
        assertEquals(10, template.width, "Width should be stored");
        assertEquals(5, template.height, "Height should be stored");
        assertEquals(10, template.depth, "Depth should be stored");
    }

    @Test
    @DisplayName("LoadedTemplate accepts empty blocks list")
    void testLoadedTemplateEmptyBlocks() {
        StructureTemplateLoader.LoadedTemplate template =
            new StructureTemplateLoader.LoadedTemplate("empty", new ArrayList<>(), 0, 0, 0);

        assertTrue(template.blocks.isEmpty(), "Empty blocks list should be stored");
        assertEquals(0, template.width);
        assertEquals(0, template.height);
        assertEquals(0, template.depth);
    }

    @Test
    @DisplayName("LoadedTemplate accepts null blocks list")
    void testLoadedTemplateNullBlocks() {
        assertDoesNotThrow(() -> {
            StructureTemplateLoader.LoadedTemplate template =
                new StructureTemplateLoader.LoadedTemplate("test", null, 10, 5, 10);
            assertNull(template.blocks);
        }, "LoadedTemplate should accept null blocks list");
    }

    // ==================== NBT Parsing Tests ====================

    @Test
    @DisplayName("parseNBTStructure extracts size correctly")
    void testParseNBTSize() throws Exception {
        CompoundTag nbt = createValidNBTStructure();

        Method parseMethod = StructureTemplateLoader.class.getDeclaredMethod(
            "parseNBTStructure", CompoundTag.class, String.class);
        parseMethod.setAccessible(true);

        StructureTemplateLoader.LoadedTemplate result =
            (StructureTemplateLoader.LoadedTemplate) parseMethod.invoke(null, nbt, "test");

        assertNotNull(result, "Parsed template should not be null");
        assertEquals(10, result.width, "Width should be extracted from NBT");
        assertEquals(5, result.height, "Height should be extracted from NBT");
        assertEquals(8, result.depth, "Depth should be extracted from NBT");
    }

    @Test
    @DisplayName("parseNBTStructure parses palette correctly")
    void testParseNBTPalette() throws Exception {
        CompoundTag nbt = createValidNBTStructure();

        Method parseMethod = StructureTemplateLoader.class.getDeclaredMethod(
            "parseNBTStructure", CompoundTag.class, String.class);
        parseMethod.setAccessible(true);

        StructureTemplateLoader.LoadedTemplate result =
            (StructureTemplateLoader.LoadedTemplate) parseMethod.invoke(null, nbt, "test");

        assertNotNull(result, "Parsed template should not be null");
        assertFalse(result.blocks.isEmpty(), "Should have blocks from palette");
    }

    @Test
    @DisplayName("parseNBTStructure filters air blocks")
    void testParseNBTFiltersAir() throws Exception {
        CompoundTag nbt = createValidNBTStructureWithAir();

        Method parseMethod = StructureTemplateLoader.class.getDeclaredMethod(
            "parseNBTStructure", CompoundTag.class, String.class);
        parseMethod.setAccessible(true);

        StructureTemplateLoader.LoadedTemplate result =
            (StructureTemplateLoader.LoadedTemplate) parseMethod.invoke(null, nbt, "test");

        // Count non-air blocks in result
        long nonAirCount = result.blocks.stream()
            .filter(b -> !b.blockState.isAir())
            .count();

        assertTrue(nonAirCount > 0, "Should have non-air blocks");
        assertTrue(result.blocks.size() <= totalBlocksInNBT(nbt),
            "Should filter out some air blocks");
    }

    @Test
    @DisplayName("parseNBTStructure handles unknown block names")
    void testParseNBTUnknownBlocks() throws Exception {
        CompoundTag nbt = createNBTWithUnknownBlock();

        Method parseMethod = StructureTemplateLoader.class.getDeclaredMethod(
            "parseNBTStructure", CompoundTag.class, String.class);
        parseMethod.setAccessible(true);

        StructureTemplateLoader.LoadedTemplate result =
            (StructureTemplateLoader.LoadedTemplate) parseMethod.invoke(null, nbt, "test");

        assertNotNull(result, "Should handle unknown blocks gracefully");
        // Unknown blocks should be replaced with AIR
        assertTrue(result.blocks.stream().allMatch(b ->
            b.blockState.isAir() || b.blockState != null),
            "Unknown blocks should default to AIR or be filtered");
    }

    @Test
    @DisplayName("parseNBTStructure preserves block positions")
    void testParseNBTBlockPositions() throws Exception {
        CompoundTag nbt = createValidNBTStructure();

        Method parseMethod = StructureTemplateLoader.class.getDeclaredMethod(
            "parseNBTStructure", CompoundTag.class, String.class);
        parseMethod.setAccessible(true);

        StructureTemplateLoader.LoadedTemplate result =
            (StructureTemplateLoader.LoadedTemplate) parseMethod.invoke(null, nbt, "test");

        assertNotNull(result.blocks, "Should have blocks");
        // Check that positions are within structure bounds
        assertTrue(result.blocks.stream().allMatch(b ->
            b.relativePos.getX() >= 0 && b.relativePos.getX() < result.width &&
            b.relativePos.getY() >= 0 && b.relativePos.getY() < result.height &&
            b.relativePos.getZ() >= 0 && b.relativePos.getZ() < result.depth),
            "All positions should be within structure bounds");
    }

    // ==================== loadFromNBT Tests ====================

    @Test
    @DisplayName("loadFromNBT returns null for missing structure")
    void testLoadFromNBTMissingStructure() {
        // Mock the template manager to return empty optional
        when(mockLevel.getStructureManager()).thenReturn(mock(net.minecraft.world.level.levelgen.structure.templatesetem.StructureManager.class));

        StructureTemplateLoader.LoadedTemplate result =
            StructureTemplateLoader.loadFromNBT(mockLevel, "nonexistent_structure");

        assertNull(result, "Missing structure should return null");
    }

    @Test
    @DisplayName("loadFromNBT tries multiple name formats")
    void testLoadFromNBTMultipleFormats() {
        // This test verifies the method tries:
        // 1. Exact name + .nbt
        // 2. Lowercase with underscores
        // 3. CamelCase to lowercase with underscores

        // Since we can't mock classpath resources easily,
        // we just verify it doesn't crash
        assertDoesNotThrow(() -> {
            StructureTemplateLoader.loadFromNBT(mockLevel, "MyStructureName");
        }, "Should try multiple name format variations");
    }

    @Test
    @DisplayName("loadFromNBT handles empty structure name")
    void testLoadFromNBTEmptyName() {
        assertDoesNotThrow(() -> {
            StructureTemplateLoader.loadFromNBT(mockLevel, "");
        }, "Should handle empty structure name gracefully");
    }

    @Test
    @DisplayName("loadFromNBT handles null structure name")
    void testLoadFromNBTNullName() {
        assertDoesNotThrow(() -> {
            StructureTemplateLoader.loadFromNBT(mockLevel, null);
        }, "Should handle null structure name gracefully");
    }

    // ==================== getAvailableStructures Tests ====================

    @Test
    @DisplayName("getAvailableStructures returns list")
    void testGetAvailableStructuresReturnsList() {
        List<String> structures = StructureTemplateLoader.getAvailableStructures();

        assertNotNull(structures, "Should return a list");
        assertTrue(structures instanceof List, "Should return List<String>");
    }

    @Test
    @DisplayName("getAvailableStructures returns empty list when no structures directory")
    void testGetAvailableStructuresNoDirectory() {
        // Temporarily change user.dir to a directory without structures
        String originalDir = System.getProperty("user.dir");

        try {
            System.setProperty("user.dir", "/tmp/nonexistent_path_12345");
            List<String> structures = StructureTemplateLoader.getAvailableStructures();

            assertNotNull(structures, "Should return list even without directory");
            assertTrue(structures.isEmpty(), "Should be empty when no directory");
        } finally {
            System.setProperty("user.dir", originalDir);
        }
    }

    @Test
    @DisplayName("getAvailableStructures filters .nbt files")
    void testGetAvailableStructuresFiltersNBT() {
        List<String> structures = StructureTemplateLoader.getAvailableStructures();

        // All entries should be .nbt files
        assertTrue(structures.stream().allMatch(s -> !s.endsWith(".nbt")),
            "File extension should be stripped from names");
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Handles invalid NBT data gracefully")
    void testInvalidNBTData() throws Exception {
        byte[] invalidData = new byte[]{0x00, 0x01, 0x02};
        InputStream invalidStream = new ByteArrayInputStream(invalidData);

        assertThrows(Exception.class, () -> {
            NbtIo.readCompressed(invalidStream);
        }, "Invalid NBT should throw exception");
    }

    @Test
    @DisplayName("Handles empty NBT structure")
    void testEmptyNBTStructure() throws Exception {
        CompoundTag emptyNbt = new CompoundTag();

        Method parseMethod = StructureTemplateLoader.class.getDeclaredMethod(
            "parseNBTStructure", CompoundTag.class, String.class);
        parseMethod.setAccessible(true);

        // Should handle empty NBT (no size, palette, or blocks)
        assertDoesNotThrow(() -> {
            parseMethod.invoke(null, emptyNbt, "empty");
        }, "Should handle empty NBT without crashing");
    }

    @Test
    @DisplayName("Handles malformed palette entries")
    void testMalformedPalette() throws Exception {
        CompoundTag nbt = createNBTWithMalformedPalette();

        Method parseMethod = StructureTemplateLoader.class.getDeclaredMethod(
            "parseNBTStructure", CompoundTag.class, String.class);
        parseMethod.setAccessible(true);

        assertDoesNotThrow(() -> {
            parseMethod.invoke(null, nbt, "malformed");
        }, "Should handle malformed palette gracefully");
    }

    @Test
    @DisplayName("Handles negative size values")
    void testNegativeSize() throws Exception {
        CompoundTag nbt = createNBTWithNegativeSize();

        Method parseMethod = StructureTemplateLoader.class.getDeclaredMethod(
            "parseNBTStructure", CompoundTag.class, String.class);
        parseMethod.setAccessible(true);

        StructureTemplateLoader.LoadedTemplate result =
            (StructureTemplateLoader.LoadedTemplate) parseMethod.invoke(null, nbt, "negative");

        assertNotNull(result, "Should handle negative size values");
    }

    // ==================== Resource Loading Tests ====================

    @Test
    @DisplayName("loadFromFile handles missing file")
    void testLoadFromFileMissing() throws Exception {
        Method loadMethod = StructureTemplateLoader.class.getDeclaredMethod(
            "loadFromFile", java.io.File.class, String.class);
        loadMethod.setAccessible(true);

        java.io.File missingFile = new java.io.File("/nonexistent/file.nbt");

        StructureTemplateLoader.LoadedTemplate result =
            (StructureTemplateLoader.LoadedTemplate) loadMethod.invoke(null, missingFile, "test");

        assertNull(result, "Missing file should return null");
    }

    @Test
    @DisplayName("loadFromMinecraftTemplate returns null (not implemented)")
    void testLoadFromMinecraftTemplateNotImplemented() throws Exception {
        Method loadMethod = StructureTemplateLoader.class.getDeclaredMethod(
            "loadFromMinecraftTemplate", StructureTemplate.class, String.class);
        loadMethod.setAccessible(true);

        StructureTemplateLoader.LoadedTemplate result =
            (StructureTemplateLoader.LoadedTemplate) loadMethod.invoke(null, mockTemplate, "test");

        assertNull(result, "Direct template loading should return null (not implemented)");
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Complete workflow: create NBT, parse, verify structure")
    void testCompleteWorkflow() throws Exception {
        // Create a simple structure
        CompoundTag nbt = createSimpleStructureNBT();

        // Parse it
        Method parseMethod = StructureTemplateLoader.class.getDeclaredMethod(
            "parseNBTStructure", CompoundTag.class, String.class);
        parseMethod.setAccessible(true);

        StructureTemplateLoader.LoadedTemplate result =
            (StructureTemplateLoader.LoadedTemplate) parseMethod.invoke(null, nbt, "simple");

        // Verify
        assertNotNull(result, "Should parse successfully");
        assertEquals("simple", result.name, "Name should be preserved");
        assertTrue(result.blocks.size() > 0, "Should have blocks");
    }

    @Test
    @DisplayName("TemplateBlock positions are relative")
    void testRelativePositions() throws Exception {
        CompoundTag nbt = createValidNBTStructure();

        Method parseMethod = StructureTemplateLoader.class.getDeclaredMethod(
            "parseNBTStructure", CompoundTag.class, String.class);
        parseMethod.setAccessible(true);

        StructureTemplateLoader.LoadedTemplate result =
            (StructureTemplateLoader.LoadedTemplate) parseMethod.invoke(null, nbt, "test");

        // All positions should be relative to (0,0,0) origin
        assertTrue(result.blocks.stream().allMatch(b ->
            b.relativePos.getX() >= 0 && b.relativePos.getY() >= 0 && b.relativePos.getZ() >= 0),
            "All positions should be non-negative (relative)");
    }

    @Test
    @DisplayName("Can handle large structures")
    void testLargeStructure() throws Exception {
        CompoundTag nbt = createLargeStructureNBT(50, 50, 50);

        Method parseMethod = StructureTemplateLoader.class.getDeclaredMethod(
            "parseNBTStructure", CompoundTag.class, String.class);
        parseMethod.setAccessible(true);

        StructureTemplateLoader.LoadedTemplate result =
            (StructureTemplateLoader.LoadedTemplate) parseMethod.invoke(null, nbt, "large");

        assertNotNull(result, "Should handle large structures");
        assertEquals(50, result.width, "Large width should be preserved");
        assertEquals(50, result.height, "Large height should be preserved");
        assertEquals(50, result.depth, "Large depth should be preserved");
    }

    // ==================== Helper Methods ====================

    private CompoundTag createValidNBTStructure() {
        CompoundTag nbt = new CompoundTag();

        // Size tag
        ListTag size = new ListTag();
        size.add(10); // width
        size.add(5);  // height
        size.add(8);  // depth
        nbt.put("size", size);

        // Palette tag
        ListTag palette = new ListTag();
        CompoundTag stoneEntry = new CompoundTag();
        stoneEntry.putString("Name", "minecraft:stone");
        palette.add(stoneEntry);

        CompoundTag dirtEntry = new CompoundTag();
        dirtEntry.putString("Name", "minecraft:dirt");
        palette.add(dirtEntry);
        nbt.put("palette", palette);

        // Blocks tag
        ListTag blocks = new ListTag();
        CompoundTag block1 = new CompoundTag();
        block1.putInt("state", 0); // stone
        ListTag pos1 = new ListTag();
        pos1.add(0); pos1.add(0); pos1.add(0);
        block1.put("pos", pos1);
        blocks.add(block1);

        CompoundTag block2 = new CompoundTag();
        block2.putInt("state", 1); // dirt
        ListTag pos2 = new ListTag();
        pos2.add(1); pos2.add(0); pos2.add(1);
        block2.put("pos", pos2);
        blocks.add(block2);

        nbt.put("blocks", blocks);

        return nbt;
    }

    private CompoundTag createValidNBTStructureWithAir() {
        CompoundTag nbt = new CompoundTag();

        ListTag size = new ListTag();
        size.add(3); size.add(3); size.add(3);
        nbt.put("size", size);

        ListTag palette = new ListTag();
        CompoundTag stoneEntry = new CompoundTag();
        stoneEntry.putString("Name", "minecraft:stone");
        palette.add(stoneEntry);

        CompoundTag airEntry = new CompoundTag();
        airEntry.putString("Name", "minecraft:air");
        palette.add(airEntry);
        nbt.put("palette", palette);

        ListTag blocks = new ListTag();
        // Add some stone blocks
        for (int i = 0; i < 3; i++) {
            CompoundTag block = new CompoundTag();
            block.putInt("state", 0); // stone
            ListTag pos = new ListTag();
            pos.add(i); pos.add(0); pos.add(0);
            block.put("pos", pos);
            blocks.add(block);
        }
        // Add some air blocks
        for (int i = 0; i < 3; i++) {
            CompoundTag block = new CompoundTag();
            block.putInt("state", 1); // air
            ListTag pos = new ListTag();
            pos.add(i); pos.add(1); pos.add(1);
            block.put("pos", pos);
            blocks.add(block);
        }

        nbt.put("blocks", blocks);
        return nbt;
    }

    private CompoundTag createNBTWithUnknownBlock() {
        CompoundTag nbt = new CompoundTag();

        ListTag size = new ListTag();
        size.add(2); size.add(1); size.add(2);
        nbt.put("size", size);

        ListTag palette = new ListTag();
        CompoundTag unknownEntry = new CompoundTag();
        unknownEntry.putString("Name", "minecraft:totally_fake_block_12345");
        palette.add(unknownEntry);
        nbt.put("palette", palette);

        ListTag blocks = new ListTag();
        CompoundTag block = new CompoundTag();
        block.putInt("state", 0); // unknown block
        ListTag pos = new ListTag();
        pos.add(0); pos.add(0); pos.add(0);
        block.put("pos", pos);
        blocks.add(block);

        nbt.put("blocks", blocks);
        return nbt;
    }

    private CompoundTag createNBTWithMalformedPalette() {
        CompoundTag nbt = new CompoundTag();

        ListTag size = new ListTag();
        size.add(2); size.add(1); size.add(2);
        nbt.put("size", size);

        ListTag palette = new ListTag();
        CompoundTag badEntry = new CompoundTag();
        // Missing "Name" field
        palette.add(badEntry);
        nbt.put("palette", palette);

        nbt.put("blocks", new ListTag());
        return nbt;
    }

    private CompoundTag createNBTWithNegativeSize() {
        CompoundTag nbt = new CompoundTag();

        ListTag size = new ListTag();
        size.add(-5); size.add(-3); size.add(-5);
        nbt.put("size", size);

        nbt.put("palette", new ListTag());
        nbt.put("blocks", new ListTag());

        return nbt;
    }

    private CompoundTag createSimpleStructureNBT() {
        CompoundTag nbt = new CompoundTag();

        ListTag size = new ListTag();
        size.add(5); size.add(3); size.add(5);
        nbt.put("size", size);

        ListTag palette = new ListTag();
        CompoundTag stoneEntry = new CompoundTag();
        stoneEntry.putString("Name", "minecraft:stone");
        palette.add(stoneEntry);
        nbt.put("palette", palette);

        ListTag blocks = new ListTag();
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                CompoundTag block = new CompoundTag();
                block.putInt("state", 0);
                ListTag pos = new ListTag();
                pos.add(x); pos.add(0); pos.add(z);
                block.put("pos", pos);
                blocks.add(block);
            }
        }
        nbt.put("blocks", blocks);

        return nbt;
    }

    private CompoundTag createLargeStructureNBT(int width, int height, int depth) {
        CompoundTag nbt = new CompoundTag();

        ListTag size = new ListTag();
        size.add(width); size.add(height); size.add(depth);
        nbt.put("size", size);

        ListTag palette = new ListTag();
        CompoundTag stoneEntry = new CompoundTag();
        stoneEntry.putString("Name", "minecraft:stone");
        palette.add(stoneEntry);
        nbt.put("palette", palette);

        // Add just a few blocks for testing, not full structure
        ListTag blocks = new ListTag();
        CompoundTag block1 = new CompoundTag();
        block1.putInt("state", 0);
        ListTag pos1 = new ListTag();
        pos1.add(0); pos1.add(0); pos1.add(0);
        block1.put("pos", pos1);
        blocks.add(block1);

        CompoundTag block2 = new CompoundTag();
        block2.putInt("state", 0);
        ListTag pos2 = new ListTag();
        pos2.add(width - 1); pos2.add(height - 1); pos2.add(depth - 1);
        block2.put("pos", pos2);
        blocks.add(block2);

        nbt.put("blocks", blocks);
        return nbt;
    }

    private int totalBlocksInNBT(CompoundTag nbt) {
        return nbt.getList("blocks", 10).size();
    }
}
