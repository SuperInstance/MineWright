# Behavior Tree Evolution: From Game Development to LLM Integration

**Research Document**
**Date:** 2026-02-28
**Author:** Orchestrator Research Agent
**Status:** Comprehensive Analysis

---

## Table of Contents

1. [Behavior Tree Origins](#1-behavior-tree-origins)
2. [Behavior Tree vs FSM](#2-behavior-tree-vs-finite-state-machines)
3. [Behavior Tree Node Types](#3-behavior-tree-node-types)
4. [Behavior Tree Patterns](#4-behavior-tree-patterns)
5. [LLM Integration](#5-llm-integration)
6. [Implementing BT in Steve AI](#6-implementing-bt-in-steve-ai)
7. [References](#7-references)

---

## 1. Behavior Tree Origins

### 1.1 The Halo 2 Revolution (2004)

**Halo 2**, released by Bungie and Microsoft on November 9, 2004, is widely recognized as **the first mainstream game to implement behavior trees** for AI decision-making. This revolutionary approach transformed game AI development.

#### Key Innovations in Halo 2

| Innovation | Description |
|------------|-------------|
| **Hierarchical Modularity** | Behaviors organized as tree structures instead of flat state transitions |
| **Visual Clarity** | Could be designed and debugged using visual editors |
| **Reactive Execution** | AI could respond to changing conditions in real-time |
| **Scalable Complexity** | Complex behaviors built from simple, reusable nodes |

#### Industry Impact

After Microsoft/Bungie shared details about Halo 2's behavior tree implementation at GDC 2005 (Damian Isla's presentation "Handling Complexity in the Halo 2 AI"), the technique spread rapidly throughout the game industry:

- **BioShock** - Complex enemy AI with emergent behaviors
- **Spore** - Creature behavior with adaptive personality systems
- **Crysis** - Tactical squad AI with dynamic responses
- **Red Dead Redemption** - Wildlife ecosystem simulation
- **Uncharted/God of War** - Cinematic enemy encounters

### 1.2 Why Behavior Trees Replaced FSMs

#### Finite State Machine (FSM) Problems

| Problem | Description | Impact |
|---------|-------------|--------|
| **Exponential Complexity** | State transitions grow O(n²) with state count | Unmaintainable at scale |
| **High Coupling** | States tightly coupled to transition logic | Changes cascade across system |
| **Poor Scalability** | Adding behaviors requires state explosion | Rapid development impossible |
| **Designer Unfriendly** | Requires programming knowledge | Designers cannot iterate |
| **Fragile Logic** | "Very fragile when logic grows" | Bugs increase exponentially |

#### Behavior Tree Advantages

| Advantage | Description | Example |
|-----------|-------------|---------|
| **Modular & Low Coupling** | Each node is independent | Change attack logic without affecting patrol |
| **Hierarchical Composition** | Complex behaviors from simple nodes | "Combat" sequence built from "aim", "fire", "reload" |
| **High Reusability** | Nodes used across multiple trees | "IsEnemyVisible" used in patrol, chase, attack |
| **Visual Editing** | Tree structure visually represented | Designers can drag-and-drop behaviors |
| **Reactive by Default** | Tree re-evaluated each tick | AI immediately responds to new threats |

### 1.3 The Architecture Shift

```
FSM Architecture (Pre-2004):
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Idle      │◄──►│   Patrol    │◄──►│   Combat    │
│             │    │             │    │             │
│ [Transitions]    [Transitions]    [Transitions]
│  everywhere      │  everywhere      │  everywhere
└─────────────┘    └─────────────┘    └─────────────┘
      ▲                  │                  ▲
      └──────────────────┴──────────────────┘
            Complex state transition matrix

Behavior Tree Architecture (Post-2004):
                    ┌──────────────────┐
                    │   Root Selector  │
                    └────────┬─────────┘
                             │
            ┌────────────────┼────────────────┐
            ▼                ▼                ▼
    ┌───────────────┐ ┌───────────────┐ ┌───────────────┐
    │   Patrol      │ │   Combat      │ │   Flee        │
    │   Sequence    │ │   Sequence    │ │   Sequence    │
    └───────┬───────┘ └───────┬───────┘ └───────┬───────┘
            │                 │                 │
      ┌─────┴─────┐     ┌─────┴─────┐     ┌─────┴─────┐
      ▼           ▼     ▼           ▼     ▼           ▼
  [MovePath]  [Look] [Chase] [Attack] [Run] [Hide]

Each node: Independent, reusable, testable
```

---

## 2. Behavior Tree vs Finite State Machines

### 2.1 Conceptual Comparison

| Aspect | Finite State Machine | Behavior Tree |
|--------|---------------------|---------------|
| **Structure** | Graph of states with transitions | Hierarchical tree of nodes |
| **Decision Logic** | Embedded in transitions | Encapsulated in control nodes |
| **Reusability** | Low - states tightly coupled | High - nodes are independent |
| **Scalability** | Poor - O(n²) transitions | Good - O(log n) depth |
| **Debugging** | Hard - trace through state maze | Easy - follow tree path |
| **Designer Access** | Requires programming | Visual editing possible |
| **Reactive Behavior** | Manual implementation | Built-in via tree re-evaluation |

### 2.2 Example: Enemy AI Comparison

#### FSM Implementation

```python
# FSM - Complex state transition logic
class EnemyFSM:
    def __init__(self):
        self.state = "IDLE"

    def update(self):
        if self.state == "IDLE":
            if self.sees_player():
                self.state = "CHASE"
            elif self.hears_noise():
                self.state = "INVESTIGATE"
            elif self.patrol_point():
                self.state = "PATROL"

        elif self.state == "CHASE":
            if self.sees_player():
                if self.in_attack_range():
                    self.state = "ATTACK"
                else:
                    self.move_to_player()
            else:
                if self.last_seen_timeout():
                    self.state = "INVESTIGATE"
                else:
                    self.state = "IDLE"

        elif self.state == "ATTACK":
            if self.sees_player():
                if self.in_attack_range():
                    self.attack()
                else:
                    self.state = "CHASE"
            else:
                self.state = "INVESTIGATE"

        elif self.state == "INVESTIGATE":
            if self.sees_player():
                self.state = "CHASE"
            elif self.investigation_complete():
                self.state = "IDLE"
            elif self.patrol_point():
                self.state = "PATROL"

        elif self.state == "PATROL":
            if self.sees_player():
                self.state = "CHASE"
            elif self.hears_noise():
                self.state = "INVESTIGATE"
            elif self.patrol_complete():
                self.state = "IDLE"
```

**Problems:** 50+ lines of nested conditionals, hard to modify, duplicated logic, difficult to extend.

#### Behavior Tree Implementation

```python
# Behavior Tree - Clear, modular structure
enemy_behavior = Selector([
    # Combat branch - highest priority
    Sequence([
        Condition("sees_player"),
        Selector([
            Sequence([
                Condition("in_attack_range"),
                Action("attack")
            ]),
            Action("chase_player")
        ])
    ]),

    # Investigation branch - medium priority
    Sequence([
        Condition("hears_noise"),
        Action("investigate_noise")
    ]),

    # Patrol branch - lowest priority
    Sequence([
        Condition("has_patrol_route"),
        Action("patrol")
    ]),

    # Default idle
    Action("idle")
])
```

**Advantages:** Clear priority hierarchy, each behavior independent, easy to add/modify, visual representation possible.

### 2.3 When to Use Each

| Use Case | Recommended Approach | Reasoning |
|----------|---------------------|-----------|
| Simple toggle behavior | FSM | Overhead of BT not justified |
| Single entity with <5 states | FSM | Simplicity wins |
| Complex AI with many behaviors | BT | Scalability essential |
| Designer-driven AI | BT | Visual editing required |
| Highly reactive AI | BT | Built-in re-evaluation |
| Scripted sequences | BT | Sequential composition natural |

---

## 3. Behavior Tree Node Types

### 3.1 Node Type Overview

```
Behavior Tree Node Hierarchy:

                    Node (Abstract)
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
   Composite     Decorator      Leaf Node
        │              │              │
   ┌────┴────┐         │      ┌───────┴───────┐
   ▼         ▼         ▼      ▼               ▼
Sequence  Selector   Inverter  Condition    Action
Parallel    Fallback  Repeater
(etc.)     (etc.)     Cooldown
                    (etc.)
```

### 3.2 Composite Nodes (Control Flow)

Composite nodes have multiple children and control how they execute.

#### Sequence Node (AND)

Executes children left-to-right. Returns **SUCCESS** only if **all** children succeed. Returns **FAILURE** immediately when any child fails.

```
[Sequence: BuildHouse]
    │
    ├──> [Action: GatherWood] ✓
    │        Returns: SUCCESS
    │
    ├──> [Action: GatherStone] ✓
    │        Returns: SUCCESS
    │
    ├──> [Action: CraftPlanks] ✓
    │        Returns: SUCCESS
    │
    └──> [Action: PlaceBlocks] ✓
             Returns: SUCCESS

Result: SUCCESS (all children succeeded)
```

**Use Cases:**
- Multi-step procedures (crafting recipes)
- Prerequisite checks (have resources → craft item)
- Sequential actions (open door → enter room → close door)

#### Selector Node (OR / Fallback)

Executes children left-to-right. Returns **SUCCESS** when **any** child succeeds. Returns **FAILURE** only if **all** children fail.

```
[Selector: OpenDoor]
    │
    ├──> [Condition: IsDoorOpen] ✗
    │        Returns: FAILURE
    │
    ├──> [Action: UseKey] ✓
    │        Returns: SUCCESS
    │
    └──> [Action: BreakDoor] (not executed)

Result: SUCCESS (key worked)
```

**Use Cases:**
- Fallback strategies (try A, if fails try B)
- Priority-based decision (attack > flee > patrol)
- Alternative solutions (key > lockpick > break)

#### Parallel Node

Executes **all** children simultaneously. Returns based on policy:

| Policy | Description |
|--------|-------------|
| **Parallel Selector** | Succeeds if ONE child succeeds (OR) |
| **Parallel Sequence** | Succeeds if ALL children succeed (AND) |
| **Parallel Hybrid** | Succeeds if N children succeed |

```
[ParallelSequence: GroupAttack]
    │
    ├──> [Action: Soldier1: Attack] ─┐
    │                                │
    ├──> [Action: Soldier2: Attack] ─┼──► Execute simultaneously
    │                                │
    └──> [Action: Soldier3: Attack] ─┘

Result: Wait for ALL to complete, then return SUCCESS/FAILURE
```

**Use Cases:**
- Squad coordination (all units attack together)
- Multi-tasking (move AND scan for enemies)
- Synchronization (wait for multiple conditions)

### 3.3 Decorator Nodes (Modifiers)

Decorator nodes modify the behavior of their single child.

#### Inverter

Inverts the child's return value:
- SUCCESS → FAILURE
- FAILURE → SUCCESS
- RUNNING → RUNNING

```
[Inverter]
    │
    └──> [Condition: IsEnemyVisible]
         If enemy NOT visible: SUCCESS (inverted)
```

**Use Cases:**
- Negative conditions ("while NOT at destination")
- Guard clauses ("if NOT in danger, patrol")

#### Repeater

Repeats child execution N times or indefinitely.

```
[Repeater: 3 times]
    │
    └──> [Action: FireWeapon]
         Executes 3 times
```

**Use Cases:**
- Burst fire (shoot 3 times)
- Retry loops (try N times before giving up)
- Continuous monitoring (repeat forever)

#### RepeatUntilSuccess/Failure

Repeats child until it returns the desired status.

```
[RepeatUntilSuccess]
    │
    └──> [Action: PickLock]
         Keep trying until lock opens
```

#### Cooldown

Prevents re-execution within specified time window.

```
[Cooldown: 5 seconds]
    │
    └──> [Action: UseSpecialAbility]
         Can only use once every 5 seconds
```

**Use Cases:**
- Rate limiting (abilities, attacks)
- Resource throttling (prevent spam)
- Game mechanics (cooldown periods)

#### Timeout

Forces FAILURE if child takes too long.

```
[Timeout: 10 seconds]
    │
    └──> [Action: HackComputer]
         Must complete within 10 seconds
```

**Use Cases:**
- Time-critical actions (breach door before alarm)
- Preventing hangs (stuck behaviors)
- Game mechanics (time limits)

#### ForceSuccess/ForceFailure

Always returns specified value, ignoring child result.

```
[ForceSuccess]
    │
    └──> [Action: PlayDeathAnimation]
         Returns SUCCESS even if animation glitches
```

**Use Cases:**
- Error tolerance (don't fail on cosmetic issues)
- Required actions (always complete, even if failed)
- Cleanup operations (run regardless of outcome)

### 3.4 Leaf Nodes

#### Condition Nodes

Check a condition and return SUCCESS/FAILURE. No side effects.

```python
class IsEnemyVisible(ConditionNode):
    def tick(self):
        enemy = self.blackboard.get('target_enemy')
        if enemy and self.can_see(enemy):
            return Status.SUCCESS
        return Status.FAILURE
```

**Common Conditions:**
- `IsThreatened` - enemy nearby
- `HasAmmo` - check ammunition
- `IsHealthy` - health above threshold
- `AtDestination` - position reached
- `CanAfford` - sufficient resources

#### Action Nodes

Execute a behavior and return SUCCESS/FAILURE/RUNNING.

```python
class MoveToPosition(ActionNode):
    def tick(self):
        target = self.blackboard.get('move_target')

        if self.at_position(target):
            return Status.SUCCESS

        self.move_toward(target)
        return Status.RUNNING  # Still moving
```

**Common Actions:**
- `MoveTo` - pathfinding and movement
- `Attack` - execute attack
- `Interact` - use object
- `Wait` - delay for time
- `PlayAnimation` - trigger animation

### 3.5 Node Return Values

| Status | Meaning | Tree Behavior |
|--------|---------|---------------|
| **SUCCESS** | Node completed successfully | Parent node continues/returns |
| **FAILURE** | Node failed | Parent node tries next child or returns failure |
| **RUNNING** | Node still executing | Tree pauses, resumes next tick |

---

## 4. Behavior Tree Patterns

### 4.1 Fallback Patterns

#### Simple Fallback

Try alternatives in order until one works.

```
[Selector: GetFood]
    ├──> [Sequence: EatInventory]
    │    ├──> [Condition: HasFoodInInventory]
    │    └──> [Action: Eat]
    │
    ├──> [Sequence: FindAndEat]
    │    ├──> [Action: FindNearbyFood]
    │    └──> [Action: MoveToAndEat]
    │
    └──> [Sequence: HuntAndCook]
         ├──> [Action: FindPrey]
         ├──> [Action: Hunt]
         └──> [Action: CookAndEat]
```

#### Reactive Fallback

Restart from first child on each tick. Allows immediate response to changing conditions.

```
[ReactiveSelector: CombatResponse]
    ├──> [Sequence: Flee]        # If critical damage
    │    ├──> [Condition: Health<20%]
    │    └──> [Action: RunAway]
    │
    ├──> [Sequence: Heal]        # If have healing
    │    ├──> [Condition: HasHealthPotion]
    │    └──> [Action: UsePotion]
    │
    └──> [Sequence: Fight]       # Default
         ├──> [Action: Attack]
```

**Reactivity:** Each tick re-evaluates health, may switch from Fight → Flee immediately.

### 4.2 Retry Patterns

#### Fixed Retry

```
[RetryDecorator: maxAttempts=3]
    │
    └──> [Action: PickLock]
         Try up to 3 times before failing
```

#### Exponential Backoff

```
[ExponentialBackoffRetry: baseDelay=1s, maxAttempts=5]
    │
    └──> [Action: ConnectToServer]
         Delays: 1s, 2s, 4s, 8s, 16s
```

**Best Practice:** Store retry count in Blackboard for observability.

```python
class ExponentialBackoffRetry(DecoratorNode):
    def tick(self):
        attempt = self.blackboard.get(f"{self.id}_attempt", 0)

        if attempt >= self.max_attempts:
            return Status.FAILURE

        if attempt > 0:
            delay = min(self.base_delay * (2 ** attempt), self.max_delay)
            if self.elapsed_time < delay:
                return Status.RUNNING

        result = self.child.tick()

        if result == Status.SUCCESS:
            self.blackboard.set(f"{self.id}_attempt", 0)
            return Status.SUCCESS

        self.blackboard.set(f"{self.id}_attempt", attempt + 1)
        return Status.RUNNING
```

### 4.3 Cooldown Patterns

#### Action Cooldown

```
[Cooldown: 5.0 seconds]
    │
    └──> [Action: UseSpecialAttack]
         Can only use once every 5 seconds
```

#### Cooldown with Failure Cache

```
[FailureCooldown: cooldownTime=30s]
    │
    └──> [Action: ExpensiveOperation]
         After failure, won't retry for 30 seconds
```

**Use Case:** Prevent repeated attempts at expensive failed operations (e.g., breaking blocks).

### 4.4 Priority Selectors

Children evaluated left-to-right, first success wins. Implicit priority ordering.

```
[Selector: CombatPriority]
    │
    ├──> [Sequence: Execute]        # Priority 1: Finish enemy
    │    ├──> [Condition: CanExecute]
    │    └──> [Action: PerformExecution]
    │
    ├──> [Sequence: HeavyAttack]    # Priority 2: Big damage
    │    ├──> [Condition: HasHeavyWeapon]
    │    └──> [Action: HeavyAttack]
    │
    ├──> [Sequence: LightAttack]    # Priority 3: Quick attack
    │    └──> [Action: LightAttack]
    │
    └──> [Sequence: Block]          # Priority 4: Defensive
         └──> [Action: RaiseShield]
```

### 4.5 Context-Sensitive Behaviors

#### Blackboard Pattern

Central shared memory for context data.

```python
class Blackboard:
    def __init__(self):
        self.read_only = {}      # System constants (max_health, speed)
        self.command = {}        # Request parameters (target, destination)
        self.cache = {}          # Temporary state (last_seen_time, path)
        self.diagnostic = {}     # Errors and statistics
```

**Blackboard Zones:**

| Zone | Purpose | Lifetime | Examples |
|------|---------|----------|----------|
| **Read-Only** | System constants | Entire session | max_health, attack_damage |
| **Command** | Request parameters | Current task | target_entity, build_position |
| **Cache** | Temporary state | Until invalidation | last_known_pos, computed_path |
| **Diagnostic** | Debugging info | Entire session | error_count, node_execution_time |

#### Entry/Exit Hooks

Setup and cleanup for context-aware behavior.

```python
class EntryExitDecorator(DecoratorNode):
    def __init__(self, child, entry_action=None, exit_action=None):
        super().__init__(child)
        self.entry_action = entry_action
        self.exit_action = exit_action

    def tick(self):
        # Entry hook (first time only)
        if not self.entered:
            if self.entry_action:
                self.entry_action.execute(self.blackboard)
            self.entered = True

        result = self.child.tick()

        # Exit hook (when complete)
        if result != Status.RUNNING:
            if self.exit_action:
                self.exit_action.execute(self.blackboard)
            self.entered = False

        return result
```

**Use Cases:**
- Resource allocation (acquire on entry, release on exit)
- State management (set flags, restore after)
- Animation control (start animation, clean up when done)

#### Reactive Sequences

Restart from beginning when conditions change.

```
[ReactiveSequence: PatrolRoute]
    ├──> [Action: MoveToCheckpoint1]
    ├──> [Action: MoveToCheckpoint2]
    └──> [Action: MoveToCheckpoint3]

# If enemy spotted at checkpoint2, restart patrol after combat
```

### 4.6 Subtree Reuse

Reference entire behavior trees as nodes.

```
[Selector: EnemyAI]
    ├──> [Subtree: CombatBehavior]      # Reusable combat tree
    ├──> [Subtree: PatrolBehavior]      # Reusable patrol tree
    └──> [Subtree: IdleBehavior]        # Reusable idle tree
```

**Benefits:**
- Template behaviors shared across enemy types
- Hierarchical organization
- Easier testing and debugging

---

## 5. LLM Integration

### 5.1 LLM-Generated Behavior Trees

#### LLM-BRAIn (2023)

**"LLM-driven Fast Generation of Robot Behaviour Tree based on Large Language Model"** (arXiv:2305.19352)

Fine-tuned Stanford Alpaca 7B on 8.5k instruction-following demonstrations to generate robot behavior trees from text descriptions.

**Key Results:**
- Runs on onboard robot microcomputers
- Produces structurally correct BTs
- Human evaluators couldn't reliably distinguish LLM-generated from human-created BTs (only 4.53/10 correct identification)

**Example Input/Output:**

```
Input: "Navigate to the kitchen, pick up the red cup, and bring it to the living room"

LLM Output:
└──> Sequence
    ├──> Action: Navigate(location="kitchen")
    ├──> Action: PickUp(object="red cup")
    └──> Sequence
        ├──> Action: Navigate(location="living room")
        └──> Action: PlaceObject()
```

#### VLM-Driven Behavior Trees (2025)

**"VLM-driven Behavior Tree for Context-aware Task Planning"** (arXiv:2501.03968)

Integrates Vision-Language Models (VLMs) to generate behavior trees with visual condition nodes.

**Key Innovation:**
- Conditions expressed as free-form text
- Real-time evaluation against images during execution
- Validated in café scenario with real robots

**Example:**

```
Input: "If the table is empty, clean it. If someone is sitting, ask them to move."

Generated BT:
└──> Selector
    ├──> Sequence
    │    ├──> VisualCondition("table is empty")
    │    └──> Action("CleanTable")
    └──> Sequence
         ├──> VisualCondition("person sitting at table")
         └──> Action("AskPersonToMove")
```

### 5.2 Natural Language to BT Compilation

#### LLM-as-BT-Planner (ICRA 2025)

**"Leveraging LLMs for Behavior Tree Generation in Robot Task Planning"**

Framework for converting natural language commands into executable behavior trees:

```
Natural Language: "Build a small house near the lake"
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│              LLM Planner                             │
│  - Extracts intent: build house                     │
│  - Identifies constraints: near lake, small         │
│  - Decomposes into primitive actions                │
└─────────────────────────────────────────────────────┘
                    │
                    ▼
Generated Behavior Tree:
└──> Sequence
    ├──> Selector
    │    ├──> Condition: AtSuitableLocation(near="lake", size="small")
    │    └──> Sequence
    │         ├──> Action: FindLocation(near="lake", size="small")
    │         └──> Action: MoveTo(location)
    ├──> Action: GatherMaterials(type="wood", quantity=64)
    ├──> Action: BuildStructure(type="house", size="small")
    └──> Action: VerifyConstruction()
```

#### Compilation Pipeline

```
1. Intent Analysis
   ├─ Extract: verbs (actions), nouns (objects), modifiers (constraints)
   └─ Semantic: understand goal, preconditions, postconditions

2. Primitive Mapping
   ├─ Map verbs to action nodes: "build" → BuildStructureAction
   ├─ Map nouns to parameters: "house" → type="house"
   └─ Map modifiers to conditions: "small" → size="small"

3. Structure Synthesis
   ├─ Determine control flow: sequential steps vs alternatives
   ├─ Insert conditions: preconditions, invariants
   └─ Add error handling: retries, fallbacks

4. Tree Generation
   └─ Output executable behavior tree code

5. Validation
   ├─ Syntax check: valid tree structure
   ├─ Semantic check: valid action parameters
   └─ Runtime check: executable on target system
```

### 5.3 Dynamic Node Creation

#### Hierarchical LLM Agent Trees (ReAcTree, 2025)

**"ReAcTree: Hierarchical LLM Agent Trees with Control Flow"** (arXiv:2511.02424)

Combines LLM agents with behavior tree structures through recursive sub-agent spawning.

**Architecture:**

```
┌─────────────────────────────────────────────────────────┐
│                    Root Agent                           │
│  Task: "Organize a defense against the incoming raid"   │
└─────────────────────┬───────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ Sub-Agent 1  │ │ Sub-Agent 2  │ │ Sub-Agent 3  │
│ Build Walls  │ │ Equip Squad  │ │ Set Traps    │
└──────┬───────┘ └──────┬───────┘ └──────┬───────┘
       │                │                │
       ▼                ▼                ▼
  [Spawn more]    [Spawn more]    [Spawn more]
   sub-agents      sub-agents      sub-agents
```

**Key Features:**
- Recursive decomposition of complex tasks
- Each sub-agent has its own behavior tree
- Control flow structures (sequence, selector) guide execution
- Self-prompting for sub-agent creation

#### Dynamic BT Adaptation

Real-time behavior tree modification based on execution feedback:

```
Initial BT:
└──> Sequence: GatherResources
    ├──> Action: MineIron
    ├──> Action: MineCoal
    └──> Action: CraftTorches

# Execution fails: coal depleted nearby

# LLM analyzes failure and generates modification:
└──> Selector: GatherResources
    ├──> Sequence: OriginalPlan
    │    ├──> Action: MineIron
    │    ├──> Action: MineCoal
    │    └──> Action: CraftTorches
    └──> Sequence: AlternativePlan    # ← NEW
         ├──> Action: MineIron
         ├──> Action: TravelToBiome(type="swamp")  # ← NEW
         ├──> Action: MineCoal
         └──> Action: CraftTorches
```

**Research:** BETR-XP-LLM and LLM-BT frameworks enable real-time BT adaptation with minimal human intervention.

### 5.4 LLM as Meta-Controller

#### Architecture Pattern

```
┌────────────────────────────────────────────────────────┐
│                   LLM Meta-Controller                  │
│  - Monitors BT execution                               │
│  - Detects failures, inefficiencies                    │
│  - Generates BT modifications                          │
│  - Explains reasoning                                  │
└────────────────────┬───────────────────────────────────┘
                     │
                     │ commands
                     ▼
┌────────────────────────────────────────────────────────┐
│              Behavior Tree Engine                       │
│  - Executes BT tick-by-tick                            │
│  - Reports status to LLM                               │
│  - Applies LLM-generated modifications                 │
└────────────────────┬───────────────────────────────────┘
                     │
                     │ actions
                     ▼
┌────────────────────────────────────────────────────────┐
│                   Game World                           │
│  - Steve entity                                        │
│  - Minecraft environment                               │
└────────────────────────────────────────────────────────┘
```

#### Feedback Loop

```
1. Execute BT tick
2. Collect execution data:
   - Which nodes ran?
   - What did they return?
   - How long did they take?
   - What blackboard data was used?
3. Send execution trace to LLM
4. LLM analyzes:
   - Is behavior as expected?
   - Are there failures?
   - Can efficiency be improved?
5. LLM generates:
   - BT modifications (if needed)
   - Diagnostic messages
   - Suggestions for next actions
6. Apply modifications and continue
```

#### Example: Failed Mining Task

```
Execution Trace:
└──> Sequence: GatherIronOre
    ├──> Condition: HasPickaxe ✓ SUCCESS
    ├──> Action: FindIronOre ✗ FAILURE (no iron found nearby)
    └──> Action: MineIron (not executed)

LLM Analysis:
"Failed to find iron ore nearby. Suggest:
1. Expand search radius
2. Check known iron locations in memory
3. Descend to lower y-levels
4. Ask other agents for iron location"

LLM-Generated Modification:
└──> Selector: GatherIronOre
    ├──> Sequence: SearchNearby
    │    ├──> Condition: HasPickaxe
    │    ├──> Action: FindIronOre(radius=64)
    │    └──> Action: MineIron
    ├──> Sequence: SearchMemory
    │    ├──> Action: QueryMemory(resource="iron_ore")
    │    ├──> Action: NavigateTo(location)
    │    └──> Action: MineIron
    └──> Sequence: RequestFromAgent
         ├──> Action: BroadcastRequest(resource="iron_ore")
         └──> Action: NavigateTo(provided_location)
```

### 5.5 Memory and Learning

#### DiaGBT (Dynamic Adaptive Generation of BTs)

Uses LLMs to define mapping rules for constructing complex BTs with memory for reusable skills.

**Memory Structure:**

```python
class SkillMemory:
    def __init__(self):
        self.atomic_skills = {}      # Primitive actions
        self.compound_skills = {}    # BT subtrees
        self.success_patterns = {}   # What worked
        self.failure_patterns = {}   # What didn't
```

**Learning Process:**

```
1. Execute task with generated BT
2. Record success/failure patterns
3. If successful subtree, store as reusable skill:
   compound_skills["build_small_house"] = bt_subtree
4. For future tasks, LLM can reference reusable skills:
   "Use the build_small_house skill, but add a basement"
```

**Example Skill Reuse:**

```
First Request: "Build a small wooden house"
Generated BT: [100 nodes, built from primitives]
Execution: SUCCESS

Stored: compound_skills["small_wooden_house"] = <BT subtree>

Second Request: "Build a small wooden house with a basement"
LLM retrieves: small_wooden_house subtree
LLM modifies: Add basement excavation before building
Generated BT: [110 nodes = 100 reusable + 10 new]
Time: 3x faster (reuse existing structure)
```

---

## 6. Implementing BT in Steve AI

### 6.1 Architecture Integration

```
Current Steve AI Architecture:
┌─────────────────────────────────────────────────────────┐
│                      GUI Layer                          │
│  User presses K → types command → "Build a house"       │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                    TaskPlanner                          │
│  Sends LLM request with context                         │
│  ResponseParser extracts structured tasks               │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                  ActionExecutor                         │
│  Executes tasks tick-by-tick (non-blocking)             │
│  Task queue, collaborative building                     │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              SteveMemory + State Machine                │
│  AgentStateMachine: IDLE, PLANNING, EXECUTING, etc.     │
└─────────────────────────────────────────────────────────┘

Proposed BT-Enhanced Architecture:
┌─────────────────────────────────────────────────────────┐
│                      GUI Layer                          │
│  User presses K → types command → "Build a house"       │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              LLM Behavior Tree Generator                │
│  NEW: Converts command → behavior tree                  │
│  - Static tree for known commands                       │
│  - Dynamic tree for novel commands                      │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│            Behavior Tree Engine                         │
│  NEW: Tick-based BT execution                           │
│  - Interprets tree structure                            │
│  - Manages node lifecycle                               │
│  - Blackboard for context sharing                       │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│               Existing Action System                    │
│  Actions remain as leaf nodes in BT                     │
│  - BaseAction → ActionNode adapter                      │
│  - Plugin architecture unchanged                        │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              SteveMemory + State Machine                │
│  Enhanced: BT execution state                           │
│  - Current node, running nodes                          │
│  - Blackboard integration                               │
└─────────────────────────────────────────────────────────┘
```

### 6.2 Node Implementation

#### Base Node Interface

```java
package com.steve.ai.bt;

import com.steve.entity.SteveEntity;

/**
 * Base interface for all behavior tree nodes
 */
public interface BTNode {
    /**
     * Execute node logic for one tick
     * @param steve The Steve entity
     * @param context Shared blackboard context
     * @return Node status
     */
    NodeStatus tick(SteveEntity steve, Blackboard context);

    /**
     * Reset node to initial state
     */
    void reset();

    /**
     * Get node name for debugging
     */
    String getName();
}

enum NodeStatus {
    SUCCESS,
    FAILURE,
    RUNNING
}
```

#### Blackboard Implementation

```java
package com.steve.ai.bt;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared memory for behavior tree nodes
 */
public class Blackboard {
    private final Map<String, Object> data = new HashMap<>();

    // Zone constants
    public static final String ZONE_READONLY = "readonly";
    public static final String ZONE_COMMAND = "command";
    public static final String ZONE_CACHE = "cache";
    public static final String ZONE_DIAGNOSTIC = "diagnostic";

    public void set(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        return value != null ? (T) value : null;
    }

    public <T> T get(String key, Class<T> type, T defaultValue) {
        T value = get(key, type);
        return value != null ? value : defaultValue;
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    public void remove(String key) {
        data.remove(key);
    }

    public void clear() {
        data.clear();
    }
}
```

#### Action Node Adapter

```java
package com.steve.ai.bt;

import com.steve.action.actions.BaseAction;
import com.steve.entity.SteveEntity;

/**
 * Adapts existing BaseAction to BT node interface
 */
public class ActionNode implements BTNode {
    private final BaseAction action;
    private final String name;

    public ActionNode(BaseAction action, String name) {
        this.action = action;
        this.name = name;
    }

    @Override
    public NodeStatus tick(SteveEntity steve, Blackboard context) {
        // Copy blackboard data to action context if needed
        // action.setContext(context);

        if (action.isComplete()) {
            boolean success = action.wasSuccessful();
            action.reset(); // Prepare for reuse
            return success ? NodeStatus.SUCCESS : NodeStatus.FAILURE;
        }

        action.tick();
        return NodeStatus.RUNNING;
    }

    @Override
    public void reset() {
        action.reset();
    }

    @Override
    public String getName() {
        return name;
    }
}
```

#### Sequence Node

```java
package com.steve.ai.bt;

import com.steve.entity.SteveEntity;
import java.util.List;

/**
 * Executes children in order, succeeds if all succeed
 */
public class SequenceNode implements BTNode {
    private final String name;
    private final List<BTNode> children;
    private int currentChild = 0;

    public SequenceNode(String name, List<BTNode> children) {
        this.name = name;
        this.children = children;
    }

    @Override
    public NodeStatus tick(SteveEntity steve, Blackboard context) {
        while (currentChild < children.size()) {
            BTNode child = children.get(currentChild);
            NodeStatus status = child.tick(steve, context);

            if (status == NodeStatus.FAILURE) {
                currentChild = 0; // Reset for next attempt
                return NodeStatus.FAILURE;
            }

            if (status == NodeStatus.RUNNING) {
                return NodeStatus.RUNNING;
            }

            // Child succeeded, move to next
            currentChild++;
        }

        // All children succeeded
        currentChild = 0;
        return NodeStatus.SUCCESS;
    }

    @Override
    public void reset() {
        currentChild = 0;
        for (BTNode child : children) {
            child.reset();
        }
    }

    @Override
    public String getName() {
        return name;
    }
}
```

#### Selector Node

```java
package com.steve.ai.bt;

import com.steve.entity.SteveEntity;
import java.util.List;

/**
 * Executes children in order, succeeds if any succeed
 */
public class SelectorNode implements BTNode {
    private final String name;
    private final List<BTNode> children;
    private int currentChild = 0;

    public SelectorNode(String name, List<BTNode> children) {
        this.name = name;
        this.children = children;
    }

    @Override
    public NodeStatus tick(SteveEntity steve, Blackboard context) {
        while (currentChild < children.size()) {
            BTNode child = children.get(currentChild);
            NodeStatus status = child.tick(steve, context);

            if (status == NodeStatus.SUCCESS) {
                currentChild = 0; // Reset for next attempt
                return NodeStatus.SUCCESS;
            }

            if (status == NodeStatus.RUNNING) {
                return NodeStatus.RUNNING;
            }

            // Child failed, try next
            currentChild++;
        }

        // All children failed
        currentChild = 0;
        return NodeStatus.FAILURE;
    }

    @Override
    public void reset() {
        currentChild = 0;
        for (BTNode child : children) {
            child.reset();
        }
    }

    @Override
    public String getName() {
        return name;
    }
}
```

#### Decorator: Cooldown

```java
package com.steve.ai.bt;

import com.steve.entity.SteveEntity;

/**
 * Prevents child execution within cooldown period
 */
public class CooldownDecorator implements BTNode {
    private final String name;
    private final BTNode child;
    private final double cooldownSeconds;
    private long lastExecutionTime = -1;

    public CooldownDecorator(String name, BTNode child, double cooldownSeconds) {
        this.name = name;
        this.child = child;
        this.cooldownSeconds = cooldownSeconds;
    }

    @Override
    public NodeStatus tick(SteveEntity steve, Blackboard context) {
        long currentTime = System.currentTimeMillis();

        if (lastExecutionTime >= 0) {
            double elapsedSeconds = (currentTime - lastExecutionTime) / 1000.0;
            if (elapsedSeconds < cooldownSeconds) {
                // Still in cooldown
                return NodeStatus.FAILURE;
            }
        }

        NodeStatus status = child.tick(steve, context);

        if (status != NodeStatus.RUNNING) {
            // Child completed, start cooldown
            lastExecutionTime = currentTime;
        }

        return status;
    }

    @Override
    public void reset() {
        child.reset();
        lastExecutionTime = -1;
    }

    @Override
    public String getName() {
        return name;
    }
}
```

### 6.3 Behavior Tree Manager

```java
package com.steve.ai.bt;

import com.steve.entity.SteveEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages behavior tree execution for a Steve entity
 */
public class BehaviorTreeManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final SteveEntity steve;
    private final BTNode rootNode;
    private final Blackboard blackboard;

    private boolean isRunning = false;

    public BehaviorTreeManager(SteveEntity steve, BTNode rootNode) {
        this.steve = steve;
        this.rootNode = rootNode;
        this.blackboard = new Blackboard();

        // Initialize with Steve's data
        blackboard.set("steve", steve);
        blackboard.set("world", steve.level());
    }

    public void tick() {
        if (!isRunning) {
            return;
        }

        try {
            NodeStatus status = rootNode.tick(steve, blackboard);

            switch (status) {
                case SUCCESS:
                    LOGGER.info("Behavior tree completed successfully");
                    isRunning = false;
                    break;
                case FAILURE:
                    LOGGER.warn("Behavior tree failed");
                    isRunning = false;
                    break;
                case RUNNING:
                    // Continue execution next tick
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Error executing behavior tree", e);
            isRunning = false;
        }
    }

    public void start() {
        isRunning = true;
        rootNode.reset();
        LOGGER.info("Starting behavior tree: {}", rootNode.getName());
    }

    public void stop() {
        isRunning = false;
        LOGGER.info("Stopping behavior tree");
    }

    public Blackboard getBlackboard() {
        return blackboard;
    }

    public boolean isRunning() {
        return isRunning;
    }
}
```

### 6.4 LLM-Driven Tree Generation

```java
package com.steve.ai.bt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steve.ai.llm.OpenAIClient;
import java.util.Map;

/**
 * Generates behavior trees from natural language using LLM
 */
public class BehaviorTreeGenerator {
    private final OpenAIClient llmClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        You are a behavior tree generator for Minecraft AI agents.

        Convert natural language commands into behavior tree structures.

        Available node types:
        - Sequence: Execute children in order, succeed if all succeed
        - Selector: Execute children in order, succeed if any succeed
        - Action: Execute a specific action (from action registry)
        - Condition: Check a condition and return success/failure

        Available actions:
        - MoveTo(target)
        - MineBlock(blockType)
        - CraftItem(itemType, count)
        - PlaceBlock(blockType, position)
        - Attack(target)

        Output format: JSON
        """;

    public BehaviorTreeGenerator(OpenAIClient llmClient) {
        this.llmClient = llmClient;
        this.objectMapper = new ObjectMapper();
    }

    public BTNode generateTree(String command) {
        String prompt = String.format("Generate a behavior tree for: %s", command);

        try {
            String response = llmClient.complete(SYSTEM_PROMPT, prompt);
            @SuppressWarnings("unchecked")
            Map<String, Object> treeStructure = objectMapper.readValue(response, Map.class);

            return buildTree(treeStructure);
        } catch (Exception e) {
            LOGGER.error("Failed to generate behavior tree", e);
            // Return fallback tree
            return createFallbackTree(command);
        }
    }

    private BTNode buildTree(Map<String, Object> structure) {
        String type = (String) structure.get("type");

        switch (type) {
            case "Sequence":
                return buildSequence(structure);
            case "Selector":
                return buildSelector(structure);
            case "Action":
                return buildAction(structure);
            case "Condition":
                return buildCondition(structure);
            default:
                throw new IllegalArgumentException("Unknown node type: " + type);
        }
    }

    private BTNode buildSequence(Map<String, Object> structure) {
        String name = (String) structure.get("name");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children =
            (List<Map<String, Object>>) structure.get("children");

        List<BTNode> childNodes = children.stream()
            .map(this::buildTree)
            .toList();

        return new SequenceNode(name, childNodes);
    }

    // Similar methods for Selector, Action, Condition...
}
```

### 6.5 Example: Building a House

#### Natural Language Command
```
User: "Build a small wooden house"
```

#### Generated Behavior Tree

```java
BTNode buildHouseTree = new SequenceNode("BuildSmallWoodenHouse", List.of(
    // Phase 1: Gather materials
    new SequenceNode("GatherMaterials", List.of(
        new SelectorNode("GetWood", List.of(
            new SequenceNode("UseInventory", List.of(
                new ConditionNode("HasWood", (steve, ctx) ->
                    steve.getInventory().count(Item.OAK_LOG) >= 64),
                new ActionNode(new DoNothingAction(), "AlreadyHaveWood")
            )),
            new SequenceNode("ChopTrees", List.of(
                new ActionNode(new FindTreeAction(), "FindTree"),
                new ActionNode(new MineBlockAction(), "ChopTree"),
                new RepeatDecorator(new ActionNode(new MineBlockAction()), 63)
            ))
        )),
        new SelectorNode("GetFuel", List.of(
            new ConditionNode("HasCoal", (steve, ctx) ->
                steve.getInventory().count(Item.COAL) >= 8),
            new SequenceNode("MineCoal", List.of(
                new ActionNode(new FindOreAction(Block.COAL_ORE), "FindCoal"),
                new RepeatDecorator(new ActionNode(new MineBlockAction()), 8)
            ))
        ))
    )),

    // Phase 2: Craft items
    new SequenceNode("CraftItems", List.of(
        new ActionNode(new CraftPlanksAction(64), "CraftPlanks"),
        new ActionNode(new CraftSticksAction(32), "CraftSticks"),
        new ActionNode(new CraftTorchesAction(16), "CraftTorches")
    )),

    // Phase 3: Find location
    new SelectorNode("FindLocation", List.of(
        new SequenceNode("UseTargetLocation", List.of(
            new ConditionNode("HasTargetLocation", (steve, ctx) ->
                ctx.has("build_location")),
            new ActionNode(new MoveToAction(() -> ctx.get("build_location", BlockPos.class)), "GoToLocation")
        )),
        new SequenceNode("FindNewLocation", List.of(
            new ActionNode(new FindBuildSiteAction(5, 5, 3), "FindSite"),
            new ActionNode(new MoveToAction(() -> steve.getBuildSite()), "GoToSite")
        ))
    )),

    // Phase 4: Build structure
    new SequenceNode("BuildStructure", List.of(
        new ActionNode(new BuildWallAction(5, 3), "BuildWalls"),
        new ActionNode(new BuildRoofAction(5, 5), "BuildRoof"),
        new ActionNode(new PlaceDoorAction(), "PlaceDoor"),
        new ActionNode(new PlaceTorchesAction(), "AddLighting")
    ))
));
```

### 6.6 Integration with Existing System

```java
package com.steve.action;

import com.steve.ai.bt.*;
import com.steve.entity.SteveEntity;
import com.steve.ai.llm.TaskPlanner;

/**
 * Enhanced ActionExecutor with behavior tree support
 */
public class BehaviorTreeActionExecutor extends ActionExecutor {
    private final BehaviorTreeGenerator btGenerator;
    private BehaviorTreeManager currentBT;

    public BehaviorTreeActionExecutor(SteveEntity steve, TaskPlanner planner) {
        super(steve, planner);
        this.btGenerator = new BehaviorTreeGenerator(planner.getLLMClient());
    }

    @Override
    public void tick() {
        // If BT is running, tick it
        if (currentBT != null && currentBT.isRunning()) {
            currentBT.tick();
            return;
        }

        // Otherwise, fall back to original behavior
        super.tick();
    }

    @Override
    public void executeTask(String command) {
        // Try to generate behavior tree
        BTNode tree = btGenerator.generateTree(command);

        if (tree != null) {
            // Execute using behavior tree
            currentBT = new BehaviorTreeManager(steve, tree);
            currentBT.start();
        } else {
            // Fall back to original task execution
            super.executeTask(command);
        }
    }
}
```

### 6.7 Benefits for Steve AI

| Aspect | Current System | BT-Enhanced System |
|--------|---------------|-------------------|
| **Task Planning** | LLM → task list | LLM → behavior tree |
| **Execution** | Sequential task queue | Hierarchical, reactive execution |
| **Error Handling** | Task-level retry | Node-level retry with fallbacks |
| **Reactivity** | Complete current task | Tree re-evaluates each tick |
| **Extensibility** | Add new actions | Add actions + control nodes |
| **Observability** | Task status logs | Full tree execution trace |
| **Reusability** | Task templates | Subtree templates |
| **Multi-Agent** | Collaborative manager | BT coordination nodes |

---

## 7. References

### Academic Papers

1. **LLM-BRAIn: AI-driven Fast Generation of Robot Behaviour Tree based on Large Language Model** (2023)
   - arXiv:2305.19352
   - [Harvard ADS](https://ui.adsabs.harvard.edu/abs/2023arXiv230519352L/abstract)

2. **VLM-driven Behavior Tree for Context-aware Task Planning** (2025)
   - arXiv:2501.03968
   - Integrates vision-language models for visual conditions

3. **LLM-as-BT-Planner: Leveraging LLMs for Behavior Tree Generation in Robot Task Planning** (2025)
   - IEEE ICRA 2025
   - Natural language to BT compilation

4. **Behavior Tree Generation using Large Language Models for Robotic Manipulation** (2024)
   - arXiv:2409.09435
   - Comparison of generation methods

5. **ReAcTree: Hierarchical LLM Agent Trees with Control Flow** (2025)
   - arXiv:2511.02424
   - Recursive sub-agent spawning

6. **Interpretable Robot Control via Structured Behavior Trees** (2025)
   - arXiv:2508.09621

### Game Development Resources

7. **Halo 2 AI - Handling Complexity** (GDC 2005)
   - Damian Isla, Bungie Studios
   - Original behavior tree presentation

8. **Behavior Tree Documentation**
   - [BehaviorTree.CPP Documentation (Chinese translation)](https://www.cnblogs.com/hokori/p/14158204.html)
   - Fallback/Selector nodes in detail

9. **Lightweight Behavior Trees for Embedded Systems**
   - [CSDN Blog](https://blog.csdn.net/stallion5632/article/details/150446949)
   - Retry, Cooldown, Timeout decorators

10. **ROS2 Behavior Tree Analysis**
    - [CSDN Blog](https://m.blog.csdn.net/weixin_44506510/article/details/140348105)
    - ReactiveFallback patterns

### Game AI History

11. **Finite State Machines vs Behavior Trees**
    - [FSM vs BT Comparison (Chinese)](https://m.blog.csdn.net/gao7009/article/details/80221163)
    - Complexity analysis and replacement reasons

12. **Unity Behavior Tree Implementation**
    - [Unity2017 Game AI Programming](https://www.cnblogs.com/apachecn/p/19164892)
    - Node types and patterns

13. **Behavior Designer (Unity Asset)**
    - [Behavior Tree and Groot](https://www.cnblogs.com/lyggqm/p/18438845)
    - Visual editor examples

### Industry Adoption

14. **BioShock AI**
    - Behavior tree for complex enemy behaviors

15. **Spore AI**
    - Adaptive personality systems with BTs

16. **Red Dead Redemption 2**
    - Wildlife ecosystem simulation

### Online Resources

17. **Behavior Tree Learning Guide**
    - [Comprehensive Guide (Chinese)](https://www.cnblogs.com/forever3329/p/19062094)
    - Node types and patterns

18. **Unreal Engine 5 AI**
    - [UE5 Behavior Tree Tutorial](https://www.bilibili.com/)
    - Blackboard integration

19. **Wjybxx.BTree.Core**
    - [NuGet Package](https://www.nuget.org/packages/Wjybxx.BTree.Core/)
    - Context sharing implementation

### Related Steve AI Documentation

- `C:\Users\casey\steve\docs\architecture\PLANNING_SYSTEM.md` - Current planning architecture
- `C:\Users\casey\steve\docs\ACTION_API.md` - Existing action system
- `C:\Users\casey\steve\docs\ACTION_SYSTEM_IMPROVEMENTS.md` - Action system enhancements
- `C:\Users\casey\steve\docs\HTN_PLANNER_DESIGN.md` - Hierarchical Task Network design
- `C:\Users\casey\steve\CLAUDE.md` - Project overview and build commands

---

## Appendix A: Quick Reference

### Common Behavior Tree Patterns

```
# Fallback with retry
[Selector]
    ├──> [RetryDecorator: maxAttempts=3]
    │    └──> [Action: TryPrimaryMethod]
    ├──> [Action: TrySecondaryMethod]
    └──> [Action: TryTertiaryMethod]

# Cooldown limiting
[Cooldown: 5 seconds]
    └──> [Action: UseExpensiveAbility]

# Parallel with threshold
[Parallel: succeedThreshold=2]
    ├──> [Action: Unit1]
    ├──> [Action: Unit2]
    └──> [Action: Unit3]
    # Succeeds if 2+ complete

# Timeout protection
[Timeout: 10 seconds]
    └──> [Action: LongRunningOperation]

# Conditional execution
[Sequence]
    ├──> [Condition: IsDaytime]
    ├──> [Action: Photosynthesis]
    └──> [Action: Grow]

# Loop with break condition
[RepeatUntilSuccess]
    └──> [Selector]
        ├──> [Condition: IsDoorOpen]
        └──> [Action: OpenDoor]
```

### Node Status Codes

| Status | Symbol | Meaning | Parent Behavior |
|--------|--------|---------|-----------------|
| SUCCESS | ✓ | Node completed successfully | Sequence: continue, Selector: return success |
| FAILURE | ✗ | Node failed | Sequence: return failure, Selector: try next |
| RUNNING | ↻ | Node still executing | Pause tree, resume next tick |

---

**Document Status:** Complete
**Last Updated:** 2026-02-28
**Next Review:** When implementing BT system

**Related Documents:**
- HTN_PLANNER_DESIGN.md - Alternative planning approach
- ACTION_API.md - Action system integration
- RESEARCH_GAME_AI_2025.md - General AI research
