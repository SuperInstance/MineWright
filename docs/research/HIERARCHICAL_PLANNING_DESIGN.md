# Hierarchical Task Network (HTN) Planning Design for Steve AI

**Document Version:** 1.0
**Last Updated:** 2025-02-28
**Status:** Design Specification
**Complexity:** Advanced (8/10)
**Project:** Steve AI - MineWright Architecture

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [HTN Fundamentals](#2-htn-fundamentals)
3. [GOAP Comparison](#3-goap-comparison)
4. [Hierarchical Architecture for Steve AI](#4-hierarchical-architecture-for-steve-ai)
5. [LLM + HTN Integration](#5-llm--htn-integration)
6. [Implementation Design](#6-implementation-design)
7. [Java Code Examples](#7-java-code-examples)
8. [Migration Path](#8-migration-path)
9. [Performance Considerations](#9-performance-considerations)
10. [Testing Strategy](#10-testing-strategy)

---

## 1. Executive Summary

### 1.1 Problem Statement

Steve AI currently uses a **flat LLM-based planning** approach where every user command triggers an API call to decompose tasks into primitive actions. This architecture has several critical limitations:

| Issue | Impact | Example |
|-------|--------|---------|
| **API Costs** | $0.01-0.10 per command | "Build a house" costs 5-10 cents |
| **Latency** | 30-60 seconds per plan | Player waits 45s for response |
| **Non-deterministic** | Different plans for same input | Inconsistent building patterns |
| **No Reusability** | Every plan regenerated | "Build house" planned from scratch each time |
| **Offline Required** | Internet dependency | Cannot play offline |

### 1.2 Proposed Solution: HTN Hierarchical Planning

**Hierarchical Task Networks (HTN)** provide a layered "brain" architecture that:

```
LLM Layer (Strategic) - Issues goals and constraints
         ↓
HTN Planner (Tactical) - Decomposes into task sequences
         ↓
Behavior Scripts (Operational) - Executes primitive actions
         ↓
Minecraft API (Physical) - Interacts with game world
```

**Key Benefits:**

| Benefit | Metric | Impact |
|---------|--------|--------|
| **API Reduction** | 60-80% fewer calls | $0.002-0.04 per command |
| **Latency** | <100ms typical | 500x faster than LLM |
| **Deterministic** | Same input = same plan | Predictable, debuggable |
| **Reusable** | Cached task hierarchies | "Build house" reused 1000x |
| **Offline Capable** | Local planning only | Works without internet |
| **Designer Control** | Predefined methods | Consistent behavior |

### 1.3 Why HTN Over GOAP?

Based on extensive research ([source](https://www.cnblogs.com/jeason1997/articles/9499051.html)):

| Criteria | HTN | GOAP | Winner for Steve AI |
|----------|-----|------|---------------------|
| **Control** | Designer-specified decomposition | Emergent behavior | **HTN** - Need predictable builds |
| **Planning** | Forward (top-down) | Backward (goal-driven) | **HTN** - Natural task breakdown |
| **Industry Usage** | Growing (Horizon Zero Dawn, MOBAs) | Declining | **HTN** - Future-proof |
| **Performance** | Fast (10-50ms) | Slower (50-200ms) | **HTN** - Better for real-time |
| **Multi-Agent** | Natural coordination | Difficult | **HTN** - Worker system |
| **Learning** | Harder initially | Easier to prototype | **GOAP** - But HTN scales better |

**Decision:** HTN is the superior choice for Steve AI's structured building tasks while maintaining LLM fallback for novel situations.

---

## 2. HTN Fundamentals

### 2.1 Core Concepts

HTN planning breaks down high-level goals into executable actions through **hierarchical decomposition**:

```
build_house (Compound Task)
├── Method: build_house_basic
│   ├── Preconditions: has_materials, has_clear_space
│   └── Subtasks:
│       ├── gather_materials (Compound)
│       │   ├── Method: gather_from_nearby
│       │   │   ├── Preconditions: forest_nearby
│       │   │   └── Subtasks:
│       │   │       ├── pathfind {target: "forest"}
│       │   │       ├── mine {block: "oak_log", quantity: 32}
│       │   │       └── return_to_site
│       │   └── Method: gather_from_storage
│       │       └── Subtasks: take_from_chest
│       ├── clear_area (Primitive)
│       ├── lay_foundation (Primitive)
│       ├── build_walls (Primitive)
│       └── add_roof (Primitive)
```

### 2.2 Task Types

| Task Type | Description | Example |
|-----------|-------------|---------|
| **Compound Task** | High-level goal requiring decomposition | `build_house` |
| **Primitive Task** | Directly executable action | `mine {block: "oak_log"}` |
| **Method** | Alternative decomposition with preconditions | `build_house_basic` vs `build_house_advanced` |

### 2.3 HTN vs Traditional Planning

**Traditional (Flat) Planning:**
```
Goal: Build a house
→ Search ALL possible action sequences
→ A* through massive state space
→ Slow, unpredictable
```

**HTN (Hierarchical) Planning:**
```
Goal: Build a house
→ Match to compound task "build_house"
→ Select applicable method based on preconditions
→ Recursively decompose subtasks
→ Fast, predictable, designer-controlled
```

### 2.4 HTN Planning Algorithm

```java
function HTN-Decompose(task, worldState):
    if task is PRIMITIVE:
        return [task]  // Base case: executable action

    if task is COMPOUND:
        for method in getMethods(task):
            if method.checkPreconditions(worldState):
                subtasks = method.getSubtasks()
                plan = []
                for subtask in subtasks:
                    subplan = HTN-Decompose(subtask, worldState)
                    if subplan == FAILURE:
                        break  // Try next method
                    plan.extend(subplan)
                if plan complete:
                    return plan
        return FAILURE  // No applicable method
```

---

## 3. GOAP Comparison

### 3.1 Architectural Differences

Based on research from [GAMES104 course notes](https://it.en369.cn/jiaocheng/1754692047a2718114.html):

**GOAP (Goal-Oriented Action Planning):**
- **Backward chaining** from goal to current state
- **Emergent behavior** through A* search
- **Explicit goals** defined as state predicates
- **Unpredictable** but adaptive

**HTN (Hierarchical Task Networks):**
- **Forward decomposition** from goal to primitives
- **Designer-specified** task hierarchies
- **Implicit goals** through task structure
- **Predictable** but brittle in dynamic environments

### 3.2 Comparative Example

**Scenario:** "Build a shelter"

**GOAP Approach (Backward):**
```
1. Goal: {has_shelter: true}
2. Find action with effect: {has_shelter: true}
3. Found: build_shelter
4. Preconditions: {has_wood: 20, has_tools: true, has_space: true}
5. Subgoal: {has_wood: 20}
6. Find action with effect: {has_wood: 20}
7. Found: gather_wood
8. Preconditions: {has_tools: true, near_trees: true}
9. Subgoal: {has_tools: true}
10. Found: craft_pickaxe
... (continues backward)
```

**HTN Approach (Forward):**
```
1. Match: "build_shelter" → Compound Task
2. Select Method: build_shelter_basic
3. Check Preconditions: {has_wood: 20, has_tools: true}
4. Decompose:
   - gather_wood (compound)
     - Method: gather_from_nearby
     - Decompose: [pathfind, mine, return]
   - craft_pickaxe (compound)
     - Method: craft_basic_pickaxe
     - Decompose: [pathfind_workbench, craft]
   - clear_area (primitive)
   - build_walls (primitive)
   - add_roof (primitive)
5. Execute sequentially
```

### 3.3 Decision Matrix

| Factor | GOAP | HTN | Steve AI Choice |
|--------|------|-----|-----------------|
| **Control** | Emergent (unpredictable) | Designer-controlled | **HTN** - Need predictable builds |
| **Performance** | 50-200ms A* search | 10-50ms decomposition | **HTN** - Real-time critical |
| **Determinism** | Same state = different plans possible | Same state = same plan | **HTN** - Reproducible behavior |
| **Scalability** | Degrades with more actions | Scales with hierarchy depth | **HTN** - Many building patterns |
| **Multi-Agent** | Difficult coordination | Natural task assignment | **HTN** - Worker system |
| **Learning** | Harder initially | Harder initially | **Tie** - Both require learning |
| **Maintenance** | Easier (add actions) | Harder (maintain hierarchy) | **HTN** - Worth the effort |

---

## 4. Hierarchical Architecture for Steve AI

### 4.1 Four-Layer Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  LAYER 1: LLM STRATEGIC PLANNER                             │
│  - Processes natural language commands                      │
│  - Issues high-level goals and constraints                  │
│  - Generates NEW methods when needed (script generation)    │
│  - Fallback for novel/complex situations                    │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  LAYER 2: HTN TACTICAL PLANNER                              │
│  - Decomposes compound tasks into primitive sequences       │
│  - Selects methods based on world state preconditions       │
│  - Manages task hierarchies and caching                     │
│  - Replanning on failure or state change                    │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  LAYER 3: BEHAVIOR SCRIPTS (BaseAction)                     │
│  - Execute primitive tasks (mine, place, pathfind)          │
│  - Tick-based execution (non-blocking)                      │
│  - Progress reporting and error handling                    │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  LAYER 4: MINECRAFT API                                     │
│  - Block manipulation                                       │
│  - Entity interaction                                       │
│  - Inventory management                                     │
│  - Pathfinding                                              │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 Layer Responsibilities

#### Layer 1: LLM Strategic Planner

**Purpose:** Convert natural language to structured goals

**Inputs:**
- User command: "Build a cozy wooden cabin near the forest"

**Outputs:**
```json
{
  "goal": "build_house",
  "constraints": {
    "material": "oak_planks",
    "style": "cozy",
    "location": "near_forest",
    "size": "small"
  },
  "priority": "normal"
}
```

**When to Use:**
- Novel commands (first time seen)
- Complex creative requests
- Ambiguous or underspecified tasks
- When HTN decomposition fails

**When NOT to Use:**
- Common repetitive tasks (use cached HTN)
- Well-defined patterns (use predefined methods)
- Offline mode (no internet)

#### Layer 2: HTN Tactical Planner

**Purpose:** Decompose goals into executable task sequences

**Inputs:**
- Goal from LLM: `{goal: "build_house", constraints: {...}}`

**Outputs:**
```java
List<Task> = [
  Task {action: "pathfind", params: {target: "forest"}},
  Task {action: "mine", params: {block: "oak_log", quantity: 32}},
  Task {action: "craft", params: {item: "oak_planks", quantity: 128}},
  Task {action: "pathfind", params: {target: "build_site"}},
  Task {action: "clear_area", params: {width: 7, height: 3, depth: 7}},
  Task {action: "build_walls", params: {material: "oak_planks", ...}},
  Task {action: "add_roof", params: {material: "oak_planks", ...}}
]
```

**Method Selection:**

```java
// HTN Domain for "build_house"
Method 1: build_house_basic
  Preconditions: {has_materials, has_clear_space}
  Priority: 100
  Subtasks: [gather_materials, clear_area, build_walls, add_roof]

Method 2: build_house_with_gathering
  Preconditions: {near_forest, has_tools}
  Priority: 90
  Subtasks: [gather_from_forest, craft_materials, clear_area, build_walls, add_roof]

Method 3: build_house_luxury
  Preconditions: {has_materials, has_decorations}
  Priority: 80
  Subtasks: [build_house_basic, add_windows, add_door, add_furniture]
```

#### Layer 3: Behavior Scripts

**Purpose:** Execute primitive actions tick-by-tick

**Existing BaseAction Subclasses:**
- `MineBlockAction` - Mine blocks at target location
- `PlaceBlockAction` - Place blocks in pattern
- `PathfindAction` - Navigate to position
- `CraftItemAction` - Craft items at workbench
- `GatherResourceAction` - Collect resources from area

**Execution Model:**
```java
// Tick-based (non-blocking)
public void tick() {
    if (currentAction != null && !currentAction.isComplete()) {
        currentAction.tick();  // Progress action
    } else if (!taskQueue.isEmpty()) {
        currentAction = createAction(taskQueue.poll());
        currentAction.start();
    }
}
```

#### Layer 4: Minecraft API

**Purpose:** Interact with game world

**Key APIs:**
- `level().setBlock(pos, state)` - Place blocks
- `level().destroyBlock(pos)` - Mine blocks
- `getNavigation().moveTo(x, y, z)` - Pathfinding
- `getInventory().getItem(item)` - Inventory access

### 4.3 Data Flow Example

**User Command:** "Build a small stone tower"

```
┌─────────────────────────────────────────────────────────────┐
│ 1. LLM LAYER (Strategic)                                    │
│    Input: "Build a small stone tower"                       │
│    Output: {goal: "build_tower", material: "stone",         │
│            size: "small", height: "medium"}                 │
│    Time: 30-60s (or cached: <1s)                           │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. HTN LAYER (Tactical)                                     │
│    Input: {goal: "build_tower", material: "stone", ...}     │
│    Match: "build_tower" compound task                       │
│    Select Method: build_tower_stone_basic                   │
│    Decompose:                                               │
│      - pathfind {target: "stone quarry"}                    │
│      - mine {block: "cobblestone", quantity: 90}           │
│      - pathfind {target: "build_site"}                     │
│      - clear_area {width: 5, depth: 5}                     │
│      - build_walls {height: 10, material: "cobblestone"}    │
│    Time: 10-50ms                                           │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. BEHAVIOR LAYER (Operational)                             │
│    Execute tasks sequentially:                              │
│    - PathfindAction navigates to quarry                     │
│    - MineBlockAction gathers cobblestone                    │
│    - PathfindAction returns to site                        │
│    - PlaceBlockAction builds tower layer by layer           │
│    Time: 2-5 minutes (in-game execution)                   │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. MINECRAFT API (Physical)                                 │
│    - level().setBlock() places each cobblestone            │
│    - Navigation moves entity                                │
│    - Entity animation plays                                 │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. LLM + HTN Integration

### 5.1 Hybrid Planning Strategy

Based on research from [HiPlan](https://arxiv.org/abs/2508.19076) and [Deep Agent planning](https://www.cnblogs.com/yangykaifa/p/19560083), the optimal approach combines LLM flexibility with HTN efficiency:

```
┌────────────────────────────────────────────────────────────┐
│                   COMMAND CLASSIFIER                       │
│                   (Keyword + Embedding)                    │
└────────────┬───────────────────────────┬──────────────────┘
             │                           │
      ┌──────▼──────┐            ┌───────▼────────┐
      │   TRIVIAL   │            │  SIMPLE/MODERATE│
      │   (Cached)  │            │  (HTN Domain)   │
      └──────┬──────┘            └───────┬────────┘
             │                           │
      ┌──────▼───────────────────────────▼──────────┐
      │         Return cached plan                  │
      │         (0-1ms, no API cost)               │
      └─────────────────────────────────────────────┘

             │                           │
      ┌──────▼──────────┐      ┌────────▼────────┐
      │   COMPLEX       │      │   NOVEL         │
      │   (Structured)  │      │   (Creative)     │
      └──────┬──────────┘      └────────┬────────┘
             │                           │
      ┌──────▼──────────┐      ┌────────▼────────┐
      │  HTN + LLM      │      │  LLM-only       │
      │  (Hybrid)       │      │  (Full plan)     │
      └─────────────────┘      └─────────────────┘
```

### 5.2 Command Classification

```java
public class CommandClassifier {
    public enum PlanningMode {
        CACHED,      // 60-80% of commands (common patterns)
        HTN_ONLY,    // 15-25% of commands (structured tasks)
        HYBRID,      // 5-10% of commands (complex but structured)
        LLM_ONLY     // 5-10% of commands (novel/creative)
    }

    public PlanningMode classifyCommand(String command) {
        String lower = command.toLowerCase().trim();

        // Check cache first
        if (htnCache.hasCachedPlan(lower)) {
            return PlanningMode.CACHED;
        }

        // Check for HTN patterns
        if (matchesHTNPattern(lower)) {
            if (isComplex(command)) {
                return PlanningMode.HYBRID;
            }
            return PlanningMode.HTN_ONLY;
        }

        // Default to LLM for novel/creative tasks
        return PlanningMode.LLM_ONLY;
    }

    private boolean matchesHTNPattern(String command) {
        // Regex patterns for HTN-supported tasks
        return command.matches("(build|construct|create)\\s+(house|tower|castle|farm).*") ||
               command.matches("(mine|gather)\\s+(iron|gold|diamond|coal).*") ||
               command.matches("(craft|make)\\s+(pickaxe|sword|axe|tool).*");
    }

    private boolean isComplex(String command) {
        // Complex = multiple constraints, creative elements
        return command.split(",").length > 2 ||
               command.contains("with") ||
               command.contains("decorated") ||
               command.contains("unique");
    }
}
```

### 5.3 Hybrid Planning Flow

**Scenario:** "Build a medieval castle with towers and a moat"

```java
// Step 1: LLM Strategic Planning (30-60s)
LLMResponse strategic = llmPlanner.plan("""
    Goal: Build a medieval castle with towers and a moat

    Decompose into high-level subgoals:
    1. Build main keep (central structure)
    2. Build corner towers (4 towers)
    3. Build curtain walls (connecting towers)
    4. Dig moat (around perimeter)
    5. Build bridge (access point)

    For each subgoal, specify:
    - Required materials
    - Dimensions
    - Dependencies (what must be done first)
""");

// LLM Output:
{
  "subgoals": [
    {id: 1, name: "build_keep", type: "build_structure",
     material: "cobblestone", dimensions: {w: 15, h: 10, d: 15},
     dependencies: []},
    {id: 2, name: "build_tower", type: "build_structure",
     material: "cobblestone", dimensions: {w: 5, h: 15, d: 5},
     dependencies: [1]},
    {id: 3, name: "build_moat", type: "dig_shape",
     shape: "rectangle", dimensions: {w: 25, depth: 3, d: 25},
     dependencies: [1, 2]},
    // ...
  ]
}

// Step 2: HTN Tactical Decomposition (10-50ms per subgoal)
for (Subgoal subgoal : strategic.subgoals) {
    // Match to HTN compound task
    CompoundTask htnTask = htnDomain.matchTask(subgoal.type);

    // Select method based on constraints
    Method method = htnTask.selectMethod(subgoal.constraints);

    // Decompose into primitives
    List<Task> primitives = method.decompose(subgoal.parameters);

    taskQueue.addAll(primitives);
}

// Step 3: Execute primitives (2-5 minutes in-game)
while (!taskQueue.isEmpty()) {
    Task task = taskQueue.poll();
    BaseAction action = actionFactory.create(task);
    action.start();

    while (!action.isComplete()) {
        action.tick();  // Non-blocking tick
    }
}
```

### 5.4 LLM-Generated Methods

**Key Innovation:** LLM can generate NEW HTN methods that get cached for future use.

```java
// First time: "Build a dwarven underground hall"
// → No HTN method exists
// → LLM generates full plan (30-60s)
// → System extracts pattern and creates NEW HTN method

// LLM-generated method:
Method build_dwarven_hall_underground = Method.builder()
    .name("build_dwarven_hall_underground")
    .task("build_structure")
    .preconditions(ws -> ws.has("material", "stone") && ws.has("location", "underground"))
    .subtasks(
        new HTNTask("dig_cavern", Type.PRIMITIVE, Map.of("width", 20, "height", 5, "depth", 30)),
        new HTNTask("place_pillars", Type.PRIMITIVE, Map.of("material", "cobblestone", "spacing", 5)),
        new HTNTask("add_fortifications", Type.PRIMITIVE, Map.of("style", "dwarven")),
        new HTNTask("place_torches", Type.PRIMITIVE, Map.of("spacing", 7))
    )
    .build();

// Cache for reuse
htnDomain.addMethod("build_structure", build_dwarven_hall_underground);

// Second time: "Build a dwarven underground hall"
// → HTN method found in cache
// → Decomposed in 10-50ms (NO LLM call!)
```

### 5.5 Method Learning from Execution

```java
public class HTNLearner {
    /**
     * Extract reusable HTN method from successful LLM plan
     */
    public Method learnFromPlan(String command, List<Task> executedTasks) {
        // Extract pattern
        String patternName = extractPatternName(command);
        Map<String, Object> constraints = extractConstraints(command, executedTasks);

        // Create method
        Method learnedMethod = Method.builder()
            .name("learned_" + patternName.hashCode())
            .task(patternName)
            .preconditions(generatePreconditions(executedTasks))
            .subtasks(convertTasksToHTN(executedTasks))
            .priority(50)  // Lower than hand-crafted methods
            .source("learned")
            .build();

        // Validate and cache
        if (validateMethod(learnedMethod)) {
            htnDomain.addMethod(patternName, learnedMethod);
            LOGGER.info("Learned new HTN method: {} from command: {}",
                learnedMethod.getName(), command);
        }

        return learnedMethod;
    }

    /**
     * Generalize parameters to make method reusable
     */
    private Map<String, Object> extractConstraints(String command, List<Task> tasks) {
        Map<String, Object> constraints = new HashMap<>();

        // Find common patterns
        for (Task task : tasks) {
            if (task.hasParameter("material")) {
                // Keep material as parameter (not hard-coded)
                constraints.put("material", "ANY");
            }
            if (task.hasParameter("quantity")) {
                // Detect if quantity scales with size
                constraints.put("quantity", "SCALABLE");
            }
        }

        return constraints;
    }
}
```

---

## 6. Implementation Design

### 6.1 Core Classes

```java
// Package structure:
// com.steve.ai.htn
// ├── HTNTask.java              - Base task (compound/primitive)
// ├── HTNMethod.java            - Decomposition method
// ├── HTNDomain.java            - Task library and methods
// ├── HTNPlanner.java           - Decomposition engine
// ├── HTNCache.java             - Plan caching
// ├── HTNLearner.java           - LLM method extraction
// └── LLMHTNBridge.java         - Integration layer
```

### 6.2 HTN Task Definition

```java
package com.steve.ai.htn;

import com.steve.action.Task;
import java.util.*;

/**
 * Hierarchical task in HTN planning system.
 * Can be compound (requires decomposition) or primitive (directly executable).
 */
public class HTNTask {
    public enum Type {
        COMPOUND,  // Requires decomposition
        PRIMITIVE  // Directly executable
    }

    private final String name;
    private final Type type;
    private final Map<String, Object> parameters;

    public HTNTask(String name, Type type, Map<String, Object> parameters) {
        this.name = name;
        this.type = type;
        this.parameters = new HashMap<>(parameters);
    }

    public String getName() { return name; }
    public Type getType() { return type; }
    public Map<String, Object> getParameters() { return new HashMap<>(parameters); }

    /**
     * Convert to Steve Task (for primitive tasks)
     */
    public Task toSteveTask() {
        if (type != Type.PRIMITIVE) {
            throw new IllegalStateException("Cannot convert compound task to Task");
        }
        return new Task(name, parameters);
    }

    @Override
    public String toString() {
        return String.format("HTNTask{name=%s, type=%s, params=%s}",
            name, type, parameters);
    }
}
```

### 6.3 HTN Method Definition

```java
package com.steve.ai.htn;

import java.util.*;
import java.util.function.Predicate;

/**
 * HTN Method: Alternative way to decompose a compound task.
 * Contains preconditions, subtasks, priority, and metadata.
 */
public class HTNMethod {
    private final String name;
    private final String taskName;  // Parent compound task
    private final Predicate<Map<String, Object>> preconditions;
    private final List<HTNTask> subtasks;
    private final int priority;
    private final String source;  // "handcrafted", "learned", "llm"

    private HTNMethod(Builder builder) {
        this.name = builder.name;
        this.taskName = builder.taskName;
        this.preconditions = builder.preconditions;
        this.subtasks = List.copyOf(builder.subtasks);
        this.priority = builder.priority;
        this.source = builder.source;
    }

    public String getName() { return name; }
    public String getTaskName() { return taskName; }
    public int getPriority() { return priority; }
    public String getSource() { return source; }
    public List<HTNTask> getSubtasks() { return subtasks; }

    /**
     * Check if this method is applicable given current world state
     */
    public boolean checkPreconditions(Map<String, Object> worldState) {
        return preconditions.test(worldState);
    }

    /**
     * Builder for HTN methods
     */
    public static Builder builder(String name, String taskName) {
        return new Builder(name, taskName);
    }

    public static class Builder {
        private final String name;
        private final String taskName;
        private Predicate<Map<String, Object>> preconditions = ws -> true;
        private final List<HTNTask> subtasks = new ArrayList<>();
        private int priority = 100;
        private String source = "handcrafted";

        private Builder(String name, String taskName) {
            this.name = name;
            this.taskName = taskName;
        }

        public Builder precondition(Predicate<Map<String, Object>> condition) {
            this.preconditions = condition;
            return this;
        }

        public Builder addSubtask(HTNTask task) {
            this.subtasks.add(task);
            return this;
        }

        public Builder addSubtasks(HTNTask... tasks) {
            this.subtasks.addAll(Arrays.asList(tasks));
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public HTNMethod build() {
            return new HTNMethod(this);
        }
    }
}
```

### 6.4 HTN Domain

```java
package com.steve.ai.htn;

import com.steve.action.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * HTN Domain: Contains all compound tasks, primitive tasks, and methods.
 * Acts as the "knowledge base" for HTN planning.
 */
public class HTNDomain {
    private static final Logger LOGGER = LoggerFactory.getLogger(HTNDomain.class);

    private final Map<String, List<HTNMethod>> methods;
    private final Set<String> compoundTasks;
    private final Set<String> primitiveTasks;

    public HTNDomain() {
        this.methods = new HashMap<>();
        this.compoundTasks = new HashSet<>();
        this.primitiveTasks = new HashSet<>();
    }

    /**
     * Load default HTN domain for Minecraft
     */
    public void loadDefaultDomain() {
        LOGGER.info("Loading default HTN domain");

        registerBuildingMethods();
        registerMiningMethods();
        registerCraftingMethods();
        registerGatheringMethods();

        LOGGER.info("Loaded {} compound tasks, {} primitive tasks, {} methods",
            compoundTasks.size(), primitiveTasks.size(),
            methods.values().stream().mapToInt(List::size).sum());
    }

    /**
     * Register building-related compound tasks and methods
     */
    private void registerBuildingMethods() {
        // Compound task: build_house
        registerCompoundTask("build_house");

        // Method 1: Basic house (requires materials)
        addMethod("build_house",
            HTNMethod.builder("build_house_basic", "build_house")
                .precondition(ws ->
                    ws.getOrDefault("has_materials", false).equals(true) &&
                    ws.getOrDefault("has_clear_space", false).equals(true))
                .addSubtask(new HTNTask("clear_area", HTNTask.Type.COMPOUND,
                    Map.of("width", 7, "depth", 7)))
                .addSubtask(new HTNTask("lay_foundation", HTNTask.Type.PRIMITIVE,
                    Map.of("material", "oak_planks")))
                .addSubtask(new HTNTask("build_walls", HTNTask.Type.PRIMITIVE,
                    Map.of("height", 3, "material", "oak_planks")))
                .addSubtask(new HTNTask("add_roof", HTNTask.Type.PRIMITIVE,
                    Map.of("style", "gable", "material", "oak_planks")))
                .priority(100)
                .source("handcrafted")
                .build()
        );

        // Method 2: House with material gathering
        addMethod("build_house",
            HTNMethod.builder("build_house_with_gathering", "build_house")
                .precondition(ws ->
                    ws.getOrDefault("near_forest", false).equals(true) &&
                    ws.getOrDefault("has_tools", false).equals(true))
                .addSubtask(new HTNTask("gather_materials", HTNTask.Type.COMPOUND,
                    Map.of("target", "oak_log", "quantity", 32)))
                .addSubtask(new HTNTask("craft_materials", HTNTask.Type.COMPOUND,
                    Map.of("target", "oak_planks", "quantity", 128)))
                .addSubtask(new HTNTask("clear_area", HTNTask.Type.COMPOUND,
                    Map.of("width", 7, "depth", 7)))
                .addSubtask(new HTNTask("lay_foundation", HTNTask.Type.PRIMITIVE,
                    Map.of("material", "oak_planks")))
                .addSubtask(new HTNTask("build_walls", HTNTask.Type.PRIMITIVE,
                    Map.of("height", 3, "material", "oak_planks")))
                .addSubtask(new HTNTask("add_roof", HTNTask.Type.PRIMITIVE,
                    Map.of("style", "gable", "material", "oak_planks")))
                .priority(90)
                .source("handcrafted")
                .build()
        );

        // Compound task: build_tower
        registerCompoundTask("build_tower");

        addMethod("build_tower",
            HTNMethod.builder("build_tower_stone", "build_tower")
                .precondition(ws -> ws.getOrDefault("material", "wood").equals("stone"))
                .addSubtask(new HTNTask("gather_stone", HTNTask.Type.COMPOUND,
                    Map.of("quantity", 90)))
                .addSubtask(new HTNTask("build_tower_structure", HTNTask.Type.PRIMITIVE,
                    Map.of("width", 5, "height", 10, "depth", 5, "material", "cobblestone")))
                .priority(100)
                .source("handcrafted")
                .build()
        );
    }

    /**
     * Register mining-related compound tasks
     */
    private void registerMiningMethods() {
        registerCompoundTask("mine_diamond");

        addMethod("mine_diamond",
            HTNMethod.builder("mine_diamond_optimal", "mine_diamond")
                .precondition(ws -> ws.getOrDefault("has_pickaxe", false).equals(true))
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE,
                    Map.of("y_level", -59, "reason", "diamond_optimal")))
                .addSubtask(new HTNTask("mine_tunnel", HTNTask.Type.PRIMITIVE,
                    Map.of("length", 32, "block", "stone")))
                .addSubtask(new HTNTask("mine_ore", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "diamond_ore", "quantity", 8)))
                .priority(100)
                .source("handcrafted")
                .build()
        );

        addMethod("mine_diamond",
            HTNMethod.builder("mine_diamond_with_crafting", "mine_diamond")
                .precondition(ws -> ws.getOrDefault("has_pickaxe", false).equals(false))
                .addSubtask(new HTNTask("craft_pickaxe", HTNTask.Type.COMPOUND,
                    Map.of("material", "iron")))
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE,
                    Map.of("y_level", -59)))
                .addSubtask(new HTNTask("mine_ore", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "diamond_ore", "quantity", 8)))
                .priority(50)
                .source("handcrafted")
                .build()
        );
    }

    /**
     * Register crafting methods
     */
    private void registerCraftingMethods() {
        registerCompoundTask("craft_pickaxe");

        addMethod("craft_pickaxe",
            HTNMethod.builder("craft_stone_pickaxe", "craft_pickaxe")
                .precondition(ws -> ws.getOrDefault("material", "wood").equals("stone"))
                .addSubtask(new HTNTask("gather_cobblestone", HTNTask.Type.PRIMITIVE,
                    Map.of("quantity", 3)))
                .addSubtask(new HTNTask("find_workbench", HTNTask.Type.PRIMITIVE, Map.of()))
                .addSubtask(new HTNTask("craft", HTNTask.Type.PRIMITIVE,
                    Map.of("item", "stone_pickaxe", "quantity", 1)))
                .priority(100)
                .source("handcrafted")
                .build()
        );
    }

    /**
     * Register gathering methods
     */
    private void registerGatheringMethods() {
        registerCompoundTask("gather_materials");

        addMethod("gather_materials",
            HTNMethod.builder("gather_oak_from_forest", "gather_materials")
                .precondition(ws -> ws.getOrDefault("target", "wood").equals("oak_log") &&
                                  ws.getOrDefault("near_forest", false).equals(true))
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE,
                    Map.of("target", "nearest_forest")))
                .addSubtask(new HTNTask("mine", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "oak_log", "quantity", 32)))
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE,
                    Map.of("target", "build_site")))
                .priority(100)
                .source("handcrafted")
                .build()
        );
    }

    /**
     * Register a compound task (requires decomposition)
     */
    public void registerCompoundTask(String taskName) {
        compoundTasks.add(taskName);
    }

    /**
     * Register a primitive task (directly executable)
     */
    public void registerPrimitiveTask(String taskName) {
        primitiveTasks.add(taskName);
    }

    /**
     * Add a method for decomposing a compound task
     */
    public void addMethod(String taskName, HTNMethod method) {
        methods.computeIfAbsent(taskName, k -> new ArrayList<>()).add(method);

        // Sort by priority (highest first)
        methods.get(taskName).sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }

    /**
     * Get all methods for a compound task
     */
    public List<HTNMethod> getMethodsForTask(String taskName) {
        return Collections.unmodifiableList(
            methods.getOrDefault(taskName, Collections.emptyList())
        );
    }

    /**
     * Check if a task has decomposition methods
     */
    public boolean hasMethodsForTask(String taskName) {
        return methods.containsKey(taskName) && !methods.get(taskName).isEmpty();
    }

    /**
     * Check if a task is compound (requires decomposition)
     */
    public boolean isCompoundTask(String taskName) {
        return compoundTasks.contains(taskName);
    }

    /**
     * Check if a task is primitive (directly executable)
     */
    public boolean isPrimitiveTask(String taskName) {
        return primitiveTasks.contains(taskName);
    }
}
```

### 6.5 HTN Planner

```java
package com.steve.ai.htn;

import com.steve.action.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * HTN Planner: Decomposes compound tasks into primitive task sequences.
 * Uses forward-chaining decomposition with method selection.
 */
public class HTNPlanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(HTNPlanner.class);

    private final HTNDomain domain;
    private final HTNCache cache;
    private final HTNLearner learner;
    private static final int MAX_DECOMPOSITION_DEPTH = 10;

    public HTNPlanner(HTNDomain domain) {
        this.domain = domain;
        this.cache = new HTNCache();
        this.learner = new HTNLearner(domain);
    }

    /**
     * Plan: Decompose compound task into primitive sequence
     *
     * @param rootTask Root compound task to decompose
     * @param worldState Current world state for precondition checking
     * @return List of executable tasks, or empty if planning fails
     */
    public List<Task> plan(HTNTask rootTask, Map<String, Object> worldState) {
        // Check cache first
        String cacheKey = generateCacheKey(rootTask, worldState);
        List<Task> cached = cache.get(cacheKey);
        if (cached != null) {
            LOGGER.debug("Cache hit for task: {}", rootTask.getName());
            return new ArrayList<>(cached);
        }

        // Decompose
        List<Task> plan = decompose(rootTask, worldState, 0);

        if (!plan.isEmpty()) {
            // Cache successful plan
            cache.put(cacheKey, plan);
            LOGGER.info("HTN plan generated: {} tasks (depth: {})",
                plan.size(), calculateDepth(rootTask));
        } else {
            LOGGER.warn("HTN planning failed for task: {}", rootTask.getName());
        }

        return plan;
    }

    /**
     * Recursive decomposition of compound tasks
     */
    private List<Task> decompose(HTNTask task, Map<String, Object> worldState, int depth) {
        // Prevent infinite recursion
        if (depth > MAX_DECOMPOSITION_DEPTH) {
            LOGGER.error("Max decomposition depth exceeded for task: {}", task.getName());
            return Collections.emptyList();
        }

        // Base case: primitive task
        if (task.getType() == HTNTask.Type.PRIMITIVE) {
            return Collections.singletonList(task.toSteveTask());
        }

        // Compound task: find applicable method
        String taskName = task.getName();
        if (!domain.hasMethodsForTask(taskName)) {
            LOGGER.warn("No methods found for compound task: {}", taskName);
            return Collections.emptyList();
        }

        // Try methods in priority order
        List<HTNMethod> methods = domain.getMethodsForTask(taskName);
        for (HTNMethod method : methods) {
            // Check preconditions
            if (!method.checkPreconditions(worldState)) {
                LOGGER.debug("Method '{}' preconditions not met", method.getName());
                continue;
            }

            LOGGER.debug("Using method '{}' to decompose '{}'",
                method.getName(), taskName);

            // Decompose subtasks recursively
            List<Task> plan = new ArrayList<>();
            boolean decompositionFailed = false;

            for (HTNTask subtask : method.getSubtasks()) {
                List<Task> subplan = decompose(subtask, worldState, depth + 1);

                if (subplan.isEmpty()) {
                    // Decomposition failed, try next method
                    LOGGER.debug("Subtask decomposition failed for '{}'", subtask.getName());
                    decompositionFailed = true;
                    break;
                }

                plan.addAll(subplan);
            }

            if (!decompositionFailed) {
                return plan;
            }
        }

        // No applicable method or all failed
        LOGGER.warn("All methods failed for compound task: {}", taskName);
        return Collections.emptyList();
    }

    /**
     * Generate cache key for plan memoization
     */
    private String generateCacheKey(HTNTask task, Map<String, Object> worldState) {
        return String.format("%s_%s", task.getName(),
            worldState.hashCode());
    }

    /**
     * Calculate maximum decomposition depth
     */
    private int calculateDepth(HTNTask task) {
        if (task.getType() == HTNTask.Type.PRIMITIVE) {
            return 1;
        }

        int maxDepth = 1;
        for (HTNMethod method : domain.getMethodsForTask(task.getName())) {
            for (HTNTask subtask : method.getSubtasks()) {
                maxDepth = Math.max(maxDepth, 1 + calculateDepth(subtask));
            }
        }
        return maxDepth;
    }

    public HTNCache getCache() { return cache; }
    public HTNLearner getLearner() { return learner; }
}
```

---

## 7. Java Code Examples

### 7.1 Complete Integration Example

```java
package com.steve.ai.htn;

import com.steve.action.Task;
import com.steve.entity.ForemanEntity;
import com.steve.llm.TaskPlanner;
import com.steve.llm.ResponseParser.ParsedResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Hybrid LLM + HTN Planning System
 *
 * Combines LLM strategic planning with HTN tactical decomposition:
 * - LLM issues high-level goals
 * - HTN decomposes into executable sequences
 * - LLM learns new HTN methods from successful plans
 */
public class HybridPlanningSystem {
    private final HTNPlanner htnPlanner;
    private final TaskPlanner llmPlanner;
    private final CommandClassifier classifier;

    public HybridPlanningSystem(TaskPlanner llmPlanner) {
        HTNDomain domain = new HTNDomain();
        domain.loadDefaultDomain();

        this.htnPlanner = new HTNPlanner(domain);
        this.llmPlanner = llmPlanner;
        this.classifier = new CommandClassifier();
    }

    /**
     * Plan command using hybrid LLM + HTN approach
     */
    public CompletableFuture<List<Task>> planAsync(
            ForemanEntity foreman,
            String command) {

        CommandClassifier.PlanningMode mode = classifier.classifyCommand(command);

        return switch (mode) {
            case CACHED -> planWithCache(command, foreman);
            case HTN_ONLY -> planWithHTN(command, foreman);
            case HYBRID -> planWithHybrid(command, foreman);
            case LLM_ONLY -> planWithLLM(command, foreman);
        };
    }

    /**
     * Plan using cached HTN decomposition (fastest)
     */
    private CompletableFuture<List<Task>> planWithCache(
            String command, ForemanEntity foreman) {
        HTNTask task = classifier.extractHTNTask(command);
        Map<String, Object> worldState = buildWorldState(foreman);

        // Should hit cache (based on classification)
        List<Task> plan = htnPlanner.plan(task, worldState);

        LOGGER.info("[CACHE] Plan for '{}': {} tasks (0ms)",
            command, plan.size());

        return CompletableFuture.completedFuture(plan);
    }

    /**
     * Plan using HTN only (no LLM call)
     */
    private CompletableFuture<List<Task>> planWithHTN(
            String command, ForemanEntity foreman) {

        HTNTask task = classifier.extractHTNTask(command);
        Map<String, Object> worldState = buildWorldState(foreman);

        long startTime = System.currentTimeMillis();
        List<Task> plan = htnPlanner.plan(task, worldState);
        long duration = System.currentTimeMillis() - startTime;

        if (!plan.isEmpty()) {
            LOGGER.info("[HTN] Plan for '{}': {} tasks ({}ms)",
                command, plan.size(), duration);
            return CompletableFuture.completedFuture(plan);
        }

        // HTN failed, fall back to LLM
        LOGGER.warn("[HTN] Planning failed, falling back to LLM");
        return planWithLLM(command, foreman);
    }

    /**
     * Plan using hybrid LLM + HTN approach
     */
    private CompletableFuture<List<Task>> planWithHybrid(
            String command, ForemanEntity foreman) {

        LOGGER.info("[HYBRID] Planning with LLM strategy + HTN tactics");

        // Step 1: LLM strategic planning (async)
        return llmPlanner.planTasksAsync(foreman, command)
            .thenCompose(llmResponse -> {
                if (llmResponse == null || llmResponse.getTasks().isEmpty()) {
                    LOGGER.warn("[HYBRID] LLM planning failed");
                    return CompletableFuture.completedFuture(Collections.emptyList());
                }

                // Step 2: Extract HTN methods from LLM plan for learning
                HTNLearner learner = htnPlanner.getLearner();
                Method learned = learner.learnFromPlan(
                    command,
                    llmResponse.getTasks()
                );

                if (learned != null) {
                    LOGGER.info("[HYBRID] Learned new HTN method: {}",
                        learned.getName());
                }

                // Step 3: Return LLM tasks (or refine with HTN)
                return CompletableFuture.completedFuture(llmResponse.getTasks());
            });
    }

    /**
     * Plan using LLM only (for novel/creative tasks)
     */
    private CompletableFuture<List<Task>> planWithLLM(
            String command, ForemanEntity foreman) {

        LOGGER.info("[LLM] Full LLM planning for: {}", command);

        return llmPlanner.planTasksAsync(foreman, command)
            .thenApply(response -> {
                if (response != null) {
                    LOGGER.info("[LLM] Plan: {} tasks", response.getTasks().size());
                    return response.getTasks();
                }
                return Collections.emptyList();
            });
    }

    /**
     * Build world state for HTN precondition checking
     */
    private Map<String, Object> buildWorldState(ForemanEntity foreman) {
        Map<String, Object> worldState = new HashMap<>();

        // Agent state
        worldState.put("position", foreman.blockPosition());
        worldState.put("has_tools", hasTools(foreman));
        worldState.put("has_materials", hasMaterials(foreman));
        worldState.put("has_pickaxe", hasPickaxe(foreman));

        // Environment state
        worldState.put("near_forest", isNearForest(foreman));
        worldState.put("near_cave", isNearCave(foreman));
        worldState.put("has_clear_space", hasClearSpace(foreman));

        return worldState;
    }

    // Helper methods for world state
    private boolean hasTools(ForemanEntity foreman) {
        return foreman.getMainHandItem().getItem() instanceof net.minecraft.world.item.ToolItem;
    }

    private boolean hasMaterials(ForemanEntity foreman) {
        return !foreman.getInventory().isEmpty();
    }

    private boolean hasPickaxe(ForemanEntity foreman) {
        return foreman.getMainHandItem().getItem() instanceof net.minecraft.world.item.PickaxeItem;
    }

    private boolean isNearForest(ForemanEntity foreman) {
        // Scan nearby for logs
        BlockPos pos = foreman.blockPosition();
        for (int x = -32; x <= 32; x++) {
            for (int z = -32; z <= 32; z++) {
                for (int y = -5; y <= 10; y++) {
                    if (foreman.level().getBlockState(pos.offset(x, y, z))
                            .is(net.minecraft.tags.BlockTags.LOGS)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isNearCave(ForemanEntity foreman) {
        // Check for dark areas below
        return false; // Placeholder
    }

    private boolean hasClearSpace(ForemanEntity foreman) {
        // Check if area nearby is clear
        return true; // Placeholder
    }
}
```

### 7.2 Command Classifier

```java
package com.steve.ai.htn;

import java.util.regex.*;
import java.util.*;

/**
 * Classifies user commands to determine optimal planning strategy
 */
public class CommandClassifier {
    public enum PlanningMode {
        CACHED,      // HTN cache hit (0-1ms)
        HTN_ONLY,    // HTN decomposition (10-50ms)
        HYBRID,      // LLM strategy + HTN tactics (30-60s)
        LLM_ONLY     // Full LLM planning (30-60s)
    }

    private final HTNCache cache;

    public CommandClassifier(HTNCache cache) {
        this.cache = cache;
    }

    /**
     * Classify command and determine optimal planning mode
     */
    public PlanningMode classifyCommand(String command) {
        String lower = command.toLowerCase().trim();

        // Check cache first (60-80% hit rate expected)
        if (cache.hasCachedPlan(lower)) {
            return PlanningMode.CACHED;
        }

        // Check for HTN patterns
        if (matchesHTNPattern(lower)) {
            if (isComplex(command)) {
                return PlanningMode.HYBRID;
            }
            return PlanningMode.HTN_ONLY;
        }

        // Default to LLM for novel/creative tasks
        return PlanningMode.LLM_ONLY;
    }

    /**
     * Extract HTN task from command
     */
    public HTNTask extractHTNTask(String command) {
        String lower = command.toLowerCase().trim();

        // Build patterns
        Matcher house = Pattern.compile("build\\s+(?:a\\s+)?(?:small|medium|large)?\\s*(?:wood|stone|cobble)?\\s*house").matcher(lower);
        if (house.find()) {
            Map<String, Object> params = new HashMap<>();
            params.put("material", extractMaterial(lower, "oak_planks"));
            return new HTNTask("build_house", HTNTask.Type.COMPOUND, params);
        }

        // Tower patterns
        Matcher tower = Pattern.compile("build\\s+(?:a\\s+)?(?:stone|cobble)?\\s*tower").matcher(lower);
        if (tower.find()) {
            Map<String, Object> params = new HashMap<>();
            params.put("material", extractMaterial(lower, "cobblestone"));
            return new HTNTask("build_tower", HTNTask.Type.COMPOUND, params);
        }

        // Mining patterns
        Matcher mine = Pattern.compile("mine\\s+(diamond|iron|gold|coal)").matcher(lower);
        if (mine.find()) {
            Map<String, Object> params = new HashMap<>();
            params.put("ore", mine.group(1));
            return new HTNTask("mine_" + mine.group(1), HTNTask.Type.COMPOUND, params);
        }

        // Default: return as-is (will likely fail HTN and fall back to LLM)
        return new HTNTask(lower.replaceAll("\\s+", "_"),
            HTNTask.Type.COMPOUND, new HashMap<>());
    }

    private boolean matchesHTNPattern(String command) {
        return command.matches("(build|construct|create)\\s+.*") ||
               command.matches("(mine|gather)\\s+.*") ||
               command.matches("(craft|make)\\s+.*");
    }

    private boolean isComplex(String command) {
        // Complex = multiple clauses, creative adjectives, or unique features
        return command.split(",|\\sand\\s").length > 2 ||
               command.contains("unique") ||
               command.contains("decorated") ||
               command.contains("with") ||
               command.contains("medieval") ||
               command.contains("ancient");
    }

    private String extractMaterial(String command, String defaultMaterial) {
        if (command.contains("wood")) return "oak_planks";
        if (command.contains("stone") || command.contains("cobble")) return "cobblestone";
        if (command.contains("iron")) return "iron_block";
        return defaultMaterial;
    }
}
```

---

## 8. Migration Path

### 8.1 Phase 1: Foundation (Week 1-2)

**Goals:** Implement HTN core infrastructure

**Tasks:**
- [ ] Create `ai.htn` package structure
- [ ] Implement `HTNTask`, `HTNMethod`, `HTNDomain` classes
- [ ] Implement `HTNPlanner` with recursive decomposition
- [ ] Implement `HTNCache` for plan memoization
- [ ] Implement `CommandClassifier` for mode selection
- [ ] Write unit tests for decomposition logic

**Deliverables:**
- Working HTN planner that can decompose simple hierarchies
- Basic domain with 3-5 compound tasks
- Test coverage >80%

### 8.2 Phase 2: Domain Building (Week 3-4)

**Goals:** Build comprehensive HTN domain for Minecraft

**Tasks:**
- [ ] Define building patterns (house, tower, farm, shelter)
- [ ] Define mining patterns (strip mine, cave mine, ore-specific)
- [ ] Define crafting patterns (tools, weapons, armor)
- [ ] Define gathering patterns (resources, materials)
- [ ] Implement precondition predicates for each method
- [ ] Test all methods with various world states

**Deliverables:**
- 20+ compound tasks with 50+ methods
- 95%+ of common commands covered by HTN
- Performance testing (all plans <100ms)

### 8.3 Phase 3: LLM Integration (Week 5-6)

**Goals:** Integrate LLM with HTN for hybrid planning

**Tasks:**
- [ ] Implement `HybridPlanningSystem`
- [ ] Implement `HTNLearner` for method extraction
- [ ] Integrate with existing `TaskPlanner`
- [ ] Add fallback logic (HTN → LLM)
- [ ] Implement method validation and caching
- [ ] Test hybrid flows

**Deliverables:**
- Working hybrid planner with LLM fallback
- Method learning from successful LLM plans
- 60-80% reduction in LLM API calls

### 8.4 Phase 4: Production Rollout (Week 7-8)

**Goals:** Hardening, optimization, and rollout

**Tasks:**
- [ ] Performance profiling and optimization
- [ ] Add comprehensive logging and monitoring
- [ ] Create debugging tools (plan visualization)
- [ ] User acceptance testing
- [ ] Gradual rollout (10% → 50% → 100%)
- [ ] Documentation and tutorials

**Deliverables:**
- Production-ready HTN system
- <50ms planning latency for 95% of commands
- Comprehensive documentation

### 8.5 Rollback Strategy

**Feature Flags:**
```toml
[htn]
enabled = true
cache_enabled = true
learning_enabled = true
fallback_to_llm = true

[htn.rollout]
percentage = 10  # 10, 50, 100
```

**Rollback Triggers:**
- Plan success rate <90%
- Average latency >200ms
- User complaints >5% of commands
- Critical bugs in decomposition

---

## 9. Performance Considerations

### 9.1 Performance Targets

| Operation | Target | Current (LLM-only) | Improvement |
|-----------|--------|-------------------|-------------|
| **Simple Commands** | <10ms | 30-60s | **3000-6000x faster** |
| **Complex Commands** | <100ms | 30-60s | **300-600x faster** |
| **Novel Commands** | 30-60s | 30-60s | Same (LLM fallback) |
| **Cache Hit Rate** | 60-80% | 0% | **New capability** |
| **API Cost/Command** | $0.001 | $0.05 | **50x cheaper** |

### 9.2 Caching Strategy

```java
public class HTNCache {
    private final Cache<String, List<Task>> planCache;
    private final Cache<String, Boolean> applicabilityCache;

    public HTNCache() {
        // LRU cache for completed plans
        this.planCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

        // Cache for precondition checking
        this.applicabilityCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    }

    public void put(String key, List<Task> plan) {
        planCache.put(key, Collections.unmodifiableList(plan));
    }

    public List<Task> get(String key) {
        return planCache.getIfPresent(key);
    }

    public boolean hasCachedPlan(String command) {
        return planCache.getIfPresent(command) != null;
    }
}
```

### 9.3 Optimization Techniques

**1. Lazy Decomposition:**
```java
// Only decompose when needed (defer until execution)
public class LazyHTNTask {
    private HTNTask task;
    private List<Task> decomposed = null;

    public List<Task> getDecomposed(Map<String, Object> worldState) {
        if (decomposed == null) {
            decomposed = htnPlanner.decompose(task, worldState);
        }
        return decomposed;
    }
}
```

**2. Parallel Method Evaluation:**
```java
// Evaluate method preconditions in parallel
List<HTNMethod> applicableMethods = methods.parallelStream()
    .filter(m -> m.checkPreconditions(worldState))
    .toList();
```

**3. Plan Compression:**
```java
// Merge consecutive identical actions
public List<Task> compressPlan(List<Task> plan) {
    List<Task> compressed = new ArrayList<>();
    Task last = null;

    for (Task task : plan) {
        if (last != null && last.getAction().equals(task.getAction())) {
            // Merge quantities
            int mergedQty = last.getIntParameter("quantity", 1) +
                           task.getIntParameter("quantity", 1);
            last = new Task(last.getAction(),
                Map.of("quantity", mergedQty));
        } else {
            if (last != null) compressed.add(last);
            last = task;
        }
    }
    if (last != null) compressed.add(last);

    return compressed;
}
```

### 9.4 Memory Profiling

| Component | Memory | Notes |
|-----------|--------|-------|
| HTN Domain | 100-500 KB | Loaded once at startup |
| HTN Cache | 1-10 MB | Grows with usage |
| Plan (typical) | 1-5 KB | Temporary (during decomposition) |
| Total (per agent) | <15 MB | Acceptable for modern systems |

---

## 10. Testing Strategy

### 10.1 Unit Testing

```java
@Test
public void testHTNDecomposition() {
    HTNDomain domain = new HTNDomain();
    domain.loadDefaultDomain();
    HTNPlanner planner = new HTNPlanner(domain);

    // Test: build_house decomposition
    HTNTask task = new HTNTask("build_house", HTNTask.Type.COMPOUND,
        Map.of("material", "oak_planks"));
    Map<String, Object> worldState = Map.of(
        "has_materials", true,
        "has_clear_space", true
    );

    List<Task> plan = planner.plan(task, worldState);

    assertFalse(plan.isEmpty());
    assertTrue(plan.stream().anyMatch(t -> t.getAction().equals("clear_area")));
    assertTrue(plan.stream().anyMatch(t -> t.getAction().equals("build_walls")));
}

@Test
public void testMethodSelection() {
    // Test that highest-priority applicable method is selected
    HTNDomain domain = new HTNDomain();
    domain.loadDefaultDomain();

    List<HTNMethod> methods = domain.getMethodsForTask("build_house");
    assertEquals(100, methods.get(0).getPriority());  // Highest priority first
}

@Test
public void testPreconditionChecking() {
    HTNMethod method = HTNMethod.builder("test", "build_house")
        .precondition(ws -> ws.getOrDefault("has_materials", false).equals(true))
        .build();

    assertTrue(method.checkPreconditions(Map.of("has_materials", true)));
    assertFalse(method.checkPreconditions(Map.of("has_materials", false)));
}
```

### 10.2 Integration Testing

```java
@Test
public void testHybridPlanning() {
    HybridPlanningSystem system = new HybridPlanningSystem(mockLLMPlanner);
    ForemanEntity foreman = createTestForeman();

    // Test HTN-only command
    CompletableFuture<List<Task>> future = system.planAsync(
        foreman, "Build a house");

    List<Task> plan = future.join();
    assertFalse(plan.isEmpty());

    // Verify HTN was used (not LLM)
    verify(mockLLMPlanner, never()).planTasksAsync(any(), any());
}

@Test
public void testLLMFallback() {
    // Setup HTN to fail
    HybridPlanningSystem system = new HybridPlanningSystem(mockLLMPlanner);

    // Test novel command that HTN cannot handle
    CompletableFuture<List<Task>> future = system.planAsync(
        foreman, "Build a magical floating castle made of enchanted blocks");

    List<Task> plan = future.join();

    // Verify LLM was called as fallback
    verify(mockLLMPlanner).planTasksAsync(eq(foreman),
        eq("Build a magical floating castle made of enchanted blocks"));
}
```

### 10.3 Performance Testing

```java
@Test
public void testHTNPlanningPerformance() {
    HTNPlanner planner = createPlanner();

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < 1000; i++) {
        planner.plan(createTestTask(), createTestWorldState());
    }

    long duration = System.currentTimeMillis() - startTime;
    double avgMs = duration / 1000.0;

    assertTrue("Average planning time: " + avgMs + "ms (target: <50ms)",
        avgMs < 50);
}

@Test
public void testCachePerformance() {
    HTNPlanner planner = createPlanner();
    HTNTask task = createTestTask();
    Map<String, Object> worldState = createTestWorldState();

    // First call (cache miss)
    long start1 = System.nanoTime();
    planner.plan(task, worldState);
    long duration1 = System.nanoTime() - start1;

    // Second call (cache hit)
    long start2 = System.nanoTime();
    planner.plan(task, worldState);
    long duration2 = System.nanoTime() - start2;

    // Cache should be >100x faster
    assertTrue("Cache speedup: " + (duration1 / duration2) + "x",
        duration1 / duration2 > 100);
}
```

---

## 11. Conclusion

### 11.1 Summary

This design document presents a comprehensive **Hierarchical Task Network (HTN)** planning system for Steve AI that:

1. **Reduces API costs by 60-80%** through cached hierarchical decomposition
2. **Improves latency from 30-60s to <100ms** for common commands
3. **Provides deterministic, predictable behavior** through designer-specified methods
4. **Enables offline operation** with local HTN planning
5. **Learns new methods** from successful LLM plans
6. **Maintains LLM flexibility** for novel/creative tasks

### 11.2 Key Innovations

| Innovation | Description | Impact |
|------------|-------------|--------|
| **Hybrid Architecture** | LLM strategy + HTN tactics | Best of both worlds |
| **Method Learning** | Extract HTN methods from LLM plans | Self-improving system |
| **Smart Classification** | Route to optimal planner | 60-80% cache hit rate |
| **Plan Caching** | Memoize successful decompositions | Near-instant replay |
| **Graceful Fallback** | HTN → LLM on failure | Always produces a plan |

### 11.3 Next Steps

1. **Implement Phase 1** (Week 1-2): HTN core infrastructure
2. **Build Domain** (Week 3-4): Minecraft task hierarchies
3. **Integrate LLM** (Week 5-6): Hybrid planning system
4. **Production Rollout** (Week 7-8): Testing and deployment

### 11.4 Expected Outcomes

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| **Average Latency** | 30-60s | <100ms (cached) | **300-600x faster** |
| **API Cost/Command** | $0.05 | $0.01 (HTN) | **80% reduction** |
| **Determinism** | Low | High | Predictable behavior |
| **Offline Support** | No | Yes | Works without internet |
| **Developer Control** | Low | High | Designer-specified methods |

---

## Sources

### HTN Fundamentals
- [游戏AI行为决策——HTN（分层任务网络）](https://m.blog.csdn.net/UWA4D/article/details/150492521) - HTN fundamentals and game AI applications
- [Games104——游戏引擎Gameplay玩法系统：高级AI](https://it.en369.cn/jiaocheng/1754692047a2718114.html) - Advanced AI course covering HTN
- [常见的游戏AI技术对比(FSM,HFSM,BT,ST,GOAP,HTN,Utility)](https://www.cnblogs.com/jeason1997/articles/9499051.html) - Comprehensive comparison of game AI techniques

### HTN vs GOAP
- [游戏AI技术对比：FSM, HFSM, BT, GOAP, HTN](https://www.cnblogs.com/moonhigh/p/17999544) - Detailed comparison with examples
- [GAMES-图形学系列笔记](https://www.cnblogs.com/apachecn/p/19626591) - Deep dive into HTN vs GOAP differences

### LLM + Hierarchical Planning
- [HiPlan: Hierarchical Planning for LLM-Based Agents](https://arxiv.org/abs/2508.19076) - Academic paper on LLM + HTN integration
- [Deep Agent 任务规划（Planner）是如何实现的](https://m.blog.csdn.net/m0_66858461/article/details/155785878) - Deep Agent planning architecture
- [Survey on Large Language Models for Automated Planning](https://www.yiyibooks.cn/__trs__/arxiv/2502.12435v1/index.html) - LLMs for automated planning survey

### Game AI Examples
- [以《决战！平安京》为例，揭秘MOBA游戏AI设计开发套路](http://www.360doc.com/content/25/1015/16/41461716_1163076660.shtml) - MOBA game AI with HTN
- [游戏叙事中需要怎么样的AI？](https://www.gcores.com/articles/203244) - AI for game narratives

---

**Document Version:** 1.0
**Author:** Claude (Anthropic)
**Date:** 2025-02-28
**Project:** Steve AI - Hierarchical Planning Architecture
**Status:** Design Specification - Ready for Implementation
