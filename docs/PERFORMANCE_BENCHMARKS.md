# MineWright Performance Benchmarks

This document defines the comprehensive performance benchmark suite for MineWright (Steve AI), measuring LLM latency, action execution, memory usage, entity scaling, and network synchronization.

## Benchmark Overview

The benchmark suite is organized into five critical areas:

1. **LLM Latency Benchmarks** - Measure response times for AI planning
2. **Action Execution Benchmarks** - Profile tick-based action performance
3. **Memory Usage Benchmarks** - Track heap consumption and retention
4. **Entity Count Benchmarks** - Test scaling with multiple crew members
5. **Network Sync Benchmarks** - Measure client/server synchronization overhead

---

## 1. LLM Latency Benchmarks

### Purpose

Measure the end-to-end latency of Large Language Model (LLM) requests for task planning. This is the critical path for user-initiated commands and directly affects perceived responsiveness.

### Benchmarks

#### 1.1 Cold Start Latency

**Description**: Measure time to first response when the LLM client is initialized (no cache, no warm connections).

**Measurement Approach**:
```java
@Test
public void benchmarkColdStartLatency() {
    // Initialize fresh TaskPlanner instance
    TaskPlanner planner = new TaskPlanner();

    // Record start time
    long startTime = System.nanoTime();

    // Make planning request
    CompletableFuture<ParsedResponse> future =
        planner.planTasksAsync(foreman, "Mine 10 iron ore");

    // Wait for completion
    ParsedResponse response = future.get();
    long endTime = System.nanoTime();

    // Record metrics
    long latencyMs = (endTime - startTime) / 1_000_000;
    metrics.recordColdStartLatency(latencyMs);
}
```

**Target Metrics**:
- OpenAI GPT-4: < 2000ms (P95), < 5000ms (P99)
- Groq Llama3-70b: < 1000ms (P95), < 2000ms (P99)
- Gemini 1.5 Flash: < 1500ms (P95), < 3000ms (P99)

#### 1.2 Warm Cache Latency

**Description**: Measure response time when the request hits the LLM cache (40-60% expected hit rate in production).

**Measurement Approach**:
```java
@Test
public void benchmarkWarmCacheLatency() {
    TaskPlanner planner = new TaskPlanner();

    // First call to populate cache
    planner.planTasksAsync(foreman, "Mine 10 iron ore").get();

    // Second call should hit cache
    long startTime = System.nanoTime();
    ParsedResponse response = planner.planTasksAsync(
        foreman, "Mine 10 iron ore").get();
    long endTime = System.nanoTime();

    long latencyMs = (endTime - startTime) / 1_000_000;
    assertTrue(response.isFromCache());
    metrics.recordCacheHitLatency(latencyMs);
}
```

**Target Metrics**:
- Cache hit: < 10ms (P95)
- Cache miss: Same as cold start

#### 1.3 Concurrent Request Latency

**Description**: Measure latency degradation when multiple crew members request planning simultaneously.

**Measurement Approach**:
```java
@Test
public void benchmarkConcurrentLatency() {
    TaskPlanner planner = new TaskPlanner();
    int numAgents = 10;

    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(numAgents);
    AtomicLong maxLatency = new AtomicLong(0);

    // Spawn agents that all request planning simultaneously
    for (int i = 0; i < numAgents; i++) {
        final int agentId = i;
        executor.submit(() -> {
            ForemanEntity agent = createAgent("agent-" + agentId);
            startLatch.await(); // Wait for all to be ready

            long startTime = System.nanoTime();
            planner.planTasksAsync(agent, "Mine 10 iron ore").get();
            long latency = (System.nanoTime() - startTime) / 1_000_000;

            maxLatency.updateAndGet(current -> Math.max(current, latency));
            endLatch.countDown();
        });
    }

    startLatch.countDown(); // Start all at once
    endLatch.await(60, TimeUnit.SECONDS);

    metrics.recordConcurrentLatency(numAgents, maxLatency.get());
}
```

**Target Metrics**:
- 1-3 agents: < 1.2x single-agent latency
- 4-10 agents: < 2x single-agent latency
- 10+ agents: Batch queue should prevent degradation

#### 1.4 Rate Limit Recovery

**Description**: Measure time to recover from rate limit errors (OpenAI: 10 req/min for free tier).

**Measurement Approach**:
```java
@Test
public void benchmarkRateLimitRecovery() {
    TaskPlanner planner = new TaskPlanner();

    // Trigger rate limit
    for (int i = 0; i < 20; i++) {
        try {
            planner.planTasksAsync(foreman, "Mine 1 block").get();
        } catch (Exception e) {
            // Expected rate limit error
        }
    }

    // Wait for circuit breaker to recover
    long recoveryStart = System.nanoTime();

    // This should eventually succeed after retry/backoff
    while (!planner.isProviderHealthy("openai")) {
        Thread.sleep(100);
    }

    ParsedResponse response = planner.planTasksAsync(
        foreman, "Mine 10 iron ore").get();

    long recoveryTimeMs = (System.nanoTime() - recoveryStart) / 1_000_000;
    metrics.recordRateLimitRecovery(recoveryTimeMs);
}
```

**Target Metrics**:
- Circuit breaker OPEN -> HALF_OPEN: < 30 seconds
- Successful request after recovery: < 5000ms

---

## 2. Action Execution Benchmarks

### Purpose

Profile the tick-based execution system to ensure actions complete efficiently without blocking the game thread.

### Benchmarks

#### 2.1 Simple Action Ticks

**Description**: Measure ticks required for simple actions (pathfind, place single block, mine single block).

**Measurement Approach**:
```java
@Test
public void benchmarkSimpleActionTicks() {
    ForemanEntity foreman = spawnForeman();
    BaseAction action = new PathfindAction(foreman, new Task(
        "pathfind", Map.of("x", 100, "y", 64, "z", 100)));

    action.start();
    int tickCount = 0;

    while (!action.isComplete() && tickCount < 1000) {
        action.tick();
        tickCount++;
    }

    assertTrue(action.isComplete(), "Action should complete");
    metrics.recordActionTicks("pathfind_short", tickCount);
}
```

**Target Metrics**:
- Pathfind (short, < 20 blocks): < 100 ticks (5 seconds)
- Place single block: < 20 ticks (1 second)
- Mine single block: < 40 ticks (2 seconds, includes tool delay)

#### 2.2 Complex Action Ticks

**Description**: Measure ticks for complex actions (build structure, craft item, combat).

**Measurement Approach**:
```java
@Test
public void benchmarkBuildStructureTicks() {
    ForemanEntity foreman = spawnForeman();

    // Build a 5x5x3 house (75 blocks)
    List<BlockPlacement> blocks = generateHouseStructure(5, 5, 3);
    BaseAction action = new BuildStructureAction(foreman, new Task(
        "build", Map.of(
            "structure", "house",
            "blocks", serializeBlocks(blocks),
            "dimensions", "5,5,3")));

    action.start();
    int tickCount = 0;

    while (!action.isComplete() && tickCount < 10000) {
        action.tick();
        tickCount++;

        if (tickCount % 100 == 0) {
            System.out.println("Progress: " +
                ((BuildStructureAction)action).getProgressPercent() + "%");
        }
    }

    assertTrue(action.isComplete(), "Build should complete");
    metrics.recordActionTicks("build_5x5x3", tickCount);
}
```

**Target Metrics**:
- Build 5x5x3 house (75 blocks): < 1500 ticks (75 seconds)
- Craft simple item (planks): < 100 ticks (5 seconds)
- Combat (skeleton kill): < 300 ticks (15 seconds)

#### 2.3 Action Overhead Per Tick

**Description**: Measure baseline overhead of the action execution system (interceptor chain, state machine).

**Measurement Approach**:
```java
@Test
public void benchmarkActionTickOverhead() {
    ForemanEntity foreman = spawnForeman();
    ActionExecutor executor = foreman.getActionExecutor();

    // Create a no-op action that just ticks
    BaseAction noOpAction = new BaseAction(foreman, new Task("noop", Map.of())) {
        private int ticks = 0;
        @Override
        protected void onStart() {}

        @Override
        protected void onTick() {
            ticks++;
        }

        @Override
        protected void onCancel() {}

        @Override
        public String getDescription() {
            return "noop";
        }

        @Override
        public boolean isComplete() {
            return ticks >= 1000;
        }
    };

    noOpAction.start();

    long startTime = System.nanoTime();
    while (!noOpAction.isComplete()) {
        noOpAction.tick();
    }
    long endTime = System.nanoTime();

    long avgNsPerTick = (endTime - startTime) / 1000;
    metrics.recordTickOverheadNs(avgNsPerTick);
}
```

**Target Metrics**:
- Per-tick overhead: < 100 microseconds (0.1ms)
- This ensures actions don't contribute to server lag

#### 2.4 Concurrent Action Execution

**Description**: Measure performance impact when multiple agents execute actions simultaneously.

**Measurement Approach**:
```java
@Test
public void benchmarkConcurrentActions() {
    int numAgents = 10;
    List<ForemanEntity> agents = new ArrayList<>();
    List<BaseAction> actions = new ArrayList<>();

    // Spawn agents and create actions
    for (int i = 0; i < numAgents; i++) {
        ForemanEntity agent = spawnForeman("agent-" + i);
        agents.add(agent);

        BaseAction action = new MineBlockAction(agent, new Task(
            "mine", Map.of("block", "stone", "quantity", 10)));
        actions.add(action);
        action.start();
    }

    // Measure server tick rate degradation
    long startTime = System.nanoTime();
    int tickCount = 0;
    boolean allComplete = false;

    while (!allComplete && tickCount < 10000) {
        long tickStart = System.nanoTime();

        // Tick all agents
        for (BaseAction action : actions) {
            action.tick();
        }

        // Tick Minecraft server
        tickServer();

        long tickTime = (System.nanoTime() - tickStart) / 1_000_000;
        metrics.recordTickTime(tickTime);

        // Check completion
        allComplete = actions.stream().allMatch(BaseAction::isComplete);
        tickCount++;
    }

    // Calculate average tick time
    double avgTickMs = metrics.getAverageTickTime();
    assertTrue(avgTickMs < 50, "Average tick should be under 50ms (20 TPS)");
}
```

**Target Metrics**:
- 1-5 agents: No tick degradation (> 19 TPS)
- 5-10 agents: < 10% tick degradation (> 18 TPS)
- 10+ agents: Slight degradation acceptable (> 15 TPS)

---

## 3. Memory Usage Benchmarks

### Purpose

Track heap consumption, memory leaks, and retention policies to ensure long-running servers remain stable.

### Benchmarks

#### 3.1 Baseline Memory per Agent

**Description**: Measure heap consumed by a single idle agent (entity, executor, memory).

**Measurement Approach**:
```java
@Test
public void benchmarkMemoryPerAgent() {
    Runtime runtime = Runtime.getRuntime();

    // Force GC and measure baseline
    System.gc();
    Thread.sleep(100);
    long baseline = runtime.totalMemory() - runtime.freeMemory();

    // Spawn agent
    ForemanEntity agent = spawnForeman();

    // Force GC again
    System.gc();
    Thread.sleep(100);
    long withAgent = runtime.totalMemory() - runtime.freeMemory();

    long memoryPerAgent = (withAgent - baseline) / 1024; // KB
    metrics.recordMemoryPerAgent(memoryPerAgent);

    assertTrue(memoryPerAgent < 1024, "Agent should use < 1MB baseline");
}
```

**Target Metrics**:
- Idle agent: < 1 MB heap
- Agent with active task: < 5 MB heap
- Agent with 100-tick action history: < 10 MB heap

#### 3.2 Memory Leak Detection

**Description**: Run agents for extended period, check for unbounded memory growth.

**Measurement Approach**:
```java
@Test
public void benchmarkMemoryLeak() {
    Runtime runtime = Runtime.getRuntime();

    // Spawn 10 agents
    List<ForemanEntity> agents = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        agents.add(spawnForeman("agent-" + i));
    }

    // Baseline
    System.gc();
    Thread.sleep(100);
    long baseline = runtime.totalMemory() - runtime.freeMemory();

    // Run for 10,000 ticks (8.3 minutes)
    for (int tick = 0; tick < 10000; tick++) {
        for (ForemanEntity agent : agents) {
            agent.getActionExecutor().tick();

            // Occasionally give tasks to exercise memory
            if (tick % 500 == 0) {
                agent.getActionExecutor().processNaturalLanguageCommand(
                    "Mine 10 cobblestone");
            }
        }

        // Sample memory every 1000 ticks
        if (tick % 1000 == 0) {
            System.gc();
            Thread.sleep(100);
            long current = runtime.totalMemory() - runtime.freeMemory();
            long growth = current - baseline;
            metrics.recordMemoryGrowth(tick, growth);
        }
    }

    // Final measurement
    System.gc();
    Thread.sleep(100);
    long finalMemory = runtime.totalMemory() - runtime.freeMemory();
    long totalGrowth = (finalMemory - baseline) / 1024 / 1024; // MB

    assertTrue(totalGrowth < 50, "Memory growth should be < 50MB after 10k ticks");
}
```

**Target Metrics**:
- Growth after 10,000 ticks: < 50 MB
- Growth after 100,000 ticks: < 100 MB
- No OutOfMemoryError after 1 hour of operation

#### 3.3 LLM Cache Memory

**Description**: Measure memory consumed by LLM response cache.

**Measurement Approach**:
```java
@Test
public void benchmarkLLMCacheMemory() {
    TaskPlanner planner = new TaskPlanner();
    LLMCache cache = planner.getLLMCache();
    Runtime runtime = Runtime.getRuntime();

    System.gc();
    Thread.sleep(100);
    long baseline = runtime.totalMemory() - runtime.freeMemory();

    // Populate cache with 1000 unique prompts
    for (int i = 0; i < 1000; i++) {
        String prompt = "Mine " + i + " blocks";
        planner.planTasksAsync(foreman, prompt).join();
    }

    System.gc();
    Thread.sleep(100);
    long withCache = runtime.totalMemory() - runtime.freeMemory();

    long cacheMemory = (withCache - baseline) / 1024; // KB
    double avgPerEntry = cacheMemory / 1000.0;

    metrics.recordCacheMemory(1000, cacheMemory, avgPerEntry);

    assertTrue(avgPerEntry < 10, "Cache entry should be < 10KB");
}
```

**Target Metrics**:
- Average cached response: < 10 KB
- 1000 cached responses: < 10 MB
- Cache eviction should prevent unbounded growth

#### 3.4 Memory During High Load

**Description**: Measure memory spikes during concurrent agent planning and execution.

**Measurement Approach**:
```java
@Test
public void benchmarkHighLoadMemory() {
    Runtime runtime = Runtime.getRuntime();
    int numAgents = 50;

    List<ForemanEntity> agents = new ArrayList<>();
    for (int i = 0; i < numAgents; i++) {
        agents.add(spawnForeman("agent-" + i));
    }

    // All agents request planning simultaneously
    List<CompletableFuture<ParsedResponse>> futures = new ArrayList<>();
    for (ForemanEntity agent : agents) {
        futures.add(agent.getActionExecutor().getTaskPlanner()
            .planTasksAsync(agent, "Build a house"));
    }

    // Measure memory peak during async execution
    long peakMemory = 0;
    while (!futures.stream().allMatch(CompletableFuture::isDone)) {
        System.gc();
        Thread.sleep(10);
        long current = runtime.totalMemory() - runtime.freeMemory();
        peakMemory = Math.max(peakMemory, current);
    }

    // Wait for completion
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    long peakMB = peakMemory / 1024 / 1024;
    metrics.recordPeakMemoryDuringLoad(numAgents, peakMB);

    assertTrue(peakMB < 500, "Peak memory should be < 500MB for 50 agents");
}
```

**Target Metrics**:
- 50 concurrent planning requests: < 500 MB peak
- Memory should return to baseline after requests complete
- No memory leaks after repeated load cycles

---

## 4. Entity Count Benchmarks

### Purpose

Determine maximum number of crew members before performance degrades unacceptably.

### Benchmarks

#### 4.1 Spawn Time Scaling

**Description**: Measure time to spawn N agents.

**Measurement Approach**:
```java
@Test
public void benchmarkSpawnScaling() {
    for (int targetCount : List.of(1, 5, 10, 25, 50)) {
        // Clear existing agents
        clearAllAgents();

        long startTime = System.nanoTime();

        for (int i = 0; i < targetCount; i++) {
            spawnForeman("agent-" + i);
        }

        long spawnTimeMs = (System.nanoTime() - startTime) / 1_000_000;
        double avgMsPerAgent = (double) spawnTimeMs / targetCount;

        metrics.recordSpawnTime(targetCount, spawnTimeMs, avgMsPerAgent);

        assertTrue(avgMsPerAgent < 100,
            "Avg spawn time should be < 100ms per agent");
    }
}
```

**Target Metrics**:
- Spawn 1 agent: < 100ms
- Spawn 10 agents: < 1 second total
- Spawn 50 agents: < 5 seconds total
- Linear scaling expected

#### 4.2 Tick Time vs Entity Count

**Description**: Measure server tick degradation as entity count increases.

**Measurement Approach**:
```java
@Test
public void benchmarkTickTimeScaling() {
    for (int numAgents : List.of(1, 5, 10, 25, 50, 100)) {
        // Clear and spawn agents
        clearAllAgents();
        for (int i = 0; i < numAgents; i++) {
            ForemanEntity agent = spawnForeman("agent-" + i);
            // Give each agent a task to keep them active
            agent.getActionExecutor().processNaturalLanguageCommand(
                "Follow me");
        }

        // Warm up for 100 ticks
        for (int i = 0; i < 100; i++) {
            tickServer();
        }

        // Measure 1000 ticks
        long totalTickTime = 0;
        for (int i = 0; i < 1000; i++) {
            long tickStart = System.nanoTime();
            tickServer();
            totalTickTime += (System.nanoTime() - tickStart);
        }

        double avgTickMs = (totalTickTime / 1000.0) / 1_000_000;
        double tps = 1000.0 / (avgTickMs + 0.1); // Avoid div/0

        metrics.recordTickMetrics(numAgents, avgTickMs, tps);

        System.out.printf("%d agents: %.2fms/tick (%.1f TPS)%n",
            numAgents, avgTickMs, tps);

        // Verify minimum TPS threshold
        assertTrue(tps > 10, "TPS should be > 10 even with " + numAgents + " agents");
    }
}
```

**Target Metrics**:
- 1-5 agents: 20 TPS (no degradation)
- 5-10 agents: > 19 TPS
- 10-25 agents: > 15 TPS
- 25-50 agents: > 10 TPS
- 50+ agents: Graceful degradation, remain playable

#### 4.3 Maximum Concurrent Actions

**Description**: Find the breaking point where too many concurrent actions cause issues.

**Measurement Approach**:
```java
@Test
public void benchmarkMaxConcurrentActions() {
    clearAllAgents();

    // Spawn 50 agents, give each a complex task
    List<ForemanEntity> agents = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
        ForemanEntity agent = spawnForeman("agent-" + i);
        agents.add(agent);
        agent.getActionExecutor().processNaturalLanguageCommand(
            "Build a stone house at " + (i * 10) + ", 64, 0");
    }

    // Run for 10,000 ticks, track completion rate
    int completedCount = 0;
    int tickCount = 0;

    for (tick = 0; tick < 10000; tick++) {
        for (ForemanEntity agent : agents) {
            agent.getActionExecutor().tick();

            if (!agent.getActionExecutor().isExecuting() &&
                agent.getActionExecutor().getCurrentGoal() != null) {
                completedCount++;
            }
        }
        tickServer();
    }

    double completionRate = (double) completedCount / 50.0;
    double avgTicksPerTask = tickCount * 50.0 / completedCount;

    metrics.recordCompletionMetrics(50, completedCount, completionRate,
        avgTicksPerTask);

    // At least 80% should make progress
    assertTrue(completionRate > 0.8,
        "At least 80% of agents should complete tasks");
}
```

**Target Metrics**:
- 50 concurrent build tasks: > 80% completion success
- 100 concurrent tasks: > 60% completion success
- Failed tasks should be logged with clear error messages

#### 4.4 Chunk Loading Performance

**Description**: Measure impact of agents loading chunks as they pathfind.

**Measurement Approach**:
```java
@Test
public void benchmarkChunkLoading() {
    clearAllAgents();

    // Spawn agents at different locations
    List<ForemanEntity> agents = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        ForemanEntity agent = spawnForeman("agent-" + i);
        // Teleport to distant locations
        agent.setPos(i * 1000, 64, 0);
        agents.add(agent);
    }

    // All agents pathfind to origin (loading chunks along the way)
    for (ForemanEntity agent : agents) {
        agent.getActionExecutor().processNaturalLanguageCommand(
            "Go to 0 64 0");
    }

    // Measure chunk load rate
    ChunkTicketManager ticketManager = server.getChunkTicketManager();
    long startTickets = ticketManager.getTicketCount();

    for (int tick = 0; tick < 5000; tick++) {
        for (ForemanEntity agent : agents) {
            agent.getActionExecutor().tick();
        }
        tickServer();
    }

    long endTickets = ticketManager.getTicketCount();
    long ticketsPerAgent = (endTickets - startTickets) / 10;

    metrics.recordChunkTicketsPerAgent(ticketsPerAgent);

    assertTrue(ticketsPerAgent < 100,
        "Each agent should request < 100 chunk tickets");
}
```

**Target Metrics**:
- Agent pathfinding: < 100 chunk tickets per agent
- Chunk loading should not cause server-wide lag spikes
- Unused chunks should be unloaded after agent leaves

---

## 5. Network Sync Benchmarks

### Purpose

Measure bandwidth and latency of synchronizing agent state between server and clients.

### Benchmarks

#### 5.1 Bandwidth per Agent

**Description**: Measure bytes transmitted per second per active agent.

**Measurement Approach**:
```java
@Test
public void benchmarkBandwidthPerAgent() {
    clearAllAgents();

    // Connect a mock client to track packets
    MockClientConnection client = connectMockClient();

    // Spawn 10 agents with various actions
    for (int i = 0; i < 10; i++) {
        ForemanEntity agent = spawnForeman("agent-" + i);
        agent.getActionExecutor().processNaturalLanguageCommand(
            "Mine cobblestone");
    }

    // Run for 1000 ticks (50 seconds)
    long totalBytes = 0;
    for (int tick = 0; tick < 1000; tick++) {
        long bytesBefore = client.getTotalBytesReceived();

        for (ForemanEntity agent : agents) {
            agent.tick();
        }
        tickServer();

        totalBytes += client.getTotalBytesReceived() - bytesBefore;
    }

    double bytesPerSecond = totalBytes / 50.0;
    double bytesPerAgentPerSecond = bytesPerSecond / 10.0;

    metrics.recordBandwidthPerAgent(bytesPerAgentPerSecond);

    assertTrue(bytesPerAgentPerSecond < 1024,
        "Bandwidth should be < 1KB/s per agent");
}
```

**Target Metrics**:
- Idle agent: < 100 bytes/s (only position updates)
- Mining agent: < 500 bytes/s (position + animation)
- Building agent: < 1 KB/s (position + block placements)
- 10 agents: < 10 KB/s total

#### 5.2 Client-Side Rendering

**Description**: Measure FPS impact on client with N agents visible.

**Measurement Approach**:
```java
@Test
@ClientSide
public void benchmarkClientRendering() {
    MinecraftClient client = getClient();

    for (int numAgents : List.of(1, 5, 10, 25, 50)) {
        // Spawn agents within view distance
        for (int i = 0; i < numAgents; i++) {
            spawnAgentNear("agent-" + i, client.player, 20);
        }

        // Measure FPS over 1000 ticks
        int totalFrames = 0;
        long startTime = System.nanoTime();

        for (int tick = 0; tick < 1000; tick++) {
            client.tick();
            if (client.render()) {
                totalFrames++;
            }
        }

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        double avgFps = (totalFrames * 1000.0) / durationMs;

        metrics.recordClientFPS(numAgents, avgFps);

        System.out.printf("%d agents: %.1f FPS%n", numAgents, avgFps);

        assertTrue(avgFps > 30,
            "Client should maintain > 30 FPS with " + numAgents + " agents");

        // Clear for next iteration
        clearAllAgents();
    }
}
```

**Target Metrics**:
- 1-10 agents visible: > 60 FPS
- 10-25 agents visible: > 45 FPS
- 25-50 agents visible: > 30 FPS
- > 50 agents: Should be playable, FPS may drop below 30

#### 5.3 Synchronization Latency

**Description**: Measure time from agent action to client observation.

**Measurement Approach**:
```java
@Test
public void benchmarkSyncLatency() {
    ForemanEntity agent = spawnForeman();
    MockClientConnection client = connectMockClient();

    // Client starts watching agent
    client.trackEntity(agent);

    // Agent performs an action
    agent.getActionExecutor().processNaturalLanguageCommand(
        "Place a stone block at 100 64 100");

    // Wait for client to see the block
    long startTime = System.nanoTime();
    boolean seen = false;

    for (int tick = 0; tick < 200; tick++) { // Max 10 seconds
        agent.tick();
        tickServer();

        if (client.sawBlockAt(100, 64, 100)) {
            seen = true;
            break;
        }
    }

    long latencyMs = (System.nanoTime() - startTime) / 1_000_000;

    assertTrue(seen, "Client should see the block placement");
    metrics.recordSyncLatency(latencyMs);

    assertTrue(latencyMs < 500,
        "Sync latency should be < 500ms (1 tick往返)");
}
```

**Target Metrics**:
- Position sync: < 100ms (1-2 ticks)
- Block placement: < 500ms (10 ticks)
- Chat message: < 200ms (4 ticks)
- Large structure update: < 2000ms (40 ticks)

#### 5.4 Multi-Client Sync

**Description**: Measure server load with multiple clients observing agents.

**Measurement Approach**:
```java
@Test
public void benchmarkMultiClientSync() {
    clearAllAgents();

    // Spawn 10 agents
    for (int i = 0; i < 10; i++) {
        ForemanEntity agent = spawnForeman("agent-" + i);
        agent.getActionExecutor().processNaturalLanguageCommand(
            "Build a house");
    }

    // Connect 5 clients
    List<MockClientConnection> clients = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
        clients.add(connectMockClient());
    }

    // Measure server CPU time for network sync
    long networkThreadTime = 0;

    for (int tick = 0; tick < 1000; tick++) {
        long threadStart = getCpuTime(NetworkThread.class);

        for (ForemanEntity agent : agents) {
            agent.tick();
        }
        tickServer();

        networkThreadTime += getCpuTime(NetworkThread.class) - threadStart;
    }

    double avgNetworkMsPerTick = networkThreadTime / 1000.0;

    metrics.recordNetworkThreadTime(10, 5, avgNetworkMsPerTick);

    assertTrue(avgNetworkMsPerTick < 5,
        "Network thread should use < 5ms/tick for 10 agents, 5 clients");
}
```

**Target Metrics**:
- 10 agents + 5 clients: < 5ms network thread per tick
- Network thread should not block game thread
- Packet batching should reduce overhead by > 50%

---

## Benchmark Infrastructure

### Test Framework

Benchmarks will be implemented using JUnit 5 with custom metric collection:

```java
// src/test/java/com/minewright/benchmark/BenchmarkSuite.java
public class BenchmarkSuite {
    private final MetricsCollector metrics = new MetricsCollector();

    @Test
    public void runAllBenchmarks() {
        // LLM benchmarks
        benchmarkColdStartLatency();
        benchmarkWarmCacheLatency();
        benchmarkConcurrentLatency();
        benchmarkRateLimitRecovery();

        // Action execution benchmarks
        benchmarkSimpleActionTicks();
        benchmarkComplexActionTicks();
        benchmarkActionTickOverhead();
        benchmarkConcurrentActions();

        // Memory benchmarks
        benchmarkMemoryPerAgent();
        benchmarkMemoryLeak();
        benchmarkLLMCacheMemory();
        benchmarkHighLoadMemory();

        // Entity count benchmarks
        benchmarkSpawnScaling();
        benchmarkTickTimeScaling();
        benchmarkMaxConcurrentActions();
        benchmarkChunkLoading();

        // Network benchmarks
        benchmarkBandwidthPerAgent();
        benchmarkClientRendering();
        benchmarkSyncLatency();
        benchmarkMultiClientSync();

        // Generate report
        metrics.generateReport("docs/BENCHMARK_RESULTS.md");
    }
}
```

### Metrics Collection

```java
// src/test/java/com/minewright/benchmark/MetricsCollector.java
public class MetricsCollector {
    private final Map<String, List<Double>> measurements = new HashMap<>();

    public void record(String metricName, double value) {
        measurements.computeIfAbsent(metricName, k -> new ArrayList<>())
            .add(value);
    }

    public void generateReport(String outputPath) {
        StringBuilder report = new StringBuilder();
        report.append("# MineWright Benchmark Results\n\n");
        report.append("Generated: ").append(new Date()).append("\n\n");

        for (Map.Entry<String, List<Double>> entry : measurements.entrySet()) {
            String name = entry.getKey();
            List<Double> values = entry.getValue();

            double min = values.stream().min(Double::compare).orElse(0);
            double max = values.stream().max(Double::compare).orElse(0);
            double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double p95 = percentile(values, 95);
            double p99 = percentile(values, 99);

            report.append("## ").append(name).append("\n");
            report.append(String.format("- Min: %.2f\n", min));
            report.append(String.format("- Max: %.2f\n", max));
            report.append(String.format("- Avg: %.2f\n", avg));
            report.append(String.format("- P95: %.2f\n", p95));
            report.append(String.format("- P99: %.2f\n", p99));
            report.append("\n");
        }

        Files.writeString(Path.of(outputPath), report.toString());
    }

    private double percentile(List<Double> values, int percentile) {
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = (int) Math.ceil(sorted.size() * percentile / 100.0) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}
```

### Running Benchmarks

```bash
# Run all benchmarks
./gradlew test --tests "*Benchmark*"

# Run specific benchmark category
./gradlew test --tests "*LLMBenchmark*"

# Run with memory profiling
./gradlew test -DjvmArgs="-Xmx2G -XX:+PrintGCDetails" --tests "*Benchmark*"
```

### Continuous Integration

Benchmarks should run on every PR to detect performance regressions:

```yaml
# .github/workflows/benchmarks.yml
name: Performance Benchmarks

on: [pull_request, push]

jobs:
  benchmark:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run benchmarks
        run: ./gradlew test --tests "*Benchmark*"
      - name: Upload results
        uses: actions/upload-artifact@v3
        with:
          name: benchmark-results
          path: docs/BENCHMARK_RESULTS.md
```

---

## Performance Targets Summary

| Metric Category | Target | Acceptable Threshold |
|-----------------|--------|---------------------|
| **LLM Latency** | | |
| Cold start (Groq) | < 1000ms | < 2000ms |
| Cache hit | < 10ms | < 50ms |
| 10 concurrent agents | < 2x degradation | < 3x degradation |
| **Action Execution** | | |
| Simple action (pathfind) | < 100 ticks | < 200 ticks |
| Complex action (build 5x5x3) | < 1500 ticks | < 3000 ticks |
| Per-tick overhead | < 0.1ms | < 0.5ms |
| 10 agents (20 TPS) | No degradation | < 5% degradation |
| **Memory Usage** | | |
| Per idle agent | < 1 MB | < 5 MB |
| 10k tick growth | < 50 MB | < 100 MB |
| 50 concurrent planning | < 500 MB peak | < 1 GB peak |
| **Entity Scaling** | | |
| Spawn 10 agents | < 1s | < 2s |
| 25 agents (TPS) | > 15 TPS | > 10 TPS |
| 50 concurrent tasks | > 80% success | > 60% success |
| **Network Sync** | | |
| Bandwidth per active agent | < 1 KB/s | < 5 KB/s |
| Client FPS (25 agents) | > 45 FPS | > 30 FPS |
| Sync latency (block place) | < 500ms | < 2000ms |

---

## Regression Detection

Any benchmark that fails to meet the "Acceptable Threshold" should be considered a regression and block merge until fixed. Performance degradation of > 20% compared to baseline should trigger a warning.

---

## Future Enhancements

Additional benchmarks to consider:

1. **Database Performance** - If integrating persistent storage for agent memories
2. **Pathfinding Optimization** - A* vs navigation mesh benchmarks
3. **Action Plugin Loading** - Time to load N plugins at startup
4. **Voice Input/Output** - Latency and accuracy of speech-to-text/text-to-speech
5. **Cross-Server Communication** - Agent coordination across multiple servers

---

*Document Version: 1.0*
*Last Updated: 2026-02-27*
*Maintained By: MineWright Development Team*
