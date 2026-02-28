# Chapter 3: RPG and Adventure Games
## AI Techniques for Autonomous Agents (1990-2025)

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Classic RPG AI Foundations](#2-classic-rpg-ai-foundations)
3. [Package-Based Behavior Systems](#3-package-based-behavior-systems)
4. [Need/Urge-Based Decision Systems](#4-needurge-based-decision-systems)
5. [Companion AI Evolution](#5-companion-ai-evolution)
6. [Quest State Machines](#6-quest-state-machines)
7. [Dialog and Relationship Systems](#7-dialog-and-relationship-systems)
8. [NPC Daily Life Simulation](#8-npc-daily-life-simulation)
9. [Extractable Patterns for Minecraft](#9-extractable-patterns-for-minecraft)
10. [Case Studies](#10-case-studies)
11. [Implementation Patterns](#11-implementation-patterns)
12. [Bibliography](#12-bibliography)

---

## 1. Introduction

Role-playing games (RPGs) and adventure games have pioneered some of the most sophisticated AI techniques in game development. From the early schedule systems of Ultima in the 1990s to the radiant AI of modern open-world games, these genres have consistently pushed the boundaries of autonomous agent behavior.

This chapter explores the evolution of RPG AI from 1990 to 2025, focusing on techniques that enable autonomous agents without requiring large language models. The patterns and systems discussed are directly applicable to creating intelligent companions in Minecraft and similar sandbox environments.

### Key Themes

1. **Schedule-based autonomy** - NPCs with daily routines and predictable behaviors
2. **Goal-oriented planning** - Agents that pursue objectives through available actions
3. **Need-based motivation** - Behavior driven by internal states (hunger, fatigue, social)
4. **Relationship tracking** - Dynamic social networks that influence decision-making
5. **Conditional programming** - Player-configurable AI behaviors through simple rules
6. **Quest state management** - Tracking complex multi-stage objectives

---

## 2. Classic RPG AI Foundations

### 2.1 Ultima Series: The Birth of Living Worlds (1980s-1990s)

The Ultima series, created by Richard Garriott and Origin Systems, pioneered the concept of NPCs with autonomous daily schedules. This innovation fundamentally changed how players perceived game worlds.

#### Ultima V: Warriors of Destiny (1988)

**Key Innovation**: First major RPG to implement comprehensive NPC schedules

- NPCs had defined daily routines: sleeping at night, working during the day, eating meals
- Shopkeepers would close shops at evening and return home
- Guards would transform from regular citizens during the day to sentries at night
- Created the illusion of a "living world" that existed independently of the player

#### Ultima VI: The False Prophet (1990)

**Technical Improvements**:
- More sophisticated schedule system with time-based transitions
- NPCs would physically walk between locations (home to shop, shop to tavern)
- Environmental interaction (sitting in chairs, sleeping in beds)
- Conversations could reference NPC current activities

```pseudo
// Ultima-style schedule pseudocode
class NPCSchedule {
    int hour;          // Game time (0-23)
    string location;   // Map coordinate/room
    string action;     // WALK, STAND, SIT, SLEEP
    string direction;  // Facing direction
}

schedule ExampleMerchant = {
    {0, "house_bedroom", "SLEEP", "NORTH"},
    {6, "house_bedroom", "STAND", "NORTH"},
    {7, "house_kitchen", "SIT", "EAST"},
    {8, "shop_counter", "WALK", "PATH_TO_MARKET"},
    {9, "shop_counter", "STAND", "NORTH"},
    {12, "tavern_table", "WALK", "PATH_TO_TAVERN"},
    {13, "tavern_table", "SIT", "SOUTH"},
    {18, "shop_counter", "WALK", "PATH_TO_MARKET"},
    {20, "house_bedroom", "WALK", "PATH_TO_HOME"},
    {22, "house_bedroom", "SLEEP", "NORTH"}
}
```

#### Ultima VII: The Black Gate (1992)

**Revolutionary Features**:
- Seamless world without separate map screens
- Every object in the world was interactive
- NPCs would use objects autonomously (eating food, sitting in chairs)
- **Emergent behavior**: NPCs could interact with each other unexpectedly

**Development Philosophy**: Create a "true living world" where:
- NPCs have realistic daily routines
- Every object is interactive
- The world persists without player intervention

**Legacy Influence**:
- Inspired the immersive sim genre (Thief, System Shock, Deus Ex)
- Influenced Grand Theft Auto and Skyrim's open-world design
- Set the standard for sandbox RPG environments

---

### 2.2 Baldur's Gate: Script-Driven Party AI (1998)

BioWare's Baldur's Gate introduced **BGScript**, a sophisticated scripting system that powered party AI and NPC behaviors through conditional logic.

#### BGScript Architecture

**Core Characteristics**:
- If-then conditional statements trigger behaviors
- Stack-based evaluation (similar to Reverse Polish Notation)
- Limited mathematical operations but sufficient for behavioral logic
- Built-in commands for attack, defense, retreat, cover, spellcasting

```pseudo
// BGScript-style conditional behavior
IF (See(Enemy))
    AND (Enemy.Allegiance == Evil)
    AND (Self.HP > 50%)
    THEN Attack(Enemy)

IF (Self.HP < 30%)
    AND (HasItem(HealingPotion))
    THEN UseItem(HealingPotion)

IF (See(Enemy))
    AND (Enemy.Is(Spellcaster))
    AND (CanCast(DispelMagic))
    THEN Cast(DispelMagic, Enemy)
```

#### Development Philosophy: "Don't Make It Too Smart"

**The "Too Intelligent" Problem**:
- Initial AI was so effective it nearly "ruined the party"
- Kobold archers in Nashkel Mines would tactically target spellcasters
- Players found the game too difficult with optimal AI
- Solution: Deliberately make AI "dumber" for balanced gameplay

**Character-Driven Design**:
- Each character views the world differently based on personality
- Alignment (good/evil) and race influence strategic AI
- Character flaws create behavioral variations (e.g., cowardly warriors)
- Stats directly affect decision-making (low Intelligence = poor tactical choices)

#### Script Collections and Modding

**Enhanced Powergaming Scripts**:
- Player-created script collections for BG2: Enhanced Edition
- Allow players to focus on tactics while AI handles nuances
- Often paired with "Sword Coast Stratagems" mod for enhanced enemy AI

**Technical Legacy**:
- Infinity Engine influenced decades of CRPGs
- GemRB project continues the architecture as open-source
- Script-based AI patterns still used in modern games

---

### 2.3 Fallout: Companion Behavior Systems (1997-2015)

The Fallout series developed increasingly sophisticated companion AI systems that balanced autonomy with player control.

#### Fallout 1 & 2 (Interplay, 1997-1998)

**Companion Commands**:
- Simple command system: Wait, Follow, Distance settings
- Inventory management: Give/take items, weapon preferences
- Basic combat AI: Attack nearest enemy, use best weapon
- **No tactical control** - companions acted independently

**Behavior Characteristics**:
- Companions would engage enemies on sight
- Could accidentally hit player or allies (friendly fire)
- Permadeath created high stakes
- Personality reflected through dialogue, not behavior

#### Fallout 3 & New Vegas (Bethesda/Obsidian, 2008-2010)

**Enhanced System**:
- Radiant AI integration (see Section 3.3)
- Companion wheel interface for tactical commands
- affinity system tracking relationship with player
- Command categories: Combat, Inventory, Movement, Dialogue

**Affinity System** (New Vegas):
- Relationship value (-1000 to +1000)
- Actions affect affinity: healing, dialogue choices, faction choices
- High affinity unlocks perks and dialogue options
- Different companions value different behaviors

```pseudo
// New Vegas affinity tracking
struct CompanionAffinity {
    int currentValue;
    map<string, int> eventModifiers;

    void ModifyAffinity(string event) {
        currentValue += eventModifiers[event];
        CheckAffinityThresholds();
    }

    void CheckAffinityThresholds() {
        if (currentValue >= 750) UnlockPerk();
        if (currentValue >= 1000) UnlockSpecialEnding();
    }
}

// Event examples
affinity[CASS] = {
    {"HealPlayer", +5},
    {"KillFiend", +2},
    {"StealFromNCR", -10},
    {"WearNCRArmor", +5}
}
```

#### Fallout 4 (2015)

**Squad Commands System**:
- Direct tactical control: Go there, Attack that, Pick up that
- Companion trade interface
- Romance system with companion-specific quests
- Unique companion perks based on affinity

**Technical Improvements**:
- Pathfinding improvements in vertical environments
- Cover-seeking behavior
- Weapon switching based on range/positioning
- Dynamic dialogue during gameplay

---

## 3. Package-Based Behavior Systems

Package-based systems define behavior through modular "instruction packages" that NPCs follow. This approach allows for flexible, reusable behavior definitions.

### 3.1 Elder Scrolls: Radiant AI (2006-Present)

Radiant AI, introduced in *The Elder Scrolls IV: Oblivion*, represented a revolutionary approach to NPC behavior using goal-oriented packages rather than rigid scripts.

#### Core Architecture

**Design Philosophy**:
- Define **what** NPCs should do (goals)
- Let AI determine **how** to achieve those goals
- NPCs interact dynamically with environment objects
- Behavior emerges from package combinations

**Technical Components**:

1. **Packages**: Instruction sets defining NPC goals
   - "Sleep in this bed from 10pm to 6am"
   - "Eat food twice a day"
   - "Wander in this area for 4 hours"

2. **Needs System**: Internal drives that push behavior
   - Hunger
   - Fatigue
   - Social interaction desire
   - Acquisitiveness (desire for items)

3. **Environmental Awareness**: NPCs evaluate surroundings
   - Locate nearest available object matching need
   - Navigate to object
   - Use object appropriately

4. **Personality Variables**: Individual behavior modifiers
   - Aggression
   - Confidence
   - Responsibility (controls illegal behavior)
   - Energy levels

```pseudo
// Radiant AI Package Structure
class AIPackage {
    string packageType; // WANDER, SLEEP, EAT, USE_OBJECT
    TimeRange schedule;
    Location targetLocation;
    ObjectReference targetObject;
    int priority;
    Flags flags; // CONTINUE_IF_ATTACKED, ALERT_IF_DANGER, etc.
}

class NPC {
    List<AIPackage> packages;
    Map<NeedType, float> currentNeeds;

    void EvaluatePackages() {
        // Sort packages by priority and current needs
        packages.Sort((a, b) =>
            CalculateUrgency(a) <=> CalculateUrgency(b)
        );

        // Execute highest-urgency package
        packages[0].Execute();
    }

    float CalculateUrgency(AIPackage package) {
        float urgency = package.priority;
        foreach (need in package.FulfilledNeeds) {
            urgency += (100 - currentNeeds[need]) * need.weight;
        }
        return urgency;
    }
}
```

#### The "Wild" Oblivion Implementation (2006)

**Experimental Features**:
- **Directive**: "Get what you want by any means necessary"
- No constraints on illegal behavior
- NPCs would steal, fight, and murder to satisfy needs
- Created emergent chaos but unpredictable gameplay

**Famous Emergent Behaviors**:
- NPCs murdering each other over food
- Characters taking 3-hour detours to return home
- Entire town populations wiped out by NPC-on-NPC violence
- Skooma addicts stealing everything in sight

```pseudo
// Oblivion-style unconstrained behavior
IF (Need(HUNGER) > 80) {
    IF (HasOwnedFood()) {
        Eat(OwnedFood());
    } ELSE IF (SeesUnownedFood()) {
        Take(UnownedFood()); // Can be theft!
        Eat(TakenFood());
    } ELSE IF (SeesNPCWithFood()) {
        StealFrom(NPCWithFood()); // Can lead to combat!
        Eat(StolenFood());
    }
}
```

#### Skyrim Refined System (2011)

**Improvements and Constraints**:

| Oblivion (2006) | Skyrim (2011) |
|-----------------|---------------|
| Unrestrained goal-pursuit | Added "responsibility" attribute |
| Chaotic emergent behaviors | More realistic, constrained choices |
| NPCs could murder for items | NPCs consider consequences before stealing |
| Full 24/7 simulation | Distance-based optimization |
| Complex pathfinding | Simplified navigation logic |

**Responsibility Attribute**:
```pseudo
// Skyrim-style constrained behavior
IF (Need(HUNGER) > 80) {
    IF (HasOwnedFood()) {
        Eat(OwnedFood());
    } ELSE IF (responsibility > 50 && SeesUnownedFood()) {
        // High responsibility: won't steal
        WaitForBetterOption();
    } ELSE IF (responsibility <= 50 && SeesUnownedFood()) {
        // Low responsibility: might steal
        IF (DetectionRisk() < 50) {
            Take(UnownedFood());
        }
    }
}
```

**Technical Optimizations**:
- NPCs outside player radius use simplified packages
- Pathfinding only calculated for visible NPCs
- Package evaluation occurs on intervals, not every frame

#### Fallout 3/4/Skyrim: Radiant Story System

**Radiant Story** (introduced in Skyrim, expanded in later games):
- Dynamic quest generation based on player actions
- Selects appropriate locations, NPCs, and rewards
- Adapts to player level and skills
- Creates "infinite" procedural content

**Example Flow**:
1. Player kills a bandit chief
2. System identifies "avenge chief" narrative hook
3. Selects appropriate dungeon location
4. Assigns bandit NPCs to location
5. Generates quest with appropriate rewards
6. Tracks completion and updates world state

```pseudo
// Radiant Story pseudocode
class RadiantQuestGenerator {
    Quest GenerateQuest(PlayerAction trigger, Player player) {
        // Select appropriate quest template
        QuestTemplate template = SelectTemplate(trigger.type);

        // Find valid location
        Location loc = FindValidLocation(
            template.requiredLocationType,
            player.currentPosition,
            template.maxDistance
        );

        // Populate with appropriate NPCs
        List<NPC> npcs = SpawnNPCs(
            template.enemyFaction,
            template.enemyCount,
            player.level
        );

        // Generate rewards based on player level
        List<Item> rewards = GenerateRewards(
            template.rewardTypes,
            player.level,
            template.rewardQuality
        );

        return Quest(template, loc, npcs, rewards);
    }
}
```

---

### 3.2 Package Design Patterns

#### Time-Based Packages

```pseudo
// Time-driven behavior switching
Package Daytime {
    Type: WANDER
    Time: 8:00 - 18:00
    Location: TownCenter
    Radius: 500 units
}

Package Nighttime {
    Type: SLEEP
    Time: 22:00 - 6:00
    Location: Home
    Target: Bed
}

Package Work {
    Type: USE_OBJECT
    Time: 9:00 - 17:00
    Location: Shop
    Target: Anvil
    Animation: Smithing
}
```

#### Conditional Packages

```pseudo
// Condition-triggered packages
Package Flee {
    Type: FLEE
    Condition: HP < 30%
    Target: NearestGuard
    Priority: CRITICAL
}

Package Combat {
    Type: ATTACK
    Condition: HasTarget && HP > 30%
    Target: CurrentEnemy
    Priority: HIGH
}

Package Patrol {
    Type: PATROL
    Condition: NoTarget && HP > 50%
    Path: PatrolRoute
    Priority: NORMAL
}
```

#### Multi-Goal Packages

```pseudo
// Package combining multiple goals
Package DailyRoutine {
    Goals: [
        {
            Type: SLEEP
            Need: REST
            Required: 8 hours
            Window: 22:00 - 6:00
        },
        {
            Type: EAT
            Need: HUNGER
            Required: 2 meals
            Window: 6:00 - 20:00
        },
        {
            Type: SOCIALIZE
            Need: SOCIAL
            Required: 2 hours
            Window: 12:00 - 14:00
        },
        {
            Type: WORK
            Need: PRODUCTIVITY
            Required: 8 hours
            Window: 8:00 - 18:00
        }
    ]
}
```

---

## 4. Need/Urge-Based Decision Systems

Need-based systems model AI behavior through internal drives that must be satisfied through interaction with the world.

### 4.1 The Sims: Autonomous Behavior Through Needs (2000-Present)

The Sims series pioneered sophisticated need-based AI that creates compelling emergent behavior without complex scripting.

#### Core Needs System

**Primary Needs** (The Sims 1-2):
- Hunger - Food requirements
- Comfort - Physical comfort level
- Hygiene - Cleanliness
- Bladder - Bathroom needs
- Energy - Sleep requirements
- Fun - Entertainment needs
- Social - Interaction requirements
- Room - Environment quality

**Need Mechanics**:
- Each need has a value from 0 (desperate) to 100 (fully satisfied)
- Needs decay over time at different rates
- Critical needs (near 0) create overwhelming urges
- Multiple low needs create decision conflicts

```pseudo
// Sims-style needs calculation
class NeedsSystem {
    map<NeedType, float> currentNeeds;
    map<NeedType, float> decayRates;

    void Update(float deltaTime) {
        foreach (need in currentNeeds.Keys) {
            currentNeeds[need] -= decayRates[need] * deltaTime;
            currentNeeds[need] = Clamp(currentNeeds[need], 0, 100);
        }
    }

    Object SelectAction() {
        // Calculate attractiveness score for each available object
        map<Object, float> objectScores;

        foreach (obj in visibleObjects) {
            float score = 0;

            // Score based on how much it helps our lowest needs
            foreach (action in obj.availableActions) {
                foreach (need in action.satisfiedNeeds) {
                    float needUrgency = (100 - currentNeeds[need.type]) / 100.0;
                    score += needUrgency * action.satisfactionAmount;
                }
            }

            // Distance penalty (further objects are less attractive)
            score /= (1 + DistanceTo(obj) * 0.01);

            objectScores[obj] = score;
        }

        // Intentionally NOT always choosing the best option
        // This creates "clumsy" behavior that needs player guidance
        return SelectWeightedRandom(objectScores);
    }
}
```

#### Intentional Imperfection Design

**"Don't Let Players Go Idle" Philosophy**:
- AI deliberately imperfect to maintain player engagement
- Randomness added to decisions (not purely optimal)
- Originally had more realistic behavior removed (e.g., privacy etiquette)
- Creates "drama" through poor decision-making

**Example Design Decisions**:
- Originally: Male Sims avoid urinals when others nearby
- Changed: Removed privacy rules to create awkward situations
- Result: Players feel needed to guide Sims

**Free Will Toggle**:
- When OFF: Sims stand idle until player commands
- When ON: Sims act autonomously but may ignore player
- Balance between automation and player control

**Technical Architecture**:
- Agent-based artificial life program
- Behavior trees for complex decision-making
- Continuous need recalculation
- Environment object interaction through affordances

```pseudo
// Behavior tree for autonomous action
Selector AutonomousBehavior {
    Sequence CriticalNeeds {
        Condition AnyNeedBelow(25)
        Action SatisfyLowestNeed()
    }

    Sequence ActiveGoals {
        Condition HasActiveGoal()
        Action PursueGoal()
    }

    Sequence RandomActivity {
        Action SelectFromTopPriorities(3)
        Action ExecuteActivity()
    }

    Action Idle()
}
```

---

### 4.2 Animal Crossing: Villager Behavior System (2001-Present)

Animal Crossing uses a dual-behavior system combining personality traits with contextual reactions.

#### Dual Behavior Architecture

**1. Intrinsic Behavior Logic** (Personality-driven):
- Each villager has one of 8 personality types
- Personality determines action frequency and preferences
- Music-lovers sing more often
- Book-lovers read more frequently

**2. Situational Behavior Logic** (Context-driven):
- Villagers react to environmental stimuli
- See bug → Might chase or might ignore (based on personality)
- Weather affects activities
- Time of day influences behavior choices

```pseudo
// Animal Crossing behavior system
class VillagerAI {
    PersonalityType personality;
    map<Activity, float> activityWeights;

    void Initialize() {
        // Base weights from personality
        activityWeights = GetPersonalityWeights(personality);

        // Adjust for individual variation
        foreach (activity in activityWeights.Keys) {
            activityWeights[activity] *= Random(0.8, 1.2);
        }
    }

    Activity SelectActivity() {
        // Get current context
        Context context = GetCurrentContext();

        // Calculate score for each activity
        map<Activity, float> scores;

        foreach (activity in AllActivities()) {
            float score = activityWeights[activity];

            // Context modifiers
            if (context.IsRaining() && activity == Indoors) {
                score *= 2.0;
            }

            if (context.IsMorning() && activity == Exercise) {
                score *= 1.5;
            }

            if (Sees(activity.TriggerObject())) {
                score *= 3.0; // React to stimuli
            }

            scores[activity] = score;
        }

        return SelectHighest(scores);
    }
}
```

#### Villager Personality System

**8 Personality Types** (New Horizons):
- Normal, Lazy, Smug, Snooty, Cranky, Jock, Peppy, Sisterly

**Personality Characteristics**:
| Personality | Activity Preference | Sleep Schedule | Social Style |
|-------------|-------------------|----------------|--------------|
| Lazy | Sleeping, relaxing | Late sleeper | Laid-back |
| Jock | Exercise, competition | Early riser | Energetic |
| Snooty | Fashion, gossip | Normal | Haughty |
| Cranky | Complaining, collecting | Normal | Grumpy |
| Peppy | Singing, playing | Early | Hyperactive |
| Normal | Varied interests | Normal | Friendly |

**Frequency Tuning**:
- Developers adjust behavior frequency to simulate "free will"
- Same personality, different behavior patterns through weight variations
- Creates ~400 unique villagers from limited base behaviors

#### Memory and Relationship Systems

**Villager Memory**:
- Remember past interactions with player
- Gift-giving affects relationship
- Time since last interaction tracked
- Relationship level changes dialogue and behavior options

```pseudo
// Relationship tracking system
class RelationshipSystem {
    map<Villager, float> friendshipLevels;
    map<Villager, List<Interaction>> interactionHistory;

    void RecordInteraction(Villager v, InteractionType type) {
        interactionHistory[v].Add(new Interaction(type, currentTime));

        // Update friendship based on interaction
        float impact = GetFriendshipImpact(type);
        friendshipLevels[v] = Clamp(friendshipLevels[v] + impact, -100, 100);

        // Decay very old interactions
        PruneOldInteractions(v);
    }

    float GetFriendshipImpact(InteractionType type) {
        switch (type) {
            case GIFT: return +5;
            case CONVERSATION: return +1;
            case INSULT: return -10;
            case CATCH_BUG_FOR: return +3;
            case HIT_WITH_NET: return -5;
            default: return 0;
        }
    }

    List<DialogueOption> GetDialogueOptions(Villager v) {
        if (friendshipLevels[v] < 20) {
            return [IntroductionDialogue];
        } else if (friendshipLevels[v] < 50) {
            return [CasualDialogue, AskFavorDialogue];
        } else {
            return [CloseFriendDialogue, SecretDialogue, GiftRequestDialogue];
        }
    }
}
```

---

### 4.3 Stardew Valley: NPC Scheduling (2016)

Stardew Valley combines rigid scheduling with dynamic event triggers to create believable NPCs.

#### Schedule File Format

**Data Structure** (stored in `Content/Characters/schedules/*.xnb`):
```
schedule_key:
    location_map tile_x tile_y facing_direction animation_frame
    ...
```

**Schedule Keys** (determining triggers):
- `spring` / `summer` / `fall` / `winter`
- `rain` / `windy` / `not_raining`
- `marriage_<character>` (married to specific character)
- `Marriage` (married to player)
- Custom events (festivals, heart events)

**Priority Order**:
1. Special schedules (events, festivals)
2. Marriage schedules
3. Normal daily schedules

```pseudo
// Stardew Valley schedule parsing
class ScheduleParser {
    Schedule LoadSchedule(string npcName, GameContext context) {
        string scheduleFile = "Characters/schedules/" + npcName;

        // Determine which schedule key to use
        string scheduleKey = DetermineScheduleKey(context);

        Schedule schedule = new Schedule();

        if (HasSpecialSchedule(npcName, context)) {
            schedule = LoadSpecialSchedule(npcName, context);
        } else if (IsMarriedToPlayer(npcName)) {
            schedule = LoadMarriageSchedule(scheduleFile);
        } else {
            schedule = LoadDailySchedule(scheduleFile, scheduleKey);
        }

        return schedule;
    }

    string DetermineScheduleKey(GameContext context) {
        List<string> keys = [];

        keys.Add(context.season.ToLower());

        if (context.isRaining) {
            keys.Add("rain");
        } else {
            keys.Add("not_raining");
        }

        // Keys are checked in order
        return keys.Join("_");
    }
}

// Schedule execution
class ScheduleExecutor {
    List<ScheduleEntry> entries;
    int currentEntry = 0;

    void Update(int gameTime) {
        // Find appropriate entry for current time
        while (currentEntry < entries.Count &&
               entries[currentEntry].time <= gameTime) {

            MoveToLocation(entries[currentEntry].location,
                          entries[currentEntry].tile,
                          entries[currentEntry].facing);

            PlayAnimation(entries[currentEntry].animation);

            currentEntry++;
        }
    }
}
```

#### Path Following System

**Technical Implementation**:
- NPCs use A* pathfinding to navigate between schedule points
- Path cached for efficiency
- NPCs pause if player blocks path
- Special handling for doors, stairs, and warp points

```pseudo
// Path execution with interruption handling
class NPCMovement {
    Queue<Vector2> pathQueue;
    ScheduleEntry currentTarget;
    bool isWaitingForPlayer = false;

    void Update() {
        if (isWaitingForPlayer) {
            // Wait for player to move out of the way
            if (!IsPlayerBlockingPath()) {
                isWaitingForPlayer = false;
            }
            return;
        }

        if (pathQueue.Count > 0) {
            Vector2 nextStep = pathQueue.Peek();

            if (CanMoveTo(nextStep)) {
                MoveTo(nextStep);
                pathQueue.Dequeue();
            } else if (IsPlayerBlocking(nextStep)) {
                isWaitingForPlayer = true;
            } else {
                // Recalculate path if blocked by something else
                RecalculatePath();
            }
        } else {
            // Reached destination
            ExecuteScheduleAction(currentTarget);
        }
    }
}
```

#### Dynamic Behavior Modifications

**Heart Level System**:
- Heart level affects dialogue and behavior
- At high heart levels: NPCs may follow player, visit farmhouse
- Marriage changes schedule entirely (moves into player's house)
- Special events trigger at specific heart thresholds

**Weather-Dependent Schedules**:
- Rain: Indoor activities, stays home more
- Storm: Most NPCs stay indoors
- Festival: All NPCs attend during festival hours

---

## 5. Companion AI Evolution

### 5.1 Conditional Programming Systems

Two groundbreaking systems pioneered player-programmable AI: Final Fantasy XII's Gambit System (2006) and Dragon Age's Tactics System (2009).

#### Final Fantasy XII: Gambit System (2006)

**Revolutionary Concept**: Players "program" their party AI through conditional rules

**System Features**:
- Up to 12 conditional instructions per character
- 200+ available gambit conditions and actions
- Priority-based execution (top to bottom)
- Progressive unlocking (start with 2 slots, unlock more)

```pseudo
// Gambit syntax
IF <condition> THEN <action>

// Example gambit configuration
GambitList PartyMember = [
    {Priority: 1,  Condition: "Ally: HP < 30%", Action: "Cast: Cure"},
    {Priority: 2,  Condition: "Ally: Status: Poison", Action: "Cast: Antidote"},
    {Priority: 3,  Condition: "Enemy: Weak: Fire", Action: "Cast: Fira"},
    {Priority: 4,  Condition: "Ally: HP < 50%", Action: "Cast: Cura"},
    {Priority: 5,  Condition: "Enemy: Flying", Action: "Attack: Bow"},
    {Priority: 6,  Condition: "Enemy: HP < 20%", Action: "Attack: Kill"},
    {Priority: 7,  Condition: "Self: MP < 10%", Action: "Item: Ether"},
    {Priority: 8,  Condition: "Leader: Attacking", Action: "Attack: Same Target"},
    {Priority: 9,  Condition: "Enemy: Visible", Action: "Attack: Nearest"},
    {Priority: 10, Condition: "None", Action: "Defend"}
]
```

**Execution Logic**:
```pseudo
void ExecuteGambits(Character character) {
    foreach (gambit in character.GambitList) {
        if (EvaluateCondition(gambit.condition, character)) {
            ExecuteAction(gambit.action, character);
            break; // Only execute one gambit per tick
        }
    }
}
```

**Implementation Challenges**:
- Originally designed for PS2 hardware constraints
- Programmer Takashi Katano: "Wow, you want me to do this on a PS2?"
- Required efficient condition evaluation
- Memory constraints limited number of gambits

**Influence on Game Design**:
- Allowed players to complete battles "hands-free" with optimized gambits
- Created meta-game of gambit configuration optimization
- Made game accessible to action-game beginners
- Praised as "one of JRPGs' best ideas in years"

**Legacy**:
- Dragon Age: Origins directly inspired by Gambit system
- Influenced numerous indie RPGs
- Final Fantasy VII Rebirth aimed to "surpass" FF12's system
- Created new genre: "programming games"

#### Dragon Age: Tactics System (2009)

**Dragon Age: Origins** built upon FF12's foundation with enhanced features.

**6 Behavior Modes** (Engagement Style):

| Mode | Counter | Chase | Attack Visible | Ranged | Melee | Avoid Melee | Avoid AoE |
|------|---------|-------|----------------|--------|-------|-------------|-----------|
| Default | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | ✓ |
| Passive | ✓ | ✗ | ✗ | ✓ | ✗ | ✓ | ✓ |
| Aggressive | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ |
| Ranged | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✓ |
| Cautious | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ |
| Defensive | ✓ | ✗ | ✗ | ✗ | ✓ | ✗ | ✓ |

**Conditional Tactics Slots**:
```pseudo
// Tactics slot structure
TacticSlot {
    int priority;        // 1-N (checked in order)
    Condition condition; // When to execute
    Action action;       // What to execute
    Target target;       // Who to target
}

// Example tactics configuration
TacticsList[] Healer = [
    {priority: 1, condition: "Self: HP < 25%", action: "Use: Health Potion", target: "Self"},
    {priority: 2, condition: "Ally: HP < 50%", action: "Cast: Heal", target: "Lowest HP% Ally"},
    {priority: 3, condition: "Ally: Status: Paralyzed", action: "Cast: Cleansing", target: "Affected Ally"},
    {priority: 4, condition: "Enemy: Casting", action: "Cast: Mind Blast", target: "Casting Enemy"},
    {priority: 5, condition: "Ally: Attacking", action: "Attack", target: "Same Target"},
    {priority: 6, condition: "Enemy: Visible", action: "Attack", target: "Nearest Enemy"}
]
```

**Execution Flow**:
```
1. Start at Tactic #1 (highest priority)
2. Check if current condition is met
3. If NO → Move to next tactic
4. If YES → Execute action
5. After execution → Return to Tactic #1
6. Continue loop until combat ends
```

**Slot Unlocks**:
- More tactics slots unlock as characters level up
- "Combat Tactics" skill increases available slots
- Allows character progression to increase AI complexity

**BioWare Developer Quote**:
> "We were inspired by Final Fantasy XII's Gambit system. It allowed players to essentially 'program' their party members' AI through conditional logic."

**Evolution in Dragon Age: Inquisition**:
- Featured in "Game AI Pro 3" as case study
- Utility scoring architecture for behavior decisions
- More sophisticated enemy roles within faction systems

---

### 5.2 Mass Effect: Squad Commands and AI (2007-2012)

#### Mass Effect 1 (2007)

**Squad Mechanics**:
- Direct control of one character at a time
- Other two squadmates use AI behavior
- Commands: Move there, Attack this, Use power

**AI Limitations**:
- "Not very intelligent" power usage decisions
- Examples of poor decisions:
  - Casting Sabotage on already-sabotaged enemies
  - Using Lift on already-lifted targets
  - Wasting Adrenaline Burst before other abilities

**Recommendations from Community**:
- Set automatic power use to "defensive only"
- Or disable it completely for manual control
- Queue powers before combat for better timing

**Positioning Issues**:
- 3D to 2D screen conversion causes pathfinding problems
- Squadmates often ignore cover position commands
- No order queuing (new commands interrupt current actions)

#### Mass Effect 3 (2012) Improvements

**Christina Norman (Gameplay Designer)**:
> "Enemies behave more complexly, responding dynamically to situations. We treat enemies as a unified force where each individual has their own role."

**Improvements**:
- Enemy complexity increases with difficulty
- Higher difficulties unlock new behaviors
- Each enemy has individual roles within faction
- Teammate AI significantly improved from previous games
- Less "hide behind cover and shoot" behavior

**Technical Changes**:
- Better enemy coordination
- Role-specific behaviors (tanks, support, rushers)
- Dynamic response to player tactics
- Improved power usage logic

---

### 5.3 F.E.A.R.: GOAP System (2005)

While not an RPG, F.E.A.R.'s Goal-Oriented Action Planning system heavily influenced RPG companion AI development.

**Core Innovation**:
- Planning-based AI instead of finite state machines
- AI figures out actions themselves
- Emergent squad tactics without explicit scripting

**Key Features**:
- AI soldiers form impromptu squads based on proximity
- Complex tactics emerge naturally from goal system
- Coordinates flanking maneuvers without explicit commands
- Uses planning algorithms to determine action sequences

**GOAP Architecture**:
```pseudo
// GOAP components
class Goal {
    string name;
    map<string, bool> desiredState;
    float priority;
}

class Action {
    string name;
    map<string, bool> preconditions;
    map<string, bool> effects;
    float cost;
}

class Planner {
    List<Action> Plan(Goal goal, WorldState currentState) {
        // Use A* to find action sequence
        return AStar(currentState, goal.desiredState, availableActions);
    }
}
```

**Behavior Example**:
```pseudo
// Example: Enemy wants to kill player
Goal goal = {
    name: "EliminatePlayer",
    desiredState: {playerAlive: false},
    priority: 1.0
}

// Planner generates sequence:
Action[] plan = [
    {action: "TakeCover", preconditions: {inCover: false}, effects: {inCover: true}},
    {action: "Suppress", preconditions: {inCover: true, hasAmmo: true}, effects: {playerPinned: true}},
    {action: "SignalFlank", preconditions: {hasAllies: true}, effects: {alliesFlanking: true}},
    {action: "Attack", preconditions: {playerPinned: true}, effects: {playerHP: -10}}
]
```

**Influence on RPGs**:
- GOAP adopted by numerous RPG and action games
- Foundation for more sophisticated companion AI
- Demonstrated value of planning over scripting

---

## 6. Quest State Machines

Quest systems rely heavily on state machines to track progress, conditions, and completion.

### 6.1 Quest State Architecture

**Standard Quest States**:
```pseudo
enum QuestState {
    NOT_STARTED,
    ACCEPTED,
    IN_PROGRESS,
    OBJECTIVES_PENDING,
    OBJECTIVES_COMPLETE,
    READY_TO_TURN_IN,
    COMPLETED,
    FAILED
}

class QuestStateMachine {
    QuestState currentState;
    List<QuestObjective> objectives;

    void Update() {
        switch (currentState) {
            case NOT_STARTED:
                // Waiting for player to accept quest
                break;

            case ACCEPTED:
                currentState = IN_PROGRESS;
                InitializeObjectives();
                break;

            case IN_PROGRESS:
                UpdateObjectives();
                if (AllObjectivesComplete()) {
                    currentState = READY_TO_TURN_IN;
                }
                break;

            case READY_TO_TURN_IN:
                // Waiting for player to return to quest giver
                break;

            case COMPLETED:
                GrantRewards();
                Cleanup();
                break;

            case FAILED:
                HandleFailure();
                break;
        }
    }
}
```

### 6.2 Objective System

**Objective Types**:
- Kill enemies (slay X creatures)
- Collect items (gather X items)
- Reach location (go to X place)
- Talk to NPC (speak with X person)
- Perform action (use X item, activate X object)
- Time-based (complete before X time)
- Conditional (triggered by event)

```pseudo
abstract class QuestObjective {
    string description;
    int targetCount;
    int currentCount;
    bool isOptional;

    abstract bool IsComplete();
    abstract void OnEvent(GameEvent e);
}

class KillObjective : QuestObjective {
    string enemyType;

    void OnEvent(GameEvent e) {
        if (e.type == ENEMY_KILLED && e.enemyType == enemyType) {
            currentCount++;
        }
    }

    bool IsComplete() {
        return currentCount >= targetCount;
    }
}

class CollectionObjective : QuestObjective {
    string itemId;

    void OnEvent(GameEvent e) {
        if (e.type == ITEM_ACQUIRED && e.itemId == itemId) {
            currentCount++;
        }
    }

    bool IsComplete() {
        return currentCount >= targetCount;
    }
}
```

### 6.3 Quest Triggers and Conditions

**Trigger Types**:
```pseudo
// Location trigger
class LocationTrigger {
    Vector3 position;
    float radius;
    Quest quest;

    void Update() {
        if (Distance(player.position, position) < radius) {
            quest.Activate();
        }
    }
}

// Dialog trigger
class DialogTrigger {
    NPC npc;
    string dialogOption;
    Quest quest;

    void OnDialogSelected(string option) {
        if (option == dialogOption) {
            quest.Activate();
        }
    }
}

// Event trigger
class EventTrigger {
    EventType eventType;
    Quest quest;

    void OnEvent(GameEvent e) {
        if (e.type == eventType) {
            quest.Activate();
        }
    }
}

// Condition-based activation
class QuestCondition {
    virtual bool Evaluate() = 0;
}

class LevelCondition : QuestCondition {
    int requiredLevel;

    bool Evaluate() {
        return player.level >= requiredLevel;
    }
}

class ItemCondition : QuestCondition {
    string requiredItem;

    bool Evaluate() {
        return player.HasItem(requiredItem);
    }
}

class QuestPrerequisiteCondition : QuestCondition {
    string prerequisiteQuest;

    bool Evaluate() {
        return QuestManager.GetQuest(prerequisiteQuest).IsCompleted();
    }
}
```

### 6.4 Dynamic Quest Generation

**Radiant Story-style Quest Generation**:
```pseudo
class DynamicQuestGenerator {
    Quest GenerateQuest(
        string questType,
        Player player,
        Location origin
    ) {
        // Select template
        QuestTemplate template = SelectTemplate(questType);

        // Find appropriate location
        Location target = FindLocation(
            template.locationType,
            origin,
            template.maxDistance
        );

        // Generate objectives
        List<QuestObjective> objectives = GenerateObjectives(
            template,
            player.level,
            target
        );

        // Select appropriate rewards
        List<Item> rewards = GenerateRewards(
            template.rewardTypes,
            player.level,
            template.difficulty
        );

        // Create quest
        return Quest(
            template.name + " " + target.name,
            objectives,
            rewards,
            target
        );
    }
}
```

---

## 7. Dialog and Relationship Systems

### 7.1 Relationship Tracking Systems

**Relationship Model**:
```pseudo
class RelationshipManager {
    map<NPC, float> relationshipValues; // -100 to +100
    map<NPC, List<Interaction>> interactionHistory;

    void ModifyRelationship(NPC npc, float amount, InteractionType type) {
        relationshipValues[npc] = Clamp(
            relationshipValues[npc] + amount,
            -100, 100
        );

        interactionHistory[npc].Add(new Interaction(
            type, currentTime, amount
        ));

        // Trigger events at thresholds
        CheckRelationshipThresholds(npc);
    }

    void CheckRelationshipThresholds(NPC npc) {
        float value = relationshipValues[npc];

        if (value >= 100 && !npc.IsMaxFriendship()) {
            npc.UnlockMaxFriendshipEvent();
        } else if (value >= 75 && !npc.IsHighFriendship()) {
            npc.UnlockHighFriendshipEvent();
        } else if (value <= -100 && !npc.IsMaxRivalry()) {
            npc.UnlockMaxRivalryEvent();
        }
    }

    DialogOptions GetAvailableDialog(NPC npc) {
        float value = relationshipValues[npc];

        if (value < -50) {
            return [HostileDialog];
        } else if (value < 0) {
            return [NeutralDialog, BasicTradeDialog];
        } else if (value < 50) {
            return [FriendlyDialog, TradeDialog, SmallTalkDialog];
        } else {
            return [CloseFriendDialog, SpecialRequestDialog, GiftDialog];
        }
    }
}
```

### 7.2 Dialog State Machines

**Dialog Tree Architecture**:
```pseudo
class DialogNode {
    string speaker;
    string text;
    List<DialogOption> options;
    DialogAction onEnter;
    DialogAction onExit;
}

class DialogOption {
    string text;
    DialogNode nextNode;
    Condition showCondition;
    Action onSelectAction;
}

class DialogStateMachine {
    DialogNode currentNode;

    void StartDialog(DialogNode startNode) {
        currentNode = startNode;
        currentNode.onEnter.Execute();
        ShowDialog(currentNode);
    }

    void SelectOption(int optionIndex) {
        DialogOption selected = currentNode.options[optionIndex];

        if (selected.onSelectAction != null) {
            selected.onSelectAction.Execute();
        }

        currentNode = selected.nextNode;

        if (currentNode != null) {
            currentNode.onEnter.Execute();
            ShowDialog(currentNode);
        } else {
            EndDialog();
        }
    }
}
```

### 7.3 Conditional Dialog

**Dynamic Dialog Options**:
```pseudo
class ConditionalDialogOption : DialogOption {
    List<Condition> conditions;

    bool IsAvailable() {
        foreach (condition in conditions) {
            if (!condition.Evaluate()) {
                return false;
            }
        }
        return true;
    }
}

// Example conditions
class QuestCondition : Condition {
    string questId;

    bool Evaluate() {
        return QuestManager.HasQuest(questId);
    }
}

class SkillCondition : Condition {
    string skillId;
    int requiredLevel;

    bool Evaluate() {
        return player.GetSkillLevel(skillId) >= requiredLevel;
    }
}

class ItemCondition : Condition {
    string itemId;

    bool Evaluate() {
        return player.HasItem(itemId);
    }
}

class RelationshipCondition : Condition {
    NPC npc;
    float minimumRelationship;

    bool Evaluate() {
        return RelationshipManager.GetValue(npc) >= minimumRelationship;
    }
}
```

---

## 8. NPC Daily Life Simulation

### 8.1 24-Hour Schedule Systems

**Comprehensive Schedule Format**:
```pseudo
class DailySchedule {
    List<ScheduleEntry> entries;

    ScheduleEntry GetEntryForTime(int hour, int minute) {
        int timeValue = hour * 60 + minute;

        foreach (entry in entries) {
            if (entry.startTime <= timeValue && entry.endTime > timeValue) {
                return entry;
            }
        }

        return defaultEntry;
    }
}

class ScheduleEntry {
    int startTime;    // In minutes from midnight
    int endTime;
    string activity;
    Location location;
    Vector3 position;
    Direction facing;
    string animation;
    List<string> allowedInteractions;
}

// Example schedule
Schedule ExampleBlacksmith = [
    {start: 0, end: 360, activity: "Sleep", location: "Home", position: bed},
    {start: 360, end: 420, activity: "Wake", location: "Home", position: bedside},
    {start: 420, end: 480, activity: "Breakfast", location: "Home", position: table},
    {start: 480, end: 540, activity: "Walk", location: "Street", path: toShop},
    {start: 540, end: 720, activity: "Work", location: "Shop", position: anvil},
    {start: 720, end: 780, activity: "Lunch", location: "Tavern", position: table},
    {start: 780, end: 1080, activity: "Work", location: "Shop", position: anvil},
    {start: 1080, end: 1140, activity: "Walk", location: "Street", path: toHome},
    {start: 1140, end: 1200, activity: "Dinner", location: "Home", position: table},
    {start: 1200, end: 1320, activity: "Relax", location: "Home", position: chair},
    {start: 1320, end: 1440, activity: "Sleep", location: "Home", position: bed}
]
```

### 8.2 Weather and Context Modifiers

**Dynamic Schedule Switching**:
```pseudo
class ScheduleManager {
    Schedule baseSchedule;
    map<Weather, Schedule> weatherOverrides;
    map<Season, Schedule> seasonOverrides;
    map<string, Schedule> eventOverrides;

    Schedule GetCurrentSchedule(GameContext context) {
        // Check event overrides first
        foreach (event in context.activeEvents) {
            if (eventOverrides.ContainsKey(event.id)) {
                return eventOverrides[event.id];
            }
        }

        // Check weather overrides
        if (weatherOverrides.ContainsKey(context.weather)) {
            return weatherOverrides[context.weather];
        }

        // Check season overrides
        if (seasonOverrides.ContainsKey(context.season)) {
            return seasonOverrides[context.season];
        }

        return baseSchedule;
    }
}
```

### 8.3 Social Scheduling

**NPC Interactions**:
```pseudo
class SocialSchedule {
    List<SocialAppointment> appointments;

    void AddAppointment(string time, NPC other, string activity) {
        appointments.Add(new SocialAppointment(time, other, activity));
    }

    bool HasAppointmentNow(int currentTime, NPC other) {
        foreach (apt in appointments) {
            if (apt.time == currentTime && apt.other == other) {
                return true;
            }
        }
        return false;
    }
}

// Example: NPCs meeting for lunch
SocialAppointment lunchAppointment = {
    time: 720, // 12:00 PM
    participants: [Blacksmith, Innkeeper],
    location: Tavern,
    activity: "Socialize"
}
```

---

## 9. Extractable Patterns for Minecraft

### 9.1 Companion Following AI

**Pattern: Follow Player with Intelligent Positioning**

```pseudo
class CompanionFollowAI {
    Entity owner;
    Entity target;
    float followDistance = 3.0;
    float maxDistance = 10.0;
    float teleportDistance = 50.0;

    void Update() {
        float distance = Distance(owner.position, target.position);

        if (distance > teleportDistance) {
            // Teleport if too far away
            TeleportToTarget();
        } else if (distance > maxDistance) {
            // Path towards target
            PathTowards(target.position);
        } else if (distance < followDistance) {
            // Too close - wait or move aside
            MaintainDistance();
        } else {
            // At good distance - look at target
            owner.LookAt(target.position);
        }
    }

    void PathTowards(Vector3 targetPos) {
        // Calculate path using Minecraft pathfinding
        Path path = CalculatePath(owner.position, targetPos);

        // Follow path, avoiding obstacles
        if (path.HasNext()) {
            Vector3 nextStep = path.Next();

            // Avoid blocks that would suffocate
            if (IsSafePosition(nextStep)) {
                owner.MoveTo(nextStep);
            }
        }
    }

    bool IsSafePosition(Vector3 pos) {
        Block head = world.GetBlock(pos + Vector3.up);
        Block feet = world.GetBlock(pos);
        Block below = world.GetBlock(pos - Vector3.up);

        // Head must be air or non-solid
        // Feet must be air or water
        // Below must be solid
        return (!head.IsSolid() &&
                (!feet.IsSolid() || feet.IsWater()) &&
                below.IsSolid());
    }
}
```

**Advanced Following Features**:

1. **Smart Positioning**: Stay to player's side, not directly behind
```pseudo
Vector3 GetFollowOffset() {
    // Determine best side based on player look direction
    Vector3 playerForward = target.GetForward();
    Vector3 playerRight = target.GetRight();

    // Alternate sides to avoid getting stuck
    int side = frameCount % 2; // 0 = right, 1 = left

    return (playerRight * (side == 0 ? 1 : -1) +
            playerForward * -followDistance);
}
```

2. ** obstacle Avoidance**: Navigate around walls and barriers
```pseudo
void NavigateAroundObstacle(Vector3 targetPos) {
    RaycastResult ray = Raycast(owner.position, targetPos);

    if (ray.hit && ray.distance < Distance(owner.position, targetPos)) {
        // Obstacle detected - find waypoint around it
        Vector3 waypoint = FindWaypointAround(ray.hitPoint, targetPos);
        owner.MoveTo(waypoint);
    }
}

Vector3 FindWaypointAround(Vector3 obstacle, Vector3 target) {
    // Try to find path around by checking adjacent positions
    List<Vector3> candidates = [
        obstacle + Vector3.right,
        obstacle + Vector3.left,
        obstacle + Vector3.forward,
        obstacle + Vector3.back,
        obstacle + Vector3.up // Try climbing
    ];

    foreach (pos in candidates) {
        if (IsSafePosition(pos) && HasLineOfSight(pos, target)) {
            return pos;
        }
    }

    // If no path found, go around obstacle at distance
    Vector3 aroundDir = (obstacle - owner.position).normalized;
    return obstacle + aroundDir * 5;
}
```

3. **Portal/Dimension Tracking**: Follow player through nether portals
```pseudo
void OnPlayerChangedDimension(Player player, Dimension from, Dimension to) {
    if (owner.dimension == from) {
        // Schedule teleport to player's new dimension
        Schedule(() => {
            owner.ChangeDimension(to, player.position);
        }, 1 second);
    }
}
```

---

### 9.2 Need-Based Behavior Triggers

**Pattern: Hunger System for Minecraft Companion**

```pseudo
class MinecraftCompanionNeeds {
    map<NeedType, float> needs;

    MinecraftCompanionNeeds() {
        needs = {
            {HUNGER, 100},
            {ENERGY, 100},
            {SOCIAL, 50},
            {FUN, 50},
            {SAFETY, 100}
        };
    }

    void Update(float deltaTime) {
        // Decay needs over time
        needs[HUNGER] -= 0.5 * deltaTime;      // Lose hunger
        needs[ENERGY] -= 0.3 * deltaTime;      // Get tired
        needs[SOCIAL] -= 0.2 * deltaTime;      // Loneliness
        needs[FUN] -= 0.4 * deltaTime;         // Boredom
        needs[SAFETY] = CalculateSafety();     // Based on nearby enemies

        // Clamp values
        foreach (need in needs.Keys) {
            needs[need] = Clamp(needs[need], 0, 100);
        }

        // Trigger behaviors based on critical needs
        CheckCriticalNeeds();
    }

    float CalculateSafety() {
        // Check for nearby hostile mobs
        List<Entity> nearbyHostiles = GetNearbyEntities(10, "Hostile");

        if (nearbyHostiles.Count == 0) {
            return 100; // Safe
        } else {
            return 100 - (nearbyHostiles.Count * 20);
        }
    }

    void CheckCriticalNeeds() {
        NeedType mostCritical = GetMostCriticalNeed();

        if (needs[mostCritical] < 20) {
            TriggerBehavior(mostCritical);
        }
    }

    NeedType GetMostCriticalNeed() {
        NeedType lowest = HUNGER;
        float minValue = needs[HUNGER];

        foreach (need in needs.Keys) {
            if (needs[need] < minValue) {
                minValue = needs[need];
                lowest = need;
            }
        }

        return lowest;
    }

    void TriggerBehavior(NeedType need) {
        switch (need) {
            case HUNGER:
                if (HasFood()) {
                    EatFood();
                } else {
                    AskPlayerForFood();
                }
                break;

            case ENERGY:
                FindBed();
                break;

            case SOCIAL:
                MoveNearPlayer();
                break;

            case SAFETY:
                if (HasWeapon()) {
                    EquipWeapon();
                    LookForEnemies();
                } else {
                    RunToPlayer();
                }
                break;

            case FUN:
                DoRandomActivity();
                break;
        }
    }
}
```

**Integration with Steve AI**:
```pseudo
class SteveAI {
    MinecraftCompanionNeeds needs;
    TaskPlanner planner;

    void tick() {
        needs.Update(0.05); // 1 tick = 0.05 seconds

        // If critical need, override current task
        NeedType critical = needs.GetMostCriticalNeed();
        if (needs.needs[critical] < 25) {
            Task urgentTask = CreateUrgentTask(critical);
            planner.SetPriorityTask(urgentTask);
        }
    }

    Task CreateUrgentTask(NeedType need) {
        switch (need) {
            case HUNGER:
                return Task("EatFood", Priority: URGENT);
            case SAFETY:
                return Task("FleeToPlayer", Priority: CRITICAL);
            case ENERGY:
                return Task("Sleep", Priority: HIGH);
            default:
                return Task("Satisfy" + need, Priority: NORMAL);
        }
    }
}
```

---

### 9.3 Relationship Evolution System

**Pattern: Dynamic Relationship Tracking**

```pseudo
class CompanionRelationship {
    float affection;      // 0-100
    float respect;        // 0-100
    float trust;          // 0-100
    List<InteractionEvent> history;

    void RecordEvent(InteractionType type, float impact) {
        history.Add(new InteractionEvent(currentTime, type, impact));

        // Update relationship values based on event
        switch (type) {
            case GIVE_FOOD:
                affection += impact;
                trust += impact * 0.5;
                break;

            case PROTECT_IN_COMBAT:
                respect += impact;
                trust += impact;
                break;

            case ABANDON_IN_DANGER:
                trust -= impact * 2;
                respect -= impact;
                break;

            case SHARE_SPOILS:
                respect += impact * 0.5;
                affection += impact * 0.3;
                break;

            case GIVE_GIFT:
                affection += impact;
                respect += impact * 0.2;
                break;

            case COMPLETE_TASK_TOGETHER:
                respect += impact * 0.3;
                trust += impact * 0.3;
                break;
        }

        // Clamp values
        affection = Clamp(affection, 0, 100);
        respect = Clamp(respect, 0, 100);
        trust = Clamp(trust, 0, 100);
    }

    int GetRelationshipLevel() {
        float avg = (affection + respect + trust) / 3;

        if (avg < 20) return 1;      // Stranger
        if (avg < 40) return 2;      // Acquaintance
        if (avg < 60) return 3;      // Friend
        if (avg < 80) return 4;      // Good Friend
        return 5;                    // Best Friend
    }

    float GetObedienceChance() {
        // Higher trust = higher obedience
        return 0.3 + (trust / 100) * 0.7;
    }

    float GetFollowDistance() {
        // Higher affection = follow closer
        return 10.0 - (affection / 100) * 7.0;
    }

    bool WillProtectPlayer() {
        // Willingness to protect based on respect and trust
        return (respect > 50 && trust > 50);
    }
}
```

---

### 9.4 Daily Routines for Minecraft NPCs

**Pattern: Time-Based Activity System**

```pseudo
class MinecraftDailySchedule {
    List<ScheduleEntry> entries;
    int currentTime; // 0-23999 (ticks in day)

    void Update() {
        currentTime = (int)(world.TimeOfDay * 24000) % 24000;

        ScheduleEntry entry = GetEntryForTime(currentTime);
        ExecuteEntry(entry);
    }

    ScheduleEntry GetEntryForTime(int time) {
        foreach (entry in entries) {
            if (time >= entry.startTick && time < entry.endTick) {
                return entry;
            }
        }
        return defaultEntry;
    }

    void ExecuteEntry(ScheduleEntry entry) {
        switch (entry.activity) {
            case "Sleep":
                if (HasBed()) {
                    MoveTo(bedPosition);
                    SleepInBed();
                }
                break;

            case "Work":
                MoveTo(workPosition);
                PerformWorkActivity();
                break;

            case "Wander":
                WanderNear(homePosition, wanderRadius);
                break;

            case "Socialize":
                FindNearbyCompanions();
                Socialize();
                break;
        }
    }
}

// Example farmer schedule
Schedule FarmerSchedule = [
    {start: 0, end: 2000, activity: "Sleep"},
    {start: 2000, end: 3000, activity: "Wander", location: farm},
    {start: 3000, end: 6000, activity: "Work", location: farmCrops},
    {start: 6000, end: 8000, activity: "Socialize", location: village},
    {start: 8000, end: 12000, activity: "Work", location: farmCrops},
    {start: 12000, end: 14000, activity: "Eat", location: house},
    {start: 14000, end: 16000, activity: "Wander", location: farm},
    {start: 16000, end: 24000, activity: "Sleep"}
]
```

**Weather-Aware Scheduling**:
```pseudo
Schedule GetScheduleForConditions() {
    if (world.IsRaining()) {
        return rainSchedule; // Stay indoors more
    } else if (world.IsNight()) {
        return nightSchedule;
    } else {
        return normalSchedule;
    }
}
```

---

### 9.5 Quest/Contract Handling

**Pattern: Task Generation from Player Requests**

```pseudo
class CompanionQuestSystem {
    List<QuestTemplate> availableQuests;

    Quest GenerateQuest(string playerRequest) {
        // Parse player request
        Intent intent = ParsePlayerIntent(playerRequest);

        // Select appropriate quest template
        QuestTemplate template = SelectTemplate(intent);

        // Generate concrete quest from template
        return template.Generate(intent.parameters);
    }

    Quest AcceptTask(string taskDescription) {
        // Create task from natural language
        Task task = ParseTaskFromNaturalLanguage(taskDescription);

        // Break down into sub-tasks
        List<SubTask> subTasks = BreakDownTask(task);

        // Create quest
        return Quest(task.name, subTasks, task.rewards);
    }

    void OnQuestComplete(Quest quest) {
        // Grant rewards to companion
        GrantRewards(quest.rewards);

        // Update relationship
        relationship.RecordEvent(COMPLETED_TASK_TOGETHER, +10);

        // Check for follow-up quests
        Quest followUp = GenerateFollowUpQuest(quest);
        if (followUp != null) {
            OfferQuest(followUp);
        }
    }
}

class QuestTemplate {
    string name;
    List<ObjectiveGenerator> objectiveGenerators;
    RewardGenerator rewardGenerator;

    Quest Generate(map<string, string> parameters) {
        List<Objective> objectives = [];

        foreach (generator in objectiveGenerators) {
            objectives.Add(generator.Generate(parameters));
        }

        Reward reward = rewardGenerator.Generate(parameters);

        return Quest(name, objectives, reward);
    }
}

// Example: Gather quest
class GatherObjectiveGenerator : ObjectiveGenerator {
    Objective Generate(map<string, string> parameters) {
        string itemType = parameters["item"];
        int count = int.Parse(parameters["count"] or "1");

        return Objective(
            "Gather " + count + " " + itemType,
            GATHER,
            itemType,
            count
        );
    }
}
```

---

### 9.6 Behavior Tree Implementation for Minecraft

**Pattern: Hierarchical Decision Making**

```pseudo
// Behavior tree for Minecraft companion
behaviorTree MinecraftCompanionTree {
    Selector RootSelector {
        // Critical needs first
        Sequence CriticalNeedSequence {
            Condition AnyCriticalNeed()
            Selector NeedSelector {
                Sequence FleeSequence {
                    Condition Need(SAFETY) < 20
                    Action FleeToPlayer()
                }
                Sequence EatSequence {
                    Condition Need(HUNGER) < 20
                    Selector FoodSelector {
                        Action EatInventoryFood()
                        Action AskPlayerForFood()
                    }
                }
                Sequence SleepSequence {
                    Condition Need(ENERGY) < 20
                    Action FindAndSleep()
                }
            }
        }

        // Player commands
        Sequence FollowCommandSequence {
            Condition HasPlayerCommand()
            Action ExecuteCommand()
        }

        // Combat
        Sequence CombatSequence {
            Condition HasHostileTarget()
            Selector CombatSelector {
                Sequence DefendSequence {
                    Condition HasWeapon()
                    Action AttackTarget()
                }
                Sequence FleeCombatSequence {
                    Condition Health() < 30
                    Action FleeToPlayer()
                }
            }
        }

        // Default behaviors
        Sequence DefaultBehaviorSequence {
            Selector DefaultSelector {
                Sequence WorkSequence {
                    Condition HasCurrentTask()
                    Action ContinueTask()
                }
                Sequence FollowSequence {
                    Action FollowPlayer()
                }
                Sequence IdleSequence {
                    Action Wander()
                    Action LookAround()
                }
            }
        }
    }
}
```

---

## 10. Case Studies

### 10.1 Ultima VII: World Simulation (1992)

**Key Innovation**: Every NPC with 24-hour schedule + full object interaction

**Technical Achievement**:
- Seamless world (no loading screens between areas)
- Every object interactive (pick up, use, or move anything)
- NPCs navigate between locations physically
- Schedule-based behavior with environmental awareness

**Legacy**:
- Inspired immersive sim genre (Thief, Deus Ex)
- Set standard for sandbox RPGs
- Direct influence on Grand Theft Auto and Skyrim

---

### 10.2 The Sims: Emergent Behavior (2000)

**Key Innovation**: Need-based AI with intentional imperfection

**Technical Achievement**:
- 8 core needs driving behavior
- Object affordance system (objects declare what they provide)
- Randomness in decision-making for player engagement
- Autonomy toggle for player control

**Legacy**:
- Defined simulation game genre
- Proved need-based AI creates compelling gameplay
- Influenced countless life simulation games

---

### 10.3 Oblivion: Radiant AI (2006)

**Key Innovation**: Goal-oriented package system with emergent behavior

**Technical Achievement**:
- NPCs with goals rather than scripts
- Dynamic environmental interaction
- Complex need systems (hunger, sleep, social)
- Emergent chaos (NPCs stealing, fighting over resources)

**Challenges**:
- Too unpredictable for some players
- Performance issues with full simulation
- Refined in Skyrim for better control

**Legacy**:
- Set standard for open-world NPC behavior
- Influenced Fallout series
- Core technology still used in Bethesda games

---

### 10.4 Final Fantasy XII: Gambit System (2006)

**Key Innovation**: Player-programmable AI through conditional logic

**Technical Achievement**:
- Up to 12 conditional rules per character
- Priority-based execution
- 200+ conditions and actions
- "Programming game" meta-layer

**Legacy**:
- Direct influence on Dragon Age Tactics system
- Inspired numerous indie RPGs
- Created new genre of conditional programming games

---

### 10.5 Skyrim: Refined Radiant AI (2011)

**Key Innovation**: Constrained goal-oriented behavior

**Improvements over Oblivion**:
- Responsibility attribute (controls illegal behavior)
- Distance-based optimization (simplified AI when far from player)
- Package evaluation at intervals, not every frame
- More predictable behaviors

**Technical Achievement**:
- Balanced autonomy with predictability
- Better performance through optimization
- Still created living world feel
- Radiant Story for dynamic quest generation

**Legacy**:
- Set modern standard for open-world RPGs
- Influenced Fallout 4 and Starfield
- Demonstrated value of constrained autonomy

---

## 11. Implementation Patterns

### 11.1 State Machine Pattern

**Use Case**: Managing quest states, NPC behavior modes, game states

```pseudo
interface IState {
    void OnEnter();
    void OnUpdate();
    void OnExit();
}

class StateMachine {
    IState currentState;

    void ChangeState(IState newState) {
        if (currentState != null) {
            currentState.OnExit();
        }
        currentState = newState;
        currentState.OnEnter();
    }

    void Update() {
        if (currentState != null) {
            currentState.OnUpdate();
        }
    }
}

// Example: Companion states
class FollowState : IState {
    void OnEnter() {
        // Start following
    }

    void OnUpdate() {
        // Follow player logic
        float distance = Distance(companion, player);
        if (distance > maxDistance) {
            PathTowards(player);
        }
    }

    void OnExit() {
        // Stop following
    }
}

class CombatState : IState {
    Entity target;

    void OnEnter() {
        target = FindNearestEnemy();
    }

    void OnUpdate() {
        if (target != null && target.IsAlive()) {
            Attack(target);
        } else {
            stateMachine.ChangeState(new FollowState());
        }
    }

    void OnExit() {
        target = null;
    }
}
```

---

### 11.2 Behavior Tree Pattern

**Use Case**: Complex hierarchical AI decision making

```pseudo
abstract class BehaviorNode {
    abstract NodeStatus Execute(); // SUCCESS, FAILURE, RUNNING
}

class Selector : BehaviorNode {
    List<BehaviorNode> children;

    NodeStatus Execute() {
        foreach (child in children) {
            NodeStatus result = child.Execute();
            if (result != FAILURE) {
                return result;
            }
        }
        return FAILURE;
    }
}

class Sequence : BehaviorNode {
    List<BehaviorNode> children;

    NodeStatus Execute() {
        foreach (child in children) {
            NodeStatus result = child.Execute();
            if (result != SUCCESS) {
                return result;
            }
        }
        return SUCCESS;
    }
}

class Condition : BehaviorNode {
    Func<bool> predicate;

    NodeStatus Execute() {
        return predicate() ? SUCCESS : FAILURE;
    }
}

class Action : BehaviorNode {
    Func<NodeStatus> action;

    NodeStatus Execute() {
        return action();
    }
}
```

---

### 11.3 Observer Pattern for Quest Tracking

**Use Case**: Tracking quest progress from game events

```pseudo
class QuestTracker {
    List<Quest> activeQuests;

    QuestTracker() {
        EventSystem.Subscribe(ENEMY_KILLED, OnEnemyKilled);
        EventSystem.Subscribe(ITEM_ACQUIRED, OnItemAcquired);
        EventSystem.Subscribe(LOCATION_REACHED, OnLocationReached);
    }

    void OnEnemyKilled(EnemyKilledEvent e) {
        foreach (quest in activeQuests) {
            quest.OnEvent(e);
        }
    }

    void OnItemAcquired(ItemAcquiredEvent e) {
        foreach (quest in activeQuests) {
            quest.OnEvent(e);
        }
    }
}

class Quest {
    List<QuestObjective> objectives;

    void OnEvent(GameEvent e) {
        foreach (objective in objectives) {
            objective.OnEvent(e);
        }

        CheckCompletion();
    }
}
```

---

### 11.4 Strategy Pattern for NPC Personalities

**Use Case**: Different behavior styles for different NPCs

```pseudo
interface AIBehaviorStrategy {
    Activity SelectActivity(List<Activity> options);
    float GetReactionToEvent(GameEvent event);
    bool ShouldHelpPlayer();
}

class AggressiveStrategy : AIBehaviorStrategy {
    Activity SelectActivity(List<Activity> options) {
        // Prefer combat activities
        return options
            .Where(a => a.type == COMBAT)
            .FirstOrDefault()
            ?? options[Random()];
    }

    float GetReactionToEvent(GameEvent event) {
        if (event.type == ENEMY_ATTACK) return 2.0; // Strong response
        return 1.0;
    }

    bool ShouldHelpPlayer() {
        return true; // Always help
    }
}

class PassiveStrategy : AIBehaviorStrategy {
    Activity SelectActivity(List<Activity> options) {
        // Avoid combat, prefer peaceful activities
        return options
            .Where(a => a.type != COMBAT)
            .FirstOrDefault()
            ?? options[Random()];
    }

    float GetReactionToEvent(GameEvent event) {
        if (event.type == ENEMY_ATTACK) return 0.5; // Weak response
        return 1.0;
    }

    bool ShouldHelpPlayer() {
        return Random() > 0.5; // Sometimes help
    }
}

class NPC {
    AIBehaviorStrategy behavior;

    void ReactToEvent(GameEvent event) {
        float reaction = behavior.GetReactionToEvent(event);

        if (reaction > 1.0) {
            RespondStrongly(event);
        } else {
            RespondWeakly(event);
        }
    }
}
```

---

## 12. Bibliography

### Academic Sources

1. **Champandard, A. J. (2003)**. "AI Game Development: Synthetic Creatures with Learning and Reactive Behaviors." *Charles River Media*.

2. **Rabin, S. (Ed.). (2014)**. "Game AI Pro: Collected Wisdom of Game AI Professionals." *CRC Press*.

3. **Rabin, S. (Ed.). (2015)**. "Game AI Pro 2: Collected Wisdom of Game AI Professionals." *CRC Press*.

4. **Rabin, S. (Ed.). (2016)**. "Game AI Pro 3: Collected Wisdom of Game AI Professionals." *CRC Press*.

5. **Isla, D. (2005)**. "Handling Complexity in Goal-Oriented Action Planning for Game AI." *Proceedings of the AIIDE Conference*.

### Game Documentation

6. **The Sims Wiki (Fandom)**. "The Sims (Game)." *https://sims.fandom.com/wiki/The_Sims*

7. **Stardew Valley Wiki**. "Modding: Schedule Data." *https://stardewvalleywiki.com/Modding:Schedule_data*

8. **Ultima Series**. TV Tropes. "Scheduling." *https://tvtropes.org/pmwiki/pmwiki.php/Scheduling/Ultima*

9. **Fallout Series**. Fallout Wiki (Fandom). *https://fallout.fandom.com/*

10. **Elder Scrolls Series**. Elder Scrolls Wiki (Fandom). *https://elderscrolls.fandom.com/*

### Industry Presentations

11. **Katano, T. (2006)**. "Developing Final Fantasy XII's Gambit System." *CEDEC Conference*.

12. **Norman, C. (2012)**. "Improving Enemy AI in Mass Effect 3." *BioWare GDC Talk*.

13. **Nintendo EAD (2020)**. "Animal Crossing: New Horizons Villager Design." *CEDEC 2020*.

### Technical Resources

14. **Unity Behavior Tree Tutorials**. CSDN. *https://blog.csdn.net/gitblog_00538/article/details/153348756*

15. **GOAP Implementation Guide**. CSDN. *https://blog.csdn.net/weixin_50702814/article/details/144515041*

16. **Quest State Machine Design**. CSDN. *https://blog.csdn.net/weixin_29155599/article/details/153333189*

17. **NPC Relationship Systems**. CSDN. *https://blog.csdn.net/weixin_38526314/article/details/151828709*

### Open Source Projects

18. **GemRB Project**. Infinity Engine Reimplementation. *https://gemrb.org/*

19. **GameReadyGoap**. C# GOAP Implementation. *https://github.com/Joy-less/GameReadyGoap*

20. **Mineflayer-Pathfinder**. Minecraft Pathfinding. *https://github.com/PrismarineJS/mineflayer-pathfinder*

### Historical Sources

21. **Garriott, R. (1992)**. Ultima VII Design Documents. *Origin Systems*.

22. **BioWare (1998)**. Baldur's Gate Modding Guide. *BioWare*

23. **Bethesda Game Studios (2006)**. The Elder Scrolls IV: Oblivion Official Strategy Guide.

### Research Papers

24. **Orkin, J. (2004)**. "Applying Goal-Oriented Action Planning to Games." *AIIDE*.

25. **Buro, M. (2004)**. "Call for AI Research in RTS Games." *AAAI*.

26. **Yannakakis, G. N., & Togelius, J. (2018)**. "Artificial Intelligence and Games." *Springer*.

---

## Appendix: Extractable Code Patterns

### A.1 Schedule System Implementation

```pseudo
// Time-based schedule system
class ScheduleSystem {
    map<int, ScheduleEntry> schedule; // tick -> entry

    void AddEntry(int startTick, int endTick, Activity activity) {
        for (int tick = startTick; tick < endTick; tick++) {
            schedule[tick] = activity;
        }
    }

    Activity GetCurrentActivity(int currentTick) {
        if (schedule.ContainsKey(currentTick)) {
            return schedule[currentTick];
        }
        return defaultActivity;
    }
}
```

### A.2 Need Calculation System

```pseudo
// Weighted need calculation
class NeedCalculator {
    map<NeedType, Need> needs;

    Activity SelectBestActivity(List<Activity> available) {
        Activity best = null;
        float bestScore = -Infinity;

        foreach (activity in available) {
            float score = 0;

            foreach (effect in activity.effects) {
                Need need = needs[effect.needType];
                float urgency = (100 - need.currentValue) / 100.0;
                score += urgency * effect.satisfactionAmount;
            }

            if (score > bestScore) {
                bestScore = score;
                best = activity;
            }
        }

        return best;
    }
}
```

### A.3 Relationship Tracking

```pseudo
// Relationship system with decay
class RelationshipTracker {
    map<Entity, float> relationships;
    float decayRate = 0.01;

    void Update() {
        foreach (entity in relationships.Keys) {
            relationships[entity] = Max(0, relationships[entity] - decayRate);
        }
    }

    void ModifyRelationship(Entity entity, float amount) {
        relationships[entity] = Clamp(relationships[entity] + amount, -100, 100);
    }

    float GetRelationship(Entity entity) {
        return relationships.GetOrDefault(entity, 0);
    }
}
```

### A.4 Path Following with Interruption

```pseudo
// Path following that can be interrupted
class PathFollower {
    Queue<Vector3> path;
    Function onArrival;

    void Update() {
        if (path.Count == 0) {
            if (onArrival != null) {
                onArrival();
                onArrival = null;
            }
            return;
        }

        Vector3 target = path.Peek();

        if (HasReached(target)) {
            path.Dequeue();
        } else {
            MoveTowards(target);
        }
    }

    void SetPath(Queue<Vector3> newPath, Function arrivalCallback) {
        path = newPath;
        onArrival = arrivalCallback;
    }

    void Interrupt() {
        path.Clear();
        onArrival = null;
    }
}
```

---

**Chapter 3 Complete**

This chapter has explored the evolution of RPG and adventure game AI from 1990-2025, focusing on non-LLM techniques that enable autonomous, believable agent behavior. The patterns and systems discussed are directly applicable to creating intelligent Minecraft companions with:

- Schedule-based daily routines
- Need-driven behavior selection
- Dynamic relationship tracking
- Quest and task management
- Conditional AI programming
- State machine and behavior tree architectures

These techniques, proven across decades of game development, provide a robust foundation for creating autonomous agents that feel intelligent and responsive without requiring modern LLM technology.
