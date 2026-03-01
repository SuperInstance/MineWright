# Script Layer Learning System

## Abstract

This document designs a learning system for the "One Abstraction Away" architecture, where automatic scripts handle fast responses while LLMs handle thoughtful planning. The system enables AI agents (Foremen) to learn efficient behaviors through experience, moving from deliberate LLM-planned actions to cached automatic responses. This design draws on cognitive science research into System 1/System 2 processing and skill acquisition.

---

## Table of Contents

1. [Theoretical Foundations](#theoretical-foundations)
2. [System Overview](#system-overview)
3. [Learning Pipeline](#learning-pipeline)
4. [Minecraft Examples](#minecraft-examples)
5. [Feedback Loop Design](#feedback-loop-design)
6. [Architecture Integration](#architecture-integration)
7. [Implementation Roadmap](#implementation-roadmap)
8. [Research Sources](#research-sources)

---

## Theoretical Foundations

### System 1 and System 2 Thinking (Kahneman)

Based on Daniel Kahneman's dual-process theory from "Thinking, Fast and Slow":

**System 1 (Fast/Automatic Processing):**
- Operates unconsciously and rapidly (~100 milliseconds)
- Relies on intuition, emotions, memory, and experience
- Handles familiar situations with pattern recognition
- Prone to biases, shortcuts, and systematic errors
- Examples: reading words, recognizing faces, intuitive reactions

**System 2 (Slow/Controlled Processing):**
- Conscious, deliberate, and slow (requires seconds)
- Demands attention and mental effort
- Lazy by default—only activates when System 1 needs help
- Handles complex calculations, logical reasoning, and planning
- Responsible for self-control and overriding System 1's impulses
- Easily fatigued and cannot run continuously

**Key Insight:** System 2 can train System 1 through repeated practice. As behaviors are learned, they transition from System 2 (controlled) to System 1 (automatic).

### Anderson's Three-Stage Skill Acquisition Theory

John Anderson's cognitive psychology framework describes how skills progress from declarative to procedural knowledge:

| Stage | Characteristics | Cognitive Load | Examples |
|-------|-----------------|----------------|----------|
| **Cognitive Stage** | Understand problem structure, identify steps/operators | High - conscious effort | First time mining iron ore |
| **Associative Stage** | Declarative knowledge -> procedural knowledge, error reduction | Medium - still attentive | Mining patterns forming |
| **Automatic Stage** | Minimal conscious effort, rapid execution, resistant to interference | Low - effortless | Automatic strip mining |

**Key Insight:** The transition from declarative to procedural knowledge (proceduralization) is the core of automaticity development.

### Neural Efficiency Research

Recent cognitive neuroscience findings:
- **Early Learning:** High prefrontal cortex activation (working memory, conscious decision-making)
- **Mastery:** Reduced brain activation, greater efficiency
- **Overlearning:** Practice beyond initial mastery to achieve automaticity
- **Contextual Interference:** Random practice enhances long-term learning vs blocked practice

**Key Insight:** Automaticity is achieved through neural pathway reinforcement—repeated successful execution strengthens connections until behavior requires minimal conscious oversight.

---

## System Overview

### Philosophy: "One Abstraction Away"

The Script Layer Learning System implements a hierarchical cognitive architecture:

```
User Command
    |
    v
[Complexity Analysis]
    |
    +---> [Known Pattern?] ---> [Script Cache] ---> FAST EXECUTION (System 1)
    |                                                    |
    |                                                    v
    |                                              Tick-based Action
    |
    +---> [Novel Situation?] ---> [LLM Planner] ---> SLOW EXECUTION (System 2)
                                                       |
                                                       v
                                                 Generate Script
                                                       |
                                                       v
                                                 [Learning Loop]
```

### Key Components

| Component | Purpose | Cognitive Analog |
|-----------|---------|------------------|
| **Script Cache** | Store learned patterns as executable scripts | Procedural memory |
| **Pattern Matcher** | Recognize when cached scripts apply | Pattern recognition |
| **Script Generator** | Create scripts from LLM plans | Proceduralization |
| **Performance Profiler** | Collect execution metrics | Metacognition |
| **Learning Validator** | Determine when to promote to cache | Consolidation |
| **Script Refiner** | Improve scripts through experience | Skill refinement |

### Learning States

Scripts progress through three stages mirroring Anderson's theory:

```java
public enum ScriptLearningState {
    /**
     * Script generated by LLM, being tested.
     * Requires full validation before caching.
     */
    PROPOSED,      // Cognitive Stage

    /**
     * Script showing promise, accumulating evidence.
     * Refining parameters through experience.
     */
    LEARNING,      // Associative Stage

    /**
     * Script proven effective, cached for fast execution.
     * Executes automatically without LLM involvement.
     */
    AUTOMATIC      // Automatic Stage
}
```

---

## Learning Pipeline

The learning pipeline consists of four phases that transform LLM-planned behaviors into automatic scripts.

### Phase 1: Script Generation (Cognitive Stage)

**Trigger:** Novel situation with no matching cached script

**Process:**
1. LLM receives command and generates task plan
2. `ScriptExtractor` analyzes plan for reusable patterns
3. Generate candidate script with parameters
4. Store in `PROPOSED` state with metadata

**Example Input:**
```
User: "Mine 64 iron ore"
LLM Plan:
  - pathfind to coordinates [100, 64, -200] (known cave)
  - execute strip mining pattern at Y=58
  - continue until inventory full
```

**Example Output Script:**
```java
@ScriptPattern(name="strip_mining_iron", state=PROPOSED)
class StripMiningScript {
    // Parameters learned from this instance
    int targetY = 58;
    int spacing = 3;
    int targetQuantity = 64;

    // Generated from LLM task sequence
    TaskSequence sequence = TaskSequence.of(
        Task.pathfind(100, 64, -200),
        Task.mine("strip_pattern", Map.of(
            "y", targetY,
            "spacing", spacing,
            "until", "inventory_full"
        ))
    );
}
```

### Phase 2: Execution & Metrics Collection

**Trigger:** Script executed (either proposed or learning)

**Process:**
1. Execute script tick-by-tick (non-blocking)
2. `PerformanceProfiler` collects metrics:
   - **Success:** Did we achieve the goal?
   - **Efficiency:** Time taken vs expected
   - **Resource Cost:** Tools used, blocks broken
   - **Errors:** Failures, replans required
3. Store execution record for analysis

**Metrics Schema:**
```java
public class ScriptExecutionMetrics {
    private boolean success;
    private long executionTimeTicks;
    private int blocksBroken;
    private int toolsUsed;
    private int replanCount;
    private double efficiency; // 0.0 to 1.0
    private List<String> errors;

    // Derived metrics
    public double getOverallScore() {
        double successWeight = 0.5;
        double efficiencyWeight = 0.3;
        double errorPenalty = 0.2;

        double successScore = success ? 1.0 : 0.0;
        double errorScore = Math.max(0, 1.0 - (replanCount * 0.25));

        return (successScore * successWeight) +
               (efficiency * efficiencyWeight) +
               (errorScore * errorPenalty);
    }
}
```

### Phase 3: Analysis & Refinement (Associative Stage)

**Trigger:** Execution completes, metrics available

**Process:**
1. `LearningValidator` evaluates execution metrics
2. If metrics exceed threshold, promote to `LEARNING` state
3. `ScriptRefiner` analyzes multiple executions to optimize:
   - Parameter tuning (e.g., optimal Y level for mining)
   - Sequence optimization (remove redundant steps)
   - Error handling (add recovery patterns)
4. Generate refined script version

**Refinement Example:**
```java
// After 10 executions of strip_mining_iron script
class ScriptRefiner {
    public Script refine(Script original, List<ScriptExecutionMetrics> history) {
        // Analyze optimal Y level
        IntSummaryStatistics yStats = history.stream()
            .mapToInt(m -> m.getParameter("y"))
            .summaryStatistics();

        // If Y=58 performed best (90% success rate vs 70% at Y=60)
        int optimalY = yStats.getAverage() < 59 ? 58 : 60;

        // Optimize spacing (3 blocks = 85% efficiency, 2 blocks = 70%)
        int optimalSpacing = history.stream()
            .filter(m -> m.getParameter("spacing") == 3)
            .mapToDouble(ScriptExecutionMetrics::getEfficiency)
            .average()
            .orElse(0.0) > 0.8 ? 3 : 2;

        // Generate refined version
        return original.withParameters(Map.of(
            "y", optimalY,
            "spacing", optimalSpacing
        ));
    }
}
```

### Phase 4: Promotion to Automatic (Automatic Stage)

**Trigger:** Consistent high performance in `LEARNING` state

**Promotion Criteria:**
- Minimum executions: 5-10 instances
- Success rate: >= 85%
- Efficiency score: >= 0.8
- Consistency: Standard deviation < 0.15
- Recent performance: Last 3 executions all successful

**Process:**
1. Validate all promotion criteria met
2. Promote script to `AUTOMATIC` state
3. Index in `ScriptCache` for fast pattern matching
4. Update `PromptBuilder` to inform LLM about available scripts

**Cache Architecture:**
```java
public class ScriptCache {
    // Semantic indexing for pattern matching
    private SemanticIndex<Script> semanticIndex;

    // Parameter-based lookup for exact matches
    private Map<ScriptSignature, Script> exactCache;

    // Hierarchical pattern matching
    private PatternHierarchy patternHierarchy;

    public Optional<Script> findMatch(CommandContext context) {
        // 1. Check for exact parameter match
        Optional<Script> exact = exactCache.get(context.getSignature());
        if (exact.isPresent()) return exact;

        // 2. Check semantic similarity
        Optional<Script> semantic = semanticIndex.findSimilar(
            context.getEmbedding(),
            threshold = 0.85
        );
        if (semantic.isPresent()) return semantic;

        // 3. Check hierarchical patterns
        return patternHierarchy.findMatch(context);
    }
}
```

---

## Minecraft Examples

### Example 1: Learning Efficient Mining Patterns

**Scenario:** Agent learns optimal strip mining for iron ore

**Phase 1 - Initial Learning (Cognitive):**
```
User: "I need iron for tools"
LLM Plan:
  - Pathfind to nearest cave (detected at [120, 64, -180])
  - Descend to Y=58 (optimal iron level)
  - Execute strip mining pattern with 3-block spacing
  - Continue until 64 iron ore collected
```

Generated Script:
```java
@Script(name="strip_mining_iron_v1", state=PROPOSED)
public class StripMiningIronScript extends BaseScript {
    @Parameter int targetY = 58;
    @Parameter int spacing = 3;
    @Parameter int targetQuantity = 64;

    @Override
    protected TaskSequence generateTasks(ForemanEntity foreman) {
        return TaskSequence.of(
            Task.pathfindToNearestCave(),
            Task.descendTo(targetY),
            Task.stripMining(Map.of(
                "spacing", spacing,
                "direction", "north",
                "until", quantityReached("iron_ore", targetQuantity)
            ))
        );
    }
}
```

**Phase 2 - Execution Metrics:**
```
Execution 1:
  - Success: true (64 iron collected)
  - Time: 847 ticks (42 seconds)
  - Efficiency: 0.72 (some backtracking)
  - Errors: 0

Execution 2:
  - Success: true (64 iron collected)
  - Time: 723 ticks (36 seconds) - improved pathfinding
  - Efficiency: 0.81
  - Errors: 0

Execution 3-5:
  - Success rate: 100%
  - Average efficiency: 0.79
  - Average time: 690 ticks
```

**Phase 3 - Refinement:**
Analysis reveals optimal parameters:
- Y=58 performs 20% better than Y=60 for iron
- 3-block spacing balances coverage vs efficiency
- North-south corridors better than east-west (chunk loading)

Refined Script:
```java
@Script(name="strip_mining_iron_v2", state=LEARNING)
public class StripMiningIronScriptRefined extends BaseScript {
    @Parameter int targetY = 58;  // Optimized through learning
    @Parameter int spacing = 3;   // Validated efficiency
    @Parameter int targetQuantity = 64;
    @Parameter String direction = "north";  // Chunk-aware direction

    @Override
    protected void handleCommonError(String error) {
        // Learned recovery pattern
        if (error.equals("lava_encountered")) {
            // Place cobblestone and continue adjacent
            sequence.insertNext(Task.placeCobblestoneBarrier());
            sequence.insertNext(Task.adjustDirection("perpendicular"));
        }
    }
}
```

**Phase 4 - Automatic Promotion:**
After 10 successful executions with 92% average efficiency:
```
Script promoted to AUTOMATIC state
Cached with semantic signature: "mine_iron_ore_underground_fast"
Pattern matcher: When user says "need iron" or "mine iron", use cached script
```

**Result:** Future "iron" commands execute in <50ms (script lookup) vs 3-5 seconds (LLM planning)

### Example 2: Learning Building Sequences

**Scenario:** Agent learns to build efficient mob grinders

**Phase 1 - Initial Script Generation:**
```
User: "Build a mob grinder at spawn"
LLM Plan:
  - Clear 20x20 area at Y=200
  - Build spawning platform (15x15)
  - Add water channels (4-sided)
  - Build collection system (hopper + chest)
  - Add lighting to prevent spawn interference
```

Generated Script:
```java
@Script(name="mob_grinder_basic", state=PROPOSED)
public class MobGrinderScript extends BaseScript {
    @Parameter int platformSize = 15;
    @Parameter int height = 200;
    @Parameter boolean includeCollection = true;

    @Override
    protected TaskSequence generateTasks(ForemanEntity foreman) {
        return TaskSequence.of(
            Task.clearArea(platformSize + 5, height),
            Task.buildPlatform("stone", platformSize),
            Task.buildWaterChannels(platformSize),
            includeCollection ? Task.buildCollectionSystem() : Task.skip(),
            Task.addLighting("torch", platformSize)
        );
    }
}
```

**Phase 2-3 - Learning & Refinement:**
Through multiple executions, agent learns:
- 21x21 platform (odd number) centers better with 4-sided water
- Height 201 reduces spawn interference from ground
- Trapdoors instead of glass for spawning floor (better mob spawning)
- Sign-based water flow control more reliable than source blocks

**Phase 4 - Automatic Script:**
```java
@Script(name="mob_grinder_optimized", state=AUTOMATIC)
public class MobGrinderOptimizedScript extends BaseScript {
    // All parameters optimized through learning
    @Parameter int platformSize = 21;  // Learned: odd number centers better
    @Parameter int height = 201;       // Learned: reduces interference
    @Parameter String floorBlock = "oak_trapdoor";  // Learned: better spawns
    @Parameter String flowControl = "sign";  // Learned: more reliable

    @Precondition("world_spawn_distance > 50")  // Only use far from spawn
    @Precondition("block_quantity_smooth_stone >= 500")

    @Override
    protected TaskSequence generateTasks(ForemanEntity foreman) {
        return TaskSequence.of(
            Task.clearArea(platformSize + 5, height),
            Task.buildPlatform(floorBlock, platformSize),
            Task.buildWaterChannels(platformSize, flowControl),
            Task.buildCollectionSystem("hopper", "double_chest"),
            Task.addLighting("sea_lantern", platformSize),  // Learned: better light
            Task.addSpawningPlatform("solid", platformSize - 2)  // Learned: edge spawns
        );
    }
}
```

### Example 3: Learning Combat Responses

**Scenario:** Agent learns to respond to zombie attacks

**Phase 1 - Initial Learning:**
```
Situation: Zombie detected within 5 blocks
LLM Response (slow):
  - Equip best weapon
  - Retreat to tactical position
  - Attack until zombie dead
  - Check for more threats
```

**Phase 2-3 - Pattern Recognition:**
Through combat encounters, agent learns:
- Sword > axe for single targets (speed)
- Shield blocks reduce damage by 60%
- Knockback critical when overwhelmed
- Height advantage increases hit rate by 30%

**Phase 4 - Combat Script:**
```java
@Script(name="combat_zombie_melee", state=AUTOMATIC, trigger=COMBAT_TRIGGERED)
public class ZombieCombatScript extends BaseScript {
    @TriggerCondition("entity_type=zombie", "distance<5")

    @Override
    protected TaskSequence generateTasks(ForemanEntity foreman) {
        Entity zombie = getContext().getNearestThreat();

        // Learned tactics
        boolean canRetreat = findTacticalPosition().isPresent();
        boolean hasShield = inventory.hasItem("shield");
        boolean hasHeightAdvantage = getPosition().getY() > zombie.getY() + 2;

        TaskSequence combat = TaskSequence.of(
            Task.equipBestWeapon(),
            hasShield ? Task.equipShieldOffhand() : Task.skip(),
            canRetreat && !hasHeightAdvantage ?
                Task.retreatToTacticalPosition() : Task.skip(),
            Task.attackTarget(zombie, "until_dead"),
            Task.scanForThreats()
        );

        return combat;
    }

    @Override
    protected void onTakingDamage(DamageSource source) {
        // Learned: Critical response pattern
        if (getHealth() < 0.3) {
            sequence.insertNext(Task.emergencyRetreat());
            sequence.insertNext(Task.consumeFood("golden_apple"));
        }
    }
}
```

**Result:** Combat response time drops from 2-3 seconds (LLM) to <100ms (automatic script)

---

## Feedback Loop Design

### Success Metrics

Scripts are evaluated across multiple dimensions:

| Metric | Formula | Target for Promotion |
|--------|---------|---------------------|
| **Success Rate** | successful_executions / total_executions | >= 85% |
| **Efficiency** | theoretical_min_time / actual_time | >= 0.8 |
| **Resource Efficiency** | optimal_resources / actual_resources | >= 0.75 |
| **Error Rate** | 1 - (errors / executions) | <= 0.15 |
| **Consistency** | 1 - std_dev(efficiency) | >= 0.85 |
| **Recency** | weighted_average(last_5, 0.7) | >= 0.8 |

**Composite Score:**
```java
public double calculatePromotionScore(Script script) {
    List<ScriptExecutionMetrics> history = script.getExecutionHistory();

    double successRate = calculateSuccessRate(history);
    double efficiency = calculateAverageEfficiency(history);
    double resourceScore = calculateResourceEfficiency(history);
    double consistency = calculateConsistency(history);
    double recency = calculateRecencyWeightedScore(history);

    return (successRate * 0.3) +
           (efficiency * 0.25) +
           (resourceScore * 0.15) +
           (consistency * 0.15) +
           (recency * 0.15);
}
```

### Failure Analysis

Scripts that fail promote analysis and refinement:

```java
public class FailureAnalyzer {
    public List<RefinementSuggestion> analyzeFailure(
        Script script,
        ScriptExecutionMetrics failedExecution
    ) {
        List<RefinementSuggestion> suggestions = new ArrayList<>();

        // Analyze failure type
        if (failedExecution.getErrorType() == ErrorType.TIMEOUT) {
            suggestions.add(new RefinementSuggestion(
                "add_timeout_handler",
                "Add timeout detection and recovery"
            ));
        }

        if (failedExecution.getErrorType() == ErrorType.RESOURCE_EXHAUSTED) {
            suggestions.add(new RefinementSuggestion(
                "check_resources_precondition",
                "Add resource availability check before execution"
            ));
        }

        if (failedExecution.getReplanCount() > 2) {
            suggestions.add(new RefinementSuggestion(
                "add_error_recovery",
                "Add recovery patterns for common failure modes"
            ));
        }

        return suggestions;
    }
}
```

### Automatic Demotion

Scripts in `AUTOMATIC` state are continuously monitored:

```java
public class ScriptHealthMonitor {
    public void checkAutomaticScripts() {
        for (Script script : scriptCache.getAutomaticScripts()) {
            ScriptHealth health = assessHealth(script);

            if (health.getRecentSuccessRate() < 0.7) {
                // Demote to LEARNING for retraining
                script.setState(ScriptLearningState.LEARNING);
                LOGGER.warn("Script '{}' demoted due to poor performance: {}",
                    script.getName(), health.getDetails());
            }

            if (health.getStaleness() > Duration.ofDays(30)) {
                // Mark for potential deletion
                script.setStale(true);
                LOGGER.info("Script '{}' marked stale (not used in 30 days)",
                    script.getName());
            }
        }
    }
}
```

### When to Use LLM vs Script

Decision flow:

```java
public class ScriptSelector {
    public ExecutionPlan selectExecutionStrategy(CommandContext command) {
        // 1. Check for exact cached script match
        Optional<Script> exactMatch = scriptCache.findExactMatch(command);
        if (exactMatch.isPresent() && exactMatch.get().isAutomatic()) {
            return ExecutionPlan.script(exactMatch.get());
        }

        // 2. Check for semantic similarity
        Optional<Script> semanticMatch = scriptCache.findSemanticMatch(command);
        if (semanticMatch.isPresent() && semanticMatch.get().isAutomatic()) {
            double confidence = semanticMatch.get().getParameterFitScore(command);
            if (confidence > 0.9) {
                return ExecutionPlan.script(semanticMatch.get());
            }
        }

        // 3. Check for in-progress learning scripts
        Optional<Script> learningScript = scriptCache.findLearningScript(command);
        if (learningScript.isPresent()) {
            return ExecutionPlan.scriptWithValidation(
                learningScript.get(),
                "Using learning script - collecting metrics"
            );
        }

        // 4. No suitable script - use LLM
        return ExecutionPlan.llm(command);
    }
}
```

---

## Architecture Integration

### Integration with Existing Components

The Script Layer Learning System integrates with existing MineWright components:

#### 1. ActionExecutor Integration

```java
public class ActionExecutor {
    private ScriptCache scriptCache;
    private ScriptSelector scriptSelector;

    public void processNaturalLanguageCommand(String command) {
        // NEW: Check script cache before LLM
        CommandContext context = CommandContext.from(foreman, command);
        ExecutionPlan plan = scriptSelector.selectExecutionStrategy(context);

        if (plan.isScriptBased()) {
            // Execute script directly (fast path)
            executeScript(plan.getScript());
        } else {
            // Use LLM (slow path) - existing behavior
            processWithLLM(command);
        }
    }

    private void executeScript(Script script) {
        stateMachine.transitionTo(AgentState.EXECUTING);

        // Generate task sequence from script
        TaskSequence tasks = script.generateTaskSequence(foreman);

        // Queue tasks for execution
        taskQueue.addAll(tasks.getTasks());

        // Track script execution for learning
        script.recordExecution(new ScriptExecutionContext(foreman));
    }
}
```

#### 2. PromptBuilder Enhancement

```java
public class PromptBuilder {
    public static String buildSystemPrompt() {
        String basePrompt = loadBasePrompt();

        // NEW: Inform LLM about available automatic scripts
        String scriptInfo = buildScriptInfoSection();

        return basePrompt + "\n\n" + scriptInfo;
    }

    private static String buildScriptInfoSection() {
        List<Script> automaticScripts = ScriptCache.getInstance()
            .getAutomaticScripts();

        if (automaticScripts.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# Available Automatic Scripts\n\n");
        sb.append("The following behaviors are handled automatically by scripts:\n\n");

        for (Script script : automaticScripts) {
            sb.append("- ")
              .append(script.getDescription())
              .append(" (pattern: ")
              .append(script.getSignature())
              .append(")\n");
        }

        sb.append("\nYou should NOT generate tasks for these patterns. ")
          .append("They will be executed automatically.\n");

        return sb.toString();
    }
}
```

#### 3. ForemanMemory Extension

```java
public class ForemanMemory {
    private ScriptExecutionHistory scriptHistory;

    public void recordScriptExecution(Script script, ScriptExecutionMetrics metrics) {
        scriptHistory.record(script.getName(), metrics);
    }

    public List<ScriptExecutionMetrics> getScriptHistory(String scriptName) {
        return scriptHistory.getHistory(scriptName);
    }

    public void saveToNBT(CompoundTag tag) {
        super.saveToNBT(tag);

        // Save script history
        CompoundTag scriptHistoryTag = scriptHistory.toNBT();
        tag.put("ScriptHistory", scriptHistoryTag);
    }

    public void loadFromNBT(CompoundTag tag) {
        super.loadFromNBT(tag);

        // Load script history
        CompoundTag scriptHistoryTag = tag.getCompound("ScriptHistory");
        scriptHistory = ScriptExecutionHistory.fromNBT(scriptHistoryTag);
    }
}
```

#### 4. AgentStateMachine Extension

```java
public enum AgentState {
    IDLE,
    PLANNING,
    EXECUTING_SCRIPT,  // NEW: Executing cached script
    EXECUTING_LLM,     // NEW: Executing LLM-generated tasks
    LEARNING,          // NEW: Collecting metrics for learning
    COMPLETED,
    FAILED,
    PAUSED
}
```

### New Components Required

#### 1. Script Core Classes

```java
// Base script class
public abstract class BaseScript {
    protected ScriptLearningState state;
    protected Map<String, Object> parameters;
    protected TaskSequence taskSequence;
    protected List<ScriptExecutionMetrics> executionHistory;

    public abstract TaskSequence generateTaskSequence(ForemanEntity foreman);
    public abstract boolean matches(CommandContext context);
    public abstract double calculateFitScore(CommandContext context);
}

// Script cache with semantic indexing
public class ScriptCache {
    private Map<String, BaseScript> exactCache;
    private SemanticIndex<BaseScript> semanticCache;
    private Map<ScriptLearningState, List<BaseScript>> stateIndex;

    public Optional<BaseScript> findMatch(CommandContext context);
    public void promoteScript(BaseScript script, ScriptLearningState newState);
    public void demoteScript(BaseScript script, ScriptLearningState newState);
}

// Script generator from LLM plans
public class ScriptGenerator {
    public Optional<BaseScript> generateFromLLMPlan(
        ResponseParser.ParsedResponse llmPlan,
        CommandContext context
    );

    private TaskSequence extractReusablePattern(TaskSequence tasks);
    private Map<String, Object> extractParameters(TaskSequence tasks);
    private String generateSemanticSignature(TaskSequence tasks);
}
```

#### 2. Learning Components

```java
// Performance profiler
public class ScriptPerformanceProfiler {
    public ScriptExecutionMetrics profile(
        BaseScript script,
        ExecutionContext context,
        ActionResult result
    );

    private double calculateEfficiency(ExecutionContext context);
    private double calculateResourceUsage(ExecutionContext context);
    private List<String> extractErrors(ExecutionContext context);
}

// Learning validator
public class LearningValidator {
    public PromotionDecision evaluatePromotion(
        BaseScript script,
        List<ScriptExecutionMetrics> history
    );

    private boolean meetsSuccessThreshold(List<ScriptExecutionMetrics> history);
    private boolean meetsEfficiencyThreshold(List<ScriptExecutionMetrics> history);
    private boolean meetsConsistencyThreshold(List<ScriptExecutionMetrics> history);
}

// Script refiner
public class ScriptRefiner {
    public BaseScript refine(
        BaseScript original,
        List<ScriptExecutionMetrics> history
    );

    private Map<String, Object> optimizeParameters(
        Map<String, Object> original,
        List<ScriptExecutionMetrics> history
    );

    private TaskSequence optimizeSequence(
        TaskSequence original,
        List<ScriptExecutionMetrics> history
    );
}
```

#### 3. Semantic Indexing

```java
// Semantic index for pattern matching
public class SemanticIndex<T> {
    private TextEmbedder embedder;
    private VectorStore<IndexEntry<T>> vectorStore;

    public void index(String key, T value, String description);
    public Optional<T> findSimilar(String query, double threshold);
    public Optional<T> findSimilar(float[] embedding, double threshold);
}

// Index entry with metadata
public class IndexEntry<T> {
    private String key;
    private T value;
    private float[] embedding;
    private Map<String, Object> metadata;
}
```

---

## Implementation Roadmap

### Phase 1: Foundation (Weeks 1-2)

**Goal:** Basic script storage and retrieval

- [ ] Create `BaseScript` abstract class
- [ ] Implement `ScriptCache` with exact matching
- [ ] Create `ScriptLearningState` enum
- [ ] Add script metadata storage to `ForemanMemory`
- [ ] Basic NBT persistence for scripts

**Testing:** Unit tests for script caching and retrieval

### Phase 2: Script Generation (Weeks 3-4)

**Goal:** Extract scripts from LLM plans

- [ ] Implement `ScriptGenerator`
- [ ] Create pattern extraction logic
- [ ] Add parameter detection and extraction
- [ ] Generate semantic signatures
- [ ] Integrate with `ResponseParser`

**Testing:** Verify script generation from various LLM responses

### Phase 3: Metrics Collection (Weeks 5-6)

**Goal:** Track script execution performance

- [ ] Implement `ScriptPerformanceProfiler`
- [ ] Add metric collection to `ActionExecutor.tick()`
- [ ] Create `ScriptExecutionMetrics` class
- [ ] Store metrics in `ForemanMemory`
- [ ] Add metric visualization (optional)

**Testing:** Validate metric accuracy across different action types

### Phase 4: Learning & Promotion (Weeks 7-8)

**Goal:** Automatic script promotion and demotion

- [ ] Implement `LearningValidator`
- [ ] Create promotion criteria evaluator
- [ ] Add automatic promotion logic
- [ ] Implement failure analysis
- [ ] Create `ScriptRefiner` for optimization

**Testing:** End-to-end learning loop validation

### Phase 5: Semantic Indexing (Weeks 9-10)

**Goal:** Smart script matching

- [ ] Integrate `TextEmbedder` for script signatures
- [ ] Implement `SemanticIndex` for scripts
- [ ] Add similarity-based script lookup
- [ ] Tune similarity thresholds
- [ ] Optimize vector storage

**Testing:** Semantic matching accuracy across diverse commands

### Phase 6: Production Hardening (Weeks 11-12)

**Goal:** Robustness and performance

- [ ] Add comprehensive error handling
- [ ] Implement script health monitoring
- [ ] Add automatic demotion logic
- [ ] Optimize cache lookup performance
- [ ] Add monitoring and observability

**Testing:** Stress testing with complex scenarios

---

## Research Sources

### System 1/System 2 Theory

- [Kahneman, D. (2011). *Thinking, Fast and Slow* - Farrar, Straus and Giroux](https://www.douban.com/review/14369472/)
- [Dual-Process Theory Overview](https://m.toutiao.com/article/7271251039544885815/)
- [Application to AI and AGI Research](https://cloud.tencent.com/developer/article/2428956)

### Skill Acquisition & Automaticity

- [Procedural Knowledge and Memory](https://www.yuwenke.com/yisi/1220331.html)
- [Learning Automaticity Process](https://baijiahao.baidu.com/s?id=1844414206557310201)
- [Neural Basis of Motor Skills](https://m.blog.csdn.net/gaochao/article/152592423)
- [Memory-Based Automated Motor Control (2025 Study)](https://www.ebiotrade.com/newsf/2025-7/20250703121749788.htm)

### ACT Theory and Procedural Learning

- [Anderson's Three-Stage Skill Acquisition](https://m.wenda.so.com/q/1662429187218773)
- [Mental Skill Formation Stages](https://www.woyaosouti.com/x_ask/ucbgpZh8EzABM87Xja.html)
- [Testing Three-Stage Model in Second Language Acquisition](https://www.cambridge.org/core/journals/studies-in-second-language-acquisition/article/testing-the-threestage-model-of-second-language-skill-acquisition/DF879921EDE594E795CBD8C18A87E86E)

### Cognitive Efficiency

- [Cognitive Development and Writing Skills](https://www.researchgate.net/publication/26605689_Training_writing_skills_A_cognitive_development_perspective)
- [ACT Model and Second Language Automaticity](https://wk.baidu.com/view/1c98a854720abb68a98271fe910ef12d2bf9a912)

---

## Conclusion

The Script Layer Learning System implements a biologically-inspired learning mechanism that transforms LLM-planned behaviors into automatic scripts. By following the progression from System 2 (controlled) to System 1 (automatic) processing, agents become more efficient over time without sacrificing the flexibility of LLM-based reasoning for novel situations.

This design extends the "One Abstraction Away" philosophy by creating a dynamic boundary between automatic and deliberative processing—one that shifts as the agent learns and masters new behaviors. The result is an AI system that becomes progressively faster and more efficient while maintaining the ability to reason about novel situations.

---

**Document Version:** 1.0
**Last Updated:** 2025-01-10
**Author:** Orchestrator Agent (Steve AI Research Team)
**Status:** Design Document - Ready for Implementation
