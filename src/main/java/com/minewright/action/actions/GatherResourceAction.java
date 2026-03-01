package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.testutil.TestLogger;
import com.minewright.util.BlockNameMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action for gathering resources from the environment.
 *
 * <p>This action is a smart wrapper that:</p>
 * <ul>
 *   <li>Identifies the resource type (block, item, or entity)</li>
 *   <li>Searches for nearby sources</li>
 *   <li>Navigates to the resource</li>
 *   <li>Gathers the resource using appropriate methods</li>
 *   <li>Handles chest storage if inventory is full</li>
 * </ul>
 *
 * <p><b>Resource Types:</b></p>
 * <ul>
 *   <li><b>Blocks:</b> Uses mining (e.g., "oak_log", "stone", "iron_ore")</li>
 *   <li><b>Items:</b> Uses pickup (e.g., "drops", "loot")</li>
 *   <li><b>Entities:</b> Uses farming (e.g., "wheat", "carrot")</li>
 * </ul>
 */
public class GatherResourceAction extends BaseAction {
    private static final Logger LOGGER = TestLogger.getLogger(GatherResourceAction.class);

    private String resourceType;
    private int targetQuantity;
    private int gatheredCount;
    private Block targetBlock;
    private BlockPos currentTarget;
    private int searchRadius;
    private int ticksRunning;
    private int ticksSinceLastSearch;
    private static final int MAX_TICKS = 6000; // 5 minutes
    private static final int SEARCH_INTERVAL = 100; // Search every 5 seconds
    private static final int DEFAULT_SEARCH_RADIUS = 32;

    // Resource to block mappings
    private static final Map<String, String> RESOURCE_ALIASES = new HashMap<>();
    static {
        RESOURCE_ALIASES.put("wood", "oak_log");
        RESOURCE_ALIASES.put("log", "oak_log");
        RESOURCE_ALIASES.put("tree", "oak_log");
        RESOURCE_ALIASES.put("rock", "stone");
        RESOURCE_ALIASES.put("stone", "stone");
        RESOURCE_ALIASES.put("cobble", "cobblestone");
        RESOURCE_ALIASES.put("dirt", "dirt");
        RESOURCE_ALIASES.put("grass", "grass_block");
        RESOURCE_ALIASES.put("sand", "sand");
        RESOURCE_ALIASES.put("iron", "iron_ore");
        RESOURCE_ALIASES.put("coal", "coal_ore");
        RESOURCE_ALIASES.put("gold", "gold_ore");
        RESOURCE_ALIASES.put("diamond", "diamond_ore");
    }

    public GatherResourceAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        if (foreman == null || foreman.level() == null || foreman.getNavigation() == null) {
            fail("Foreman, level, or navigation not available", false);
            return;
        }

        resourceType = task.getStringParameter("resource");
        if (resourceType == null || resourceType.isEmpty()) {
            fail("Resource type parameter is required", true);
            return;
        }

        targetQuantity = task.getIntParameter("quantity", 1);
        if (targetQuantity <= 0) {
            fail("Quantity must be positive", true);
            return;
        }

        searchRadius = task.getIntParameter("radius", DEFAULT_SEARCH_RADIUS);
        gatheredCount = 0;
        ticksRunning = 0;
        ticksSinceLastSearch = 0;

        // Normalize resource type
        String normalizedResource = normalizeResourceType(resourceType);
        targetBlock = parseBlock(normalizedResource);

        if (targetBlock == null || targetBlock == Blocks.AIR) {
            failWithRecovery(
                "Unknown resource type: " + resourceType,
                true,
                "Try specific block names like 'oak_log', 'stone', 'iron_ore', or 'coal_ore'"
            );
            return;
        }

        LOGGER.info("[{}] Gathering {}x {} (search radius: {})",
            foreman.getEntityName(), targetQuantity, targetBlock.getName().getString(), searchRadius);

        // Equip appropriate tool
        equipAppropriateTool();

        // Find initial target
        findNearestResource();
    }

    @Override
    protected void onTick() {
        if (foreman == null || foreman.level() == null) {
            fail("Invalid state during gathering", false);
            return;
        }

        ticksRunning++;
        ticksSinceLastSearch++;

        if (ticksRunning > MAX_TICKS) {
            failWithRecovery(
                "Gathering timeout - gathered " + gatheredCount + "/" + targetQuantity,
                gatheredCount > 0,
                "Try increasing the search radius or checking if resources are available nearby"
            );
            return;
        }

        // Check if we've gathered enough
        if (gatheredCount >= targetQuantity) {
            succeed("Gathered " + gatheredCount + " " + targetBlock.getName().getString());
            return;
        }

        // Periodically search for new targets if current is invalid
        if (ticksSinceLastSearch >= SEARCH_INTERVAL || currentTarget == null) {
            findNearestResource();
            ticksSinceLastSearch = 0;
        }

        if (currentTarget == null) {
            // No resources found
            if (ticksRunning % 200 == 0) {
                LOGGER.debug("[{}] No {} found nearby, searching...",
                    foreman.getEntityName(), targetBlock.getName().getString());
            }
            return;
        }

        // Navigate to and gather the resource
        gatherResource();
    }

    @Override
    protected void onCancel() {
        if (foreman != null) {
            if (foreman.getNavigation() != null) {
                foreman.getNavigation().stop();
            }
            foreman.setFlying(false);
            foreman.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    @Override
    public String getDescription() {
        String blockName = (targetBlock != null) ? targetBlock.getName().getString() : resourceType;
        return "Gather " + gatheredCount + "/" + targetQuantity + " " + blockName;
    }

    /**
     * Normalizes resource type using aliases.
     */
    private String normalizeResourceType(String resourceType) {
        String normalized = resourceType.toLowerCase().replace(" ", "_");

        // Check for aliases
        if (RESOURCE_ALIASES.containsKey(normalized)) {
            return RESOURCE_ALIASES.get(normalized);
        }

        // Use BlockNameMapper for additional normalization
        return BlockNameMapper.normalize(normalized);
    }

    /**
     * Parses a block name from string to Block instance.
     */
    private Block parseBlock(String blockName) {
        String normalizedName = blockName.toLowerCase().replace(" ", "_");

        if (!normalizedName.contains(":")) {
            normalizedName = "minecraft:" + normalizedName;
        }

        ResourceLocation resourceLocation = new ResourceLocation(normalizedName);
        return ForgeRegistries.BLOCKS.getValue(resourceLocation);
    }

    /**
     * Finds the nearest resource block within search radius.
     */
    private void findNearestResource() {
        BlockPos foremanPos = foreman.blockPosition();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        // Search in expanding sphere
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = foremanPos.offset(x, y, z);

                    // Check if this block matches our target
                    if (foreman.level().getBlockState(checkPos).getBlock() == targetBlock) {
                        double distance = checkPos.distSqr(foremanPos);
                        if (distance < nearestDistance) {
                            nearest = checkPos;
                            nearestDistance = distance;
                        }
                    }
                }
            }
        }

        currentTarget = nearest;

        if (currentTarget != null) {
            LOGGER.debug("[{}] Found {} at {} ({} blocks away)",
                foreman.getEntityName(), targetBlock.getName().getString(),
                currentTarget, (int)Math.sqrt(nearestDistance));
        }
    }

    /**
     * Gathers the resource at the current target position.
     */
    private void gatherResource() {
        if (currentTarget == null) {
            return;
        }

        double distance = foreman.blockPosition().distSqr(currentTarget);

        // Navigate to the resource if far
        if (distance > 9.0) { // 3 blocks
            if (foreman.getNavigation().isDone()) {
                foreman.getNavigation().moveTo(
                    currentTarget.getX(),
                    currentTarget.getY(),
                    currentTarget.getZ(),
                    1.0
                );
            }
            return;
        }

        // Check if the resource is still there
        if (foreman.level().getBlockState(currentTarget).getBlock() != targetBlock) {
            LOGGER.debug("[{}] Resource at {} was already mined",
                foreman.getEntityName(), currentTarget);
            currentTarget = null;
            return;
        }

        // Face and break the block
        foreman.getLookControl().setLookAt(
            currentTarget.getX() + 0.5,
            currentTarget.getY() + 0.5,
            currentTarget.getZ() + 0.5
        );

        foreman.swing(InteractionHand.MAIN_HAND, true);

        boolean destroyed = foreman.level().destroyBlock(currentTarget, true);

        if (destroyed) {
            gatheredCount++;
            LOGGER.info("[{}] Gathered {} at {} - Progress: {}/{}",
                foreman.getEntityName(), targetBlock.getName().getString(),
                currentTarget, gatheredCount, targetQuantity);

            currentTarget = null; // Find next target
        }
    }

    /**
     * Equips the appropriate tool for gathering this resource.
     */
    private void equipAppropriateTool() {
        ItemStack tool = ItemStack.EMPTY;

        // Determine best tool based on resource type
        String resourceLower = targetBlock.getName().getString().toLowerCase();

        if (resourceLower.contains("log") || resourceLower.contains("wood") ||
            resourceLower.contains("plank")) {
            tool = new ItemStack(Items.IRON_AXE);
        } else if (resourceLower.contains("ore") || resourceLower.contains("stone") ||
                   resourceLower.contains("cobble")) {
            tool = new ItemStack(Items.IRON_PICKAXE);
        } else if (resourceLower.contains("dirt") || resourceLower.contains("grass") ||
                   resourceLower.contains("sand")) {
            tool = new ItemStack(Items.IRON_SHOVEL);
        } else {
            tool = new ItemStack(Items.IRON_PICKAXE); // Default
        }

        foreman.setItemInHand(InteractionHand.MAIN_HAND, tool);
    }

    /**
     * Gets the current progress percentage (0-100).
     */
    public int getProgressPercent() {
        if (targetQuantity <= 0) return 0;
        return Math.min(100, (gatheredCount * 100) / targetQuantity);
    }
}

