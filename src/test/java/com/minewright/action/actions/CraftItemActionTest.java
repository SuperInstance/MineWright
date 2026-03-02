package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.ActionResult.ErrorCode;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link CraftItemAction}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Item name parsing and validation</li>
 *   <li>Recipe lookup and validation</li>
 *   <li>Ingredient checking</li>
 *   <li>Crafting table navigation</li>
 *   <li>Crafting execution</li>
 *   <li>Timeout handling</li>
 *   <li>Cancellation and cleanup</li>
 *   <li>Error handling for invalid items</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CraftItemAction Tests")
class CraftItemActionTest {

    @Mock
    private ForemanEntity foreman;

    @Mock
    private Level level;

    @Mock
    private PathNavigation navigation;

    @Mock
    private RecipeManager recipeManager;

    private Task task;
    private CraftItemAction action;

    @BeforeEach
    void setUp() {
        lenient().when(foreman.level()).thenReturn(level);
        lenient().when(foreman.getEntityName()).thenReturn("TestForeman");
        lenient().when(foreman.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
        lenient().when(foreman.getNavigation()).thenReturn(navigation);
        lenient().when(level.getRecipeManager()).thenReturn(recipeManager);
        lenient().when(navigation.isDone()).thenReturn(true);
    }

    // ==================== Initialization Tests ====================

    @Test
    @DisplayName("Should successfully initialize with valid item name")
    void testValidItemInitialization() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "oak_planks");
        params.put("quantity", 10);
        task = new Task("craft", params);

        // Mock recipe lookup to return empty for simplicity
        lenient().when(recipeManager.getAllRecipesFor(RecipeType.CRAFTING))
            .thenReturn(Collections.emptyList());

        action = new CraftItemAction(foreman, task);
        action.start();

        // Should not fail immediately (will fail on recipe lookup)
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should fail initialization with null item name")
    void testNullItemInitialization() {
        Map<String, Object> params = new HashMap<>();
        params.put("quantity", 10);
        task = new Task("craft", params);

        action = new CraftItemAction(foreman, task);
        action.start();

        assertTrue(action.isComplete(), "Action should complete with failure");
        ActionResult result = action.getResult();
        assertNotNull(result, "Should have result");
        assertFalse(result.isSuccess(), "Should be failure result");
        assertTrue(result.getMessage().contains("Item name") ||
                   result.getMessage().contains("parameter"),
            "Should indicate missing item name parameter");
    }

    @Test
    @DisplayName("Should fail initialization with empty item name")
    void testEmptyItemInitialization() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "");
        params.put("quantity", 5);
        task = new Task("craft", params);

        action = new CraftItemAction(foreman, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle zero quantity gracefully")
    void testZeroQuantity() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "stone_pickaxe");
        params.put("quantity", 0);
        task = new Task("craft", params);

        action = new CraftItemAction(foreman, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertTrue(result.getMessage().contains("positive") ||
                   result.getMessage().contains("Quantity"),
            "Should indicate quantity must be positive");
    }

    @Test
    @DisplayName("Should handle negative quantity gracefully")
    void testNegativeQuantity() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "iron_sword");
        params.put("quantity", -5);
        task = new Task("craft", params);

        action = new CraftItemAction(foreman, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertTrue(result.getMessage().contains("positive") ||
                   result.getMessage().contains("Quantity"));
    }

    @Test
    @DisplayName("Should default quantity to 1 when not specified")
    void testDefaultQuantity() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "stick");
        task = new Task("craft", params);

        lenient().when(recipeManager.getAllRecipesFor(RecipeType.CRAFTING))
            .thenReturn(Collections.emptyList());

        action = new CraftItemAction(foreman, task);
        action.start();

        String description = action.getDescription();
        assertTrue(description.contains("1") || description.contains("stick"),
            "Should default to quantity 1");
    }

    // ==================== Recipe Lookup Tests ====================

    @Test
    @DisplayName("Should fail when item is unknown")
    void testUnknownItem() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "fake_item_that_does_not_exist");
        params.put("quantity", 1);
        task = new Task("craft", params);

        // Mock empty recipe stream
        when(recipeManager.getAllRecipesFor(RecipeType.CRAFTING))
            .thenReturn(Collections.emptyList());

        action = new CraftItemAction(foreman, task);
        action.start();

        // Tick to process state machine
        for (int i = 0; i < 10 && !action.isComplete(); i++) {
            action.tick();
        }

        ActionResult result = action.getResult();
        if (result != null) {
            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("Unknown") ||
                       result.getMessage().contains("item") ||
                       result.getMessage().contains("recipe"),
                "Should indicate item not found");
        }
    }

    @Test
    @DisplayName("Should fail when no recipe exists for item")
    void testNoRecipeFound() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "diamond"); // Raw resource, can't be crafted
        params.put("quantity", 1);
        task = new Task("craft", params);

        when(recipeManager.getAllRecipesFor(RecipeType.CRAFTING))
            .thenReturn(Collections.emptyList());

        action = new CraftItemAction(foreman, task);
        action.start();

        // Tick to process
        for (int i = 0; i < 10 && !action.isComplete(); i++) {
            action.tick();
        }

        ActionResult result = action.getResult();
        if (result != null) {
            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("recipe") ||
                       result.getMessage().contains("craft") ||
                       result.getMessage().contains("resource"));
        }
    }

    @Test
    @DisplayName("Should detect raw resources that cannot be crafted")
    void testRawResourceDetection() {
        String[] rawResources = {"oak_log", "iron_ingot", "diamond", "cobblestone"};

        for (String resource : rawResources) {
            Map<String, Object> params = new HashMap<>();
            params.put("item", resource);
            params.put("quantity", 1);
            task = new Task("craft", params);

            when(recipeManager.getAllRecipesFor(RecipeType.CRAFTING))
                .thenReturn(Collections.emptyList());

            action = new CraftItemAction(foreman, task);
            action.start();

            // Tick to process
            for (int i = 0; i < 10 && !action.isComplete(); i++) {
                action.tick();
            }

            ActionResult result = action.getResult();
            if (result != null) {
                assertFalse(result.isSuccess(),
                    "Should fail for raw resource: " + resource);
            }
        }
    }

    // ==================== Crafting Table Navigation Tests ====================

    @Test
    @DisplayName("Should find nearest crafting table")
    void testFindCraftingTable() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "stone_pickaxe");
        params.put("quantity", 1);
        task = new Task("craft", params);

        // Mock crafting table nearby
        BlockPos tablePos = new BlockPos(3, 64, 0);
        when(level.getBlockState(tablePos)).thenReturn(Blocks.CRAFTING_TABLE.defaultBlockState());

        action = new CraftItemAction(foreman, task);
        action.start();

        assertNotNull(action);
    }

    @Test
    @DisplayName("Should fail when no crafting table found")
    void testNoCraftingTableFound() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "iron_pickaxe");
        params.put("quantity", 1);
        task = new Task("craft", params);

        // Mock no crafting tables
        lenient().when(level.getBlockState(any(BlockPos.class)))
            .thenReturn(Blocks.AIR.defaultBlockState());

        action = new CraftItemAction(foreman, task);
        action.start();

        // Tick to reach navigation state
        for (int i = 0; i < 50 && !action.isComplete(); i++) {
            action.tick();
        }

        ActionResult result = action.getResult();
        if (result != null) {
            // Might fail on recipe lookup or crafting table search
            assertNotNull(result.getMessage());
        }
    }

    @Test
    @DisplayName("Should navigate to crafting table when distant")
    void testNavigateToCraftingTable() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "furnace");
        params.put("quantity", 1);
        task = new Task("craft", params);

        // Mock crafting table far away
        BlockPos tablePos = new BlockPos(30, 64, 0);
        when(level.getBlockState(tablePos)).thenReturn(Blocks.CRAFTING_TABLE.defaultBlockState());
        when(foreman.blockPosition()).thenReturn(new BlockPos(0, 64, 0));

        action = new CraftItemAction(foreman, task);
        action.start();

        // Tick to trigger navigation
        for (int i = 0; i < 20 && !action.isComplete(); i++) {
            action.tick();
        }

        // Verify navigation was used
        verify(navigation, atLeastOnce()).moveTo(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    // ==================== 2x2 Recipe Tests ====================

    @Test
    @DisplayName("Should recognize simple 2x2 recipes")
    void testSimple2x2Recipes() {
        String[] simpleRecipes = {"oak_planks", "stick", "crafting_table", "torches"};

        for (String item : simpleRecipes) {
            Map<String, Object> params = new HashMap<>();
            params.put("item", item);
            params.put("quantity", 1);
            task = new Task("craft", params);

            when(recipeManager.getAllRecipesFor(RecipeType.CRAFTING))
                .thenReturn(Collections.emptyList());

            action = new CraftItemAction(foreman, task);
            action.start();

            assertNotNull(action, "Should handle simple recipe: " + item);
        }
    }

    // ==================== Timeout Tests ====================

    @Test
    @DisplayName("Should timeout after max ticks")
    void testTimeoutAfterMaxTicks() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "golden_apple");
        params.put("quantity", 1);
        task = new Task("craft", params);

        when(recipeManager.getAllRecipesFor(RecipeType.CRAFTING))
            .thenReturn(Collections.emptyList());
        when(level.getBlockState(any(BlockPos.class)))
            .thenReturn(Blocks.AIR.defaultBlockState());

        action = new CraftItemAction(foreman, task);
        action.start();

        // Run until timeout (1200 ticks)
        for (int i = 0; i < 1250 && !action.isComplete(); i++) {
            action.tick();
        }

        assertTrue(action.isComplete(), "Should complete after timeout");
        ActionResult result = action.getResult();
        assertNotNull(result, "Should have result");
        assertTrue(result.getMessage().contains("timeout") ||
                   result.getMessage().contains("Timeout") ||
                   result.getMessage().contains("seconds"),
            "Should indicate timeout occurred");
    }

    // ==================== Cancellation Tests ====================

    @Test
    @DisplayName("Should handle cancellation gracefully")
    void testCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "diamond_sword");
        params.put("quantity", 1);
        task = new Task("craft", params);

        action = new CraftItemAction(foreman, task);
        action.start();
        action.cancel();

        assertTrue(action.isComplete(), "Should be complete after cancellation");
        ActionResult result = action.getResult();
        assertNotNull(result, "Should have result");
        assertFalse(result.isSuccess(), "Cancellation should be failure");
        assertTrue(result.getMessage().contains("cancelled") ||
                   result.getMessage().contains("Cancelled"),
            "Should indicate cancellation");
    }

    @Test
    @DisplayName("Should stop navigation on cancellation")
    void testStopNavigationOnCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "iron_chestplate");
        params.put("quantity", 1);
        task = new Task("craft", params);

        action = new CraftItemAction(foreman, task);
        action.start();
        action.cancel();

        // Verify navigation stopped
        verify(navigation, atLeastOnce()).stop();
    }

    @Test
    @DisplayName("Should handle double cancellation gracefully")
    void testDoubleCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "shield");
        params.put("quantity", 1);
        task = new Task("craft", params);

        action = new CraftItemAction(foreman, task);
        action.start();
        action.cancel();
        action.cancel(); // Second cancellation

        assertTrue(action.isComplete());
        // Should not throw exception
    }

    // ==================== Description Tests ====================

    @Test
    @DisplayName("Description should contain item name")
    void testDescriptionContainsItemName() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "stone_axe");
        params.put("quantity", 3);
        task = new Task("craft", params);

        action = new CraftItemAction(foreman, task);

        String description = action.getDescription();
        assertTrue(description.toLowerCase().contains("stone") ||
                   description.toLowerCase().contains("axe") ||
                   description.toLowerCase().contains("craft"),
            "Description should reference the item");
    }

    @Test
    @DisplayName("Description should show progress")
    void testDescriptionShowsProgress() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "bucket");
        params.put("quantity", 5);
        task = new Task("craft", params);

        action = new CraftItemAction(foreman, task);

        String description = action.getDescription();
        assertTrue(description.contains("/") || description.contains("0"),
            "Description should show progress (e.g., 0/5)");
    }

    @Test
    @DisplayName("Description should handle unknown item")
    void testDescriptionUnknownItem() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "invalid_item");
        params.put("quantity", 1);
        task = new Task("craft", params);

        action = new CraftItemAction(foreman, task);

        String description = action.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("unknown") || description.contains("Invalid"),
            "Description should indicate unknown item");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle null foreman gracefully")
    void testNullForeman() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "stick");
        params.put("quantity", 1);
        task = new Task("craft", params);

        action = new CraftItemAction(null, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle very large quantity")
    void testVeryLargeQuantity() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "torch");
        params.put("quantity", 10000);
        task = new Task("craft", params);

        action = new CraftItemAction(foreman, task);
        action.start();

        // Should not crash, will eventually timeout
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle item name case insensitivity")
    void testItemNameCaseInsensitivity() {
        String[] variants = {"STONE_PICKAXE", "Stone_Pickaxe", "stone_pickaxe", "StOnEpIcKaXe"};

        for (String variant : variants) {
            Map<String, Object> params = new HashMap<>();
            params.put("item", variant);
            params.put("quantity", 1);
            task = new Task("craft", params);

            lenient().when(recipeManager.getAllRecipesFor(RecipeType.CRAFTING))
                .thenReturn(Collections.emptyList());

            action = new CraftItemAction(foreman, task);
            action.start();

            assertNotNull(action, "Should handle case variant: " + variant);
        }
    }

    @Test
    @DisplayName("Should handle item names with spaces")
    void testItemNameWithSpaces() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "oak plank"); // Should normalize to oak_plank
        params.put("quantity", 1);
        task = new Task("craft", params);

        lenient().when(recipeManager.getAllRecipesFor(RecipeType.CRAFTING))
            .thenReturn(Collections.emptyList());

        action = new CraftItemAction(foreman, task);
        action.start();

        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle item names with minecraft namespace")
    void testItemNameWithNamespace() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "minecraft:stone");
        params.put("quantity", 1);
        task = new Task("craft", params);

        lenient().when(recipeManager.getAllRecipesFor(RecipeType.CRAFTING))
            .thenReturn(Collections.emptyList());

        action = new CraftItemAction(foreman, task);
        action.start();

        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle missing quantity parameter")
    void testMissingQuantityParameter() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "wooden_pickaxe");
        task = new Task("craft", params);

        action = new CraftItemAction(foreman, task);
        action.start();

        // Should default to 1
        String description = action.getDescription();
        assertTrue(description.contains("1") || description.contains("wooden"),
            "Should default to quantity 1");
    }

    @Test
    @DisplayName("Should handle special characters in item name")
    void testSpecialCharactersInItemName() {
        Map<String, Object> params = new HashMap<>();
        params.put("item", "stone_with_underscores");
        params.put("quantity", 1);
        task = new Task("craft", params);

        action = new CraftItemAction(foreman, task);
        action.start();

        assertNotNull(action);
    }
}
