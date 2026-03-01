package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.ActionResult.ErrorCode;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Action for placing blocks at specific positions.
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li>INVALID_PARAMS: Missing or invalid block name/position</li>
 *   <li>BLOCKED: Target position is occupied</li>
 *   <li>TIMEOUT: Took too long to reach target</li>
 *   <li>INVALID_STATE: Foreman or level not available</li>
 * </ul>
 */
public class PlaceBlockAction extends BaseAction {
    private static final int MAX_TICKS = 200;
    private static final double PLACEMENT_DISTANCE = 5.0;

    private Block blockToPlace;
    private BlockPos targetPos;
    private int ticksRunning;

    public PlaceBlockAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        try {
            // Validate state
            if (foreman == null || foreman.level() == null || foreman.getNavigation() == null) {
                fail(ErrorCode.INVALID_STATE, "Foreman, level, or navigation not available", false);
                return;
            }

            // Parse and validate block name
            String blockName = task.getStringParameter("block");
            if (blockName == null || blockName.isEmpty()) {
                fail(ErrorCode.INVALID_PARAMS, "Block name parameter is required", true);
                return;
            }

            // Parse and validate position
            int x = task.getIntParameter("x", 0);
            int y = task.getIntParameter("y", 0);
            int z = task.getIntParameter("z", 0);

            targetPos = new BlockPos(x, y, z);
            ticksRunning = 0;

            // Parse block type
            blockToPlace = parseBlock(blockName);

            if (blockToPlace == null || blockToPlace == Blocks.AIR) {
                fail(ErrorCode.NOT_FOUND, "Invalid block type: " + blockName, true);
                return;
            }

            LOGGER.debug("[{}] Place block action initialized: {} at {}",
                foreman.getEntityName(), blockName, targetPos);

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
            // Validate state
            if (foreman == null || foreman.level() == null || targetPos == null || blockToPlace == null) {
                fail(ErrorCode.INVALID_STATE, "Invalid state during block placement", false);
                cleanup();
                return;
            }

            ticksRunning++;

            // Check timeout
            if (ticksRunning > MAX_TICKS) {
                failTimeout(MAX_TICKS + " ticks (" + (MAX_TICKS / 20) + " seconds)");
                cleanup();
                return;
            }

            // Navigate to target if far
            if (!foreman.blockPosition().closerThan(targetPos, PLACEMENT_DISTANCE)) {
                if (foreman.getNavigation().isDone()) {
                    foreman.getNavigation().moveTo(targetPos.getX(), targetPos.getY(),
                        targetPos.getZ(), 1.0);
                }
                return;
            }

            // Check if position is empty
            BlockState currentState = foreman.level().getBlockState(targetPos);
            if (!currentState.isAir() && !currentState.getFluidState().isEmpty()) {
                failBlocked("Position occupied by " + currentState.getBlock().getName().getString());
                cleanup();
                return;
            }

            // Place the block
            try {
                foreman.level().setBlock(targetPos, blockToPlace.defaultBlockState(), 3);
                succeed("Placed " + blockToPlace.getName().getString() + " at " + targetPos);
                LOGGER.info("[{}] Successfully placed {} at {}",
                    foreman.getEntityName(), blockToPlace.getName().getString(), targetPos);
            } catch (Exception e) {
                LOGGER.error("[{}] Failed to place block at {}: {}",
                    foreman.getEntityName(), targetPos, e.getMessage());
                fail(ErrorCode.EXECUTION_ERROR, "Failed to place block: " + e.getMessage(), true);
            }

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
        LOGGER.debug("[{}] Place block action cleanup complete", foreman.getEntityName());
    }

    @Override
    public String getDescription() {
        String blockName = (blockToPlace != null) ? blockToPlace.getName().getString() : "unknown";
        return "Place " + blockName + " at " + (targetPos != null ? targetPos : "unknown");
    }

    /**
     * Parses a block name from string to Block instance.
     *
     * @param blockName The block name to parse
     * @return The Block, or null if not found
     */
    private Block parseBlock(String blockName) {
        try {
            String normalizedName = blockName.toLowerCase().replace(" ", "_");
            if (!normalizedName.contains(":")) {
                normalizedName = "minecraft:" + normalizedName;
            }
            ResourceLocation resourceLocation = new ResourceLocation(normalizedName);
            return ForgeRegistries.BLOCKS.getValue(resourceLocation);
        } catch (Exception e) {
            LOGGER.warn("[{}] Failed to parse block name '{}': {}",
                foreman != null ? foreman.getEntityName() : "unknown", blockName, e.getMessage());
            return null;
        }
    }
}

