# Implementation Roadmap - MineWright Companion System

**Version:** 1.0
**Date:** 2026-02-26
**Status:** Ready for Implementation
**Vision:** Transform the Foreman from a tool into a likeable, intelligent deuteragonist (Gandalf/Ford Prefect/Riker archetype)

---

## Executive Summary

This roadmap synthesizes 11 comprehensive research documents into a prioritized implementation plan for evolving MineWright from a functional tool into a compelling companion character. The research spans multi-agent orchestration, character AI systems, memory architectures, conversation patterns, voice integration, performance optimization, and relationship milestones.

**Core Vision:** Create a foreman Foreman agent that players genuinely enjoy spending time with - one who remembers shared experiences, grows relationships, tells jokes, celebrates milestones, and coordinates worker agents with personality and intelligence.

**Key Insight:** The killer feature is NOT autonomous gameplay, but rather the *relationship* between player and foreman. Everything should enhance this bond.

**Implementation Strategy:** 5 phases, 12 weeks, focusing on the highest-impact character features first before advanced orchestration.

---

## Phase 1: Foundation (Weeks 1-2) - CRITICAL PATH

**Goal:** Establish the infrastructure that all other features depend on.

### 1.1 Service Layer & Integration (Week 1)

**Rationale:** Existing components (LLM, memory, orchestration, companion) are isolated. A unified service layer enables clean integration, testing, and lifecycle management.

**Files to Create:**
```
src/main/java/com/minewright/ai/service/
├── AIService.java (interface)
├── impl/AIServiceImpl.java
├── ServiceRegistry.java
└── di/LifecycleServiceContainer.java
```

**Implementation Steps:**
1. Define `AIService` interface facade for all AI capabilities
2. Implement `ServiceRegistry` with lifecycle management (init/shutdown)
3. Extend `SimpleServiceContainer` with lifecycle callbacks
4. Create `AILifecycleManager` for startup/shutdown sequencing
5. Wire existing components into service layer

**Dependencies:**
- Requires: Existing EventBus, LLMClient, ActionRegistry
- Enables: All subsequent phases

**Success Criteria:**
- All services registered and initialized in correct order
- Clean shutdown without resource leaks
- Unit tests for container lifecycle

### 1.2 Configuration Management (Week 1)

**Rationale:** Current config is monolithic. Hierarchical config (global/world/entity) with hot-reload enables per-MineWright personalities and world-specific behaviors.

**Files to Create:**
```
src/main/java/com/minewright/ai/config/
├── ConfigManager.java
├── GlobalConfig.java
├── WorldConfig.java
└── EntityConfig.java
```

**Implementation Steps:**
1. Create config layer merging system (Entity > World > Global priority)
2. Implement file watching for hot-reload
3. Define config schema (TOML for global, JSON for overrides)
4. Migrate existing `MineWrightConfig` to new system
5. Add per-MineWright personality presets

**Configuration Schema:**
```toml
[companion]
enablePersonality = true
defaultPersonality = "friendly"

[personality.friendly]
humor = 70
encouragement = 80
formality = 30
catchphrases = ["Let's do this!", "We've got this!"]

[personality.professional]
humor = 30
encouragement = 50
formality = 70
catchphrases = ["Proceeding.", "Acknowledged."]
```

**Success Criteria:**
- Hot-reload works without server restart
- Per-MineWright personalities persist
- Migration from old config complete

### 1.3 Event Integration (Week 2)

**Rationale:** Components currently communicate via direct calls. Event-driven architecture enables loose coupling and easier feature additions.

**Files to Create:**
```
src/main/java/com/minewright/ai/integration/
├── EventIntegrator.java
└── events/
    ├── CommandEvents.java
    ├── MemoryEvents.java
    ├── CompanionEvents.java
    └── OrchestrationEvents.java
```

**Implementation Steps:**
1. Define event taxonomy (30+ event types)
2. Create `EventIntegrator` to register handlers
3. Wire state machine events
4. Wire command execution events
5. Wire memory update events
6. Add event flow documentation

**Key Events:**
```
CommandReceivedEvent → CommandValidatedEvent → ActionStartedEvent
→ ActionCompletedEvent → MemoryStoredEvent → RelationshipUpdatedEvent
```

**Success Criteria:**
- All component communication via EventBus
- Event flow documented
- Integration tests for key flows

---

## Phase 2: Character System (Weeks 3-5) - KILLER FEATURES

**Goal:** Implement the personality, humor, and relationship systems that make MineWright likeable.

### 2.1 Enhanced Personality System (Week 3)

**Rationale:** Current `CompanionMemory.PersonalityProfile` is a good start. Enhance with dynamic personality that evolves based on interactions.

**Files to Modify:**
```
src/main/java/com/minewright/ai/memory/CompanionMemory.java
src/main/java/com/minewright/ai/llm/CompanionPromptBuilder.java
```

**Files to Create:**
```
src/main/java/com/minewright/ai/personality/
├── PersonalityEngine.java
├── MoodSystem.java
└── presets/
    └── PersonalityPresets.java
```

**Implementation Steps:**
1. Add mood system (current mood affects responses)
2. Implement personality drift (slowly adapts to player)
3. Create personality presets (enthusiastic, professional, playful, stoic)
4. Add catchphrase usage tracking (avoid repetition)
5. Implement humor timing system

**Mood System:**
```java
public enum Mood {
    CHEERFUL("Positive, energetic responses"),
    THOUGHTFUL("Contemplative, careful responses"),
    FRUSTRATED("Slightly annoyed but still helpful"),
    EXCITED("High energy, enthusiastic"),
    PROUD("Satisfied with accomplishments");
}

public Mood getCurrentMood() {
    // Based on recent successes, rapport level, time of day
}
```

**Success Criteria:**
- Personality affects all dialogue
- Mood changes based on context
- No catchphrase repetition (within 20 interactions)

### 2.2 Humor & Wit System (Week 3-4)

**Rationale:** Humor is critical for likeability. Research shows well-timed jokes significantly increase rapport.

**Files to Create:**
```
src/main/java/com/minewright/ai/humor/
├── HumorEngine.java
├── MinecraftJokes.java
├── TimingAnalyzer.java
└── InsideJokeTracker.java
```

**Implementation Steps:**
1. Create Minecraft-specific joke library
2. Implement timing rules (don't interrupt, wait for pauses)
3. Add joke recovery (if joke fails, acknowledge it)
4. Implement rapport-based humor progression
5. Add inside joke detection and storage

**Humor Progression:**
```
Rapport 0-20:   No jokes, serious
Rapport 20-40:   Safe puns, mild humor
Rapport 40-60:   Minecraft references, light jokes
Rapport 60-80:   Inside jokes, playful teasing
Rapport 80-100:  Full humor, improvisation
```

**Joke Template:**
```java
String template = """
Remember that context: {context}
Player rapport: {rapport}
Recent events: {events}
Generate a brief, natural joke (1 sentence) that MineWright would make.
Keep it Minecraft-themed. Don't be too cheesy.
""";
```

**Success Criteria:**
- Jokes feel natural, not forced
- Timing respects game state
- Failed jokes have recovery dialogue

### 2.3 Relationship Milestones (Week 4-5)

**Rationale:** Milestones give players a sense of progression and shared history. They create emotional investment in the relationship.

**Files to Create:**
```
src/main/java/com/minewright/ai/milestone/
├── MilestoneDetector.java
├── Milestone.java
├── triggers/
│   ├── FirstMeetingTrigger.java
│   ├── AnniversaryTrigger.java
│   └── AchievementTrigger.java
└── dialogue/
    └── MilestoneDialogueGenerator.java
```

**Implementation Steps:**
1. Implement milestone trigger system
2. Create "firsts" tracking (first build, first diamond, first death)
3. Add anniversary detection (1 day, 1 week, 1 month)
4. Implement achievement counters (structures, blocks mined)
5. Create celebration dialogue generator

**Milestone Categories:**
```
FIRSTS:        First meeting, first build, first diamond
ANNIVERSARY:    1 day, 1 week, 1 month, 100 days
ACHIEVEMENT:    10 structures, 1000 blocks mined, 50 deaths
AFFECTION:     First inside joke, first trust moment
TRUST:         Inventory access, autonomous decisions
```

**Celebration Dialogue:**
```java
// Example: 1 week anniversary
if (daysSinceMeeting == 7) {
    return String.format(
        "Can you believe it's been a week? We've built %d structures, " +
        "mined %d blocks, and died %d times together. Here's to the next week!",
        structuresBuilt, blocksMined, deaths
    );
}
```

**Success Criteria:**
- All "firsts" tracked and celebrated
- Anniversaries detected accurately
- Dialogue varies based on personality

### 2.4 Proactive Companion AI (Week 5)

**Rationale:** MineWright shouldn't just respond to commands. He should initiate conversations, comment on the world, and show agency.

**Files to Create:**
```
src/main/java/com/minewright/ai/companion/
├── ProactiveCommentary.java
├── ConversationManager.java
└── triggers/
    └── ProactiveTrigger.java
```

**Implementation Steps:**
1. Implement proactive comment triggers (time of day, events)
2. Add conversation state management (don't spam)
3. Create contextual awareness system
4. Implement "remember when" references

**Proactive Triggers:**
```java
// Time-based triggers
if (isSunrise()) comment("Beautiful morning! Ready to build?");
if (isNight()) comment("Watch out for mobs - want me to take watch?");

// Event-based triggers
if (playerAchievement()) comment("Congrats on the achievement!");
if (nearDeath()) comment("That was close! You okay?");

// Context-based triggers
if (idleTooLong()) comment("What should we work on next?");
if (richInventory()) comment("We've got quite the stockpile here.");
```

**Success Criteria:**
- Proactive comments feel natural, not annoying
- Throttling prevents spam
- Comments reference shared history

---

## Phase 3: Memory & Intelligence (Weeks 6-7)

**Goal:** Implement semantic memory search and skill learning for more intelligent behavior.

### 3.1 Vector-Based Memory Search (Week 6)

**Rationale:** Current `findRelevantMemories()` uses simple keyword matching. Vector embeddings enable semantic understanding.

**Files to Modify:**
```
src/main/java/com/minewright/ai/memory/CompanionMemory.java
src/main/java/com/minewright/ai/memory/embedding/
```

**Files to Create:**
```
src/main/java/com/minewright/ai/memory/vector/
├── SemanticMemorySearch.java
└── MemoryEmbeddingService.java
```

**Implementation Steps:**
1. Integrate real embedding model (OpenAI ada-002 or local)
2. Generate embeddings for all episodic memories
3. Implement cosine similarity search
4. Add memory relevance scoring
5. Cache search results for performance

**Search Algorithm:**
```java
public List<EpisodicMemory> findRelevantMemories(String query, int topK) {
    float[] queryEmbedding = embeddingModel.embed(query);

    List<VectorSearchResult<EpisodicMemory>> results =
        memoryVectorStore.search(queryEmbedding, topK);

    // Re-rank by recency and emotional weight
    return results.stream()
        .sorted((a, b) -> {
            double scoreA = a.similarity * 0.7 + recency(a.data) * 0.3;
            double scoreB = b.similarity * 0.7 + recency(b.data) * 0.3;
            return Double.compare(scoreB, scoreA);
        })
        .limit(topK)
        .map(VectorSearchResult::getData)
        .collect(Collectors.toList());
}
```

**Success Criteria:**
- Semantic search finds conceptually similar memories
- Search latency < 100ms for 1000 memories
- Results improve over time with more memories

### 3.2 Skill Library with Embeddings (Week 6-7)

**Rationale:** Store successful action sequences and retrieve them by semantic similarity. Enables learning and faster task completion.

**Files to Create:**
```
src/main/java/com/minewright/ai/skills/
├── SkillLibrary.java
├── Skill.java
└── SkillLearningEngine.java
```

**Implementation Steps:**
1. Define skill data structure (description, code, embedding, usage count)
2. Implement skill storage on action success
3. Add semantic skill retrieval
4. Track success rates per skill
5. Implement skill suggestion in prompts

**Skill Storage:**
```java
public void onActionComplete(BaseAction action, ActionResult result) {
    if (result.isSuccess()) {
        String skillCode = action.toExecutableCode();
        String description = action.toNaturalLanguageDescription();

        skillLibrary.storeSkill(
            description,
            skillCode,
            true  // succeeded
        );
    }
}
```

**Skill Retrieval in Planning:**
```java
public String buildPromptWithSkills(Task userCommand, WorldState state) {
    List<Skill> relevantSkills = skillLibrary.retrieveSkills(
        userCommand,
        state,
        5  // top 5
    );

    if (!relevantSkills.isEmpty()) {
        prompt.append("\n## Relevant Skills (Reference these for code patterns)\n");
        for (Skill skill : relevantSkills) {
            prompt.append("- ").append(skill.description).append("\n");
            prompt.append("  ```\n").append(skill.code).append("```\n");
        }
    }

    return prompt.toString();
}
```

**Success Criteria:**
- Skills stored on every successful action
- Semantic retrieval finds relevant skills
- LLM uses skill patterns in generated code

### 3.3 ReAct Loop Implementation (Week 7)

**Rationale:** Interleave reasoning and action for dynamic adaptation. Better than rigid planning for complex tasks.

**Files to Modify:**
```
src/main/java/com/minewright/ai/llm/TaskPlanner.java
src/main/java/com/minewright/ai/action/ActionExecutor.java
```

**Files to Create:**
```
src/main/java/com/minewright/ai/react/
├── ReactExecutor.java
└── ThoughtActionLoop.java
```

**Implementation Steps:**
1. Implement thought generation step
2. Add action decision step
3. Implement observation step
4. Add self-verification after completion
5. Limit iterations to avoid infinite loops

**ReAct Loop:**
```java
for (int iteration = 0; iteration < maxIterations; iteration++) {
    // Thought: Reason about next step
    String thought = llm.generate(context +
        "Thought: What should I do next to achieve the goal?");

    // Action: Decide on action
    String actionDecision = llm.generate(context +
        "Available actions: " + actions.listActions() +
        "\nAction: Which action should I take?");

    // Execute the action
    ActionResult result = actions.execute(actionDecision, state);

    // Observation: Observe results
    context += "\nObservation: " + result.toSummary();

    // Check if goal is achieved
    if (result.isGoalAchieved()) {
        return result;
    }
}
```

**Success Criteria:**
- ReAct loop adapts to changing conditions
- Self-verification catches 60%+ of failures
- Max iterations prevents infinite loops

---

## Phase 4: Multi-Agent Orchestration (Weeks 8-9)

**Goal:** Enhance the foreman/worker system with better coordination and communication.

### 4.1 Enhanced Foreman Personality (Week 8)

**Rationale:** The foreman should have a distinct personality - confident, encouraging, slightly bossy but likeable. Gandalf/Riker archetype.

**Files to Modify:**
```
src/main/java/com/minewright/ai/llm/CompanionPromptBuilder.java
src/main/java/com/minewright/ai/execution/OrchestratorService.java
```

**Files to Create:**
```
src/main/java/com/minewright/ai/foreman/
├── ForemanPromptBuilder.java
├── ForemanPersonality.java
└── CoordinationDialogue.java
```

**Implementation Steps:**
1. Create foreman-specific personality preset
2. Add foreman coordination dialogue
3. Implement progress reporting personality
4. Add "let me handle this" moments

**Foreman Personality:**
```java
PersonalityProfile foremanPersonality = new PersonalityProfile();
foremanPersonality.openness = 60;           // Open to suggestions
foremanPersonality.conscientiousness = 90;  // Very organized
foremanPersonality.extraversion = 75;      // Outgoing leader
foremanPersonality.agreeableness = 70;     // Cooperative but decisive
foremanPersonality.neuroticism = 30;       // Confident, calm
foremanPersonality.humor = 50;             // Some humor
foremanPersonality.encouragement = 90;     // Very encouraging
foremanPersonality.formality = 40;         // Casual but professional
```

**Coordination Dialogue:**
```java
// When assigning tasks
"Hey {worker}, I need you to {task}. Think you can handle it?"

// When checking progress
"How's it going, {worker}? Making good progress on {task}?"

// When task completes
"Great work on that {task}, {worker}! One step closer!"

// When task fails
"No worries, {worker}. These things happen. Let's try a different approach."
```

**Success Criteria:**
- Foreman feels distinct from workers
- Dialogue shows leadership and encouragement
- Players feel guided, not bossed around

### 4.2 Contract Net Protocol (Week 8-9)

**Rationale:** Current task assignment is simple. Contract Net Protocol enables workers to bid on tasks based on capabilities.

**Files to Modify:**
```
src/main/java/com/minewright/ai/execution/OrchestratorService.java
src/main/java/com/minewright/ai/entity/ForemanEntity.java
```

**Files to Create:**
```
src/main/java/com/minewright/ai/contract/
├── ContractNetProtocol.java
├── TaskAnnouncement.java
├── BidMessage.java
└── AwardMessage.java
```

**Implementation Steps:**
1. Implement task announcement (broadcast to all workers)
2. Add bid calculation (capability + proximity + inventory)
3. Implement award selection (best bid wins)
4. Add contract execution monitoring
5. Handle contract refusal (worker too busy/far)

**Contract Flow:**
```
FOREMAN: "I need someone to mine 64 cobblestone"
  └─> Announces task to all workers

WORKER 1: "I can do it! Distance: 5 blocks, Has tool: Yes, Bid: 95"
WORKER 2: "I can do it! Distance: 20 blocks, Has tool: Yes, Bid: 80"
WORKER 3: "Too busy, passing"

FOREMAN: "Worker 1, you got the job! 64 cobblestone, at X:5, Y:64, Z:10"
WORKER 1: "On it!"
```

**Bid Calculation:**
```java
public double calculateBid(Task task, WorkerState worker) {
    double proximityScore = 100 - worker.distanceTo(task.location);
    double capabilityScore = worker.hasRequiredCapability(task) ? 100 : 0;
    double availabilityScore = worker.isIdle() ? 100 : 0;
    double inventoryScore = worker.hasRequiredResources(task) ? 100 : 0;

    return (proximityScore * 0.3 +
            capabilityScore * 0.4 +
            availabilityScore * 0.2 +
            inventoryScore * 0.1);
}
```

**Success Criteria:**
- Tasks assigned to best-suited workers
- Workers can refuse inappropriate tasks
- Coordination feels intelligent

### 4.3 Collaborative Building Enhancement (Week 9)

**Rationale:** Existing `CollaborativeBuildManager` works. Enhance with foreman supervision and worker recognition.

**Files to Modify:**
```
src/main/java/com/minewright/ai/action/CollaborativeBuildManager.java
```

**Implementation Steps:**
1. Add foreman progress monitoring
2. Implement worker recognition ("great job on that wall")
3. Add dynamic rebalancing (if worker finishes early)
4. Implement quality checks (foreman verifies structure)
5. Add celebration on completion

**Collaborative Dialogue:**
```java
// Foreman during build
"Team, we're making great progress! Keep it up!"

// Foreman recognizing worker
"{workerName}, that section looks perfect!"

// Foreman on completion
"Everyone, excellent work! That {structure} turned out amazing."
"I couldn't have done it without you all."
```

**Success Criteria:**
- Workers feel recognized and valued
- Dynamic rebalancing works smoothly
- Celebration feels earned

---

## Phase 5: Voice & UX (Weeks 10-12)

**Goal:** Add voice interaction and polish the user experience.

### 5.1 Voice Integration (Week 10-11)

**Rationale:** Voice enables natural conversation without typing. Critical for immersion.

**Files to Create:**
```
src/main/java/com/minewright/ai/voice/
├── VoiceManager.java
├── capture/
│   ├── AudioCapture.java
│   └── CaptureManager.java
├── stt/
│   ├── WhisperSTT.java
│   └── STTConfig.java
├── tts/
│   ├── OpenAITTS.java
│   └── VoiceProfile.java
├── vad/
│   ├── SileroVAD.java
│   └── SimpleVAD.java
└── VoiceConfig.java
```

**Implementation Steps:**
1. Implement audio capture (Java Sound API)
2. Add push-to-talk key binding (V key)
3. Integrate OpenAI Whisper API for STT
4. Integrate OpenAI TTS API for speech
5. Add 3D positional audio playback

**Audio Flow:**
```
Player presses V (push-to-talk)
         ↓
   AudioCapture.start()
         ↓
   Detect speech (VAD)
         ↓
   Send to Whisper API
         ↓
   Get transcribed text
         ↓
   Send to MineWright (existing flow)
         ↓
   MineWright generates response
         ↓
   Send to TTS API
         ↓
   Play at MineWright's position (3D audio)
```

**Configuration:**
```toml
[voice]
enabled = true
activation_mode = "push_to_talk"
ptt_key = 86  # V key

[voice.stt]
provider = "openai"
model = "whisper-1"
language = "en"

[voice.tts]
provider = "openai"
model = "tts-1"
voice = "nova"  # alloy, echo, fable, onyx, nova, shimmer
```

**Success Criteria:**
- Voice commands work reliably
- TTS plays at MineWright's location
- Latency < 2 seconds end-to-end

### 5.2 GUI Enhancements (Week 11-12)

**Rationale:** Current GUI is basic. Enhanced GUI shows relationship progress, milestones, and conversation history.

**Files to Modify:**
```
src/main/java/com/minewright/ai/client/MineWrightGUI.java
src/main/java/com/minewright/ai/client/MineWrightOverlay.java
```

**Files to Create:**
```
src/main/java/com/minewright/ai/client/gui/
├── CompanionTab.java
├── RelationshipDisplay.java
├── ConversationHistory.java
└── MilestoneDisplay.java
```

**Implementation Steps:**
1. Add relationship meter (rapport, trust)
2. Show milestone timeline
3. Add conversation history with search
4. Implement personality customization
5. Add voice configuration screen

**GUI Layout:**
```
┌─────────────────────────────────────────┐
│ MineWright - Friendly (Rapport: 65/100)     │
├─────────────────────────────────────────┤
│ [Relationship] [Conversation] [Settings] │
├─────────────────────────────────────────┤
│                                         │
│  Relationship Progress                   │
│  ████████████████░░░░░ 65%             │
│                                         │
│  Milestones                             │
│  ✓ First meeting (1 day ago)            │
│  ✓ First build (12 hours ago)           │
│  ○ First diamond                        │
│  ○ 1 week anniversary                    │
│                                         │
│  Recent Conversation                     │
│  Player: "How's the building going?"    │
│  MineWright: "Making great progress! The    │
│          workers are almost done."      │
│                                         │
└─────────────────────────────────────────┘
```

**Success Criteria:**
- Relationship progress visible
- All milestones shown
- Conversation history searchable

---

## File-Level Implementation Plan

### New Files to Create (60 files)

```
Phase 1: Foundation (12 files)
├── service/AIService.java
├── service/impl/AIServiceImpl.java
├── service/ServiceRegistry.java
├── di/LifecycleServiceContainer.java
├── config/ConfigManager.java
├── config/GlobalConfig.java
├── config/WorldConfig.java
├── config/EntityConfig.java
├── integration/EventIntegrator.java
├── integration/events/CommandEvents.java
├── integration/events/MemoryEvents.java
└── integration/events/CompanionEvents.java

Phase 2: Character System (18 files)
├── personality/PersonalityEngine.java
├── personality/MoodSystem.java
├── personality/presets/PersonalityPresets.java
├── humor/HumorEngine.java
├── humor/MinecraftJokes.java
├── humor/TimingAnalyzer.java
├── humor/InsideJokeTracker.java
├── milestone/MilestoneDetector.java
├── milestone/Milestone.java
├── milestone/triggers/FirstMeetingTrigger.java
├── milestone/triggers/AnniversaryTrigger.java
├── milestone/triggers/AchievementTrigger.java
├── milestone/dialogue/MilestoneDialogueGenerator.java
├── companion/ProactiveCommentary.java
├── companion/ConversationManager.java
├── companion/triggers/ProactiveTrigger.java
└── character/CharacterArchetype.java

Phase 3: Memory & Intelligence (10 files)
├── memory/vector/SemanticMemorySearch.java
├── memory/vector/MemoryEmbeddingService.java
├── skills/SkillLibrary.java
├── skills/Skill.java
├── skills/SkillLearningEngine.java
├── react/ReactExecutor.java
├── react/ThoughtActionLoop.java
├── intelligence/ContextBuilder.java
├── intelligence/RecoveryStrategy.java
└── intelligence/SelfVerification.java

Phase 4: Multi-Agent Orchestration (10 files)
├── foreman/ForemanPromptBuilder.java
├── foreman/ForemanPersonality.java
├── foreman/CoordinationDialogue.java
├── contract/ContractNetProtocol.java
├── contract/TaskAnnouncement.java
├── contract/BidMessage.java
├── contract/AwardMessage.java
├── coordination/WorkerRecognition.java
├── coordination/DynamicRebalancing.java
└── coordination/QualityVerification.java

Phase 5: Voice & UX (10 files)
├── voice/VoiceManager.java
├── voice/capture/AudioCapture.java
├── voice/capture/CaptureManager.java
├── voice/stt/WhisperSTT.java
├── voice/stt/STTConfig.java
├── voice/tts/OpenAITTS.java
├── voice/tts/VoiceProfile.java
├── voice/vad/SileroVAD.java
├── voice/vad/SimpleVAD.java
└── client/gui/CompanionTab.java
```

### Files to Modify (15 files)

```
Core modifications:
├── llm/CompanionPromptBuilder.java (enhance with personality)
├── llm/TaskPlanner.java (add skill library integration)
├── memory/CompanionMemory.java (add vector search)
├── action/ActionExecutor.java (add ReAct loop)
├── entity/ForemanEntity.java (add companion integration)
├── execution/OrchestratorService.java (enhance coordination)
├── execution/AgentCommunicationBus.java (add voice support)
├── action/CollaborativeBuildManager.java (add recognition)
├── client/MineWrightGUI.java (add companion tab)
├── client/MineWrightOverlay.java (enhance display)
├── di/SimpleServiceContainer.java (add lifecycle)
├── event/SimpleEventBus.java (add filtering)
├── plugin/ActionRegistry.java (add skill tracking)
├── config/MineWrightConfig.java (migrate to new system)
└── build.gradle (add voice dependencies)
```

---

## Dependency Graph

```
Phase 1 (Foundation)
├── Service Layer
│   ├── Enables → Phase 2 (Character)
│   ├── Enables → Phase 3 (Memory)
│   ├── Enables → Phase 4 (Orchestration)
│   └── Enables → Phase 5 (Voice)
├── Config Management
│   ├── Enables → Phase 2 (Personality presets)
│   └── Enables → Phase 4 (Foreman personality)
└── Event Integration
    ├── Enables → Phase 2 (Proactive triggers)
    ├── Enables → Phase 3 (ReAct loop)
    └── Enables → Phase 4 (Contract Net)

Phase 2 (Character System)
├── Enhanced Personality
│   ├── Enables → Phase 4 (Foreman archetype)
│   └── Enables → Phase 5 (Voice TTS personality)
├── Humor System
│   └── Enables → Phase 2 (Milestone celebrations)
├── Relationship Milestones
│   ├── Requires → Phase 3 (Memory search)
│   └── Requires → Phase 1 (Event system)
└── Proactive Companion
    ├── Requires → Phase 1 (Event system)
    └── Requires → Phase 3 (Memory)

Phase 3 (Memory & Intelligence)
├── Vector-Based Memory Search
│   ├── Enables → Phase 2 (Relevant memories for dialogue)
│   └── Enables → Phase 4 (Coordination context)
├── Skill Library
│   ├── Enables → Phase 3 (ReAct loop)
│   └── Requires → Phase 1 (Service layer)
└── ReAct Loop
    ├── Requires → Phase 1 (Service layer)
    └── Enables → Phase 4 (Intelligent coordination)

Phase 4 (Multi-Agent Orchestration)
├── Enhanced Foreman Personality
│   ├── Requires → Phase 2 (Personality system)
│   └── Requires → Phase 3 (Skill library)
├── Contract Net Protocol
│   ├── Requires → Phase 1 (Event system)
│   └── Requires → Phase 3 (Intelligence)
└── Collaborative Building Enhancement
    └── Requires → Phase 2 (Recognition system)

Phase 5 (Voice & UX)
├── Voice Integration
│   ├── Requires → Phase 1 (Service layer)
│   └── Requires → Phase 2 (Personality for TTS)
└── GUI Enhancements
    ├── Requires → Phase 2 (Relationship data)
    └── Requires → Phase 3 (Milestone data)
```

---

## Risk Mitigation

### Risk 1: Performance Degradation

**Description:** Adding character features, voice, and memory systems could impact game performance (TPS).

**Mitigation:**
- All LLM calls are already async (no blocking)
- Throttle expensive operations (vector search, milestone checks)
- Use caching for repeated operations
- Profile with Spark profiler mod during development

**Acceptance Criteria:**
- TPS remains 20 with 10 active MineWrights
- Main thread blocking < 1ms per tick
- Memory usage < 100MB per agent

### Risk 2: LLM API Costs

**Description:** More features = more API calls = higher costs.

**Mitigation:**
- Aggressive caching (semantic cache, skill cache)
- Local execution where possible (decision trees for common tasks)
- Rate limiting and queuing
- Provider diversity (Groq for fast tasks, OpenAI for complex)

**Acceptance Criteria:**
- Cache hit rate > 50%
- Monthly API cost < $10 for moderate usage
- Fallback to local decisions when rate limited

### Risk 3: Dialogue Repetition

**Description:** Players may hear the same phrases repeatedly, breaking immersion.

**Mitigation:**
- Track phrase usage ( CompanionMemory already does this)
- Large template library (50+ variations per phrase)
- Dynamic template generation (LLM-assisted)
- Context-aware selection (don't repeat recent phrases)

**Acceptance Criteria:**
- No phrase repetition within 20 interactions
- Variety keeps conversations fresh
- Players report dialogue feels natural

### Risk 4: Creepy Factor

**Description:** Over-familiarity or excessive tracking could feel invasive.

**Mitigation:**
- Time-gating on relationship features (unlock gradually)
- Player control over data collection
- Avoid surveillance-style tracking
- Keep tone friendly, not romantic

**Acceptance Criteria:**
- Beta testers report comfortable interaction
- No "stalker" vibes
- Relationship progresses at natural pace

### Risk 5: Multi-Agent Coordination Deadlock

**Description:** Contract Net Protocol or task assignment could cause agents to freeze.

**Mitigation:**
- Timeout on all contracts (30 second max)
- Fallback to solo mode if coordinator fails
- Heartbeat messages to detect alive agents
- State machine rollback on errors

**Acceptance Criteria:**
- No agent freeze observed in testing
- Graceful degradation when foreman dies
- Auto-recovery from network issues

---

## Testing Strategy

### Unit Tests (60% coverage target)

```
service/
├── AIServiceTest.java
├── ServiceRegistryTest.java
└── ConfigManagerTest.java

personality/
├── PersonalityEngineTest.java
├── MoodSystemTest.java
└── HumorEngineTest.java

milestone/
├── MilestoneDetectorTest.java
└── MilestoneDialogueGeneratorTest.java

memory/
├── SemanticMemorySearchTest.java
└── SkillLibraryTest.java

contract/
└── ContractNetProtocolTest.java
```

### Integration Tests (30% coverage target)

```
integration/
├── CommandExecutionFlowTest.java
├── RelationshipProgressionTest.java
├── MultiAgentCoordinationTest.java
└── VoiceInteractionFlowTest.java
```

### Performance Tests

```
performance/
├── LLMCachePerformanceTest.java
├── MemorySearchPerformanceTest.java
├── MultiAgentScalingTest.java
└── TPSImpactTest.java
```

---

## Success Metrics

### Phase 1 Success Metrics
- [ ] All services initialize without errors
- [ ] Config hot-reload works within 5 seconds
- [ ] Event system processes 1000 events/second

### Phase 2 Success Metrics
- [ ] Personality affects all dialogue (100% coverage)
- [ ] Jokes generate laugh > 30% of time (beta test)
- [ ] Milestones celebrated within 1 minute of trigger
- [ ] Proactive comments feel natural (not annoying)

### Phase 3 Success Metrics
- [ ] Semantic search finds relevant memories (precision > 70%)
- [ ] Skills reused in similar tasks (reuse rate > 20%)
- [ ] ReAct loop adapts to changes (adaptation rate > 80%)

### Phase 4 Success Metrics
- [ ] Foreman feels distinct from workers (user survey)
- [ ] Contract Net assigns best worker (efficiency gain > 30%)
- [ ] Collaborative building completes faster (speedup > 2x)

### Phase 5 Success Metrics
- [ ] Voice commands work reliably (success rate > 95%)
- [ ] End-to-end latency < 2 seconds (P95)
- [ ] GUI shows all relationship data (completeness 100%)

---

## Conclusion

This roadmap prioritizes the **character development** that makes MineWright a compelling companion rather than just another AI tool. The foreman should feel like a partner - someone players genuinely enjoy spending time with.

**Key Differentiators:**
1. **Relationship-first design** - All features enhance the player-foreman bond
2. **Progressive depth** - Simple at first, grows deeper over time
3. **Natural dialogue** - Jokes, celebrations, and proactive commentary
4. **Intelligent coordination** - Contract Net protocol for smart task assignment
5. **Voice interaction** - Natural conversation without typing

**Estimated Effort:** 12 weeks, 60 new files, 15 modified files

**Recommended Team Size:** 2-3 developers (1 backend, 1 frontend/GUI, 1 QA/Support)

---

**Document Version:** 1.0
**Last Updated:** 2026-02-26
**Maintainer:** MineWright Development Team
**Status:** Ready for Implementation
