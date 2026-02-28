# Crew Specialization System for MineWright

**Project:** MineWright - Minecraft Autonomous Agents
**Component:** Crew Specialization System
**Version:** 1.0
**Date:** 2026-02-27

---

## Executive Summary

This document outlines a comprehensive crew specialization system that transforms the current generic worker model into distinct archetypes with unique capabilities, personalities, and roles. The system builds upon the existing orchestration framework and integrates with the current AgentRole enum (FOREMAN, WORKER, SPECIALIST) to create meaningful specialization.

**Key Design Goals:**
1. **Distinct Identities:** Each specialization has unique personality, dialogue, and behaviors
2. **Meaningful Gameplay:** Specializations affect task efficiency and capabilities
3. **Progressive Unlocking:** Crew members grow and develop specializations over time
4. **Strategic Coordination:** Different specializations work better together
5. **Visual Distinction:** Players can identify specializations at a glance

---

## Table of Contents

1. [Current State Analysis](#current-state-analysis)
2. [Specialization Taxonomy](#specialization-taxonomy)
3. [Skill Progression System](#skill-progression-system)
4. [Task Routing Logic](#task-routing-logic)
5. [Personality & Dialogue](#personality--dialogue)
6. [Visual Indicators](#visual-indicators)
7. [Specialization Unlock Mechanics](#specialization-unlock-mechanics)
8. [Multi-Specialization Coordination](#multi-specialization-coordination)
9. [Integration with Existing Systems](#integration-with-existing-systems)
10. [Implementation Roadmap](#implementation-roadmap)

---

## Current State Analysis

### Existing Architecture

**AgentRole Enum:**
```java
public enum AgentRole {
    FOREMAN,    // Orchestrator - coordinates tasks
    WORKER,     // Standard worker - executes tasks
    SPECIALIST, // Role is defined but not implemented
    SOLO        // Independent operation
}
```

**Current Limitations:**
- All workers have identical capabilities
- Nicknames are random (e.g., "Foreman-1", "Crew-2")
- No skill differentiation
- No specialization-based task routing
- No visual distinction between crew members

**Existing Components to Leverage:**
- OrchestratorService for task distribution
- ActionExecutor with plugin-based action system
- ProactiveDialogueManager for personality-driven dialogue
- CompanionMemory for individual personality profiles
- AgentCommunicationBus for inter-crew coordination

---

## Specialization Taxonomy

### Primary Archetypes

Crew members specialize into one of six primary archetypes, each with distinct capabilities and personality traits:

#### 1. MINER ("The Excavator")

**Role:** Efficient resource extraction and tunneling

**Strengths:**
- 50% faster mining speed
- Can identify ore veins from greater distance
- Reduced tool durability loss
- Underground pathfinding bonus

**Weaknesses:**
- Slower at building/placement tasks
- Limited combat capability
- Becomes anxious in open/bright areas

**Personality Traits (Big Five):**
- Openness: 0.5 (practical, focused)
- Conscientiousness: 0.9 (methodical, thorough)
- Extraversion: 0.4 (prefers underground solitude)
- Agreeableness: 0.7 (shares resources willingly)
- Neuroticism: 0.4 (uneasy above ground)

**Dialogue Style:**
- Gruff but friendly
- Mining jargon ("Hit the motherlode", "Clear the drift")
- Complains about sunlight/open spaces
- Enthusiastic about caves and ores

**Equipment Visuals:**
- Pickaxe (always equipped in main hand)
- Mining helmet with glow effect
- Darker, dust-covered skin texture
- Carries ore pouch

---

#### 2. BUILDER ("The Architect")

**Role:** Construction and structural projects

**Strengths:**
- 40% faster block placement
- Can blueprint structures from memory
- Structural integrity awareness
- Scaffolding and platform bonus

**Weaknesses:**
- Slower at mining/combat
- Perfectionist (slows down for quality)
- Gets frustrated with destruction

**Personality Traits (Big Five):**
- Openness: 0.8 (creative, visionary)
- Conscientiousness: 0.95 (obsessive about quality)
- Extraversion: 0.6 (proud of work)
- Agreeableness: 0.5 (critical of others' work)
- Neuroticism: 0.6 (stressed by imperfections)

**Dialogue Style:**
- Architectural terminology
- Perfectionist commentary
- Praises good design
- Critical of "ugly" builds

**Equipment Visuals:**
- Blueprints/scroll in off-hand
- T-square or measuring tape item
- Clean, professional appearance
- Wears tool belt

---

#### 3. GUARD ("The Protector")

**Role:** Combat, perimeter defense, threat elimination

**Strengths:**
- 60% increased combat damage
- Hostile mob detection (wider range)
- Can patrol designated areas
- Shield/protection capabilities

**Weaknesses:**
- Slower at resource tasks
- Becomes bored without threats
- Aggressive personality (may attack neutrals)

**Personality Traits (Big Five):**
- Openness: 0.4 (focused, single-minded)
- Conscientiousness: 0.8 (disciplined, vigilant)
- Extraversion: 0.7 (confident, commanding)
- Agreeableness: 0.4 (suspicious of strangers)
- Neuroticism: 0.3 (fearless, maybe reckless)

**Dialogue Style:**
- Military/mercenary tone
- Constant vigilance commentary
- Threat assessments
- Protective of the crew

**Equipment Visuals:**
- Sword/weapon always equipped
- Shield or armor piece
- Bandage or combat scars
- Alert posture

---

#### 4. SCOUT ("The Pathfinder")

**Role:** Exploration, mapping, resource discovery

**Strengths:**
- 80% faster movement speed
- Extended render distance for discoveries
- Can map and mark locations
- Night vision capabilities

**Weaknesses:**
- Light inventory capacity
- Weaker combat (hit-and-run style)
- Gets bored staying in one place

**Personality Traits (Big Five):**
- Openness: 0.95 (adventurous, curious)
- Conscientiousness: 0.5 (spontaneous, disorganized)
- Extraversion: 0.7 (enthusiastic explorer)
- Agreeableness: 0.8 (shares discoveries)
- Neuroticism: 0.2 (fearless wanderer)

**Dialogue Style:**
- Excited about new discoveries
- Traveler's tales and stories
- Directions and landmarks
- Restless when stationary

**Equipment Visuals:**
- Spyglass or telescope
- Map/compass in off-hand
- Traveler's cloak/cape
- Light, nimble appearance

---

#### 5. FARMER ("The Cultivator")

**Role:** Agriculture, animal husbandry, food production

**Strengths:**
- 3x crop growth speed when tending
- Can breed animals automatically
- Weather prediction abilities
- Bonemeal efficiency bonus

**Weaknesses:**
- Slower at mining/combat
- Peaceful (refuses combat unless attacked)
- Becomes stressed in hostile environments

**Personality Traits (Big Five):**
- Openness: 0.6 (interested in nature)
- Conscientiousness: 0.85 (patient, nurturing)
- Extraversion: 0.5 (quiet, contemplative)
- Agreeableness: 0.9 (gentle, kind)
- Neuroticism: 0.3 (at peace with nature)

**Dialogue Style:**
- Agricultural wisdom
- Weather predictions
- Gentle, nurturing tone
- Concern for plants/animals

**Equipment Visuals:**
- Hoe or watering can
- Carries seeds/crops
- Straw hat or flower accessory
- Earth-tone colors

---

#### 6. ARTISAN ("The Crafter")

**Role:** Smelting, crafting, enchanting, redstone

**Strengths:**
- Instant crafting (no animation delay)
- Can auto-smelt/manage furnaces
- Redstone circuit understanding
- Enchanting bonus

**Weaknesses:**
- Slower at physical tasks
- Needs workshop access
- Becomes frustrated without resources

**Personality Traits (Big Five):**
- Openness: 0.9 (innovative, experimental)
- Conscientiousness: 0.8 (precise, careful)
- Extraversion: 0.4 (focused worker)
- Agreeableness: 0.6 (shares knowledge)
- Neuroticism: 0.5 (perfectionist about recipes)

**Dialogue Style:**
- Technical crafting terminology
- Recipe suggestions
- Redstone enthusiasm
- Efficiency optimization

**Equipment Visuals:**
- Carries workbench or crafting table
- Redstone dust particles
- Goggles or glasses
- Tool apron

---

### Specialization Compatibility Matrix

Some specializations work exceptionally well together:

| Pairing | Synergy Effect | Example Use Case |
|---------|---------------|-----------------|
| **Miner + Builder** | "Construction Team" - 30% faster build projects | Large-scale building |
| **Scout + Guard** | "Patrol Squad" - extended perimeter defense | Base protection |
| **Farmer + Artisan** | "Production Line" - automated food + processing | Sustainable base |
| **Builder + Artisan** | "Master Builders" - complex redstone structures | Technical builds |
| **Scout + Miner** | "Expedition Team" - remote resource acquisition | Exploration missions |

---

## Skill Progression System

### Skill Categories

Each specialization has 5 skill categories that progress independently:

```java
public enum SkillCategory {
    EFFICIENCY,      // Speed of primary tasks
    QUALITY,         // Output quality/bonuses
    KNOWLEDGE,       // Unlocks new capabilities
    ENDURANCE,       // Work before rest needed
    SPECIALIZATION   // Unique specialization abilities
}
```

### Progression Mechanics

**XP Sources:**

1. **Task Completion:** +10-50 XP per task based on difficulty
2. **Successful Collaboration:** +25 XP when working with compatible specialization
3. **Teaching:** +15 XP when helping lower-level crew
4. **Discoveries:** +100 XP for finding new resources/locations
5. **Milestones:** +200 XP for significant achievements

**Skill Levels:**

| Level | XP Required | Unlock |
|-------|------------|--------|
| 1 (Novice) | 0 | Basic abilities |
| 10 (Apprentice) | 500 | +10% efficiency |
| 25 (Journeyman) | 2,500 | New skill unlocks |
| 50 (Expert) | 10,000 | +25% efficiency, teaching ability |
| 75 (Master) | 30,000 | +50% efficiency, unique abilities |
| 100 (Legend) | 100,000 | Title, visual effects |

### Skill Implementation

```java
public class SpecializationSkills {
    private final Map<SkillCategory, SkillLevel> skills;
    private final SpecializationType specialization;
    private int totalXP;

    public void addXP(SkillCategory category, int amount, GameContext context) {
        SkillLevel level = skills.get(category);

        // Apply specialization bonus
        if (category == SkillCategory.SPECIALIZATION) {
            amount *= specialization.getSkillMultiplier();
        }

        // Apply context bonuses
        if (isCollaborating(context)) {
            amount *= 1.25; // Collaboration bonus
        }

        level.addXP(amount);

        // Check for level-up
        if (level.justLeveledUp()) {
            onSkillLevelUp(category, level.getLevel());
        }
    }

    private void onSkillLevelUp(SkillCategory category, int newLevel) {
        // Unlock new abilities
        SpecializationAbilities abilities = specialization.getAbilities();
        abilities.unlockForLevel(category, newLevel);

        // Notify player
        String message = String.format("%s reached %s %d!",
            crewName, category, newLevel);
        sendChatMessage(message);

        // Visual celebration
        spawnLevelUpParticles();
    }
}
```

### Skill Decay and Maintenance

**Optional:** Skills decay slowly if not used (configurable):

- **Unused Skill Decay:** -1% per day (configurable)
- **Minimum Floor:** Never drops below Apprentice level
- **Rapid Recovery:** 2x XP gain until returning to previous level

```java
public class SkillDecayManager {
    public void tick() {
        for (SkillCategory category : SkillCategory.values()) {
            if (shouldDecay(category)) {
                int decayAmount = calculateDecay(category);
                skills.get(category).decay(decayAmount);
            }
        }
    }

    private boolean shouldDecay(SkillCategory category) {
        // Decay if:
        // - Not used in 3 Minecraft days
        // - Config is enabled
        // - Not at minimum floor
        return config.skillDecayEnabled()
            && getLastUsed(category).isBefore(Instant.now().minus(3, ChronoUnit.DAYS))
            && skills.get(category).getLevel() > 10;
    }
}
```

---

## Task Routing Logic

### Specialization-Aware Task Distribution

The OrchestratorService must be enhanced to consider specializations when assigning tasks:

```java
public class SpecializationAwareTaskRouter {
    private final Map<String, CrewSpecialization> crewSpecializations;
    private final SpecializationCompatibility compatibility;

    public String assignTask(Task task, List<String> availableCrew) {
        // Determine task type
        TaskType taskType = classifyTask(task);

        // Score each available crew member
        Map<String, Double> scores = new HashMap<>();
        for (String crewId : availableCrew) {
            double score = calculateAssignmentScore(crewId, task, taskType);
            scores.put(crewId, score);
        }

        // Select best match
        String bestCrew = scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(availableCrew.get(0));

        return bestCrew;
    }

    private double calculateAssignmentScore(String crewId, Task task, TaskType taskType) {
        double score = 0.0;
        CrewSpecialization spec = crewSpecializations.get(crewId);

        // Base compatibility (0.0 to 1.0)
        score += spec.getCompatibilityWithTask(taskType);

        // Skill level bonus (up to 0.5)
        score += spec.getSkillLevel(taskType) * 0.005;

        // Current workload penalty
        score -= getCurrentWorkload(crewId) * 0.2;

        // Distance to task location
        score -= getDistanceToTask(crewId, task) * 0.001;

        // Collaboration bonus
        List<String> assignedCrew = getAssignedCrew(task);
        for (String otherId : assignedCrew) {
            if (compatibility.hasSynergy(spec, crewSpecializations.get(otherId))) {
                score += 0.3;
            }
        }

        return score;
    }
}
```

### Task Classification

```java
public enum TaskType {
    // Primary tasks
    MINING,         // Ore/stone extraction
    BUILDING,       // Block placement/construction
    COMBAT,         // Hostile mob engagement
    EXPLORATION,    // Discovering new areas
    FARMING,        // Crop/animal management
    CRAFTING,       // Item creation/smelting

    // Secondary tasks
    LOGISTICS,      // Item transport
    MAINTENANCE,    // Repair/cleaning
    DEFENSE,        // Perimeter protection
    RESEARCH,       // Redstone/experimentation
    SOCIAL,         // Dialogue/teaching

    // Special tasks
    EMERGENCY,      // Critical situations
    COORDINATION,   // Multi-crew tasks
    TEACHING,       // Training other crew
    LEARNING;       // Gaining new skills
}
```

### Specialization Task Compatibility

| Specialization | MINING | BUILDING | COMBAT | EXPLORATION | FARMING | CRAFTING |
|----------------|--------|----------|--------|------------|---------|----------|
| **Miner** | 1.0 | 0.4 | 0.3 | 0.6 | 0.3 | 0.4 |
| **Builder** | 0.4 | 1.0 | 0.2 | 0.4 | 0.3 | 0.7 |
| **Guard** | 0.3 | 0.3 | 1.0 | 0.5 | 0.4 | 0.2 |
| **Scout** | 0.4 | 0.3 | 0.5 | 1.0 | 0.3 | 0.3 |
| **Farmer** | 0.3 | 0.3 | 0.2 | 0.4 | 1.0 | 0.6 |
| **Artisan** | 0.4 | 0.7 | 0.2 | 0.4 | 0.5 | 1.0 |

---

## Personality & Dialogue

### Specialization-Based Personalities

Each specialization has a unique personality profile that affects:

1. **Dialogue Content:** What they talk about
2. **Reaction Styles:** How they respond to events
3. **Proactive Comments:** What they notice and comment on
4. **Relationship Dynamics:** How they interact with player and other crew

```java
public class SpecializationPersonality {
    private final SpecializationType type;
    private final PersonalityProfile baseProfile;
    private final DialogueStyle dialogueStyle;
    private final List<TopicPreference> preferredTopics;
    private final List<TopicPreference> dislikedTopics;

    public String generateComment(GameEvent event, GameContext context) {
        // Filter by specialization interests
        if (!isInterestedInEvent(event)) {
            return null; // Won't comment on uninteresting events
        }

        // Generate base comment
        String baseComment = dialogueStyle.generateForEvent(event);

        // Apply personality adjustments
        baseComment = baseProfile.adjustResponse(baseComment, context);

        // Add specialization flavor
        baseComment = addSpecializationFlavor(baseComment, event);

        return baseComment;
    }

    private boolean isInterestedInEvent(GameEvent event) {
        // Each specialization has different interests
        return switch(type) {
            case MINER -> event.type == EventType.ORE_FOUND
                       || event.type == EventType.CAVE_DISCOVERED;
            case BUILDER -> event.type == EventType.BLOCK_PLACED
                         || event.type == EventType.STRUCTURE_COMPLETE;
            case GUARD -> event.type == EventType.HOSTILE_NEARBY
                      || event.type == EventType.PERIMETER_BREACH;
            case SCOUT -> event.type == EventType.NEW_BIOME
                      || event.type == EventType.STRUCTURE_FOUND;
            case FARMER -> event.type == EventType.CROP_GROWN
                       || event.type == EventType.ANIMAL_BRED;
            case ARTISAN -> event.type == EventType.ITEM_CRAFTED
                        || event.type == EventType.REDSTONE_CREATED;
        };
    }
}
```

### Example Dialogue by Specialization

**MINER Dialogue:**
```
On finding diamonds: "NOW we're talking! Look at that sparkle. Beautiful."
On entering caves: "Ah, home sweet home. Nothing like the smell of stone."
On building tasks: "I'll place the blocks, but don't expect me to make it pretty."
On sunlight: "Ugh, too bright. Where's a good dark tunnel when you need one?"
```

**BUILDER Dialogue:**
```
On structure complete: "Finally! Architecture takes patience. Behold my masterpiece."
On ugly builds: "I'm not saying it's terrible, but... it's not great."
On receiving materials: "Excellent. These will do. Quality matters, you know."
On construction start: "Time to build something magnificent. Watch and learn."
```

**GUARD Dialogue:**
```
On hostile mobs: "Threat eliminated. Perimeter secure. You're welcome."
On peaceful areas: "Too quiet. I don't trust it. Stay alert."
On nightfall: "Night is here. Things are going to get ugly. I'm ready."
On crew protection: "Nothing touches my crew. Nothing."
```

**SCOUT Dialogue:**
```
On new biome: "Incredible! Look at this place! Never seen anything like it!"
On staying put: "How long have we been here? Too long. Adventure awaits!"
On discoveries: "Found something amazing! You have GOT to see this!"
On mapping: "Every path discovered is a story waiting to happen."
```

**FARMER Dialogue:**
```
On crop growth: "Look at them grow! Nature is miraculous, isn't it?"
On rain: "Perfect weather for the crops. The plants will love this."
On combat: "I... I'll help, but I don't like violence. Can't we all get along?"
On animals: "Who's a good sheep? You are! Yes you are!"
```

**ARTISAN Dialogue:**
```
On crafting: "Precision is everything. One wrong ingredient, wasted effort."
On redstone: "The beauty of circuits! This design is... elegant. Efficient."
On manual tasks: "There must be a more efficient way to do this. Let me automate it."
On recipe success: "Perfection. The ratios, the timing... simply masterful."
```

### Inter-Specialization Banter

When crew members of different specializations work together, they exchange contextual banter:

```java
public class CrewBanterSystem {
    public Optional<String> generateBanter(String crew1, String crew2, Task currentTask) {
        SpecializationType spec1 = getSpecialization(crew1);
        SpecializationType spec2 = getSpecialization(crew2);

        // Check for compatible or incompatible pairs
        BanterTemplate template = banterLibrary.getTemplate(spec1, spec2, currentTask);

        if (template != null && shouldTriggerBanter()) {
            String banter = template.generate(crew1, crew2, currentTask);
            return Optional.of(banter);
        }

        return Optional.empty();
    }
}
```

**Example Banter Pairs:**

**Miner + Builder:**
```
Miner: "I got the stone you wanted. It's not pretty, but it's solid."
Builder: "It'll do. FOR NOW. I'll make it presentable."
Miner: "Heh. Good luck with that. Stone is stubborn."
Builder: "So am I. That's why we work well together."
```

**Guard + Scout:**
```
Guard: "Stop running off. I can't protect you if you're three biomes away."
Scout: "But there's SO MUCH to see! You're missing everything!"
Guard: "I'm missing danger signs when you wander into hostile territory."
Scout: "That's half the fun! Live a little!"
```

**Farmer + Artisan:**
```
Farmer: "Could you build an automated harvester? My back is killing me."
Artisan: "Excellent idea! I'll need redstone, iron, and... three days."
Farmer: "I'll wait. Anything to stop bending over these wheat fields."
Artisan: "Appreciate the patience. Artistry takes time."
```

---

## Visual Indicators

### Equipment and Appearance

Each specialization has distinct visual cues:

**Equipment (Main Hand):**
- Miner: Pickaxe (stone/iron/diamond based on level)
- Builder: Mason's hammer or blueprint
- Guard: Sword or shield
- Scout: Spyglass or compass
- Farmer: Hoe or watering can
- Artisan: Workbench or redstone

**Accessories (Off Hand/Armor):**
- Miner: Mining helmet (glowing when in dark)
- Builder: Tool belt, measuring tape
- Guard: Shield, armor piece
- Scout: Map, cape/cloak
- Farmer: Straw hat, seed pouch
- Artisan: Goggles, apron

**Skin/Appearance Variations:**
- Miner: Dust-covered, darker skin, mining clothes
- Builder: Clean, professional appearance, blueprints
- Guard: Armor pieces, battle scars, imposing posture
- Scout: Traveler's gear, cloak, nimble stance
- Farmer: Earth tones, straw hat, gentle posture
- Artisan: Goggles, tool apron, redstone dust particles

### Name Plate Indicators

When hovering over a crew member, display:

```
[NAME] - [SPECIALIZATION] Lv.[LEVEL]
  ⚒ Miner | Efficiency: 75 | XP: 2340/5000
```

**Color Coding:**
- Miner: Dark Gray (stone color)
- Builder: Brown (wood color)
- Guard: Red (danger color)
- Scout: Green (exploration color)
- Farmer: Yellow (wheat color)
- Artisan: Blue (technical color)

### Particle Effects

**Active Work Particles:**
- Miner: Stone dust particles when mining
- Builder: Blueprint glow when placing
- Guard: Sword sweep effect in combat
- Scout: Wind particles when moving fast
- Farmer: Green particles when tending crops
- Artisan: Redstone spark when crafting

**Level-Up Effects:**
- Miner: Ore particles exploding outward
- Builder: Structure outline forming
- Guard: Shield effect expanding
- Scout: Compass spinning
- Farmer: Growth burst particles
- Artisan: Enchanting table glyphs

### HUD Integration

**Crew Status Display:**

```
┌─────────────────────────────────────┐
│  CREW MANAGEMENT                    │
├─────────────────────────────────────┤
│  [⚒] Grum - Miner Lv.25            │
│      Mining iron ore... 67%         │
│      Status: Working | Mood: Focused │
│                                      │
│  [🔨] Bella - Builder Lv.18         │
│      Idle - awaiting tasks          │
│      Status: Ready | Mood: Creative │
│                                      │
│  [⚔] Thorin - Guard Lv.32          │
│      Patrolling perimeter...         │
│      Status: Active | Mood: Alert   │
└─────────────────────────────────────┘
```

---

## Specialization Unlock Mechanics

### Unlock System Design

Crew members start as generic workers and unlock specializations through:

#### 1. **Natural Affinity** (Automatic at Creation)

When spawned, each crew member has hidden "affinity scores" for each specialization:

```java
public class AffinityScores {
    private final Map<SpecializationType, Double> affinities;

    public static AffinityScores random() {
        AffinityScores scores = new AffinityScores();
        Random random = new Random();

        for (SpecializationType type : SpecializationType.values()) {
            // Each specialization gets 0.0 to 1.0 affinity
            double affinity = random.nextDouble();
            scores.setAffinity(type, affinity);
        }

        // Boost one random specialization (natural talent)
        SpecializationType talent = SpecializationType.values()[
            random.nextInt(SpecializationType.values().length)
        ];
        scores.boostAffinity(talent, 0.3); // +30% bonus

        return scores;
    }
}
```

**First Specialization Unlock:**
- Complete 10 tasks of a type
- Affinity for that type > 0.6
- Crew member expresses interest in dialogue

#### 2. **Task-Based Progression** (Active Unlock)

Track task completion to unlock specializations:

```java
public class SpecializationProgress {
    private final Map<TaskType, Integer> taskCounts;
    private final Map<SpecializationType, Double> progress;

    public void recordTaskCompletion(TaskType type) {
        taskCounts.put(type, taskCounts.getOrDefault(type, 0) + 1);

        // Update relevant specialization progress
        for (SpecializationType spec : getRelatedSpecializations(type)) {
            double currentProgress = progress.getOrDefault(spec, 0.0);
            double increment = 1.0 / getTasksRequiredForUnlock(spec);
            progress.put(spec, Math.min(1.0, currentProgress + increment));

            // Check for unlock
            if (currentProgress < 1.0 && progress.get(spec) >= 1.0) {
                unlockSpecialization(spec);
            }
        }
    }
}
```

**Unlock Thresholds:**
| Specialization | Tasks Required | Qualifying Tasks |
|----------------|----------------|------------------|
| Miner | 15 mining tasks | mine, gather |
| Builder | 20 building tasks | place, build |
| Guard | 10 combat tasks | attack, defend |
| Scout | 5 exploration tasks | explore, scout |
| Farmer | 10 farming tasks | farm, breed |
| Artisan | 15 crafting tasks | craft, smelt |

#### 3. **Player Assignment** (Manual Unlock)

Players can manually assign specializations:

```
/crew specialize <name> <specialization>
```

**Requirements:**
- Crew member has > 20% affinity for that specialization
- Crew member has completed at least 5 qualifying tasks
- Player has "Specialization Token" (earned through achievements)

**Cost:**
- First specialization: Free
- Second specialization: 10 tokens (multi-specialization)
- Respecialization: 5 tokens

#### 4. **Teaching/Learning** (Social Unlock)

High-level crew members can teach others:

```java
public class TeachingSystem {
    public boolean attemptTeaching(String teacher, String student,
                                   SpecializationType spec) {
        // Requirements:
        CrewSpecialization teacherSpec = getSpecialization(teacher);
        CrewSpecialization studentSpec = getSpecialization(student);

        if (!meetsTeachingRequirements(teacherSpec, studentSpec, spec)) {
            return false;
        }

        // Teaching takes time (real-time minutes)
        int teachingDuration = calculateTeachingDuration(teacherSpec, spec);
        startTeachingSession(teacher, student, spec, teachingDuration);

        return true;
    }

    private boolean meetsTeachingRequirements(CrewSpecialization teacher,
                                              CrewSpecialization student,
                                              SpecializationType spec) {
        // Teacher must be level 50+ in specialization
        if (teacher.getSkillLevel(spec) < 50) {
            return false;
        }

        // Student must have > 30% affinity
        if (student.getAffinity(spec) < 0.3) {
            return false;
        }

        // Student must not already specialize in this
        if (student.getSpecialization() == spec) {
            return false;
        }

        return true;
    }
}
```

### Specialization Change (Respecialization)

Crew members can change specializations, but with penalties:

**Respecialization Costs:**
- Lose 25% of XP in old specialization
- New specialization starts at 30% of old level (minimum level 10)
- 3-day cooldown between changes
- Temporary mood debuff ("identity crisis")

**Respecialization Dialogue:**
```
"I've been thinking... maybe I'm not meant to be a miner.
The earth is nice, but I've been dreaming about building..."
```

---

## Multi-Specialization Coordination

### Crew Composition Strategies

Optimal crew compositions for different playstyles:

#### **The Construction Crew**
- 1x Foreman (coordination)
- 2x Builder (rapid building)
- 1x Miner (material supply)
- 1x Artisan (redstone/technical)

**Best For:** Large building projects, technical builds

#### **The Expedition Team**
- 1x Scout (pathfinding)
- 1x Guard (protection)
- 1x Miner (resource acquisition)
- 1x Artisan (tool repair/crafting)

**Best For:** Exploration, remote operations

#### **The Sustainable Base**
- 1x Farmer (food production)
- 1x Builder (maintenance)
- 1x Guard (perimeter defense)
- 1x Artisan (automation)

**Best For:** Long-term base operations, self-sufficiency

#### **The Mining Operation**
- 1x Foreman (coordination)
- 3x Miner (rapid excavation)
- 1x Builder (tunnel support)

**Best For:** Large-scale mining, resource extraction

### Synergy Bonuses

When compatible specializations work together:

```java
public class SynergyManager {
    public double calculateSynergyBonus(List<CrewMember> crew) {
        double totalBonus = 1.0; // Base multiplier

        // Count specialization pairs
        Map<SpecializationPair, Integer> pairCounts = countSpecializationPairs(crew);

        for (Map.Entry<SpecializationPair, Integer> entry : pairCounts.entrySet()) {
            SpecializationPair pair = entry.getKey();
            int count = entry.getValue();

            if (pair.hasSynergy()) {
                // Each pair adds 10% bonus
                totalBonus += 0.1 * count;
            }
        }

        // Cap at 2.0x (100% bonus)
        return Math.min(2.0, totalBonus);
    }
}
```

**Synergy Effects:**
- **Speed Bonus:** Tasks complete faster
- **Quality Bonus:** Better output (e.g., more drops, better builds)
- **XP Bonus:** Faster skill progression
- **Mood Bonus:** Crew members stay happier

### Coordination Commands

New commands for multi-specialization crews:

```
/crew form <team_name> <composition>
  Example: /crew form mining 3miner 1builder

/crew assign <team_name> <task>
  Example: /crew assign mining "Excavate quadrant 4"

/crew disband <team_name>
  Example: /crew disband mining

/crew status <team_name>
  Example: /crew status mining
```

### Cross-Training

Crew members can learn secondary specializations:

```java
public class CrossTrainingSystem {
    public void startCrossTraining(String crewId, SpecializationType newSpec) {
        CrewMember crew = getCrewMember(crewId);

        // Requirements:
        // - Primary specialization at level 30+
        // - Completed 50+ tasks with crew of target specialization
        // - Affinity > 0.4 for new specialization

        if (meetsCrossTrainingRequirements(crew, newSpec)) {
            // Training takes real-time hours
            int trainingHours = calculateTrainingTime(crew, newSpec);
            startTrainingSession(crewId, newSpec, trainingHours);
        }
    }

    private void onTrainingComplete(String crewId, SpecializationType newSpec) {
        CrewMember crew = getCrewMember(crewId);

        // Add as secondary specialization (50% effectiveness)
        crew.addSecondarySpecialization(newSpec, 0.5);

        // Unlock dual-specialization abilities
        unlockDualAbilities(crew.getPrimarySpecialization(), newSpec);
    }
}
```

**Dual-Specialization Examples:**
- **Miner-Builder:** Expert at underground construction
- **Guard-Scout:** Reconnaissance and patrol specialist
- **Farmer-Artisan:** Automated farming systems
- **Builder-Artisan:** Redstone technical builds

---

## Integration with Existing Systems

### AgentRole Enhancement

Extend AgentRole enum to support specializations:

```java
public enum AgentRole {
    FOREMAN,     // Orchestrator - can be any specialization background
    WORKER,      // Standard worker - gains specialization over time
    SPECIALIST,  // Fully specialized - focuses on specific tasks
    SOLO;        // Independent - can have any specialization

    private SpecializationType specialization = null;

    public void setSpecialization(SpecializationType type) {
        this.specialization = type;
    }

    public SpecializationType getSpecialization() {
        return specialization;
    }

    public boolean isSpecialized() {
        return specialization != null;
    }
}
```

### OrchestratorService Integration

Enhance task distribution to consider specializations:

```java
public class OrchestratorService {
    private SpecializationAwareTaskRouter taskRouter;

    private void distributeTasks(PlanExecution plan, Collection<ForemanEntity> availableSteves) {
        List<Task> tasks = plan.getRemainingTasks();
        List<ForemanEntity> availableWorkers = availableSteves.stream()
            .filter(s -> !s.getSteveName().equals(foremanId))
            .filter(s -> !workerAssignments.containsKey(s.getSteveName()))
            .collect(Collectors.toList());

        LOGGER.info("[Orchestrator] Distributing {} tasks to {} specialized workers",
            tasks.size(), availableWorkers.size());

        // Use specialized router
        for (Task task : tasks) {
            String bestWorker = taskRouter.assignTask(task,
                availableWorkers.stream().map(Entity::getUUID).map(UUID::toString).toList());

            if (bestWorker != null) {
                assignTaskToAgent(plan, task, bestWorker);
            }
        }
    }
}
```

### ForemanEntity Integration

Add specialization data to entity:

```java
public class ForemanEntity extends PathfinderMob {
    private SpecializationType specialization;
    private SpecializationSkills skills;
    private DualSpecialization dualSpec; // Optional secondary spec

    // Save/load specialization
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putString("Specialization", specialization.name());
        tag.putInt("SpecializationLevel", skills.getTotalLevel());

        // Save individual skills
        CompoundTag skillsTag = new CompoundTag();
        skills.saveToNBT(skillsTag);
        tag.put("Skills", skillsTag);

        // Save dual specialization if present
        if (dualSpec != null) {
            tag.putString("DualSpecialization", dualSpec.getType().name());
            tag.putDouble("DualSpecEffectiveness", dualSpec.getEffectiveness());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("Specialization")) {
            specialization = SpecializationType.valueOf(tag.getString("Specialization"));
            skills = new SpecializationSkills(specialization);

            if (tag.contains("Skills")) {
                skills.loadFromNBT(tag.getCompound("Skills"));
            }

            if (tag.contains("DualSpecialization")) {
                DualSpecializationType dualType = DualSpecializationType.valueOf(
                    tag.getString("DualSpecialization")
                );
                dualSpec = new DualSpecialization(dualType, tag.getDouble("DualSpecEffectiveness"));
            }

            // Apply specialization bonuses
            applySpecializationBonuses();
        }
    }

    private void applySpecializationBonuses() {
        // Apply stat bonuses based on specialization
        switch (specialization) {
            case MINER:
                getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(
                    new AttributeModifier("miner_bonus", 0.1, AttributeModifier.Operation.MULTIPLY_TOTAL)
                );
                break;
            case BUILDER:
                // Building speed bonuses
                break;
            case GUARD:
                getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(
                    new AttributeModifier("guard_bonus", 0.6, AttributeModifier.Operation.MULTIPLY_TOTAL)
                );
                break;
            case SCOUT:
                getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(
                    new AttributeModifier("scout_bonus", 0.8, AttributeModifier.Operation.MULTIPLY_TOTAL)
                );
                break;
            // ... other specializations
        }
    }
}
```

### ActionExecutor Integration

Specialization affects action execution:

```java
public class ActionExecutor {
    private BaseAction createAction(Task task) {
        SpecializationType spec = foreman.getSpecialization();

        // Check if action is compatible with specialization
        double compatibility = spec.getCompatibilityWithTask(task.getAction());

        if (compatibility < 0.3) {
            // Log warning about poor task assignment
            MineWrightMod.LOGGER.warn("Crew member {} (specialized in {}) assigned incompatible task: {}",
                foreman.getSteveName(), spec, task.getAction());
        }

        // Apply efficiency modifier
        double efficiency = 1.0 + (spec.getSkillLevel(task.getAction()) * 0.01);
        task.setEfficiencyModifier(efficiency);

        // Create action
        return createActionInternal(task);
    }
}
```

### ProactiveDialogueManager Integration

Add specialization-aware dialogue:

```java
public class ProactiveDialogueManager {
    private String generateSpecializationComment(GameEvent event) {
        SpecializationType spec = foreman.getSpecialization();
        SpecializationPersonality personality = personalityProfiles.get(spec);

        return personality.generateComment(event, getContext());
    }
}
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Goals:** Basic specialization system infrastructure

**Tasks:**
- [ ] Create `SpecializationType` enum with 6 types
- [ ] Create `CrewSpecialization` class with skills, XP, levels
- [ ] Create `SpecializationPersonality` class
- [ ] Add specialization field to `ForemanEntity`
- [ ] Implement specialization save/load
- [ ] Create basic `SpecializationConfig`

**Deliverables:**
- Specialization can be assigned to crew members
- Specialization persists across save/load
- Basic skills tracking

### Phase 2: Task Routing (Week 3)

**Goals:** Specialization-aware task distribution

**Tasks:**
- [ ] Create `TaskType` enum and classifier
- [ ] Create `SpecializationAwareTaskRouter`
- [ ] Implement task compatibility scoring
- [ ] Integrate with `OrchestratorService`
- [ ] Add specialization tooltips to crew UI

**Deliverables:**
- Tasks are routed to appropriate specializations
- Efficiency bonuses/penalties applied
- UI shows specialization info

### Phase 3: Visual Indicators (Week 4)

**Goals:** Distinct appearance for each specialization

**Tasks:**
- [ ] Create equipment models for each specialization
- [ ] Implement skin/appearance variations
- [ ] Add particle effects for active work
- [ ] Create color-coded name plates
- [ ] Design HUD crew status display

**Deliverables:**
- Visual distinction between specializations
- Equipment displays correctly
- HUD shows specialization status

### Phase 4: Dialogue & Personality (Week 5-6)

**Goals:** Specialization-specific dialogue and personalities

**Tasks:**
- [ ] Create `SpecializationPersonality` profiles
- [ ] Write dialogue templates for each specialization
- [ ] Implement specialization-specific comments
- [ ] Add inter-specialization banter system
- [ ] Integrate with `ProactiveDialogueManager`

**Deliverables:**
- Each specialization has unique dialogue
- Banter occurs between crew members
- Personality affects responses

### Phase 5: Skill Progression (Week 7-8)

**Goals:** Full skill system with XP and unlocks

**Tasks:**
- [ ] Implement skill categories and XP tracking
- [ ] Create skill level-up system
- [ ] Add unlockable abilities per specialization
- [ ] Implement teaching/cross-training
- [ ] Create skill decay (optional, config-based)

**Deliverables:**
- Skills progress through use
- Level-ups unlock new abilities
- Teaching system works
- Configurable skill decay

### Phase 6: Multi-Specialization Coordination (Week 9)

**Goals:** Crew composition strategies and synergy

**Tasks:**
- [ ] Implement synergy bonus system
- [ ] Create team formation commands
- [ ] Add dual-specialization system
- [ ] Implement coordination UI
- [ ] Create crew composition presets

**Deliverables:**
- Synergy bonuses work correctly
- Teams can be formed and managed
- Dual-specialization functional
- Preset compositions available

### Phase 7: Polish & Balance (Week 10)

**Goals:** Refine gameplay balance and user experience

**Tasks:**
- [ ] Balance skill progression rates
- [ ] Adjust synergy bonus percentages
- [ ] Fine-tune dialogue frequency
- [ ] Optimize performance
- [ ] Write user documentation

**Deliverables:**
- Balanced gameplay experience
- Good performance
- Complete documentation

---

## Configuration

### Config File Format (minewright-common.toml)

```toml
# Specialization System Configuration

[Specializations]
# Enable specialization system
enabled = true

# Require specialization unlock (vs. auto-assign at creation)
require_unlock = true

# Allow manual specialization assignment by players
allow_manual_assignment = true

# Allow re-specialization (changing specialization)
allow_respecialization = true

# Re-specialization cost in tokens
respecialization_cost = 5

# Cooldown between re-specialization (in days)
respecialization_cooldown_days = 3

[Skills]
# Enable skill progression
enabled = true

# Base XP multiplier (higher = faster progression)
xp_multiplier = 1.0

# Enable skill decay (skills decrease if not used)
decay_enabled = false

# Skill decay rate per day (as percentage)
decay_rate_percent = 1.0

# Minimum skill level (floor before decay stops)
minimum_level = 10

[Synergy]
# Enable synergy bonuses for compatible specializations
enabled = true

# Base synergy bonus per compatible pair (as percentage)
base_bonus_percent = 10

# Maximum total synergy bonus (capped at this value)
max_bonus_percent = 100

[Teaching]
# Enable high-level crew teaching others
enabled = true

# Minimum level to teach
min_teaching_level = 50

# Minimum student affinity requirement
min_student_affinity = 0.3

# Teaching time multiplier (higher = slower)
teaching_time_multiplier = 1.0

[Visuals]
# Show specialization in name plates
show_in_nameplates = true

# Show equipment indicators
show_equipment = true

# Enable particle effects
particles_enabled = true

# Color code specializations in UI
color_coding = true
```

---

## Conclusion

This crew specialization system transforms the generic worker model into a rich, strategic gameplay layer where crew members have distinct identities, capabilities, and roles. The system integrates seamlessly with existing MineWright architecture while adding significant depth to crew management.

**Key Benefits:**

1. **Strategic Depth:** Players must consider crew composition for different tasks
2. **Emotional Attachment:** Distinct personalities make crew members memorable
3. **Replayability:** Different crew compositions offer varied gameplay
4. **Progression:** Skill system provides long-term goals
5. **Coordination:** Synergy bonuses reward smart team building

**Future Enhancements:**

- Specialization-specific quests and storylines
- Legendary crew members with unique abilities
- Faction system (crew can form rivalries/alliances)
- Crew relationships and social dynamics
- Specialization mastery challenges

---

**Document Version:** 1.0
**Author:** MineWright AI Development Team
**Status:** Research Complete - Ready for Implementation
**Last Updated:** 2026-02-27
