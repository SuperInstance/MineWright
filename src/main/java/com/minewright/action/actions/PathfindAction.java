package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;

public class PathfindAction extends BaseAction {
    private BlockPos targetPos;
    private int ticksRunning;
    private static final int MAX_TICKS = 600; // 30 seconds timeout

    public PathfindAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        if (foreman == null || foreman.getNavigation() == null) {
            result = ActionResult.failure("Foreman or navigation not available");
            return;
        }

        int x = task.getIntParameter("x", 0);
        int y = task.getIntParameter("y", 0);
        int z = task.getIntParameter("z", 0);

        targetPos = new BlockPos(x, y, z);
        ticksRunning = 0;

        foreman.getNavigation().moveTo(x, y, z, 1.0);
    }

    @Override
    protected void onTick() {
        if (foreman == null || foreman.getNavigation() == null || targetPos == null) {
            result = ActionResult.failure("Invalid state during pathfinding");
            return;
        }

        ticksRunning++;

        if (foreman.blockPosition().closerThan(targetPos, 2.0)) {
            result = ActionResult.success("Reached target position");
            return;
        }

        if (ticksRunning > MAX_TICKS) {
            result = ActionResult.failure("Pathfinding timeout after " + MAX_TICKS + " ticks");
            return;
        }

        if (foreman.getNavigation().isDone() && !foreman.blockPosition().closerThan(targetPos, 2.0)) {
            foreman.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.0);
        }
    }

    @Override
    protected void onCancel() {
        if (foreman != null && foreman.getNavigation() != null) {
            foreman.getNavigation().stop();
        }
    }

    @Override
    public String getDescription() {
        return "Pathfind to " + (targetPos != null ? targetPos : "unknown");
    }
}

