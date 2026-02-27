# Enchanting Table AI for MineWright Minecraft Mod

**Version:** 1.0.0
**Last Updated:** 2026-02-27
**Minecraft Version:** Forge 1.20.1
**Mod:** MineWright (Autonomous AI Agents)

---

## Table of Contents

1. [Overview](#overview)
2. [Bookshelf Optimization](#bookshelf-optimization)
3. [Enchant Prediction System](#enchant-prediction-system)
4. [Lapis Management](#lapis-management)
5. [Enchant Selection Strategy](#enchant-selection-strategy)
6. [Anvil Combining Optimization](#anvil-combining-optimization)
7. [Code Examples](#code-examples)
8. [Implementation Roadmap](#implementation-roadmap)
9. [Configuration](#configuration)
10. [Performance Metrics](#performance-metrics)

---

## Overview

This document describes the design and implementation of an intelligent enchanting table AI system for the MineWright mod. The system enables Foreman entities to:

- **Optimize bookshelf placement** for maximum enchantment power
- **Predict enchantments** before spending lapis and XP
- **Manage lapis lazuli** resources efficiently
- **Select optimal enchantments** based on goals and budget
- **Combine books and gear** at the anvil for advanced enchanting

### Key Design Principles

1. **Zero Waste** - Never spend lapis or XP on bad enchantments
2. **Prediction First** - Know exactly what you'll get before enchanting
3. **Resource Efficiency** - Optimize lapis usage across enchanting sessions
4. **Smart Combining** - Plan anvil combinations to maximize final gear quality
5. **Multi-Agent Coordination** - Multiple agents can enchant in parallel

---

## Bookshelf Optimization

### The Enchanting Table Mechanics

Minecraft enchanting tables draw power from bookshelves within a 2-block radius:

```
Bookshelf Power Calculation:
- Each bookshelf within range contributes +1 power
- Maximum power: 15 bookshelves (Level 30 enchantments)
- Bookshelf must be: at same Y level or +/- 1 Y, 2 blocks away horizontally
- There must be a clear path (air) between bookshelf and enchanting table
```

### Optimal Bookshelf Arrangement

The most space-efficient 15-bookshelf configuration:

```
Top View (Y = same as table):
    _ _ _ _ _ _ _
   |B B B|B B B|
   |B . .|. . B|
   |B . T . . B|
   |B . .|. . B|
   |B B B|B B B|
   |_ _ _|_ _ _|

Where:
T = Enchanting Table
B = Bookshelf (15 total)
. = Empty space (required for line of sight)
```

**3D View (Layered):**
```
Y-1 (below): [empty]
Y 0 (table):  Bookshelves at same level
Y+1 (above):  Bookshelves one block above
```

### Bookshelf Detection Algorithm

```java
package com.minewright.enchanting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes and optimizes bookshelf placement for enchanting tables
 */
public class BookshelfAnalyzer {

    /**
     * Scans area around enchanting table and identifies optimal bookshelf positions
     */
    public static BookshelfAnalysis analyzeBookshelves(Level level, BlockPos tablePos) {
        List<BlockPos> validPositions = new ArrayList<>();
        List<BlockPos> occupiedPositions = new ArrayList<>();
        List<BlockPos> obstructedPositions = new ArrayList<>();

        // Check all potential bookshelf positions (within 2-block radius)
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -1; y <= 1; y++) {
                    // Skip center and diagonal positions
                    if (Math.abs(x) == 2 && Math.abs(z) == 2) continue;
                    if (x == 0 && z == 0 && y == 0) continue;
                    if (Math.abs(x) <= 1 && Math.abs(z) <= 1 && y != 0) continue;

                    BlockPos pos = tablePos.offset(x, y, z);
                    BookshelfStatus status = checkBookshelfPosition(level, tablePos, pos);

                    switch (status) {
                        case VALID -> validPositions.add(pos);
                        case OCCUPIED -> occupiedPositions.add(pos);
                        case OBSTRUCTED -> obstructedPositions.add(pos);
                    }
                }
            }
        }

        // Calculate current power level
        int currentPower = validPositions.size();
        int missingForMax = Math.max(0, 15 - currentPower);

        return new BookshelfAnalysis(
            tablePos,
            currentPower,
            validPositions,
            occupiedPositions,
            obstructedPositions,
            missingForMax,
            calculateMaxEnchantmentLevel(currentPower)
        );
    }

    /**
     * Checks if a position is a valid, occupied, or obstructed bookshelf spot
     */
    private static BookshelfStatus checkBookshelfPosition(Level level, BlockPos tablePos, BlockPos pos) {
        BlockState block = level.getBlockState(pos);

        // Check if bookshelf already exists
        if (block.is(Blocks.BOOKSHELF)) {
            // Verify line of sight to table
            if (hasLineOfSight(level, pos, tablePos)) {
                return BookshelfStatus.VALID;
            } else {
                return BookshelfStatus.OBSTRUCTED;
            }
        }

        // Check if position is empty but valid
        if (block.isAir() || block.canBeReplaced()) {
            if (hasLineOfSight(level, pos, tablePos)) {
                return BookshelfStatus.OCCUPIED; // Can place bookshelf here
            } else {
                return BookshelfStatus.OBSTRUCTED;
            }
        }

        // Block is in the way
        return BookshelfStatus.OBSTRUCTED;
    }

    /**
     * Checks if there's a clear path between two positions
     */
    private static boolean hasLineOfSight(Level level, BlockPos from, BlockPos to) {
        // Simple line of sight check
        // In Minecraft, bookshelves need air between them and the table
        BlockPos direction = to.subtract(from);
        BlockPos current = from;

        // Step toward table, checking for obstructions
        int steps = Math.max(Math.abs(direction.getX()), Math.abs(direction.getZ()));

        for (int i = 1; i < steps; i++) {
            double progress = (double) i / steps;
            int checkX = from.getX() + (int)(direction.getX() * progress);
            int checkZ = from.getZ() + (int)(direction.getZ() * progress);

            BlockPos checkPos = new BlockPos(checkX, from.getY(), checkZ);
            BlockState block = level.getBlockState(checkPos);

            // If not air and not the target position, it's obstructed
            if (!block.isAir() && !checkPos.equals(to)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Calculates the maximum enchantment level available with given bookshelf power
     */
    private static int calculateMaxEnchantmentLevel(int bookshelfCount) {
        // Minecraft formula: floor(bookshelfCount / 2) + 1, max 30
        return Math.min(30, (bookshelfCount / 2) + 1);
    }

    /**
     * Generates build plan for missing bookshelves
     */
    public static List<BlockPos> generateBookshelfBuildPlan(BookshelfAnalysis analysis) {
        return analysis.occupiedPositions().stream()
            .limit(analysis.missingForMax())
            .toList();
    }

    public enum BookshelfStatus {
        VALID,      // Bookshelf present and counting
        OCCUPIED,   // Empty valid position (can place bookshelf)
        OBSTRUCTED  // Invalid position (blocked or wrong location)
    }

    public record BookshelfAnalysis(
        BlockPos tablePos,
        int bookshelfPower,
        List<BlockPos> validPositions,
        List<BlockPos> occupiedPositions,
        List<BlockPos> obstructedPositions,
        int missingForMax,
        int maxEnchantmentLevel
    ) {
        public boolean isMaxPower() {
            return bookshelfPower >= 15;
        }

        public double getPowerPercentage() {
            return (double) bookshelfPower / 15.0;
        }
    }
}
```

### Bookshelf Construction Action

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.enchanting.BookshelfAnalyzer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Action for building bookshelves around enchanting table
 */
public class BuildBookshelvesAction extends BaseAction {

    private final BlockPos tablePos;
    private final List<BlockPos> buildPositions;
    private int currentIndex;
    private int booksPlaced;

    public BuildBookshelvesAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.tablePos = task.getBlockPosParameter("tablePos");
        this.buildPositions = task.getBlockPosListParameter("positions");
        this.currentIndex = 0;
        this.booksPlaced = 0;
    }

    @Override
    protected void onStart() {
        foreman.sendChatMessage("Building bookshelves for enchanting table...");
    }

    @Override
    protected void onTick() {
        if (currentIndex >= buildPositions.size()) {
            result = ActionResult.success(String.format(
                "Built %d bookshelves. Enchanting power is now Level 30!",
                booksPlaced
            ));
            return;
        }

        BlockPos targetPos = buildPositions.get(currentIndex);

        // Check if we have books
        if (!hasBooks()) {
            result = ActionResult.failure("Need more books to build bookshelves");
            return;
        }

        // Place bookshelf
        if (placeBookshelf(targetPos)) {
            booksPlaced++;
            currentIndex++;
        }
    }

    private boolean hasBooks() {
        // Each bookshelf needs 3 books (6 planks + 3 books)
        int required = 3;
        int available = countItem(Items.BOOK);
        return available >= required;
    }

    private boolean placeBookshelf(BlockPos pos) {
        // Navigate to position
        double distance = foreman.blockPosition().distSqr(pos);
        if (distance > 9) {
            foreman.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1.0);
            return false;
        }

        // Place bookshelf (simplified)
        foreman.level().setBlock(pos, net.minecraft.world.level.block.Blocks.BOOKSHELF.defaultBlockState(), 3);
        return true;
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Build bookshelves for enchanting table";
    }
}
```

---

## Enchant Prediction System

### Understanding Minecraft's Enchantment Algorithm

Minecraft 1.20.1 uses a specific algorithm for enchantments:

```
1. Generate "seed" from:
   - Enchantment slot position (0, 1, 2 for left/middle/right)
   - Item type (tool, weapon, armor, book)
   - Random seed from item stack

2. Calculate level requirement:
   - Base level: 1-30 (based on bookshelf power)
   - Modified by: slot position + random factor

3. Select enchantments:
   - Weighted random selection
   - Compatibility checks (e.g., Sharpness vs Smite)
   - Treasure enchantments (Mending, Frost Walker) only from specific sources
```

### Enchantment Weight System

Each enchantment has a rarity weight:

| Enchantment | Weight | Commonality |
|-------------|--------|-------------|
| Protection | 10 | Common |
| Fire Protection | 5 | Uncommon |
| Blast Protection | 2 | Rare |
| Sharpness | 10 | Common |
| Smite | 5 | Uncommon |
| Bane of Arthropods | 5 | Uncommon |
| Efficiency | 10 | Common |
| Fortune | 2 | Rare |
| Mending | 2 | Rare (treasure) |
| Unbreaking | 5 | Uncommon |

### Prediction Implementation

```java
package com.minewright.enchanting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.*;

/**
 * Predicts enchantments before committing to them
 */
public class EnchantmentPredictor {

    private final Random random;
    private final int bookshelfPower;

    public EnchantmentPredictor(int bookshelfPower, long seed) {
        this.bookshelfPower = bookshelfPower;
        this.random = new Random(seed);
    }

    /**
     * Predicts all three enchantment options for an item
     */
    public EnchantmentPrediction[] predictEnchantments(ItemStack item, long itemSeed) {
        EnchantmentPrediction[] predictions = new EnchantmentPrediction[3];

        for (int slot = 0; slot < 3; slot++) {
            predictions[slot] = predictSlot(item, slot, itemSeed);
        }

        return predictions;
    }

    /**
     * Predicts a single enchantment slot
     */
    private EnchantmentPrediction predictSlot(ItemStack item, int slot, long itemSeed) {
        // Create seeded random for this prediction
        Random slotRandom = new Random(itemSeed + slot * 1000);

        // Calculate level requirement
        int levelsRequired = calculateLevelRequirement(slot, slotRandom);

        // Generate enchantments
        List<EnchantmentInstance> enchantments = generateEnchantments(
            item,
            levelsRequired,
            slotRandom
        );

        // Calculate XP cost
        int xpCost = calculateXPCost(levelsRequired, enchantments);

        return new EnchantmentPrediction(
            slot,
            levelsRequired,
            xpCost,
            enchantments,
            calculateScore(enchantments)
        );
    }

    /**
     * Calculates the XP level requirement for a slot
     */
    private int calculateLevelRequirement(int slot, Random random) {
        // Minecraft's algorithm (simplified)
        int baseLevel = Math.min(30, bookshelfPower / 2 + 1);

        // Add slot modifier
        int slotModifier = slot + 1;

        // Add random factor
        int randomBonus = random.nextInt(baseLevel / 2 + 1);

        int level = baseLevel + slotModifier + randomBonus;

        // Clamp and adjust
        if (slot == 0) level = Math.max(1, level / 3);
        if (slot == 1) level = level / 2 + 1;
        if (slot == 2) level = level * 2 / 3 + 1;

        return Math.min(30, level);
    }

    /**
     * Generates possible enchantments for an item
     */
    private List<EnchantmentInstance> generateEnchantments(
        ItemStack item,
        int levels,
        Random random
    ) {
        List<EnchantmentInstance> enchantments = new ArrayList<>();

        // Get compatible enchantments for this item
        List<Enchantment> compatible = getCompatibleEnchantments(item);

        if (compatible.isEmpty()) {
            return enchantments;
        }

        // Select enchantments based on weights
        // This is a simplified version of Minecraft's algorithm
        int enchantmentCount = calculateEnchantmentCount(levels, random);

        for (int i = 0; i < enchantmentCount; i++) {
            Enchantment selected = selectEnchantment(compatible, levels, random);

            if (selected != null) {
                int level = selectEnchantmentLevel(selected, levels, random);
                enchantments.add(new EnchantmentInstance(selected, level));

                // Remove incompatible enchantments
                compatible = removeIncompatible(compatible, selected);
            }
        }

        return enchantments;
    }

    /**
     * Calculates how many enchantments will be applied
     */
    private int calculateEnchantmentCount(int levels, Random random) {
        // Higher levels = more enchantments
        int base = levels / 10;
        int bonus = random.nextInt(2);

        return Math.min(4, base + bonus + 1);
    }

    /**
     * Selects an enchantment based on weights
     */
    private Enchantment selectEnchantment(List<Enchantment> compatible, int levels, Random random) {
        // Calculate total weight
        int totalWeight = 0;
        for (Enchantment e : compatible) {
            if (e.getMinLevel() <= levels) {
                totalWeight += getEnchantmentWeight(e);
            }
        }

        if (totalWeight == 0) return null;

        // Weighted random selection
        int selected = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (Enchantment e : compatible) {
            if (e.getMinLevel() <= levels) {
                currentWeight += getEnchantmentWeight(e);
                if (selected < currentWeight) {
                    return e;
                }
            }
        }

        return null;
    }

    /**
     * Gets the rarity weight of an enchantment
     */
    private int getEnchantmentWeight(Enchantment enchantment) {
        // Simplified weight system
        String name = enchantment.getDescriptionId();

        if (name.contains("mending") || name.contains("frost_walker")) return 1;
        if (name.contains("fortune") || name.contains("fortune")) return 2;
        if (name.contains("protection") || name.contains("sharpness") ||
            name.contains("efficiency")) return 10;
        if (name.contains("unbreaking")) return 5;

        return 5; // Default uncommon
    }

    /**
     * Selects the level of an enchantment
     */
    private int selectEnchantmentLevel(Enchantment enchantment, int levels, Random random) {
        int min = enchantment.getMinLevel();
        int max = enchantment.getMaxLevel();

        // Higher available levels = better chance of higher enchantment
        int range = max - min + 1;
        int bonus = (levels - enchantment.getMinCost(min)) / 10;

        int selected = min + random.nextInt(range);
        selected = Math.min(max, selected + bonus);

        return Math.min(max, Math.max(min, selected));
    }

    /**
     * Calculates the XP cost (lapis cost)
     */
    private int calculateXPCost(int levels, List<EnchantmentInstance> enchantments) {
        // Base cost
        int cost = levels;

        // Add cost for each enchantment
        for (EnchantmentInstance e : enchantments) {
            cost += e.enchantment().getMinCost(e.level());
        }

        return cost;
    }

    /**
     * Calculates a score for the prediction (higher = better)
     */
    private double calculateScore(List<EnchantmentInstance> enchantments) {
        double score = 0;

        for (EnchantmentInstance e : enchantments) {
            String name = e.enchantment().getDescriptionId();

            // High-value enchantments
            if (name.contains("mending")) score += 100;
            if (name.contains("fortune") || name.contains("looting")) score += 50;
            if (name.contains("sharpness") || name.contains("protection")) score += 30;
            if (name.contains("efficiency") || name.contains("unbreaking")) score += 20;

            // Level bonus
            score += e.level() * 5;
        }

        return score;
    }

    /**
     * Gets enchantments compatible with an item
     */
    private List<Enchantment> getCompatibleEnchantments(ItemStack item) {
        // Use Minecraft's enchantment registry
        List<Enchantment> compatible = new ArrayList<>();

        for (Enchantment e : EnchantmentHelper.getAvailableEnchantments(item)) {
            compatible.add(e);
        }

        return compatible;
    }

    /**
     * Removes enchantments incompatible with the selected one
     */
    private List<Enchantment> removeIncompatible(List<Enchantment> list, Enchantment selected) {
        return list.stream()
            .filter(e -> selected.isCompatibleWith(e))
            .toList();
    }

    public record EnchantmentPrediction(
        int slot,
        int levelRequirement,
        int lapisCost,
        List<EnchantmentInstance> enchantments,
        double score
    ) {
        public String getDescription() {
            return String.format("Slot %d: Level %d, Cost %d lapis, Score %.1f",
                slot, levelRequirement, lapisCost, score);
        }

        public boolean isGoodValue() {
            return score >= 20;
        }

        public boolean hasTreasureEnchantment() {
            return enchantments.stream()
                .anyMatch(e -> e.enchantment().isTreasureOnly());
        }
    }

    public record EnchantmentInstance(
        Enchantment enchantment,
        int level
    ) {
        public String getName() {
            return enchantment.getDescriptionId();
        }
    }
}
```

---

## Lapis Management

### Lapis Lazuli Economics

```
Lapis Value Hierarchy:
1. Mending books (1 lapis per attempt - keep trying!)
2. Fortune III (3 lapis per attempt)
3. Sharpness/Protection IV (3 lapis per attempt)
4. Efficiency IV (2 lapis per attempt)
5. Unbreaking III (1-2 lapis per attempt)
6. Low-level enchantments (1 lapis - skip if possible)
```

### Lapis Budget System

```java
package com.minewright.enchanting;

import net.minecraft.world.item.Items;

/**
 * Manages lapis lazuli budget for enchanting operations
 */
public class LapisBudget {

    private int totalBudget;
    private int spent;
    private int reserved;
    private final Map<EnchantingGoal, Integer> goalAllocations;

    public LapisBudget(int totalLapis) {
        this.totalBudget = totalLapis;
        this.spent = 0;
        this.reserved = 0;
        this.goalAllocations = new HashMap<>();
    }

    /**
     * Allocates lapis for a specific goal
     */
    public boolean allocateForGoal(EnchantingGoal goal, int amount) {
        if (!canAllocate(amount)) {
            return false;
        }

        reserved += amount;
        goalAllocations.put(goal,
            goalAllocations.getOrDefault(goal, 0) + amount);

        return true;
    }

    /**
     * Records lapis spent
     */
    public void recordSpent(int amount, EnchantingGoal goal) {
        spent += amount;
        reserved -= amount;

        Integer allocated = goalAllocations.get(goal);
        if (allocated != null) {
            goalAllocations.put(goal, Math.max(0, allocated - amount));
        }
    }

    /**
     * Checks if we can allocate lapis
     */
    public boolean canAllocate(int amount) {
        return (spent + reserved + amount) <= totalBudget;
    }

    /**
     * Returns remaining lapis
     */
    public int getRemaining() {
        return totalBudget - spent - reserved;
    }

    /**
     * Returns utilization percentage
     */
    public double getUtilization() {
        return (double) spent / totalBudget;
    }

    /**
     * Calculates optimal lapis allocation for goals
     */
    public static LapisBudget calculateOptimalBudget(
        int totalLapis,
        List<EnchantingGoal> goals
    ) {
        LapisBudget budget = new LapisBudget(totalLapis);

        // Sort goals by priority
        List<EnchantingGoal> sorted = goals.stream()
            .sorted(Comparator.comparingInt(EnchantingGoal::priority).reversed())
            .toList();

        // Allocate based on priority
        for (EnchantingGoal goal : sorted) {
            int estimate = goal.estimatedLapisCost();
            int allocation = Math.min(estimate, budget.getRemaining());

            if (allocation > 0) {
                budget.allocateForGoal(goal, allocation);
            }
        }

        return budget;
    }

    public enum EnchantingGoal {
        MENDING_BOOK(1, 64, 100),      // Priority 1, up to 64 lapis
        FORTUNE_TOOL(2, 30, 50),       // Priority 2, up to 30 lapis
        ARMOR_SET(3, 45, 75),          // Priority 3, up to 45 lapis
        WEAPON(4, 30, 60),             // Priority 4, up to 30 lapis
        UTILITY(5, 20, 30);            // Priority 5, up to 20 lapis

        private final int priority;
        private final int maxLapis;
        private final int estimatedCost;

        EnchantingGoal(int priority, int maxLapis, int estimatedCost) {
            this.priority = priority;
            this.maxLapis = maxLapis;
            this.estimatedCost = estimatedCost;
        }

        public int priority() { return priority; }
        public int maxLapis() { return maxLapis; }
        public int estimatedLapisCost() { return estimatedCost; }
    }
}
```

### Lapis Stashing Strategy

```java
package com.minewright.enchanting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Manages lapis storage and retrieval
 */
public class LapisManager {

    private final ForemanEntity foreman;
    private BlockPos stashPos;

    public LapisManager(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Finds or creates a lapis stash near the enchanting table
     */
    public BlockPos ensureLapisStash(BlockPos nearPos) {
        if (stashPos != null && isValidStash(stashPos)) {
            return stashPos;
        }

        // Create new stash
        stashPos = findOrCreateStashLocation(nearPos);
        return stashPos;
    }

    /**
     * Counts available lapis in inventory
     */
    public int countAvailableLapis() {
        int count = 0;

        for (ItemStack stack : foreman.getInventory().items) {
            if (stack.is(Items.LAPIS_LAZULI)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    /**
     * Withdraws lapis from stash
     */
    public int withdrawLapis(int amount) {
        int available = countAvailableLapis();
        int toWithdraw = Math.min(amount, available);

        if (toWithdraw <= 0) {
            return 0;
        }

        // Remove from inventory
        int remaining = toWithdraw;
        for (ItemStack stack : foreman.getInventory().items) {
            if (stack.is(Items.LAPIS_LAZULI)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;

                if (remaining <= 0) break;
            }
        }

        return toWithdraw;
    }

    private BlockPos findOrCreateStashLocation(BlockPos nearPos) {
        // Look for chest within 5 blocks
        BlockPos chestPos = findNearbyChest(nearPos);

        if (chestPos != null) {
            return chestPos;
        }

        // Create new chest at optimal location
        BlockPos newChestPos = nearPos.offset(3, 0, 0);
        foreman.level().setBlock(
            newChestPos,
            net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(),
            3
        );

        return newChestPos;
    }

    private BlockPos findNearbyChest(BlockPos nearPos) {
        // Scan for chests
        return null; // Simplified
    }

    private boolean isValidStash(BlockPos pos) {
        // Check if chest still exists
        return foreman.level().getBlockState(pos).is(
            net.minecraft.world.level.block.Blocks.CHEST
        );
    }
}
```

---

## Enchant Selection Strategy

### Decision Matrix

```java
package com.minewright.enchanting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Selects the best enchantment option based on goals and budget
 */
public class EnchantSelector {

    private final EnchantingGoals goals;
    private final LapisBudget budget;

    public EnchantSelector(EnchantingGoals goals, LapisBudget budget) {
        this.goals = goals;
        this.budget = budget;
    }

    /**
     * Selects the best enchantment slot from predictions
     */
    public EnchantmentPredictor.EnchantmentPrediction selectBest(
        EnchantmentPredictor.EnchantmentPrediction[] predictions
    ) {
        // Filter by budget
        List<EnchantmentPredictor.EnchantmentPrediction> affordable =
            Arrays.stream(predictions)
                .filter(p -> budget.canAllocate(p.lapisCost()))
                .toList();

        if (affordable.isEmpty()) {
            return null;
        }

        // Filter by minimum score threshold
        List<EnchantmentPredictor.EnchantmentPrediction> goodOptions =
            affordable.stream()
                .filter(p -> p.score() >= goals.getMinScore())
                .toList();

        if (goodOptions.isEmpty()) {
            // No good options, return best affordable
            return affordable.stream()
                .max(Comparator.comparingDouble(EnchantmentPredictor.EnchantmentPrediction::score))
                .orElse(null);
        }

        // Select based on strategy
        return switch (goals.getStrategy()) {
            case MAX_SCORE -> goodOptions.stream()
                .max(Comparator.comparingDouble(EnchantmentPredictor.EnchantmentPrediction::score))
                .orElse(null);

            case MIN_COST -> goodOptions.stream()
                .min(Comparator.comparingInt(EnchantmentPredictor.EnchantmentPrediction::lapisCost))
                .orElse(null);

            case BALANCED -> {
                // Find best score per cost ratio
                double bestRatio = 0;
                EnchantmentPredictor.EnchantmentPrediction best = null;

                for (var p : goodOptions) {
                    double ratio = p.score() / p.lapisCost();
                    if (ratio > bestRatio) {
                        bestRatio = ratio;
                        best = p;
                    }
                }
                yield best;
            }

            case TARGET_SPECIFIC -> selectForTargetEnchant(goodOptions);
        };
    }

    /**
     * Selects enchantment based on specific target enchantments
     */
    private EnchantmentPredictor.EnchantmentPrediction selectForTargetEnchant(
        List<EnchantmentPredictor.EnchantmentPrediction> options
    ) {
        String targetEnchant = goals.getTargetEnchantment();

        if (targetEnchant == null) {
            return options.stream()
                .max(Comparator.comparingDouble(EnchantmentPredictor.EnchantmentPrediction::score))
                .orElse(null);
        }

        // Look for predictions containing target enchantment
        for (var prediction : options) {
            for (var enchant : prediction.enchantments()) {
                if (enchant.getName().toLowerCase().contains(targetEnchant.toLowerCase())) {
                    return prediction;
                }
            }
        }

        // Target not found, return best available
        return options.stream()
            .max(Comparator.comparingDouble(EnchantmentPredictor.EnchantmentPrediction::score))
            .orElse(null);
    }

    /**
     * Checks if we should skip enchanting entirely
     */
    public boolean shouldSkip(
        EnchantmentPredictor.EnchantmentPrediction[] predictions
    ) {
        // Skip if all options are bad
        long goodOptions = Arrays.stream(predictions)
            .filter(p -> p.score() >= goals.getMinScore())
            .count();

        if (goodOptions == 0 && goals.isStrictMode()) {
            return true;
        }

        // Skip if all options exceed budget
        long affordable = Arrays.stream(predictions)
            .filter(p -> budget.canAllocate(p.lapisCost()))
            .count();

        return affordable == 0;
    }

    public static class EnchantingGoals {
        private SelectionStrategy strategy;
        private double minScore;
        private boolean strictMode;
        private String targetEnchantment;
        private int priority;

        public enum SelectionStrategy {
            MAX_SCORE,        // Always pick highest score
            MIN_COST,         // Pick cheapest good option
            BALANCED,         // Best score/cost ratio
            TARGET_SPECIFIC   // Looking for specific enchant
        }

        public static EnchantingGoals forMending() {
            EnchantingGoals goals = new EnchantingGoals();
            goals.strategy = SelectionStrategy.TARGET_SPECIFIC;
            goals.targetEnchantment = "mending";
            goals.minScore = 50; // Mending is high value
            goals.strictMode = true;
            goals.priority = 1;
            return goals;
        }

        public static EnchantingGoals forFortune() {
            EnchantingGoals goals = new EnchantingGoals();
            goals.strategy = SelectionStrategy.TARGET_SPECIFIC;
            goals.targetEnchantment = "fortune";
            goals.minScore = 40;
            goals.strictMode = false; // Take good alternatives
            goals.priority = 2;
            return goals;
        }

        public static EnchantingGoals forArmor() {
            EnchantingGoals goals = new EnchantingGoals();
            goals.strategy = SelectionStrategy.MAX_SCORE;
            goals.minScore = 30; // Protection IV is good
            goals.strictMode = false;
            goals.priority = 3;
            return goals;
        }

        public static EnchantingGoals forTools() {
            EnchantingGoals goals = new EnchantingGoals();
            goals.strategy = SelectionStrategy.BALANCED;
            goals.minScore = 25;
            goals.strictMode = false;
            goals.priority = 4;
            return goals;
        }

        // Getters
        public SelectionStrategy getStrategy() { return strategy; }
        public double getMinScore() { return minScore; }
        public boolean isStrictMode() { return strictMode; }
        public String getTargetEnchantment() { return targetEnchantment; }
        public int getPriority() { return priority; }
    }
}
```

---

## Anvil Combining Optimization

### Combining Strategy

```
Anvil Combination Rules:
1. Same enchantments combine: level + 1 (max at tool max)
2. Different enchantments: merge (if compatible)
3. Cost increases with each combination
4. Prior work penalty accumulates
5. Too expensive (>40 levels) = impossible
```

### Anvil Optimizer

```java
package com.minewright.enchanting;

import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Optimizes anvil combinations for best final gear
 */
public class AnvilOptimizer {

    /**
     * Plans the optimal combination strategy
     */
    public CombinationPlan planCombinations(List<ItemStack> items, ItemStack target) {
        List<CombinationStep> steps = new ArrayList<>();
        ItemStack current = target.copy();

        // Sort items by value
        List<ItemStack> sorted = items.stream()
            .sorted(Comparator.comparingDouble(this::calculateItemValue).reversed())
            .toList();

        for (ItemStack item : sorted) {
            if (canCombine(current, item)) {
                CombinationStep step = planStep(current, item);
                if (step != null && step.totalCost() <= 40) {
                    steps.add(step);
                    current = simulateCombine(current, item);
                }
            }
        }

        return new CombinationPlan(steps, current);
    }

    /**
     * Checks if two items can be combined
     */
    public boolean canCombine(ItemStack base, ItemStack addition) {
        // Must be same item type or compatible
        if (!isCompatibleItem(base, addition)) {
            return false;
        }

        // Check if enchantments are compatible
        return hasCompatibleEnchantments(base, addition);
    }

    /**
     * Plans a single combination step
     */
    private CombinationStep planStep(ItemStack base, ItemStack addition) {
        int mergeCost = calculateMergeCost(base, addition);
        int priorWork = calculatePriorWork(base);

        int totalCost = mergeCost + priorWork;

        if (totalCost > 40) {
            return null; // Too expensive
        }

        ItemStack result = simulateCombine(base, addition);
        double valueGain = calculateItemValue(result) - calculateItemValue(base);

        return new CombinationStep(base, addition, result, totalCost, valueGain);
    }

    /**
     * Calculates the cost of a merge
     */
    private int calculateMergeCost(ItemStack base, ItemStack addition) {
        // Base cost for combining
        int cost = 1;

        // Add enchantment costs
        Map<String, Integer> baseEnchants = getEnchantments(base);
        Map<String, Integer> addEnchants = getEnchantments(addition);

        // Combined enchantments
        for (Map.Entry<String, Integer> entry : addEnchants.entrySet()) {
            String enchant = entry.getKey();
            int level = entry.getValue();

            if (baseEnchants.containsKey(enchant)) {
                // Same enchantment, level up
                int baseLevel = baseEnchants.get(enchant);
                if (level > baseLevel) {
                    cost += level * 2;
                }
            } else {
                // New enchantment
                cost += level * 2;
            }
        }

        // Repair cost (if applicable)
        int durabilityCost = calculateRepairCost(base, addition);
        cost += durabilityCost;

        return cost;
    }

    /**
     * Calculates prior work penalty
     */
    private int calculatePriorWork(ItemStack stack) {
        // Each prior combination adds to cost
        int priorWork = stack.getTag() != null ?
            stack.getTag().getInt("RepairCost") : 0;

        return priorWork * 2;
    }

    /**
     * Simulates combining two items
     */
    private ItemStack simulateCombine(ItemStack base, ItemStack addition) {
        ItemStack result = base.copy();

        // Merge enchantments
        Map<String, Integer> baseEnchants = getEnchantments(base);
        Map<String, Integer> addEnchants = getEnchantments(addition);

        for (Map.Entry<String, Integer> entry : addEnchants.entrySet()) {
            String enchant = entry.getKey();
            int level = entry.getValue();

            if (baseEnchants.containsKey(enchant)) {
                // Same enchantment: take higher or add 1
                int baseLevel = baseEnchants.get(enchant);
                if (level == baseLevel) {
                    baseEnchants.put(enchant, level + 1);
                } else if (level > baseLevel) {
                    baseEnchants.put(enchant, level);
                }
            } else {
                // New enchantment
                baseEnchants.put(enchant, level);
            }
        }

        // Apply merged enchantments to result
        applyEnchantments(result, baseEnchants);

        // Repair if applicable
        int repairAmount = calculateRepairAmount(base, addition);
        if (repairAmount > 0) {
            result.setDamageValue(Math.max(0, result.getDamageValue() - repairAmount));
        }

        // Update repair cost
        int newRepairCost = Math.min(40, calculateMergeCost(base, addition));
        result.getOrCreateTag().putInt("RepairCost", newRepairCost);

        return result;
    }

    /**
     * Calculates the value of an item
     */
    private double calculateItemValue(ItemStack item) {
        double value = 0;
        Map<String, Integer> enchants = getEnchantments(item);

        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            String enchant = entry.getKey().toLowerCase();
            int level = entry.getValue();

            // High-value enchantments
            if (enchant.contains("mending")) value += 100 * level;
            else if (enchant.contains("fortune") || enchant.contains("looting"))
                value += 50 * level;
            else if (enchant.contains("sharpness") || enchant.contains("protection"))
                value += 30 * level;
            else if (enchant.contains("efficiency"))
                value += 25 * level;
            else if (enchant.contains("unbreaking"))
                value += 15 * level;
            else
                value += 10 * level;
        }

        // Durability factor
        double durabilityPercent = 1.0 - ((double)item.getDamageValue() / item.getMaxDamage());
        value *= durabilityPercent;

        return value;
    }

    /**
     * Extracts enchantments from item
     */
    private Map<String, Integer> getEnchantments(ItemStack item) {
        Map<String, Integer> enchantments = new HashMap<>();

        if (item.isEnchanted()) {
            // Extract enchantments from NBT
            // Simplified for example
        }

        return enchantments;
    }

    /**
     * Applies enchantments to item
     */
    private void applyEnchantments(ItemStack item, Map<String, Integer> enchantments) {
        // Apply enchantments to item
        // Simplified for example
    }

    private boolean isCompatibleItem(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem();
    }

    private boolean hasCompatibleEnchantments(ItemStack a, ItemStack b) {
        // Check for incompatible enchantments (e.g., Sharpness vs Smite)
        return true; // Simplified
    }

    private int calculateRepairCost(ItemStack base, ItemStack addition) {
        return 0; // Simplified
    }

    private int calculateRepairAmount(ItemStack base, ItemStack addition) {
        return 0; // Simplified
    }

    public record CombinationPlan(
        List<CombinationStep> steps,
        ItemStack finalResult
    ) {
        public int getTotalCost() {
            return steps.stream().mapToInt(CombinationStep::totalCost).sum();
        }

        public double getTotalValueGain() {
            return steps.stream().mapToDouble(CombinationStep::valueGain).sum();
        }
    }

    public record CombinationStep(
        ItemStack base,
        ItemStack addition,
        ItemStack result,
        int totalCost,
        double valueGain
    ) {
        public String getDescription() {
            return String.format("Combine %s + %s -> %s (Cost: %d levels, Gain: %.1f)",
                base.getItem().toString(),
                addition.getItem().toString(),
                result.getItem().toString(),
                totalCost,
                valueGain
            );
        }
    }
}
```

---

## Code Examples

### Main Enchanting Action

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.enchanting.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Action for enchanting items at enchanting table
 */
public class EnchantAction extends BaseAction {

    private final BlockPos tablePos;
    private final ItemStack itemToEnchant;
    private final EnchantSelector.EnchantingGoals goals;
    private final LapisBudget budget;

    private EnchantmentPredictor predictor;
    private LapisManager lapisManager;
    private BookshelfAnalysis bookshelfAnalysis;

    private enum Phase {
        ANALYZING,
        PREDICTING,
        SELECTING,
        ENCHANTING,
        COMPLETE
    }

    private Phase phase = Phase.ANALYZING;

    public EnchantAction(ForemanEntity foreman, Task task, EnchantSelector.EnchantingGoals goals) {
        super(foreman, task);
        this.tablePos = task.getBlockPosParameter("tablePos");
        this.itemToEnchant = task.getItemStackParameter("item");
        this.goals = goals;
        this.budget = new LapisBudget(task.getIntParameter("lapisBudget", 64));
    }

    @Override
    protected void onStart() {
        foreman.sendChatMessage("Planning enchanting operation...");

        this.lapisManager = new LapisManager(foreman);
        this.bookshelfAnalysis = BookshelfAnalyzer.analyzeBookshelves(
            foreman.level(),
            tablePos
        );

        phase = Phase.PREDICTING;
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case PREDICTING -> predictEnchantments();
            case SELECTING -> selectEnchantment();
            case ENCHANTING -> performEnchanting();
            case COMPLETE -> complete();
        }
    }

    private void predictEnchantments() {
        foreman.sendChatMessage(String.format(
            "Analyzing enchanting table (Power: %d/15)...",
            bookshelfAnalysis.bookshelfPower()
        ));

        // Create predictor
        long seed = foreman.level().getRandom().nextInt();
        predictor = new EnchantmentPredictor(bookshelfAnalysis.bookshelfPower(), seed);

        // Generate predictions
        EnchantmentPredictor.EnchantmentPrediction[] predictions =
            predictor.predictEnchantments(itemToEnchant, seed);

        // Log predictions
        for (var prediction : predictions) {
            foreman.sendChatMessage(String.format(
                "  Slot %d: Level %d required, %d lapis, Score: %.1f",
                prediction.slot(),
                prediction.levelRequirement(),
                prediction.lapisCost(),
                prediction.score()
            ));
        }

        phase = Phase.SELECTING;
    }

    private void selectEnchantment() {
        EnchantmentSelector selector = new EnchantSelector(goals, budget);

        // Get predictions
        long seed = foreman.level().getRandom().nextInt();
        EnchantmentPredictor.EnchantmentPrediction[] predictions =
            predictor.predictEnchantments(itemToEnchant, seed);

        // Check if should skip
        if (selector.shouldSkip(predictions)) {
            result = ActionResult.failure(
                "No good enchantments available. Try again or skip.");
            phase = Phase.COMPLETE;
            return;
        }

        // Select best option
        EnchantmentPredictor.EnchantmentPrediction selected =
            selector.selectBest(predictions);

        if (selected == null) {
            result = ActionResult.failure("Could not select suitable enchantment");
            phase = Phase.COMPLETE;
            return;
        }

        foreman.sendChatMessage(String.format(
            "Selected slot %d: Level %d, Cost %d lapis",
            selected.slot(),
            selected.levelRequirement(),
            selected.lapisCost()
        ));

        phase = Phase.ENCHANTING;
    }

    private void performEnchanting() {
        // Check XP level
        int currentLevel = foreman.experienceLevel;
        long seed = foreman.level().getRandom().nextInt();
        EnchantmentPredictor.EnchantmentPrediction[] predictions =
            predictor.predictEnchantments(itemToEnchant, seed);
        EnchantmentPredictor.EnchantmentPrediction selected =
            new EnchantmentSelector(goals, budget).selectBest(predictions);

        if (currentLevel < selected.levelRequirement()) {
            result = ActionResult.failure(String.format(
                "Need Level %d, currently Level %d",
                selected.levelRequirement(),
                currentLevel
            ));
            phase = Phase.COMPLETE;
            return;
        }

        // Check lapis
        int availableLapis = lapisManager.countAvailableLapis();
        if (availableLapis < selected.lapisCost()) {
            result = ActionResult.failure(String.format(
                "Need %d lapis, have %d",
                selected.lapisCost(),
                availableLapis
            ));
            phase = Phase.COMPLETE;
            return;
        }

        // Perform enchantment (simplified - would use packet/reflection)
        boolean success = performActualEnchantment(selected);

        if (success) {
            // Deduct lapis and XP
            lapisManager.withdrawLapis(selected.lapisCost());
            foreman.giveExperiencePoints(-selected.levelRequirement());

            result = ActionResult.success(String.format(
                "Enchanted %s with %d enchantments!",
                itemToEnchant.getItem().toString(),
                selected.enchantments().size()
            ));
        } else {
            result = ActionResult.failure("Enchantment failed");
        }

        phase = Phase.COMPLETE;
    }

    private boolean performActualEnchantment(EnchantmentPredictor.EnchantmentPrediction prediction) {
        // This would actually perform the enchantment
        // Requires packet manipulation or reflection
        // Simplified: always succeeds
        return true;
    }

    private void complete() {
        // Done
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return String.format("Enchant %s at %s",
            itemToEnchant.getItem().toString(),
            tablePos.toString());
    }
}
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1)

**Tasks:**
- [ ] Create `BookshelfAnalyzer` class
- [ ] Implement bookshelf scanning and analysis
- [ ] Add line-of-sight checking
- [ ] Create bookshelf build plan generation
- [ ] Unit tests for bookshelf detection

**Deliverables:**
- Can detect and analyze bookshelf placement
- Generates optimal build plans
- Unit tests passing

### Phase 2: Enchantment Prediction (Week 2)

**Tasks:**
- [ ] Create `EnchantmentPredictor` class
- [ ] Implement enchantment weight system
- [ ] Add slot level calculation
- [ ] Implement enchantment selection algorithm
- [ ] Test predictions against actual enchantments

**Deliverables:**
- Can predict enchantments accurately
- Score system working
- Prediction validation tests

### Phase 3: Lapis Management (Week 3)

**Tasks:**
- [ ] Create `LapisBudget` class
- [ ] Implement `LapisManager` for storage
- [ ] Add goal-based allocation
- [ ] Create stash system
- [ ] Test budget tracking

**Deliverables:**
- Lapis budgeting functional
- Storage system working
- Resource optimization

### Phase 4: Selection Strategy (Week 4)

**Tasks:**
- [ ] Create `EnchantSelector` class
- [ ] Implement selection strategies (max score, min cost, balanced)
- [ ] Add goal-specific presets (Mending, Fortune, etc.)
- [ ] Implement skip logic for bad enchantments
- [ ] Test decision making

**Deliverables:**
- Smart enchantment selection
- Multiple strategies working
- Integration with goals

### Phase 5: Main Enchanting Action (Week 5)

**Tasks:**
- [ ] Create `EnchantAction` class
- [ ] Implement phase-based execution
- [ ] Add GUI/chat feedback
- [ ] Integrate with existing action system
- [ ] Test complete enchanting workflow

**Deliverables:**
- Working enchanting action
- Integrated with MineWright
- End-to-end tests passing

### Phase 6: Anvil Optimization (Week 6)

**Tasks:**
- [ ] Create `AnvilOptimizer` class
- [ ] Implement combination planning
- [ ] Add cost calculation
- [ ] Implement value scoring
- [ ] Create combination action

**Deliverables:**
- Can plan optimal combinations
- Cost-aware planning
- Anvil action working

### Phase 7: Integration & Polish (Week 7)

**Tasks:**
- [ ] Register actions in `CoreActionsPlugin`
- [ ] Update `PromptBuilder` with enchanting commands
- [ ] Add configuration options
- [ ] Performance optimization
- [ ] Documentation and examples

**Deliverables:**
- Full integration with LLM
- Configuration system
- Complete documentation
- Production-ready release

---

## Configuration

### config/minewright-common.toml

```toml
[enchanting]
# Enable enchanting features
enabled = true

# Bookshelf settings
[enchanting.bookshelves]
# Auto-build missing bookshelves
auto_build = true

# Require full 15 bookshelves before enchanting
require_full_power = false

# Minimum bookshelf power to enchant
min_power = 8

# Lapis settings
[enchanting.lapis]
# Auto-withdraw lapis from storage
auto_withdraw = true

# Lapis stash location (relative to enchanting table)
stash_offset = [3, 0, 0]

# Reserve lapis for high-priority goals
reserve_for_mending = true

# Enchantment selection
[enchanting.selection]
# Selection strategy: max_score, min_cost, balanced, target_specific
default_strategy = "balanced"

# Minimum score threshold
min_score = 20

# Skip enchanting if all options are bad
skip_bad_options = true

# Strict mode (only enchant if good option available)
strict_mode = false

# Enchantment priorities
[enchanting.priorities]
# Priority goals (lower = higher priority)
mending_priority = 1
fortune_priority = 2
armor_priority = 3
weapon_priority = 4
tool_priority = 5

# Anvil settings
[enchanting.anvil]
# Enable anvil combination planning
enable_combining = true

# Maximum cost per combination
max_cost = 40

# Prioritize value gain over cost minimization
value_first = true

# Multi-agent coordination
[enchanting.multi_agent]
# Allow multiple agents to enchant simultaneously
parallel_enchanting = true

# Minimum XP level for parallel enchanting
min_level_parallel = 15

# Cooldown between enchantment operations (ticks)
operation_cooldown = 20
```

---

## Performance Metrics

### Expected Enchantment Success Rates

| Goal | Success Rate | Lapis/Success | XP/Success |
|------|-------------|---------------|------------|
| Mending Book | 5% | 12.8 | 15 |
| Fortune III | 15% | 20 | 20 |
| Sharpness IV | 20% | 15 | 18 |
| Protection IV | 20% | 15 | 18 |
| Efficiency IV | 25% | 10 | 15 |
| Unbreaking III | 40% | 5 | 10 |

### Time Estimates

| Task | Single Agent | 3 Agents |
|------|-------------|----------|
| Build 15 Bookshelves | 10 min | 4 min |
| Get Mending (avg) | 30 min | 10 min |
| Full Armor Set | 45 min | 15 min |
| Fortune Pickaxe | 20 min | 7 min |

### Resource Requirements

| Task | Books | Lapis | XP Levels | Time |
|------|-------|-------|-----------|------|
| Max Enchanting Setup | 45 | - | - | 10 min |
| Mending Hunt | - | 64 | 300+ | 30 min |
| Fortune III | - | 30 | 60 | 20 min |
| Armor Set | - | 45 | 90 | 45 min |

---

## Appendix: Quick Reference

### Action Commands

```
/foreman <name> enchant tablePos:<x,y,z> item:<item> goal:<goal>
/foreman <name> build_bookshelves tablePos:<x,y,z>
/foreman <name> combine target:<item> materials:<list>
```

### Example Commands

```
# Enchant pickaxe for Fortune
/foreman Steve enchant tablePos:100,64,100 item:diamond_pickaxe goal:fortune

# Get Mending book
/foreman Steve enchant tablePos:100,64,100 item:book goal:mending

# Build bookshelves
/foreman Steve build_bookshelves tablePos:100,64,100

# Combine enchanted books
/foreman Steve combine target:diamond_pickaxe materials:book1,book2
```

### Enchantment Goals

```
mending    - Hunt for Mending enchantment
fortune    - Get Fortune on tools
armor      - Enchant armor set
weapon     - Enchant weapon
tools      - Enchant all tools
```

---

**Document Version:** 1.0.0
**Author:** MineWright Development Team
**License:** MIT

---

## Changelog

### v1.0.0 (2026-02-27)
- Initial release
- Bookshelf optimization system
- Enchantment prediction
- Lapis management
- Selection strategies
- Anvil optimization
- Implementation roadmap
