# Emotional AI Framework for Game Companions

## 3.1 The OCC Model of Emotions

The Ortony-Clore-Collins (OCC) model, first formalized in *The Cognitive Structure of Emotions* (1988), provides a cognitively-based appraisal theory of emotions that has become foundational in computational affective computing. Unlike dimensional models that reduce emotions to valence-arousal coordinates, the OCC model posits a taxonomy of 22 distinct emotion types arising from cognitive appraisals of events, agents, and objects.

### Cognitive Structure

The OCC model organizes emotions around three valuation classes:

1. **Event-based emotions**: Reactions to consequences of events, distinguished by whether events affect oneself, others, or are merely observed. These include *joy* and *distress* (prospective), *happy* and *sorry* (retrospective), and *hope* and *fear* (prospective/uncertain). The intensity depends on the *desirability* of the event outcome, its *likelihood*, and its *realization* status.

2. **Agent-based emotions**: Reactions to actions of agents, categorized as *approving/disapproving* (judging others' actions) and *pride/shame* (judging one's own actions). These depend on the *praiseworthiness* of the action and the *standing* of the agent relative to the evaluator.

3. **Object-based emotions**: Emotions toward atemporal objects or attributes, including *liking/disliking* and *love/hate*, based purely on *appealingness* without temporal or causal considerations.

### Intensity Calculations

The formal model specifies intensity functions for each emotion. For example, the intensity of joy at time t is:

```
I(joy,t) = D(event) × L(realization) × G(global)
```

Where D is desirability, L is likelihood/realization, and G represents global factors like unexpectedness and arousal. This mathematical formalization enables direct computational implementation while maintaining psychological validity.

The OCC model's structured taxonomy and intensity calculations make it particularly suitable for game AI, where predictable yet nuanced emotional responses enhance companion believability without requiring machine learning approaches that may produce unpredictable results.

## 3.2 Affective Computing Foundations

Rosalind Picard's seminal work *Affective Computing* (1997) established the interdisciplinary field of computing that relates to, arises from, or deliberately influences emotions. Picard distinguished between two complementary challenges:

**Emotion Recognition**: Identifying human emotional states through physiological signals, facial expressions, vocal analysis, or behavioral patterns. In games, this enables adaptive difficulty and empathetic response systems.

**Emotion Generation**: Synthesizing appropriate emotional expressions in artificial agents. This requires both *computational models* of emotion and *expression mechanisms* that convey internal states through behavior, dialogue, appearance, or movement.

Computational models of emotion fall into three categories:

1. **Categorical Models**: Discrete emotion labels (basic emotions: happiness, sadness, anger, fear, disgust, surprise)
2. **Dimensional Models**: Continuous coordinates in valence-arousal space (e.g., PAD: Pleasure-Arousal-Dominance)
3. **Appraisal Models**: Emotions as consequences of cognitive evaluation (e.g., OCC)

Game AI has historically favored simple dimensional or categorical approaches for computational efficiency (Reilly, 1996). However, appraisal models like OCC offer superior explanatory power for social and emotional agents because they connect emotions to understandable cognitive processes.

The integration of affective computing in games serves multiple purposes:
- **Believability**: Emotional consistency creates coherent, memorable characters
- **Narrative Impact**: Emotional engagement enhances story resonance
- **Social Dynamics**: Emotions drive emergent interpersonal relationships
- **Player Investment**: Emotional attachment increases retention

## 3.3 Implementation: OCC for Minecraft Companions

The following Java implementation demonstrates a complete OCC-based emotional system for Minecraft companion agents. This system integrates with the existing Steve AI architecture while adding sophisticated emotional modeling.

```java
package com.steve.ai.emotion;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emotional AI system implementing the OCC model for Minecraft companions.
 *
 * <p>Emotions are organized into three valuation classes:
 * <ul>
 *   <li>Event-based: joy, distress, hope, fear, happy, sorry</li>
 *   <li>Agent-based: pride, shame, admiration, reproach, gratitude, anger</li>
 *   <li>Object-based: liking, disliking, love, hate</li>
 * </ul>
 *
 * <p>All emotions decay over time toward baseline, preventing emotional saturation.
 *
 * @author Steve AI Team
 * @see Ortony, Clore, & Collins (1988)
 */
public class EmotionalCompanionAgent {

    /**
     * Complete enumeration of OCC emotion types with intensity ranges.
     */
    public enum Emotion {
        // Event-based emotions (consequences of events)
        JOY(0.0, 1.0),
        DISTRESS(0.0, 1.0),
        HAPPY_FOR(0.0, 1.0),
        SORRY_FOR(0.0, 1.0),
        GLOATING(0.0, 1.0),
        RESENTMENT(0.0, 1.0),
        HOPE(0.0, 1.0),
        FEAR(0.0, 1.0),
        SATISFACTION(0.0, 1.0),
        FEARS_CONFIRMED(0.0, 1.0),
        RELIEF(0.0, 1.0),
        DISAPPOINTMENT(0.0, 1.0),

        // Agent-based emotions (actions of agents)
        PRIDE(0.0, 1.0),
        SHAME(0.0, 1.0),
        ADMIRATION(0.0, 1.0),
        REPROACH(0.0, 1.0),
        GRATITUDE(0.0, 1.0),
        ANGER(0.0, 1.0),
        GRATITUDE(0.0, 1.0),

        // Object-based emotions (atemporal attributes)
        LIKING(0.0, 1.0),
        DISLIKING(0.0, 1.0),
        LOVE(0.0, 1.0),
        HATE(0.0, 1.0);

        private final double min;
        private final double max;

        Emotion(double min, double max) {
            this.min = min;
            this.max = max;
        }

        public double clamp(double value) {
            return Math.max(min, Math.min(max, value));
        }
    }

    /** Current emotion intensities, accessed concurrently by game threads */
    private final ConcurrentHashMap<Emotion, Double> emotionIntensities;

    /** Traits affecting emotional baseline and reactivity */
    private final EmotionalTraits traits;

    /** Memory of significant events for emotional context */
    private final EmotionalMemory emotionalMemory;

    /** Behavioral expression system */
    private final EmotionalExpression expression;

    /** Decay rates for each emotion (per second) */
    private static final Map<Emotion, Double> DECAY_RATES = ImmutableMap.of(
        // Fast-decaying emotions (momentary reactions)
        Emotion.JOY, 0.15,
        Emotion.DISTRESS, 0.10,
        Emotion.HOPE, 0.20,
        Emotion.FEAR, 0.12,
        Emotion.SATISFACTION, 0.18,
        Emotion.RELIEF, 0.20,

        // Medium-decaying emotions (short-term attitudes)
        Emotion.HAPPY_FOR, 0.08,
        Emotion.SORRY_FOR, 0.07,
        Emotion.GLOATING, 0.10,
        Emotion.RESENTMENT, 0.06,
        Emotion.ADMIRATION, 0.05,
        Emotion.REPROACH, 0.05,

        // Slow-decaying emotions (long-term relationships)
        Emotion.GRATITUDE, 0.02,
        Emotion.ANGER, 0.015,
        Emotion.PRIDE, 0.03,
        Emotion.SHAME, 0.025,

        // Very slow-decaying emotions (stable preferences)
        Emotion.LIKING, 0.005,
        Emotion.DISLIKING, 0.005,
        Emotion.LOVE, 0.001,
        Emotion.HATE, 0.001
    );

    /**
     * Creates a new emotional agent with baseline emotions.
     *
     * @param traits Personality traits affecting emotional responses
     */
    public EmotionalCompanionAgent(EmotionalTraits traits) {
        this.traits = traits;
        this.emotionIntensities = new ConcurrentHashMap<>();
        this.emotionalMemory = new EmotionalMemory();
        this.expression = new EmotionalExpression();

        // Initialize baseline emotions
        for (Emotion e : Emotion.values()) {
            emotionIntensities.put(e, 0.0);
        }
    }

    /**
     * Appraises an event and generates appropriate emotional responses.
     *
     * <p>This implements the OCC cognitive appraisal process:
     * <ol>
     *   <li>Determine valuation class (event/agent/object)</li>
     *   <li>Calculate desirability/praiseworthiness/appealingness</li>
     *   <li>Account for likelihood and realization status</li>
     *   <li>Apply trait-based modifiers</li>
     *   <li>Update emotional intensities with clamping</li>
     * </ol>
     *
     * @param event The event to appraise
     */
    public void appraiseEvent(EmotionalEvent event) {
        double desirability = calculateDesirability(event);
        double likelihood = event.getLikelihood();
        double praiseworthiness = calculatePraiseworthiness(event);

        // Event-based emotions
        if (event.affectsSelf()) {
            if (event.isProspective()) {
                if (likelihood < 1.0) {
                    // Hopeful about desirable uncertain outcomes
                    updateEmotion(Emotion.HOPE, desirability * likelihood * traits.reactivity);
                    // Fearful about undesirable uncertain outcomes
                    updateEmotion(Emotion.FEAR, -desirability * likelihood * traits.reactivity);
                }
            } else if (event.isRealized()) {
                // Joy at desirable realized events
                updateEmotion(Emotion.JOY, desirability * traits.reactivity);
                updateEmotion(Emotion.SATISFACTION, desirability * 0.8 * traits.reactivity);
            } else {
                // Distress at undesirable realized events
                updateEmotion(Emotion.DISTRESS, -desirability * traits.reactivity);
                updateEmotion(Emotion.DISAPPOINTMENT, -desirability * 0.8 * traits.reactivity);
            }
        }

        // Agent-based emotions (evaluating other agents)
        if (event.hasAgent()) {
            if (event.affectsOtherPositively()) {
                // Happy for others' good fortune
                updateEmotion(Emotion.HAPPY_FOR,
                    desirability * traits.empathy * traits.reactivity);
            } else if (event.affectsOtherNegatively()) {
                // Sorry for others' misfortune
                updateEmotion(Emotion.SORRY_FOR,
                    -desirability * traits.empathy * traits.reactivity);
            }

            // Gratitude for helpful actions
            if (event.isHelpfulAction()) {
                double gratitudeIntensity = praiseworthiness * desirability * traits.reactivity;
                updateEmotion(Emotion.GRATITUDE, gratitudeIntensity);
            }

            // Anger at harmful actions
            if (event.isHarmfulAction()) {
                double angerIntensity = -praiseworthiness * desirability * traits.reactivity;
                updateEmotion(Emotion.ANGER, angerIntensity);
            }
        }

        // Store significant emotional events in memory
        if (getMaximumEmotionIntensity() > 0.5) {
            emotionalMemory.recordEvent(event, getCurrentEmotions());
        }
    }

    /**
     * Calculates the desirability of an event outcome based on goals,
     * preferences, and current needs.
     *
     * @param event The event to evaluate
     * @return Desirability score from -1.0 (very undesirable) to 1.0 (very desirable)
     */
    private double calculateDesirability(EmotionalEvent event) {
        double desirability = 0.0;

        // Survival-critical events have high absolute desirability
        if (event.isLifeThreatening()) {
            desirability -= 0.9;
        }

        // Resource acquisition based on scarcity
        if (event.givesResources()) {
            desirability += event.getResourceScarcity() * traits.materialism;
        }

        // Social bonding events
        if (event.isSocialInteraction()) {
            desirability += traits.sociability * 0.5;
        }

        // Achievement and progress
        if (event.isAchievement()) {
            desirability += traits.ambition * event.getAchievementSignificance();
        }

        // Clamp to valid range
        return Math.max(-1.0, Math.min(1.0, desirability));
    }

    /**
     * Calculates the praiseworthiness of an agent's action based on
     * social norms and the action's consequences.
     *
     * @param event The action event to evaluate
     * @return Praiseworthiness score from -1.0 (very blameworthy) to 1.0 (very praiseworthy)
     */
    private double calculatePraiseworthiness(EmotionalEvent event) {
        if (!event.isAction()) {
            return 0.0;
        }

        double praiseworthiness = 0.0;

        // Helpful actions are praiseworthy
        if (event.isHelpfulAction()) {
            praiseworthiness += event.getHelpMagnitude();
        }

        // Harmful actions are blameworthy
        if (event.isHarmfulAction()) {
            praiseworthiness -= event.getHarmMagnitude();
        }

        // Consider social norms and intent
        praiseworthiness *= event.getIntentGoodness();

        return Math.max(-1.0, Math.min(1.0, praiseworthiness));
    }

    /**
     * Updates an emotion's intensity by the specified delta, applying
     * clamping to keep within valid range.
     *
     * @param emotion The emotion to update
     * @param delta The intensity change (can be negative)
     */
    private void updateEmotion(Emotion emotion, double delta) {
        double current = emotionIntensities.get(emotion);
        double updated = emotion.clamp(current + delta);
        emotionIntensities.put(emotion, updated);
    }

    /**
     * Returns the current intensity of a specific emotion.
     *
     * @param emotion The emotion to query
     * @return Current intensity from 0.0 to 1.0
     */
    public double getEmotionIntensity(Emotion emotion) {
        return emotionIntensities.get(emotion);
    }

    /**
     * Calculates the dominant emotion currently active.
     *
     * @return The emotion with highest intensity, or null if all are at baseline
     */
    public Emotion getDominantEmotion() {
        return emotionIntensities.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .filter(e -> e.getValue() > 0.1)
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    /**
     * Gets the maximum intensity across all emotions.
     *
     * @return Maximum emotion intensity
     */
    public double getMaximumEmotionIntensity() {
        return emotionIntensities.values().stream()
            .max(Double::compare)
            .orElse(0.0);
    }

    /**
     * Returns a snapshot of all current emotion intensities.
     *
     * @return Immutable map of emotions to their intensities
     */
    public Map<Emotion, Double> getCurrentEmotions() {
        return ImmutableMap.copyOf(emotionIntensities);
    }

    /**
     * Calculates overall mood from emotional state.
     *
     * <p>Mood is a longer-term affective state derived from weighted
     * combinations of current emotions. This uses a simplified dimensional
     * mapping to valence-arousal coordinates.
     *
     * @return Current mood as valence (negative=unpleasant, positive=pleasant)
     */
    public double calculateMood() {
        double valence = 0.0;

        // Positive emotions contribute to positive valence
        valence += getEmotionIntensity(Emotion.JOY) * 1.2;
        valence += getEmotionIntensity(Emotion.HOPE) * 0.8;
        valence += getEmotionIntensity(Emotion.HAPPY_FOR) * 0.6;
        valence += getEmotionIntensity(Emotion.GRATITUDE) * 0.7;
        valence += getEmotionIntensity(Emotion.LIKING) * 0.5;
        valence += getEmotionIntensity(Emotion.LOVE) * 1.0;

        // Negative emotions contribute to negative valence
        valence -= getEmotionIntensity(Emotion.DISTRESS) * 1.2;
        valence -= getEmotionIntensity(Emotion.FEAR) * 0.9;
        valence -= getEmotionIntensity(Emotion.ANGER) * 1.0;
        valence -= getEmotionIntensity(Emotion.SHAME) * 0.7;
        valence -= getEmotionIntensity(Emotion.HATE) * 1.1;

        // Arousal-adjacent emotions affect intensity
        double arousal = 0.0;
        arousal += getEmotionIntensity(Emotion.FEAR) * 1.3;
        arousal += getEmotionIntensity(Emotion.ANGER) * 1.1;
        arousal += getEmotionIntensity(Emotion.JOY) * 0.8;
        arousal += getEmotionIntensity(Emotion.DISTRESS) * 1.0;

        return valence * (1.0 + arousal * 0.2);  // Arousal amplifies valence
    }

    /**
     * Updates emotional state for one game tick.
     * Called each tick to apply emotional decay.
     *
     * @param deltaTime Time since last update in seconds
     */
    public void tick(double deltaTime) {
        // Apply decay to all emotions
        for (Emotion emotion : Emotion.values()) {
            double decay = DECAY_RATES.getOrDefault(emotion, 0.1);
            double current = emotionIntensities.get(emotion);
            double decayed = current * Math.pow(1.0 - decay, deltaTime);
            emotionIntensities.put(emotion, decayed);
        }

        // Express current emotional state through behavior
        expression.tick(this, deltaTime);
    }

    /**
     * Records a shared emotional experience with another agent,
     * strengthening social bonds.
     *
     * @param otherAgent The other agent
     * @param event The shared event
     * @param sharedEmotion The emotion felt by both
     */
    public void recordSharedExperience(
        EmotionalCompanionAgent otherAgent,
        EmotionalEvent event,
        Emotion sharedEmotion
    ) {
        // Shared positive experiences increase liking
        if (sharedEmotion == Emotion.JOY || sharedEmotion == Emotion.HOPE) {
            updateEmotion(Emotion.LIKING, 0.05);
            updateEmotion(Emotion.GRATITUDE, 0.03);
        }

        // Shared trauma creates strong bonds
        if (sharedEmotion == Emotion.DISTRESS || sharedEmotion == Emotion.FEAR) {
            updateEmotion(Emotion.LIKING, 0.08);  // Stronger effect
            emotionalMemory.recordSharedTrauma(otherAgent, event);
        }
    }

    /**
     * Called when this agent and another are reunited after separation.
     * Generates appropriate emotional response based on relationship and
     * separation duration.
     *
     * @param otherAgent The agent being reunited with
     * @param separationDuration How long they were separated
     */
    public void onReunion(EmotionalCompanionAgent otherAgent, double separationDuration) {
        double relationshipStrength = getEmotionIntensity(Emotion.LIKING);

        if (relationshipStrength > 0.5) {
            // Joy at reunion with liked companion
            updateEmotion(Emotion.JOY, relationshipStrength * 0.5);

            // Longer separations with strong bonds produce more intense emotions
            if (separationDuration > 300.0) {  // 5 minutes
                updateEmotion(Emotion.RELIEF, relationshipStrength * 0.3);
            }
        }

        // Update emotional memory
        emotionalMemory.recordReunion(otherAgent, separationDuration);
    }
}

/**
 * Represents an event that can be emotionally appraised by OCC agents.
 */
class EmotionalEvent {
    private final String type;
    private final double likelihood;
    private final boolean realized;
    private final boolean prospective;
    private final boolean affectsSelf;
    private final boolean affectsOtherPositively;
    private final boolean affectsOtherNegatively;

    // Cached calculations
    private Double desirabilityCache = null;
    private Double praiseworthinessCache = null;

    // Event-specific properties
    private boolean lifeThreatening = false;
    private boolean givesResources = false;
    private double resourceScarcity = 0.0;
    private boolean socialInteraction = false;
    private boolean achievement = false;
    private double achievementSignificance = 0.0;
    private boolean isAction = false;
    private boolean helpfulAction = false;
    private boolean harmfulAction = false;
    private double helpMagnitude = 0.0;
    private double harmMagnitude = 0.0;
    private double intentGoodness = 1.0;

    public EmotionalEvent(String type, double likelihood, boolean realized) {
        this.type = type;
        this.likelihood = likelihood;
        this.realized = realized;
        this.prospective = !realized && likelihood < 1.0;
        this.affectsSelf = true;
        this.affectsOtherPositively = false;
        this.affectsOtherNegatively = false;
    }

    // Getters
    public double getLikelihood() { return likelihood; }
    public boolean isRealized() { return realized; }
    public boolean isProspective() { return prospective; }
    public boolean affectsSelf() { return affectsSelf; }
    public boolean affectsOtherPositively() { return affectsOtherPositively; }
    public boolean affectsOtherNegatively() { return affectsOtherNegatively; }
    public boolean isLifeThreatening() { return lifeThreatening; }
    public boolean givesResources() { return givesResources; }
    public double getResourceScarcity() { return resourceScarcity; }
    public boolean isSocialInteraction() { return socialInteraction; }
    public boolean isAchievement() { return achievement; }
    public double getAchievementSignificance() { return achievementSignificance; }
    public boolean isAction() { return isAction; }
    public boolean isHelpfulAction() { return helpfulAction; }
    public boolean isHarmfulAction() { return harmfulAction; }
    public double getHelpMagnitude() { return helpMagnitude; }
    public double getHarmMagnitude() { return harmMagnitude; }
    public double getIntentGoodness() { return intentGoodness; }
    public boolean hasAgent() { return isAction; }

    // Fluent setters for event configuration
    public EmotionalEvent lifeThreatening() {
        this.lifeThreatening = true;
        return this;
    }

    public EmotionalEvent givesResources(double scarcity) {
        this.givesResources = true;
        this.resourceScarcity = scarcity;
        return this;
    }

    public EmotionalEvent socialInteraction() {
        this.socialInteraction = true;
        return this;
    }

    public EmotionalEvent achievement(double significance) {
        this.achievement = true;
        this.achievementSignificance = significance;
        return this;
    }

    public EmotionalEvent action(boolean helpful, boolean harmful) {
        this.isAction = true;
        this.helpfulAction = helpful;
        this.harmfulAction = harmful;
        return this;
    }
}

/**
 * Personality traits that modulate emotional responses.
 * All traits range from 0.0 (absent) to 1.0 (very strong).
 */
class EmotionalTraits {
    double reactivity = 0.7;        // How strongly emotions are felt
    double empathy = 0.6;           // Emotional sensitivity to others
    double sociability = 0.5;       // Enjoyment of social interaction
    double materialism = 0.4;       // Importance of material resources
    double ambition = 0.5;          // Drive for achievement

    public EmotionalTraits() {}

    public EmotionalTraits(
        double reactivity, double empathy, double sociability,
        double materialism, double ambition
    ) {
        this.reactivity = reactivity;
        this.empathy = empathy;
        this.sociability = sociability;
        this.materialism = materialism;
        this.ambition = ambition;
    }
}

/**
 * Memory system for emotionally significant events.
 */
class EmotionalMemory {
    private final Map<String, EmotionalEvent> significantEvents = new HashMap<>();

    public void recordEvent(EmotionalEvent event, Map<EmotionalCompanionAgent.Emotion, Double> emotions) {
        significantEvents.put(event.type, event);
    }

    public void recordSharedTrauma(EmotionalCompanionAgent other, EmotionalEvent event) {
        // Trauma bonding creates lasting relationship changes
    }

    public void recordReunion(EmotionalCompanionAgent other, double duration) {
        // Track separations for appropriate emotional responses
    }
}

/**
 * Expresses internal emotional states through observable behaviors.
 */
class EmotionalExpression {
    public void tick(EmotionalCompanionAgent agent, double deltaTime) {
        Emotion dominant = agent.getDominantEmotion();
        if (dominant != null) {
            expressEmotion(dominant, agent.getEmotionIntensity(dominant));
        }
    }

    private void expressEmotion(Emotion emotion, double intensity) {
        // Map emotions to Minecraft behaviors:
        // - Joy: Jump animation, positive sounds
        // - Fear: Running away, cowering
        // - Anger: Aggressive posturing
        // - Gratitude: Gift-giving behavior
        // etc.
    }
}
```

### Integration Points

This emotional system integrates with the Steve AI architecture at several points:

1. **Task Execution**: Action callbacks trigger emotional events (e.g., completing tasks → joy/satisfaction)
2. **World Events**: Combat, resource gathering, and exploration generate appraisals
3. **Social Interactions**: Player commands and interactions create agent-based emotions
4. **Decision Making**: Emotional state weights task priorities in the planning system

### Minecraft-Specific Extensions

The base OCC model can be extended with Minecraft-domain emotions:

```java
public enum MinecraftEmotion {
    // Resource-based emotions
    EXCITEMENT_DISCOVERY,    // Finding new biomes/structures
    SATISFACTION_CRAFTING,   // Successfully crafting items

    // Combat emotions
    VALOR,                   // Fighting despite fear
    PANIC,                   // Overwhelmed in combat

    // Building emotions
    PRIDE_CREATION,          // Completing structures
    FRUSTRATION_BUILDING     // Placement failures
}
```

## 3.4 Comparison: OCC Model vs Simple Approval Systems

Game AI relationship systems historically use simple approval or reputation scores. The OCC model offers significant advantages for believable companions:

| Aspect | OCC Model | Simple Approval |
|--------|-----------|-----------------|
| **Emotional Dimensions** | 22 distinct emotion types | 1 scale (approval/disapproval) |
| **Cognitive Basis** | Appraisal-based, psychologically grounded | Simple arithmetic updates |
| **Temporal Dynamics** | Different decay rates per emotion | Uniform or no decay |
| **Behavioral Expression** | Rich, emotion-specific behaviors | Generic positive/negative responses |
| **Believability** | Very High - emotions match cognition | Medium - responses feel mechanical |
| **Implementation Complexity** | High - requires emotion taxonomy and appraisal | Low - single integer value |
| **Computational Cost** | Moderate - O(22) per event update | Minimal - O(1) per event |
| **Explainability** | High - emotions traceable to appraisals | Low - unclear why approval changed |
| **Narrative Depth** | Enables complex relationship arcs | Limits to "likes/dislikes" |
| **Emergent Behavior** | Yes - emotions combine in unexpected ways | No - behavior is deterministic |

### When Simple Approval Suffices

Simple approval systems are appropriate for:
- **Merchants**: Transaction-focused NPCs
- **Quest Givers**: Task-based interactions
- **Background NPCs**: Minimal relationship depth

### When OCC is Necessary

The OCC model shines for:
- **Companions**: Long-term party members
- **Party-based RPGs**: Inter-character dynamics
- **Narrative Games**: Emotional storytelling
- **Sandbox Games**: Persistent, evolving relationships

## 3.5 Minecraft Applications

The Minecraft sandbox environment provides unique opportunities for emotional AI that enhance the autonomous companion concept. Several application domains demonstrate the value of OCC-based emotions:

### Shared Trauma Bonding

Surviving life-threatening situations together creates profound social bonds in human psychology. In Minecraft, this translates to:

- **Combat Survivors**: Fighting off creeper attacks or zombie hordes generates shared distress. When both companions survive, the relief and residual fear strengthen attachment.

- **Environmental Hazards**: Navigating dangerous terrain (cliffs, lava, drowning) creates mutual dependence. Successful traversal generates joy and pride.

- **Base Defense**: Protecting a shared home from raiders or monsters creates investment in mutual safety.

The implementation supports this through:
```java
if (event.isLifeThreatening() && event.getSurvivors().contains(companion)) {
    double traumaBond = 0.08;  // Significant boost
    updateEmotion(Emotion.LIKING, traumaBond);
    updateEmotion(Emotion.GRATITUDE, 0.05);
    emotionalMemory.recordSharedTrauma(companion, event);
}
```

This creates emergent narrative moments where companions who have "been through hell together" develop lasting bonds visible to players.

### Gratitude Systems

Gift-giving and help behaviors provide tangible expressions of gratitude. Unlike simple approval systems where gratitude is just a number, OCC agents can:

- **Express Gratitude Behaviorally**: Companions can gift resources they've gathered, prioritize tasks the player requested, or offer protective behavior.

- **Remember Favors**: The emotional memory system tracks who helped whom, enabling reciprocal altruism.

- **Differentiate Gratitude Types**:
  - *Material gratitude* for resources (liking + small amount)
  - *Life-saving gratitude* for combat protection (gratitude + large amount)
  - *Emotional support gratitude* for shared experiences (liking + love)

```java
public void expressGratitudeTowards(Entity target, double intensity) {
    if (intensity > 0.7) {
        // High gratitude: gift valuable resources
        ItemStack gift = selectValuableResource();
        offerItem(target, gift);
    } else if (intensity > 0.3) {
        // Medium gratitude: prioritize their requests
        taskPriorities.boostPlayerRequests(target, 0.5);
    } else {
        // Low gratitude: express verbally
        sendChatMessage("Thank you for the help!");
    }
}
```

### Separation and Reunion Behaviors

Minecraft's exploration mechanics naturally separate players from companions. OCC emotions make these moments meaningful:

- **Separation Anxiety**: High-liking companions experience distress when separated
- **Joyful Reunion**: Reunions generate proportional joy based on relationship strength
- **Separation Duration**: Long separations with strong bonds produce relief upon reunion
- **Reunion Rituals**: Companions can run to the player, offer gifts, or express affection

```java
public void onReunion(EmotionalCompanionAgent other, double separationDuration) {
    double bond = getEmotionIntensity(Emotion.LIKING);

    if (bond > 0.8 && separationDuration > 300) {
        // Strong bonds + long separation = intense reunion
        updateEmotion(Emotion.JOY, bond * 0.6);
        updateEmotion(Emotion.RELIEF, bond * 0.4);

        // Express through behavior
        runTowards(other);
        offerGift(other);
        emotionalMemory.recordReunion(other, separationDuration);
    }
}
```

### Moral Conflict Mechanics

Minecraft's ethical ambiguity creates opportunities for moral emotions:

- **Pride vs Shame**: Companions feel pride at building achievements, shame at destruction or griefing
- **Admiration vs Reproach**: Evaluating player moral choices (e.g., attacking villagers)
- **Moral Disagreement**: Companions with different values can experience reproach toward player actions
- **Forgiveness**: Long-term relationships allow recovery from moral transgressions

```java
public void appraiseMoralAction(EmotionalEvent action, boolean isMoral) {
    double moralImpact = isMoral ? 0.5 : -0.5;

    if (action.isPlayerAction()) {
        if (isMoral) {
            updateEmotion(Emotion.ADMIRATION, moralImpact * traits.empathy);
        } else {
            updateEmotion(Emotion.REPROACH, -moralImpact * traits.empathy);
        }
    } else if (action.isSelfAction()) {
        if (isMoral) {
            updateEmotion(Emotion.PRIDE, moralImpact);
        } else {
            updateEmotion(Emotion.SHAME, -moralImpact);
        }
    }
}
```

This enables narrative arcs where companions challenge player behavior or develop distinct moral identities.

### Emotional Learning and Adaptation

The OCC framework enables companions to learn emotional associations:

- **Biome Preferences**: Repeated positive experiences in certain biomes increase object-based liking
- **Mob Fears**: Dangerous encounters create lasting fear responses to specific mobs
- **Player Personality Modeling**: Companions learn individual player interaction styles
- **Shared Achievement Pride**: Celebrating completed structures or defeated bosses

This creates emergent personality where each companion develops unique emotional profiles based on their experiences with specific players and worlds.

## References

- Ortony, A., Clore, G. L., & Collins, A. (1988). *The Cognitive Structure of Emotions*. Cambridge University Press.

- Picard, R. W. (1997). *Affective Computing*. MIT Press.

- Reilly, W. S. (1996). *Believable Social and Emotional Agents* (Doctoral dissertation, Carnegie Mellon University).

- Bartneck, C. (2002). Integrating the OCC model of emotions in embodied characters. *Proceedings of the Workshop on Virtual Conversational Characters: Applications, Methods, and Research Challenges*.

- Hudlicka, E. (2008). Affective computing for game design. *Proceedings of the 4th International Conference on Foundations of Digital Games*.

- Dias, J., & Paiva, A. (2005). Feeling and reasoning: A computational model for emotional characters. *Proceedments of the Portuguese Conference on Artificial Intelligence*.

---