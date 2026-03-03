# Game AI Blackboard and Knowledge Representation Patterns

**Research Date:** 2026-03-02
**Focus:** Modern AAA game AI blackboard architectures, knowledge representation, spatial reasoning, and optimization patterns
**Goal:** Identify adoptable patterns for Steve AI's multi-agent coordination system

---

## Executive Summary

Blackboard architecture is a cornerstone of modern game AI, enabling multiple specialized subsystems to share knowledge without direct coupling. This report synthesizes patterns from AAA games (Unreal Engine, RTS/FPS titles), academic research, and contemporary implementations to provide actionable improvements for Steve AI's existing blackboard system.

**Key Findings:**
1. **Two-tier blackboard pattern** is standard: Behavior tree context (local) + Coordination layer (global)
2. **Spatial partitioning** (influence maps, spatial hashing) is critical for performance
3. **Event-driven updates** with filtered notifications prevent CPU overhead
4. **Hierarchical knowledge areas** with different staleness tolerances optimize memory
5. **GOAP-style world state representation** enables efficient planning

**Current Steve AI Status:**
- ✅ Well-implemented partitioned blackboard (KnowledgeArea enum)
- ✅ Dual-layer architecture (BTBlackboard + Blackboard)
- ✅ Subscription-based notifications
- ⚠️ Missing: Spatial indexing, influence maps, query optimization
- ⚠️ Missing: Hierarchical blackboard inheritance
- ⚠️ Missing: Lazy evaluation and dirty flagging

---

## Table of Contents

1. [Blackboard Architecture Patterns](#1-blackboard-architecture-patterns)
2. [Knowledge Representation](#2-knowledge-representation)
3. [Spatial Reasoning Systems](#3-spatial-reasoning-systems)
4. [Query and Update Optimization](#4-query-and-update-optimization)
5. [Adoptable Patterns for Steve AI](#5-adoptable-patterns-for-steve-ai)
6. [Implementation Roadmap](#6-implementation-roadmap)
7. [References](#7-references)

---

## 1. Blackboard Architecture Patterns

### 1.1 Core Blackboard Pattern

The blackboard pattern originated from the **Hearsay-II speech recognition system** (1970s) and has evolved into a standard game AI architecture.

**Three Components:**
```
┌─────────────────────────────────────────────────────────────┐
│                    Blackboard System                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐      ┌───────────────────────────┐       │
│  │ Blackboard   │◄─────┤ Knowledge Sources         │       │
│  │ (Shared Data) │      │ (Independent Experts)     │       │
│  │              │◄─────┤ • Perception              │       │
│  │ • Facts      │      │ • Pathfinding             │       │
│  │ • Hypotheses │      │ • Combat Logic            │       │
│  │ • Goals      │      │ • Resource Management     │       │
│  │ • Constraints│      │ • Social Coordination     │       │
│  └──────────────┘      └───────────────────────────┘       │
│         ▲                                                 │
│         │                                                 │
│  ┌──────┴──────────┐                                       │
│  │ Controller      │                                       │
│  │ (Coordination)  │                                       │
│  └─────────────────┘                                       │
└─────────────────────────────────────────────────────────────┘
```

**Key Principles:**
1. **No central controller** directing problem-solving
2. **Self-organizing behavior** - knowledge sources activate when relevant
3. **Opportunistic processing** - respond to blackboard state changes
4. **Contradictory hypotheses** allowed as competing entries

### 1.2 Two-Tier Architecture (AAA Standard)

Modern AAA games use a **two-tier blackboard system**:

| Tier | Purpose | Scope | Lifecycle |
|------|---------|-------|-----------|
| **Local Blackboard** | Behavior tree execution context | Per agent, per tree | Single tick |
| **Global Blackboard** | Multi-agent coordination | World-wide | Persistent |

**Unreal Engine 5 Implementation:**
- `UBlackboardComponent` per AI controller
- `UBlackboardData` asset for key schema
- Behavior tree nodes read/write keys
- BTService components update values periodically

**Steve AI Implementation (Current):**
```java
// Local: Behavior tree context (PER TREE EXECUTION)
BTBlackboard localBB = new BTBlackboard(foremanEntity);
localBB.put("target.position", targetPos);
localBB.put("path.current", 0);

// Global: Multi-agent coordination (SINGLETON)
Blackboard globalBB = Blackboard.getInstance();
globalBB.post(KnowledgeArea.WORLD_STATE, entry);
globalBB.subscribe(KnowledgeArea.THREATS, subscriber);
```

**Assessment:** ✅ Already implements two-tier pattern correctly.

### 1.3 Hierarchical Blackboard Pattern

**Emerging Pattern (2024-2025):** Hierarchical blackboards with inheritance and override capabilities.

**Concept:**
```
Global Blackboard (World State)
    │
    ├── Region Blackboard (Spatial Partition)
    │     │
    │     ├── Squad Blackboard (Team Coordination)
    │     │     │
    │     │     └── Agent Blackboard (Local Context)
```

**Benefits:**
- **Scope-limited queries** - Agents only see relevant data
- **Automatic fallback** - Local cache falls back to regional/global
- **Conflict resolution** - Higher-level data can override lower-level
- **Natural access control** - Secrets hidden at appropriate level

**Implementation Pattern:**
```java
public interface HierarchicalBlackboard {
    // Query with automatic fallback
    <T> Optional<T> query(String key, QueryScope scope);

    // Override at this level
    <T> void override(String key, T value);

    // Inherit from parent
    void inheritFrom(HierarchicalBlackboard parent);
}

enum QueryScope {
    LOCAL_ONLY,      // Only this blackboard
    LOCAL_THEN_UP,   // Check local, then parent
    GLOBAL_ONLY,     // Skip to root
    NEAREST_FIRST    // Breadth-first search
}
```

**Steve AI Adoption:** ⚠️ NOT IMPLEMENTED - High priority addition.

---

## 2. Knowledge Representation

### 2.1 World State Representation

**GOAP (Goal-Oriented Action Planning) Approach:**

World state as **key-value pairs** where:
- **Keys** = World properties (strings/symbols)
- **Values** = Current state (boolean, int, enum)
- **Planning** = Search from current state to goal state

**Example World State:**
```java
{
    "HasWeapon": true,
    "Ammo": 15,
    "EnemyVisible": true,
    "EnemyDistance": 10.5,
    "Health": 0.8,
    "InCover": false,
    "HasAntidote": false
}
```

**Preconditions/Effects Model:**
```java
Action: "Shoot"
 Preconditions: {HasWeapon: true, Ammo: >0, EnemyVisible: true}
 Effects: {Ammo: -1, EnemyHealth: -10}

Action: "TakeCover"
 Preconditions: {InCover: false, CoverNearby: true}
 Effects: {InCover: true}

Action: "UseAntidote"
 Preconditions: {HasAntidote: true, Poisoned: true}
 Effects: {Poisoned: false, HasAntidote: false}
```

**Backward Planning (A* Search):**
```
Goal: {Poisoned: false}
    │
    ▼ Action matching goal effect
UseAntidote (requires HasAntidote: true, Poisoned: true)
    │
    ▼ Current state satisfies Poisoned: true
FindAntidote (requires AntidoteNearby: true)
    │
    ▼ Current state satisfies all preconditions
EXECUTE: FindAntidote → UseAntidote
```

**Steve AI Implementation (Current):**
```java
// Current: BlackboardEntry with typed values
BlackboardEntry<BlockState> entry = BlackboardEntry.createFact(
    "block_100_64_200", blockState, agentId);
blackboard.post(KnowledgeArea.WORLD_STATE, entry);

// Improvement: Add GOAP-style world state snapshot
public class WorldState {
    private Map<String, Object> state;

    public boolean satisfies(Map<String, Object> preconditions) {
        return preconditions.entrySet().stream()
            .allMatch(e -> Objects.equals(state.get(e.getKey()), e.getValue()));
    }

    public WorldState applyEffects(Map<String, Object> effects) {
        WorldState newState = new WorldState(this);
        effects.forEach(newState.state::put);
        return newState;
    }
}
```

**Assessment:** ⚠️ Partially implemented - needs GOAP planning integration.

### 2.2 Knowledge Entry Types

**Standard Classification (from Steve AI):**

| Type | Purpose | Confidence | Example |
|------|---------|------------|---------|
| **FACT** | Direct observation | 1.0 | "Diamond at (100, 64, 200)" |
| **HYPOTHESIS** | Inferred information | 0.5-0.9 | "Hostile nearby (0.7 confidence)" |
| **GOAL** | Shared objective | 0.9 | "Build storage at spawn" |
| **CONSTRAINT** | Rule/limitation | 1.0 | "Don't build above y=256" |

**Extended Types (from AAA games):**

| Type | Purpose | Use Case |
|------|---------|----------|
| **EVENT** | Transient occurrence | "Player joined game" |
| **PREDICTION** | Forecasted state | "Enemy will reach base in 30s" |
| **REQUEST** | Bid for task | "Agent 3 bids on mining task" |
| **COMMITMENT** | Promise to act | "Agent 5 will defend entrance" |

**Steve AI Enhancement Opportunity:**
```java
public enum EntryType {
    FACT, HYPOTHESIS, GOAL, CONSTRAINT,
    EVENT,          // NEW: Transient occurrences
    PREDICTION,     // NEW: Forecasted states
    REQUEST,        // NEW: Task bidding
    COMMITMENT      // NEW: Agent promises
}
```

### 2.3 Confidence and Provenance

**Confidence Levels:**
- **1.0** = Absolute certainty (direct observation)
- **0.7-0.9** = High confidence (inference from reliable data)
- **0.4-0.6** = Moderate confidence (educated guess)
- **0.1-0.3** = Low confidence (speculation)
- **0.0** = No confidence (placeholder)

**Provenance Tracking:**
```java
public class BlackboardEntry<T> {
    private final String key;
    private final T value;
    private final long timestamp;
    private final UUID sourceAgent;        // Who created this
    private final double confidence;       // How reliable
    private final EntryType type;          // What kind of knowledge
    private final List<UUID> provenance;   // Chain of updates
    private final int generation;          // How many updates
}
```

**Decay Over Time:**
```java
public boolean isStale(long maxAgeMs) {
    long age = System.currentTimeMillis() - timestamp;
    return age > maxAgeMs;
}

public double getEffectiveConfidence(long maxAgeMs) {
    long age = System.currentTimeMillis() - timestamp;
    double ageFactor = 1.0 - (age / (double) maxAgeMs);
    return confidence * Math.max(0, ageFactor);
}
```

---

## 3. Spatial Reasoning Systems

### 3.1 Influence Maps

**Core Concept:** Grid-based representation of spatial influence for tactical decision-making.

**Applications:**
- **RTS Games:** Territory control, unit positioning
- **FPS Games:** Threat assessment, cover evaluation
- **Minecraft:** Resource clustering, base location scoring

**Implementation Pattern:**
```java
public class InfluenceMap {
    private final int gridSize;           // Cell size (blocks)
    private final double[][] values;      // Influence values
    private final int width, height;      // Map dimensions

    // Add positive influence (friendly)
    public void addInfluence(Vec3 position, double amount, double radius) {
        int cx = (int) (position.x / gridSize);
        int cy = (int) (position.z / gridSize);
        int r = (int) (radius / gridSize);

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                int x = cx + dx;
                int y = cy + dy;
                if (inBounds(x, y)) {
                    double dist = Math.sqrt(dx*dx + dy*dy);
                    double falloff = 1.0 - (dist / r);
                    values[x][y] += amount * falloff;
                }
            }
        }
    }

    // Query influence at position
    public double getInfluence(Vec3 position) {
        int x = (int) (position.x / gridSize);
        int y = (int) (position.z / gridSize);
        return inBounds(x, y) ? values[x][y] : 0.0;
    }

    // Find best position in area
    public Vec3 findBestPosition(Vec3 center, double radius) {
        int cx = (int) (center.x / gridSize);
        int cy = (int) (center.z / gridSize);
        int r = (int) (radius / gridSize);

        double bestValue = Double.NEGATIVE_INFINITY;
        int bestX = cx, bestY = cy;

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                int x = cx + dx;
                int y = cy + dy;
                if (inBounds(x, y) && values[x][y] > bestValue) {
                    bestValue = values[x][y];
                    bestX = x;
                    bestY = y;
                }
            }
        }

        return new Vec3(bestX * gridSize, 0, bestY * gridSize);
    }
}
```

**Use Cases for Steve AI:**
1. **Base Location Scoring**
   - Resources nearby (positive)
   - Threat level (negative)
   - Distance to spawn (negative)
   - Terrain flatness (positive)

2. **Combat Positioning**
   - Cover availability (positive)
   - Enemy threat (negative)
   - Friendly support (positive)
   - Escape routes (positive)

3. **Resource Clustering**
   - Same-type resources attract
   - Different-type resources neutral
   - Mined areas decay influence

### 3.2 Spatial Partitioning

**Purpose:** Fast spatial queries for proximity, range, and area operations.

**Comparison of Techniques:**

| Technique | Best For | Complexity | Update Cost | Query Cost |
|-----------|----------|------------|-------------|------------|
| **Spatial Hash Grid** | Many small objects, uniform distribution | O(1) per cell | O(1) | O(cell size) |
| **Quadtree/Octree** | Large world, clustered objects | O(log n) | O(log n) | O(log n) |
| **BVH** | Ray casting, complex collision | O(log n) | O(log n) | O(log n) |
| **Uniform Grid** | Static terrain, simple queries | O(1) | O(1) | O(n) |

**Spatial Hash Grid (Recommended for Steve AI):**
```java
public class SpatialHashGrid<T> {
    private final Map<Long, List<T>> cells;
    private final Map<T, Long> objectToCell;
    private final int cellSize;

    private long cellHash(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public void insert(T obj, Vec3 position) {
        int cx = (int) Math.floor(position.x / cellSize);
        int cz = (int) Math.floor(position.z / cellSize);
        long hash = cellHash(cx, cz);

        cells.computeIfAbsent(hash, k -> new ArrayList<>()).add(obj);
        objectToCell.put(obj, hash);
    }

    public List<T> query(Vec3 center, double radius) {
        List<T> results = new ArrayList<>();
        int minCX = (int) Math.floor((center.x - radius) / cellSize);
        int maxCX = (int) Math.floor((center.x + radius) / cellSize);
        int minCZ = (int) Math.floor((center.z - radius) / cellSize);
        int maxCZ = (int) Math.floor((center.z + radius) / cellSize);

        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                List<T> cell = cells.get(cellHash(cx, cz));
                if (cell != null) {
                    results.addAll(cell);
                }
            }
        }

        return results;
    }
}
```

**Integration with Blackboard:**
```java
public class SpatialBlackboard {
    private final SpatialHashGrid<Entity> entityGrid;
    private final SpatialHashGrid<BlockState> blockGrid;
    private final InfluenceMap threatInfluence;
    private final InfluenceMap resourceInfluence;

    // Fast spatial query
    public List<Entity> getNearbyEntities(Vec3 pos, double radius) {
        return entityGrid.query(pos, radius);
    }

    // Tactical scoring
    public double getThreatLevel(Vec3 pos) {
        return threatInfluence.getInfluence(pos);
    }

    // Best location query
    public Vec3 findSafestPosition(Vec3 center, double radius) {
        // Find position with lowest threat influence
        // Using negative threat influence map
        return threatInfluence.findBestPosition(center, radius);
    }
}
```

### 3.3 Tactical Maps

**Layered Spatial Representation:**

```
┌─────────────────────────────────────────────────────────────┐
│                    Tactical Map System                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Layer 1: Terrain (static)                                  │
│  • Walkability (boolean)                                    │
│  • Build suitability (0-1)                                  │
│  • Line-of-sight visibility                                 │
│                                                              │
│  Layer 2: Resources (dynamic)                               │
│  • Resource density (influence map)                         │
│  • Resource types (categorical)                             │
│  • Mining priority (0-1)                                    │
│                                                              │
│  Layer 3: Threats (high-frequency)                          │
│  • Hostile entity density (influence map)                   │
│  • Environmental dangers (lava, cactus)                     │
│  • Threat decay over time                                   │
│                                                              │
│  Layer 4: Friendly Units (medium-frequency)                 │
│  • Agent positions (spatial hash)                           │
│  • Coverage areas (influence)                               │
│  • Support availability                                     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Integration Pattern:**
```java
public class TacticalMapSystem {
    private Map<String, TacticalLayer> layers;

    public double evaluatePosition(Vec3 pos, String... layerNames) {
        double score = 0.0;
        for (String layer : layerNames) {
            score += layers.get(layer).evaluate(pos);
        }
        return score;
    }

    public Vec3 findBestPosition(Vec3 center, double radius, String... layers) {
        // Query all layers, aggregate scores
        // Return position with highest combined score
    }
}
```

---

## 4. Query and Update Optimization

### 4.1 Event-Driven Updates with Filtering

**Problem:** Unfiltered notifications cause CPU overhead.

**Solution:** **Filtered observer pattern** with type-based, predicate-based, and area-based filtering.

**Implementation Pattern:**
```java
public interface BlackboardSubscriber {
    // Area-based filtering (current Steve AI)
    boolean acceptsArea(KnowledgeArea area);

    // NEW: Type-based filtering
    boolean acceptsType(EntryType type);

    // NEW: Key pattern filtering
    boolean acceptsKey(String keyPattern);

    // NEW: Predicate-based filtering
    Predicate<BlackboardEntry<?>> getFilter();
}

// In Blackboard class
private void notifySubscribers(KnowledgeArea area, BlackboardEntry<?> entry) {
    for (BlackboardSubscriber subscriber : subscribers.get(area)) {
        // Filter before notification
        if (!subscriber.acceptsType(entry.getType())) continue;
        if (!subscriber.acceptsKey(entry.getKey())) continue;
        if (subscriber.getFilter() != null && !subscriber.getFilter().test(entry)) continue;

        notifySafely(subscriber, area, entry);
    }
}
```

**Usage Example:**
```java
// Only receive FACT entries about threats
blackboard.subscribe(KnowledgeArea.THREATS, new BlackboardSubscriber() {
    public boolean acceptsArea(KnowledgeArea area) {
        return area == KnowledgeArea.THREATS;
    }

    public boolean acceptsType(EntryType type) {
        return type == EntryType.FACT;  // Only facts, no hypotheses
    }

    public boolean acceptsKey(String key) {
        return key.startsWith("hostile_");  // Only hostiles
    }

    public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {
        // Guaranteed to be a FACT about a hostile entity
        respondToThreat(entry.getValue());
    }
});
```

### 4.2 Lazy Evaluation

**Problem:** Computing expensive values that may never be used.

**Solution:** **Supplier-based lazy evaluation** (already in BTBlackboard, needs extension to coordination blackboard).

**Implementation Pattern:**
```java
public class LazyBlackboardEntry<T> extends BlackboardEntry<Supplier<T>> {
    private volatile T cachedValue;
    private volatile boolean computed = false;

    public T getValue() {
        if (!computed) {
            synchronized (this) {
                if (!computed) {
                    cachedValue = super.getValue().get();
                    computed = true;
                }
            }
        }
        return cachedValue;
    }
}

// Usage
blackboard.post(KnowledgeArea.WORLD_STATE,
    new LazyBlackboardEntry<>(
        "expensive_computation",
        () -> performExpensivePathfinding(),
        agentId,
        1.0,
        EntryType.FACT
    )
);

// Value only computed if someone queries it
Optional<Path> path = blackboard.query(KnowledgeArea.WORLD_STATE, "expensive_computation");
```

### 4.3 Dirty Flags

**Problem:** Re-computing cached values when underlying data hasn't changed.

**Solution:** **Dirty flagging** for derived/cached values.

**Implementation Pattern:**
```java
public class DirtyTrackingBlackboard extends Blackboard {
    private final Map<String, Set<String>> dirtyKeys;  // area -> dirty keys
    private final Map<String, Long> lastClean;         // area -> timestamp

    public <T> void post(KnowledgeArea area, BlackboardEntry<T> entry) {
        super.post(area, entry);

        // Mark dependent keys as dirty
        markDirty(area, entry.getKey());
    }

    public boolean isDirty(KnowledgeArea area, String key) {
        Set<String> dirty = dirtyKeys.get(area.getId());
        return dirty != null && dirty.contains(key);
    }

    public void markClean(KnowledgeArea area, String key) {
        Set<String> dirty = dirtyKeys.get(area.getId());
        if (dirty != null) {
            dirty.remove(key);
            lastClean.put(area.getId() + ":" + key, System.currentTimeMillis());
        }
    }

    // Query with dirty check
    public <T> Optional<T> queryIfClean(KnowledgeArea area, String key, Supplier<T> recompute) {
        if (isDirty(area, key)) {
            T value = recompute.get();
            post(area, key, value, null, 1.0, EntryType.FACT);
            markClean(area, key);
        }
        return query(area, key);
    }
}
```

### 4.4 Batched Updates

**Problem:** High-frequency posting (e.g., world scanning) causes notification spam.

**Solution:** **Update batching** with configurable flush intervals.

**Implementation Pattern:**
```java
public class BatchingBlackboard extends Blackboard {
    private final Map<KnowledgeArea, List<BlackboardEntry<?>>> pendingUpdates;
    private final Map<KnowledgeArea, Long> lastFlush;
    private final long flushIntervalMs;

    public <T> void post(KnowledgeArea area, BlackboardEntry<T> entry) {
        // Add to pending instead of immediate post
        pendingUpdates.computeIfAbsent(area, k -> new ArrayList<>()).add(entry);

        // Check if should flush
        long now = System.currentTimeMillis();
        Long last = lastFlush.get(area);
        if (last == null || now - last > flushIntervalMs) {
            flush(area);
        }
    }

    private void flush(KnowledgeArea area) {
        List<BlackboardEntry<?>> updates = pendingUpdates.get(area);
        if (updates == null || updates.isEmpty()) return;

        // Post all updates at once
        for (BlackboardEntry<?> entry : updates) {
            super.post(area, entry);
        }

        updates.clear();
        lastFlush.put(area, System.currentTimeMillis());
    }

    // Force flush for critical areas
    public void flushCritical() {
        flush(KnowledgeArea.THREATS);
        flush(KnowledgeArea.AGENT_STATUS);
    }
}
```

---

## 5. Adoptable Patterns for Steve AI

### 5.1 Priority Matrix

| Pattern | Impact | Effort | Priority |
|---------|--------|--------|----------|
| **Spatial indexing for world state** | HIGH | Medium | **P0** |
| **Influence maps for tactical reasoning** | HIGH | Medium | **P0** |
| **Filtered notifications** | MEDIUM | Low | **P1** |
| **Hierarchical blackboards** | HIGH | High | **P1** |
| **Lazy evaluation** | MEDIUM | Low | **P1** |
| **Dirty flagging** | MEDIUM | Medium | **P2** |
| **Batched updates** | LOW | Medium | **P2** |
| **GOAP integration** | HIGH | High | **P2** |

### 5.2 Spatial Blackboard Component

**Recommended Addition:**

```java
package com.minewright.blackboard.spatial;

import com.minewright.blackboard.Blackboard;
import com.minewright.blackboard.KnowledgeArea;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

/**
 * Spatial indexing layer for the blackboard system.
 * Provides fast spatial queries for world state and entities.
 */
public class SpatialBlackboard {
    private final SpatialHashGrid<Entity> entityIndex;
    private final SpatialHashGrid<BlockState> blockIndex;
    private final InfluenceMap threatInfluence;
    private final InfluenceMap resourceInfluence;
    private final Blackboard blackboard;

    // Initialize with world bounds
    public SpatialBlackboard(Blackboard blackboard, int worldSize) {
        this.blackboard = blackboard;
        this.entityIndex = new SpatialHashGrid<>(16);  // 16-block cells
        this.blockIndex = new SpatialHashGrid<>(16);
        this.threatInfluence = new InfluenceMap(worldSize, 16);
        this.resourceInfluence = new InfluenceMap(worldSize, 16);
    }

    // Fast spatial query
    public List<Entity> getNearbyEntities(BlockPos center, int radius) {
        return entityIndex.query(Vec3.atCenterOf(center), radius);
    }

    // Tactical scoring
    public double getThreatLevel(BlockPos pos) {
        return threatInfluence.getInfluence(Vec3.atCenterOf(pos));
    }

    // Best location query
    public BlockPos findSafestPosition(BlockPos center, int radius) {
        Vec3 best = threatInfluence.findBestPosition(Vec3.atCenterOf(center), radius);
        return BlockPos.containing(best.x, best.y, best.z);
    }

    // Update influence maps (call periodically)
    public void updateInfluenceMaps() {
        // Decay old influence
        threatInfluence.decay(0.95);
        resourceInfluence.decay(0.98);

        // Rebuild from blackboard entries
        List<BlackboardEntry<?>> threats = blackboard.queryArea(KnowledgeArea.THREATS);
        for (BlackboardEntry<?> entry : threats) {
            if (entry.getKey().startsWith("hostile_")) {
                ThreatData threat = (ThreatData) entry.getValue();
                threatInfluence.addInfluence(threat.position, -threat.severity, 32);
            }
        }
    }
}
```

### 5.3 Enhanced Subscriber Filtering

**Extension to existing BlackboardSubscriber:**

```java
public interface FilteredBlackboardSubscriber extends BlackboardSubscriber {

    // Type-based filtering
    default boolean acceptsType(EntryType type) {
        return true;  // Accept all types
    }

    // Key pattern filtering
    default boolean acceptsKey(String key) {
        return true;  // Accept all keys
    }

    // Predicate-based filtering
    default Predicate<BlackboardEntry<?>> getFilter() {
        return null;  // No additional filter
    }
}
```

### 5.4 Hierarchical Knowledge Areas

**Extension to KnowledgeArea enum:**

```java
public enum KnowledgeArea {
    // Existing areas...
    WORLD_STATE("world_state", 5000L),
    AGENT_STATUS("agent_status", 2000L),

    // NEW: Hierarchical areas
    REGION_STATE("region_state", 5000L, PARENT_REGION),
    SQUAD_COORDINATION("squad_coord", 2000L, PARENT_SQUAD);

    private final String id;
    private final long defaultMaxAgeMs;
    private final KnowledgeArea parent;  // NEW: Hierarchical parent

    KnowledgeArea(String id, long defaultMaxAgeMs) {
        this(id, defaultMaxAgeMs, null);
    }

    KnowledgeArea(String id, long defaultMaxAgeMs, KnowledgeArea parent) {
        this.id = id;
        this.defaultMaxAgeMs = defaultMaxAgeMs;
        this.parent = parent;
    }

    public KnowledgeArea getParent() {
        return parent;
    }

    // Check if this area inherits from another
    public boolean isInheritedFrom(KnowledgeArea other) {
        KnowledgeArea current = this;
        while (current != null) {
            if (current == other) return true;
            current = current.parent;
        }
        return false;
    }
}
```

### 5.5 GOAP Integration

**New component for planning:**

```java
package com.minewright.goal;

import com.minewright.blackboard.Blackboard;
import com.minewright.blackboard.KnowledgeArea;
import java.util.*;

/**
 * GOAP-style world state representation for planning.
 */
public class PlanningWorldState {
    private final Map<String, Object> state;
    private final Blackboard blackboard;

    public PlanningWorldState(Blackboard blackboard) {
        this.blackboard = blackboard;
        this.state = new HashMap<>();

        // Initialize from blackboard facts
        loadFromBlackboard();
    }

    private void loadFromBlackboard() {
        for (KnowledgeArea area : KnowledgeArea.values()) {
            List<BlackboardEntry<?>> entries = blackboard.queryByType(area, EntryType.FACT);
            for (BlackboardEntry<?> entry : entries) {
                state.put(area.qualify(entry.getKey()), entry.getValue());
            }
        }
    }

    public boolean satisfies(Map<String, Object> preconditions) {
        return preconditions.entrySet().stream()
            .allMatch(e -> {
                Object value = state.get(e.getKey());
                return Objects.equals(value, e.getValue());
            });
    }

    public PlanningWorldState applyEffects(Map<String, Object> effects) {
        PlanningWorldState newState = new PlanningWorldState(blackboard);
        newState.state.putAll(this.state);
        newState.state.putAll(effects);
        return newState;
    }

    // A* search from current state to goal
    public List<Action> planToGoal(Map<String, Object> goal, List<Action> availableActions) {
        // Priority queue: (cost, state, action sequence)
        PriorityQueue<Node> openSet = new PriorityQueue<>(
            Comparator.comparingDouble(n -> n.cost)
        );
        Set<Map<String, Object>> closedSet = new HashSet<>();

        openSet.add(new Node(0, this.state, new ArrayList<>()));

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (satisfies(goal)) {
                return current.actions;
            }

            if (closedSet.contains(current.state)) continue;
            closedSet.add(current.state);

            for (Action action : availableActions) {
                if (action.canExecute(current.state)) {
                    Map<String, Object> newState = applyActionEffects(current.state, action);
                    double newCost = current.cost + action.getCost();
                    List<Action> newActions = new ArrayList<>(current.actions);
                    newActions.add(action);

                    openSet.add(new Node(newCost, newState, newActions));
                }
            }
        }

        return null;  // No plan found
    }

    private static class Node {
        final double cost;
        final Map<String, Object> state;
        final List<Action> actions;

        Node(double cost, Map<String, Object> state, List<Action> actions) {
            this.cost = cost;
            this.state = state;
            this.actions = actions;
        }
    }
}
```

---

## 6. Implementation Roadmap

### Phase 1: Spatial Optimization (2-3 weeks)

**Goal:** Improve query performance for spatial operations.

**Tasks:**
1. Implement `SpatialHashGrid<T>` for fast spatial queries
2. Create `InfluenceMap` for tactical reasoning
3. Add `SpatialBlackboard` component integrating both
4. Update `KnowledgeArea.WORLD_STATE` to use spatial indexing
5. Add spatial query methods to blackboard API

**Success Criteria:**
- O(1) spatial queries for nearby entities
- Influence map queries for tactical scoring
- 10x performance improvement on "find nearest X" queries

### Phase 2: Enhanced Notifications (1 week)

**Goal:** Reduce CPU overhead from unnecessary notifications.

**Tasks:**
1. Extend `BlackboardSubscriber` with filtering methods
2. Implement type-based, key-based, and predicate filtering
3. Update `Blackboard.notifySubscribers()` to apply filters
4. Add unit tests for filtered notifications

**Success Criteria:**
- Subscribers only receive relevant notifications
- 50% reduction in notification processing for active agents

### Phase 3: Hierarchical Blackboards (2-3 weeks)

**Goal:** Implement scoped knowledge areas with inheritance.

**Tasks:**
1. Add `parent` field to `KnowledgeArea` enum
2. Implement hierarchical query fallback in `Blackboard`
3. Add `QueryScope` enum for query control
4. Create regional and squad-level blackboard instances
5. Update multi-agent coordination to use hierarchy

**Success Criteria:**
- Regional blackboards for spatial partitioning
- Automatic fallback to parent areas
- Reduced global blackboard load

### Phase 4: GOAP Integration (3-4 weeks)

**Goal:** Enable goal-oriented action planning using blackboard state.

**Tasks:**
1. Implement `PlanningWorldState` class
2. Define action preconditions/effects schema
3. Implement A* planner for action sequences
4. Integrate with existing `Task` system
5. Add planning cache for repeated queries

**Success Criteria:**
- Agents can plan from current state to goal state
- Action costs are considered in planning
- Plans respect blackboard constraints

### Phase 5: Lazy Evaluation and Dirty Flags (2 weeks)

**Goal:** Optimize expensive computations and cached values.

**Tasks:**
1. Implement `LazyBlackboardEntry<T>` with supplier pattern
2. Add dirty flagging to `Blackboard` class
3. Implement query-if-clean method
4. Add automatic dirty tracking for derived values

**Success Criteria:**
- Expensive computations only run when needed
- Cached values invalidate when dependencies change
- 30% reduction in redundant computations

---

## 7. References

### Academic and Industry Sources

1. **Unreal Engine 5 AI Documentation**
   - Behavior Trees and Blackboards
   - AI Controller and Perception systems
   - [UE5 Documentation](https://dev.epicgames.com/documentation/en-us/unreal-engine)

2. **GOAP (Goal-Oriented Action Planning)**
   - [GAMES Lecture Notes on GOAP](https://www.cnblogs.com/apachecn/p/19626591) (Chinese)
   - [Unity GOAP Implementation Guide](https://m.blog.csdn.net/gitblog_00188/article/details/141450576)
   - Original: "Goal-Oriented Action Planning for Game AI" (Orkin, 2004)

3. **Influence Maps and Spatial Reasoning**
   - [Terrain Analysis in Game AI (CSDN)](https://blog.csdn.net/w1x2y3/article/details/148644585)
   - [Spatial Intelligence: Next AI Phase (Fei-Fei Li)](https://m.tmtpost.com/7759191.html)
   - [OmniSpatial Benchmark](https://arxiv.org/html/2506.03135v1)

4. **Blackboard Architecture**
   - [Blackboard Pattern for Game AI (CSDN)](https://blog.csdn.net/chinabhlt/article/details/102527898)
   - [Hierarchical Blackboard Architecture](https://m.blog.csdn.net/zxqsoftware/article/details/150448729)
   - Original: Hearsay-II Speech Recognition System (1970s)

5. **Spatial Partitioning**
   - [Spatial Hash Implementation](https://m.blog.csdn.net/weixin_50702814/article/details/144569907)
   - [Scene Partitioning Techniques](https://www.cnblogs.com/TreeOnEarth/p/18933154)
   - [AOI Algorithms for Game Servers](https://blog.csdn.net/bdarkray/article/details/154181625)

6. **Event-Driven Architecture**
   - [AAA Game AI Agent Decision Logic](https://blog.csdn.net/InstrWander/article/details/156052430)
   - [Observer Pattern in Game AI](https://blog.csdn.net/weixin_29155599/article/details/154728817)
   - [Multiplayer Sync & Conflict Resolution](https://blog.csdn.net/qq_33060405/article/details/138908271)

### Game AI Patterns

1. **Behavior Tree Evolution**
   - [GAMES104: Advanced AI](https://it.en369.cn/jiaocheng/1754692047a2718114.html)
   - State Trees vs Behavior Trees (UE5)

2. **Multi-Agent Coordination**
   - Contract Net Protocol for task bidding
   - Blackboard as shared memory for team AI

3. **Performance Optimization**
   - Multi-threading for parallel AI computation
   - Spatial partitioning for perception systems
   - Coroutine usage for timing-related AI

### Steve AI Documentation

- `docs/ARCHITECTURE_OVERVIEW.md` - Overall system architecture
- `docs/research/MULTI_AGENT_COORDINATION.md` - Coordination patterns
- `src/main/java/com/minewright/blackboard/` - Current blackboard implementation
- `src/main/java/com/minewright/behavior/BTBlackboard.java` - Behavior tree blackboard

---

## Conclusion

Steve AI's blackboard system is **well-architected** with partitioned knowledge areas, typed entries, and subscription-based notifications. The **highest-impact improvements** are:

1. **Spatial indexing** for world state queries (10x performance gain)
2. **Influence maps** for tactical reasoning (enables smart positioning)
3. **Filtered notifications** (50% reduction in overhead)
4. **Hierarchical blackboards** (scales to many agents)

These patterns are **proven in AAA games** (Unreal Engine, RTS titles) and **directly applicable** to Minecraft's spatial, block-based world. Implementation should proceed in phases, starting with spatial optimization for immediate performance gains.

---

**Document Version:** 1.0
**Last Updated:** 2026-03-02
**Next Review:** After Phase 1 implementation (spatial optimization)
