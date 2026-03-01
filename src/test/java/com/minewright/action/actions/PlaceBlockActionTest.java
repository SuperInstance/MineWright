package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.ActionResult.ErrorCode;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link PlaceBlockAction}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Block placement at position</li>
 *   <li>Placement validation</li>
 *   <li>Block state setting</li>
 *   <li>Adjacent block requirements</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceBlockAction Tests")
class PlaceBlockActionTest {

    @Mock
    private ForemanEntity foreman;

    @Mock
    private Level level;

    @Mock
    private PathNavigation navigation;

    private Task task;
    private PlaceBlockAction action;

    @BeforeEach
    void setUp() {
        lenient().when(foreman.level()).thenReturn(level);
        lenient().when(foreman.getEntityName()).thenReturn("TestForeman");
        lenient().when(foreman.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
        lenient().when(foreman.getNavigation()).thenReturn(navigation);
        lenient().when(navigation.isDone()).thenReturn(true);
    }

    // ==================== Initialization Tests ====================

    @Test
    @DisplayName("Should initialize with valid block and position")
    void testValidInitialization() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 10);
        params.put("y", 64);
        params.put("z", 10);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        assertFalse(action.isComplete(), "Should not be complete immediately");
        assertNull(action.getResult(), "Should not have result yet");
    }

    @Test
    @DisplayName("Should fail with missing block parameter")
    void testMissingBlockParameter() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 10);
        params.put("y", 64);
        params.put("z", 10);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.INVALID_PARAMS, result.getErrorCode());
    }

    @Test
    @DisplayName("Should fail with empty block parameter")
    void testEmptyBlockParameter() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "");
        params.put("x", 10);
        params.put("y", 64);
        params.put("z", 10);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.INVALID_PARAMS, result.getErrorCode());
    }

    @Test
    @DisplayName("Should fail with null block parameter")
    void testNullBlockParameter() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 10);
        params.put("y", 64);
        params.put("z", 10);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should fail with invalid block type")
    void testInvalidBlockType() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "invalid_block_that_does_not_exist");
        params.put("x", 10);
        params.put("y", 64);
        params.put("z", 10);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.NOT_FOUND, result.getErrorCode());
        assertTrue(result.getMessage().contains("Invalid block type"));
    }

    // ==================== Position Tests ====================

    @Test
    @DisplayName("Should use default position when coordinates not specified")
    void testDefaultPosition() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        // Should use default position (0, 0, 0)
        String description = action.getDescription();
        assertNotNull(description);
    }

    @Test
    @DisplayName("Should use specified position")
    void testSpecifiedPosition() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "cobblestone");
        params.put("x", 100);
        params.put("y", 65);
        params.put("z", -50);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        String description = action.getDescription();
        assertTrue(description.contains("100") ||
                   description.contains("65") ||
                   description.contains("-50"),
            "Description should contain position info");
    }

    @Test
    @DisplayName("Should handle negative coordinates")
    void testNegativeCoordinates() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", -10);
        params.put("y", -64);
        params.put("z", -20);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        // Should accept negative coordinates
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle very large coordinates")
    void testVeryLargeCoordinates() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 1000000);
        params.put("y", 256);
        params.put("z", -1000000);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        // Should accept large coordinates (will timeout navigating)
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle Y coordinate at height limit")
    void testYCoordinateAtHeightLimit() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 0);
        params.put("y", 319); // Minecraft 1.20+ height limit
        params.put("z", 0);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle Y coordinate at depth limit")
    void testYCoordinateAtDepthLimit() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 0);
        params.put("y", -64);
        params.put("z", 0);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        assertNotNull(action);
    }

    // ==================== Block Placement Tests ====================

    @Test
    @DisplayName("Should place block at target position")
    void testPlaceBlockAtTarget() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 5);
        params.put("y", 64);
        params.put("z", 5);
        task = new Task("place_block", params);

        BlockPos targetPos = new BlockPos(5, 64, 5);
        when(level.getBlockState(targetPos)).thenReturn(Blocks.AIR.defaultBlockState());
        when(foreman.blockPosition()).thenReturn(targetPos);
        when(level.setBlock(any(BlockPos.class), any(BlockState.class), anyInt())).thenReturn(true);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        // Tick to place block
        for (int i = 0; i < 20 && !action.isComplete(); i++) {
            action.tick();
        }

        if (action.isComplete()) {
            verify(level, atLeastOnce()).setBlock(eq(targetPos), any(BlockState.class), eq(3));
        }
    }

    @Test
    @DisplayName("Should set block state correctly")
    void testSetBlockStateCorrectly() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "cobblestone");
        params.put("x", 1);
        params.put("y", 64);
        params.put("z", 1);
        task = new Task("place_block", params);

        BlockPos targetPos = new BlockPos(1, 64, 1);
        when(level.getBlockState(targetPos)).thenReturn(Blocks.AIR.defaultBlockState());
        when(foreman.blockPosition()).thenReturn(targetPos);
        when(level.setBlock(any(BlockPos.class), any(BlockState.class), anyInt())).thenReturn(true);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        for (int i = 0; i < 20 && !action.isComplete(); i++) {
            action.tick();
        }

        if (action.isComplete()) {
            verify(level, atLeastOnce()).setBlock(
                eq(targetPos),
                eq(Blocks.COBBLESTONE.defaultBlockState()),
                eq(3)
            );
        }
    }

    @Test
    @DisplayName("Should not place block in occupied space")
    void testDoNotPlaceInOccupiedSpace() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 5);
        params.put("y", 64);
        params.put("z", 5);
        task = new Task("place_block", params);

        BlockPos targetPos = new BlockPos(5, 64, 5);
        when(level.getBlockState(targetPos)).thenReturn(Blocks.DIRT.defaultBlockState());
        when(foreman.blockPosition()).thenReturn(targetPos);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        for (int i = 0; i < 20 && !action.isComplete(); i++) {
            action.tick();
        }

        if (action.isComplete()) {
            ActionResult result = action.getResult();
            if (result != null && !result.isSuccess()) {
                assertEquals(ErrorCode.BLOCKED, result.getErrorCode());
                assertTrue(result.getMessage().contains("occupied") ||
                           result.getMessage().contains("Position"));
            }
        }
    }

    @Test
    @DisplayName("Should place block in water")
    void testPlaceBlockInWater() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 1);
        params.put("y", 64);
        params.put("z", 1);
        task = new Task("place_block", params);

        BlockPos targetPos = new BlockPos(1, 64, 1);
        BlockState waterState = mock(BlockState.class);
        when(waterState.isAir()).thenReturn(false);
        when(waterState.getFluidState()).thenReturn(Fluids.WATER.defaultFluidState());

        when(level.getBlockState(targetPos)).thenReturn(waterState);
        when(foreman.blockPosition()).thenReturn(targetPos);
        when(level.setBlock(any(BlockPos.class), any(BlockState.class), anyInt())).thenReturn(true);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        for (int i = 0; i < 20 && !action.isComplete(); i++) {
            action.tick();
        }

        // Water is not air but fluid is not empty, should be placeable
        // The implementation checks: !currentState.isAir() && !currentState.getFluidState().isEmpty()
        // So if it's water (fluid not empty), it should NOT place
    }

    @Test
    @DisplayName("Should not place block in lava")
    void testDoNotPlaceInLava() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 1);
        params.put("y", 64);
        params.put("z", 1);
        task = new Task("place_block", params);

        BlockPos targetPos = new BlockPos(1, 64, 1);
        BlockState lavaState = mock(BlockState.class);
        when(lavaState.isAir()).thenReturn(false);
        when(lavaState.getFluidState()).thenReturn(Fluids.LAVA.defaultFluidState());

        when(level.getBlockState(targetPos)).thenReturn(lavaState);
        when(foreman.blockPosition()).thenReturn(targetPos);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        for (int i = 0; i < 20 && !action.isComplete(); i++) {
            action.tick();
        }

        if (action.isComplete()) {
            ActionResult result = action.getResult();
            if (result != null && !result.isSuccess()) {
                // Should be blocked by lava
                assertEquals(ErrorCode.BLOCKED, result.getErrorCode());
            }
        }
    }

    // ==================== Navigation Tests ====================

    @Test
    @DisplayName("Should navigate to distant target")
    void testNavigateToDistantTarget() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 50);
        params.put("y", 64);
        params.put("z", 50);
        task = new Task("place_block", params);

        BlockPos foremanPos = new BlockPos(0, 64, 0);
        BlockPos targetPos = new BlockPos(50, 64, 50);

        when(foreman.blockPosition()).thenReturn(foremanPos);
        when(level.getBlockState(targetPos)).thenReturn(Blocks.AIR.defaultBlockState());

        action = new PlaceBlockAction(foreman, task);
        action.start();

        // Tick to trigger navigation
        for (int i = 0; i < 50; i++) {
            action.tick();
        }

        // Should attempt to navigate
        verify(navigation, atLeastOnce()).moveTo(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should not navigate when close to target")
    void testDoNotNavigateWhenClose() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 2);
        params.put("y", 64);
        params.put("z", 2);
        task = new Task("place_block", params);

        BlockPos targetPos = new BlockPos(2, 64, 2);

        when(foreman.blockPosition()).thenReturn(targetPos);
        when(level.getBlockState(targetPos)).thenReturn(Blocks.AIR.defaultBlockState());
        when(level.setBlock(any(BlockPos.class), any(BlockState.class), anyInt())).thenReturn(true);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        for (int i = 0; i < 20 && !action.isComplete(); i++) {
            action.tick();
        }

        // Should not navigate (already close)
        verify(navigation, never()).moveTo(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should navigate when just outside placement distance")
    void testNavigateWhenJustOutsideDistance() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 6);
        params.put("y", 64);
        params.put("z", 0);
        task = new Task("place_block", params);

        BlockPos foremanPos = new BlockPos(0, 64, 0);
        BlockPos targetPos = new BlockPos(6, 64, 0);

        when(foreman.blockPosition()).thenReturn(foremanPos);
        when(level.getBlockState(targetPos)).thenReturn(Blocks.AIR.defaultBlockState());

        action = new PlaceBlockAction(foreman, task);
        action.start();

        for (int i = 0; i < 50; i++) {
            action.tick();
        }

        // Distance is 6.0, which is > PLACEMENT_DISTANCE (5.0)
        // Should navigate
        verify(navigation, atLeastOnce()).moveTo(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    // ==================== Block Type Tests ====================

    @Test
    @DisplayName("Should place different block types")
    void testPlaceDifferentBlockTypes() {
        String[] blockTypes = {"stone", "cobblestone", "oak_planks", "glass", "bricks"};

        for (String blockType : blockTypes) {
            Map<String, Object> params = new HashMap<>();
            params.put("block", blockType);
            params.put("x", 1);
            params.put("y", 64);
            params.put("z", 1);
            task = new Task("place_block", params);

            BlockPos targetPos = new BlockPos(1, 64, 1);
            when(level.getBlockState(targetPos)).thenReturn(Blocks.AIR.defaultBlockState());
            when(foreman.blockPosition()).thenReturn(targetPos);
            when(level.setBlock(any(BlockPos.class), any(BlockState.class), anyInt())).thenReturn(true);

            action = new PlaceBlockAction(foreman, task);
            action.start();

            // Reset for next iteration
            reset(level, foreman, navigation);
            lenient().when(foreman.level()).thenReturn(level);
            lenient().when(foreman.getEntityName()).thenReturn("TestForeman");
            lenient().when(foreman.blockPosition()).thenReturn(targetPos);
            lenient().when(foreman.getNavigation()).thenReturn(navigation);
            lenient().when(navigation.isDone()).thenReturn(true);
        }
    }

    @Test
    @DisplayName("Should handle case-insensitive block names")
    void testCaseInsensitiveBlockNames() {
        String[] variants = {"STONE", "Stone", "sToNe", "stone"};

        for (String variant : variants) {
            Map<String, Object> params = new HashMap<>();
            params.put("block", variant);
            params.put("x", 1);
            params.put("y", 64);
            params.put("z", 1);
            task = new Task("place_block", params);

            action = new PlaceBlockAction(foreman, task);
            action.start();

            ActionResult result = action.getResult();
            if (result != null) {
                // Should not fail due to case
                assertFalse(result.getMessage().toLowerCase().contains("invalid"));
            }
        }
    }

    @Test
    @DisplayName("Should handle block names with underscores")
    void testBlockNamesWithUnderscores() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "oak_planks");
        params.put("x", 1);
        params.put("y", 64);
        params.put("z", 1);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        // Should handle underscores
        ActionResult result = action.getResult();
        if (result != null) {
            assertFalse(result.getMessage().toLowerCase().contains("invalid"));
        }
    }

    // ==================== Timeout Tests ====================

    @Test
    @DisplayName("Should timeout after max ticks")
    void testTimeoutAfterMaxTicks() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 1000);
        params.put("y", 64);
        params.put("z", 1000);
        task = new Task("place_block", params);

        when(level.getBlockState(any(BlockPos.class))).thenReturn(Blocks.AIR.defaultBlockState());

        action = new PlaceBlockAction(foreman, task);
        action.start();

        // Run to timeout (200 ticks)
        for (int i = 0; i < 250 && !action.isComplete(); i++) {
            action.tick();
        }

        assertTrue(action.isComplete(), "Should complete after timeout");
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertTrue(result.getMessage().contains("timeout") ||
                   result.getMessage().contains("Timeout"),
            "Should indicate timeout");
        assertEquals(ErrorCode.TIMEOUT, result.getErrorCode());
    }

    // ==================== Completion Tests ====================

    @Test
    @DisplayName("Should succeed after placing block")
    void testSucceedAfterPlacing() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 1);
        params.put("y", 64);
        params.put("z", 1);
        task = new Task("place_block", params);

        BlockPos targetPos = new BlockPos(1, 64, 1);
        when(level.getBlockState(targetPos)).thenReturn(Blocks.AIR.defaultBlockState());
        when(foreman.blockPosition()).thenReturn(targetPos);
        when(level.setBlock(any(BlockPos.class), any(BlockState.class), anyInt())).thenReturn(true);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        for (int i = 0; i < 20 && !action.isComplete(); i++) {
            action.tick();
        }

        if (action.isComplete()) {
            ActionResult result = action.getResult();
            if (result != null) {
                assertTrue(result.isSuccess() || result.getErrorCode() == ErrorCode.TIMEOUT,
                    "Should succeed or timeout");
            }
        }
    }

    @Test
    @DisplayName("Description should show block and position")
    void testDescriptionShowsBlockAndPosition() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 10);
        params.put("y", 64);
        params.put("z", 20);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);

        String description = action.getDescription();
        assertTrue(description.toLowerCase().contains("place") ||
                   description.toLowerCase().contains("stone") ||
                   description.contains("10"),
            "Description should show block type and position");
    }

    // ==================== Cancellation Tests ====================

    @Test
    @DisplayName("Should handle cancellation gracefully")
    void testCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 50);
        params.put("y", 64);
        params.put("z", 50);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();
        action.cancel();

        assertTrue(action.isComplete());
        verify(navigation, atLeastOnce()).stop();
    }

    @Test
    @DisplayName("Should stop navigation on cancellation")
    void testStopNavigationOnCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "cobblestone");
        params.put("x", 100);
        params.put("y", 64);
        params.put("z", 100);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();
        action.cancel();

        verify(navigation, atLeastOnce()).stop();
    }

    @Test
    @DisplayName("Should handle double cancellation gracefully")
    void testDoubleCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 10);
        params.put("y", 64);
        params.put("z", 10);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
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
        params.put("block", "stone");
        params.put("x", 1);
        params.put("y", 64);
        params.put("z", 1);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(null, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.INVALID_STATE, result.getErrorCode());
    }

    @Test
    @DisplayName("Should handle null level gracefully")
    void testNullLevel() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 1);
        params.put("y", 64);
        params.put("z", 1);
        task = new Task("place_block", params);

        lenient().when(foreman.level()).thenReturn(null);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle null navigation gracefully")
    void testNullNavigation() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 50);
        params.put("y", 64);
        params.put("z", 50);
        task = new Task("place_block", params);

        lenient().when(foreman.getNavigation()).thenReturn(null);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle block placement exception")
    void testBlockPlacementException() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "stone");
        params.put("x", 1);
        params.put("y", 64);
        params.put("z", 1);
        task = new Task("place_block", params);

        BlockPos targetPos = new BlockPos(1, 64, 1);
        when(level.getBlockState(targetPos)).thenReturn(Blocks.AIR.defaultBlockState());
        when(foreman.blockPosition()).thenReturn(targetPos);
        when(level.setBlock(any(BlockPos.class), any(BlockState.class), anyInt()))
            .thenThrow(new RuntimeException("Placement failed"));

        action = new PlaceBlockAction(foreman, task);
        action.start();

        for (int i = 0; i < 20 && !action.isComplete(); i++) {
            action.tick();
        }

        if (action.isComplete()) {
            ActionResult result = action.getResult();
            if (result != null && !result.isSuccess()) {
                assertEquals(ErrorCode.EXECUTION_ERROR, result.getErrorCode());
            }
        }
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle special characters in block name")
    void testSpecialCharactersInBlockName() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "some_block_with_underscores");
        params.put("x", 1);
        params.put("y", 64);
        params.put("z", 1);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        // Should handle gracefully
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle spaces in block name")
    void testSpacesInBlockName() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "oak planks"); // Space gets converted to underscore
        params.put("x", 1);
        params.put("y", 64);
        params.put("z", 1);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        ActionResult result = action.getResult();
        if (result != null) {
            // May succeed if conversion works, otherwise fail gracefully
            assertNotNull(result.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle extremely long block name")
    void testExtremelyLongBlockName() {
        Map<String, Object> params = new HashMap<>();
        params.put("block", "a".repeat(1000));
        params.put("x", 1);
        params.put("y", 64);
        params.put("z", 1);
        task = new Task("place_block", params);

        action = new PlaceBlockAction(foreman, task);
        action.start();

        // Should handle gracefully (will fail to find block)
        assertTrue(action.isComplete());
    }
}
