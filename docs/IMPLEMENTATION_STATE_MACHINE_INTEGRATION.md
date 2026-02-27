# State Machine Integration Implementation Guide

## Overview

The `AgentStateMachine` is instantiated in `ActionExecutor` but state transitions are never called. This document provides the exact code changes needed to properly integrate the state machine into the action execution flow.

## Current State

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`

- Line 67: `AgentStateMachine` is instantiated
- Line 80: Passed to `ActionContext`
- Line 411: Reset is called in `stopCurrentAction()`
- **Missing:** No transitions are called during normal execution flow

## Required State Transitions

### 1. IDLE → PLANNING when `processNaturalLanguageCommand()` starts

**Location:** `processNaturalLanguageCommand()` method, after line 125 (after the `isPlanning` check)

**Add this code:**

```java
// Transition to PLANNING state
if (!stateMachine.transitionTo(AgentState.PLANNING, "Command received: " + command)) {
    MineWrightMod.LOGGER.warn("Foreman '{}' cannot transition to PLANNING state (current: {})",
        foreman.getSteveName(), stateMachine.getCurrentState());
    sendToGUI(foreman.getSteveName(), "I'm busy right now, try again in a moment.");
    return;
}
```

**Full context (lines 117-127 modified):**

```java
public void processNaturalLanguageCommand(String command) {
    MineWrightMod.LOGGER.info("Foreman '{}' processing command (async): {}", foreman.getSteveName(), command);

    // If already planning, ignore new commands
    if (isPlanning) {
        MineWrightMod.LOGGER.warn("Foreman '{}' is already planning, ignoring command: {}", foreman.getSteveName(), command);
        sendToGUI(foreman.getSteveName(), "Hold on, I'm still thinking about the previous command...");
        return;
    }

    // NEW: Transition to PLANNING state
    if (!stateMachine.transitionTo(AgentState.PLANNING, "Command received: " + command)) {
        MineWrightMod.LOGGER.warn("Foreman '{}' cannot transition to PLANNING state (current: {})",
            foreman.getSteveName(), stateMachine.getCurrentState());
        sendToGUI(foreman.getSteveName(), "I'm busy right now, try again in a moment.");
        return;
    }

    // Cancel any current actions
    if (currentAction != null) {
        currentAction.cancel();
        currentAction = null;
    }
    // ... rest of method continues
```

### 2. PLANNING → EXECUTING when LLM planning completes

**Location:** `tick()` method, inside the async planning completion block (after line 245)

**Add this code:**

```java
// Transition to EXECUTING state
stateMachine.transitionTo(AgentState.EXECUTING, "Planning complete, " + taskQueue.size() + " tasks queued");
```

**Full context (lines 229-249 modified):**

```java
// Check if async planning is complete (non-blocking check!)
if (isPlanning && planningFuture != null && planningFuture.isDone()) {
    try {
        ResponseParser.ParsedResponse response = planningFuture.get();

        if (response != null) {
            currentGoal = response.getPlan();
            foreman.getMemory().setCurrentGoal(currentGoal);

            taskQueue.clear();
            taskQueue.addAll(response.getTasks());

            if (MineWrightConfig.ENABLE_CHAT_RESPONSES.get()) {
                sendToGUI(foreman.getSteveName(), "Okay! " + currentGoal);
            }

            MineWrightMod.LOGGER.info("Foreman '{}' async planning complete: {} tasks queued",
                foreman.getSteveName(), taskQueue.size());

            // NEW: Transition to EXECUTING state
            stateMachine.transitionTo(AgentState.EXECUTING, "Planning complete, " + taskQueue.size() + " tasks queued");
        } else {
            sendToGUI(foreman.getSteveName(), "I couldn't understand that command.");
            MineWrightMod.LOGGER.warn("Foreman '{}' async planning returned null response", foreman.getSteveName());

            // NEW: Transition back to IDLE on failure
            stateMachine.transitionTo(AgentState.IDLE, "Planning failed - null response");
        }

    } catch (java.util.concurrent.CancellationException e) {
        // ... exception handling
```

### 3. PLANNING → FAILED on planning errors

**Location:** `tick()` method, exception handlers (lines 251-261)

**Add transitions to exception handlers:**

```java
} catch (java.util.concurrent.CancellationException e) {
    MineWrightMod.LOGGER.info("Foreman '{}' planning was cancelled", foreman.getSteveName());
    sendToGUI(foreman.getSteveName(), "Planning cancelled.");
    // NEW: Transition back to IDLE on cancellation
    stateMachine.transitionTo(AgentState.IDLE, "Planning cancelled");
} catch (Exception e) {
    MineWrightMod.LOGGER.error("Foreman '{}' failed to get planning result", foreman.getSteveName(), e);
    sendToGUI(foreman.getSteveName(), "Oops, something went wrong while planning!");
    // NEW: Transition to FAILED on error
    stateMachine.transitionTo(AgentState.FAILED, "Planning error: " + e.getMessage());
} finally {
    isPlanning = false;
    planningFuture = null;
    pendingCommand = null;
}
```

### 4. PLANNING → FAILED in `processNaturalLanguageCommand()` exception handlers

**Location:** `processNaturalLanguageCommand()` method, exception handlers (lines 151-161)

**Add transitions:**

```java
} catch (NoClassDefFoundError e) {
    MineWrightMod.LOGGER.error("Failed to initialize AI components", e);
    sendToGUI(foreman.getSteveName(), "Sorry, I'm having trouble with my AI systems!");
    // NEW: Transition to FAILED
    stateMachine.transitionTo(AgentState.FAILED, "AI components not available");
    isPlanning = false;
    planningFuture = null;
} catch (Exception e) {
    MineWrightMod.LOGGER.error("Error starting async planning", e);
    sendToGUI(foreman.getSteveName(), "Oops, something went wrong!");
    // NEW: Transition to FAILED
    stateMachine.transitionTo(AgentState.FAILED, "Planning error: " + e.getMessage());
    isPlanning = false;
    planningFuture = null;
}
```

### 5. EXECUTING → COMPLETED when all tasks done

**Location:** `tick()` method, at the end of the method (after line 320)

**Add this logic:**

```java
// Check if we just completed all tasks and should transition to COMPLETED
if (currentAction == null && taskQueue.isEmpty() && currentGoal != null &&
    stateMachine.getCurrentState() == AgentState.EXECUTING) {
    stateMachine.transitionTo(AgentState.COMPLETED, "All tasks completed");
    currentGoal = null;  // Clear the goal after completion
    // Will auto-transition to IDLE on next tick if no new command
}

// When completely idle (no tasks, no goal), follow nearest player
if (taskQueue.isEmpty() && currentAction == null && currentGoal == null) {
    // If we're in COMPLETED state, transition to IDLE
    if (stateMachine.getCurrentState() == AgentState.COMPLETED) {
        stateMachine.transitionTo(AgentState.IDLE, "Ready for next command");
    }

    if (idleFollowAction == null) {
        idleFollowAction = new IdleFollowAction(foreman);
        idleFollowAction.start();
    } else if (idleFollowAction.isComplete()) {
        // Restart idle following if it stopped
        idleFollowAction = new IdleFollowAction(foreman);
        idleFollowAction.start();
    } else {
        // Continue idle following
        idleFollowAction.tick();
    }
} else if (idleFollowAction != null) {
    idleFollowAction.cancel();
    idleFollowAction = null;
}
```

### 6. EXECUTING → FAILED on action failures

**Location:** `tick()` method, action completion check (lines 264-282)

**Add transition on action failure:**

```java
if (currentAction != null) {
    if (currentAction.isComplete()) {
        ActionResult result = currentAction.getResult();
        MineWrightMod.LOGGER.info("Foreman '{}' - Action completed: {} (Success: {})",
            foreman.getSteveName(), result.getMessage(), result.isSuccess());

        foreman.getMemory().addAction(currentAction.getDescription());

        if (!result.isSuccess()) {
            // Action failed - always notify player via chat
            foreman.sendChatMessage("Action failed: " + result.getMessage());
            if (result.requiresReplanning()) {
                // Also show in GUI if enabled
                if (MineWrightConfig.ENABLE_CHAT_RESPONSES.get()) {
                    sendToGUI(foreman.getSteveName(), "Problem: " + result.getMessage());
                }
                // NEW: Transition to FAILED if action requires replanning
                stateMachine.transitionTo(AgentState.FAILED, "Action failed: " + result.getMessage());
            }
        }

        currentAction = null;
    } else {
        if (ticksSinceLastAction % 100 == 0) {
            MineWrightMod.LOGGER.info("Foreman '{}' - Ticking action: {}",
                foreman.getSteveName(), currentAction.getDescription());
        }
        currentAction.tick();
        return;
    }
}
```

### 7. FAILED → IDLE in `stopCurrentAction()`

**Location:** `stopCurrentAction()` method (line 398-412)

**The reset() call already handles this, but we can make it more explicit:**

```java
public void stopCurrentAction() {
    if (currentAction != null) {
        currentAction.cancel();
        currentAction = null;
    }
    if (idleFollowAction != null) {
        idleFollowAction.cancel();
        idleFollowAction = null;
    }
    taskQueue.clear();
    currentGoal = null;

    // Reset state machine to IDLE (this handles any current state)
    stateMachine.reset();
}
```

## Additional Improvements

### Check state machine before accepting commands

**Location:** `processNaturalLanguageCommand()` method, at the beginning (before line 118)

**Add this check:**

```java
public void processNaturalLanguageCommand(String command) {
    // Check if we can accept new commands
    if (!stateMachine.canAcceptCommands()) {
        MineWrightMod.LOGGER.warn("Foreman '{}' cannot accept commands in state: {}",
            foreman.getSteveName(), stateMachine.getCurrentState());
        sendToGUI(foreman.getSteveName(), "I'm busy right now (state: " +
            stateMachine.getCurrentState().getDisplayName() + ")");
        return;
    }

    MineWrightMod.LOGGER.info("Foreman '{}' processing command (async): {}", foreman.getSteveName(), command);
    // ... rest of method
```

## Summary of Changes

| Method | Line | Transition | Condition |
|--------|------|------------|-----------|
| `processNaturalLanguageCommand()` | ~125 | IDLE → PLANNING | Starting new command |
| `processNaturalLanguageCommand()` | ~152 | → FAILED | NoClassDefFoundError |
| `processNaturalLanguageCommand()` | ~158 | → FAILED | Generic exception |
| `tick()` | ~246 | PLANNING → EXECUTING | Planning successful |
| `tick()` | ~249 | PLANNING → IDLE | Null response |
| `tick()` | ~253 | PLANNING → IDLE | CancellationException |
| `tick()` | ~257 | PLANNING → FAILED | Generic exception |
| `tick()` | ~278 | EXECUTING → FAILED | Action failed with replan |
| `tick()` | ~320 | EXECUTING → COMPLETED | All tasks done |
| `tick()` | ~325 | COMPLETED → IDLE | After completion, ready for next |
| `stopCurrentAction()` | 411 | Any → IDLE | Explicit stop/reset |

## Testing Checklist

After implementing these changes:

- [ ] State transitions occur in correct order during normal flow
- [ ] FAILED state is entered when planning errors occur
- [ ] FAILED state is entered when actions fail
- [ ] COMPLETED state is entered when all tasks finish
- [ ] State returns to IDLE after completion
- [ ] State machine prevents invalid transitions (check logs)
- [ ] `canAcceptCommands()` correctly rejects commands during active states
- [ ] State transition events are published to EventBus (verify with subscribers)

## Related Files

- `C:\Users\casey\steve\src\main\java\com\minewright\execution\AgentStateMachine.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\execution\AgentState.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\event\StateTransitionEvent.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`
