# Conversation Coordination Design

## Executive Summary

This document describes how to make AI agents (MineWright entities) have natural, ongoing conversations with players while working. The goal is to transform agents from "idle workers waiting for commands" into "fellow players" - companions who chat about their work, coordinate with each other, and feel like teammates.

## Current State Analysis

### Existing Infrastructure

The codebase already has strong foundations for conversation:

1. **ProactiveDialogueManager** - Handles event-based commentary
   - Tick-based trigger checking (currently minimized for "minimal chatter")
   - Personality-driven response generation
   - Relationship-aware dialogue selection
   - Speech pattern tracking for variety

2. **ConversationManager** - Processes player messages
   - Distinguishes between task commands and casual chat
   - Generates conversational responses via LLM
   - Tracks interaction history

3. **CompanionMemory** - Rich relationship tracking
   - Rapport and trust levels (0-100)
   - Episodic memories (shared experiences)
   - Emotional memories (significant moments)
   - Inside jokes and conversational history
   - Player preferences and playstyle metrics

4. **AgentCommunicationBus** - Inter-agent messaging
   - Direct messages and broadcasts
   - Priority-based queues
   - Message filtering capabilities

5. **OrchestratorService** - Multi-agent coordination
   - Foreman-worker hierarchy
   - Task distribution and progress tracking
   - Plan execution monitoring

### Current Limitations

1. **Reactive Only** - Agents only speak when triggered by events
2. **No Agent-to-Agent Chat** - Workers don't talk to each other
3. **Single Foreman Voice** - Only the foreman communicates with players
4. **No Ongoing Commentary** - Work happens in silence
5. **No Parallel Conversations** - Can't chat while working

---

## Design Principles

1. **Non-Blocking** - Conversations never pause work execution
2. **Context-Aware** - Comments fit the current situation and activity
3. **Personality-Driven** - Each agent has a unique voice based on traits
4. **Relationship-Scaled** - Higher rapport = more chatty and familiar
5. **Variety-Conscious** - Avoid repetitive phrases and topics
6. **Interruptible** - Important events can interrupt idle chatter
7. **Queue-Based** - Conversations are managed in queues, not blocking calls

---

## 1. Proactive Conversation Triggers

### 1.1 Time-Based Triggers

Triggers that fire periodically based on game time and agent state.

```java
// Time-based trigger configuration
public class ConversationTimers {
    // Check intervals (in ticks)
    static final int RAPPORT_CHECK_INTERVAL = 600;     // 30 seconds
    static final int ACTIVITY_COMMENT_INTERVAL = 1200;  // 1 minute
    static final int PROGRESS_UPDATE_INTERVAL = 200;    // 10 seconds (only when working)

    // Cooldowns (prevents spam)
    static final int SAME_TOPIC_COOLDOWN = 3600;        // 3 minutes
    static final int IDLE_CHATTER_COOLDOWN = 6000;      // 5 minutes
}
```

**Implementation Pattern:**

```java
public class TimeBasedConversationTrigger {
    private final ForemanEntity agent;
    private final CompanionMemory memory;
    private int ticksSinceLastCheck = 0;
    private int ticksSinceLastComment = 0;

    public void tick() {
        ticksSinceLastCheck++;
        ticksSinceLastComment++;

        // Rapport-scaled check-in
        if (ticksSinceLastCheck >= ConversationTimers.RAPPORT_CHECK_INTERVAL) {
            ticksSinceLastCheck = 0;
            considerPeriodicCheckIn();
        }

        // Activity-based comment
        if (ticksSinceLastComment >= getActivityCommentInterval()) {
            considerActivityComment();
        }
    }

    private int getActivityCommentInterval() {
        // Higher rapport = more frequent comments
        int rapport = memory.getRapportLevel();
        if (rapport > 70) return 800;  // 40 seconds - close friends
        if (rapport > 40) return 1200; // 1 minute - friends
        return 2000;                   // 100 seconds - acquaintances
    }

    private void considerPeriodicCheckIn() {
        // Only check in if player is nearby and agent isn't too busy
        if (!isPlayerNearby(20)) return;
        if (isCriticallyBusy()) return;

        // Rapport-based check-in chance
        int rapport = memory.getRapportLevel();
        double checkInChance = 0.1 + (rapport / 200.0); // 10% to 60%

        if (Math.random() < checkInChance) {
            generateCheckInComment();
        }
    }
}
```

**Example Dialogues:**

```
// Low rapport (formal, professional)
Mace: "Pardon me, just checking in - how are you finding the progress?"

// Medium rapport (friendly, interested)
Mace: "Hey! How's everything looking from your end? Need any adjustments?"

// High rapport (casual, familiar)
Mace: "So... we gonna talk about that creeper from earlier, or what?"
Mace: "How's it hanging? Ready for the next big project?"
```

### 1.2 Event-Based Triggers

Triggers that fire in response to specific game events.

**Event Categories:**

```java
public enum ConversationTrigger {
    // Discovery events
    BIOME_DISCOVERED,
    STRUCTURE_FOUND,
    RARE_BLOCK_FOUND,
    CAVE_DISCOVERED,

    // Problem events
    BLOCKED_PATH,
    LOW_INVENTORY,
    TOOL_BROKEN,
    STUCK_ON_TASK,

    // Achievement events
    TASK_MILESTONE,
    PLAN_COMPLETE,
    RECORD_BREAKING,  // "Fastest we've ever done this!"
    FIRST_TIME_SUCCESS,

    // Coordination events
    WORKER_FINISHED,
    ALL_WORKERS_BUSY,
    NEED_ASSISTANCE,
    COORDINATION_REQUEST
}
```

**Implementation Pattern:**

```java
public class EventBasedConversationTrigger {
    private final ConversationQueue conversationQueue;
    private final EventBus eventBus;

    public EventBasedConversationTrigger(ForemanEntity agent) {
        this.conversationQueue = new ConversationQueue(agent);
        this.eventBus = agent.getActionExecutor().getEventBus();

        subscribeToEvents();
    }

    private void subscribeToEvents() {
        // Discovery events
        eventBus.subscribe(BlockMinedEvent.class, this::onBlockMined);
        eventBus.subscribe(BiomeEnteredEvent.class, this::onBiomeEntered);

        // Problem events
        eventBus.subscribe(PathBlockedEvent.class, this::onPathBlocked);
        eventBus.subscribe(InventoryFullEvent.class, this::onInventoryFull);

        // Achievement events
        eventBus.subscribe(ActionCompletedEvent.class, this::onActionCompleted);
        eventBus.subscribe(PlanCompletedEvent.class, this::onPlanCompleted);
    }

    private void onBlockMined(BlockMinedEvent event) {
        // Only comment on rare/interesting blocks
        if (isInterestingBlock(event.getBlockType())) {
            conversationQueue.enqueuePrioritized(
                ConversationPriority.HIGH,
                "discovery",
                "Found " + event.getBlockType()
            );
        }
    }

    private void onActionCompleted(ActionCompletedEvent event) {
        // Comment on milestones, not every action
        if (event.isMilestone()) {
            conversationQueue.enqueuePrioritized(
                ConversationPriority.MEDIUM,
                "achievement",
                event.getDescription()
            );
        }
    }
}
```

**Example Dialogues:**

```
// Discovery
Worker: "Hey boss, found diamonds! ...never mind, just gravel."
Mace: "Ooh, diamonds! We eatin' good tonight!"
Worker: "Check this out - abandoned mineshaft!"

// Problems
Worker: "Uh, slight hiccup - path's blocked. Rerouting."
Mace: "Inventory's full! Can someone come clear this out?"
Worker: "Hammer's broke. Need a replacement!"

// Achievements
Mace: "That's the last wall! Structure's complete!"
Mace: "New personal record - 100 blocks in 2 minutes!"
Worker: "First time getting it right on the first try!"
```

### 1.3 Context-Based Triggers

Triggers based on the agent's current situation and environment.

```java
public class ContextBasedConversationTrigger {
    private Biome lastBiome = null;
    private boolean lastRaining = false;
    private String lastActivity = null;

    public void checkContextTriggers(ForemanEntity agent) {
        // Biome changes
        Biome currentBiome = getCurrentBiome(agent);
        if (currentBiome != lastBiome) {
            onBiomeChange(currentBiome);
            lastBiome = currentBiome;
        }

        // Weather changes
        boolean isRaining = agent.level().isRaining();
        if (isRaining != lastRaining) {
            onWeatherChange(isRaining);
            lastRaining = isRaining;
        }

        // Activity changes
        String currentActivity = getCurrentActivity(agent);
        if (!currentActivity.equals(lastActivity)) {
            onActivityChange(currentActivity, lastActivity);
            lastActivity = currentActivity;
        }
    }

    private void onBiomeChange(Biome newBiome) {
        if (newBiome == Biome.NETHER) {
            enqueueComment("danger", "Entered the Nether - careful!");
        } else if (newBiome == Biome.DESERT) {
            enqueueComment("observation", "Desert again. At least it's sunny.");
        }
    }
}
```

**Example Dialogues:**

```
// Location-based
Mace: "Nice view from up here!"
Worker: "This cave goes deep... really deep."
Mace: "Never thought I'd see a mushroom field this big."

// Weather-based
Mace: "Rain's perfect for underground work!"
Worker: "Thunder's picking up. Maybe we should head inside?"

// Activity-based
Worker: "Finally moving again! Being stuck is the worst."
Mace: "Taking a breather while the others catch up."
```

### 1.4 Memory-Based Triggers

Triggers that reference past conversations and experiences.

```java
public class MemoryBasedConversationTrigger {
    private final CompanionMemory memory;
    private final Set<String> referencedMemories = new HashSet<>();

    public void considerMemoryReference() {
        // Get a random significant memory
        EmotionalMemory significant = memory.getMostSignificantMemory();
        if (significant == null) return;

        // Don't reference same memory too often
        if (referencedMemories.contains(significant.description)) {
            referencedMemories.clear(); // Reset when all referenced
        }

        // Chance to reference based on relationship strength
        int rapport = memory.getRapportLevel();
        double referenceChance = 0.05 + (rapport / 500.0); // 5% to 25%

        if (Math.random() < referenceChance) {
            generateMemoryReference(significant);
            referencedMemories.add(significant.description);
        }
    }

    private void generateMemoryReference(EmotionalMemory memory) {
        String template = getReferenceTemplate(memory.emotionalWeight);
        String comment = String.format(template, memory.description);
        enqueueComment("memory_reference", comment);
    }
}
```

**Example Dialogues:**

```
// Positive memories
Mace: "Remember that first house we built? Look at us now!"
Mace: "This reminds me of when we found that spawner - good times."
Worker: "Last time we did this, we fell in a hole. Let's not do that again."

// Negative memories (learning moments)
Mace: "After what happened last time, I'm double-checking everything."
Worker: "Learned from last time - bringing extra torches!"
```

---

## 2. Multi-Agent Conversation

### 2.1 Agent-to-Agent Communication

Agents should talk to each other about coordination, progress, and problems.

**Message Types:**

```java
public enum InterAgentMessageType {
    // Coordination
    COORDINATION_REQUEST,   // "Need help over here!"
    COORDINATION_OFFER,     // "I can handle that section"
    TASK_HANDOFF,           // "Done with my part, taking yours"

    // Information
    DISCOVERY_ANNOUNCEMENT, // "Found diamonds at 100, 64, -200"
    WARNING,                // "Creeper nearby!"
    STATUS_UPDATE,          // "Progress at 50%"

    // Social
    BANTER,                 // Casual chat between agents
    ENCOURAGEMENT,          // "Almost there!"
    CELEBRATION,            // "Section complete!"
}
```

**Implementation Pattern:**

```java
public class AgentConversationManager {
    private final AgentCommunicationBus commBus;
    private final ForemanEntity agent;
    private final PersonalityProfile personality;

    public void handleInterAgentMessage(AgentMessage message) {
        switch (message.getType()) {
            case COORDINATION_REQUEST:
                handleCoordinationRequest(message);
                break;
            case DISCOVERY_ANNOUNCEMENT:
                handleDiscoveryAnnouncement(message);
                break;
            case BANTER:
                handleBanter(message);
                break;
        }
    }

    private void handleCoordinationRequest(AgentMessage message) {
        // Decide whether to help based on personality and current state
        if (personality.agreeableness > 60 && !isBusy()) {
            // Send acceptance
            AgentMessage response = AgentMessage.builder()
                .type(InterAgentMessageType.COORDINATION_OFFER)
                .sender(agent.getEntityName())
                .recipient(message.getSenderName())
                .content("I'll help!")
                .build();
            commBus.publish(response);
        } else {
            // Send polite decline
            AgentMessage response = AgentMessage.builder()
                .type(InterAgentMessageType.STATUS_UPDATE)
                .sender(agent.getEntityName())
                .recipient(message.getSenderName())
                .content("Swamped right now, sorry!")
                .build();
            commBus.publish(response);
        }
    }

    private void handleBanter(AgentMessage message) {
        // Only engage in banter if high extraversion
        if (personality.extraversion > 70) {
            String response = generateBanterResponse(message.getContent());
            enqueueComment("banter_response", response);
        }
    }
}
```

**Example Dialogues:**

```
// Coordination
Worker1: "Hey, someone take the west wall? I'm overloaded."
Worker2: "Got it! Heading there now."
Worker1: "Thanks! You're a lifesaver."

// Discovery sharing
Worker3: "Diamonds at 100, 64, -200! Just saying."
Mace: "Noted! We'll sweep that area after the build."

// Banter
Worker1: "Race you to the corner!"
Worker2: "You're on! Loser places the torches."
Mace: "Both of you focus. We have a deadline."

// Encouragement
Mace: "Looking good everyone! Keep it up!"
Worker3: "Almost done with my section!"
Worker1: "Nice! I'm at 80%."
```

### 2.2 Shared Conversation Context

All agents should share awareness of the ongoing conversation to avoid repetition and enable contextual responses.

```java
public class SharedConversationContext {
    private final Map<String, ConversationTopic> recentTopics = new ConcurrentHashMap<>();
    private final Set<String> exhaustedTopics = ConcurrentHashMap.newKeySet();
    private final Queue<String> recentComments = new ConcurrentLinkedQueue<>();

    public void recordComment(String agentName, String topic, String content) {
        // Store the comment
        recentTopics.put(topic, new ConversationTopic(agentName, topic, content, Instant.now()));
        recentComments.offer(content);
        if (recentComments.size() > 20) recentComments.poll();

        // Mark topic as recently discussed
        exhaustedTopics.add(topic);

        // Clean up old topics after 5 minutes
        cleanupOldTopics();
    }

    public boolean isTopicExhausted(String topic) {
        return exhaustedTopics.contains(topic);
    }

    public void refreshTopics() {
        // Call periodically to allow topics to be discussed again
        exhaustedTopics.clear();
    }

    private void cleanupOldTopics() {
        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
        recentTopics.entrySet().removeIf(entry ->
            entry.getValue().timestamp.isBefore(fiveMinutesAgo));
    }

    public static class ConversationTopic {
        public final String agentName;
        public final String topic;
        public final String content;
        public final Instant timestamp;

        // Constructor, getters...
    }
}
```

### 2.3 Avoiding Repetitive Conversations

Prevent agents from saying the same things repeatedly.

```java
public class ConversationDeduplicator {
    private final Map<String, Integer> phraseUsageCount = new ConcurrentHashMap<>();
    private final Queue<String> recentPhrases = new ConcurrentLinkedQueue<>();

    public boolean shouldAllowPhrase(String phrase) {
        // Check if phrase was used recently
        int recentCount = 0;
        for (String recent : recentPhrases) {
            if (recent.equalsIgnoreCase(phrase)) {
                recentCount++;
            }
        }

        // Allow unless used 2+ times in last 10 comments
        return recentCount < 2;
    }

    public void recordPhrase(String phrase) {
        phraseUsageCount.merge(phrase, 1, Integer::sum);
        recentPhrases.offer(phrase);
        if (recentPhrases.size() > 10) recentPhrases.poll();
    }

    public String getAlternativePhrase(String basePhrase) {
        // Generate variation using personality traits
        List<String> alternatives = generateVariations(basePhrase);
        for (String alt : alternatives) {
            if (shouldAllowPhrase(alt)) {
                return alt;
            }
        }
        return basePhrase; // Fallback
    }
}
```

### 2.4 Personality-Driven Dialogue Differences

Each agent should have a unique voice based on their personality traits.

```java
public class PersonalityDialogueFilter {
    public static String applyPersonality(String baseMessage, PersonalityProfile personality) {
        String filtered = baseMessage;

        // Apply verbal tics
        if (personality.shouldUseVerbalTic()) {
            String tic = personality.getRandomVerbalTic();
            filtered = tic + " " + filtered;
        }

        // Adjust formality
        if (personality.formality > 70) {
            filtered = formalize(filtered);
        } else if (personality.formality < 30) {
            filtered = casualize(filtered);
        }

        // Adjust enthusiasm based on extraversion
        if (personality.extraversion > 70 && !filtered.endsWith("!")) {
            filtered = filtered + "!";
        }

        return filtered;
    }

    private static String casualize(String message) {
        return message
            .replace("I am", "I'm")
            .replace("you are", "you're")
            .replace("cannot", "can't")
            .replace("do not", "don't")
            .replace("Yes", "Yep")
            .replace("No", "Nope");
    }

    private static String formalize(String message) {
        return message
            .replace("I'm", "I am")
            .replace("you're", "you are")
            .replace("can't", "cannot")
            .replace("don't", "do not")
            .replace("Yep", "Yes")
            .replace("Nope", "No");
    }
}
```

**Example Personality Transformations:**

```
// Base message: "I'm going to handle the west wall."

// High formality, low extraversion
"Dutifully, I shall attend to the west wall."

// Low formality, high extraversion
"West wall? I got it! Let's do this!"

// High humor
"West wall duty calls! Myahaha... wait, that's not my catchphrase."

// High neuroticism (nervous)
"I'll, um, try the west wall? If that's okay?"
```

---

## 3. Concurrent Work + Talk

### 3.1 Separating Conversation from Action Execution

Conversations must never block or slow down work.

**Architecture:**

```
┌─────────────────────────────────────────────────────┐
│                    ForemanEntity                     │
│  ┌──────────────────┐  ┌──────────────────────────┐ │
│  │  ActionExecutor  │  │ ConversationController    │ │
│  │  - Task Queue    │  │ - ConversationQueue       │ │
│  │  - Current Action│  │ - LLM Async Calls         │ │
│  │  - tick()        │  │ - Speech Synthesis        │ │
│  └────────┬─────────┘  └──────────┬───────────────┘ │
│           │                       │                   │
│           │                       │                   │
│  ┌────────▼───────────────────────▼───────┐         │
│  │          Entity.tick()                  │         │
│  │  1. actionExecutor.tick()               │         │
│  │  2. conversationController.process()   │         │
│  └─────────────────────────────────────────┘         │
└─────────────────────────────────────────────────────┘
```

**Implementation:**

```java
public class ConversationController {
    private final ConversationQueue queue;
    private final AsyncLLMClient llmClient;
    private final SpeechSynthesizer speech;

    private CompletableFuture<String> pendingComment = null;
    private boolean isSpeaking = false;

    public void tick() {
        // Don't interfere with critical actions
        if (isCriticalMoment()) {
            return;
        }

        // Check if pending comment completed
        if (pendingComment != null && pendingComment.isDone()) {
            try {
                String comment = pendingComment.getNow(null);
                if (comment != null) {
                    speak(comment);
                }
            } finally {
                pendingComment = null;
            }
        }

        // Start next comment if ready
        if (pendingComment == null && !isSpeaking && !queue.isEmpty()) {
            ConversationItem item = queue.dequeue();
            if (item != null) {
                startGeneratingComment(item);
            }
        }
    }

    private void startGeneratingComment(ConversationItem item) {
        // Check cooldowns and context
        if (!canSpeakNow(item)) {
            return;
        }

        // Start async generation (non-blocking!)
        pendingComment = llmClient.sendAsync(item.getPrompt(), item.getParams());
    }

    private void speak(String comment) {
        isSpeaking = true;

        // Send to chat
        agent.sendChatMessage(comment);

        // Optional: Text-to-speech
        speech.speakAsync(comment).thenRun(() -> isSpeaking = false);

        // Or just use a timer
        int duration = estimateSpeakingTime(comment);
        scheduler.schedule(() -> isSpeaking = false, duration, TimeUnit.MILLISECONDS);
    }

    private boolean isCriticalMoment() {
        // Don't chat during critical actions
        ActionExecutor executor = agent.getActionExecutor();
        BaseAction current = executor.getCurrentAction();

        if (current instanceof CombatAction) return true;
        if (current instanceof EvasionAction) return true;

        return false;
    }
}
```

### 3.2 Non-Blocking LLM Responses

LLM calls must be async and checkable without blocking.

```java
public class AsyncConversationGenerator {
    private final AsyncLLMClient llmClient;
    private final BatchingLLMClient batchingClient;

    public CompletableFuture<String> generateComment(
        ConversationTrigger trigger,
        ConversationContext context
    ) {
        // Build prompt
        String prompt = buildPrompt(trigger, context);

        // Use batching for lower-priority comments
        if (trigger.getPriority() == ConversationPriority.LOW) {
            return batchingClient.submit(
                prompt,
                PromptBatcher.PromptType.CONVERSATION,
                context.toMap()
            );
        }

        // Direct call for higher priority
        Map<String, Object> params = Map.of(
            "maxTokens", 50,
            "temperature", 0.9,
            "systemPrompt", buildSystemPrompt(context)
        );

        return llmClient.sendAsync(prompt, params);
    }

    public boolean isReady(CompletableFuture<String> future) {
        return future != null && future.isDone();
    }

    public String getIfReady(CompletableFuture<String> future) {
        if (isReady(future)) {
            try {
                return future.getNow(null);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
```

### 3.3 Queue-Based Conversation Management

Prioritize and manage multiple concurrent conversations.

```java
public class ConversationQueue {
    private final PriorityQueue<ConversationItem> queue;
    private final ForemanEntity agent;
    private final ConversationDeduplicator deduplicator;

    public void enqueue(ConversationItem item) {
        // Check for duplicates
        if (!deduplicator.shouldAllowPhrase(item.getContent())) {
            return;
        }

        // Check cooldowns
        if (isOnCooldown(item.getType())) {
            return;
        }

        // Add to queue
        queue.offer(item);
    }

    public void enqueuePrioritized(ConversationPriority priority, String type, String content) {
        ConversationItem item = new ConversationItem(priority, type, content, Instant.now());
        enqueue(item);
    }

    public ConversationItem dequeue() {
        ConversationItem item = queue.poll();
        if (item != null) {
            deduplicator.recordPhrase(item.getContent());
            setCooldown(item.getType());
        }
        return item;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    // Clear queue for urgent events
    public void clear() {
        queue.clear();
    }
}

public class ConversationItem implements Comparable<ConversationItem> {
    private final ConversationPriority priority;
    private final String type;
    private final String content;
    private final Instant timestamp;

    @Override
    public int compareTo(ConversationItem other) {
        // Priority first, then timestamp (older first)
        int priorityCompare = other.priority.compareTo(this.priority);
        if (priorityCompare != 0) return priorityCompare;
        return this.timestamp.compareTo(other.timestamp);
    }
}

public enum ConversationPriority {
    URGENT,      // Danger, errors - interrupt immediately
    HIGH,        // Achievements, discoveries
    MEDIUM,      // Progress updates, coordination
    LOW,         // Idle chatter, banter
    BACKGROUND   // Ambient comments, can be dropped
}
```

### 3.4 Interruptible Conversations

Important events should interrupt idle chatter.

```java
public class InterruptibleConversation {
    private final ConversationQueue queue;
    private CompletableFuture<String> currentGeneration;

    public void handleUrgentEvent(ConversationTrigger trigger) {
        // Cancel current low-priority generation
        if (currentGeneration != null && !currentGeneration.isDone()) {
            currentGeneration.cancel(true);
            currentGeneration = null;
        }

        // Clear low-priority items from queue
        queue.clearLowPriority();

        // Enqueue urgent message
        ConversationItem urgent = new ConversationItem(
            ConversationPriority.URGENT,
            trigger.getType(),
            trigger.getContext(),
            Instant.now()
        );
        queue.enqueuePrioritized(urgent);
    }

    public boolean canBeInterrupted(ConversationPriority priority) {
        // Only LOW and BACKGROUND can be interrupted
        return priority == ConversationPriority.LOW ||
               priority == ConversationPriority.BACKGROUND;
    }
}
```

---

## 4. Conversation Content

### 4.1 Discussing Strategy and Plans

Agents should talk about what they're doing and why.

```java
public class StrategyCommentGenerator {
    public CompletableFuture<String> generateStrategyComment(
        OrchestratorService.PlanExecution plan,
        int progressPercent
    ) {
        String prompt = buildStrategyPrompt(plan, progressPercent);

        Map<String, Object> params = Map.of(
            "maxTokens", 80,
            "temperature", 0.7,
            "systemPrompt", buildStrategySystemPrompt()
        );

        return llmClient.sendAsync(prompt, params);
    }

    private String buildStrategyPrompt(PlanExecution plan, int progress) {
        return String.format("""
            Current plan: %s
            Progress: %d%%
            Tasks remaining: %d

            Generate a brief strategy update comment.
            Reference the plan goals and remaining work.
            Stay in character.
            """,
            plan.getDescription(),
            progress,
            plan.getRemainingTaskCount()
        );
    }
}
```

**Example Dialogues:**

```
// Plan announcement
Mace: "Alright team, here's the plan: we're building a starter house.
       Worker1, you're on the frame. Worker2, handle the flooring.
       I'll take the roof. Let's make it happen!"

// Progress update
Mace: "Frame's up, flooring's halfway done. Roof materials are staged
       and ready. Looking good for an on-time finish."

// Strategy shift
Mace: "Change of plans - we're running short on stone. I'm having
       Worker3 switch to cobble while Worker2 keeps the main frame going.
       We'll make up the time."

// Completion
Mace: "And that's a wrap! Starter house complete. We finished ahead
       of schedule, and I think it's our best work yet. Great job team!"
```

### 4.2 Reporting Problems and Asking for Help

Agents should proactively communicate issues.

```java
public class ProblemReportGenerator {
    public CompletableFuture<String> generateProblemReport(
        String problemType,
        String problemDescription,
        boolean needsHelp
    ) {
        String prompt = buildProblemPrompt(problemType, problemDescription, needsHelp);

        Map<String, Object> params = Map.of(
            "maxTokens", 100,
            "temperature", 0.6,
            "systemPrompt", buildProblemSystemPrompt()
        );

        return llmClient.sendAsync(prompt, params);
    }

    private String buildProblemPrompt(String type, String description, boolean needsHelp) {
        return String.format("""
            Problem type: %s
            Description: %s
            %s

            Generate a brief problem report.
            %s
            Stay in character.
            """,
            type,
            description,
            needsHelp ? "Need assistance: YES" : "Need assistance: NO",
            needsHelp ? "Ask for help clearly." : "State that you'll handle it."
        );
    }
}
```

**Example Dialogues:**

```
// Asking for help
Worker1: "Hey, I'm stuck. Can't reach the top of this wall. Someone taller
         give me a hand?"

// Reporting a problem
Worker2: "Running low on stone. About 20 left. Should I switch to cobble?"

// Offering help
Mace: "I see Worker3's section is piling up. I'm heading over to assist."

// Independent problem solving
Worker1: "Hit a snag with the placement, but I think I can work around it.
         Give me a minute."
```

### 4.3 Celebrating Achievements Together

Shared successes should be celebrated as a team.

```java
public class CelebrationCommentGenerator {
    private final List<String> CELEBRATION_TEMPLATES = List.of(
        "We did it! %s",
        "That's a wrap on %s! Great work everyone!",
        "%s - complete! Team, you're amazing!",
        "And that's how %s is done! Beautiful work!",
        "%s? Finished. What's next?"
    );

    public String generateCelebration(String achievement, int rapportLevel) {
        // Select template based on rapport
        String template;
        if (rapportLevel > 70) {
            template = CELEBRATION_TEMPLATES.get(2); // Most enthusiastic
        } else if (rapportLevel > 40) {
            template = CELEBRATION_TEMPLATES.get(1);
        } else {
            template = CELEBRATION_TEMPLATES.get(0); // Professional
        }

        return String.format(template, achievement);
    }

    public CompletableFuture<String> generatePersonalizedCelebration(
        String achievement,
        CompanionMemory memory
    ) {
        String prompt = String.format("""
            Generate a celebration comment for: %s

            Relationship context:
            - Rapport: %d/100
            - Shared successes: %d
            - Inside jokes: %d

            Reference your shared history if natural.
            Stay in character.
            """,
            achievement,
            memory.getRapportLevel(),
            memory.getSharedSuccessCount(),
            memory.getInsideJokeCount()
        );

        return llmClient.sendAsync(prompt, Map.of("maxTokens", 60, "temperature", 0.9));
    }
}
```

**Example Dialogues:**

```
// Low rapport (professional)
Mace: "Structure complete. Well executed team."

// Medium rapport (friendly)
Mace: "That's a wrap! Great work everyone!"

// High rapport (enthusiastic, personal)
Mace: "WE DID IT! Remember when we couldn't even build a dirt hut?
       Look at us now!"

// Team celebration
Mace: "First place! Fastest build we've ever done!"
Worker1: "New personal record!"
Worker2: "That went surprisingly smooth."
Worker3: "Can we celebrate with cake? Please?"
```

### 4.4 Banter and Personality Expression

Casual chat that builds relationships.

```java
public class BanterGenerator {
    private static final Map<String, String[]> BANTER_TEMPLATES = Map.of(
        "idle", new String[]{
            "So... got any plans after this?",
            "Nice weather we're having.",
            "Ever wonder what villagers do all day?",
            "Think the player notices how hard we work?"
        },
        "working", new String[]{
            "Hey, pass me that block? Thanks, you're the best.",
            "Race you to see who finishes their section first!",
            "I've got a rhythm going now. Don't break my flow!",
            "Who designed this structure? Actually, don't tell me."
        },
        "waiting", new String[]{
            "Waiting for materials. Exciting stuff.",
            "If I stand here long enough, do I become a block?",
            "Smooth jazz would really complete this moment."
        }
    );

    public String generateBanter(String context, PersonalityProfile personality) {
        String[] templates = BANTER_TEMPLATES.get(context);
        if (templates == null) return null;

        String base = templates[new Random().nextInt(templates.length)];
        return PersonalityDialogueFilter.applyPersonality(base, personality);
    }
}
```

**Example Dialogues:**

```
// Idle banter
Worker1: "So, you ever think about what we're really doing here?"
Worker2: "Building blocks for a higher power who can do it themselves?"
Worker1: "Exactly. Deep, right?"
Worker2: "Not really. It's a job."

// Work banter
Mace: "Who had 'wall falls down twice' on their bingo card?"
Worker1: "Not me, but I should have!"
Worker2: "Third time's the charm... right?"

// Relationship-based banter
Mace: "Hey, remember when you placed that door upside down?"
Worker1: "We agreed never to speak of that."
Mace: "And yet, here we are."
Worker1: "Some mysteries are better left unsolved."

// Personality-driven
High Humor: "I'd tell you a joke about construction, but I'm still working on it."
High Neuroticism: "Everything is fine. Everything is fine. Why wouldn't it be fine?"
High Extraversion: "Best. Team. EVER! Just saying!"
```

### 4.5 Asking Player for Clarification or Decisions

Involve the player in decisions when appropriate.

```java
public class PlayerQueryGenerator {
    public CompletableFuture<String> generateQuery(
        String situation,
        List<String> options,
        CompanionMemory memory
    ) {
        String prompt = String.format("""
            Situation: %s

            Options:
            %s

            Generate a brief query to the player asking for their preference.
            Make it conversational and in-character.
            """,
            situation,
            String.join("\n", options)
        );

        return llmClient.sendAsync(prompt, Map.of("maxTokens", 80, "temperature", 0.7));
    }

    public void queryPlayer(String question, Consumer<String> responseCallback) {
        // Send question to player
        PlayerQueryUI.showQuestion(agent.getEntityName(), question, responseCallback);

        // Also speak it
        agent.sendChatMessage(question);
    }
}
```

**Example Dialogues:**

```
// Material choice
Mace: "Hey boss, we're running low on stone. Should I switch to cobble
       or wait for more stone?"

// Design decision
Mace: "For the roof, I'm thinking spruce or dark oak. What's your pick?"

// Priority question
Mace: "We can either finish the wall now or start the roof. Your call -
       what's more important?"

// Progress check-in
Mace: "How's the pace? Should we speed up or slow down?"
```

---

## 5. Mace as Primary Conversationalist

### 5.1 Mace as the "Face" of the Crew

Mace (the foreman agent) should be the primary interface to the player, while workers communicate through Mace.

```java
public class ForemanConversationalist {
    private final ForemanEntity foreman;
    private final OrchestratorService orchestrator;
    private final ConversationQueue foremanQueue;

    public void handleWorkerMessage(AgentMessage message) {
        String workerName = message.getSenderName();
        String content = message.getContent();

        switch (message.getType()) {
            case DISCOVERY_ANNOUNCEMENT:
                // Foreman relays to player
                String relay = String.format("%s: %s", workerName, content);
                foremanQueue.enqueuePrioritized(
                    ConversationPriority.MEDIUM,
                    "worker_relay",
                    relay
                );
                break;

            case COORDINATION_REQUEST:
                // Foreman handles worker coordination
                handleWorkerCoordination(message);
                break;

            case STATUS_UPDATE:
                // Foreman acknowledges, may relay to player if important
                if (isImportantStatus(message)) {
                    String status = String.format("%s reports: %s", workerName, content);
                    foremanQueue.enqueuePrioritized(
                        ConversationPriority.LOW,
                        "worker_status",
                        status
                    );
                }
                break;
        }
    }

    public void reportAggregateProgress(OrchestratorService.PlanExecution plan) {
        int progress = plan.getProgressPercent();
        int completed = plan.getCompletedCount();
        int total = plan.getTotalTaskCount();

        String report = String.format(
            "Progress report: %d/%d tasks complete (%d%%). %s",
            completed, total, progress,
            getProgressMilestoneComment(progress)
        );

        foremanQueue.enqueuePrioritized(
            ConversationPriority.MEDIUM,
            "progress_report",
            report
        );
    }

    private String getProgressMilestoneComment(int progress) {
        if (progress == 25) return "Quarter way there!";
        if (progress == 50) return "Halfway done!";
        if (progress == 75) return "Three-quarters complete!";
        if (progress == 90) return "In the home stretch!";
        return "Steady progress.";
    }
}
```

**Example Dialogues:**

```
// Mace relaying worker discovery
Worker3: (on comm) "Found diamonds at 100, 64, -200!"
Mace: "Team, Worker3 found diamonds. We'll sweep that area after the build."

// Mace reporting aggregate progress
Mace: "Update: 3 of 5 sections complete. Worker1's on the roof, Worker2
       is finishing the walls. We're ahead of schedule."

// Mace facilitating worker discussion
Worker1: (on comm) "Need more cobble."
Mace: "Worker2, can you spare some cobble for Worker1?"
Worker2: (on comm) "On it."
Mace: "Worker1, help is on the way."
```

### 5.2 Mace Translating Player Intent to Worker Scripts

Mace should interpret player commands and communicate them to workers in natural language.

```java
public class ForemanTranslator {
    private final TaskPlanner taskPlanner;
    private final AgentCommunicationBus commBus;

    public void translateAndDelegate(String playerCommand, List<ForemanEntity> workers) {
        // Parse command into tasks
        ResponseParser.ParsedResponse plan = taskPlanner.planTasks(foreman, playerCommand);

        // Announce plan to workers
        String announcement = buildPlanAnnouncement(plan);
        foreman.sendChatMessage(announcement);

        // Distribute tasks to workers
        for (Task task : plan.getTasks()) {
            ForemanEntity assignedWorker = selectWorkerForTask(task, workers);
            if (assignedWorker != null) {
                String taskDescription = buildTaskDescription(task);

                AgentMessage assignment = AgentMessage.builder()
                    .type(InterAgentMessageType.TASK_ASSIGNMENT)
                    .sender(foreman.getEntityName())
                    .recipient(assignedWorker.getEntityName())
                    .content(taskDescription)
                    .payload("task", task)
                    .build();

                commBus.publish(assignment);
            }
        }
    }

    private String buildPlanAnnouncement(ResponseParser.ParsedResponse plan) {
        return String.format(
            "New plan from the player: %s. %s",
            plan.getPlan(),
            plan.getTasks().size() > 1
                ? "Dividing work among the team."
                : "I'll handle this personally."
        );
    }
}
```

**Example Dialogues:**

```
// Player command: "Build a stone house"
Mace: "Alright team, player wants a stone house. I'll handle the structure,
       Worker1 takes the walls, Worker2 handles the roof. Let's build!"

// Player command: "Everyone gather wood"
Mace: "You heard the player - everyone on wood gathering duty.
       Split up and hit the nearest forest. Meet back here when full."

// Player command: "Clear this area"
Mace: "Team, clear this area. Workers, start from the edges and work inward.
       I'll handle the center. Watch for creepers!"
```

### 5.3 Mace Reporting Aggregate Progress

Mace should provide high-level progress updates, not worker-level detail.

```java
public class ForemanProgressReporter {
    private final OrchestratorService orchestrator;
    private final ForemanEntity foreman;
    private int lastReportedProgress = 0;

    public void tick() {
        String currentPlanId = foreman.getCurrentTaskId();
        if (currentPlanId == null) return;

        int progress = orchestrator.getPlanProgress(currentPlanId);
        if (progress == -1) return;

        // Report at milestones (25%, 50%, 75%, 100%)
        if (shouldReportProgress(progress)) {
            reportProgress(progress);
            lastReportedProgress = progress;
        }
    }

    private boolean shouldReportProgress(int currentProgress) {
        int[] milestones = {25, 50, 75, 100};
        for (int milestone : milestones) {
            if (currentProgress >= milestone && lastReportedProgress < milestone) {
                return true;
            }
        }
        return false;
    }

    private void reportProgress(int progress) {
        String comment = buildProgressComment(progress);
        foreman.sendChatMessage(comment);
    }

    private String buildProgressComment(int progress) {
        return switch (progress) {
            case 25 -> "Quarter done! Looking good so far.";
            case 50 -> "Halfway there! Team's performing excellently.";
            case 75 -> "Three-quarters complete! In the home stretch now.";
            case 100 -> "That's a wrap! Another job well done team!";
            default -> String.format("Progress at %d%%. Steady work.", progress);
        };
    }
}
```

**Example Dialogues:**

```
// Progress milestones
Mace: "25% complete. Foundation's solid, materials flowing well."
Mace: "Halfway done! Everyone's performing great. Keep it up!"
Mace: "75% there! Just the finishing touches remaining."
Mace: "Complete! Beautiful work team. Player's gonna love this."

// Problem during execution
Mace: "Small hiccup - reorganizing tasks. Should be back on track in a moment."
Mace: "Worker2 fell behind. I'm having Worker1 assist. No impact to timeline."
```

### 5.4 Mace Facilitating Worker Discussions

Mace should mediate and facilitate communication between workers.

```java
public class ForemanFacilitator {
    private final AgentCommunicationBus commBus;
    private final ForemanEntity foreman;

    public void facilitateWorkerDiscussion(String topic, List<String> participants) {
        String facilitationPrompt = String.format(
            "Team, let's discuss: %s. %s, your thoughts?",
            topic,
            String.join(", ", participants)
        );

        foreman.sendChatMessage(facilitationPrompt);

        // Listen to responses and facilitate further discussion
        commBus.subscribe(foreman.getEntityName(), message -> {
            if (message.getType() == InterAgentMessageType.BANTER) {
                String response = buildFacilitationResponse(message);
                foreman.sendChatMessage(response);
            }
        });
    }

    private String buildFacilitationResponse(AgentMessage message) {
        String sender = message.getSenderName();
        String content = message.getContent();

        return String.format("%s has a point. Any other perspectives?", sender);
    }
}
```

**Example Dialogues:**

```
// Mace facilitating coordination
Mace: "Team, we need to coordinate the roof. Who's available?"
Worker1: "I can do it!"
Worker2: "Me too, if needed."
Mace: "Worker1, you take it. Worker2, help Worker3 with the final wall."

// Mace facilitating problem-solving
Mace: "Team, we're behind schedule. Ideas for speeding up?"
Worker3: "I can work faster if someone handles the placement."
Mace: "Good idea. Worker2, support Worker3. Let's pick up the pace."

// Mace mediating disagreement
Worker1: "I should handle the redstone."
Worker2: "No way, I'm better at it."
Mace: "Alright, both of you. Worker1 takes the wiring, Worker2 handles the
       torch placement. Work together."
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

1. **Create conversation infrastructure**
   - `ConversationController` class
   - `ConversationQueue` with priority system
   - `ConversationDeduplicator` for variety

2. **Integrate with existing systems**
   - Hook into `ForemanEntity.tick()`
   - Connect to `ProactiveDialogueManager`
   - Link to `AgentCommunicationBus`

### Phase 2: Triggers (Week 2-3)

3. **Implement trigger systems**
   - Time-based triggers
   - Event-based triggers
   - Context-based triggers
   - Memory-based triggers

4. **Add trigger configuration**
   - Cooldown system
   - Rapport scaling
   - Priority assignment

### Phase 3: Multi-Agent (Week 3-4)

5. **Agent-to-agent communication**
   - Message types for coordination
   - Shared conversation context
   - Personality-driven dialogue differences

6. **Mace as conversationalist**
   - Worker message relay
   - Progress aggregation
   - Discussion facilitation

### Phase 4: Content (Week 4-5)

7. **Comment generators**
   - Strategy discussion
   - Problem reporting
   - Celebration
   - Banter
   - Player queries

8. **LLM integration**
   - Async comment generation
   - Prompt templates
   - Fallback responses

### Phase 5: Polish (Week 5-6)

9. **Testing and tuning**
   - Cooldown balancing
   - Variety optimization
   - Personality refinement

10. **Documentation**
    - API documentation
    - Configuration guide
    - Example personalities

---

## Configuration

Add to `config/steve-common.toml`:

```toml
[conversation]
# Enable/disable conversations
enabled = true

# Conversation frequency (rapport-scaled modifiers)
base_check_interval = 600  # 30 seconds
rapport_frequency_bonus = 0.5  # +50% frequency at max rapport

# Cooldowns (in ticks)
same_topic_cooldown = 3600  # 3 minutes
idle_chatter_cooldown = 6000  # 5 minutes

# Priority thresholds
urgent_interrupt_all = true
high_interrupts_low = true
medium_interrupts_background = true

# Personality settings
mace_archetype = "THE_FOREMAN"
worker_archetype = "THE_WORKER"

# Multi-agent settings
enable_agent_to_agent_chat = true
shared_conversation_context = true
max_concurrent_conversations = 3
```

---

## Example Personalities

### Mace (The Foreman)

```java
PersonalityProfile mace = new PersonalityProfile(
    openness = 70,        // Creative, adaptable
    conscientiousness = 90,  // Organized, responsible
    extraversion = 80,    // Outspoken, leader
    agreeableness = 70,   // Collaborative
    neuroticism = 30,     // Calm under pressure
    humor = 60,           // Occasionally witty
    encouragement = 85,   // Very supportive
    formality = 40        // Casual but professional
);

mace.catchphrases = List.of(
    "Let's get to work!",
    "Team, you're amazing.",
    "Steady as she goes.",
    "That's how we do it!"
);

mace.verbalTics = List.of(
    "Alright then,",
    "Here's the plan:",
    "Team,"
);
```

### Worker Personality (Generic)

```java
PersonalityProfile worker = new PersonalityProfile(
    openness = 60,
    conscientiousness = 80,  // Hard-working
    extraversion = 50,       // Average sociability
    agreeableness = 75,      // Helpful
    neuroticism = 40,        // Somewhat anxious
    humor = 50,
    encouragement = 60,
    formality = 30           // Casual
);

worker.catchphrases = List.of(
    "On it!",
    "Gotcha boss.",
    "Working on it.",
    "Consider it done."
);
```

### Alternative Archetypes

```java
// The Joker
PersonalityProfile joker = new PersonalityProfile();
joker.humor = 95;
joker.extraversion = 85;
joker.formality = 10;
joker.catchphrases = List.of(
    "I'd tell you a joke, but...",
    "Plot twist!",
    "Comedy gold!",
    "I'm here all week."
);

// The Perfectionist
PersonalityProfile perfectionist = new PersonalityProfile();
perfectionist.conscientiousness = 100;
perfectionist.neuroticism = 60;
perfectionist.formality = 70;
perfectionist.catchphrases = List.of(
    "Measure twice, place once.",
    "Quality over speed.",
    "Let me double-check that.",
    "Precision matters."
);

// The Rookie
PersonalityProfile rookie = new PersonalityProfile();
rookie.openness = 80;
rookie.neuroticism = 70;  // Nervous, eager
rookie.encouragement = 90;
rookie.catchphrases = List.of(
    "I'll do my best!",
    "Teach me!",
    "Is this right?",
    "Learning every day!"
);
```

---

## Testing Scenarios

### Scenario 1: Team Building

```
Player: "Build a stone house"

Mace: "Team, player wants a stone house. I'll handle the structure,
       Worker1 takes the walls, Worker2 handles the roof. Let's build!"

[2 minutes later]

Mace: "25% complete. Foundation's solid, materials flowing well."

Worker2: "Hey, I'm running low on stone. Should I switch to cobble?"

Mace: "Worker2, switch to cobble for the roof. Player won't see it anyway."

[2 minutes later]

Mace: "Halfway done! Team's performing great. Keep it up!"

Worker1: "Walls are up! Moving to the interior now."

Mace: "Excellent work, Worker1. Worker2, how's the roof coming?"

Worker2: "Almost done! Just a few more rows."

[1 minute later]

Mace: "That's a wrap! Beautiful work team. Player's gonna love this."
```

### Scenario 2: Problem Solving

```
[Team working on a complex structure]

Worker1: "Uh, slight hiccup - path's blocked. Can't reach the upper level."

Mace: "Team, pause for a moment. Worker1's blocked."

Worker3: "I can help! I'm free."

Mace: "Worker3, assist Worker1. Clear the path."

Worker1: "Thanks Worker3! You're a lifesaver."

[5 minutes later]

Mace: "Path cleared. Everyone back to work. We lost some time, but we can
       make it up. Let's pick up the pace!"

Worker2: "On it! Moving faster now."

Mace: "That's the spirit!"
```

### Scenario 3: Discovery and Celebration

```
[Team mining in a cave]

Worker3: "Guys... you might want to see this."

Mace: "What is it, Worker3?"

Worker3: "Diamonds. A whole vein of them."

Worker1: "No way! How many?"

Worker3: "At least 8 from what I can see."

Mace: "Team! Excellent find! Worker3, mine carefully. Worker1, clear the
       surrounding area. Worker2, stand guard for mobs."

[2 minutes of tense mining]

Worker3: "Got them all! 12 diamonds total!"

Mace: "12 DIAMONDS! Team, this is our biggest haul yet!

Worker1: "We eatin' good tonight!"

Worker2: "Remember when we were happy with 5 iron?"

Mace: "Those were simpler times. Team, we've earned a celebration break.
       Good work everyone!"
```

---

## Conclusion

This design transforms AI agents from passive workers into active teammates. By implementing natural conversation triggers, multi-agent communication, concurrent work+talk, and rich conversation content, agents become "fellow players" rather than "idle workers waiting for commands."

The key innovations are:

1. **Non-blocking conversations** - Work never stops for chat
2. **Shared context** - Agents know what's been discussed
3. **Personality-driven dialogue** - Unique voices for each agent
4. **Mace as the interface** - Clean player experience through the foreman
5. **Rapport scaling** - Conversations grow richer over time

The implementation builds on existing infrastructure (ProactiveDialogueManager, ConversationManager, CompanionMemory, AgentCommunicationBus) and adds queue-based management, trigger systems, and multi-agent conversation patterns.
