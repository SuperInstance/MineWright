package com.minewright.animal.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Action to shear sheep and collect wool.
 *
 * <p>Sheep must be adults and have wool (not already sheared).
 * The foreman will approach each sheep and shear it, collecting
 * the wool drops.</p>
 */
public class ShearAction extends BaseAction {
    private Sheep currentTarget;
    private int sheepSheared = 0;
    private int targetCount = 0;
    private int ticksSearching = 0;
    private static final int MAX_TICKS = 1200; // 1 minute timeout
    private static final double SHEAR_RANGE = 3.5;

    public ShearAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        targetCount = task.getIntParameter("quantity", 0); // 0 = all nearby
        foreman.sendChatMessage("I'll shear the sheep");
    }

    @Override
    protected void onTick() {
        ticksSearching++;

        if (ticksSearching > MAX_TICKS) {
            result = ActionResult.success("Sheared " + sheepSheared + " sheep");
            return;
        }

        // Check if target count reached
        if (targetCount > 0 && sheepSheared >= targetCount) {
            result = ActionResult.success("Sheared " + sheepSheared + " sheep as requested");
            return;
        }

        // Find or validate target
        if (currentTarget == null || !currentTarget.isAlive()) {
            findNextSheep();
            if (currentTarget == null) {
                // No more sheep to shear
                result = ActionResult.success("Sheared " + sheepSheared + " sheep (no more found)");
                return;
            }
        }

        // Move towards sheep
        double distance = foreman.distanceTo(currentTarget);
        if (distance > SHEAR_RANGE) {
            foreman.getNavigation().moveTo(currentTarget, 1.2);
            return;
        }

        // Face the sheep
        foreman.getLookControl().setLookAt(currentTarget);

        // Shear the sheep
        if (canShear(currentTarget)) {
            shearSheep(currentTarget);
            sheepSheared++;

            // Notify progress every 5 sheep
            if (sheepSheared % 5 == 0) {
                foreman.sendChatMessage("Sheared " + sheepSheared + " sheep so far");
            }

            // Reset for next sheep
            currentTarget = null;
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Sheeping sheep (" + sheepSheared + " done)";
    }

    /**
     * Find the next sheep that needs shearing
     */
    private void findNextSheep() {
        AABB searchBox = foreman.getBoundingBox().inflate(32.0);
        List<Entity> entities = foreman.level().getEntities(foreman, searchBox);

        Sheep nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : entities) {
            if (!(entity instanceof Sheep sheep)) continue;
            if (!canShear(sheep)) continue;

            double distance = foreman.distanceTo(sheep);
            if (distance < nearestDistance) {
                nearest = sheep;
                nearestDistance = distance;
            }
        }

        currentTarget = nearest;
    }

    /**
     * Check if a sheep can be sheared
     */
    private boolean canShear(Sheep sheep) {
        if (!sheep.isAlive()) return false;
        if (sheep.isBaby()) return false; // Can't shear babies

        // In Minecraft, sheep ready for shearing return true to isShearable
        // We check if the sheep has wool (not already sheared)
        return !sheep.isSheared();
    }

    /**
     * Shear a sheep and collect wool
     */
    private void shearSheep(Sheep sheep) {
        // In actual Minecraft, shearing is done via ItemShear usage
        // We simulate the effect here

        // Mark sheep as sheared (visual change)
        sheep.shear(null); // null player = sheared by foreman

        // The sheep will drop wool items automatically in Minecraft
        // In a real implementation, we'd need to handle the drops

        foreman.notifyTaskCompleted("Sheared a " + sheep.getColor().getName() + " sheep");
    }
}
