# Minecraft AI Agents & Bots Research Review
**Actionable Insights for MineWright Development**

**Date:** 2025-02-27
**Research Focus:** Architecture, design patterns, and execution strategies from leading Minecraft AI projects

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Baritone - Advanced Pathfinding Engine](#1-baritone---advanced-pathfinding-engine)
3. [Mineflayer - JavaScript Bot Framework](#2-mineflayer---javascript-bot-framework)
4. [Project Malmo - Microsoft Research Platform](#3-project-malmo---microsoft-research-platform)
5. [Voyager - LLM-Powered Lifelong Learning](#4-voyager---llm-powered-lifelong-learning)
6. [MineDojo - NVIDIA's RL Framework](#5-minedojo---nvidias-rl-framework)
7. [MineRL - Diamond Benchmark Competition](#6-minerl---diamond-benchmark-competition)
8. [STEVE Series - Vision-Language-Action Agents](#7-steve-series---vision-language-action-agents)
9. [Cross-Project Architectural Patterns](#cross-project-architectural-patterns)
10. [Actionable Recommendations for MineWright](#actionable-recommendations-for-minewright)

---

## Executive Summary

This document synthesizes research from seven major Minecraft AI/bot projects to identify proven architectural patterns, execution strategies, and innovations that can enhance MineWright's capabilities. Key findings include:

**Core Pattern: Hierarchical Task Decomposition**
- All successful projects use multi-level planning (high-level goals → low-level primitives)
- LLM-based agents excel at task decomposition; RL-based agents excel at skill execution

**Best-in-Class Technologies:**
| Domain | Leader | Key Innovation |
|--------|--------|----------------|
| Pathfinding | Baritone | Enhanced A* with hierarchical planning |
| LLM Integration | Voyager | Self-improving code generation |
| Framework | Mineflayer | Modular plugin architecture |
| RL Training | MineDojo | Internet-scale knowledge base |
| Benchmark | MineRL | Long-horizon sparse rewards |

**Critical Gap Analysis for MineWright:**
- ❌ No dedicated pathfinding system (relies on vanilla Minecraft AI)
- ❌ Limited multi-agent coordination algorithms
- ❌ No skill library or persistent learning
- ✅ Strong async LLM integration
- ✅ Good plugin architecture foundation
- ✅ State machine and interceptor patterns implemented

---

## 1. Baritone - Advanced Pathfinding Engine

### Overview
**Project:** [cabaletta/baritone](https://github.com/cabaletta/baritone)
**Language:** Java (Forge/Fabric/NeoForge mod)
**Status:** Production-ready, 7.5k+ GitHub stars

### Architecture

#### Core Algorithm: Enhanced A* Pathfinding
```
┌─────────────────────────────────────────────────────────────┐
│                    Path Calculation                          │
├─────────────────────────────────────────────────────────────┤
│  1. World Modeling                                          │
│     - Chunk-based caching                                   │
│     - Real-time obstacle updates                            │
│     - Cost evaluation per movement type                     │
│                                                              │
│  2. Hierarchical Path Calculation                           │
│     - Long-distance: Macro routes (coarse granularity)      │
│     - Near-target: Precision refinement (fine granularity)  │
│                                                              │
│  3. Optimization                                            │
│     - Node pruning (remove redundancies)                    │
│     - Priority queue management                             │
│     - Lazy evaluation (calculate only when needed)          │
└─────────────────────────────────────────────────────────────┘
```

#### Key Components

| Component | Responsibility |
|-----------|----------------|
| `AStarPathFinder.java` | Core A* algorithm with custom heuristics |
| `PathingBehavior.java` | High-level navigation control |
| `MovementHelper.java` | Movement cost calculations (walk/jump/swim/place/break) |
| `ChunkPacker.java` | Efficient world data caching |
| `AvoidanceSystem.java` | Hazard detection (lava, void, entities) |

### Design Patterns Applied

**1. Strategy Pattern (Movement Costs)**
```java
// Different movement types have configurable costs
walk_cost = 1.0
jump_cost = 2.0
break_block_cost = 10.0
place_block_cost = 15.0
```

**2. Hierarchical Planning**
- **Macro Level:** Plan route across chunks (low resolution)
- **Micro Level:** Refine path within 16 blocks of target
- **Result:** 30x faster than predecessor MineBot

**3. Dynamic Replanning**
- Event-driven updates when obstacles detected
- Path recalculation without full recompute
- Avoidance weight system for hazardous areas

### Key Features

| Feature | Implementation |
|---------|----------------|
| **Auto-navigation** | `#goto <x> <z>` command |
| **Parkour movement** | Precise jump mechanics (`MovementParkour` class) |
| **Block breaking/placing** | Integrated into pathfinding cost |
| **Elytra flight** | Optimized nether travel |
| **Schematic building** | Integration with Litematica/Schematica |
| **Biome awareness** | Environment-specific path strategies |

### Lessons for MineWright

**Priority 1: Adopt Enhanced A* Pathfinding**
- MineWright currently lacks dedicated pathfinding
- Baritone's hierarchical approach is proven at scale
- Implement similar chunk caching and lazy evaluation

**Priority 2: Movement Cost System**
```java
// Add to PathfindAction or new NavigationManager
public enum MovementCost {
    WALK(1.0), JUMP(2.0), SWIM(3.0),
    BREAK_BLOCK(10.0), PLACE_SCAFFOLD(15.0);
}
```

**Priority 3: Dynamic Replanning**
- Current `PathfindAction` is static
- Implement event-driven path updates
- Add obstacle avoidance weights

---

## 2. Mineflayer - JavaScript Bot Framework

### Overview
**Project:** [PrismarineJS/mineflayer](https://github.com/PrismarineJS/mineflayer)
**Language:** JavaScript/Node.js
**Versions:** Supports Minecraft 1.8 to 1.21.8 (11 years of compatibility)

### Architecture

#### Modular Plugin System
```
mineflayer-core
    │
    ├── prismarine-physics (gravity, collision)
    ├── prismarine-pathfinder (A* navigation)
    ├── mineflayer-pathfinder (goals, movements)
    ├── mineflayer-pvp (combat)
    ├── mineflayer-collectblock (item gathering)
    └── mineflayer-tool (smart tool selection)
```

### Key Plugin: mineflayer-pathfinder

#### Enhanced A* Implementation
```javascript
const pathfinder = require('mineflayer-pathfinder').pathfinder
const Movements = require('mineflayer-pathfinder').Movements
const { GoalNear, GoalBlock, GoalComposite } = require('mineflayer-pathfinder').goals

// Configurable movement costs
const defaultMove = new Movements(bot)
defaultMove.canDig = true  // Allow breaking obstacles
defaultMove.scafoldingBlocks = []  // Blocks for building bridges

// Composite goals support
bot.pathfinder.setGoal(new GoalComposite(
    new GoalNear(x1, y1, z1, 1),
    new GoalBlock(x2, y2, z2)
))
```

#### Goal System
| Goal Type | Description |
|-----------|-------------|
| `GoalNear` | Reach proximity to position |
| `GoalBlock` | Exact block position |
| `GoalXZ` | Horizontal pathfinding |
| `GoalComposite` | Combine multiple goals |
| `GoalInverted` | Avoid specific areas |

### Design Patterns

**1. Event-Driven Architecture**
```javascript
bot.on('chat', (username, message) => {
    if (message === 'come') {
        bot.pathfinder.setGoal(new GoalNear(target.x, target.y, target.z, 1))
    }
})

bot.on('path_update', (path) => {
    // React to path changes
})
```

**2. Promise-Based Async API**
```javascript
try {
    await bot.pathfinder.goto(goal)
    bot.chat('Reached destination!')
} catch (err) {
    bot.chat('Pathfinding failed: ' + err)
}
```

**3. Physics Simulation**
- `prismarine-physics` provides independent physics engine
- Gravity, collision detection, entity interactions
- Allows prediction without server interaction

### Lessons for MineWright

**Priority 1: Goal Abstraction Layer**
```java
// Create goal system similar to mineflayer-pathfinder
public interface NavigationGoal {
    boolean isReached(Vec3 pos);
    double heuristic(Vec3 from);
}

public class GoalNear implements NavigationGoal {
    private final Vec3 target;
    private final double radius;
    // ...
}

public class GoalComposite implements NavigationGoal {
    private final List<NavigationGoal> goals;
    // ...
}
```

**Priority 2: Physics Prediction**
- MineWright relies on server-side physics
- Add client-side prediction for smoother pathfinding
- Prevent unnecessary server calls

**Priority 3: Modular Action System**
- Current `ActionRegistry` is good foundation
- Extend to support composite actions
- Add action dependencies and prerequisites

---

## 3. Project Malmo - Microsoft Research Platform

### Overview
**Project:** [Microsoft/Malmo](https://github.com/Microsoft/Malmo)
**Maintainer:** Microsoft Research (Cambridge)
**Purpose:** AI experimentation and reinforcement learning research

### Architecture

#### Two Implementation Approaches

**1. MalmoEnv (Recommended - Pure Python)**
```python
from malmo import MalmoPython

# OpenAI gym-like interface
import gym
import malmoenv

env = gym.make('MalmoEnv-v0')
obs = env.reset()
```

**2. Native Implementation (Python + Java)**
- Direct Java integration for modded Minecraft
- Lower latency, more control
- Requires build process

### Key Features

| Feature | Description |
|---------|-------------|
| **Multi-Agent Support** | Test cooperation/competition strategies |
| **Reinforcement Learning** | Built-in reward function system |
| **Computer Vision** | Pixel-based observation for vision models |
| **Task Specification** | XML-based mission configuration |
| **Cross-Platform** | Windows, Linux, macOS support |

### Mission XML Format
```xml
<MissionGoal>
    <Goal>
        <Item type="diamond" amount="1"/>
    </Goal>
</MissionGoal>

<AgentSection>
    <Name>Bot0</Name>
    <AgentStart>
        <Placement x="0.5" y="56" z="0.5"/>
    </AgentStart>
    <AgentHandlers>
        <ObservationFromFullStats/>
        <ContinuousMovementCommands/>
        <InventoryCommands/>
    </AgentHandlers>
</AgentSection>
```

### Design Patterns

**1. Declarative Task Specification**
- XML missions define goals, constraints, environment
- Separates task logic from agent implementation
- Enables reproducible experiments

**2. Observation Abstraction**
```python
# Multiple observation types available
observations = {
    'full_stats': json_observation,
    'pixels': image_observation,  # For vision models
    'radar': entity_positions,
    'inventory': item_counts
}
```

**3. Multi-Agent Coordination Protocol**
```xml
<AgentSection mode="Survival">
    <Name>Agent1</Name>
    <AgentHandlers>
        <ChatCommands/>
        <ObservationFromChat/>
    </AgentHandlers>
</AgentSection>
```

### Lessons for MineWright

**Priority 1: Task Specification Language**
- MineWright uses natural language (LLM-dependent)
- Add structured task format for reliability
- Example: JSON task definitions

```java
public class StructuredTask {
    String taskType;  // "mine", "build", "craft"
    Map<String, Object> parameters;
    List<String> prerequisites;
    String successCondition;
}
```

**Priority 2: Multi-Agent Communication**
- Current `AgentCommunicationBus` is good foundation
- Add structured message types (inspired by Malmo's chat)
- Implement agent roles explicitly

**Priority 3: Observation Abstraction Layer**
```java
// Create similar observation abstraction
public interface ObservationSource {
    Observation getObservation();
}

public enum ObservationType {
    INVENTORY, WORLD_STATE, ENTITY_RADAR, VISUAL
}
```

---

## 4. Voyager - LLM-Powered Lifelong Learning

### Overview
**Project:** [MineDojo/Voyager](https://github.com/MineDojo/Voyager)
**Paper:** "Voyager: An Open-Ended Embodied Agent with Large Language Models" (2023)
**Language:** Python (uses Mineflayer for Minecraft interaction)

### Architecture: Three Core Components

```
┌──────────────────────────────────────────────────────────────┐
│  1. AUTOMATIC CURRICULUM (Exploration Engine)               │
│     - Generates increasingly difficult tasks                │
│     - Maximizes exploration novelty                         │
│     - Expands agent's capabilities progressively            │
├──────────────────────────────────────────────────────────────┤
│  2. SKILL LIBRARY (Code Repository)                        │
│     - Stores executable code functions                      │
│     - Self-verifying and self-improving                    │
│     - Temporally extended, compositional behaviors          │
├──────────────────────────────────────────────────────────────┤
│  3. ITERATIVE PROMPTING MECHANISM                           │
│     - Environment feedback incorporation                     │
│     - Execution error handling                              │
│     - Self-verification for program improvement             │
└──────────────────────────────────────────────────────────────┘
```

### Component 1: Automatic Curriculum

**Task Generation Strategy**
```python
def propose_next_task(agent_state, world_state):
    # LLM generates candidate tasks
    candidates = llm.generate([
        "Build a shelter",
        "Craft iron tools",
        "Find diamonds"
    ])

    # Rank by novelty and feasibility
    ranked = rank_by_novelty(candidates, agent_state)

    return ranked[0]  # Return best task
```

**Curriculum Progression**
1. **Early Game:** Basic survival (wood, stone, shelter)
2. **Mid Game:** Resource gathering (iron, food, trading)
3. **Late Game:** Advanced goals (diamonds, enchanting, nether)

### Component 2: Self-Improving Skill Library

**Skill Storage Format**
```python
# Each skill is executable code
def craft_iron_pickaxe(bot):
    """
    Crafts an iron pickaxe if materials available.
    Self-verifying: checks inventory before and after.
    """
    if not bot.inventory.has_item('iron_ingot', 3):
        return False

    bot.craft_item('iron_pickaxe')
    assert bot.inventory.has_item('iron_pickaxe')
    return True
```

**Skill Composition**
```python
# Complex actions compose primitive skills
def establish_iron_farm(bot):
    mine_iron_ore(bot)           # Primitive skill
    smelt_iron_ingots(bot)       # Primitive skill
    craft_iron_pickaxe(bot)      # Primitive skill
    build_farm_structure(bot)    # Composite skill
```

**Skill Properties**
| Property | Description |
|----------|-------------|
| **Executable** | Direct Python code (no interpretation) |
| **Compositional** | Skills can combine into complex behaviors |
| **Self-Verifying** | Assertions check success conditions |
| **Iteratively Improved** | Failed executions trigger refinement |

### Component 3: Iterative Prompting Mechanism

**Feedback Loop**
```
┌────────────────┐
│ Initial Prompt │
└────────┬───────┘
         │
         ▼
┌────────────────┐
│ Generate Code  │
└────────┬───────┘
         │
         ▼
┌────────────────┐
│ Execute in MC  │
└────────┬───────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐ ┌──────────┐
│ Success │ │  Error   │
└───┬────┘ └─────┬────┘
    │            │
    ▼            ▼
Store Skill   Refine with
in Library    Error Feedback
```

**Prompt Template**
```python
prompt = f"""
Task: {task_description}
Context: {world_state}
Available Skills: {skill_library}

Previous Attempt (Failed):
{previous_code}
Error: {error_message}

Generate improved code:
"""
```

### Performance Results

| Metric | Voyager | Prior SOTA | Improvement |
|--------|---------|------------|-------------|
| Unique Items | 3.3x more | Baseline | +230% |
| Distance Traveled | 2.3x more | Baseline | +130% |
| Tech Tree Speed | 15.3x faster | Baseline | +1430% |

### Key Innovation: Blackbox LLM Interaction
- No model fine-tuning required
- Uses GPT-4 via API only
- Skills transfer to new worlds (zero-shot generalization)

### Lessons for MineWright

**Priority 1: Implement Skill Library**
```java
// Add to memory package
public class SkillLibrary {
    private Map<String, ExecutableSkill> skills;

    public void learnSkill(String name, String code, TaskContext context) {
        ExecutableSkill skill = compileAndVerify(code, context);
        skills.put(name, skill);
    }

    public Optional<ExecutableSkill> getSkill(String name) {
        return Optional.ofNullable(skills.get(name));
    }

    public List<String> findRelevantSkills(String task) {
        // Semantic search for applicable skills
        return vectorStore.search(task);
    }
}

public interface ExecutableSkill {
    ActionResult execute(TaskContext context);
    boolean verifyPreconditions(TaskContext context);
    boolean verifyPostconditions(TaskContext context);
}
```

**Priority 2: Automatic Curriculum Generation**
```java
// Add to orchestration package
public class CurriculumGenerator {
    public Task proposeNextTask(AgentState state, WorldKnowledge world) {
        // Use LLM to generate candidate tasks
        List<Task> candidates = llmClient.generateTasks(state, world);

        // Rank by novelty and feasibility
        return candidates.stream()
            .max(Comparator.comparing(t -> calculateNovelty(t, state)))
            .orElse(null);
    }

    private double calculateNovelty(Task task, AgentState state) {
        // Novelty = f(rarity, difficulty, new_skills_required)
        double rarity = 1.0 / (task.getFrequency() + 1);
        double difficulty = task.getEstimatedDifficulty();
        double learning = task.getNewSkills().size();
        return rarity * difficulty * learning;
    }
}
```

**Priority 3: Iterative Code Improvement**
```java
// Enhance ResponseParser to handle code refinement
public class IterativeCodeRefiner {
    public String refineCode(String originalCode, String error, TaskContext context) {
        String prompt = String.format("""
            Task: %s
            Original Code:
            %s

            Execution Error:
            %s

            World State:
            %s

            Generate fixed code:
            """, context.getTask(), originalCode, error, context.getWorldState());

        return llmClient.generate(prompt);
    }
}
```

---

## 5. MineDojo - NVIDIA's RL Framework

### Overview
**Project:** [MineDojo](https://github.com/MineDojo/MineDojo)
**Organization:** NVIDIA Research
**Award:** NeurIPS Distinguished Dataset Award (2022)

### Architecture: Knowledge-Base Driven Learning

```
┌─────────────────────────────────────────────────────────────┐
│                  INTERNET-SCALE KNOWLEDGE BASE              │
├─────────────────────────────────────────────────────────────┤
│  730K YouTube Videos (Gameplay Demonstrations)              │
│  7K Wiki Pages (Game Mechanics & Knowledge)                 │
│  340K Reddit Posts (Community Strategies & Discussions)     │
│  ↓                                                           │
│  Video-Language Models for Reward Functions                 │
│  Skill Extraction from Gameplay Videos                      │
│  Knowledge Graph of Game Concepts                           │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

**1. MineDojo Environment**
```python
import minedojo

env = minedojo.make(
    task_id="combat_spider_melee",
    world_seed=123,
    image_size=(800, 600)
)

obs, info = env.reset()
# obs contains: rgb_image, inventory, location, entities...
```

**2. Task Suite (Thousands of Tasks)**
- **Combat:** Fight various mobs with different weapons
- **Construction:** Build specific structures
- **Crafting:** Progress through tech tree
- **Exploration:** Find biomes, structures
- **Survival:** Manage health, hunger, resources

**3. Knowledge Base Integration**
```python
# Pre-trained CLIP-like model for visual grounding
from minedojo.tasks import CLIPReward

reward_fn = CLIPReward(
    prompt="diamond sword",
    knowledge_base="minecraftrun"
)
```

### Design Patterns

**1. Imitation Learning from Videos**
```python
# Extract skills from YouTube gameplay
video demonstrations = load_youtube_videos("minecraft house building")
skills = extract_skills_from_videos(demonstrations)

# Use for imitation learning
agent.learn_from_demonstrations(skills)
```

**2. Multi-Task Learning**
```python
# Single agent learns multiple tasks simultaneously
tasks = [
    "obtain_diamond",
    "build_house",
    "kill_dragon",
    "enchant_item"
]

agent.train_multitask(tasks)
```

**3. Knowledge-Guided Exploration**
```python
# Wiki knowledge informs task decomposition
wiki_knowledge = query_wiki("how to craft diamond pickaxe")
# Returns: ["craft_table", "find_iron", "smelt_iron", "craft_sticks", ...]
```

### Lessons for MineWright

**Priority 1: Knowledge Base Integration**
```java
// Enhance WorldKnowledge with external knowledge
public class EnhancedWorldKnowledge extends WorldKnowledge {
    private VectorStore wikiKnowledge;
    private Map<String, List<String>> craftingRecipes;

    public List<Task> decomposeTaskWithKnowledge(String task) {
        // Query knowledge base for task decomposition
        List<String> steps = wikiKnowledge.search(task + " steps");
        return steps.stream()
            .map(this::parseTask)
            .collect(Collectors.toList());
    }

    public Optional<String> getCraftingRecipe(String item) {
        return Optional.ofNullable(craftingRecipes.get(item));
    }
}
```

**Priority 2: Multi-Task Training**
```java
// Enhance training to handle multiple goals
public class MultiTaskOrchestrator extends OrchestratorService {
    private Map<String, AgentRole> taskRoleMapping;

    public void assignAgentsForMultiTask(List<Task> tasks) {
        for (Task task : tasks) {
            AgentRole optimalRole = determineOptimalRole(task);
            assignTask(task, optimalRole);
        }
    }

    private AgentRole determineOptimalRole(Task task) {
        // Match task to best-suited agent archetype
        return taskRoleMapping.getOrDefault(task.getType(), AgentRole.GENERALIST);
    }
}
```

**Priority 3: Demonstration Learning**
```java
// Add capability to learn from examples
public class DemonstrationLearner {
    public void learnFromExample(String command, Task demonstration) {
        // Store successful execution patterns
        TaskPattern pattern = extractPattern(demonstration);
        patternLibrary.store(command, pattern);
    }

    public Task replicatePattern(String command, TaskContext context) {
        TaskPattern pattern = patternLibrary.find(command);
        return pattern.instantiate(context);
    }
}
```

---

## 6. MineRL - Diamond Benchmark Competition

### Overview
**Competition:** MineRL Challenge @ NeurIPS (2019-2022)
**Benchmark:** `MineRLObtainDiamond-v0`
**Goal:** Obtain diamond from scratch in Minecraft

### Benchmark Challenge

**Task Complexity**
```
Start → Wood → Workbench → Wooden Pick → Stone → Stone Pick
     → Iron → Furnace → Iron Ingots → Iron Pick → Diamond
```

**Why It's Hard**
| Challenge | Description |
|-----------|-------------|
| **Sparse Rewards** | ~7-12 steps from start to diamond |
| **Long Horizons** | Mistakes early cascade to failure |
| **Random Seeds** | Each world is procedurally generated |
| **Large Action Space** | 8000+ wiki pages of mechanics |
| **No Ground Truth** | Must explore and navigate unknown terrain |

### Key Breakthrough: DreamerV3 (DeepMind, 2024)

**Published in Nature** - First algorithm to collect diamonds **without human data**

**World Model Architecture**
```
Observations → World Model (Latent Dynamics) → Planning
                  ↓
              Learn Transition Model
                  ↓
          Predict Future States & Rewards
                  ↓
          Select Best Action via Imagination
```

**Key Innovations**
1. **Latent Space World Model**
   - Compress observations to latent representation
   - Learn dynamics model in latent space
   - Enables long-horizon planning without expensive environment interaction

2. **Model-Based RL**
   - Plan in imagination (fast) before executing in environment (slow)
   - 1 billion steps training (~9 days continuous gameplay)

3. **Reward Shaping**
   - Intermediate rewards for each milestone
   - Prevents credit assignment problem

### Competition Winners

| Year | Team | Approach | Achievement |
|------|------|----------|-------------|
| 2021 | NetEase (Athena AI) | RL from scratch | First successful diamond mining |
| 2022 | Tencent (Juewu) | Imitation + RL | Dominant winner |
| 2023 | DeepMind (DreamerV3) | Model-based RL | Zero-shot success (no imitation) |

### Lessons for MineWright

**Priority 1: Hierarchical Reward System**
```java
// Add reward shaping for complex tasks
public class RewardShaper {
    public double calculateReward(Task task, ActionResult result) {
        double reward = 0;

        // Base reward for completion
        if (result.isSuccess()) {
            reward += task.getBaseReward();
        }

        // Intermediate milestone rewards
        for (Milestone milestone : task.getMilestones()) {
            if (result.hasMilestone(milestone)) {
                reward += milestone.getReward();
            }
        }

        // Penalty for time/resources
        reward -= result.getTimeCost() * TIME_PENALTY;
        reward -= result.getResourceCost() * RESOURCE_PENALTY;

        return reward;
    }
}
```

**Priority 2: Curriculum Learning**
```java
// Implement progressive task difficulty
public class CurriculumScheduler {
    private List<TaskTier> tiers = List.of(
        new TaskTier("T1", List.of("get_wood", "craft_planks")),
        new TaskTier("T2", List.of("craft_stone_pick", "get_iron")),
        new TaskTier("T3", List.of("smelt_iron", "craft_iron_pick")),
        new TaskTier("T4", List.of("find_diamond", "mine_diamond"))
    );

    public Task getNextTask(AgentState state) {
        // Agent must master Tier N before Tier N+1
        for (TaskTier tier : tiers) {
            if (!state.hasMastered(tier)) {
                return tier.selectNextTask(state);
            }
        }
        return null;
    }
}
```

**Priority 3: World State Prediction**
```java
// Add predictive modeling for planning
public class WorldModel {
    public List<WorldState> predictFutures(WorldState current, Action action, int steps) {
        List<WorldState> predictions = new ArrayList<>();
        WorldState simulated = current;

        for (int i = 0; i < steps; i++) {
            simulated = simulateAction(simulated, action);
            predictions.add(simulated);
        }

        return predictions;
    }

    public Action selectBestAction(WorldState state, List<Action> candidates) {
        return candidates.stream()
            .max(Comparator.comparing(a -> predictReward(state, a, 10)))
            .orElse(null);
    }
}
```

---

## 7. STEVE Series - Vision-Language-Action Agents

### Overview

**STEVE-1 (NeurIPS 2023)**
- Generative model for text-to-behavior
- Foundation for video-based Minecraft agents

**STEVE-EYE (ECCV 2024)**
- Fine-tuned Llama with internet text
- Improved planning via in-context learning

**MineDreamer (ECCV 2024)**
- Instruction-following with VLMs
- Predicts future visual observations

### Architecture: Vision-Language-Action (VLA) Model

```
┌─────────────────────────────────────────────────────────────┐
│                     VLA PIPELINE                             │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Text Instruction → LLM Encoder → Text Embedding            │
│       ↓                                                      │
│  Video Frame → Vision Encoder → Visual Embedding            │
│       ↓                                                      │
│  Concatenate(Text, Visual) → Action Decoder                 │
│       ↓                                                      │
│  Minecraft Actions (Keyboard + Mouse)                        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Key Innovation: Direct Action Generation

**Traditional Approach (2-stage)**
```
LLM → High-Level Plan → Grounding Policy → Low-Level Actions
```

**VLA Approach (1-stage)**
```
Multimodal Input → Direct Action Generation
```

### STEVE-1 Architecture

**Training Pipeline**
1. **Video Collection:** 73,000 YouTube videos (2700 hours)
2. **Annotation:** Text descriptions for key moments
3. **Model Training:** Text → Video frames → Actions
4. **Fine-tuning:** Minecraft-specific action space

**Action Space**
```
Keyboard: W, A, S, D, Space, Shift, 1-9 (hotbar)
Mouse: Move (camera), Click (attack/break), Right-click (place/use)
```

### Lessons for MineWright

**Priority 1: Visual Observation Integration**
```java
// Add visual context to planning
public class VisualObserver {
    public List<VisualFeature> extractFeatures(PoseStack pose, Level level) {
        // Screenshot analysis
        BufferedImage screenshot = captureScreen();

        // Entity detection
        List<Entity> visibleEntities = getVisibleEntities();

        // Block detection
        List<Block> visibleBlocks = getVisibleBlocks();

        // Compose visual context
        return List.of(
            new VisualFeature("entities", encodeEntities(visibleEntities)),
            new VisualFeature("blocks", encodeBlocks(visibleBlocks)),
            new VisualFeature("terrain", encodeTerrain(screenshot))
        );
    }
}
```

**Priority 2: Multimodal Prompt Building**
```java
// Enhance PromptBuilder with visual context
public class MultimodalPromptBuilder extends PromptBuilder {
    public String buildMultimodalPrompt(String userCommand, List<VisualFeature> visuals) {
        return String.format("""
            %s

            VISUAL CONTEXT:
            %s

            USER COMMAND:
            %s

            Available Actions:
            %s

            Generate action sequence:
            """,
            buildSystemPrompt(),
            formatVisualContext(visuals),
            userCommand,
            listAvailableActions()
        );
    }

    private String formatVisualContext(List<VisualFeature> visuals) {
        return visuals.stream()
            .map(v -> String.format("- %s: %s", v.name(), v.data()))
            .collect(Collectors.joining("\n"));
    }
}
```

---

## Cross-Project Architectural Patterns

### Pattern 1: Hierarchical Task Decomposition

**Universal Across All Projects**
```
High-Level Goal
    ↓
Mid-Level Plan
    ↓
Low-Level Primitives
```

**Implementations:**
| Project | High-Level | Mid-Level | Low-Level |
|---------|------------|-----------|-----------|
| Baritone | #goto | Chunk route | Walk/Jump/Place |
| Mineflayer | GoalComposite | Path segments | Movements |
| Voyager | Task | Sub-goals | Skill functions |
| MineDojo | Mission | Objectives | Actions |

**MineWright Enhancement:**
```java
public class HierarchicalPlanner {
    public Plan plan(Task highLevelTask) {
        // Level 1: Task decomposition
        List<SubGoal> subGoals = decomposeTask(highLevelTask);

        // Level 2: Action selection
        List<ActionSequence> sequences = subGoals.stream()
            .map(this::selectActions)
            .toList();

        // Level 3: Primitive execution
        return new Plan(sequences);
    }
}
```

### Pattern 2: Modular Plugin Architecture

**Best Practices:**
1. **Core + Plugins** (Mineflayer model)
2. **Plugin Discovery** (SPI/ServiceLoader)
3. **Lifecycle Management** (init/enable/disable)
4. **Dependency Resolution** (plugin dependencies)

**MineWright Status:**
- ✅ Good foundation with `ActionPlugin`, `PluginManager`
- ⚠️ Missing plugin dependencies
- ⚠️ Missing plugin lifecycle hooks

**Enhancement:**
```java
public interface ActionPlugin {
    String getName();
    String getVersion();
    List<String> getDependencies();  // NEW

    void onEnable();  // NEW
    void onDisable(); // NEW
}

public class PluginManager {
    private void resolveDependencies(List<ActionPlugin> plugins) {
        // Topological sort by dependencies
        // Load dependencies first
        // Detect circular dependencies
    }
}
```

### Pattern 3: Async Non-Blocking Execution

**All Projects Avoid Blocking Game Thread:**

| Project | Approach |
|---------|----------|
| Baritone | Multi-threaded path calculation |
| Mineflayer | Promise-based API |
| Voyager | Async LLM calls + event loop |
| MineWright | ✅ `AsyncLLMClient` implemented |

**MineWright Strength:**
- Already has excellent async patterns
- `CompletableFuture` usage throughout
- Resilience4j integration for retries

### Pattern 4: Skill/Action Library

**Common Concept:**
- Store reusable behaviors
- Enable skill composition
- Support skill retrieval

**Implementations:**
| Project | Storage | Retrieval |
|---------|---------|-----------|
| Voyager | Python functions | Semantic search |
| MineDojo | Pre-trained models | Task embedding |
| MineWright | ❌ Missing | - |

**MineWright Enhancement:**
```java
public class SkillLibraryManager {
    private final VectorStore skillIndex;
    private final Map<String, CompiledSkill> skills;

    public void registerSkill(String name, String code, List<String> tags) {
        CompiledSkill skill = compile(code);
        skills.put(name, skill);

        // Index for semantic retrieval
        String embedding = embed(code + " " + String.join(" ", tags));
        skillIndex.add(name, embedding, tags);
    }

    public List<CompiledSkill> findSkills(String query) {
        return skillIndex.search(query, topK=5).stream()
            .map(result -> skills.get(result.id()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
```

### Pattern 5: Multi-Agent Coordination

**Approaches:**
| Project | Coordination Method |
|---------|---------------------|
| Malmo | Chat-based messaging |
| MineDojo | Centralized task distribution |
| MineWright | ✅ `AgentCommunicationBus` |

**MineWright Status:**
- ✅ Good communication bus foundation
- ⚠️ Missing task allocation algorithms
- ⚠️ Missing conflict resolution

**Enhancement:**
```java
public class MultiAgentCoordinator {
    private final Map<String, AgentRole> agentRoles;

    public TaskAssignment assignTasks(List<Task> tasks, List<ForemanEntity> agents) {
        // Hungarian algorithm for optimal assignment
        double[][] costMatrix = calculateCostMatrix(tasks, agents);
        int[] assignment = hungarianAlgorithm(costMatrix);

        return buildAssignment(agents, tasks, assignment);
    }

    private double calculateCost(Task task, ForemanEntity agent) {
        // Consider: distance, capability, current workload
        double distance = agent.position().distTo(task.location());
        double capability = agent.getRole().capabilityFor(task);
        double workload = agent.getCurrentWorkload();

        return distance / (capability + 0.1) * (1 + workload);
    }
}
```

---

## Actionable Recommendations for MineWright

### Priority Matrix

| Priority | Feature | Impact | Effort | Projects |
|----------|---------|--------|--------|----------|
| 🔴 P0 | Enhanced A* Pathfinding | Critical | High | Baritone, Mineflayer |
| 🔴 P0 | Skill Library System | Critical | Medium | Voyager |
| 🟠 P1 | Visual Observations | High | Medium | STEVE, MineDojo |
| 🟠 P1 | Multi-Agent Task Allocation | High | Medium | Malmo, MineDojo |
| 🟠 P1 | Hierarchical Planning | High | Medium | All projects |
| 🟡 P2 | World Model Prediction | Medium | High | DreamerV3 |
| 🟡 P2 | Knowledge Base Integration | Medium | Medium | MineDojo |
| 🟡 P2 | Curriculum Learning | Medium | Low | MineRL, Voyager |
| 🟢 P3 | VLA Model Integration | Medium | Very High | STEVE series |

---

### Recommendation 1: Implement Enhanced A* Pathfinding (P0)

**Inspired By:** Baritone, mineflayer-pathfinder

**Implementation Plan:**

1. **Create Navigation Package**
```
com.minewright.navigation/
├── pathfinding/
│   ├── AStarPathfinder.java
│   ├── Path.java
│   ├── PathNode.java
│   └── PathCostCalculator.java
├── goals/
│   ├── NavigationGoal.java
│   ├── GoalNear.java
│   ├── GoalBlock.java
│   └── GoalComposite.java
├── movements/
│   ├── Movement.java
│   ├── MovementCost.java
│   └── MovementExecutor.java
└── world/
    ├── ChunkCache.java
    └── WorldSnapshot.java
```

2. **Key Features to Implement**
   - [ ] Hierarchical path calculation (macro + micro)
   - [ ] Chunk-based caching
   - [ ] Configurable movement costs
   - [ ] Dynamic replanning on obstacles
   - [ ] Block breaking/placing in paths
   - [ ] Entity avoidance

3. **Integration Point**
```java
// Replace current PathfindAction
public class SmartPathfindAction extends BaseAction {
    private final AStarPathfinder pathfinder;
    private final NavigationGoal goal;
    private Path currentPath;

    @Override
    public void tick() {
        if (currentPath == null || currentPath.isBlocked()) {
            currentPath = pathfinder.calculatePath(steve.getPosition(), goal);
        }

        Movement next = currentPath.nextMovement();
        movementExecutor.execute(steve, next);
    }
}
```

---

### Recommendation 2: Build Skill Library System (P0)

**Inspired By:** Voyager

**Implementation Plan:**

1. **Create Skill Package**
```
com.minewright.skills/
├── SkillLibrary.java
├── ExecutableSkill.java
├── SkillCompiler.java
├── SkillVerifier.java
└── SkillRepository.java
```

2. **Skill Storage Format**
```java
public class ExecutableSkill {
    private final String name;
    private final String description;
    private final String code;  // Groovy/JS for execution
    private final List<String> prerequisites;
    private final List<String> tags;

    public ActionResult execute(TaskContext context) {
        // Execute compiled code
        // Verify postconditions
        // Return result
    }

    public boolean canExecute(TaskContext context) {
        // Check prerequisites
        // Check available resources
    }
}
```

3. **LLM Integration for Skill Generation**
```java
public class SkillGenerator {
    public ExecutableSkill generateSkill(String task, TaskContext context) {
        String prompt = buildSkillPrompt(task, context);
        String code = llmClient.generate(prompt);

        ExecutableSkill skill = compiler.compile(code);
        if (verifier.verify(skill, context)) {
            skillLibrary.save(skill);
            return skill;
        } else {
            return refineSkill(skill, context);
        }
    }
}
```

---

### Recommendation 3: Add Visual Observations (P1)

**Inspired By:** STEVE series, MineDojo

**Implementation Plan:**

1. **Extend Observation System**
```java
public class MultimodalObserver {
    public Observation observe(Level level, ForemanEntity steve, PoseStack pose) {
        return Observation.builder()
            .withText(buildTextObservation(level, steve))
            .withVisual(captureVisualContext(pose))
            .withEntities(detectEntities(level, steve))
            .withTerrain(analyzeTerrain(level, steve))
            .build();
    }

    private VisualContext captureVisualContext(PoseStack pose) {
        // Take screenshot
        // Detect key features
        // Encode for LLM
    }
}
```

2. **Enhance PromptBuilder**
```java
public class EnhancedPromptBuilder extends PromptBuilder {
    public String buildWithVisuals(String command, Observation obs) {
        return String.format("""
            %s

            VISUAL OBSERVATION:
            Entities nearby: %s
            Visible blocks: %s
            Terrain type: %s
            Time of day: %s

            USER COMMAND:
            %s
            """,
            buildSystemPrompt(),
            obs.getEntities(),
            obs.getVisibleBlocks(),
            obs.getTerrainType(),
            obs.getTimeOfDay(),
            command
        );
    }
}
```

---

### Recommendation 4: Multi-Agent Task Allocation (P1)

**Inspired By:** Malmo, MineDojo

**Implementation Plan:**

1. **Task Allocation Algorithm**
```java
public class TaskAllocator {
    public Map<ForemanEntity, List<Task>> allocate(
        List<Task> tasks,
        List<ForemanEntity> agents
    ) {
        // Calculate cost matrix
        double[][] costs = new double[tasks.size()][agents.size()];

        for (int i = 0; i < tasks.size(); i++) {
            for (int j = 0; j < agents.size(); j++) {
                costs[i][j] = calculateCost(tasks.get(i), agents.get(j));
            }
        }

        // Hungarian algorithm for optimal assignment
        int[] assignment = HungarianAlgorithm.allocate(costs);

        return buildAssignment(tasks, agents, assignment);
    }

    private double calculateCost(Task task, ForemanEntity agent) {
        double distance = agent.position().distTo(task.location());
        double capability = agent.getRole().capabilityScore(task);
        double workload = agent.getCurrentTaskCount();

        return (distance * 0.4) + (1.0 / capability * 0.4) + (workload * 0.2);
    }
}
```

2. **Role-Based Capability System**
```java
public enum AgentRole {
    BUILDER(Map.of("build", 1.0, "place_block", 1.0, "mine", 0.5)),
    MINER(Map.of("mine", 1.0, "gather", 1.0, "build", 0.3)),
    GENERALIST(Map.of());  // All capabilities at 0.7

    private final Map<String, Double> capabilities;

    public double capabilityScore(Task task) {
        return capabilities.getOrDefault(task.getType(), 0.5);
    }
}
```

---

### Recommendation 5: Hierarchical Planning System (P1)

**Inspired By:** All projects

**Implementation Plan:**

1. **Create Planning Package**
```
com.minewright.planning/
├── HierarchicalPlanner.java
├── TaskDecomposer.java
├── SubGoal.java
├── ActionSequence.java
└── Plan.java
```

2. **Three-Level Planning**
```java
public class HierarchicalPlanner {
    public Plan plan(Task highLevelTask) {
        // Level 1: Task → Sub-goals
        List<SubGoal> subGoals = decomposeIntoSubGoals(highLevelTask);

        // Level 2: Sub-goals → Action sequences
        List<ActionSequence> sequences = subGoals.stream()
            .map(this::planSubGoal)
            .toList();

        // Level 3: Action sequences → Primitive actions
        return new Plan(sequences);
    }

    private List<SubGoal> decomposeIntoSubGoals(Task task) {
        // Use LLM for decomposition
        String prompt = String.format("""
            Decompose this task into 3-5 sub-goals:
            Task: %s
            Context: %s

            Output as JSON list of sub-goals.
            """, task.getDescription(), task.getContext());

        return parseSubGoals(llmClient.generate(prompt));
    }
}
```

---

### Recommendation 6: Knowledge Base Integration (P2)

**Inspired By:** MineDojo

**Implementation Plan:**

1. **Create Knowledge Package**
```
com.minewright.knowledge/
├── MinecraftKnowledgeBase.java
├── CraftingRecipe.java
├── WikiKnowledge.java
└── KnowledgeRetriever.java
```

2. **Static Knowledge Base**
```java
public class MinecraftKnowledgeBase {
    private final Map<String, CraftingRecipe> craftingRecipes;
    private final Map<String, List<String>> taskDecompositions;

    public MinecraftKnowledgeBase() {
        // Load from JSON
        loadCraftingRecipes();
        loadTaskDecompositions();
    }

    public Optional<CraftingRecipe> getRecipe(String item) {
        return Optional.ofNullable(craftingRecipes.get(item));
    }

    public List<String> decomposeTask(String task) {
        // Return cached decomposition or empty
        return taskDecompositions.getOrDefault(task, List.of());
    }
}
```

3. **LLM-Augmented Knowledge**
```java
public class KnowledgeRetriever {
    public String queryKnowledge(String question) {
        // Search local knowledge base first
        String local = knowledgeBase.search(question);
        if (local != null) return local;

        // Fall back to LLM with knowledge base context
        String prompt = String.format("""
            Given this Minecraft knowledge:
            %s

            Answer this question:
            %s
            """, knowledgeBase.summarize(), question);

        return llmClient.generate(prompt);
    }
}
```

---

### Recommendation 7: World Model Prediction (P2)

**Inspired By:** DreamerV3

**Implementation Plan:**

1. **Predictive Modeling**
```java
public class WorldModel {
    public PredictionResult predict(
        WorldState current,
        Action action,
        int stepsAhead
    ) {
        WorldState simulated = current;
        double cumulativeReward = 0;

        for (int i = 0; i < stepsAhead; i++) {
            // Predict next state
            simulated = simulateTransition(simulated, action);

            // Predict reward
            cumulativeReward += predictReward(simulated);
        }

        return new PredictionResult(simulated, cumulativeReward);
    }

    private WorldState simulateTransition(WorldState state, Action action) {
        // Simplified physics simulation
        // Block changes, entity movements, etc.
    }
}
```

2. **Planning with Imagination**
```java
public class ImaginationPlanner {
    public Action selectBestAction(
        WorldState state,
        List<Action> candidates
    ) {
        return candidates.stream()
            .max(Comparator.comparing(a -> {
                PredictionResult pred = worldModel.predict(state, a, 10);
                return pred.getExpectedReward();
            }))
            .orElse(null);
    }
}
```

---

### Recommendation 8: Curriculum Learning (P2)

**Inspired By:** MineRL, Voyager

**Implementation Plan:**

1. **Curriculum Definition**
```java
public class Curriculum {
    private final List<TaskTier> tiers;

    public Curriculum() {
        tiers = List.of(
            new TaskTier(1, "Basic", List.of(
                "get_wood", "craft_planks", "craft_sticks"
            )),
            new TaskTier(2, "Stone Age", List.of(
                "craft_stone_pickaxe", "get_cobblestone", "craft_furnace"
            )),
            new TaskTier(3, "Iron Age", List.of(
                "get_iron_ore", "smelt_iron", "craft_iron_pickaxe"
            )),
            new TaskTier(4, "Diamond", List.of(
                "find_diamonds", "mine_diamonds"
            ))
        );
    }

    public Task selectNextTask(AgentState state) {
        for (TaskTier tier : tiers) {
            if (!state.hasCompletedTier(tier.getId())) {
                return tier.selectNextUncompletedTask(state);
            }
        }
        return null;
    }
}
```

2. **Progressive Difficulty**
```java
public class AdaptiveCurriculum extends Curriculum {
    public Task adaptToAgent(AgentState state) {
        double successRate = state.getRecentSuccessRate();

        if (successRate > 0.9) {
            // Increase difficulty
            return getNextTierTask(state);
        } else if (successRate < 0.5) {
            // Decrease difficulty
            return getPreviousTierTask(state);
        } else {
            // Same tier
            return getCurrentTierTask(state);
        }
    }
}
```

---

### Recommendation 9: Plugin Dependencies (P2)

**Inspired By:** Mineflayer plugin system

**Implementation Plan:**

1. **Enhanced Plugin Interface**
```java
public interface ActionPlugin {
    String getName();
    String getVersion();
    List<String> getDependencies();  // NEW

    void onLoad();      // Called when loaded
    void onEnable();    // Called when enabled
    void onDisable();   // Called when disabled
}

public class PluginDependency {
    private final String pluginName;
    private final String minVersion;
    private final boolean required;

    public boolean isSatisfiedBy(ActionPlugin plugin) {
        return plugin.getName().equals(pluginName) &&
               versionCompare(plugin.getVersion(), minVersion) >= 0;
    }
}
```

2. **Dependency Resolution**
```java
public class PluginManager {
    private void resolveDependencies(List<ActionPlugin> plugins) {
        // Topological sort
        List<ActionPlugin> sorted = new ArrayList<>();
        Set<ActionPlugin> visited = new HashSet<>();

        for (ActionPlugin plugin : plugins) {
            if (!visited.contains(plugin)) {
                visit(plugin, plugins, visited, sorted);
            }
        }

        // Check for missing dependencies
        for (ActionPlugin plugin : plugins) {
            for (String dep : plugin.getDependencies()) {
                if (!hasPlugin(dep)) {
                    throw new MissingDependencyException(dep, plugin.getName());
                }
            }
        }

        return sorted;
    }

    private void visit(ActionPlugin plugin, List<ActionPlugin> all,
                      Set<ActionPlugin> visited, List<ActionPlugin> sorted) {
        // Depth-first search for topological sort
    }
}
```

---

## Appendix: Additional Resources

### GitHub Repositories

| Project | URL | Stars | Language |
|---------|-----|-------|----------|
| Baritone | https://github.com/cabaletta/baritone | 7.5k+ | Java |
| Mineflayer | https://github.com/PrismarineJS/mineflayer | 5k+ | JavaScript |
| Malmo | https://github.com/Microsoft/Malmo | 3k+ | Java/Python |
| Voyager | https://github.com/MineDojo/Voyager | 4k+ | Python |
| MineDojo | https://github.com/MineDojo/MineDojo | 3k+ | Python |
| MineRL | https://github.com/MineRL/MineRL | 2k+ | Python |

### Key Papers

1. **Voyager:** Wang et al. "Voyager: An Open-Ended Embodied Agent with Large Language Models" (2023)
2. **DreamerV3:** Hafner et al. "Mastering Diverse Domains through World Models" (Nature, 2024)
3. **STEVE-1:** Lifshitz et al. "Text2Behavior: Playing Minecraft with Videos" (NeurIPS 2023)
4. **MineDojo:** Fan et al. "MineDojo: Building Open-Ended Embodied Agents with Internet-Scale Knowledge" (NeurIPS 2022)

### Online Resources

- **Baritone Documentation:** https://github.com/cabaletta/baritone/wiki
- **Mineflayer Docs:** https://mineflayer.js.org/
- **Project Malmo:** https://www.microsoft.com/en-us/research/project/project-malmo/
- **MineRL Competition:** https://www.microsoft.com/en-us/research/project/project-malmo/competitions/

---

## Sources

### Baritone
- [Baritone: Minecraft Pathfinding Automation Solution](https://m.blog.csdn.net/gitblog_01087/article/details/157828480)
- [Baritone Path Node Generation & Priority Queue Optimization](https://m.blog.csdn.net/gitblog_00430/article/details/154051501)
- [Baritone Intelligent Pathfinding System](https://m.blog.csdn.net/gitblog_00799/article/details/155385231)
- [Baritone Smart Jump Mechanics & Landing Calculation](https://m.blog.csdn.net/gitblog_00610/article/details/154051638)

### Mineflayer
- [Mineflayer Pathfinder: Autonomous Navigation Tool](https://m.blog.csdn.net/gitblog_00499/article/details/142248077)
- [Mineflayer Pathfinding Project Tutorial](https://m.blog.csdn.net/gitblog_00336/article/details/142247147)
- [Mineflayer Block Finding Algorithm: 5 Techniques](https://m.blog.csdn.net/gitblog_01165/article/details/151730743)
- [Mineflayer Core API Guide](https://m.blog.csdn.net/gitblog_00366/article/details/151305673)

### Project Malmo
- [Project Malmo Overview](https://m.blog.csdn.net/gitblog_00129/article/details/140981707)
- [Microsoft's Minecraft AI Testing](https://www.pkvs.com/news/0/2516.html)
- [Minecraft Malmo: AI Research Virtual Laboratory](https://m.blog.csdn.net/gitblog_00146/article/details/155405104)
- [Minecraft Malmo AI Experiment Platform](https://blog.csdn.net/weixin_42351520/article/details/153842391)

### Voyager
- [Voyager: Open-Ended Embodied Agent with LLM](https://github.com/MineDojo/Voyager)
- [Voyager LLM-Driven Continuous Learning](https://m.blog.csdn.net/gitblog_00533/article/details/151387307)
- [MineDojo/Voyager GitHub](https://github.com/MineDojo/Voyager)

### MineRL & Reinforcement Learning
- [Minecraft Reinforcement Learning: Building a Diamond-Finding Robot](https://blog.csdn.net/xuner1213/article/details/149340042)
- [AI in Minecraft: Can It Unlock the Diamond Mystery?](http://m.finance.itbear.com.cn/html/2025-05/168590.html)
- [DeepMind's DreamerV3: Nature-Featured Breakthrough](https://blog.csdn.net/weixin_29102619/article/details/157823679)
- [NetEase AI Lab: Athena AI First to Mine Diamonds](https://baijiahao.baidu.com/s?id=1837214477752543912)

### STEVE Series & Vision-Language-Action
- [STEVE-1: Generative Model for Text-to-Behavior](https://m.blog.csdn.net/m0_59235699/article/details/145888309)
- [Game-TARS: Pretrained Foundation Models](https://arxiv.org/html/2510.23691v1)
- [Towards Generalist Multimodal Minecraft Agents](https://arxiv.org/html/2506.10357v1)
- [JARVIS-VLA: Vision-Language Models for Visual Games](https://arxiv.org/html/2503.16365v1)

### General Minecraft AI
- [YuvDwi/Steve: Autonomous AI Agent for Minecraft](https://github.com/YuvDwi/Steve)
- [Mindcraft Project: LLM-Driven Minecraft AI](https://blog.csdn.net/gitblog_00277/article/details/154764935)
- [Plan4MC: Directed Acyclic Graph Planning + RL Execution](https://m.blog.csdn.net/weixin_36954807/article/details/146168163)
- [MCU: Generative Open-World Benchmark](https://m.163.com/news/article/JVF36M550511AQHO.html)

---

**Document Version:** 1.0
**Last Updated:** 2025-02-27
**Next Review:** 2025-03-31
**Status:** Ready for Implementation Planning
