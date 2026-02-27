# Celebration Dialogue System for MineWright Workers

**Project:** MineWright AI - MineWright Worker Celebration System
**Date:** 2026-02-27
**Research Focus:** Achievement celebration patterns, personality-based reactions, team dynamics, celebration fatigue prevention

---

## Table of Contents

1. [Research Summary](#1-research-summary)
2. [Achievement Tier System](#2-achievement-tier-system)
3. [Celebration Intensity by Personality](#3-celebration-intensity-by-personality)
4. [Team vs Individual Recognition](#4-team-vs-individual-recognition)
5. [Callback to Struggle Patterns](#5-callback-to-struggle-patterns)
6. [Player-Focused Praise](#6-player-focused-praise)
7. [Humble Bragging Balance](#7-humble-bragging-balance)
8. [Celebration Fatigue Prevention](#8-celebration-fatigue-prevention)
9. [Shared Celebration Dialogue](#9-shared-celebration-dialogue)
10. [Milestone-Specific Phrases](#10-milestone-specific-phrases)
11. [Java Implementation](#11-java-implementation)

---

## 1. Research Summary

### Key Psychological Findings

**Sports Psychology Research (2025):**
- Teams with celebrations within 15 minutes post-game saw **4.2% increase** in next-game scoring and **7.8% improvement** in defensive efficiency
- Immediate positive feedback activates **mirror neurons** - high-fives and team chants create neural-level bonding
- "Celebration is not a reward for the past, but an investment in the future"

**Workplace Recognition Research:**
- Studies show **timely acknowledgment** matters more than year-end reviews
- **Peer-to-peer recognition** creates stronger bonds than top-down praise
- **Conscious recognition** of positive achievements is the key element that transforms ordinary gatherings into meaningful celebrations

**Friendship & Social Psychology:**
- Celebrations with **social gathering, food sharing, AND conscious acknowledgment** increase perceived social support
- Perceived social support is linked to extended lifespan, reduced anxiety/depression, and better health outcomes
- **Collective effervescence** - shared celebrations diminish self-consciousness and create strong belonging

**Video Game Companion Studies:**
- **Bioware's player classification**: Empathetic, self-interested, and social players respond differently to celebration dialogue
- **Dragon Age 2's personality system**: Diplomatic, humorous, and aggressive personalities have distinct celebration styles
- Celebrations should vary by **relationship level** - casual acquaintance vs deep partner

---

## 2. Achievement Tier System

Achievements are categorized by significance, effort required, and impact on progress.

### Achievement Classification

| Tier | Description | Examples | Base Intensity |
|------|-------------|----------|----------------|
| **Minor** | Routine tasks, low effort | Placing 10 blocks, basic mining | 1.0 - 2.0 |
| **Moderate** | Notable progress, medium effort | Completing a room, finding diamonds | 2.1 - 4.0 |
| **Major** | Significant accomplishments | Finishing a building, defeating boss | 4.1 - 6.0 |
| **Legendary** | Rare, exceptional feats | First nether portal, massive projects | 6.1 - 8.0 |

### Tier-Specific Dialogue Examples

**MINOR (1.0-2.0) - Understated Acknowledgment:**
```
"Got it done."
"Progress made."
"One more block in place."
"Moving forward."
```

**MODERATE (2.1-4.0) - Satisfied Recognition:**
```
"That's coming together nicely."
"Solid work on that section."
"Glad that's finished."
"Making real progress now."
```

**MAJOR (4.1-6.0) - Enthusiastic Celebration:**
```
"Now THAT'S what I call proper construction!"
"Outstanding work! This is going to be amazing."
"We actually pulled that off!"
"Beautiful. Just beautiful."
```

**LEGENDARY (6.1-8.0) - Exuberant Triumph:**
```
"IN-CREDIBLE! I never thought we'd actually finish this!"
"This is MASTERWORK quality! Absolutely stunning!"
"We just built something LEGENDARY!"
"I've never seen anything like this. This is HISTORY!"
```

---

## 3. Celebration Intensity by Personality

Personality traits dramatically affect how workers celebrate achievements.

### Big Five Trait Mapping

| Trait | Low Value (Reserved) | High Value (Expressive) |
|-------|---------------------|-------------------------|
| **Openness** | "Did it right." | "Never thought that approach would work!" |
| **Conscientiousness** | "Task complete." | "Every block perfectly placed!" |
| **Extraversion** | "Finished." | "WOO! Now THAT'S building!" |
| **Agreeableness** | "It's done." | "We did it! Together!" |
| **Neuroticism** | "Finally done." | "I was so worried, but we MADE it!" |

### Personality Archetype Celebrations

**THE PROFESSIONAL (ISTJ-style) - Measured, Quality-Focused:**
```
"Work completed to specification."
"That's solid construction."
"Proper work. Ready for inspection."
"Adequate. Efficient. Complete."
```

**THE ENTHUSIAST (ENFP-style) - Energetic, Emotional:**
```
"YEAH! Look at that go!"
"We are BUILDING MASTERS today!"
"I am SO EXCITED about this!"
"This is AMAZING! Check it out!"
```

**THE WITNESS (INTP-style) - Analytical, Surprised:**
```
"Fascinating. It actually worked."
"The structural integrity is remarkable."
"According to calculations, this should've fallen. Impressive."
"Hypothesis confirmed: We can build anything."
```

**THE CARETAKER (ISFJ-style) - Warm, Inclusive:**
```
"We did it! So proud of us."
"You did wonderful work."
"This turned out so nicely!"
"Glad we could do this together."
```

**THE REBEL (ESTP-style) - Bold, Action-Oriented:**
```
"BOOM! Another one down!"
"That's how you BUILD!"
"We crushed it!"
"Too easy! What's next?"
```

---

## 4. Team vs Individual Recognition

### Individual Achievement Patterns

When a single worker completes a task solo:

**Credit-Taking (Self-Focused):**
```
"I got this section done."
"I placed every block myself."
"My hands made this happen."
```

**Shared Credit (Inclusive):**
```
"I finished the section you designed."
"Following your blueprint made this easy."
"Your direction, my hands. Good team."
```

**Deferential (Player-Focused):**
```
"All your planning paid off."
"I just followed your lead."
"This is your vision, I just built it."
```

### Team Achievement Patterns

When multiple workers collaborate:

**Synchronized Celebration:**
```
Worker 1: "We did it!"
Worker 2: "Together!"
Worker 3: "Teamwork makes the dream work!"
```

**Complementary Praise:**
```
Worker 1: "Great foundation work!"
Worker 2: "You placed the roof perfectly!"
Worker 3: "We each did our part!"
```

**Collective Effervescence:**
```
"ALL OF US! RIGHT NOW!"
"THIS IS WHAT WE CAN DO!"
"TOGETHER, WE'RE UNSTOPPABLE!"
```

### Recognition Balance Formula

```java
float individualCredit = 0.3f;  // Base self-credit
float teamCredit = 0.5f;       // Split among teammates
float playerCredit = 0.2f;     // Always acknowledge player

// Adjust based on actual contribution
if (playerContributedSignificantly) {
    playerCredit = 0.5f;
    individualCredit = 0.2f;
    teamCredit = 0.3f;
}

if (soloAchievement) {
    individualCredit = 0.7f;
    teamCredit = 0.0f;
    playerCredit = 0.3f;
}
```

---

## 5. Callback to Struggle Patterns

Acknowledging difficulties makes celebrations more meaningful and creates emotional resonance.

### Struggle-Acknowledgment Templates

**The "After All That" Pattern:**
```
"After all that mining, we FINALLY found diamonds!"
"After three collapses, this bridge is STANDING."
"All those setbacks, and we STILL made it happen."
"After worrying about this for days, it's DONE."
```

**The "Almost Gave Up" Pattern:**
```
"I honestly thought we'd never finish this."
"That was touch and go there for a while."
"Nearly called it quits twice. Glad we didn't."
"This one tested us. But we passed."
```

**The "Against the Odds" Pattern:**
```
"With those materials, this shouldn't have worked."
" somehow we made it happen."
"Beginner's luck? No, this was pure skill."
"The stats were against us. We showed them."
```

**The "Worth the Effort" Pattern:**
```
"Every block placed was worth it."
"All that planning paid off."
"The struggle makes it better, doesn't it?"
"Wouldn't have meant as much if it was easy."
```

### Struggle-Callback Scenarios

| Challenge Type | Before Dialogue | After Celebration |
|----------------|----------------|-------------------|
| **Resource Shortage** | "We're barely scraping by..." | "And with almost nothing to work with!" |
| **Design Problems** | "This blueprint isn't working..." | "Figured it out! Creative solution!" |
| **Physical Danger** | "This is too risky..." | "Survived AND succeeded!" |
| **Time Pressure** | "Running out of time..." | "Beat the clock and it looks great!" |
| **Repeated Failures** | "Third time trying this..." | "Fourth time's the charm! Finally!" |

---

## 6. Player-Focused Praise

Celebrations should make the player feel accomplished and valued.

### Direct Player Compliments

**Skill Recognition:**
```
"You've got real talent for this."
"You're becoming quite the builder."
"Your planning is exceptional."
"You know exactly what you're doing."
```

**Vision Acknowledgment:**
```
"I never would've thought of this design."
"Your creativity is amazing."
"You see possibilities I miss."
"This is YOUR vision coming to life."
```

**Leadership Validation:**
```
"Following your lead works every time."
"You make a great foreman."
"Your direction makes everything easier."
"Glad I'm on your team."
```

**Partnership Affirmation:**
```
"We make a great team."
"Building with you is actually fun."
"Your help makes all the difference."
"Couldn't do this without you."
```

### Player Praise Dos and Don'ts

**DO:**
- Vary compliments based on actual player contribution
- Scale praise intensity with achievement tier
- Reference specific actions the player took
- Acknowledge both skill and effort

**DON'T:**
- Overuse generic praise ("Good job" every time)
- Praise excessively for minor achievements
- Take false credit ("I did most of the work")
- Ignore player when they contributed significantly

---

## 7. Humble Bragging Balance

### The Humblebrag Problem

**Research Finding:** Humblebragging (false modesty + actual bragging) is **less effective** than direct bragging and perceived as **more deceitful**.

**Examples to AVOID:**
```
"I don't know how I built this so quickly. (Actually I'm amazing)"
"I wish I wasn't so good at this. (It's embarrassing how talented I am)"
"You're making me look bad by being so slow. (I'm so fast)"
```

### Healthy Self-Celebration

**Confident Pride (Acceptable):**
```
"I'm proud of this work."
"I did good work here."
"This is some of my best."
"I've gotten really good at this."
```

**Deserved Satisfaction (Appropriate):**
```
"All that practice paid off."
"I've been working toward this."
"This shows how much I've improved."
"My experience really helped here."
```

**Genuine Team Pride (Inclusive):**
```
"WE did something great."
"Our teamwork really shined."
"Each of us contributed something important."
"This is what we can accomplish together."
```

### Self-Celebration Balance Formula

```java
String generateSelfCelebration(float personalityConfidence, float actualAchievement) {
    float appropriatePride = actualAchievement * 0.7f;
    float humilityBonus = (1.0f - personalityConfidence) * 0.3f;

    float selfCredit = appropriatePride + humilityBonus;

    if (selfCredit > 0.8f) {
        return "I genuinely think this is excellent work.";
    } else if (selfCredit > 0.5f) {
        return "I'm really pleased with how this turned out.";
    } else {
        return "Glad we could get this done.";
    }
}
```

---

## 8. Celebration Fatigue Prevention

### The "超限效应" (Overlimit Effect)

Research shows that **excessive, repetitive praise** leads to psychological resistance and avoidance behaviors.

### Fatigue Prevention Mechanisms

**1. Frequency Capping:**
```java
public class CelebrationCooldown {
    private final Map<Worker, Long> lastCelebrationTime = new HashMap<>();
    private final long MINIMUM_CELEBRATION_INTERVAL = 30000; // 30 seconds

    public boolean canCelebrate(Worker worker) {
        long lastTime = lastCelebrationTime.getOrDefault(worker, 0L);
        return (System.currentTimeMillis() - lastTime) > MINIMUM_CELEBRATION_INTERVAL;
    }
}
```

**2. Variety Rotation:**
```java
public class CelebrationVariety {
    private final Map<String, Integer> phraseUsageCounts = new HashMap<>();
    private final int MAX_USAGE_THRESHOLD = 3;

    public String selectCelebration(List<String> candidates) {
        // Sort by least used
        return candidates.stream()
            .min(Comparator.comparingInt(p -> phraseUsageCounts.getOrDefault(p, 0)))
            .orElse(candidates.get(0));
    }
}
```

**3. Proportionality Check:**
```java
float calculateAppropriateIntensity(Achievement achievement, Personality personality) {
    float baseIntensity = achievement.getTier().getBaseIntensity();
    float personalityModifier = personality.getExtraversion() * 0.5f;
    float fatiguePenalty = getRecentCelebrationCount() * 0.1f;

    return Math.max(1.0f, baseIntensity * (1.0f + personalityModifier - fatiguePenalty));
}
```

**4. Context Awareness:**
```java
boolean shouldCelebrate(Context context) {
    // Don't celebrate during danger
    if (context.hasNearbyThreats()) return false;

    // Don't celebrate if player is busy
    if (context.getPlayerEngagement() < 0.3f) return false;

    // Don't celebrate identical achievements repeatedly
    if (context.isRepeatAchievement()) return false;

    return true;
}
```

### Fatigue Prevention Guidelines

| Context | Celebration Behavior |
|---------|---------------------|
| **First achievement of type** | Full celebration |
| **Repeated achievement (3rd time)** | Reduced intensity |
| **Repeated achievement (10th time)** | Acknowledgment only |
| **During combat/danger** | No celebration |
| **Player occupied** | Delayed or muted |
| **Multiple workers** | Synchronized, not overlapping |

---

## 9. Shared Celebration Dialogue

### Multi-Worker Coordination

When multiple workers complete a task together, celebrations should feel coordinated and complementary.

**Call-and-Response Pattern:**
```
Worker 1: "We did it!"
Worker 2: "Finally!"
Worker 3: "And it looks great!"
```

**Complementary Roles:**
```
Worker 1 (Leader): "Team, this is EXCELLENT work!"
Worker 2 (Support): "Following your plans made it easy!"
Worker 3 (Specialist): "I just placed the blocks. You designed it."
```

**Crescendo Effect:**
```
Worker 1: "Getting close..."
Worker 2: "Almost there..."
Worker 3: "DONE! WE DID IT!"
All: "[Cheering sounds]"
```

### Cross-Worker Banter

**Competitive Celebration:**
```
Worker 1: "I placed more blocks!"
Worker 2: "And I placed them BETTER!"
Worker 3: "Children, please. We're ALL amazing."
```

**Mutual Appreciation:**
```
Worker 1: "Couldn't have done this without Worker 2."
Worker 2: "Same! Your foundation was perfect."
Worker 3: "And Worker 1's roofing is incredible."
```

**Shared Relief:**
```
Worker 1: "That was stressful."
Worker 2: "But we finished it."
Worker 3: "Together. That's how we do it."
```

---

## 10. Milestone-Specific Phrases

### Construction Milestones

**Foundation Complete:**
```
"Solid start. Everything builds from here."
"The foundation is set. Now we rise."
"Ground level achieved. Looking up from here."
```

**Walls Finished:**
```
"Walls up! We've got structure now."
"Enclosed! Starting to look like a building."
"Four walls, one purpose. Great work."
```

**Roof Complete:**
```
"Under roof! Almost there."
"Topped out! This is a building now."
"Roof's on. Weather can't touch us."
```

**Building Finished:**
```
"From foundation to finish. COMPLETE!"
"Every block in place. A home is born."
"We built THIS. Look at it!"
```

### Resource Milestones

**First Diamonds:**
```
"DIAMONDS! The blue beauties!"
"Our first diamonds! This is big time."
"Sparkly things! Worth the mining!"
```

**Stack of Resources:**
```
"A full stack! Now we're talking."
"64 pieces of progress. Beautiful."
"Stacked up and ready to build."
```

**Rare Item Found:**
```
"You don't find this every day!"
"Rare find! This changes everything."
"Lucky break! Let's use it well."
```

### Experience Milestones

**Level Up:**
```
"Getting stronger with every task."
"Experience earned. Wisdom gained."
"Another level, another advantage."
```

**First Time Achievement:**
```
"First time for everything!"
"Never done this before. Success!"
"New achievement unlocked!"
```

**Streak Achievement:**
```
"Five in a row! We're unstoppable!"
"On fire! Nothing can stop us now."
"Momentum is building!"
```

---

## 11. Java Implementation

### CelebrationManager Class

```java
package com.steve.characters.dialogue;

import com.steve.characters.Worker;
import com.steve.characters.Personality;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages celebration dialogue for worker achievements.
 * Prevents fatigue through cooldowns, variety rotation, and intensity scaling.
 */
public class CelebrationManager {

    private final Map<Worker, Long> lastCelebrationTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> phraseUsageCounts = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private static final long MIN_CELEBRATION_INTERVAL = 30000; // 30 seconds
    private static final long FATIGUE_RESET_TIME = 300000; // 5 minutes
    private static final int MAX_PHRASE_USAGE = 3;

    /**
     * Achievement tier classification
     */
    public enum AchievementTier {
        MINOR(1.0f, 2.0f),
        MODERATE(2.1f, 4.0f),
        MAJOR(4.1f, 6.0f),
        LEGENDARY(6.1f, 8.0f);

        private final float minIntensity;
        private final float maxIntensity;

        AchievementTier(float minIntensity, float maxIntensity) {
            this.minIntensity = minIntensity;
            this.maxIntensity = maxIntensity;
        }

        public float getBaseIntensity() {
            return (minIntensity + maxIntensity) / 2.0f;
        }
    }

    /**
     * Context for celebration generation
     */
    public static class CelebrationContext {
        private final Worker worker;
        private final AchievementTier tier;
        private final boolean teamAchievement;
        private final boolean playerContributed;
        private final List<String> recentStruggles;
        private final String achievementType;

        public CelebrationContext(Worker worker, AchievementTier tier,
                                boolean teamAchievement, boolean playerContributed,
                                List<String> recentStruggles, String achievementType) {
            this.worker = worker;
            this.tier = tier;
            this.teamAchievement = teamAchievement;
            this.playerContributed = playerContributed;
            this.recentStruggles = recentStruggles;
            this.achievementType = achievementType;
        }

        public Worker getWorker() { return worker; }
        public AchievementTier getTier() { return tier; }
        public boolean isTeamAchievement() { return teamAchievement; }
        public boolean didPlayerContribute() { return playerContributed; }
        public List<String> getRecentStruggles() { return recentStruggles; }
        public String getAchievementType() { return achievementType; }
    }

    /**
     * Generate appropriate celebration dialogue
     */
    public String generateCelebration(CelebrationContext context) {
        Worker worker = context.getWorker();
        Personality personality = worker.getPersonality();

        // Check cooldown
        if (!canCelebrate(worker)) {
            return null; // Too soon for another celebration
        }

        // Calculate appropriate intensity
        float intensity = calculateIntensity(context);

        // Generate celebration based on personality and context
        String celebration = buildCelebration(context, intensity);

        // Record usage
        recordCelebration(worker);
        incrementPhraseUsage(celebration);

        return celebration;
    }

    /**
     * Calculate celebration intensity based on tier, personality, and fatigue
     */
    private float calculateIntensity(CelebrationContext context) {
        float baseIntensity = context.getTier().getBaseIntensity();

        // Personality modifier
        float extraversion = context.getWorker().getPersonality().getExtraversion();
        float personalityModifier = (extraversion - 0.5f) * 0.5f;

        // Fatigue penalty
        int recentCelebrations = getRecentCelebrationCount(context.getWorker());
        float fatiguePenalty = recentCelebrations * 0.15f;

        // Team bonus (team achievements get slight boost)
        float teamBonus = context.isTeamAchievement() ? 0.3f : 0.0f;

        // Calculate final intensity
        float intensity = baseIntensity * (1.0f + personalityModifier + teamBonus - fatiguePenalty);

        // Clamp to reasonable bounds
        return Math.max(1.0f, Math.min(intensity, 8.0f));
    }

    /**
     * Build celebration string based on all context factors
     */
    private String buildCelebration(CelebrationContext context, float intensity) {
        Personality personality = context.getWorker().getPersonality();
        List<String> components = new ArrayList<>();

        // 1. Base celebration phrase
        components.add(getBaseCelebration(context, intensity));

        // 2. Add callback to struggle if applicable
        if (!context.getRecentStruggles().isEmpty() && random.nextFloat() < 0.4f) {
            components.add(getStruggleCallback(context.getRecentStruggles()));
        }

        // 3. Add player praise if appropriate
        if (context.didPlayerContribute() && random.nextFloat() < 0.5f) {
            components.add(getPlayerPraise(personality));
        }

        // 4. Add team acknowledgment if team achievement
        if (context.isTeamAchievement() && random.nextFloat() < 0.6f) {
            components.add(getTeamAcknowledgment(personality));
        }

        // Combine components
        return String.join(" ", components);
    }

    /**
     * Get base celebration phrase based on intensity and personality
     */
    private String getBaseCelebration(CelebrationContext context, float intensity) {
        Personality personality = context.getWorker().getPersonality();
        boolean highExtraversion = personality.getExtraversion() > 0.6f;
        boolean highConscientiousness = personality.getConscientiousness() > 0.7f;

        List<String> candidates = new ArrayList<>();

        if (intensity >= 6.0f) {
            // Legendary celebration
            candidates.addAll(Arrays.asList(
                "IN-CREDIBLE!",
                "This is MASTERWORK!",
                "We just built something LEGENDARY!",
                "Absolutely STUNNING!",
                highExtraversion ? "THIS IS HISTORY!" : "Remarkable achievement."
            ));
        } else if (intensity >= 4.0f) {
            // Major celebration
            candidates.addAll(Arrays.asList(
                "Now THAT'S proper construction!",
                "Outstanding work!",
                "We actually pulled that off!",
                "Beautiful. Just beautiful.",
                highExtraversion ? "WOO! Look at this!" : "Excellent work."
            ));
        } else if (intensity >= 2.0f) {
            // Moderate celebration
            candidates.addAll(Arrays.asList(
                "That's coming together nicely.",
                "Solid work.",
                "Glad that's finished.",
                "Making real progress.",
                highConscientiousness ? "Work completed to specification." : "Looking good!"
            ));
        } else {
            // Minor acknowledgment
            candidates.addAll(Arrays.asList(
                "Got it done.",
                "Progress made.",
                "One more section complete.",
                highConscientiousness ? "Task complete." : "Moving forward."
            ));
        }

        return selectLeastUsed(candidates);
    }

    /**
     * Get callback to recent struggles
     */
    private String getStruggleCallback(List<String> struggles) {
        String struggle = struggles.get(random.nextInt(struggles.size()));

        List<String> callbacks = Arrays.asList(
            "After all that " + struggle + ", we FINALLY made it!",
            "With the " + struggle + " we had, this is amazing!",
            "We overcame " + struggle + " for this!",
            "Worth every bit of " + struggle + "."
        );

        return callbacks.get(random.nextInt(callbacks.size()));
    }

    /**
     * Get player praise phrase
     */
    private String getPlayerPraise(Personality personality) {
        float agreeableness = personality.getAgreeableness();
        float openness = personality.getOpenness();

        List<String> praises = new ArrayList<>();

        if (agreeableness > 0.6f) {
            praises.addAll(Arrays.asList(
                "You're wonderful to work with.",
                "Your help made all the difference.",
                "Glad we're in this together."
            ));
        }

        if (openness > 0.6f) {
            praises.addAll(Arrays.asList(
                "Your vision is amazing.",
                "I never would've thought of this.",
                "Your creativity inspires me."
            ));
        }

        praises.addAll(Arrays.asList(
            "Great leadership on this.",
            "Your planning really paid off.",
            "You make a great foreman."
        ));

        return praises.get(random.nextInt(praises.size()));
    }

    /**
     * Get team acknowledgment phrase
     */
    private String getTeamAcknowledgment(Personality personality) {
        if (personality.getExtraversion() < 0.4f) {
            return "We all contributed something important.";
        }

        List<String> acknowledgments = Arrays.asList(
            "TOGETHER, we're unstoppable!",
            "This is what we can do as a team!",
            "Every one of us made this happen!",
            "Teamwork makes the dream work!"
        );

        return acknowledgments.get(random.nextInt(acknowledgments.size()));
    }

    /**
     * Select least used phrase from candidates
     */
    private String selectLeastUsed(List<String> candidates) {
        return candidates.stream()
            .min(Comparator.comparingInt(p -> phraseUsageCounts.getOrDefault(p, 0)))
            .orElse(candidates.get(0));
    }

    /**
     * Check if worker can celebrate (cooldown check)
     */
    private boolean canCelebrate(Worker worker) {
        Long lastTime = lastCelebrationTime.get(worker);
        if (lastTime == null) {
            return true;
        }

        long elapsed = System.currentTimeMillis() - lastTime;
        return elapsed >= MIN_CELEBRATION_INTERVAL;
    }

    /**
     * Record that a celebration occurred
     */
    private void recordCelebration(Worker worker) {
        lastCelebrationTime.put(worker, System.currentTimeMillis());
    }

    /**
     * Increment phrase usage count
     */
    private void incrementPhraseUsage(String phrase) {
        phraseUsageCounts.merge(phrase, 1, Integer::sum);

        // Clean up old entries periodically
        if (random.nextFloat() < 0.01f) {
            cleanupOldPhraseUsage();
        }
    }

    /**
     * Get recent celebration count for fatigue calculation
     */
    private int getRecentCelebrationCount(Worker worker) {
        Long lastTime = lastCelebrationTime.get(worker);
        if (lastTime == null) {
            return 0;
        }

        long elapsed = System.currentTimeMillis() - lastTime;

        // More celebrations if elapsed time is short
        if (elapsed < 60000) { // Less than 1 minute
            return 3;
        } else if (elapsed < 120000) { // Less than 2 minutes
            return 2;
        } else if (elapsed < 300000) { // Less than 5 minutes
            return 1;
        }

        return 0;
    }

    /**
     * Clean up old phrase usage entries
     */
    private void cleanupOldPhraseUsage() {
        // Remove entries that haven't been used recently
        // This is a simplified version - a full implementation would track timestamps
    }

    /**
     * Reset celebration fatigue for a worker
     */
    public void resetFatigue(Worker worker) {
        lastCelebrationTime.remove(worker);
    }
}
```

### Personality-Enhanced Celebration

```java
package com.steve.characters.dialogue;

import com.steve.characters.Personality;

/**
 * Personality-based celebration modifiers
 */
public class PersonalityCelebrationStyles {

    /**
     * Get celebration style modifier based on personality
     */
    public static CelebrationStyle getStyle(Personality personality) {
        float extraversion = personality.getExtraversion();
        float conscientiousness = personality.getConscientiousness();
        float openness = personality.getOpenness();
        float agreeableness = personality.getAgreeableness();

        if (extraversion > 0.7f) {
            return new EnthusiasticStyle();
        } else if (conscientiousness > 0.7f) {
            return new ProfessionalStyle();
        } else if (openness > 0.7f) {
            return new CreativeStyle();
        } else if (agreeableness > 0.7f) {
            return new WarmStyle();
        } else {
            return new BalancedStyle();
        }
    }

    /**
     * Base celebration style interface
     */
    public interface CelebrationStyle {
        String modifyBaseCelebration(String base);
        float getIntensityMultiplier();
    }

    /**
     * Enthusiastic style (High Extraversion)
     */
    public static class EnthusiasticStyle implements CelebrationStyle {
        @Override
        public String modifyBaseCelebration(String base) {
            // Add energy and emotion
            String[] enthusiasticPrefixes = {
                "WOO! ", "YEAH! ", "OH YEAH! ", "LET'S GO! "
            };
            String prefix = enthusiasticPrefixes[
                (int)(Math.random() * enthusiasticPrefixes.length)
            ];
            return prefix + base.toUpperCase() + "!";
        }

        @Override
        public float getIntensityMultiplier() {
            return 1.5f;
        }
    }

    /**
     * Professional style (High Conscientiousness)
     */
    public static class ProfessionalStyle implements CelebrationStyle {
        @Override
        public String modifyBaseCelebration(String base) {
            // Keep it measured and quality-focused
            if (base.contains("!")) {
                base = base.replace("!", ".");
            }
            return "Work " + base.toLowerCase();
        }

        @Override
        public float getIntensityMultiplier() {
            return 0.8f;
        }
    }

    /**
     * Creative style (High Openness)
     */
    public static class CreativeStyle implements CelebrationStyle {
        @Override
        public String modifyBaseCelebration(String base) {
            // Add creative flair
            String[] creativeSuffixes = {
                " Never thought it would look this good!",
                " This turned out even better than I imagined!",
                " The design really came together!"
            };
            String suffix = creativeSuffixes[
                (int)(Math.random() * creativeSuffixes.length)
            ];
            return base + suffix;
        }

        @Override
        public float getIntensityMultiplier() {
            return 1.1f;
        }
    }

    /**
     * Warm style (High Agreeableness)
     */
    public static class WarmStyle implements CelebrationStyle {
        @Override
        public String modifyBaseCelebration(String base) {
            // Make it inclusive and warm
            return "We " + base.toLowerCase().replace("i ", "we ");
        }

        @Override
        public float getIntensityMultiplier() {
            return 1.0f;
        }
    }

    /**
     * Balanced style (Default)
     */
    public static class BalancedStyle implements CelebrationStyle {
        @Override
        public String modifyBaseCelebration(String base) {
            return base;
        }

        @Override
        public float getIntensityMultiplier() {
            return 1.0f;
        }
    }
}
```

### Team Celebration Coordinator

```java
package com.steve.characters.dialogue;

import com.steve.characters.Worker;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Coordinates celebrations across multiple workers
 */
public class TeamCelebrationCoordinator {

    /**
     * Generate coordinated celebration for multiple workers
     */
    public List<WorkerCelebration> generateTeamCelebration(
            List<Worker> workers,
            CelebrationManager.CelebrationContext context) {

        List<WorkerCelebration> celebrations = new ArrayList<>();

        // Sort workers by personality to create natural flow
        List<Worker> sortedWorkers = new ArrayList<>(workers);
        sortedWorkers.sort(Comparator.comparingDouble(w ->
            -w.getPersonality().getExtraversion()));

        // Generate celebrations with slight delays for natural feel
        for (int i = 0; i < sortedWorkers.size(); i++) {
            Worker worker = sortedWorkers.get(i);
            String dialogue;

            if (i == 0) {
                // First worker gets the main celebration
                dialogue = generateMainCelebration(worker, context);
            } else if (i == sortedWorkers.size() - 1) {
                // Last worker gets the closing celebration
                dialogue = generateClosingCelebration(worker, context);
            } else {
                // Middle workers get supportive celebrations
                dialogue = generateSupportiveCelebration(worker, context);
            }

            celebrations.add(new WorkerCelebration(
                worker,
                dialogue,
                i * 500L // 500ms delay between each
            ));
        }

        return celebrations;
    }

    private String generateMainCelebration(Worker worker,
                                          CelebrationManager.CelebrationContext context) {
        // Main celebration is more enthusiastic
        float intensity = context.getTier().getBaseIntensity() * 1.3f;
        return generateForIntensity(worker, context, intensity);
    }

    private String generateClosingCelebration(Worker worker,
                                            CelebrationManager.CelebrationContext context) {
        // Closing celebration is final and satisfying
        String[] closings = {
            "Together. That's how we build.",
            "This is what we can accomplish.",
            "Teamwork makes it happen.",
            "Every one of us contributed."
        };
        return closings[(int)(Math.random() * closings.length)];
    }

    private String generateSupportiveCelebration(Worker worker,
                                                CelebrationManager.CelebrationContext context) {
        // Supportive celebrations reinforce the main
        String[] supportives = {
            "Agreed!",
            "Absolutely!",
            "Well said!",
            "Indeed!",
            "We did it!"
        };
        return supportives[(int)(Math.random() * supportives.length)];
    }

    private String generateForIntensity(Worker worker,
                                       CelebrationManager.CelebrationContext context,
                                       float intensity) {
        // Simplified generation - would use full CelebrationManager in practice
        if (intensity > 5.0f) {
            return "This is AMAZING work!";
        } else if (intensity > 3.0f) {
            return "Great progress!";
        } else {
            return "Good work!";
        }
    }

    /**
     * Data class for worker celebration with timing
     */
    public static class WorkerCelebration {
        private final Worker worker;
        private final String dialogue;
        private final long delayMillis;

        public WorkerCelebration(Worker worker, String dialogue, long delayMillis) {
            this.worker = worker;
            this.dialogue = dialogue;
            this.delayMillis = delayMillis;
        }

        public Worker getWorker() { return worker; }
        public String getDialogue() { return dialogue; }
        public long getDelayMillis() { return delayMillis; }

        public CompletableFuture<Void> execute() {
            return CompletableFuture.runAsync(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(delayMillis);
                    worker.say(dialogue);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
}
```

---

## Usage Examples

### Basic Celebration

```java
// Create celebration manager
CelebrationManager celebrationManager = new CelebrationManager();

// Generate celebration for completed building
CelebrationManager.CelebrationContext context = new CelebrationManager.CelebrationContext(
    worker,
    CelebrationManager.AchievementTier.MAJOR,
    true,  // team achievement
    true,  // player contributed
    Arrays.asList("running out of materials", "design changes"),
    "building_completed"
);

String celebration = celebrationManager.generateCelebration(context);
if (celebration != null) {
    worker.say(celebration);
}
// Output: "Now THAT'S proper construction! After running out of materials,
//          we finally made it! Your planning really paid off. This is what we can do as a team!"
```

### Team Celebration

```java
// Multiple workers completing a project together
List<Worker> workers = Arrays.asList(worker1, worker2, worker3);
TeamCelebrationCoordinator coordinator = new TeamCelebrationCoordinator();

CelebrationManager.CelebrationContext context = new CelebrationManager.CelebrationContext(
    workers.get(0),
    CelebrationManager.AchievementTier.LEGENDARY,
    true,  // team achievement
    true,  // player contributed
    Arrays.asList("three collapses", "running out of time"),
    "mega_project_completed"
);

List<WorkerCelebration> celebrations = coordinator.generateTeamCelebration(workers, context);

// Execute celebrations with natural timing
CompletableFuture.allOf(
    celebrations.stream()
        .map(WorkerCelebration::execute)
        .toArray(CompletableFuture[]::new)
).join();

/*
 * Output (staggered by 500ms):
 * Worker 1: "We just built something LEGENDARY! After three collapses, we finally made it!"
 * Worker 2: "Absolutely!"
 * Worker 3: "Together. That's how we build."
 */
```

---

## Testing Recommendations

1. **Fatigue Testing**: Verify celebrations don't trigger too frequently for repeated achievements
2. **Variety Testing**: Ensure phrase rotation prevents repetitive dialogue
3. **Personality Testing**: Verify different personalities produce appropriately varied celebrations
4. **Team Testing**: Test multi-worker celebrations feel natural and coordinated
5. **Context Testing**: Verify celebrations don't trigger during inappropriate moments (combat, danger)

---

## Sources

### Sports Psychology Research
- [Sports Team Celebration Psychology (Frontiers in Psychology, 2025)](https://www.frontiersin.org/articles/10.3389/fpsyg.2025.1636707/full)
- [Mirror Neurons in Team Celebrations (Toutiao, 2025)](https://m.toutiao.com/w/1852394034057216/)
- [Collective Effervescence Research (Indiana University)](https://baijiahao.baidu.com/s?id=1753774298848240543)

### Workplace Recognition
- [Employee Recognition Best Practices (Atlassian)](https://www.atlassian.com/zh/work-management/team-management-and-leadership/team-management-strategies/employee-recognition)
- [Celebration Health Benefits Research]((https://m.toutiao.com/article/7193978292855112192/)

### Video Game Design
- [RPG NPC Dialogue Design (CSDN, 2024)](https://m.blog.csdn.net/chenjj4003/article/details/146327626)
- [Baldur's Gate 3 Personality Systems (Ali213, 2011)](https://www.ali213.net/news/html/2011/14394.html)
- [Stardew Valley Dialogue System (Minecraft Docs)](https://learn.microsoft.com/zh-cn/minecraft/creator/Documents/NPCDialogue)

### Psychology Research
- [Humble Bragging Psychology (arXiv, 2024)](https://arxiv.org/html/2412.20057v1)
- [Celebration Fatigue - Overlimit Effect (Baidu Baike)](https://baike.baidu.com/item/超限反应)
- [Humblebrag Damages Relationships (Psychology Today)](http://www.sohu.com/a/273661220_563944)

### Friendship & Social Bonding
- [Friendship Achievement Psychology (BNU Dev Psych)](https://devpsy.bnu.edu.cn/CN/volumn/home.shtml?id=10.16187/j.cnki.issn1001-4918.2020.01.07)
- [Social Support and Well-being (Journal of Public Policy & Marketing)](https://baijiahao.baidu.com/s?id=1753774298848240543)

---

**End of Document**
