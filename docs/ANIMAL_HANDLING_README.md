# Animal Handling AI for MineWright - Complete Documentation

## Quick Links

| Document | Description | File |
|----------|-------------|------|
| **Main Design** | Complete system design with all features | [ANIMAL_HANDLING.md](ANIMAL_HANDLING.md) |
| **Quick Start** | Get started fast with common commands | [ANIMAL_HANDLING_QUICK_START.md](ANIMAL_HANDLING_QUICK_START.md) |
| **Architecture** | System architecture and flow diagrams | [ANIMAL_HANDLING_ARCHITECTURE.md](ANIMAL_HANDLING_ARCHITECTURE.md) |
| **Test Plan** | Comprehensive testing strategy | [ANIMAL_HANDLING_TEST_PLAN.md](ANIMAL_HANDLING_TEST_PLAN.md) |
| **Summary** | Executive summary and roadmap | [ANIMAL_HANDLING_SUMMARY.md](ANIMAL_HANDLING_SUMMARY.md) |

## What is Animal Handling AI?

The Animal Handling AI system enables MineWright Foreman entities to autonomously manage animals in Minecraft. Players can use natural language commands to have AI agents:

- **Tame** wild animals (wolves, cats, parrots, horses)
- **Breed** livestock for resources (cows, sheep, pigs, chickens)
- **Build** automatic animal pens with feeders and water
- **Collect** animal products (wool, milk, eggs)
- **Command** pets to follow, stay, or protect

## Features Overview

### 1. Animal Detection & Classification
- Scan nearby animals within configurable radius
- Classify by behavior (tamable, breedable, mount, hostile, neutral, passive)
- Find nearest animal of specific type
- Identify breedable pairs automatically

### 2. Taming Automation
| Animal | Taming Item | Method |
|--------|-------------|--------|
| Wolf | Bone | Right-click repeatedly |
| Cat | Fish (Cod/Salmon) | Right-click slowly |
| Parrot | Seeds | Right-click repeatedly |
| Horse | None | Ride until hearts appear |
| Llama | None | Ride until hearts appear |

### 3. Breeding Optimization
| Animal | Breeding Food | Cooldown | Products |
|--------|---------------|----------|----------|
| Cow | Wheat | 5 minutes | Milk, Beef, Leather |
| Sheep | Wheat | 5 minutes | Wool, Mutton |
| Pig | Carrot | 5 minutes | Pork |
| Chicken | Seeds | 5 minutes | Eggs, Chicken |
| Goat | Wheat | 5 minutes | Milk |

### 4. Pen Management
- Automatic pen construction with fences and gates
- Configurable dimensions (width, depth, height)
- Built-in feeders (hay blocks) and water troughs
- Animal tracking by UUID
- Escape detection and recovery

### 5. Pet Commands
- **Follow** - Pets follow the foreman
- **Stay** - Pets stay in place (sit)
- **Protect** - Pets defend against hostile mobs
- **Roam** - Free roam within area

## Getting Started

### Step 1: Basic Commands

```
/foreman spawn Rancher
/foreman tell Rancher "tame a wolf"
/foreman tell Rancher "breed the cows"
/foreman tell Rancher "shear the sheep"
```

### Step 2: Build a Farm

```
/foreman tell Rancher "build a chicken coop"
/foreman tell Rancher "breed 5 chickens"
/foreman tell Rancher "collect eggs"
```

### Step 3: Pet Management

```
/foreman tell Rancher "tame 3 wolves"
/foreman tell Rancher "make my pets protect me"
```

## Example Implementations

The `docs/examples/` directory contains ready-to-use implementations:

- **AnimalClassification.java** - Enum for categorizing animals
- **ShearAction.java** - Shear sheep for wool
- **BuildPenAction.java** - Build animal enclosures
- **PromptBuilderWithAnimals.java** - LLM integration

## Architecture Overview

```
ForemanEntity
    ├── PetManager (tame, command pets)
    ├── BreedingManager (schedule breeding)
    ├── PenManager (build and manage pens)
    └── AnimalDetector (scan and classify)
            └── AnimalClassification (enum)
```

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
- [x] AnimalClassification enum
- [x] AnimalDetector for scanning
- [x] AnimalMemory for tracking
- [ ] Integration with WorldKnowledge

### Phase 2: Taming (Week 2-3)
- [x] TameAction for wolves/cats/parrots
- [x] PetManager for owned pets
- [ ] Horse taming by riding
- [ ] NBT persistence

### Phase 3: Breeding (Week 3-4)
- [x] BreedAction implementation
- [x] BreedingManager scheduler
- [ ] Cooldown management
- [ ] Statistics tracking

### Phase 4: Pen Management (Week 4-5)
- [x] PenManager core
- [x] BuildPenAction
- [ ] Animal assignment
- [ ] Escape detection

### Phase 5: Resources (Week 5-6)
- [x] ShearAction
- [ ] MilkAction
- [ ] CollectEggsAction
- [ ] Inventory integration

### Phase 6: Integration (Week 6-7)
- [ ] Register all actions in CoreActionsPlugin
- [ ] Update PromptBuilder
- [ ] Add animal context to WorldKnowledge
- [ ] Comprehensive testing

### Phase 7: Polish (Week 7-8)
- [ ] Pet GUI
- [ ] Command system UI
- [ ] Health monitoring
- [ ] Final documentation

## Code Integration

### 1. Register Actions

Add to `CoreActionsPlugin.java`:

```java
registry.register("tame", (f, t, c) -> new TameAction(f, t), priority, "core");
registry.register("breed", (f, t, c) -> new BreedAction(f, t), priority, "core");
registry.register("shear", (f, t, c) -> new ShearAction(f, t), priority, "core");
registry.register("milk", (f, t, c) -> new MilkAction(f, t), priority, "core");
registry.register("build_pen", (f, t, c) -> new BuildPenAction(f, t), priority, "core");
registry.register("pet_follow", (f, t, c) -> new PetFollowAction(f, t), priority, "core");
```

### 2. Add to ForemanEntity

```java
public class ForemanEntity extends PathfinderMob {
    private PetManager petManager;
    private BreedingManager breedingManager;
    private PenManager penManager;

    public ForemanEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.petManager = new PetManager(this);
        this.breedingManager = new BreedingManager(this);
        this.penManager = new PenManager(this);
    }

    @Override
    public void tick() {
        super.tick();
        petManager.tick();
        breedingManager.tick();
    }
}
```

### 3. Update PromptBuilder

Add animal actions to system prompt:

```java
String animalActions = """
ACTIONS FOR ANIMAL HANDLING:
- tame: {"target": "wolf"}
- breed: {"animal": "cow", "count": 2}
- shear: {"target": "sheep"}
- milk: {"target": "cow"}
- collect_eggs: {}
- build_pen: {"type": "sheep", "size": [16, 16, 3]}
- pet_follow: {"mode": "follow"}
""";
```

## Testing

### Run Unit Tests

```bash
./gradlew test --tests "*Animal*Test"
```

### Manual Testing

Use these in-game commands:

```
/foreman spawn Rancher
/foreman tell Rancher "tame a wolf"
/foreman tell Rancher "breed 3 cows"
/foreman tell Rancher "build a sheep pen"
/foreman tell Rancher "shear all sheep"
```

## Configuration

Add to `config/steve-common.toml`:

```toml
[animal]
scanRadius = 32
maxPets = 10
maxBreedingQueue = 20
maxAnimalsPerPen = 20
breedingCooldownMinutes = 5
```

## Performance Considerations

1. **Sparse Scanning**: Scan every 100 ticks instead of every tick
2. **Chunk-Based Caching**: Cache animal positions by chunk
3. **Distance-Based Priority**: Process closer animals first
4. **Batch Processing**: Handle multiple animals per tick
5. **Memory Management**: Use UUIDs instead of entity references

## Troubleshooting

### Animals Not Taming
- Ensure foreman has correct taming items
- Check distance (< 4 blocks)
- Verify animal not already tamed
- Check animal classification

### Breeding Not Working
- Ensure both animals are adults
- Check breeding cooldown (5 minutes)
- Verify foreman has breeding food
- Ensure animals are same type

### Animals Escaping Pens
- Check wall height (minimum 2 blocks)
- Verify all gates are closed
- Check for blocks animals can jump on
- Ensure pen has solid floor

## API Reference

### AnimalDetector

```java
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

## Future Enhancements

1. **Advanced AI** - Animals graze, seek shelter, predators hunt
2. **Trading** - Sell livestock to villagers
3. **Genetics** - Track lineages, breed for traits
4. **Competitions** - Horse racing, animal shows
5. **Cross-breeding** - Horse + Donkey = Mule

## Contributing

When adding new animal types:

1. Add to `AnimalClassification` enum
2. Update breeding food mapping
3. Add taming logic if applicable
4. Update PromptBuilder with new commands
5. Add tests for new animal type
6. Update documentation

## Support

For issues or questions:
- Review main design document: [ANIMAL_HANDLING.md](ANIMAL_HANDLING.md)
- Check example implementations: `docs/examples/`
- Run tests: `./gradlew test --tests "*Animal*Test"`
- Check architecture diagrams: [ANIMAL_HANDLING_ARCHITECTURE.md](ANIMAL_HANDLING_ARCHITECTURE.md)

## License

This is part of the MineWright mod project.

---

**Version:** 1.0.0
**Last Updated:** 2026-02-27
**Status:** Design Document - Ready for Implementation
