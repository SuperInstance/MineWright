# Chapter 2: First-Person Shooter Games - AI Without LLMs

**Dissertation:** Game AI Automation Techniques That Don't Require LLMs
**Chapter:** 2 - FPS Games (1993-2025)
**Author:** Claude Code Research Team
**Date:** 2025-02-28

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Classic FPS AI (1993-2005)](#2-classic-fps-ai-1993-2005)
3. [Core AI Techniques](#3-core-ai-techniques)
4. [State Machines and Decision Trees](#4-state-machines-and-decision-trees)
5. [Bot Evolution (2006-2025)](#5-bot-evolution-2006-2025)
6. [Extractable Patterns for Minecraft](#6-extractable-patterns-for-minecraft)
7. [Technical Implementation](#7-technical-implementation)
8. [Case Studies](#8-case-studies)
9. [Sources](#9-sources)

---

## 1. Introduction

First-Person Shooter (FPS) games have been at the forefront of AI development for over three decades. From the simple monster behaviors of DOOM (1993) to the sophisticated planning systems of F.E.A.R. (2005) and modern behavior trees in contemporary titles, FPS AI has evolved through classic techniques that remain relevant today—without requiring Large Language Models or machine learning.

This chapter examines the evolution of FPS AI from 1993 to 2025, focusing on techniques that can be applied to autonomous agent development in Minecraft and other games.

### Why FPS AI Matters for Minecraft

Despite the genre differences, FPS AI techniques are highly applicable to Minecraft combat and navigation:

- **3D Spatial Navigation**: Both genres require pathfinding in complex 3D environments
- **Combat Targeting**: Selecting and engaging enemies with various weapons/tools
- **Cover Systems**: Finding and using environmental protection
- **Squad Coordination**: Multi-agent tactics and communication
- **Threat Assessment**: Evaluating danger and prioritizing targets
- **State-Based Behavior**: Reacting to changing combat conditions

---

## 2. Classic FPS AI (1993-2005)

### 2.1 DOOM (1993) - Foundation of Monster AI

DOOM, released by id Software in December 1993, established fundamental patterns for FPS AI that would influence game design for decades.

#### Key Design Philosophy: Orthogonal Unit Differentiation

DOOM's design principle was that each monster should have unique, stable, clear, and learnable behavior patterns:

```pseudocode
// DOOM Monster Behavior Pattern
struct MonsterAI {
    string name
    float health
    float damage
    float speed
    AttackType attackType
    AggressionLevel aggression
    bool canMelee
    bool canRanged
    bool canInfight
}

// Monster activation triggers
function activateMonster(monster, trigger):
    if trigger == SEES_PLAYER or trigger == TAKES_DAMAGE or trigger == HEARS_ATTACK:
        monster.state = HUNTING
        monster.target = player
```

#### Infighting System

One of DOOM's most innovative features was monster infighting—monsters could accidentally hurt each other and would turn against their attacker instead of the player:

```pseudocode
// DOOM Infighting Logic
function processMonsterDamage(attacker, victim, damage):
    applyDamage(victim, damage)

    if victim != attacker and victim.target == attacker:
        // Already targeting attacker
        return

    if victim.target == PLAYER and attacker != PLAYER:
        // Monster was hunting player, now switches to attacking other monster
        victim.target = attacker
        victim.state = HUNTING

    if victim.state == DORMANT:
        victim.state = HUNTING
        victim.target = attacker
```

#### Monster AI Characteristics

1. **Activation System**: Monsters start dormant and activate when they:
   - See the player (line-of-sight check)
   - Take damage from any source
   - Hear a player attack (even silent attacks like the fist)

2. **Hunting Behavior**: Once activated, monsters pursue their target relentlessly, sensing target location even out of sight and navigating around obstacles

3. **Environmental Interaction**: Monsters can use teleporters, activate lifts, and open certain doors

4. **Stagger System**: Monsters can be interrupted (staggered) when hit, creating tactical depth

### 2.2 Quake (1996) - Bot AI Emergence

Quake introduced proper bot AI to multiplayer FPS games, though the original game didn't include navigation meshes—that technology came later.

#### Navigation Systems

Classic Quake bots used waypoint systems rather than navigation meshes:

```pseudocode
// Quake Waypoint Navigation
struct Waypoint {
    Vector3 position
    Waypoint[] connections
    string flags  // "jump", "crouch", "camp", "armor", "weapon"
}

struct BotNavigation {
    Waypoint[] waypoints
    Waypoint currentWaypoint
    Waypoint targetWaypoint
}

function navigateTo(bot, targetPosition):
    path = findPathAStar(bot.currentWaypoint, nearestWaypoint(targetPosition))
    bot.movementTarget = nextWaypointInPath(path)
```

#### BSP Tree for Visibility

Quake introduced Binary Space Partitioning (BSP) trees for efficient visibility determination and collision detection:

```pseudocode
// BSP Tree Structure
struct BSPNode {
    Plane splittingPlane
    BSPNode frontChild
    BSPNode backChild
    Polygon[] polygons  // Only in leaf nodes
}

// Visibility determination using PVS (Potentially Visible Set)
function isPotentiallyVisible(fromLeaf, toLeaf):
    return fromLeaf.pvs.contains(toLeaf)

function renderView(playerPosition):
    currentLeaf = findLeaf(playerPosition)
    for leaf in currentLeaf.pvs:
        if isInFrustum(leaf, playerPosition):
            renderPolygons(leaf)
```

### 2.3 Half-Life (1998) - Squad Tactics Revolution

Half-Life's HECU (Hazardous Environment Combat Unit) Marine AI was groundbreaking for introducing squad-based coordination to FPS games.

#### Squad Communication System

```pseudocode
// Half-Life Squad System
struct Squad {
    Marine[] members
    Marine leader
    Vector3 knownEnemyPosition
    float lastContactTime
}

struct Marine {
    Weapon weapon
    float health
    MarineRole role
    CombatState state
}

function updateSquadBehavior(squad):
    // Squad shares information instantly through leader
    if squad.leader == null:
        electNewLeader(squad)
        for marine in squad.members:
            marine.state = INDEPENDENT
    else:
        // Share enemy positions
        for marine in squad.members:
            if marine.canSeeEnemy():
                squad.knownEnemyPosition = marine.lastSeenEnemyPosition
                squad.lastContactTime = currentTime()

        // Coordinate attacks
        coordinateAssault(squad, squad.knownEnemyPosition)
```

#### Tactical Behaviors

Half-Life Marines were the first FPS enemies to:
- Work in squads of up to four members with a squad leader
- Share information instantly with each other (enemy locations)
- Use suppressive fire
- Place grenades to cover retreats
- Use laser trip mines to block player routes
- Flank and coordinate attacks
- Prioritize certain enemies (Alien Grunts and Gargantuas)

### 2.4 Counter-Strike Bots (2000s) - Waypoint Mastery

Counter-Strike bots, particularly the famous PODbot (Ping of Death), demonstrated sophisticated waypoint-based navigation.

#### Waypoint System

```pseudocode
// Counter-Strike Waypoint System
struct CSWaypoint {
    Vector3 position
    float radius  // Influence radius for bot positioning
    WaypointFlag flag  // "camp", "snipe", "bomb", "rescue", "hostage"
    WaypointConnection[] connections
}

enum WaypointFlag {
    NONE,
    CAMP,           // Camping spot
    SNIPE,          // Sniping position
    BOMB_SITE,      // Bomb plant/defuse location
    HOSTAGE_AREA,   // Hostage rescue zone
    JUMP,           // Requires jump
    CROUCH          // Requires crouch
}

// Bot navigation using waypoints
function navigateWaypoints(bot, destination):
    currentWp = findNearestWaypoint(bot.position)
    targetWp = findNearestWaypoint(destination)

    path = findPathAStar(currentWp, targetWp)

    for waypoint in path:
        if waypoint.flag == JUMP:
            bot.jump()
        else if waypoint.flag == CROUCH:
            bot.crouch()

        moveToPosition(bot, waypoint.position)
```

#### Weapon Selection Logic

```pseudocode
// Counter-Strike Bot Weapon Selection
function selectBestWeapon(bot, targetDistance, enemyVisible):
    bestWeapon = null
    bestScore = -1

    for weapon in bot.weapons:
        score = calculateWeaponUtility(weapon, targetDistance, enemyVisible)
        if score > bestScore:
            bestScore = score
            bestWeapon = weapon

    return bestWeapon

function calculateWeaponUtility(weapon, distance, enemyVisible):
    score = 0

    // Distance-based utility
    optimalRange = weapon.optimalRange
    distanceFactor = 1.0 - abs(distance - optimalRange) / optimalRange
    score += distanceFactor * 40

    // Damage consideration
    score += weapon.damage * 2

    // Accuracy at range
    accuracy = weapon.getAccuracyAtRange(distance)
    score += accuracy * 30

    // Ammo consideration
    if weapon.ammo < weapon.clipSize * 0.2:
        score -= 20  // Penalty for low ammo

    // Indoor penalty for sniper rifles
    if weapon.isSniper and bot.isIndoors():
        score -= 30

    return score
```

### 2.5 F.E.A.R. (2005) - GOAP Revolution

F.E.A.R. (First Encounter Assault Recon), developed by Monolith Productions with AI architect Jeff Orkin, introduced Goal-Oriented Action Planning (GOAP) to FPS games—a revolutionary approach that replaced complex finite state machines with flexible planning systems.

#### GOAP Fundamentals

GOAP is based on STRIPS-style planning where actions have:
- **Preconditions**: What must be true before the action can execute
- **Effects**: What becomes true after the action executes
- **Cost**: The "expense" of performing the action (time, distance, risk)

```pseudocode
// F.E.A.R. GOAP Action Structure
struct GOAPAction {
    string name
    map<string, bool> preconditions
    map<string, bool> effects
    float cost
    function execute(actor, target)
}

// Example actions
action TakeCover = {
    name: "Take Cover",
    preconditions: {
        "has_cover": true,
        "in_combat": true,
        "health_low": true
    },
    effects: {
        "in_cover": true,
        "exposed": false
    },
    cost: 5.0,
    execute: moveToCover
}

action FlankEnemy = {
    name: "Flank Enemy",
    preconditions: {
        "enemy_visible": true,
        "has_flank_route": true,
        "teammate_distacting": true
    },
    effects: {
        "enemy_flanked": true,
        "combat_advantage": true
    },
    cost: 15.0,
    execute: executeFlank
}
```

#### Planning Algorithm

```pseudocode
// GOAP Planning using A* Search
function planActions(worldState, goalState):
    openSet = PriorityQueue()
    closedSet = Set()

    startNode = {
        state: worldState,
        actions: [],
        g: 0,
        h: heuristic(worldState, goalState),
        f: heuristic(worldState, goalState)
    }

    openSet.push(startNode)

    while not openSet.isEmpty():
        current = openSet.pop()

        if satisfiesGoals(current.state, goalState):
            return current.actions

        closedSet.add(current.state)

        for action in getAvailableActions(current.state):
            if not satisfiesPreconditions(current.state, action):
                continue

            newState = applyEffects(current.state, action)

            if closedSet.contains(newState):
                continue

            newNode = {
                state: newState,
                actions: current.actions + [action],
                g: current.g + action.cost,
                h: heuristic(newState, goalState),
                f: current.g + action.cost + heuristic(newState, goalState)
            }

            openSet.push(newNode)

    return null  // No plan found
```

#### The 3-State Miracle

Despite its complex behavior, F.E.A.R.'s AI used only a simple 3-state finite state machine:
1. **Goto**: Movement to destination
2. **Execute Action**: Performing planned action
3. **Interact with Flexible Objects**: Using environment elements

```pseudocode
// F.E.A.R. Simple State Machine with Complex Planning
enum AIState {
    GOTO,
    EXECUTE_ACTION,
    INTERACT
}

struct FEAREnemy {
    AIState currentState
    Action[] currentPlan
    int currentActionIndex
    WorldState worldState
}

function updateAI(enemy):
    switch enemy.currentState:
        case GOTO:
            if reachedDestination(enemy):
                enemy.currentState = EXECUTE_ACTION
            else:
                continueMovement(enemy)

        case EXECUTE_ACTION:
            action = enemy.currentPlan[enemy.currentActionIndex]

            if action.isComplete():
                enemy.currentActionIndex++

                if enemy.currentActionIndex >= length(enemy.currentPlan):
                    // Plan complete, reassess and replan
                    replan(enemy)
                else:
                    executeNextAction(enemy)

        case INTERACT:
            if interactionComplete():
                enemy.currentState = GOTO
```

#### Cover System

```pseudocode
// F.E.A.R. Cover Detection and Usage
struct CoverPoint {
    Vector3 position
    float qualityScore
    bool protectsFromEnemy
    bool allowsShooting
    CoverType type  // "full", "half", "corner"
}

function findBestCover(agent, enemyPosition):
    availableCover = []

    // Scan environment for cover points
    for potentialCover in environment.coverPoints:
        if not lineOfSight(enemyPosition, potentialCover.position):
            // Protects from enemy
            if canShootFrom(potentialCover.position, enemyPosition):
                quality = calculateCoverQuality(agent, potentialCover)
                availableCover.add((potentialCover, quality))

    // Sort by quality and return best
    availableCover.sort(byQuality)
    return availableCover[0]

function calculateCoverQuality(agent, cover):
    score = 0

    // Distance from enemy (not too close, not too far)
    distance = distance(cover.position, agent.knownEnemyPosition)
    optimalDistance = 15.0  // meters
    score += 100 - abs(distance - optimalDistance) * 5

    // Protection level
    if cover.type == FULL:
        score += 50
    else if cover.type == HALF:
        score += 30
    else if cover.type == CORNER:
        score += 40

    // Allows return fire
    if cover.allowsShooting:
        score += 30

    // Proximity to agent
    distanceToAgent = distance(cover.position, agent.position)
    score -= distanceToAgent * 2

    return score
```

#### Squad Communication

```pseudocode
// F.E.A.R. Squad Coordination
struct SquadMember {
    string name
    Vector3 position
    Role role  // "pointman", "support", "flanker"
    Action currentAction
}

struct SquadCommunication {
    map<string, any> sharedKnowledge
    SquadMember[] members
}

function coordinateSquadAttack(squad, enemyPosition):
    // Assign roles based on situation
    pointman = selectPointman(squad)
    flanker = selectFlanker(squad)
    support = selectSupport(squad)

    // Share knowledge
    squad.sharedKnowledge["enemy_position"] = enemyPosition
    squad.sharedKnowledge["enemy_count"] = countVisibleEnemies(squad)

    // Plan coordinated attack
    pointman.assignPlan(createPlan("engage_front", enemyPosition))
    flanker.assignPlan(createPlan("flank_left", enemyPosition))
    support.assignPlan(createPlan("provide_suppression", enemyPosition))

    // Synchronize timing
    waitForAllReady(squad)
    signalAttack(squad)
```

---

## 3. Core AI Techniques

### 3.1 Finite State Machines (FSM)

FSMs are the most common AI technique in FPS games, where NPCs exist in defined states with clear transition rules.

#### Basic FSM Structure

```pseudocode
// Combat FSM
enum CombatState {
    IDLE,
    PATROL,
    ALERT,
    CHASE,
    ATTACK,
    RETREAT,
    SEARCH,
    DEAD
}

struct CombatAI {
    CombatState currentState
    float health
    Vector3 lastKnownEnemyPosition
    Weapon weapon
    float alertLevel
}

function updateCombatAI(ai):
    switch ai.currentState:
        case IDLE:
            if canSeeEnemy(ai):
                ai.currentState = ATTACK
            else if heardNoise(ai):
                ai.currentState = ALERT

        case ALERT:
            if canSeeEnemy(ai):
                ai.currentState = ATTACK
            else if alertExpired(ai):
                ai.currentState = PATROL
            else:
                investigateNoise(ai)

        case CHASE:
            if canSeeEnemy(ai):
                ai.currentState = ATTACK
            else if reachedLastKnownPosition(ai):
                ai.currentState = SEARCH
            else:
                moveTo(ai, ai.lastKnownEnemyPosition)

        case ATTACK:
            if not canSeeEnemy(ai):
                ai.currentState = CHASE
                ai.lastKnownEnemyPosition = getEnemyPosition()
            else if ai.health < 20:
                ai.currentState = RETREAT
            else:
                engageEnemy(ai)

        case RETREAT:
            if ai.health > 50 or foundCover(ai):
                ai.currentState = ATTACK
            else:
                moveToCover(ai)

        case SEARCH:
            if canSeeEnemy(ai):
                ai.currentState = ATTACK
            else if searchTimeout(ai):
                ai.currentState = PATROL
            else:
                searchArea(ai)

        case DEAD:
            // Terminal state
```

#### Hierarchical FSM (HFSM)

For complex behaviors, FSMs can be nested:

```pseudocode
// Hierarchical FSM for Tactical Combat
struct HFSMNode {
    string name
    HFSMNode[] children
    function execute
    function canTransitionTo
}

// Top-level states
hierarchy = {
    name: "Combat",
    children: [
        {
            name: "Offensive",
            children: [
                { name: "Assault", execute: assaultBehavior },
                { name: "Flank", execute: flankBehavior },
                { name: "Suppress", execute: suppressBehavior }
            ]
        },
        {
            name: "Defensive",
            children: [
                { name: "TakeCover", execute: takeCoverBehavior },
                { name: "Retreat", execute: retreatBehavior },
                { name: "HoldPosition", execute: holdPositionBehavior }
            ]
        },
        {
            name: "Support",
            children: [
                { name: "ProvideCoveringFire", execute: coveringFireBehavior },
                { name: "Regroup", execute: regroupBehavior },
                { name: "CallReinforcements", execute: callReinforcementsBehavior }
            ]
        }
    ]
}
```

### 3.2 Utility AI

Utility AI scores different actions based on multiple factors and selects the highest-scoring option.

```pseudocode
// Utility AI System
struct UtilityAction {
    string name
    function calculateScore
    function execute
}

function evaluateUtilityActions(agent, context):
    actions = [
        {
            name: "Attack",
            calculateScore: calculateAttackScore
        },
        {
            name: "TakeCover",
            calculateScore: calculateCoverScore
        },
        {
            name: "Reload",
            calculateScore: calculateReloadScore
        },
        {
            name: "Retreat",
            calculateScore: calculateRetreatScore
        },
        {
            name: "CallBackup",
            calculateScore: calculateBackupScore
        }
    ]

    bestAction = null
    bestScore = -1

    for action in actions:
        score = action.calculateScore(agent, context)

        // Add some randomness for variety
        score += random() * 0.1

        if score > bestScore:
            bestScore = score
            bestAction = action

    return bestAction

function calculateAttackScore(agent, context):
    score = 0

    // Base attack desire
    score += 50

    // Enemy visible bonus
    if context.enemyVisible:
        score += 30

    // Weapon has ammo
    if agent.weapon.ammo > 0:
        score += 20

    // Health consideration
    if agent.health < 30:
        score -= 40  // Don't attack if low health
    else if agent.health > 70:
        score += 10

    // Distance consideration
    distance = distance(agent.position, context.enemyPosition)
    if agent.weapon.isOptimalAtRange(distance):
        score += 15
    else:
        score -= 10

    // Cover penalty
    if not agent.inCover:
        score -= 20

    return clamp(score, 0, 100)

function calculateCoverScore(agent, context):
    score = 0

    // Base cover desire
    score += 20

    // Under fire bonus
    if context.underFire:
        score += 50

    // Low health bonus
    if agent.health < 40:
        score += 40

    // Not in cover bonus
    if not agent.inCover:
        score += 30

    // Cover nearby
    if hasNearbyCover(agent):
        score += 25

    // Enemy visible penalty (seek cover first)
    if context.enemyVisible:
        score += 20

    return clamp(score, 0, 100)
```

### 3.3 Behavior Trees

Behavior Trees provide a hierarchical, modular approach to AI decision-making.

```pseudocode
// Behavior Tree Nodes
enum BTNodeStatus {
    SUCCESS,
    FAILURE,
    RUNNING
}

// Composite Nodes
struct BTSequence {
    BTNode[] children

    function tick:
        for child in children:
            status = child.tick()
            if status != SUCCESS:
                return status
        return SUCCESS
}

struct BTSelector {
    BTNode[] children

    function tick:
        for child in children:
            status = child.tick()
            if status != FAILURE:
                return status
        return FAILURE
}

struct BTParallel {
    BTNode[] children
    Policy successPolicy  // "require_one" or "require_all"
    Policy failurePolicy

    function tick:
        results = []
        for child in children:
            results.append(child.tick())

        // Check success condition
        if successPolicy == "require_one":
            if any(results == SUCCESS):
                return SUCCESS
        else if successPolicy == "require_all":
            if all(results == SUCCESS):
                return SUCCESS

        // Check failure condition
        if failurePolicy == "require_one":
            if any(results == FAILURE):
                return FAILURE
        else if failurePolicy == "require_all":
            if all(results == FAILURE):
                return FAILURE

        return RUNNING
}

// Decorator Nodes
struct BTCondition {
    BTNode child
    function condition

    function tick:
        if condition():
            return child.tick()
        return FAILURE
}

struct BTRepeat {
    BTNode child
    int count

    function tick:
        for i in range(count):
            status = child.tick()
            if status != SUCCESS:
                return status
        return SUCCESS
}

// Combat Behavior Tree
combatBehaviorTree = BTSelector(
    children: [
        // Am I dead?
        BTSequence([
            BTCondition(condition: isDead),
            BTAction(execute: Die)
        ]),

        // Am I in critical danger?
        BTSequence([
            BTCondition(condition: healthCritical),
            BTSelector([
                BTAction(execute: CallForHelp),
                BTAction(execute: RetreatToCover)
            ])
        ]),

        // Do I need to reload?
        BTSequence([
            BTCondition(condition: weaponEmpty),
            BTCondition(condition: hasSafeReloadPosition),
            BTAction(execute: ReloadWeapon)
        ]),

        // Can I attack the enemy?
        BTSequence([
            BTCondition(condition: canSeeEnemy),
            BTCondition(condition: weaponReady),
            BTSelector([
                BTSequence([
                    BTCondition(condition: enemyInOptimalRange),
                    BTAction(execute: FireWeapon)
                ]),
                BTAction(execute: MoveToOptimalRange)
            ])
        ]),

        // Default: patrol
        BTAction(execute: Patrol)
    ]
)
```

### 3.4 A* Pathfinding

A* is the standard pathfinding algorithm in FPS games for waypoint and navigation mesh traversal.

```pseudocode
// A* Pathfinding
struct PathNode {
    Vector3 position
    PathNode parent
    float g  // Cost from start
    float h  // Heuristic to goal
    float f  // g + h
}

function findPathAStar(start, goal, graph):
    openSet = PriorityQueue()
    closedSet = Set()

    startNode = {
        position: start,
        parent: null,
        g: 0,
        h: heuristic(start, goal),
        f: heuristic(start, goal)
    }

    openSet.push(startNode)

    while not openSet.isEmpty():
        current = openSet.pop()

        if current.position == goal:
            return reconstructPath(current)

        closedSet.add(current.position)

        for neighbor in getNeighbors(current, graph):
            if closedSet.contains(neighbor.position):
                continue

            tentativeG = current.g + distance(current.position, neighbor.position)

            if not openSet.contains(neighbor) or tentativeG < neighbor.g:
                neighbor.parent = current
                neighbor.g = tentativeG
                neighbor.h = heuristic(neighbor.position, goal)
                neighbor.f = neighbor.g + neighbor.h

                if not openSet.contains(neighbor):
                    openSet.push(neighbor)

    return null  // No path found

function heuristic(from, to):
    // Euclidean distance for open spaces
    return distance(from, to)

function reconstructPath(node):
    path = []
    while node != null:
        path.prepend(node.position)
        node = node.parent
    return path
```

### 3.5 Navigation Meshes (NavMesh)

Modern FPS games use navigation meshes instead of waypoints for more flexible pathfinding.

```pseudocode
// Navigation Mesh Structure
struct NavPolygon {
    Vector3[] vertices
    NavPolygon[] neighbors
    Vector3 center
    float cost  // Traversal cost multiplier
}

struct NavMesh {
    NavPolygon[] polygons
    map<Vector3, NavPolygon> polygonLookup
}

function findPathNavMesh(start, goal, navMesh):
    startPoly = findPolygon(navMesh, start)
    goalPoly = findPolygon(navMesh, goal)

    if startPoly == null or goalPoly == null:
        return null

    // A* over polygons
    openSet = PriorityQueue()
    closedSet = Set()

    startNode = {
        polygon: startPoly,
        parent: null,
        g: 0,
        h: distance(startPoly.center, goalPoly.center),
        f: distance(startPoly.center, goalPoly.center)
    }

    openSet.push(startNode)

    while not openSet.isEmpty():
        current = openSet.pop()

        if current.polygon == goalPoly:
            return buildPathFromPolygons(current, start, goal)

        closedSet.add(current.polygon)

        for neighbor in current.polygon.neighbors:
            if closedSet.contains(neighbor):
                continue

            // Calculate traversal cost
            edgeMidpoint = findSharedEdge(current.polygon, neighbor)
            edgeCost = distance(current.polygon.center, edgeMidpoint)
            tentativeG = current.g + edgeCost * neighbor.cost

            if not openSet.contains(neighbor) or tentativeG < neighbor.g:
                neighbor.parent = current
                neighbor.g = tentativeG
                neighbor.h = distance(neighbor.center, goalPoly.center)
                neighbor.f = neighbor.g + neighbor.h

                if not openSet.contains(neighbor):
                    openSet.push(neighbor)

    return null

function findSharedEdge(poly1, poly2):
    // Find the shared edge between two adjacent polygons
    for i in range(poly1.vertices.length):
        v1 = poly1.vertices[i]
        v2 = poly1.vertices[(i + 1) % poly1.vertices.length]

        for j in range(poly2.vertices.length):
            v3 = poly2.vertices[j]
            v4 = poly2.vertices[(j + 1) % poly2.vertices.length]

            if (v1 == v3 and v2 == v4) or (v1 == v4 and v2 == v3):
                return (v1 + v2) / 2  // Midpoint of shared edge

    return null
```

---

## 4. State Machines and Decision Trees

### 4.1 Combat State Machine Example

```pseudocode
// Advanced Combat FSM with Tactical Decisions
struct TacticalAI {
    CombatState state
    CombatTactic tactic
    TargetInfo target
    SquadInfo squad
    EnvironmentInfo environment
}

enum CombatTactic {
    AGGRESSIVE_ASSAULT,
    DEFENSIVE_HOLD,
    FLANKING_MANEUVER,
    SUPPRESSION_FIRE,
    GUERRILLA_AMBUSH,
    RETREAT_AND_REGROUP
}

function updateTacticalAI(ai):
    // Assess current situation
    threatLevel = assessThreat(ai)
    squadStrength = calculateSquadStrength(ai.squad)
    enemyStrength = calculateEnemyStrength(ai.target)
    advantage = squadStrength - enemyStrength

    // Choose tactic based on situation
    if advantage > 30 and ai.target.inOpen:
        ai.tactic = AGGRESSIVE_ASSAULT
    else if advantage > 0 and ai.environment.hasCover:
        ai.tactic = DEFENSIVE_HOLD
    else if advantage < -20 and ai.squad.canFlank:
        ai.tactic = FLANKING_MANEUVER
    else if advantage < -40:
        ai.tactic = RETREAT_AND_REGROUP
    else if ai.environment.hasAmbushPositions:
        ai.tactic = GUERRILLA_AMBUSH

    // Execute tactic
    executeTactic(ai, ai.tactic)

function executeTactic(ai, tactic):
    switch tactic:
        case AGGRESSIVE_ASSAULT:
            moveAggressively(ai)
            fireAtWill(ai)
            callForSupport(ai)

        case DEFENSIVE_HOLD:
            moveToCover(ai)
            maintainOverwatch(ai)
            conserveAmmo(ai)

        case FLANKING_MANEUVER:
            splitSquad(ai)
            diversionTeam(ai).createDistraction()
            flankTeam(ai).executeFlank()

        case SUPPRESSION_FIRE:
            if ai.squad.hasSupportWeapons:
                suppressEnemy(ai)
            else:
                harassEnemy(ai)

        case GUERRILLA_AMBUSH:
            moveToAmbushPosition(ai)
            waitForTarget(ai)
            executeAmbush(ai)

        case RETREAT_AND_REGROUP:
            laySuppressiveFire(ai)
            fallBack(ai)
            regroup(ai)
```

### 4.2 Decision Tree for Target Selection

```pseudocode
// Target Selection Decision Tree
struct TargetCandidate {
    Entity entity
    float distance
    float threat
    float priority
    bool isVisible
    bool isInOptimalRange
}

function selectBestTarget(ai, candidates):
    if candidates.isEmpty():
        return null

    // Filter by visibility
    visibleTargets = candidates.filter(t -> t.isVisible)
    if visibleTargets.isEmpty():
        // Target last known position
        return getHighestPriorityTarget(candidates)

    // Filter by combat role
    if ai.role == SNIPER:
        return selectSniperTarget(visibleTargets)
    else if ai.role == ASSAULT:
        return selectAssaultTarget(visibleTargets)
    else if ai.role == SUPPORT:
        return selectSupportTarget(visibleTargets)

function selectAssaultTarget(targets):
    // Prioritize: closest > most threatening > lowest health

    // First, filter by optimal range
    inRangeTargets = targets.filter(t -> t.isInOptimalRange)
    if inRangeTargets.isEmpty():
        inRangeTargets = targets  // Consider all if none in optimal range

    // Score targets
    bestTarget = null
    bestScore = -1

    for target in inRangeTargets:
        score = 0

        // Distance factor (closer = higher priority)
        score += (100 - target.distance) * 0.4

        // Threat factor (more threatening = higher priority)
        score += target.threat * 0.3

        // Health factor (lower health = higher priority)
        score += (100 - target.entity.health) * 0.2

        // Class factor (prioritize certain classes)
        if target.entity.class == MEDIC:
            score += 20
        else if target.entity.class == HEAVY:
            score += 10

        if score > bestScore:
            bestScore = score
            bestTarget = target

    return bestTarget
```

### 4.3 Cover Decision Tree

```pseudocode
// Cover System Decision Tree
struct CoverDecision {
    CoverPoint coverPoint
    float urgency
    CoverAction action
}

enum CoverAction {
    MOVE_TO_COVER,
    FIRE_FROM_COVER,
    BLIND_FIRE,
    RELOAD_FROM_COVER,
    WAIT_FOR_SUPPORT
}

function decideCoverAction(ai):
    if not ai.inCover:
        if hasImmediateCover(ai):
            return MOVE_TO_COVER
        else if canReachCover(ai):
            return MOVE_TO_COVER
        else:
            return FIRE_FROM_OPEN  // No cover available

    // Already in cover
    if ai.underFire:
        if ai.canFireWithoutExposure:
            return BLIND_FIRE
        else:
            return WAIT_FOR_SUPPORT

    if ai.weapon.ammo == 0:
        return RELOAD_FROM_COVER

    if ai.hasClearShot:
        return FIRE_FROM_COVER
    else:
        return WAIT_FOR_SUPPORT  // Wait for better angle

function evaluateCoverPoint(ai, coverPoint):
    score = 0

    // Protection quality
    if coverPoint.type == FULL:
        score += 50
    else if coverPoint.type == HALF:
        score += 30
    else if coverPoint.type == CORNER:
        score += 35

    // Distance to cover
    distanceToCover = distance(ai.position, coverPoint.position)
    if distanceToCover < 5:
        score += 30  // Close cover is better
    else if distanceToCover < 15:
        score += 20
    else:
        score -= 10  // Too far

    // Can return fire
    if coverPoint.allowsShooting:
        score += 25

    // Enemy angle coverage
    enemyAngle = angleToEnemy(ai, coverPoint)
    if coverPoint.protectsFromAngle(enemyAngle):
        score += 40
    else:
        score -= 20  // Cover doesn't protect

    // Proximity to teammates
    nearbyTeammates = countTeammatesNear(coverPoint.position, 10)
    if nearbyTeammates > 0:
        score += 10 * nearbyTeammates

    return score
```

---

## 5. Bot Evolution (2006-2025)

### 5.1 Quake III Arena Bots (1999)

Quake III Arena featured some of the most sophisticated bot AI of its era without using machine learning.

#### Neural Network Controversy

While often rumored to use neural networks, Quake III bots actually used:
- **Finite State Machines** for high-level behavior
- **Waypoint navigation** (AAS - Area Awareness System)
- **Item timing prediction** for powerup control
- **Weapon preference tables** for different situations

```pseudocode
// Quake III Bot AI Structure
struct Q3Bot {
    string name
    float skill  // 0-1
    int personality  // Aggressive, Defensive, Balanced
    WeaponPreference[] weaponPreferences
    float[] itemTimings
    FSM brain
}

function updateQ3Bot(bot):
    // Update state machine
    bot.brain.update(bot)

    // Item prediction
    for item in level.items:
        predictedSpawn = predictItemSpawn(item)
        bot.itemTimings[item.id] = predictedSpawn

    // Combat decision
    if bot.brain.currentState == COMBAT:
        enemy = bot.brain.getTarget()
        weapon = selectWeapon(bot, enemy)
        attack(bot, enemy, weapon)

function predictItemSpawn(item):
    // Items respawn on fixed intervals (e.g., 25 seconds for armor)
    timeSincePickup = currentTime() - item.lastPickedUp
    respawnTime = item.respawnInterval

    if timeSincePickup >= respawnTime:
        return currentTime() + 0.1  // Due any moment
    else:
        return item.lastPickedUp + respawnTime
```

#### Tactical Goal System

```pseudocode
// Quake III Tactical Goal Decision System
enum GameType {
    FFA,          // Free For All
    TEAM,         // Team Deathmatch
    CTF,          // Capture The Flag
    1FCTF,        // One Flag CTF
    OBELISK       // Obelisk Mode
}

struct TacticalGoal {
    string name
    float priority
    Vector3 position
    function execute
}

function selectTacticalGoal(bot):
    goals = []

    switch bot.gameType:
        case FFA:
            goals = [
                {
                    name: "Get Armor",
                    priority: 0.7,
                    position: level.nearestArmor.position,
                    execute: pickupItem
                },
                {
                    name: "Get Weapon",
                    priority: 0.6,
                    position: level.nearestWeapon.position,
                    execute: pickupItem
                },
                {
                    name: "Attack Enemy",
                    priority: 0.8,
                    position: bot.nearestEnemy.position,
                    execute: attackEnemy
                },
                {
                    name: "Get Health",
                    priority: bot.health < 50 ? 0.9 : 0.3,
                    position: level.nearestHealth.position,
                    execute: pickupItem
                }
            ]

        case CTF:
            goals = [
                {
                    name: "Defend Flag",
                    priority: bot.role == DEFENDER ? 0.9 : 0.4,
                    position: bot.teamFlag.position,
                    execute: defendFlag
                },
                {
                    name: "Attack Enemy Flag",
                    priority: bot.role == ATTACKER ? 0.9 : 0.5,
                    position: bot.enemyFlag.position,
                    execute: attackFlag
                },
                {
                    name: "Return Flag",
                    priority: bot.teamFlag.isStolen ? 1.0 : 0.0,
                    position: bot.teamFlag.position,
                    execute: returnFlag
                },
                {
                    name: "Capture Flag",
                    priority: bot.hasEnemyFlag ? 1.0 : 0.0,
                    position: bot.teamFlag.position,
                    execute: captureFlag
                }
            ]

    // Select highest priority goal
    goals.sort(byPriority)
    return goals[0]
```

### 5.2 Counter-Strike: Source / CS:GO Bot Improvements

Modern Counter-Strike bots improved upon earlier versions with:
- **Navigation mesh-based pathfinding** (instead of waypoints)
- **Learning from human players** (waypoint learning)
- **Improved weapon selection**
- **Better awareness of sound**
- **Coordinated team tactics**

```pseudocode
// Modern CS Bot Navigation
struct CS2Bot {
    NavMesh navMesh
    DecisionTree behaviorTree
    WeaponSystem weaponSystem
    AwarenessSystem awareness
    TeamCoordinator team
}

function updateCS2Bot(bot):
    // Update awareness
    bot.awareness.update(bot)

    // Check for new threats
    if bot.awareness.heardGunshot:
        bot.memory.remember(bot.awareness.lastSoundLocation, currentTime())

    // Coordinate with team
    if bot.team.shouldCoordinate():
        bot.team.assignRole(bot)

    // Execute behavior tree
    bot.behaviorTree.tick(bot)

// CS:GO-style learning system
struct WaypointLearner {
    map<Vector3, int> humanPositionFrequency
    map<Vector3, float> dangerRating
}

function learnFromHumans(learner, humanPlayers):
    for player in humanPlayers:
        position = player.position

        // Record where humans go
        learner.humanPositionFrequency[position]++

        // Record danger zones
        if player.justDied():
            learner.dangerRating[player.deathLocation] += 1.0

function suggestBottifulPositions(learner):
    // Suggest positions that humans frequent but aren't too dangerous
    suggestions = []
    for position in learner.humanPositionFrequency.keys:
        frequency = learner.humanPositionFrequency[position]
        danger = learner.dangerRating[position]

        score = frequency - danger * 2
        suggestions.add((position, score))

    suggestions.sort(byScore)
    return suggestions
```

### 5.3 Tactical Shooter AI (Rainbow Six, SWAT)

Tactical shooters emphasize:
- **Room clearing procedures**
- **Stacking and breaching**
- **Cover and concealment**
- **Rules of engagement**
- **Civilian consideration**

```pseudocode
// Rainbow Six-style Tactical AI
struct TacticalTeam {
    TacticalOperative[] members
    RoomClearOrder currentOrder
    BreachMethod breachMethod
    RulesOfEngagement roe
}

enum BreachMethod {
    STEALTH,
    SPEED_AND_DYNAMIC,
    DELAYED,
    ROOM_BY_ROOM
}

function executeRoomClear(team, room):
    // Stack on door
    team.members[0].moveTo(room.doorPosition)
    team.members[1].moveTo(room.doorPosition + offset(1))
    team.members[2].moveTo(room.doorPosition + offset(2))
    team.members[3].prepareGrenade()

    // Wait for all in position
    waitForReady(team)

    // Execute breach
    switch team.breachMethod:
        case SPEED_AND_DYNAMIC:
            team.members[3].throwFlashbang()
            wait(0.5)  // Wait for bang
            team.members[0].enterRoom(room)
            team.members[1].enterRoom(room)
            team.members[2].enterRoom(room)

        case STEALTH:
            if canOpenDoorQuietly(room.door):
                team.members[0].openDoorQuietly()
                team.members[0].throwGasGrenade()
                wait(2.0)
                team.enterRoom(room)

    // Clear corners
    for operative in team.members:
        clearCorners(operative, room)

    // Secure room
    team.currentOrder = SECURE
    for operative in team.members:
        operative.takePosition(room.coverPoint)
```

### 5.4 Modern Behavior Trees (Unreal Engine)

Modern FPS games use behavior trees extensively through engines like Unreal Engine.

```pseudocode
// Unreal Engine-style Behavior Tree
struct UBehaviorTree {
    BTCompositeNode root
    BlackboardData blackboard
    AISpecificData aiData
}

// Combat behavior tree example
combatBT = UBehaviorTree(
    root: BTSelector(
        children: [
            // Death check
            BTSequence([
                BTBlackboardCheck("IsDead", isEqual: true),
                BTTask("Die")
            ]),

            // Critical health
            BTSequence([
                BTBlackboardCheck("Health", lessThan: 30),
                BTSelector([
                    BTSequence([
                        BTService("FindCover"),
                        BTDecorator("HasCover"),
                        BTTask("MoveToCover")
                    ]),
                    BTTask("Retreat")
                ])
            ]),

            // Combat mode
            BTSequence([
                BTBlackboardCheck("HasTarget", isEqual: true),
                BTSelector([
                    // Range check
                    BTSequence([
                        BTDecorator("TargetOutOfRange"),
                        BTTask("MoveToRange")
                    ]),
                    // Weapon ready
                    BTSequence([
                        BTDecorator("WeaponEmpty"),
                        BTTask("ReloadWeapon")
                    ]),
                    // Attack
                    BTSequence([
                        BTDecorator("CanSeeTarget"),
                        BTTask("FireWeapon")
                    ])
                ])
            ]),

            // Patrol
            BTTask("Patrol")
        ]
    )
)
```

---

## 6. Extractable Patterns for Minecraft

### 6.1 Combat Targeting

FPS targeting systems translate well to Minecraft combat:

```pseudocode
// Minecraft Combat Targeting
struct MinecraftCombatAI {
    Entity[] nearbyEntities
    Entity currentTarget
    Weapon currentWeapon
    float targetPriority
}

function selectCombatTarget(ai):
    candidates = getNearbyHostileEntities(ai, 32)

    if candidates.isEmpty():
        ai.currentTarget = null
        return

    bestTarget = null
    bestScore = -1

    for entity in candidates:
        score = calculateTargetScore(ai, entity)
        if score > bestScore:
            bestScore = score
            bestTarget = entity

    ai.currentTarget = bestTarget

function calculateTargetScore(ai, entity):
    score = 0

    // Distance factor (closer = higher priority)
    distance = distance(ai.position, entity.position)
    score += max(0, 32 - distance) * 2

    // Threat level (more dangerous = higher priority)
    threat = getEntityThreatLevel(entity)
    score += threat * 15

    // Health factor (weaker = higher priority)
    score += (entity.maxHealth - entity.health) * 0.5

    // Player-owned entities have lower priority
    if entity.isPlayerOwned():
        score -= 10

    // Current attacker bonus
    if entity == ai.lastAttacker:
        score += 20

    // Shield check (has shield = lower priority)
    if entity.hasActiveShield():
        score -= 15

    return score
```

### 6.2 Path Planning Under Fire

FPS movement tactics apply to Minecraft combat:

```pseudocode
// Minecraft Combat Movement
enum CombatMovementTactic {
    STRAFE,
    RETREAT,
    ADVANCE,
    CIRCLE,
    KITE
}

function executeCombatMovement(ai):
    if ai.currentTarget == null:
        return

    distance = distance(ai.position, ai.currentTarget.position)
    healthPercent = ai.health / ai.maxHealth

    // Decide tactic based on situation
    if healthPercent < 0.3:
        tactic = RETREAT
    else if distance < 5:
        tactic = STRAFE
    else if distance > 15:
        tactic = ADVANCE
    else if ai.hasRangedWeapon:
        tactic = KITE
    else:
        tactic = CIRCLE

    switch tactic:
        case RETREAT:
            direction = normalize(ai.position - ai.currentTarget.position)
            moveDirection = direction + perpendicular(direction) * 0.3  // Strafe while retreating
            ai.setMoveDirection(moveDirection)

        case STRAFE:
            direction = normalize(ai.currentTarget.position - ai.position)
            strafeDirection = perpendicular(direction)

            // Randomly switch strafe direction
            if random() < 0.1:
                ai.strafeDirection = -ai.strafeDirection

            ai.setMoveDirection(strafeDirection * ai.strafeDirection)

        case ADVANCE:
            direction = normalize(ai.currentTarget.position - ai.position)

            // Zigzag approach
            if random() < 0.2:
                zigzag = perpendicular(direction)
                direction = direction + zigzag * 0.5

            ai.setMoveDirection(direction)

        case KITE:
            direction = normalize(ai.currentTarget.position - ai.position)

            // Maintain ideal range
            if distance < 12:
                moveDirection = -direction  // Back away
            else if distance > 18:
                moveDirection = direction  // Close in
            else:
                moveDirection = perpendicular(direction)  // Strafe

            ai.setMoveDirection(moveDirection)

        case CIRCLE:
            direction = normalize(ai.currentTarget.position - ai.position)
            circleDirection = perpendicular(direction)

            if random() < 0.05:
                ai.circleDirection = -ai.circleDirection

            ai.setMoveDirection(circleDirection * ai.circleDirection)
```

### 6.3 Weapon/Tool Selection

FPS weapon selection logic maps to Minecraft items:

```pseudocode
// Minecraft Item Selection
struct ItemUtility {
    Item item
    float utility
}

function selectBestItem(ai, situation):
    items = ai.inventory.getItems()

    bestItem = null
    bestUtility = -1

    for item in items:
        utility = calculateItemUtility(ai, item, situation)
        if utility > bestUtility:
            bestUtility = utility
            bestItem = item

    return bestItem

function calculateItemUtility(ai, item, situation):
    utility = 0

    distance = situation.targetDistance

    // Sword (melee combat)
    if item.type == SWORD:
        if distance < 4:
            utility += 50 + item.damage * 5
        else if distance < 6:
            utility += 20 + item.damage * 2
        else:
            utility += 0  // Out of range

        // Sharpness enchantment bonus
        utility += item.enchantments.get("sharpness") * 5

    // Bow (ranged combat)
    else if item.type == BOW:
        if distance > 8:
            utility += 40 + item.damage * 3
        else if distance > 15:
            utility += 60  // Optimal range

        // Power enchantment bonus
        utility += item.enchantments.get("power") * 5

        // Ammo check
        if not ai.inventory.hasItem(ARROW):
            utility -= 100  # Can't use without arrows

    // Crossbow (higher damage, slower)
    else if item.type == CROSSBOW:
        if distance > 8:
            utility += 50 + item.damage * 4
        else if distance > 20:
            utility += 70

        // Piercing/Multishot bonuses
        utility += item.enchantments.get("piercing") * 7
        utility += item.enchantments.get("multishot") * 10

        if not ai.inventory.hasItem(ARROW) or not ai.inventory.hasItem(FIREWORK):
            utility -= 100

    // Trident (melee + ranged)
    else if item.type == TRIDENT:
        if distance < 4:
            utility += 45 + item.damage * 4
        else if distance < 20 and item.enchantments.has("loyalty"):
            utility += 55 + item.damage * 3

        utility += item.enchantments.get("loyalty") * 8
        utility += item.enchantments.get("riptide") * 5
        utility += item.enchantments.get("channeling") * 3

    // Shield (defense)
    else if item.type == SHIELD:
        if ai.health < ai.maxHealth * 0.5:
            utility += 40
        if ai.isTakingDamage():
            utility += 30

    // Food (healing)
    else if item.type == FOOD:
        if ai.health < ai.maxHealth * 0.7:
            hungerRestore = item.hungerRestore
            healthRestore = item.saturationRestore
            utility += (hungerRestore + healthRestore) * 10

    // Block (for cover)
    else if item.type == BLOCK and situation.inCombat:
        if ai.needsCover():
            utility += 30
        if item.type == OBSIDIAN or item.type == NETHERITE_BLOCK:
            utility += 10  # Better blocks

    // Utility penalty for low durability
    durabilityPercent = item.durability / item.maxDurability
    utility *= durabilityPercent

    return utility
```

### 6.4 Threat Assessment

```pseudocode
// Minecraft Threat Assessment
struct ThreatInfo {
    Entity entity
    float threatLevel
    float attackPower
    float attackSpeed
    float range
    bool canReachMe
    float timeToReach
}

function assessThreats(ai):
    entities = getNearbyEntities(ai, 64)
    threats = []

    for entity in entities:
        if not entity.isHostile():
            continue

        threat = calculateThreat(ai, entity)
        threats.add(threat)

    threats.sort(byThreatLevel)
    return threats

function calculateThreat(ai, entity):
    threat = ThreatInfo()
    threat.entity = entity
    threat.attackPower = estimateEntityAttackPower(entity)
    threat.attackSpeed = estimateEntityAttackSpeed(entity)
    threat.range = estimateEntityAttackRange(entity)

    distance = distance(ai.position, entity.position)
    threat.canReachMe = distance <= threat.range

    if threat.canReachMe:
        threat.timeToReach = 0
    else:
        # Estimate time to reach us
        entitySpeed = getEntityMovementSpeed(entity)
        threat.timeToReach = (distance - threat.range) / entitySpeed

    # Calculate DPS
    dps = threat.attackPower * threat.attackSpeed

    # Threat decreases with time (immediate threats worse)
    threat.threatLevel = dps / (1 + threat.timeToReach * 0.5)

    # Player-owned entities are lower priority
    if entity.isPlayerOwned():
        threat.threatLevel *= 0.7

    # Bosses get bonus threat
    if entity.isBoss():
        threat.threatLevel *= 1.5

    # Flying entities get bonus (can reach us easier)
    if entity.canFly():
        threat.threatLevel *= 1.3

    # Entities that can break blocks get bonus
    if entity.canBreakBlocks():
        threat.threatLevel *= 1.4

    return threat
```

### 6.5 Retreat/Advance Decisions

```pseudocode
// Tactical Positioning Decisions
struct CombatPosition {
    Vector3 position
    float tacticalValue
    float distanceToEnemy
    float coverLevel
    float escapeRoute
    float heightAdvantage
}

function decideTacticalPosition(ai):
    currentSituation = assessCombatSituation(ai)

    if currentSituation.shouldRetreat:
        return findRetreatPosition(ai)
    else if currentSituation.shouldAdvance:
        return findAdvancePosition(ai)
    else:
        return findHoldPosition(ai)

function assessCombatSituation(ai):
    situation = {}
    situation.myHealth = ai.health / ai.maxHealth
    situation.enemyHealth = ai.currentTarget.health / ai.currentTarget.maxHealth
    situation.myWeaponPower = ai.currentItem.damage
    situation.enemyWeaponPower = estimateEnemyDamage(ai.currentTarget)
    situation.distanceToEnemy = distance(ai.position, ai.currentTarget.position)
    situation.ammoCount = ai.inventory.countItem(ai.currentItem.ammoType)
    situation.nearbyAllies = countAlliesInRange(ai, 16)
    situation.nearbyEnemies = countEnemiesInRange(ai, 16)

    # Calculate power balance
    situation.powerBalance = (situation.myWeaponPower * situation.myHealth) /
                             (situation.enemyWeaponPower * situation.enemyHealth + 0.1)

    # Should retreat?
    situation.shouldRetreat = (
        situation.myHealth < 0.3 or
        situation.powerBalance < 0.5 or
        (situation.ammoCount == 0 and situation.distanceToEnemy < 8)
    )

    # Should advance?
    situation.shouldAdvance = (
        situation.myHealth > 0.7 and
        situation.powerBalance > 1.5 and
        situation.distanceToEnemy > 15
    )

    return situation

function findRetreatPosition(ai):
    candidates = []

    # Look for positions with cover
    for block in getNearbyBlocks(ai.position, 20):
        if not block.isSolid():
            continue

        # Check if this provides cover
        coverPosition = findCoverPositionNear(block)
        if coverPosition == null:
            continue

        position = CombatPosition()
        position.position = coverPosition
        position.distanceToEnemy = distance(coverPosition, ai.currentTarget.position)
        position.coverLevel = calculateCoverLevel(coverPosition, ai.currentTarget.position)
        position.escapeRoute = countEscapeRoutes(coverPosition)
        position.heightAdvantage = 0  # Retreating, so height less important

        # Score position
        position.tacticalValue = (
            position.coverLevel * 30 +
            position.escapeRoute * 20 +
            position.distanceToEnemy * 1.5
        )

        candidates.add(position)

    if candidates.isEmpty():
        # No cover found, just run away
        direction = normalize(ai.position - ai.currentTarget.position)
        return ai.position + direction * 20

    candidates.sort(byTacticalValue)
    return candidates[0].position
```

### 6.6 Cover System for Minecraft

```pseudocode
// Minecraft Cover Detection and Usage
struct MinecraftCover {
    Vector3 position
    Block protectingBlock
    float protectionQuality
    bool allowsShooting
    float height
}

function findCover(ai, enemyPosition):
    candidates = []

    # Scan nearby blocks for potential cover
    scanRadius = 16
    for x in range(-scanRadius, scanRadius):
        for y in range(-3, 4):
            for z in range(-scanRadius, scanRadius):
                blockPos = ai.position + Vector3(x, y, z)
                block = getBlock(blockPos)

                if not block.isSolid():
                    continue

                # Check if we can take cover behind this block
                coverPosition = findCoverPosition(blockPos, enemyPosition)
                if coverPosition == null:
                    continue

                cover = MinecraftCover()
                cover.position = coverPosition
                cover.protectingBlock = block
                cover.protectionQuality = calculateProtectionQuality(coverPosition, blockPos, enemyPosition)
                cover.allowsShooting = canShootFrom(coverPosition, enemyPosition)
                cover.height = blockPos.y - ai.position.y

                # Only consider cover that's not too high or low
                if cover.height < -2 or cover.height > 2:
                    continue

                candidates.add(cover)

    if candidates.isEmpty():
        return null

    # Score candidates
    for cover in candidates:
        score = cover.protectionQuality * 40

        if cover.allowsShooting:
            score += 30

        # Prefer cover at our level
        if abs(cover.height) <= 1:
            score += 20

        # Distance from enemy (not too close, not too far)
        distanceToEnemy = distance(cover.position, enemyPosition)
        if distanceToEnemy > 8 and distanceToEnemy < 20:
            score += 15

        cover.tacticalScore = score

    candidates.sort(byTacticalScore)
    return candidates[0]

function findCoverPosition(blockPos, enemyPosition):
    # The cover position is adjacent to the block, on the opposite side from enemy
    direction = normalize(blockPos - enemyPosition)

    # Check positions around the block
    for offset in [direction * -1, direction * -0.5]:
        coverPos = blockPos + offset

        if not isWalkable(coverPos):
            continue

        # Check if this position actually provides cover
        if lineOfSight(enemyPosition, coverPos):
            continue  # Enemy can see us, not good cover

        return coverPos

    return null

function calculateProtectionQuality(coverPos, blockPos, enemyPosition):
    quality = 0

    # Check if block actually blocks line of sight
    enemyDir = normalize(enemyPosition - coverPos)
    blockDir = normalize(blockPos - coverPos)

    angle = angleBetween(enemyDir, blockDir)
    if angle < 45 degrees:
        quality = 100  # Perfect cover
    elif angle < 90 degrees:
        quality = 70   # Good cover
    elif angle < 135 degrees:
        quality = 40   # Partial cover
    else:
        quality = 10   # Poor cover

    # Block hardness matters
    blockHardness = getBlockHardness(blockPos)
    quality *= blockHardness

    return quality
```

---

## 7. Technical Implementation

### 7.1 Pseudocode: Complete FPS AI System

```pseudocode
// Complete FPS AI System Integration
struct FPSAgent {
    // Core systems
    FSM brain
    Blackboard memory
    PerceptionSystem perception
    NavigationSystem navigation
    CombatSystem combat
    SquadSystem squad

    // State
    Vector3 position
    Vector3 velocity
    float health
    Weapon currentWeapon
    Entity currentTarget

    // Configuration
    AIProfile profile
}

function updateFPSAgent(agent, deltaTime):
    # 1. Update perception
    agent.perception.update(agent, deltaTime)

    # 2. Update blackboard with perceived information
    updateBlackboard(agent)

    # 3. Run state machine
    agent.brain.update(agent, deltaTime)

    # 4. Execute current state's behavior
    executeCurrentState(agent, deltaTime)

// Perception System
struct PerceptionSystem {
    float visionRange
    float visionAngle
    float hearingRange
    map<Entity, float> seenEntities
    map<Sound, Vector3> heardSounds
}

function updatePerception(perception, agent, deltaTime):
    # Visual perception
    for entity in getEntitiesInRange(agent.position, perception.visionRange):
        if not isInVisionCone(agent, entity):
            continue

        if not lineOfSight(agent.position, entity.position):
            continue

        distance = distance(agent.position, entity.position)
        visibility = 1.0 - (distance / perception.visionRange)
        perception.seenEntities[entity] = visibility

    # Auditory perception
    for sound in getActiveSounds():
        if sound.position == null:
            continue  # No position

        distance = distance(agent.position, sound.position)

        if distance <= perception.hearingRange:
            loudness = sound.volume / (distance * distance)
            if loudness > 0.1:
                perception.heardSounds[sound] = sound.position

// Combat System
struct CombatSystem {
    Weapon[] availableWeapons
    float accuracy
    float reactionTime
    float lastShotTime
}

function updateCombat(combat, agent, deltaTime):
    if agent.currentTarget == null:
        return

    # Select best weapon
    bestWeapon = selectBestWeaponForSituation(agent)
    if bestWeapon != agent.currentWeapon:
        agent.switchWeapon(bestWeapon)

    # Aim at target
    targetPosition = predictTargetPosition(agent.currentTarget)
    aimError = calculateAimError(agent, agent.currentTarget)
    finalAimPosition = targetPosition + aimError

    agent.aimAt(finalAimPosition)

    # Decide whether to shoot
    if shouldShoot(agent, agent.currentTarget):
        agent.currentWeapon.fire()
        combat.lastShotTime = currentTime()

function predictTargetPosition(target):
    # Predict where target will be based on velocity and projectile travel time
    if target.velocity == Vector3.zero:
        return target.position

    projectileSpeed = currentWeapon.projectileSpeed
    distance = distance(agent.position, target.position)
    travelTime = distance / projectileSpeed

    predictedPosition = target.position + target.velocity * travelTime
    return predictedPosition

function calculateAimError(agent, target):
    # Aim error based on:
    # - Agent skill
    # - Target movement speed
    # - Distance
    # - Agent taking damage
    # - Current weapon accuracy

    baseError = (1.0 - agent.profile.accuracy) * 0.5  # radians

    # Distance penalty
    distance = distance(agent.position, target.position)
    distanceError = distance * 0.001

    # Movement penalty
    movementError = length(target.velocity) * 0.01

    # Damage penalty (flinch)
    if agent.isTakingDamage():
        damageError = 0.1
    else:
        damageError = 0

    # Weapon accuracy
    weaponError = (1.0 - agent.currentWeapon.accuracy) * 0.2

    totalError = baseError + distanceError + movementError + damageError + weaponError

    # Convert to 3D vector
    return randomDirection() * totalError

// Navigation System
struct NavigationSystem {
    NavMesh navMesh
    Path currentPath
    int currentPathIndex
    Vector3 destination
    bool pathRecalculateNeeded
}

function updateNavigation(navigation, agent, deltaTime):
    if navigation.destination == null:
        return

    # Recalculate path if needed
    if navigation.pathRecalculateNeeded or navigation.currentPath == null:
        navigation.currentPath = findPath(agent.position, navigation.destination)
        navigation.currentPathIndex = 0
        navigation.pathRecalculateNeeded = false

    # Follow path
    if navigation.currentPath == null:
        return  # No path found

    if navigation.currentPathIndex >= length(navigation.currentPath):
        return  # Reached destination

    currentWaypoint = navigation.currentPath[navigation.currentPathIndex]

    # Check if we reached this waypoint
    if distance(agent.position, currentWaypoint) < 1.0:
        navigation.currentPathIndex++
        if navigation.currentPathIndex >= length(navigation.currentPath):
            return  # Reached destination
        currentWaypoint = navigation.currentPath[navigation.currentPathIndex]

    # Move toward waypoint
    direction = normalize(currentWaypoint - agent.position)
    agent.setMoveDirection(direction)

// Squad System
struct SquadSystem {
    Squad squad
    SquadRole role
}

function updateSquad(squadSystem, agent, deltaTime):
    if squadSystem.squad == null:
        return

    # Update squad coordination
    squadSystem.squad.update(agent, deltaTime)

    # Execute role-specific behavior
    switch squadSystem.role:
        case LEADER:
            executeLeaderBehavior(agent)
        case POINTMAN:
            executePointmanBehavior(agent)
        case SUPPORT:
            executeSupportBehavior(agent)
        case FLANKER:
            executeFlankerBehavior(agent)

struct Squad {
    FPSAgent[] members
    FPSAgent leader
    Vector3 objective
    SquadFormation formation
    map<string, any> sharedKnowledge
}

function updateSquad(squad, agent, deltaTime):
    # Share knowledge
    if agent.perception.hasNewInformation():
        squad.sharedKnowledge[agent.id] = agent.perception.getVisibleEnemies()

    # Update formation
    if squad.leader == agent:
        updateSquadFormation(squad)

    # Coordinate actions
    if squad.leader == agent:
        coordinateSquadActions(squad)
```

### 7.2 Minecraft-Specific FPS AI Adaptation

```pseudocode
// Minecraft-Specific Adaptation
struct MinecraftFPSAgent {
    // Inherits from FPSAgent
    // Minecraft-specific additions

    Block targetBlock
    Item[] hotbarItems
    int selectedHotbarSlot
    boolean isShieldActive
    int ticksSinceLastAttack
    int attackCooldown
}

function updateMinecraftAgent(agent, deltaTime):
    # Call base FPS update
    updateFPSAgent(agent, deltaTime)

    # Minecraft-specific updates
    updateShieldUsage(agent)
    updateAttackCooldown(agent)
    updateHotbarSelection(agent)

function updateShieldUsage(agent):
    # Activate shield when:
    # - Taking damage
    # - Enemy projectile incoming
    # - Advancing toward enemy

    if agent.isTakingDamage():
        agent.activateShield()
    else if agent.detectIncomingProjectile():
        agent.activateShield()
    else if agent.currentTarget != null:
        distance = distance(agent.position, agent.currentTarget.position)
        if distance < 8 and agent.isMovingToward(agent.currentTarget):
            agent.activateShield()

function updateAttackCooldown(agent):
    agent.ticksSinceLastAttack++

    if agent.ticksSinceLastAttack >= agent.attackCooldown:
        agent.canAttack = true
    else:
        agent.canAttack = false

function combatAttack(agent, target):
    if not agent.canAttack:
        return

    # Perform attack
    agent.performAttack(target)
    agent.ticksSinceLastAttack = 0

    # Reset cooldown based on weapon
    if agent.currentItem.type == SWORD:
        agent.attackCooldown = getAttackCooldown(agent.currentItem)
    else if agent.currentItem.type == BOW:
        agent.attackCooldown = 20  # Bow charge time
    else:
        agent.attackCooldown = 10  # Default

function getAttackCooldown(item):
    # Base cooldown modified by attributes
    base = 10
    cooldown = base

    # Material affects speed
    if item.material == WOOD:
        cooldown = 12
    else if item.material == STONE:
        cooldown = 10
    else if item.material == IRON:
        cooldown = 8
    else if item.material == DIAMOND:
        cooldown = 7
    else if item.material == NETHERITE:
        cooldown = 6

    # Enchantments
    if item.enchantments.has("sweeping_edge"):
        cooldown *= 1.1

    return cooldown
```

---

## 8. Case Studies

### 8.1 DOOM (1993) - Simplicity Through Design

**Key Innovation**: Monster infighting created emergent gameplay despite simple AI.

**Lessons for Minecraft**:
- Simple state machines can create complex behavior through environmental interaction
- Agent vs agent combat (as in DOOM infighting) can be implemented in Minecraft
- Clear, orthogonal behaviors make AI more predictable and exploitable by players

### 8.2 Half-Life (1998) - Squad Coordination

**Key Innovation**: First FPS to feature coordinated squad tactics with information sharing.

**Lessons for Minecraft**:
- Multi-agent systems need communication channels
- Shared knowledge (blackboard pattern) enables coordinated behavior
- Role-based AI (leader, support, flanker) scales to multiple agents

### 8.3 F.E.A.R. (2005) - GOAP Revolution

**Key Innovation**: Goal-Oriented Action Planning enabled emergent, unpredictable AI behavior from simple atomic actions.

**Lessons for Minecraft**:
- Planning systems can create intelligent behavior without hard-coded behaviors
- STRIPS-style planning (preconditions/effects) is well-suited for Minecraft's block-based world
- AI can "reason from first principles" about how to achieve goals

### 8.4 Quake III Arena (1999) - Bot Mastery

**Key Innovation**: Sophisticated bot AI without machine learning, using FSMs, item prediction, and tactical goals.

**Lessons for Minecraft**:
- Item timing prediction (armor, weapons) applies to Minecraft resources
- Weapon preference tables translate to Minecraft tool selection
- Different game modes require different AI priorities (deathmatch vs CTF)

### 8.5 Left 4 Dead (2008) - Director AI

**Key Innovation**: AI Director dynamically adjusted difficulty based on player performance and stress levels.

**Lessons for Minecraft**:
- Procedural difficulty adjustment keeps players in "flow state"
- Monitoring player metrics (health, resources, progress) enables dynamic challenge scaling
- Resource placement can be algorithmically controlled based on player state

---

## 9. Sources

### Academic & Technical Papers

- Orkin, J. (2003). "Applying Goal-Oriented Action Planning to Games" - AI Game Programming Wisdom 2
- Orkin, J. (2006). "AI Architecture: GOAP Planning in F.E.A.R." - GDC Presentation

### Online Resources

#### FPS AI General

- [Game AI Programming Wisdom Series - Various Authors](https://www.cnblogs.com/sols/articles/8456727.html)
- [GAMES104: Game Engine AI Systems](https://m.blog.csdn.net/yx314636922/article/details/142365525)
- [FPS Game AI Development - Unreal Engine](https://www.unrealengine.com/en-US/ai)

#### DOOM (1993)

- [Doom Wiki - Monster AI](https://doomwiki.org/wiki/Monster_behavior)
- [DOOM Infighting Mechanics](https://doomwiki.org/wiki/Infighting)

#### Half-Life (1998)

- [Combine OverWiki - HECU Marine AI](https://combineoverwiki.net/wiki/HECU_Marine)
- [TV Tropes - Half-Life AI Description](https://tvtropes.org/pmwiki/pmwiki.php/VideoGame/HalfLife)

#### F.E.A.R. (2005) & GOAP

- [GOAP目标导向型AI在F.E.A.R中的应用](https://www.cnblogs.com/blakehuangdong/p/11339941.html)
- [游戏AI——GOAP技术要点](https://m.blog.csdn.net/qw_6918966011/article/details/131752868)
- [游戏AI新境界：GOAP技术全解析](https://m.blog.csdn.net/qq_33060405/article/details/148981402)
- [GOAP 目标导向型行为计划 AI 算法](https://www.pianshen.com/article/80871131663/)
- [GameReadyGoap GitHub Repository](https://github.com/Joy-less/GameReadyGoap)

#### Counter-Strike Bots

- [Counter-Strike Wiki - Bot Overview](https://counterstrike.fandom.com/wiki/Bot)
- [PODBot GitHub Repository](https://github.com/APGRoboCop/podbot_mm)

#### Quake III Arena

- [Quake III Arena Source Code Analysis](https://gitcode.com/gh_mirrors/qu/Quake-III-Arena)
- [DeepMind Lab Environment](https://github.com/deepmind/lab)

#### Navigation & Pathfinding

- [Recast & Detour Navigation Mesh](https://m.blog.csdn.net/weixin_43679037/article/details/125774963)
- [Navigation Mesh Inner Workings - Unity](https://docs.unity.cn/cn/560/Manual/nav-InnerWorkings.html)
- [A* Algorithm in Game Pathfinding](https://www.cnblogs.com/sols/articles/8456727.html)

#### Behavior Trees

- [虚幻引擎人工智能实用指南](https://www.cnblogs.com/apachecn/p/19167931)
- [虚幻-5-人工智能-全](https://www.cnblogs.com/apachecn/p/19167929)
- [行为树 UE实现](https://wenku.csdn.net/answer/oxahiw8fb8)

#### AI Comparisons

- [常见的游戏AI技术对比](https://www.cnblogs.com/jeason1997/articles/9499051.html)
- [游戏人工智能开发之6种决策方法](https://m.blog.csdn.net/u012419410/article/details/50680817)
- [游戏AI智能体行为设计全攻略](https://m.blog.csdn.net/simsolve/article/details/156052295)

#### Tactical Shooters

- [Rainbow Six Vegas Features](https://www.ali213.net/ubisoft/html/42.html)
- [Arma 3 AI Morale Mods](https://game.3loumao.org/992878167)
- [Left 4 Dead AI Director](https://developer.valvesoftware.com/wiki/Left_4_Dead)

#### Minecraft AI Mods

- [Angry Mobs Mod](https://modrinth.com/mod/angry-mobs)
- [Special AI Mod](https://www.mcmod.cn/class/4280.html)
- [Custom Mob Targets](https://m.mcmod.cn/class/23395.html)

#### Utility AI

- [游戏AI行为选择算法一览](https://blog.csdn.net/m0_55958664/article/details/128113650)
- [Utility AI in Game Development](https://www.gamedeveloper.com/programming/utility-ai-for-games)

#### BSP & Visibility

- [Quake III Arena BSP Tree Optimization](https://m.blog.csdn.net/gitblog_00944/article/details/154328788)
- [L4D Level Design Visibility](https://developer.valvesoftware.com/wiki/Zh/L4D_Level_Design/Visibility)
- [Deep Dive into PVS System](https://wenku.csdn.net/doc/37p9axd527)

---

## Conclusion

Chapter 2 has demonstrated that FPS games have developed sophisticated AI techniques over three decades without requiring Large Language Models or machine learning. The key takeaways for Minecraft autonomous agent development are:

1. **State Machines** remain the foundation of game AI, scalable through hierarchical design
2. **Goal-Oriented Action Planning (GOAP)** enables emergent, intelligent behavior from atomic actions
3. **Utility AI** provides flexible, maintainable decision-making for complex scenarios
4. **Behavior Trees** offer modular, hierarchical behavior composition
5. **Squad Coordination** through shared knowledge enables multi-agent tactics
6. **Cover Systems** translate directly to Minecraft's block-based environment
7. **Threat Assessment** algorithms enable intelligent target prioritization
8. **Weapon/Tool Selection** logic from FPS applies to Minecraft's item system

These techniques, proven across decades of FPS development, provide a robust foundation for building intelligent autonomous agents in Minecraft without the complexity, cost, or latency of LLM-based approaches.

---

**Next Chapter:** Chapter 3 will examine Real-Time Strategy (RTS) games and their macro/micro AI patterns.
