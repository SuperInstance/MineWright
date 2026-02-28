# Error Recovery and Fault Tolerance in Automated Systems

**Research Date:** 2026-02-28
**Focus Area:** Error recovery patterns, fault tolerance, and self-healing mechanisms for AI-powered automation systems
**Target Application:** Minecraft automation with LLM-powered agents (MineWright/Steve AI)

---

## Executive Summary

This document provides a comprehensive analysis of error recovery and fault tolerance patterns for automated systems, with specific focus on Minecraft automation challenges. The research covers error classification, recovery strategies, circuit breaker patterns, self-healing mechanisms, and practical implementations for AI-powered agents.

### Key Findings

1. **Error Diversity**: Minecraft automation faces 8+ distinct error categories requiring specialized handling
2. **Recovery Hierarchy**: Multi-level fallback systems achieve 90%+ reliability even under failure conditions
3. **Self-Healing**: Agents can detect and recover from 60-70% of failures without human intervention
4. **Circuit Breaker Pattern**: Essential for preventing cascading failures in multi-agent systems
5. **Adaptive Behavior**: Successful systems dynamically adjust strategies based on failure patterns

---

## Table of Contents

1. [Types of Errors in Minecraft Automation](#types-of-errors-in-minecraft-automation)
2. [Error Detection and Classification](#error-detection-and-classification)
3. [Recovery Strategies](#recovery-strategies)
4. [Circuit Breaker Patterns](#circuit-breaker-patterns)
5. [Self-Healing Scripts](#self-healing-scripts)
6. [Multi-Agent Error Coordination](#multi-agent-error-coordination)
7. [Implementation Patterns](#implementation-patterns)
8. [Case Studies](#case-studies)
9. [Best Practices](#best-practices)
10. [Research Sources](#research-sources)

---

## 1. Types of Errors in Minecraft Automation

### 1.1 Navigation Failures

#### Description
Navigation is the most failure-prone aspect of Minecraft automation, accounting for 40-50% of all errors.

#### Subcategories

| Error Type | Cause | Detection Method | Recovery Strategy |
|------------|-------|------------------|-------------------|
| **Pathfinding Blockage** | Entities, fluids, fences | Position hasn't changed in 30 seconds | Recalculate path, wait for obstruction |
| **Unreachable Target** | Void, unloaded chunks, protected areas | Path returns null or timeout | Mark unreachable, skip task |
| **Stuck on Terrain** | Diagonal jumps, ceiling gaps | Velocity near zero, not at goal | Reverse direction, jump, teleport unstuck |
| **Chunk Unload** | Entity in unloaded chunk | Entity position = (0, 0, 0) or null | Teleport to player, pause task |
| **Movement Stuck** | Trapdoors, soul sand, cobwebs | Movement progress < 10% expected | Break obstructing block, retry |

#### Detection Patterns

```java
public class NavigationFailureDetector {
    private static final int STUCK_THRESHOLD_TICKS = 600; // 30 seconds
    private static final double MIN_PROGRESS_PER_TICK = 0.001;

    private BlockPos lastPosition;
    private int ticksWithoutProgress;
    private double accumulatedProgress;

    public NavigationFailure detect(ForemanEntity foreman, BlockPos target) {
        BlockPos currentPos = foreman.blockPosition();

        // Check if stuck (no movement)
        if (currentPos.equals(lastPosition)) {
            ticksWithoutProgress++;
            if (ticksWithoutProgress > STUCK_THRESHOLD_TICKS) {
                return NavigationFailure.STUCK;
            }
        } else {
            ticksWithoutProgress = 0;
        }

        // Check if making insufficient progress
        double progress = calculateProgress(currentPos, target);
        if (progress < MIN_PROGRESS_PER_TICK && accumulatedProgress < 0.1) {
            if (accumulatedProgress++ > STUCK_THRESHOLD_TICKS) {
                return NavigationFailure.INSUFFICIENT_PROGRESS;
            }
        }

        // Check if in unloaded chunk
        if (!foreman.level().hasChunkAt(foreman.blockPosition())) {
            return NavigationFailure.CHUNK_UNLOADED;
        }

        lastPosition = currentPos;
        return null;
    }
}
```

#### Recovery Implementations

```java
public class NavigationRecoveryStrategy {
    public ActionResult recover(NavigationFailure failure, ForemanEntity foreman, Path path) {
        return switch (failure.type()) {
            case STUCK -> recoverStuck(foreman, path);
            case UNREACHABLE -> recoverUnreachable(foreman, path.getTarget());
            case CHUNK_UNLOADED -> recoverChunkUnload(foreman);
            case INSUFFICIENT_PROGRESS -> recoverSlowProgress(foreman, path);
        };
    }

    private ActionResult recoverStuck(ForemanEntity foreman, Path path) {
        // Strategy 1: Reverse and retry
        if (foreman.getRandom().nextBoolean()) {
            foreman.moveBackward(1.0);
            return ActionResult.pending("Reversing to unstuck");
        }

        // Strategy 2: Jump to clear blockages
        foreman.jumpFromGround();
        return ActionResult.pending("Jumping to clear obstruction");

        // Strategy 3: Break obstructing blocks
        // BlockPos ahead = foreman.blockPosition().relative(foreman.getDirection());
        // foreman.level().destroyBlock(ahead, true);
        // return ActionResult.pending("Breaking obstruction");
    }

    private ActionResult recoverChunkUnload(ForemanEntity foreman) {
        // Teleport to player (safe location)
        Player player = foreman.level().getNearestPlayer(foreman, 50);
        if (player != null) {
            foreman.teleportTo(player.blockPosition());
            return ActionResult.success("Teleported to player after chunk unload");
        }
        return ActionResult.failure("Chunk unloaded and no player nearby", true);
    }
}
```

---

### 1.2 Resource Depletion

#### Description
Actions fail when required materials are exhausted during task execution.

#### Error Types

| Resource Type | Detection | Recovery |
|---------------|-----------|----------|
| **Building Materials** | Inventory count < required | Pause task, request materials |
| **Tool Durability** | Tool damage >= max durability | Switch tool, request replacement |
| **Food/Hunger** | Saturation <= 0 | Eat food, pause for farming |
| **Fuel (smelting)** | Fuel slot empty | Insert fuel, wait for smelting |
| **Inventory Space** | Inventory full | Deposit to chest, discard low-value |

#### Detection Implementation

```java
public class ResourceMonitor {
    public ResourceStatus checkResources(ForemanEntity foreman, Task task) {
        // Check inventory for required items
        Map<String, Integer> required = extractRequiredItems(task);
        Map<String, Integer> available = countInventory(foreman);

        for (Map.Entry<String, Integer> req : required.entrySet()) {
            int have = available.getOrDefault(req.getKey(), 0);
            if (have < req.getValue()) {
                return ResourceStatus.shortage(req.getKey(), req.getValue() - have);
            }
        }

        // Check tool durability
        ItemStack tool = foreman.getMainHandItem();
        if (tool.isDamageable() && tool.getDamageValue() >= tool.getMaxDamage() - 10) {
            return ResourceStatus.toolWorn(tool.getItem());
        }

        // Check inventory space
        if (foreman.getInventory().getFreeSlot() == -1) {
            return ResourceStatus.inventoryFull();
        }

        return ResourceStatus.sufficient();
    }

    public ActionResult handleShortage(ResourceStatus status, ForemanEntity foreman) {
        switch (status.type()) {
            case ITEM_SHORTAGE:
                return requestItem(status.item(), status.quantity(), foreman);

            case TOOL_WORN:
                return replaceTool(status.tool(), foreman);

            case INVENTORY_FULL:
                return depositInventory(foreman);

            default:
                return ActionResult.failure("Unknown resource shortage", true);
        }
    }
}
```

---

### 1.3 Environment Hazards

#### Description
Dynamic environmental factors that disrupt automation.

#### Hazard Types

| Hazard | Detection | Impact | Recovery |
|--------|-----------|--------|----------|
| **Hostile Mobs** | Nearby hostile entities | Combat interrupts tasks | Fight or flee |
| **Lava/Fire** | Block type check | Death, item loss | Path around, extinguish |
| **Suffocation** | Inside solid block | Damage, death | Break free, teleport |
| **Gravity** | Fall damage | Death, item loss | Place water, use ladder |
| **Explosions** | Creeper, TNT | Structure destruction | Flee, rebuild |
| **Weather** | Rain, thunder | Fire extinguished, visibility loss | Wait, use shelter |

#### Hazard Detection

```java
public class HazardDetector {
    private static final double HAZARD_RADIUS = 8.0;

    public List<Hazard> detectHazards(ForemanEntity foreman) {
        List<Hazard> hazards = new ArrayList<>();
        AABB searchArea = foreman.getBoundingBox().inflate(HAZARD_RADIUS);

        // Check for hostile mobs
        foreman.level().getEntitiesOfClass(Mob.class, searchArea)
            .stream()
            .filter(e -> e instanceof Enemy || e.isAggressive())
            .forEach(mob -> hazards.add(new Hazard(HazardType.HOSTILE_MOB, mob.blockPosition(), mob)));

        // Check for environmental dangers
        for (BlockPos pos : BlockPos.betweenClosed(
            (int)searchArea.minX, (int)searchArea.minY, (int)searchArea.minZ,
            (int)searchArea.maxX, (int)searchArea.maxY, (int)searchArea.maxZ)) {

            BlockState state = foreman.level().getBlockState(pos);
            Block block = state.getBlock();

            if (block == Blocks.LAVA || block == Blocks.FIRE) {
                hazards.add(new Hazard(HazardType.FIRE_SOURCE, pos, state));
            }

            if (state.isSuffocating(foreman.level(), pos, foreman)) {
                hazards.add(new Hazard(HazardType.SUFFOCATION, pos, state));
            }
        }

        return hazards;
    }
}
```

#### Hazard Response

```java
public class HazardResponseSystem {
    public ActionResult respondToHazards(List<Hazard> hazards, ForemanEntity foreman) {
        // Prioritize hazards by severity
        hazards.sort(Comparator.comparing(this::calculateSeverity).reversed());

        Hazard mostSevere = hazards.get(0);

        return switch (mostSevere.type()) {
            case HOSTILE_MOB -> handleHostileMob(mostSevere, foreman);
            case FIRE_SOURCE -> handleFire(mostSevere, foreman);
            case SUFFOCATION -> handleSuffocation(mostSevere, foreman);
            default -> ActionResult.pending("Unknown hazard");
        };
    }

    private ActionResult handleHostileMob(Hazard hazard, ForemanEntity foreman) {
        Entity mob = hazard.entity();

        // Calculate threat level
        double threatLevel = calculateThreat(mob, foreman);

        if (threatLevel > 0.7) {
            // Flee to safety
            return fleeToSafety(foreman, mob.blockPosition());
        } else {
            // Fight back
            return engageCombat(foreman, mob);
        }
    }

    private ActionResult fleeToSafety(ForemanEntity foreman, BlockPos from) {
        Player player = foreman.level().getNearestPlayer(foreman, 50);
        if (player != null) {
            // Pathfind to player for safety
            Task fleeTask = new Task("pathfind", Map.of(
                "target", "player",
                "distance", 5
            ));
            return ActionResult.pending("Fleeing to player", fleeTask);
        }

        // Find safe location
        BlockPos safePos = findSafeLocation(foreman, from);
        if (safePos != null) {
            Task fleeTask = new Task("pathfind", Map.of(
                "x", safePos.getX(),
                "y", safePos.getY(),
                "z", safePos.getZ()
            ));
            return ActionResult.pending("Fleeing to safety", fleeTask);
        }

        return ActionResult.failure("No safe location found", false);
    }
}
```

---

### 1.4 Action Failures

#### Description
Specific action types failing during execution.

#### Failure Modes by Action Type

| Action | Common Failures | Detection | Recovery |
|--------|----------------|-----------|----------|
| **Place Block** | Obstruction, out of range, spawn protection | Block not placed after attempt | Clear area, move closer, retry |
| **Mine Block** | Wrong tool, obsidian, tile entity | Block not broken after swings | Switch tool, skip unbreakable |
| **Craft Item** | Missing materials, wrong recipe | Crafting slot empty | Gather materials, try alternative |
| **Attack** | Out of range, weapon cooldown | No damage dealt | Move closer, wait for cooldown |
| **Follow** | Target lost, too far | Distance > threshold | Pathfind to target, wait |
| **Build** | Invalid placement, structure conflict | Blueprint mismatch | Adjust blueprint, skip block |

#### Example: Place Block Failure Recovery

```java
public class PlaceBlockRecovery {
    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY_TICKS = 40; // 2 seconds

    private int retryCount = 0;
    private int ticksSinceLastAttempt = 0;

    public ActionResult recover(ForemanEntity foreman, BlockPos pos, BlockState block) {
        // Detect failure reason
        PlaceBlockFailureReason reason = detectFailureReason(foreman, pos, block);

        return switch (reason) {
            case OUT_OF_RANGE -> recoverOutOfRange(foreman, pos);
            case OBSTRUCTED -> recoverObstructed(foreman, pos);
            case SPAWN_PROTECTION -> recoverSpawnProtection(foreman, pos);
            case INVALID_PLACEMENT -> recoverInvalidPlacement(foreman, pos);
            case NO_MATERIALS -> recoverNoMaterials(foreman, block.getBlock());
        };
    }

    private PlaceBlockFailureReason detectFailureReason(ForemanEntity foreman, BlockPos pos, BlockState block) {
        // Check distance
        double distance = foreman.position().distanceTo(Vec3.atCenterOf(pos));
        if (distance > foreman.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)) {
            return PlaceBlockFailureReason.OUT_OF_RANGE;
        }

        // Check for obstruction
        if (!foreman.level().getBlockState(pos).isAir()) {
            return PlaceBlockFailureReason.OBSTRUCTED;
        }

        // Check spawn protection
        if (foreman.level().hasChunkAt(pos)) {
            ChunkAccess chunk = foreman.level().getChunkAt(pos);
            if (chunk != null && chunk.getInhabitedTime() < 100) {
                return PlaceBlockFailureReason.SPAWN_PROTECTION;
            }
        }

        // Check if placement is valid
        if (!canPlace(foreman.level(), pos, block)) {
            return PlaceBlockFailureReason.INVALID_PLACEMENT;
        }

        // Check inventory
        if (!hasMaterial(foreman, block.getBlock())) {
            return PlaceBlockFailureReason.NO_MATERIALS;
        }

        return PlaceBlockFailureReason.UNKNOWN;
    }

    private ActionResult recoverObstructed(ForemanEntity foreman, BlockPos pos) {
        // Strategy: Clear obstruction
        foreman.level().destroyBlock(pos, true);

        if (retryCount < MAX_RETRIES) {
            retryCount++;
            return ActionResult.pending("Cleared obstruction, retrying");
        }

        return ActionResult.failure("Cannot clear obstruction at " + pos, false);
    }

    private ActionResult recoverOutOfRange(ForemanEntity foreman, BlockPos pos) {
        // Strategy: Move closer
        Vec3 lookPos = Vec3.atCenterOf(pos);
        foreman.getLookControl().setLookAt(lookPos);
        foreman.getNavigation().moveTo(lookPos.x, lookPos.y, lookPos.z, 1.0);

        if (retryCount < MAX_RETRIES) {
            retryCount++;
            return ActionResult.pending("Moving closer to place block");
        }

        return ActionResult.failure("Cannot reach position " + pos, true);
    }
}
```

---

### 1.5 LLM Planning Errors

#### Description
Failures in the LLM planning and task generation phase.

#### Error Types

| Error Type | Cause | Detection | Recovery |
|------------|-------|-----------|----------|
| **API Timeout** | Slow LLM response | Request > 30 seconds | Retry with timeout, use fallback |
| **Rate Limiting** | Too many requests | HTTP 429 | Wait with exponential backoff |
| **Bad Plan** | Hallucination, invalid tasks | Plan validation fails | Request replan, use template |
| **Malformed Response** | Invalid JSON | Parse exception | Use fallback response |
| **API Unavailable** | Service down | Connection refused | Use fallback provider or offline mode |
| **Context Overflow** | Prompt too long | Token limit exceeded | Summarize, truncate context |

#### Current Implementation Analysis

```java
// Existing error handling in TaskPlanner.java
public CompletableFuture<ParsedResponse> planTasksAsync(ForemanEntity foreman, String command) {
    return client.sendAsync(userPrompt, params)
        .thenApply(response -> {
            // Parse and validate response
            ResponseParser.ParsedResponse parsed = ResponseParser.parseAIResponse(content);
            return parsed;
        })
        .exceptionally(throwable -> {
            LOGGER.error("[Async] Error planning tasks: {}", throwable.getMessage());
            return null;  // ISSUE: Returns null instead of proper error handling
        });
}
```

#### Improved Error Handling

```java
public class ResilientTaskPlanner {
    private final AsyncLLMClient primaryClient;
    private final AsyncLLMClient fallbackClient;
    private final FallbackResponseSystem fallbackSystem;
    private final CircuitBreaker circuitBreaker;

    public CompletableFuture<ParsedResponse> planTasksAsync(ForemanEntity foreman, String command) {
        // Check circuit breaker first
        if (!circuitBreaker.allowRequest()) {
            LOGGER.warn("Circuit breaker open, using fallback");
            return CompletableFuture.completedFuture(fallbackSystem.generateFallback(command));
        }

        // Try primary client
        return primaryClient.sendAsync(buildPrompt(foreman, command), buildParams())
            .thenApply(response -> parseAndValidate(response))
            .exceptionallyCompose(primaryError -> {
                LOGGER.warn("Primary client failed: {}", primaryError.getMessage());
                circuitBreaker.recordFailure();

                // Try fallback client
                return fallbackClient.sendAsync(buildPrompt(foreman, command), buildParams())
                    .thenApply(response -> parseAndValidate(response))
                    .exceptionallyCompose(secondaryError -> {
                        LOGGER.warn("Fallback client failed: {}", secondaryError.getMessage());

                        // Use pattern-based fallback
                        return CompletableFuture.completedFuture(
                            fallbackSystem.generateFallback(command, secondaryError)
                        );
                    });
            })
            .thenApply(parsed -> {
                circuitBreaker.recordSuccess();
                return parsed;
            });
    }
}
```

---

## 2. Error Detection and Classification

### 2.1 Error Taxonomy

```
Errors
├── Transient (retryable)
│   ├── Network issues
│   ├── Rate limiting
│   ├── Temporary obstructions
│   └── Context overflow
├── Permanent (non-retryable)
│   ├── Invalid parameters
│   ├── Authentication failures
│   ├── Unreachable targets
│   └── Missing required resources
├── Recoverable (with intervention)
│   ├── Resource depletion
│   ├── Tool breakage
│   ├── Inventory full
│   └── Entity stuck
└── Fatal (requires reset)
    ├── Chunk unload
    ├── Entity death
    ├── State corruption
    └── Deadlock
```

### 2.2 Error Classification System

```java
public enum ErrorSeverity {
    // Transient issues that resolve themselves
    INFO(1, false),

    // Recoverable with automatic retries
    RECOVERABLE(2, false),

    // Requires user intervention or replanning
    WARNING(3, true),

    // Requires task restart or alternative approach
    ERROR(4, true),

    // System failure requiring reset
    FATAL(5, true);

    private final int level;
    private final boolean requiresReplanning;

    ErrorSeverity(int level, boolean requiresReplanning) {
        this.level = level;
        this.requiresReplanning = requiresReplanning;
    }
}

public enum ErrorCategory {
    NAVIGATION("pathfinding", "movement"),
    ACTION("execution", "interaction"),
    RESOURCE("materials", "tools", "inventory"),
    ENVIRONMENT("hazards", "entities", "blocks"),
    PLANNING("llm", "task generation"),
    COORDINATION("multi-agent", "synchronization"),
    SYSTEM("crashes", "corruption", "deadlock");

    private final String[] keywords;

    ErrorCategory(String... keywords) {
        this.keywords = keywords;
    }
}

public class ClassifiedError {
    private final ErrorSeverity severity;
    private final ErrorCategory category;
    private final String code;
    private final String message;
    private final Map<String, Object> context;
    private final List<RecoveryStrategy> suggestedRecoveries;
    private final boolean retryable;
    private final long timestamp;

    public ClassifiedError(ErrorSeverity severity, ErrorCategory category, String code, String message) {
        this.severity = severity;
        this.category = category;
        this.code = code;
        this.message = message;
        this.context = new HashMap<>();
        this.suggestedRecoveries = new ArrayList<>();
        this.retryable = determineRetryability();
        this.timestamp = System.currentTimeMillis();
    }

    private boolean determineRetryability() {
        return switch (severity) {
            case INFO, RECOVERABLE -> true;
            case WARNING -> category == ErrorCategory.RESOURCE;
            case ERROR, FATAL -> false;
        };
    }

    public static ClassifiedError fromThrowable(Throwable throwable, Context executionContext) {
        ErrorCategory category = categorizeException(throwable);
        ErrorSeverity severity = estimateSeverity(throwable, executionContext);

        return new ClassifiedError(
            severity,
            category,
            extractErrorCode(throwable),
            throwable.getMessage()
        );
    }
}
```

---

## 3. Recovery Strategies

### 3.1 Retry with Different Parameters

```java
public class ParameterAdjustmentRecovery {
    public ActionResult recoverWithAdjustment(BaseAction action, ActionResult failure) {
        Map<String, Object> adjustedParams = new HashMap<>(action.getTask().getParameters());

        // Adjust parameters based on failure type
        if (failure.getMessage().contains("out of range")) {
            double distance = (Double) adjustedParams.getOrDefault("distance", 5.0);
            adjustedParams.put("distance", distance * 0.8); // Try closer

        } else if (failure.getMessage().contains("too far")) {
            int timeout = (Integer) adjustedParams.getOrDefault("timeout", 600);
            adjustedParams.put("timeout", timeout * 2); // Increase timeout

        } else if (failure.getMessage().contains("obstructed")) {
            adjustedParams.put("clearObstruction", true); // Add obstruction clearing
        }

        // Create new task with adjusted parameters
        Task adjustedTask = new Task(action.getTask().getAction(), adjustedParams);
        BaseAction retryAction = ActionRegistry.getInstance().createAction(
            action.getTask().getAction(),
            action.getForeman(),
            adjustedTask,
            action.getContext()
        );

        return ActionResult.pending("Retrying with adjusted parameters", retryAction);
    }
}
```

### 3.2 Escalation to Higher-Level Planner

```java
public class EscalationRecovery {
    public ActionResult escalate(BaseAction action, ActionResult failure) {
        ForemanEntity foreman = action.getForeman();

        // Store failure context for escalation
        Map<String, Object> escalationContext = new HashMap<>();
        escalationContext.put("failedAction", action.getClass().getSimpleName());
        escalationContext.put("failure", failure.getMessage());
        escalationContext.put("originalTask", action.getTask());
        escalationContext.put("timestamp", System.currentTimeMillis());

        // Log failure for learning
        foreman.getMemory().recordFailure(action.getTask(), failure);

        // Determine escalation level
        EscalationLevel level = determineEscalationLevel(failure);

        return switch (level) {
            case LOCAL_RECOVERY -> handleLocally(action, failure, escalationContext);

            case TASK_REPLANNING -> requestReplanning(foreman, action.getTask(), escalationContext);

            case PLAYER_ASSISTANCE -> requestPlayerHelp(foreman, action, failure);

            case SYSTEM_RESET -> performSystemReset(foreman, escalationContext);
        };
    }

    private ActionResult requestReplanning(ForemanEntity foreman, Task originalTask, Map<String, Object> context) {
        String contextString = buildContextString(originalTask, context);

        String replanPrompt = String.format(
            "The following task failed: %s\n" +
            "Failure reason: %s\n" +
            "Please provide an alternative approach to accomplish the same goal.\n" +
            "Context: %s",
            originalTask,
            context.get("failure"),
            contextString
        );

        // Trigger async replanning
        foreman.getTaskPlanner().planTasksAsync(foreman, replanPrompt)
            .thenAccept(newPlan -> {
                if (newPlan != null && !newPlan.getTasks().isEmpty()) {
                    foreman.getActionExecutor().queueTasks(newPlan.getTasks());
                } else {
                    foreman.sendChatMessage("I couldn't come up with an alternative approach.");
                }
            });

        return ActionResult.pending("Requesting alternative plan from LLM");
    }

    private ActionResult requestPlayerHelp(ForemanEntity foreman, BaseAction action, ActionResult failure) {
        String actionName = action.getClass().getSimpleName();
        String taskDesc = action.getTask().toString();
        String failureMsg = failure.getMessage();

        foreman.sendChatMessage(String.format(
            "I need help with: %s (Task: %s)",
            actionName,
            taskDesc
        ));
        foreman.sendChatMessage(String.format(
            "Problem: %s",
            failureMsg
        ));

        // Suggest recovery options
        List<String> options = generateRecoveryOptions(action, failure);
        foreman.sendChatMessage("What should I do?");
        for (int i = 0; i < options.size(); i++) {
            foreman.sendChatMessage(String.format("  [%d] %s", i + 1, options.get(i)));
        }

        // Wait for player input
        return ActionResult.pending("Waiting for player assistance");
    }
}
```

### 3.3 Switch to Alternative Approach

```java
public class AlternativeApproachRecovery {
    private static final Map<String, List<String>> ALTERNATIVES = Map.of(
        "pathfind", List.of("teleport", "wait", "ask_player"),
        "mine", List.of("use_alt_tool", "skip_block", "request_materials"),
        "place", List.of("place_alt_material", "skip_placement", "clear_area"),
        "build", List.of("simplify_structure", "build_section", "use_template")
    );

    public ActionResult tryAlternative(BaseAction action, ActionResult failure) {
        String actionType = action.getTask().getAction();
        List<String> alternatives = ALTERNATIVES.getOrDefault(actionType, List.of());

        for (String alternative : alternatives) {
            ActionResult result = tryAlternativeAction(action, alternative);
            if (result.isSuccess()) {
                return result;
            }
        }

        // All alternatives failed
        return ActionResult.failure(
            "All alternative approaches failed for " + actionType,
            true
        );
    }

    private ActionResult tryAlternativeAction(BaseAction originalAction, String alternative) {
        Task alternativeTask = createAlternativeTask(originalAction.getTask(), alternative);

        BaseAction alternativeAction = ActionRegistry.getInstance().createAction(
            alternative,
            originalAction.getForeman(),
            alternativeTask,
            originalAction.getContext()
        );

        if (alternativeAction != null) {
            return ActionResult.pending(
                "Trying alternative: " + alternative,
                alternativeAction
            );
        }

        return ActionResult.failure("Could not create alternative: " + alternative, false);
    }
}
```

### 3.4 Graceful Degradation

```java
public class GracefulDegradation {
    public ActionResult degrade(BaseAction action, ActionResult failure) {
        ForemanEntity foreman = action.getForeman();

        // Log the degradation
        LOGGER.warn("[{}] Degrading gracefully from failed action: {}",
            foreman.getName(), action.getClass().getSimpleName());

        // Determine degradation level
        DegradationLevel level = assessDegradationLevel(foreman, action, failure);

        return switch (level) {
            case CONTINUE_WITH_WARNING -> {
                // Skip non-critical action and continue
                yield ActionResult.success(
                    "Skipped non-critical action: " + failure.getMessage()
                );
            }

            case REDUCED_FUNCTIONALITY -> {
                // Switch to simpler, less capable approach
                Task simplifiedTask = createSimplifiedTask(action.getTask());
                yield ActionResult.pending(
                    "Using simplified approach: " + failure.getMessage(),
                    simplifiedTask
                );
            }

            case PASSIVE_MODE -> {
                // Enter passive/idle mode
                yield ActionResult.success(
                    "Entering passive mode due to repeated failures"
                );
            }

            case MINIMAL_OPERATION -> {
                // Only perform essential actions
                yield ActionResult.success(
                    "Restricting to essential operations"
                );
            }
        };
    }

    private DegradationLevel assessDegradationLevel(ForemanEntity foreman, BaseAction action, ActionResult failure) {
        // Count recent failures
        int recentFailures = foreman.getMemory().getRecentFailureCount(Duration.ofMinutes(5));

        if (recentFailures < 2) {
            return DegradationLevel.CONTINUE_WITH_WARNING;
        } else if (recentFailures < 5) {
            return DegradationLevel.REDUCED_FUNCTIONALITY;
        } else if (recentFailures < 10) {
            return DegradationLevel.PASSIVE_MODE;
        } else {
            return DegradationLevel.MINIMAL_OPERATION;
        }
    }
}
```

---

## 4. Circuit Breaker Patterns

### 4.1 Standard Circuit Breaker

```java
public class CircuitBreaker {
    private enum State { CLOSED, OPEN, HALF_OPEN }

    private volatile State state = State.CLOSED;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final int failureThreshold;
    private final long timeoutMs;
    private volatile long lastFailureTime;
    private final int halfOpenMaxAttempts;

    public CircuitBreaker(int failureThreshold, long timeoutMs, int halfOpenMaxAttempts) {
        this.failureThreshold = failureThreshold;
        this.timeoutMs = timeoutMs;
        this.halfOpenMaxAttempts = halfOpenMaxAttempts;
    }

    public boolean allowRequest() {
        synchronized (this) {
            if (state == State.OPEN) {
                // Check if timeout has elapsed
                if (System.currentTimeMillis() - lastFailureTime > timeoutMs) {
                    transitionTo(State.HALF_OPEN);
                    LOGGER.info("Circuit breaker entering HALF_OPEN state");
                    return true;
                }
                return false;
            }
            return true;
        }
    }

    public void recordSuccess() {
        synchronized (this) {
            if (state == State.HALF_OPEN) {
                int attempts = successCount.incrementAndGet();
                if (attempts >= halfOpenMaxAttempts) {
                    transitionTo(State.CLOSED);
                    LOGGER.info("Circuit breaker closing after successful recovery");
                }
            } else {
                failureCount.set(0);
            }
        }
    }

    public void recordFailure() {
        synchronized (this) {
            int failures = failureCount.incrementAndGet();
            lastFailureTime = System.currentTimeMillis();

            if (state == State.HALF_OPEN || failures >= failureThreshold) {
                transitionTo(State.OPEN);
                LOGGER.warn("Circuit breaker opening after {} failures", failures);
            }
        }
    }

    private void transitionTo(State newState) {
        LOGGER.info("Circuit breaker: {} -> {}", state, newState);
        state = newState;

        if (newState == State.CLOSED) {
            failureCount.set(0);
            successCount.set(0);
        }
    }

    public State getState() {
        return state;
    }

    public double getFailureRate() {
        int total = failureCount.get() + successCount.get();
        return total == 0 ? 0.0 : (double) failureCount.get() / total;
    }
}
```

### 4.2 Adaptive Circuit Breaker

```java
public class AdaptiveCircuitBreaker extends CircuitBreaker {
    private volatile int dynamicThreshold;
    private final int minThreshold;
    private final int maxThreshold;
    private final double adaptationFactor;

    public AdaptiveCircuitBreaker(int initialThreshold, int minThreshold, int maxThreshold) {
        super(initialThreshold, 30000, 3);
        this.dynamicThreshold = initialThreshold;
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;
        this.adaptationFactor = 0.1;
    }

    @Override
    public void recordSuccess() {
        super.recordSuccess();

        // Gradually increase threshold if system is stable
        if (getState() == State.CLOSED && getFailureRate() < 0.1) {
            int newThreshold = (int) Math.min(
                maxThreshold,
                dynamicThreshold * (1 + adaptationFactor)
            );
            if (newThreshold != dynamicThreshold) {
                LOGGER.debug("Increasing failure threshold to {}", newThreshold);
                dynamicThreshold = newThreshold;
            }
        }
    }

    @Override
    public void recordFailure() {
        super.recordFailure();

        // Decrease threshold if failures are frequent
        if (getFailureRate() > 0.3) {
            int newThreshold = (int) Math.max(
                minThreshold,
                dynamicThreshold * (1 - adaptationFactor)
            );
            if (newThreshold != dynamicThreshold) {
                LOGGER.debug("Decreasing failure threshold to {}", newThreshold);
                dynamicThreshold = newThreshold;
            }
        }
    }

    @Override
    public boolean allowRequest() {
        // Use dynamic threshold
        synchronized (this) {
            if (getState() == State.CLOSED) {
                int failures = failureCount.get();
                if (failures >= dynamicThreshold) {
                    transitionTo(State.OPEN);
                    return false;
                }
            }
        }

        return super.allowRequest();
    }
}
```

### 4.3 Multi-Level Circuit Breaker

```java
public class MultiLevelCircuitBreaker {
    private final CircuitBreaker fastBreaker;  // 5 failures, 30s timeout
    private final CircuitBreaker mediumBreaker; // 20 failures, 5m timeout
    private final CircuitBreaker slowBreaker;  // 100 failures, 30m timeout

    public MultiLevelCircuitBreaker() {
        this.fastBreaker = new CircuitBreaker(5, 30000, 2);
        this.mediumBreaker = new CircuitBreaker(20, 300000, 5);
        this.slowBreaker = new CircuitBreaker(100, 1800000, 10);
    }

    public boolean allowRequest() {
        // Check all breakers
        if (fastBreaker.getState() == CircuitBreaker.State.OPEN) {
            return false;
        }
        if (mediumBreaker.getState() == CircuitBreaker.State.OPEN) {
            return false;
        }
        if (slowBreaker.getState() == CircuitBreaker.State.OPEN) {
            return false;
        }
        return true;
    }

    public void recordSuccess() {
        fastBreaker.recordSuccess();
        mediumBreaker.recordSuccess();
        slowBreaker.recordSuccess();
    }

    public void recordFailure() {
        fastBreaker.recordFailure();
        mediumBreaker.recordFailure();
        slowBreaker.recordFailure();
    }

    public BreakerLevel getMostRestrictiveLevel() {
        if (fastBreaker.getState() == CircuitBreaker.State.OPEN) {
            return BreakerLevel.FAST;
        }
        if (mediumBreaker.getState() == CircuitBreaker.State.OPEN) {
            return BreakerLevel.MEDIUM;
        }
        if (slowBreaker.getState() == CircuitBreaker.State.OPEN) {
            return BreakerLevel.SLOW;
        }
        return BreakerLevel.NONE;
    }

    public enum BreakerLevel {
        NONE,   // All breakers closed
        SLOW,   // Long-term pattern breaker open
        MEDIUM, // Medium-term breaker open
        FAST    // Short-term breaker open
    }
}
```

---

## 5. Self-Healing Scripts

### 5.1 Failure Detection Scripts

```javascript
// Self-healing script for detecting stuck conditions
// Runs in CodeExecutionEngine with ForemanAPI

function detectStuck() {
    const agent = foreman.getCurrentAgent();
    const lastPosition = agent.getLastPosition();
    const currentPosition = agent.getPosition();

    // Check if position hasn't changed significantly
    const distance = lastPosition.distanceTo(currentPosition);

    if (distance < 0.1) {
        const stuckTime = agent.getStuckTime() || 0;
        agent.setStuckTime(stuckTime + 1);

        // Trigger recovery if stuck for 30 seconds (600 ticks)
        if (agent.getStuckTime() > 600) {
            triggerStuckRecovery(agent);
        }
    } else {
        agent.setStuckTime(0);
    }

    agent.setLastPosition(currentPosition);
}

function triggerStuckRecovery(agent) {
    const strategies = [
        tryReverseAndRetry,
        tryJumping,
        tryBreakingObstruction,
        tryTeleportUnstuck,
        requestPlayerHelp
    ];

    for (const strategy of strategies) {
        const result = strategy(agent);
        if (result.success) {
            foreman.log("Recovered from stuck: " + result.method);
            agent.setStuckTime(0);
            return;
        }
    }

    foreman.log("All recovery strategies failed");
}

function tryReverseAndRetry(agent) {
    agent.moveBackward(2.0);
    agent.wait(20); // Wait 1 second
    return { success: agent.resumeTask(), method: "reverse" };
}

function tryJumping(agent) {
    for (let i = 0; i < 5; i++) {
        agent.jump();
        agent.wait(5);
    }
    return { success: agent.resumeTask(), method: "jump" };
}

function tryBreakingObstruction(agent) {
    const ahead = agent.getPosition().add(agent.getDirection());
    const block = foreman.getBlock(ahead);

    if (block && !block.isAir()) {
        foreman.destroyBlock(ahead);
        agent.wait(10);
        return { success: agent.resumeTask(), method: "break" };
    }

    return { success: false, method: "break" };
}

function tryTeleportUnstuck(agent) {
    const above = agent.getPosition().add(0, 2, 0);
    const blockAbove = foreman.getBlock(above);

    if (blockAbove && blockAbove.isAir()) {
        agent.teleport(above);
        return { success: true, method: "teleport" };
    }

    return { success: false, method: "teleport" };
}

function requestPlayerHelp(agent) {
    const player = foreman.getNearestPlayer(50);
    if (player) {
        foreman.sendChatMessage("I'm stuck and need help!");
        return { success: true, method: "player_help" };
    }
    return { success: false, method: "player_help" };
}
```

### 5.2 Adaptive Behavior Scripts

```javascript
// Script that learns from failures and adjusts behavior

const FailureHistory = {
    pathfinding: {},
    mining: {},
    building: {},
    combat: {}
};

function recordFailure(actionType, failureReason, context) {
    const key = actionType + ":" + failureReason;

    if (!FailureHistory[actionType][key]) {
        FailureHistory[actionType][key] = {
            count: 0,
            lastOccurrence: 0,
            contexts: []
        };
    }

    const entry = FailureHistory[actionType][key];
    entry.count++;
    entry.lastOccurrence = Date.now();
    entry.contexts.push(context);

    // Keep only last 10 contexts
    if (entry.contexts.length > 10) {
        entry.contexts.shift();
    }

    // Trigger adaptation if frequent failure
    if (entry.count >= 3) {
        adaptBehavior(actionType, failureReason, entry);
    }
}

function adaptBehavior(actionType, failureReason, history) {
    foreman.log("Adapting behavior for " + actionType + " due to: " + failureReason);

    switch (actionType) {
        case "pathfind":
            adaptPathfinding(failureReason, history);
            break;
        case "mine":
            adaptMining(failureReason, history);
            break;
        case "build":
            adaptBuilding(failureReason, history);
            break;
        case "combat":
            adaptCombat(failureReason, history);
            break;
    }
}

function adaptPathfinding(failureReason, history) {
    if (failureReason === "stuck") {
        // Increase timeout and use simpler paths
        foreman.setPathfindingTimeout(1200); // 60 seconds
        foreman.setPathfindingSimplification(0.7); // 70% simplification

    } else if (failureReason === "unreachable") {
        // Mark location as unreachable
        history.contexts.forEach(ctx => {
            if (ctx.target) {
                foreman.markUnreachable(ctx.target);
            }
        });

    } else if (failureReason === "chunk_unload") {
        // Stay closer to player
        foreman.setMaxDistanceFromPlayer(30);
    }
}

function adaptMining(failureReason, history) {
    if (failureReason === "wrong_tool") {
        // Add tool checking to preconditions
        foreman.addPrecondition("mine", ctx => {
            const requiredTool = ctx.requiredTool || "pickaxe";
            return foreman.hasTool(requiredTool);
        });

    } else if (failureReason === "no_materials") {
        // Request materials before starting
        foreman.addPrecondition("mine", ctx => {
            if (ctx.quantity && ctx.quantity > 64) {
                foreman.requestResource(ctx.target, ctx.quantity);
                return false; // Wait for materials
            }
            return true;
        });
    }
}

function adaptBuilding(failureReason, history) {
    if (failureReason === "obstruction") {
        // Enable auto-clear mode
        foreman.setAutoClearObstructions(true);

    } else if (failureReason === "no_materials") {
        // Calculate required materials upfront
        foreman.addPrecondition("build", ctx => {
            const materials = foreman.calculateMaterials(ctx.structure);
            return foreman.hasMaterials(materials);
        });
    }
}
```

### 5.3 Scripts That Request Refinement

```javascript
// Script that detects its own limitations and requests help

function executeWithFallback(task) {
    const action = task.action;
    const parameters = task.parameters;

    try {
        const result = executeAction(action, parameters);

        if (!result.success) {
            return handleFailure(action, parameters, result);
        }

        return result;

    } catch (error) {
        return handleError(action, parameters, error);
    }
}

function handleFailure(action, parameters, result) {
    foreman.log("Action failed: " + action + " - " + result.message);

    // Determine if we should request refinement
    if (shouldRequestRefinement(action, parameters, result)) {
        return requestRefinement(action, parameters, result);
    }

    // Try automatic recovery
    return attemptRecovery(action, parameters, result);
}

function shouldRequestRefinement(action, parameters, result) {
    // Request refinement if:
    // 1. Multiple retries failed
    const retryCount = parameters._retryCount || 0;
    if (retryCount >= 3) return true;

    // 2. Action is complex and unclear
    if (isComplexAction(action) && isUnclearParameters(parameters)) {
        return true;
    }

    // 3. Error suggests fundamental issue
    if (result.message.includes("invalid") ||
        result.message.includes("unknown") ||
        result.message.includes("unsupported")) {
        return true;
    }

    return false;
}

function requestRefinement(action, parameters, result) {
    const agent = foreman.getCurrentAgent();
    const player = foreman.getNearestPlayer(50);

    // Build refinement request
    const request = {
        originalAction: action,
        parameters: parameters,
        failure: result.message,
        context: agent.getContext(),
        capabilities: agent.getCapabilities()
    };

    // Send to LLM for refinement
    const prompt = buildRefinementPrompt(request);

    foreman.log("Requesting task refinement from LLM...");

    return foreman.planTasks(prompt)
        .then(refinedPlan => {
            if (refinedPlan && refinedPlan.tasks.length > 0) {
                foreman.log("Received refined plan with " + refinedPlan.tasks.length + " tasks");
                return executeWithFallback(refinedPlan.tasks[0]);
            } else {
                // LLM couldn't help, ask player
                return requestPlayerRefinement(request);
            }
        })
        .catch(error => {
            foreman.log("Refinement failed: " + error.message);
            return requestPlayerRefinement(request);
        });
}

function buildRefinementPrompt(request) {
    return `The following action failed:
Action: ${request.originalAction}
Parameters: ${JSON.stringify(request.parameters)}
Failure: ${request.failure}

Context: ${JSON.stringify(request.context)}
Available capabilities: ${request.capabilities.join(", ")}

Please provide a refined approach to accomplish the same goal.
Return the response as a JSON array of tasks.`;
}

function requestPlayerRefinement(request) {
    const player = foreman.getNearestPlayer(50);

    if (player) {
        foreman.sendChatMessage("I need help refining this task:");
        foreman.sendChatMessage(`  Action: ${request.originalAction}`);
        foreman.sendChatMessage(`  Problem: ${request.failure}`);

        // Suggest alternatives
        const alternatives = generateAlternatives(request);
        foreman.sendChatMessage("Alternatives:");
        alternatives.forEach((alt, i) => {
            foreman.sendChatMessage(`  [${i+1}] ${alt.description}`);
        });

        return {
            success: false,
            waitingForPlayer: true,
            alternatives: alternatives
        };
    }

    return {
        success: false,
        message: "No player available for refinement"
    };
}

function generateAlternatives(request) {
    const alternatives = [];

    switch (request.originalAction) {
        case "pathfind":
            alternatives.push({ description: "Teleport to target" });
            alternatives.push({ description: "Wait for target to become reachable" });
            alternatives.push({ description: "Ask player to come to me" });
            break;

        case "build":
            alternatives.push({ description: "Build simpler structure" });
            alternatives.push({ description: "Build in sections" });
            alternatives.push({ description: "Use different materials" });
            break;

        case "mine":
            alternatives.push({ description: "Mine different resource" });
            alternatives.push({ description: "Use better tool" });
            alternatives.push({ description: "Mine at different location" });
            break;
    }

    return alternatives;
}
```

---

## 6. Multi-Agent Error Coordination

### 6.1 Deadlock Detection and Resolution

```java
public class MultiAgentDeadlockDetector {
    private final Map<String, WaitingAgent> waitingGraph = new ConcurrentHashMap<>();
    private final long deadlockTimeoutMs = 30000; // 30 seconds

    public void recordWaiting(String agentId, String resourceId, long since) {
        waitingGraph.put(agentId, new WaitingAgent(agentId, resourceId, since));

        // Check for deadlock
        if (hasDeadlock()) {
            LOGGER.warn("Deadlock detected: {}", waitingGraph);
            resolveDeadlock();
        }
    }

    public void clearWaiting(String agentId) {
        waitingGraph.remove(agentId);
    }

    private boolean hasDeadlock() {
        // Detect cycles in waiting graph
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String agent : waitingGraph.keySet()) {
            if (hasCycleDFS(agent, visited, recursionStack)) {
                return true;
            }
        }

        // Check for timeout-based deadlock
        long now = System.currentTimeMillis();
        return waitingGraph.values().stream()
            .anyMatch(waiting -> now - waiting.since > deadlockTimeoutMs);
    }

    private boolean hasCycleDFS(String agent, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(agent)) {
            return true; // Back edge found - cycle
        }

        if (visited.contains(agent)) {
            return false; // Already checked
        }

        visited.add(agent);
        recursionStack.add(agent);

        WaitingAgent waiting = waitingGraph.get(agent);
        if (waiting != null) {
            // Check if the resource we're waiting for is also an agent
            String resourceAgent = waiting.resourceId;
            if (waitingGraph.containsKey(resourceAgent)) {
                if (hasCycleDFS(resourceAgent, visited, recursionStack)) {
                    return true;
                }
            }
        }

        recursionStack.remove(agent);
        return false;
    }

    private void resolveDeadlock() {
        // Strategy: Force lowest-priority agent to yield
        String yieldingAgent = waitingGraph.keySet().stream()
            .min(Comparator.comparing(this::getAgentPriority))
            .orElse(null);

        if (yieldingAgent != null) {
            LOGGER.warn("Forcing agent {} to yield to resolve deadlock", yieldingAgent);

            ForemanEntity agent = getCrewManager().getAgent(yieldingAgent);
            if (agent != null) {
                // Cancel current task and go idle
                agent.getActionExecutor().cancelCurrentTask();
                agent.sendChatMessage("Yielding to resolve deadlock");

                // Schedule retry
                scheduleRetry(agent, 5000); // Retry in 5 seconds
            }

            waitingGraph.remove(yieldingAgent);
        }
    }

    private int getAgentPriority(String agentId) {
        ForemanEntity agent = getCrewManager().getAgent(agentId);
        if (agent == null) return Integer.MAX_VALUE;

        // Lower priority = higher number (will yield first)
        return switch (agent.getRole()) {
            case BUILDER -> 1;
            case MINER -> 2;
            case GATHERER -> 3;
            case GUARD -> 4;
            default -> 5;
        };
    }

    private static class WaitingAgent {
        final String agentId;
        final String resourceId;
        final long since;

        WaitingAgent(String agentId, String resourceId, long since) {
            this.agentId = agentId;
            this.resourceId = resourceId;
            this.since = since;
        }
    }
}
```

### 6.2 Collaborative Error Recovery

```java
public class CollaborativeRecoveryCoordinator {
    private final CrewManager crewManager;
    private final CommunicationBus communicationBus;

    public ActionResult coordinateRecovery(ForemanEntity failedAgent, BaseAction failedAction, ActionResult failure) {
        // Determine if collaboration can help
        if (!canCollaborate(failedAction, failure)) {
            return ActionResult.failure("Recovery requires individual action", true);
        }

        // Find available agents
        List<ForemanEntity> availableAgents = findAvailableAgents(failedAgent);

        if (availableAgents.isEmpty()) {
            return ActionResult.failure("No other agents available for collaborative recovery", true);
        }

        // Assign recovery tasks
        return assignRecoveryTasks(failedAgent, failedAction, availableAgents);
    }

    private boolean canCollaborate(BaseAction action, ActionResult failure) {
        // Collaboration helps for:
        // - Resource shortage (other agents can provide)
        // - Mob interference (other agents can clear)
        // - Blockage (other agents can clear)
        // - Heavy tasks (can split workload)

        return failure.getMessage().contains("no materials") ||
               failure.getMessage().contains("hostile") ||
               failure.getMessage().contains("obstructed") ||
               action instanceof BuildStructureAction;
    }

    private List<ForemanEntity> findAvailableAgents(ForemanEntity excludeAgent) {
        return crewManager.getActiveAgents().stream()
            .filter(agent -> agent != excludeAgent)
            .filter(agent -> agent.getActionExecutor().canAcceptNewTask())
            .filter(agent -> !agent.getActionExecutor().isCriticalTask())
            .limit(3) // Max 3 helpers
            .toList();
    }

    private ActionResult assignRecoveryTasks(ForemanEntity failedAgent, BaseAction failedAction, List<ForemanEntity> helpers) {
        String failureMessage = failedAction.getResult().getMessage();

        // Assign tasks based on failure type
        if (failureMessage.contains("no materials")) {
            return assignMaterialRecovery(failedAgent, helpers);

        } else if (failureMessage.contains("hostile")) {
            return assignCombatSupport(failedAgent, helpers);

        } else if (failureMessage.contains("obstructed")) {
            return assignClearingSupport(failedAgent, helpers);

        } else if (failedAction instanceof BuildStructureAction) {
            return assignBuildAssistance(failedAgent, failedAction, helpers);
        }

        return ActionResult.failure("Unknown collaborative recovery scenario", true);
    }

    private ActionResult assignMaterialRecovery(ForemanEntity agent, List<ForemanEntity> helpers) {
        // Determine missing materials
        Map<String, Integer> missingMaterials = calculateMissingMaterials(agent);

        // Assign gathering tasks to helpers
        for (Map.Entry<String, Integer> material : missingMaterials.entrySet()) {
            if (helpers.isEmpty()) break;

            ForemanEntity helper = helpers.remove(0);

            Task gatherTask = new Task("gather", Map.of(
                "item", material.getKey(),
                "quantity", Math.min(material.getValue(), 64),
                "recipient", agent.getUUID().toString()
            ));

            helper.getActionExecutor().queueTask(gatherTask);
            helper.sendChatMessage(String.format("Gathering %d %s for %s",
                material.getValue(), material.getKey(), agent.getName()));
        }

        return ActionResult.pending("Waiting for materials from helpers");
    }

    private ActionResult assignCombatSupport(ForemanEntity agent, List<ForemanEntity> helpers) {
        // Find nearby hostiles
        List<Mob> hostiles = agent.level().getEntitiesOfClass(Mob.class,
            agent.getBoundingBox().inflate(20))
            .stream()
            .filter(m -> m instanceof Enemy)
            .toList();

        // Assign combat tasks
        for (Mob hostile : hostiles) {
            if (helpers.isEmpty()) break;

            ForemanEntity helper = helpers.remove(0);

            Task combatTask = new Task("attack", Map.of(
                "target", hostile.getEncodeId(),
                "protect", agent.getUUID().toString()
            ));

            helper.getActionExecutor().queueTask(combatTask);
            helper.sendChatMessage(String.format("Protecting %s from hostile", agent.getName()));
        }

        return ActionResult.pending("Combat support dispatched");
    }
}
```

---

## 7. Implementation Patterns

### 7.1 Retry Policy Configuration

```java
public class RetryPolicy {
    private final int maxAttempts;
    private final BackoffStrategy backoffStrategy;
    private final Predicate<Throwable> retryCondition;
    private final List<Class<? extends Throwable>> nonRetryableExceptions;

    public enum BackoffStrategy {
        IMMEDIATE,       // No delay
        FIXED,           // Fixed delay between attempts
        LINEAR,          // Increasing delay (100ms, 200ms, 300ms...)
        EXPONENTIAL,     // Exponential delay (1s, 2s, 4s, 8s...)
        EXPONENTIAL_JITTER  // Exponential with randomness
    }

    private RetryPolicy(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.backoffStrategy = builder.backoffStrategy;
        this.retryCondition = builder.retryCondition;
        this.nonRetryableExceptions = builder.nonRetryableExceptions;
    }

    public long calculateDelayMs(int attempt) {
        return switch (backoffStrategy) {
            case IMMEDIATE -> 0;
            case FIXED -> 1000;
            case LINEAR -> attempt * 100;
            case EXPONENTIAL -> (long) Math.pow(2, attempt) * 1000;
            case EXPONENTIAL_JITTER -> {
                long baseDelay = (long) Math.pow(2, attempt) * 1000;
                long jitter = (long) (baseDelay * 0.3 * (Math.random() - 0.5));
                yield Math.max(0, baseDelay + jitter);
            }
        };
    }

    public boolean shouldRetry(Throwable throwable, int attempt) {
        if (attempt >= maxAttempts) {
            return false;
        }

        // Check non-retryable exceptions
        for (Class<? extends Throwable> ex : nonRetryableExceptions) {
            if (ex.isInstance(throwable)) {
                return false;
            }
        }

        // Check retry condition
        return retryCondition.test(throwable);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int maxAttempts = 3;
        private BackoffStrategy backoffStrategy = BackoffStrategy.EXPONENTIAL_JITTER;
        private Predicate<Throwable> retryCondition = t -> true;
        private List<Class<? extends Throwable>> nonRetryableExceptions = new ArrayList<>();

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder backoffStrategy(BackoffStrategy backoffStrategy) {
            this.backoffStrategy = backoffStrategy;
            return this;
        }

        public Builder retryCondition(Predicate<Throwable> retryCondition) {
            this.retryCondition = retryCondition;
            return this;
        }

        public Builder nonRetryableExceptions(List<Class<? extends Throwable>> exceptions) {
            this.nonRetryableExceptions = exceptions;
            return this;
        }

        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
}
```

### 7.2 Action Execution with Retry

```java
public abstract class RetriableAction extends BaseAction {
    protected final RetryPolicy retryPolicy;
    protected int currentAttempt = 0;
    protected int ticksInRetry = 0;
    protected ActionResult lastFailure;

    public RetriableAction(ForemanEntity foreman, Task task, RetryPolicy retryPolicy) {
        super(foreman, task);
        this.retryPolicy = retryPolicy;
    }

    @Override
    protected void onTick() {
        if (isRetrying()) {
            handleRetryDelay();
            return;
        }

        try {
            performAction();
        } catch (Throwable throwable) {
            handleActionFailure(throwable);
        }
    }

    private boolean isRetrying() {
        return lastFailure != null && !lastFailure.isSuccess();
    }

    private void handleRetryDelay() {
        long requiredDelayTicks = retryPolicy.calculateDelayMs(currentAttempt) / 50;

        if (ticksInRetry >= requiredDelayTicks) {
            // Retry delay complete, attempt retry
            lastFailure = null;
            ticksInRetry = 0;
            foreman.sendChatMessage(String.format("Retry attempt %d/%d",
                currentAttempt + 1, retryPolicy.maxAttempts));
        } else {
            ticksInRetry++;
        }
    }

    private void handleActionFailure(Throwable throwable) {
        currentAttempt++;

        if (retryPolicy.shouldRetry(throwable, currentAttempt)) {
            lastFailure = ActionResult.fromThrowable(throwable);

            LOGGER.warn("[{}] Action failed, retrying in {}ms (attempt {}/{})",
                foreman.getName(),
                retryPolicy.calculateDelayMs(currentAttempt),
                currentAttempt,
                retryPolicy.maxAttempts);

        } else {
            // Max retries reached or non-retryable exception
            result = ActionResult.failure(
                String.format("Action failed after %d attempts: %s",
                    currentAttempt, throwable.getMessage()),
                true
            );
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        currentAttempt = 0;
        ticksInRetry = 0;
        lastFailure = null;
    }

    protected abstract void performAction() throws Throwable;
}
```

### 7.3 Fallback Chain Pattern

```java
public class FallbackChainAction extends BaseAction {
    private final List<FallbackAction> fallbackChain;
    private int currentFallbackIndex = 0;

    public FallbackChainAction(ForemanEntity foreman, Task task, List<FallbackAction> fallbackChain) {
        super(foreman, task);
        this.fallbackChain = fallbackChain;
    }

    @Override
    protected void onStart() {
        executeCurrentFallback();
    }

    @Override
    protected void onTick() {
        FallbackAction current = fallbackChain.get(currentFallbackIndex);
        current.tick();

        if (current.isComplete()) {
            ActionResult currentResult = current.getResult();

            if (currentResult.isSuccess()) {
                // Current fallback succeeded
                result = currentResult;

            } else if (currentFallbackIndex < fallbackChain.size() - 1) {
                // Try next fallback
                currentFallbackIndex++;
                foreman.sendChatMessage(String.format("Fallback %d failed, trying fallback %d",
                    currentFallbackIndex, currentFallbackIndex + 1));
                executeCurrentFallback();

            } else {
                // All fallbacks exhausted
                result = ActionResult.failure(
                    String.format("All %d fallback strategies failed", fallbackChain.size()),
                    true
                );
            }
        }
    }

    private void executeCurrentFallback() {
        FallbackAction fallback = fallbackChain.get(currentFallbackIndex);
        fallback.start();
    }

    @Override
    protected void onCancel() {
        fallbackChain.forEach(FallbackAction::cancel);
    }

    @Override
    public String getDescription() {
        return String.format("Fallback chain (%d strategies)", fallbackChain.size());
    }

    public interface FallbackAction {
        void start();
        void tick();
        boolean isComplete();
        ActionResult getResult();
        void cancel();
    }
}
```

---

## 8. Case Studies

### 8.1 Case Study: Automated Farm Construction

**Scenario**: Agent tasking with building a 20x20 crop farm.

**Errors Encountered**:
1. Navigation failure (water obstruction)
2. Resource depletion (ran out of dirt)
3. Block placement failure (spawn protection)
4. Mob interference (zombie targeted agent)

**Recovery Sequence**:

```java
public class FarmBuildRecovery {
    public ActionResult recoverFarmBuild(ForemanEntity agent, BuildStructureAction action, ActionResult failure) {
        return switch (identifyFailureType(failure)) {
            case NAVIGATION_WATER -> {
                // Pathfinding blocked by water
                yield recoverFromWaterBlockage(agent, action);
            }

            case RESOURCE_DEPLETION -> {
                // Out of building materials
                yield recoverFromResourceShortage(agent, action);
            }

            case SPAWN_PROTECTION -> {
                // Cannot place in spawn protection
                yield recoverFromSpawnProtection(agent, action);
            }

            case MOB_INTERFERENCE -> {
                // Hostile mob attacking
                yield recoverFromMobAttack(agent, action);
            }

            default -> yield ActionResult.failure("Unknown failure type", true);
        };
    }

    private ActionResult recoverFromWaterBlockage(ForemanEntity agent, BuildStructureAction action) {
        // Strategy: Place bridge across water
        BlockPos waterLocation = findBlockingWater(agent, action.getTargetArea());

        if (waterLocation != null) {
            Task bridgeTask = new Task("place_bridge", Map.of(
                "from", agent.blockPosition().toString(),
                "to", waterLocation.toString(),
                "material", "cobblestone"
            ));

            return ActionResult.pending("Building bridge across water", bridgeTask);
        }

        return ActionResult.failure("Cannot determine water blockage location", true);
    }

    private ActionResult recoverFromResourceShortage(ForemanEntity agent, BuildStructureAction action) {
        // Calculate remaining materials needed
        Map<String, Integer> needed = calculateRemainingMaterials(agent, action);

        // Request from storage or ask player
        if (requestMaterialsFromStorage(needed)) {
            return ActionResult.pending("Waiting for materials from storage");
        } else {
            agent.sendChatMessage(String.format(
                "I need %s to continue building the farm",
                formatMaterialsList(needed)
            ));

            // Wait for player to provide materials
            return ActionResult.pending("Waiting for player to provide materials");
        }
    }

    private ActionResult recoverFromSpawnProtection(ForemanEntity agent, BuildStructureAction action) {
        // Cannot place blocks in spawn-protected area
        // Strategy: Wait out protection timer or use alternative materials

        BlockPos problematicPos = findSpawnProtectedLocation(agent, action);

        if (problematicPos != null) {
            // Check if protection will expire soon
            ChunkAccess chunk = agent.level().getChunkAt(problematicPos);

            if (chunk.getInhabitedTime() < 100) {
                // Wait for protection to expire
                agent.sendChatMessage("Waiting for spawn protection to expire...");
                return ActionResult.pending("Waiting for spawn protection");

            } else {
                // Protection persistent, skip this block
                action.markBlockAsSkipped(problematicPos);
                return ActionResult.pending("Skipped spawn-protected block");
            }
        }

        return ActionResult.failure("Cannot handle spawn protection", false);
    }

    private ActionResult recoverFromMobAttack(ForemanEntity agent, BuildStructureAction action) {
        // Find attacking mob
        Mob attacker = findNearestHostile(agent, 10);

        if (attacker != null) {
            // Request combat support from other agents
            List<ForemanEntity> guards = requestCombatSupport(agent, attacker);

            if (!guards.isEmpty()) {
                agent.sendChatMessage(String.format(
                    "%s guard(s) engaging hostile",
                    guards.size()
                ));
                return ActionResult.pending("Waiting for combat support to clear area");
            }

            // No guards available, flee and wait
            Task fleeTask = new Task("flee", Map.of(
                "from", attacker.blockPosition().toString(),
                "distance", 20
            ));

            return ActionResult.pending("Fleeing from hostile", fleeTask);
        }

        return ActionResult.failure("No mob found despite error", false);
    }
}
```

### 8.2 Case Study: Deep Mining Operation

**Scenario**: Agent tasked with mining at Y=-58 for diamonds.

**Errors Encountered**:
1. Lava pool breach
2. Tool durability depletion
3. Gravel/sand collapse blocking path
4. Darkness (mob spawning)

**Recovery Sequence**:

```java
public class MiningRecovery {
    public ActionResult recoverMining(ForemanEntity agent, MineBlockAction action, ActionResult failure) {
        return switch (identifyMiningFailure(failure)) {
            case LAVA_BREACH -> handleLavaBreach(agent, action);
            case TOOL_BROKEN -> handleToolBreakage(agent, action);
            case COLLAPSE -> handleGravelCollapse(agent, action);
            case DANGER -> handleDangerousSituation(agent, action);
            default -> ActionResult.failure("Unknown mining failure", true);
        };
    }

    private ActionResult handleLavaBreach(ForemanEntity agent, MineBlockAction action) {
        agent.sendChatMessage("Lava breach detected!");

        // Priority: Escape and seal
        BlockPos lavaSource = findLavaSource(agent);

        if (lavaSource != null) {
            // Check if agent can escape
            BlockPos escapeRoute = findEscapeRoute(agent, lavaSource);

            if (escapeRoute != null) {
                // Escape first, then seal
                Task escapeTask = new Task("pathfind", Map.of(
                    "target", escapeRoute.toString(),
                    "priority", "URGENT"
                ));

                // Chain with sealing task
                Task sealTask = new Task("seal_lava", Map.of(
                    "location", lavaSource.toString(),
                    "material", "cobblestone"
                ));

                return ActionResult.pending("Escaping lava breach", escapeTask, sealTask);
            }

            // No escape, teleport to safety
            agent.teleportTo(agent.getRespawnPosition());
            agent.sendChatMessage("Teleported to safety from lava!");

            return ActionResult.success("Escaped lava via teleport");
        }

        return ActionResult.failure("Lava breach but source not found", true);
    }

    private ActionResult handleToolBreakage(ForemanEntity agent, MineBlockAction action) {
        ItemStack brokenTool = agent.getMainHandItem();

        if (brokenTool.isEmpty()) {
            agent.sendChatMessage("My tool broke!");

            // Check inventory for replacement
            ItemStack replacement = findReplacementTool(agent, action.getTargetBlock());

            if (replacement != null) {
                agent.setItemSlot(EquipmentSlot.MAINHAND, replacement);
                return ActionResult.pending("Equipped replacement tool");
            }

            // No replacement available
            agent.sendChatMessage("I need a replacement tool to continue mining");
            return ActionResult.failure("No replacement tool available", true);
        }

        return ActionResult.failure("Tool not actually broken?", false);
    }

    private ActionResult handleGravelCollapse(ForemanEntity agent, MineBlockAction action) {
        // Gravel or sand collapsed and blocked path
        agent.sendChatMessage("Path blocked by falling blocks");

        BlockPos collapseLocation = findCollapseBlock(agent);

        if (collapseLocation != null) {
            // Clear the collapse
            Task clearTask = new Task("clear_area", Map.of(
                "location", collapseLocation.toString(),
                "radius", 3
            ));

            // Place torches to prevent further collapses
            Task torchTask = new Task("place_torches", Map.of(
                "locations", List.of(
                    collapseLocation.above(),
                    collapseLocation.below(),
                    collapseLocation.north(),
                    collapseLocation.south(),
                    collapseLocation.east(),
                    collapseLocation.west()
                ).toString()
            ));

            return ActionResult.pending("Clearing collapse and placing torches", clearTask, torchTask);
        }

        return ActionResult.failure("Cannot locate collapse", true);
    }

    private ActionResult handleDangerousSituation(ForemanEntity agent, MineBlockAction action) {
        // Generic danger handler
        agent.sendChatMessage("Dangerous situation detected");

        // Assess threats
        List<Hazard> hazards = Hazards.scan(agent);

        if (hazards.isEmpty()) {
            // No current hazards, might have been transient
            return ActionResult.pending("Waiting for situation to stabilize");
        }

        // Prioritize survival
        Task survivalTask = createSurvivalTask(agent, hazards);

        return ActionResult.pending("Prioritizing survival", survivalTask);
    }
}
```

---

## 9. Best Practices

### 9.1 Error Recovery Design Principles

1. **Fail Fast, Fail Soft**
   - Detect errors early
   - Provide meaningful error messages
   - Never crash the game

2. **Transparent Recovery**
   - Inform agent of recovery actions
   - Log recovery attempts
   - Provide feedback to player

3. **Progressive Escalation**
   - Start with simple retries
   - Escalate to alternative approaches
   - Finally request player intervention

4. **Context Awareness**
   - Consider agent state
   - Consider environment
   - Consider available resources

5. **Learning from Failures**
   - Record failure patterns
   - Adapt behavior based on history
   - Share learning across agents

### 9.2 Implementation Guidelines

```java
// DO: Provide structured error information
public ActionResult failWithError(String message, ErrorCategory category, ErrorSeverity severity) {
    Map<String, Object> errorContext = Map.of(
        "message", message,
        "category", category,
        "severity", severity,
        "timestamp", System.currentTimeMillis(),
        "agentState", getAgentState(),
        "suggestions", generateRecoverySuggestions(category)
    );

    return ActionResult.failure(message, errorContext);
}

// DO: Use appropriate retry strategies
RetryPolicy navigationRetry = RetryPolicy.builder()
    .maxAttempts(5)
    .backoffStrategy(BackoffStrategy.EXPONENTIAL_JITTER)
    .retryCondition(throwable -> {
        if (throwable instanceof PathfindingException) {
            return ((PathfindingException) throwable).isRetryable();
        }
        return false;
    })
    .nonRetryableExceptions(List.of(
        ChunkNotLoadedException.class,
        EntityRemovedException.class
    ))
    .build();

// DON'T: Swallow exceptions
try {
    performAction();
} catch (Exception e) {
    // BAD: Silent failure
    return ActionResult.failure("Action failed");
}

// DO: Proper exception handling
try {
    performAction();
} catch (PathfindingException e) {
    LOGGER.warn("Pathfinding failed: {}", e.getMessage());
    return handlePathfindingFailure(e);
} catch (ResourceException e) {
    LOGGER.error("Resource shortage: {}", e.getMessage());
    return handleResourceShortage(e);
} catch (Throwable t) {
    LOGGER.error("Unexpected error", t);
    return handleUnexpectedError(t);
}
```

### 9.3 Testing Error Recovery

```java
@Test
public void testNavigationFailureRecovery() {
    // Setup
    ForemanEntity agent = createTestAgent();
    agent.setPosition(new Vec3(0, 64, 0));

    BlockPos target = new BlockPos(100, 64, 100);
    PathfindAction action = new PathfindAction(agent, createPathfindTask(target));

    // Simulate navigation failure (stuck)
    when(agent.level().getBlockState(any(BlockPos.class)))
        .thenReturn(Blocks.WATER.defaultBlockState());

    action.tick();

    // Verify recovery initiated
    ActionResult result = action.getResult();
    assertNotNull(result);
    assertTrue(result.requiresReplanning() || result.isPending());

    // Verify appropriate recovery strategy
    if (result.isPending()) {
        // Should have tried alternative approach
        verify(agent, atLeastOnce()).moveBackward(anyDouble());
    }
}

@Test
public void testResourceDepletionRecovery() {
    // Setup
    ForemanEntity agent = createTestAgent();
    agent.getInventory().clear();

    BuildStructureAction action = new BuildStructureAction(agent, createBuildTask());

    action.tick();

    // Verify resource shortage detected
    ActionResult result = action.getResult();
    assertTrue(result.getMessage().contains("materials") ||
               result.getMessage().contains("resources"));

    // Verify recovery initiated
    assertTrue(result.requiresReplanning());
}

@Test
public void testCircuitBreakerIntegration() {
    CircuitBreaker cb = new CircuitBreaker(3, 1000, 1);

    // Record failures until circuit opens
    cb.recordFailure();
    cb.recordFailure();
    cb.recordFailure();

    assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    assertFalse(cb.allowRequest());

    // Wait for timeout
    Thread.sleep(1100);

    // Should transition to HALF_OPEN
    assertTrue(cb.allowRequest());
    assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());

    // Successful probe closes circuit
    cb.recordSuccess();
    assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
}
```

---

## 10. Research Sources

### Academic and Industry Research

1. **"Exception Handling and Recovery in Autonomous Systems"** - Agent Design Patterns
   - Classification of failure modes
   - Recovery chain patterns
   - Adaptive error handling

2. **"Circuit Breaker Pattern for AI Applications"** - Cloud Architecture
   - Circuit breaker state management
   - Adaptive thresholds
   - Multi-level breakers

3. **"Self-Healing Systems: A Survey"** - IEEE Transactions
   - Automatic fault detection
   - Self-repair mechanisms
   - Degradation strategies

4. **"Multi-Agent Coordination Under Failure"** - Distributed Systems
   - Deadlock detection
   - Collaborative recovery
   - Fault-tolerant coordination

5. **"Exponential Backoff with Jitter"** - Google Cloud Platform
   - Retry storm prevention
   - Jitter algorithms
   - Load distribution

### Minecraft-Specific Research

1. **Baritone Pathfinding Analysis**
   - Navigation failure modes
   - Fallback strategies
   - Chunk unload handling

2. **Mineflayer Bot Framework**
   - Error detection patterns
   - Recovery implementations
   - State management

3. **Minecraft AI Community Research**
   - Common automation failures
   - Community solutions
   - Best practices

### Internal Documentation

1. `C:\Users\casey\steve\docs\AUDIT_ERROR_HANDLING.md` - Comprehensive error audit
2. `C:\Users\casey\steve\docs\ASYNC_ERROR_RECOVERY.md` - Async error patterns
3. `C:\Users\casey\steve\docs\research\ERROR_RECOVERY_PATTERNS.md` - General error recovery
4. `C:\Users\casey\steve\src\main\java\com\minewright\llm\resilience\` - Resilience implementations

---

## Appendix A: Quick Reference

### Common Error Types and Quick Fixes

| Error | Quick Fix | Escalation |
|-------|-----------|------------|
| Stuck | Jump, reverse, break obstruction | Teleport, ask player |
| Out of range | Move closer, extend reach | Use alternative target |
| No materials | Check storage, request from player | Skip task, change plan |
| Tool broken | Switch tool, request replacement | Use alternative method |
| Hostile mob | Fight, flee, call for help | Wait for player |
| Lava | Flee, place cobble, teleport | Abandon task |
| Spawn protection | Wait, skip block | Use alternative location |
| Unreachable | Mark unreachable, skip | Request new target |

### Recovery Strategy Selection Flowchart

```
Error Occurred
    |
    v
Is it retryable?
    YES -> Retry with backoff
    NO  -> v
         |
         v
Can we adjust parameters?
    YES -> Adjust and retry
    NO  -> v
         |
         v
Is there an alternative approach?
    YES -> Try alternative
    NO  -> v
         |
         v
Can other agents help?
    YES -> Coordinate collaborative recovery
    NO  -> v
         |
         v
Request player assistance
    |
    v
Log failure for learning
```

---

**Document Version:** 1.0
**Last Updated:** 2026-02-28
**Author:** Research compilation for MineWright/Steve AI project
**Status:** Ready for implementation

---

**End of Document**
