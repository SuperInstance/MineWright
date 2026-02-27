package com.minewright.testutil;

import com.minewright.action.Task;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder pattern for creating Task objects in tests.
 * Provides a fluent API for test data construction.
 */
public class TaskBuilder {
    private String action;
    private Map<String, Object> params = new HashMap<>();

    /**
     * Create a new task builder.
     */
    public static TaskBuilder aTask() {
        return new TaskBuilder();
    }

    /**
     * Create a new task builder with an action.
     */
    public static TaskBuilder aTask(String action) {
        return new TaskBuilder().withAction(action);
    }

    /**
     * Set the action type for this task.
     */
    public TaskBuilder withAction(String action) {
        this.action = action;
        return this;
    }

    /**
     * Add a parameter to the task.
     */
    public TaskBuilder withParam(String key, Object value) {
        this.params.put(key, value);
        return this;
    }

    /**
     * Add multiple parameters to the task.
     */
    public TaskBuilder withParams(Map<String, Object> params) {
        this.params.putAll(params);
        return this;
    }

    /**
     * Set the block parameter (common for mining/placing tasks).
     */
    public TaskBuilder withBlock(String block) {
        return withParam("block", block);
    }

    /**
     * Set the quantity parameter (common for gathering/crafting tasks).
     */
    public TaskBuilder withQuantity(int quantity) {
        return withParam("quantity", quantity);
    }

    /**
     * Set the position parameters (x, y, z).
     */
    public TaskBuilder withPosition(int x, int y, int z) {
        return withParam("x", x)
            .withParam("y", y)
            .withParam("z", z);
    }

    /**
     * Set the structure parameter (for building tasks).
     */
    public TaskBuilder withStructure(String structure) {
        return withParam("structure", structure);
    }

    /**
     * Set the item parameter (for crafting tasks).
     */
    public TaskBuilder withItem(String item) {
        return withParam("item", item);
    }

    /**
     * Set the target parameter (for attack/follow tasks).
     */
    public TaskBuilder withTarget(String target) {
        return withParam("target", target);
    }

    /**
     * Set the player parameter (for follow tasks).
     */
    public TaskBuilder withPlayer(String player) {
        return withParam("player", player);
    }

    /**
     * Build the task object.
     */
    public Task build() {
        return new Task(action, params);
    }

    /**
     * Common task presets for testing.
     */
    public static class Presets {
        /**
         * Create a standard mining task.
         */
        public static Task mineStone(int quantity) {
            return aTask("mine")
                .withBlock("stone")
                .withQuantity(quantity)
                .build();
        }

        /**
         * Create a standard building task.
         */
        public static Task buildHouse(String structure, int width, int height, int depth) {
            return aTask("build")
                .withStructure(structure)
                .withParam("blocks", new String[]{"oak_planks"})
                .withParam("dimensions", new int[]{width, height, depth})
                .build();
        }

        /**
         * Create a standard crafting task.
         */
        public static Task craftItem(String item, int quantity) {
            return aTask("craft")
                .withItem(item)
                .withQuantity(quantity)
                .build();
        }

        /**
         * Create a standard place block task.
         */
        public static Task placeBlock(String block, int x, int y, int z) {
            return aTask("place")
                .withBlock(block)
                .withPosition(x, y, z)
                .build();
        }

        /**
         * Create a standard pathfinding task.
         */
        public static Task pathfindTo(int x, int y, int z) {
            return aTask("pathfind")
                .withPosition(x, y, z)
                .build();
        }

        /**
         * Create an invalid task (missing required parameters).
         */
        public static Task invalidTask() {
            return aTask("invalid_action")
                .withParam("incomplete", true)
                .build();
        }
    }
}
