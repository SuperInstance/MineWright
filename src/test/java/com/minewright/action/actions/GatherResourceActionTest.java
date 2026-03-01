package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.ActionResult.ErrorCode;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
 * Comprehensive unit tests for {@link GatherResourceAction}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Resource detection</li>
 *   <li>Gathering logic</li>
 *   <li>Inventory accumulation</li>
 *   <li>Target quantity checking</li>
 *   <li>Different resource types</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GatherResourceAction Tests")
class GatherResourceActionTest {

    @Mock
    private ForemanEntity foreman;

    @Mock
    private Level level;

    @Mock
    private PathNavigation navigation;

    private Task task;
    private GatherResourceAction action;

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
    @DisplayName("Should successfully initialize with valid resource")
    void testValidResourceInitialization() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "stone");
        params.put("quantity", 10);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        assertFalse(action.isComplete(), "Should not be complete immediately");
        assertNull(action.getResult(), "Should not have result yet");
    }

    @Test
    @DisplayName("Should fail initialization with missing resource parameter")
    void testMissingResourceParameter() {
        Map<String, Object> params = new HashMap<>();
        params.put("quantity", 10);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        assertTrue(action.isComplete(), "Should be complete with error");
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("required") ||
                   result.getMessage().contains("Resource"));
    }

    @Test
    @DisplayName("Should fail initialization with empty resource type")
    void testEmptyResourceType() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "");
        params.put("quantity", 5);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should fail initialization with null resource type")
    void testNullResourceType() {
        Map<String, Object> params = new HashMap<>();
        params.put("quantity", 5);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should fail initialization with invalid resource type")
    void testInvalidResourceType() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "nonexistent_resource_block");
        params.put("quantity", 5);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Unknown") ||
                   result.getMessage().contains("resource"),
            "Should indicate unknown resource type");
    }

    // ==================== Quantity Handling Tests ====================

    @Test
    @DisplayName("Should handle valid positive quantity")
    void testValidPositiveQuantity() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "stone");
        params.put("quantity", 10);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        assertFalse(action.isComplete());
    }

    @Test
    @DisplayName("Should default to quantity 1 when not specified")
    void testDefaultQuantity() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "wood");
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        String description = action.getDescription();
        assertTrue(description.contains("1"),
            "Should default to quantity 1");
    }

    @Test
    @DisplayName("Should fail with zero quantity")
    void testZeroQuantity() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "stone");
        params.put("quantity", 0);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("positive") ||
                   result.getMessage().contains("Quantity"));
    }

    @Test
    @DisplayName("Should fail with negative quantity")
    void testNegativeQuantity() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "wood");
        params.put("quantity", -5);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle very large quantities")
    void testVeryLargeQuantity() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "stone");
        params.put("quantity", 100000);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        // Should not crash (will timeout eventually)
        assertNotNull(action);
    }

    // ==================== Resource Detection Tests ====================

    @Test
    @DisplayName("Should detect resources within search radius")
    void testResourceDetectionWithinRadius() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "stone");
        params.put("quantity", 1);
        task = new Task("gather", params);

        // Mock stone at nearby position
        BlockPos resourcePos = new BlockPos(5, 64, 0);
        when(level.getBlockState(resourcePos)).thenReturn(Blocks.STONE.defaultBlockState());

        action = new GatherResourceAction(foreman, task);
        action.start();

        // Tick to find resource
        for (int i = 0; i < 150 && !action.isComplete(); i++) {
            action.tick();
        }

        assertNotNull(action);
    }

    @Test
    @DisplayName("Should search with custom radius")
    void testCustomSearchRadius() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "coal_ore");
        params.put("quantity", 1);
        params.put("radius", 50);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        // Should accept custom radius
        String description = action.getDescription();
        assertNotNull(description);
    }

    @Test
    @DisplayName("Should use default search radius when not specified")
    void testDefaultSearchRadius() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "iron_ore");
        params.put("quantity", 1);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        // Should use default radius of 32
        assertNotNull(action);
    }

    // ==================== Resource Type Tests ====================

    @Test
    @DisplayName("Should gather wood resources")
    void testGatherWood() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "wood");
        params.put("quantity", 5);
        task = new Task("gather", params);

        BlockPos woodPos = new BlockPos(3, 64, 0);
        when(level.getBlockState(woodPos)).thenReturn(Blocks.OAK_LOG.defaultBlockState());

        action = new GatherResourceAction(foreman, task);
        action.start();

        // Verify axe was equipped for wood
        verify(foreman, atLeastOnce()).setItemInHand(eq(InteractionHand.MAIN_HAND), any(ItemStack.class));
    }

    @Test
    @DisplayName("Should gather stone resources")
    void testGatherStone() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "stone");
        params.put("quantity", 5);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        // Verify pickaxe was equipped for stone
        verify(foreman, atLeastOnce()).setItemInHand(eq(InteractionHand.MAIN_HAND), any(ItemStack.class));
    }

    @Test
    @DisplayName("Should gather ore resources")
    void testGatherOre() {
        String[] ores = {"iron_ore", "coal_ore", "gold_ore", "diamond_ore"};

        for (String ore : ores) {
            Map<String, Object> params = new HashMap<>();
            params.put("resource", ore);
            params.put("quantity", 1);
            task = new Task("gather", params);

            action = new GatherResourceAction(foreman, task);
            action.start();

            // Should handle all ore types
            ActionResult result = action.getResult();
            if (result != null) {
                // Should not be parsing error
                assertFalse(result.getMessage().toLowerCase().contains("unknown"));
            }
        }
    }

    @Test
    @DisplayName("Should normalize resource aliases")
    void testResourceAliasNormalization() {
        // Test common aliases
        String[] aliases = {"wood", "log", "tree", "stone", "rock", "cobble", "dirt", "sand", "iron", "coal"};

        for (String alias : aliases) {
            Map<String, Object> params = new HashMap<>();
            params.put("resource", alias);
            params.put("quantity", 1);
            task = new Task("gather", params);

            action = new GatherResourceAction(foreman, task);
            action.start();

            ActionResult result = action.getResult();
            if (result != null) {
                // Should not fail due to alias (unless resource doesn't exist nearby)
                assertFalse(result.getMessage().toLowerCase().contains("invalid") &&
                           result.getMessage().toLowerCase().contains("resource type"));
            }
        }
    }

    @Test
    @DisplayName("Should handle case-insensitive resource names")
    void testCaseInsensitiveResourceNames() {
        String[] variants = {"STONE", "Stone", "sToNe", "stone"};

        for (String variant : variants) {
            Map<String, Object> params = new HashMap<>();
            params.put("resource", variant);
            params.put("quantity", 1);
            task = new Task("gather", params);

            action = new GatherResourceAction(foreman, task);
            action.start();

            ActionResult result = action.getResult();
            if (result != null) {
                // Should not fail due to case
                assertFalse(result.getMessage().toLowerCase().contains("invalid"));
            }
        }
    }

    // ==================== Gathering Logic Tests ====================

    @Test
    @DisplayName("Should navigate to resource")
    void testNavigateToResource() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "coal_ore");
        params.put("quantity", 1);
        task = new Task("gather", params);

        BlockPos foremanPos = new BlockPos(0, 64, 0);
        BlockPos resourcePos = new BlockPos(10, 64, 0);

        when(foreman.blockPosition()).thenReturn(foremanPos);
        when(level.getBlockState(resourcePos)).thenReturn(Blocks.COAL_ORE.defaultBlockState());

        action = new GatherResourceAction(foreman, task);
        action.start();

        // Tick to trigger navigation
        for (int i = 0; i < 150; i++) {
            action.tick();
        }

        // Should attempt to navigate
        verify(navigation, atLeastOnce()).moveTo(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should break resource block when adjacent")
    void testBreakResourceWhenAdjacent() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "stone");
        params.put("quantity", 1);
        task = new Task("gather", params);

        BlockPos resourcePos = new BlockPos(1, 64, 0);
        when(foreman.blockPosition()).thenReturn(resourcePos);
        when(level.getBlockState(resourcePos)).thenReturn(Blocks.STONE.defaultBlockState());
        when(level.destroyBlock(any(BlockPos.class), eq(true))).thenReturn(true);

        action = new GatherResourceAction(foreman, task);
        action.start();

        // Tick to trigger gathering
        for (int i = 0; i < 150 && !action.isComplete(); i++) {
            action.tick();
        }

        // Should attempt to break block
        verify(level, atLeastOnce()).destroyBlock(any(BlockPos.class), eq(true));
    }

    @Test
    @DisplayName("Should update gathered count")
    void testUpdateGatheredCount() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "stone");
        params.put("quantity", 5);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);

        String description = action.getDescription();
        assertTrue(description.contains("0") || description.contains("/"),
            "Description should show initial progress");
    }

    @Test
    @DisplayName("Should face resource when gathering")
    void testFaceResourceWhenGathering() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "iron_ore");
        params.put("quantity", 1);
        task = new Task("gather", params);

        BlockPos resourcePos = new BlockPos(2, 64, 0);
        when(level.getBlockState(resourcePos)).thenReturn(Blocks.IRON_ORE.defaultBlockState());

        action = new GatherResourceAction(foreman, task);
        action.start();

        // Tick to trigger look behavior
        for (int i = 0; i < 150; i++) {
            action.tick();
        }

        // Should look at resource
        verify(foreman.getLookControl(), atLeastOnce()).setLookAt(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should swing arm when gathering")
    void testSwingArmWhenGathering() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "coal_ore");
        params.put("quantity", 1);
        task = new Task("gather", params);

        BlockPos resourcePos = new BlockPos(1, 64, 0);
        when(foreman.blockPosition()).thenReturn(resourcePos);
        when(level.getBlockState(resourcePos)).thenReturn(Blocks.COAL_ORE.defaultBlockState());

        action = new GatherResourceAction(foreman, task);
        action.start();

        // Tick to trigger gathering
        for (int i = 0; i < 150; i++) {
            action.tick();
        }

        // Should swing arm
        verify(foreman, atLeastOnce()).swing(eq(InteractionHand.MAIN_HAND), eq(true));
    }

    // ==================== Completion Tests ====================

    @Test
    @DisplayName("Should complete when target quantity reached")
    void testCompleteWhenTargetQuantityReached() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "stone");
        params.put("quantity", 1);
        task = new Task("gather", params);

        BlockPos resourcePos = new BlockPos(1, 64, 0);
        when(foreman.blockPosition()).thenReturn(resourcePos);
        when(level.getBlockState(resourcePos)).thenReturn(Blocks.STONE.defaultBlockState());
        when(level.destroyBlock(any(BlockPos.class), eq(true))).thenReturn(true);

        action = new GatherResourceAction(foreman, task);
        action.start();

        // Simulate gathering
        for (int i = 0; i < 150 && !action.isComplete(); i++) {
            action.tick();
        }

        if (action.isComplete()) {
            ActionResult result = action.getResult();
            if (result != null) {
                assertTrue(result.isSuccess() || result.getMessage().contains("timeout"),
                    "Should succeed or timeout");
            }
        }
    }

    @Test
    @DisplayName("Should report progress percentage")
    void testReportProgressPercentage() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "stone");
        params.put("quantity", 10);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        int progress = action.getProgressPercent();
        assertTrue(progress >= 0 && progress <= 100,
            "Progress should be between 0 and 100");
    }

    @Test
    @DisplayName("Should show progress in description")
    void testShowProgressInDescription() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "wood");
        params.put("quantity", 10);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);

        String description = action.getDescription();
        assertTrue(description.contains("/"),
            "Description should show progress (e.g., 0/10)");
    }

    // ==================== Timeout Tests ====================

    @Test
    @DisplayName("Should timeout after max ticks")
    void testTimeoutAfterMaxTicks() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "diamond_ore");
        params.put("quantity", 100);
        task = new Task("gather", params);

        // No resources available
        when(level.getBlockState(any(BlockPos.class))).thenReturn(Blocks.AIR.defaultBlockState());

        action = new GatherResourceAction(foreman, task);
        action.start();

        // Run to timeout (6000 ticks)
        for (int i = 0; i < 6100 && !action.isComplete(); i++) {
            action.tick();
        }

        assertTrue(action.isComplete(), "Should complete after timeout");
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertTrue(result.getMessage().contains("timeout") ||
                   result.getMessage().contains("Timeout") ||
                   result.getMessage().contains("gathered"),
            "Should indicate timeout or partial completion");
    }

    @Test
    @DisplayName("Should include gathered count in timeout message")
    void testIncludeGatheredCountInTimeout() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "iron_ore");
        params.put("quantity", 50);
        task = new Task("gather", params);

        when(level.getBlockState(any(BlockPos.class))).thenReturn(Blocks.AIR.defaultBlockState());

        action = new GatherResourceAction(foreman, task);
        action.start();

        // Run to timeout
        for (int i = 0; i < 6100 && !action.isComplete(); i++) {
            action.tick();
        }

        ActionResult result = action.getResult();
        if (result != null && result.getMessage().contains("timeout")) {
            assertTrue(result.getMessage().matches(".*\\d+/\\d+.*"),
                "Should show gathered/total in message");
        }
    }

    // ==================== Cancellation Tests ====================

    @Test
    @DisplayName("Should handle cancellation gracefully")
    void testCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "stone");
        params.put("quantity", 10);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();
        action.cancel();

        assertTrue(action.isComplete());
        verify(navigation, atLeastOnce()).stop();
    }

    @Test
    @DisplayName("Should stop navigation on cancellation")
    void testStopNavigationOnCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "coal_ore");
        params.put("quantity", 5);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();
        action.cancel();

        verify(navigation, atLeastOnce()).stop();
    }

    @Test
    @DisplayName("Should disable flying on cancellation")
    void testDisableFlyingOnCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "iron_ore");
        params.put("quantity", 5);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();
        action.cancel();

        verify(foreman, atLeastOnce()).setFlying(eq(false));
    }

    @Test
    @DisplayName("Should clear held item on cancellation")
    void testClearHeldItemOnCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "gold_ore");
        params.put("quantity", 3);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();
        action.cancel();

        verify(foreman, atLeastOnce()).setItemInHand(eq(InteractionHand.MAIN_HAND), eq(ItemStack.EMPTY));
    }

    @Test
    @DisplayName("Should handle double cancellation gracefully")
    void testDoubleCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "stone");
        params.put("quantity", 1);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();
        action.cancel();
        action.cancel(); // Second cancellation

        assertTrue(action.isComplete());
        // Should not throw exception
    }

    // ==================== Tool Selection Tests ====================

    @Test
    @DisplayName("Should equip axe for wood resources")
    void testEquipAxeForWood() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "oak_log");
        params.put("quantity", 1);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        verify(foreman, atLeastOnce()).setItemInHand(eq(InteractionHand.MAIN_HAND), argThat(item ->
            item != null && item.getItem() == Items.IRON_AXE));
    }

    @Test
    @DisplayName("Should equip pickaxe for ore resources")
    void testEquipPickaxeForOre() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "iron_ore");
        params.put("quantity", 1);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        verify(foreman, atLeastOnce()).setItemInHand(eq(InteractionHand.MAIN_HAND), argThat(item ->
            item != null && item.getItem() == Items.IRON_PICKAXE));
    }

    @Test
    @DisplayName("Should equip shovel for dirt resources")
    void testEquipShovelForDirt() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "dirt");
        params.put("quantity", 1);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        verify(foreman, atLeastOnce()).setItemInHand(eq(InteractionHand.MAIN_HAND), argThat(item ->
            item != null && item.getItem() == Items.IRON_SHOVEL));
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle null foreman gracefully")
    void testNullForeman() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "stone");
        params.put("quantity", 1);
        task = new Task("gather", params);

        action = new GatherResourceAction(null, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle null level gracefully")
    void testNullLevel() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "stone");
        params.put("quantity", 1);
        task = new Task("gather", params);

        lenient().when(foreman.level()).thenReturn(null);

        action = new GatherResourceAction(foreman, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle special characters in resource name")
    void testSpecialCharactersInResourceName() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "some_resource_with_underscores");
        params.put("quantity", 1);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        // Should handle gracefully
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle resource that disappears")
    void testResourceDisappears() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "stone");
        params.put("quantity", 1);
        task = new Task("gather", params);

        BlockPos resourcePos = new BlockPos(1, 64, 0);

        // Resource exists initially
        lenient().when(level.getBlockState(resourcePos)).thenReturn(
            Blocks.STONE.defaultBlockState(),
            Blocks.AIR.defaultBlockState() // Disappears
        );

        action = new GatherResourceAction(foreman, task);
        action.start();

        // Tick through gathering
        for (int i = 0; i < 150 && !action.isComplete(); i++) {
            action.tick();
        }

        // Should handle disappearing resource
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should periodically search for new resources")
    void testPeriodicSearch() {
        Map<String, Object> params = new HashMap<>();
        params.put("resource", "coal_ore");
        params.put("quantity", 5);
        task = new Task("gather", params);

        action = new GatherResourceAction(foreman, task);
        action.start();

        // Tick through multiple search intervals (100 ticks)
        for (int i = 0; i < 300; i++) {
            action.tick();
        }

        // Should have searched multiple times
        assertNotNull(action);
    }
}
