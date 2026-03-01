package com.minewright.behavior;

import net.minecraft.world.level.Level;

/**
 * Simple test stub that provides the minimal interface needed for BTBlackboard in tests.
 * This avoids the complex initialization required by Minecraft entities.
 */
public class MockForemanEntity {

    private final String name;

    public MockForemanEntity(String name) {
        this.name = name;
    }

    public String getEntityName() {
        return name;
    }

    public Level level() {
        return null; // Not used in tests
    }
}
