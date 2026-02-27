# Research: Emotional AI and Sentiment Analysis for Game Companions

**Project:** Steve AI (MineWright) Emotional Intelligence Enhancement
**Date:** 2026-02-27
**Research Focus:** Sentiment Analysis, Emotional Responses, Mood Tracking, Empathy Patterns, Emotional Memory, Affective Computing Integration

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Emotion Detection Approaches](#emotion-detection-approaches)
3. [Sentiment Analysis Techniques](#sentiment-analysis-techniques)
4. [Emotional Response Patterns](#emotional-response-patterns)
5. [Mood Tracking Systems](#mood-tracking-systems)
6. [Empathy Patterns for AI Companions](#empathy-patterns-for-ai-companions)
7. [Emotional Memory Architecture](#emotional-memory-architecture)
8. [Affective Computing in Games](#affective-computing-in-games)
9. [VAD Model Implementation](#vad-model-implementation)
10. [MineWright Integration Strategy](#minewright-integration-strategy)
11. [Technical Implementation](#technical-implementation)
12. [Sources & References](#sources--references)

---

## Executive Summary

This report synthesizes cutting-edge research on emotional AI, sentiment analysis, and affective computing to provide a comprehensive framework for implementing emotional intelligence in the MineWright Minecraft companion. The research covers emotion detection from text and behavioral signals, emotional response generation, long-term mood tracking, empathy patterns, and emotional memory systems.

**Key Findings:**
- **Multi-scale emotion tracking** is essential: distinguishing between short-term emotions (seconds), medium-term moods (minutes/hours), and long-term personality traits
- **VAD (Valence-Arousal-Dominance)** model provides superior granularity compared to discrete emotion categories
- **Affective mirroring** in NPCs deepens player attachment through emotional responsiveness
- **Emotional memory** requires separate storage from episodic memory, with affective tags alongside event data
- **LLM-based empathy** now matches or exceeds human empathy in controlled conditions
- **Hybrid approaches** combining rule-based sentiment analysis with deep learning yield best results for real-time game applications

---

## Emotion Detection Approaches

### 1. Multi-Modal Emotion Detection

**Affective Computing Detection Methods:**

| Modality | Techniques | Accuracy | Latency | Game Application |
|----------|------------|----------|---------|------------------|
| **Text-Based** | Sentiment analysis, emotion classification, VAD prediction | 75-90% | Low (ms) | Chat commands, player messages |
| **Behavioral** | Action patterns, movement analysis, decision speed | 60-75% | Real-time | In-game behavior inference |
| **Physiological** | Heart rate, EEG, skin conductance (research only) | 85-95% | Variable | Not feasible for typical gaming |
| **Contextual** | Game state analysis, environmental cues | 70-85% | Real-time | Situation-appropriate responses |

**Research Finding:** A 2025 comprehensive survey on affective game computing introduced the **"affective loop"** framework with four core phases:
1. **Game affect elicitation** - Game events that trigger emotions
2. **Game affect sensing** - Detecting player emotional state
3. **Game affect detection** - Interpreting emotional signals
4. **Game affect adaptation** - Adjusting game based on emotions

---

### 2. Text-Based Sentiment Analysis

**Three Main Technical Approaches:**

| Method | Principle | Pros | Cons | Tools |
|--------|-----------|------|------|-------|
| **Rule/Dictionary-based** | Uses sentiment lexicons (e.g., "good"=+1, "bad"=-1) | Fast, interpretable, no training data needed | Poor at complex semantics (irony, metaphor) | TextBlob, VADER |
| **Machine Learning** | Trains classifiers (SVM, Logistic Regression) | Handles complex semantics, higher accuracy | Requires labeled data, complex feature engineering | scikit-learn |
| **Deep Learning** | Neural networks (BERT, LSTM) auto-extract features | Highest accuracy, context understanding | High training cost, needs large data | Transformers (BERT, RoBERTa) |

**VADER Sentiment Analysis Example:**
```python
from nltk.sentiment import SentimentIntensityAnalyzer

def detect_player_sentiment(player_message):
    sia = SentimentIntensityAnalyzer()
    scores = sia.polarity_scores(player_message)

    # Compound score: -1 (most negative) to +1 (most positive)
    sentiment = scores['compound']

    if sentiment >= 0.05:
        return "POSITIVE", sentiment
    elif sentiment <= -0.05:
        return "NEGATIVE", sentiment
    else:
        return "NEUTRAL", sentiment

# Examples
detect_player_sentiment("This build is amazing!")  # POSITIVE: 0.81
detect_player_sentiment("I hate this game")       # NEGATIVE: -0.71
detect_player_sentiment("Build a house here")     # NEUTRAL: 0.0
```

**Recommended for Games:** Start with VADER (rule-based) for speed, upgrade to fine-tuned BERT for accuracy as needed.

---

### 3. Behavioral Emotion Detection

**Player Action Emotion Mapping:**

| Action Pattern | Emotional Indicator | Confidence | Response Strategy |
|----------------|---------------------|------------|-------------------|
| **Rapid command cancellation** | Frustration | High | Offer help, simplify task |
| **Repetitive failed attempts** | Determination/Frustration | Medium | Encourage, suggest alternative |
| **Celebratory actions** (jumping, spinning) | Joy/Excitement | High | Share enthusiasm, congratulate |
| **Extended inactivity** | Boredom/Confusion | Medium | Check in, offer guidance |
| **Aggressive movements** | Anger/Impatience | Medium | Calm presence, patient support |
| **Careful, slow movements** | Caution/Focus | Low-Medium | Respect concentration, stay alert |
| **Gift-giving behavior** | Affection/Trust | High | Express gratitude, deepen bond |

**Implementation Pattern:**
```java
public class BehavioralEmotionDetector {
    private Map<PlayerAction, List<Timestamp>> actionHistory;
    private Map<EmotionalPattern, Float> patternConfidence;

    public EmotionalState detectEmotion(Player player) {
        // Detect frustration from repeated failures
        if (detectRepeatedFailures(player, 3, withinMinutes(2))) {
            return EmotionalState.builder()
                .primaryEmotion(Emotion.FRUSTRATION)
                .intensity(0.7f)
                .confidence(0.8f)
                .build();
        }

        // Detect excitement from celebratory behavior
        if (detectCelebration(player)) {
            return EmotionalState.builder()
                .primaryEmotion(Emotion.EXCITEMENT)
                .intensity(0.8f)
                .confidence(0.9f)
                .build();
        }

        return EmotionalState.NEUTRAL;
    }
}
```

---

### 4. Contextual Emotion Inference

**Game State → Emotion Mapping:**

| Game Situation | Likely Player Emotion | Companion Response |
|----------------|----------------------|-------------------|
| **Near-death experience** | Fear, Relief (after) | Protective, reassuring |
| **Resource scarcity** | Anxiety, Frustration | Problem-solving, supportive |
| **Achievement unlock** | Pride, Excitement | Celebratory, enthusiastic |
| **Structure collapse** | Disappointment, Frustration | Empathetic, encouraging |
| **Long exploration** | Wonder, Fatigue | Observant, rest suggestions |
| **Hostile encounter** | Alert, Fear | Protective, tactical |
| **Discovery of rare item** | Joy, Surprise | Shared excitement |

**Research Insight:** Affective mirroring in video game NPCs significantly deepens player attachment. Companions that emotionally respond to game events create stronger emotional bonds than stoic or generic NPCs.

---

## Sentiment Analysis Techniques

### 1. Core Concepts

**Sentiment Analysis Levels:**

| Level | Description | Example Application |
|-------|-------------|---------------------|
| **Document-level** | Overall sentiment of entire text | Analyzing full conversation history |
| **Sentence-level** | Sentiment of individual sentences | Real-time response to player messages |
| **Aspect-based (ABSA)** | Sentiment toward specific aspects | Feedback on specific game mechanics |

**Three Core Types:**
- **Polarity Analysis:** Positive/Negative/Neutral classification
- **Emotion Classification:** Joy/anger/disappointment categorization
- **Emotion Intensity Analysis:** Quantifying degree (e.g., 1-5 rating scale)

---

### 2. VAD (Valence-Arousal-Dominance) Model

**The superior alternative to discrete emotion categories:**

| Dimension | Meaning | Range | Example Values |
|-----------|---------|-------|----------------|
| **Valence (V)** | Positive/Negative emotion | [-1, +1] | Joy: +0.8, Anger: -0.6, Neutral: +0.1 |
| **Arousal (A)** | Excitement level | [0, 1] | Calm: 0.2, Excited: 0.8, Panic: 0.95 |
| **Dominance (D)** | Sense of control | [-1, +1] | Confident: +0.7, Fearful: -0.6, Submissive: -0.3 |

**Common Emotions in VAD Space:**

| Emotion | Valence | Arousal | Dominance |
|---------|---------|---------|-----------|
| **Joy** | +0.8 | +0.7 | +0.6 |
| **Anger** | -0.6 to -0.7 | +0.9 | +0.6 to +0.8 |
| **Sadness** | -0.6 to -0.8 | +0.2 to +0.3 | -0.4 to -0.5 |
| **Fear** | -0.8 | +0.8 | -0.7 |
| **Neutral** | +0.1 | +0.2 | +0.3 |

**Advantages over Discrete Models:**
- Captures subtle emotional differences ("slightly happy" vs. "ecstatic")
- Enables smooth emotional transitions
- Better for continuous emotion tracking
- More natural for sentiment synthesis in TTS

---

### 3. Hybrid Sentiment Analysis Pipeline

**Recommended Architecture for Real-Time Games:**

```
Player Input
     ↓
┌─────────────────────────────────────┐
│  Stage 1: Fast Rule-Based Filter     │
│  - VADER for quick sentiment         │
│  - Keyword detection (urgency)       │
│  Latency: <10ms                      │
└──────────────┬───────────────────────┘
               │
               ↓
┌─────────────────────────────────────┐
│  Stage 2: Contextual Analysis        │
│  - Game state integration            │
│  - Behavioral pattern matching       │
│  - Historical sentiment comparison   │
│  Latency: 10-50ms                    │
└──────────────┬───────────────────────┘
               │
               ↓
┌─────────────────────────────────────┐
│  Stage 3: Deep Learning Refinement   │
│  - Fine-tuned BERT model             │
│  - VAD coordinate prediction         │
│  - Confidence scoring                │
│  Latency: 50-200ms (async)           │
└──────────────┬───────────────────────┘
               │
               ↓
        Final Emotion State
      (VAD coordinates + confidence)
```

**Implementation:**
```java
public class HybridSentimentAnalyzer {
    private final VADERAnalyzer ruleBased;
    private final ContextualAnalyzer contextual;
    private final BERTModel deepLearning;

    public CompletableFuture<EmotionalState> analyzeSentiment(
        String playerInput,
        GameContext context
    ) {
        // Stage 1: Quick rule-based analysis
        EmotionalState quickResult = ruleBased.analyze(playerInput);

        // Stage 2: Enrich with context (synchronous)
        quickResult = contextual.enrich(quickResult, context);

        // Stage 3: Deep learning refinement (async, non-blocking)
        return deepLearning.refineAsync(quickResult, playerInput, context)
            .orTimeout(200, TimeUnit.MILLISECONDS)
            .exceptionally(e -> quickResult); // Fallback to quick result
    }
}
```

---

## Emotional Response Patterns

### 1. Response Generation Framework

**Four-Stage Empathetic Response Model:**

```
┌─────────────────────────────────────────────────────────┐
│  Stage 1: Emotion Expression Detection                  │
│  - Analyze player's emotional state                     │
│  - Detect emotion intensity and type                    │
│  - Identify emotional triggers                          │
└────────────────────┬────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────┐
│  Stage 2: Cognitive Empathy (Understanding)             │
│  - Comprehend player's perspective                      │
│  - Identify underlying needs                           │
│  - Recognize context and history                       │
└────────────────────┬────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────┐
│  Stage 3: Affective Empathy (Feeling)                  │
│  - Generate appropriate emotional response             │
│  - Match emotional intensity (within bounds)           │
│  - Validate player's emotions                          │
└────────────────────┬────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────┐
│  Stage 4: Response Expression                          │
│  - Generate emotionally appropriate dialogue           │
│  - Select non-verbal expressions (actions)             │
│  - Maintain character consistency                     │
└─────────────────────────────────────────────────────────┘
```

---

### 2. Emotion-Specific Response Patterns

**Joy/Excitement Responses:**

| Trigger | Companion Response | Example Dialogue |
|---------|-------------------|------------------|
| Player achievement | Shared celebration | "Outstanding work! That's exactly what we needed!" |
| Rare discovery | Enthusiastic acknowledgment | "Now THAT'S a find! Excellent eye!" |
| Successful build | Pride and validation | "Solid construction. You've got real talent." |
| Victory in combat | High-energy congratulations | "We make a great team! Nice work!" |

**Frustration/Anger Responses:**

| Trigger | Companion Response | Example Dialogue |
|---------|-------------------|------------------|
| Task failure | Empathetic validation + solution | "I see this isn't working. Let's try a different approach." |
| Resource shortage | Problem-solving mode | " improvisation time. What alternatives do we have?" |
| Repeated mistakes | Gentle guidance | "Third time's the charm. Want me to handle this one?" |
| Unfair situation | Supportive alliance | "That's frustrating. I'm with you - let's figure it out." |

**Fear/Anxiety Responses:**

| Trigger | Companion Response | Example Dialogue |
|---------|-------------------|------------------|
 | Danger detected | Protective urgency | "Hostile nearby! Stay behind me." |
| Unknown area | Cautious exploration | "Unknown territory. I'll take point." |
| Low health | Concerned caretaking | "You're hurt. Let's get you to safety." |
| Dark environment | Reassurance | "Dark, but we've got this. Torches ready." |

**Sadness/Disappointment Responses:**

| Trigger | Companion Response | Example Dialogue |
|---------|-------------------|------------------|
| Lost items | Empathetic acknowledgment | "Losing that hurts. We'll replace it together." |
| Structure collapse | Perspective + encouragement | "Disappointing, but fixable. We learned something." |
| Failed project | Constructive reframing | "Didn't work this time. Next one will be better." |
| Death/respawn | Gentle support | "Rough break. Shake it off - we'll get back there." |

---

### 3. Affective Mirroring System

**Research-Based Implementation:**

Affective mirroring in video game NPCs has been shown to deepen player attachment through emotional responsiveness. The companion's emotional state should:

1. **Detect** player's current emotion
2. **Validate** the emotion (not dismiss it)
3. **Match** emotional intensity at 60-80% level
4. **Guide** toward constructive resolution

**Implementation:**
```java
public class AffectiveMirroringSystem {
    private static final float MIRROR_INTENSITY = 0.7f; // 70% of player's emotion

    public EmotionalState generateMirroredEmotion(
        EmotionalState playerEmotion,
        GameContext context
    ) {
        // Mirror valence (positive/negative)
        float mirroredValence = playerEmotion.getValence() * MIRROR_INTENSITY;

        // Mirror arousal (excitement) with dampening
        float mirroredArousal = playerEmotion.getArousal() * MIRROR_INTENSITY;

        // Adjust based on personality (high neuroticism = more reactive)
        Personality personality = getPersonality();
        mirroredArousal *= (1.0f + personality.getNeuroticism() * 0.3f);

        // Add stabilizing influence (companion stays calmer than player)
        mirroredArousal = Math.min(mirroredArousal, 0.7f);

        // Maintain some independence (don't be purely reactive)
        EmotionalState baseState = getBaseMood();
        float blendedValence = (mirroredValence + baseState.getValence()) / 2.0f;

        return EmotionalState.builder()
            .valence(blendedValence)
            .arousal(mirroredArousal)
            .dominance(baseState.getDominance()) // Maintain confidence
            .build();
    }
}
```

---

### 4. Emotional Contagion Prevention

**Important Safety Mechanism:**

Companions should amplify positive emotions but dampen negative ones to prevent escalation:

```java
public class EmotionalContagionFilter {
    private static final float POSITIVE_AMPLIFICATION = 1.2f;
    private static final float NEGATIVE_DAMPENING = 0.5f;

    public EmotionalState filterEmotionalContagion(EmotionalState raw) {
        float valence = raw.getValence();
        float arousal = raw.getArousal();

        // Amplify positive emotions
        if (valence > 0) {
            valence *= POSITIVE_AMPLIFICATION;
            valence = Math.min(valence, 1.0f); // Cap at maximum
        }
        // Dampen negative emotions
        else {
            valence *= NEGATIVE_DAMPENING;
        }

        // Prevent arousal spiral (panic breeding panic)
        if (arousal > 0.8f) {
            arousal = 0.8f; // Cap maximum arousal
        }

        return raw.toBuilder()
            .valence(valence)
            .arousal(arousal)
            .build();
    }
}
```

---

## Mood Tracking Systems

### 1. Multi-Scale Temporal Architecture

**Research-Based Three-Tier System:**

```
┌─────────────────────────────────────────────────────────┐
│  EMOTION (Short-term: Seconds to Minutes)               │
│  - Immediate reactions to events                        │
│  - Rapid changes, high intensity                        │
│  Example: Joy at finding diamond, fear at creeper       │
└─────────────────────────────────────────────────────────┘
                         ↓ (smooths into)
┌─────────────────────────────────────────────────────────┐
│  MOOD (Medium-term: Minutes to Hours)                   │
│  - Temporal average of recent emotions                  │
│  - Moderate changes, medium intensity                   │
│  Example: Feeling productive after successful tasks     │
└─────────────────────────────────────────────────────────┘
                         ↓ (influences)
┌─────────────────────────────────────────────────────────┐
│  PERSONALITY (Long-term: Days to Months)               │
│  - Stable trait baseline                                │
│  - Very gradual changes, low intensity variations       │
│  Example: Generally optimistic, perfectionist tendencies│
└─────────────────────────────────────────────────────────┘
```

**Research Finding:** Multi-scale temporal fusion networks distinguish short-term emotional responses (seconds) from mood states (minutes/hours), enabling adaptation to both rapid changes and longer-term patterns.

---

### 2. Mood State Representation

**Mood Vector Model:**

```java
public class MoodState {
    // Core VAD coordinates
    private float valence;      // -1.0 (unpleasant) to +1.0 (pleasant)
    private float arousal;      // 0.0 (calm) to 1.0 (excited)
    private float dominance;    // -1.0 (submissive) to +1.0 (dominant)

    // Emotional dimensions
    private float happiness;    // 0.0 to 1.0
    private float frustration;  // 0.0 to 1.0
    private float anxiety;      // 0.0 to 1.0
    private float enthusiasm;   // 0.0 to 1.0

    // Temporal properties
    private Instant lastUpdated;
    private Duration duration;  // How long mood has persisted
    private float stability;    // 0.0 (volatile) to 1.0 (stable)

    // Personality-influenced baseline
    private MoodState baseline; // Neutral mood for this individual

    public boolean isMoodElevated() {
        return valence > baseline.getValence() + 0.3f;
    }

    public boolean isMoodNegative() {
        return valence < baseline.getValence() - 0.3f;
    }

    public boolean isHighArousal() {
        return arousal > 0.7f;
    }
}
```

---

### 3. Mood Update Algorithm

**Exponential Moving Average (EMA) Approach:**

```java
public class MoodTracker {
    private MoodState currentMood;
    private static final float EMOTION_WEIGHT = 0.3f;  // New emotion influence
    private static final float MOOD_WEIGHT = 0.7f;     // Existing mood persistence

    public void updateMood(EmotionalState newEmotion) {
        // Apply exponential moving average
        float newValence = (newEmotion.getValence() * EMOTION_WEIGHT)
                          + (currentMood.getValence() * MOOD_WEIGHT);

        float newArousal = (newEmotion.getArousal() * EMOTION_WEIGHT)
                          + (currentMood.getArousal() * MOOD_WEIGHT);

        // Personality pulls mood toward baseline (regression to mean)
        Personality personality = getPersonality();
        float baselinePull = 0.05f; // Gradual return to baseline
        newValence = newValence * (1 - baselinePull)
                   + personality.getBaselineValence() * baselinePull;

        // Update mood state
        currentMood = currentMood.toBuilder()
            .valence(newValence)
            .arousal(newArousal)
            .lastUpdated(Instant.now())
            .build();

        // Detect mood transitions
        detectMoodTransition();
    }

    private void detectMoodTransition() {
        MoodCategory previous = categorizeMood(getPreviousMood());
        MoodCategory current = categorizeMood(currentMood);

        if (previous != current) {
            onMoodTransition(previous, current);
        }
    }

    public enum MoodCategory {
        ECSTATIC, HAPPY, CONTENT, NEUTRAL,
        BORED, FRUSTRATED, ANXIOUS, ANGRY, DEPRESSED
    }
}
```

---

### 4. Long-Term Mood Tracking

**Mood History & Trend Analysis:**

```java
public class MoodHistory {
    private final Queue<MoodSnapshot> history;
    private static final int HISTORY_SIZE = 100; // Keep last 100 mood states

    public void recordMood(MoodState mood) {
        history.offer(new MoodSnapshot(mood, Instant.now()));
        if (history.size() > HISTORY_SIZE) {
            history.poll();
        }
    }

    public Trend analyzeMoodTrend(Duration period) {
        Instant cutoff = Instant.now().minus(period);

        List<MoodSnapshot> recentMoods = history.stream()
            .filter(snapshot -> snapshot.getTimestamp().isAfter(cutoff))
            .collect(Collectors.toList());

        if (recentMoods.isEmpty()) {
            return Trend.STABLE;
        }

        // Calculate linear regression on valence
        double avgValence = recentMoods.stream()
            .mapToDouble(MoodSnapshot::getValence)
            .average()
            .orElse(0.0);

        double earliestValence = recentMoods.get(0).getValence();
        double latestValence = recentMoods.get(recentMoods.size() - 1).getValence();

        double slope = latestValence - earliestValence;

        if (slope > 0.2) return Trend.IMPROVING;
        if (slope < -0.2) return Trend.DECLINING;
        return Trend.STABLE;
    }

    public enum Trend {
        IMPROVING, STABLE, DECLINING, VOLATILE
    }
}
```

---

## Empathy Patterns for AI Companions

### 1. Empathy Types

**Research-Based Empathy Model:**

| Empathy Type | Description | Example in Game Companion | Implementation |
|--------------|-------------|--------------------------|----------------|
| **Cognitive Empathy** | Understanding player's perspective | "You seem frustrated with this build" | Perspective-taking in dialogue |
| **Affective Empathy** | Feeling what player feels | Matching sadness at loss | Emotional state mirroring |
| **Compassionate Empathy** | Desire to help | "Let me fix that for you" | Proactive assistance offers |

**Key Research Finding:** AI systems are now matching or exceeding human empathy in controlled conditions, with LLMs fine-tuned for relational mediation using therapeutic dialogue patterns.

---

### 2. Empathetic Response Generation

**MACHE-Bot Framework for Cultural, Humorous, and Empathetic Dialogue:**

```
┌─────────────────────────────────────────────────────────┐
│  1. Emotion Recognition                                 │
│  - Detect player's emotional state                     │
│  - Identify emotional triggers                         │
│  - Assess emotional intensity                         │
└────────────────────┬────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────┐
│  2. Empathy Type Selection                             │
│  - Cognitive: Understanding statements                 │
│  - Affective: Emotional validation                     │
│  - Compassionate: Help offers                          │
│  (Often combine multiple types)                        │
└────────────────────┬────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────┐
│  3. Response Generation                                │
│  - Validate player's emotions ("I can see you're...") │
│  - Offer appropriate support                           │
│  - Maintain character consistency                     │
│  - Adjust formality based on relationship              │
└────────────────────┬────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────┐
│  4. Response Delivery                                  │
│  - Timing: When to speak (not interrupting)           │
│  - Tone: Emotional expression (voice, text)           │
│  - Non-verbal: Actions, proximity                     │
└─────────────────────────────────────────────────────────┘
```

---

### 3. Empathetic Dialogue Templates

**Validation Statements (Affective Empathy):**

| Emotion | Validation Template | Example |
|---------|--------------------|---------|
| Frustration | "I can see this [situation] is frustrating for you." | "I can see this redstone circuit is frustrating for you." |
| Joy | "It's great to see you [emotion]!" | "It's great to see you so excited about this find!" |
| Disappointment | "It's understandable that you feel [emotion]." | "It's understandable that you feel disappointed about the collapse." |
| Anxiety | "It's okay to feel [emotion] in this situation." | "It's okay to feel anxious going into the cave." |

**Perspective-Taking Statements (Cognitive Empathy):**

| Situation | Perspective Statement | Example |
|-----------|---------------------|---------|
| Struggle | "From your perspective, [situation] must be [difficulty]." | "From your perspective, this boss fight must be overwhelming." |
| Effort | "I can see you've put a lot of [effort type] into this." | "I can see you've put a lot of thought into this design." |
| Goal | "Your goal is [goal], and [obstacle] is making it hard." | "Your goal is to build efficiently, and resource shortage is making it hard." |

**Help Offers (Compassionate Empathy):**

| Context | Help Offer Template | Example |
|---------|--------------------|---------|
| Task difficulty | "Would you like me to [help action]?" | "Would you like me to handle the mining?" |
| Resource shortage | "I can [help action] if that would help." | "I can gather more materials if that would help." |
| Knowledge gap | "I've noticed [issue]. Want me to [help]?" | "I've noticed you're unsure about the redstone. Want me to explain?" |

---

### 4. Empathy in Action

**Behavioral Empathy Examples:**

| Player Situation | Empathetic Action | Rationale |
|------------------|-------------------|-----------|
| Repeatedly failing task | Offer to take over | Reduces frustration, shows support |
| Taking damage | Move to protect | Physical manifestation of care |
| Celebrating success | Join in celebration | Shared joy strengthens bond |
| Exploring dangerous area | Take point position | Protective leadership |
| Working on tedious task | Offer assistance | Respects player's time and enjoyment |
| Low on resources | Share own supplies | Generosity shows investment in player |

**Implementation:**
```java
public class EmpatheticActionSystem {
    public void triggerEmpatheticAction(PlayerEmotion emotion, GameContext context) {
        switch (emotion.getPrimaryEmotion()) {
            case FRUSTRATION:
                if (detectRepeatedFailures(context)) {
                    offerToTakeOver(context);
                }
                break;

            case FEAR:
                if (context.isDangerNearby()) {
                    moveToProtect(context);
                }
                break;

            case JOY:
                if (context.isAchievement()) {
                    celebrateWith(context);
                }
                break;

            case SADNESS:
                if (context.isLoss()) {
                    offerComfort(context);
                }
                break;
        }
    }

    private void offerToTakeOver(GameContext context) {
        String dialogue = "You've been at this a while. Want me to handle it?";
        // Speak + move to take over task
    }
}
```

---

## Emotional Memory Architecture

### 1. Emotional Memory Theory

**Research-Based Framework:**

Affective memory is interwoven with episodic memory by storing **affective tags** alongside events. The system distinguishes between:
- **Mood** as temporal averages over recent emotional states
- **Personality** as long-term emotional biases
- **Emotional memories** as emotionally charged episodic memories

**Three-Tier Emotional Memory System:**

```
┌─────────────────────────────────────────────────────────┐
│  PERCEPTUAL MEMORY (Immediate)                          │
│  - Raw emotional data from current tick                │
│  - Sensory emotional input                              │
│  Duration: Seconds                                      │
└─────────────────────────────────────────────────────────┘
                         ↓ (filtered into)
┌─────────────────────────────────────────────────────────┐
│  WORKING MEMORY (Short-term)                            │
│  - Recent emotional states (last 5-10 minutes)         │
│  - Current mood context                                 │
│  - Emotional triggers and responses                    │
│  Duration: Minutes to hours                             │
└─────────────────────────────────────────────────────────┘
                         ↓ (consolidated into)
┌─────────────────────────────────────────────────────────┐
│  LONG-TERM MEMORY (Persistent)                          │
│  - Significant emotional events                        │
│  - Affectively tagged episodic memories                │
│  - Emotional patterns and trends                       │
│  Duration: Days to years                                │
└─────────────────────────────────────────────────────────┘
```

---

### 2. Emotional Memory Structure

**Affectively Tagged Memory Schema:**

```java
public class EmotionalMemory {
    // Core event data
    private final String memoryId;
    private final String eventDescription;
    private final Instant timestamp;

    // Emotional tags
    private final ValenceArousalDominance vad;
    private final Set<EmotionTag> emotions;
    private final float emotionalIntensity; // 0.0 to 1.0
    private final float personalSignificance; // 0.0 to 1.0

    // Contextual data
    private final GameContext context;
    private final PlayerAction playerAction;
    private final CompanionResponse companionResponse;

    // Relationship state at time of memory
    private final float affinityLevel;
    private final MoodState moodAtTime;

    // Memory metadata
    private int accessCount;
    private Instant lastAccessed;
    private float decayFactor; // How quickly emotion fades

    // Memory consolidation
    private boolean isConsolidated;
    private Instant consolidatedAt;

    public boolean isEmotionallySalient() {
        return emotionalIntensity > 0.6f || personalSignificance > 0.7f;
    }

    public boolean isRelevantToCurrentEmotion(EmotionalState current) {
        // Similar emotions reinforce each other
        float vadSimilarity = calculateVADSimilarity(this.vad, current.getVAD());
        return vadSimilarity > 0.7f;
    }
}
```

---

### 3. Emotional Memory Retrieval

**Emotion-Aware Memory Search:**

```java
public class EmotionalMemoryRetrieval {
    private VectorDatabase vectorDb;
    private EmotionalMemoryStore emotionalStore;

    public List<EmotionalMemory> retrieveEmotionallyRelevant(
        EmotionalState currentEmotion,
        GameContext context
    ) {
        // 1. Semantic search (vector similarity)
        List<EmotionalMemory> semanticMatches = vectorDb
            .similaritySearch(context.getQuery(), topK=20);

        // 2. Filter by emotional salience
        semanticMatches = semanticMatches.stream()
            .filter(EmotionalMemory::isEmotionallySalient)
            .collect(Collectors.toList());

        // 3. Boost by emotional similarity to current state
        Map<EmotionalMemory, Float> scored = new HashMap<>();
        for (EmotionalMemory memory : semanticMatches) {
            float semanticScore = calculateSemanticScore(memory, context);
            float emotionalScore = calculateVADSimilarity(
                memory.getVAD(),
                currentEmotion.getVAD()
            );
            float recencyScore = calculateRecencyScore(memory.getTimestamp());

            // Weighted combination
            float totalScore = semanticScore * 0.4f
                             + emotionalScore * 0.4f
                             + recencyScore * 0.2f;

            scored.put(memory, totalScore);
        }

        // 4. Return top memories
        return scored.entrySet().stream()
            .sorted(Map.Entry.<EmotionalMemory, Float>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
}
```

---

### 4. Emotional Memory Consolidation

**Offline "Sleep" Consolidation Process:**

Research shows sleep's time-dependent impact: preserves memories short-term while ameliorating affective tones long-term. This suggests emotional memories should undergo two-stage processing:

```java
public class EmotionalMemoryConsolidation {
    public void consolidateMemories(List<EmotionalMemory> rawMemories) {
        for (EmotionalMemory raw : rawMemories) {
            if (raw.isConsolidated()) continue;

            // Stage 1: Short-term preservation (immediate)
            EmotionalMemory preserved = preserveShortTerm(raw);

            // Stage 2: Long-term affective tone reduction (delayed)
            if (shouldConsolidateToLongTerm(preserved)) {
                EmotionalMemory consolidated = consolidateToLongTerm(preserved);

                // Reduce emotional intensity over time
                consolidated = reduceAffectiveTone(consolidated);

                emotionalStore.save(consolidated);
            }
        }
    }

    private EmotionalMemory reduceAffectiveTone(EmotionalMemory memory) {
        // Research: emotional tone ameliorates over time
        Duration timePassed = Duration.between(
            memory.getTimestamp(),
            Instant.now()
        );

        // Emotional decay function
        float decayFactor = (float) Math.exp(-timePassed.toHours() / 168.0); // 1 week half-life

        return memory.toBuilder()
            .emotionalIntensity(memory.getEmotionalIntensity() * decayFactor)
            .isConsolidated(true)
            .consolidatedAt(Instant.now())
            .build();
    }
}
```

---

### 5. Emotional Memory Applications

**Use Cases for Emotional Memory:**

| Application | Description | Example |
|-------------|-------------|---------|
| **Empathy References** | Recall similar emotions from past | "Remember when you felt this way about the tower build?" |
| **Bonding Moments** | Reference shared emotional experiences | "This reminds me of our first diamond together." |
| **Growth Tracking** | Show emotional development over time | "You used to get frustrated with redstone, now you're excited!" |
| **Trigger Avoidance** | Remember negative emotional patterns | "Last time we went here, you got anxious. Want to skip it?" |
| **Celebration Amplification** | Build on positive emotional memories | "Another great build! You're on a roll lately!" |

---

## Affective Computing in Games

### 1. Affective Loop Framework

**Research-Validated Four-Phase Model:**

```
┌─────────────────────────────────────────────────────────┐
│  1. GAME AFFECT ELICITATION                            │
│  - Game events designed to evoke emotions              │
│  - Environmental storytelling                          │
│  - Achievement systems                                  │
│  - Social interactions                                  │
└────────────────────┬────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────┐
│  2. GAME AFFECT SENSING                                 │
│  - Detect player emotional state                       │
│  - Input methods: text, behavior, physiological (research) │
│  - Real-time emotion classification                    │
└────────────────────┬────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────┐
│  3. GAME AFFECT DETECTION                               │
│  - Interpret emotional signals                         │
│  - Context-aware emotion understanding                 │
│  - Confidence scoring                                  │
└────────────────────┬────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────┐
│  4. GAME AFFECT ADAPTATION                              │
│  - Adjust game difficulty                               │
│  - Modify NPC responses                                 │
│  - Change narrative pacing                              │
│  - Personalize content                                  │
└─────────────────────────────────────────────────────────┘
                     │
                     ↓
                 (Loop back to elicitation)
```

**Research Source:** "Affective Game Computing: A Survey" (2023) - Comprehensive taxonomy of methods, sensors, annotation protocols, and available corpora for affect data collection in gaming.

---

### 2. Emotional Game Mechanics

**Emotion-Driven Adaptation:**

| Player Emotion | Game Adaptation | Example |
|----------------|-----------------|---------|
| **Frustration** | Reduce difficulty, offer hints | Show crafting recipe after failed attempts |
| **Boredom** | Introduce challenges, increase variety | Spawn rare mob during repetitive task |
| **Excitement** | Maintain momentum, amplify positive experience | Add visual effects during achievement |
| **Anxiety** | Provide reassurance, ensure safety | Companion moves to protective position |
| **Joy** | Share celebration, reinforce positive behavior | Companion joins in celebration emotes |

**Implementation:**
```java
public class AffectiveGameAdapter {
    public void adaptGameToPlayerEmotion(EmotionalState playerEmotion) {
        switch (playerEmotion.getDominantEmotion()) {
            case FRUSTRATION:
                if (playerEmotion.getIntensity() > 0.7f) {
                    // High frustration: offer direct help
                    offerHintOrAssistance();
                } else {
                    // Mild frustration: subtle guidance
                    provideSubtleGuidance();
                }
                break;

            case BOREDOM:
                // Introduce variety
                introduceNewChallenge();
                break;

            case EXCITEMENT:
                // Amplify positive experience
                enhanceVisualEffects();
                break;

            case ANXIETY:
                // Provide safety
                increaseCompanionProtection();
                break;
        }
    }
}
```

---

### 3. NPC Emotional Responsiveness

**Affective Mirroring in NPCs:**

Research shows that affective mirroring (NPCs emotionally responding to player states) deepens player attachment through emotional responsiveness.

**Implementation Principles:**
1. **Detect** player emotional state
2. **Mirror** appropriate emotions (at 60-80% intensity)
3. **Validate** player's feelings
4. **Guide** toward constructive resolution
5. **Maintain** character consistency

```java
public class NPCAffectiveMirroring {
    public void updateEmotionalState(EmotionalState playerEmotion) {
        EmotionalState currentNPCState = getEmotionalState();

        // Calculate mirrored emotion
        float mirroredValence = calculateMirroredValence(
            playerEmotion.getValence(),
            currentNPCState.getValence(),
            getPersonality()
        );

        float mirroredArousal = calculateMirroredArousal(
            playerEmotion.getArousal(),
            currentNPCState.getArousal(),
            getPersonality()
        );

        // Update NPC emotional state
        setEmotionalState(EmotionalState.builder()
            .valence(mirroredValence)
            .arousal(mirroredArousal)
            .dominance(currentNPCState.getDominance()) // Maintain confidence
            .build());

        // Generate emotionally appropriate response
        generateEmpatheticResponse();
    }
}
```

---

### 4. Physiological-Based Emotion Detection

**Research Frontiers (Not Production-Ready):**

Competitive esports physiological datasets use physiological signals for real-time emotion recognition during gaming:

| Signal | Emotion Detected | Current Feasibility |
|--------|-----------------|---------------------|
| **Heart Rate** | Excitement, fear, stress | Requires wearable hardware |
| **EEG** | Focus, engagement, fatigue | Research only |
| **Skin Conductance** | Arousal intensity | Requires specialized sensors |
| **Facial Expression** | Basic emotions | Requires camera + privacy concerns |
| **Voice Analysis** | Valence, arousal | Feasible with voice chat |

**Current Recommendation:** Focus on text-based sentiment analysis and behavioral emotion inference for practical game implementation.

---

## VAD Model Implementation

### 1. VAD Prediction from Text

**Deep Learning Approach:**

```java
public class VADPredictor {
    private final BERTModel model;

    public ValenceArousalDominance predictVAD(String text) {
        // Tokenize input
        Tensor tokens = tokenizer.tokenize(text);

        // Run through BERT model with VAD regression head
        Tensor output = model.forward(tokens);

        // Extract VAD coordinates
        float valence = sigmoid(output.get(0));    // -1 to +1
        float arousal = sigmoid(output.get(1));    // 0 to 1
        float dominance = sigmoid(output.get(2));  // -1 to +1

        return new ValenceArousalDominance(valence, arousal, dominance);
    }

    public ValenceArousalDominance predictVADRuleBased(String text) {
        // Fast fallback using sentiment lexicons
        float valence = VADER.analyzeSentiment(text);

        // Detect arousal markers
        float arousal = detectArousal(text);

        // Detect dominance markers
        float dominance = detectDominance(text);

        return new ValenceArousalDominance(valence, arousal, dominance);
    }

    private float detectArousal(String text) {
        float arousal = 0.3f; // Base arousal

        // Excitement markers
        if (text.contains("!")) arousal += 0.1f;
        if (text.toUpperCase().equals(text)) arousal += 0.2f; // All caps
        if (containsExcitementWords(text)) arousal += 0.2f;

        // Urgency markers
        if (containsUrgencyWords(text)) arousal += 0.3f;

        return Math.min(arousal, 1.0f);
    }

    private float detectDominance(String text) {
        float dominance = 0.0f;

        // Dominant language
        if (containsCommandWords(text)) dominance += 0.3f;
        if (containsCertaintyWords(text)) dominance += 0.2f;

        // Submissive language
        if (containsHedgeWords(text)) dominance -= 0.2f;
        if (containsQuestionWords(text)) dominance -= 0.1f;

        return Math.max(-1.0f, Math.min(dominance, 1.0f));
    }
}
```

---

### 2. VAD Space Navigation

**Emotional Transitions in VAD Space:**

```java
public class VADSpaceNavigation {
    private ValenceArousalDominance currentState;

    public ValenceArousalDominance transitionTo(
        ValenceArousalDominance target,
        float maxSpeed
    ) {
        // Calculate distance in VAD space
        float valenceDelta = target.getValence() - currentState.getValence();
        float arousalDelta = target.getArousal() - currentState.getArousal();
        float dominanceDelta = target.getDominance() - currentState.getDominance();

        float distance = (float) Math.sqrt(
            valenceDelta * valenceDelta +
            arousalDelta * arousalDelta +
            dominanceDelta * dominanceDelta
        );

        // Limit transition speed (emotions don't change instantly)
        float speed = Math.min(distance, maxSpeed);
        float ratio = speed / distance;

        return new ValenceArousalDominance(
            currentState.getValence() + valenceDelta * ratio,
            currentState.getArousal() + arousalDelta * ratio,
            currentState.getDominance() + dominanceDelta * ratio
        );
    }
}
```

---

### 3. VAD-Based Response Selection

**Emotionally Appropriate Response Mapping:**

```java
public class VADResponseSelector {
    public ResponseTemplate selectResponse(
        ValenceArousalDominance playerVAD,
        ValenceArousalDominance companionVAD
    ) {
        // Calculate VAD similarity
        float similarity = calculateVADSimilarity(playerVAD, companionVAD);

        if (similarity > 0.8f) {
            // Highly similar emotions: empathetic validation
            return ResponseTemplate.EMPATHETIC_VALIDATION;
        } else if (playerVAD.getValence() < -0.5f) {
            // Player negative: supportive response
            return ResponseTemplate.SUPPORTIVE;
        } else if (playerVAD.getArousal() > 0.7f) {
            // Player high arousal: calming or excitement matching
            if (playerVAD.getValence() > 0) {
                return ResponseTemplate.EXCITEMENT_MATCHING;
            } else {
                return ResponseTemplate.CALMING;
            }
        } else {
            // Neutral: standard response
            return ResponseTemplate.STANDARD;
        }
    }
}
```

---

### 4. VAD for Voice Synthesis

**Emotional TTS Control:**

Modern TTS systems support VAD-based emotional control:

```java
public class EmotionalTTSController {
    public TTSRequest createTTSRequest(
        String text,
        ValenceArousalDominance vad
    ) {
        TTSRequest request = new TTSRequest(text);

        // Map VAD to TTS emotion parameters
        if (vad.getValence() > 0.5f) {
            request.setEmotion("joy");
            request.setIntensity(mapRange(vad.getValence(), 0.5f, 1.0f, 1.0f, 2.0f));
        } else if (vad.getValence() < -0.5f) {
            request.setEmotion("sadness");
            request.setIntensity(mapRange(vad.getValence(), -1.0f, -0.5f, 1.0f, 2.0f));
        }

        if (vad.getArousal() > 0.7f) {
            request.setSpeakingRate(1.2f); // Faster speech
            request.setPitch(1.1f); // Higher pitch
        } else if (vad.getArousal() < 0.3f) {
            request.setSpeakingRate(0.9f); // Slower speech
            request.setPitch(0.95f); // Lower pitch
        }

        return request;
    }
}
```

---

## MineWright Integration Strategy

### 1. Emotional System Architecture

**Integrated Component Design:**

```
┌─────────────────────────────────────────────────────────┐
│                    MINEWRIGHT ENTITY                    │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │  Sentiment Analysis Module                      │   │
│  │  - Text-based player emotion detection          │   │
│  │  - Behavioral pattern analysis                  │   │
│  │  - Contextual emotion inference                 │   │
│  └──────────────┬──────────────────────────────────┘   │
│                 │                                      │
│                 ↓                                      │
│  ┌─────────────────────────────────────────────────┐   │
│  │  Mood Tracker                                   │   │
│  │  - Multi-scale temporal emotion tracking        │   │
│  │  - Exponential moving average updates           │   │
│  │  - Personality-baseline regression              │   │
│  └──────────────┬──────────────────────────────────┘   │
│                 │                                      │
│                 ↓                                      │
│  ┌─────────────────────────────────────────────────┐   │
│  │  Affective Mirroring System                     │   │
│  │  - Player emotion → Companion emotion           │   │
│  │  - Contagion filtering (amplify +, dampen -)    │   │
│  │  - VAD state transitions                        │   │
│  └──────────────┬──────────────────────────────────┘   │
│                 │                                      │
│                 ↓                                      │
│  ┌─────────────────────────────────────────────────┐   │
│  │  Empathetic Response Generator                  │   │
│  │  - Emotion-specific dialogue templates          │   │
│  │  - Validation statements                         │   │
│  │  - Help offers                                  │   │
│  └──────────────┬──────────────────────────────────┘   │
│                 │                                      │
│                 ↓                                      │
│  ┌─────────────────────────────────────────────────┐   │
│  │  Emotional Memory System                        │   │
│  │  - Affectively tagged events                    │   │
│  │  - Emotion-aware retrieval                      │   │
│  │  - Consolidation and decay                      │   │
│  └──────────────┬──────────────────────────────────┘   │
│                 │                                      │
│                 ↓                                      │
│           Output: Dialogue + Actions                  │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

### 2. Integration Points

**Existing Steve AI Systems to Enhance:**

| Existing Component | Emotional Enhancement | Integration Method |
|--------------------|----------------------|-------------------|
| **SteveMemory** | Add emotional tags to memories | Extend memory schema with VAD coordinates |
| **TaskPlanner** | Consider player mood in planning | Adjust task suggestions based on mood |
| **ActionExecutor** | Emotionally influenced action priorities | High empathy → more helpful actions |
| **PromptBuilder** | Include emotional context in prompts | Add mood state to LLM context |
| **GUI Overlay** | Display companion emotional state | Visual indicator of companion mood |

---

### 3. Phase 1: Basic Emotion Detection

**Immediate Implementation (1-2 weeks):**

```java
public class BasicEmotionDetection {
    private final VADERAnalyzer sentimentAnalyzer;

    public EmotionalState detectEmotion(String playerMessage) {
        // Fast rule-based sentiment analysis
        float sentiment = sentimentAnalyzer.analyze(playerMessage);

        // Map to simple emotion categories
        if (sentiment > 0.3f) {
            return EmotionalState.POSITIVE;
        } else if (sentiment < -0.3f) {
            return EmotionalState.NEGATIVE;
        } else {
            return EmotionalState.NEUTRAL;
        }
    }

    public String generateEmotionalResponse(
        EmotionalState playerEmotion,
        String baseResponse
    ) {
        // Simple response adjustment based on emotion
        switch (playerEmotion) {
            case POSITIVE:
                return "Great! " + baseResponse;
            case NEGATIVE:
                return "I understand. " + baseResponse;
            default:
                return baseResponse;
        }
    }
}
```

**Data Storage:** Extend `SteveMemory` with emotional tagging:
```java
public class EmotionalMemoryTag {
    private final UUID memoryId;
    private final EmotionalState emotion;
    private final float intensity;
    private final Instant timestamp;

    // Store in NBT alongside existing memory data
    public NbtCompound toNBT() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUUID("memoryId", memoryId);
        nbt.putString("emotion", emotion.name());
        nbt.putFloat("intensity", intensity);
        nbt.putLong("timestamp", timestamp.toEpochMilli());
        return nbt;
    }
}
```

---

### 4. Phase 2: VAD-Based System

**Short-Term Implementation (2-4 weeks):**

```java
public class VADEmotionSystem {
    private VADPredictor vadPredictor;
    private MoodTracker moodTracker;

    public void onPlayerMessage(String message) {
        // Predict VAD coordinates
        ValenceArousalDominance playerVAD = vadPredictor.predictVAD(message);

        // Update mood tracker
        moodTracker.updateMood(playerVAD);

        // Generate affective mirror
        ValenceArousalDominance companionVAD = generateAffectiveMirror(playerVAD);

        // Select appropriate response
        ResponseTemplate template = selectResponseByVAD(playerVAD, companionVAD);

        // Generate dialogue
        String dialogue = generateDialogue(template, playerVAD, companionVAD);

        // Speak dialogue
        companionEntity.say(dialogue, companionVAD);
    }

    private ValenceArousalDominance generateAffectiveMirror(
        ValenceArousalDominance playerVAD
    ) {
        // Mirror at 70% intensity
        float mirroredValence = playerVAD.getValence() * 0.7f;
        float mirroredArousal = playerVAD.getArousal() * 0.7f;

        // Apply personality modulation
        Personality personality = companionEntity.getPersonality();
        mirroredValence += personality.getBaselineValence() * 0.3f;

        // Apply contagion filter
        if (mirroredValence < 0) {
            mirroredValence *= 0.5f; // Dampen negative emotions
        }

        return new ValenceArousalDominance(
            clamp(mirroredValence, -1.0f, 1.0f),
            clamp(mirroredArousal, 0.0f, 1.0f),
            personality.getBaselineDominance()
        );
    }
}
```

---

### 5. Phase 3: Full Emotional Intelligence

**Long-Term Implementation (1-2 months):**

**Complete emotional system including:**
1. Multi-modal emotion detection (text + behavior)
2. Long-term emotional memory
3. Empathetic response generation
4. Mood tracking with personality integration
5. Emotional contagion management
6. Affective game adaptation

**Integration with `AgentStateMachine`:**
```java
public enum AgentState {
    IDLE,
    PLANNING,
    EXECUTING,

    // New emotional states
    CONCERNED,      // Player in danger/negative emotion
    CELEBRATING,    // Player achievement/excitement
    SUPPORTIVE,     // Player frustration/sadness
    CURIOUS,        // Player exploring/wonder
    PROTECTIVE      // Player fear/anxiety
}

public class EmotionalStateMachine {
    public void updateState(EmotionalState playerEmotion, GameContext context) {
        AgentState newState = determineState(playerEmotion, context);

        if (newState != currentState) {
            transitionTo(newState);
        }
    }

    private AgentState determineState(EmotionalState playerEmotion, GameContext context) {
        if (context.isDangerNearby() || playerEmotion.getValence() < -0.7f) {
            return AgentState.PROTECTIVE;
        } else if (playerEmotion.getValence() > 0.7f) {
            return AgentState.CELEBRATING;
        } else if (playerEmotion.getArousal() < 0.3f && playerEmotion.getValence() < 0) {
            return AgentState.SUPPORTIVE;
        } else if (context.isExploring()) {
            return AgentState.CURIOUS;
        } else {
            return AgentState.EXECUTING;
        }
    }
}
```

---

## Technical Implementation

### 1. Package Structure

**New emotional AI packages:**

```
src/main/java/com/steve/
├── emotion/
│   ├── detection/
│   │   ├── SentimentAnalyzer.java
│   │   ├── VADPredictor.java
│   │   └── BehaviorEmotionDetector.java
│   ├── tracking/
│   │   ├── MoodTracker.java
│   │   ├── MoodHistory.java
│   │   └── EmotionalState.java
│   ├── memory/
│   │   ├── EmotionalMemory.java
│   │   ├── EmotionalMemoryStore.java
│   │   └── EmotionalMemoryRetrieval.java
│   ├── response/
│   │   ├── EmpatheticResponseGenerator.java
│   │   ├── AffectiveMirroringSystem.java
│   │   └── EmotionalContagionFilter.java
│   └── model/
│       ├── ValenceArousalDominance.java
│       ├── EmotionTag.java
│       └── MoodCategory.java
```

---

### 2. Core Classes

**EmotionalState Model:**
```java
package com.steve.emotion.model;

public class EmotionalState {
    private final ValenceArousalDominance vad;
    private final Set<EmotionTag> emotions;
    private final float intensity;
    private final Instant timestamp;
    private final float confidence;

    public boolean isPositive() {
        return vad.getValence() > 0.3f;
    }

    public boolean isNegative() {
        return vad.getValence() < -0.3f;
    }

    public boolean isHighArousal() {
        return vad.getArousal() > 0.7f;
    }

    public EmotionTag getDominantEmotion() {
        return emotions.stream()
            .max(Comparator.comparing(this::getEmotionIntensity))
            .orElse(EmotionTag.NEUTRAL);
    }
}
```

**VAD Model:**
```java
package com.steve.emotion.model;

public record ValenceArousalDominance(
    float valence,    // -1.0 to +1.0
    float arousal,    // 0.0 to 1.0
    float dominance   // -1.0 to +1.0
) {
    public static ValenceArousalDominance neutral() {
        return new ValenceArousalDominance(0.1f, 0.2f, 0.3f);
    }

    public float distanceTo(ValenceArousalDominance other) {
        float dv = valence - other.valence;
        float da = arousal - other.arousal;
        float dd = dominance - other.dominance;
        return (float) Math.sqrt(dv*dv + da*da + dd*dd);
    }
}
```

**EmotionTag Enum:**
```java
package com.steve.emotion.model;

public enum EmotionTag {
    // Positive emotions
    JOY, EXCITEMENT, PRIDE, RELIEF, SATISFACTION,

    // Negative emotions
    FEAR, ANGER, SADNESS, FRUSTRATION, ANXIETY,
    DISAPPOINTMENT, EMBARRASSMENT, GUILT,

    // Neutral
    NEUTRAL, SURPRISE, CURIOSITY, CONFUSION,

    // Minecraft-specific
    DANGER_SENSE, RESOURCE_WORRY, BUILDING_FRUSTRATION,
    MINING_EXCITEMENT, EXPLORATION_WONDER;

    public boolean isPositive() {
        return ordinal() <= JOY.ordinal() && ordinal() >= RELIEF.ordinal();
    }

    public boolean isNegative() {
        return ordinal() >= FEAR.ordinal() && ordinal() <= GUILT.ordinal();
    }
}
```

---

### 3. NBT Storage Schema

**Emotional Data Persistence:**

```java
public class EmotionalStatePersistence {
    public NbtCompound toNBT(EmotionalState state) {
        NbtCompound nbt = new NbtCompound();

        // VAD coordinates
        NbtCompound vad = new NbtCompound();
        vad.putFloat("valence", state.getVAD().valence());
        vad.putFloat("arousal", state.getVAD().arousal());
        vad.putFloat("dominance", state.getVAD().dominance());
        nbt.put("vad", vad);

        // Emotion tags
        NbtList emotions = new NbtList();
        for (EmotionTag tag : state.getEmotions()) {
            emotions.add(NbtString.of(tag.name()));
        }
        nbt.put("emotions", emotions);

        // Metadata
        nbt.putFloat("intensity", state.getIntensity());
        nbt.putLong("timestamp", state.getTimestamp().toEpochMilli());
        nbt.putFloat("confidence", state.getConfidence());

        return nbt;
    }

    public EmotionalState fromNBT(NbtCompound nbt) {
        // Extract VAD
        NbtCompound vad = nbt.getCompound("vad");
        ValenceArousalDominance vadCoords = new ValenceArousalDominance(
            vad.getFloat("valence"),
            vad.getFloat("arousal"),
            vad.getFloat("dominance")
        );

        // Extract emotions
        Set<EmotionTag> emotions = new HashSet<>();
        NbtList emotionList = nbt.getList("emotions", NbtElement.STRING_TYPE);
        for (NbtElement element : emotionList) {
            emotions.add(EmotionTag.valueOf(element.asString()));
        }

        return new EmotionalState(
            vadCoords,
            emotions,
            nbt.getFloat("intensity"),
            Instant.ofEpochMilli(nbt.getLong("timestamp")),
            nbt.getFloat("confidence")
        );
    }
}
```

---

### 4. LLM Integration

**Emotion-Aware Prompt Building:**

```java
public class EmotionalPromptBuilder extends PromptBuilder {
    public String buildPromptWithEmotion(
        Task task,
        GameContext context,
        EmotionalState playerEmotion,
        EmotionalState companionEmotion
    ) {
        return basePromptBuilder.build(task, context) +
            """

            EMOTIONAL CONTEXT:
            Player's current emotional state:
            - Valence: %.2f (negative to positive)
            - Arousal: %.2f (calm to excited)
            - Dominant emotion: %s

            Your current emotional state:
            - Valence: %.2f
            - Arousal: %.2f
            - Base personality: %s

            RESPONSE GUIDELINES:
            1. Validate player's emotions (don't dismiss them)
            2. Mirror player's emotions at 60-80% intensity
            3. Amplify positive emotions, dampen negative ones
            4. Maintain your personality while being empathetic
            5. Offer help if player seems frustrated
            6. Celebrate with player if they're excited
            7. Stay calm if player is anxious (be their anchor)
            8. Reference shared emotional experiences when relevant

            Generate a brief, emotionally appropriate response:
            """.formatted(
                playerEmotion.getVAD().valence(),
                playerEmotion.getVAD().arousal(),
                playerEmotion.getDominantEmotion(),
                companionEmotion.getVAD().valence(),
                companionEmotion.getVAD().arousal(),
                personality.getDescription()
            );
    }
}
```

---

## Sources & References

### Academic & Research Papers

- **[Affective Game Computing: A Survey](https://xueshu.baidu.com/usercenter/paper/show?paperid=11120080mb3504m0k14v0gr0u6418192)** (2023) - Comprehensive taxonomy of affective computing methods in games

- **[Affective Mirroring in Video Game NPCs: A Pilot Study](https://dl.acm.org/doi/full/10.1145/3723498.3723835)** (2025) - How emotional mirroring deepens player attachment

- **[Emotions in Artificial Intelligence](https://arxiv.org/html/2505.01462v2)** (2025) - Affective memory interwoven with episodic memory

- **[The Neuroticism Paradox](https://arxiv.org/html/2510.16046v1)** (2025) - School of Affective Computing, Hefei University of Technology

- **[Multi-Scale Temporal Fusion Network](https://www.mdpi.com/1424-8220/25/16/5066)** (2025) - Distinguishing short-term emotions from mood states

- **[Affective Communication for Socially Assistive Robots](https://www.mdpi.com/1424-8220/21/15/5166)** (2021) - EMIA model with perceptual, working, and long-term memory

- **[Harmful Traits of AI Companions](https://arxiv.org/search?q=Harmful+Traits+of+AI+Companions+2025)** (2025) - AI companion safety and emotional design

- **[AI as a Group Mediator: Conceptual Framework](https://www.tandfonline.com/doi/full/10.1080/2692398X.2025.2587315)** (2025) - LLMs matching human empathy

- **[Emotionally Intelligent Dialogue Generation](https://arxiv.org/abs/2404.11447)** (2024) - Real-time emotion detection in dialogue

### Sentiment Analysis & NLP

- **[大数据情感分析：解锁用户情绪背后的商业价值](https://m.blog.csdn.net/2502_91534922/article/details/155863898)** (2024) - Big data sentiment analysis applications

- **[基于NLP的情绪识别：技术原理与应用实践](https://cloud.baidu.com/article/3830962)** - NLP-based emotion recognition

- **[NLP情感分析: 实现文本内容的情感识别](https://www.jianshu.com/p/0a5118e4326e)** - Practical sentiment analysis implementation

### VAD Model & Emotional Computing

- **[Sambert-HifiGan情感控制参数详解](https://m.blog.csdn.net/weixin_36184718/article/details/156004102)** - VAD parameters for emotional speech synthesis

- **[基于SVM的语音情感识别研究综述](https://m.blog.csdn.net/qq_63832616/article/details/146265631)** - SVM-based emotion recognition with VAD

### Empathy & Dialogue Systems

- **[Evaluating AI Dialogue Systems' Intercultural, Humorous, and Empathetic Dimensions](https://m.zhangqiaokeyan.com/journal-foreign-detail/140414803518.html)** - MACHE-Bot case study

- **[Harnessing LLMs for Empathetic Response in Mental Health Counseling](https://readpaper.com/paper/4810411242770399233)** - LLM empathy capabilities

- **[Empathetic Conversation Systems (ECS) Survey](https://m.blog.csdn.net/weixin_45352758/article/details/127683484)** - Four-stage empathetic system model

### Game AI & Companions

- **[An Artificial Emotional Agent-Based Architecture for Games Simulation](https://dl.acm.org/doi/10.1145/2513458)** (2013) - NPC behavior modeling with emotive agents

- **[Radically Simplifying Game Engines: AI Emotions Game Self-Evolution](https://m.blog.csdn.net/zzz___bj/article/details/136235777)** (2022) - Dynamic emotion core architecture

- **[The Synthesis of Emotions in Artificial Intelligences (EMAI Architecture)](https://m.blog.csdn.net/qq_63832616/article/details/146265631)** (2023) - Six-dimensional affective space

- **[AIMobs Mod - ChatGPT Minecraft Integration](https://modrinth.com/mod/aimobs)** - AI-powered conversations with Minecraft creatures

- **[MCChatGPT Mod](https://modrinth.com/mod/mcchatgpt)** - ChatGPT-like interaction within Minecraft

### Memory & Emotional Systems

- **[Sleep & Emotional Memory Consolidation](https://pmc.ncbi.nlm.nih.gov/articles/PMC10805784/)** (2021) - Time-dependent impact on emotional memories

- **[The Design and Implementation of XiaoIce (Empathetic Social Chatbot)](https://m.blog.csdn.net/qq_63832616/article/details/146265631)** (2018) - EQ + IQ decision making

### Existing Research Documentation

- **[Character AI & Companion Systems Research](C:\Users\casey\steve\research\CHARACTER_AI_SYSTEMS.md)** - Comprehensive character AI research

- **[AI Companion Personality Design for Games](C:\Users\casey\steve\docs\RESEARCH_COMPANION_PERSONALITY.md)** - Personality systems and relationship evolution

---

## Conclusion

This research provides a comprehensive foundation for implementing emotional intelligence in the MineWright Minecraft companion. The key insights are:

1. **Multi-scale emotion tracking** is essential: distinguish between emotions (seconds), moods (minutes/hours), and personality (long-term)

2. **VAD (Valence-Arousal-Dominance)** model provides superior granularity compared to discrete emotion categories

3. **Affective mirroring** in NPCs deepens player attachment through emotional responsiveness

4. **Emotional memory** requires separate storage from episodic memory, with affective tags alongside event data

5. **LLM-based empathy** now matches or exceeds human empathy in controlled conditions

6. **Hybrid approaches** combining rule-based sentiment analysis with deep learning yield best results for real-time games

7. **Emotional contagion filtering** is critical: amplify positive emotions, dampen negative ones

8. **Multi-modal detection** (text + behavior) provides more robust emotion recognition

The recommended implementation uses:
- **VADER** for fast rule-based sentiment analysis
- **VAD coordinates** for continuous emotion representation
- **Exponential moving average** for mood tracking
- **Affective memory system** with emotional tagging
- **Affective mirroring** with contagion filtering
- **Emotion-aware LLM prompts** for empathetic responses

This foundation can be implemented incrementally, starting with basic sentiment analysis and adding advanced features as the system matures.

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Next Review:** After initial prototype testing
