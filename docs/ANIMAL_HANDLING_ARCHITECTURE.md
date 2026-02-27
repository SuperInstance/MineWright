# Animal Handling AI - Architecture Diagram

## System Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           MineWright Foreman                            │
│                              (Entity)                                   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ contains
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
        ┌───────────────┐ ┌─────────────────┐ ┌──────────────┐
        │  PetManager   │ │ BreedingManager │ │ PenManager   │
        │               │ │                 │ │              │
        │ - tameAnimal()│ │ - schedule()    │ │ - createPen()│
        │ - commandPet()│ │ - findPairs()   │ │ - assign()   │
        │ - setMode()   │ │ - feedParents() │ │ - countIn()  │
        └───────────────┘ └─────────────────┘ └──────────────┘
                    │               │               │
                    └───────────────┼───────────────┘
                                    │ uses
                                    ▼
                        ┌───────────────────────┐
                        │   AnimalDetector      │
                        │                       │
                        │ - scan(radius)        │
                        │ - classify(entity)    │
                        │ - findNearest(type)   │
                        │ - findBreedingPairs() │
                        └───────────────────────┘
                                    │ uses
                                    ▼
                        ┌───────────────────────┐
                        │ AnimalClassification  │
                        │ (Enum)                │
                        │                       │
                        │ TAMABLE_PET           │
                        │ BREEDABLE_LIVESTOCK   │
                        │ MOUNT                 │
                        │ HOSTILE               │
                        │ NEUTRAL               │
                        │ PASSIVE               │
                        └───────────────────────┘
```

## Action Flow Diagram

```
User Command
      │
      ▼
┌─────────────────┐
│  TaskPlanner    │
│  (LLM)          │
└────────┬────────┘
         │ generates
         ▼
┌─────────────────┐
│   Animal Task   │
│                 │
│ - tame          │
│ - breed         │
│ - shear         │
│ - milk          │
│ - build_pen     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ ActionExecutor  │
│                 │
│ queueTask()     │
│ tick()          │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      Specific Action                │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  TameAction                 │   │
│  │  - findTarget()             │   │
│  │  - attemptTame()            │   │
│  │  - tameWolf/Cat/Parrot()    │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  BreedAction                │   │
│  │  - findPair()               │   │
│  │  - feedAnimals()            │   │
│  │  - waitForBaby()            │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  ShearAction                │   │
│  │  - findSheep()              │   │
│  │  - shearSheep()             │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  BuildPenAction             │   │
│  │  - findLocation()           │   │
│  │  - generateBlocks()         │   │
│  │  - placeBlocks()            │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────┐
│   ActionResult  │
│                 │
│ success/failure │
└─────────────────┘
```

## Animal State Machine

```
                    ┌─────────────┐
                    │   WILD      │
                    └──────┬──────┘
                           │ tame
                           ▼
                    ┌─────────────┐
                    │   TAMED     │◄────────────┐
                    └──────┬──────┘             │
                           │                    │
           ┌───────────────┼───────────────┐   │
           ▼               ▼               ▼   │
    ┌─────────────┐ ┌──────────┐ ┌────────────┐│
    │   FOLLOW    │ │   STAY   │ │  PROTECT   ││
    └──────┬──────┘ └─────┬────┘ └─────┬──────┘│
           │               │              │      │
           └───────────────┴──────────────┘      │
                    commandPet()                │
                           │                     │
                           │ breed (if adult)    │
                           ▼                     │
                    ┌─────────────┐             │
                    │ IN_LOVE     │             │
                    └──────┬──────┘             │
                           │ breed with partner  │
                           ▼                     │
                    ┌─────────────┐             │
                    │ BREEDING    │─────────────┘
                    └──────┬──────┘
                           │ 5 minutes
                           ▼
                    ┌─────────────┐
                    │  BABY       │
                    │  (growing)  │
                    └──────┬──────┘
                           │ grow up
                           ▼
                    ┌─────────────┐
                    │  ADULT      │
                    └─────────────┘
```

## Pen Management Flow

```
User Command: "build a sheep pen"
              │
              ▼
    ┌─────────────────┐
    │ BuildPenAction  │
    └────────┬────────┘
             │
             ▼
    ┌─────────────────┐
    │ Find flat area  │
    │ (16x16 search)  │
    └────────┬────────┘
             │
             ▼
    ┌─────────────────┐
    │ Generate blocks │
    │ - Walls         │
    │ - Floor         │
    │ - Gate          │
    │ - Feeder        │
    └────────┬────────┘
             │
             ▼
    ┌─────────────────┐
    │ Place blocks    │
    │ (4 per tick)    │
    └────────┬────────┘
             │
             ▼
    ┌─────────────────┐
    │ Pen complete    │
    └────────┬────────┘
             │
             ▼
    ┌─────────────────┐
    │ Assign animals  │
    │ - Guide to pen  │
    │ - Track UUIDs   │
    └─────────────────┘
```

## Breeding Optimization Algorithm

```
1. Scan nearby animals (32 block radius)
   │
   ▼
2. Group by type and age
   │
   ▼
3. Filter: only adults, not already breeding
   │
   ▼
4. Find compatible pairs (same type, opposite sex if applicable)
   │
   ▼
5. Check breeding food availability
   │
   ▼
6. Create breeding queue
   │
   ▼
7. Execute breeding batches
   ├─ Move close to pair
   ├─ Feed both animals (trigger love mode)
   ├─ Wait for baby to spawn
   └─ Record statistics
   │
   ▼
8. Update breeding cooldown (5 minutes)
   │
   ▼
9. Repeat for next pair
```

## Data Structures

### PetData
```
PetData {
    UUID: uuid              // Animal's unique ID
    EntityType: type        // e.g., WOLF, CAT
    PetFollowMode: mode     // FOLLOW, STAY, PROTECT, ROAM
    long: tamedTime         // When tamed
    String: name            // Optional custom name
}
```

### Pen
```
Pen {
    String: id              // "pen_1", "pen_2", etc.
    BlockPos: center        // Center position
    PenDimensions: size     // width, depth, height
    String: purpose         // "sheep", "cows", etc.
    long: createdTime
    Set<UUID>: animals      // UUIDs of animals in pen
}
```

### BreedingTask
```
BreedingTask {
    BreedingPair: pair      // Parent animals
    long: scheduledTime     // When scheduled
    boolean: completed      // Done status
}
```

## Integration Points

### With ForemanEntity
```java
public class ForemanEntity {
    private PetManager petManager;
    private BreedingManager breedingManager;
    private PenManager penManager;

    @Override
    public void tick() {
        // ... existing code ...

        petManager.tick();
        breedingManager.tick();
    }
}
```

### With ActionExecutor
```java
public class ActionExecutor {
    public void queueTask(Task task) {
        String actionType = task.getAction();

        // Route to appropriate action
        switch (actionType) {
            case "tame":
                queueAction(new TameAction(foreman, task));
                break;
            case "breed":
                queueAction(new BreedAction(foreman, task));
                break;
            // ... other actions
        }
    }
}
```

### With PromptBuilder
```java
public class PromptBuilder {
    public static String buildSystemPrompt() {
        return basePrompt + animalActions;
    }

    public static String buildUserPrompt(ForemanEntity foreman, String command) {
        String animalContext = AnimalDetector.getAnimalSummary(foreman);
        return baseUserPrompt + "\nNearby Animals: " + animalContext;
    }
}
```

## Performance Considerations

### Optimization Strategies

1. **Sparse Scanning**: Scan for animals every 100 ticks, not every tick
2. **Chunk-Based Caching**: Cache animal positions by chunk
3. **Lazy Evaluation**: Only classify animals when needed
4. **Batch Processing**: Process multiple animals per tick
5. **Distance-Based Priority**: Prioritize closer animals

### Memory Management

1. **UUID Tracking**: Store UUIDs instead of entity references
2. **Pen Limits**: Max 20 animals per pen
3. **Pet Limits**: Max 10 pets per foreman
4. **Breeding Queue**: Max 20 pending breeding tasks
5. **Memory Cleanup**: Remove dead animals from tracking

### Threading

1. **ConcurrentHashMap**: For thread-safe animal tracking
2. **AtomicBoolean**: For state flags
3. **ConcurrentLinkedQueue**: For breeding tasks
4. **Synchronized Blocks**: Minimize critical sections

## Error Handling

### Common Scenarios

1. **Animal Disappears**: Cancel action, notify user
2. **No Breeding Food**: Log warning, skip breeding
3. **Pen Location Blocked**: Find alternative location
4. **Taming Fails**: Retry with different item
5. **Escape Detected**: Guide animal back to pen

### Recovery Strategies

1. **Graceful Degradation**: Continue with remaining animals
2. **User Notification**: Send chat message for issues
3. **State Reset**: Clear stuck actions
4. **Timeout Protection**: Max ticks for each action
5. **Fallback Behavior**: Return to idle if stuck
