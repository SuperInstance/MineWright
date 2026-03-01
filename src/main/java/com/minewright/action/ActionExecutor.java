package com.minewright.action;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import com.minewright.action.actions.*;
import com.minewright.di.ServiceContainer;
import com.minewright.di.SimpleServiceContainer;
import com.minewright.event.EventBus;
import com.minewright.event.SimpleEventBus;
import com.minewright.execution.*;
import com.minewright.llm.ResponseParser;
import com.minewright.llm.TaskPlanner;
import com.minewright.config.MineWrightConfig;
import com.minewright.entity.ForemanEntity;
import com.minewright.plugin.ActionRegistry;
import com.minewright.plugin.PluginManager;
import com.minewright.voice.VoiceManager;
import com.minewright.util.TickProfiler;
import com.minewright.skill.ExecutionTracker;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executes actions for a MineWright crew member using a tick-based execution model.
 *
 * <p><b>Purpose:</b></p>
 * <p>The ActionExecutor is responsible for queuing tasks and executing them
 * one action at a time, with each action progressing tick-by-tick (20 times per second).
 * This design prevents the Minecraft server from freezing during long-running operations
 * like LLM planning calls.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li><b>Tick-Based Execution:</b> Actions implement {@code tick()} and are called
 *       once per game tick, allowing progress without blocking</li>
 *   <li><b>Async LLM Planning:</b> Natural language commands are processed asynchronously
 *       using {@link CompletableFuture}, returning immediately while planning happens
 *       on a separate thread pool</li>
 *   <li><b>Plugin Architecture:</b> Actions are created via {@link ActionRegistry}
 *       using factory pattern, allowing dynamic extension</li>
 *   <li><b>State Machine:</b> Explicit tracking of agent states (IDLE, PLANNING,
 *       EXECUTING, WAITING, ERROR) via {@link AgentStateMachine}</li>
 *   <li><b>Interceptor Chain:</b> Cross-cutting concerns (logging, metrics, events)
 *       handled via {@link InterceptorChain}</li>
 *   <li><b>Event Bus:</b> Action lifecycle events published for monitoring and coordination</li>
 *   <li><b>Tick Budget Enforcement:</b> AI operations are profiled and must complete
 *       within configured budget (default 5ms) to prevent server lag</li>
 *   <li><b>Error Recovery:</b> Automatic retry and recovery strategies for transient failures</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li><b>Structured Error Codes:</b> All errors categorized with specific error codes</li>
 *   <li><b>Recovery Strategies:</b> Automatic recovery based on error category</li>
 *   <li><b>State Cleanup:</b> Guaranteed cleanup via finally blocks</li>
 *   <li><b>Structured Logging:</b> Detailed logging for debugging and monitoring</li>
 * </ul>
 *
 * @see BaseAction
 * @see Task
 * @see com.minewright.plugin.ActionRegistry
 * @see com.minewright.execution.AgentStateMachine
 * @see com.minewright.execution.InterceptorChain
 * @see ErrorRecoveryStrategy
 *
 * @since 1.0.0
 */
public class ActionExecutor {
    private static final Logger LOGGER = TestLogger.getLogger(ActionExecutor.class);

    /**
     * The foreman entity this executor is managing actions for.
     * Used for context, world access, and state updates.
     */
    private final ForemanEntity foreman;

    /**
     * Task planner for converting natural language commands into structured tasks.
     * Lazy-initialized to avoid loading LLM dependencies on entity creation.
     */
    private TaskPlanner taskPlanner;

    /**
     * Thread-safe queue of tasks pending execution.
     * Tasks are added via LLM planning or orchestration assignment.
     * Uses LinkedBlockingQueue for thread-safe concurrent access.
     */
    private final BlockingQueue<Task> taskQueue;

    /**
     * The currently executing action, or null if no action is active.
     * Only one action runs at a time per executor.
     */
    private BaseAction currentAction;

    /**
     * High-level description of the current goal being worked toward.
     * Set from the LLM's plan description when processing commands.
     */
    private String currentGoal;

    /**
     * Counter for tracking ticks since the last action was started.
     * Used to enforce delays between actions for pacing.
     */
    private int ticksSinceLastAction;

    /**
     * Action to execute when completely idle (no tasks, no goal).
     * Typically follows the nearest player to stay nearby.
     */
    private BaseAction idleFollowAction;

    /**
     * Async LLM planning future for non-blocking command processing.
     * Marked volatile for visibility between game thread and LLM callback threads.
     */
    private volatile CompletableFuture<ResponseParser.ParsedResponse> planningFuture;

    /**
     * Flag indicating whether async planning is currently in progress.
     * Prevents concurrent planning requests which could cause state corruption.
     * Uses AtomicBoolean for thread-safe compare-and-set operations.
     */
    private final AtomicBoolean isPlanning = new AtomicBoolean(false);

    /**
     * Command text being processed during async planning.
     * Stored for error handling and user feedback.
     */
    private volatile String pendingCommand;

    /**
     * Action context providing services and dependencies to actions.
     * Includes service container, event bus, state machine, and interceptors.
     */
    private final ActionContext actionContext;

    /**
     * Interceptor chain for cross-cutting concerns around action execution.
     * Handles logging, metrics collection, and event publishing.
     */
    private final InterceptorChain interceptorChain;

    /**
     * State machine tracking the agent's current execution state.
     * States: IDLE, PLANNING, EXECUTING, WAITING, ERROR.
     */
    private final AgentStateMachine stateMachine;

    /**
     * Event bus for publishing action lifecycle events.
     * Other components can subscribe to monitor action progress.
     */
    private final EventBus eventBus;

    /**
     * Tick profiler for enforcing AI operation budget constraints.
     * Tracks time spent in AI operations to prevent server lag.
     * AI operations must complete within configured budget (default 5ms).
     */
    private final TickProfiler tickProfiler;

    /**
     * Retry policy for failed actions.
     */
    private final RetryPolicy retryPolicy = RetryPolicy.STANDARD;

    /**
     * Execution tracker for recording action sequences for skill learning.
     */
    private final ExecutionTracker executionTracker;

    public ActionExecutor(ForemanEntity foreman) {
        this.foreman = foreman;
        this.taskPlanner = null;  // Will be initialized when first needed
        this.taskQueue = new LinkedBlockingQueue<>();  // Thread-safe queue
        this.ticksSinceLastAction = 0;
        this.idleFollowAction = null;
        this.planningFuture = null;
        this.pendingCommand = null;

        // Initialize plugin architecture components
        this.eventBus = new SimpleEventBus();
        this.stateMachine = new AgentStateMachine(eventBus, foreman.getEntityName());
        this.interceptorChain = new InterceptorChain();

        // Setup interceptors
        interceptorChain.addInterceptor(new LoggingInterceptor());
        interceptorChain.addInterceptor(new MetricsInterceptor());
        interceptorChain.addInterceptor(new EventPublishingInterceptor(eventBus, foreman.getEntityName()));

        // Build action context
        ServiceContainer container = new SimpleServiceContainer();
        this.actionContext = ActionContext.builder()
            .serviceContainer(container)
            .eventBus(eventBus)
            .stateMachine(stateMachine)
            .interceptorChain(interceptorChain)
            .build();

        // Initialize tick profiler for budget enforcement
        this.tickProfiler = new TickProfiler();

        // Initialize execution tracker for skill learning
        this.executionTracker = ExecutionTracker.getInstance();

        LOGGER.debug("ActionExecutor initialized with plugin architecture for Foreman '{}'",
            foreman.getEntityName());
    }

    /**
     * Gets the TaskPlanner instance (lazy-initialized).
     * Public for access by dialogue and other systems.
     */
    public TaskPlanner getTaskPlanner() {
        if (taskPlanner == null) {
            LOGGER.info("Initializing TaskPlanner for Foreman '{}'", foreman.getEntityName());
            taskPlanner = new TaskPlanner();
        }
        return taskPlanner;
    }

    /**
     * Processes a natural language command using ASYNC non-blocking LLM calls.
     *
     * <p>This method returns immediately and does NOT block the game thread.
     * The LLM response is processed in tick() when the CompletableFuture completes.</p>
     *
     * <p><b>Non-blocking flow:</b></p>
     * <ol>
     *   <li>User sends command</li>
     *   <li>This method starts async LLM call, returns immediately</li>
     *   <li>Game continues running normally (no freeze!)</li>
     *   <li>tick() checks if planning is done</li>
     *   <li>When done, tasks are queued and execution begins</li>
     * </ol>
     *
     * <p><b>Thread Safety:</b> Uses AtomicBoolean.compareAndSet() to prevent race conditions
     * when multiple commands are submitted simultaneously.</p>
     *
     * @param command The natural language command from the user
     */
    public void processNaturalLanguageCommand(String command) {
        LOGGER.info("Foreman '{}' processing command (async): {}", foreman.getEntityName(), command);

        // THREAD-SAFE: Use compareAndSet to atomically check and set planning state
        // This prevents race conditions when multiple threads submit commands
        if (!isPlanning.compareAndSet(false, true)) {
            LOGGER.warn("Foreman '{}' is already planning, ignoring command: {}", foreman.getEntityName(), command);
            sendToGUI(foreman.getEntityName(), "Hold your horses! I'm still figuring out the last job. Give me a moment!");
            return;
        }

        // Cancel any current actions
        if (currentAction != null) {
            currentAction.cancel();
            currentAction = null;
        }

        if (idleFollowAction != null) {
            idleFollowAction.cancel();
            idleFollowAction = null;
        }

        try {
            // Store command and start async planning
            this.pendingCommand = command;

            // Send immediate feedback to user
            sendToGUI(foreman.getEntityName(), "Looking over the blueprints...");

            // Start async LLM call - returns immediately!
            planningFuture = getTaskPlanner().planTasksAsync(foreman, command);

            LOGGER.info("Foreman '{}' started async planning for: {}", foreman.getEntityName(), command);

        } catch (NoClassDefFoundError e) {
            LOGGER.error("Failed to initialize AI components", e);
            sendToGUI(foreman.getEntityName(), "Sorry boss, my planning tools aren't working right now!");
            isPlanning.set(false);
            planningFuture = null;
        } catch (Exception e) {
            LOGGER.error("Error starting async planning", e);
            sendToGUI(foreman.getEntityName(), "Something went wrong with the planning! Try again in a moment.");
            isPlanning.set(false);
            planningFuture = null;
        }
    }

    /**
     * Legacy synchronous command processing (blocking).
     *
     * <p><b>Warning:</b> This method blocks the game thread for 30-60 seconds during LLM calls.
     * Use {@link #processNaturalLanguageCommand(String)} instead for non-blocking execution.</p>
     *
     * @param command The natural language command
     * @deprecated Use {@link #processNaturalLanguageCommand(String)} instead
     */
    @Deprecated
    public void processNaturalLanguageCommandSync(String command) {
        LOGGER.info("Foreman '{}' processing command (SYNC - blocking!): {}", foreman.getEntityName(), command);

        if (currentAction != null) {
            currentAction.cancel();
            currentAction = null;
        }

        if (idleFollowAction != null) {
            idleFollowAction.cancel();
            idleFollowAction = null;
        }

        try {
            // BLOCKING CALL - freezes game for 30-60 seconds!
            ResponseParser.ParsedResponse response = getTaskPlanner().planTasks(foreman, command);

            if (response == null) {
                sendToGUI(foreman.getEntityName(), "I couldn't make heads or tails of that order, boss. Could you rephrase it?");
                return;
            }

            currentGoal = response.getPlan();
            foreman.getMemory().setCurrentGoal(currentGoal);

            taskQueue.clear();
            taskQueue.addAll(response.getTasks());

            if (MineWrightConfig.ENABLE_CHAT_RESPONSES.get()) {
                sendToGUI(foreman.getEntityName(), "You got it! " + currentGoal);
            }
        } catch (NoClassDefFoundError e) {
            LOGGER.error("Failed to initialize AI components", e);
            sendToGUI(foreman.getEntityName(), "Sorry boss, my planning tools aren't working right now!");
        }

        LOGGER.info("Foreman '{}' queued {} tasks", foreman.getEntityName(), taskQueue.size());
    }
    
    /**
     * Send a message to the GUI pane (client-side only, no chat spam)
     */
    private void sendToGUI(String foremanName, String message) {
        if (foreman.level().isClientSide) {
            com.minewright.client.ForemanOfficeGUI.addCrewMessage(foremanName, message);
        }

        // Also speak the message if voice is enabled
        VoiceManager.getInstance().speakIfEnabled(message);
    }

    /**
     * Main update loop called every game tick (20 times per second).
     *
     * <p>This method orchestrates all execution logic in a non-blocking manner:</p>
     * <ol>
     *   <li>Check if async LLM planning has completed and queue tasks if so</li>
     *   <li>If an action is currently running, call its {@code tick()} method</li>
     *   <li>If action completed, handle result and check for replanning needs</li>
     *   <li>If no action running but tasks are queued, start the next task</li>
     *   <li>If completely idle, follow the nearest player</li>
     * </ol>
     *
     * <p><b>Non-Blocking Design:</b></p>
     * <ul>
     *   <li>LLM planning results are checked via {@link CompletableFuture#isDone()}</li>
     *   <li>Results are retrieved with getNow() which NEVER blocks</li>
     *   <li>All long-running work happens on separate threads</li>
     *   <li>This method always returns quickly to maintain server performance</li>
     * </ul>
     *
     * <p><b>Error Handling:</b></p>
     * <ul>
     *   <li>Planning cancellations reset state machine to IDLE</li>
     *   <li>Exceptions are caught and logged, resetting state</li>
     *   <li>Action failures notify player and may trigger replanning</li>
     * </ul>
     *
     * <p><b>Thread Safety:</b> Called on the Minecraft server thread only.
     * Interacts with volatile/atomic fields for visibility across threads.</p>
     *
     * <p><b>Tick Budget Enforcement:</b></p>
     * <p>Uses {@link TickProfiler} to ensure AI operations complete within budget.
     * If budget is exceeded, operations defer to next tick to prevent server lag.</p>
     */
    public void tick() {
        // Start tick profiling for budget enforcement
        tickProfiler.startTick();
        ticksSinceLastAction++;

        // Check if async planning is complete (non-blocking check!)
        if (isPlanning.get() && planningFuture != null && planningFuture.isDone()) {
            try {
                // NON-BLOCKING: getNow() returns immediately, never blocks the server thread
                ResponseParser.ParsedResponse response = planningFuture.getNow(null);

                if (response != null) {
                    currentGoal = response.getPlan();
                    foreman.getMemory().setCurrentGoal(currentGoal);

                    taskQueue.clear();
                    taskQueue.addAll(response.getTasks());

                    // Start tracking execution sequence for skill learning
                    executionTracker.startTracking(foreman.getEntityName(), currentGoal);

                    if (MineWrightConfig.ENABLE_CHAT_RESPONSES.get()) {
                        sendToGUI(foreman.getEntityName(), "You got it! " + currentGoal);
                    }

                    LOGGER.info("Foreman '{}' async planning complete: {} tasks queued",
                        foreman.getEntityName(), taskQueue.size());
                } else {
                    sendToGUI(foreman.getEntityName(), "I couldn't make heads or tails of that order, boss. Could you rephrase it?");
                    LOGGER.warn("Foreman '{}' async planning returned null response", foreman.getEntityName());
                }

            } catch (java.util.concurrent.CancellationException e) {
                LOGGER.info("Foreman '{}' planning was cancelled", foreman.getEntityName());
                sendToGUI(foreman.getEntityName(), "Planning cancelled. Back to work!");
                // Reset state machine to allow recovery
                stateMachine.forceTransition(AgentState.IDLE, "planning cancelled");
            } catch (java.util.concurrent.CompletionException e) {
                LOGGER.error("Foreman '{}' planning failed with exception", foreman.getEntityName(), e.getCause());
                sendToGUI(foreman.getEntityName(), "Something went wrong with the planning! Let's try that again.");
                // Reset state machine to allow recovery
                stateMachine.forceTransition(AgentState.IDLE, "planning failed");
            } catch (Exception e) {
                LOGGER.error("Foreman '{}' failed to get planning result", foreman.getEntityName(), e);
                sendToGUI(foreman.getEntityName(), "Something went wrong with the planning! Let's try that again.");
                // Reset state machine to allow recovery
                stateMachine.forceTransition(AgentState.IDLE, "planning failed");
            } finally {
                isPlanning.set(false);
                planningFuture = null;
                pendingCommand = null;
            }
        }

        // Check budget after planning processing
        if (tickProfiler.isOverBudget()) {
            tickProfiler.logWarningIfExceeded();
            return; // Defer remaining work to next tick
        }

        if (currentAction != null) {
            if (currentAction.isComplete()) {
                ActionResult result = currentAction.getResult();

                LOGGER.info("[{}] Action completed: {} (Success: {}, Error: {})",
                    foreman.getEntityName(), result.getMessage(),
                    result.isSuccess(), result.getErrorCode());

                // Record action execution for skill learning
                executionTracker.recordAction(foreman.getEntityName(), currentAction, result);

                // Handle action result with error recovery
                handleActionResult(result, currentAction.getTask());

                currentAction = null;
            } else {
                if (ticksSinceLastAction % 100 == 0) {
                    LOGGER.info("Foreman '{}' - Ticking action: {}",
                        foreman.getEntityName(), currentAction.getDescription());
                }

                // Check budget before calling action.tick()
                if (tickProfiler.isOverBudget()) {
                    tickProfiler.logWarningIfExceeded();
                    return; // Defer action tick to next tick
                }

                currentAction.tick();

                // Log warning if budget exceeded after action tick
                tickProfiler.logWarningIfExceeded();
                return;
            }
        }

        // Check budget before processing task queue
        if (tickProfiler.isOverBudget()) {
            tickProfiler.logWarningIfExceeded();
            return; // Defer task queue processing to next tick
        }

        if (ticksSinceLastAction >= MineWrightConfig.ACTION_TICK_DELAY.get()) {
            if (!taskQueue.isEmpty()) {
                Task nextTask = taskQueue.poll();
                executeTask(nextTask);
                ticksSinceLastAction = 0;
                return;
            }
        }

        // Check budget before idle processing
        if (tickProfiler.isOverBudget()) {
            tickProfiler.logWarningIfExceeded();
            return; // Defer idle processing to next tick
        }

        // When completely idle (no tasks, no goal), follow nearest player
        if (taskQueue.isEmpty() && currentAction == null && currentGoal == null) {
            // End tracking if we were tracking a sequence
            if (executionTracker.isTracking(foreman.getEntityName())) {
                executionTracker.endTracking(foreman.getEntityName(), true);
                LOGGER.debug("Ended execution tracking for agent '{}' (sequence complete)",
                    foreman.getEntityName());
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

        // Log budget status at end of tick (only if over threshold to reduce log spam)
        tickProfiler.logWarningIfExceeded();
    }

    private void executeTask(Task task) {
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

    /**
     * Handles action creation errors.
     *
     * @param task The task that failed
     */
    private void handleTaskCreationError(Task task) {
        String errorMsg = "Unknown action type: " + task.getAction();
        LOGGER.error("[{}] FAILED to create action for task: {}",
            foreman.getEntityName(), task);

        ActionResult result = ActionResult.failure(
            ActionResult.ErrorCode.INVALID_ACTION_TYPE,
            errorMsg,
            true
        );

        handleActionResult(result, task);
    }

    /**
     * Handles unexpected execution errors.
     *
     * @param task The task being executed
     * @param e   The exception
     */
    private void handleExecutionError(Task task, Exception e) {
        LOGGER.error("[{}] Execution error for task {}: {}",
            foreman.getEntityName(), task.getAction(), e.getMessage(), e);

        ActionResult result = ActionResult.failure(
            ActionResult.ErrorCode.EXECUTION_ERROR,
            "Execution error: " + e.getClass().getSimpleName() + ": " + e.getMessage(),
            true
        );

        handleActionResult(result, task);
    }

    /**
     * Handles action results with error recovery.
     *
     * @param result The action result
     * @param task   The task that was executed
     */
    private void handleActionResult(ActionResult result, Task task) {
        if (result.isSuccess()) {
            LOGGER.info("[{}] Action completed successfully: {}",
                foreman.getEntityName(), result.getMessage());
            String description = (currentAction != null) ? currentAction.getDescription() :
                (task != null ? task.getAction() : "unknown");
            foreman.getMemory().addAction(description);
            return;
        }

        // Handle failure with error recovery strategy
        ErrorRecoveryStrategy recoveryStrategy = ErrorRecoveryStrategy.fromResult(result);

        LOGGER.warn("[{}] Action failed [{}]: {}",
            foreman.getEntityName(), result.getErrorCode(), result.getMessage());

        // Attempt recovery
        boolean canRecover = recoveryStrategy.attemptRecovery(foreman, result);

        // Notify player via chat
        foreman.sendChatMessage("Job hit a snag: " + result.getMessage());

        // Show in GUI if enabled
        if (MineWrightConfig.ENABLE_CHAT_RESPONSES.get()) {
            sendToGUI(foreman.getEntityName(), "Problem: " + result.getMessage());
        }

        // Log recovery suggestion if available
        if (result.getRecoverySuggestion() != null) {
            LOGGER.info("[{}] Recovery suggestion: {}",
                foreman.getEntityName(), result.getRecoverySuggestion());
        }

        // Check if replanning is needed
        if (result.requiresReplanning() && result.getErrorCode().getRecoveryCategory() !=
            ErrorRecoveryStrategy.RecoveryCategory.PERMANENT) {
            LOGGER.info("[{}] Task requires replanning due to recoverable error",
                foreman.getEntityName());
        }
    }

    /**
     * Creates an action using the plugin registry with legacy fallback.
     *
     * <p>First attempts to create the action via ActionRegistry (plugin system).
     * If the registry doesn't have the action or creation fails, falls back
     * to the legacy switch statement for backward compatibility.</p>
     *
     * @param task Task containing action type and parameters
     * @return Created action, or null if unknown action type
     */
    private BaseAction createAction(Task task) {
        String actionType = task.getAction();

        // Try registry-based creation first (plugin architecture)
        ActionRegistry registry = ActionRegistry.getInstance();
        if (registry.hasAction(actionType)) {
            BaseAction action = registry.createAction(actionType, foreman, task, actionContext);
            if (action != null) {
                LOGGER.debug("Created action '{}' via registry (plugin: {})",
                    actionType, registry.getPluginForAction(actionType));
                return action;
            }
        }

        // Fallback to legacy switch statement for backward compatibility
        LOGGER.debug("Using legacy fallback for action: {}", actionType);
        return createActionLegacy(task);
    }

    /**
     * Legacy action creation using switch statement.
     *
     * <p>Kept for backward compatibility during migration to plugin system.
     * Will be removed in a future version once all actions are registered
     * via plugins.</p>
     *
     * @param task Task containing action type and parameters
     * @return Created action, or null if unknown
     * @deprecated Use ActionRegistry instead
     */
    @Deprecated
    private BaseAction createActionLegacy(Task task) {
        return switch (task.getAction()) {
            case "pathfind" -> new PathfindAction(foreman, task);
            case "mine" -> new MineBlockAction(foreman, task);
            case "place" -> new PlaceBlockAction(foreman, task);
            case "craft" -> new CraftItemAction(foreman, task);
            case "attack" -> new CombatAction(foreman, task);
            case "follow" -> new FollowPlayerAction(foreman, task);
            case "gather" -> new GatherResourceAction(foreman, task);
            case "build" -> new BuildStructureAction(foreman, task);
            default -> {
                LOGGER.warn("Unknown action type: {}", task.getAction());
                yield null;
            }
        };
    }

    public void stopCurrentAction() {
        // End tracking if we were tracking a sequence
        if (executionTracker.isTracking(foreman.getEntityName())) {
            executionTracker.endTracking(foreman.getEntityName(), false);
            LOGGER.debug("Ended execution tracking for agent '{}' (action stopped)",
                foreman.getEntityName());
        }

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

        // Reset state machine
        stateMachine.reset();
    }

    public boolean isExecuting() {
        return currentAction != null || !taskQueue.isEmpty();
    }

    public String getCurrentGoal() {
        return currentGoal;
    }

    /**
     * Returns the event bus for subscribing to action events.
     *
     * @return EventBus instance
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Returns the agent state machine.
     *
     * @return AgentStateMachine instance
     */
    public AgentStateMachine getStateMachine() {
        return stateMachine;
    }

    /**
     * Returns the interceptor chain for adding custom interceptors.
     *
     * @return InterceptorChain instance
     */
    public InterceptorChain getInterceptorChain() {
        return interceptorChain;
    }

    /**
     * Returns the action context.
     *
     * @return ActionContext instance
     */
    public ActionContext getActionContext() {
        return actionContext;
    }

    /**
     * Checks if the agent is currently planning (async LLM call in progress).
     *
     * @return true if planning
     */
    public boolean isPlanning() {
        return isPlanning.get();
    }

    /**
     * Queues a task for execution without going through the LLM planner.
     * Used by the orchestration system when tasks are assigned by the foreman.
     *
     * <p><b>Thread Safety:</b> This method is thread-safe and can be called from
     * any thread (e.g., orchestration thread). Uses {@link BlockingQueue#offer}
     * for non-blocking insertion.</p>
     *
     * @param task Task to queue
     */
    public void queueTask(Task task) {
        if (task == null) {
            LOGGER.warn("Attempted to queue null task for Foreman '{}'", foreman.getEntityName());
            return;
        }

        // Use offer() for thread-safe, non-blocking insertion
        if (taskQueue.offer(task)) {
            LOGGER.info("Foreman '{}' - Task queued: {}",
                foreman.getEntityName(), task.getAction());
        } else {
            LOGGER.warn("Foreman '{}' - Failed to queue task (queue full): {}",
                foreman.getEntityName(), task.getAction());
        }
    }

    /**
     * Gets the current action progress percentage (0-100).
     * Used by the orchestration system for progress reporting.
     *
     * @return Progress percentage
     */
    public int getCurrentActionProgress() {
        if (currentAction == null) {
            return taskQueue.isEmpty() ? 100 : 0;
        }

        // Try to get progress from the action if it supports it
        if (currentAction instanceof com.minewright.action.actions.BuildStructureAction) {
            com.minewright.action.actions.BuildStructureAction buildAction =
                (com.minewright.action.actions.BuildStructureAction) currentAction;
            return buildAction.getProgressPercent();
        }

        // Default to 50% if action is in progress
        return currentAction.isComplete() ? 100 : 50;
    }

    /**
     * Gets the current action being executed.
     * Used by the GUI to display progress information.
     *
     * @return Current action, or null if no action is active
     */
    public com.minewright.action.actions.BaseAction getCurrentAction() {
        return currentAction;
    }
}

