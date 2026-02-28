# Pre-LLM Game Automation: A Historical Research Document

**Document Version:** 1.0
**Last Updated:** 2026-02-28
**Authors:** Research Compilation
**Purpose:** Understanding how sophisticated game automation was built before Large Language Models, to enhance those approaches with modern LLM capabilities.

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Early Game Bots (1990s-2000s)](#early-game-bots-1990s-2000s)
3. [MMO Automation (2000s)](#mmo-automation-2000s)
4. [RTS AI Systems](#rts-ai-systems)
5. [Core Decision-Making Patterns](#core-decision-making-patterns)
6. [Memory Systems](#memory-systems)
7. [Trigger Systems](#trigger-systems)
8. [Navigation Techniques](#navigation-techniques)
9. [Combat Systems](#combat-systems)
10. [Code Examples](#code-examples)
11. [Lessons for LLM-Enhanced Automation](#lessons-for-llm-enhanced-automation)
12. [Sources](#sources)

---

## Executive Summary

Before Large Language Models revolutionized game automation, developers built incredibly sophisticated systems using fundamental computer science techniques. This document explores those historical approaches, including:

- **Finite State Machines (FSM)** for behavior management
- **Waypoint systems** and **A* pathfinding** for navigation
- **Utility systems** for weighted decision-making
- **Trigger systems** for event-driven responses
- **Behavior trees** for hierarchical decision logic
- **Memory systems** for pattern recognition

These techniques, when enhanced with LLM reasoning capabilities, can create even more powerful automation systems that combine the reliability of classical approaches with the adaptability of modern AI.

---

## Early Game Bots (1990s-2000s)

### Quake/QuakeWorld Bots

The First Person Shooter (FPS) genre saw some of the earliest sophisticated game bots, with Quake and QuakeWorld leading innovation in the mid-1990s.

#### Prominent Early Bots

| Bot Name | Release Year | Notable Features |
|----------|--------------|------------------|
| **Reaper Bot** | October 1996 | Considered "the best bot for a long time" |
| **Zeus Bot** | 1996-1997 | Early QuakeWorld implementation |
| **Frog Bot** | 1997 | Advanced QuakeWorld bot, still used in ezQuake |
| **Omicron Bot** | 1996-1997 | Competitor to Reaper Bot |
| **Oak Bot** | 1996-1997 | Alternative implementation |

#### Navigation Systems

**Waypoint-Based Navigation:**
Early Quake bots used waypoint systems - discrete position points in 3D space that formed a navigation graph.

**Waypoint Types:**
- **Normal waypoints** - Standard path nodes
- **Jump waypoints** - For traversing gaps
- **Camp waypoints** - Strategic camping positions
- **Goal waypoints** - Objectives (weapons, flags, etc.)
- **Rescue waypoints** - For team-based objectives
- **Ladder waypoints** - Vertical movement

**Creation Process:**
Waypoints were either:
1. **Manually placed** by map creators using console commands
2. **Auto-generated** by bots exploring maps (e.g., `autowaypoint on`)

Example waypoint commands (Counter-Strike PODBot, inherited from Quake):
```
waypoint on          # Enable waypoint editing
waypoint add         # Add waypoint at current position
pathwaypoint on      # Show path connections
pathwaypoint add x   # Connect to waypoint x
waypoint save        # Save waypoint file
```

#### Finite State Machine Architecture

Quake III Arena (1999) formalized the FSM approach with clear state definitions:

**Core States:**
- `AINode_Intermission` - Between matches
- `AINode_Respawn` - Spawning in game
- `AINode_Seek_NBG` - Seeking nearby targets
- `AINode_Battle_Fight` - Active combat
- `AINode_Battle_Retreat` - Fling from combat

**State Transition Logic:**
```
IF (enemy visible AND weapon ready AND health > 30%)
    → TRANSITION_TO(Battle_Fight)
ELSE IF (enemy visible AND health < 30%)
    → TRANSITION_TO(Battle_Retreat)
ELSE IF (ammo low AND health pickup nearby)
    → TRANSITION_TO(Seek_NBG)
```

**Decision-Making Without Neural Networks:**

1. **Item Evaluation:** Items were scored based on current needs
   ```java
   float evaluateItem(Item item, BotState state) {
       float score = 0.0f;
       if (item.type == HEALTH && state.health < 50)
           score += (100 - state.health) * 2.0f;
       if (item.type == AMMO && state.ammo < 20)
           score += (100 - state.ammo) * 1.5f;
       if (item.type == WEAPON && state.hasBetterWeapon())
           score += 50.0f;
       return score / distanceTo(item);
   }
   ```

2. **Combat Positioning:** Used pre-computed "danger zones" and tactical positions
   - Camp waypoints marked good defensive positions
   - Jump waypoints for flanking routes
   - Goal waypoints for objective-based play

3. **Weapon Selection:** Priority-based system
   ```java
   Weapon selectBestWeapon(Target target, float distance) {
       List<Weapon> available = getAvailableWeapons();
       Weapon best = null;
       float bestScore = -1.0f;

       for (Weapon w : available) {
           float score = w.getDamage();
           if (distance < w.getEffectiveRange())
               score *= 2.0f;  // Bonus for in-range
           if (w.getAmmo() < 10)
               score *= 0.5f;  // Penalty for low ammo

           if (score > bestScore) {
               best = w;
               bestScore = score;
           }
       }
       return best;
   }
   ```

#### Quake III: AAS (Area Awareness System)

Quake III introduced the **Area Awareness System (AAS)**, a major advancement over waypoint systems:

**AAS Features:**
- **Level structure analysis** - Automatically extracted navigable spaces
- **Surface classification** - Walkable areas, swim zones, ladders
- **Pre-computed routing** - Routes to common destinations
- **Hierarchical clustering** - Layered pathfinding for efficiency
- **3D navigation awareness** - Jump pads, teleporters, elevators

**How AAS Worked:**
1. **Offline preprocessing:** Maps were analyzed to generate navigation data
2. **Area representation:** World divided into convex areas (not just waypoints)
3. **Reachability:** Calculated which areas connect to which
4. **Runtime query:** Bot queries "route from A to B" and gets path

This was revolutionary because it:
- Eliminated manual waypoint placement
- Handled complex 3D movement naturally
- Scaled to large maps efficiently
- Inspired modern navigation mesh systems

---

## MMO Automation (2000s)

### World of Warcraft Bots

The mid-2000s saw sophisticated automation for MMORPGs, with World of Warcraft being a major target.

#### WoW Glider (2006)

**Architecture:**
- **External process** - Didn't inject code into WoW client
- **Screen reading** - Captured game window pixels
- **Memory reading** - Read game memory (coordinates, HP, etc.)
- **Input simulation** - Sent keyboard/mouse events to WoW window

**Navigation System:**
WoW Glider used XML-based "profiles" containing waypoint routes:

```xml
<Glides>
    <Glides name="Elwynn Forest - Boar Grinding">
        <Waypoint>
            <X>-12345.67</X>
            <Y>890.12</Y>
            <Z>45.67</Z>
            <Action>Kill</Action>
        </Waypoint>
        <Waypoint>
            <X>-12350.00</X>
            <Y>895.00</Y>
            <Z>46.00</Z>
            <Action>Move</Action>
        </Waypoint>
    </Glides>
</Glides>
```

**Combat System:**
Priority-based ability rotation:
```java
class CombatRoutine {
    void executeCombat(Target target) {
        while (target.isAlive()) {
            // Priority system
            if (myHealth() < 40 && canCast("Heal"))
                cast("Heal");
            else if (target.getHealth() < 20 && canCast("Execute"))
                cast("Execute");
            else if (!hasDebuff("Curse of Agony") && canCast("Curse of Agony"))
                cast("Curse of Agony");
            else if (canCast("Shadow Bolt"))
                cast("Shadow Bolt");
            else
                meleeAttack();

            sleep(castTime + 100);
        }

        // Loot
        if (target.canLoot()) {
            loot(target);
        }
    }
}
```

**Trigger Systems:**
Events triggered actions:
- `OnHealthLow(int percent)` - Use healing potions
- `OnManaLow(int percent)` - Drink mana potions
- `OnTargetDead()` - Loot and skin
- `OnInventoryFull()` - Hearth to town, vendor, return
- `OnPlayerNearby()` - Pause or logout (anti-detection)

#### HonorBuddy (2010+)

**Advanced Features:**
- **Navigation meshes** instead of waypoints - smoother pathfinding
- **Combat Routines** - Custom classes (CRs) for each spec
- **Questing** - Could complete quests, not just grind
- **Dungeon bots** - Automated 5-man instances
- **Battleground bots** - Automated PvP

**Navigation Mesh System:**
```java
class NavigationMesh {
    // Pre-computed triangle mesh for each zone
    List<Triangle> triangles;
    Map<Triangle, List<Triangle>> adjacency;

    List<Vector3f> findPath(Vector3f start, Vector3f end) {
        Triangle startTri = findTriangle(start);
        Triangle endTri = findTriangle(end);

        // A* over triangle mesh
        PriorityQueue<TriangleNode> openSet = new PriorityQueue<>();
        Set<Triangle> closedSet = new HashSet<>();

        TriangleNode startNode = new TriangleNode(startTri);
        startNode.gScore = 0;
        startNode.fScore = heuristic(startTri, endTri);
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            TriangleNode current = openSet.poll();

            if (current.triangle == endTri) {
                return reconstructPath(current);
            }

            closedSet.add(current.triangle);

            for (Triangle neighbor : adjacency.get(current.triangle)) {
                if (closedSet.contains(neighbor))
                    continue;

                float tentativeG = current.gScore + distance(current, neighbor);

                TriangleNode neighborNode = findNode(neighbor, openSet);
                if (neighborNode == null) {
                    neighborNode = new TriangleNode(neighbor);
                    openSet.add(neighborNode);
                } else if (tentativeG >= neighborNode.gScore) {
                    continue;
                }

                neighborNode.cameFrom = current;
                neighborNode.gScore = tentativeG;
                neighborNode.fScore = neighborNode.gScore + heuristic(neighbor, endTri);
            }
        }

        return Collections.emptyList(); // No path found
    }
}
```

**Combat Routine Framework:**
```java
interface CombatRoutine {
    void initialize();
    void execute(Target target);
    void terminate();
}

class FuryWarriorRoutine implements CombatRoutine {
    private float lastRage;
    private boolean enrageUp;

    public void execute(Target target) {
        // Reflection-based API
        float rage = Me.Rage;
        int healthPercent = Me.HealthPercent;

        // Priority queue of actions
        PriorityQueue<CombatAction> actions = new PriorityQueue<>(
            (a, b) -> Float.compare(b.getPriority(), a.getPriority())
        );

        // Build action list based on state
        if (rage >= 20 && target.hasDebuff("Rend").expired()) {
            actions.add(new CastAction("Rend", 80.0f));
        }
        if (rage >= 30 && !Me.hasBuff("Enraged Regeneration")) {
            actions.add(new CastAction("Enraged Regeneration", 70.0f));
        }
        if (rage >= 85) {
            actions.add(new CastAction("Raging Blow", 95.0f));
        }
        if (rage >= 20 && Me.hasBuff("Enrage")) {
            actions.add(new CastAction("Bloodthirst", 90.0f));
        }

        // Execute highest priority
        while (!actions.isEmpty()) {
            CombatAction action = actions.poll();
            if (action.canCast()) {
                action.execute();
                break;
            }
        }
    }
}
```

### Ultima Online Automation

**EasyUO** - One of the earliest MMO automation tools (late 1990s):

**Script Structure:**
EasyUO used a simple scripting language with event-based triggers:

```easyuo
; Find and mine ore
initevents
set %oreType WFHF ; Iron Ore type
set %pickaxeType TSF ; Pickaxe type

; Main loop
main:
    finditem %oreType G_10
    if #findkind = -1
    {
        event sysmessage No ore found
        goto main
    }

    ; Move to ore
    move #findx #findy 0 0
    wait 10

    ; Mine the ore
    set #ltargetid #findid
    set #ltargetkind 1
    event macro 17 0 ; Use item in hand
    target
    event macro 22 0 ; Target last target

    ; Wait for mining to complete
    wait 50

    ; Check if overloaded
    if #weight > #maxweight - 20
    {
        gosub depositOre
    }

    goto main

; Subroutine to deposit ore
sub depositOre
    finditem %oreType C_ , #backpackid
    if #findkind = -1
        return

    event drag #findid
    wait 10
    click 275 430 ; Click forge location
    wait 20
    return
```

**Key Techniques:**
- **Color detection** - `finditem` used color/type matching
- **Event system** - `initevents`, `event macro` for game interaction
- **Subroutines** - Modular code organization
- **State tracking** - Variables for persistent state
- **Loop with conditions** - `if`, `goto` for flow control

---

## RTS AI Systems

### Age of Empires (1997)

**AI Scripting Language:**

Age of Empires used a rule-based scripting language with a simple structure:

```age-of-empires
; Basic rule structure
(defrule
    (condition-1)
    (condition-2)
=>
    (action-1)
    (action-2)
)

; Example: Build economy
(defrule
    (can-build-farm)
    (food-amount >= 60)
=>
    (build-farm)
)

; Example: Respond to enemy attack
(defrule
    (enemy-soldiers-nearby > 3)
    (my-soldiers-nearby < 5)
=>
    (retreat)
    (request-reinforcements)
)

; Example: Always-on rule (loop)
(defrule
    true
=>
    (maintain-food-buffer 100)
    (maintain-wood-buffer 200)
)
```

**Key Concepts:**
- **Rules fire when all conditions are met**
- **Only one action per rule per "tick"**
- **Rules processed in order** (priority by position)
- **Simple but effective** for strategic decisions

**Build Order Scripting:**
```age-of-empires
; Dark Age Rush Build Order
(defrule (civ-selected mongol) (difficulty >= moderate)
=>
    (set-goal-id 1) ; Rush goal
)

(defrule (goal 1) (can-scout)
=> (scp-neutral-player))

(defrule (goal 1) (unit-type-count-total scout-cavalry-line == 0) (can-train scout-cavalry-line)
=> (train scout-cavalry-line))

(defrule (goal 1) (food-amount >= 50) (can-train militia-line)
=> (train militia-line))

(defrule (goal 1) (food-amount >= 60) (can-build-farm)
=> (build-farm))

(defrule (goal 1) (can-research feudal-age)
=> (research feudal-age))
```

### StarCraft AI

**BWAPI (Brood War API) Framework:**

BWAPI enabled sophisticated StarCraft AI by exposing game state and allowing programmatic control:

```cpp
// Basic BWAI Bot Structure
class MyBot : public BWAPI::BWAPICallback {
    void onFrame() override {
        // Called every game frame (approx 24 times per second)

        // 1. Update game state
        updateGameState();

        // 2. Make strategic decisions
        makeStrategicDecisions();

        // 3. Execute tactical commands
        executeTacticalCommands();

        // 4. Manage production
        manageProduction();

        // 5. Control military
        controlMilitary();
    }

private:
    void makeStrategicDecisions() {
        // Macro vs Micro balance
        if (shouldExpand()) {
            expand();
        } else if (shouldAttack()) {
            attack();
        } else if (shouldDefend()) {
            defend();
        }
    }

    bool shouldExpand() {
        // Expanding conditions
        int workers = BWAPI::Broodwar->self()->allUnitCount(BWAPI::UnitTypes::Terran_SCV);
        int bases = BWAPI::Broodwar->self()->allUnitCount(BWAPI::UnitTypes::Terran_Command_Center);

        // Expand if we have resources and workers
        return (workers > bases * 20) &&
               (BWAPI::Broodwar->self()->minerals() > 400) &&
               (!isUnderAttack());
    }

    void manageProduction() {
        // Worker production
        BWAPI::UnitType workerType = BWAPI::UnitTypes::Terran_SCV;
        foreach (BWAPI::Unit unit, BWAPI::Broodwar->self()->getUnits()) {
            if (unit->getType() == BWAPI::UnitTypes::Terran_Command_Center) {
                if (unit->isIdle() && !unit->isTraining()) {
                    if (BWAPI::Broodwar->self()->minerals() >= 50 &&
                        (BWAPI::Broodwar->self()->allUnitCount(workerType) < 60)) {
                        unit->train(workerType);
                    }
                }
            }
        }

        // Supply depot production
        int supplyUsed = BWAPI::Broodwar->self()->supplyUsed();
        int supplyTotal = BWAPI::Broodwar->self()->supplyTotal();
        if (supplyUsed + 8 >= supplyTotal) {
            // Build supply depot
            buildBuilding(BWAPI::UnitTypes::Terran_Supply_Depot);
        }
    }

    void controlMilitary() {
        BWAPI::Unitset army = getArmy();

        for (auto& unit : army) {
            if (unit->isIdle()) {
                BWAPI::Unitset enemies = unit->getUnitsInRadius(400, BWAPI::Filter::IsEnemy);

                if (!enemies.empty()) {
                    // Attack weakest enemy
                    BWAPI::Unit target = *std::min_element(enemies.begin(), enemies.end(),
                        [](BWAPI::Unit a, BWAPI::Unit b) {
                            return a->getHitPoints() < b->getHitPoints();
                        });
                    unit->attack(target);
                } else {
                    // Move to patrol position
                    unit->attack(getPatrolPosition());
                }
            }
        }
    }
};
```

**Build Order Execution:**
```cpp
class BuildOrderExecutor {
private:
    std::queue<BuildItem> buildQueue;

public:
    void executeBuildOrder() {
        if (buildQueue.empty())
            return;

        BuildItem item = buildQueue.front();

        if (canBuild(item)) {
            if (item.isUnit()) {
                trainUnit(item.getUnitType());
            } else if (item.isBuilding()) {
                buildBuilding(item.getBuildingType());
            } else if (item.isTech()) {
                researchTech(item.getTechType());
            }
            buildQueue.pop();
        }
    }

    void loadBuildOrder(std::vector<BuildItem> items) {
        for (auto& item : items) {
            buildQueue.push(item);
        }
    }
};

// Example Build Order
void loadStandardBuild() {
    executor.loadBuildOrder({
        BuildItem(UnitTypes::Terran_SCV, 9),        // 9 SCVs
        BuildItem(UnitTypes::Terran_Supply_Depot),  // Supply Depot
        BuildItem(UnitTypes::Terran_SCV, 12),       // 12 SCVs
        BuildItem(UnitTypes::Terran_Barracks),      // Barracks
        BuildItem(UnitTypes::Terran_SCV, 14),       // 14 SCVs
        BuildItem(UnitTypes::Terran_Refinery),      // Refinery
        BuildItem(UnitTypes::Terran_SCV, 16),       // 16 SCVs
        // ... continue build order
    });
}
```

**Macro vs Micro Management:**

**Macro (Strategic Level):**
- Economy management (workers, resources)
- Production buildings (queues, timings)
- Technology research (upgrades)
- Expansion timing

**Micro (Tactical Level):**
- Unit positioning (kiting, concaves)
- Ability usage (stim, siege mode)
- Focus firing (target priority)
- Retreat decisions

```cpp
class MicroManager {
    void microControl(BWAPI::Unitset units) {
        for (auto& unit : units) {
            // Kiting (attack while retreating)
            if (unit->getType() == BWAPI::UnitTypes::Terran_Vulture) {
                performKiting(unit);
            }

            // Siege Tank positioning
            if (unit->getType() == BWAPI::UnitTypes::Terran_Siege_Tank_Tank_Mode) {
                if (shouldSiege(unit)) {
                    unit->siege();
                }
            }

            // Medic healing
            if (unit->getType() == BWAPI::UnitTypes::Terran_Medic) {
                BWAPI::Unit wounded = findWoundedAlly(unit);
                if (wounded) {
                    unit->useTech(BWAPI::TechTypes::Healing, wounded);
                }
            }
        }
    }

private:
    void performKiting(BWAPI::Unit unit) {
        BWAPI::Unitset enemies = unit->getUnitsInRadius(unit->getType().groundWeapon().maxRange(),
            BWAPI::Filter::IsEnemy);

        if (!enemies.empty() && unit->canAttack()) {
            // Attack weapon ready
            if (unit->getGroundWeaponCooldown() == 0) {
                unit->attack(*enemies.begin());
            } else {
                // Weapon on cooldown, retreat
                BWAPI::Position retreatPos = calculateRetreatPosition(unit);
                unit->move(retreatPos);
            }
        }
    }

    BWAPI::Unit findWoundedAlly(BWAPI::Unit medic) {
        BWAPI::Unitset allies = medic->getUnitsInRadius(256, BWAPI::Filter::IsAlly);
        BWAPI::Unit mostWounded = nullptr;
        int lowestHP = 9999;

        for (auto& ally : allies) {
            int hp = ally->getHitPoints();
            if (hp < ally->getType().maxHitPoints() && hp < lowestHP) {
                mostWounded = ally;
                lowestHP = hp;
            }
        }

        return mostWounded;
    }
};
```

---

## Core Decision-Making Patterns

### 1. Finite State Machines (FSM)

**Concept:** Break AI behavior into discrete "states" with clear transitions.

**Advantages:**
- Simple to implement and debug
- Easy to visualize
- Low computational overhead
- Intuitive design

**Disadvantages:**
- Difficult to maintain with many states
- Can become unwieldy in complex games
- Limited reusability

**Implementation Pattern:**

```java
public abstract class FSMState {
    protected final String name;

    public FSMState(String name) {
        this.name = name;
    }

    public abstract void onEnter(Agent agent);
    public abstract void execute(Agent agent);
    public abstract void onExit(Agent agent);
}

public class AgentFSM {
    private FSMState currentState;
    private Map<String, FSMState> states = new HashMap<>();

    public void addState(FSMState state) {
        states.put(state.name, state);
    }

    public void transitionTo(String stateName) {
        FSMState newState = states.get(stateName);
        if (newState != null && newState != currentState) {
            if (currentState != null) {
                currentState.onExit(agent);
            }
            currentState = newState;
            currentState.onEnter(agent);
        }
    }

    public void update() {
        if (currentState != null) {
            currentState.execute(agent);
        }
    }
}

// Example: Combat Bot States
class IdleState extends FSMState {
    public IdleState() { super("Idle"); }

    @Override
    public void execute(Agent agent) {
        // Look for enemies
        Entity enemy = agent.getNearestVisibleEnemy();
        if (enemy != null) {
            agent.getMemory().remember("last_enemy", enemy);
            agent.getFSM().transitionTo("Combat");
        }

        // Look for items
        Entity item = agent.getNearestCollectible();
        if (item != null && agent.canReach(item)) {
            agent.getFSM().transitionTo("Collect");
        }
    }
}

class CombatState extends FSMState {
    public CombatState() { super("Combat"); }

    @Override
    public void execute(Agent agent) {
        Entity target = agent.getMemory().get("last_enemy", Entity.class);

        // Check if target still valid
        if (target == null || !target.isAlive() || !agent.canSee(target)) {
            agent.getFSM().transitionTo("Idle");
            return;
        }

        // Check health
        if (agent.getHealthPercent() < 30) {
            agent.getFSM().transitionTo("Retreat");
            return;
        }

        // Combat behavior
        float distance = agent.distanceTo(target);
        Weapon bestWeapon = agent.selectBestWeapon(distance);

        if (distance > bestWeapon.getEffectiveRange()) {
            agent.moveTo(target.getPosition());
        } else {
            agent.attack(target, bestWeapon);
        }
    }
}

class RetreatState extends FSMState {
    public RetreatState() { super("Retreat"); }

    @Override
    public void execute(Agent agent) {
        // Find safe location
        Position safePos = agent.findNearestSafeLocation();
        agent.moveTo(safePos);

        // Use healing items
        if (agent.hasItem("Health Potion") && agent.getHealthPercent() < 50) {
            agent.useItem("Health Potion");
        }

        // Return to combat when healthy
        if (agent.getHealthPercent() > 80) {
            agent.getFSM().transitionTo("Idle");
        }
    }
}
```

### 2. Behavior Trees

**Concept:** Hierarchical tree structure where leaf nodes represent behaviors and composite nodes control flow.

**Node Types:**
- **Sequence:** Execute children in order, fail if any child fails
- **Selector:** Execute children in order, succeed if any child succeeds
- **Parallel:** Execute all children simultaneously
- **Decorator:** Modify child behavior (repeat, invert, timeout)

```java
// Behavior Tree Node Interface
public interface BTNode {
    BTStatus execute(Agent agent, float deltaTime);
}

enum BTStatus {
    SUCCESS,
    FAILURE,
    RUNNING
}

// Composite Nodes
class Sequence implements BTNode {
    private List<BTNode> children = new ArrayList<>();

    public Sequence(BTNode... children) {
        this.children.addAll(Arrays.asList(children));
    }

    @Override
    public BTStatus execute(Agent agent, float deltaTime) {
        for (BTNode child : children) {
            BTStatus status = child.execute(agent, deltaTime);
            if (status != BTStatus.SUCCESS) {
                return status; // Fail or running
            }
        }
        return BTStatus.SUCCESS;
    }
}

class Selector implements BTNode {
    private List<BTNode> children = new ArrayList<>();

    public Selector(BTNode... children) {
        this.children.addAll(Arrays.asList(children));
    }

    @Override
    public BTStatus execute(Agent agent, float deltaTime) {
        for (BTNode child : children) {
            BTStatus status = child.execute(agent, deltaTime);
            if (status != BTStatus.FAILURE) {
                return status; // Success or running
            }
        }
        return BTStatus.FAILURE;
    }
}

// Leaf Nodes (Actions)
class MoveToTarget implements BTNode {
    @Override
    public BTStatus execute(Agent agent, float deltaTime) {
        Entity target = agent.getBlackboard().get("target", Entity.class);
        if (target == null) return BTStatus.FAILURE;

        agent.moveTo(target.getPosition());
        return agent.atTarget() ? BTStatus.SUCCESS : BTStatus.RUNNING;
    }
}

class AttackTarget implements BTNode {
    @Override
    public BTStatus execute(Agent agent, float deltaTime) {
        Entity target = agent.getBlackboard().get("target", Entity.class);
        if (target == null || !target.isAlive()) return BTStatus.FAILURE;

        agent.attack(target);
        return BTStatus.SUCCESS;
    }
}

// Condition Nodes
class IsHealthLow implements BTNode {
    private final float threshold;

    public IsHealthLow(float threshold) {
        this.threshold = threshold;
    }

    @Override
    public BTStatus execute(Agent agent, float deltaTime) {
        return agent.getHealthPercent() < threshold ? BTStatus.SUCCESS : BTStatus.FAILURE;
    }
}

class HasTarget implements BTNode {
    @Override
    public BTStatus execute(Agent agent, float deltaTime) {
        Entity target = agent.getBlackboard().get("target", Entity.class);
        return (target != null && target.isAlive()) ? BTStatus.SUCCESS : BTStatus.FAILURE;
    }
}

// Building a Behavior Tree
BTNode combatBehavior = new Selector(
    // Low health: retreat
    new Sequence(
        new IsHealthLow(30.0f),
        new FindSafeLocation(),
        new MoveToTarget()
    ),

    // Have target: attack
    new Sequence(
        new HasTarget(),
        new MoveToTarget(),
        new AttackTarget()
    ),

    // No target: patrol
    new Sequence(
        new SelectPatrolPoint(),
        new MoveToTarget()
    )
);
```

### 3. Utility Systems

**Concept:** Score each possible action based on weighted factors, execute highest-scoring action.

**Advantages:**
- Smooth, natural behavior transitions
- Easy to add new actions
- Handles complex trade-offs
- Very flexible

```java
public class UtilityAction {
    private final String name;
    private final List<UtilityFactor> factors = new ArrayList<>();

    public UtilityAction(String name) {
        this.name = name;
    }

    public void addFactor(UtilityFactor factor) {
        factors.add(factor);
    }

    public float calculateScore(Agent agent) {
        float totalScore = 0.0f;
        float totalWeight = 0.0f;

        for (UtilityFactor factor : factors) {
            float weight = factor.getWeight(agent);
            float score = factor.evaluate(agent);

            totalScore += score * weight;
            totalWeight += weight;
        }

        return totalWeight > 0 ? totalScore / totalWeight : 0.0f;
    }
}

public interface UtilityFactor {
    float evaluate(Agent agent);
    float getWeight(Agent agent);
}

// Utility AI Decision Maker
public class UtilityAI {
    private List<UtilityAction> actions = new ArrayList<>();

    public void addAction(UtilityAction action) {
        actions.add(action);
    }

    public void execute(Agent agent) {
        UtilityAction bestAction = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        for (UtilityAction action : actions) {
            float score = action.calculateScore(agent);
            if (score > bestScore) {
                bestScore = score;
                bestAction = action;
            }
        }

        if (bestAction != null && bestScore > 0.0f) {
            bestAction.execute(agent);
        }
    }
}

// Example: Combat Utility System
UtilityAction attackAction = new UtilityAction("Attack");
attackAction.addFactor(new UtilityFactor() {
    @Override
    public float evaluate(Agent agent) {
        Entity target = agent.getNearestEnemy();
        if (target == null) return 0.0f;
        return 1.0f; // Attack is always relevant
    }

    @Override
    public float getWeight(Agent agent) {
        return 10.0f; // High priority
    }
});
attackAction.addFactor(new UtilityFactor() {
    @Override
    public float evaluate(Agent agent) {
        float healthPercent = agent.getHealthPercent();
        return healthPercent / 100.0f; // More likely to attack when healthy
    }

    @Override
    public float getWeight(Agent agent) {
        return 5.0f;
    }
});
attackAction.addFactor(new UtilityFactor() {
    @Override
    public float evaluate(Agent agent) {
        Entity target = agent.getNearestEnemy();
        float distance = agent.distanceTo(target);
        float range = agent.getWeaponRange();
        if (distance > range) return 0.2f;
        return 1.0f;
    }

    @Override
    public float getWeight(Agent agent) {
        return 8.0f;
    }
});

UtilityAction healAction = new UtilityAction("Heal");
healAction.addFactor(new UtilityFactor() {
    @Override
    public float evaluate(Agent agent) {
        float healthPercent = agent.getHealthPercent();
        return 1.0f - (healthPercent / 100.0f); // Higher when low health
    }

    @Override
    public float getWeight(Agent agent) {
        return 15.0f; // Very high priority when low
    }
});
healAction.addFactor(new UtilityFactor() {
    @Override
    public float evaluate(Agent agent) {
        return agent.hasHealthPotion() ? 1.0f : 0.0f;
    }

    @Override
    public float getWeight(Agent agent) {
        return 10.0f;
    }
});

UtilityAction reloadAction = new UtilityAction("Reload");
reloadAction.addFactor(new UtilityFactor() {
    @Override
    public float evaluate(Agent agent) {
        float ammoPercent = agent.getAmmoPercent();
        return 1.0f - (ammoPercent / 100.0f);
    }

    @Override
    public float getWeight(Agent agent) {
        return 7.0f;
    }
});
reloadAction.addFactor(new UtilityFactor() {
    @Override
    public float evaluate(Agent agent) {
        Entity nearestEnemy = agent.getNearestEnemy();
        if (nearestEnemy == null) return 1.0f; // Safe to reload

        float distance = agent.distanceTo(nearestEnemy);
        return distance > 20.0f ? 1.0f : 0.3f; // Lower when enemies close
    }

    @Override
    public float getWeight(Agent agent) {
        return 10.0f;
    }
});
```

### 4. Goal-Oriented Action Planning (GOAP)

**Concept:** Define goals and actions, let AI plan sequence of actions to reach goals.

```java
public class WorldState {
    private Map<String, Object> values = new HashMap<>();

    public void set(String key, Object value) {
        values.put(key, value);
    }

    public Object get(String key) {
        return values.get(key);
    }

    public boolean matches(WorldState goal) {
        for (Map.Entry<String, Object> entry : goal.values.entrySet()) {
            Object ourValue = values.get(entry.getKey());
            if (!entry.getValue().equals(ourValue)) {
                return false;
            }
        }
        return true;
    }
}

public class GOAPAction {
    private String name;
    private WorldState preconditions = new WorldState();
    private WorldState effects = new WorldState();
    private float cost;

    public boolean checkProceduralPrecondition(Agent agent) {
        return true; // Override for complex conditions
    }

    public void execute(Agent agent) {
        // Override with action logic
    }
}

public class GOAPPlanner {
    public Queue<GOAPAction> plan(Agent agent,
                                   List<GOAPAction> availableActions,
                                   WorldState goal) {
        // A* search over action space
        PriorityQueue<GOAPNode> openSet = new PriorityQueue<>(
            (a, b) -> Float.compare(a.fScore, b.fScore)
        );
        Set<GOAPNode> closedSet = new HashSet<>();

        WorldState startState = agent.getWorldState();
        GOAPNode startNode = new GOAPNode(null, null, startState, 0);
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            GOAPNode current = openSet.poll();

            if (current.state.matches(goal)) {
                return reconstructPlan(current);
            }

            closedSet.add(current);

            for (GOAPAction action : availableActions) {
                if (!action.checkProceduralPrecondition(agent))
                    continue;

                WorldState newState = applyAction(current.state, action);
                float newCost = current.gScore + action.cost;

                GOAPNode neighbor = findNode(newState, openSet);
                if (neighbor == null) {
                    neighbor = new GOAPNode(action, current, newState, newCost);
                    neighbor.fScore = neighbor.gScore + heuristic(newState, goal);
                    openSet.add(neighbor);
                } else if (newCost < neighbor.gScore) {
                    neighbor.cameFrom = current;
                    neighbor.action = action;
                    neighbor.gScore = newCost;
                    neighbor.fScore = neighbor.gScore + heuristic(newState, goal);
                }
            }
        }

        return null; // No plan found
    }

    private WorldState applyAction(WorldState state, GOAPAction action) {
        WorldState newState = new WorldState();
        newState.values.putAll(state.values);
        newState.values.putAll(action.effects.values);
        return newState;
    }
}

// Example: Combat GOAP
class AttackAction extends GOAPAction {
    public AttackAction() {
        name = "Attack";
        cost = 1.0f;
        preconditions.set("has_weapon", true);
        preconditions.set("enemy_visible", true);
        effects.set("enemy_alive", false);
    }

    @Override
    public boolean checkProceduralPrecondition(Agent agent) {
        return agent.getWeapon() != null &&
               agent.getNearestEnemy() != null;
    }

    @Override
    public void execute(Agent agent) {
        Entity target = agent.getNearestEnemy();
        agent.attack(target);
    }
}

class ReloadAction extends GOAPAction {
    public ReloadAction() {
        name = "Reload";
        cost = 2.0f;
        preconditions.set("has_ammo", false);
        effects.set("has_ammo", true);
    }

    @Override
    public void execute(Agent agent) {
        agent.reload();
    }
}

class FindCoverAction extends GOAPAction {
    public FindCoverAction() {
        name = "FindCover";
        cost = 3.0f;
        effects.set("in_cover", true);
    }

    @Override
    public void execute(Agent agent) {
        Position cover = agent.findNearestCover();
        agent.moveTo(cover);
    }
}
```

### 5. Hierarchical Task Networks (HTN)

**Concept:** Decompose high-level tasks into subtasks recursively.

```java
public class Task {
    private final String name;

    public Task(String name) {
        this.name = name;
    }

    public boolean isPrimitive() {
        return false;
    }
}

public class PrimitiveTask extends Task {
    private final Consumer<Agent> executor;

    public PrimitiveTask(String name, Consumer<Agent> executor) {
        super(name);
        this.executor = executor;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    public void execute(Agent agent) {
        executor.accept(agent);
    }
}

public class CompoundTask extends Task {
    private final List<Method> methods = new ArrayList<>();

    public CompoundTask(String name) {
        super(name);
    }

    public void addMethod(Method method) {
        methods.add(method);
    }

    public Method getSatisfiedMethod(Agent agent, WorldState state) {
        for (Method method : methods) {
            if (method.isValid(agent, state)) {
                return method;
            }
        }
        return null;
    }
}

public class Method {
    private final List<Task> subtasks = new ArrayList<>();
    private final Predicate<WorldState> condition;

    public Method(Predicate<WorldState> condition, Task... subtasks) {
        this.condition = condition;
        this.subtasks.addAll(Arrays.asList(subtasks));
    }

    public boolean isValid(Agent agent, WorldState state) {
        return condition.test(state);
    }

    public List<Task> getSubtasks() {
        return subtasks;
    }
}

public class HTNPlanner {
    public Queue<Task> plan(Agent agent, Task rootTask, WorldState state) {
        Queue<Task> plan = new LinkedList<>();
        decompose(agent, rootTask, state, plan);
        return plan;
    }

    private boolean decompose(Agent agent, Task task, WorldState state, Queue<Task> plan) {
        if (task.isPrimitive()) {
            PrimitiveTask primitive = (PrimitiveTask) task;
            plan.add(primitive);
            return true;
        }

        CompoundTask compound = (CompoundTask) task;
        Method method = compound.getSatisfiedMethod(agent, state);

        if (method == null)
            return false;

        for (Task subtask : method.getSubtasks()) {
            if (!decompose(agent, subtask, state, plan)) {
                return false;
            }
        }

        return true;
    }
}

// Example: Combat HTN
CompoundTask combat = new CompoundTask("Combat");

// Method: Melee combat
combat.addMethod(new Method(
    state -> (boolean)state.get("enemy_nearby") && (boolean)state.get("has_melee_weapon"),
    new PrimitiveTask("MoveToMeleeRange", agent -> agent.moveToMeleeRange()),
    new PrimitiveTask("MeleeAttack", agent -> agent.meleeAttack())
));

// Method: Ranged combat
combat.addMethod(new Method(
    state -> (boolean)state.get("enemy_visible") && (boolean)state.get("has_ranged_weapon"),
    new PrimitiveTask("AimAtTarget", agent -> agent.aimAtTarget()),
    new PrimitiveTask("Fire", agent -> agent.fire())
));

// Method: Reload then fire
combat.addMethod(new Method(
    state -> !(boolean)state.get("has_ammo") && (boolean)state.get("has_reserve_ammo"),
    new PrimitiveTask("TakeCover", agent -> agent.takeCover()),
    new PrimitiveTask("Reload", agent -> agent.reload()),
    new PrimitiveTask("AimAtTarget", agent -> agent.aimAtTarget()),
    new PrimitiveTask("Fire", agent -> agent.fire())
));
```

---

## Memory Systems

Memory systems allowed bots to remember past events and player patterns.

### Signal Memory Systems

**Concept:** Store information about detected signals (enemies, sounds, etc.) with timestamps.

```java
public class SignalMemory {
    private static class Signal {
        final Vector3f position;
        final long timestamp;
        final int signalType;
        final float confidence;

        Signal(Vector3f position, int signalType, float confidence) {
            this.position = position;
            this.timestamp = System.currentTimeMillis();
            this.signalType = signalType;
            this.confidence = confidence;
        }

        boolean isExpired(long maxAge) {
            return System.currentTimeMillis() - timestamp > maxAge;
        }
    }

    private final List<Signal> signals = new ArrayList<>();
    private final long maxAge = 30000; // 30 seconds

    public void addSignal(Vector3f position, int type, float confidence) {
        signals.add(new Signal(position, type, confidence));
    }

    public Vector3f getLastKnownPosition(int type) {
        for (int i = signals.size() - 1; i >= 0; i--) {
            Signal signal = signals.get(i);
            if (signal.signalType == type && !signal.isExpired(maxAge)) {
                return signal.position;
            }
        }
        return null;
    }

    public long getTimeSinceLastSignal(int type) {
        for (int i = signals.size() - 1; i >= 0; i--) {
            Signal signal = signals.get(i);
            if (signal.signalType == type) {
                return System.currentTimeMillis() - signal.timestamp;
            }
        }
        return Long.MAX_VALUE;
    }

    public void cleanup() {
        signals.removeIf(s -> s.isExpired(maxAge));
    }
}
```

### Pattern Recognition Memory

**Concept:** Remember player behaviors to predict future actions.

```java
public class PlayerPatternMemory {
    private static class PlayerObservation {
        final int playerId;
        final String action;
        final Vector3f position;
        final long timestamp;
        final Map<String, Object> context;

        PlayerObservation(int playerId, String action, Vector3f position,
                         Map<String, Object> context) {
            this.playerId = playerId;
            this.action = action;
            this.position = position;
            this.timestamp = System.currentTimeMillis();
            this.context = context;
        }
    }

    private final List<PlayerObservation> observations = new ArrayList<>();
    private final int maxObservations = 1000;

    public void recordAction(int playerId, String action, Vector3f position,
                            Map<String, Object> context) {
        observations.add(new PlayerObservation(playerId, action, position, context));
        if (observations.size() > maxObservations) {
            observations.remove(0);
        }
    }

    public String predictNextAction(int playerId, Vector3f currentPosition) {
        // Find recent observations by this player
        List<PlayerObservation> playerObs = observations.stream()
            .filter(obs -> obs.playerId == playerId)
            .collect(Collectors.toList());

        if (playerObs.size() < 5) {
            return null; // Not enough data
        }

        // Simple pattern: what did they do last time in this area?
        for (int i = playerObs.size() - 1; i >= 0; i--) {
            PlayerObservation obs = playerObs.get(i);
            float distance = currentPosition.distance(obs.position);

            if (distance < 50.0f) {
                // Similar position, predict same action
                return obs.action;
            }
        }

        // Fall back to most common recent action
        Map<String, Integer> actionCounts = new HashMap<>();
        for (int i = Math.max(0, playerObs.size() - 10); i < playerObs.size(); i++) {
            String action = playerObs.get(i).action;
            actionCounts.put(action, actionCounts.getOrDefault(action, 0) + 1);
        }

        return actionCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    public List<Vector3f> getFrequentPositions(int playerId) {
        // Cluster positions to find frequently visited areas
        Map<Vector3i, Integer> positionCounts = new HashMap<>();

        for (PlayerObservation obs : observations) {
            if (obs.playerId == playerId) {
                Vector3i cell = new Vector3i(
                    (int)(obs.position.x / 10),
                    (int)(obs.position.y / 10),
                    (int)(obs.position.z / 10)
                );
                positionCounts.put(cell, positionCounts.getOrDefault(cell, 0) + 1);
            }
        }

        // Return most frequent positions
        return positionCounts.entrySet().stream()
            .filter(e -> e.getValue() >= 5) // At least 5 visits
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .map(e -> new Vector3f(
                e.getKey().x * 10 + 5,
                e.getKey().y * 10 + 5,
                e.getKey().z * 10 + 5
            ))
            .collect(Collectors.toList());
    }
}
```

---

## Trigger Systems

Trigger systems enabled event-driven responses without constant polling.

### Event-Driven Architecture

```java
public class EventBus {
    private static class Handler {
        final Predicate<Object> condition;
        final Consumer<Object> callback;

        Handler(Predicate<Object> condition, Consumer<Object> callback) {
            this.condition = condition;
            this.callback = callback;
        }
    }

    private final Map<Class<?>, List<Handler>> handlers = new HashMap<>();

    public <T> void subscribe(Class<T> eventType, Predicate<T> condition, Consumer<T> callback) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>())
            .add(new Handler(
                obj -> condition.test(eventType.cast(obj)),
                callback
            ));
    }

    public void publish(Object event) {
        List<Handler> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (Handler handler : eventHandlers) {
                if (handler.condition.test(event)) {
                    handler.callback.accept(event);
                }
            }
        }
    }
}

// Example Game Events
class HealthLowEvent {
    final int entityId;
    final float healthPercent;

    HealthLowEvent(int entityId, float healthPercent) {
        this.entityId = entityId;
        this.healthPercent = healthPercent;
    }
}

class EnemySpottedEvent {
    final int enemyId;
    final Vector3f position;
    final float distance;

    EnemySpottedEvent(int enemyId, Vector3f position, float distance) {
        this.enemyId = enemyId;
        this.position = position;
        this.distance = distance;
    }
}

class InventoryFullEvent {
    final int entityId;

    InventoryFullEvent(int entityId) {
        this.entityId = entityId;
    }
}

// Setting up triggers
EventBus eventBus = new EventBus();

// Trigger: Use healing potion when health low
eventBus.subscribe(HealthLowEvent.class,
    event -> event.healthPercent < 30,
    event -> {
        Entity entity = world.getEntity(event.entityId);
        if (entity.hasItem("Health Potion")) {
            entity.useItem("Health Potion");
        }
    }
);

// Trigger: Alert nearby allies when enemy spotted
eventBus.subscribe(EnemySpottedEvent.class,
    event -> event.distance < 50.0f,
    event -> {
        Entity me = world.getLocalPlayer();
        List<Entity> allies = world.getAlliesInRange(me.getPosition(), 100.0f);
        for (Entity ally : allies) {
            ally.getAI().alertEnemy(event.enemyId, event.position);
        }
    }
);

// Trigger: Return to town when inventory full
eventBus.subscribe(InventoryFullEvent.class,
    event -> true,
    event -> {
        Entity entity = world.getEntity(event.entityId);
        entity.getAI().setGoal("return_to_town");
    }
);
```

### Scheduled Triggers

```java
public class TriggerSystem {
    private static class Trigger {
        final Runnable action;
        final long interval;
        long lastExecution;

        Trigger(Runnable action, long interval) {
            this.action = action;
            this.interval = interval;
            this.lastExecution = 0;
        }

        boolean shouldExecute(long currentTime) {
            return currentTime - lastExecution >= interval;
        }

        void execute(long currentTime) {
            action.run();
            lastExecution = currentTime;
        }
    }

    private final List<Trigger> triggers = new ArrayList<>();

    public void addPeriodicTrigger(Runnable action, long intervalMs) {
        triggers.add(new Trigger(action, intervalMs));
    }

    public void update() {
        long currentTime = System.currentTimeMillis();
        for (Trigger trigger : triggers) {
            if (trigger.shouldExecute(currentTime)) {
                trigger.execute(currentTime);
            }
        }
    }
}

// Example usage
TriggerSystem triggers = new TriggerSystem();

// Check for loot every 2 seconds
triggers.addPeriodicTrigger(() -> {
    Entity loot = world.getNearestLoot(player.getPosition(), 20.0f);
    if (loot != null && !player.isInventoryFull()) {
        player.moveTo(loot.getPosition());
        player.pickup(loot);
    }
}, 2000);

// Save state every 5 minutes
triggers.addPeriodicTrigger(() -> {
    saveGameState();
}, 300000);

// Check for zone changes every 10 seconds
triggers.addPeriodicTrigger(() -> {
    String currentZone = world.getZoneAt(player.getPosition());
    if (!currentZone.equals(player.getCurrentZone())) {
        player.setCurrentZone(currentZone);
        eventBus.publish(new ZoneChangeEvent(currentZone));
    }
}, 10000);
```

---

## Navigation Techniques

### A* Pathfinding

The gold standard for pathfinding in games.

```java
public class AStarPathfinding {
    private static class Node {
        final Vector3i position;
        Node parent;
        float gScore; // Cost from start
        float fScore; // gScore + heuristic

        Node(Vector3i position) {
            this.position = position;
        }
    }

    public List<Vector3f> findPath(Vector3f start, Vector3f goal,
                                   NavigationMesh navMesh) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(
            (a, b) -> Float.compare(a.fScore, b.fScore)
        );
        Map<Vector3i, Node> allNodes = new HashMap<>();
        Set<Vector3i> closedSet = new HashSet<>();

        Vector3i startCell = navMesh.getCellAt(start);
        Vector3i goalCell = navMesh.getCellAt(goal);

        Node startNode = new Node(startCell);
        startNode.gScore = 0;
        startNode.fScore = heuristic(startCell, goalCell);
        openSet.add(startNode);
        allNodes.put(startCell, startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.position.equals(goalCell)) {
                return reconstructPath(current, navMesh);
            }

            closedSet.add(current.position);

            for (Vector3i neighbor : navMesh.getNeighbors(current.position)) {
                if (closedSet.contains(neighbor))
                    continue;

                float tentativeG = current.gScore +
                    navMesh.getCost(current.position, neighbor);

                Node neighborNode = allNodes.get(neighbor);
                if (neighborNode == null) {
                    neighborNode = new Node(neighbor);
                    neighborNode.gScore = tentativeG;
                    neighborNode.fScore = neighborNode.gScore +
                        heuristic(neighbor, goalCell);
                    openSet.add(neighborNode);
                    allNodes.put(neighbor, neighborNode);
                } else if (tentativeG < neighborNode.gScore) {
                    neighborNode.parent = current;
                    neighborNode.gScore = tentativeG;
                    neighborNode.fScore = neighborNode.gScore +
                        heuristic(neighbor, goalCell);
                }
            }
        }

        return null; // No path found
    }

    private float heuristic(Vector3i a, Vector3i b) {
        // Manhattan distance
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y) + Math.abs(a.z - b.z);
    }

    private List<Vector3f> reconstructPath(Node goalNode, NavigationMesh navMesh) {
        LinkedList<Vector3f> path = new LinkedList<>();
        Node current = goalNode;

        while (current != null) {
            path.addFirst(navMesh.getCenter(current.position));
            current = current.parent;
        }

        return path;
    }
}
```

### Navigation Mesh Systems

```java
public class NavigationMesh {
    private static class Triangle {
        final Vector3f[] vertices = new Vector3f[3];
        final Vector3f center;
        final List<Triangle> neighbors = new ArrayList<>();

        Triangle(Vector3f a, Vector3f b, Vector3f c) {
            vertices[0] = a;
            vertices[1] = b;
            vertices[2] = c;
            center = new Vector3f(
                (a.x + b.x + c.x) / 3,
                (a.y + b.y + c.y) / 3,
                (a.z + b.z + c.z) / 3
            );
        }

        boolean contains(Vector3f point) {
            // Barycentric coordinate test
            Vector3f v0 = vertices[2].sub(vertices[0], new Vector3f());
            Vector3f v1 = vertices[1].sub(vertices[0], new Vector3f());
            Vector3f v2 = point.sub(vertices[0], new Vector3f());

            float d00 = v0.dot(v0);
            float d01 = v0.dot(v1);
            float d11 = v1.dot(v1);
            float d20 = v2.dot(v0);
            float d21 = v2.dot(v1);

            float denom = d00 * d11 - d01 * d01;
            if (Math.abs(denom) < 0.0001f) return false;

            float v = (d11 * d20 - d01 * d21) / denom;
            float w = (d00 * d21 - d01 * d20) / denom;
            float u = 1.0f - v - w;

            return u >= 0 && v >= 0 && w >= 0;
        }
    }

    private final List<Triangle> triangles = new ArrayList<>();

    public void addTriangle(Vector3f a, Vector3f b, Vector3f c) {
        Triangle tri = new Triangle(a, b, c);
        triangles.add(tri);

        // Find neighbors (share 2 vertices)
        for (Triangle other : triangles) {
            if (other == tri) continue;
            int sharedVertices = countSharedVertices(tri, other);
            if (sharedVertices == 2) {
                tri.neighbors.add(other);
                other.neighbors.add(tri);
            }
        }
    }

    public Triangle findTriangle(Vector3f point) {
        for (Triangle tri : triangles) {
            if (tri.contains(point)) {
                return tri;
            }
        }
        return null;
    }

    public List<Vector3f> findPath(Vector3f start, Vector3f goal) {
        Triangle startTri = findTriangle(start);
        Triangle goalTri = findTriangle(goal);

        if (startTri == null || goalTri == null)
            return null;

        // A* over triangles
        PriorityQueue<TriangleNode> openSet = new PriorityQueue<>(
            (a, b) -> Float.compare(a.fScore, b.fScore)
        );
        Set<Triangle> closedSet = new HashSet<>();

        TriangleNode startNode = new TriangleNode(startTri);
        startNode.gScore = 0;
        startNode.fScore = startTri.center.distance(goalTri.center);
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            TriangleNode current = openSet.poll();

            if (current.triangle == goalTri) {
                return reconstructPath(current, start, goal);
            }

            closedSet.add(current.triangle);

            for (Triangle neighbor : current.triangle.neighbors) {
                if (closedSet.contains(neighbor))
                    continue;

                float cost = current.triangle.center.distance(neighbor.center);
                float tentativeG = current.gScore + cost;

                TriangleNode neighborNode = findNode(neighbor, openSet);
                if (neighborNode == null) {
                    neighborNode = new TriangleNode(neighbor);
                    neighborNode.gScore = tentativeG;
                    neighborNode.fScore = tentativeG +
                        neighbor.center.distance(goalTri.center);
                    openSet.add(neighborNode);
                } else if (tentativeG < neighborNode.gScore) {
                    neighborNode.parent = current;
                    neighborNode.gScore = tentativeG;
                    neighborNode.fScore = neighborNode.gScore +
                        neighbor.center.distance(goalTri.center);
                }
            }
        }

        return null;
    }

    private static class TriangleNode {
        Triangle triangle;
        TriangleNode parent;
        float gScore;
        float fScore;

        TriangleNode(Triangle triangle) {
            this.triangle = triangle;
        }
    }
}
```

---

## Combat Systems

### Targeting Priority Systems

```java
public class TargetSelector {
    public Entity selectBestTarget(Agent agent, List<Entity> enemies) {
        if (enemies.isEmpty()) return null;

        Entity bestTarget = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        for (Entity enemy : enemies) {
            float score = evaluateTarget(agent, enemy);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = enemy;
            }
        }

        return bestTarget;
    }

    private float evaluateTarget(Agent agent, Entity target) {
        float score = 0.0f;

        // Factor 1: Threat value (enemies closer are more threatening)
        float distance = agent.distanceTo(target);
        score += Math.max(0, 100 - distance);

        // Factor 2: Low health targets (finish them off)
        float healthPercent = target.getHealthPercent();
        if (healthPercent < 30) {
            score += 50;
        }

        // Factor 3: Target class priority
        String targetClass = target.getClass();
        switch (targetClass) {
            case "Healer":
                score += 100; // Priority target
                break;
            case "Mage":
                score += 80;
                break;
            case "DamageDealer":
                score += 60;
                break;
            case "Tank":
                score += 20; // Low priority
                break;
        }

        // Factor 4: Can we damage them effectively?
        Weapon weapon = agent.getBestWeapon();
        if (distance > weapon.getEffectiveRange()) {
            score *= 0.5f; // Penalty for out of range
        }

        // Factor 5: Are they attacking us?
        if (target.getTarget() == agent) {
            score += 30;
        }

        return score;
    }
}
```

### Weapon Selection Systems

```java
public class WeaponSystem {
    private static class WeaponScore {
        final Weapon weapon;
        final float score;

        WeaponScore(Weapon weapon, float score) {
            this.weapon = weapon;
            this.score = score;
        }
    }

    public Weapon selectBestWeapon(Agent agent, Entity target) {
        float distance = agent.distanceTo(target);
        List<Weapon> available = agent.getAvailableWeapons();

        List<WeaponScore> scores = new ArrayList<>();
        for (Weapon weapon : available) {
            scores.add(new WeaponScore(weapon, scoreWeapon(agent, target, weapon, distance)));
        }

        scores.sort((a, b) -> Float.compare(b.score, a.score));

        return scores.isEmpty() ? null : scores.get(0).weapon;
    }

    private float scoreWeapon(Agent agent, Entity target, Weapon weapon, float distance) {
        float score = 0.0f;

        // Base damage
        score += weapon.getDamage();

        // Range effectiveness
        if (distance <= weapon.getEffectiveRange()) {
            score += 50; // In range bonus
        } else if (distance > weapon.getMaxRange()) {
            return Float.NEGATIVE_INFINITY; // Can't reach
        } else {
            score -= 20; // Suboptimal range penalty
        }

        // Ammo consideration
        if (weapon.getAmmo() == 0) {
            return Float.NEGATIVE_INFINITY; // Can't use
        }
        if (weapon.getAmmo() < weapon.getClipSize() * 0.2f) {
            score -= 30; // Low ammo penalty
        }

        // Target type bonus
        if (weapon.isEffectiveAgainst(target.getType())) {
            score += 40;
        }

        // Reload state
        if (weapon.isReloading()) {
            return Float.NEGATIVE_INFINITY; // Can't fire
        }

        // Fire rate preference for different situations
        if (target.getHealthPercent() < 20) {
            // High damage for finishing
            score += weapon.getBurstDamage() * 2;
        } else {
            // DPS for sustained combat
            score += weapon.getDPS();
        }

        return score;
    }
}
```

### Combat Rotation Systems

```java
public class CombatRotation {
    private static class Action {
        final String name;
        final Predicate<Agent> condition;
        final Consumer<Agent> executor;
        final float priority;

        Action(String name, Predicate<Agent> condition,
               Consumer<Agent> executor, float priority) {
            this.name = name;
            this.condition = condition;
            this.executor = executor;
            this.priority = priority;
        }
    }

    private final List<Action> actions = new ArrayList<>();

    public void addAction(String name, Predicate<Agent> condition,
                         Consumer<Agent> executor, float priority) {
        actions.add(new Action(name, condition, executor, priority));
    }

    public void execute(Agent agent) {
        // Sort by priority
        actions.sort((a, b) -> Float.compare(b.priority, a.priority));

        // Execute first viable action
        for (Action action : actions) {
            if (action.condition.test(agent)) {
                action.executor.accept(agent);
                return;
            }
        }
    }
}

// Example: Warrior combat rotation
CombatRotation warriorRotation = new CombatRotation();

// Highest priority: Execute low health enemies
warriorRotation.addAction("Execute",
    agent -> {
        Entity target = agent.getTarget();
        return target != null && target.getHealthPercent() < 20 &&
               agent.getRage() >= 10;
    },
    agent -> {
        agent.castSpell("Execute");
    },
    100.0f
);

// Second: Maintain Rend debuff
warriorRotation.addAction("Rend",
    agent -> {
        Entity target = agent.getTarget();
        return target != null &&
               !target.hasDebuff("Rend") &&
               agent.getRage() >= 20;
    },
    agent -> {
        agent.castSpell("Rend");
    },
    80.0f
);

// Third: Use defensive cooldowns when low
warriorRotation.addAction("Shield Wall",
    agent -> agent.getHealthPercent() < 30 &&
             agent.canCast("Shield Wall"),
    agent -> agent.castSpell("Shield Wall"),
    95.0f
);

// Default: Heroic Strike for damage
warriorRotation.addAction("Heroic Strike",
    agent -> agent.getRage() >= 30 &&
             agent.getTarget() != null,
    agent -> agent.castSpell("Heroic Strike"),
    50.0f
);
```

---

## Code Examples

### Complete FPS Bot Example

```java
public class FPSBot {
    private final FSM fsm;
    private final SignalMemory memory;
    private final TargetSelector targetSelector;
    private final WeaponSystem weaponSystem;
    private final CombatRotation combatRotation;

    private Entity currentTarget;
    private Vector3f lastKnownEnemyPosition;
    private long lastEnemySeenTime;

    public FPSBot() {
        this.fsm = new FSM();
        this.memory = new SignalMemory();
        this.targetSelector = new TargetSelector();
        this.weaponSystem = new WeaponSystem();
        this.combatRotation = buildCombatRotation();

        setupFSM();
    }

    private void setupFSM() {
        fsm.addState(new IdleState());
        fsm.addState(new PatrolState());
        fsm.addState(new CombatState());
        fsm.addState(new RetreatState());
        fsm.addState(new SearchState());

        fsm.transitionTo("Idle");
    }

    public void update(float deltaTime) {
        // Update memory
        memory.cleanup();

        // Scan for enemies
        scanForEnemies();

        // Update FSM
        fsm.update(this, deltaTime);
    }

    private void scanForEnemies() {
        List<Entity> visibleEnemies = getVisibleEnemies();

        if (!visibleEnemies.isEmpty()) {
            Entity nearest = getNearestEntity(visibleEnemies);
            currentTarget = nearest;
            lastKnownEnemyPosition = nearest.getPosition();
            lastEnemySeenTime = System.currentTimeMillis();

            memory.addSignal(nearest.getPosition(),
                           SignalMemory.SIGHT,
                           1.0f);
        }
    }

    public Entity getCurrentTarget() {
        // Check if target is still valid
        if (currentTarget == null) return null;
        if (!currentTarget.isAlive()) {
            currentTarget = null;
            return null;
        }
        if (!canSee(currentTarget)) {
            // Lost sight of target
            if (System.currentTimeMillis() - lastEnemySeenTime > 5000) {
                currentTarget = null;
            }
            return null;
        }
        return currentTarget;
    }

    public Vector3f getLastKnownEnemyPosition() {
        return lastKnownEnemyPosition;
    }

    // States
    private class IdleState implements FSMState {
        @Override
        public void execute(Agent agent, float deltaTime) {
            Entity target = getCurrentTarget();
            if (target != null) {
                fsm.transitionTo("Combat");
                return;
            }

            if (lastKnownEnemyPosition != null &&
                System.currentTimeMillis() - lastEnemySeenTime < 10000) {
                fsm.transitionTo("Search");
                return;
            }

            if (shouldPatrol()) {
                fsm.transitionTo("Patrol");
            }
        }
    }

    private class CombatState implements FSMState {
        @Override
        public void execute(Agent agent, float deltaTime) {
            Entity target = getCurrentTarget();

            if (target == null) {
                if (lastKnownEnemyPosition != null) {
                    fsm.transitionTo("Search");
                } else {
                    fsm.transitionTo("Idle");
                }
                return;
            }

            // Check health
            if (agent.getHealthPercent() < 30) {
                fsm.transitionTo("Retreat");
                return;
            }

            // Combat behavior
            float distance = agent.distanceTo(target);
            Weapon bestWeapon = weaponSystem.selectBestWeapon(agent, target);

            // Movement
            if (distance > bestWeapon.getEffectiveRange()) {
                agent.moveTo(target.getPosition());
            } else if (distance < bestWeapon.getOptimalRange() * 0.5f) {
                // Too close, back up
                Vector3f retreatPos = agent.getPosition()
                    .sub(target.getPosition(), new Vector3f())
                    .normalize()
                    .scale(bestWeapon.getOptimalRange(), new Vector3f())
                    .add(target.getPosition(), new Vector3f());
                agent.moveTo(retreatPos);
            }

            // Aiming
            agent.aimAt(target);

            // Shooting
            if (agent.hasLineOfSight(target)) {
                combatRotation.execute(agent);
            }
        }
    }

    private class SearchState implements FSMState {
        @Override
        public void execute(Agent agent, float deltaTime) {
            Entity target = getCurrentTarget();
            if (target != null) {
                fsm.transitionTo("Combat");
                return;
            }

            if (lastKnownEnemyPosition == null ||
                System.currentTimeMillis() - lastEnemySeenTime > 15000) {
                fsm.transitionTo("Idle");
                return;
            }

            // Move to last known position
            agent.moveTo(lastKnownEnemyPosition);

            if (agent.distanceTo(lastKnownEnemyPosition) < 5.0f) {
                // Reached position, didn't find enemy
                fsm.transitionTo("Idle");
            }
        }
    }

    private class RetreatState implements FSMState {
        @Override
        public void execute(Agent agent, float deltaTime) {
            // Find cover
            Vector3f cover = findNearestCover();
            agent.moveTo(cover);

            // Use health item if available
            if (agent.getHealthPercent() < 50 && agent.hasItem("Medkit")) {
                agent.useItem("Medkit");
            }

            // Return to combat when healthy
            if (agent.getHealthPercent() > 80) {
                Entity target = getCurrentTarget();
                if (target != null) {
                    fsm.transitionTo("Combat");
                } else {
                    fsm.transitionTo("Idle");
                }
            }
        }
    }
}
```

---

## Lessons for LLM-Enhanced Automation

### 1. Combine Classical AI with LLM Reasoning

**Pattern:** Use LLM for high-level planning, classical systems for execution.

```java
public class HybridAISystem {
    private final LLMPlanner llmPlanner;
    private final FSM executionFSM;
    private final UtilitySystem utilityAI;

    public Plan handleUserCommand(String command) {
        // LLM analyzes natural language and creates high-level plan
        Plan plan = llmPlanner.createPlan(command, getCurrentContext());

        // Classical AI executes each step reliably
        for (PlanStep step : plan.getSteps()) {
            executeWithClassicalAI(step);
        }

        return plan;
    }

    private void executeWithClassicalAI(PlanStep step) {
        switch (step.getType()) {
            case "navigate":
                Vector3f target = step.getTargetPosition();
                List<Vector3f> path = pathfinding.findPath(getPosition(), target);
                navigationSystem.followPath(path);
                break;

            case "combat":
                executionFSM.transitionTo("Combat");
                break;

            case "collect":
                utilityAI.setGoal("collect_item", step.getTarget());
                break;
        }
    }
}
```

### 2. Use LLM to Generate Classical AI Parameters

**Pattern:** LLM can tune weights, thresholds, and parameters for classical systems.

```java
public class AdaptiveUtilityAI {
    private UtilityAI utilityAI;
    private LLMClient llmClient;

    public void adaptToSituation(GameState state) {
        // Ask LLM for optimal weights given current situation
        String prompt = String.format(
            "Given this game state: %s, what are optimal utility weights for: %s",
            state.getSummary(),
            utilityAI.getActionNames()
        );

        LLMResponse response = llmClient.complete(prompt);
        Map<String, Float> newWeights = parseWeights(response);

        // Apply weights to classical system
        utilityAI.updateWeights(newWeights);
    }
}
```

### 3. LLM-Enhanced Memory Systems

**Pattern:** Use LLM to analyze patterns in memory and predict behaviors.

```java
public class IntelligentMemory {
    private PlayerPatternMemory patternMemory;
    private LLMClient llmClient;

    public PlayerAction predictPlayerAction(Entity player) {
        // Gather observations
        List<PlayerObservation> observations =
            patternMemory.getRecentObservations(player.getId(), 20);

        if (observations.size() < 10) {
            return null; // Not enough data
        }

        // Use LLM to analyze patterns
        String prompt = String.format(
            "Analyze these player actions and predict what they'll do next: %s",
            formatObservations(observations)
        );

        LLMResponse response = llmClient.complete(prompt);
        return parsePredictedAction(response);
    }
}
```

### 4. Hybrid Decision Making

**Pattern:** LLM for novel situations, classical AI for routine tasks.

```java
public class HybridDecisionSystem {
    public Decision makeDecision(Agent agent, GameContext context) {
        // Check if we have a classical solution
        Decision classical = classicalAI.getDecision(agent, context);

        if (classical != null && classical.getConfidence() > 0.8f) {
            return classical; // Use reliable classical solution
        }

        // Novel situation - use LLM
        String prompt = buildContextualPrompt(agent, context);
        LLMResponse llmDecision = llmClient.complete(prompt);
        Decision llm = parseDecision(llmDecision);

        // Validate LLM decision with classical constraints
        if (classicalAI.isFeasible(llm)) {
            return llm;
        } else {
            return classical; // Fall back to classical
        }
    }
}
```

### 5. LLM-Generated Behavior Trees

**Pattern:** Use LLM to generate behavior trees for specific scenarios.

```java
public class DynamicBehaviorGenerator {
    private LLMClient llmClient;

    public BTNode generateBehavior(String taskDescription, GameContext context) {
        String prompt = String.format(
            "Generate a behavior tree for: %s\n" +
            "Available actions: %s\n" +
            "Available conditions: %s\n" +
            "Output in JSON format",
            taskDescription,
            context.getAvailableActions(),
            context.getAvailableConditions()
        );

        LLMResponse response = llmClient.complete(prompt);
        BehaviorTreeSpec spec = parseBehaviorTreeSpec(response);
        return buildBehaviorTree(spec);
    }

    private BTNode buildBehaviorTree(BehaviorTreeSpec spec) {
        // Convert LLM output to actual behavior tree
        switch (spec.type) {
            case "selector":
                return new Selector(
                    spec.children.stream()
                        .map(this::buildBehaviorTree)
                        .toArray(BTNode[]::new)
                );
            case "sequence":
                return new Sequence(
                    spec.children.stream()
                        .map(this::buildBehaviorTree)
                        .toArray(BTNode[]::new)
                );
            case "action":
                return new ActionNode(spec.actionName);
            case "condition":
                return new ConditionNode(spec.conditionName);
            default:
                throw new IllegalArgumentException("Unknown node type: " + spec.type);
        }
    }
}
```

### Key Takeaways

1. **Classical AI is still valuable:** FSM, Behavior Trees, Utility Systems, and pathfinding are efficient and reliable.
2. **LLMs complement rather than replace:** Use LLMs for planning, reasoning, and adaptation; use classical systems for execution.
3. **Memory is crucial:** Both classical and LLM systems benefit from rich memory systems.
4. **Trigger systems enable responsiveness:** Event-driven architectures work well with both approaches.
5. **Hierarchical planning works:** Decompose complex tasks into simpler subtasks.

---

## Sources

### Quake and FPS Bots
- QuakeWorld Wiki - Quake Bot History
- Quake III Arena Bot System Documentation
- PODBot Waypoint Documentation

### MMO Automation
- WoW Glider Documentation and Legal Cases
- HonorBuddy Architecture Documentation
- EasyUO Scripting Documentation

### RTS AI
- Age of Empires AI Scripting Guide
- BWAPI Documentation and Tutorials
- StarCraft AI Competition Papers

### Game AI Programming
- "Programming Game AI by Example" by Mat Buckland
- "AI Game Programming Wisdom" Series
- "Game AI Pro" Series

### Academic Research
- NCSoft Bot Detection Research
- Signal Memory Systems in Game AI
- Player Behavior Analysis Papers

---

## Appendix: Terminology

- **A* (A-Star):** Pathfinding algorithm using heuristics to find optimal paths
- **AAS (Area Awareness System):** Quake III's navigation system
- **Behavior Tree:** Hierarchical decision-making structure
- **Finite State Machine (FSM):** Model with discrete states and transitions
- **GOAP (Goal-Oriented Action Planning):** Planning system for goal achievement
- **HTN (Hierarchical Task Network):** Task decomposition planning system
- **Navigation Mesh:** 3D geometry representing walkable surfaces
- **Utility AI:** Decision system using weighted scoring
- **Waypoint:** Specific point in 3D space used for navigation
- **PODBot:** Ping of Death Bot for Counter-Strike
- **BWAPI:** Brood War API for StarCraft AI

---

**End of Document**

This document provides a comprehensive overview of pre-LLM game automation techniques. By understanding these historical approaches, we can better integrate them with modern LLM capabilities to create more robust, efficient, and intelligent automation systems.
