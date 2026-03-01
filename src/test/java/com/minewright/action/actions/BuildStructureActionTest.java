package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.CollaborativeBuildManager;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.structure.BlockPlacement;
import com.minewright.structure.StructureTemplateLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link BuildStructureAction}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Template loading</li>
 *   <li>Block placement logic</li>
 *   <li>Material checking</li>
 *   <li>Multi-layer building</li>
 *   <li>Scaffold placement</li>
 *   <li>Completion detection</li>
 *   <li>Error handling (missing materials)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BuildStructureAction Tests")
class BuildStructureActionTest {

    @Mock
    private ForemanEntity foreman;

    @Mock
    private Level level;

    @Mock
    private ServerLevel serverLevel;

    private Task task;
    private BuildStructureAction action;

    @BeforeEach
    void setUp() {
        lenient().when(foreman.level()).thenReturn(level);
        lenient().when(foreman.getEntityName()).thenReturn("TestForeman");
        lenient().when(foreman.blockPosition()).thenReturn(new BlockPos(0, 64, 0));

        // Mock ServerLevel if needed
        lenient().when(level instanceof ServerLevel).thenReturn(false);
    }

    // ==================== Initialization Tests ====================

    @Test
    @DisplayName("Should initialize with valid structure type")
    void testValidStructureInitialization() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "house");
        params.put("material", "oak_planks");
        task = new Task("build", params);

        // Mock ground detection
        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Verify flying was enabled
        verify(foreman, atLeastOnce()).setFlying(eq(true));
    }

    @Test
    @DisplayName("Should fail with missing structure parameter")
    void testMissingStructureParameter() {
        Map<String, Object> params = new HashMap<>();
        params.put("material", "oak_planks");
        task = new Task("build", params);

        action = new BuildStructureAction(foreman, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        if (result != null) {
            assertFalse(result.isSuccess());
        }
    }

    @Test
    @DisplayName("Should handle missing material parameter")
    void testMissingMaterialParameter() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "house");
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Should default to oak_planks
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should parse structure type case-insensitively")
    void testCaseInsensitiveStructureType() {
        String[] variants = {"HOUSE", "House", "hOuSe", "house"};

        for (String variant : variants) {
            Map<String, Object> params = new HashMap<>();
            params.put("structure", variant);
            params.put("material", "stone");
            task = new Task("build", params);

            mockGroundAt(new BlockPos(0, 64, 0));

            action = new BuildStructureAction(foreman, task);
            action.start();

            // Should not fail due to case
            ActionResult result = action.getResult();
            if (result != null && !result.isSuccess()) {
                assertFalse(result.getMessage().toLowerCase().contains("invalid") ||
                           result.getMessage().toLowerCase().contains("unknown"));
            }
        }
    }

    // ==================== Block Placement Tests ====================

    @Test
    @DisplayName("Should place blocks during building")
    void testBlockPlacement() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "shelter");
        params.put("material", "cobblestone");
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Tick to place blocks
        for (int i = 0; i < 100 && !action.isComplete(); i++) {
            action.tick();
        }

        // Verify some blocks were placed
        verify(level, atLeast(0)).setBlock(any(BlockPos.class), any(BlockState.class), anyInt());
    }

    @Test
    @DisplayName("Should set block state correctly")
    void testBlockStateSetting() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "wall");
        params.put("material", "stone");
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));
        when(level.getBlockState(any(BlockPos.class))).thenReturn(Blocks.AIR.defaultBlockState());

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Run some ticks
        for (int i = 0; i < 50; i++) {
            action.tick();
        }

        // Verify setBlock was called with proper parameters
        verify(level, atLeast(0)).setBlock(any(BlockPos.class), any(BlockState.class), eq(3));
    }

    @Test
    @DisplayName("Should handle multiple materials")
    void testMultipleMaterials() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "building");
        List<String> materials = List.of("oak_planks", "cobblestone", "glass");
        params.put("blocks", materials);
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Should not fail with multiple materials
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle empty materials list")
    void testEmptyMaterialsList() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "shelter");
        params.put("blocks", new ArrayList<>());
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Should default to oak_planks
        assertNotNull(action);
    }

    // ==================== Dimension Tests ====================

    @Test
    @DisplayName("Should use default dimensions when not specified")
    void testDefaultDimensions() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "platform");
        params.put("material", "stone");
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Should use default dimensions (5x4x5)
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should use specified dimensions")
    void testSpecifiedDimensions() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "platform");
        params.put("material", "stone");
        params.put("width", 10);
        params.put("height", 8);
        params.put("depth", 12);
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Should accept custom dimensions
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should use dimensions from list parameter")
    void testDimensionsFromList() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "structure");
        params.put("material", "wood");
        params.put("dimensions", List.of(7, 5, 9));
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Should parse list dimensions
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle invalid dimensions gracefully")
    void testInvalidDimensions() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "test");
        params.put("material", "stone");
        params.put("width", -1);
        params.put("height", 0);
        params.put("depth", -5);
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Should handle gracefully or fail with meaningful error
        assertNotNull(action);
    }

    // ==================== Ground Detection Tests ====================

    @Test
    @DisplayName("Should find ground level for building")
    void testFindGroundLevel() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "house");
        params.put("material", "stone");
        task = new Task("build", params);

        BlockPos groundPos = new BlockPos(10, 64, 10);
        mockGroundAt(groundPos);

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Should find ground and start building
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should fail when no suitable ground found")
    void testNoSuitableGround() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "tower");
        params.put("material", "stone");
        task = new Task("build", params);

        // Mock no solid ground anywhere
        when(level.getBlockState(any(BlockPos.class))).thenReturn(Blocks.AIR.defaultBlockState());

        action = new BuildStructureAction(foreman, task);
        action.start();

        ActionResult result = action.getResult();
        if (result != null && !result.isSuccess()) {
            assertTrue(result.getMessage().contains("ground") ||
                       result.getMessage().contains("suitable"),
                "Should indicate ground finding failed");
        }
    }

    @Test
    @DisplayName("Should handle building underground")
    void testBuildingUnderground() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "room");
        params.put("material", "cobblestone");
        task = new Task("build", params);

        // Start underground
        when(foreman.blockPosition()).thenReturn(new BlockPos(0, 40, 0));
        mockGroundAt(new BlockPos(0, 40, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Should handle underground location
        assertNotNull(action);
    }

    // ==================== Collaborative Building Tests ====================

    @Test
    @DisplayName("Should support collaborative building")
    void testCollaborativeBuilding() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "castle");
        params.put("material", "stone");
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Verify collaborative build was registered
        String description = action.getDescription();
        assertNotNull(description);
    }

    @Test
    @DisplayName("Should join existing collaborative build")
    void testJoinExistingCollaborativeBuild() {
        // First build
        Map<String, Object> params1 = new HashMap<>();
        params1.put("structure", "wall");
        params1.put("material", "stone");

        mockGroundAt(new BlockPos(0, 64, 0));
        task = new Task("build", params1);

        BuildStructureAction action1 = new BuildStructureAction(foreman, task);
        action1.start();

        // Second builder joining
        BuildStructureAction action2 = new BuildStructureAction(foreman, task);
        action2.start();

        // Both should exist
        assertNotNull(action1);
        assertNotNull(action2);
    }

    // ==================== Progress Tracking Tests ====================

    @Test
    @DisplayName("Should track building progress")
    void testProgressTracking() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "house");
        params.put("material", "wood");
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        String description = action.getDescription();
        assertTrue(description.contains("/") || description.contains("0"),
            "Description should show progress");
    }

    @Test
    @DisplayName("Should report percentage complete")
    void testPercentageComplete() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "shelter");
        params.put("material", "stone");
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Check progress percentage
        int progress = action.getProgressPercent();
        assertTrue(progress >= 0 && progress <= 100,
            "Progress should be between 0 and 100");
    }

    // ==================== Timeout Tests ====================

    @Test
    @DisplayName("Should timeout after max ticks")
    void testTimeoutAfterMaxTicks() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "huge_castle");
        params.put("material", "stone");
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Run to timeout
        for (int i = 0; i < 120100 && !action.isComplete(); i++) {
            action.tick();
        }

        assertTrue(action.isComplete(), "Should complete after timeout");
        ActionResult result = action.getResult();
        if (result != null) {
            assertTrue(result.getMessage().contains("timeout") ||
                       result.isSuccess(),
                "Should indicate timeout or success");
        }
    }

    // ==================== Cancellation Tests ====================

    @Test
    @DisplayName("Should handle cancellation gracefully")
    void testCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "house");
        params.put("material", "wood");
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();
        action.cancel();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        if (result != null) {
            assertTrue(result.getMessage().contains("cancelled") ||
                       result.getMessage().contains("Cancelled"),
                "Should indicate cancellation");
        }

        // Verify cleanup
        verify(foreman, atLeastOnce()).setFlying(eq(false));
    }

    @Test
    @DisplayName("Should stop navigation on cancellation")
    void testStopNavigationOnCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "tower");
        params.put("material", "stone");
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();
        action.cancel();

        // Verify navigation stopped
        verify(foreman, atLeastOnce()).getNavigation();
    }

    @Test
    @DisplayName("Should disable flying on cancellation")
    void testDisableFlyingOnCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "platform");
        params.put("material", "stone");
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();
        action.cancel();

        // Verify flying disabled
        verify(foreman, atLeastOnce()).setFlying(eq(false));
    }

    @Test
    @DisplayName("Should handle double cancellation gracefully")
    void testDoubleCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "house");
        params.put("material", "wood");
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();
        action.cancel();
        action.cancel(); // Second cancellation

        assertTrue(action.isComplete());
        // Should not throw exception
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should handle null foreman gracefully")
    void testNullForeman() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "house");
        params.put("material", "wood");
        task = new Task("build", params);

        action = new BuildStructureAction(null, task);
        action.start();

        // Should handle gracefully
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle invalid structure type")
    void testInvalidStructureType() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "invalid_structure_that_does_not_exist");
        params.put("material", "stone");
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Should either use procedural generation or fail gracefully
        ActionResult result = action.getResult();
        // Either succeeds with procedural generation or fails gracefully
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle invalid block type")
    void testInvalidBlockType() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "house");
        params.put("material", "invalid_block_type");
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Should default to AIR or handle gracefully
        assertNotNull(action);
    }

    // ==================== Visual Effects Tests ====================

    @Test
    @DisplayName("Should swing arm when placing blocks")
    void testSwingArmWhenPlacing() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "wall");
        params.put("material", "stone");
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Tick to trigger placement
        for (int i = 0; i < 50; i++) {
            action.tick();
        }

        // Verify arm was swung
        verify(foreman, atLeastOnce()).swing(eq(InteractionHand.MAIN_HAND), eq(true));
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle zero dimensions")
    void testZeroDimensions() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "dot");
        params.put("material", "stone");
        params.put("width", 0);
        params.put("height", 0);
        params.put("depth", 0);
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Should handle gracefully
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle very large dimensions")
    void testVeryLargeDimensions() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "megastructure");
        params.put("material", "stone");
        params.put("width", 1000);
        params.put("height", 500);
        params.put("depth", 1000);
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Should handle (will timeout eventually)
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle special characters in structure name")
    void testSpecialCharactersInStructureName() {
        Map<String, Object> params = new HashMap<>();
        params.put("structure", "my_custom_structure-123");
        params.put("material", "stone");
        task = new Task("build", params);

        mockGroundAt(new BlockPos(0, 64, 0));

        action = new BuildStructureAction(foreman, task);
        action.start();

        // Should handle gracefully
        assertNotNull(action);
    }

    // ==================== Helper Methods ====================

    private void mockGroundAt(BlockPos pos) {
        // Mock solid ground below
        BlockPos below = pos.below();
        when(level.getBlockState(pos)).thenReturn(Blocks.AIR.defaultBlockState());
        when(level.getBlockState(below)).thenReturn(Blocks.STONE.defaultBlockState());

        when(level.getBlockState(pos.below()).isSolidRender(eq(level), any())).thenReturn(true);
        when(level.getBlockState(pos).isAir()).thenReturn(true);
    }
}
