# Quick Start Guide - MineWright Implementation

**For developers starting work on the MineWright Companion System**

---

## Executive Summary (TL;DR)

**Vision:** Transform the Foreman from a tool into a likeable, intelligent deuteragonist (Gandalf/Ford Prefect/Riker archetype)

**Timeline:** 12 weeks, 5 phases
**New Files:** 60 files
**Modified Files:** 15 files
**Team Size:** 2-3 developers

**Key Insight:** The killer feature is the RELATIONSHIP between player and the Foreman. Everything should enhance this bond.

---

## Phase Overview

```
Phase 1 (Week 1-2):  Foundation - Service layer, config, events
Phase 2 (Week 3-5):  Character - Personality, humor, milestones
Phase 3 (Week 6-7):  Intelligence - Vector memory, skills, ReAct
Phase 4 (Week 8-9):  Orchestration - Foreman, Contract Net, collab building
Phase 5 (Week 10-12): Voice & UX - STT/TTS, GUI enhancements
```

---

## Where to Start

### If you're working on Foundation (Week 1-2)

**Read these files first:**
```
src/main/java/com/minewright/ai/di/SimpleServiceContainer.java
src/main/java/com/minewright/ai/event/SimpleEventBus.java
src/main/java/com/minewright/ai/llm/OpenAIClient.java
src/main/java/com/minewright/ai/config/MineWrightConfig.java
```

**Your first task:**
Create `service/AIService.java` interface with these methods:
```java
CompletableFuture<ExecutionResult> executeCommand(String minewrightName, String command);
MemoryService getMemoryService(String minewrightName);
CompanionService getCompanionService(String minewrightName);
```

**Key principle:** All functionality goes through service interfaces. No direct component access.

### If you're working on Character (Week 3-5)

**Read these files first:**
```
src/main/java/com/minewright/ai/memory/CompanionMemory.java
src/main/java/com/minewright/ai/llm/CompanionPromptBuilder.java
```

**Your first task:**
Create `personality/MoodSystem.java` that returns current mood based on:
- Recent successes/failures
- Rapport level with player
- Time of day in-game

**Key principle:** Personality should be felt in EVERY interaction. No generic responses.

### If you're working on Intelligence (Week 6-7)

**Read these files first:**
```
src/main/java/com/minewright/ai/memory/CompanionMemory.java (findRelevantMemories)
src/main/java/com/minewright/ai/action/ActionExecutor.java
```

**Your first task:**
Create `memory/vector/SemanticMemorySearch.java` with this API:
```java
List<EpisodicMemory> findRelevantMemories(String query, int topK)
```

**Key principle:** Semantic search > keyword matching. Use embeddings.

### If you're working on Orchestration (Week 8-9)

**Read these files first:**
```
src/main/java/com/minewright/ai/execution/OrchestratorService.java
src/main/java/com/minewright/ai/entity/ForemanEntity.java (orchestration methods)
```

**Your first task:**
Create `foreman/ForemanPersonality.java` with this archetype:
- Confident, encouraging, slightly bossy but likeable
- Gandalf (guidance) + Riker (leadership) + Ford Prefect (wit)

**Key principle:** Foreman should feel DISTINCT from workers. Different personality, different role.

### If you're working on Voice (Week 10-12)

**Read these files first:**
```
src/main/java/com/minewright/ai/client/MineWrightGUI.java
```

**Your first task:**
Create `voice/VoiceManager.java` with this flow:
1. Player presses V (push-to-talk)
2. Capture audio
3. Send to Whisper API
4. Get transcript
5. Send to MineWright (existing flow)
6. Get response
7. Send to TTS API
8. Play at MineWright's position

**Key principle:** Voice must feel NATURAL. Low latency is critical.

---

## Architecture Patterns

### Service Layer Pattern
```java
// DON'T: Direct component access
TaskPlanner planner = new TaskPlanner();

// DO: Service-based access
AIService aiService = serviceContainer.getService(AIService.class);
aiService.executeCommand(minewrightName, command);
```

### Event-Driven Pattern
```java
// DON'T: Direct method calls
companionMemory.recordInteraction(player, type, positive);

// DO: Event publication
eventBus.publish(new InteractionRecordedEvent(minewrightName, player, type, positive));
```

### Dependency Injection Pattern
```java
// DON'T: New up dependencies
public class CompanionServiceImpl {
    private MemoryService memory = new MemoryServiceImpl();  // BAD
}

// DO: Constructor injection
public class CompanionServiceImpl {
    private final MemoryService memory;

    public CompanionServiceImpl(ServiceContainer container) {
        this.memory = container.getService(MemoryService.class);
    }
}
```

---

## Code Style Guidelines

### File Organization
```
src/main/java/com/minewright/ai/
├── service/          # High-level service interfaces
├── impl/             # Service implementations
├── personality/      # Character personality system
├── humor/            # Jokes and wit
├── milestone/        # Relationship milestones
├── memory/           # Memory systems (already exists)
├── llm/              # LLM integration (already exists)
├── action/           # Action execution (already exists)
├── orchestration/    # Multi-agent coordination (already exists)
└── client/           # GUI and client-side (already exists)
```

### Naming Conventions
```java
// Interfaces: Simple, descriptive names
interface AIService { }
interface MemoryService { }

// Implementations: Interface + Impl
class AIServiceImpl implements AIService { }

// Events: What + Event
class CommandReceivedEvent { }
class RelationshipUpdatedEvent { }

// Builders: What + Builder
class AgentMessage.Builder { }
```

### Error Handling
```java
// DON'T: Silent failures
try {
    llmCall();
} catch (Exception e) {
    // Ignore
}

// DO: Graceful degradation
try {
    return llmCall();
} catch (LLMException e) {
    LOGGER.warn("LLM call failed, using cache", e);
    return cache.get(prompt);
}
```

---

## Testing Guidelines

### Unit Tests
```java
@Test
public void testPersonalityAffectsDialogue() {
    // Given: Enthusiastic personality
    PersonalityProfile personality = PersonalityPresets.ENTHUSIASTIC;
    MoodSystem moodSystem = new MoodSystem(personality);

    // When: Generate dialogue
    String response = generateDialogue(moodSystem, "Task complete!");

    // Then: Should be enthusiastic
    assertTrue(response.contains("!"));
    assertFalse(response.matches(".*[.!?]\\s*[a-z].*"));  // No lowercase after punctuation
}
```

### Integration Tests
```java
@Test
public void testCommandExecutionFlow() {
    // Given: Test fixture
    AITestFixture fixture = new AITestFixture();
    TestContext context = fixture.createTestContext("TestMineWright");

    // When: Execute command
    AIService aiService = context.getContainer().getService(AIService.class);
    CompletableFuture<ExecutionResult> future = aiService.executeCommand("TestMineWright", "Mine 5 iron ore");

    // Then: Wait for completion
    ExecutionResult result = future.join(30, TimeUnit.SECONDS);
    assertTrue(result.isSuccess());

    // And: Verify event flow
    context.assertEventPublished(CommandReceivedEvent.class);
    context.assertEventPublished(CommandExecutionCompletedEvent.class);
}
```

---

## Performance Guidelines

### LLM Call Optimization
```java
// DON'T: Call LLM for every decision
String response = llm.complete("What should I say?");  // EXPENSIVE

// DO: Use templates for common cases
if (isCommonSituation()) {
    response = templateEngine.fill("Great job on the {task}!", params);
} else {
    response = llm.complete("What should I say?");  // Only for unique cases
}
```

### Caching Strategy
```java
// Semantic cache for LLM responses
String cacheKey = prompt + "|" + contextHash;
String cached = llmCache.get(cacheKey);
if (cached != null) {
    return cached;  // Cache hit!
}

// ... call LLM ...
llmCache.put(cacheKey, response, expiry = 1 hour);
```

### Async Operations
```java
// DON'T: Block the game thread
String response = llm.complete(prompt);  // FREEZES GAME!

// DO: Use async
CompletableFuture<String> future = llm.completeAsync(prompt);
// Check future.isDone() in tick()
```

---

## Common Pitfalls

### Pitfall 1: Breaking Character
```java
// BAD: Generic responses
public String onSuccess() {
    return "Task completed successfully.";  // ROBOT
}

// GOOD: Personality-driven
public String onSuccess() {
    if (personality.humor > 50) {
        return "Nailed it! What's next?";
    } else {
        return "Task complete. Awaiting orders.";
    }
}
```

### Pitfall 2: Ignoring Context
```java
// BAD: Same joke every time
public String tellJoke() {
    return "Why did the creeper cross the road? To get to the other ssside!";
}

// GOOD: Context-aware
public String tellJoke(Context context) {
    if (context.recentlyHeard("creeper joke")) {
        return "So... uh... nice weather we're having?";
    }
    return "Why did the creeper cross the road? To get to the other ssside!";
}
```

### Pitfall 3: Over-Engineering
```java
// BAD: Complex abstractions for simple things
interface IJokeGeneratorStrategyFactory { }

// GOOD: Simple and direct
class JokeGenerator {
    String tellJoke(Context context) { }
}
```

---

## Debugging Tips

### Enable Debug Logging
```toml
# config/minewright-common.toml
[logging]
level = "DEBUG"
packages = ["com.minewright"]
```

### Event Inspection
```java
// Add event listener for debugging
eventBus.subscribe(Event.class, event -> {
    LOGGER.debug("Event: {} | Payload: {}", event.getClass(), event);
});
```

### LLM Prompt Inspection
```java
// Log all LLM prompts for debugging
llmClient.setPromptLogger(prompt -> {
    LOGGER.debug("LLM Prompt:\n{}", prompt);
});
```

---

## Key Files to Understand

### Before You Start Coding, Read These:

**Foundation:**
- `SimpleServiceContainer.java` - How dependencies are managed
- `SimpleEventBus.java` - How components communicate
- `MineWrightConfig.java` - How configuration works

**Character:**
- `CompanionMemory.java` - How relationships are tracked
- `CompanionPromptBuilder.java` - How prompts are built

**Intelligence:**
- `TaskPlanner.java` - How LLM planning works
- `ActionExecutor.java` - How actions are executed

**Orchestration:**
- `OrchestratorService.java` - How multi-agent coordination works
- `ForemanEntity.java` - How entities are implemented

---

## Weekly Deliverables Checklist

### Week 1
- [ ] AIService interface
- [ ] ServiceRegistry implementation
- [ ] LifecycleServiceContainer
- [ ] Unit tests for container

### Week 2
- [ ] ConfigManager with hot-reload
- [ ] EventIntegrator
- [ ] All event types defined
- [ ] Integration tests

### Week 3
- [ ] MoodSystem
- [ ] PersonalityEngine
- [ ] HumorEngine
- [ ] Beta test with players

### Week 4
- [ ] MilestoneDetector
- [ ] Anniversary triggers
- [ ] Celebration dialogue

### Week 5
- [ ] ProactiveCommentary
- [ ] ConversationManager
- [ ] Beta test feedback

### Week 6
- [ ] SemanticMemorySearch
- [ ] MemoryEmbeddingService
- [ ] SkillLibrary

### Week 7
- [ ] ReAct loop
- [ ] Self-verification
- [ ] Performance tests

### Week 8
- [ ] ForemanPersonality
- [ ] ContractNetProtocol
- [ ] Multi-agent tests

### Week 9
- [ ] Worker recognition
- [ ] Dynamic rebalancing
- [ ] Quality verification

### Week 10-11
- [ ] Voice capture
- [ ] Whisper STT integration
- [ ] OpenAI TTS integration

### Week 12
- [ ] GUI enhancements
- [ ] Performance optimization
- [ ] Final polish

---

## Questions?

**Architecture Questions:** Ask about service layer, events, DI
**Character Questions:** Ask about personality, humor, milestones
**Intelligence Questions:** Ask about memory, skills, ReAct
**Orchestration Questions:** Ask about foreman, Contract Net
**Voice/UX Questions:** Ask about STT/TTS, GUI

---

## Resources

**Internal Documents:**
- `IMPLEMENTATION_ROADMAP.md` - Full implementation plan
- `TIMELINE.md` - Visual timeline
- `research/` - All research summaries

**External Resources:**
- OpenAI API docs: https://platform.openai.com/docs
- Minecraft Forge docs: https://docs.minecraftforge.net
- Java 17 features: https://openjdk.org/projects/jdk/17

---

**Document Version:** 1.0
**Last Updated:** 2026-02-26
**Maintainer:** MineWright Development Team
**Status:** Ready for Developers
