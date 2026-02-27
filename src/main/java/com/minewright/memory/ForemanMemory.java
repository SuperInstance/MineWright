package com.minewright.memory;

import com.minewright.entity.ForemanEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ForemanMemory {
    private final ForemanEntity minewright;
    private String currentGoal;
    private final Deque<String> taskQueue;
    private final Deque<String> recentActions;
    private static final int MAX_RECENT_ACTIONS = 20;

    public ForemanMemory(ForemanEntity minewright) {
        this.minewright = minewright;
        this.currentGoal = "";
        this.taskQueue = new ArrayDeque<>();
        this.recentActions = new ArrayDeque<>();
    }

    public String getCurrentGoal() {
        return currentGoal;
    }

    public void setCurrentGoal(String goal) {
        this.currentGoal = goal;
    }

    public void addAction(String action) {
        recentActions.addFirst(action);
        if (recentActions.size() > MAX_RECENT_ACTIONS) {
            recentActions.removeLast();
        }
    }

    public List<String> getRecentActions(int count) {
        List<String> result = new ArrayList<>();
        for (String action : recentActions) {
            if (result.size() >= count) break;
            result.add(action);
        }
        return result;
    }

    public void clearTaskQueue() {
        taskQueue.clear();
        currentGoal = "";
    }

    public void saveToNBT(CompoundTag tag) {
        tag.putString("CurrentGoal", currentGoal);

        ListTag actionsList = new ListTag();
        for (String action : recentActions) {
            actionsList.add(StringTag.valueOf(action));
        }
        tag.put("RecentActions", actionsList);
    }

    public void loadFromNBT(CompoundTag tag) {
        currentGoal = tag.getString("CurrentGoal");

        ListTag actionsList = tag.getList("RecentActions", 8); // 8 = String type
        if (!actionsList.isEmpty()) {
            recentActions.clear();
            for (int i = 0; i < actionsList.size(); i++) {
                recentActions.add(actionsList.getString(i));
            }
        }
    }
}

