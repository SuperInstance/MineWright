# Stuck Detection and Recovery System Design

**Document Version:** 1.0
**Date:** 2026-03-01
**Status:** Design Specification
**Priority:** P1 (Critical Reliability)

---

## Executive Summary

This document describes a comprehensive stuck detection and recovery system for Steve AI agents. The system addresses a critical gap identified in cross-game bot research: **successful game automation requires robust stuck detection and automatic recovery**.

**Key Insights from Research:**
- OnmyojiAutoScript, game automation frameworks, and 30 years of bot development all rely on stuck detection
- Common failure modes: position locks, state stalls, progress halts, pathfinding failures
- Recovery strategies range from simple retries to LLM-assisted replanning

**Design Goals:**
1. **Detect** stuck conditions within 5 seconds (100 ticks at 20 TPS)
2. **Recover** automatically from 95%+ of stuck states
3. **Escalate** to LLM for complex failures
4. **Learn** from successful recovery patterns

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Detection Types](#detection-types)
3. [Recovery Strategies](#recovery-strategies)
4. [Class Specifications](#class-specifications)
5. [Integration Plan](#integration-plan)
6. [Configuration](#configuration)
7. [Test Scenarios](#test-scenarios)
8. [Performance Considerations](#performance-considerations)

---

## 1. Architecture Overview

### 1.1 System Architecture

```
FOREMAN ENTITY TICK LOOP
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│                    STUCK DETECTION LAYER                     │
│                                                               │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │ Position Detector│  │ State Detector  │  │ Progress    │ │
│  │ (hasn't moved)  │  │ (same state too │  │ Detector    │ │
│  │                 │  │  long)          │  │ (no blocks  │ │
│  └─────────────────┘  └─────────────────┘  │  mined)     │ │
│                                              └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ StuckDetectedEvent
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   RECOVERY ORCHESTRATOR                      │
│                                                               │
│  Analyzes stuck type, selects strategy, executes recovery    │
│  - Maintains recovery history                                │
│  - Escalates to LLM if needed                                │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ RecoveryStrategy
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                     RECOVERY STRATEGIES                       │
│                                                               │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────┐   │
│  │ Retry       │  │ Repath      │  │ Teleport         │   │
│  │ (try again) │  │ (new route) │  │ (safe location)  │   │
│  └─────────────┘  └─────────────┘  └──────────────────┘   │
│                                                               │
│  ┌─────────────┐  ┌──────────────────────────────────────┐│
│  │ Abort       │  │ Escalate                             ││
│  │ (give up)   │  │ (ask LLM for help)                   ││
│  └─────────────┘  └──────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
                            │
                            │ RecoveryCompleteEvent
                            ▼
                    RESUME NORMAL OPERATION
```

### 1.2 Data Flow

```
Tick() → Update Detectors → Check Stuck → Select Strategy → Execute Recovery → Log Result
```

### 1.3 State Machine Integration

The stuck detection system integrates with `AgentStateMachine` by adding a new state:

```
Existing States: IDLE, PLANNING, EXECUTING, PAUSED, COMPLETED, FAILED

New State: RECOVERING
  - Entered when stuck condition detected
  - Executes recovery strategy
  - Returns to previous state on success
  - Transitions to FAILED on unrecoverable failure

Updated Transitions:
  EXECUTING → RECOVERING (stuck detected)
  RECOVERING → EXECUTING (recovery successful)
  RECOVERING → FAILED (recovery failed after all attempts)
```

---

## 2. Detection Types

### 2.1 Position-Based Detection

**Purpose:** Detect when agent hasn't moved in X ticks

**Implementation:**
```java
public class PositionStuckDetector {
    private Vec3 lastPosition;
    private int stuckTicks = 0;
    private static final int STUCK_THRESHOLD = 100; // 5 seconds at 20 TPS
    private static final double MOVEMENT_THRESHOLD = 0.1; // blocks

    public StuckDetectionResult detect(Vec3 currentPosition) {
        if (lastPosition == null) {
            lastPosition = currentPosition;
            return StuckDetectionResult.NOT_STUCK;
        }

        double distance = currentPosition.distanceTo(lastPosition);

        if (distance < MOVEMENT_THRESHOLD) {
            stuckTicks++;
            if (stuckTicks > STUCK_THRESHOLD) {
                return StuckDetectionResult.stuck(StuckType.POSITION_UNCHANGED,
                    "Agent hasn't moved for " + (stuckTicks / 20.0) + " seconds");
            }
        } else {
            stuckTicks = 0;
            lastPosition = currentPosition;
        }

        return StuckDetectionResult.NOT_STUCK;
    }
}
```

**Use Cases:**
- Agent walking but not progressing
- Navigation pathfinding failure
- Agent blocked by obstacle
- Agent stuck in geometry

---

### 2.2 State-Based Detection

**Purpose:** Detect when agent in same state for too long

**Implementation:**
```java
public class StateStuckDetector {
    private AgentState lastState;
    private int ticksInState = 0;
    private static final Map<AgentState, Integer> STATE_TIMEOUTS;

    static {
        STATE_TIMEOUTS = Map.of(
            AgentState.PLANNING, 600,    // 30 seconds
            AgentState.EXECUTING, 1200,  // 60 seconds
            AgentState.RECOVERING, 300   // 15 seconds
        );
    }

    public StuckDetectionResult detect(AgentState currentState) {
        if (currentState != lastState) {
            lastState = currentState;
            ticksInState = 0;
            return StuckDetectionResult.NOT_STUCK;
        }

        ticksInState++;

        Integer timeout = STATE_TIMEOUTS.get(currentState);
        if (timeout != null && ticksInState > timeout) {
            return StuckDetectionResult.stuck(StuckType.STATE_STALLED,
                "Agent stuck in " + currentState + " state for " + (ticksInState / 20.0) + " seconds");
        }

        return StuckDetectionResult.NOT_STUCK;
    }
}
```

**Use Cases:**
- Planning taking too long
- Task executing but not completing
- Recovery looping

---

### 2.3 Progress-Based Detection

**Purpose:** Detect when task progress has stalled

**Implementation:**
```java
public class ProgressStuckDetector {
    private int lastProgress = 0;
    private int stuckTicks = 0;
    private static final int STUCK_THRESHOLD = 200; // 10 seconds

    public StuckDetectionResult detect(int currentProgress, int totalProgress) {
        if (currentProgress == lastProgress && currentProgress < totalProgress) {
            stuckTicks++;
            if (stuckTicks > STUCK_THRESHOLD) {
                return StuckDetectionResult.stuck(StuckType.PROGRESS_HALTED,
                    "Progress stalled at " + currentProgress + "/" + totalProgress +
                    " for " + (stuckTicks / 20.0) + " seconds");
            }
        } else {
            stuckTicks = 0;
            lastProgress = currentProgress;
        }

        return StuckDetectionResult.NOT_STUCK;
    }
}
```

**Use Cases:**
- Mining: blocks mined not increasing
- Building: blocks placed not increasing
- Gathering: items collected not increasing

---

### 2.4 Path-Based Detection

**Purpose:** Detect when pathfinding fails repeatedly

**Implementation:**
```java
public class PathStuckDetector {
    private int consecutiveFailures = 0;
    private static final int FAILURE_THRESHOLD = 3;

    public StuckDetectionResult detect(boolean pathfindingSuccess) {
        if (!pathfindingSuccess) {
            consecutiveFailures++;
            if (consecutiveFailures >= FAILURE_THRESHOLD) {
                return StuckDetectionResult.stuck(StuckType.PATHFINDING_FAILED,
                    "Pathfinding failed " + consecutiveFailures + " times consecutively");
            }
        } else {
            consecutiveFailures = 0;
        }

        return StuckDetectionResult.NOT_STUCK;
    }
}
```

**Use Cases:**
- Route to destination blocked
- Unreachable target
- Navigation mesh corrupted

---

### 2.5 Composite Detection

**Purpose:** Combine all detectors into single stuck detection system

**Implementation:**
```java
public class StuckDetectionManager {
    private final List<StuckDetector> detectors = new ArrayList<>();
    private final EventBus eventBus;

    public StuckDetectionManager(EventBus eventBus) {
        this.eventBus = eventBus;
        registerDetectors();
    }

    private void registerDetectors() {
        detectors.add(new PositionStuckDetector());
        detectors.add(new StateStuckDetector());
        detectors.add(new ProgressStuckDetector());
        detectors.add(new PathStuckDetector());
    }

    public StuckDetectionResult detect(StuckDetectionContext context) {
        for (StuckDetector detector : detectors) {
            StuckDetectionResult result = detector.detect(context);
            if (result.isStuck()) {
                eventBus.publish(new StuckDetectedEvent(result));
                return result;
            }
        }
        return StuckDetectionResult.NOT_STUCK;
    }
}
```

---

## 3. Recovery Strategies

### 3.1 Strategy Hierarchy

```
RecoveryStrategy (interface)
    ├── RetryStrategy
    ├── RepathStrategy
    ├── TeleportStrategy
    ├── AbortStrategy
    └── EscalateStrategy
```

### 3.2 Retry Strategy

**Purpose:** Try again with same or modified parameters

**When to Use:**
- Transient failures (temporary blocks)
- Network timeout
- First-time stuck

**Implementation:**
```java
public class RetryStrategy implements RecoveryStrategy {
    private final RetryPolicy retryPolicy;

    @Override
    public RecoveryResult execute(ForemanEntity foreman, StuckDetectionResult stuck) {
        ActionResult result = foreman.getActionExecutor().getCurrentActionResult();

        // Check if we can retry
        if (retryPolicy.getCurrentAttempt() >= retryPolicy.getMaxAttempts()) {
            return RecoveryResult.failure("Max retries exceeded");
        }

        // Wait with exponential backoff
        long delay = retryPolicy.getDelayMs(retryPolicy.getCurrentAttempt());
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RecoveryResult.failure("Retry interrupted");
        }

        // Retry the action
        LOGGER.info("[{}] Retrying action (attempt {}/{})",
            foreman.getEntityName(),
            retryPolicy.getCurrentAttempt() + 1,
            retryPolicy.getMaxAttempts());

        retryPolicy.incrementAttempt();
        foreman.getActionExecutor().retryCurrentAction();

        return RecoveryResult.success("Action retried");
    }
}
```

**Configuration:**
```toml
[stuck_detection.retry]
max_attempts = 3
initial_delay_ms = 1000
max_delay_ms = 10000
backoff_multiplier = 2.0
```

---

### 3.3 Repath Strategy

**Purpose:** Calculate new path to destination

**When to Use:**
- Pathfinding failures
- Position-based stuck
- Route blocked

**Implementation:**
```java
public class RepathStrategy implements RecoveryStrategy {
    private final HierarchicalPathfinder pathfinder;

    @Override
    public RecoveryResult execute(ForemanEntity foreman, StuckDetectionResult stuck) {
        // Get current action target
        BlockPos target = foreman.getActionExecutor().getCurrentTarget();
        BlockPos current = foreman.blockPosition();

        LOGGER.info("[{}] Repathing from {} to {}",
            foreman.getEntityName(), current, target);

        // Calculate new path with alternative route
        Path newPath = pathfinder.findPathAlternative(current, target);

        if (newPath == null || newPath.isEmpty()) {
            LOGGER.warn("[{}] Repath failed - no alternative route", foreman.getEntityName());
            return RecoveryResult.failure("No alternative path found");
        }

        // Update action with new path
        foreman.getActionExecutor().updatePath(newPath);

        LOGGER.info("[{}] Repath successful - {} steps", foreman.getEntityName(), newPath.getLength());
        return RecoveryResult.success("New path calculated with " + newPath.getLength() + " steps");
    }
}
```

**Configuration:**
```toml
[stuck_detection.repath]
max_alternatives = 5
timeout_ms = 5000
allow_unsafe = false
```

---

### 3.4 Teleport Strategy

**Purpose:** Move agent to safe location

**When to Use:**
- Agent stuck in geometry
- Repath failed multiple times
- Critical recovery needed

**Safety Considerations:**
- Only teleport to safe locations (not in blocks, not in lava)
- Prefer teleporting to last known good position
- Log all teleports for debugging

**Implementation:**
```java
public class TeleportStrategy implements RecoveryStrategy {
    private final SafetyValidator safetyValidator;

    @Override
    public RecoveryResult execute(ForemanEntity foreman, StuckDetectionResult stuck) {
        BlockPos safeLocation = findSafeLocation(foreman);

        if (safeLocation == null) {
            LOGGER.error("[{}] No safe location found for teleport", foreman.getEntityName());
            return RecoveryResult.failure("No safe teleport location");
        }

        LOGGER.warn("[{}] Teleporting from {} to {} (RECOVERY)",
            foreman.getEntityName(),
            foreman.blockPosition(),
            safeLocation);

        // Teleport the entity
        foreman.teleportTo(safeLocation.getX(), safeLocation.getY(), safeLocation.getZ());

        // Reset navigation state
        foreman.getActionExecutor().clearPath();

        return RecoveryResult.success("Teleported to safe location");
    }

    private BlockPos findSafeLocation(ForemanEntity foreman) {
        // Priority order:
        // 1. Last known good position
        // 2. Nearest safe surface
        // 3. Spawn point
        // 4. Player location

        BlockPos lastGood = foreman.getActionExecutor().getLastKnownGoodPosition();
        if (lastGood != null && safetyValidator.isSafe(lastGood)) {
            return lastGood;
        }

        BlockPos nearestSafe = findNearestSafeSurface(foreman);
        if (nearestSafe != null) {
            return nearestSafe;
        }

        // Fall back to spawn or player
        return foreman.level().getSharedSpawnPos();
    }
}
```

**Configuration:**
```toml
[stuck_detection.teleport]
enabled = true
require_safe = true
max_distance = 100
prefer_last_known = true
```

---

### 3.5 Abort Strategy

**Purpose:** Give up and report failure to user

**When to Use:**
- All recovery strategies exhausted
- Permanent error detected
- User requested cancellation

**Implementation:**
```java
public class AbortStrategy implements RecoveryStrategy {
    @Override
    public RecoveryResult execute(ForemanEntity foreman, StuckDetectionResult stuck) {
        LOGGER.error("[{}] Aborting task after all recovery attempts failed: {}",
            foreman.getEntityName(),
            stuck.getReason());

        // Fail the current action
        ActionResult failure = ActionResult.failure(
            "Task aborted: " + stuck.getReason() +
            " (recovery attempts exhausted)");

        foreman.getActionExecutor().failCurrentAction(failure);

        // Notify user
        foreman.sendSystemMessage(Component.literal(
            "Task failed: " + stuck.getReason()));

        return RecoveryResult.failure("Task aborted");
    }
}
```

---

### 3.6 Escalate Strategy

**Purpose:** Ask LLM for help with complex failures

**When to Use:**
- Multiple stuck detections in short time
- Unknown failure patterns
- Complex multi-step tasks

**Implementation:**
```java
public class EscalateStrategy implements RecoveryStrategy {
    private final TaskPlanner taskPlanner;

    @Override
    public RecoveryResult execute(ForemanEntity foreman, StuckDetectionResult stuck) {
        LOGGER.info("[{}] Escalating to LLM for recovery assistance", foreman.getEntityName());

        // Build context for LLM
        String context = buildEscalationContext(foreman, stuck);

        // Ask LLM for help
        String llmResponse = taskPlanner.askForHelp(
            foreman,
            "I'm stuck: " + stuck.getReason() +
            ". Context: " + context +
            ". What should I do?"
        );

        // Parse LLM response and execute
        RecoveryResult result = parseAndExecuteLLMResponse(foreman, llmResponse);

        LOGGER.info("[{}] LLM escalation result: {}",
            foreman.getEntityName(),
            result.getMessage());

        return result;
    }

    private String buildEscalationContext(ForemanEntity foreman, StuckDetectionResult stuck) {
        return String.format(
            "Location: %s, State: %s, Current Task: %s, Stuck Type: %s",
            foreman.blockPosition(),
            foreman.getActionExecutor().getStateMachine().getCurrentState(),
            foreman.getActionExecutor().getCurrentTask(),
            stuck.getStuckType()
        );
    }
}
```

**Configuration:**
```toml
[stuck_detection.escalate]
enabled = true
max_escalations_per_task = 2
require_confirmation = false
```

---

## 4. Class Specifications

### 4.1 Core Classes

```java
// Base detector interface
public interface StuckDetector {
    StuckDetectionResult detect(StuckDetectionContext context);
    void reset();
}

// Detection result
public class StuckDetectionResult {
    private final boolean stuck;
    private final StuckType stuckType;
    private final String reason;
    private final BlockPos position;
    private final long timestamp;

    public static StuckDetectionResult stuck(StuckType type, String reason) {
        return new StuckDetectionResult(true, type, reason, ...);
    }

    public static final StuckDetectionResult NOT_STUCK =
        new StuckDetectionResult(false, null, null, ...);
}

// Stuck types enum
public enum StuckType {
    POSITION_UNCHANGED,   // Agent hasn't moved
    STATE_STALLED,        // Same state too long
    PROGRESS_HALTED,      // Task progress stalled
    PATHFINDING_FAILED,   // Can't find path
    UNKNOWN               // Unclassified stuck
}

// Detection context passed to detectors
public class StuckDetectionContext {
    private final Vec3 currentPosition;
    private final AgentState currentState;
    private final int currentProgress;
    private final int totalProgress;
    private final boolean pathfindingSuccess;
    // ... getters
}
```

### 4.2 Recovery Classes

```java
// Base strategy interface
public interface RecoveryStrategy {
    RecoveryResult execute(ForemanEntity foreman, StuckDetectionResult stuck);
    String getName();
    int getPriority();
}

// Recovery result
public class RecoveryResult {
    private final boolean success;
    private final String message;
    private final RecoveryStrategyType strategyType;

    public static RecoveryResult success(String message) {
        return new RecoveryResult(true, message, ...);
    }

    public static RecoveryResult failure(String message) {
        return new RecoveryResult(false, message, ...);
    }
}

// Recovery orchestrator
public class RecoveryOrchestrator {
    private final List<RecoveryStrategy> strategies;
    private final Map<StuckType, List<RecoveryStrategy>> strategyMap;
    private final EventBus eventBus;

    public RecoveryResult executeRecovery(ForemanEntity foreman, StuckDetectionResult stuck) {
        // Get strategies for this stuck type
        List<RecoveryStrategy> applicableStrategies =
            strategyMap.getOrDefault(stuck.getStuckType(), strategies);

        // Try each strategy in priority order
        for (RecoveryStrategy strategy : applicableStrategies) {
            LOGGER.info("[{}] Attempting recovery: {}",
                foreman.getEntityName(), strategy.getName());

            RecoveryResult result = strategy.execute(foreman, stuck);

            if (result.isSuccess()) {
                eventBus.publish(new RecoveryCompleteEvent(strategy, result));
                return result;
            }

            LOGGER.warn("[{}] Recovery {} failed: {}",
                foreman.getEntityName(), strategy.getName(), result.getMessage());
        }

        // All strategies failed
        return RecoveryResult.failure("All recovery strategies exhausted");
    }
}
```

### 4.3 Event Classes

```java
// Stuck detected event
public class StuckDetectedEvent {
    private final String agentId;
    private final StuckType stuckType;
    private final String reason;
    private final BlockPos position;
    private final long timestamp;
}

// Recovery complete event
public class RecoveryCompleteEvent {
    private final String agentId;
    private final RecoveryStrategy strategy;
    private final RecoveryResult result;
    private final long duration;
}

// Recovery failed event
public class RecoveryFailedEvent {
    private final String agentId;
    private final StuckType stuckType;
    private final List<RecoveryResult> attemptedStrategies;
    private final String finalMessage;
}
```

---

## 5. Integration Plan

### 5.1 ForemanEntity Integration

**Location:** `src/main/java/com/minewright/entity/ForemanEntity.java`

**Changes Required:**

1. **Add stuck detection manager as field:**
```java
private StuckDetectionManager stuckDetectionManager;
```

2. **Initialize in constructor:**
```java
this.stuckDetectionManager = new StuckDetectionManager(eventBus);
```

3. **Call detection in tick() method:**
```java
@Override
public void tick() {
    super.tick();

    // Existing tick logic...

    // Stuck detection (every tick)
    StuckDetectionContext context = new StuckDetectionContext(
        position(),
        actionExecutor.getStateMachine().getCurrentState(),
        actionExecutor.getCurrentProgress(),
        // ... other context
    );

    StuckDetectionResult result = stuckDetectionManager.detect(context);
    if (result.isStuck()) {
        handleStuck(result);
    }

    // Rest of tick logic...
}
```

4. **Add stuck handler:**
```java
private void handleStuck(StuckDetectionResult stuck) {
    LOGGER.warn("[{}] Stuck detected: {}", getEntityName(), stuck.getReason());

    // Transition to RECOVERING state
    actionExecutor.getStateMachine().transitionTo(
        AgentState.RECOVERING,
        "Stuck: " + stuck.getReason()
    );

    // Execute recovery
    RecoveryResult recovery = recoveryOrchestrator.executeRecovery(this, stuck);

    if (recovery.isSuccess()) {
        // Resume previous state
        actionExecutor.getStateMachine().transitionTo(
            AgentState.EXECUTING,
            "Recovery successful"
        );
    } else {
        // Fail the task
        actionExecutor.getStateMachine().transitionTo(
            AgentState.FAILED,
            "Recovery failed: " + recovery.getMessage()
        );
    }
}
```

---

### 5.2 AgentStateMachine Integration

**Location:** `src/main/java/com/minewright/execution/AgentStateMachine.java`

**Changes Required:**

1. **Add RECOVERING state to enum:**
```java
public enum AgentState {
    IDLE,
    PLANNING,
    EXECUTING,
    RECOVERING,  // NEW
    PAUSED,
    COMPLETED,
    FAILED
}
```

2. **Add valid transitions:**
```java
// In VALID_TRANSITIONS initialization:
VALID_TRANSITIONS.put(AgentState.EXECUTING,
    EnumSet.of(AgentState.COMPLETED, AgentState.FAILED, AgentState.PAUSED, AgentState.RECOVERING));

VALID_TRANSITIONS.put(AgentState.RECOVERING,
    EnumSet.of(AgentState.EXECUTING, AgentState.FAILED, AgentState.IDLE));
```

---

### 5.3 ActionExecutor Integration

**Location:** `src/main/java/com/minewright/action/ActionExecutor.java`

**Changes Required:**

1. **Add recovery tracking:**
```java
private int consecutiveStuckCount = 0;
private static final int MAX_STUCK_BEFORE_ABORT = 5;
```

2. **Add retry support:**
```java
public void retryCurrentAction() {
    if (currentAction != null) {
        currentAction.reset();
        // Retry will happen on next tick
    }
}
```

3. **Update stuck count on detection:**
```java
public void onStuckDetected() {
    consecutiveStuckCount++;
    if (consecutiveStuckCount >= MAX_STUCK_BEFORE_ABORT) {
        // Give up and report failure
        failCurrentAction(ActionResult.failure("Too many stuck detections"));
    }
}

public void onRecoverySuccess() {
    consecutiveStuckCount = 0;
}
```

---

### 5.4 Configuration Integration

**Location:** `src/main/java/com/minewright/config/MineWrightConfig.java`

**Add stuck detection configuration:**
```java
public static final ForgeConfigSpec.ConfigValue<Boolean> STUCK_DETECTION_ENABLED;
public static final ForgeConfigSpec.ConfigValue<Integer> STUCK_POSITION_THRESHOLD;
public static final ForgeConfigSpec.ConfigValue<Integer> STUCK_STATE_TIMEOUT;
public static final ForgeConfigSpec.ConfigValue<Boolean> RECOVERY_TELEPORT_ENABLED;
public static final ForgeConfigSpec.ConfigValue<Boolean> RECOVERY_ESCALATE_ENABLED;

// In builder:
STUCK_DETECTION_ENABLED = builder
    .comment("Enable stuck detection system")
    .define("stuck_detection.enabled", true);

STUCK_POSITION_THRESHOLD = builder
    .comment("Ticks without movement before considered stuck (20 ticks = 1 second)")
    .defineInRange("stuck_detection.position_threshold", 100, 20, 600);

STUCK_STATE_TIMEOUT = builder
    .comment("Ticks in same state before considered stuck")
    .defineInRange("stuck_detection.state_timeout", 600, 60, 3600);

RECOVERY_TELEPORT_ENABLED = builder
    .comment("Allow teleport as recovery strategy")
    .define("stuck_detection.recovery.teleport_enabled", true);

RECOVERY_ESCALATE_ENABLED = builder
    .comment("Allow escalation to LLM for complex failures")
    .define("stuck_detection.recovery.escalate_enabled", true);
```

---

## 6. Configuration

### 6.1 Complete Configuration File

**Location:** `config/minewright-common.toml`

```toml
[stuck_detection]
# Enable/disable stuck detection system
enabled = true

# Detection thresholds
position_threshold_ticks = 100        # 5 seconds at 20 TPS
movement_threshold_blocks = 0.1      # Minimum movement to reset counter
state_timeout_ticks = 600            # 30 seconds
progress_stall_ticks = 200           # 10 seconds
pathfinding_failure_count = 3        # Consecutive failures

# Recovery settings
[stuck_detection.recovery]
# Strategy priority order (higher = tried first)
retry_priority = 100
repath_priority = 90
teleport_priority = 50
escalate_priority = 30
abort_priority = 10

# Retry configuration
[stuck_detection.recovery.retry]
max_attempts = 3
initial_delay_ms = 1000
max_delay_ms = 10000
backoff_multiplier = 2.0
add_jitter = true

# Repath configuration
[stuck_detection.recovery.repath]
max_alternatives = 5
timeout_ms = 5000
allow_unsafe_paths = false

# Teleport configuration
[stuck_detection.recovery.teleport]
enabled = true
require_safe_location = true
max_distance_blocks = 100
prefer_last_known_position = true
allow_spawn_teleport = true

# Escalation configuration
[stuck_detection.recovery.escalate]
enabled = true
max_escalations_per_task = 2
require_user_confirmation = false
timeout_ms = 30000

# Abort configuration
[stuck_detection.recovery.abort]
max_consecutive_stuck = 5
require_user_notification = true
log_to_console = true

# Learning configuration
[stuck_detection.learning]
track_successful_recoveries = true
track_failed_recoveries = true
adapt_thresholds = false
```

### 6.2 Per-Agent Configuration

Agents can have individual stuck detection settings:

```java
public class AgentStuckConfig {
    private int positionThreshold = 100;
    private boolean teleportEnabled = true;
    private boolean escalateEnabled = true;

    // Can be customized per agent based on:
    // - Role (foreman vs worker)
    // - Personality traits
    // - Past performance
}
```

---

## 7. Test Scenarios

### 7.1 Unit Tests

**Test File:** `src/test/java/com/minewright/action/StuckDetectionTest.java`

```java
public class StuckDetectionTest {

    @Test
    public void testPositionStuckDetection() {
        PositionStuckDetector detector = new PositionStuckDetector();
        Vec3 pos = new Vec3(0, 64, 0);

        // 100 ticks should not trigger
        for (int i = 0; i < 100; i++) {
            assertEquals(StuckDetectionResult.NOT_STUCK, detector.detect(pos));
        }

        // 101st tick should trigger
        StuckDetectionResult result = detector.detect(pos);
        assertTrue(result.isStuck());
        assertEquals(StuckType.POSITION_UNCHANGED, result.getStuckType());
    }

    @Test
    public void testPositionStuckResetsOnMovement() {
        PositionStuckDetector detector = new PositionStuckDetector();
        Vec3 pos1 = new Vec3(0, 64, 0);
        Vec3 pos2 = new Vec3(1, 64, 0);

        // 50 ticks without movement
        for (int i = 0; i < 50; i++) {
            detector.detect(pos1);
        }

        // Movement resets counter
        detector.detect(pos2);

        // Another 100 ticks should not trigger (counter was reset)
        for (int i = 0; i < 100; i++) {
            assertEquals(StuckDetectionResult.NOT_STUCK, detector.detect(pos2));
        }
    }

    @Test
    public void testStateStuckDetection() {
        StateStuckDetector detector = new StateStuckDetector();

        // Should not trigger immediately
        assertEquals(StuckDetectionResult.NOT_STUCK,
            detector.detect(AgentState.EXECUTING));

        // Simulate 601 ticks in same state (exceeds 600 tick timeout)
        for (int i = 0; i < 600; i++) {
            detector.detect(AgentState.EXECUTING);
        }

        StuckDetectionResult result = detector.detect(AgentState.EXECUTING);
        assertTrue(result.isStuck());
        assertEquals(StuckType.STATE_STALLED, result.getStuckType());
    }

    @Test
    public void testProgressStuckDetection() {
        ProgressStuckDetector detector = new ProgressStuckDetector();

        // Progress: 0/100 blocks mined
        assertEquals(StuckDetectionResult.NOT_STUCK,
            detector.detect(0, 100));

        // Simulate 200 ticks of no progress
        for (int i = 0; i < 200; i++) {
            detector.detect(0, 100);
        }

        StuckDetectionResult result = detector.detect(0, 100);
        assertTrue(result.isStuck());
        assertEquals(StuckType.PROGRESS_HALTED, result.getStuckType());
    }

    @Test
    public void testProgressStuckResetsOnProgress() {
        ProgressStuckDetector detector = new ProgressStuckDetector();

        // 100 ticks of no progress
        for (int i = 0; i < 100; i++) {
            detector.detect(0, 100);
        }

        // Progress resets counter
        detector.detect(5, 100);

        // Another 200 ticks should not trigger
        for (int i = 0; i < 200; i++) {
            assertEquals(StuckDetectionResult.NOT_STUCK,
                detector.detect(5, 100));
        }
    }

    @Test
    public void testPathStuckDetection() {
        PathStuckDetector detector = new PathStuckDetector();

        // First failure - not stuck
        assertEquals(StuckDetectionResult.NOT_STUCK,
            detector.detect(false));

        // Second failure - not stuck
        assertEquals(StuckDetectionResult.NOT_STUCK,
            detector.detect(false));

        // Third failure - stuck
        StuckDetectionResult result = detector.detect(false);
        assertTrue(result.isStuck());
        assertEquals(StuckType.PATHFINDING_FAILED, result.getStuckType());
    }
}
```

### 7.2 Integration Tests

**Test File:** `src/test/java/com/minewright/integration/StuckRecoveryIntegrationTest.java`

```java
public class StuckRecoveryIntegrationTest {

    @Test
    public void testStuckDetectionAndRecovery() {
        // Setup test world
        TestWorld world = new TestWorld();
        ForemanEntity steve = world.spawnForeman(new BlockPos(0, 64, 0));

        // Give steve a mining task
        steve.executeTask(new MineTask(new BlockPos(10, 64, 0), "stone"));

        // Simulate getting stuck (wall in the way)
        world.setBlock(new BlockPos(5, 64, 0), Blocks.OBSIDIAN);

        // Tick until stuck detected
        for (int i = 0; i < 150; i++) {
            steve.tick();
        }

        // Verify stuck was detected
        assertEquals(AgentState.RECOVERING, steve.getState());

        // Tick through recovery
        for (int i = 0; i < 20; i++) {
            steve.tick();
        }

        // Verify recovery successful
        assertEquals(AgentState.EXECUTING, steve.getState());
        assertNotEquals(new BlockPos(5, 64, 0), steve.blockPosition());
    }

    @Test
    public void testRecoveryExhaustion() {
        TestWorld world = new TestWorld();
        ForemanEntity steve = world.spawnForeman(new BlockPos(0, 64, 0));

        // Create impossible task (completely walled in)
        surroundWithObsidian(steve.blockPosition());
        steve.executeTask(new MineTask(new BlockPos(100, 64, 0), "stone"));

        // Tick through all recovery attempts
        for (int i = 0; i < 1000; i++) {
            steve.tick();
        }

        // Verify task failed after all recoveries exhausted
        assertEquals(AgentState.FAILED, steve.getState());
    }

    @Test
    public void testRecoveryWithTeleport() {
        TestWorld world = new TestWorld();
        ForemanEntity steve = world.spawnForeman(new BlockPos(0, 64, 0));

        // Enable teleport recovery
        steve.getConfig().setRecoveryTeleportEnabled(true);

        // Create stuck scenario
        world.setBlock(steve.blockPosition().above(), Blocks.BEDROCK);
        world.setBlock(steve.blockPosition().below(), Blocks.BEDROCK);
        world.setBlock(steve.blockPosition().north(), Blocks.BEDROCK);
        world.setBlock(steve.blockPosition().south(), Blocks.BEDROCK);
        world.setBlock(steve.blockPosition().east(), Blocks.BEDROCK);
        world.setBlock(steve.blockPosition().west(), Blocks.BEDROCK);

        steve.executeTask(new MineTask(new BlockPos(10, 64, 0), "stone"));

        // Tick until recovery
        for (int i = 0; i < 200; i++) {
            steve.tick();
        }

        // Verify teleport occurred
        assertNotEquals(new BlockPos(0, 64, 0), steve.blockPosition());
        assertEquals(AgentState.EXECUTING, steve.getState());
    }
}
```

### 7.3 Performance Tests

**Test File:** `src/test/java/com/minewright/performance/StuckDetectionPerformanceTest.java`

```java
public class StuckDetectionPerformanceTest {

    @Test
    public void testStuckDetectionOverhead() {
        StuckDetectionManager manager = new StuckDetectionManager(mockEventBus);
        StuckDetectionContext context = createContext();

        long startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            manager.detect(context);
        }
        long duration = System.nanoTime() - startTime;

        // Should be less than 1ms per detection
        long avgNs = duration / 10000;
        assertTrue(avgNs < 1_000_000,
            "Detection took " + avgNs + "ns, expected < 1ms");
    }

    @Test
    public void testRecoveryLatency() {
        RecoveryOrchestrator orchestrator = createOrchestrator();

        long startTime = System.nanoTime();
        RecoveryResult result = orchestrator.executeRecovery(
            mockForeman(),
            StuckDetectionResult.stuck(StuckType.POSITION_UNCHANGED, "Test")
        );
        long duration = System.nanoTime() - startTime;

        // Recovery should complete within 100ms
        assertTrue(duration < 100_000_000,
            "Recovery took " + (duration / 1_000_000) + "ms, expected < 100ms");
    }
}
```

---

## 8. Performance Considerations

### 8.1 Performance Targets

| Metric | Target | Rationale |
|--------|--------|-----------|
| **Detection Overhead** | < 1ms per tick | Must not impact game performance |
| **Recovery Latency** | < 100ms | Fast enough to not break immersion |
| **Memory Overhead** | < 1KB per agent | Minimal state tracking |
| **False Positive Rate** | < 5% | Avoid unnecessary recoveries |

### 8.2 Optimization Strategies

1. **Lazy Detection:**
   - Only run detectors when agent is EXECUTING
   - Skip detection when agent is IDLE or PAUSED

2. **Adaptive Thresholds:**
   - Increase thresholds for agents that rarely get stuck
   - Decrease thresholds for problematic agents

3. **Caching:**
   - Cache safe teleport locations
   - Cache alternative paths

4. **Async Recovery:**
   - Run LLM escalation asynchronously
   - Don't block tick thread for recovery

### 8.3 Monitoring

**Metrics to Track:**

```java
public class StuckDetectionMetrics {
    private final AtomicInteger stuckDetectionCount = new AtomicInteger(0);
    private final AtomicInteger recoverySuccessCount = new AtomicInteger(0);
    private final AtomicInteger recoveryFailureCount = new AtomicInteger(0);
    private final AtomicLong totalRecoveryTime = new AtomicLong(0);

    public void recordDetection(StuckType type) {
        stuckDetectionCount.incrementAndGet();
    }

    public void recordRecovery(boolean success, long duration) {
        if (success) {
            recoverySuccessCount.incrementAndGet();
        } else {
            recoveryFailureCount.incrementAndGet();
        }
        totalRecoveryTime.addAndGet(duration);
    }

    public double getSuccessRate() {
        int total = recoverySuccessCount.get() + recoveryFailureCount.get();
        return total == 0 ? 0 : (double) recoverySuccessCount.get() / total;
    }

    public double getAverageRecoveryTime() {
        int total = recoverySuccessCount.get() + recoveryFailureCount.get();
        return total == 0 ? 0 : (double) totalRecoveryTime.get() / total;
    }
}
```

---

## 9. Success Metrics

### 9.1 Reliability Metrics

| Metric | Current | Target | Measurement |
|--------|---------|--------|-------------|
| **Stuck Detection Rate** | 0% | >95% | Percentage of stuck states correctly detected |
| **Recovery Success Rate** | 0% | >90% | Percentage of recoveries that succeed |
| **False Positive Rate** | N/A | <5% | Percentage of false stuck detections |
| **Recovery Time (P50)** | N/A | <50ms | Median time to complete recovery |

### 9.2 User Experience Metrics

| Metric | Current | Target | Measurement |
|--------|---------|--------|-------------|
| **Task Completion Rate** | ~70% | >95% | Tasks complete without manual intervention |
| **User Intervention Rate** | High | <5% | Tasks requiring user help |
| **Agent Session Uptime** | ~30 min | >2 hours | Continuous operation without failure |

---

## 10. Implementation Roadmap

### Phase 1: Core Detection (Week 1)
- [ ] Implement `StuckDetector` interface
- [ ] Implement `PositionStuckDetector`
- [ ] Implement `StateStuckDetector`
- [ ] Implement `StuckDetectionManager`
- [ ] Add unit tests for detectors

### Phase 2: Recovery Strategies (Week 2)
- [ ] Implement `RecoveryStrategy` interface
- [ ] Implement `RetryStrategy`
- [ ] Implement `RepathStrategy`
- [ ] Implement `TeleportStrategy`
- [ ] Implement `AbortStrategy`
- [ ] Implement `RecoveryOrchestrator`
- [ ] Add unit tests for strategies

### Phase 3: Integration (Week 3)
- [ ] Integrate with `ForemanEntity.tick()`
- [ ] Add `RECOVERING` state to `AgentStateMachine`
- [ ] Wire up event bus for stuck events
- [ ] Add configuration options
- [ ] Integration testing

### Phase 4: Advanced Features (Week 4)
- [ ] Implement `ProgressStuckDetector`
- [ ] Implement `PathStuckDetector`
- [ ] Implement `EscalateStrategy` (LLM escalation)
- [ ] Add metrics and monitoring
- [ ] Performance optimization

---

## 11. Risk Mitigation

### 11.1 Technical Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| **Performance degradation** | High | Low | Benchmark detectors, keep overhead < 1ms |
| **False positives** | Medium | Medium | Tune thresholds, add hysteresis |
| **Recovery loops** | High | Low | Track consecutive recoveries, abort after N attempts |
| **Teleport abuse** | Low | Low | Require safe locations, log all teleports |

### 11.2 Design Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| **Too complex** | Medium | Low | Start simple, add features incrementally |
| **Too aggressive recovery** | Medium | Medium | Conservative thresholds, user confirmation option |
| **LLM escalation cost** | Low | Low | Limit escalations per task, cache results |

---

## 12. References

### 12.1 Research Documents

1. **Multi-Game Bot Patterns** - Error recovery strategies across 30 years of automation
2. **Script System Improvement Plan** - Stuck detection as P1 feature
3. **OnmyojiAutoScript Analysis** - Game automation stuck detection patterns

### 12.2 Design Patterns

1. **Strategy Pattern** - Recovery strategies
2. **Observer Pattern** - Event bus for stuck/recovery events
3. **State Pattern** - AgentStateMachine with RECOVERING state
4. **Chain of Responsibility** - Detector pipeline

### 12.3 Related Systems

1. **RetryPolicy** - Existing retry logic (will be integrated)
2. **ErrorRecoveryStrategy** - Existing error categorization (will be extended)
3. **AgentStateMachine** - State management (will be enhanced)

---

## 13. Conclusion

The stuck detection and recovery system addresses a critical gap in Steve AI's reliability. By learning from 30 years of game automation research, this design provides:

1. **Comprehensive Detection** - 4 detection types cover all common stuck scenarios
2. **Intelligent Recovery** - 5 recovery strategies with automatic escalation
3. **Minimal Overhead** - < 1ms detection overhead, < 100ms recovery time
4. **High Success Rate** - Target 95%+ detection, 90%+ recovery success

**Next Steps:**
1. Review and approve design
2. Begin Phase 1 implementation (core detection)
3. Iterate based on testing feedback
4. Deploy to production when metrics meet targets

---

**Document Version:** 1.0
**Last Updated:** 2026-03-01
**Author:** Claude (Design Agent)
**Status:** Ready for Implementation
**Review Date:** After Phase 2 completion (Week 2)
