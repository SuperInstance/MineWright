# Animal Handling AI - Quick Start Guide

## Overview

This guide provides quick reference examples for implementing and using the Animal Handling AI system in MineWright.

## Setup

### 1. Add Dependencies

The animal handling system integrates with existing MineWright components. No additional dependencies required.

### 2. Register Actions

The actions are auto-registered via the `CoreActionsPlugin`. Ensure you have the latest version.

### 3. Initialize Managers

Add to `ForemanEntity` constructor:

```java
public ForemanEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
    super(entityType, level);

    // Existing initialization...
    this.memory = new ForemanMemory(this);
    this.actionExecutor = new ActionExecutor(this);

    // Add animal managers
    this.petManager = new PetManager(this);
    this.breedingManager = new BreedingManager(this);
    this.penManager = new PenManager(this);
}
```

## Common Commands

### Taming

```
User command: "tame a wolf"
LLLM Response: {"action": "tame", "parameters": {"target": "wolf"}}
```

```
User command: "tame all the cats nearby"
LLM Response: {"action": "tame", "parameters": {"target": "cat", "all": true}}
```

### Breeding

```
User command: "breed the cows"
LLM Response: {"action": "breed", "parameters": {"animal": "cow", "count": 2}}
```

```
User command: "start a chicken farm"
LLM Response: {"action": "breed", "parameters": {"animal": "chicken", "count": 4}},
               {"action": "build_pen", "parameters": {"type": "chicken", "size": [10, 10, 3]}}
```

### Resource Collection

```
User command: "shear the sheep"
LLM Response: {"action": "shear", "parameters": {"target": "sheep"}}
```

```
User command: "collect eggs and milk"
LLM Response: {"action": "collect_eggs", "parameters": {}},
               {"action": "milk", "parameters": {"target": "cow"}}
```

### Pen Management

```
User command: "build a sheep pen"
LLM Response: {"action": "build_pen", "parameters": {"type": "sheep", "size": [16, 16, 3]}}
```

```
User command: "put all cows in the pen"
LLM Response: {"action": "assign_pen", "parameters": {"pen": "pen_1", "animals": "cow"}}
```

## API Reference

### AnimalDetector

```java
// Create detector
AnimalDetector detector = new AnimalDetector(foreman);

// Find nearest tamable animal
Animal wolf = detector.findNearest(AnimalClassification.TAMABLE_PET);

// Find all breedable pairs
List<BreedingPair> pairs = detector.findBreedingPairs();

// Count animals by type
Map<EntityType<?>, Integer> counts = detector.countByType();
```

### PetManager

```java
PetManager petManager = foreman.getPetManager();

// Tame an animal
petManager.tameAnimal(wolf);

// Command all pets
petManager.setFollowMode(PetFollowMode.FOLLOW);

// Command specific pet
petManager.commandPet(wolf.getUUID(), PetCommand.STAY);

// Get all owned pets
Collection<PetData> pets = petManager.getOwnedPets();
```

### BreedingManager

```java
BreedingManager breedingManager = foreman.getBreedingManager();

// Schedule breeding
breedingManager.scheduleBreeding(EntityType.COW, 5);

// Get statistics
BreedingStats stats = breedingManager.getStats(EntityType.COW);

// Process in tick loop
breedingManager.tick();
```

### PenManager

```java
PenManager penManager = foreman.getPenManager();

// Create a pen
Pen pen = penManager.createPen(centerPos, 16, 16, 3, "sheep");

// Generate structure
List<BlockPlacement> blocks = penManager.generatePenStructure(pen);

// Assign animals
penManager.assignToPen(pen.id(), List.of(sheep1, sheep2));

// Count in pen
int count = penManager.countInPen(pen.id());

// Find escaped animals
List<Animal> escaped = penManager.findEscapedAnimals();
```

## Testing Commands

### In-Game Commands

```
/foreman spawn Farmer
/foreman tell Farmer "tame a wolf"
/foreman tell Farmer "breed the cows"
/foreman tell Farmer "build a chicken pen"
/foreman tell Farmer "shear all sheep"
```

### Debug Commands

```java
// List all nearby animals
/foreman execute AnimalDetector nearby

// Show breeding stats
/foreman execute BreedingManager stats

// List pens
/foreman execute PenManager list

// Show owned pets
/foreman execute PetManager list
```

## Troubleshooting

### Animals Not Taming

1. Ensure foreman has correct taming items in inventory
2. Check distance to animal (must be within 4 blocks)
3. Verify animal is not already tamed
4. Check animal classification

### Breeding Not Working

1. Ensure both animals are adults (not babies)
2. Check breeding cooldown (5 minutes)
3. Verify foreman has breeding food
4. Ensure animals are of same type

### Animals Escaping Pens

1. Check pen wall height (minimum 2 blocks)
2. Verify all gates are closed
3. Check for blocks animals can jump on
4. Ensure pen has solid floor

## Performance Tips

1. **Limit Scan Radius**: Use smaller radii (16-32 blocks) for animal detection
2. **Batch Breeding**: Breed multiple pairs at once for efficiency
3. **Pen Spacing**: Keep pens at least 10 blocks apart
4. **Resource Caching**: Cache animal lists between ticks

## Next Steps

1. Read full [ANIMAL_HANDLING.md](ANIMAL_HANDLING.md) for detailed design
2. Review implementation examples in `com.minewright.animal`
3. Test with simple commands first (taming, breeding)
4. Gradually add complex features (pen management, automation)

## Support

For issues or questions:
- Check the main design document
- Review code examples in `docs/examples/`
- Test with debug commands enabled
