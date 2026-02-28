# Utility AI System

## Overview

The Utility AI System is a task prioritization framework that scores tasks based on multiple weighted factors. Inspired by game AI utility systems, it enables intelligent, context-aware decision making by evaluating how "desirable" each task is given the current situation.

### Key Benefits

- **Context-Aware Decisions**: Tasks scored based on current world state, agent status, and environment
- **Flexible Prioritization**: Easy to add new factors and adjust weights
- **Transparent Scoring**: Clear breakdown of why tasks are prioritized
- **Multi-Factor Evaluation**: Consider safety, urgency, efficiency, and more simultaneously
- **Extensible**: Simple API for adding custom factors

## Architecture

```
Task Queue
      |
      v
TaskPrioritizer
      |
      +---> Apply Factors
      |     |
      |     +---> URGENCY (1.8x)
      |     +---> SAFETY (2.0x)
      |     +---> RESOURCE_PROXIMITY (1.5x)
      |     +---> EFFICIENCY (1.2x)
      |     +---> SKILL_MATCH (1.0x)
      |     +---> PLAYER_PREFERENCE (1.0x)
      |     +---> TOOL_READINESS (0.8x)
      |     +---> HEALTH_STATUS (0.8x)
      |     +---> HUNGER_STATUS (0.7x)
      |     +---> TIME_OF_DAY (0.5x)
      |     +---> WEATHER_CONDITIONS (0.3x)
      |
      v
UtilityScore Calculation
      |
      +---> Weighted Sum / Total Weight
      |
      v
Sorted Task List (Highest Score First)
```

## Core Components

### UtilityFactor Interface

Functional interface for calculating task desirability.

```java
@FunctionalInterface
public interface UtilityFactor {
    /**
     * Calculates this factor's contribution to a task's utility score.
     * Returns a value from 0.0 (very unfavorable) to 1.0 (very favorable).
     */
    double calculate(Task task, DecisionContext context);

    /** Returns the name of this factor for logging/debugging */
    String getName();

    /** Returns the default weight (1.0 if not overridden) */
    default double getDefaultWeight() { return 1.0; }

    /** Returns whether this factor applies to the given task */
    default boolean appliesTo(Task task) { return true; }

    /** Returns a description of what this factor evaluates */
    default Optional<String> getDescription() { return Optional.empty(); }
}
```

### UtilityScore Record

Immutable record storing the score and breakdown.

```java
public record UtilityScore(
    double baseValue,           // Starting value (0.5 = neutral)
    Map<String, Double> factors, // Individual factor values
    double finalScore           // Final weighted score (0.0 to 1.0)
) {
    // High priority: score >= 0.7
    public boolean isHighPriority() { return finalScore >= 0.7; }

    // Low priority: score <= 0.3
    public boolean isLowPriority() { return finalScore <= 0.3; }

    // Detailed breakdown string
    public String toDetailedString() {
        return String.format("UtilityScore[%.2f] {urgency=%.2f, safety=%.2f, ...}",
            finalScore, factors.get("urgency"), factors.get("safety"));
    }
}
```

### DecisionContext

Provides all relevant information for scoring decisions.

```java
public class DecisionContext {
    // Agent state
    private final ForemanEntity foreman;
    private final double healthLevel;
    private final double hungerLevel;
    private final BlockPos agentPosition;

    // World state
    private final long gameTime;
    private final LocalDateTime realTime;
    private final boolean isDaytime;
    private final boolean isRaining;
    private final boolean isThundering;
    private final Collection<Entity> nearbyThreats;

    // Player info
    private final Player nearestPlayer;
    private final double distanceToPlayer;
    private final double relationshipLevel;

    // Resources
    private final Map<String, Integer> availableResources;
    private final Collection<ItemStack> availableTools;

    // Tasks
    private final Optional<Task> currentTask;
    private final Collection<Task> queuedTasks;

    // Factory method
    public static DecisionContext of(ForemanEntity foreman, Collection<Task> tasks) {
        // Automatically extracts all relevant information
        ...
    }
}
```

### TaskPrioritizer

Main class for prioritizing tasks with utility scoring.

```java
public class TaskPrioritizer {
    private final Map<UtilityFactor, Double> factors;

    public TaskPrioritizer() {
        this.factors = new ConcurrentHashMap<>();
    }

    // Add factor with default weight
    public TaskPrioritizer addFactor(UtilityFactor factor) {
        factors.put(factor, factor.getDefaultWeight());
        return this;
    }

    // Add factor with custom weight
    public TaskPrioritizer addFactor(UtilityFactor factor, double weight) {
        factors.put(factor, weight);
        return this;
    }

    // Prioritize a list of tasks
    public List<Task> prioritize(List<Task> tasks, DecisionContext context) {
        // Score all tasks, sort by score (highest first)
        ...
    }

    // Calculate score for a single task
    public UtilityScore score(Task task, DecisionContext context) {
        // Apply all applicable factors, combine weighted values
        ...
    }

    // Create with default factors
    public static TaskPrioritizer withDefaults() {
        TaskPrioritizer prioritizer = new TaskPrioritizer();
        prioritizer.addDefaultFactors();
        return prioritizer;
    }
}
```

## Scoring Factors

### Built-in Factors

#### URGENCY (Weight: 1.8)

Evaluates time pressure and deadlines.

**Scoring**:
- 1.0 = Deadline within 100 ticks (5 seconds)
- 0.9 = Overdue
- 0.7 = Deadline within 1000 ticks (50 seconds)
- 0.5 = No deadline or distant deadline
- 0.3 = Long-term task

**Action-based urgency**:
- attack: 0.8
- follow: 0.6
- mine/gather: 0.4
- build/place: 0.3

#### SAFETY (Weight: 2.0)

Assesses threat level and danger.

**Scoring**:
- 1.0 = Completely safe (no threats, full health, daytime)
- 0.7 = Minor threats (1-2 mobs, full health)
- 0.5 = Moderate risk (3-5 mobs or some damage)
- 0.3 = High risk (many mobs or low health)
- 0.0 = Extreme danger

**Adjustments**:
- Health < 20%: -0.5 penalty
- Health < 50%: -0.3 penalty
- Each threat: -0.1 to -0.4 penalty
- Nighttime: -0.1 penalty
- Combat tasks: -0.2 penalty (inherently risky)

#### RESOURCE_PROXIMITY (Weight: 1.5)

Evaluates distance to required resources.

**Scoring**:
- 1.0 = Within 16 blocks
- 0.7 = Within 64 blocks
- 0.5 = Within 128 blocks
- 0.3 = Far away

#### EFFICIENCY (Weight: 1.2)

Estimates completion efficiency.

**Base efficiency by action**:
- pathfind: 0.9
- follow: 0.8
- craft: 0.8
- place: 0.7
- mine: 0.6
- gather: 0.6
- build: 0.5
- attack: 0.5

**Quantity adjustments**:
- Quantity > 64: -0.2 penalty
- Quantity > 16: -0.1 penalty

#### SKILL_MATCH (Weight: 1.0)

Evaluates how well agent's skills match the task.

**Scoring based on completion history**:
- 1.0 = Expert (50+ completions)
- 0.7 = Skilled (20+ completions)
- 0.5 = Competent (5+ completions)
- 0.3 = Novice (0-4 completions)

#### PLAYER_PREFERENCE (Weight: 1.0)

Evaluates alignment with player preferences.

**Scoring**:
- Based on relationship level (0.0 to 1.0)
- Range: 0.3 to 0.7
- Higher relationship = higher preference alignment

#### TOOL_READINESS (Weight: 0.8)

Checks if agent has necessary tools.

**Scoring**:
- 1.0 = Has optimal tool (e.g., diamond pickaxe)
- 0.9 = Has adequate tool
- 0.5 = Can do without tool (by hand)
- 0.3 = Needs tool, has none

**Applies to**: mine, gather, build, place, craft

#### HEALTH_STATUS (Weight: 0.8)

Evaluates agent's current health level.

**Scoring**:
- 1.0 = Full health (>90%)
- 0.7 = Good health (60-90%)
- 0.5 = Moderate health (30-60%)
- 0.3 = Low health (<30%)

#### HUNGER_STATUS (Weight: 0.7)

Evaluates agent's hunger/saturation level.

**Scoring**:
- 1.0 = Well fed (>80% saturation)
- 0.7 = Moderate hunger (50-80%)
- 0.5 = Hungry (20-50%)
- 0.3 = Starving (<20%)

**Special case**: Food gathering gets 1.0 when hunger < 50%

#### TIME_OF_DAY (Weight: 0.5)

Evaluates whether task is suitable for current time.

**Daytime preferences**:
- build, place, gather, mine: 0.8
- pathfind: 0.7

**Nighttime preferences**:
- attack: 0.7 (mobs spawn at night)
- pathfind: 0.5 (more dangerous)

**Neutral**:
- craft: 0.7 (anytime)
- follow: 0.7 (anytime)

#### WEATHER_CONDITIONS (Weight: 0.3)

Evaluates weather impact on tasks.

**Clear weather**: All tasks = 1.0

**Rain**:
- craft, pathfind: 0.8
- mine, gather: 0.7
- build, place: 0.6

**Thunder**:
- craft, pathfind: 0.8
- attack: 0.5
- Others: 0.3 (dangerous)

## Usage Examples

### Basic Prioritization

```java
// Create prioritizer with default factors
TaskPrioritizer prioritizer = TaskPrioritizer.withDefaults();

// Create context from agent
DecisionContext context = DecisionContext.of(foreman, tasks);

// Prioritize task list
List<Task> prioritized = prioritizer.prioritize(tasks, context);

// Execute highest priority task
if (!prioritized.isEmpty()) {
    Task bestTask = prioritized.get(0);
    executeTask(bestTask);
}
```

### Custom Factors

```java
// Create custom factor
UtilityFactor myFactor = new UtilityFactor() {
    @Override
    public double calculate(Task task, DecisionContext context) {
        // Your custom logic here
        if (someCondition) return 1.0;
        if (otherCondition) return 0.5;
        return 0.0;
    }

    @Override
    public String getName() {
        return "my_custom_factor";
    }

    @Override
    public double getDefaultWeight() {
        return 1.5; // High importance
    }
};

// Add to prioritizer
prioritizer.addFactor(myFactor, 1.5);
```

### Adjusting Weights

```java
TaskPrioritizer prioritizer = new TaskPrioritizer();

// Safety is critical
prioritizer.addFactor(UtilityFactors.SAFETY, 2.5); // Higher than default

// Weather is less important
prioritizer.addFactor(UtilityFactors.WEATHER_CONDITIONS, 0.1); // Lower than default

// Urgency is very important
prioritizer.addFactor(UtilityFactors.URGENCY, 2.0);
```

### Examining Score Breakdown

```java
UtilityScore score = prioritizer.score(task, context);

// Check if high priority
if (score.isHighPriority()) {
    logger.info("Task is high priority!");
}

// Get detailed breakdown
System.out.println(score.toDetailedString());
// Output: UtilityScore[0.85] {urgency=0.90, safety=0.70, resource_proximity=1.00, ...}

// Access individual factor values
double safetyScore = score.getFactorValue("safety").orElse(0.5);
```

## Scoring Formula

```
finalScore = (baseValue * 0.2) + (factorAverage * 0.8)

where:
    baseValue = 0.5 (neutral starting point)
    factorAverage = sum(factor[i] * weight[i]) / totalWeight

Factors are clamped to [0.0, 1.0]
Final score is clamped to [0.0, 1.0]
```

### Example Calculation

Task: "mine 10 stone" at night, raining, low health

| Factor | Value | Weight | Contribution |
|--------|-------|--------|--------------|
| URGENCY | 0.4 | 1.8 | 0.72 |
| SAFETY | 0.3 | 2.0 | 0.60 |
| RESOURCE_PROXIMITY | 1.0 | 1.5 | 1.50 |
| EFFICIENCY | 0.6 | 1.2 | 0.72 |
| SKILL_MATCH | 0.5 | 1.0 | 0.50 |
| TOOL_READINESS | 0.9 | 0.8 | 0.72 |
| HEALTH_STATUS | 0.3 | 0.8 | 0.24 |
| HUNGER_STATUS | 0.7 | 0.7 | 0.49 |
| TIME_OF_DAY | 0.4 | 0.5 | 0.20 |
| WEATHER_CONDITIONS | 0.7 | 0.3 | 0.21 |

```
totalWeight = 1.8 + 2.0 + 1.5 + 1.2 + 1.0 + 0.8 + 0.8 + 0.7 + 0.5 + 0.3 = 10.6
weightedSum = 0.72 + 0.60 + 1.50 + 0.72 + 0.50 + 0.72 + 0.24 + 0.49 + 0.20 + 0.21 = 5.90
factorAverage = 5.90 / 10.6 = 0.557
finalScore = (0.5 * 0.2) + (0.557 * 0.8) = 0.10 + 0.446 = 0.546
```

**Result**: 0.55 (moderate priority)

## Customization

### Adding Custom Factors

```java
public class CustomFactors {
    public static final UtilityFactor COMBAT_READINESS = (task, context) -> {
        // Check if agent has weapon and armor
        boolean hasWeapon = context.getAvailableTools().stream()
            .anyMatch(tool -> tool.getItem().toString().contains("sword"));

        boolean hasArmor = context.getAvailableTools().stream()
            .anyMatch(tool -> tool.getItem().toString().contains("armor"));

        if ("attack".equals(task.getAction())) {
            if (hasWeapon && hasArmor) return 1.0;
            if (hasWeapon) return 0.7;
            return 0.3; // Attacking without weapon is bad
        }

        return 0.5; // Neutral for non-combat tasks
    };
}
```

### Context-Aware Factors

```java
UtilityFactor proximityFactor = (task, context) -> {
    if (!task.hasParameters("x", "y", "z")) {
        return 0.5; // No location specified
    }

    BlockPos target = new BlockPos(
        task.getIntParameter("x", 0),
        task.getIntParameter("y", 0),
        task.getIntParameter("z", 0)
    );

    double distance = context.getAgentPosition().distSqr(target);

    // Closer is better
    if (distance < 256) return 1.0;    // Within 16 blocks
    if (distance < 4096) return 0.7;  // Within 64 blocks
    if (distance < 16384) return 0.5; // Within 128 blocks
    return 0.3; // Far away
};
```

## Best Practices

1. **Weight Critical Factors Highly**: Safety and urgency should have 1.5-2.0x weights
2. **Use Factor Applicability**: Only apply relevant factors to specific task types
3. **Balance Subjective Factors**: Player preference should be one of many factors
4. **Test and Iterate**: Adjust weights based on observed behavior
5. **Log Score Breakdowns**: Enable debugging to understand why tasks are prioritized
6. **Consider Agent State**: Health, hunger, and inventory are important context
7. **Account for Environment**: Time of day and weather affect task desirability

## Configuration

Config file: `config/steve-common.toml`

```toml
[utility]
# Enable utility-based prioritization
enabled = true

# Factor weights (overrides defaults)
[utility.weights]
urgency = 1.8
safety = 2.0
resource_proximity = 1.5
efficiency = 1.2
skill_match = 1.0
player_preference = 1.0
tool_readiness = 0.8
health_status = 0.8
hunger_status = 0.7
time_of_day = 0.5
weather_conditions = 0.3

# Disable specific factors
[utility.disabled]
weather_conditions = false  # Disable if you don't care about weather

# Custom thresholds
[utility.thresholds]
high_priority = 0.7
low_priority = 0.3
```

## Troubleshooting

### All Tasks Have Same Score

**Symptom**: Tasks aren't being differentiated

**Solutions**:
1. Check that factors are being applied: `factor.appliesTo(task)`
2. Verify factors return varied values (not all 0.5)
3. Adjust weights to increase differentiation
4. Add more context-specific factors

### Important Tasks Not Prioritized

**Symptom**: Critical tasks getting low scores

**Solutions**:
1. Increase weight of relevant factors (e.g., URGENCY for time-sensitive)
2. Add custom factors for specific task types
3. Adjust factor calculations to return higher values
4. Check context is being populated correctly

### Too Much Priority Switching

**Symptom**: Agent constantly changing tasks

**Solutions**:
1. Add task stickiness factor (favor current task)
2. Increase task completion priority
3. Reduce factor weight variance
4. Add hysteresis to prevent rapid switching

## References

- **Inspired By**: Utility AI systems in game development
- **Related**: `UtilityFactor`, `UtilityScore`, `DecisionContext`
- **See Also**: `TaskPrioritizer`, `UtilityFactors`
