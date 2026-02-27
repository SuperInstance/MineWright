# Wool and Concrete Farming for MineWright

**Version:** 1.0.0
**Last Updated:** 2026-02-27
**Status:** Design Document

## Table of Contents

1. [Overview](#overview)
2. [Sheep Farming Architecture](#sheep-farming-architecture)
3. [Wool Production System](#wool-production-system)
4. [Concrete Production Line](#concrete-production-line)
5. [Color Management](#color-management)
6. [Collection Systems](#collection-systems)
7. [Multi-Color Coordination](#multi-color-coordination)
8. [Code Examples](#code-examples)
9. [Implementation Roadmap](#implementation-roadmap)
10. [Integration Guide](#integration-guide)

---

## Overview

The Wool and Concrete Farming system enables MineWright Foreman entities to autonomously manage large-scale wool production and concrete manufacturing. This includes automated sheep shearing, dye crafting, concrete powder hardening, and multi-color coordination for building projects.

### Key Features

- **Automated Sheep Farming**: Color-organized sheep pens with automated shearing
- **Wool Collection**: Efficient wool gathering with sorting by color
- **Dye Production**: Automated crafting of all 16 Minecraft dye colors
- **Concrete Manufacturing**: Convert sand/gravel/dye into concrete powder, then harden to concrete
- **Color Coordination**: Multi-foreman coordination for simultaneous multi-color production
- **Inventory Management**: Smart sorting and storage of colored materials

### Resource Flow

```
Grass/Sand ──> Sheep Feed ──> Wool Growth ──> Shearing ──> Wool
                                       │
                                       v
                       Dye Crafting (Flowers/Plants)
                                       │
                                       v
                       Concrete Powder (Sand + Gravel + Dye)
                                       │
                                       v
                       Water Hardening ──> Concrete Blocks
```

---

## Sheep Farming Architecture

### Sheep Pen Design

#### Multi-Color Pen Layout

```
┌─────────────────────────────────────────────────────────┐
│                    SHEEP FARM COMPLEX                   │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐      │
│  │WHITE│ │ORANGE│ │MAGENTA│ │LIGHT BLUE│ │YELLOW│ │LIME│  │
│  └─────┘ └─────┘ └─────┘ └─────┘ └─────┘ └─────┘      │
│                                                         │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐      │
│  │PINK │ │GRAY │ │LIGHT_GRAY│ │CYAN│ │PURPLE│ │BLUE│   │
│  └─────┘ └─────┘ └─────┘ └─────┘ └─────┘ └─────┘      │
│                                                         │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐                      │
│  │BROWN│ │GREEN│ │RED │ │BLACK│                      │
│  └─────┘ └─────┘ └─────┘ └─────┘                      │
│                                                         │
│  Central: Storage + Shearing Station + Breeding Area   │
└─────────────────────────────────────────────────────────┘
```

#### Pen Specifications

Each colored sheep pen should be:
- **Size**: 8x8 blocks minimum, 16x16 recommended
- **Height**: 3 blocks (fence + 1 block headroom)
- **Floor**: Grass blocks (for sheep grazing and regrowth)
- **Lighting**: Glowstone or lanterns (level 7+ to prevent spawning)
- **Capacity**: 4-8 sheep per pen
- **Features**:
  - Grass floor for eating
  - Water trough (2x1 water source)
  - Feeder with wheat
  - Fence gate for Foreman access
  - Carpet floor matching sheep color (for visual organization)

### SheepPenGenerator

```java
package com.minewright.farming;

import com.minewright.structure.BlockPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FenceBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates automated sheep pen structures optimized for wool farming
 */
public class SheepPenGenerator {

    /**
     * Generate a complete sheep pen with all amenities
     */
    public static List<BlockPlacement> generateSheepPen(
        BlockPos center,
        int width,
        int depth,
        String colorName
    ) {
        List<BlockPlacement> blocks = new ArrayList<>();

        // Get wool block for the floor
        var woolBlock = getWoolBlock(colorName);
        var fenceBlock = Blocks.OAK_FENCE;
        var fenceGate = Blocks.OAK_FENCE_GATE;

        // Floor - alternating grass and colored wool
        for (int x = -width/2; x < width/2; x++) {
            for (int z = -depth/2; z < depth/2; z++) {
                // Checkerboard pattern: grass and colored wool
                boolean isGrass = (x + z) % 2 == 0;
                var floorBlock = isGrass ? Blocks.GRASS_BLOCK : woolBlock;
                blocks.add(new BlockPlacement(
                    center.offset(x, 0, z),
                    floorBlock.defaultBlockState()
                ));
            }
        }

        // Fence walls (height 2)
        for (int y = 1; y <= 2; y++) {
            for (int x = -width/2; x < width/2; x++) {
                // North fence
                blocks.add(new BlockPlacement(
                    center.offset(x, y, -depth/2),
                    fenceBlock.defaultBlockState()
                ));
                // South fence
                blocks.add(new BlockPlacement(
                    center.offset(x, y, depth/2),
                    fenceBlock.defaultBlockState()
                ));
            }

            for (int z = -depth/2; z < depth/2; z++) {
                // West fence
                blocks.add(new BlockPlacement(
                    center.offset(-width/2, y, z),
                    fenceBlock.defaultBlockState()
                ));
                // East fence
                blocks.add(new BlockPlacement(
                    center.offset(width/2, y, z),
                    fenceBlock.defaultBlockState()
                ));
            }
        }

        // Fence gate (south side, center)
        blocks.add(new BlockPlacement(
            center.offset(0, 1, depth/2),
            fenceGate.defaultBlockState()
        ));

        // Water trough (north side)
        for (int x = -1; x <= 1; x++) {
            blocks.add(new BlockPlacement(
                center.offset(x, 1, -depth/2 + 1),
                Blocks.WATER.defaultBlockState()
            ));
        }

        // Feeder (hay block)
        blocks.add(new BlockPlacement(
            center.offset(0, 1, 0),
            Blocks.HAY_BLOCK.defaultBlockState()
        ));

        // Lighting (4 corners)
        blocks.add(new BlockPlacement(
            center.offset(-width/2 + 1, 2, -depth/2 + 1),
            Blocks.LANTERN.defaultBlockState()
        ));
        blocks.add(new BlockPlacement(
            center.offset(width/2 - 1, 2, -depth/2 + 1),
            Blocks.LANTERN.defaultBlockState()
        ));
        blocks.add(new BlockPlacement(
            center.offset(-width/2 + 1, 2, depth/2 - 1),
            Blocks.LANTERN.defaultBlockState()
        ));
        blocks.add(new BlockPlacement(
            center.offset(width/2 - 1, 2, depth/2 - 1),
            Blocks.LANTERN.defaultBlockState()
        ));

        return blocks;
    }

    /**
     * Generate a multi-pen complex for all 16 colors
     */
    public static List<BlockPlacement> generateSheepFarmComplex(BlockPos origin) {
        List<BlockPlacement> allBlocks = new ArrayList<>();

        String[] colors = {
            "white", "orange", "magenta", "light_blue",
            "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue",
            "brown", "green", "red", "black"
        };

        int pensPerRow = 4;
        int penWidth = 10;
        int penDepth = 10;
        int spacing = 2;

        for (int i = 0; i < colors.length; i++) {
            int row = i / pensPerRow;
            int col = i % pensPerRow;

            BlockPos penCenter = origin.offset(
                col * (penWidth + spacing),
                0,
                row * (penDepth + spacing)
            );

            allBlocks.addAll(generateSheepPen(
                penCenter,
                penWidth,
                penDepth,
                colors[i]
            ));
        }

        // Add central storage building
        int totalWidth = pensPerRow * (penWidth + spacing);
        int totalDepth = 4 * (penDepth + spacing);
        BlockPos storageCenter = origin.offset(
            totalWidth / 2,
            0,
            totalDepth / 2
        );

        allBlocks.addAll(generateStorageBuilding(storageCenter));

        return allBlocks;
    }

    private static List<BlockPlacement> generateStorageBuilding(BlockPos center) {
        List<BlockPlacement> blocks = new ArrayList<>();

        // Simple storage shed structure
        int width = 12;
        int depth = 12;
        int height = 4;

        // Floor
        for (int x = -width/2; x < width/2; x++) {
            for (int z = -depth/2; z < depth/2; z++) {
                blocks.add(new BlockPlacement(
                    center.offset(x, 0, z),
                    Blocks.SMOOTH_STONE.defaultBlockState()
                ));
            }
        }

        // Walls (stone bricks)
        for (int y = 1; y <= height; y++) {
            for (int x = -width/2; x < width/2; x++) {
                for (int z = -depth/2; z < depth/2; z++) {
                    boolean isEdge = (x == -width/2 || x == width/2 - 1 ||
                                     z == -depth/2 || z == depth/2 - 1);
                    boolean isDoor = (y <= 2 && x == 0 && z == depth/2 - 1);

                    if (isEdge && !isDoor) {
                        blocks.add(new BlockPlacement(
                            center.offset(x, y, z),
                            Blocks.STONE_BRICKS.defaultBlockState()
                        ));
                    }
                }
            }
        }

        // Roof (oak planks)
        for (int x = -width/2 - 1; x < width/2 + 1; x++) {
            for (int z = -depth/2 - 1; z < depth/2 + 1; z++) {
                blocks.add(new BlockPlacement(
                    center.offset(x, height + 1, z),
                    Blocks.OAK_PLANKS.defaultBlockState()
                ));
            }
        }

        // Add chests for storage
        blocks.add(new BlockPlacement(
            center.offset(-3, 1, -3),
            Blocks.CHEST.defaultBlockState()
        ));
        blocks.add(new BlockPlacement(
            center.offset(3, 1, -3),
            Blocks.CHEST.defaultBlockState()
        ));
        blocks.add(new BlockPlacement(
            center.offset(-3, 1, 3),
            Blocks.BARREL.defaultBlockState()
        ));
        blocks.add(new BlockPlacement(
            center.offset(3, 1, 3),
            Blocks.BARREL.defaultBlockState()
        ));

        // Add crafting table
        blocks.add(new BlockPlacement(
            center.offset(0, 1, 0),
            Blocks.CRAFTING_TABLE.defaultBlockState()
        ));

        return blocks;
    }

    private static net.minecraft.world.level.block.Block getWoolBlock(String colorName) {
        return switch (colorName.toLowerCase()) {
            case "white" -> Blocks.WHITE_WOOL;
            case "orange" -> Blocks.ORANGE_WOOL;
            case "magenta" -> Blocks.MAGENTA_WOOL;
            case "light_blue" -> Blocks.LIGHT_BLUE_WOOL;
            case "yellow" -> Blocks.YELLOW_WOOL;
            case "lime" -> Blocks.LIME_WOOL;
            case "pink" -> Blocks.PINK_WOOL;
            case "gray" -> Blocks.GRAY_WOOL;
            case "light_gray" -> Blocks.LIGHT_GRAY_WOOL;
            case "cyan" -> Blocks.CYAN_WOOL;
            case "purple" -> Blocks.PURPLE_WOOL;
            case "blue" -> Blocks.BLUE_WOOL;
            case "brown" -> Blocks.BROWN_WOOL;
            case "green" -> Blocks.GREEN_WOOL;
            case "red" -> Blocks.RED_WOOL;
            case "black" -> Blocks.BLACK_WOOL;
            default -> Blocks.WHITE_WOOL;
        };
    }
}
```

---

## Wool Production System

### SheepClassification

```java
package com.minewright.farming;

import net.minecraft.world.entity.animal.Sheep;

/**
 * Manages sheep color classification and tracking
 */
public class SheepClassification {

    public enum WoolColor {
        WHITE(0, "white", "white_wool"),
        ORANGE(1, "orange", "orange_wool"),
        MAGENTA(2, "magenta", "magenta_wool"),
        LIGHT_BLUE(3, "light_blue", "light_blue_wool"),
        YELLOW(4, "yellow", "yellow_wool"),
        LIME(5, "lime", "lime_wool"),
        PINK(6, "pink", "pink_wool"),
        GRAY(7, "gray", "gray_wool"),
        LIGHT_GRAY(8, "light_gray", "light_gray_wool"),
        CYAN(9, "cyan", "cyan_wool"),
        PURPLE(10, "purple", "purple_wool"),
        BLUE(11, "blue", "blue_wool"),
        BROWN(12, "brown", "brown_wool"),
        GREEN(13, "green", "green_wool"),
        RED(14, "red", "red_wool"),
        BLACK(15, "black", "black_wool");

        private final int colorIndex;
        private final String colorName;
        private final String woolBlockName;

        WoolColor(int colorIndex, String colorName, String woolBlockName) {
            this.colorIndex = colorIndex;
            this.colorName = colorName;
            this.woolBlockName = woolBlockName;
        }

        public int getColorIndex() { return colorIndex; }
        public String getColorName() { return colorName; }
        public String getWoolBlockName() { return woolBlockName; }

        /**
         * Get WoolColor from Minecraft sheep color byte
         */
        public static WoolColor fromByte(byte colorByte) {
            int index = colorByte & 0xFF; // Convert to unsigned
            if (index >= 0 && index < values().length) {
                return values()[index];
            }
            return WHITE;
        }

        /**
         * Get WoolColor from color name string
         */
        public static WoolColor fromName(String name) {
            for (WoolColor color : values()) {
                if (color.colorName.equalsIgnoreCase(name)) {
                    return color;
                }
            }
            return WHITE; // Default
        }
    }

    /**
     * Get the wool color from a sheep entity
     */
    public static WoolColor getSheepColor(Sheep sheep) {
        byte colorByte = sheep.getColor();
        return WoolColor.fromByte(colorByte);
    }

    /**
     * Check if sheep is ready for shearing (has wool grown)
     */
    public static boolean isShearable(Sheep sheep) {
        return !sheep.isSheared(); // In Minecraft, this checks if sheep has wool
    }
}
```

### ShearAction

```java
package com.minewright.farming.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.farming.SheepClassification;
import com.minewright.farming.SheepClassification.WoolColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Action to shear sheep and collect wool
 */
public class ShearAction extends BaseAction {
    private List<Sheep> sheepToShear;
    private int sheepIndex = 0;
    private int shearedCount = 0;
    private final List<WoolCollection> collectedWool = new ArrayList<>();
    private int ticksSinceLastShear = 0;
    private static final int TICKS_BETWEEN_SHEARS = 20; // 1 second between shears

    public ShearAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        String colorParam = task.getStringParameter("color", "all");
        int maxSheep = task.getIntParameter("quantity", 16);

        sheepToShear = findSheepToShear(colorParam, maxSheep);

        if (sheepToShear.isEmpty()) {
            result = ActionResult.failure("No shearable sheep found");
            return;
        }

        foreman.sendChatMessage("Found " + sheepToShear.size() + " sheep to shear");
    }

    @Override
    protected void onTick() {
        if (sheepIndex >= sheepToShear.size()) {
            result = ActionResult.success("Sheared " + shearedCount + " sheep");
            return;
        }

        Sheep sheep = sheepToShear.get(sheepIndex);

        // Check if sheep still valid
        if (!sheep.isAlive()) {
            sheepIndex++;
            return;
        }

        // Check if still shearable
        if (!SheepClassification.isShearable(sheep)) {
            sheepIndex++;
            return;
        }

        // Move towards sheep
        double distance = foreman.distanceTo(sheep);
        if (distance > 3.0) {
            foreman.getNavigation().moveTo(sheep, 1.2);
            return;
        }

        // Wait between shears
        ticksSinceLastShear++;
        if (ticksSinceLastShear < TICKS_BETWEEN_SHEARS) {
            return;
        }
        ticksSinceLastShear = 0;

        // Face the sheep
        foreman.getLookControl().setLookAt(sheep);

        // Shear the sheep
        if (shearSheep(sheep)) {
            WoolColor color = SheepClassification.getSheepColor(sheep);
            collectedWool.add(new WoolCollection(color, 1 + foreman.getRandom().nextInt(3)));
            shearedCount++;
            sheepIndex++;

            // Notify progress
            if (shearedCount % 5 == 0) {
                foreman.sendChatMessage("Sheared " + shearedCount + "/" + sheepToShear.size());
            }
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Shearing " + (sheepToShear != null ? sheepToShear.size() : 0) + " sheep";
    }

    /**
     * Find sheep that can be sheared
     */
    private List<Sheep> findSheepToShear(String colorParam, int maxSheep) {
        AABB searchBox = foreman.getBoundingBox().inflate(32.0);
        List<Entity> entities = foreman.level().getEntities(foreman, searchBox);

        List<Sheep> shearableSheep = new ArrayList<>();

        for (Entity entity : entities) {
            if (!(entity instanceof Sheep sheep)) continue;
            if (!SheepClassification.isShearable(sheep)) continue;

            // Filter by color if specified
            if (!colorParam.equals("all")) {
                WoolColor color = SheepClassification.getSheepColor(sheep);
                if (!color.getColorName().equals(colorParam.toLowerCase())) {
                    continue;
                }
            }

            shearableSheep.add(sheep);
            if (shearableSheep.size() >= maxSheep) break;
        }

        return shearableSheep;
    }

    /**
     * Shear a sheep and return wool
     */
    private boolean shearSheep(Sheep sheep) {
        // In Minecraft, sheep drop 1-3 wool when sheared
        // We need to simulate this

        if (!sheep.isAlive()) return false;

        // Set sheep as sheared
        sheep.setSheared(true);

        // Drop wool items at sheep's position
        WoolColor color = SheepClassification.getSheepColor(sheep);
        int woolAmount = 1 + foreman.getRandom().nextInt(3);

        // In a real implementation, we would spawn ItemEntity or add to inventory
        // For now, we track it
        for (int i = 0; i < woolAmount; i++) {
            ItemStack woolStack = getWoolItem(color);
            // Add to foreman's inventory or drop
            // inventory.add(woolStack);
        }

        return true;
    }

    private ItemStack getWoolItem(WoolColor color) {
        return switch (color) {
            case WHITE -> new ItemStack(Items.WHITE_WOOL);
            case ORANGE -> new ItemStack(Items.ORANGE_WOOL);
            case MAGENTA -> new ItemStack(Items.MAGENTA_WOOL);
            case LIGHT_BLUE -> new ItemStack(Items.LIGHT_BLUE_WOOL);
            case YELLOW -> new ItemStack(Items.YELLOW_WOOL);
            case LIME -> new ItemStack(Items.LIME_WOOL);
            case PINK -> new ItemStack(Items.PINK_WOOL);
            case GRAY -> new ItemStack(Items.GRAY_WOOL);
            case LIGHT_GRAY -> new ItemStack(Items.LIGHT_GRAY_WOOL);
            case CYAN -> new ItemStack(Items.CYAN_WOOL);
            case PURPLE -> new ItemStack(Items.PURPLE_WOOL);
            case BLUE -> new ItemStack(Items.BLUE_WOOL);
            case BROWN -> new ItemStack(Items.BROWN_WOOL);
            case GREEN -> new ItemStack(Items.GREEN_WOOL);
            case RED -> new ItemStack(Items.RED_WOOL);
            case BLACK -> new ItemStack(Items.BLACK_WOOL);
        };
    }

    public List<WoolCollection> getCollectedWool() {
        return collectedWool;
    }

    public record WoolCollection(WoolColor color, int amount) {}
}
```

### SheepBreedingAction

```java
package com.minewright.farming.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.farming.SheepClassification.WoolColor;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Breed sheep to expand the flock
 */
public class SheepBreedingAction extends BaseAction {
    private Sheep parent1, parent2;
    private WoolColor targetColor;
    private int breedingAttempts = 0;
    private static final int MAX_ATTEMPTS = 100;

    public SheepBreedingAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        String colorParam = task.getStringParameter("color", "white");
        targetColor = WoolColor.fromName(colorParam);

        // Find breeding pair
        var pair = findBreedingPair(targetColor);
        if (pair == null) {
            result = ActionResult.failure("No breeding pair found for " + colorParam);
            return;
        }

        parent1 = pair.parent1();
        parent2 = pair.parent2();

        foreman.sendChatMessage("Breeding " + colorParam + " sheep");
    }

    @Override
    protected void onTick() {
        breedingAttempts++;

        if (breedingAttempts > MAX_ATTEMPTS) {
            result = ActionResult.failure("Breeding timed out");
            return;
        }

        // Check if parents still valid
        if (!parent1.isAlive() || !parent2.isAlive()) {
            result = ActionResult.failure("Parent sheep died");
            return;
        }

        // Check if breeding complete (baby spawned)
        if (isBreedingComplete()) {
            result = ActionResult.success("Successfully bred sheep");
            return;
        }

        // Move close to parents
        double dist1 = foreman.distanceTo(parent1);
        if (dist1 > 6.0) {
            foreman.getNavigation().moveTo(parent1, 1.0);
            return;
        }

        // Feed the parents
        feedSheep(parent1);
        feedSheep(parent2);
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Breeding " + targetColor.getColorName() + " sheep";
    }

    private BreedingPair findBreedingPair(WoolColor color) {
        var searchBox = foreman.getBoundingBox().inflate(32.0);
        var entities = foreman.level().getEntities(foreman, searchBox);

        List<Sheep> matchingSheep = new java.util.ArrayList<>();

        for (var entity : entities) {
            if (!(entity instanceof Sheep sheep)) continue;
            if (!sheep.isAlive()) continue;

            // Check color
            var sheepColor = com.minewright.farming.SheepClassification.getSheepColor(sheep);
            if (sheepColor != color) continue;

            // Check if adult
            if (!(sheep instanceof AgeableMob ageable)) continue;
            if (ageable.getAge() != 0) continue; // Skip babies

            matchingSheep.add(sheep);
            if (matchingSheep.size() >= 2) break;
        }

        if (matchingSheep.size() >= 2) {
            return new BreedingPair(matchingSheep.get(0), matchingSheep.get(1));
        }

        return null;
    }

    private void feedSheep(Sheep sheep) {
        // Feed wheat to put in love mode
        if (sheep instanceof AgeableMob ageable) {
            ageable.setAge(0);
            ageable.setInLoveTime(600);
        }
    }

    private boolean isBreedingComplete() {
        if (parent1 instanceof AgeableMob ageable) {
            return !ageable.isInLove(); // Love mode ended = baby spawned
        }
        return false;
    }

    private record BreedingPair(Sheep parent1, Sheep parent2) {}
}
```

---

## Concrete Production Line

### ConcreteCraftingAction

```java
package com.minewright.farming.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.farming.SheepClassification.WoolColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;

/**
 * Craft concrete powder from sand, gravel, and dye
 */
public class ConcreteCraftingAction extends BaseAction {
    private WoolColor targetColor;
    private int quantity;
    private int craftedCount = 0;
    private BlockPos craftingPosition;
    private final Map<WoolColor, Integer> targetAmounts = new HashMap<>();

    public ConcreteCraftingAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        String colorParam = task.getStringParameter("color", "white");
        targetColor = WoolColor.fromName(colorParam);
        quantity = task.getIntParameter("quantity", 64);

        // Parse multi-color request
        String colorsParam = task.getStringParameter("colors", null);
        if (colorsParam != null) {
            String[] colorParts = colorsParam.split(",");
            for (String part : colorParts) {
                String[] keyValue = part.split(":");
                if (keyValue.length == 2) {
                    WoolColor color = WoolColor.fromName(keyValue[0].trim());
                    int amount = Integer.parseInt(keyValue[1].trim());
                    targetAmounts.put(color, amount);
                }
            }
        } else {
            targetAmounts.put(targetColor, quantity);
        }

        // Find crafting position
        craftingPosition = findCraftingPosition();
        if (craftingPosition == null) {
            result = ActionResult.failure("No crafting table found");
            return;
        }

        foreman.sendChatMessage("Crafting " + quantity + " " + colorParam + " concrete powder");
    }

    @Override
    protected void onTick() {
        if (targetAmounts.isEmpty()) {
            result = ActionResult.success("Crafted " + craftedCount + " concrete powder");
            return;
        }

        // Process each color
        for (var entry : targetAmounts.entrySet()) {
            WoolColor color = entry.getKey();
            int remaining = entry.getValue();

            if (remaining <= 0) continue;

            // Craft one batch of this color
            if (craftConcretePowder(color, Math.min(remaining, 64))) {
                int crafted = Math.min(remaining, 64);
                targetAmounts.put(color, remaining - crafted);
                craftedCount += crafted;
            }

            // Move to crafting position
            double distance = foreman.blockPosition().distSqr(craftingPosition);
            if (distance > 9.0) {
                foreman.getNavigation().moveTo(craftingPosition.getX(), craftingPosition.getY(), craftingPosition.getZ(), 1.0);
                return;
            }

            return; // Process one color per tick
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Crafting concrete powder";
    }

    private BlockPos findCraftingPosition() {
        // Find nearest crafting table
        var searchBox = foreman.getBoundingBox().inflate(16.0);
        var entities = foreman.level().getBlockEntities(craftingPosition);

        // For now, use foreman's position
        return foreman.blockPosition();
    }

    private boolean craftConcretePowder(WoolColor color, int amount) {
        // Recipe: 4 sand + 4 gravel + 1 dye = 8 concrete powder
        var dyeItem = getDyeItem(color);
        var concretePowder = getConcretePowderBlock(color);

        // Check if we have resources (simplified)
        // In real implementation, check inventory

        // Craft the concrete powder
        // This would use Minecraft's crafting system

        return true;
    }

    private ItemStack getDyeItem(WoolColor color) {
        return switch (color) {
            case WHITE -> new ItemStack(Items.WHITE_DYE);
            case ORANGE -> new ItemStack(Items.ORANGE_DYE);
            case MAGENTA -> new ItemStack(Items.MAGENTA_DYE);
            case LIGHT_BLUE -> new ItemStack(Items.LIGHT_BLUE_DYE);
            case YELLOW -> new ItemStack(Items.YELLOW_DYE);
            case LIME -> new ItemStack(Items.LIME_DYE);
            case PINK -> new ItemStack(Items.PINK_DYE);
            case GRAY -> new ItemStack(Items.GRAY_DYE);
            case LIGHT_GRAY -> new ItemStack(Items.LIGHT_GRAY_DYE);
            case CYAN -> new ItemStack(Items.CYAN_DYE);
            case PURPLE -> new ItemStack(Items.PURPLE_DYE);
            case BLUE -> new ItemStack(Items.BLUE_DYE);
            case BROWN -> new ItemStack(Items.BROWN_DYE);
            case GREEN -> new ItemStack(Items.GREEN_DYE);
            case RED -> new ItemStack(Items.RED_DYE);
            case BLACK -> new ItemStack(Items.BLACK_DYE);
        };
    }

    private net.minecraft.world.level.block.Block getConcretePowderBlock(WoolColor color) {
        return switch (color) {
            case WHITE -> Blocks.WHITE_CONCRETE_POWDER;
            case ORANGE -> Blocks.ORANGE_CONCRETE_POWDER;
            case MAGENTA -> Blocks.MAGENTA_CONCRETE_POWDER;
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_CONCRETE_POWDER;
            case YELLOW -> Blocks.YELLOW_CONCRETE_POWDER;
            case LIME -> Blocks.LIME_CONCRETE_POWDER;
            case PINK -> Blocks.PINK_CONCRETE_POWDER;
            case GRAY -> Blocks.GRAY_CONCRETE_POWDER;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_CONCRETE_POWDER;
            case CYAN -> Blocks.CYAN_CONCRETE_POWDER;
            case PURPLE -> Blocks.PURPLE_CONCRETE_POWDER;
            case BLUE -> Blocks.BLUE_CONCRETE_POWDER;
            case BROWN -> Blocks.BROWN_CONCRETE_POWDER;
            case GREEN -> Blocks.GREEN_CONCRETE_POWDER;
            case RED -> Blocks.RED_CONCRETE_POWDER;
            case BLACK -> Blocks.BLACK_CONCRETE_POWDER;
        };
    }
}
```

### ConcreteHardeningAction

```java
package com.minewright.farming.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.farming.SheepClassification.WoolColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Place concrete powder in water to harden into concrete
 */
public class ConcreteHardeningAction extends BaseAction {
    private WoolColor targetColor;
    private int quantity;
    private BlockPos hardeningAreaStart;
    private BlockPos hardeningAreaEnd;
    private int hardenedCount = 0;
    private final List<BlockPos> placementQueue = new ArrayList<>();
    private int placementIndex = 0;

    public ConcreteHardeningAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        String colorParam = task.getStringParameter("color", "white");
        targetColor = WoolColor.fromName(colorParam);
        quantity = task.getIntParameter("quantity", 64);

        // Determine hardening area
        hardeningAreaStart = task.getBlockPosParameter("area_start");
        hardeningAreaEnd = task.getBlockPosParameter("area_end");

        if (hardeningAreaStart == null) {
            // Find water area near foreman
            hardeningAreaStart = findWaterArea();
            if (hardeningAreaStart == null) {
                result = ActionResult.failure("No water found for hardening concrete");
                return;
            }
            hardeningAreaEnd = hardeningAreaStart.offset(quantity, 0, 1);
        }

        // Generate placement positions
        generatePlacementQueue();

        foreman.sendChatMessage("Hardening " + quantity + " " + colorParam + " concrete");
    }

    @Override
    protected void onTick() {
        if (placementIndex >= placementQueue.size()) {
            result = ActionResult.success("Hardened " + hardenedCount + " concrete blocks");
            return;
        }

        BlockPos currentPos = placementQueue.get(placementIndex);

        // Move towards placement position
        if (foreman.blockPosition().distSqr(currentPos) > 9.0) {
            foreman.getNavigation().moveTo(currentPos, 1.0);
            return;
        }

        // Place concrete powder above water
        if (placeConcretePowder(currentPos)) {
            hardenedCount++;
            placementIndex++;
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Hardening concrete";
    }

    private BlockPos findWaterArea() {
        BlockPos searchStart = foreman.blockPosition();
        int searchRadius = 16;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                for (int y = -3; y <= 3; y++) {
                    BlockPos pos = searchStart.offset(x, y, z);
                    BlockState state = foreman.level().getBlockState(pos);

                    if (state.is(Blocks.WATER)) {
                        // Found water, return position above it
                        return pos.above();
                    }
                }
            }
        }

        return null;
    }

    private void generatePlacementQueue() {
        int x1 = Math.min(hardeningAreaStart.getX(), hardeningAreaEnd.getX());
        int x2 = Math.max(hardeningAreaStart.getX(), hardeningAreaEnd.getX());
        int z1 = Math.min(hardeningAreaStart.getZ(), hardeningAreaEnd.getZ());
        int z2 = Math.max(hardeningAreaStart.getZ(), hardeningAreaEnd.getZ());
        int y = hardeningAreaStart.getY();

        for (int x = x1; x <= x2 && placementQueue.size() < quantity; x++) {
            for (int z = z1; z <= z2 && placementQueue.size() < quantity; z++) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockPos belowPos = pos.below();

                // Check if there's water below
                BlockState belowState = foreman.level().getBlockState(belowPos);
                if (belowState.is(Blocks.WATER)) {
                    placementQueue.add(pos);
                }
            }
        }
    }

    private boolean placeConcretePowder(BlockPos pos) {
        var concretePowder = getConcretePowderBlock(targetColor);

        // Place the concrete powder
        foreman.level().setBlock(pos, concretePowder.defaultBlockState(), 3);

        // In Minecraft, concrete powder hardens when it touches water
        // This happens automatically, so we don't need to do anything else

        return true;
    }

    private net.minecraft.world.level.block.Block getConcretePowderBlock(WoolColor color) {
        return switch (color) {
            case WHITE -> Blocks.WHITE_CONCRETE_POWDER;
            case ORANGE -> Blocks.ORANGE_CONCRETE_POWDER;
            case MAGENTA -> Blocks.MAGENTA_CONCRETE_POWDER;
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_CONCRETE_POWDER;
            case YELLOW -> Blocks.YELLOW_CONCRETE_POWDER;
            case LIME -> Blocks.LIME_CONCRETE_POWDER;
            case PINK -> Blocks.PINK_CONCRETE_POWDER;
            case GRAY -> Blocks.GRAY_CONCRETE_POWDER;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_CONCRETE_POWDER;
            case CYAN -> Blocks.CYAN_CONCRETE_POWDER;
            case PURPLE -> Blocks.PURPLE_CONCRETE_POWDER;
            case BLUE -> Blocks.BLUE_CONCRETE_POWDER;
            case BROWN -> Blocks.BROWN_CONCRETE_POWDER;
            case GREEN -> Blocks.GREEN_CONCRETE_POWDER;
            case RED -> Blocks.RED_CONCRETE_POWDER;
            case BLACK -> Blocks.BLACK_CONCRETE_POWDER;
        };
    }
}
```

---

## Color Management

### ColorPaletteManager

```java
package com.minewright.farming;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * Manages dye production and inventory for all 16 colors
 */
public class ColorPaletteManager {

    private final Map<WoolColor, DyeInventory> dyeInventory = new EnumMap<>(WoolColor.class);
    private final Map<WoolColor, DyeRecipe> dyeRecipes = new EnumMap<>(WoolColor.class);

    public ColorPaletteManager() {
        initializeRecipes();
        initializeInventory();
    }

    private void initializeRecipes() {
        dyeRecipes.put(WoolColor.WHITE, new DyeRecipe(
            List.of(new Ingredient(Items.BONE_MEAL, 1)),
            Items.WHITE_DYE
        ));
        dyeRecipes.put(WoolColor.ORANGE, new DyeRecipe(
            List.of(new Ingredient(Items.ORANGE_TULIP, 1), new Ingredient(Items.RED_DYE, 1), new Ingredient(Items.YELLOW_DYE, 1)),
            Items.ORANGE_DYE
        ));
        dyeRecipes.put(WoolColor.MAGENTA, new DyeRecipe(
            List.of(new Ingredient(Items.LILAC, 1), new Ingredient(Items.ALLIUM, 1)),
            Items.MAGENTA_DYE
        ));
        dyeRecipes.put(WoolColor.LIGHT_BLUE, new DyeRecipe(
            List.of(new Ingredient(Items.BLUE_ORCHID, 1)),
            Items.LIGHT_BLUE_DYE
        ));
        dyeRecipes.put(WoolColor.YELLOW, new DyeRecipe(
            List.of(new Ingredient(Items.DANDELION, 1), new Ingredient(Items.SUNFLOWER, 1)),
            Items.YELLOW_DYE
        ));
        dyeRecipes.put(WoolColor.LIME, new DyeRecipe(
            List.of(new Ingredient(Items.SLIME_BALL, 1)),
            Items.LIME_DYE
        ));
        dyeRecipes.put(WoolColor.PINK, new DyeRecipe(
            List.of(new Ingredient(Items.PINK_TULIP, 1), new Ingredient(Items.PEONY, 1)),
            Items.PINK_DYE
        ));
        dyeRecipes.put(WoolColor.GRAY, new DyeRecipe(
            List.of(new Ingredient(Items.AZURE_BLUET, 1), new Ingredient(Items.INK_SAC, 1)),
            Items.GRAY_DYE
        ));
        dyeRecipes.put(WoolColor.LIGHT_GRAY, new DyeRecipe(
            List.of(new Ingredient(Items.WHITE_TULIP, 1), new Ingredient(Items.OXEYE_DAISY, 1)),
            Items.LIGHT_GRAY_DYE
        ));
        dyeRecipes.put(WoolColor.CYAN, new DyeRecipe(
            List.of(new Ingredient(Items.LILY_PAD, 1)),
            Items.CYAN_DYE
        ));
        dyeRecipes.put(WoolColor.PURPLE, new DyeRecipe(
            List.of(new Ingredient(Items.PURPLE_TULIP, 1)),
            Items.PURPLE_DYE
        ));
        dyeRecipes.put(WoolColor.BLUE, new DyeRecipe(
            List.of(new Ingredient(Items.CORNFLOWER, 1), new Ingredient(Items.LAPIS_LAZULI, 1)),
            Items.BLUE_DYE
        ));
        dyeRecipes.put(WoolColor.BROWN, new DyeRecipe(
            List.of(new Ingredient(Items.COCOA_BEANS, 1)),
            Items.BROWN_DYE
        ));
        dyeRecipes.put(WoolColor.GREEN, new DyeRecipe(
            List.of(new Ingredient(Items.CACTUS, 1)),
            Items.GREEN_DYE
        ));
        dyeRecipes.put(WoolColor.RED, new DyeRecipe(
            List.of(new Ingredient(Items.POPPY, 1), new Ingredient(Items.ROSE_BUSH, 1), new Ingredient(Items.BEETROOT, 1)),
            Items.RED_DYE
        ));
        dyeRecipes.put(WoolColor.BLACK, new DyeRecipe(
            List.of(new Ingredient(Items.INK_SAC, 1), new Ingredient(Items.WITHER_ROSE, 1)),
            Items.BLACK_DYE
        ));
    }

    private void initializeInventory() {
        for (WoolColor color : WoolColor.values()) {
            dyeInventory.put(color, new DyeInventory(color, 0));
        }
    }

    public DyeRecipe getRecipe(WoolColor color) {
        return dyeRecipes.get(color);
    }

    public DyeInventory getInventory(WoolColor color) {
        return dyeInventory.get(color);
    }

    public void addDye(WoolColor color, int amount) {
        dyeInventory.get(color).addDye(amount);
    }

    public boolean consumeDye(WoolColor color, int amount) {
        DyeInventory inventory = dyeInventory.get(color);
        if (inventory.getAmount() >= amount) {
            inventory.consumeDye(amount);
            return true;
        }
        return false;
    }

    public Map<WoolColor, Integer> getAllDyeAmounts() {
        Map<WoolColor, Integer> amounts = new EnumMap<>(WoolColor.class);
        for (var entry : dyeInventory.entrySet()) {
            amounts.put(entry.getKey(), entry.getValue().getAmount());
        }
        return amounts;
    }

    public record DyeRecipe(List<Ingredient> ingredients, net.minecraft.world.item.Item result) {}
    public record Ingredient(net.minecraft.world.item.Item item, int count) {}

    public static class DyeInventory {
        private final WoolColor color;
        private int amount;

        public DyeInventory(WoolColor color, int amount) {
            this.color = color;
            this.amount = amount;
        }

        public void addDye(int amount) {
            this.amount += amount;
        }

        public void consumeDye(int amount) {
            this.amount = Math.max(0, this.amount - amount);
        }

        public WoolColor getColor() { return color; }
        public int getAmount() { return amount; }
    }
}
```

---

## Collection Systems

### WoolCollectionSystem

```java
package com.minewright.farming;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Manages collection and sorting of wool by color
 */
public class WoolCollectionSystem {

    private final Map<WoolColor, List<BlockPos>> storageLocations = new EnumMap<>(WoolColor.class);
    private final Map<WoolColor, Integer> collectedAmounts = new EnumMap<>(WoolColor.class);
    private final ForemanEntity foreman;

    public WoolCollectionSystem(ForemanEntity foreman) {
        this.foreman = foreman;
        initializeCollectedAmounts();
    }

    private void initializeCollectedAmounts() {
        for (WoolColor color : WoolColor.values()) {
            collectedAmounts.put(color, 0);
        }
    }

    /**
     * Add collected wool to the system
     */
    public void addWool(WoolColor color, int amount) {
        collectedAmounts.merge(color, amount, Integer::sum);
    }

    /**
     * Get the amount of collected wool for a color
     */
    public int getCollectedAmount(WoolColor color) {
        return collectedAmounts.getOrDefault(color, 0);
    }

    /**
     * Get all collected amounts
     */
    public Map<WoolColor, Integer> getAllCollectedAmounts() {
        return new EnumMap<>(collectedAmounts);
    }

    /**
     * Register a storage location for a specific color
     */
    public void registerStorageLocation(WoolColor color, BlockPos location) {
        storageLocations.computeIfAbsent(color, k -> new ArrayList<>()).add(location);
    }

    /**
     * Store wool at designated location
     */
    public boolean storeWool(WoolColor color, BlockPos location) {
        BlockState currentBlock = foreman.level().getBlockState(location);

        // Store in chest or barrel
        if (currentBlock.is(Blocks.CHEST) || currentBlock.is(Blocks.BARREL)) {
            // Add to chest inventory (simplified)
            return true;
        }

        return false;
    }

    /**
     * Find nearest storage location for a color
     */
    public BlockPos findNearestStorage(WoolColor color) {
        List<BlockPos> locations = storageLocations.get(color);
        if (locations == null || locations.isEmpty()) {
            return null;
        }

        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos location : locations) {
            double distance = foreman.blockPosition().distSqr(location);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = location;
            }
        }

        return nearest;
    }

    /**
     * Get summary of all collected wool
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder("Collected Wool:\n");

        int total = 0;
        for (var entry : collectedAmounts.entrySet()) {
            int amount = entry.getValue();
            if (amount > 0) {
                summary.append(String.format("  %s: %d\n", entry.getKey().getColorName(), amount));
                total += amount;
            }
        }

        summary.append(String.format("  Total: %d", total));
        return summary.toString();
    }
}
```

### HopperCollectionSystem

```java
package com.minewright.farming;

import com.minewright.structure.BlockPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates hopper-based collection systems for sheep pens
 */
public class HopperCollectionSystem {

    /**
     * Generate a collection system for a sheep pen
     * Uses hoppers to collect wool as sheep are sheared
     */
    public static List<BlockPlacement> generateCollectionSystem(BlockPos penCenter, int width, int depth) {
        List<BlockPlacement> blocks = new ArrayList<>();

        // Place hoppers around the perimeter
        // Hoppers collect items that drop on them

        int hopperSpacing = 4;

        for (int x = -width/2; x < width/2; x += hopperSpacing) {
            // North side hoppers
            BlockPos northPos = penCenter.offset(x, 0, -depth/2 + 1);
            blocks.add(new BlockPlacement(northPos, Blocks.HOPPER.defaultBlockState()));

            // South side hoppers
            BlockPos southPos = penCenter.offset(x, 0, depth/2 - 1);
            blocks.add(new BlockPlacement(southPos, Blocks.HOPPER.defaultBlockState()));
        }

        for (int z = -depth/2; z < depth/2; z += hopperSpacing) {
            // West side hoppers
            BlockPos westPos = penCenter.offset(-width/2 + 1, 0, z);
            blocks.add(new BlockPlacement(westPos, Blocks.HOPPER.defaultBlockState()));

            // East side hoppers
            BlockPos eastPos = penCenter.offset(width/2 - 1, 0, z);
            blocks.add(new BlockPlacement(eastPos, Blocks.HOPPER.defaultBlockState()));
        }

        // Add chest below center hopper for collection
        BlockPos centerChest = penCenter.offset(0, -1, 0);
        blocks.add(new BlockPlacement(centerChest, Blocks.CHEST.defaultBlockState()));

        return blocks;
    }
}
```

---

## Multi-Color Coordination

### MultiColorBuildCoordinator

```java
package com.minewright.farming;

import com.minewright.action.CollaborativeBuildManager;
import com.minewright.entity.ForemanEntity;
import com.minewright.orchestration.AgentMessage;
import net.minecraft.core.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates multiple Foremen for simultaneous multi-color production
 */
public class MultiColorBuildCoordinator {

    private final Map<WoolColor, ColorProductionTask> activeTasks = new ConcurrentHashMap<>();
    private final Map<String, WoolColor> foremanAssignments = new ConcurrentHashMap<>();
    private final ForemanEntity coordinator;

    public MultiColorBuildCoordinator(ForemanEntity coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Assign colors to available Foremen for parallel production
     */
    public void assignColorsToForemen(Map<WoolColor, Integer> colorRequirements, List<String> availableForemen) {
        // Sort colors by quantity (highest first)
        List<Map.Entry<WoolColor, Integer>> sortedColors = new ArrayList<>(colorRequirements.entrySet());
        sortedColors.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int foremenPerColor = Math.max(1, availableForemen.size() / sortedColors.size());

        int foremanIndex = 0;
        for (var entry : sortedColors) {
            WoolColor color = entry.getKey();
            int quantity = entry.getValue();

            // Assign multiple foremen to high-quantity colors
            int assignedCount = 0;
            while (assignedCount < foremenPerColor && foremanIndex < availableForemen.size()) {
                String foremanName = availableForemen.get(foremanIndex);
                foremanAssignments.put(foremanName, color);

                // Send task assignment
                ColorProductionTask task = new ColorProductionTask(
                    color,
                    quantity / foremenPerColor,
                    System.currentTimeMillis()
                );
                activeTasks.put(color, task);

                // Send message to foreman
                AgentMessage message = new AgentMessage.Builder()
                    .type(AgentMessage.Type.TASK_ASSIGNMENT)
                    .sender(coordinator.getEntityName(), coordinator.getEntityName())
                    .recipient(foremanName)
                    .content(String.format("Produce %d %s concrete", quantity / foremenPerColor, color.getColorName()))
                    .payload("color", color.getColorName())
                    .payload("quantity", String.valueOf(quantity / foremenPerColor))
                    .payload("action", "craft_concrete")
                    .build();

                coordinator.sendMessage(message);

                assignedCount++;
                foremanIndex++;
            }
        }

        coordinator.sendChatMessage("Assigned " + sortedColors.size() + " colors to " + foremanIndex + " Foremen");
    }

    /**
     * Track progress of color production tasks
     */
    public void updateProgress(String foremanName, int progress) {
        WoolColor assignedColor = foremanAssignments.get(foremanName);
        if (assignedColor != null) {
            ColorProductionTask task = activeTasks.get(assignedColor);
            if (task != null) {
                task.setProgress(task.getProgress() + progress);
            }
        }
    }

    /**
     * Check if all color production is complete
     */
    public boolean isComplete() {
        return activeTasks.values().stream().allMatch(ColorProductionTask::isComplete);
    }

    /**
     * Get overall progress percentage
     */
    public int getOverallProgress() {
        if (activeTasks.isEmpty()) return 0;

        int totalProgress = activeTasks.values().stream()
            .mapToInt(ColorProductionTask::getProgress)
            .sum();

        int totalRequired = activeTasks.values().stream()
            .mapToInt(ColorProductionTask::getRequired)
            .sum();

        return totalRequired > 0 ? (totalProgress * 100) / totalRequired : 0;
    }

    /**
     * Get status of all color tasks
     */
    public Map<WoolColor, Integer> getTaskStatus() {
        Map<WoolColor, Integer> status = new EnumMap<>(WoolColor.class);
        for (var entry : activeTasks.entrySet()) {
            status.put(entry.getKey(), entry.getValue().getProgress());
        }
        return status;
    }

    public static class ColorProductionTask {
        private final WoolColor color;
        private final int required;
        private int progress;
        private final long startTime;

        public ColorProductionTask(WoolColor color, int required, long startTime) {
            this.color = color;
            this.required = required;
            this.progress = 0;
            this.startTime = startTime;
        }

        public WoolColor getColor() { return color; }
        public int getRequired() { return required; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = Math.min(required, progress); }
        public boolean isComplete() { return progress >= required; }
        public long getStartTime() { return startTime; }
    }
}
```

### SynchronizedShearingAction

```java
package com.minewright.farming.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.orchestration.AgentMessage;
import com.minewright.orchestration.OrchestratorService;
import net.minecraft.world.entity.animal.Sheep;

import java.util.List;

/**
 * Coordinates shearing across multiple Foremen for efficiency
 */
public class SynchronizedShearingAction extends BaseAction {

    private final OrchestratorService orchestrator;
    private final List<WoolColor> targetColors;
    private int sheepProcessed = 0;
    private boolean isCoordinator = false;

    public SynchronizedShearingAction(ForemanEntity foreman, Task task, OrchestratorService orchestrator) {
        super(foreman, task);
        this.orchestrator = orchestrator;

        // Parse target colors
        String colorsParam = task.getStringParameter("colors", "all");
        if (colorsParam.equals("all")) {
            this.targetColors = List.of(com.minewright.farming.SheepClassification.WoolColor.values());
        } else {
            String[] colorNames = colorsParam.split(",");
            this.targetColors = new java.util.ArrayList<>();
            for (String name : colorNames) {
                this.targetColors.add(com.minewright.farming.SheepClassification.WoolColor.fromName(name.trim()));
            }
        }
    }

    @Override
    protected void onStart() {
        // Determine if this foreman is the coordinator
        isCoordinator = foreman.getRole() == com.minewright.orchestration.AgentRole.FOREMAN;

        if (isCoordinator) {
            // Coordinate shearing operation
            coordinateShearing();
        }
    }

    @Override
    protected void onTick() {
        if (isCoordinator) {
            // Monitor progress
            monitorProgress();
        } else {
            // Perform shearing
            performShearing();
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();

        // Notify other foremen of cancellation
        if (isCoordinator) {
            broadcastCancellation();
        }
    }

    @Override
    public String getDescription() {
        return "Synchronized shearing operation";
    }

    private void coordinateShearing() {
        // Assign colors to worker foremen
        int foremenCount = getWorkerCount();
        int colorsPerForeman = (int) Math.ceil((double) targetColors.size() / foremenCount);

        for (int i = 0; i < foremenCount; i++) {
            int startIndex = i * colorsPerForeman;
            int endIndex = Math.min(startIndex + colorsPerForeman, targetColors.size());

            if (startIndex >= targetColors.size()) break;

            List<WoolColor> assignedColors = targetColors.subList(startIndex, endIndex);
            String workerName = getWorkerName(i);

            // Send assignment
            AgentMessage assignment = new AgentMessage.Builder()
                .type(AgentMessage.Type.TASK_ASSIGNMENT)
                .sender(foreman.getEntityName(), foreman.getEntityName())
                .recipient(workerName)
                .content("Shear " + assignedColors.size() + " colors")
                .payload("action", "shear")
                .payload("colors", String.join(",", assignedColors.stream()
                    .map(com.minewright.farming.SheepClassification.WoolColor::getColorName)
                    .toList()))
                .build();

            orchestrator.getCommunicationBus().publish(assignment);
        }

        foreman.sendChatMessage("Coordinating shearing across " + foremenCount + " Foremen");
    }

    private void performShearing() {
        // Create shearing action for assigned colors
        ShearAction shearAction = new ShearAction(foreman, task);
        shearAction.start();

        while (!shearAction.isComplete()) {
            shearAction.tick();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                break;
            }
        }

        ActionResult result = shearAction.getResult();
        if (result.success()) {
            // Report completion to coordinator
            reportCompletion();
        }
    }

    private void monitorProgress() {
        // Wait for worker foremen to complete
        // In a real implementation, this would track progress reports

        if (sheepProcessed >= getExpectedTotalSheep()) {
            result = ActionResult.success("Coordinated shearing complete");
        }
    }

    private void reportCompletion() {
        AgentMessage report = new AgentMessage.Builder()
            .type(AgentMessage.Type.STATUS_REPORT)
            .sender(foreman.getEntityName(), foreman.getEntityName())
            .recipient("foreman")
            .content("Shearing complete")
            .payload("sheep_processed", String.valueOf(sheepProcessed))
            .build();

        orchestrator.getCommunicationBus().publish(report);
    }

    private void broadcastCancellation() {
        AgentMessage cancellation = new AgentMessage.Builder()
            .type(AgentMessage.Type.BROADCAST)
            .sender(foreman.getEntityName(), foreman.getEntityName())
            .content("Shearing operation cancelled")
            .build();

        orchestrator.getCommunicationBus().publish(cancellation);
    }

    private int getWorkerCount() {
        // Simplified - in real implementation, query orchestrator
        return 2;
    }

    private String getWorkerName(int index) {
        return "worker_" + index;
    }

    private int getExpectedTotalSheep() {
        return targetColors.size() * 4; // Estimate 4 sheep per color
    }
}
```

---

## Code Examples

### Example 1: Building a Complete Sheep Farm

```java
package com.minewright.farming.examples;

import com.minewright.entity.ForemanEntity;
import com.minewright.farming.SheepPenGenerator;
import com.minewright.structure.BlockPlacement;
import com.minewright.action.actions.BuildStructureAction;
import com.minewright.action.Task;
import net.minecraft.core.BlockPos;

import java.util.Map;

public class SheepFarmExample {

    /**
     * Build a complete sheep farm complex with all 16 color pens
     */
    public static void buildSheepFarm(ForemanEntity foreman, BlockPos origin) {
        // Generate the sheep farm structure
        var buildPlan = SheepPenGenerator.generateSheepFarmComplex(origin);

        // Create build task
        Task buildTask = new Task("build sheep farm", Map.of(
            "structure", "sheep_farm",
            "blocks", buildPlan
        ));

        // Execute building
        BuildStructureAction buildAction = new BuildStructureAction(foreman, buildTask);
        buildAction.start();

        foreman.sendChatMessage("Building sheep farm with 16 color pens at " + origin);
    }
}
```

### Example 2: Multi-Color Concrete Production

```java
package com.minewright.farming.examples;

import com.minewright.entity.ForemanEntity;
import com.minewright.farming.*;
import com.minewright.orchestration.AgentMessage;

import java.util.List;
import java.util.Map;

public class ConcreteProductionExample {

    /**
     * Coordinate production of multiple concrete colors across multiple Foremen
     */
    public static void produceMultiColorConcrete(
        ForemanEntity coordinator,
        Map<WoolColor, Integer> colorRequirements,
        List<String> availableForemen
    ) {
        // Create coordinator
        MultiColorBuildCoordinator buildCoordinator = new MultiColorBuildCoordinator(coordinator);

        // Assign colors to foremen
        buildCoordinator.assignColorsToForemen(colorRequirements, availableForemen);

        // Monitor progress
        while (!buildCoordinator.isComplete()) {
            int progress = buildCoordinator.getOverallProgress();

            if (progress % 10 == 0) {
                coordinator.sendChatMessage("Progress: " + progress + "%");
            }

            try {
                Thread.sleep(1000); // Check every second
            } catch (InterruptedException e) {
                break;
            }
        }

        coordinator.sendChatMessage("Multi-color concrete production complete!");
    }
}
```

### Example 3: Automated Wool Harvesting

```java
package com.minewright.farming.examples;

import com.minewright.entity.ForemanEntity;
import com.minewright.farming.WoolCollectionSystem;
import com.minewright.farming.actions.ShearAction;
import com.minewright.action.Task;

import java.util.Map;

public class AutomatedWoolHarvest {

    /**
     * Fully automated wool harvesting cycle
     */
    public static void harvestWoolCycle(ForemanEntity foreman) {
        // Create collection system
        WoolCollectionSystem collectionSystem = new WoolCollectionSystem(foreman);

        // Shear all sheep
        Task shearTask = new Task("shear all sheep", Map.of(
            "color", "all",
            "quantity", 128
        ));

        ShearAction shearAction = new ShearAction(foreman, shearTask);
        shearAction.start();

        while (!shearAction.isComplete()) {
            shearAction.tick();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                break;
            }
        }

        // Collect and sort wool
        for (var wool : shearAction.getCollectedWool()) {
            collectionSystem.addWool(wool.color(), wool.amount());
        }

        // Report summary
        foreman.sendChatMessage(collectionSystem.getSummary());
    }
}
```

### Example 4: Concrete Production Line

```java
package com.minewright.farming.examples;

import com.minewright.entity.ForemanEntity;
import com.minewright.farming.SheepClassification.WoolColor;
import com.minewright.farming.actions.*;
import com.minewright.action.Task;

import java.util.Map;

public class ConcreteProductionLine {

    /**
     * Complete concrete production: crafting + hardening
     */
    public static void produceConcrete(
        ForemanEntity foreman,
        WoolColor color,
        int quantity
    ) {
        // Phase 1: Craft concrete powder
        Task craftTask = new Task("craft concrete powder", Map.of(
            "color", color.getColorName(),
            "quantity", quantity
        ));

        ConcreteCraftingAction craftAction = new ConcreteCraftingAction(foreman, craftTask);
        craftAction.start();

        while (!craftAction.isComplete()) {
            craftAction.tick();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                break;
            }
        }

        // Phase 2: Harden concrete powder
        Task hardenTask = new Task("harden concrete", Map.of(
            "color", color.getColorName(),
            "quantity", quantity
        ));

        ConcreteHardeningAction hardenAction = new ConcreteHardeningAction(foreman, hardenTask);
        hardenAction.start();

        while (!hardenAction.isComplete()) {
            hardenAction.tick();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                break;
            }
        }

        foreman.sendChatMessage("Produced " + quantity + " " + color.getColorName() + " concrete");
    }
}
```

---

## Implementation Roadmap

### Phase 1: Sheep Farm Foundation (Week 1-2)
- [ ] Implement `SheepClassification` enum and utilities
- [ ] Create `SheepPenGenerator` for pen structures
- [ ] Implement `ShearAction` for automated shearing
- [ ] Add `SheepBreedingAction` for flock expansion
- [ ] Create sheep detection and tracking

### Phase 2: Wool Collection (Week 2-3)
- [ ] Implement `WoolCollectionSystem` for sorting
- [ ] Create `HopperCollectionSystem` for automated collection
- [ ] Add chest/barrel storage integration
- [ ] Implement wool inventory management

### Phase 3: Dye Production (Week 3-4)
- [ ] Implement `ColorPaletteManager` for dye inventory
- [ ] Create all 16 dye recipes
- [ ] Add flower/plant detection for dye sources
- [ ] Implement automated dye crafting

### Phase 4: Concrete Crafting (Week 4-5)
- [ ] Implement `ConcreteCraftingAction`
- [ ] Add concrete powder recipe crafting
- [ ] Create resource management (sand, gravel, dye)
- [ ] Implement crafting queue optimization

### Phase 5: Concrete Hardening (Week 5-6)
- [ ] Implement `ConcreteHardeningAction`
- [ ] Add water detection for hardening areas
- [ ] Create concrete powder placement logic
- [ ] Implement hardening monitoring

### Phase 6: Multi-Color Coordination (Week 6-7)
- [ ] Implement `MultiColorBuildCoordinator`
- [ ] Create foreman assignment algorithms
- [ ] Add progress tracking and reporting
- [ ] Implement synchronized operations

### Phase 7: Integration (Week 7-8)
- [ ] Register all farming actions in `CoreActionsPlugin`
- [ ] Update `PromptBuilder` with farming commands
- [ ] Add farming context to `WorldKnowledge`
- [ ] Create comprehensive tests

### Phase 8: Polish (Week 8-9)
- [ ] Optimize performance for large farms
- [ ] Add GUI for farm status
- [ ] Implement farm statistics and analytics
- [ ] Create documentation and tutorials

---

## Integration Guide

### 1. Register Farming Actions

Add to `CoreActionsPlugin.java`:

```java
@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {
    // ... existing registrations ...

    // Sheep farming actions
    registry.register("shear",
        (foreman, task, ctx) -> new ShearAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("breed_sheep",
        (foreman, task, ctx) -> new SheepBreedingAction(foreman, task),
        priority, PLUGIN_ID);

    // Concrete production actions
    registry.register("craft_concrete",
        (foreman, task, ctx) -> new ConcreteCraftingAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("harden_concrete",
        (foreman, task, ctx) -> new ConcreteHardeningAction(foreman, task),
        priority, PLUGIN_ID);

    // Build actions
    registry.register("build_sheep_farm",
        (foreman, task, ctx) -> new BuildSheepFarmAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("build_concrete_farm",
        (foreman, task, ctx) -> new BuildConcreteFarmAction(foreman, task),
        priority, PLUGIN_ID);
}
```

### 2. Update PromptBuilder

Add farming-related actions to system prompt:

```java
String farmingActions = """
ACTIONS:
- shear: {"color": "white", "quantity": 64} (shear sheep for wool)
- breed_sheep: {"color": "red"} (breed sheep of specific color)
- craft_concrete: {"color": "blue", "quantity": 64} (craft concrete powder)
- harden_concrete: {"color": "blue", "quantity": 64} (harden powder to concrete)
- build_sheep_farm: {"location": [x, y, z]} (build complete sheep farm)
- build_concrete_farm: {"location": [x, y, z]} (build concrete production facility)
- produce_concrete: {"colors": "red:64,blue:64,green:64"} (multi-color production)
""";
```

### 3. Add Farming Context to WorldKnowledge

```java
public class WorldKnowledge {
    // ... existing code ...

    private WoolCollectionSystem woolCollection;
    private ColorPaletteManager dyeManager;
    private Map<WoolColor, Integer> sheepCounts;

    public String getFarmingStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Sheep: ").append(getSheepCount()).append("\n");
        status.append("Wool: ").append(woolCollection != null ? woolCollection.getSummary() : "none").append("\n");
        status.append("Dyes: ").append(dyeManager != null ? dyeManager.getAllDyeAmounts() : "none");
        return status.toString();
    }

    public int getSheepCount() {
        return sheepCounts != null ? sheepCounts.values().stream().mapToInt(Integer::intValue).sum() : 0;
    }
}
```

### 4. Update ForemanEntity

Add farming managers to entity:

```java
public class ForemanEntity extends PathfinderMob {
    private WoolCollectionSystem woolCollection;
    private ColorPaletteManager dyeManager;

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // ... existing code ...
        }
    }

    // Getters
    public WoolCollectionSystem getWoolCollection() {
        if (woolCollection == null) {
            woolCollection = new WoolCollectionSystem(this);
        }
        return woolCollection;
    }

    public ColorPaletteManager getDyeManager() {
        if (dyeManager == null) {
            dyeManager = new ColorPaletteManager();
        }
        return dyeManager;
    }
}
```

### 5. Example Commands for Users

```
"build a sheep farm"
-> {"action": "build_sheep_farm", "parameters": {"location": [100, 64, 100]}}

"shear all sheep"
-> {"action": "shear", "parameters": {"color": "all", "quantity": 128}}

"breed red sheep"
-> {"action": "breed_sheep", "parameters": {"color": "red"}}

"make 64 blue concrete"
-> {"action": "craft_concrete", "parameters": {"color": "blue", "quantity": 64}}

"produce multi-colored concrete: 64 red, 64 blue, 64 green"
-> {"action": "produce_concrete", "parameters": {"colors": "red:64,blue:64,green:64"}}

"harden the concrete"
-> {"action": "harden_concrete", "parameters": {"color": "blue", "quantity": 64}}
```

---

## Testing Checklist

### Sheep Farming
- [ ] Sheep pens generate correctly for all 16 colors
- [ ] Sheep are properly color-classified
- [ ] Shearing action collects correct wool color
- [ ] Breeding produces offspring with correct colors
- [ ] Sheep stay within pens
- [ ] Escaped sheep are detected

### Wool Collection
- [ ] Wool is sorted by color
- [ ] Storage chests are used correctly
- [ ] Hopper collection system works
- [ ] Inventory tracking is accurate
- [ ] Collection summary reports correctly

### Dye Production
- [ ] All 16 dye recipes work
- [ ] Dye sources are detected
- [ ] Dye inventory is tracked
- [ ] Dye consumption is managed
- [ ] Recipe crafting is optimized

### Concrete Production
- [ ] Concrete powder crafting works
- [ ] Resources are consumed correctly
- [ ] Hardening areas are found
- [ ] Concrete hardens in water
- [ ] Multi-color production coordinates

### Multi-Foreman Coordination
- [ ] Colors are assigned to foremen
- [ ] Progress is tracked across foremen
- [ ] Communication works correctly
- [ ] Load balancing is effective
- [ ] Overall completion is detected

---

## Performance Considerations

### Optimization Strategies

1. **Batch Processing**
   - Process sheep in batches of 8-16
   - Craft concrete in stacks of 64
   - Harden multiple blocks simultaneously

2. **Spatial Partitioning**
   - Divide farms into zones
   - Assign foremen to different zones
   - Minimize cross-zone travel

3. **Resource Caching**
   - Cache sheep locations
   - Pre-locate water for hardening
   - Store dye recipes in memory

4. **Async Operations**
   - Shear multiple sheep in parallel
   - Craft multiple dyes simultaneously
   - Harden concrete across multiple foremen

### Scalability

| Farm Size | Sheep Count | Foremen | Production Rate |
|-----------|-------------|---------|-----------------|
| Small | 64 | 1 | 128 wool/min |
| Medium | 256 | 2-4 | 512 wool/min |
| Large | 1024 | 4-8 | 2048 wool/min |
| Massive | 4096 | 8-16 | 8192 wool/min |

---

## Future Enhancements

1. **Advanced Features**
   - Sheep genetics and breeding optimization
   - Automated sheep feeding systems
   - Sheep shearing scheduling
   - Color mutation breeding

2. **Production Optimization**
   - Just-in-time concrete production
   - Predictive resource gathering
   - Automatic restocking
   - Production analytics

3. **Multi-Farm Coordination**
   - Farm network management
   - Resource sharing between farms
   - Centralized distribution
   - Farm expansion planning

4. **Integration with Other Systems**
   - Trading with villagers
   - Selling wool to players
   - Building with harvested materials
   - Storage optimization

---

**Document End**

This design document provides a comprehensive foundation for implementing wool and concrete farming systems in MineWright. The modular architecture allows for incremental implementation and easy extension with new features and optimizations.
