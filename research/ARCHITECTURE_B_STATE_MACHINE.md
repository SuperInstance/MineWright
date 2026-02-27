# State Machine Architecture - MineWright Foreman System

**Document Version:** 1.0
**Last Updated:** 2026-02-26
**Complexity Rating:** 5/10
**Architecture Pattern:** State Pattern + Hierarchical State Machine

---

## 1. Overview

The MineWright system employs a **hierarchical state machine** architecture to manage agent lifecycle and orchestration. The Foreman coordinates multiple crew members through explicit state transitions, ensuring predictable behavior and clear separation of concerns.

### Key Design Principles

1. **Explicit State Transitions** - All state changes are validated and logged
2. **Thread Safety** - Uses `AtomicReference` for concurrent access
3. **Event-Driven** - State changes publish events via the EventBus
4. **Hierarchical Organization** - Foreman orchestrates workers in a tree structure
5. **Recovery Support** - Forced transitions for error recovery scenarios

### Architecture Diagram

```
                    Human Player
                         │
                         ▼
              ┌─────────────────────┐
              │   FOREMAN STATE     │
              │     Machine         │
              └──────────┬──────────┘
                         │
            ┌────────────┼────────────┐
            ▼            ▼            ▼
     ┌──────────┐  ┌──────────┐  ┌──────────┐
     │ WORKER   │  │ WORKER   │  │ WORKER   │
     │ STATE    │  │ STATE    │  │ STATE    │
     │ Machine  │  │ Machine  │  │ Machine  │
     └──────────┘  └──────────┘  └──────────┘
```

---

## 2. State Definitions

### 2.1 AgentState Enum

Located at `C:\Users\casey\minewright\src\main\java\com\minewright\ai\execution\AgentState.java`

```java
public enum AgentState {
    /** Agent is idle, waiting for a command */
    IDLE("Idle", "Agent is waiting for commands"),

    /** Agent is processing a command through the LLM */
    PLANNING("Planning", "Processing command with AI"),

    /** Agent is actively executing tasks */
    EXECUTING("Executing", "Performing actions"),

    /** Agent execution is temporarily paused */
    PAUSED("Paused", "Execution temporarily suspended"),

    /** Agent has completed all tasks successfully */
    COMPLETED("Completed", "All tasks finished successfully"),

    /** Agent encountered an error and stopped */
    FAILED("Failed", "Encountered an error");
}
```

### 2.2 State Descriptions

| State | Display Name | Description | Can Accept Commands | Is Terminal | Is Active |
|-------|--------------|-------------|---------------------|-------------|-----------|
| IDLE | Idle | Agent is waiting for commands | Yes | No | No |
| PLANNING | Planning | Processing command with AI | No | No | Yes |
| EXECUTING | Executing | Performing actions | No | No | Yes |
| PAUSED | Paused | Execution temporarily suspended | No | No | No |
| COMPLETED | Completed | All tasks finished successfully | Yes | Yes | No |
| FAILED | Failed | Encountered an error | Yes | Yes | No |

---

## 3. Transition Table

### 3.1 Valid State Transitions

| From State | To State | Trigger Condition | Side Effects |
|------------|----------|-------------------|--------------|
| IDLE | PLANNING | New command received | Start async LLM call |
| PLANNING | EXECUTING | Planning complete, tasks ready | Begin task execution |
| PLANNING | FAILED | Planning error/timeout | Log error, notify user |
| PLANNING | IDLE | Planning cancelled | Clean up resources |
| EXECUTING | COMPLETED | All tasks done | Publish completion event |
| EXECUTING | FAILED | Execution error | Log failure, may retry |
| EXECUTING | PAUSED | User pause request | Suspend execution |
| PAUSED | EXECUTING | Resume request | Continue execution |
| PAUSED | IDLE | Cancel request | Clean up, return to idle |
| COMPLETED | IDLE | Auto-transition after notification | Ready for next command |
| FAILED | IDLE | Reset after error | Clear error state |

### 3.2 Invalid Transitions

The following transitions are **explicitly prevented**:

- IDLE → EXECUTING (must plan first)
- IDLE → COMPLETED (no work to complete)
- IDLE → FAILED (nothing to fail)
- PLANNING → PAUSED (cannot pause during planning)
- PLANNING → COMPLETED (must execute first)
- EXECUTING → PLANNING (must complete or cancel first)
- EXECUTING → IDLE (must complete or pause first)
- PAUSED → COMPLETED (must resume first)
- PAUSED → FAILED (must resume first)
- COMPLETED → Any (terminal state, only to IDLE)
- FAILED → Any (terminal state, only to IDLE)

---

## 4. State Diagram

```
                    ┌──────────────────────────────────────────┐
                    │                                          │
                    │          Human Commands                  │
                    │          (new task, cancel, pause)        │
                    │                                          │
                    └─────────────────┬────────────────────────┘
                                      │
                                      ▼
   ┌─────────────────────────────────────────────────────────────────┐
   │                         IDLE                                    │
   │                     Ready for work                               │
   └─────────────────────────────┬───────────────────────────────────┘
                                 │ new command
                                 ▼
   ┌─────────────────────────────────────────────────────────────────┐
   │                       PLANNING                                   │
   │              Processing with LLM (async)                         │
   └───┬───────────────────────┬───────────────────────────────────┬─┘
       │                       │                                   │
   success                  error                               cancel
       │                       │                                   │
       ▼                       ▼                                   ▼
┌──────────────┐        ┌──────────────┐                   ┌──────────────┐
│  EXECUTING   │        │   FAILED     │◄──────────────────│   IDLE       │
│  Performing  │        │   (error)    │                   │  (return)    │
│   tasks      │        └──────┬───────┘                   └──────────────┘
└───┬───┬───┬───┘               │
    │   │   │                   │
    │   │   │               auto-reset
    │   │   └───────────────────┘
    │   │
    │   └───►┌──────────────┐
    │        │   PAUSED     │
    │        │ (user pause) │
    │        └──────┬───────┘
    │               │
    │          resume│cancel
    │               │
    └───────────────┴───┐
                        │
                   all done
                        │
                        ▼
                ┌──────────────┐
                │  COMPLETED   │
                │ (success!)   │
                └──────┬───────┘
                       │
                  auto-return
                       │
                       ▼
                ┌──────────────┐
                │     IDLE     │
                └──────────────┘
```

---

## 5. Java Implementation

### 5.1 AgentStateMachine Class

Located at `C:\Users\casey\minewright\src\main\java\com\minewright\ai\execution\AgentStateMachine.java`

```java
/**
 * State machine for managing agent execution states.
 *
 * <p>Implements the State Pattern with explicit transition validation.
 * Invalid transitions are rejected and logged. State changes publish
 * events to the EventBus for observers.</p>
 *
 * <p><b>Thread Safety:</b> Uses AtomicReference for thread-safe state updates.</p>
 */
public class AgentStateMachine {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentStateMachine.class);

    /**
     * Defines valid transitions from each state.
     * Uses EnumMap for O(1) lookup performance.
     */
    private static final Map<AgentState, Set<AgentState>> VALID_TRANSITIONS;

    static {
        VALID_TRANSITIONS = new EnumMap<>(AgentState.class);

        // IDLE can go to PLANNING (new command)
        VALID_TRANSITIONS.put(AgentState.IDLE,
            EnumSet.of(AgentState.PLANNING));

        // PLANNING can go to EXECUTING (success) or FAILED (error) or IDLE (cancel)
        VALID_TRANSITIONS.put(AgentState.PLANNING,
            EnumSet.of(AgentState.EXECUTING, AgentState.FAILED, AgentState.IDLE));

        // EXECUTING can complete, fail, or pause
        VALID_TRANSITIONS.put(AgentState.EXECUTING,
            EnumSet.of(AgentState.COMPLETED, AgentState.FAILED, AgentState.PAUSED));

        // PAUSED can resume or cancel
        VALID_TRANSITIONS.put(AgentState.PAUSED,
            EnumSet.of(AgentState.EXECUTING, AgentState.IDLE));

        // COMPLETED goes back to IDLE
        VALID_TRANSITIONS.put(AgentState.COMPLETED,
            EnumSet.of(AgentState.IDLE));

        // FAILED can go back to IDLE (reset)
        VALID_TRANSITIONS.put(AgentState.FAILED,
            EnumSet.of(AgentState.IDLE));
    }

    /**
     * Current state (thread-safe).
     */
    private final AtomicReference<AgentState> currentState;

    /**
     * Event bus for publishing state change events.
     */
    private final EventBus eventBus;

    /**
     * Agent identifier for logging.
     */
    private final String agentId;

    /**
     * Constructs a state machine starting in IDLE state.
     *
     * @param eventBus Event bus for state change notifications (can be null)
     * @param agentId  Agent identifier for logging
     */
    public AgentStateMachine(EventBus eventBus, String agentId) {
        this.currentState = new AtomicReference<>(AgentState.IDLE);
        this.eventBus = eventBus;
        this.agentId = agentId;
        LOGGER.debug("[{}] State machine initialized in IDLE state", agentId);
    }

    /**
     * Returns the current state.
     */
    public AgentState getCurrentState() {
        return currentState.get();
    }

    /**
     * Checks if transition to target state is valid.
     *
     * @param targetState Desired target state
     * @return true if transition is valid
     */
    public boolean canTransitionTo(AgentState targetState) {
        if (targetState == null) return false;

        AgentState current = currentState.get();
        Set<AgentState> validTargets = VALID_TRANSITIONS.get(current);

        return validTargets != null && validTargets.contains(targetState);
    }

    /**
     * Transitions to a new state if valid.
     *
     * <p>If transition is valid, publishes a StateTransitionEvent to the EventBus.</p>
     *
     * @param targetState Target state
     * @param reason      Reason for transition (for logging/events)
     * @return true if transition was successful
     */
    public boolean transitionTo(AgentState targetState, String reason) {
        if (targetState == null) {
            LOGGER.warn("[{}] Cannot transition to null state", agentId);
            return false;
        }

        AgentState fromState = currentState.get();

        // Check if transition is valid
        if (!canTransitionTo(targetState)) {
            LOGGER.warn("[{}] Invalid state transition: {} → {} (allowed: {})",
                agentId, fromState, targetState, VALID_TRANSITIONS.get(fromState));
            return false;
        }

        // Atomic compare-and-set for thread safety
        if (currentState.compareAndSet(fromState, targetState)) {
            LOGGER.info("[{}] State transition: {} → {}{}",
                agentId, fromState, targetState,
                reason != null ? " (reason: " + reason + ")" : "");

            // Publish event
            if (eventBus != null) {
                eventBus.publish(new StateTransitionEvent(agentId, fromState, targetState, reason));
            }

            return true;
        } else {
            // State changed between get and compareAndSet (race condition)
            LOGGER.warn("[{}] State transition failed: concurrent modification", agentId);
            return false;
        }
    }

    /**
     * Forces a transition to a state, bypassing validation.
     *
     * <p><b>Warning:</b> Use only for recovery scenarios. Prefer transitionTo().</p>
     *
     * @param targetState Target state
     * @param reason      Reason for forced transition
     */
    public void forceTransition(AgentState targetState, String reason) {
        if (targetState == null) return;

        AgentState fromState = currentState.getAndSet(targetState);
        LOGGER.warn("[{}] FORCED state transition: {} → {} (reason: {})",
            agentId, fromState, targetState, reason);

        if (eventBus != null) {
            eventBus.publish(new StateTransitionEvent(agentId, fromState, targetState,
                "FORCED: " + reason));
        }
    }

    /**
     * Resets the state machine to IDLE.
     *
     * <p>Publishes a state transition event if state changes.</p>
     */
    public void reset() {
        AgentState previous = currentState.getAndSet(AgentState.IDLE);
        if (previous != AgentState.IDLE) {
            LOGGER.info("[{}] State machine reset: {} → IDLE", agentId, previous);
            if (eventBus != null) {
                eventBus.publish(new StateTransitionEvent(agentId, previous, AgentState.IDLE, "reset"));
            }
        }
    }

    /**
     * Checks if the agent can accept new commands.
     *
     * @return true if in IDLE, COMPLETED, or FAILED state
     */
    public boolean canAcceptCommands() {
        return currentState.get().canAcceptCommands();
    }

    /**
     * Checks if the agent is actively working.
     *
     * @return true if in PLANNING or EXECUTING state
     */
    public boolean isActive() {
        return currentState.get().isActive();
    }

    /**
     * Returns valid transitions from current state.
     *
     * @return Set of valid target states
     */
    public Set<AgentState> getValidTransitions() {
        Set<AgentState> valid = VALID_TRANSITIONS.get(currentState.get());
        return valid != null ? EnumSet.copyOf(valid) : EnumSet.noneOf(AgentState.class);
    }
}
```

### 5.2 State-Dependent Behavior Example

How the same input produces different output per state:

```java
/**
 * Example: State-dependent command processing
 */
public class StateDependentCommandHandler {

    private final AgentStateMachine stateMachine;

    public void handleCommand(String command) {
        AgentState currentState = stateMachine.getCurrentState();

        switch (currentState) {
            case IDLE:
            case COMPLETED:
            case FAILED:
                // Accept new command
                startPlanning(command);
                break;

            case PLANNING:
                // Reject new command - already planning
                sendMessage("I'm still thinking about the previous command!");
                break;

            case EXECUTING:
                // Queue command for later execution
                queueCommand(command);
                sendMessage("I'll get to that once I'm done here.");
                break;

            case PAUSED:
                // Can either resume or replace current task
                sendMessage("I'm paused. Should I continue or do something else?");
                break;
        }
    }

    private void startPlanning(String command) {
        stateMachine.transitionTo(AgentState.PLANNING, "New command: " + command);
        // ... trigger LLM planning
    }
}
```

---

## 6. Foreman-Specific State Machine

### 6.1 ForemanStateMachine Extension

The foreman requires an extended state machine to handle orchestration responsibilities:

```java
/**
 * Extended state machine for the Foreman agent.
 *
 * <p>Adds orchestration-specific states for managing workers and
 * coordinating multi-agent tasks.</p>
 */
public class ForemanStateMachine extends AgentStateMachine {

    /**
     * Foreman-specific states extend the base AgentState.
     */
    public enum ForemanState {
        /** Foreman is waiting for human commands */
        IDLE,

        /** Foreman is decomposing a goal into worker tasks */
        COORDINATING,

        /** Foreman is conversing with human (clarification, status updates) */
        CONVERSING,

        /** Foreman is waiting for workers to complete tasks */
        WAITING,

        /** Foreman is assigning tasks to workers */
        ASSIGNING,

        /** Foreman encountered an orchestration error */
        ERROR;

        public boolean canAcceptCommands() {
            return this == IDLE || this == WAITING || this == ERROR;
        }

        public boolean isTerminal() {
            return this == ERROR;
        }
    }

    private final AtomicReference<ForemanState> foremanState;
    private final OrchestratorService orchestrator;

    public ForemanStateMachine(EventBus eventBus, String agentId, OrchestratorService orchestrator) {
        super(eventBus, agentId);
        this.foremanState = new AtomicReference<>(ForemanState.IDLE);
        this.orchestrator = orchestrator;
    }

    /**
     * Transitions to a new foreman state with validation.
     */
    public boolean transitionTo(ForemanState targetState, String reason) {
        ForemanState current = foremanState.get();

        if (!isValidTransition(current, targetState)) {
            LOGGER.warn("[Foreman] Invalid transition: {} → {}", current, targetState);
            return false;
        }

        if (foremanState.compareAndSet(current, targetState)) {
            LOGGER.info("[Foreman] State: {} → {} ({})", current, targetState, reason);

            // Sync base state machine
            syncBaseState(targetState);

            return true;
        }

        return false;
    }

    private boolean isValidTransition(ForemanState from, ForemanState to) {
        return switch (from) {
            case IDLE -> to == ForemanState.COORDINATING || to == ForemanState.CONVERSING;
            case COORDINATING -> to == ForemanState.ASSIGNING || to == ForemanState.ERROR;
            case ASSIGNING -> to == ForemanState.WAITING || to == ForemanState.ERROR;
            case WAITING -> to == ForemanState.IDLE || to == ForemanState.CONVERSING
                         || to == ForemanState.COORDINATING;
            case CONVERSING -> to == ForemanState.IDLE || to == ForemanState.COORDINATING;
            case ERROR -> to == ForemanState.IDLE;
        };
    }

    private void syncBaseState(ForemanState foremanState) {
        AgentState baseState = switch (foremanState) {
            case IDLE -> AgentState.IDLE;
            case COORDINATING, ASSIGNING -> AgentState.PLANNING;
            case WAITING -> AgentState.EXECUTING;
            case CONVERSING -> AgentState.EXECUTING;
            case ERROR -> AgentState.FAILED;
        };

        // Update base state machine without double-publishing events
        super.forceTransition(baseState, "Sync with foreman state: " + foremanState);
    }

    public ForemanState getForemanState() {
        return foremanState.get();
    }
}
```

### 6.2 Foreman State Diagram

```
                    Human Command
                         │
                         ▼
    ┌───────────────────────────────────────┐
    │              IDLE                     │
    │    Waiting for human commands         │
    └───────────────────┬───────────────────┘
                        │
          ┌─────────────┴─────────────┐
          │                           │
    needs clarification          needs planning
          │                           │
          ▼                           ▼
 ┌───────────────┐         ┌─────────────────┐
 │  CONVERSING   │         │  COORDINATING   │
 │ (chat/clarify)│         │ (decompose task)│
 └───────┬───────┘         └────────┬────────┘
         │                          │
    resolved              tasks created
         │                          │
         └──────────┬───────────────┘
                    │
                    ▼
         ┌──────────────────┐
         │    ASSIGNING     │
         │ (give to workers)│
         └────────┬─────────┘
                  │
              assigned
                  │
                  ▼
         ┌──────────────────┐
         │     WAITING      │◄─────────────────┐
         │ (monitor workers)│                  │
         └────────┬─────────┘                  │
                  │                          │
            all done / error                   │
                  │                          │
          ┌───────┴────────┐                 │
          │                │                 │
     success            error                │
          │                │                 │
          ▼                ▼                 │
     ┌─────────┐      ┌──────────┐          │
     │  IDLE   │      │  ERROR   │          │
     └─────────┘      └────┬─────┘          │
                           │                │
                      recovery              │
                           │                │
                           └────────────────┘
```

---

## 7. State-Dependent Behavior

### 7.1 Command Handling by State

| State | Command Handling | Response |
|-------|-----------------|----------|
| IDLE | Accept command immediately | "Working on it!" |
| PLANNING | Reject/Queue | "Hold on, I'm thinking..." |
| EXECUTING | Queue for later | "I'll get to that next." |
| PAUSED | Ask for clarification | "Resume or new task?" |
| COMPLETED | Accept command | "What's next?" |
| FAILED | Accept command (retry) | "Let's try again." |

### 7.2 Foreman-Specific Behaviors

| Foreman State | Behavior | Worker Interaction |
|---------------|----------|-------------------|
| IDLE | Waiting for human command | Workers idle |
| COORDINATING | Decomposing goal into tasks | N/A |
| ASSIGNING | Distributing tasks to workers | Sending task assignments |
| WAITING | Monitoring worker progress | Receiving progress updates |
| CONVERSING | Chatting with human | Workers continue tasks |
| ERROR | Handling orchestration failure | May reassign tasks |

### 7.3 Example: State-Dependent Message Handling

```java
/**
 * Foreman handles messages differently based on state.
 */
public class ForemanMessageHandler {

    private final ForemanStateMachine stateMachine;

    public void handleMessage(AgentMessage message) {
        ForemanState state = stateMachine.getForemanState();

        switch (state) {
            case IDLE:
                // Most messages are unexpected in IDLE
                if (message.getType() == Type.TASK_COMPLETE) {
                    LOGGER.warn("Received task completion in IDLE - may be stale");
                }
                break;

            case COORDINATING:
                // Don't interrupt planning
                queueMessageForLater(message);
                break;

            case ASSIGNING:
                // Worker status updates are expected
                if (message.getType() == Type.STATUS_REPORT) {
                    updateWorkerStatus(message);
                }
                break;

            case WAITING:
                // Actively monitoring for task updates
                if (message.getType() == Type.TASK_PROGRESS) {
                    handleProgressUpdate(message);
                } else if (message.getType() == Type.TASK_COMPLETE) {
                    handleTaskComplete(message);
                } else if (message.getType() == Type.TASK_FAILED) {
                    handleTaskFailed(message);
                }
                break;

            case CONVERSING:
                // May respond to worker help requests even while chatting
                if (message.getType() == Type.HELP_REQUEST) {
                    prioritizeHelpRequest(message);
                }
                break;

            case ERROR:
                // Limited response - focus on recovery
                if (message.getType() == Type.HELLO) {
                    // Worker checking in after error
                    LOGGER.info("Worker '{}' checking in during error state",
                        message.getSenderName());
                }
                break;
        }
    }
}
```

---

## 8. Integration with ActionExecutor

The state machine integrates tightly with the `ActionExecutor` class:

```java
public class ActionExecutor {
    private final AgentStateMachine stateMachine;
    private boolean isPlanning = false;

    public void processNaturalLanguageCommand(String command) {
        // Check state first
        if (!stateMachine.canAcceptCommands()) {
            AgentState current = stateMachine.getCurrentState();
            if (current == AgentState.PLANNING) {
                sendMessage("I'm already planning something!");
            } else if (current == AgentState.EXECUTING) {
                sendMessage("I'm busy right now!");
            }
            return;
        }

        // Transition to PLANNING
        stateMachine.transitionTo(AgentState.PLANNING, "New command: " + command);
        isPlanning = true;

        // Start async planning...
    }

    public void tick() {
        // Check if planning complete
        if (isPlanning && planningFuture.isDone()) {
            // Get results and queue tasks
            // Transition to EXECUTING
            stateMachine.transitionTo(AgentState.EXECUTING, "Planning complete");
            isPlanning = false;
        }

        // Execute current action
        if (currentAction != null && currentAction.isComplete()) {
            if (taskQueue.isEmpty()) {
                // All done!
                stateMachine.transitionTo(AgentState.COMPLETED, "All tasks complete");
                // Auto-transition back to IDLE
                stateMachine.transitionTo(AgentState.IDLE, "Ready for next command");
            }
        }
    }
}
```

---

## 9. Event System Integration

State transitions trigger events via the EventBus:

```java
public class StateTransitionEvent {
    private final String agentId;
    private final AgentState fromState;
    private final AgentState toState;
    private final String reason;
    private final Instant timestamp;

    // Constructor, getters...
}

// Example subscriber
public class StateTransitionLogger {
    @Subscribe
    public void onStateTransition(StateTransitionEvent event) {
        LOGGER.info("[StateTransition] {}: {} → {} (reason: {})",
            event.getAgentId(),
            event.getFromState(),
            event.getToState(),
            event.getReason());

        // Could trigger UI updates, metrics, etc.
    }
}
```

---

## 10. Pros and Cons

### 10.1 Advantages

1. **Clear Behavior**
   - Explicit states make system behavior predictable
   - Easy to understand what the agent is doing at any moment
   - State transitions are logged and auditable

2. **Easy to Debug**
   - Current state is always queryable
   - Invalid transitions are caught and logged
   - Event system provides transition history

3. **Thread Safety**
   - `AtomicReference` ensures safe concurrent access
   - Compare-and-set prevents race conditions
   - No locks required for state reads

4. **Predictable**
   - Same input always produces same output for a given state
   - No surprises from hidden state
   - Easy to test

5. **Extensible**
   - Easy to add new states
   - Foreman can extend base state machine
   - State-specific behavior isolated

6. **Observable**
   - Event system allows reactive programming
   - UI can respond to state changes
   - Metrics can track time-in-state

### 10.2 Disadvantages

1. **Rigid Structure**
   - Cannot easily handle emergent behaviors
   - State explosion risk as features grow
   - Difficult to model complex workflows

2. **State Explosion**
   - Adding features often requires new states
   - Number of transitions grows quadratically
   - Maintenance burden increases

3. **Hard to Handle Unexpected Events**
   - Edge cases may require "hacks" or forced transitions
   - Unexpected inputs may not fit state model
   - Recovery paths can be complex

4. **Context Switching**
   - State transitions can be expensive if they trigger many events
   - Careful design needed to avoid transition storms

5. **Testing Complexity**
   - Must test all state transition paths
   - Number of test cases grows with states
   - Race conditions in concurrent scenarios

### 10.3 When to Use

**Use State Machine When:**
- System has clear, distinct modes of operation
- Behavior is well-specified and predictable
- You need auditability and debuggability
- Thread safety is important
- UI needs to reflect current mode

**Consider Alternatives When:**
- Behavior is highly dynamic and context-dependent
- Number of states would exceed ~10
- You need fuzzy or probabilistic state
- Emergent behavior is desired
- Performance is critical (transitions are overhead)

---

## 11. Complexity Rating: 5/10

### Breakdown

| Aspect | Complexity (1-10) | Notes |
|--------|------------------|-------|
| State Definitions | 3/10 | Simple enum, clear meanings |
| Transition Logic | 4/10 | Explicit validation, straightforward |
| Thread Safety | 6/10 | AtomicReference adds complexity |
| Event Integration | 5/10 | EventBus pattern, moderate complexity |
| Foreman Extension | 7/10 | Hierarchical states add complexity |
| Overall | 5/10 | Moderate complexity, good trade-off |

### Complexity Factors

**Increases Complexity:**
- Adding new states (requires transition updates)
- Hierarchical state machines (foreman/worker)
- Concurrent state access
- Event-driven transitions

**Reduces Complexity:**
- Enum-based states (type-safe)
- Explicit transition validation
- AtomicReference (no locks)
- Clear logging

---

## 12. Usage Examples

### 12.1 Basic Usage

```java
// Create state machine
EventBus eventBus = new SimpleEventBus();
AgentStateMachine stateMachine = new AgentStateMachine(eventBus, "MineWright");

// Subscribe to events
eventBus.subscribe(StateTransitionEvent.class, event -> {
    System.out.println(event.getAgentId() + ": " + event.getFromState() + " -> " + event.getToState());
});

// Transition states
stateMachine.transitionTo(AgentState.PLANNING, "New command");
stateMachine.transitionTo(AgentState.EXECUTING, "Planning complete");
stateMachine.transitionTo(AgentState.COMPLETED, "Done!");

// Check state
if (stateMachine.getCurrentState() == AgentState.IDLE) {
    // Ready for new command
}

// Reset
stateMachine.reset();
```

### 12.2 Foreman Usage

```java
// Create foreman state machine
OrchestratorService orchestrator = new OrchestratorService();
ForemanStateMachine foremanSM = new ForemanStateMachine(eventBus, "ForemanMineWright", orchestrator);

// Process command
foremanSM.transitionTo(ForemanState.COORDINATING, "Human command received");
// ... decompose tasks
foremanSM.transitionTo(ForemanState.ASSIGNING, "Tasks created");
// ... assign to workers
foremanSM.transitionTo(ForemanState.WAITING, "Workers assigned");
// ... wait for completion
foremanSM.transitionTo(ForemanState.IDLE, "All done");
```

### 12.3 Error Handling

```java
// Attempt invalid transition
boolean success = stateMachine.transitionTo(AgentState.EXECUTING, "Skip planning");
// Returns false, logs warning: "Invalid state transition: IDLE -> EXECUTING"

// Force transition for recovery
stateMachine.forceTransition(AgentState.IDLE, "Recovery from corrupted state");

// Check if commands can be accepted
if (!stateMachine.canAcceptCommands()) {
    AgentState current = stateMachine.getCurrentState();
    System.out.println("Cannot accept commands in state: " + current);
}
```

---

## 13. Testing Strategies

### 13.1 Unit Tests

```java
@Test
public void testValidTransition() {
    AgentStateMachine sm = new AgentStateMachine(null, "test");
    assertTrue(sm.transitionTo(AgentState.PLANNING, "test"));
    assertEquals(AgentState.PLANNING, sm.getCurrentState());
}

@Test
public void testInvalidTransition() {
    AgentStateMachine sm = new AgentStateMachine(null, "test");
    assertFalse(sm.transitionTo(AgentState.EXECUTING, "skip planning"));
    assertEquals(AgentState.IDLE, sm.getCurrentState());
}

@Test
public void testTerminalStates() {
    AgentStateMachine sm = new AgentStateMachine(null, "test");
    sm.transitionTo(AgentState.PLANNING, "start");
    sm.transitionTo(AgentState.EXECUTING, "planned");
    sm.transitionTo(AgentState.COMPLETED, "done");

    assertTrue(sm.getCurrentState().isTerminal());
    assertTrue(sm.canAcceptCommands());
}
```

### 13.2 Integration Tests

```java
@Test
public void testStateEventPublishing() {
    EventBus eventBus = new SimpleEventBus();
    AgentStateMachine sm = new AgentStateMachine(eventBus, "test");

    List<StateTransitionEvent> events = new ArrayList<>();
    eventBus.subscribe(StateTransitionEvent.class, events::add);

    sm.transitionTo(AgentState.PLANNING, "test");

    assertEquals(1, events.size());
    assertEquals(AgentState.IDLE, events.get(0).getFromState());
    assertEquals(AgentState.PLANNING, events.get(0).getToState());
}
```

---

## 14. Related Files

| File | Purpose |
|------|---------|
| `src/main/java/com/minewright/execution/AgentState.java` | State enum definition |
| `src/main/java/com/minewright/execution/AgentStateMachine.java` | State machine implementation |
| `src/main/java/com/minewright/execution/ActionContext.java` | Context passed to actions |
| `src/main/java/com/minewright/action/ActionExecutor.java` | Uses state machine for command processing |
| `src/main/java/com/minewright/entity/ForemanEntity.java` | Entity that owns the state machine |
| `src/main/java/com/minewright/orchestration/OrchestratorService.java` | Foreman orchestration logic |
| `src/main/java/com/minewright/orchestration/AgentRole.java` | Agent role definitions (FOREMAN, CREW_MEMBER) |
| `src/main/java/com/minewright/event/StateTransitionEvent.java` | State transition event |

---

## 15. Conclusion

The state machine architecture provides a solid foundation for managing agent lifecycle in the MineWright AI foreman system. The explicit state transitions, thread safety, and event-driven design make it easy to understand, debug, and extend.

While the state machine approach has some rigidity, the benefits of predictability and clarity outweigh the drawbacks for this use case. The foreman's extended state machine demonstrates how the base pattern can be specialized for orchestration needs.

**Key Takeaways:**
- State machines excel when behavior is well-defined and modes are distinct
- Thread safety via AtomicReference avoids lock overhead
- Event integration enables reactive UI and monitoring
- Hierarchical states (foreman/worker) can extend the base pattern
- Complexity remains manageable (5/10) with good design

---

**Document End**
