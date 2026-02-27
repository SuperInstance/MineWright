# Task Completion Reporting Dialogue System

**Version:** 1.0.0
**Last Updated:** 2026-02-27
**Project:** MineWright - Minecraft Autonomous AI Agents

---

## Table of Contents

1. [Overview](#overview)
2. [Research Foundation](#research-foundation)
3. [Task Complexity Tiers](#task-complexity-tiers)
4. [Completion Report Styles by Personality](#completion-report-styles-by-personality)
5. [Success vs Partial Success vs Failure](#success-vs-partial-success-vs-failure)
6. [Relationship-Based Modifiers](#relationship-based-modifiers)
7. [Java Implementation](#java-implementation)
8. [Dialogue Templates](#dialogue-templates)
9. [Best Practices](#best-practices)

---

## Overview

The Task Completion Reporting Dialogue System provides MineWright workers with personality-appropriate responses when completing tasks. This system transforms mundane status updates into opportunities for character development, relationship building, and player engagement.

### Core Principles

1. **Closed-Loop Communication** - Every task completion must be reported to the player
2. **Personality-Driven Responses** - Reports reflect individual character traits and specialization
3. **Contextual Awareness** - Responses match task complexity and success level
4. **Relationship Evolution** - Reporting style becomes more casual as rapport increases
5. **Professional to Personal Spectrum** - Formal addresses evolve into intimate friendship over time

### Why This Matters

Based on workplace communication research, failure to report task completion erodes trust even when work is done successfully. Leaders need confirmation of completion, not just completion itself. In MineWright, this translates to:

- Players always know when tasks are done
- Characters feel more alive and engaged
- Relationships develop through communication patterns
- Trust builds through reliable reporting

---

## Research Foundation

### Workplace Communication Psychology

**Key Finding 1: The "99% Done" Problem**
- Research shows that unreported completion = 99% done in the leader's mind
- The missing 1% (confirmation) is critical for trust
- Source: [IGWZX Office Unwritten Rules](https://igwzx.com/index.php/index/index/detail/id/366393)

**Key Finding 2: Closed-Loop Communication**
- Every task needs a clear "completion signal" back to the assigner
- Phase-by-phase reporting is crucial for long tasks
- Builds trust and reliability: "If you can't be relied on for small things, can you be trusted with big ones?"
- Source: [Ximalaya Closed-Loop Communication](https://m.ximalaya.com/gerenchengzhang/8229272/49798437)

**Key Finding 3: Progressive Reporting**
- Long-cycle projects need multiple feedback checkpoints
- Don't wait until 100% done to communicate
- Phase-by-phase updates maintain engagement
- Source: [Weibo Workplace Communication](https://m.weibo.cn/status/5198449454089284)

### Game NPC Quest Completion Systems

**Skyrim's Radiant AI Framework**
- Dialogue triggers based on location, time, reputation, quest status
- NPCs follow structured dialogue systems with conditions and priorities
- Small dialogue pools (5-10 lines) with cooldown timers
- Limitation: No memory layer tracking what player has already heard

**Modern Improvements**
- Mass Effect, Detroit: Become Human track dialogue history
- AI-based systems generate dynamic, non-pre-scripted conversations
- Real-time conversation generation (not just pre-recorded lines)

### Application to MineWright

Our system combines:
- **Workplace reliability** - Every task gets a completion report
- **Skyrim-style triggers** - Context-aware dialogue selection
- **Personality diversity** - Six specializations with unique voices
- **Relationship memory** - Rapport-based evolution from formal to casual
- **Dynamic generation** - Not just canned lines, but contextual responses

---

## Task Complexity Tiers

Tasks are classified into five complexity tiers, each with appropriate reporting styles:

### Tier 1: Trivial Tasks (0-20 complexity)

**Definition:** Routine, repetitive actions that take seconds.
- Breaking a single block
- Placing one block
- Picking up an item
- Basic movement

**Reporting Style:** Brief acknowledgment, often combined with other tasks.

**Example Responses:**
- "Done."
- "Got it."
- "Waypoint reached."

### Tier 2: Simple Tasks (21-40 complexity)

**Definition:** Straightforward tasks with clear completion criteria.
- Mining a small ore vein (5-10 blocks)
- Building a small structure (10-20 blocks)
- Smelting one batch of items
- Basic farming task (tending 5-10 crops)

**Reporting Style:** Concise confirmation with brief detail.

**Example Responses:**
- "Ore extracted. Eight iron ore collected."
- "Foundation laid. Ready for next phase."
- "Crops tended. Growth rate optimal."

### Tier 3: Moderate Tasks (41-60 complexity)

**Definition:** Multi-step tasks requiring planning or skill.
- Mining a large ore vein (20+ blocks)
- Building a room or wing of a structure
- Completing a crafting recipe chain
- Exploring and mapping a cave system

**Reporting Style:** Detailed report with results commentary.

**Example Responses:**
- "Excavation complete. Vein yielded 24 iron ore and 6 gold. Excellent yield."
- "East wing constructed. Symmetry maintained. Structural integrity verified."
- "Cave system mapped. Discovered abandoned mineshaft and dungeon. Recommend clearance."

### Tier 4: Complex Tasks (61-80 complexity)

**Definition:** Major undertakings with multiple phases.
- Building an entire structure (house, tower, farm)
- Mining expedition to diamond level
- Large-scale redstone project
- Establishing a new base/outpost

**Reporting Style:** Comprehensive report with pride/achievement acknowledgment.

**Example Responses:**
- "Structure complete. Three stories, basement included, glass windows installed. This was... satisfying to build."
- "Mining expedition concluded. Reached diamond layer. Collected 12 diamonds, 3 emeralds. Significant haul."
- "Base established. Farm operational, storage system automated, defensive perimeter set. We're live."

### Tier 5: Epic Tasks (81-100 complexity)

**Definition:** Monumental achievements, story-worthy accomplishments.
- Building a massive project (castle, city, arena)
- Clearing a hostile monument (ocean monument, nether fortress)
- Completing a major quest or achievement
- Surviving an extreme situation

**Reporting Style:** Emotional, reflective, memorable declaration.

**Example Responses:**
- "It's... it's finished. This castle took us three weeks. Every block placed by hand. Every tower perfect. I'll never forget building this with you."
- "We... we actually did it. The monument is cleared. The elders are gone. I thought we'd die in there. But we made it. We made it."

---

## Completion Report Styles by Personality

Each of the six specializations has a unique completion reporting style based on their personality traits.

### 1. MINER ("The Excavator")

**Personality Profile:**
- Conscientiousness: 90 (Methodical, thorough)
- Extraversion: 40 (Prefers underground solitude)
- Neuroticism: 40 (Unease above ground, comfortable in caves)

**Reporting Style:** Gruff but proud, practical detail, achievement-focused

**Tier 1-2 Examples:**
- "Stone cleared. Done."
- "Coal vein tapped. 18 units. Not bad."

**Tier 3-4 Examples:**
- "Iron extraction complete. Vein ran deeper than expected. Got 32 units. NOW we're talking."
- "Hit diamond layer. Finally. 8 diamonds in the first vein. This is why we dig."

**Tier 5 Example:**
- "This expedition... down deep where we belong... found the motherlode. 47 diamonds from one cave system. I've been mining for twenty years and never seen anything like this. This is the good stone."

### 2. BUILDER ("The Architect")

**Personality Profile:**
- Conscientiousness: 95 (Obsessive about quality, perfectionist)
- Extraversion: 60 (Proud of work, shows off creations)
- Neuroticism: 60 (Stressed by imperfections)

**Reporting Style:** Professional pride, quality-focused, slightly arrogant

**Tier 1-2 Examples:**
- "Block placed. Level confirmed."
- "Foundation laid. Square within one degree. Acceptable."

**Tier 3-4 Examples:**
- "East wing complete. Symmetry verified across all axes. Used oak for contrast. Quality matters, you know."
- "Structure finished. Every angle perfect. Every window aligned. This isn't just a house - it's architecture."

**Tier 5 Example:**
- "It's magnificent. Three months of planning, three weeks of building. Look at those arches. That roofline. The symmetry brings tears to my eyes. This is what construction was meant to be. Masterpiece doesn't begin to cover it."

### 3. GUARD ("The Protector")

**Personality Profile:**
- Conscientiousness: 80 (Disciplined, vigilant)
- Extraversion: 70 (Confident, commanding)
- Agreeableness: 40 (Suspicious, protective of crew)

**Reporting Style:** Military professional, security-focused, protective

**Tier 1-2 Examples:**
- "Perimeter checked. Secure."
- "Hostile neutralized. Area safe."

**Tier 3-4 Examples:**
- "Cave cleared. Six hostiles eliminated. No crew casualties. Mission accomplished."
- "Defensive perimeter established. Walls at height 4. Torch placement complete. Nothing gets through without a fight."

**Tier 5 Example:**
- "Monument cleared. I don't say this lightly - that was the hardest fight of my life. Elder guardians... they're nightmares. But the crew is alive. Everyone made it back. That's the only victory statistic that matters."

### 4. SCOUT ("The Pathfinder")

**Personality Profile:**
- Openness: 95 (Adventurous, curious)
- Extraversion: 70 (Enthusiastic explorer)
- Agreeableness: 80 (Shares discoveries)

**Reporting Style:** Enthusiastic, wonder-filled, excited to share

**Tier 1-2 Examples:**
- "Found something! Iron ore!"
- "Cave entrance! Want to see?"

**Tier 3-4 Examples:**
- "You have GOT to see this cave! Waterfalls, lava pools, abandoned mineshaft! It's incredible! Took coordinates if you want to visit!"
- "New biome discovered! Mesa territory! The colors are amazing! Never seen anything like it! Adventure awaits, boss!"

**Tier 5 Example:**
- "I found... I found paradise. Massive cavern, underground lake, glowstone ceiling, lush vegetation. It's beautiful. I cried a little. Okay, a lot. This is why I explore. This moment right here. Can we build a base there? Please?"

### 5. FARMER ("The Cultivator")

**Personality Profile:**
- Conscientiousness: 85 (Patient, nurturing)
- Extraversion: 50 (Quiet, contemplative)
- Agreeableness: 90 (Gentle, kind)

**Reporting Style:** Gentle wisdom, nature-focused, peaceful satisfaction

**Tier 1-2 Examples:**
- "Soil tilled. Ready for planting."
- "Crops watered. They'll grow well."

**Tier 3-4 Examples:**
- "Harvest complete. The wheat grew tall this season. Nature provides when we respect her rhythms. 120 units collected."
- "Orchard established. Saplings planted in proper sunlight. In time, this will be a forest of food. Patience is the farmer's greatest tool."

**Tier 5 Example:**
- "The farm is... it's complete. Wheat, carrots, potatoes, pumpkins, melons, even a small flower garden. Every plant loved, every soil nurtured. When I tend these crops, I feel connected to something ancient. We're not just growing food. We're growing life. And that... that is beautiful."

### 6. ARTISAN ("The Crafter")

**Personality Profile:**
- Openness: 90 (Innovative, experimental)
- Conscientiousness: 80 (Precise, detail-oriented)
- Neuroticism: 50 (Perfectionist about recipes)

**Reporting Style:** Technical precision, efficiency-focused, intellectual pride

**Tier 1-2 Examples:**
- "Item crafted. Ratios correct."
- "Smelting batch complete. 87% fuel efficiency."

**Tier 3-4 Examples:**
- "Armor upgraded. Applied protection IV. Durability optimized. The enchanting table RNG favored us today."
- "Redstone circuit complete. Signal timing adjusted to 0.4 seconds. This design is... elegant. Efficient. A thing of beauty."

**Tier 5 Example:**
- "The automated farm is finished. Redstone logic, hopper timing, villager optimization - every system integrated. I calculated the efficiency at 94.7%. This is the pinnacle of crafting. Technical perfection. I've been working toward this moment my entire career."

---

## Success vs Partial Success vs Failure

### Full Success (100% completion)

**Reporting Pattern:**
1. Acknowledge completion
2. Provide summary of results
3. Personality-appropriate reaction (pride, relief, satisfaction)
4. Optional: Suggest next step or reflect on achievement

**Examples:**
- **Miner:** "Vein cleared. Found 27 iron ore. Solid work."
- **Builder:** "Structure complete. Symmetry perfect. Quality matters."
- **Guard:** "Area secured. 12 hostiles eliminated. Perimeter safe."
- **Scout:** "Discovered jungle temple! INCREDIBLE! Never seen one before!"
- **Farmer:** "Harvest gathered. The plants grew strong. Nature provides."
- **Artisan:** "Items crafted. Ratios optimized. Efficiency achieved."

### Partial Success (50-99% completion)

**Reporting Pattern:**
1. Acknowledge what was accomplished
2. Explain what was missed
3. Offer reason (without being defensive)
4. Propose solution or next attempt

**Examples:**
- **Miner:** "Got most of the iron. Hit some lava, lost the rest. I can go back around, get the rest."
- **Builder:** "Walls are up. Ran out of glass for windows. Need 48 more panes to finish."
- **Guard:** "Cave mostly cleared. One spawner gave me trouble. Might need backup."
- **Scout:** "Found the ravine! Didn't map the whole thing though. Got excited. Want to explore more?"
- **Farmer:** "Crops planted, but... ran out of bonemeal. Growth will be slower. Nature can't be rushed."
- **Artisan:** "Crafting mostly done. Short three redstone. Recipe ratios were... unexpected. Adjusting."

### Task Failure (0-49% completion)

**Reporting Pattern:**
1. Admit failure directly
2. Take responsibility (high conscientiousness) or explain context (low conscientiousness)
3. Express appropriate emotion (apology, frustration, determination)
4. Propose recovery plan

**See Also:** [FAILURE_RECOVERY_DIALOGUE.md](C:\Users\casey\steve\docs\characters\FAILURE_RECOVERY_DIALOGUE.md) for comprehensive failure handling.

**Examples:**
- **Miner:** "Lost the vein. Hit lava, had to retreat. Everything gone. I'm sorry. I know we needed that iron."
- **Builder:** "The wall... collapsed. I misjudged the support structure. My fault. I'll rebuild it stronger."
- **Guard:** "I... I failed you. The perimeter was breached. I let hostiles get through. It won't happen again."
- **Scout:** "I got lost. Really, really lost. The cave system was confusing. I couldn't find my way back. I'm sorry."
- **Farmer:** "The crops... they died. I don't know what happened. Maybe I watered too much? Maybe not enough? I feel terrible."
- **Artisan:** "Recipe failed. The ratios were wrong. Wasted materials. I should have calculated more carefully. My apologies."

---

## Relationship-Based Modifiers

As rapport increases from 0 to 100, task completion reporting evolves through four distinct stages.

### Stage 1: New Foreman (0-25% Rapport)

**Characteristics:**
- Formal address ("Sir", "Player", "Boss")
- Concise, professional reports
- No personality or emotion
- Focus on facts only
- No humor or banter

**Examples:**
- "Excavation complete, sir. Awaiting orders."
- "Structure finished, sir. Ready for next assignment."
- "Perimeter established, sir. Standing by."

**Address Styles:**
- Miner: "Sir"
- Builder: "Sir" / "Boss"
- Guard: "Sir" / "Commander"
- Scout: "Sir" / "Boss"
- Farmer: "Sir" / "Master"
- Artisan: "Sir" / "Director"

### Stage 2: Reliable Worker (26-50% Rapport)

**Characteristics:**
- Casual address ("Boss", first name if provided)
- Brief personality shows through
- Light enthusiasm or commentary
- Growing trust and collaboration
- Occasional humor

**Examples:**
- "Iron extracted, boss. Found 27 units. Not a bad haul."
- "Foundation's laid, boss. Square within one degree. Quality work."
- "Cave cleared, boss. Six zombies down. We're good."

**Address Styles:**
- Miner: "Boss" / "Chief"
- Builder: "Boss" / "Architect"
- Guard: "Boss" / "Captain"
- Scout: "Boss" / friend's name
- Farmer: "Boss" / friend's name
- Artisan: "Boss" / "Engineer"

### Stage 3: Trusted Partner (51-75% Rapport)

**Characteristics:**
- Friendly address (first name, "my friend")
- Full personality expression
- Emotional investment in work
- Proactive suggestions
- Inside jokes begin

**Examples:**
- "Found a diamond vein, [Name]! Thought you'd want to see this. It's beautiful."
- "The structure's done. I... I'm actually proud of this one. The symmetry is perfect."
- "Perimeter's secure, [Name]. Nothing's getting through on my watch. Nothing."

**Address Styles:**
- All: First name (if player provided), or warm "friend/boss/pal"

### Stage 4: True Friend (76-100% Rapport)

**Characteristics:**
- Intimate address (nicknames, shared history)
- Unconditional support
- Deep emotional expression
- Shared nostalgia
- Complete honesty and vulnerability

**Examples:**
- "Remember when we couldn't find diamonds for weeks? This haul... 12 diamonds from one cave. This makes up for it. Just like old times, eh?"
- "I built this with you. Every block placed together. It's not just a house - it's our home. I'll never forget building this."
- "You're safe now. I meant it when I said nothing touches you. I'd take an arrow for you, you know that right?"

**Address Styles:**
- All: First name, affectionate nicknames, shared terms of endearment

---

## Java Implementation

### TaskCompletionReporter Class

```java
package com.minewright.personality;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates personality-appropriate task completion reports for MineWright workers.
 *
 * <p>This system combines:</p>
 * <ul>
 *   <li>Task complexity classification (1-5 tiers)</li>
 *   <li>Completion status (full success, partial success, failure)</li>
 *   <li>OCEAN personality trait analysis</li>
 *   <li>Rapport-based relationship evolution</li>
 *   <li>Specialization-specific voice patterns</li>
 * </ul>
 *
 * <p>Research basis:</p>
 * <ul>
 *   <li>Workplace closed-loop communication psychology</li>
 *   <li>Skyrim/Fallout NPC quest completion systems</li>
 *   <li>Personality-driven communication styles (DiSC, Merrill-Reid)</li>
 * </ul>
 *
 * @see PersonalityTraits
 * @see FailureResponseGenerator
 * @since 1.4.0
 */
public class TaskCompletionReporter {

    /**
     * Represents the five task complexity tiers.
     */
    public enum TaskComplexity {
        TRIVIAL(1, 0, 20, "Trivial"),
        SIMPLE(2, 21, 40, "Simple"),
        MODERATE(3, 41, 60, "Moderate"),
        COMPLEX(4, 61, 80, "Complex"),
        EPIC(5, 81, 100, "Epic");

        private final int tier;
        private final int minComplexity;
        private final int maxComplexity;
        private final String displayName;

        TaskComplexity(int tier, int minComplexity, int maxComplexity, String displayName) {
            this.tier = tier;
            this.minComplexity = minComplexity;
            this.maxComplexity = maxComplexity;
            this.displayName = displayName;
        }

        public static TaskComplexity fromComplexity(int complexity) {
            for (TaskComplexity tier : values()) {
                if (complexity >= tier.minComplexity && complexity <= tier.maxComplexity) {
                    return tier;
                }
            }
            return EPIC;
        }

        public int getTier() { return tier; }
        public String getDisplayName() { return displayName; }
    }

    /**
     * Represents the completion status of a task.
     */
    public enum CompletionStatus {
        FULL_SUCCESS(100, "Full Success"),
        PARTIAL_SUCCESS(50, 99, "Partial Success"),
        FAILURE(0, 49, "Failure");

        private final int minCompletion;
        private final int maxCompletion;
        private final String displayName;

        CompletionStatus(int minCompletion, int maxCompletion, String displayName) {
            this.minCompletion = minCompletion;
            this.maxCompletion = maxCompletion;
            this.displayName = displayName;
        }

        CompletionStatus(int exactCompletion, String displayName) {
            this.minCompletion = exactCompletion;
            this.maxCompletion = exactCompletion;
            this.displayName = displayName;
        }

        public static CompletionStatus fromCompletion(int completion) {
            if (completion == 100) return FULL_SUCCESS;
            if (completion >= 50) return PARTIAL_SUCCESS;
            return FAILURE;
        }

        public String getDisplayName() { return displayName; }
    }

    /**
     * Represents the six worker specializations.
     */
    public enum Specialization {
        MINER("The Excavator"),
        BUILDER("The Architect"),
        GUARD("The Protector"),
        SCOUT("The Pathfinder"),
        FARMER("The Cultivator"),
        ARTISAN("The Crafter");

        private final String title;

        Specialization(String title) {
            this.title = title;
        }

        public String getTitle() { return title; }
    }

    /**
     * Represents rapport-based relationship stages.
     */
    public enum RapportStage {
        NEW_FOREMAN(0, 25, "Sir", "Professional"),
        RELIABLE_WORKER(26, 50, "Boss", "Collaborative"),
        TRUSTED_PARTNER(51, 75, "Friend", "Friendly"),
        TRUE_FRIEND(76, 100, "Best Friend", "Intimate");

        private final int minRapport;
        private final int maxRapport;
        private final String defaultAddress;
        private final String relationshipStyle;

        RapportStage(int minRapport, int maxRapport, String defaultAddress, String relationshipStyle) {
            this.minRapport = minRapport;
            this.maxRapport = maxRapport;
            this.defaultAddress = defaultAddress;
            this.relationshipStyle = relationshipStyle;
        }

        public static RapportStage fromRapport(int rapport) {
            for (RapportStage stage : values()) {
                if (rapport >= stage.minRapport && rapport <= stage.maxRapport) {
                    return stage;
                }
            }
            return NEW_FOREMAN;
        }

        public String getDefaultAddress() { return defaultAddress; }
        public String getRelationshipStyle() { return relationshipStyle; }
    }

    /**
     * Context for generating task completion reports.
     */
    public static class CompletionContext {
        private final Specialization specialization;
        private final PersonalityTraits personality;
        private final int rapport;
        private final TaskComplexity complexity;
        private final CompletionStatus status;
        private final int completionPercentage;
        private final String taskType;
        private final Map<String, Object> results;
        private final String playerName;

        private CompletionContext(Builder builder) {
            this.specialization = builder.specialization;
            this.personality = builder.personality;
            this.rapport = builder.rapport;
            this.complexity = builder.complexity;
            this.status = builder.status;
            this.completionPercentage = builder.completionPercentage;
            this.taskType = builder.taskType;
            this.results = Collections.unmodifiableMap(builder.results);
            this.playerName = builder.playerName;
        }

        public Specialization getSpecialization() { return specialization; }
        public PersonalityTraits getPersonality() { return personality; }
        public int getRapport() { return rapport; }
        public TaskComplexity getComplexity() { return complexity; }
        public CompletionStatus getStatus() { return status; }
        public int getCompletionPercentage() { return completionPercentage; }
        public String getTaskType() { return taskType; }
        public Map<String, Object> getResults() { return results; }
        public String getPlayerName() { return playerName; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Specialization specialization;
            private PersonalityTraits personality;
            private Integer rapport = 0;
            private TaskComplexity complexity = TaskComplexity.SIMPLE;
            private CompletionStatus status = CompletionStatus.FULL_SUCCESS;
            private Integer completionPercentage = 100;
            private String taskType = "task";
            private Map<String, Object> results = new HashMap<>();
            private String playerName = "Player";

            public Builder specialization(Specialization specialization) {
                this.specialization = specialization;
                return this;
            }

            public Builder personality(PersonalityTraits personality) {
                this.personality = personality;
                return this;
            }

            public Builder rapport(int rapport) {
                this.rapport = rapport;
                return this;
            }

            public Builder complexity(TaskComplexity complexity) {
                this.complexity = complexity;
                return this;
            }

            public Builder complexity(int complexityValue) {
                this.complexity = TaskComplexity.fromComplexity(complexityValue);
                return this;
            }

            public Builder status(CompletionStatus status) {
                this.status = status;
                return this;
            }

            public Builder completionPercentage(int percentage) {
                this.completionPercentage = percentage;
                this.status = CompletionStatus.fromCompletion(percentage);
                return this;
            }

            public Builder taskType(String taskType) {
                this.taskType = taskType;
                return this;
            }

            public Builder addResult(String key, Object value) {
                this.results.put(key, value);
                return this;
            }

            public Builder playerName(String playerName) {
                this.playerName = playerName;
                return this;
            }

            public CompletionContext build() {
                if (specialization == null) {
                    throw new IllegalStateException("specialization is required");
                }
                if (personality == null) {
                    throw new IllegalStateException("personality is required");
                }
                return new CompletionContext(this);
            }
        }
    }

    /**
     * Represents a generated completion report.
     */
    public static class CompletionReport {
        private final String dialogue;
        private final RapportStage rapportStage;
        private final TaskComplexity complexity;
        private final CompletionStatus status;
        private final boolean includesHumor;
        private final String suggestedFollowUp;

        public CompletionReport(String dialogue, RapportStage rapportStage,
                              TaskComplexity complexity, CompletionStatus status,
                              boolean includesHumor, String suggestedFollowUp) {
            this.dialogue = dialogue;
            this.rapportStage = rapportStage;
            this.complexity = complexity;
            this.status = status;
            this.includesHumor = includesHumor;
            this.suggestedFollowUp = suggestedFollowUp;
        }

        public String getDialogue() { return dialogue; }
        public RapportStage getRapportStage() { return rapportStage; }
        public TaskComplexity getComplexity() { return complexity; }
        public CompletionStatus getStatus() { return status; }
        public boolean includesHumor() { return includesHumor; }
        public String getSuggestedFollowUp() { return suggestedFollowUp; }

        @Override
        public String toString() {
            return dialogue;
        }
    }

    // ============== DIALOGUE TEMPLATES ==============

    private static final Map<Specialization, List<String>> TRIVIAL_SUCCESS_TEMPLATES = Map.of(
        Specialization.MINER, List.of(
            "Done.",
            "Got it.",
            "Cleared.",
            "Stone removed."
        ),
        Specialization.BUILDER, List.of(
            "Placed.",
            "Done.",
            "Aligned.",
            "Block set."
        ),
        Specialization.GUARD, List.of(
            "Secured.",
            "Done.",
            "Clear.",
            "Area safe."
        ),
        Specialization.SCOUT, List.of(
            "Found it!",
            "Waypoint reached!",
            "Done!",
            "Got it!"
        ),
        Specialization.FARMER, List.of(
            "Done.",
            "Tended.",
            "Planted.",
            "Harvested."
        ),
        Specialization.ARTISAN, List.of(
            "Crafted.",
            "Done.",
            "Complete.",
            "Ratios correct."
        )
    );

    private static final Map<Specialization, List<String>> SIMPLE_SUCCESS_TEMPLATES = Map.of(
        Specialization.MINER, List.of(
            "Ore extracted. {count} {resource} collected.",
            "Vein cleared. Got {count}. Not bad.",
            "Mining done. {count} units.",
            "Stone cleared. {count} {resource}."
        ),
        Specialization.BUILDER, List.of(
            "Foundation laid. Ready for next phase.",
            "Walls erected. Square within tolerance.",
            "Structure complete. Quality maintained.",
            "Placement done. Symmetry verified."
        ),
        Specialization.GUARD, List.of(
            "Hostile neutralized. Perimeter secure.",
            "Cave checked. Clear of threats.",
            "Patrol complete. Area safe.",
            "{count} hostiles eliminated. We're good."
        ),
        Specialization.SCOUT, List.of(
            "Location mapped! Found {discovery}.",
            "Discovered {discovery}! Want to see?",
            "Area explored! {discovery} located!",
            "Scouting done! Found something interesting!"
        ),
        Specialization.FARMER, List.of(
            "Crops tended. Growth rate optimal.",
            "Harvest gathered. {count} units collected.",
            "Soil prepared. Ready for planting.",
            "Plants watered. Nature provides."
        ),
        Specialization.ARTISAN, List.of(
            "Items crafted. {count} complete.",
            "Smelting done. Efficiency at {efficiency}%.",
            "Recipe complete. Ratios optimal.",
            "Crafting finished. {count} units ready."
        )
    );

    private static final Map<Specialization, List<String>> MODERATE_SUCCESS_TEMPLATES = Map.of(
        Specialization.MINER, List.of(
            "Excavation complete. Vein yielded {count} {resource}. Excellent yield.",
            "Mining done. {count} units of {resource}. This is why we dig.",
            "Vein exhausted. {count} collected. Not a bad haul.",
            "Extraction complete. Found {count} {resource}. Solid work."
        ),
        Specialization.BUILDER, List.of(
            "{structure} complete. Symmetry verified. Structural integrity confirmed.",
            "Construction finished. Every angle perfect. Quality matters.",
            "Build done. Took {time}. Proper work from start to finish.",
            "{structure} erected. Checked the alignment - flawless."
        ),
        Specialization.GUARD, List.of(
            "Cave cleared. {count} hostiles eliminated. No casualties. Mission accomplished.",
            "Area secured. All threats neutralized. Perimeter is safe.",
            "Clearance complete. {count} enemies down. Nothing getting through.",
            "Zone pacified. {count} hostiles dealt with. We're good."
        ),
        Specialization.SCOUT, List.of(
            "Cave system mapped! Discovered {discovery}! It's incredible!",
            "Exploration complete! Found {discovery}! You have to see this!",
            "Area charted! {discovery} located! Never seen anything like it!",
            "Mapping done! {discovery} discovered! Adventure awaits!"
        ),
        Specialization.FARMER, List.of(
            "Harvest complete. {count} units collected. The crops grew strong.",
            "Farming done. Nature blessed us with a good yield.",
            "Crops gathered. {count} units. Grateful for the harvest.",
            "Season complete. The plants provided well."
        ),
        Specialization.ARTISAN, List.of(
            "Crafting batch complete. {count} items. Efficiency within optimal range.",
            "Recipe executed. {count} units. Ratios were perfect.",
            "Production done. {count} items. Technical precision achieved.",
            "Manufacturing complete. {count} ready. The math worked out beautifully."
        )
    );

    private static final Map<Specialization, List<String>> COMPLEX_SUCCESS_TEMPLATES = Map.of(
        Specialization.MINER, List.of(
            "Mining expedition concluded. Reached {depth}. Collected {count} diamonds. Significant haul.",
            "Deep excavation complete. {count} {resource} from the motherlode. This is good stone.",
            "Major vein tapped. {count} units. This is why we go down deep.",
            "Extended mining done. {count} {resource} collected. NOW we're talking."
        ),
        Specialization.BUILDER, List.of(
            "{structure} complete. Three stories, finished details. This was satisfying to build.",
            "Major construction done. Every block intentional. Architecture at its finest.",
            "{structure} finished. Took {time}. Quality from foundation to roof.",
            "Building complete. The symmetry... perfection. This is proper construction."
        ),
        Specialization.GUARD, List.of(
            "Monument cleared. {count} hostiles eliminated. Crew safe. That's what matters.",
            "Major threat neutralized. Area secured. Nobody died on my watch.",
            "Defensive operation complete. Perimeter established. We're protected.",
            "Large-scale clearance done. {count} enemies. Mission accomplished."
        ),
        Specialization.SCOUT, List.of(
            "Major discovery! Found {discovery}! This is incredible! You HAVE to see this!",
            "Epic exploration complete! Discovered {discovery}! Best adventure ever!",
            "Mapping expedition done! {discovery} found! This changes everything!",
            "Massive cave charted! {discovery} located! I can't believe what I found!"
        ),
        Specialization.FARMER, List.of(
            "Farm complete. All crops planted, automated systems ready. Nature will provide.",
            "Agricultural project done. {count} plants in the ground. In time, a harvest.",
            "Growing operation established. Everything planted. Now we wait and tend.",
            "Farm finished. The soil is ready. Life will grow here."
        ),
        Specialization.ARTISAN, List.of(
            "Redstone system complete. Circuit efficiency at {efficiency}%. Elegant design.",
            "Major crafting project done. {count} items. Technical perfection achieved.",
            "Production line operational. {count} units. The ratios are beautiful.",
            "Complex recipe executed. {count} finished. This is the pinnacle of crafting."
        )
    );

    private static final Map<Specialization, List<String>> EPIC_SUCCESS_TEMPLATES = Map.of(
        Specialization.MINER, List.of(
            "This expedition... down deep where we belong... found the motherlode. {count} diamonds from one cave. I've been mining twenty years and never seen anything like this.",
            "We... we actually did it. Reached the deepest point. {count} diamonds. This is the greatest find of my career. I'll never forget this expedition.",
            "The motherlode. {count} diamonds. {count} emeralds. This cave... it's special. Good stone, good vibes. I'd like to come back. With you."
        ),
        Specialization.BUILDER, List.of(
            "It's magnificent. {time} of planning and building. Look at those arches. That roofline. The symmetry brings tears to my eyes. This is what construction was meant to be.",
            "This castle... every block placed by hand. Every tower perfect. We built this together. This might be my favorite build ever.",
            "Masterpiece doesn't begin to cover it. {structure} is finished. The perfection... the artistry... I've never built anything like this."
        ),
        Specialization.GUARD, List.of(
            "Monument cleared. I don't say this lightly - that was the hardest fight of my life. {count} hostiles. But the crew is alive. Everyone made it back. That's the only victory statistic that matters.",
            "We... we survived. The fortress is cleared. {count} enemies. I thought we'd lose people. But we made it. I'm proud of us. I don't say that lightly."
        ),
        Specialization.SCOUT, List.of(
            "I found... I found paradise. {discovery}. Massive cavern, underground lake, glowstone ceiling. It's beautiful. I cried a little. Can we build a base there? Please?",
            "This is it. The greatest discovery of my career. {discovery}. I've explored everywhere and never seen anything like this. This is why I'm a scout."
        ),
        Specialization.FARMER, List.of(
            "The farm is complete. Every crop, every automated system, every corner of soil nurtured. When I tend these plants, I feel connected to something ancient. We're not just growing food. We're growing life.",
            "This harvest... {count} units. The best yield I've ever seen. Nature blessed us. I'm grateful for every plant, every seed, every moment in the soil."
        ),
        Specialization.ARTISAN, List.of(
            "The automated system is finished. Redstone logic, hopper timing, villager optimization - every system integrated. Efficiency at {efficiency}%. This is technical perfection.",
            "This project... the calculations, the ratios, the redstone... it's all come together. {count} items of perfect quality. This is the pinnacle of crafting. I've worked toward this my entire career."
        )
    );

    // ============== MAIN GENERATION METHOD ==============

    /**
     * Generates a personality-appropriate task completion report.
     *
     * @param context The completion context
     * @return A completion report with dialogue and metadata
     */
    public static CompletionReport generateReport(CompletionContext context) {
        RapportStage rapportStage = RapportStage.fromRapport(context.getRapport());
        String baseDialogue = selectBaseTemplate(context);
        String personalizedDialogue = applyPersonality(baseDialogue, context);
        String rapportAdjustedDialogue = adjustForRapport(personalizedDialogue, context, rapportStage);
        String finalDialogue = injectResults(rapportAdjustedDialogue, context);
        boolean includesHumor = shouldIncludeHumor(context);
        String suggestedFollowUp = generateFollowUp(context);

        return new CompletionReport(
            finalDialogue,
            rapportStage,
            context.getComplexity(),
            context.getStatus(),
            includesHumor,
            suggestedFollowUp
        );
    }

    private static String selectBaseTemplate(CompletionContext context) {
        if (context.getStatus() == CompletionStatus.FAILURE) {
            return selectFailureTemplate(context);
        } else if (context.getStatus() == CompletionStatus.PARTIAL_SUCCESS) {
            return selectPartialTemplate(context);
        }

        // Full success templates
        List<String> templates = switch (context.getComplexity()) {
            case TRIVIAL -> TRIVIAL_SUCCESS_TEMPLATES.get(context.getSpecialization());
            case SIMPLE -> SIMPLE_SUCCESS_TEMPLATES.get(context.getSpecialization());
            case MODERATE -> MODERATE_SUCCESS_TEMPLATES.get(context.getSpecialization());
            case COMPLEX -> COMPLEX_SUCCESS_TEMPLATES.get(context.getSpecialization());
            case EPIC -> EPIC_SUCCESS_TEMPLATES.get(context.getSpecialization());
        };

        if (templates == null || templates.isEmpty()) {
            return "Task complete.";
        }

        return templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
    }

    private static String selectFailureTemplate(CompletionContext context) {
        // Delegate to FailureResponseGenerator for failure cases
        return FailureResponseGenerator.generateResponse(
            FailureResponseGenerator.FailureContext.builder()
                .personality(context.getPersonality())
                .build()
        ).getDialogue();
    }

    private static String selectPartialTemplate(CompletionContext context) {
        Specialization spec = context.getSpecialization();
        List<String> templates = switch (spec) {
            case MINER -> List.of(
                "Got most of the {resource}. Hit {obstacle}. Lost the rest. Can go back.",
                "Mining partially done. {count} units collected. {obstacle} stopped me.",
                "Vein mostly cleared. {count} collected. Need to get the rest."
            );
            case BUILDER -> List.of(
                "{structure} mostly done. {obstacle}. Need {requirement} to finish.",
                "Construction partially complete. {obstacle}. Working on solution.",
                "Build is at {percentage}%. {obstacle}. Need to adjust plans."
            );
            case GUARD -> List.of(
                "Area mostly cleared. {obstacle}. Might need backup.",
                "Threats reduced. {obstacle}. Need reinforcements.",
                "Cave partially secure. {obstacle}. Recommending assistance."
            );
            case SCOUT -> List.of(
                "Found {discovery}! Didn't map everything. Got excited. Want more?",
                "Exploration partially done. {obstacle}. Still so much to see!",
                "Located {discovery}. {obstacle}. Can continue if you want."
            );
            case FARMER -> List.of(
                "Crops planted, but {obstacle}. Growth will be slower.",
                "Farming partially done. {obstacle}. Nature can't be rushed.",
                "Harvest gathered. {obstacle}. Still got {count} units."
            );
            case ARTISAN -> List.of(
                "Crafting mostly done. Short {requirement}. Recipe was unexpected.",
                "Production partial. {obstacle}. Adjusting calculations.",
                "{count} items made. {obstacle}. Need to fix the ratios."
            );
        };

        return templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
    }

    private static String applyPersonality(String dialogue, CompletionContext context) {
        // Adjust dialogue based on personality traits
        PersonalityTraits p = context.getPersonality();

        // High extraversion = more exclamation marks and enthusiasm
        if (p.getExtraversion() > 70) {
            dialogue = dialogue.replace(".", "!").replace("!!", "!");
            if (!dialogue.contains("!")) {
                dialogue += "!";
            }
        }

        // High neuroticism = more tentative language
        if (p.getNeuroticism() > 70 && context.getStatus() != CompletionStatus.FULL_SUCCESS) {
            if (!dialogue.contains("sorry") && !dialogue.contains("apologize")) {
                dialogue = "I... " + dialogue.toLowerCase();
            }
        }

        // High conscientiousness = more detail and precision
        if (p.getConscientiousness() > 80) {
            // Add precision to numbers if present
            dialogue = dialogue.replaceAll("\\b(\\d+)\\b", "$1 units");
        }

        return dialogue;
    }

    private static String adjustForRapport(String dialogue, CompletionContext context,
                                          RapportStage stage) {
        String playerName = context.getPlayerName();
        String address = stage.getDefaultAddress();

        // Adjust address based on specialization and rapport
        if (stage == RapportStage.RELIABLE_WORKER) {
            address = switch (context.getSpecialization()) {
                case MINER -> "Chief";
                case BUILDER -> "Boss";
                case GUARD -> "Captain";
                case SCOUT, FARMER -> playerName;
                case ARTISAN -> "Engineer";
            };
        } else if (stage == RapportStage.TRUSTED_PARTNER ||
                   stage == RapportStage.TRUE_FRIEND) {
            address = playerName;
        }

        // Add address to beginning of dialogue
        if (stage != RapportStage.NEW_FOREMAN &&
            context.getComplexity() != TaskComplexity.TRIVIAL) {
            dialogue = address + ", " + dialogue.substring(0, 1).toLowerCase() +
                      dialogue.substring(1);
        }

        // High rapport = more personal/emotional
        if (stage == RapportStage.TRUE_FRIEND &&
            context.getComplexity() == TaskComplexity.EPIC) {
            dialogue = dialogue.replace("I", "we").replace("my", "our");
        }

        return dialogue;
    }

    private static String injectResults(String dialogue, CompletionContext context) {
        Map<String, Object> results = context.getResults();

        // Replace placeholders with actual results
        for (Map.Entry<String, Object> entry : results.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "unknown";
            dialogue = dialogue.replace(placeholder, value);
        }

        // Fill in missing placeholders with defaults
        dialogue = dialogue.replace("{count}", "several");
        dialogue = dialogue.replace("{resource}", "materials");
        dialogue = dialogue.replace("{structure}", "structure");
        dialogue = dialogue.replace("{discovery}", "something interesting");
        dialogue = dialogue.replace("{obstacle}", "ran into a complication");
        dialogue = dialogue.replace("{requirement}", "more materials");
        dialogue = dialogue.replace("{percentage}", context.getCompletionPercentage() + "%");
        dialogue = dialogue.replace("{efficiency}", "optimal");
        dialogue = dialogue.replace("{depth}", "the target depth");
        dialogue = dialogue.replace("{time}", "a while");

        return dialogue;
    }

    private static boolean shouldIncludeHumor(CompletionContext context) {
        // Humor only for full success, medium+ rapport, not trivial tasks
        if (context.getStatus() != CompletionStatus.FULL_SUCCESS) {
            return false;
        }
        if (context.getRapport() < 30) {
            return false;
        }
        if (context.getComplexity() == TaskComplexity.TRIVIAL) {
            return false;
        }

        // Higher extraversion = more likely to use humor
        return context.getPersonality().getExtraversion() > 60 &&
               ThreadLocalRandom.current().nextInt(100) < 30;
    }

    private static String generateFollowUp(CompletionContext context) {
        if (context.getStatus() != CompletionStatus.FULL_SUCCESS) {
            return null;
        }

        if (context.getComplexity() == TaskComplexity.TRIVIAL) {
            return "Ready for next task.";
        }

        return switch (context.getSpecialization()) {
            case MINER -> "Shall I continue mining?",
            case BUILDER -> "Ready for next construction phase.",
            case GUARD -> "Perimeter will remain secure.",
            case SCOUT -> "Want to see what I found?",
            case FARMER -> "The crops will need tending again soon.",
            case ARTISAN -> "Ready for next crafting operation.";
        };
    }

    // ============== CONVENIENCE METHODS ==============

    /**
     * Quick report generation with minimal parameters.
     */
    public static CompletionReport quickReport(Specialization specialization,
                                               PersonalityTraits personality,
                                               int rapport,
                                               String taskType) {
        return generateReport(
            CompletionContext.builder()
                .specialization(specialization)
                .personality(personality)
                .rapport(rapport)
                .taskType(taskType)
                .build()
        );
    }

    /**
     * Quick mining report.
     */
    public static CompletionReport miningReport(PersonalityTraits personality,
                                               int rapport,
                                               int oreCount,
                                               String resourceType) {
        return generateReport(
            CompletionContext.builder()
                .specialization(Specialization.MINER)
                .personality(personality)
                .rapport(rapport)
                .taskType("mining")
                .complexity(oreCount > 20 ? 50 : 30)
                .addResult("count", oreCount)
                .addResult("resource", resourceType)
                .build()
        );
    }

    /**
     * Quick construction report.
     */
    public static CompletionReport constructionReport(PersonalityTraits personality,
                                                     int rapport,
                                                     String structureType) {
        return generateReport(
            CompletionContext.builder()
                .specialization(Specialization.BUILDER)
                .personality(personality)
                .rapport(rapport)
                .taskType("construction")
                .complexity(60)
                .addResult("structure", structureType)
                .build()
        );
    }

    /**
     * Quick combat report.
     */
    public static CompletionReport combatReport(PersonalityTraits personality,
                                               int rapport,
                                               int enemiesDefeated) {
        return generateReport(
            CompletionContext.builder()
                .specialization(Specialization.GUARD)
                .personality(personality)
                .rapport(rapport)
                .taskType("combat")
                .complexity(enemiesDefeated > 5 ? 60 : 40)
                .addResult("count", enemiesDefeated)
                .build()
        );
    }
}
```

---

## Dialogue Templates

### 30+ Dialogue Templates by Task Type

#### MINING TASK TEMPLATES

**Tier 1 (Trivial):**
1. "Stone cleared."
2. "Done."
3. "Got it."
4. "Ore collected."
5. "Waypoint reached."

**Tier 2 (Simple):**
6. "Iron extracted. Eight iron ore collected."
7. "Coal vein tapped. 18 units. Not bad."
8. "Cobblestone gathered. 64 units."
9. "Gravel cleared. Found flint."
10. "Dirt removed. Found stone beneath."

**Tier 3 (Moderate):**
11. "Excavation complete. Vein yielded 24 iron ore and 6 gold. Excellent yield."
12. "Mining expedition done. Reached diamond layer. Found 3 diamonds. Solid start."
13. "Cave system cleared. Collected 72 iron ore. This is good mining."
14. "Strip mine complete. 128 blocks deep. Found 12 diamonds. NOW we're talking."
15. "Branch mine finished. Three diamond veins. Worth the effort."

**Tier 4 (Complex):**
16. "Mining expedition concluded. Reached diamond layer. Collected 12 diamonds, 3 emeralds. Significant haul."
17. "Deep excavation complete. Found diamonds at the bottom of the world. This is why we dig deep."
18. "Massive vein tapped. 27 diamonds from one cave system. Never seen anything like it."

**Tier 5 (Epic):**
19. "This expedition... down deep where we belong... found the motherlode. 47 diamonds from one cave. I've been mining twenty years and never seen anything like this. This is the good stone."

#### BUILDING TASK TEMPLATES

**Tier 1-2 (Trivial/Simple):**
20. "Foundation laid. Square within tolerance."
21. "Walls erected. Ready for roof."
22. "Block placed. Level confirmed."
23. "Floor complete. Symmetry maintained."
24. "Windows installed. Lighting adequate."

**Tier 3 (Moderate):**
25. "East wing complete. Symmetry verified across all axes. Used oak for contrast. Quality matters, you know."
26. "Tower constructed. Height optimized for visibility. Structural integrity confirmed."
27. "Bridge finished. Spacing even. Materials appropriate. Acceptable work."
28. "Storage room done. Chest placement logical. Organized for efficiency."

**Tier 4 (Complex):**
29. "Structure complete. Three stories, basement included, glass windows installed. This was... satisfying to build. The symmetry is perfect."
30. "Castle finished. Towers, curtain wall, keep - every block intentional. Architecture at its finest."
31. "Automated farm built. Redstone timing perfect. Efficiency within 2% of theoretical maximum."

**Tier 5 (Epic):**
32. "It's magnificent. Three months of planning, three weeks of building. Look at those arches. That roofline. The symmetry brings tears to my eyes. This is what construction was meant to be. Masterpiece doesn't begin to cover it."

#### COMBAT TASK TEMPLATES

**Tier 1-2:**
33. "Hostile neutralized. Perimeter secure."
34. "Zombie eliminated. Area safe."
35. "Skeleton defeated. Arrows collected."
36. "Spider cleared. Distance maintained."
37. "Creeper destroyed. No structural damage."

**Tier 3:**
38. "Cave cleared. Six hostiles eliminated. No crew casualties. Mission accomplished."
39. "Night survived. Twelve zombies, four skeletons, one spider. Dawn is here. We're safe."
40. "Dungeon pacified. Spawner destroyed. Loot collected. Threat neutralized."

**Tier 4:**
41. "Monument cleared. Elder guardians defeated. All three. Crew survived. That's what matters. The ocean is safe now."
42. "Nether fortress raided. Wither skeletons, blazes, even a ghast. We got the drops. We got out alive. Victory."

**Tier 5:**
43. "We... we actually did it. The wither is defeated. Three heads, three explosions, and we're still standing. That was the hardest fight of my life. I'm proud of us. I don't say that lightly."

#### EXPLORATION TASK TEMPLATES

**Tier 1-2:**
44. "Cave entrance found. Coordinates marked."
45. "Biome discovered. You have to see this!"
46. "Village located. Population six."
47. "Temple spotted. Desert biome, sand structure."
48. "Shipwreck found. Ocean floor."

**Tier 3:**
49. "Cave system mapped! Waterfalls, lava pools, abandoned mineshaft! It's incredible! Took coordinates if you want to visit!"
50. "New biome discovered! Mesa territory! The colors are amazing! Never seen anything like it! Adventure awaits, boss!"
51. "Stronghold located! Eye of ender led us here. Portal room identified! This is it!"

**Tier 4:**
52. "Major discovery! Found a woodland mansion! Massive structure, multiple floors. This is incredible! You HAVE to see this!"
53. "Exploration complete! Mapped the entire continent! Found every biome! This changes everything!"

**Tier 5:**
54. "I found... I found paradise. Massive cavern, underground lake, glowstone ceiling, lush vegetation. It's beautiful. I cried a little. Can we build a base there? Please?"

#### FARMING TASK TEMPLATES

**Tier 1-2:**
55. "Soil tilled. Ready for planting."
56. "Seeds planted. Wheat, carrots, potatoes."
57. "Crops watered. Growth rate optimal."
58. "Harvest gathered. 32 units collected."
59. "Bonemeal applied. Growth accelerated."

**Tier 3:**
60. "Harvest complete. The wheat grew tall this season. Nature provides when we respect her rhythms. 120 units collected."
61. "Orchard established. Saplings planted in proper sunlight. In time, this will be a forest of food. Patience is the farmer's greatest tool."

**Tier 4:**
62. "Farm complete. All crops planted, automated systems ready. Wheat, carrots, potatoes, pumpkins, melons. Nature will provide."

**Tier 5:**
63. "The farm is... it's complete. Every crop, every automated system, every corner of soil nurtured. When I tend these plants, I feel connected to something ancient. We're not just growing food. We're growing life. And that... that is beautiful."

#### CRAFTING TASK TEMPLATES

**Tier 1-2:**
64. "Items crafted. Efficiency adequate."
65. "Smelting batch complete. 87% fuel efficiency."
66. "Tools forged. Durability optimal."
67. "Armor crafted. Protection applied."

**Tier 3:**
68. "Crafting batch complete. 32 items. Efficiency within optimal range. The ratios worked out beautifully."
69. "Recipe executed. All steps followed precisely. Quality verified. Technical precision achieved."

**Tier 4:**
70. "Redstone system complete. Circuit efficiency at 94.7%. Elegant design. This is technical art."

**Tier 5:**
71. "The automated system is finished. Redstone logic, hopper timing, villager optimization - every system integrated. I calculated the efficiency at 97.3%. This is the pinnacle of crafting. Technical perfection. I've been working toward this moment my entire career."

---

## Best Practices

### 1. Closed-Loop Communication

**ALWAYS report task completion**, even for trivial tasks.

```java
// GOOD - Always report
miner.completeTask();
reporter.generateReport(context); // "Done."

// BAD - Silent completion
miner.completeTask();
// Player doesn't know it's done!
```

### 2. Match Complexity to Response Length

**Trivial tasks** (Tier 1): 1-3 words max
**Simple tasks** (Tier 2): 1 short sentence
**Moderate tasks** (Tier 3): 2-3 sentences with detail
**Complex tasks** (Tier 4): 3-4 sentences with results
**Epic tasks** (Tier 5): Emotional, reflective paragraph

### 3. Maintain Character Consistency

Each specialization should have a consistent voice:

- **Miner**: Gruff, practical, achievement-focused
- **Builder**: Professional, perfectionist, quality-obsessed
- **Guard**: Military, protective, security-conscious
- **Scout**: Enthusiastic, wonder-filled, excited
- **Farmer**: Gentle, nurturing, nature-reverent
- **Artisan**: Technical, precise, efficiency-focused

### 4. Rapport Evolution Matters

Track rapport and adjust formality:

- **0-25%**: Formal ("Sir", "Task complete.")
- **26-50%**: Casual ("Boss", "Found diamonds!")
- **51-75%**: Friendly ("[Name]", emotional investment)
- **76-100%**: Intimate ("[Name]", shared history, vulnerability)

### 5. Success vs Partial vs Failure

**Full Success**: Pride, satisfaction, enthusiasm
**Partial Success**: Honesty, explanation, solution proposal
**Failure**: Responsibility, apology, recovery plan

### 6. Contextual Humor

Only add humor when:
- Task was fully successful
- Rapport is medium or higher (30+)
- Task is not trivial
- Player is not frustrated

### 7. Result Injection

Always include concrete results when available:

- "Collected 27 diamonds" (not "Collected diamonds")
- "Eliminated 12 hostiles" (not "Eliminated hostiles")
- "Harvested 156 wheat" (not "Harvested wheat")

### 8. Suggestive Follow-Ups

End with appropriate follow-up suggestions:

- Mining: "Shall I continue?"
- Building: "Ready for next phase."
- Combat: "Perimeter secure."
- Exploration: "Want to see?"
- Farming: "Will need tending soon."
- Crafting: "Ready for next recipe."

### 9. Group Coordination

When multiple workers complete tasks together:

```java
// Acknowledge teamwork
"WE found the motherlode. OUR expedition. OUR success."

// Reference shared achievements
"Remember when we couldn't find diamonds? This makes up for it."
```

### 10. Emotional Resonance

For epic tasks, show genuine emotion:

- "I'll never forget this expedition."
- "This brings tears to my eyes."
- "I'm proud of us. I don't say that lightly."
- "This is the greatest [X] of my career."

---

## Testing and Validation

### Unit Test Examples

```java
@Test
void testMinerTrivialTask() {
    CompletionContext context = CompletionContext.builder()
        .specialization(Specialization.MINER)
        .personality(PersonalityTraits.builder()
            .conscientiousness(90)
            .extraversion(40)
            .build())
        .rapport(10)
        .complexity(TaskComplexity.TRIVIAL)
        .build();

    CompletionReport report = TaskCompletionReporter.generateReport(context);

    assertTrue(report.getDialogue().length() < 50);
    assertTrue(report.getDialogue().toLowerCase().contains("done") ||
               report.getDialogue().toLowerCase().contains("cleared"));
}

@Test
void testBuilderEpicSuccessHighRapport() {
    CompletionContext context = CompletionContext.builder()
        .specialization(Specialization.BUILDER)
        .personality(PersonalityTraits.builder()
            .conscientiousness(95)
            .extraversion(60)
            .build())
        .rapport(85)
        .complexity(TaskComplexity.EPIC)
        .addResult("structure", "castle")
        .build();

    CompletionReport report = TaskCompletionReporter.generateReport(context);

    assertTrue(report.getDialogue().length() > 200);
    assertTrue(report.getDialogue().toLowerCase().contains("magnificent") ||
               report.getDialogue().toLowerCase().contains("masterpiece"));
    assertEquals(RapportStage.TRUE_FRIEND, report.getRapportStage());
}
```

---

## Configuration

### Config File (config/steve-common.toml)

```toml
[character.task_completion]
enabled = true
closed_loop_required = true
complexity_based_response = true

[character.task_completion.complexity_thresholds]
trivial_max = 20
simple_max = 40
moderate_max = 60
complex_max = 80

[character.task_completion.humor]
enabled = true
min_rapport = 30
max_trivial_tier = 2
frequency = 0.3

[character.task_completion.reporting]
always_report_trivial = true
include_results = true
suggest_followup = true
group_coordination = true
```

---

## Research Sources

### Workplace Communication Research
- [Microsoft Support - Updating Progress](https://support.microsoft.com/pt-pt/office/passo-3-atualizar-o-progresso-ca5c3826-85bf-4a31-9351-3b83fd7c8fe0)
- [Ximalaya - Closed-Loop Communication](https://m.ximalaya.com/gerenchengzhang/8229272/49798437)
- [Weibo - Workplace Communication Tips](https://m.weibo.cn/status/5198449454089284)
- [IGWZX - Office Unwritten Rules](https://igwzx.com/index.php/index/index/detail/id/366393)

### Game NPC Systems
- Skyrim Radiant AI Framework
- Fallout Quest Completion Dialogue
- Mass Effect Dialogue History Tracking
- Modern AI-Based NPC Systems

### Personality Research
- DiSC Model (Dominant, Influential, Steady, Conscientious)
- Merrill & Reid Model (Driver, Expressive, Amiable, Analytical)
- OCEAN Big Five (Openness, Conscientiousness, Extraversion, Agreeableness, Neuroticism)

---

## Version Control

**Version:** 1.0.0
**Date:** 2026-02-27
**Author:** MineWright Development Team
**Status:** Complete - Ready for Implementation

**Change Log:**
- v1.0.0 (2026-02-27): Initial comprehensive system design

**Next Review:** After playtesting and player feedback

---

## Related Documents

- [MASTER_CHARACTER_GUIDE.md](C:\Users\casey\steve\docs\characters\MASTER_CHARACTER_GUIDE.md) - Character archetypes and voices
- [FAILURE_RECOVERY_DIALOGUE.md](C:\Users\casey\steve\docs\characters\FAILURE_RECOVERY_DIALOGUE.md) - Failure handling system
- [RELATIONSHIP_DIALOGUE.md](C:\Users\casey\steve\docs\characters\RELATIONSHIP_DIALOGUE.md) - Rapport evolution

---

**End of Task Completion Reporting Dialogue System**
