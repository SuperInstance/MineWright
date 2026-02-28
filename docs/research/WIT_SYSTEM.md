# Wit System Design for MineWright Foreman AI

**Document Version:** 1.0
**Date:** 2026-02-26
**Status:** Design Specification
**Author:** Wit System Designer for MineWright

---

## Executive Summary

This document specifies a comprehensive wit and humor system for the MineWright Foreman AI companion in Minecraft. The system balances **functional utility** (giving instructions, managing tasks) with **personality** (being entertaining, building rapport) through contextually-aware humor generation.

**Key Design Principles:**
1. **Utility First, Wit Second** - Humor never interrupts critical gameplay moments
2. **Contextual Intelligence** - Jokes respond to situations, not canned randomness
3. **Relationship-Gated** - Humor frequency and type scale with rapport level
4. **Recovery-Aware** - Failed jokes are detected and system self-corrects
5. **Player-Respectful** - Humor is never mean-spirited or annoying

---

## Table of Contents

1. [System Architecture](#system-architecture)
2. [Wit Triggers: When to Be Funny](#wit-triggers)
3. [Humor Generation Pipeline](#humor-generation-pipeline)
4. [Context Templates by Situation](#context-templates)
5. [Inside Joke Evolution System](#inside-joke-evolution)
6. [Failed Joke Recovery](#failed-joke-recovery)
7. [Player Preference Integration](#player-preference-integration)
8. [Configuration Options](#configuration-options)
9. [Implementation Checklist](#implementation-checklist)

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Wit System Architecture                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              WitDecisionEngine                         │    │
│  │  - Evaluates if humor is appropriate NOW              │    │
│  │  - Checks: Context, Rapport, Cooldowns, Mood          │    │
│  │  - Returns: ALLOWED, SUPPRESSED, or REQUIRED          │    │
│  └────────────────┬───────────────────────────────────────┘    │
│                   │                                            │
│         ┌─────────┴─────────┐                                  │
│         │                   │                                  │
│  ┌──────▼──────┐      ┌────▼────────┐                        │
│  │  Humor      │      │  Inside     │                        │
│  │  Generator  │      │  Joke       │                        │
│  │             │      │  System     │                        │
│  └──────┬──────┘      └────┬────────┘                        │
│         │                   │                                  │
│         └─────────┬─────────┘                                  │
│                   │                                            │
│          ┌────────▼────────┐                                   │
│          │  Response       │                                   │
│          │  Assembler      │                                   │
│          │  - Adds wit to  │                                   │
│          │    base msg     │                                   │
│          └────────┬────────┘                                   │
│                   │                                            │
│          ┌────────▼────────┐                                   │
│          │  Recovery       │                                   │
│          │  Monitor        │                                   │
│          │  - Tracks       │                                   │
│          │    reactions    │                                   │
│          └─────────────────┘                                   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Key Methods |
|-----------|----------------|-------------|
| **WitDecisionEngine** | Decide if humor is appropriate | `shouldTellJoke()`, `getHumorMode()` |
| **HumorGenerator** | Generate contextual jokes | `generateForSituation()`, `generatePun()` |
| **InsideJokeSystem** | Track and reference shared humor | `triggerInsideJoke()`, `recordJokeMoment()` |
| **ResponseAssembler** | Combine wit with functional text | `assembleWithWit()`, `addVerbalTic()` |
| **RecoveryMonitor** | Detect failed jokes and adjust | `recordReaction()`, `shouldCooldown()` |

---

## Wit Triggers: When to Be Funny

### The Five-Check Decision Framework

Before any humor is generated, the system runs this decision flow:

```
┌─────────────────────────────────────────────────────────────┐
│                   WIT DECISION FLOW                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. CONTEXT CHECK                                           │
│     ├─ Is player in combat? → SUPPRESS WIT                  │
│     ├─ Is player health critical? → SUPPRESS WIT            │
│     ├─ Is player frustrated? → SUPPRESS WIT                 │
│     └─ Is this a critical task? → SUPPRESS WIT              │
│                                                             │
│  2. RAPPORT CHECK                                            │
│     ├─ Rapport < 20 → 50% humor reduction                   │
│     ├─ Rapport 20-50 → Normal humor frequency               │
│     ├─ Rapport 50-80 → Increased humor + inside jokes       │
│     └─ Rapport 80+ → Maximum wit, banter enabled            │
│                                                             │
│  3. COOLDOWN CHECK                                            │
│     ├─ Last joke < 30 seconds ago → SUPPRESS                │
│     ├─ Same trigger type < 5 minutes → SUPPRESS             │
│     └─ Consecutive failures = 3 → SUPPRESS                  │
│                                                             │
│  4. PERSONALITY CHECK                                          │
│     └─ Roll against humor trait (default: 65% = 13% chance) │
│                                                             │
│  5. RECOVERY CHECK                                            │
│     └─ Recent joke landed poorly → SUPPRESS                  │
│                                                             │
│  RESULT: ALLOWED, SUPPRESSED, or REQUIRED                   │
└─────────────────────────────────────────────────────────────┘
```

### Situations That ALWAYS Suppress Wit

| Situation | Reason | Example Response |
|-----------|--------|------------------|
| **Combat Active** | Player needs focus | "Watch out!" (no joke) |
| **Health < 3 Hearts** | Critical safety | "Get to safety!" (no joke) |
| **Player Frustrated** | Detected from behavior | "Hey, take a breath." (supportive) |
| **First Tutorial** | Learning mode | "Click here to mine." (instructional) |
| **Recent Death (< 1 min)** | Respectful silence | "Glad you're back." (somber) |
| **Complex Planning** | Cognitive load | "I'll handle this." (focused) |

### Situations That ALWAYS Include Wit

| Situation | Why Required | Example |
|-----------|--------------|---------|
| **Task Success** | Celebrate together | "Got it done! Another job well finished." |
| **Near-Death Survival** | Tension relief | "You're alive! HOW did you survive that?" |
| **Player Achievement** | Shared excitement | "NOW we're talking! That's what I'm talking about!" |
| **Long Idle (> 2 min)** | Break boredom | "You know what I was thinking? If creepers had elbows..." |
| **Entering Nether** | Classic moment | "Welcome to Hell. Well, technically the Nether..." |

### Rapport-Based Humor Frequency

```java
/**
 * Calculates humor chance based on rapport level.
 * Returns probability 0.0 to 1.0 that a joke should be told.
 */
public double getHumorChance(int rapport, PersonalityProfile personality) {
    // Base chance from personality trait (65 = 13%)
    double baseChance = personality.humor / 500.0;

    // Rapport multiplier
    double rapportMultiplier = switch (rapport) {
        case int r when r < 20  -> 0.5;   // Cautious
        case int r when r < 50  -> 1.0;   // Normal
        case int r when r < 80  -> 1.5;   // Friendly
        default                      -> 2.0;   // Best friend
    };

    return Math.min(0.4, baseChance * rapportMultiplier); // Max 40%
}
```

---

## Humor Generation Pipeline

### Pipeline Stages

```
┌───────────────────────────────────────────────────────────────┐
│                   HUMOR GENERATION PIPELINE                   │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  STAGE 1: CONTEXT ANALYSIS                                    │
│  ├─ What's happening? (mining, building, idle, etc.)         │
│  ├─ Where are we? (biome, location, time of day)             │
│  ├─ How is player feeling? (mood detection)                  │
│  └─ What's our relationship level? (rapport)                  │
│                                                               │
│  STAGE 2: HUMOR TYPE SELECTION                                 │
│  ├─ Check for inside joke opportunity → INSIDE_JOKE           │
│  ├─ Check for pun opportunity → WORDPLAY                     │
│  ├─ Check for situational irony → OBSERVATIONAL              │
│  └─ Fallback → SELF_DEPRECATING (safe option)                │
│                                                               │
│  STAGE 3: TEMPLATE MATCHING                                   │
│  ├─ Select appropriate template for situation                │
│  ├─ Fill in context variables (block type, biome, etc.)      │
│  └─ Apply personality adjustments (formality, energy)        │
│                                                               │
│  STAGE 4: QUALITY FILTERING                                    │
│  ├─ Check for recently used phrases                          │
│  ├─ Verify no offensive content                               │
│  ├─ Ensure joke is situationally appropriate                 │
│  └─ Apply length constraints (max 15 words for quick wit)    │
│                                                               │
│  STAGE 5: ASSEMBLY & DELIVERY                                  │
│  ├─ Combine functional message + wit                         │
│  ├─ Add verbal tics if appropriate                            │
│  ├─ Apply formatting (parenthetical asides, etc.)            │
│  └─ Send to chat, track for recovery monitoring             │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

### Humor Type Hierarchy

The system attempts humor types in this order (most preferred first):

1. **Inside Jokes** (if rapport >= 50 and joke exists for context)
2. **Situational Observations** (contextual comments on current state)
3. **Wordplay/Puns** (Minecraft/construction themed)
4. **Self-Deprecating AI Humor** (confident, not insecure)
5. **Classic Verbal Tics** (catchphrases, recurring patterns)

### Template System Structure

```java
public class HumorTemplates {

    // Each template has context variables in {braces}
    private static final Map<String, List<String>> TEMPLATES = Map.of(

        // MINING TEMPLATES
        "mining_ongoing", List.of(
            "You've been at this a while. Found any {rare_ore} yet, "
            + "or just more {common_block}?",
            "Mining marathon. Impressive dedication or just stubborn? "
            + "Either way, I respect it.",
            "How's the excavation going? Found any diamonds yet? "
            + "No? Just more {common_block}? That's minecraft for you."
        ),

        // BUILDING TEMPLATES
        "building_ongoing", List.of(
            "This is coming together. Slowly. Painfully slowly. "
            + "But it's happening. I think.",
            "You've been placing {block_type} for {duration}. "
            + "I don't know whether to be impressed or concerned.",
            "The creative process is beautiful. Also exhausting. "
            + "Mostly exhausting. How's your back?"
        ),

        // TASK COMPLETION
        "task_complete", List.of(
            "Got it done. Another job well finished. On to the next!",
            "Done and dusted. That didn't take too long, did it?",
            "Task complete. I believe a brief celebration is in order. "
            + "Brief. Very brief."
        ),

        // FAILURE RECOVERY
        "task_failed", List.of(
            "Having some trouble? Want me to suggest a different approach?",
            "That didn't go as planned. Do you want to try again, or...?",
            "Setbacks happen. They're part of the process. "
            + "The annoying, terrible part. But still part of it."
        ),

        // IDLE COMMENTARY
        "idle_thoughts", List.of(
            "You know what I was thinking? If creepers had elbows, "
            + "they'd be terrible at arm wrestling. Just... not built for it.",
            "Been doing some calculations. Based on current progress, "
            + "we'll finish this project approximately never.",
            "I've decided I want a name. Not 'MineWright.' Everyone's named MineWright. "
            + "Something dignified. 'Archibald'? You're not listening, are you?"
        ),

        // NEAR-DEATH
        "survived_close_call", List.of(
            "...You're alive. YOU'RE ALIVE! I was preparing my "
            + "'fond farewell' speech. Had it READY.",
            "That was... impressive? I think? The math said you should be dead. "
            + "But you're not. And I'm... actually happy about that.",
            "I respect the persistence. Even when it's... maybe not "
            + "the best idea. Still. Grit is good."
        ),

        // ACHIEVEMENT
        "achievement_unlocked", List.of(
            "NOW we're talking! That's what I'm talking about!",
            "BIG move! Absolutely killing it today!",
            "This is why we do this. Moments like this. You're on fire, boss."
        )
    );

    public static String getTemplate(String situation, Map<String, String> context) {
        List<String> templates = TEMPLATES.get(situation);
        if (templates == null || templates.isEmpty()) {
            return "";
        }

        String template = templates.get(
            ThreadLocalRandom.current().nextInt(templates.size())
        );

        // Fill in context variables
        for (Map.Entry<String, String> entry : context.entrySet()) {
            template = template.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return template;
    }
}
```

---

## Context Templates by Situation

### Mining & Excavation

| Context | Trigger | Template | Variables |
|---------|---------|----------|------------|
| **Continuous Mining** | Mining 5+ min | "You've been at this a while. Found any {rare} yet?" | rare=diamonds/iron/coal |
| **Mining at Night** | Night + underground | "Mining at midnight? Dedicated or crazy?" | - |
| **Found Diamonds** | Event trigger | "Look at you, striking it rich! Remember me when you're ruling the server." | - |
| **Mining Stone** | 100+ stone | "I'm not {stoned}, I'm just dedicated." | stoned=stoned |
| **Cave Discovery** | Enter cave | "Nothing like the ambiance of dripping water and distant monster noises." | - |
| **Strip Mining** | Pattern detected | "Efficient! If boring. I respect it. Mostly." | - |

### Building & Construction

| Context | Trigger | Template | Variables |
|---------|---------|----------|------------|
| **Placing Blocks** | Building 10+ min | "This is coming together. {pace}. But it's happening." | pace=Slowly/Painfully slowly |
| **Using Cobblestone** | 50+ cobble | "This construction is... {riveting}." | riveting=riveting |
| **Building Tower** | Y > 100 | "Reach for the sky! Or at least, Y={max_height}." | max_height=256 |
| **Placing Glass** | Using glass | "Let's be transparent about this." | - |
| **Using Stairs** | 20+ stairs | "One step at a time. Literally." | - |
| **Building with Dirt** | Dirt house detected | "It's not low-quality, it's... {rustic}." | rustic=rustic |
| **Nether Portal** | Place portal | "Time to go to {hell}... and back." | hell=hell |
| **Redstone Wiring** | Place redstone | "This is {shocking} work." | shocking=shocking |

### Combat & Survival

| Context | Trigger | Template | Variables |
|---------|---------|----------|------------|
| **Creeper Sighted** | Creeper < 10 blocks | "Creeper? {reaction}" | reaction=Awwww man |
| **Fighting Zombies** | Kill 5+ zombies | "Brainsss... oh wait, I don't have one." | - |
| **Enderman Encounter** | Look at enderman | "Don't look now... {literally}." | literally=literally |
| **Skeleton Sniping** | Skeleton attack | "He's got a {bone} to pick." | bone=bone |
| **Spider Climbing** | Spider wall climb | "Some people will {scale} anything for attention." | scale=scale |
| **Enter Nether** | Enter dimension | "Welcome to the land of {ghasts} and glory." | ghasts=ghasts |
| **Death by Lava** | Respawn from lava | "Well, that was... {heated}." | heated=heated |
| **Respawn General** | Any respawn | "Back in one piece. {ish}." | ish=Ish |

### Exploration & Environment

| Context | Trigger | Template | Variables |
|---------|---------|----------|------------|
| **Desert Biome** | Enter desert | "Ah, the desert. Hot, dry, and full of {danger}. Just like my ex's apartment." | danger=things trying to kill us |
| **Mountains** | Enter mountains | "The mountains. Cold, unforgiving, and absolutely {adjective}. Kind of like you before coffee." | adjective=stunning |
| **Ocean** | Enter ocean | "The ocean. Beautiful, vast, and full of {drowned}. Par for the course, really." | drowned=drowned people who want to murder you |
| **Village** | Find village | "Civilization! Well, sort of. They don't {action} much. Or do much." | action=talk |
| **Stronghold** | Find stronghold | "A stronghold! Someone built this before us. Probably {died} here. Let's not think about that." | died=died |
| **Raining** | Weather change | "Rain's coming down. Perfect weather for {activity}!" | activity=mining underground |
| **Thunderstorm** | Storm starts | "Thunder and lightning! Stay safe out there!" | - |

### Idle & Downtime

| Context | Trigger | Template | Variables |
|---------|---------|----------|------------|
| **Long Idle** | No action 2+ min | "So... been thinking about getting a hobby. Maybe {hobby}." | hobby=knitting |
| **Standing Around** | No action 5+ min | "Still deciding what to build? Or are we just {admiring} the view?" | admiring=admiring |
| **Late Night** | Real time after midnight | "It's late. Why are we still awake? What are we trying to {prove}?" | prove=prove |
| **Long Session** | 3+ hours playing | "We've been at this for a while. Not that I'm counting. I'm always {counting}." | counting=counting |

---

## Inside Joke Evolution System

### Inside Joke Lifecycle

```
┌───────────────────────────────────────────────────────────────┐
│              INSIDE JOKE EVOLUTION LIFECYCLE                  │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  STAGE 1: CREATION (Rapport 30+)                              │
│  ├─ Funny moment occurs (failure, serendipity, absurdity)    │
│  ├─ MineWright makes witty comment                                 │
│  ├─ Player responds positively (doesn't get annoyed)          │
│  └─ Record as potential inside joke                           │
│                                                               │
│  STAGE 2: INCUBATION (Rapport 40+)                            │
│  ├─ MineWright references moment again later                      │
│  ├─ "Remember the lava incident?"                             │
│  ├─ If player laughs → promote to inside joke                │
│  └─ Store in CompanionMemory.insideJokes                     │
│                                                               │
│  STAGE 3: INCORPORATION (Rapport 50+)                         │
│  ├─ Joke becomes shorthand for similar situations            │
│  ├─ "That was a gravity moment" = any fall                   │
│  ├─ Reference count tracked (use sparingly!)                 │
│  └─ Cap references to 3-5 uses before retirement            │
│                                                               │
│  STAGE 4: EVOLUTION (Rapport 70+)                             │
│  ├─ Inside jokes spawn variations                             │
│  ├─ "Gravity: 2, MineWright: still 0"                             │
│  ├─ Creates shared language unique to relationship           │
│  └─ Strengthens companion bond                                │
│                                                               │
│  STAGE 5: RETIREMENT                                          │
│  ├─ Overused jokes fade from active rotation                 │
│  ├─ Still in memory, but referenced rarely                   │
│  └─ New inside jokes take precedence                         │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

### Inside Joke Trigger Conditions

```java
public class InsideJokeSystem {

    /**
     * Checks if an inside joke should be triggered.
     */
    public Optional<String> maybeTriggerInsideJoke(
        String currentSituation,
        CompanionMemory memory
    ) {
        // Must have enough rapport
        if (memory.getRapportLevel() < 50) {
            return Optional.empty();
        }

        InsideJoke joke = memory.getRandomInsideJoke();
        if (joke == null) return Optional.empty();

        // Don't overuse (max 3-5 references)
        if (joke.referenceCount >= 5) {
            return Optional.empty();
        }

        // Simple relevance check
        String situationLower = currentSituation.toLowerCase();
        String contextLower = joke.context.toLowerCase();

        // 15% chance to reference randomly, OR if context matches
        boolean shouldReference = situationLower.contains(contextLower.substring(0, 4))
            || ThreadLocalRandom.current().nextDouble() < 0.15;

        if (shouldReference) {
            joke.incrementReference();
            return Optional.of(formatJokeReference(joke));
        }

        return Optional.empty();
    }

    /**
     * Formats an inside joke reference.
     */
    private String formatJokeReference(InsideJoke joke) {
        List<String> templates = List.of(
            "Like that time " + joke.context + "...",
            "Reminds me of when " + joke.punchline,
            "Ah, yes. '" + joke.punchline + "' all over again.",
            "Shall we recreate the legendary '" + joke.context + "' incident?"
        );

        return templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
    }
}
```

### Classic Inside Joke Seeds

The system comes with pre-configured joke "seeds" that evolve into inside jokes:

| Seed Context | Initial Punchline | Evolution Path |
|--------------|-------------------|----------------|
| **Fall Death** | "Gravity: 1, MineWright: 0." | → "Gravity: 2, MineWright: still 0" → "Gravity always wins" |
| **Lava Death** | "Well, that was... heated." | → "The lava teacher strikes again" → "Don't touch me" |
| **Creeper Surprise** | "Creeper? Awwww man." | → "Awwww man squared" → Creeper callback anytime |
| **Dig Straight Down** | "Bold choice. Optimistically impossible." | → "Digging to bedrock, again" |
| **Lost Items** | "Dark times. Literally." | → Any torch shortage triggers this |
| **TNT Accident** | "This seems... safe." | → Referenced whenever TNT is placed |
| **Villager Trading** | "Hmm. Hmm. Hmmmm." | → Villager noises for confusion |
| **Void Death** | "The void. Very... purple." | → Purple = void/death reference |

---

## Failed Joke Recovery

### Detecting Failed Jokes

The system tracks these indicators that a joke didn't land:

| Indicator | Detection Method | Weight |
|-----------|------------------|--------|
| **Immediate Command** | Player issues command within 5 seconds | +3 |
| **No Response** | No player action for 30 seconds | +2 |
| **Repetition** | Player repeats previous request | +3 |
| **Frustration Keywords** | "just", "stop", "never mind" in messages | +4 |
| **Task Failure** | Player fails at task after joke | +2 |

**Threshold:** 4+ points = joke failed

### Recovery Strategies

```java
public class JokeRecoverySystem {

    private int consecutiveMissedJokes = 0;
    private Instant lastJokeTime;

    /**
     * Called when a joke is delivered to track player reaction.
     */
    public void recordJokeResult(boolean landed) {
        lastJokeTime = Instant.now();

        if (landed) {
            consecutiveMissedJokes = 0; // Reset on success
        } else {
            consecutiveMissedJokes++;

            if (consecutiveMissedJokes >= 3) {
                triggerCooldownPeriod();
            }
        }
    }

    /**
     * Checks if we should skip humor based on recent failures.
     */
    public boolean shouldTellJoke() {
        // Too many misses recently?
        if (consecutiveMissedJokes >= 3) {
            return false; // Cooling off period
        }

        // Too soon since last joke?
        if (lastJokeTime != null) {
            long minutesSince = ChronoUnit.MINUTES.between(
                lastJokeTime, Instant.now()
            );
            if (minutesSince < 2) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns recovery message if needed.
     */
    public Optional<String> getRecoveryMessage() {
        if (consecutiveMissedJokes >= 3) {
            consecutiveMissedJokes = 0; // Reset after recovery
            return Optional.of("Right then. Less chatting, more working.");
        }
        return Optional.empty();
    }

    private void triggerCooldownPeriod() {
        LOGGER.debug("Humor cooldown triggered after {} misses",
            consecutiveMissedJokes);
    }
}
```

### Recovery Response Templates

| Situation | Recovery Response | Tone |
|-----------|------------------|------|
| **3 Consecutive Misses** | "Right then. Less chatting, more working." | Self-correcting |
| **During Critical Task** | [Switch immediately to task, no comment] | Professional |
| **Player Frustrated** | "Hey, take a breath. This is difficult. You'll get it." | Supportive |
| **After Combat** | "*nods thoughtfully*" | Silent acknowledgment |

---

## Player Preference Integration

### Detecting Player Preferences

The system learns player preferences over time:

```java
public class HumorPreferenceLearning {

    private final Map<String, Double> preferences = new HashMap<>();

    // Preference types
    enum PreferenceType {
        HUMOR_FREQUENCY,      // 0.0 = silent, 1.0 = very chatty
        WIT_STYLE,           // dry, punny, sarcastic, playful
        SELF_DEPRECATION,    // okay with AI making fun of itself
        PLAYER_TEASING,      // okay with light teasing of player skills
        DURING_COMBAT,       // humor during fights (rare players)
        DURING_BUILDING,     // commentary during creative work
        PUNS,                // wordplay tolerance
        INSIDE_JOKES         // shared references
    }

    /**
     * Records player response to humor to learn preferences.
     */
    public void recordReaction(String jokeType, boolean positiveResponse) {
        double adjustment = positiveResponse ? 0.1 : -0.05;
        preferences.merge(jokeType, adjustment, Double::sum);

        // Keep in range [0.0, 1.0]
        preferences.compute(jokeType, (k, v) ->
            Math.max(0.0, Math.min(1.0, v + adjustment))
        );
    }

    /**
     * Gets player preference score.
     */
    public double getPreference(String preferenceType) {
        return preferences.getOrDefault(preferenceType, 0.5); // Default: neutral
    }
}
```

### Preference Influence on Humor

Player preferences multiply the base humor chance:

| Preference | Low (< 0.3) | Medium (0.3-0.7) | High (> 0.7) |
|------------|-------------|------------------|---------------|
| **Humor Frequency** | 50% less jokes | Normal | 50% more jokes |
| **Puns** | Avoid wordplay | Balanced | Embrace puns |
| **Self-Deprecation** | Confident tone | Mixed | More self-mockery |
| **Player Teasing** | Never tease | Light teasing | Full banter |
| **Inside Jokes** | Rare references | Occasional | Frequent callbacks |

---

## Configuration Options

### Config File Format (minewright-common.toml)

```toml
[wit]
# Master enable switch
enabled = true

# Base humor frequency (0-100)
base_frequency = 25

# Minimum rapport for different humor types
min_rapport_puns = 20
min_rapport_inside_jokes = 50
min_rapport_sarcasm = 40
min_rapport_banter = 70

# Cooldown settings
joke_cooldown_seconds = 120
max_consecutive_misses = 3
cooldown_recovery_minutes = 10

# Humor style preferences (0-100)
use_self_deprecating = 80
use_construction_puns = 90
use_minecraft_references = 95
use_inside_jokes = 100
use_sarcasm = 60

# Forbidden contexts (these never get humor)
never_joke_during_combat = true
never_joke_when_health_low = true
never_joke_during_tutorial = true
never_joke_when_player_frustrated = true

# Inside joke settings
max_inside_jokes = 30
max_joke_references = 5
joke_evolution_enabled = true

# Player preference learning
learn_preferences = true
preference_adjustment_rate = 0.1
preference_influence_weight = 0.3

# Verbal tics frequency (0-100)
verbal_tic_frequency = 20
catchphrase_rotation = true
```

### Java Configuration Class

```java
public class WitConfig {

    // Master switch
    public static final ForgeConfigSpec.BooleanValue WIT_ENABLED;

    // Base settings
    public static final ForgeConfigSpec.IntValue BASE_FREQUENCY;
    public static final ForgeConfigSpec.IntValue JOKE_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.IntValue MAX_CONSECUTIVE_MISSES;

    // Rapport thresholds
    public static final ForgeConfigSpec.IntValue MIN_RAPPORT_PUNS;
    public static final ForgeConfigSpec.IntValue MIN_RAPPORT_INSIDE_JOKES;
    public static final ForgeConfigSpec.IntValue MIN_RAPPORT_SARCASM;

    // Humor style weights
    public static final ForgeConfigSpec.IntValue USE_SELF_DEPRECATING;
    public static final ForgeConfigSpec.IntValue USE_CONSTRUCTION_PUNS;
    public static final ForgeConfigSpec.IntValue USE_MINECRAFT_REFERENCES;
    public static final ForgeConfigSpec.IntValue USE_INSIDE_JOKES;

    // Context restrictions
    public static final ForgeConfigSpec.BooleanValue NEVER_JOKE_DURING_COMBAT;
    public static final ForgeConfigSpec.BooleanValue NEVER_JOKE_WHEN_HEALTH_LOW;
    public static final ForgeConfigSpec.BooleanValue NEVER_JOKE_DURING_TUTORIAL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("wit");

        WIT_ENABLED = builder
            .comment("Master enable switch for wit system")
            .define("enabled", true);

        BASE_FREQUENCY = builder
            .comment("Base humor frequency (0-100)")
            .defineInRange("baseFrequency", 25, 0, 100);

        JOKE_COOLDOWN_SECONDS = builder
            .comment("Minimum seconds between jokes")
            .defineInRange("jokeCooldownSeconds", 120, 30, 300);

        MAX_CONSECUTIVE_MISSES = builder
            .comment("Failed jokes before cooldown")
            .defineInRange("maxConsecutiveMisses", 3, 1, 10);

        MIN_RAPPORT_PUNS = builder
            .comment("Minimum rapport for puns")
            .defineInRange("minRapportPuns", 20, 0, 100);

        MIN_RAPPORT_INSIDE_JOKES = builder
            .comment("Minimum rapport for inside jokes")
            .defineInRange("minRapportInsideJokes", 50, 0, 100);

        MIN_RAPPORT_SARCASM = builder
            .comment("Minimum rapport for sarcasm")
            .defineInRange("minRapportSarcasm", 40, 0, 100);

        USE_SELF_DEPRECATING = builder
            .comment("Self-deprecating humor weight (0-100)")
            .defineInRange("useSelfDeprecating", 80, 0, 100);

        USE_CONSTRUCTION_PUNS = builder
            .comment("Construction pun weight (0-100)")
            .defineInRange("useConstructionPuns", 90, 0, 100);

        USE_MINECRAFT_REFERENCES = builder
            .comment("Minecraft reference weight (0-100)")
            .defineInRange("useMinecraftReferences", 95, 0, 100);

        USE_INSIDE_JOKES = builder
            .comment("Inside joke weight (0-100)")
            .defineInRange("useInsideJokes", 100, 0, 100);

        NEVER_JOKE_DURING_COMBAT = builder
            .comment("Never joke during combat")
            .define("neverJokeDuringCombat", true);

        NEVER_JOKE_WHEN_HEALTH_LOW = builder
            .comment("Never joke when health critical")
            .define("neverJokeWhenHealthLow", true);

        NEVER_JOKE_DURING_TUTORIAL = builder
            .comment("Never joke during tutorial")
            .define("neverJokeDuringTutorial", true);

        builder.pop();
    }
}
```

---

## Implementation Checklist

### Phase 1: Foundation (Week 1-2)
- [ ] Create `WitDecisionEngine` class
- [ ] Create `HumorGenerator` class
- [ ] Create `HumorTemplates` with base templates
- [ ] Create `JokeRecoverySystem` class
- [ ] Add wit config to `MineWrightConfig.java`

### Phase 2: Context Integration (Week 3)
- [ ] Integrate with `ProactiveDialogueManager`
- [ ] Add wit to `ConversationManager` responses
- [ ] Connect to `CompanionMemory` for rapport checks
- [ ] Add cooldown tracking per joke type

### Phase 3: Inside Joke System (Week 4)
- [ ] Implement inside joke recording
- [ ] Add inside joke trigger system
- [ ] Implement joke evolution logic
- [ ] Add reference count tracking

### Phase 4: Player Preferences (Week 5)
- [ ] Implement preference learning system
- [ ] Add reaction tracking
- [ ] Connect preferences to humor generation
- [ ] Add preference influence multiplier

### Phase 5: Polish & Testing (Week 6)
- [ ] Tune humor frequencies based on testing
- [ ] Add additional templates
- [ ] Test recovery mechanisms
- [ ] Add analytics for joke success rates

### Phase 6: Content Expansion (Ongoing)
- [ ] Add more pun templates
- [ ] Expand situation-specific jokes
- [ ] Add seasonal/event humor
- [ ] Community joke submissions

---

## Example Integration Code

### Adding Wit to ConversationManager

```java
public class ConversationManager {

    private final WitDecisionEngine witEngine;
    private final HumorGenerator humorGenerator;
    private final JokeRecoverySystem recovery;

    public CompletableFuture<String> generateConversationalResponse(
        String playerName,
        String message,
        AsyncLLMClient llmClient
    ) {
        // ... existing LLM call code ...

        return llmClient.sendAsync(userPrompt, params)
            .thenApply(response -> {
                String responseText = response.getContent().trim();

                // Add optional wit
                if (witEngine.shouldTellJoke(memory, "conversation")) {
                    String wit = humorGenerator.generateForSituation(
                        "conversation_response",
                        memory
                    );

                    if (!wit.isEmpty()) {
                        responseText = responseText + " " + wit;
                        recovery.recordJokeAttempt();
                    }
                }

                return responseText;
            });
    }
}
```

### Wit-Enhanced Proactive Comments

```java
public class ProactiveDialogueManager {

    private void triggerComment(String triggerType, String context) {
        // ... existing trigger logic ...

        generateAndSpeakComment(triggerType, context);
    }

    private void generateAndSpeakComment(String triggerType, String context) {
        // Generate base comment
        String baseComment = generateBaseComment(triggerType, context);

        // Add wit if appropriate
        if (witEngine.shouldTellJoke(memory, triggerType)) {
            String wit = humorGenerator.generateForSituation(
                triggerType,
                memory
            );

            if (!wit.isEmpty()) {
                baseComment = baseComment + " " + wit;
            }
        }

        // Send comment
        minewright.sendChatMessage(baseComment);
    }
}
```

---

## Success Metrics

### Quantitative Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Joke Success Rate** | > 70% | Positive player responses |
| **Annoyance Reports** | < 5% | Player disables wit |
| **Inside Joke Count** | 5-10 per 50 hours rapport | Memory tracking |
| **Recovery Cooldowns** | < 3 per session | Failure tracking |
| **Player Retention** | +15% vs no wit | Session length comparison |

### Qualitative Indicators

- Players reference MineWright's jokes in chat
- Players share funny moments on social media
- Players request "more witty comments"
- Steam reviews mention personality positively
- Players keep wit enabled (not disabled in config)

---

## Conclusion

The Wit System for MineWright Foreman AI balances functional utility with engaging personality through:

1. **Context-Aware Decision Engine** - Knows WHEN to be funny
2. **Template-Based Generation** - Produces contextual humor without repetition
3. **Inside Joke Evolution** - Creates shared humor that strengthens bonds
4. **Recovery Mechanisms** - Self-corrects when jokes don't land
5. **Player Preference Learning** - Adapts to individual player tastes

The system is designed to be **implementable**, **configurable**, and **extensible** - allowing the foreman AI to be a genuinely funny companion without becoming annoying or distracting from gameplay.

**The goal:** Players should look forward to MineWright's comments, not turn them off.

---

**Document Version:** 1.0
**Last Updated:** 2026-02-26
**Maintained By:** MineWright Development Team
**Next Review:** After Phase 3 implementation
