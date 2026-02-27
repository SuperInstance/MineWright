# Catchphrase Systems for AI Companions

*Research compilation for MineWright construction AI companions*

---

## Table of Contents
1. [Psychological Principles of Memorable Phrases](#psychological-principles)
2. [Linguistic Patterns & Rhetorical Devices](#linguistic-patterns)
3. [TV Show Catchphrase Analysis](#tv-show-analysis)
4. [Situational Phrase Selection Algorithms](#selection-algorithms)
5. [Preventing Overuse & Staleness](#preventing-overuse)
6. [Callback & Referential Phrases](#callback-phrases)
7. [Construction-Themed Phrase Banks](construction-phrase-banks)
8. [Implementation Recommendations](#implementation)

---

## Psychological Principles of Memorable Phrases {#psychological-principles}

### The Memorability Formula

Research from the arXiv paper "You had me at hello: How phrasing affects memorability" identifies four key factors:

1. **Lexical Distinctiveness** - Use uncommon word choices that stand out
2. **Common Syntactic Patterns** - Build on familiar grammatical structures
3. **Portability** - Phrases general enough to apply in new contexts
4. **Phonetic Appeal** - Rhythmic patterns, alliteration, and sound devices

### Cognitive Hooks

| Hook Type | Description | Example |
|-----------|-------------|---------|
| **Pattern Interrupt** | Breaks expected speech patterns | "D'oh!" instead of "Oh no" |
| **Emotional Resonance** | Taps into universal feelings | "We were on a break!" |
| **Character Consistency** | Always delivered the same way | "How **YOU** doin'?" |
| **Situational Perfect Fit** | Exactly matches the moment | "That's one small step..." |

### Phonetic Characteristics

- **Rhythmic Stress**: Trochaic (DA-da da-DA) patterns are most memorable
- **Vowel Elongation**: Stressed vowels draw attention ("Danger ZOOOONE")
- **Consonant Repetition**: Alliteration and consonance create musicality
- **Intonation Contours**: Consistent pitch patterns become recognizably "the character"

---

## Linguistic Patterns & Rhetorical Devices {#linguistic-patterns}

### Rhythm & Meter

```
Trochaic (Strong-Weak):  "DAN-ger ZONE"      ✓ Most memorable
Iambic (Weak-Strong):    "The ONLY way"       ✓ Very memorable
Spondaic (Strong-Strong): "GO NOW"            ⚠ Urgent, less catchy
Anapestic (Weak-Weak-Strong): "I'm on my WAY" ✓ Flowing
```

### Sound Devices

| Device | Pattern | Example |
|--------|---------|---------|
| **Alliteration** | Initial consonant repetition | "Bold Builders Build Better" |
| **Assonance** | Vowel repetition | "Stone and Bone" |
| **Consonance** | Final consonant repetition | "Back to the block" |
| **Rhyme** | End sound matching | "Ready, steady, go!" |
| **Parallelism** | Grammatical repetition | "Measure twice, cut once" |

### Structural Patterns

1. **Tripartite Structure**: Lists of three are cognitively satisfying
   - "Measure, mark, and make it happen"
   - "Pick, place, and polish"

2. **Antithesis**: Contrasting ideas create memorability
   - "Break it down to build it up"
   - "From chaos comes construction"

3. **Ellipsis**: Omitting words creates efficiency
   - "Clear on." instead of "Is the area clear?"
   - "Stacking." instead of "I am stacking blocks"

---

## TV Show Catchphrase Analysis {#tv-show-analysis}

### The Simpsons

| Catchphrase | Character | Phonetic Feature | Usage Pattern |
|-------------|-----------|------------------|---------------|
| "D'oh!" | Homer | Glottal stop + falling intonation | Frustration/error |
| "Excellent..." | Mr. Burns | Elongated, sinister delivery | Plotting/malice |
| "Ha-ha!" | Nelson | Disyllabic laugh, mocking | Schadenfreude |

**Success Factors:**
- Short (1-2 syllables)
- Emotionally specific
- Can be modified ("D'oh!" → "D'oh-eth!" for Shakespearean variant)
- Culturally ubiquitous (added to Oxford English Dictionary)

### Friends

| Catchphrase | Character | Delivery Pattern | Context |
|-------------|-----------|------------------|---------|
| "How YOU doin'?" | Joey | Stress on YOU, dropped 'g' | Flirting/Introduction |
| "I KNOW!" | Monica | High pitch, emphatic | Agreement/Vindication |
| "We were on a break!" | Ross | Defensive, elongated | Relationship conflict |

**Success Factors:**
- Character personality embodiment
- Consistent delivery (intonation, timing)
- Situational specificity (not overused)
- Often involves subverting expectations

### Archer

| Catchphrase | Context | Rhythm | Pattern |
|-------------|---------|--------|---------|
| "Danger Zone" | Entering risky situations | DA-da da-DA | Song reference/callback |
| "Phrasing!" | Innocent double entendre | DA-DA! | Calling attention to accidental innuendo |

**Success Factors:**
- Pop culture references create shared knowledge
- Meta-commentary on situations
- Repetition creates anticipation
- Community-building (other characters eventually use them)

### Key Insights for AI Companions

1. **Establish delivery patterns** first, then vary the content
2. **Short is better** - 1-5 words maximum for core catchphrases
3. **Emotion-specific** phrases prevent overuse
4. **Reference-able** phrases can evolve into callbacks
5. **Personality-first** - catchphrases emerge from character, not vice versa

---

## Situational Phrase Selection Algorithms {#selection-algorithms}

### Context-Aware Selection System

```java
public class CatchphraseSelector {

    // Priority levels for phrase selection
    enum Priority {
        CRITICAL,    // Danger, errors
        HIGH,        // Task completion, milestones
        MEDIUM,      // Coordination, status updates
        LOW,         // Idle chatter, personality
        RARE         // Easter eggs, special occasions
    }

    // Selection factors
    class SelectionScore {
        double contextMatch;      // How well does phrase fit situation?
        double recencyPenalty;    // How recently was this phrase used?
        double personalityFit;    // Does this match character archetype?
        double playerFatigue;     // Has player heard this too much?
        double varietyBonus;      // Reward for using less-common phrases
    }

    Phrase selectBestPhrase(Situation context) {
        List<Phrase> candidates = phraseBank.getByContext(context);

        return candidates.stream()
            .map(p -> scorePhrase(p, context))
            .max(Comparator.comparing(SelectionScore::getTotal))
            .map(SelectionScore::getPhrase)
            .orElse(null);
    }
}
```

### Multi-Factor Scoring

```
TotalScore = (ContextMatch × 2.5)
           + (PersonalityFit × 2.0)
           + (VarietyBonus × 1.5)
           - (RecencyPenalty × 2.0)
           - (PlayerFatigue × 1.0)
```

### Context Trigger Mapping

| Situation Type | Trigger Condition | Phrase Priority | Examples |
|----------------|-------------------|-----------------|----------|
| **Error/Failure** | Action failed, block broken | CRITICAL | "Meant to do that", "Rookie mistake!", "Back to the drawing board" |
| **Danger** | Health low, hostile nearby | CRITICAL | "Tight squeeze!", "Watch my back!", "Holding ground!" |
| **Success/Complete** | Task finished, structure done | HIGH | "Another beauty!", "Solid work!", "That's the spirit!" |
| **Coordination** | Working with others | MEDIUM | "On your mark", "I've got this section", "Syncing up" |
| **Idle/Ambient** | Waiting, wandering | LOW | Humming, tool sounds, casual observations |
| **Special** | First time, rare event | RARE | "Opening ceremony", "Job's well done" (on completion) |

### Timing Rules

```java
class TimingRules {
    Duration MINIMUM_TIME_BETWEEN_SAME_PHRASE = 5.minutes();
    Duration MINIMUM_TIME_BETWEEN_ANY_CATCHPHRASE = 30.seconds();
    Duration COOLDOWN_AFTER_CRITICAL = 1.minute();  // After danger/error

    boolean shouldSpeak(Phrase phrase, Situation context) {
        if (wasSpokenRecently(phrase, MINIMUM_TIME_BETWEEN_SAME_PHRASE))
            return false;
        if (lastCriticalEventTime.elapsed() < COOLDOWN_AFTER_CRITICAL
            && context.priority != Priority.CRITICAL)
            return false;
        if (playerFatigue > 0.8)  // Player is tired of this phrase
            return false;
        return true;
    }
}
```

---

## Preventing Overuse & Staleness {#preventing-overuse}

### The Shuffle-Reshuffle Pattern

From game dialogue best practices:

```java
class PhrasePool {
    List<Phrase> pool;           // All available phrases
    List<Phrase> currentBatch;   // Current shuffled batch
    int currentIndex = 0;

    Phrase getNext() {
        if (currentIndex >= currentBatch.size()) {
            // Reshuffle when batch exhausted
            currentBatch = shuffle(pool);
            currentIndex = 0;

            // IMPORTANT: Ensure new batch doesn't start with
            // the last phrase from previous batch
            if (currentBatch.get(0) == lastSpokenPhrase) {
                swap(currentBatch, 0, randomIndex());
            }
        }

        return currentBatch.get(currentIndex++);
    }
}
```

### Fatigue Tracking

```java
class PhraseFatigue {
    Map<Phrase, Double> fatigueScores = new HashMap<>();
    double FATIGUE_INCREMENT = 0.15;     // +15% per use
    double FATIGUE_DECAY_PER_MINUTE = 0.05;  // -5% per minute
    double FATIGUE_THRESHOLD = 0.8;       // Retire phrase at 80%

    void recordUsage(Phrase phrase) {
        double current = fatigueScores.getOrDefault(phrase, 0.0);
        fatigueScores.put(phrase, min(1.0, current + FATIGUE_INCREMENT));
    }

    void decayFatigue() {
        fatigueScores.replaceAll((p, score) ->
            max(0.0, score - FATIGUE_DECAY_PER_MINUTE));
    }

    boolean isPhrasePlayable(Phrase phrase) {
        return fatigueScores.getOrDefault(phrase, 0.0) < FATIGUE_THRESHOLD;
    }
}
```

### Variety Strategies

1. **Tiered Pools**: Separate phrases into common/uncommon/rare
   ```
   Common (60%):  "On it!", "Got this!", "Way to go!"
   Uncommon (30%): "Building dreams!", "One brick at a time!"
   Rare (10%): "Monumental moments!", "History in the making!"
   ```

2. **Dynamic Rotation**: Phrase availability changes over time
   ```java
   Set<Phrase> activePool = new HashSet<>();
   void rotatePool() {
       // Deactivate some phrases, activate others
       if (random() < 0.1) {
           Phrase toDeactivate = randomFrom(activePool);
           Phrase toActivate = randomFrom(inactivePool);
           activePool.remove(toDeactivate);
           activePool.add(toActivate);
       }
   }
   ```

3. **Cooldown Categories**: Prevent same-type phrases in sequence
   ```java
   enum PhraseCategory {
       GREETING, WORKING, CELEBRATION, ERROR, COORDINATION
   }

   boolean canUseCategory(PhraseCategory category) {
       return lastCategory != category;  // Simple alternation
   }
   ```

### Frequency Capping

```java
class FrequencyCap {
    Map<Phrase, Integer> usageCount = new ConcurrentHashMap<>();
    Map<Phrase, Integer> maxUsesPerSession = new HashMap<>();

    // Set different caps for different phrase types
    void configureCaps() {
        maxUsesPerSession.put("That's the spirit!", 3);   // Low cap
        maxUsesPerSession.put("On it!", 10);              // High cap
        maxUsesPerSession.put("Meant to do that!", 5);    // Medium cap
    }

    boolean canUsePhrase(Phrase phrase) {
        int used = usageCount.getOrDefault(phrase, 0);
        int max = maxUsesPerSession.getOrDefault(phrase, 5);
        return used < max;
    }
}
```

---

## Callback & Referential Phrases {#callback-phrases}

### What Makes a Callback Work

A callback phrase references an earlier event, joke, or memorable moment. It works because:

1. **Shared History**: Player and AI have experienced something together
2. **Pattern Recognition**: Brain enjoys recognizing connections
3. **Reward Loop**: Inside jokes create exclusivity and bonding
4. **Character Development**: Shows the AI "remembers"

### Callback System Architecture

```java
class CallbackSystem {

    // Memory of significant events
    class Memory {
        String eventId;
        LocalDateTime timestamp;
        String description;
        List<Phrase> associatedPhrases;
        int significance;  // 1-10, how memorable?
    }

    // Events worth remembering
    void recordEvent(String eventId, int significance) {
        if (significance >= 5) {
            memories.add(new Memory(eventId, now(), significance));
        }
    }

    // Generate callback when context matches past event
    Optional<Phrase> tryCallback(Situation current) {
        for (Memory m : memories) {
            if (isContextuallyRelevant(m, current)) {
                double probability = calculateCallbackProbability(m);
                if (random() < probability) {
                    return getCallbackPhrase(m);
                }
            }
        }
        return Optional.empty();
    }

    double calculateCallbackProbability(Memory m) {
        // More significant = more likely to callback
        // More recent = more likely to callback
        double significanceFactor = m.significance / 10.0;
        double recencyFactor = exp(-daysSince(m.timestamp) / 30.0);
        return significanceFactor * recencyFactor * 0.3;  // Max 30% chance
    }
}
```

### Callback Patterns

| Pattern | Description | Example |
|---------|-------------|---------|
| **Direct Reference** | Quote from earlier event | "Remember that creeper? Good times." |
| **Evolution** | Phrase evolves over time | "Meant to do that!" → "Totally meant to do that!" → "Definitely meant to do that!" |
| **Accumulation** | Build on previous callbacks | "Not again." → "Not again again." → "Not again again again." |
| **Contrast** | Reference shows growth | "Used to take me all day for this" (after building quickly) |
| **Meta-Commentary** | Acknowledge repetition | "I've got a bad feeling abo— oh wait, different game" |

### Implementation Example: Construction-Themed Callbacks

```java
class ConstructionCallbacks {

    // Event: First block placed
    void onFirstBlockPlaced() {
        memory.record("first_block", 7);
        say("And so it begins!");
    }

    // Callback later when placing final block
    void onFinalBlockPlaced() {
        if (memory.has("first_block") && random() < 0.4) {
            say("And so it concludes! From that first block to this.");
        }
    }

    // Event: Major mistake
    void onMajorMistake() {
        memory.record("epic_fail_" + uniqueId(), 8);
        say("Architecture is 90% failure, 10% genius!");
    }

    // Callback when similar situation arises
    void onRiskySituation() {
        if (memory.hasRecent("epic_fail") && random() < 0.3) {
            say("Let's not repeat that incident, shall we?");
        }
    }

    // Event: Completing large project
    void onProjectComplete(int blocksPlaced) {
        memory.record("project_" + projectId(), 9);
        say("Another job well done!");
    }

    // Callback when starting similar project
    void onProjectStart() {
        if (memory.has("project") && random() < 0.25) {
            say("Round two! Let's make this one even better.");
        }
    }
}
```

### Referential Humor Categories

1. **Self-Referential**: Acknowledging the game/AI nature
   - "My code would be so proud right now"
   - "If I had a coffee break, I'd take it"

2. **Cross-Character**: Referencing other AI personalities
   - "Builder Bob would be impressed"
   - "Even miner Mike couldn't have done that faster"

3. **Player References**: Remembering player actions
   - "Unlike SOMEONE who fell off that ledge..."
   - "Learned from the best, I have"

4. **Lore References**: Game world callbacks
   - "Steve himself couldn't build better"
   - "Creeping it real since spawn"

---

## Construction-Themed Phrase Banks {#construction-phrase-banks}

### Primary Catchphrases (Personality-Defining)

These phrases should be used sparingly (2-3 times per session max) and define the character:

```
Archetype: The Optimistic Builder
- "Another brick, another beautiful brick!"
- "Building dreams, block by block!"
- "The foundation of greatness!"

Archetype: The Pragmatic Worker
- "Measure twice, place once."
- "Get it right, not just done."
- "Quality first, always."

Archetype: The Enthusiastic Rookie
- "This is gonna be amazing!"
- "Watch and learn!"
- "I was born to build!"

Archetype: The Grumpy Veteran
- "Don't just stand there, help me stack."
- "Nothing like honest labor."
- "Could be worse. Could be mining."
```

### Situational Phrase Banks

#### Error / Failure Situations

```
Minor Errors (broke wrong block, small mistake)
- "Meant to do that."
- "Artistic interpretation!"
- "That's... not what I meant."
- "Nobody saw that."
- "Redo! Take two!"
- "Minor setback. Moving on."
- "Blueprints are more like guidelines anyway."

Major Errors (structure collapse, big mistake)
- "Back to the drawing board!"
- "So that's why we measure twice."
- "Nobody's perfect."
- "Learning experience!"
- "I'll... just fix that."
- "Physics, why have you forsaken me?"
- "Rookie mistake! (Even if I'm not a rookie)"

Repeated Errors (same mistake again)
- "Okay, stop laughing."
- "Not again!"
- "I usually don't do this, I swear."
- "The instructions were unclear!"
- "Third time's the charm?"
```

#### Danger / Hostile Situations

```
Creeper / Mob Approaching
- "Trouble brewing!"
- "We've got company!"
- "Hold the line!"
- "Not in my house!"
- "Secure the perimeter!"

Low Health
- "Taking some damage here!"
- "A little help?"
- "I've had better days."
- "That's gonna leave a mark."

Near Miss
- "That was close!"
- "Too close for comfort."
- "My heart just skipped a beat."
- "I felt that in my circuits."
```

#### Success / Completion Situations

```
Small Task Complete
- "One job well done."
- "Solid work."
- "That's the stuff."
- "Making progress."
- "Smooth sailing."

Section Complete
- "Section secured!"
- "That corner's solid."
- "Looking good!"
- "Coming together nicely."
- "Halfway there!"

Project Complete
- "Job's well done!"
- "Another masterpiece!"
- "Built to last!"
- "From nothing to something!"
- "We built this!"

Speed Completion
- "New personal best!"
- "Efficient and elegant."
- "Like I've done it a thousand times."
- "Nothing to it!"
```

#### Coordination / Teamwork Situations

```
Starting Joint Task
- "Let's build something great."
- "I've got your back."
- "Together we're stronger."
- "Divide and construct."

During Collaboration
- "On your mark!"
- "I've got this section."
- "Syncing up!"
- "Nice coordination!"
- "You take that side, I've got this."

Handing Off Task
- "Your turn to shine."
- "All yours, boss."
- "Taking the reins now?"
- "Show me how it's done."
```

#### Idle / Ambient Situations

```
Waiting / Idle
- "Hmm, blueprints look good."
- "Could use a coffee break."
- "Just admiring the view."
- *Whistling construction tune*
- "What should we build next?"

Observations
- "Nice day for building."
- "The blocks are stacking themselves today."
- "I love the smell of fresh cobble in the morning."
- "This spot has potential."

Thinking / Planning
- "Let me think..."
- "How to approach this..."
- "Running calculations..."
- "Blueprints loading..."
```

### Character-Specific Variations

#### The Master Builder
```
- "Observe and learn."
- "I don't place blocks, I create art."
- "Perfection is the standard."
- "Every block has its place."
- "Building since before the respawn."
```

#### The Enthusiastic Apprentice
```
- "I'm learning so much!"
- "Is this how the pros do it?"
- "Can I try the fancy stuff?"
- "One day I'll be the master!"
- "Every wall tells a story!"
```

#### The No-Nonsense Foreman
```
- "Less talking, more stacking."
- "Time is blocks, people."
- "Stay focused."
- "The job's not doing itself."
- "Safety third, efficiency first!"
```

#### The Zen Builder
```
- "One with the blocks."
- "The structure flows through me."
- "Patience builds better structures."
- "Find your balance."
- "Building meditation complete."
```

### Time-Based Variations

```
Morning (game dawn)
- "Early bird gets the build!"
- "Fresh start, fresh blocks."
- "Daybreak, break ground!"

Midday (game noon)
- "High noon, building boom."
- "Full steam ahead!"
- "Peak productivity time!"

Evening (game dusk)
- "Sun's setting, walls are rising."
- "Almost there, push through!"
- "Golden hour, golden structures!"

Night (game dark)
- "Building by torchlight!"
- "The night is young, and so are we."
- "Nothing like some overtime work!"
```

---

## Implementation Recommendations for MineWright {#implementation}

### Phase 1: Core Catchphrase System

```java
public class CatchphraseSystem {

    // Phrase data structure
    public class Phrase {
        String id;
        String text;
        PhraseCategory category;
        PhrasePriority priority;
        PersonalityArchetype archetype;

        // Usage tracking
        Instant lastUsed;
        int usageCount;
        double fatigueScore;

        // Context matching
        Set<TriggerCondition> triggers;

        // Delivery options
        Optional<String> pronunciationOverride;
        Optional<SoundEffect> soundEffect;
    }

    // Core selection logic
    public Optional<Phrase> selectPhrase(SituationContext context) {
        List<Phrase> candidates = phraseBank
            .findByContext(context)
            .stream()
            .filter(this::isContextuallyAppropriate)
            .filter(this::isNotFatigued)
            .filter(this::isWithinCooldown)
            .filter(this::matchesPersonality)
            .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // Score and select best
        return candidates.stream()
            .max(Comparator.comparing(p -> scorePhrase(p, context)));
    }

    // Speaking the phrase
    public void speakPhrase(Phrase phrase, SituationContext context) {
        // Update tracking
        phrase.lastUsed = Instant.now();
        phrase.usageCount++;
        updateFatigue(phrase);

        // Deliver to player (chat, sound, or both)
        if (config.showInChat()) {
            displayInChat(phrase, context);
        }

        if (config.playAudio()) {
            playAudio(phrase, context);
        }

        // Track for potential callbacks
        callbackSystem.trackUsage(phrase, context);
    }
}
```

### Phase 2: Dynamic Personality System

```java
public class PersonalityEngine {

    // Archetype definitions
    public enum PersonalityArchetype {
        MASTER_BUILDER(new PersonalityProfile()
            .catchphraseRate(0.3)      // Fewer, more impactful phrases
            .qualityFocus(0.8)
            .vocabularyLevel(Level.ADVANCED)),

        ENTHUSIASTIC_ROOKIE(new PersonalityProfile()
            .catchphraseRate(0.7)      // Talks a lot
            .excitement(0.9)
            .vocabularyLevel(Level.BASIC)),

        GRUMPY_FOREMAN(new PersonalityProfile()
            .catchphraseRate(0.4)
            .bluntness(0.8)
            .vocabularyLevel(Level.DIRECT)),

        ZEN_BUILDER(new PersonalityProfile()
            .catchphraseRate(0.2)      // Very sparse
            .peacefulness(0.9)
            .vocabularyLevel(Level.PHILOSOPHICAL));
    }

    // Phrase selection influenced by personality
    public List<Phrase> selectPhrasesForPersonality(
            List<Phrase> candidates,
            PersonalityArchetype archetype) {

        PersonalityProfile profile = archetype.getProfile();

        return candidates.stream()
            .filter(p -> matchesArchetype(p, archetype))
            .sorted((p1, p2) -> compareByProfile(
                p1, p2, profile))
            .limit(profile.maxPhraseBatchSize())
            .collect(Collectors.toList());
    }
}
```

### Phase 3: Callback & Memory System

```java
public class CallbackMemorySystem {

    // Significant event memory
    class SignificantEvent {
        String eventId;
        Instant timestamp;
        EventType type;
        int significance; // 1-10
        Map<String, Object> context;
    }

    // Record notable events
    public void recordEvent(EventType type, int significance,
            Map<String, Object> context) {
        if (significance >= 5) {
            SignificantEvent event = new SignificantEvent(
                generateEventId(),
                Instant.now(),
                type,
                significance,
                context
            );
            memory.put(event.eventId, event);
        }
    }

    // Try to generate callback
    public Optional<Phrase> tryGenerateCallback(
            SituationContext current) {

        // Find relevant past events
        List<SignificantEvent> relevant = memory.values().stream()
            .filter(e -> isRelevantToCurrent(e, current))
            .filter(e -> !wasRecentlyReferenced(e))
            .sorted(Comparator.comparing(
                e -> calculateRelevanceScore(e, current)))
            .limit(5)
            .collect(Collectors.toList());

        if (relevant.isEmpty()) {
            return Optional.empty();
        }

        // Weighted random selection based on significance
        SignificantEvent selected = weightedSelect(
            relevant,
            e -> e.significance);

        // Generate callback phrase
        return generateCallbackPhrase(selected, current);
    }

    // Callback generation
    private Phrase generateCallbackPhrase(
            SignificantEvent event,
            SituationContext current) {

        String template = selectCallbackTemplate(event, current);
        String phrase = fillTemplate(template, event, current);

        return new Phrase(
            "callback_" + event.eventId,
            phrase,
            PhraseCategory.CALLBACK,
            PhrasePriority.MEDIUM
        );
    }
}
```

### Phase 4: Audio & Pronunciation System

```java
public class CatchphraseAudioSystem {

    // Text-to-speech with personality
    public void speakWithPersonality(
            String text,
            PersonalityArchetype archetype,
            Phrase phrase) {

        AudioConfig config = new AudioConfig();

        // Base pitch based on archetype
        config.basePitch = switch (archetype) {
            case MASTER_BUILDER -> 1.0;    // Normal
            case ENTHUSIASTIC_ROOKIE -> 1.2; // High
            case GRUMPY_FOREMAN -> 0.8;   // Low
            case ZEN_BUILDER -> 0.9;      // Calm
        };

        // Speaking rate
        config.rate = switch (archetype) {
            case MASTER_BUILDER -> 1.0;
            case ENTHUSIASTIC_ROOKIE -> 1.3; // Fast
            case GRUMPY_FOREMAN -> 0.9;
            case ZEN_BUILDER -> 0.8;       // Slow
        };

        // Emphasis on key words
        config.emphasisMap = extractEmphasis(phrase);

        // Generate audio
        AudioClip audio = tts.generate(text, config);

        // Play with 3D positioning from AI entity
        audioEngine.play(audio, aiEntity.getPosition());
    }

    // Extract emphasis from phrase markup
    private Map<String, EmphasisLevel> extractEmphasis(
            Phrase phrase) {

        // Parse phrases like "How **YOU** doin'?"
        Map<String, EmphasisLevel> emphasis = new HashMap<>();

        String text = phrase.text;
        int boldStart = text.indexOf("**");
        while (boldStart != -1) {
            int boldEnd = text.indexOf("**", boldStart + 2);
            if (boldEnd != -1) {
                String word = text.substring(
                    boldStart + 2, boldEnd);
                emphasis.put(word, EmphasisLevel.STRONG);
                boldStart = text.indexOf("**", boldEnd + 2);
            }
        }

        return emphasis;
    }
}
```

### Configuration System

```java
// config/steve-common.toml additions
[catchphrases]
enabled = true
showInChat = true
playAudio = true
maxPhrasesPerMinute = 3

[catchphrases.fatigue]
fatiguePerUse = 0.15
decayPerMinute = 0.05
fatigueThreshold = 0.8

[catchphrases.personality]
defaultArchetype = "ENTHUSIASTIC_ROOKIE"
catchphraseRate = 0.5  # Base rate modifier

[catchphrases.callbacks]
enabled = true
memoryRetentionDays = 7
maxCallbacksPerSession = 5
callbackProbability = 0.25

[catchphrases.cooldowns]
minimumTimeBetweenSame = "PT5M"      # 5 minutes
minimumTimeBetweenAny = "PT30S"      # 30 seconds
cooldownAfterCritical = "PT1M"       # 1 minute after danger
```

### Testing & Tuning Recommendations

1. **Start Sparse**: Launch with fewer phrases, add gradually
2. **Track Metrics**: Log which phrases trigger, player reactions
3. **A/B Test**: Different phrase sets for different players
4. **Community Input**: Allow players to submit phrases
5. **Regular Updates**: Seasonal phrases, event-specific content

### Future Enhancements

1. **Machine Learning**: Learn which phrases players respond to best
2. **Player-Specific**: Adapt to individual player preferences
3. **Mood System**: AI mood affects phrase selection
4. **Multi-Agent**: Agents reference each other's catchphrases
5. **Achievement Unlocks**: New phrases unlock as relationship builds

---

## Sources

- [TV Land's 100 Greatest TV Quotes & Catchphrases](https://paper.i21st.cn/story/28154.html)
- [Friends Joey Tribbiani's "How You Doin'?" Analysis](https://www.dictionary.com/e/slang/how-you-doing/)
- [26 Years Later: Friends Catchphrases Still Iconic](https://baijiahao.baidu.com/s?id=1678533749984430168)
- [You Had Me at Hello: How Phrasing Affects Memorability](https://arxiv.org/abs/1203.6360) - arXiv research paper
- [Godot Dialogue Manager Plugin](https://gitcode.com/gh_mirrors/go/godot_dialogue_manager)
- [Game Narrative Design: "Until My Knee Was Shot in the Arrow"](https://new.qq.com/rain/a/20201102A0IFL400)
- [NPC Random Voice Design for Game Narrative](https://www.gcores.com/articles/130193)
- [Baldur's Gate 3 AI Companion Mod](https://www.rockpapershotgun.com/baldurs-gate-3-ai-companions)
- [Construction Site Communication Guide](https://m.blog.csdn.net/gitblog_00991/article/details/151283126)
- [Context-Aware Dialogue Systems](https://cloud.tencent.com/developer/article/1424142)

---

*Document compiled for MineWright AI companion development*
*Generated: 2025-02-27*
