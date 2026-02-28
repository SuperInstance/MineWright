# Chapter 7: Comprehensive Pattern Library
## Game AI Automation Techniques Without LLMs

**Dissertation Chapter:** 7
**Date:** February 28, 2026
**Subject:** Comprehensive catalog of AI patterns from 30 years of game development
**Application:** Building autonomous agents without machine learning dependencies

---

## Table of Contents

1. [Decision Patterns](#1-decision-patterns)
2. [Memory Patterns](#2-memory-patterns)
3. [Coordination Patterns](#3-coordination-patterns)
4. [Learning Without ML](#4-learning-without-ml)
5. [Reactive Patterns](#5-reactive-patterns)
6. [Proactive Patterns](#6-proactive-patterns)
7. [Pattern Selection Guide](#7-pattern-selection-guide)
8. [Implementation Matrix](#8-implementation-matrix)
9. [References](#9-references)

---

## 1. Decision Patterns

Decision patterns determine how an AI agent chooses between available actions. These patterns form the core of autonomous behavior and have been refined through decades of game development.

### 1.1 Priority-Based Selection

**Description:** Actions are arranged in a strict priority order. The agent evaluates conditions from highest to lowest priority, executing the first action whose conditions are met.

**When to Use:**
- Clear hierarchy of importance (survival > tasks > idle)
- Deterministic behavior is required
- Fast decision-making is critical
- Simple, predictable AI is desired

**Pseudocode Implementation:**
```python
def select_action(agent, world):
    # Evaluate in priority order
    if is_critical Danger(agent, world):
        return EMERGENCY_ACTION
    elif has_critical_task(agent):
        return CRITICAL_TASK
    elif has_opportunity(agent, world):
        return OPPORTUNITY_ACTION
    elif has_assigned_task(agent):
        return ASSIGNED_TASK
    else:
        return IDLE_BEHAVIOR

# Example: Minecraft mining agent
def mining_agent_priority(agent):
    if agent.health < 20:
        return "flee_to_safety"
    elif agent.inventory.full():
        return "deposit_items"
    elif agent.sees_hostile():
        return "combat"
    elif agent.has_assigned_block():
        return "mine_target"
    elif agent.sees_valuable_ore():
        return "mine_opportunity"
    else:
        return "patrol"
```

**Minecraft Application:**
```java
public class PriorityBasedDecision {

    public Action selectAction(SteveEntity steve, World world) {
        // Priority 1: Survival
        if (steve.getHealth() < 6.0f) {
            return new FleeAction(steve, findSafeLocation());
        }
        if (hasHostileNearby(steve, world)) {
            return new CombatAction(steve, findNearestHostile());
        }

        // Priority 2: Critical Tasks
        Task current = steve.getCurrentTask();
        if (current != null && current.isUrgent()) {
            return createActionForTask(current);
        }

        // Priority 3: Opportunities
        if (seesValuableResource(steve)) {
            return new GatherAction(steve, getNearestResource());
        }

        // Priority 4: Assigned Work
        if (current != null) {
            return createActionForTask(current);
        }

        // Priority 5: Idle Behavior
        return new IdleAction(steve);
    }
}
```

**Variations:**

**Priority with Interruption:**
```java
public class InterruptiblePriority {
    private Action currentAction;

    public void tick(SteveEntity steve) {
        Action newAction = selectAction(steve);

        // Only interrupt if new action has significantly higher priority
        if (currentAction == null ||
            newAction.getPriority() > currentAction.getPriority() + 2) {
            currentAction = newAction;
        }

        currentAction.execute();
    }
}
```

### 1.2 Weighted Random Selection

**Description:** Each action is assigned a weight. Actions with higher weights are selected more frequently, but all actions have some probability of being chosen. Useful for variety and unpredictability.

**When to Use:**
- Human-like unpredictability is desired
- Multiple valid actions exist
- Exploration of different strategies is needed
- Avoiding repetitive behavior

**Pseudocode Implementation:**
```python
def weighted_random_select(actions, weights):
    """
    Select action based on weights using weighted random selection
    """
    total_weight = sum(weights)
    rand = random.uniform(0, total_weight)

    cumulative = 0
    for action, weight in zip(actions, weights):
        cumulative += weight
        if rand <= cumulative:
            return action

    return actions[-1]  # Fallback to last action

# Example: Resource gathering weights
def select_gathering_target(agent):
    targets = {
        "diamond": 1,      # Rare, valuable
        "iron": 5,         # Common, useful
        "coal": 10,        # Very common
        "dirt": 2          # Sometimes needed
    }

    available = [t for t in targets if agent.can_reach(t)]
    weights = [targets[t] for t in available]

    return weighted_random_select(available, weights)
```

**Minecraft Application:**
```java
public class WeightedRandomDecision {

    private final Map<ActionType, Double> baseWeights = new HashMap<>();

    public WeightedRandomDecision() {
        // Initialize weights based on utility
        baseWeights.put(ActionType.MINE_DIAMOND, 10.0);
        baseWeights.put(ActionType.MINE_IRON, 25.0);
        baseWeights.put(ActionType.MINE_COAL, 40.0);
        baseWeights.put(ActionType.EXPLORE, 20.0);
        baseWeights.put(ActionType.RESTOCK, 15.0);
    }

    public Action selectAction(SteveEntity steve, List<Action> available) {
        // Calculate dynamic weights
        List<Double> weights = new ArrayList<>();
        double totalWeight = 0.0;

        for (Action action : available) {
            double weight = calculateWeight(steve, action);
            weights.add(weight);
            totalWeight += weight;
        }

        // Weighted random selection
        double rand = steve.getRandom().nextDouble() * totalWeight;
        double cumulative = 0.0;

        for (int i = 0; i < available.size(); i++) {
            cumulative += weights.get(i);
            if (rand <= cumulative) {
                return available.get(i);
            }
        }

        return available.get(available.size() - 1);
    }

    private double calculateWeight(SteveEntity steve, Action action) {
        double baseWeight = baseWeights.getOrDefault(action.getType(), 1.0);

        // Apply modifiers
        if (action.isUrgent()) baseWeight *= 3.0;
        if (steve.hasToolFor(action)) baseWeight *= 1.5;
        if (steve.getDistanceTo(action.getTarget()) < 10) baseWeight *= 2.0;
        if (steve.getInventory().isFull()) baseWeight *= 0.1;

        return baseWeight;
    }
}
```

**Advanced Variation: Softmax Selection**
```java
public class SoftmaxSelection {
    private static final double TEMPERATURE = 2.0;  // Controls randomness

    public Action selectAction(SteveEntity steve, List<Action> available) {
        // Calculate softmax probabilities
        double[] utilities = available.stream()
            .mapToDouble(a -> calculateUtility(steve, a))
            .toArray();

        double[] expUtilities = new double[utilities.length];
        double sumExp = 0.0;

        for (int i = 0; i < utilities.length; i++) {
            expUtilities[i] = Math.exp(utilities[i] / TEMPERATURE);
            sumExp += expUtilities[i];
        }

        // Random selection based on probabilities
        double rand = steve.getRandom().nextDouble() * sumExp;
        double cumulative = 0.0;

        for (int i = 0; i < available.size(); i++) {
            cumulative += expUtilities[i];
            if (rand <= cumulative) {
                return available.get(i);
            }
        }

        return available.get(0);
    }
}
```

### 1.3 Utility Scoring

**Description:** Each action is evaluated using a utility function that combines multiple factors into a single score. The action with the highest utility score is selected.

**When to Use:**
- Multiple competing factors influence decisions
- Smooth, nuanced behavior is desired
- Trade-offs between different goals are needed
- Avoiding brittle priority chains

**Pseudocode Implementation:**
```python
def calculate_utility(agent, action):
    """
    Combine multiple factors into utility score
    """
    score = 0.0

    # Factor 1: Importance (0-1)
    score += action.importance * 40

    # Factor 2: Urgency (0-1)
    score += action.urgency * 30

    # Factor 3: Feasibility (0-1)
    score += action.feasibility * 20

    # Factor 4: Resource availability (0-1)
    score += action.has_resources * 10

    return score

# Example: Minecraft building utility
def building_utility(agent, structure):
    utility = 0.0

    # Distance factor (closer is better)
    distance = agent.distance_to(structure.location)
    utility += max(0, 1 - distance / 100) * 30

    # Resource availability
    if agent.has_materials_for(structure):
        utility += 40
    else:
        utility -= 20  # Penalty for missing materials

    # Priority
    utility += structure.priority * 20

    # Time of day (build during day)
    if is_daytime():
        utility += 10

    return utility
```

**Minecraft Application:**
```java
public class UtilityScoringDecision {

    public Action selectAction(SteveEntity steve, List<Action> candidates) {
        Action bestAction = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Action action : candidates) {
            double score = calculateUtility(steve, action);
            if (score > bestScore) {
                bestScore = score;
                bestAction = action;
            }
        }

        return bestAction != null ? bestAction : new IdleAction(steve);
    }

    private double calculateUtility(SteveEntity steve, Action action) {
        double utility = 0.0;

        // Importance factor (0-100 scale)
        utility += action.getImportance() * 0.35;

        // Urgency factor (0-100 scale)
        utility += action.getUrgency() * 0.25;

        // Feasibility factor (0-100 scale)
        utility += calculateFeasibility(steve, action) * 0.25;

        // Resource availability (0-100 scale)
        utility += calculateResourceScore(steve, action) * 0.15;

        return utility;
    }

    private double calculateFeasibility(SteveEntity steve, Action action) {
        double score = 100.0;

        // Distance penalty
        double distance = steve.position().distanceTo(action.getTargetPosition());
        score -= distance * 0.5;

        // Tool requirement
        if (action.requiresTool() && !steve.hasRequiredTool(action)) {
            score -= 50;
        }

        // Pathfinding check
        if (!steve.canReach(action.getTargetPosition())) {
            score -= 100;  // Impossible
        }

        return max(0, score);
    }

    private double calculateResourceScore(SteveEntity steve, Action action) {
        if (!action.requiresResources()) {
            return 100.0;
        }

        int required = action.getRequiredResourceCount();
        int available = steve.getInventory().count(action.getRequiredResource());

        if (available >= required) {
            return 100.0;
        } else {
            return (available / (double) required) * 100.0;
        }
    }
}
```

**Utility Curve Patterns:**
```java
public class UtilityCurves {

    // Linear: y = x
    public static double linear(double x) {
        return x;
    }

    // Quadratic: y = x^2 (amplifies differences)
    public static double quadratic(double x) {
        return x * x;
    }

    // Sigmoid: Smooth S-curve
    public static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-10 * (x - 0.5)));
    }

    // Exponential decay: y = e^(-kx)
    public static double exponentialDecay(double x, double k) {
        return Math.exp(-k * x);
    }

    // Logistic: S-curve with parameters
    public static double logistic(double x, double midpoint, double steepness) {
        return 1.0 / (1.0 + Math.exp(-steepness * (x - midpoint)));
    }

    // Example: Health utility using curves
    public static double healthUtility(double currentHealth, double maxHealth) {
        double ratio = currentHealth / maxHealth;

        // Critical health: Very steep utility drop
        if (ratio < 0.2) {
            return sigmoid(ratio) * 100;  // High urgency
        }

        // Normal range: Linear concern
        return ratio * 50;
    }

    // Example: Distance utility
    public static double distanceUtility(double distance) {
        // Exponential decay: nearby actions much preferred
        return 100 * exponentialDecay(distance / 50.0, 3.0);
    }
}
```

### 1.4 Rule Chaining

**Description:** Complex decisions are made by chaining simple rules together. Each rule evaluates a condition and either produces a decision or passes to the next rule.

**When to Use:**
- Complex decision logic that needs decomposition
- Sequential refinement of decisions
- Hierarchical decision structures
- Combining multiple decision systems

**Pseudocode Implementation:**
```python
class Rule:
    def __init__(self, condition, action, next_rule=None):
        self.condition = condition
        self.action = action
        self.next_rule = next_rule

    def evaluate(self, agent, world):
        if self.condition(agent, world):
            return self.action
        elif self.next_rule:
            return self.next_rule.evaluate(agent, world)
        else:
            return None

# Build rule chain
survival_rule = Rule(
    condition=lambda a, w: a.health < 20,
    action="flee"
)

combat_rule = Rule(
    condition=lambda a, w: w.has_enemy(),
    action="combat",
    next_rule=survival_rule  # Chain
)

task_rule = Rule(
    condition=lambda a, w: a.has_task(),
    action="do_task",
    next_rule=combat_rule
)

# Usage
action = task_rule.evaluate(agent, world)
```

**Minecraft Application:**
```java
public interface DecisionRule {
    ActionDecision evaluate(SteveEntity steve, World world);
}

public class RuleChain implements DecisionRule {
    private final List<DecisionRule> rules = new ArrayList<>();

    public RuleChain addRule(DecisionRule rule) {
        rules.add(rule);
        return this;
    }

    @Override
    public ActionDecision evaluate(SteveEntity steve, World world) {
        for (DecisionRule rule : rules) {
            ActionDecision decision = rule.evaluate(steve, world);
            if (decision != null && decision.shouldExecute()) {
                return decision;
            }
        }
        return ActionDecision.idle();
    }
}

// Rule implementations
public class HealthEmergencyRule implements DecisionRule {
    @Override
    public ActionDecision evaluate(SteveEntity steve, World world) {
        if (steve.getHealth() < 6.0f) {
            BlockPos safeZone = findNearestSafeZone(steve);
            return ActionDecision.execute(new FleeAction(steve, safeZone))
                .withPriority(Priority.CRITICAL);
        }
        return null;
    }
}

public class HostileThreatRule implements DecisionRule {
    @Override
    public ActionDecision evaluate(SteveEntity steve, World world) {
        Entity hostile = findNearestHostile(steve, world, 20.0);
        if (hostile != null) {
            return ActionDecision.execute(new CombatAction(steve, hostile))
                .withPriority(CombatThreatRule.HIGH);
        }
        return null;
    }
}

public class TaskAssignmentRule implements DecisionRule {
    @Override
    public ActionDecision evaluate(SteveEntity steve, World world) {
        Task task = steve.getAssignedTask();
        if (task != null && !task.isComplete()) {
            return ActionDecision.execute(createActionForTask(task));
        }
        return null;
    }
}

// Build decision system
public class DecisionSystem {
    private final RuleChain ruleChain;

    public DecisionSystem() {
        ruleChain = new RuleChain()
            .addRule(new HealthEmergencyRule())
            .addRule(new HostileThreatRule())
            .addRule(new TaskAssignmentRule())
            .addRule(new ResourceOpportunityRule())
            .addRule(new IdleBehaviorRule());
    }

    public Action selectAction(SteveEntity steve, World world) {
        ActionDecision decision = ruleChain.evaluate(steve, world);
        return decision.getAction();
    }
}
```

### 1.5 Fallback Cascades

**Description:** Actions are attempted in order, with each fallback providing a less optimal but still acceptable alternative. Used for graceful degradation.

**When to Use:**
- Actions may fail or be unavailable
- Graceful degradation is required
- Primary options are resource-intensive
- Robustness is critical

**Pseudocode Implementation:**
```python
def fallback_execute(agent, world):
    """
    Try actions in order, falling back on failure
    """
    # Attempt 1: Optimal approach
    try:
        result = attempt_optimal(agent, world)
        if is_successful(result):
            return result
    except Exception:
        pass

    # Attempt 2: Less optimal but workable
    try:
        result = attempt_secondary(agent, world)
        if is_successful(result):
            return result
    except Exception:
        pass

    # Attempt 3: Degraded but functional
    try:
        result = attempt_degraded(agent, world)
        if is_successful(result):
            return result
    except Exception:
        pass

    # Attempt 4: Safe minimum
    return attempt_minimum(agent, world)

# Example: Mining block with tool fallback
def mine_block_with_fallbacks(agent, block):
    # Try best tool
    if agent.has_tool("diamond_pickaxe"):
        return mine_with(agent, block, "diamond_pickaxe")

    # Try lower tier tools
    for tool in ["iron_pickaxe", "stone_pickaxe", "wooden_pickaxe"]:
        if agent.has_tool(tool):
            return mine_with(agent, block, tool)

    # Try by hand (very slow)
    return mine_by_hand(agent, block)
```

**Minecraft Application:**
```java
public class FallbackCascade {

    public ActionResult executeAction(SteveEntity steve, BlockPos target) {
        // Attempt 1: Optimal tool and approach
        ActionResult result = tryOptimalApproach(steve, target);
        if (result.isSuccess()) {
            return result;
        }

        // Attempt 2: Alternative pathing
        result = tryAlternativePath(steve, target);
        if (result.isSuccess()) {
            return result;
        }

        // Attempt 3: Defer for later
        result = tryDeferAction(steve, target);
        if (result.isSuccess()) {
            return result;
        }

        // Attempt 4: Abandon with reason
        return ActionResult.failed("Unable to reach target after all attempts");
    }

    private ActionResult tryOptimalApproach(SteveEntity steve, BlockPos target) {
        // Check if optimal tool available
        ItemStack optimalTool = steve.getOptimalTool(target);
        if (optimalTool.isEmpty()) {
            return ActionResult.failed("No optimal tool");
        }

        // Check if path exists
        Path path = steve.getPathTo(target);
        if (path == null || path.isBlocked()) {
            return ActionResult.failed("No direct path");
        }

        // Execute
        return steve.executePathedAction(path, () -> breakBlock(steve, target, optimalTool));
    }

    private ActionResult tryAlternativePath(SteveEntity steve, BlockPos target) {
        // Try navigating around obstacles
        Path alternativePath = steve.findAlternativePath(target);
        if (alternativePath != null && alternativePath.reachesTarget()) {
            ItemStack anyTool = steve.getAnyViableTool(target);
            if (!anyTool.isEmpty()) {
                return steve.executePathedAction(alternativePath,
                    () -> breakBlock(steve, target, anyTool));
            }
        }

        // Try clearing obstacles
        ActionResult clearResult = tryClearObstacles(steve, target);
        if (clearResult.isSuccess()) {
            return tryOptimalApproach(steve, target);  // Retry optimal
        }

        return ActionResult.failed("No alternative path available");
    }

    private ActionResult tryDeferAction(SteveEntity steve, BlockPos target) {
        // Add to deferred task queue
        steve.getDeferredTasks().add(new DeferredTask(target, System.currentTimeMillis() + 60000));
        return ActionResult.deferred("Action deferred for 60 seconds");
    }
}
```

---

## 2. Memory Patterns

Memory patterns enable AI agents to retain information over time, learn from experience, and make decisions based on historical context.

### 2.1 Short-Term Memory (Recent Events)

**Description:** Temporary storage of recent events, typically lasting seconds to minutes. Used for immediate context and situational awareness.

**When to Use:**
- Tracking recent combat encounters
- Monitoring short-term resource changes
- Maintaining conversation context
- Detecting immediate patterns

**Pseudocode Implementation:**
```python
class ShortTermMemory:
    def __init__(self, capacity=100, duration_seconds=300):
        self.capacity = capacity
        self.duration = duration_seconds
        self.events = []  # List of (timestamp, event)

    def add(self, event):
        self.events.append((time.time(), event))
        self.cleanup()

    def cleanup(self):
        # Remove old events
        cutoff = time.time() - self.duration
        self.events = [(t, e) for t, e in self.events if t > cutoff]

        # Enforce capacity
        if len(self.events) > self.capacity:
            self.events = self.events[-self.capacity:]

    def get_recent(self, seconds=60):
        cutoff = time.time() - seconds
        return [e for t, e in self.events if t > cutoff]

    def count(self, event_type, seconds=60):
        recent = self.get_recent(seconds)
        return sum(1 for e in recent if e.type == event_type)
```

**Minecraft Application:**
```java
public class ShortTermMemory {
    private final int capacity;
    private final long durationMs;
    private final ConcurrentLinkedQueue<MemoryEvent> events;

    public ShortTermMemory(int capacity, long durationMs) {
        this.capacity = capacity;
        this.durationMs = durationMs;
        this.events = new ConcurrentLinkedQueue<>();
    }

    public void remember(MemoryEvent event) {
        events.add(event);
        cleanup();
    }

    private void cleanup() {
        long cutoff = System.currentTimeMillis() - durationMs;

        // Remove old events
        events.removeIf(e -> e.getTimestamp() < cutoff);

        // Enforce capacity
        while (events.size() > capacity) {
            events.poll();
        }
    }

    public List<MemoryEvent> getRecentEvents(long durationMs) {
        long cutoff = System.currentTimeMillis() - durationMs;
        return events.stream()
            .filter(e -> e.getTimestamp() > cutoff)
            .collect(Collectors.toList());
    }

    public int countEvents(MemoryEventType type, long durationMs) {
        return (int) getRecentEvents(durationMs).stream()
            .filter(e -> e.getType() == type)
            .count();
    }

    public Optional<MemoryEvent> findMostRecent(MemoryEventType type) {
        return events.stream()
            .filter(e -> e.getType() == type)
            .max(Comparator.comparingLong(MemoryEvent::getTimestamp));
    }

    public boolean hasRecentEvent(MemoryEventType type, long withinMs) {
        return findMostRecent(type)
            .map(e -> System.currentTimeMillis() - e.getTimestamp() < withinMs)
            .orElse(false);
    }
}

// Usage examples
public class CombatMemory {
    private final ShortTermMemory memory = new ShortTermMemory(500, 300000);  // 5 minutes

    public boolean shouldFlee(SteveEntity steve) {
        // Check if took damage recently
        if (memory.hasRecentEvent(MemoryEventType.DAMAGED, 5000)) {
            // Count recent hostile encounters
            int hostileCount = memory.countEvents(MemoryEventType.HOSTILE_SPOTTED, 30000);
            if (hostileCount >= 3) {
                return true;  // Too many hostiles
            }
        }
        return false;
    }

    public void recordDamage(SteveEntity steve, DamageSource source) {
        memory.remember(new MemoryEvent(
            MemoryEventType.DAMAGED,
            steve.position(),
            Map.of("source", source.getMsgId())
        ));
    }
}
```

### 2.2 Long-Term Memory (Learned Patterns)

**Description:** Persistent storage of important information, patterns, and learned behaviors. Survives across sessions and enables learning from experience.

**When to Use:**
- Remembering resource locations
- Learning player preferences
- Storing successful strategies
- Tracking long-term patterns

**Pseudocode Implementation:**
```python
class LongTermMemory:
    def __init__(self, storage_path):
        self.storage_path = storage_path
        self.data = self.load()

    def remember(self, key, value, importance=1.0):
        if key not in self.data or value.importance > self.data[key].importance:
            self.data[key] = value
            self.save()

    def recall(self, key):
        return self.data.get(key)

    def forget(self, key):
        if key in self.data:
            del self.data[key]
            self.save()

    def save(self):
        with open(self.storage_path, 'w') as f:
            json.dump(self.data, f)

    def load(self):
        if os.path.exists(self.storage_path):
            with open(self.storage_path, 'r') as f:
                return json.load(f)
        return {}
```

**Minecraft Application:**
```java
public class LongTermMemory {
    private final Map<String, MemoryEntry> memory;
    private final Path storagePath;

    public LongTermMemory(Path storagePath) {
        this.storagePath = storagePath;
        this.memory = load();
    }

    public void remember(String key, Object value, double importance) {
        MemoryEntry entry = new MemoryEntry(
            System.currentTimeMillis(),
            value,
            importance,
            0  // access count
        );

        // Only store if more important than existing
        MemoryEntry existing = memory.get(key);
        if (existing == null || importance > existing.importance) {
            memory.put(key, entry);
            save();
        }
    }

    public Optional<Object> recall(String key) {
        MemoryEntry entry = memory.get(key);
        if (entry != null) {
            entry.accessCount++;
            entry.lastAccessed = System.currentTimeMillis();
            return Optional.of(entry.value);
        }
        return Optional.empty();
    }

    public void forget(String key) {
        memory.remove(key);
        save();
    }

    private Map<String, MemoryEntry> load() {
        try {
            if (Files.exists(storagePath)) {
                String json = Files.readString(storagePath);
                return gson.fromJson(json, memoryType);
            }
        } catch (IOException e) {
            // Log error
        }
        return new ConcurrentHashMap<>();
    }

    private void save() {
        try {
            String json = gson.toJson(memory);
            Files.writeString(storagePath, json);
        } catch (IOException e) {
            // Log error
        }
    }
}

// Specific memory implementations
public class ResourceLocationMemory {
    private final LongTermMemory memory;

    public void rememberResourceLocation(BlockPos pos, Block resource, double quality) {
        String key = "resource:" + resource + ":" + pos.toShortString();
        Map<String, Object> data = Map.of(
            "position", pos,
            "resource", resource,
            "quality", quality,
            "discovered", System.currentTimeMillis()
        );
        memory.remember(key, data, quality);
    }

    public Optional<BlockPos> findNearestResource(Block resource, BlockPos center, double minQuality) {
        // Search memory for matching resources
        return memory.getAllKeys().stream()
            .filter(k -> k.startsWith("resource:" + resource + ":"))
            .map(k -> (Map<String, Object>) memory.recall(k).get())
            .filter(m -> (double) m.get("quality") >= minQuality)
            .map(m -> (BlockPos) m.get("position"))
            .min(Comparator.comparingDouble(p -> p.distSqr(center)));
    }
}

public class PlayerPreferenceMemory {
    private final LongTermMemory memory;

    public void recordPreference(String playerId, String preference, Object value) {
        String key = "preference:" + playerId + ":" + preference;
        memory.remember(key, value, 1.0);
    }

    public Optional<Object> getPreference(String playerId, String preference) {
        return memory.recall("preference:" + playerId + ":" + preference);
    }

    public void recordCommandSuccess(String playerId, String command, boolean success) {
        String key = "command_success:" + playerId + ":" + command;
        Double currentRate = (Double) memory.recall(key).orElse(0.5);
        double newRate = currentRate * 0.9 + (success ? 1.0 : 0.0) * 0.1;
        memory.remember(key, newRate, 1.0);
    }
}
```

### 2.3 Working Memory (Current Task)

**Description:** Temporary storage for information relevant to the current task or goal. Cleared when task changes or completes.

**When to Use:**
- Tracking multi-step task progress
- Maintaining intermediate calculation results
- Storing temporary navigation data
- Holding partial construction state

**Pseudocode Implementation:**
```python
class WorkingMemory:
    def __init__(self):
        self.data = {}
        self.current_task = None

    def set_task(self, task):
        self.current_task = task
        self.clear()

    def clear(self):
        self.data.clear()

    def set(self, key, value):
        self.data[key] = value

    def get(self, key, default=None):
        return self.data.get(key, default)

    def has(self, key):
        return key in self.data
```

**Minecraft Application:**
```java
public class WorkingMemory {
    private final Map<String, Object> data = new HashMap<>();
    private Task currentTask;

    public void setCurrentTask(Task task) {
        if (currentTask != null && !currentTask.equals(task)) {
            clear();  // Clear memory when task changes
        }
        this.currentTask = task;
    }

    public void clear() {
        data.clear();
    }

    public void set(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        return value != null && type.isInstance(value) ? (T) value : null;
    }

    public <T> T get(String key, Class<T> type, T defaultValue) {
        T value = get(key, type);
        return value != null ? value : defaultValue;
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }
}

// Usage in task execution
public class BuildTaskExecutor {
    private final WorkingMemory workingMemory = new WorkingMemory();

    public void executeBuildTask(SteveEntity steve, BuildTask task) {
        workingMemory.setCurrentTask(task);

        // Plan construction
        List<BlockPlacement> placements = planConstruction(task.getStructure());
        workingMemory.set("placements", placements);
        workingMemory.set("current_index", 0);

        // Execute
        while (workingMemory.get("current_index", Integer.class, 0) < placements.size()) {
            int index = workingMemory.get("current_index", Integer.class);
            BlockPlacement placement = placements.get(index);

            ActionResult result = placeBlock(steve, placement);
            if (result.isSuccess()) {
                workingMemory.set("current_index", index + 1);
            } else if (result.shouldRetry()) {
                // Retry same block
            } else {
                // Handle failure
                workingMemory.set("failed_at", index);
                break;
            }
        }
    }
}
```

### 2.4 Shared Memory (Group Knowledge)

**Description:** Memory accessible to multiple agents, enabling coordination and knowledge sharing. Can be implemented as a blackboard or shared data structure.

**When to Use:**
- Multi-agent coordination
- Distributed task completion
- Avoiding redundant work
- Collaborative problem solving

**Pseudocode Implementation:**
```python
class SharedMemory:
    def __init__(self):
        self.data = {}
        self.lock = threading.Lock()

    def write(self, agent_id, key, value):
        with self.lock:
            if key not in self.data or self.data[key]["timestamp"] < value["timestamp"]:
                self.data[key] = {
                    "value": value,
                    "agent": agent_id,
                    "timestamp": time.time()
                }

    def read(self, key):
        with self.lock:
            entry = self.data.get(key)
            return entry["value"] if entry else None

    def update(self, key, updater):
        with self.lock:
            if key in self.data:
                self.data[key]["value"] = updater(self.data[key]["value"])
```

**Minecraft Application:**
```java
public class SharedMemory {
    private final ConcurrentHashMap<String, SharedEntry> data = new ConcurrentHashMap<>();

    public void write(String agentId, String key, Object value) {
        SharedEntry newEntry = new SharedEntry(
            value,
            agentId,
            System.currentTimeMillis()
        );

        // Only update if newer
        data.compute(key, (k, existing) ->
            existing == null || newEntry.timestamp > existing.timestamp ? newEntry : existing
        );
    }

    public Optional<Object> read(String key) {
        SharedEntry entry = data.get(key);
        return Optional.ofNullable(entry).map(e -> e.value);
    }

    public <T> T read(String key, Class<T> type) {
        return read(key).filter(type::isInstance).map(type::cast).orElse(null);
    }

    public void update(String key, Function<Object, Object> updater) {
        data.computeIfPresent(key, (k, entry) -> {
            Object newValue = updater.apply(entry.value);
            return new SharedEntry(newValue, entry.agentId, System.currentTimeMillis());
        });
    }

    private static class SharedEntry {
        final Object value;
        final String agentId;
        final long timestamp;

        SharedEntry(Object value, String agentId, long timestamp) {
            this.value = value;
            this.agentId = agentId;
            this.timestamp = timestamp;
        }
    }
}

// Shared knowledge systems
public class ResourceClaimSystem {
    private final SharedMemory sharedMemory;

    public boolean claimResource(String agentId, BlockPos pos, Block block) {
        String key = "resource_claim:" + pos.toShortString();

        // Try to claim
        SharedEntry claim = new SharedEntry(
            Map.of("agent", agentId, "block", block),
            agentId,
            System.currentTimeMillis()
        );

        SharedEntry existing = sharedMemory.data.putIfAbsent(key, claim);
        return existing == null;
    }

    public void releaseResource(String agentId, BlockPos pos) {
        String key = "resource_claim:" + pos.toShortString();
        sharedMemory.data.computeIfPresent(key, (k, entry) ->
            entry.agentId.equals(agentId) ? null : entry
        );
    }

    public Optional<String> getClaimOwner(BlockPos pos) {
        String key = "resource_claim:" + pos.toShortString();
        return sharedMemory.read(key).map(v -> (Map<?, ?>) v)
            .map(m -> (String) m.get("agent"));
    }
}

public class SharedWorldKnowledge {
    private final SharedMemory sharedMemory;

    public void reportHostile(String agentId, BlockPos pos, EntityType type) {
        String key = "hostile:" + type + ":" + pos.toShortString();
        sharedMemory.write(agentId, key, Map.of(
            "position", pos,
            "type", type,
            "last_seen", System.currentTimeMillis()
        ));
    }

    public List<Map<String, Object>> getRecentHostiles(BlockPos center, double radius) {
        long cutoff = System.currentTimeMillis() - 30000;  // 30 seconds

        return sharedMemory.data.entrySet().stream()
            .filter(e -> e.getKey().startsWith("hostile:"))
            .map(e -> (Map<String, Object>) e.getValue().value)
            .filter(m -> (Long) m.get("last_seen") > cutoff)
            .filter(m -> ((BlockPos) m.get("position")).distSqr(center) < radius * radius)
            .collect(Collectors.toList());
    }
}
```

---

## 3. Coordination Patterns

Coordination patterns enable multiple AI agents to work together effectively, avoiding conflicts and achieving goals that require collaboration.

### 3.1 Leader-Follower

**Description:** One agent designated as leader makes decisions and issues commands to followers. Followers execute commands and report results.

**When to Use:**
- Clear hierarchy exists
- Centralized decision-making is beneficial
- Followers need direction
- Coordinated group action required

**Pseudocode Implementation:**
```python
class Leader:
    def __init__(self):
        self.followers = []
        self.tasks = []

    def add_follower(self, follower):
        self.followers.append(follower)

    def assign_tasks(self):
        for follower in self.followers:
            if follower.is_idle():
                task = self.get_next_task()
                if task:
                    follower.assign_task(task)

    def coordinate(self):
        # Assess situation
        situation = self.assess()

        # Make decisions
        decisions = self.make_decisions(situation)

        # Issue commands
        for follower, command in decisions.items():
            follower.execute(command)

class Follower:
    def __init__(self, leader):
        self.leader = leader
        self.current_task = None

    def execute(self, command):
        # Execute leader's command
        result = self.do_command(command)

        # Report back
        self.leader.report_result(self, command, result)
```

**Minecraft Application:**
```java
public class LeaderFollowerSystem {

    public static class ForemanAgent {
        private final List<WorkerAgent> workers = new ArrayList<>();
        private final Queue<Task> pendingTasks = new LinkedList<>();
        private final SharedMemory sharedMemory;

        public ForemanAgent(SharedMemory sharedMemory) {
            this.sharedMemory = sharedMemory;
        }

        public void addWorker(WorkerAgent worker) {
            workers.add(worker);
        }

        public void coordinate() {
            // Assess current situation
            SituationReport report = assessSituation();

            // Make high-level decisions
            List<Assignment> assignments = planAssignments(report);

            // Issue commands to workers
            for (Assignment assignment : assignments) {
                WorkerAgent worker = findBestWorker(assignment);
                if (worker != null) {
                    worker.assignTask(assignment.task);
                    sharedMemory.write("foreman", "assignment:" + worker.getId(),
                        Map.of("task", assignment.task, "priority", assignment.priority));
                }
            }
        }

        private SituationReport assessSituation() {
            SituationReport report = new SituationReport();

            // Check worker status
            for (WorkerAgent worker : workers) {
                WorkerStatus status = worker.getStatus();
                if (status.isIdle()) {
                    report.idleWorkers.add(worker);
                }
            }

            // Check world state
            report.availableResources = scanAvailableResources();
            report.threats = detectThreats();
            report.playerGoals = getPlayerGoals();

            return report;
        }

        private List<Assignment> planAssignments(SituationReport report) {
            List<Assignment> assignments = new ArrayList<>();

            // Priority 1: Critical tasks
            for (Task task : getCriticalTasks()) {
                assignments.add(new Assignment(task, Priority.CRITICAL));
            }

            // Priority 2: Worker efficiency
            for (WorkerAgent worker : report.idleWorkers) {
                Task bestTask = findBestTaskFor(worker, report);
                if (bestTask != null) {
                    assignments.add(new Assignment(bestTask, Priority.NORMAL));
                }
            }

            return assignments;
        }
    }

    public static class WorkerAgent {
        private final String id;
        private final SteveEntity entity;
        private Task currentTask;

        public WorkerAgent(String id, SteveEntity entity) {
            this.id = id;
            this.entity = entity;
        }

        public void assignTask(Task task) {
            this.currentTask = task;
        }

        public void tick() {
            if (currentTask != null) {
                ActionResult result = executeTask(currentTask);

                if (result.isComplete()) {
                    reportCompletion(currentTask, result);
                    currentTask = null;
                } else if (result.shouldRetry()) {
                    // Continue trying
                } else {
                    reportFailure(currentTask, result);
                    currentTask = null;
                }
            }
        }

        private ActionResult executeTask(Task task) {
            // Execute the task
            return switch (task.getType()) {
                case MINE -> executeMineTask((MineTask) task);
                case BUILD -> executeBuildTask((BuildTask) task);
                case GATHER -> executeGatherTask((GatherTask) task);
                default -> ActionResult.failed("Unknown task type");
            };
        }

        public WorkerStatus getStatus() {
            if (currentTask == null) {
                return WorkerStatus.idle();
            }
            return WorkerStatus.working(currentTask);
        }
    }
}
```

### 3.2 Consensus Voting

**Description:** Agents propose and vote on decisions. The decision with majority consensus is selected. Enables democratic decision-making.

**When to Use:**
- No clear leader exists
- Group agreement is important
- Distributed decision-making needed
- Avoiding single point of failure

**Pseudocode Implementation:**
```python
class ConsensusSystem:
    def __init__(self, agents):
        self.agents = agents
        self.proposals = {}

    def propose(self, agent, proposal):
        self.proposals[proposal.id] = {
            "proposal": proposal,
            "votes_for": [],
            "votes_against": [],
            "status": "pending"
        }

    def vote(self, agent, proposal_id, vote):
        proposal = self.proposals[proposal_id]
        if vote:
            proposal["votes_for"].append(agent)
        else:
            proposal["votes_against"].append(agent)

        # Check if consensus reached
        if len(proposal["votes_for"]) > len(self.agents) / 2:
            proposal["status"] = "accepted"
            return True
        return False
```

**Minecraft Application:**
```java
public class ConsensusSystem {
    private final List<Agent> agents;
    private final Map<String, Proposal> proposals = new ConcurrentHashMap<>();

    public ConsensusSystem(List<Agent> agents) {
        this.agents = agents;
    }

    public String propose(Agent proposer, String description, Runnable action) {
        String proposalId = UUID.randomUUID().toString();
        Proposal proposal = new Proposal(
            proposalId,
            proposer.getId(),
            description,
            action,
            System.currentTimeMillis()
        );
        proposals.put(proposalId, proposal);

        // Notify all agents to vote
        for (Agent agent : agents) {
            agent.requestVote(proposal);
        }

        return proposalId;
    }

    public VoteResult vote(String agentId, String proposalId, boolean vote) {
        Proposal proposal = proposals.get(proposalId);
        if (proposal == null || proposal.isClosed()) {
            return VoteResult.failed("Proposal not available");
        }

        proposal.recordVote(agentId, vote);

        // Check consensus
        int totalAgents = agents.size();
        int votesFor = proposal.getVotesForCount();
        int votesAgainst = proposal.getVotesAgainstCount();

        if (votesFor > totalAgents / 2) {
            proposal.close(ProposalStatus.ACCEPTED);
            executeProposal(proposal);
            return VoteResult.accepted();
        } else if (votesAgainst > totalAgents / 2) {
            proposal.close(ProposalStatus.REJECTED);
            return VoteResult.rejected();
        } else if (votesFor + votesAgainst >= totalAgents) {
            // Everyone voted, no majority
            proposal.close(ProposalStatus.REJECTED);
            return VoteResult.rejected();
        }

        return VoteResult.pending();
    }

    private void executeProposal(Proposal proposal) {
        proposal.action.run();
    }

    // Consensus on task priority
    public Task selectConsensusTask(List<Task> candidates) {
        // Each agent votes for preferred task
        Map<Task, Integer> votes = new HashMap<>();

        for (Agent agent : agents) {
            Task preference = agent.selectPreferredTask(candidates);
            votes.merge(preference, 1, Integer::sum);
        }

        // Return task with most votes
        return votes.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
}
```

### 3.3 Auction/Bidding

**Description:** Tasks are announced and agents bid for them. The agent with the best bid (lowest cost, highest capability) wins the task.

**When to Use:**
- Task allocation problem
- Agents have different capabilities
- Efficient resource utilization needed
- Fair distribution desired

**Pseudocode Implementation:**
```python
class AuctionSystem:
    def __init__(self):
        self.active_auctions = {}

    def announce_task(self, task, duration=30):
        auction_id = str(uuid.uuid4())
        self.active_auctions[auction_id] = {
            "task": task,
            "bids": [],
            "deadline": time.time() + duration,
            "status": "open"
        }
        return auction_id

    def submit_bid(self, agent, auction_id, bid):
        auction = self.active_auctions.get(auction_id)
        if not auction or auction["status"] != "open":
            return False

        auction["bids"].append({
            "agent": agent,
            "bid": bid,
            "time": time.time()
        })
        return True

    def close_auction(self, auction_id):
        auction = self.active_auctions[auction_id]
        auction["status"] = "closed"

        # Select best bid (lowest cost)
        best_bid = min(auction["bids"], key=lambda b: b["bid"].cost)

        return best_bid["agent"], best_bid["bid"]
```

**Minecraft Application:**
```java
public class AuctionSystem {
    private final ConcurrentHashMap<String, Auction> activeAuctions = new ConcurrentHashMap<>();

    public String announceTask(Task task, long durationMs) {
        String auctionId = UUID.randomUUID().toString();
        Auction auction = new Auction(
            auctionId,
            task,
            System.currentTimeMillis() + durationMs
        );
        activeAuctions.put(auctionId, auction);
        return auctionId;
    }

    public boolean submitBid(String agentId, String auctionId, Bid bid) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null || !auction.isOpen()) {
            return false;
        }

        auction.addBid(new AgentBid(agentId, bid));
        return true;
    }

    public Optional<AuctionResult> closeAuction(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) {
            return Optional.empty();
        }

        auction.close();

        // Select best bid
        AgentBid bestBid = auction.selectBestBid();
        if (bestBid != null) {
            activeAuctions.remove(auctionId);
            return Optional.of(new AuctionResult(bestBid.agentId, bestBid.bid, auction.getTask()));
        }

        return Optional.empty();
    }

    // Agent bidding logic
    public static class BiddingAgent {
        private final String id;
        private final SteveEntity entity;

        public Bid calculateBid(Task task) {
            // Estimate completion time
            double estimatedTime = estimateCompletionTime(task);

            // Calculate cost
            double cost = calculateCost(task, estimatedTime);

            // Check capability
            double capability = calculateCapability(task);

            return new Bid(cost, estimatedTime, capability);
        }

        private double calculateCost(Task task, double estimatedTime) {
            // Base cost from time
            double cost = estimatedTime;

            // Distance cost
            double distance = entity.position().distTo(task.getTargetPosition());
            cost += distance * 0.1;

            // Tool cost
            if (!entity.hasToolFor(task)) {
                cost += 100;  // Need to acquire tool
            }

            // Current load cost
            if (entity.getCurrentTask() != null) {
                cost += 50;  // Would need to interrupt
            }

            return cost;
        }

        private double calculateCapability(Task task) {
            double capability = 1.0;

            // Tool capability
            if (entity.hasOptimalTool(task)) {
                capability *= 2.0;
            } else if (entity.hasViableTool(task)) {
                capability *= 1.0;
            } else {
                capability *= 0.1;
            }

            // Distance factor
            double distance = entity.position().distTo(task.getTargetPosition());
            capability *= Math.max(0.1, 1.0 - distance / 100.0);

            // Experience factor
            capability *= (1.0 + entity.getExperienceIn(task.getType()) * 0.1);

            return capability;
        }
    }
}
```

### 3.4 Blackboard Sharing

**Description:** Shared data structure where agents post observations, discoveries, and intentions. Agents read from blackboard to inform their decisions.

**When to Use:**
- Decentralized information sharing
- Agents need situational awareness
- Collaborative problem solving
- Avoiding information silos

**Pseudocode Implementation:**
```python
class Blackboard:
    def __init__(self):
        self.facts = {}
        self.subscriptions = defaultdict(list)

    def post(self, agent, key, value, confidence=1.0):
        fact = {
            "value": value,
            "agent": agent,
            "timestamp": time.time(),
            "confidence": confidence
        }

        # Update fact
        if key not in self.facts or fact["confidence"] > self.facts[key]["confidence"]:
            self.facts[key] = fact

            # Notify subscribers
            for callback in self.subscriptions[key]:
                callback(key, fact)

    def read(self, key):
        return self.facts.get(key)

    def subscribe(self, key, callback):
        self.subscriptions[key].append(callback)
```

**Minecraft Application:**
```java
public class Blackboard {
    private final ConcurrentHashMap<String, Fact> facts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Consumer<Fact>>> subscriptions = new ConcurrentHashMap<>();

    public void post(String agentId, String key, Object value, double confidence) {
        Fact newFact = new Fact(value, agentId, System.currentTimeMillis(), confidence);

        // Update if more confident or newer
        facts.compute(key, (k, existing) ->
            existing == null || newFact.confidence > existing.confidence ? newFact : existing
        );

        // Notify subscribers
        List<Consumer<Fact>> subscribers = subscriptions.get(key);
        if (subscribers != null) {
            subscribers.forEach(sub -> sub.accept(newFact));
        }
    }

    public Optional<Fact> read(String key) {
        return Optional.ofNullable(facts.get(key));
    }

    public <T> Optional<T> read(String key, Class<T> type) {
        return read(key)
            .map(f -> f.value)
            .filter(type::isInstance)
            .map(type::cast);
    }

    public void subscribe(String key, Consumer<Fact> callback) {
        subscriptions.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(callback);
    }

    // Domain-specific blackboard areas
    public static class WorldKnowledgeBlackboard {
        private final Blackboard blackboard;

        public void reportOre(String agentId, BlockPos pos, OreType type, double quality) {
            String key = "ore:" + type + ":" + pos.toShortString();
            blackboard.post(agentId, key, new OreDiscovery(pos, type, quality), quality);
        }

        public List<OreDiscovery> findNearbyOre(BlockPos center, double radius, double minQuality) {
            return blackboard.facts.entrySet().stream()
                .filter(e -> e.getKey().startsWith("ore:"))
                .map(e -> (OreDiscovery) e.getValue().value)
                .filter(o -> o.position.distSqr(center) < radius * radius)
                .filter(o -> o.quality >= minQuality)
                .collect(Collectors.toList());
        }
    }

    public static class ThreatBlackboard {
        private final Blackboard blackboard;

        public void reportThreat(String agentId, BlockPos pos, EntityType type, double danger) {
            String key = "threat:" + pos.toShortString();
            blackboard.post(agentId, key, new ThreatInfo(pos, type, danger), danger);
        }

        public List<ThreatInfo> getActiveThreats() {
            long cutoff = System.currentTimeMillis() - 60000;  // 1 minute

            return blackboard.facts.values().stream()
                .filter(f -> f.timestamp > cutoff)
                .filter(f -> f.value instanceof ThreatInfo)
                .map(f -> (ThreatInfo) f.value)
                .collect(Collectors.toList());
        }
    }

    public static class TaskStatusBlackboard {
        private final Blackboard blackboard;

        public void updateStatus(String agentId, String taskId, TaskStatus status, double progress) {
            String key = "task_status:" + taskId;
            blackboard.post(agentId, key, new TaskStatusUpdate(taskId, status, progress), 1.0);
        }

        public Optional<TaskStatusUpdate> getTaskStatus(String taskId) {
            return blackboard.read("task_status:" + taskId, TaskStatusUpdate.class);
        }
    }
}
```

### 3.5 Role Assignment

**Description:** Agents are assigned specialized roles (builder, miner, defender, etc.) and focus on tasks within their role. Roles can be dynamic or static.

**When to Use:**
- Specialized capabilities exist
- Efficient division of labor needed
- Clear responsibilities beneficial
- Dynamic role switching possible

**Pseudocode Implementation:**
```python
class RoleSystem:
    def __init__(self):
        self.roles = {}
        self.assignments = {}

    def define_role(self, name, capabilities):
        self.roles[name] = {
            "capabilities": capabilities,
            "agents": []
        }

    def assign_role(self, agent, role):
        if role not in self.roles:
            return False

        # Remove from old role
        if agent in self.assignments:
            old_role = self.assignments[agent]
            self.roles[old_role]["agents"].remove(agent)

        # Assign to new role
        self.assignments[agent] = role
        self.roles[role]["agents"].append(agent)
        return True

    def get_role_for_task(self, task):
        best_role = None
        best_score = 0

        for role_name, role in self.roles.items():
            score = self.calculate_role_score(role, task)
            if score > best_score:
                best_score = score
                best_role = role_name

        return best_role
```

**Minecraft Application:**
```java
public class RoleAssignmentSystem {
    private final Map<String, Role> roles = new HashMap<>();
    private final Map<String, String> agentRoles = new ConcurrentHashMap<>();

    public void defineRole(String roleName, RoleDefinition definition) {
        roles.put(roleName, new Role(roleName, definition));
    }

    public boolean assignRole(String agentId, String roleName) {
        Role role = roles.get(roleName);
        if (role == null) {
            return false;
        }

        // Remove from old role
        String oldRole = agentRoles.put(agentId, roleName);
        if (oldRole != null) {
            roles.get(oldRole).removeAgent(agentId);
        }

        // Add to new role
        role.addAgent(agentId);
        return true;
    }

    public String selectBestRoleFor(Task task) {
        return roles.entrySet().stream()
            .max(Comparator.comparingDouble(e -> e.getValue().calculateSuitability(task)))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    public Agent selectBestAgentFor(Task task) {
        String bestRole = selectBestRoleFor(task);
        if (bestRole == null) {
            return null;
        }

        return roles.get(bestRole).selectBestAgent(task);
    }
}

public class Role {
    private final String name;
    private final RoleDefinition definition;
    private final Set<String> agents = ConcurrentHashMap.newKeySet();

    public Role(String name, RoleDefinition definition) {
        this.name = name;
        this.definition = definition;
    }

    public double calculateSuitability(Task task) {
        double score = 0.0;

        // Check if role handles this task type
        if (definition.capabilities.contains(task.getType())) {
            score += 50;
        }

        // Check tool availability
        if (definition.requiredTools.stream().allMatch(task::hasToolAvailable)) {
            score += 30;
        }

        // Check agent availability
        int availableAgents = getAvailableAgentCount();
        score += Math.min(20, availableAgents * 5);

        return score;
    }

    public Agent selectBestAgent(Task task) {
        return agents.stream()
            .filter(id -> getAgent(id).isAvailable())
            .max(Comparator.comparingDouble(agent ->
                calculateAgentSuitability(agent, task)))
            .map(id -> getAgent(id))
            .orElse(null);
    }
}

// Predefined roles
public class StandardRoles {
    public static void defineStandardRoles(RoleAssignmentSystem system) {
        // Miner role
        system.defineRole("miner", new RoleDefinition(
            Set.of(TaskType.MINE, TaskType.GATHER),
            Set.of("pickaxe"),
            Set.of("see_ores", "dig_down"),
            1.0  // Base mining efficiency
        ));

        // Builder role
        system.defineRole("builder", new RoleDefinition(
            Set.of(TaskType.BUILD, TaskType.PLACE),
            Set.of("blocks"),
            Set.of("place_blocks", "read_blueprints"),
            1.0
        ));

        // Defender role
        system.defineRole("defender", new RoleDefinition(
            Set.of(TaskType.COMBAT, TaskType.PATROL),
            Set.of("weapon", "armor"),
            Set.of("detect_threats", "melee_combat"),
            1.0
        ));

        // Courier role
        system.defineRole("courier", new RoleDefinition(
            Set.of(TaskType.TRANSPORT, TaskType.DELIVER),
            Set.of(),
            Set.of("carry_items", "find_path"),
            1.0
        ));
    }
}
```

---

## 4. Learning Without ML

Learning patterns enable AI agents to improve their behavior over time without requiring machine learning algorithms. These patterns use simple counting, adaptation, and pattern extraction.

### 4.1 Success/Failure Counting

**Description:** Track success and failure rates for different actions, strategies, or locations. Use statistics to inform future decisions.

**When to Use:**
- Identifying effective strategies
- Avoiding repeated failures
- Learning from experience
- Simple performance optimization

**Pseudocode Implementation:**
```python
class SuccessFailureCounter:
    def __init__(self):
        self.stats = defaultdict(lambda: {"success": 0, "failure": 0, "attempts": 0})

    def record_attempt(self, key, success):
        self.stats[key]["attempts"] += 1
        if success:
            self.stats[key]["success"] += 1
        else:
            self.stats[key]["failure"] += 1

    def get_success_rate(self, key):
        stats = self.stats.get(key, {})
        if stats["attempts"] == 0:
            return 0.0
        return stats["success"] / stats["attempts"]

    def get_confidence(self, key, min_attempts=10):
        stats = self.stats.get(key, {})
        if stats["attempts"] < min_attempts:
            return 0.0
        return self.get_success_rate(key)

    def should_attempt(self, key, threshold=0.5):
        return self.get_confidence(key) >= threshold
```

**Minecraft Application:**
```java
public class SuccessFailureTracker {
    private final Map<String, ActionStatistics> stats = new ConcurrentHashMap<>();

    public void recordAttempt(String actionKey, boolean success, long durationMs) {
        stats.compute(actionKey, (key, existing) -> {
            ActionStatistics s = existing != null ? existing : new ActionStatistics();
            s.attempts++;
            if (success) {
                s.successes++;
            } else {
                s.failures++;
            }
            s.totalDurationMs += durationMs;
            s.lastAttempt = System.currentTimeMillis();
            return s;
        });
    }

    public double getSuccessRate(String actionKey) {
        ActionStatistics s = stats.get(actionKey);
        if (s == null || s.attempts == 0) {
            return 0.5;  // Unknown = neutral
        }
        return (double) s.successes / s.attempts;
    }

    public double getConfidence(String actionKey, int minAttempts) {
        ActionStatistics s = stats.get(actionKey);
        if (s == null || s.attempts < minAttempts) {
            return 0.5;  // Not enough data
        }
        return getSuccessRate(actionKey);
    }

    public double getAverageDuration(String actionKey) {
        ActionStatistics s = stats.get(actionKey);
        if (s == null || s.successes == 0) {
            return Double.MAX_VALUE;
        }
        return (double) s.totalDurationMs / s.successes;
    }

    public boolean shouldAttempt(String actionKey, double minSuccessRate, int minAttempts) {
        return getConfidence(actionKey, minAttempts) >= minSuccessRate;
    }

    private static class ActionStatistics {
        int attempts = 0;
        int successes = 0;
        int failures = 0;
        long totalDurationMs = 0;
        long lastAttempt = 0;
    }
}

// Usage in decision making
public class AdaptiveDecisionMaker {
    private final SuccessFailureTracker tracker = new SuccessFailureTracker();

    public Action selectMiningAction(SteveEntity steve, BlockPos target) {
        String actionKey = "mine:" + target.getX() + "," + target.getY() + "," + target.getZ();

        // Check if we've tried this before
        if (tracker.shouldAttempt(actionKey, 0.3, 3)) {
            // Good success rate, proceed
            return new MineAction(steve, target);
        }

        // Low success rate, try alternative approach
        if (steve.hasAlternativePath(target)) {
            return new PathedMineAction(steve, target);
        }

        // No alternative, might still fail
        return new MineAction(steve, target);
    }

    public void recordActionComplete(Action action, boolean success, long duration) {
        String key = generateKey(action);
        tracker.recordAttempt(key, success, duration);
    }
}
```

### 4.2 Adaptive Thresholds

**Description:** Adjust decision thresholds based on recent performance. If success rate is high, become more aggressive. If low, become more conservative.

**When to Use:**
- Dynamic difficulty adjustment
- Risk management based on performance
- Balancing exploration vs exploitation
- Adapting to changing conditions

**Pseudocode Implementation:**
```python
class AdaptiveThreshold:
    def __init__(self, initial=0.5, min_val=0.1, max_val=0.9, adaptation_rate=0.1):
        self.threshold = initial
        self.min_val = min_val
        self.max_val = max_val
        self.adaptation_rate = adaptation_rate
        self.recent_results = []

    def record_result(self, success):
        self.recent_results.append(success)
        if len(self.recent_results) > 10:
            self.recent_results.pop(0)

        # Adapt threshold based on recent performance
        if len(self.recent_results) >= 5:
            success_rate = sum(self.recent_results) / len(self.recent_results)

            if success_rate > 0.7:
                # Doing well, be more aggressive
                self.threshold = min(self.max_val, self.threshold + self.adaptation_rate)
            elif success_rate < 0.3:
                # Struggling, be more conservative
                self.threshold = max(self.min_val, self.threshold - self.adaptation_rate)

    def should_act(self, confidence):
        return confidence >= self.threshold
```

**Minecraft Application:**
```java
public class AdaptiveThresholdSystem {
    private final Map<String, AdaptiveThreshold> thresholds = new ConcurrentHashMap<>();

    public AdaptiveThreshold getThreshold(String key) {
        return thresholds.computeIfAbsent(key,
            k -> new AdaptiveThreshold(0.5, 0.1, 0.9, 0.05));
    }

    public void recordResult(String key, boolean success) {
        AdaptiveThreshold threshold = getThreshold(key);
        threshold.recordResult(success);
    }

    public boolean shouldAct(String key, double confidence) {
        AdaptiveThreshold threshold = getThreshold(key);
        return confidence >= threshold.getValue();
    }

    public static class AdaptiveThreshold {
        private double value;
        private final double minValue;
        private final double maxValue;
        private final double adaptationRate;
        private final Queue<Boolean> recentResults = new LinkedList<>();
        private static final int WINDOW_SIZE = 10;

        public AdaptiveThreshold(double initial, double min, double max, double rate) {
            this.value = initial;
            this.minValue = min;
            this.maxValue = max;
            this.adaptationRate = rate;
        }

        public void recordResult(boolean success) {
            recentResults.add(success);
            if (recentResults.size() > WINDOW_SIZE) {
                recentResults.poll();
            }

            if (recentResults.size() >= 5) {
                adapt();
            }
        }

        private void adapt() {
            long successes = recentResults.stream().filter(b -> b).count();
            double successRate = (double) successes / recentResults.size();

            if (successRate > 0.7) {
                // High success, be more aggressive (lower threshold)
                value = Math.max(minValue, value - adaptationRate);
            } else if (successRate < 0.3) {
                // Low success, be more conservative (higher threshold)
                value = Math.min(maxValue, value + adaptationRate);
            }
        }

        public double getValue() {
            return value;
        }
    }
}

// Usage example
public class CombatDecisionSystem {
    private final AdaptiveThresholdSystem thresholds = new AdaptiveThresholdSystem();

    public boolean shouldEngage(SteveEntity steve, Entity hostile) {
        double threatLevel = calculateThreatLevel(hostile);
        double confidence = calculateCombatConfidence(steve, hostile);

        String key = "combat:" + hostile.getType().toString();

        // Check adaptive threshold
        return thresholds.shouldAct(key, confidence);
    }

    public void recordCombatResult(Entity hostileType, boolean success) {
        String key = "combat:" + hostileType.toString();
        thresholds.recordResult(key, success);
    }

    private double calculateCombatConfidence(SteveEntity steve, Entity hostile) {
        double confidence = 0.5;

        // Health advantage
        double healthRatio = steve.getHealth() / hostile.getHealth();
        confidence += (healthRatio - 1.0) * 0.2;

        // Equipment advantage
        if (steve.hasWeapon() && !hostileIsArmed(hostile)) {
            confidence += 0.2;
        }

        // Number advantage
        if (countNearbyAllies(steve) > countNearbyEnemies(hostile)) {
            confidence += 0.1;
        }

        return Math.max(0.0, Math.min(1.0, confidence));
    }
}
```

### 4.3 Pattern Extraction

**Description:** Identify recurring patterns in behavior or environment. Extract and store these patterns for future recognition and response.

**When to Use:**
- Recognizing situations
- Predicting events
- Optimizing common scenarios
- Building situational awareness

**Pseudocode Implementation:**
```python
class PatternExtractor:
    def __init__(self, min_support=3):
        self.patterns = {}
        self.min_support = min_support

    def observe(self, events):
        # Extract frequent sequences
        sequences = self.extract_sequences(events)

        for seq, count in sequences.items():
            if count >= self.min_support:
                self.patterns[seq] = count

    def extract_sequences(self, events):
        sequences = defaultdict(int)

        # Find all subsequences of length 2-4
        for length in range(2, 5):
            for i in range(len(events) - length + 1):
                seq = tuple(events[i:i+length])
                sequences[seq] += 1

        return sequences

    def predict_next(self, context):
        # Find patterns matching context
        matches = [p for p in self.patterns.keys() if self.matches(context, p)]

        if matches:
            # Return most common continuation
            continuations = [p[len(context):] for p in matches]
            return max(set(continuations), key=continuations.count)

        return None
```

**Minecraft Application:**
```java
public class PatternExtractor {
    private final Map<SequenceKey, Integer> patterns = new ConcurrentHashMap<>();
    private final int minSupport;

    public PatternExtractor(int minSupport) {
        this.minSupport = minSupport;
    }

    public void observe(List<GameEvent> events) {
        // Extract sequences of 2-4 events
        for (int length = 2; length <= 4; length++) {
            for (int i = 0; i <= events.size() - length; i++) {
                List<GameEvent> sequence = events.subList(i, i + length);
                SequenceKey key = new SequenceKey(sequence);
                patterns.merge(key, 1, Integer::sum);
            }
        }
    }

    public Optional<GameEvent> predictNext(List<GameEvent> context) {
        // Find patterns that start with context
        Map<GameEvent, Integer> continuations = new HashMap<>();

        for (Map.Entry<SequenceKey, Integer> entry : patterns.entrySet()) {
            SequenceKey pattern = entry.getKey();
            if (pattern.startsWith(context)) {
                GameEvent continuation = pattern.getEvent(context.size());
                continuations.merge(continuation, entry.getValue(), Integer::sum);
            }
        }

        // Return most common continuation
        return continuations.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey);
    }

    public double getPatternProbability(List<GameEvent> pattern) {
        SequenceKey key = new SequenceKey(pattern);
        Integer count = patterns.get(key);
        if (count == null || count < minSupport) {
            return 0.0;
        }

        // Calculate probability based on prefix
        if (pattern.size() > 1) {
            List<GameEvent> prefix = pattern.subList(0, pattern.size() - 1);
            SequenceKey prefixKey = new SequenceKey(prefix);
            Integer prefixCount = patterns.get(prefixKey);
            if (prefixCount != null) {
                return (double) count / prefixCount;
            }
        }

        return 1.0;
    }
}

// Specific pattern extractors
public class ResourcePatternExtractor {
    private final PatternExtractor extractor;

    public void observeMining(SteveEntity steve, BlockPos blockPos, Block block) {
        GameEvent event = new GameEvent("mine", blockPos, block);

        // Get recent context
        List<GameEvent> context = getRecentEvents(steve, 10);
        context.add(event);

        extractor.observe(context);
    }

    public Optional<BlockPos> predictNextOre(SteveEntity steve) {
        List<GameEvent> context = getRecentEvents(steve, 3);

        return extractor.predictNext(context)
            .filter(e -> "mine".equals(e.getType()))
            .map(GameEvent::getPosition);
    }
}

public class CavePatternExtractor {
    private final PatternExtractor extractor;

    public void observeCaveFeature(SteveEntity steve, CaveFeature feature) {
        GameEvent event = new GameEvent("cave_feature", steve.position(), feature);
        List<GameEvent> context = getRecentCaveEvents(steve, 5);
        context.add(event);
        extractor.observe(context);
    }

    public CaveType predictCaveType(SteveEntity steve) {
        List<GameEvent> context = getRecentCaveEvents(steve, 2);

        return extractor.predictNext(context)
            .filter(e -> "cave_feature".equals(e.getType()))
            .map(e -> (CaveFeature) e.getData())
            .map(CaveFeature::getType)
            .orElse(CaveType.UNKNOWN);
    }
}
```

### 4.4 Experience-Weighted Averaging

**Description:** Combine multiple sources of information or strategies, weighting them by their past performance. Better-performing sources get more influence.

**When to Use:**
- Combining multiple decision systems
- Weighting expert opinions
- Blending strategies
- Ensemble decision making

**Pseudocode Implementation:**
```python
class ExperienceWeightedAverage:
    def __init__(self):
        self.weights = {}
        self.performance = {}

    def add_source(self, name, initial_weight=1.0):
        self.weights[name] = initial_weight
        self.performance[name] = []

    def record_performance(self, source, outcome):
        # Outcome: 0.0 (bad) to 1.0 (good)
        self.performance[source].append(outcome)

        # Keep only recent performance
        if len(self.performance[source]) > 20:
            self.performance[source].pop(0)

        # Update weight based on average performance
        avg_perf = sum(self.performance[source]) / len(self.performance[source])
        self.weights[source] = avg_perf

    def get_weighted_decision(self, decisions):
        """
        decisions: {source: decision_value}
        Returns: weighted average
        """
        weighted_sum = 0.0
        total_weight = 0.0

        for source, value in decisions.items():
            weight = self.weights.get(source, 1.0)
            weighted_sum += weight * value
            total_weight += weight

        return weighted_sum / total_weight if total_weight > 0 else 0.0

    def select_best_source(self, decisions_with_confidence):
        """
        Select source with highest weighted confidence
        """
        best_source = None
        best_score = -1

        for source, confidence in decisions_with_confidence.items():
            score = confidence * self.weights.get(source, 1.0)
            if score > best_score:
                best_score = score
                best_source = source

        return best_source
```

**Minecraft Application:**
```java
public class ExperienceWeightedDecisionSystem {
    private final Map<String, DecisionSource> sources = new ConcurrentHashMap<>();
    private final Map<String, PerformanceTracker> performance = new ConcurrentHashMap<>();

    public void addSource(String name, DecisionSource source, double initialWeight) {
        sources.put(name, source);
        performance.put(name, new PerformanceTracker(initialWeight));
    }

    public Action makeDecision(SteveEntity steve, World world) {
        // Get decisions from all sources
        Map<String, DecisionWithConfidence> decisions = new HashMap<>();

        for (Map.Entry<String, DecisionSource> entry : sources.entrySet()) {
            String sourceName = entry.getKey();
            DecisionSource source = entry.getValue();

            DecisionWithConfidence decision = source.decide(steve, world);
            decisions.put(sourceName, decision);
        }

        // Select best source based on weighted confidence
        return selectBestWeightedDecision(decisions);
    }

    private Action selectBestWeightedDecision(Map<String, DecisionWithConfidence> decisions) {
        String bestSource = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Map.Entry<String, DecisionWithConfidence> entry : decisions.entrySet()) {
            String sourceName = entry.getKey();
            DecisionWithConfidence decision = entry.getValue();

            double weight = performance.get(sourceName).getWeight();
            double score = decision.confidence * weight;

            if (score > bestScore) {
                bestScore = score;
                bestSource = sourceName;
            }
        }

        return decisions.get(bestSource).action;
    }

    public void recordOutcome(String sourceName, double outcome) {
        PerformanceTracker tracker = performance.get(sourceName);
        if (tracker != null) {
            tracker.recordPerformance(outcome);
        }
    }

    private static class PerformanceTracker {
        private final Queue<Double> recentPerformance = new LinkedList<>();
        private double weight;
        private static final int WINDOW_SIZE = 20;

        public PerformanceTracker(double initialWeight) {
            this.weight = initialWeight;
        }

        public void recordPerformance(double outcome) {
            recentPerformance.add(outcome);
            if (recentPerformance.size() > WINDOW_SIZE) {
                recentPerformance.poll();
            }

            // Update weight based on average performance
            double avgPerformance = recentPerformance.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.5);

            this.weight = avgPerformance;
        }

        public double getWeight() {
            return weight;
        }
    }
}

// Example decision sources
public class PathfindingSource implements DecisionSource {
    @Override
    public DecisionWithConfidence decide(SteveEntity steve, World world) {
        BlockPos target = steve.getCurrentTarget();
        Path path = steve.findPath(target);

        if (path != null && path.reachesTarget()) {
            double confidence = 1.0 / (1.0 + path.getLength() * 0.01);
            return new DecisionWithConfidence(new FollowPathAction(steve, path), confidence);
        }

        return new DecisionWithConfidence(null, 0.0);
    }
}

public class CombatSource implements DecisionSource {
    @Override
    public DecisionWithConfidence decide(SteveEntity steve, World world) {
        Entity hostile = findNearestHostile(steve, world, 20.0);

        if (hostile != null) {
            double confidence = calculateCombatAdvantage(steve, hostile);
            return new DecisionWithConfidence(new CombatAction(steve, hostile), confidence);
        }

        return new DecisionWithConfidence(null, 0.0);
    }
}
```

---

## 5. Reactive Patterns

Reactive patterns enable AI agents to respond immediately to events and changes in the environment. These patterns are essential for dynamic, responsive behavior.

### 5.1 Event-Triggered Responses

**Description:** Actions are triggered by specific events in the game world. When an event occurs, the agent evaluates whether to respond and how.

**When to Use:**
- Immediate response required
- Event-driven behavior
- Reactive AI design
- Interrupt-based actions

**Pseudocode Implementation:**
```python
class EventTriggeredSystem:
    def __init__(self):
        self.triggers = {}

    def register_trigger(self, event_type, handler):
        if event_type not in self.triggers:
            self.triggers[event_type] = []
        self.triggers[event_type].append(handler)

    def on_event(self, event):
        handlers = self.triggers.get(event.type, [])
        for handler in handlers:
            if handler.should_respond(event):
                handler.execute(event)

    def tick(self, agent):
        # Process pending events
        for event in agent.get_pending_events():
            self.on_event(event)
```

**Minecraft Application:**
```java
public class EventTriggeredSystem {
    private final Map<EventType, List<TriggerHandler>> triggers = new ConcurrentHashMap<>();

    public void registerTrigger(EventType eventType, TriggerHandler handler) {
        triggers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public void onEvent(GameEvent event, SteveEntity steve) {
        List<TriggerHandler> handlers = triggers.get(event.getType());
        if (handlers != null) {
            for (TriggerHandler handler : handlers) {
                if (handler.shouldRespond(event, steve)) {
                    handler.execute(event, steve);
                }
            }
        }
    }
}

// Example triggers
public class HostileSpottedTrigger implements TriggerHandler {
    @Override
    public boolean shouldRespond(GameEvent event, SteveEntity steve) {
        return event.getType() == EventType.HOSTILE_SPOTTED
            && steve.getHealth() > 10.0f;  // Only if healthy enough
    }

    @Override
    public void execute(GameEvent event, SteveEntity steve) {
        Entity hostile = event.getEntity();

        // Evaluate response based on threat
        if (isHighThreat(hostile)) {
            steve.setCurrentTask(new CombatTask(hostile, Priority.HIGH));
        } else {
            steve.getCurrentTask().addInterrupt(new HostileInterrupt(hostile));
        }
    }
}

public class BlockBrokenTrigger implements TriggerHandler {
    @Override
    public boolean shouldRespond(GameEvent event, SteveEntity steve) {
        return event.getType() == EventType.BLOCK_BROKEN
            && event.get_block().is_valuable();  // Only valuable blocks
    }

    @Override
    public void execute(GameEvent event, SteveEntity steve) {
        BlockPos pos = event.getPosition();

        // Add to mining queue
        steve.getTaskQueue().add(new MiningTask(pos, Priority.NORMAL));
    }
}

public class HealthLowTrigger implements TriggerHandler {
    @Override
    public boolean shouldRespond(GameEvent event, SteveEntity steve) {
        return event.getType() == EventType.HEALTH_CHANGED
            && steve.getHealth() < 6.0f;  // Below 3 hearts
    }

    @Override
    public void execute(GameEvent event, SteveEntity steve) {
        // Emergency response
        steve.clearAllTasks();
        steve.setCurrentTask(new FleeTask(findSafeLocation(steve), Priority.CRITICAL));

        // Notify allies
        for (Entity ally : steve.getNearbyAllies(20.0)) {
            ally.sendSignal(new HelpRequestSignal(steve));
        }
    }
}
```

### 5.2 Cooldown Systems

**Description:** Prevent spam or rapid repetition of actions by enforcing cooldown periods between uses.

**When to Use:**
- Preventing action spam
- Resource management
- Balancing game mechanics
- Avoiding repetitive behavior

**Pseudocode Implementation:**
```python
class CooldownSystem:
    def __init__(self):
        self.cooldowns = {}

    def set_cooldown(self, action, duration):
        self.cooldowns[action] = time.time() + duration

    def is_ready(self, action):
        if action not in self.cooldowns:
            return True
        return time.time() >= self.cooldowns[action]

    def get_remaining(self, action):
        if action not in self.cooldowns:
            return 0
        return max(0, self.cooldowns[action] - time.time())

    def can_use(self, action):
        return self.is_ready(action)

    def use(self, action, duration):
        if self.can_use(action):
            self.set_cooldown(action, duration)
            return True
        return False
```

**Minecraft Application:**
```java
public class CooldownSystem {
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<String, Long> durations = new ConcurrentHashMap<>();

    public void setCooldown(String actionId, long durationMs) {
        durations.put(actionId, durationMs);
        cooldowns.put(actionId, System.currentTimeMillis() + durationMs);
    }

    public boolean isReady(String actionId) {
        Long cooldownEnd = cooldowns.get(actionId);
        return cooldownEnd == null || System.currentTimeMillis() >= cooldownEnd;
    }

    public long getRemainingMs(String actionId) {
        Long cooldownEnd = cooldowns.get(actionId);
        if (cooldownEnd == null) {
            return 0;
        }
        return Math.max(0, cooldownEnd - System.currentTimeMillis());
    }

    public boolean tryUse(String actionId) {
        if (isReady(actionId)) {
            Long duration = durations.get(actionId);
            if (duration != null) {
                setCooldown(actionId, duration);
            }
            return true;
        }
        return false;
    }

    public float getProgress(String actionId) {
        Long duration = durations.get(actionId);
        Long remaining = getRemainingMs(actionId);

        if (duration == null || duration == 0) {
            return 1.0f;
        }

        return 1.0f - (remaining / (float) duration);
    }
}

// Usage examples
public class ActionCooldowns {
    private final CooldownSystem cooldowns = new CooldownSystem();

    public ActionCooldowns() {
        // Define cooldowns
        cooldowns.setCooldown("combat.attack", 500);      // 0.5 seconds
        cooldowns.setCooldown("combat.block", 200);       // 0.2 seconds
        cooldowns.setCooldown("movement.dash", 3000);     // 3 seconds
        cooldowns.setCooldown("utility.place_torch", 1000); // 1 second
    }

    public boolean canAttack() {
        return cooldowns.isReady("combat.attack");
    }

    public boolean tryAttack(SteveEntity steve, Entity target) {
        if (cooldowns.tryUse("combat.attack")) {
            steve.attack(target);
            return true;
        }
        return false;
    }

    public boolean canDash() {
        return cooldowns.isReady("movement.dash");
    }

    public boolean tryDash(SteveEntity steve, Vec3 direction) {
        if (cooldowns.tryUse("movement.dash")) {
            steve.applyVelocity(direction.scale(2.0));
            return true;
        }
        return false;
    }
}
```

### 5.3 Interrupt Handling

**Description:** Allow important events to interrupt current actions. Manage interrupt priority and resumption of interrupted tasks.

**When to Use:**
- Emergency responses
- Priority-based actions
- Reactive behavior
- Handling unexpected events

**Pseudocode Implementation:**
```python
class InterruptSystem:
    def __init__(self):
        self.interrupts = []
        self.current_task = None
        self.interrupted_task = None

    def can_interrupt(self, interrupt, task):
        return interrupt.priority > task.priority

    def handle_interrupt(self, interrupt):
        if self.current_task and self.can_interrupt(interrupt, self.current_task):
            # Save current task
            self.interrupted_task = self.current_task
            self.current_task = interrupt.task
            return True
        return False

    def complete_interrupt(self):
        if self.interrupted_task:
            # Resume interrupted task
            self.current_task = self.interrupted_task
            self.interrupted_task = None
        else:
            self.current_task = None
```

**Minecraft Application:**
```java
public class InterruptSystem {
    private final PriorityQueue<Interrupt> interruptQueue = new PriorityQueue<>(
        Comparator.comparingInt(Interrupt::getPriority).reversed()
    );
    private Task currentTask;
    private Task interruptedTask;

    public boolean tryInterrupt(Interrupt interrupt) {
        if (canInterrupt(interrupt)) {
            if (currentTask != null && currentTask.isInterruptible()) {
                interruptedTask = currentTask;
                interruptedTask.interrupt();
            }

            executeInterrupt(interrupt);
            return true;
        }
        return false;
    }

    private boolean canInterrupt(Interrupt interrupt) {
        if (currentTask == null) {
            return true;
        }

        // Higher priority can interrupt
        return interrupt.getPriority() > currentTask.getPriority();
    }

    private void executeInterrupt(Interrupt interrupt) {
        currentTask = interrupt.getTask();
        currentTask.execute();
    }

    public void onCurrentTaskComplete() {
        if (interruptedTask != null) {
            // Resume interrupted task
            currentTask = interruptedTask;
            interruptedTask = null;
            currentTask.resume();
        } else {
            currentTask = null;
        }
    }

    public void processInterrupts() {
        while (!interruptQueue.isEmpty()) {
            Interrupt interrupt = interruptQueue.poll();
            if (tryInterrupt(interrupt)) {
                break;  // Handled this interrupt, stop processing
            }
        }
    }
}

// Interrupt types
public class HostileInterrupt extends Interrupt {
    private final Entity hostile;

    public HostileInterrupt(Entity hostile) {
        super(Priority.COMBAT);
        this.hostile = hostile;
    }

    @Override
    public Task getTask() {
        return new CombatTask(hostile, Priority.HIGH);
    }
}

public class DamageInterrupt extends Interrupt {
    private final DamageSource source;
    private final float damage;

    public DamageInterrupt(DamageSource source, float damage) {
        super(damage > 5.0f ? Priority.CRITICAL : Priority.HIGH);
        this.source = source;
        this.damage = damage;
    }

    @Override
    public Task getTask() {
        if (damage > 10.0f) {
            return new FleeTask(Priority.CRITICAL);
        } else {
            return new DefendTask(source.getEntity(), Priority.HIGH);
        }
    }
}

public class PlayerCommandInterrupt extends Interrupt {
    private final String command;

    public PlayerCommandInterrupt(String command) {
        super(Priority.PLAYER_COMMAND);
        this.command = command;
    }

    @Override
    public Task getTask() {
        return new PlayerCommandTask(command, Priority.PLAYER_COMMAND);
    }
}
```

### 5.4 Recovery Behaviors

**Description:** Specific behaviors for recovering from failures, errors, or bad states. Enable graceful degradation and recovery.

**When to Use:**
- Handling action failures
- Recovering from bad states
- Graceful error handling
- Resilient behavior

**Pseudocode Implementation:**
```python
class RecoverySystem:
    def __init__(self):
        self.recovery_behaviors = {}

    def register_recovery(self, error_type, behavior):
        self.recovery_behaviors[error_type] = behavior

    def recover_from(self, error, context):
        behavior = self.recovery_behaviors.get(type(error))
        if behavior:
            return behavior.attempt_recovery(error, context)
        return self.default_recovery(error, context)

    def default_recovery(self, error, context):
        # Default: return to safe state
        return "return_to_safe_state"
```

**Minecraft Application:**
```java
public class RecoverySystem {
    private final Map<ErrorType, RecoveryBehavior> recoveryBehaviors = new ConcurrentHashMap<>();

    public void registerRecovery(ErrorType errorType, RecoveryBehavior behavior) {
        recoveryBehaviors.put(errorType, behavior);
    }

    public ActionResult recoverFrom(ErrorType error, ActionContext context, Exception e) {
        RecoveryBehavior behavior = recoveryBehaviors.get(error);
        if (behavior != null) {
            return behavior.attemptRecovery(context, e);
        }
        return defaultRecovery(context, e);
    }

    private ActionResult defaultRecovery(ActionContext context, Exception e) {
        // Default: return to safe state
        return ActionResult.failed("Unable to recover: " + e.getMessage());
    }
}

// Recovery behaviors
public class PathfindingRecovery implements RecoveryBehavior {
    @Override
    public ActionResult attemptRecovery(ActionContext context, Exception e) {
        SteveEntity steve = context.getSteve();
        BlockPos target = context.getTarget();

        // Try alternative path
        Path alternative = steve.findAlternativePath(target);
        if (alternative != null && alternative.reachesTarget()) {
            context.setPath(alternative);
            return ActionResult.retry("Found alternative path");
        }

        // Try clearing obstacles
        ActionResult clearResult = clearObstacles(steve, target);
        if (clearResult.isSuccess()) {
            return ActionResult.retry("Cleared obstacles, retrying");
        }

        // Give up
        return ActionResult.failed("Cannot reach target");
    }
}

public class CombatRecovery implements RecoveryBehavior {
    @Override
    public ActionResult attemptRecovery(ActionContext context, Exception e) {
        SteveEntity steve = context.getSteve();

        if (steve.getHealth() < 6.0f) {
            // Low health, flee
            return ActionResult.switchAction(new FleeAction(steve, findSafeLocation(steve)));
        }

        // Try switching weapons
        if (steve.hasAlternativeWeapon()) {
            steve.switchToAlternativeWeapon();
            return ActionResult.retry("Switched weapons");
        }

        // Call for help
        requestAlliedSupport(steve);

        return ActionResult.retry("Requested support");
    }
}

public class BlockInteractionRecovery implements RecoveryBehavior {
    @Override
    public ActionResult attemptRecovery(ActionContext context, Exception e) {
        SteveEntity steve = context.getSteve();
        BlockPos target = context.getTarget();

        // Check if block still exists
        if (!steve.level().getBlockState(target).is(context.getExpectedBlock())) {
            return ActionResult.failed("Block changed");
        }

        // Check if still in range
        if (steve.position().distanceTo(Vec3.atCenterOf(target)) > 5.0) {
            ActionResult moveResult = steve.moveTo(target);
            if (moveResult.isSuccess()) {
                return ActionResult.retry("Moved into range");
            }
        }

        // Check if right tool
        ItemStack currentTool = steve.getMainHandItem();
        ItemStack betterTool = steve.getBetterToolFor(target);
        if (!betterTool.isEmpty() && !betterTool.equals(currentTool)) {
            steve.setItemSlot(EquipmentSlot.MAINHAND, betterTool);
            return ActionResult.retry("Switched to better tool");
        }

        return ActionResult.failed("Cannot interact with block");
    }
}
```

---

## 6. Proactive Patterns

Proactive patterns enable AI agents to take initiative and anticipate needs rather than just reacting to events. These patterns create more intelligent, forward-looking behavior.

### 6.1 Goal Generation

**Description:** Agents generate their own goals based on environment analysis, needs assessment, and opportunity detection. Not just executing player commands.

**When to Use:**
- Autonomous behavior desired
- Agent should take initiative
- Player wants helper, not just executor
- Complex, long-term objectives

**Pseudocode Implementation:**
```python
class GoalGenerator:
    def __init__(self):
        self.goal_sources = []

    def add_goal_source(self, source):
        self.goal_sources.append(source)

    def generate_goals(self, agent, world):
        goals = []

        for source in self.goal_sources:
            new_goals = source.generate(agent, world)
            goals.extend(new_goals)

        # Prioritize goals
        goals.sort(key=lambda g: g.priority, reverse=True)

        return goals

    def select_top_goal(self, agent, world):
        goals = self.generate_goals(agent, world)
        return goals[0] if goals else None
```

**Minecraft Application:**
```java
public class GoalGenerator {
    private final List<GoalSource> goalSources = new ArrayList<>();

    public void addGoalSource(GoalSource source) {
        goalSources.add(source);
    }

    public List<Goal> generateGoals(SteveEntity steve, World world) {
        List<Goal> goals = new ArrayList<>();

        for (GoalSource source : goalSources) {
            goals.addAll(source.generate(steve, world));
        }

        // Sort by priority
        goals.sort(Comparator.comparingDouble(Goal::getPriority).reversed());

        return goals;
    }

    public Optional<Goal> selectTopGoal(SteveEntity steve, World world) {
        List<Goal> goals = generateGoals(steve, world);
        return goals.isEmpty() ? Optional.empty() : Optional.of(goals.get(0));
    }
}

// Goal sources
public class ResourceGoalSource implements GoalSource {
    @Override
    public List<Goal> generate(SteveEntity steve, World world) {
        List<Goal> goals = new ArrayList<>();

        // Check inventory needs
        if (steve.getInventory().count(Items.DIAMOND) < 5) {
            goals.add(new GatherGoal(Items.DIAMOND, 5, Priority.MEDIUM));
        }

        if (steve.getInventory().count(Items.IRON_INGOT) < 64) {
            goals.add(new GatherGoal(Items.IRON_INGOT, 64, Priority.LOW));
        }

        // Check tool durability
        if (steve.getToolDurability() < 20) {
            goals.add(new CraftGoal(Items.DIAMOND_PICKAXE, Priority.HIGH));
        }

        return goals;
    }
}

public class SafetyGoalSource implements GoalSource {
    @Override
    public List<Goal> generate(SteveEntity steve, World world) {
        List<Goal> goals = new ArrayList<>();

        // Check for threats
        if (hasNearbyHostile(steve, world, 20.0)) {
            goals.add(new EliminateThreatGoal(Priority.CRITICAL));
        }

        // Check health
        if (steve.getHealth() < 10.0f) {
            goals.add(new HealGoal(Priority.HIGH));
        }

        // Check time of day
        if (!world.isDay() && !hasShelter(steve)) {
            goals.add(new FindShelterGoal(Priority.MEDIUM));
        }

        return goals;
    }
}

public class ConstructionGoalSource implements GoalSource {
    private final Queue<StructurePlan> plannedStructures = new LinkedList<>();

    @Override
    public List<Goal> generate(SteveEntity steve, World world) {
        List<Goal> goals = new ArrayList<>();

        // Add planned structures
        for (StructurePlan plan : plannedStructures) {
            if (canStartBuilding(steve, plan)) {
                goals.add(new BuildGoal(plan, calculatePriority(plan)));
            }
        }

        // Generate new structure goals based on needs
        if (!hasStorage(steve)) {
            goals.add(new PlanAndBuildGoal("storage", Priority.MEDIUM));
        }

        if (!hasFarm(steve) && hasFoodSource(steve)) {
            goals.add(new PlanAndBuildGoal("farm", Priority.LOW));
        }

        return goals;
    }
}
```

### 6.2 Opportunity Detection

**Description:** Continuously scan environment for opportunities that align with agent goals or player needs. Alert and act on opportunities.

**When to Use:**
- Agent should notice things player misses
- Proactive assistance
- Resource optimization
- Strategic planning

**Pseudocode Implementation:**
```python
class OpportunityDetector:
    def __init__(self):
        self.detectors = []

    def add_detector(self, detector):
        self.detectors.append(detector)

    def scan(self, agent, world):
        opportunities = []

        for detector in self.detectors:
            ops = detector.detect(agent, world)
            opportunities.extend(ops)

        # Score and rank opportunities
        opportunities.sort(key=lambda o: o.score, reverse=True)

        return opportunities

    def get_best_opportunity(self, agent, world):
        opportunities = self.scan(agent, world)
        return opportunities[0] if opportunities else None
```

**Minecraft Application:**
```java
public class OpportunityDetector {
    private final List<Detector> detectors = new ArrayList<>();

    public void addDetector(Detector detector) {
        detectors.add(detector);
    }

    public List<Opportunity> scan(SteveEntity steve, World world) {
        List<Opportunity> opportunities = new ArrayList<>();

        for (Detector detector : detectors) {
            opportunities.addAll(detector.detect(steve, world));
        }

        // Sort by score
        opportunities.sort(Comparator.comparingDouble(Opportunity::getScore).reversed());

        return opportunities;
    }

    public Optional<Opportunity> getBestOpportunity(SteveEntity steve, World world) {
        List<Opportunity> opportunities = scan(steve, world);
        return opportunities.isEmpty() ? Optional.empty() : Optional.of(opportunities.get(0));
    }
}

// Opportunity detectors
public class ResourceOpportunityDetector implements Detector {
    @Override
    public List<Opportunity> detect(SteveEntity steve, World world) {
        List<Opportunity> opportunities = new ArrayList<>();

        // Scan for valuable ores
        for (BlockPos pos : findNearbyOres(steve, 50)) {
            Block block = world.getBlockState(pos).getBlock();

            if (isValuable(block)) {
                double score = calculateOreScore(steve, pos, block);
                opportunities.add(new ResourceOpportunity(pos, block, score));
            }
        }

        return opportunities;
    }

    private double calculateOreScore(SteveEntity steve, BlockPos pos, Block block) {
        double score = 100.0;

        // Value of resource
        score += getResourceValue(block) * 10;

        // Distance factor
        double distance = steve.position().distanceTo(Vec3.atCenterOf(pos));
        score -= distance * 0.5;

        // Accessibility
        if (steve.canReach(pos)) {
            score += 20;
        }

        // Tool availability
        if (steve.hasToolFor(block)) {
            score += 10;
        }

        return score;
    }
}

public class ConstructionOpportunityDetector implements Detector {
    @Override
    public List<Opportunity> detect(SteveEntity steve, World world) {
        List<Opportunity> opportunities = new ArrayList<>();

        // Check for good building locations
        BlockPos currentLocation = steve.blockPosition();

        for (BlockPos pos : findFlatAreasNearby(currentLocation, 30)) {
            double score = rateBuildingLocation(steve, pos);
            if (score > 50) {
                opportunities.add(new ConstructionOpportunity(pos, score));
            }
        }

        // Check for incomplete structures
        for (Structure structure : getIncompleteStructures(steve)) {
            if (canContinueBuilding(steve, structure)) {
                double score = calculateContinuationScore(steve, structure);
                opportunities.add(new ContinueConstructionOpportunity(structure, score));
            }
        }

        return opportunities;
    }
}

public class ThreatOpportunityDetector implements Detector {
    @Override
    public List<Opportunity> detect(SteveEntity steve, World world) {
        List<Opportunity> opportunities = new ArrayList<>();

        // Detect threats early
        for (Entity entity : findNearbyHostiles(steve, 30)) {
            double threatLevel = calculateThreatLevel(entity);

            // High threat = opportunity to eliminate before becomes danger
            if (threatLevel > 0.5) {
                double score = threatLevel * 100;
                opportunities.add(new EliminateThreatOpportunity(entity, score));
            }
        }

        return opportunities;
    }
}
```

### 6.3 Anticipatory Behavior

**Description:** Predict future events or needs and prepare for them in advance. Don't wait for problems to occur.

**When to Use:**
- Preventing problems
- Optimizing workflows
- Strategic preparation
- Reducing reactive overhead

**Pseudocode Implementation:**
```python
class AnticipatorySystem:
    def __init__(self):
        self.predictors = []

    def add_predictor(self, predictor):
        self.predictors.append(predictor)

    def anticipate_and_prepare(self, agent, world):
        predictions = []

        for predictor in self.predictors:
            pred = predictor.predict(agent, world)
            predictions.append(pred)

        # Prepare for likely events
        for pred in predictions:
            if pred.probability > 0.7:
                self.prepare_for(agent, pred.event)

    def prepare_for(self, agent, event):
        if event.type == "night":
            agent.prepare_shelter()
        elif event.type == "combat":
            agent.prepare_weapons()
        # etc.
```

**Minecraft Application:**
```java
public class AnticipatorySystem {
    private final List<Predictor> predictors = new ArrayList<>();

    public void addPredictor(Predictor predictor) {
        predictors.add(predictor);
    }

    public void anticipateAndPrepare(SteveEntity steve, World world) {
        for (Predictor predictor : predictors) {
            Prediction prediction = predictor.predict(steve, world);

            if (prediction.getProbability() > 0.7) {
                prepareFor(steve, prediction);
            }
        }
    }

    private void prepareFor(SteveEntity steve, Prediction prediction) {
        PreparedEvent event = prediction.getEvent();

        switch (event.getType()) {
            case NIGHT_TIME:
                prepareForNight(steve);
                break;
            case COMBAT:
                prepareForCombat(steve);
                break;
            case MINING_EXPEDITION:
                prepareForMining(steve);
                break;
            case LONG_TRAVEL:
                prepareForTravel(steve);
                break;
        }
    }

    private void prepareForNight(SteveEntity steve) {
        // Find or build shelter before night
        if (!hasShelter(steve)) {
            BlockPos shelterLoc = findNearestShelter(steve);
            if (shelterLoc != null) {
                steve.setDestination(shelterLoc);
            } else {
                // Build emergency shelter
                steve.setCurrentTask(new BuildShelterTask(steve.blockPosition()));
            }
        }

        // Place torches
        steve.ensureLighting();
    }

    private void prepareForCombat(SteveEntity steve) {
        // Equip best weapon
        steve.equipBestWeapon();

        // Equip armor
        steve.equipBestArmor();

        // Stock healing items
        steve.ensureHealingItems(2);

        // Clear escape path
        steve.clearPathToSafety();
    }

    private void prepareForMining(SteveEntity steve) {
        // Ensure tools
        steve.ensurePickaxeDurability(100);

        // Bring torches
        steve.ensureItemCount(Items.TORCH, 64);

        // Bring food
        steve.ensureFood(20);

        // Clear inventory space
        steve.clearLowPriorityItems();
    }
}

// Predictors
public class TimeOfDayPredictor implements Predictor {
    @Override
    public Prediction predict(SteveEntity steve, World world) {
        long dayTime = world.getDayTime() % 24000;

        if (dayTime > 12000 && dayTime < 13000) {
            // Sunset approaching
            return new Prediction(PreparedEvent.NIGHT_TIME, 0.95);
        }

        return Prediction.NONE;
    }
}

public class HostilePredictor implements Predictor {
    @Override
    public Prediction predict(SteveEntity steve, World world) {
        // Check for nearby hostiles
        List<Entity> hostiles = findHostilesNearby(steve, 20);
        if (!hostiles.isEmpty()) {
            return new Prediction(PreparedEvent.COMBAT, 0.9);
        }

        // Check for dark areas (spawns)
        if (isInDarkArea(steve, world)) {
            return new Prediction(PreparedEvent.COMBAT, 0.7);
        }

        return Prediction.NONE;
    }
}

public class ResourceNeedPredictor implements Predictor {
    @Override
    public Prediction predict(SteveEntity steve, World world) {
        // Predict resource needs based on current task
        Task current = steve.getCurrentTask();

        if (current instanceof BuildTask) {
            BuildTask buildTask = (BuildTask) current;
            Map<Item, Integer> needed = buildTask.getRemainingMaterials();

            for (Map.Entry<Item, Integer> entry : needed.entrySet()) {
                int have = steve.getInventory().count(entry.getKey());
                if (have < entry.getValue()) {
                    return new Prediction(
                        new GatherResourceEvent(entry.getKey(), entry.getValue() - have),
                        0.8
                    );
                }
            }
        }

        return Prediction.NONE;
    }
}
```

### 6.4 Planning Lookahead

**Description:** Generate plans for multiple steps ahead, not just immediate actions. Consider future consequences and requirements.

**When to Use:**
- Complex multi-step tasks
- Resource management
- Coordinated actions
- Avoiding dead ends

**Pseudocode Implementation:**
```python
class LookaheadPlanner:
    def __init__(self, depth=5):
        self.depth = depth

    def plan(self, agent, world, goal):
        # Generate sequence of actions
        plan = []

        for i in range(self.depth):
            # Predict world state after current plan
            predicted_state = self.simulate(agent, world, plan)

            # Find best next action
            action = self.select_best_action(agent, predicted_state, goal)
            if not action:
                break

            plan.append(action)

        return plan

    def simulate(self, agent, world, plan):
        # Simulate world state after executing plan
        state = world.copy()

        for action in plan:
            state = action.simulate(state)

        return state
```

**Minecraft Application:**
```java
public class LookaheadPlanner {
    private final int depth;

    public LookaheadPlanner(int depth) {
        this.depth = depth;
    }

    public List<Action> plan(SteveEntity steve, World world, Goal goal) {
        List<Action> plan = new ArrayList<>();
        WorldState simulatedState = new WorldState(world);

        for (int i = 0; i < depth; i++) {
            // Select best action given predicted state
            Action action = selectBestAction(steve, simulatedState, goal);
            if (action == null) {
                break;
            }

            plan.add(action);

            // Simulate action effects
            simulatedState = simulateAction(steve, simulatedState, action);

            // Check if goal reached
            if (goal.isAchieved(simulatedState)) {
                break;
            }
        }

        return plan;
    }

    private Action selectBestAction(SteveEntity steve, WorldState state, Goal goal) {
        List<Action> possibleActions = generatePossibleActions(steve, state);

        Action bestAction = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Action action : possibleActions) {
            WorldState newState = simulateAction(steve, state, action);
            double score = evaluateState(newState, goal);

            if (score > bestScore) {
                bestScore = score;
                bestAction = action;
            }
        }

        return bestAction;
    }

    private WorldState simulateAction(SteveEntity steve, WorldState state, Action action) {
        WorldState newState = state.copy();

        // Simulate position change
        newState.setAgentPosition(steve.getId(), action.predictDestination());

        // Simulate inventory change
        action.predictInventoryChange(newState);

        // Simulate world change
        action.predictWorldChange(newState);

        return newState;
    }

    private double evaluateState(WorldState state, Goal goal) {
        double score = 0.0;

        // Distance to goal
        score -= state.distanceToGoal(goal) * 10;

        // Resource availability
        score += state.getResourceScore(goal) * 5;

        // Time estimate
        score -= state.getTimeToGoal(goal);

        // Risk assessment
        score -= state.getRiskLevel() * 20;

        return score;
    }

    private List<Action> generatePossibleActions(SteveEntity steve, WorldState state) {
        List<Action> actions = new ArrayList<>();

        // Movement actions
        for (BlockPos neighbor : getReachablePositions(steve, state, 5)) {
            actions.add(new MoveAction(steve, neighbor));
        }

        // Mining actions
        for (BlockPos ore : getNearbyOres(steve, state, 20)) {
            if (steve.hasToolFor(state.getBlockState(ore))) {
                actions.add(new MineAction(steve, ore));
            }
        }

        // Building actions
        if (steve.getCurrentTask() instanceof BuildTask) {
            BuildTask buildTask = (BuildTask) steve.getCurrentTask();
            for (BlockPlacement placement : buildTask.getNextPlacements(3)) {
                if (canPlace(steve, state, placement)) {
                    actions.add(new PlaceBlockAction(steve, placement));
                }
            }
        }

        return actions;
    }
}

// Example usage for multi-step planning
public class ComplexTaskPlanner {
    private final LookaheadPlanner planner = new LookaheadPlanner(10);

    public TaskPlan planComplexTask(SteveEntity steve, BuildTask task) {
        // Goal: Complete structure
        Goal goal = new StructureCompletionGoal(task.getStructure());

        // Generate plan
        List<Action> actions = planner.plan(steve, steve.level(), goal);

        // Organize into phases
        TaskPlan plan = new TaskPlan(task);

        // Phase 1: Gather materials
        plan.addPhase(createGatheringPhase(actions, task));

        // Phase 2: Prepare site
        plan.addPhase(createSitePrepPhase(actions, task));

        // Phase 3: Build structure
        plan.addPhase(createConstructionPhase(actions, task));

        return plan;
    }
}
```

---

## 7. Pattern Selection Guide

Choosing the right pattern for your use case is critical for effective AI behavior. This guide helps match patterns to specific needs.

### Decision Matrix

| Requirement | Recommended Patterns | Alternatives |
|-------------|---------------------|--------------|
| **Simple, fast decisions** | Priority-Based, Rule Chaining | Utility Scoring |
| **Nuanced trade-offs** | Utility Scoring | Weighted Random |
| **Variety/unpredictability** | Weighted Random | Softmax Selection |
| **Graceful failure handling** | Fallback Cascades | Error Recovery |
| **Learning from experience** | Success/Failure Counting | Adaptive Thresholds |
| **Multi-agent work** | Leader-Follower, Auction | Consensus Voting |
| **Information sharing** | Blackboard | Shared Memory |
| **Emergency response** | Event-Triggered | Interrupt Handling |
| **Initiative-taking** | Goal Generation | Opportunity Detection |
| **Strategic planning** | Planning Lookahead | Anticipatory Behavior |

### Complexity Comparison

```
Low Complexity
├── Priority-Based Selection (Simplest)
├── Rule Chaining
├── Success/Failure Counting
├── Event-Triggered Responses
├── Cooldown Systems
└── Leader-Follower

Medium Complexity
├── Weighted Random Selection
├── Fallback Cascades
├── Short-Term Memory
├── Working Memory
├── Role Assignment
├── Recovery Behaviors
├── Goal Generation
└── Opportunity Detection

High Complexity
├── Utility Scoring
├── Adaptive Thresholds
├── Pattern Extraction
├── Long-Term Memory
├── Shared Memory
├── Consensus Voting
├── Auction/Bidding
├── Blackboard Sharing
├── Interrupt Handling
├── Anticipatory Behavior
└── Planning Lookahead
```

### Performance Considerations

| Pattern | CPU Cost | Memory | Best For |
|---------|----------|--------|----------|
| Priority-Based | Very Low | Very Low | Real-time decisions |
| Rule Chaining | Low | Low | Complex logic chains |
| Utility Scoring | Medium | Low | Nuanced decisions |
| Weighted Random | Low | Low | Variety |
| Fallback Cascades | Medium | Low | Robustness |
| Success/Failure | Low | Medium | Learning |
| Adaptive Thresholds | Low | Low | Adaptation |
| Pattern Extraction | High | High | Prediction |
| Leader-Follower | Low | Low | Coordination |
| Consensus Voting | Medium | Medium | Democracy |
| Auction/Bidding | Medium | Medium | Allocation |
| Blackboard | Medium | High | Information sharing |
| Event-Triggered | Low | Low | Reactivity |
| Cooldowns | Very Low | Low | Rate limiting |
| Interrupts | Low | Medium | Priority handling |
| Recovery | Medium | Low | Resilience |
| Goal Generation | Medium | Medium | Autonomy |
| Opportunity Detection | High | Medium | Proactivity |
| Anticipatory | High | Medium | Preparation |
| Planning Lookahead | Very High | High | Strategy |

---

## 8. Implementation Matrix

### Minecraft-Specific Pattern Applications

#### Mining Automation

| Phase | Primary Patterns | Secondary Patterns |
|-------|-----------------|-------------------|
| **Ore Detection** | Opportunity Detection, Event-Triggered | Pattern Extraction |
| **Path Selection** | Utility Scoring, Fallback Cascades | Success/Failure Counting |
| **Resource Allocation** | Auction/Bidding, Role Assignment | Leader-Follower |
| **Execution** | Event-Triggered, Recovery Behaviors | Cooldown Systems |
| **Learning** | Success/Failure Counting, Pattern Extraction | Adaptive Thresholds |

#### Building Construction

| Phase | Primary Patterns | Secondary Patterns |
|-------|-----------------|-------------------|
| **Planning** | Planning Lookahead, Goal Generation | Anticipatory Behavior |
| **Material Gathering** | Role Assignment, Leader-Follower | Utility Scoring |
| **Site Preparation** | Fallback Cascades, Priority-Based | Recovery Behaviors |
| **Construction** | Rule Chaining, Working Memory | Event-Triggered |
| **Coordination** | Blackboard Sharing, Role Assignment | Consensus Voting |

#### Combat System

| Phase | Primary Patterns | Secondary Patterns |
|-------|-----------------|-------------------|
| **Threat Detection** | Opportunity Detection, Event-Triggered | Pattern Extraction |
| **Decision Making** | Utility Scoring, Priority-Based | Adaptive Thresholds |
| **Response** | Interrupt Handling, Recovery Behaviors | Cooldown Systems |
| **Coordination** | Leader-Follower, Shared Memory | Consensus Voting |
| **Learning** | Success/Failure Counting | Pattern Extraction |

#### Resource Management

| Phase | Primary Patterns | Secondary Patterns |
|-------|-----------------|-------------------|
| **Inventory Tracking** | Short-Term Memory, Long-Term Memory | Shared Memory |
| **Needs Assessment** | Goal Generation, Anticipatory Behavior | Utility Scoring |
| **Acquisition** | Role Assignment, Leader-Follower | Auction/Bidding |
| **Distribution** | Blackboard Sharing, Consensus Voting | Priority-Based |
| **Optimization** | Success/Failure Counting | Pattern Extraction |

### Integration Examples

#### Example 1: Autonomous Mining Crew

```java
public class MiningCrewSystem {
    // Decision Making
    private final PriorityBasedDecision priorityDecision;
    private final UtilityScoringDecision utilityDecision;

    // Memory
    private final ShortTermMemory shortTermMemory;
    private final LongTermMemory longTermMemory;
    private final SharedMemory sharedMemory;

    // Coordination
    private final LeaderFollowerSystem leaderFollower;
    private final AuctionSystem auctionSystem;
    private final Blackboard blackboard;

    // Learning
    private final SuccessFailureTracker successFailure;
    private final PatternExtractor patternExtractor;

    // Reactive
    private final EventTriggeredSystem eventSystem;
    private final InterruptSystem interruptSystem;

    // Proactive
    private final GoalGenerator goalGenerator;
    private final OpportunityDetector opportunityDetector;

    public void tick(MiningCrew crew, World world) {
        // 1. Check for immediate threats (Priority)
        for (MinerAgent miner : crew.getMiners()) {
            if (priorityDecision.selectAction(miner, world).isEmergency()) {
                handleEmergency(miner, world);
                return;
            }
        }

        // 2. Share discoveries (Blackboard)
        for (MinerAgent miner : crew.getMiners()) {
            for (OreDiscovery discovery : miner.getRecentDiscoveries()) {
                blackboard.post(miner.getId(), "ore:" + discovery.position, discovery);
            }
        }

        // 3. Assign tasks (Auction)
        for (Task task : crew.getPendingTasks()) {
            String auctionId = auctionSystem.announceTask(task, 5000);
            for (MinerAgent miner : crew.getAvailableMiners()) {
                Bid bid = miner.calculateBid(task);
                auctionSystem.submitBid(miner.getId(), auctionId, bid);
            }
            AuctionResult result = auctionSystem.closeAuction(auctionId);
            if (result != null) {
                crew.assignTask(result.winnerId, task);
            }
        }

        // 4. Execute tasks with learning
        for (MinerAgent miner : crew.getMiners()) {
            Task task = miner.getCurrentTask();
            if (task != null) {
                ActionResult result = miner.executeTask(task);
                successFailure.recordAttempt(generateKey(task), result.isSuccess(), result.getDuration());
            }
        }

        // 5. Generate new goals
        for (MinerAgent miner : crew.getMiners()) {
            if (miner.isIdle()) {
                List<Goal> goals = goalGenerator.generateGoals(miner, world);
                for (Goal goal : goals) {
                    if (goal.getPriority() > 0.7) {
                        miner.setGoal(goal);
                        break;
                    }
                }
            }
        }

        // 6. Detect opportunities
        List<Opportunity> opportunities = opportunityDetector.scan(crew.getForeman(), world);
        for (Opportunity opp : opportunities) {
            if (opp.getScore() > 50) {
                crew.addTask(opp.toTask());
            }
        }
    }
}
```

#### Example 2: Adaptive Combat Agent

```java
public class AdaptiveCombatAgent {
    // Decision systems
    private final PriorityBasedDecision priorityDecision;
    private final UtilityScoringDecision utilityDecision;
    private final AdaptiveThresholdSystem thresholds;

    // Memory
    private final ShortTermMemory combatMemory;
    private final LongTermMemory learningMemory;

    // Reactive
    private final EventTriggeredSystem eventSystem;
    private final InterruptSystem interruptSystem;
    private final CooldownSystem cooldowns;

    // Recovery
    private final RecoverySystem recoverySystem;

    // Learning
    private final SuccessFailureTracker combatTracker;

    public void onAttacked(SteveEntity steve, DamageSource source) {
        // Record event
        combatMemory.remember(new MemoryEvent(
            MemoryEventType.DAMAGED,
            steve.position(),
            Map.of("source", source.getMsgId(), "damage", source.getDamage())
        ));

        // Check for interrupt
        DamageInterrupt interrupt = new DamageInterrupt(source, source.getDamage());
        if (interruptSystem.tryInterrupt(interrupt)) {
            return;
        }

        // Adaptive response based on past performance
        String combatKey = "combat:" + source.getEntity().getType().toString();
        double successRate = combatTracker.getSuccessRate(combatKey);

        if (successRate < 0.3) {
            // Poor performance, flee
            steve.setCurrentTask(new FleeTask(Priority.HIGH));
        } else if (successRate > 0.7) {
            // Good performance, engage aggressively
            steve.setCurrentTask(new CombatTask(source.getEntity(), Priority.HIGH));
        } else {
            // Unknown, use utility decision
            Action decision = utilityDecision.selectAction(steve,
                List.of(new CombatAction(steve, source.getEntity()),
                       new FleeAction(steve, findSafeLocation(steve))));
            decision.execute();
        }
    }

    public void onCombatComplete(SteveEntity steve, Entity enemy, boolean victory) {
        // Record outcome
        String combatKey = "combat:" + enemy.getType().toString();
        combatTracker.recordAttempt(combatKey, victory,
            System.currentTimeMillis() - steve.getCombatStartTime());

        if (victory) {
            // Adjust thresholds - be more willing to engage
            thresholds.recordResult(combatKey, true);
        } else {
            // Adjust thresholds - be more cautious
            thresholds.recordResult(combatKey, false);
        }

        // Learn from the encounter
        combatMemory.remember(new MemoryEvent(
            MemoryEventType.COMBAT_RESULT,
            steve.position(),
            Map.of("enemy", enemy.getType(), "victory", victory)
        ));
    }
}
```

#### Example 3: Proactive Builder Agent

```java
public class ProactiveBuilderAgent {
    // Proactive systems
    private final GoalGenerator goalGenerator;
    private final OpportunityDetector opportunityDetector;
    private final AnticipatorySystem anticipatorySystem;
    private final LookaheadPlanner lookaheadPlanner;

    // Decision making
    private final UtilityScoringDecision utilityDecision;

    // Memory
    private final WorkingMemory workingMemory;
    private final LongTermMemory longTermMemory;

    // Coordination
    private final RoleAssignmentSystem roleSystem;
    private final Blackboard blackboard;

    public void tick(SteveEntity steve, World world) {
        // 1. Anticipate and prepare
        anticipatorySystem.anticipateAndPrepare(steve, world);

        // 2. Scan for opportunities
        List<Opportunity> opportunities = opportunityDetector.scan(steve, world);
        for (Opportunity opp : opportunities) {
            if (opp.getScore() > 70) {
                steve.addGoal(opp.toGoal());
            }
        }

        // 3. Generate goals
        List<Goal> goals = goalGenerator.generateGoals(steve, world);

        // 4. Select best goal using utility
        Goal bestGoal = utilityDecision.selectBestGoal(steve, goals);

        // 5. Plan ahead
        if (bestGoal != null) {
            List<Action> plan = lookaheadPlanner.plan(steve, world, bestGoal.toPlannerGoal());
            workingMemory.set("current_plan", plan);

            // Share plan with team
            blackboard.post(steve.getId(), "plan:" + bestGoal.getId(), plan);
        }

        // 6. Execute current plan
        List<Action> currentPlan = workingMemory.get("current_plan", List.class);
        if (currentPlan != null && !currentPlan.isEmpty()) {
            Action nextAction = currentPlan.get(0);
            ActionResult result = nextAction.execute();

            if (result.isSuccess()) {
                currentPlan.remove(0);

                // Report progress
                blackboard.post(steve.getId(), "progress:" + bestGoal.getId(),
                    Map.of("complete", currentPlan.isEmpty()));
            } else if (result.shouldRetry()) {
                // Retry same action
            } else {
                // Handle failure
                handlePlanFailure(steve, bestGoal, result);
            }
        }
    }

    private void handlePlanFailure(SteveEntity steve, Goal goal, ActionResult result) {
        // Record failure
        longTermMemory.remember("failure:" + goal.getId(),
            Map.of("reason", result.getFailureReason(), "time", System.currentTimeMillis()),
            1.0);

        // Generate new plan
        workingMemory.set("current_plan", null);

        // If multiple failures, abandon goal temporarily
        int failureCount = (int) longTermMemory.recall("failure_count:" + goal.getId()).orElse(0) + 1;
        longTermMemory.remember("failure_count:" + goal.getId(), failureCount, 1.0);

        if (failureCount >= 3) {
            steve.abandonGoal(goal);
        }
    }
}
```

---

## 9. References

### Academic Sources

1. **Orkin, J. (2004)** - "Applying Goal-Oriented Action Planning to Games" - AAAI Conference
2. **Isla, D. (2005)** - "Handling Complexity in the Halo 2 AI" - GDC Proceedings
3. **Rabin, S. (2002)** - "AI Game Programming Wisdom" - Charles River Media
4. **Buckland, M. (2005)** - "Programming Game AI by Example" - Wordware Publishing
5. **Champandard, A.J. (2003)** - "AI Game Development" - New Riders

### Game Industry Sources

6. **Halo 2 AI** - Behavior tree pioneer, 2004
7. **F.E.A.R. AI** - GOAP implementation, 2005
8. **The Sims** - Utility AI for needs, 2000
9. **Civilization IV** - Weighted decision systems, 2005
10. **StarCraft II** - Macro/micro coordination, 2010

### Modern Game AI References

11. **Millington, I. (2019)** - "AI for Games, 3rd Edition" - CRC Press
12. **Mark, D. (2009)** - "Behavior Trees for AI" - GDC Presentation
13. **Gormley, C. (2018)** - "Reactive AI Design" - Game Developer Magazine
14. **Rescorla, M. (2020)** - "Utility Systems in Practice" - AI Game Programming

### Open Source Projects

15. **libGDX AI** - Behavior tree and utility AI framework
16. **Apex AI** - Behavior tree library for Unity
17. **Cricle AI** - GOAP implementation for games
18. **Flentis AI** - Utility AI framework

### Minecraft AI Projects

19. **Baritone** - Automated pathfinding and mining
20. **Mineflayer** - Node.js Minecraft bot framework
21. **Steve AI** - "Cursor for Minecraft" autonomous agents
22. **AutoMc** - Macro automation system

---

## Appendix: Quick Reference

### Pattern Summary Table

| Category | Pattern | Complexity | CPU | Memory | Use Case |
|----------|---------|-----------|-----|--------|----------|
| **Decision** | Priority-Based | Low | Very Low | Very Low | Fast, simple decisions |
| **Decision** | Weighted Random | Low | Low | Low | Variety in behavior |
| **Decision** | Utility Scoring | Medium | Medium | Low | Nuanced trade-offs |
| **Decision** | Rule Chaining | Low | Low | Low | Complex logic chains |
| **Decision** | Fallback Cascades | Medium | Medium | Low | Graceful degradation |
| **Memory** | Short-Term | Medium | Medium | Medium | Recent events |
| **Memory** | Long-Term | High | Low | High | Persistent learning |
| **Memory** | Working | Low | Very Low | Very Low | Task state |
| **Memory** | Shared | High | Medium | High | Team knowledge |
| **Coordination** | Leader-Follower | Medium | Low | Low | Hierarchical teams |
| **Coordination** | Consensus | High | Medium | Medium | Democratic decisions |
| **Coordination** | Auction | Medium | Medium | Low | Task allocation |
| **Coordination** | Blackboard | High | Medium | High | Information sharing |
| **Coordination** | Role Assignment | Medium | Low | Low | Specialization |
| **Learning** | Success/Failure | Low | Low | Medium | Performance tracking |
| **Learning** | Adaptive Thresholds | Low | Low | Low | Dynamic adjustment |
| **Learning** | Pattern Extraction | High | High | High | Prediction |
| **Learning** | Experience Weighted | Medium | Medium | Medium | Ensemble decisions |
| **Reactive** | Event-Triggered | Low | Low | Low | Immediate response |
| **Reactive** | Cooldowns | Very Low | Very Low | Low | Rate limiting |
| **Reactive** | Interrupts | Low | Low | Medium | Priority handling |
| **Reactive** | Recovery | Medium | Medium | Low | Error handling |
| **Proactive** | Goal Generation | Medium | Medium | Medium | Autonomy |
| **Proactive** | Opportunity Detection | High | High | Medium | Initiative |
| **Proactive** | Anticipatory | High | High | Medium | Preparation |
| **Proactive** | Planning Lookahead | Very High | Very High | High | Strategy |

### Implementation Checklist

- [ ] Identify decision pattern requirements
- [ ] Select appropriate decision system
- [ ] Define memory architecture
- [ ] Implement coordination mechanisms
- [ ] Add learning capabilities
- [ ] Configure reactive behaviors
- [ ] Implement proactive systems
- [ ] Test pattern combinations
- [ ] Profile performance
- [ ] Tune parameters
- [ ] Document pattern usage
- [ ] Monitor and iterate

---

**End of Chapter 7: Comprehensive Pattern Library**

This chapter has cataloged 30+ years of game AI development patterns that enable sophisticated autonomous behavior without requiring LLMs or machine learning. When properly combined, these patterns create agents that are reactive, proactive, adaptive, and capable of complex coordinated behavior.

The next chapter explores how LLMs can enhance these classical patterns, not replace them.
