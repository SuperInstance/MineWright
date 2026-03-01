# Script System Improvement Plan: Cross-Game Bot Research Synthesis

**Document Version:** 1.0
**Date:** 2026-03-01
**Purpose:** Comprehensive improvement plan for Steve AI's Script DSL system based on analysis of 5 game bot research documents
**Status:** Research & Planning

---

## Executive Summary

This document synthesizes insights from five comprehensive research documents on game automation architectures:

1. **WoW Glider Analysis** - Memory-based state extraction, FSM combat systems, humanization techniques
2. **Honorbuddy Analysis** - Plugin architecture, behavior trees, XML profiles, navigation meshes
3. **Minecraft Bot Analysis** - Baritone's goal composition, Mineflayer's async patterns
4. **Humanization Techniques** - Timing randomization, mistake simulation, behavioral noise
5. **Multi-Game Bot Patterns** - Cross-game patterns, error recovery, scripting DSLs

**Key Finding:** Steve AI's Script DSL has a solid foundation (behavior trees, triggers, actions) but is missing critical patterns that have proven essential across 30 years of game automation: **goal composition**, **process arbitration**, **random event handling**, **graceful degradation**, and **humanization**.

**Impact Assessment:**
- **High Impact (Adopt Immediately):** Goal composition, stuck detection, retry with backoff
- **Medium Impact (Short-term):** Process arbitration, random events, humanization
- **Strategic Impact (Long-term):** DSL language, item rules engine, skill learning

---

## Table of Contents

1. [Cross-Cutting Pattern Synthesis](#cross-cutting-pattern-synthesis)
2. [Gap Analysis: Current vs. Target](#gap-analysis-current-vs-target)
3. [Prioritized Improvement Roadmap](#prioritized-improvement-roadmap)
4. [Detailed Implementation Specifications](#detailed-implementation-specifications)
5. [Integration with Existing Systems](#integration-with-existing-systems)
6. [Testing Strategy](#testing-strategy)
7. [Success Metrics](#success-metrics)

---

## 1. Cross-Cutting Pattern Synthesis

### 1.1 Universal Patterns (Present in All Successful Bots)

| Pattern | Found In | Purpose | Steve AI Status |
|---------|----------|---------|-----------------|
| **Hierarchical State Management** | All bots | Manage complexity through layering | ✅ Implemented (AgentStateMachine) |
| **Event-Driven Reactivity** | MUD, OSRS, GW2 | Respond to game events efficiently | ✅ Implemented (EventBus) |
| **Rule-Based Decisions** | Diablo, WoW | Declarative item/target selection | ❌ Missing (item rules, targeting) |
| **Error Recovery** | EVE, Diablo, OSRS | Retry, rollback, degrade gracefully | ⚠️ Partial (RetryPolicy exists, not wired) |
| **Stuck Detection** | Onmyoji, game automation | Detect and recover from failure states | ❌ Missing |
| **Behavior Trees** | Honorbuddy, modern games | Modular reactive behaviors | ✅ Implemented (BTNode hierarchy) |
| **Plugin Architecture** | Honorbuddy, WoW Glider | Extensible without core changes | ⚠️ Partial (ActionRegistry, no hot-load) |

### 1.2 Pattern Categories

#### **Category A: Decision Architecture**
- **Goal Composition** (Baritone) - Hierarchical targeting with any-satisfies logic
- **Process Arbitration** (Baritone) - Independent behaviors request control instead of seizing
- **Utility Scoring** (Diablo, WoW) - Weighted criteria for action selection
- **Priority Queues** (EVE, WoW) - Interruptible task ordering

#### **Category B: Execution Reliability**
- **Stuck Detection** (Universal) - Position/state-based timeout detection
- **Retry with Backoff** (Network/Automation) - Exponential delay retries
- **Graceful Degradation** (EVE) - Reduce functionality instead of failing
- **State Rollback** (Transaction systems) - Undo to last known good state

#### **Category C: Humanization**
- **Timing Randomization** (All bots) - Gaussian jitter on delays
- **Mistake Simulation** (OSRS, WoW) - Intentional 2-5% error rate
- **Behavioral Noise** (WoW, OSRS) - Micro-movements, idle actions
- **Session Modeling** (WoW) - Warm-up, fatigue, break patterns

#### **Category D: Knowledge Management**
- **Blackboard Pattern** (BT systems) - Shared state across behaviors
- **Item Rules Engine** (Diablo) - Declarative item filtering
- **Random Event Handlers** (OSRS) - First-class interrupt system
- **Persistent Context** (All long-running bots) - State checkpointing

### 1.3 Minecraft-Specific Patterns (from Baritone & Mineflayer)

| Pattern | Source | Purpose | Applicability |
|---------|--------|---------|---------------|
| **GoalComposite** | Baritone | Navigate to ANY of multiple targets | Mining: go to nearest diamond |
| **Segmented Pathfinding** | Baritone | Long-distance path calculation | Handle >1000 block paths |
| **Async Task Composition** | Mineflayer | Non-blocking LLM integration | LLM→Script generation |
| **Path Events** | Baritone | Monitor navigation state | Debug, trigger on events |
| **Process Control** | Baritone | Prevent behavior conflicts | Mining vs. combat arbitration |

---

## 2. Gap Analysis: Current vs. Target

### 2.1 Current Script DSL Capabilities

**Implemented Strengths:**
```java
// Script structure
Script {
  metadata: ScriptMetadata
  parameters: Map<String, Parameter>
  requirements: ScriptRequirements
  scriptNode: ScriptNode (root of BT)
  errorHandlers: Map<FailureType, List<ScriptNode>>
  telemetry: ScriptTelemetry
}

// Behavior Tree nodes (src/main/java/com/minewright/behavior/)
- BTNode (abstract base)
- Composite: SequenceNode, SelectorNode, ParallelNode
- Decorator: InverterNode, RepeaterNode, CooldownNode
- Leaf: ActionNode, ConditionNode
- BTBlackboard (shared state)
```

**Trigger System:**
```java
// Script triggers
- EVENT: Fired on game event
- CONDITION: Evaluated continuously
- TIME: Fired at intervals
- PLAYER_ACTION: Fired on player action
```

**Missing Critical Patterns:**

| Missing Pattern | Impact | Effort | Priority |
|----------------|--------|--------|----------|
| Goal composition for navigation | High | Medium | P1 |
| Stuck detection/recovery | High | Low | P1 |
| Retry with exponential backoff | High | Low | P1 |
| Process arbitration (behavior conflicts) | High | Medium | P2 |
| Random event interrupt system | Medium | Medium | P2 |
| Item rules engine (pickit-style) | Medium | Medium | P2 |
| Humanization (timing, mistakes) | Medium | Low | P2 |
| Script DSL language (text format) | High | High | P3 |
| Skill learning loop | High | High | P3 |

### 2.2 Specific Gaps by Research Document

#### **From WoW Glider:**
- ❌ No fatigue modeling (session phases)
- ❌ No mistake simulation (intentional errors)
- ❌ Fixed action delays (no Gaussian jitter)
- ❌ No multi-target priority scoring
- ⚠️ Stuck detection exists in pathfinding but not general

#### **From Honorbuddy:**
- ❌ No plugin hot-loading system
- ❌ No XML/JSON profile format for scripts
- ❌ No combat routine priority system
- ❌ No quest chain management
- ✅ Behavior trees already implemented

#### **From Baritone/Mineflayer:**
- ❌ No goal composition (GoalComposite missing)
- ❌ No segmented pathfinding for long distances
- ❌ No async task composition for LLM calls
- ❌ No path event system
- ⚠️ Hierarchical pathfinding exists but not segmented

#### **From Humanization Techniques:**
- ❌ No timing randomization (fixed delays)
- ❌ No movement variation (speed, micro-movements)
- ❌ No mistake simulation
- ❌ No idle behaviors
- ❌ No session/fatigue modeling

#### **From Multi-Game Patterns:**
- ❌ No item rules engine (pickit-style)
- ❌ No random event handlers (first-class)
- ❌ No graceful degradation levels
- ❌ No state rollback mechanism
- ⚠️ ErrorRecoveryStrategy exists but not integrated

---

## 3. Prioritized Improvement Roadmap

### Phase 1: Critical Reliability (Weeks 1-2)
**Goal:** Eliminate common failure modes that cause frustration

| Feature | Source | Effort | Impact | Description |
|---------|--------|--------|--------|-------------|
| **Stuck Detection** | Multi-Game, Glider | 2 days | High | Detect position/state timeout, trigger recovery |
| **Retry with Backoff** | Multi-Game, EVE | 1 day | High | Exponential delay for LLM/API failures |
| **State Rollback** | Multi-Game | 2 days | High | Undo to last known good state on failure |

**Implementation Order:**
1. StuckDetector class (position-based, 5 second threshold)
2. Wire RetryPolicy into ActionExecutor
3. Add checkpoint system to AgentStateMachine

### Phase 2: Decision Architecture (Weeks 3-4)
**Goal:** Enable intelligent behavior selection and conflict resolution

| Feature | Source | Effort | Impact | Description |
|---------|--------|--------|--------|-------------|
| **Goal Composition** | Baritone | 3 days | High | CompositeNavigationGoal for flexible targeting |
| **Process Arbitration** | Baritone | 3 days | High | BehaviorProcess with desire-based selection |
| **Utility Scoring** | Diablo, WoW | 2 days | Medium | Weighted action selection |

**Implementation Order:**
1. NavigationGoal interface + CompositeNavigationGoal
2. BehaviorProcess interface + ProcessManager
3. UtilityDecisionMaker for task prioritization

### Phase 3: Humanization (Weeks 5-6)
**Goal:** Make agents feel more natural and characterful

| Feature | Source | Effort | Impact | Description |
|---------|--------|--------|--------|-------------|
| **Timing Randomization** | All bots | 1 day | Medium | Gaussian jitter on action delays |
| **Mistake Simulation** | OSRS, WoW | 2 days | Medium | 2-3% intentional error rate |
| **Idle Behaviors** | Humanization | 2 days | Low | Personality-driven idle actions |
| **Session Modeling** | WoW Glider | 2 days | Low | Warm-up, fatigue phases |

**Implementation Order:**
1. JitterUtils for Gaussian noise
2. MistakeSimulator for intentional errors
3. IdleBehaviorController with personality
4. SessionManager for fatigue tracking

### Phase 4: Knowledge Management (Weeks 7-8)
**Goal:** Enable declarative configuration and smart resource management

| Feature | Source | Effort | Impact | Description |
|---------|--------|--------|--------|-------------|
| **Item Rules Engine** | Diablo | 3 days | Medium | Pickit-style item filtering |
| **Random Event Handlers** | OSRS | 3 days | Medium | First-class interrupt system |
| **Blackboard Enhancement** | Multi-Game | 2 days | Medium | Zone-specific shared state |

**Implementation Order:**
1. ItemRule interface + ItemRuleEngine
2. RandomEventSystem with interrupt capability
3. Enhanced BTBlackboard with zone awareness

### Phase 5: Script Language (Weeks 9-12)
**Goal:** Enable human-readable script definitions

| Feature | Source | Effort | Impact | Description |
|---------|--------|--------|--------|-------------|
| **Text DSL Format** | MUD, Razor | 5 days | High | JSON/YAML script definitions |
| **DSL Parser** | MUD, TinTin++ | 3 days | High | Parse DSL to Script objects |
| **Script Templates** | Honorbuddy | 2 days | Medium | Reusable script patterns |
| **LLM→Script Generator** | Mineflayer | 5 days | High | Generate DSL from natural language |

**Implementation Order:**
1. Define DSL JSON schema
2. DSLParser to convert JSON to Script
3. ScriptTemplate library
4. ScriptGenerator integration with LLM

---

## 4. Detailed Implementation Specifications

### 4.1 P1: Stuck Detection System

**Pattern Source:** Multi-Game Bot Patterns, OnmyojiAutoScript

**Specification:**
```java
// Location: src/main/java/com/minewright/behavior/StuckDetector.java
public class StuckDetector {
    private final Random random = new Random();
    private Position lastPosition;
    private int stuckTicks = 0;
    private static final int STUCK_THRESHOLD = 100; // 5 seconds at 20 TPS

    public enum StuckType {
        POSITION_UNCHANGED,  // Not moving
        STATE_STALLED,       // Same state too long
        PROGRESS_HALTED      // Quest/task progress stalled
    }

    public void update(Position currentPosition) {
        if (currentPosition.equals(lastPosition)) {
            stuckTicks++;
            if (stuckTicks > STUCK_THRESHOLD) {
                throw new StuckException(StuckType.POSITION_UNCHANGED,
                    "Agent hasn't moved for 5 seconds");
            }
        } else {
            stuckTicks = 0;
            lastPosition = currentPosition;
        }
    }

    public void triggerRecovery() {
        // Recovery strategies in order:
        // 1. Try random movement
        // 2. Try jumping
        // 3. Repath to destination
        // 4. Return to last known good position
        // 5. Request human intervention
    }
}

// Integration: src/main/java/com/minewright/execution/AgentStateMachine.java
public class AgentStateMachine {
    private final StuckDetector stuckDetector = new StuckDetector();

    public void tick() {
        // Update stuck detector every tick
        stuckDetector.update(foreman.position());

        try {
            currentState.tick();
        } catch (StuckException e) {
            handleStuck(e);
        }
    }

    private void handleStuck(StuckException e) {
        switch (e.getStuckType()) {
            case POSITION_UNCHANGED:
                currentState.transitionTo(AgentState.RECOVERING,
                    "Stuck: attempting recovery");
                break;
        }
    }
}
```

**Testing:**
```java
@Test
public void testStuckDetection() {
    StuckDetector detector = new StuckDetector();
    Position pos = new Position(0, 64, 0);

    // Simulate 101 ticks of no movement
    for (int i = 0; i < 101; i++) {
        detector.update(pos);
    }

    assertThrows(StuckException.class, () -> detector.update(pos));
}
```

### 4.2 P1: Retry with Exponential Backoff

**Pattern Source:** Multi-Game Bot Patterns, EVE Online

**Specification:**
```java
// Location: src/main/java/com/minewright/execution/RetryPolicy.java (already exists)
// Enhance to add exponential backoff
public class RetryPolicy {
    private final int maxAttempts;
    private final long baseDelay; // milliseconds
    private final long maxDelay;
    private final Random random = new Random();

    public RetryPolicy(int maxAttempts, long baseDelay, long maxDelay) {
        this.maxAttempts = maxAttempts;
        this.baseDelay = baseDelay;
        this.maxDelay = maxDelay;
    }

    public long getDelay(int attempt) {
        // Exponential backoff with jitter
        long exponentialDelay = (long) (baseDelay * Math.pow(2, attempt));
        long delay = Math.min(exponentialDelay, maxDelay);

        // Add ±20% jitter
        double jitter = 0.8 + (random.nextDouble() * 0.4);
        return (long) (delay * jitter);
    }

    public <T> T execute(Callable<T> operation) throws Exception {
        Exception lastException = null;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                return operation.call();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxAttempts - 1) {
                    long delay = getDelay(attempt);
                    Thread.sleep(delay);
                }
            }
        }

        throw new RuntimeException("Max retries exceeded", lastException);
    }
}

// Usage: src/main/java/com/minewright/llm/TaskPlanner.java
public class TaskPlanner {
    private final RetryPolicy llmRetryPolicy = new RetryPolicy(3, 1000, 30000);

    public ResponseParser.ParsedResponse planTasks(ForemanEntity foreman, String command) {
        return llmRetryPolicy.execute(() -> {
            return llmClient.generateSync(buildPrompt(foreman, command));
        });
    }
}
```

**Configuration:**
```toml
# config/minewright-common.toml
[retry]
max_attempts = 3
base_delay_ms = 1000
max_delay_ms = 30000
```

### 4.3 P1: Goal Composition System

**Pattern Source:** Baritone GoalComposite

**Specification:**
```java
// Location: src/main/java/com/minewright/script/navigation/NavigationGoal.java (new)
public interface NavigationGoal {
    /**
     * Check if position satisfies this goal
     */
    boolean isSatisfied(BlockPos position);

    /**
     * Calculate heuristic cost to reach this goal
     */
    double heuristic(BlockPos position);
}

// Location: src/main/java/com/minewright/script/navigation/CompositeNavigationGoal.java
public class CompositeNavigationGoal implements NavigationGoal {
    private final List<NavigationGoal> goals;

    public CompositeNavigationGoal(List<NavigationGoal> goals) {
        this.goals = new ArrayList<>(goals);
    }

    @Override
    public boolean isSatisfied(BlockPos position) {
        // ANY goal satisfies
        return goals.stream().anyMatch(g -> g.isSatisfied(position));
    }

    @Override
    public double heuristic(BlockPos position) {
        // Minimum heuristic among all goals
        return goals.stream()
            .mapToDouble(g -> g.heuristic(position))
            .min()
            .orElse(Double.MAX_VALUE);
    }
}

// Example goals
public class BlockGoal implements NavigationGoal {
    private final BlockPos target;
    private final double tolerance;

    public BlockGoal(BlockPos target, double tolerance) {
        this.target = target;
        this.tolerance = tolerance;
    }

    @Override
    public boolean isSatisfied(BlockPos position) {
        return position.distSqr(target) <= tolerance * tolerance;
    }

    @Override
    public double heuristic(BlockPos position) {
        return Math.sqrt(position.distSqr(target));
    }
}

public class NearGoal implements NavigationGoal {
    private final BlockPos center;
    private final double radius;

    public NearGoal(BlockPos center, double radius) {
        this.center = center;
        this.radius = radius;
    }

    @Override
    public boolean isSatisfied(BlockPos position) {
        return position.distSqr(center) <= radius * radius;
    }

    @Override
    public double heuristic(BlockPos position) {
        return Math.max(0, Math.sqrt(position.distSqr(center)) - radius);
    }
}

// Integration: src/main/java/com/minewright/script/ScriptDSL.java
public enum AtomicCommand {
    GOTO("goto", "Navigate to a goal"),
    // ... existing commands
}

// Script execution
if (command == AtomicCommand.GOTO) {
    NavigationGoal goal = parseGoal(parameters);
    // pathfinder.navigateTo(goal);
}

// Example script usage
NavigationGoal goal = new CompositeNavigationGoal(Arrays.asList(
    new BlockGoal(new BlockPos(100, 64, 200), 2.0),
    new BlockGoal(new BlockPos(150, 64, 250), 2.0),
    new BlockGoal(new BlockPos(120, 64, 180), 2.0)
));
// Agent will navigate to NEAREST of the three targets
```

**Use Cases:**
- Mining: Go to nearest diamond ore
- Building: Go to any of multiple build sites
- Gathering: Go to nearest tree type

### 4.4 P2: Process Arbitration System

**Pattern Source:** Baritone Process-Based Architecture

**Specification:**
```java
// Location: src/main/java/com/minewright/behavior/BehaviorProcess.java (new)
public interface BehaviorProcess {
    /**
     * Returns desire level (0-1) if this process wants control.
     * Returns Optional.empty() if not interested.
     */
    Optional<Double> getDesire(ExecutionContext context);

    /**
     * Called when this process is granted control.
     */
    void onControlGranted(ExecutionContext context);

    /**
     * Called when this process loses control.
     */
    void onControlRevoked(ExecutionContext context);
}

// Location: src/main/java/com/minewright/behavior/ProcessManager.java (new)
public class ProcessManager {
    private final List<BehaviorProcess> processes = new ArrayList<>();
    private BehaviorProcess currentProcess;

    public void registerProcess(BehaviorProcess process) {
        processes.add(process);
    }

    public void tick(ExecutionContext context) {
        // Find process with highest desire
        BehaviorProcess selected = null;
        double maxDesire = 0.0;

        for (BehaviorProcess process : processes) {
            Optional<Double> desire = process.getDesire(context);
            if (desire.isPresent() && desire.get() > maxDesire) {
                maxDesire = desire.get();
                selected = process;
            }
        }

        // Switch processes if needed
        if (selected != null && selected != currentProcess) {
            if (currentProcess != null) {
                currentProcess.onControlRevoked(context);
            }
            currentProcess = selected;
            currentProcess.onControlGranted(context);
        }

        // Execute current process
        if (currentProcess != null) {
            // Execution happens through process's onControlGranted
        }
    }
}

// Example processes
public class SurvivalProcess implements BehaviorProcess {
    @Override
    public Optional<Double> getDesire(ExecutionContext context) {
        double healthPercent = context.getHealth() / context.getMaxHealth();

        if (healthPercent < 0.2) {
            return Optional.of(1.0); // Maximum desire - survive!
        } else if (healthPercent < 0.5) {
            return Optional.of(0.8); // High desire - cautious
        } else {
            return Optional.empty(); // No interest - healthy
        }
    }

    @Override
    public void onControlGranted(ExecutionContext context) {
        // Flee to safety, eat food, etc.
        context.transitionTo(AgentState.FLEEING, "Low health");
    }

    @Override
    public void onControlRevoked(ExecutionContext context) {
        // Clean up fleeing behavior
    }
}

public class TaskProcess implements BehaviorProcess {
    private final TaskQueue taskQueue;

    @Override
    public Optional<Double> getDesire(ExecutionContext context) {
        if (taskQueue.isEmpty()) {
            return Optional.empty();
        }

        // Base desire from having tasks
        double desire = 0.5;

        // Increase if tasks are urgent
        if (context.hasUrgentTasks()) {
            desire = 0.9;
        }

        return Optional.of(desire);
    }

    @Override
    public void onControlGranted(ExecutionContext context) {
        // Continue with task queue
        context.transitionTo(AgentState.EXECUTING, "Working on tasks");
    }
}

public class IdleProcess implements BehaviorProcess {
    @Override
    public Optional<Double> getDesire(Optional<Double> getDesire(ExecutionContext context) {
        // Always has low desire - engages when nothing else wants control
        return Optional.of(0.1);
    }

    @Override
    public void onControlGranted(ExecutionContext context) {
        // Follow player, look around, etc.
        context.transitionTo(AgentState.IDLE, "Nothing to do");
    }
}

// Integration: src/main/java/com/minewright/execution/ActionExecutor.java
public class ActionExecutor {
    private final ProcessManager processManager = new ProcessManager();

    public void initializeProcesses() {
        processManager.registerProcess(new SurvivalProcess());
        processManager.registerProcess(new TaskProcess(taskQueue));
        processManager.registerProcess(new IdleProcess());
    }

    @Override
    public void tick() {
        processManager.tick(context);
        // ... rest of tick logic
    }
}
```

**Benefits:**
- Prevents behavior conflicts (mining vs. fleeing)
- Priority-based execution (survival > tasks > idle)
- Easy to add new behaviors
- Clear separation of concerns

### 4.5 P2: Random Event Interrupt System

**Pattern Source:** OSRS Random Event Solvers

**Specification:**
```java
// Location: src/main/java/com/minewright/behavior/RandomEventSystem.java (new)
public class RandomEventSystem {
    private final List<RandomEventHandler> handlers = new ArrayList<>();
    private Task originalTask;
    private boolean handlingEvent = false;

    public void registerHandler(RandomEventHandler handler) {
        handlers.add(handler);
    }

    public void onGameEvent(GameEvent event) {
        // Check if any handler recognizes this event
        for (RandomEventHandler handler : handlers) {
            if (handler.detects(event)) {
                interruptCurrentTask(handler, event);
                return;
            }
        }
    }

    private void interruptCurrentTask(RandomEventHandler handler, GameEvent event) {
        if (!handlingEvent) {
            originalTask = actionExecutor.getCurrentTask();
            handlingEvent = true;
        }

        // Handle the event
        handler.handle(event);

        // Resume original task after event complete
        if (handler.isComplete(event)) {
            if (originalTask != null) {
                actionExecutor.enqueueTask(originalTask);
            }
            handlingEvent = false;
        }
    }
}

public interface RandomEventHandler {
    /**
     * Check if this handler recognizes the event
     */
    boolean detects(GameEvent event);

    /**
     * Handle the event
     */
    void handle(GameEvent event);

    /**
     * Check if event handling is complete
     */
    boolean isComplete(GameEvent event);
}

// Example handlers
public class ZombieSiegeHandler implements RandomEventHandler {
    @Override
    public boolean detects(GameEvent event) {
        return event instanceof ZombieEvent;
    }

    @Override
    public void handle(GameEvent event) {
        // Run to safe location
        // Equip weapon
        // Wait for siege to end
    }

    @Override
    public boolean isComplete(GameEvent event) {
        ZombieEvent zombie = (ZombieEvent) event;
        return zombie.isOver();
    }
}

public class LowHealthHandler implements RandomEventHandler {
    @Override
    public boolean detects(GameEvent event) {
        if (event instanceof HealthUpdateEvent) {
            HealthUpdateEvent health = (HealthUpdateEvent) event;
            return health.getHealthPercent() < 0.2;
        }
        return false;
    }

    @Override
    public void handle(GameEvent event) {
        // Eat food
        // Retreat to safety
    }

    @Override
    public boolean isComplete(GameEvent event) {
        HealthUpdateEvent health = (HealthUpdateEvent) event;
        return health.getHealthPercent() > 0.5;
    }
}

// Integration: src/main/java/com/minewright/execution/AgentStateMachine.java
public class AgentStateMachine {
    private final RandomEventSystem randomEventSystem = new RandomEventSystem();

    public void initialize() {
        randomEventSystem.registerHandler(new ZombieSiegeHandler());
        randomEventSystem.registerHandler(new LowHealthHandler());
        randomEventSystem.registerHandler(new InventoryFullHandler());
    }

    public void onEvent(GameEvent event) {
        randomEventSystem.onGameEvent(event);
    }
}
```

### 4.6 P2: Item Rules Engine (Pickit-Style)

**Pattern Source:** Diablo Demonbuddy NIP Files

**Specification:**
```java
// Location: src/main/java/com/minewright/behavior/ItemRule.java (new)
public interface ItemRule {
    boolean matches(ItemStack item);
    ItemAction getAction(ItemStack item);
}

public enum ItemAction {
    KEEP,       // Put in inventory/keep slot
    SELL,       // Sell to vendor
    DROP,       // Drop on ground
    STASH,      // Put in storage chest
    CRAFT       // Use for crafting
}

// Location: src/main/java/com/minewright/behavior/ItemRuleEngine.java (new)
public class ItemRuleEngine {
    private final List<ItemRule> rules = new ArrayList<>();

    public void addRule(ItemRule rule) {
        rules.add(rule);
    }

    public ItemAction decide(ItemStack item) {
        for (ItemRule rule : rules) {
            if (rule.matches(item)) {
                return rule.getAction(item);
            }
        }
        return ItemAction.DROP; // Default
    }

    public void loadFromConfig(Config config) {
        // Load rules from config file
        List<ItemRule> configRules = config.getItemRules();
        rules.addAll(configRules);
    }
}

// Example rule implementations
public class TypeRule implements ItemRule {
    private final Set<ItemType> types;
    private final ItemAction action;

    public TypeRule(Set<ItemType> types, ItemAction action) {
        this.types = types;
        this.action = action;
    }

    @Override
    public boolean matches(ItemStack item) {
        return types.contains(item.getType());
    }

    @Override
    public ItemAction getAction(ItemStack item) {
        return action;
    }
}

public class QualityRule implements ItemRule {
    private final ItemQuality minQuality;
    private final ItemAction action;

    public QualityRule(ItemQuality minQuality, ItemAction action) {
        this.minQuality = minQuality;
        this.action = action;
    }

    @Override
    public boolean matches(ItemStack item) {
        return item.getQuality().ordinal() >= minQuality.ordinal();
    }

    @Override
    public ItemAction getAction(ItemStack item) {
        return action;
    }
}

public class CountRule implements ItemRule {
    private final int maxCount;
    private final ItemAction thenAction;
    private final ItemAction elseAction;

    public CountRule(int maxCount, ItemAction thenAction, ItemAction elseAction) {
        this.maxCount = maxCount;
        this.thenAction = thenAction;
        this.elseAction = elseAction;
    }

    @Override
    public boolean matches(ItemStack item) {
        // Matches all, but action depends on count
        return true;
    }

    @Override
    public ItemAction getAction(ItemStack item) {
        return item.getCount() >= maxCount ? thenAction : elseAction;
    }
}

// Configuration format
# config/minewright-common.toml
[[item_rules]]
type = ["diamond", "iron_ingot", "gold_ingot"]
action = "KEEP"

[[item_rules]]
type = ["dirt", "cobblestone"]
action = "DROP"

[[item_rules]]
type = ["oak_log"]
max_count = 64
action = "KEEP"
overflow_action = "STASH"
```

### 4.7 P3: Script DSL Language

**Pattern Source:** MUD Clients (TinTin++), Razor, Honorbuddy XML

**Specification:**
```json
// Script DSL JSON Schema
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Steve AI Script DSL",
  "type": "object",
  "properties": {
    "name": { "type": "string" },
    "description": { "type": "string" },
    "version": { "type": "string" },
    "parameters": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name": { "type": "string" },
          "type": { "enum": ["string", "number", "boolean", "position"] },
          "default": {},
          "required": { "type": "boolean" }
        }
      }
    },
    "triggers": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "type": { "enum": ["event", "condition", "time", "player_action"] },
          "condition": { "type": "string" },
          "interval_ticks": { "type": "number" }
        }
      }
    },
    "behavior": {
      "type": "object",
      "properties": {
        "type": {
          "enum": ["sequence", "selector", "parallel", "action", "condition", "loop", "repeat_until"]
        },
        "children": { "type": "array" },  // For composite nodes
        "action": { "type": "string" },   // For action nodes
        "condition": { "type": "string" }, // For condition nodes
        "count": { "type": "number" }     // For loop nodes
      }
    }
  }
}

// Example script: Mining automation
{
  "name": "Iron Mining",
  "description": "Automatically mine iron ore in area",
  "version": "1.0",
  "parameters": [
    {
      "name": "radius",
      "type": "number",
      "default": 32,
      "required": false
    },
    {
      "name": "keep_inventory_space",
      "type": "number",
      "default": 5,
      "required": false
    }
  ],
  "triggers": [
    {
      "type": "player_action",
      "condition": "command starts with 'mine'"
    }
  ],
  "behavior": {
    "type": "sequence",
    "children": [
      {
        "type": "action",
        "action": "find_nearest_ore",
        "parameters": {
          "ore_type": "iron_ore",
          "radius": "${radius}"
        }
      },
      {
        "type": "condition",
        "condition": "ore_found != null",
        "children": [
          {
            "type": "action",
            "action": "pathfind_to",
            "parameters": {
              "target": "${ore_found}"
            }
          },
          {
            "type": "action",
            "action": "mine_block",
            "parameters": {
              "target": "${ore_found}"
            }
          }
        ]
      },
      {
        "type": "condition",
        "condition": "inventory_free < ${keep_inventory_space}",
        "children": [
          {
            "type": "action",
            "action": "deposit_items",
            "parameters": {
              "chest": "nearest_storage"
            }
          }
        ]
      }
    ]
  }
}

// Parser implementation
// Location: src/main/java/com/minewright/script/DSLParser.java (new)
public class DSLParser {
    public Script parse(File scriptFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(scriptFile);

        Script script = new Script();
        script.setMetadata(parseMetadata(root));
        script.setParameters(parseParameters(root));
        script.setTriggers(parseTriggers(root));
        script.setScriptNode(parseBehavior(root.get("behavior")));

        return script;
    }

    private ScriptMetadata parseMetadata(JsonNode root) {
        ScriptMetadata metadata = new ScriptMetadata();
        metadata.setName(root.path("name").asText());
        metadata.setDescription(root.path("description").asText());
        metadata.setVersion(root.path("version").asText());
        return metadata;
    }

    private Map<String, Parameter> parseParameters(JsonNode root) {
        Map<String, Parameter> params = new HashMap<>();
        JsonNode paramsNode = root.path("parameters");

        for (JsonNode paramNode : paramsNode) {
            Parameter param = new Parameter();
            param.setName(paramNode.path("name").asText());
            param.setType(parseType(paramNode.path("type").asText()));
            param.setDefaultValue(parseValue(paramNode.path("default")));
            param.setRequired(paramNode.path("required").asBoolean(false));
            params.put(param.getName(), param);
        }

        return params;
    }

    private ScriptNode parseBehavior(JsonNode behaviorNode) {
        String type = behaviorNode.path("type").asText();

        switch (type) {
            case "sequence":
                return parseSequence(behaviorNode);
            case "selector":
                return parseSelector(behaviorNode);
            case "parallel":
                return parseParallel(behaviorNode);
            case "action":
                return parseAction(behaviorNode);
            case "condition":
                return parseCondition(behaviorNode);
            case "loop":
                return parseLoop(behaviorNode);
            default:
                throw new IllegalArgumentException("Unknown node type: " + type);
        }
    }

    private ScriptNode parseSequence(JsonNode node) {
        SequenceNode sequence = new SequenceNode();
        for (JsonNode child : node.path("children")) {
            sequence.addChild(parseBehavior(child));
        }
        return sequence;
    }

    // ... other parse methods

    public static void main(String[] args) throws IOException {
        DSLParser parser = new DSLParser();
        Script script = parser.parse(new File("scripts/mining_iron.json"));
        ScriptRegistry.register(script.getName(), script);
    }
}
```

**LLM Integration:**
```java
// Location: src/main/java/com/minewright/script/ScriptGenerator.java (enhance)
public class ScriptGenerator {
    private final DSLParser dslParser = new DSLParser();

    public Script generateFromNaturalLanguage(String command) {
        // Step 1: Generate DSL JSON using LLM
        String dslJson = llmClient.generate("""
            Convert this command to Steve AI Script DSL JSON:
            Command: {command}

            Return ONLY valid JSON matching the schema.
        """);

        // Step 2: Parse JSON to Script
        try {
            JsonNode json = new ObjectMapper().readTree(dslJson);
            return dslParser.parseBehavior(json.path("behavior"));
        } catch (Exception e) {
            LOGGER.error("Failed to parse generated DSL", e);
            return getDefaultScript();
        }
    }
}
```

### 4.8 P2: Humanization System

**Pattern Source:** All 5 research documents

**Specification:**
```java
// Location: src/main/java/com/minewright/behavior/humanization/JitterUtils.java (new)
public class JitterUtils {
    private static final Random random = new Random();

    /**
     * Add Gaussian noise to a value
     * @param value Base value
     * @param stdDevFraction Standard deviation as fraction of value (e.g., 0.3 = ±30%)
     * @return Value with noise
     */
    public static double addGaussianJitter(double value, double stdDevFraction) {
        double jitter = random.nextGaussian() * (value * stdDevFraction);
        return value + jitter;
    }

    /**
     * Add Gaussian noise and clamp to range
     */
    public static int addGaussianJitter(int value, double stdDevFraction, int min, int max) {
        double jittered = addGaussianJitter(value, stdDevFraction);
        return (int) Math.max(min, Math.min(max, jittered));
    }
}

// Location: src/main/java/com/minewright/behavior/humanization/MistakeSimulator.java (new)
public class MistakeSimulator {
    private static final double BASE_ERROR_RATE = 0.03; // 3%
    private final Random random = new Random();

    public boolean shouldMakeMistake() {
        return random.nextDouble() < BASE_ERROR_RATE;
    }

    public BlockPos getMistakenTarget(BlockPos intended) {
        Direction[] directions = Direction.values();
        Direction randomDir = directions[random.nextInt(directions.length)];
        return intended.relative(randomDir);
    }

    public int getMistakenDelay() {
        // Occasional "spacing out" - extra delay
        return 10 + random.nextInt(20); // 0.5-1.5 seconds extra
    }
}

// Location: src/main/java/com/minewright/behavior/humanization/SessionManager.java (new)
public class SessionManager {
    private final long sessionStartTime;
    private long lastBreakTime;

    public enum SessionPhase {
        WARMUP,      // First 10 minutes
        PERFORMANCE, // Main gameplay
        FATIGUE      // After 2 hours
    }

    public SessionManager() {
        this.sessionStartTime = System.currentTimeMillis();
        this.lastBreakTime = sessionStartTime;
    }

    public SessionPhase getCurrentPhase() {
        long elapsed = System.currentTimeMillis() - sessionStartTime;

        if (elapsed < 10 * 60 * 1000) { // 10 minutes
            return SessionPhase.WARMUP;
        } else if (elapsed < 60 * 60 * 1000) { // 1 hour
            return SessionPhase.PERFORMANCE;
        } else {
            return SessionPhase.FATIGUE;
        }
    }

    public double getReactionMultiplier() {
        return switch (getCurrentPhase()) {
            case WARMUP -> 1.3;    // 30% slower
            case PERFORMANCE -> 1.0; // Normal
            case FATIGUE -> 1.5;    // 50% slower
        };
    }

    public double getErrorMultiplier() {
        return switch (getCurrentPhase()) {
            case WARMUP -> 1.5;     // 50% more mistakes
            case PERFORMANCE -> 1.0;
            case FATIGUE -> 2.0;     // 2x mistakes
        };
    }

    public boolean shouldTakeBreak() {
        long timeSinceBreak = System.currentTimeMillis() - lastBreakTime;
        return timeSinceBreak > 30 * 60 * 1000 && random.nextDouble() < 0.1;
    }
}

// Integration: src/main/java/com/minewright/execution/ActionExecutor.java
public class ActionExecutor {
    private final MistakeSimulator mistakeSim = new MistakeSimulator();
    private final SessionManager sessionManager = new SessionManager();

    private int getActionDelayTicks() {
        int baseDelay = MineWrightConfig.ACTION_TICK_DELAY.get();

        // Apply session phase multiplier
        double sessionMultiplier = sessionManager.getReactionMultiplier();
        int adjustedDelay = (int) (baseDelay * sessionMultiplier);

        // Add Gaussian jitter: ±30% variance
        return JitterUtils.addGaussianJitter(adjustedDelay, 0.3, 2, adjustedDelay * 2);
    }

    protected void onTick() {
        // Check for break
        if (sessionManager.shouldTakeBreak()) {
            stateMachine.transitionTo(AgentState.PAUSED, "Taking a short break");
            scheduleResume(2 * 60 * 1000); // 2 minutes
            return;
        }

        // Check for mistakes
        if (mistakeSim.shouldMakeMistake()) {
            handleMistake();
        }

        // ... normal tick logic
    }
}
```

**Configuration:**
```toml
# config/minewright-common.toml
[humanization]
enabled = true

# Timing randomization
timing_variance = 0.3  # 30% variance
min_action_delay_ticks = 2
max_action_delay_ticks = 20

# Mistakes
mistake_rate = 0.03  # 3%

# Session modeling
warmup_duration_minutes = 10
fatigue_start_minutes = 60
break_interval_minutes = 30
```

---

## 5. Integration with Existing Systems

### 5.1 Script DSL Integration

The Script DSL system (`src/main/java/com/minewright/script/`) is well-designed and needs enhancement, not replacement.

**Enhancement Points:**

| Existing Component | Enhancement Required |
|-------------------|---------------------|
| `ScriptNode` | Add `AsyncActionNode` for LLM calls |
| `ScriptExecution` | Add stuck detection to execution loop |
| `Action` | Add `GOTO` action for NavigationGoal |
| `Trigger` | Add `RANDOM_EVENT` trigger type |
| `ScriptCache` | Cache compiled DSL scripts |

**New Components to Add:**
```
src/main/java/com/minewright/script/
├── navigation/
│   ├── NavigationGoal.java (interface)
│   ├── CompositeNavigationGoal.java
│   ├── BlockGoal.java
│   └── NearGoal.java
├── rules/
│   ├── ItemRule.java (interface)
│   ├── ItemRuleEngine.java
│   ├── TypeRule.java
│   └── QualityRule.java
├── dsl/
│   ├── DSLParser.java
│   ├── DSLSchema.java
│   └── ScriptGenerator.java (enhanced)
└── humanization/
    ├── JitterUtils.java
    ├── MistakeSimulator.java
    └── SessionManager.java
```

### 5.2 Behavior Tree Integration

The behavior tree implementation (`src/main/java/com/minewright/behavior/`) is complete and well-structured.

**Enhancement Points:**

| Existing Component | Enhancement Required |
|-------------------|---------------------|
| `BTBlackboard` | Add zone-specific state, resource tracking |
| `SequenceNode` | Add timeout per child (stuck detection) |
| `SelectorNode` | Add utility scoring support |
| `ActionNode` | Add mistake simulation wrapper |

**Integration Example:**
```java
// Enhance BTBlackboard with zone awareness
public class BTBlackboard {
    // Existing: Map<String, Object> data

    // New: Zone-specific state
    public void recordResourceLocation(String zone, String type, Position pos) {
        String key = "zone_" + zone + "_resource_" + type;
        put(key, pos);
    }

    public Position getLastResourceLocation(String zone, String type) {
        String key = "zone_" + zone + "_resource_" + type;
        return get(key, Position.class);
    }

    // New: Threat assessment
    public void setThreatLevel(ThreatLevel level) {
        put("current_threat", level);
    }

    public ThreatLevel getThreatLevel() {
        return getOrDefault("current_threat", ThreatLevel.NONE);
    }
}

// Wrap action nodes with mistake simulation
public class HumanizedActionNode extends ActionNode {
    private final ActionNode wrapped;
    private final MistakeSimulator mistakeSim = new MistakeSimulator();

    @Override
    public NodeStatus execute(BTBlackboard blackboard) {
        // Check for mistake
        if (mistakeSim.shouldMakeMistake()) {
            // Simulate mistake
            BlockPos mistakeTarget = mistakeSim.getMistakenTarget(getTarget());
            blackboard.put("target", mistakeTarget);
        }

        return wrapped.execute(blackboard);
    }
}
```

### 5.3 Execution System Integration

The execution system (`src/main/java/com/minewright/execution/`) has interceptor chains that are perfect for cross-cutting concerns.

**Enhancement Points:**

| Existing Component | Enhancement Required |
|-------------------|---------------------|
| `ActionExecutor` | Add ProcessManager, apply timing jitter |
| `AgentStateMachine` | Add stuck detection, state rollback |
| `InterceptorChain` | Add HumanizationInterceptor |
| `RetryPolicy` | Wire into LLM clients |

**Integration Example:**
```java
// New interceptor for humanization
public class HumanizationInterceptor implements ActionInterceptor {
    private final SessionManager sessionManager;
    private final MistakeSimulator mistakeSim;

    @Override
    public ActionResult intercept(ActionResult result, ActionContext context) {
        // Apply session effects
        double errorMultiplier = sessionManager.getErrorMultiplier();

        // Check for mistake
        if (mistakeSim.shouldMakeMistake() * errorMultiplier > random.nextDouble()) {
            return ActionResult.failure("Simulated human error");
        }

        // Apply timing jitter
        int baseDelay = context.getDelay();
        int jitteredDelay = JitterUtils.addGaussianJitter(baseDelay, 0.3, 2, baseDelay * 2);
        context.setDelay(jitteredDelay);

        return result;
    }
}

// Wire into InterceptorChain
public class InterceptorChain {
    private final List<ActionInterceptor> interceptors = Arrays.asList(
        new LoggingInterceptor(),
        new HumanizationInterceptor(),  // NEW
        new MetricsInterceptor(),
        new EventPublishingInterceptor()
    );
}
```

### 5.4 Pathfinding Integration

The pathfinding system exists (HierarchicalPathfinder, PathExecutor) but lacks some Baritone optimizations.

**Enhancement Points:**

| Existing Component | Enhancement Required |
|-------------------|---------------------|
| `HierarchicalPathfinder` | Add segmented calculation |
| `PathExecutor` | Add stuck detection, path events |
| `MovementValidator` | Add micro-movements |

**Integration Example:**
```java
// Enhance PathExecutor with events
public class PathExecutor {
    public enum PathEvent {
        CALCULATION_STARTED,
        CALCULATION_FINISHED,
        CALCULATION_FAILED,
        PATH_EXECUTED,
        BLOCK_STUCK,
        GOAL_REACHED,
        PATH_INTERRUPTED
    }

    private final List<PathEventListener> listeners = new ArrayList<>();

    public void addListener(PathEventListener listener) {
        listeners.add(listener);
    }

    private void emitEvent(PathEvent eventType, Path path) {
        PathEvent event = new PathEvent(eventType, path, System.currentTimeMillis());
        listeners.forEach(l -> l.onPathEvent(event));
    }

    // Call emitEvent at appropriate points in pathfinding
}

// Enhance with stuck detection
public class PathExecutor {
    private final StuckDetector stuckDetector = new StuckDetector();

    public void tick() {
        Position current = foreman.position();
        stuckDetector.update(current);

        try {
            // ... path following logic
        } catch (StuckException e) {
            emitEvent(PathEvent.BLOCK_STUCK, currentPath);
            handleStuck(e);
        }
    }

    private void handleStuck(StuckException e) {
        // Recovery strategies:
        // 1. Try jumping
        // 2. Try random direction
        // 3. Recalculate path
        // 4. Return to last known good position
    }
}
```

---

## 6. Testing Strategy

### 6.1 Unit Testing

**New Test Classes Required:**
```
src/test/java/com/minewright/behavior/
├── StuckDetectorTest.java
├── RetryPolicyTest.java
├── NavigationGoalTest.java
├── CompositeNavigationGoalTest.java
├── ProcessManagerTest.java
├── ItemRuleEngineTest.java
├── DSLParserTest.java
├── JitterUtilsTest.java
├── MistakeSimulatorTest.java
└── SessionManagerTest.java
```

**Example Test:**
```java
// src/test/java/com/minewright/behavior/StuckDetectorTest.java
public class StuckDetectorTest {
    @Test
    public void testStuckDetectionAfterThreshold() {
        StuckDetector detector = new StuckDetector();
        Position pos = new Position(0, 64, 0);

        // Simulate 100 ticks of no movement (should not trigger)
        for (int i = 0; i < 100; i++) {
            detector.update(pos);
        }

        // 101st tick should trigger
        assertThrows(StuckException.class, () -> detector.update(pos));
    }

    @Test
    public void testStuckDetectorResetsOnMovement() {
        StuckDetector detector = new StuckDetector();
        Position pos1 = new Position(0, 64, 0);
        Position pos2 = new Position(1, 64, 0);

        // 50 ticks without movement
        for (int i = 0; i < 50; i++) {
            detector.update(pos1);
        }

        // Movement resets counter
        detector.update(pos2);

        // Another 50 ticks without movement should not trigger
        for (int i = 0; i < 100; i++) {
            detector.update(pos2);
        }

        // Should not throw - counter was reset
        assertDoesNotThrow(() -> detector.update(pos2));
    }
}
```

### 6.2 Integration Testing

**Test Scenarios:**
1. **Stuck Recovery:** Agent gets stuck, recovers, resumes task
2. **Retry with Backoff:** LLM fails, retries with delays, succeeds
3. **Goal Composition:** Agent navigates to nearest of 3 targets
4. **Process Arbitration:** Survival interrupts mining, resumes after safe
5. **Random Events:** Zombie siege interrupts mining, resumes after
6. **Item Rules:** Agent keeps diamonds, drops dirt, stashes excess logs

**Example Integration Test:**
```java
// src/test/java/com/minewright/integration/ProcessArbitrationTest.java
public class ProcessArbitrationTest {
    @Test
    public void testSurvivalInterruptsMining() {
        // Setup
        ProcessManager manager = new ProcessManager();
        MockSurvivalProcess survival = new MockSurvivalProcess();
        MockTaskProcess mining = new MockTaskProcess();
        MockExecutionContext context = new MockExecutionContext();

        manager.registerProcess(survival);
        manager.registerProcess(mining);

        // Mining has desire 0.5, Survival has 0.0 (healthy)
        context.setHealthPercent(1.0);
        survival.setDesire(0.0);
        mining.setDesire(0.5);

        manager.tick(context);

        assertEquals(mining, manager.getCurrentProcess());

        // Health drops - survival desire increases
        context.setHealthPercent(0.15);
        survival.setDesire(1.0);

        manager.tick(context);

        // Survival should take control
        assertEquals(survival, manager.getCurrentProcess());
        assertTrue(mining.wasRevoked());
    }
}
```

### 6.3 Performance Testing

**Benchmarks Required:**
1. **Stuck Detection Overhead:** Should be < 1ms per tick
2. **Goal Heuristic Calculation:** Should handle 1000+ goals without lag
3. **DSL Parsing:** Should parse complex script in < 100ms
4. **Item Rule Matching:** Should evaluate 1000 items in < 10ms
5. **Retry Backoff:** Should not accumulate memory

**Example Benchmark:**
```java
// src/test/java/com/minewright/performance/GoalCompositionBenchmark.java
public class GoalCompositionBenchmark {
    @Test
    public void benchmarkCompositeGoalHeuristic() {
        // Create composite with 1000 goals
        List<NavigationGoal> goals = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            goals.add(new BlockGoal(new BlockPos(i, 64, i * 2), 2.0));
        }
        NavigationGoal composite = new CompositeNavigationGoal(goals);

        BlockPos testPos = new BlockPos(500, 64, 1000);

        long start = System.nanoTime();
        double heuristic = composite.heuristic(testPos);
        long duration = System.nanoTime() - start;

        // Should complete in < 1ms
        assertTrue(duration < 1_000_000,
            "Heuristic took " + duration + "ns, expected < 1ms");
    }
}
```

---

## 7. Success Metrics

### 7.1 Reliability Metrics

| Metric | Current | Target | Measurement |
|--------|---------|--------|-------------|
| **Stuck Recovery Rate** | 0% | >95% | Agents recover from stuck states automatically |
| **LLM Failure Recovery** | Partial | >90% | Retry with backoff succeeds after transient failures |
| **Task Completion Rate** | ~70% | >95% | Tasks complete without manual intervention |
| **Session Uptime** | ~30 min | >2 hours | Agents run continuously without crashes |

### 7.2 Performance Metrics

| Metric | Current | Target | Measurement |
|--------|---------|--------|-------------|
| **Tick Time (P50)** | ~5ms | <10ms | 50th percentile tick processing time |
| **Tick Time (P99)** | ~20ms | <50ms | 99th percentile tick processing time |
| **Memory per Agent** | ~50MB | <100MB | Memory usage per active agent |
| **Goal Heuristic Time** | N/A | <1ms | Time to calculate heuristic for composite goal |

### 7.3 Humanization Metrics

| Metric | Current | Target | Measurement |
|--------|---------|--------|-------------|
| **Action Delay Variance** | 0% | >15% | Standard deviation of action delays |
| **Mistake Rate** | 0% | 2-3% | Percentage of intentional errors |
| **Idle Action Rate** | ~1% | 2-5% | Idle actions per minute when not working |
| **Session Pattern Compliance** | N/A | >80% | Follows warm-up/performance/fatigue phases |

### 7.4 Capability Metrics

| Metric | Current | Target | Measurement |
|--------|---------|--------|-------------|
| **DSL Scripts Supported** | 0 | 20+ | Number of working DSL scripts |
| **Item Rules** | Hardcoded | Configurable | Declarative item filtering |
| **Concurrent Behaviors** | Conflicts | Arbitrated | Process manager prevents conflicts |
| **Navigation Flexibility** | Fixed target | Composite goals | Can target nearest of multiple locations |

---

## 8. Implementation Timeline Summary

### Week 1-2: Critical Reliability
- [ ] StuckDetector with position-based detection
- [ ] RetryPolicy with exponential backoff
- [ ] State rollback mechanism
- [ ] Wire into AgentStateMachine

### Week 3-4: Decision Architecture
- [ ] NavigationGoal interface
- [ ] CompositeNavigationGoal implementation
- [ ] BehaviorProcess interface
- [ ] ProcessManager with desire-based selection
- [ ] UtilityDecisionMaker for task prioritization

### Week 5-6: Humanization
- [ ] JitterUtils for Gaussian noise
- [ ] MistakeSimulator for intentional errors
- [ ] IdleBehaviorController with personality
- [ ] SessionManager for fatigue tracking

### Week 7-8: Knowledge Management
- [ ] ItemRule interface
- [ ] ItemRuleEngine with rule priority
- [ ] RandomEventSystem with interrupt capability
- [ ] Enhanced BTBlackboard with zone awareness

### Week 9-12: Script Language
- [ ] DSL JSON schema definition
- [ ] DSLParser implementation
- [ ] ScriptTemplate library
- [ ] LLM→Script generation pipeline

---

## 9. Risk Mitigation

### 9.1 Technical Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Performance degradation** | High | Benchmark all new features, set strict limits |
| **Behavior conflicts** | Medium | Process arbitration prevents conflicts |
| **DSL parsing errors** | Medium | Extensive validation, error messages |
| **State corruption** | High | Rollback mechanism, extensive testing |

### 9.2 Integration Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Breaking existing scripts** | High | Backward compatibility, migration path |
| **Plugin conflicts** | Medium | Careful interface design, versioning |
| **LLM hallucination** | Medium | Validate generated DSL, fallback to defaults |

### 9.3 Usability Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| **DSL too complex** | Medium | Provide templates, examples, documentation |
| **Too many configuration options** | Low | Sensible defaults, optional tuning |
| **Humanization feels fake** | Medium | Calibrate based on feedback, make configurable |

---

## 10. Conclusion

This improvement plan synthesizes 30 years of game automation research into a concrete roadmap for enhancing Steve AI's Script DSL system. The patterns identified across all five research documents converge on a set of core principles:

**Universal Truths in Game Automation:**
1. **Reliability First:** Stuck detection and error recovery are non-negotiable
2. **Humanization Matters:** Timing variation and mistakes feel more natural than perfection
3. **Composable Decisions:** Goal composition and process arbitration enable flexibility
4. **Declarative Configuration:** DSLs and rule engines enable rapid iteration
5. **Event-Driven Reactivity:** Respond to game events, don't just poll

**Implementation Priority:**
1. **Phase 1 (Weeks 1-2):** Eliminate frustration → Stuck detection, retry, rollback
2. **Phase 2 (Weeks 3-4):** Enable intelligence → Goal composition, process arbitration
3. **Phase 3 (Weeks 5-6):** Add character → Humanization, mistakes, sessions
4. **Phase 4 (Weeks 7-8):** Manage knowledge → Item rules, random events
5. **Phase 5 (Weeks 9-12):** Empower users → Script DSL language

**Expected Outcomes:**
- **Reliability:** 95%+ task completion without intervention
- **Naturalness:** Human-like timing, occasional mistakes, idle behaviors
- **Flexibility:** Composite goals, process arbitration, declarative rules
- **Usability:** Human-readable scripts, LLM generation, template library

By following this roadmap, Steve AI's Script DSL will evolve from a capable automation system into a sophisticated, characterful AI companion that learns from experience and feels natural to play with.

---

**Document Version:** 1.0
**Last Updated:** 2026-03-01
**Next Review:** After Phase 1 completion (Week 2)
**Maintained By:** Claude (Orchestrator Agent)
**Status:** Ready for implementation

---

## Appendix A: Quick Reference Implementation

### A.1 Minimal Stuck Detection (1 file, ~50 lines)

```java
// src/main/java/com/minewright/behavior/StuckDetector.java
public class StuckDetector {
    private Vec3 lastPosition;
    private int stuckTicks = 0;
    private static final int STUCK_THRESHOLD = 100; // 5 seconds at 20 TPS

    public void update(Vec3 currentPosition) {
        if (lastPosition != null && currentPosition.distanceTo(lastPosition) < 0.1) {
            stuckTicks++;
            if (stuckTicks > STUCK_THRESHOLD) {
                throw new StuckException("Agent hasn't moved for 5 seconds");
            }
        } else {
            stuckTicks = 0;
            lastPosition = currentPosition;
        }
    }
}
```

### A.2 Minimal Retry with Backoff (enhance existing, ~20 lines)

```java
// Enhance src/main/java/com/minewright/execution/RetryPolicy.java
public long getDelay(int attempt) {
    long exponentialDelay = (long) (baseDelay * Math.pow(2, attempt));
    long delay = Math.min(exponentialDelay, maxDelay);
    double jitter = 0.8 + (Math.random() * 0.4); // ±20%
    return (long) (delay * jitter);
}
```

### A.3 Minimal Goal Composition (3 files, ~60 lines)

```java
// src/main/java/com/minewright/script/navigation/NavigationGoal.java
public interface NavigationGoal {
    boolean isSatisfied(BlockPos position);
    double heuristic(BlockPos position);
}

// src/main/java/com/minewright/script/navigation/CompositeNavigationGoal.java
public class CompositeNavigationGoal implements NavigationGoal {
    private final List<NavigationGoal> goals;

    public CompositeNavigationGoal(List<NavigationGoal> goals) {
        this.goals = goals;
    }

    public boolean isSatisfied(BlockPos position) {
        return goals.stream().anyMatch(g -> g.isSatisfied(position));
    }

    public double heuristic(BlockPos position) {
        return goals.stream().mapToDouble(g -> g.heuristic(position)).min().orElse(Double.MAX_VALUE);
    }
}

// src/main/java/com/minewright/script/navigation/BlockGoal.java
public class BlockGoal implements NavigationGoal {
    private final BlockPos target;
    private final double tolerance;

    public BlockGoal(BlockPos target, double tolerance) {
        this.target = target;
        this.tolerance = tolerance;
    }

    public boolean isSatisfied(BlockPos position) {
        return position.distSqr(target) <= tolerance * tolerance;
    }

    public double heuristic(BlockPos position) {
        return Math.sqrt(position.distSqr(target));
    }
}
```

### A.4 Minimal Humanization (2 files, ~40 lines)

```java
// src/main/java/com/minewright/behavior/humanization/JitterUtils.java
public class JitterUtils {
    public static int addJitter(int value, double varianceFraction) {
        double jitter = (Math.random() - 0.5) * 2 * varianceFraction * value;
        return (int) Math.max(value * 0.5, Math.min(value * 1.5, value + jitter));
    }
}

// src/main/java/com/minewright/behavior/humanization/MistakeSimulator.java
public class MistakeSimulator {
    public boolean shouldMakeMistake() {
        return Math.random() < 0.03; // 3% error rate
    }

    public BlockPos getMistakenTarget(BlockPos intended) {
        Direction[] dirs = Direction.values();
        return intended.relative(dirs[(int) (Math.random() * dirs.length)]);
    }
}
```

These minimal implementations can be added incrementally without disrupting existing functionality, then enhanced over time.

---

**End of Document**
