# Trigger Systems, Weight-Based Decisions, and Script Logic in Game Automation

**Date:** February 28, 2026
**Research Focus:** Historical and modern patterns for game automation decision systems
**Application:** Enhancement opportunities for Steve AI (MineWright) system

---

## Executive Summary

This document researches trigger systems, weighted decision-making (Utility AI before it was called that), and script logic patterns used in game automation. It analyzes how "dumb scripts" achieved intelligent behavior through simple state tracking, trigger chains, and priority-based fallback systems. The research concludes with recommendations for enhancing these patterns with modern LLM review while preserving their predictability and efficiency.

### Key Findings

| Pattern Type | Era | Core Mechanism | LLM Enhancement Opportunity |
|--------------|-----|----------------|---------------------------|
| **Trigger Chains** | 1990s-2000s | Event → Condition → Action | LLM validates/optimize chains |
| **Utility Scoring** | 2000s-2010s | Weighted factor sum | LLM explains choices |
| **Priority Queues** | 1990s-Present | FIFO with priority override | LLM adjusts weights |
| **Fallback Chains** | 1980s-Present | Try A, then B, then C | LLM generates fallbacks |
| **State Memory** | 1990s-Present | Variables + circular buffers | LLM interprets patterns |

---

## Table of Contents

1. [Trigger Systems](#1-trigger-systems)
2. [Weight-Based Decision Systems (Utility AI)](#2-weight-based-decision-systems-utility-ai)
3. [Script Logic Patterns](#3-script-logic-patterns)
4. [Memory in Dumb Scripts](#4-memory-in-dumb-scripts)
5. [Historical Examples](#5-historical-examples)
6. [Modern Implementations in Steve AI](#6-modern-implementations-in-steve-ai)
7. [LLM Enhancement Patterns](#7-llm-enhancement-patterns)
8. [Recommended Architecture](#8-recommended-architecture)

---

## 1. Trigger Systems

### 1.1 What Are Triggers?

Triggers are **event-condition-action (ECA)** rules that fire when specific game states occur. They're the foundation of reactive AI behavior.

**Basic Structure:**
```
WHEN <event occurs>
AND <conditions are met>
THEN <execute action>
```

### 1.2 Common Trigger Types in Game Automation

#### Health-Based Triggers

```java
// Classic MMORPG healer bot trigger (2000s era)
if (healthPercent < 30 && target == "ally") {
    if (hasSpell("Greater Heal")) {
        cast("Greater Heal", target);
    } else if (hasSpell("Heal")) {
        cast("Heal", target);
    } else {
        useItem("Minor Healing Potion", target);
    }
}
```

**Analysis:**
- **Event:** Health drops below threshold
- **Condition:** Target is ally
- **Priority Chain:** Best spell → Adequate spell → Item fallback
- **No state:** Purely reactive, no memory of previous heals

#### Enemy Distance Triggers

```java
// FPS bot proximity trigger (Quake era, 1996)
double enemyDist = getDistanceToNearestEnemy();
if (enemyDist < 10.0) {
    if (getCurrentWeapon() == "rocket_launcher") {
        fire(); // Splash damage effective at close range
    } else if (getCurrentWeapon() == "shotgun") {
        fire(); // Also good close range
    }
} else if (enemyDist < 50.0 && enemyDist >= 10.0) {
    if (getCurrentWeapon() == "railgun") {
        fire(); // Precision for medium range
    }
} else {
    // Far away - close distance
    moveTo(enemyPosition);
}
```

**Analysis:**
- **Spatial awareness:** Distance-based branching
- **Weapon selection:** Context-aware choice
- **Movement fallback:** Close distance when can't attack

#### Time-Based Triggers

```java
// RTS worker automation (StarCraft era, 1998)
int gameTime = getGameTime();
if (gameTime % 120 == 0) { // Every 2 minutes
    if (getWorkerCount() < maxWorkers) {
        trainWorker();
    }
}
if (gameTime == 300) { // At 5 minutes exactly
    if (getMinerals() > 300) {
        upgrade("base");
    }
}
```

**Analysis:**
- **Periodic triggers:** Consistent worker production
- **Timestamp triggers:** Timed upgrades
- **Resource checking:** Preconditions before action

### 1.3 Trigger Chaining

Triggers can be **chained** to create complex behaviors:

```java
// Chain: Attack → Low Health → Retreat → Heal → Return
if (inCombat && health < 20) {
    trigger("retreat_to_safety");
} else if (inSafeZone && health < 100) {
    trigger("use_healing_item");
} else if (health == 100 && wasInCombat) {
    trigger("return_to_battle");
}
```

**State tracking enables chains:**
```java
// Script memory variables (without databases)
boolean wasInCombat = false;
boolean isInCombat = false;

void onTakeDamage() {
    isInCombat = true;
    wasInCombat = true;
}

void onHealComplete() {
    if (wasInCombat) {
        trigger("return_to_battle");
        wasInCombat = false; // Reset after using
    }
}
```

### 1.4 Priority Systems for Conflicting Triggers

When multiple triggers could fire, **priority** resolves conflicts:

```java
class Trigger {
    int priority; // Higher = more important
    String condition;
    Runnable action;
}

List<Trigger> triggers = List.of(
    new Trigger(100, "enemy_attacking", this::flee),
    new Trigger(50, "health_low", this::heal),
    new Trigger(10, "idle", this::explore)
);

void tick() {
    // Find highest priority trigger that fires
    triggers.stream()
        .filter(t -> t.conditionMet())
        .max(Comparator.comparingInt(t -> t.priority))
        .ifPresent(t -> t.action.run());
}
```

**Modern implementation from Steve AI:**
```java
// From ProactiveDialogueManager.java
private double getTriggerChance(String triggerType) {
    double baseChance = 0.3;
    double rapportBonus = (rapport / 25) * 0.1;  // Higher rapport = more chatty
    double contextModifier = getContextModifier(triggerType);
    double speechPatternPenalty = getSpeechPatternPenalty(triggerType);

    return Math.max(0.1, Math.min(0.9,
        baseChance + rapportBonus + contextModifier - speechPatternPenalty
    ));
}
```

### 1.5 Trigger Cooldowns

Prevent spam with time-based cooldowns:

```java
Map<String, Long> lastTriggerTime = new HashMap<>();

boolean canTrigger(String triggerId, long cooldownMs) {
    Long lastTime = lastTriggerTime.get(triggerId);
    if (lastTime == null) {
        return true;
    }
    return (System.currentTimeMillis() - lastTime) >= cooldownMs;
}

void fireTrigger(String triggerId, Runnable action) {
    if (canTrigger(triggerId, 5000)) { // 5 second cooldown
        action.run();
        lastTriggerTime.put(triggerId, System.currentTimeMillis());
    }
}
```

---

## 2. Weight-Based Decision Systems (Utility AI)

### 2.1 What Is Utility AI?

**Utility AI** scores actions based on multiple weighted factors, selecting the highest-scoring option. This was used long before it had a formal name.

**Core Formula:**
```
Score(Action) = (Factor1 × Weight1) + (Factor2 × Weight2) + ... + (FactorN × WeightN)
                           ──────────────────────────────────────────────
                                               TotalWeight
```

### 2.2 Historical Example: Quake Bot (1996)

```java
// Pre-Utility AI era: Hardcoded priorities
String selectWeapon() {
    if (enemyDistance < 5 && hasAmmo("shotgun")) {
        return "shotgun"; // Close range
    } else if (enemyDistance > 50 && hasAmmo("railgun")) {
        return "railgun"; // Long range
    } else {
        return "blaster"; // Fallback
    }
}
```

**Problem:** Brittle, doesn't adapt to context (ammo, health, armor).

### 2.3 Early Utility Scoring (2000s Era)

```java
// Early utility scoring for weapon selection
class WeaponScore {
    double distanceScore;
    double ammoScore;
    double damageScore;
    double finalScore;
}

String selectWeaponUtility() {
    Map<String, WeaponScore> scores = new HashMap<>();

    for (String weapon : availableWeapons) {
        WeaponScore score = new WeaponScore();

        // Distance factor (closer = better for shotgun)
        score.distanceScore = calculateDistanceScore(weapon, enemyDistance);

        // Ammo factor (more ammo = better)
        score.ammoScore = getAmmoPercent(weapon) / 100.0;

        // Damage factor (higher damage = better)
        score.damageScore = getWeaponDamage(weapon) / maxDamage;

        // Weighted sum
        score.finalScore = (score.distanceScore * 0.5) +
                          (score.ammoScore * 0.3) +
                          (score.damageScore * 0.2);

        scores.put(weapon, score);
    }

    return scores.entrySet().stream()
        .max(Map.Entry.comparingDoubleByValue(e -> e.getValue().finalScore))
        .map(Map.Entry::getKey)
        .orElse("pistol");
}
```

### 2.4 Modern Utility AI Implementation (Steve AI)

From the actual Steve AI codebase:

```java
// From UtilityFactors.java
public static final UtilityFactor SAFETY = new UtilityFactor() {
    @Override
    public double calculate(Task task, DecisionContext context) {
        double score = 1.0;

        // Reduce score based on health
        double health = context.getHealthLevel();
        if (health < 0.2) score -= 0.5; // Critical health
        else if (health < 0.5) score -= 0.3; // Hurt

        // Reduce score based on nearby threats
        int threatCount = context.getNearbyThreats().size();
        if (threatCount > 5) score -= 0.4;
        else if (threatCount > 3) score -= 0.2;
        else if (threatCount > 0) score -= 0.1;

        // Combat tasks get lower safety score
        if ("attack".equals(task.getAction())) {
            score -= 0.2;
        }

        // Night time is less safe
        if (!context.isDaytime()) {
            score -= 0.1;
        }

        return Math.max(0.0, Math.min(1.0, score));
    }

    @Override
    public double getDefaultWeight() {
        return 1.5; // Safety is important
    }
};
```

**Key improvements over early utility AI:**
1. **Multiple factors**: Health, threats, task type, time of day
2. **Non-linear scoring**: Threshold-based reductions
3. **Weighted factors**: Safety gets 1.5x weight
4. **Normalized output**: Always returns 0.0-1.0

### 2.5 Response Curves

Early utility AI used simple linear scoring. Modern systems use **response curves** for more natural behavior:

```java
// Linear: Direct proportionality
double linearScore(double input) {
    return Math.max(0, Math.min(1, input));
}

// Logistic: S-curve for thresholds
double logisticScore(double input, double midpoint, double steepness) {
    return 1.0 / (1.0 + Math.exp(-steepness * (input - midpoint)));
}

// Example: Health threshold for fleeing
// Below 30% health: strong urge to flee
// Above 30% health: minimal concern
double fleeUrgency = logisticScore(healthPercent, 0.3, 10.0);

if (fleeUrgency > 0.7) {
    trigger("flee");
}
```

### 2.6 Context-Sensitive Weighting

Smart systems adjust weights based on context:

```java
// From TaskPrioritizer.java
public void addDefaultFactors() {
    // Critical factors
    addFactor(UtilityFactors.SAFETY, 2.0);     // High weight
    addFactor(UtilityFactors.URGENCY, 1.8);    // High weight

    // Situation-dependent weights
    if (isInCombat()) {
        updateFactorWeight(UtilityFactors.SAFETY, 3.0); // Even higher
        updateFactorWeight(UtilityFactors.EFFICIENCY, 0.5); // Less important
    } else if (isBuilding()) {
        updateFactorWeight(UtilityFactors.EFFICIENCY, 1.5);
        updateFactorWeight(UtilityFactors.TOOL_READINESS, 1.2);
    }
}
```

---

## 3. Script Logic Patterns

### 3.1 If-Then-Else Chains

The most basic pattern, still widely used:

```java
// Classic bot decision tree
void decide() {
    if (health < 20) {
        heal();
    } else if (enemyNearby && hasAmmo()) {
        attack();
    } else if (hasTarget()) {
        moveToTarget();
    } else if (isBored()) {
        explore();
    } else {
        idle();
    }
}
```

**Problems with deep nesting:**
```java
// Anti-pattern: Too many nested conditions
void badDecide() {
    if (condition1) {
        if (condition2) {
            if (condition3) {
                if (condition4) {
                    action();
                } else {
                    alternative();
                }
            }
        }
    }
}
```

**Better: Early returns**
```java
void goodDecide() {
    if (!condition1) return;
    if (!condition2) return;
    if (!condition3) return;
    if (!condition4) {
        alternative();
        return;
    }
    action();
}
```

### 3.2 Switch/Case State Handling

State machines use switch statements for clean state transitions:

```java
// From AgentState.java
enum AgentState {
    IDLE, PLANNING, EXECUTING, PAUSED, COMPLETED, FAILED
}

// State handling
void handleState(AgentState state) {
    switch (state) {
        case IDLE -> checkForNewTasks();
        case PLANNING -> waitForLLMResponse();
        case EXECUTING -> tickCurrentAction();
        case PAUSED -> waitForResume();
        case COMPLETED -> reportSuccess();
        case FAILED -> handleError();
    }
}
```

**Modern Java pattern matching (Java 21+):**
```java
void handleStateModern(AgentState state) {
    switch (state) {
        case IDLE -> checkForNewTasks();
        case PLANNING p -> waitForLLMResponse(p.task());
        case EXECUTING e -> tickCurrentAction(e.task());
        case PAUSED p -> waitForResume(p.reason());
        case COMPLETED c -> reportSuccess(c.result());
        case FAILED f -> handleError(f.error());
    }
}
```

### 3.3 Nested Conditions and Priority Handling

Combining conditions with priorities:

```java
// Priority-based action selection
class PrioritizedAction {
    int priority;
    BooleanSupplier condition;
    Runnable action;
}

List<PrioritizedAction> actionQueue = List.of(
    new PrioritizedAction(100, () -> health < 10, () -> flee()),
    new PrioritizedAction(90, () -> underAttack && hasWeapon(), () -> attack()),
    new PrioritizedAction(80, () -> hungry, () -> eat()),
    new PrioritizedAction(50, () -> hasTarget(), () -> pursue()),
    new PrioritizedAction(10, () -> true, () -> patrol())
);

void decide() {
    actionQueue.stream()
        .filter(a -> a.condition.getAsBoolean())
        .max(Comparator.comparingInt(a -> a.priority))
        .ifPresent(a -> a.action.run());
}
```

### 3.4 Fallback Patterns

The "Try A, if fails try B, if fails try C" pattern:

```java
// Basic fallback chain
Result executeWithFallback() {
    // Try primary method
    try {
        return methodA();
    } catch (Exception e) {
        logger.warn("Method A failed, trying B");
    }

    // Try fallback
    try {
        return methodB();
    } catch (Exception e) {
        logger.warn("Method B failed, trying C");
    }

    // Last resort
    return methodC();
}
```

**Real-world example from Steve AI:**
```java
// From ActionExecutor.java - Plugin system with legacy fallback
private BaseAction createAction(Task task) {
    String actionType = task.getAction();

    // Try registry-based creation first (plugin architecture)
    ActionRegistry registry = ActionRegistry.getInstance();
    if (registry.hasAction(actionType)) {
        BaseAction action = registry.createAction(actionType, foreman, task, actionContext);
        if (action != null) {
            return action;
        }
    }

    // Fallback to legacy switch statement
    return createActionLegacy(task);
}

@Deprecated
private BaseAction createActionLegacy(Task task) {
    return switch (task.getAction()) {
        case "pathfind" -> new PathfindAction(foreman, task);
        case "mine" -> new MineBlockAction(foreman, task);
        case "place" -> new PlaceBlockAction(foreman, task);
        // ... other cases
        default -> null;
    };
}
```

### 3.5 Graceful Degradation Pattern

When advanced features fail, fall back to basic functionality:

```java
// From FallbackResponseSystem.java
public LLMResponse generateFallback(String systemPrompt, String userPrompt, Throwable error) {
    LOGGER.warn("LLM failed, using pattern matching fallback");

    // Queue for replay when LLM recovers
    queuePendingRequest(systemPrompt, userPrompt);

    // Activate degraded mode
    enterDegradedMode();

    // Detect intent from user input
    Intent intent = detectIntent(userPrompt);

    // Generate appropriate response
    String responseContent = generateResponseForIntent(intent, userPrompt);

    return LLMResponse.builder()
        .content(responseContent)
        .model("fallback-system")
        .providerId("fallback")
        .build();
}

private Intent detectIntent(String prompt) {
    if (prompt.matches("(?i).*(build|construct|create).*")) {
        return Intent.BUILD_INTENT;
    } else if (prompt.matches("(?i).*(mine|dig|gather).*")) {
        return Intent.MINE_INTENT;
    }
    // ... more patterns
    return Intent.UNKNOWN_INTENT;
}
```

---

## 4. Memory in Dumb Scripts

### 4.1 Simple State Variables

Scripts "remember" using primitive variables:

```java
// Bot state without databases
class BotMemory {
    // Last known positions
    double lastEnemyX, lastEnemyY, lastEnemyZ;

    // Cooldowns
    long lastHealTime;
    long lastAttackTime;

    // Counters
    int deathCount;
    int killCount;

    // Flags
    boolean wasInCombat;
    boolean isRetreating;
    boolean hasSeenEnemy;
}
```

### 4.2 Circular Buffers for Recent Events

Track recent events without growing memory:

```java
class CircularBuffer<T> {
    private final Object[] buffer;
    private int head = 0;
    private int size = 0;

    public CircularBuffer(int capacity) {
        this.buffer = new Object[capacity];
    }

    public void add(T item) {
        buffer[head] = item;
        head = (head + 1) % buffer.length;
        if (size < buffer.length) size++;
    }

    public List<T> getLast(int n) {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < Math.min(n, size); i++) {
            int idx = (head - 1 - i + buffer.length) % buffer.length;
            result.add((T) buffer[idx]);
        }
        return result;
    }
}

// Usage: Track recent damage sources
CircularBuffer<String> recentAttackers = new CircularBuffer<>(10);

void onAttacked(String attacker) {
    recentAttackers.add(attacker);

    // Check if same attacker hit us recently
    if (recentAttackers.getLast(5).stream()
        .filter(attacker::equals)
        .count() >= 3) {
        // Being focused by same enemy - prioritize defense
        trigger("evasive_manuevers");
    }
}
```

### 4.3 Pattern Recognition in Fixed Code

Detect patterns by comparing current state to history:

```java
class PatternDetector {
    private final CircularBuffer<Double> healthHistory = new CircularBuffer<>(100);
    private final CircularBuffer<String> actionHistory = new CircularBuffer<>(50);

    public void update(double health, String action) {
        healthHistory.add(health);
        actionHistory.add(action);

        detectPatterns();
    }

    private void detectPatterns() {
        // Pattern: Taking rapid damage
        if (isHealthDroppingRapidly()) {
            trigger("under_attack");
        }

        // Pattern: Same action failing repeatedly
        if (isActionFailingRepeatedly()) {
            trigger("change_strategy");
        }

        // Pattern: Time to restock
        if (isLowOnSupplies()) {
            trigger("return_to_base");
        }
    }

    private boolean isHealthDroppingRapidly() {
        List<Double> recent = healthHistory.getLast(10);
        if (recent.size() < 10) return false;

        // Check if health dropped by 20+ in last 10 ticks
        double drop = recent.get(0) - recent.get(9);
        return drop > 20.0;
    }

    private boolean isActionFailingRepeatedly() {
        List<String> recent = actionHistory.getLast(5);
        if (recent.size() < 5) return false;

        // Check if last 5 actions were all failures
        return recent.stream().allMatch(a -> a.startsWith("fail_"));
    }
}
```

### 4.4 Memory Categories

Different types of memory for different purposes:

```java
class BotMemory {
    // SHORT-TERM: Current state
    double currentHealth;
    String currentTarget;
    Weapon currentWeapon;

    // MEDIUM-TERM: Recent history (circular buffers)
    CircularBuffer<Position> recentPositions = new CircularBuffer<>(100);
    CircularBuffer<String> recentActions = new CircularBuffer<>(50);

    // LONG-TERM: Persistent counters
    Map<String, Integer> killCounts = new HashMap<>();
    Map<String, Integer> deathCounts = new HashMap<>();

    // STRATEGIC: Goals and plans
    List<String> currentGoals = new ArrayList<>();
    String currentStrategy;

    // EPISODIC: Memorable events
    List<MemorableEvent> importantEvents = new ArrayList<>();

    void recordMemorableEvent(String event, double importance) {
        importantEvents.add(new MemorableEvent(event, importance));

        // Keep only top 100 most important
        if (importantEvents.size() > 100) {
            importantEvents.sort((a, b) -> Double.compare(b.importance, a.importance));
            importantEvents = importantEvents.subList(0, 100);
        }
    }
}
```

---

## 5. Historical Examples

### 5.1 Diablo I (1996) - Simple State Machine

```java
// Simplified Diablo monster AI
enum MonsterState {
    IDLE, CHASE, ATTACK, RETREAT
}

class MonsterAI {
    MonsterState state = MonsterState.IDLE;

    void tick() {
        switch (state) {
            case IDLE:
                if (canSeePlayer() && getDistance() < 20) {
                    state = MonsterState.CHASE;
                }
                break;

            case CHASE:
                if (getDistance() < 2) {
                    state = MonsterState.ATTACK;
                } else if (health < 10) {
                    state = MonsterState.RETREAT;
                } else {
                    moveTo(player);
                }
                break;

            case ATTACK:
                attack(player);
                if (health < 10) {
                    state = MonsterState.RETREAT;
                } else if (getDistance() > 5) {
                    state = MonsterState.CHASE;
                }
                break;

            case RETREAT:
                moveAwayFrom(player);
                if (health > 50) {
                    state = MonsterState.CHASE;
                }
                break;
        }
    }
}
```

### 5.2 StarCraft (1998) - Resource Worker Triggers

```java
// StarCraft worker automation (early bot)
class WorkerAI {
    enum WorkerTask {
        MINING, GATHERING, BUILDING, IDLE
    }

    WorkerTask task = WorkerTask.IDLE;
    Unit currentTarget;

    void tick() {
        switch (task) {
            case IDLE:
                // Find nearest mineral patch
                currentTarget = findNearestMineralPatch();
                if (currentTarget != null) {
                    task = WorkerTask.MINING;
                    rightClick(currentTarget);
                }
                break;

            case MINING:
                // Check if carrying minerals
                if (isCarryingResources()) {
                    // Return to base
                    currentTarget = getNearestCommandCenter();
                    rightClick(currentTarget);
                    task = WorkerTask.GATHERING;
                }
                // Check if mineral patch depleted
                else if (!currentTarget.exists()) {
                    task = WorkerTask.IDLE;
                }
                break;

            case GATHERING:
                // Check if delivered resources
                if (!isCarryingResources()) {
                    task = WorkerTask.IDLE;
                }
                break;
        }
    }
}
```

### 5.3 Counter-Strike (2000) - Waypoint Navigation

```cpp
// Counter-Strike bot waypoint system
class WaypointBot {
    vector<Vector> waypoints;
    int currentWaypoint = 0;

    void navigate() {
        if (waypoints.empty()) return;

        Vector target = waypoints[currentWaypoint];
        float distance = (position - target).length();

        if (distance < 50.0f) {
            // Reached waypoint, move to next
            currentWaypoint = (currentWaypoint + 1) % waypoints.size();
        } else {
            // Move toward waypoint
            moveToward(target);
        }
    }

    void onEnemySeen(Vector enemyPos) {
        // Interrupt navigation to attack
        attack(enemyPos);

        // Remember enemy location
        lastEnemyPosition = enemyPos;
        lastEnemyTime = getGameTime();
    }
};
```

### 5.4 World of Warcraft (2005) - Rotation Bot

```lua
-- WoW bot rotation (Lua script)
local lastCastTime = 0
local gcd = 1.5 -- Global cooldown

function tick()
    local now = GetTime()

    -- Check GCD
    if now - lastCastTime < gcd then
        return
    end

    -- Priority rotation
    if UnitHealth("player") / UnitHealthMax("player") < 0.3 then
        CastSpellByName("Healing Potion")
        lastCastTime = now
    elseif UnitMana("player") > 40 and UnitExists("target") then
        CastSpellByName("Fireball")
        lastCastTime = now
    elseif UnitMana("player") < 20 then
        CastSpellByName("Drink")
        lastCastTime = now
    end
end
```

**Analysis:**
- **Priority queue**: Health > Combat > Mana
- **Cooldown tracking**: GCD prevents spam
- **Resource checks**: Mana before casting
- **No memory**: Purely reactive

---

## 6. Modern Implementations in Steve AI

### 6.1 State Machine with Event Publishing

From `AgentStateMachine.java`:

```java
public class AgentStateMachine {
    private final AtomicReference<AgentState> currentState;
    private final EventBus eventBus;

    // Valid transitions defined statically
    private static final Map<AgentState, Set<AgentState>> VALID_TRANSITIONS = Map.of(
        AgentState.IDLE, Set.of(AgentState.PLANNING),
        AgentState.PLANNING, Set.of(AgentState.EXECUTING, AgentState.FAILED),
        AgentState.EXECUTING, Set.of(AgentState.COMPLETED, AgentState.FAILED, AgentState.PAUSED),
        // ...
    );

    public boolean transitionTo(AgentState targetState, String reason) {
        AgentState fromState = currentState.get();

        // Validate transition
        if (!canTransitionTo(targetState)) {
            LOGGER.warn("Invalid transition: {} → {}", fromState, targetState);
            return false;
        }

        // Atomic transition
        if (currentState.compareAndSet(fromState, targetState)) {
            // Publish event for observers
            eventBus.publish(new StateTransitionEvent(agentId, fromState, targetState, reason));
            return true;
        }
        return false;
    }
}
```

**Improvements over historical:**
1. **Thread-safe**: AtomicReference for concurrent access
2. **Event-driven**: Observers subscribe to state changes
3. **Validated**: Invalid transitions rejected
4. **Reason tracking**: Why did transition happen?

### 6.2 Utility Factors with Context

From `UtilityFactors.java`:

```java
public static final UtilityFactor HUNGER_STATUS = new UtilityFactor() {
    @Override
    public double calculate(Task task, DecisionContext context) {
        double hunger = context.getHungerLevel();

        // Special case: Food tasks get priority boost when hungry
        String action = task.getAction();
        boolean isFoodTask = action.equals("gather") &&
            task.getStringParameter("resource", "").toLowerCase().contains("food");

        if (isFoodTask && hunger < 0.5) {
            return 1.0; // Very important to get food
        }

        // Normal hunger scoring
        if (hunger > 0.8) return 1.0;  // Well fed
        if (hunger > 0.5) return 0.7;  // Moderate hunger
        if (hunger > 0.2) return 0.5;  // Hungry
        return 0.3;  // Starving
    }
};
```

**Key insights:**
- **Context-aware**: Same task scored differently based on agent state
- **Priority boost**: Food gathering becomes critical when hungry
- **Normalized**: Always returns 0.0-1.0

### 6.3 Proactive Dialogue with Triggers

From `ProactiveDialogueManager.java`:

```java
// Trigger types with cooldowns
private final Map<String, Long> triggerCooldowns = new HashMap<>();

public void tick() {
    if (ticksSinceLastCheck < baseCheckInterval) {
        return; // Only check periodically
    }
    ticksSinceLastCheck = 0;

    // Check important triggers only
    checkContextBasedTriggers();  // Danger warnings
    checkActionStateTriggers();   // Task completion
}

private void checkContextBasedTriggers() {
    // Low health trigger
    if (minewright.getHealth() < minewright.getMaxHealth() * 0.3) {
        if (canTrigger("low_health", 2000)) {
            triggerComment("low_health", "Health is low!");
        }
    }

    // Biome triggers
    String biome = getBiome();
    if (biome.contains("nether")) {
        if (canTrigger("nether_biome", 3000)) {
            triggerComment("nether_biome", "Entered the Nether");
        }
    }
}

private boolean canTrigger(String triggerType, int cooldownTicks) {
    Long lastTrigger = triggerCooldowns.get(triggerType);
    if (lastTrigger == null) return true;

    long ticksSinceTrigger = ticksSinceLastComment;
    return ticksSinceTrigger >= cooldownTicks;
}
```

**Pattern advantages:**
1. **Reduced chatter**: Cooldowns prevent spam
2. **Context-aware**: Different responses for different situations
3. **Relationship-scaled**: Higher rapport = more dialogue
4. **Priority-aware**: Important triggers (danger) bypass cooldowns

### 6.4 Interceptor Chain Pattern

From `ActionExecutor.java`:

```java
public class ActionExecutor {
    private final InterceptorChain interceptorChain;

    public ActionExecutor(ForemanEntity foreman) {
        this.interceptorChain = new InterceptorChain();

        // Setup interceptors (chain of responsibility)
        interceptorChain.addInterceptor(new LoggingInterceptor());
        interceptorChain.addInterceptor(new MetricsInterceptor());
        interceptorChain.addInterceptor(new EventPublishingInterceptor(eventBus, foreman.getEntityName()));
    }

    private void executeTask(Task task) {
        BaseAction action = createAction(task);

        // Execute through interceptor chain
        interceptorChain.execute(action, () -> {
            action.start();
            while (!action.isComplete()) {
                action.tick();
            }
            return action.getResult();
        });
    }
}
```

**Pattern benefits:**
1. **Separation of concerns**: Logging, metrics, events separated
2. **Extensible**: Add interceptors without changing action code
3. **Consistent**: All actions go through same pipeline

---

## 7. LLM Enhancement Patterns

### 7.1 LLM as Pattern Validator

Traditional triggers + LLM validation for optimization:

```java
class EnhancedTriggerSystem {
    private final LLMClient llmClient;
    private final List<Trigger> triggers;

    public void evaluateTriggers() {
        List<Trigger> fired = new ArrayList<>();

        // Find all triggers that fire
        for (Trigger trigger : triggers) {
            if (trigger.conditionMet()) {
                fired.add(trigger);
            }
        }

        // If multiple triggers conflict, ask LLM for guidance
        if (fired.size() > 1) {
            Trigger chosen = resolveConflictWithLLM(fired);
            chosen.execute();
        } else if (!fired.isEmpty()) {
            fired.get(0).execute();
        }
    }

    private Trigger resolveConflictWithLLM(List<Trigger> conflicted) {
        String prompt = buildConflictPrompt(conflicted);
        LLMResponse response = llmClient.complete(prompt);

        // Parse LLM recommendation
        String chosenId = parseTriggerId(response.getText());
        return conflicted.stream()
            .filter(t -> t.getId().equals(chosenId))
            .findFirst()
            .orElse(conflicted.get(0)); // Fallback
    }

    private String buildConflictPrompt(List<Trigger> triggers) {
        StringBuilder sb = new StringBuilder();
        sb.append("Multiple triggers fired. Choose the best action:\n");

        for (Trigger t : triggers) {
            sb.append(String.format("- %s: %s (priority: %d)\n",
                t.getId(), t.getDescription(), t.getPriority()));
        }

        sb.append("\nCurrent context:\n");
        sb.append(String.format("- Health: %.1f%%\n", healthPercent));
        sb.append(String.format("- Location: %s\n", location));
        sb.append(String.format("- Nearby enemies: %d\n", enemyCount));

        sb.append("\nRespond with only the trigger ID.");
        return sb.toString();
    }
}
```

### 7.2 LLM as Explainer

Let LLM explain why a decision was made (for debugging/teaching):

```java
class ExplainableUtilityAI {
    private final TaskPrioritizer prioritizer;
    private final LLMClient llmClient;

    public Decision selectWithExplanation(List<Task> options, DecisionContext context) {
        // Score all options
        List<ScoredOption> scored = new ArrayList<>();
        for (Task option : options) {
            UtilityScore score = prioritizer.score(option, context);
            scored.add(new ScoredOption(option, score));
        }

        // Sort by score
        scored.sort(Comparator.comparingDouble(s -> -s.score.finalScore()));

        ScoredOption chosen = scored.get(0);

        // Generate explanation
        String explanation = generateExplanation(chosen, scored);

        return new Decision(chosen.task, chosen.score, explanation);
    }

    private String generateExplanation(ScoredOption chosen, List<ScoredOption> allOptions) {
        StringBuilder sb = new StringBuilder();
        sb.append("I chose to ").append(chosen.task.getAction()).append(" because:\n\n");

        // Explain factors
        for (Map.Entry<String, Double> factor : chosen.score.factors().entrySet()) {
            String name = factor.getKey();
            double value = factor.getValue();

            String interpretation = interpretFactor(name, value);
            sb.append(String.format("- %s: %.2f (%s)\n", name, value, interpretation));
        }

        sb.append("\nWhy not alternatives?\n");
        for (int i = 1; i < Math.min(3, allOptions.size()); i++) {
            ScoredOption alternative = allOptions.get(i);
            sb.append(String.format("- %s: Score %.2f (lower priority)\n",
                alternative.task.getAction(), alternative.score.finalScore()));
        }

        return sb.toString();
    }

    private String interpretFactor(String factorName, double value) {
        // Use LLM to generate human-readable interpretation
        String prompt = String.format(
            "The AI factor '%s' has a value of %.2f (0.0 = bad, 1.0 = good). " +
            "Explain what this means in one sentence:",
            factorName, value);

        return llmClient.complete(prompt).getText();
    }
}
```

### 7.3 LLM for Trigger Generation

LLM generates new triggers based on observed patterns:

```java
class TriggerLearner {
    private final LLMClient llmClient;
    private final List<TriggerExecution> history = new ArrayList<>();

    public void recordExecution(Trigger trigger, boolean success, double utility) {
        history.add(new TriggerExecution(trigger, success, utility, System.currentTimeMillis()));

        // Learn from failed triggers
        if (!success && history.size() > 100) {
            suggestImprovements(trigger);
        }
    }

    private void suggestImprovements(Trigger failedTrigger) {
        // Get recent history
        List<TriggerExecution> recent = history.stream()
            .filter(e -> System.currentTimeMillis() - e.timestamp() < 300000) // Last 5 minutes
            .toList();

        String prompt = buildLearningPrompt(failedTrigger, recent);
        LLMResponse response = llmClient.complete(prompt);

        // Parse suggestions
        List<TriggerSuggestion> suggestions = parseSuggestions(response.getText());

        LOGGER.info("LLM suggested improvements for trigger {}:", failedTrigger.getId());
        for (TriggerSuggestion suggestion : suggestions) {
            LOGGER.info("- {}", suggestion.description());
        }
    }

    private String buildLearningPrompt(Trigger failedTrigger, List<TriggerExecution> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("Trigger '").append(failedTrigger.getId()).append("' failed.\n");
        sb.append("Condition: ").append(failedTrigger.getCondition()).append("\n");
        sb.append("Action: ").append(failedTrigger.getAction()).append("\n\n");

        sb.append("Recent execution history:\n");
        for (TriggerExecution exec : history) {
            sb.append(String.format("- %s: success=%s, utility=%.2f\n",
                exec.trigger().getId(), exec.success(), exec.utility()));
        }

        sb.append("\nSuggest improvements to this trigger. Consider:\n");
        sb.append("1. Additional preconditions\n");
        sb.append("2. Timing/cooldown adjustments\n");
        sb.append("3. Alternative actions\n");
        sb.append("4. Context checks\n");

        return sb.toString();
    }
}
```

### 7.4 LLM for Fallback Response Generation

When complex logic fails, LLM generates appropriate fallback:

```java
class SmartFallbackSystem {
    private final LLMClient llmClient;
    private final Map<Intent, String[]> templateResponses;

    public String generateResponse(Intent intent, Map<String, Object> context) {
        // Try template first
        String[] templates = templateResponses.get(intent);
        if (templates != null && templates.length > 0) {
            // Use template with some variability
            return templates[random.nextInt(templates.length)];
        }

        // No template - ask LLM for help
        return llmGenerateFallback(intent, context);
    }

    private String llmGenerateFallback(Intent intent, Map<String, Object> context) {
        String prompt = String.format("""
            Generate a brief, natural response for situation:

            Intent: %s
            Context: %s

            Requirements:
            - Max 10 words
            - Natural language
            - In character (helpful AI assistant)
            - No emojis
            """,
            intent,
            context
        );

        LLMResponse response = llmClient.complete(prompt);
        return response.getText();
    }
}
```

---

## 8. Recommended Architecture

### 8.1 Hybrid Architecture: Fast Scripts + Smart LLM

```
┌─────────────────────────────────────────────────────────────────┐
│                    FAST PATH (99% of decisions)                 │
│  - Deterministic triggers                                       │
│  - Utility scoring (microseconds)                               │
│  - State machine transitions (nanoseconds)                      │
│  - No network calls                                             │
└─────────────────────────────────────────────────────────────────┘
                               ↓
                    Decision confidence low?
                               ↓
┌─────────────────────────────────────────────────────────────────┐
│                    SLOW PATH (1% of decisions)                  │
│  - LLM consultation (seconds)                                   │
│  - Complex reasoning                                            │
│  - Novel situations                                             │
│  - Learning/optimization                                        │
└─────────────────────────────────────────────────────────────────┘
```

### 8.2 Implementation Template

```java
class HybridDecisionEngine {
    // Fast path: Pre-compiled triggers
    private final List<Trigger> fastTriggers = List.of(
        new HealthTrigger(),
        new CombatTrigger(),
        new ResourceTrigger()
    );

    // Slow path: LLM consultation
    private final LLMClient llmClient;

    public Decision decide(Context context) {
        // Try fast triggers first
        List<Decision> options = new ArrayList<>();
        for (Trigger trigger : fastTriggers) {
            if (trigger.applies(context)) {
                double score = trigger.score(context);
                options.add(new Decision(trigger.getAction(), score));
            }
        }

        // If clear winner, use it
        if (options.size() == 1) {
            return options.get(0);
        }

        if (options.size() > 1) {
            // Multiple options - check confidence
            options.sort(Comparator.comparingDouble(d -> -d.score()));

            double bestScore = options.get(0).score();
            double secondScore = options.get(1).score();

            // If best is significantly better, use it
            if (bestScore - secondScore > 0.3) {
                return options.get(0);
            }

            // Scores too close - consult LLM
            return consultLLM(options, context);
        }

        // No triggers matched - use LLM
        return consultLLM(Collections.emptyList(), context);
    }

    private Decision consultLLM(List<Decision> options, Context context) {
        String prompt = buildDecisionPrompt(options, context);
        LLMResponse response = llmClient.complete(prompt);

        // Parse LLM response
        String chosenAction = extractAction(response.getText());
        String reasoning = extractReasoning(response.getText());

        return new Decision(chosenAction, reasoning);
    }
}
```

### 8.3 Pattern: Confidence Threshold

Decide when to use LLM based on confidence:

```java
class ConfidenceBasedDecision {
    private static final double CONFIDENCE_THRESHOLD = 0.7;

    public Decision decide(List<Option> options) {
        if (options.isEmpty()) {
            return fallbackToLLM("No options available");
        }

        // Sort by score
        options.sort(Comparator.comparingDouble(o -> -o.score()));

        Option best = options.get(0);

        // High confidence - use best option
        if (best.score() >= CONFIDENCE_THRESHOLD) {
            return new Decision(best.action(), "High confidence: " + best.score(),
                             DecisionSource.FAST_TRIGGER);
        }

        // Low confidence - ask LLM
        return consultLLM(options);
    }

    private Decision consultLLM(List<Option> options) {
        // Present top options to LLM with context
        String prompt = buildPrompt(options);
        LLMResponse response = llmClient.complete(prompt);

        return new Decision(
            parseAction(response),
            "LLM decision based on " + options.size() + " options",
            DecisionSource.LLM_CONSULTATION
        );
    }
}
```

### 8.4 Pattern: Adaptive Weight Tuning

LLM suggests weight adjustments based on outcomes:

```java
class AdaptiveWeightSystem {
    private final Map<String, Double> weights = new HashMap<>();
    private final List<DecisionOutcome> history = new ArrayList<>();

    public void recordOutcome(Decision decision, boolean success, double utility) {
        history.add(new DecisionOutcome(decision, success, utility));

        // Periodically review and adjust
        if (history.size() % 100 == 0) {
            reviewAndAdjustWeights();
        }
    }

    private void reviewAndAdjustWeights() {
        // Analyze recent history
        String prompt = buildAnalysisPrompt(history);

        LLMResponse response = llmClient.complete(prompt);
        Map<String, Double> suggestedWeights = parseWeightSuggestions(response.getText());

        // Apply suggestions with validation
        for (Map.Entry<String, Double> entry : suggestedWeights.entrySet()) {
            double oldWeight = weights.getOrDefault(entry.getKey(), 1.0);
            double newWeight = entry.getValue();

            // Validate: Don't allow extreme changes
            if (Math.abs(newWeight - oldWeight) < oldWeight * 0.5) {
                weights.put(entry.getKey(), newWeight);
                LOGGER.info("Adjusted weight {} from {} to {}",
                    entry.getKey(), oldWeight, newWeight);
            }
        }
    }

    private String buildAnalysisPrompt(List<DecisionOutcome> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyze the last 100 decisions and suggest weight adjustments.\n\n");
        sb.append("Current weights:\n");
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            sb.append(String.format("- %s: %.2f\n", entry.getKey(), entry.getValue()));
        }

        sb.append("\nRecent outcomes:\n");
        for (DecisionOutcome outcome : history) {
            sb.append(String.format("- %s: success=%s, utility=%.2f, factors=%s\n",
                outcome.decision().action(),
                outcome.success(),
                outcome.utility(),
                outcome.decision().factors()));
        }

        sb.append("\nSuggest weight adjustments to improve future decisions.");
        sb.append("Respond with JSON: {\"factor\": newWeight, ...}");

        return sb.toString();
    }
}
```

---

## Conclusion

### Key Takeaways

1. **Triggers remain fundamental**: Event-condition-action rules are still the backbone of reactive AI
2. **Utility AI is mature**: Weighted scoring systems have evolved from simple sums to sophisticated multi-factor analysis
3. **Memory doesn't require databases**: Circular buffers and state variables suffice for most bot memory needs
4. **Fallback chains are essential**: "Try A, then B, then C" pattern prevents system failures
5. **LLMs enhance, not replace**: The best systems use fast deterministic logic with LLM consultation for edge cases

### Recommendations for Steve AI

1. **Implement confidence thresholds**: Use LLM only when confidence is low
2. **Add weight learning**: Periodically review decisions and adjust utility weights
3. **Trigger learning**: Use LLM to suggest new triggers based on failure patterns
4. **Explainable AI**: Use LLM to explain why decisions were made (debugging/teaching)
5. **Graceful degradation**: Maintain full functionality when LLM is unavailable

### Historical Pattern → Modern Enhancement

| Historical Pattern | Modern Enhancement | Steve AI Implementation |
|-------------------|-------------------|------------------------|
| Fixed trigger conditions | LLM-suggested conditions | Dynamic trigger generation |
| Static weights | Adaptive weight learning | Periodic LLM weight review |
| Simple priority | Utility scoring | TaskPrioritizer class |
| Hardcoded fallbacks | LLM-generated fallbacks | FallbackResponseSystem |
| No explanations | LLM explanations | DecisionExplanation class |

---

**Document Version:** 1.0
**Last Updated:** February 28, 2026
**Author:** Research Agent
**Project:** Steve AI (MineWright) Decision System Enhancement
