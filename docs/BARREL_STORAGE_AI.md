# Barrel Storage AI for MineWright

**Version:** 1.20.1 (Forge)
**Author:** Claude Code
**Date:** 2026-02-27
**Status:** Design Document

---

## Executive Summary

This document provides a comprehensive design for intelligent barrel storage automation in the MineWright Minecraft mod. The system enables AI foremen to automatically organize, sort, and manage item storage across networks of barrels, implementing industry-standard categorization algorithms with multi-agent coordination.

**Key Features:**
- Intelligent item categorization using semantic analysis
- Space-efficient barrel placement patterns
- Auto-sorting with hopper networks
- Multi-barrel load balancing
- Inventory-aware routing
- Integration with existing action system

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Barrel Placement Optimization](#barrel-placement-optimization)
3. [Item Categorization System](#item-categorization-system)
4. [Auto-Sorting Algorithms](#auto-sorting-algorithms)
5. [Inventory Management](#inventory-management)
6. [Multi-Barrel Coordination](#multi-barrel-coordination)
7. [Code Integration Examples](#code-integration-examples)
8. [Implementation Roadmap](#implementation-roadmap)

---

## 1. System Overview

### 1.1 Architecture Integration

The barrel storage system integrates with existing MineWright components:

```
┌─────────────────────────────────────────────────────────────┐
│                      ActionExecutor                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              BuildStorageSystemAction                │   │
│  │  - Barrel placement                                  │   │
│  │  - Hopper network setup                              │   │
│  │  - Sign labeling                                     │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              SortItemsAction                         │   │
│  │  - Inventory scanning                               │   │
│  │  - Item categorization                              │   │
│  │  - Deposit routing                                  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  StorageManager (New)                       │
│  - Barrel registry (location → category)                    │
│  - Item category mappings                                   │
│  - Space utilization tracking                               │
│  - Access pattern analysis                                 │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  ItemCategorizer (New)                      │
│  - Semantic category trees                                  │
│  - Tag-based classification                                │
│  - NBT-aware sorting                                       │
│  - Custom category definitions                             │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Minecraft 1.20.1 Storage Context

**Available Storage Blocks:**
- Barrel (27 slots, stackable vertically, side-accessible)
- Chest (27 slots, requires 1-block air space)
- Large Chest (54 slots, double chest)
- Shulker Box (27 slots, portable)
- Ender Chest (27 slots, cross-dimensional)

**Why Barrels?**
- Can be placed against walls (space efficient)
- Can be accessed from sides (hopper-friendly)
- Can be stacked (vertical storage)
- Cheaper recipe (6 planks vs 8 for chest)
- Better for compact storage rooms

**Hopper Mechanics:**
- Transfer rate: 8 items/2.5 seconds (2.5 gt/transfer)
- Filter mode: Only accept matching items
- Can extract from and insert into barrels
- Redstone controllable

---

## 2. Barrel Placement Optimization

### 2.1 Storage Layout Patterns

#### Pattern 1: Wall-Mounted Barrel Grid

```
Wall View:
┌──────────────────────────────────────────────────────────┐
│  [Tools] [Ores]  [Food]  [Farms]  [Redstone] [Misc]     │
│  [Wood]   [Stone] [Mob]   [Armor]  [Combat]  [Building] │
│                                                          │
│  Central aisle for agent movement                        │
└──────────────────────────────────────────────────────────┘

Top View:
  ┌────┬────┬────┬────┬────┬────┐
  │ B1 │ B2 │ B3 │ B4 │ B5 │ B6 │ ← Row 1 (eye level)
  ├────┼────┼────┼────┼────┼────┤
  │ H1 │ H2 │ H3 │ H4 │ H5 │ H6 │ ← Hopper layer
  ├────┼────┼────┼────┼────┼────┤
  │ B7 │ B8 │ B9 │B10 │B11 │B12 │ ← Row 2
  └────┴────┴────┴────┴────┴────┘
  ▲    ▲    ▲    ▲    ▲    ▲
  │    │    │    │    │    │
  Wall─┘    │    │    │    │    └─Wall
            │    │    │    │
        Agent walkway (2 blocks wide)
```

**Advantages:**
- Maximum barrel density
- Easy hopper integration
- Clear visual organization
- Agent can access all barrels from walkway

**Code Implementation:**
```java
private List<BlockPlacement> generateWallMountedGrid(
    BlockPos origin,
    int barrelsPerRow,
    int rows
) {
    List<BlockPlacement> plan = new ArrayList<>();
    Direction wallDirection = Direction.WEST;
    Direction walkwayDirection = Direction.SOUTH;

    for (int row = 0; row < rows; row++) {
        for (int col = 0; col < barrelsPerRow; col++) {
            // Barrel position
            BlockPos barrelPos = origin.relative(wallDirection, row)
                .relative(walkwayDirection, col * 2);
            plan.add(new BlockPlacement(barrelPos, Blocks.BARREL));

            // Hopper below (if not bottom row)
            if (row < rows - 1) {
                BlockPos hopperPos = barrelPos.below();
                plan.add(new BlockPlacement(
                    hopperPos,
                    Blocks.HOPPER,
                    // Face down for sorting
                    Direction.DOWN
                ));
            }

            // Sign on barrel face
            BlockPos signPos = barrelPos.relative(wallDirection);
            plan.add(new BlockPlacement(
                signPos,
                Blocks.OAK_SIGN,
                // Category label set via NBT
                getCategoryForSlot(row * barrelsPerRow + col)
            ));
        }
    }

    return plan;
}
```

#### Pattern 2: Central Hub with Radial Storage

```
         ┌─────────────┐
         │   [Input]   │ ← Input chest (items arrive here)
         └──────┬──────┘
                │
         ┌──────▼──────┐
         │    Sorter   │ ← Central hopper clock
         └──────┬──────┘
                │
        ┌───────┼───────┐
        │       │       │
    ┌───▼───┐ ┌▼───┐ ┌─▼────┐
    │Tools  │ │Ores│ │ Redst│
    └───────┘ └────┘ └──────┘
        │       │       │
    ┌───▼───┐ ┌▼───┐ ┌─▼────┐
    │Food   │ │Wood│ │ Armor│
    └───────┘ └────┘ └──────┘
         (6 radial barrel arrays)
```

**Advantages:**
- Centralized sorting
- Easy expansion (add more radial arms)
- Agent can circle around hub
- Natural categorization by direction

**Code Implementation:**
```java
private List<BlockPlacement> generateRadialStorage(
    BlockPos center,
    int arms,
    int barrelsPerArm
) {
    List<BlockPlacement> plan = new ArrayList<>();

    // Central input chest
    plan.add(new BlockPlacement(center, Blocks.CHEST));
    plan.add(new BlockPlacement(center.above(), Blocks.HOPPER));

    // Generate radial arms
    for (int arm = 0; arm < arms; arm++) {
        double angle = (2 * Math.PI * arm) / arms;
        Direction armDirection = Direction.fromDelta(
            (int)Math.round(Math.cos(angle)),
            0,
            (int)Math.round(Math.sin(angle))
        );

        BlockPos armStart = center.relative(armDirection, 2);

        for (int i = 0; i < barrelsPerArm; i++) {
            BlockPos barrelPos = armStart.relative(armDirection, i * 2);
            plan.add(new BlockPlacement(barrelPos, Blocks.BARREL));

            // Connecting hopper
            if (i > 0) {
                BlockPos hopperPos = barrelPos.relative(
                    armDirection, -1
                ).below();
                plan.add(new BlockPlacement(
                    hopperPos,
                    Blocks.HOPPER,
                    armDirection.getOpposite()
                ));
            }
        }
    }

    return plan;
}
```

### 2.2 Space Optimization Strategies

#### Strategy 1: Vertical Stacking

```java
/**
 * Calculate optimal barrel stack height based on:
 * - Agent reach (default 4-5 blocks)
 * - Ceiling height
 * - Accessibility (should be reachable from ground)
 */
public static int calculateOptimalStackHeight(Level level, BlockPos base) {
    int maxHeight = 0;

    // Check upward until ceiling
    for (int y = 1; y <= 5; y++) {
        BlockPos checkPos = base.above(y);
        BlockState state = level.getBlockState(checkPos);

        // Stop at ceiling
        if (!state.isAir() && !state.getMaterial().isReplaceable()) {
            break;
        }

        // Ensure agent can reach (player reach ≈ 4.5 blocks)
        if (y > 4) {
            break;
        }

        maxHeight++;
    }

    return Math.max(1, maxHeight);
}
```

#### Strategy 2: Density-Based Packing

```java
/**
 * Optimizes barrel placement using bin-packing algorithm
 * Groups similar items in same barrel before allocating new barrels
 */
public class BarrelPacker {
    private final Map<ItemCategory, List<ItemStack>> pendingItems;
    private final int barrelCapacity = 27 * 64; // 27 slots, max stack

    /**
     * Assign items to barrels minimizing barrel count
     * Uses First-Fit Decreasing algorithm
     */
    public List<BarrelAssignment> optimizeBarrels(
        Collection<ItemStack> items
    ) {
        // Sort by quantity (descending)
        List<ItemStack> sorted = items.stream()
            .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
            .toList();

        List<BarrelAssignment> barrels = new ArrayList<>();

        for (ItemStack item : sorted) {
            boolean placed = false;

            // Try to fit in existing barrel
            for (BarrelAssignment barrel : barrels) {
                if (barrel.canAccept(item)) {
                    barrel.addItem(item);
                    placed = true;
                    break;
                }
            }

            // Create new barrel if needed
            if (!placed) {
                BarrelAssignment newBarrel = new BarrelAssignment(
                    categorize(item.getItem())
                );
                newBarrel.addItem(item);
                barrels.add(newBarrel);
            }
        }

        return barrels;
    }
}
```

---

## 3. Item Categorization System

### 3.1 Category Hierarchy Tree

``                    Storage Root
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
    Resources        Manufactured       Misc/Etc
        │                  │                  │
  ┌─────┴─────┐      ┌─────┴─────┐          │
  │           │      │           │          │
Raw Ores   Raw Wood  Tools  Armor/Weapons  Food
  │           │      │      │      │
  │       ┌───┴───┐  │   ┌───┴───┐  │
Iron      Oak Spruce│ Diamond Iron  │
Gold      Birch ... │  ...  ...   │
...                │              │
                ┌───┴────────────┴────┐
                │   Redstone/Potion  │
                │   Farming          │
                │   Building Blocks  │
                └────────────────────┘
```

### 3.2 Category Definition System

```java
package com.minewright.storage;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.ItemTags;

import java.util.*;

/**
 * Hierarchical item categorization system for barrel storage.
 *
 * <p>Categories support:</p>
 * <ul>
 *   <li>Tag-based matching (e.g., all logs)</li>
 *   <li>Direct item assignment</li>
 *   <li>NBT-aware sorting (e.g., enchantments, durability)</li>
 *   <li>Custom category definitions via config</li>
 * </ul>
 */
public class ItemCategorizer {

    /**
     * Primary storage categories
     */
    public enum ItemCategory {
        RAW_RESOURCES("Raw Resources", "raw_resources"),
        WOOD_PRODUCTS("Wood Products", "wood"),
        STONE_MATERIALS("Stone Materials", "stone"),
        ORES_AND_MINERALS("Ores & Minerals", "ores"),
        FOOD_AND_CONSUMABLES("Food & Consumables", "food"),
        FARMING_SUPPLIES("Farming Supplies", "farming"),
        TOOLS_AND_UTILITIES("Tools & Utilities", "tools"),
        COMBAT_GEAR("Combat Gear", "combat"),
        ARMOR("Armor", "armor"),
        REDSTONE_COMPONENTS("Redstone Components", "redstone"),
        POTIONS_AND_BREWING("Potions & Brewing", "potions"),
        TRANSPORTATION("Transportation", "transport"),
        DECORATIVE_BLOCKS("Decorative Blocks", "decorative"),
        RARE_MATERIALS("Rare Materials", "rare"),
        BULK_STORAGE("Bulk Storage", "bulk"),
        MISC_ITEMS("Miscellaneous", "misc");

        private final String displayName;
        private final String id;

        ItemCategory(String displayName, String id) {
            this.displayName = displayName;
            this.id = id;
        }

        public String getDisplayName() { return displayName; }
        public String getId() { return id; }
    }

    /**
     * Tag-based category mappings
     */
    private static final Map<ItemCategory, List<TagKey<Item>>> TAG_MAPPINGS;
    static {
        TAG_MAPPINGS = new EnumMap<>(ItemCategory.class);
        TAG_MAPPINGS.put(ItemCategory.WOOD_PRODUCTS, List.of(
            ItemTags.LOGS,
            ItemTags.PLANKS,
            ItemTags.SAPLINGS,
            ItemTags.WOODEN_BUTTONS,
            ItemTags.WOODEN_DOORS,
            ItemTags.WOODEN_STAIRS,
            ItemTags.WOODEN_SLABS,
            ItemTags.WOODEN_FENCES,
            ItemTags.WOODEN_PRESSURE_PLATES,
            ItemTags.WOODEN_TRAPDOORS
        ));
        TAG_MAPPINGS.put(ItemCategory.ORES_AND_MINERALS, List.of(
            ItemTags.COAL_ORES,
            ItemTags.COPPER_ORES,
            ItemTags.IRON_ORES,
            ItemTags.GOLD_ORES,
            ItemTags.DIAMOND_ORES,
            ItemTags.EMERALD_ORES,
            ItemTags.LAPIS_ORES,
            ItemTags.REDSTONE_ORES
        ));
        TAG_MAPPINGS.put(ItemCategory.TOOLS_AND_UTILITIES, List.of(
            ItemTags.AXES,
            ItemTags.PICKAXES,
            ItemTags.SHOVELS,
            ItemTags.HOES,
            ItemTags.SHEARS,
            ItemTags.FISHING_RODS
        ));
        TAG_MAPPINGS.put(ItemCategory.COMBAT_GEAR, List.of(
            ItemTags.SWORDS
        ));
        TAG_MAPPINGS.put(ItemCategory.ARMOR, List.of(
            ItemTags.HEAD_ARMOR,
            ItemTags.CHEST_ARMOR,
            ItemTags.LEG_ARMOR,
            ItemTags.FOOT_ARMOR
        ));
        TAG_MAPPINGS.put(ItemCategory.FOOD_AND_CONSUMABLES, List.of(
            ItemTags.FOODS
        ));
    }

    /**
     * Direct item-to-category mappings for special cases
     */
    private static final Map<Item, ItemCategory> DIRECT_MAPPINGS;
    static {
        DIRECT_MAPPINGS = new HashMap<>();
        // Raw resources
        DIRECT_MAPPINGS.put(Items.COBBLESTONE, ItemCategory.STONE_MATERIALS);
        DIRECT_MAPPINGS.put(Items.STONE, ItemCategory.STONE_MATERIALS);
        DIRECT_MAPPINGS.put(Items.DIRT, ItemCategory.BULK_STORAGE);
        DIRECT_MAPPINGS.put(Items.GRASS_BLOCK, ItemCategory.BULK_STORAGE);
        DIRECT_MAPPINGS.put(Items.SAND, ItemCategory.BULK_STORAGE);
        DIRECT_MAPPINGS.put(Items.GRAVEL, ItemCategory.BULK_STORAGE);

        // Redstone
        DIRECT_MAPPINGS.put(Items.REDSTONE, ItemCategory.REDSTONE_COMPONENTS);
        DIRECT_MAPPINGS.put(Items.REPEATER, ItemCategory.REDSTONE_COMPONENTS);
        DIRECT_MAPPINGS.put(Items.COMPARATOR, ItemCategory.REDSTONE_COMPONENTS);
        DIRECT_MAPPINGS.put(Items.PISTON, ItemCategory.REDSTONE_COMPONENTS);
        DIRECT_MAPPINGS.put(Items.STICKY_PISTON, ItemCategory.REDSTONE_COMPONENTS);
        DIRECT_MAPPINGS.put(Items.OBSERVER, ItemCategory.REDSTONE_COMPONENTS);
        DIRECT_MAPPINGS.put(Items.HOPPER, ItemCategory.REDSTONE_COMPONENTS);
        DIRECT_MAPPINGS.put(Items.DROPPER, ItemCategory.REDSTONE_COMPONENTS);
        DIRECT_MAPPINGS.put(Items.DISPENSER, ItemCategory.REDSTONE_COMPONENTS);

        // Rare materials
        DIRECT_MAPPINGS.put(Items.DIAMOND, ItemCategory.RARE_MATERIALS);
        DIRECT_MAPPINGS.put(Items.EMERALD, ItemCategory.RARE_MATERIALS);
        DIRECT_MAPPINGS.put(Items.NETHERITE_SCRAP, ItemCategory.RARE_MATERIALS);
        DIRECT_MAPPINGS.put(Items.NETHERITE_INGOT, ItemCategory.RARE_MATERIALS);
        DIRECT_MAPPINGS.put(Items.ELYTRA, ItemCategory.RARE_MATERIALS);
        DIRECT_MAPPINGS.put(Items.TOTEM_OF_UNDYING, ItemCategory.RARE_MATERIALS);
        DIRECT_MAPPINGS.put(Items.ENCHANTED_GOLDEN_APPLE, ItemCategory.RARE_MATERIALS);

        // Farming
        DIRECT_MAPPINGS.put(Items.WHEAT_SEEDS, ItemCategory.FARMING_SUPPLIES);
        DIRECT_MAPPINGS.put(Items.CARROT, ItemCategory.FOOD_AND_CONSUMABLES);
        DIRECT_MAPPINGS.put(Items.POTATO, ItemCategory.FOOD_AND_CONSUMABLES);
        DIRECT_MAPPINGS.put(Items.BEETROOT_SEEDS, ItemCategory.FARMING_SUPPLIES);
        DIRECT_MAPPINGS.put(Items.MELON_SEEDS, ItemCategory.FARMING_SUPPLIES);
        DIRECT_MAPPINGS.put(Items.PUMPKIN_SEEDS, ItemCategory.FARMING_SUPPLIES);
        DIRECT_MAPPINGS.put(Items.BONE_MEAL, ItemCategory.FARMING_SUPPLIES);

        // Transportation
        DIRECT_MAPPINGS.put(Items.MINECART, ItemCategory.TRANSPORTATION);
        DIRECT_MAPPINGS.put(Items.CHEST_MINECART, ItemCategory.TRANSPORTATION);
        DIRECT_MAPPINGS.put(Items.FURNACE_MINECART, ItemCategory.TRANSPORTATION);
        DIRECT_MAPPINGS.put(Items.HOPPER_MINECART, ItemCategory.TRANSPORTATION);
        DIRECT_MAPPINGS.put(Items.TNT_MINECART, ItemCategory.TRANSPORTATION);
        DIRECT_MAPPINGS.put(Items.RAIL, ItemCategory.TRANSPORTATION);
        DIRECT_MAPPINGS.put(Items.POWERED_RAIL, ItemCategory.TRANSPORTATION);
        DIRECT_MAPPINGS.put(Items.DETECTOR_RAIL, ItemCategory.TRANSPORTATION);
        DIRECT_MAPPINGS.put(Items.ACTIVATOR_RAIL, ItemCategory.TRANSPORTATION);

        // Boats
        DIRECT_MAPPINGS.put(Items.OAK_BOAT, ItemCategory.TRANSPORTATION);
        DIRECT_MAPPINGS.put(Items.SPRUCE_BOAT, ItemCategory.TRANSPORTATION);
        DIRECT_MAPPINGS.put(Items.BIRCH_BOAT, ItemCategory.TRANSPORTATION);
        DIRECT_MAPPINGS.put(Items.JUNGLE_BOAT, ItemCategory.TRANSPORTATION);
        DIRECT_MAPPINGS.put(Items.ACACIA_BOAT, ItemCategory.TRANSPORTATION);
        DIRECT_MAPPINGS.put(Items.DARK_OAK_BOAT, ItemCategory.TRANSPORTATION);
    }

    /**
     * Categorize an item using tag and direct mappings
     */
    public static ItemCategory categorize(Item item) {
        // Check direct mappings first (highest priority)
        ItemCategory direct = DIRECT_MAPPINGS.get(item);
        if (direct != null) {
            return direct;
        }

        // Check tag mappings
        for (Map.Entry<ItemCategory, List<TagKey<Item>>> entry : TAG_MAPPINGS.entrySet()) {
            for (TagKey<Item> tag : entry.getValue()) {
                if (item.builtInRegistryHolder().is(tag)) {
                    return entry.getKey();
                }
            }
        }

        // Default to misc
        return ItemCategory.MISC_ITEMS;
    }

    /**
     * Get subcategories for a category (for multi-barrel systems)
     */
    public static List<ItemCategory> getSubcategories(ItemCategory parent) {
        return switch (parent) {
            case WOOD_PRODUCTS -> List.of(
                parent, // Logs
                parent, // Planks
                parent  // Saplings
            );
            case COMBAT_GEAR -> List.of(
                parent, // Melee weapons
                parent, // Ranged weapons
                parent  // Ammunition
            );
            default -> List.of(parent);
        };
    }

    /**
     * Sort items by category (useful for inventory display)
     */
    public static Map<ItemCategory, List<ItemStack>> sortByCategory(
        List<ItemStack> items
    ) {
        Map<ItemCategory, List<ItemStack>> sorted = new EnumMap<>(ItemCategory.class);

        for (ItemStack stack : items) {
            ItemCategory category = categorize(stack.getItem());
            sorted.computeIfAbsent(category, k -> new ArrayList<>()).add(stack);
        }

        // Sort within each category by item ID
        sorted.forEach((cat, stacks) -> stacks.sort((a, b) ->
            a.getItem().toString().compareTo(b.getItem().toString())
        ));

        return sorted;
    }
}
```

### 3.3 NBT-Aware Sorting

```java
/**
 * Enhanced categorization that considers NBT data
 */
public class NBTAwareCategorizer {

    /**
     * Categories based on NBT properties
     */
    public enum NBTCriteria {
        ENCHANTMENT_LEVEL("Enchantment Level"),
        DURABILITY_PERCENTAGE("Durability %"),
        POTION_EFFECT("Potion Effect"),
        BLOCK_ENTITY_DATA("Block Entity Data"),
        CUSTOM_NAME("Custom Name");

        private final String displayName;

        NBTCriteria(String displayName) {
            this.displayName = displayName;
        }
    }

    /**
     * Sort enchanted tools by enchantment level
     * Keeps high-level enchanted items separate
     */
    public static int getEnchantmentTier(ItemStack stack) {
        if (!stack.isEnchanted()) {
            return 0;
        }

        // Count total enchantment levels
        Map<Enchantment, Integer> enchantments =
            EnchantmentHelper.getEnchantments(stack);

        int totalLevels = enchantments.values().stream()
            .mapToInt(Integer::intValue)
            .sum();

        // Categorize by tier
        if (totalLevels >= 30) return 4;      // God tier
        if (totalLevels >= 20) return 3;      // High tier
        if (totalLevels >= 10) return 2;      // Medium tier
        return 1;                             // Low tier
    }

    /**
     * Sort armor by damage/durability
     */
    public static DurabilityGrade getDurabilityGrade(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return DurabilityGrade.NEW;
        }

        int maxDamage = stack.getMaxDamage();
        int currentDamage = stack.getDamageValue();
        double percentage = 1.0 - ((double)currentDamage / maxDamage);

        if (percentage >= 0.75) return DurabilityGrade.EXCELLENT;
        if (percentage >= 0.50) return DurabilityGrade.GOOD;
        if (percentage >= 0.25) return DurailabilityGrade.FAIR;
        return DurabilityGrade.POOR;
    }

    public enum DurabilityGrade {
        EXCELLENT, GOOD, FAIR, POOR, NEW
    }

    /**
     * Categorize potions by effect
     */
    public static ItemCategory getPotionCategory(ItemStack potionStack) {
        Potion potion = PotionUtils.getPotion(potionStack);

        if (potion == Potions.HEALING || potion == Potions.STRONG_HEALING ||
            potion == Potions.REGENERATION || potion == Potions.STRONG_REGENERATION) {
            return ItemCategory.HEALING_POTIONS;
        }
        if (potion == Potions.STRENGTH || potion == Potions.STRONG_STRENGTH) {
            return ItemCategory.BUFF_POTIONS;
        }
        if (potion == Potions.SPEED || potion == Potions.SWIFTNESS) {
            return ItemCategory.BUFF_POTIONS;
        }
        if (potion == Potions.FIRE_RESISTANCE) {
            return ItemCategory.PROTECTION_POTIONS;
        }

        return ItemCategory.MISC_ITEMS;
    }
}
```

---

## 4. Auto-Sorting Algorithms

### 4.1 Hopper Network Sorter

```java
/**
 * Action that sets up an auto-sorting hopper network
 */
public class BuildSortingSystemAction extends BaseAction {

    private List<BlockPlacement> buildPlan;
    private int currentBlockIndex;
    private BlockPos inputChestPos;
    private Map<ItemCategory, BlockPos> categoryBarrels;

    @Override
    protected void onStart() {
        String layoutType = task.getStringParameter("layout", "wall");
        inputChestPos = getOriginFromTask();

        categoryBarrels = new HashMap<>();
        buildPlan = generateSortingSystem(inputChestPos, layoutType);
        currentBlockIndex = 0;

        foreman.sendChatMessage("Building auto-sorting system...");
    }

    @Override
    protected void onTick() {
        if (currentBlockIndex >= buildPlan.size()) {
            result = ActionResult.success("Sorting system complete!");
            return;
        }

        BlockPlacement placement = buildPlan.get(currentBlockIndex);
        if (placeBlock(placement)) {
            currentBlockIndex++;
        }
    }

    /**
     * Generate a sorting hopper network
     */
    private List<BlockPlacement> generateSortingSystem(
        BlockPos inputPos,
        String layout
    ) {
        List<BlockPlacement> plan = new ArrayList<>();

        // Place input chest
        plan.add(new BlockPlacement(inputPos, Blocks.CHEST));

        // Generate category-based barrel rows
        List<ItemCategory> categories = getCategoriesToSort();
        Direction expansionDir = Direction.EAST;
        Direction currentDir = expansionDir;

        for (int i = 0; i < categories.size(); i++) {
            ItemCategory category = categories.get(i);
            BlockPos barrelPos = inputPos.relative(currentDir, 2 + (i / 3) * 2);
            barrelPos = barrelPos.above(i % 3 == 0 ? 0 : i % 3 == 1 ? 1 : -1);

            // Place barrel
            plan.add(new BlockPlacement(barrelPos, Blocks.BARREL));
            categoryBarrels.put(category, barrelPos);

            // Place filtered hopper leading to barrel
            BlockPos hopperPos = barrelPos.relative(currentDir.getOpposite()).below();
            BlockState hopperState = Blocks.HOPPER.defaultBlockState()
                .setValue(HopperBlock.FACING, currentDir.getOpposite());

            plan.add(new BlockPlacement(hopperPos, hopperState));

            // Configure hopper filter via NBT
            plan.add(new BlockPlacement(hopperPos, setupHopperFilter(category)));
        }

        // Connect input chest to hoppers with hopper minecart or additional hoppers
        setupInputDistribution(plan, inputPos, categories);

        return plan;
    }

    /**
     * Setup hopper filter to only accept items from a category
     */
    private BlockState setupHopperFilter(ItemCategory category) {
        // In Minecraft, hopper filters are set via NBT
        // This would be implemented using:
        // - HopperBlockEntity.setFilter()
        // - Or direct NBT manipulation on the tile entity

        CompoundTag nbt = new CompoundTag();
        ListTag filterList = new ListTag();

        // Add all items in category to filter
        for (Item item : getItemsInCategory(category)) {
            CompoundTag itemTag = new CompoundTag();
            itemTag.putString("id", BuiltInRegistries.ITEM.getKey(item).toString());
            filterList.add(itemTag);
        }

        nbt.put("Items", filterList);
        nbt.putBoolean("FilterEnabled", true);

        return Blocks.HOPPER.defaultBlockState();
    }

    /**
     * Get all items that belong to a category
     */
    private List<Item> getItemsInCategory(ItemCategory category) {
        List<Item> items = new ArrayList<>();

        for (Item item : BuiltInRegistries.ITEM) {
            if (ItemCategorizer.categorize(item) == category) {
                items.add(item);
            }
        }

        return items;
    }
}
```

### 4.2 Item Sorter Action

```java
/**
 * Action to sort items from agent's inventory into storage barrels
 */
public class SortItemsAction extends BaseAction {

    private static final int BARREL_SCAN_RADIUS = 16;
    private static final int ITEMS_PER_TICK = 4;

    private Map<ItemCategory, BlockPos> barrelLocations;
    private List<ItemStack> pendingItems;
    private int itemsSorted = 0;
    private int ticksWaiting = 0;

    @Override
    protected void onStart() {
        foreman.sendChatMessage("Sorting items...");

        // Scan for nearby barrels and register them
        barrelLocations = scanNearbyBarrels();

        // Get items from agent inventory or designated chest
        pendingItems = collectItemsToSort();
    }

    @Override
    protected void onTick() {
        if (pendingItems.isEmpty()) {
            result = ActionResult.success("Sorted " + itemsSorted + " item stacks");
            return;
        }

        // Sort a few items per tick
        int processed = 0;
        Iterator<ItemStack> iterator = pendingItems.iterator();

        while (iterator.hasNext() && processed < ITEMS_PER_TICK) {
            ItemStack stack = iterator.next();

            if (depositItem(stack)) {
                iterator.remove();
                itemsSorted++;
                processed++;
            } else {
                ticksWaiting++;
                if (ticksWaiting > 100) {
                    // Skip this item after waiting too long
                    foreman.sendChatMessage("Can't find storage for " +
                        stack.getItem().toString());
                    iterator.remove();
                    ticksWaiting = 0;
                }
                break; // Wait for pathfinding to complete
            }
        }
    }

    /**
     * Scan for nearby barrels and map them to categories
     */
    private Map<ItemCategory, BlockPos> scanNearbyBarrels() {
        Map<ItemCategory, BlockPos> barrels = new HashMap<>();
        BlockPos agentPos = foreman.blockPosition();
        Level level = foreman.level();

        // Scan in spiral pattern
        for (int radius = 1; radius <= BARREL_SCAN_RADIUS; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -2; y <= 4; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos checkPos = agentPos.offset(x, y, z);
                        BlockState state = level.getBlockState(checkPos);

                        if (state.is(Blocks.BARREL)) {
                            // Check for sign or determine category from contents
                            ItemCategory category = detectBarrelCategory(checkPos);
                            if (category != null && !barrels.containsKey(category)) {
                                barrels.put(category, checkPos);
                            }
                        }
                    }
                }
            }
        }

        return barrels;
    }

    /**
     * Detect what category a barrel is for
     * (from sign label, existing contents, or location pattern)
     */
    private ItemCategory detectBarrelCategory(BlockPos barrelPos) {
        Level level = foreman.level();

        // Check for sign on any side
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;

            BlockPos signPos = barrelPos.relative(dir);
            BlockState signState = level.getBlockState(signPos);

            if (signState.getBlock() instanceof SignBlock) {
                // Read sign text
                BlockEntity be = level.getBlockEntity(signPos);
                if (be instanceof SignBlockEntity sign) {
                    String line1 = sign.getMessage(0, true).getString();
                    return parseCategoryFromLabel(line1);
                }
            }
        }

        // Check existing contents
        BlockEntity barrelBE = level.getBlockEntity(barrelPos);
        if (barrelBE instanceof BarrelBlockEntity barrel) {
            ItemStack firstItem = null;
            for (int i = 0; i < barrel.getContainerSize(); i++) {
                ItemStack stack = barrel.getItem(i);
                if (!stack.isEmpty()) {
                    firstItem = stack;
                    break;
                }
            }

            if (firstItem != null) {
                return ItemCategorizer.categorize(firstItem.getItem());
            }
        }

        return null;
    }

    /**
     * Deposit a single item stack into its designated barrel
     */
    private boolean depositItem(ItemStack stack) {
        ItemCategory category = ItemCategorizer.categorize(stack.getItem());
        BlockPos barrelPos = barrelLocations.get(category);

        if (barrelPos == null) {
            // No barrel for this category
            return false;
        }

        // Navigate to barrel
        if (!foreman.blockPosition().closerThan(barrelPos, 3.0)) {
            foreman.getNavigation().moveTo(barrelPos.getX(), barrelPos.getY(), barrelPos.getZ(), 1.0);
            return false; // Not there yet
        }

        // Deposit item
        Level level = foreman.level();
        BlockEntity be = level.getBlockEntity(barrelPos);

        if (be instanceof BarrelBlockEntity barrel) {
            // Try to insert
            ItemStack remainder = barrel.addItem(stack);
            if (remainder.isEmpty() || remainder.getCount() < stack.getCount()) {
                // Item deposited (fully or partially)
                level.playSound(null, barrelPos, SoundEvents.BARREL_OPEN,
                    SoundSource.BLOCKS, 0.5f, 1.0f);
                return true;
            }

            // Barrel full
            foreman.sendChatMessage("Barrel for " + category.getDisplayName() + " is full!");
            return true; // Count as processed (even if failed)
        }

        return false;
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Sorting items (" + itemsSorted + " done)";
    }
}
```

### 4.3 Sorting Algorithm Optimization

```java
/**
 * Advanced sorting using traveling salesman algorithm
 * Minimizes travel distance between barrels
 */
public class OptimizedSorter {

    /**
     * Sort items into barrels using optimal route
     * Uses nearest-neighbor TSP approximation
     */
    public List<SortOperation> planOptimalSortRoute(
        BlockPos agentPos,
        Map<ItemCategory, List<ItemStack>> itemsByCategory,
        Map<ItemCategory, BlockPos> barrelLocations
    ) {
        List<SortOperation> operations = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        BlockPos currentPos = agentPos;

        while (!itemsByCategory.isEmpty()) {
            // Find nearest barrel with items to deposit
            Map.Entry<ItemCategory, BlockPos> nearest = null;
            double nearestDistance = Double.MAX_VALUE;

            for (Map.Entry<ItemCategory, BlockPos> entry : barrelLocations.entrySet()) {
                if (visited.contains(entry.getValue())) continue;
                if (!itemsByCategory.containsKey(entry.getKey())) continue;

                double dist = currentPos.distSqr(entry.getValue());
                if (dist < nearestDistance) {
                    nearestDistance = dist;
                    nearest = entry;
                }
            }

            if (nearest == null) break; // No more reachable barrels

            // Add operation for this category
            operations.add(new SortOperation(
                nearest.getKey(),
                nearest.getValue(),
                itemsByCategory.get(nearest.getKey())
            ));

            itemsByCategory.remove(nearest.getKey());
            visited.add(nearest.getValue());
            currentPos = nearest.getValue();
        }

        return operations;
    }

    /**
     * Represents a single sorting operation
     */
    public record SortOperation(
        ItemCategory category,
        BlockPos barrelPos,
        List<ItemStack> items
    ) {}
}
```

---

## 5. Inventory Management

### 5.1 Storage Capacity Tracking

```java
/**
 * Tracks capacity and utilization of storage barrels
 */
public class StorageMonitor {

    private final Map<BlockPos, BarrelInfo> barrelInfo;
    private final ForemanEntity foreman;

    public StorageMonitor(ForemanEntity foreman) {
        this.foreman = foreman;
        this.barrelInfo = new ConcurrentHashMap<>();
    }

    /**
     * Scan and update barrel capacity information
     */
    public void updateStorageStatus() {
        BlockPos agentPos = foreman.blockPosition();
        Level level = foreman.level();

        for (int x = -16; x <= 16; x += 4) {
            for (int y = -2; y <= 5; y++) {
                for (int z = -16; z <= 16; z += 4) {
                    BlockPos pos = agentPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (state.is(Blocks.BARREL)) {
                        updateBarrelInfo(pos);
                    }
                }
            }
        }
    }

    /**
     * Update information for a single barrel
     */
    private void updateBarrelInfo(BlockPos pos) {
        Level level = foreman.level();
        BlockEntity be = level.getBlockEntity(pos);

        if (!(be instanceof BarrelBlockEntity barrel)) {
            return;
        }

        int usedSlots = 0;
        int totalItems = 0;
        Set<Item> itemTypes = new HashSet<>();

        for (int i = 0; i < barrel.getContainerSize(); i++) {
            ItemStack stack = barrel.getItem(i);
            if (!stack.isEmpty()) {
                usedSlots++;
                totalItems += stack.getCount();
                itemTypes.add(stack.getItem());
            }
        }

        ItemCategory category = null;
        if (!itemTypes.isEmpty()) {
            category = ItemCategorizer.categorize(itemTypes.iterator().next());
        }

        barrelInfo.put(pos, new BarrelInfo(
            pos,
            usedSlots,
            27, // Total slots
            totalItems,
            category,
            new ArrayList<>(itemTypes)
        ));
    }

    /**
     * Find a barrel with space for a specific item
     */
    public Optional<BlockPos> findBarrelForItem(Item item) {
        ItemCategory category = ItemCategorizer.categorize(item);

        return barrelInfo.values().stream()
            .filter(info -> info.category() == category)
            .filter(info -> info.usedSlots() < info.totalSlots())
            .filter(info -> !info.itemTypes().contains(item) ||
                           info.usedSlots() < info.totalSlots())
            .min(Comparator.comparingDouble(info ->
                foreman.blockPosition().distSqr(info.pos())));
    }

    /**
     * Get storage efficiency report
     */
    public StorageReport generateReport() {
        int totalBarrels = barrelInfo.size();
        int fullBarrels = 0;
        int emptyBarrels = 0;
        double avgFillPercentage = 0;

        for (BarrelInfo info : barrelInfo.values()) {
            double fillPercentage = (double) info.usedSlots() / info.totalSlots();
            avgFillPercentage += fillPercentage;

            if (info.usedSlots() == 0) emptyBarrels++;
            if (info.usedSlots() == info.totalSlots()) fullBarrels++;
        }

        if (totalBarrels > 0) {
            avgFillPercentage /= totalBarrels;
        }

        return new StorageReport(
            totalBarrels,
            fullBarrels,
            emptyBarrels,
            avgFillPercentage
        );
    }

    public record BarrelInfo(
        BlockPos pos,
        int usedSlots,
        int totalSlots,
        int totalItems,
        ItemCategory category,
        List<Item> itemTypes
    ) {}

    public record StorageReport(
        int totalBarrels,
        int fullBarrels,
        int emptyBarrels,
        double averageFillPercentage
    ) {}
}
```

### 5.2 Inventory Routing

```java
/**
 * Routes items to appropriate storage locations
 */
public class InventoryRouter {

    private final StorageMonitor storageMonitor;
    private final Map<ItemCategory, List<BlockPos>> categoryRoutes;

    /**
     * Determine optimal storage location for an item
     * Considers: category, distance, barrel capacity
     */
    public BlockPos routeItem(ItemStack stack) {
        Item item = stack.getItem();
        ItemCategory category = ItemCategorizer.categorize(item);

        // Try to find existing barrel for this category
        Optional<BlockPos> existingBarrel =
            storageMonitor.findBarrelForItem(item);

        if (existingBarrel.isPresent()) {
            return existingBarrel.get();
        }

        // Check for dedicated barrel for this category
        if (categoryRoutes.containsKey(category)) {
            for (BlockPos barrelPos : categoryRoutes.get(category)) {
                BarrelInfo info = storageMonitor.getBarrelInfo(barrelPos);
                if (info != null && info.usedSlots() < info.totalSlots()) {
                    return barrelPos;
                }
            }
        }

        // Find nearest barrel with space
        return storageMonitor.findNearestBarrelWithSpace();
    }

    /**
     * Consolidate partially filled barrels
     * Moves items to consolidate categories into fewer barrels
     */
    public List<TransferOperation> planConsolidation() {
        List<TransferOperation> transfers = new ArrayList<>();

        Map<ItemCategory, List<BarrelInfo>> byCategory =
            storageMonitor.getBarrelsByCategory();

        for (Map.Entry<ItemCategory, List<BarrelInfo>> entry : byCategory.entrySet()) {
            List<BarrelInfo> barrels = entry.getValue();

            // If multiple barrels for same category
            if (barrels.size() > 1) {
                // Sort by fill percentage (descending)
                barrels.sort((a, b) -> Double.compare(
                    ((double) b.usedSlots() / b.totalSlots()),
                    ((double) a.usedSlots() / a.totalSlots())
                ));

                // Plan transfers to fill primary barrel
                BarrelInfo primary = barrels.get(0);
                int availableSpace = primary.totalSlots() - primary.usedSlots();

                for (int i = 1; i < barrels.size(); i++) {
                    BarrelInfo secondary = barrels.get(i);

                    if (availableSpace > 0 && secondary.usedSlots() > 0) {
                        transfers.add(new TransferOperation(
                            secondary.pos(),
                            primary.pos(),
                            Math.min(secondary.usedSlots(), availableSpace)
                        ));

                        availableSpace -= secondary.usedSlots();
                    }
                }
            }
        }

        return transfers;
    }

    public record TransferOperation(
        BlockPos from,
        BlockPos to,
        int slotCount
    ) {}
}
```

---

## 6. Multi-Barrel Coordination

### 6.1 Distributed Storage System

```java
/**
 * Coordinates multiple agents sorting items simultaneously
 */
public class DistributedStorageCoordinator {

    private final Set<ForemanEntity> activeAgents;
    private final Map<BlockPos, AgentAssignment> barrelAssignments;
    private final Queue<SortTask> pendingTasks;

    /**
     * Assign barrels to agents to avoid conflicts
     */
    public void assignBarrelsToAgents(List<ForemanEntity> agents) {
        List<BlockPos> allBarrels = scanAllBarrels();

        // Partition barrels by spatial location
        Map<AgentZone, List<BlockPos>> zones =
            partitionBarrelsByZone(allBarrels, agents.size());

        // Assign each agent to a zone
        for (int i = 0; i < agents.size(); i++) {
            if (i >= zones.size()) break;

            AgentZone zone = new ArrayList<>(zones.keySet()).get(i);
            List<BlockPos> zoneBarrels = zones.get(zone);
            ForemanEntity agent = agents.get(i);

            for (BlockPos barrel : zoneBarrels) {
                barrelAssignments.put(barrel,
                    new AgentAssignment(agent, zone));
            }

            agent.sendChatMessage("Assigned to storage zone " + (i + 1) +
                " (" + zoneBarrels.size() + " barrels)");
        }
    }

    /**
     * Partition barrels into non-overlapping zones
     */
    private Map<AgentZone, List<BlockPos>> partitionBarrelsByZone(
        List<BlockPos> barrels,
        int zoneCount
    ) {
        if (barrels.isEmpty() || zoneCount <= 0) {
            return Map.of();
        }

        // Sort barrels by position
        barrels.sort(Comparator.comparingInt(pos -> pos.getX()));

        // Use spatial hashing for even distribution
        int minX = barrels.stream().mapToInt(BlockPos::getX).min().orElse(0);
        int maxX = barrels.stream().mapToInt(BlockPos::getX).max().orElse(0);
        int rangeX = maxX - minX;

        Map<AgentZone, List<BlockPos>> zones = new HashMap<>();

        for (BlockPos barrel : barrels) {
            // Determine zone based on X coordinate
            int zoneIndex = (int) ((barrel.getX() - minX) * zoneCount / (rangeX + 1));
            AgentZone zone = new AgentZone(zoneIndex);

            zones.computeIfAbsent(zone, k -> new ArrayList<>()).add(barrel);
        }

        return zones;
    }

    /**
     * Coordinate multiple agents sorting simultaneously
     */
    public void coordinateSorting(List<ForemanEntity> agents, List<ItemStack> items) {
        // Categorize items
        Map<ItemCategory, List<ItemStack>> byCategory =
            ItemCategorizer.sortByCategory(items);

        // Create tasks
        for (Map.Entry<ItemCategory, List<ItemStack>> entry : byCategory.entrySet()) {
            pendingTasks.add(new SortTask(entry.getKey(), entry.getValue()));
        }

        // Assign tasks to agents
        for (ForemanEntity agent : agents) {
            SortTask task = pendingTasks.poll();
            if (task != null) {
                assignSortTask(agent, task);
            }
        }
    }

    private void assignSortTask(ForemanEntity agent, SortTask task) {
        Task sortTask = new Task("sort_items", Map.of(
            "category", task.category().getId(),
            "items", task.items()
        ));

        agent.getActionExecutor().queueTask(sortTask);
    }

    private record AgentZone(int zoneId) {}
    private record AgentAssignment(ForemanEntity agent, AgentZone zone) {}
    private record SortTask(ItemCategory category, List<ItemStack> items) {}
}
```

### 6.2 Load Balancing

```java
/**
 * Balances storage load across multiple agents
 */
public class StorageLoadBalancer {

    /**
     * Redistribute barrels when agents join/leave
     */
    public void rebalance(List<ForemanEntity> agents) {
        Map<ForemanEntity, List<BlockPos>> currentAssignments =
            getCurrentAssignments();

        // Calculate target assignment size
        List<BlockPos> allBarrels = getAllBarrels();
        int barrelsPerAgent = allBarrels.size() / agents.size();

        // Reassign if needed
        for (ForemanEntity agent : agents) {
            List<BlockPos> assigned = currentAssignments.getOrDefault(
                agent, new ArrayList<>());

            if (assigned.size() > barrelsPerAgent * 1.5) {
                // This agent has too many barrels, reassign excess
                List<BlockPos> excess = assigned.subList(barrelsPerAgent, assigned.size());
                reassignBarrels(excess, agents, agent);
            }
        }
    }

    /**
     * Assign barrels to nearest available agent
     */
    private void reassignBarrels(
        List<BlockPos> barrels,
        List<ForemanEntity> agents,
        ForemanEntity excludeAgent
    ) {
        for (BlockPos barrel : barrels) {
            // Find nearest agent (excluding current)
            ForemanEntity nearest = agents.stream()
                .filter(a -> a != excludeAgent)
                .filter(a -> a.getActionExecutor().isExecuting() == false)
                .min(Comparator.comparingDouble(a ->
                    a.blockPosition().distSqr(barrel)))
                .orElse(null);

            if (nearest != null) {
                assignBarrelToAgent(barrel, nearest);
            }
        }
    }
}
```

---

## 7. Code Integration Examples

### 7.1 Storage Actions Plugin

```java
package com.minewright.storage.plugin;

import com.minewright.plugin.ActionPlugin;
import com.minewright.plugin.ActionRegistry;
import com.minewright.di.ServiceContainer;
import com.minewright.storage.actions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin that registers all storage-related actions
 */
public class StorageActionsPlugin implements ActionPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageActionsPlugin.class);
    private static final String PLUGIN_ID = "storage-actions";

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void onLoad(ActionRegistry registry, ServiceContainer container) {
        LOGGER.info("Loading StorageActionsPlugin");

        int priority = getPriority();

        // Register storage actions
        registry.register("build_storage",
            (foreman, task, ctx) -> new BuildStorageSystemAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("sort_items",
            (foreman, task, ctx) -> new SortItemsAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("deposit_item",
            (foreman, task, ctx) -> new DepositItemAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("withdraw_item",
            (foreman, task, ctx) -> new WithdrawItemAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("organize_storage",
            (foreman, task, ctx) -> new OrganizeStorageAction(foreman, task),
            priority, PLUGIN_ID);

        LOGGER.info("StorageActionsPlugin loaded {} actions", 5);
    }

    @Override
    public void onUnload() {
        LOGGER.info("StorageActionsPlugin unloaded");
    }

    @Override
    public int getPriority() {
        return 500; // Lower than core, higher than custom
    }

    @Override
    public String[] getDependencies() {
        return new String[]{"core-actions"};
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Barrel storage and item sorting actions";
    }
}
```

### 7.2 Example Usage in PromptBuilder

```java
/**
 * Extend PromptBuilder to include storage actions
 */
public class StoragePromptBuilder extends PromptBuilder {

    @Override
    protected String buildAvailableActions() {
        StringBuilder actions = new StringBuilder();
        actions.append(super.buildAvailableActions());

        actions.append("\n### Storage Actions\n");
        actions.append("- **build_storage**: Build a barrel storage system\n");
        actions.append("  - Parameters:\n");
        actions.append("    - layout: 'wall', 'radial', or 'stacked'\n");
        actions.append("    - categories: list of categories to include\n");
        actions.append("    - dimensions: [width, height, depth]\n\n");

        actions.append("- **sort_items**: Sort inventory items into barrels\n");
        actions.append("  - Parameters:\n");
        actions.append("    - source: 'inventory', 'chest', or 'all'\n");
        actions.append("    - scope: 'nearby' or 'all'\n\n");

        actions.append("- **deposit_item**: Deposit specific item type\n");
        actions.append("  - Parameters:\n");
        actions.append("    - item: item name (e.g., 'diamond', 'oak_log')\n");
        actions.append("    - quantity: number of items (0 = all)\n\n");

        actions.append("- **withdraw_item**: Withdraw items from storage\n");
        actions.append("  - Parameters:\n");
        actions.append("    - item: item name\n");
        actions.append("    - quantity: number to withdraw\n\n");

        actions.append("- **organize_storage**: Reorganize existing barrels\n");
        actions.append("  - Parameters:\n");
        actions.append("    - mode: 'consolidate', 'categorize', or 'optimize'\n\n");

        return actions.toString();
    }

    @Override
    protected String buildItemCategories() {
        StringBuilder categories = new StringBuilder();
        categories.append("### Item Storage Categories\n");

        for (ItemCategory category : ItemCategory.values()) {
            categories.append("- **").append(category.getId())
                .append("**: ").append(category.getDisplayName()).append("\n");
        }

        return categories.toString();
    }
}
```

### 7.3 Complete Example: Building and Using Storage

```java
/**
 * Example: Command to build storage and sort items
 */
public class StorageExample {

    /**
     * Player command: "Build a storage room for my resources"
     *
     * LLM generates tasks:
     * 1. build_storage with layout=wall, categories=[ores, wood, food, tools]
     * 2. sort_items with source=all, scope=nearby
     */

    public static void exampleUsage(ForemanEntity foreman) {
        ActionExecutor executor = foreman.getActionExecutor();

        // Queue storage system build
        Task buildTask = new Task("build_storage", Map.of(
            "layout", "wall",
            "categories", List.of("ores", "wood", "food", "tools", "redstone"),
            "dimensions", List.of(12, 4, 2)
        ));

        // Queue item sorting
        Task sortTask = new Task("sort_items", Map.of(
            "source", "all",
            "scope", "nearby"
        ));

        executor.queueTask(buildTask);
        executor.queueTask(sortTask);
    }
}
```

---

## 8. Implementation Roadmap

### Phase 1: Core Infrastructure (Week 1-2)
- [ ] Create `ItemCategorizer` with basic categories
- [ ] Implement tag-based category detection
- [ ] Add `StorageMonitor` for barrel scanning
- [ ] Create `StorageActionsPlugin` skeleton

### Phase 2: Build Actions (Week 3)
- [ ] Implement `BuildStorageSystemAction`
- [ ] Add wall-mounted layout pattern
- [ ] Add sign labeling for barrels
- [ ] Integrate with existing `PlaceBlockAction`

### Phase 3: Sort Actions (Week 4)
- [ ] Implement `SortItemsAction`
- [ ] Add barrel scanning and category detection
- [ ] Implement item deposit logic
- [ ] Add navigation to barrels

### Phase 4: Advanced Features (Week 5)
- [ ] Implement hopper network sorting
- [ ] Add NBT-aware categorization
- [ ] Create `InventoryRouter` for optimization
- [ ] Add storage consolidation

### Phase 5: Multi-Agent Coordination (Week 6)
- [ ] Implement `DistributedStorageCoordinator`
- [ ] Add zone-based barrel assignment
- [ ] Implement load balancing
- [ ] Add conflict resolution

### Phase 6: Polish and Testing (Week 7)
- [ ] Performance optimization
- [ ] Error handling and recovery
- [ ] User feedback integration
- [ ] Documentation and examples

---

## Appendix: Quick Reference

### A.1 Item Categories Summary

| Category | Items | Barrel Color Suggestion |
|----------|-------|------------------------|
| Ores | Coal, Iron, Gold, Copper, Diamond, etc. | Gray/Brown |
| Wood | Logs, planks, saplings, stairs | Oak/Spruce colors |
| Food | Bread, meat, crops, etc. | Green |
| Tools | Pickaxes, axes, shovels, etc. | Orange |
| Redstone | Redstone, repeaters, pistons | Red |
| Combat | Swords, bows, arrows | Dark Gray |
| Armor | Helmets, chestplates, etc. | Iron color |
| Rare | Diamonds, netherite, elytra | Cyan/Purple |
| Misc | Everything else | White |

### A.2 Barrel Capacity Reference

```
Single Barrel: 27 slots x 64 stack = 1,728 items max
Double Barrel (vertical): 3,456 items max
Standard Storage Room: 48 barrels = 82,944 items max
```

### A.3 Common Patterns

```
Wall Storage: 6x4 grid = 24 barrels
Row Storage: 12x1 row = 12 barrels
Radial Storage: 6 arms x 4 barrels = 24 barrels
Vertical Stack: 1 column x 5 high = 5 barrels
```

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Status:** Ready for Implementation
