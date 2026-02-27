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
        resourceType = task.getStringParameter("resource");
        quantity = task.getIntParameter("quantity", 1);

        // This is essentially a smart wrapper around mining that:
        // - Mines them

        result = ActionResult.failure("Resource gathering not yet fully implemented", false);
    }

    @Override
    protected void onTick() {
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Gather " + quantity + " " + resourceType;
    }
}

