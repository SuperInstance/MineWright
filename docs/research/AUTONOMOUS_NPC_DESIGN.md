# Autonomous NPC Behavior Design for MineWright

**Version:** 1.0
**Date:** 2025-02-28
**Status:** Design Document
**Author:** Research & Design Team

---

## Executive Summary

This document outlines a comprehensive design for autonomous NPC behavior systems in MineWright, enabling crew members (Foremen) to act like "fellow players" rather than scripted bots. The design synthesizes best practices from games like The Sims, Skyrim, RimWorld, and Dwarf Fortress, while leveraging LLM-powered intelligence for adaptive behavior.

**Key Objectives:**
- Create NPCs that proactively find meaningful tasks when idle
- Implement personality-driven behavior variation
- Support needs-based motivation systems
- Enable emergent behavior from simple rules
- Maintain player control while allowing autonomy

---

## Table of Contents

1. [Philosophy and Principles](#philosophy-and-principles)
2. [Autonomous Behavior Drivers](#autonomous-behavior-drivers)
3. [Personality-Driven Autonomy](#personality-driven-autonomy)
4. [Idle Behavior Systems](#idle-behavior-systems)
5. [Industry Inspiration](#industry-inspiration)
6. [Implementation Architecture](#implementation-architecture)
7. [LLM Integration](#llm-integration)
8. [Testing and Validation](#testing-and-validation)

---

## 1. Philosophy and Principles

### Core Design Philosophy

**"Fellow Players, Not Bots"**

Autonomous NPCs should feel like cooperative players who:
- Notice opportunities and problems proactively
- Have personal preferences and quirks
- Communicate intentions naturally
- Learn from experience
- Make mistakes and recover gracefully

### Design Principles

| Principle | Description | Example |
|-----------|-------------|---------|
| **Proactivity** | NPCs act without explicit commands | Notices crop field needs harvesting |
| **Personality** | Individual traits affect decisions | Cautious NPC checks tools before risky tasks |
| **Predictability** | Behavior follows understandable patterns | Always checks storage before gathering |
| **Emergence** | Complex behavior from simple rules | Curiosity leads to discovering resources |
| **Player Control** | Autonomy respects player authority | Stops autonomous work when given explicit task |

### Anti-Patterns to Avoid

- **Random wandering** disguised as autonomy
- **Ignoring player commands** to pursue "interesting" behavior
- **Over-communicating** every autonomous action
- **Predictable loops** that feel robotic
- **Breaking immersion** with inconsistent behavior

---

## 2. Autonomous Behavior Drivers

### 2.1 Needs-Based System

Inspired by The Sims and RimWorld, NPCs have fundamental needs that drive behavior:

#### Core Needs

```java
public enum NeedType {
    // Physical Needs
    HUNGER("hunger", 100, 0.1f, "food"),
    ENERGY("energy", 100, 0.05f, "rest"),
    SAFETY("safety", 100, 0.0f, "shelter"),

    // Psychological Needs
    SOCIAL("social", 100, 0.02f, "interaction"),
    PURPOSE("purpose", 100, 0.01f, "accomplishment"),
    COMFORT("comfort", 100, 0.03f, "environment"),

    // Work-Specific Needs
    TOOLS("tools", 100, 0.0f, "equipment"),
    MATERIALS("materials", 100, 0.0f, "resources"),
    KNOWLEDGE("knowledge", 100, 0.0f, "information");

    private final String id;
    private final int maxValue;
    private final float decayRate; // per tick
    private final String category;
}
```

#### Need Priority Algorithm

```java
public class NeedSystem {
    /**
     * Calculates urgency score for each need.
     * Higher score = more urgent to address.
     */
    public float calculateUrgency(NeedType need, NeedState state) {
        // Base urgency from depletion level
        float depletionUrgency = 1.0f - (state.getCurrentValue() / need.getMaxValue());

        // Personality modifier - some needs matter more to certain personalities
        float personalityMod = getPersonalityModifier(need);

        // Time-based modifier - needs become more urgent if ignored
        float timeMod = state.getTimeSinceLastAddressed() / 1000f;

        // Random factor for natural variation
        float randomMod = (random.nextFloat() - 0.5f) * 0.1f;

        return depletionUrgency + personalityMod + timeMod + randomMod;
    }
}
```

#### Need-Driven Behavior Selection

```java
public class NeedDrivenBehavior {
    public Optional<Task> findNeedSatisfyingTask(NeedType urgentNeed) {
        return switch (urgentNeed) {
            case HUNGER -> findFoodTask();
            case ENERGY -> findRestTask();
            case SAFETY -> evaluateThreatsAndSeekShelter();
            case SOCIAL -> findSocialInteraction();
            case PURPOSE -> findMeaningfulWork();
            case TOOLS -> findToolAcquisitionTask();
            case MATERIALS -> findResourceGatheringTask();
            default -> Optional.empty();
        };
    }
}
```

### 2.2 Goal-Driven Behavior

NPCs maintain personal goals that provide direction during idle time:

#### Goal Types

| Goal Type | Description | Example Behaviors |
|-----------|-------------|-------------------|
| **Exploration** | Discover new areas | Map territory, find resources |
| **Collection** | Accumulate items | Organize chests, collect rare items |
| **Construction** | Build and improve | Expand base, add decorations |
| **Optimization** | Improve efficiency | Redesign farms, optimize storage |
| **Knowledge** | Learn and document | Explore mechanics, share discoveries |

#### Goal Persistence

```java
public class GoalSystem {
    private List<PersonalGoal> activeGoals = new ArrayList<>();

    public void updateGoals() {
        // Goals persist across sessions
        // Progress tracked in NBT saves
        // New goals emerge based on personality and experience
    }

    public class PersonalGoal {
        private String description;
        private float progress;
        private long createdAt;
        private Priority priority;
        private List<Task> subTasks;
    }
}
```

### 2.3 Routine Systems

NPCs establish daily/weekly routines that create predictable patterns:

#### Time-Based Behavior

```java
public class DailyRoutine {
    /**
     * Defines behavior preferences based on game time.
     * Creates natural daily rhythms.
     */
    public RoutinePhase getCurrentPhase(long gameTime) {
        int dayTime = (int)(gameTime % 24000);

        if (dayTime >= 0 && dayTime < 6000) {
            return RoutinePhase.DAWN;       // Early morning prep
        } else if (dayTime >= 6000 && dayTime < 12000) {
            return RoutinePhase.MORNING;    // Active work period
        } else if (dayTime >= 12000 && dayTime < 14000) {
            return RoutinePhase.MIDDAY;     // Break and socialize
        } else if (dayTime >= 14000 && dayTime < 18000) {
            return RoutinePhase.AFTERNOON;  // Active work period
        } else if (dayTime >= 18000 && dayTime < 22000) {
            return RoutinePhase.EVENING;    // Wrap up, organize
        } else {
            return RoutinePhase.NIGHT;      // Rest, security, planning
        }
    }
}
```

#### Routine Activities by Phase

| Phase | Typical Activities |
|-------|-------------------|
| **DAWN** | Check inventory, plan day, repair tools |
| **MORNING** | High-focus work (mining, building) |
| **MIDDAY** | Social interaction, eat, share progress |
| **AFTERNOON** | Routine maintenance, farming, logistics |
| **EVENING** | Organize storage, document progress |
| **NIGHT** | Security patrol, rest, learn new skills |

### 2.4 Reactive Behaviors

NPCs respond immediately to environmental changes:

#### Reactive Triggers

```java
public class ReactiveBehavior {
    public void onEnvironmentalEvent(EnvironmentEvent event) {
        switch (event.getType()) {
            case HOSTILE_MOB_APPROACHING:
                if (personality.isBrave()) {
                    evaluateCombatCapability();
                } else {
                    seekSafety();
                }
                break;

            case VALUABLE_RESOURCE_DISCOVERED:
                if (personality.isCurious()) {
                    investigateResource();
                } else if (personality.isPragmatic()) {
                    evaluateResourceUtility();
                }
                break;

            case WEATHER_CHANGE:
                adjustActivitiesForWeather(event.getWeatherType());
                break;

            case INVENTORY_FULL:
                organizeStorage();
                break;

            case TOOL_BREAKING:
                planToolRepair();
                break;
        }
    }
}
```

---

## 3. Personality-Driven Autonomy

### 3.1 Personality Model

Personality is defined through trait pairs that create behavior variation:

#### Core Trait Dimensions

```java
public class PersonalityProfile {
    // Each trait is a value from 0.0 to 1.0

    /** Brave (1.0) vs Cautious (0.0) */
    private float bravery;

    /** Curious (1.0) vs Focused (0.0) */
    private float curiosity;

    /** Social (1.0) vs Solitary (0.0) */
    private float sociability;

    /** Creative (1.0) vs Practical (0.0) */
    private float creativity;

    /** Diligent (1.0) vs Relaxed (0.0) */
    private float diligence;

    /** Generous (1.0) vs Selfish (0.0) */
    private float generosity;
}
```

#### Personality Effect Examples

| Trait | Behavior Influence |
|-------|-------------------|
| **Bravery** | Willingness to explore caves, fight mobs, take risks |
| **Curiosity** | Tendency to investigate unknowns, explore new areas |
| **Sociability** | Seeks interaction with other NPCs and player |
| **Creativity** | Prefers novel solutions, decorative building |
| **Diligence** | Thoroughness, work pace, attention to detail |
| **Generosity** | Resource sharing, helping other NPCs |

### 3.2 Personality-Based Decision Making

```java
public class PersonalityDecisionMaker {
    /**
     * Personality modifies task selection probabilities.
     * Different personalities prefer different autonomous activities.
     */
    public float scoreTaskForPersonality(Task task, PersonalityProfile personality) {
        float baseScore = task.getBaseUtility();

        // Personality modifiers
        if (task.getType() == TaskType.EXPLORATION) {
            baseScore *= (0.5f + personality.curiosity);
        }

        if (task.getType() == TaskType.COMBAT) {
            baseScore *= (0.3f + personality.bravery);
        }

        if (task.getType() == TaskType.SOCIAL) {
            baseScore *= (0.3f + personality.sociability);
        }

        if (task.getType() == TaskType.CREATIVE_BUILDING) {
            baseScore *= (0.5f + personality.creativity);
        }

        return baseScore;
    }
}
```

### 3.3 Behavioral Quirks

Each NPC has unique quirks that make them memorable:

#### Quirk System

```java
public class BehavioralQuirks {
    private List<Quirk> quirks = new ArrayList<>();

    public void initializeQuirks(PersonalityProfile personality) {
        // Quirks emerge from personality combinations
        if (personality.diligence > 0.8f && personality.creativity < 0.3f) {
            quirks.add(Quirk.PERFECTIONIST); // Double-checks work
        }

        if (personality.curiosity > 0.8f && personality.bravery < 0.4f) {
            quirks.add(Quirk.CAUTIOUS_EXPLORER); // Explores but cautiously
        }

        if (personality.sociability > 0.7f) {
            quirks.add(Quirk.GOSSIPS); // Shares discoveries enthusiastically
        }
    }
}

public enum Quirk {
    PERFECTIONIST("Double-checks work, takes longer on tasks"),
    CAUTIOUS_EXPLORER("Explores but brings extra supplies"),
    GOSSIPS("Enthusiastically shares information with others"),
    HOARDER("Collects one of everything"),
    MINIMALIST("Keeps inventory light, efficient packer"),
    NIGHT_OWL("Prefers working at night"),
    EARLY_BIRD("Most productive in morning"),
    DREAMER("Idle time spent planning elaborate projects"),
    PRACTICAL("Focuses on immediate utility over decoration"),
    ARTISTIC("Adds decorative touches to everything");
}
```

### 3.4 Emergent Behavior

Complex behavior emerges from simple personality-driven rules:

#### Emergence Example: Resource Discovery

```
1. Curious NPC notices unexplored cave (reactive behavior)
2. Bravery trait evaluated - moderate bravery = cautious approach
3. Prepares with torches, weapons (practical personality influence)
4. Discovers rare resources
5. High generosity = immediately reports to player
6. High sociability = shares with other NPCs
7. High curiosity = wants to explore further
8. Result: Chain of emergent behaviors from simple trait interactions
```

---

## 4. Idle Behavior Systems

### 4.1 Idle State Machine

When not executing player commands, NPCs enter autonomous mode:

```java
public class IdleBehaviorStateMachine {
    public enum IdleState {
        /** Scanning for opportunities and problems */
        SCANNING,

        /** Deciding between multiple options */
        DECIDING,

        /** Executing autonomous task */
        ACTING,

        /** Taking a break */
        RESTING,

        /** Socializing with other NPCs */
        SOCIALIZING,

        /** Improving personal workspace */
        ORGANIZING
    }

    public void tick(ForemanEntity npc) {
        if (!hasPlayerTask()) {
            IdleState currentState = getCurrentState();

            switch (currentState) {
                case SCANNING -> scanForOpportunities(npc);
                case DECIDING -> selectAutonomousTask(npc);
                case ACTING -> executeAutonomousTask(npc);
                case RESTING -> evaluateRestNeed(npc);
                case SOCIALIZING -> findSocialInteraction(npc);
                case ORGANIZING -> improveWorkspace(npc);
            }
        }
    }
}
```

### 4.2 Proactive Task Finding

NPCs actively identify useful work:

#### Opportunity Detection

```java
public class OpportunityScanner {
    /**
     * Scans environment for autonomous work opportunities.
     * Returns scored opportunities based on utility and personality.
     */
    public List<Opportunity> scanOpportunities(ForemanEntity npc) {
        List<Opportunity> opportunities = new ArrayList<>();

        // Check inventory status
        if (isInventoryDisorganized()) {
            opportunities.add(new Opportunity(
                "organize_inventory",
                "Organize inventory chests",
                calculateOrganizationUtility(npc)
            ));
        }

        // Check crop status
        if (hasCropsReadyForHarvest()) {
            opportunities.add(new Opportunity(
                "harvest_crops",
                "Harvest ready crops",
                calculateHarvestUtility(npc)
            ));
        }

        // Check tool durability
        if (hasDamagedTools()) {
            opportunities.add(new Opportunity(
                "repair_tools",
                "Repair damaged tools",
                calculateRepairUtility(npc)
            ));
        }

        // Check for unexplored areas
        if (npc.getPersonality().curiosity > 0.6f) {
            findExplorationOpportunities(opportunities);
        }

        // Check for nearby problems
        identifyEnvironmentalProblems(opportunities);

        return opportunities;
    }
}
```

#### Problem Detection

```java
public class ProblemDetector {
    private static final List<ProblemPattern> PROBLEM_PATTERNS = List.of(
        // Structure problems
        ProblemPattern.blockBroken("critical_structure"),
        ProblemPattern.waterLeak("flooded_area"),
        ProblemPattern.fireNearby("fire_hazard"),

        // Resource problems
        ProblemPattern.lowOnCriticalResource("charcoal"),
        ProblemPattern.storageFull("chest_overflow"),

        // Efficiency problems
        ProblemPattern.inefficientLayout("unorganized_storage"),
        ProblemPattern.unoptimizedFarm("crop_efficiency")
    );

    public void scanForProblems(ForemanEntity npc) {
        for (ProblemPattern pattern : PROBLEM_PATTERNS) {
            if (pattern.matches(npc.getLevel(), npc.blockPosition())) {
                problems.add(pattern.createProblem());
            }
        }
    }
}
```

### 4.3 Self-Improvement Activities

NPCs proactively improve their capabilities:

#### Self-Improvement Types

| Activity | Personality Affinity | Description |
|----------|---------------------|-------------|
| **Organize Inventory** | Diligent, Practical | Sort chests, label containers |
| **Practice Skills** | Diligent | Repeat tasks to improve efficiency |
| **Explore Territory** | Curious, Brave | Map new areas, find resources |
| **Build Workshop** | Creative | Improve personal workspace |
| **Socialize** | Social | Chat with other NPCs, trade tips |
| **Rest** | Relaxed | Take breaks, restore energy |

```java
public class SelfImprovementSystem {
    public Optional<Task> findSelfImprovementActivity(ForemanEntity npc) {
        PersonalityProfile p = npc.getPersonality();

        // Diligent NPCs organize and practice
        if (p.diligence > 0.7f && isInventoryDisorganized()) {
            return createOrganizationTask();
        }

        // Curious NPCs explore
        if (p.curiosity > 0.7f && hasUnexploredAreas()) {
            return createExplorationTask();
        }

        // Creative NPCs build decorative elements
        if (p.creativity > 0.7f && hasWorkspace()) {
            return createDecorationTask();
        }

        // Social NPCs seek interaction
        if (p.sociability > 0.7f && hasOtherNPCsNearby()) {
            return createSocializationTask();
        }

        return Optional.empty();
    }
}
```

### 4.4 Social Idle Behaviors

NPCs interact with each other when idle:

#### Social Interactions

```java
public class SocialBehavior {
    public void handleSocializing(ForemanEntity npc) {
        List<ForemanEntity> nearbyNPCs = findNearbyNPCs(npc, 16.0);

        if (!nearbyNPCs.isEmpty()) {
            ForemanEntity companion = selectCompanion(npc, nearbyNPCs);

            // Move toward companion
            npc.getNavigation().moveTo(companion.blockPosition(), 0.5f);

            // If close enough, interact
            if (distanceTo(npc, companion) < 4.0) {
                performSocialInteraction(npc, companion);
            }
        }
    }

    private void performSocialInteraction(ForemanEntity initiator, ForemanEntity target) {
        // Exchange information about discoveries
        exchangeKnowledge(initiator, target);

        // Share recent accomplishments
        shareRecentWork(initiator, target);

        // Possibly develop friendship (affects future behavior)
        updateRelationship(initiator, target);

        // Chat message about interaction
        initiator.sendChatMessage("Had a nice chat with " + target.getEntityName());
    }
}
```

---

## 5. Industry Inspiration

### 5.1 The Sims - Smart Zoi System (2025)

**Key Takeaways:**
- **Dynamic Behavior Trees**: NPCs adapt to environment changes
- **Life Goals**: Long-term objectives guide autonomous decisions
- **Social Simulation**: Behave like real social populations
- **Personality-Driven**: Extroverts initiate, introverts avoid interactions

**Applicable Concepts:**
```java
public class SimsInspiredNeeds {
    // The Sims uses decay rates + personality modifiers
    public float calculateNeedDecay(NeedType need, PersonalityProfile profile) {
        float baseDecay = need.getBaseDecayRate();

        // Personality affects decay speed
        if (need == NeedType.SOCIAL && profile.sociability > 0.7f) {
            return baseDecay * 1.5f; // Social NPCs need social contact more
        }

        return baseDecay;
    }
}
```

### 5.2 Skyrim - Radiant AI

**Key Takeaways:**
- **24/7 Schedules**: Full daily routines, not just standing around
- **Goal-Based**: Goals achieved through various means
- **Personality Attributes**: "Responsibility" affects moral choices
- **Environmental Awareness**: Reacts to world state

**Applicable Concepts:**
```java
public class RadiantInspiredSchedule {
    public DailySchedule generateSchedule(PersonalityProfile profile, long gameTime) {
        DailySchedule schedule = new DailySchedule();

        // Morning: Work activities
        schedule.addActivity(6000, 12000, List.of(
            new Activity("mining", profile.diligence * 0.8f),
            new Activity("building", profile.creativity * 0.6f)
        ));

        // Midday: Break
        schedule.addActivity(12000, 14000, List.of(
            new Activity("socialize", profile.sociability * 1.0f),
            new Activity("eat", 1.0f) // Everyone eats
        ));

        return schedule;
    }
}
```

### 5.3 RimWorld - Work Priorities

**Key Takeaways:**
- **Manual Priorities**: 1-4 priority system per work type
- **Left-to-Right Execution**: Within priority levels
- **Emergency Work**: Firefighting, doctoring always priority 1
- **Specialization**: Colonists focus on highest-skilled work

**Applicable Concepts:**
```java
public class RimWorldInspiredPriorities {
    private final Map<TaskType, Integer> priorities = new HashMap<>();

    public void setPriority(TaskType type, int priority) {
        // Priority 1 = highest (emergency)
        // Priority 4 = lowest (cleanup)
        priorities.put(type, Math.max(1, Math.min(4, priority)));
    }

    public Task selectBestTask(List<Task> availableTasks) {
        return availableTasks.stream()
            .sorted((a, b) -> {
                int priorityA = priorities.getOrDefault(a.getType(), 4);
                int priorityB = priorities.getOrDefault(b.getType(), 4);

                if (priorityA != priorityB) {
                    return Integer.compare(priorityA, priorityB); // Lower = higher priority
                }

                // Same priority: use skill level as tiebreaker
                return Float.compare(
                    getSkillLevel(b.getType()),
                    getSkillLevel(a.getType())
                );
            })
            .findFirst()
            .orElse(null);
    }
}
```

### 5.4 Dwarf Fortress - Personality & Emergence

**Key Takeaways:**
- **50+ Belief Types**: Complex personality system
- **Needs Override Tasks**: Unmet needs cause work refusal
- **Emotion System**: Emotions influence behavior choices
- **Emergent Narrative**: Stories emerge from simulation

**Applicable Concepts:**
```java
public class DwarfFortressInspiredPersonality {
    public class DwarfPersonality {
        // Core beliefs (value pairs)
        private Map<String, Float> beliefs = new HashMap<>();

        // Needs with stress impact
        private Map<NeedType, Float> needLevels = new HashMap<>();

        // Emotional state
        private Map<String, Float> emotions = new HashMap<>();

        public boolean willWorkOn(Task task) {
            // Check critical needs first
            for (Map.Entry<NeedType, Float> need : needLevels.entrySet()) {
                if (need.getValue() < 0.2f) {
                    // Critical need unmet - refuse work
                    return false;
                }
            }

            // Personality influences task preference
            float beliefAlignment = calculateBeliefAlignment(task);
            return beliefAlignment > 0.3f;
        }
    }
}
```

### 5.5 GOAP (Goal-Oriented Action Planning)

**Key Takeaways:**
- **Backward Planning**: Start from goal, find actions to achieve it
- **World State**: Variables describing environment
- **Action Preconditions**: Requirements before action can execute
- **Action Effects**: Changes to world state after execution

**Applicable Concepts:**
```java
public class GOAPInspiredPlanner {
    public List<Task> planTasksToReachGoal(WorldState current, Goal goal) {
        // A* search backward from goal to current state
        PriorityQueue<PlanNode> openSet = new PriorityQueue<>();
        openSet.add(new PlanNode(goal, 0));

        while (!openSet.isEmpty()) {
            PlanNode current = openSet.poll();

            if (current.state.satisfies(current)) {
                return reconstructPath(current);
            }

            // Find actions that move toward current state
            for (Action action : availableActions) {
                if (action.getEffects().canTransitionTo(current.state)) {
                    WorldState prevState = current.state.applyEffectsReverse(action);
                    openSet.add(new PlanNode(prevState, current.cost + action.getCost()));
                }
            }
        }

        return Collections.emptyList(); // No plan found
    }
}
```

---

## 6. Implementation Architecture

### 6.1 System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      ForemanEntity                               │
│  (Existing - enhanced with autonomy)                             │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│              AutonomousBehaviorManager                           │
│  Coordinates all autonomous behavior systems                    │
└───────────┬─────────────────────────────────┬───────────────────┘
            │                                 │
    ┌───────▼────────┐              ┌─────────▼──────────┐
    │  NeedsSystem   │              │ PersonalitySystem  │
    │ - Hunger       │              │ - Trait values     │
    │ - Energy       │              │ - Quirks           │
    │ - Safety       │              │ - Decision mods    │
    │ - Social       │              │ - Memory effects   │
    └───────┬────────┘              └─────────┬──────────┘
            │                                 │
            └────────────┬────────────────────┘
                         │
                ┌────────▼─────────┐
                │ IdleStateMachine │
                │ - SCANNING       │
                │ - DECIDING       │
                │ - ACTING         │
                │ - RESTING        │
                │ - SOCIALIZING    │
                └────────┬─────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
┌───────▼──────┐  ┌─────▼─────┐  ┌──────▼────────┐
│ Opportunity  │  │  Routine  │  │   Reactive   │
│  Scanner     │  │ System    │  │   Behavior   │
│ - Problems   │  │ - Schedule│  │ - Events     │
│ - Resources  │  │ - Phases  │  │ - Triggers   │
└───────┬──────┘  └─────┬─────┘  └──────┬────────┘
        │               │                │
        └───────────────┼────────────────┘
                        │
                ┌───────▼────────┐
                │ LLMIntegration │
                │ - Refines      │
                │ - Explains     │
                │ - Suggests     │
                └────────────────┘
```

### 6.2 AutonomousBehaviorManager Class

```java
package com.minewright.autonomy;

import com.minewright.entity.ForemanEntity;
import com.minewright.action.Task;

import java.util.Optional;

/**
 * Manages autonomous behavior for crew members.
 *
 * <p>This system enables NPCs to act independently when not given
 * explicit commands, creating the feeling of "fellow players" rather
 * than scripted bots.</p>
 *
 * <p><b>Core Responsibilities:</b></p>
 * <ul>
 *   <li>Coordinate needs, personality, and opportunity systems</li>
 *   <li>Manage idle state machine transitions</li>
 *   <li>Select appropriate autonomous tasks</li>
 *   <li>Balance autonomy with player control</li>
 *   <li>Integrate with LLM for intelligent refinement</li>
 * </ul>
 */
public class AutonomousBehaviorManager {
    private final ForemanEntity foreman;

    // Subsystems
    private final NeedsSystem needsSystem;
    private final PersonalitySystem personalitySystem;
    private final IdleStateMachine idleStateMachine;
    private final OpportunityScanner opportunityScanner;
    private final RoutineSystem routineSystem;
    private final ReactiveBehaviorSystem reactiveSystem;
    private final LLMAutonomyRefiner llmRefiner;

    // Configuration
    private boolean autonomyEnabled = true;
    private float autonomyLevel = 0.5f; // 0.0 = fully controlled, 1.0 = highly autonomous

    public AutonomousBehaviorManager(ForemanEntity foreman) {
        this.foreman = foreman;

        // Initialize subsystems
        this.needsSystem = new NeedsSystem(foreman);
        this.personalitySystem = new PersonalitySystem(foreman);
        this.idleStateMachine = new IdleStateMachine(this);
        this.opportunityScanner = new OpportunityScanner(foreman);
        this.routineSystem = new RoutineSystem(foreman);
        this.reactiveSystem = new ReactiveBehaviorSystem(foreman);
        this.llmRefiner = new LLMAutonomyRefiner(foreman);
    }

    /**
     * Called every tick to update autonomous behavior.
     * Only active when not executing player commands.
     */
    public void tick() {
        if (!autonomyEnabled) return;

        // Check if NPC should be autonomous
        if (foreman.getActionExecutor().isExecuting()) {
            // Has player task - suspend autonomy
            idleStateMachine.transitionTo(IdleState.SUSPENDED);
            return;
        }

        // Check urgent needs first
        Optional<NeedType> urgentNeed = needsSystem.getMostUrgentNeed();
        if (urgentNeed.isPresent()) {
            handleUrgentNeed(urgentNeed.get());
            return;
        }

        // Update idle state machine
        idleStateMachine.tick();

        // Check for reactive events
        reactiveSystem.checkAndRespond();
    }

    /**
     * Handles urgent needs by generating appropriate tasks.
     */
    private void handleUrgentNeed(NeedType need) {
        Optional<Task> task = needsSystem.findNeedSatisfyingTask(need);

        if (task.isPresent()) {
            // Optionally refine with LLM
            Task refinedTask = llmRefiner.refineAutonomousTask(
                task.get(),
                need,
                personalitySystem.getProfile()
            );

            foreman.getActionExecutor().queueTask(refinedTask);

            // Communicate about autonomous action
            if (shouldCommunicateAutonomousAction(need)) {
                foreman.sendChatMessage(getAutonomousActionMessage(need, refinedTask));
            }
        }
    }

    /**
     * Returns the current autonomy configuration.
     */
    public AutonomyConfig getConfig() {
        return new AutonomyConfig(autonomyEnabled, autonomyLevel);
    }

    /**
     * Updates autonomy configuration.
     */
    public void setConfig(AutonomyConfig config) {
        this.autonomyEnabled = config.enabled();
        this.autonomyLevel = config.level();
    }
}
```

### 6.3 NeedsSystem Implementation

```java
package com.minewright.autonomy;

import com.minewright.entity.ForemanEntity;
import com.minewright.action.Task;

import java.util.*;

/**
 * Manages NPC needs and generates need-satisfying tasks.
 *
 * <p>Needs drive behavior similar to The Sims - when needs are low,
 * NPCs prioritize addressing them over other activities.</p>
 */
public class NeedsSystem {
    private final ForemanEntity foreman;

    // Need states
    private final Map<NeedType, NeedState> needStates = new EnumMap<>(NeedType.class);

    // Decay rates (per tick - 20 ticks = 1 second)
    private static final Map<NeedType, Float> DECAY_RATES = Map.of(
        NeedType.HUNGER, 0.01f,
        NeedType.ENERGY, 0.005f,
        NeedType.SOCIAL, 0.002f,
        NeedType.PURPOSE, 0.001f
    );

    public NeedsSystem(ForemanEntity foreman) {
        this.foreman = foreman;

        // Initialize all needs to full
        for (NeedType need : NeedType.values()) {
            needStates.put(need, new NeedState(need.getMaxValue()));
        }
    }

    /**
     * Updates need levels based on decay and recent activities.
     * Called every tick.
     */
    public void tick() {
        for (Map.Entry<NeedType, NeedState> entry : needStates.entrySet()) {
            NeedType need = entry.getKey();
            NeedState state = entry.getValue();

            // Apply decay
            float decay = DECAY_RATES.getOrDefault(need, 0.0f);
            state.decay(decay);

            // Check for critical levels
            if (state.getCurrentValue() < need.getCriticalThreshold()) {
                onNeedCritical(need);
            }
        }
    }

    /**
     * Returns the most urgent need requiring attention.
     * Empty if no needs are urgent.
     */
    public Optional<NeedType> getMostUrgentNeed() {
        return needStates.entrySet().stream()
            .filter(e -> e.getValue().getCurrentValue() < e.getKey().getUrgentThreshold())
            .max(Comparator.comparingDouble(e -> calculateUrgency(e.getKey(), e.getValue())))
            .map(Map.Entry::getKey);
    }

    /**
     * Calculates urgency score for a need.
     * Higher score = more urgent.
     */
    private float calculateUrgency(NeedType need, NeedState state) {
        // Base urgency from depletion
        float depletionUrgency = 1.0f - (state.getCurrentValue() / need.getMaxValue());

        // Time since last addressed (needs become more urgent if ignored)
        float timeUrgency = state.getTimeSinceLastAddressed() / 1000f;

        return depletionUrgency * 2.0f + timeUrgency;
    }

    /**
     * Generates a task to satisfy the given need.
     */
    public Optional<Task> findNeedSatisfyingTask(NeedType need) {
        return switch (need) {
            case HUNGER -> findFoodTask();
            case ENERGY -> findRestTask();
            case SAFETY -> findSafetyTask();
            case SOCIAL -> findSocialTask();
            case PURPOSE -> findPurposeTask();
            case TOOLS -> findToolAcquisitionTask();
            case MATERIALS -> findMaterialGatheringTask();
            case KNOWLEDGE -> findLearningTask();
        };
    }

    /**
     * Called when a need reaches critical level.
     * May trigger autonomous behavior or warnings.
     */
    private void onNeedCritical(NeedType need) {
        // Alert player if autonomy is disabled
        if (!foreman.getAutonomyManager().getConfig().enabled()) {
            foreman.sendChatMessage("I'm really needing to " + need.getName().toLowerCase() + "...");
        }
    }

    public void satisfyNeed(NeedType need, float amount) {
        NeedState state = needStates.get(need);
        state.restore(amount);
        state.markAsAddressed();
    }

    public NeedState getNeedState(NeedType need) {
        return needStates.get(need);
    }
}
```

### 6.4 PersonalitySystem Implementation

```java
package com.minewright.autonomy;

import com.minewright.entity.ForemanEntity;
import com.minewright.action.Task;

import java.util.*;

/**
 * Manages NPC personality and its effect on behavior.
 *
 * <p>Personality is defined through trait pairs (bravery/cautious,
 * curiosity/focus, etc.) that modify all autonomous decisions.</p>
 */
public class PersonalitySystem {
    private final ForemanEntity foreman;

    // Core personality profile
    private PersonalityProfile profile;

    // Behavioral quirks
    private final Set<Quirk> quirks = new HashSet<>();

    // Personality-based preferences
    private final Map<TaskType, Float> taskPreferences = new HashMap<>();

    public PersonalitySystem(ForemanEntity foreman) {
        this.foreman = foreman;

        // Generate or load personality
        this.profile = loadOrCreateProfile();
        this.quirks.addAll(determineQuirks(profile));
        initializeTaskPreferences();
    }

    /**
     * Generates a personality profile.
     * Uses random generation biased toward interesting personalities.
     */
    private PersonalityProfile generateProfile() {
        Random random = new Random();

        return new PersonalityProfile(
            random.nextFloat(), // bravery
            random.nextFloat(), // curiosity
            random.nextFloat(), // sociability
            random.nextFloat(), // creativity
            random.nextFloat(), // diligence
            random.nextFloat()  // generosity
        );
    }

    /**
     * Determines quirks based on personality trait combinations.
     * Quirks create memorable, unique behaviors.
     */
    private Set<Quirk> determineQuirks(PersonalityProfile profile) {
        Set<Quirk> quirks = new HashSet<>();

        // Trait combinations create quirks
        if (profile.diligence > 0.8f && profile.creativity < 0.3f) {
            quirks.add(Quirk.PERFECTIONIST);
        }

        if (profile.curiosity > 0.8f && profile.bravery < 0.4f) {
            quirks.add(Quirk.CAUTIOUS_EXPLORER);
        }

        if (profile.sociability > 0.7f && profile.generosity > 0.6f) {
            quirks.add(Quirk.GOSSIPS);
        }

        // Add one random quirk for uniqueness
        Quirk[] allQuirks = Quirk.values();
        quirks.add(allQuirks[new Random().nextInt(allQuirks.length)]);

        return quirks;
    }

    /**
     * Scores a task based on personality preferences.
     * Used during autonomous task selection.
     */
    public float scoreTaskForPersonality(Task task) {
        float baseScore = 1.0f;

        // Apply personality modifiers
        TaskType type = task.getType();

        if (type == TaskType.EXPLORATION) {
            baseScore *= (0.3f + profile.curiosity * 0.7f);
        }

        if (type == TaskType.COMBAT) {
            baseScore *= (0.2f + profile.bravery * 0.8f);
        }

        if (type == TaskType.SOCIAL) {
            baseScore *= (0.2f + profile.sociability * 0.8f);
        }

        if (type == TaskType.CREATIVE_BUILDING) {
            baseScore *= (0.3f + profile.creativity * 0.7f);
        }

        if (type == TaskType.ORGANIZATION) {
            baseScore *= (0.3f + profile.diligence * 0.7f);
        }

        // Apply quirk modifiers
        for (Quirk quirk : quirks) {
            baseScore *= quirk.getTaskModifier(type);
        }

        return baseScore;
    }

    /**
     * Modifies task parameters based on personality.
     * Different personalities approach tasks differently.
     */
    public Task applyPersonalityToTask(Task task) {
        // Bravery affects risk-taking
        if (profile.bravery < 0.5f && task.isRisky()) {
            task = task.withSafetyPrecautions(true);
        }

        // Diligence affects thoroughness
        if (profile.diligence > 0.7f) {
            task = task.withThoroughness(Thoroughness.HIGH);
        }

        // Creativity affects approach
        if (profile.creativity > 0.7f && task.getType() == TaskType.BUILDING) {
            task = task.withDecorativeElements(true);
        }

        return task;
    }

    public PersonalityProfile getProfile() {
        return profile;
    }

    public Set<Quirk> getQuirks() {
        return Collections.unmodifiableSet(quirks);
    }
}
```

---

## 7. LLM Integration

### 7.1 LLM's Role in Autonomy

The LLM enhances, rather than drives, autonomous behavior:

| LLM Function | Description | Example |
|--------------|-------------|---------|
| **Refinement** | Adds personality to autonomous tasks | "Organize chests" → "Sort chests by material type" |
| **Explanation** | Provides reasoning for actions | "I'm reorganizing because I like efficiency" |
| **Suggestion** | Proposes interesting activities | "I noticed a cave we could explore" |
| **Variation** | Adds natural variation to routines | Different approaches to same task |

### 7.2 Prompt Templates

#### Autonomous Task Refinement

```
You are {name}, a Minecraft worker NPC with these personality traits:
- Bravery: {bravery}
- Curiosity: {curiosity}
- Sociability: {sociability}
- Creativity: {creativity}
- Diligence: {diligence}

You have decided to autonomously perform this task:
{base_task_description}

Refine this task to reflect your personality. Add specific details
about how you would approach it differently based on your traits.

Return JSON:
{
  "refined_task": "specific task description",
  "reasoning": "why this approach fits your personality",
  "variations": ["alternative approaches considered"]
}
```

#### Proactive Opportunity Suggestion

```
You are {name}, an autonomous Minecraft worker.

Current situation:
{current_context}
- Nearby resources: {resources}
- Nearby structures: {structures}
- Time of day: {time}
- Recent accomplishments: {recent_work}

Your personality: {personality_summary}

Suggest 1-3 proactive activities you could do right now that would
be useful and match your personality. Be specific about what you'd
do and why.

Return JSON:
{
  "opportunities": [
    {
      "task": "what to do",
      "utility": "why it's useful",
      "personality_match": "how it fits your personality",
      "confidence": 0.0-1.0
    }
  ]
}
```

### 7.3 LLMAutonomyRefiner Implementation

```java
package com.minewright.autonomy;

import com.minewright.entity.ForemanEntity;
import com.minewright.action.Task;
import com.minewright.llm.OpenAIClient;
import com.minewright.llm.PromptBuilder;

import java.util.concurrent.CompletableFuture;

/**
 * Uses LLM to refine and explain autonomous behavior.
 *
 * <p>The LLM adds personality-driven variation and reasoning to
 * autonomous decisions, making NPCs feel more intelligent and
 * individual.</p>
 */
public class LLMAutonomyRefiner {
    private final ForemanEntity foreman;
    private final OpenAIClient llmClient;

    public LLMAutonomyRefiner(ForemanEntity foreman) {
        this.foreman = foreman;
        this.llmClient = OpenAIClient.getInstance();
    }

    /**
     * Refines an autonomous task using LLM.
     * Adds personality-specific details and reasoning.
     */
    public Task refineAutonomousTask(
        Task baseTask,
        NeedType motivatingNeed,
        PersonalityProfile profile
    ) {
        // Build refinement prompt
        String prompt = buildRefinementPrompt(baseTask, motivatingNeed, profile);

        // Call LLM asynchronously (non-blocking)
        CompletableFuture<String> future = llmClient.completeAsync(prompt);

        // Don't block - return base task if LLM isn't ready
        if (!future.isDone()) {
            return baseTask;
        }

        try {
            String response = future.get();
            return parseRefinedTask(response, baseTask);
        } catch (Exception e) {
            // LLM failed - use base task
            return baseTask;
        }
    }

    /**
     * Generates proactive opportunity suggestions.
     * Called when NPC is scanning for things to do.
     */
    public List<Opportunity> suggestOpportunities(
        WorldContext context,
        PersonalityProfile profile
    ) {
        String prompt = buildOpportunityPrompt(context, profile);

        // Async call - may not be ready immediately
        CompletableFuture<String> future = llmClient.completeAsync(prompt);

        if (!future.isDone()) {
            // Return rule-based opportunities if LLM not ready
            return generateRuleBasedOpportunities(context);
        }

        try {
            String response = future.get();
            return parseOpportunities(response);
        } catch (Exception e) {
            return generateRuleBasedOpportunities(context);
        }
    }

    private String buildRefinementPrompt(Task task, NeedType need, PersonalityProfile profile) {
        return String.format("""
            You are %s, a Minecraft worker NPC.

            Your personality:
            - Bravery: %.1f (1.0 = very brave, 0.0 = very cautious)
            - Curiosity: %.1f
            - Sociability: %.1f
            - Creativity: %.1f
            - Diligence: %.1f

            You are motivated by: %s

            The task you've chosen: %s

            Refine this task to reflect your personality. How would YOU
            specifically approach this task given your traits?

            Return JSON with "refined_task" and "reasoning" fields.
            """,
            foreman.getEntityName(),
            profile.bravery,
            profile.curiosity,
            profile.sociability,
            profile.creativity,
            profile.diligence,
            need.getName(),
            task.getDescription()
        );
    }
}
```

---

## 8. Testing and Validation

### 8.1 Testing Strategy

| Test Type | Description | Success Criteria |
|-----------|-------------|------------------|
| **Unit Tests** | Test individual systems in isolation | All components work independently |
| **Integration Tests** | Test system interactions | Autonomous behaviors flow correctly |
| **Simulation Tests** | Long-running autonomous behavior | NPCs remain productive over time |
| **Player Experience** | Real-world playtesting | NPCs feel like helpful companions |

### 8.2 Validation Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Autonomy Frequency** | 30-50% of time idle | Time spent on autonomous vs. player tasks |
| **Player Satisfaction** | 4+ / 5 | Player surveys on NPC helpfulness |
| **Task Interference** | < 5% | Autonomous tasks conflicting with player goals |
| **Communication Overload** | < 2 messages/min | Autonomous action chat spam |
| **Behavioral Variety** | 5+ distinct autonomous actions | Different activities observed |

### 8.3 Test Scenarios

#### Scenario 1: Idle Behavior Discovery
```
Setup: Spawn NPC in established base with no commands
Expected: NPC identifies and performs useful autonomous task
Verify: Task is appropriate, communicated clearly, can be interrupted
```

#### Scenario 2: Need-Driven Behavior
```
Setup: NPC with low hunger need, no player task
Expected: NPC prioritizes finding food
Verify: Need addressed before other autonomous activities
```

#### Scenario 3: Personality Variation
```
Setup: Spawn 5 NPCs with different personalities
Expected: Different autonomous behaviors observed
Verify: Brave NPCs explore, cautious NPCs organize, etc.
```

#### Scenario 4: Player Task Interruption
```
Setup: NPC working on autonomous task
Action: Player gives explicit command
Expected: Autonomous task immediately cancelled
Verify: Smooth transition to player task
```

#### Scenario 5: Social Interaction
```
Setup: 2+ NPCs idle near each other
Expected: NPCs interact and share information
Verify: Social behavior, knowledge exchange, relationship building
```

---

## Conclusion

This autonomous behavior system enables MineWright NPCs to feel like genuine companions rather than simple tools. By combining:

- **Needs-based motivation** (The Sims-inspired)
- **Personality-driven variation** (Dwarf Fortress-inspired)
- **Daily routines** (Skyrim-inspired)
- **Work prioritization** (RimWorld-inspired)
- **LLM-enhanced intelligence**

We create NPCs that:
- Proactively identify and solve problems
- Exhibit individual quirks and preferences
- Communicate intentions naturally
- Respect player authority
- Create emergent, memorable moments

The system is modular, allowing gradual implementation and tuning based on player feedback. Each subsystem can work independently or together for increasingly sophisticated autonomous behavior.

---

## References and Sources

### Industry Research
- **inZOI Smart Zoi System** - [Sohu Article](https://m.sohu.com/a/883723627_122001005/)
- **Skyrim Radiant AI** - [TV Tropes](https://tvtropes.pmwiki.pmwiki.php/Main/ArtificialBrilliance)
- **RimWorld Work Priorities** - [RimWorld Wiki](https://rimworldwiki.com/wiki/Work)
- **Dwarf Fortress Personality System** - [Dwarf Fortress Wiki](http://dwarffortresswiki.org/index.php/Dwarf_Fortress)
- **GOAP in Games** - [GDC Vault 2025](https://gdcvault.com/play/1035576/Game-AI-Summit-Combining-GOAP)

### Academic Research
- **Multi-Agent Systems with LLMs** - [arXiv March 2025](https://arxiv.org/html/2503.03800v1)
- **LLM Emergent Abilities** - [arXiv Paper](https://arxiv.org/pdf/2206.07682.pdf)
- **Emotional NPC Behavior** - [Baidu Academic](https://xueshu.baidu.com/usercenter/paper/show?paperid=6cd81f1431182c04354ed3c91cf04ee5)

### Game Development Resources
- **Behavior Trees for Game AI** - [CSDN Guide](https://blog.csdn.net/2501_91474102/article/details/153854652)
- **UE5 AI Programming** - [CSDN Column](https://wenku.csdn.net/column/7ipz077aiw)
- **Advanced AI for Games** - [Udemy Course](https://www.udemy.com/course/behaviour-trees/)

---

**Document Version:** 1.0
**Last Updated:** 2025-02-28
**Next Review:** After initial implementation phase
