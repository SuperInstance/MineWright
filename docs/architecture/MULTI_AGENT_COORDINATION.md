# Multi-Agent Coordination

## Overview

The Multi-Agent Coordination system enables multiple autonomous agents to work together effectively through two key mechanisms: the Contract Net Protocol for task allocation and the Blackboard pattern for shared knowledge. This architecture allows agents to bid on tasks, collaborate on complex projects, and share information without centralized control.

### Key Benefits

- **Decentralized Task Allocation**: Agents self-organize through competitive bidding
- **Shared Knowledge**: Blackboard enables information sharing across all agents
- **Dynamic Collaboration**: Agents can form teams for complex tasks
- **Fault Tolerance**: No single point of failure
- **Scalability**: Easy to add/remove agents from the system

## Architecture

```
User Command: "coordinate crew to build a castle"
                    |
                    v
         Task Decomposition (LLM)
                    |
                    +---> Task 1: Foundation
                    +---> Task 2: Walls
                    +---> Task 3: Towers
                    +---> Task 4: Roof
                    |
                    v
         ContractNetManager.announceTask()
                    |
                    +------------------------+
                    |                        |
                    v                        v
            Agent 1 Bids              Agent 2 Bids
            (score=0.8)               (score=0.6)
                    |                        |
                    +------------------------+
                                |
                                v
                    ContractNetManager.awardContract()
                                |
                                v
                       Agent 1 Executes Task
                                |
                                v
                        Blackboard.post(progress)
                                |
                                +--------> Agent 2 Sees Progress
                                +--------> Agent 3 Sees Progress
                                |
                                v
                          Task Complete
```

## Contract Net Protocol

### Protocol Flow

```
Manager                  Agent
  |                        |
  |-- announceTask ------> |
  |    (task, reqs)       |
  |                        |-- evaluate capability
  |                        |-- calculate bid value
  |                        |
  | <-- submitBid ---------|
  |    (score, time)      |
  |                        |
  |-- evaluate all bids   |
  |                        |
  |-- awardContract ------>| (winner only)
  |                        |
  |                        |-- execute task
  |                        |
  | <-- taskComplete ------|
  |    (results)          |
```

### Components

#### TaskAnnouncement

Broadcast when a task needs allocation.

```java
public record TaskAnnouncement(
    String announcementId,     // Unique identifier
    Task task,                  // Task to be allocated
    UUID requesterId,           // Agent requesting the task
    long deadline,              // Bid submission deadline
    Map<String, Object> requirements  // Capability requirements
) {
    // Requirements map keys:
    // - "skills": Set<String> of required skills
    // - "maxDistance": Double, maximum distance from task
    // - "tools": Set<String> of required tools
    // - "minProficiency": Double, minimum skill level (0.0-1.0)
    // - "priority": Task priority for sorting
}
```

#### TaskBid

Agent's bid for an announced task.

```java
public class TaskBid implements Comparable<TaskBid> {
    private final String announcementId;     // Responding to this announcement
    private final UUID bidderId;              // Agent submitting bid
    private final double score;               // How well agent can do task (0.0-1.0)
    private final long estimatedTime;         // Expected completion (ms)
    private final double confidence;          // Confidence in success (0.0-1.0)
    private final Map<String, Object> capabilities;  // Agent's capabilities

    // Bid value for comparison (higher is better)
    public double getBidValue() {
        double timeFactor = estimatedTime / 1000.0;
        if (timeFactor < 1.0) timeFactor = 1.0;
        return (score * confidence) / timeFactor;
    }

    // Comparison: higher bid value wins, ties broken by time
    public int compareTo(TaskBid other) {
        int comparison = Double.compare(other.getBidValue(), this.getBidValue());
        if (comparison != 0) return comparison;
        return Long.compare(this.estimatedTime, other.estimatedTime);
    }
}
```

#### ContractNetManager

Manages the bidding process.

```java
public class ContractNetManager {
    private final Map<String, ContractNegotiation> negotiations;

    // Announce a task for bidding
    public String announceTask(Task task, UUID requesterId, long deadlineMs) {
        TaskAnnouncement announcement = TaskAnnouncement.builder()
            .task(task)
            .requesterId(requesterId)
            .deadline(System.currentTimeMillis() + deadlineMs)
            .build();

        negotiations.put(announcement.announcementId(), new ContractNegotiation(announcement));
        return announcement.announcementId();
    }

    // Submit a bid
    public boolean submitBid(TaskBid bid) {
        ContractNegotiation negotiation = negotiations.get(bid.announcementId());

        // Validate bid
        if (negotiation == null || negotiation.isClosed()) return false;
        if (negotiation.getAnnouncement().isExpired()) return false;
        if (alreadyBid(negotiation, bid.bidderId())) return false;

        // Add bid
        negotiations.put(bid.announcementId(), negotiation.withBid(bid));
        return true;
    }

    // Award contract to best bidder
    public boolean awardContract(String announcementId, TaskBid winner) {
        ContractNegotiation negotiation = negotiations.get(announcementId);

        ContractNegotiation updated = negotiation
            .withWinningBid(winner)
            .withState(ContractState.AWARDED);

        negotiations.put(announcementId, updated);
        return true;
    }

    // Get all bids for an announcement
    public List<TaskBid> getBids(String announcementId) {
        ContractNegotiation negotiation = negotiations.get(announcementId);
        return negotiation != null ? negotiation.getBids() : List.of();
    }

    // Select winning bid
    public Optional<TaskBid> selectWinner(String announcementId) {
        ContractNegotiation negotiation = negotiations.get(announcementId);
        return negotiation.getBids().stream().max(TaskBid::compareTo);
    }

    // Award to best bidder automatically
    public Optional<TaskBid> awardToBestBidder(String announcementId) {
        Optional<TaskBid> winner = selectWinner(announcementId);
        if (winner.isPresent() && awardContract(announcementId, winner.get())) {
            return winner;
        }
        return Optional.empty();
    }
}
```

### Contract States

```java
public enum ContractState {
    ANNOUNCED,   // Announcement created, waiting for bids
    EVALUATING,  // Bidding closed, evaluating winner
    AWARDED,     // Contract awarded, agent executing
    COMPLETED,   // Task completed successfully
    FAILED,      // Task failed or cancelled
    EXPIRED      // Bidding deadline expired with no bids
}
```

## Blackboard System

### Architecture

```
+--------------------------------------------------+
|                   Blackboard                     |
|                                                  |
|  +--------------+  +--------------+  +----------+  |
|  | WORLD_STATE  |  | AGENT_STATUS |  | THREATS  |  |
|  +--------------+  +--------------+  +----------+  |
|                                                  |
|  +--------------+  +--------------+  +----------+  |
|  | TASKS        |  | RESOURCES    |  | BUILD... |  |
|  +--------------+  +--------------+  +----------+  |
+--------------------------------------------------+
         |                    |                    |
         v                    v                    v
    +--------+           +--------+           +--------+
    | Agent1 |           | Agent2 |           | Agent3 |
    +--------+           +--------+           +--------+
```

### Knowledge Areas

```java
public enum KnowledgeArea {
    WORLD_STATE("world_state", 5000L),
        // Blocks, entities, biomes, environmental data
        // Update frequency: High (every tick)
        // Staleness tolerance: Low (world changes constantly)

    AGENT_STATUS("agent_status", 2000L),
        // Health, position, inventory, activity state
        // Update frequency: Medium (on state change)
        // Staleness tolerance: Low (affects coordination)

    TASKS("tasks", 10000L),
        // Active, pending, and completed tasks
        // Update frequency: Medium (on task state change)
        // Staleness tolerance: Medium (task queues change slowly)

    RESOURCES("resources", 15000L),
        // Available materials, storage locations, stockpiles
        // Update frequency: Low (on inventory change)
        // Staleness tolerance: Medium (resource counts change slowly)

    THREATS("threats", 1000L),
        // Hostile mobs, environmental dangers
        // Update frequency: High (immediate on detection)
        // Staleness tolerance: Very low (threats require immediate response)

    BUILD_PLANS("build_plans", 3000L),
        // Structure templates, build progress, construction goals
        // Update frequency: Medium (on build progress)
        // Staleness tolerance: Low (build plans must stay synchronized)

    PLAYER_PREFS("player_prefs", 60000L),
        // Player preferences, interaction history
        // Update frequency: Low (on player action)
        // Staleness tolerance: High (preferences change rarely)
}
```

### BlackboardEntry

Single piece of knowledge with metadata.

```java
public class BlackboardEntry<T> {
    private final String key;              // Unique key within area
    private final T value;                 // Knowledge value
    private final long timestamp;          // Creation time (ms)
    private final UUID sourceAgent;        // Agent who created entry
    private final double confidence;       // Confidence level (0.0-1.0)
    private final EntryType type;          // FACT, HYPOTHESIS, GOAL, CONSTRAINT

    public enum EntryType {
        FACT,       // Direct observation or verified information
        HYPOTHESIS, // Inferred or uncertain information
        GOAL,       // Shared objective or target state
        CONSTRAINT  // Rule or limitation on behavior
    }

    // Factory methods
    public static <T> BlackboardEntry<T> createFact(String key, T value, UUID sourceAgent) {
        return new BlackboardEntry<>(key, value, sourceAgent, 1.0, EntryType.FACT);
    }

    public static <T> BlackboardEntry<T> createHypothesis(String key, T value, UUID sourceAgent, double confidence) {
        return new BlackboardEntry<>(key, value, sourceAgent, confidence, EntryType.HYPOTHESIS);
    }

    public static <T> BlackboardEntry<T> createGoal(String key, T value, UUID sourceAgent) {
        return new BlackboardEntry<>(key, value, sourceAgent, 0.9, EntryType.GOAL);
    }

    public static <T> BlackboardEntry<T> createConstraint(String key, T value) {
        return new BlackboardEntry<>(key, value, null, 1.0, EntryType.CONSTRAINT);
    }
}
```

### Blackboard API

```java
public class Blackboard {
    // Post knowledge to an area
    public void post(KnowledgeArea area, String key, Object value, UUID sourceAgent, double confidence, EntryType type) {
        BlackboardEntry<?> entry = new BlackboardEntry<>(key, value, sourceAgent, confidence, type);
        areas.get(area).put(key, entry);
    }

    // Query a specific area
    public List<BlackboardEntry<?>> queryArea(KnowledgeArea area) {
        return new ArrayList<>(areas.get(area).values());
    }

    // Query by key pattern
    public List<BlackboardEntry<?>> queryPattern(KnowledgeArea area, String pattern) {
        return areas.get(area).entrySet().stream()
            .filter(e -> e.getKey().matches(pattern))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    // Subscribe to area changes
    public void subscribe(KnowledgeArea area, Consumer<BlackboardEntry<?>> listener) {
        subscribers.computeIfAbsent(area, k -> new ArrayList<>()).add(listener);
    }

    // Get a specific entry
    public Optional<BlackboardEntry<?>> get(KnowledgeArea area, String key) {
        return Optional.ofNullable(areas.get(area).get(key));
    }

    // Remove stale entries
    public int cleanup() {
        int cleaned = 0;
        for (KnowledgeArea area : KnowledgeArea.values()) {
            cleaned += cleanupArea(area, area.getDefaultMaxAgeMs());
        }
        return cleaned;
    }
}
```

## Agent Communication

### Message Types

```java
public class AgentMessage {
    private final String messageId;
    private final UUID senderId;
    private final UUID receiverId;      // null for broadcast
    private final MessageType type;
    private final String content;
    private final long timestamp;

    public enum MessageType {
        TASK_ANNOUNCEMENT,   // New task available
        TASK_BID,            // Bid on task
        TASK_AWARD,          // Task awarded
        TASK_COMPLETE,       // Task finished
        KNOWLEDGE_SHARE,     // Share information
        STATUS_UPDATE,       // Agent status change
        COORDINATION_REQUEST,// Request coordination
        COORDINATION_RESPONSE // Response to request
    }
}
```

### Communication Bus

```java
public class AgentCommunicationBus {
    // Send message to specific agent
    public void send(UUID receiverId, AgentMessage message) {
        queues.get(receiverId).add(message);
    }

    // Broadcast to all agents
    public void broadcast(AgentMessage message) {
        for (UUID agentId : queues.keySet()) {
            if (!agentId.equals(message.getSenderId())) {
                send(agentId, message);
            }
        }
    }

    // Receive messages for this agent
    public List<AgentMessage> receive(UUID agentId) {
        Queue<AgentMessage> queue = queues.get(agentId);
        List<AgentMessage> messages = new ArrayList<>();
        while (!queue.isEmpty()) {
            messages.add(queue.poll());
        }
        return messages;
    }

    // Subscribe to message types
    public void subscribe(MessageType type, UUID agentId, Consumer<AgentMessage> handler) {
        subscribers.computeIfAbsent(type, k -> new HashMap<>())
            .put(agentId, handler);
    }
}
```

## Example Workflows

### Scenario 1: Collaborative Building

```
User: "coordinate the crew to build a castle"

1. Task Decomposition
   - Foreman breaks into sub-tasks
   - Task 1: Foundation (100 blocks)
   - Task 2: Walls (4 walls, 20 blocks each)
   - Task 3: Towers (4 corners, 30 blocks each)
   - Task 4: Roof (50 blocks)

2. Contract Net Process
   - Foreman announces Task 1 (foundation)
   - Agents 1, 2, 3 submit bids
   - Agent 1 wins (closest to site, has stone)
   - Agent 1 executes, posts progress to blackboard

3. Concurrent Execution
   - While Agent 1 builds foundation, Task 2 announced
   - Agents 2 and 3 win wall tasks (different walls)
   - All agents post progress updates

4. Coordination via Blackboard
   - Agent 2 posts: "wall_north_complete"
   - Agent 3 sees: wall_north done, starts adjacent wall
   - No conflicts, no overlaps

5. Completion
   - All tasks complete
   - Castle finished in 1/4 the time
```

### Scenario 2: Resource Distribution

```
User: "distribute iron evenly among all agents"

1. Initial State (from Blackboard)
   - AGENT_STATUS: Agent1 has 64 iron, Agent2 has 0, Agent3 has 0
   - RESOURCES: Total iron = 64

2. Contract Net
   - Announce: "distribute_iron" task
   - Agents bid based on need
   - Agent2 bid: score=1.0 (no iron, high need)
   - Agent3 bid: score=1.0 (no iron, high need)
   - Agent1 bid: score=0.0 (has iron, no need)

3. Execution
   - Agent1 awarded distribution task
   - Gives 32 iron to Agent2
   - Gives 32 iron to Agent3
   - Posts to Blackboard: "iron_distributed"

4. Final State
   - AGENT_STATUS: All agents have 32 iron
   - Equal distribution achieved
```

### Scenario 3: Threat Response

```
Event: Zombie detected near base

1. Detection (Agent1)
   - Agent1 sees zombie at (100, 64, 200)
   - Posts to THREATS area:
     key="hostile_zombie_001"
     value={position: (100,64,200), health: 20, type: zombie}
     confidence=1.0, type=FACT

2. Notification (Blackboard)
   - All agents subscribed to THREATS receive update
   - Agent2, Agent3, Agent4 notified

3. Coordination
   - Agent2 (closest) bids on "attack_zombie_001"
   - Agent3 posts to blackboard: "I'll cover Agent2's position"
   - Agent4 posts: "I'll guard the base entrance"

4. Execution
   - Agent2 attacks and kills zombie
   - Posts to TASKS: "threat_eliminated=zombie_001"
   - Threat removed from blackboard

5. Recovery
   - Agents return to previous tasks
   - Coordination complete
```

## Configuration

Config file: `config/steve-common.toml`

```toml
[coordination.contract_net]
# Enable contract net protocol
enabled = true

# Default bid deadline (milliseconds)
defaultDeadline = 30000

# Minimum bids required before awarding
minBids = 1

# Maximum time to wait for bids before timeout
maxWaitTime = 60000

[coordination.blackboard]
# Enable blackboard system
enabled = true

# Maximum entry age before cleanup (milliseconds)
defaultMaxAge = 300000

# Cleanup interval (milliseconds)
cleanupInterval = 60000

# Maximum entries per area
maxEntriesPerArea = 1000

[coordination.communication]
# Enable agent communication
enabled = true

# Message queue size
queueSize = 100

# Message timeout (milliseconds)
messageTimeout = 30000
```

## Best Practices

1. **Bid Realistically**: Agents should calculate bids based on actual capability
2. **Share Proactively**: Post important information to blackboard immediately
3. **Subscribe Selectively**: Only subscribe to relevant knowledge areas
4. **Clean Up Regularly**: Remove stale entries to prevent memory bloat
5. **Handle Timeouts**: Always have timeout logic for contract negotiations
6. **Validate Capabilities**: Don't bid on tasks you can't complete
7. **Coordinate via Blackboard**: Use shared knowledge for implicit coordination
8. **Acknowledge Messages**: Confirm receipt of important messages

## Troubleshooting

### No Bids on Task

**Symptom**: Task announced but no agents bid

**Solutions**:
1. Check requirements are not too strict
2. Verify all agents are active and listening
3. Increase bid deadline
4. Check if agents have required skills/tools
5. Review task complexity - may need decomposition

### Blackboard Growing Too Large

**Symptom**: Memory usage increasing over time

**Solutions**:
1. Increase cleanup frequency
2. Reduce max age per area
3. Add max entries per area limit
4. Implement eviction policy (LRU, LFU)
5. Use more specific keys to reduce duplicates

### Agents Not Coordinating

**Symptom**: Agents working at cross-purposes

**Solutions**:
1. Ensure blackboard is being used for status updates
2. Check subscriptions are set up correctly
3. Verify communication bus is working
4. Add explicit coordination messages
5. Review contract net awards - are best agents winning?

## References

- **Inspired By**: Contract Net Protocol (Smith, 1980), Blackboard Pattern (Penny Nii, 1986)
- **Related**: `TaskAnnouncement`, `TaskBid`, `ContractNetManager`, `Blackboard`, `BlackboardEntry`, `KnowledgeArea`
- **See Also**: `AgentCommunicationBus`, `AgentMessage`
