package com.minewright.action.actions;

import com.minewright.MineWrightMod;
import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;

public abstract class BaseAction {
    protected final ForemanEntity foreman;
    protected final Task task;
    protected ActionResult result;
    protected boolean started = false;
    protected boolean cancelled = false;

    public BaseAction(ForemanEntity foreman, Task task) {
        this.foreman = foreman;
        this.task = task;
    }

    public void start() {
        if (started) {
            MineWrightMod.LOGGER.warn("[{}] Action already started: {}", foreman.getSteveName(), getDescription());
            return;
        }
        started = true;
        MineWrightMod.LOGGER.debug("[{}] Starting action: {}", foreman.getSteveName(), getDescription());
        onStart();
    }

    public void tick() {
        if (!started || isComplete()) return;
        try {
            onTick();
        } catch (Exception e) {
            MineWrightMod.LOGGER.error("[{}] Error during action tick: {}", foreman.getSteveName(), getDescription(), e);
            result = ActionResult.failure("Error during action execution: " + e.getMessage());
        }
    }

    public void cancel() {
        if (cancelled) {
            MineWrightMod.LOGGER.debug("[{}] Action already cancelled: {}", foreman.getSteveName(), getDescription());
            return;
        }
        cancelled = true;
        result = ActionResult.failure("Action cancelled");
        MineWrightMod.LOGGER.info("[{}] Cancelling action: {}", foreman.getSteveName(), getDescription());
        onCancel();
    }

    public boolean isComplete() {
        return result != null || cancelled;
    }

    public ActionResult getResult() {
        return result;
    }

    protected abstract void onStart();
    protected abstract void onTick();
    protected abstract void onCancel();

    public abstract String getDescription();
}

