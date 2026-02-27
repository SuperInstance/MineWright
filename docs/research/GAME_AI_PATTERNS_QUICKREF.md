# Game AI Patterns - Quick Reference

**Date:** February 27, 2026
**Project:** Steve AI Foreman/Worker System

---

## Decision Matrix: Which AI Pattern to Use?

```
┌─────────────────────────────────────────────────────────────────┐
│                    Decision Flowchart                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Need to assign workers to tasks?                              │
│  └─→ UTILITY AI (score each worker-task pair)                  │
│                                                                  │
│  Need to react to urgent events?                               │
│  └─→ BEHAVIOR TREE (event-driven priority selection)           │
│                                                                  │
│  Have common, repeatable task patterns?                        │
│  └─→ HTN (hierarchical decomposition with caching)             │
│                                                                  │
│  Need natural player communication?                            │
│  └─→ DIALOGUE STATE MACHINE + LLM (hybrid approach)            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Pattern Comparison Table

| Pattern | Best For | Complexity | Performance | Maturity |
|---------|----------|------------|-------------|----------|
| **Utility AI** | Worker assignment, scoring decisions | Medium | High (<0.5ms) | Production |
| **Behavior Trees** | Reactive decision making, prioritization | High | Medium (<1ms) | Production |
| **HTN** | Hierarchical task decomposition | High | High (cached) | Growing |
| **GOAP** | Emergent planning, unpredictable scenarios | Medium | Low (A* search) | Declining |
| **Dialogue FSM** | Player communication, conversation flow | Medium | High (<0.1ms) | Production |

---

## Quick Implementation Checklist

### Utility AI Implementation

```java
// 1. Define Response Curves
ResponseCurve inverseDistance = new InverseExponentialCurve(0.1);
ResponseCurve healthThreshold = new LogisticCurve(2.0, 0.3);
ResponseCurve linear = new LinearCurve(1.0, 0.0);

// 2. Create Utility Actions
UtilityAction assignMiner = new UtilityAction("assign_mining")
    .addConsideration("distance_to_ore", inverseDistance, 1.0, ctx -> ctx.getOreDistance())
    .addConsideration("worker_available", linear, 1.0, ctx -> ctx.getAvailableWorkers())
    .addConsideration("resource_need", linear, 0.8, ctx -> 1.0 - ctx.getResourcePct());

// 3. Select Best Action
UtilityAction best = utilitySystem.selectBestAction(context);
```

### Behavior Tree Implementation

```java
// 1. Build Tree Structure
SelectorNode root = new SelectorNode();
SequenceNode combatBranch = new SequenceNode();
combatBranch.addChild(new EnemyProximityCondition());
combatBranch.addChild(new AssignCombatAction());
root.addChild(combatBranch);

// 2. Tick the Tree
NodeStatus status = root.tick();

// 3. Handle Events
bt.onEvent(new CombatEvent());
```

### HTN Implementation

```java
// 1. Define Task Decomposition
HTNMethod buildHouse = HTNMethod.builder("build_house_basic", "build_house")
    .addSubtask(new HTNTask("gather", Type.PRIMITIVE))
    .addSubtask(new HTNTask("build", Type.PRIMITIVE))
    .precondition(ws -> ws.has("materials"))
    .build();

// 2. Decompose Task
List<Task> tasks = htnPlanner.decompose("build_house", worldState);

// 3. Execute or Cache
cache.put("build_house", tasks);
```

### Dialogue State Machine

```java
// 1. Define States
enum DialogueState {
    IDLE, PLANNING, EXECUTING, REPORTING
}

// 2. Define Transitions
public DialogueState getNextState(PlayerIntent intent) {
    return switch (intent) {
        case BUILD_REQUEST -> BUILD_PLANNING;
        case STATUS_QUERY -> REPORTING_STATUS;
        case CANCEL -> IDLE;
    };
}

// 3. Handle Input
stateMachine.handlePlayerInput(playerMessage);
```

---

## Response Curve Formulas

### Linear Curve
```
f(x) = m × x + b
Use: Direct proportional relationships
```

### Logistic Curve (S-Curve)
```
f(x) = 1 / (1 + e^(-k × (x - x₀)))
Use: Threshold-based decisions (health, inventory levels)
```

### Inverse Exponential
```
f(x) = e^(-decay_rate × x)
Use: Distance decay (closer = better)
```

### Binary
```
f(x) = 1 if x > 0 else 0
Use: Boolean conditions (has item, can place)
```

---

## Common Worker Utility Considerations

| Consideration | Curve | Weight | Input |
|--------------|-------|--------|-------|
| Distance to task | Inverse Exponential | 1.0 | worker.distanceTo(task) |
| Skill match | Linear | 0.8 | worker.skill == task.type |
| Current load | Inverse Linear | 0.6 | 1.0 - worker.getLoad() |
| Has materials | Binary | 0.5 | worker.hasRequiredItems() |
| Worker availability | Linear | 1.0 | worker.isAvailable() |

---

## Behavior Tree Template for Foreman

```
ROOT (Selector)
├── Sequence: COMBAT RESPONSE (Priority 1)
│   ├── Condition: Enemy within 16 blocks?
│   ├── Condition: Has combat worker?
│   └── Action: Assign combat task
│
├── Sequence: RESOURCE SHORTAGE (Priority 2)
│   ├── Condition: Resource < threshold?
│   ├── Condition: Has available worker?
│   └── Action: Assign gathering task
│
├── Sequence: PLAYER COMMAND (Priority 3)
│   ├── Condition: Has pending command?
│   └── Action: Process command
│
└── Sequence: BUILD TASK (Priority 4)
    ├── Condition: Has queued build?
    ├── Condition: Has materials?
    └── Action: Assign build task
```

---

## HTN Task Decomposition Example

```
build_house (Compound)
├── Method: build_house_basic
│   ├── Preconditions: has_materials, has_clear_space
│   └── Subtasks:
│       ├── gather_materials (Compound)
│       │   ├── Method: gather_from_nearby
│       │   │   └── Subtasks: [pathfind, mine, return]
│       │   └── Method: gather_from_storage
│       │       └── Subtasks: [take_from_chest]
│       ├── clear_area (Primitive)
│       ├── lay_foundation (Primitive)
│       ├── build_walls (Primitive)
│       └── add_roof (Primitive)
```

---

## Performance Benchmarks

| Operation | Target | Notes |
|-----------|--------|-------|
| Utility scoring (single action) | <0.1ms | 5-10 considerations |
| Worker assignment (10 workers) | <1ms | Score all workers |
| Behavior tree tick | <1ms | Full tree traversal |
| HTN decomposition (cached) | <0.5ms | Memory lookup |
| LLM planning (uncached) | 3-5s | Async, non-blocking |
| Dialogue state transition | <0.1ms | Simple state check |

---

## Integration Points

```
┌─────────────────────────────────────────────────────────┐
│                    ForemanEntity                        │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  UtilitySystem → WorkerAssignmentSystem                 │
│      ↓                                                   │
│  ForemanBehaviorTree → Reactive task selection          │
│      ↓                                                   │
│  HTNPlanner → Task decomposition (with LLM fallback)    │
│      ↓                                                   │
│  DialogueStateMachine → Player communication            │
│      ↓                                                   │
│  WorkerManager → Task assignment & execution            │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

---

## Key Takeaways

### Do's
- ✅ Use Utility AI for worker assignment (highest ROI)
- ✅ Use Behavior Trees for reactive decision making
- ✅ Use HTN for common, repeatable task patterns
- ✅ Use Dialogue FSM + LLM for natural communication
- ✅ Cache everything (plans, scores, decompositions)
- ✅ Profile performance (<1ms per tick goal)

### Don'ts
- ❌ Don't use GOAP for structured building (HTN is better)
- ❌ Don't tick behavior trees continuously (use event-driven)
- ❌ Don't skip caching (40-60% API cost savings possible)
- ❌ Don't use LLM for simple queries (state machine is faster)
- ❌ Don't hard-code worker assignments (use utility scoring)

---

## Implementation Timeline

| Phase | Duration | Deliverables |
|-------|----------|--------------|
| **Phase 1**: Utility AI | 2 weeks | Worker assignment system |
| **Phase 2**: Behavior Trees | 2 weeks | Reactive task selection |
| **Phase 3**: Dialogue FSM | 2 weeks | Natural communication |
| **Phase 4**: HTN Planning | 3 weeks | Cached task patterns |
| **Phase 5**: Polish | 2 weeks | Optimization & docs |

**Total: 11 weeks**

---

## Related Documentation

- `GAME_AI_PATTERNS.md` - Full research document
- `HTN_PLANNER_DESIGN.md` - HTN implementation details
- `RESEARCH_GAME_AI_2025.md` - Industry AI trends
- `WORKER_ROLE_SYSTEM.md` - Current worker system

---

**Quick Reference Version:** 1.0
**Last Updated:** February 27, 2026
