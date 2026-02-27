# Multi-Agent Communication Protocols Research

**Date:** 2026-02-27
**Focus:** Inter-agent communication protocols for multi-agent systems
**Target:** Improvements for AgentCommunicationBus in MineWright AI Project

---

## Executive Summary

This document provides a comprehensive analysis of multi-agent communication protocols, standards, and patterns. The research covers modern protocols (MCP, A2A), classical standards (FIPA ACL), architectural patterns (Contract Net, Blackboard), and implementation best practices for message passing, priority handling, and failure recovery.

**Key Findings:**
1. **FIPA ACL** remains the gold standard for agent communication languages with 20 performative types
2. **Contract Net Protocol (CNP)** provides robust task allocation through market-based negotiation
3. **Blackboard Pattern** enables opportunistic problem-solving through shared state
4. **Publish-Subscribe** pattern offers loose coupling and flexibility
5. **Priority queues** are essential for handling urgent messages in time-sensitive systems

---

## Table of Contents

1. [Modern Communication Protocols (2024-2025)](#modern-protocols)
2. [FIPA ACL - Agent Communication Language](#fipa-acl)
3. [Contract Net Protocol](#contract-net)
4. [Blackboard Pattern](#blackboard)
5. [Message Passing Patterns](#message-passing)
6. [Priority and Urgency Handling](#priority-handling)
7. [Failure and Timeout Handling](#failure-handling)
8. [Recommendations for AgentCommunicationBus](#recommendations)

---

## Modern Communication Protocols (2024-2025) {#modern-protocols}

### MCP (Model Context Protocol)

**Developer:** Anthropic (2024)
**Purpose:** Unified communication standard for AI agents, analogous to HTTP for the web

**Key Features:**
- Standardized message format for agent-to-agent communication
- Request-response pattern with correlation IDs
- JSON-based message payloads
- Built-in timeout and retry mechanisms
- **Security:** MCP v1.1 (2026) added end-to-end encryption and authentication

**Architecture:**
```
Client Agent           Server Agent
    |                      |
    | --- Request ------>  |
    |                     (process)
    | <--- Response ----   |
```

### A2A (Agent-to-Agent) Protocol

**Developer:** Google (2024)
**Purpose:** Agent interoperability and decentralized exchange

**Key Features:**
- Game-theoretic negotiation protocols (A2A-Negotiation)
- Decentralized agent discovery
- Peer-to-peer information dissemination
- Semantic communication with shared ontologies

### SwarMCP

**Developer:** DeepMind
**Purpose:** Extends MCP with swarm intelligence algorithms

**Key Features:**
- Swarm-based coordination patterns
- Hierarchical agent organization
- Collective decision-making protocols

### Current Research Papers

| Paper | Focus | Link |
|-------|-------|------|
| A Survey of AI Agent Protocols (arXiv 2504.16736v2) | Comprehensive review of existing protocols | [arxiv.org/html/2504.16736v2](https://arxiv.org/html/2504.16736v2) |
| Fetch.ai: Architecture for Modern MAS (arXiv 2510.18699) | Gaps in formal communication protocols | [arxiv.org/html/2510.18699v1](https://arxiv.org/html/2510.18699v1) |
| Agent Communications at Edge (arXiv 2508.15819) | DAWN framework for distributed discovery | [arxiv.org/html/2508.15819v1](https://arxiv.org/html/2508.15819v1) |
| Mod-X: Decentralized Exchange (arXiv 2507.04376) | A2A and MCP comparison | [arxiv.org/html/2507.04376v2](https://arxiv.org/html/2507.04376v2) |

---

## FIPA ACL - Agent Communication Language {#fipa-acl}

### Overview

**FIPA** (Foundation for Intelligent Physical Agents) is an international organization that established interoperability standards for heterogeneous software agents. FIPA ACL is based on **Searle's Speech Act Theory** and provides a standardized framework for agent communication.

### Core Services

FIPA defines three basic services for agent platforms:

| Service | Description |
|---------|-------------|
| **AMS** (Agent Management System) | Agent lifecycle management |
| **DF** (Directory Facilitator) | Yellow pages registration and service discovery |
| **ACC** (Agent Communication Channel) | Message routing infrastructure |

### Message Structure

```lisp
(performative
  :sender agent-id-1
  :receiver agent-id-2
  :content "message content"
  :protocol fipa-request
  :language JSON
  :ontology task-ontology
  :conversation-id c123
  :reply-with response-tag
  :in-reply-to previous-tag)
```

### Performative Types

FIPA ACL defines **20 performative types**. The most common ones:

#### 1. **Inform** (告知)
- **Purpose:** Notify another agent that a proposition is true
- **Pre-conditions:**
  - Sender believes the content is true
  - Sender intends recipient to believe it
  - Recipient doesn't already know the content
- **Example:**
  ```lisp
  (inform
    :sender Foreman
    :receiver Worker1
    :content "Stone deposit located at (100, 64, 200)")
  ```

#### 2. **Request** (请求)
- **Purpose:** Request another agent to perform an action
- **Pre-conditions:**
  - Sender intends action to be performed
  - Sender believes recipient is capable
  - Recipient doesn't already intend to perform it
- **Example:**
  ```lisp
  (request
    :sender Foreman
    :receiver Worker1
    :content "(mine stone 10)")
  ```

#### 3. **Agree** (同意)
- **Purpose:** Accept a request or proposal
- **Example:**
  ```lisp
  (agree
    :sender Worker1
    :receiver Foreman
    :content "(mine stone 10)")
  ```

#### 4. **Refuse** (拒绝)
- **Purpose:** Decline to perform a requested action
- **Example:**
  ```lisp
  (refuse
    :sender Worker1
    :receiver Foreman
    :content "(mine stone 10)"
    :reason "No tools available")
  ```

#### 5. Additional Performatives

| Performative | Description |
|--------------|-------------|
| **Query-if** | Ask if a proposition is true |
| **Query-ref** | Ask for an object/reference |
| **Not-understood** | Message not understood |
| **CFP (Call For Proposal)** | Request proposals (used in CNP) |
| **Propose** | Submit a proposal |
| **Accept-proposal** | Accept a proposal |
| **Reject-proposal** | Reject a proposal |
| **Subscribe** | Subscribe to notifications |
| **Cancel** | Cancel previous request |

### Interaction Protocols

FIPA standardizes several interaction protocols:

1. **FIPA-Request**
   ```
   Initiator            Participant
      |                      |
      | ---- Request ---->   |
      |                     (process)
      | <--- Agree --------  |
      |                     (execute)
      | <--- Failure/----   |
      |      Inform         |
   ```

2. **FIPA-Query**
   ```
   Initiator            Participant
      |                      |
      | ---- Query ----->    |
      |                     (query)
      | <--- Inform ------   |
   ```

3. **FIPA-Contract-Net** (see Contract Net section)

---

## Contract Net Protocol {#contract-net}

### Overview

The **Contract Net Protocol (CNP)** is a market-based negotiation protocol for task allocation in multi-agent systems. It models a contracting marketplace where managers (initiators) and contractors (participants) negotiate task assignments.

### Protocol Stages

```
Manager                 Participant1           Participant2
   |                         |                      |
   | ---- CFP (Call For Proposal) ------------>    |
   | --------------------------------------------> |
   |                         |                      |
   | <---- Refuse ----------                      |
   | <---- Proposal -----------------------------  |
   |                         |                      |
   | -- Accept-Proposal -->                       |
   |                         |                      |
   | <---- Result --------                        |
   |                         |                      |
```

### Detailed Stages

| Stage | Description | Message Type |
|-------|-------------|--------------|
| **1. Tendering** | Manager broadcasts task announcement | CFP (Call For Proposal) |
| **2. Bidding** | Participants evaluate and submit bids | Propose / Refuse |
| **3. Awarding** | Manager evaluates bids and awards contract | Accept-proposal / Reject-proposal |
| **4. Execution** | Contractor performs the task | - |
| **5. Completion** | Contractor reports results | Inform / Failure |

### Industrial Applications

#### Flexible Manufacturing (Dark Factory)

**Agents:**
- **Task Agent:** Represents orders, initiates bidding
- **Resource Agent:** Represents equipment/workstations
- **Material Agent:** Handles supply chain matching
- **Coordinator Agent:** Monitors conflicts, maintains Blackboard

**Bid Evaluation (MCDA - Multi-Criteria Decision Analysis):**
- Delivery & Cost: 70% weight
- Load & Reputation: 30% weight

**Dynamic Rescheduling:**
- Local re-tendering when disturbances occur
- Reallocation of failed tasks

### Implementation Example

```python
class ContractNetAgent:
    def __init__(self, agent_id, capabilities):
        self.agent_id = agent_id
        self.capabilities = capabilities  # e.g., {"mine": 0.9, "build": 0.7}

    def receive_cfp(self, cfp):
        """Evaluate suitability for task"""
        suitability = 0
        for req in cfp.requirements:
            if req in self.capabilities:
                suitability += self.capabilities[req]
        return suitability

    def submit_bid(self, cfp, suitability):
        """Submit bid if suitable"""
        if suitability > threshold:
            return Proposal(
                bidder=self.agent_id,
                bid_value=suitability,
                estimated_time=calculate_time()
            )
        return Refuse(reason="Not suitable")

    def evaluate_bids(self, bids):
        """Manager evaluates received bids"""
        # MCDA: Weighted scoring
        scores = {}
        for bid in bids:
            scores[bid.bidder] = (
                bid.bid_value * 0.5 +
                (1/bid.estimated_time) * 0.3 +
                bid.reputation * 0.2
            )
        return max(scores, key=scores.get)
```

### Extensions

1. **ACNBP (Agent Capability Negotiation and Binding Protocol)**
   - Extends CNP for diverse/specialized agent populations
   - Builds on FIPA standards

2. **Incremental CNP**
   - Supports partial task decomposition
   - Progressive refinement of requirements

3. **Coalition-Based CNP**
   - Agents form coalitions for complex tasks
   - Used in defense applications (missile intercept allocation)

---

## Blackboard Pattern {#blackboard}

### Overview

The **Blackboard Pattern** is a structured communication mechanism that allows multiple agents to collaborate on complex problems through a shared data structure. It simulates experts gathering around a blackboard to solve problems together.

### Architecture Components

```
+--------------------------------------------------+
|                  Blackboard                      |
|  +----------+  +----------+  +----------+       |
|  | Level 1  |  | Level 2  |  | Level 3  |       |
|  | (data)   |  | (hypotheses)| |(solutions)|     |
|  +----------+  +----------+  +----------+       |
+--------------------------------------------------+
         ^               ^               ^
         |               |               |
    +----------+   +----------+   +----------+
    | Agent 1  |   | Agent 2  |   | Agent 3  |
    | (KS1)    |   | (KS2)    |   | (KS3)    |
    +----------+   +----------+   +----------+
         ^                               ^
         |                               |
    +---------------------------------------+
    |           Controller                  |
    |    (monitors & triggers agents)       |
    +---------------------------------------+
```

### Components

| Component | Description |
|-----------|-------------|
| **Blackboard** | Shared data structure storing problem states, hypotheses, and solutions |
| **Knowledge Sources (Agents)** | Agents that read/write to the blackboard |
| **Controller** | Monitors state and triggers agent activation |

### Key Characteristics

| Characteristic | Benefit |
|----------------|---------|
| **High Decoupling** | Agents don't need to know about each other |
| **High Flexibility** | Agents can be dynamically added/removed |
| **Opportunistic Problem Solving** | Suitable for problems without predetermined solution order |
| **Multi-level Abstraction** | Multiple abstraction levels for complex problems |

### Implementation Approaches

1. **Database-based** - Common for persistence
2. **JADE Platform** - Popular Java agent framework
3. **Partitioned Blackboards** - Divide into sections for better organization
4. **Acquaintance Set Communication** - Reduces communication bottlenecks

### Application Scenarios

- Expert systems
- Complex problem solving
- Multi-disciplinary collaboration
- Situation assessment
- Black-start decision support
- Distributed collaborative decision support (DCDSS)

### Example Implementation

```java
public class Blackboard<T> {
    private final Map<String, T> data = new ConcurrentHashMap<>();
    private final List<BlackboardListener<T>> listeners = new CopyOnWriteArrayList<>();

    public void write(String key, T value) {
        data.put(key, value);
        notifyListeners(key, value);
    }

    public T read(String key) {
        return data.get(key);
    }

    public void subscribe(String keyPattern, Consumer<T> handler) {
        listeners.add(new BlackboardListener<>(keyPattern, handler));
    }

    private void notifyListeners(String key, T value) {
        for (BlackboardListener<T> listener : listeners) {
            if (listener.matches(key)) {
                listener.onUpdate(key, value);
            }
        }
    }
}
```

---

## Message Passing Patterns {#message-passing}

### Publish-Subscribe Pattern

The **Pub-Sub pattern** decouples senders (publishers) from receivers (subscribers) through a message broker.

```
Publisher1          Publisher2
    |                   |
    +-------+           +-------+
            \           /
             \         /
              v       v
         +------------------+
         |  Message Broker  |
         |  (Topics/Events) |
         +------------------+
              /       \
             /         \
            v           v
      Subscriber1    Subscriber2
      (Topic A)      (Topic B)
```

### Key Features

| Feature | Description |
|---------|-------------|
| **Loose Coupling** | Publishers/subscribers don't know each other |
| **Topic-based Routing** | Messages categorized by topics (e.g., "task.*") |
| **Wildcard Support** | Subscribe to patterns like "agent.*.status" |
| **Asynchronous** | Non-blocking communication |
| **Event-driven** | Ideal for reactive architectures |

### Comparison with Request/Reply

| Aspect | Pub-Sub | Request/Reply |
|--------|---------|---------------|
| **Coupling** | Very loose | Tighter |
| **Scalability** | High | Limited |
| **Real-time** | Excellent | Variable |
| **Response Required** | No | Yes |
| **Use Case** | Notifications, events | Queries, commands |

### Technologies

| Technology | Type | Use Case |
|------------|------|----------|
| **MQTT** | Lightweight protocol | IoT, embedded systems |
| **RabbitMQ** | Message broker | Enterprise messaging |
| **Kafka** | Event streaming | High-throughput systems |
| **Redis Pub/Sub** | In-memory | Fast, ephemeral messages |
| **AMQP** | Protocol standard | Enterprise integration |

### Example Implementation

```python
class MessageBroker:
    def __init__(self):
        self.subscriptions = defaultdict(list)  # topic -> [subscribers]

    def subscribe(self, topic_pattern, agent):
        """Subscribe agent to topic pattern"""
        self.subscriptions[topic_pattern].append(agent)

    def publish(self, topic, message, sender):
        """Publish message to all matching subscribers"""
        for pattern, subscribers in self.subscriptions.items():
            if self._matches(pattern, topic):
                for agent in subscribers:
                    if agent.agent_id != sender.agent_id:
                        agent.receive(message)

    def _matches(self, pattern, topic):
        """Wildcard matching (e.g., 'task.*' matches 'task.update')"""
        if pattern == "*":
            return True
        if pattern.endswith("*"):
            prefix = pattern[:-1]
            return topic.startswith(prefix)
        return pattern == topic
```

---

## Priority and Urgency Handling {#priority-handling}

### Priority Queue Basics

Priority queues ensure urgent messages are processed before regular ones. Common implementations:

```java
// Priority by message type and timestamp
PriorityBlockingQueue<AgentMessage> queue = new PriorityBlockingQueue<>(11,
    Comparator.comparingInt((AgentMessage m) -> -m.getPriority().getLevel())
        .thenComparing(AgentMessage::getTimestamp)
);
```

### Priority Levels

| Level | Value | Use Case |
|-------|-------|----------|
| **CRITICAL** | 50 | System failures, safety issues |
| **URGENT** | 20 | Help requests, obstacles detected |
| **HIGH** | 10 | Task completions, human commands |
| **NORMAL** | 5 | Regular task assignments, status updates |
| **LOW** | 1 | Progress reports, telemetry |

### Message Prioritization Strategies

#### 1. Static Priority (Message Type)

```java
public enum MessageType {
    TASK_ASSIGNMENT(5),
    TASK_COMPLETE(10),
    HELP_REQUEST(20),
    SYSTEM_SHUTDOWN(50);

    private final int priority;
}
```

#### 2. Dynamic Priority (Context-Aware)

```java
public int calculateDynamicPriority(AgentMessage message) {
    int basePriority = message.getType().getPriority();

    // Boost if sender is important
    if (isImportantAgent(message.getSenderId())) {
        basePriority *= 1.5;
    }

    // Boost if task is blocked
    if (isBlockingTask(message)) {
        basePriority *= 2.0;
    }

    // Boost if deadline approaching
    long timeToDeadline = getTimeToDeadline(message);
    if (timeToDeadline < 1000) {  // < 1 second
        basePriority += 20;
    }

    return basePriority;
}
```

#### 3. Age-Based Promotion

```java
// Promote old messages to prevent starvation
public void promoteStarvingMessages() {
    long now = System.currentTimeMillis();
    for (AgentMessage msg : queue) {
        long age = now - msg.getTimestamp().toEpochMilli();
        if (age > STARVATION_THRESHOLD && msg.getPriority() < HIGH) {
            msg.setPriority(msg.getPriority() + 5);
        }
    }
}
```

### Urgent Message Handling

For time-sensitive systems (emergency control, safety):

| Technique | Description |
|-----------|-------------|
| **Preemptive Queuing** | Interrupt current message for urgent ones |
| **Dedicated Channel** | Separate high-priority queue |
| **Bypass Queuing** | Immediate delivery for critical messages |
| **Escalation** | Messages age up in priority over time |

---

## Failure and Timeout Handling {#failure-handling}

### Common Failure Scenarios

| Scenario | Detection | Recovery |
|----------|-----------|----------|
| **Agent Unreachable** | Heartbeat timeout | Retry, mark offline, redistribute tasks |
| **Message Timeout** | Response deadline | Retry with backoff, escalate |
| **Queue Overflow** | Queue size threshold | Drop oldest, notify sender |
| **Network Partition** | No ACK received | Buffer messages, reconcile when healed |
| **Agent Crash** | No heartbeat + no ACK | Restart agent, recover state |

### Timeout Strategies

#### 1. Heartbeat Monitoring

```java
public class HeartbeatMonitor {
    private final Map<String, Long> lastHeartbeat = new ConcurrentHashMap<>();
    private final long TIMEOUT_MS = 30000;  // 30 seconds

    public void recordHeartbeat(String agentId) {
        lastHeartbeat.put(agentId, System.currentTimeMillis());
    }

    public boolean isAlive(String agentId) {
        Long last = lastHeartbeat.get(agentId);
        if (last == null) return false;
        return System.currentTimeMillis() - last < TIMEOUT_MS;
    }

    public void checkAllAgents() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : lastHeartbeat.entrySet()) {
            if (now - entry.getValue() > TIMEOUT_MS) {
                handleTimeout(entry.getKey());
            }
        }
    }
}
```

#### 2. Retry with Exponential Backoff

```java
public class RetryHandler {
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 100;

    public void sendWithRetry(AgentMessage message) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                sendMessage(message);
                return;  // Success
            } catch (TimeoutException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    handleFinalFailure(message);
                    return;
                }
                long delay = BASE_DELAY_MS * (1 << attempt);  // 100, 200, 400
                Thread.sleep(delay);
            }
        }
    }
}
```

#### 3. Adaptive Timeout

```java
public class AdaptiveTimeout {
    private final Map<String, Long> averageResponseTime = new ConcurrentHashMap<>();

    public long getTimeout(String recipientId) {
        Long avg = averageResponseTime.get(recipientId);
        if (avg == null) {
            return DEFAULT_TIMEOUT;
        }
        // Timeout = 3 * average response time
        return avg * 3;
    }

    public void recordResponseTime(String recipientId, long responseTime) {
        // Exponential moving average
        Long currentAvg = averageResponseTime.getOrDefault(recipientId, responseTime);
        long newAvg = (currentAvg * 7 + responseTime) / 8;  // 0.875 weight on old
        averageResponseTime.put(recipientId, newAvg);
    }
}
```

### Exception Handling

Based on WCF communication patterns:

```java
try {
    AgentMessage response = sendMessageAndWait(message, timeout);
    // Success
} catch (TimeoutException e) {
    // Retry or escalate
    retryWithBackoff(message);
} catch (CommunicationException e) {
    // Check if channel still usable
    if (channel.getState() != CommunicationState.OPENED) {
        channel.abort();
        createNewChannel();
    }
}
```

### Connection State Validation

```java
public enum AgentState {
    CONNECTED,
    DISCONNECTED,
    RECONNECTING,
    FAILED
}

public void validateConnection(String agentId) {
    AgentState state = getConnectionState(agentId);
    switch (state) {
        case DISCONNECTED:
            attemptReconnection(agentId);
            break;
        case FAILED:
            redistributeTasks(agentId);
            break;
        case RECONNECTING:
            bufferMessages(agentId);
            break;
    }
}
```

### Keep-Alive Mechanism

Prevent firewall drops and detect failures faster:

```java
// Send ping every 40 seconds, timeout after 60 seconds
public class KeepAlive {
    private static final long PING_INTERVAL_MS = 40000;
    private static final long TIMEOUT_MS = 60000;

    @Scheduled(fixedRate = PING_INTERVAL_MS)
    public void sendPing() {
        for (String agentId : agents) {
            sendPingMessage(agentId);
        }
    }
}
```

---

## Recommendations for AgentCommunicationBus {#recommendations}

### Current State Assessment

**Strengths:**
- Thread-safe concurrent collections
- Priority-based queuing
- Message filtering
- Broadcast and point-to-point support
- Statistics tracking
- Message history

**Gaps:**
- No FIPA ACL standard compliance
- No timeout handling
- No retry mechanism
- No heartbeat monitoring
- Limited error handling
- No correlation-based request-response

### Recommended Improvements

#### 1. Add FIPA ACL Performatives

```java
public enum FIPAPerformative {
    INFORM, REQUEST, AGREE, REFUSE, QUERY_IF, QUERY_REF,
    NOT_UNDERSTOOD, CFP, PROPOSE, ACCEPT_PROPOSAL, REJECT_PROPOSAL,
    SUBSCRIBE, CANCEL, FAILURE, CONFIRM, DISCONFIRM
}

public class AgentMessage {
    private FIPAPerformative performative;  // Replace or extend Type
    private String protocol;  // e.g., "fipa-request", "fipa-contract-net"
    private String ontology;  // Shared ontology reference
    private String replyWith;  // For correlation
    private String inReplyTo;  // Response correlation
    private String conversationId;  // Thread all related messages
}
```

#### 2. Implement Contract Net Protocol

```java
public class ContractNetManager {
    private final AgentCommunicationBus bus;

    public CompletableFuture<Proposal> announceTask(TaskDescription task) {
        // Send CFP to all capable agents
        AgentMessage cfp = AgentMessage.builder()
            .performative(FIPAPerformative.CFP)
            .content(task)
            .protocol("fipa-contract-net")
            .build();

        bus.publish(cfp);

        // Wait for proposals with timeout
        return collectProposals(task.getTaskId(), DEADLINE_MS)
            .thenApply(this::selectBestProposal);
    }

    public void awardContract(String winnerId, Proposal proposal) {
        AgentMessage accept = AgentMessage.builder()
            .performative(FIPAPerformative.ACCEPT_PROPOSAL)
            .recipientId(winnerId)
            .protocol("fipa-contract-net")
            .build();

        bus.publish(accept);
    }
}
```

#### 3. Add Timeout and Retry Mechanism

```java
public class AgentCommunicationBus {
    private final ScheduledExecutorService retryExecutor;
    private final Map<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    public CompletableFuture<AgentMessage> request(AgentMessage request, long timeoutMs) {
        CompletableFuture<AgentMessage> future = new CompletableFuture<>();

        String correlationId = UUID.randomUUID().toString();
        request.setReplyWith(correlationId);

        pendingRequests.put(correlationId, new PendingRequest(future, timeoutMs));

        // Initial send
        publish(request);

        // Schedule timeout
        ScheduledFuture<?> timeoutFuture = retryExecutor.schedule(
            () -> handleTimeout(correlationId),
            timeoutMs, TimeUnit.MILLISECONDS
        );

        return future;
    }

    private void handleTimeout(String correlationId) {
        PendingRequest pending = pendingRequests.remove(correlationId);
        if (pending != null) {
            if (pending.retryCount < MAX_RETRIES) {
                // Retry with exponential backoff
                long delay = BASE_RETRY_MS * (1 << pending.retryCount);
                retryExecutor.schedule(() -> retryRequest(pending), delay, TimeUnit.MILLISECONDS);
            } else {
                pending.future.completeExceptionally(new TimeoutException());
            }
        }
    }
}
```

#### 4. Implement Heartbeat Monitoring

```java
public class HeartbeatService {
    private final AgentCommunicationBus bus;
    private final Map<String, Long> lastHeartbeat = new ConcurrentHashMap<>();
    private final Map<String, AgentState> agentStates = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 5000)  // Every 5 seconds
    public void checkHeartbeats() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : lastHeartbeat.entrySet()) {
            long age = now - entry.getValue();
            if (age > TIMEOUT_MS) {
                handleAgentTimeout(entry.getKey());
            }
        }
    }

    public void sendHeartbeat(String agentId) {
        lastHeartbeat.put(agentId, System.currentTimeMillis());
        agentStates.put(agentId, AgentState.CONNECTED);
    }

    private void handleAgentTimeout(String agentId) {
        agentStates.put(agentId, AgentState.DISCONNECTED);
        // Notify subscribers
        AgentMessage notification = AgentMessage.builder()
            .type(Type.STATUS_REPORT)
            .content("Agent timeout: " + agentId)
            .priority(Priority.HIGH)
            .build();
        bus.publish(notification);
    }
}
```

#### 5. Add Blackboard Pattern Option

```java
public class AgentBlackboard<T> {
    private final Map<String, T> state = new ConcurrentHashMap<>();
    private final List<BlackboardListener<T>> listeners = new CopyOnWriteArrayList<>();

    public void write(String key, T value, String agentId) {
        state.put(key, value);
        notifyListeners(key, value, agentId);
    }

    public Optional<T> read(String key) {
        return Optional.ofNullable(state.get(key));
    }

    public void subscribe(String keyPattern, Consumer<BlackboardEvent<T>> handler) {
        listeners.add(new BlackboardListener<>(keyPattern, handler));
    }

    private void notifyListeners(String key, T value, String writer) {
        BlackboardEvent<T> event = new BlackboardEvent<>(key, value, writer);
        for (BlackboardListener<T> listener : listeners) {
            if (listener.matches(key)) {
                listener.onEvent(event);
            }
        }
    }
}

// Usage for shared world state
AgentBlackboard<WorldState> worldBlackboard = new AgentBlackboard<>();
worldBlackboard.write("deposit.stone.location", new BlockPos(100, 64, 200), "scout-agent");
```

#### 6. Add Message Correlation for Request-Response

```java
public class CorrelationManager {
    private final Map<String, CompletableFuture<AgentMessage>> pending = new ConcurrentHashMap<>();

    public String registerPendingRequest(CompletableFuture<AgentMessage> future) {
        String correlationId = generateCorrelationId();
        pending.put(correlationId, future);
        return correlationId;
    }

    public void handleResponse(AgentMessage response) {
        String correlationId = response.getInReplyTo();
        CompletableFuture<AgentMessage> future = pending.remove(correlationId);
        if (future != null) {
            future.complete(response);
        } else {
            LOGGER.warn("No pending request for correlation: {}", correlationId);
        }
    }

    public void cleanupTimeouts() {
        // Remove expired pending requests
        pending.entrySet().removeIf(entry -> {
            if (entry.getValue().isDone()) {
                return true;
            }
            // Check age
            return isExpired(entry.getValue());
        });
    }
}
```

#### 7. Add Message Durability for Offline Agents

```java
public class PersistentMessageQueue {
    private final Map<String, Queue<AgentMessage>> offlineQueues = new ConcurrentHashMap<>();

    public void queueForOfflineAgent(String agentId, AgentMessage message) {
        offlineQueues.computeIfAbsent(agentId, k -> new ConcurrentLinkedQueue<>())
            .offer(message);
    }

    public List<AgentMessage> flushQueue(String agentId) {
        Queue<AgentMessage> queue = offlineQueues.remove(agentId);
        return queue != null ? new ArrayList<>(queue) : Collections.emptyList();
    }

    public boolean hasQueuedMessages(String agentId) {
        Queue<AgentMessage> queue = offlineQueues.get(agentId);
        return queue != null && !queue.isEmpty();
    }
}
```

### Implementation Priority

| Priority | Feature | Complexity | Benefit |
|----------|---------|------------|---------|
| **1** | Timeout & Retry | Medium | High reliability |
| **1** | Heartbeat | Low | Detect failures early |
| **2** | Message Correlation | Medium | Enable request-response |
| **2** | FIPA Performatives | Low | Standards compliance |
| **3** | Contract Net | High | Better task allocation |
| **3** | Blackboard Pattern | Medium | Shared state management |
| **4** | Offline Queueing | Medium | Handle disconnected agents |

### Example: Enhanced AgentMessage

```java
public class AgentMessage {
    // Existing fields...
    private Type type;
    private Priority priority;

    // New FIPA ACL fields
    private FIPAPerformative performative;
    private String protocol;  // "fipa-request", "fipa-contract-net"
    private String ontology;  // "minecraft-blocks", "steve-tasks"
    private String language;  // "json", "nbt"
    private String replyWith;  // Correlation for response
    private String inReplyTo;  // Response correlation
    private String conversationId;  // Thread messages

    // New metadata
    private Long timeoutMs;
    private Integer retryCount;
    private Instant expiryTime;

    // Builder enhancements
    public static class Builder {
        private FIPAPerformative performative;
        private String protocol;
        private String ontology;
        private Long timeoutMs = 5000L;  // Default 5s

        public Builder performative(FIPAPerformative performative) {
            this.performative = performative;
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder timeout(Long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public AgentMessage build() {
            AgentMessage msg = new AgentMessage(this);
            msg.expiryTime = Instant.now().plusMillis(timeoutMs);
            return msg;
        }
    }
}
```

---

## Sources

### Modern Protocols
- [多智能体通信协议标准化：MCP与A2A如何编织智能体"狼群"？](https://m.toutiao.com/a7608759491768730148/)
- [A Survey of AI Agent Protocols (arXiv 2504.16736v2)](https://arxiv.org/html/2504.16736v2)
- [Fetch.ai: An Architecture for Modern Multi-Agent Systems (arXiv 2510.18699)](https://arxiv.org/html/2510.18699v1)
- [Agent Communications toward Agentic AI at Edge (arXiv 2508.15819)](https://arxiv.org/html/2508.15819v1)
- [Mod-X: A Modular Open Decentralized eXchange Framework (arXiv 2507.04376)](https://arxiv.org/html/2507.04376v2)
- [Mechanisms For Collaboration In AI Agentic Multi-Agent Architectures](https://www.researchgate.net/publication/392200454)

### FIPA ACL
- [38、FIPA ACL：多代理系统中的标准化通信语言](https://download.csdn.net/blog/column/13001862/149219447)
- [FIPA简介](https://zhidao.baidu.com/question/723606425556568685.html)
- [基于JADE 3.7的多智能体系统开发平台详解](https://wenku.csdn.net/doc/63mdo71zju)

### Contract Net Protocol
- [Agent Capability Negotiation and Binding Protocol (ACNBP)](https://arxiv.org/html/2506.13590v1)
- [Advancing Multi-Agent Systems Through Model Context Protocol](https://arxiv.org/html/2504.21030v1)
- [某大型工厂"十五五"基于Agent的柔性制造排程](https://blog.csdn.net/weixin_54114441/article/details/158315729)
- [解密多代理系统中的角色分配与任务协调机制](https://m.blog.csdn.net/2301_76268839/article/details/151689130)
- [Multi Agent Systems: Applications & Comparison of Tools](https://research.aimultiple.com/multi-agent-systems/)
- [Distributed Task Allocation in Multi-Robot Systems](https://www.researchgate.net/publication/269369763)

### Blackboard Pattern
- [A multi-agent architecture for adaptive E-learning systems using a blackboard agent](https://m.zhangqiaokeyan.com/academic-conference-foreign_meeting-274068_thesis/0705012657928.html)
- [基于黑板结构的多智能体系统实现方法的研究](https://wap.cnki.net/lunwen-1013116335.nh.html)
- [基于黑板的多Agent分布式协同决策](https://xueshu.baidu.com/usercenter/paper/show?paperid=bb3e6633b070f2d91572476d4f43b422)
- [设计模式之状态黑板模式](https://blog.csdn.net/qq_39437730/article/details/143180048)
- [系统架构设计师-黑板架构模式](https://www.cnblogs.com/guanghe/articles/15251081.html)

### Message Passing & Priority
- [Top AI Agent Orchestration Platforms in 2026](https://redis.io/blog/ai-agent-orchestration-platforms/)
- [探究AI人工智能领域多智能体系统的通信机制](https://m.blog.csdn.net/2501_91473346/article/details/148802827)
- [How do multi-agent systems manage scalability?](https://milvus.io/ai-quick-reference/how-do-multiagent-systems-manage-scalability)
- [智能体消息队列：AI Agents for Beginners异步处理机制](https://m.blog.csdn.net/gitblog_00333/article/details/151419270)
- [观察者模式 和 发布-订阅模式](https://m.blog.csdn.net/weixin_66082551/article/details/145705303)

### Failure & Timeout Handling
- [Agent Communication Root Causes & Recovery Solutions](https://m.blog.csdn.net/InstrWander/article/details/156056104)
- [Expected Exceptions in WCF Communication](https://learn.microsoft.com/zh-cn/dotnet/framework/wcf/samples/expected-exceptions)
- [Agent Disconnect & Reconnection Issues](https://knowledge.broadcom.com/external/article/380658)
- [Multi-Agent Communication Timeout Strategies](http://www.dictall.com/indu/111/1108119F371.htm)
- [Multi-Agent Exception Handling Model for CSCD](https://www.doc88.com/p-9059971162877.html)

---

## Appendix: Quick Reference

### FIPA ACL Message Template

```lisp
(performative
  :sender agent-id
  :receiver agent-id
  :content "message content"
  :protocol protocol-name
  :language content-language
  :ontology ontology-name
  :conversation-id conv-id
  :reply-with correlation-id
  :in-reply-to previous-id
  :reply-by deadline)
```

### Priority Queue Comparison

| Implementation | Thread-Safe | Blocking | Priority Support |
|----------------|-------------|----------|------------------|
| `PriorityBlockingQueue` | Yes | Yes | Yes |
| `ConcurrentLinkedQueue` | Yes | No | No |
| `LinkedBlockingQueue` | Yes | Yes | No |
| `ArrayBlockingQueue` | Yes | Yes | No |

### Common Timeout Values

| Operation | Timeout | Reason |
|-----------|---------|--------|
| Agent heartbeat | 30s | Detect failure |
| Message response | 5s | Normal task |
| Bid submission | 10s | Contract Net |
| Proposal evaluation | 5s | Manager decision |
| Task execution | Variable | Per task |
| Keep-alive ping | 40s | Prevent firewall drops |

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Author:** Claude Research Agent
**Project:** MineWright AI - Multi-Agent Communication Enhancement
