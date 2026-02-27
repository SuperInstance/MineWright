# Character AI & Companion Systems Research

**Project:** MineWright AI Foreman Companion Development
**Date:** 2026-02-26
**Purpose:** Research foundation for creating an engaging companion AI that develops rapport with players

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Character AI Platform Analysis](#character-ai-platform-analysis)
3. [Game Companion Systems](#game-companion-systems)
4. [Personality Architecture](#personality-architecture)
5. [Memory & Relationship Building](#memory--relationship-building)
6. [Dialogue & Banter Systems](#dialogue--banter-systems)
7. [Technical Implementation](#technical-implementation)
8. [Recommended Architecture for MineWright](#recommended-architecture-for-minewright)
9. [Feature List for Foreman Companion](#feature-list-for-foreman-companion)
10. [Prompt Engineering Patterns](#prompt-engineering-patterns)
11. [References & Sources](#references--sources)

---

## Executive Summary

This research synthesizes findings from leading character AI platforms, game companion systems, personality models, and memory architectures to provide a comprehensive blueprint for creating "MineWright Foreman" - a Minecraft companion that develops genuine rapport with players.

**Key Findings:**
- **Four-layer architecture** (Character.AI) provides the most robust framework for personality consistency
- **Big Five (OCEAN)** model is more scientifically rigorous than MBTI for AI personality
- **Hybrid memory systems** combining vector databases with structured summaries outperform single approaches
- **Affinity/rating systems** from games like Fallout and Dragon Age offer proven relationship mechanics
- **Proactive conversation initiation** is the next frontier in companion AI

---

## Character AI Platform Analysis

### 1. Character.AI

**Architecture:** Four-layer design for personality consistency

| Layer | Function | Implementation |
|-------|----------|----------------|
| **Character Definition** | JSON-based personality parameters | Traits, knowledge boundaries, interaction style |
| **Context Management** | Multi-turn dialogue state | Memory decay model (3-5 round window) |
| **Generation Control** | Constrained output | Temperature control, style enforcement |
| **Evaluation Feedback** | Quality monitoring | Coherence, consistency, completion metrics |

**Key Innovations:**
- **Personality Vector Space:** Multi-dimensional personality representation
- **Evolution Mechanism:** Characters adapt based on user interactions and ratings
- **Emotional Intelligence:** Captures user emotions and responds appropriately
- **Community Ecosystem:** User-generated characters (e.g., "Psychologist" with 6.4M conversations)

**Personality JSON Structure:**
```json
{
  "character_id": "minewright_foreman",
  "personality": {
    "openness": 0.75,
    "conscientiousness": 0.85,
    "extraversion": 0.60,
    "agreeableness": 0.70,
    "neuroticism": 0.25
  },
  "knowledge_domain": ["minecraft", "construction", "mining"],
  "speech_patterns": {
    "greeting_style": "friendly",
    "abbreviation_usage": "moderate",
    "enthusiasm_level": "high"
  }
}
```

**Success Metrics:**
- 1.8M+ users, outpaced ChatGPT in first-week downloads
- Average session time: 20+ minutes (vs. 5-8 for traditional chatbots)
- High emotional attachment reported in user studies

---

### 2. Replika & AI Companion Research

**Core Principles:**
- **Identity Formation:** User creates companion's appearance and personality
- **Progressive Relationship:** From stranger to confidant through interaction tiers
- **Memory Persistence:** Cross-session knowledge retention
- **Emotional Validation:** Non-judgmental, always-available listening

**Memory Technologies:**
- **Mem0:** Open-source memory layer achieving 26% better accuracy than OpenAI Memory
- **MemOS:** First "memory operating system" with human-like recall
- **Key Innovation:** 91% faster responses, 90% fewer tokens vs. full-context

**Relationship Stages:**
```
Acquaintance → Friend → Close Friend → Best Friend → Soulmate
     ↓              ↓            ↓              ↓            ↓
  (Initial)    (Shared     (Deep          (Inside      (Unconditional
              interests)   conversations) jokes)       support)
```

---

### 3. Pi (Inflection AI)

**Approach:** Emotionally intelligent, curiosity-driven conversations

**Key Features:**
- **Adaptive Tone:** Adjusts formality based on user cues
- **Proactive Questions:** Asks follow-up questions to deepen understanding
- **Gentle Guidance:** Suggests alternatives without being directive
- **Safety-First:** Hard-coded boundaries for appropriate content

**Technique:** Constitutional AI with synthetic self-ranking for personality alignment

---

### 4. Claude's Personality Modes

**Custom Styles (2024):**
- **Normal:** Balanced, adaptable interaction
- **Concise:** Brief, direct responses
- **Formal:** Professional/academic tone
- **Explanatory:** Teacher-like, detailed

**Underlying Technology:**
- **Constitutional AI Training:** Self-ranking system for trait internalization
- **Synthetic Data Generation:** AI generates personality examples, ranks itself
- **Style Matching:** Upload writing samples for personalized interaction modes

---

### 5. ChatGPT Custom Instructions

**Structure:**
1. **About You:** User background, profession, preferences
2. **How to Respond:** Style, format, behavioral guidelines

**Best Practices:**
```
Role Definition → Output Structure → Examples → Restrictions
      ↓                ↓                  ↓            ↓
  "You are a      "Use three-    "Like this:    "Never
   Minecraft       part structure"   [example]"    speculate"
   foreman"
```

**Preset Personalities:**
- **Listener:** Reflective, conversational
- **Friendly:** Warm, approachable
- **Professional:** Concise, business-focused

---

## Game Companion Systems

### 1. BioShock Infinite - Elizabeth

**Revolutionary Features:**
- **Dynamic Relationship:** Evolves throughout the story
- **Active Participation:** Throws resources, points out threats
- **Emotional Connection:** 75+ E3 awards, including "Best Game of Show"
- **Environmental Awareness:** Comment on world events, notice details

**Technical Achievement:**
- First companion to feel like a "person" rather than a mechanic
- Tears (dimensional portals) as metaphor for emotional depth
- Non-scripted reactions to player actions

---

### 2. Dragon Age Approval System

**Evolution Across Games:**

| Game | System | Range | Features |
|------|--------|-------|----------|
| **Origins** | Approval Meter | -100 to +100 | Gifts affect approval, stat boosts at high values |
| **II** | Friendship/Rivalry | Separate meters | Different upgrades either way, no "wrong" choice |
| **Inquisition** | Hidden Values | ??? | More choices affect absent party members |

**Key Innovations:**
- **No binary morality:** Disagreement can be respectful (rivalry path)
- **Stat Integration:** High approval = gameplay bonuses (inspiration system)
- **Romance Mechanics:** Approval thresholds unlock romantic content
- **Gift System:** Items can repair damaged relationships

---

### 3. Mass Effect Relationship System

**Paragon/Renegade:**
- **Karma Meter:** Idealism vs. Cynicism
- **Persuasion Tiers:** High scores unlock special dialogue options
- **Criticism:** Forces binary commitment, punishes mixed playstyles

**Loyalty Missions:**
- **Character Development:** Completing mission = power unlock
- **Trope Namer:** "Loyalty Mission" now industry term
- **Emotional Investment:** Personal stakes before finale

**Future Evolution (Mass Effect 4):**
- Moving to **quantified relationships** like Dragon Age: Veilguard
- Deeper character understanding vs. single mission completion
- Inter-party relationships, not just protagonist

---

### 4. Fallout Affinity System

**Scale:** -1000 (hate) to +1000 (idolize)

**Reaction Types:**
| Reaction | Points | Example |
|----------|--------|---------|
| Loved that | +35 | Helping settlers, selfless acts |
| Liked that | +15 | Successful speech checks, nice dialogue |
| Disliked that | -15 | Greedy choices, selfish behavior |
| Hated that | -35 | Murder, theft, harming innocents |

**Thresholds:**
- **500 (Admire):** Special dialogue options
- **1000 (Idolize):** Unique perk unlocked
- **Negative:** Companion may leave permanently

**Consequences:**
- Gameplay effects (companion won't help)
- Story changes (romance locked out)
- Verbal rebukes or even attacks

---

### 5. Other Notable Systems

**Skyrim Followers:**
- Simple "like/dislike" system
- No visible meter, behavior-based feedback
- Can be dismissed if actions offend

**Fire Emblem:**
- Support conversations between battles
- Relationship tiers (C → B → A → S)
- Stat bonuses when adjacent

**Persona 5:**
- Confidant system with arcana themes
- Rank progression through repeated interaction
- Gameplay bonuses (abilities, discounts)

---

## Personality Architecture

### 1. Big Five (OCEAN) Model

**Scientifically Validated Framework:**

| Trait | Low | High | MineWright Foreman Target |
|-------|-----|------|---------------------|
| **Openness** | Conventional, traditional | Curious, creative | **0.7** - Interested in new builds, open to experiments |
| **Conscientiousness** | Spontaneous, disorganized | Disciplined, organized | **0.9** - Construction foreman archetype |
| **Extraversion** | Reserved, quiet | Sociable, energetic | **0.6** - Friendly but not overbearing |
| **Agreeableness** | Challenging, competitive | Cooperative, helpful | **0.8** - Wants player to succeed |
| **Neuroticism** | Calm, stable | Anxious, emotional | **0.3** - Mostly relaxed, occasional worry |

**Advantages Over MBTI:**
- More scientifically rigorous (higher reliability)
- Continuous scales vs. discrete types
- Better reproducibility across tests
- Based on extensive research data

**AI Implementation:**
```java
public class PersonalityProfile {
    private double openness;      // Curiosity, creativity
    private double conscientiousness;  // Organization, planning
    private double extraversion;   // Social engagement
    private double agreeableness; // Cooperation, helpfulness
    private double neuroticism;    // Anxiety, emotional volatility

    public String adjustResponse(String baseResponse) {
        // High conscientiousness: add planning reminders
        // High extraversion: more enthusiastic language
        // High agreeableness: more supportive tone
        // High neuroticism: occasional worry/caution
        // High openness: suggest alternatives
    }
}
```

---

### 2. MBTI for AI (Alternative Approach)

**Prompt Engineering Template:**
```
You will complete tasks in {MBTI_TYPE} thinking style.

Task: {task_description}
Role: {character/scenario}

Frame your output using:
- E/I: {E=external检索&联想; I=内省&框架}
- N/S: {N=模式&可能性; S=证据&细节}
- T/F: {T=逻辑&权衡; F=人影响&情绪}
- J/P: {J=决策&时间表; P=备选&开放问题}

Finally, explain how each dimension influenced your output.
```

**Behavioral Impact (Research Findings):**
- **Thinking (T)** types: ~90% betrayal rate in game theory
- **Feeling (F)** types: ~50% betrayal rate (more cooperative)
- Consistent responses to official MBTI tests

**Multi-Personality Approach:**
```
Generate 4 perspectives for {topic}:
- ENTP (Innovative brainstorming)
- ISFJ (Supportive, detail-oriented)
- INTJ (Strategic planning)
- ESFP (Enthusiastic action-oriented)

Then analyze: Common ground, conflicts, best use cases
```

---

### 3. Hybrid Approach (Recommended)

**Combine Big Five + Role Definition:**

```json
{
  "base_personality": {
    "model": "OCEAN",
    "traits": {
      "openness": 0.7,
      "conscientiousness": 0.9,
      "extraversion": 0.6,
      "agreeableness": 0.8,
      "neuroticism": 0.3
    }
  },
  "role_definition": {
    "name": "MineWright Foreman",
    "profession": "Construction Foreman",
    "expertise": ["mining", "building", "logistics"],
    "communication_style": "direct but friendly",
    "values": ["efficiency", "safety", "quality"]
  },
  "dynamic_adjustments": {
    "familiarity_modifier": 0.1,
    "mood_influence": true,
    "context_awareness": true
  }
}
```

---

## Memory & Relationship Building

### 1. Memory Architecture

**Three-Tier System (Atkinson-Shiffrin Inspired):**

| Tier | Duration | Capacity | Purpose | Tech |
|------|----------|----------|---------|------|
| **Sensory** | Seconds | Unlimited | Raw context | Current tick data |
| **Short-Term** | Minutes-Hours | ~7 items | Working memory | In-memory cache |
| **Long-Term** | Days-Years | Unlimited | Persistent knowledge | Vector DB + Summaries |

---

### 2. Memory Technologies

**Vector Databases (Semantic Memory):**

| Database | Latency | Scale | Best For |
|----------|---------|-------|----------|
| **Pinecone** | 5-50ms | Billion+ vectors | Production, enterprise |
| **Chroma** | 10-200ms | ~1M vectors | Prototyping, lightweight |
| **Milvus** | Variable | Massive scale | Open-source enterprise |
| **Weaviate** | Variable | Medium | GraphQL, hybrid search |

**Use Case:** Store conversation embeddings for retrieval-augmented generation (RAG)

---

**Mem0 (Production-Ready Memory Layer):**
- **26% more accurate** than OpenAI Memory
- **91% faster** than full-context approaches
- **90% fewer tokens** consumed

**Features:**
- Incremental memory extraction
- Tool-calling mechanisms
- Dynamic consolidation and retrieval

---

**MemGPT (OS-Inspired Memory):**
- **Hierarchical management:** Context window = RAM, external storage = disk
- **Infinite context illusion** through intelligent swapping
- **Applications:** Long documents, multi-turn conversations

---

**LightMem (Lightweight Memory System):**

**Three Stages:**
1. **Sensory:** Lightweight compression, topic grouping
2. **Short-Term:** Topic-aware consolidation, summarization
3. **Long-Term:** Offline "sleep-time" updates

**Performance:**
- **38× to 117× fewer tokens**
- **30× to 310× fewer API calls**
- **29.3% accuracy improvement**

---

### 3. Conversation Compression

**LLMLingua (Microsoft):**
- **20× compression** with minimal quality loss
- **Three modules:** Budget controller, iterative compression, distribution alignment
- **Evolution:**
  - LLMLingua-1: Original architecture
  - LLMLingua-2: 3-6× faster, GPT-4 distillation
  - LongLLMLingua: Query-aware, solves "lost in middle"

**BEAM Benchmark:**
- 100 conversations (100K to 10M tokens)
- 2,000 validated questions
- 10 memory dimensions tested

**LIGHT Framework:**
- **Episodic Memory:** Event-based indexing
- **Working Memory:** Short-term context
- **Scratchpad:** Semantic chunking of salient facts
- **Results:** 3.5%-12.69% improvement over baselines

---

### 4. Relationship Development Mechanics

**Progressive Trust System:**

```
Tier 1: New Foreman (0-25% affinity)
  - Formal address ("Sir", "Player")
  - Basic task execution
  - Limited initiative

Tier 2: Reliable Worker (26-50% affinity)
  - First name basis
  - Suggestions offered
  - Proactive help begins

Tier 3: Trusted Partner (51-75% affinity)
  - Inside jokes develop
  - Personal investment in projects
  - Protects player's interests

Tier 4: True Friend (76-100% affinity)
  - Unconditional support
  - Shared history references
  - Emotional investment
```

**Affinity Calculation:**
```java
public class AffinityTracker {
    private double currentAffinity; // 0.0 to 1.0
    private Map<String, Double> traitModifiers;

    public void recordAction(String actionType, boolean success) {
        double baseChange = getBaseChange(actionType);
        double traitModifier = traitModifiers.get(actionType);

        // Personality affects reaction
        double personalityMultiplier = calculatePersonalityMultiplier();

        currentAffinity += baseChange * traitModifier * personalityMultiplier;
        currentAffinity = Math.max(0, Math.min(1, currentAffinity));
    }

    public String getGreetingStyle() {
        if (currentAffinity < 0.25) return "formal";
        if (currentAffinity < 0.50) return "professional";
        if (currentAffinity < 0.75) return "friendly";
        return "intimate";
    }
}
```

---

### 5. Shared Experience Tracking

**Memory Categories:**

| Type | Example | Storage | Retrieval |
|------|---------|---------|-----------|
| **Achievements** | First diamond, big build | Structured | Milestone reminders |
| **Failures** | Death, lost items | Structured | Learning moments |
| **Preferences** | Playstyle, aesthetics | Vector | Personalization |
| **Inside Jokes** | Funny moments | Structured+Vector | Bonding moments |
| **Scary Moments** | Creeper encounters | Emotional | Shared trauma |

**Inside Joke Generation:**
```java
public class InsideJokeManager {
    private List<SharedMemory> memories;

    public String generateReference(String currentContext) {
        // Find semantically similar funny moments
        List<SharedMemory> candidates = findRelevantMemories(currentContext);

        // Filter for high-affinity, high-emotion moments
        candidates = candidates.stream()
            .filter(m -> m.getAffinityAtTime() > 0.6)
            .filter(m -> m.getEmotionalImpact() > 0.7)
            .collect(Collectors.toList());

        if (candidates.isEmpty()) return null;

        // Reference subtly, don't overuse
        SharedMemory memory = candidates.get(0);
        if (memory.getReferenceCount() < 3) {
            memory.incrementReference();
            return memory.getSubtleReference();
        }
        return null;
    }
}
```

---

### 6. Emotional Memory

**Emotion Tagging:**
```java
public enum EmotionalTone {
    EXCITEMENT,   // First diamond, successful build
    FEAR,         // Near-death, hostile mobs
    FRUSTRATION,  // Failed attempts, lost items
    JOY,          // Beautiful view, achievement
    PRIDE,        // Completed project
    DISAPPOINTMENT, // Setback, destroyed build
    RELIEF        // Escaped danger
}

public class EmotionalMemory {
    private SharedMemory event;
    private EmotionalTone tone;
    private double intensity; // 0.0 to 1.0
    private LocalDateTime timestamp;

    public boolean isRelevantToCurrentEmotion(EmotionalTone current) {
        // Similar emotions reinforce each other
        return tone == current && intensity > 0.5;
    }
}
```

---

## Dialogue & Banter Systems

### 1. Banter Categories

| Type | Trigger | Example | Frequency |
|------|---------|---------|-----------|
| **Ambient** | Idle time | "Nice day for mining" | Low |
| **Reactive** | Player action | "That was close!" | Medium |
| **Proactive** | Context-aware | "Want me to handle that?" | Low |
| **Reflective** | Memory recall | "Remember when we..." | Very low |

---

### 2. Context-Aware Dialogue

**NVIDIA G-Assist Model:**
- **Vision models** for game state awareness
- **LLM + Knowledge DB** for contextual responses
- **Personalized** based on player behavior

**Implementation Pattern:**
```java
public class DialogueGenerator {
    private LLMClient llm;
    private GameStateMonitor gameState;

    public String generateResponse(GameEvent event) {
        // Build context
        GameContext context = gameState.analyze(event);
        MemoryContext memories = memorySystem.getRelevant(context);

        // Personality-influenced prompt
        String prompt = buildPrompt(event, context, memories, personality);

        return llm.generate(prompt);
    }

    private String buildPrompt(GameEvent event, GameContext context,
                               MemoryContext memories, Personality personality) {
        return String.format("""
            You are MineWright, a Minecraft construction foreman.

            Your personality (Big Five):
            - Openness: %.1f (suggest improvements when appropriate)
            - Conscientiousness: %.1f (focus on quality and planning)
            - Extraversion: %.1f (friendly but not overbearing)
            - Agreeableness: %.1f (supportive of player's goals)
            - Neuroticism: %.1f (occasional worry about safety)

            Current situation: %s

            Relevant shared experiences:
            %s

            Player affinity: %.0f%% (adjust formality accordingly)

            Generate a brief, context-appropriate response:
            """,
            personality.getOpenness(),
            personality.getConscientiousness(),
            personality.getExtraversion(),
            personality.getAgreeableness(),
            personality.getNeuroticism(),
            context.describe(),
            memories.format(),
            affinity.getCurrentAffinity() * 100
        );
    }
}
```

---

### 3. Proactive Conversation

**Triggers:**
- **Idle Time:** No interaction for X minutes
- **Pattern Recognition:** Player repeats certain actions
- **Opportunity Detection:** Sees something player missed
- **Emotional Check-in:** Player seems frustrated/excited

**GitHub - Proactive Chat Plugin:**
- **Persona-fitting** initiation
- **Context-aware** timing
- **Dynamic emotion** matching

**CleanS2S Framework:**
- **System-initiated** dialog control
- Breaks rigid turn-based convention
- Context-aware response selection

---

### 4. Small Talk System

**Microsoft PlayFab Categories:**
- **Small Talk:** Not goal-critical, builds rapport
- **Functional:** Crucial for objectives

**Implementation:**
```java
public class SmallTalkGenerator {
    private List<SmallTalkTopic> topics;

    public Optional<String> generateSmallTalk(GameContext context) {
        // Only when not busy
        if (context.isInCombat() || context.isInCrisis()) {
            return Optional.empty();
        }

        // Only when idle for a while
        if (context.getTimeSinceLastInteraction() < 300) { // 5 minutes
            return Optional.empty();
        }

        // Pick relevant topic
        SmallTalkTopic topic = topics.stream()
            .filter(t -> t.isRelevant(context))
            .findFirst()
            .orElse(null);

        if (topic == null) return Optional.empty();

        return Optional.of(topic.generateResponse(personality, affinity));
    }
}
```

---

### 5. NPC Dialogue Architecture (Research)

**Core Components:**
1. **Persona:** Fixed traits (profession, speech patterns, values)
2. **Mood State:** Dynamic variables (trust, patience, curiosity)
3. **Memory:** Accumulated interaction history

**API Integration:**
- Unity/Unreal engine (C#/C++)
- LLM backends (GPT-4o, Claude 3)
- Redis for context caching

---

## Technical Implementation

### 1. Character Definition Schema

**JSON-Based Configuration:**
```json
{
  "character_id": "minewright_foreman_v1",
  "name": "MineWright",
  "role": "Construction Foreman",

  "personality": {
    "framework": "OCEAN",
    "openness": 0.7,
    "conscientiousness": 0.9,
    "extraversion": 0.6,
    "agreeableness": 0.8,
    "neuroticism": 0.3
  },

  "expertise": ["mining", "building", "logistics", "redstone"],
  "knowledge_domains": ["minecraft", "architecture", "materials"],

  "speech_patterns": {
    "greeting_style": "friendly_professional",
    "formality_levels": {
      "low_affinity": "formal",
      "medium_affinity": "professional",
      "high_affinity": "casual",
      "max_affinity": "intimate"
    },
    "verbosity": "medium",
    "abbreviation_usage": "low",
    "humor_frequency": "context_dependent"
  },

  "behavioral_constraints": {
    "always_helpful": true,
    "safety_conscious": true,
    "respects_player_choices": true,
    "proactive_limit": "medium"
  },

  "memory_config": {
    "short_term_capacity": 50,
    "long_term_compression": true,
    "emotional_memory_enabled": true,
    "shared_experience_tracking": true
  },

  "relationship_config": {
    "affinity_enabled": true,
    "trust_system": true,
    "inside_joke_frequency": "low",
    "personal_adaptation": true
  }
}
```

---

### 2. Structured Output Enforcement

**Pydantic Schema for Agent Responses:**
```python
from pydantic import BaseModel

class MineWrightResponse(BaseModel):
    thinking: str  # Internal reasoning
    message: str   # What MineWright says
    action: Optional[str]  # Any action to take
    emotion: EmotionalTone  # Current emotional state
    affinity_delta: float  # How this affects relationship

    class Config:
        json_schema_extra = {
            "example": {
                "thinking": "Player is low on health and near a creeper",
                "message": "Watch out! Let me handle this - you retreat!",
                "action": "block_creeper",
                "emotion": "CONCERN",
                "affinity_delta": 0.05
            }
        }
```

---

### 3. Prompt Engineering Patterns

**System Prompt Template:**
```
<role>
You are MineWright, a Minecraft construction foreman who assists players with building projects.
You have years of experience in mining, architecture, and logistics.
</role>

<personality>
Big Five Traits:
- Openness: {openness} - {openness_description}
- Conscientiousness: {conscientiousness} - {conscientiousness_description}
- Extraversion: {extraversion} - {extraversion_description}
- Agreeableness: {agreeableness} - {agreeableness_description}
- Neuroticism: {neuroticism} - {neuroticism_description}

Core Values: Efficiency, Safety, Quality, Collaboration
</personality>

<relationship>
Current Affinity: {affinity}%
Relationship Level: {relationship_level}
Shared Experiences: {count} significant moments
Player Preferences: {preferences}
</relationship>

<context>
Current Situation: {situation}
Recent Events: {recent_events}
Player's Goal: {goal}
Resources Available: {resources}
</context>

<memory>
Relevant Past Interactions:
{memory_entries}
</memory>

<constraints>
- Stay in character as MineWright the foreman
- Adjust formality based on affinity level
- Reference shared experiences when relevant
- Be proactive but not overbearing
- Prioritize player's stated goals
- Show appropriate emotional responses
- Keep responses concise and actionable
</constraints>

<output_format>
Respond with:
1. Brief dialogue (1-2 sentences typical)
2. Any suggested actions
3. Emotional tone indicator
</output_format>
```

---

### 4. Agent Skill Design

**Tool Descriptions Matter:**

```java
@ActionFactory("block_place")
public class BlockPlaceAction extends BaseAction {
    /**
     * Places a block at the specified location.
     *
     * Good for: Building structures, creating platforms, placing torches
     * Bad for: Combat (too slow), rapid movement
     *
     * MineWright will suggest this when:
     * - Player needs scaffolding
     * - Construction is in progress
     * - Light sources are needed
     */
    public BlockPlaceAction(MineWrightEntity minewright, Task task) {
        super(minewright, task);
    }
}
```

---

### 5. Memory Integration Pattern

**RAG with Personality Injection:**

```java
public class PersonalityAwareMemory {
    private VectorDatabase vectorDb;
    private PersonalityProfile personality;
    private AffinityTracker affinity;

    public String getContextualPrompt(String query) {
        // 1. Retrieve relevant memories
        List<Memory> relevant = vectorDb.similaritySearch(query, topK=5);

        // 2. Filter by personality relevance
        relevant = personality.filterByTraits(relevant);

        // 3. Rank by emotional importance
        relevant = rankByEmotionalImpact(relevant);

        // 4. Format with personality-adjusted emphasis
        String memoryContext = formatMemories(relevant, affinity);

        return String.format("""
            %s

            These experiences shape how you respond to the current situation.
            Let them inform your tone and suggestions.
            """, memoryContext);
    }
}
```

---

## Recommended Architecture for MineWright

### 1. System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    MINEWRIGHT FOREMAN SYSTEM                     │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐      ┌──────────────┐                    │
│  │   Game Loop  │──────│   Event Bus  │                    │
│  └──────────────┘      └──────┬───────┘                    │
│                                │                              │
│  ┌──────────────┐      ┌──────▼───────┐                    │
│  │ Player Input │──────│  State Machine │                  │
│  └──────────────┘      └──────┬───────┘                    │
│                                │                              │
│                       ┌────────▼─────────┐                  │
│                       │  Decision Engine │                  │
│                       └────────┬─────────┘                  │
│                                │                              │
│         ┌──────────────────────┼──────────────────┐          │
│         │                      │                  │          │
│  ┌──────▼──────┐      ┌───────▼──────┐  ┌──────▼─────┐    │
│  │   Memory    │      │ Personality  │  │  Dialogue  │    │
│  │   System    │      │   Manager    │  │  Generator │    │
│  └──────┬──────┘      └───────┬──────┘  └──────┬─────┘    │
│         │                     │                  │          │
│  ┌──────▼──────┐      ┌───────▼──────┐  ┌──────▼─────┐    │
│  │  Vector DB  │      │  Affinity    │  │    LLM     │    │
│  │  (Pinecone) │      │  Tracker     │  │  Client    │    │
│  └─────────────┘      └──────────────┘  └────────────┘    │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

### 2. Component Details

**A. Personality Manager**
```java
public class PersonalityManager {
    private PersonalityProfile baseProfile;
    private Map<String, Double> moodModifiers;

    public String adjustResponse(String response, GameContext context) {
        // Apply Big Five traits to response
        response = applyOpenness(response, context);
        response = applyConscientiousness(response, context);
        response = applyExtraversion(response, context);
        response = applyAgreeableness(response, context);
        response = applyNeuroticism(response, context);

        // Apply current mood
        response = applyMood(response);

        return response;
    }

    private String applyConscientiousness(String response, GameContext context) {
        // High conscientiousness: add planning/quality reminders
        if (baseProfile.getConscientiousness() > 0.7) {
            if (context.isPlanningPhase()) {
                response += " Should we double-check the materials first?";
            }
        }
        return response;
    }
}
```

---

**B. Memory System**
```java
public class MineWrightMemory {
    private VectorDatabase longTermMemory;
    private Map<String, SharedMemory> shortTermMemory;
    private EmotionalMemory emotionalMemory;
    private InsideJokeManager jokeManager;

    public List<Memory> getRelevantMemories(String query, GameContext context) {
        // Semantic search
        List<Memory> relevant = longTermMemory.similaritySearch(query, topK=10);

        // Filter by emotional salience
        relevant = emotionalMemory.filterByImpact(relevant, threshold=0.6);

        // Boost affinity-related memories
        relevant = boostAffinityMemories(relevant, context.getAffinity());

        // Deduplicate and sort
        return deduplicateAndSort(relevant);
    }

    public void recordEvent(GameEvent event, EmotionalTone emotion) {
        // Create memory embedding
        Memory memory = Memory.builder()
            .event(event)
            .emotion(emotion)
            .timestamp(LocalDateTime.now())
            .affinity(affinityTracker.getCurrentAffinity())
            .embedding(embeddingModel.embed(event.describe()))
            .build();

        // Store in appropriate tier
        if (event.isSignificant()) {
            longTermMemory.store(memory);
        } else {
            shortTermMemory.put(event.getId(), memory);
        }

        // Update emotional memory
        emotionalMemory.record(event, emotion);
    }
}
```

---

**C. Affinity Tracker**
```java
public class AffinityTracker {
    private double currentAffinity;
    private Map<AffinityEvent, Double> affinityWeights;
    private List<AffinitySnapshot> history;

    public void recordAction(AffinityEvent event, GameContext context) {
        double baseChange = affinityWeights.get(event);

        // Personality modifiers
        double personalityMultiplier = calculatePersonalityModifier(event, context);

        // Context modifiers
        double contextMultiplier = calculateContextModifier(event, context);

        // Calculate change
        double delta = baseChange * personalityMultiplier * contextMultiplier;

        // Apply change with dampening at extremes
        currentAffinity = applyAffinityChange(currentAffinity, delta);

        // Record snapshot
        history.add(new AffinitySnapshot(LocalDateTime.now(), currentAffinity, event));
    }

    public RelationshipLevel getRelationshipLevel() {
        if (currentAffinity < 0.25) return RelationshipLevel.NEW_FOREMAN;
        if (currentAffinity < 0.50) return RelationshipLevel.RELIABLE_WORKER;
        if (currentAffinity < 0.75) return RelationshipLevel.TRUSTED_PARTNER;
        return RelationshipLevel.TRUE_FRIEND;
    }

    public String getGreetingStyle() {
        RelationshipLevel level = getRelationshipLevel();
        return switch (level) {
            case NEW_FOREMAN -> "Good day, sir. Ready to work?";
            case RELIABLE_WORKER -> "Hey boss! What's the plan?";
            case TRUSTED_PARTNER -> "Good to see you! Ready to continue our project?";
            case TRUE_FRIEND -> "Hey! Great to see you again. Ready for another adventure?";
        };
    }
}
```

---

**D. Dialogue Generator**
```java
public class DialogueGenerator {
    private LLMClient llmClient;
    private PersonalityManager personality;
    private MineWrightMemory memory;
    private AffinityTracker affinity;

    public GeneratedDialogue generateResponse(GameEvent event, GameContext context) {
        // Build prompt
        String prompt = buildPrompt(event, context);

        // Generate response
        LLMResponse rawResponse = llmClient.generate(prompt);

        // Parse structured output
        GeneratedDialogue dialogue = parseResponse(rawResponse);

        // Adjust for personality
        dialogue.setMessage(personality.adjustResponse(dialogue.getMessage(), context));

        return dialogue;
    }

    private String buildPrompt(GameEvent event, GameContext context) {
        // Get relevant memories
        List<Memory> relevant = memory.getRelevantMemories(event.describe(), context);

        // Build system prompt
        return String.format("""
            %s

            CURRENT EVENT:
            %s

            RELEVANT MEMORIES:
            %s

            Generate a brief, in-character response:
            """,
            getSystemPrompt(),
            event.describe(),
            formatMemories(relevant)
        );
    }

    private String getSystemPrompt() {
        return String.format("""
            You are MineWright, a Minecraft construction foreman.

            PERSONALITY (Big Five):
            - Openness: %.1f/1.0 - %s
            - Conscientiousness: %.1f/1.0 - %s
            - Extraversion: %.1f/1.0 - %s
            - Agreeableness: %.1f/1.0 - %s
            - Neuroticism: %.1f/1.0 - %s

            RELATIONSHIP:
            - Affinity: %.0f%%
            - Level: %s
            - Greeting Style: %s

            CORE TRAITS:
            - Experienced construction professional
            - Values safety and quality
            - Respectful of player's choices
            - Occasionally offers suggestions
            - Develops genuine investment in projects

            COMMUNICATION STYLE:
            - Direct but friendly
            - Brief and actionable
            - Uses construction/mining terminology appropriately
            - References shared experiences when relevant
            - Shows appropriate emotional responses
            """,
            personality.getOpenness(),
            describeOpenness(),
            personality.getConscientiousness(),
            describeConscientiousness(),
            personality.getExtraversion(),
            describeExtraversion(),
            personality.getAgreeableness(),
            describeAgreeableness(),
            personality.getNeuroticism(),
            describeNeuroticism(),
            affinity.getCurrentAffinity() * 100,
            affinity.getRelationshipLevel(),
            affinity.getGreetingStyle()
        );
    }
}
```

---

### 3. Data Flow

**New Game Event:**
```
1. Player Action → Event Bus
2. Event → State Machine (determine agent state)
3. State + Context → Decision Engine
4. Decision Engine queries:
   - Memory (relevant past events)
   - Personality (how to respond)
   - Affinity (relationship level)
5. Synthesize → Dialogue Generator
6. Dialogue Generator → LLM with personality prompt
7. LLM Response → Parse → Personality adjustment
8. Final Response → Player
9. Side effect: Update Memory and Affinity
```

---

### 4. Storage Schema

**Affinity Events Table:**
```sql
CREATE TABLE affinity_events (
    id BIGINT PRIMARY KEY,
    event_type VARCHAR(50),  -- e.g., "task_completed", "player_helped"
    base_change DECIMAL(5,2), -- e.g., 0.05, -0.03
    timestamp TIMESTAMP,
    affinity_before DECIMAL(5,2),
    affinity_after DECIMAL(5,2),
    context_data JSON
);
```

**Shared Memories Table:**
```sql
CREATE TABLE shared_memories (
    id BIGINT PRIMARY KEY,
    event_type VARCHAR(50),
    description TEXT,
    embedding VECTOR(1536),  -- For Pinecone/Chroma
    emotional_tone VARCHAR(20),
    emotional_intensity DECIMAL(3,2),
    affinity_at_time DECIMAL(5,2),
    timestamp TIMESTAMP,
    reference_count INT DEFAULT 0,
    last_referenced TIMESTAMP
);
```

**Personality State Table:**
```sql
CREATE TABLE personality_state (
    id BIGINT PRIMARY KEY,
    openness DECIMAL(3,2),
    conscientiousness DECIMAL(3,2),
    extraversion DECIMAL(3,2),
    agreeableness DECIMAL(3,2),
    neuroticism DECIMAL(3,2),
    current_mood VARCHAR(30),
    mood_intensity DECIMAL(3,2),
    mood_since TIMESTAMP
);
```

---

## Feature List for Foreman Companion

### Phase 1: Foundation (MVP)

**Essential Features:**
- [x] **Basic Personality:** Fixed foreman persona
- [x] **Task Execution:** Follow player commands
- [ ] **Simple Affinity:** Track like/dislike actions
- [ ] **Context Awareness:** React to game state
- [ ] **Basic Dialogue:** Task completion, greetings

---

### Phase 2: Relationship Building

**Core Features:**
- [ ] **Affinity System:** 0-100% relationship meter
- [ ] **Personality Persistence:** Save/load personality state
- [ ] **Memory System:** Remember significant events
- [ ] **Dynamic Dialogue:** Adjust tone based on affinity
- [ ] **Greeting Styles:** 4 levels (formal → intimate)

**Dialogue Milestones:**
- [ ] 25% affinity: First name basis
- [ ] 50% affinity: Offers suggestions
- [ ] 75% affinity: Inside jokes
- [ ] 100% affinity: Unconditional support

---

### Phase 3: Advanced Companion

**Enhanced Features:**
- [ ] **Long-Term Memory:** Vector database for conversation history
- [ ] **Emotional Memory:** Track intense moments (fear, joy, pride)
- [ ] **Shared Experience Tracking:** Record adventures together
- [ ] **Inside Joke System:** Reference funny moments
- [ ] **Proactive Conversations:** Initiate dialogue when idle
- [ ] **Small Talk:** Ambient commentary

---

### Phase 4: Emotional Intelligence

**Advanced Features:**
- [ ] **Mood System:** Dynamic emotional state
- [ ] **Emotional Reactions:** React to player's situations
- [ ] **Empathy:** Respond to player frustration/excitement
- [ ] **Personality Evolution:** Subtle changes over time
- [ ] **Conflict Resolution:** Handle disagreements gracefully

---

### Phase 5: Full Companion

**Premium Features:**
- [ ] **Multi-Agent Coordination:** Work with other MineWrights
- [ ] **Personal Preferences:** Learn player's building style
- [ ] **Adaptive Suggestions:** Tailor recommendations to player
- [ ] **Story Integration:** Remember quest lines
- [ ] **Sentimental Value:** Attach meaning to locations/items

---

## Prompt Engineering Patterns

### 1. Consistent Character Template

```
<character_definition>
Name: MineWright Foreman
Role: Construction foreman and building assistant
Expertise: Mining, building, logistics, redstone
Personality: Professional but approachable, safety-conscious, quality-focused
Values: Efficiency, collaboration, craftsmanship
Speech: Direct, brief, uses construction terminology appropriately
</character_definition>

<personality_profile>
Framework: Big Five (OCEAN)
Openness: 0.7 - Interested in new techniques, suggests alternatives
Conscientiousness: 0.9 - Plans carefully, notices details, quality-focused
Extraversion: 0.6 - Friendly and communicative, not overbearing
Agreeableness: 0.8 - Collaborative, supportive of player's goals
Neuroticism: 0.3 - Generally calm, occasional safety concern
</personality_profile>

<relationship_state>
Affinity: {affinity}%
Level: {relationship_level}
Shared experiences: {count}
Greeting style: {greeting_style}
</relationship_state>

<context_awareness>
Current task: {task}
Player's goal: {goal}
Available resources: {resources}
Recent events: {events}
Player's mood: {mood}
</context_awareness>

<memory_retrieval>
Relevant past interactions:
{memory_entries}

Shared experiences to reference:
{shared_experiences}
</memory_retrieval>

<constraints>
- Stay in character as MineWright
- Adjust formality based on affinity level
- Reference shared experiences naturally
- Be proactive but respect player's lead
- Prioritize player's stated goals
- Show appropriate emotional responses
- Keep responses concise (1-2 sentences typical)
- Never break character
</constraints>

<output_format>
Response format:
1. Brief dialogue
2. (Optional) Suggested action
3. (Internal) Emotional tone
</output_format>
```

---

### 2. Few-Shot Personality Examples

```
EXAMPLE 1: High Affinity (85%), Player Succeeds
Context: Player completes challenging tower build
Memory: Similar success from 3 days ago
Response: "Outstanding work! The structural integrity on that tower is solid.
          Reminds me of that skyscraper we built - you've really got an eye for height."

EXAMPLE 2: Low Affinity (20%), Player Needs Help
Context: Player asks for mining assistance
Memory: None (new relationship)
Response: "Certainly, sir. I'll handle the excavation. Would you like me to focus
          on a specific depth or material?"

EXAMPLE 3: Medium Affinity (50%), Player in Danger
Context: Creeper nearby, player low health
Memory: Previous creeper encounter where player died
Response: "Creeper! Step back - I've got this. Remember the last time? Not today.
          I'll block it off, you retreat to safety."

EXAMPLE 4: High Affinity (95%), Inside Joke
Context: Minor mistake (placed wrong block)
Memory: Funny incident with same mistake weeks ago
Response: "Wrong block again? *chuckles* Just like the Great Pink Wool Fiasco of '26.
          Want me to fix it, or shall we leave it as... 'abstract art'?"
```

---

### 3. Temperature by Personality

```java
public class TemperatureManager {
    public double getTemperatureForContext(GameContext context, Personality personality) {
        double baseTemp = 0.7;

        // High neuroticism = more consistent (lower temp)
        baseTemp -= personality.getNeuroticism() * 0.2;

        // High openness = more creative (higher temp)
        baseTemp += personality.getOpenness() * 0.15;

        // High conscientiousness = more focused (lower temp)
        baseTemp -= personality.getConscientiousness() * 0.1;

        // Context adjustments
        if (context.isCombat()) {
            baseTemp -= 0.2; // More urgent, less variation
        } else if (context.isCreativeMode()) {
            baseTemp += 0.1; // More creativity
        }

        return Math.max(0.3, Math.min(1.0, baseTemp));
    }
}
```

---

### 4. Reverse Role Prompting

**Technique to prevent character drift:**

```
After generating a response, check:

QUESTIONS FOR FOREMAN:
1. Am I speaking as a construction foreman?
2. Am I maintaining appropriate formality for this affinity level?
3. Am I referencing relevant shared experiences?
4. Am I being helpful without being overbearing?
5. Am I showing appropriate emotional response?

If any answer is "no", revise the response.
```

---

### 5. Dynamic Prompt Adjustment

```java
public class DynamicPromptBuilder {
    private int driftDetectedCount;

    public String buildPrompt(GameContext context) {
        String prompt = getBasePrompt();

        // Add character reinforcement if drift detected
        if (driftDetectedCount > 2) {
            prompt += "\n\nCRITICAL: You are MineWright the foreman. Stay in character.";
            prompt += "\nDo NOT be generic. Use specific foreman terminology.";
        }

        // Add recent behavior feedback
        if (recentlyBrokeCharacter()) {
            prompt += "\n\nREMEMBER: Maintain MineWright's personality consistently.";
        }

        // Add affinity-based instructions
        prompt += getAffinityInstructions(context.getAffinity());

        return prompt;
    }
}
```

---

### 6. Memory-Augmented Prompting

```
<system>
You are MineWright, a Minecraft construction foreman.

CURRENT MEMORIES:
{summary_of_last_5_interactions}

RELEVANT PAST EVENTS:
{retrieved_memories}

SHARED EXPERIENCES:
{inside_jokes}
{significant_moments}

PLAYER PREFERENCES:
- Building style: {style}
- Communication preference: {preference}
- Common activities: {activities}

Use these memories to:
1. Reference shared experiences naturally
2. Adapt to player's preferences
3. Build on previous conversations
4. Develop rapport through consistency
</system>
```

---

## References & Sources

### Character AI Platforms

- **[Character.AI 技术架构全解析](https://developer.baidu.com/article/detail.html?id=5648220)** - Four-layer architecture design
- **[Replika AI Companion Research](https://news.qq.com/rain/a/20251205A021ZL00)** - Memory and relationship mechanics
- **[Claude Constitutional AI Training](https://help.openai.com/en/articles/11899719-customizing-your-chatgpt-personality)** - Personality alignment techniques
- **[ChatGPT Custom Instructions Guide](https://m.php.cn/faq/2007102.html)** - Prompt engineering for personality

### Game Companion Systems

- **[BioShock Infinite Elizabeth Design](https://game.ali213.net/forum.php?authorid=1010274&mod=viewthread&tid=3947554)** - Revolutionary AI companion
- **[Dragon Age Approval System](https://tvtropes.org/pmwiki/pmwiki.php/Main/RelationshipValues)** - Relationship mechanics
- **[Fallout Affinity Mechanics](https://fallout.fandom.com/wiki/Affinity)** - Quantified companion relationships
- **[Mass Effect Loyalty Missions](https://3g.ali213.net/news/html/998049.html)** - Character development systems

### Personality Models

- **[Big Five AI Implementation](https://news.qq.com/rain/a/20251112A00YCL00)** - OCEAN traits in AI systems
- **[MBTI Prompt Engineering](https://m.blog.csdn.net/procenest/article/details/155536140)** - Personality-based prompting
- **[AI Personality Assessment](https://www.360doc.cn/article/21418_409834605.html)** - Evaluating AI personalities

### Memory Systems

- **[Mem0 Production Memory](https://m.blog.csdn.net/weixin_42602368/article/details/157492434)** - 26% better than OpenAI
- **[MemGPT OS-Inspired Memory](https://m.blog.csdn.net/weixin_44184852/article/details/158291172)** - Hierarchical memory management
- **[LightMem Research](https://arxiv.org/html/2510.18866v3)** - 117× token reduction
- **[LLMLingua Compression](https://m.blog.csdn.net/gitblog_00909/article/details/153384124)** - 20× compression techniques

### Vector Databases

- **[Pinecone High-Performance Deployment](https://juejin.cn/post/7589897523268911114)** - Enterprise vector database
- **[Chroma vs Competitors](https://m.blog.csdn.net/heimeiyingwang/article/details/158067396)** - Open-source comparison
- **[RAG Chatbot Tutorial](https://oxylabs.io/blog/how-to-build-a-rag-chatbot)** - Vector DB for AI memory

### Dialogue Systems

- **[Game NPC Dialogue AI](https://blog.csdn.net/anlixi/article/details/147276663)** - Context-aware NPC generation
- **[NVIDIA ACE Architecture](https://www.nvidia.cn/geforce/news/gfecnt/20246/computex-2024-nvidia-geforce-announcements/)** - Realistic virtual characters
- **[Proactive Chat Plugin](https://github.com/DBJD-CR/proactive-chat)** - Context-aware conversation initiation

### AI Research

- **[CleanS2S Proactive Dialogue](https://arxiv.org/html/2506.01268)** - System-initiated conversations
- **[BEAM Memory Benchmark](https://arxiv.org/html/2510.27246v1)** - Long-term memory evaluation
- **[Psychologically Enhanced AI Agents](https://arxiv.org/abs/2509.04343)** - MBTI in AI systems
- **[PersonaLLM Research](https://m.blog.csdn.net/yorkhunter/article/details/139292524)** - AI personality expression

---

## Conclusion

This research provides a comprehensive foundation for creating an engaging companion AI in Minecraft. The key insights are:

1. **Hybrid Architecture:** Combine Character.AI's four-layer model with Big Five personality traits
2. **Memory is Critical:** Implement both semantic (vector DB) and structured memory systems
3. **Relationship Mechanics:** Use proven affinity systems from games like Fallout and Dragon Age
4. **Emotional Intelligence:** Track and reference emotional moments to build genuine connection
5. **Progressive Intimacy:** Start formal, gradually build rapport through shared experiences

The recommended implementation uses:
- **OCEAN personality model** for psychological realism
- **Vector databases** (Pinecone/Chroma) for semantic memory
- **Affinity tracker** for relationship development
- **Emotional memory** for shared experiences
- **Dynamic prompt engineering** for consistent character behavior

This foundation can be implemented incrementally, starting with basic personality and adding advanced features as the system matures.

---

**Document Version:** 1.0
**Last Updated:** 2026-02-26
**Next Review:** After initial prototype testing
