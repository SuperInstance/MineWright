# Frog Farming AI Design for MineWright (Forge 1.20.1)

## Overview

This document outlines the design and implementation of frog farming AI capabilities for the MineWright Minecraft mod. The frog farming system will enable Foreman entities to autonomously breed frogs, harvest froglights from magma cubes, and manage tadpole development in mangrove swamp biomes.

## Table of Contents

1. [Frog Breeding Mechanics](#frog-breeding-mechanics)
2. [Froglight Collection System](#froglight-collection-system)
3. [Tadpole Management](#tadpole-management)
4. [Mangrove Swamp Navigation](#mangrove-swamp-navigation)
5. [Multi-Frog Coordination](#multi-frog-coordination)
6. [Implementation Roadmap](#implementation-roadmap)
7. [Code Examples](#code-examples)

---

## Frog Breeding Mechanics

### Vanilla Minecraft Behavior

Frogs in Minecraft 1.20.1 have the following breeding mechanics:

- **Breeding Item**: Slimeballs
- **Breeding Process**: Feed two frogs slimeballs to enter love mode
- **Pregnancy**: One frog becomes pregnant and searches for water
- **Egg Laying**: Pregnant frog lays frogspawn in water blocks with air above
- **Hatching**: Frogspawn hatches into tadpoles after ~2-3 minutes
- **Growth**: Tadpoles take 20 minutes to grow into frogs (accelerated by slimeballs)

### Frog Variants

The frog variant is determined by the biome temperature where the tadpole grows up:

| Variant | Color | Biome Temperature | Froglight Color |
|---------|-------|-------------------|-----------------|
| Temperate | Green | Temperate | Ochre (yellow) |
| Warm | White/Gray | Warm | Pearlescent (pink/purple) |
| Cold | White | Cold | Verdant (green) |

### AI Behavior Design

The Foreman AI will implement the following breeding behaviors:

1. **Slimeball Collection**: Auto-hunt slimes for breeding materials
2. **Frog Location**: Scan nearby areas for frogs (swamp biomes)
3. **Breeding Execution**: Feed frogs slimeballs in pairs
4. **Spawn Protection**: Guard frogspawn from hostile mobs
5. **Growth Monitoring**: Track tadpole development progress

---

## Froglight Collection System

### Magma Cube Farming Strategy

Froglights are produced when frogs eat small magma cubes. The AI will implement:

1. **Nether Portal Construction**: Build portal to Nether
2. **Magma Cube Spawner Discovery**: Locate magma cube spawners or basalt delta areas
3. **Frog Transport**: Safely move frogs to magma cube farms
4. **Collection Automation**: Hopper system or manual collection
5. **Return Transport**: Bring frogs back to overworld base

### Froglight Farm Design

```
┌─────────────────────────────────────┐
│   Froglight Collection Chamber       │
├─────────────────────────────────────┤
│  ┌─────┐  ┌─────┐  ┌─────┐          │
│  │Frog │  │Frog │  │Frog │          │
│  └─────┘  └─────┘  └─────┘          │
│      ↓       ↓       ↓               │
│  ┌─────────────────────────┐        │
│  │   Magma Cube Spawn      │        │
│  │   (Water Trap)          │        │
│  └─────────────────────────┘        │
│              ↓                       │
│  ┌─────────────────────────┐        │
│  │   Hopper Collection     │        │
│  └─────────────────────────┘        │
└─────────────────────────────────────┘
```

### Collection AI Workflow

1. **Position Foreman**: Stand near collection point
2. **Monitor Frogs**: Watch for froglight drops
3. **Collect Items**: Pick up froglights within range
4. **Storage Management**: Move froglights to designated chest
5. **Quality Control**: Sort froglights by color type

---

## Tadpole Management

### Tadpole Care System

Tadpoles are vulnerable and require special handling:

- **Container**: Water bucket or enclosed water area
- **Protection**: Guard against axolotls and other predators
- **Growth Acceleration**: Feed slimeballs to speed up growth
- **Variant Control**: Control biome temperature for desired variant

### Tadpole Lifecycle Management

```
Frogspawn (2-3 min)
       ↓
   Tadpole (20 min natural)
       ↓
  Feed Slimeball (-10% time each)
       ↓
    Frog (variant based on biome)
```

### AI Tadpole Behaviors

1. **Spawn Monitoring**: Check frogspawn blocks regularly
2. **Hatching Detection**: Detect when tadpoles spawn
3. **Feeding Schedule**: Automatic slimeball feeding for growth
4. **Protection Duty**: Fight off axolotls and guardians
5. **Variant Selection**: Move tadpoles to specific biomes for variants

---

## Mangrove Swamp Navigation

### Biome Characteristics

Mangrove swamps present unique navigation challenges:

- **Water Coverage**: Extensive water and mud blocks
- **Tree Density**: Dense mangrove tree canopy
- **Terrain**: Uneven terrain with waterlogged blocks
- **Mob Spawning**: High hostile mob spawn rate

### Navigation AI

1. **Pathfinding**: Water-aware pathfinding algorithm
2. **Tree Climbing**: Mangrove root navigation
3. **Boat Usage**: Deploy boats for water travel
4. **Mob Avoidance**: Evasive maneuvers for hostile mobs
5. **Base Building**: Elevated platforms for frog housing

### Swamp Base Design

```
                ┌───────────────┐
                │   Frog House  │
                │  (Elevated)   │
                └───────┬───────┘
                        │
         ┌──────────────┴──────────────┐
         │     Water Channels          │
         │  (Tadpole/Pond Area)        │
         └─────────────────────────────┘
         │      │      │      │      │
       Mangrove Trees (Frog Housing)
```

---

## Multi-Frog Coordination

### Colony Management

Multiple Foremen can coordinate for large-scale frog farming:

1. **Role Assignment**: Specialized roles for efficiency
   - **Breeder**: Handles frog breeding
   - **Collector**: Gathers froglights
   - **Protector**: Defends from mobs
   - **Feeder**: Manages tadpole feeding

2. **Resource Sharing**: Share slimeballs and froglights
3. **Task Synchronization**: Coordinate breeding cycles
4. **Alert System**: Communicate threats and opportunities

### Communication Protocol

```java
// AgentMessage types for frog farming
AgentMessage frogStatus = AgentMessage.builder()
    .type(AgentMessage.Type.FROG_STATUS)
    .payload("frogCount", 12)
    .payload("tadpoleCount", 8)
    .payload("froglightStock", 64)
    .build();

AgentMessage breedingRequest = AgentMessage.builder()
    .type(AgentMessage.Type.BREEDING_REQUEST)
    .payload("slimeballsNeeded", 4)
    .payload("targetVariant", "warm")
    .build();
```

---

## Implementation Roadmap

### Phase 1: Basic Frog Interaction (Week 1-2)

**Tasks:**
- [ ] Create `FrogBreedingAction` class
- [ ] Implement slimeball feeding logic
- [ ] Add frog detection and tracking
- [ ] Create frogspawn monitoring

**Files to Create:**
```
src/main/java/com/minewright/action/actions/FrogBreedingAction.java
src/main/java/com/minewright/action/actions/FrogFeedAction.java
src/main/java/com/minewright/entity/FrogTracker.java
```

### Phase 2: Tadpole Management (Week 3)

**Tasks:**
- [ ] Create `TadpoleCareAction` class
- [ ] Implement growth acceleration
- [ ] Add variant control system
- [ ] Create protection behaviors

**Files to Create:**
```
src/main/java/com/minewright/action/actions/TadpoleCareAction.java
src/main/java/com/minewright/entity/TadpoleManager.java
```

### Phase 3: Froglight Collection (Week 4)

**Tasks:**
- [ ] Create `FroglightCollectAction` class
- [ ] Implement magma cube detection
- [ ] Add Nether portal navigation
- [ ] Create collection automation

**Files to Create:**
```
src/main/java/com/minewright/action/actions/FroglightCollectAction.java
src/main/java/com/minewright/nether/MagmaCubeFarm.java
```

### Phase 4: Swamp Navigation (Week 5)

**Tasks:**
- [ ] Create `SwampNavigationAction` class
- [ ] Implement boat deployment
- [ ] Add mangrove pathfinding
- [ ] Create base building in swamps

**Files to Create:**
```
src/main/java/com/minewright/action/actions/SwampNavigationAction.java
src/main/java/com/minewright/navigation/SwampPathfinder.java
```

### Phase 5: Multi-Agent Coordination (Week 6)

**Tasks:**
- [ ] Extend `AgentMessage` with frog farming types
- [ ] Create colony management system
- [ ] Implement role assignment
- [ ] Add resource sharing

**Files to Modify:**
```
src/main/java/com/minewright/orchestration/AgentMessage.java
src/main/java/com/minewright/orchestration/FrogColonyManager.java
```

### Phase 6: LLM Integration (Week 7)

**Tasks:**
- [ ] Update `PromptBuilder` with frog farming actions
- [ ] Add frog farming context to prompts
- [ ] Create task planning for frog operations

**Files to Modify:**
```
src/main/java/com/minewright/llm/PromptBuilder.java
src/main/java/com/minewright/plugin/CoreActionsPlugin.java
```

---

## Code Examples

### Action 1: Frog Breeding

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Frog;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Comparator;

public class FrogBreedingAction extends BaseAction {
    private Frog targetFrog1;
    private Frog targetFrog2;
    private int slimeballsNeeded;
    private int ticksWaiting;
    private BlockPos breedingLocation;
    private static final int BREEDING_TIMEOUT = 1200; // 60 seconds
    private static final double SEARCH_RADIUS = 32.0;

    public FrogBreedingAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        slimeballsNeeded = task.getIntParameter("quantity", 2);
        breedingLocation = task.getBlockPosParameter("location");

        // Ensure we have slimeballs
        if (!hasSlimeballs()) {
            result = ActionResult.failure("No slimeballs available for breeding");
            return;
        }

        // Find two frogs
        findBreedingPair();

        if (targetFrog1 == null || targetFrog2 == null) {
            result = ActionResult.failure("Could not find two frogs for breeding");
        }
    }

    @Override
    protected void onTick() {
        ticksWaiting++;

        if (ticksWaiting > BREEDING_TIMEOUT) {
            result = ActionResult.failure("Breeding timeout - frogs didn't breed");
            return;
        }

        // Check if frogs are still valid
        if (!targetFrog1.isAlive() || !targetFrog2.isAlive()) {
            result = ActionResult.failure("One or both frogs died");
            return;
        }

        // Move to breeding location if specified
        if (breedingLocation != null) {
            double distance = foreman.distanceToSqr(
                breedingLocation.getX(), breedingLocation.getY(), breedingLocation.getZ()
            );
            if (distance > 9.0) {
                foreman.getNavigation().moveTo(
                    breedingLocation.getX(), breedingLocation.getY(), breedingLocation.getZ(),
                    1.0
                );
                return;
            }
        }

        // Feed frogs slimeballs
        if (ticksWaiting % 40 == 0) { // Every 2 seconds
            feedFrog(targetFrog1);
            feedFrog(targetFrog2);

            // Check if breeding started
            if (targetFrog1.isInLove() && targetFrog2.isInLove()) {
                result = ActionResult.success("Frogs are breeding!");
            }
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Breeding frogs at " + breedingLocation;
    }

    private boolean hasSlimeballs() {
        return foreman.getInventory().contains(
            new ItemStack(Items.SLIMEBALL, slimeballsNeeded * 2)
        );
    }

    private void findBreedingPair() {
        AABB searchBox = foreman.getBoundingBox().inflate(SEARCH_RADIUS);
        List<Entity> entities = foreman.level().getEntities(foreman, searchBox);

        List<Frog> frogs = entities.stream()
            .filter(e -> e instanceof Frog)
            .map(e -> (Frog) e)
            .filter(Frog::isAlive)
            .sorted(Comparator.comparingDouble(f -> foreman.distanceToSqr(f)))
            .limit(2)
            .toList();

        if (frogs.size() >= 2) {
            targetFrog1 = frogs.get(0);
            targetFrog2 = frogs.get(1);
        }
    }

    private void feedFrog(Frog frog) {
        // Simulate feeding the frog
        frog.setInLove(null); // Enter love mode
        foreman.getInventory().removeItem(
            foreman.getInventory().findSlotMatchingItemCount(
                new ItemStack(Items.SLIMEBALL)
            ), 1
        );
    }
}
```

### Action 2: Froglight Collection

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Frog;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class FroglightCollectAction extends BaseAction {
    private Frog assignedFrog;
    private BlockPos collectionPoint;
    private int froglightsCollected;
    private int targetQuantity;
    private int ticksRunning;
    private static final int MAX_DURATION = 6000; // 5 minutes

    public FroglightCollectAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        targetQuantity = task.getIntParameter("quantity", 16);
        collectionPoint = task.getBlockPosParameter("collectionPoint");

        // Find frog to "work with" (monitor)
        findFrog();

        if (assignedFrog == null) {
            result = ActionResult.failure("No frog found for froglight collection");
            return;
        }

        foreman.sendChatMessage("Monitoring frog for froglights...");
    }

    @Override
    protected void onTick() {
        ticksRunning++;

        if (ticksRunning > MAX_DURATION) {
            result = ActionResult.success("Collection period complete. Got " +
                froglightsCollected + " froglights");
            return;
        }

        if (froglightsCollected >= targetQuantity) {
            result = ActionResult.success("Collected " + froglightsCollected + " froglights!");
            return;
        }

        // Move to collection point
        if (collectionPoint != null) {
            double distance = foreman.distanceToSqr(
                collectionPoint.getX(), collectionPoint.getY(), collectionPoint.getZ()
            );
            if (distance > 9.0) {
                foreman.getNavigation().moveTo(
                    collectionPoint.getX(), collectionPoint.getY(), collectionPoint.getZ(),
                    1.0
                );
                return;
            }
        }

        // Look for magma cubes near frog
        if (ticksRunning % 20 == 0) {
            directFrogToMagmaCubes();
        }

        // Collect dropped froglights
        collectNearbyFroglights();
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return String.format("Collecting froglights (%d/%d)",
            froglightsCollected, targetQuantity);
    }

    private void findFrog() {
        AABB searchBox = foreman.getBoundingBox().inflate(16.0);
        List<Entity> entities = foreman.level().getEntities(foreman, searchBox);

        assignedFrog = entities.stream()
            .filter(e -> e instanceof Frog)
            .map(e -> (Frog) e)
            .filter(Frog::isAlive)
            .findFirst()
            .orElse(null);
    }

    private void directFrogToMagmaCubes() {
        if (assignedFrog == null || !assignedFrog.isAlive()) {
            findFrog();
            return;
        }

        // Find nearby magma cubes
        AABB searchBox = assignedFrog.getBoundingBox().inflate(16.0);
        List<Entity> entities = foreman.level().getEntities(assignedFrog, searchBox);

        boolean hasMagmaCubes = entities.stream()
            .anyMatch(e -> e instanceof MagmaCube);

        if (!hasMagmaCubes) {
            // Foreman could attract/lure magma cubes
            foreman.sendChatMessage("Waiting for magma cubes...");
        }
    }

    private void collectNearbyFroglights() {
        AABB collectBox = foreman.getBoundingBox().inflate(4.0);
        List<Entity> items = foreman.level().getEntities(
            foreman, collectBox,
            e -> e.getType() == EntityType.ITEM
        );

        for (Entity item : items) {
            net.minecraft.world.entity.item.ItemEntity itemEntity =
                (net.minecraft.world.entity.item.ItemEntity) item;

            if (itemEntity.getItem().is(Items.FROGLIGHT)) {
                // Collect the item
                foreman.getInventory().addItem(itemEntity.getItem());
                itemEntity.discard();
                froglightsCollected++;
            }
        }
    }
}
```

### Action 3: Tadpole Care

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Tadpole;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class TadpoleCareAction extends BaseAction {
    private List<Tadpole> tadpoles;
    private BlockPos careArea;
    private int targetFrogs;
    private String desiredVariant;
    private int ticksRunning;
    private static final int GROWTH_CHECK_INTERVAL = 100; // 5 seconds
    private static final int FEED_INTERVAL = 200; // 10 seconds

    public TadpoleCareAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        targetFrogs = task.getIntParameter("quantity", 4);
        desiredVariant = task.getStringParameter("variant", "temperate");
        careArea = task.getBlockPosParameter("area");

        findTadpoles();

        if (tadpoles.isEmpty()) {
            result = ActionResult.failure("No tadpoles found in area");
            return;
        }

        foreman.sendChatMessage("Caring for " + tadpoles.size() + " tadpoles");
    }

    @Override
    protected void onTick() {
        ticksRunning++;

        // Move to care area center
        if (careArea != null) {
            double distance = foreman.distanceToSqr(
                careArea.getX(), careArea.getY(), careArea.getZ()
            );
            if (distance > 16.0) {
                foreman.getNavigation().moveTo(
                    careArea.getX(), careArea.getY(), careArea.getZ(),
                    1.0
                );
                return;
            }
        }

        // Feed tadpoles periodically
        if (ticksRunning % FEED_INTERVAL == 0) {
            feedTadpoles();
        }

        // Check for predators
        if (ticksRunning % 40 == 0) {
            checkForPredators();
        }

        // Monitor growth
        if (ticksRunning % GROWTH_CHECK_INTERVAL == 0) {
            checkGrowthProgress();
        }

        // Check if task complete
        long grownFrogs = tadpoles.stream()
            .filter(t -> !t.isAlive() || t.getAge() >= 0)
            .count();

        if (grownFrogs >= targetFrogs) {
            result = ActionResult.success("Raised " + grownFrogs + " frogs to adulthood!");
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return String.format("Caring for %d tadpoles (%s variant)",
            tadpoles.size(), desiredVariant);
    }

    private void findTadpoles() {
        AABB searchBox = careArea != null ?
            new AABB(careArea).inflate(16.0) :
            foreman.getBoundingBox().inflate(32.0);

        List<Entity> entities = foreman.level().getEntities(foreman, searchBox);

        tadpoles = entities.stream()
            .filter(e -> e instanceof Tadpole)
            .map(e -> (Tadpole) e)
            .filter(Tadpole::isAlive)
            .toList();
    }

    private void feedTadpoles() {
        if (!hasSlimeballs()) {
            return;
        }

        for (Tadpole tadpole : tadpoles) {
            if (tadpole.isAlive()) {
                // Feed slimeball to accelerate growth
                tadpole.setAge(tadpole.getAge() + 1200); // Reduce time by ~10%
                foreman.getInventory().removeItem(
                    foreman.getInventory().findSlotMatchingItemCount(
                        new ItemStack(Items.SLIMEBALL)
                    ), 1
                );
                foreman.sendChatMessage("Fed tadpole - growth accelerated!");
            }
        }
    }

    private void checkForPredators() {
        AABB searchBox = careArea != null ?
            new AABB(careArea).inflate(16.0) :
            foreman.getBoundingBox().inflate(16.0);

        List<Entity> entities = foreman.level().getEntities(foreman, searchBox);

        boolean hasPredator = entities.stream()
            .anyMatch(e -> e.getType().getDescription().getString()
                .toLowerCase().contains("axolotl"));

        if (hasPredator) {
            foreman.sendChatMessage("Warning: Predator near tadpoles!");
            // Could trigger combat action here
        }
    }

    private void checkGrowthProgress() {
        int matureCount = 0;
        int growingCount = 0;

        for (Tadpole tadpole : tadpoles) {
            if (!tadpole.isAlive()) {
                matureCount++; // Became a frog
            } else if (tadpole.getAge() >= 0) {
                matureCount++;
            } else {
                growingCount++;
            }
        }

        foreman.sendChatMessage(String.format(
            "Progress: %d grown, %d growing", matureCount, growingCount
        ));
    }

    private boolean hasSlimeballs() {
        return foreman.getInventory().contains(new ItemStack(Items.SLIMEBALL));
    }
}
```

### Action 4: Swamp Navigation

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.phys.Vec3;

public class SwampNavigationAction extends BaseAction {
    private BlockPos destination;
    private Boat boat;
    private boolean inBoat;
    private boolean reachedDestination;

    public SwampNavigationAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        destination = task.getBlockPosParameter("destination");

        if (destination == null) {
            result = ActionResult.failure("No destination specified");
            return;
        }

        // Check if we're in a swamp biome
        if (!isInSwamp()) {
            result = ActionResult.failure("Not in a mangrove swamp biome");
            return;
        }

        // Deploy boat if in water
        if (foreman.isInWater()) {
            deployBoat();
        }

        foreman.sendChatMessage("Navigating mangrove swamp...");
    }

    @Override
    protected void onTick() {
        // Check if reached destination
        double distance = foreman.distanceToSqr(
            destination.getX(), destination.getY(), destination.getZ()
        );

        if (distance < 9.0) {
            reachedDestination = true;
            result = ActionResult.success("Reached destination!");
            return;
        }

        // Navigate based on terrain
        if (foreman.isInWater() && !inBoat) {
            deployBoat();
        } else if (!foreman.isInWater() && inBoat) {
            exitBoat();
        }

        // Move towards destination
        if (inBoat && boat != null) {
            // Boat navigation
            Vec3 direction = new Vec3(
                destination.getX() - boat.getX(),
                0,
                destination.getZ() - boat.getZ()
            ).normalize();

            boat.setDeltaMovement(direction.scale(0.5));
        } else {
            // Normal navigation
            foreman.getNavigation().moveTo(
                destination.getX(), destination.getY(), destination.getZ(),
                1.0
            );
        }
    }

    @Override
    protected void onCancel() {
        if (inBoat && boat != null) {
            exitBoat();
        }
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Navigating to " + destination;
    }

    private boolean isInSwamp() {
        return foreman.level().getBiome(foreman.blockPosition())
            .is(BiomeTags.IS_MANGROVE);
    }

    private void deployBoat() {
        if (hasBoat()) {
            boat = new Boat(Boat.Type.MANGROVE, foreman.level(),
                foreman.getX(), foreman.getY(), foreman.getZ());
            foreman.level().addFreshEntity(boat);
            foreman.startRiding(boat);
            inBoat = true;
            foreman.sendChatMessage("Deployed mangrove boat");
        }
    }

    private void exitBoat() {
        if (boat != null) {
            foreman.stopRiding();
            inBoat = false;
            // Remove boat or leave it for later use
        }
    }

    private boolean hasBoat() {
        return foreman.getInventory().contains(
            new ItemStack(Items.MANGROVE_BOAT)
        );
    }
}
```

### Plugin Registration

```java
// Add to CoreActionsPlugin.java

@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {
    // ... existing registrations ...

    // Frog farming actions
    registry.register("breed_frogs",
        (foreman, task, ctx) -> new FrogBreedingAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("collect_froglights",
        (foreman, task, ctx) -> new FroglightCollectAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("care_tadpoles",
        (foreman, task, ctx) -> new TadpoleCareAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("navigate_swamp",
        (foreman, task, ctx) -> new SwampNavigationAction(foreman, task),
        priority, PLUGIN_ID);
}
```

### LLM Prompt Integration

```java
// Add to PromptBuilder.java

ACTIONS:
- breed_frogs: {"quantity": 2, "location": [x, y, z]} - Breed frogs at location
- collect_froglights: {"quantity": 16, "collectionPoint": [x, y, z]} - Collect froglights from magma cubes
- care_tadpoles: {"quantity": 4, "variant": "temperate|warm|cold", "area": [x, y, z]} - Care for tadpoles
- navigate_swamp: {"destination": [x, y, z]} - Navigate mangrove swamp

EXAMPLES:

Input: "start a frog farm"
{"reasoning": "Setting up frog breeding operation", "plan": "Breed frogs and care for tadpoles", "tasks": [
    {"action": "breed_frogs", "parameters": {"quantity": 4, "location": [0, 64, 0]}},
    {"action": "care_tadpoles", "parameters": {"quantity": 4, "variant": "temperate", "area": [0, 64, 0]}}
]}

Input: "collect froglights"
{"reasoning": "Farming froglights from magma cubes", "plan": "Monitor frog and collect drops", "tasks": [
    {"action": "collect_froglights", "parameters": {"quantity": 32, "collectionPoint": [100, 64, 100]}}
]}
```

---

## Testing Strategy

### Unit Tests

1. **FrogBreedingActionTest**
   - Test slimeball detection
   - Test frog finding logic
   - Test breeding timeout
   - Test breeding success conditions

2. **FroglightCollectActionTest**
   - Test collection point navigation
   - Test froglight detection
   - Test inventory management

3. **TadpoleCareActionTest**
   - Test tadpole finding
   - Test feeding logic
   - Test predator detection

### Integration Tests

1. **Full Breeding Cycle**
   - Spawn frogs → Breed → Collect frogspawn → Raise tadpoles → Harvest frogs

2. **Froglight Farm**
   - Navigate to Nether → Position frogs → Collect froglights → Return

3. **Multi-Agent Coordination**
   - Spawn 4 Foremen → Assign roles → Coordinate breeding cycle

---

## Performance Considerations

1. **Entity Search Optimization**
   - Limit search radius for frogs
   - Cache frog locations
   - Use spatial partitioning for large farms

2. **Tick Management**
   - Reduce frequency of expensive checks
   - Use cooldown timers for breeding attempts
   - Batch entity operations

3. **Memory Management**
   - Limit number of tracked frogs
   - Clean up dead entities
   - Use weak references for entity caching

---

## Future Enhancements

1. **Advanced Breeding Programs**
   - Selective breeding for specific variants
   - Genetic tracking (mod addition)
   - Cross-breeding experiments

2. **Automated Froglight Farms**
   - Redstone-controlled magma cube spawners
   - Hopper systems
   - Storage sorting

3. **Frog Trading**
   - Trade frogs with villagers
   - Sell froglights for emeralds
   - Market pricing system

4. **Frog Competitions**
   - Frog jumping contests
   - Beauty competitions
   - Speed breeding challenges

---

## References

- [Frog - Minecraft Wiki](https://minecraft.fandom.com/wiki/Frog)
- [我的世界青蛙繁殖方法介绍](https://news.17173.com/z/mc/content/07062024/230105755.shtml)
- [Mob Menagerie: Frog](https://www.minecraft.net/zh-hans/article/mob-menagerie--frog)
- [Magma Cube - Minecraft Wiki](https://baike.sogou.com/v291180.htm)

---

## Conclusion

The frog farming AI system will provide MineWright with autonomous capabilities for breeding frogs, harvesting froglights, and managing tadpole development. This system integrates seamlessly with the existing action framework and supports multi-agent coordination for large-scale farming operations.

The modular design allows for incremental implementation and easy extension with new features as needed.
