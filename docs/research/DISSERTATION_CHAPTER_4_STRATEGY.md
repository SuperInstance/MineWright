# Chapter 4: Strategy and Simulation Games

## Dissertations on Game AI Automation Techniques (Non-LLM Approaches)

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [City Builders & Management](#2-city-builders--management)
3. [Turn-Based Strategy](#3-turn-based-strategy)
4. [Grand Strategy](#4-grand-strategy)
5. [Key Techniques](#5-key-techniques)
6. [Simulation Techniques](#6-simulation-techniques)
7. [Extractable Patterns for Minecraft](#7-extractable-patterns-for-minecraft)
8. [Case Studies](#8-case-studies)
9. [Conclusion](#9-conclusion)

---

## 1. Introduction

Strategy and simulation games represent a unique domain in game AI where complexity emerges from carefully designed systems rather than advanced machine learning. From 1990 to 2025, these games have demonstrated that sophisticated AI behavior can be achieved through:

- **Utility-based decision systems** that score actions based on weighted factors
- **Agent-based modeling** where individual entities follow simple rules
- **Emergent behavior** that arises from interconnected systems
- **Resource flow optimization** using classical algorithms
- **Perfect information design** that eliminates randomness

Unlike action games requiring real-time reflexes, or role-playing games needing narrative understanding, strategy games excel at creating deep, meaningful decisions through deterministic systems. This chapter explores the evolution of these techniques and their applicability to Minecraft automation.

---

## 2. City Builders & Management

### 2.1 SimCity: Traffic Simulation and Demand Systems (1989-2013)

**Historical Context**: Will Wright's SimCity pioneered the city-building genre, demonstrating that complex urban dynamics could emerge from simple rules without sophisticated AI.

#### Core Systems

**Traffic Simulation (SimCity 2000, 1993)**

SimCity's traffic system used a **cellular automaton** approach inspired by Conway's Game of Life. Each road cell had a state representing congestion, updated based on:

```
Congestion_new = f(Incoming_traffic, Road_capacity, Connected_development)
```

Key innovations:
- **Trip generation**: Each building type generated trips based on population/jobs
- **Route assignment**: Simple Dijkstra-like pathfinding through road network
- **Congestion propagation**: Traffic jams spread to neighboring cells

**Demand System (SimCity 4, 2003)**

The RCI (Residential-Commercial-Industrial) demand system used weighted feedback loops:

```
R_demand = w1 * (Jobs - Population) + w2 * (Pollution_level) + w3 * (Tax_rate)
C_demand = w1 * (Population - Jobs) + w2 * (Customer_spending) + w3 * (Crime_rate)
I_demand = w1 * (Materials_available) + w2 * (Freight_capacity) + w3 * (Tax_rate)
```

Each zone type evaluated desirability through utility scoring, creating natural supply-demand cycles.

**Legacy**: SimCity proved that urban complexity could emerge from **100 interconnected variables** without any agents making "intelligent" decisions—just feedback loops and cellular automata.

### 2.2 Cities: Skylines - Agent-Based Pathfinding (2015-2023)

**Historical Context**: Colossal Order's Cities: Skylines revolutionized the genre by moving from statistical traffic models to true agent-based simulation.

#### Agent-Based Pathfinding System

**Cities: Skylines I (2015)**

Used proximity-based pathfinding:
- Agents selected shortest Euclidean distance routes
- Ignored road network connectivity quality
- Resulted in "herding" behavior on same roads

**Cities: Skylines II (2023)**: Multi-Factor Cost Calculation

Redesigned pathfinding using weighted cost functions:

```
Path_Cost = w1 * Time + w2 * Comfort + w3 * Money + w4 * Behavior
```

| Factor | Description | Weight |
|--------|-------------|--------|
| **Time** | Actual travel duration | Primary (adults) |
| **Comfort** | Road complexity, intersection density | Secondary |
| **Money** | Fuel costs, tolls, parking fees | Varies by income |
| **Behavior** | Personality traits, event responses | Tertiary |

**Dynamic Route Adjustment**:
- Agents recalculate routes when accidents detected
- Emergency vehicles trigger lane-sharing behavior
- Service vehicles get priority weighting
- Real-time lane changes to avoid incidents

**Technical Implementation**:
- Hierarchical A* with region decomposition
- Cost-based edge weights updated per tick
- Agent decision caching with invalidation on network changes

### 2.3 RimWorld - Story Generator and Social Dynamics (2013-Present)

**Historical Context**: Tynan Sylvester's RimWorld demonstrates how story generation emerges from psychological simulation rather than scripted narrative.

#### AI Storyteller System

Inspired by Left 4 Dead's AI Director, the Storyteller uses **utility-based event selection**:

```
Event_Score = w1 * (Drama_level) + w2 * (Colony_strength) + w3 * (Time_since_crisis)
```

**Three Storyteller Types**:

| Storyteller | Utility Weights | Pattern |
|-------------|----------------|---------|
| **Cassandra Classic** | Progressive difficulty increase | Rising tension |
| **Phoebe Chillax** | Low crisis frequency | Long building phases |
| **Randy Random** | Uniform random selection | Unpredictable chaos |

#### Social Dynamics System

**Needs-Based Psychology**:

Each colonist has 12+ needs tracked simultaneously:
- Physical: Food, rest, comfort
- Mental: Mood, recreation, drug withdrawal
- Social: Talking, room quality, romance

```
Mood = Sum(Individual_need_scores * importance_weights)
Break_threshold = -10 (varies by traits)
```

**Relationship Formation**:

- Opinion score range: -100 to +100
- Daily interaction opportunities
- Traits modify opinion change rates (e.g., "Kind" traits build relationships faster)
- Relationships trigger events: bonding, brawls, marriage, murder

**Emergent Narrative Example**:
```
1. Colonist A ("Neurotic" trait) fails craft → Mood drop
2. Colonist A snaps → Starts brawl with Colonist B
3. Colonist B ("Psychopathic" trait) doesn't forgive → Opinion -40
4. Colonist B plots revenge → Sabotages critical equipment
5. Equipment failure during raid → Colony crisis
6. Player must choose: repair or defend?
```

No "story writer" needed—drama emerges from need thresholds and trait-driven behaviors.

### 2.4 Dwarf Fortress - Legendary Complexity Without AI (2006-Present)

**Historical Context**: Tarn Adams' Dwarf Fortress proves that depth doesn't require AI—just absurdly detailed simulation.

#### Simulation Architecture

**World Generation**:
- Procedurally generates 1000+ years of history
- Tracks: civilizations, wars, births, deaths, artifacts, sites
- Each historical figure has: personality, appearance, relationships, beliefs
- Legend mode allows reading complete history

**Entity Simulation**:
- Each dwarf: 50+ attributes (strength, empathy, creativity, etc.)
- Material properties: density, melting point, edge quality for 200+ materials
- Anatomical combat: 30+ body parts, each with independent damage
- Emotional memory: each traumatic event stored and referenced

**Emergent Complexity Examples**:

1. **Economy from price multipliers**:
   ```
   Item_value = Base_value * Material_quality * Crafter_skill * Decorations
   Trade_deals = Utility(Item_value_needed) - Utility(Item_value_offered)
   ```

2. **Social networks**:
   - Dwarves form friendships based on shared interests
   - Grief cascades when friends die
   - Tantrum spirals can destroy fortresses

3. **Military tactics**:
   - Squad positioning uses formation algorithms
   - Equipment assignment based on skill preferences
   - Training improves attributes through use-based practice

**No AI Needed**: All behavior emerges from simulation rules:
- Pathfinding: A* on 3D grid
- Combat: Material hardness calculations
- Social: Need/opinion thresholds
- Economy: Supply/demand from item availability

### 2.5 Factorio - Production Chain Optimization (2012-Present)

**Historical Context**: Wube Software's Factorio demonstrates mathematical optimization through factory automation.

#### Production Chain Calculations

**Recipe-Based Planning**:

All planning derives from recipe definitions:
```
Recipe: Electronic Circuit (Green)
Inputs: 1 Iron Plate, 1 Copper Wire (×2 copper plates)
Time: 0.5 seconds
Output: 2 Electronic Circuits
```

**Backwards Resource Calculation**:

To achieve target output (e.g., 60 science packs/minute):
```
Required_machines = Target_output / (Recipe_output / Recipe_time)
Input_resources = Required_machines × Recipe_inputs
```

Recursively calculate up raw material tree:
```
Science Packs → Assembler → Circuits → Wires + Plates → Smelters → Ore
```

**Machine Ratio Optimization**:

Common ratios derived from recipe analysis:
| Product | Machine Ratio | Standardized Block |
|---------|---------------|-------------------|
| Red Circuits | 3:2:1 (Wire:Circuit:Assembler) | 6:4:2 |
| Blue Circuits | 16:4:1 | 48:12:3 |
| Processing Units | 20:5:2:1 | Complex |

**Throughput Optimization**:

Belt throughput determines factory scaling:
- Yellow belt: 15 items/sec
- Red belt: 30 items/sec
- Blue belt: 45 items/sec

Optimal bus design:
- Use belt compression (sideloading)
- Balance lane inputs
- Minimize belt crossings
- Group related materials

**Solving with Classical Algorithms**:

According to Factorio Learning Environment research:
- 99% of Factorio can be "solved" with 1980s Operations Research algorithms
- Linear programming for resource allocation
- Network flow for logistics optimization
- Genetic algorithms for layout search
- No machine learning required for optimal factories

---

## 3. Turn-Based Strategy

### 3.1 Civilization Series - Diplomacy and Research Choices (1991-Present)

**Historical Context**: Sid Meier's Civilization series evolved from simple reaction systems to sophisticated agenda-based AI.

#### Diplomatic AI Evolution

**Civilization I-V (1991-2010)**: Hidden Weight Systems

AI leaders used hidden personality weights:
```
War_probability = Base_aggression + (Border_friction × w1) + (Resource_competition × w2) - (Diplomatic_bonus × w3)
```

Player criticism: AI behavior felt random and emotional ("like unstable children").

**Civilization VI (2016)**: Agenda-Based Transparency

Each leader has TWO agendas:

| Agenda Type | Description | Example |
|-------------|-------------|---------|
| **Historical** | Based on real historical traits | Cleopatra: Likes strong militaries |
| **Hidden** | Randomized each game | Secret goal: Prefers high-population civs |

Utility scoring makes diplomacy transparent:
```
Opinion_score = (Agenda_matches × positive_weight) + (Agenda_conflicts × negative_weight) + (Deal_history × memory_weight)
```

Players can discover hidden agendas through espionage or relationship building.

#### Research AI

Technology selection uses **weighted benefit scoring**:

```
Tech_Score = w1 * (Military_benefit) + w2 * (Economic_benefit) + w3 * (Strategic_value) + w4 * (Random_factor)
```

- Military civs weight unit technologies
- Expansionist civs weight movement/harvesting
- Religious civs weight faith-generating techs

**No lookahead**: AI doesn't plan tech trees—reacts to current needs.

### 3.2 X-COM - Tactical AI and Cover Evaluation (1994-2016)

**Historical Context**: Firaxis' XCOM: Enemy Unknown reinvented tactical combat through predictable cover systems.

#### Cover Evaluation System

**Cover Mechanics**:

Cover reduces enemy accuracy based on coverage angle:
- Full cover: 90% aim reduction
- Half cover: 45% aim reduction
- Flanking: Calculated at any angle; closer to flank = less benefit

**AI Cover Selection Algorithm**:

```python
def evaluate_cover(agent, position):
    score = 0

    # Check threat visibility
    for enemy in visible_enemies:
        if has_line_of_sight(position, enemy):
            exposed_threats += enemy.threat_level

    # Evaluate cover quality
    cover_quality = get_cover_rating(position)
    score += cover_quality * 100

    # Penalize exposure
    score -= exposed_threats * 50

    # Consider offensive positioning
    flanking_bonus = count_flankable_enemies(position) * 20
    score += flanking_bonus

    return score
```

**Free Movement on Detection**:

When players first encounter enemies:
- AI gets "activation move" to find cover
- Prevents player advantage from discovering enemies
- Balances asymmetry between player initiative and AI positioning

**Tactical Behavior**:

1. **Overwatch**: AI enters overwatch when no good shots available
2. **Flanking**: AI tries to move outside player cover arcs
3. **Grenades**: Used when player groups behind low cover
4. **Retreat**: Wounded units seek low-visibility positions

### 3.3 Into the Breach - Perfect Information AI (2018)

**Historical Context**: Subset Games' Into the Breach eliminates randomness through telegraphed enemy actions.

#### Perfect Information Design

**Core Innovation**: All enemy attacks explicitly shown before execution:
- Attack type and damage
- Target location (telegraphed)
- Attack direction

This creates a **deterministic puzzle** rather than probabilistic combat.

**AI Behavior**:

The "AI" is essentially a state machine:
```
1. Player sees all enemy intentions
2. Player plans actions (move, attack, push)
3. Player actions modify enemy intentions
4. Enemies execute telegraphed attacks
5. New threats revealed → Repeat
```

**Strategic Depth from Constraints**:

Strategy comes from manipulating known behaviors:
- Push enemy into other enemy's attack path
- Take damage to protect building
- Leave enemy alive to block reinforcement spawn
- Use environment to redirect attacks

**Designer Quote** (Matthew Davis, GDC 2019):
> "When we decided we had to show what every enemy was doing every single turn, it became clear how bad that nightmare would be. But it also revealed the game's true potential."

**Impact**: Proved that **complete transparency** creates deeper strategy than hidden information.

---

## 4. Grand Strategy

### 4.1 Europa Universalis - Complex Decision Weights (2000-Present)

**Historical Context**: Paradox Interactive's grand strategy games use extensive weighted decision tables.

#### AI Decision Architecture

**Multi-Layered Weight System**:

```
Action_Score = (Base_weight × Personality_modifier) + (Context_modifier) + (Random_variance)
```

**Diplomatic Decisions**:

AI evaluates alliances using weighted factors:
```python
def evaluate_alliance(target_country):
    score = 0

    # Historical friendship
    score += historical_relations * 0.3

    # Strategic value
    score += (target_country.military_strength / my_strength) * 0.2

    # Threat assessment
    if has_shared_enemy(target_country):
        score += 0.4

    # Distance penalty
    score -= distance_to_capital * 0.01

    return score > alliance_threshold
```

**Military AI**:

Army movement uses utility scoring:
```
Province_desirability =
    w1 * (Strategic_value)
    + w2 * (Supply_limit)
    - w3 * (Enemy_presence)
    + w4 * (Mission_priority)
```

**No Machine Learning**: All behavior from manually tuned weights and conditionals.

### 4.2 Crusader Kings - Character AI and Plotting (2004-2020)

**Historical Context**: Paradox's Crusader Kings simulates medieval dynasties through character-driven AI.

#### Hidden Personality System

Each character has **5 hidden AI traits** determining behavior:

| Hidden Trait | Low Value Behavior | High Value Behavior |
|--------------|-------------------|---------------------|
| **Rationality** | Foolish attacks | Calculated decisions |
| **Fanaticism** | Secular focus | Religious wars |
| **Greed** | Ideological decisions | Easily bribed |
| **Honor** | Schemes freely | Loyal to allies |
| **Ambition** | Content with lands | Expansionist |

**Plotting System**:

Characters can join schemes against each other:
```
Plot_recruitment_score =
    w1 * (Target_relation) [negative = willing]
    + w2 * (Ambition_level)
    + w3 * (Plot_benefit)
    - w4 * (Target_power) [fear penalty]
```

- High honor → won't plot against friends
- High ambition → eager for power
- Low rationality → joins risky plots

**Opinion System**:

Opinion range: -100 to +100
```
Opinion_change = Base_change * (1 + Personality_modifier)
```

Events trigger opinion shifts:
- Marriage alliance: +20 to +40
- Executed family member: -50 to -100
- Religious difference: -10 to -30

**Emergent Narrative**:
No scripted stories—plots, wars, and marriages emerge from trait-driven decisions.

---

## 5. Key Techniques

### 5.1 Utility Scoring Systems

**Definition**: Utility AI assigns numerical scores to actions based on weighted game state factors.

#### Architecture

```python
def calculate_utility(agent, action):
    score = 0

    for factor in action.considerations:
        normalized_value = normalize(factor.get_value(agent), factor.min, factor.max)
        score += normalized_value * factor.weight

    return score

def select_action(agent, available_actions):
    scored_actions = [(action, calculate_utility(agent, action)) for action in available_actions]
    return max(scored_actions, key=lambda x: x[1])[0]
```

#### Common Utility Functions

| Function Type | Formula | Use Case |
|---------------|---------|----------|
| **Linear** | y = ax + b | Distance-based scaling |
| **Exponential** | y = 1 - e^(-x) | Urgency (health, resources) |
| **Sigmoid** | y = 1 / (1 + e^(-x)) | Smooth transitions |
| **Piecewise Constant** | y = step(x) | Binary thresholds |

#### Example: Combat AI

```
Attack_Score =
    w1 * normalized(Damage_to_enemy) +
    w2 * normalized(1 / Hit_chance) +
    w3 * normalized(Ammo_remaining) +
    w4 * normalized(Enemy_threat_level)

Retreat_Score =
    w1 * normalized(1 / Health) +
    w2 * normalized(Distance_to_safety) +
    w3 * normalized(Enemy_proximity)
```

Select max(Attack_Score, Retreat_Score) - no behavior trees needed.

**Advantages**:
- Easy to extend: add new considerations
- Transparent: debuggable scores
- Nuanced behavior: subtle weighting changes
- Design-friendly: describeable in natural language

**Disadvantages**:
- Manual tuning required
- Can be computationally expensive
- Harder to trace decision chains

### 5.2 Weighted Random Selection

**Definition**: Select elements from a collection based on probability weights.

#### Algorithm Implementation

**Method 1: Linear Scan (O(n))**

```python
def weighted_random_linearscan(items, weights):
    total = sum(weights)
    random_value = random.uniform(0, total)

    for item, weight in zip(items, weights):
        random_value -= weight
        if random_value <= 0:
            return item
```

**Method 2: Prefix Sum + Binary Search (O(log n))**

```python
class WeightedSelector:
    def __init__(self, items, weights):
        self.items = items
        self.prefix_sums = []
        cumulative = 0
        for weight in weights:
            cumulative += weight
            self.prefix_sums.append(cumulative)
        self.total = cumulative

    def select(self):
        random_value = random.uniform(0, self.total)

        # Binary search
        left, right = 0, len(self.prefix_sums) - 1
        while left < right:
            mid = (left + right) // 2
            if self.prefix_sums[mid] < random_value:
                left = mid + 1
            else:
                right = mid

        return self.items[left]
```

#### Game Applications

**Monster Spawning**:

| Monster | Weight | Probability |
|---------|--------|-------------|
| Lion | 5 | 11.1% |
| Tiger | 8 | 17.8% |
| Snake | 12 | 26.7% |
| Crocodile | 20 | 44.4% |

**Gacha/Loot Systems**:

```python
def select_loot(rarity_weights):
    # 50% common, 40% uncommon, 9% rare, 1% legendary
    rarity = weighted_random(["common", "uncommon", "rare", "legendary"], [50, 40, 9, 1])

    if rarity == "legendary":
        # Roll legendary item table
        return weighted_random(legendary_items, legendary_weights)
    # ... etc
```

**AI Behavior Selection**:

```python
def decide_action(agent):
    # Weight by current priorities
    attack_weight = max(0, 100 - agent.health)  # More aggressive when healthy
    flee_weight = agent.health * 2  # Flee when damaged
    heal_weight = (100 - agent.health) * 3  # Heal when damaged

    return weighted_random(["attack", "flee", "heal"], [attack_weight, flee_weight, heal_weight])
```

### 5.3 Priority Queues

**Definition**: Data structure for efficiently retrieving highest-priority elements.

#### Applications

**Task Scheduling**:

```python
import heapq

class TaskScheduler:
    def __init__(self):
        self.tasks = []  # (priority, task) tuples

    def schedule(self, task, priority):
        heapq.heappush(self.tasks, (priority, task))

    def get_next(self):
        if self.tasks:
            return heapq.heappop(self.tasks)[1]
        return None

# Example: Production queue
scheduler.schedule("worker", priority=10)
scheduler.schedule("soldier", priority=5)  # Higher priority
# Next task: soldier
```

**Event Processing**:

```python
class EventQueue:
    def __init__(self):
        self.events = []  # (time, event) tuples

    def schedule_event(self, time, event):
        heapq.heappush(self.events, (time, event))

    def process_until(self, current_time):
        while self.events and self.events[0][0] <= current_time:
            _, event = heapq.heappop(self.events)
            event.execute()
```

**Pathfinding Optimization**:

Dijkstra and A* use priority queues for node expansion:
```python
def a_star_search(start, goal):
    open_set = [(0, start)]  # (f_score, node)
    came_from = {}
    g_score = {start: 0}

    while open_set:
        _, current = heapq.heappop(open_set)

        if current == goal:
            return reconstruct_path(came_from, current)

        for neighbor in get_neighbors(current):
            tentative_g = g_score[current] + distance(current, neighbor)
            if tentative_g < g_score.get(neighbor, infinity):
                came_from[neighbor] = current
                g_score[neighbor] = tentative_g
                f_score = tentative_g + heuristic(neighbor, goal)
                heapq.heappush(open_set, (f_score, neighbor))
```

### 5.4 Resource Flow Optimization

**Definition**: Optimizing the flow of resources through networks using classical algorithms.

#### Network Flow Algorithms

**Maximum Flow Problem (Ford-Fulkerson)**:

```python
def max_flow(graph, source, sink):
    parent = {}
    max_flow = 0

    while bfs_find_path(graph, source, sink, parent):
        # Find minimum residual capacity along path
        path_flow = min_capacity(graph, parent, source, sink)

        # Update residual capacities
        update_residual_graph(graph, parent, path_flow)

        max_flow += path_flow

    return max_flow
```

#### Game Applications

**Supply Chain Optimization** (Factorio-style):

```python
def optimize_production_chain(target_items, recipes):
    # Build recipe graph
    graph = build_recipe_graph(recipes)

    # Calculate required inputs for each target
    requirements = {}
    for item, quantity in target_items:
        requirements[item] = requirements.get(item, 0) + quantity
        propagate_requirements(graph, item, quantity, requirements)

    # Optimize production
    production_plan = {}
    for item, needed_qty in requirements.items():
        if item in recipes:
            recipe = recipes[item]
            machines = ceil(needed_qty / recipe.output_per_machine)
            production_plan[item] = machines

    return production_plan
```

**Logistics Routing**:

```python
def optimize_transportation(sources, destinations, demands):
    # Create bipartite graph
    graph = create_transport_graph(sources, destinations)

    # Solve assignment problem using Hungarian algorithm
    # or minimum-cost maximum-flow
    assignment = solve_min_cost_flow(graph, demands)

    return assignment
```

### 5.5 Threat Assessment Matrices

**Definition**: Quantifying danger levels from multiple threat sources.

#### Basic Threat Matrix

```python
def calculate_threat_matrix(agent, enemies):
    threats = []

    for enemy in enemies:
        threat_score = 0

        # Distance threat (closer = more threatening)
        distance = get_distance(agent, enemy)
        threat_score += (1 / distance) * 100

        # Combat strength
        threat_score += enemy.attack_power * 10

        # Current targeting
        if enemy.target == agent:
            threat_score += 50

        # Weapon type
        if enemy.weapon.type == "ranged" and distance < 20:
            threat_score += 30

        threats.append((enemy, threat_score))

    return sorted(threats, key=lambda x: x[1], reverse=True)
```

#### Advanced: Multi-Parameter Assessment

```python
def advanced_threat_assessment(agent, enemies, terrain):
    threat_matrix = np.zeros((len(enemies), 4))  # 4 threat dimensions

    for i, enemy in enumerate(enemies):
        # Dimension 1: Firepower
        threat_matrix[i][0] = enemy.damage / enemy.attack_cooldown

        # Dimension 2: Mobility
        threat_matrix[i][1] = enemy.movement_speed / get_distance(agent, enemy)

        # Dimension 3: Tactical advantage
        if has_flanking_position(enemy, agent, terrain):
            threat_matrix[i][2] = 1.5  # Flanking bonus
        else:
            threat_matrix[i][2] = 1.0

        # Dimension 4: Urgency
        if enemy.target == agent and enemy.can_attack_next_turn:
            threat_matrix[i][3] = 2.0  # Immediate threat

    # Weighted combination
    weights = [0.3, 0.2, 0.25, 0.25]
    total_threat = np.dot(threat_matrix, weights)

    return total_threat
```

#### Applications

**Combat Target Selection**:
```python
def select_target(agent, enemies):
    threats = calculate_threat_matrix(agent, enemies)

    # Target highest threat that's killable
    for enemy, threat in threats:
        if estimated_attacks_to_kill(agent, enemy) <= 3:
            return enemy

    # Otherwise, target weakest
    return min(enemies, key=lambda e: e.health)
```

**Positional Evaluation**:
```python
def evaluate_position_safety(agent, position, enemies):
    threat_score = 0

    for enemy in enemies:
        if can_see(enemy, position):
            # Higher threat if enemy can reach position quickly
            threat_score += enemy.threat_level / (distance_to(enemy, position) + 1)

    return -threat_score  # Lower threat = better position
```

---

## 6. Simulation Techniques

### 6.1 Agent-Based Modeling

**Definition**: Modeling systems as collections of autonomous agents following simple rules.

#### Core Principles

1. **Decentralization**: No central controller
2. **Autonomy**: Agents make independent decisions
3. **Emergence**: System-level patterns from individual behavior

#### Implementation Pattern

```python
class Agent:
    def __init__(self, position, attributes):
        self.position = position
        self.attributes = attributes
        self.state = "idle"

    def perceive(self, world):
        # Gather local information
        nearby_entities = world.get_entities_in_range(self.position, perception_range)
        return nearby_entities

    def decide(self, perceptions):
        # Simple rule-based decision making
        if self.needs_food() and self.sees_food(perceptions):
            return "seek_food"
        elif self.sees_threat(perceptions):
            return "flee"
        else:
            return "wander"

    def act(self, world, action):
        if action == "seek_food":
            self.move_towards(nearest_food(perceptions))
        elif action == "flee":
            self.move_away_from(nearest_threat(perceptions))
```

#### Emergent Behavior Examples

**Flocking (Boids Algorithm)**:

Three simple rules create realistic bird flocking:
```python
def flocking_behavior(boid, neighbors):
    separation = vector_away_from_close_neighbors(neighbors, radius=5)
    alignment = average_velocity_of_neighbors(neighbors, radius=10)
    cohesion = vector_towards_center_of_neighbors(neighbors, radius=10)

    return separation + alignment + cohesion
```

**Traffic Jams**:

Cars following simple rules:
```python
def car_update(car, cars_ahead):
    # Accelerate if road clear
    if distance_to_next_car > safe_distance:
        speed += acceleration

    # Decelerate if car ahead
    else:
        speed = next_car.speed * 0.8

    # Random braking (imperfection)
    if random() < 0.01:
        speed *= 0.5
```

Result: **Phantom traffic jams** emerge from nothing—waves of braking propagating backward.

### 6.2 Emergent Behavior from Simple Rules

**Definition**: Complex system-level behavior arising from simple local interactions.

#### Conway's Game of Life (1970)

Four rules create universe of complexity:

```
1. Live cell < 2 neighbors → dies (underpopulation)
2. Live cell 2-3 neighbors → lives
3. Live cell > 3 neighbors → dies (overpopulation)
4. Dead cell = 3 neighbors → becomes alive (reproduction)
```

**Emergent Patterns**:
- Still lifes (blocks, beehives)
- Oscillators (blinkers, glider guns)
- Spaceships (gliders, LWSS)
- Turing-complete computation

#### Will Wright's Design Philosophy

From SimCity to The Sims:
```
Complexity = (Rules ^ Agents) × Interconnections
```

**Key Insight**:
- Don't script behaviors
- Define relationships between variables
- Let players discover emergent strategies

**Example**: The Sims (2000)
- No "story"—just needs (hunger, energy, social)
- Emergent narrative from need interactions
- Players create soap operas from autonomous behavior

#### Strategy Game Applications

**Chess**:
- Rules: 6 piece types × ~30 movement rules
- Emergent: Infinite strategic depth
- No AI required for depth

**Go**:
- Rules: Placement + capture rules
- Emergent: Territory battles, life-and-death
- Complexity exceeds chess despite simpler rules

### 6.3 Economic Simulation

**Definition**: Modeling supply, demand, and market dynamics.

#### Supply-Demand Curves

```python
class Market:
    def __init__(self):
        self.supply = {}
        self.demand = {}
        self.prices = {}

    def update_prices(self):
        for good in self.goods:
            # Price increases with scarcity
            surplus = self.supply[good] - self.demand[good]
            price_change = -surplus * elasticity

            self.prices[good] *= (1 + price_change)
            self.prices[good] = clamp(self.prices[good], min_price, max_price)
```

#### Complex Production Chains

**Multi-Stage Production**:

```
Iron Ore → Iron Plate → → →
                    ↓
                 Gear → → → → Engine → → Car
                    ↑
                Copper Plate
```

Each stage:
- Has its own supply/demand
- Responds to price signals
- Creates forward/backward propagation of changes

#### Inflation and Currencies

```python
def update_economy(empire):
    # Calculate money supply
    money_supply = sum(agent.gold for agent in empire.agents)

    # Calculate total production (GDP)
    total_production = sum(empire.production.values())

    # Inflation: more money chasing same goods
    inflation_rate = money_supply / total_production

    # Adjust all prices
    for good in empire.markets:
        empire.markets[good].price *= (1 + inflation_rate * 0.1)
```

### 6.4 Population Dynamics

**Definition**: Modeling population changes over time.

#### Predator-Prey Models (Lotka-Volterra)

```
dx/dt = αx - βxy  (Prey population change)
dy/dt = δxy - γy  (Predator population change)

Where:
x = prey population
y = predator population
α = prey birth rate
β = predation rate
δ = predator reproduction from prey
γ = predator death rate
```

**In Games**:

```python
def update_ecosystem(delta_time):
    # Rabbits (prey)
    rabbit_growth = rabbit_birth_rate * rabbit_population
    rabbit_predation = predation_rate * rabbit_population * wolf_population
    rabbit_population += (rabbit_growth - rabbit_predation) * delta_time

    # Wolves (predators)
    wolf_growth = reproduction_efficiency * rabbit_predation
    wolf_death = wolf_death_rate * wolf_population
    wolf_population += (wolf_growth - wolf_death) * delta_time
```

**Result**: Oscillating populations (rabbits up → wolves up → rabbits down → wolves down → repeat)

#### Demographic Models

```python
class Population:
    def __init__(self):
        self.age_groups = {
            "children": [],
            "adults": [],
            "elders": []
        }

    def update(self, delta_time):
        # Births
        new_births = len(self.age_groups["adults"]) * birth_rate * delta_time
        self.age_groups["children"].extend([Person(age=0) for _ in range(int(new_births))])

        # Aging
        for person in self.age_groups["children"]:
            person.age += delta_time
            if person.age >= adulthood_age:
                transition_to_adults(person)

        # Deaths
        death_probability = calculate_mortality(self.age_groups)
        remove_deceased(death_probability)
```

---

## 7. Extractable Patterns for Minecraft

### 7.1 Resource Management

**From**: Anno 1800, Factorio
**To**: Steve AI resource allocation

#### Pattern: Resource-Based Prioritization

```java
// Factorio-inspired backwards planning
public class ResourcePlanner {

    public ProductionPlan planForTarget(Item target, int quantity) {
        ProductionPlan plan = new ProductionPlan();

        // Calculate required inputs recursively
        Queue<ItemRequirement> queue = new LinkedList<>();
        queue.add(new ItemRequirement(target, quantity));

        while (!queue.isEmpty()) {
            ItemRequirement req = queue.poll();

            // Find recipe
            Recipe recipe = recipeBook.getRecipe(req.item());
            if (recipe != null) {
                int machinesNeeded = (int) Math.ceil(
                    (double) req.quantity() / recipe.outputQuantity()
                );

                plan.addProduction(req.item(), machinesNeeded);

                // Add input requirements
                for (Item input : recipe.inputs()) {
                    int needed = machinesNeeded * recipe.inputCount(input);
                    queue.add(new ItemRequirement(input, needed));
                }
            } else {
                // Raw material - add to gathering requirements
                plan.addGathering(req.item(), req.quantity());
            }
        }

        return plan;
    }
}
```

#### Pattern: Utility-Based Resource Selection

```java
// RimWorld-inspired utility scoring
public class ResourceSelector {

    public Block selectMostValuableResource(List<Block> availableResources, AgentContext context) {

        return availableResources.stream()
            .max(Comparator.comparingDouble(block -> calculateResourceUtility(block, context)))
            .orElse(null);
    }

    private double calculateResourceUtility(Block block, AgentContext context) {
        double utility = 0;

        // Base value (scarcity)
        double distance = context.steve().getPosition().distanceTo(block.getPos());
        utility += 100 / (distance + 1);

        // Current need priority
        double needPriority = context.memory().getNeedPriority(block.asItem());
        utility += needPriority * 50;

        // Tool availability
        if (context.inventory().hasRequiredTool(block)) {
            utility += 30;
        } else {
            utility -= 50; // Penalty for needing tool change
        }

        // Danger assessment
        double danger = assessDanger(block.getPos(), context);
        utility -= danger * 20;

        return utility;
    }
}
```

### 7.2 Base Layout Optimization

**From**: Cities: Skylines, StarCraft AI
**To**: Steve AI structure placement

#### Pattern: Cellular Automata Site Analysis

```java
// SimCity-inspired cellular analysis
public class SiteAnalyzer {

    public SiteScore analyzeSite(BlockPos center, World world, int radius) {

        SiteScore score = new SiteScore();

        // Analyze each cell in radius
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    BlockState block = world.getBlockState(pos);

                    // Factor 1: Flatness (desirable for building)
                    if (y == 0 && isBuildableSurface(block)) {
                        score.flatness++;
                    }

                    // Factor 2: Space availability
                    if (isAir(block)) {
                        score.space++;
                    }

                    // Factor 3: Resource proximity
                    if (isValuableResource(block)) {
                        score.resourceProximity += 1.0 / (pos.distSqr(center) + 1);
                    }

                    // Factor 4: Defense potential
                    if (isDefensiblePosition(block, world)) {
                        score.defensibility++;
                    }
                }
            }
        }

        return score;
    }

    public BlockPos findOptimalBuildingSite(BlockPos preferredCenter, World world, int searchRadius, int buildingSize) {
        double bestScore = Double.NEGATIVE_INFINITY;
        BlockPos bestSite = null;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                BlockPos candidate = preferredCenter.add(x, 0, z);

                // Check if can fit building
                if (!canFitBuilding(candidate, world, buildingSize)) {
                    continue;
                }

                SiteScore score = analyzeSite(candidate, world, buildingSize / 2);
                double totalScore =
                    score.flatness * 2.0 +
                    score.space * 0.5 +
                    score.resourceProximity * 50.0 +
                    score.defensibility * 3.0;

                if (totalScore > bestScore) {
                    bestScore = totalScore;
                    bestSite = candidate;
                }
            }
        }

        return bestSite;
    }
}
```

#### Pattern: Zone-Based Layout

```java
// Cities: Skylines-inspired zoning
public class BaseLayoutZoner {

    public enum Zone {
        RESIDENTIAL,    // Living quarters
        INDUSTRIAL,     // Farms, smelters
        STORAGE,        // Warehouses
        UTILITY,        // Farms, enchanting
        DEFENSIVE       // Walls, turrets
    }

    public Map<BlockPos, Zone> createZonedLayout(BlockPos center, World world) {
        Map<BlockPos, Zone> zones = new HashMap<>();

        // Central area: residential/utility
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                BlockPos pos = center.add(x, 0, z);
                if (x * x + z * z <= 25) {
                    zones.put(pos, Zone.RESIDENTIAL);
                }
            }
        }

        // Outer ring: storage
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                BlockPos pos = center.add(x, 0, z);
                int dist = x * x + z * z;
                if (dist > 25 && dist <= 100) {
                    zones.put(pos, Zone.STORAGE);
                }
            }
        }

        // Extended area: industrial (near resources)
        List<BlockPos> resourceNodes = findNearbyResources(center, world, 30);
        for (BlockPos resource : resourceNodes) {
            BlockPos zonePos = findNearestBuildable(resource, world);
            zones.put(zonePos, Zone.INDUSTRIAL);
        }

        // Perimeter: defensive
        List<BlockPos> perimeter = generatePerimeter(center, 15);
        for (BlockPos pos : perimeter) {
            zones.put(pos, Zone.DEFENSIVE);
        }

        return zones;
    }
}
```

### 7.3 Production Chains

**From**: Factorio, Anno 1800
**To**: Steve AI crafting automation

#### Pattern: Recipe Dependency Graph

```java
// Factorio-inspired production graph
public class ProductionGraph {

    private Map<Item, ProductionNode> nodes = new HashMap<>();

    public void buildGraph(Set<Item> targetItems) {
        // Build production tree from targets backwards
        for (Item target : targetItems) {
            buildNodeRecursive(target);
        }
    }

    private ProductionNode buildNodeRecursive(Item item) {
        if (nodes.containsKey(item)) {
            return nodes.get(item);
        }

        ProductionNode node = new ProductionNode(item);
        nodes.put(item, node);

        Recipe recipe = recipeBook.getRecipe(item);
        if (recipe != null) {
            // Recipe requires inputs - recursively add them
            for (Item input : recipe.getInputs()) {
                ProductionNode inputNode = buildNodeRecursive(input);
                node.addInput(inputNode);
            }
        }

        return node;
    }

    public ProductionPlan optimizeProduction(Item target, int targetQuantity) {
        ProductionPlan plan = new ProductionPlan();

        ProductionNode targetNode = nodes.get(target);
        if (targetNode == null) {
            // Raw material - just gather
            plan.addGathering(target, targetQuantity);
            return plan;
        }

        // Calculate required inputs
        Recipe recipe = recipeBook.getRecipe(target);
        int batchesNeeded = (int) Math.ceil((double) targetQuantity / recipe.getOutputQuantity());

        // Add this production step
        plan.addCrafting(target, batchesNeeded);

        // Recursively add input requirements
        for (Item input : recipe.getInputs()) {
            int inputNeeded = batchesNeeded * recipe.getInputCount(input);
            ProductionPlan inputPlan = optimizeProduction(input, inputNeeded);
            plan.merge(inputPlan);
        }

        return plan;
    }
}
```

#### Pattern: Throughput Balancing

```java
// Factorio-inspired throughput optimization
public class ProductionBalancer {

    public ProductionConfiguration balanceProduction(Item target, double outputPerMinute) {

        // Get recipe
        Recipe recipe = recipeBook.getRecipe(target);
        double recipeTime = recipe.getCraftingTimeSeconds();
        double recipeOutput = recipe.getOutputQuantity();

        // Calculate machines needed
        double machinesNeeded = (outputPerMinute / 60.0) * recipeTime / recipeOutput;
        int actualMachines = (int) Math.ceil(machinesNeeded);

        ProductionConfiguration config = new ProductionConfiguration();
        config.targetItem = target;
        config.machines = actualMachines;
        config.actualOutput = (actualMachines / recipeTime) * recipeOutput * 60;

        // Calculate input requirements per minute
        for (Item input : recipe.getInputs()) {
            double inputPerMinute =
                (config.actualOutput / 60.0) *
                (recipe.getInputCount(input) / recipe.getOutputQuantity()) *
                60.0;

            config.inputRequirements.put(input, inputPerMinute);
        }

        return config;
    }

    // Example: Calculate balanced production tree
    public ProductionTree createBalancedTree(Item finalProduct, double targetOutput) {
        ProductionTree tree = new ProductionTree();

        Queue<TargetRequirement> queue = new LinkedList<>();
        queue.add(new TargetRequirement(finalProduct, targetOutput));

        while (!queue.isEmpty()) {
            TargetRequirement req = queue.poll();

            ProductionConfiguration config = balanceProduction(req.item, req.quantity);
            tree.addProduction(req.item, config);

            // Add inputs as requirements
            for (Map.Entry<Item, Double> input : config.inputRequirements.entrySet()) {
                queue.add(new TargetRequirement(input.getKey(), input.getValue()));
            }
        }

        return tree;
    }
}
```

### 7.4 Threat Response

**From**: Civilization, X-COM, RimWorld
**To**: Steve AI survival behavior

#### Pattern: Multi-Factor Threat Assessment

```java
// X-COM inspired threat evaluation
public class ThreatEvaluator {

    public ThreatAssessment assessThreats(LivingEntity steve, World world) {

        List<HostileEntity> hostiles = findNearbyHostiles(steve, world, 32);
        ThreatAssessment assessment = new ThreatAssessment();

        for (HostileEntity hostile : hostiles) {
            double threatScore = 0;

            // Factor 1: Combat capability
            threatScore += hostile.getAttackDamage() * 2;

            // Factor 2: Proximity (inverse square law)
            double distance = steve.getPosition().distSqr(hostile.getPosition());
            threatScore += 100 / (distance + 1);

            // Factor 3: Current targeting
            if (hostile.getAttackTarget() == steve) {
                threatScore += 50; // Immediate threat
            }

            // Factor 4: Count (groups are more threatening)
            if (hostile.hasAllies()) {
                threatScore *= 1.5;
            }

            // Factor 5: Time of day (night = more dangerous)
            if (world.isNight()) {
                threatScore *= 1.3;
            }

            assessment.addThreat(hostile, threatScore);
        }

        return assessment;
    }

    public CombatAction decideCombatResponse(LivingEntity steve, ThreatAssessment threats) {

        if (threats.getTotalThreat() == 0) {
            return CombatAction.CONTINUE;
        }

        // Check health status
        double healthPercent = steve.getHealth() / steve.getMaxHealth();

        if (healthPercent < 0.3) {
            // Critical - flee prioritized
            return CombatAction.FLEE;
        } else if (healthPercent < 0.6) {
            // Damaged - consider retreating to cover
            if (findNearestCover(steve, threats) != null) {
                return CombatAction.TAKE_COVER;
            }
        }

        // Evaluate offensive capability
        HostileEntity biggestThreat = threats.getBiggestThreat();
        if (canDefeat(steve, biggestThreat)) {
            return CombatAction.ENGAGE;
        } else {
            return CombatAction.AVOID;
        }
    }
}
```

#### Pattern: Defensive Positioning

```java
// X-COM inspired cover evaluation
public class DefensivePositioner {

    public BlockPos findBestCover(LivingEntity steve, List<HostileEntity> threats, World world) {

        double bestScore = Double.NEGATIVE_INFINITY;
        BlockPos bestPosition = null;

        // Search radius for cover
        int searchRadius = 16;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos candidate = steve.getBlockPos().add(x, y, z);

                    double score = evaluateCoverPosition(candidate, steve, threats, world);
                    if (score > bestScore) {
                        bestScore = score;
                        bestPosition = candidate;
                    }
                }
            }
        }

        return bestPosition;
    }

    private double evaluateCoverPosition(BlockPos pos, LivingEntity steve,
                                        List<HostileEntity> threats, World world) {

        double score = 0;

        // Factor 1: Visibility to threats (lower is better)
        double visibilityPenalty = 0;
        for (HostileEntity threat : threats) {
            if (hasLineOfSight(world, pos, threat.getBlockPos())) {
                visibilityPenalty += 50 / (pos.distSqr(threat.getBlockPos()) + 1);
            }
        }
        score -= visibilityPenalty;

        // Factor 2: Physical cover value
        double coverValue = calculateCoverValue(pos, world, threats);
        score += coverValue * 30;

        // Factor 3: Proximity to safety (distance from current position)
        double distance = steve.getBlockPos().distSqr(pos);
        score -= distance * 0.1; // Prefer closer positions

        // Factor 4: Mobility (can we escape if cornered?)
        double escapeRoutes = countEscapeRoutes(pos, world);
        score += escapeRoutes * 5;

        // Factor 5: Strategic height (high ground advantage)
        if (pos.getY() > steve.getBlockPos().getY()) {
            score += 15;
        }

        return score;
    }

    private double calculateCoverValue(BlockPos pos, World world, List<HostileEntity> threats) {
        // Check for blocks that block line of sight
        double coverScore = 0;

        for (HostileEntity threat : threats) {
            Direction threatDirection = Direction.getFacing(
                pos.getX() - threat.getX(),
                pos.getZ() - threat.getZ()
            );

            BlockPos coverBlock = pos.relative(threatDirection);
            if (isFullCover(world, coverBlock)) {
                coverScore += 1.0; // Full cover
            } else if (isHalfCover(world, coverBlock)) {
                coverScore += 0.5; // Partial cover
            }
        }

        return coverScore;
    }
}
```

### 7.5 Long-Term Planning

**From**: Civilization tech trees, RimWorld needs
**To**: Steve AI goal-oriented behavior

#### Pattern: Goal Decomposition

```java
// GOAP-inspired goal decomposition
public class GoalPlanner {

    public Queue<Action> planGoals(Goal targetGoal, AgentContext context) {

        Queue<Action> plan = new LinkedList<>();
        Set<Goal> achievedGoals = new HashSet<>();

        // Decompose goal into sub-goals recursively
        decomposeGoalRecursive(targetGoal, context, plan, achievedGoals, 0);

        return plan;
    }

    private void decomposeGoalRecursive(Goal goal, AgentContext context,
                                       Queue<Action> plan, Set<Goal> achieved,
                                       int depth) {

        if (depth > 10) return; // Prevent infinite recursion
        if (achieved.contains(goal)) return;

        // Check if goal already satisfied
        if (goal.isSatisfied(context)) {
            achieved.add(goal);
            return;
        }

        // Find prerequisites
        List<Goal> prerequisites = goal.getPrerequisites(context);

        // First satisfy all prerequisites
        for (Goal prereq : prerequisites) {
            decomposeGoalRecursive(prereq, context, plan, achieved, depth + 1);
        }

        // Add actions to achieve this goal
        List<Action> actions = goal.getActionsToAchieve(context);
        for (Action action : actions) {
            plan.add(action);
        }

        achieved.add(goal);
    }

    // Example goal hierarchy
    public static class CraftingGoal implements Goal {

        private Item targetItem;
        private int quantity;

        @Override
        public boolean isSatisfied(AgentContext context) {
            return context.inventory().count(targetItem) >= quantity;
        }

        @Override
        public List<Goal> getPrerequisites(AgentContext context) {
            List<Goal> prereqs = new ArrayList<>();

            Recipe recipe = recipeBook.getRecipe(targetItem);
            if (recipe != null) {
                // Need crafting table
                if (!context.nearbyHasCraftingTable()) {
                    prereqs.add(new PlaceCraftingTableGoal());
                }

                // Need ingredients
                for (Item ingredient : recipe.getInputs()) {
                    int needed = recipe.getInputCount(ingredient);
                    int have = context.inventory().count(ingredient);
                    if (have < needed) {
                        prereqs.add(new AcquisitionGoal(ingredient, needed - have));
                    }
                }
            }

            return prereqs;
        }

        @Override
        public List<Action> getActionsToAchieve(AgentContext context) {
            return List.of(
                new ApproachCraftingTableAction(),
                new CraftAction(targetItem, quantity)
            );
        }
    }
}
```

#### Pattern: Priority-Based Scheduling

```java
// RimWorld-inspired need-based priority
public class NeedScheduler {

    private List<Need> needs = Arrays.asList(
        new Need("health", 1.0),      // Always highest priority
        new Need("safety", 0.8),      // High priority
        new Need("hunger", 0.6),      // Medium-high
        new Need("tools", 0.4),       // Medium
        new Need("resources", 0.3),   // Medium-low
        new Need("building", 0.2),    // Low
        new Need("exploration", 0.1)  // Lowest
    );

    public Action selectNextAction(AgentContext context) {

        // Score each need based on urgency and importance
        List<NeedScore> scoredNeeds = new ArrayList<>();

        for (Need need : needs) {
            double urgency = calculateUrgency(need, context);
            double importance = need.importance;
            double score = urgency * importance * 100;

            scoredNeeds.add(new NeedScore(need, score));
        }

        // Sort by score
        scoredNeeds.sort((a, b) -> Double.compare(b.score, a.score));

        // Return action for highest-priority need
        if (!scoredNeeds.isEmpty()) {
            Need topNeed = scoredNeeds.get(0).need;
            return topNeed.getSatisfyingAction(context);
        }

        return new IdleAction();
    }

    private double calculateUrgency(Need need, AgentContext context) {
        switch (need.name) {
            case "health":
                // Urgency increases as health decreases
                double healthPercent = context.steve().getHealth() / context.steve().getMaxHealth();
                return 1.0 - healthPercent;

            case "safety":
                // Urgency based on nearby threats
                int threatCount = context.getNearbyThreats(16).size();
                return Math.min(1.0, threatCount * 0.3);

            case "hunger":
                // Urgency based on food saturation
                double saturation = context.steve().getFoodData().getSaturationLevel();
                return Math.max(0, 1.0 - saturation / 20.0);

            // ... etc for other needs

            default:
                return 0;
        }
    }
}
```

---

## 8. Case Studies

### 8.1 Case Study: SimCity 2000's Cellular Automata Traffic

**Problem**: Simulate urban traffic flow without pathfinding each agent.

**Solution**: Cellular automaton approach.

**Implementation**:
```
Each road cell maintains:
- Traffic state (0-100 congestion level)
- Trip generation (local activity)
- Trip attraction (local destinations)

Update per tick:
congestion[i] += trip_generation[i] - trip_absorption[i]
congestion[i] += (congestion[neighbor] - congestion[i]) * diffusion_rate
congestion[i] = clamp(congestion[i], 0, 100)
```

**Result**: Traffic jams emerge naturally from local congestion propagation—no global coordination needed.

**Lessons for Minecraft**:
- Local updates can create global patterns
- State diffusion simulates agent movement cheaply
- Cellular automata work well for grid-based worlds

### 8.2 Case Study: RimWorld's Storyteller

**Problem**: Create engaging narrative without scripting.

**Solution**: Utility-based event selection.

**Implementation**:
```python
def select_event(storyteller, colony):
    event_scores = {}

    for event in available_events:
        # Drama factor: how interesting is this?
        drama = calculate_drama_impact(event, colony)

        # Difficulty: can the colony handle this?
        difficulty = estimate_colony_strength(colony) / estimate_event_threat(event)

        # Timing: how long since last crisis?
        crisis_recency = time_since_last_crisis(colony)

        # Combine
        score = (drama * storyteller.drama_weight +
                 difficulty * storyteller.difficulty_weight +
                 crisis_recency * storyteller.pacing_weight)

        event_scores[event] = score

    # Weighted random from top-scoring events
    top_events = sorted(event_scores.items(), key=lambda x: x[1], reverse=True)[:5]
    return weighted_random(top_events)
```

**Result**: Each playthrough creates unique stories—no two games alike.

**Lessons for Minecraft**:
- Events don't need to be scripted
- Utility scoring creates variety
- Timing is crucial for engagement

### 8.3 Case Study: Dwarf Fortress's Depth Without AI

**Problem**: Create deep simulation without complex AI.

**Solution**: Simulate everything at maximum detail.

**Implementation**:
- Each creature: 50+ attributes
- Each material: 10+ properties
- Each item: detailed creation history
- Each event: stored in world history

**Result**: Emergent complexity from sheer detail.

**Key Insight**: "Legend mode" where players read generated history—drama emerges from simulation, not narrative.

**Lessons for Minecraft**:
- Detail creates depth
- Tracking everything enables rich interactions
- Simulation > AI for emergent behavior

### 8.4 Case Study: Into the Breach's Perfect Information

**Problem**: Strategy games rely on randomness, creating frustration.

**Solution**: Telegraph all enemy intentions.

**Implementation**:
```
Turn structure:
1. Player sees all enemy intentions
2. Player plans moves
3. Player actions modify enemy intentions
4. Enemies execute telegraphed attacks
```

**Result**: Pure skill-based tactical puzzles—no "RNG failure" frustration.

**Key Insight**: Transparency > complexity. Perfect information enables deeper strategy.

**Lessons for Minecraft**:
- Predictable enemies are more engaging
- Information is a gameplay mechanic
- Telegraph intentions creates fair challenge

### 8.5 Case Study: Factorio's Solvability

**Problem**: Complex production optimization.

**Solution**: Classical Operations Research algorithms.

**Implementation**:
- Linear programming for resource allocation
- Network flow for logistics
- Genetic algorithms for layout optimization
- No machine learning needed

**Key Quote**: "99% of Factorio can be solved with 1980s algorithms."

**Lessons for Minecraft**:
- Production planning is math, not AI
- Backwards calculation works for crafting trees
- Optimization algorithms > neural networks for logistics

---

## 9. Conclusion

### 9.1 Key Insights

**1. Emergence Over Scripting**

The most compelling strategy games don't script behavior—they define systems and let complexity emerge:
- **SimCity**: Traffic from cellular automata
- **RimWorld**: Stories from need thresholds
- **Dwarf Fortress**: Depth from detailed simulation
- **Factorio**: Complexity from recipe chains

**2. Utility Systems Are Universal**

From RimWorld's needs to Civilization's agendas, utility scoring works everywhere:
- Easy to understand and debug
- Extensible without restructuring
- Creates nuanced, human-like behavior
- No machine learning required

**3. Information Is a Mechanic**

Games thrive when information is managed intentionally:
- **Into the Breach**: Perfect information creates puzzles
- **X-COM**: Fog of war creates tension
- **Civilization VI**: Transparency builds trust

**4. Classical Algorithms Still Work**

Modern games don't need modern AI:
- A* for pathfinding
- Linear programming for resource optimization
- Network flow for logistics
- Priority queues for task scheduling

**5. Detail Creates Depth**

More simulation → more emergent possibilities:
- Dwarf Fortress: Everything simulated → infinite stories
- RimWorld: Every need tracked → social drama
- Cities: Skylines II: Each agent calculates paths → realistic traffic

### 9.2 Applications to Minecraft Automation

**Immediate Applications**:

1. **Resource Management** (Factorio-style)
   - Backwards planning from crafting goals
   - Production chain optimization
   - Throughput balancing

2. **Base Layout** (Cities: Skylines-style)
   - Zone-based organization
   - Cellular site analysis
   - Resource proximity optimization

3. **Threat Response** (X-COM-style)
   - Multi-factor threat assessment
   - Cover evaluation
   - Defensive positioning

4. **Priority Scheduling** (RimWorld-style)
   - Need-based utility scoring
   - Dynamic priority adjustment
   - Long-term goal decomposition

**Research Directions**:

1. **Multi-Agent Coordination**
   - Inspired by: Anno 1800 trade routes, Factorio trains
   - Apply to: Multiple Steves collaborating on projects

2. **Emergent Narrative**
   - Inspired by: RimWorld social dynamics, Dwarf Fortress history
   - Apply to: Steve stories emerging from needs and interactions

3. **Predictable Encounters**
   - Inspired by: Into the Breach telegraphing
   - Apply to: Mob combat strategies with perfect information

4. **Production Automation**
   - Inspired by: Factorio blueprint systems
   - Apply to: Automatic farm, furnace, and crafting layouts

### 9.3 The Future: Classical AI's Renaissance

As LLMs dominate headlines, strategy games prove that **classical AI remains superior** for:

- Deterministic domains
- Resource optimization
- Tactical planning
- Long-term reasoning

The 35-year history (1990-2025) of strategy games shows:
- Better systems → better AI
- More simulation → more depth
- Classical algorithms → proven results

For Minecraft automation specifically, the path forward is clear:
1. Implement robust resource planning (Factorio)
2. Create utility-based decision systems (RimWorld)
3. Use classical optimization (Operations Research)
4. Let emergence provide complexity (Dwarf Fortress)

**No LLM required**—just careful system design and proven algorithms.

---

## References and Sources

### Academic Papers

- Bonabeau, E. (2002). "Agent-based modeling: Methods and techniques for simulating human systems." *Proceedings of the National Academy of Sciences*.
- Cho, H., Kim, K., & Cho, S. "Replay-based Strategy Prediction and Build Order Adaptation." *IEEE Conference on Computational Intelligence and Games*.
- Xing, P., et al. (2011). "Validation of Agent-Based Simulation through Human Computation: An Example of Crowd Simulation." *MABS Workshop, AAMAS*.

### Game Development Resources

- **Factorio**:
  - [FactorioLab Production Calculator](https://m.blog.csdn.net/gitblog_00780/article/details/157238821)
  - [Factorio Learning Environment](https://news.ycombinator.com/item?id=43331582)
  - [Production Planning Models](https://m.zhangqiaokeyan.com/academic-journal-cn_international-journal-minerals-metallurgy-materials_thesis/0201290359164.html)

- **Cities: Skylines**:
  - [Traffic AI Dev Log](https://www.bilibili.com/read/mobile?id=24605518)
  - [Traffic AI Guide](https://wiki.biligame.com/csl2/index.php?diff=1997&oldid=1894&title=Traffic)

- **RimWorld**:
  - [Steam Store Page](https://store.steampowered.com/app/294100/RimWorld/)
  - [AI Story Generator Article](https://m.sohu.com/a/940379695_122042258)

- **Civilization Series**:
  - [Civ VI IGN Review](https://www.ign.com/articles/2016/10/26/sid-meiers-civilization-vi-review)
  - [Cicero Diplomacy AI](https://m.blog.csdn.net/gitblog_00827/article/details/141986291)
  - [Civ V Diplomacy Principles](https://zhidao.baidu.com/question/1700549484305502748.html)

- **X-COM**:
  - [X-COM Analysis](https://www.gamedeveloper.com/design/overwatching-an-analysis-of-x-com-enemy-unknown)
  - [Cover Generator Plugin](https://m.blog.csdn.net/gitblog_00366/article/details/141617966)

- **Into the Breach**:
  - [GDC 2019 Postmortem](https://www.bilibili.com/video/BV1GsRoYxEMZ)
  - [Design Analysis](https://www.gcores.com/articles/200948)
  - [Steam Store](https://store.steampowered.com/app/590380/)

- **Crusader Kings**:
  - [Steam Store](https://store.steampowered.com/app/1158310/Crusader_Kings_III/)
  - [Dev Diary #120](https://www.bilibili.com/read/cv22599868)

- **Dwarf Fortress**:
  - [Steam Store](https://store.steampowered.com/app/975370/)
  - [Gentoo Wiki](https://wiki.gentoo.org/wiki/Dwarf_Fortress)

- **Anno 1800**:
  - [Steam Store](https://store.steampowered.com/app/916440/1800/)
  - [Production Guides](https://zhidao.baidu.com/question/533169340017547205.html)

- **Age of Empires**:
  - [AI Wiki](https://ageofempires.fandom.com/wiki/Artificial_intelligence)
  - [AI Analysis](https://www.toolify.ai/zh/ai-news-cn/%E5%B8%9D%E5%9B%BD%E6%97%B6%E4%BB%A32%E4%B8%ADai%E6%80%9D%E8%80%83%E6%96%B9%E5%BC%8F%E7%9A%84%E6%8F%AD%E7%A7%98-681946)

- **RTS Base Building**:
  - [PySC2 Building Planning](https://blog.csdn.net/gitblog_00277/article/details/154052062)
  - [RTS Planning](https://xueshu.baidu.com/usercenter/service/searchpaper?p=651d7ec21e2b02ceeeb2915d4fe10f8a)

### Technical References

- [Weighted Random Selection](https://m.blog.csdn.net/qq_41973169/article/details/137628731)
- [Utility AI Systems](https://developer.aliyun.com/article/1165183)
- [Agent-Based Modeling](https://m.zhangqiaokeyan.com/academic-journal-cn_international-journal-minerals-metallurgy-materials_thesis/0201290359164.html)
- [Resource Flow Optimization](https://m.zhangqiaokeyan.com/academic-journal-cn_international-journal-minerals-metallurgy-materials_thesis/0201290359164.html)

---

**End of Chapter 4**

**Next Chapter**: Chapter 5 - Emergent Behavior and Systems Design
