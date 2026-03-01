# PRIORITIZED ROADMAP - MineWright Foreman Companion

**Version:** 1.1
**Date:** 2026-03-01
**Status:** Ready for Implementation
**Vision:** Transform the Foreman from a functional tool into a smart, likeable deuteragonist (Gandalf/Ford Prefect/Riker archetype)

---

## Executive Summary

This roadmap synthesizes codebase analysis with 20+ research documents to create a **prioritized, dependency-aware implementation plan**. The analysis reveals:

**2026-03-01 Audit Summary:**
| Metric | Value |
|--------|-------|
| Source Files | 234 |
| Test Files | 54 (23% coverage) |
| Documentation Files | 425 |
| TODO/FIXME Count | 4 (clean codebase) |
| Security Status | All critical issues resolved |

**What's Already Built (Production-Ready):**
- ✅ Async LLM with batching (non-blocking commands)
- ✅ Multi-agent orchestration framework (foreman/worker communication)
- ✅ Memory systems (episodic, semantic, emotional, conversational)
- ✅ State machine with interceptor chain
- ✅ Plugin architecture for actions
- ✅ Proactive dialogue framework (stubs implemented)
- ✅ Artificer archetypes (Lucius Fox, Getafix, Hephaestus, Phineas)
- ✅ Security layer (InputSanitizer, environment variable config)

**What's Designed But Not Built:**
- ❌ Humor/wit injection system
- ❌ Relationship milestone detection & celebration
- ❌ Foreman personality integration (archetypes exist but not wired)
- ❌ Voice system (stubs only)
- ❌ Real embedding model (using placeholder)
- ❌ Contract Net Protocol for task allocation
- ❌ ReAct loop for adaptive planning

**Killer App Insight:** The foreman should feel like a **smart, slightly bossy but encouraging partner** - someone players genuinely enjoy spending time with. All features must enhance this bond.

---

## Current State Assessment

### Production-Ready Components

| Component | Status | Quality | Notes |
|-----------|--------|--------|-------|
| **Async LLM System** | ✅ Built | HIGH | `TaskPlanner.planTasksAsync()` - non-blocking, batched |
| **Multi-Agent Orchestration** | ✅ Built | HIGH | `OrchestratorService` - foreman coordinates workers |
| **Memory Systems** | ✅ Built | HIGH | `CompanionMemory` - all memory types operational |
| **State Machine** | ✅ Built | HIGH | `AgentStateMachine` - proper state transitions |
| **Plugin Architecture** | ✅ Built | HIGH | `ActionRegistry` - dynamic action loading |
| **Proactive Dialogue** | ⚠️ Partial | MEDIUM | Framework exists, needs triggers + LLM integration |
| **Artificer Archetypes** | ✅ Built | HIGH | `ArtificerArchetype` enum - fully defined |

### Stub/Placeholder Components

| Component | Status | Blocker | Priority |
|-----------|--------|---------|----------|
| **Humor System** | ❌ Designed only | Implementation | HIGH |
| **Milestone Detection** | ❌ Designed only | Implementation | HIGH |
| **Foreman Personality** | ⚠️ Defined but not integrated | Wiring | HIGH |
| **Voice System** | ❌ Stubs | API integration | MEDIUM |
| **Real Embeddings** | ❌ Placeholder | Model selection | MEDIUM |
| **Contract Net Protocol** | ❌ Designed only | Implementation | LOW |
| **ReAct Loop** | ❌ Designed only | Implementation | LOW |

---

## Phase 1: IMMEDIATE - Unblock the Killer App (Week 1-2)

**Goal:** Make the foreman feel like a companion, not a tool.

### 1.1 Wire Artificer Archetype into Foreman Personality

**Impact:** HIGH | **Effort:** SMALL | **Dependency:** None

**Current State:** `ArtificerArchetype` enum is fully defined with 4 personalities (Lucius Fox, Getafix, Hephaestus, Phineas). `CompanionMemory.PersonalityProfile` exists but is not connected to archetypes.

**What to Build:**
```java
// In CompanionMemory.java
public void setArchetype(ArtificerArchetype archetype) {
    personality.openness = archetype.getTraits().getOpenness();
    personality.conscientiousness = archetype.getTraits().getConscientiousness();
    personality.extraversion = archetype.getTraits().getExtraversion();
    personality.agreeableness = archetype.getTraits().getAgreeableness();
    personality.neuroticism = archetype.getTraits().getNeuroticism();
    personality.humor = archetype.getHumor();
    personality.encouragement = archetype.getEncouragement();
    personality.formality = archetype.getFormality();
    personality.catchphrases = new ArrayList<>(archetype.getCatchphrases());
}

// In ForemanEntity.java (foreman only)
public void setForemanArchetype(ArtificerArchetype archetype) {
    companionMemory.setArchetype(archetype);
    sendChatMessage("Personality updated: " + archetype.getName());
}
```

**Files to Modify:**
- `CompanionMemory.java` - Add `setArchetype()` method
- `ForemanEntity.java` - Add archetype selection for foreman
- `CompanionPromptBuilder.java` - Use archetype in system prompt

**Success Criteria:**
- Foreman's dialogue changes based on archetype
- Catchphrases appear in responses
- Personality traits affect all communication

---

### 1.2 Add Humor Injection to Command Responses

**Impact:** HIGH | **Effort:** MEDIUM | **Dependency:** 1.1

**Current State:** Extensive humor research exists (`HUMOR_AND_WIT.md`). `PersonalityProfile.humor` field exists but is unused. No humor injection happens.

**What to Build:**

**Step 1: Create humor service**
```java
// New file: humor/HumorService.java
public class HumorService {
    private final CompanionMemory memory;
    private final MinecraftHumorLibrary library;

    public String maybeAddComment(String situation, String baseResponse) {
        if (!shouldTellJoke(situation)) {
            return baseResponse;
        }

        int humorChance = memory.getPersonality().humor / 5; // 65% = 13% chance
        if (new Random().nextInt(100) < humorChance) {
            String joke = library.getContextualJoke(situation);
            if (!joke.isEmpty()) {
                return baseResponse + " " + joke;
            }
        }
        return baseResponse;
    }

    private boolean shouldTellJoke(String situation) {
        // Never joke during combat
        if (isCombatSituation(situation)) return false;

        // Check rapport-based humor frequency
        int rapport = memory.getRapportLevel();
        if (rapport < 20 && new Random().nextInt(100) > 5) return false;
        if (rapport < 40 && new Random().nextInt(100) > 15) return false;

        return true;
    }
}
```

**Step 2: Create humor library**
```java
// New file: humor/MinecraftHumorLibrary.java
public class MinecraftHumorLibrary {
    private static final Map<String, List<String>> PUNS = Map.of(
        "build", List.of(
            "This construction is... riveting.",
            "Let's raise the roof.",
            "Another day, another block."
        ),
        "mine", List.of(
            "I've really dug myself into this one. Literally.",
            "Going deep. Very deep.",
            "Diamonds are forever. This excavation is not."
        ),
        "fail", List.of(
            "I'm not programmed to fail, but I'm very good at it.",
            "Gravity: 1, MineWright: 0.",
            "That went... according to plan. A bad plan, but a plan."
        )
    );

    public static String getPunForSituation(String situation) {
        // Extract keyword and return random pun
    }
}
```

**Step 3: Wire into response flow**
```java
// In ResponseParser.java or TaskPlanner.java
public ParsedResponse parse(String llmResponse, CompanionMemory memory) {
    ParsedResponse parsed = /* existing parsing */;

    // Add optional humor comment
    HumorService humor = new HumorService(memory);
    String enhancedPlan = humor.maybeAddComment(
        parsed.getPlan(),
        parsed.getPlan()
    );
    parsed.plan = enhancedPlan;

    return parsed;
}
```

**Files to Create:**
- `humor/HumorService.java`
- `humor/MinecraftHumorLibrary.java`

**Files to Modify:**
- `ResponseParser.java` - Add humor injection
- `TaskPlanner.java` - Wire humor service

**Success Criteria:**
- Jokes appear in 10-20% of responses (configurable)
- Jokes match the situation (building, mining, failing)
- No jokes during combat
- Joke frequency scales with rapport

---

### 1.3 Implement Milestone Detection & Celebration

**Impact:** HIGH | **Effort:** MEDIUM | **Dependency:** 1.1

**Current State:** `MilestoneTracker` class exists with basic structure but no triggers. Research document `RELATIONSHIP_MILESTONES.md` defines all milestones.

**What to Build:**

**Step 1: Create milestone triggers**
```java
// New file: milestone/triggers/FirstMeetingTrigger.java
public class FirstMeetingTrigger implements MilestoneTrigger {
    @Override
    public Optional<Milestone> check(ForemanEntity foreman, CompanionMemory memory) {
        if (memory.getFirstMeeting() == null) {
            return Optional.of(new Milestone(
                "first_meeting",
                "First Meeting",
                "The beginning of a beautiful friendship",
                Instant.now()
            ));
        }
        return Optional.empty();
    }
}

// New file: milestone/triggers/TaskMilestoneTrigger.java
public class TaskMilestoneTrigger implements MilestoneTrigger {
    private final Map<String, Integer> taskCounts = new ConcurrentHashMap<>();

    @Override
    public Optional<Milestone> check(MineWrightEntity minewright, CompanionMemory memory) {
        int completedTasks = memory.getInteractionCount();

        if (completedTasks == 10 && !hasMilestone("tasks_10")) {
            return Optional.of(new Milestone(
                "tasks_10",
                "Getting Started",
                "Completed 10 tasks together",
                Instant.now()
            ));
        }

        if (completedTasks == 100 && !hasMilestone("tasks_100")) {
            return Optional.of(new Milestone(
                "tasks_100",
                "Productive Partnership",
                "Completed 100 tasks together",
                Instant.now()
            ));
        }

        return Optional.empty();
    }
}
```

**Step 2: Check milestones on events**
```java
// In ForemanEntity.java, add to tick()
private void checkMilestones() {
    for (MilestoneTrigger trigger : milestoneTriggers) {
        Optional<Milestone> milestone = trigger.check(this, companionMemory);

        if (milestone.isPresent()) {
            companionMemory.getMilestoneTracker().achieve(milestone.get());
            celebrateMilestone(milestone.get());
        }
    }
}

private void celebrateMilestone(Milestone milestone) {
    String celebration = milestoneDialogue.generate(milestone, companionMemory);
    sendChatMessage(celebration);

    // Add confetti effect or special sound
}
```

**Files to Create:**
- `milestone/triggers/FirstMeetingTrigger.java`
- `milestone/triggers/TaskMilestoneTrigger.java`
- `milestone/triggers/TimeMilestoneTrigger.java` (1 day, 1 week, etc.)
- `milestone/dialogue/MilestoneDialogueGenerator.java`

**Files to Modify:**
- `MineWrightEntity.java` - Add milestone checking to tick()
- `CompanionMemory.java` - Wire milestone tracker
- `ActionExecutor.java` - Notify on task completion

**Success Criteria:**
- "Firsts" detected (first build, first mine, first death)
- Anniversaries celebrated (1 day, 1 week, 1 month)
- Task milestones (10, 50, 100 tasks)
- Celebration dialogue varies by personality

---

### 1.4 Enhance Proactive Dialogue with LLM Integration

**Impact:** MEDIUM | **Effort:** MEDIUM | **Dependency:** None

**Current State:** `ProactiveDialogueManager` exists with trigger detection framework but uses static fallback comments. LLM integration exists (`ConversationManager.generateProactiveComment()`) but needs wiring.

**What to Build:**

```java
// In ProactiveDialogueManager.java, enhance generateAndSpeakComment()
private void generateAndSpeakComment(String triggerType, String context) {
    // Build context for LLM
    Map<String, Object> promptContext = new HashMap<>();
    promptContext.put("triggerType", triggerType);
    promptContext.put("situation", context);
    promptContext.put("personality", memory.getPersonality().toPromptContext());
    promptContext.put("rapport", memory.getRapportLevel());

    // Generate with LLM
    CompletableFuture<String> commentFuture = conversationManager.generateProactiveComment(
        promptContext,
        llmClient
    );

    commentFuture.thenAccept(comment -> {
        String finalComment = comment;

        // Fallback to static if LLM fails
        if (finalComment == null || finalComment.trim().isEmpty()) {
            finalComment = getFallbackComment(triggerType);
        }

        if (finalComment != null && !finalComment.isEmpty()) {
            minewright.sendChatMessage(finalComment);
            LOGGER.debug("Proactive comment [{}]: {}", triggerType, finalComment);
        }
    });
}
```

**Files to Modify:**
- `ProactiveDialogueManager.java` - Wire LLM generation
- `ConversationManager.java` - Add context-aware prompt building

**Success Criteria:**
- Proactive comments use LLM generation
- Comments reference shared history
- Personality affects all comments
- Fallback to static if LLM unavailable

---

## Phase 2: SHORT-TERM - Make the Foreman Compelling (Week 3-4)

**Goal:** Add depth and polish to the companion experience.

### 2.1 Implement "Remember When" References

**Impact:** HIGH | **Effort:** MEDIUM | **Dependency:** Phase 1

**Current State:** `CompanionMemory.findRelevantMemories()` uses vector search but it's not wired into dialogue. No "remember when" references happen.

**What to Build:**

```java
// In ConversationManager.java
public String addRememberWhenReference(String currentSituation, CompanionMemory memory) {
    // Only reference memories if rapport > 40
    if (memory.getRapportLevel() < 40) {
        return "";
    }

    // Find relevant past experiences
    List<EpisodicMemory> relevantMemories = memory.findRelevantMemories(
        currentSituation,
        3  // top 3
    );

    if (relevantMemories.isEmpty()) {
        return "";
    }

    EpisodicMemory memoryToReference = relevantMemories.get(0);

    // Only reference if it's been a while (avoid repetition)
    long daysSince = ChronoUnit.DAYS.between(
        memoryToReference.timestamp,
        Instant.now()
    );

    if (daysSince < 1) {
        return ""; // Too recent
    }

    // Generate reference phrase
    List<String> templates = List.of(
        "Kind of reminds me of when " + memoryToReference.description.toLowerCase(),
        "Like that time we " + memoryToReference.description.toLowerCase(),
        "Similar to when " + memoryToReference.description.toLowerCase()
    );

    return templates.get(new Random().nextInt(templates.size()));
}
```

**Files to Modify:**
- `ConversationManager.java` - Add memory reference generation
- `CompanionPromptBuilder.java` - Include memories in prompts

**Success Criteria:**
- MineWright references past experiences
- References feel natural, not forced
- No repetition of recent memories

---

### 2.2 Add Dynamic Personality Adaptation

**Impact:** MEDIUM | **Effort:** MEDIUM | **Dependency:** 1.1

**Current State:** `PersonalityProfile` traits are static. No adaptation based on player interaction.

**What to Build:**

```java
// In CompanionMemory.java
public void adaptPersonalityBasedOnInteraction(String playerResponse) {
    PersonalityProfile personality = getPersonality();

    // If player uses humor, MineWright becomes more humorous
    if (containsHumor(playerResponse)) {
        personality.humor = Math.min(100, personality.humor + 1);
    }

    // If player is formal, MineWright becomes more formal
    if (isFormal(playerResponse)) {
        personality.formality = Math.min(100, personality.formality + 1);
    }

    // If player is casual, MineWright becomes less formal
    if (isCasual(playerResponse)) {
        personality.formality = Math.max(0, personality.formality - 1);
    }

    // Slow drift toward player's communication style
    personality.extraversion = slowlyAdapt(
        personality.extraversion,
        playerExtraversion(playerResponse),
        0.1  // 10% drift rate
    );
}

private int slowlyAdapt(int current, int target, double rate) {
    int diff = target - current;
    return current + (int) (diff * rate);
}
```

**Files to Modify:**
- `CompanionMemory.java` - Add personality adaptation methods
- `ConversationManager.java` - Call adaptation after each interaction

**Success Criteria:**
- Personality slowly drifts toward player's style
- Changes are subtle (not jarring)
- Drift rate is configurable

---

### 2.3 Implement Mood System

**Impact:** MEDIUM | **Effort:** SMALL | **Dependency:** 1.1

**Current State:** `PersonalityProfile.mood` field exists but is static. No mood changes.

**What to Build:**

```java
// In CompanionMemory.java
public enum Mood {
    CHEERFUL("Positive, energetic"),
    THOUGHTFUL("Contemplative"),
    FRUSTRATED("Slightly annoyed"),
    EXCITED("High energy"),
    PROUD("Satisfied"),
    CALM("Relaxed");
}

public Mood getCurrentMood() {
    // Calculate mood based on recent events
    int recentSuccesses = countRecentSuccesses();
    int recentFailures = countRecentFailures();
    int rapport = getRapportLevel();

    if (recentSuccesses > 3) {
        return Mood.EXCITED;
    }

    if (recentFailures > 3) {
        return Mood.FRUSTRATED;
    }

    if (rapport > 70) {
        return Mood.CHEERFUL;
    }

    if (rapport < 30) {
        return Mood.CALM;
    }

    return Mood.THOUGHTFUL;
}
```

**Files to Modify:**
- `CompanionMemory.java` - Add mood calculation
- `CompanionPromptBuilder.java` - Include mood in prompts

**Success Criteria:**
- Mood changes based on recent events
- Mood affects response style
- Mood transitions are smooth

---

### 2.4 Add Catchphrase Usage Tracking

**Impact:** LOW | **Effort:** SMALL | **Dependency:** 1.1

**Current State:** `PersonalityProfile.catchphrases` exists but no usage tracking. Repetition possible.

**What to Build:**

```java
// In CompanionMemory.java
private final Map<String, Integer> catchphraseUsage = new ConcurrentHashMap<>();

public String getRandomCatchphrase() {
    List<String> available = personality.catchphrases.stream()
        .filter(phrase -> catchphraseUsage.getOrDefault(phrase, 0) < 2)
        .collect(Collectors.toList());

    if (available.isEmpty()) {
        // Reset all usage if all used twice
        catchphraseUsage.clear();
        available = personality.catchphrases;
    }

    String chosen = available.get(new Random().nextInt(available.size()));
    catchphraseUsage.merge(chosen, 1, Integer::sum);

    return chosen;
}
```

**Files to Modify:**
- `CompanionMemory.java` - Add catchphrase tracking

**Success Criteria:**
- No catchphrase repeated within 3 uses
- All catchphrases used evenly

---

## Phase 3: MEDIUM-TERM - Add Depth & Polish (Week 5-7)

**Goal:** Enhance intelligence and add voice support.

### 3.1 Integrate Real Embedding Model

**Impact:** MEDIUM | **Effort:** MEDIUM | **Dependency:** None

**Current State:** `PlaceholderEmbeddingModel` returns zeros. Real vector search doesn't work.

**What to Build:**

**Option A: Use OpenAI Embeddings (easiest)**
```java
// New file: memory/embedding/OpenAIEmbeddingModel.java
public class OpenAIEmbeddingModel implements EmbeddingModel {
    private final String apiKey;
    private final HttpClient client;

    @Override
    public float[] embed(String text) {
        // Call OpenAI embeddings API
        // Return embedding vector
    }
}
```

**Option B: Use Local Model (free but slower)**
```java
// New file: memory/embedding/SentenceTransformerEmbedding.java
public class SentenceTransformerEmbedding implements EmbeddingModel {
    // Load small BERT model via ONNX
    // Run inference locally
}
```

**Files to Create:**
- `memory/embedding/OpenAIEmbeddingModel.java` (or local alternative)

**Files to Modify:**
- `CompanionMemory.java` - Replace placeholder with real model

**Success Criteria:**
- Semantic search finds conceptually similar memories
- Search latency < 100ms for 1000 memories
- Embedding API costs <$5/month

---

### 3.2 Add Voice Support (TTS Only)

**Impact:** MEDIUM | **Effort:** MEDIUM | **Dependency:** None

**Current State:** `VoiceManager` stub exists. No actual TTS integration.

**What to Build:**

```java
// In voice/tts/OpenAITTS.java
public class OpenAITTS implements TextToSpeech {
    private final String apiKey;
    private final String voice;  // "nova", "alloy", "echo", etc.

    @Override
    public void speak(String text, Vec3 position) {
        // Call OpenAI TTS API
        // Get audio bytes
        // Play at position using Minecraft sound system
    }
}
```

**Files to Modify:**
- `VoiceManager.java` - Wire real TTS implementation
- `build.gradle` - Add audio dependencies

**Success Criteria:**
- Foreman speaks responses with TTS
- Audio plays from MineWright's position (3D audio)
- Latency < 1 second

---

### 3.3 Implement Skill Library

**Impact:** MEDIUM | **Effort:** MEDIUM | **Dependency:** 3.1

**Current State:** No learning from past actions. Each action planned from scratch.

**What to Build:**

```java
// New file: skills/SkillLibrary.java
public class SkillLibrary {
    private final Map<String, Skill> skills = new ConcurrentHashMap<>();

    public void storeSuccessfulAction(BaseAction action, ActionResult result) {
        String skillCode = action.toExecutableCode();
        String description = action.toNaturalLanguageDescription();

        Skill skill = new Skill(
            description,
            skillCode,
            true,  // succeeded
            System.currentTimeMillis()
        );

        skills.put(description, skill);
    }

    public List<Skill> retrieveSkills(String query, int topK) {
        // Use embedding model to find similar past successes
        // Return top K most relevant skills
    }
}
```

**Files to Create:**
- `skills/SkillLibrary.java`
- `skills/Skill.java`

**Files to Modify:**
- `ActionExecutor.java` - Store successful actions
- `TaskPlanner.java` - Include skills in prompts

**Success Criteria:**
- Successful actions stored as skills
- Similar tasks reuse past code patterns
- LLM uses skill patterns in generated code

---

### 3.4 Implement Contract Net Protocol

**Impact:** LOW | **Effort:** LARGE | **Dependency:** None

**Current State:** Task assignment is round-robin. No capability matching.

**What to Build:** (Full implementation in research doc `MULTI_AGENT_ORCHESTRATION.md`)

**Note:** This is lower priority because current round-robin works well for small teams (2-4 workers). CNP becomes valuable at scale (10+ workers).

**Files to Create:**
- `contract/ContractNetProtocol.java`
- `contract/BidMessage.java`

**Files to Modify:**
- `OrchestratorService.java` - Replace round-robin with CNP

**Success Criteria:**
- Workers bid on tasks based on capability
- Best-suited worker wins each task
- Dynamic rebalancing on worker failure

---

## Phase 4: LONG-TERM - Advanced Features (Week 8+)

**Goal:** Add advanced intelligence and polish.

### 4.1 Implement ReAct Loop

**Impact:** MEDIUM | **Effort:** LARGE | **Dependency:** 3.3

**What to Build:** (Full spec in `IMPLEMENTATION_ROADMAP.md`)

**Why Lower Priority:** Current one-shot planning works for 80% of tasks. ReAct adds value for complex, multi-stage problems.

---

### 4.2 Add Multiplayer Support

**Impact:** HIGH | **Effort:** LARGE | **Dependency:** None

**What to Build:**
- Per-player relationship tracking
- Foreman coordinates for multiple players
- Shared world state across clients

---

### 4.3 Add Human Oversight System

**Impact:** MEDIUM | **Effort:** MEDIUM | **Dependency:** 4.2

**What to Build:**
- Approval workflow for dangerous actions
- GUI for reviewing planned actions
- Veto/override capabilities

---

## Dependency Graph

```
Phase 1 (Immediate - Killer App)
├── 1.1 Wire Archetype (S)
│   ├── Enables → 1.2 Humor System (M)
│   ├── Enables → 1.3 Milestones (M)
│   └── Enables → 1.4 Proactive Dialogue (M)
└── Unblocker for ALL personality features

Phase 2 (Short-term - Compelling)
├── 2.1 Remember When (M) [Requires 1.1, 1.4]
├── 2.2 Personality Adaptation (M) [Requires 1.1]
├── 2.3 Mood System (S) [Requires 1.1]
└── 2.4 Catchphrase Tracking (S) [Requires 1.1]

Phase 3 (Medium-term - Depth)
├── 3.1 Real Embeddings (M) [No deps]
├── 3.2 Voice TTS (M) [No deps]
├── 3.3 Skill Library (M) [Requires 3.1]
└── 3.4 Contract Net (L) [No deps, but lower value]

Phase 4 (Long-term - Advanced)
├── 4.1 ReAct Loop (L) [Requires 3.3]
├── 4.2 Multiplayer (L) [No deps]
└── 4.3 Human Oversight (M) [Requires 4.2]
```

**Legend:** S = Small, M = Medium, L = Large

---

## Implementation Order Summary

### Week 1
1. **Day 1-2:** Wire Artificer Archetype (1.1)
2. **Day 3-4:** Add Humor Injection (1.2)
3. **Day 5:** Add Catchphrase Tracking (2.4)

### Week 2
1. **Day 1-2:** Implement Milestone Detection (1.3)
2. **Day 3-4:** Enhance Proactive Dialogue (1.4)
3. **Day 5:** Add Mood System (2.3)

### Week 3-4
1. **Remember When** References (2.1)
2. **Personality Adaptation** (2.2)

### Week 5-6
1. **Real Embedding Model** (3.1)
2. **Skill Library** (3.3)

### Week 7
1. **Voice TTS** (3.2)

### Week 8+
1. **Contract Net Protocol** (3.4) - if scaling needed
2. **ReAct Loop** (4.1) - if complex tasks fail
3. **Multiplayer Support** (4.2) - if demand exists

---

## Success Metrics

### Phase 1 Success (Week 2)
- [ ] Foreman has distinct personality (archetype wired)
- [ ] Jokes appear in 10-20% of responses
- [ ] Milestones celebrated (firsts, anniversaries)
- [ ] Proactive comments happen naturally
- [ ] **Player feedback:** "The Foreman feels like a companion"

### Phase 2 Success (Week 4)
- [ ] The Foreman references shared experiences
- [ ] Personality slowly adapts to player
- [ ] Mood changes based on context
- [ ] No catchphrase repetition
- [ ] **Player feedback:** "The Foreman has a personality"

### Phase 3 Success (Week 7)
- [ ] Semantic search finds relevant memories
- [ ] The Foreman speaks with TTS
- [ ] Successful patterns reused
- [ ] **Player feedback:** "The Foreman is smart and helpful"

---

## Risk Mitigation

### Risk 1: Humor Annoyance
**Mitigation:**
- Start with low humor frequency (5-10%)
- Add config option to disable
- Track "joke failed" signals (player ignores, gives rapid commands)
- Auto-reduce humor if 3+ consecutive failures

### Risk 2: Performance Degradation
**Mitigation:**
- All LLM calls are already async (✅)
- Vector search is cached (✅)
- Profile before/after each feature
- Set TPS budget (must stay above 15)

### Risk 3: API Cost Overrun
**Mitigation:**
- Use Groq for fast/cheap responses where possible
- Aggressive caching (semantic cache, skill cache)
- Rate limiting and queuing
- Monitor costs daily during development

### Risk 4: Uncanny Valley
**Mitigation:**
- Time-gate relationship features (unlock gradually)
- Avoid surveillance-style tracking
- Keep tone friendly, not romantic
- Beta test with diverse players

---

## File Creation Summary

### Phase 1: 6 new files, 6 modified
```
NEW:
humor/HumorService.java
humor/MinecraftHumorLibrary.java
milestone/triggers/FirstMeetingTrigger.java
milestone/triggers/TaskMilestoneTrigger.java
milestone/triggers/TimeMilestoneTrigger.java
milestone/dialogue/MilestoneDialogueGenerator.java

MODIFY:
CompanionMemory.java (setArchetype, catchphrase tracking)
MineWrightEntity.java (archetype selection, milestone checking)
ResponseParser.java (humor injection)
TaskPlanner.java (humor wiring)
ProactiveDialogueManager.java (LLM integration)
ConversationManager.java (context building)
```

### Phase 2: 2 new files, 3 modified
```
NEW:
(Using existing ConversationManager.java enhancements)

MODIFY:
CompanionMemory.java (personality adaptation, mood)
ConversationManager.java (memory references)
CompanionPromptBuilder.java (mood in prompts)
```

### Phase 3: 4 new files, 4 modified
```
NEW:
memory/embedding/OpenAIEmbeddingModel.java
skills/SkillLibrary.java
skills/Skill.java
voice/tts/OpenAITTS.java

MODIFY:
CompanionMemory.java (replace placeholder)
ActionExecutor.java (store skills)
TaskPlanner.java (use skills)
VoiceManager.java (wire TTS)
build.gradle (audio deps)
```

---

## Testing Strategy

### Unit Tests (Priority)
```
humor/
├── HumorServiceTest.java (timing, appropriateness)
└── MinecraftHumorLibraryTest.java (pun categories)

milestone/
├── FirstMeetingTriggerTest.java
└── TaskMilestoneTriggerTest.java

personality/
└── PersonalityAdaptationTest.java (drift rates)
```

### Integration Tests
```
integration/
├── HumorIntegrationTest.java (full joke flow)
├── MilestoneIntegrationTest.java (detection + celebration)
└── PersonalityEvolutionTest.java (adaptation over time)
```

### Playtesting (Critical)
- **Week 2:** Internal playtest of Phase 1 features
- **Week 4:** Alpha playtest with 5 players
- **Week 6:** Beta playtest with 20 players
- **Week 7:** Feedback-driven polish

---

## Conclusion

This roadmap prioritizes **character development** over technical complexity. The foreman should feel like a partner - someone players genuinely enjoy spending time with.

**Key Differentiators:**
1. **Personality-first** - Archetype defines all behavior
2. **Progressive depth** - Simple at first, grows richer over time
3. **Natural dialogue** - Jokes, memories, celebrations
4. **Adaptive intelligence** - Learns from player, reuses patterns
5. **Voice interaction** - Natural conversation without typing

**Estimated Effort:** 7 weeks to "killer app" (Phase 1-2), 11 weeks to full polish (Phase 1-3)

**Recommended Team Size:** 1-2 developers (this is prioritized for solo/small team)

---

**Document Version:** 1.0
**Last Updated:** 2026-02-26
**Maintained By:** MineWright Development Team
**Status:** Ready for Implementation
