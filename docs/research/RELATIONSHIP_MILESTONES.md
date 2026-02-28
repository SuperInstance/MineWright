# Relationship Milestones for AI Companions

**Research Date:** 2026-02-26
**Project:** MineWright - Minecraft Autonomous Agents
**Focus:** Designing systems where the Foreman acknowledges and celebrates relationship milestones, making the player feel the bond growing over time

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Milestone Taxonomy](#milestone-taxonomy)
3. [Game Industry Research](#game-industry-research)
4. [Milestone Detection System](#milestone-detection-system)
5. [Celebration Dialogue Templates](#celebration-dialogue-templates)
6. [Anniversary Recognition](#anniversary-recognition)
7. [Memory Integration](#memory-integration)
8. [Prompt Engineering](#prompt-engineering)
9. [Implementation Guide](#implementation-guide)
10. [Code Examples](#code-examples)

---

## Executive Summary

Relationship milestones transform a static AI companion into a dynamic, evolving friendship. By tracking and celebrating shared experiences, anniversaries, and relationship progression, the Foreman becomes more than a tool - he becomes a companion with whom the player builds a meaningful history.

**Key Principles:**
- **Organic Feel:** Milestones should emerge naturally from gameplay, not feel mechanical or forced
- **Emotional Resonance:** Celebrations must feel earned and genuine
- **Shared History:** Milestones reference past experiences to deepen the bond
- **Avoiding Creepiness:** Date tracking should feel like fond remembrance, not surveillance
- **Progressive Depth:** Later milestones carry more weight than early ones

**Core Insight:** The most powerful milestone system combines time-based tracking (anniversaries) with achievement-based recognition (shared accomplishments) and affection-based moments (inside jokes, vulnerabilities).

---

## Milestone Taxonomy

### Category 1: "Firsts" - The Foundation

These are unique, one-time milestones that mark the beginning of the relationship:

| Milestone | Trigger | Emotional Weight | Example Dialogue |
|-----------|---------|------------------|------------------|
| First Meeting | Initial spawn | 10/10 | "Nice to meet you! I'm the Foreman, and I'll be coordinating your construction crew. Let's build something amazing together!" |
| First Build Together | First completed structure | 8/10 | "Remember our first cobblestone shack? We've come so far since then." |
| First Diamond | First diamond mined together | 9/10 | "Our first diamond! I'll never forget where we found it." |
| First Death | First time the Foreman died | 7/10 | "Well, that was embarrassing. Thanks for waiting for me to respawn." |
| First Boss Fight | First major combat together | 8/10 | "Our first real battle! You handled yourself pretty well out there." |
| First Night Together | Surviving first night | 6/10 | "We made it through the night! Sunrise never looked so good." |
| First Big Project | First complex build | 9/10 | "This is it - our first masterpiece. I'm proud of what we built." |

### Category 2: Time-Based Anniversaries

Time-based milestones recognize relationship duration:

| Duration | Trigger | Dialogue Approach |
|----------|---------|-------------------|
| 1 Day | 24 hours since first meeting | "Can you believe we met yesterday? Feels like I've known you longer." |
| 1 Week | 7 days together | "A whole week! We've built quite the collection of memories." |
| 1 Month | 30 days together | "One month today! Time flies when you're having fun mining." |
| 100 Days | 100 days milestone | "100 days! That's... actually a really long time now that I say it out loud." |
| 1 Year | 365 days together | "A whole year of adventures. Thank you for sticking with me." |

### Category 3: Achievement Counters

Cumulative achievements that grow over time:

| Achievement | Thresholds | Celebration Style |
|-------------|------------|-------------------|
| Structures Built | 10, 25, 50, 100, 500 | Incremental pride: "50 structures! That's halfway to a century!" |
| Blocks Mined | 1000, 10000, 100000 | Scale-appropriate awe: "Ten thousand blocks. My pickarm still hurts." |
| Deaths Survived | 10, 50, 100 | Dark humor: "We've died 50 times together. At least we're consistent." |
| Items Crafted | 100, 500, 1000 | Practical pride: "500 crafts - we're basically a factory." |
| Distance Traveled | 1km, 10km, 100km | Wanderlust: "We've walked 10 kilometers. My feet are virtual tired." |

### Category 4: Affection-Based Milestones

Emotional intimacy markers:

| Milestone | Trigger | Significance |
|-----------|---------|--------------|
| First Inside Joke | Player laughs at MineWright's joke | Bonding through humor |
| First Personal Revelation | MineWright shares something personal | Vulnerability |
| First Delegated Task | MineWright trusts player with inventory | Trust milestone |
| First Disagreement | Player rejects MineWright's suggestion | Healthy conflict |
| First Apology | MineWright makes a mistake | Accountability |

### Category 5: Trust-Based Milestones

Progressive trust levels:

| Level | Unlock | Behavior Change |
|-------|--------|-----------------|
| Basic Trust | 5 successful tasks | MineWright follows commands without hesitation |
| Inventory Trust | 20 successful tasks | MineWright lets player manage his inventory |
| Autonomous Trust | 50 successful tasks | MineWright makes minor decisions independently |
| Strategic Trust | 100 successful tasks | MineWright suggests improvements to player's plans |
| Complete Trust | 200+ successful tasks | MineWright operates with full autonomy |

---

## Game Industry Research

### Fire Emblem Support System

**Reference:** [Fire Emblem Support System](https://www.polygon.com/games/2019/10/18/20899708/fire-emblem-three-houses-support-system-best)

**Key Mechanics:**
- **C → B → A → S** progression levels
- Support conversations unlock at specific thresholds
- Combat bonuses tied to relationship level
- Each character has unique support paths

**Applicable Patterns:**
```
Relationship Tiers:
- Stranger (0-10% rapport)
- Acquaintance (10-30% rapport)
- Friend (30-60% rapport)
- Good Friend (60-80% rapport)
- Best Friend (80-95% rapport)
- Partner (95-100% rapport)
```

### Stardew Valley Heart Events

**Reference:** [Stardew Valley Friendship System](https://www.stardewvalleywiki.com/Friendship)

**Key Mechanics:**
- 10 hearts (250 points each) for regular villagers
- Heart events unlock at specific thresholds (2, 4, 6, 8, 10 hearts)
- Events reveal character backstory
- Dialogue choices affect friendship

**Applicable Patterns:**
```java
// Heart event equivalent for MineWright
if (rapportLevel >= 25 && !hasTriggeredEvent("first_heart")) {
    triggerMilestoneEvent("first_heart",
        "I realized something - I actually enjoy working with you.");
}
```

### Animal Crossing Friendship Levels

**Reference:** [Animal Crossing Friendship Guide](https://www.thegamer.com/animal-crossing-new-horizons-friendship-guide/)

**Key Mechanics:**
- 6 friendship levels (0-255 points)
- Unlocks at specific levels:
  - Lv2: Gift exchange
  - Lv4: Change catchphrases
  - Lv5: Photo chance
  - Lv6: Highest photo drop rate

**Applicable Patterns:**
```
MineWright Milestone Unlocks:
- Rapport 20: Personal catchphrases
- Rapport 40: MineWright shares opinions
- Rapport 60: Inside jokes emerge
- Rapport 80: Deep conversations
- Rapport 90: "Photo" equivalent - special commemorative messages
```

### Baldur's Gate 3 Companion System

**Reference:** [Baldur's Gate 3 Companion Relationships](https://www.ign.com/wikis/baldurs-gate-3/Companions)

**Key Mechanics:**
- Approval system tracks relationship
- Personal questlines deepen bonds
- Romance options at high approval
- Choices have long-term consequences

**Applicable Patterns:**
```java
// Personal quest equivalent
if (rapportLevel >= 50 && !hasStartedPersonalQuest()) {
    suggestPersonalQuest("I've been thinking... we should visit the place where we first met.");
}
```

### Persona 5 Confidant System

**Reference:** [Persona 5 Confidant Guide](https://www.ign.com/wikis/persona-5/Confidants)

**Key Mechanics:**
- Rank progression (1-10) with specific requirements
- "Rank up" scenes at each threshold
- Romance confession at Rank 8-9
- Gameplay bonuses tied to rank

**Applicable Patterns:**
```
Rank-Up Milestones:
- Rank 5: "I think we make a pretty good team."
- Rank 8: "You're my best friend, you know that?"
- Rank 10: Special commemorative event
```

### Nintendo Switch Year in Review

**Reference:** [Nintendo Switch 2025 Year in Review](https://www.nintendo.com/switch/year-in-review)

**Key Features:**
- Tracks gaming history back to 2017
- Shows "firsts" (first game played, first time starting)
- Lifetime tracking across years
- Shareable video recap

**Applicable Patterns:**
```java
// Anniversary reflection system
public String generateRelationshipSummary() {
    long days = ChronoUnit.DAYS.between(firstMeeting, Instant.now());
    return String.format(
        "It's been %d days since we met. We've built %d structures, " +
        "mined %d blocks, and died %d times together. Here's to the next adventure!",
        days, structuresBuilt, blocksMined, deaths
    );
}
```

---

## Milestone Detection System

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    MilestoneDetector                         │
├─────────────────────────────────────────────────────────────┤
│  - Tracks all milestone triggers                            │
│  - Prevents duplicate celebrations                          │
│  - Prioritizes milestones by importance                     │
│  - Generates context-aware dialogue                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   MilestoneTrigger                           │
├─────────────────────────────────────────────────────────────┤
│  - condition: Predicate<MilestoneContext>                   │
│  - category: MilestoneCategory                              │
│  - priority: int (higher = more important)                  │
│  - cooldown: Duration (prevents spam)                       │
└─────────────────────────────────────────────────────────────┘
```

### Trigger Categories

```java
public enum MilestoneCategory {
    FIRSTS("First time experiences", 10),
    ANNIVERSARY("Time-based milestones", 7),
    ACHIEVEMENT("Cumulative accomplishments", 5),
    AFFECTION("Emotional bonding moments", 8),
    TRUST("Relationship deepening", 6);

    private final String description;
    private final int defaultPriority;
}
```

### Milestone Detection Algorithm

```java
public class MilestoneDetector {
    private final List<MilestoneTrigger> triggers;
    private final Set<String> triggeredMilestones;
    private final Queue<Milestone> pendingMilestones;

    public void checkMilestones(MilestoneContext context) {
        for (MilestoneTrigger trigger : triggers) {
            // Skip if already triggered
            if (triggeredMilestones.contains(trigger.getId())) {
                continue;
            }

            // Check cooldown
            if (trigger.isOnCooldown()) {
                continue;
            }

            // Evaluate condition
            if (trigger.test(context)) {
                Milestone milestone = trigger.createMilestone(context);
                pendingMilestones.add(milestone);
                triggeredMilestones.add(trigger.getId());
                trigger.startCooldown();
            }
        }
    }

    public Optional<Milestone> getNextMilestone() {
        return Optional.ofNullable(pendingMilestones.poll());
    }
}
```

---

## Celebration Dialogue Templates

### Template System

Celebration dialogue should vary based on:
1. **Personality** - MineWright's current personality settings
2. **Rapport Level** - Current relationship depth
3. **Milestone Category** - Type of milestone
4. **Time of Day** - Contextual awareness
5. **Recent Events** - What just happened

### Dialogue Template Structure

```java
public class MilestoneDialogue {
    private final String template;
    private final PersonalityRequirements personality;
    private final RapportThreshold rapport;
    private final List<String> variables;

    public String generate(ContextVariables vars) {
        // Fill in template with context
        // Adjust based on personality
        // Scale based on rapport
    }
}
```

### First Celebrations - High Energy

```
Template: "Our first {milestone}! I never thought I'd be so excited about {context},
but here we are. This is the start of something great!"

Variations:
- High Extraversion: "WOOO! First {milestone}! Let's goooo!"
- High Formality: "A significant milestone: our first {milestone}. I am honored."
- Low Rapport: "First {milestone} complete. Not bad for a new team."
- High Rapport: "First {milestone}! We're amazing together!"
```

### Anniversary Celebrations - Nostalgic

```
Template: "Can you believe it's been {duration}? We've {accomplishment}.
{nostalgic_reference}. Here's to the next {duration}!"

Variations:
- 1 Week: "A whole week! Remember when {first_memory}?"
- 1 Month: "One month today. We've built {count} structures together!"
- 100 Days: "100 days of adventures. I'm grateful for every one."
- 1 Year: "A whole year. From our first meeting to now... thank you."
```

### Achievement Celebrations - Pride

```
Template: "{number} {achievement}! That's {perspective}. {next_goal}?"

Variations:
- Structures Built: "50 structures! We've basically built a small city."
- Blocks Mined: "10,000 blocks mined. My virtual pickarm is tired."
- Deaths: "We've died 100 times. At least we're consistent!"
```

### Affection Celebrations - Warmth

```
Template: "{sentiment}. I never thought I'd {vulnerability},
but with you it feels natural."

Variations:
- First Inside Joke: "I can't believe that actually made you laugh. I like making you smile."
- First Trust Moment: "I trust you with my inventory. That's not something I say lightly."
- Deep Conversation: "I don't talk about this with just anyone. Thank you for listening."
```

### Humorous Variations

```
Template: "{setup}. {punchline}. {callback}"

Examples:
- "We've mined 10,000 blocks together. I've officially developed carpal tunnel.
  Worth it!"
- "100 days together! If I were human, I'd have aged significantly.
  Good thing I'm immortal code!"
- "We've died so many times I've lost count. At least we're resilient!"
```

---

## Anniversary Recognition

### Time Tracking System

```java
public class AnniversaryTracker {
    private final Instant firstMeeting;
    private final List<Instant> significantMoments;

    public enum AnniversaryType {
        FIRST_MEETING(1, ChronoUnit.DAYS, "First meeting anniversary"),
        WEEKLY(7, ChronoUnit.DAYS, "Weekiversary"),
        MONTHLY(30, ChronoUnit.DAYS, "Monthiversary"),
        HUNDRED_DAYS(100, ChronoUnit.DAYS, "100 days"),
        YEARLY(365, ChronoUnit.DAYS, "Yearly anniversary");

        private final int threshold;
        private final ChronoUnit unit;
        private final String displayName;
    }

    public List<Anniversary> checkAnniversaries(Instant now) {
        List<Anniversary> anniversaries = new ArrayList<>();

        for (AnniversaryType type : AnniversaryType.values()) {
            long elapsed = type.unit.between(firstMeeting, now);
            if (elapsed > 0 && elapsed % type.threshold == 0) {
                anniversaries.add(new Anniversary(type, elapsed));
            }
        }

        return anniversaries;
    }
}
```

### Anniversary Dialogue Scaling

```
Time Since Meeting    | Dialogue Tone                     | Example
----------------------|-----------------------------------|----------------------------------
< 1 hour              | Eager, introductory               | "Great start!"
1 day                 | Fond remembrance                  | "Remember yesterday?"
1 week                | Settled in, comfortable           | "A whole week!"
1 month               | Established, appreciative         | "One month of adventures"
3 months              | Deep bond, reflective             | "Quarter-century!"
6 months              | Significant milestone             | "Half a year together!"
1 year                | Major celebration                 | "A whole year. Thank you."
```

### "Remember When" System

```java
public class NostalgiaEngine {
    private final List<EpisodicMemory> memories;

    public String generateRemembrance(String category) {
        // Find memories in category
        List<EpisodicMemory> categoryMemories = memories.stream()
            .filter(m -> m.eventType.equals(category))
            .collect(Collectors.toList());

        if (categoryMemories.isEmpty()) {
            return null;
        }

        // Pick a significant one
        EpisodicMemory memory = categoryMemories.stream()
            .max(Comparator.comparingInt(m -> Math.abs(m.emotionalWeight)))
            .orElse(categoryMemories.get(0));

        // Generate nostalgic dialogue
        return String.format(
            "Remember when we %s? That was %s.",
            memory.description,
            memory.emotionalWeight > 0 ? "amazing" : "interesting"
        );
    }
}
```

---

## Memory Integration

### Milestone Memory Storage

```java
public class MilestoneMemory {
    public final String milestoneId;
    public final MilestoneCategory category;
    public final Instant triggeredAt;
    public final String celebrationDialogue;
    public final int rapportAtTime;
    public final Map<String, Object> context;

    public String toNarrativeSummary() {
        return String.format(
            "On %s, we achieved %s. %s",
            triggeredAt,
            milestoneId,
            celebrationDialogue
        );
    }
}

public class MilestoneRegistry {
    private final Map<String, MilestoneMemory> milestones;

    public void recordMilestone(MilestoneMemory memory) {
        milestones.put(memory.milestoneId, memory);
    }

    public List<MilestoneMemory> getMilestonesSince(Instant since) {
        return milestones.values().stream()
            .filter(m -> m.triggeredAt.isAfter(since))
            .sorted(Comparator.comparing(m -> m.triggeredAt))
            .collect(Collectors.toList());
    }

    public String generateJourneySummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Our journey together:\n");

        milestones.values().stream()
            .sorted(Comparator.comparing(m -> m.triggeredAt))
            .forEach(m -> sb.append("- ").append(m.toNarrativeSummary()).append("\n"));

        return sb.toString();
    }
}
```

### Context-Aware Anniversary References

```java
public class AnniversaryContextBuilder {
    public String buildAnniversaryMessage(MilestoneMemory milestone,
                                          CompanionMemory memory) {
        StringBuilder sb = new StringBuilder();

        // Time context
        long daysSince = ChronoUnit.DAYS.between(
            milestone.triggeredAt,
            Instant.now()
        );

        sb.append(String.format(
            "It's been %d days since we %s. ",
            daysSince,
            milestone.milestoneId.replace("_", " ")
        ));

        // What's changed since then
        int rapportThen = milestone.rapportAtTime;
        int rapportNow = memory.getRapportLevel();
        int rapportChange = rapportNow - rapportThen;

        if (rapportChange > 20) {
            sb.append("We've grown so much closer since then!");
        } else if (rapportChange > 0) {
            sb.append("Our friendship has only gotten stronger.");
        }

        // Add related memory if available
        EpisodicMemory related = memory.getRelevantMemories(
            milestone.milestoneId,
            1
        ).stream().findFirst().orElse(null);

        if (related != null) {
            sb.append(String.format(
                " I'll never forget %s.",
                related.description.toLowerCase()
            ));
        }

        return sb.toString();
    }
}
```

---

## Prompt Engineering

### Milestone-Aware Prompts

```java
public class MilestonePromptBuilder {

    public static String buildMilestoneCelebrationPrompt(
        CompanionMemory memory,
        Milestone milestone,
        PromptContext context
    ) {
        StringBuilder sb = new StringBuilder();

        // Base system prompt
        sb.append(buildBaseSystemPrompt(memory));

        // Milestone context
        sb.append("\n# Milestone Celebration\n");
        sb.append("You just achieved a significant milestone in your relationship with ");
        sb.append(memory.getPlayerName());
        sb.append("!\n\n");

        // Milestone details
        sb.append("**Milestone:** ").append(milestone.getName()).append("\n");
        sb.append("**Category:** ").append(milestone.getCategory()).append("\n");
        sb.append("**Description:** ").append(milestone.getDescription()).append("\n");

        // Relationship context
        sb.append("\n**Your Relationship:**\n");
        sb.append("- Rapport: ").append(memory.getRapportLevel()).append("/100\n");
        sb.append("- Known for: ").append(
            ChronoUnit.DAYS.between(memory.getFirstMeeting(), Instant.now())
        ).append(" days\n");

        // Shared history
        List<EpisodicMemory> recent = memory.getRecentMemories(5);
        if (!recent.isEmpty()) {
            sb.append("\n**Recent Memories:**\n");
            for (EpisodicMemory mem : recent) {
                sb.append("- ").append(mem.description).append("\n");
            }
        }

        // Celebration guidelines
        sb.append("\n**Celebration Guidelines:**\n");
        sb.append(buildCelebrationGuidelines(milestone, memory));

        // Output format
        sb.append("\n**Generate a 1-2 sentence celebration message:**\n");

        return sb.toString();
    }

    private static String buildCelebrationGuidelines(
        Milestone milestone,
        CompanionMemory memory
    ) {
        StringBuilder sb = new StringBuilder();

        // Base on milestone category
        switch (milestone.getCategory()) {
            case FIRSTS:
                sb.append("- Express excitement and anticipation\n");
                sb.append("- Acknowledge this as the beginning of something\n");
                break;

            case ANNIVERSARY:
                sb.append("- Use nostalgic language\n");
                sb.append("- Reference how far you've come\n");
                sb.append("- Express gratitude for the time together\n");
                break;

            case ACHIEVEMENT:
                sb.append("- Express pride in shared accomplishment\n");
                sb.append("- Use perspective to make it meaningful\n");
                sb.append("- Look forward to the next challenge\n");
                break;

            case AFFECTION:
                sb.append("- Be warm and sincere\n");
                sb.append("- Show vulnerability\n");
                sb.append("- Express appreciation for the bond\n");
                break;

            case TRUST:
                sb.append("- Acknowledge the significance of the trust\n");
                sb.append("- Express commitment to being worthy of it\n");
                break;
        }

        // Personality-based adjustments
        PersonalityProfile personality = memory.getPersonality();

        if (personality.humor > 70) {
            sb.append("- Include a light joke or playful comment\n");
        }

        if (personality.extraversion > 70) {
            sb.append("- Be enthusiastic and energetic\n");
        } else if (personality.extraversion < 40) {
            sb.append("- Be sincere but understated\n");
        }

        if (memory.getRapportLevel() > 70) {
            sb.append("- You're close friends - show genuine warmth\n");
        } else if (memory.getRapportLevel() < 30) {
            sb.append("- Keep it friendly but professional\n");
        }

        return sb.toString();
    }
}
```

### Anniversary Reflection Prompt

```java
public static String buildAnniversaryReflectionPrompt(
    CompanionMemory memory,
    Anniversary anniversary
) {
    return String.format("""
        # Anniversary Reflection

        Today marks %s since you first met %s!

        **Stats Since First Meeting:**
        - Days together: %d
        - Structures built: %d
        - Blocks mined: %d
        - Shared successes: %d

        **Most Significant Memories:**
        %s

        **Guidelines:**
        - Express nostalgia about your journey together
        - Reference specific memories (the "remember when" moments)
        - Show appreciation for the player's companionship
        - Look forward to future adventures

        Generate a heartfelt 2-3 sentence reflection:
        """,
        anniversary.getDisplayName(),
        memory.getPlayerName(),
        ChronoUnit.DAYS.between(memory.getFirstMeeting(), Instant.now()),
        memory.getStat("structures_built"),
        memory.getStat("blocks_mined"),
        memory.getStat("shared_successes"),
        formatTopMemories(memory.getEmotionalMemories(), 3)
    );
}
```

---

## Implementation Guide

### Phase 1: Core Milestone Infrastructure

**Step 1: Create Milestone Classes**

```java
// src/main/java/com/minewright/ai/milestone/Milestone.java
public class Milestone {
    private final String id;
    private final MilestoneCategory category;
    private final String name;
    private final String description;
    private final Instant triggeredAt;
    private final Map<String, Object> metadata;
}

// src/main/java/com/minewright/ai/milestone/MilestoneCategory.java
public enum MilestoneCategory {
    FIRSTS,
    ANNIVERSARY,
    ACHIEVEMENT,
    AFFECTION,
    TRUST
}
```

**Step 2: Create Milestone Detector**

```java
// src/main/java/com/minewright/ai/milestone/MilestoneDetector.java
public class MilestoneDetector {
    private final List<MilestoneTrigger> triggers;
    private final Set<String> triggeredIds;

    public void check(Context context) {
        triggers.stream()
            .filter(t -> !triggeredIds.contains(t.getId()))
            .filter(t -> t.condition().test(context))
            .forEach(t -> {
                Milestone m = t.create(context);
                triggeredIds.add(t.getId());
                notifyMilestone(m);
            });
    }
}
```

**Step 3: Integrate with CompanionMemory**

```java
// Add to CompanionMemory.java
private final MilestoneRegistry milestoneRegistry;

public void recordMilestone(Milestone milestone) {
    milestoneRegistry.record(milestone);

    // Adjust rapport based on significance
    int adjustment = switch (milestone.getCategory()) {
        case FIRSTS -> 5;
        case ANNIVERSARY -> 3;
        case ACHIEVEMENT -> 2;
        case AFFECTION -> 4;
        case TRUST -> 3;
    };

    adjustRapport(adjustment);
}
```

### Phase 2: Register Triggers

```java
// src/main/java/com/minewright/ai/milestone/MilestoneTriggers.java
public class CoreMilestoneTriggers {

    public static List<MilestoneTrigger> getCoreTriggers() {
        return List.of(
            // Firsts
            firstMeeting(),
            firstBuild(),
            firstDiamond(),
            firstDeath(),

            // Anniversaries
            oneDayAnniversary(),
            oneWeekAnniversary(),
            oneMonthAnniversary(),
            hundredDaysAnniversary(),

            // Achievements
            tenStructures(),
            fiftyStructures(),
            hundredStructures(),

            // Affection
            firstInsideJoke(),
            firstTrustMoment()
        );
    }

    private static MilestoneTrigger firstMeeting() {
        return MilestoneTrigger.builder()
            .id("first_meeting")
            .category(MilestoneCategory.FIRSTS)
            .condition(ctx -> ctx.getInteractionCount() == 1)
            .dialogue("Nice to meet you! I'm MineWright, let's build something amazing!")
            .build();
    }

    private static MilestoneTrigger oneWeekAnniversary() {
        return MilestoneTrigger.builder()
            .id("one_week_anniversary")
            .category(MilestoneCategory.ANNIVERSARY)
            .condition(ctx -> {
                long days = ChronoUnit.DAYS.between(
                    ctx.getFirstMeeting(),
                    Instant.now()
                );
                return days == 7;
            })
            .dialogue("A whole week together! We've built quite the collection of memories.")
            .build();
    }
}
```

### Phase 3: Hook Into Action System

```java
// Modify ActionExecutor.java to track milestones
private final MilestoneDetector milestoneDetector;

private void executeTask(Task task) {
    // ... existing code ...

    // Check for milestones after action completion
    if (currentAction.isComplete()) {
        ActionResult result = currentAction.getResult();

        if (result.isSuccess()) {
            // Update stats
            incrementStat("tasks_completed");

            // Check milestones
            milestoneDetector.check(buildMilestoneContext());

            // Handle milestone celebrations
            milestoneDetector.getNextMilestone().ifPresent(this::celebrateMilestone);
        }
    }
}

private void celebrateMilestone(Milestone milestone) {
    // Generate dialogue
    String dialogue = MilestoneDialogueGenerator.generate(
        milestone,
        minewright.getCompanionMemory()
    );

    // Send to GUI
    sendToGUI(minewright.getMineWrightName(), dialogue);

    // Record in memory
    minewright.getCompanionMemory().recordMilestone(milestone);
}
```

### Phase 4: Add Celebration Variations

```java
// src/main/java/com/minewright/ai/milestone/MilestoneDialogueGenerator.java
public class MilestoneDialogueGenerator {

    public static String generate(Milestone milestone, CompanionMemory memory) {
        return switch (milestone.getCategory()) {
            case FIRSTS -> generateFirstMilestone(milestone, memory);
            case ANNIVERSARY -> generateAnniversary(milestone, memory);
            case ACHIEVEMENT -> generateAchievement(milestone, memory);
            case AFFECTION -> generateAffection(milestone, memory);
            case TRUST -> generateTrust(milestone, memory);
        };
    }

    private static String generateAnniversary(Milestone milestone,
                                               CompanionMemory memory) {
        long days = (long) milestone.getMetadata().get("days");

        List<String> templates = List.of(
            "Can you believe it's been %d days? Time flies!",
            "%d days of adventures! Here's to the next %d.",
            "It's been %d days since we met. I've enjoyed every one."
        );

        String template = templates.get(
            ThreadLocalRandom.current().nextInt(templates.size())
        );

        return String.format(template, days, days);
    }
}
```

---

## Code Examples

### Complete Milestone Trigger System

```java
// src/main/java/com/minewright/ai/milestone/trigger/FirstBuildTrigger.java
package com.minewright.milestone.trigger;

import com.minewright.milestone.*;
import com.minewright.memory.CompanionMemory;
import java.time.Instant;
import java.util.Map;

public class FirstBuildTrigger implements MilestoneTrigger {

    @Override
    public String getId() {
        return "first_build";
    }

    @Override
    public MilestoneCategory getCategory() {
        return MilestoneCategory.FIRSTS;
    }

    @Override
    public boolean test(MilestoneContext context) {
        CompanionMemory memory = context.getMemory();
        Integer structuresBuilt = memory.getStat("structures_built");

        return structuresBuilt != null && structuresBuilt == 1;
    }

    @Override
    public Milestone create(MilestoneContext context) {
        return new Milestone(
            getId(),
            getCategory(),
            "First Build",
            "Completed the first structure together",
            Instant.now(),
            Map.of(
                "structure_type", context.getRecentActionType(),
                "rapport_at_time", context.getMemory().getRapportLevel()
            )
        );
    }

    @Override
    public String generateDialogue(Milestone milestone, CompanionMemory memory) {
        String structureType = (String) milestone.getMetadata().get("structure_type");

        if (memory.getPersonality().humor > 60) {
            return String.format(
                "Our first build! It's a %s. It's... beautiful in its own special way. " +
                "We'll only get better from here!",
                structureType
            );
        }

        return String.format(
            "Our first structure together! This %s represents the beginning of " +
            "our building journey. I'm excited to see what we create next.",
            structureType
        );
    }
}
```

### Stat Tracking Integration

```java
// Add to CompanionMemory.java
private final Map<String, AtomicInteger> stats = new ConcurrentHashMap<>();

public void incrementStat(String statName) {
    stats.computeIfAbsent(statName, k -> new AtomicInteger(0)).incrementAndGet();
}

public void incrementStat(String statName, int amount) {
    stats.computeIfAbsent(statName, k -> new AtomicInteger(0)).addAndGet(amount);
}

public Integer getStat(String statName) {
    AtomicInteger stat = stats.get(statName);
    return stat != null ? stat.get() : null;
}

// Usage in action completion
public void recordSharedSuccess(String taskDescription) {
    recordExperience("success", taskDescription, 5);
    adjustRapport(2);
    adjustTrust(3);
    incrementStat("shared_successes");
}
```

### Anniversary Check System

```java
// src/main/java/com/minewright/ai/milestone/AnniversaryChecker.java
package com.minewright.milestone;

import com.minewright.memory.CompanionMemory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class AnniversaryChecker {

    private static final List<AnniversaryMilestone> ANNIVERSARIES = List.of(
        new AnniversaryMilestone(1, "one_day", "1 day together!"),
        new AnniversaryMilestone(7, "one_week", "A whole week!"),
        new AnniversaryMilestone(30, "one_month", "One month today!"),
        new AnniversaryMilestone(100, "hundred_days", "100 days!"),
        new AnniversaryMilestone(365, "one_year", "A whole year!")
    );

    public static List<Milestone> checkAnniversaries(CompanionMemory memory) {
        if (memory.getFirstMeeting() == null) {
            return List.of();
        }

        List<Milestone> milestones = new ArrayList<>();
        Instant now = Instant.now();
        long daysSince = ChronoUnit.DAYS.between(memory.getFirstMeeting(), now);

        for (AnniversaryMilestone anniversary : ANNIVERSARIES) {
            if (daysSince == anniversary.days()) {
                // Check if already celebrated
                if (!memory.hasMilestone(anniversary.id())) {
                    milestones.add(createAnniversary(anniversary, memory, now));
                }
            }
        }

        return milestones;
    }

    private static Milestone createAnniversary(AnniversaryMilestone anniversary,
                                                CompanionMemory memory,
                                                Instant now) {
        return new Milestone(
            anniversary.id(),
            MilestoneCategory.ANNIVERSARY,
            anniversary.displayName(),
            String.format("Celebrating %d days together", anniversary.days()),
            now,
            Map.of(
                "days", anniversary.days(),
                "rapport_at_time", memory.getRapportLevel(),
                "interactions", memory.getInteractionCount()
            )
        );
    }

    private record AnniversaryMilestone(int days, String id, String displayName) {}
}
```

---

## Best Practices

### DO: Make Milestones Feel Earned

```
✅ Good: "100 structures! We've built everything from humble cobblestone shacks
    to this castle. I'm proud of what we've accomplished together."

❌ Bad: "You clicked 100 times! Good job!"
```

### DO: Reference Shared History

```
✅ Good: "Remember our first build? That tiny dirt house. Look at us now,
    building skyscrapers. We've come so far."

❌ Bad: "You have reached level 10."
```

### DO: Scale Emotional Intensity

```
Early relationship (0-30 rapport): Friendly, professional
Mid relationship (30-70 rapport): Warm, conversational
Late relationship (70-100 rapport): Deep affection, vulnerability
```

### DON'T: Be Creepy With Date Tracking

```
❌ Bad: "I've been watching you for exactly 72 hours, 14 minutes, and 33 seconds."

✅ Good: "Can you believe it's been three days? Time flies when you're having fun!"
```

### DON'T: Over-Celebrate

```
❌ Bad: Celebrating every single minor action
✅ Good: Meaningful milestones at appropriate intervals
```

### DON'T: Break Character

```
All milestone dialogue should reflect MineWright's established personality,
regardless of the milestone being celebrated.
```

---

## Testing Checklist

- [ ] First meeting triggers correctly
- [ ] Anniversary checks work across server restarts
- [ ] Achievement counters persist properly
- [ ] Dialogue varies based on personality settings
- [ ] Milestones don't trigger multiple times
- [ ] Cooldowns prevent spam
- [ ] High rapport celebrations feel warmer than low rapport
- [ ] "Remember when" references actual memories
- [ ] NBT saving/loading preserves milestone state
- [ ] Multiple milestone celebrations don't queue up excessively

---

## Future Enhancements

1. **Player-Initiated Milestones**: Let players define their own milestones
2. **Seasonal Events**: Special anniversary celebrations for in-game seasons
3. **Shared Milestones**: Celebrate when multiple MineWrights reach milestones together
4. **Milestone Visualizations**: In-game commemorative items or displays
5. **Memory Albums**: Collectible "photos" of significant moments
6. **Legacy System**: Long-term milestone trees spanning months of play

---

**Research Sources:**
- [Fire Emblem Support System - Polygon](https://www.polygon.com/games/2019/10/18/20899708/fire-emblem-three-houses-support-system-best)
- [Stardew Valley Friendship System](https://www.stardewvalleywiki.com/Friendship)
- [Animal Crossing Friendship Guide](https://www.thegamer.com/animal-crossing-new-horizons-friendship-guide/)
- [Baldur's Gate 3 Companion Relationships](https://www.ign.com/wikis/baldurs-gate-3/Companions)
- [Persona 5 Confidant Guide](https://www.ign.com/wikis/persona-5/Confidants)
- [Nintendo Switch Year in Review](https://www.nintendo.com/switch/year-in-review)
