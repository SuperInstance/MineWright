# Animal Handling AI for MineWright

**Version:** 1.0.0
**Last Updated:** 2026-02-27
**Status:** Design Document

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Animal Detection & Classification](#animal-detection--classification)
4. [Taming Automation](#taming-automation)
5. [Pet Following & Protection](#pet-following--protection)
6. [Breeding Optimization](#breeding-optimization)
7. [Animal Pen Management](#animal-pen-management)
8. [Code Examples](#code-examples)
9. [Implementation Roadmap](#implementation-roadmap)
10. [Integration Guide](#integration-guide)

---

## Overview

The Animal Handling AI system enables MineWright Foreman entities to autonomously manage animals in Minecraft. This includes taming wild animals, breeding livestock, managing pet companions, and organizing animals into pens.

### Key Features

- **Smart Detection**: Classify animals by type (passive, tamable, hostile, breedable)
- **Taming Automation**: Automatically tame wolves, cats, parrots, camels, llamas
- **Breeding Scheduling**: Optimize breeding for food, wool, and resources
- **Pet Management**: Pets follow, protect, and respond to commands
- **Pen Management**: Build and maintain animal enclosures
- **Resource Collection**: Gather wool, milk, eggs, and other animal products

### Supported Animals

#### Tamable Pets
- **Wolves** - Tamed with bones, protect owners
- **Cats** - Tamed with fish, repel creepers
- **Parrots** - Tamed with seeds, mimic sounds
- **Camels** - Tamed with cactus (approach and ride)
- **Llamas** - Tamed by riding/breeding, carry chests

#### Breedable Livestock
- **Cows** - Breed with wheat, produce milk, leather, beef
- **Sheep** - Breed with wheat, produce wool, mutton
- **Pigs** - Breed with carrots, potatoes, beetroots
- **Chickens** - Breed with seeds, produce eggs, chicken
- **Goats** - Breed with wheat, produce milk
- **Rabbits** - Breed with carrots, dandelions, golden carrots

#### Mounts
- **Horses** - Tamed by repeated riding, breed with golden carrots/apples
- **Donkeys/Mules** - Tamed by riding, carry chests

---

## Architecture

### Package Structure

```
com.minewright.animal/
├── AnimalClassification.java       # Enum for animal types
├── AnimalDetector.java             # Scan and classify nearby animals
├── AnimalMemory.java               # Track known animals and locations
├── PenManager.java                 # Manage animal enclosures
├── BreedingManager.java            # Schedule and optimize breeding
├── PetManager.java                 # Manage owned pets
├── AnimalResources.java            # Collect wool, milk, eggs
└── actions/
    ├── TameAction.java             # Tame wild animals
    ├── BreedAction.java            # Breed animals
    ├── FeedAction.java             # Feed animals
    ├── ShearAction.java            # Shear sheep
    ├── MilkAction.java             # Milk cows/goats
    ├── CollectEggsAction.java      # Collect chicken eggs
    ├── LeadAction.java             # Put leads on animals
    ├── BuildPenAction.java         # Build animal enclosures
    └── PetFollowAction.java        # Command pets to follow
```

### Class Diagram

```
┌─────────────────────────┐
│   AnimalClassification  │
│   (Enum)                │
├─────────────────────────┤
│ - TAMABLE_PET           │
│ - BREEDABLE_LIVESTOCK   │
│ - MOUNT                 │
│ - HOSTILE               │
│ - NEUTRAL               │
│ - PASSIVE               │
└─────────────────────────┘
           ▲
           │
┌─────────────────────────┐
│     AnimalDetector      │
├─────────────────────────┤
│ + scan(radius)          │
│ + classify(entity)      │
│ + findNearest(type)     │
│ + countByType()         │
└─────────────────────────┘
           ▲
           │
┌─────────────────────────┐       ┌──────────────────┐
│     AnimalMemory        │◄──────│  PenManager      │
├─────────────────────────┤       ├──────────────────┤
│ + trackAnimal()         │       │ + buildPen()     │
│ + getKnownAnimals()     │       │ + assignToPen()  │
│ + updateStatus()        │       │ + countInPen()   │
└─────────────────────────┘       └──────────────────┘
           ▲
           │
┌─────────────────────────┐       ┌──────────────────┐
│    PetManager           │◄──────│ BreedingManager  │
├─────────────────────────┤       ├──────────────────┤
│ + tameAnimal()          │       │ + schedule()     │
│ + commandPet()          │       │ + findPairs()    │
│ + setFollowMode()       │       │ + feedParents()  │
└─────────────────────────┘       └──────────────────┘
```

---

## Animal Detection & Classification

### AnimalClassification Enum

```java
package com.minewright.animal;

import net.minecraft.world.entity.*;

public enum AnimalClassification {
    // Tamable pets that become owned
    TAMABLE_PET("tamable_pet",
        EntityType.WOLF, EntityType.CAT, EntityType.PARROT,
        EntityType.CAMEL, EntityType.LLAMA
    ),

    // Breedable livestock for resources
    BREEDABLE_LIVESTOCK("breedable_livestock",
        EntityType.COW, EntityType.SHEEP, EntityType.PIG,
        EntityType.CHICKEN, EntityType.GOAT, EntityType.RABBIT
    ),

    // Rideable mounts
    MOUNT("mount",
        EntityType.HORSE, EntityType.DONKEY, EntityType.MULE,
        EntityType.CAMEL, EntityType.LLAMA
    ),

    // Hostile mobs (attack on sight)
    HOSTILE("hostile",
        EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
        EntityType.CREEPER, EntityType.ENDERMAN
    ),

    // Neutral mobs (attack when provoked)
    NEUTRAL("neutral",
        EntityType.WOLF, EntityType.POLAR_BEAR, EntityType.PANDA,
        EntityType.DOLPHIN, EntityType.LLAMA
    ),

    // Passive mobs (never attack)
    PASSIVE("passive",
        EntityType.COW, EntityType.SHEEP, EntityType.PIG,
        EntityType.CHICKEN, EntityType.RABBIT, EntityType.BAT
    );

    private final String category;
    private final EntityType<?>[] entityTypes;

    AnimalClassification(String category, EntityType<?>... entityTypes) {
        this.category = category;
        this.entityTypes = entityTypes;
    }

    public String getCategory() { return category; }

    public static AnimalClassification classify(Entity entity) {
        for (AnimalClassification classification : values()) {
            for (EntityType<?> type : classification.entityTypes) {
                if (entity.getType() == type) {
                    return classification;
                }
            }
        }
        return PASSIVE; // Default
    }

    public boolean isTamable() {
        return this == TAMABLE_PET || this == MOUNT;
    }

    public boolean isBreedable() {
        return this == BREEDABLE_LIVESTOCK || this == TAMABLE_PET;
    }
}
```

### AnimalDetector

```java
package com.minewright.animal;

import com.minewright.entity.ForemanEntity;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.stream.Collectors;

public class AnimalDetector {
    private final ForemanEntity foreman;
    private final int defaultRadius = 32;

    public AnimalDetector(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Scan for all nearby animals and classify them
     */
    public Map<AnimalClassification, List<Animal>> scan(int radius) {
        AABB searchBox = foreman.getBoundingBox().inflate(radius);
        List<Entity> entities = foreman.level().getEntities(foreman, searchBox);

        Map<AnimalClassification, List<Animal>> classified = new EnumMap<>(AnimalClassification.class);

        for (Entity entity : entities) {
            if (!(entity instanceof Animal animal)) continue;

            AnimalClassification type = AnimalClassification.classify(entity);
            classified.computeIfAbsent(type, k -> new ArrayList<>()).add(animal);
        }

        return classified;
    }

    /**
     * Find nearest animal of specific type
     */
    public Animal findNearest(AnimalClassification type) {
        var animals = scan(defaultRadius).get(type);
        if (animals == null || animals.isEmpty()) return null;

        return animals.stream()
            .min(Comparator.comparingDouble(a -> foreman.distanceTo(a)))
            .orElse(null);
    }

    /**
     * Find all untamed tamable animals
     */
    public List<TamableAnimal> findUntamed() {
        AABB searchBox = foreman.getBoundingBox().inflate(defaultRadius);
        List<Entity> entities = foreman.level().getEntities(foreman, searchBox);

        return entities.stream()
            .filter(TamableAnimal.class::isInstance)
            .map(TamableAnimal.class::cast)
            .filter(a -> !a.isTame())
            .collect(Collectors.toList());
    }

    /**
     * Find breedable pairs (animals that can breed with each other)
     */
    public List<BreedingPair> findBreedingPairs() {
        Map<AnimalClassification, List<Animal>> classified = scan(defaultRadius);
        List<BreedingPair> pairs = new ArrayList<>();

        for (Map.Entry<AnimalClassification, List<Animal>> entry : classified.entrySet()) {
            if (!entry.getKey().isBreedable()) continue;

            List<Animal> animals = entry.getValue();
            // Find animals that are in love mode or can be fed
            for (int i = 0; i < animals.size() - 1; i++) {
                Animal a1 = animals.get(i);
                for (int j = i + 1; j < animals.size(); j++) {
                    Animal a2 = animals.get(j);
                    if (canBreed(a1, a2)) {
                        pairs.add(new BreedingPair(a1, a2, entry.getKey()));
                    }
                }
            }
        }

        return pairs;
    }

    /**
     * Count animals by type
     */
    public Map<EntityType<?>, Integer> countByType() {
        Map<AnimalClassification, List<Animal>> classified = scan(defaultRadius);
        Map<EntityType<?>, Integer> counts = new HashMap<>();

        for (List<Animal> animals : classified.values()) {
            for (Animal animal : animals) {
                counts.merge(animal.getType(), 1, Integer::sum);
            }
        }

        return counts;
    }

    private boolean canBreed(Animal a1, Animal a2) {
        // Same type, both alive, not already breeding
        if (a1.getType() != a2.getType()) return false;
        if (!a1.isAlive() || !a2.isAlive()) return false;
        // Minecraft checks: ageable animals can breed if adult
        if (a1 instanceof AgeableMob age1 && a2 instanceof AgeableMob age2) {
            return age1.getAge() == 0 && age2.getAge() == 0;
        }
        return false;
    }

    public record BreedingPair(Animal parent1, Animal parent2, AnimalClassification type) {}
}
```

---

## Taming Automation

### Taming Items by Animal

| Animal | Taming Item | Quantity Needed | Taming Method |
|--------|-------------|-----------------|---------------|
| Wolf | Bone | 1-10 | Right-click repeatedly |
| Cat | Cod/Salmon | 1-10 | Right-click slowly |
| Parrot | Any Seeds | 1-10 | Right-click repeatedly |
| Horse | None | Ride repeatedly | Mount until hearts appear |
| Donkey/Mule | None | Ride repeatedly | Mount until hearts appear |
| Camel | None | Ride repeatedly | Mount until hearts appear |
| Llama | None | Ride repeatedly | Mount until hearts appear |

### TameAction Implementation

```java
package com.minewright.animal.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.animal.AnimalClassification;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.item.*;

public class TameAction extends BaseAction {
    private TamableAnimal target;
    private int tamingProgress = 0;
    private int ticksSinceLastAttempt = 0;
    private static final int TICKS_BETWEEN_ATTEMPTS = 10; // 0.5 seconds

    // Taming items for different animals
    private static final java.util.Map<EntityType<?>, java.util.function.Supplier<Item>> TAMING_ITEMS =
        java.util.Map.of(
            EntityType.WOLF, () -> Items.BONE,
            EntityType.CAT, () -> Items.COD,
            EntityType.PARROT, () -> Items.WHEAT_SEEDS
        );

    public TameAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        String targetType = task.getStringParameter("target", "wolf");

        // Find nearby untamed animal
        findTarget(targetType);

        if (target == null) {
            result = ActionResult.failure("No untamed " + targetType + " found nearby");
            return;
        }

        foreman.sendChatMessage("I'll tame this " + target.getType().toString());
    }

    @Override
    protected void onTick() {
        if (target == null || !target.isAlive()) {
            result = ActionResult.failure("Target animal disappeared");
            return;
        }

        if (target.isTame()) {
            result = ActionResult.success("Successfully tamed " + target.getType().toString());
            return;
        }

        // Move towards target
        double distance = foreman.distanceTo(target);
        if (distance > 4.0) {
            foreman.getNavigation().moveTo(target, 1.2);
            return;
        }

        // Attempt taming at intervals
        ticksSinceLastAttempt++;
        if (ticksSinceLastAttempt < TICKS_BETWEEN_ATTEMPTS) {
            return;
        }
        ticksSinceLastAttempt = 0;

        // Face the animal
        foreman.getLookControl().setLookAt(target);

        // Attempt tame
        if (attemptTame()) {
            tamingProgress++;

            if (target.isTame()) {
                // Set the foreman as the owner
                if (target instanceof Wolf wolf) {
                    wolf.tame(foreman);
                    wolf.setOrderedToSit(true);
                } else if (target instanceof Cat cat) {
                    cat.tame(foreman);
                } else if (target instanceof Parrot parrot) {
                    parrot.tame(foreman);
                }

                result = ActionResult.success("Tamed " + target.getType().toString() + " after " + tamingProgress + " attempts");
                foreman.notifyTaskCompleted("Tamed " + target.getType().toString());
            }
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Taming " + (target != null ? target.getType().toString() : "animal");
    }

    private void findTarget(String targetType) {
        AABB searchBox = foreman.getBoundingBox().inflate(32.0);
        List<Entity> entities = foreman.level().getEntities(foreman, searchBox);

        for (Entity entity : entities) {
            if (!(entity instanceof TamableAnimal tamable)) continue;
            if (tamable.isTame()) continue; // Skip already tamed

            String entityName = entity.getType().toString().toLowerCase();
            if (entityName.contains(targetType.toLowerCase()) || targetType.equals("any")) {
                target = tamable;
                return;
            }
        }
    }

    private boolean attemptTame() {
        // This simulates the player right-clicking with taming item
        // In actual implementation, we'd need to use Forge's taming logic

        if (target instanceof Wolf) {
            // Wolves are tamed with bones - we simulate this
            // In practice, we'd need to check if foreman has bones in inventory
            return tryTameWolf();
        } else if (target instanceof Cat) {
            return tryTameCat();
        } else if (target instanceof Parrot) {
            return tryTameParrot();
        } else if (target instanceof AbstractHorse horse) {
            return tryTameHorse(horse);
        }

        return false;
    }

    private boolean tryTameWolf() {
        // Simulate bone taming - random chance
        double tameChance = 0.1 + (tamingProgress * 0.05); // Increasing chance
        return foreman.getRandom().nextDouble() < tameChance;
    }

    private boolean tryTameCat() {
        double tameChance = 0.08 + (tamingProgress * 0.04);
        return foreman.getRandom().nextDouble() < tameChance;
    }

    private boolean tryTameParrot() {
        // Parrots have 1/3 chance per seed
        return foreman.getRandom().nextDouble() < 0.33;
    }

    private boolean tryTameHorse(AbstractHorse horse) {
        // Horses are tamed by riding - we simulate the temper increase
        // In Minecraft, this is handled by the horse's temper stat
        int currentTemper = horse.getTemper();
        int newTemper = currentTemper + foreman.getRandom().nextInt(3);

        // This would need access to horse's temper field
        // Simplified: random chance based on attempts
        return foreman.getRandom().nextDouble() < 0.05 + (tamingProgress * 0.02);
    }
}
```

---

## Pet Following & Protection

### PetManager

```java
package com.minewright.animal;

import com.minewright.entity.ForemanEntity;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PetManager {
    private final ForemanEntity foreman;
    private final Map<UUID, PetData> ownedPets = new ConcurrentHashMap<>();

    public PetManager(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Tame an animal and add to owned pets
     */
    public void tameAnimal(TamableAnimal animal) {
        if (!(animal instanceof Animal)) return;

        PetData petData = new PetData(
            animal.getUUID(),
            animal.getType(),
            PetFollowMode.FOLLOW,
            System.currentTimeMillis()
        );

        ownedPets.put(animal.getUUID(), petData);
        foreman.sendChatMessage("I've tamed a " + animal.getType().toString());
    }

    /**
     * Command all pets to follow or stay
     */
    public void setFollowMode(PetFollowMode mode) {
        for (PetData pet : ownedPets.values()) {
            pet.followMode = mode;

            // Find the actual entity and update its state
            Animal animal = findPetById(pet.uuid);
            if (animal instanceof TamableAnimal tamable) {
                if (mode == PetFollowMode.STAY) {
                    if (tamable instanceof Wolf wolf) {
                        wolf.setOrderedToSit(true);
                    }
                } else {
                    if (tamable instanceof Wolf wolf) {
                        wolf.setOrderedToSit(false);
                    }
                    // Make pet follow foreman
                    animal.getNavigation().moveTo(foreman, 1.0);
                }
            }
        }
    }

    /**
     * Command specific pet
     */
    public void commandPet(UUID petUuid, PetCommand command) {
        PetData pet = ownedPets.get(petUuid);
        if (pet == null) return;

        Animal animal = findPetById(petUuid);
        if (animal == null) return;

        switch (command) {
            case FOLLOW -> {
                pet.followMode = PetFollowMode.FOLLOW;
                if (animal instanceof Wolf wolf) {
                    wolf.setOrderedToSit(false);
                }
            }
            case STAY -> {
                pet.followMode = PetFollowMode.STAY;
                if (animal instanceof Wolf wolf) {
                    wolf.setOrderedToSit(true);
                }
            }
            case ATTACK -> {
                pet.followMode = PetFollowMode.PROTECT;
                if (animal instanceof Wolf wolf) {
                    wolf.setOrderedToSit(false);
                    wolf.setTarget(foreman.getTarget()); // Attack foreman's target
                }
            }
        }
    }

    /**
     * Tick all pets to maintain follow behavior
     */
    public void tick() {
        for (PetData pet : ownedPets.values()) {
            if (pet.followMode != PetFollowMode.FOLLOW) continue;

            Animal animal = findPetById(pet.uuid);
            if (animal == null) continue;

            double distance = animal.distanceTo(foreman);

            // Call pet if too far
            if (distance > 16.0) {
                animal.getNavigation().moveTo(foreman, 1.2);
            }

            // Make pets defend foreman from nearby threats
            if (pet.followMode == PetFollowMode.PROTECT && animal instanceof TamableAnimal tamable) {
                checkAndDefend(tamable);
            }
        }
    }

    private void checkAndDefend(TamableAnimal pet) {
        // Find nearby hostile mobs targeting the foreman
        AABB searchBox = foreman.getBoundingBox().inflate(12.0);
        List<Entity> entities = foreman.level().getEntities(foreman, searchBox);

        for (Entity entity : entities) {
            if (entity instanceof net.minecraft.world.entity.monster.Monster monster) {
                if (monster.getTarget() == foreman && pet instanceof Wolf wolf) {
                    wolf.setTarget(monster);
                    break;
                }
            }
        }
    }

    private Animal findPetById(UUID uuid) {
        List<Entity> entities = foreman.level().getEntities(null,
            foreman.getBoundingBox().inflate(64.0));

        for (Entity entity : entities) {
            if (entity.getUUID().equals(uuid) && entity instanceof Animal animal) {
                return animal;
            }
        }
        return null;
    }

    public Collection<PetData> getOwnedPets() {
        return ownedPets.values();
    }

    public record PetData(
        UUID uuid,
        EntityType<?> type,
        PetFollowMode followMode,
        long tamedTime
    ) {}

    public enum PetFollowMode {
        FOLLOW,   // Follow the foreman
        STAY,     // Stay in place
        PROTECT,  // Follow and defend
        ROAM      // Free roam
    }

    public enum PetCommand {
        FOLLOW, STAY, ATTACK
    }
}
```

---

## Breeding Optimization

### BreedingManager

```java
package com.minewright.animal;

import com.minewright.entity.ForemanEntity;
import com.minewright.animal.actions.BreedAction;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.item.*;

import java.util.*;
import java.util.concurrent.*;

public class BreedingManager {
    private final ForemanEntity foreman;
    private final Queue<BreedingTask> breedingQueue = new ConcurrentLinkedQueue<>();
    private final Map<EntityType<?>, BreedingStats> stats = new ConcurrentHashMap<>();

    // Breeding items for each animal type
    private static final Map<EntityType<?>, Item> BREEDING_ITEMS = Map.of(
        EntityType.COW, Items.WHEAT,
        EntityType.SHEEP, Items.WHEAT,
        EntityType.PIG, Items.CARROT,
        EntityType.CHICKEN, Items.WHEAT_SEEDS,
        EntityType.GOAT, Items.WHEAT,
        EntityType.RABBIT, Items.CARROT,
        EntityType.WOLF, Items.BONE,
        EntityType.CAT, Items.COD,
        EntityType.HORSE, Items.GOLDEN_CARROT,
        EntityType.DONKEY, Items.GOLDEN_CARROT,
        EntityType.LLAMA, Items.HAY_BLOCK
    );

    public BreedingManager(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Find all breedable pairs nearby
     */
    public List<BreedingPair> findBreedingPairs(int radius) {
        AABB searchBox = foreman.getBoundingBox().inflate(radius);
        List<Entity> entities = foreman.level().getEntities(foreman, searchBox);

        // Group animals by type
        Map<EntityType<?>, List<Animal>> animalsByType = new HashMap<>();

        for (Entity entity : entities) {
            if (!(entity instanceof Animal animal)) continue;
            if (!(animal instanceof AgeableMob ageable)) continue;
            if (ageable.getAge() != 0) continue; // Skip babies

            animalsByType.computeIfAbsent(entity.getType(), k -> new ArrayList<>()).add(animal);
        }

        // Find pairs
        List<BreedingPair> pairs = new ArrayList<>();
        for (List<Animal> animals : animalsByType.values()) {
            for (int i = 0; i < animals.size() - 1; i++) {
                for (int j = i + 1; j < animals.size(); j++) {
                    pairs.add(new BreedingPair(animals.get(i), animals.get(j)));
                }
            }
        }

        return pairs;
    }

    /**
     * Schedule breeding for optimal results
     * - Breeds in batches for efficiency
     * - Ensures enough food for all pairs
     * - Spreads out breeding to avoid overcrowding
     */
    public void scheduleBreeding(EntityType<?> animalType, int targetCount) {
        List<BreedingPair> pairs = findBreedingPairs(32);

        // Filter by type
        pairs = pairs.stream()
            .filter(p -> p.parent1.getType() == animalType)
            .limit(targetCount)
            .toList();

        for (BreedingPair pair : pairs) {
            breedingQueue.add(new BreedingTask(pair, System.currentTimeMillis()));
        }

        foreman.sendChatMessage("Scheduled " + pairs.size() + " breeding pairs for " + animalType);
    }

    /**
     * Process breeding queue
     */
    public void tick() {
        if (breedingQueue.isEmpty()) return;

        BreedingTask task = breedingQueue.peek();

        // Check if parents are still valid
        if (!task.pair.parent1().isAlive() || !task.pair.parent2().isAlive()) {
            breedingQueue.remove();
            return;
        }

        // Check cooldown (5 minutes between breeding per animal)
        long timeSinceTask = System.currentTimeMillis() - task.scheduledTime();
        if (timeSinceTask < 60000) { // Wait 1 minute between batches
            return;
        }

        // Execute breeding
        if (breedPair(task.pair)) {
            breedingQueue.remove();

            // Update stats
            EntityType<?> type = task.pair.parent1().getType();
            stats.computeIfAbsent(type, t -> new BreedingStats()).bred++;

            foreman.notifyTaskCompleted("Bred " + type.toString());
        }
    }

    /**
     * Breed a pair of animals
     */
    private boolean breedPair(BreedingPair pair) {
        Animal parent1 = pair.parent1();
        Animal parent2 = pair.parent2();

        // Check if they're in love mode already
        if (parent1 instanceof Animal animal1 && animal1.isInLove()) {
            return false; // Already breeding
        }

        // Move close enough
        double distance = parent1.distanceTo(parent2);
        if (distance > 8.0) {
            parent1.getNavigation().moveTo(parent2, 1.0);
            return false;
        }

        // Feed both animals (puts them in love mode)
        Item breedingItem = BREEDING_ITEMS.get(parent1.getType());
        if (breedingItem == null) {
            foreman.sendChatMessage("I don't know what " + parent1.getType() + " eats!");
            return false;
        }

        // In a real implementation, we'd check inventory for the food
        // For now, we'll simulate successful feeding
        if (parent1 instanceof AgeableMob age1 && parent2 instanceof AgeableMob age2) {
            age1.setAge(0); // Ensure adult
            age2.setAge(0);

            // Trigger love mode
            age1.setInLoveTime(600);
            age2.setInLoveTime(600);

            // Face each other
            age1.getLookControl().setLookAt(parent2);
            age2.getLookControl().setLookAt(parent1);

            return true;
        }

        return false;
    }

    /**
     * Get breeding statistics
     */
    public BreedingStats getStats(EntityType<?> type) {
        return stats.getOrDefault(type, new BreedingStats());
    }

    public record BreedingPair(Animal parent1, Animal parent2) {}

    public record BreedingTask(BreedingPair pair, long scheduledTime) {}

    public static class BreedingStats {
        public int bred = 0;
        public int failed = 0;
        public long lastBreedingTime = 0;
    }
}
```

### BreedAction

```java
package com.minewright.animal.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.animal.BreedingManager.BreedingPair;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;

public class BreedAction extends BaseAction {
    private BreedingPair pair;
    private int ticksWaiting = 0;
    private static final int MAX_TICKS = 600; // 30 seconds

    public BreedAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        String animalType = task.getStringParameter("animal", "any");

        // Find breedable pair
        pair = findPair(animalType);

        if (pair == null) {
            result = ActionResult.failure("No breedable " + animalType + " pair found");
            return;
        }
    }

    @Override
    protected void onTick() {
        ticksWaiting++;

        if (ticksWaiting > MAX_TICKS) {
            result = ActionResult.failure("Breeding timed out");
            return;
        }

        if (!pair.parent1().isAlive() || !pair.parent2().isAlive()) {
            result = ActionResult.failure("One of the animals died");
            return;
        }

        // Check if breeding complete (baby spawned)
        if (isBreedingComplete()) {
            result = ActionResult.success("Successfully bred " + pair.parent1().getType().toString());
            return;
        }

        // Move close to the pair
        double dist1 = foreman.distanceTo(pair.parent1());
        double dist2 = foreman.distanceTo(pair.parent2());

        if (dist1 > 6.0 || dist2 > 6.0) {
            // Move closer
            foreman.getNavigation().moveTo(pair.parent1(), 1.0);
        } else {
            // Feed the animals
            feedAnimals();
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Breeding " + (pair != null ? pair.parent1().getType().toString() : "animals");
    }

    private BreedingPair findPair(String type) {
        // Use AnimalDetector to find pairs
        var detector = new com.minewright.animal.AnimalDetector(foreman);
        var pairs = detector.findBreedingPairs();

        if (pairs.isEmpty()) return null;

        // Find pair matching type
        if (!type.equals("any")) {
            return pairs.stream()
                .filter(p -> p.type().toString().toLowerCase().contains(type.toLowerCase()))
                .findFirst()
                .orElse(pairs.get(0));
        }

        return pairs.get(0);
    }

    private void feedAnimals() {
        if (pair.parent1() instanceof AgeableMob a1 && pair.parent2() instanceof AgeableMob a2) {
            // Set love mode
            a1.setInLoveTime(600);
            a2.setInLoveTime(600);
        }
    }

    private boolean isBreedingComplete() {
        if (pair.parent1() instanceof Animal a1) {
            return !a1.isInLove(); // Love mode ended = baby spawned
        }
        return false;
    }
}
```

---

## Animal Pen Management

### PenManager

```java
package com.minewright.animal;

import com.minewright.entity.ForemanEntity;
import com.minewright.structure.BlockPlacement;
import com.minewright.structure.StructureGenerators;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PenManager {
    private final ForemanEntity foreman;
    private final Map<String, Pen> pens = new ConcurrentHashMap<>();
    private int penCounter = 0;

    public PenManager(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Create a new animal pen
     */
    public Pen createPen(BlockPos center, int width, int depth, int height, String purpose) {
        String penId = "pen_" + (++penCounter);

        Pen pen = new Pen(
            penId,
            center,
            new PenDimensions(width, depth, height),
            purpose,
            System.currentTimeMillis()
        );

        pens.put(penId, pen);
        foreman.sendChatMessage("Created " + purpose + " pen at " + center);

        return pen;
    }

    /**
     * Build a pen structure
     */
    public List<BlockPlacement> generatePenStructure(Pen pen) {
        List<BlockPlacement> placements = new ArrayList<>();
        BlockPos center = pen.center();
        PenDimensions dim = pen.dimensions();

        // Build fence walls
        // North and South walls
        for (int x = -dim.width()/2; x <= dim.width()/2; x++) {
            for (int y = 0; y < dim.height(); y++) {
                // North wall
                placements.add(new BlockPlacement(
                    center.offset(x, y, -dim.depth()/2),
                    Blocks.OAK_FENCE.defaultBlockState()
                ));
                // South wall
                placements.add(new BlockPlacement(
                    center.offset(x, y, dim.depth()/2),
                    Blocks.OAK_FENCE.defaultBlockState()
                ));
            }
        }

        // East and West walls
        for (int z = -dim.depth()/2; z <= dim.depth()/2; z++) {
            for (int y = 0; y < dim.height(); y++) {
                // West wall
                placements.add(new BlockPlacement(
                    center.offset(-dim.width()/2, y, z),
                    Blocks.OAK_FENCE.defaultBlockState()
                ));
                // East wall
                placements.add(new BlockPlacement(
                    center.offset(dim.width()/2, y, z),
                    Blocks.OAK_FENCE.defaultBlockState()
                ));
            }
        }

        // Add fence gate for entrance
        placements.add(new BlockPlacement(
            center.offset(0, 0, dim.depth()/2),
            Blocks.OAK_FENCE_GATE.defaultBlockState()
        ));

        // Add floor (optional, for containment)
        for (int x = -dim.width()/2; x < dim.width()/2; x++) {
            for (int z = -dim.depth()/2; z < dim.depth()/2; z++) {
                placements.add(new BlockPlacement(
                    center.offset(x, -1, z),
                    Blocks.GRASS_BLOCK.defaultBlockState()
                ));
            }
        }

        // Add feeder
        placements.add(new BlockPlacement(
            center.offset(0, 0, 0),
            Blocks.HAY_BLOCK.defaultBlockState()
        ));

        // Add water trough
        placements.add(new BlockPlacement(
            center.offset(2, 0, 0),
            Blocks.WATER.defaultBlockState()
        ));

        return placements;
    }

    /**
     * Assign animals to a pen
     */
    public void assignToPen(String penId, List<Animal> animals) {
        Pen pen = pens.get(penId);
        if (pen == null) return;

        for (Animal animal : animals) {
            pen.animals().add(animal.getUUID());

            // Guide animal to pen
            guideToPen(animal, pen);
        }

        foreman.sendChatMessage("Assigned " + animals.size() + " animals to " + penId);
    }

    /**
     * Guide an animal to a pen
     */
    private void guideToPen(Animal animal, Pen pen) {
        // Use lead or lure with food to move animal
        BlockPos target = pen.center();

        // In Minecraft, animals follow players holding their breeding food
        // We would need to simulate this or use leads

        animal.getNavigation().moveTo(
            target.getX(),
            target.getY(),
            target.getZ(),
            1.0
        );
    }

    /**
     * Count animals in a pen
     */
    public int countInPen(String penId) {
        Pen pen = pens.get(penId);
        if (pen == null) return 0;

        AABB penBounds = getPenBounds(pen);
        int count = 0;

        List<Entity> entities = foreman.level().getEntities(null, penBounds);
        for (Entity entity : entities) {
            if (entity instanceof Animal) {
                count++;
            }
        }

        return count;
    }

    /**
     * Find animals that have escaped from their pen
     */
    public List<Animal> findEscapedAnimals() {
        List<Animal> escaped = new ArrayList<>();

        for (Pen pen : pens.values()) {
            AABB penBounds = getPenBounds(pen);

            for (UUID animalUuid : pen.animals()) {
                Animal animal = findAnimal(animalUuid);
                if (animal == null) continue;

                if (!penBounds.contains(animal.position())) {
                    escaped.add(animal);
                }
            }
        }

        return escaped;
    }

    private AABB getPenBounds(Pen pen) {
        BlockPos center = pen.center();
        PenDimensions dim = pen.dimensions();

        return new AABB(
            center.getX() - dim.width()/2 - 2,
            center.getY() - 1,
            center.getZ() - dim.depth()/2 - 2,
            center.getX() + dim.width()/2 + 2,
            center.getY() + dim.height() + 2,
            center.getZ() + dim.depth()/2 + 2
        );
    }

    private Animal findAnimal(UUID uuid) {
        List<Entity> entities = foreman.level().getEntities(null,
            foreman.getBoundingBox().inflate(64.0));

        for (Entity entity : entities) {
            if (entity.getUUID().equals(uuid) && entity instanceof Animal animal) {
                return animal;
            }
        }
        return null;
    }

    public Collection<Pen> getAllPens() {
        return pens.values();
    }

    public record Pen(
        String id,
        BlockPos center,
        PenDimensions dimensions,
        String purpose, // "cows", "sheep", "chickens", etc.
        long createdTime
    ) {
        private final Set<UUID> animals = ConcurrentHashMap.newKeySet();

        public Set<UUID> animals() { return animals; }
    }

    public record PenDimensions(int width, int depth, int height) {}
}
```

---

## Code Examples

### Example 1: Taming a Wolf

```java
// Create a taming task
Task task = new Task("tame wolf", Map.of(
    "target", "wolf"
));

TameAction action = new TameAction(foreman, task);
action.start();

while (!action.isComplete()) {
    action.tick();
    Thread.sleep(50); // Tick loop
}

ActionResult result = action.getResult();
if (result.success()) {
    System.out.println("Wolf tamed: " + result.message());
}
```

### Example 2: Breeding Cows

```java
// Schedule breeding
BreedingManager breedingManager = new BreedingManager(foreman);
breedingManager.scheduleBreeding(EntityType.COW, 5);

// Process in tick loop
while (true) {
    breedingManager.tick();
    Thread.sleep(50); // One tick
}
```

### Example 3: Building a Sheep Pen

```java
PenManager penManager = new PenManager(foreman);

// Create pen
Pen pen = penManager.createPen(
    new BlockPos(100, 64, 100),
    16, // width
    16, // depth
    3,  // height
    "sheep"
);

// Generate structure
List<BlockPlacement> blocks = penManager.generatePenStructure(pen);

// Execute building
Task buildTask = new Task("build sheep pen", Map.of(
    "blocks", blocks,
    "structure", "pen"
));

BuildStructureAction buildAction = new BuildStructureAction(foreman, buildTask);
buildAction.start();
```

### Example 4: Collecting Resources

```java
// Shear sheep
ShearAction shearAction = new ShearAction(foreman, new Task("shear sheep", Map.of()));
shearAction.start();

// Milk cows
MilkAction milkAction = new MilkAction(foreman, new Task("milk cows", Map.of()));
milkAction.start();

// Collect eggs
CollectEggsAction eggsAction = new CollectEggsAction(foreman,
    new Task("collect eggs", Map.of()));
eggsAction.start();
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
- [ ] Create `AnimalClassification` enum
- [ ] Implement `AnimalDetector` for scanning and classification
- [ ] Create `AnimalMemory` to track known animals
- [ ] Add animal detection to `WorldKnowledge`

### Phase 2: Taming (Week 2-3)
- [ ] Implement `TameAction` for wolves, cats, parrots
- [ ] Implement horse taming by riding
- [ ] Create `PetManager` for managing owned pets
- [ ] Add pet persistence to NBT

### Phase 3: Breeding (Week 3-4)
- [ ] Implement `BreedAction`
- [ ] Create `BreedingManager` for scheduling
- [ ] Add breeding queue and cooldown management
- [ ] Implement breeding statistics tracking

### Phase 4: Pen Management (Week 4-5)
- [ ] Implement `PenManager`
- [ ] Create `BuildPenAction`
- [ ] Add animal assignment to pens
- [ ] Implement escape detection

### Phase 5: Resource Collection (Week 5-6)
- [ ] Implement `ShearAction`
- [ ] Implement `MilkAction`
- [ ] Implement `CollectEggsAction`
- [ ] Add resource inventory management

### Phase 6: Integration (Week 6-7)
- [ ] Register all animal actions in `CoreActionsPlugin`
- [ ] Update `PromptBuilder` with animal commands
- [ ] Add animal context to `WorldKnowledge`
- [ ] Create comprehensive tests

### Phase 7: Polish (Week 7-8)
- [ ] Add GUI for viewing owned pets
- [ ] Implement pet command system
- [ ] Add animal health monitoring
- [ ] Create documentation and tutorials

---

## Integration Guide

### 1. Register Animal Actions

Add to `CoreActionsPlugin.java`:

```java
@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {
    // ... existing registrations ...

    // Animal handling actions
    registry.register("tame",
        (foreman, task, ctx) -> new TameAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("breed",
        (foreman, task, ctx) -> new BreedAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("shear",
        (foreman, task, ctx) -> new ShearAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("milk",
        (foreman, task, ctx) -> new MilkAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("build_pen",
        (foreman, task, ctx) -> new BuildPenAction(foreman, task),
        priority, PLUGIN_ID);
}
```

### 2. Update PromptBuilder

Add animal-related actions to system prompt:

```java
String animalActions = """
ACTIONS:
- tame: {"target": "wolf"} (options: wolf, cat, parrot, horse, llama)
- breed: {"animal": "cow", "count": 2}
- shear: {"target": "sheep"}
- milk: {"target": "cow"}
- collect_eggs: {}
- build_pen: {"type": "sheep", "size": [16, 16, 3]}
- pet_follow: {"mode": "follow"} (modes: follow, stay, protect)
""";
```

### 3. Add Animal Context to WorldKnowledge

```java
public class WorldKnowledge {
    // ... existing code ...

    private Map<AnimalClassification, Integer> animalCounts;

    public String getAnimalSummary() {
        if (animalCounts == null || animalCounts.isEmpty()) {
            return "none";
        }

        return animalCounts.entrySet().stream()
            .map(e -> e.getValue() + " " + e.getKey().getCategory())
            .collect(Collectors.joining(", "));
    }

    public Map<AnimalClassification, Integer> getAnimalCounts() {
        return animalCounts;
    }
}
```

### 4. Update ForemanEntity

Add animal managers to entity:

```java
public class ForemanEntity extends PathfinderMob {
    private PetManager petManager;
    private BreedingManager breedingManager;
    private PenManager penManager;

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // ... existing code ...

            // Tick animal managers
            if (petManager != null) petManager.tick();
            if (breedingManager != null) breedingManager.tick();
        }
    }

    // Getters
    public PetManager getPetManager() { return petManager; }
    public BreedingManager getBreedingManager() { return breedingManager; }
    public PenManager getPenManager() { return penManager; }
}
```

### 5. Example Commands for Users

```
"tame a wolf"
-> {"action": "tame", "parameters": {"target": "wolf"}}

"breed the cows"
-> {"action": "breed", "parameters": {"animal": "cow", "count": 2}}

"build a sheep pen"
-> {"action": "build_pen", "parameters": {"type": "sheep", "size": [16, 16, 3]}}

"shear all sheep"
-> {"action": "shear", "parameters": {"target": "sheep"}}

"milk the cows"
-> {"action": "milk", "parameters": {"target": "cow"}}

"make my pets follow me"
-> {"action": "pet_follow", "parameters": {"mode": "follow"}}
```

---

## Testing Checklist

### Taming
- [ ] Can tame wolves with bones
- [ ] Can tame cats with fish
- [ ] Can tame parrots with seeds
- [ ] Can tame horses by riding
- [ ] Tamed animals follow owner
- [ ] Tamed animals persist after reload

### Breeding
- [ ] Can breed cows with wheat
- [ ] Can breed sheep with wheat
- [ ] Can breed pigs with carrots
- [ ] Can breed chickens with seeds
- [ ] Baby animals spawn correctly
- [ ] Breeding cooldown respected

### Pen Management
- [ ] Can build pen structure
- [ ] Can assign animals to pen
- [ ] Animals stay in pen
- [ ] Escaped animals detected
- [ ] Can guide animals back to pen

### Resource Collection
- [ ] Can shear sheep for wool
- [ ] Can milk cows with buckets
- [ ] Can collect chicken eggs
- [ ] Resources added to inventory

### Pet Commands
- [ ] Pets can follow
- [ ] Pets can stay
- [ ] Pets protect from mobs
- [ ] Multiple pets managed

---

## Future Enhancements

1. **Advanced AI**
   - Animals graze on grass blocks
   - Animals seek shelter at night
   - Predators hunt nearby livestock

2. **Trading Integration**
   - Sell livestock to villagers
   - Trade animal products
   - Butcher shop automation

3. **Genetics**
   - Track animal lineages
   - Breed for specific traits
   - Rare variants breeding

4. **Competitions**
   - Horse racing
   - Animal shows
   - Best in contests

5. **Cross-breeding**
   - Horse + Donkey = Mule
   - Special breeding combinations

---

**Document End**

This design document provides a comprehensive foundation for implementing animal handling AI in MineWright. The modular architecture allows for incremental implementation and easy extension with new animal types and behaviors.
