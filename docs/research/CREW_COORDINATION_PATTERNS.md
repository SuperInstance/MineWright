# Crew Coordination Patterns for Game AI

**Document Version:** 1.0
**Date:** 2026-03-02
**Author:** Claude Orchestrator
**Status:** Research Complete

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Coordination Architectures](#coordination-architectures)
3. [Task Allocation Patterns](#task-allocation-patterns)
4. [Communication Patterns](#communication-patterns)
5. [Case Studies](#case-studies)
6. [Application to MineWright](#application-to-minewright)
7. [Code Patterns](#code-patterns)
8. [Implementation Roadmap](#implementation-roadmap)
9. [References](#references)

---

## Executive Summary

Multi-agent coordination is one of the most challenging aspects of game AI. As agents scale from 1-2 to 10-20 or more, the complexity of task distribution, conflict resolution, and communication grows exponentially. This document researches proven coordination patterns from:

- **Real-Time Strategy (RTS) games** - StarCraft, Age of Empires worker coordination
- **Swarm robotics** - Decentralized, stigmergic coordination
- **Multi-Agent Systems (MAS)** - Contract Net Protocol, blackboard architectures
- **Multi-Agent Reinforcement Learning (MARL)** - AlphaStar, coordination graphs

The findings reveal that hierarchical coordination (foreman-worker) combined with market-based task allocation (Contract Net Protocol) provides the best balance of:

- **Scalability** - Handles 20+ agents without exponential complexity growth
- **Robustness** - Continues functioning when agents fail or disappear
- **Efficiency** - Maximizes parallel work while minimizing communication overhead
- **Simplicity** - Clear mental model for debugging and extension

**Key Recommendation:** Implement a hybrid architecture where:
1. Foreman uses LLM for strategic planning and task decomposition
2. Workers bid on tasks using Contract Net Protocol
3. Spatial partitioning prevents collision and enables parallel work
4. Blackboard system maintains shared world state
5. Event bus handles pub/sub communication

---

## Coordination Architectures

### 1. Centralized (Master-Slave)

**Description:** Single coordinator makes all decisions and issues direct commands.

```
        ┌─────────────┐
        │   MASTER    │
        │ Coordinator │
        └──────┬──────┘
               │
      ┌────────┼────────┬────────┐
      ▼        ▼        ▼        ▼
   ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐
   │Slave1│ │Slave2│ │Slave3│ │Slave4│
   └──────┘ └──────┘ └──────┘ └──────┘
```

**Advantages:**
- Simple implementation
- Global optimization possible
- Easy to debug (single decision point)
- Consistent behavior

**Disadvantages:**
- Single point of failure
- Bottleneck at scale
- No agent autonomy
- High communication latency

**Best For:**
- Small teams (2-5 agents)
- Highly coordinated tasks requiring global knowledge
- Scenarios where simplicity trumps fault tolerance

**Implementation in MineWright:**
- Already implemented in `OrchestratorService`
- Foreman receives human command, decomposes into tasks, assigns directly
- Workers execute tasks and report back
- Round-robin distribution for load balancing

**Code Example:**
```java
// From OrchestratorService.java
private void distributeTasks(PlanExecution plan, Collection<ForemanEntity> availableSteves) {
    List<Task> tasks = plan.getRemainingTasks();
    List<ForemanEntity> availableWorkers = availableSteves.stream()
        .filter(s -> !s.getEntityName().equals(foremanId))
        .filter(s -> !workerAssignments.containsKey(s.getEntityName()))
        .collect(Collectors.toList());

    int workerIndex = 0;
    for (Task task : tasks) {
        if (availableWorkers.isEmpty()) {
            assignTaskToAgent(plan, task, foremanId);
        } else {
            ForemanEntity worker = availableWorkers.get(workerIndex % availableWorkers.size());
            assignTaskToAgent(plan, task, worker.getEntityName());
            workerIndex++;
        }
    }
}
```

---

### 2. Decentralized (Peer-to-Peer)

**Description:** All agents equal, coordination through local communication and negotiation.

```
        ┌─────────────────────────────┐
        │                             │
    ┌───┴───┐ ┌─────────┐ ┌─────────┐│
    │Agent1 │ │  Agent2 │ │  Agent3 ││
    └───┬───┘ └────┬────┘ └────┬────┘│
        │          │           │     │
        └──────────┼───────────┘     │
                   │                 │
        ┌──────────┴───────────┐     │
        │   Local Communication│     │
        └──────────────────────┘     │
                                    └─
```

**Advantages:**
- No single point of failure
- Scales horizontally
- Agents can join/leave dynamically
- Lower latency decisions

**Disadvantages:**
- Complex coordination logic
- No global optimization
- Potential for oscillation/instability
- Harder to debug

**Best For:**
- Large teams (10+ agents)
- Dynamic environments with frequent agent changes
- Tasks that can be decomposed spatially
- Fault-tolerant scenarios

**Implementation Patterns:**
- **Consensus protocols** - Agents vote on decisions
- **Gossip protocols** - Information spreads gradually
- **Leader election** - Dynamic coordinator selection
- **Token passing** - Coordination token circulates

---

### 3. Hierarchical (Foreman-Worker)

**Description:** Multi-level tree structure with clear chains of command.

```
                    ┌─────────────┐
                    │   FOREMAN   │
                    │ (Strategic) │
                    └──────┬──────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
    ┌───────────┐    ┌───────────┐    ┌───────────┐
    │  CREW     │    │  CREW     │    │  CREW     │
    │ Leader A  │    │ Leader B  │    │ Leader C  │
    │ (Tactical)│    │ (Tactical)│    │ (Tactical)│
    └─────┬─────┘    └─────┬─────┘    └─────┬─────┘
          │                │                │
    ┌─────┴────┐      ┌────┴────┐      ┌────┴────┐
    ▼          ▼      ▼         ▼      ▼         ▼
  ┌────┐    ┌────┐  ┌────┐   ┌────┐  ┌────┐   ┌────┐
  │Work│    │Work│  │Work│   │Work│  │Work│   │Work│
  └────┘    └────┘  └────┘   └────┘  └────┘   └────┘
```

**Advantages:**
- Clear chain of responsibility
- Scalable organization
- Natural for human-like teams
- Efficient communication flow

**Disadvantages:**
- Multiple points of failure (each level)
- Slower decision propagation
- Potential for bottleneck at foreman
- Complex hierarchy management

**Best For:**
- Medium teams (5-20 agents)
- Tasks with natural decomposition (mining, building, combat)
- Scenarios requiring both strategy and tactics
- Human-understandable organization

**Implementation in MineWright:**
- `OrchestratorService` implements 2-level hierarchy (foreman + workers)
- Extensible to 3-level with crew leaders for spatial partitioning
- Natural mapping to Minecraft's building/mining tasks

**Code Pattern for 3-Level Hierarchy:**
```java
public class CrewLeader {
    private final String crewId;
    private final List<UUID> workers;
    private final BlockPos operatingArea;
    private TaskPriority currentPriority;

    public void assignLocalTasks(List<Task> tasks) {
        // Distribute tasks to workers in this crew
        // Based on proximity and capability
    }

    public void reportToForeman(CrewStatusReport report) {
        // Aggregate status and report upward
    }
}
```

---

### 4. Blackboard-Based

**Description:** Shared knowledge space that agents read/write asynchronously.

```
┌─────────────────────────────────────────────────┐
│              BLACKBOARD (Shared State)           │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌────────┐ │
│  │WorldMap │ │TaskQueue│ │Resource │ │Status  │ │
│  │         │ │         │ │Tracker  │ │Board   │ │
│  └─────────┘ └─────────┘ └─────────┘ └────────┘ │
└────────┬────────────────────────────────────────┘
         │
    ┌────┴────┬──────────┬──────────┬──────────┐
    ▼         ▼          ▼          ▼          ▼
 ┌──────┐ ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐
 │Agent1│ │Agent2│  │Agent3│  │Agent4│  │Agent5│
 └──────┘ └──────┘  └──────┘  └──────┘  └──────┘
(Read/Write) (Read/Write) (Read/Write) (Read/Write) (Read/Write)
```

**Advantages:**
- Loose coupling between agents
- Flexible knowledge sharing
- Natural for asymmetric information
- Easy to add new agents

**Disadvantages:**
- No direct control flow
- Potential for stale data
- Complex synchronization
- Hard to guarantee consistency

**Best For:**
- Heterogeneous agents with different expertise
- Complex problem spaces with partial knowledge
- Scenarios requiring shared situational awareness
- Research and exploration tasks

**Implementation in MineWright:**
- `BlackboardEntry` and `KnowledgeArea` classes exist
- Can be enhanced with reactive updates and subscriptions
- Useful for sharing resource locations, danger zones, build plans

**Code Example:**
```java
public class SharedBlackboard {
    private final Map<String, BlackboardEntry> entries;
    private final Map<String, Set<UUID>> subscribers;

    public void write(String key, Object value, UUID agentId) {
        BlackboardEntry entry = new BlackboardEntry(key, value, agentId, System.currentTimeMillis());
        entries.put(key, entry);
        notifySubscribers(key, entry);
    }

    public Optional<BlackboardEntry> read(String key) {
        return Optional.ofNullable(entries.get(key));
    }

    public void subscribe(String key, UUID agentId, Consumer<BlackboardEntry> callback) {
        subscribers.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(agentId);
        // Register callback for this agent
    }
}
```

---

## Task Allocation Patterns

### 1. Contract Net Protocol (CNP)

**Description:** Market-based bidding system where agents compete for tasks.

**Flow:**
```
Manager              Agent1              Agent2              Agent3
  │                    │                   │                   │
  ├── announceTask ────┼───────────────────┼───────────────────>│
  │                    │                   │                   │
  │                    ├─ evaluate ────────┤                   │
  │                    ├─ capability ──────┤                   │
  │                    │                   │                   │
  │<── submitBid ──────┼───────────────────┼───────────────────┤
  │     (bid: $50)     │    (bid: $30)     │    (bid: $40)     │
  │                    │                   │                   │
  ├─ evaluate bids ────┤                   │                   │
  ├─ select winner ────┤                   │                   │
  │                    │                   │                   │
  ├── awardContract ───────────────────────────────────────────>│
  │                    │        (to Agent2 - lowest bid)        │
  │                    │                   │                   │
  │                    │                   ├─ execute ──────────┤
  │                    │                   │                   │
  │<─── progressUpdate ┼───────────────────┼───────────────────┤
  │<──── taskComplete ─────────────────────────────────────────┤
```

**Advantages:**
- Self-organizing - no central allocation logic
- Efficient - agents self-select based on capability
- Fair - open competition for tasks
- Extensible - easy to add bid criteria

**Disadvantages:**
- Communication overhead (bid/award messages)
- Delay while waiting for bids
- Complex winner selection logic
- Potential for no bids (task starvation)

**Best For:**
- Heterogeneous agents with varying capabilities
- Tasks where agents have different costs/abilities
- Dynamic environments where agent availability changes

**Implementation in MineWright:**
- `ContractNetProtocol` class implements full CNP
- `TaskBid` captures agent estimates
- `AwardSelector` chooses winners based on criteria
- `BidCollector` manages bid collection window

**Code Example:**
```java
// Agent submitting a bid
public class WorkerAgent {
    public void onTaskAnnounced(TaskAnnouncement announcement) {
        Task task = announcement.task();

        // Evaluate capability
        double capability = capabilityRegistry.getCapability(getId(), task.getAction());
        if (capability <= 0) return; // Not capable

        // Calculate cost
        double distance = getPosition().distanceTo(task.getTargetLocation());
        double estimatedTime = estimateExecutionTime(task, distance);
        double bidAmount = capability / estimatedTime; // Higher is better

        // Submit bid
        TaskBid bid = TaskBid.builder()
            .announcementId(announcement.announcementId())
            .bidderId(getId())
            .bidAmount(bidAmount)
            .estimatedDuration(estimatedTime)
            .confidence(calculateConfidence(task))
            .build();

        contractNet.submitBid(bid);
    }
}

// Foreman selecting winner
public class ForemanAgent {
    public void onBidsCollected(String announcementId) {
        List<TaskBid> bids = contractNet.getBids(announcementId);

        if (bids.isEmpty()) {
            // No bids - handle failure
            handleTaskUnannounced(announcementId);
            return;
        }

        // Select best bid (AwardSelector handles ties, conflicts)
        TaskBid winner = awardSelector.selectBestBid(bids);

        // Award contract
        contractNet.awardContract(announcementId, winner);

        // Send task assignment
        sendTaskAssignment(winner.bidderId(), announcementId);
    }
}
```

**Bid Calculation Strategies:**

1. **Distance-Based:**
```java
double bid = 1000.0 / (1.0 + distanceToTask);
```

2. **Capability-Based:**
```java
double skillLevel = capabilityRegistry.getSkill(agentId, taskType);
double bid = skillLevel * 100.0;
```

3. **Load-Balanced:**
```java
int currentTasks = getAssignedTaskCount(agentId);
double availabilityFactor = 1.0 / (1.0 + currentTasks);
double bid = baseCapability * availabilityFactor;
```

4. **Composite (Recommended):**
```java
double distanceFactor = 1.0 / (1.0 + distanceToTask / 10.0);
double capabilityFactor = capabilityRegistry.getCapability(agentId, taskType);
double loadFactor = 1.0 / (1.0 + getAssignedTaskCount(agentId));
double confidence = calculateConfidence(task);

double bid = distanceFactor * capabilityFactor * loadFactor * confidence;
```

---

### 2. Market-Based Allocation

**Description:** Virtual currency system where agents "buy" and "sell" tasks.

**Concepts:**
- **Currency:** Agents earn money by completing tasks, spend to acquire tasks
- **Price:** Determined by supply/demand, difficulty, urgency
- **Profit:** Agents maximize (earnings - costs)
- **Market:** Central exchange where tasks are listed and bids placed

**Advantages:**
- Naturally balances supply and demand
- Incentivizes efficiency
- Self-regulating
- Easy to add new economic factors

**Disadvantages:**
- Complex to tune (inflation, deflation)
- Agents may game the system
- Requires currency management
- Hard to predict behavior

**Best For:**
- Economic simulations
- Resource allocation with scarcity
- Long-running agent systems

**Code Example:**
```java
public class TaskMarket {
    private final Map<String, TaskListing> listings;
    private final Map<UUID, Double> agentBalances;

    public void listTask(Task task, double maxPrice, UUID seller) {
        String listingId = UUID.randomUUID().toString();
        TaskListing listing = new TaskListing(listingId, task, maxPrice, seller);
        listings.put(listingId, listing);

        notifyBuyers(listing);
    }

    public boolean placeBid(String listingId, double bidPrice, UUID buyer) {
        TaskListing listing = listings.get(listingId);

        if (bidPrice > listing.getMaxPrice()) {
            return false; // Seller won't accept
        }

        if (agentBalances.get(buyer) < bidPrice) {
            return false; // Buyer can't afford
        }

        listing.addBid(new Bid(buyer, bidPrice));
        return true;
    }

    public void executeTrade(String listingId) {
        TaskListing listing = listings.get(listingId);
        Bid winningBid = listing.getHighestBid();

        // Transfer money
        agentBalances.put(winningBid.getBuyer(),
            agentBalances.get(winningBid.getBuyer()) - winningBid.getPrice());
        agentBalances.put(listing.getSeller(),
            agentBalances.get(listing.getSeller()) + winningBid.getPrice());

        // Assign task
        assignTask(listing.getTask(), winningBid.getBuyer());
    }
}
```

---

### 3. Priority Queue

**Description:** Tasks sorted by priority, agents take from top of queue.

**Structure:**
```java
PriorityQueue<Task> taskQueue = new PriorityQueue<>(
    Comparator.comparingInt(Task::getPriority).reversed()
    .thenComparingDouble(Task::getUrgency).reversed()
    .thenComparingLong(Task::getCreatedAt)
);
```

**Advantages:**
- Simple to implement
- Clear priority semantics
- Efficient (O(log n) insertion/removal)
- No communication overhead

**Disadvantages:**
- No agent consideration
- Centralized bottleneck
- Can't adapt to agent capabilities
- Priority inversion possible

**Best For:**
- Single-agent systems
- Simple task ordering
- When all agents are equivalent

**Implementation in MineWright:**
```java
public class TaskPrioritizer {
    private final PriorityQueue<Task> queue;

    public TaskPrioritizer() {
        this.queue = new PriorityQueue<>(
            // Higher priority first
            Comparator.comparingInt(Task::getPriority).reversed()
            // Then by urgency (tasks with deadlines first)
            .thenComparingDouble(t -> t.getDeadline() != null ?
                t.getDeadline() - System.currentTimeMillis() : Long.MAX_VALUE)
            // Then by creation time (older tasks first)
            .thenComparingLong(Task::getCreatedAt)
        );
    }

    public void addTask(Task task) {
        queue.offer(task);
    }

    public Task getNextTask() {
        return queue.poll();
    }

    public List<Task> getTopTasks(int count) {
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < count && !queue.isEmpty(); i++) {
            tasks.add(queue.poll());
        }
        return tasks;
    }
}
```

---

### 4. Token Passing

**Description:** Coordination token circulates among agents, holder has control.

**Flow:**
```
Agent1 ──has token──> Agent2 ──has token──> Agent3 ──has token──> Agent1
  │                        │                        │
  │ Take action            │ Take action            │ Take action
  ▼                        ▼                        ▼
Execute task            Execute task            Execute task
Pass token              Pass token              Pass token
```

**Advantages:**
- Fair - everyone gets a turn
- Simple - no bidding or negotiation
- No central coordinator
- Natural for sequential tasks

**Disadvantages:**
- Slow - one agent at a time
- Token can be lost
- Doesn't utilize parallelism
- Not suitable for independent tasks

**Best For:**
- Sequential tasks
- Shared resources requiring mutual exclusion
- Fair scheduling requirements

**Code Example:**
```java
public class TokenRing {
    private final List<UUID> agents;
    private volatile UUID tokenHolder;
    private final AtomicLong tokenVersion;

    public void passToken(UUID from, UUID to) {
        if (!tokenHolder.equals(from)) {
            throw new IllegalStateException("Only token holder can pass token");
        }

        tokenHolder = to;
        tokenVersion.incrementAndGet();

        notifyTokenHolder(to);
    }

    public void onReceiveToken(UUID agentId, Consumer<Token> action) {
        if (tokenHolder.equals(agentId)) {
            Token token = new Token(tokenVersion.get());
            action.accept(token);
        }
    }
}
```

---

## Communication Patterns

### 1. Direct Messaging

**Description:** Point-to-point communication between specific agents.

**Pattern:**
```java
agentA.sendMessage(agentB, message);
```

**Advantages:**
- Targeted communication
- Low overhead
- Clear sender/receiver relationship
- Easy to trace

**Disadvantages:**
- Requires knowledge of recipient
- No broadcast capability
- Must handle availability

**Implementation in MineWright:**
```java
// From CommunicationBus.java
public void send(AgentMessage message) {
    if (message.isBroadcast()) {
        for (UUID agentId : handlers.keySet()) {
            if (!agentId.equals(message.senderId())) {
                deliverTo(agentId, message);
            }
        }
    } else {
        if (!handlers.containsKey(message.recipientId())) {
            LOGGER.warn("Recipient not registered: {}", message.recipientId());
            return;
        }
        deliverTo(message.recipientId(), message);
    }
}
```

---

### 2. Publish-Subscribe

**Description:** Agents publish to topics, others subscribe to topics of interest.

**Pattern:**
```java
// Subscribe
agentB.subscribe("task_announcements", handler);

// Publish (all subscribers receive)
agentA.publish("task_announcements", message);
```

**Advantages:**
- Decoupled - publisher doesn't know subscribers
- Flexible - agents can subscribe/unsubscribe dynamically
- Natural for event-driven systems
- One-to-many communication

**Disadvantages:**
- Less direct control
- Potential for message storms
- Need topic management

**Implementation Pattern:**
```java
public class MessageBus {
    private final Map<String, Set<UUID>> subscriptions;
    private final Map<UUID, Consumer<Message>> handlers;

    public void subscribe(UUID agentId, String topic, Consumer<Message> handler) {
        subscriptions.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet())
            .add(agentId);
        handlers.put(agentId, handler);
    }

    public void publish(String topic, Message message) {
        Set<UUID> subscribers = subscriptions.get(topic);
        if (subscribers != null) {
            for (UUID agentId : subscribers) {
                Consumer<Message> handler = handlers.get(agentId);
                if (handler != null) {
                    handler.accept(message);
                }
            }
        }
    }
}
```

---

### 3. Shared Blackboard

**Description:** Agents read/write shared data structure asynchronously.

**Pattern:**
```java
// Agent writes
blackboard.write("resource_location", new BlockPos(100, 64, 200), agentId);

// Agent reads
Optional<BlockPos> location = blackboard.read("resource_location");
```

**Advantages:**
- Implicit communication
- Persistent state
- Natural for knowledge sharing
- No direct coupling

**Disadvantages:**
- Stale data possible
- No notification by default
- Concurrency control needed
- Hard to track data provenance

**Implementation Pattern:**
```java
public class SharedBlackboard {
    private final Map<String, BlackboardEntry> entries;
    private final Map<String, Set<UUID>> watchers;

    public void write(String key, Object value, UUID agentId) {
        BlackboardEntry entry = new BlackboardEntry(key, value, agentId, System.currentTimeMillis());
        entries.put(key, entry);

        // Notify watchers
        Set<UUID> keyWatchers = watchers.get(key);
        if (keyWatchers != null) {
            for (UUID watcher : keyWatchers) {
                notifyWatcher(watcher, entry);
            }
        }
    }

    public void watch(String key, UUID agentId, Consumer<BlackboardEntry> callback) {
        watchers.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(agentId);
        // Register callback
    }
}
```

---

### 4. Stigmergy (Environment-Mediated)

**Description:** Agents communicate indirectly through environment modifications.

**Examples from Nature:**
- Ants leaving pheromone trails
- Bees performing waggle dances
- Termites building mounds

**Digital Implementation:**
- Agents leave "markers" in virtual space
- Other agents sense and react to markers
- No direct communication required

**Advantages:**
- Truly decentralized
- Scales to large numbers
- No communication overhead
- Emergent coordination

**Disadvantages:**
- Indirect and slow
- Hard to guarantee behavior
- Environment-dependent
- Complex to debug

**Application to MineWright:**
```java
public class StigmergicMarkers {
    private final Map<BlockPos, Marker> markers;

    public void placeMarker(BlockPos pos, MarkerType type, UUID agentId) {
        Marker marker = new Marker(type, agentId, System.currentTimeMillis());
        markers.put(pos, marker);

        // Marker decays over time
        scheduleDecay(pos, marker);
    }

    public List<Marker> getNearbyMarkers(BlockPos center, int radius) {
        return markers.entrySet().stream()
            .filter(e -> e.getKey().distSqr(center) <= radius * radius)
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    private void scheduleDecay(BlockPos pos, Marker marker) {
        // Remove marker after timeout
        scheduler.schedule(() -> {
            markers.remove(pos);
        }, marker.getLifetime(), TimeUnit.SECONDS);
    }
}

// Agent usage
public class WorkerAgent {
    public void onBlockMined(BlockPos pos) {
        // Leave "resource depleted" marker
        markers.placeMarker(pos, MarkerType.RESOURCE_DEPLETED, getId());
    }

    public BlockPos findMiningTarget() {
        // Check for nearby markers
        List<Marker> nearbyMarkers = markers.getNearbyMarkers(getPosition(), 50);

        // Avoid depleted areas
        // Follow trails to resources
        // ...
    }
}
```

---

## Case Studies

### 1. RTS Game Worker Coordination (StarCraft, Age of Empires)

**Challenge:** Coordinate 50-100 workers across resource gathering, building, and combat.

**Solutions:**

**a. Worker Allocation (StarCraft)**
```python
# Python-like pseudocode from StarCraft AI
def assign_workers(workers, tasks):
    # Sort workers by proximity to each task
    for task in tasks:
        best_worker = None
        best_distance = float('inf')

        for worker in workers:
            if not worker.is_assigned():
                distance = worker.distance_to(task.location)
                if distance < best_distance:
                    best_distance = distance
                    best_worker = worker

        if best_worker:
            best_worker.assign(task)
```

**b. Resource Splitting (Age of Empires)**
```python
# Assign workers to different resource patches
def distribute_gatherers(workers, resource_patches):
    workers_per_patch = len(workers) // len(resource_patches)

    for i, patch in enumerate(resource_patches):
        start = i * workers_per_patch
        end = start + workers_per_patch if i < len(resource_patches) - 1 else len(workers)

        for worker in workers[start:end]:
            worker.assign_target(patch)
```

**c. Dynamic Rebalancing**
```python
# Reassign idle workers
def check_worker_status(workers):
    idle_workers = [w for w in workers if w.is_idle()]

    for worker in idle_workers:
        # Find most urgent task
        urgent_task = find_urgent_task()
        worker.assign(urgent_task)
```

**Key Takeaways for MineWright:**
- Spatial distribution reduces collision
- Idle workers should be reassigned immediately
- Distance-based assignment minimizes travel time
- Resource patches should have worker capacity limits

---

### 2. Swarm Robotics

**Challenge:** Coordinate 10-100 simple robots for exploration, coverage, or retrieval.

**Approaches:**

**a. Boid Flocking (Reynolds, 1986)**
```
Three simple rules produce complex flocking behavior:

1. SEPARATION: Steer to avoid crowding local flockmates
2. ALIGNMENT: Steer towards average heading of local flockmates
3. COHESION: Steer to move towards average position of local flockmates
```

**Implementation:**
```java
public class BoidFlocking {
    public Vector3d calculateSeparation(List<Agent> nearbyAgents) {
        Vector3d steer = new Vector3d();
        int count = 0;

        for (Agent other : nearbyAgents) {
            double distance = getPosition().distance(other.getPosition());

            if (distance < SEPARATION_RADIUS && distance > 0) {
                Vector3d diff = getPosition().subtract(other.getPosition());
                diff = diff.normalize();
                diff = diff.divide(distance); // Weight by distance
                steer = steer.add(diff);
                count++;
            }
        }

        if (count > 0) {
            steer = steer.divide(count);
            steer = steer.normalize().multiply(MAX_SPEED).subtract(velocity);
            steer = truncate(steer, MAX_FORCE);
        }

        return steer;
    }

    public Vector3d calculateAlignment(List<Agent> nearbyAgents) {
        Vector3d sum = new Vector3d();
        int count = 0;

        for (Agent other : nearbyAgents) {
            double distance = getPosition().distance(other.getPosition());

            if (distance < NEIGHBOR_RADIUS) {
                sum = sum.add(other.getVelocity());
                count++;
            }
        }

        if (count > 0) {
            sum = sum.divide(count);
            sum = sum.normalize().multiply(MAX_SPEED);
            Vector3d steer = sum.subtract(velocity);
            steer = truncate(steer, MAX_FORCE);
            return steer;
        }

        return new Vector3d();
    }

    public Vector3d calculateCohesion(List<Agent> nearbyAgents) {
        Vector3d sum = new Vector3d();
        int count = 0;

        for (Agent other : nearbyAgents) {
            double distance = getPosition().distance(other.getPosition());

            if (distance < NEIGHBOR_RADIUS) {
                sum = sum.add(other.getPosition());
                count++;
            }
        }

        if (count > 0) {
            sum = sum.divide(count);
            return seek(sum); // Seek target position
        }

        return new Vector3d();
    }
}
```

**Application to MineWright:**
- Workers maintain separation to avoid collision
- Workers align movement when moving to same area
- Workers stay cohesive when performing group tasks

---

**b. Gradient-Based Navigation**
```java
public class GradientField {
    private final Map<BlockPos, Double> field;

    public void setAttraction(BlockPos pos, double strength) {
        field.put(pos, strength);
    }

    public void setRepulsion(BlockPos pos, double strength) {
        field.put(pos, -strength);
    }

    public Vector3d getGradient(BlockPos pos) {
        Vector3d gradient = new Vector3d();

        for (Map.Entry<BlockPos, Double> entry : field.entrySet()) {
            BlockPos source = entry.getKey();
            double strength = entry.getValue();

            Vector3d direction = new Vector3d(
                source.getX() - pos.getX(),
                source.getY() - pos.getY(),
                source.getZ() - pos.getZ()
            );

            double distance = direction.length();
            direction = direction.normalize();

            // Strength decays with distance
            double magnitude = strength / (1.0 + distance * 0.1);

            gradient = gradient.add(direction.multiply(magnitude));
        }

        return gradient;
    }
}
```

---

### 3. Multi-Agent Reinforcement Learning (AlphaStar)

**Challenge:** Learn to coordinate multiple units in complex RTS environments.

**AlphaStar Approach (2019):**

**Architecture:**
```
┌─────────────────────────────────────────────────────────┐
│                 Centralized Critic                       │
│            (Evaluates team performance)                  │
└────────────────────┬────────────────────────────────────┘
                     │
         ┌───────────┼───────────┐
         ▼           ▼           ▼
    ┌─────────┐ ┌─────────┐ ┌─────────┐
    │  Actor  │ │  Actor  │ │  Actor  │
    │ Unit 1  │ │ Unit 2  │ │ Unit 3  │
    └─────────┘ └─────────┘ └─────────┘
```

**Key Techniques:**

1. **Centralized Training, Decentralized Execution**
   - Training: Global critic sees all unit states
   - Execution: Each unit actor only sees local state

2. **Coordination Graphs**
   - Units represented as nodes in graph
   - Edges represent coordination dependencies
   - Payoff functions for coordinated actions

3. **Attention Mechanisms**
   - Units attend to relevant teammates
   - Dynamic attention based on context

**Application to MineWright:**
```java
// Simplified coordination graph
public class CoordinationGraph {
    private final Map<UUID, List<UUID>> dependencies;
    private final Map<String, Double> payoffFunctions;

    public double getCoordinationBenefit(UUID agent1, UUID agent2, Action a1, Action a2) {
        // Calculate payoff for coordinated actions
        String key = a1.getType() + ":" + a2.getType();
        return payoffFunctions.getOrDefault(key, 0.0);
    }

    public List<Action> selectCoordinatedActions(Map<UUID, List<Action>> candidateActions) {
        // Select action combination that maximizes total payoff
        // Using greedy approximation or message passing
    }
}
```

---

## Application to MineWright

### Current Architecture Analysis

**Existing Components (from code audit):**

1. **OrchestratorService** - Hierarchical foreman-worker coordination
2. **CommunicationBus** - Direct messaging and pub/sub
3. **ContractNetProtocol** - Market-based bidding (infrastructure exists, not fully integrated)
4. **AgentMessage** - Message format for inter-agent communication
5. **BlackboardEntry** - Shared knowledge structure
6. **CapabilityRegistry** - Agent capability tracking

**Current Flow:**
```
Human Command
    │
    ▼
LLM Planning (Foreman)
    │
    ▼
Task Decomposition
    │
    ▼
Round-Robin Assignment (OrchestratorService)
    │
    ├─> Worker 1
    ├─> Worker 2
    └─> Worker 3
```

**Limitations:**
- No consideration of worker capability or proximity
- No worker input on task assignment
- No dynamic rebalancing based on progress
- No spatial partitioning for collision avoidance

---

### Recommended Enhanced Architecture

**Phase 1: Contract Net Integration (Immediate)**

```
Human Command
    │
    ▼
LLM Planning (Foreman)
    │
    ▼
Task Decomposition
    │
    ▼
Contract Net Bidding
    │
    ├────────────────┐
    │                │
    ▼                ▼
Worker Bids      Bid Collection
    │                │
    ▼                ▼
Best Bid Selection
    │
    ▼
Task Assignment to Winner
```

**Implementation:**
```java
public class EnhancedOrchestratorService {
    private final ContractNetProtocol contractNet;

    public String processHumanCommand(ResponseParser.ParsedResponse response,
                                     Collection<ForemanEntity> availableSteves) {
        String planId = UUID.randomUUID().toString().substring(0, 8);

        // Decompose into tasks
        List<Task> tasks = response.getTasks();

        // Announce each task for bidding
        for (Task task : tasks) {
            String announcementId = contractNet.announceTask(
                task,
                foremanId,
                Duration.ofSeconds(10) // 10 second bidding window
            );

            // Store announcement for tracking
            pendingAnnouncements.put(announcementId, task);
        }

        // Wait for bids and award contracts
        scheduler.schedule(() -> {
            awardContracts(planId);
        }, 12, TimeUnit.SECONDS);

        return planId;
    }

    private void awardContracts(String planId) {
        for (String announcementId : pendingAnnouncements.keySet()) {
            List<TaskBid> bids = contractNet.getContractNetManager().getBids(announcementId);

            if (bids.isEmpty()) {
                // No bids - foreman takes task
                Task task = pendingAnnouncements.get(announcementId);
                foremanAssignTask(task);
            } else {
                // Select winner
                TaskBid winner = contractNet.getAwardSelector()
                    .selectBestBid(bids);

                // Award contract
                contractNet.getContractNetManager()
                    .awardContract(announcementId, winner);

                // Send assignment
                sendTaskAssignment(winner.bidderId(),
                    pendingAnnouncements.get(announcementId));
            }
        }
    }
}
```

---

**Phase 2: Spatial Partitioning (Short-term)**

```
World Partitioned into Zones:
┌─────────┬─────────┬─────────┐
│ Zone A  │ Zone B  │ Zone C  │
│ Crew 1  │ Crew 2  │ Crew 3  │
└─────────┼─────────┼─────────┘
│ Zone D  │ Zone E  │ Zone F  │
│ Crew 4  │ Crew 5  │ Crew 6  │
└─────────┴─────────┴─────────┘

Benefits:
- Workers in different zones never collide
- Tasks assigned to nearest zone
- Natural parallel work distribution
```

**Implementation:**
```java
public class SpatialPartition {
    private final int zoneSize;
    private final Map<ZoneId, CrewZone> zones;

    public CrewZone getZone(BlockPos pos) {
        int zoneX = pos.getX() / zoneSize;
        int zoneZ = pos.getZ() / zoneSize;
        ZoneId zoneId = new ZoneId(zoneX, zoneZ);
        return zones.getOrDefault(zoneId, createZone(zoneId));
    }

    public List<UUID> getWorkersInZone(BlockPos pos) {
        return getZone(pos).getWorkers();
    }

    public void assignWorkerToZone(UUID workerId, BlockPos preferredLocation) {
        CrewZone zone = getZone(preferredLocation);
        zone.addWorker(workerId);
    }
}

public class CrewZone {
    private final ZoneId zoneId;
    private final Set<UUID> workers;
    private final BlockPos center;
    private final Queue<Task> taskQueue;

    public void addTask(Task task) {
        // Add task to this zone's queue
        taskQueue.offer(task);
    }

    public void assignTasks() {
        // Distribute tasks to workers in this zone
        while (!taskQueue.isEmpty() && hasIdleWorkers()) {
            Task task = taskQueue.poll();
            UUID worker = getIdleWorker();
            assignTask(worker, task);
        }
    }
}
```

---

**Phase 3: Collision Avoidance (Medium-term)**

**Approach 1: Reservation System**
```java
public class SpaceReservation {
    private final Map<BlockPos, UUID> reservations;
    private final Map<BlockPos, Long> expirationTimes;

    public boolean reserve(UUID agentId, BlockPos pos, long duration) {
        UUID currentOwner = reservations.get(pos);

        if (currentOwner == null || currentOwner.equals(agentId)) {
            reservations.put(pos, agentId);
            expirationTimes.put(pos, System.currentTimeMillis() + duration);
            return true;
        }

        // Check if reservation expired
        if (expirationTimes.get(pos) < System.currentTimeMillis()) {
            reservations.put(pos, agentId);
            expirationTimes.put(pos, System.currentTimeMillis() + duration);
            return true;
        }

        return false; // Space is reserved by someone else
    }

    public void release(UUID agentId, BlockPos pos) {
        if (reservations.get(pos) != null &&
            reservations.get(pos).equals(agentId)) {
            reservations.remove(pos);
            expirationTimes.remove(pos);
        }
    }
}
```

**Approach 2: Collision Prediction**
```java
public class CollisionPredictor {
    private final Map<UUID, AgentState> agentStates;

    public boolean willCollide(UUID agent1, UUID agent2, int lookAheadTicks) {
        AgentState state1 = agentStates.get(agent1);
        AgentState state2 = agentStates.get(agent2);

        // Predict future positions
        List<BlockPos> path1 = predictPath(state1, lookAheadTicks);
        List<BlockPos> path2 = predictPath(state2, lookAheadTicks);

        // Check for intersection
        for (int i = 0; i < path1.size() && i < path2.size(); i++) {
            if (path1.get(i).equals(path2.get(i))) {
                return true; // Collision predicted
            }
        }

        return false;
    }

    private List<BlockPos> predictPath(AgentState state, int ticks) {
        // Simple linear prediction
        List<BlockPos> path = new ArrayList<>();
        BlockPos pos = state.getPosition();
        Vec3 velocity = state.getVelocity();

        for (int i = 0; i < ticks; i++) {
            pos = pos.offset(
                (int) Math.round(velocity.x),
                (int) Math.round(velocity.y),
                (int) Math.round(velocity.z)
            );
            path.add(pos);
        }

        return path;
    }
}
```

---

### Priority-Based Task Selection

**Workers should bid based on:**

1. **Distance** - Closer tasks preferred
2. **Capability** - Skill level for task type
3. **Load** - Current assignment count
4. **Priority** - Task urgency
5. **Resources** - Required tool availability

**Composite Bid Formula:**
```java
public class BidCalculator {
    public double calculateBid(Task task, AgentState agent) {
        double distanceScore = calculateDistanceScore(task, agent);
        double capabilityScore = calculateCapabilityScore(task, agent);
        double loadScore = calculateLoadScore(agent);
        double priorityScore = task.getPriority();
        double resourceScore = calculateResourceScore(task, agent);

        // Weighted combination
        return distanceScore * 0.3 +
               capabilityScore * 0.3 +
               loadScore * 0.2 +
               priorityScore * 0.1 +
               resourceScore * 0.1;
    }

    private double calculateDistanceScore(Task task, AgentState agent) {
        double distance = agent.getPosition().distSqr(task.getTargetLocation());
        return 100.0 / (1.0 + distance / 100.0); // 0-100 scale
    }

    private double calculateCapabilityScore(Task task, AgentState agent) {
        String skillKey = task.getAction();
        double skillLevel = agent.getSkillLevel(skillKey);
        return skillLevel * 10.0; // 0-100 scale (0-10 skill)
    }

    private double calculateLoadScore(AgentState agent) {
        int taskCount = agent.getActiveTaskCount();
        return 100.0 / (1.0 + taskCount); // Decreases with more tasks
    }

    private double calculateResourceScore(Task task, AgentState agent) {
        // Check if agent has required tools
        if (task.requiresTool("pickaxe") && !agent.hasTool("pickaxe")) {
            return 0.0;
        }
        return 100.0;
    }
}
```

---

## Code Patterns

### Message Format Standardization

**Standard Message Structure:**
```java
public class AgentMessage {
    public enum Type {
        // Coordination
        TASK_ANNOUNCEMENT,
        TASK_BID,
        CONTRACT_AWARD,
        TASK_ASSIGNMENT,

        // Progress
        TASK_PROGRESS,
        TASK_COMPLETE,
        TASK_FAILED,

        // Status
        STATUS_REPORT,
        HELP_REQUEST,
        IDLE_NOTIFICATION,

        // Discovery
        AGENT_DISCOVER,
        AGENT_LEAVE,

        // Communication
        CHAT_MESSAGE,
        ALERT
    }

    public enum Priority {
        LOW, NORMAL, HIGH, URGENT
    }

    private final Type type;
    private final UUID senderId;
    private final String senderName;
    private final UUID recipientId; // null for broadcast
    private final String content;
    private final Map<String, Object> payload;
    private final Priority priority;
    private final long timestamp;
    private final UUID correlationId; // For request/response
    private final UUID messageId;

    // Builder pattern for construction
    public static class Builder {
        private Type type;
        private UUID senderId;
        private String senderName;
        private UUID recipientId;
        private String content;
        private Map<String, Object> payload = new HashMap<>();
        private Priority priority = Priority.NORMAL;
        private UUID correlationId;

        public Builder type(Type type) { this.type = type; return this; }
        public Builder sender(UUID id, String name) {
            this.senderId = id;
            this.senderName = name;
            return this;
        }
        public Builder recipient(UUID id) { this.recipientId = id; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder payload(String key, Object value) {
            this.payload.put(key, value);
            return this;
        }
        public Builder priority(Priority priority) { this.priority = priority; return this; }
        public Builder correlationId(UUID id) { this.correlationId = id; return this; }

        public AgentMessage build() {
            return new AgentMessage(this);
        }
    }

    // Convenience factory methods
    public static AgentMessage taskAnnouncement(UUID sender, String taskJson) {
        return new Builder()
            .type(Type.TASK_ANNOUNCEMENT)
            .sender(sender, "System")
            .recipient(null) // Broadcast
            .content("New task available")
            .payload("task", taskJson)
            .build();
    }

    public static AgentMessage taskBid(UUID sender, UUID recipient,
                                       String announcementId, double bidAmount) {
        return new Builder()
            .type(Type.TASK_BID)
            .sender(sender, "Worker")
            .recipient(recipient)
            .content("Bid for task")
            .payload("announcementId", announcementId)
            .payload("bidAmount", bidAmount)
            .build();
    }
}
```

---

### Synchronization Primitives

**Distributed Lock:**
```java
public class DistributedLock {
    private final Map<String, LockHolder> locks;
    private final long lockTimeout;

    public boolean acquire(String lockKey, UUID agentId, long timeout) {
        LockHolder current = locks.get(lockKey);

        // Check if lock is free or expired
        if (current == null ||
            current.getExpirationTime() < System.currentTimeMillis() ||
            current.getHolder().equals(agentId)) {

            locks.put(lockKey, new LockHolder(agentId,
                System.currentTimeMillis() + timeout));
            return true;
        }

        return false;
    }

    public void release(String lockKey, UUID agentId) {
        LockHolder current = locks.get(lockKey);
        if (current != null && current.getHolder().equals(agentId)) {
            locks.remove(lockKey);
        }
    }

    private static class LockHolder {
        private final UUID holder;
        private final long expirationTime;

        LockHolder(UUID holder, long expirationTime) {
            this.holder = holder;
            this.expirationTime = expirationTime;
        }
    }
}
```

**Barrier Synchronization:**
```java
public class Barrier {
    private final int expectedParties;
    private final AtomicInteger waiting;
    private final CountDownLatch latch;
    private final Runnable barrierAction;

    public Barrier(int parties, Runnable action) {
        this.expectedParties = parties;
        this.waiting = new AtomicInteger(0);
        this.latch = new CountDownLatch(1);
        this.barrierAction = action;
    }

    public boolean await(UUID agentId, long timeout) throws InterruptedException {
        int count = waiting.incrementAndGet();

        if (count == expectedParties) {
            // Last agent to arrive - execute action and release
            if (barrierAction != null) {
                barrierAction.run();
            }
            latch.countDown();
        }

        return latch.await(timeout, TimeUnit.MILLISECONDS);
    }

    public void reset() {
        waiting.set(0);
        // New latch for next cycle
    }
}
```

---

### Conflict Resolution

**Priority-Based Resolution:**
```java
public class ConflictResolver {
    public Resolution resolveConflict(List<TaskBid> tiedBids) {
        // Primary: Higher priority wins
        List<TaskBid> byPriority = tiedBids.stream()
            .sorted(Comparator.comparing(b -> b.getTaskPriority()).reversed())
            .collect(Collectors.toList());

        if (byPriority.size() == 1) {
            return Resolution.selected(byPriority.get(0), "Higher priority");
        }

        // Secondary: Lower bid amount (cost) wins
        List<TaskBid> byCost = byPriority.stream()
            .sorted(Comparator.comparing(TaskBid::getBidAmount))
            .collect(Collectors.toList());

        if (byCost.size() == 1) {
            return Resolution.selected(byCost.get(0), "Lower cost");
        }

        // Tertiary: Random selection
        TaskBid selected = byCost.get(ThreadLocalRandom.current().nextInt(byCost.size()));
        return Resolution.selected(selected, "Random tiebreaker");
    }

    public static class Resolution {
        private final TaskBid selectedBid;
        private final String reasoning;

        private Resolution(TaskBid selectedBid, String reasoning) {
            this.selectedBid = selectedBid;
            this.reasoning = reasoning;
        }

        public static Resolution selected(TaskBid bid, String reasoning) {
            return new Resolution(bid, reasoning);
        }
    }
}
```

---

**Distance-Based Resolution:**
```java
public class ProximityResolver {
    public Resolution resolveByProximity(List<TaskBid> tiedBids, BlockPos taskLocation) {
        TaskBid closest = tiedBids.stream()
            .min(Comparator.comparing(b -> b.getAgentPosition().distSqr(taskLocation)))
            .orElse(tiedBids.get(0));

        double distance = Math.sqrt(closest.getAgentPosition().distSqr(taskLocation));

        return Resolution.selected(closest,
            String.format("Closest to target (%.1f blocks)", distance));
    }
}
```

---

## Implementation Roadmap

### Phase 1: Contract Net Integration (Week 1-2)

**Tasks:**
1. Integrate ContractNetProtocol with OrchestratorService
2. Implement worker bid calculation logic
3. Add task announcement to agent message handler
4. Implement bid submission and award notification
5. Add tests for bidding scenarios

**Files to Modify:**
- `OrchestratorService.java` - Use Contract Net for task distribution
- `ForemanEntity.java` - Handle task announcements and bids
- `TaskBid.java` - Add bid calculation helper methods
- `AgentMessage.java` - Add bidding-related message types

**Tests:**
- Single worker bidding
- Multiple workers bidding
- No bids scenario
- Tie resolution
- Bid rejection

---

### Phase 2: Spatial Partitioning (Week 3-4)

**Tasks:**
1. Create SpatialPartition class
2. Implement CrewZone for zone management
3. Add zone-based task assignment
4. Integrate with pathfinding for zone-aware paths
5. Add zone visualization for debugging

**New Files:**
- `coordination/SpatialPartition.java`
- `coordination/CrewZone.java`
- `coordination/ZoneId.java`
- `pathfinding/ZoneAwarePathfinder.java`

**Tests:**
- Zone calculation from position
- Worker assignment to zones
- Zone-based task distribution
- Zone boundary crossing

---

### Phase 3: Collision Avoidance (Week 5-6)

**Tasks:**
1. Implement SpaceReservation system
2. Add CollisionPredictor for path conflicts
3. Integrate with movement actions
4. Add collision resolution (repath, wait)
5. Visual debugging for reserved spaces

**New Files:**
- `coordination/SpaceReservation.java`
- `coordination/CollisionPredictor.java`
- `action/ReservationAwareMoveAction.java`

**Tests:**
- Single block reservation
- Path reservation
- Collision detection
- Collision resolution
- Reservation expiration

---

### Phase 4: Enhanced Blackboard (Week 7-8)

**Tasks:**
1. Implement reactive blackboard with notifications
2. Add knowledge areas (resources, dangers, opportunities)
3. Implement subscription mechanism
4. Add data freshness tracking
5. Integrate with agent decision making

**Files to Modify:**
- `blackboard/BlackboardEntry.java` - Add reactivity
- `blackboard/KnowledgeArea.java` - Add areas
- New: `blackboard/ReactiveBlackboard.java`
- New: `blackboard/KnowledgeSubscription.java`

**Tests:**
- Write notification
- Subscription triggering
- Data expiration
- Multiple subscribers
- Cross-agent knowledge sharing

---

## References

### Academic Papers

1. **Smith, R. G. (1980).** "The Contract Net Protocol: High-Level Communication and Control in a Distributed Problem Solver." *IEEE Transactions on Computers*, C-29(12), 1104-1113.

2. **Vinyals, O., et al. (2019).** "Grandmaster level in StarCraft II using multi-agent reinforcement learning." *Nature*, 575, 350-354.

3. **Parunak, H. V. D. (1997).** "Go to the ant: Engineering principles from natural multi-agent systems." *Annals of Operations Research*, 75, 69-101.

4. **Reynolds, C. W. (1987).** "Flocks, herds and schools: A distributed behavioral model." *SIGGRAPH '87 Proceedings*, 25-34.

### Game AI References

5. **Buckland, M. (2004).** *Programming Game AI by Example.* Wordware Publishing.

6. **Champandard, A. J. (2003).** *AI Game Development: Synthetic Creatures with Learning and Reactive Behaviors.* New Riders.

### Online Resources

7. **CSDN - Multi-Agent Collaboration Patterns** (2025)
   - https://blog.csdn.net/Peter_Changyb/article/details/158315811

8. **LinkedIn - Orchestrating Multi-Agent Systems** (2025)
   - https://www.linkedin.com/pulse/orchestrating-multi-agent-systems-technical-patterns-complex-kiran-b8o2f

9. **CSDN - Swarm Robotics Coordination** (2025)
   - https://blog.csdn.net/lpfasd123/article/details/151704940

10. **arXiv - Automatic Design of Stigmergy-Based Behaviors** (2024)
    - https://www.nature.com/articles/s41586-024-05755-5

### MineWright Codebase

11. `OrchestratorService.java` - Current hierarchical coordination implementation
12. `ContractNetProtocol.java` - Market-based bidding infrastructure
13. `CommunicationBus.java` - Inter-agent messaging system
14. `BlackboardEntry.java` - Shared knowledge structure

---

## Appendix: Quick Reference

### Message Types Quick Reference

| Type | Direction | Purpose |
|------|-----------|---------|
| `TASK_ANNOUNCEMENT` | Foreman → Workers | Broadcast available task |
| `TASK_BID` | Worker → Foreman | Submit bid for task |
| `CONTRACT_AWARD` | Foreman → Worker | Notify winner |
| `TASK_ASSIGNMENT` | Foreman → Worker | Assign task with details |
| `TASK_PROGRESS` | Worker → Foreman | Report completion percentage |
| `TASK_COMPLETE` | Worker → Foreman | Notify task finished |
| `TASK_FAILED` | Worker → Foreman | Report failure with reason |
| `IDLE_NOTIFICATION` | Worker → Foreman | Worker ready for new task |
| `HELP_REQUEST` | Worker → Foreman | Request assistance |

### Coordination Pattern Selection Guide

| Scenario | Recommended Pattern | Rationale |
|----------|-------------------|-----------|
| 2-5 agents | Centralized | Simple, no overhead |
| 5-20 agents | Hierarchical | Natural team structure |
| 20+ agents | Hierarchical + Spatial | Scalable, reduces collision |
| Dynamic agent count | Decentralized | Fault-tolerant |
| Complex tasks | Contract Net | Capability matching |
| Simple tasks | Priority Queue | Fast, low overhead |
| Shared resource access | Token Passing | Fair, prevents conflict |
| Knowledge sharing | Blackboard | Asynchronous, flexible |

### Bid Formula Weights

```java
// Recommended weights for different scenarios

// Standard operation (balanced)
distance: 0.3, capability: 0.3, load: 0.2, priority: 0.1, resource: 0.1

// Speed-focused (minimize travel time)
distance: 0.5, capability: 0.2, load: 0.1, priority: 0.1, resource: 0.1

// Quality-focused (use best workers)
distance: 0.1, capability: 0.6, load: 0.1, priority: 0.1, resource: 0.1

// Fairness-focused (balance workload)
distance: 0.2, capability: 0.2, load: 0.4, priority: 0.1, resource: 0.1
```

---

**End of Document**

This research document provides a comprehensive foundation for implementing advanced multi-agent coordination in MineWright. The patterns and code examples are directly applicable to the existing codebase and can be incrementally integrated following the implementation roadmap.

For questions or clarifications, consult the referenced academic papers and existing MineWright coordination classes.
