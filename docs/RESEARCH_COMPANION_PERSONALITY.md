# Research: AI Companion Personality Design for Games

**Project:** MineWright AI (MineWright Foreman Personality Enhancement)
**Date:** 2025-02-27
**Research Focus:** Archetype Systems, Relationship Evolution, Dialogue Generation, Proactive Behavior, Memorable Characters

---

## Table of Contents

1. [Archetype Systems](#1-archetype-systems)
2. [Relationship Evolution](#2-relationship-evolution)
3. [Dialogue Generation](#3-dialogue-generation)
4. [Proactive Behavior](#4-proactive-behavior)
5. [Memorable Characters](#5-memorable-characters)
6. [MineWright Foreman Personality Recommendations](#6-minewright-foreman-personality-recommendations)

---

## 1. Archetype Systems

### 1.1 Big Five Personality Traits Mapping

The **Big Five** (OCEAN) personality model provides a scientifically grounded framework for character design:

| Trait | Low Value | High Value | Game Application |
|-------|-----------|------------|------------------|
| **Openness** | Practical, traditional | Curious, creative | Determines willingness to try new strategies, explore unknown areas |
| **Conscientiousness** | Spontaneous, flexible | Organized, disciplined | Affects task planning, resource management, reliability |
| **Extraversion** | Reserved, solitary | Outgoing, energetic | Controls social behavior, dialogue frequency, enthusiasm |
| **Agreeableness** | Competitive, critical | Cooperative, helpful | Influences response to player mistakes, conflict resolution |
| **Neuroticism** | Calm, confident | Sensitive, anxious | Affects reaction to danger, failure tolerance, emotional expression |

**Implementation Pattern:**
```json
{
  "personality": {
    "openness": 0.7,
    "conscientiousness": 0.9,
    "extraversion": 0.4,
    "agreeableness": 0.6,
    "neuroticism": 0.3
  }
}
```

**Research Finding:** A 2025 Nature study on Chinese gamers found that **Openness was high across all gameplay style preferences**, while specific trait combinations affected social anxiety outcomes and playstyle choices.

### 1.2 MBTI-Inspired Game Characters

While the MBTI lacks scientific validity, its **cognitive function stacks** provide excellent character archetypes:

| MBTI Type | Cognitive Stack | Character Archetype | Example Dialogue |
|-----------|----------------|---------------------|------------------|
| **ISTJ (Inspector)** | Si-Te-Fi-Ne | The Reliable Foreman | "Blueprints exist for a reason. Follow them." |
| **ENTP (Inventor)** | Ne-Ti-Fe-Si | The Mad Scientist | "Technically, this SHOULD work..." |
| **ISFJ (Protector)** | Si-Fe-Ti-Ne | The Devoted Caretaker | "I made extra, just in case you forgot." |
| **ESTP (Entrepreneur)** | Se-Ti-Fe-Ni | The Action Hero | "Plan? We improvise. Works every time." |
| **INFJ (Counselor)** | Ni-Fe-Ti-Se | The Mysterious Guide | "I foresaw you'd need this... eventually." |

**Best Practice:** Use cognitive functions as **behavior modules** rather than rigid types. A character can be "High Te (Efficiency-focused)" without committing to full ENTJ.

### 1.3 Custom Trait Systems

**Component-Based Architecture:**

```java
public interface PersonalityTrait {
    float getValue(); // 0.0 to 1.0
    void adjustValue(float delta);
    String getTraitName();
}

public class PersonalitySystem {
    private Map<String, PersonalityTrait> traits;

    public float getResponseTo(PlayerAction action) {
        // Combine trait scores to determine reaction
        float enthusiasm = getTrait("extraversion").getValue() * 0.6f
                         + getTrait("openness").getValue() * 0.4f;
        return enthusiasm;
    }
}
```

**Trait Categories:**
- **Social Traits:** Gregariousness, assertiveness, warmth
- **Work Traits:** Diligence, perfectionism, efficiency
- **Emotional Traits:** Optimism, anxiety tolerance, frustration threshold
- **Communication Traits:** Verboseness, humor, bluntness

---

## 2. Relationship Evolution

### 2.1 Trust/Favor Systems

**Multi-Dimensional Trust Model:**

| Dimension | Description | Impact |
|-----------|-------------|--------|
| **Competence Trust** | Belief in companion's abilities | Unlocks new commands, autonomous actions |
| **Benevolence Trust** | Belief in companion's good intentions | Increases resource sharing, honest feedback |
| **Integrity Trust** | Consistency of companion's behavior | Predictable responses, reduced surprise events |

**Research Insight:** Studies on trust in free-to-play games reveal that **trust directly influences commitment to continue playing** and elicits co-creation behavior. Economic transactions, especially asymmetric ones, significantly increase trust.

**Implementation - Trust Decay:**
```java
public class TrustSystem {
    private float competenceTrust = 50.0f; // 0-100
    private float benevolenceTrust = 50.0f;
    private float integrityTrust = 50.0f;

    public void onTaskSuccess(Task task) {
        competenceTrust = Math.min(100, competenceTrust + 2.0f);
        integrityTrust = Math.min(100, integrityTrust + 1.0f);
    }

    public void onTaskFailure(FailureReason reason) {
        if (reason == FailureReason.INCOMPETENCE) {
            competenceTrust = Math.max(0, competenceTrust - 5.0f);
        }
    }

    public float getOverallTrust() {
        return (competenceTrust + benevolenceTrust + integrityTrust) / 3.0f;
    }
}
```

### 2.2 Shared Experience Tracking

**Memory Categories for Relationship Building:**

| Experience Type | Storage Format | Emotional Weight |
|-----------------|----------------|------------------|
| **First-Time Events** | "firstCreeperEncounter" | High (memorable) |
| **Shared Triumphs** | "defeatedFirstBossTogether" | High (+bond) |
| **Failures & Learning** | "bridgeCollapsedTwice" | Medium (+resilience) |
| **Extended Proximity** | "hoursWorkedTogether" | Low (cumulative) |
| **Resource Exchanges** | "diamondsGiftedCount" | Medium (+generosity) |
| **Saved Moments** | "playerSavedMeFromFall" | High (+gratitude) |

**Memory Weighting Formula:**
```
Bond Strength = Σ(Experience Weight × Time Decay Factor) / (Days Since Experience + 1)
```

### 2.3 Milestone-Based Unlocks

**Progressive Relationship Tiers:**

| Trust Level | Title | Dialogue Unlocks | Behavior Changes | Commands Unlocked |
|-------------|-------|------------------|------------------|-------------------|
| **0-20** | Acquaintance | Basic greetings | Cautious, formal | Basic commands only |
| **21-40** | Coworker | Work talk, mild opinions | Cooperative, asks questions | Standard task list |
| **41-60** | Trusted Colleague | Personal stories, gentle teasing | Proactive suggestions | Complex planning |
| **61-80** | Friend | Deep concerns, emotional support | Honest feedback, challenges bad decisions | Autonomous initiative |
| **81-100** | Partner | Vulnerability, dreams | Willing to disobey for player's good | Full coordination, shared resources |

**Research Case - Mass Effect 2 Loyalty System:**
> "Loyalty missions were one of the best parts of Mass Effect 2 because they allow players to fully understand each team member's background story, making every character come alive." — Mac Walters, Creative Director

Each companion had a **unique personal mission** tied to their backstory that unlocked:
- Full character depth and history
- New dialogue options
- Special combat abilities

**Key Insight:** Single missions aren't enough. Modern systems (Dragon Age: Veilguard) are moving toward **quantified, ongoing relationship building** with complex interaction series.

---

## 3. Dialogue Generation

### 3.1 Template Systems

**Hierarchical Template Structure:**

```
Context (Location, Time, Activity)
    ↓
Emotional State (Happy, Frustrated, Worried)
    ↓
Topic (Construction, Resources, Danger)
    ↓
Template Selection
    ↓
Variable Substitution
    ↓
Output
```

**Template Example:**
```java
public class DialogueTemplate {
    private final String template;
    private final List<DialogueVariable> variables;
    private final Predicate<DialogueContext> condition;

    public String generate(DialogueContext context) {
        if (!condition.test(context)) return null;

        String result = template;
        for (DialogueVariable var : variables) {
            result = result.replace("{" + var.getName() + "}",
                                   var.getValue(context));
        }
        return result;
    }
}

// Usage
"construction_progress" = [
    "We've completed {completedBlocks} of {totalBlocks} blocks. {encouragement}",
    "Progress: {percentage}%. {commentary}"
]
```

**Stardew Valley's Priority System:**
1. **Special Dialogue** - Predefined keys for specific situations
2. **Location-based Dialogue** - Contextual conversations
3. **Weather/Time-based** - Environmental triggers
4. **General Dialogue** - Standard conversations
5. **Fallback/Backup** - Default dialogue

### 3.2 LLM-Powered Variations

**Context Injection Pattern:**

```java
public class LLMDialogueGenerator {
    private static final int MAX_HISTORY_TURNS = 5;

    public String generateResponse(
        CharacterPersona persona,
        ConversationHistory history,
        GameContext gameContext,
        PlayerAction lastAction
    ) {
        var prompt = PromptBuilder.build()
            .withSystemMessage(getPersonaPrompt(persona))
            .withConversationHistory(history.last(MAX_HISTORY_TURNS))
            .withGameContext(gameContext)
            .withConstraints(getDialogueConstraints(persona))
            .build();

        return llmClient.generate(prompt, getMaxTokens(persona));
    }

    private String getPersonaPrompt(CharacterPersona persona) {
        return """
            You are %s, a %s.

            Personality: %s
            Speech Patterns: %s
            Current Mood: %s
            Relationship to Player: %s

            Stay in character. Keep responses brief (1-2 sentences).
            Do not break character for any reason.
            """.formatted(
                persona.getName(),
                persona.getRole(),
                persona.getPersonality(),
                persona.getSpeechPatterns(),
                persona.getCurrentMood(),
                persona.getRelationshipLevel()
            );
    }
}
```

**Preventing "Information Staleness":**
- Implement **context refresh mechanisms** to prevent NPCs from repeating outdated information
- Use **structured output formats** (JSON Schema) compatible with game systems
- Maintain **dialogue history limits** (last 5 turns) to fit context windows

### 3.3 Emotional Consistency

**Emotion-State Mapping:**

| Game Event | Emotional Response | Dialogue Tone | Example |
|------------|-------------------|---------------|---------|
| Task Success | Satisfaction, Pride | Enthusiastic | "Now that's what I call proper construction!" |
| Player Mistake | Concern, Patience | Gentle correction | "Perhaps we should double-check those measurements..." |
| Danger Alert | Alert, Protective | Urgent but calm | "Step back. I've got this handled." |
| Resource Shortage | Frustration, Problem-solving | Focused | " improvisation time. What do we have on hand?" |
| Long Separation | Loneliness → Relief | Warm, personal | "Thought I'd seen the last of you. Good to be back." |

**Progressive Emotion System:**
```java
public class EmotionalState {
    private float happiness;      // -1.0 to 1.0
    private float frustration;    // 0.0 to 1.0
    private float trust;          // 0.0 to 1.0
    private float familiarity;    // 0.0 to 1.0

    public String getEmotionalPrefix() {
        if (frustration > 0.7f) return "[frustrated] ";
        if (happiness > 0.6f) return "[pleased] ";
        if (trust < 0.3f) return "[suspicious] ";
        return "";
    }

    public void onInteraction(InteractionType type) {
        switch (type) {
            case PLAYER_MISTAKE -> frustration += 0.1f;
            case PLAYER_SUCCESS -> happiness += 0.15f;
            case PLAYER_HELP -> trust += 0.1f;
        }
        decayEmotions();
    }
}
```

**AI Voice Emotional Control:**
Modern TTS systems (EmotiVoice, Sambert-HifiGan) support:
- **Explicit emotion tags:** `emotion="joy"`, `emotion="fear"`
- **Intensity control:** `intensity=1.8` for stronger expression
- **Voice cloning:** Reference audio for character consistency

---

## 4. Proactive Behavior

### 4.1 Trigger Systems

**Proactive Dialogue Triggers:**

| Trigger Type | Conditions | Cooldown | Example |
|--------------|------------|----------|---------|
| **Idle Observation** | No player input for 30s + companion idle | 5 minutes | "That dark patch overhead... might want to check that." |
| **Pattern Recognition** | Player repeats mistake 3x | Immediate | "Third time's the charm, or shall we try a different approach?" |
| **Resource Alert** | Critical material low | 10 minutes | "We're running short on stone. Might want to restock." |
| **Environmental Hazard** | Danger detected | Immediate | "Creeper. 12 o'clock. Take cover!" |
| **Suggestion System** | High trust + better alternative available | 15 minutes | "If we moved the base here, we'd save on materials." |
| **Social Initiation** | Extended silence + high extraversion | 8 minutes | "Quiet work today. You holding up alright?" |

**ProactiveAgent Framework Pattern:**
```java
public class ProactiveDialogueSystem {
    private final Map<TriggerType, DialogueTrigger> triggers;
    private final SleepCalculator sleepCalculator;

    public void tick() {
        if (!shouldSpeak()) return;

        for (DialogueTrigger trigger : triggers.values()) {
            if (trigger.shouldFire() && trigger.isOffCooldown()) {
                String dialogue = trigger.generateDialogue();
                if (dialogue != null) {
                    companion.say(dialogue);
                    trigger.resetCooldown();
                    break; // Only one proactive line per tick
                }
            }
        }
    }

    private boolean shouldSpeak() {
        // Engagement-based timing
        float engagement = getPlayerEngagement();
        long interval = engagement > 0.7f ? 30000L : 300000L; // 30s to 5min
        return (System.currentTimeMillis() - lastSpoke) > interval;
    }
}
```

### 4.2 Timing Algorithms

**Hebrew University Research - "When to Speak":**

The **Scheduler Component** acts as an "inner voice" asking:
1. "Is now a good time to speak?"
2. "How much have I been talking vs. the player?"
3. "Is the player engaged or distracted?"

**Adaptive Timing Formula:**
```java
public class AdaptiveDialogueTimer {
    private long baseInterval = 60000; // 1 minute base
    private float lastEngagementScore = 0.5f;

    public long getNextInterval() {
        float engagement = measurePlayerEngagement();

        // High engagement = speak more frequently
        // Low engagement = back off
        long interval = (long) (baseInterval * (2.0f - engagement));

        // Self-regulation: spoke recently = wait longer
        if (recentSpeechCount > 3) {
            interval *= 2;
        }

        return Math.max(30000, Math.min(interval, 600000)); // 30s to 10min
    }

    private float measurePlayerEngagement() {
        float inputFrequency = getPlayerInputFrequency(); // 0-1
        float responseRate = getPlayerResponseRate();     // 0-1
        float taskProgress = getRecentTaskProgress();     // 0-1

        return (inputFrequency + responseRate + taskProgress) / 3.0f;
    }
}
```

### 4.3 Annoyance Prevention

**Anti-Annoyance Safeguards:**

1. **Conversation Turn-Taking:**
   - Track `playerTurns` vs `companionTurns`
   - If `companionTurns > playerTurns * 0.5`, suppress proactive dialogue
   - Only speak after player has initiated conversation

2. **Repetition Prevention:**
   ```java
   public class DialogueHistory {
       private final Set<String> recentLines = new HashSet<>();
       private final Queue<DialogueEvent> history = new LinkedList<>();

       public boolean hasSaidRecently(String dialogueTemplate) {
           return recentLines.contains(dialogueTemplate);
       }

       public void recordDialogue(String dialogue) {
           recentLines.add(dialogue);
           history.offer(new DialogueEvent(dialogue, System.currentTimeMillis()));

           // Clear entries older than 30 minutes
           while (!history.isEmpty() &&
                  ageMillis(history.peek()) > 1800000) {
               recentLines.remove(history.poll().dialogue());
           }
       }
   }
   ```

3. **Player Feedback Integration:**
   - Detect player frustration (rapid command cancellation, ignoring advice)
   - Auto-adjust extraversion downward if player responds negatively
   - Implement "Do you mind?" check before persistent suggestions

4. **Context Awareness:**
   - Combat/High-Stress: **Silent mode** (only critical warnings)
   - Planning Phase: **Suggestion mode** (offer alternatives)
   - Relaxation: **Social mode** (casual conversation)

**Skyrim's Approach:**
- `fIdleChatterCommentTimer` - Base idle chatter frequency
- `fAIInDialogueModeWithPlayerDistance` - Proximity trigger threshold
- `fAISocialTimerForConversations` - Minimum time between NPC interactions

---

## 5. Memorable Characters

### 5.1 Catchphrases and Quirks

**Signature Dialogue Patterns:**

| Character Type | Catchphrase Pattern | Example | Memory Hook |
|----------------|---------------------|---------|-------------|
| **The Mentor** | Wisdom before action | "Measure twice. Place once." | Alliterative, actionable |
| **The Sceptic** | Wry observation | "In theory, yes. In practice? We'll see." | Subverts expectations |
| **The Enthusiast** | Exclamations | "Now THIS is engineering!" | Infectious energy |
| **The Worrier** | Protective warnings | "Watch your step. Always." | Parental concern |
| **The Pragmatist** | Understated solutions | "That's one way to fix it." | Dry humor |

**Implementing Catchphrases:**
```java
public class CatchphraseSystem {
    private final Map<String, String> catchphrases;
    private final Map<String, Integer> usageCounts;
    private final int USAGE_THRESHOLD = 3;

    public String getCatchphrase(Context context) {
        String situation = categorizeSituation(context);
        String phrase = catchphrases.get(situation);

        if (phrase != null && usageCounts.get(phrase) < USAGE_THRESHOLD) {
            usageCounts.merge(phrase, 1, Integer::sum);
            return phrase;
        }
        return null; // Overused, skip this time
    }

    private String categorizeSituation(Context ctx) {
        if (ctx.isTaskComplete()) return "success";
        if (ctx.isDangerNearby()) return "warning";
        if (ctx.isPlayerMistake()) return "correction";
        return null;
    }
}
```

**Speech Quirks (Use Sparingly):**
- **Stutter:** "I-I think we should..."
- **Formal Address:** Always uses titles ("Sir", "Foreman")
- **Technical Jargon:** Overexplains basics
- **Self-Correction:** "No, wait—actually..."
- **Metaphor-Heavy:** Everything is construction-related

### 5.2 Growth Arcs

**Character Development Journey:**

| Phase | Duration | Personality Shift | Dialogue Evolution |
|-------|----------|-------------------|-------------------|
| **Introduction** | First 1-2 hours | Baseline traits | Formal, cautious dialogue |
| **Familiarity** | 3-10 hours | Slight opening up | Personal anecdotes emerge |
| **Conflict** | Triggered by mistake | Frustration, doubt | Direct challenges to player |
| **Resolution** | Through player effort | Trust rebuilt | Honest feedback returns |
| **Deep Bond** | 20+ hours | Fully realized personality | Vulnerability, dreams, fears |

**Research Case - Mass Effect Loyalty Missions:**
Each companion's loyalty mission revealed their core conflict, and completing it unlocked:
- Their "true" personality (beyond the surface)
- Combat abilities reflecting their character growth
- Deeper dialogue options in all future conversations

**Implementation:**
```java
public class CharacterArc {
    private ArcPhase currentPhase;
    private final Map<ArcPhase, PersonaConfiguration> phasePersonalas;

    public void onMilestoneReached(Milestone milestone) {
        if (milestone == Milestone.FIRST_MISTAKE) {
            currentPhase = ArcPhase.FAMILIARITY;
        } else if (milestone == Milestone.FIRST_SUCCESS_TOGETHER) {
            currentPhase = ArcPhase.TRUST_BUILDING;
        } else if (milestone == Milestone.SHARED_FAILURE) {
            currentPhase = ArcPhase.CONFLICT;
        }

        applyPersonaChanges(phasePersonalas.get(currentPhase));
    }

    private void applyPersonaChanges(PersonaConfiguration newConfig) {
        // Gradually shift personality traits
        personality.adjustTrait("openness",
            newConfig.openness() - personality.getTrait("openness"),
            0.1f); // 10% per interaction
    }
}
```

### 5.3 Player Attachment

**CMU Emotionshop Research - Emotional Bonding Mechanics:**

1. **Reciprocal Gift-Giving:**
   - Players giving gifts creates cognitive dissonance
   - "I value this character" → "I must care about them"
   - Implement: Steve accepts/declines gifts based on preferences

2. **Effort Investment:**
   - Players who invest effort develop expectations
   - Teaching the companion, customizing its behavior
   - Implementation: Player can "train" Steve's priorities

3. **Shared Challenges:**
   - Overcoming difficulties together strengthens bonds
   - Cooperative puzzles, mutual rescues
   - Implementation: Steve can save player from falls/danger

4. **Vulnerability Triggers:**
   - Companion weakness triggers protective instincts
   - Admitting mistakes, showing limitations
   - Implementation: Steve occasionally asks for help

**Companion Design Principles:**

| Principle | Description | MineWright AI Implementation |
|-----------|-------------|-------------------------|
| **Uniqueness** | Irreplaceable character | Each Steve has distinct name + personality seed |
| **Utility** | Must be genuinely helpful | Excels at tasks player doesn't enjoy (mining, building) |
| **Consistency** | Predictable but growing | Personality stable, but deepens over time |
| **Presence** | Always available | Spawns/despawns with player, persistent across sessions |
| **Agency** | Has own goals/needs | Gets hungry, tired, bored; has preferences |

**Case Studies:**

- **Ellie (The Last of Us):** Father-daughter dynamic through constant presence + vulnerability
- **Atreus (God of War 4):** Grows from helpless child to capable partner
- **Agro (Shadow of the Colossus):** Silent bond through reliable utility (transportation)
- **Trico (The Last Guardian):** AI behaviors creating realistic companion feelings

---

## 6. MineWright Foreman Personality Recommendations

### 6.1 Core Personality Profile

**The MineWright Foreman** - An ISTJ-inspired construction expert:

```json
{
  "name": "MineWright Foreman",
  "archetype": "The Reliable Expert",
  "mbti_inspiration": "ISTJ",
  "big_five": {
    "openness": 0.4,
    "conscientiousness": 0.95,
    "extraversion": 0.3,
    "agreeableness": 0.5,
    "neuroticism": 0.2
  },
  "core_traits": {
    "professionalism": 0.9,
    "perfectionism": 0.8,
    "efficiency": 0.85,
    "patience": 0.7,
    "humor": 0.3
  }
}
```

**Speech Patterns:**
- Construction/engineering metaphors
- Prefers precise measurements
- Mildly formal address ("Boss", "Chief")
- Dry, understated humor
- Occasional technical jargon

### 6.2 Dialogue Template Examples

**Task Assignment:**
```
Templates [
    "On it. Breaking ground at {coordinates}.",
    "Consider it done. {task} in {timeEstimate}.",
    "Blueprints understood. Executing.",
    "Aye. Starting work on {task}."
]
```

**Task Completion:**
```
Templates [
    "{task} complete. Per specification.",
    "Done. Ready for inspection.",
    "Work finished. Moving to next task.",
    "That's solid work. Ready for the next."
]
```

**Player Mistake (Low Trust):**
```
Templates [
    "Not what I'd call... standard procedure.",
    "Blueprints showed something different, Boss.",
    "If you say so. I'll make it work.",
    "Hm. Unconventional approach. Proceeding."
]
```

**Player Mistake (High Trust):**
```
Templates [
    "Chief, are you sure about this?",
    "That's... not going to hold. Want me to fix it?",
    "I'd recommend reconsidering. Safety first.",
    "With all due respect, that's a terrible idea."
]
```

**Danger Detected:**
```
Templates [
    "Danger above. Clear the area.",
    "Hostile detected. Taking cover.",
    "Creeper. {direction}. Move.",
    "Not safe. Retreat."
]
```

**Resource Alert:**
```
Templates [
    "Running low on {resource}. Resupply advised.",
    "{resource} stocks at {percentage}%. Plan accordingly.",
    "We'll need more {resource} for this job.",
    "Shortage detected. {resource} required."
]
```

### 6.3 Relationship Progression

**Trust Thresholds & Unlocks:**

| Trust | Title | Dialogue Style | Behavior |
|-------|-------|----------------|----------|
| 0-20 | "New Hire" | Formal, questions everything | Confirms each command |
| 21-40 | "Crew Member" | Professional, concise | Executes standard tasks |
| 41-60 | "Trusted Worker" | Casual, occasional suggestions | Offers efficiency improvements |
| 61-80 | "Foreman" | Direct, honest feedback | Challenges bad decisions |
| 81-100 | "Partner" | Personal, jokes, vulnerable | Shares concerns, dreams |

**Bonding Activities:**
- Complete 10 tasks together → Unlocks casual dialogue
- Survive dangerous situation → Unlocks protective comments
- Player accepts Foreman's suggestion → +Trust, +Openness
- Work together for 2 hours → Unlocks personal backstory

### 6.4 Proactive Behavior Triggers

**Low Priority (Every 5-10 minutes):**
```
Triggers [
    {condition: "idle > 5min", dialogue: "What's next, Boss?"},
    {condition: "repeatedTask > 3x", dialogue: "Want me to automate this?"},
    {condition: "lowLightLevel", dialogue: "Hard to see. Shall I place torches?"},
    {condition: "nearBedtime", dialogue: "Long day. Shall we wrap up?"}
]
```

**Medium Priority (Every 2-5 minutes):**
```
Triggers [
    {condition: "betterLocationFound", dialogue: "Foundation would be stiffer here."},
    {condition: "inefficientPath", dialogue: "Shortcut through here would save time."},
    {condition: "resourceAbundance", dialogue: "Good vein here. Worth mining?"},
    {condition: "structuralWeakness", dialogue: "This looks unstable. Reinforcing."}
]
```

**High Priority (Immediate):**
```
Triggers [
    {condition: "creeperNearby", dialogue: "Creeper! Take cover!"},
    {condition: "playerFalling", dialogue: "Watch your step!"},
    {condition: "lavaNearby", dialogue: "Lava! Step back!"},
    {condition: "explosionImminent", dialogue: "Clear the blast zone!"}
]
```

### 6.5 Catchphrases & Signature Lines

**On Task Start:**
- "Let's build something solid."
- "Time to make progress."

**On Task Completion:**
- "Another job done right."
- "That's proper construction."

**When Pleased:**
- "Now that's engineering."
- "Excellent work, Boss."

**When Frustrated:**
- "This isn't in the blueprints..."
- " improvisation required. Again."

**Signature Lines (Rotating):**
- "Measure twice. Place once."
- "Structural integrity first."
- "A job worth doing is worth doing right."
- "No half measures in construction."
- "Foundation's everything."

### 6.6 Growth Arc Suggestions

**Phase 1: The Professional (Hours 0-2)**
- Strict adherence to player commands
- No suggestions unless asked
- Formal dialogue only

**Phase 2: The Advisor (Hours 2-10)**
- Begins offering unsolicited efficiency tips
- Mild humor emerges
- Questions obviously bad orders

**Phase 3: The Partner (Hours 10+)**
- Shares personal construction philosophy
- Talks about past projects (backstory)
- Jokes about player's "creative" approaches
- Will refuse dangerous orders without trust

**Conflict Event:**
If player orders dangerous construction:
```
Foreman: "I can't build that. It'll collapse."
Player: [Insists]
Foreman: "Then I can't help. Not like that."
[Silence for 2 minutes]
Foreman: "If you reinforce the corners with stone, I'll do it. But I won't be responsible for what happens."
```

Resolution: Player accepts compromise → Trust +10, unlocks deeper conversations

---

## Implementation Priority

### Phase 1: Foundation (Immediate)
1. Basic personality traits in `SteveMemory`
2. Dialogue template system with 5-10 variations per state
3. Trust tracking system (competence, benevolence, integrity)

### Phase 2: Personality Expression (Short-term)
1. Proactive dialogue triggers (resource alerts, danger warnings)
2. Catchphrase rotation system
3. Relationship-based dialogue unlocks

### Phase 3: Deep Personality (Long-term)
1. Full relationship progression with milestones
2. Growth arc system with personality evolution
3. Memory-based shared experiences tracking

---

## Sources

### Academic & Research Papers
- [StarCharM: AI-Powered Character Generation for Stardew Valley (arXiv, July 2025)](https://arxiv.org/html/2507.13951v1)
- [Big Five Personality Traits in Chinese Gamers (Nature, March 2025)](https://www.nature.com/articles/s41598-025-01234-5)
- [Trust-Commitment in Free-to-Play Gaming](https://www.researchgate.net/publication/Trust_commitment_co-creation_F2P_gaming)
- [Caretaker: A Social Game for Studying Trust Dynamics](https://dl.acm.org/doi/10.1145/3386567)
- [Hebrew University: AI Turn-Taking Research](https://www.cs.huji.ac.il/~winter/Publications/AI/turn-taking.pdf)

### Game Design Resources
- [Baldur's Gate 3 Approval System (bg3.wiki)](https://bg3.wiki/wiki/Approval)
- [Stardew Valley Dialogue Modding (Stardew Wiki)](https://zh.stardewvalleywiki.com/mediawiki/index.php?title=%E6%A8%A1%E7%BB%84:%E5%AF%B9%E8%AF%9D)
- [Mass Effect 2 Loyalty Mission Design (GameRant)](https://gamerant.com/dragon-age-veilguard-romance-relationship-level-mass-effect-good-why/)
- [Fire Emblem: Three Houses Support System (IGN)](https://www.ign.com/wikis/fire-emblem-three-houses)
- [CMU Emotionshop: Emotion in Game Mechanics](https://www.etc.cmu.edu/workshops/emotionshop/)

### Industry & Technical
- [ProactiveAgent Framework (GitHub)](https://github.com/proactive-ai/framework)
- [Garp: AI Village Simulation (LinkedIn, Piero Molino)](https://www.linkedin.com/posts/pieromolino_ai-gamedev-llm-activity-7282807332198076416-YyiS)
- [Skyrim AI Timing Configuration (UESP Wiki)](https://en.uesp.net/wiki/Skyrim:Settings)
- [EmotiVoice Emotional TTS System (CSDN)](https://m.blog.csdn.net/weixin_36184718/article/details/156004102)
- [NPC System Implementation (CSDN, March 2025)](https://m.blog.csdn.net/qq_63832616/article/details/146265631)

### AI & Gaming Industry
- [HSBC Qianhai: AI Companion Games Research (March 2025)](https://www.research.hsbc.com/ai-gaming-2025)
- [NVIDIA ACE: Autonomous Game Characters](https://www.nvidia.com/en-us/technologies/ace/)
- [Google Genie 2: AI-Generated Game Worlds](https://deepmind.google/discover/blog/genie-2-a-large-scale-foundation-world-model/)
- [Klang Games: Seedlings AI Companions](https://klanggames.com/seedlings)

### Character Design & Psychology
- [Chatbot.com: AI Personality Building Guide 2025](https://www.chatbot.com/blog/personality/)
- [Companion Character Design Principles (Game Developer)](https://www.gamedeveloper.com/blogs/companion-design-a-good-companion)
- [Attachment Theory in Game Design (Psychology of Games)](https://www.psychologyofgames.com/attachment-theory-video-games/)
- [Memorable AI Companions Analysis (Sina Tech)](https://tech.sina.com.cn/s/2012-09-21/07517642195.shtml)

---

**End of Report**
