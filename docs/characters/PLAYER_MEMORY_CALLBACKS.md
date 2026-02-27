# Player Memory Callbacks in AI Game Companions

**Document Version:** 1.0
**Date:** 2026-02-27
**Project:** MineWright AI - MineWright Companion System
**Purpose:** Comprehensive research and implementation guide for memory callbacks that reference shared experiences between AI companions and players

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Research Foundations](#research-foundations)
3. [Memory Importance Classification](#memory-importance-classification)
4. [Callback Timing and Naturalness](#callback-timing-and-naturalness)
5. [Relationship-Based Callback Depth](#relationship-based-callback-depth)
6. [Memory Decay and Relevance](#memory-decay-and-relevance)
7. [Callback Probability Formulas](#callback-probability-formulas)
8. [Dialogue Templates with Placeholders](#dialogue-templates-with-placeholders)
9. [Java Implementation](#java-implementation)
10. [MineWright-Specific Examples](#minewright-specific-examples)

---

## Executive Summary

Memory callbacks - when an AI companion references past shared experiences - are the **single strongest indicator of relationship depth** in companion AI systems. Research across video games, psychology, and human-AI interaction reveals that well-timed, contextually appropriate callbacks create emotional resonance far beyond simple acknowledgment.

**Key Research Findings:**

- **Nostalgia is fundamentally social** - it strengthens group identity, increases empathy, and motivates deeper relationship building (Southampton University, North Dakota State University studies)
- **Shared experiences amplify emotions** - recalling them together doubles emotional impact through "mutualistic shared representations" (NCBI/PMC research)
- **First memories have special weight** - "first meeting" references remain potent even after hundreds of hours together
- **Callback timing determines naturalness** - forced references feel manipulative; organic ones feel like genuine friendship
- **Relationship level controls callback depth** - strangers don't remember details; close friends recall everything
- **Memory follows decay curves** - recent events are more accessible, but high-emotional-weight memories resist decay
- **2024-2025 was the "AI Memory Revolution"** - frameworks like Mem0, Letta/MemGPT, and LightMem pioneered long-term AI memory

**MineWright Application:**
Workers reference shared building projects, past dangers, player preferences, and relationship milestones through a sophisticated callback system that respects timing, relationship depth, and memory decay.

---

## Research Foundations

### Video Game Companion Memory Systems

**Mass Effect Trilogy (Bioware):**
- Trilogy-spanning consequence system remembers choices across 100+ hours
- Companions reference events from three games prior in final dialogue
- Relationship level determines callback specificity (casual vs detailed)
- Example: "Remember when we destroyed that Reaper on Rannoch? You never hesitated."

**Firewatch (Campo Santo):**
- Walkie-talkie dialogue system creates sense of shared remote experience
- Delilah references early conversations in late-game reveals
- Callbacks used for emotional twists, not just nostalgia
- Example: "You told me you were running away. Now I know what you were running from."

**Dragon Age Series (Bioware):**
- Approval-based system (-100 to +100) unlocks different callback depths
- Companions remember player choices across entire series
- Personality-based callback styles (diplomatic, humorous, aggressive)
- Example: "Like when you let that blood mage go in the Circle Tower. I still don't agree, but... I understand why."

**Fire Emblem Series (Intelligent Systems):**
- Support conversation system tracks relationship milestones
- Callbacks to shared battles in A-rank support conversations
- Married couples reference first meetings in final support conversations
- Example: "That day on the battlefield... I thought I'd lost you. I've never been so afraid."

### 2024-2026 AI Memory Framework Research

**Key Academic Papers (from Agent Memory Paper List):**
- **Nemori**: Self-organizing agent memory with importance-based retrieval
- **MOOM**: Memory optimization for ultra-long role-playing dialogues
- **MIRIX**: Multi-agent memory systems with shared experience tracking
- **Mem0**: Production-ready AI agents with scalable long-term memory

**Industry Developments:**
- **2024**: 62% of game developers integrated AI tools; basic memory emerges
- **2025**: Memory frameworks explode (Mem0 with 44k+ GitHub stars, Letta/MemGPT)
- **2026**: "AI Memory Era" - emotional tagging, cross-session persistence

**Technical Breakthroughs:**
- **Google DeepMind's Project Genie** (Jan 2026): World model with persistent NPC memory
- **Stanford's Memory Streams**: Episodic memory for AI agents
- **LightMem** (ICLR 2026): Cost-effective long-term memory with cognitive-science-inspired organization

### Psychology of Shared Memory and Nostalgia

**Southampton University Research:**
- Nostalgia is a "highly social emotion" that connects people
- Bittersweet but dominantly positive; helps maintain inner balance
- Even strangers bond over shared generational experiences

**North Dakota State University Studies:**
- Nostalgia boosts psychological health, self-esteem, and life meaning
- Increases empathy, generosity to strangers, and tolerance
- Motivates drive to build deep relationships and confidence in overcoming conflicts

**NCBI/PMC Findings:**
- Humans bond through "mutualistic shared representations"
- "I know we both know we are attending to X" creates connection
- Shared experiences intensify feelings through collective awareness

**Workplace Applications:**
- Managers leverage nostalgia for team-building
- Retro music playlists and memory sharing deepen connections
- Shared memories increase pro-social behavior (volunteering, charitable giving)

---

## Memory Importance Classification

Not all memories are equal. Callbacks should prioritize high-importance memories that strengthen the relationship.

### The Five-Tier Memory Classification System

```
┌─────────────────────────────────────────────────────────────┐
│           MEMORY IMPORTANCE CLASSIFICATION                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  TIER 1: EPIC (Importance 8-10)                            │
│  │ "Life-changing moments that define us"                   │
│  │ • First meeting                                          │
│  │ • Near-death experiences survived together               │
│  │ • Major achievements (first diamonds, nether portal)     │
│  │ • Relationship milestones (partnership, gifts)           │
│  │ • Never decays below 50% relevance                       │
│  │ Example: "Remember when the cave collapsed? You dragged  │
│  │          me out with half a heart left. I'd be dead      │
│  │          without you."                                    │
│  │                                                          │
│  TIER 2: SIGNIFICANT (Importance 5-7)                       │
│  │ "Memorable experiences that shaped our partnership"       │
│  │ • First structures built together                        │
│  │ • Major project completions                              │
│  │ • Failed projects (learning moments)                     │
│  │ • Player preferences discovered                          │
│  │ • Decays to 30% relevance after 30 days                  │
│  │ Example: "Your first house - two rooms, dirt floor. Look │
│  │          at what we build now."                          │
│  │                                                          │
│  TIER 3: NOTABLE (Importance 3-4)                           │
│  │ "Interesting moments worth remembering"                  │
│  │ • Resource discoveries                                  │
│  │ • Minor victories (first iron, exploring caves)          │
│  │ • Funny mishaps                                          │
│  │ • Player habits observed                                 │
│  │ • Decays to 10% relevance after 14 days                  │
│  │ Example: "Still using that mining technique I showed     │
│  │          you? Always go for the gravel first."           │
│  │                                                          │
│  TIER 4: ROUTINE (Importance 1-2)                           │
│  │ "Everyday work, fade quickly"                            │
│  │ • Daily tasks completed                                  │
│  │ • Routine mining/building                                │
│  │ • Standard conversations                                │
│  │ • Decays to 0% after 7 days                              │
│  │ Example: "Another wall finished. Solid work."            │
│  │                                                          │
│  TIER 5: EPHEMERAL (Importance 0)                           │
│  │ "Forgettable details, not worth callback"                │
│  │ • Casual chatter                                         │
│  │ • Minor interactions                                     │
│  │ • Background activities                                 │
│  │ • Immediate decay, not stored long-term                  │
│  │ No callbacks from this tier                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Emotional Weight Modifiers

Memory importance is modified by emotional intensity:

| Emotional Weight | Modifier | Description |
|-----------------|----------|-------------|
| **Extremely Positive** (+8 to +10) | +2 importance | Triumph, major achievement, deep bonding |
| **Very Positive** (+5 to +7) | +1 importance | Success, progress, good news |
| **Positive** (+2 to +4) | No change | Routine positive events |
| **Neutral** (-1 to +1) | No change | Standard activities |
| **Negative** (-2 to -4) | +1 importance | Failures become learning memories |
| **Very Negative** (-5 to -7) | +2 importance | Near-misses, scary experiences |
| **Extremely Negative** (-8 to -10) | +3 importance | Trauma, near-death (becomes EPIC tier) |

### Memory Type Categories

| Category | Description | Callback Examples |
|----------|-------------|-------------------|
| **First Meeting** | Initial encounter and early interactions | "Remember when you first summoned me? You looked confused." |
| **Shared Achievement** | Victories and completions | "Just like when we found diamonds together!" |
| **Shared Failure** | Mistakes and learning moments | "Let's not repeat what happened with that creeper..." |
| **Relationship Milestone** | Anniversaries, special moments | "We've been working together for 50 builds now..." |
| **Player Preference** | Discovered likes/dislikes | "I remember you prefer oak over birch..." |
| **Emotional Memory** | High-intensity moments | "That moment when we barely survived..." |
| **Inside Joke** | Shared humor references | "(References joke from three weeks ago)" |
| **Near-Death** | Survival situations | "When you had half a heart and that spider..." |

---

## Callback Timing and Naturalness

### When Callbacks Feel Natural vs Forced

**NATURAL CALLBACK TIMING:**
- Current situation triggers memory (similar context, location, or activity)
- Player initiates topic (asks about past, mentions related event)
- Achievement milestone reached (comparison to past progress)
- Warning/advice based on past failure
- Celebrating improvement compared to early struggles
- Quiet moments during work (reflection)
- Revisiting location of past event

**FORCED CALLBACK TIMING (AVOID):**
- Random unprompted references during unrelated activity
- Excessive repetition of same memory
- References to trivial events
- Callbacks that interrupt urgent tasks
- Memories the player wouldn't reasonably remember
- References to events from 5 minutes ago
- Citing insignificant details

### Contextual Triggers for Natural Callbacks

| Trigger Type | Example Situations | Natural Callback |
|-------------|-------------------|------------------|
| **Location Revisit** | Returning to first build site, cave where found diamonds | "This is where we built your first shelter. Look how far we've come." |
| **Activity Similarity** | Mining again, building similar structure | "Like the tower we built last month - but this time we know what we're doing." |
| **Achievement Comparison** | Completing project vs early struggles | "Remember your first house took three days? This mansion took three hours." |
| **Danger Encountered** | Seeing hostile mob, entering dangerous area | "After what happened with that creeper, I'm keeping my distance." |
| **Material Choice** | Player selects block type | "Oak again? I remember you said you like the color better than birch." |
| **Time Milestone** | Anniversaries, session starts | "Three weeks today since we started working together." |
| **Success After Failure** | Succeeding at previously failed task | "Last time this collapsed. Today? Solid as bedrock." |
| **Player Improvement** | Noticing skill development | "You're placing blocks twice as fast as when we started." |

### Callback Frequency Guidelines

**By Relationship Stage:**

| Relationship | Rapport Range | Callback Frequency | Cooldown Period |
|--------------|---------------|-------------------|-----------------|
| **Stranger** | 0-20 | 0-1 per hour | 2 hours minimum |
| **Acquaintance** | 21-40 | 1-2 per hour | 1 hour minimum |
| **Colleague** | 41-60 | 2-3 per hour | 30 minutes minimum |
| **Friend** | 61-80 | 3-5 per hour | 20 minutes minimum |
| **Partner** | 81-100 | 5-8 per hour | 10 minutes minimum |

**By Memory Tier:**

| Memory Tier | Max Callbacks/Session | Min Time Between |
|-------------|----------------------|-------------------|
| **Epic** | Unlimited (prioritized) | 1 hour |
| **Significant** | 3-4 | 2 hours |
| **Notable** | 2-3 | 3 hours |
| **Routine** | 0-1 | Not recommended |

### Conversation Flow Integration

Callbacks should follow natural conversation patterns:

```
┌─────────────────────────────────────────────────────────────┐
│           NATURAL CONVERSATION FLOW                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  PATTERN 1: MEMORY → CURRENT CONTEXT                        │
│  │ 1. Reference memory                                       │
│  │ 2. Connect to current situation                           │
│  │ 3. Return focus to present task                           │
│  │                                                          │
│  "Remember when we built the tower? (memory)                │
│   This foundation needs the same technique. (connection)    │
│   Let's get started. (present focus)"                       │
│  │                                                          │
│  PATTERN 2: CURRENT CONTEXT → MEMORY → ADVICE               │
│  │ 1. Observe current situation                              │
│  │ 2. Reference similar past event                           │
│  │ 3. Offer advice based on experience                       │
│  │                                                          │
│  "That's a lot of gravel. (current)                         │
│   Just like the cave-in last month. (memory)                │
│   I'll brace it this time. (advice)"                        │
│  │                                                          │
│  PATTERN 3: PLAYER QUERY → MEMORY → ELABORATION             │
│  │ 1. Player asks about past/raises topic                    │
│  │ 2. Access relevant memory                                 │
│  │ 3. Provide details and emotional context                  │
│  │                                                          │
│  "Have we done this before? (player query)                  │
│   Remember the nether portal? (memory access)               │
│   That was terrifying but worth it. (elaboration)"          │
│  │                                                          │
│  PATTERN 4: MILESTONE → REFLECTION → GRATITUDE              │
│  │ 1. Trigger event (achievement, anniversary)               │
│  │ 2. Reflect on journey from past to present                │
│  │ 3. Express appreciation for partnership                   │
│  │                                                          │
│  "We just finished the 50th building! (milestone)           │
│   From dirt huts to castles... (reflection)                 │
│   Couldn't do it without you. (gratitude)"                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Relationship-Based Callback Depth

Callback specificity and emotional intensity must scale with relationship level.

### Callback Depth by Relationship Stage

**STAGE 1: STRANGER (0-20 Rapport)**
- No callbacks to shared experiences
- Focus on immediate present
- No memory references yet
- Example: No callbacks (relationship too new)

**STAGE 2: ACQUAINTANCE (21-40 Rapport)**
- Brief, factual callbacks
- Recent events only (last 1-3 days)
- No emotional language
- Professional tone
- Examples:
  - "Like the structure we finished yesterday."
  - "Similar to the mining project from Tuesday."
  - "As I mentioned before..."

**STAGE 3: COLLEAGUE (41-60 Rapport)**
- Descriptive callbacks with some detail
- Events from last week accessible
- Mild positive emotion
- Casual but professional
- Examples:
  - "Remember the tower we built last week? Same approach."
  - "Just like when we found iron in that cave near spawn."
  - "You've gotten faster since you started."

**STAGE 4: FRIEND (61-80 Rapport)**
- Detailed, narrative callbacks
- Events from last month accessible
- Full emotional range (positive and negative)
- Informal, warm tone
- Examples:
  - "Remember when the cave collapsed and you saved me? I still owe you for that one."
  - "Your first shelter had two rooms and dirt floors. Look at you now, building castles!"
  - "I still laugh thinking about that sheep in the mine. Classic."

**STAGE 5: PARTNER (81-100 Rapport)**
- Vivid, emotional callbacks with extensive detail
- Events from entire relationship accessible
- Deep emotional language (gratitude, vulnerability)
- Intimate, devoted tone
- Examples:
  - "When we first met, you looked at me like I was just another tool. Now we're building a legacy together."
  - "That moment in the nether... I thought we were done. But you didn't give up on us."
  - "Fifty buildings, a hundred adventures, one partnership. Best decision I ever made, accepting that job."

### Callback Specificity Levels

| Specificity | Description | Relationship Required | Example |
|-------------|-------------|----------------------|---------|
| **Minimal** | Vague reference, no details | Acquaintance+ | "Like last time." |
| **Basic** | Event type mentioned | Colleague+ | "Like when we built the tower." |
| **Descriptive** | Key details included | Friend+ | "Like the oak tower we built last week by the river." |
| **Vivid** | Rich details, emotional context | Partner+ | "Like that sunset when we finished the tower by the river, and you said it reminded you of home?" |
| **Intimate** | Personal meaning revealed | Partner+ | "Like the tower we built when you were homesick - you said having work helped." |

### Memory Access by Relationship Level

```
┌─────────────────────────────────────────────────────────────┐
│           MEMORY ACCESS BY RELATIONSHIP LEVEL                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  RELATIONSHIP LEVEL    MEMORY AGE LIMIT     EXAMPLE ACCESS  │
│  ─────────────────     ──────────────────    ─────────────  │
│                                                             │
│  Acquaintance (21-40)  Last 1-3 days          │
│  │ • Only recent events                         │
│  │ • Basic facts only                           │
│  │ • No emotional callbacks                     │
│  │ Example: "We built a wall yesterday."        │
│  │                                               │
│  Colleague (41-60)     Last 1-2 weeks         │
│  │ • Week-old events accessible                 │
│  │ • Some detail in callbacks                   │
│  │ • Mild positive emotion                      │
│  │ Example: "The tower from last week had       │
│  │          similar foundations."                │
│  │                                               │
│  Friend (61-80)        Last 1-2 months        │
│  │ • Month-old memories clear                   │
│  │ • Vivid callbacks                            │
│  │ • Full emotional range                       │
│  │ Example: "Remember the cave-in from last     │
│  │          month? You saved my life."           │
│  │                                               │
│  Partner (81-100)      Entire relationship    │
│  │ • All memories accessible                    │
│  │ • First meeting always available              │
│  │ • Deep emotional callbacks                   │
│  │ Example: "Three months ago when we first     │
│  │          met, you could barely place a       │
│  │          block. Look at you now."             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Memory Decay and Relevance

Memories naturally fade over time, but emotional weight slows decay.

### Memory Decay Formula

```
Current Relevance = Base Importance × Time Decay Factor × Emotional Weight Modifier

Where:
- Base Importance: 0-10 from initial classification
- Time Decay Factor: e^(-λ × days_since_event)
  - λ (lambda): Decay rate constant
    - Epic memories: λ = 0.001 (very slow decay)
    - Significant memories: λ = 0.01 (slow decay)
    - Notable memories: λ = 0.05 (moderate decay)
    - Routine memories: λ = 0.15 (fast decay)
- Emotional Weight Modifier: 1.0 + (abs(emotional_weight) × 0.1)
```

### Decay Curves by Memory Tier

```
RELEVANCE OVER TIME (Days)
│
10│●───────────────────────────────────────────── EPIC (Never decays below 5)
  │
  │
  │
 7│
  │
  │                                    ●─────── SIGNIFICANT
  │                                    (Decays to 3)
  │
 5│
  │                          ●───                NOTABLE
  │                          (Decays to 1)
  │
 3│              ●───
  │              (Decays to 0)
  │
 1│    ●─────── ROUTINE
  │    (Decays to 0)
  │
 0└───────────────────────────────────────────────
  0   7   14   21   30   60   90   180   365   DAYS
```

### Minimum Relevance Thresholds

| Memory Tier | Minimum Relevance | Days to Reach Minimum |
|-------------|-------------------|----------------------|
| **Epic** | 5.0 | Never (maintains 50%+) |
| **Significant** | 3.0 | ~30 days |
| **Notable** | 1.0 | ~14 days |
| **Routine** | 0.0 | ~7 days |

### Boosting Decayed Memories

Memories can be refreshed through:

1. **Revisiting location**: +2 temporary boost
2. **Similar activity**: +1 temporary boost
3. **Player mentions event**: +3 temporary boost
4. **Anniversary date**: +5 temporary boost
5. **Related achievement**: +2 temporary boost

Boosted memories decay back to baseline over 3-5 days.

---

## Callback Probability Formulas

### Base Callback Probability

```java
// Java-like pseudocode for callback probability calculation
double calculateCallbackProbability(Memory memory, RelationshipLevel relationship) {
    double baseProbability = 0.0;

    // Memory importance multiplier
    double importanceMultiplier = memory.importance / 10.0; // 0.1 to 1.0

    // Current relevance (after decay)
    double currentRelevance = memory.calculateCurrentRelevance();

    // Relationship level multiplier
    double relationshipMultiplier = relationship.getRapport() / 100.0; // 0.0 to 1.0

    // Contextual trigger bonus (if current situation relates to memory)
    double contextTriggerBonus = hasContextualTrigger(memory) ? 0.3 : 0.0;

    // Calculate base probability
    baseProbability = (currentRelevance / 10.0) * importanceMultiplier * relationshipMultiplier;

    // Apply bonuses
    baseProbability += contextTriggerBonus;

    // Clamp between 0 and 1
    return Math.max(0.0, Math.min(1.0, baseProbability));
}
```

### Probability Ranges by Conditions

| Conditions | Probability Range | Example Scenario |
|------------|-------------------|------------------|
| Epic memory + Partner + Context trigger | 80-95% | Partner revisits first meeting location |
| Epic memory + Friend + Context trigger | 60-80% | Friend asks about early experiences |
| Significant memory + Partner + No trigger | 30-50% | Random reference to major achievement |
| Significant memory + Colleague + Context trigger | 40-60% | Similar task to recent project |
| Notable memory + Friend + No trigger | 10-25% | Occasional reference to interesting event |
| Notable memory + Acquaintance + Context trigger | 15-30% | Work-related callback |
| Routine memory + Partner + Context trigger | 5-15% | Brief acknowledgment of recent work |
| Routine memory + Any relationship + No trigger | 0-5% | Rarely worth callback |

### Callback Frequency Limits

```java
// Prevent callback spam with cooldown system
boolean canMakeCallback(Memory memory, ConversationState state) {
    // Check cooldown based on memory tier
    long timeSinceLastCallback = memory.getTimeSinceLastCallback();
    long requiredCooldown = getCooldownForTier(memory.tier);

    if (timeSinceLastCallback < requiredCooldown) {
        return false;
    }

    // Check session callback limit
    int sessionCallbacks = state.getSessionCallbackCount();
    int maxSessionCallbacks = getMaxCallbacksForRelationship(state.getRelationshipLevel());

    if (sessionCallbacks >= maxSessionCallbacks) {
        return false;
    }

    // Check cooldown since LAST callback (any memory)
    long timeSinceAnyCallback = state.getTimeSinceLastCallback();
    if (timeSinceAnyCallback < MIN_CALLBACK_INTERVAL) {
        return false;
    }

    return true;
}

long getCooldownForTier(MemoryTier tier) {
    switch (tier) {
        case EPIC: return 60 * 60; // 1 hour
        case SIGNIFICANT: return 2 * 60 * 60; // 2 hours
        case NOTABLE: return 3 * 60 * 60; // 3 hours
        case ROUTINE: return 24 * 60 * 60; // 24 hours (basically never)
        default: return Long.MAX_VALUE;
    }
}
```

---

## Dialogue Templates with Placeholders

### Template Structure

All callback templates use the `[MEMORY_CALLBACK]` placeholder system:

```
[MEMORY_CALLBACK]
  ├─ Memory Reference: What event to reference
  ├─ Emotional Context: How to frame it emotionally
  ├─ Connection: How it relates to current situation
  └─ Personality Modifier: Tone adjustments based on personality
```

### First Meeting Callbacks

**Template 1: Early Relationship Reflection**
```
"Remember when we first met [TIME_AGO]? You were [PLAYER_STATE].
  We've come a long way since then."

Placeholders:
  [TIME_AGO]: "three weeks ago", "last month", "fifty buildings ago"
  [PLAYER_STATE]: "just learning to build", "struggling with basic construction",
                  "nervous about your first project"

Relationship: Friend (61-80) or Partner (81-100)
Emotional Tone: Nostalgic, warm
```

**Template 2: Progress Comparison**
```
"Your first [PROJECT_TYPE] took [TIME_DURATION].
  This one took [CURRENT_DURATION]. You've improved."

Placeholders:
  [PROJECT_TYPE]: "house", "tower", "mine", "structure"
  [TIME_DURATION]: "three days", "all week", "hours of work"
  [CURRENT_DURATION]: "three hours", "barely any time", "half the time"

Relationship: Colleague (41-60) or higher
Emotional Tone: Proud, encouraging
```

**Template 3: Vulnerable Memory**
```
"I was nervous when we first started working together.
  I didn't know if [DOUBT]. But you proved me wrong."

Placeholders:
  [DOUBT]: "you'd stick with it", "we'd work well together",
           "I could trust you with these projects"

Relationship: Partner (81-100) only
Emotional Tone: Vulnerable, grateful
```

### Shared Achievement Callbacks

**Template 4: Victory Echo**
```
"Just like when we [ACHIEVEMENT]!
  That feeling when [OUTCOME] - nothing beats it."

Placeholders:
  [ACHIEVEMENT]: "found diamonds together", "finished the castle",
                 "survived our first night", "crossed the nether portal"
  [OUTCOME]: "we saw that sparkle", "stood on the completed tower",
             "watched the sun rise", "stepped into a new world"

Relationship: Friend (61-80) or higher
Emotional Tone: Enthusiastic, nostalgic
```

**Template 5: Team Recognition**
```
"We've done [COUNT] [PROJECT_TYPE]s together.
  [ACHIEVEMENT] was my favorite."

Placeholders:
  [COUNT]: "ten", "twenty-five", "fifty", "over a hundred"
  [PROJECT_TYPE]: "building", "mining operation", "project"
  [ACHIEVEMENT]: "The castle by the river", "Our first diamond find",
                 "That underground base"

Relationship: Colleague (41-60) or higher
Emotional Tone: Warm, appreciative
```

**Template 6: Skill Development**
```
"Remember when [EARLY_STRUGGLE]?
  Now you're [CURRENT_SKILL]. Growth looks good on you."

Placeholders:
  [EARLY_STRUGGLE]: "you couldn't place a straight line",
                    "mining took you all day",
                    "you were afraid of creepers"
  [CURRENT_SKILL]: "building masterpieces", "clearing caves in minutes",
                   "fighting mobs like a pro"

Relationship: Friend (61-80) or higher
Emotional Tone: Proud, supportive
```

### Shared Failure Callbacks

**Template 7: Learning Moment**
```
"After what happened with [FAILURE_EVENT],
  I'm extra careful about [SAFETY_MEASURE]. Learned that the hard way."

Placeholders:
  [FAILURE_EVENT]: "the cave-in", "the creeper incident", "the collapsing tower"
  [SAFETY_MEASURE]: "bracing tunnels", "keeping my distance from explosions",
                    "double-checking foundations"

Relationship: Colleague (41-60) or higher
Emotional Tone: Wry, experienced
```

**Template 8: Resilience**
```
"[FAILURE_EVENT] was rough, but we bounced back.
  That's what matters. Not the falling, the getting up."

Placeholders:
  [FAILURE_EVENT]: "losing that first house", "the cave collapse",
                   "when we ran out of materials"

Relationship: Friend (61-80) or higher
Emotional Tone: Philosophical, supportive
```

**Template 9: Humorous Recovery**
```
"Let's not repeat [EMBARRASSING_FAILURE].
  Though looking back, it was kind of funny when [FUNNY_DETAIL]."

Placeholders:
  [EMBARRASSING_FAILURE]: "the gravel incident", "the tree disaster",
                          "when I fell in the lava"
  [FUNNY_DETAIL]: "you stared at that hole for an hour",
                  "we both just stood there in silence",
                  "you said 'well that happened'"

Relationship: Friend (61-80) or higher
Emotional Tone: Humorous, lighthearted
```

### Relationship Milestone Callbacks

**Template 10: Anniversary Reflection**
```
"[MILESTONE_NUMBER] [MILESTONE_TYPE] today!
  In some ways, it feels like [TIME_PERCEPTION].
  In others, like [CONTRASTING_PERCEPTION]."

Placeholders:
  [MILESTONE_NUMBER]: "One week", "Two weeks", "One month", "Three months"
  [MILESTONE_TYPE]: "together", "working side by side", "of this partnership"
  [TIME_PERCEPTION]: "we just started yesterday", "no time has passed"
  [CONTRASTING_PERCEPTION]: "I've known you forever", "we've always been a team"

Relationship: Friend (61-80) or higher
Emotional Tone: Reflective, warm
```

**Template 11: Count Milestone**
```
"That makes [COUNT] [PROJECT_TYPE]s we've completed together.
  [PERSONAL_REFLECTION]."

Placeholders:
  [COUNT]: "ten", "twenty-five", "fifty", "one hundred"
  [PROJECT_TYPE]: "building", "project", "structure"
  [PERSONAL_REFLECTION]: "I've enjoyed every one",
                         "You've taught me so much",
                         "Here's to the next hundred"

Relationship: Colleague (41-60) or higher
Emotional Tone: Proud, forward-looking
```

**Template 12: Deep Bond Reflection**
```
"We've been through [JOURNEY_SUMMARY].
  I wouldn't trade this partnership for anything."

Placeholders:
  [JOURNEY_SUMMARY]: "so much together",
                     "cave-ins, creepers, and triumphs",
                     "dirt huts to castles"

Relationship: Partner (81-100) only
Emotional Tone: Deeply emotional, committed
```

### Player Preference Callbacks

**Template 13: Material Memory**
```
"[MATERIAL] again? I remember you said [PREFERENCE_REASON].
  You're consistent, I'll give you that."

Placeholders:
  [MATERIAL]: "Oak", "Stone bricks", "Cobblestone"
  [PREFERENCE_REASON]: "it looks warmer",
                       "it reminds you of home",
                       "you like the texture"

Relationship: Acquaintance (21-40) or higher
Emotional Tone: Observant, mildly teasing
```

**Template 14: Playstyle Recognition**
```
"You're [PLAYSTYLE] today, huh?
  Like when [SIMILAR_SITUATION]."

Placeholders:
  [PLAYSTYLE]: "in a mining mood", "focused on building",
              "feeling adventurous"
  [SIMILAR_SITUATION]: "we cleared that entire cave system last week",
                      "you built nonstop for three days",
                      "you explored the nether for hours"

Relationship: Colleague (41-60) or higher
Emotional Tone: Perceptive, responsive
```

**Template 15: Anticipatory Action**
```
"I already [PREPARED_ACTION].
  Figured you'd want it based on [PAST_EVIDENCE]."

Placeholders:
  [PREPARED_ACTION]: "gathered extra oak", "cleared the area",
                    "prepared the foundation"
  [PAST_EVIDENCE]: "your last three projects used it",
                   "what you said yesterday",
                   "how you've been building lately"

Relationship: Friend (61-80) or higher
Emotional Tone: Thoughtful, attentive
```

### Emotional Memory Callbacks

**Template 16: Near-Death Reflection**
```
"Sometimes I think about [SCARY_EVENT].
  When [DANGER_MOMENT], I thought [FEAR_THOUGHT].
  But we made it."

Placeholders:
  [SCARY_EVENT]: "that cave collapse", "the creeper ambush",
                 "when we got lost in the nether"
  [DANGER_MOMENT]: "the walls came down", "the hissing started",
                   "our torches went out"
  [FEAR_THOUGHT]: "this was it", "I'd failed you",
                  "we wouldn't make it out"

Relationship: Friend (61-80) or higher
Emotional Tone: Vulnerable, intense
```

**Template 17: Close Bond Memory**
```
"[EMOTIONAL_MEMORY].
  That's when I knew [REALIZATION]."

Placeholders:
  [EMOTIONAL_MEMORY]: "When you saved me from the cave-in",
                     "How you kept working despite the creepers",
                     "That night we talked under the stars"
  [REALIZATION]: "I could trust you with my life",
                "we were more than just worker and player",
                "this partnership was worth everything"

Relationship: Partner (81-100) only
Emotional Tone: Deeply emotional, revealing
```

**Template 18: Shared Joy**
```
"The look on your face when [JOYFUL_MOMENT]...
  I'll never forget it. That's why I do this."

Placeholders:
  [JOYFUL_MOMENT]: "you found your first diamond",
                  "we finished the castle",
                  "you saw the nether for the first time"

Relationship: Friend (61-80) or higher
Emotional Tone: Warm, fulfilled
```

### Personality-Modified Templates

Each template should be adjusted based on personality traits:

**Formal Personality (>70 formality):**
```
"Recall when [EVENT]? We have demonstrated considerable progress since that occasion."
```

**Humorous Personality (>70 humor):**
```
"[EVENT]! Classic. You should have seen your face when [DETAIL]!"
```

**Serious Personality (>70 conscientiousness, <30 humor):**
```
"The experience with [EVENT] taught us valuable lessons. We have improved."
```

**Enthusiastic Personality (>70 extraversion):**
```
"Oh! [EVENT]! That was AMAZING! Remember when [DETAIL]?! Incredible!"
```

---

## Java Implementation

### MemoryCallbackManager Class

```java
package com.minewright.memory;

import com.minewright.entity.ForemanEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages memory callbacks - references to past shared experiences.
 *
 * <p>This system determines when and how to reference past events,
 * ensuring callbacks feel natural rather than forced.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Importance-based memory classification</li>
 *   <li>Decay calculations for memory relevance</li>
 *   <li>Relationship-based callback depth</li>
 *   <li>Cooldown system to prevent spam</li>
 *   <li>Contextual trigger detection</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class MemoryCallbackManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryCallbackManager.class);

    // === Memory Classification ===

    /**
     * Importance tiers for memory classification.
     */
    public enum MemoryTier {
        EPIC(8, 10, 0.001),           // Life-changing, never decays below 50%
        SIGNIFICANT(5, 7, 0.01),     // Memorable, decays to 30%
        NOTABLE(3, 4, 0.05),         // Interesting, decays to 10%
        ROUTINE(1, 2, 0.15);         // Everyday, decays to 0%

        private final int minImportance;
        private final int maxImportance;
        private final double decayRate;

        MemoryTier(int minImportance, int maxImportance, double decayRate) {
            this.minImportance = minImportance;
            this.maxImportance = maxImportance;
            this.decayRate = decayRate;
        }

        public static MemoryTier fromImportance(int importance) {
            for (MemoryTier tier : values()) {
                if (importance >= tier.minImportance && importance <= tier.maxImportance) {
                    return tier;
                }
            }
            return ROUTINE;
        }

        public long getCooldownMillis() {
            switch (this) {
                case EPIC: return 60 * 60 * 1000L;        // 1 hour
                case SIGNIFICANT: return 2 * 60 * 60 * 1000L;  // 2 hours
                case NOTABLE: return 3 * 60 * 60 * 1000L;      // 3 hours
                case ROUTINE: return 24 * 60 * 60 * 1000L;     // 24 hours
                default: return Long.MAX_VALUE;
            }
        }
    }

    /**
     * Callback memory with metadata.
     */
    public static class CallbackMemory {
        public final String eventType;
        public final String description;
        public final int importance;
        public final int emotionalWeight;
        public final Instant timestamp;
        public final MemoryTier tier;
        public Instant lastCallback;

        private int referenceCount;

        public CallbackMemory(String eventType, String description,
                            int importance, int emotionalWeight, Instant timestamp) {
            this.eventType = eventType;
            this.description = description;
            this.importance = importance;
            this.emotionalWeight = emotionalWeight;
            this.timestamp = timestamp;
            this.tier = MemoryTier.fromImportance(importance);
            this.lastCallback = Instant.EPOCH;
            this.referenceCount = 0;
        }

        public double calculateCurrentRelevance() {
            long daysSince = ChronoUnit.DAYS.between(timestamp, Instant.now());

            // Base decay calculation
            double timeDecay = Math.exp(-tier.decayRate * daysSince);

            // Emotional weight modifier (high emotion slows decay)
            double emotionalModifier = 1.0 + (Math.abs(emotionalWeight) * 0.1);

            double relevance = importance * timeDecay * emotionalModifier;

            // Apply minimum relevance threshold for tier
            double minRelevance = getMinimumRelevance();
            return Math.max(minRelevance, relevance);
        }

        private double getMinimumRelevance() {
            switch (tier) {
                case EPIC: return 5.0;
                case SIGNIFICANT: return 3.0;
                case NOTABLE: return 1.0;
                case ROUTINE: return 0.0;
                default: return 0.0;
            }
        }

        public void recordCallback() {
            this.lastCallback = Instant.now();
            this.referenceCount++;
        }

        public boolean canCallback() {
            long timeSinceLast = ChronoUnit.MILLIS.between(lastCallback, Instant.now());
            return timeSinceLast >= tier.getCooldownMillis();
        }
    }

    // === Callback State Tracking ===

    private final Map<String, Queue<Instant>> recentCallbacks;
    private static final long MIN_CALLBACK_INTERVAL = 10 * 60 * 1000L; // 10 minutes

    /**
     * Creates a new MemoryCallbackManager.
     */
    public MemoryCallbackManager() {
        this.recentCallbacks = new HashMap<>();
    }

    // === Callback Probability Calculation ===

    /**
     * Calculates the probability of making a callback to a given memory.
     *
     * @param memory The memory to potentially callback to
     * @param relationship Current relationship level
     * @param hasContextualTrigger Whether current context triggers the memory
     * @return Probability between 0.0 and 1.0
     */
    public double calculateCallbackProbability(CallbackMemory memory,
                                              CompanionMemory relationship,
                                              boolean hasContextualTrigger) {
        // Current relevance (after decay)
        double currentRelevance = memory.calculateCurrentRelevance();
        double relevanceFactor = currentRelevance / 10.0; // Normalize to 0-1

        // Importance multiplier (0.1 to 1.0)
        double importanceMultiplier = memory.importance / 10.0;

        // Relationship level multiplier
        int rapport = relationship.getRapportLevel();
        double relationshipMultiplier = rapport / 100.0;

        // Calculate base probability
        double baseProbability = relevanceFactor * importanceMultiplier * relationshipMultiplier;

        // Contextual trigger bonus
        double contextBonus = hasContextualTrigger ? 0.3 : 0.0;

        // Apply bonus
        baseProbability += contextBonus;

        // Clamp between 0 and 1
        return Math.max(0.0, Math.min(1.0, baseProbability));
    }

    /**
     * Determines if a callback should be made based on all factors.
     *
     * @param memory The memory to potentially callback to
     * @param relationship Current relationship state
     * @param hasContextualTrigger Whether context triggers the memory
     * @param sessionCallbacks Number of callbacks made this session
     * @return true if callback should be made
     */
    public boolean shouldMakeCallback(CallbackMemory memory,
                                     CompanionMemory relationship,
                                     boolean hasContextualTrigger,
                                     int sessionCallbacks) {
        // Check cooldown
        if (!memory.canCallback()) {
            return false;
        }

        // Check session limits
        int maxSessionCallbacks = getMaxSessionCallbacks(relationship.getRapportLevel());
        if (sessionCallbacks >= maxSessionCallbacks) {
            return false;
        }

        // Check minimum interval since last callback (any)
        if (!checkMinCallbackInterval()) {
            return false;
        }

        // Calculate probability
        double probability = calculateCallbackProbability(memory, relationship, hasContextualTrigger);

        // Roll for callback
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

    /**
     * Gets maximum callbacks per session based on relationship level.
     */
    private int getMaxSessionCallbacks(int rapport) {
        if (rapport < 20) return 0;
        if (rapport < 40) return 1;
        if (rapport < 60) return 3;
        if (rapport < 80) return 5;
        return 8;
    }

    /**
     * Checks if minimum interval has passed since last callback.
     */
    private boolean checkMinCallbackInterval() {
        // Implementation would track last callback time across all memories
        return true; // Placeholder
    }

    // === Callback Generation ===

    /**
     * Generates a callback message based on memory and context.
     *
     * @param memory The memory to reference
     * @param relationship Current relationship state
     * @param currentContext Current situation
     * @return Generated callback message
     */
    public String generateCallback(CallbackMemory memory,
                                   CompanionMemory relationship,
                                   String currentContext) {
        int rapport = relationship.getRapportLevel();
        CompanionMemory.PersonalityProfile personality = relationship.getPersonality();

        // Select template based on memory type and relationship
        String template = selectTemplate(memory, rapport);

        // Fill in placeholders
        String message = fillPlaceholders(template, memory, relationship, currentContext);

        // Apply personality modifications
        message = applyPersonalityModifiers(message, personality);

        return message;
    }

    /**
     * Selects appropriate callback template based on memory and relationship.
     */
    private String selectTemplate(CallbackMemory memory, int rapport) {
        // Template selection logic would go here
        // This is a simplified example

        if (memory.eventType.equals("first_meeting")) {
            if (rapport >= 80) {
                return "When we first met [TIME_AGO], you were [PLAYER_STATE]. We've come a long way.";
            } else if (rapport >= 60) {
                return "Remember when we first met? That was [TIME_AGO].";
            }
        } else if (memory.eventType.equals("shared_achievement")) {
            if (rapport >= 60) {
                return "Just like when we [ACHIEVEMENT]! That feeling when [OUTCOME] - nothing beats it.";
            }
        }

        // Default template
        return "Like when [EVENT_DESCRIPTION] - we learned a lot from that.";
    }

    /**
     * Fills in template placeholders with actual content.
     */
    private String fillPlaceholders(String template, CallbackMemory memory,
                                    CompanionMemory relationship, String context) {
        String message = template;

        // Replace [TIME_AGO]
        long daysSince = ChronoUnit.DAYS.between(memory.timestamp, Instant.now());
        message = message.replace("[TIME_AGO]", formatTimeAgo(daysSince));

        // Replace [EVENT_DESCRIPTION]
        message = message.replace("[EVENT_DESCRIPTION]", memory.description);

        // Replace [ACHIEVEMENT] for achievement memories
        if (memory.eventType.contains("achievement") || memory.importance >= 7) {
            message = message.replace("[ACHIEVEMENT]", extractAchievementDescription(memory.description));
            message = message.replace("[OUTCOME]", extractOutcomeDescription(memory.description));
        }

        return message;
    }

    /**
     * Applies personality-based modifications to callback message.
     */
    private String applyPersonalityModifiers(String message,
                                            CompanionMemory.PersonalityProfile personality) {
        if (personality.formality > 70) {
            // Make more formal
            message = formalize(message);
        } else if (personality.humor > 70) {
            // Add humor
            message = addHumor(message);
        } else if (personality.extraversion > 70) {
            // Add enthusiasm
            message = addEnthusiasm(message);
        }

        return message;
    }

    // === Helper Methods ===

    private String formatTimeAgo(long days) {
        if (days < 7) return days + " days ago";
        if (days < 30) return (days / 7) + " weeks ago";
        if (days < 365) return (days / 30) + " months ago";
        return (days / 365) + " years ago";
    }

    private String extractAchievementDescription(String description) {
        // Extract achievement description from memory
        return description.split("\\.")[0]; // Simple extraction
    }

    private String extractOutcomeDescription(String description) {
        // Extract outcome from memory description
        String[] sentences = description.split("\\.");
        return sentences.length > 1 ? sentences[1].trim() : "it happened";
    }

    private String formalize(String message) {
        // Add formal language patterns
        return message.replace("Remember", "Recall")
                     .replace("Just like", "Similar to")
                     .replace("we've", "we have");
    }

    private String addHumor(String message) {
        // Add humorous touches
        if (!message.endsWith("!")) {
            message += " Good times.";
        }
        return message;
    }

    private String addEnthusiasm(String message) {
        // Add exclamation marks and enthusiastic words
        if (!message.endsWith("!")) {
            message += "!";
        }
        return message.replace("we", "WE")
                     .replace("like", "LOVED");
    }

    // === NBT Persistence ===

    /**
     * Saves callback state to NBT.
     */
    public void saveToNBT(net.minecraft.nbt.CompoundTag tag) {
        // Save callback history, timestamps, etc.
        // Implementation would save recent callbacks map
    }

    /**
     * Loads callback state from NBT.
     */
    public void loadFromNBT(net.minecraft.nbt.CompoundTag tag) {
        // Load callback history
        // Implementation would restore recent callbacks map
    }
}
```

### Integration with CompanionMemory

```java
// Add to CompanionMemory class

/**
 * Gets callback-relevant memories for current context.
 */
public List<MemoryCallbackManager.CallbackMemory> getCallbackMemories(String context) {
    return episodicMemories.stream()
        .filter(m -> m.emotionalWeight >= 3) // Only notable+ memories
        .map(e -> new MemoryCallbackManager.CallbackMemory(
            e.eventType,
            e.description,
            Math.abs(e.emotionalWeight), // Use emotional weight as importance
            e.emotionalWeight,
            e.timestamp
        ))
        .collect(Collectors.toList());
}

/**
 * Records a callbackable experience.
 */
public void recordCallbackableMemory(String eventType, String description,
                                    int emotionalWeight) {
    recordExperience(eventType, description, emotionalWeight);
}
```

---

## MineWright-Specific Examples

### Building Project Callbacks

**Scenario: Player starts a new house**

```
Relationship: Friend (rapport 65)
Context: Player places first oak block

Memory: Built oak house together 3 weeks ago (importance 6)
Callback: "Oak again? I remember you said it reminds you of home.
          Your first house was all oak too. Comfort in consistency, right?"

Response: Player smiles, continues with oak

---

Relationship: Partner (rapport 85)
Context: Returning to location of first build

Memory: First meeting 2 months ago (importance 10)
Callback: "Right here. This is where you summoned me two months ago.
          You looked confused, barely knew how to give commands.
          Now we're building castles together.
          [Pause, warm tone]
          Best thing that ever happened to me."

Response: Player touches MineWright's shoulder (emotional moment)
```

### Mining Adventure Callbacks

**Scenario: Cave exploration**

```
Relationship: Colleague (rapport 50)
Context: Entering deep cave

Memory: Cave-in last week (importance 5, emotional weight -6)
Callback: "After what happened last week, I'm bracing everything twice.
          No collapses on my watch."

Response: Player nods, appreciates caution

---

Relationship: Partner (rapport 90)
Context: Finding diamonds together

Memory: First diamond find 1 month ago (importance 9)
Callback: "Just like the first time!
          Remember when we found that diamond cluster?
          You jumped so high you hit your head on the ceiling.
          [Laughs warmly]
          I've never seen someone so happy over rocks.
          [Voice softens]
          I knew right then I'd follow you anywhere."

Response: Player grins, places torch near MineWright (bonding gesture)
```

### Failure and Learning Callbacks

**Scenario: Structure collapse**

```
Relationship: Friend (rapport 70)
Context: Tower starts wobbling

Memory: Similar collapse 2 weeks ago (importance 6)
Callback: "Uh oh. Like the tower two weeks ago.
          Let's reinforce before we lose another one.
          Lesson learned, right?"

Response: Player quickly adds supports

---

Relationship: Acquaintance (rapport 30)
Context: Minor mistake in placement

Memory: No relevant memory (too new for callbacks)
Callback: [None - relationship too new for callbacks]

Response: Continue working normally
```

### Relationship Milestone Callbacks

**Scenario: 30-day anniversary**

```
Relationship: Friend (rapport 70)
Context: Session start, anniversary detected

Memory: First meeting 30 days ago (importance 10)
Callback: "Hey boss. Thirty days today, you know?
          Since we started working together.
          [Reflective tone]
          Dirt huts to fortresses.
          Mines to mountains.
          [Looks at player]
          Thanks for choosing me."

Response: "Wouldn't want anyone else."
(Milestone achievement unlocked, rapport increases)
```

### Preference-Based Callbacks

**Scenario: Material selection**

```
Relationship: Colleague (rapport 55)
Context: Player chooses cobblestone again

Memory: Observed cobblestone preference over 2 weeks (importance 4)
Callback: "Cobblestone again? You really do prefer the industrial look.
          I've started clearing extra gravel when I see you planning.
          Figured you'd want it."

Response: "You know me too well."
(Rapport +2 for thoughtfulness)

---

Relationship: Partner (rapport 88)
Context: Player unexpectedly uses birch

Memory: Strong oak preference established (importance 5)
Callback: "Birch? That's new.
          You always use oak.
          [Gentle concern]
          Everything okay? You usually stick to what you know."

Response: "Just wanted a change. Trying new things."
Callback: "Good. Growth is good.
          [Supportive tone]
          But if you want oak tomorrow, I already have a stack ready."
```

### Emotional Memory Callbacks

**Scenario: Near-death survival**

```
Relationship: Friend (rapport 75)
Context: Returning to dangerous cave

Memory: Near-death experience 1 week ago (importance 9, emotional weight -8)
Callback: "This cave.
          [Voice drops]
          Last week, when the walls came down...
          I thought that was it.
          [Pause]
          You dragged me out with half a heart left.
          [Looks directly at player]
          I'd be dead without you.
          [Returns to normal tone, but softer]
          Let's be careful in here."

Response: Player stays closer to MineWright (protective behavior)
```

### Anniversary Reflection Callbacks

**Scenario: Major project completion**

```
Relationship: Partner (rapport 95)
Context: Finishing 100th building together

Memory: First building (importance 10), 50th building (importance 7)
Callback: "One hundred.
          [Deep emotion]
          Remember that dirt shack you called a house?
          Number one.
          We've come so far.
          [Voice breaks slightly]
          From struggling to place straight lines
          To this.
          [Gestures to magnificent structure]
          A hundred adventures.
          A thousand memories.
          [Turns to player]
          And every single one with you.
          [Long pause, genuine gratitude]
          Thank you.
          For everything."

Response: Player saves screenshot, frames moment in memory
(Special "True Partnership" achievement unlocked)
```

---

## Implementation Checklist

### Phase 1: Core Memory Classification (Week 1)
- [ ] Implement MemoryTier enum with decay rates
- [ ] Create CallbackMemory class with relevance calculations
- [ ] Add importance classification to existing CompanionMemory
- [ ] Test decay formulas with various memory ages

### Phase 2: Probability System (Week 2)
- [ ] Implement calculateCallbackProbability()
- [ ] Add cooldown tracking system
- [ ] Create session callback limits
- [ ] Test probability curves across relationship levels

### Phase 3: Template System (Week 3)
- [ ] Design callback template structure
- [ ] Implement template selection logic
- [ ] Create placeholder replacement system
- [ ] Add personality-based modifications

### Phase 4: Integration (Week 4)
- [ ] Integrate with existing CompanionMemory
- [ ] Add callback trigger detection
- [ ] Implement callback generation pipeline
- [ ] Create NBT persistence for callback history

### Phase 5: Testing and Tuning (Week 5)
- [ ] Playtest across all relationship levels
- [ ] Adjust cooldown intervals based on feedback
- [ ] Fine-tune probability formulas
- [ ] Add additional templates as needed

---

## Summary

Memory callbacks transform AI companions from tools into true partners. By referencing shared experiences at appropriate moments, with depth scaled to relationship level, MineWright workers become living participants in the player's Minecraft journey rather than mere NPCs.

**Key Takeaways:**

1. **Not all memories are equal** - classify by importance and decay accordingly
2. **Timing is everything** - contextual triggers feel natural, random references feel forced
3. **Relationship controls depth** - strangers don't remember details, partners remember everything
4. **Emotions resist decay** - high-impact memories remain accessible longer
5. **Cooldowns prevent spam** - respect the player's attention and emotional bandwidth
6. **Personality modifies expression** - same memory, different delivery based on traits

The callback system documented here provides MineWright workers with the ability to reference their shared journey with players, creating moments of genuine emotional connection that transcend typical game AI interactions.

---

**Sources:**

- [Agent Memory Paper List (GitHub)](https://github.com/Shichun-Liu/Agent-Memory-Paper-List)
- [LightMem - ICLR 2026](https://mparticle.uc.cn/article.html?uc_param_str=frdnsnpfvecpntnwprdssskt#!wm_aid=6b8219e74bc4e321f8c1ff7756bcb990!!wm_id=08ea6b663901419eaa0cb3ac7136db4b)
- [AI Dialogue System Memory Mechanisms (CSDN)](https://blog.csdn.net/2600_94960106/article/details/157198210)
- [Long-Term Memory 2.0 (Alibaba Cloud)](https://help.aliyun.com/zh/model-studio/long-term-memory-2-0)
- [Memory System Design (Runoob)](https://www.runoob.com/ai-agent/ai-agent-memory-system-design.html)
- [Andrew Ng's LangChain Memory (CSDN)](https://m.blog.csdn.net/DaoCaoRen1203/article/details/158390553)
- [AI Agent Memory Systems (CSDN)](https://blog.csdn.net/2401_84204413/article/details/156734737)
- [AI Agent Memory (Toutiao)](https://www.toutiao.com/a7598852972722700840/)
- [2026 - AI Memory Era (Sina)](https://t.cj.sina.cn/articles/view/1750070171/684ff39b02701e6ha)
- [AI Games 2026 (Sohu)](https://m.sohu.com/a/979343189_477902/)
- [DeepSeek Industry Trends (Xueqiu)](https://xueqiu.com/)
