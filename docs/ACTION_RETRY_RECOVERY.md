# Action Retry and Recovery Patterns

## Overview

This document describes retry and recovery patterns for MineWright action execution. Actions in Minecraft can fail due to various environmental factors such as pathfinding issues, block changes, mob interference, and network problems. This document provides a comprehensive taxonomy of failure modes and strategies for handling them gracefully.

## Current State Analysis

### Existing Failure Handling

The current action system has basic failure handling:

**BaseAction.java** (C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BaseAction.java)
- `cancel()` method sets cancelled flag and calls `onCancel()` cleanup
- `isComplete()` checks for result or cancellation
- Actions track their own completion state

**ActionResult.java** (C:\Users\casey\steve\src\main\java\com\minewright\action\ActionResult.java)
- Success/failure tracking
- `requiresReplanning` flag for indicating when LLM needs to generate new tasks
- Factory methods: `success()` and `failure()`

**ActionExecutor.java** (C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java)
- Lines 272-281: Basic failure notification when actions complete unsuccessfully
- Checks `result.requiresReplanning()` for decisions
- Sends chat messages on failure

### Current Timeout Patterns

Different actions have different timeout values:
- `PathfindAction.MAX_TICKS = 600` (30 seconds)
- `PlaceBlockAction.MAX_TICKS = 200` (10 seconds)
- `MineBlockAction.MAX_TICKS = 24000` (20 minutes)
- `BuildStructureAction.MAX_TICKS = 120000` (100 minutes)
- `CombatAction.MAX_TICKS = 600` (30 seconds)

### Existing Resilience Patterns

The LLM layer already uses Resilience4j for fault tolerance:
- Circuit Breaker: Fail fast when provider is down
- Retry: Exponential backoff (1s, 2s, 4s)
- Rate Limiter: Prevent API quota exhaustion
- Bulkhead: Limit concurrent requests

These patterns should be adapted for action-level resilience.

## Failure Taxonomy

### 1. Transient Failures (Retryable)

These failures are temporary and can be resolved by retrying:

| Failure Type | Description | Example Actions | Recovery Strategy |
|--------------|-------------|-----------------|-------------------|
| **Pathfinding Blockage** | Navigation blocked by temporary entities | PathfindAction, FollowPlayerAction | Retry with delay, find alternate route |
| **Mob Interference** | Hostile mobs interrupting action | CombatAction, GatherResourceAction | Clear mobs, retry action |
| **Block Changes** | Target block changed during action | MineBlockAction, PlaceBlockAction | Re-scan, update target, retry |
| **Movement Stuck** | Entity stuck on terrain | All movement actions | Teleport unstuck, retry |
| **Resource Temporarily Unavailable** | Items/blocks not accessible | CraftItemAction, GatherResourceAction | Wait and retry |

### 2. Persistent Failures (Non-Retryable)

These failures require alternative approaches:

| Failure Type | Description | Recovery Strategy |
|--------------|-------------|-------------------|
| **Invalid Parameters** | Wrong block type, invalid coordinates | Validate parameters early, fail fast |
| **Missing Resources** | No materials in inventory | Trigger gather action, or request from player |
| **Unreachable Location** | Target in unloaded chunk, void | Mark as unreachable, skip or report |
| **Permission Denied** | Can't break/place blocks (spawn protection) | Inform user, suggest alternative location |
| **World Edge/Bedrock** | Trying to mine bedrock or beyond world | Detect early, skip with message |

### 3. Partial Failures (Recoverable)

Actions that partially succeeded before failing:

| Failure Type | Description | Recovery Strategy |
|--------------|-------------|-------------------|
| **Batch Operation** | Built 50/100 blocks, then failed | Preserve progress, continue remaining |
| **Multi-Stage Task** | Pathfound successfully, but placement failed | Use intermediate state, don't restart from scratch |
| **Resource Depletion** | Had materials, ran out mid-action | Record what was completed, request more materials |

### 4. Catastrophic Failures

Failures that require intervention:

| Failure Type | Description | Recovery Strategy |
|--------------|-------------|-------------------|
| **Chunk Unload** | Entity in unloaded chunk | Teleport to player, fail with message |
| **Server Shutdown** | Server stopping | Save state, fail gracefully |
| **Entity Despawn** | Foreman removed | Log event, cleanup resources |

## Retry Policy Design

### Retry Strategies

#### 1. Immediate Retry

Use for simple transient failures where the issue is likely already resolved:

```java
// Example: Block state mismatch
@Override
protected void onTick() {
    BlockState currentState = foreman.level().getBlockState(targetPos);

    if (currentState.getBlock() != targetBlock) {
        // Block changed, maybe it's back now?
        if (retryCount < MAX_IMMEDIATE_RETRIES) {
            retryCount++;
            return; // Try again next tick
        }

        // Still not matching after immediate retries
        result = ActionResult.failure("Block mismatch at " + targetPos);
    }
}
```

#### 2. Fixed Delay Retry

Use for failures that need a brief cooldown:

```java
// Example: Mob interference clearing
@Override
protected void onTick() {
    if (isMobNearby()) {
        if (ticksSinceLastAttempt < RETRY_DELAY_TICKS) {
            ticksSinceLastAttempt++;
            return; // Wait before retry
        }

        // Clear mobs and retry
        clearMobsInArea();
        ticksSinceLastAttempt = 0;
        retryCount++;

        if (retryCount > MAX_FIXED_RETRIES) {
            result = ActionResult.failure("Cannot clear mobs, giving up");
        }
        return;
    }
}
```

#### 3. Exponential Backoff

Use for rate limiting or when immediate retries cause issues:

```java
// Example: Rate-limited block placement
@Override
protected void onTick() {
    if (placementFailed) {
        if (retryCount >= MAX_EXPONENTIAL_RETRIES) {
            result = ActionResult.failure("Max retries exceeded");
            return;
        }

        int delayTicks = (int) Math.pow(2, retryCount) * 20; // 1s, 2s, 4s, 8s
        if (ticksWaiting < delayTicks) {
            ticksWaiting++;
            return;
        }

        // Retry after exponential delay
        ticksWaiting = 0;
        retryCount++;
        attemptPlacement();
    }
}
```

#### 4. Exponential Backoff with Jitter

Prevents thundering herd problem when multiple entities retry simultaneously:

```java
// Example: Multiple foremen competing for resources
@Override
protected void onTick() {
    if (resourceUnavailable) {
        if (retryCount >= MAX_JITTER_RETRIES) {
            result = ActionResult.failure("Resource unavailable");
            return;
        }

        // Base delay: 2^retryCount seconds
        int baseDelayTicks = (int) Math.pow(2, retryCount) * 20;

        // Add random jitter: +/- 50%
        int jitter = (int) (baseDelayTicks * 0.5 * (Math.random() - 0.5));
        int totalDelay = baseDelayTicks + jitter;

        if (ticksWaiting < totalDelay) {
            ticksWaiting++;
            return;
        }

        ticksWaiting = 0;
        retryCount++;
        attemptResourceAcquisition();
    }
}
```

### Retry Configuration

```java
/**
 * Retry policy configuration for different failure types
 */
public class RetryPolicy {
    private final int maxAttempts;
    private final BackoffStrategy backoffStrategy;
    private final Set<FailureType> retryableFailures;

    public enum BackoffStrategy {
        IMMEDIATE,      // No delay
        FIXED,          // Constant delay
        EXPONENTIAL,    // Exponential increase
        EXPONENTIAL_JITTER  // Exponential with randomness
    }

    /**
     * Pre-configured policies for common scenarios
     */
    public static final RetryPolicy PATHFINDING_RETRY = new RetryPolicy(
        3,                          // max 3 attempts
        BackoffStrategy.EXPONENTIAL_JITTER,
        EnumSet.of(FailureType.PATHFINDING_BLOCKED, FailureType.ENTITY_STUCK)
    );

    public static final RetryPolicy BLOCK_PLACEMENT_RETRY = new RetryPolicy(
        5,                          // max 5 attempts
        BackoffStrategy.EXPONENTIAL,
        EnumSet.of(FailureType.BLOCK_CHANGED, FailureType.MOB_INTERFERENCE)
    );

    public static final RetryPolicy MINING_RETRY = new RetryPolicy(
        2,                          // max 2 attempts (fail fast for mining)
        BackoffStrategy.IMMEDIATE,
        EnumSet.of(FailureType.BLOCK_CHANGED)
    );
}
```

### Retry State Machine

```java
/**
 * Enhanced BaseAction with retry support
 */
public abstract class RetriableAction extends BaseAction {
    protected int retryCount = 0;
    protected int ticksInCurrentRetry = 0;
    protected RetryPolicy retryPolicy;
    protected FailureType lastFailure;

    @Override
    protected void onTick() {
        if (shouldRetry()) {
            handleRetry();
            return;
        }

        attemptAction();
    }

    protected boolean shouldRetry() {
        if (lastFailure == null) return false;
        if (retryCount >= retryPolicy.getMaxAttempts()) return false;
        if (!retryPolicy.isRetryable(lastFailure)) return false;

        // Check backoff delay
        int requiredDelay = calculateBackoffDelay();
        return ticksInCurrentRetry < requiredDelay;
    }

    protected int calculateBackoffDelay() {
        return switch (retryPolicy.getBackoffStrategy()) {
            case IMMEDIATE -> 0;
            case FIXED -> 40; // 2 seconds
            case EXPONENTIAL -> (int) Math.pow(2, retryCount) * 20; // 1s, 2s, 4s...
            case EXPONENTIAL_JITTER -> {
                int baseDelay = (int) Math.pow(2, retryCount) * 20;
                int jitter = (int) (baseDelay * 0.5 * (Math.random() - 0.5));
                yield baseDelay + jitter;
            }
        };
    }

    protected void handleRetry() {
        ticksInCurrentRetry++;

        if (ticksInCurrentRetry >= calculateBackoffDelay()) {
            retryCount++;
            ticksInCurrentRetry = 0;
            lastFailure = null;
            onRetry();
        }
    }

    protected void onRetry() {
        // Called when retry attempt starts
        // Subclasses can override to reset state
    }

    protected void recordFailure(FailureType type, String message) {
        this.lastFailure = type;

        if (!retryPolicy.isRetryable(type) || retryCount >= retryPolicy.getMaxAttempts()) {
            result = ActionResult.failure(message + " (after " + retryCount + " retries)",
                requiresReplanning(type));
        }
    }

    protected boolean requiresReplanning(FailureType failureType) {
        // Determine if LLM needs to generate new tasks
        return switch (failureType) {
            case INVALID_PARAMETERS, UNREACHABLE_LOCATION, MISSING_RESOURCES -> true;
            default -> false;
        };
    }
}
```

## Recovery Action Chaining

### Recovery Actions Table

| Original Action | Failure Type | Recovery Action | Description |
|-----------------|--------------|-----------------|-------------|
| PathfindAction | Pathfinding blocked | TeleportAction | Teleport past obstacle |
| PlaceBlockAction | Missing materials | GatherResourceAction | Gather required materials |
| MineBlockAction | Block not found | PathfindAction + MineBlockAction | Search nearby area |
| BuildStructureAction | Position occupied | PlaceBlockAction (clear) | Remove obstruction |
| CraftItemAction | Missing crafting table | BuildStructureAction | Build crafting table |
| FollowPlayerAction | Player out of range | PathfindAction | Pathfind to player last position |

### Recovery Chain Implementation

```java
/**
 * Recovery action that chains alternative strategies
 */
public class RecoveryAction extends RetriableAction {
    private final BaseAction originalAction;
    private final List<Supplier<BaseAction>> recoveryChain;
    private int currentStrategy = 0;

    public RecoveryAction(BaseAction original, List<Supplier<BaseAction>> recoveryStrategies) {
        super(original.foreman, original.task);
        this.originalAction = original;
        this.recoveryChain = recoveryStrategies;
    }

    @Override
    protected void onStart() {
        tryCurrentStrategy();
    }

    @Override
    protected void onTick() {
        BaseAction currentAction = getCurrentAction();
        currentAction.tick();

        if (currentAction.isComplete()) {
            ActionResult result = currentAction.getResult();

            if (result.isSuccess()) {
                // Current strategy succeeded
                this.result = result;
            } else if (currentStrategy < recoveryChain.size()) {
                // Try next recovery strategy
                currentStrategy++;
                tryCurrentStrategy();
            } else {
                // All strategies failed
                this.result = ActionResult.failure(
                    "All recovery strategies failed: " + result.getMessage()
                );
            }
        }
    }

    private void tryCurrentStrategy() {
        if (currentStrategy == 0) {
            // Try original action first
            // Already initialized
        } else {
            // Try recovery strategy
            Supplier<BaseAction> recoverySupplier = recoveryChain.get(currentStrategy - 1);
            BaseAction recoveryAction = recoverySupplier.get();
            recoveryAction.start();
        }
    }

    private BaseAction getCurrentAction() {
        if (currentStrategy == 0) return originalAction;
        return recoveryChain.get(currentStrategy - 1).get();
    }

    @Override
    protected void onCancel() {
        if (originalAction != null) originalAction.cancel();
        for (Supplier<BaseAction> supplier : recoveryChain) {
            BaseAction action = supplier.get();
            if (action != null) action.cancel();
        }
    }

    @Override
    public String getDescription() {
        return "Recovery: " + getCurrentAction().getDescription();
    }
}
```

### Recovery Chain Usage Example

```java
/**
 * Example: Place block with multiple recovery strategies
 */
public class ResilientPlaceBlockAction extends BaseAction {
    private BaseAction currentAttempt;

    @Override
    protected void onStart() {
        List<Supplier<BaseAction>> recoveryChain = new ArrayList<>();

        // Strategy 1: Normal placement (original)
        recoveryChain.add(() -> new PlaceBlockAction(foreman, task));

        // Strategy 2: Clear obstruction and retry
        recoveryChain.add(() -> {
            // First clear the block
            BlockPos targetPos = getTargetPosition();
            foreman.level().destroyBlock(targetPos, true);

            // Then retry placement
            return new PlaceBlockAction(foreman, task);
        });

        // Strategy 3: Pathfind closer and retry
        recoveryChain.add(() -> {
            return new CompositeAction(foreman, task,
                // First pathfind closer
                new PathfindAction(foreman, createPathfindTask()),
                // Then place block
                new PlaceBlockAction(foreman, task)
            );
        });

        // Strategy 4: Request player assistance
        recoveryChain.add(() -> {
            foreman.sendChatMessage("I need help placing this block at " + getTargetPosition());
            return new WaitForPlayerAction(foreman, task);
        });

        currentAttempt = new RecoveryAction(
            new PlaceBlockAction(foreman, task),
            recoveryChain
        );
        currentAttempt.start();
    }

    @Override
    protected void onTick() {
        currentAttempt.tick();
        if (currentAttempt.isComplete()) {
            result = currentAttempt.getResult();
        }
    }

    @Override
    protected void onCancel() {
        if (currentAttempt != null) currentAttempt.cancel();
    }

    @Override
    public String getDescription() {
        return currentAttempt != null ? currentAttempt.getDescription() : "Place block (resilient)";
    }
}
```

## Partial Progress Preservation

### State Snapshot Pattern

Preserve progress when actions need to restart:

```java
/**
 * Action that preserves partial progress
 */
public abstract class StatefulAction extends BaseAction {
    protected ActionState savedState;

    /**
     * Snapshot current state for recovery
     */
    protected void saveState() {
        savedState = new ActionState();
        savedState.timestamp = System.currentTimeMillis();
        savedState.progress = calculateProgress();
        savedState.partialResults = getPartialResults();
        savedState.context = getContext();
    }

    /**
     * Restore from saved state
     */
    protected void restoreState(ActionState state) {
        this.savedState = state;
        applyPartialResults(state.partialResults);
        setContext(state.context);
    }

    /**
     * Calculate progress percentage (0-100)
     */
    protected abstract int calculateProgress();

    /**
     * Get partial results to save
     */
    protected abstract Map<String, Object> getPartialResults();

    /**
     * Apply saved partial results
     */
    protected abstract void applyPartialResults(Map<String, Object> partialResults);

    /**
     * Get action context for recovery
     */
    protected abstract Map<String, Object> getContext();

    /**
     * Set action context from recovery
     */
    protected abstract void setContext(Map<String, Object> context);

    public static class ActionState implements Serializable {
        public long timestamp;
        public int progress;
        public Map<String, Object> partialResults;
        public Map<String, Object> context;
    }
}
```

### Example: Build Structure with Progress Preservation

```java
/**
 * Enhanced BuildStructureAction that preserves progress
 */
public class ResilientBuildStructureAction extends StatefulAction {
    private Set<BlockPos> placedBlocks;  // Track what was placed

    @Override
    protected void onStart() {
        placedBlocks = new HashSet<>();

        // Check for saved state
        ActionState savedState = loadSavedState();
        if (savedState != null) {
            restoreState(savedState);
            foreman.sendChatMessage("Continuing build from " + savedState.progress + "%");
        }
    }

    @Override
    protected void onTick() {
        // Place block
        BlockPos nextPos = getNextBlockPosition();
        placeBlock(nextPos);
        placedBlocks.add(nextPos);

        // Save progress every 10 blocks
        if (placedBlocks.size() % 10 == 0) {
            saveState();
        }

        if (placedBlocks.size() >= buildPlan.size()) {
            result = ActionResult.success("Built structure with " + placedBlocks.size() + " blocks");
            clearSavedState(); // Cleanup
        }
    }

    @Override
    protected int calculateProgress() {
        if (buildPlan.isEmpty()) return 0;
        return (placedBlocks.size() * 100) / buildPlan.size();
    }

    @Override
    protected Map<String, Object> getPartialResults() {
        Map<String, Object> partial = new HashMap<>();
        partial.put("placedBlocks", new ArrayList<>(placedBlocks));
        partial.put("currentIndex", currentBlockIndex);
        return partial;
    }

    @Override
    protected void applyPartialResults(Map<String, Object> partialResults) {
        @SuppressWarnings("unchecked")
        List<BlockPos> saved = (List<BlockPos>) partialResults.get("placedBlocks");
        if (saved != null) {
            placedBlocks = new HashSet<>(saved);
            currentBlockIndex = (int) partialResults.get("currentIndex");
        }
    }

    @Override
    protected Map<String, Object> getContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("structureType", structureType);
        context.put("startPos", startPos);
        return context;
    }

    @Override
    protected void setContext(Map<String, Object> context) {
        this.structureType = (String) context.get("structureType");
        this.startPos = (BlockPos) context.get("startPos");
    }

    @Override
    protected void onCancel() {
        // Save state on cancellation for potential recovery
        saveState();
    }

    private ActionState loadSavedState() {
        // Load from persistent storage (NBT, file, etc.)
        String key = "build_" + structureType + "_" + startPos.hashCode();
        return PersistenceManager.loadActionState(key);
    }

    private void saveState() {
        ActionState state = new ActionState();
        state.progress = calculateProgress();
        state.partialResults = getPartialResults();
        state.context = getContext();

        String key = "build_" + structureType + "_" + startPos.hashCode();
        PersistenceManager.saveActionState(key, state);
    }

    private void clearSavedState() {
        String key = "build_" + structureType + "_" + startPos.hashCode();
        PersistenceManager.deleteActionState(key);
    }
}
```

## Action Cancellation and Cleanup

### Cleanup Protocol

```java
/**
 * Enhanced cancellation with proper resource cleanup
 */
public abstract class CancellableAction extends BaseAction {
    private final List<AutoCloseable> resources;
    private volatile boolean cancelled = false;

    public CancellableAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.resources = new ArrayList<>();
    }

    /**
     * Register a resource for cleanup on cancellation
     */
    protected <T extends AutoCloseable> T registerResource(T resource) {
        if (!cancelled) {
            resources.add(resource);
        }
        return resource;
    }

    @Override
    public void cancel() {
        if (cancelled) return; // Already cancelled

        cancelled = true;

        // Close all registered resources in reverse order
        for (int i = resources.size() - 1; i >= 0; i--) {
            try {
                resources.get(i).close();
            } catch (Exception e) {
                MineWrightMod.LOGGER.error("Error cleaning up resource: " + e.getMessage(), e);
            }
        }
        resources.clear();

        // Call subclass cleanup
        onCancel();

        result = ActionResult.failure("Action cancelled");
    }

    /**
     * Check if action should abort early
     */
    protected boolean checkCancelled() {
        return cancelled;
    }

    @Override
    protected void onTick() {
        if (checkCancelled()) {
            return;
        }

        // Normal action logic
        performAction();
    }

    protected abstract void performAction();
}
```

### Cleanup Examples

```java
/**
 * Example: Mining with proper cleanup
 */
public class SafeMineBlockAction extends CancellableAction {
    private NavigationPath navigationPath;

    @Override
    protected void onStart() {
        // Register navigation for cleanup
        navigationPath = registerResource(
            foreman.getNavigation().createPath(targetPos)
        );

        // Register flying state cleanup
        registerResource(() -> {
            foreman.setFlying(false);
            foreman.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
    }

    @Override
    protected void performAction() {
        if (checkCancelled()) return;

        // Mine block
        foreman.level().destroyBlock(currentTarget, true);
        minedCount++;

        // Check for cancellation between blocks
        if (checkCancelled()) return;

        findNextBlock();
    }

    @Override
    protected void onCancel() {
        // Additional cleanup specific to mining
        foreman.getNavigation().stop();

        // Drop any held items
        foreman.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        MineWrightMod.LOGGER.info("Mining cancelled at {} blocks mined", minedCount);
    }

    @Override
    public String getDescription() {
        return "Mining " + targetBlock.getName().getString();
    }
}
```

## User Notification of Failures

### Notification Levels

| Level | Trigger | User Action Required | Example |
|-------|---------|---------------------|---------|
| INFO | Successful completion | No | "Built house successfully!" |
| WARNING | Recoverable failure | Optional | "Path blocked, taking alternative route" |
| ERROR | Unrecoverable failure | Yes (usually) | "Can't reach target location" |
| CRITICAL | Multiple failures | Yes | "Stuck in combat loop, please help" |

### Notification System

```java
/**
 * Failure notification system
 */
public class ActionFailureNotifier {

    public enum NotificationLevel {
        INFO,     // Just informational
        WARNING,  // Recoverable issue
        ERROR,    // Needs attention
        CRITICAL  // Urgent intervention needed
    }

    public static void notifyFailure(ForemanEntity foreman,
                                     BaseAction action,
                                     ActionResult result,
                                     NotificationLevel level) {
        String message = formatFailureMessage(action, result, level);

        // Log the failure
        logFailure(foreman, action, result, level);

        // Send in-game notification based on level
        switch (level) {
            case INFO -> sendToGUI(foreman, message);
            case WARNING -> sendToGUI(foreman, message);
            case ERROR -> sendToChat(foreman, message);
            case CRITICAL -> {
                sendToChat(foreman, message);
                sendTitle(foreman, "Action Failed", message);
                playSound(foreman, SoundEvents.NOTE_BLOCK_DIDGERIDOO);
            }
        }

        // Store in memory for learning
        foreman.getMemory().recordFailure(action.getClass().getSimpleName(),
                                         result.getMessage(),
                                         level);
    }

    private static String formatFailureMessage(BaseAction action,
                                              ActionResult result,
                                              NotificationLevel level) {
        String actionName = action.getClass().getSimpleName();
        String failureReason = result.getMessage();

        return switch (level) {
            case INFO -> String.format("%s completed: %s", actionName, failureReason);
            case WARNING -> String.format("Issue with %s: %s (trying to recover...)",
                                        actionName, failureReason);
            case ERROR -> String.format("Failed to %s: %s",
                                       actionName.replace("Action", "").toLowerCase(),
                                       failureReason);
            case CRITICAL -> String.format("CRITICAL: %s failed - %s",
                                         actionName, failureReason);
        };
    }

    /**
     * Suggest recovery actions to user
     */
    public static void suggestRecovery(ForemanEntity foreman,
                                      BaseAction action,
                                      FailureType failureType) {
        List<String> suggestions = getRecoverySuggestions(failureType);

        if (!suggestions.isEmpty()) {
            foreman.sendChatMessage("Recovery options:");
            for (int i = 0; i < suggestions.size(); i++) {
                foreman.sendChatMessage("  " + (i + 1) + ". " + suggestions.get(i));
            }
        }
    }

    private static List<String> getRecoverySuggestions(FailureType failureType) {
        return switch (failureType) {
            case PATHFINDING_BLOCKED -> List.of(
                "Clear the path manually",
                "Teleport me closer to target",
                "Choose a different target location"
            );
            case MISSING_RESOURCES -> List.of(
                "Provide the required materials",
                "Ask me to gather resources first",
                "Use alternative materials"
            );
            case MOB_INTERFERENCE -> List.of(
                "Clear mobs in the area",
                "Wait for mobs to despawn",
                "Move to a safer location"
            );
            default -> List.of(
                "Check the area manually",
                "Try a different command",
                "Ask me to try again later"
            );
        };
    }
}
```

### Interactive Recovery Prompt

```java
/**
 * Interactive recovery prompt for user
 */
public class RecoveryPrompt {
    private final ForemanEntity foreman;
    private final BaseAction failedAction;
    private final ActionResult failureResult;

    public RecoveryPrompt(ForemanEntity foreman, BaseAction action, ActionResult result) {
        this.foreman = foreman;
        this.failedAction = action;
        this.failureResult = result;
    }

    /**
     * Show recovery options to player
     */
    public void showRecoveryOptions() {
        String actionName = failedAction.getClass().getSimpleName();

        // Send title
        sendTitle(foreman, actionName + " Failed", failureResult.getMessage());

        // Get recovery options
        List<RecoveryOption> options = generateRecoveryOptions();

        // Display options
        foreman.sendChatMessage("What should I do?");
        for (int i = 0; i < options.size(); i++) {
            RecoveryOption option = options.get(i);
            foreman.sendChatMessage(String.format("  [%d] %s", i + 1, option.description));
        }

        // Store options for response handling
        storePendingRecovery(options);
    }

    private List<RecoveryOption> generateRecoveryOptions() {
        List<RecoveryOption> options = new ArrayList<>();

        // Option 1: Retry action
        options.add(new RecoveryOption(
            "Retry the action",
            () -> retryAction()
        ));

        // Option 2: Skip this step
        options.add(new RecoveryOption(
            "Skip this step and continue",
            () -> skipAction()
        ));

        // Option 3: Request help
        options.add(new RecoveryOption(
            "Wait for your help",
            () -> waitForHelp()
        ));

        // Context-specific options
        if (failedAction instanceof PlaceBlockAction) {
            options.add(new RecoveryOption(
                "Clear the area and retry",
                () -> clearAndRetry()
            ));
        } else if (failedAction instanceof PathfindAction) {
            options.add(new RecoveryOption(
                "Teleport to target",
                () -> teleportToTarget()
            ));
        } else if (failedAction instanceof MineBlockAction) {
            options.add(new RecoveryOption(
                "Search in a larger area",
                () -> expandSearchArea()
            ));
        }

        return options;
    }

    private void retryAction() {
        foreman.getActionExecutor().queueTask(failedAction.task);
        foreman.sendChatMessage("Retrying action...");
    }

    private void skipAction() {
        foreman.sendChatMessage("Skipping this step");
        // Continue to next task
    }

    private void waitForHelp() {
        foreman.sendChatMessage("I'll wait here for you");
        // Enter waiting state
    }

    private void clearAndRetry() {
        // Implementation specific to action type
    }

    private void teleportToTarget() {
        // Implementation for pathfinding recovery
    }

    private void expandSearchArea() {
        // Implementation for mining recovery
    }

    private void storePendingRecovery(List<RecoveryOption> options) {
        // Store in entity data for command response
        foreman.getPersistentData().put("recovery_options", options);
    }

    public record RecoveryOption(String description, Runnable action) {}
}
```

## Integration with Existing Action System

### Enhanced ActionExecutor Integration

```java
/**
 * Enhanced ActionExecutor with retry and recovery support
 */
public class ResilientActionExecutor extends ActionExecutor {
    private final ActionRecoveryManager recoveryManager;
    private final FailureTracker failureTracker;

    public ResilientActionExecutor(ForemanEntity foreman) {
        super(foreman);
        this.recoveryManager = new ActionRecoveryManager(foreman);
        this.failureTracker = new FailureTracker(foreman.getSteveName());
    }

    @Override
    protected void onActionComplete(BaseAction action, ActionResult result) {
        // Track the result
        failureTracker.recordResult(action.getClass().getSimpleName(), result);

        if (result.isSuccess()) {
            // Success - continue as normal
            super.onActionComplete(action, result);
        } else {
            // Failure - handle with recovery
            handleFailure(action, result);
        }
    }

    private void handleFailure(BaseAction action, ActionResult result) {
        FailureType failureType = classifyFailure(action, result);

        // Check if we should retry
        if (shouldRetry(action, failureType)) {
            int retryCount = failureTracker.getRetryCount(action.getClass().getSimpleName());

            if (retryCount < getMaxRetries(action)) {
                MineWrightMod.LOGGER.info("Retrying action {} (attempt {}/{})",
                    action.getClass().getSimpleName(), retryCount + 1, getMaxRetries(action));

                // Retry with backoff
                scheduleRetry(action, retryCount);
                return;
            }
        }

        // Check if we have recovery strategies
        List<BaseAction> recoveryActions = recoveryManager.getRecoveryActions(action, failureType);

        if (!recoveryActions.isEmpty()) {
            MineWrightMod.LOGGER.info("Executing recovery strategies for {}",
                action.getClass().getSimpleName());

            // Execute recovery chain
            executeRecoveryChain(action, recoveryActions);
        } else {
            // No recovery available - notify user
            notifyFailure(action, result, failureType);

            // Check if we need replanning
            if (result.requiresReplanning()) {
                triggerReplanning(action, result);
            }
        }
    }

    private boolean shouldRetry(BaseAction action, FailureType failureType) {
        // Check if failure is retryable
        return switch (failureType) {
            case PATHFINDING_BLOCKED, MOB_INTERFERENCE, BLOCK_CHANGED,
                 MOVEMENT_STUCK, RESOURCE_TEMPORARILY_UNAVAILABLE -> true;
            default -> false;
        };
    }

    private int getMaxRetries(BaseAction action) {
        // Action-specific retry limits
        return switch (action.getClass().getSimpleName()) {
            case "PathfindAction" -> 3;
            case "PlaceBlockAction" -> 5;
            case "MineBlockAction" -> 2;
            case "CombatAction" -> 1;
            default -> 1;
        };
    }

    private void scheduleRetry(BaseAction action, int retryCount) {
        // Calculate backoff delay
        int delayTicks = (int) Math.pow(2, retryCount) * 20; // Exponential backoff

        // Schedule retry
        Task retryTask = action.task;

        // Use existing scheduling mechanism
        queueTaskWithDelay(retryTask, delayTicks);
    }

    private void executeRecoveryChain(BaseAction originalAction, List<BaseAction> recoveryActions) {
        // Create recovery chain
        RecoveryAction recovery = new RecoveryAction(originalAction, recoveryActions);
        recovery.start();

        currentAction = recovery;
    }

    private FailureType classifyFailure(BaseAction action, ActionResult result) {
        // Classify failure based on result message and action type
        String message = result.getMessage().toLowerCase();

        if (message.contains("timeout") || message.contains("took too long")) {
            return FailureType.TIMEOUT;
        } else if (message.contains("path") || message.contains("stuck")) {
            return FailureType.PATHFINDING_BLOCKED;
        } else if (message.contains("block") && message.contains("not found")) {
            return FailureType.BLOCK_CHANGED;
        } else if (message.contains("mob") || message.contains("entity")) {
            return FailureType.MOB_INTERFERENCE;
        } else if (message.contains("invalid") || message.contains("unknown")) {
            return FailureType.INVALID_PARAMETERS;
        } else if (message.contains("no") && message.contains("material")) {
            return FailureType.MISSING_RESOURCES;
        } else {
            return FailureType.UNKNOWN;
        }
    }
}
```

### Failure Tracker

```java
/**
 * Track failures and retry counts for each action type
 */
public class FailureTracker {
    private final String agentId;
    private final Map<String, ActionFailureStats> stats;

    public FailureTracker(String agentId) {
        this.agentId = agentId;
        this.stats = new ConcurrentHashMap<>();
    }

    public void recordResult(String actionType, ActionResult result) {
        ActionFailureStats actionStats = stats.computeIfAbsent(
            actionType, k -> new ActionFailureStats()
        );

        if (result.isSuccess()) {
            actionStats.successCount++;
            actionStats.consecutiveFailures = 0;
        } else {
            actionStats.failureCount++;
            actionStats.consecutiveFailures++;
            actionStats.lastFailureMessage = result.getMessage();
            actionStats.lastFailureTime = System.currentTimeMillis();
        }
    }

    public int getRetryCount(String actionType) {
        ActionFailureStats actionStats = stats.get(actionType);
        return actionStats != null ? actionStats.consecutiveFailures : 0;
    }

    public double getSuccessRate(String actionType) {
        ActionFailureStats actionStats = stats.get(actionType);
        if (actionStats == null || actionStats.getTotalAttempts() == 0) {
            return 1.0;
        }

        return (double) actionStats.successCount / actionStats.getTotalAttempts();
    }

    public boolean shouldAvoidAction(String actionType) {
        ActionFailureStats actionStats = stats.get(actionType);

        // Avoid if high failure rate or many consecutive failures
        return actionStats != null &&
               (actionStats.consecutiveFailures >= 5 ||
                getSuccessRate(actionType) < 0.2);
    }

    public static class ActionFailureStats {
        private int successCount = 0;
        private int failureCount = 0;
        private int consecutiveFailures = 0;
        private String lastFailureMessage;
        private long lastFailureTime;

        public int getTotalAttempts() {
            return successCount + failureCount;
        }

        // Getters...
    }
}
```

## Best Practices

### 1. Action Design Guidelines

**DO:**
- Implement proper timeout handling
- Check for cancellation in long-running loops
- Save partial progress periodically
- Clean up resources in `onCancel()`
- Use descriptive failure messages
- Set `requiresReplanning` appropriately

**DON'T:**
- Block indefinitely without timeout
- Ignore entity state changes
- Leave resources open on failure
- Use generic failure messages
- Assume world state won't change

### 2. Retry Strategy Selection

| Scenario | Strategy | Reason |
|----------|----------|--------|
| Pathfinding blocked | Exponential with jitter | Multiple entities may retry |
| Block placement failed | Fixed delay | Need cooldown between attempts |
| Resource temporarily unavailable | Exponential backoff | May take time to become available |
| Mob interference | Immediate | Clear mobs and retry quickly |
| Network/async issues | Exponential with jitter | Standard retry pattern |

### 3. Recovery Chain Design

Order recovery strategies from least to most invasive:

1. **Retry same action** - Cheapest, works for transient issues
2. **Adjust parameters** - Try with different values (position, materials)
3. **Clear obstructions** - Remove blocking entities/blocks
4. **Alternative approach** - Use different method to achieve goal
5. **Request help** - Last resort, requires user intervention

### 4. State Preservation

Save state at these points:
- Before starting expensive operations
- After completing atomic units of work
- Periodically during long operations
- On cancellation (for potential recovery)

### 5. User Communication

Be transparent about failures:
- Inform user of issues (don't silently retry)
- Explain what went wrong
- Suggest recovery options
- Learn from failures (record in memory)

## Implementation Checklist

When implementing retry and recovery for an action:

- [ ] Add retry count tracking
- [ ] Implement appropriate backoff strategy
- [ ] Add state snapshot/restore for partial progress
- [ ] Implement proper cleanup in `onCancel()`
- [ ] Set descriptive failure messages
- [ ] Determine if failure requires replanning
- [ ] Add recovery strategies (if applicable)
- [ ] Configure retry limits
- [ ] Add failure tracking
- [ ] Test failure scenarios

## Testing Strategies

### Unit Testing

```java
@Test
public void testPathfindingRetry() {
    // Mock pathfinding to fail twice, then succeed
    when(navigation.isDone()).thenReturn(false, false, true);

    PathfindAction action = new PathfindAction(foreman, task);
    action.start();

    // Tick through retries
    for (int i = 0; i < 600; i++) {
        action.tick();
        if (action.isComplete()) break;
    }

    assertTrue(action.getResult().isSuccess());
    assertEquals(2, action.getRetryCount());
}

@Test
public void testMaxRetriesExceeded() {
    // Mock to always fail
    when(navigation.isDone()).thenReturn(false);

    PlaceBlockAction action = new PlaceBlockAction(foreman, task);
    action.setMaxRetries(3);
    action.start();

    // Tick through max retries
    for (int i = 0; i < 1000; i++) {
        action.tick();
        if (action.isComplete()) break;
    }

    assertFalse(action.getResult().isSuccess());
    assertTrue(action.getResult().getMessage().contains("max retries"));
}
```

### Integration Testing

Test realistic failure scenarios:
- Block disappears during mining
- Pathfinding gets stuck behind wall
- Mob interferes with action
- Resources run out mid-task
- Player moves target during action

## Configuration

### Config File (config/steve-common.toml)

```toml
[actionRetry]
# Enable/disable automatic retries
enabled = true

# Default retry policy
maxRetries = 3
backoffStrategy = "EXPONENTIAL_JITTER" # IMMEDIATE, FIXED, EXPONENTIAL, EXPONENTIAL_JITTER
baseDelayTicks = 20  # 1 second

# Action-specific overrides
[actionRetry.pathfind]
maxRetries = 5
backoffStrategy = "EXPONENTIAL_JITTER"

[actionRetry.placeBlock]
maxRetries = 5
backoffStrategy = "EXPONENTIAL"

[actionRetry.mine]
maxRetries = 2
backoffStrategy = "IMMEDIATE"

[actionRecovery]
# Enable automatic recovery chains
enabled = true

# Enable user prompts for unrecoverable failures
promptUser = true

# Save partial progress for long-running actions
saveProgress = true
progressSaveInterval = 100  # ticks
```

## Future Enhancements

1. **Machine Learning**: Learn from failures to predict and prevent issues
2. **Collaborative Recovery**: Multiple foremen can help each other recover
3. **Smart Retry**: Use success rate history to adapt retry strategies
4. **Rollback**: Undo partial actions on certain failures
5. **Predictive Prevention**: Detect potential failures before they occur

## References

- **Existing Code**:
  - `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BaseAction.java`
  - `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`
  - `C:\Users\casey\steve\src\main\java\com\minewright\llm\resilience\ResilientLLMClient.java`
  - `C:\Users\casey\steve\src\main\java\com\minewright\llm\resilience\ResilienceConfig.java`
  - `C:\Users\casey\steve\src\main\java\com\minewright\execution\AgentStateMachine.java`

- **Design Patterns**:
  - State Pattern (AgentStateMachine)
  - Decorator Pattern (ResilientLLMClient)
  - Chain of Responsibility (RecoveryAction)
  - Template Method (BaseAction)

- **Libraries**:
  - Resilience4j: Circuit breaker, retry, rate limiter, bulkhead

---

**Document Version**: 1.0
**Last Updated**: 2026-02-27
**Author**: Research and analysis of MineWright action system
