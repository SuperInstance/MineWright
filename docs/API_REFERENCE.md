# Steve AI - API Reference

**Version:** 1.1.0
**Last Updated:** 2026-03-02
**Status:** Production Ready

---

## Table of Contents

1. [Action API](#action-api) - Creating custom actions
2. [LLM Client API](#llm-client-api) - Using different LLM providers
3. [Event Bus API](#event-bus-api) - Publishing and subscribing to events
4. [Memory API](#memory-api) - Storing and retrieving memories
5. [Skill Library API](#skill-library-api) - Registering and using skills
6. [Plugin System](#plugin-system) - Extension architecture

---

## Action API

The Action API enables creating custom actions that agents can execute. Actions follow a tick-based execution model (20 ticks per second) to prevent server blocking.

### Core Classes

#### BaseAction

Abstract base class for all actions. Extends this to create custom behaviors.

**Package:** `com.minewright.action.actions`

**Lifecycle Methods:**

```java
public abstract class BaseAction {
    // Called once when action starts - validate and initialize
    protected abstract void onStart();

    // Called each game tick (20 times per second) - implement your logic here
    protected abstract void onTick();

    // Called when action is cancelled - cleanup resources
    protected abstract void onCancel();

    // Human-readable description of this action
    public abstract String getDescription();
}
```

**Result Methods:**

```java
// Mark action as successful
protected void succeed(String message);

// Mark action as failed (may trigger replanning)
protected void fail(String message, boolean requiresReplanning);

// Mark action as failed with specific error code
protected void fail(ErrorCode errorCode, String message, boolean requiresReplanning);

// Mark action as failed with recovery suggestion
protected void failWithRecovery(String message, boolean requiresReplanning, String recoverySuggestion);

// Mark action as timed out
protected void failTimeout(String duration);

// Mark action as blocked
protected void failBlocked(String reason);
```

**Validation Methods:**

```java
// Validate required parameter exists
protected void requireParameter(String param, String paramName) throws ActionException;

// Validate foreman entity and level are available
protected void validateState() throws ActionException;
```

**State Methods:**

```java
// Check if action has completed
public boolean isComplete();

// Get the action result
public ActionResult getResult();

// Get the associated task
public Task getTask();

// Get retry count
public int getRetryCount();
```

#### Task

Encapsulates action parameters from the LLM.

**Package:** `com.minewright.action`

```java
public class Task {
    private final String action;              // Action type (e.g., "mine", "build")
    private final Map<String, Object> parameters;

    // Get the action type
    public String getAction();

    // Get all parameters
    public Map<String, Object> getParameters();

    // Get a specific parameter
    public Object getParameter(String key);

    // Get parameter as string
    public String getStringParameter(String key);
    public String getStringParameter(String key, String defaultValue);

    // Get parameter as integer
    public int getIntParameter(String key, int defaultValue);

    // Get parameter as boolean
    public boolean getBooleanParameter(String key, boolean defaultValue);

    // Get parameter as long
    public long getLongParameter(String key, long defaultValue);

    // Check if parameter exists
    public boolean hasParameter(String key);
    public boolean hasParameters(String... keys);
}
```

#### ActionResult

Represents the outcome of an action execution.

**Package:** `com.minewright.action`

```java
public class ActionResult {
    private final boolean success;
    private final ErrorCode errorCode;
    private final String message;
    private final boolean requiresReplanning;
    private final ActionException exception;
    private final String recoverySuggestion;
    private final long timestamp;

    // Check success
    public boolean isSuccess();

    // Get error details
    public ErrorCode getErrorCode();
    public String getMessage();
    public ActionException getException();

    // Check if replanning is needed
    public boolean requiresReplanning();

    // Get formatted message with recovery suggestion
    public String getFormattedMessage();

    // Static factory methods
    public static ActionResult success(String message);
    public static ActionResult failure(String message);
    public static ActionResult failure(ErrorCode errorCode, String message, boolean requiresReplanning);
    public static ActionResult failureWithRecovery(String message, boolean requiresReplanning, String recoverySuggestion);
    public static ActionResult timeout(String actionType, String duration);
    public static ActionResult blocked(String actionType, String reason);
}
```

**Error Codes:**

| Code | Description | Recovery Category |
|------|-------------|-------------------|
| `SUCCESS` | Action completed successfully | TRANSIENT |
| `UNKNOWN` | Uncategorized error | PERMANENT |
| `TIMEOUT` | Action timed out | TRANSIENT |
| `BLOCKED` | Blocked by obstacle | RECOVERABLE |
| `NOT_FOUND` | Required resource not found | PERMANENT |
| `INVALID_PARAMS` | Invalid action parameters | PERMANENT |
| `NAVIGATION_FAILURE` | Pathfinding failure | RECOVERABLE |
| `INVALID_ACTION_TYPE` | Unknown action type | PERMANENT |
| `EXECUTION_ERROR` | Runtime exception | RECOVERABLE |
| `RESOURCE_UNAVAILABLE` | Temporarily unavailable | TRANSIENT |
| `PERMISSION_DENIED` | Permission denied | PERMANENT |
| `INVALID_STATE` | State corruption | CRITICAL |

### Creating a Custom Action

**Example: Simple Mining Action**

```java
package com.minewright.action.actions;

public class CustomMineAction extends BaseAction {
    private final String blockType;
    private final int quantity;
    private int blocksMined = 0;

    public CustomMineAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.blockType = task.getStringParameter("block", "stone");
        this.quantity = task.getIntParameter("quantity", 1);
    }

    @Override
    protected void onStart() {
        validateState();
        requireParameter("block", "Block Type");

        LOGGER.info("[{}] Starting to mine {} blocks of {}",
            foreman.getEntityName(), quantity, blockType);
    }

    @Override
    protected void onTick() {
        // Check if complete
        if (blocksMined >= quantity) {
            succeed("Mined " + quantity + " " + blockType + " blocks");
            return;
        }

        // Mine one block per tick (simplified)
        if (mineBlock()) {
            blocksMined++;
        }

        // Check for timeout (30 seconds = 600 ticks)
        if (getRetryCount() > 600) {
            failTimeout("30 seconds");
        }

        retryCount++;
    }

    @Override
    protected void onCancel() {
        LOGGER.info("[{}] Mining cancelled at {}/{} blocks",
            foreman.getEntityName(), blocksMined, quantity);
    }

    @Override
    public String getDescription() {
        return "Mining " + quantity + " " + blockType;
    }

    private boolean mineBlock() {
        // Implementation: find and mine a block
        // Return true if successful
        return true;
    }
}
```

### Registering Custom Actions

**Using ActionRegistry:**

```java
ActionRegistry registry = ActionRegistry.getInstance();

// Register with lambda (preferred)
registry.register("custom_mine", (foreman, task, ctx) ->
    new CustomMineAction(foreman, task));

// Register with method reference
registry.register("custom_mine", CustomMineAction::new);

// Register with priority for conflict resolution
registry.register("custom_mine",
    (foreman, task, ctx) -> new CustomMineAction(foreman, task),
    100,  // priority (higher wins conflicts)
    "my-plugin"  // plugin ID
);
```

**Creating an ActionPlugin:**

```java
package com.example.minewright;

public class MyActionsPlugin implements ActionPlugin {
    @Override
    public String getPluginId() {
        return "my-actions";
    }

    @Override
    public void onLoad(ActionRegistry registry, ServiceContainer container) {
        // Register your actions here
        registry.register("custom_mine", CustomMineAction::new);
        registry.register("custom_build", CustomBuildAction::new);
        registry.register("custom_farm", CustomFarmAction::new);
    }

    @Override
    public void onUnload() {
        // Cleanup if needed
    }

    @Override
    public int getPriority() {
        return 100;  // Load before default plugins
    }

    @Override
    public String[] getDependencies() {
        return new String[] { "core-actions" };
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Custom actions for MineWright AI";
    }
}
```

**Register with SPI:**

Create file: `src/main/resources/META-INF/services/com.minewright.plugin.ActionPlugin`

```
com.example.minewright.MyActionsPlugin
```

### Common Patterns

**Pattern 1: Multi-Stage Actions**

```java
public class BuildHouseAction extends BaseAction {
    private enum Stage { FOUNDATION, WALLS, ROOF, FINISH }
    private Stage currentStage = Stage.FOUNDATION;

    @Override
    protected void onTick() {
        switch (currentStage) {
            case FOUNDATION -> buildFoundation();
            case WALLS -> buildWalls();
            case ROOF -> buildRoof();
            case FINISH -> succeed("House built!");
        }
    }

    private void buildFoundation() {
        // Build foundation
        if (foundationComplete) {
            currentStage = Stage.WALLS;
        }
    }
}
```

**Pattern 2: Timeout with Retry**

```java
@Override
protected void onTick() {
    int maxAttempts = 100;  // 5 seconds at 20 TPS
    int backoffTicks = 10;  // Wait between retries

    if (retryCount >= maxAttempts) {
        failTimeout("5 seconds");
        return;
    }

    if (retryCount % backoffTicks == 0) {
        // Attempt the operation
        if (tryOperation()) {
            succeed("Operation complete");
        }
    }

    retryCount++;
}
```

**Pattern 3: Progress Tracking**

```java
public class ComplexAction extends BaseAction {
    private final int totalSteps;
    private int completedSteps = 0;

    @Override
    public int getProgressPercent() {
        return (completedSteps * 100) / totalSteps;
    }

    @Override
    protected void onTick() {
        if (completedSteps < totalSteps) {
            doNextStep();
            completedSteps++;
        } else {
            succeed("Completed " + totalSteps + " steps");
        }
    }
}
```

---

## LLM Client API

The LLM Client API provides asynchronous access to multiple LLM providers with built-in resilience patterns.

### Core Interfaces

#### AsyncLLMClient

Interface for non-blocking LLM API calls.

**Package:** `com.minewright.llm.async`

```java
public interface AsyncLLMClient {
    /**
     * Sends an asynchronous request to the LLM provider.
     * Returns immediately with a CompletableFuture.
     */
    CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params);

    /**
     * Returns the provider ID (e.g., "openai", "groq", "gemini").
     */
    String getProviderId();

    /**
     * Checks if the client is healthy (circuit breaker not OPEN).
     */
    boolean isHealthy();
}
```

**LLMResponse:**

```java
public class LLMResponse {
    private final String content;       // Response text
    private final String providerId;    // Provider that generated it
    private final long latencyMs;       // Response time
    private final int tokensUsed;       // Tokens consumed
    private final boolean fromCache;    // Whether from cache

    public String getContent();
    public String getProviderId();
    public long getLatencyMs();
    public int getTokensUsed();
    public boolean isFromCache();
}
```

#### TaskPlanner

Converts natural language commands into structured tasks using LLMs.

**Package:** `com.minewright.llm`

```java
public class TaskPlanner {
    // Async planning (recommended)
    public CompletableFuture<ParsedResponse> planTasksAsync(ForemanEntity foreman, String command);

    // Async planning with batching control
    public CompletableFuture<ParsedResponse> planTasksAsync(ForemanEntity foreman, String command, boolean isUserInitiated);

    // Sync planning (blocking - not recommended)
    public ParsedResponse planTasks(ForemanEntity foreman, String command);

    // Cascade routing (intelligent model selection)
    public CompletableFuture<ParsedResponse> planTasksWithCascade(ForemanEntity foreman, String command);

    // Background task planning
    public CompletableFuture<String> submitBackgroundTask(ForemanEntity foreman, String taskDescription);

    // Task validation
    public boolean validateTask(Task task);
    public List<Task> validateAndFilterTasks(List<Task> tasks);

    // Health checks
    public boolean isProviderHealthy(String provider);
}
```

**ParsedResponse:**

```java
public class ParsedResponse {
    private final String plan;      // High-level description
    private final List<Task> tasks; // Structured task list

    public String getPlan();
    public List<Task> getTasks();
}
```

### Using Different Providers

**OpenAI (GPT-4, GPT-3.5):**

```java
// Configuration in config/minewright-common.toml:
[openai]
apiKey = "sk-..."
model = "gpt-4"

// Usage (automatic via TaskPlanner)
TaskPlanner planner = new TaskPlanner();
planner.planTasksAsync(foreman, "Build a house")
    .thenAccept(response -> {
        System.out.println("Plan: " + response.getPlan());
        System.out.println("Tasks: " + response.getTasks().size());
    });
```

**Groq (Llama models):**

```java
// Configuration
[groq]
apiKey = "gsk_..."
model = "llama-3.1-8b-instant"

// Usage (automatic fallback or explicit)
TaskPlanner planner = new TaskPlanner();
```

**Gemini (Google):**

```java
// Configuration
[gemini]
apiKey = "..."
model = "gemini-1.5-flash"

// Usage (automatic)
```

**z.ai (GLM models):**

```java
// Configuration
[zai]
apiKey = "..."
apiEndpoint = "https://api.z.ai/api/paas/v4/chat/completions"
foremanModel = "glm-5"
workerSimpleModel = "glm-4.7-air"
workerComplexModel = "glm-5"

// Usage (automatic with cascade routing)
```

### Cascade Routing

The cascade router automatically selects the best model based on task complexity.

**Enable Cascade Routing:**

```java
TaskPlanner planner = new TaskPlanner();
planner.setCascadeRoutingEnabled(true);
```

**Complexity Levels:**

| Complexity | Routing | Model Example |
|------------|---------|---------------|
| TRIVIAL | Cache (60-80% hit rate) | N/A |
| SIMPLE | FAST tier | Groq llama-3.1-8b-instant |
| MODERATE | BALANCED tier | Groq llama-3.3-70b |
| COMPLEX | SMART tier | GPT-4, Claude, GLM-5 |
| NOVEL | SMART tier | GPT-4, Claude, GLM-5 |

**Check Cascade Statistics:**

```java
planner.logCascadeStats();
planner.resetCascadeMetrics();
```

### Resilience Patterns

**Automatic Features:**
- Circuit breaker (stops calling failing APIs)
- Retry with exponential backoff
- Timeout (60 seconds maximum)
- Fallback to secondary provider
- Response caching (40-60% hit rate)

**Batching:**

```java
// Enable batching for rate limit management
planner.setBatchingEnabled(true);

// Submit background task (batched aggressively)
CompletableFuture<String> future = planner.submitBackgroundTask(foreman, "Explore the area");
```

### Common Patterns

**Pattern 1: Async Command Processing**

```java
ActionExecutor executor = foreman.getActionExecutor();

// Non-blocking command processing
executor.processNaturalLanguageCommand("Build a shelter");

// Check planning status
if (executor.isPlanning()) {
    // Planning in progress
}

// Get current goal
String goal = executor.getCurrentGoal();
```

**Pattern 2: Custom LLM Integration**

```java
public class CustomLLMClient implements AsyncLLMClient {
    @Override
    public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> {
            // Make HTTP request to your LLM
            String response = callYourAPI(prompt, params);

            return new LLMResponse(
                response,
                "custom",  // providerId
                System.currentTimeMillis() - startTime,
                estimateTokens(response),
                false  // fromCache
            );
        });
    }

    @Override
    public String getProviderId() {
        return "custom";
    }

    @Override
    public boolean isHealthy() {
        return true;  // Implement health check
    }
}
```

**Pattern 3: Prompt Building**

```java
// Build system prompt
String systemPrompt = PromptBuilder.buildSystemPrompt();

// Build user prompt with context
WorldKnowledge worldKnowledge = new WorldKnowledge(foreman);
String userPrompt = PromptBuilder.buildUserPrompt(foreman, command, worldKnowledge);

// Add custom context
String customPrompt = userPrompt + "\n\nAdditional context: " + customContext;
```

---

## Event Bus API

The Event Bus enables publish-subscribe messaging between components using the Observer pattern.

### Core Interface

#### EventBus

**Package:** `com.minewright.event`

```java
public interface EventBus {
    /**
     * Subscribe to events of a specific type.
     * Returns a subscription handle for unsubscribing.
     */
    <T> Subscription subscribe(Class<T> eventType, Consumer<T> subscriber);

    /**
     * Subscribe with priority (higher = called first).
     */
    <T> Subscription subscribe(Class<T> eventType, Consumer<T> subscriber, int priority);

    /**
     * Publish an event synchronously.
     */
    <T> void publish(T event);

    /**
     * Publish an event asynchronously.
     */
    <T> void publishAsync(T event);

    /**
     * Unsubscribe all subscribers for an event type.
     */
    void unsubscribeAll(Class<?> eventType);

    /**
     * Clear all subscriptions.
     */
    void clear();

    /**
     * Get subscriber count for an event type.
     */
    int getSubscriberCount(Class<?> eventType);
}
```

**Subscription Handle:**

```java
interface Subscription {
    void unsubscribe();   // Unsubscribe this specific subscription
    boolean isActive();   // Check if still active
}
```

### Built-in Events

**ActionStartedEvent:**

```java
public class ActionStartedEvent {
    private final String agentName;
    private final String actionType;
    private final String description;

    public ActionStartedEvent(String agentName, String actionType, String description);

    public String getAgentName();
    public String getActionType();
    public String getDescription();
}
```

**ActionCompletedEvent:**

```java
public class ActionCompletedEvent {
    private final String agentName;
    private final String actionType;
    private final boolean success;
    private final String message;

    public ActionCompletedEvent(String agentName, String actionType, boolean success, String message);

    public String getAgentName();
    public String getActionType();
    public boolean isSuccess();
    public String getMessage();
}
```

**StateTransitionEvent:**

```java
public class StateTransitionEvent {
    private final String agentName;
    private final AgentState fromState;
    private final AgentState toState;
    private final String reason;

    public StateTransitionEvent(String agentName, AgentState fromState, AgentState toState, String reason);

    public String getAgentName();
    public AgentState getFromState();
    public AgentState getToState();
    public String getReason();
}
```

### Usage Examples

**Basic Subscription:**

```java
EventBus eventBus = new SimpleEventBus();

// Subscribe to action start events
eventBus.subscribe(ActionStartedEvent.class, event -> {
    LOGGER.info("Action started: {} for agent {}",
        event.getActionType(), event.getAgentName());
});
```

**Priority Subscription:**

```java
// High priority subscriber (called first)
eventBus.subscribe(ActionStartedEvent.class, event -> {
    LOGGER.info("HIGH: Processing action start");
}, 100);

// Normal priority subscriber
eventBus.subscribe(ActionStartedEvent.class, event -> {
    LOGGER.info("NORMAL: Processing action start");
}, 0);
```

**Unsubscribe:**

```java
// Subscribe and keep handle
EventBus.Subscription sub = eventBus.subscribe(
    ActionCompletedEvent.class,
    this::handleActionComplete
);

// Later, unsubscribe
sub.unsubscribe();
```

**Publish Events:**

```java
// Synchronous publish (subscribers called on same thread)
eventBus.publish(new ActionStartedEvent("Steve", "mine", "Mining stone"));

// Asynchronous publish (subscribers called on background thread)
eventBus.publishAsync(new ActionCompletedEvent("Steve", "mine", true, "Mined 64 stone"));
```

**Access from ActionExecutor:**

```java
ActionExecutor executor = foreman.getActionExecutor();
EventBus eventBus = executor.getEventBus();

// Subscribe to events
eventBus.subscribe(ActionCompletedEvent.class, event -> {
    if (event.getAgentName().equals(foreman.getEntityName())) {
        // Handle my agent's action completion
    }
});
```

### Common Patterns

**Pattern 1: Cross-Component Communication**

```java
// Component A: Publish events
public class ActionExecutor {
    private final EventBus eventBus;

    public void executeTask(Task task) {
        eventBus.publish(new ActionStartedEvent(agentName, task.getAction(), "Starting task"));
        // ... execute task
        eventBus.publish(new ActionCompletedEvent(agentName, task.getAction(), true, "Complete"));
    }
}

// Component B: Subscribe to events
public class MetricsCollector {
    public void register(EventBus eventBus) {
        eventBus.subscribe(ActionCompletedEvent.class, event -> {
            if (event.isSuccess()) {
                recordSuccess(event.getActionType());
            } else {
                recordFailure(event.getActionType());
            }
        });
    }
}
```

**Pattern 2: Decoupled Coordination**

```java
// Agent A publishes completion
eventBus.publish(new ActionCompletedEvent("Agent1", "build", true, "Wall built"));

// Agent B subscribes and continues
eventBus.subscribe(ActionCompletedEvent.class, event -> {
    if ("Agent1".equals(event.getAgentName()) && "build".equals(event.getActionType())) {
        // Agent B can now start their task
        startNextPhase();
    }
});
```

**Pattern 3: Event Filtering**

```java
// Subscribe to specific agent's events
eventBus.subscribe(ActionCompletedEvent.class, event -> {
    if ("Steve".equals(event.getAgentName())) {
        // Only process Steve's events
        handleSteveEvent(event);
    }
});
```

---

## Memory API

The Memory API provides advanced memory systems for AI agents including episodic memory, semantic memory, and relationship tracking.

### Core Classes

#### CompanionMemory

Advanced memory system supporting relationship building and shared experiences.

**Package:** `com.minewright.memory`

```java
public class CompanionMemory {
    // === Memory Recording ===

    /**
     * Records a shared experience with the player.
     * @param eventType Type of event (build, explore, combat, etc.)
     * @param description What happened
     * @param emotionalWeight How memorable (-10 to +10)
     */
    public void recordExperience(String eventType, String description, int emotionalWeight);

    /**
     * Records a fact learned about the player.
     * @param category Category (preference, skill, habit, etc.)
     * @param key Fact key
     * @param value Fact value
     */
    public void learnPlayerFact(String category, String key, Object value);

    /**
     * Records an inside joke or memorable quote.
     */
    public void recordInsideJoke(String context, String punchline);

    /**
     * Adds to working memory for current context.
     */
    public void addToWorkingMemory(String type, String content);

    /**
     * Records player's playstyle observation.
     */
    public void recordPlaystyleMetric(String metricName, int delta);

    // === Memory Retrieval ===

    /**
     * Retrieves recent episodic memories.
     */
    public List<EpisodicMemory> getRecentMemories(int count);

    /**
     * Retrieves memories similar to the given context using semantic search.
     */
    public List<EpisodicMemory> findRelevantMemories(String query, int k);

    /**
     * Gets a player preference.
     */
    public <T> T getPlayerPreference(String key, T defaultValue);

    /**
     * Gets a random inside joke for reference.
     */
    public InsideJoke getRandomInsideJoke();

    /**
     * Gets working memory as context string.
     */
    public String getWorkingMemoryContext();

    /**
     * Builds relationship summary for prompting.
     */
    public String getRelationshipContext();

    /**
     * Builds optimized context string for LLM prompting with memory prioritization.
     */
    public String buildOptimizedContext(String query, int maxTokens);

    // === Relationship Management ===

    /**
     * Initializes the relationship on first meeting.
     */
    public void initializeRelationship(String playerName);

    /**
     * Adjusts rapport level.
     */
    public void adjustRapport(int delta);

    /**
     * Adjusts trust level.
     */
    public void adjustTrust(int delta);

    /**
     * Called when a shared task succeeds.
     */
    public void recordSharedSuccess(String taskDescription);

    /**
     * Called when a shared task fails.
     */
    public void recordSharedFailure(String taskDescription, String reason);

    // === Getters ===

    public int getRapportLevel();
    public int getTrustLevel();
    public String getPlayerName();
    public int getInteractionCount();
    public Instant getFirstMeeting();
    public Set<String> getSessionTopics();
    public int getInsideJokeCount();

    // === Milestone Tracking ===

    public MilestoneTracker getMilestoneTracker();
    public List<MilestoneTracker.Milestone> getMilestones();
    public boolean hasMilestone(String milestoneId);

    // === NBT Persistence ===

    public void saveToNBT(CompoundTag tag);
    public void loadFromNBT(CompoundTag tag);
}
```

**Memory Types:**

| Type | Description | Example |
|------|-------------|---------|
| Episodic | Specific events and experiences | "Built a house together on day 1" |
| Semantic | Facts about the player | "Prefers stone over wood" |
| Emotional | High-impact moments | "First time fighting a boss together" |
| Conversational | Topics discussed, inside jokes | "Inside joke: 'Not the bees!'" |
| Working | Recent context | "Currently building at (100, 64, 200)" |

**Inner Classes:**

```java
// Episodic memory of a specific event
public static class EpisodicMemory {
    public final String eventType;
    public final String description;
    public final int emotionalWeight;  // -10 to +10
    public final Instant timestamp;

    public void recordAccess();  // Increase importance
    public void setMilestone(boolean milestone);
    public boolean isProtected();  // Cannot be evicted
    public int getAccessCount();
    public Instant getLastAccessed();
    public String toContextString();
}

// Semantic memory (fact about the player)
public static class SemanticMemory {
    public final String category;
    public final String key;
    public final Object value;
    public final Instant learnedAt;
    public int confidence;  // 0-10
}

// Emotionally significant memory
public static class EmotionalMemory {
    public final String eventType;
    public final String description;
    public final int emotionalWeight;
    public final Instant timestamp;
}

// Inside joke or memorable phrase
public static class InsideJoke {
    public final String context;
    public final String punchline;
    public final Instant createdAt;
    public int referenceCount;
    public void incrementReference();
}

// Working memory entry
public static class WorkingMemoryEntry {
    public final String type;
    public final String content;
    public final Instant timestamp;
}
```

**Relationship State:**

```java
public static class Relationship {
    private final int rapport;  // 0-100
    private final int trust;    // 0-100
    private final Mood mood;

    public int getAffection();
    public int getTrust();
    public Mood getCurrentMood();
}

public enum Mood {
    CHEERFUL, FOCUSED, PLAYFUL, SERIOUS,
    EXCITED, CALM, TIRED, HAPPY
}
```

### Usage Examples

**Initialize Relationship:**

```java
CompanionMemory memory = foreman.getMemory();
memory.initializeRelationship("PlayerName");

// First meeting is automatically recorded as a milestone
```

**Record Experiences:**

```java
// Positive experience
memory.recordExperience("build", "Built a house together", 8);

// Negative experience
memory.recordExperience("death", "Fell in a hole and lost items", -5);

// Learning player preferences
memory.learnPlayerFact("preference", "block", "stone");
memory.learnPlayerFact("preference", "style", "modern");

// Recording playstyle
memory.recordPlaystyleMetric("aggression", 2);  // More aggressive
memory.recordPlaystyleMetric("exploration", 5);  // Very exploratory
```

**Retrieve Memories:**

```java
// Get recent memories
List<EpisodicMemory> recent = memory.getRecentMemories(10);

// Semantic search for relevant memories
List<EpisodicMemory> buildingMemories = memory.findRelevantMemories("building", 5);

// Get player preferences
String favoriteBlock = memory.getPlayerPreference("block", "cobblestone");

// Get relationship context
String relationshipInfo = memory.getRelationshipContext();
// Returns: "Relationship Status:
//   - Rapport Level: 75/100
//   - Trust Level: 60/100
//   - Interactions: 42
//   - Known for: 7 days
//   - Known preferences: [block, style]"

// Get working memory
String workingContext = memory.getWorkingMemoryContext();
```

**Track Relationships:**

```java
// Success strengthens bond
memory.recordSharedSuccess("Built a castle");
// Increases rapport by 2, trust by 3

// Failure is forgivable
memory.recordSharedFailure("Died in lava", "Didn't see the lava");
// Doesn't reduce rapport - "we're in this together"

// Adjust manually
memory.adjustRapport(5);   // Boost rapport
memory.adjustTrust(-2);   // Decrease trust
```

**Use Inside Jokes:**

```java
// Record an inside joke
memory.recordInsideJoke("Tried to build with dirt", "Dirt castle!");

// Retrieve for dialogue
InsideJoke joke = memory.getRandomInsideJoke();
if (joke != null) {
    String dialogue = "Remember when we " + joke.context + "? " + joke.punchline;
}
```

**Build Optimized Context for LLM:**

```java
// Get optimized context for prompting (respects token limits)
String context = memory.buildOptimizedContext("building a house", 2000);
// Includes:
// - Relationship status
// - High-emotion memories
// - Semantically relevant memories
// - Recent working memory
```

### Common Patterns

**Pattern 1: Progressive Relationship**

```java
// Check relationship milestones
if (memory.getRapportLevel() >= 50 && !memory.hasMilestone("friends")) {
    memory.recordExperience("milestone", "We've become friends!", 10);
}

if (memory.getInteractionCount() >= 100) {
    memory.recordExperience("milestone", "100 interactions together!", 8);
}
```

**Pattern 2: Contextual Dialogue**

```java
public String generateGreeting(CompanionMemory memory) {
    int rapport = memory.getRapportLevel();

    if (rapport < 20) {
        return "Hello there!";
    } else if (rapport < 50) {
        return "Hey, good to see you again!";
    } else if (rapport < 80) {
        return "Great to see you, friend!";
    } else {
        InsideJoke joke = memory.getRandomInsideJoke();
        if (joke != null) {
            return "Hey! " + joke.punchline;
        }
        return "Always wonderful to see you!";
    }
}
```

**Pattern 3: Memory Consolidation**

```java
// Get old memories for consolidation
List<EpisodicMemory> oldMemories = memory.getConsolidatableMemories(30);

// Summarize and remove
for (EpisodicMemory old : oldMemories) {
    String summary = summarize(old);
    memory.learnPlayerFact("summary", old.eventType, summary);
}

int removed = memory.removeMemories(oldMemories);
```

---

## Skill Library API

The Skill Library manages learned and built-in skills following the Voyager pattern.

### Core Classes

#### Skill

Interface for reusable skills that agents can learn and execute.

**Package:** `com.minewright.skill`

```java
public interface Skill {
    // Identity
    String getName();              // Unique skill identifier
    String getDescription();       // Human-readable description
    String getCategory();          // "mining", "building", "farming", etc.

    // Execution
    List<String> getRequiredActions();  // Actions needed to use this skill
    List<String> getRequiredItems();    // Items needed in inventory
    String generateCode(Map<String, Object> context);  // Generate executable code

    // Applicability
    boolean isApplicable(Task task);  // Check if skill can handle task

    // Statistics
    double getSuccessRate();       // 0.0 to 1.0
    int getExecutionCount();       // Number of times executed
    void recordSuccess(boolean success);  // Record outcome

    // Estimation
    int getEstimatedTicks();       // Estimated execution time
}
```

#### SkillLibrary

Central registry for managing learned and built-in skills.

**Package:** `com.minewright.skill`

```java
public class SkillLibrary {
    // Singleton access
    public static SkillLibrary getInstance();

    // === Skill Management ===

    /**
     * Adds a new skill to the library.
     * Returns false if duplicate exists.
     */
    public boolean addSkill(Skill skill);

    /**
     * Gets a skill by name.
     */
    public Skill getSkill(String name);

    /**
     * Removes a skill from the library.
     */
    public boolean removeSkill(String name);

    /**
     * Checks if a skill exists.
     */
    public boolean hasSkill(String name);

    // === Skill Discovery ===

    /**
     * Finds all skills applicable to the given task.
     * Returns sorted by success rate (highest first).
     */
    public List<Skill> findApplicableSkills(Task task);

    /**
     * Performs semantic search for skills matching a query.
     */
    public List<Skill> semanticSearch(String query);

    /**
     * Gets all skills in a specific category.
     */
    public List<Skill> getSkillsByCategory(String category);

    /**
     * Gets all skills sorted by success rate.
     */
    public List<Skill> getSkillsBySuccessRate();

    // === Statistics ===

    /**
     * Gets the total number of skills.
     */
    public int getSkillCount();

    /**
     * Gets statistics about the skill library.
     */
    public Map<String, Integer> getStatistics();

    // === Outcome Tracking ===

    /**
     * Records the outcome of a skill execution.
     */
    public void recordOutcome(String skillName, boolean success);
}
```

#### ExecutableSkill

Concrete implementation of Skill with builder pattern.

**Package:** `com.minewright.skill`

```java
public class ExecutableSkill implements Skill {
    // Builder for creating skills
    public static ExecutableSkillBuilder builder(String name);

    // Builder methods
    public ExecutableSkillBuilder description(String description);
    public ExecutableSkillBuilder category(String category);
    public ExecutableSkillBuilder codeTemplate(String template);
    public ExecutableSkillBuilder requiredActions(String... actions);
    public ExecutableSkillBuilder requiredItems(String... items);
    public ExecutableSkillBuilder estimatedTicks(int ticks);
    public ExecutableSkillBuilder applicabilityPattern(String regex);

    public ExecutableSkill build();
}
```

### Built-in Skills

| Name | Description | Category |
|------|-------------|----------|
| `digStaircase` | Dig a staircase downwards for safe mining | mining |
| `stripMine` | Execute strip mining at Y=-58 | mining |
| `branchMine` | Create branching tunnels from main shaft | mining |
| `buildShelter` | Build basic shelter for protection | building |
| `buildPlatform` | Build flat platform for building | building |
| `farmWheat` | Automated wheat farming | farming |
| `farmTree` | Plant saplings in grid pattern | farming |
| `organizeInventory` | Organize inventory by type | utility |
| `collectDrops` | Collect dropped items in area | utility |

### Usage Examples

**Create and Register a Custom Skill:**

```java
SkillLibrary library = SkillLibrary.getInstance();

// Create skill using builder
Skill customSkill = ExecutableSkill.builder("customSkill")
    .description("Description of what this skill does")
    .category("mining")
    .codeTemplate("""
        // JavaScript code template
        var quantity = {{quantity}};
        for (var i = 0; i < quantity; i++) {
            steve.mineBlock(startX + i, startY, startZ);
        }
        """)
    .requiredActions("mine", "pathfind")
    .requiredItems("pickaxe")
    .estimatedTicks(200)
    .applicabilityPattern("mine.*line|mining.*row")
    .build();

// Register in library
library.addSkill(customSkill);
```

**Find and Use Skills:**

```java
SkillLibrary library = SkillLibrary.getInstance();

// Find applicable skills for a task
Task task = new Task("mine", Map.of("block", "stone", "quantity", 64));
List<Skill> applicable = library.findApplicableSkills(task);

// Get best skill (highest success rate)
if (!applicable.isEmpty()) {
    Skill bestSkill = applicable.get(0);

    // Check applicability
    if (bestSkill.isApplicable(task)) {
        // Generate code
        Map<String, Object> context = new HashMap<>();
        context.put("quantity", 64);
        context.put("startX", 100);
        context.put("startY", 64);
        context.put("startZ", 200);

        String code = bestSkill.generateCode(context);

        // Execute code (via GraalVM)
        executeCode(code);

        // Record outcome
        library.recordOutcome(bestSkill.getName(), true);
    }
}
```

**Semantic Search:**

```java
SkillLibrary library = SkillLibrary.getInstance();

// Search for skills by description
List<Skill> miningSkills = library.semanticSearch("digging down for mining");
List<Skill> buildingSkills = library.semanticSearch("create shelter");
List<Skill> farmingSkills = library.semanticSearch("plant crops");

// Results sorted by relevance
for (Skill skill : miningSkills) {
    System.out.println(skill.getName() + ": " + skill.getDescription());
    System.out.println("Success rate: " + (skill.getSuccessRate() * 100) + "%");
}
```

**Track Statistics:**

```java
SkillLibrary library = SkillLibrary.getInstance();

// Get overall statistics
Map<String, Integer> stats = library.getStatistics();
System.out.println("Total skills: " + stats.get("total"));
System.out.println("Mining skills: " + stats.get("mining"));
System.out.println("Total executions: " + stats.get("totalExecutions"));

// Get skills by category
List<Skill> miningSkills = library.getSkillsByCategory("mining");

// Get top performing skills
List<Skill> topSkills = library.getSkillsBySuccessRate();
```

### Common Patterns

**Pattern 1: Dynamic Skill Discovery**

```java
public Skill findBestSkill(Task task) {
    SkillLibrary library = SkillLibrary.getInstance();

    // Find applicable skills
    List<Skill> applicable = library.findApplicableSkills(task);

    if (applicable.isEmpty()) {
        return null;  // No skill found
    }

    // Return best skill (highest success rate)
    return applicable.get(0);
}
```

**Pattern 2: Skill Auto-Generation**

```java
public void generateSkillFromSequence(String goal, List<Task> successfulSequence) {
    SkillGenerator generator = new SkillGenerator();

    // Extract pattern from successful task sequence
    ExecutableSkill skill = generator.generateFromTasks(goal, successfulSequence);

    // Validate and register
    if (skill != null) {
        SkillLibrary library = SkillLibrary.getInstance();
        if (library.addSkill(skill)) {
            LOGGER.info("Generated new skill: {}", skill.getName());
        }
    }
}
```

**Pattern 3: Skill Refinement**

```java
public void refineSkill(String skillName, boolean success) {
    SkillLibrary library = SkillLibrary.getInstance();

    // Record outcome
    library.recordOutcome(skillName, success);

    // Get skill
    Skill skill = library.getSkill(skillName);

    // Check if skill needs improvement
    if (skill.getSuccessRate() < 0.5 && skill.getExecutionCount() > 10) {
        // Success rate below 50% after 10 executions
        LOGGER.warn("Skill {} needs refinement (success rate: {:.2f}%)",
            skillName, skill.getSuccessRate() * 100);

        // Could trigger relearning or alternative approach
    }
}
```

---

## Plugin System

The plugin system enables extending MineWright AI with custom actions, skills, and behaviors.

### Core Interfaces

#### ActionPlugin

Service Provider Interface (SPI) for action plugins.

**Package:** `com.minewright.plugin`

```java
public interface ActionPlugin {
    // Identity
    String getPluginId();           // Unique identifier
    String getVersion();            // Semantic version (default: "1.0.0")
    String getDescription();        // Human-readable description

    // Lifecycle
    void onLoad(ActionRegistry registry, ServiceContainer container);
    void onUnload();               // Cleanup

    // Dependencies
    int getPriority();              // Loading order (higher = first)
    String[] getDependencies();     // Required plugins
}
```

#### ActionRegistry

Central registry for action factories using Registry Pattern.

**Package:** `com.minewright.plugin`

```java
public class ActionRegistry {
    // Singleton access
    public static ActionRegistry getInstance();

    // === Registration ===

    /**
     * Registers an action factory with default priority.
     */
    void register(String actionName, ActionFactory factory);

    /**
     * Registers with priority and plugin ID.
     */
    void register(String actionName, ActionFactory factory, int priority, String pluginId);

    /**
     * Unregisters an action.
     */
    boolean unregister(String actionName);

    /**
     * Clears all registered actions.
     */
    void clear();

    // === Action Creation ===

    /**
     * Creates an action instance using the registered factory.
     */
    BaseAction createAction(String actionName, ForemanEntity foreman, Task task, ActionContext context);

    // === Query ===

    /**
     * Checks if an action is registered.
     */
    boolean hasAction(String actionName);

    /**
     * Returns all registered action names.
     */
    Set<String> getRegisteredActions();

    /**
     * Returns the number of registered actions.
     */
    int getActionCount();

    /**
     * Returns which plugin registered a specific action.
     */
    String getPluginForAction(String actionName);

    /**
     * Returns comma-separated list of action names.
     */
    String getActionsAsList();
}
```

#### ActionFactory

Functional interface for creating action instances.

**Package:** `com.minewright.plugin`

```java
@FunctionalInterface
public interface ActionFactory {
    /**
     * Creates a new action instance.
     */
    BaseAction create(ForemanEntity foreman, Task task, ActionContext context);
}
```

#### ActionContext

Provides dependencies and services to actions.

**Package:** `com.minewright.execution`

```java
public class ActionContext {
    private final ServiceContainer serviceContainer;
    private final EventBus eventBus;
    private final AgentStateMachine stateMachine;
    private final InterceptorChain interceptorChain;

    // Builder
    public static ActionContextBuilder builder();

    // Getters
    public ServiceContainer getServiceContainer();
    public EventBus getEventBus();
    public AgentStateMachine getStateMachine();
    public InterceptorChain getInterceptorChain();
}
```

### Creating a Plugin

**Step 1: Implement ActionPlugin**

```java
package com.example.minewright;

import com.minewright.plugin.*;
import com.minewright.di.ServiceContainer;
import com.minewright.action.actions.BaseAction;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.execution.ActionContext;

public class MyCustomPlugin implements ActionPlugin {
    @Override
    public String getPluginId() {
        return "my-custom-plugin";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Custom actions for special behaviors";
    }

    @Override
    public int getPriority() {
        return 100;  // Load before default plugins
    }

    @Override
    public String[] getDependencies() {
        return new String[] { "core-actions" };
    }

    @Override
    public void onLoad(ActionRegistry registry, ServiceContainer container) {
        // Register simple action (lambda)
        registry.register("dance", (foreman, task, ctx) ->
            new DanceAction(foreman, task), 0, "my-custom-plugin");

        // Register action with dependencies (lambda)
        registry.register("smart_build", (foreman, task, ctx) -> {
            LLMCache cache = ctx.getServiceContainer().getService(LLMCache.class);
            return new SmartBuildAction(foreman, task, cache);
        }, 0, "my-custom-plugin");

        // Register with method reference
        registry.register("custom_mine", CustomMineAction::new, 0, "my-custom-plugin");
    }

    @Override
    public void onUnload() {
        // Cleanup resources if needed
        System.out.println("MyCustomPlugin unloaded");
    }
}
```

**Step 2: Create Custom Action**

```java
package com.example.minewright;

public class DanceAction extends BaseAction {
    private int danceTicks = 0;

    public DanceAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        validateState();
        LOGGER.info("[{}] Starting to dance!", foreman.getEntityName());
    }

    @Override
    protected void onTick() {
        // Dance for 5 seconds (100 ticks)
        if (danceTicks >= 100) {
            succeed("Dance complete!");
            return;
        }

        // Spin around
        foreman.setYRot(foreman.getYRot() + 45);
        danceTicks++;
    }

    @Override
    protected void onCancel() {
        LOGGER.info("[{}] Dance cancelled", foreman.getEntityName());
    }

    @Override
    public String getDescription() {
        return "Dancing";
    }
}
```

**Step 3: Register with SPI**

Create file: `src/main/resources/META-INF/services/com.minewright.plugin.ActionPlugin`

```
com.example.minewright.MyCustomPlugin
```

### Advanced Plugin Features

**Dependency Injection:**

```java
@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {
    // Get services from container
    LLMCache cache = container.getService(LLMCache.class);
    EventBus eventBus = container.getService(EventBus.class);

    // Use services in action factories
    registry.register("cached_action", (foreman, task, ctx) -> {
        return new CachedAction(foreman, task, cache);
    });
}
```

**Conflict Resolution:**

```java
// Register with high priority to override other plugins
registry.register("mine",
    (foreman, task, ctx) -> new ImprovedMineAction(foreman, task),
    1000,  // High priority
    "my-override-plugin"
);
```

**Event Bus Integration:**

```java
@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {
    // Get event bus
    EventBus eventBus = container.getService(EventBus.class);

    // Subscribe to events
    eventBus.subscribe(ActionCompletedEvent.class, event -> {
        if ("mine".equals(event.getActionType())) {
            // Track mining statistics
            trackMining(event);
        }
    });

    // Register actions
    registry.register("tracked_mine", TrackedMineAction::new);
}
```

**Plugin Dependencies:**

```java
@Override
public String[] getDependencies() {
    // Ensure core-actions is loaded first
    return new String[] { "core-actions" };
}
```

### Plugin Best Practices

1. **Use Unique IDs:** Ensure your plugin ID is unique across all plugins
2. **Register in onLoad:** All action registration should happen in `onLoad()`
3. **Clean Up in onUnload:** Release resources in `onUnload()`
4. **Set Appropriate Priority:** Higher priority plugins load first
5. **Declare Dependencies:** Use `getDependencies()` for required plugins
6. **Version Your Plugin:** Use semantic versioning
7. **Provide Descriptions:** Help users understand what your plugin does
8. **Handle Errors Gracefully:** Catch exceptions in action factories
9. **Use Thread-Safe Patterns:** Plugins may be called from multiple threads
10. **Test Thoroughly:** Test with various game states and edge cases

### Example: Complete Plugin

```java
package com.example.minewright;

public class CompletePlugin implements ActionPlugin {
    private EventBus eventBus;
    private MetricsCollector metrics;

    @Override
    public String getPluginId() {
        return "complete-example";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Complete example plugin with all features";
    }

    @Override
    public int getPriority() {
        return 500;  // High priority
    }

    @Override
    public String[] getDependencies() {
        return new String[] { "core-actions" };
    }

    @Override
    public void onLoad(ActionRegistry registry, ServiceContainer container) {
        // Get services
        this.eventBus = container.getService(EventBus.class);
        this.metrics = new MetricsCollector(eventBus);

        // Register actions
        registry.register("action1", Action1::new, 100, "complete-example");
        registry.register("action2", (foreman, task, ctx) ->
            new Action2(foreman, task, metrics), 100, "complete-example");

        // Subscribe to events
        eventBus.subscribe(ActionCompletedEvent.class, this::handleActionComplete);
    }

    @Override
    public void onUnload() {
        // Cleanup
        if (metrics != null) {
            metrics.shutdown();
        }
    }

    private void handleActionComplete(ActionCompletedEvent event) {
        metrics.record(event.getActionType(), event.isSuccess());
    }
}
```

---

## Appendix

### Common Parameters

**Task Parameters:**

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `block` | String | Block type | `"stone"`, `"oak_log"` |
| `quantity` | Integer | Number of items/blocks | `64`, `10` |
| `x`, `y`, `z` | Integer | Coordinates | `100`, `64`, `-200` |
| `target` | String | Entity or block target | `"zombie"`, `"chest"` |
| `player` | String | Player name | `"Steve"` |
| `direction` | String | Cardinal direction | `"north"`, `"up"` |
| `item` | String | Item name | `"diamond_pickaxe"` |
| `structure` | String | Structure type | `"house"`, `"bridge"` |

### Error Recovery Categories

| Category | Description | Example |
|----------|-------------|---------|
| TRANSIENT | Temporary failures, retry succeeds | Timeout, rate limit |
| RECOVERABLE | Can be recovered with action | Blocked path, out of range |
| PERMANENT | Cannot be recovered, needs replanning | Invalid params, unknown action |
| CRITICAL | System state corruption | Invalid state |

### Thread Safety Guidelines

1. **Actions:** Ticked on main game thread only
2. **EventBus:** Thread-safe for publish/subscribe
3. **SkillLibrary:** Thread-safe for concurrent access
4. **CompanionMemory:** Thread-safe memory operations
5. **TaskPlanner:** Thread-safe async planning
6. **ActionRegistry:** Thread-safe registration/creation

### Performance Considerations

1. **Tick Budget:** Actions must complete within 5ms per tick
2. **Async Operations:** Use CompletableFuture for long tasks
3. **Caching:** Enable LLM response caching for 40-60% reduction
4. **Batching:** Use batching for rate limit management
5. **Memory:** Limit episodic memories to 200 entries

### Further Reading

- [CLAUDE.md](../CLAUDE.md) - Project overview and architecture
- [TEST_COVERAGE.md](TEST_COVERAGE.md) - Testing guidelines
- [RESEARCH_DOCUMENTS](../docs/research/) - Game automation research
- [GITHUB_REPO](https://github.com/your-repo) - Source code

---

**Document Version:** 1.1.0
**Last Updated:** 2026-03-02
**Maintained By:** Steve AI Development Team
