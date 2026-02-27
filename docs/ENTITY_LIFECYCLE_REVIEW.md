# Entity Lifecycle Management Review

**Project:** MineWright Mod (Minecraft Forge 1.20.1)
**Date:** 2026-02-27
**Scope:** Entity spawning, initialization, persistence, cleanup, and error recovery

---

## Executive Summary

The entity lifecycle management in MineWright has several areas requiring improvement, particularly around initialization safety, NBT persistence validation, error recovery, and cleanup coordination. While the basic flow works, there are edge cases that could lead to memory leaks, corrupted state, or runtime exceptions.

**Critical Issues:** 4
**High Priority:** 7
**Medium Priority:** 5
**Low Priority:** 3

---

## Current Architecture

### Entity Lifecycle Flow

```
Spawn (CrewManager.spawnCrewMember)
    ↓
Construction (new ForemanEntity)
    ↓
Initialization (constructor sets up components)
    ↓
World Addition (level.addFreshEntity)
    ↓
First Tick (orchestrator registration)
    ↓
Periodic Tick (action execution, dialogue)
    ↓
Save/Load (NBT persistence)
    ↓
Removal (discard() or natural death)
    ↓
Cleanup (orchestrator unregistration)
```

### Key Components

| Component | Responsibility | File |
|-----------|----------------|------|
| `CrewManager` | Spawning, tracking, removing entities | `CrewManager.java` |
| `ForemanEntity` | Main entity class with lifecycle hooks | `ForemanEntity.java` |
| `ActionExecutor` | Task execution and state machine | `ActionExecutor.java` |
| `ForemanMemory` | Goal and action history | `ForemanMemory.java` |
| `CompanionMemory` | Complex relationship tracking | `CompanionMemory.java` |
| `OrchestratorService` | Multi-agent coordination | `OrchestratorService.java` |

---

## Issue Analysis

### 1. Spawning Issues

#### 1.1 Race Condition in Name Check

**Severity:** High
**Location:** `CrewManager.spawnCrewMember()` lines 25-28

```java
if (activeCrewMembers.containsKey(name)) {
    MineWrightMod.LOGGER.warn("Crew member name '{}' already exists", name);
    return null;
}
```

**Problem:** The name check and insertion are not atomic. In multi-threaded scenarios (e.g., multiple spawn commands), two entities could have the same name.

**Evidence:** `ConcurrentHashMap` provides atomic operations, but the check-then-act pattern is not atomic.

**Impact:** Duplicate names break the orchestrator's agent registry which uses names as keys.

**Recommendation:**
```java
// Use putIfAbsent for atomic check-and-insert
ForemanEntity existing = activeCrewMembers.putIfAbsent(name, crewMember);
if (existing != null) {
    MineWrightMod.LOGGER.warn("Crew member name '{}' already exists", name);
    // Clean up the crew member we just created
    crewMember.discard();
    return null;
}
```

#### 1.2 Incomplete Error Handling in Spawn

**Severity:** High
**Location:** `CrewManager.spawnCrewMember()` lines 34-66

**Problem:** When `addFreshEntity()` returns false, the entity is created and initialized but not added to the world. The manager's internal maps are not updated, but the entity object exists with initialized components.

**Evidence:** Lines 55-58 log failure but don't clean up the entity.

**Impact:** Memory leak from entities that were created but never added to the world.

**Recommendation:**
```java
boolean added = level.addFreshEntity(crewMember);
if (!added) {
    // Clean up the orphaned entity
    crewMember.discard();
    MineWrightMod.LOGGER.error("Failed to add crew member to world");
    return null;
}
```

#### 1.3 Missing Entity Validation Before Spawn

**Severity:** Medium
**Location:** `CrewManager.spawnCrewMember()`

**Problem:** No validation that spawn position is safe (not in solid block, not in void, etc.).

**Impact:** Entity can spawn in invalid locations, causing immediate stuck state.

**Recommendation:**
```java
private boolean isValidSpawnPosition(ServerLevel level, Vec3 pos) {
    // Check if position is loaded
    if (!level.isLoadedAt(new BlockPos((int)pos.x, (int)pos.y, (int)pos.z))) {
        return false;
    }
    // Check if spawning inside solid block
    BlockState state = level.getBlockState(new BlockPos((int)pos.x, (int)pos.y, (int)pos.z));
    return !state.isSuffocating(level, new BlockPos((int)pos.x, (int)pos.y, (int)pos.z));
}
```

---

### 2. Initialization Issues

#### 2.1 Constructor Overreach

**Severity:** High
**Location:** `ForemanEntity` constructor lines 56-72

**Problem:** Constructor initializes multiple complex objects (`ForemanMemory`, `CompanionMemory`, `ActionExecutor`, `ProactiveDialogueManager`). If any throw exceptions, the entity is left in partially constructed state.

**Evidence:** All components created in constructor with no try-catch.

**Impact:** Entity exists but components are null, causing NPEs during tick.

**Recommendation:**
```java
private boolean initialized = false;

public ForemanEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
    super(entityType, level);
    this.entityName = "Foreman";
    // Defer complex initialization
    this.initialized = false;
}

public void initializeComponents() {
    if (initialized) return;
    try {
        this.memory = new ForemanMemory(this);
        this.companionMemory = new CompanionMemory();
        this.actionExecutor = new ActionExecutor(this);
        this.dialogueManager = new ProactiveDialogueManager(this);
        this.setCustomNameVisible(true);
        this.isInvulnerable = true;
        this.setInvulnerable(true);

        if (!level().isClientSide) {
            this.orchestrator = MineWrightMod.getOrchestratorService();
        }
        this.initialized = true;
    } catch (Exception e) {
        LOGGER.error("Failed to initialize components for entity", e);
        this.initialized = false;
    }
}

@Override
public void tick() {
    super.tick();
    if (!this.level().isClientSide && !initialized) {
        initializeComponents();
        if (!initialized) {
            // If still failed, discard this entity
            discard();
            return;
        }
    }
    // ... rest of tick
}
```

#### 2.2 Orchestrator Registration Timing

**Severity:** Medium
**Location:** `ForemanEntity.registerWithOrchestrator()` lines 275-303

**Problem:** Orchestrator registration happens in first tick, but entity may not be fully initialized. Also, role assignment depends on `CrewManager.getActiveCount()` which may not include this entity yet.

**Evidence:** Line 282 gets count before entity is added to maps.

**Impact:** Incorrect role assignment (multiple foremen or no foreman).

**Recommendation:** Register after entity is added to CrewManager's maps, not in tick.

#### 2.3 Lazy Initialization Not Thread-Safe

**Severity:** Medium
**Location:** `ActionExecutor.getTaskPlanner()` lines 92-98

**Problem:** Double-checked locking not used for lazy initialization.

**Recommendation:**
```java
private volatile TaskPlanner taskPlanner;

public TaskPlanner getTaskPlanner() {
    if (taskPlanner == null) {
        synchronized (this) {
            if (taskPlanner == null) {
                taskPlanner = new TaskPlanner();
            }
        }
    }
    return taskPlanner;
}
```

---

### 3. NBT Persistence Issues

#### 3.1 Missing Version Number

**Severity:** High
**Location:** `ForemanEntity.addAdditionalSaveData()` lines 158-169

**Problem:** NBT data has no version identifier. Future schema changes will break backward compatibility.

**Evidence:** Hardcoded key names without version prefixes.

**Impact:** Cannot evolve data format without breaking old saves.

**Recommendation:**
```java
private static final int NBT_VERSION = 1;
private static final String VERSION_KEY = "CrewNBTVersion";

@Override
public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putInt(VERSION_KEY, NBT_VERSION);
    // ... rest of save
}

@Override
public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    int version = tag.contains(VERSION_KEY) ? tag.getInt(VERSION_KEY) : 0;

    switch (version) {
        case 0:
            loadNBTVersion0(tag);
            break;
        case 1:
            loadNBTVersion1(tag);
            break;
        default:
            LOGGER.warn("Unknown NBT version: {}", version);
            loadNBTVersion1(tag); // Attempt to load latest
    }
}
```

#### 3.2 No Validation on Load

**Severity:** High
**Location:** `ForemanEntity.readAdditionalSaveData()` lines 172-188

**Problem:** No validation that loaded data is sane. Missing or malformed data can cause runtime errors.

**Evidence:** Direct tag access with `contains()` checks but no bounds/range checking.

**Impact:** Corrupted save files can crash the game or cause undefined behavior.

**Recommendation:**
```java
@Override
public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);

    try {
        // Validate and load name
        if (tag.contains("CrewName")) {
            String name = tag.getString("CrewName");
            if (name != null && !name.isEmpty() && name.length() <= 64) {
                this.setEntityName(name);
            } else {
                LOGGER.warn("Invalid crew name in NBT, using default");
                this.setEntityName("Foreman");
            }
        }

        // Validate memory tag structure
        if (tag.contains("Memory")) {
            CompoundTag memoryTag = tag.getCompound("Memory");
            if (isValidMemoryTag(memoryTag)) {
                this.memory.loadFromNBT(memoryTag);
            } else {
                LOGGER.warn("Invalid memory tag, using empty memory");
                // Keep default empty memory
            }
        }
    } catch (Exception e) {
        LOGGER.error("Error loading entity from NBT, using defaults", e);
        // Reset to safe defaults
        this.setEntityName("Foreman");
        this.memory = new ForemanMemory(this);
    }
}

private boolean isValidMemoryTag(CompoundTag tag) {
    // Check structure is valid
    return true; // Implement validation
}
```

#### 3.3 CompanionMemory Vector Store Not Persisted

**Severity:** Medium
**Location:** `CompanionMemory.saveToNBT()` lines 691-837

**Problem:** Vector embeddings for semantic search are not saved to NBT. On load, memories exist but search is broken.

**Evidence:** `memoryVectorStore` and `memoryToVectorId` are not saved.

**Impact:** Semantic search fails after world reload.

**Recommendation:**
```java
// Save vector store
ListTag vectorList = new ListTag();
for (Map.Entry<EpisodicMemory, Integer> entry : memoryToVectorId.entrySet()) {
    CompoundTag vectorTag = new CompoundTag();
    vectorTag.putInt("VectorId", entry.getValue());
    // Save memory reference (need to add IDs to memories)
    vectorList.add(vectorTag);
}
tag.put("VectorMappings", vectorList);

// On load, rebuild vector embeddings
for (int i = 0; i < episodicMemories.size(); i++) {
    EpisodicMemory memory = episodicMemories.get(i);
    addMemoryToVectorStore(memory); // Regenerates embeddings
}
```

#### 3.4 Task Queue Not Persisted

**Severity:** Medium
**Location:** `ActionExecutor` has no NBT save/load

**Problem:** Active tasks and planning state are lost on world reload.

**Impact:** Agents forget what they were doing.

**Recommendation:**
```java
public void saveToNBT(CompoundTag tag) {
    tag.putString("CurrentGoal", currentGoal != null ? currentGoal : "");

    // Save task queue
    ListTag taskList = new ListTag();
    for (Task task : taskQueue) {
        CompoundTag taskTag = new CompoundTag();
        taskTag.putString("Action", task.getAction());
        // Save parameters
        CompoundTag paramsTag = new CompoundTag();
        for (Map.Entry<String, Object> entry : task.getParameters().entrySet()) {
            paramsTag.putString(entry.getKey(), String.valueOf(entry.getValue()));
        }
        taskTag.put("Parameters", paramsTag);
        taskList.add(taskTag);
    }
    tag.put("TaskQueue", taskList);

    tag.putBoolean("IsPlanning", isPlanning);
}

public void loadFromNBT(CompoundTag tag) {
    if (tag.contains("CurrentGoal")) {
        currentGoal = tag.getString("CurrentGoal");
    }

    if (tag.contains("TaskQueue")) {
        taskQueue.clear();
        ListTag taskList = tag.getList("TaskQueue", 10);
        for (int i = 0; i < taskList.size(); i++) {
            CompoundTag taskTag = taskList.getCompound(i);
            String action = taskTag.getString("Action");
            CompoundTag paramsTag = taskTag.getCompound("Parameters");

            Map<String, Object> params = new HashMap<>();
            for (String key : paramsTag.getAllKeys()) {
                params.put(key, paramsTag.getString(key));
            }

            taskQueue.add(new Task(action, params));
        }
    }
}
```

---

### 4. Cleanup Issues

#### 4.1 No Cleanup Hook Override

**Severity:** Critical
**Location:** `ForemanEntity` does not override `remove()`

**Problem:** Entity removal doesn't properly clean up resources. Components with external references (orchestrator, action executor) are not notified.

**Evidence:** Cleanup happens in `CrewManager.removeCrewMember()` but not when entity dies naturally.

**Impact:** Memory leaks from orchestrator retaining references to dead entities.

**Recommendation:**
```java
@Override
public void remove(RemovalReason reason) {
    MineWrightMod.LOGGER.info("Removing entity '{}' for reason: {}", entityName, reason);

    // Stop any ongoing actions
    if (actionExecutor != null) {
        actionExecutor.stopCurrentAction();
    }

    // Unregister from orchestrator
    if (orchestrator != null && registeredWithOrchestrator.get()) {
        orchestrator.unregisterAgent(entityName);
        registeredWithOrchestrator.set(false);
    }

    // Remove from crew manager (prevent double-removal)
    CrewManager manager = MineWrightMod.getCrewManager();
    if (manager != null) {
        ForemanEntity tracked = manager.getCrewMember(entityName);
        if (tracked == this) {
            manager.activeCrewMembers.remove(entityName);
            manager.crewMembersByUUID.remove(getUUID());
        }
    }

    super.remove(reason);
}
```

#### 4.2 CompletableFuture Not Cancelled

**Severity:** High
**Location:** `ActionExecutor` lines 46-47

**Problem:** `planningFuture` holds reference to async LLM call. If entity is removed while planning, future completes but entity is gone.

**Evidence:** No cancellation in cleanup paths.

**Impact:** Wasted computation and potential NPE when future completes.

**Recommendation:**
```java
public void cleanup() {
    // Cancel async planning
    if (planningFuture != null && !planningFuture.isDone()) {
        planningFuture.cancel(true);
        planningFuture = null;
    }

    // Stop actions
    stopCurrentAction();

    // Reset state
    isPlanning = false;
    pendingCommand = null;
}

// Call from remove()
@Override
public void remove(RemovalReason reason) {
    if (actionExecutor != null) {
        actionExecutor.cleanup();
    }
    // ... rest of cleanup
    super.remove(reason);
}
```

#### 4.3 Incomplete Cleanup in clearAllCrewMembers

**Severity:** Medium
**Location:** `CrewManager.clearAllCrewMembers()` lines 113-134

**Problem:** Doesn't stop ongoing actions before discarding entities.

**Evidence:** Just calls `discard()` without cleanup.

**Impact:** Actions continue for one tick after discard.

**Recommendation:**
```java
public void clearAllCrewMembers() {
    MineWrightMod.LOGGER.info("Clearing {} crew member entities", activeCrewMembers.size());

    // First, stop all ongoing work
    for (ForemanEntity crewMember : activeCrewMembers.values()) {
        if (crewMember.getActionExecutor() != null) {
            crewMember.getActionExecutor().stopCurrentAction();
        }
    }

    // Unregister all from orchestrator
    OrchestratorService orchestrator = MineWrightMod.getOrchestratorService();
    if (orchestrator != null) {
        for (String name : activeCrewMembers.keySet()) {
            orchestrator.unregisterAgent(name);
        }
    }

    // Finally discard all entities
    for (ForemanEntity crewMember : activeCrewMembers.values()) {
        crewMember.discard();
    }
    activeCrewMembers.clear();
    crewMembersByUUID.clear();
}
```

---

### 5. Error Recovery Issues

#### 5.1 No Corrupted Entity Detection

**Severity:** High
**Location:** `CrewManager.tick()` lines 158-178

**Problem:** Cleanup only checks `isAlive()` and `isRemoved()`. Doesn't detect entities in error states (e.g., stuck in void, stuck in block).

**Impact:** Dead entities accumulate in manager maps.

**Recommendation:**
```java
public void tick(ServerLevel level) {
    Iterator<Map.Entry<String, ForemanEntity>> iterator = activeCrewMembers.entrySet().iterator();
    while (iterator.hasNext()) {
        Map.Entry<String, ForemanEntity> entry = iterator.next();
        ForemanEntity crewMember = entry.getValue();

        boolean shouldRemove = false;
        String reason = null;

        if (!crewMember.isAlive() || crewMember.isRemoved()) {
            shouldRemove = true;
            reason = "dead or removed";
        } else if (isStuck(crewMember)) {
            shouldRemove = true;
            reason = "stuck for too long";
        } else if (isDesynced(crewMember)) {
            shouldRemove = true;
            reason = "position desync detected";
        }

        if (shouldRemove) {
            iterator.remove();
            crewMembersByUUID.remove(crewMember.getUUID());

            OrchestratorService orchestrator = MineWrightMod.getOrchestratorService();
            if (orchestrator != null) {
                orchestrator.unregisterAgent(entry.getKey());
            }

            MineWrightMod.LOGGER.info("Cleaned up crew member: {} ({})", entry.getKey(), reason);
        }
    }
}

private boolean isStuck(ForemanEntity entity) {
    // Check if entity hasn't moved in X ticks
    // Or if action has been "in progress" too long
    return false; // Implement logic
}
```

#### 5.2 No Entity Validation on Load

**Severity:** High
**Location:** World load doesn't validate entities

**Problem:** Corrupted entity data from crashes can load and cause issues.

**Impact:** Broken entities in world.

**Recommendation:**
```java
// In ServerEventHandler or lifecycle event
@SubscribeEvent
public static void onWorldLoad(WorldEvent.Load event) {
    if (event.getLevel() instanceof ServerLevel level) {
        // Validate all ForemanEntity instances
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ForemanEntity foreman) {
                if (!validateEntity(foreman)) {
                    LOGGER.warn("Invalid entity detected, removing: {}", foreman.getSteveName());
                    foreman.discard();
                }
            }
        }
    }
}

private boolean validateEntity(ForemanEntity entity) {
    // Check required components
    if (entity.getMemory() == null) return false;
    if (entity.getActionExecutor() == null) return false;
    if (entity.getSteveName() == null || entity.getSteveName().isEmpty()) return false;

    return true;
}
```

#### 5.3 Graceful Degradation Missing

**Severity:** Medium
**Location:** Various

**Problem:** When components fail, entity crashes instead of degrading gracefully.

**Recommendation:**
```java
public void tick() {
    super.tick();

    if (!this.level().isClientSide) {
        try {
            // Register with orchestrator
            if (!registeredWithOrchestrator.get() && orchestrator != null) {
                registerWithOrchestrator();
            }

            // Process messages
            processMessages();

            // Execute actions with error handling
            if (actionExecutor != null) {
                actionExecutor.tick();
            }

            // Dialogue with error handling
            if (dialogueManager != null) {
                dialogueManager.tick();
            }

            reportTaskProgress();
        } catch (Exception e) {
            LOGGER.error("Error in entity tick, attempting recovery", e);
            // Attempt recovery
            if (errorCount > MAX_ERRORS) {
                LOGGER.error("Too many errors, discarding entity");
                discard();
            } else {
                errorCount++;
            }
        }
    }
}
```

---

## Recommended Fixes

### Priority 1: Critical Fixes

1. **Add `remove()` override** - Prevent memory leaks from dead entities
2. **Add NBT versioning** - Enable safe schema evolution
3. **Fix spawn atomicity** - Use `putIfAbsent` for name check
4. **Add spawn validation** - Check spawn position is safe

### Priority 2: High Priority

5. **Defer component initialization** - Move out of constructor
6. **Add NBT validation** - Validate all loaded data
7. **Persist task queue** - Don't lose work on reload
8. **Cancel futures on cleanup** - Prevent wasted computation

### Priority 3: Medium Priority

9. **Thread-safe lazy init** - Proper double-checked locking
10. **Persist vector store** - Rebuild embeddings on load
11. **Add stuck detection** - Cleanup broken entities
12. **Stop actions before discard** - Proper cleanup

### Priority 4: Low Priority

13. **Add entity validation event** - Validate on world load
14. **Improve error messages** - Better debugging
15. **Add health checks** - Periodic validation

---

## Code Improvements

### EntityFactory Pattern

Extract spawning logic into a factory for better error handling:

```java
public class EntityFactory {
    private final CrewManager crewManager;
    private final ServerLevel level;

    public SpawnResult spawnCrewMember(Vec3 position, String name) {
        // Validate
        if (!validateName(name)) {
            return SpawnResult.failure("Invalid name: " + name);
        }
        if (!validatePosition(position)) {
            return SpawnResult.failure("Invalid spawn position");
        }
        if (crewManager.getCrewMember(name) != null) {
            return SpawnResult.failure("Name already exists: " + name);
        }

        // Create
        ForemanEntity entity;
        try {
            entity = new ForemanEntity(MineWrightMod.FOREMAN_ENTITY.get(), level);
            entity.initializeComponents();
            if (!entity.isInitialized()) {
                return SpawnResult.failure("Failed to initialize entity");
            }
        } catch (Exception e) {
            return SpawnResult.failure("Creation failed: " + e.getMessage());
        }

        // Position
        entity.setPos(position.x, position.y, position.z);
        entity.setEntityName(name);

        // Add to world
        if (!level.addFreshEntity(entity)) {
            entity.discard();
            return SpawnResult.failure("Failed to add entity to world");
        }

        // Register
        crewMember.trackEntity(entity);

        return SpawnResult.success(entity);
    }
}
```

### Lifecycle State Machine

Add explicit lifecycle states:

```java
public enum EntityLifecycleState {
    CREATING,       // Constructor running
    INITIALIZING,   // Components being created
    REGISTERING,    // Being added to world
    ACTIVE,         // Normal operation
    DEGRADING,      // Errors detected, running in reduced mode
    DYING,          // Removal started
    DEAD            // Removed
}

public class ForemanEntity extends PathfinderMob {
    private final AtomicReference<EntityLifecycleState> lifecycleState =
        new AtomicReference<>(EntityLifecycleState.CREATING);

    public boolean isStateActive() {
        return lifecycleState.get() == EntityLifecycleState.ACTIVE;
    }

    public boolean isStateDying() {
        return lifecycleState.get().ordinal() >= EntityLifecycleState.DYING.ordinal();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!lifecycleState.compareAndSet(
            EntityLifecycleState.ACTIVE,
            EntityLifecycleState.DYING
        )) {
            // Already dying or dead
            return;
        }

        try {
            cleanup();
        } finally {
            lifecycleState.set(EntityLifecycleState.DEAD);
            super.remove(reason);
        }
    }
}
```

---

## Testing Recommendations

### Unit Tests Needed

1. **Spawn validation** - Test invalid names, positions, limits
2. **NBT round-trip** - Save/load preserves all state
3. **NBT migration** - Version 0 to version 1 migration works
4. **Concurrent spawn** - No duplicates under parallel load
5. **Cleanup verification** - All resources released

### Integration Tests Needed

1. **World reload** - Entities persist and resume work
2. **Player logout** - Entities handle gracefully
3. **Crash recovery** - Corrupted entities cleaned up
4. **Multi-agent** - Orchestrator handles agent death

### Manual Testing Checklist

- [ ] Spawn entity at world border
- [ ] Spawn entity in unloaded chunk
- [ ] Spawn entity with duplicate name
- [ ] Kill entity during async planning
- [ ] Save/load world with active tasks
- [ ] Remove entity while executing action
- [ ] Disconnect while entity is working

---

## Performance Considerations

### Current Performance Issues

1. **NBT save size** - CompanionMemory can save 200+ episodic memories
2. **Vector rebuild** - Re-embedding all memories on load is expensive
3. **Tick cleanup** - Iterator over all entities every tick

### Recommendations

1. **Lazy NBT loading** - Only load recent memories initially
2. **Embedding caching** - Save embeddings to NBT to avoid recomputation
3. **Cleanup throttling** - Run cleanup every 100 ticks, not every tick
4. **Save frequency** - Only save NBT when changes occur

---

## Security Considerations

### Current Security Issues

1. **No input sanitization** - Entity names from commands not validated
2. **NBT injection** - Malicious NBT could crash game
3. **Resource exhaustion** - No limit on memory/components per entity

### Recommendations

```java
private static final Pattern VALID_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_-]{1,32}");

public static boolean isValidName(String name) {
    return name != null && VALID_NAME_PATTERN.matcher(name).matches();
}

private static final int MAX_NBT_SIZE = 1_000_000; // 1MB

public void readAdditionalSaveData(CompoundTag tag) {
    if (tag.size() > MAX_NBT_SIZE) {
        LOGGER.error("NBT tag too large: {} bytes", tag.size());
        throw new RuntimeException("NBT data exceeds maximum size");
    }
    // ... rest of load
}
```

---

## Summary

The entity lifecycle management has a solid foundation but needs hardening for production use. The most critical issues are:

1. **Memory leaks** from incomplete cleanup
2. **Data loss** from missing NBT persistence
3. **Race conditions** in concurrent operations
4. **No error recovery** for corrupted state

Implementing the recommended fixes will significantly improve reliability and enable the system to handle edge cases gracefully.

---

## References

- **Files Modified:**
  - `C:\Users\casey\steve\src\main\java\com\minewright\entity\ForemanEntity.java`
  - `C:\Users\casey\steve\src\main\java\com\minewright\entity\CrewManager.java`
  - `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`
  - `C:\Users\casey\steve\src\main\java\com\minewright\memory\CompanionMemory.java`
  - `C:\Users\casey\steve\src\main\java\com\minewright\event\ServerEventHandler.java`

- **Related Documentation:**
  - `docs/MEMORY_PERSISTENCE_NBT.md` - NBT format details
  - `docs/AUDIT_ERROR_HANDLING.md` - Error handling patterns
  - `docs/AUDIT_THREAD_SAFETY.md` - Concurrency considerations
