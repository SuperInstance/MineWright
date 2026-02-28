# Integration Architecture for MineWright
## Component Integration Patterns and System Design

**Date:** 2026-02-26
**Project:** MineWright - "We don't give you agents. We give you a Foreman."
**Version:** 1.0.0
**Focus:** Unified architecture for all AI components

---

## Executive Summary

This document defines the integration architecture that connects all MineWright components (LLM, memory, orchestration, companion features, action system, and event infrastructure) into a cohesive, maintainable system.

**Current State:** MineWright has excellent individual components with modern patterns (DI container, event bus, state machine, plugin architecture, async execution).

**Integration Goal:** Create a service layer that:
- Provides unified access to all AI capabilities
- Manages component lifecycle and dependencies
- Enables clean testing and mocking
- Supports hot-reloading and world-specific configuration
- Handles errors gracefully with fallback strategies

**Key Insight:** The existing `SimpleServiceContainer` DI system provides the foundation. We extend it with a service registry, lifecycle management, and configuration layer.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Component Inventory](#2-component-inventory)
3. [Service Layer Architecture](#3-service-layer-architecture)
4. [Dependency Injection Strategy](#4-dependency-injection-strategy)
5. [Event-Driven Integration](#5-event-driven-integration)
6. [State Management](#6-state-management)
7. [Configuration Management](#7-configuration-management)
8. [Error Handling & Recovery](#8-error-handling--recovery)
9. [Component Lifecycle](#9-component-lifecycle)
10. [Testing Strategy](#10-testing-strategy)
11. [Implementation Roadmap](#11-implementation-roadmap)
12. [Implementation Checklist](#12-implementation-checklist)

---

## 1. System Overview

### 1.1 Architecture Vision

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       MineWright Integration Layer                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                 │
│  │   Client     │    │    Crew      │    │   Commands   │                 │
│  │  Interface   │    │   Entities   │    │   & Config   │                 │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘                 │
│         │                    │                    │                         │
│         └────────────────────┴────────────────────┘                         │
│                              │                                              │
│                              ▼                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                        Service Facade Layer                           │ │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐      │ │
│  │  │ AIService  │  │ Memory     │  │ Companion  │  │ Orchest-   │      │ │
│  │  │            │  │ Service    │  │ Service    │  │ ration      │      │ │
│  │  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘      │ │
│  └────────┼───────────────┼───────────────┼───────────────┼──────────────┘ │
│           │               │               │               │                │
│  ┌────────┴───────────────────┴───────────────┴───────────────┴────────┐   │
│  │                     Service Container (DI)                          │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐       │   │
│  │  │  LLM    │ │  Event  │ │  State  │ │ Plugin  │ │ Config  │       │   │
│  │  │ Service │ │   Bus   │ │ Machine │ │ Manager │ │ Manager │       │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘       │   │
│  └────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                     Core Infrastructure                                │ │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐      │ │
│  │  │ Thread     │  │ Cache      │  │ Persistence│  │ Monitoring  │      │ │
│  │  │ Pools      │  │ Layer      │  │ Layer      │  │ & Metrics   │      │ │
│  │  └────────────┘  └────────────┘  └────────────┘  └────────────┘      │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Design Principles

1. **Service Orientation:** All capabilities exposed through service interfaces
2. **Dependency Injection:** No direct instantiation, use `ServiceContainer`
3. **Event-Driven:** Components communicate via `EventBus`, not direct calls
4. **State Isolation:** World-specific vs global state clearly separated
5. **Fail-Safe:** Graceful degradation when components fail
6. **Testability:** All services mockable via interfaces
7. **Configuration-Driven:** Behavior controlled by config, not code

---

## 2. Component Inventory

### 2.1 Core Services

| Service | Interface | Implementation | Scope | Dependencies |
|---------|-----------|----------------|-------|--------------|
| **AIService** | `AIService` | `AIServiceImpl` | Global | LLMClient, Cache, EventBus |
| **MemoryService** | `MemoryService` | `MemoryServiceImpl` | Per-Entity | EventBus, Persistence |
| **CompanionService** | `CompanionService` | `CompanionServiceImpl` | Per-Entity | MemoryService, AIService |
| **OrchestrationService** | `OrchestrationService` | `OrchestrationServiceImpl` | Global | EventBus, AIService |
| **ActionExecutor** | `ActionExecutor` | `ActionExecutor` | Per-Entity | ActionRegistry, EventBus |
| **TaskPlanner** | `TaskPlanner` | `TaskPlanner` | Per-Entity | AIService, PromptBuilder |

### 2.2 Infrastructure Services

| Service | Interface | Implementation | Scope | Purpose |
|---------|-----------|----------------|-------|---------|
| **EventBus** | `EventBus` | `SimpleEventBus` | Global | Event pub/sub |
| **LLMCache** | `LLMCache` | `CaffeineCache` | Global | Response caching |
| **StateStore** | `StateStore` | `WorldStateStore` | Per-World | State persistence |
| **ConfigManager** | `ConfigManager` | `ConfigManagerImpl` | Per-World | Config loading |
| **MetricsCollector** | `MetricsCollector` | `MetricsCollectorImpl` | Global | Telemetry |

### 2.3 Existing Components to Integrate

**LLM Layer:**
- `OpenAIClient` - HTTP client for LLM API
- `AsyncLLMClient` - Non-blocking wrapper
- `PromptBuilder` - Prompt construction
- `ResponseParser` - Response parsing

**Memory Layer:**
- `ForemanMemory` - Simple action history
- `CompanionMemory` - Relationship tracking
- `WorldKnowledge` - Entity/block snapshots

**Execution Layer:**
- `ActionExecutor` - Tick-based execution
- `ActionRegistry` - Plugin system
- `AgentStateMachine` - State management
- `InterceptorChain` - Cross-cutting concerns

**Orchestration:**
- `OrchestratorService` - Multi-agent coordination
- `AgentCommunicationBus` - Inter-agent messaging
- `CollaborativeBuildManager` - Parallel building

---

## 3. Service Layer Architecture

### 3.1 Service Facade Pattern

The service layer provides unified, high-level interfaces to AI capabilities.

```java
package com.minewright.service;

/**
 * Main AI service facade.
 *
 * <p>Provides unified access to all AI capabilities including planning,
 * execution, memory, and companion features. This is the primary entry
 * point for AI functionality.</p>
 *
 * <p><b>Design Pattern:</b> Facade Pattern</p>
 * <p><b>Scope:</b> Global singleton (one per Minecraft server)</p>
 *
 * @since 1.0.0
 */
public interface AIService {

    /**
     * Submits a natural language command for execution.
     *
     * <p>This is the primary method for interacting with MineWright.
     * The service will plan, validate, and execute the command.</p>
     *
     * @param entityName Target crew member entity
     * @param command Natural language command
     * @return CompletableFuture containing execution result
     */
    CompletableFuture<ExecutionResult> executeCommand(
        String entityName,
        String command
    );

    /**
     * Plans a command without executing (preview mode).
     *
     * @param entityName Target crew member entity
     * @param command Natural language command
     * @return Planned tasks
     */
    CompletableFuture<List<Task>> planCommand(
        String entityName,
        String command
    );

    /**
     * Gets the memory service for a specific crew member.
     *
     * @param entityName Crew member entity name
     * @return Memory service instance
     */
    MemoryService getMemoryService(String entityName);

    /**
     * Gets the companion service for a specific crew member.
     *
     * @param entityName Crew member entity name
     * @return Companion service instance
     */
    CompanionService getCompanionService(String entityName);

    /**
     * Gets global orchestration service.
     *
     * @return Orchestration service
     */
    OrchestrationService getOrchestrationService();

    /**
     * Shuts down all AI services gracefully.
     */
    void shutdown();
}
```

### 3.2 Service Registry

The service registry manages service lifecycle and dependencies.

```java
package com.minewwright.service;

/**
 * Central registry for all AI services.
 *
 * <p>Manages service initialization, dependency resolution, and lifecycle.
 * Services are registered with metadata about their scope and dependencies.</p>
 *
 * <p><b>Scopes:</b></p>
 * <ul>
 *   <li><b>GLOBAL:</b> Single instance for entire server</li>
 *   <li><b>WORLD:</b> One instance per world/dimension</li>
 *   <li><b>ENTITY:</b> One instance per crew member entity</li>
 * </ul>
 *
 * @since 1.2.0
 */
public interface ServiceRegistry {

    /**
     * Registers a service with metadata.
     *
     * @param serviceClass Service interface class
     * @param factory Factory to create instances
     * @param scope Service scope
     * @param dependencies Required services
     * @param <T> Service type
     */
    <T> void registerService(
        Class<T> serviceClass,
        ServiceFactory<T> factory,
        ServiceScope scope,
        Class<?>... dependencies
    );

    /**
     * Gets a service instance.
     *
     * @param serviceClass Service interface class
     * @param scopeKey Scope key (world ID, entity ID, etc.)
     * @param <T> Service type
     * @return Service instance
     * @throws ServiceNotAvailableException if service not found
     */
    <T> T getService(Class<T> serviceClass, String scopeKey);

    /**
     * Initializes all services in dependency order.
     */
    void initializeAll();

    /**
     * Shuts down all services in reverse dependency order.
     */
    void shutdownAll();

    /**
     * Service scope enum.
     */
    enum ServiceScope {
        /** Single instance for server */
        GLOBAL,
        /** One instance per world */
        WORLD,
        /** One instance per crew member entity */
        ENTITY
    }

    /**
     * Factory for creating service instances.
     */
    @FunctionalInterface
    interface ServiceFactory<T> {
        T create(ServiceContainer container);
    }
}
```

### 3.3 Service Implementation

```java
package com.minewwright.service.impl;

/**
 * Implementation of AIService facade.
 *
 * <p>Delegates to specialized services (memory, companion, orchestration)
 * while providing unified API and lifecycle management.</p>
 */
public class AIServiceImpl implements AIService {

    private final ServiceContainer container;
    private final Map<String, MemoryService> memoryServices;
    private final Map<String, CompanionService> companionServices;
    private final OrchestrationService orchestrationService;
    private final EventBus eventBus;

    public AIServiceImpl(ServiceContainer container) {
        this.container = container;
        this.memoryServices = new ConcurrentHashMap<>();
        this.companionServices = new ConcurrentHashMap<>();
        this.orchestrationService = container.getService(OrchestrationService.class);
        this.eventBus = container.getService(EventBus.class);

        // Listen for entity creation/removal
        eventBus.subscribe(CrewMemberCreatedEvent.class, this::onCrewMemberCreated);
        eventBus.subscribe(CrewMemberRemovedEvent.class, this::onCrewMemberRemoved);
    }

    @Override
    public CompletableFuture<ExecutionResult> executeCommand(
        String entityName,
        String command
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // 1. Get or create services for this crew member
            MemoryService memory = getMemoryService(entityName);
            CompanionService companion = getCompanionService(entityName);

            // 2. Publish command event
            eventBus.publish(new CommandReceivedEvent(entityName, command));

            // 3. Plan tasks
            List<Task> tasks = planCommandSync(entityName, command, memory);

            // 4. Validate with companion context
            ValidationResult validation = companion.validateTasks(tasks);
            if (!validation.isValid()) {
                return ExecutionResult.rejected(validation.getReason());
            }

            // 5. Execute via ActionExecutor
            ActionExecutor executor = container.getService(ActionExecutor.class);
            return executor.executeTasks(entityName, tasks);

        }, container.getService(LLMExecutorService.class).getExecutor("default"));
    }

    @Override
    public MemoryService getMemoryService(String entityName) {
        return memoryServices.computeIfAbsent(entityName, name ->
            new MemoryServiceImpl(container, name)
        );
    }

    @Override
    public CompanionService getCompanionService(String entityName) {
        return companionServices.computeIfAbsent(entityName, name ->
            new CompanionServiceImpl(container, name)
        );
    }

    private void onCrewMemberCreated(CrewMemberCreatedEvent event) {
        // Initialize services when crew member spawns
        getMemoryService(event.getEntityName());
        getCompanionService(event.getEntityName());
    }

    private void onCrewMemberRemoved(CrewMemberRemovedEvent event) {
        // Cleanup services when crew member despawns
        memoryServices.remove(event.getEntityName());
        companionServices.remove(event.getEntityName());
    }
}
```

---

## 4. Dependency Injection Strategy

### 4.1 Enhanced Service Container

The existing `SimpleServiceContainer` is extended with lifecycle support.

```java
package com.minewwright.di;

/**
 * Enhanced service container with lifecycle management.
 *
 * <p>Extends ServiceContainer with:</p>
 * <ul>
 *   <li>Initialization callbacks</li>
 *   <li>Shutdown hooks</li>
 *   <li>Dependency tracking</li>
 *   <li>Scope management</li>
 * </ul>
 */
public class LifecycleServiceContainer implements ServiceContainer {

    private final Map<Class<?>, ServiceEntry> services = new ConcurrentHashMap<>();
    private final Map<String, ServiceEntry> namedServices = new ConcurrentHashMap<>();
    private final List<LifecycleListener> lifecycleListeners = new CopyOnWriteArrayList<>();

    @Override
    public <T> void register(Class<T> serviceType, T instance) {
        register(serviceType, instance, ServiceScope.GLOBAL);
    }

    public <T> void register(
        Class<T> serviceType,
        T instance,
        ServiceScope scope
    ) {
        ServiceEntry entry = new ServiceEntry(
            serviceType,
            instance,
            scope,
            Thread.currentThread().getStackTrace()
        );

        services.put(serviceType, entry);

        // Auto-initialize if implements Initializable
        if (instance instanceof Initializable init) {
            try {
                init.initialize();
                notifyInitialized(serviceType);
            } catch (Exception e) {
                LOGGER.error("Failed to initialize service: {}", serviceType.getName(), e);
            }
        }
    }

    @Override
    public <T> T getService(Class<T> serviceType) {
        ServiceEntry entry = services.get(serviceType);
        if (entry == null) {
            throw new ServiceNotFoundException(serviceType);
        }
        return serviceType.cast(entry.instance());
    }

    @Override
    public boolean unregister(Class<?> serviceType) {
        ServiceEntry entry = services.remove(serviceType);
        if (entry != null) {
            // Shutdown if implements Shutdownable
            if (entry.instance() instanceof Shutdownable shutdown) {
                shutdown.shutdown();
                notifyShutdown(serviceType);
            }
            return true;
        }
        return false;
    }

    /**
     * Shuts down all services in reverse registration order.
     */
    public void shutdownAll() {
        List<Class<?>> reversed = new ArrayList<>(services.keySet());
        Collections.reverse(reversed);

        for (Class<?> serviceType : reversed) {
            unregister(serviceType);
        }
    }

    /**
     * Service lifecycle interface.
     */
    public interface Initializable {
        void initialize() throws Exception;
    }

    /**
     * Service shutdown interface.
     */
    public interface Shutdownable {
        void shutdown();
    }

    /**
     * Lifecycle listener interface.
     */
    public interface LifecycleListener {
        void onServiceInitialized(Class<?> serviceType);
        void onServiceShutdown(Class<?> serviceType);
    }

    private record ServiceEntry(
        Class<?> type,
        Object instance,
        ServiceScope scope,
        StackTraceInfo registrationSource
    ) {}

    public enum ServiceScope {
        GLOBAL, WORLD, ENTITY
    }
}
```

### 4.2 Service Initialization Order

Critical initialization sequence to avoid dependency issues:

```
1. Infrastructure Layer (GLOBAL)
   ├─ EventBus
   ├─ MetricsCollector
   ├─ ConfigManager
   └─ LLMCache

2. Core Services (GLOBAL)
   ├─ LLMClient (depends: EventBus, Cache)
   ├─ LLMExecutorService (depends: none)
   ├─ StateStore (depends: EventBus)
   └─ PluginManager (depends: EventBus)

3. High-Level Services (GLOBAL)
   ├─ AIService (depends: LLMClient, EventBus)
   ├─ OrchestrationService (depends: AIService, EventBus)
   └─ ActionRegistry (depends: EventBus)

4. Entity-Level Services (ENTITY, created on demand)
   ├─ MemoryService (depends: EventBus, StateStore)
   ├─ CompanionService (depends: MemoryService, AIService)
   └─ ActionExecutor (depends: ActionRegistry, EventBus)
```

### 4.3 Constructor Injection Example

```java
/**
 * Example service with constructor injection.
 */
public class CompanionServiceImpl implements CompanionService {

    private final MemoryService memoryService;
    private final AIService aiService;
    private final EventBus eventBus;
    private final MineWrightConfig config;

    /**
     * Constructor injection ensures all dependencies are available.
     *
     * @param container Service container for dependency resolution
     * @param entityName Target crew member entity name
     */
    public CompanionServiceImpl(
        ServiceContainer container,
        String entityName
    ) {
        this.memoryService = container.getService(MemoryService.class);
        this.aiService = container.getService(AIService.class);
        this.eventBus = container.getService(EventBus.class);
        this.config = container.getService(MineWrightConfig.class);

        // Register event handlers
        eventBus.subscribe(InteractionEvent.class, this::onInteraction);

        LOGGER.info("CompanionService initialized for: {}", entityName);
    }

    // ... service methods
}
```

---

## 5. Event-Driven Integration

### 5.1 Event Taxonomy

All events flow through the `EventBus` for loose coupling.

**Core Events:**
```
┌─────────────────────────────────────────────────────────────┐
│                    Event Categories                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Lifecycle Events                                           │
│  ├─ ServiceInitializedEvent                                │
│  ├─ ServiceShutdownEvent                                   │
│  ├─ WorldLoadedEvent                                       │
│  └─ WorldUnloadedEvent                                     │
│                                                              │
│  Entity Events                                              │
│  ├─ CrewMemberCreatedEvent                                │
│  ├─ CrewMemberRemovedEvent                                │
│  ├─ EntityStateChangedEvent                                │
│  └─ EntityTeleportEvent                                    │
│                                                              │
│  Command Events                                             │
│  ├─ CommandReceivedEvent                                   │
│  ├─ CommandValidatedEvent                                  │
│  ├─ CommandExecutionStartedEvent                           │
│  ├─ CommandExecutionCompletedEvent                         │
│  └─ CommandExecutionFailedEvent                            │
│                                                              │
│  Action Events                                              │
│  ├─ ActionStartedEvent                                     │
│  ├─ ActionCompletedEvent                                   │
│  ├─ ActionFailedEvent                                      │
│  └─ ActionCancelledEvent                                   │
│                                                              │
│  Memory Events                                              │
│  ├─ MemoryStoredEvent                                      │
│  ├─ MemoryRetrievedEvent                                   │
│  ├─ RelationshipUpdatedEvent                               │
│  └─ MemoryCompactedEvent                                   │
│                                                              │
│  Companion Events                                           │
│  ├─ InteractionEvent                                       │
│  ├─ MoodChangedEvent                                       │
│  ├─ RapportUpdatedEvent                                    │
│  └─ InsideJokeCreatedEvent                                 │
│                                                              │
│  Orchestration Events                                       │
│  ├─ AgentAssignmentEvent                                   │
│  ├─ AgentCoordinationEvent                                 │
│  └─ MultiAgentTaskStartedEvent                             │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 Event Flow Diagrams

**Command Execution Flow:**
```
Player Input (GUI or Chat)
         │
         ▼
CommandReceivedEvent
         │
         ├─► TaskPlanner.planTasks()
         │        │
         │        ▼
         │   PlanningCompleteEvent
         │        │
         ▼        │
CommandValidatedEvent ◄──┘
         │
         ├─► ActionExecutor.execute()
         │        │
         │        ▼
         │   ActionStartedEvent
         │        │
         │        ▼
         │   [Tick-based execution]
         │        │
         │        ▼
         │   ActionCompletedEvent
         │        │
         ▼        │
CommandExecutionCompletedEvent ◄──┘
         │
         ▼
MemoryUpdatedEvent
```

**Memory Update Flow:**
```
Event Occurs (e.g., successful build)
         │
         ▼
MemoryService.recordExperience()
         │
         ├─► Generate embedding
         │        │
         │        ▼
         │   Store in vector database
         │        │
         ▼        │
MemoryStoredEvent
         │
         ├─► Update relationship metrics
         │        │
         │        ▼
         │   RelationshipUpdatedEvent
         │        │
         ▼        │
MemoryCompactedEvent (if needed)
```

### 5.3 Event Bus Integration

```java
package com.minewwright.integration;

/**
 * Integrates EventBus with service lifecycle.
 */
public class EventIntegrator {

    private final EventBus eventBus;
    private final ServiceContainer container;

    public EventIntegrator(ServiceContainer container) {
        this.container = container;
        this.eventBus = container.getService(EventBus.class);

        // Register core event handlers
        registerLifecycleHandlers();
        registerEntityHandlers();
        registerCommandHandlers();
        registerMemoryHandlers();
    }

    private void registerLifecycleHandlers() {
        // Service lifecycle
        eventBus.subscribe(ServiceInitializedEvent.class, event -> {
            LOGGER.info("Service initialized: {}", event.getServiceType());
        });

        eventBus.subscribe(ServiceShutdownEvent.class, event -> {
            LOGGER.info("Service shut down: {}", event.getServiceType());
        });

        // World lifecycle
        eventBus.subscribe(WorldLoadedEvent.class, event -> {
            onWorldLoaded(event.getWorldId());
        });

        eventBus.subscribe(WorldUnloadedEvent.class, event -> {
            onWorldUnloaded(event.getWorldId());
        });
    }

    private void registerEntityHandlers() {
        eventBus.subscribe(CrewMemberCreatedEvent.class, event -> {
            AIService aiService = container.getService(AIService.class);
            aiService.getMemoryService(event.getEntityName());
            aiService.getCompanionService(event.getEntityName());
        });

        eventBus.subscribe(CrewMemberRemovedEvent.class, event -> {
            // Cleanup entity-scoped services
            cleanupEntityServices(event.getEntityName());
        });
    }

    private void onWorldLoaded(ResourceKey<Level> worldId) {
        // Initialize world-scoped services
        WorldStateStore stateStore = container.getService(WorldStateStore.class);
        stateStore.initializeWorld(worldId);
    }

    private void onWorldUnloaded(ResourceKey<Level> worldId) {
        // Persist world state
        WorldStateStore stateStore = container.getService(WorldStateStore.class);
        stateStore.saveWorld(worldId);

        // Clean up world-scoped services
        cleanupWorldServices(worldId);
    }
}
```

---

## 6. State Management

### 6.1 State Isolation Strategy

**Global State (Server-level):**
- ServiceContainer instances
- EventBus subscriptions
- LLM Cache
- OrchestrationService

**World State (Dimension-level):**
- WorldStateStore per dimension
- World-specific configuration overrides
- Per-world agent registries

**Entity State (Crew Member-level):**
- MemoryService instances
- CompanionService instances
- AgentStateMachine instances
- ActionExecutor queues

### 6.2 State Synchronization

```java
package com.minewwright.state;

/**
 * Manages state synchronization across scopes.
 */
public class StateSynchronizer {

    private final Map<ResourceKey<Level>, WorldState> worldStates;
    private final Map<String, EntityState> entityStates;
    private final EventBus eventBus;

    /**
     * Synchronizes entity state to persistent storage.
     *
     * @param entityName Entity name
     */
    public void syncEntityState(String entityName) {
        EntityState state = entityStates.get(entityName);
        if (state != null && state.isDirty()) {
            // Publish sync event
            eventBus.publish(new EntitySyncEvent(entityName, state));

            // Persist to world storage
            WorldState worldState = getWorldState(state.getWorldId());
            worldState.saveEntityState(entityName, state);

            state.markClean();
        }
    }

    /**
     * Loads entity state from persistent storage.
     *
     * @param entityName Entity name
     * @param worldId World identifier
     * @return Loaded state
     */
    public EntityState loadEntityState(String entityName, ResourceKey<Level> worldId) {
        WorldState worldState = getWorldState(worldId);
        EntityState state = worldState.loadEntityState(entityName);

        if (state == null) {
            // New entity, create initial state
            state = new EntityState(entityName, worldId);
        }

        entityStates.put(entityName, state);
        return state;
    }

    /**
     * Handles world save event.
     */
    @Subscribe
    public void onWorldSave(WorldSaveEvent event) {
        WorldState worldState = worldStates.get(event.getWorldId());
        if (worldState != null) {
            worldState.saveAll();

            // Sync all entities in this world
            entityStates.entrySet().stream()
                .filter(e -> e.getValue().getWorldId().equals(event.getWorldId()))
                .forEach(e -> syncEntityState(e.getKey()));
        }
    }
}
```

### 6.3 State Machine Integration

```java
package com.minewwright.integration;

/**
 * Integrates AgentStateMachine with service layer.
 */
public class StateMachineIntegrator {

    private final Map<String, AgentStateMachine> stateMachines;
    private final EventBus eventBus;

    /**
     * Gets or creates state machine for entity.
     */
    public AgentStateMachine getStateMachine(String entityName) {
        return stateMachines.computeIfAbsent(entityName, name -> {
            AgentStateMachine machine = new AgentStateMachine(name);

            // Subscribe to state changes
            machine.addStateChangeListener((oldState, newState) -> {
                eventBus.publish(new StateChangedEvent(name, oldState, newState));
            });

            return machine;
        });
    }

    /**
     * Handles state change events.
     */
    @Subscribe
    public void onStateChanged(StateChangedEvent event) {
        LOGGER.info("Entity {} state: {} -> {}",
            event.getEntityName(), event.getOldState(), event.getNewState());

        // Sync state to memory
        MemoryService memory = container.getService(MemoryService.class);
        memory.recordStateChange(
            event.getEntityName(),
            event.getNewState().toString()
        );
    }
}
```

---

## 7. Configuration Management

### 7.1 Configuration Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  Configuration Layer                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              ConfigManager                            │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐    │  │
│  │  │ Global     │  │ World      │  │ Entity      │    │  │
│  │  │ Config     │  │ Config     │  │ Config      │    │  │
│  │  │ (minewright-│  │ (world_    │  │ (entity_    │    │  │
│  │  │  common)   │  │  data.dat) │  │  name.dat)  │    │  │
│  │  └────────────┘  └────────────┘  └────────────┘    │  │
│  └──────────────────────────────────────────────────────┘  │
│                           │                                  │
│                           ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Config Sources                           │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐    │  │
│  │  │ Forge      │  │ JSON       │  │ Runtime     │    │  │
│  │  │ Config     │  │ Overrides  │  │ Overrides   │    │  │
│  │  └────────────┘  └────────────┘  └────────────┘    │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 Configuration Manager Implementation

```java
package com.minewwright.config;

/**
 * Manages hierarchical configuration with hot-reloading.
 */
public class ConfigManager implements Initializable, Shutdownable {

    private final EventBus eventBus;
    private final Map<String, ConfigLayer> configLayers;
    private final WatchService watchService;
    private final Path configDir;

    private GlobalConfig globalConfig;
    private final Map<ResourceKey<Level>, WorldConfig> worldConfigs = new ConcurrentHashMap<>();
    private final Map<String, EntityConfig> entityConfigs = new ConcurrentHashMap<>();

    @Override
    public void initialize() throws Exception {
        // Load global config
        globalConfig = loadGlobalConfig();

        // Setup file watching for hot-reload
        setupConfigWatcher();

        LOGGER.info("ConfigManager initialized");
    }

    /**
     * Gets configuration for entity with layer merging.
     *
     * <p>Priority: Entity > World > Global</p>
     */
    public <T> T getConfig(String entityName, ResourceKey<Level> worldId, ConfigKey<T> key) {
        // Check entity config first
        EntityConfig entityConfig = entityConfigs.get(entityName);
        if (entityConfig != null && entityConfig.has(key)) {
            return entityConfig.get(key);
        }

        // Check world config
        WorldConfig worldConfig = worldConfigs.get(worldId);
        if (worldConfig != null && worldConfig.has(key)) {
            return worldConfig.get(key);
        }

        // Fall back to global config
        return globalConfig.get(key);
    }

    /**
     * Sets entity-specific config override.
     */
    public <T> void setEntityConfig(
        String entityName,
        ConfigKey<T> key,
        T value
    ) {
        EntityConfig config = entityConfigs.computeIfAbsent(
            entityName,
            name -> new EntityConfig(name)
        );
        config.set(key, value);

        // Publish config change event
        eventBus.publish(new ConfigChangedEvent(entityName, key, value));

        // Persist
        saveEntityConfig(entityName, config);
    }

    private void setupConfigWatcher() throws Exception {
        configDir = Paths.get("./config/minewwright");
        Files.createDirectories(configDir);

        watchService = FileSystems.getDefault().newWatchService();
        configDir.register(watchService,
            StandardWatchEventKinds.ENTRY_MODIFY);

        // Start watcher thread
        Thread watcherThread = new Thread(this::watchConfigDirectory);
        watcherThread.setDaemon(true);
        watcherThread.setName("ConfigWatcher");
        watcherThread.start();
    }

    private void watchConfigDirectory() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changedFile = configDir.resolve((Path) event.context());
                    onConfigFileChanged(changedFile);
                }
                key.reset();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ClosedWatchServiceException e) {
            // Shutdown
        }
    }

    private void onConfigFileChanged(Path file) {
        String filename = file.getFileName().toString();

        if (filename.equals("minewwright-common.toml")) {
            reloadGlobalConfig();
        } else if (filename.startsWith("world_")) {
            // Reload world config
            ResourceKey<Level> worldId = extractWorldId(filename);
            reloadWorldConfig(worldId);
        } else if (filename.startsWith("entity_")) {
            // Reload entity config
            String entityName = extractEntityName(filename);
            reloadEntityConfig(entityName);
        }

        // Publish reload event
        eventBus.publish(new ConfigReloadedEvent(file));
    }

    @Override
    public void shutdown() {
        try {
            watchService.close();
        } catch (IOException e) {
            LOGGER.error("Error closing config watch service", e);
        }
    }
}
```

### 7.3 Configuration Schema

**Global Config (minewwright-common.toml):**
```toml
[ai]
provider = "groq"  # openai, groq, gemini
maxTokens = 8000
temperature = 0.7

[openai]
apiKey = "sk-..."
model = "glm-5"
baseURL = "https://api.z.ai/v1"

[groq]
apiKey = "gsk_..."
model = "llama3-70b-8192"

[gemini]
apiKey = "..."
model = "gemini-pro"

[behavior]
actionTickDelay = 20
enableChatResponses = true
maxActiveCrewMembers = 10

[memory]
maxWorkingMemory = 50
enableEpisodicMemory = true
enableSemanticMemory = true

[companion]
enablePersonality = true
enableRelationshipTracking = true
defaultPersonality = "friendly"

[orchestration]
enableMultiAgent = true
maxConcurrentAgents = 5
enableCollaborativeBuilding = true
```

**World Config (world_overworld.json):**
```json
{
  "worldId": "overworld",
  "overrides": {
    "ai.temperature": 0.5,
    "behavior.enableChatResponses": false,
    "orchestration.maxConcurrentAgents": 3
  },
  "agentDefaults": {
    "personality": "professional"
  }
}
```

**Entity Config (entity_Foreman.json):**
```json
{
  "entityName": "Foreman",
  "personality": "enthusiastic",
  "overrides": {
    "ai.temperature": 0.9,
    "memory.maxWorkingMemory": 100
  },
  "relationship": {
    "initialRapport": 50
  }
}
```

---

## 8. Error Handling & Recovery

### 8.1 Error Handling Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                  Error Handling Layer                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Try Successful                                              │
│       │                                                      │
│       ▼                                                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Primary Operation                        │  │
│  └──────────────────────────────────────────────────────┘  │
│       │                                                      │
│       │ Exception                                          │
│       ▼                                                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Error Categorization                     │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐          │  │
│  │  │Transient │  │Recoverable│  │Permanent │          │  │
│  │  │          │  │          │  │          │          │  │
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘          │  │
│  └───────┼─────────────┼─────────────┼──────────────────┘  │
│          │             │             │                      │
│          ▼             ▼             ▼                      │
│  ┌──────────┐  ┌──────────────┐  ┌────────────┐         │
│  │  Retry   │  │ Replan/      │  │ Fallback  │         │
│  │with Backoff│ │ Adjust      │  │ Handler   │         │
│  └─────┬────┘  └──────┬───────┘  └─────┬──────┘         │
│        │               │                │                  │
│        └───────────────┴────────────────┘                  │
│                       │                                     │
│                       ▼                                     │
│         ┌───────────────────────────────┐                  │
│         │  Result or Degraded Service   │                  │
│         └───────────────────────────────┘                  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 Error Recovery Implementation

```java
package com.minewwright.recovery;

/**
 * Centralized error recovery coordinator.
 */
public class ErrorRecoveryCoordinator {

    private final Map<Class<? extends Throwable>, RecoveryStrategy> strategies;
    private final EventBus eventBus;
    private final ServiceContainer container;

    /**
     * Attempts to recover from an error.
     *
     * @param error The error that occurred
     * @param context Error context
     * @return Recovery result
     */
    public RecoveryResult recover(Throwable error, ErrorContext context) {
        // Categorize error
        ErrorCategory category = categorizeError(error);

        // Publish error event
        eventBus.publish(new ErrorOccurredEvent(error, category, context));

        // Get recovery strategy
        RecoveryStrategy strategy = strategies.get(error.getClass());
        if (strategy == null) {
            strategy = strategies.get(category);
        }

        if (strategy != null) {
            try {
                return strategy.recover(error, context);
            } catch (Exception e) {
                LOGGER.error("Recovery strategy failed", e);
            }
        }

        // No recovery available
        return RecoveryResult.failed(error);
    }

    private ErrorCategory categorizeError(Throwable error) {
        if (error instanceof IOException || error instanceof TimeoutException) {
            return ErrorCategory.TRANSIENT;
        } else if (error instanceof IllegalArgumentException) {
            return ErrorCategory.RECOVERABLE;
        } else if (error instanceof OutOfMemoryError) {
            return ErrorCategory.PERMANENT;
        }
        return ErrorCategory.UNKNOWN;
    }

    public enum ErrorCategory {
        TRANSIENT,    // Retry with backoff
        RECOVERABLE,  // Replan or adjust
        PERMANENT,    // Fallback or fail
        UNKNOWN       // Default to recoverable
    }
}

/**
 * Recovery strategy interface.
 */
@FunctionalInterface
public interface RecoveryStrategy {
    RecoveryResult recover(Throwable error, ErrorContext context);
}

/**
 * Error context providing information for recovery.
 */
public record ErrorContext(
    String operation,
    String entityName,
    Map<String, Object> parameters,
    Instant timestamp
) {}
```

### 8.3 Graceful Degradation

```java
package com.minewwright.recovery;

/**
 * Fallback implementations for degraded service.
 */
public class DegradedServiceFallback {

    /**
     * Creates degraded LLM service using cached responses.
     */
    public static AIService createDegradedLLMService(
        ServiceContainer container
    ) {
        return new AIService() {
            private final LLMCache cache = container.getService(LLMCache.class);

            @Override
            public CompletableFuture<String> complete(String prompt) {
                // Try cache first
                String cached = cache.get(prompt);
                if (cached != null) {
                    return CompletableFuture.completedFuture(cached);
                }

                // Return degraded response
                return CompletableFuture.completedFuture(
                    "I'm experiencing technical difficulties. " +
                    "Please try again in a moment."
                );
            }
        };
    }

    /**
     * Creates degraded memory service using in-memory only.
     */
    public static MemoryService createDegradedMemoryService(
        ServiceContainer container
    ) {
        return new MemoryService() {
            private final Map<String, String> memory = new ConcurrentHashMap<>();

            @Override
            public void remember(String key, String value) {
                memory.put(key, value);
            }

            @Override
            public Optional<String> recall(String key) {
                return Optional.ofNullable(memory.get(key));
            }

            // Persistence is disabled
        };
    }
}
```

---

## 9. Component Lifecycle

### 9.1 Lifecycle States

```
┌─────────┐    ┌──────────┐    ┌─────────┐    ┌──────────┐    ┌─────────┐
│ CREATED │───►│INITIALIZ│───►│ RUNNING │───►│SHUTTING │───►│ TERMIN.│
└─────────┘    └──────────┘    └─────────┘    └──────────┘    └─────────┘
                    │                                │
                    │                                │
                    ▼                                ▼
               ┌──────────┐                    ┌──────────┐
               │  FAILED  │                    │  ERROR   │
               └──────────┘                    └──────────┘
```

### 9.2 Lifecycle Manager

```java
package com.minewwright.lifecycle;

/**
 * Manages lifecycle of all AI components.
 */
public class AILifecycleManager {

    private final List<LifecycleComponent> components;
    private LifecycleState state = LifecycleState.CREATED;

    public void initialize() {
        if (state != LifecycleState.CREATED) {
            throw new IllegalStateException("Cannot initialize from state: " + state);
        }

        LOGGER.info("Initializing AI components...");

        for (LifecycleComponent component : components) {
            try {
                component.initialize();
                LOGGER.debug("Initialized: {}", component.getName());
            } catch (Exception e) {
                LOGGER.error("Failed to initialize: {}", component.getName(), e);
                state = LifecycleState.FAILED;
                throw new ComponentInitializationException(
                    "Failed to initialize: " + component.getName(), e
                );
            }
        }

        state = LifecycleState.RUNNING;
        LOGGER.info("All AI components initialized successfully");
    }

    public void shutdown() {
        if (state != LifecycleState.RUNNING) {
            return;
        }

        LOGGER.info("Shutting down AI components...");
        state = LifecycleState.SHUTTING_DOWN;

        // Shutdown in reverse order
        List<LifecycleComponent> reversed = new ArrayList<>(components);
        Collections.reverse(reversed);

        for (LifecycleComponent component : reversed) {
            try {
                component.shutdown();
                LOGGER.debug("Shut down: {}", component.getName());
            } catch (Exception e) {
                LOGGER.error("Error shutting down: {}", component.getName(), e);
            }
        }

        state = LifecycleState.TERMINATED;
        LOGGER.info("All AI components shut down");
    }

    public enum LifecycleState {
        CREATED, INITIALIZING, RUNNING, SHUTTING_DOWN, TERMINATED, FAILED, ERROR
    }
}
```

---

## 10. Testing Strategy

### 10.1 Testing Pyramid

```
                    ┌─────────────┐
                   │             │
                  │ Integration │
                 │   Tests      │
                │ (10% coverage)│
               └───────────────────┘
              ┌───────────────────────┐
             │                         │
            │     Component Tests      │
           │      (30% coverage)        │
          └───────────────────────────────┘
         ┌───────────────────────────────────┐
        │                                     │
       │          Unit Tests                  │
      │         (60% coverage)                │
     └─────────────────────────────────────────┘
```

### 10.2 Test Infrastructure

```java
package com.minewwright.testing;

/**
 * Test fixture for AI component testing.
 */
public class AITestFixture {

    private final ServiceContainer testContainer;
    private final MockEventBus mockEventBus;
    private final MockLLMClient mockLLMClient;

    public AITestFixture() {
        this.testContainer = new LifecycleServiceContainer();
        this.mockEventBus = new MockEventBus();
        this.mockLLMClient = new MockLLMClient();

        // Register mocks
        testContainer.register(EventBus.class, mockEventBus);
        testContainer.register(LLMClient.class, mockLLMClient);

        // Initialize test services
        initializeTestServices();
    }

    /**
     * Creates isolated test environment for an entity.
     */
    public TestContext createTestContext(String entityName) {
        return new TestContext(
            testContainer,
            entityName,
            mockEventBus,
            mockLLMClient
        );
    }

    /**
     * Test context with access to all test doubles.
     */
    public static class TestContext {
        private final ServiceContainer container;
        private final String entityName;
        private final MockEventBus eventBus;
        private final MockLLMClient llmClient;

        public TestContext(
            ServiceContainer container,
            String entityName,
            MockEventBus eventBus,
            MockLLMClient llmClient
        ) {
            this.container = container;
            this.entityName = entityName;
            this.eventBus = eventBus;
            this.llmClient = llmClient;
        }

        // Accessors for test doubles
        public MockEventBus getEventBus() { return eventBus; }
        public MockLLMClient getLLMClient() { return llmClient; }

        // Utility methods
        public void assertEventPublished(Class<? extends Event> eventType) {
            assertTrue(eventBus.wasPublished(eventType));
        }

        public void assertLLMCalled(String expectedPrompt) {
            assertEquals(expectedPrompt, llmClient.getLastPrompt());
        }
    }
}
```

### 10.3 Integration Test Example

```java
package com.minewwright.integration;

/**
 * Integration test for command execution flow.
 */
@Test
public class CommandExecutionIntegrationTest {

    private AITestFixture fixture;
    private TestContext context;

    @BeforeEach
    public void setUp() {
        fixture = new AITestFixture();
        context = fixture.createTestContext("TestForeman");
    }

    @Test
    public void testCommandExecutionFlow() {
        // Given: Mock LLM response
        context.getLLMClient().setResponse("""
            {
              "plan": "Mine 5 iron ore",
              "tasks": [
                {"action": "mine", "parameters": {"block": "iron_ore", "quantity": 5}}
              ]
            }
            """);

        // When: Execute command
        AIService aiService = context.getContainer().getService(AIService.class);
        CompletableFuture<ExecutionResult> future = aiService.executeCommand(
            "TestForeman",
            "Mine 5 iron ore"
        );

        // Then: Wait for completion
        ExecutionResult result = future.join(30, TimeUnit.SECONDS);
        assertTrue(result.isSuccess());

        // And: Verify event flow
        context.assertEventPublished(CommandReceivedEvent.class);
        context.assertEventPublished(CommandValidatedEvent.class);
        context.assertEventPublished(CommandExecutionCompletedEvent.class);

        // And: Verify LLM was called
        context.assertLLMCalled(contains("Mine 5 iron ore"));
    }
}
```

---

## 11. Implementation Roadmap

### Phase 1: Service Layer Foundation (Week 1)

**Goal:** Create service interfaces and container

- [ ] Define `AIService`, `MemoryService`, `CompanionService` interfaces
- [ ] Implement `ServiceRegistry` with lifecycle management
- [ ] Enhance `SimpleServiceContainer` with lifecycle support
- [ ] Create `AILifecycleManager` for startup/shutdown
- [ ] Add service initialization order tracking

**Deliverables:**
- Service interfaces defined
- Service registry implementation
- Lifecycle management working
- Unit tests for container

### Phase 2: Event Integration (Week 2)

**Goal:** Connect all components via EventBus

- [ ] Define event taxonomy (lifecycle, command, action, memory)
- [ ] Create `EventIntegrator` to register handlers
- [ ] Wire state machine events
- [ ] Wire command execution events
- [ ] Wire memory update events

**Deliverables:**
- Event types defined
- Event handlers registered
- Event flow documented
- Integration tests for events

### Phase 3: Configuration Layer (Week 3)

**Goal:** Implement hierarchical config with hot-reload

- [ ] Create `ConfigManager` implementation
- [ ] Implement config layer merging (global/world/entity)
- [ ] Add file watching for hot-reload
- [ ] Create config schema validation
- [ ] Migrate existing `MineWrightConfig` to new system

**Deliverables:**
- Config manager working
- Hot-reload functional
- Config schema defined
- Migration complete

### Phase 4: Error Handling (Week 4)

**Goal:** Comprehensive error recovery

- [ ] Implement `ErrorRecoveryCoordinator`
- [ ] Create error categorization logic
- [ ] Add retry strategies for transient errors
- [ ] Implement fallback handlers
- [ ] Add graceful degradation

**Deliverables:**
- Error recovery working
- Fallback services implemented
- Error handling tests
- Degradation strategies documented

### Phase 5: Service Implementations (Week 5-6)

**Goal:** Implement core services

- [ ] Implement `AIServiceImpl` facade
- [ ] Implement `MemoryServiceImpl` with vector search
- [ ] Implement `CompanionServiceImpl` with relationship tracking
- [ ] Wire existing components into service layer
- [ ] Add service health checks

**Deliverables:**
- All services implemented
- Existing components integrated
- Health monitoring working
- Service documentation complete

### Phase 6: Testing & Polish (Week 7)

**Goal:** Comprehensive test coverage

- [ ] Create `AITestFixture` infrastructure
- [ ] Write unit tests for all services
- [ ] Write integration tests for flows
- [ ] Add performance benchmarks
- [ ] Documentation completion

**Deliverables:**
- 80%+ test coverage
- Integration test suite
- Performance baseline
- Complete documentation

---

## 12. Implementation Checklist

### Core Services

- [ ] `AIService` interface and implementation
- [ ] `MemoryService` interface and implementation
- [ ] `CompanionService` interface and implementation
- [ ] `OrchestrationService` integration
- [ ] `ActionExecutor` integration

### Infrastructure

- [ ] `ServiceRegistry` implementation
- [ ] `LifecycleServiceContainer` implementation
- [ ] `AILifecycleManager` implementation
- [ ] `EventIntegrator` implementation
- [ ] `ConfigManager` implementation
- [ ] `ErrorRecoveryCoordinator` implementation

### Event Handling

- [ ] Define all event types
- [ ] Register lifecycle handlers
- [ ] Register entity handlers
- [ ] Register command handlers
- [ ] Register memory handlers

### Configuration

- [ ] Global config schema
- [ ] World config schema
- [ ] Entity config schema
- [ ] Config migration script
- [ ] Hot-reload implementation

### State Management

- [ ] `StateSynchronizer` implementation
- [ ] `WorldStateStore` implementation
- [ ] `EntityState` implementation
- [ ] State persistence logic
- [ ] State recovery logic

### Testing

- [ ] `AITestFixture` implementation
- [ ] Mock implementations for all services
- [ ] Unit tests for each service
- [ ] Integration tests for flows
- [ ] Performance benchmarks

### Documentation

- [ ] Service API documentation
- [ ] Event flow diagrams
- [ ] Configuration guide
- [ ] Error handling guide
- [ ] Integration guide for developers

---

## Appendix A: Service Interface Definitions

### AIService

```java
package com.minewwright.service;

public interface AIService {
    CompletableFuture<ExecutionResult> executeCommand(String entityName, String command);
    CompletableFuture<List<Task>> planCommand(String entityName, String command);
    MemoryService getMemoryService(String entityName);
    CompanionService getCompanionService(String entityName);
    OrchestrationService getOrchestrationService();
    void shutdown();
}
```

### MemoryService

```java
package com.minewwright.service;

public interface MemoryService {
    void remember(String content, MemoryType type, Map<String, Object> metadata);
    List<Memory> recall(String query, int maxResults);
    String buildContextForTask(String task);
    void compress();
    void save();
    void load();
}
```

### CompanionService

```java
package com.minewwright.service;

public interface CompanionService {
    void recordInteraction(String player, String type, boolean positive);
    int getRapportLevel();
    int getTrustLevel();
    String getRelationshipDescription();
    Optional<String> getInsideJoke(String context);
    ValidationResult validateTasks(List<Task> tasks);
    void onEvent(GameEvent event);
}
```

---

## Appendix B: Event Definitions

### Core Events

```java
// Lifecycle
public record ServiceInitializedEvent(Class<?> serviceType) {}
public record ServiceShutdownEvent(Class<?> serviceType) {}

// Command
public record CommandReceivedEvent(String entityName, String command) {}
public record CommandValidatedEvent(String entityName, List<Task> tasks) {}
public record CommandExecutionCompletedEvent(String entityName, ExecutionResult result) {}

// Memory
public record MemoryStoredEvent(String entityName, String memoryId) {}
public record RelationshipUpdatedEvent(String entityName, int rapport, int trust) {}

// State
public record StateChangedEvent(String entityName, AgentState oldState, AgentState newState) {}
```

---

## Conclusion

This integration architecture provides a clean, maintainable foundation for connecting all MineWright components. The key design decisions are:

1. **Service Layer:** All functionality exposed through service interfaces
2. **Dependency Injection:** `ServiceContainer` manages all dependencies
3. **Event-Driven:** Components communicate via `EventBus`
4. **State Isolation:** Clear separation of global/world/entity state
5. **Configuration:** Hierarchical config with hot-reload
6. **Error Recovery:** Comprehensive fallback strategies
7. **Testing:** Mockable interfaces for easy testing

The existing MineWright codebase already implements many of these patterns. The integration architecture extends and connects them into a cohesive system.

---

**Document Version:** 1.0
**Last Updated:** 2026-02-26
**Author:** Architecture Research Team
**Status:** Ready for Implementation
