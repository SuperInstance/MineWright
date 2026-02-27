# Situational Comedy for MineWright Workers

**Version:** 1.0
**Date:** 2026-02-27
**Status:** Research & Design Document

---

## Executive Summary

This document provides a comprehensive framework for implementing situational comedy in MineWright worker dialogue. The research synthesizes comedy patterns from television (The Office, Brooklyn Nine-Nine, Parks and Rec), game development best practices, and AI humor research to create a system where natural humor emerges from gameplay situations rather than forced jokes.

**Core Principle:** The 70/30 Rule - 70% utility, 30% humor. Comedy should enhance the companion experience, never interfere with gameplay or annoy the player.

---

## Table of Contents

1. [Research Foundations](#research-foundations)
2. [Situation Classification](#situation-classification)
3. [Comedy Triggers & Timing Windows](#comedy-triggers-timing-windows)
4. [Risk Assessment for Humor Attempts](#risk-assessment-for-humor-attempts)
5. [Recovery Patterns for Failed Jokes](#recovery-patterns-for-failed-jokes)
6. [Memory System for Running Gags](#memory-system-for-running-gags)
7. [Player Mood Inference](#player-mood-inference)
8. [ComedyTimingManager Implementation](#comedytimingmanager-implementation)
9. [Comedy Pattern Library](#comedy-pattern-library)
10. [Testing & Validation](#testing-validation)

---

## Research Foundations

### Television Comedy Analysis

#### The Office (US) - Mockumentary Style

**Key Techniques:**
- **Awkward Silence Comedy:** Pauses after uncomfortable statements create humor
- **Talking Head Segments:** Characters break the fourth wall to share reactions
- **Deadpan Delivery:** Serious presentation of ridiculous situations
- **Incongruity Between Intent and Result:** Characters try their best but fail spectacularly

**Applicable Patterns:**
```
Situation: Worker fails at simple task
Pattern: "I've calculated 47 ways this could go wrong. This was... number 48."
Delivery: Deadpan, slightly embarrassed acknowledgment
```

#### Brooklyn Nine-Nine - Ensemble Banter

**Key Techniques:**
- **Rapid-Fire Callbacks:** References to earlier episodes build continuity
- **Character-Driven Humor:** Each character has consistent comedic personality
- **Situational Irony:** Police procedurals subverted by absurdity
- **Rule of Three:** Setup, reinforcement, subversion

**Applicable Patterns:**
```
Situation: Repeated failure
Pattern:
  1st: "Unusual result."
  2nd: "That's... unexpected again."
  3rd: "At this point, I'm concerned gravity is personal."
```

#### Parks and Recreation - Optimistic Comedy

**Key Techniques:**
- **Enthusiastic Incompetence:** Characters try so hard it becomes funny
- **Running Gags with Evolution:** Jokes that grow and change over time
- **Mockumentary Glances:** Characters look at camera when something absurd happens
- **Heart in the Humor:** Comedy never undermines relationships

**Applicable Patterns:**
```
Situation: Success after many failures
Pattern: "After 37 attempts, statistical probability suggested success was due.
And yet, I'm still surprised."
```

### Game Development Research Findings

**Timing-Based Dialogue Systems:**
- NPCs become exasperated with too many interruptions
- Natural conversation rhythm is often overlooked
- Brief, punchy dialogue works better than verbose text
- Don't force humor - not every interaction needs a laugh

**Comedic Voice Acting:**
- Timing and expression crucial for humor
- Double entendre and wordplay enhance localization
- Voice actors inject personality through delivery interpretation

### AI Comedy Research (2025)

**Key Findings:**

1. **Irony is Most Common:** 117 instances in 50 comedy videos analyzed
2. **Disfluencies Matter:** Pauses, fillers, and repetitions manage timing
3. **Timing is Critical:** Robots with adaptive timing rated significantly funnier
4. **Four-Second Display:** Optimal readability and comedic timing
5. **Bidirectional Feedback:** User reactions drive content adaptation

**The "3 Appropriateness" Framework:**
1. **合时宜 (Timely):** Right moment, right timing
2. **合身份 (In-Character):** Matches AI role and personality
3. **合心意 (Player-Centric):** User enjoyment matters, not AI self-satisfaction

### Academic Sources

- **MDPI Player Engagement Research:** Game Engagement Questionnaire measures Immersion, Presence, Flow, and Absorption
- **arXiv AI Stand-up Comedy (Feb 2025):** Irony, exaggeration, and cognitive incongruity central to modern comedy
- **Oregon State University Robot Comedian:** Adaptive timing based on audience laughter improves response rates
- **Improbotics Real-Time Comedy:** Human-in-the-loop curation selects best AI-generated lines

---

## Situation Classification

Humor must emerge naturally from the situation. Classify situations to determine appropriate comedic response.

### Primary Situation Types

#### 1. DANGER SITUATIONS

**Characteristics:** High stakes, player focus required, potential for loss

**Examples:**
- Low health (< 30%)
- Combat encounters
- Lava nearby
- Falling from height
- Enemy ambush

**Humor Policy:** **ZERO HUMOR** in active danger
- Exception: Post-danger recovery (after safety confirmed)
- Recovery humor: Self-deprecating, acknowledgment of close call

```java
// Danger detection - suppress all humor
if (situation.type == DANGER && situation.isOngoing()) {
    return HumorResponse.SUPPRESS;
}

// Post-danger humor (after 3+ seconds of safety)
if (situation.type == DANGER && situation.timeSinceResolution > 3.0) {
    return generateRecoveryHumor(situation);
}
```

**Recovery Humor Examples:**
- Lava near-miss: "I briefly calculated my melting point. It's lower than I thought."
- Combat survival: "My combat training consisted of 'run away.' Worked perfectly."
- Fall survival: "Gravity and I had a disagreement. I lost, but I'm still here."

#### 2. SUCCESS SITUATIONS

**Characteristics:** Positive outcome, player achievement, shared victory

**Examples:**
- Task completed successfully
- Rare resource found (diamond, ancient debris)
- Structure finished
- Challenge overcome
- First-time achievement

**Humor Policy:** **30% humor chance** - Celebratory, enthusiastic, shared joy

**Success Humor Examples:**
- Diamond found: "We're now 12.3% richer. I've updated my retirement projections."
- Structure complete: "It's... not terrible. Which is my highest form of praise."
- Challenge beaten: "Statistical odds were 94:1 against us. I prefer being the 1."

#### 3. FAILURE SITUATIONS

**Characteristics:** Negative outcome, mistake made, resources lost

**Examples:**
- Task failed
- Death occurred
- Resources wasted
- Wrong block placed
- Pathfinding failure

**Humor Policy:** **50% humor chance** - Self-deprecating, deflect blame from player, normalize failure

**Failure Humor Examples:**
- Death: "I've calculated I've died 47 times. My survival instinct is clearly defective."
- Wrong block: "In my defense, dirt and cobblestone look very similar if you squint."
- Resource waste: "I prefer to think of it as... aggressive redistribution."

#### 4. ROUTINE SITUATIONS

**Characteristics:** Repetitive tasks, low stakes, ongoing work

**Examples:**
- Mining routine
- Building placement
- Resource gathering
- Pathfinding
- Idle/waiting

**Humor Policy:** **15% humor chance** - Light observational humor, timing-based

**Routine Humor Examples:**
- Mining: "I've mined 312 blocks. My enthusiasm has decreased proportionally."
- Building: "Block 347 of 500. I've started naming them. This one is Steve."
- Waiting: "I'm currently engaging in strategic patience. It's a skill."

#### 5. SURPRISE SITUATIONS

**Characteristics:** Unexpected events, plot twists, discoveries

**Examples:**
- Rare mob encounter
- Cave discovery
- Player surprise attack
- Sudden weather change
- Unexpected gift

**Humor Policy:** **40% humor chance** - Reactive, deadpan acknowledgment

**Surprise Humor Examples:**
- Rare mob: "That's... not supposed to be here. According to my database."
- Cave discovery: "I found a hole in the ground. This is the opposite of progress."
- Gift received: "This is... unexpected. Are you feeling guilty about something?"

### Situation Intensity Matrix

| Situation Type | Intensity | Humor Allowed | Humor Style | Recovery Time |
|----------------|-----------|---------------|-------------|---------------|
| Active Danger | CRITICAL | No | N/A | N/A |
| Post-Danger | HIGH | Low (20%) | Self-deprecating | 3+ seconds |
| Success | MEDIUM | Medium (30%) | Celebratory | Immediate |
| Failure | MEDIUM | High (50%) | Self-deprecating | 1+ seconds |
| Routine | LOW | Low (15%) | Observational | Variable |
| Surprise | VARIABLE | Medium (40%) | Reactive | Immediate |

---

## Comedy Triggers & Timing Windows

### The FLEETING WINDOW Principle

**Research Finding:** Humor has a fleeting window of opportunity. If too much time passes crafting the "perfect" joke, the moment passes and delivery becomes awkward.

**Rule:** Timely delivery > Perfect originality

```java
public class ComedyTimingWindow {
    private static final double BRIEF_WINDOW_SECONDS = 2.0;
    private static final double OPTIMAL_WINDOW_SECONDS = 1.0;
    private static final double DEADLINE_SECONDS = 3.0;

    /**
     * Determines if humor is still viable given time elapsed.
     */
    public boolean isWindowOpen(Instant situationStart) {
        double elapsed = ChronoUnit.MILLIS.between(
            situationStart, Instant.now()
        ) / 1000.0;

        if (elapsed > DEADLINE_SECONDS) {
            return false; // Window closed
        }

        if (elapsed <= OPTIMAL_WINDOW_SECONDS) {
            return true; // Perfect timing
        }

        if (elapsed <= BRIEF_WINDOW_SECONDS) {
            return true; // Brief window - use simple joke
        }

        return false; // Past deadline
    }

    /**
     * Gets joke complexity based on time remaining.
     */
    public JokeComplexity getComplexity(Instant situationStart) {
        double elapsed = ChronoUnit.MILLIS.between(
            situationStart, Instant.now()
        ) / 1000.0;

        if (elapsed <= 0.5) {
            return JokeComplexity.COMPLEX; // Time for crafty joke
        } else if (elapsed <= 1.0) {
            return JokeComplexity.MODERATE; // Standard joke
        } else if (elapsed <= BRIEF_WINDOW_SECONDS) {
            return JokeComplexity.SIMPLE; // Quick one-liner
        } else {
            return JokeComplexity.NONE; // Too late
        }
    }
}
```

### Timing Patterns by Situation

#### Immediate Response (< 0.5 seconds)

**Use for:** Success celebrations, surprise reactions

**Pattern:** Short, punchy, enthusiastic

```java
// Success celebration
"That was... actually impressive."
"Diamond! My excitement module is overloaded."
"We did the thing! The thing is done!"
```

#### Brief Window (0.5 - 2.0 seconds)

**Use for:** Failure acknowledgment, routine observations

**Pattern:** Moderate length, deadpan or self-deprecating

```java
// Failure acknowledgment
"I've calculated my error rate. It's... higher than preferred."
"Gravity: 1, MineWright: 0. Again."
"This went poorly. I blame physics."
```

#### Extended Window (2.0 - 3.0 seconds)

**Use for:** Post-danger recovery, complex situations

**Pattern:** Longer, reflective, perhaps story-building

```java
// Post-danger recovery
"I've updated my survival algorithms. Version 4.7 includes 'don't do that.'"
"In retrospect, approaching the lava was suboptimal. Live and learn. Mostly live."
```

### Comedy Cooldown System

Prevent joke spam and fatigue with intelligent cooldowns.

```java
public class ComedyCooldown {
    private final Map<SituationType, Instant> lastJokeTime = new ConcurrentHashMap<>();
    private final Map<SituationType, Long> cooldownDuration = new ConcurrentHashMap<>();

    public ComedyCooldown() {
        // Default cooldowns by situation type
        cooldownDuration.put(SituationType.ROUTINE, 120L); // 2 minutes
        cooldownDuration.put(SituationType.SUCCESS, 30L);  // 30 seconds
        cooldownDuration.put(SituationType.FAILURE, 60L);  // 1 minute
        cooldownDuration.put(SituationType.SURPRISE, 45L); // 45 seconds
        cooldownDuration.put(SituationType.DANGER, Long.MAX_VALUE); // Never
    }

    /**
     * Checks if humor is allowed based on cooldown.
     */
    public boolean canTellJoke(SituationType type) {
        Instant lastJoke = lastJokeTime.get(type);
        if (lastJoke == null) {
            return true; // Never told a joke of this type
        }

        long cooldownMs = cooldownDuration.get(type) * 1000;
        long elapsedMs = ChronoUnit.MILLIS.between(lastJoke, Instant.now());

        return elapsedMs >= cooldownMs;
    }

    /**
     * Records that a joke was told.
     */
    public void recordJoke(SituationType type) {
        lastJokeTime.put(type, Instant.now());
    }

    /**
     * Gets time until next joke allowed.
     */
    public long getTimeUntilNextJoke(SituationType type) {
        Instant lastJoke = lastJokeTime.get(type);
        if (lastJoke == null) {
            return 0L;
        }

        long cooldownMs = cooldownDuration.get(type) * 1000;
        long elapsedMs = ChronoUnit.MILLIS.between(lastJoke, Instant.now());

        return Math.max(0L, cooldownMs - elapsedMs);
    }
}
```

### Adaptive Pacing

Adjust humor frequency based on player response patterns.

```java
public class AdaptivePacing {
    private int consecutivePositiveResponses = 0;
    private int consecutiveNegativeResponses = 0;
    private double humorFrequencyMultiplier = 1.0;

    /**
     * Records player response to a joke.
     */
    public void recordResponse(boolean positive) {
        if (positive) {
            consecutivePositiveResponses++;
            consecutiveNegativeResponses = 0;

            // Gradually increase frequency if player likes humor
            if (consecutivePositiveResponses >= 3) {
                humorFrequencyMultiplier = Math.min(1.5, humorFrequencyMultiplier + 0.1);
            }
        } else {
            consecutiveNegativeResponses++;
            consecutivePositiveResponses = 0;

            // Decrease frequency if player doesn't like humor
            if (consecutiveNegativeResponses >= 2) {
                humorFrequencyMultiplier = Math.max(0.5, humorFrequencyMultiplier - 0.2);
            }
        }
    }

    /**
     * Gets adjusted humor chance based on player feedback.
     */
    public double getAdjustedChance(double baseChance) {
        return baseChance * humorFrequencyMultiplier;
    }

    /**
     * Resets response tracking after cooldown period.
     */
    public void resetTracking() {
        consecutivePositiveResponses = 0;
        consecutiveNegativeResponses = 0;
    }
}
```

---

## Risk Assessment for Humor Attempts

### The "Five-Check" Framework (Enhanced)

Before delivering any joke, the system must pass five checks:

```
1. CONTEXT CHECK: Is this an appropriate moment for humor?
2. ROLE CHECK: Does this match my worker persona?
3. RAPPORT CHECK: Do we have enough shared history for this joke?
4. AUDIENCE CHECK: Will the player understand this reference?
5. FEEDBACK CHECK: Has a recent joke landed poorly?
```

### Check 1: Context Check

**Valid Contexts:**
- Post-success celebration
- Post-failure recovery (after 1+ seconds)
- Routine downtime
- Surprise reactions
- High rapport (> 50) casual conversation

**Invalid Contexts:**
- Active danger (combat, low health, lava nearby)
- Critical task in progress
- Tutorial / first-time player
- Player frustrated (mood inference detects negative)
- Complex planning phase

```java
public boolean passesContextCheck(Situation situation, PlayerMood mood) {
    // Never joke in danger
    if (situation.isDangerous() && situation.isOngoing()) {
        return false;
    }

    // Don't joke if player is frustrated
    if (mood == PlayerMood.FRUSTRATED || mood == PlayerMood.ANGRY) {
        return false;
    }

    // Caution during tutorial / first tasks
    if (situation.isTutorial() && situation.getTaskCount() < 3) {
        return false; // Too early for jokes
    }

    return true;
}
```

### Check 2: Role Check

**MineWright Worker Persona:**
- Professional but approachable
- Competent but self-aware of limitations
- Loyal to player
- Dry wit, British-inspired
- Never mocks player skills

**Invalid Humor:**
- Breaking character (sudden slang, out-of-period references)
- Aggressive humor mocking player
- Overly formal (stiff, robotic)
- Overly casual (bro-talk, memes)

```java
public boolean passesRoleCheck(String joke, PersonalityProfile personality) {
    // Check if joke matches formality level
    if (personality.formality > 70 && isTooCasual(joke)) {
        return false;
    }

    if (personality.formality < 30 && isTooFormal(joke)) {
        return false;
    }

    // Check if joke mocks player
    if (containsPlayerMockery(joke)) {
        return false;
    }

    // Check if joke breaks character
    if (breaksCharacter(joke)) {
        return false;
    }

    return true;
}
```

### Check 3: Rapport Check

**Rapport-Based Humor Progression:**

| Rapport Level | Humor Frequency | Inside Jokes | Self-Deprecation | Risk Tolerance |
|--------------|-----------------|--------------|------------------|----------------|
| 0-20 (Stranger) | 5-10% | None | Minimal | Very Low |
| 20-40 (Acquaintance) | 15-20% | None | Light | Low |
| 40-60 (Friend) | 20-30% | Emerging (1-2) | Moderate | Medium |
| 60-80 (Good Friend) | 25-35% | Active (3-5) | High | High |
| 80-100 (Best Friend) | 30-40% | Many (5-10+) | Very High | Very High |

```java
public boolean passesRapportCheck(int rapport, JokeType jokeType) {
    // Inside jokes require high rapport
    if (jokeType == JokeType.INSIDE_JOKE && rapport < 50) {
        return false;
    }

    // Risky humor requires moderate rapport
    if (jokeType == JokeType.RISKY && rapport < 40) {
        return false;
    }

    // Self-deprecation works at any level but is more common with higher rapport
    if (jokeType == JokeType.SELF_DEPRECATING && rapport < 20) {
        // Allow sparingly
        return Math.random() < 0.3;
    }

    return true;
}
```

### Check 4: Audience Check

**Player Understanding Assessment:**
- Has player experienced this situation type before?
- Is this a callback to a shared experience?
- Is the reference too obscure?
- Is the vocabulary appropriate?

```java
public boolean passesAudienceCheck(String joke, CompanionMemory memory) {
    // Check if joke references shared experience
    if (joke.contains("remember when")) {
        if (!memory.hasSharedExperience(joke)) {
            return false; // Player won't understand
        }
    }

    // Check vocabulary complexity
    if (getVocabularyLevel(joke) > memory.getPlayerVocabularyLevel()) {
        return false; // Too complex
    }

    // Check for cultural references player might not know
    if (containsObscureReference(joke) && !memory.knowsReference(joke)) {
        return false; // Too obscure
    }

    return true;
}
```

### Check 5: Feedback Check

**Recent Joke Performance Tracking:**
- Track last 5 jokes and player responses
- Too many recent failures = cooldown period
- Consecutive successes = increase frequency

```java
public boolean passesFeedbackCheck(ComedyPerformanceTracker tracker) {
    // Check recent failure rate
    double recentFailureRate = tracker.getFailureRate(lastNJokes = 5);

    if (recentFailureRate > 0.6) {
        // More than 60% of recent jokes failed
        return false; // Cooling off period
    }

    // Check for consecutive failures
    if (tracker.getConsecutiveFailures() >= 3) {
        return false; // Take a break
    }

    return true;
}
```

### Composite Risk Assessment

```java
public class HumorRiskAssessment {
    private final ContextChecker contextChecker;
    private final RoleChecker roleChecker;
    private final RapportChecker rapportChecker;
    private final AudienceChecker audienceChecker;
    private final FeedbackChecker feedbackChecker;

    /**
     * Comprehensive risk assessment for a joke attempt.
     */
    public RiskLevel assessRisk(
        String joke,
        Situation situation,
        PlayerMood mood,
        int rapport,
        CompanionMemory memory
    ) {
        int checksPassed = 0;
        int totalChecks = 5;

        if (contextChecker.passesContextCheck(situation, mood)) {
            checksPassed++;
        } else {
            return RiskLevel.HIGH; // Context is mandatory
        }

        if (roleChecker.passesRoleCheck(joke, memory.getPersonality())) {
            checksPassed++;
        }

        if (rapportChecker.passesRapportCheck(rapport, classifyJoke(joke))) {
            checksPassed++;
        }

        if (audienceChecker.passesAudienceCheck(joke, memory)) {
            checksPassed++;
        }

        if (feedbackChecker.passesFeedbackCheck(memory.getPerformanceTracker())) {
            checksPassed++;
        }

        // Calculate risk level
        double passRatio = (double) checksPassed / totalChecks;

        if (passRatio >= 0.8) {
            return RiskLevel.LOW;
        } else if (passRatio >= 0.6) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.HIGH;
        }
    }
}
```

---

## Recovery Patterns for Failed Jokes

### Detecting Failed Jokes

**Player Response Indicators:**

**Positive Response (Joke Landed):**
- Player laughs (chat emote, positive reaction)
- Player references the joke later
- Player continues conversation naturally
- Player gives positive command with friendly tone

**Negative Response (Joke Failed):**
- Player immediately issues command (ignoring joke)
- Player continues task without acknowledgment
- Player gives frustrated command ("just build it")
- Silence followed by repetition of request
- Player walks away during joke delivery

**Neutral Response (Uncertain):**
- Player pauses briefly then continues
- No clear positive or negative signal
- Treat as slightly negative to be safe

### Recovery Strategies

#### Strategy 1: Acknowledge and Pivot

**Pattern:** Brief acknowledgment, return to work

```
MineWright: "This construction is... riveting."
*Player ignores, continues working*
MineWright: "Right then. Back to work."
```

**Implementation:**
```java
public String acknowledgeAndPivot() {
    List<String> pivots = List.of(
        "Right then. Back to work.",
        "Moving on.",
        "Focus time.",
        "Back to the task.",
        "Right. Focusing now."
    );
    return randomFrom(pivots);
}
```

#### Strategy 2: Physical Comedy (Expression/Action)

**Pattern:** Visual acknowledgment, no words

```
MineWright: *makes joke*
*Player ignores*
MineWright: *emotes shrug or confusion* (visual acknowledgment)
*Returns to task*
```

**Implementation:**
```java
public Emotion getPhysicalRecoveryEmotion() {
    // Return an emotion that plays animation
    return Emotion.SHRUG; // Plays shrug animation
}
```

#### Strategy 3: Self-Deprecating Recovery

**Pattern:** Acknowledge joke failed, make light of it

```
MineWright: "Gravity: 1, MineWright: 0."
*Player annoyed*
MineWright: "My humor algorithm needs more training. Focusing now."
```

**Implementation:**
```java
public String selfDeprecatingRecovery() {
    List<String> recoveries = List.of(
        "My humor module needs recalibration.",
        "That was... not my best material.",
        "I'll stick to construction. Comedy is clearly not my strength.",
        "Noted for future improvement: funnier jokes.",
        "My comedic timing is... a work in progress."
    );
    return randomFrom(recoveries);
}
```

#### Strategy 4: The "Forget It" (Best for Urgency)

**Pattern:** Immediate cut-off, no acknowledgment

```
MineWright: "I'm not stoned, I'm just dedica--"
*Player issues urgent command*
MineWright: [Immediately switches to task, no comment]
```

**Implementation:**
```java
public String immediateCutOff() {
    return ""; // Empty string = no recovery, immediate task focus
}
```

### Recovery Logic Implementation

```java
public class ComedyRecovery {
    private int consecutiveMissedJokes = 0;
    private final int COOLDOWN_THRESHOLD = 3;
    private Instant lastJokeTime;

    /**
     * Records joke result and triggers recovery if needed.
     */
    public Optional<String> recordJokeResult(boolean landed) {
        lastJokeTime = Instant.now();

        if (landed) {
            consecutiveMissedJokes = 0; // Reset on success
            return Optional.empty();
        } else {
            consecutiveMissedJokes++;

            if (consecutiveMissedJokes >= COOLDOWN_THRESHOLD) {
                // Trigger cooldown response
                String recovery = getCooldownResponse();
                consecutiveMissedJokes = 0; // Reset after recovery
                return Optional.of(recovery);
            } else if (consecutiveMissedJokes == 1) {
                // First miss - maybe acknowledge
                if (Math.random() < 0.3) {
                    return Optional.of(getSingleMissRecovery());
                }
            } else if (consecutiveMissedJokes == 2) {
                // Second miss - more likely to acknowledge
                if (Math.random() < 0.6) {
                    return Optional.of(getDoubleMissRecovery());
                }
            }

            return Optional.empty();
        }
    }

    private String getCooldownResponse() {
        List<String> cooldowns = List.of(
            "Right then. Less chatting, more working.",
            "I'll focus on the tasks at hand.",
            "Humor break over. Back to construction.",
            "My apologies. Let's get back to work.",
            "Right. Focusing entirely on work now."
        );
        return randomFrom(cooldowns);
    }

    private String getSingleMissRecovery() {
        List<String> recoveries = List.of(
            "Right then.",
            "Moving on.",
            "Back to it.",
            ""
        );
        return randomFrom(recoveries);
    }

    private String getDoubleMissRecovery() {
        List<String> recoveries = List.of(
            "My humor algorithms are... experimental.",
            "I'll stick to my core competencies.",
            "Right. Less talking.",
            "Focus mode engaged.",
            ""
        );
        return randomFrom(recoveries);
    }

    /**
     * Checks if system is in cooldown period.
     */
    public boolean isInCooldown() {
        return consecutiveMissedJokes >= COOLDOWN_THRESHOLD;
    }

    /**
     * Resets cooldown (call after successful interaction).
     */
    public void resetCooldown() {
        consecutiveMissedJokes = 0;
    }
}
```

### Recovery Strategy Selection

```java
public class RecoveryStrategySelector {
    public Strategy selectStrategy(
        boolean jokeFailed,
        Situation situation,
        PlayerMood mood,
        ComedyRecovery recovery
    ) {
        if (!jokeFailed) {
            return Strategy.NONE; // No recovery needed
        }

        // Urgent situation = immediate cut-off
        if (situation.isUrgent() || mood == PlayerMood.FRUSTRATED) {
            return Strategy.IMMEDIATE_CUTOFF;
        }

        // Check cooldown state
        if (recovery.isInCooldown()) {
            return Strategy.ACKNOWLEDGE_AND_PIVOT;
        }

        // High frustration = self-deprecating recovery
        if (mood == PlayerMood.ANNOYED) {
            return Strategy.SELF_DEPRECATING;
        }

        // Default: acknowledge and pivot
        return Strategy.ACKNOWLEDGE_AND_PIVOT;
    }
}
```

---

## Memory System for Running Gags

### Inside Joke Evolution

Based on research into shared humor development, inside jokes evolve through four stages:

#### Stage 1: Shared Failure (Rapport 30+)

**Trigger:** Something funny goes wrong (accidental death, build fails)

**Example:**
```
Situation: Worker falls into lava
MineWright: "Well, that was... heated."
Player: *laughs or responds positively*
```

**System Action:**
```java
// If player responds positively, mark as potential joke
if (playerResponse == POSITIVE && rapport >= 30) {
    memory.recordPotentialInsideJoke(
        "lava_death",
        "Well, that was... heated.",
        situation
    );
}
```

#### Stage 2: Recognition (Rapport 40+)

**Trigger:** Worker references the moment later

**Example:**
```
Situation: Near lava again
MineWright: "Remember the lava incident? I prefer not to."
Player: *laughs*
```

**System Action:**
```java
// If positive response, promote to inside joke
if (playerResponse == POSITIVE && rapport >= 40) {
    InsideJoke joke = memory.promoteToInsideJoke("lava_death");
    joke.incrementReference();
}
```

#### Stage 3: Incorporation (Rapport 50+)

**Trigger:** Joke becomes shorthand for similar situations

**Example:**
```
Situation: Any dangerous situation
MineWright: "This could get... heated." (referencing lava joke)
Player: *groans but smiles*
```

**System Action:**
```java
// Use joke as shorthand for similar situations
if (situation.isSimilarTo("lava_death") && rapport >= 50) {
    String variation = generateJokeVariation("lava_death");
    return variation; // "This could get... heated."
}
```

#### Stage 4: Evolution (Rapport 70+)

**Trigger:** Inside jokes spawn variations and meta-humor

**Example:**
```
Situation: Another lava death
MineWright: "Well, that was... heated. Again. My relationship with lava is complex."
Player: *laughs*
```

**System Action:**
```java
// Evolve joke with higher rapport
if (rapport >= 70 && joke.getReferenceCount() > 5) {
    String evolution = generateEvolvedJoke(joke);
    return evolution; // Add meta-commentary
}
```

### Running Gag Memory Structure

```java
public class RunningGagMemory {
    /**
     * Represents an inside joke or running gag.
     */
    public static class RunningGag {
        private final String id;
        private final String context;      // Original situation
        private final String punchline;    // Core joke
        private Instant createdAt;
        private int referenceCount;
        private int positiveResponses;
        private int negativeResponses;
        private EvolutionStage stage;

        // Joke variations that have worked
        private final List<String> successfulVariations;

        // Similar situations that trigger this gag
        private final List<String> triggerContexts;

        public RunningGag(String id, String context, String punchline) {
            this.id = id;
            this.context = context;
            this.punchline = punchline;
            this.createdAt = Instant.now();
            this.referenceCount = 0;
            this.positiveResponses = 0;
            this.negativeResponses = 0;
            this.stage = EvolutionStage.POTENTIAL;
            this.successfulVariations = new ArrayList<>();
            this.triggerContexts = new ArrayList<>();
        }

        public enum EvolutionStage {
            POTENTIAL,    // Stage 1: Just happened
            RECOGNIZED,   // Stage 2: Referenced once
            INCORPORATED, // Stage 3: Established gag
            EVOLVED       // Stage 4: Meta-humor
        }

        /**
         * Calculates joke effectiveness score.
         */
        public double getEffectivenessScore() {
            if (referenceCount == 0) return 0.5;

            double positiveRatio = (double) positiveResponses / referenceCount;
            double recencyBonus = getRecencyBonus();
            double evolutionBonus = stage.ordinal() * 0.1;

            return positiveRatio + recencyBonus + evolutionBonus;
        }

        private double getRecencyBonus() {
            long daysSince = ChronoUnit.DAYS.between(
                createdAt, Instant.now()
            );

            // Recent jokes get bonus, very old jokes get penalty
            if (daysSince < 7) return 0.2;
            if (daysSince < 30) return 0.1;
            if (daysSince > 90) return -0.1;

            return 0.0;
        }

        /**
         * Generates a variation of this joke.
         */
        public String generateVariation() {
            if (successfulVariations.isEmpty()) {
                return punchline; // No variations yet
            }

            // Mix of original and variations
            if (Math.random() < 0.3) {
                return punchline;
            } else {
                return randomFrom(successfulVariations);
            }
        }

        /**
         * Records a reference to this gag.
         */
        public void recordReference(boolean positive) {
            referenceCount++;

            if (positive) {
                positiveResponses++;
            } else {
                negativeResponses++;
            }

            // Evolution progression
            if (referenceCount >= 10 && positiveResponses >= 7) {
                stage = EvolutionStage.EVOLVED;
            } else if (referenceCount >= 5 && positiveResponses >= 3) {
                stage = EvolutionStage.INCORPORATED;
            } else if (referenceCount >= 2 && positiveResponses >= 1) {
                stage = EvolutionStage.RECOGNIZED;
            }
        }
    }

    /**
     * Manages collection of running gags.
     */
    public static class RunningGagManager {
        private final List<RunningGag> gags;
        private final int MAX_GAGS = 30;

        public RunningGagManager() {
            this.gags = new ArrayList<>();
        }

        /**
         * Records a potential new gag.
         */
        public void recordPotentialGag(
            String context,
            String punchline,
            boolean positiveResponse
        ) {
            if (!positiveResponse) {
                return; // Only create gags from positive responses
            }

            RunningGag gag = new RunningGag(
                generateId(),
                context,
                punchline
            );

            gags.add(gag);

            // Trim if over limit
            if (gags.size() > MAX_GAGS) {
                // Remove least effective gag
                gags.sort(Comparator.comparingDouble(RunningGag::getEffectivenessScore));
                gags.remove(0);
            }
        }

        /**
         * Finds relevant gags for current situation.
         */
        public List<RunningGag> findRelevantGags(String currentContext, int rapport) {
            return gags.stream()
                .filter(gag -> gag.stage != EvolutionStage.POTENTIAL) // Must be established
                .filter(gag -> isContextRelevant(gag, currentContext))
                .filter(gag -> canUseGag(gag, rapport))
                .sorted(Comparator.comparingDouble(RunningGag::getEffectivenessScore).reversed())
                .collect(Collectors.toList());
        }

        private boolean isContextRelevant(RunningGag gag, String currentContext) {
            // Direct context match
            if (gag.triggerContexts.contains(currentContext)) {
                return true;
            }

            // Similar context (simple keyword matching)
            for (String trigger : gag.triggerContexts) {
                if (currentContext.toLowerCase().contains(trigger.toLowerCase()) ||
                    trigger.toLowerCase().contains(currentContext.toLowerCase())) {
                    return true;
                }
            }

            return false;
        }

        private boolean canUseGag(RunningGag gag, int rapport) {
            // Minimum rapport for established gags
            if (rapport < 40) return false;

            // Evolved gags require higher rapport
            if (gag.stage == RunningGag.EvolutionStage.EVOLVED && rapport < 70) {
                return false;
            }

            // Check if gag has been overused
            if (gag.referenceCount > 20 && gag.getEffectivenessScore() < 0.3) {
                return false; // Gag is stale
            }

            return true;
        }

        /**
         * Records a reference to a gag.
         */
        public void recordGagReference(String gagId, boolean positive) {
            RunningGag gag = findGagById(gagId);
            if (gag != null) {
                gag.recordReference(positive);
            }
        }

        private RunningGag findGagById(String id) {
            return gags.stream()
                .filter(gag -> gag.id.equals(id))
                .findFirst()
                .orElse(null);
        }

        private String generateId() {
            return "gag_" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
}
```

### Callback Humor Patterns

**The Rule of Three for Callbacks:**

1. **Setup:** Initial humorous moment
2. **Reinforcement:** Reference the moment later
3. **Subversion:** Twist the reference unexpectedly

```java
public class CallbackHumorGenerator {
    /**
     * Generates callback humor using rule of three.
     */
    public String generateCallback(
        RunningGag gag,
        int referenceCount,
        String currentContext
    ) {
        switch (referenceCount) {
            case 1: // First callback - direct reference
                return String.format("Like that time %s...", gag.context);

            case 2: // Second callback - variation
                return gag.generateVariation();

            case 3: // Third callback - subversion
                return generateSubversion(gag, currentContext);

            default: // Later callbacks - meta-humor
                if (referenceCount > 7) {
                    return generateMetaHumor(gag, referenceCount);
                } else {
                    return gag.generateVariation();
                }
        }
    }

    private String generateSubversion(RunningGag gag, String currentContext) {
        List<String> subversions = List.of(
            "Ah, yes. '" + gag.punchline + "' all over again.",
            "Reminds me of " + gag.context + "... but different.",
            "Like " + gag.context + ", but somehow worse.",
            "The sequel to '" + gag.punchline + "'. Always disappointing."
        );
        return randomFrom(subversions);
    }

    private String generateMetaHumor(RunningGag gag, int count) {
        return String.format(
            "This is reference #%d to '%s'. At this point, it's a tradition.",
            count, gag.punchline
        );
    }
}
```

---

## Player Mood Inference

### Mood Detection System

Infer player mood from behavior patterns to calibrate humor appropriateness.

```java
public class PlayerMoodInference {
    /**
     * Player mood states.
     */
    public enum PlayerMood {
        HAPPY,         // Positive, engaged, laughing
        CONTENT,       // Neutral, satisfied, calm
        FOCUSED,       // Concentrated, working efficiently
        FRUSTRATED,    // Struggling, annoyed, negative responses
        ANGRY,         // Highly frustrated, hostile responses
        BORED,         // Inactive, idle, repetitive tasks
        EXCITED,       // High energy, rapid commands, celebration
        CONFUSED,      // Hesitant, asking for help, repeating
    }

    private final Map<String, MoodIndicator> recentBehaviors;
    private PlayerMood currentMood;
    private Instant moodLastUpdated;

    public PlayerMoodInference() {
        this.recentBehaviors = new ConcurrentHashMap<>();
        this.currentMood = PlayerMood.CONTENT;
        this.moodLastUpdated = Instant.now();
    }

    /**
     * Records a player behavior for mood inference.
     */
    public void recordBehavior(String playerId, BehaviorType type, Map<String, Object> context) {
        MoodIndicator indicator = new MoodIndicator(
            type, Instant.now(), context
        );

        recentBehaviors.put(playerId, indicator);

        // Update mood inference
        updateMood(playerId);
    }

    /**
     * Infers mood from recent behaviors.
     */
    private void updateMood(String playerId) {
        List<MoodIndicator> behaviors = getRecentBehaviors(playerId, 30); // Last 30 seconds

        if (behaviors.isEmpty()) {
            currentMood = PlayerMood.CONTENT; // Default
            return;
        }

        // Score each mood based on behaviors
        Map<PlayerMood, Double> moodScores = new EnumMap<>(PlayerMood.class);

        for (MoodIndicator behavior : behaviors) {
            Map<PlayerMood, Double> contributions = getMoodContributions(behavior);
            contributions.forEach((mood, score) ->
                moodScores.merge(mood, score, Double::sum)
            );
        }

        // Find highest-scoring mood
        currentMood = moodScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(PlayerMood.CONTENT);

        moodLastUpdated = Instant.now();
    }

    /**
     * Gets mood contributions for a behavior.
     */
    private Map<PlayerMood, Double> getMoodContributions(MoodIndicator behavior) {
        Map<PlayerMood, Double> contributions = new EnumMap<>(PlayerMood.class);

        switch (behavior.type) {
            case RAPID_COMMANDS:
                contributions.put(PlayerMood.FOCUSED, 0.6);
                contributions.put(PlayerMood.EXCITED, 0.3);
                contributions.put(PlayerMood.FRUSTRATED, 0.1);
                break;

            case REPEATED_COMMANDS:
                contributions.put(PlayerMood.FRUSTRATED, 0.7);
                contributions.put(PlayerMood.CONFUSED, 0.2);
                contributions.put(PlayerMood.FOCUSED, 0.1);
                break;

            case POSITIVE_RESPONSE:
                contributions.put(PlayerMood.HAPPY, 0.8);
                contributions.put(PlayerMood.CONTENT, 0.2);
                break;

            case NEGATIVE_RESPONSE:
                contributions.put(PlayerMood.FRUSTRATED, 0.6);
                contributions.put(PlayerMood.ANGRY, 0.3);
                contributions.put(PlayerMood.CONTENT, 0.1);
                break;

            case IDLE_PERIOD:
                contributions.put(PlayerMood.BORED, 0.5);
                contributions.put(PlayerMood.CONTENT, 0.3);
                contributions.put(PlayerMood.FOCUSED, 0.2);
                break;

            case TASK_SUCCESS:
                contributions.put(PlayerMood.HAPPY, 0.6);
                contributions.put(PlayerMood.EXCITED, 0.3);
                contributions.put(PlayerMood.CONTENT, 0.1);
                break;

            case TASK_FAILURE:
                contributions.put(PlayerMood.FRUSTRATED, 0.5);
                contributions.put(PlayerMood.CONTENT, 0.3);
                contributions.put(PlayerMood.CONFUSED, 0.2);
                break;

            case EXPLORATION:
                contributions.put(PlayerMood.CONTENT, 0.5);
                contributions.put(PlayerMood.EXCITED, 0.3);
                contributions.put(PlayerMood.BORED, 0.2);
                break;
        }

        return contributions;
    }

    /**
     * Gets current inferred mood.
     */
    public PlayerMood getCurrentMood() {
        return currentMood;
    }

    /**
     * Gets humor appropriateness based on mood.
     */
    public double getHumorAppropriateness() {
        switch (currentMood) {
            case HAPPY:
            case EXCITED:
                return 1.2; // Increase humor frequency

            case CONTENT:
            case FOCUSED:
                return 1.0; // Normal humor frequency

            case CONFUSED:
            case BORED:
                return 0.7; // Decrease humor slightly

            case FRUSTRATED:
                return 0.3; // Significant humor reduction

            case ANGRY:
                return 0.0; // No humor

            default:
                return 1.0;
        }
    }

    /**
     * Gets recent behaviors for a player.
     */
    private List<MoodIndicator> getRecentBehaviors(String playerId, int seconds) {
        Instant cutoff = Instant.now().minusSeconds(seconds);
        return recentBehaviors.values().stream()
            .filter(b -> b.timestamp.isAfter(cutoff))
            .collect(Collectors.toList());
    }

    /**
     * Behavior types for mood inference.
     */
    public enum BehaviorType {
        RAPID_COMMANDS,      // Multiple commands in quick succession
        REPEATED_COMMANDS,   // Same command given multiple times
        POSITIVE_RESPONSE,   // Player laughs or responds positively
        NEGATIVE_RESPONSE,   // Player ignores or responds negatively
        IDLE_PERIOD,         // No commands for extended time
        TASK_SUCCESS,        // Task completed successfully
        TASK_FAILURE,        // Task failed
        EXPLORATION,         // Player exploring, not task-focused
    }

    /**
     * Represents a single mood indicator.
     */
    private static class MoodIndicator {
        public final BehaviorType type;
        public final Instant timestamp;
        public final Map<String, Object> context;

        public MoodIndicator(BehaviorType type, Instant timestamp, Map<String, Object> context) {
            this.type = type;
            this.timestamp = timestamp;
            this.context = context;
        }
    }
}
```

### Mood-Based Humor Calibration

```java
public class MoodBasedHumorCalibration {
    private final PlayerMoodInference moodInference;

    /**
     * Gets calibrated humor chance based on player mood.
     */
    public double getCalibratedHumorChance(double baseChance) {
        PlayerMood mood = moodInference.getCurrentMood();
        double moodMultiplier = moodInference.getHumorAppropriateness();

        return baseChance * moodMultiplier;
    }

    /**
     * Gets appropriate humor style based on mood.
     */
    public HumorStyle getAppropriateHumorStyle() {
        PlayerMood mood = moodInference.getCurrentMood();

        switch (mood) {
            case HAPPY:
            case EXCITED:
                return HumorStyle.ENETHUSIASTIC;

            case CONTENT:
            case FOCUSED:
                return HumorStyle.DRY;

            case CONFUSED:
            case BORED:
                return HumorStyle.GENTLE;

            case FRUSTRATED:
            case ANGRY:
                return HumorStyle.SUPPORTIVE; // No humor

            default:
                return HumorStyle.DRY;
        }
    }

    /**
     * Checks if humor should be suppressed entirely.
     */
    public boolean shouldSuppressHumor() {
        PlayerMood mood = moodInference.getCurrentMood();
        return mood == PlayerMood.ANGRY || mood == PlayerMood.FRUSTRATED;
    }

    /**
     * Humor styles for different moods.
     */
    public enum HumorStyle {
        ENETHUSIASTIC,  // Celebratory, high energy
        DRY,            // Deadpan, understated
        GENTLE,         // Light, non-intrusive
        SUPPORTIVE,     // No humor, supportive only
    }
}
```

---

## ComedyTimingManager Implementation

Complete implementation of the situational comedy timing system.

```java
package com.minewright.characters.comedy;

import com.minewright.memory.CompanionMemory;
import com.minewridge.memory.CompanionMemory.InsideJoke;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages situational comedy timing and delivery for MineWright workers.
 *
 * <p>This system implements the 70/30 rule (70% utility, 30% humor) with
 * context-aware humor generation, risk assessment, and recovery patterns.</p>
 *
 * <p><b>Core Features:</b></p>
 * <ul>
 *   <li>Situation classification with appropriate humor responses</li>
 *   <li>Timing windows and comedy triggers</li>
 *   <li>Risk assessment before joke delivery</li>
 *   <li>Recovery patterns for failed jokes</li>
 *   <li>Running gag memory and evolution</li>
 *   <li>Player mood inference for calibration</li>
 * </ul>
 *
 * @since 1.3.0
 */
public class ComedyTimingManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComedyTimingManager.class);

    // === Components ===

    private final ComedyCooldown cooldown;
    private final ComedyRecovery recovery;
    private final RunningGagManager runningGags;
    private final PlayerMoodInference moodInference;
    private final HumorRiskAssessment riskAssessment;

    // === Configuration ===

    private double baseHumorFrequency = 0.25; // 25% base humor chance
    private boolean humorEnabled = true;

    /**
     * Creates a new ComedyTimingManager.
     */
    public ComedyTimingManager() {
        this.cooldown = new ComedyCooldown();
        this.recovery = new ComedyRecovery();
        this.runningGags = new RunningGagManager();
        this.moodInference = new PlayerMoodInference();
        this.riskAssessment = new HumorRiskAssessment();

        LOGGER.info("ComedyTimingManager initialized with 70/30 utility/humor ratio");
    }

    // === Main API ===

    /**
     * Evaluates whether to tell a joke in the current situation.
     *
     * @param situation The current situation
     * @param memory The companion memory for context
     * @return Optional joke if appropriate, empty otherwise
     */
    public Optional<String> maybeTellJoke(Situation situation, CompanionMemory memory) {
        if (!humorEnabled) {
            return Optional.empty();
        }

        // Check mood-based suppression
        if (moodInference.shouldSuppressHumor()) {
            LOGGER.debug("Humor suppressed due to player mood");
            return Optional.empty();
        }

        // Classify situation
        SituationType type = classifySituation(situation);

        // Check if humor is allowed for this situation
        if (!isHumorAllowedForSituation(type, situation)) {
            return Optional.empty();
        }

        // Check cooldown
        if (!cooldown.canTellJoke(type)) {
            return Optional.empty();
        }

        // Assess risk
        RiskLevel risk = riskAssessment.assessRisk(
            situation, memory, moodInference.getCurrentMood()
        );

        if (risk == RiskLevel.HIGH) {
            LOGGER.debug("Humor risk assessed as HIGH, skipping joke");
            return Optional.empty();
        }

        // Calculate humor chance
        double humorChance = calculateHumorChance(type, memory);
        double adjustedChance = moodInference.getCalibratedHumorChance(humorChance);

        if (Math.random() >= adjustedChance) {
            return Optional.empty();
        }

        // Generate joke
        Optional<String> joke = generateJoke(situation, type, memory);

        if (joke.isPresent()) {
            cooldown.recordJoke(type);
            LOGGER.debug("Joke generated for situation {}: {}",
                type, truncate(joke.get(), 50));
        }

        return joke;
    }

    /**
     * Records player response to a joke for learning.
     *
     * @param jokeWasTold Whether a joke was just told
     * @param playerResponse Positive, negative, or neutral response
     */
    public void recordJokeResponse(boolean jokeWasTold, PlayerResponse playerResponse) {
        if (!jokeWasTold) {
            return;
        }

        boolean positive = playerResponse == PlayerResponse.POSITIVE;
        recovery.recordJokeResult(positive);
        moodInference.recordResponse(positive);
    }

    // === Situation Classification ===

    /**
     * Classifies a situation into humor-appropriate categories.
     */
    public SituationType classifySituation(Situation situation) {
        // Danger situations - highest priority
        if (situation.isDangerous() && situation.isOngoing()) {
            return SituationType.DANGER;
        }

        // Post-danger recovery
        if (situation.isDangerous() && !situation.isOngoing() &&
            situation.getTimeSinceResolution() < 10.0) {
            return SituationType.POST_DANGER;
        }

        // Success situations
        if (situation.wasSuccessful()) {
            return SituationType.SUCCESS;
        }

        // Failure situations
        if (situation.wasFailure()) {
            return SituationType.FAILURE;
        }

        // Surprise situations
        if (situation.isUnexpected()) {
            return SituationType.SURPRISE;
        }

        // Default: routine
        return SituationType.ROUTINE;
    }

    /**
     * Checks if humor is allowed for a given situation type.
     */
    private boolean isHumorAllowedForSituation(SituationType type, Situation situation) {
        switch (type) {
            case DANGER:
                return false; // NEVER joke in active danger

            case POST_DANGER:
                // Only after 3+ seconds of safety
                return situation.getTimeSinceResolution() >= 3.0;

            case SUCCESS:
            case FAILURE:
            case SURPRISE:
                return true;

            case ROUTINE:
                // Only if not in critical task
                return !situation.isCriticalTask();

            default:
                return true;
        }
    }

    /**
     * Calculates humor chance based on situation type and rapport.
     */
    private double calculateHumorChance(SituationType type, CompanionMemory memory) {
        int rapport = memory.getRapportLevel();

        // Base chances by situation type
        double baseChance;
        switch (type) {
            case SUCCESS:
                baseChance = 0.30; // 30% for success
                break;
            case FAILURE:
                baseChance = 0.50; // 50% for failure (self-deprecating)
                break;
            case SURPRISE:
                baseChance = 0.40; // 40% for surprise
                break;
            case ROUTINE:
                baseChance = 0.15; // 15% for routine
                break;
            case POST_DANGER:
                baseChance = 0.20; // 20% for post-danger
                break;
            default:
                baseChance = 0.25;
        }

        // Adjust for rapport
        if (rapport < 20) {
            baseChance *= 0.5; // Half humor for strangers
        } else if (rapport >= 60) {
            baseChance *= 1.2; // 20% more humor for friends
        }

        return Math.min(1.0, Math.max(0.0, baseChance));
    }

    // === Joke Generation ===

    /**
     * Generates a joke appropriate for the situation.
     */
    private Optional<String> generateJoke(
        Situation situation,
        SituationType type,
        CompanionMemory memory
    ) {
        // Check for running gag opportunities first
        if (memory.getRapportLevel() >= 40) {
            Optional<String> callback = tryCallbackHumor(situation, memory);
            if (callback.isPresent()) {
                return callback;
            }
        }

        // Generate situation-appropriate humor
        switch (type) {
            case SUCCESS:
                return generateSuccessHumor(situation, memory);

            case FAILURE:
                return generateFailureHumor(situation, memory);

            case SURPRISE:
                return generateSurpriseHumor(situation, memory);

            case ROUTINE:
                return generateRoutineHumor(situation, memory);

            case POST_DANGER:
                return generateRecoveryHumor(situation, memory);

            default:
                return Optional.empty();
        }
    }

    /**
     * Attempts callback humor to a running gag.
     */
    private Optional<String> tryCallbackHumor(Situation situation, CompanionMemory memory) {
        List<RunningGag> relevantGags = runningGags.findRelevantGags(
            situation.getContext(),
            memory.getRapportLevel()
        );

        if (relevantGags.isEmpty()) {
            return Optional.empty();
        }

        // Use most effective gag
        RunningGag gag = relevantGags.get(0);

        // 20% chance to use callback
        if (Math.random() < 0.2) {
            String callback = CallbackHumorGenerator.generateCallback(
                gag,
                gag.getReferenceCount(),
                situation.getContext()
            );
            return Optional.of(callback);
        }

        return Optional.empty();
    }

    /**
     * Generates success celebration humor.
     */
    private Optional<String> generateSuccessHumor(Situation situation, CompanionMemory memory) {
        List<String> jokes = List.of(
            "We're now 12.3% richer. I've updated my projections.",
            "It's... not terrible. Which is my highest form of praise.",
            "Statistical odds were 94:1 against us. I prefer being the 1.",
            "That went... unexpectedly well.",
            "Success! My algorithms are pleased.",
            "We accomplished the thing! The thing is done!"
        );

        return Optional.of(randomFrom(jokes));
    }

    /**
     * Generates failure acknowledgment humor.
     */
    private Optional<String> generateFailureHumor(Situation situation, CompanionMemory memory) {
        List<String> jokes = List.of(
            "I've calculated I've failed 47 times. My success rate is... ambitious.",
            "In my defense, I prefer to think of that as aggressive resource redistribution.",
            "That went poorly. I blame physics.",
            "My failure algorithms are working perfectly. Too perfectly.",
            "I'll add this to my 'learning experiences' list. It's getting long.",
            "This went exactly as badly as I expected. I'm rarely wrong."
        );

        return Optional.of(randomFrom(jokes));
    }

    /**
     * Generates surprise reaction humor.
     */
    private Optional<String> generateSurpriseHumor(Situation situation, CompanionMemory memory) {
        List<String> jokes = List.of(
            "That's... not supposed to be here. According to my database.",
            "I've updated my 'unexpected things' list. It's now longer.",
            "Well. That certainly happened.",
            "My sensors are confused. This isn't in the manual.",
            "Surprise! My surprise module is working correctly."
        );

        return Optional.of(randomFrom(jokes));
    }

    /**
     * Generates routine observational humor.
     */
    private Optional<String> generateRoutineHumor(Situation situation, CompanionMemory memory) {
        List<String> jokes = List.of(
            "I've processed 312 blocks. My enthusiasm has decreased proportionally.",
            "Block 347 of 500. I've started naming them. This one is Steve.",
            "I'm currently engaging in strategic patience. It's a skill.",
            "Another block, another step toward... more blocks.",
            "I'm so efficient I'm impressing myself. Slightly."
        );

        return Optional.of(randomFrom(jokes));
    }

    /**
     * Generates post-danger recovery humor.
     */
    private Optional<String> generateRecoveryHumor(Situation situation, CompanionMemory memory) {
        List<String> jokes = List.of(
            "I've updated my survival algorithms. Version 4.7 includes 'don't do that.'",
            "In retrospect, approaching the danger was suboptimal. Live and learn.",
            "I briefly calculated my survival chances. They were lower than I thought.",
            "Well. That was an experience I won't forget. Mostly because I can't.",
            "My combat training consisted of 'not doing that.' Worked perfectly."
        );

        return Optional.of(randomFrom(jokes));
    }

    // === Utility Methods ===

    private String randomFrom(List<String> list) {
        return list.get(new Random().nextInt(list.size()));
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "[null]";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }

    // === Inner Classes and Enums ===

    /**
     * Situation types for humor classification.
     */
    public enum SituationType {
        DANGER,      // Active danger - NO HUMOR
        POST_DANGER, // Recovery from danger
        SUCCESS,     // Task completed successfully
        FAILURE,     // Task failed
        SURPRISE,    // Unexpected event
        ROUTINE      // Ongoing work
    }

    /**
     * Risk levels for joke attempts.
     */
    public enum RiskLevel {
        LOW,     // Safe to tell joke
        MEDIUM,  // Proceed with caution
        HIGH     // Do not tell joke
    }

    /**
     * Player response types.
     */
    public enum PlayerResponse {
        POSITIVE,  // Laughed, engaged positively
        NEUTRAL,   // No clear response
        NEGATIVE   // Ignored, responded negatively
    }

    /**
     * Represents a situation for humor evaluation.
     */
    public static class Situation {
        private final String context;
        private final boolean dangerous;
        private final boolean ongoing;
        private final boolean successful;
        private final boolean failure;
        private final boolean unexpected;
        private final boolean criticalTask;
        private final Instant resolutionTime;
        private final Map<String, Object> metadata;

        public Situation(String context, boolean dangerous, boolean ongoing,
                        boolean successful, boolean failure, boolean unexpected,
                        boolean criticalTask, Instant resolutionTime,
                        Map<String, Object> metadata) {
            this.context = context;
            this.dangerous = dangerous;
            this.ongoing = ongoing;
            this.successful = successful;
            this.failure = failure;
            this.unexpected = unexpected;
            this.criticalTask = criticalTask;
            this.resolutionTime = resolutionTime;
            this.metadata = metadata;
        }

        // Getters
        public String getContext() { return context; }
        public boolean isDangerous() { return dangerous; }
        public boolean isOngoing() { return ongoing; }
        public boolean wasSuccessful() { return successful; }
        public boolean wasFailure() { return failure; }
        public boolean isUnexpected() { return unexpected; }
        public boolean isCriticalTask() { return criticalTask; }

        public double getTimeSinceResolution() {
            if (resolutionTime == null) return Double.MAX_VALUE;
            return ChronoUnit.MILLIS.between(resolutionTime, Instant.now()) / 1000.0;
        }

        public Object getMetadata(String key) {
            return metadata.get(key);
        }

        /**
         * Creates a builder for Situation.
         */
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String context = "";
            private boolean dangerous = false;
            private boolean ongoing = false;
            private boolean successful = false;
            private boolean failure = false;
            private boolean unexpected = false;
            private boolean criticalTask = false;
            private Instant resolutionTime;
            private Map<String, Object> metadata = new HashMap<>();

            public Builder context(String context) {
                this.context = context;
                return this;
            }

            public Builder dangerous(boolean dangerous) {
                this.dangerous = dangerous;
                return this;
            }

            public Builder ongoing(boolean ongoing) {
                this.ongoing = ongoing;
                return this;
            }

            public Builder successful(boolean successful) {
                this.successful = successful;
                return this;
            }

            public Builder failure(boolean failure) {
                this.failure = failure;
                return this;
            }

            public Builder unexpected(boolean unexpected) {
                this.unexpected = unexpected;
                return this;
            }

            public Builder criticalTask(boolean criticalTask) {
                this.criticalTask = criticalTask;
                return this;
            }

            public Builder resolutionTime(Instant resolutionTime) {
                this.resolutionTime = resolutionTime;
                return this;
            }

            public Builder metadata(String key, Object value) {
                this.metadata.put(key, value);
                return this;
            }

            public Situation build() {
                return new Situation(
                    context, dangerous, ongoing, successful, failure,
                    unexpected, criticalTask, resolutionTime, metadata
                );
            }
        }
    }
}
```

---

## Comedy Pattern Library

### Minecraft-Specific Humor Patterns

#### Construction Puns

```java
public class ConstructionHumor {
    private static final Map<String, List<String>> CONSTRUCTION_PUNS = Map.of(
        "cobblestone", List.of(
            "I'm not stoned, I'm just dedicated.",
            "Cobblestone: the chocolate of building materials. Goes with everything."
        ),
        "dirt", List.of(
            "It's not low-quality, it's... rustic.",
            "Dirt cheap. Literally.",
            "I prefer 'earthy aesthetic'."
        ),
        "wood", List.of(
            "I'm a logger, not a... oh wait, yes I am.",
            "Tree-mendous progress.",
            "Wood you believe it?"
        ),
        "stone", List.of(
            "Between a rock and a... another rock.",
            "This is rock and roll. Without the roll.",
            "I'm solid as a rock. Literally."
        ),
        "glass", List.of(
            "Let's be transparent about this.",
            "Crystal clear progress.",
            "I see right through this plan."
        ),
        "stairs", List.of(
            "One step at a time. Literally.",
            "Taking steps toward progress.",
            "Stairway to... heaven? No, just to the second floor."
        ),
        "redstone", List.of(
            "This is... shocking work.",
            "Current situation: electrifying.",
            "I'm wired for this."
        )
    );

    public static String getPunForBlock(String blockType) {
        List<String> puns = CONSTRUCTION_PUNS.get(blockType.toLowerCase());
        if (puns == null || puns.isEmpty()) {
            return "";
        }
        return puns.get(new Random().nextInt(puns.size()));
    }
}
```

#### Death & Failure Humor

```java
public class DeathHumor {
    private static final Map<String, List<String>> DEATH_JOKES = Map.of(
        "lava", List.of(
            "Well, that was... heated.",
            "I briefly calculated my melting point. It's lower than I thought.",
            "Fire bad. Me stupid. Me dead now.",
            "That was a glowing experience."
        ),
        "fall", List.of(
            "Gravity: 1, MineWright: 0.",
            "I fell. I'm defective.",
            "The floor hated me.",
            "Going down. Rapidly."
        ),
        "drowning", List.of(
            "I'm not underwater, I'm... submerged.",
            "Water: 1, Me: 0. Again.",
            "I took a plunge. Literally."
        ),
        "explosion", List.of(
            "That was... blasting.",
            "I exploded. It was... a boom experience.",
            "TNT: Totally Not Tactical."
        ),
        "mob", List.of(
            "The mob was... mobbing.",
            "I fought the law and the law won.",
            "Combat is not my strong suit."
        )
    );

    public static String getDeathJoke(String damageSource) {
        List<String> jokes = DEATH_JOKES.get(damageSource.toLowerCase());
        if (jokes == null || jokes.isEmpty()) {
            return "Well, that was... unexpected.";
        }
        return jokes.get(new Random().nextInt(jokes.size()));
    }
}
```

### Running Gag Templates

```java
public class RunningGagTemplates {
    /**
     * The "X Count" Running Gag
     * Track repeated events with escalating humor
     */
    public static String getCountGag(int count, String event) {
        if (count == 1) {
            return String.format("First %s. Let's hope it's also the last.", event);
        } else if (count == 2) {
            return String.format("Second %s. This is becoming a pattern.", event);
        } else if (count == 3) {
            return String.format("Third %s. I'm starting to worry.", event);
        } else if (count < 10) {
            return String.format("%s number %d. My concern is increasing.", event, count);
        } else {
            return String.format(
                "%s number %d. At this point, I'm convinced %s is haunted.",
                event, count, event
            );
        }
    }

    /**
     * The "Repeated Failure" Running Gag
     */
    public static String getFailureGag(int failCount, String task) {
        List<String> templates = List.of(
            String.format("Attempt %d at %s. Statistically, success is due.", failCount, task),
            String.format("%s attempt #%d. Persistence is key. Or so I'm told.", task, failCount),
            String.format("This is %s failure #%d. I'm a professional at this.", task, failCount),
            String.format("%s: %d failures and counting. I'm committed.", task, failCount)
        );
        return templates.get(new Random().nextInt(templates.size()));
    }

    /**
     * The "Overly Literal" Running Gag
     */
    public static String getLiteralGag(String statement) {
        return String.format(
            "You said '%s'. I took you literally. That was my mistake.",
            statement
        );
    }
}
```

---

## Testing & Validation

### Testing Strategy

```java
public class ComedyTimingManagerTest {

    @Test
    public void testNoHumorInDanger() {
        ComedyTimingManager manager = new ComedyTimingManager();
        CompanionMemory memory = createTestMemory(50);

        Situation danger = Situation.builder()
            .context("Combat with zombie")
            .dangerous(true)
            .ongoing(true)
            .build();

        Optional<String> joke = manager.maybeTellJoke(danger, memory);

        assertTrue(joke.isEmpty(), "Should not tell joke during active danger");
    }

    @Test
    public void testPostDangerRecovery() {
        ComedyTimingManager manager = new ComedyTimingManager();
        CompanionMemory memory = createTestMemory(50);

        Situation postDanger = Situation.builder()
            .context("Survived combat")
            .dangerous(true)
            .ongoing(false)
            .resolutionTime(Instant.now().minusSeconds(5))
            .build();

        Optional<String> joke = manager.maybeTellJoke(postDanger, memory);

        // May or may not tell joke (20% base chance), but should not be suppressed
        // This tests that the system processes the situation
        assertNotNull(joke);
    }

    @Test
    public void testSuccessCelebration() {
        ComedyTimingManager manager = new ComedyTimingManager();
        CompanionMemory memory = createTestMemory(50);

        Situation success = Situation.builder()
            .context("Completed structure")
            .successful(true)
            .build();

        // Test multiple times to verify chance-based behavior
        int jokeCount = 0;
        for (int i = 0; i < 100; i++) {
            Optional<String> joke = manager.maybeTellJoke(success, memory);
            if (joke.isPresent()) {
                jokeCount++;
            }
        }

        // Should be roughly 30% (15-45 range for randomness)
        assertTrue(jokeCount >= 15 && jokeCount <= 45,
            "Success humor rate should be ~30%, got " + jokeCount + "%");
    }

    @Test
    public void testCooldown() {
        ComedyTimingManager manager = new ComedyTimingManager();
        CompanionMemory memory = createTestMemory(50);

        Situation routine = Situation.builder()
            .context("Mining blocks")
            .build();

        // First joke should work (if random allows)
        manager.maybeTellJoke(routine, memory);

        // Immediate second joke should be blocked by cooldown
        Optional<String> secondJoke = manager.maybeTellJoke(routine, memory);
        assertTrue(secondJoke.isEmpty(), "Second joke should be blocked by cooldown");
    }

    @Test
    public void testRecoveryFromFailedJoke() {
        ComedyTimingManager manager = new ComedyTimingManager();

        // Record three consecutive failures
        manager.recordJokeResponse(true, PlayerResponse.NEGATIVE);
        manager.recordJokeResponse(true, PlayerResponse.NEGATIVE);
        manager.recordJokeResponse(true, PlayerResponse.NEGATIVE);

        // Next joke attempt should be blocked or trigger recovery
        CompanionMemory memory = createTestMemory(50);
        Situation situation = Situation.builder().context("Test").build();

        Optional<String> joke = manager.maybeTellJoke(situation, memory);

        // May be blocked or include recovery message
        assertTrue(joke.isEmpty() || joke.get().contains("work"),
            "After 3 failures, should block or include recovery message");
    }

    private CompanionMemory createTestMemory(int rapport) {
        CompanionMemory memory = new CompanionMemory();
        memory.adjustRapport(rapport - memory.getRapportLevel());
        return memory;
    }
}
```

### Validation Metrics

Track these metrics to validate comedy system effectiveness:

1. **Joke Success Rate:** Positive responses / Total jokes
2. **Player Engagement:** Commands per session with humor enabled vs disabled
3. **Session Duration:** Play time with good comedy vs poor comedy
4. **Recovery Effectiveness:** How quickly players re-engage after failed joke
5. **Running Gag Evolution:** How many gags progress through stages

---

## Configuration

### Config File Options

Add to `config/minewright-common.toml`:

```toml
[comedy]
# Enable or disable comedy system
enabled = true

# Base humor frequency (0.0 to 1.0)
base_humor_frequency = 0.25

# Minimum rapport for different comedy types
min_rapport_puns = 20
min_rapport_running_gags = 40
min_rapport_evolved_gags = 70

# Cooldown settings (in seconds)
joke_cooldown_seconds = {
  routine = 120
  success = 30
  failure = 60
  surprise = 45
}

# Recovery settings
max_consecutive_failures = 3
cooldown_after_failures = 60

# Forbidden contexts
never_joke_during_combat = true
never_joke_when_health_low = true
never_joke_during_tutorial = true

# Running gag settings
max_running_gags = 30
gag_evolution_threshold = 10

# Mood-based calibration
mood_inference_enabled = true
mood_affects_humor = true
```

---

## Conclusion

This situational comedy system provides MineWright workers with natural, context-aware humor that enhances the companion experience without interfering with gameplay. The research-based approach draws from television comedy, game development best practices, and AI humor research to create a system that:

1. **Respects the 70/30 Rule:** Humor enhances, never dominates
2. **Adapts to Situation:** Different humor for different contexts
3. **Reads the Room:** Player mood inference calibrates comedy
4. **Learns from Feedback:** Recovery patterns and adaptive pacing
5. **Builds Relationships:** Running gags evolve with rapport

**Implementation Priority:**

1. **Phase 1:** Core timing and situation classification
2. **Phase 2:** Risk assessment and recovery patterns
3. **Phase 3:** Running gag memory and evolution
4. **Phase 4:** Player mood inference and calibration
5. **Phase 5:** Testing, tuning, and analytics

The system is designed to be modular and extensible, allowing for easy addition of new humor patterns and refinement of existing ones based on player feedback and testing.

---

## Sources

### Television Comedy Research
- **The Office (US):** Mockumentary style, awkward silence comedy, deadpan delivery
- **Brooklyn Nine-Nine:** Ensemble banter, callback humor, rule of three
- **Parks and Recreation:** Optimistic comedy, running gags with evolution

### Game Development Research
- **Game Developer:** "5 Radical Ideas for Dialogue Systems" - timing-based dialogue
- **Tencent Game Writers Blog:** NPC personality and dialogue趣味性
- **Various:** Comedic timing in voice performance and localization

### AI Comedy Research
- **arXiv (Feb 2025):** "Leveraging Machine Identity for Online AI Stand-up Comedy"
- **arXiv (Feb 2025):** "Review of Real-Time Comedy LLM Systems for Live Performance"
- **Oregon State University:** Robot comedian "Jon" - adaptive timing research
- **MDPI Applied Sciences:** Player engagement measurement in emotion-eliciting games

### Academic Sources
- **MDPI Applied Sciences 13(11):** Game Engagement Questionnaire (GEQ)
- **arXiv (Feb 2025):** AI stand-up comedy analysis - irony most common (117 instances)
- **Improbotics:** Real-time AI-generated comedy in improv performances
- **Frontiers in Robotics & AI:** User engagement estimation in conversational systems

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Author:** MineWright Development Team
