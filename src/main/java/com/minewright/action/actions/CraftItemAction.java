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
        if (foreman == null || foreman.getNavigation() == null) {
            result = ActionResult.failure("Foreman or navigation not available");
            return;
        }

        itemName = task.getStringParameter("item");
        if (itemName == null || itemName.isEmpty()) {
            result = ActionResult.failure("Item name parameter is required");
            return;
        }

        quantity = task.getIntParameter("quantity", 1);
        if (quantity <= 0) {
            result = ActionResult.failure("Quantity must be positive");
            return;
        }

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
        // No-op - marked as not implemented
    }

    @Override
    protected void onCancel() {
        if (foreman != null) {
            MineWrightMod.LOGGER.info("[{}] Craft action cancelled: {}x {}", foreman.getEntityName(), quantity, itemName);
            if (foreman.getNavigation() != null) {
                foreman.getNavigation().stop();
            }
        }
    }

    @Override
    public String getDescription() {
        return "Craft " + quantity + " " + (itemName != null ? itemName : "unknown");
    }
}

