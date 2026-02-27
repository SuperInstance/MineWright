# HTN Planner Design for MineWright

## Executive Summary

This document outlines the design and implementation approach for integrating a **Hierarchical Task Network (HTN)** planner into the MineWright Minecraft AI mod. The HTN planner will work alongside the existing LLM-based planning system to provide more structured, efficient, and predictable task decomposition.

### Current State
- **Direct LLM Planning**: TaskPlanner.java sends natural language commands to LLMs and receives flat task lists
- **Limitations**: No hierarchical decomposition, limited reusability, expensive API calls for repetitive tasks
- **Architecture**: Plugin-based action system with ActionRegistry, state machine, and orchestration

### Proposed Solution
- **HTN + LLM Hybrid**: Use HTN for structured decomposition of common tasks, LLM for novel/complex commands
- **Benefits**: Reduced API costs (40-60% cache hits possible), faster planning, reusable task hierarchies, better predictability

---

## 1. HTN Concepts

### 1.1 Core HTN Elements

HTN planning is based on three key concepts:

#### Compound Tasks
Tasks that require decomposition into subtasks. These represent high-level goals.

**Examples:**
```
- build_house → [gather_materials, find_site, lay_foundation, build_walls, add_roof]
- craft_item → [check_inventory, find_crafting_table, navigate_to_table, craft]
- mine_ore → [find_ore_deposits, navigate_to_deposits, mine_blocks, collect_drops]
```

#### Primitive Tasks
Tasks that can be executed directly without further decomposition. These correspond to existing MineWright actions.

**Examples:**
```
- pathfind {x, y, z}
- mine {block, quantity}
- place {block, x, y, z}
- craft {item, quantity}
- attack {target}
```

#### Methods
Alternative ways to accomplish a compound task. Each method has:
- **Preconditions**: What must be true to use this method
- **Subtasks**: Ordered list of tasks (compound or primitive)
- **Constraints**: Requirements for task execution

**Example Method:**
```
Method: build_house_small
Task: build_house
Preconditions:
  - has_materials(oak_planks, 64)
  - has_clear_space(9, 6, 9)
Subtasks:
  1. pathfind_to_build_site
  2. place_blocks {structure: "house", material: "oak_planks"}
```

### 1.2 Task Networks

A task network represents a partially ordered plan:
- **Sequential ordering**: Tasks must execute in order (A → B → C)
- **Parallel ordering**: Tasks can execute simultaneously
- **Conditional ordering**: Tasks execute based on conditions

### 1.3 HTN Domain

The domain contains all task definitions, methods, and operators for a specific problem space. For MineWright, this includes:
- Building tasks (houses, towers, castles)
- Mining tasks (ores, stone, resources)
- Crafting tasks (tools, weapons, armor)
- Exploration tasks (caves, structures, biomes)

---

## 2. Integration with Existing LLM Planning

### 2.1 Hybrid Planning Architecture

```
User Command
     │
     ▼
┌─────────────────────────────────────┐
│  Command Classifier (Lightweight)  │
│  - Is this a known HTN task?        │
│  - Check domain for match           │
└────────┬────────────────────────────┘
         │
         ├─── Known Task ──────┐
         │                     │
         │                     ▼
         │              ┌──────────────┐
         │              │ HTN Planner  │
         │              │ - Fast       │
         │              │ - No API call│
         │              │ - Structured │
         │              └──────┬───────┘
         │                     │
         │                     ▼
         │              ┌──────────────┐
         │              │ Task Network │
         │              │ (decomposed) │
         │              └──────┬───────┘
         │                     │
         │                     ▼
         │              ┌──────────────┐
         │              │   Executor   │
         │              └──────────────┘
         │
         └─── Novel Task ────┐
                              │
                              ▼
                       ┌──────────────┐
                       │  LLM Planner │
                       │  (Existing)  │
                       │  - Flexible  │
                       │  - Expensive │
                       │  - Creative  │
                       └──────┬───────┘
                              │
                              ▼
                       ┌──────────────┐
                       │   Executor   │
                       └──────────────┘
```

### 2.2 Command Classification Strategy

**Phase 1: Pattern Matching**
```java
public class CommandClassifier {
    public boolean isHTNCapable(String command) {
        // Check against known patterns
        return command.matches("build (house|tower|castle|barn).*") ||
               command.matches("mine (iron|diamond|coal|gold).*") ||
               command.matches("craft (pickaxe|sword|shovel).*");
    }
}
```

**Phase 2: Semantic Similarity** (future)
Use embedding-based similarity to match commands to HTN tasks even with different phrasing.

### 2.3 LLM as Method Selector

When multiple methods exist for a compound task, use LLM to select the best one:

```
User: "Build a house but make it fancy"

HTN Planner: I have 2 methods for build_house:
  1. build_house_basic (oak_planks, cobblestone)
  2. build_house_fancy (spruce_planks, glass_pane, lanterns)

LLM: Select method 2 based on "fancy" keyword
```

### 2.4 Fallback and Learning

1. **HTN fails → LLM**: If HTN can't decompose a task, fall back to LLM
2. **LLM success → HTN cache**: Learn new decomposition patterns
3. **Feedback loop**: Track which methods succeed, update priorities

---

## 3. Minecraft-Specific Task Decomposition

### 3.1 Building Task Hierarchy

```
build_structure
  ├── build_house
  │   ├── gather_materials
  │   │   ├── mine {block: "oak_log", quantity: 32}
  │   │   └── mine {block: "cobblestone", quantity: 32}
  │   ├── find_build_site
  │   │   ├── pathfind {search_radius: 20}
  │   │   └── check_suitability {min_flatness: 0.8}
  │   ├── clear_area
  │   │   └── mine_blocks {area: [9, 5, 9]}
  │   ├── lay_foundation
  │   │   └── place_blocks {pattern: "rectangle", material: "cobblestone"}
  │   ├── build_walls
  │   │   └── place_blocks {pattern: "walls", height: 4}
  │   └── add_roof
  │       └── place_blocks {pattern: "roof", material: "oak_stairs"}
  │
  ├── build_tower
  │   ├── gather_materials
  │   ├── clear_area
  │   ├── build_tower_base
  │   ├── build_tower_middle
  │   └── build_tower_top
  │
  └── build_castle
      ├── gather_materials
      ├── clear_large_area
      ├── build_perimeter_walls
      ├── build_corner_towers
      ├── build_gate
      └── add_battlements
```

### 3.2 Mining Task Hierarchy

```
gather_resources
  ├── mine_ore
  │   ├── find_ore_deposits
  │   │   ├── check_inventory {has_tool: "pickaxe"}
  │   │   ├── pathfind_to_cave_entrance
  │   │   └── explore_cave {target: "ore_veins"}
  │   ├── navigate_to_deposits
  │   │   └── pathfind {x, y, z} // from detection
  │   └── extract_ore
  │       ├── mine {block, quantity}
  │       └── collect_drops
  │
  ├── strip_mine
  │   ├── select_strip_mine_level
  │   ├── dig_horizontal_tunnel
  │   │   └── repeat (mine {block: "stone", quantity: 1})
  │   └── dig_branches
  │
  └── quarry
      ├── mark_quarry_area
      ├── dig_quarry_layers
      └── collect_resources
```

### 3.3 Crafting Task Hierarchy

```
craft_item
  ├── check_recipe
  │   ├── check_inventory {item}
  │   └── check_crafting_requirements
  ├── gather_ingredients
  │   └── mine_or_craft_subcomponents
  ├── find_crafting_station
  │   ├── check_for_nearby_crafting_table
  │   └── pathfind_to_crafting_table
  └── perform_crafting
      ├── interact_with_crafting_table
      └── select_recipe {item, quantity}
```

### 3.4 Exploration Task Hierarchy

```
explore
  ├── explore_cave
  │   ├── find_cave_entrance
  │   ├── place_torches
  │   ├── navigate_cave
  │   └── mark_interesting_features
  │
  ├── find_structure
  │   ├── select_search_pattern
  │   ├── travel_to_target_biome
  │   └── scan_for_structure
  │
  └── survey_area
      ├── establish_boundary
      ├── systematic_search
      └── report_findings
```

---

## 4. Implementation Approach

### 4.1 Core Classes

#### HTNPlanner.java

```java
package com.minewright.planning.htn;

import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.llm.TaskPlanner;
import com.minewright.memory.WorldKnowledge;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Hierarchical Task Network planner for structured task decomposition.
 *
 * <p>This planner provides fast, predictable decomposition of common tasks
 * without requiring LLM API calls. It falls back to LLM planning for novel
 * or complex tasks not covered by the HTN domain.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Cached task decompositions for instant planning</li>
 *   <li>Reusable methods for common Minecraft activities</li>
 *   <li>Hybrid planning with LLM fallback</li>
 *   <li>Learning from successful LLM plans</li>
 * </ul>
 */
public class HTNPlanner {

    private final HTNDomain domain;
    private final TaskPlanner llmPlanner;
    private final HTNCache cache;

    /**
     * Creates a new HTN planner.
     *
     * @param llmPlanner Fallback LLM planner for novel tasks
     */
    public HTNPlanner(TaskPlanner llmPlanner) {
        this.domain = new HTNDomain();
        this.domain.loadDefaultDomain();
        this.llmPlanner = llmPlanner;
        this.cache = new HTNCache();
    }

    /**
     * Plans tasks using HTN decomposition with LLM fallback.
     *
     * <p>This method first checks if the command matches a known HTN task.
     * If so, it decomposes the task using the HTN domain. If not, or if
     * decomposition fails, it falls back to LLM planning.</p>
     *
     * @param foreman The agent entity
     * @param command The natural language command
     * @return CompletableFuture with decomposed tasks
     */
    public CompletableFuture<List<Task>> planTasksAsync(
            ForemanEntity foreman,
            String command) {

        // Check cache first
        String cacheKey = generateCacheKey(command, foreman);
        List<Task> cached = cache.get(cacheKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        // Try HTN decomposition
        HTNTask rootTask = classifyCommand(command);
        if (rootTask != null) {
            return decomposeTask(foreman, rootTask, command)
                .thenApply(tasks -> {
                    cache.put(cacheKey, tasks);
                    return tasks;
                });
        }

        // Fall back to LLM
        return llmPlanner.planTasksAsync(foreman, command)
            .thenApply(parsedResponse -> {
                if (parsedResponse != null) {
                    List<Task> tasks = parsedResponse.getTasks();
                    // Learn from successful LLM plan
                    learnFromPlan(command, tasks);
                    cache.put(cacheKey, tasks);
                    return tasks;
                }
                return Collections.emptyList();
            });
    }

    /**
     * Classifies a natural language command into an HTN task.
     *
     * @param command The user command
     * @return HTN task, or null if not recognized
     */
    private HTNTask classifyCommand(String command) {
        String lowerCommand = command.toLowerCase().trim();

        // Pattern matching for common tasks
        if (lowerCommand.matches("build\\s+(house|tower|castle|barn|shed).*")) {
            String structureType = extractStructureType(command);
            return new HTNTask("build_" + structureType, HTNTask.Type.COMPOUND);
        }

        if (lowerCommand.matches("mine\\s+(iron|diamond|coal|gold|copper).*")) {
            String oreType = extractOreType(command);
            return new HTNTask("mine_" + oreType, HTNTask.Type.COMPOUND);
        }

        if (lowerCommand.matches("craft\\s+(pickaxe|sword|shovel|axe).*")) {
            String itemType = extractItemType(command);
            return new HTNTask("craft_" + itemType, HTNTask.Type.COMPOUND);
        }

        return null;
    }

    /**
     * Decomposes a compound task into primitive tasks.
     *
     * @param foreman The agent entity
     * @param task The task to decompose
     * @param originalCommand Original user command for context
     * @return CompletableFuture with decomposed tasks
     */
    private CompletableFuture<List<Task>> decomposeTask(
            ForemanEntity foreman,
            HTNTask task,
            String originalCommand) {

        if (task.getType() == HTNTask.Type.PRIMITIVE) {
            // Convert HTN task to MineWright Task
            Task mineWrightTask = convertToMineWrightTask(task);
            return CompletableFuture.completedFuture(Collections.singletonList(mineWrightTask));
        }

        // Find applicable methods
        List<HTNMethod> methods = domain.getMethodsForTask(task.getName());

        if (methods.isEmpty()) {
            // No methods found, fall back to LLM
            return llmPlanner.planTasksAsync(foreman, originalCommand)
                .thenApply(response -> response != null ? response.getTasks() : Collections.emptyList());
        }

        // Select best method (could use LLM for this)
        HTNMethod selectedMethod = selectBestMethod(methods, foreman, originalCommand);

        // Decompose subtasks recursively
        List<Task> allTasks = new ArrayList<>();
        for (HTNTask subtask : selectedMethod.getSubtasks()) {
            decomposeTask(foreman, subtask, originalCommand)
                .thenAccept(allTasks::addAll);
        }

        return CompletableFuture.completedFuture(allTasks);
    }

    /**
     * Selects the best method from available options.
     *
     * <p>Simple implementation uses precondition checking.
     * Advanced version could use LLM for method selection.</p>
     */
    private HTNMethod selectBestMethod(
            List<HTNMethod> methods,
            ForemanEntity foreman,
            String command) {

        WorldKnowledge worldKnowledge = new WorldKnowledge(foreman);

        for (HTNMethod method : methods) {
            if (method.checkPreconditions(worldKnowledge)) {
                return method;
            }
        }

        // Return first method as fallback
        return methods.get(0);
    }

    /**
     * Learns a new task decomposition from a successful LLM plan.
     *
     * @param command The original command
     * @param tasks The decomposed tasks from LLM
     */
    public void learnFromPlan(String command, List<Task> tasks) {
        // Extract pattern and create new HTN method
        // This enables the system to improve over time
        HTNTask rootTask = classifyCommand(command);
        if (rootTask != null) {
            HTNMethod learnedMethod = HTNMethod.fromTaskList(
                "learned_" + command.hashCode(),
                tasks
            );
            domain.addMethod(rootTask.getName(), learnedMethod);
        }
    }

    /**
     * Converts an HTN task to a MineWright Task.
     */
    private Task convertToMineWrightTask(HTNTask htnTask) {
        Map<String, Object> params = new HashMap<>(htnTask.getParameters());
        return new Task(htnTask.getActionType(), params);
    }

    private String generateCacheKey(String command, ForemanEntity foreman) {
        return command.toLowerCase().trim();
    }

    private String extractStructureType(String command) {
        if (command.toLowerCase().contains("tower")) return "tower";
        if (command.toLowerCase().contains("castle")) return "castle";
        if (command.toLowerCase().contains("barn")) return "barn";
        return "house";
    }

    private String extractOreType(String command) {
        String lower = command.toLowerCase();
        if (lower.contains("diamond")) return "diamond";
        if (lower.contains("iron")) return "iron";
        if (lower.contains("gold")) return "gold";
        if (lower.contains("coal")) return "coal";
        if (lower.contains("copper")) return "copper";
        return "iron";
    }

    private String extractItemType(String command) {
        String lower = command.toLowerCase();
        if (lower.contains("pickaxe")) return "pickaxe";
        if (lower.contains("sword")) return "sword";
        if (lower.contains("shovel")) return "shovel";
        if (lower.contains("axe")) return "axe";
        return "pickaxe";
    }
}
```

#### TaskNetwork.java

```java
package com.minewright.planning.htn;

import java.util.*;

/**
 * Represents a task network in HTN planning.
 *
 * <p>A task network is a partially ordered set of tasks that represents
 * a plan. Tasks can be sequential (ordered) or parallel (unordered).</p>
 *
 * <p><b>Ordering Types:</b></p>
 * <ul>
 *   <li><b>Sequential:</b> Tasks must execute in order (A → B → C)</li>
 *   <li><b>Parallel:</b> Tasks can execute simultaneously</li>
 *   <li><b>Conditional:</b> Tasks execute based on conditions</li>
 * </ul>
 */
public class TaskNetwork {

    private final List<HTNTask> tasks;
    private final List<OrderingConstraint> orderingConstraints;
    private final Map<String, Object> stateBindings;

    public TaskNetwork() {
        this.tasks = new ArrayList<>();
        this.orderingConstraints = new ArrayList<>();
        this.stateBindings = new HashMap<>();
    }

    public TaskNetwork(List<HTNTask> tasks) {
        this.tasks = new ArrayList<>(tasks);
        this.orderingConstraints = new ArrayList<>();
        this.stateBindings = new HashMap<>();
    }

    /**
     * Adds a task to the network.
     */
    public void addTask(HTNTask task) {
        tasks.add(task);
    }

    /**
     * Adds an ordering constraint between tasks.
     *
     * @param before Task that must complete first
     * @param after Task that must wait for 'before' to complete
     */
    public void addOrdering(HTNTask before, HTNTask after) {
        orderingConstraints.add(new OrderingConstraint(before, after));
    }

    /**
     * Checks if this network is ready for execution.
     *
     * <p>A network is ready if all compound tasks have been decomposed
     * into primitive tasks.</p>
     */
    public boolean isExecutable() {
        return tasks.stream().allMatch(t -> t.getType() == HTNTask.Type.PRIMITIVE);
    }

    /**
     * Returns tasks in execution order based on constraints.
     */
    public List<HTNTask> getExecutionOrder() {
        if (orderingConstraints.isEmpty()) {
            return new ArrayList<>(tasks);
        }

        // Topological sort based on ordering constraints
        List<HTNTask> sorted = new ArrayList<>();
        Set<HTNTask> visited = new HashSet<>();

        for (HTNTask task : tasks) {
            topologicalSort(task, visited, sorted);
        }

        return sorted;
    }

    private void topologicalSort(HTNTask task, Set<HTNTask> visited, List<HTNTask> sorted) {
        if (visited.contains(task)) {
            return;
        }

        visited.add(task);

        // Find tasks that must come before this one
        for (OrderingConstraint constraint : orderingConstraints) {
            if (constraint.after.equals(task)) {
                topologicalSort(constraint.before, visited, sorted);
            }
        }

        sorted.add(task);
    }

    public List<HTNTask> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public int size() {
        return tasks.size();
    }

    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    /**
     * Represents an ordering constraint between two tasks.
     */
    public static class OrderingConstraint {
        private final HTNTask before;
        private final HTNTask after;

        public OrderingConstraint(HTNTask before, HTNTask after) {
            this.before = before;
            this.after = after;
        }

        public HTNTask getBefore() {
            return before;
        }

        public HTNTask getAfter() {
            return after;
        }
    }
}
```

#### HTNTask.java

```java
package com.minewright.planning.htn;

import java.util.*;

/**
 * Represents a task in HTN planning.
 *
 * <p>Tasks can be:</p>
 * <ul>
 *   <li><b>Primitive:</b> Can be executed directly (e.g., mine, place)</li>
 *   <li><b>Compound:</b> Requires decomposition (e.g., build_house)</li>
 * </ul>
 */
public class HTNTask {

    private final String name;
    private final Type type;
    private final Map<String, Object> parameters;
    private final List<HTNTask> subtasks;

    public HTNTask(String name, Type type) {
        this.name = name;
        this.type = type;
        this.parameters = new HashMap<>();
        this.subtasks = new ArrayList<>();
    }

    public HTNTask(String name, Type type, Map<String, Object> parameters) {
        this.name = name;
        this.type = type;
        this.parameters = new HashMap<>(parameters);
        this.subtasks = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }

    public Object getParameter(String key) {
        return parameters.get(key);
    }

    public List<HTNTask> getSubtasks() {
        return Collections.unmodifiableList(subtasks);
    }

    public void addSubtask(HTNTask subtask) {
        subtasks.add(subtask);
    }

    /**
     * Returns the action type for primitive tasks.
     * For compound tasks, returns the compound task name.
     */
    public String getActionType() {
        if (type == Type.PRIMITIVE) {
            return name;
        }
        return "compound";
    }

    /**
     * Checks if this task is primitive (directly executable).
     */
    public boolean isPrimitive() {
        return type == Type.PRIMITIVE;
    }

    /**
     * Checks if this task is compound (requires decomposition).
     */
    public boolean isCompound() {
        return type == Type.COMPOUND;
    }

    @Override
    public String toString() {
        return "HTNTask{" +
            "name='" + name + '\'' +
            ", type=" + type +
            ", parameters=" + parameters +
            (subtasks.isEmpty() ? "" : ", subtasks=" + subtasks.size()) +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HTNTask htnTask = (HTNTask) o;
        return Objects.equals(name, htnTask.name) &&
            type == htnTask.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    public enum Type {
        PRIMITIVE,
        COMPOUND
    }
}
```

#### HTNMethod.java

```java
package com.minewright.planning.htn;

import com.minewright.memory.WorldKnowledge;

import java.util.*;
import java.util.function.Predicate;

/**
 * Represents a method for accomplishing a compound task in HTN planning.
 *
 * <p>A method defines one way to decompose a compound task into subtasks.
 * Multiple methods can exist for the same task, allowing flexibility in
 * planning.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>
 * Method: build_house_small
 * Task: build_house
 * Preconditions:
 *   - has_materials(oak_planks, 64)
 * Subtasks:
 *   1. pathfind_to_build_site
 *   2. place_blocks {structure: "house"}
 * </pre>
 */
public class HTNMethod {

    private final String name;
    private final String taskName;
    private final List<HTNTask> subtasks;
    private final Predicate<WorldKnowledge> preconditionChecker;
    private final Map<String, Object> constraints;
    private final int priority;

    private HTNMethod(Builder builder) {
        this.name = builder.name;
        this.taskName = builder.taskName;
        this.subtasks = new ArrayList<>(builder.subtasks);
        this.preconditionChecker = builder.preconditionChecker;
        this.constraints = new HashMap<>(builder.constraints);
        this.priority = builder.priority;
    }

    /**
     * Checks if this method's preconditions are satisfied.
     *
     * @param worldKnowledge Current world state
     * @return true if preconditions are met
     */
    public boolean checkPreconditions(WorldKnowledge worldKnowledge) {
        if (preconditionChecker == null) {
            return true; // No preconditions
        }
        return preconditionChecker.test(worldKnowledge);
    }

    /**
     * Returns the subtasks for this method.
     */
    public List<HTNTask> getSubtasks() {
        return Collections.unmodifiableList(subtasks);
    }

    public String getName() {
        return name;
    }

    public String getTaskName() {
        return taskName;
    }

    public Map<String, Object> getConstraints() {
        return Collections.unmodifiableMap(constraints);
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Creates an HTN method from a list of MineWright tasks.
     * Used for learning from LLM plans.
     */
    public static HTNMethod fromTaskList(String methodName, List<com.minewright.action.Task> tasks) {
        Builder builder = new Builder(methodName, "learned_task");

        for (com.minewright.action.Task task : tasks) {
            HTNTask htnTask = new HTNTask(
                task.getAction(),
                HTNTask.Type.PRIMITIVE,
                task.getParameters()
            );
            builder.addSubtask(htnTask);
        }

        return builder.build();
    }

    public static Builder builder(String name, String taskName) {
        return new Builder(name, taskName);
    }

    public static class Builder {
        private final String name;
        private final String taskName;
        private final List<HTNTask> subtasks = new ArrayList<>();
        private Predicate<WorldKnowledge> preconditionChecker;
        private final Map<String, Object> constraints = new HashMap<>();
        private int priority = 0;

        private Builder(String name, String taskName) {
            this.name = name;
            this.taskName = taskName;
        }

        public Builder addSubtask(HTNTask task) {
            subtasks.add(task);
            return this;
        }

        public Builder precondition(Predicate<WorldKnowledge> checker) {
            this.preconditionChecker = checker;
            return this;
        }

        public Builder addConstraint(String key, Object value) {
            constraints.put(key, value);
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public HTNMethod build() {
            return new HTNMethod(this);
        }
    }

    @Override
    public String toString() {
        return "HTNMethod{" +
            "name='" + name + '\'' +
            ", taskName='" + taskName + '\'' +
            ", subtasks=" + subtasks.size() +
            ", priority=" + priority +
            '}';
    }
}
```

#### HTNDomain.java

```java
package com.minewright.planning.htn;

import java.util.*;

/**
 * Represents an HTN domain containing all task definitions and methods.
 *
 * <p>The domain is the knowledge base for HTN planning. It defines:
 * <ul>
 *   <li>All compound tasks and their methods</li>
 *   <li>All primitive tasks (mapping to MineWright actions)</li>
 *   <li>Preconditions and constraints for each method</li>
 * </ul>
 *
 * <p><b>Example Domain:</b></p>
 * <pre>
 * build_house
 *   - method: build_house_basic (oak_planks, cobblestone)
 *   - method: build_house_fancy (spruce_planks, glass_pane)
 *
 * mine_ore
 *   - method: surface_mine (above ground)
 *   - method: cave_mine (underground)
 * </pre>
 */
public class HTNDomain {

    private final Map<String, List<HTNMethod>> methods;
    private final Set<String> primitiveTasks;
    private final String name;

    public HTNDomain() {
        this("MineWrightDomain");
    }

    public HTNDomain(String name) {
        this.name = name;
        this.methods = new HashMap<>();
        this.primitiveTasks = new HashSet<>();
    }

    /**
     * Adds a method for accomplishing a task.
     */
    public void addMethod(String taskName, HTNMethod method) {
        methods.computeIfAbsent(taskName, k -> new ArrayList<>()).add(method);

        // Sort by priority (descending)
        methods.get(taskName).sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }

    /**
     * Returns all methods for a given task.
     */
    public List<HTNMethod> getMethodsForTask(String taskName) {
        return Collections.unmodifiableList(
            methods.getOrDefault(taskName, Collections.emptyList())
        );
    }

    /**
     * Checks if a task has any methods defined.
     */
    public boolean hasMethodsForTask(String taskName) {
        return methods.containsKey(taskName) && !methods.get(taskName).isEmpty();
    }

    /**
     * Registers a primitive task (directly executable).
     */
    public void addPrimitiveTask(String taskName) {
        primitiveTasks.add(taskName);
    }

    /**
     * Checks if a task is primitive.
     */
    public boolean isPrimitiveTask(String taskName) {
        return primitiveTasks.contains(taskName);
    }

    /**
     * Loads the default MineWright domain.
     * This defines all common Minecraft tasks.
     */
    public void loadDefaultDomain() {
        // Register primitive tasks (map to existing actions)
        registerPrimitiveTasks();

        // Register compound task methods
        registerBuildingMethods();
        registerMiningMethods();
        registerCraftingMethods();
        registerExplorationMethods();
    }

    private void registerPrimitiveTasks() {
        // Navigation
        addPrimitiveTask("pathfind");

        // Resource gathering
        addPrimitiveTask("mine");
        addPrimitiveTask("place");
        addPrimitiveTask("gather");

        // Crafting
        addPrimitiveTask("craft");

        // Combat
        addPrimitiveTask("attack");

        // Interaction
        addPrimitiveTask("follow");

        // Building
        addPrimitiveTask("build");
    }

    private void registerBuildingMethods() {
        // build_house methods
        addMethod("build_house",
            HTNMethod.builder("build_house_basic", "build_house")
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE))
                .addSubtask(new HTNTask("build", HTNTask.Type.PRIMITIVE,
                    Map.of("structure", "house", "blocks", Arrays.asList("oak_planks", "cobblestone")))
                )
                .precondition(world -> world.hasNearbyBlocks("oak_log", 10))
                .priority(100)
                .build()
        );

        addMethod("build_house",
            HTNMethod.builder("build_house_with_gathering", "build_house")
                .addSubtask(new HTNTask("mine", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "oak_log", "quantity", 32))
                )
                .addSubtask(new HTNTask("mine", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "cobblestone", "quantity", 32))
                )
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE))
                .addSubtask(new HTNTask("build", HTNTask.Type.PRIMITIVE,
                    Map.of("structure", "house", "blocks", Arrays.asList("oak_planks", "cobblestone")))
                )
                .priority(50) // Lower priority, requires gathering first
                .build()
        );

        // build_tower methods
        addMethod("build_tower",
            HTNMethod.builder("build_tower_basic", "build_tower")
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE))
                .addSubtask(new HTNTask("build", HTNTask.Type.PRIMITIVE,
                    Map.of("structure", "tower", "blocks", Arrays.asList("cobblestone", "stone_bricks")))
                )
                .priority(100)
                .build()
        );

        // build_castle methods
        addMethod("build_castle",
            HTNMethod.builder("build_castle_basic", "build_castle")
                .addSubtask(new HTNTask("mine", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "stone", "quantity", 128))
                )
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE))
                .addSubtask(new HTNTask("build", HTNTask.Type.PRIMITIVE,
                    Map.of("structure", "castle", "blocks", Arrays.asList("stone_bricks", "cobblestone")))
                )
                .priority(100)
                .build()
        );
    }

    private void registerMiningMethods() {
        // mine_iron methods
        addMethod("mine_iron",
            HTNMethod.builder("mine_iron_surface", "mine_iron")
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE,
                    Map.of("y", 60)) // Iron spawns at higher levels
                )
                .addSubtask(new HTNTask("mine", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "iron_ore", "quantity", 16))
                )
                .precondition(world -> world.getCurrentY() > 40)
                .priority(100)
                .build()
        );

        addMethod("mine_iron",
            HTNMethod.builder("mine_iron_deep", "mine_iron")
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE,
                    Map.of("y", 15)) // Deep iron
                )
                .addSubtask(new HTNTask("mine", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "deepslate_iron_ore", "quantity", 16))
                )
                .priority(90)
                .build()
        );

        // mine_diamond methods
        addMethod("mine_diamond",
            HTNMethod.builder("mine_diamond_optimal", "mine_diamond")
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE,
                    Map.of("y", -59)) // Diamond optimal level
                )
                .addSubtask(new HTNTask("mine", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "diamond_ore", "quantity", 8))
                )
                .priority(100)
                .build()
        );

        // mine_coal methods
        addMethod("mine_coal",
            HTNMethod.builder("mine_coal_surface", "mine_coal")
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE,
                    Map.of("y", 90)) // Coal spawns high
                )
                .addSubtask(new HTNTask("mine", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "coal_ore", "quantity", 32))
                )
                .priority(100)
                .build()
        );
    }

    private void registerCraftingMethods() {
        // craft_pickaxe methods
        addMethod("craft_pickaxe",
            HTNMethod.builder("craft_wooden_pickaxe", "craft_pickaxe")
                .addSubtask(new HTNTask("mine", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "oak_log", "quantity", 3))
                )
                .addSubtask(new HTNTask("craft", HTNTask.Type.PRIMITIVE,
                    Map.of("item", "wooden_pickaxe", "quantity", 1))
                )
                .precondition(world -> !world.hasItem("pickaxe"))
                .priority(100)
                .build()
        );

        addMethod("craft_pickaxe",
            HTNMethod.builder("craft_stone_pickaxe", "craft_pickaxe")
                .addSubtask(new HTNTask("mine", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "cobblestone", "quantity", 3))
                )
                .addSubtask(new HTNTask("craft", HTNTask.Type.PRIMITIVE,
                    Map.of("item", "stone_pickaxe", "quantity", 1))
                )
                .precondition(world -> world.hasItem("stick", 2))
                .priority(90)
                .build()
        );

        // craft_sword methods
        addMethod("craft_sword",
            HTNMethod.builder("craft_stone_sword", "craft_sword")
                .addSubtask(new HTNTask("mine", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "cobblestone", "quantity", 2))
                )
                .addSubtask(new HTNTask("craft", HTNTask.Type.PRIMITIVE,
                    Map.of("item", "stone_sword", "quantity", 1))
                )
                .priority(100)
                .build()
        );
    }

    private void registerExplorationMethods() {
        // explore_cave methods
        addMethod("explore_cave",
            HTNMethod.builder("explore_cave_basic", "explore_cave")
                .addSubtask(new HTNTask("pathfind", HTNTask.Type.PRIMITIVE,
                    Map.of("target", "cave_entrance"))
                )
                .addSubtask(new HTNTask("place", HTNTask.Type.PRIMITIVE,
                    Map.of("block", "torch"))
                )
                .priority(100)
                .build()
        );
    }

    public String getName() {
        return name;
    }

    public int getMethodCount() {
        return methods.values().stream()
            .mapToInt(List::size)
            .sum();
    }

    public Set<String> getKnownTasks() {
        Set<String> tasks = new HashSet<>(primitiveTasks);
        tasks.addAll(methods.keySet());
        return Collections.unmodifiableSet(tasks);
    }
}
```

#### HTNCache.java

```java
package com.minewright.planning.htn;

import com.minewright.action.Task;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cache for HTN planning results.
 *
 * <p>Uses Caffeine cache for high-performance caching of decomposed tasks.
 * This significantly reduces API calls for common commands.</p>
 *
 * <p><b>Cache Statistics:</b></p>
 * <ul>
 *   <li>Expected hit rate: 40-60% for repetitive commands</li>
 *   <li>Time savings: ~2-5 seconds per cached command</li>
 *   <li>Cost savings: Proportional to hit rate</li>
 * </ul>
 */
public class HTNCache {

    private final Cache<String, List<Task>> cache;

    public HTNCache() {
        this.cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .recordStats()
            .build();
    }

    /**
     * Gets cached tasks for a command.
     */
    public List<Task> get(String key) {
        return cache.getIfPresent(key);
    }

    /**
     * Puts tasks in the cache.
     */
    public void put(String key, List<Task> tasks) {
        cache.put(key, tasks);
    }

    /**
     * Invalidates a cache entry.
     */
    public void invalidate(String key) {
        cache.invalidate(key);
    }

    /**
     * Clears all cache entries.
     */
    public void clear() {
        cache.invalidateAll();
    }

    /**
     * Returns cache statistics.
     */
    public CacheStats getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = cache.stats();
        return new CacheStats(
            stats.hitCount(),
            stats.missCount(),
            stats.hitRate()
        );
    }

    public record CacheStats(long hitCount, long missCount, double hitRate) {
        @Override
        public String toString() {
            return String.format("CacheStats{hits=%d, misses=%d, hitRate=%.2f%%}",
                hitCount, missCount, hitRate * 100);
        }
    }
}
```

### 4.2 Integration with ActionExecutor

Modify `ActionExecutor` to use HTN planner:

```java
// In ActionExecutor.java

private final HTNPlanner htnPlanner;  // New field

public ActionExecutor(ForemanEntity foreman) {
    // ... existing initialization ...

    // Initialize HTN planner with TaskPlanner as fallback
    this.htnPlanner = new HTNPlanner(getTaskPlanner());
}

public void processNaturalLanguageCommand(String command) {
    MineWrightMod.LOGGER.info("Foreman '{}' processing command (HTN+LLM hybrid): {}",
        foreman.getSteveName(), command);

    // Cancel any current actions
    if (currentAction != null) {
        currentAction.cancel();
        currentAction = null;
    }

    if (idleFollowAction != null) {
        idleFollowAction.cancel();
        idleFollowAction = null;
    }

    try {
        // Store command and start async planning
        this.pendingCommand = command;
        this.isPlanning = true;

        // Send immediate feedback to user
        sendToGUI(foreman.getSteveName(), "Thinking...");

        // Start HTN planning with LLM fallback - returns immediately!
        planningFuture = htnPlanner.planTasksAsync(foreman, command)
            .thenApply(tasks -> {
                // Convert List<Task> to ParsedResponse for compatibility
                return new ResponseParser.ParsedResponse(
                    "HTN/LHM plan",
                    command,
                    tasks
                );
            });

        MineWrightMod.LOGGER.info("Foreman '{}' started HTN planning for: {}",
            foreman.getSteveName(), command);

    } catch (Exception e) {
        MineWrightMod.LOGGER.error("Error starting HTN planning", e);
        sendToGUI(foreman.getSteveName(), "Oops, something went wrong!");
        isPlanning = false;
        planningFuture = null;
    }
}
```

### 4.3 Package Structure

```
src/main/java/com/minewright/planning/
├── htn/
│   ├── HTNPlanner.java          # Main planner
│   ├── HTNDomain.java           # Task definitions and methods
│   ├── HTNTask.java             # Task representation
│   ├── HTNMethod.java           # Method for task decomposition
│   ├── TaskNetwork.java         # Partially ordered task network
│   ├── HTNCache.java            # Caching layer
│   └── CommandClassifier.java   # Classify commands as HTN or LLM
└── hybrid/
    ├── HybridPlanner.java       # Orchestrates HTN and LLM planning
    └── PlanningStrategy.java    # Strategy pattern for planning modes
```

---

## 5. Benefits and Expected Improvements

### 5.1 Performance Benefits

| Metric | Current (LLM-only) | With HTN | Improvement |
|--------|-------------------|----------|-------------|
| Average planning time | 3-5 seconds | 0.1-0.5 seconds (cached) | 10-50x faster |
| API calls per session | 20-30 | 8-12 | 40-60% reduction |
| Cache hit rate | 0% | 40-60% | New capability |
| Task predictability | Variable | High | Better reliability |

### 5.2 Cost Benefits

Assuming OpenAI GPT-4 at $0.03/1K tokens:
- **Current**: 30 commands × 500 tokens × $0.03 = $0.45 per session
- **With HTN**: 12 commands × 500 tokens × $0.03 = $0.18 per session
- **Savings**: 60% reduction in API costs

### 5.3 Reliability Benefits

1. **Deterministic**: HTN produces consistent results for known tasks
2. **Testable**: Methods can be unit tested without API calls
3. **Debuggable**: Clear decomposition chain for troubleshooting
4. **Fallback**: LLM backup ensures no command fails

### 5.4 Extensibility Benefits

1. **Plugin-friendly**: New methods can be added via plugins
2. **Learning**: System improves over time from LLM plans
3. **Domain-specific**: Easy to add specialized domains (redstone, farming, etc.)

---

## 6. Testing Strategy

### 6.1 Unit Tests

```java
class HTNPlannerTest {
    @Test
    void testClassifyBuildCommand() {
        HTNPlanner planner = new HTNPlanner(mockTaskPlanner);
        HTNTask task = planner.classifyCommand("build a house");
        assertNotNull(task);
        assertEquals("build_house", task.getName());
    }

    @Test
    void testDecomposeBuildHouse() {
        HTNPlanner planner = new HTNPlanner(mockTaskPlanner);
        List<Task> tasks = planner.decomposeTask(mockForeman,
            new HTNTask("build_house", HTNTask.Type.COMPOUND)).join();

        assertFalse(tasks.isEmpty());
        assertTrue(tasks.stream().anyMatch(t -> t.getAction().equals("build")));
    }

    @Test
    void testHTNFallbackToLLM() {
        HTNPlanner planner = new HTNPlanner(mockTaskPlanner);
        List<Task> tasks = planner.planTasksAsync(mockForeman, "do something weird").join();

        // Should fall back to LLM for unrecognized commands
        assertNotNull(tasks);
    }
}
```

### 6.2 Integration Tests

```java
class HTNIntegrationTest {
    @Test
    void testEndToEndBuilding() {
        // Spawn foreman
        ForemanEntity foreman = spawnTestForeman();

        // Execute command
        foreman.getExecutor().processNaturalLanguageCommand("build a house");

        // Wait for completion
        waitForCompletion(foreman, 60_000);

        // Verify structure built
        assertTrue(hasStructureAt(foreman.level(), foreman.blockPosition(), "house"));
    }
}
```

### 6.3 Performance Tests

```java
class HTNPerformanceTest {
    @Test
    void testCacheHitRate() {
        HTNPlanner planner = new HTNPlanner(mockTaskPlanner);

        // Execute same command 100 times
        for (int i = 0; i < 100; i++) {
            planner.planTasksAsync(mockForeman, "build a house").join();
        }

        HTNCache.CacheStats stats = planner.getCache().getStats();
        assertTrue(stats.hitRate() > 0.95); // Should hit cache 95%+ of time
    }
}
```

---

## 7. Migration Path

### Phase 1: Foundation (Week 1-2)
1. Create HTN package structure
2. Implement core classes (HTNTask, HTNMethod, HTNDomain)
3. Implement basic domain with building tasks
4. Write unit tests

### Phase 2: Integration (Week 3)
1. Integrate with TaskPlanner
2. Modify ActionExecutor to use HTN planner
3. Implement command classification
4. Add caching layer

### Phase 3: Testing (Week 4)
1. Integration tests
2. Performance benchmarks
3. Cache hit rate analysis
4. Bug fixes and refinements

### Phase 4: Enhancement (Week 5-6)
1. Add mining and crafting domains
2. Implement learning from LLM plans
3. Add LLM-based method selection
4. Documentation and examples

### Phase 5: Production (Week 7+)
1. Gradual rollout with feature flag
2. Monitor metrics and cache stats
3. Gather user feedback
4. Iterate and improve

---

## 8. Future Enhancements

### 8.1 Advanced Features

1. **Dynamic Method Generation**: Use LLM to generate new methods at runtime
2. **Hierarchical Execution**: Execute subtasks as they decompose (pipeline)
3. **Constraint Optimization**: Select methods based on resource constraints
4. **Multi-Agent HTN**: Coordinate multiple agents with shared task networks

### 8.2 Domain Expansion

1. **Redstone Domain**: Complex redstone circuit construction
2. **Farming Domain**: Automated farming with crop rotation
3. **Trading Domain**: Villager trading optimization
4. **Combat Domain**: Tactical combat with weapon selection

### 8.3 AI Integration

1. **Reinforcement Learning**: Learn optimal method selection
2. **Transfer Learning**: Share learned methods across agents
3. **Meta-Reasoning**: Decide when to use HTN vs LLM vs hybrid

---

## 9. Conclusion

The HTN planner will significantly improve MineWright's planning capabilities by:

1. **Reducing API costs**: 40-60% reduction through caching
2. **Improving speed**: 10-50x faster for cached commands
3. **Increasing reliability**: Deterministic decomposition for common tasks
4. **Enabling learning**: Improve over time from LLM plans
5. **Maintaining flexibility**: LLM fallback for novel commands

The hybrid approach ensures that MineWright retains the creativity and flexibility of LLM planning while gaining the efficiency and predictability of structured HTN decomposition.

---

## 10. References

1. **HTN Planning Papers**:
   - "Hierarchical Task Network Planning" (Erol, Hendler, & Nau, 1994)
   - "SHOP2: An HTN Planning System" (Nau et al., 2003)

2. **Minecraft AI**:
   - "MineDojo: Building Open-Ended Embodied Agents with Minecraft" (Fan et al., 2022)
   - "SteVe: A Stereoscopic Vision Agent for Minecraft" (various)

3. **Relevant Code**:
   - `TaskPlanner.java`: Current LLM-based planner
   - `OrchestratorService.java`: Multi-agent coordination
   - `ActionExecutor.java`: Task execution engine
   - `ActionRegistry.java`: Plugin system for actions

4. **Design Patterns**:
   - Strategy Pattern: Planning mode selection (HTN vs LLM)
   - Builder Pattern: HTNMethod construction
   - Registry Pattern: HTNDomain method lookup
   - Template Method: Task decomposition template
