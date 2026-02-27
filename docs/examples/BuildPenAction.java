package com.minewright.animal.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.action.actions.PlaceBlockAction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Action to build an animal pen enclosure.
 *
 * <p>Constructs a fenced enclosure with a gate, feeder, and water trough.
 * Pens can be customized for different animal types.</p>
 *
 * <p>Pen structure includes:</p>
 * <ul>
 *   <li>Fence walls ( configurable height)</li>
 *   <li>Fence gate entrance</li>
 *   <li>Grass floor</li>
 *   <li>Hay block feeder</li>
 *   <li>Water trough</li>
 * </ul>
 */
public class BuildPenAction extends BaseAction {
    private BlockPos center;
    private int width, depth, height;
    private String animalType;
    private List<BlockPos> blocksToPlace;
    private int blocksPlaced = 0;
    private int currentBlockIndex = 0;

    public BuildPenAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        // Parse parameters
        animalType = task.getStringParameter("type", "generic");
        width = task.getIntParameter("width", 16);
        depth = task.getIntParameter("depth", 16);
        height = task.getIntParameter("height", 3);

        // Determine center (either specified or near foreman)
        if (task.hasParameter("x") && task.hasParameter("z")) {
            int x = task.getIntParameter("x", foreman.blockPosition().getX());
            int y = task.getIntParameter("y", foreman.blockPosition().getY());
            int z = task.getIntParameter("z", foreman.blockPosition().getZ());
            center = new BlockPos(x, y, z);
        } else {
            // Find flat area near foreman
            center = findBuildLocation();
        }

        if (center == null) {
            result = ActionResult.failure("Could not find suitable build location");
            return;
        }

        // Generate block list
        blocksToPlace = generatePenBlocks();

        foreman.sendChatMessage("Building " + animalType + " pen at " + center +
            " (" + blocksToPlace.size() + " blocks)");
    }

    @Override
    protected void onTick() {
        if (currentBlockIndex >= blocksToPlace.size()) {
            result = ActionResult.success("Built " + animalType + " pen with " + blocksPlaced + " blocks");
            return;
        }

        // Place a few blocks per tick for faster building
        int blocksThisTick = 0;
        while (currentBlockIndex < blocksToPlace.size() && blocksThisTick < 4) {
            BlockPos pos = blocksToPlace.get(currentBlockIndex);

            // Calculate block type for this position
            BlockState blockState = getBlockForPosition(pos);

            // Place block (simplified - in real implementation use PlaceBlockAction)
            if (placeBlock(pos, blockState)) {
                blocksPlaced++;
            }

            currentBlockIndex++;
            blocksThisTick++;
        }

        // Move to next section periodically
        if (blocksPlaced % 20 == 0) {
            BlockPos nextPos = blocksToPlace.get(
                Math.min(currentBlockIndex + 10, blocksToPlace.size() - 1)
            );
            foreman.getNavigation().moveTo(nextPos.getX(), nextPos.getY(), nextPos.getZ(), 1.0);
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Building " + animalType + " pen (" + blocksPlaced + "/" + blocksToPlace.size() + ")";
    }

    /**
     * Find a suitable location for the pen (flat area)
     */
    private BlockPos findBuildLocation() {
        BlockPos foremanPos = foreman.blockPosition();

        // Search for flat area within 16 blocks
        for (int x = -16; x <= 16; x += 4) {
            for (int z = -16; z <= 16; z += 4) {
                BlockPos testPos = foremanPos.offset(x, 0, z);

                if (isAreaFlat(testPos, width, depth)) {
                    return testPos;
                }
            }
        }

        return null;
    }

    /**
     * Check if area is flat enough for building
     */
    private boolean isAreaFlat(BlockPos center, int w, int d) {
        int y = center.getY();

        for (int x = -w/2; x <= w/2; x += 2) {
            for (int z = -d/2; z <= d/2; z += 2) {
                BlockPos checkPos = center.offset(x, 0, z);

                // Check if there's solid ground
                if (!foreman.level().getBlockState(checkPos.below()).isSolid()) {
                    return false;
                }

                // Check if area is clear
                if (!foreman.level().getBlockState(checkPos).isAir() &&
                    !foreman.level().getBlockState(checkPos).canBeReplaced()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Generate all block positions for the pen
     */
    private List<BlockPos> generatePenBlocks() {
        List<BlockPos> blocks = new ArrayList<>();

        // Floor
        for (int x = -width/2; x < width/2; x++) {
            for (int z = -depth/2; z < depth/2; z++) {
                blocks.add(center.offset(x, -1, z));
            }
        }

        // Walls (fences)
        for (int x = -width/2; x <= width/2; x++) {
            for (int y = 0; y < height; y++) {
                blocks.add(center.offset(x, y, -depth/2)); // North
                blocks.add(center.offset(x, y, depth/2));  // South
            }
        }

        for (int z = -depth/2; z <= depth/2; z++) {
            for (int y = 0; y < height; y++) {
                blocks.add(center.offset(-width/2, y, z)); // West
                blocks.add(center.offset(width/2, y, z));  // East
            }
        }

        // Corner posts (fence gates)
        blocks.add(center.offset(0, 0, depth/2)); // Entrance

        return blocks;
    }

    /**
     * Determine which block type to place at a position
     */
    private BlockState getBlockForPosition(BlockPos pos) {
        int dx = pos.getX() - center.getX();
        int dy = pos.getY() - center.getY();
        int dz = pos.getZ() - center.getZ();

        // Floor
        if (dy == -1) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }

        // Entrance gate
        if (dx == 0 && dz == depth/2 && dy == 0) {
            return Blocks.OAK_FENCE_GATE.defaultBlockState();
        }

        // Walls (fences)
        if (Math.abs(dx) == width/2 || Math.abs(dz) == depth/2) {
            return Blocks.OAK_FENCE.defaultBlockState();
        }

        // Feeder in center
        if (dx == 0 && dz == 0 && dy == 0) {
            return Blocks.HAY_BLOCK.defaultBlockState();
        }

        // Water trough
        if (dx == 2 && dz == 0 && dy == 0) {
            return Blocks.WATER.defaultBlockState();
        }

        return Blocks.AIR.defaultBlockState();
    }

    /**
     * Place a block at the specified position
     */
    private boolean placeBlock(BlockPos pos, BlockState blockState) {
        if (blockState.isAir()) return false;

        // Move to position
        double distance = foreman.blockPosition().distSqr(pos);
        if (distance > 25.0) { // 5 blocks away
            foreman.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1.0);
            return false;
        }

        // Place the block
        foreman.level().setBlock(pos, blockState, 3);
        return true;
    }
}
