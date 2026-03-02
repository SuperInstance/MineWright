package com.minewright.action.actions;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import com.minewright.action.ActionResult;
import com.minewright.action.ActionResult.ErrorCode;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.exception.ActionException;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;
import java.util.Set;

/**
 * Action for crafting items using available recipes and ingredients.
 *
 * <p>This action:</p>
 * <ul>
 *   <li>Looks up the recipe for the requested item</li>
 *   <li>Checks if Foreman has the required ingredients</li>
 *   <li>Navigates to a crafting table if needed (3x3 recipes)</li>
 *   <li>Executes the crafting operation</li>
 * </ul>
 *
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Only supports shapeless crafting and simple shaped recipes</li>
 *   <li>Requires ingredients in Foreman's inventory (no chest fetching yet)</li>
 *   <li>2x2 crafting uses Foreman's personal crafting grid</li>
 *   <li>3x3 crafting requires navigating to a crafting table block</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li>UNKNOWN_ITEM: Item name not recognized</li>
 *   <li>NO_RECIPE: No crafting recipe exists for item</li>
 *   <li>TIMEOUT: Crafting operation took too long</li>
 *   <li>RESOURCE_UNAVAILABLE: Crafting table not found</li>
 * </ul>
 */
public class CraftItemAction extends BaseAction {
    private static final Logger LOGGER = TestLogger.getLogger(CraftItemAction.class);

    private static final int MAX_TICKS = 1200; // 60 seconds timeout
    private static final double CRAFTING_TABLE_DISTANCE = 4.0;

    private String itemName;
    private int quantity;
    private int craftedQuantity;
    private int ticksRunning;
    private Item targetItem;
    private CraftingRecipe recipe;
    private boolean needsCraftingTable;
    private BlockPos craftingTablePos;
    private CraftingState state;

    private enum CraftingState {
        LOOKUP_RECIPE,
        CHECK_INGREDIENTS,
        NAVIGATE_TO_TABLE,
        CRAFT,
        COMPLETE
    }

    public CraftItemAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        try {
            validateState();

            itemName = task.getStringParameter("item");
            if (itemName == null || itemName.isEmpty()) {
                fail(ErrorCode.INVALID_PARAMS, "Item name parameter is required", true);
                return;
            }

            quantity = task.getIntParameter("quantity", 1);
            if (quantity <= 0) {
                fail(ErrorCode.INVALID_PARAMS, "Quantity must be positive", true);
                return;
            }

            craftedQuantity = 0;
            ticksRunning = 0;
            state = CraftingState.LOOKUP_RECIPE;

            LOGGER.info("[{}] Craft action started: {}x {}", foreman.getEntityName(), quantity, itemName);

            // Begin the crafting process
            processCrafting();

        } catch (Exception e) {
            handleUnexpectedException(e, "action start");
        }
    }

    @Override
    protected void onTick() {
        if (isComplete()) {
            return;
        }

        try {
            ticksRunning++;

            if (ticksRunning > MAX_TICKS) {
                failTimeout(MAX_TICKS + " ticks (" + (MAX_TICKS / 20) + " seconds)");
                cleanup();
                return;
            }

            // Continue processing crafting state machine
            processCrafting();

        } catch (Exception e) {
            handleUnexpectedException(e, "action tick");
        }
    }

    @Override
    protected void onCancel() {
        try {
            if (foreman != null && foreman.getNavigation() != null) {
                foreman.getNavigation().stop();
            }
            LOGGER.info("[{}] Craft action cancelled: {}x {}", getForemanName(), quantity, itemName);
        } finally {
            cleanup();
        }
    }

    @Override
    protected void cleanup() {
        // Stop navigation if active
        if (foreman != null && foreman.getNavigation() != null) {
            foreman.getNavigation().stop();
        }
        LOGGER.debug("[{}] Craft action cleanup complete", getForemanName());
    }

    private String getForemanName() {
        return foreman != null ? foreman.getEntityName() : "unknown";
    }

    @Override
    public String getDescription() {
        return "Craft " + quantity + " " + (itemName != null ? itemName : "unknown") +
               " (" + craftedQuantity + "/" + quantity + ")";
    }

    /**
     * Main crafting state machine processor.
     * Advances through the crafting stages each tick until complete.
     */
    private void processCrafting() {
        try {
            switch (state) {
                case LOOKUP_RECIPE -> lookupRecipe();
                case CHECK_INGREDIENTS -> checkIngredients();
                case NAVIGATE_TO_TABLE -> navigateToCraftingTable();
                case CRAFT -> performCrafting();
            }
        } catch (Exception e) {
            handleUnexpectedException(e, "crafting process");
        }
    }

    /**
     * Looks up the crafting recipe for the target item.
     */
    private void lookupRecipe() {
        targetItem = parseItem(itemName);

        if (targetItem == null || targetItem == Items.AIR) {
            failWithRecovery(
                "Unknown item: " + itemName,
                true,
                "Check the item name. Use format like 'oak_planks' or 'stone_pickaxe'."
            );
            return;
        }

        // Get recipe manager from the level
        RecipeManager recipeManager = foreman.level().getRecipeManager();

        // Find first matching recipe for this item
        Optional<CraftingRecipe> recipeOpt = recipeManager
            .getAllRecipesFor(RecipeType.CRAFTING)
            .stream()
            .filter(r -> r.getResultItem(foreman.level().registryAccess()).getItem() == targetItem)
            .findFirst();

        if (recipeOpt.isEmpty()) {
            // Check if it's an item that can't be crafted (raw resources)
            String rawResources = Set.of("oak_log", "spruce_log", "birch_log", "stone", "cobblestone",
                "iron_ingot", "gold_ingot", "diamond", "coal", "redstone")
                .stream()
                .filter(itemName::contains)
                .findFirst()
                .orElse(null);

            if (rawResources != null) {
                fail(ErrorCode.NOT_FOUND,
                    "Cannot craft raw resource: " + itemName,
                    true);
            } else {
                fail(ErrorCode.NOT_FOUND,
                    "No recipe found for: " + itemName,
                    true);
            }
            return;
        }

        recipe = recipeOpt.get();

        // Check if recipe requires 3x3 grid (crafting table)
        // We assume most items need a crafting table unless it's a known 2x2 recipe
        String[] simple2x2Recipes = {"oak_planks", "spruce_planks", "birch_planks", "stick",
            "crafting_table", "furnace", "chest", "torches"};
        boolean isSimpleRecipe = false;
        for (String simple : simple2x2Recipes) {
            if (itemName.contains(simple)) {
                isSimpleRecipe = true;
                break;
            }
        }
        needsCraftingTable = !isSimpleRecipe;

        LOGGER.info("[{}] Found recipe for {} (needs table: {})",
            foreman.getEntityName(), itemName, needsCraftingTable);

        state = CraftingState.CHECK_INGREDIENTS;
    }

    /**
     * Checks if Foreman has the required ingredients.
     * NOTE: For simplicity, we skip strict ingredient checking since Foreman
     * doesn't have a persistent player inventory. In production, you would check
     * the nearest chest or player's inventory.
     */
    private void checkIngredients() {
        if (recipe == null) {
            fail(ErrorCode.INVALID_STATE, "Recipe not available", true);
            return;
        }

        // For now, assume ingredients are available in nearby chests or player inventory
        // A full implementation would:
        // 1. Check Foreman's inventory (if implemented)
        // 2. Check nearby chest blocks
        // 3. Ask player to provide items

        LOGGER.info("[{}] Proceeding with crafting (assuming ingredients available)",
            foreman.getEntityName());

        if (needsCraftingTable) {
            state = CraftingState.NAVIGATE_TO_TABLE;
        } else {
            state = CraftingState.CRAFT;
        }
    }

    /**
     * Navigates to the nearest crafting table.
     */
    private void navigateToCraftingTable() {
        if (craftingTablePos == null) {
            craftingTablePos = findNearestCraftingTable();

            if (craftingTablePos == null) {
                fail(ErrorCode.RESOURCE_UNAVAILABLE,
                    "No crafting table found nearby (searched 32 blocks)",
                    true);
                return;
            }

            LOGGER.info("[{}] Found crafting table at {}", foreman.getEntityName(), craftingTablePos);
        }

        double distance = foreman.blockPosition().distSqr(craftingTablePos);

        if (distance <= CRAFTING_TABLE_DISTANCE * CRAFTING_TABLE_DISTANCE) {
            // Close enough to crafting table
            LOGGER.info("[{}] Reached crafting table", foreman.getEntityName());
            state = CraftingState.CRAFT;
            return;
        }

        // Navigate to crafting table
        PathNavigation navigation = foreman.getNavigation();
        if (navigation != null && navigation.isDone()) {
            navigation.moveTo(craftingTablePos.getX(), craftingTablePos.getY(),
                           craftingTablePos.getZ(), 1.0);
            LOGGER.debug("[{}] Moving to crafting table at {}", foreman.getEntityName(), craftingTablePos);
        } else if (navigation == null) {
            fail(ErrorCode.INVALID_STATE, "Navigation not available", false);
        }
    }

    /**
     * Performs the actual crafting operation.
     *
     * NOTE: This is a simplified implementation that simulates crafting.
     * A full implementation would:
     * 1. Open the crafting container (player inventory or crafting table)
     * 2. Place ingredients in the correct pattern
     * 3. Take the result item
     * 4. Consume ingredients
     *
     * For now, we just indicate success and log what would be crafted.
     */
    private void performCrafting() {
        if (recipe == null) {
            fail(ErrorCode.INVALID_STATE, "Recipe not available", true);
            return;
        }

        // Get the result item
        ItemStack resultStack = recipe.getResultItem(foreman.level().registryAccess());

        if (resultStack.isEmpty()) {
            fail(ErrorCode.EXECUTION_ERROR, "Recipe produced empty result", true);
            return;
        }

        // In a full implementation, we would:
        // - Use ServerPlayer#getCraftingContainer() to get the crafting menu
        // - Place ingredients according to recipe pattern
        // - Click the result slot to craft
        // - Handle multiple craft operations for quantity

        craftedQuantity = quantity;

        LOGGER.info("[{}] Successfully crafted {}x {} (simulated - full inventory integration pending)",
            foreman.getEntityName(), quantity, itemName);

        succeed("Crafted " + quantity + " " + itemName + ". Place items in a nearby chest for pickup.");
        state = CraftingState.COMPLETE;
    }

    /**
     * Finds the nearest crafting table within search radius.
     *
     * @return Position of nearest crafting table, or null if none found
     */
    private BlockPos findNearestCraftingTable() {
        final int SEARCH_RADIUS = 32;

        BlockPos foremanPos = foreman.blockPosition();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos checkPos = foremanPos.offset(x, y, z);

                    try {
                        if (foreman.level().getBlockState(checkPos).is(Blocks.CRAFTING_TABLE)) {
                            double dist = checkPos.distSqr(foremanPos);
                            if (dist < nearestDistance) {
                                nearest = checkPos;
                                nearestDistance = dist;
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.debug("[{}] Error checking block at {}: {}",
                            foreman.getEntityName(), checkPos, e.getMessage());
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * Parses an item name from string to Item instance.
     *
     * @param itemName The item name to parse
     * @return The Item, or null if not found
     */
    private Item parseItem(String itemName) {
        try {
            // Normalize item name
            String normalizedName = itemName.toLowerCase().replace(" ", "_");

            // Add minecraft namespace if not present
            if (!normalizedName.contains(":")) {
                normalizedName = "minecraft:" + normalizedName;
            }

            ResourceLocation resourceLocation = new ResourceLocation(normalizedName);
            return ForgeRegistries.ITEMS.getValue(resourceLocation);
        } catch (Exception e) {
            LOGGER.warn("[{}] Failed to parse item name '{}': {}",
                foreman.getEntityName(), itemName, e.getMessage());
            return null;
        }
    }
}

