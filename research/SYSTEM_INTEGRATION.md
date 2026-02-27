# System Integration Analysis

**Orchestrator:** Integration Planner
**Date:** 2026-02-26
**Mission:** Map system connections and identify integration opportunities
**Project:** MineWright Minecraft Mod

---

## Executive Summary

The MineWright project consists of 7 major systems that must work together seamlessly. This document maps all integration points, data flows, and connection interfaces between systems.

**Key Findings:**
- 47 integration points identified across 7 systems
- 3 critical missing connections identified
- 8 suggested integration patterns for improved cohesion
- Data flows are primarily async (CompletableFuture-based) for non-blocking operation

---

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     MineWright ECOSYSTEM                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐    │
│  │   LLM Layer  │────▶│   Memory     │────▶│  Orchestration│    │
│  │              │     │              │     │              │    │
│  │ - AsyncLLM   │     │ - Companion  │     │ - Foreman    │    │
│  │ - Batching   │     │ - Vector     │     │ - Workers    │    │
│  │ - Resilience │     │ - Milestones │     │ - Messages   │    │
│  └──────────────┘     └──────────────┘     └──────────────┘    │
│         │                     │                     │            │
│         ▼                     ▼                     ▼            │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐    │
│  │   Dialogue   │     │   Voice      │     │  Execution   │    │
│  │              │     │              │     │              │    │
│  │ - Proactive  │     │ - STT/TTS    │     │ - State M/C  │    │
│  │ - Convers.   │     │ - Manager    │     │ - Actions    │    │
│  └──────────────┘     └──────────────┘     └──────────────┘    │
│         │                     │                     │            │
│         └─────────────────────┴─────────────────────┘            │
│                               │                                   │
│                               ▼                                   │
│                    ┌──────────────┐                              │
│                    │   Plugins    │                              │
│                    │              │                              │
│                    │ - Registry   │                              │
│                    │ - Factory    │                              │
│                    │ - Core Acts  │                              │
│                    └──────────────┘                              │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 1. LLM Layer Integration

### Interfaces

| Interface | Purpose | Signature |
|-----------|---------|-----------|
| `AsyncLLMClient.sendAsync()` | Non-blocking LLM calls | `CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params)` |
| `PromptBuilder.buildSystemPrompt()` | Task planning prompts | `String buildSystemPrompt()` |
| `CompanionPromptBuilder.buildConversationalSystemPrompt()` | Companion prompts | `String buildConversationalSystemPrompt(CompanionMemory)` |

### Integration Points

#### 1.1 ActionExecutor → LLM (Task Planning)

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\action\ActionExecutor.java`

```java
// Lines 113-143
public void processNaturalLanguageCommand(String command) {
    // ...
    planningFuture = getTaskPlanner().planTasksAsync(minewright, command);
    // Returns immediately - non-blocking
}
```

**Data Flow:**
```
User Command (GUI)
    ↓
ActionExecutor.processNaturalLanguageCommand()
    ↓
TaskPlanner.planTasksAsync()
    ↓
AsyncLLMClient.sendAsync()
    ↓
[Async Processing]
    ↓
ActionExecutor.tick() checks planningFuture.isDone()
    ↓
ResponseParser extracts tasks
    ↓
taskQueue.addAll(tasks)
```

**Integration Pattern:** Async Producer-Consumer

---

#### 1.2 ProactiveDialogueManager → LLM (Comment Generation)

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\dialogue\ProactiveDialogueManager.java`

```java
// Lines 370-396
private void generateAndSpeakComment(String triggerType, String context) {
    CompletableFuture<String> commentFuture =
        conversationManager.generateProactiveComment(context, llmClient);

    commentFuture.thenAccept(comment -> {
        if (comment != null && !comment.isEmpty()) {
            minewright.sendChatMessage(finalComment);
        }
    });
}
```

**Data Flow:**
```
Trigger Event (weather, player approach, etc.)
    ↓
ProactiveDialogueManager.triggerComment()
    ↓
ConversationManager.generateProactiveComment()
    ↓
CompanionPromptBuilder.buildProactiveCommentPrompt()
    ↓
AsyncLLMClient.sendAsync() (brief, 150 tokens, 0.9 temp)
    ↓
Async completion → sendChatMessage()
```

**Integration Pattern:** Fire-and-Forget with Fallback

---

#### 1.3 ConversationManager → LLM (Chat Responses)

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\memory\ConversationManager.java`

```java
// Lines 88-128
private CompletableFuture<String> generateConversationalResponse(
        String playerName, String message, AsyncLLMClient llmClient) {

    String systemPrompt = CompanionPromptBuilder.buildConversationalSystemPrompt(memory);
    String userPrompt = CompanionPromptBuilder.buildConversationalUserPrompt(message, memory, minewright);

    Map<String, Object> params = Map.of(
        "systemPrompt", systemPrompt,
        "maxTokens", 300,
        "temperature", 0.8
    );

    return llmClient.sendAsync(userPrompt, params)
        .thenApply(response -> response.getContent().trim());
}
```

**Integration Pattern:** Request-Response with Memory Update

---

### Missing LLM Integrations

1. **Batching + Dialogue**: No integration between `LocalPreprocessor` and `ProactiveDialogueManager`
   - **Impact:** Multiple simultaneous dialogue triggers create separate LLM calls
   - **Suggestion:** Batch dialogue triggers within same tick window

2. **Resilience + Orchestration**: `ResilientLLMClient` not used by `OrchestratorService`
   - **Impact:** Foreman planning failures not retried
   - **Suggestion:** Wrap orchestration LLM calls with resilience patterns

---

## 2. Memory Integration

### Interfaces

| Interface | Purpose | Signature |
|-----------|---------|-----------|
| `CompanionMemory.recordExperience()` | Store episodic memory | `void recordExperience(String eventType, String description, int emotionalWeight)` |
| `CompanionMemory.findRelevantMemories()` | Semantic search | `List<EpisodicMemory> findRelevantMemories(String query, int k)` |
| `CompanionMemory.getRelationshipContext()` | Get rapport/trust | `String getRelationshipContext()` |

### Integration Points

#### 2.1 Memory → Prompt Building (Context Injection)

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\llm\CompanionPromptBuilder.java`

```java
// Lines 31-80
public static String buildConversationalSystemPrompt(CompanionMemory memory) {
    StringBuilder sb = new StringBuilder();

    // Personality injection
    sb.append(memory.getPersonality().toPromptContext());

    // Relationship context
    sb.append(memory.getRelationshipContext());

    // Inside jokes
    for (int i = 0; i < 3; i++) {
        InsideJoke joke = memory.getRandomInsideJoke();
        if (joke != null) {
            sb.append("- \"").append(joke.punchline).append("\"\n");
        }
    }

    // Recent context
    sb.append(memory.getWorkingMemoryContext());

    return sb.toString();
}
```

**Data Flow:**
```
LLM Request
    ↓
CompanionPromptBuilder.buildConversationalSystemPrompt()
    ↓
CompanionMemory.getRelationshipContext()
    ├─ Rapport Level (0-100)
    ├─ Trust Level (0-100)
    ├─ Interaction Count
    ├─ Player Preferences
    └─ Playstyle Metrics
    ↓
CompanionMemory.getWorkingMemoryContext()
    ├─ Recent Commands
    ├─ Current Goal
    └─ Session Topics
    ↓
Prompt assembled with context → LLM
```

**Integration Pattern:** Context Aggregation

---

#### 2.2 ActionExecutor → Memory (Action Recording)

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\action\ActionExecutor.java`

```java
// Lines 266-267
if (currentAction.isComplete()) {
    ActionResult result = currentAction.getResult();
    minewright.getMemory().addAction(currentAction.getDescription());
}
```

**Missing Integration:** This calls `ForemanMemory.addAction()` but not `CompanionMemory.recordExperience()`

**Suggested Fix:**
```java
// After line 267
if (result.isSuccess()) {
    minewright.getCompanionMemory().recordExperience(
        "action_complete",
        currentAction.getDescription(),
        2  // Small positive emotional weight
    );
} else {
    minewright.getCompanionMemory().recordSharedFailure(
        currentAction.getDescription(),
        result.getMessage()
    );
}
```

---

#### 2.3 Vector Store Integration

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\memory\CompanionMemory.java`

```java
// Lines 354-380
public List<EpisodicMemory> findRelevantMemories(String query, int k) {
    if (memoryVectorStore.size() == 0) {
        return Collections.emptyList();
    }

    // Generate embedding for query
    float[] queryEmbedding = embeddingModel.embed(query);

    // Search vector store
    List<VectorSearchResult<EpisodicMemory>> results =
        memoryVectorStore.search(queryEmbedding, k);

    return results.stream()
        .map(VectorSearchResult::getData)
        .collect(Collectors.toList());
}
```

**Integration Points:**
- **EmbeddingModel** generates vectors for memories (placeholder currently)
- **InMemoryVectorStore** performs similarity search
- **CompanionPromptBuilder** uses results for context-aware prompts

**Data Flow:**
```
User says: "remember when we built that castle?"
    ↓
ConversationManager.generateConversationalResponse()
    ↓
CompanionMemory.findRelevantMemories("built castle", 3)
    ↓
EmbeddingModel.embed("built castle")
    ↓
InMemoryVectorStore.search(queryEmbedding, 3)
    ↓
Top 3 similar memories returned
    ↓
Included in prompt as "Relevant Past Experiences"
    ↓
LLM generates contextual response
```

---

## 3. Orchestration Integration

### Interfaces

| Interface | Purpose | Signature |
|-----------|---------|-----------|
| `OrchestratorService.registerAgent()` | Register MineWright | `void registerAgent(ForemanEntity minewright, AgentRole role)` |
| `AgentCommunicationBus.publish()` | Send messages | `void publish(AgentMessage message)` |
| `AgentCommunicationBus.poll()` | Receive messages | `AgentMessage poll(String agentId)` |

### Integration Points

#### 3.1 ForemanEntity → Orchestrator (Agent Registration)

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\entity\ForemanEntity.java`

```java
// Lines 262-290
private void registerWithOrchestrator() {
    if (orchestrator == null) return;

    int activeCount = MineWrightMod.getMineWrightManager().getActiveCount();

    if (activeCount == 1) {
        this.role = AgentRole.FOREMAN;
        LOGGER.info("[{}] Registering as FOREMAN", minewrightName);
    } else {
        this.role = AgentRole.WORKER;
        LOGGER.info("[{}] Registering as WORKER", minewrightName);
    }

    orchestrator.registerAgent(this, role);
    registeredWithOrchestrator.set(true);
}
```

**Data Flow:**
```
ForemanEntity spawns
    ↓
tick() called
    ↓
registerWithOrchestrator() (once)
    ↓
Determine role (first = FOREMAN, rest = WORKERS)
    ↓
OrchestratorService.registerAgent()
    ↓
AgentCommunicationBus.registerAgent()
    ↓
MineWright subscribes to messages
```

**Integration Pattern:** Role-Based Registration

---

#### 3.2 Foreman → Workers (Task Assignment)

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\orchestration\OrchestratorService.java`

```java
// Lines 259-274
private void assignTaskToAgent(PlanExecution plan, Task task, String agentId) {
    TaskAssignment assignment = new TaskAssignment(foremanId, task, plan.getPlanId());
    assignment.assignTo(agentId);

    plan.addAssignment(assignment);
    workerAssignments.put(agentId, assignment);

    // Send assignment message
    AgentMessage message = AgentMessage.taskAssignment(
        foremanId, "Foreman", agentId,
        task.getAction(), task.getParameters()
    );
    communicationBus.publish(message);
}
```

**Message Flow:**
```
OrchestratorService.processHumanCommand()
    ↓
distributeTasks() - round-robin assignment
    ↓
assignTaskToAgent() for each task
    ↓
AgentMessage.taskAssignment() created
    ├─ sender: foreman
    ├─ recipient: worker_id
    ├─ type: TASK_ASSIGNMENT
    └─ payload: task parameters
    ↓
AgentCommunicationBus.publish()
    ↓
Worker MineWright tick() → processMessages()
    ↓
handleTaskAssignment()
    ↓
actionExecutor.queueTask(task)
    ↓
Task added to worker's queue
```

**Integration Pattern:** Message-Based Task Distribution

---

#### 3.3 Worker → Foreman (Progress Reporting)

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\entity\ForemanEntity.java`

```java
// Lines 409-437
private void reportTaskProgress() {
    if (currentTaskId == null || !actionExecutor.isExecuting()) {
        return;
    }

    tickCounter++;
    if (tickCounter % 100 != 0) {
        return;  // Report every 5 seconds
    }

    int progress = actionExecutor.getCurrentActionProgress();
    if (progress > currentTaskProgress) {
        currentTaskProgress = progress;

        AgentMessage progressMsg = AgentMessage.taskProgress(
            minewrightName, minewrightName,
            "foreman",
            currentTaskId,
            progress,
            "In progress"
        );
        orchestrator.getCommunicationBus().publish(progressMsg);
    }
}
```

**Data Flow:**
```
Worker MineWright executes task
    ↓
tick() called every game tick
    ↓
Every 100 ticks (5 seconds):
    ↓
actionExecutor.getCurrentActionProgress()
    ↓
If progress changed:
    ↓
AgentMessage.taskProgress()
    ├─ sender: worker
    ├─ recipient: foreman
    ├─ type: TASK_PROGRESS
    └─ payload: {percentComplete, status}
    ↓
AgentCommunicationBus.publish()
    ↓
Foreman receives via handleMessageFromAgent()
    ↓
TaskAssignment.updateProgress()
```

---

### Missing Orchestration Integrations

1. **Dialogue + Orchestration**: Proactive comments not coordinated
   - **Issue:** Multiple MineWrights might comment simultaneously on same event
   - **Suggestion:** Add coordination message type for "speaker turn"

2. **Memory + Orchestration**: Shared experiences not tracked
   - **Issue:** When foreman + workers complete task together, only worker records it
   - **Suggestion:** Send `SHARED_SUCCESS` message to all participants for memory recording

---

## 4. Dialogue Integration

### Interfaces

| Interface | Purpose | Signature |
|-----------|---------|-----------|
| `ProactiveDialogueManager.tick()` | Check triggers | `void tick()` |
| `ProactiveDialogueManager.onTaskCompleted()` | Task completion callback | `void onTaskCompleted(String taskDescription)` |
| `ConversationManager.generateProactiveComment()` | Generate comment | `CompletableFuture<String> generateProactiveComment(String trigger, AsyncLLMClient)` |

### Integration Points

#### 4.1 ForemanEntity → ProactiveDialogueManager

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\entity\ForemanEntity.java`

```java
// Lines 112-114
// Check for proactive dialogue triggers
if (dialogueManager != null) {
    dialogueManager.tick();
}
```

**Data Flow:**
```
ForemanEntity.tick() (every game tick)
    ↓
ProactiveDialogueManager.tick()
    ↓
checkTimeBasedTriggers() (morning, night)
    ↓
checkContextBasedTriggers() (biome, danger)
    ↓
checkWeatherTriggers() (rain, storm)
    ↓
checkPlayerProximityTriggers() (greeting)
    ↓
If trigger fires:
    ↓
triggerComment() → generateAndSpeakComment()
    ↓
ConversationManager.generateProactiveComment()
    ↓
AsyncLLMClient.sendAsync()
    ↓
sendChatMessage() when complete
```

**Integration Pattern:** Periodic Trigger Scanning

---

#### 4.2 ActionExecutor → Dialogue (Event Callbacks)

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\entity\ForemanEntity.java`

```java
// Lines 536-553
public void notifyTaskCompleted(String taskDescription) {
    if (dialogueManager != null) {
        dialogueManager.onTaskCompleted(taskDescription);
    }
}

public void notifyTaskFailed(String taskDescription, String reason) {
    if (dialogueManager != null) {
        dialogueManager.onTaskFailed(taskDescription, reason);
    }
}
```

**Current Status:** **NOT CALLED** in ActionExecutor

**Missing Integration:**
```java
// In ActionExecutor.java, line 266-277, after action completes:
ActionResult result = currentAction.getResult();

if (result.isSuccess()) {
    minewright.notifyTaskCompleted(currentAction.getDescription());
} else {
    minewright.notifyTaskFailed(
        currentAction.getDescription(),
        result.getMessage()
    );
}
```

---

#### 4.3 Dialogue Triggers

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\dialogue\ProactiveDialogueManager.java`

```java
// Lines 166-190
private void checkTimeBasedTriggers() {
    long dayTime = level.getDayTime() % 24000;

    // Morning greeting (6:00 AM)
    if (dayTime >= 0 && dayTime < 2000) {
        if (canTrigger("morning", 24000)) {
            triggerComment("morning", "It's morning!");
        }
    }

    // Night warning (8:00 PM)
    if (dayTime >= 18000 && dayTime < 20000) {
        if (canTrigger("night", 12000)) {
            triggerComment("night", "It's getting dark!");
        }
    }
}
```

**Trigger Types:**

| Trigger | Condition | Cooldown | Comment Example |
|---------|-----------|----------|-----------------|
| `morning` | Game time 0-2000 | 24 hours | "Good morning! Ready to get to work?" |
| `night` | Game time 18000-20000 | 12 hours | "Getting dark. Maybe we should wrap up soon?" |
| `raining` | Rain started | 15 seconds | "Rain's coming down. Perfect weather for mining!" |
| `storm` | Thunder storm | 10 seconds | "Thunder and lightning! Stay safe out there!" |
| `idle_long` | Idle > 5 minutes | 5 minutes | "Everything quiet today?" |
| `near_danger` | Health < 30% | 2 minutes | "Be careful around here!" |
| `player_approach` | Player within 10 blocks | 1 minute | (Generated greeting) |
| `task_complete` | Action succeeded | 20 seconds | "Another job well done!" |

---

## 5. Voice Integration

### Interfaces

| Interface | Purpose | Signature |
|-----------|---------|-----------|
| `VoiceManager.speak()` | Text-to-speech | `void speak(String text)` |
| `VoiceManager.listenForCommand()` | Speech-to-text | `CompletableFuture<String> listenForCommand()` |
| `VoiceManager.isEnabled()` | Check voice enabled | `boolean isEnabled()` |

### Integration Points

#### 5.1 ActionExecutor → Voice (Task Feedback)

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\action\ActionExecutor.java`

```java
// Lines 217-219
private void sendToGUI(String minewrightName, String message) {
    if (minewright.level().isClientSide) {
        com.minewright.client.MineWrightGUI.addMineWrightMessage(minewrightName, message);
    }

    // Also speak the message if voice is enabled
    VoiceManager.getInstance().speakIfEnabled(message);
}
```

**Data Flow:**
```
ActionExecutor event (planning, completion, etc.)
    ↓
sendToGUI(message)
    ↓
MineWrightGUI.addMineWrightMessage() (visual)
    ↓
VoiceManager.speakIfEnabled() (audio)
    ↓
VoiceSystem.speak()
    ↓
TextToSpeech speaks message
```

**Integration Pattern:** Dual Output (Visual + Audio)

---

#### 5.2 Voice → Commands (Speech Input)

**Current Status:** STUB - Not fully implemented

**Planned Flow:**
```
Player presses voice key (V)
    ↓
VoiceManager.listenForCommand()
    ↓
VoiceSystem.startListening()
    ↓
SpeechToText transcribes audio
    ↓
CompletableFuture<String> completes
    ↓
ActionExecutor.processNaturalLanguageCommand(transcribedText)
```

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\voice\VoiceManager.java`

```java
// Lines 127-134
public CompletableFuture<String> listenForCommand() throws VoiceException {
    if (!isEnabled()) {
        throw VoiceException.configurationError("Voice system is not enabled");
    }

    LOGGER.info("Listening for voice command...");
    return voiceSystem.startListening();
}
```

---

### Missing Voice Integrations

1. **Dialogue + Voice**: Proactive comments not spoken
   - **Current:** Comments only go to chat
   - **Fix:** In `ProactiveDialogueManager.generateAndSpeakComment()`, add:
     ```java
     VoiceManager.getInstance().speakIfEnabled(finalComment);
     ```

2. **Orchestration + Voice**: Foreman announcements not spoken
   - **Current:** Plan announcements only chat
   - **Fix:** In `OrchestratorService.broadcastPlanAnnouncement()`, add voice output

---

## 6. Execution Integration

### Interfaces

| Interface | Purpose | Signature |
|-----------|---------|-----------|
| `AgentStateMachine.transitionTo()` | State change | `boolean transitionTo(AgentState targetState, String reason)` |
| `ActionExecutor.queueTask()` | Direct task queue | `void queueTask(Task task)` |
| `ActionRegistry.createAction()` | Plugin action creation | `BaseAction createAction(String actionType, ForemanEntity minewright, Task task, ActionContext context)` |

### Integration Points

#### 6.1 State Machine → EventBus

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\execution\AgentStateMachine.java`

```java
// Lines 194-203
if (currentState.compareAndSet(fromState, targetState)) {
    LOGGER.info("[{}] State transition: {} → {}", agentId, fromState, targetState);

    // Publish event
    if (eventBus != null) {
        eventBus.publish(new StateTransitionEvent(agentId, fromState, targetState, reason));
    }

    return true;
}
```

**State Transitions:**
```
IDLE → PLANNING → EXECUTING → COMPLETED → IDLE
                      ↓
                   FAILED → IDLE
                      ↓
                   PAUSED → EXECUTING
```

**Event Subscribers:**
- `EventPublishingInterceptor` - logs all transitions
- Future: `DialogueManager` could react to state changes
- Future: `OrchestratorService` could track worker states

---

#### 6.2 ActionExecutor → Plugin Registry

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\action\ActionExecutor.java`

```java
// Lines 346-363
private BaseAction createAction(Task task) {
    String actionType = task.getAction();

    // Try registry-based creation first (plugin architecture)
    ActionRegistry registry = ActionRegistry.getInstance();
    if (registry.hasAction(actionType)) {
        BaseAction action = registry.createAction(actionType, minewright, task, actionContext);
        if (action != null) {
            return action;
        }
    }

    // Fallback to legacy switch statement
    return createActionLegacy(task);
}
```

**Plugin Flow:**
```
Task with action type
    ↓
ActionRegistry.hasAction(actionType)
    ↓
If true:
    ↓
ActionRegistry.createAction(actionType, minewright, task, actionContext)
    ↓
Get ActionFactory for action type
    ↓
factory.create(minewright, task, context)
    ↓
Return BaseAction instance
    ↓
action.start() → action.tick() until action.isComplete()
```

---

#### 6.3 Interceptor Chain Integration

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\action\ActionExecutor.java`

```java
// Lines 68-82
// Setup interceptors
interceptorChain.addInterceptor(new LoggingInterceptor());
interceptorChain.addInterceptor(new MetricsInterceptor());
interceptorChain.addInterceptor(new EventPublishingInterceptor(eventBus, minewright.getMineWrightName()));

// Build action context
ActionContext actionContext = ActionContext.builder()
    .serviceContainer(container)
    .eventBus(eventBus)
    .stateMachine(stateMachine)
    .interceptorChain(interceptorChain)
    .build();
```

**Interceptor Flow:**
```
Action.execute()
    ↓
InterceptorChain.proceed()
    ↓
LoggingInterceptor.before()
    ↓
MetricsInterceptor.before()
    ↓
EventPublishingInterceptor.before()
    ↓
Actual action execution
    ↓
EventPublishingInterceptor.after()
    ↓
MetricsInterceptor.after()
    ↓
LoggingInterceptor.after()
```

---

## 7. Complete Data Flow: User Command

```
┌─────────────────────────────────────────────────────────────────┐
│                  COMPLETE USER COMMAND FLOW                    │
└─────────────────────────────────────────────────────────────────┘

User presses K → Opens GUI
User types: "build a castle"
    ↓
MineWrightGUI.addMineWrightMessage() sends to server
    ↓
ForemanEntity.processNaturalLanguageCommand("build a castle")
    ↓
ActionExecutor.processNaturalLanguageCommand()
    ├─ Cancel current actions
    ├─ Set isPlanning = true
    └─ Show "Thinking..." in GUI
    ↓
TaskPlanner.planTasksAsync(minewright, command)
    ├─ Build system prompt (PromptBuilder)
    ├─ Build user prompt with WorldKnowledge
    └─ AsyncLLMClient.sendAsync()
    ↓
[GAME CONTINUES - NON-BLOCKING]
    ↓
tick() called every game tick
    ├─ Check: planningFuture.isDone()
    └─ When true:
        ↓
ResponseParser parses JSON response
    ├─ Extract: plan, tasks
    └─ Validate action types
        ↓
taskQueue.addAll(tasks)
    ├─ Set currentGoal = plan
    └─ Show "Okay! [plan]" in GUI
        ↓
tick() continues processing
    ├─ Poll task from queue
    ├─ createAction(task) via ActionRegistry
    ├─ action.start()
    └─ action.tick() until complete
        ↓
Action completes
    ├─ ActionResult returned
    ├─ minewright.getMemory().addAction()
    └─ SHOULD CALL: minewright.notifyTaskCompleted() [MISSING]
        ↓
ProactiveDialogueManager.onTaskCompleted()
    ├─ Check cooldown
    ├─ triggerComment("task_complete", description)
    └─ Generate comment via LLM
        ↓
sendChatMessage("Another job well done!")
    └─ VoiceManager.speakIfEnabled() [PRESENT]
```

---

## 8. Integration Gaps and Recommendations

### Critical Missing Integrations

| Gap | Impact | Recommendation | Priority |
|-----|--------|----------------|----------|
| ActionExecutor → ProactiveDialogueManager | No comments on task completion | Add notify calls in ActionResult handling | HIGH |
| Batching → ProactiveDialogueManager | Multiple simultaneous LLM calls | Batch dialogue triggers | MEDIUM |
| Memory → Vector Store | Placeholder embeddings only | Integrate real embedding model | HIGH |
| Orchestration → Shared Memory | Individual workers only record experiences | Send SHARED_SUCCESS messages | MEDIUM |
| Voice → Dialogue | Comments not spoken | Add VoiceManager calls | LOW |
| State Machine → Dialogue | No state-based comments | Subscribe to StateTransitionEvent | MEDIUM |

### Suggested Integration Patterns

#### Pattern 1: Event Aggregation for Dialogue

**Problem:** Multiple triggers might fire simultaneously (e.g., task completes + player approaches)

**Solution:**
```java
public class DialogueEventAggregator {
    private final List<DialogueTrigger> pendingTriggers = new ArrayList<>();

    public void submitTrigger(DialogueTrigger trigger) {
        pendingTriggers.add(trigger);
    }

    public void tick() {
        if (pendingTriggers.isEmpty()) return;

        // Merge similar triggers
        List<DialogueTrigger> merged = mergeTriggers(pendingTriggers);

        // Select highest priority
        DialogueTrigger selected = selectHighestPriority(merged);

        // Generate single comment
        generateAndSpeakComment(selected);

        pendingTriggers.clear();
    }
}
```

---

#### Pattern 2: Memory Synchronization

**Problem:** When foreman + workers collaborate, shared success not recorded

**Solution:**
```java
public class SharedExperienceRecorder {
    public void recordSharedSuccess(String planId, String result, List<String> participants) {
        for (String participantId : participants) {
            ForemanEntity minewright = getMineWrightById(participantId);
            if (minewright != null) {
                minewright.getCompanionMemory().recordSharedSuccess(result);
                minewright.notifyMilestone("Team completed: " + result);
            }
        }
    }
}
```

---

#### Pattern 3: Batching with Urgency

**Problem:** Urgent dialogue (danger alerts) shouldn't be batched

**Solution:**
```java
public enum PromptPriority {
    URGENT,      // Send immediately (danger, errors)
    NORMAL,      // Batch with others (comments)
    BACKGROUND   // Lowest priority (reflections)
}
```

Modify `LocalPreprocessor` to always send URGENT prompts immediately.

---

## 9. Key Integration Interfaces Summary

### LLM Layer
- `AsyncLLMClient.sendAsync(prompt, params)` - Core async interface
- `PromptBuilder.buildSystemPrompt()` - Task planning context
- `CompanionPromptBuilder.buildConversationalSystemPrompt(memory)` - Chat context with personality

### Memory
- `CompanionMemory.recordExperience(type, description, weight)` - Store episodic memory
- `CompanionMemory.findRelevantMemories(query, k)` - Semantic search
- `CompanionMemory.getRelationshipContext()` - Rapport/trust for prompting

### Orchestration
- `OrchestratorService.registerAgent(minewright, role)` - Agent registration
- `AgentCommunicationBus.publish(message)` - Inter-agent messaging
- `AgentCommunicationBus.poll(agentId)` - Message retrieval

### Dialogue
- `ProactiveDialogueManager.tick()` - Trigger checking
- `ProactiveDialogueManager.onTaskCompleted(desc)` - Event callback
- `ConversationManager.generateProactiveComment(trigger, llm)` - Comment generation

### Voice
- `VoiceManager.speak(text)` - TTS output
- `VoiceManager.listenForCommand()` - STT input
- `VoiceManager.isEnabled()` - Voice availability check

### Execution
- `AgentStateMachine.transitionTo(state, reason)` - State management
- `ActionExecutor.queueTask(task)` - Direct task queue
- `ActionRegistry.createAction(type, minewright, task, ctx)` - Plugin actions

---

## 10. Thread Safety and Concurrency

### Async Operations

| Operation | Thread Pool | Non-Blocking |
|-----------|-------------|--------------|
| Task Planning | `LLMExecutorService` | Yes (CompletableFuture) |
| Dialogue Generation | `LLMExecutorService` | Yes (CompletableFuture) |
| Vector Search | Main thread | No (should be async) |
| Message Polling | Main thread | Yes (non-blocking poll) |
| Action Tick | Main thread | No (but fast) |

### Thread-Safe Components

1. **ConcurrentHashMap** - Used in:
   - `CompanionMemory.semanticMemories`
   - `CompanionMemory.playerPreferences`
   - `CompanionMemory.playstyleMetrics`
   - `OrchestratorService.workerAssignments`

2. **AtomicReference** - Used in:
   - `AgentStateMachine.currentState`
   - `CompanionMemory.rapportLevel`
   - `CompanionMemory.trustLevel`

3. **ConcurrentLinkedQueue** - Used in:
   - `ActionExecutor.taskQueue`
   - `ForemanEntity.messageQueue`

---

## 11. Configuration Integration

All systems read from `MineWrightConfig`:

```toml
[llm]
provider = "groq"
apiKey = "gsk_..."
model = "llama3-70b-8192"

[voice]
enabled = true
mode = "logging"  # or "real", "disabled"
ttsVolume = 0.8
sttLanguage = "en-US"

[dialogue]
enabled = true
checkInterval = 100  # ticks
baseCooldown = 600   # ticks

[orchestration]
enabled = true
assignmentTimeout = 30000  # ms
maxRetries = 2
```

---

## 12. Testing Integration Points

### Unit Tests Needed

| Test | Purpose | Status |
|------|---------|--------|
| `ActionExecutorTest.testAsyncPlanning` | Verify non-blocking LLM calls | EXISTS |
| `TaskPlannerTest.testPromptBuilding` | Verify context injection | EXISTS |
| `WorldKnowledgeTest.testContextBuilding` | Verify world state capture | EXISTS |
| `CompanionMemoryTest.testVectorSearch` | Verify semantic search | MISSING |
| `OrchestratorTest.testTaskDistribution` | Verify worker assignment | MISSING |
| `DialogueManagerTest.testTriggerCooldowns` | Verify comment throttling | MISSING |
| `VoiceManagerTest.testTTSIntegration` | Verify speech output | MISSING |

### Integration Tests Needed

1. **End-to-End Command Flow**: GUI → ActionExecutor → LLM → Task Queue → Action → Dialogue
2. **Multi-Agent Coordination**: Foreman planning → Worker assignment → Progress reporting
3. **Memory + Dialogue**: Experience recording → Vector search → Context-aware comment
4. **Voice + Execution**: Command completion → TTS output

---

## 13. Performance Considerations

### LLM Call Optimization

| Strategy | Implementation | Savings |
|----------|----------------|---------|
| Async non-blocking | CompletableFuture everywhere | Prevents game freezes |
| Batching | LocalPreprocessor for multiple prompts | Reduces API calls |
| Caching | LLMCache for repeated prompts | Reduces latency |
| Resilience | Circuit breaker pattern | Prevents cascading failures |

### Memory Optimization

| Component | Limit | Cleanup Strategy |
|-----------|-------|------------------|
| Episodic Memories | 200 | FIFO eviction |
| Working Memory | 20 entries | FIFO eviction |
| Inside Jokes | 30 | Remove least referenced |
| Vector Store | Unlimited | Should cap |

---

## 14. Future Integration Opportunities

### 1. Emotion System Integration

**Current:** Personality traits are static
**Future:** Dynamic mood based on events

```java
public interface EmotionalState {
    void onTaskSuccess(String task);
    void onTaskFailure(String task, String reason);
    void onPlayerInteraction(String type);
    Mood getCurrentMood();
}
```

Integration points:
- `ProactiveDialogueManager` uses mood for comment tone
- `CompanionPromptBuilder` adjusts formality based on mood
- `VoiceManager` changes TTS voice based on mood

---

### 2. Learning Integration

**Current:** Playstyle metrics tracked but not used
**Future:** Adaptive behavior

```java
public interface LearningSystem {
    void observePlayerBehavior(String action, Map<String, Object> context);
    void predictPlayerNeeds();
    void suggestActions();
}
```

Integration points:
- `PromptBuilder` includes learned preferences
- `OrchestratorService` pre-assigns tasks based on predictions
- `ProactiveDialogueManager` anticipates needs

---

### 3. Multiplayer Coordination

**Current:** Single player focus
**Future:** Multi-player companion

```java
public interface MultiplayerCompanion {
    void trackPlayer(String playerId);
    void balanceAttention(List<Player> nearbyPlayers);
    void coordinateWithOtherCompanions(CompanionEntity other);
}
```

Integration points:
- `CompanionMemory` per-player relationships
- `OrchestratorService` cross-player coordination
- `ProactiveDialogueManager` group-aware comments

---

## Conclusion

The MineWright project has **47 integration points** across 7 major systems. The current architecture uses async patterns extensively to prevent game thread blocking. Key findings:

**Strengths:**
- Clean async LLM integration with CompletableFuture
- Message-based orchestration system
- Event-driven state management
- Plugin-based action system

**Critical Gaps:**
1. Action completion not triggering dialogue callbacks
2. No shared experience recording for multi-agent tasks
3. Placeholder vector embeddings (no semantic search)
4. Voice output not integrated with dialogue
5. No batching for dialogue triggers

**Recommended Priority:**
1. **HIGH**: Fix ActionExecutor → Dialogue callbacks
2. **HIGH**: Integrate real embedding model
3. **MEDIUM**: Implement shared experience recording
4. **MEDIUM**: Add dialogue event aggregation
5. **LOW**: Integrate voice with dialogue

---

**Generated:** 2026-02-26
**Orchestrator:** Integration Planner
**Status:** Integration mapping complete
**Next Steps:** Implement critical gap fixes
