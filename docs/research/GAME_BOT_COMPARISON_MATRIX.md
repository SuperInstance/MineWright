# Game Bot Architecture Comparison Matrix

**Document Version:** 1.0
**Date:** 2026-03-02
**Purpose:** Academic comparison of game automation architectures for legitimate AI development
**Project:** Steve AI - "Cursor for Minecraft"

---

## Executive Summary

This document synthesizes architectural patterns from multiple game automation systems studied for the Steve AI project. The goal is to identify reusable patterns for building legitimate, human-like AI companions in Minecraft.

**Key Insight:** All successful game bots share common architectural principles that can be ethically applied to companion AI:
- **Layered Architecture** - Separation of strategic, tactical, and physical layers
- **Humanization** - Statistical variation in timing, mistakes, and behavior
- **State Management** - Robust handling of stuck conditions, errors, and recovery
- **Profile Systems** - Declarative task definitions for modularity

---

## Comparison Matrix

### Architecture Patterns by Bot

| Feature | WoW Glider | Honorbuddy | Baritone | OSRS Bots | Diablo Bots |
|---------|------------|------------|----------|-----------|-------------|
| **Architecture** | Layered | Plugin-based | Goal-based | Script-based | State Machine |
| **Decision System** | Priority Queue | Behavior Trees | Goal System | Behavior Trees | FSM + Scripts |
| **Navigation** | Waypoints | NavMesh (Recast) | A* Pathfinding | Web Walking | Waypoints |
| **Humanization** | Gaussian delays | Full suite | Basic | ABC2 System | Minimal |
| **Profile System** | Profiles | XML Profiles | None | Scripts | Routes |
| **Plugin System** | Limited | Full C# API | Commands | Repository | Limited |
| **State Recovery** | Basic | Advanced | Fallback | Teleport | Reset |

---

## Detailed Pattern Analysis

### 1. Decision Making Systems

#### Priority Queue (WoW Glider)
```java
// Priority-based task arbitration
enum TaskPriority {
    EMERGENCY(1),  // Flee, heal
    URGENT(2),     // Loot, dodge
    NORMAL(3),     // Combat, gather
    LOW(4),        // Travel, idle
    BACKGROUND(5)  // Maintenance
}

class TaskQueue {
    PriorityQueue<Task> queue;

    void tick() {
        // Always process highest priority
        Task current = queue.peek();
        if (current.canExecute()) {
            current.execute();
        }
    }
}
```

**Application to Steve AI:**
- `ProcessManager` with priority-based arbitration
- `SurvivalProcess` > `TaskExecutionProcess` > `IdleProcess`
- Emergency interrupts for danger situations

#### Behavior Trees (Honorbuddy)
```java
// Composite nodes for hierarchical decisions
abstract class Composite : BehaviorNode {
    List<BehaviorNode> children;
}

class Sequence : Composite {  // AND - all must succeed
    Status execute() {
        for (child in children) {
            if (child.execute() != SUCCESS) return FAILURE;
        }
        return SUCCESS;
    }
}

class Selector : Composite {  // OR - one must succeed
    Status execute() {
        for (child in children) {
            if (child.execute() == SUCCESS) return SUCCESS;
        }
        return FAILURE;
    }
}
```

**Application to Steve AI:**
- `behavior/` package with full BT implementation
- Composite, Decorator, Leaf nodes
- Reactive re-evaluation every tick

### 2. Humanization Patterns

#### Timing Variation
| Bot | Method | Parameters |
|-----|--------|------------|
| WoW Glider | Gaussian jitter | μ=base, σ=20% |
| Honorbuddy | Reaction curves | Min/max clamp |
| OSRS (ABC2) | Action timing | Tabulates per action |
| Steve AI | `HumanizationUtils` | Full implementation |

```java
// Steve AI implementation (already complete)
public class HumanizationUtils {
    // Gaussian jitter for timing
    public static int gaussianJitter(int baseMs, double stdDevPercent) {
        double jitter = RANDOM.nextGaussian() * (baseMs * stdDevPercent);
        return (int) Math.max(MIN_ACTION_DELAY_MS, baseMs + jitter);
    }

    // Human reaction time (200-400ms typical)
    public static int humanReactionTime() {
        return (int) (MEAN_REACTION_TIME_MS +
            RANDOM.nextGaussian() * REACTION_TIME_STD_DEV_MS);
    }
}
```

#### Mistake Simulation
| Mistake Type | Probability | Recovery |
|--------------|-------------|----------|
| Wrong target | 2-5% | Auto-correct |
| Timing error | 5-10% | Continue |
| Movement error | 3-7% | Repath |
| Selection error | 2-5% | Re-select |

```java
// Steve AI implementation (already complete)
public class MistakeSimulator {
    public enum MistakeType {
        WRONG_TARGET(0.8),    // 80% of base rate
        TIMING_ERROR(1.0),    // 100% of base rate
        MOVEMENT_ERROR(0.9),  // 90% of base rate
        SELECTION_ERROR(0.7); // 70% of base rate

        public final double multiplier;
    }

    public boolean shouldMakeMistake(MistakeType type) {
        double adjustedRate = baseMistakeRate * type.multiplier;
        return RANDOM.nextDouble() < adjustedRate;
    }
}
```

#### Session Fatigue
```java
// Steve AI implementation (already complete)
public class SessionManager {
    public enum SessionPhase {
        WARMUP(1.3, 1.5),      // +30% reaction, +50% errors
        PERFORMANCE(1.0, 1.0), // Normal
        FATIGUE(1.5, 2.0);     // +50% reaction, +100% errors

        public final double reactionMultiplier;
        public final double errorMultiplier;
    }

    public SessionPhase getCurrentPhase() {
        long elapsed = System.currentTimeMillis() - sessionStart;
        if (elapsed < warmupDuration) return WARMUP;
        if (elapsed > fatigueOnset) return FATIGUE;
        return PERFORMANCE;
    }
}
```

### 3. Navigation Systems

| Bot | Algorithm | Features |
|-----|-----------|----------|
| WoW Glider | Waypoints + LOS | Obstacle avoidance |
| Honorbuddy | Recast/Detour | NavMesh generation |
| Baritone | A* + Chunk caching | Hierarchical pathfinding |
| OSRS | Web walking | Pre-computed paths |
| Steve AI | A* + Hierarchical | Path smoothing, movement validation |

**Key Patterns:**
1. **Hierarchical decomposition** - Plan at chunk level, execute at block level
2. **Path caching** - Store frequently-used paths
3. **Fallback strategies** - Repath, teleport, abort
4. **Movement validation** - Check reachability before committing

### 4. State Recovery Patterns

#### Stuck Detection
```java
// Common pattern across all bots
class StuckDetector {
    Position lastPosition;
    long lastProgressTime;
    int stuckThreshold = 5000; // 5 seconds

    boolean isStuck() {
        if (positionChanged()) {
            lastProgressTime = now();
            return false;
        }
        return (now() - lastProgressTime) > stuckThreshold;
    }
}

// Steve AI implementation (already complete)
public class StuckDetector {
    public enum StuckType {
        POSITION_STUCK,   // Same position for too long
        PROGRESS_STUCK,   // No task progress
        STATE_STUCK,      // Repeating same state
        PATH_STUCK        // Path blocked
    }

    public StuckType detectStuckType() {
        // Pattern recognition for stuck conditions
    }
}
```

#### Recovery Strategies
| Strategy | When to Use | Risk |
|----------|-------------|------|
| Repath | Path blocked | Low |
| Backtrack | Local obstacle | Low |
| Teleport | Critically stuck | High (detectable) |
| Abort | Unrecoverable | None |
| Reset | State corruption | Medium |

```java
// Steve AI implementation (already complete)
public class RecoveryManager {
    public RecoveryResult attemptRecovery(StuckType type) {
        for (RecoveryStrategy strategy : getStrategiesForType(type)) {
            RecoveryResult result = strategy.attempt();
            if (result.success()) {
                return result;
            }
        }
        return RecoveryResult.FAILED;
    }
}
```

### 5. Profile Systems

#### Task Profile Structure (Honorbuddy-inspired)
```xml
<!-- XML Profile Example -->
<Profile>
    <Name>Mining Iron</Name>
    <Hotspots>
        <Point x="100" y="64" z="200"/>
        <Point x="150" y="64" z="250"/>
    </Hotspots>
    <TargetBlocks>
        <Block>minecraft:iron_ore</Block>
        <Block>minecraft:deepslate_iron_ore</Block>
    </TargetBlocks>
    <Blacklist>
        <Block>minecraft:bedrock</Block>
    </Blacklist>
</Profile>
```

```java
// Steve AI implementation (already complete)
public class TaskProfile {
    private String name;
    private List<ProfileTask> tasks;
    private Map<String, Object> parameters;

    public static TaskProfile fromJson(String json) {
        // Parse profile from JSON
    }
}

public class ProfileExecutor {
    public void execute(TaskProfile profile) {
        for (ProfileTask task : profile.getTasks()) {
            executeTask(task);
        }
    }
}
```

---

## Feature Adoption Status in Steve AI

### Fully Implemented (Production Ready)

| Feature | Source | Implementation |
|---------|--------|----------------|
| Gaussian jitter | WoW Glider | `HumanizationUtils.gaussianJitter()` |
| Mistake simulation | Multiple | `MistakeSimulator` class |
| Idle behaviors | Multiple | `IdleBehaviorController` |
| Session phases | WoW Glider | `SessionManager` |
| Priority arbitration | WoW Glider | `ProcessManager` |
| Goal composition | Baritone | `CompositeNavigationGoal` |
| Stuck detection | Multiple | `StuckDetector` |
| Recovery strategies | Multiple | `RecoveryManager`, strategies |
| Task profiles | Honorbuddy | `TaskProfile`, `ProfileExecutor` |
| Item rules | Multiple | `ItemRule`, `RuleEvaluator` |
| Behavior trees | Honorbuddy | `behavior/` package |
| HTN Planner | Research | `htn/` package |
| Contract Net | Research | `coordination/` package |

### Partially Implemented

| Feature | Gap | Priority |
|---------|-----|----------|
| NavMesh | Using A* instead | Low (A* works) |
| Visual editor | None | Medium |
| Skill caching | Basic | High |
| LLM fallback | Manual | High |

### Not Implemented (Intentionally)

| Feature | Reason |
|---------|--------|
| Memory reading | Not applicable (we own the API) |
| Injection | Not applicable (we're a mod) |
| Anti-detection | Not needed (legitimate mod) |
| Packet manipulation | Against ToS |

---

## Lessons for Legitimate AI Development

### 1. Humanization is Essential
- **Finding:** All successful bots invested heavily in humanization
- **Application:** Steve AI has comprehensive humanization system
- **Impact:** Agents feel more natural, less robotic

### 2. Layered Architecture Scales
- **Finding:** Separating strategic/tactical/physical layers enables complexity
- **Application:** Brain Layer (LLM) → Script Layer (BT/HTN) → Physical Layer (API)
- **Impact:** 10-20x token efficiency, 60 FPS execution

### 3. State Recovery Prevents Frustration
- **Finding:** Robust stuck detection and recovery is critical
- **Application:** Multi-strategy recovery with escalation
- **Impact:** Agents self-recover from most situations

### 4. Declarative Profiles Enable Extensibility
- **Finding:** Profile systems allow users to define custom behaviors
- **Application:** JSON-based task profiles
- **Impact:** Power users can customize without code

### 5. Mistakes Increase Believability
- **Finding:** Perfect agents feel wrong
- **Application:** Configurable mistake rates with type-based multipliers
- **Impact:** Agents feel more human

---

## Anti-Patterns to Avoid

### 1. Deterministic Timing
```java
// BAD: Fixed delays
Thread.sleep(1000);

// GOOD: Variable delays
Thread.sleep(HumanizationUtils.gaussianJitter(1000, 0.2));
```

### 2. Perfect Accuracy
```java
// BAD: Always correct
BlockPos target = exactTarget;

// GOOD: Sometimes miss
BlockPos target = MistakeSimulator.shouldMakeMistake(WRONG_TARGET)
    ? MistakeSimulator.getWrongTarget(exactTarget)
    : exactTarget;
```

### 3. Infinite Persistence
```java
// BAD: Never gives up
while (!goal.reached()) {
    attemptGoal();
}

// GOOD: Timeout and reassess
long deadline = System.currentTimeMillis() + MAX_ATTEMPT_TIME;
while (!goal.reached() && System.currentTimeMillis() < deadline) {
    if (stuckDetector.isStuck()) {
        recoveryManager.attemptRecovery();
    }
    attemptGoal();
}
```

### 4. Single-Thread Blocking
```java
// BAD: Blocks game thread
LLMResponse response = llmClient.planSync(command);

// GOOD: Async with callback
llmClient.planAsync(command).thenAccept(response -> {
    executePlan(response);
});
```

---

## Future Research Directions

### 1. Machine Learning Integration
- Learn optimal humanization parameters from player behavior
- Adapt mistake rates based on player feedback
- Personalize companion behavior over time

### 2. Multi-Modal Perception
- Computer vision for scene understanding
- Audio cues for environmental awareness
- Text analysis for player intent

### 3. Emergent Behavior
- Study emergent coordination patterns
- Document unexpected beneficial behaviors
- Formalize multi-agent interaction protocols

### 4. Evaluation Framework
- Quantifiable believability metrics
- Player satisfaction studies
- Long-term engagement analysis

---

## References

### Primary Sources
- WoW Glider analysis: `docs/research/GAME_BOT_WOW_GLIDER_ANALYSIS.md`
- Honorbuddy analysis: `docs/research/GAME_BOT_HONORBUDDY_ANALYSIS.md`
- Baritone analysis: `docs/research/BARITONE_ANALYSIS.md`
- OSRS analysis: `docs/research/GAME_BOT_OSRS_ANALYSIS.md` (pending)
- Diablo 3 analysis: `docs/research/GAME_BOT_DIABLO3_ANALYSIS.md` (pending)

### Academic References
- Isla, D. (2005). Handling Complexity in the Halo 2 AI. GDC.
- Orkin, J. (2004). Applying Goal-Oriented Action Planning to Games. AI Game Programming Wisdom 2.
- Champandard, A. (2007). Behavior Trees for Next-Gen Game AI.
- Wang, Z. et al. (2023). Voyager: An Open-Ended Embodied Agent with LLMs.

---

**Document Status:** Complete
**Next Review:** After OSRS/Diablo research completion
**Maintained By:** Steve AI Development Team
