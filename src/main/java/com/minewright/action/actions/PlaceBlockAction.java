package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PlaceBlockAction extends BaseAction {
    private Block blockToPlace;
    private BlockPos targetPos;
    private int ticksRunning;
    private static final int MAX_TICKS = 200;

    public PlaceBlockAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        if (foreman == null || foreman.level() == null || foreman.getNavigation() == null) {
            result = ActionResult.failure("Foreman, level, or navigation not available");
            return;
        }

        String blockName = task.getStringParameter("block");
        if (blockName == null || blockName.isEmpty()) {
            result = ActionResult.failure("Block name parameter is required");
            return;
        }

        int x = task.getIntParameter("x", 0);
        int y = task.getIntParameter("y", 0);
        int z = task.getIntParameter("z", 0);

        targetPos = new BlockPos(x, y, z);
        ticksRunning = 0;

        blockToPlace = parseBlock(blockName);

        if (blockToPlace == null || blockToPlace == Blocks.AIR) {
            result = ActionResult.failure("Invalid block type: " + blockName);
            return;
        }
    }

    @Override
    protected void onTick() {
        if (foreman == null || foreman.level() == null || targetPos == null || blockToPlace == null) {
            result = ActionResult.failure("Invalid state during block placement");
            return;
        }

        ticksRunning++;

        if (ticksRunning > MAX_TICKS) {
            result = ActionResult.failure("Place block timeout after " + MAX_TICKS + " ticks");
            return;
        }

        if (!foreman.blockPosition().closerThan(targetPos, 5.0)) {
            foreman.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.0);
            return;
        }

        BlockState currentState = foreman.level().getBlockState(targetPos);
        if (!currentState.isAir() && !currentState.liquid()) {
            result = ActionResult.failure("Position is not empty (occupied by " + currentState.getBlock() + ")");
            return;
        }

        foreman.level().setBlock(targetPos, blockToPlace.defaultBlockState(), 3);
        result = ActionResult.success("Placed " + blockToPlace.getName().getString());
    }

    @Override
    protected void onCancel() {
        if (foreman != null && foreman.getNavigation() != null) {
            foreman.getNavigation().stop();
        }
    }

    @Override
    public String getDescription() {
        String blockName = (blockToPlace != null) ? blockToPlace.getName().getString() : "unknown";
        return "Place " + blockName + " at " + (targetPos != null ? targetPos : "unknown");
    }

    private Block parseBlock(String blockName) {
        blockName = blockName.toLowerCase().replace(" ", "_");
        if (!blockName.contains(":")) {
            blockName = "minecraft:" + blockName;
        }
        ResourceLocation resourceLocation = new ResourceLocation(blockName);
        return BuiltInRegistries.BLOCK.get(resourceLocation);
    }
}

