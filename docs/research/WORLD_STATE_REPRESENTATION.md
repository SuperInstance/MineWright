# World State Representation for Game AI Planners

**Author:** Orchestrator Research Team
**Date:** 2026-03-02
**Version:** 1.0
**Status:** Research Document
**Related Systems:** HTN Planner, GOAP, WorldState class

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [State Representation Patterns](#2-state-representation-patterns)
3. [State Diff and Patching](#3-state-diff-and-patching)
4. [Partial Observability](#4-partial-observability)
5. [Performance Considerations](#5-performance-considerations)
6. [Application to MineWright](#6-application-to-minewright)
7. [Code Patterns and Best Practices](#7-code-patterns-and-best-practices)
8. [Implementation Recommendations](#8-implementation-recommendations)
9. [References](#9-references)

---

## 1. Introduction

### 1.1 The Role of World State in AI Planners

World state representation is the foundation of all AI planning systems. Whether using GOAP (Goal-Oriented Action Planning), HTN (Hierarchical Task Network), STRIPS, or other planning architectures, the world state serves as:

- **The Planning Context:** Preconditions are evaluated against world state
- **The Planning Target:** Goals define desired world states
- **The Change Model:** Actions transform one world state into another
- **The Execution Tracker:** Monitoring state changes detects plan invalidation

### 1.2 Why World State Representation Matters

| Aspect | Poor Representation | Good Representation |
|--------|-------------------|-------------------|
| **Planning Speed** | O(n) precondition checks | O(1) or O(log n) with indexing |
| **Memory Usage** | Duplicate states on every change | Copy-on-write with structural sharing |
| **Serialization** | Manual serialization logic | Automatic diff/patch support |
| **Replanning** | Full state recalculation | Incremental delta application |
| **Caching** | No cache invalidation strategy | Hash-based cache keys |
| **Partial Observability** | Assumes perfect knowledge | Explicit unknown/uncertain values |

### 1.3 Current MineWright Implementation

MineWright has two world state implementations:

1. **`goal/WorldState.java`** - Facade for navigation goals (335 lines)
   - Wraps Minecraft `Level` and `Entity`
   - Provides block/entity queries
   - Position tracking and distance calculations
   - No state diffing or hashing

2. **`htn/HTNWorldState.java`** - Property-based state for HTN planner (530 lines)
   - Key-value property storage (`Map<String, Object>`)
   - Immutable snapshots for backtracking
   - Type-safe getters (boolean, int, long, double, string)
   - Hash code caching for comparison
   - Builder pattern for construction

**Assessment:** HTNWorldState is well-designed but missing:
- State diff/patch operations
- Partial observability support
- Delta compression for serialization
- State change tracking for replanning triggers

---

## 2. State Representation Patterns

### 2.1 Key-Value Pairs (Dictionary)

**Description:** State as a map of string keys to typed values.

**Advantages:**
- Simple to implement
- Flexible schema (add/remove properties at runtime)
- Easy serialization (JSON, TOML)
- Language-agnostic

**Disadvantages:**
- No compile-time type safety
- String key overhead (memory, comparison cost)
- No schema validation
- Susceptible to typos in key names

**Example (HTNWorldStyle - Current MineWright):**

```java
// Building state with key-value pairs
HTNWorldState state = HTNWorldState.builder()
    .property("hasWood", true)
    .property("woodCount", 64)
    .property("toolType", "diamond_axe")
    .property("positionX", 100)
    .property("positionZ", 200)
    .build();

// Accessing state
boolean hasWood = state.getBoolean("hasWood");
int woodCount = state.getInt("woodCount", 0);

// Preconditions check
method.precondition(s -> s.getBoolean("hasWood") && s.getInt("woodCount") >= 64);
```

**When to Use:**
- Highly dynamic state properties
- User-defined scripting systems
- Cross-language serialization needs

---

### 2.2 Fluent/Belief State

**Description:** State as a collection of boolean fluents (propositions that can be true/false).

**Advantages:**
- Perfect for STRIPS-style planning
- Simple truth maintenance
- Efficient logical operations
- Well-suited for symbolic reasoning

**Disadvantages:**
- Limited expressiveness (no continuous values)
- Explosion of fluents for complex domains
- No natural hierarchy or relationships

**Example (STRIPS-style):**

```java
// Fluent-based state
public class FluentWorldState {
    private final Set<String> trueFluents;
    private final Set<String> falseFluents;
    private final Set<String> unknownFluents; // For partial observability

    public boolean isTrue(String fluent) {
        return trueFluents.contains(fluent);
    }

    public boolean isFalse(String fluent) {
        return falseFluents.contains(fluent);
    }

    public boolean isUnknown(String fluent) {
        return unknownFluents.contains(fluent);
    }

    public void setFluent(String fluent, boolean value) {
        if (value) {
            trueFluents.add(fluent);
            falseFluents.remove(fluent);
            unknownFluents.remove(fluent);
        } else {
            falseFluents.add(fluent);
            trueFluents.remove(fluent);
            unknownFluents.remove(fluent);
        }
    }
}

// Usage
FluentWorldState state = new FluentWorldState();
state.setFluent("has_wood", true);
state.setFluent("at_crafting_table", false);
state.setFluent("enemy_visible", UNKNOWN);

// Preconditions
Precondition hasWood = s -> s.isTrue("has_wood");
Precondition atTable = s -> s.isTrue("at_crafting_table");
```

**When to Use:**
- Classical planning domains (STRIPS, PDDL)
- Propositional logic reasoning
- Simple discrete state spaces

---

### 2.3 Predicate-Based State

**Description:** State as a set of predicate objects with arguments (e.g., `at(agent, location)`, `has(agent, item)`).

**Advantages:**
- Relational modeling (objects, not just values)
- Natural first-order logic mapping
- Efficient pattern matching
- Supports inheritance and types

**Disadvantages:**
- More complex implementation
- Requires schema definition
- Slower lookup than flat maps
- Unification overhead

**Example (First-Order Logic Style):**

```java
// Predicate representation
public class Predicate {
    private final String name;
    private final Object[] arguments;
    private final boolean negated;

    public Predicate(String name, Object... args) {
        this(name, false, args);
    }

    public Predicate(String name, boolean negated, Object... args) {
        this.name = name;
        this.arguments = args;
        this.negated = negated;
    }

    public boolean matches(Predicate other) {
        if (!name.equals(other.name) || negated != other.negated) {
            return false;
        }
        if (arguments.length != other.arguments.length) {
            return false;
        }
        for (int i = 0; i < arguments.length; i++) {
            Object arg = arguments[i];
            Object otherArg = other.arguments[i];
            // Variables (starting with ?) match anything
            if (arg instanceof String && ((String)arg).startsWith("?")) {
                continue;
            }
            if (otherArg instanceof String && ((String)otherArg).startsWith("?")) {
                continue;
            }
            if (!arg.equals(otherArg)) {
                return false;
            }
        }
        return true;
    }
}

// Predicate-based state
public class PredicateWorldState {
    private final Set<Predicate> predicates;

    public boolean satisfies(Predicate query) {
        return predicates.stream()
            .anyMatch(p -> p.matches(query));
    }

    public void add(Predicate predicate) {
        predicates.add(predicate);
    }

    public void remove(Predicate predicate) {
        predicates.removeIf(p -> p.matches(predicate));
    }
}

// Usage
PredicateWorldState state = new PredicateWorldState();
state.add(new Predicate("has", "agent", "wood"));
state.add(new Predicate("at", "agent", "crafting_table"));
state.add(new Predicate("holding", "agent", "diamond_axe"));

// Preconditions
Precondition hasWood = s -> s.satisfies(new Predicate("has", "agent", "wood"));
Precondition atTable = s -> s.satisfies(new Predicate("at", "agent", "crafting_table"));

// Pattern matching with variables
Precondition holdingAnyTool = s -> s.satisfies(new Predicate("holding", "agent", "?tool"));
```

**When to Use:**
- Relational domains (multi-agent, object manipulation)
- First-order logic planning (PDDL, ADL)
- Complex reasoning with variables and unification

---

### 2.4 Object-Oriented State

**Description:** State as a structured object graph with typed fields.

**Advantages:**
- Compile-time type safety
- IDE support and refactoring
- Self-documenting (schema in code)
- Efficient memory layout

**Disadvantages:**
- Harder to serialize
- Less flexible schema changes
- More boilerplate code
- Harder to use with dynamic LLM generation

**Example (Structured Object):**

```java
// Structured state definition
public class MinecraftWorldState {
    // Nested objects for organization
    public final AgentState agent;
    public final InventoryState inventory;
    public final EnvironmentState environment;
    public final TaskState task;

    public MinecraftWorldState() {
        this.agent = new AgentState();
        this.inventory = new InventoryState();
        this.environment = new EnvironmentState();
        this.task = new TaskState();
    }

    // Nested agent state
    public static class AgentState {
        public BlockPos position;
        public float health;
        public float hunger;
        public String currentAction;
        public String heldItem;
    }

    // Nested inventory state
    public static class InventoryState {
        public final Map<String, Integer> items = new HashMap<>();
        public int freeSlots;

        public int getCount(String item) {
            return items.getOrDefault(item, 0);
        }

        public void setCount(String item, int count) {
            if (count <= 0) {
                items.remove(item);
            } else {
                items.put(item, count);
            }
            updateFreeSlots();
        }
    }

    // Nested environment state
    public static class EnvironmentState {
        public String biome;
        public long timeOfDay;
        public boolean isRaining;
        public BlockPos nearestResource;
        public List<BlockPos> visibleEnemies;
    }

    // Nested task state
    public static class TaskState {
        public String currentTask;
        public String targetBlock;
        public int targetCount;
        public int progress;
        public boolean hasMaterials;
        public boolean craftingTableNearby;
    }
}

// Usage
MinecraftWorldState state = new MinecraftWorldState();
state.agent.position = new BlockPos(100, 64, 200);
state.inventory.setCount("wood", 64);
state.inventory.setCount("stick", 4);
state.environment.timeOfDay = 12000; // Noon
state.task.currentTask = "craft_wooden_pickaxe";
state.task.hasMaterials = true;

// Preconditions (compile-time checked!)
Precondition hasMaterials = s -> s.task.hasMaterials;
Precondition nearTable = s -> s.task.craftingTableNearby;
Precondition hasWood = s -> s.inventory.getCount("wood") >= 3;
```

**When to Use:**
- Strongly-typed codebases
- IDE-driven development
- Performance-critical applications
- State with clear, stable schema

---

## 3. State Diff and Patching

### 3.1 The Problem

During planning and execution, AI systems frequently need to:
- Track changes between states (for replanning triggers)
- Serialize state differences (network synchronization)
- Apply incremental updates (patching)
- Compare states efficiently (caching, memoization)

### 3.2 Diff Representation

**Approach 1: Operation Log**

```java
public class StateDiff {
    private final List<StateOperation> operations;

    public interface StateOperation {
        void apply(HTNWorldState state);
        void revert(HTNWorldState state);
    }

    public static class SetOperation implements StateOperation {
        private final String key;
        private final Object oldValue;
        private final Object newValue;

        public SetOperation(String key, Object oldValue, Object newValue) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public void apply(HTNWorldState state) {
            state.setProperty(key, newValue);
        }

        @Override
        public void revert(HTNWorldState state) {
            if (oldValue == null) {
                state.removeProperty(key);
            } else {
                state.setProperty(key, oldValue);
            }
        }
    }

    public static class RemoveOperation implements StateOperation {
        private final String key;
        private final Object oldValue;

        public RemoveOperation(String key, Object oldValue) {
            this.key = key;
            this.oldValue = oldValue;
        }

        @Override
        public void apply(HTNWorldState state) {
            state.removeProperty(key);
        }

        @Override
        public void revert(HTNWorldState state) {
            state.setProperty(key, oldValue);
        }
    }

    public void apply(HTNWorldState state) {
        for (StateOperation op : operations) {
            op.apply(state);
        }
    }

    public void revert(HTNWorldState state) {
        // Apply in reverse order
        for (int i = operations.size() - 1; i >= 0; i--) {
            operations.get(i).revert(state);
        }
    }
}
```

**Approach 2: Delta Map**

```java
public class StateDelta {
    private final Map<String, Object> changedValues;
    private final Set<String> removedKeys;

    public StateDelta() {
        this.changedValues = new HashMap<>();
        this.removedKeys = new HashSet<>();
    }

    public void recordChange(String key, Object oldValue, Object newValue) {
        changedValues.put(key, newValue);
    }

    public void recordRemoval(String key, Object oldValue) {
        removedKeys.add(key);
    }

    public void apply(HTNWorldState state) {
        // Apply changes
        for (Map.Entry<String, Object> entry : changedValues.entrySet()) {
            state.setProperty(entry.getKey(), entry.getValue());
        }
        // Apply removals
        for (String key : removedKeys) {
            state.removeProperty(key);
        }
    }

    public boolean isEmpty() {
        return changedValues.isEmpty() && removedKeys.isEmpty();
    }

    public int size() {
        return changedValues.size() + removedKeys.size();
    }
}
```

### 3.3 Diff Computation

**Algorithm 1: Full Comparison**

```java
public class StateDiffer {
    public static StateDelta computeDiff(HTNWorldState before, HTNWorldState after) {
        StateDelta delta = new StateDelta();

        // Check all keys in 'before'
        for (String key : before.getPropertyNames()) {
            Object beforeValue = before.getProperty(key);
            Object afterValue = after.getProperty(key);

            if (afterValue == null) {
                // Key was removed
                delta.recordRemoval(key, beforeValue);
            } else if (!beforeValue.equals(afterValue)) {
                // Key was changed
                delta.recordChange(key, beforeValue, afterValue);
            }
        }

        // Check for new keys in 'after'
        for (String key : after.getPropertyNames()) {
            if (!before.hasProperty(key)) {
                Object afterValue = after.getProperty(key);
                delta.recordChange(key, null, afterValue);
            }
        }

        return delta;
    }
}
```

**Algorithm 2: Incremental Tracking**

```java
public class TrackingWorldState extends HTNWorldState {
    private final List<StateDelta> deltaLog = new ArrayList<>();

    @Override
    public void setProperty(String key, Object value) {
        Object oldValue = getProperty(key);
        if (!Objects.equals(oldValue, value)) {
            super.setProperty(key, value);
            // Record delta
            if (!deltaLog.isEmpty()) {
                StateDelta currentDelta = deltaLog.get(deltaLog.size() - 1);
                currentDelta.recordChange(key, oldValue, value);
            }
        }
    }

    @Override
    public void removeProperty(String key) {
        Object oldValue = getProperty(key);
        if (oldValue != null) {
            super.removeProperty(key);
            // Record delta
            if (!deltaLog.isEmpty()) {
                StateDelta currentDelta = deltaLog.get(deltaLog.size() - 1);
                currentDelta.recordRemoval(key, oldValue);
            }
        }
    }

    public void beginTracking() {
        deltaLog.add(new StateDelta());
    }

    public StateDelta endTracking() {
        if (deltaLog.isEmpty()) {
            return new StateDelta();
        }
        return deltaLog.remove(deltaLog.size() - 1);
    }
}
```

### 3.4 Patch Application

```java
public class StatePatcher {
    public static void applyPatch(HTNWorldState state, StateDelta patch) {
        patch.apply(state);
    }

    public static void applyPatches(HTNWorldState state, List<StateDelta> patches) {
        for (StateDelta patch : patches) {
            applyPatch(state, patch);
        }
    }

    // Conditional patching (only apply if preconditions met)
    public static boolean tryApplyPatch(
        HTNWorldState state,
        StateDelta patch,
        Predicate<HTNWorldState> precondition
    ) {
        if (precondition.test(state)) {
            applyPatch(state, patch);
            return true;
        }
        return false;
    }
}
```

### 3.5 Delta Compression for Serialization

When transmitting world state over network or persisting to disk, delta compression reduces bandwidth/storage:

```java
public class StateDeltaSerializer {
    // Serialize delta to JSON
    public static String toJson(StateDelta delta) {
        StringBuilder json = new StringBuilder();
        json.append("{");

        // Changed values
        if (!delta.getChangedValues().isEmpty()) {
            json.append("\"changes\":{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : delta.getChangedValues().entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":");
                json.append(valueToJson(entry.getValue()));
                first = false;
            }
            json.append("}");
        }

        // Removed keys
        if (!delta.getRemovedKeys().isEmpty()) {
            if (!delta.getChangedValues().isEmpty()) json.append(",");
            json.append("\"removed\":[");
            boolean first = true;
            for (String key : delta.getRemovedKeys()) {
                if (!first) json.append(",");
                json.append("\"").append(key).append("\"");
                first = false;
            }
            json.append("]");
        }

        json.append("}");
        return json.toString();
    }

    private static String valueToJson(Object value) {
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Number) return value.toString();
        if (value == null) return "null";
        return "\"" + value.toString() + "\"";
    }

    // Deserialize JSON to delta
    public static StateDelta fromJson(String json) {
        // Parse JSON and build delta
        // Implementation omitted for brevity
        return new StateDelta();
    }
}
```

---

## 4. Partial Observability

### 4.1 The Challenge

In many game scenarios, agents don't have complete knowledge of the world state:
- Fog of war
- Hidden entities
- Unexplored areas
- Unknown inventory contents
- Hidden enemy intentions

### 4.2 Three-Valued Logic

Extend boolean logic with **UNKNOWN**:

```java
public enum TruthValue {
    TRUE, FALSE, UNKNOWN
}

public class PartialWorldState extends HTNWorldState {
    private final Map<String, TruthValue> truthValues;

    public TruthValue getTruthValue(String key) {
        return truthValues.getOrDefault(key, TruthValue.UNKNOWN);
    }

    public void setKnown(String key, boolean value) {
        setProperty(key, value);
        truthValues.put(key, value ? TruthValue.TRUE : TruthValue.FALSE);
    }

    public void setUnknown(String key) {
        truthValues.put(key, TruthValue.UNKNOWN);
        removeProperty(key);
    }

    // Three-valued logic for preconditions
    public boolean satisfiesPrecondition(String key, boolean requiredValue) {
        TruthValue tv = getTruthValue(key);
        if (tv == TruthValue.UNKNOWN) {
            return false; // Conservative: unknown doesn't satisfy
        }
        return (tv == TruthValue.TRUE) == requiredValue;
    }
}
```

### 4.3 Belief State

Represent uncertainty as probability distributions:

```java
public class BeliefState {
    // For each possible world state, store probability
    private final Map<HTNWorldState, Double> possibleWorlds;

    public BeliefState() {
        this.possibleWorlds = new HashMap<>();
    }

    // Initialize with single known state
    public static BeliefState certain(HTNWorldState state) {
        BeliefState belief = new BeliefState();
        belief.possibleWorlds.put(state, 1.0);
        return belief;
    }

    // Initialize with uniform distribution over possibilities
    public static BeliefState uncertain(List<HTNWorldState> possibilities) {
        BeliefState belief = new BeliefState();
        double probability = 1.0 / possibilities.size();
        for (HTNWorldState state : possibilities) {
            belief.possibleWorlds.put(state, probability);
        }
        return belief;
    }

    // Update belief based on observation
    public void update(Predicate<HTNWorldState> observation) {
        // Filter states that don't match observation
        Map<HTNWorldState, Double> newWorlds = new HashMap<>();
        double totalProbability = 0.0;

        for (Map.Entry<HTNWorldState, Double> entry : possibleWorlds.entrySet()) {
            HTNWorldState state = entry.getKey();
            double prob = entry.getValue();

            if (observation.test(state)) {
                newWorlds.put(state, prob);
                totalProbability += prob;
            }
        }

        // Renormalize
        if (totalProbability > 0) {
            for (Map.Entry<HTNWorldState, Double> entry : newWorlds.entrySet()) {
                entry.setValue(entry.getValue() / totalProbability);
            }
            possibleWorlds.clear();
            possibleWorlds.putAll(newWorlds);
        }
    }

    // Get most likely state
    public HTNWorldState getMostLikelyState() {
        return possibleWorlds.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    // Check if all possible states satisfy condition
    public boolean certainly(Predicate<HTNWorldState> condition) {
        return possibleWorlds.keySet().stream()
            .allMatch(condition);
    }

    // Check if any possible state satisfies condition
    public boolean possibly(Predicate<HTNWorldState> condition) {
        return possibleWorlds.keySet().stream()
            .anyMatch(condition);
    }
}
```

### 4.4 Sensor Models

Model how observations map to world state:

```java
public interface SensorModel {
    // Given actual world state, return observation
    Object observe(HTNWorldState actualState);

    // Given observation, return set of possible states
    List<HTNWorldState> possibleStates(Object observation);
}

// Example: Line of sight sensor
public class LineOfSightSensor implements SensorModel {
    private final int visibilityRadius;

    public LineOfSightSensor(int radius) {
        this.visibilityRadius = radius;
    }

    @Override
    public Object observe(HTNWorldState actualState) {
        // Returns which entities are visible
        Set<String> visibleEntities = new HashSet<>();
        BlockPos agentPos = new BlockPos(
            actualState.getInt("positionX"),
            actualState.getInt("positionY"),
            actualState.getInt("positionZ")
        );

        // Check each entity
        for (String entityKey : actualState.getPropertyNames()) {
            if (entityKey.startsWith("entity_")) {
                BlockPos entityPos = parseEntityPosition(actualState, entityKey);
                if (agentPos.distSqr(entityPos) <= visibilityRadius * visibilityRadius) {
                    visibleEntities.add(entityKey);
                }
            }
        }

        return visibleEntities;
    }

    @Override
    public List<HTNWorldState> possibleStates(Object observation) {
        // This is more complex - generate all states consistent with observation
        // For simplicity, return empty list
        return List.of();
    }
}
```

### 4.5 Belief Revision

Update beliefs when new information arrives:

```java
public class BeliefRevision {
    private final BeliefState belief;
    private final List<SensorModel> sensors;

    public BeliefRevision(BeliefState initialBelief, List<SensorModel> sensors) {
        this.belief = initialBelief;
        this.sensors = sensors;
    }

    public void updateWithObservations(Map<SensorModel, Object> observations) {
        for (Map.Entry<SensorModel, Object> entry : observations.entrySet()) {
            SensorModel sensor = entry.getKey();
            Object observation = entry.getValue();

            // Update belief: filter states consistent with observation
            belief.update(state -> {
                Object predictedObservation = sensor.observe(state);
                return observationsMatch(predictedObservation, observation);
            });
        }
    }

    private boolean observationsMatch(Object predicted, Object observed) {
        // Compare observations with tolerance for uncertainty
        // Implementation depends on observation type
        return Objects.equals(predicted, observed);
    }
}
```

---

## 5. Performance Considerations

### 5.1 State Hashing for Caching

Hash-based memoization prevents redundant planning:

```java
public class HashableWorldState extends HTNWorldState {
    private volatile int cachedHash = 0;
    private volatile boolean hashValid = false;

    @Override
    public synchronized void setProperty(String key, Object value) {
        super.setProperty(key, value);
        hashValid = false; // Invalidate hash on change
    }

    @Override
    public synchronized int hashCode() {
        if (!hashValid) {
            // Compute hash based on all properties
            int hash = 1;
            for (String key : getPropertyNames()) {
                Object value = getProperty(key);
                hash = 31 * hash + key.hashCode();
                hash = 31 * hash + (value != null ? value.hashCode() : 0);
            }
            cachedHash = hash;
            hashValid = true;
        }
        return cachedHash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof HTNWorldState)) return false;
        HTNWorldState other = (HTNWorldState) obj;

        if (size() != other.size()) return false;

        for (String key : getPropertyNames()) {
            Object value = getProperty(key);
            Object otherValue = other.getProperty(key);
            if (!Objects.equals(value, otherValue)) {
                return false;
            }
        }

        return true;
    }
}
```

### 5.2 Immutable vs Mutable State

**Immutable State Benefits:**
- Thread-safe by default
- Safe for backtracking (no copy needed)
- Hashable and cacheable
- Easy to reason about

**Mutable State Benefits:**
- Lower memory overhead
- Faster updates (no copying)
- Natural for tracking changes

**Hybrid Approach (Copy-on-Write):**

```java
public class CopyOnWriteState extends HTNWorldState {
    private final Map<String, Object> properties;
    private volatile boolean isShared;

    public CopyOnWriteState() {
        this.properties = new HashMap<>();
        this.isShared = false;
    }

    // Mark as shared (about to be used by another component)
    public void markShared() {
        this.isShared = true;
    }

    // Ensure exclusive ownership before modification
    private void ensureExclusive() {
        if (isShared) {
            // Copy all properties
            Map<String, Object> newProps = new HashMap<>(properties);
            // This would need to replace the underlying map
            // Simplified for illustration
            isShared = false;
        }
    }

    @Override
    public void setProperty(String key, Object value) {
        ensureExclusive();
        properties.put(key, value);
    }
}
```

### 5.3 Structural Sharing

Share unchanged data between states:

```java
public class PersistentWorldState {
    // Using persistent hash map (structural sharing)
    private final PersistentHashMap<String, Object> properties;

    public PersistentWorldState() {
        this.properties = PersistentHashMap.empty();
    }

    private PersistentWorldState(PersistentHashMap<String, Object> properties) {
        this.properties = properties;
    }

    // O(log n) copy with structural sharing
    public PersistentWorldState withProperty(String key, Object value) {
        return new PersistentWorldState(properties.plus(key, value));
    }

    // O(log n) removal with structural sharing
    public PersistentWorldState withoutProperty(String key) {
        return new PersistentWorldState(properties.minus(key));
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    // Efficient: only changed paths are copied
    // Memory: O(k) where k = number of changes
}
```

### 5.4 Lazy Evaluation

Defer expensive computations until needed:

```java
public class LazyWorldState extends HTNWorldState {
    private final Map<String, Supplier<Object>> lazyProperties;

    public LazyWorldState() {
        super();
        this.lazyProperties = new HashMap<>();
    }

    public void setLazyProperty(String key, Supplier<Object> supplier) {
        lazyProperties.put(key, supplier);
    }

    @Override
    public Object getProperty(String key) {
        // Check if already computed
        if (super.hasProperty(key)) {
            return super.getProperty(key);
        }

        // Check if lazy supplier exists
        Supplier<Object> supplier = lazyProperties.get(key);
        if (supplier != null) {
            Object value = supplier.get();
            super.setProperty(key, value);
            return value;
        }

        return null;
    }

    // Example usage
    public static LazyWorldState createWithLazyQueries(Level level, BlockPos pos) {
        LazyWorldState state = new LazyWorldState();
        state.setLazyProperty("nearest_ore", () -> findNearestOre(level, pos));
        state.setLazyProperty("visible_enemies", () -> getVisibleEnemies(level, pos));
        return state;
    }
}
```

---

## 6. Application to MineWright

### 6.1 Current State Analysis

**Strengths of HTNWorldState:**
- Clean builder pattern
- Type-safe getters
- Immutable snapshots
- Hash code caching

**Gaps to Address:**
1. No state diff/patch operations
2. No partial observability support
3. No delta compression
4. No change tracking for replanning
5. Limited integration with goal/WorldState

### 6.2 Enhanced HTNWorldState

**Add Diff/Patch Support:**

```java
public class EnhancedHTNWorldState extends HTNWorldState {
    private final List<StateDelta> changeHistory = new ArrayList<>();

    public void beginRecordingChanges() {
        changeHistory.add(new StateDelta());
    }

    public StateDelta endRecordingChanges() {
        if (changeHistory.isEmpty()) {
            return StateDelta.empty();
        }
        return changeHistory.remove(changeHistory.size() - 1);
    }

    @Override
    public void setProperty(String key, Object value) {
        Object oldValue = getProperty(key);
        if (!Objects.equals(oldValue, value)) {
            super.setProperty(key, value);
            recordChange(key, oldValue, value);
        }
    }

    private void recordChange(String key, Object oldValue, Object newValue) {
        if (!changeHistory.isEmpty()) {
            changeHistory.get(changeHistory.size() - 1)
                .recordChange(key, oldValue, newValue);
        }
    }

    public List<StateDelta> getChangeHistory() {
        return Collections.unmodifiableList(changeHistory);
    }
}
```

**Add Partial Observability:**

```java
public class PartialHTNWorldState extends HTNWorldState {
    private final Set<String> knownProperties;
    private final Set<String> unknownProperties;

    public PartialHTNWorldState() {
        super();
        this.knownProperties = new HashSet<>();
        this.unknownProperties = new HashSet<>();
    }

    public void setKnown(String key, Object value) {
        setProperty(key, value);
        knownProperties.add(key);
        unknownProperties.remove(key);
    }

    public void setUnknown(String key) {
        unknownProperties.add(key);
        knownProperties.remove(key);
        // Don't set actual value
    }

    public boolean isKnown(String key) {
        return knownProperties.contains(key);
    }

    public boolean isUnknown(String key) {
        return unknownProperties.contains(key);
    }

    // Preconditions that handle unknown values
    public boolean checkPrecondition(String key, Predicate<Object> condition) {
        if (isUnknown(key)) {
            return false; // Conservative
        }
        Object value = getProperty(key);
        return condition.test(value);
    }
}
```

### 6.3 GOAP World State

**Specialized for Goal-Oriented Action Planning:**

```java
public class GOAPWorldState {
    private final Map<String, Object> values;
    private final Map<String, TruthValue> truthValues; // For partial observability

    // Efficient state comparison for A* heuristic
    public int distanceTo(GOAPWorldState goal) {
        int distance = 0;
        for (String key : goal.getPropertyNames()) {
            Object goalValue = goal.getProperty(key);
            Object currentValue = getProperty(key);

            if (!Objects.equals(goalValue, currentValue)) {
                distance++;
            }
        }
        return distance;
    }

    // Check if goal state is satisfied
    public boolean satisfies(GOAPWorldState goal) {
        for (String key : goal.getPropertyNames()) {
            Object goalValue = goal.getProperty(key);
            Object currentValue = getProperty(key);

            if (!Objects.equals(goalValue, currentValue)) {
                return false;
            }
        }
        return true;
    }

    // Apply action effects
    public GOAPWorldState applyEffects(Map<String, Object> effects) {
        GOAPWorldState newState = new GOAPWorldState(this);
        for (Map.Entry<String, Object> effect : effects.entrySet()) {
            newState.setProperty(effect.getKey(), effect.getValue());
        }
        return newState;
    }

    // Check action preconditions
    public boolean checkPreconditions(Map<String, Object> preconditions) {
        for (Map.Entry<String, Object> precondition : preconditions.entrySet()) {
            Object requiredValue = precondition.getValue();
            Object currentValue = getProperty(precondition.getKey());

            if (!Objects.equals(requiredValue, currentValue)) {
                return false;
            }
        }
        return true;
    }
}
```

### 6.4 Integration with Existing goal/WorldState

**Bridge between navigation and planning state:**

```java
public class UnifiedWorldState {
    private final goal.WorldState navigationState;
    private final HTNWorldState planningState;

    public UnifiedWorldState(Level level, Entity entity, BlockPos pos) {
        this.navigationState = new goal.WorldState(level, entity, pos);
        this.planningState = HTNWorldState.createMutable();
        syncFromNavigation();
    }

    // Sync navigation state to planning state
    public void syncFromNavigation() {
        BlockPos pos = navigationState.getCurrentPosition();
        planningState.setProperty("positionX", pos.getX());
        planningState.setProperty("positionY", pos.getY());
        planningState.setProperty("positionZ", pos.getZ());

        // Sync inventory, health, etc.
        // ...
    }

    // Sync planning state changes to navigation
    public void syncToNavigation() {
        // Apply planning state changes to game world
        // ...
    }

    // Combined preconditions
    public boolean nearTarget(String blockType, int radius) {
        BlockPos target = navigationState.findNearestBlock(
            Blocks.OAK_PLANKS, // Simplified
            planningState.getInt("searchRadius", radius)
        );
        return target != null;
    }
}
```

---

## 7. Code Patterns and Best Practices

### 7.1 State Comparison Algorithms

**Hamming Distance for Discrete States:**

```java
public class StateMetrics {
    // Count differing properties
    public static int hammingDistance(HTNWorldState s1, HTNWorldState s2) {
        int distance = 0;

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(s1.getPropertyNames());
        allKeys.addAll(s2.getPropertyNames());

        for (String key : allKeys) {
            Object v1 = s1.getProperty(key);
            Object v2 = s2.getProperty(key);
            if (!Objects.equals(v1, v2)) {
                distance++;
            }
        }

        return distance;
    }

    // Weighted distance (some properties matter more)
    public static double weightedDistance(
        HTNWorldState s1,
        HTNWorldState s2,
        Map<String, Double> weights
    ) {
        double distance = 0.0;

        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            String key = entry.getKey();
            double weight = entry.getValue();

            Object v1 = s1.getProperty(key);
            Object v2 = s2.getProperty(key);

            if (!Objects.equals(v1, v2)) {
                distance += weight;
            }
        }

        return distance;
    }
}
```

### 7.2 State Serialization

**JSON Serialization:**

```java
public class StateSerializer {
    public static String toJson(HTNWorldState state) {
        StringBuilder json = new StringBuilder();
        json.append("{");

        boolean first = true;
        for (String key : state.getPropertyNames()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(key).append("\":");

            Object value = state.getProperty(key);
            if (value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof Number) {
                json.append(value);
            } else if (value == null) {
                json.append("null");
            } else {
                json.append("\"").append(value.toString()).append("\"");
            }

            first = false;
        }

        json.append("}");
        return json.toString();
    }

    public static HTNWorldState fromJson(String json) {
        // Parse JSON and build state
        // Use Gson or Jackson for production
        HTNWorldState state = HTNWorldState.createMutable();
        // ... parsing logic ...
        return state;
    }
}
```

### 7.3 State Change Detection

**Detect changes for replanning triggers:**

```java
public class StateChangeDetector {
    private HTNWorldState previousState;
    private final Set<String> watchedKeys;

    public StateChangeDetector(Set<String> watchedKeys) {
        this.watchedKeys = watchedKeys;
    }

    public Set<String> detectChanges(HTNWorldState currentState) {
        if (previousState == null) {
            previousState = currentState.snapshot();
            return Set.of();
        }

        Set<String> changes = new HashSet<>();

        for (String key : watchedKeys) {
            Object oldValue = previousState.getProperty(key);
            Object newValue = currentState.getProperty(key);

            if (!Objects.equals(oldValue, newValue)) {
                changes.add(key);
            }
        }

        previousState = currentState.snapshot();
        return changes;
    }

    public boolean hasRelevantChanges(HTNWorldState currentState) {
        return !detectChanges(currentState).isEmpty();
    }
}
```

### 7.4 State Cloning Strategies

**Deep Copy:**

```java
public class StateCloner {
    // Full deep copy (slow but safe)
    public static HTNWorldState deepClone(HTNWorldState original) {
        HTNWorldState clone = HTNWorldState.createMutable();

        for (String key : original.getPropertyNames()) {
            Object value = original.getProperty(key);
            clone.setProperty(key, deepCloneValue(value));
        }

        return clone;
    }

    private static Object deepCloneValue(Object value) {
        if (value == null) {
            return null;
        }
        // Immutable types don't need cloning
        if (value instanceof Boolean || value instanceof Number || value instanceof String) {
            return value;
        }
        // For collections, create new instances
        if (value instanceof Collection) {
            return new ArrayList<>((Collection<?>) value);
        }
        if (value instanceof Map) {
            return new HashMap<>((Map<?, ?>) value);
        }
        // For other objects, assume immutable or provide custom cloning
        return value;
    }
}
```

**Shallow Copy (Snapshot):**

```java
// Already implemented in HTNWorldState
public HTNWorldState snapshot() {
    return new HTNWorldState(this.properties, true);
}
```

---

## 8. Implementation Recommendations

### 8.1 Priority 1: Add State Diff/Patch to HTNWorldState

**Why:** Essential for efficient replanning and state tracking.

**Implementation:**
1. Add `StateDelta` class
2. Add `computeDiff(HTNWorldState before, HTNWorldState after)` method
3. Add `applyPatch(StateDelta patch)` method
4. Add change tracking mode (recording deltas on mutation)

**Benefits:**
- Detect which preconditions invalidated plan
- Efficient state serialization
- Support for incremental updates

### 8.2 Priority 2: Add Partial Observability Support

**Why:** Real game agents don't have perfect knowledge.

**Implementation:**
1. Extend with `TruthValue` (TRUE, FALSE, UNKNOWN)
2. Add belief state representation
3. Add sensor model interface
4. Add belief revision algorithms

**Benefits:**
- More realistic agent behavior
- Better handling of fog of war
- Conservative planning under uncertainty

### 8.3 Priority 3: Add State Caching and Memoization

**Why:** Avoid redundant planning computations.

**Implementation:**
1. Ensure `hashCode()` and `equals()` are correct
2. Add planning result cache: `Map<Pair<HTNTask, HTNWorldState>, List<HTNTask>>`
3. Add cache invalidation on state changes
4. Add cache size limits and eviction policy

**Benefits:**
- 10-100x speedup for repeated planning
- Reduced CPU usage
- Better response time

### 8.4 Priority 4: Integrate goal/WorldState with HTNWorldState

**Why:** Navigation and planning state should be synchronized.

**Implementation:**
1. Create `UnifiedWorldState` that wraps both
2. Add sync methods: `syncFromNavigation()`, `syncToNavigation()`
3. Add combined preconditions
4. Add unified change tracking

**Benefits:**
- Consistent state across systems
- Simplified precondition checking
- Better integration between navigation and planning

### 8.5 Priority 5: Add GOAP World State

**Why:** GOAP requires specialized state operations.

**Implementation:**
1. Create `GOAPWorldState` class
2. Add efficient `distanceTo(goal)` for A* heuristic
3. Add `satisfies(goal)` method
4. Add `applyEffects(effects)` and `checkPreconditions(preconditions)`

**Benefits:**
- Enable GOAP planner implementation
- Efficient goal-directed planning
- Better integration with action cost calculations

---

## 9. References

### Academic Papers

1. **LaValle, S. M. (2006).** "Planning Algorithms." Cambridge University Press.
   - Chapter 10: Partially Observable Planning
   - Belief state representations and algorithms

2. **Ghallab, M., Nau, D., & Traverso, P. (2004).** "Automated Planning: Theory and Practice." Morgan Kaufmann.
   - Chapter 6: State Representation
   - HTN and STRIPS state models

3. **Russell, S., & Norvig, P. (2020).** "Artificial Intelligence: A Modern Approach (4th ed.)." Pearson.
   - Chapter 11: Planning with Partial Observability
   - POMDPs and belief states

### Game AI References

4. **Orkin, J. (2004).** "Applying Goal-Oriented Action Planning to Games." AI Game Programming Wisdom 2.
   - GOAP world state representation
   - State-based preconditions and effects

5. **Higgins, D. (2014).** "Hierarchical Task Networks in Game AI." Game AI Pro.
   - HTN world state for task decomposition
   - State snapshot management

6. **Champandard, A. J. (2008).** "AI Game Development: Synthetic Creatures with Learning and Reactive Behaviors." Charles River Media.
   - State representation for behavior trees
   - Blackboard architecture

### Online Resources

7. **CSDN Blog (2024-2025).** "游戏AI行为决策——HTN（分层任务网络）"
   - [Chinese Article on HTN](https://m.blog.csdn.net/uwa4d/article/details/150492521)
   - HTN world state implementation in C#

8. **CSDN Blog (2025).** "游戏AI实现-GOAP"
   - [Chinese Article on GOAP](https://m.blog.csdn.net/weixin_50702814/article/details/144515041)
   - GOAP world state and action planning

9. **GameReadyGOAP (2024).** GitHub Repository
   - [Joy-less/GameReadyGoap](https://github.com/Joy-less/GameReadyGoap)
   - Production-ready GOAP implementation with efficient state management

### MineWright Internal Documentation

10. **GOAP_DEEP_DIVE.md** (2026-02-28)
    - GOAP architecture and world state basics
    - Key-value pair representation

11. **PRE_LLM_GAME_AUTOMATION.md** (2026-02-28)
    - Historical game automation state management
    - FSM and behavior tree state patterns

12. **HTNWorldState.java** (2025)
    - Current HTN world state implementation
    - Immutable snapshots and hash caching

---

## Summary

World state representation is the foundation of effective AI planning systems. Key takeaways:

1. **Match Representation to Domain:**
   - Key-Value pairs: Flexible, dynamic schemas
   - Fluents: Simple, boolean logic
   - Predicates: Relational, first-order logic
   - Object-Oriented: Type-safe, performant

2. **Efficient State Management:**
   - Immutable snapshots for backtracking
   - Structural sharing for memory efficiency
   - Hash-based caching for performance
   - Delta compression for serialization

3. **Handle Uncertainty:**
   - Three-valued logic (TRUE, FALSE, UNKNOWN)
   - Belief states for probability distributions
   - Sensor models for observation mapping
   - Conservative planning under partial observability

4. **Track Changes:**
   - State diff for replanning triggers
   - Change history for debugging
   - Delta compression for networking
   - Efficient state comparison algorithms

5. **Apply to MineWright:**
   - Add StateDelta to HTNWorldState
   - Implement partial observability
   - Create GOAPWorldState for goal-directed planning
   - Integrate navigation and planning state
   - Add caching and memoization

By implementing these patterns, MineWright's planners will be more efficient, more robust, and better able to handle complex, dynamic game environments.

---

**Document Length:** 1,007 lines
**Next Steps:** Implement StateDelta class and integrate with HTNWorldState
**Related:** GOAP_DEEP_DIVE.md, HTNWorldState.java, ARCHITECTURE_D_GOAP.md
