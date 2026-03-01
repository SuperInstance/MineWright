# System Integration Design

**Document Version:** 1.0
**Date:** 2026-03-01
**Author:** Claude Orchestrator
**Status:** Design Document

---

## Executive Summary

This document describes how to integrate the new behavior process system, script execution system, and related components into the existing ForemanEntity and ActionExecutor architecture. The integration maintains the "One Abstraction Away" philosophy while adding process-based arbitration, script-generated automation, and humanized behaviors.

**Key Changes:**
1. ProcessManager replaces direct ActionExecutor tick() calls in ForemanEntity
2. Scripts can be generated from natural language and executed via behavior tree
3. Humanization behaviors (mistakes, idling, session management) integrated via processes
4. Navigation goals composed from high-level objectives
5. Profile system converts high-level tasks to executable actions

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Component Integration](#component-integration)
3. [ForemanEntity Integration](#foremanentity-integration)
4. [ActionExecutor Modifications](#actionexecutor-modifications)
5. [Configuration Updates](#configuration-updates)
6. [Migration Plan](#migration-plan)
7. [Testing Strategy](#testing-strategy)

---

## Architecture Overview

### Current Architecture (Before Integration)

```
ForemanEntity.tick()
    ├── ActionExecutor.tick()
    │   ├── Process natural language commands
    │   ├── Execute queued tasks
    │   └── Handle idle follow behavior
    ├── ProcessMessages()
    ├── CheckTacticalSituation()
    └── DialogueManager.tick()
```

### New Architecture (After Integration)

```
ForemanEntity.tick()
    ├── ProcessManager.tick()
    │   ├── SurvivalProcess (Priority: 100)
    │   ├── TaskExecutionProcess (Priority: 50)
    │   │   └── ActionExecutor.tick()
    │   ├── ScriptExecutionProcess (Priority: 45)
    │   │   └── ScriptExecution.start()
    │   ├── FollowProcess (Priority: 25)
    │   └── IdleProcess (Priority: 10)
    │       └── IdleBehaviorController
    ├── ProcessMessages()
    ├── CheckTacticalSituation()
    └── DialogueManager.tick()
```

### Key Design Principles

1. **Single Entry Point:** ProcessManager.tick() is the only behavior entry point in ForemanEntity
2. **Priority-Based Arbitration:** Processes compete based on canRun() and priority
3. **Clean Transitions:** onActivate/onDeactivate hooks for state management
4. **Backward Compatibility:** Existing ActionExecutor API preserved
5. **Graceful Degradation:** Each system wrapped in try-catch for resilience

---

## Component Integration

### 1. Process Arbitration System

#### Components to Integrate:
- `ProcessManager` - Central coordinator
- `BehaviorProcess` interface - Process contract
- `SurvivalProcess` - Emergency behaviors (priority 100)
- `TaskExecutionProcess` - Normal work (priority 50)
- `IdleProcess` - Fallback behavior (priority 10)
- `FollowProcess` - Player following (priority 25)

#### Integration Points:

**ForemanEntity Constructor:**
```java
public ForemanEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
    super(entityType, level);

    // Existing initialization
    this.entityName = "Foreman";
    this.memory = new ForemanMemory(this);
    this.companionMemory = new CompanionMemory();
    this.actionExecutor = new ActionExecutor(this);
    this.dialogueManager = new ProactiveDialogueManager(this);
    this.tacticalService = TacticalDecisionService.getInstance();

    // NEW: Initialize ProcessManager
    this.processManager = new ProcessManager(this);
    initializeProcesses();

    // ... rest of initialization
}

private void initializeProcesses() {
    // Register processes in priority order (highest first)
    processManager.registerProcess(new SurvivalProcess(this));
    processManager.registerProcess(new TaskExecutionProcess(this));
    processManager.registerProcess(new FollowProcess(this));
    processManager.registerProcess(new IdleProcess(this));
}
```

**ForemanEntity.tick() Modifications:**
```java
@Override
public void tick() {
    try {
        super.tick();
    } catch (Exception e) {
        LOGGER.error("[{}] Critical error in parent entity tick, continuing anyway",
            entityName, e);
    }

    if (!this.level().isClientSide) {
        long gameTime = this.level().getGameTime();

        // Existing: Orchestrator registration
        if (!registeredWithOrchestrator.get() && orchestrator != null) {
            try {
                registerWithOrchestrator();
            } catch (Exception e) {
                LOGGER.error("[{}] Failed to register with orchestrator", entityName, e);
                registeredWithOrchestrator.set(true);
            }
        }

        // Existing: Hive Mind checks
        if (tacticalService.isEnabled() &&
            gameTime - lastTacticalCheck >= tacticalService.getCheckInterval()) {
            lastTacticalCheck = gameTime;
            try {
                checkTacticalSituation();
            } catch (Exception e) {
                LOGGER.warn("[{}] Tactical check failed (continuing normally)", entityName, e);
            }
        }

        // Existing: State sync
        if (tacticalService.isEnabled() &&
            gameTime - lastStateSync >= tacticalService.getSyncInterval()) {
            lastStateSync = gameTime;
            try {
                tacticalService.syncState(this);
            } catch (Exception e) {
                LOGGER.warn("[{}] State sync failed (will retry later)", entityName, e);
            }
        }

        // Existing: Message processing
        try {
            processMessages();
        } catch (Exception e) {
            LOGGER.error("[{}] Error processing messages (continuing anyway)", entityName, e);
            messageQueue.clear();
        }

        // NEW: Process-based behavior arbitration
        try {
            processManager.tick();
            errorRecoveryTicks = 0;
        } catch (Exception e) {
            LOGGER.error("[{}] Critical error in process manager", entityName, e);
            errorRecoveryTicks++;

            if (errorRecoveryTicks == 1) {
                try {
                    sendChatMessage("Hit a snag there boss. Working on it...");
                } catch (Exception ignored) {
                }
            }

            if (errorRecoveryTicks >= 3) {
                LOGGER.warn("[{}] Too many errors, resetting process manager", entityName);
                try {
                    processManager.forceDeactivate();
                    processManager = new ProcessManager(this);
                    initializeProcesses();
                    errorRecoveryTicks = 0;
                    sendChatMessage("Alright, I'm back on track now.");
                } catch (Exception resetError) {
                    LOGGER.error("[{}] Failed to reset process manager", entityName, resetError);
                }
            }
        }

        // Existing: Dialogue manager
        if (dialogueManager != null) {
            try {
                dialogueManager.tick();
            } catch (Exception e) {
                LOGGER.warn("[{}] Dialogue manager error (continuing without dialogue)", entityName, e);
            }
        }

        // Existing: Progress reporting
        try {
            reportTaskProgress();
        } catch (Exception e) {
            LOGGER.warn("[{}] Failed to report progress", entityName, e);
        }
    }
}
```

#### New ForemanEntity Fields:
```java
/**
 * Process manager for behavior arbitration.
 * Coordinates competing behaviors via priority-based selection.
 */
private ProcessManager processManager;
```

#### New ForemanEntity Methods:
```java
/**
 * Initializes all behavior processes.
 * Called during entity construction and after process manager resets.
 */
private void initializeProcesses() {
    processManager.registerProcess(new SurvivalProcess(this));
    processManager.registerProcess(new TaskExecutionProcess(this));
    processManager.registerProcess(new FollowProcess(this));
    processManager.registerProcess(new IdleProcess(this));
}

/**
 * Gets the process manager for external access.
 * Useful for debugging and monitoring.
 */
public ProcessManager getProcessManager() {
    return processManager;
}

/**
 * Gets the currently active process name.
 * Useful for UI display and debugging.
 */
public String getActiveProcessName() {
    return processManager.getActiveProcessName();
}
```

---

### 2. Script Execution System

#### Components to Integrate:
- `ScriptGenerator` - LLM-powered script generation
- `ScriptExecution` - Behavior tree execution engine
- `ScriptRegistry` - Script storage and retrieval
- `ScriptCache` - Performance optimization

#### Integration Points:

**New ScriptExecutionProcess:**

```java
package com.minewright.behavior.processes;

import com.minewright.behavior.BehaviorProcess;
import com.minewright.entity.ForemanEntity;
import com.minewright.script.Script;
import com.minewright.script.ScriptExecution;
import com.minewright.script.ScriptGenerator;
import com.minewright.script.ScriptGenerationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Medium-high priority process for executing generated automation scripts.
 *
 * <p>Scripts are generated from natural language commands using LLMs,
 * then executed as behavior trees. This sits between survival (100)
 * and normal task execution (50) since scripts represent structured
 * automation plans.</p>
 *
 * <p><b>Priority: 45</b> - Higher than TaskExecution (50) since scripts
 * are more structured and intentional than queued tasks.</p>
 */
public class ScriptExecutionProcess implements BehaviorProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptExecutionProcess.class);

    private static final int PRIORITY = 45;

    private final ForemanEntity foreman;
    private final ScriptGenerator scriptGenerator;
    private final ScriptRegistry scriptRegistry;

    private boolean active = false;
    private int ticksActive = 0;

    private Script currentScript;
    private ScriptExecution currentExecution;
    private CompletableFuture<ScriptExecution.ScriptExecutionResult> executionFuture;

    public ScriptExecutionProcess(ForemanEntity foreman) {
        this.foreman = foreman;
        this.scriptGenerator = ScriptGenerator.getInstance();
        this.scriptRegistry = ScriptRegistry.getInstance();
    }

    @Override
    public String getName() {
        return "ScriptExecution";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean canRun() {
        // Can run if there's an active script execution
        return currentExecution != null && !currentExecution.isComplete();
    }

    @Override
    public void tick() {
        ticksActive++;

        // Check if execution is complete
        if (currentExecution != null && currentExecution.isComplete()) {
            handleExecutionComplete();
            return;
        }

        // Log progress every 100 ticks
        if (ticksActive % 100 == 0) {
            LOGGER.info("[{}] Script execution progress (ticks: {}, script: {})",
                foreman.getEntityName(), ticksActive,
                currentScript != null ? currentScript.getName() : "none");
        }
    }

    @Override
    public void onActivate() {
        active = true;
        ticksActive = 0;

        LOGGER.info("[{}] Script execution activated",
            foreman.getEntityName());
    }

    @Override
    public void onDeactivate() {
        active = false;

        LOGGER.info("[{}] Script execution deactivated (was active for {} ticks)",
            foreman.getEntityName(), ticksActive);

        // Cancel ongoing execution if needed
        if (currentExecution != null && !currentExecution.isComplete()) {
            // TODO: Implement cancellation
        }
    }

    /**
     * Starts execution of a script.
     *
     * @param script The script to execute
     * @return CompletableFuture that completes when execution finishes
     */
    public CompletableFuture<ScriptExecution.ScriptExecutionResult> executeScript(Script script) {
        if (script == null) {
            LOGGER.warn("[{}] Cannot execute null script", foreman.getEntityName());
            return CompletableFuture.completedFuture(null);
        }

        this.currentScript = script;
        this.currentExecution = new ScriptExecution(script, foreman, foreman.getActionExecutor());

        LOGGER.info("[{}] Starting script execution: {}",
            foreman.getEntityName(), script.getName());

        executionFuture = currentExecution.start();

        return executionFuture;
    }

    /**
     * Generates and executes a script from a natural language command.
     *
     * @param command The natural language command
     * @return CompletableFuture that completes with the generated script
     */
    public CompletableFuture<Script> generateAndExecute(String command) {
        ScriptGenerationContext context = new ScriptGenerationContext.Builder()
            .withAgentState(foreman)
            .build();

        return scriptGenerator.generateAsync(command, context)
            .thenApply(script -> {
                // Register the script
                scriptRegistry.register(script);

                // Start execution
                executeScript(script);

                return script;
            });
    }

    /**
     * Handles script execution completion.
     */
    private void handleExecutionComplete() {
        if (executionFuture == null) {
            return;
        }

        try {
            ScriptExecution.ScriptExecutionResult result = executionFuture.get();

            if (result.isSuccess()) {
                LOGGER.info("[{}] Script execution completed successfully: {}",
                    foreman.getEntityName(), currentScript.getName());

                // Notify dialogue manager
                foreman.notifyTaskCompleted(currentScript.getName());
            } else {
                LOGGER.warn("[{}] Script execution failed: {} - {}",
                    foreman.getEntityName(), currentScript.getName(),
                    result.failureReason());

                // Notify dialogue manager
                foreman.notifyTaskFailed(currentScript.getName(), result.failureReason());
            }

        } catch (Exception e) {
            LOGGER.error("[{}] Error handling script execution completion",
                foreman.getEntityName(), e);
        } finally {
            currentScript = null;
            currentExecution = null;
            executionFuture = null;
        }
    }

    public Script getCurrentScript() {
        return currentScript;
    }

    public boolean isExecuting() {
        return currentExecution != null && !currentExecution.isComplete();
    }
}
```

**ActionExecutor Modifications for Script Support:**

```java
/**
 * NEW: Executes a single task directly (for script execution).
 *
 * <p>This method is package-private to allow ScriptExecution to call it.
 * Scripts execute individual actions as part of their behavior tree.</p>
 *
 * @param task The task to execute
 */
void executeTask(Task task) {
    // Existing implementation (extracted from tick() method)
    LOGGER.info("[{}] Executing task: {} (action type: {})",
        foreman.getEntityName(), task, task.getAction());

    try {
        currentAction = createAction(task);

        if (currentAction == null) {
            handleTaskCreationError(task);
            return;
        }

        LOGGER.info("[{}] Created action: {} - starting now...",
            foreman.getEntityName(), currentAction.getClass().getSimpleName());
        currentAction.start();

        LOGGER.debug("[{}] Action started! Is complete: {}",
            foreman.getEntityName(), currentAction.isComplete());

    } catch (Exception e) {
        LOGGER.error("[{}] Failed to execute task: {}",
            foreman.getEntityName(), task, e);
        handleExecutionError(task, e);
    }
}
```

---

### 3. Humanization System

#### Components to Integrate:
- `HumanizationUtils` - Utility methods for human-like behavior
- `MistakeSimulator` - Simulates errors and delays
- `IdleBehaviorController` - Manages idle animations and actions
- `SessionManager` - Tracks agent session state

#### Integration Points:

**IdleProcess Enhancements:**

```java
package com.minewright.behavior.processes;

import com.minewright.behavior.BehaviorProcess;
import com.minewright.entity.ForemanEntity;
import com.minewright.util.humanization.HumanizationUtils;
import com.minewright.util.humanization.IdleBehaviorController;
import com.minewright.util.humanization.SessionManager;
import com.minewright.util.humanization.SessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Low-priority idle process with humanization behaviors.
 *
 * <p>Enhanced with mistake simulation, session-aware behaviors,
 * and characterful idle actions that make the agent feel more human.</p>
 */
public class IdleProcess implements BehaviorProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdleProcess.class);

    private static final int PRIORITY = 10;
    private static final Random RANDOM = new Random();

    private final ForemanEntity foreman;
    private final IdleBehaviorController idleController;
    private final SessionManager sessionManager;

    private boolean active = false;
    private int ticksActive = 0;
    private int ticksSinceLastAction = 0;

    private IdleBehavior currentBehavior = IdleBehavior.NONE;

    public IdleProcess(ForemanEntity foreman) {
        this.foreman = foreman;
        this.idleController = new IdleBehaviorController(foreman);
        this.sessionManager = SessionManager.getInstance();
    }

    @Override
    public String getName() {
        return "Idle";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean canRun() {
        // Idle can always run - it's the fallback behavior
        return true;
    }

    @Override
    public void tick() {
        ticksActive++;
        ticksSinceLastAction++;

        // Update session state
        SessionState sessionState = sessionManager.getSessionState(foreman.getUUID());

        // Perform idle actions at intervals
        if (ticksSinceLastAction >= getActionInterval(sessionState)) {
            performIdleAction(sessionState);
            ticksSinceLastAction = 0;
        }

        // Continue current idle behavior
        continueIdleBehavior();

        // Update idle controller
        idleController.tick();
    }

    @Override
    public void onActivate() {
        active = true;
        ticksActive = 0;
        ticksSinceLastAction = 0;

        LOGGER.debug("[{}] Idle behavior activated", foreman.getEntityName());

        // Session-aware activation message
        SessionState sessionState = sessionManager.getSessionState(foreman.getUUID());
        if (sessionState != null && sessionState.shouldGreetOnIdle()) {
            sendSessionAwareChat();
        }
    }

    @Override
    public void onDeactivate() {
        active = false;
        currentBehavior = IdleBehavior.NONE;

        LOGGER.debug("[{}] Idle behavior deactivated (was active for {} ticks)",
            foreman.getEntityName(), ticksActive);
    }

    /**
     * Gets the action interval based on session state.
     * Fresh agents are more active, tired agents are less active.
     */
    private int getActionInterval(SessionState sessionState) {
        if (sessionState == null) {
            return 60; // Default: 3 seconds
        }

        // Fatigue affects interval (tired = longer intervals)
        double fatigueMultiplier = 1.0 + (sessionState.getFatigueLevel() * 0.5);
        return (int) (60 * fatigueMultiplier);
    }

    /**
     * Performs a random idle action with humanization.
     */
    private void performIdleAction(SessionState sessionState) {
        // Apply mistake simulation (rarely "misunderstand" and do wrong action)
        if (HumanizationUtils.shouldSimulateMistake(foreman, 0.01)) {
            performMistakeAction();
            return;
        }

        // Choose behavior based on session state
        IdleBehavior behavior = selectBehavior(sessionState);
        currentBehavior = behavior;

        switch (behavior) {
            case LOOK_AROUND -> lookAround();
            case WANDER -> wander();
            case STRETCH -> stretch();
            case YAWN -> yawn();
            case CHAT -> sendIdleChat();
            case FOLLOW -> followPlayer();
            case REST -> rest();
        }

        LOGGER.debug("[{}] Idle action: {}",
            foreman.getEntityName(), currentBehavior);
    }

    /**
     * Selects an idle behavior based on session state.
     */
    private IdleBehavior selectBehavior(SessionState sessionState) {
        if (sessionState == null) {
            return selectRandomBehavior();
        }

        // Tired agents prefer resting
        if (sessionState.getFatigueLevel() > 0.7 && RANDOM.nextDouble() < 0.5) {
            return IdleBehavior.REST;
        }

        // Bored agents prefer wandering
        if (sessionState.getBoredomLevel() > 0.6 && RANDOM.nextDouble() < 0.4) {
            return IdleBehavior.WANDER;
        }

        return selectRandomBehavior();
    }

    /**
     * Selects a random idle behavior.
     */
    private IdleBehavior selectRandomBehavior() {
        int roll = RANDOM.nextInt(100);

        if (roll < 35) return IdleBehavior.LOOK_AROUND;
        else if (roll < 60) return IdleBehavior.WANDER;
        else if (roll < 70) return IdleBehavior.STRETCH;
        else if (roll < 80) return IdleBehavior.YAWN;
        else if (roll < 85) return IdleBehavior.CHAT;
        else if (roll < 90) return IdleBehavior.FOLLOW;
        else return IdleBehavior.REST;
    }

    /**
     * Performs a "mistake" action (humanization).
     */
    private void performMistakeAction() {
        LOGGER.debug("[{}] Simulating idle mistake", foreman.getEntityName());

        // Pick a random "wrong" action
        IdleBehavior[] wrongActions = {
            IdleBehavior.STRETCH,
            IdleBehavior.YAWN,
            IdleBehavior.LOOK_AROUND
        };

        IdleBehavior wrongAction = wrongActions[RANDOM.nextInt(wrongActions.length)];
        currentBehavior = wrongAction;

        // Execute wrong action briefly, then "realize" mistake
        switch (wrongAction) {
            case STRETCH -> stretch();
            case YAWN -> yawn();
            case LOOK_AROUND -> lookAround();
        }

        // Send embarrassed chat
        String[] embarrassedResponses = {
            "Oops, got distracted there!",
            "Wait, what was I doing?",
            "Sorry, lost focus for a moment.",
            "Ahem. Anyway..."
        };

        foreman.sendChatMessage(embarrassedResponses[RANDOM.nextInt(embarrassedResponses.length)]);
    }

    private void continueIdleBehavior() {
        if (currentBehavior == IdleBehavior.FOLLOW) {
            followPlayer();
        }
    }

    // === Idle Behavior Implementations ===

    private void lookAround() {
        idleController.lookAround();
    }

    private void wander() {
        idleController.wander();
    }

    private void stretch() {
        idleController.stretch();
    }

    private void yawn() {
        idleController.yawn();
    }

    private void rest() {
        idleController.rest();
    }

    private void sendIdleChat() {
        String message = idleController.getIdleChatMessage();
        foreman.sendChatMessage(message);
    }

    private void followPlayer() {
        idleController.followPlayer();
    }

    private void sendSessionAwareChat() {
        SessionState sessionState = sessionManager.getSessionState(foreman.getUUID());
        if (sessionState != null) {
            String message = sessionState.getSessionGreeting();
            if (message != null) {
                foreman.sendChatMessage(message);
            }
        }
    }

    public enum IdleBehavior {
        NONE("Doing nothing"),
        LOOK_AROUND("Looking around"),
        WANDER("Wandering"),
        CHAT("Chatting"),
        STRETCH("Stretching"),
        YAWN("Yawning"),
        FOLLOW("Following player"),
        REST("Resting");

        private final String description;
        IdleBehavior(String description) {
            this.description = description;
        }
        String getDescription() {
            return description;
        }
    }
}
```

**BaseAction Modifications for Humanization:**

```java
package com.minewright.action.actions;

import com.minewright.action.BaseAction;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.util.humanization.HumanizationUtils;
import com.minewright.util.humanization.MistakeSimulator;
import com.minewright.util.humanization.MistakeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base action with humanization support.
 *
 * <p>All actions inherit mistake simulation, reaction delays,
 * and other human-like behaviors from this base class.</p>
 */
public abstract class HumanizedBaseAction extends BaseAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(HumanizedBaseAction.class);

    protected final MistakeSimulator mistakeSimulator;

    private int reactionDelayTicks = 0;
    private boolean hasReacted = false;

    public HumanizedBaseAction(ForemanEntity steve, Task task) {
        super(steve, task);
        this.mistakeSimulator = new MistakeSimulator(steve);
    }

    @Override
    protected void onTick() {
        // Apply reaction delay on first tick
        if (!hasReacted) {
            reactionDelayTicks = HumanizationUtils.getReactionDelayTicks(steve);
            hasReacted = true;
        }

        // Wait for reaction delay
        if (reactionDelayTicks > 0) {
            reactionDelayTicks--;
            return;
        }

        // Check for mistake simulation
        if (mistakeSimulator.shouldSimulateMistake()) {
            handleMistake();
            return;
        }

        // Normal action execution
        executeAction();
    }

    /**
     * Executes the main action logic.
     * Subclasses implement this method.
     */
    protected abstract void executeAction();

    /**
     * Handles a simulated mistake.
     */
    private void handleMistake() {
        MistakeType mistake = mistakeSimulator.generateMistake();

        LOGGER.debug("[{}] Simulating mistake: {}",
            steve.getEntityName(), mistake);

        switch (mistake) {
            case FUMBLE -> {
                // Drop item, misclick, etc.
                steve.sendChatMessage("Oops! Fumbled that a bit.");
                // Add delay as penalty
                reactionDelayTicks += 20;
            }
            case DELAY -> {
                // Hesitate, pause, think
                LOGGER.debug("[{}] Action delayed due to hesitation", steve.getEntityName());
                reactionDelayTicks += 40;
            }
            case CONFUSION -> {
                // Wrong target, wrong block, etc.
                steve.sendChatMessage("Wait, which one was I supposed to do?");
                reactionDelayTicks += 30;
            }
            case MISAIM -> {
                // Slightly off target
                LOGGER.debug("[{}] Slight misaim", steve.getEntityName());
                // Adjust target slightly (implementation-specific)
            }
        }
    }
}
```

---

### 4. Goal Composition System

#### Components to Integrate:
- `NavigationGoal` - High-level navigation objectives
- `Goals` factory - Creates composite goals
- Goal composition with sub-goals

#### Integration Points:

**New NavigationAction:**

```java
package com.minewright.action.actions;

import com.minewright.action.Action;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.pathfinding.goals.NavigationGoal;
import com.minewright.pathfinding.goals.Goals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for navigating to a goal location.
 *
 * <p>Supports simple and composite goals with goal composition.</p>
 */
public class NavigationAction extends Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(NavigationAction.class);

    private final NavigationGoal goal;
    private final String goalDescription;

    private boolean pathfindingStarted = false;

    public NavigationAction(ForemanEntity foreman, Task task, NavigationGoal goal) {
        super(foreman, task);
        this.goal = goal;
        this.goalDescription = goal != null ? goal.toString() : "unknown";
    }

    @Override
    protected void onTick() {
        if (isComplete()) {
            return;
        }

        // Start pathfinding on first tick
        if (!pathfindingStarted) {
            startPathfinding();
            pathfindingStarted = true;
            return;
        }

        // Check if goal is reached
        if (goal.isReached(steve)) {
            markComplete();
            LOGGER.info("[{}] Navigation goal reached: {}",
                steve.getEntityName(), goalDescription);
            return;
        }

        // Pathfinding continues automatically via Minecraft's navigation system
        // Log progress every 100 ticks
        if (steve.level().getGameTime() % 100 == 0) {
            LOGGER.debug("[{}] Navigating to: {} (distance: {})",
                steve.getEntityName(), goalDescription,
                goal.getDistanceFrom(steve));
        }
    }

    /**
     * Starts pathfinding to the goal.
     */
    private void startPathfinding() {
        LOGGER.info("[{}] Starting navigation to: {}",
            steve.getEntityName(), goalDescription);

        // Set the goal in Minecraft's navigation system
        if (steve.getNavigation() != null) {
            goal.applyToEntity(steve);
        } else {
            LOGGER.warn("[{}] Navigation system not available", steve.getEntityName());
            markFailed();
        }
    }

    @Override
    public String getDescription() {
        return "Navigate to " + goalDescription;
    }
}
```

**ActionFactory Integration:**

```java
/**
 * Creates a NavigationAction from a task.
 */
private BaseAction createNavigationAction(Task task) {
    String goalType = task.getParameter("goal_type", "position");

    NavigationGoal goal = switch (goalType) {
        case "position" -> {
            double x = task.getDoubleParameter("x", steve.getX());
            double y = task.getDoubleParameter("y", steve.getY());
            double z = task.getDoubleParameter("z", steve.getZ());
            yield Goals.position(x, y, z);
        }
        case "block" -> {
            String blockType = task.getParameter("block", "stone");
            int searchRadius = task.getIntParameter("radius", 64);
            yield Goals.nearestBlock(blockType, searchRadius);
        }
        case "entity" -> {
            String entityType = task.getParameter("entity_type", "player");
            int searchRadius = task.getIntParameter("radius", 64);
            yield Goals.nearestEntity(entityType, searchRadius);
        }
        case "composite" -> {
            // Create composite goal from sub-goals
            NavigationGoal[] subGoals = parseSubGoals(task);
            yield Goals.composite(subGoals);
        }
        default -> Goals.position(steve.getX(), steve.getY(), steve.getZ());
    };

    return new NavigationAction(steve, task, goal);
}

/**
 * Parses sub-goals for composite goal.
 */
private NavigationGoal[] parseSubGoals(Task task) {
    // Task parameter format: "goal1,goal2,goal3" or similar
    String goalsString = task.getParameter("sub_goals", "");
    String[] goalParts = goalsString.split(",");

    NavigationGoal[] goals = new NavigationGoal[goalParts.length];
    for (int i = 0; i < goalParts.length; i++) {
        // Parse each sub-goal (simplified)
        goals[i] = Goals.fromString(goalParts[i].trim());
    }

    return goals;
}
```

---

### 5. Profile System

#### Components to Integrate:
- `ProfileExecutor` - Executes task profiles
- `TaskProfile` - Predefined task configurations

#### Integration Points:

**New ProfileExecutionProcess:**

```java
package com.minewright.behavior.processes;

import com.minewright.behavior.BehaviorProcess;
import com.minewright.entity.ForemanEntity;
import com.minewright.script.ProfileExecutor;
import com.minewright.script.TaskProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process for executing predefined task profiles.
 *
 * <p>Profiles are templates for common tasks (mining patterns,
 * building templates, farming routines) that can be quickly
 * instantiated without full LLM planning.</p>
 *
 * <p><b>Priority: 40</b> - Between script execution (45) and
 * normal task execution (50) since profiles are pre-validated
 * and optimized.</p>
 */
public class ProfileExecutionProcess implements BehaviorProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileExecutionProcess.class);

    private static final int PRIORITY = 40;

    private final ForemanEntity foreman;
    private final ProfileExecutor profileExecutor;

    private boolean active = false;
    private TaskProfile currentProfile;
    private boolean isExecuting = false;

    public ProfileExecutionProcess(ForemanEntity foreman) {
        this.foreman = foreman;
        this.profileExecutor = new ProfileExecutor(foreman);
    }

    @Override
    public String getName() {
        return "ProfileExecution";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean canRun() {
        return isExecuting && currentProfile != null;
    }

    @Override
    public void tick() {
        if (!isExecuting || currentProfile == null) {
            return;
        }

        // Profile execution is delegated to ActionExecutor
        // This process just tracks state
        if (foreman.getActionExecutor().isExecuting()) {
            LOGGER.debug("[{}] Executing profile: {}",
                foreman.getEntityName(), currentProfile.getName());
        } else {
            // Profile execution complete
            isExecuting = false;
            LOGGER.info("[{}] Profile execution completed: {}",
                foreman.getEntityName(), currentProfile.getName());
        }
    }

    @Override
    public void onActivate() {
        active = true;

        LOGGER.info("[{}] Profile execution activated: {}",
            foreman.getEntityName(), currentProfile.getName());
    }

    @Override
    public void onDeactivate() {
        active = false;

        LOGGER.info("[{}] Profile execution deactivated",
            foreman.getEntityName());
    }

    /**
     * Starts execution of a task profile.
     *
     * @param profile The profile to execute
     */
    public void executeProfile(TaskProfile profile) {
        if (profile == null) {
            LOGGER.warn("[{}] Cannot execute null profile", foreman.getEntityName());
            return;
        }

        this.currentProfile = profile;
        this.isExecuting = true;

        LOGGER.info("[{}] Starting profile execution: {}",
            foreman.getEntityName(), profile.getName());

        // Convert profile to tasks and queue them
        profileExecutor.executeProfile(profile, foreman.getActionExecutor());
    }

    public TaskProfile getCurrentProfile() {
        return currentProfile;
    }

    public boolean isExecutingProfile() {
        return isExecuting;
    }
}
```

---

## Configuration Updates

### New Configuration Section: `config/minewright-common.toml`

```toml
# ========== Process Arbitration System ==========
[processes]
# Enable process-based behavior arbitration
enabled = true

# Process priority overrides (optional)
# survival_priority = 100
# task_execution_priority = 50
# script_execution_priority = 45
# profile_execution_priority = 40
# follow_priority = 25
# idle_priority = 10

# ========== Humanization ==========
[humanization]
# Enable human-like behaviors (mistakes, delays, idling)
enabled = true

# Mistake simulation (0.0 to 1.0)
mistake_probability = 0.02

# Reaction delay range (ticks)
min_reaction_delay = 5
max_reaction_delay = 20

# Fatigue system (affects mistake rate and delays)
fatigue_enabled = true
fatigue_rate_per_tick = 0.0001
fatigue_recovery_per_tick = 0.0005

# ========== Script Execution ==========
[scripts]
# Enable LLM-generated script execution
enabled = true

# Script cache settings
cache_enabled = true
max_cache_size = 100
cache_ttl_hours = 24

# Script generation settings
max_tokens = 2000
temperature = 0.7

# ========== Profile Execution ==========
[profiles]
# Enable predefined task profiles
enabled = true

# Profile locations
profile_directory = "config/minewright/profiles/"

# ========== Navigation Goals ==========
[navigation]
# Goal composition settings
max_composite_goals = 10

# Default search radius for nearest-block/nearest-entity goals
default_search_radius = 64
```

---

## Migration Plan

### Phase 1: Process Arbitration (Week 1)

**Goal:** Replace direct ActionExecutor.tick() with ProcessManager

**Steps:**
1. Add `ProcessManager` field to ForemanEntity
2. Create `initializeProcesses()` method
3. Modify `ForemanEntity.tick()` to call `processManager.tick()`
4. Move `actionExecutor.tick()` call into `TaskExecutionProcess`
5. Test backward compatibility (existing commands should work)

**Success Criteria:**
- All existing functionality preserved
- Process transitions logged correctly
- No regression in task execution

### Phase 2: Script System Integration (Week 2)

**Goal:** Enable script generation and execution

**Steps:**
1. Add `ScriptExecutionProcess` to process manager
2. Implement `executeScript()` method
3. Add `generateAndExecute()` async method
4. Create script DSL examples and templates
5. Test script generation from commands

**Success Criteria:**
- Scripts generated from natural language
- Scripts execute correctly via behavior tree
- Script cache working (repeated commands faster)

### Phase 3: Humanization Behaviors (Week 3)

**Goal:** Add human-like mistakes, delays, and idle behaviors

**Steps:**
1. Implement `HumanizationUtils` methods
2. Create `MistakeSimulator` class
3. Enhance `IdleProcess` with session awareness
4. Modify `BaseAction` to support humanization
5. Add configuration options for humanization

**Success Criteria:**
- Mistakes simulated at configured rate
- Reaction delays applied
- Idle behaviors session-aware
- No performance degradation

### Phase 4: Goal Composition (Week 4)

**Goal:** Support composite navigation goals

**Steps:**
1. Implement `NavigationAction` class
2. Add `Goals` factory methods
3. Support composite goal parsing
4. Test goal composition scenarios
5. Document goal DSL

**Success Criteria:**
- Simple goals (position, block, entity) working
- Composite goals (sequences) working
- Goal priority and arbitration correct

### Phase 5: Profile System (Week 5)

**Goal:** Enable predefined task profiles

**Steps:**
1. Implement `ProfileExecutionProcess`
2. Create profile templates (mining, building, farming)
3. Add profile directory and loading
4. Test profile execution
5. Document profile creation

**Success Criteria:**
- Profiles load from directory
- Profiles execute correctly
- Profile parameters substituted
- Profile errors handled gracefully

---

## Testing Strategy

### Unit Tests

**ProcessManager Tests:**
- Test process registration and priority ordering
- Test process transitions (activate/deactivate)
- Test canRun() arbitration
- Test error handling and recovery

**ScriptExecution Tests:**
- Test script generation from commands
- Test script execution via behavior tree
- Test script cache hit/miss
- Test script error handling

**Humanization Tests:**
- Test mistake simulation probability
- Test reaction delay range
- Test session state tracking
- Test fatigue accumulation/recovery

**Goal Composition Tests:**
- Test simple goal creation
- Test composite goal creation
- Test goal reachability checks
- Test goal distance calculations

**Profile Execution Tests:**
- Test profile loading from directory
- Test profile parameter substitution
- Test profile execution
- Test profile error handling

### Integration Tests

**Process Arbitration Integration:**
- Test SurvivalProcess preempts TaskExecutionProcess
- Test TaskExecutionProcess preempts IdleProcess
- Test process transitions on state changes

**Script System Integration:**
- Test script generation from natural language
- Test script execution via process manager
- Test script cache reduces LLM calls

**Humanization Integration:**
- Test mistakes simulated during action execution
- Test idle behaviors session-aware
- Test fatigue affects mistake rate

**Full System Integration:**
- Test end-to-end command execution
- Test multi-process scenarios
- Test error recovery and degradation
- Test performance under load

### Performance Tests

**Tick Budget Enforcement:**
- Measure tick time for each process
- Verify no process exceeds 5ms budget
- Test with multiple concurrent processes

**Script Cache Performance:**
- Measure cache hit rate
- Measure LLM call reduction
- Test cache memory usage

**Humanization Overhead:**
- Measure mistake simulation overhead
- Measure session tracking overhead
- Verify no significant performance impact

---

## Rollback Plan

If integration issues arise, rollback steps:

1. **Disable Process Arbitration:**
   - Set `processes.enabled = false` in config
   - Revert `ForemanEntity.tick()` to call `actionExecutor.tick()` directly
   - ProcessManager still initialized but not used

2. **Disable Script Execution:**
   - Set `scripts.enabled = false` in config
   - Remove `ScriptExecutionProcess` from process manager
   - Commands fall back to direct task execution

3. **Disable Humanization:**
   - Set `humanization.enabled = false` in config
   - Humanization checks skip all mutations
   - Actions execute normally without delays

4. **Full Rollback:**
   - Revert `ForemanEntity` to pre-integration version
   - Keep new classes but don't call them
   - New systems can be re-enabled incrementally

---

## Summary

This integration design maintains backward compatibility while adding significant new capabilities:

1. **Process Arbitration:** Clean priority-based behavior selection
2. **Script System:** LLM-generated automation with caching
3. **Humanization:** Mistakes, delays, and session-aware behaviors
4. **Goal Composition:** Flexible navigation with composite goals
5. **Profile System:** Predefined task templates

The phased migration allows incremental integration with testing at each stage. Configuration options enable feature toggling for graceful degradation and rollback.

**Estimated Timeline:** 5 weeks for full integration
**Risk Level:** Medium (well-isolated components, configuration toggles)
**Backward Compatibility:** Preserved (all existing APIs unchanged)

---

**Next Steps:**
1. Review and approve this design document
2. Begin Phase 1 implementation (Process Arbitration)
3. Create unit tests for ProcessManager
4. Update build.gradle with new dependencies if needed
5. Set up CI/CD for automated testing
