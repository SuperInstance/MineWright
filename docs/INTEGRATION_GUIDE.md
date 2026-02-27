# MineWright Integration Guide

This guide explains how to extend MineWright through plugins, custom actions, and event handling.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Plugin Development](#plugin-development)
3. [Action Creation](#action-creation)
4. [Event Handling](#event-handling)
5. [Advanced Topics](#advanced-topics)
6. [Testing](#testing)
7. [Deployment](#deployment)

---

## Quick Start

### Project Setup

Create a new Maven/Gradle project with MineWright as a dependency:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.minewright</groupId>
    <artifactId>minewright-api</artifactId>
    <version>1.3.0</version>
    <scope>provided</scope>
</dependency>
```

### Basic Plugin Structure

```java
package com.example.myplugin;

import com.minewright.plugin.*;
import com.minewright.di.ServiceContainer;

public class MyPlugin implements ActionPlugin {
    @Override
    public String getPluginId() {
        return "my-plugin";
    }

    @Override
    public void onLoad(ActionRegistry registry, ServiceContainer container) {
        // Register actions here
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public String[] getDependencies() {
        return new String[0];
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "My MineWright plugin";
    }

    @Override
    public void onUnload() {
        // Cleanup
    }
}
```

---

## Plugin Development

### Understanding Plugins

Plugins use the **Service Provider Interface (SPI)** pattern. They are discovered automatically at runtime and can extend MineWright's functionality.

### Plugin Lifecycle

1. **Discovery** - Plugin discovered via SPI
2. **Dependency Resolution** - Dependencies loaded first
3. **Loading** - `onLoad()` called
4. **Active** - Plugin contributes to system
5. **Unloading** - `onUnload()` called

### Plugin Metadata

```java
public class MyPlugin implements ActionPlugin {

    @Override
    public String getPluginId() {
        // Unique identifier for this plugin
        // Use reverse domain notation: com.example.myplugin
        return "com.example.myplugin";
    }

    @Override
    public String getVersion() {
        // Semantic version: MAJOR.MINOR.PATCH
        return "1.0.0";
    }

    @Override
    public int getPriority() {
        // Higher = loaded first
        // Core actions: 1000
        // Normal plugins: 100
        // Overrides: 10
        return 100;
    }

    @Override
    public String[] getDependencies() {
        // Plugin IDs that must load before this one
        return new String[]{"core-actions"};
    }

    @Override
    public String getDescription() {
        return "Human-readable plugin description";
    }
}
```

### Registering Actions

```java
@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {

    // Simple action registration
    registry.register("myaction",
        (foreman, task, ctx) -> new MyAction(foreman, task)
    );

    // With priority and plugin ID
    registry.register("override_action",
        (foreman, task, ctx) -> new OverrideAction(foreman, task),
        1000,  // High priority overrides core
        getPluginId()
    );
}
```

### Accessing Services

```java
@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {

    // Register services for your actions to use
    container.register(MyService.class, new MyServiceImpl());

    // Get existing services
    EventBus eventBus = container.getService(EventBus.class);
    eventBus.subscribe(MyEvent.class, this::handleEvent);
}
```

### Plugin Registration File

Create `META-INF/services/com.minewright.plugin.ActionPlugin`:

```
com.example.myplugin.MyPlugin
com.example.anotherplugin.AnotherPlugin
```

---

## Action Creation

### Action Base Class

All actions extend `BaseAction`:

```java
package com.example.myplugin.actions;

import com.minewright.action.actions.BaseAction;
import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;

public class MyAction extends BaseAction {

    // Action state
    private int ticksRunning = 0;
    private boolean isComplete = false;

    public MyAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        // Called once when action starts
        // Validate parameters
        if (!validateParameters()) {
            result = ActionResult.failure("Invalid parameters");
            return;
        }

        // Initialize action
        ticksRunning = 0;
        isComplete = false;
    }

    @Override
    protected void onTick() {
        // Called every game tick
        // Do work here incrementally

        ticksRunning++;

        if (shouldComplete()) {
            result = ActionResult.success("Action completed!");
            isComplete = true;
        } else if (shouldFail()) {
            result = ActionResult.failure("Action failed");
            isComplete = true;
        } else if (ticksRunning > MAX_TICKS) {
            result = ActionResult.failure("Action timeout");
            isComplete = true;
        }
    }

    @Override
    protected void onCancel() {
        // Called when action is cancelled
        // Clean up resources
        cleanup();
    }

    @Override
    public String getDescription() {
        // Human-readable description
        return String.format("MyAction (running for %d ticks)", ticksRunning);
    }

    private boolean validateParameters() {
        // Check task parameters
        return task.hasRequiredParameters("param1", "param2");
    }

    private boolean shouldComplete() {
        // Check if action should complete
        return isComplete;
    }

    private boolean shouldFail() {
        // Check if action should fail
        return false;
    }

    private void cleanup() {
        // Clean up resources
    }

    private static final int MAX_TICKS = 6000; // 5 minutes
}
```

### Parameter Access

```java
@Override
protected void onStart() {
    // Get string parameter
    String name = task.getStringParameter("name", "default");

    // Get int parameter
    int count = task.getIntParameter("count", 10);

    // Get any parameter
    Object value = task.getParameter("key");

    // Check if parameters exist
    if (task.hasParameters("param1", "param2")) {
        // Both parameters present
    }
}
```

### World Interaction

```java
@Override
protected void onTick() {
    // Get agent position
    BlockPos pos = foreman.blockPosition();

    // Get block at position
    BlockState blockState = foreman.level().getBlockState(pos);
    Block block = blockState.getBlock();

    // Set block
    foreman.level().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);

    // Destroy block
    foreman.level().destroyBlock(pos, true);

    // Find nearest player
    Player player = foreman.level().getNearestPlayer(
        foreman, 10.0  // search radius
    );

    // Pathfinding
    foreman.getNavigation().moveTo(targetX, targetY, targetZ, 1.0);

    // Look at position
    foreman.getLookControl().setLookAt(
        targetX, targetY, targetZ
    );
}
```

### Result Handling

```java
// Success result
result = ActionResult.success("Completed successfully");

// Failure result (requires replanning)
result = ActionResult.failure("Failed, will retry");

// Failure result (no replanning)
result = ActionResult.failure(
    "Failed permanently",
    false  // doesn't require replanning
);
```

### Complex Action Example

```java
public class FarmAction extends BaseAction {

    private enum Phase { SCANNING, PLANTING, GROWING, HARVESTING }
    private Phase currentPhase = Phase.SCANNING;
    private List<BlockPos> farm plots;
    private int currentPlotIndex = 0;

    @Override
    protected void onStart() {
        String cropType = task.getStringParameter("crop", "wheat");
        int plotSize = task.getIntParameter("size", 5);

        farmPlots = findSuitableLocation(plotSize);
        if (farmPlots == null) {
            result = ActionResult.failure("Cannot find farm location");
            return;
        }

        prepareSoil();
    }

    @Override
    protected void onTick() {
        switch (currentPhase) {
            case SCANNING -> {
                if (scanComplete()) {
                    currentPhase = Phase.PLANTING;
                }
            }
            case PLANTING -> {
                plantNextCrop();
                if (allPlanted()) {
                    currentPhase = Phase.GROWING;
                }
            }
            case GROWING -> {
                if (cropsFullyGrown()) {
                    currentPhase = Phase.HARVESTING;
                }
            }
            case HARVESTING -> {
                harvestNextPlot();
                if (allHarvested()) {
                    result = ActionResult.success(
                        "Farm complete: " + farmPlots.size() + " crops"
                    );
                }
            }
        }
    }

    @Override
    protected void onCancel() {
        // Clear any partial state
        currentPhase = Phase.SCANNING;
    }

    @Override
    public String getDescription() {
        return String.format("Farming %s (%d/%d plots, phase: %s)",
            task.getStringParameter("crop"),
            currentPlotIndex,
            farmPlots.size(),
            currentPhase);
    }
}
```

---

## Event Handling

### Subscribing to Events

```java
public class MyEventHandler {

    private final EventBus eventBus;

    public MyEventHandler(EventBus eventBus) {
        this.eventBus = eventBus;
        registerSubscriptions();
    }

    private void registerSubscriptions() {

        // Subscribe to action started
        eventBus.subscribe(ActionStartedEvent.class, this::onActionStarted);

        // Subscribe with priority
        eventBus.subscribe(
            ActionCompletedEvent.class,
            this::onActionCompleted,
            100  // High priority
        );

        // Subscribe to all events
        eventBus.subscribe(Object.class, this::onAnyEvent);
    }

    private void onActionStarted(ActionStartedEvent event) {
        // Log action start
        logger.info("Action started: {} by agent {}",
            event.getActionName(),
            event.getAgentId());
    }

    private void onActionCompleted(ActionCompletedEvent event) {
        // Handle completion
        if (event.isSuccess()) {
            metrics.recordSuccess(event.getActionName(), event.getDurationMs());
        } else {
            metrics.recordFailure(event.getActionName(), event.getMessage());
        }
    }

    private void onAnyEvent(Object event) {
        // See all events for debugging
        logger.debug("Event: {}", event);
    }
}
```

### Publishing Custom Events

```java
public class MyCustomEvent {
    private final String agentId;
    private final String data;
    private final Instant timestamp;

    public MyCustomEvent(String agentId, String data) {
        this.agentId = agentId;
        this.data = data;
        this.timestamp = Instant.now();
    }

    // Getters...
}

// Publish event
eventBus.publish(new MyCustomEvent(
    foreman.getSteveName(),
    "Custom data"
));
```

### Event Filtering

```java
// Only handle specific actions
eventBus.subscribe(ActionStartedEvent.class, event -> {
    if ("mine".equals(event.getActionName())) {
        handleMiningAction(event);
    }
});

// Only handle failures
eventBus.subscribe(ActionCompletedEvent.class, event -> {
    if (!event.isSuccess()) {
        handleFailure(event);
    }
});
```

### Event Aggregation

```java
public class EventAggregator {

    private final List<ActionCompletedEvent> recentEvents = new ArrayList<>();
    private final EventBus eventBus;

    public EventAggregator(EventBus eventBus) {
        this.eventBus = eventBus;
        eventBus.subscribe(ActionCompletedEvent.class, this::onEvent);
    }

    private void onEvent(ActionCompletedEvent event) {
        recentEvents.add(event);

        if (recentEvents.size() >= 10) {
            reportMetrics();
            recentEvents.clear();
        }
    }

    private void reportMetrics() {
        long totalTime = recentEvents.stream()
            .mapToLong(ActionCompletedEvent::getDurationMs)
            .sum();

        long successCount = recentEvents.stream()
            .filter(ActionCompletedEvent::isSuccess)
            .count();

        logger.info("Last 10 actions: {} successful, total time: {}ms",
            successCount, totalTime);
    }
}
```

---

## Advanced Topics

### Dependency Injection

Use the `ServiceContainer` to manage dependencies:

```java
@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {

    // Register service
    container.register(MyService.class, new MyServiceImpl());

    // Register singleton
    container.registerSingleton(
        MyCache.class,
        new MyCache()
    );

    // Register factory
    container.registerFactory(
        MyRepository.class,
        () -> new MyRepositoryImpl(getConfig())
    );
}
```

Access services in actions:

```java
public class MyAction extends BaseAction {

    private final MyService service;

    public MyAction(ForemanEntity foreman, Task task, ActionContext context) {
        super(foreman, task);
        this.service = context.getService(MyService.class);
    }

    @Override
    protected void onTick() {
        // Use service
        service.doSomething();
    }
}
```

### Collaborative Actions

Support multi-agent collaboration:

```java
public class CollaborativeDigAction extends BaseAction {

    private static final Map<String, DigSite> activeSites = new ConcurrentHashMap<>();

    @Override
    protected void onStart() {
        String siteId = task.getStringParameter("site");
        DigSite site = activeSites.computeIfAbsent(siteId,
            id -> new DigSite(getLocation())
        );

        // Claim section
        DigSection section = site.claimSection();
        if (section == null) {
            result = ActionResult.failure("No sections available");
            return;
        }

        // Work on section
        digSection(section);
    }

    @Override
    protected void onTick() {
        // Continue digging
        if (sectionComplete()) {
            DigSite site = activeSites.get(siteId);
            site.completeSection(section);

            if (site.isComplete()) {
                activeSites.remove(siteId);
                result = ActionResult.success("Dig site complete");
            } else {
                result = ActionResult.success("Section complete");
            }
        }
    }
}
```

### Async Operations

Perform async work in actions:

```java
public class AsyncAction extends BaseAction {

    private CompletableFuture<Result> asyncOperation;

    @Override
    protected void onStart() {
        // Start async operation
        asyncOperation = CompletableFuture.supplyAsync(() -> {
            // Do heavy work
            return doHeavyWork();
        });
    }

    @Override
    protected void onTick() {
        // Check if complete
        if (asyncOperation.isDone()) {
            try {
                Result result = asyncOperation.get();
                this.result = ActionResult.success(result.toString());
            } catch (Exception e) {
                this.result = ActionResult.failure("Async failed: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onCancel() {
        if (asyncOperation != null) {
            asyncOperation.cancel(true);
        }
    }
}
```

### Code Execution

Use the code execution engine:

```java
public class ScriptedAction extends BaseAction {

    private final CodeExecutionEngine executor;
    private String script;

    @Override
    protected void onStart() {
        script = task.getStringParameter("script");

        // Execute script
        try {
            Object result = executor.execute(script,
                Map.of("foreman", foreman, "task", task));

            this.result = ActionResult.success("Script executed: " + result);
        } catch (Exception e) {
            this.result = ActionResult.failure("Script failed: " + e.getMessage());
        }
    }

    @Override
    protected void onTick() {
        // Script completed in onStart
    }

    @Override
    protected void onCancel() {
        // Scripts can't be cancelled once started
    }
}
```

---

## Testing

### Unit Testing Actions

```java
@Test
public void testMineAction() {
    // Setup
    ForemanEntity mockForeman = mock(ForemanEntity.class);
    Task task = new Task("mine",
        Map.of("block", "iron_ore", "quantity", 10));

    // Execute
    MineBlockAction action = new MineBlockAction(mockForeman, task);
    action.start();

    for (int i = 0; i < 100 && !action.isComplete(); i++) {
        action.tick();
    }

    // Verify
    ActionResult result = action.getResult();
    assertTrue(result.isSuccess());
    assertTrue(result.getMessage().contains("iron_ore"));
}
```

### Integration Testing Plugins

```java
@Test
public void testPluginLoading() {
    // Setup
    ActionRegistry registry = ActionRegistry.getInstance();
    ServiceContainer container = new SimpleServiceContainer();
    MyPlugin plugin = new MyPlugin();

    // Execute
    plugin.onLoad(registry, container);

    // Verify
    assertTrue(registry.hasAction("myaction"));
    assertEquals("my-plugin", registry.getPluginForAction("myaction"));
}
```

### Event Testing

```java
@Test
public void testEventPublishing() {
    // Setup
    EventBus eventBus = new SimpleEventBus();
    List<ActionStartedEvent> capturedEvents = new ArrayList<>();

    eventBus.subscribe(ActionStartedEvent.class, capturedEvents::add);

    // Execute
    eventBus.publish(new ActionStartedEvent(
        "agent-1", "mine", "Mining iron", Map.of()
    ));

    // Verify
    assertEquals(1, capturedEvents.size());
    assertEquals("mine", capturedEvents.get(0).getActionName());
}
```

---

## Deployment

### Building Your Plugin

```bash
# Build plugin JAR
./gradlew build

# Output in build/libs/
```

### Plugin JAR Structure

```
my-plugin.jar
├── META-INF/
│   └── services/
│       └── com.minewright.plugin.ActionPlugin
├── com/example/myplugin/
│   ├── MyPlugin.class
│   ├── actions/
│   │   └── MyAction.class
│   └── events/
│       └── MyEvent.class
└── dependencies/
    └── (if needed)
```

### Installation

1. Copy plugin JAR to `mods/` folder
2. Start Minecraft server/client
3. Plugin auto-discovers via SPI
4. Check logs for loading confirmation

### Distribution

```bash
# Create release
./gradlew release

# Outputs:
# - build/libs/my-plugin-1.0.0.jar
# - build/libs/my-plugin-1.0.0-sources.jar
# - build/libs/my-plugin-1.0.0-javadoc.jar
```

---

## Best Practices

### Action Design

1. **Keep actions focused** - Single responsibility
2. **Make them interruptible** - Always handle onCancel
3. **Use timeouts** - Prevent infinite actions
4. **Provide feedback** - Good descriptions and results
5. **Validate input** - Check parameters in onStart

### Plugin Design

1. **Document dependencies** - Clear dependency declaration
2. **Use appropriate priority** - Don't override core unnecessarily
3. **Clean up resources** - Implement onUnload
4. **Handle errors gracefully** - Don't crash the system
5. **Version properly** - Semantic versioning

### Event Handling

1. **Keep subscribers fast** - Don't block
2. **Handle exceptions** - Catch and log
3. **Unsubscribe when done** - Prevent memory leaks
4. **Use priorities wisely** - Higher = earlier
5. **Document event contracts** - Clear payload specs

---

## Troubleshooting

### Plugin Not Loading

**Problem**: Plugin doesn't appear in logs

**Solutions**:
- Check `META-INF/services/com.minewright.plugin.ActionPlugin` exists
- Verify class name is fully qualified
- Check for exceptions in logs
- Ensure no dependency conflicts

### Action Not Registered

**Problem**: Action not available to LLM

**Solutions**:
- Verify `registry.register()` called in `onLoad()`
- Check action name doesn't conflict (use priority)
- Update `PromptBuilder` to include action
- Check `hasAction()` returns true

### Events Not Firing

**Problem**: Subscribers not receiving events

**Solutions**:
- Verify `eventBus.publish()` called
- Check subscriber registered before publish
- Use `publishAsync()` for slow subscribers
- Check for exceptions in subscriber

### Actions Timing Out

**Problem**: Actions fail with timeout

**Solutions**:
- Increase `MAX_TICKS` constant
- Reduce work per action
- Split into multiple actions
- Optimize tick logic

---

## Resources

### API Reference

- [Action API](ACTION_API.md)
- [Event API](EVENT_API.md)
- [Configuration](CONFIGURATION.md)

### Examples

- [Example Plugins](../examples/plugins/)
- [Example Actions](../examples/actions/)
- [Integration Tests](../src/test/java/)

### Community

- GitHub Issues
- Discord Server
- Wiki
