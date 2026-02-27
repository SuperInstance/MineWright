# Animal Handling AI for MineWright - Summary

## Document Overview

This document provides a complete animal handling AI system for the MineWright Minecraft mod (Forge 1.20.1). The system enables AI-controlled Foreman entities to autonomously manage animals including taming, breeding, pen management, and resource collection.

## Key Documents

| Document | Purpose |
|----------|---------|
| `ANIMAL_HANDLING.md` | Main design document with full implementation details |
| `ANIMAL_HANDLING_QUICK_START.md` | Quick reference guide for common tasks |
| `ANIMAL_HANDLING_ARCHITECTURE.md` | System architecture and flow diagrams |
| `examples/` | Ready-to-use implementation examples |

## Features Summary

### 1. Animal Detection & Classification
- Scan nearby animals within configurable radius
- Classify by type (tamable, breedable, mount, hostile, neutral, passive)
- Find nearest animal of specific type
- Count animals by type
- Find breedable pairs

### 2. Taming Automation
- Tame wolves with bones
- Tame cats with fish
- Tame parrots with seeds
- Tame horses by riding
- Persistent ownership tracking
- Pet command system (follow, stay, protect)

### 3. Breeding Optimization
- Automatic pair finding
- Breeding queue management
- Cooldown tracking (5 minutes per animal)
- Batch breeding for efficiency
- Statistics tracking
- Food requirement checking

### 4. Pen Management
- Build fenced enclosures automatically
- Configure size (width, depth, height)
- Add gates, feeders, water troughs
- Assign animals to pens
- Track animals by UUID
- Detect escaped animals
- Guide animals back to pens

### 5. Resource Collection
- Shear sheep for wool
- Milk cows with buckets
- Collect chicken eggs
- Inventory integration
- Automatic resource gathering

## Supported Animals

### Tamable Pets
- **Wolves** - Tamed with bones, protect owners, attack on command
- **Cats** - Tamed with fish, repel creepers, gift items
- **Parrots** - Tamed with seeds, dance to music, mimic sounds
- **Horses** - Tamed by riding, breedable, rideable
- **Donkeys/Mules** - Tamed by riding, can carry chests
- **Camels** - Tamed by riding, two-seater mount
- **Llamas** - Tamed by riding, can carry chests, spit at enemies

### Breedable Livestock
- **Cows** - Wheat, produce milk/beef/leather
- **Sheep** - Wheat, produce wool/mutton
- **Pigs** - Carrots, produce pork
- **Chickens** - Seeds, produce eggs/chicken
- **Goats** - Wheat, produce milk
- **Rabbits** - Carrots, produce rabbit meat/hide

## Quick Start Commands

### In-Game Commands
```
/foreman spawn Rancher
/foreman tell Rancher "tame a wolf"
/foreman tell Rancher "breed 5 cows"
/foreman tell Rancher "build a sheep pen"
/foreman tell Rancher "shear all sheep"
/foreman tell Rancher "make my pets follow me"
```

### Natural Language Commands
```
"tame a wolf"
"breed the cows"
"build a chicken coop"
"shear the sheep"
"collect eggs"
"milk the cows"
"make my pets protect me"
"put all animals in the pen"
```

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
- [x] AnimalClassification enum
- [x] AnimalDetector for scanning
- [x] AnimalMemory for tracking
- [ ] Add to WorldKnowledge

### Phase 2: Taming (Week 2-3)
- [x] TameAction for wolves/cats/parrots
- [x] PetManager for owned pets
- [ ] Horse taming by riding
- [ ] Pet persistence to NBT

### Phase 3: Breeding (Week 3-4)
- [x] BreedAction
- [x] BreedingManager for scheduling
- [ ] Cooldown management
- [ ] Statistics tracking

### Phase 4: Pen Management (Week 4-5)
- [x] PenManager
- [x] BuildPenAction
- [ ] Animal assignment
- [ ] Escape detection

### Phase 5: Resources (Week 5-6)
- [x] ShearAction
- [ ] MilkAction
- [ ] CollectEggsAction
- [ ] Inventory management

### Phase 6: Integration (Week 6-7)
- [ ] Register all actions
- [ ] Update PromptBuilder
- [ ] Add animal context
- [ ] Create tests

### Phase 7: Polish (Week 7-8)
- [ ] Pet GUI
- [ ] Command system
- [ ] Health monitoring
- [ ] Documentation

## Code Examples Location

All example implementations are in `docs/examples/`:

- `AnimalClassification.java` - Classification enum
- `AnimalDetector.java` - Scanning and classification
- `TameAction.java` - Taming implementation
- `BreedAction.java` - Breeding implementation
- `ShearAction.java` - Shearing sheep
- `MilkAction.java` - Milking cows
- `BuildPenAction.java` - Building pens
- `PetManager.java` - Managing owned pets
- `BreedingManager.java` - Breeding scheduler
- `PenManager.java` - Pen management
- `PromptBuilderWithAnimals.java` - LLM integration

## Integration Steps

### 1. Add Package
```
src/main/java/com/minewright/animal/
```

### 2. Register Actions
Add to `CoreActionsPlugin.java`:
```java
registry.register("tame", (f, t, c) -> new TameAction(f, t), priority, "core");
registry.register("breed", (f, t, c) -> new BreedAction(f, t), priority, "core");
registry.register("shear", (f, t, c) -> new ShearAction(f, t), priority, "core");
registry.register("build_pen", (f, t, c) -> new BuildPenAction(f, t), priority, "core");
```

### 3. Update ForemanEntity
```java
private PetManager petManager;
private BreedingManager breedingManager;
private PenManager penManager;

@Override
public void tick() {
    petManager.tick();
    breedingManager.tick();
}
```

### 4. Update PromptBuilder
Add animal actions to system prompt and animal context to user prompt.

## Testing Checklist

### Unit Tests
- [ ] AnimalClassification.classify()
- [ ] AnimalDetector.scan()
- [ ] AnimalDetector.findNearest()
- [ ] PetManager.tameAnimal()
- [ ] BreedingManager.scheduleBreeding()
- [ ] PenManager.createPen()

### Integration Tests
- [ ] Tame wolf with bones
- [ ] Breed cows with wheat
- [ ] Build sheep pen
- [ ] Shear sheep for wool
- [ ] Milk cows
- [ ] Collect chicken eggs
- [ ] Pet follow/stay/protect

### Performance Tests
- [ ] Scan 100 animals
- [ ] Track 50 pets
- [ ] Manage 10 pens
- [ ] Process breeding queue

## Configuration

### Config Options (minewright-common.toml)
```toml
[animal]
scanRadius = 32
maxPets = 10
maxBreedingQueue = 20
maxAnimalsPerPen = 20
breedingCooldownMinutes = 5
```

## Performance Considerations

1. **Sparse Scanning**: Scan every 100 ticks, not every tick
2. **Chunk-Based Caching**: Cache animal positions by chunk
3. **Distance-Based Priority**: Prioritize closer animals
4. **Batch Processing**: Process multiple animals per tick
5. **Memory Management**: Use UUIDs instead of entity references

## Future Enhancements

1. **Advanced AI**: Animals graze, seek shelter, predators hunt
2. **Trading**: Sell livestock to villagers
3. **Genetics**: Track lineages, breed for traits
4. **Competitions**: Horse racing, animal shows
5. **Cross-breeding**: Horse + Donkey = Mule

## Troubleshooting

### Animals Not Taming
- Ensure foreman has taming items
- Check distance (< 4 blocks)
- Verify animal not already tamed

### Breeding Not Working
- Ensure both animals are adults
- Check breeding cooldown
- Verify food availability
- Same animal type

### Animals Escaping Pens
- Check wall height (min 2 blocks)
- Verify gates closed
- Check for jump blocks
- Ensure solid floor

## Support

For implementation help:
1. Review main design document (ANIMAL_HANDLING.md)
2. Check example implementations (docs/examples/)
3. Test with debug commands
4. Review architecture diagrams (ANIMAL_HANDLING_ARCHITECTURE.md)

## License

This design is part of the MineWright mod project.

## Version

**Version:** 1.0.0
**Last Updated:** 2026-02-27
**Status:** Design Document - Ready for Implementation
