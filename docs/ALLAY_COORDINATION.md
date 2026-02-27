# Allay Coordination for MineWright Minecraft Mod

## Table of Contents
1. [Overview](#overview)
2. [Allay Behavior Mechanics](#allay-behavior-mechanics)
3. [Collection Optimization](#collection-optimization)
4. [Multi-Allay Systems](#multi-allay-systems)
5. [Code Examples for Integration](#code-examples-for-integration)
6. [Implementation Roadmap](#implementation-roadmap)

---

## Overview

Allays are small flying passive mobs that collect dropped items matching the item they are given. They follow players or note blocks, making them ideal for automated item collection and sorting in Minecraft 1.20.1.

### Key Allay Characteristics

| Attribute | Value | Description |
|-----------|-------|-------------|
| Health | 20 HP (10 hearts) | Same as player |
| Hitbox | 0.6 x 0.35 blocks | Small, can fit in tight spaces |
| Speed | 0.4 | Moderate flying speed |
| Follow Range | 64 blocks | Cubic area around player |
| Collection Range | 32 blocks | Seeks items within this radius |
| Pickup Range | ~1.3 blocks | Must be very close to item |
| Delivery Range | ~3 blocks | Drops items near target |
| Inventory | 1 slot + held item | Can carry a stack (64 items) |
| Cooldown | 3 seconds | Between collection trips |
| Note Block Duration | 30 seconds | Stays at note block |
| Note Block Range | 16 blocks | Detection range |

### Vanilla Allay Behavior

```
┌─────────────────────────────────────────────────────────────────┐
│                    Vanilla Allay Behavior                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. IDLE STATE                                                  │
│     ├── Wanders randomly                                        │
│     ├── No item held                                           │
│     └── Awaits player interaction                               │
│                                                                 │
│  2. TAMED STATE (Given Item)                                   │
│     ├── Follows player within 64 blocks                        │
│     ├── Seeks dropped items (32 blocks)                        │
│     ├── Collects matching items                                │
│     ├── Returns to player and drops items                     │
│     └── 3-second cooldown between trips                        │
│                                                                 │
│  3. NOTE BLOCK STATE                                           │
│     ├── Triggered by note block sound (16 blocks)              │
│     ├── Switches target from player to note block              │
│     ├── Duration: 30 seconds                                   │
│     ├── Collects items for note block location                 │
│     ├── Only responds to same note block (reset timer)         │
│     └── Returns to player after 30 seconds                     │
│                                                                 │
│  4. DUPLICATION (Requires Amethyst Shard + Jukebox)            │
│     ├── Must be dancing (jukebox nearby)                       │
│     ├── Given amethyst shard                                   │
│     ├── Splits into two allays                                 │
│     └── 5-minute cooldown per allay                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Allay Behavior Mechanics

### Allay Detection and Tracking

Allays can be detected and managed through Minecraft's entity system. The Forge `EntityType.ALLAY` provides access to allay entities.

```java
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;

/**
 * Detects and tracks allays within a specified range.
 */
public class AllayDetector {
    private final ServerLevel level;
    private final BlockPos center;
    private final int searchRadius;

    public AllayDetector(ServerLevel level, BlockPos center, int searchRadius) {
        this.level = level;
        this.center = center;
        this.searchRadius = searchRadius;
    }

    /**
     * Finds all allays within the search radius.
     * @return List of detected allays
     */
    public List<Allay> findAllays() {
        AABB searchBox = new AABB(center).inflate(searchRadius);
        return level.getEntitiesOfClass(Allay.class, searchBox);
    }

    /**
     * Finds allays with a specific held item.
     * @param item The item to match
     * @return List of allays holding that item
     */
    public List<Allay> findAllaysWithItem(Item item) {
        return findAllays().stream()
            .filter(allay -> {
                ItemStack heldItem = allay.getMainHandItem();
                return !heldItem.isEmpty() && heldItem.is(item);
            })
            .collect(Collectors.toList());
    }

    /**
     * Finds allays targeting a specific note block.
     * @param noteBlockPos Position of the note block
     * @return List of allays targeting that note block
     */
    public List<Allay> findAllaysAtNoteBlock(BlockPos noteBlockPos) {
        return findAllays().stream()
            .filter(allay -> isTargetingNoteBlock(allay, noteBlockPos))
            .collect(Collectors.toList());
    }

    private boolean isTargetingNoteBlock(Allay allay, BlockPos noteBlockPos) {
        // Allays targeting note blocks have specific behavior patterns
        // Check if allay is within delivery range of note block
        BlockPos allayPos = allay.blockPosition();
        return allayPos.distSqr(noteBlockPos) <= 9; // 3 blocks squared
    }
}
```

### Allay Taming and Management

```java
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

/**
 * Manages allay taming and configuration for MineWright.
 */
public class AllayManager {
    private final Map<UUID, AllayData> trackedAllays;
    private final ForemanEntity foreman;

    public AllayManager(ForemanEntity foreman) {
        this.foreman = foreman;
        this.trackedAllays = new ConcurrentHashMap<>();
    }

    /**
     * Tames an allay by giving it an item.
     * @param allay The allay to tame
     * @param item The item to give (defines collection target)
     * @return true if successfully tamed
     */
    public boolean tameAllay(Allay allay, ItemStack item) {
        if (allay == null || item.isEmpty()) {
            return false;
        }

        // Give item to allay (tames it)
        ItemStack itemCopy = item.copy();
        itemCopy.setCount(1);
        allay.setItemSlot(EquipmentSlot.MAINHAND, itemCopy);

        // Track the allay
        AllayData data = new AllayData(
            allay.getUUID(),
            allay.blockPosition(),
            item.getItem(),
            System.currentTimeMillis()
        );
        trackedAllays.put(allay.getUUID(), data);

        foreman.sendChatMessage("Tamed allay to collect " + item.getItem().getDescription());
        return true;
    }

    /**
     * Assigns an allay to a note block for item delivery.
     * @param allay The allay to assign
     * @param noteBlockPos Position of the note block
     */
    public void assignToNoteBlock(Allay allay, BlockPos noteBlockPos) {
        // Play note block to attract allay
        Level level = foreman.level();
        BlockState state = level.getBlockState(noteBlockPos);

        if (state.getBlock() instanceof NoteBlock) {
            // Trigger note block sound
            level.playSound(null, noteBlockPos, SoundEvents.NOTE_BLOCK_HARP.value(),
                SoundSource.BLOCKS, 1.0F, 1.0F);

            // Update tracking
            AllayData data = trackedAllays.get(allay.getUUID());
            if (data != null) {
                data.setNoteBlockTarget(noteBlockPos);
                data.setAssignmentTime(System.currentTimeMillis());
            }

            foreman.sendChatMessage("Assigned allay to note block at " + noteBlockPos);
        }
    }

    /**
     * Reclaims an allay from a note block.
     * @param allay The allay to reclaim
     */
    public void reclaimFromNoteBlock(Allay allay) {
        // Take back the held item to stop note block targeting
        ItemStack heldItem = allay.getMainHandItem();

        // Create a fake player interaction to take item
        allay.interact(foreman, InteractionHand.MAIN_HAND);

        // Update tracking
        AllayData data = trackedAllays.get(allay.getUUID());
        if (data != null) {
            data.clearNoteBlockTarget();
        }

        foreman.sendChatMessage("Reclaimed allay from note block");
    }

    /**
     * Duplicates an allay (requires amethyst shard and dancing).
     * @param allay The allay to duplicate
     * @return The new allay, or null if failed
     */
    public Allay duplicateAllay(Allay allay) {
        // Check if allay is dancing (near jukebox)
        if (!isAllayDancing(allay)) {
            foreman.sendChatMessage("Allay must be dancing to duplicate!");
            return null;
        }

        // Check duplication cooldown
        AllayData data = trackedAllays.get(allay.getUUID());
        if (data != null && data.isOnDuplicationCooldown()) {
            foreman.sendChatMessage("Allay is on duplication cooldown");
            return null;
        }

        // Give amethyst shard to trigger duplication
        ItemStack amethyst = new ItemStack(Items.AMETHYST_SHARD);
        if (!foreman.getInventory().contains(amethyst)) {
            foreman.sendChatMessage("Need an amethyst shard to duplicate allay!");
            return null;
        }

        // Perform duplication (vanilla mechanic)
        Allay newAllay = EntityType.ALLAY.create(foreman.level());
        if (newAllay != null) {
            newAllay.moveTo(allay.getX(), allay.getY(), allay.getZ());
            newAllay.setItemSlot(EquipmentSlot.MAINHAND, allay.getMainHandItem().copy());
            foreman.level().addFreshEntity(newAllay);

            // Set cooldowns
            if (data != null) {
                data.setDuplicationCooldown();
            }

            foreman.sendChatMessage("Successfully duplicated allay!");
            return newAllay;
        }

        return null;
    }

    private boolean isAllayDancing(Allay allay) {
        // Check if allay is near a playing jukebox
        BlockPos pos = allay.blockPosition();
        return foreman.level().getBlockEntities(pos, 5).stream()
            .filter(be -> be.getType() == BlockEntityType.JUKEBOX)
            .anyMatch(jb -> {
                JukeboxBlockEntity jukebox = (JukeboxBlockEntity) jb;
                return jukebox.isRecordPlaying();
            });
    }

    /**
     * Data class for tracking allay state.
     */
    public static class AllayData {
        private final UUID uuid;
        private BlockPos lastKnownPosition;
        private final Item collectionTarget;
        private final long tameTime;
        private BlockPos noteBlockTarget;
        private long noteBlockAssignmentTime;
        private long duplicationCooldownEnd;

        public AllayData(UUID uuid, BlockPos pos, Item target, long tameTime) {
            this.uuid = uuid;
            this.lastKnownPosition = pos;
            this.collectionTarget = target;
            this.tameTime = tameTime;
        }

        public boolean isOnDuplicationCooldown() {
            return System.currentTimeMillis() < duplicationCooldownEnd;
        }

        public void setDuplicationCooldown() {
            this.duplicationCooldownEnd = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes
        }

        public boolean isNoteBlockAssignmentValid() {
            if (noteBlockTarget == null) return false;
            long elapsed = System.currentTimeMillis() - noteBlockAssignmentTime;
            return elapsed < 30000; // 30 seconds
        }

        // Getters and setters
        public void setNoteBlockTarget(BlockPos pos) {
            this.noteBlockTarget = pos;
            this.noteBlockAssignmentTime = System.currentTimeMillis();
        }

        public void clearNoteBlockTarget() {
            this.noteBlockTarget = null;
            this.noteBlockAssignmentTime = 0;
        }
    }
}
```

---

## Collection Optimization

### Allay Swarm Optimization

Allays work best in coordinated groups. This section describes optimization strategies for multiple allays.

```java
import java.util.*;

/**
 * Optimizes allay collection behavior through strategic positioning.
 */
public class AllaySwarmOptimizer {
    private final List<Allay> swarm;
    private final BlockPos collectionCenter;
    private final int swarmRadius;

    /**
     * Optimal allay positioning for maximum collection efficiency.
     *
     * Layout pattern (top-down view):
     *
     *     A   A   A       (outer ring: range expansion)
     *   A   C   C   A     (middle ring: backup collectors)
     *     C   N   C       (center: delivery zone + note block)
     *   A   C   C   A
     *     A   A   A
     *
     * Legend:
     *   A = Allay position
     *   C = Chest (delivery target)
     *   N = Note Block (coordination point)
     */
    private static final int[][] OPTIMAL_POSITIONS = {
        {-3, 0}, {3, 0}, {0, -3}, {0, 3},     // Cardinal positions
        {-2, -2}, {2, -2}, {-2, 2}, {2, 2},  // Diagonal positions
        {-1, 0}, {1, 0}, {0, -1}, {0, 1}      // Inner ring
    };

    public AllaySwarmOptimizer(BlockPos center, int swarmRadius) {
        this.collectionCenter = center;
        this.swarmRadius = swarmRadius;
        this.swarm = new ArrayList<>();
    }

    /**
     * Assigns optimal positions to allays in the swarm.
     */
    public void assignOptimalPositions() {
        int allayCount = swarm.size();
        int positionsNeeded = Math.min(allayCount, OPTIMAL_POSITIONS.length);

        for (int i = 0; i < positionsNeeded; i++) {
            Allay allay = swarm.get(i);
            int[] offset = OPTIMAL_POSITIONS[i];
            BlockPos targetPos = collectionCenter.offset(offset[0], 0, offset[1]);

            // Guide allay to position
            guideAllayToPosition(allay, targetPos);
        }
    }

    /**
     * Balances collection load among allays based on item distribution.
     * @param itemDistribution Map of item types to quantities in area
     */
    public void balanceCollectionLoad(Map<Item, Integer> itemDistribution) {
        // Sort items by quantity (descending)
        List<Map.Entry<Item, Integer>> sortedItems = itemDistribution.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .toList();

        // Assign most common items to more allays
        int allayIndex = 0;
        for (Map.Entry<Item, Integer> entry : sortedItems) {
            Item item = entry.getKey();
            int quantity = entry.getValue();

            // Calculate allays needed for this item
            int allaysNeeded = Math.max(1, quantity / 64); // One allay per stack
            allaysNeeded = Math.min(allaysNeeded, swarm.size() - allayIndex);

            // Assign allays to this item
            for (int i = 0; i < allaysNeeded && allayIndex < swarm.size(); i++) {
                Allay allay = swarm.get(allayIndex);
                reassignAllayTarget(allay, item);
                allayIndex++;
            }
        }
    }

    /**
     * Creates a collection zone with hoppers and note blocks.
     * @return Map of note block positions to their assigned items
     */
    public Map<BlockPos, Item> createCollectionZone() {
        Map<BlockPos, Item> noteBlockAssignments = new HashMap<>();

        // Create note blocks at strategic positions
        BlockPos[] noteBlockPositions = {
            collectionCenter.offset(0, 0, 0),   // Center
            collectionCenter.offset(-2, 0, 0), // West
            collectionCenter.offset(2, 0, 0),  // East
            collectionCenter.offset(0, 0, -2), // North
            collectionCenter.offset(0, 0, 2)   // South
        };

        // Assign unique items to each note block
        Set<Item> assignedItems = new HashSet<>();
        for (Allay allay : swarm) {
            Item heldItem = allay.getMainHandItem().getItem();
            if (assignedItems.add(heldItem)) {
                int index = assignedItems.size() - 1;
                if (index < noteBlockPositions.length) {
                    noteBlockAssignments.put(noteBlockPositions[index], heldItem);
                }
            }
        }

        return noteBlockAssignments;
    }

    private void guideAllayToPosition(Allay allay, BlockPos targetPos) {
        // Use pathfinding to guide allay
        // Allays follow players, so foreman must lead them
        // Alternatively, use note blocks to attract them
    }

    private void reassignAllayTarget(Allay allay, Item newItem) {
        // Take current item
        // Give new item
        ItemStack newItemStack = new ItemStack(newItem);
        allay.setItemSlot(EquipmentSlot.MAINHAND, newItemStack);
    }
}
```

### Item-Specific Collection Strategies

```java
/**
 * Strategies for optimizing collection of specific item types.
 */
public class CollectionStrategyManager {

    public enum CollectionStrategy {
        HOARDER,       // Collect everything of one type
        SORTER,        // Sort into specific chests
        FARMER,        // Collect drops from farms
        MINER,         // Collect mined ores
        BUILDER,       // Collect construction materials
        GENERALIST     // Collect mixed items
    }

    /**
     * Determines the best collection strategy for an item type.
     */
    public static CollectionStrategy getStrategyForItem(Item item) {
        if (item == Items.WHEAT || item == Items.CARROT || item == Items.POTATO) {
            return CollectionStrategy.FARMER;
        } else if (item instanceof BlockItem && isOre(item)) {
            return CollectionStrategy.MINER;
        } else if (isConstructionMaterial(item)) {
            return CollectionStrategy.BUILDER;
        } else if (isStackableAndCommon(item)) {
            return CollectionStrategy.HOARDER;
        } else {
            return CollectionStrategy.SORTER;
        }
    }

    /**
     * Configures allay behavior based on strategy.
     */
    public static void configureAllayForStrategy(Allay allay, CollectionStrategy strategy,
                                                  BlockPos targetChest) {
        switch (strategy) {
            case HOARDER:
                // Assign to chest with hopper system
                break;
            case SORTER:
                // Assign to specific item sorting system
                break;
            case FARMER:
                // Position near farm output
                break;
            case MINER:
                // Position near mining machine
                break;
            case BUILDER:
                // Position near construction site
                break;
            case GENERALIST:
                // Use note block for flexible collection
                break;
        }
    }

    private static boolean isOre(Item item) {
        return item == Items.IRON_ORE || item == Items.GOLD_ORE ||
               item == Items.COPPER_ORE || item == Items.COAL_ORE ||
               item == Items.DIAMOND || item == Items.EMERALD;
    }

    private static boolean isConstructionMaterial(Item item) {
        return item == Items.COBBLESTONE || item == Items.STONE ||
               item == Items.DIRT || item == Items.OAK_PLANKS ||
               item instanceof BlockItem;
    }

    private static boolean isStackableAndCommon(Item item) {
        return item.getMaxStackSize() == 64;
    }
}
```

---

## Multi-Allay Systems

### Allay Squad Coordination

```java
import java.util.*;
import java.util.concurrent.*;

/**
 * Coordinates multiple allays for complex collection tasks.
 */
public class AllaySquad {
    private final String squadName;
    private final List<Allay> members;
    private final Map<UUID, AllayRole> roles;
    private final BlockPos headquarters;
    private final ForemanEntity commander;

    /**
     * Defines specialized roles for allays in a squad.
     */
    public enum AllayRole {
        LEADER,         // Primary allay, coordinates others
        COLLECTOR,      // Standard collection role
        TRANSPORTER,    // Moves items between locations
        SORTER,         // Organizes items by type
        RESERVE,        // Backup collector
        SPECIALIST      // Handles specific item types
    }

    public AllaySquad(String name, BlockPos hq, ForemanEntity commander) {
        this.squadName = name;
        this.members = new CopyOnWriteArrayList<>();
        this.roles = new ConcurrentHashMap<>();
        this.headquarters = hq;
        this.commander = commander;
    }

    /**
     * Adds an allay to the squad with a specified role.
     */
    public void recruitMember(Allay allay, AllayRole role) {
        members.add(allay);
        roles.put(allay.getUUID(), role);

        commander.sendChatMessage("Recruited " + allay.getName().getString() +
                                 " as " + role);
    }

    /**
     * Assigns tasks to squad members based on their roles.
     * @param task The collection task to execute
     */
    public void executeSquadTask(CollectionTask task) {
        switch (task.getType()) {
            case AREA_CLEANUP:
                executeAreaCleanup(task);
                break;
            case FARM_HARVEST:
                executeFarmHarvest(task);
                break;
            case ITEM_SORTING:
                executeItemSorting(task);
                break;
            case ORE_COLLECTION:
                executeOreCollection(task);
                break;
        }
    }

    private void executeAreaCleanup(CollectionTask task) {
        // Assign collectors to grid positions
        BlockPos areaStart = task.getStartPos();
        BlockPos areaEnd = task.getEndPos();
        int areaSize = (areaEnd.getX() - areaStart.getX()) *
                      (areaEnd.getZ() - areaStart.getZ());

        int allaysPerRow = (int) Math.sqrt(members.size());
        int rowsPerAllay = (areaEnd.getZ() - areaStart.getZ()) / allaysPerRow;

        int index = 0;
        for (int x = 0; x < allaysPerRow; x++) {
            for (int z = 0; z < allaysPerRow; z++) {
                if (index >= members.size()) break;

                Allay allay = members.get(index);
                BlockPos zoneStart = areaStart.offset(
                    x * (areaEnd.getX() - areaStart.getX()) / allaysPerRow,
                    0,
                    z * rowsPerAllay
                );

                assignAllayToZone(allay, zoneStart, rowsPerAllay);
                index++;
            }
        }
    }

    private void executeFarmHarvest(CollectionTask task) {
        // Position allays along farm rows
        List<Allay> collectors = members.stream()
            .filter(a -> roles.get(a.getUUID()) == AllayRole.COLLECTOR)
            .toList();

        BlockPos farmStart = task.getStartPos();
        int rowLength = task.getRowLength();
        int numRows = task.getNumRows();

        for (int i = 0; i < collectors.size() && i < numRows; i++) {
            Allay allay = collectors.get(i);
            BlockPos rowStart = farmStart.offset(0, 0, i * task.getRowSpacing());

            // Assign allay to collect from this row
            assignAllayToFarmRow(allay, rowStart, rowLength);
        }
    }

    private void executeItemSorting(CollectionTask task) {
        // Create note block sorting system
        Map<Item, BlockPos> sortingDestinations = task.getSortingDestinations();

        int allayIndex = 0;
        for (Map.Entry<Item, BlockPos> entry : sortingDestinations.entrySet()) {
            if (allayIndex >= members.size()) break;

            Allay allay = members.get(allayIndex);
            BlockPos noteBlockPos = entry.getValue();
            Item targetItem = entry.getKey();

            // Configure allay for this item
            ItemStack itemStack = new ItemStack(targetItem);
            allay.setItemSlot(EquipmentSlot.MAINHAND, itemStack);

            // Assign to note block
            commander.level().playSound(null, noteBlockPos,
                SoundEvents.NOTE_BLOCK_HARP.value(),
                SoundSource.BLOCKS, 1.0F, 1.0F);

            allayIndex++;
        }
    }

    private void executeOreCollection(CollectionTask task) {
        // Position allays at mining machine outputs
        List<BlockPos> machineOutputs = task.getMachineOutputs();

        for (int i = 0; i < members.size() && i < machineOutputs.size(); i++) {
            Allay allay = members.get(i);
            BlockPos outputPos = machineOutputs.get(i);

            // Position allay near output
            assignAllayToMachine(allay, outputPos);
        }
    }

    private void assignAllayToZone(Allay allay, BlockPos zoneStart, int zoneDepth) {
        // Guide allay to patrol zone
        // Use note blocks at zone corners to define boundaries
    }

    private void assignAllayToFarmRow(Allay allay, BlockPos rowStart, int rowLength) {
        // Place note blocks at row ends to guide allay
    }

    private void assignAllayToMachine(Allay allay, BlockPos outputPos) {
        // Position allay to collect from hopper output
    }

    /**
     * Rebalances squad when members are lost.
     */
    public void rebalanceSquad() {
        // Remove dead allays
        members.removeIf(allay -> !allay.isAlive());

        // Reassign roles if needed
        if (members.size() < 3) {
            commander.sendChatMessage("Squad " + squadName +
                                    " understrength! Recruit more allays.");
        }
    }

    /**
     * Returns squad statistics.
     */
    public SquadStats getStats() {
        SquadStats stats = new SquadStats();
        stats.memberCount = members.size();
        stats.roleDistribution = new HashMap<>();
        stats.headquarters = headquarters;

        for (AllayRole role : roles.values()) {
            stats.roleDistribution.merge(role, 1, Integer::sum);
        }

        return stats;
    }

    public static class SquadStats {
        public int memberCount;
        public Map<AllayRole, Integer> roleDistribution;
        public BlockPos headquarters;
    }
}
```

### Multi-Squad Coordination

```java
/**
 * Coordinates multiple allay squads for large-scale operations.
 */
public class AllayBattalion {
    private final Map<String, AllaySquad> squads;
    private final ForemanEntity commander;
    private final BlockPos commandCenter;

    public AllayBattalion(ForemanEntity commander, BlockPos commandCenter) {
        this.squads = new ConcurrentHashMap<>();
        this.commander = commander;
        this.commandCenter = commandCenter;
    }

    /**
     * Creates a new squad with specified parameters.
     */
    public AllaySquad createSquad(String name, int size, AllaySquad.AllayRole... roleDistribution) {
        AllaySquad squad = new AllaySquad(name, commandCenter.offset(squads.size() * 10, 0, 0), commander);

        // Spawn and recruit allays
        AllaySpawner spawner = new AllaySpawner(commander.level());
        for (int i = 0; i < size; i++) {
            Allay allay = spawner.spawnAllay(commandCenter.offset(i * 2, 0, 0));
            if (allay != null) {
                AllaySquad.AllayRole role = i < roleDistribution.length ?
                    roleDistribution[i] : AllaySquad.AllayRole.COLLECTOR;
                squad.recruitMember(allay, role);
            }
        }

        squads.put(name, squad);
        commander.sendChatMessage("Created squad '" + name + "' with " + size + " allays");
        return squad;
    }

    /**
     * Assigns squads to coordinated tasks.
     */
    public void executeBattalionTask(BattalionTask task) {
        switch (task.getOperationType()) {
            case LARGE_FARM_HARVEST:
                executeLargeHarvest(task);
                break;
            case MASS_ITEM_SORTING:
                executeMassSorting(task);
                break;
            case MULTI_ZONE_COLLECTION:
                executeMultiZoneCollection(task);
                break;
        }
    }

    private void executeLargeHarvest(BattalionTask task) {
        // Divide farm area among squads
        List<AllaySquad> availableSquads = new ArrayList<>(squads.values());
        int squadsPerRow = (int) Math.sqrt(availableSquads.size());

        for (int i = 0; i < availableSquads.size(); i++) {
            AllaySquad squad = availableSquads.get(i);
            CollectionTask squadTask = task.createSubTask(i, squadsPerRow);
            squad.executeSquadTask(squadTask);
        }
    }

    private void executeMassSorting(BattalionTask task) {
        // Assign different item categories to different squads
        Map<ItemCategory, AllaySquad> categoryAssignments = new HashMap<>();

        int squadIndex = 0;
        List<AllaySquad> squadList = new ArrayList<>(squads.values());

        for (ItemCategory category : task.getCategories()) {
            if (squadIndex >= squadList.size()) break;

            AllaySquad squad = squadList.get(squadIndex++);
            squad.executeSquadTask(task.createCategoryTask(category));
        }
    }

    private void executeMultiZoneCollection(BattalionTask task) {
        // Assign each squad to a different zone
        List<BlockPos> zones = task.getCollectionZones();
        List<AllaySquad> squadList = new ArrayList<>(squads.values());

        for (int i = 0; i < squadList.size() && i < zones.size(); i++) {
            AllaySquad squad = squadList.get(i);
            squad.executeSquadTask(task.createZoneTask(zones.get(i)));
        }
    }

    /**
     * Coordinates resource sharing between squads.
     */
    public void shareResources() {
        // Collect all allays from all squads
        List<Allay> allAllays = squads.values().stream()
            .flatMap(squad -> squad.getMembers().stream())
            .toList();

        // Redistribute based on current needs
        // This allows squads to borrow allays temporarily
    }

    public enum ItemCategory {
        FOOD, CROPS, ORES, WOOD, STONE, MISCELLANEOUS
    }
}
```

---

## Code Examples for Integration

### Action: TameAllayAction

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.allay.AllayManager;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.item.ItemStack;

/**
 * Action that tames an allay for item collection.
 */
public class TameAllayAction extends BaseAction {
    private enum Phase {
        SEARCHING,
        TAMING,
        COMPLETE
    }

    private Phase phase = Phase.SEARCHING;
    private Allay targetAllay;
    private ItemStack itemToGive;
    private int searchRadius = 32;
    private int ticksWithoutProgress = 0;

    public TameAllayAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.itemToGive = task.getParameter("item", ItemStack.class);
    }

    @Override
    protected void onStart() {
        if (itemToGive == null || itemToGive.isEmpty()) {
            result = ActionResult.failure("No item specified to give allay");
            return;
        }

        foreman.sendChatMessage("Searching for allay to tame...");
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case SEARCHING -> searchForAllay();
            case TAMING -> tameAllay();
        }
    }

    private void searchForAllay() {
        AllayDetector detector = new AllayDetector(
            (ServerLevel) foreman.level(),
            foreman.blockPosition(),
            searchRadius
        );

        List<Allay> allays = detector.findAllays();
        if (allays.isEmpty()) {
            ticksWithoutProgress++;
            if (ticksWithoutProgress > 100) {
                result = ActionResult.failure("No allays found within " + searchRadius + " blocks");
            }
            return;
        }

        // Find nearest allay
        targetAllay = allays.stream()
            .min(Comparator.comparingDouble(a -> a.distanceToSqr(foreman)))
            .orElse(null);

        if (targetAllay != null) {
            phase = Phase.TAMING;
            foreman.sendChatMessage("Found allay, attempting to tame...");
        }
    }

    private void tameAllay() {
        if (targetAllay == null || !targetAllay.isAlive()) {
            result = ActionResult.failure("Target allay disappeared");
            return;
        }

        // Move close to allay
        if (foreman.distanceToSqr(targetAllay) > 9) {
            foreman.getNavigation().moveTo(targetAllay, 0.5);
            ticksWithoutProgress++;
            if (ticksWithoutProgress > 200) {
                result = ActionResult.failure("Cannot reach allay");
            }
            return;
        }

        // Give item to allay
        ItemStack itemCopy = itemToGive.copy();
        itemCopy.setCount(1);

        // Use vanilla interaction to tame
        targetAllay.interact(foreman, net.minecraft.world.InteractionHand.MAIN_HAND);

        // Verify allay is holding item
        if (!targetAllay.getMainHandItem().isEmpty()) {
            result = ActionResult.success("Successfully tamed allay!");
            foreman.getMemory().rememberAllay(targetAllay.getUUID(), itemToGive.getItem());
        } else {
            ticksWithoutProgress++;
            if (ticksWithoutProgress > 40) {
                result = ActionResult.failure("Failed to tame allay");
            }
        }
    }

    @Override
    protected void onCancel() {
        if (targetAllay != null && targetAllay.isAlive()) {
            // Allay returns to wild
        }
    }

    @Override
    public String getDescription() {
        return "Taming allay with " + (itemToGive != null ? itemToGive.getItem().getDescription() : "unknown item");
    }
}
```

### Action: AssignAllayToNoteBlockAction

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Action that assigns an allay to a note block for item delivery.
 */
public class AssignAllayToNoteBlockAction extends BaseAction {
    private enum Phase {
        MOVING_TO_POSITION,
        PLACING_NOTE_BLOCK,
        ACTIVATING_ALLAY,
        COMPLETE
    }

    private Phase phase = Phase.MOVING_TO_POSITION;
    private BlockPos noteBlockPos;
    private net.minecraft.world.entity.animal.allay.Allay targetAllay;
    private int ticksWaiting = 0;

    public AssignAllayToNoteBlockAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.noteBlockPos = task.getParameter("position", BlockPos.class);
    }

    @Override
    protected void onStart() {
        if (noteBlockPos == null) {
            result = ActionResult.failure("No note block position specified");
            return;
        }

        // Find nearby allay
        AllayDetector detector = new AllayDetector(
            (ServerLevel) foreman.level(),
            foreman.blockPosition(),
            16
        );

        var allays = detector.findAllays();
        if (allays.isEmpty()) {
            result = ActionResult.failure("No allays nearby to assign");
            return;
        }

        targetAllay = allays.get(0);
        foreman.sendChatMessage("Assigning allay to note block at " + noteBlockPos);
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case MOVING_TO_POSITION -> moveToPosition();
            case PLACING_NOTE_BLOCK -> placeNoteBlock();
            case ACTIVATING_ALLAY -> activateAllay();
        }
    }

    private void moveToPosition() {
        double distance = foreman.blockPosition().distSqr(noteBlockPos);
        if (distance > 9) {
            foreman.getNavigation().moveTo(noteBlockPos.getX(), noteBlockPos.getY(), noteBlockPos.getZ(), 0.5);
            ticksWaiting++;
            if (ticksWaiting > 300) {
                result = ActionResult.failure("Cannot reach note block position");
            }
        } else {
            phase = Phase.PLACING_NOTE_BLOCK;
        }
    }

    private void placeNoteBlock() {
        BlockState currentState = foreman.level().getBlockState(noteBlockPos);

        if (currentState.getBlock() instanceof NoteBlock) {
            phase = Phase.ACTIVATING_ALLAY;
            return;
        }

        // Place note block (if foreman has one in inventory)
        // This would require inventory management
        phase = Phase.ACTIVATING_ALLAY;
    }

    private void activateAllay() {
        if (targetAllay == null || !targetAllay.isAlive()) {
            result = ActionResult.failure("Allay disappeared");
            return;
        }

        // Play note block sound to attract allay
        foreman.level().playSound(null, noteBlockPos,
            SoundEvents.NOTE_BLOCK_HARP.value(),
            SoundSource.BLOCKS, 1.0F, 1.0F);

        ticksWaiting++;
        if (ticksWaiting > 100) {
            // Verify allay is targeting note block
            BlockPos allayPos = targetAllay.blockPosition();
            if (allayPos.distSqr(noteBlockPos) < 25) {
                result = ActionResult.success("Allay assigned to note block!");
            } else {
                result = ActionResult.failure("Allay not responding to note block");
            }
        }
    }

    @Override
    protected void onCancel() {
        foreman.sendChatMessage("Cancelled allay assignment");
    }

    @Override
    public String getDescription() {
        return "Assigning allay to note block at " + noteBlockPos;
    }
}
```

### Integration with ForemanEntity

```java
package com.minewright.entity;

import com.minewright.allay.*;

public class ForemanEntity extends PathfinderMob {
    // ... existing fields ...

    private AllayManager allayManager;
    private AllayBattalion allayBattalion;

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // ... existing tick code ...

            // Tick allay systems
            if (allayManager != null) {
                allayManager.tick();
            }
        }
    }

    /**
     * Gets or creates the allay manager for this foreman.
     */
    public AllayManager getAllayManager() {
        if (allayManager == null) {
            allayManager = new AllayManager(this);
        }
        return allayManager;
    }

    /**
     * Gets or creates the allay battalion for large-scale operations.
     */
    public AllayBattalion getAllayBattalion() {
        if (allayBattalion == null) {
            allayBattalion = new AllayBattalion(this, this.blockPosition());
        }
        return allayBattalion;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        // Save allay tracking data
        if (allayManager != null) {
            CompoundTag allayTag = new CompoundTag();
            allayManager.saveToNBT(allayTag);
            tag.put("AllayManager", allayTag);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        // Load allay tracking data
        if (tag.contains("AllayManager")) {
            allayManager = new AllayManager(this);
            allayManager.loadFromNBT(tag.getCompound("AllayManager"));
        }
    }
}
```

### Plugin Registration

```java
package com.minewright.plugin;

import com.minewright.action.actions.*;

public class AllayActionsPlugin implements ActionPlugin {
    @Override
    public void registerActions(ActionRegistry registry) {
        registry.register("tame_allay",
            (foreman, task, ctx) -> new TameAllayAction(foreman, task),
            10, "allay");

        registry.register("assign_allay",
            (foreman, task, ctx) -> new AssignAllayToNoteBlockAction(foreman, task),
            10, "allay");

        registry.register("duplicate_allay",
            (foreman, task, ctx) -> new DuplicateAllayAction(foreman, task),
            10, "allay");

        registry.register("create_allay_squad",
            (foreman, task, ctx) -> CreateAllaySquadAction(foreman, task),
            10, "allay");
    }

    @Override
    public String getPluginId() {
        return "allay";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
```

### Prompt Builder Integration

```java
package com.minewright.llm;

public class PromptBuilder {
    // ... existing code ...

    private static final String ALLAY_ACTIONS = """

Allay Coordination Actions:
- tame_allay(item): Tame a nearby allay with the specified item
  Parameters: item (ItemStack)
  Example: "tame_allay(diamond)"

- assign_allay(position): Assign a tamed allay to a note block at position
  Parameters: position (BlockPos)
  Example: "assign_allay(100, 64, 200)"

- duplicate_allay(): Duplicate a dancing allay using amethyst shard
  Requirements: Jukebox nearby playing music, amethyst shard in inventory

- create_allay_squad(name, size, roles): Create an allay squad for large operations
  Parameters: name (String), size (int), roles (array of AllayRole)
  Example: "create_allay_squad(harvesters, 5, [LEADER, COLLECTOR, COLLECTOR, COLLECTOR, TRANSPORTER])"

Allay Behavior Notes:
- Allays collect items matching the item they're given
- They can be assigned to note blocks for automatic delivery
- Multiple allays can coordinate for large collection tasks
- Allays have a 3-second cooldown between collection trips
- Note block assignments last 30 seconds before allay returns to player
""";

    public static String buildSystemPrompt() {
        return BASE_SYSTEM_PROMPT + ALLAY_ACTIONS + getRegisteredActions();
    }
}
```

---

## Implementation Roadmap

### Phase 1: Core Allay Integration (Week 1-2)

**Objective**: Basic allay detection and taming

- [ ] Create `AllayDetector` class
  - Implement radius-based entity search
  - Filter for allay entities only
  - Add held item inspection

- [ ] Create `AllayManager` class
  - Implement basic taming functionality
  - Track tamed allays by UUID
  - Add note block assignment methods

- [ ] Implement `TameAllayAction`
  - Search for nearby allays
  - Navigate to allay position
  - Give item to tame allay
  - Handle taming failures

- [ ] Implement `AssignAllayToNoteBlockAction`
  - Place/locate note block
  - Play note block sound
  - Verify allay response

- [ ] Add `ForemanEntity` integration
  - Add allay manager field
  - Implement save/load NBT
  - Add tick processing

- [ ] Testing
  - Spawn allays in test world
  - Test taming with various items
  - Test note block assignment
  - Verify allay delivery behavior

### Phase 2: Collection Optimization (Week 3-4)

**Objective**: Optimize allay collection behavior

- [ ] Create `AllaySwarmOptimizer`
  - Implement optimal positioning algorithm
  - Add load balancing logic
  - Create collection zone builder

- [ ] Create `CollectionStrategyManager`
  - Define item-specific strategies
  - Implement strategy detection
  - Add strategy configuration

- [ ] Implement `GatherWithAllaysAction`
  - Coordinate multiple allays
  - Assign collection zones
  - Monitor collection progress

- [ ] Add hopper integration
  - Connect allays to chest systems
  - Implement automatic sorting
  - Handle overflow scenarios

- [ ] Performance optimization
  - Reduce allay scan frequency
  - Cache allay positions
  - Optimize chunk loading

- [ ] Testing
  - Test with 5-10 allays
  - Measure collection rates
  - Verify load balancing
  - Test hopper integration

### Phase 3: Multi-Allay Systems (Week 5-6)

**Objective**: Squad and battalion-level coordination

- [ ] Create `AllaySquad` class
  - Implement role system
  - Add squad task execution
  - Create task distribution logic

- [ ] Create `AllayBattalion` class
  - Implement multi-squad coordination
  - Add resource sharing
  - Create battalion task types

- [ ] Implement squad actions
  - `CreateAllaySquadAction`
  - `AssignSquadTaskAction`
  - `RebalanceSquadAction`

- [ ] Add specialized roles
  - Leader allay behavior
  - Transporter routing
  - Sorter assignment

- [ ] Communication system
  - Squad-to-squad messages
  - Status reporting
  - Task delegation

- [ ] Testing
  - Test with multiple squads
  - Verify role distribution
  - Test inter-squad coordination
  - Measure overall efficiency

### Phase 4: Advanced Features (Week 7-8)

**Objective**: Advanced allay capabilities

- [ ] Allay duplication automation
  - Detect dancing allays
  - Auto-feed amethyst shards
  - Manage duplication cooldowns

- [ ] Allay breeding stations
  - Jukebox positioning
  - Amethyst supply
  - New allay assignment

- [ ] Smart sorting systems
  - Item categorization
  - Automatic routing
  - Priority handling

- [ ] GUI integration
  - Allay management screen
  - Squad status display
  - Task assignment interface

- [ ] Memory integration
  - Remember allay locations
  - Track collection statistics
  - Learn optimal patterns

- [ ] Testing
  - Full automation tests
  - GUI usability tests
  - Memory system validation
  - Performance benchmarks

### Phase 5: Production Readiness (Week 9-10)

**Objective**: Polish and deploy

- [ ] Error handling
  - Allay death recovery
  - Lost allay detection
  - Chunk unload handling

- [ ] Configuration
  - Allay limit settings
  - Squad size limits
  - Performance tuning

- [ ] Documentation
  - User guide
  - API documentation
  - Tutorial videos

- [ ] Performance optimization
  - Reduce server tick impact
  - Optimize entity tracking
  - Memory leak prevention

- [ ] Final testing
  - Multiplayer testing
  - Long-duration tests
  - Edge case coverage

- [ ] Release preparation
  - Version tagging
  - Release notes
  - Distribution

---

## Configuration

### config/steve-common.toml

```toml
[allay]
# Maximum allays that can be tamed per foreman
max_allays = 20

# Maximum squads per battalion
max_squads = 5

# Maximum allays per squad
max_squad_size = 10

# Allay search radius (blocks)
search_radius = 64

# How often to scan for allays (ticks)
scan_interval = 100

# Enable automatic allay duplication
auto_duplicate = true

# Amethyst shard consumption limit per hour
amethyst_limit = 12

[note_block]
# Duration of note block assignment (ticks)
assignment_duration = 600

# Note block sound range
sound_range = 16

# Auto-refresh note block assignment
auto_refresh = true

[collection]
# Collection strategy to use
default_strategy = "HOARDER"

# Enable hopper integration
use_hoppers = true

# Hopper transfer cooldown (ticks)
hopper_cooldown = 8

# Enable automatic item sorting
auto_sort = true

[squad]
# Default squad size
default_size = 5

# Enable role auto-assignment
auto_assign_roles = true

# Enable inter-squad communication
enable_communication = true

# Status update interval (ticks)
status_interval = 200
```

---

## Troubleshooting

### Common Issues

**Issue**: Allays not collecting items
- **Cause**: Allay doesn't have matching item, or mobGriefing is disabled
- **Solution**: Verify allay is holding correct item, check `/gamerule mobGriefing`

**Issue**: Allays not responding to note blocks
- **Cause**: Note block out of range (16 blocks) or wool blocking vibration
- **Solution**: Move note block closer, remove wool blocks

**Issue**: Allays returning to player instead of note block
- **Cause**: 30-second timer expired
- **Solution**: Replay note block sound to reset timer

**Issue**: Duplication not working
- **Cause**: Allay not dancing (no jukebox) or on cooldown
- **Solution**: Ensure jukebox is playing, wait 5 minutes between duplications

**Issue**: Low collection efficiency
- **Cause**: Too few allays for area size, poor positioning
- **Solution**: Add more allays, use `AllaySwarmOptimizer` for better positioning

### Debug Commands

```java
// List all tamed allays
/steve allay list

// Show allay details
/steve allay info <uuid>

// Reassign allay to note block
/steve allay assign <uuid> <x> <y> <z>

// Create squad
/steve squad create <name> <size>

// Show squad status
/steve squad status <name>

// Debug allay behavior
/steve allay debug <uuid>
```

---

## Performance Considerations

### Tick Impact

- **Allay scanning**: Only scan every 100 ticks (5 seconds)
- **Entity tracking**: Use UUIDs instead of entity references
- **Chunk loading**: Pre-load chunks containing allays
- **Hopper checks**: Batch hopper operations

### Memory Management

- **Tracking limits**: Max 20 allays per foreman
- **Squad limits**: Max 5 squads with 10 allays each
- **History cleanup**: Remove dead allays from tracking
- **NBT compression**: Compress allay data in saves

### Server Impact

- **Async operations**: Use separate threads for allay calculations
- **Rate limiting**: Limit allay operations per tick
- **Chunk unloading**: Pause allays in unloaded chunks
- **Network optimization**: Batch allay status updates

---

## Conclusion

This allay coordination system provides comprehensive integration of Minecraft's allay mechanics with MineWright's AI agents. The modular design allows for incremental implementation, starting with basic taming and progressing to advanced multi-squad operations.

Key benefits:
- **Automated collection**: Allays handle repetitive item gathering
- **Scalable**: From single allay to battalion-sized operations
- **Flexible**: Supports multiple collection strategies
- **Integrated**: Works with existing MineWright infrastructure
- **Performant**: Optimized for minimal server impact

The implementation roadmap provides a clear path from concept to production, with testing and validation at each phase.
