# MineWright Integration Test Scenarios

**Version:** 1.0.0
**Last Updated:** 2026-02-27
**Status:** Design Document

## Overview

This document defines comprehensive integration test scenarios for the MineWright mod (formerly "Steve AI" - autonomous AI agents that play Minecraft with you). These scenarios cover end-to-end workflows, multi-agent coordination, failure handling, world persistence, and GUI interactions.

**Testing Philosophy:**
- **Integration tests** verify that multiple components work together correctly
- **End-to-end tests** validate complete user workflows from GUI to execution
- **Resilience tests** ensure the system handles failures gracefully
- **Manual test scenarios** can be executed by developers for validation

---

## Test Categories

1. **End-to-End Command Flow** - GUI → LLM → Action → Result
2. **Multi-Agent Coordination** - Foreman + Workers orchestration
3. **LLM Failure Handling** - Timeout, rate limit, error recovery
4. **World Persistence** - Save/Load with entity state
5. **GUI Interactions** - All GUI features and user interactions
6. **Action Execution** - Individual action behaviors
7. **Memory & Context** - Memory persistence and retrieval

---

## 1. End-to-End Command Flow Tests

### 1.1 Simple Command - Single Action

**Scenario:** User submits a simple command that requires a single action

**Test Steps:**
1. Spawn a Foreman entity: `/steve spawn Foreman`
2. Open GUI by pressing **K**
3. Type command: "Mine 10 stone"
4. Press **Enter** to submit
5. Verify GUI shows "Thinking..." message
6. Wait for LLM response (async, non-blocking)
7. Verify GUI shows "Okay! [plan]" message
8. Verify Foreman entity begins mining stone
9. Verify 10 stone blocks are mined

**Expected Behaviors:**
- GUI opens smoothly without blocking game
- Command is sent to ActionExecutor
- Async LLM call is initiated (no game freeze)
- Planning state is set to true
- When LLM responds, tasks are queued
- ActionExecutor executes the MineBlockAction
- Chat/GUI confirms completion

**Test Automation Approach:**
```java
@Test
void testSimpleCommandFlow() {
    // Given
    ForemanEntity foreman = spawnTestForeman();
    ActionExecutor executor = foreman.getActionExecutor();

    // When
    executor.processNaturalLanguageCommand("Mine 10 stone");

    // Then
    await().atMost(30, TimeUnit.SECONDS)
        .until(() -> !executor.isPlanning());

    await().atMost(60, TimeUnit.SECONDS)
        .until(() -> !executor.isExecuting());

    verifyActionsCompleted(foreman, 1);
}
```

---

### 1.2 Complex Command - Multi-Step Plan

**Scenario:** User submits a complex command requiring multiple actions

**Test Steps:**
1. Spawn Foreman entity
2. Open GUI and type: "Build a 5x5 cobblestone house"
3. Submit command
4. Verify LLM generates a multi-step plan:
   - Pathfind to location
   - Mine/place cobblestone
   - Build structure
5. Verify actions execute sequentially
6. Verify structure is built correctly

**Expected Behaviors:**
- LLM generates 3-5 tasks
- Tasks are queued in ActionExecutor
- Actions execute sequentially
- Each action completion triggers next action
- Final structure matches specifications

**Test Automation Approach:**
```java
@Test
void testComplexCommandFlow() {
    ForemanEntity foreman = spawnTestForeman();

    executor.processNaturalLanguageCommand("Build a 5x5 cobblestone house");

    await().atMost(120, TimeUnit.SECONDS)
        .until(() -> !executor.isExecuting());

    StructureValidator validator = new StructureValidator();
    assertTrue(validator.validateHouse(foreman.level(), 5, 5, Material.COBBLESTONE));
}
```

---

### 1.3 Invalid Command - Error Handling

**Scenario:** User submits an invalid or unclear command

**Test Steps:**
1. Spawn Foreman entity
2. Open GUI and type: "Do something impossible"
3. Submit command
4. Verify appropriate error message

**Expected Behaviors:**
- LLM returns null or unparseable response
- GUI shows: "I couldn't understand that command."
- No actions are queued
- Foreman returns to idle state
- No exceptions thrown

**Test Automation Approach:**
```java
@Test
void testInvalidCommand() {
    ForemanEntity foreman = spawnTestForeman();

    executor.processNaturalLanguageCommand("Do something impossible");

    await().atMost(30, TimeUnit.SECONDS)
        .until(() -> !executor.isPlanning());

    assertFalse(executor.isExecuting());
    assertEquals("", executor.getCurrentGoal());
}
```

---

## 2. Multi-Agent Coordination Tests

### 2.1 Single Agent - Solo Mode

**Scenario:** Single Foreman entity works independently

**Test Steps:**
1. Spawn single Foreman: `/steve solo Foreman`
2. Assign task: "Mine 20 iron ore"
3. Verify agent registers as FOREMAN role
4. Verify agent completes task independently
5. Verify no coordination messages sent

**Expected Behaviors:**
- Agent registers as FOREMAN (first agent)
- OrchestratorService processes command in solo mode
- Task is executed by the single agent
- No communication bus messages exchanged

**Test Automation Approach:**
```java
@Test
void testSingleAgentSoloMode() {
    ForemanEntity foreman = spawnTestForeman("solo");

    executor.processNaturalLanguageCommand("Mine 20 iron ore");

    await().atMost(120, TimeUnit.SECONDS)
        .until(() -> !executor.isExecuting());

    assertEquals(AgentRole.FOREMAN, foreman.getRole());
    verify(orchestrator, never()).processHumanCommand(any(), any());
}
```

---

### 2.2 Two Agents - Foreman + Worker

**Scenario:** Foreman delegates tasks to worker

**Test Steps:**
1. Spawn Foreman: `/steve spawn Foreman`
2. Spawn Worker: `/steve spawn Worker1`
3. Open GUI and type: "Build a 10x10 stone platform"
4. Submit command
5. Verify Foreman processes command and delegates
6. Verify Worker receives task assignment
7. Verify Worker executes tasks
8. Verify progress reporting from Worker to Foreman

**Expected Behaviors:**
- Foreman (first spawned) registers as FOREMAN role
- Worker1 (second spawned) registers as WORKER role
- OrchestratorService creates plan with tasks
- Foreman broadcasts plan announcement
- Worker receives TASK_ASSIGNMENT message
- Worker executes tasks and sends TASK_PROGRESS updates
- Foreman receives updates and tracks completion

**Test Automation Approach:**
```java
@Test
void testForemanWorkerCoordination() {
    ForemanEntity foreman = spawnTestForeman("Foreman");
    ForemanEntity worker = spawnTestForeman("Worker1");

    foreman.getActionExecutor().processNaturalLanguageCommand(
        "Build a 10x10 stone platform"
    );

    await().atMost(10, TimeUnit.SECONDS)
        .until(() -> worker.getCurrentTaskId() != null);

    assertNotNull(worker.getCurrentTaskId());
    assertEquals(AgentRole.FOREMAN, foreman.getRole());
    assertEquals(AgentRole.WORKER, worker.getRole());

    await().atMost(180, TimeUnit.SECONDS)
        .until(() -> !worker.getActionExecutor().isExecuting());
}
```

---

### 2.3 Multiple Workers - Task Distribution

**Scenario:** Foreman distributes tasks across multiple workers

**Test Steps:**
1. Spawn Foreman: `/steve spawn Foreman`
2. Spawn 3 workers: Worker1, Worker2, Worker3
3. Open GUI and type: "Build three 5x5 cobblestone towers"
4. Submit command
5. Verify tasks are distributed across workers
6. Verify each worker works on separate tower
7. Verify all workers complete in parallel

**Expected Behaviors:**
- OrchestratorService divides tasks among available workers
- Round-robin or spatial distribution is used
- Workers operate independently without blocking each other
- Foreman tracks overall progress
- All tasks complete successfully

**Test Automation Approach:**
```java
@Test
void testMultipleWorkerDistribution() {
    ForemanEntity foreman = spawnTestForeman("Foreman");
    List<ForemanEntity> workers = spawnTestWorkers(3);

    foreman.getActionExecutor().processNaturalLanguageCommand(
        "Build three 5x5 cobblestone towers"
    );

    await().atMost(10, TimeUnit.SECONDS)
        .until(() -> workers.stream()
            .allMatch(w -> w.getCurrentTaskId() != null));

    // Verify parallel execution
    Set<String> taskIds = workers.stream()
        .map(ForemanEntity::getCurrentTaskId)
        .collect(Collectors.toSet());

    assertTrue(taskIds.size() > 1, "Tasks should be distributed");
}
```

---

### 2.4 Worker Failure - Task Reassignment

**Scenario:** Worker fails and task is reassigned

**Test Steps:**
1. Spawn Foreman + 2 workers
2. Assign multi-step task
3. Simulate Worker1 failure (remove entity)
4. Verify OrchestratorService detects failure
5. Verify task is reassigned to Worker2
6. Verify task completes successfully

**Expected Behaviors:**
- OrchestratorService detects worker unregistration
- TaskAssignment is marked as needing reassignment
- New worker is assigned the failed task
- Task retry counter increments
- Task completes on new worker

**Test Automation Approach:**
```java
@Test
void testWorkerFailureReassignment() {
    ForemanEntity foreman = spawnTestForeman("Foreman");
    ForemanEntity worker1 = spawnTestForeman("Worker1");
    ForemanEntity worker2 = spawnTestForeman("Worker2");

    foreman.getActionExecutor().processNaturalLanguageCommand(
        "Build a 10x10 stone platform"
    );

    await().atMost(5, TimeUnit.SECONDS)
        .until(() -> worker1.getCurrentTaskId() != null);

    String taskId = worker1.getCurrentTaskId();

    // Simulate worker failure
    worker1.discard();
    crewManager.removeCrewMember("Worker1");

    // Verify reassignment
    await().atMost(10, TimeUnit.SECONDS)
        .until(() -> worker2.getCurrentTaskId() != null);

    assertEquals(taskId, worker2.getCurrentTaskId());
}
```

---

## 3. LLM Failure Handling Tests

### 3.1 LLM Timeout - Circuit Breaker

**Scenario:** LLM provider times out repeatedly

**Test Steps:**
1. Mock LLM client to timeout (>30s)
2. Submit command: "Mine 10 stone"
3. Verify timeout exception is caught
4. Verify retry mechanism triggers (configurable retries)
5. Verify circuit breaker opens after threshold
6. Verify fallback response is generated
7. Verify user receives fallback response

**Expected Behaviors:**
- CircuitBreaker records failures
- After failure threshold, circuit opens
- Subsequent requests fail fast (no waiting)
- FallbackHandler generates pattern-based response
- User receives: "I'm having trouble connecting, but I'll try: [fallback action]"

**Test Automation Approach:**
```java
@Test
void testLLMTimeoutCircuitBreaker() {
    AsyncLLMClient mockClient = mockTimeoutClient();
    ResilientLLMClient resilientClient = new ResilientLLMClient(
        mockClient, new LLMCache(), new LLMFallbackHandler()
    );

    // Trigger multiple timeouts
    for (int i = 0; i < 5; i++) {
        CompletableFuture<LLMResponse> future = resilientClient.sendAsync(
            "Mine 10 stone", Map.of()
        );
        future.orTimeout(35, TimeUnit.SECONDS);
    }

    // Verify circuit breaker is open
    assertEquals(
        CircuitBreaker.State.OPEN,
        resilientClient.getCircuitBreakerState()
    );

    // Verify fallback response
    CompletableFuture<LLMResponse> fallbackFuture = resilientClient.sendAsync(
        "Mine 10 stone", Map.of()
    );

    LLMResponse response = fallbackFuture.join();
    assertTrue(response.isFallback());
}
```

---

### 3.2 Rate Limiting - Request Throttling

**Scenario:** Too many requests trigger rate limiter

**Test Steps:**
1. Configure rate limiter to 10 requests/minute
2. Submit 15 commands rapidly
3. Verify first 10 succeed
4. Verify next 5 are rejected with rate limit error
5. Verify wait time is enforced
6. Verify requests succeed after wait

**Expected Behaviors:**
- RateLimiter tracks request rate
- Excess requests are rejected immediately
- Rejection message includes wait time
- After rate limit window, requests succeed again

**Test Automation Approach:**
```java
@Test
void testRateLimiting() {
    ResilientLLMClient client = createResilientClient();

    List<CompletableFuture<LLMResponse>> futures = new ArrayList<>();

    // Submit 15 requests rapidly
    for (int i = 0; i < 15; i++) {
        futures.add(client.sendAsync("Command " + i, Map.of()));
    }

    // Verify first 10 succeed, next 5 fail with rate limit
    int successCount = 0;
    int rateLimitedCount = 0;

    for (CompletableFuture<LLMResponse> future : futures) {
        try {
            future.get(5, TimeUnit.SECONDS);
            successCount++;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RateLimiterOverflowException) {
                rateLimitedCount++;
            }
        }
    }

    assertEquals(10, successCount);
    assertEquals(5, rateLimitedCount);
}
```

---

### 3.3 Network Error - Retry with Backoff

**Scenario:** Intermittent network failures

**Test Steps:**
1. Mock LLM client to fail with IOException
2. Submit command
3. Verify first attempt fails
4. Verify retry is triggered with exponential backoff
5. Verify second attempt succeeds
6. Verify total time includes backoff delays

**Expected Behaviors:**
- Retry decorator catches IOException
- Exponential backoff: 1s, 2s, 4s, 8s, 16s
- Successful retry returns response
- Response is cached for future requests

**Test Automation Approach:**
```java
@Test
void testRetryWithBackoff() throws Exception {
    AsyncLLMClient mockClient = mock(AsyncLLMClient.class);

    // First call fails, second succeeds
    when(mockClient.sendAsync(any(), any()))
        .thenReturn(completedFuture(errorResponse()))
        .thenReturn(completedFuture(successResponse()));

    ResilientLLMClient client = new ResilientLLMClient(
        mockClient, new LLMCache(), new LLMFallbackHandler()
    );

    long startTime = System.currentTimeMillis();
    LLMResponse response = client.sendAsync("test", Map.of()).get();
    long duration = System.currentTimeMillis() - startTime;

    assertTrue(response.isSuccess());
    assertTrue(duration >= 1000, "Should include backoff delay");
    verify(mockClient, times(2)).sendAsync(any(), any());
}
```

---

### 3.4 Invalid API Key - Authentication Error

**Scenario:** LLM provider rejects invalid API key

**Test Steps:**
1. Configure invalid API key
2. Submit command
3. Verify 401 authentication error
4. Verify circuit breaker opens immediately
5. Verify fallback response is generated
6. Verify user is notified of auth issue

**Expected Behaviors:**
- HTTP 401 triggers circuit breaker
- No retries for auth errors (wasteful)
- Fallback response generated
- User message: "API key is invalid. Please check configuration."

**Test Automation Approach:**
```java
@Test
void testInvalidApiKey() {
    configureInvalidApiKey();

    executor.processNaturalLanguageCommand("Mine 10 stone");

    await().atMost(5, TimeUnit.SECONDS)
        .until(() -> !executor.isPlanning());

    // Should use fallback immediately
    verify(logger).error(contains("401"), any());
    assertFalse(executor.isExecuting());
}
```

---

## 4. World Persistence Tests

### 4.1 Entity Save - All State Persisted

**Scenario:** Save world with active Foreman entity

**Test Steps:**
1. Spawn Foreman entity
2. Assign complex task with multiple actions
3. Let Foreman complete some actions
4. Save world (Minecraft save mechanism)
5. Verify NBT data includes:
   - Entity name
   - Current goal
   - Recent actions
   - Task queue
   - Companion memory
   - Personality traits
   - Relationship data

**Expected Behaviors:**
- `addAdditionalSaveData()` is called
- All state is written to CompoundTag
- NBT structure matches schema
- No data loss or corruption

**Test Automation Approach:**
```java
@Test
void testEntitySave() {
    ForemanEntity foreman = spawnTestForeman();
    foreman.setEntityName("TestForeman");
    foreman.getMemory().setCurrentGoal("Build house");
    foreman.getMemory().addAction("Mine stone");
    foreman.getCompanionMemory().getRelationship().increaseAffection(10);

    CompoundTag tag = new CompoundTag();
    foreman.addAdditionalSaveData(tag);

    assertEquals("TestForeman", tag.getString("CrewName"));
    assertEquals("Build house", tag.getCompound("Memory").getString("CurrentGoal"));

    ListTag actions = tag.getCompound("Memory").getList("RecentActions", 8);
    assertEquals(1, actions.size());
    assertEquals("Mine stone", actions.getString(0));

    CompoundTag companion = tag.getCompound("CompanionMemory");
    assertTrue(compound.contains("Relationship"));
}
```

---

### 4.2 Entity Load - State Restored

**Scenario:** Load world with saved Foreman entity

**Test Steps:**
1. Create saved NBT data from previous test
2. Create new ForemanEntity instance
3. Call `readAdditionalSaveData()` with NBT
4. Verify all state is restored:
   - Entity name
   - Current goal
   - Recent actions
   - Companion memory
   - Relationship values

**Expected Behaviors:**
- `readAdditionalSaveData()` restores all fields
- Entity continues from saved state
- No null pointers or missing data
- Memory and relationship data intact

**Test Automation Approach:**
```java
@Test
void testEntityLoad() {
    // Create NBT data
    CompoundTag tag = createTestNBT();

    // Load into new entity
    ForemanEntity foreman = new ForemanEntity(entityType, testLevel);
    foreman.readAdditionalSaveData(tag);

    // Verify restoration
    assertEquals("TestForeman", foreman.getEntityName());
    assertEquals("Build house", foreman.getMemory().getCurrentGoal());
    assertEquals(1, foreman.getMemory().getRecentActions(10).size());
    assertEquals(10, foreman.getCompanionMemory().getRelationship().getAffection());
}
```

---

### 4.3 World Reload - Continuity

**Scenario:** Save and reload entire Minecraft world

**Test Steps:**
1. Start test server
2. Spawn 3 Foreman entities with different tasks
3. Let them execute some tasks
4. Save and close server
5. Restart server (load world)
6. Verify all entities exist
7. Verify all state is restored
8. Verify tasks continue from where they left off

**Expected Behaviors:**
- All 3 entities reload correctly
- Each entity has correct name and role
- Task queues are preserved
- Companion memory is intact
- Relationship data persists
- Actions continue after reload

**Test Automation Approach:**
```java
@Test
void testWorldReload() {
    // Setup
    MinecraftServer server = startTestServer();
    spawnTestForeman("Foreman1");
    spawnTestForeman("Worker1");
    spawnTestForeman("Worker2");

    // Execute some actions
    executeCommands();

    // Save and reload
    CompoundTag levelData = saveWorld(server);
    server.close();

    server = loadWorld(levelData);

    // Verify
    CrewManager manager = getCrewManager();
    assertEquals(3, manager.getActiveCount());
    assertNotNull(manager.getCrewMember("Foreman1"));
    assertNotNull(manager.getCrewMember("Worker1"));
    assertNotNull(manager.getCrewMember("Worker2"));
}
```

---

### 4.4 Migration - Backward Compatibility

**Scenario:** Load old save file with deprecated fields

**Test Steps:**
1. Create NBT with old schema (SteveName instead of CrewName)
2. Load into current version
3. Verify backward compatibility handling
4. Verify old field names are mapped to new ones

**Expected Behaviors:**
- Old "SteveName" is read and mapped to "CrewName"
- No data loss from old saves
- Deprecation warnings in logs
- Entity functions correctly after migration

**Test Automation Approach:**
```java
@Test
void testBackwardCompatibility() {
    // Create old-style NBT
    CompoundTag oldTag = new CompoundTag();
    oldTag.putString("SteveName", "OldSteve");

    // Load into new entity
    ForemanEntity foreman = new ForemanEntity(entityType, testLevel);
    foreman.readAdditionalSaveData(oldTag);

    // Verify migration
    assertEquals("OldSteve", foreman.getEntityName());
}
```

---

## 5. GUI Interactions Tests

### 5.1 GUI Open/Close - Toggle Behavior

**Scenario:** Open and close GUI with K key

**Test Steps:**
1. Start game client
2. Press **K** key
3. Verify GUI slides in from right side
4. Verify animation is smooth (20 ticks)
5. Press **K** key again
6. Verify GUI slides out
7. Verify no screen artifacts

**Expected Behaviors:**
- `toggle()` is called on ForemanOfficeGUI
- `isOpen` state toggles correctly
- SlideOffset animates smoothly
- Screen overlay appears/disappears
- No blocking of game thread

**Test Automation Approach:**
```java
@Test
void testGUIToggle() {
    Minecraft mc = Minecraft.getInstance();

    // Press K key
    KeyEvent keyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, KeyEvent.VK_K);
    mc.keyboardHandler.setKeyRepeatEvents(true);

    ForemanOfficeGUI.toggle();

    assertTrue(ForemanOfficeGUI.isOpen());
    assertNotNull(mc.screen);
    assertTrue(mc.screen instanceof ForemanOverlayScreen);

    ForemanOfficeGUI.toggle();

    assertFalse(ForemanOfficeGUI.isOpen());
    assertNull(mc.screen);
}
```

---

### 5.2 Command Input - Text Entry

**Scenario:** Type and submit command via GUI

**Test Steps:**
1. Open GUI (press K)
2. Click on input box
3. Type: "Build a small house"
4. Verify text appears in input box
5. Press **Enter**
6. Verify command is submitted
7. Verify user message appears in chat history
8. Verify input box is cleared

**Expected Behaviors:**
- Input box receives focus
- Text is entered correctly
- Command is sent to ActionExecutor
- Message appears in GUI message history
- Input box clears after submission
- Command is added to history (for arrow keys)

**Test Automation Approach:**
```java
@Test
void testCommandInput() {
    ForemanOfficeGUI.toggle();
    EditBox inputBox = ForemanOfficeGUI.getInputBox();

    inputBox.setValue("Build a small house");
    ForemanOfficeGUI.handleKeyPress(KeyEvent.VK_ENTER, 0, 0);

    verify(commandExecutor).processNaturalLanguageCommand("Build a small house");
    assertEquals("", inputBox.getValue());

    List<ChatMessage> messages = ForemanOfficeGUI.getMessages();
    assertFalse(messages.isEmpty());
    assertEquals("You", messages.get(messages.size() - 1).sender);
}
```

---

### 5.3 Command History - Arrow Navigation

**Scenario:** Navigate through previous commands with arrow keys

**Test Steps:**
1. Submit 3 commands: "Mine stone", "Build house", "Craft pickaxe"
2. Press **Up Arrow**
3. Verify "Craft pickaxe" appears in input box
4. Press **Up Arrow** again
5. Verify "Build house" appears
6. Press **Down Arrow**
7. Verify "Craft pickaxe" appears again
8. Press **Down Arrow** again
9. Verify input box is empty

**Expected Behaviors:**
- Command history is maintained (max 50)
- Up arrow navigates backward in history
- Down arrow navigates forward
- History wraps correctly
- Index tracking is accurate

**Test Automation Approach:**
```java
@Test
void testCommandHistory() {
    ForemanOfficeGUI gui = new ForemanOfficeGUI();
    EditBox inputBox = gui.getInputBox();

    gui.sendCommand("Mine stone");
    gui.sendCommand("Build house");
    gui.sendCommand("Craft pickaxe");

    gui.handleKeyPress(KeyEvent.VK_UP, 0, 0);
    assertEquals("Craft pickaxe", inputBox.getValue());

    gui.handleKeyPress(KeyEvent.VK_UP, 0, 0);
    assertEquals("Build house", inputBox.getValue());

    gui.handleKeyPress(KeyEvent.VK_DOWN, 0, 0);
    assertEquals("Craft pickaxe", inputBox.getValue());

    gui.handleKeyPress(KeyEvent.VK_DOWN, 0, 0);
    assertEquals("", inputBox.getValue());
}
```

---

### 5.4 Message History - Scroll and Display

**Scenario:** View and scroll through message history

**Test Steps:**
1. Open GUI
2. Submit 10 commands
3. Verify messages appear in reverse chronological order
4. Verify latest message at bottom
5. Scroll up with mouse wheel
6. Verify scroll offset changes
7. Verify older messages become visible
8. Verify scrollbar appears
9. Verify scrollbar position reflects scroll

**Expected Behaviors:**
- Messages are stored (max 500)
- Messages display with sender, text, bubble color
- User messages right-aligned (green)
- Crew messages left-aligned (blue)
- System messages orange
- Scrollbar appears when messages overflow
- Scrolling is smooth and responsive

**Test Automation Approach:**
```java
@Test
void testMessageHistoryScroll() {
    ForemanOfficeGUI gui = new ForemanOfficeGUI();

    for (int i = 0; i < 10; i++) {
        gui.addUserMessage("Command " + i);
    }

    assertEquals(10, gui.getMessageCount());

    int initialOffset = gui.getScrollOffset();
    gui.handleMouseScroll(-5.0); // Scroll up
    assertTrue(gui.getScrollOffset() > initialOffset);

    gui.handleMouseScroll(5.0); // Scroll down
    assertEquals(0, gui.getScrollOffset()); // Back to bottom
}
```

---

### 5.5 Spawn Command - Via GUI

**Scenario:** Spawn new crew member via GUI

**Test Steps:**
1. Open GUI
2. Type: "spawn Builder"
3. Press Enter
4. Verify spawn command is executed
5. Verify system message: "Spawning crew member: Builder"
6. Verify new entity appears in world
7. Verify `/steve spawn Builder` is sent

**Expected Behaviors:**
- GUI detects spawn command prefix
- Sends `/steve spawn` command to server
- System message confirms spawn
- CrewManager creates new entity
- Entity appears at player location

**Test Automation Approach:**
```java
@Test
void testSpawnViaGUI() {
    ForemanOfficeGUI gui = new ForemanOfficeGUI();
    gui.sendCommand("spawn Builder");

    verify(player.connection).sendCommand("steve spawn Builder");

    List<ChatMessage> messages = gui.getMessages();
    ChatMessage systemMsg = messages.get(messages.size() - 1);
    assertEquals("System", systemMsg.sender);
    assertTrue(systemMsg.text.contains("Builder"));
}
```

---

### 5.6 Multi-Target Commands - Parsing

**Scenario:** Send commands to multiple crew members

**Test Steps:**
1. Spawn 3 crew: Alpha, Beta, Gamma
2. Open GUI
3. Type: "Alpha, Beta: Mine stone"
4. Submit command
5. Verify both Alpha and Beta receive command
6. Verify Gamma does not receive command
7. Type: "all crew: Stop"
8. Verify all 3 crew members receive command

**Expected Behaviors:**
- GUI parses comma-separated targets
- `all crew` targets all active members
- Each targeted member receives command
- GUI confirms targets in system message

**Test Automation Approach:**
```java
@Test
void testMultiTargetCommands() {
    spawnTestCrew("Alpha", "Beta", "Gamma");

    ForemanOfficeGUI gui = new ForemanOfficeGUI();
    gui.sendCommand("Alpha, Beta: Mine stone");

    verify(alpha.executor).processNaturalLanguageCommand("Mine stone");
    verify(beta.executor).processNaturalLanguageCommand("Mine stone");
    verify(gamma.executor, never()).processNaturalLanguageCommand(any());

    gui.sendCommand("all crew: Stop");

    verify(alpha.executor).stopCurrentAction();
    verify(beta.executor).stopCurrentAction();
    verify(gamma.executor).stopCurrentAction();
}
```

---

## 6. Action Execution Tests

### 6.1 MineBlockAction - Resource Gathering

**Scenario:** Foreman mines specified blocks

**Test Steps:**
1. Place 10 stone blocks at target location
2. Submit command: "Mine 10 stone"
3. Verify Foreman pathfinds to blocks
4. Verify Foreman breaks blocks
5. Verify items are collected
6. Verify completion message

**Expected Behaviors:**
- ActionExecutor creates MineBlockAction
- Action pathfinds to target
- Action breaks block (left-click simulation)
- Items are picked up
- Progress tracking (0-100%)
- Completion triggers next action

**Test Automation Approach:**
```java
@Test
void testMineBlockAction() {
    ForemanEntity foreman = spawnTestForeman();
    placeBlocks(Material.STONE, 10, targetPos);

    Task task = new Task("mine", Map.of(
        "block", "stone",
        "count", 10
    ));

    MineBlockAction action = new MineBlockAction(foreman, task);
    action.start();

    while (!action.isComplete()) {
        action.tick();
        tickServer();
    }

    ActionResult result = action.getResult();
    assertTrue(result.isSuccess());
    assertEquals(0, countBlocks(Material.STONE, targetPos));
}
```

---

### 6.2 BuildStructureAction - Construction

**Scenario:** Foreman builds a structure

**Test Steps:**
1. Submit command: "Build a 5x5 cobblestone square"
2. Verify LLM generates structure plan
3. Verify BuildStructureAction is created
4. Verify Foreman places blocks according to plan
5. Verify structure matches specifications
6. Verify all blocks are correct type

**Expected Behaviors:**
- StructureGenerators creates block placements
- Action executes placements sequentially
- Pathfinding between block positions
- Correct block type is placed
- Structure is complete and accurate

**Test Automation Approach:**
```java
@Test
void testBuildStructureAction() {
    ForemanEntity foreman = spawnTestForeman();

    Task task = new Task("build", Map.of(
        "structure", "square",
        "size", 5,
        "material", "cobblestone"
    ));

    BuildStructureAction action = new BuildStructureAction(foreman, task);
    action.start();

    while (!action.isComplete()) {
        action.tick();
        tickServer();
    }

    assertTrue(action.getResult().isSuccess());
    assertTrue(validateStructure(foreman.level(), 5, 5, Material.COBBLESTONE));
}
```

---

### 6.3 PathfindAction - Navigation

**Scenario:** Foreman navigates to target location

**Test Steps:**
1. Place Foreman at (0, 64, 0)
2. Submit command: "Go to (100, 64, 100)"
3. Verify PathfindAction is created
4. Verify Foreman navigates around obstacles
5. Verify Foreman reaches target
6. Verify completion message

**Expected Behaviors:**
- Minecraft pathfinding is used
- Obstacles are avoided
- Entity moves toward target each tick
- Action completes when close enough (<2 blocks)
- Timeout after 30 seconds

**Test Automation Approach:**
```java
@Test
void testPathfindAction() {
    ForemanEntity foreman = spawnTestForeman(new BlockPos(0, 64, 0));
    BlockPos target = new BlockPos(100, 64, 100);

    Task task = new Task("pathfind", Map.of(
        "x", target.getX(),
        "y", target.getY(),
        "z", target.getZ()
    ));

    PathfindAction action = new PathfindAction(foreman, task);
    action.start();

    while (!action.isComplete()) {
        action.tick();
        tickServer();
        assertFalse(action.isExpired());
    }

    BlockPos finalPos = foreman.blockPosition();
    assertTrue(finalPos.distSqr(target) < 4.0);
}
```

---

### 6.4 CraftItemAction - Crafting

**Scenario:** Foreman crafts an item

**Test Steps:**
1. Provide crafting table and materials
2. Submit command: "Craft a stone pickaxe"
3. Verify CraftItemAction is created
4. Verify Foreman pathfinds to crafting table
5. Verify recipe is executed
6. Verify item is crafted
7. Verify item is in inventory

**Expected Behaviors:**
- Recipe is looked up
- Required materials are checked
- Pathfinding to crafting table
- Crafting simulation
- Item added to inventory
- Materials consumed

**Test Automation Approach:**
```java
@Test
void testCraftItemAction() {
    ForemanEntity foreman = spawnTestForeman();
    giveItem(foreman, Items.COBBLESTONE, 3);
    giveItem(foreman, Items.STICK, 2);
    placeCraftingTable(foreman.blockPosition().above());

    Task task = new Task("craft", Map.of(
        "item", "stone_pickaxe"
    ));

    CraftItemAction action = new CraftItemAction(foreman, task);
    action.start();

    while (!action.isComplete()) {
        action.tick();
        tickServer();
    }

    assertTrue(action.getResult().isSuccess());
    assertTrue(hasItem(foreman, Items.STONE_PICKAXE));
}
```

---

## 7. Memory & Context Tests

### 7.1 ForemanMemory - Recent Actions

**Scenario:** Foreman remembers recent actions

**Test Steps:**
1. Execute 25 different actions
2. Verify only last 20 are stored
3. Retrieve last 5 actions
4. Verify correct order
5. Verify oldest action is dropped

**Expected Behaviors:**
- MAX_RECENT_ACTIONS = 20
- Actions added to tail of queue
- Oldest removed when queue full
- getRecentActions() returns correct subset

**Test Automation Approach:**
```java
@Test
void testRecentActions() {
    ForemanEntity foreman = spawnTestForeman();
    ForemanMemory memory = foreman.getMemory();

    for (int i = 0; i < 25; i++) {
        memory.addAction("Action " + i);
    }

    List<String> recent = memory.getRecentActions(20);
    assertEquals(20, recent.size());
    assertEquals("Action 24", recent.get(19));
    assertEquals("Action 5", recent.get(0));
}
```

---

### 7.2 CompanionMemory - Relationship Growth

**Scenario:** Relationship develops through interactions

**Test Steps:**
1. Spawn new Foreman
2. Interact 50 times (commands, completions)
3. Verify affection increases
4. Verify trust level increases
5. Verify relationship title changes
6. Verify inside jokes develop
7. Verify milestones are tracked

**Expected Behaviors:**
- Affection starts at 0, max 100
- Each interaction increases affection
- Trust level increases with positive interactions
- Relationship titles: Stranger → Acquaintance → Friend → Close Friend → Best Friend
- Inside jokes accumulate (max 10)
- Milestones are recorded

**Test Automation Approach:**
```java
@Test
void testRelationshipGrowth() {
    ForemanEntity foreman = spawnTestForeman();
    CompanionMemory memory = foreman.getCompanionMemory();

    assertEquals(0, memory.getRelationship().getAffection());
    assertEquals("Stranger", getRelationshipTitle(memory));

    for (int i = 0; i < 50; i++) {
        memory.recordInteraction("positive");
    }

    assertTrue(memory.getRelationship().getAffection() > 30);
    assertTrue(memory.getRelationship().getTrust() > 0);
    assertEquals("Friend", getRelationshipTitle(memory));
}
```

---

### 7.3 Memory Persistence - NBT Save/Load

**Scenario:** Memory survives world reload

**Test Steps:**
1. Build up relationship and history
2. Save world
3. Reload world
4. Verify affection is preserved
5. Verify recent actions are preserved
6. Verify inside jokes are preserved
7. Verify milestones are preserved

**Expected Behaviors:**
- All memory fields saved to NBT
- All memory fields restored on load
- No data loss
- Relationship continues from saved state

**Test Automation Approach:**
```java
@Test
void testMemoryPersistence() {
    ForemanEntity foreman = spawnTestForeman();
    CompanionMemory memory = foreman.getCompanionMemory();

    memory.getRelationship().increaseAffection(50);
    memory.addInsideJoke("Test joke");
    memory.recordMilestone("First achievement");

    CompoundTag tag = new CompoundTag();
    memory.saveToNBT(tag);

    CompanionMemory newMemory = new CompanionMemory();
    newMemory.loadFromNBT(tag);

    assertEquals(50, newMemory.getRelationship().getAffection());
    assertEquals(1, newMemory.getInsideJokeCount());
    assertEquals(1, newMemory.getMilestones().size());
}
```

---

## Test Automation Framework

### Recommended Tools

**Unit/Integration Testing:**
- JUnit 5 - Test framework
- Mockito - Mocking framework
- Awaitility - Async assertions
- Minecraft Forge Test Framework - Mock Minecraft objects

**End-to-End Testing:**
- Selenium-like approach for GUI
- Scriptable test client
- Automated server spawning
- World state snapshots

### Test Structure

```
src/test/java/
├── integration/
│   ├── command/
│   │   ├── SimpleCommandTest.java
│   │   ├── ComplexCommandTest.java
│   │   └── InvalidCommandTest.java
│   ├── coordination/
│   │   ├── SingleAgentTest.java
│   │   ├── ForemanWorkerTest.java
│   │   └── MultiAgentTest.java
│   ├── resilience/
│   │   ├── CircuitBreakerTest.java
│   │   ├── RateLimiterTest.java
│   │   └── RetryTest.java
│   ├── persistence/
│   │   ├── EntitySaveTest.java
│   │   ├── EntityLoadTest.java
│   │   └── WorldReloadTest.java
│   ├── gui/
│   │   ├── GUIToggleTest.java
│   │   ├── CommandInputTest.java
│   │   └── MessageHistoryTest.java
│   └── actions/
│       ├── MineBlockActionTest.java
│       ├── BuildStructureActionTest.java
│       └── CraftItemActionTest.java
├── util/
│   ├── TestServer.java
│   ├── ForemanEntityBuilder.java
│   └── WorldValidator.java
└── base/
    ├── IntegrationTestBase.java
    └── MockMinecraftServer.java
```

### Test Utilities

**ForemanEntityBuilder:**
```java
public class ForemanEntityBuilder {
    private String name = "TestForeman";
    private BlockPos position = new BlockPos(0, 64, 0);
    private ServerLevel level;

    public ForemanEntityBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ForemanEntityBuilder position(BlockPos pos) {
        this.position = pos;
        return this;
    }

    public ForemanEntity build() {
        ForemanEntity foreman = new ForemanEntity(
            MineWrightMod.FOREMAN_ENTITY.get(), level
        );
        foreman.setEntityName(name);
        foreman.setPos(position.getX(), position.getY(), position.getZ());
        level.addFreshEntity(foreman);
        return foreman;
    }
}
```

**Awaitility Configuration:**
```java
public class TestAwaitility {
    static {
        Awaitility.setDefaultTimeout(60, TimeUnit.SECONDS);
        Awaitility.setDefaultPollInterval(1, TimeUnit.SECONDS);
        Awaitility.setDefaultPollDelay(100, TimeUnit.MILLISECONDS);
    }
}
```

---

## Manual Testing Checklist

### Pre-Test Setup
- [ ] Minecraft server started
- [ ] MineWright mod loaded
- [ ] API keys configured (OpenAI/Groq/Gemini)
- [ ] Test world prepared (flat terrain for building)
- [ ] Creative mode for quick setup

### Command Flow
- [ ] Spawn Foreman entity
- [ ] Open GUI with K key
- [ ] Submit simple command
- [ ] Verify non-blocking (game continues during planning)
- [ ] Verify task executes
- [ ] Verify completion message

### Multi-Agent
- [ ] Spawn 3 crew members
- [ ] Assign complex task
- [ ] Verify task distribution
- [ ] Verify parallel execution
- [ ] Remove worker mid-task
- [ ] Verify task reassignment

### Failure Handling
- [ ] Disconnect internet (simulate network failure)
- [ ] Submit command
- [ ] Verify circuit breaker opens
- [ ] Verify fallback response
- [ ] Reconnect internet
- [ ] Verify circuit breaker closes after timeout

### Persistence
- [ ] Execute multiple commands
- [ ] Build relationship
- [ ] Save and quit
- [ ] Reload world
- [ ] Verify all state restored

### GUI
- [ ] Test all keyboard shortcuts
- [ ] Test command history (up/down arrows)
- [ ] Test message scrolling
- [ ] Test multi-target commands
- [ ] Test spawn command via GUI

---

## Test Coverage Goals

**Target Coverage:**
- Unit tests: 80%+ line coverage
- Integration tests: 70%+ line coverage
- Critical paths: 100% coverage

**Priority Areas:**
1. ActionExecutor - critical for all commands
2. ResilientLLMClient - critical for reliability
3. OrchestratorService - critical for coordination
4. ForemanMemory/CompanionMemory - critical for persistence
5. GUI - critical for user experience

**Coverage Exclusions:**
- Minecraft/Forge framework code
- Third-party libraries (Resilience4j, Caffeine)
- Generated code

---

## Continuous Integration

### GitHub Actions Workflow

```yaml
name: Integration Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build with Gradle
      run: ./gradlew build --no-daemon

    - name: Run unit tests
      run: ./gradlew test --no-daemon

    - name: Run integration tests
      run: ./gradlew integrationTest --no-daemon
      env:
        OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
        GROQ_API_KEY: ${{ secrets.GROQ_API_KEY }}

    - name: Upload test results
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: test-results
        path: build/test-results/test/
```

---

## Conclusion

This test suite provides comprehensive coverage of MineWright's integration points:

- **End-to-end command flow** ensures user commands are executed correctly
- **Multi-agent coordination** validates the orchestration system
- **LLM failure handling** ensures resilience against provider issues
- **World persistence** guarantees state survival across reloads
- **GUI interactions** validate the user interface
- **Action execution** tests individual behaviors
- **Memory & context** verify state management

**Next Steps:**
1. Implement test utilities (TestServer, ForemanEntityBuilder)
2. Write initial integration tests for critical paths
3. Set up CI/CD pipeline
4. Add tests incrementally with new features
5. Maintain test coverage >70%

---

**Document Version:** 1.0.0
**Last Updated:** 2026-02-27
**Author:** MineWright Development Team
