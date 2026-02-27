package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;

public class GatherResourceAction extends BaseAction {
    private String resourceType;
    private int quantity;

    public GatherResourceAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        if (foreman == null || foreman.getNavigation() == null) {
            result = ActionResult.failure("Foreman or navigation not available");
            return;
        }

        resourceType = task.getStringParameter("resource");
        if (resourceType == null || resourceType.isEmpty()) {
            result = ActionResult.failure("Resource type parameter is required");
            return;
        }

        quantity = task.getIntParameter("quantity", 1);
        if (quantity <= 0) {
            result = ActionResult.failure("Quantity must be positive");
            return;
        }

        // This is essentially a smart wrapper around mining that:
        // - Mines them
        result = ActionResult.failure("Resource gathering not yet fully implemented", false);
    }

    @Override
    protected void onTick() {
        // No-op - marked as not implemented
    }

    @Override
    protected void onCancel() {
        if (foreman != null && foreman.getNavigation() != null) {
            foreman.getNavigation().stop();
        }
    }

    @Override
    public String getDescription() {
        return "Gather " + quantity + " " + (resourceType != null ? resourceType : "unknown");
    }
}

