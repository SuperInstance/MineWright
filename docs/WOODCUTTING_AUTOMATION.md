# Woodcutting Automation AI Design

**Author:** Claude Code
**Date:** 2026-02-27
**Version:** 1.0
**Status:** Design Document

## Table of Contents

1. [Overview](#overview)
2. [Tree Detection System](#tree-detection-system)
3. [Efficient Felling Patterns](#efficient-felling-patterns)
4. [Leaf Decay Optimization](#leaf-decay-optimization)
5. [Replanting Logic](#replanting-logic)
6. [Multi-Tree Coordination](#multi-tree-coordination)
7. [Implementation Examples](#implementation-examples)
8. [Implementation Roadmap](#implementation-roadmap)

---

## Overview

The Woodcutting Automation system enables MineWright AI agents to autonomously detect, fell, and replant trees with maximum efficiency. This system integrates with the existing action framework and supports multi-agent coordination for large-scale forestry operations.

### Key Features

- **Intelligent Tree Detection**: Multi-algorithm approach combining BFS, flood-fill, and pattern matching
- **Optimized Felling Patterns**: Top-down, spiral, and segmented cutting strategies
- **Smart Leaf Decay**: Waiting vs. active clearing based on tree size and agent count
- **Automatic Replanting**: Sapling selection, placement, and growth acceleration
- **Multi-Agent Coordination**: Forest partitioning, parallel processing, and resource sharing
- **Tool Management**: Automatic axe selection, durability monitoring, and replacement

### Integration Points

```java
// Registration in CoreActionsPlugin
registry.register("chop",
    (foreman, task, ctx) -> new ChopTreeAction(foreman, task),
    priority, PLUGIN_ID);

registry.register("deforest",
    (foreman, task, ctx) -> new DeforestAreaAction(foreman, task),
    priority, PLUGIN_ID);

registry.register("plant_forest",
    (foreman, task, ctx) -> new PlantForestAction(foreman, task),
    priority, PLUGIN_ID);
```

---

## Tree Detection System

### 1. Tree Definition and Types

A tree is defined as a vertical structure consisting of:
- **Trunk**: Log blocks stacked vertically (same wood type)
- **Canopy**: Leaf blocks surrounding the trunk
- **Base**: Ground-level position where sapling was planted

#### Supported Tree Types

| Tree Type | Log Block | Leaf Block | Max Height | Sapling |
|-----------|-----------|------------|------------|---------|
| Oak | `oak_log` | `oak_leaves` | 5-7 | `oak_sapling` |
| Birch | `birch_log` | `birch_leaves` | 5-7 | `birch_sapling` |
| Spruce | `spruce_log` | `spruce_leaves` | 8-15 | `spruce_sapling` |
| Jungle | `jungle_log` | `jungle_leaves` | 10-20 | `jungle_sapling` |
| Acacia | `acacia_log` | `acacia_leaves` | 6-8 | `acacia_sapling` |
| Dark Oak | `dark_oak_log` | `dark_oak_leaves` | 6-8 | `dark_oak_sapling` |
| Mangrove | `mangrove_log` | `mangrove_leaves` | 10-15 | `mangrove_propagule` |
| Cherry | `cherry_log` | `cherry_leaves` | 6-8 | `cherry_sapling` |
| Crimson | `crimson_stem` | `nether_wart_block` | 8-15 | `crimson_fungus` |
| Warped | `warped_stem` | `warped_wart_block` | 8-15 | `warped_fungus` |

### 2. Detection Algorithms

#### Algorithm 1: Breadth-First Search (BFS)

Best for: Dense forests, finding nearest tree quickly

```java
public class TreeDetector {
    private static final int MAX_SEARCH_RADIUS = 32;
    private static final int MAX_TREE_HEIGHT = 25;

    public static Optional<Tree> findNearestTree(Level level, BlockPos startPos) {
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            // Check if current position is a log block
            if (isLogBlock(level, current)) {
                Optional<Tree> tree = analyzeTree(level, current);
                if (tree.isPresent()) {
                    return tree;
                }
            }

            // Add neighbors to queue
            for (BlockPos neighbor : getNeighbors(current)) {
                if (!visited.contains(neighbor) &&
                    startPos.distSqr(neighbor) <= MAX_SEARCH_RADIUS * MAX_SEARCH_RADIUS) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return Optional.empty();
    }
}
```

#### Algorithm 2: Vertical Column Scan

Best for: Open areas, sparse forests, tall trees

```java
public static Optional<Tree> findTreeByColumnScan(Level level, BlockPos center) {
    int radius = 16;

    for (int x = -radius; x <= radius; x++) {
        for (int z = -radius; z <= radius; z++) {
            BlockPos checkPos = center.offset(x, 0, z);

            // Find ground level
            BlockPos groundPos = findGroundLevel(level, checkPos);
            if (groundPos == null) continue;

            // Check for log starting at ground
            if (isLogBlock(level, groundPos.above())) {
                Optional<Tree> tree = analyzeTree(level, groundPos.above());
                if (tree.isPresent()) {
                    return tree;
                }
            }
        }
    }

    return Optional.empty();
}
```

#### Algorithm 3: Leaf-to-Trunk Reverse Search

Best for: Large trees, jungle trees, dark oak (2x2 trunks)

```java
public static Optional<Tree> findTreeFromLeaves(Level level, BlockPos start) {
    Queue<BlockPos> queue = new LinkedList<>();
    Set<BlockPos> visited = new HashSet<>();
    Map<BlockPos, Integer> distance = new HashMap<>();

    // Start from leaf blocks
    for (BlockPos pos : getLeafBlocksInRange(level, start, 16)) {
        queue.add(pos);
        distance.put(pos, 0);
        visited.add(pos);
    }

    while (!queue.isEmpty()) {
        BlockPos current = queue.poll();
        int dist = distance.get(current);

        if (dist > 10) continue; // Max distance from leaves to trunk

        if (isLogBlock(level, current)) {
            // Found trunk, trace down to base
            Optional<Tree> tree = analyzeTree(level, current);
            if (tree.isPresent()) {
                return tree;
            }
        }

        // Search outward
        for (BlockPos neighbor : getNeighbors(current)) {
            BlockState state = level.getBlockState(neighbor);
            if ((isLogBlock(level, neighbor) || isLeafBlock(state)) &&
                !visited.contains(neighbor)) {
                visited.add(neighbor);
                distance.put(neighbor, dist + 1);
                queue.add(neighbor);
            }
        }
    }

    return Optional.empty();
}
```

### 3. Tree Analysis

```java
public class TreeAnalyzer {

    public static Optional<Tree> analyzeTree(Level level, BlockPos trunkBase) {
        if (!isLogBlock(level, trunkBase)) {
            return Optional.empty();
        }

        Block logType = level.getBlockState(trunkBase).getBlock();
        Block leafType = getCorrespondingLeaf(logType);

        // Trace trunk upward
        List<BlockPos> trunkBlocks = new ArrayList<>();
        BlockPos current = trunkBase;

        while (isLogBlock(level, current) && trunkBlocks.size() < MAX_TREE_HEIGHT) {
            trunkBlocks.add(current);
            current = current.above();
        }

        if (trunkBlocks.isEmpty()) {
            return Optional.empty();
        }

        // Check for 2x2 trunk (dark oak, jungle)
        int trunkWidth = detectTrunkWidth(level, trunkBase);

        // Find all leaf blocks
        Set<BlockPos> leafBlocks = findConnectedLeaves(level, trunkBlocks, leafType);

        // Determine tree base
        BlockPos treeBase = findTreeBase(level, trunkBase);

        Tree tree = new Tree(
            generateTreeId(),
            logType,
            leafType,
            trunkBlocks,
            leafBlocks,
            treeBase,
            trunkBlocks.size(),
            trunkWidth
        );

        return Optional.of(tree);
    }

    private static int detectTrunkWidth(Level level, BlockPos base) {
        // Check adjacent blocks at base level
        boolean hasLogEast = isLogBlock(level, base.east());
        boolean hasLogSouth = isLogBlock(level, base.south());
        boolean hasLogSouthEast = isLogBlock(level, base.east().south());

        if (hasLogEast && hasLogSouth && hasLogSouthEast) {
            return 2; // 2x2 trunk
        }
        return 1; // 1x1 trunk
    }

    private static Set<BlockPos> findConnectedLeaves(Level level,
                                                     List<BlockPos> trunkBlocks,
                                                     Block expectedLeafType) {
        Set<BlockPos> leaves = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        // Start from top of trunk, search outward
        BlockPos treeTop = trunkBlocks.get(trunkBlocks.size() - 1);

        for (BlockPos neighbor : getNeighbors(treeTop)) {
            BlockState state = level.getBlockState(neighbor);
            if (isLeafBlock(state)) {
                queue.add(neighbor);
                leaves.add(neighbor);
            }
        }

        // Flood fill through leaves
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            for (BlockPos neighbor : getNeighbors(current)) {
                if (!leaves.contains(neighbor)) {
                    BlockState state = level.getBlockState(neighbor);
                    if (isLeafBlock(state)) {
                        leaves.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        return leaves;
    }
}
```

### 4. Tree Data Structure

```java
public class Tree {
    private final String treeId;
    private final Block logType;
    private final Block leafType;
    private final List<BlockPos> trunkBlocks;
    private final Set<BlockPos> leafBlocks;
    private final BlockPos basePosition;
    private final int height;
    private final int trunkWidth; // 1 or 2
    private final TreeType treeType;
    private final Instant discoveryTime;

    public enum TreeType {
        OAK, BIRCH, SPRUCE, JUNGLE, ACACIA,
        DARK_OAK, MANGROVE, CHERRY, CRIMSON, WARPED
    }

    public int getLogCount() {
        return trunkBlocks.size() * (trunkWidth * trunkWidth);
    }

    public int getEstimatedLeafDrops() {
        // Approximation: 1 sapling per 20 leaf blocks
        return leafBlocks.size() / 20;
    }

    public boolean isWithinRange(BlockPos pos, int radius) {
        return basePosition.distSqr(pos) <= radius * radius;
    }
}
```

---

## Efficient Felling Patterns

### 1. Top-Down Pattern (Standard)

Best for: Most trees, single agent

```java
public class TopDownFellingStrategy implements FellingStrategy {

    @Override
    public List<BlockPos> getFellingOrder(Tree tree) {
        List<BlockPos> order = new ArrayList<>();
        List<BlockPos> trunk = new ArrayList<>(tree.getTrunkBlocks());

        // Reverse order: top to bottom
        trunk.sort((a, b) -> Integer.compare(b.getY(), a.getY()));

        order.addAll(trunk);
        return order;
    }

    @Override
    public String getDescription() {
        return "Top-down felling (cuts from top to bottom)";
    }
}
```

### 2. Spiral Pattern (Large Trees)

Best for: Jungle trees, spruce, minimizing travel distance

```java
public class SpiralFellingStrategy implements FellingStrategy {

    @Override
    public List<BlockPos> getFellingOrder(Tree tree) {
        List<BlockPos> order = new ArrayList<>();

        if (tree.getTrunkWidth() == 1) {
            // Single trunk: top-down with spiral optimization
            List<BlockPos> trunk = new ArrayList<>(tree.getTrunkBlocks());
            trunk.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
            order.addAll(trunk);
        } else {
            // 2x2 trunk: process in layers
            List<BlockPos> allTrunkBlocks = getAllTrunkBlocks(tree);
            Map<Integer, List<BlockPos>> byY = groupByY(allTrunkBlocks);

            // Process from top to bottom
            List<Integer> yLevels = new ArrayList<>(byY.keySet());
            yLevels.sort(Collections.reverseOrder());

            for (int y : yLevels) {
                order.addAll(byY.get(y));
            }
        }

        return order;
    }

    private List<BlockPos> getAllTrunkBlocks(Tree tree) {
        List<BlockPos> allBlocks = new ArrayList<>();
        BlockPos base = tree.getBasePosition();
        int height = tree.getHeight();

        if (tree.getTrunkWidth() == 2) {
            // 2x2 trunk
            for (int y = 0; y < height; y++) {
                allBlocks.add(base.offset(0, y, 0));
                allBlocks.add(base.offset(1, y, 0));
                allBlocks.add(base.offset(0, y, 1));
                allBlocks.add(base.offset(1, y, 1));
            }
        } else {
            allBlocks.addAll(tree.getTrunkBlocks());
        }

        return allBlocks;
    }
}
```

### 3. Segmented Pattern (Multi-Agent)

Best for: Multiple agents on large trees

```java
public class SegmentedFellingStrategy implements FellingStrategy {
    private final int segmentCount;

    public SegmentedFellingStrategy(int segmentCount) {
        this.segmentCount = segmentCount;
    }

    @Override
    public Map<String, List<BlockPos>> getAgentSegments(Tree tree, List<String> agentIds) {
        List<BlockPos> trunk = new ArrayList<>(tree.getTrunkBlocks());
        trunk.sort((a, b) -> Integer.compare(b.getY(), a.getY())); // Top to bottom

        Map<String, List<BlockPos>> segments = new HashMap<>();
        int segmentSize = Math.max(1, trunk.size() / segmentCount);

        for (int i = 0; i < segmentCount; i++) {
            String agentId = agentIds.get(i % agentIds.size());
            List<BlockPos> segment = new ArrayList<>();

            int start = i * segmentSize;
            int end = Math.min((i + 1) * segmentSize, trunk.size());

            for (int j = start; j < end; j++) {
                segment.add(trunk.get(j));
            }

            segments.merge(agentId, segment, (old, newSeg) -> {
                old.addAll(newSeg);
                return old;
            });
        }

        return segments;
    }
}
```

### 4. ChopTreeAction Implementation

```java
public class ChopTreeAction extends BaseAction {
    private final TreeDetector detector;
    private final FellingStrategy strategy;
    private Tree targetTree;
    private BlockPos currentTargetBlock;
    private int logsChopped = 0;
    private int saplingsCollected = 0;
    private boolean leavesDecayed = false;
    private boolean replanted = false;
    private static final int CHOP_DELAY_TICKS = 5;
    private int ticksSinceLastChop = 0;

    @Override
    protected void onStart() {
        String woodType = task.getStringParameter("wood", "any");
        int quantity = task.getIntParameter("quantity", 1);
        boolean replant = task.getBooleanParameter("replant", true);

        detector = new TreeDetector(foreman.level());
        strategy = new TopDownFellingStrategy();

        equipAxe();

        if (task.hasParameter("x") && task.hasParameter("z")) {
            // Chop specific tree
            BlockPos targetPos = new BlockPos(
                task.getIntParameter("x"),
                task.getIntParameter("y", foreman.getBlockY()),
                task.getIntParameter("z")
            );
            findTreeAt(targetPos);
        } else {
            // Find nearest tree
            findNearestTree(woodType);
        }

        if (targetTree == null) {
            result = ActionResult.failure("No suitable tree found");
            return;
        }

        MineWrightMod.LOGGER.info("[{}] Starting to chop {} at {}",
            foreman.getSteveName(),
            targetTree.getTreeType(),
            targetTree.getBasePosition());
    }

    @Override
    protected void onTick() {
        if (targetTree == null) {
            result = ActionResult.failure("Target tree lost");
            return;
        }

        ticksSinceLastChop++;

        if (ticksSinceLastChop < CHOP_DELAY_TICKS) {
            return; // Wait between chops
        }

        // Move to next block
        if (currentTargetBlock == null ||
            foreman.level().getBlockState(currentTargetBlock).isAir()) {

            currentTargetBlock = getNextBlockToChop();

            if (currentTargetBlock == null) {
                // Trunk complete
                if (!leavesDecayed) {
                    waitForLeafDecay();
                } else if (!replanted) {
                    replantSapling();
                } else {
                    result = ActionResult.success(String.format(
                        "Chopped %d logs, collected %d saplings, replanted",
                        logsChopped, saplingsCollected
                    ));
                }
                return;
            }

            // Navigate to block
            foreman.getNavigation().moveTo(
                currentTargetBlock.getX(),
                currentTargetBlock.getY(),
                currentTargetBlock.getZ(),
                1.0
            );
        }

        // Check if in range
        if (foreman.blockPosition().distSqr(currentTargetBlock) > 9) {
            return; // Still moving
        }

        // Chop the block
        chopBlock(currentTargetBlock);
        ticksSinceLastChop = 0;
    }

    private BlockPos getNextBlockToChop() {
        List<BlockPos> fellingOrder = strategy.getFellingOrder(targetTree);

        for (BlockPos pos : fellingOrder) {
            if (foreman.level().getBlockState(pos).getBlock() == targetTree.getLogType()) {
                return pos;
            }
        }

        return null; // All blocks chopped
    }

    private void chopBlock(BlockPos pos) {
        BlockState state = foreman.level().getBlockState(pos);

        if (state.getBlock() != targetTree.getLogType()) {
            return;
        }

        // Break block
        foreman.swing(InteractionHand.MAIN_HAND, true);
        foreman.level().destroyBlock(pos, true); // Drop items

        logsChopped++;

        MineWrightMod.LOGGER.debug("[{}] Chopped log at {} ({}/{})",
            foreman.getSteveName(), pos, logsChopped, targetTree.getLogCount());
    }

    private void waitForLeafDecay() {
        // Check if leaves have decayed
        boolean hasLeaves = false;

        for (BlockPos leafPos : targetTree.getLeafBlocks()) {
            BlockState state = foreman.level().getBlockState(leafPos);
            if (isLeafBlock(state)) {
                hasLeaves = true;
                break;
            }
        }

        if (!hasLeaves) {
            leavesDecayed = true;
            collectDrops();
        } else {
            // Leaves still decaying, wait
            MineWrightMod.LOGGER.debug("[{}] Waiting for leaf decay...",
                foreman.getSteveName());
        }
    }

    private void collectDrops() {
        // Scan ground for dropped items
        AABB searchArea = new AABB(
            targetTree.getBasePosition().offset(-5, -1, -5),
            targetTree.getBasePosition().offset(6, 2, 6)
        );

        List<ItemEntity> items = foreman.level().getEntitiesOfClass(
            ItemEntity.class, searchArea
        );

        for (ItemEntity item : items) {
            ItemStack stack = item.getItem();
            if (stack.getItem() instanceof Item itemObj) {
                String itemName = itemObj.toString();
                if (itemName.contains("sapling")) {
                    saplingsCollected += stack.getCount();
                    item.discard(); // Pick up
                }
            }
        }
    }

    private void replantSapling() {
        Block sapling = getSaplingForTree(targetTree.getTreeType());
        BlockPos plantPos = targetTree.getBasePosition();

        if (foreman.level().getBlockState(plantPos).isAir()) {
            foreman.level().setBlock(
                plantPos,
                sapling.defaultBlockState(),
                3
            );

            foreman.swing(InteractionHand.MAIN_HAND, true);

            MineWrightMod.LOGGER.info("[{}] Replanted {} sapling",
                foreman.getSteveName(),
                targetTree.getTreeType());
        }

        replanted = true;
    }

    private void equipAxe() {
        ItemStack axe = findBestAxe();
        if (axe != null) {
            foreman.setItemInHand(InteractionHand.MAIN_HAND, axe);
        } else {
            // Give diamond axe if inventory has none
            foreman.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(Items.DIAMOND_AXE));
        }
    }

    private ItemStack findBestAxe() {
        // Priority: Diamond > Iron > Stone > Wooden
        ItemStack[] axes = {
            new ItemStack(Items.DIAMOND_AXE),
            new ItemStack(Items.IRON_AXE),
            new ItemStack(Items.STONE_AXE),
            new ItemStack(Items.WOODEN_AXE)
        };

        // Check inventory for best axe
        // For now, just return diamond axe
        return axes[0];
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
        foreman.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    @Override
    public String getDescription() {
        if (targetTree != null) {
            return String.format("Chopping %s (%d/%d logs)",
                targetTree.getTreeType(),
                logsChopped,
                targetTree.getLogCount());
        }
        return "Chopping tree...";
    }
}
```

---

## Leaf Decay Optimization

### 1. Decay Strategies

#### Strategy A: Passive Waiting (Default)

Best for: Small to medium trees, single agent

```java
public class PassiveLeafDecayStrategy implements LeafDecayStrategy {

    @Override
    public boolean shouldWaitForDecay(Tree tree, int agentCount) {
        // Wait for decay if tree is small or we have few agents
        return tree.getLeafBlocks().size() < 200 || agentCount < 3;
    }

    @Override
    public void processDecay(Level level, Tree tree, Consumer<BlockPos> onDrop) {
        // No action - wait for natural decay
        MineWrightMod.LOGGER.debug("Waiting for natural leaf decay on {} tree",
            tree.getTreeType());
    }
}
```

#### Strategy B: Active Clearing (Large Trees)

Best for: Jungle trees, dark oak, multi-agent scenarios

```java
public class ActiveLeafClearingStrategy implements LeafDecayStrategy {
    private static final int CLEARING_RADIUS = 8;
    private static final int BLOCKS_PER_TICK = 4;

    @Override
    public boolean shouldWaitForDecay(Tree tree, int agentCount) {
        // Actively clear if tree is large and we have multiple agents
        return tree.getLeafBlocks().size() >= 200 && agentCount >= 2;
    }

    @Override
    public void processDecay(Level level, Tree tree, Consumer<BlockPos> onDrop) {
        Queue<BlockPos> toClear = new LinkedList<>(tree.getLeafBlocks());
        int cleared = 0;

        while (!toClear.isEmpty() && cleared < BLOCKS_PER_TICK) {
            BlockPos pos = toClear.poll();
            BlockState state = level.getBlockState(pos);

            if (isLeafBlock(state)) {
                // Break leaf manually to speed up decay
                level.destroyBlock(pos, true); // Drop items
                cleared++;

                onDrop.accept(pos);
            }
        }
    }
}
```

### 2. Hybrid Approach

```java
public class HybridLeafDecayStrategy implements LeafDecayStrategy {
    private final PassiveLeafDecayStrategy passive;
    private final ActiveLeafClearingStrategy active;

    @Override
    public void processDecay(Level level, Tree tree,
                             Consumer<BlockPos> onDrop,
                             int agentCount) {

        // Estimate decay time based on leaf count
        int leafCount = tree.getLeafBlocks().size();
        int estimatedDecayTicks = leafCount / 5; // Rough estimate

        // Decide strategy
        if (shouldActivelyClear(tree, agentCount)) {
            // Start with active clearing
            active.processDecay(level, tree, onDrop);
        } else {
            // Wait and check periodically
            checkForDecayCompletion(level, tree);
        }
    }

    private boolean shouldActivelyClear(Tree tree, int agentCount) {
        // Clear actively if:
        // - More than 3 agents available
        // - Tree has > 300 leaf blocks
        // - Efficiency gain > 30%
        return agentCount >= 3 && tree.getLeafBlocks().size() > 300;
    }

    private void checkForDecayCompletion(Level level, Tree tree) {
        int remainingLeaves = 0;

        for (BlockPos leafPos : tree.getLeafBlocks()) {
            if (isLeafBlock(level.getBlockState(leafPos))) {
                remainingLeaves++;
            }
        }

        if (remainingLeaves < tree.getLeafBlocks().size() * 0.1) {
            // 90% decayed, consider complete
            MineWrightMod.LOGGER.info("Leaf decay 90% complete, {} leaves remaining",
                remainingLeaves);
        }
    }
}
```

---

## Replanting Logic

### 1. Sapling Selection

```java
public class SaplingSelector {

    public static Block getSaplingForTree(Tree.TreeType treeType) {
        return switch (treeType) {
            case OAK -> Blocks.OAK_SAPLING;
            case BIRCH -> Blocks.BIRCH_SAPLING;
            case SPRUCE -> Blocks.SPRUCE_SAPLING;
            case JUNGLE -> Blocks.JUNGLE_SAPLING;
            case ACACIA -> Blocks.ACACIA_SAPLING;
            case DARK_OAK -> Blocks.DARK_OAK_SAPLING;
            case MANGROVE -> Blocks.MANGROVE_PROPAGULE;
            case CHERRY -> Blocks.CHERRY_SAPLING;
            case CRIMSON -> Blocks.CRIMSON_FUNGUS;
            case WARPED -> Blocks.WARPED_FUNGUS;
        };
    }

    public static boolean canPlantAt(Level level, BlockPos pos, Block sapling) {
        BlockState below = level.getBlockState(pos.below());

        // Check if soil is suitable
        if (!isSuitableSoil(below)) {
            return false;
        }

        // Check space above
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        // Dark oak and jungle need 2x2 space
        if (sapling == Blocks.DARK_OAK_SAPLING ||
            sapling == Blocks.JUNGLE_SAPLING) {
            return canPlant2x2(level, pos);
        }

        return true;
    }

    private static boolean isSuitableSoil(BlockState soil) {
        return soil.is(Blocks.DIRT) ||
               soil.is(Blocks.GRASS_BLOCK) ||
               soil.is(Blocks.PODZOL) ||
               soil.is(Blocks.COARSE_DIRT) ||
               soil.is(Blocks.MYCELIUM) ||
               soil.is(Blocks.FARMLAND);
    }

    private static boolean canPlant2x2(Level level, BlockPos basePos) {
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                BlockPos checkPos = basePos.offset(x, 0, z);
                if (!level.getBlockState(checkPos).isAir()) {
                    return false;
                }

                BlockState below = level.getBlockState(checkPos.below());
                if (!isSuitableSoil(below)) {
                    return false;
                }
            }
        }
        return true;
    }
}
```

### 2. Bone Meal Application

```java
public class TreeGrowthAccelerator {

    public static boolean applyBoneMeal(Level level, BlockPos saplingPos) {
        BlockState saplingState = level.getBlockState(saplingPos);

        if (!(saplingState.getBlock() instanceof SaplingBlock)) {
            return false;
        }

        // Simulate bone meal application
        SaplingBlock sapling = (SaplingBlock) saplingState.getBlock();

        // In vanilla, bone meal has a chance to grow the tree
        // For automation, we can force growth with a custom approach

        return attemptGrowth(level, saplingPos, saplingState);
    }

    private static boolean attemptGrowth(Level level, BlockPos pos, BlockState state) {
        // Try to grow the tree using vanilla mechanics
        // This is a simplified version - actual implementation would use forge events

        if (level instanceof ServerLevel serverLevel) {
            // Force grow with bonemeal
            return state.getBlock().bonemealTrigger(
                serverLevel,
                serverLevel.getRandom(),
                pos,
                state
            );
        }

        return false;
    }

    public static int getBoneMealNeeded(Tree.TreeType type) {
        // Approximate bone meal needed for instant growth
        return switch (type) {
            case OAK, BIRCH -> 2;
            case SPRUCE, ACACIA -> 3;
            case JUNGLE, DARK_OAK -> 5;
            case CHERRY -> 2;
            case MANGROVE -> 4;
            case CRIMSON, WARPED -> 3;
        };
    }
}
```

### 3. Forest Replanting Pattern

```java
public class PlantForestAction extends BaseAction {
    private final Tree.TreeType treeType;
    private final int treeCount;
    private final int spacing;
    private final boolean useBonemeal;
    private int treesPlanted = 0;
    private BlockPos currentPlantPos;

    public PlantForestAction(ForemanEntity foreman, Task task) {
        super(foreman, task);

        String typeStr = task.getStringParameter("type", "oak");
        this.treeType = parseTreeType(typeStr);
        this.treeCount = task.getIntParameter("count", 10);
        this.spacing = task.getIntParameter("spacing", 5);
        this.useBonemeal = task.getBooleanParameter("bonemeal", true);
    }

    @Override
    protected void onStart() {
        Block sapling = SaplingSelector.getSaplingForTree(treeType);
        foreman.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(sapling));

        currentPlantPos = foreman.blockPosition();
    }

    @Override
    protected void onTick() {
        if (treesPlanted >= treeCount) {
            result = ActionResult.success("Planted " + treesPlanted + " " + treeType + " trees");
            return;
        }

        // Calculate next planting position (grid pattern)
        BlockPos plantPos = calculateNextPlantPos();

        if (!foreman.blockPosition().closerThan(plantPos, 3)) {
            // Navigate to position
            foreman.getNavigation().moveTo(plantPos.getX(), plantPos.getY(), plantPos.getZ(), 1.0);
            return;
        }

        // Plant sapling
        Block sapling = SaplingSelector.getSaplingForTree(treeType);

        if (SaplingSelector.canPlantAt(foreman.level(), plantPos, sapling)) {
            foreman.level().setBlock(plantPos, sapling.defaultBlockState(), 3);
            foreman.swing(InteractionHand.MAIN_HAND, true);

            treesPlanted++;

            // Apply bonemeal if requested
            if (useBonemeal) {
                int bonemealNeeded = TreeGrowthAccelerator.getBoneMealNeeded(treeType);
                for (int i = 0; i < bonemealNeeded; i++) {
                    TreeGrowthAccelerator.applyBoneMeal(foreman.level(), plantPos);
                }
            }

            MineWrightMod.LOGGER.info("[{}] Planted {} sapling at {} ({}/{})",
                foreman.getSteveName(), treeType, plantPos, treesPlanted, treeCount);
        } else {
            MineWrightMod.LOGGER.warn("[{}] Cannot plant at {}, skipping",
                foreman.getSteveName(), plantPos);
        }

        // Move to next position
        advanceToNextPosition();
    }

    private BlockPos calculateNextPlantPos() {
        // Simple grid pattern
        int row = treesPlanted / 5;
        int col = treesPlanted % 5;

        int offsetX = col * spacing;
        int offsetZ = row * spacing;

        return currentPlantPos.offset(offsetX, 0, offsetZ);
    }

    private void advanceToNextPosition() {
        // Move to next grid position
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return String.format("Planting forest: %d/%d %s trees",
            treesPlanted, treeCount, treeType);
    }
}
```

---

## Multi-Tree Coordination

### 1. Forest Manager

```java
public class ForestManager {
    private static final Map<String, Forest> activeForests = new ConcurrentHashMap<>();

    public static Forest registerForest(String forestId, BlockPos center, int radius) {
        Forest forest = new Forest(forestId, center, radius);
        activeForests.put(forestId, forest);

        MineWrightMod.LOGGER.info("Registered forest '{}' at {} with radius {}",
            forestId, center, radius);

        return forest;
    }

    public static Tree assignNextTree(String agentId, String forestId,
                                      Tree.TreeType preferredType) {
        Forest forest = activeForests.get(forestId);
        if (forest == null) {
            return null;
        }

        return forest.assignTree(agentId, preferredType);
    }

    public static void completeTree(String agentId, String forestId, String treeId) {
        Forest forest = activeForests.get(forestId);
        if (forest != null) {
            forest.completeTree(agentId, treeId);
        }
    }
}
```

### 2. Forest Data Structure

```java
public class Forest {
    private final String forestId;
    private final BlockPos center;
    private final int radius;
    private final List<Tree> trees;
    private final Map<String, Tree> agentToTreeMap;
    private final Map<String, Set<Tree>> treeTypeIndex;
    private final AtomicInteger treesCompleted = new AtomicInteger(0);

    public Forest(String forestId, BlockPos center, int radius) {
        this.forestId = forestId;
        this.center = center;
        this.radius = radius;
        this.trees = new ArrayList<>();
        this.agentToTreeMap = new ConcurrentHashMap<>();
        this.treeTypeIndex = new ConcurrentHashMap<>();
    }

    public void addTree(Tree tree) {
        trees.add(tree);
        treeTypeIndex.computeIfAbsent(tree.getTreeType().name(), k -> ConcurrentHashMap.newKeySet())
                     .add(tree);
    }

    public Tree assignTree(String agentId, Tree.TreeType preferredType) {
        // Check if agent already has a tree
        if (agentToTreeMap.containsKey(agentId)) {
            return agentToTreeMap.get(agentId);
        }

        // Find unassigned tree of preferred type
        Set<Tree> typeTrees = treeTypeIndex.get(preferredType.name());
        if (typeTrees != null) {
            for (Tree tree : typeTrees) {
                if (!tree.isAssigned()) {
                    tree.assignTo(agentId);
                    agentToTreeMap.put(agentId, tree);
                    return tree;
                }
            }
        }

        // No preferred type available, assign any tree
        for (Tree tree : trees) {
            if (!tree.isAssigned()) {
                tree.assignTo(agentId);
                agentToTreeMap.put(agentId, tree);
                return tree;
            }
        }

        return null; // No trees available
    }

    public void completeTree(String agentId, String treeId) {
        Tree tree = agentToTreeMap.get(agentId);
        if (tree != null && tree.getTreeId().equals(treeId)) {
            tree.markComplete();
            agentToTreeMap.remove(agentId);
            treesCompleted.incrementAndGet();
        }
    }

    public int getProgress() {
        return (treesCompleted.get() * 100) / trees.size();
    }

    public boolean isComplete() {
        return treesCompleted.get() >= trees.size();
    }
}
```

### 3. Spatial Partitioning for Multiple Agents

```java
public class ForestPartitioner {

    public static Map<String, Quadrant> partitionForest(Forest forest, List<String> agentIds) {
        if (agentIds.size() == 1) {
            // Single agent gets entire forest
            Map<String, Quadrant> partitions = new HashMap<>();
            partitions.put(agentIds.get(0), new Quadrant(forest.getCenter(), forest.getRadius()));
            return partitions;
        }

        // Partition into quadrants
        int quadrantsNeeded = Math.min(4, agentIds.size());
        Map<String, Quadrant> partitions = new HashMap<>();

        for (int i = 0; i < agentIds.size(); i++) {
            String agentId = agentIds.get(i);
            int quadrantIndex = i % quadrantsNeeded;
            Quadrant quadrant = createQuadrant(forest.getCenter(), forest.getRadius(), quadrantIndex);
            partitions.put(agentId, quadrant);
        }

        return partitions;
    }

    private static Quadrant createQuadrant(BlockPos center, int radius, int index) {
        // Divide into 2x2 grid
        int halfRadius = radius / 2;

        return switch (index) {
            case 0 -> new Quadrant(center.offset(-halfRadius, 0, -halfRadius), halfRadius); // NW
            case 1 -> new Quadrant(center.offset(halfRadius, 0, -halfRadius), halfRadius);  // NE
            case 2 -> new Quadrant(center.offset(-halfRadius, 0, halfRadius), halfRadius);  // SW
            case 3 -> new Quadrant(center.offset(halfRadius, 0, halfRadius), halfRadius);   // SE
            default -> new Quadrant(center, radius);
        };
    }

    public static class Quadrant {
        private final BlockPos center;
        private final int radius;

        public Quadrant(BlockPos center, int radius) {
            this.center = center;
            this.radius = radius;
        }

        public boolean contains(BlockPos pos) {
            return pos.distSqr(center) <= radius * radius;
        }

        public BlockPos getCenter() {
            return center;
        }

        public int getRadius() {
            return radius;
        }
    }
}
```

### 4. DeforestAreaAction (Multi-Agent Coordination)

```java
public class DeforestAreaAction extends BaseAction {
    private Forest forest;
    private Tree currentTree;
    private int treesCompleted = 0;
    private final boolean replant;
    private final String forestId;

    public DeforestAreaAction(ForemanEntity foreman, Task task) {
        super(foreman, task);

        this.forestId = "forest_" + System.currentTimeMillis();
        this.replant = task.getBooleanParameter("replant", true);
    }

    @Override
    protected void onStart() {
        int radius = task.getIntParameter("radius", 32);
        BlockPos center = task.hasParameter("x") ?
            new BlockPos(task.getIntParameter("x"), 0, task.getIntParameter("z")) :
            foreman.blockPosition();

        // Register forest
        forest = ForestManager.registerForest(forestId, center, radius);

        // Scan for trees
        scanForest(center, radius);

        // Get assigned tree
        currentTree = ForestManager.assignNextTree(
            foreman.getSteveName(),
            forestId,
            Tree.TreeType.OAK // Accept any type
        );

        if (currentTree == null) {
            result = ActionResult.failure("No trees found in area");
            return;
        }

        equipAxe();

        MineWrightMod.LOGGER.info("[{}] Starting deforestation of {} ({} trees)",
            foreman.getSteveName(), forestId, forest.getTreeCount());
    }

    @Override
    protected void onTick() {
        if (currentTree == null) {
            // Get next tree
            currentTree = ForestManager.assignNextTree(
                foreman.getSteveName(),
                forestId,
                Tree.TreeType.OAK
            );

            if (currentTree == null) {
                result = ActionResult.success(String.format(
                    "Deforestation complete: %d trees processed",
                    treesCompleted
                ));
                return;
            }
        }

        // Chop current tree
        chopCurrentTree();

        if (currentTree.isComplete()) {
            ForestManager.completeTree(foreman.getSteveName(), forestId, currentTree.getTreeId());
            treesCompleted++;
            currentTree = null;

            // Report progress
            foreman.notifyMilestone(String.format("Completed %d trees", treesCompleted));
        }
    }

    private void scanForest(BlockPos center, int radius) {
        TreeDetector detector = new TreeDetector(foreman.level());

        for (int x = -radius; x <= radius; x += 4) {
            for (int z = -radius; z <= radius; z += 4) {
                BlockPos scanPos = center.offset(x, 0, z);

                detector.findTreeByColumnScan(foreman.level(), scanPos)
                    .ifPresent(tree -> {
                        if (tree.isWithinRange(center, radius)) {
                            forest.addTree(tree);
                        }
                    });
            }
        }
    }

    private void chopCurrentTree() {
        // Simplified chopping logic
        BlockPos trunkBase = currentTree.getBasePosition().above();

        if (!foreman.blockPosition().closerThan(trunkBase, 5)) {
            foreman.getNavigation().moveTo(trunkBase.getX(), trunkBase.getY(), trunkBase.getZ(), 1.0);
            return;
        }

        BlockState state = foreman.level().getBlockState(trunkBase);
        if (state.getBlock() == currentTree.getLogType()) {
            foreman.swing(InteractionHand.MAIN_HAND, true);
            foreman.level().destroyBlock(trunkBase, true);

            // Check if tree is complete
            boolean hasLogs = false;
            for (BlockPos pos : currentTree.getTrunkBlocks()) {
                if (foreman.level().getBlockState(pos).getBlock() == currentTree.getLogType()) {
                    hasLogs = true;
                    break;
                }
            }

            if (!hasLogs) {
                currentTree.markComplete();

                if (replant) {
                    replantTree();
                }
            }
        }
    }

    private void replantTree() {
        Block sapling = SaplingSelector.getSaplingForTree(currentTree.getTreeType());
        BlockPos plantPos = currentTree.getBasePosition();

        if (SaplingSelector.canPlantAt(foreman.level(), plantPos, sapling)) {
            foreman.level().setBlock(plantPos, sapling.defaultBlockState(), 3);
            foreman.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
        if (currentTree != null) {
            ForestManager.completeTree(foreman.getSteveName(), forestId, currentTree.getTreeId());
        }
    }

    @Override
    public String getDescription() {
        return String.format("Deforesting: %d/%d trees", treesCompleted, forest.getTreeCount());
    }
}
```

---

## Implementation Examples

### Example 1: Basic Tree Chopping

```java
// User command via LLM
// "Chop down 5 oak trees near me"

Task task = new Task("chop", Map.of(
    "wood", "oak",
    "quantity", 5,
    "replant", true
));

ChopTreeAction action = new ChopTreeAction(foreman, task);
actionExecutor.executeAction(action);
```

### Example 2: Multi-Agent Forest Clearing

```java
// Foreman coordinates multiple workers
// "Clear the forest north of base, replant with birch"

// Foreman creates plan
String forestId = "north_forest_" + System.currentTimeMillis();

// Assign workers to quadrants
List<String> workers = Arrays.asList("worker1", "worker2", "worker3", "worker4");
Map<String, Quadrant> partitions = ForestPartitioner.partitionForest(forest, workers);

// Send tasks to each worker
for (Map.Entry<String, Quadrant> entry : partitions.entrySet()) {
    String workerId = entry.getKey();
    Quadrant quadrant = entry.getValue();

    Task workerTask = new Task("deforest", Map.of(
        "forest_id", forestId,
        "quadrant_center", quadrant.getCenter(),
        "quadrant_radius", quadrant.getRadius(),
        "replant", true,
        "replant_type", "birch"
    ));

    AgentMessage message = AgentMessage.taskAssignment(
        "foreman", "foreman",
        workerId,
        UUID.randomUUID().toString(),
        workerTask.getParameters()
    );

    orchestrator.getCommunicationBus().publish(message);
}
```

### Example 3: Automated Tree Farm

```java
// Continuous tree farming operation
public class TreeFarmAction extends BaseAction {
    private enum Phase { PLANT, GROW, HARVEST }
    private Phase currentPhase = Phase.PLANT;
    private int cycleCount = 0;
    private final int maxCycles;

    public TreeFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.maxCycles = task.getIntParameter("cycles", 10);
    }

    @Override
    protected void onTick() {
        switch (currentPhase) {
            case PLANT -> plantSaplings();
            case GROW -> waitForGrowth();
            case HARVEST -> harvestTrees();
        }
    }

    private void plantSaplings() {
        // Plant saplings in grid pattern
        // Apply bonemeal if available
        currentPhase = Phase.GROW;
    }

    private void waitForGrowth() {
        // Check if trees are fully grown
        // Use bonemeal to accelerate
        boolean allGrown = checkTreeGrowth();
        if (allGrown) {
            currentPhase = Phase.HARVEST;
        }
    }

    private void harvestTrees() {
        // Chop all trees
        // Collect saplings
        // Replant
        cycleCount++;

        if (cycleCount >= maxCycles) {
            result = ActionResult.success("Completed " + cycleCount + " farm cycles");
        } else {
            currentPhase = Phase.PLANT;
        }
    }
}
```

---

## Implementation Roadmap

### Phase 1: Core Tree Detection (Week 1)

- [ ] Implement `TreeDetector` with BFS algorithm
- [ ] Implement `TreeAnalyzer` for tree structure analysis
- [ ] Create `Tree` data structure
- [ ] Add tree type definitions (all vanilla trees)
- [ ] Unit tests for detection accuracy

### Phase 2: Basic Chopping (Week 2)

- [ ] Implement `ChopTreeAction`
- [ ] Top-down felling strategy
- [ ] Tool management (axe equip/durability)
- [ ] Single-tree chopping workflow
- [ ] Integration with action system

### Phase 3: Leaf Handling (Week 3)

- [ ] Passive leaf decay waiting
- [ ] Drop collection system
- [ ] Active leaf clearing for large trees
- [ ] Hybrid decay strategy
- [ ] Sapling collection tracking

### Phase 4: Replanting (Week 4)

- [ ] `SaplingSelector` implementation
- [ ] Soil suitability checking
- [ ] 2x2 sapling placement (dark oak, jungle)
- [ ] Bone meal application
- [ ] Growth acceleration

### Phase 5: Multi-Agent Coordination (Week 5-6)

- [ ] `ForestManager` implementation
- [ ] `Forest` data structure
- [ ] Spatial partitioning system
- [ ] `DeforestAreaAction` with multi-agent support
- [ ] Agent communication for tree assignment

### Phase 6: Advanced Features (Week 7-8)

- [ ] Automated tree farm action
- [ ] Efficient felling patterns (spiral, segmented)
- [ ] Forest regeneration patterns
- [ ] Performance optimization
- [ ] Analytics and reporting

### Phase 7: Testing & Polish (Week 9-10)

- [ ] Integration testing
- [ ] Performance profiling
- [ ] Edge case handling
- [ ] Documentation
- [ ] User acceptance testing

---

## Configuration

### config/steve-common.toml

```toml
[woodcutting]
# Maximum search radius for tree detection
max_search_radius = 32

# Delay between chopping blocks (ticks)
chop_delay = 5

# Enable automatic replanting
auto_replant = true

# Leaf decay strategy: "passive", "active", "hybrid"
leaf_decay_strategy = "hybrid"

# Threshold for active leaf clearing (leaf count)
active_clearing_threshold = 200

# Minimum agent count for multi-agent optimization
multi_agent_threshold = 3

# Default spacing for forest replanting
replant_spacing = 5

# Enable bone meal for growth acceleration
use_bonemeal = true
```

---

## Performance Considerations

1. **Detection Optimization**
   - Use column scanning in sparse forests (faster)
   - Use BFS in dense forests (more accurate)
   - Cache detected trees for reuse

2. **Leaf Decay**
   - Passive decay: 0 CPU cost, slow (~1-5 minutes)
   - Active clearing: Higher CPU, fast (~10-30 seconds)
   - Use hybrid for best balance

3. **Multi-Agent Coordination**
   - Spatial partitioning prevents conflicts
   - Dynamic rebalancing when agents finish early
   - Communication overhead ~100ms per assignment

4. **Memory Usage**
   - Forest with 1000 trees: ~500 KB
   - Single tree with 300 leaves: ~50 KB
   - Consider chunk loading for large forests

---

## Future Enhancements

1. **Tree Species Detection**
   - Automatic sapling selection based on biome
   - Rare tree detection (chorus, mushroom)

2. **Sustainable Forestry**
   - Selective logging (leave seed trees)
   - Age-based harvesting
   - Biodiversity metrics

3. **Advanced Patterns**
   - Hedge maze creation
   - Orchard layouts
   - Bonsai tree farming

4. **Integration with Other Systems**
   - Automatic chest sorting for wood
   - Crafting integration (sticks, planks)
   - Building integration (wooden structures)

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Status:** Ready for Implementation
