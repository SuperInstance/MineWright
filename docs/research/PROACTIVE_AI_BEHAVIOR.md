# Proactive AI Behavior Research
## Autonomous Actions and Initiative-Taking Patterns for Assistants

**Research Date:** 2026-02-27
**Project:** Steve AI - Minecraft Autonomous Agents
**Component:** ProactiveDialogueManager Enhancement
**Version:** 1.0

---

## Executive Summary

This document synthesizes cutting-edge research on proactive AI behavior, anticipatory systems, and autonomous agent initiative-taking from 2024-2025. It provides actionable improvements for the Steve AI ProactiveDialogueManager, focusing on when AI should speak without prompting, how to detect opportunities for help, and balancing proactive vs. reactive behaviors.

**Key Findings:**
1. **Timing is Critical:** Well-timed proactive suggestions achieve 52% engagement rates vs. 62% dismissal for poorly-timed interruptions
2. **Context Awareness Matters:** Multi-modal signal integration (behavioral patterns, physiological cues, environmental factors) improves intervention accuracy
3. **Adaptive Learning:** User preference adaptation reduces repetitive explanations and improves satisfaction
4. **Competence Preservation:** Overly proactive AI can undermine user competence and reduce satisfaction
5. **Personality-Based Patterns:** Different users have different collaboration styles requiring adaptive approaches

---

## Table of Contents

1. [When Should AI Speak Without Prompting](#when-should-ai-speak-without-prompting)
2. [Detecting Opportunities for Help](#detecting-opportunities-for-help)
3. [Balancing Proactive vs. Reactive](#balancing-proactive-vs-reactive)
4. [Context Triggers for Suggestions](#context-triggers-for-suggestions)
5. [Learning User Preferences](#learning-user-preferences)
6. [Improvements for ProactiveDialogueManager](#improvements-for-proactivedialoguemanager)
7. [Implementation Roadmap](#implementation-roadmap)
8. [References](#references)

---

## When Should AI Speak Without Prompting

### Research-Based Timing Principles

#### 1. **Workflow Boundary Interventions**

**Finding:** Interventions at natural workflow breaks achieve dramatically higher engagement.

| Timing Context | Engagement Rate | Dismissal Rate |
|----------------|-----------------|----------------|
| **Post-task completion** (e.g., after git commit) | 52% | 15% |
| **Mid-task** (during active editing) | 18% | 62% |
| **Context transitions** (environment changes) | 45% | 28% |
| **Idle periods** (no activity >45s) | 38% | 35% |

**Key Principle:** Align AI commentary with natural cognitive breakpoints. When users complete a task, their attention is available for new input. During active work, interruptions are disruptive.

**Application to Steve AI:**
```java
// Current: Fixed interval checking
private final int baseCheckInterval = 100;  // Every 5 seconds

// Improved: Workflow-aware checking
private boolean shouldCheckTriggers() {
    // Don't interrupt during:
    // - Active combat (high cognitive load)
    // - Redstone placement (precision work)
    // - First login (learning phase)
    // - Recent death (emotional recovery)

    // Prefer:
    // - After task completion
    // - During idle periods
    // - At biome transitions
    // - After successful actions
}
```

#### 2. **Predictive Intervention Windows**

**Finding:** AI can predict user needs 20 seconds before explicit requests based on behavioral signals.

**Observable Predictive Signals:**
- **Gaze Patterns:** Rapid scanning indicates confusion or search
- **Movement Changes:** Slowed pace or stopping indicates decision point
- **Repetition:** Repeated failed attempts indicate stuck state
- **Environmental:** Approaching danger (lava, cliffs) without noticing

**LlamaPIE Research (University of Washington, 2025):**
- Two-model architecture: Lightweight timing model + larger content model
- Assistance limited to 1-3 words to avoid flow disruption
- On-device processing for privacy
- Strong user preference over reactive models

**Application to Steve AI:**
```java
/**
 * Predictive trigger system based on behavioral signals
 */
public class PredictiveTriggerDetector {

    private final int SIGNAL_WINDOW_TICKS = 40;  // 2 seconds observation

    public InterventionPriority shouldIntervene(Entity steve, Player player) {
        // Signal 1: Player stuck (repetitive movement)
        if (detectStuckBehavior(player)) {
            return InterventionPriority.HIGH;
        }

        // Signal 2: Danger proximity (unnoticed)
        if (detectUnnoticedDanger(player)) {
            return InterventionPriority.CRITICAL;
        }

        // Signal 3: Idle/uncertain (slow movement, looking around)
        if (detectUncertainty(player)) {
            return InterventionPriority.MEDIUM;
        }

        // Signal 4: Post-task completion
        if (detectTaskCompletion(steve)) {
            return InterventionPriority.LOW;
        }

        return InterventionPriority.NONE;
    }

    private boolean detectStuckBehavior(Player player) {
        // Track movement patterns over 2 seconds
        // If player moves back and forth in small area
        // Or repeatedly tries same action
        return patternAnalyzer.isStuck(player.getUUID());
    }

    private boolean detectUnnoticedDanger(Player player) {
        // Check for nearby threats player isn't facing
        // Lava, cliffs, hostile mobs behind player
        BlockPos playerPos = player.blockPosition();
        Vec3 lookDir = player.getLookAngle();

        return dangerDetector.findUnnoticedThreats(playerPos, lookDir)
            .size() > 0;
    }
}
```

#### 3. **Multi-Modal Signal Integration**

**ProAct Framework (Peking University, 2025):** Dual-system proactive agent using:
- **Context Encoder:** Compresses conversation history, visual frames, past behaviors
- **Behavior Planner:** Evaluates motivation from 5 dimensions:
  1. Visual scene changes
  2. User intent signals
  3. Dialogue state
  4. Social norms
  5. Emotional response needs

**Application to Steve AI:**
```java
/**
 * Multi-dimensional motivation evaluator for proactive dialogue
 */
public class MotivationEvaluator {

    public double calculateMotivationScore(SteveEntity steve, Player player) {
        double score = 0.0;

        // Dimension 1: Visual scene changes (20%)
        score += evaluateSceneChanges(steve, player) * 0.20;

        // Dimension 2: User intent signals (25%)
        score += evaluateUserIntent(steve, player) * 0.25;

        // Dimension 3: Dialogue state (20%)
        score += evaluateDialogueState(steve, player) * 0.20;

        // Dimension 4: Social norms (15%)
        score += evaluateSocialNorms(steve, player) * 0.15;

        // Dimension 5: Emotional response needs (20%)
        score += evaluateEmotionalNeeds(steve, player) * 0.20;

        return score;
    }

    private double evaluateSceneChanges(SteveEntity steve, Player player) {
        // Biome change? New structure discovered?
        // Day/night transition? Weather change?
        double changeScore = 0.0;

        if (contextTracker.biomeChanged()) changeScore += 0.4;
        if (contextTracker.timeTransitioned()) changeScore += 0.3;
        if (contextTracker.weatherChanged()) changeScore += 0.3;

        return changeScore;
    }

    private double evaluateUserIntent(SteveEntity steve, Player player) {
        // What is player trying to do?
        // Mining? Building? Exploring? Fighting?

        PlayerIntent intent = intentDetector.detectCurrentIntent(player);

        // Align commentary with intent
        return switch (intent.type()) {
            case MINING -> 0.3;  // Low - don't distract
            case BUILDING -> 0.4;  // Medium - occasional encouragement
            case EXPLORING -> 0.7;  // High - good time to chat
            case IDLE -> 0.9;  // Very high - initiate conversation
            case COMBAT -> 0.1;  // Minimal - critical focus needed
            default -> 0.5;
        };
    }
}
```

---

## Detecting Opportunities for Help

### Opportunity Detection Patterns

#### 1. **Hidden Pain Point Discovery**

**Finding:** Best AI opportunities often appear as "invisible pain points" - problems users don't even recognize as problems anymore.

**AI Opportunity Tree Framework (2025):** Six categories of proactive value:
1. **Automation & Productivity** - Repetitive task elimination
2. **Improvement & Enhancement** - Optimizing existing workflows
3. **Personalization** - Tailoring to individual preferences
4. **Inspiration & Innovation** - Suggesting novel approaches
5. **Convenience** - Reducing friction
6. **Emotional Value** - Providing support, encouragement

**Application to Steve AI:**
```java
/**
 * Opportunity detection for Minecraft-specific scenarios
 */
public class OpportunityDetector {

    public List<Opportunity> detectOpportunities(SteveEntity steve, Player player) {
        List<Opportunity> opportunities = new ArrayList<>();

        // Category 1: Automation opportunities
        opportunities.addAll(detectAutomationOpportunities(steve, player));

        // Category 2: Enhancement opportunities
        opportunities.addAll(detectEnhancementOpportunities(steve, player));

        // Category 3: Personalization opportunities
        opportunities.addAll(detectPersonalizationOpportunities(steve, player));

        // Category 4: Inspiration opportunities
        opportunities.addAll(detectInspirationOpportunities(steve, player));

        // Category 5: Convenience opportunities
        opportunities.addAll(detectConvenienceOpportunities(steve, player));

        // Category 6: Emotional support opportunities
        opportunities.addAll(detectEmotionalOpportunities(steve, player));

        return opportunities;
    }

    private List<Opportunity> detectAutomationOpportunities(SteveEntity steve, Player player) {
        List<Opportunity> ops = new ArrayList<>();

        // Detect repetitive mining patterns
        if (activityTracker.isRepetitiveMining(player)) {
            ops.add(new Opportunity(
                OpportunityType.AUTOMATION,
                "I notice you've been mining this tunnel for a while. " +
                "Would you like me to continue the pattern while you do something else?",
                0.7  // Confidence score
            ));
        }

        // Detect repetitive farming
        if (activityTracker.isRepetitiveFarming(player)) {
            ops.add(new Opportunity(
                OpportunityType.AUTOMATION,
                "Those crops need harvesting again? I can handle that if you'd like.",
                0.8
            ));
        }

        return ops;
    }

    private List<Opportunity> detectEnhancementOpportunities(SteveEntity steve, Player player) {
        List<Opportunity> ops = new ArrayList<>();

        // Detect inefficient mining patterns
        if (activityTracker.isInefficientMining(player)) {
            ops.add(new Opportunity(
                OpportunityType.ENHANCEMENT,
                "I've noticed a more efficient mining pattern for this situation. " +
                "Want me to show you?",
                0.6
            ));
        }

        // Detect suboptimal building techniques
        if (activityTracker.hasBuildingImprovement(player)) {
            ops.add(new Opportunity(
                OpportunityType.ENHANCEMENT,
                "That structure could be more stable with a different foundation. " +
                "Shall I suggest an alternative?",
                0.5
            ));
        }

        return ops;
    }

    private List<Opportunity> detectEmotionalOpportunities(SteveEntity steve, Player player) {
        List<Opportunity> ops = new ArrayList<>();

        // Detect recent death (frustration)
        if (playerStats.getTimeSinceLastDeath() < 300) {  // 5 minutes
            ops.add(new Opportunity(
                OpportunityType.EMOTIONAL_SUPPORT,
                "Tough break back there. Want help recovering your items?",
                0.9
            ));
        }

        // Detect long work session (fatigue)
        if (activityTracker.getSessionDuration(player) > 3600) {  // 1 hour
            ops.add(new Opportunity(
                OpportunityType.EMOTIONAL_SUPPORT,
                "You've been at it a while. Maybe take a break? I can hold down the fort.",
                0.6
            ));
        }

        // Detect achievement (celebration)
        if (playerStats.getRecentAchievements().size() > 0) {
            ops.add(new Opportunity(
                OpportunityType.EMOTIONAL_SUPPORT,
                "That was impressive! Great work!",
                0.95
            ));
        }

        return ops;
    }
}
```

#### 2. **Anticipatory Pattern Recognition**

**Sensible Agent Framework (arXiv, 2025):** Two-layer taxonomy for proactive actions:

**Action Categories:**
- Suggest - Propose ideas or actions
- Remind - Recall important information
- Guide - Provide direction or instruction
- Summarize - Condense complex situations
- Automate - Execute tasks autonomously
- Visual Augmentation - Enhance perception
- Information Retrieval - Fetch relevant data
- Take App Action - Interact with applications

**Context Categories:**
- **Familiarity-Based** - New vs. familiar situations
- **Urgency-Based** - Time-sensitive matters
- **Social Coordination** - Multi-player scenarios
- **Cognitive Load** - Mental bandwidth availability
- **Sensory Disruption** - Environmental interference

**Application to Steve AI:**
```java
/**
 * Context-aware action selection
 */
public class ProactiveActionSelector {

    public ProactiveAction selectAction(ActionCategory category, ContextCategory context) {
        return switch (category) {
            case SUGGEST -> selectSuggestion(context);
            case REMIND -> selectReminder(context);
            case GUIDE -> selectGuidance(context);
            case SUMMARIZE -> selectSummary(context);
            case AUTOMATE -> selectAutomation(context);
            default -> null;
        };
    }

    private ProactiveAction selectSuggestion(ContextCategory context) {
        return switch (context) {
            case FAMILIARITY_NEW -> {
                // Player is new to this situation - provide helpful suggestions
                yield new SuggestionAction(
                    "Since you're new to this area, would you like some tips on " +
                    "navigating the terrain safely?",
                    SuggestionType.HELPFUL
                );
            }
            case URGENCY_HIGH -> {
                // Time is critical - be direct and brief
                yield new SuggestionAction(
                    "We need to move. Follow me - I know a safer path.",
                    SuggestionType.URGENT
                );
            }
            case COGNITIVE_LOAD_HIGH -> {
                // Player is overwhelmed - keep it simple
                yield new SuggestionAction(
                    "Focus on what you're doing. I'll handle the rest.",
                    SuggestionType.REASSURING
                );
            }
            default -> new SuggestionAction(
                "Let me know if you need any suggestions.",
                SuggestionType.GENERAL
            );
        };
    }
}
```

#### 3. **Continuous Exploration Patterns**

**Finding:** AI agents should actively seek new information and discover "unknown unknowns" rather than just optimizing within predefined solution spaces.

**Agent Design Patterns (Tencent Cloud, 2025):** Exploration and discovery patterns enable AI to:
- Proactively seek new information
- Discover possibilities outside current knowledge
- Identify hidden opportunities
- Adapt to rapidly evolving domains

**Application to Steve AI:**
```java
/**
 * Exploration and discovery system for Steve AI
 */
public class ExplorationManager {

    /**
     * Steve should proactively explore and report findings
     */
    public void exploreSurroundings(SteveEntity steve, Player player) {
        // Don't explore if player is busy
        if (isPlayerBusy(player)) {
            return;
        }

        // Explore nearby areas
        List<Discovery> discoveries = scoutNearbyAreas(steve);

        // Report interesting findings
        for (Discovery discovery : discoveries) {
            if (discovery.interestScore() > 0.7) {
                reportDiscovery(steve, player, discovery);
            }
        }
    }

    private List<Discovery> scoutNearbyAreas(SteveEntity steve) {
        List<Discovery> discoveries = new ArrayList<>();

        // Check for villages
        discoveries.addAll(findStructures(steve, StructureType.VILLAGE));

        // Check for resources
        discoveries.addAll(findResources(steve));

        // Check for biomes
        discoveries.addAll(findBiomeTransitions(steve));

        // Check for dangers
        discoveries.addAll(findDangers(steve));

        return discoveries;
    }

    private void reportDiscovery(SteveEntity steve, Player player, Discovery discovery) {
        String report = switch (discovery.type()) {
            case STRUCTURE -> String.format(
                "I found a %s %d blocks that way. Might be worth investigating.",
                discovery.structureType(),
                discovery.distance()
            );
            case RESOURCE -> String.format(
                "There's a cluster of %s nearby if we need it.",
                discovery.resourceType()
            );
            case DANGER -> String.format(
                "Watch out - there's %s in that direction.",
                discovery.dangerType()
            );
            case BIOME -> String.format(
                "The terrain changes to %s ahead. Just a heads up.",
                discovery.biomeType()
            );
        };

        steve.sendChatMessage(report);
    }
}
```

---

## Balancing Proactive vs. Reactive

### The Competence Preservation Principle

**Critical Finding (Springer, 2025):** Proactive (vs. reactive) help from AI agents can lead to **higher loss of users' competence-based self-esteem**, reducing system satisfaction. This effect is moderated by users' AI knowledge - those with higher AI knowledge experience greater competence loss from proactive help.

**Implication:** Too much proactivity undermines user agency and perceived competence.

### Collaboration Style Personalization

**Carnegie Mellon Research (2026):** Four collaboration personality types:

| Collaboration Style | Characteristics | Optimal AI Approach |
|---------------------|----------------|---------------------|
| **Hands-off** | Trusts AI, rarely intervenes | High autonomy, minimal confirmation |
| **Takeover** | Assumes full control when issues arise | Wait for problems, then step back |
| **Close supervision** | Frequent monitoring and adjustments | Ask permission often, provide transparency |
| **Collaborative** | Selective intervention at key points | **Optimal balance** - autonomous but confirm major actions |

**Application to Steve AI:**
```java
/**
 * Detect and adapt to player's collaboration style
 */
public class CollaborationStyleDetector {

    public enum CollaborationStyle {
        HANDS_OFF,      // Let Steve do his thing
        TAKEOVER,       // Take over when problems occur
        CLOSE_SUPERVISION,  // Monitor everything
        COLLABORATIVE    // Balanced approach (ideal)
    }

    private final Map<UUID, PlayerInteractionHistory> histories = new ConcurrentHashMap<>();

    public CollaborationStyle detectStyle(UUID playerId) {
        PlayerInteractionHistory history = histories.get(playerId);
        if (history == null) {
            return CollaborationStyle.COLLABORATIVE;  // Default
        }

        // Analyze interaction patterns
        double interventionRate = history.getInterventionRate();
        double correctionRate = history.getCorrectionRate();
        double monitoringFrequency = history.getMonitoringFrequency();

        // Classify based on patterns
        if (interventionRate < 0.1 && correctionRate < 0.1) {
            return CollaborationStyle.HANDS_OFF;
        } else if (correctionRate > 0.5) {
            return CollaborationStyle.TAKEOVER;
        } else if (monitoringFrequency > 0.8) {
            return CollaborationStyle.CLOSE_SUPERVISION;
        } else {
            return CollaborationStyle.COLLABORATIVE;
        }
    }

    /**
     * Adjust autonomy based on collaboration style
     */
    public double getAutonomyLevel(UUID playerId) {
        return switch (detectStyle(playerId)) {
            case HANDS_OFF -> 0.9;  // High autonomy
            case TAKEOVER -> 0.3;  // Low autonomy (wait for issues)
            case CLOSE_SUPERVISION -> 0.5;  // Medium autonomy, confirm often
            case COLLABORATIVE -> 0.7;  // Balanced autonomy
        };
    }
}
```

### Adaptive Frequency Management

**PaRT Framework (arXiv, 2025):** Personalized Real-Time Recommendations address limitations in current proactive dialogue systems:
- **Overly generic content** - Direct prompting produces topics misaligned with user preferences
- **Knowledge boundaries** - LLMs struggle with real-time context and domain-specific conversations
- **Solution:** User profiling modules with memory mechanisms to better match preferences

**Application to Steve AI:**
```java
/**
 * Adaptive frequency manager based on player engagement
 */
public class AdaptiveDialogueFrequency {

    private double currentFrequencyMultiplier = 1.0;
    private int ignoredCount = 0;
    private int acknowledgedCount = 0;
    private final Map<UUID, PlayerProfile> playerProfiles = new ConcurrentHashMap<>();

    /**
     * Update frequency based on player response
     */
    public void onCommentDelivered(UUID playerId, boolean acknowledged) {
        PlayerProfile profile = playerProfiles.computeIfAbsent(
            playerId, k -> new PlayerProfile()
        );

        if (acknowledged) {
            profile.acknowledgedCount++;
            ignoredCount = 0;

            // Slowly increase frequency if player likes it
            if (profile.acknowledgedCount > 5 && ignoredCount == 0) {
                currentFrequencyMultiplier = Math.min(1.5, currentFrequencyMultiplier * 1.05);
            }
        } else {
            ignoredCount++;
            profile.ignoredCount++;

            // Decrease frequency if ignored
            if (ignoredCount > 3) {
                currentFrequencyMultiplier = Math.max(0.2, currentFrequencyMultiplier * 0.7);
            }
        }

        // Also track time-based patterns
        profile.recordInteraction(System.currentTimeMillis(), acknowledged);
    }

    /**
     * Check if comment should be suppressed based on adaptive frequency
     */
    public boolean shouldSuppressComment(UUID playerId, CommentType type) {
        PlayerProfile profile = playerProfiles.get(playerId);
        if (profile == null) {
            return false;  // No history, allow comment
        }

        // Always allow critical comments (danger, important events)
        if (type.getPriority() == CommentPriority.CRITICAL) {
            return false;
        }

        // Calculate time-of-day preference
        double timePreference = profile.getTimeOfDayPreference(System.currentTimeMillis());

        // Calculate recent interaction density (don't spam)
        double recentInteractionPenalty = profile.getRecentInteractionDensity() * 0.3;

        // Apply frequency multiplier
        double adjustedChance = type.getBaseChance() * currentFrequencyMultiplier * timePreference;
        adjustedChance -= recentInteractionPenalty;

        // Roll against adjusted chance
        return Math.random() > adjustedChance;
    }

    /**
     * Player profile with temporal preferences
     */
    private static class PlayerProfile {
        int acknowledgedCount = 0;
        int ignoredCount = 0;
        private final List<Long> interactionTimestamps = new ArrayList<>();
        private final Map<Integer, Double> hourlyPreference = new HashMap<>();

        void recordInteraction(long timestamp, boolean acknowledged) {
            interactionTimestamps.add(timestamp);

            int hour = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
            ).getHour();

            // Update hourly preference (exponential moving average)
            double oldValue = hourlyPreference.getOrDefault(hour, 0.5);
            double newValue = acknowledged ? 1.0 : 0.0;
            hourlyPreference.put(hour, oldValue * 0.9 + newValue * 0.1);
        }

        double getTimeOfDayPreference(long timestamp) {
            int hour = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
            ).getHour();

            return hourlyPreference.getOrDefault(hour, 0.5);
        }

        double getRecentInteractionDensity() {
            // Count interactions in last 5 minutes
            long fiveMinutesAgo = System.currentTimeMillis() - 300000;
            long recentCount = interactionTimestamps.stream()
                .filter(t -> t > fiveMinutesAgo)
                .count();

            return Math.min(1.0, recentCount / 10.0);  // Cap at 10 interactions
        }
    }
}
```

### Context-Aware Suppression

**AiGet Research (arXiv, 2025):** 7-day study showed users developed personalized usage habits and emphasized:
- Flexible output options
- On/off controls
- Decreased distraction and cognitive load over time

**Application to Steve AI:**
```java
/**
 * Context-aware suppression - know when to stay silent
 */
public class ContextAwareSuppression {

    public static boolean shouldSuppress(SteveEntity steve, Player player) {
        // NEVER speak during these situations:

        // 1. Critical combat (boss fights, overwhelming odds)
        if (isInCriticalCombat(player)) {
            return true;
        }

        // 2. Precision work (redstone, detailed building)
        if (isDoingPrecisionWork(player)) {
            return true;
        }

        // 3. Player frustration detected (rapid failed attempts)
        if (isPlayerFrustrated(player)) {
            return true;  // Let them figure it out
        }

        // 4. Health critical (low health, danger)
        if (isHealthCritical(player)) {
            return true;  // Focus on survival
        }

        // 5. First tutorial (let player learn basics)
        if (isFirstTutorialSession(player)) {
            return true;
        }

        // 6. Recent death (grief recovery)
        if (hasRecentDeath(player)) {
            return true;  // Give them space
        }

        // 7. Player explicitly requested silence
        if (hasSilencePreference(player)) {
            return true;
        }

        return false;
    }

    private static boolean isPlayerFrustrated(Player player) {
        // Detect frustration through:
        // - Rapid block placement/breaking
        // - Repeated deaths in same area
        // - Excessive jumping/movement
        // - Low health from self-damage

        return frustrationDetector.isFrustrated(player.getUUID());
    }

    private static boolean isDoingPrecisionWork(Player player) {
        // Detect precision work:
        // - Redstone component placement
        // - Detailed pixel art
        // - intricate farming setups

        return activityTracker.isDoingPrecisionWork(player.getUUID());
    }
}
```

---

## Context Triggers for Suggestions

### Trigger Categories and Thresholds

Based on research, triggers should be organized by priority and context:

#### 1. **Critical Triggers** (Immediate Action Required)

| Trigger | Condition | Cooldown | Example |
|---------|-----------|----------|---------|
| `DANGER_IMMEDIATE` | Lava/creeper within 3 blocks | 10s | "Watch out!" |
| `DEATH_IMMINENT` | Player health < 2 hearts | 5s | "Fall back!" |
| `HOSTILE_AMBUSH` | Multiple hostiles attacking | 15s | "Behind you!" |

#### 2. **High Priority Triggers** (Important but Not Urgent)

| Trigger | Condition | Cooldown | Example |
|---------|-----------|----------|---------|
| `TASK_COMPLETE` | Steve finishes task | 1m | "Done!" |
| `ACHIEVEMENT` | Player advancement | 5m | "Nice work!" |
| `RESOURCE_LOW` | Essential item depleted | 3m | "Low on torches" |
| `WEATHER_STORM` | Thunderstorm starts | 2m | "Seek shelter!" |

#### 3. **Medium Priority Triggers** (Contextual Opportunities)

| Trigger | Condition | Cooldown | Example |
|---------|-----------|----------|---------|
| `BIOME_ENTER` | Enter new biome | 5m | "Interesting area..." |
| `TIME_DUSK` | Sunset approaching | 10m | "Night coming soon" |
| `STRUCTURE_FOUND` | Discover village/temple | 15m | "Civilization!" |
| `OPPORTUNITY_AUTO` | Repetitive task detected | 8m | "Want me to continue?" |

#### 4. **Low Priority Triggers** (Ambient Commentary)

| Trigger | Condition | Cooldown | Example |
|---------|-----------|----------|---------|
| `IDLE_SHORT` | No activity 45s | 3m | "Nice view..." |
| `IDLE_LONG` | No activity 5m | 10m | "Still deciding?" |
| `REFLECTION` | Past experience relevant | 20m | "Remember when..." |
| `HUMOR` | Opportune moment | 30m | [Joke] |

### Context-Aware Trigger Adjustment

**Google Project Astra (2025):** Proactive, context-aware AI that:
- Watches and listens constantly, deciding when to intervene
- Can proactively point out mistakes
- Reminds based on schedules
- Alerts based on integrated data

**Application to Steve AI:**
```java
/**
 * Context-aware trigger system
 */
public class ContextAwareTriggerSystem {

    /**
     * Evaluate if trigger should fire based on context
     */
    public boolean shouldTrigger(DialogueTrigger trigger, SteveEntity steve, Player player) {
        // Base trigger check
        if (!trigger.checkCondition(steve, player)) {
            return false;
        }

        // Check cooldown
        if (cooldownManager.isOnCooldown(trigger.getType())) {
            return false;
        }

        // Context-aware adjustment
        double contextMultiplier = calculateContextMultiplier(trigger, steve, player);

        // Roll against adjusted probability
        double adjustedChance = trigger.getBaseProbability() * contextMultiplier;
        return Math.random() < adjustedChance;
    }

    private double calculateContextMultiplier(DialogueTrigger trigger, SteveEntity steve, Player player) {
        double multiplier = 1.0;

        // Rapport-based adjustment
        int rapport = steve.getCompanionMemory().getRapportLevel();
        multiplier *= (0.5 + (rapport / 100.0));  // 0.5x to 1.5x based on rapport

        // Player state adjustment
        if (isPlayerStressed(player)) {
            multiplier *= 0.5;  // Reduce commentary when stressed
        }

        // Time of day adjustment
        long dayTime = steve.level().getDayTime() % 24000;
        if (dayTime > 13000 && dayTime < 14000) {  // Dusk
            multiplier *= 1.2;  // Slightly more chatty at dusk
        }

        // Recent interaction density
        if (interactionTracker.getRecentCommentCount(300000) > 5) {  // 5 min
            multiplier *= 0.3;  // Reduce if already commented recently
        }

        // Trigger type vs activity alignment
        PlayerActivity activity = activityTracker.getCurrentActivity(player);
        if (!trigger.isCompatibleWith(activity)) {
            multiplier *= 0.1;  // Strong penalty for incompatible activities
        }

        return multiplier;
    }

    private boolean isPlayerStressed(Player player) {
        // Detect stress through:
        // - Low health
        // - Low hunger
        // - Recent combat
        // - Rapid movements
        // - Inefficient actions

        return stressDetector.calculateStressLevel(player) > 0.7;
    }
}
```

---

## Learning User Preferences

### Preference Learning Strategies

**Meta's PAHF System (2026):** System that "remembers" user preferences:
- Faster learning when preferences change vs. initial learning
- Good generalization across simple and complex tasks
- Reduces repetitive explanation burden
- Includes proactive inquiry mechanism to avoid blind guessing

### Multi-Dimensional Preference Tracking

**Application to Steve AI:**
```java
/**
 * Multi-dimensional user preference tracking
 */
public class UserPreferenceTracker {

    private final Map<UUID, UserPreferences> preferences = new ConcurrentHashMap<>();

    /**
     * Track user's preferred interaction style
     */
    public void recordInteraction(UUID playerId, InteractionType type, boolean positive) {
        UserPreferences prefs = preferences.computeIfAbsent(
            playerId, k -> new UserPreferences()
        );

        prefs.recordInteraction(type, positive);

        // Update derived preferences
        updateDerivedPreferences(playerId, prefs);
    }

    private void updateDerivedPreferences(UUID playerId, UserPreferences prefs) {
        // 1. Comment frequency preference
        double totalInteractions = prefs.getTotalInteractions();
        double positiveRatio = prefs.getPositiveRatio();

        if (totalInteractions > 10) {
            if (positiveRatio > 0.7) {
                prefs.setFrequencyPreference(FrequencyPreference.HIGH);
            } else if (positiveRatio < 0.3) {
                prefs.setFrequencyPreference(FrequencyPreference.LOW);
            } else {
                prefs.setFrequencyPreference(FrequencyPreference.MEDIUM);
            }
        }

        // 2. Comment type preferences
        prefs.preferredTypes = prefs.typePreferences.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // 3. Collaboration style
        prefs.collaborationStyle = detectCollaborationStyle(prefs);

        // 4. Temporal patterns
        prefs.updateTemporalPatterns();
    }

    /**
     * Get personalized comment frequency
     */
    public double getPersonalizedFrequency(UUID playerId) {
        UserPreferences prefs = preferences.get(playerId);
        if (prefs == null) {
            return 0.5;  // Default
        }

        return switch (prefs.getFrequencyPreference()) {
            case HIGH -> 0.8;
            case MEDIUM -> 0.5;
            case LOW -> 0.2;
            case ADAPTIVE -> prefs.getAdaptiveFrequency();
        };
    }

    /**
     * Get preferred comment types for this user
     */
    public List<CommentType> getPreferredTypes(UUID playerId) {
        UserPreferences prefs = preferences.get(playerId);
        if (prefs == null) {
            return List.of(CommentType.OBSERVATIONAL, CommentType.SUPPORTIVE);
        }

        return prefs.preferredTypes;
    }

    /**
     * Comprehensive user preference model
     */
    private static class UserPreferences {
        // Interaction history
        private final Map<InteractionType, Integer> interactionCounts = new HashMap<>();
        private final Map<InteractionType, Integer> positiveCounts = new HashMap<>();

        // Derived preferences
        private FrequencyPreference frequencyPreference = FrequencyPreference.MEDIUM;
        private List<CommentType> preferredTypes = List.of();
        private CollaborationStyle collaborationStyle = CollaborationStyle.COLLABORATIVE;

        // Temporal patterns
        private final Map<Integer, Double> hourlyEngagement = new HashMap<>();
        private final Map<DayOfWeek, Double> dailyEngagement = new HashMap<>();

        // Domain-specific preferences
        private final Map<ActivityDomain, Double> domainPreferences = new HashMap<>();

        void recordInteraction(InteractionType type, boolean positive) {
            interactionCounts.merge(type, 1, Integer::sum);
            if (positive) {
                positiveCounts.merge(type, 1, Integer::sum);
            }
        }

        double getTotalInteractions() {
            return interactionCounts.values().stream().mapToInt(Integer::intValue).sum();
        }

        double getPositiveRatio() {
            long total = getTotalInteractions();
            long positive = positiveCounts.values().stream().mapToInt(Integer::intValue).sum();
            return total > 0 ? (double) positive / total : 0.5;
        }

        double getAdaptiveFrequency() {
            // Calculate based on recent positive ratio
            return Math.min(1.0, Math.max(0.0, getPositiveRatio()));
        }

        void updateTemporalPatterns() {
            // Update hourly engagement (exponential moving average)
            int currentHour = LocalDateTime.now().getHour();
            // ... implementation

            // Update daily engagement
            DayOfWeek currentDay = LocalDate.now().getDayOfWeek();
            // ... implementation
        }
    }

    public enum FrequencyPreference {
        LOW, MEDIUM, HIGH, ADAPTIVE
    }

    public enum CollaborationStyle {
        HANDS_OFF, TAKEOVER, CLOSE_SUPERVISION, COLLABORATIVE
    }

    public enum ActivityDomain {
        MINING, BUILDING, EXPLORING, COMBAT, FARMING, REDSTONE
    }
}
```

### Proactive Inquiry Mechanism

**Key Principle:** Don't guess - ask when uncertain about preferences.

**Application to Steve AI:**
```java
/**
 * Proactive inquiry system for learning preferences
 */
public class ProactiveInquirySystem {

    /**
     * When uncertain about preference, proactively ask
     */
    public void inquireIfUncertain(SteveEntity steve, Player player, PreferenceType prefType) {
        UserPreferenceTracker tracker = steve.getPreferenceTracker();
        UUID playerId = player.getUUID();

        // Only inquire if we have insufficient data
        if (tracker.hasSufficientData(playerId, prefType)) {
            return;  // We know their preference
        }

        // Check if good time to ask (low cognitive load)
        if (!isGoodTimeToAsk(player)) {
            return;  // Player is busy
        }

        // Ask appropriate question
        String inquiry = generateInquiry(prefType);
        steve.sendChatMessage(inquiry);

        // Track that we asked
        tracker.recordInquiry(playerId, prefType);
    }

    private boolean isGoodTimeToAsk(Player player) {
        // Good time if:
        // - Not in combat
        // - Not doing precision work
        // - Not low health
        // - Idle or low-stress activity
        // - Haven't asked recently (within 10 minutes)

        return !ContextAwareSuppression.shouldSuppress(null, player)
            && activityTracker.getCurrentActivity(player) == PlayerActivity.IDLE
            && inquiryTracker.timeSinceLastInquiry(player.getUUID()) > 600000;  // 10 min
    }

    private String generateInquiry(PreferenceType prefType) {
        return switch (prefType) {
            case COMMENT_FREQUENCY ->
                "Hey, am I talking too much, too little, or just about right?";
            case COMMENT_TYPE ->
                "Do you prefer I stick to factual observations, or are you okay with " +
                "some occasional humor and suggestions?";
            case AUTONOMY_LEVEL ->
                "Would you prefer I ask before taking action, or should I just go " +
                "ahead unless it's something major?";
            case HELP_TIMING ->
                "Do you want me to offer help proactively, or would you rather ask " +
                "when you need something?";
        };
    }
}
```

---

## Improvements for ProactiveDialogueManager

### Current Implementation Analysis

**Strengths:**
- Solid foundation with tick-based checking
- Good trigger categorization (time, context, weather, proximity)
- Rapport-based frequency adjustment
- Fallback comments for reliability
- Cooldown system to prevent spam

**Weaknesses:**
- Fixed interval checking doesn't respect workflow boundaries
- No predictive intervention (only reactive)
- Limited multi-modal signal integration
- No collaboration style detection
- Missing preference learning system
- Insufficient context-aware suppression

### Proposed Improvements

#### 1. Workflow-Aware Timing

```java
public class ProactiveDialogueManager {

    // OLD: Fixed interval
    // private final int baseCheckInterval = 100;  // Every 5 seconds

    // NEW: Workflow-aware checking
    private boolean shouldEvaluateNow() {
        // Don't interrupt during:
        if (isInCriticalMoment()) {
            return false;
        }

        // Prefer natural breakpoints:
        if (isAtWorkflowBoundary()) {
            return true;
        }

        // Standard periodic check (but less frequent)
        return tickCounter >= 200;  // Every 10 seconds instead of 5
    }

    private boolean isInCriticalMoment() {
        Player player = getNearestPlayer();
        if (player == null) return false;

        // Critical moments:
        return player.getHealth() < player.getMaxHealth() * 0.3  // Low health
            || isInCombat(player)  // In combat
            || isDoingPrecisionWork(player)  // Redstone, detailed building
            || hasRecentDeath(player)  // Died recently
            || isFirstSession(player);  // Still learning basics
    }

    private boolean isAtWorkflowBoundary() {
        // Natural breakpoints:
        return minewright.getActionExecutor().isExecuting() == false  // Just finished task
            || isPlayerIdle(getNearestPlayer())  // Player idle
            || didContextJustChange();  // Biome/weather change
    }
}
```

#### 2. Predictive Intervention System

```java
public class ProactiveDialogueManager {

    private final PredictiveTriggerDetector predictiveDetector;
    private final MotivationEvaluator motivationEvaluator;

    public void tick() {
        if (!shouldEvaluateNow()) {
            return;
        }

        // NEW: Check predictive triggers first
        InterventionPriority priority = predictiveDetector.shouldIntervene(
            minewright, getNearestPlayer()
        );

        if (priority != InterventionPriority.NONE) {
            // Calculate motivation score
            double motivation = motivationEvaluator.calculateMotivationScore(
                minewright, getNearestPlayer()
            );

            // Only intervene if motivation is high enough
            double threshold = priority.getMinimumMotivation();
            if (motivation >= threshold) {
                triggerPredictiveComment(priority, motivation);
            }
        }

        // Then fall back to regular triggers
        checkTimeBasedTriggers();
        checkContextBasedTriggers();
        checkWeatherTriggers();
        checkPlayerProximityTriggers();
        checkActionStateTriggers();
    }

    private void triggerPredictiveComment(InterventionPriority priority, double motivation) {
        // Select comment based on priority and motivation
        CommentType type = selectCommentTypeForPriority(priority);

        // Generate contextually appropriate comment
        String comment = generatePredictiveComment(type, priority, motivation);

        // Deliver if it passes all filters
        if (shouldDeliverComment(type)) {
            deliverComment(comment, type);
        }
    }
}
```

#### 3. Enhanced Preference Learning

```java
public class ProactiveDialogueManager {

    private final UserPreferenceTracker preferenceTracker;
    private final ProactiveInquirySystem inquirySystem;

    // Track player responses to learn preferences
    public void recordPlayerResponse(UUID playerId, boolean positive) {
        preferenceTracker.recordInteraction(
            playerId,
            InteractionType.PROACTIVE_COMMENT,
            positive
        );

        // Adjust adaptive frequency
        adaptiveFrequency.onCommentDelivered(positive);
    }

    // Use learned preferences in decision making
    private boolean shouldConsiderSpeaking() {
        Player player = getNearestPlayer();
        if (player == null) return false;

        // Get personalized frequency
        double personalizedFreq = preferenceTracker.getPersonalizedFrequency(
            player.getUUID()
        );

        // Apply to base probability
        double adjustedChance = baseCommentChance * personalizedFreq;

        return random.nextDouble() < adjustedChance;
    }

    // Select comment type based on preferences
    private CommentType selectCommentType() {
        Player player = getNearestPlayer();
        if (player == null) {
            return CommentType.OBSERVATIONAL;
        }

        List<CommentType> preferredTypes = preferenceTracker.getPreferredTypes(
            player.getUUID()
        );

        if (preferredTypes.isEmpty()) {
            return CommentType.OBSERVATIONAL;
        }

        // Weighted random from preferred types
        return preferredTypes.get(random.nextInt(preferredTypes.size()));
    }

    // Periodically inquire about preferences
    public void maybeInquireAboutPreferences() {
        Player player = getNearestPlayer();
        if (player == null) return;

        // Check various preference types
        inquirySystem.inquireIfUncertain(minewright, player, PreferenceType.COMMENT_FREQUENCY);
        inquirySystem.inquireIfUncertain(minewright, player, PreferenceType.COMMENT_TYPE);
        inquirySystem.inquireIfUncertain(minewright, player, PreferenceType.AUTONOMY_LEVEL);
    }
}
```

#### 4. Enhanced Context-Aware Suppression

```java
public class ProactiveDialogueManager {

    private boolean shouldDeliverComment(CommentType type) {
        Player player = getNearestPlayer();
        if (player == null) return false;

        // NEW: Enhanced suppression checks
        if (ContextAwareSuppression.shouldSuppress(minewright, player)) {
            LOGGER.debug("Comment suppressed - inappropriate context");
            return false;
        }

        // Check adaptive frequency
        if (adaptiveFrequency.shouldSuppressComment(player.getUUID(), type)) {
            LOGGER.debug("Comment suppressed - adaptive frequency");
            return false;
        }

        // Check collaboration style compatibility
        CollaborationStyle style = collaborationStyleDetector.detectStyle(player.getUUID());
        if (!type.isCompatibleWith(style)) {
            LOGGER.debug("Comment suppressed - incompatible with collaboration style");
            return false;
        }

        // Check time-of-day preference
        if (!isGoodTimeOfDay(player)) {
            LOGGER.debug("Comment suppressed - not a good time of day");
            return false;
        }

        return true;
    }

    private boolean isGoodTimeOfDay(Player player) {
        UUID playerId = player.getUUID();
        int currentHour = LocalDateTime.now().getHour();

        // Get player's hourly preference
        double preference = preferenceTracker.getHourlyEngagementPreference(
            playerId, currentHour
        );

        // If preference is very low, suppress
        return preference > 0.2;
    }
}
```

#### 5. Opportunity Detection Integration

```java
public class ProactiveDialogueManager {

    private final OpportunityDetector opportunityDetector;

    public void tick() {
        // ... existing trigger checks ...

        // NEW: Check for opportunities
        List<Opportunity> opportunities = opportunityDetector.detectOpportunities(
            minewright, getNearestPlayer()
        );

        for (Opportunity opp : opportunities) {
            if (shouldPursueOpportunity(opp)) {
                offerOpportunity(opp);
                break;  // Only offer one opportunity per tick
            }
        }
    }

    private boolean shouldPursueOpportunity(Opportunity opp) {
        // Check if opportunity type matches player preferences
        Player player = getNearestPlayer();
        List<CommentType> preferredTypes = preferenceTracker.getPreferredTypes(
            player.getUUID()
        );

        boolean typePreferred = switch (opp.type()) {
            case AUTOMATION -> preferredTypes.contains(CommentType.SUGGESTIVE);
            case ENHANCEMENT -> preferredTypes.contains(CommentType.SUGGESTIVE);
            case INSPIRATION -> preferredTypes.contains(CommentType.INQUISITIVE);
            case EMOTIONAL_SUPPORT -> preferredTypes.contains(CommentType.SUPPORTIVE);
            default -> true;
        };

        // Check confidence threshold
        if (opp.confidence() < 0.6) {
            return false;
        }

        // Check if good timing
        return !ContextAwareSuppression.shouldSuppress(minewright, player)
            && typePreferred;
    }

    private void offerOpportunity(Opportunity opp) {
        String message = opp.suggestion();

        // Adjust based on rapport
        int rapport = memory.getRapportLevel();
        if (rapport < 50) {
            // More formal for new relationships
            message = "Pardon me, but " + message.toLowerCase();
        } else if (rapport > 80) {
            // More casual for close relationships
            // Keep message as-is
        }

        minewright.sendChatMessage(message);
        recordCommentDelivered(CommentType.fromOpportunityType(opp.type()));
    }
}
```

#### 6. Collaboration Style Adaptation

```java
public class ProactiveDialogueManager {

    private final CollaborationStyleDetector styleDetector;

    private boolean shouldConsiderSpeaking() {
        Player player = getNearestPlayer();
        if (player == null) return false;

        // Detect collaboration style
        CollaborationStyle style = styleDetector.detectStyle(player.getUUID());

        // Adjust behavior based on style
        double autonomyLevel = styleDetector.getAutonomyLevel(player.getUUID());

        // Hands-off users: more autonomy, less commenting
        // Takeover users: less autonomy, wait for problems
        // Close supervision: confirm often
        // Collaborative: balanced approach

        double adjustedChance = baseCommentChance * autonomyLevel;

        return random.nextDouble() < adjustedChance;
    }

    private void deliverComment(String comment, CommentType type) {
        Player player = getNearestPlayer();
        CollaborationStyle style = styleDetector.detectStyle(player.getUUID());

        // Adjust delivery based on collaboration style
        switch (style) {
            case HANDS_OFF -> {
                // Just say it, no confirmation needed
                minewright.sendChatMessage(comment);
            }
            case TAKEOVER -> {
                // Rarely comment, only if important
                if (type.getPriority() == CommentPriority.CRITICAL) {
                    minewright.sendChatMessage(comment);
                }
            }
            case CLOSE_SUPERVISION -> {
                // Provide more context, ask if okay
                String enhancedComment = comment + " Is that alright?";
                minewright.sendChatMessage(enhancedComment);
            }
            case COLLABORATIVE -> {
                // Balanced approach - standard delivery
                minewright.sendChatMessage(comment);
            }
        }
    }
}
```

### Complete Enhanced Class Structure

```java
/**
 * Enhanced ProactiveDialogueManager with research-backed improvements
 */
public class ProactiveDialogueManager {

    // Core components
    private final ForemanEntity minewright;
    private final CompanionMemory memory;
    private final ConversationManager conversationManager;
    private final AsyncLLMClient llmClient;
    private final Random random;

    // NEW: Advanced systems
    private final PredictiveTriggerDetector predictiveDetector;
    private final MotivationEvaluator motivationEvaluator;
    private final OpportunityDetector opportunityDetector;
    private final UserPreferenceTracker preferenceTracker;
    private final ProactiveInquirySystem inquirySystem;
    private final CollaborationStyleDetector styleDetector;
    private final AdaptiveDialogueFrequency adaptiveFrequency;

    // State tracking
    private int tickCounter = 0;
    private int ticksSinceLastComment = 0;
    private long lastCommentTimestamp = 0;
    private String lastCommentType = null;

    // Cooldown tracking
    private final Map<String, Long> triggerCooldowns = new HashMap<>();

    public ProactiveDialogueManager(ForemanEntity minewright) {
        this.minewright = minewright;
        this.memory = minewright.getCompanionMemory();
        this.conversationManager = new ConversationManager(minewright);
        this.random = new Random();

        // Initialize advanced systems
        this.predictiveDetector = new PredictiveTriggerDetector(minewright);
        this.motivationEvaluator = new MotivationEvaluator(minewright);
        this.opportunityDetector = new OpportunityDetector(minewright);
        this.preferenceTracker = new UserPreferenceTracker();
        this.inquirySystem = new ProactiveInquirySystem(preferenceTracker);
        this.styleDetector = new CollaborationStyleDetector();
        this.adaptiveFrequency = new AdaptiveDialogueFrequency();

        // Initialize LLM client
        this.llmClient = new AsyncGroqClient(
            MineWrightConfig.OPENAI_API_KEY.get(),
            "llama-3.1-8b-instant",
            150,  // Brief comments
            0.9   // High creativity
        );

        LOGGER.info("Enhanced ProactiveDialogueManager initialized for '{}'",
            minewright.getSteveName());
    }

    /**
     * Enhanced tick loop with workflow-aware timing
     */
    public void tick() {
        if (!enabled) {
            return;
        }

        tickCounter++;
        ticksSinceLastComment++;

        // NEW: Workflow-aware timing instead of fixed intervals
        if (!shouldEvaluateNow()) {
            return;
        }

        // Check predictive triggers first (highest priority)
        checkPredictiveTriggers();

        // Then opportunity detection
        checkOpportunities();

        // Then regular triggers (existing behavior)
        checkTimeBasedTriggers();
        checkContextBasedTriggers();
        checkWeatherTriggers();
        checkPlayerProximityTriggers();
        checkActionStateTriggers();

        // Periodically inquire about preferences
        if (tickCounter % 6000 == 0) {  // Every 5 minutes
            maybeInquireAboutPreferences();
        }
    }

    private boolean shouldEvaluateNow() {
        // Don't interrupt critical moments
        if (isInCriticalMoment()) {
            return false;
        }

        // Prefer workflow boundaries
        if (isAtWorkflowBoundary()) {
            return true;
        }

        // Standard periodic check (less frequent than before)
        return tickCounter % 200 == 0;  // Every 10 seconds
    }

    private boolean isInCriticalMoment() {
        Player player = getNearestPlayer();
        if (player == null) return false;

        return ContextAwareSuppression.shouldSuppress(minewright, player);
    }

    private boolean isAtWorkflowBoundary() {
        return !minewright.getActionExecutor().isExecuting()  // Task just finished
            || isPlayerIdle(getNearestPlayer())
            || didContextJustChange();
    }

    private void checkPredictiveTriggers() {
        Player player = getNearestPlayer();
        if (player == null) return;

        InterventionPriority priority = predictiveDetector.shouldIntervene(
            minewright, player
        );

        if (priority != InterventionPriority.NONE) {
            double motivation = motivationEvaluator.calculateMotivationScore(
                minewright, player
            );

            if (motivation >= priority.getMinimumMotivation()) {
                triggerPredictiveComment(priority, motivation);
            }
        }
    }

    private void checkOpportunities() {
        Player player = getNearestPlayer();
        if (player == null) return;

        List<Opportunity> opportunities = opportunityDetector.detectOpportunities(
            minewright, player
        );

        for (Opportunity opp : opportunities) {
            if (shouldPursueOpportunity(opp, player)) {
                offerOpportunity(opp);
                break;
            }
        }
    }

    // ... rest of implementation ...
}
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Goal:** Implement core improvements without breaking existing functionality

1. **Add workflow-aware timing**
   - Replace fixed interval checks with `shouldEvaluateNow()`
   - Add `isInCriticalMoment()` and `isAtWorkflowBoundary()` methods
   - Update existing trigger checks to respect workflow boundaries

2. **Implement enhanced context suppression**
   - Add frustration detection
   - Add precision work detection
   - Integrate suppression into trigger evaluation

3. **Add basic preference tracking**
   - Implement `UserPreferenceTracker` with interaction recording
   - Add `recordPlayerResponse()` method
   - Use preferences to adjust comment frequency

### Phase 2: Predictive Systems (Week 3-4)

**Goal:** Add predictive intervention capabilities

1. **Implement predictive trigger detector**
   - Add stuck behavior detection
   - Add unnoticed danger detection
   - Add uncertainty detection
   - Integrate into main tick loop

2. **Add motivation evaluator**
   - Implement multi-dimensional scoring
   - Add scene change detection
   - Add user intent evaluation
   - Connect to trigger decisions

3. **Integrate opportunity detection**
   - Implement automation opportunity detection
   - Add enhancement opportunity detection
   - Add emotional support opportunity detection
   - Create opportunity offering system

### Phase 3: Advanced Personalization (Week 5-6)

**Goal:** Deep personalization and adaptation

1. **Implement collaboration style detection**
   - Track player interaction patterns
   - Classify collaboration style
   - Adjust autonomy based on style

2. **Add proactive inquiry system**
   - Implement preference inquiry logic
   - Add good timing detection
   - Track inquiry history

3. **Enhance temporal preference learning**
   - Track hourly engagement patterns
   - Track daily patterns
   - Apply temporal adjustments to commenting

### Phase 4: Refinement and Testing (Week 7-8)

**Goal:** Polish and validate improvements

1. **Performance optimization**
   - Profile new systems
   - Optimize hot paths
   - Add caching where beneficial

2. **User testing**
   - Gather feedback on commenting frequency
   - Test collaboration style detection accuracy
   - Validate preference learning

3. **Documentation**
   - Update system documentation
   - Create configuration guide
   - Write troubleshooting guide

---

## References

### Academic Papers

1. **Developer Interaction Patterns with Proactive AI: A Five-Day Field Study** (arXiv, January 2026)
   - https://arxiv.org/html/2601.10253v1
   - Key findings on workflow boundary interventions and engagement rates

2. **ProActive Agent: Peking University's Proactive Social Agent** (February 2026)
   - Dual-system architecture with context encoder and behavior planner
   - Five-dimensional motivation evaluation

3. **When AI-Based Agents Are Proactive: Implications for Competence** (Springer, 2025)
   - https://link.springer.com/article/10.1007/s12599-024-00918-y
   - Competence preservation principle

4. **LlamaPIE: Proactive In-Ear Conversation Assistants** (arXiv, May 2025)
   - https://arxiv.org/html/2505.04066v1
   - Two-model pipeline for timing and content

5. **Sensible Agent: Framework for Unobtrusive Interaction** (arXiv, September 2025)
   - https://arxiv.org/html/2509.09255v1
   - Two-layer taxonomy for proactive actions and contexts

6. **PaRT: Personalized Real-Time Recommendations** (arXiv, April 2025)
   - https://arxiv.org/html/2504.20624v1
   - User profiling with memory mechanisms

7. **AiGet: Everyday Knowledge System** (arXiv, January 2025)
   - https://arxiv.org/html/2501.16240v1
   - 7-day study on proactive systems and flexible controls

8. **Google Project Astra** (LinkedIn, May 2025)
   - https://www.linkedin.com/pulse/project-astra-googles-vision-universal-multimodal-ai-assistant-anna-gcmtc
   - Proactive, context-aware AI assistant principles

9. **Carnegie Mellon Human-AI Collaboration Study** (February 2026)
   - Four collaboration personality types
   - Three main reasons for human intervention

10. **Proactive Conversational AI: A Comprehensive Survey** (CSDN, October 2025)
    - https://blog.csdn.net/2401_87847301/article/details/153675814
    - Three elements of proactivity: Anticipation, Initiative, Adaptation

### Industry Research

11. **AWS Agentic AI Security Scoping Matrix** (November 2025)
    - https://aws.amazon.com/blogs/security/the-agentic-ai-security-scoping-matrix-a-framework-for-securing-autonomous-ai-systems/
    - Agency vs. Autonomy balance framework

12. **AI Opportunity Tree Framework** (Juejin, 2025)
    - https://juejin.cn/post/7515714245021204521
    - Six categories of proactive AI value

13. **Agent Design Patterns: Exploration and Discovery** (Tencent Cloud)
    - http://www.cloud.tencent.com/developer/article/2581269
    - Pattern recognition for hidden opportunities

14. **Proact or AI: WisdomAI's 24/7 Data Analysts** (Baidu, 2025)
    - https://baijiahao.baidu.com/s?id=1842295207485808230
    - Real-time anomaly detection and proactive insights

### Books and Comprehensive Guides

15. **Context-Aware Computing Foundation** (Interaction Design Foundation, April 2025)
    - https://www.interaction-design.org/literature/book/the-encyclopedia-of-human-computer-interaction-2nd-edition/context-aware-computing-context-awareness-context-aware-user-interfaces-and-implicit-interaction
    - Foundational work on proactive applications

---

## Conclusion

This research document provides a comprehensive foundation for enhancing the Steve AI ProactiveDialogueManager with cutting-edge proactive AI behaviors. The key improvements focus on:

1. **Timing Precision:** Workflow-aware interventions instead of fixed intervals
2. **Predictive Capability:** Anticipating user needs before explicit requests
3. **Personalization:** Learning and adapting to individual user preferences
4. **Context Sensitivity:** Understanding when to speak and when to remain silent
5. **Competence Preservation:** Balancing helpfulness with user autonomy

By implementing these research-backed improvements, Steve AI will become a more natural, helpful, and contextually appropriate companion that enhances the Minecraft experience without being intrusive or annoying.

---

**Document Version:** 1.0
**Author:** Steve AI Development Team
**Status:** Ready for Implementation
**Last Updated:** 2026-02-27
