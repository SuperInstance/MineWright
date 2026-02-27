package com.minewright.action.actions;

import com.minewright.MineWrightMod;
import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;

public class CraftItemAction extends BaseAction {
    private String itemName;
    private int quantity;
    private int ticksRunning;

    public CraftItemAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        itemName = task.getStringParameter("item");
        quantity = task.getIntParameter("quantity", 1);
        ticksRunning = 0;

        MineWrightMod.LOGGER.info("[{}] Craft action started: {}x {}", foreman.getEntityName(), quantity, itemName);

        // - Check if recipe exists
        // - Check if Foreman has ingredients
        // - Navigate to crafting table if needed

        MineWrightMod.LOGGER.warn("[{}] Crafting not yet implemented for item: {}", foreman.getEntityName(), itemName);
        result = ActionResult.failure("Crafting not yet implemented", false);
    }

    @Override
    protected void onTick() {
        ticksRunning++;
    }

    @Override
    protected void onCancel() {
        MineWrightMod.LOGGER.info("[{}] Craft action cancelled: {}x {}", foreman.getEntityName(), quantity, itemName);
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Craft " + quantity + " " + itemName;
    }
}

