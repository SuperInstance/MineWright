# Proactive Dialogue System for the Foreman

**Project:** MineWright - Minecraft Autonomous Agents
**Component:** Proactive Dialogue System
**Version:** 1.0
**Date:** 2026-02-26

---

## Executive Summary

The Proactive Dialogue System enables the Foreman to make contextual, appropriate comments without being prompted - a key feature for making him feel alive and present as a companion. This system carefully balances engagement with non-intrusiveness, using intelligent triggers, cooldowns, and player engagement detection to avoid becoming annoying.

**Key Design Principles:**
1. **Context-Aware:** Comments match the current situation, activity, and environment
2. **Non-Intrusive:** Respects player attention and avoids interrupting critical moments
3. **Adaptive:** Adjusts frequency based on player engagement and rapport level
4. **Personality-Driven:** All comments reflect the Foreman's persona and relationship with the player

---

## Table of Contents

1. [System Architecture](#system-architecture)
2. [Trigger System Design](#trigger-system-design)
3. [Comment Selection Algorithm](#comment-selection-algorithm)
4. [Cooldown & Anti-Spam](#cooldown--anti-spam)
5. [Code Implementation](#code-implementation)
6. [Integration Points](#integration-points)
7. [Configuration](#configuration)
8. [Example Comments](#example-comments)

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                  Proactive Dialogue System                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │            ProactiveDialogueManager                    │    │
│  │  - Main coordinator                                     │    │
│  │  - Tick-based evaluation                               │    │
│  │  - Integrates all components                           │    │
│  └────────────────────────────────────────────────────────┘    │
│                           │                                      │
│         ┌─────────────────┼─────────────────┐                  │
│         │                 │                 │                  │
│  ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐          │
│  │  Trigger    │  │   Comment   │  │ Engagement  │          │
│  │  System     │  │   Library   │  │  Tracker    │          │
│  └─────────────┘  └─────────────┘  └─────────────┘          │
│         │                 │                 │                  │
│         └─────────────────┼─────────────────┘                  │
│                           │                                      │
│                  ┌────────▼────────┐                            │
│                  │  Decision       │                            │
│                  │  Engine         │                            │
│                  │  - Check triggers│                            │
│                  │  - Select comment│                           │
│                  │  - Apply filters │                           │
│                  └────────┬────────┘                            │
│                           │                                      │
│                  ┌────────▼────────┐                            │
│                  │  Output         │                            │
│                  │  - Chat message │                            │
│                  │  - Voice TTS    │                            │
│                  │  - GUI overlay  │                            │
│                  └─────────────────┘                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Key Methods |
|-----------|----------------|-------------|
| **ProactiveDialogueManager** | Main coordinator, tick loop | `tick()`, `evaluateTriggers()` |
| **DialogueTrigger** | Specific trigger condition | `shouldTrigger()`, `getPriority()` |
| **CommentLibrary** | Stores and retrieves comments | `getComment()`, `addComment()` |
| **EngagementTracker** | Monitors player engagement | `isPlayerEngaged()`, `recordResponse()` |
| **CooldownManager** | Prevents spam | `isOnCooldown()`, `resetCooldown()` |

---

## Trigger System Design

### Trigger Categories

Triggers are organized into five categories based on priority and frequency:

#### 1. **Idle Time Triggers** (Priority: LOW)
Triggered when the player is inactive for a period of time.

| Trigger | Condition | Cooldown | Example |
|---------|-----------|----------|---------|
| `IDLE_SHORT` | No action for 45 seconds | 3 minutes | "Nice view from here..." |
| `IDLE_MEDIUM` | No action for 2 minutes | 5 minutes | "You've been staring at that wall for a while." |
| `IDLE_LONG` | No action for 5 minutes | 10 minutes | "Still deciding what to build?" |

#### 2. **Context-Based Triggers** (Priority: MEDIUM)
Triggered by changes in the game environment.

| Trigger | Condition | Cooldown | Example |
|---------|-----------|----------|---------|
| `BIOME_ENTER` | Enter new biome | 2 minutes | "Ah, the desert. Hot. Dry. Full of things trying to kill us." |
| `TIME_CHANGE` | Dawn/Dusk/Midnight | 30 minutes | "Sun's coming up. Perfect time to get some work done." |
| `WEATHER_CHANGE` | Rain/storm starts | 5 minutes | "Rain. Great for crops, terrible for morale." |
| `STRUCTURE_FIND` | Find village/temple | 10 minutes | "Civilization! Well, sort of." |

#### 3. **Activity-Based Triggers** (Priority: MEDIUM)
Triggered by sustained player activities.

| Trigger | Condition | Cooldown | Example |
|---------|-----------|----------|---------|
| `MINING_SUSTAINED` | Mining for 5+ minutes | 8 minutes | "You've been at this a while. Found anything good?" |
| `BUILDING_SUSTAINED` | Building for 10+ minutes | 10 minutes | "This is coming together. Slowly. Painfully slowly." |
| `EXPLORING_SUSTAINED` | Traveling for 15+ minutes | 12 minutes | "We've covered a lot of ground. Adventure awaits!" |
| `COMBAT_ACTIVE` | In combat for 2+ minutes | 5 minutes | "That was intense! You okay?" |

#### 4. **Event-Based Triggers** (Priority: HIGH)
Triggered by significant game events.

| Trigger | Condition | Cooldown | Example |
|---------|-----------|----------|---------|
| `TASK_COMPLETE` | Action executor finishes task | 1 minute | "Got it done. Another job well finished." |
| `TASK_FAIL` | Action fails repeatedly | 2 minutes | "Having trouble? Want me to try a different approach?" |
| `LEVEL_UP` | Player levels up (if modded) | 5 minutes | "Feeling stronger already!" |
| `ACHIEVEMENT` | Achievement unlocked | 5 minutes | "Now THAT was impressive!" |

#### 5. **Environmental Triggers** (Priority: VARIABLE)
Triggered by specific environmental conditions.

| Trigger | Condition | Cooldown | Example |
|---------|-----------|----------|---------|
| `MOB_NEARBY` | Hostile mob within 10 blocks | 30 seconds | "Heard something. Keep your eyes open." |
| `LOW_RESOURCES` | Low on essential items | 3 minutes | "We're running low on torches. Just saying." |
| `DANGER_NEAR` | Lava/creeper detected | 10 seconds | "Watch your step!" |
| `SAFE_SPOT` | Found well-lit enclosed area | 5 minutes | "This seems like a safe spot to rest." |

### Trigger Evaluation Flow

```
START TICK
    │
    ├─► Check: Is player engaged?
    │   └─► NO → Skip all triggers
    │   └─► YES → Continue
    │
    ├─► Check: Is any critical trigger active? (DANGER, COMBAT)
    │   └─► YES → Immediately evaluate, skip others if triggered
    │   └─► NO → Continue
    │
    ├─► Check: Is any high-priority trigger ready? (TASK_COMPLETE, ACHIEVEMENT)
    │   └─► Check cooldowns
    │   └─► Evaluate triggers
    │   └─► If triggered → Select comment, deliver, reset cooldowns
    │
    ├─► Check: Is any medium-priority trigger ready? (CONTEXT, ACTIVITY)
    │   └─► Check cooldowns
    │   └─► Check context (not in combat, not in critical task)
    │   └─► Evaluate triggers
    │   └─► If triggered → Select comment, deliver, reset cooldowns
    │
    └─► Check: Is any low-priority trigger ready? (IDLE)
        └─► Check cooldowns
        └─► Verify truly idle (no recent actions)
        └─► Small random chance (15%) to actually trigger
        └─► If triggered → Select comment, deliver, reset cooldowns
```

---

## Comment Selection Algorithm

### Selection Process

Comments are selected through a multi-stage filtering process:

```
1. TRIGGER MATCH
   └─► Get all comments for this trigger type
   └─► Filter by context (biome, time, activity)

2. RAPPORT FILTER
   └─► Remove comments requiring higher rapport
   └─► Adjust formality based on rapport level

3. PERSONALITY FILTER
   └─► Filter by humor trait
   └─► Filter by formality setting
   └─► Apply mood adjustments

4. MEMORY CHECK
   └─► Prefer comments referencing shared experiences
   └──► Avoid recently used phrases
   └─► Boost relevance of inside jokes

5. DIVERSITY CHECK
   └──► Avoid repeating same comment within session
   └──► Rotate through comment types

6. FINAL SELECTION
   └──► Weighted random from top candidates
   └──► Apply personality adjustments
   └──► Return final comment
```

### Weighting Factors

| Factor | Weight | Description |
|--------|--------|-------------|
| **Context Relevance** | 40% | How well matches current situation |
| **Rapport Appropriateness** | 25% | Suitable for current relationship level |
| **Personality Match** | 20% | Fits MineWright's current mood and traits |
| **Memory Connection** | 10% | References shared experiences |
| **Novelty** | 5% | Not recently used |

### Comment Types

| Type | Purpose | Frequency | Example |
|------|---------|-----------|---------|
| **OBSERVATIONAL** | Note something interesting | Common | "That's an interesting formation..." |
| **SUGGESTIVE** | Propose an idea | Medium | "We could expand this area..." |
| **REFLECTIVE** | Reference past events | Low | "Remember when we first came here?" |
| **HUMOROUS** | Light joke or pun | Low (based on humor trait) | "I've seen a lot of dirt, but this is a LOT of dirt." |
| **SUPPORTIVE** | Encourage player | Medium | "You're doing great!" |
| **INQUISITIVE** | Ask question | Low | "What are you thinking about building?" |

---

## Cooldown & Anti-Spam

### Cooldown System

Each comment type has independent cooldowns to prevent repetition:

```java
// Cooldown durations by priority
enum CooldownDuration {
    CRITICAL(10, TimeUnit.SECONDS),      // Danger warnings
    HIGH(1, TimeUnit.MINUTES),           // Events, achievements
    MEDIUM(3, TimeUnit.MINUTES),         // Context, activity
    LOW(5, TimeUnit.MINUTES),            // Idle comments
    VERY_LOW(10, TimeUnit.MINUTES);      // Rare reflections
}
```

### Anti-Annoyance Safeguards

#### 1. **Player Engagement Detection**

The system tracks whether the player is actually engaged:

```java
public class EngagementTracker {
    private int consecutiveIgnoredComments = 0;
    private Instant lastPlayerResponse;
    private int interactionCountThisSession;

    public boolean isPlayerEngaged() {
        // Player is engaged if:
        // - Responded to last comment (within 2 minutes)
        // - Not ignoring comments (> 3 in a row ignored)
        // - Active within last 30 seconds
        // - Interaction count > 0 this session

        if (consecutiveIgnoredComments >= 3) {
            return false; // Player seems uninterested
        }

        if (lastPlayerResponse != null) {
            long minutesSince = ChronoUnit.MINUTES.between(
                lastPlayerResponse, Instant.now()
            );
            if (minutesSince < 2) {
                return true; // Recently responsive
            }
        }

        return isPlayerActive();
    }

    public void recordPlayerResponse() {
        lastPlayerResponse = Instant.now();
        consecutiveIgnoredComments = 0;
        interactionCountThisSession++;
    }

    public void recordCommentIgnored() {
        consecutiveIgnoredComments++;
    }
}
```

#### 2. **Silence Preference Detection**

If a player consistently ignores comments, MineWright reduces frequency:

```java
public class AdaptiveFrequencyManager {
    private double currentFrequencyMultiplier = 1.0;
    private int ignoredCount;
    private int acknowledgedCount;

    public void onCommentDelivered(boolean acknowledged) {
        if (acknowledged) {
            acknowledgedCount++;
            // Slowly increase frequency if player likes it
            if (acknowledgedCount > 5 && ignoredCount == 0) {
                currentFrequencyMultiplier = Math.min(1.5, currentFrequencyMultiplier * 1.05);
            }
        } else {
            ignoredCount++;
            // Decrease frequency if ignored
            if (ignoredCount > 3) {
                currentFrequencyMultiplier = Math.max(0.2, currentFrequencyMultiplier * 0.7);
            }
        }
    }

    public boolean shouldSuppressComment(CommentType type) {
        // Apply frequency multiplier
        double adjustedChance = type.baseChance() * currentFrequencyMultiplier;

        // Always allow critical comments
        if (type.priority() == Priority.CRITICAL) {
            return false;
        }

        // Roll against adjusted chance
        return Math.random() > adjustedChance;
    }
}
```

#### 3. **Context-Aware Suppression**

Never speak during these situations:

```java
public class ContextAwareSuppression {
    public static boolean shouldSuppress(GameContext context) {
        // NEVER speak during:
        return context.isInCombat()
            || context.isPlayerFrustrated()  // Detected from behavior
            || context.isInCriticalTask()    // Boss fight, redstone work
            || context.isHealthCritical()    // Player < 3 hearts
            || context.isFirstTutorial()     // Let player learn basics
            || context.hasRecentDeath();     // Died within last minute
    }
}
```

#### 4. **Rapport-Based Frequency**

Higher rapport = more frequent comments (because player likes MineWright):

```java
public int getCommentFrequencyForRapport(int rapport) {
    if (rapport < 20) {
        return 5;   // Very reserved
    } else if (rapport < 40) {
        return 10;  // Cautious
    } else if (rapport < 60) {
        return 20;  // Friendly
    } else if (rapport < 80) {
        return 30;  // Talkative
    } else {
        return 40;  // Companion-level chatter
    }
}
```

---

## Code Implementation

### File Structure

```
src/main/java/com/minewright/ai/dialogue/
├── ProactiveDialogueManager.java   # Main coordinator
├── trigger/
│   ├── DialogueTrigger.java         # Base trigger interface
│   ├── IdleTrigger.java
│   ├── ContextTrigger.java
│   ├── ActivityTrigger.java
│   ├── EventTrigger.java
│   └── EnvironmentalTrigger.java
├── comment/
│   ├── CommentLibrary.java          # Comment storage
│   ├── CommentType.java             # Type enumeration
│   └── Comment.java                 # Comment data class
├── engagement/
│   ├── EngagementTracker.java       # Player engagement monitoring
│   ├── AdaptiveFrequencyManager.java
│   └── ContextAwareSuppression.java
└── cooldown/
    ├── CooldownManager.java         # Cooldown tracking
    └── TriggerCooldowns.java
```

### Core Classes

#### 1. ProactiveDialogueManager.java

```java
package com.minewright.dialogue;

import com.minewright.dialogue.comment.*;
import com.minewright.dialogue.cooldown.CooldownManager;
import com.minewright.dialogue.engagement.*;
import com.minewright.dialogue.trigger.*;
import com.minewright.entity.ForemanEntity;
import com.minewright.memory.CompanionMemory;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages proactive dialogue for the Foreman.
 *
 * <p>This system enables the Foreman to make contextual comments without being prompted,
 * making him feel more alive and present as a companion.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Context-aware trigger system</li>
 *   <li>Adaptive frequency based on player engagement</li>
 *   <li>Cooldown system to prevent spam</li>
 *   <li>Rapport-based comment selection</li>
 *   <li>Memory-aware conversations</li>
 * </ul>
 */
public class ProactiveDialogueManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProactiveDialogueManager.class);

    private final MineWrightEntity minewright;
    private final CompanionMemory memory;
    private final CommentLibrary commentLibrary;
    private final CooldownManager cooldownManager;
    private final EngagementTracker engagementTracker;
    private final AdaptiveFrequencyManager frequencyManager;
    private final List<DialogueTrigger> triggers;
    private final Random random;

    private int tickCounter = 0;
    private Instant lastCommentTime;
    private int commentsThisSession = 0;

    /**
     * Creates a new proactive dialogue manager.
     *
     * @param minewright The MineWright entity
     * @param memory The companion memory system
     */
    public ProactiveDialogueManager(MineWrightEntity minewright, CompanionMemory memory) {
        this.minewright = minewright;
        this.memory = memory;
        this.commentLibrary = new CommentLibrary();
        this.cooldownManager = new CooldownManager();
        this.engagementTracker = new EngagementTracker();
        this.frequencyManager = new AdaptiveFrequencyManager();
        this.triggers = new ArrayList<>();
        this.random = new Random();

        initializeTriggers();
    }

    /**
     * Main tick loop - called every game tick.
     */
    public void tick() {
        tickCounter++;

        // Only evaluate every 20 ticks (1 second) to save performance
        if (tickCounter % 20 != 0) {
            return;
        }

        // Check if we should even consider speaking
        if (!shouldConsiderSpeaking()) {
            return;
        }

        // Evaluate triggers
        Optional<Comment> potentialComment = evaluateTriggers();

        if (potentialComment.isPresent()) {
            Comment comment = potentialComment.get();

            // Apply final filters
            if (shouldDeliverComment(comment)) {
                deliverComment(comment);
            }
        }
    }

    /**
     * Checks if basic conditions are met to consider speaking.
     */
    private boolean shouldConsiderSpeaking() {
        Level level = minewright.level();

        // Never on client side
        if (level.isClientSide) {
            return false;
        }

        // Check player engagement
        if (!engagementTracker.isPlayerEngaged()) {
            LOGGER.debug("Skipping proactive dialogue - player not engaged");
            return false;
        }

        // Check time since last comment (minimum 30 seconds)
        if (lastCommentTime != null) {
            long secondsSince = ChronoUnit.SECONDS.between(lastCommentTime, Instant.now());
            if (secondsSince < 30) {
                return false;
            }
        }

        // Check adaptive frequency
        double currentFreq = frequencyManager.getCurrentFrequency();
        if (random.nextDouble() > currentFreq) {
            return false;
        }

        return true;
    }

    /**
     * Evaluates all triggers and returns the best candidate comment.
     */
    private Optional<Comment> evaluateTriggers() {
        // Sort triggers by priority (highest first)
        triggers.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

        for (DialogueTrigger trigger : triggers) {
            // Check cooldown
            if (cooldownManager.isOnCooldown(trigger.getTriggerType())) {
                continue;
            }

            // Check if trigger conditions are met
            if (trigger.shouldTrigger(minewright, memory)) {
                // Get candidate comments for this trigger
                List<Comment> candidates = commentLibrary.getCommentsForTrigger(
                    trigger.getTriggerType()
                );

                // Filter and select best comment
                Optional<Comment> selected = selectBestComment(candidates, trigger);

                if (selected.isPresent()) {
                    // Set cooldown
                    cooldownManager.setCooldown(trigger.getTriggerType(), trigger.getCooldown());

                    return selected;
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Selects the best comment from candidates using weighted selection.
     */
    private Optional<Comment> selectBestComment(List<Comment> candidates, DialogueTrigger trigger) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // Score each comment
        Map<Comment, Double> scores = new HashMap<>();

        for (Comment comment : candidates) {
            double score = calculateCommentScore(comment, trigger);
            scores.put(comment, score);
        }

        // Weighted random selection
        double totalScore = scores.values().stream().mapToDouble(Double::doubleValue).sum();
        double randomValue = random.nextDouble() * totalScore;

        double cumulativeScore = 0;
        for (Map.Entry<Comment, Double> entry : scores.entrySet()) {
            cumulativeScore += entry.getValue();
            if (randomValue <= cumulativeScore) {
                return Optional.of(entry.getKey());
            }
        }

        // Fallback to highest-scoring comment
        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey);
    }

    /**
     * Calculates a score for a comment based on multiple factors.
     */
    private double calculateCommentScore(Comment comment, DialogueTrigger trigger) {
        double score = 0.0;

        // Base score
        score += 10.0;

        // Context relevance (40% weight)
        score += calculateContextRelevance(comment) * 40.0;

        // Rapport appropriateness (25% weight)
        score += calculateRapportMatch(comment) * 25.0;

        // Personality match (20% weight)
        score += calculatePersonalityMatch(comment) * 20.0;

        // Memory connection (10% weight)
        score += calculateMemoryConnection(comment) * 10.0;

        // Novelty bonus (5% weight)
        score += calculateNoveltyScore(comment) * 5.0;

        return score;
    }

    private double calculateContextRelevance(Comment comment) {
        // Check if comment matches current biome, time, activity
        Level level = minewright.level();

        double relevance = 0.5; // Base relevance

        // Biome match
        if (comment.getRequiredBiome() != null) {
            if (level.getBiome(minewright.blockPosition()).toString()
                    .toLowerCase().contains(comment.getRequiredBiome().toLowerCase())) {
                relevance += 0.3;
            } else {
                relevance -= 0.5; // Penalty for mismatch
            }
        }

        // Time of day match
        if (comment.getRequiredTimeOfDay() != null) {
            long dayTime = level.getDayTime() % 24000;
            boolean matches = switch (comment.getRequiredTimeOfDay()) {
                case "dawn" -> dayTime >= 23000 || dayTime < 1000;
                case "day" -> dayTime >= 1000 && dayTime < 13000;
                case "dusk" -> dayTime >= 13000 && dayTime < 14000;
                case "night" -> dayTime >= 14000;
                default -> true;
            };
            if (matches) {
                relevance += 0.2;
            }
        }

        return Math.max(0, Math.min(1, relevance));
    }

    private double calculateRapportMatch(Comment comment) {
        int rapport = memory.getRapportLevel();
        int minRapport = comment.getMinRapport();

        if (rapport < minRapport) {
            return 0.0; // Not eligible
        }

        // Higher score for comments closer to current rapport
        int rapportDiff = Math.abs(rapport - minRapport);
        return 1.0 - (rapportDiff / 100.0);
    }

    private double calculatePersonalityMatch(Comment comment) {
        CompanionMemory.PersonalityProfile personality = memory.getPersonality();

        double match = 0.5;

        // Humor trait
        if (comment.getType() == CommentType.HUMOROUS) {
            int humorDiff = Math.abs(personality.humor - comment.getHumorRequirement());
            match += (1.0 - humorDiff / 100.0) * 0.5;
        }

        // Formality
        int formalityDiff = Math.abs(personality.formality - comment.getFormalityLevel());
        match += (1.0 - formalityDiff / 100.0) * 0.3;

        return Math.max(0, Math.min(1, match));
    }

    private double calculateMemoryConnection(Comment comment) {
        // Check if comment references shared experiences
        if (comment.getReferencesMemory()) {
            // Boost if we have relevant memories
            List<CompanionMemory.EpisodicMemory> relevant = memory.findRelevantMemories(
                comment.getTopic(), 3
            );
            return relevant.size() > 0 ? 1.0 : 0.3;
        }
        return 0.5; // Neutral for non-memory comments
    }

    private double calculateNoveltyScore(Comment comment) {
        // Check if recently used
        if (cooldownManager.isRecentlyUsed(comment.getId())) {
            return 0.2; // Penalty for repetition
        }
        return 1.0;
    }

    /**
     * Final check before delivering comment.
     */
    private boolean shouldDeliverComment(Comment comment) {
        // Check adaptive frequency
        if (frequencyManager.shouldSuppressComment(comment.getType())) {
            LOGGER.debug("Comment suppressed by adaptive frequency");
            return false;
        }

        // Context-aware suppression
        if (ContextAwareSuppression.shouldSuppress(getGameContext())) {
            LOGGER.debug("Comment suppressed by context");
            return false;
        }

        return true;
    }

    /**
     * Delivers the comment through the appropriate channel.
     */
    private void deliverComment(Comment comment) {
        String message = comment.getText();

        // Apply personality adjustments
        message = adjustForPersonality(message);

        // Send via chat
        minewright.sendChatMessage(message);

        // Record delivery
        lastCommentTime = Instant.now();
        commentsThisSession++;
        cooldownManager.markAsUsed(comment.getId());

        LOGGER.debug("Delivered proactive comment: {}", message);

        // Track for engagement (will be updated if player responds)
        engagementTracker.recordCommentDelivered();
    }

    /**
     * Adjusts comment text based on personality.
     */
    private String adjustForPersonality(String baseMessage) {
        CompanionMemory.PersonalityProfile personality = memory.getPersonality();

        // Adjust formality
        if (personality.formality > 70) {
            // More formal
            baseMessage = baseMessage.replace("!", ".")
                                   .replace("hey", "greetings")
                                   .replace("hi", "hello");
        } else if (personality.formality < 30) {
            // More casual
            baseMessage = baseMessage.replace("do not", "don't")
                                   .replace("cannot", "can't")
                                   .replace("I am", "I'm");
        }

        return baseMessage;
    }

    /**
     * Gets current game context for triggers.
     */
    private GameContext getGameContext() {
        return new GameContext(minewright, memory);
    }

    /**
     * Records that player responded to a comment.
     */
    public void recordPlayerResponse() {
        engagementTracker.recordPlayerResponse();
        frequencyManager.onCommentDelivered(true);
    }

    /**
     * Records that player ignored a comment (called periodically).
     */
    public void recordCommentIgnored() {
        engagementTracker.recordCommentIgnored();
        frequencyManager.onCommentDelivered(false);
    }

    /**
     * Initializes all dialogue triggers.
     */
    private void initializeTriggers() {
        triggers.add(new IdleTrigger());
        triggers.add(new ContextTrigger());
        triggers.add(new ActivityTrigger());
        triggers.add(new EventTrigger());
        triggers.add(new EnvironmentalTrigger());

        LOGGER.info("Initialized {} dialogue triggers", triggers.size());
    }

    public int getCommentsThisSession() {
        return commentsThisSession;
    }

    public void resetSessionCounters() {
        commentsThisSession = 0;
        engagementTracker.resetSessionCounters();
    }
}
```

---

## Integration Points

### 1. Integration with MineWrightEntity

Add to `MineWrightEntity.java`:

```java
public class MineWrightEntity extends PathfinderMob {
    // ... existing fields ...

    private ProactiveDialogueManager dialogueManager;

    public MineWrightEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        // ... existing initialization ...

        // Initialize proactive dialogue
        this.dialogueManager = new ProactiveDialogueManager(this, companionMemory);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // ... existing tick logic ...

            // Tick proactive dialogue system
            dialogueManager.tick();
        }
    }

    /**
     * Called when player responds to MineWright (via chat or command).
     */
    public void onPlayerResponse() {
        dialogueManager.recordPlayerResponse();
    }
}
```

### 2. Integration with EventBus

Subscribe to events for reactive comments:

```java
public class ProactiveDialogueEventHandler {
    private final ProactiveDialogueManager manager;

    public ProactiveDialogueEventHandler(ProactiveDialogueManager manager, EventBus eventBus) {
        this.manager = manager;

        // Subscribe to relevant events
        eventBus.subscribe(ActionCompletedEvent.class, this::onActionCompleted);
        eventBus.subscribe(PlayerDeathEvent.class, this::onPlayerDeath);
        eventBus.subscribe(AchievementEvent.class, this::onAchievement);
    }

    private void onActionCompleted(ActionCompletedEvent event) {
        // Trigger task completion comments
        manager.triggerImmediate(TriggerType.TASK_COMPLETE, event);
    }

    private void onPlayerDeath(PlayerDeathEvent event) {
        // Trigger sympathy/support comments
        manager.triggerImmediate(TriggerType.PLAYER_DEATH, event);
    }

    private void onAchievement(AchievementEvent event) {
        // Trigger celebration comments
        manager.triggerImmediate(TriggerType.ACHIEVEMENT, event);
    }
}
```

### 3. Integration with ActionExecutor

Hook into action completion/failure:

```java
public class ActionExecutor {
    // ... existing code ...

    private void executeTask(Task task) {
        // ... existing execution logic ...

        currentAction = createAction(task);

        if (currentAction != null) {
            currentAction.start();

            // Notify dialogue system about action start
            // (For comment opportunities)
        }
    }
}
```

---

## Configuration

### Add to MineWrightConfig.java

```java
public class MineWrightConfig {
    // ... existing config ...

    // Proactive Dialogue Configuration
    public static final ForgeConfigSpec.BooleanValue DIALOGUE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue DIALOGUE_BASE_FREQUENCY;
    public static final ForgeConfigSpec.IntValue DIALOGUE_IDLE_TRIGGER_SECONDS;
    public static final ForgeConfigSpec.IntValue DIALOGUE_MIN_RAPPORT_FOR_CHATTY;
    public static final ForgeConfigSpec.BooleanValue DIALOGUE_ADAPTIVE_FREQUENCY;
    public static final ForgeConfigSpec.IntValue DIALOGUE_MAX_IGNORED_BEFORE_SILENT;

    static {
        // ... existing config ...

        // Proactive Dialogue Configuration
        builder.comment("Proactive Dialogue System Configuration").push("dialogue");

        DIALOGUE_ENABLED = builder
            .comment("Enable proactive commentary (MineWright speaks without being prompted)")
            .define("enabled", true);

        DIALOGUE_BASE_FREQUENCY = builder
            .comment("Base comment frequency as percentage (0.0-1.0)")
            .defineInRange("baseFrequency", 0.25, 0.0, 1.0);

        DIALOGUE_IDLE_TRIGGER_SECONDS = builder
            .comment("Seconds of inactivity before idle comments")
            .defineInRange("idleTriggerSeconds", 45, 15, 300);

        DIALOGUE_MIN_RAPPORT_FOR_CHATTY = builder
            .comment("Minimum rapport level for frequent comments")
            .defineInRange("minRapportForChatty", 50, 0, 100);

        DIALOGUE_ADAPTIVE_FREQUENCY = builder
            .comment("Automatically adjust frequency based on player engagement")
            .define("adaptiveFrequency", true);

        DIALOGUE_MAX_IGNORED_BEFORE_SILENT = builder
            .comment("Consecutive ignored comments before reducing frequency")
            .defineInRange("maxIgnoredBeforeSilent", 3, 1, 10);

        builder.pop();
    }
}
```

### Config File Format (minewright-common.toml)

```toml
# Proactive Dialogue Settings

[dialogue]
# Enable proactive commentary
enabled = true

# Base frequency (0.0 = silent, 1.0 = very chatty)
base_frequency = 0.25

# Idle trigger settings
idle_trigger_seconds = 45

# Relationship-based frequency
min_rapport_for_chatty = 50

# Adaptive engagement detection
adaptive_frequency = true
max_ignored_before_silent = 3

# Cooldown settings (in seconds)
[dialogue.cooldown]
idle = 300
context = 120
activity = 180
event = 60
critical = 10

# Comment type preferences (0-100)
[dialogue.preferences]
observational = 70
suggestive = 50
reflective = 30
humorous = 40
supportive = 60
inquisitive = 20
```

---

## Example Comments

### Idle Comments

```
"You know what I was thinking? If creepers had elbows, they'd be terrible at arm wrestling. Just... not built for it."

"Been doing some calculations. Based on current progress, we'll finish this project approximately never. But I appreciate the optimism."

"I've decided I want a name. Not 'MineWright.' Everyone's named MineWright. Something dignified. 'Archibald'? 'Maximilian'? You're not listening, are you?"

"Sometimes I wonder about the villagers. Do they have dreams? Aspirations? Or do they just stand around staring at each other all day? Existential questions."

"I'm bored. Can we fight something? I want to see you nearly die again. That was entertaining."
```

### Context Comments (Biomes)

```
# Desert
"Ah, the desert. Hot, dry, and full of things trying to kill us. Just like my ex's apartment."
"Reminder: Hydration is important. Not for me, obviously. I'm fine. YOU need water. A lot of it."

# Mountains
"The mountains. Cold, unforgiving, and absolutely stunning. Kind of like you before coffee."
"Watch your step. Gravity gets enthusiastic at altitude. I speak from experience. Observational experience."

# Nether
"Welcome to Hell. Well, technically the Nether, but close enough. Try not to die immediately?"
"I hate this place. Everything wants to kill us. Even the ground. Especially the ground."

# Village
"Civilization! Well, sort of. They don't talk much. Or do much. Or... really anything except stare and trade emeralds."
"Look at them. Just... standing there. I think they're judging your building skills. I certainly am."
```

### Activity Comments

```
# Mining (sustained)
"You've been at this a while. Found anything good, or just expanding your collection of cobblestone?"
"Mining marathon. Impressive dedication or just stubborn? Either way, I respect it."
"How's the excavation going? Found any diamonds yet? No? Just more stone? That's... that's minecraft for you."

# Building (sustained)
"This is coming together. Slowly. Painfully slowly. But it's happening. I think."
"You've been placing blocks for two hours. I don't know whether to be impressed or concerned."
"The creative process is beautiful. Also exhausting. Mostly exhausting. How's your back?"

# Combat (after)
"That was... intense. You okay? You look like you just fought a horde of angry monsters. Oh wait, you did."
"Battle-hardened veteran. I respect it. Maybe get some armor next time? Just a suggestion."
```

### Event Comments

```
# Task Complete
"Got it done. Another job well finished. On to the next!"
"Done and dusted. That didn't take too long, did it?"
"Task complete. I believe a brief celebration is in order. Brief. Very brief."

# Task Fail
"Having some trouble? Want me to suggest a different approach?"
"That didn't go as planned. Do you want to try again, or...?"
"Setbacks happen. They're part of the process. The annoying, terrible part. But still part of it."

# Achievement
"NOW we're talking! That's what I'm talking about!"
"BIG move! Absolutely killing it today!"
"This is why we do this. Moments like this. You're on fire, boss."
```

### Supportive Comments

```
"You're doing great. Just wanted you to know that."

"I know this is tedious. But you're making real progress."

"Hey, take a break if you need to. I'll be here when you get back."

"You've got this. I believe in you. (Not that you need my belief, you're clearly capable.)"

"Remember: Rome wasn't built in a day. But they also didn't have an immortal construction foreman helping them."
```

---

## Performance Considerations

### Optimization Strategies

1. **Tick Throttling**: Only evaluate triggers every 20 ticks (1 second)
2. **Lazy Evaluation**: Stop evaluating as soon as a trigger fires
3. **Cached Context**: Reuse game context calculations within tick
4. **Indexed Comments**: Use hash maps for O(1) comment lookup
5. **Concurrent Data**: Use concurrent collections for thread safety

### Memory Usage

- **Comment Library**: ~50-100 KB (hundreds of comments)
- **Trigger State**: ~1-2 KB per trigger
- **Cooldown Tracking**: ~500 bytes
- **Engagement History**: ~1 KB
- **Total**: ~55-105 KB per MineWright entity

---

## Testing Checklist

- [ ] Idle triggers fire correctly after inactivity
- [ ] Context triggers respond to biome/time/weather changes
- [ ] Activity triggers detect sustained actions
- [ ] Event triggers respond to game events
- [ ] Cooldowns prevent comment spam
- [ ] Adaptive frequency adjusts to player response
- [ ] Rapport filtering prevents inappropriate comments
- [ ] Personality adjustments affect comment style
- [ ] Memory references appear at high rapport
- [ ] Context suppression prevents comments during combat
- [ ] Multiple MineWright entities don't interfere

---

## Future Enhancements

1. **Machine Learning**: Train models on player feedback to improve comment selection
2. **Proactive Suggestions**: Suggest actions based on current context
3. **Emotional Awareness**: Detect player mood from behavior
4. **Voice Integration**: Use TTS for spoken commentary
5. **Multi-Agent Banter**: MineWrights talking to each other
6. **Player Learning**: Remember player preferences for comment types
7. **Situational Awareness**: Detect player frustration, offer help

---

**Document Version:** 1.0
**Author:** MineWright AI Development Team
**Status:** Ready for Implementation
**Last Updated:** 2026-02-26
