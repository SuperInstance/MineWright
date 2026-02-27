# Multi-Agent Orchestration Research Report
## The Foreman: Hierarchical Coordination for Minecraft AI

**Date:** 2026-02-26
**Project:** MineWright - Autonomous Minecraft Agents
**Focus:** Multi-agent orchestration and hierarchical coordination patterns

---

## Executive Summary

This report investigates modern multi-agent orchestration frameworks and coordination patterns to design a system where the "Foreman" coordinates multiple crew members in a Minecraft mod. Based on analysis of top frameworks (AutoGen, CrewAI, LangGraph, CAMEL, MetaGPT) and research into orchestration patterns, we recommend a **hierarchical master-worker architecture** with **HTN-based task decomposition** and **spatial partitioning for parallel work**.

**Key Recommendation:** Implement a "Foreman" pattern combining:
- HTN (Hierarchical Task Networks) for high-level planning
- Contract Net Protocol for dynamic task allocation
- Spatial partitioning for parallel building/mining
- Blackboard architecture for shared state
- Human-in-the-loop oversight for safety

---

## Table of Contents

1. [Framework Analysis](#1-framework-analysis)
2. [Orchestration Patterns](#2-orchestration-patterns)
3. [Communication Protocols](#3-communication-protocols)
4. [Recommended Architecture](#4-recommended-architecture)
5. [Implementation Plan](#5-implementation-plan)
6. [Code Examples](#6-code-examples)
7. [Sources](#7-sources)

---

## 1. Framework Analysis

### 1.1 Top 5 Frameworks Comparison

| Framework | Type | Strengths | Weaknesses | Relevance to MineWright |
|-----------|------|-----------|------------|-------------------|
| **AutoGen** (Microsoft) | Multi-agent conversation | Human-in-the-loop, tool calling, .NET + Python | Heavyweight, complex setup | ★★★★★ - Excellent for hierarchical coordination |
| **CrewAI** | Role-playing teams | Simple JD-based agents, sequential/hierarchical flows | Limited to Python | ★★★★☆ - Good model for specialized workers |
| **LangGraph** | Stateful workflows | Persistence, human oversight, visual debugging | Python-focused, LangChain dependency | ★★★★☆ - State machine aligns with MineWright's architecture |
| **CAMEL** | Communicative agents | Role-playing, first LLM multi-agent framework | Less mature ecosystem | ★★★☆☆ - Interesting but less applicable |
| **MetaGPT** | Software company simulation | SOP-based, full lifecycle automation | Domain-specific to software | ★★☆☆☆ - Too specialized for Minecraft |

### 1.2 Detailed Framework Analysis

#### 1.2.1 AutoGen (Microsoft) - Top Recommendation

**Overview:** AutoGen is Microsoft's open-source framework for building multi-agent applications powered by LLMs. In October 2025, Microsoft released the unified **Microsoft Agent Framework** combining AutoGen with Semantic Kernel.

**Key Features:**
- Multi-agent collaboration through natural language conversations
- Flexible agent types: LLMAgent, ToolUseAgent, OrchestratorAgent
- Iterative dialogue with feedback and error correction
- Human-in-the-loop at any point
- Both Python and .NET support
- Visual development tool (AutoGen Studio)

**Relevance to MineWright:**
- **OrchestratorAgent pattern** directly maps to "Foreman MineWright"
- **Tool-calling capabilities** align with MineWright's action system
- **Human oversight** is critical for Minecraft supervision
- **Event-driven architecture** matches MineWright's existing EventBus

**Adoption:** Over 10,000 organizations using Azure AI Foundry Agent Service. BMW, KPMG, and Fujitsu have production deployments.

#### 1.2.2 CrewAI - Second Choice

**Overview:** CrewAI enables autonomous AI agents with role-playing capabilities to collaborate seamlessly.

**Key Features:**
- Agents as "employees" with defined roles, goals, and backstories
- Sequential vs. Hierarchical process types
- Standard Operating Procedure (SOP) automation
- Tool integration and long-chain workflows

**Relevance to MineWright:**
- **Hierarchical process** is exactly what Foreman MineWright needs
- **Role-based workers** (Miner MineWright, Builder MineWright, etc.)
- **SOP automation** for repetitive tasks
- **Simple API** for quick agent definition

**Best Use Cases:** Long-chain workflows, research reports, business analysis - NOT simple single-step tasks.

#### 1.2.3 LangGraph - Strong Contender

**Overview:** Low-level orchestration framework for building long-running, stateful agents. Released April 2024 as LangChain extension.

**Core Concepts:**
- **State**: Shared memory flowing through workflow
- **Node**: Processing step (LLM call, tool invocation, logic)
- **Edge**: Execution order (fixed, conditional, loops)

**Key Capabilities:**
- Persistent execution with recovery from failures
- Human-in-the-loop integration
- Comprehensive memory management
- Visual debugging (LangGraph Studio)
- Production-ready deployment

**Relevance to MineWright:**
- **State machine pattern** already implemented in MineWright
- **Persistence** crucial for long Minecraft tasks
- **Conditional branching** for decision trees
- **Loop support** for iterative refinement

**Production Users:** Klarna, Elastic, Replit

#### 1.2.4 CAMEL - Educational Value

**Overview:** First LLM-based multi-agent framework (NeurIPS 2023). Open-source project exploring agent scaling laws.

**Core Components:**
- ChatAgent: Basic building block
- RolePlaying: AI teams with assigned roles
- Workforce: Coordinates multiple teams

**Relevance to MineWright:**
- **Role-playing mechanism** useful for specialized workers
- **Task decomposition** patterns applicable
- **Structured communication** format

**Less relevant due to academic focus and less production-ready tooling.

#### 1.2.5 MetaGPT - Niche Application

**Overview:** Multi-agent framework simulating a software company (Product Manager → Architect → Engineer → QA).

**Philosophy:** "Code = SOP(Team)" - applies Standard Operating Procedures to LLM teams.

**Relevance to MineWright:**
- **Role-based collaboration** is applicable
- **Structured output generation** useful
- **Cost efficiency** ($2 for medium project)

**Less relevant due to software development specificity, but patterns transferable.

### 1.3 Framework Selection Matrix

```
                    Hierarchical  Human   Spatial  Failure  Code
                    Coordination  Oversight Partition Handling Complexity
AutoGen             ★★★★★        ★★★★★   ★★★☆☆    ★★★★☆    ★★★★☆
CrewAI              ★★★★☆        ★★★★☆   ★★★☆☆    ★★★☆☆    ★★★☆☆
LangGraph           ★★★★☆        ★★★★★   ★★☆☆☆    ★★★★☆    ★★★★☆
CAMEL               ★★★☆☆        ★★★☆☆   ★★☆☆☆    ★★★☆☆    ★★★☆☆
MetaGPT             ★★★★☆        ★★★☆☆   ★☆☆☆☆    ★★★☆☆    ★★★★☆

WINNER: AutoGen for production, LangGraph for state management
```

---

## 2. Orchestration Patterns

### 2.1 Pattern Overview

| Pattern | Description | Use Case | Complexity |
|---------|-------------|----------|------------|
| **Master-Worker** | Central coordinator delegates tasks | Hierarchical command | Low |
| **HTN** | Hierarchical decomposition of complex tasks | Goal-oriented planning | Medium |
| **Contract Net** | Competitive bidding for tasks | Dynamic allocation | Medium |
| **Blackboard** | Shared workspace for collaboration | Loose coupling | Medium |
| **Pipelined** | Sequential processing stages | Assembly-line tasks | High |
| **Collaborative Planning** | Joint plan creation | Complex multi-step goals | High |

### 2.2 Hierarchical Task Networks (HTN)

**Overview:** HTN decomposes complex tasks into hierarchical subtasks, enabling efficient handling through modular, layered planning.

**Key Concepts:**
- **Compound Tasks**: Can be decomposed into subtasks
- **Primitive Tasks**: Directly executable actions
- **Methods**: Alternative ways to decompose tasks
- **Task Networks**: Partially ordered sets of subtasks

**Applications:**
- RoboCup simulation league coordination
- Military combat simulations
- Emergency response systems
- Game AI (Horizon Zero Dawn)

**Recent Advances (2024):**
- **HiBerNAC**: Brain-inspired hierarchical neural agent collective with 23% task completion reduction
- **HTN-enhanced MARL**: Combines symbolic planning with multi-agent reinforcement learning

**Pseudo-Code:**
```python
task = "Build a castle"

# HTN Decomposition
compound_task(task) = [
    subtask("Gather resources", [
        primitive("Mine stone", quantity=1000),
        primitive("Mine wood", quantity=500)
    ]),
    subtask("Prepare site", [
        primitive("Clear area", radius=50),
        primitive("Level terrain")
    ]),
    subtask("Construct structure", [
        primitive("Build walls", height=10),
        primitive("Add towers", count=4)
    ])
]
```

### 2.3 Contract Net Protocol (CNP)

**Overview:** Market-based coordination where agents bid for tasks through competitive negotiation.

**Process:**
1. **Task Announcement**: Manager broadcasts task requirements
2. **Bidding**: Capable agents submit bids based on capabilities
3. **Award**: Manager evaluates bids and selects optimal contractor
4. **Execution & Reporting**: Selected agent executes and reports results

**Advantages:**
- **Dynamic allocation**: Tasks go to most capable/available agents
- **Scalability**: No central bottleneck
- **Flexibility**: Agents can refuse or counter-offer
- **Robustness**: Failed bids can be re-tendered

**Applications:**
- Multi-agent manufacturing systems
- E-commerce transaction coordination
- Distributed robotics
- Multi-UAV cooperative exploration

**Pseudo-Code:**
```python
# Foreman announces task
foreman.announce_task(
    task=Task(type="MINING", resource="iron_ore", quantity=64),
    deadline=1000
)

# Workers bid
worker1.submit_bid(
    task_id="task_001",
    bid=Bid(capability=0.9, availability=1.0, estimated_time=500)
)

worker2.submit_bid(
    task_id="task_001",
    bid=Bid(capability=0.7, availability=0.5, estimated_time=800)
)

# Foreman awards task
foreman.award_task("task_001", winner=worker1)  # Higher score
```

### 2.4 Blackboard Architecture

**Overview:** Shared workspace where agents communicate indirectly through read/write operations.

**Components:**
- **Blackboard**: Shared data structure storing problem states
- **Knowledge Sources (Agents)**: Read/write to blackboard
- **Controller**: Manages access and coordination

**Advantages:**
- **Decoupled communication**: No direct agent dependencies
- **Flexibility**: Agents can be dynamically added/removed
- **Auditability**: Central state is trackable
- **Scalability**: New agents simply connect to blackboard

**Applications:**
- E-learning adaptive systems
- Sensor networks
- Regression testing
- Distributed decision making

**Integration with MineWright:**
```java
public class MinecraftBlackboard {
    private Map<String, Object> sharedState = new ConcurrentHashMap<>();

    public void put(String key, Object value) {
        sharedState.put(key, value);
        notifyObservers(key, value);
    }

    public Object get(String key) {
        return sharedState.get(key);
    }

    // Agents observe changes
    public void addObserver(String key, Consumer<Object> observer) {
        // ...
    }
}
```

### 2.5 Master-Worker Pattern (Recommended for MineWright)

**Overview:** Orchestrator-agent coordinates, delegates tasks, and monitors worker agents.

**Key Features:**
- **Centralized coordination**: Single point of decision-making
- **Natural resilience**: Worker failures don't crash system
- **Task delegation**: Workers receive focused assignments
- **Progress tracking**: Master monitors overall progress

**Recent Research:**
- **Anemoi (2024)**: Semi-centralized multi-agent system with planner + autonomous workers
- **Mindcraft**: Multi-agent collaboration platform for Minecraft (up to 1000 agents!)
- **STEVE Series**: Hierarchical multi-agent systems 2.5x-7.3x more efficient than SOTA

**Pseudo-Code:**
```python
class ForemanMineWright:
    def __init__(self):
        self.workers = []
        self.task_queue = []
        self.progress = {}

    def coordinate(self, goal):
        # HTN decomposition
        subtasks = self.decompose(goal)

        # Assign to workers
        for task in subtasks:
            worker = self.select_best_worker(task)
            worker.assign(task)

        # Monitor progress
        while not self.is_complete():
            self.rebalance_if_needed()
            self.handle_failures()
```

### 2.6 Spatial Partitioning for Parallel Work

**Overview:** Divide environment into regions for parallel processing by multiple agents.

**Key Research:**
- **ros-dmapf**: Distributed solver handling 2000 robots on 100x100 maps in 7 minutes
- **Multi-UAV exploration**: Task-density based partitioning
- **Decentralized algorithms**: Equal workload without global info

**Approaches:**

1. **Geographic Partitioning**:
   - Divide space into quadrants (NW, NE, SW, SE)
   - Each agent works in assigned region
   - Dynamic rebalancing based on task density

2. **Task-Density Partitioning**:
   - Assign agents based on workload, not just position
   - Rolling horizon optimization
   - Reduces redundancy and improves efficiency

3. **Boundary Switching Strategy (BSS)**:
   - Autonomous and uniform partitioning
   - Agents can trade boundary regions
   - Input-to-state stability analysis

**MineWright's Current Implementation:**
```java
// Already implemented in CollaborativeBuildManager.java
private List<BuildSection> divideBuildIntoSections(List<BlockPlacement> plan) {
    // Divides into 4 quadrants (NW, NE, SW, SE)
    // Each quadrant sorted BOTTOM-TO-TOP
    // Agents claim sections atomically
}
```

---

## 3. Communication Protocols

### 3.1 Protocol Landscape

| Protocol | Purpose | Status | Relevance |
|----------|---------|--------|-----------|
| **A2A** (Agent-to-Agent) | Direct agent communication | Linux Foundation standard | ★★★★★ |
| **MCP** (Model Context Protocol) | Agent-to-Tool integration | Open standard | ★★★★☆ |
| **FIPA ACL** | Agent communication language | Historical standard | ★★★☆☆ |
| **KQML** | Knowledge query/manipulation | Legacy | ★★☆☆☆ |
| **Coral Protocol** | Multi-agent infrastructure | Research (2024) | ★★★☆☆ |

### 3.2 A2A Protocol (Agent-to-Agent)

**Overview:** Google's open standard for horizontal communication between agents, contributed to Linux Foundation.

**Key Components:**
- **Agent Card**: Discovery mechanism (capabilities, status)
- **Task**: Collaboration format (task definition, requirements)
- **JSON-RPC**: Communication layer

**Mechanisms:**
```json
{
  "agent_card": {
    "id": "minewright_miner_01",
    "name": "Miner MineWright",
    "capabilities": ["mine", "gather", "smelt"],
    "status": "available",
    "load": 0.3
  },
  "task": {
    "type": "mine",
    "resource": "iron_ore",
    "quantity": 64,
    "deadline": 1000
  }
}
```

**Integration with MineWright:**
```java
public interface AgentToAgentProtocol {
    // Discovery
    AgentCard getAgentCard();

    // Task coordination
    TaskResponse assignTask(Task task);
    void updateStatus(Status status);

    // Communication
    Message sendMessage(String to, Message message);
    void receiveMessage(Message message);
}
```

### 3.3 MCP (Model Context Protocol)

**Overview:** Open standard for agent-to-tool integration, enabling LLMs to interact with external systems.

**Key Features:**
- **Tool discovery**: Agents can find available tools
- **Standardized interface**: Consistent API across tools
- **Cross-platform**: Framework agnostic

**Relevance to MineWright:**
- **Action Registry**: Already implements tool-like pattern
- **Dynamic tool loading**: Plugin architecture matches MCP
- **Standardized invocation**: `Task` objects similar to MCP calls

### 3.4 Event-Driven Communication

**MineWright's Current Architecture:**
```java
// Already implemented in EventBus
public interface EventBus {
    void publish(Event event);
    void subscribe(Class<? extends Event> eventType, EventHandler handler);
    void unsubscribe(EventHandler handler);
}

// Events for coordination
public class TaskAssignedEvent extends Event {
    String workerId;
    Task task;
}

public class TaskCompletedEvent extends Event {
    String workerId;
    ActionResult result;
}

public class WorkerFailedEvent extends Event {
    String workerId;
    Exception error;
}
```

### 3.5 Recommended Communication Design

**Hybrid Approach:**
1. **Event Bus** (Existing): For state changes, progress updates
2. **Direct Messaging** (New): For task assignment, coordination
3. **Blackboard** (New): For shared world state
4. **Human Oversight Channel** (New): For user intervention

```java
public class CommunicationProtocol {
    private EventBus eventBus;          // Existing
    private MessageChannel messageChannel;  // New
    private MinecraftBlackboard blackboard;  // New
    private OversightChannel oversight;      // New

    // Broadcast to all workers
    public void broadcast(Task task) {
        messageChannel.send(Message.broadcast(task));
    }

    // Direct message to specific worker
    public void sendTo(String workerId, Command command) {
        messageChannel.send(Message.direct(workerId, command));
    }

    // Update shared state
    public void updateSharedState(String key, Object value) {
        blackboard.put(key, value);
    }

    // Request human approval
    public CompletableFuture<Approval> requestApproval(String workerId, String action) {
        return oversight.requestApproval(workerId, action);
    }
}
```

---

## 4. Recommended Architecture

### 4.1 Foreman/Worker Model

**Design Principles:**
1. **Single Foreman**: One "MineWright" acts as coordinator
2. **Specialized Workers**: Multiple worker MineWrights with specific roles
3. **Hierarchical Planning**: Foreman uses HTN for task decomposition
4. **Dynamic Allocation**: Contract Net Protocol for task assignment
5. **Spatial Partitioning**: Parallel work in different regions
6. **Shared State**: Blackboard for world knowledge
7. **Human Oversight**: Approval workflow for critical actions

### 4.2 Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         FOREMAN                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Task Planner (HTN)                                   │  │
│  │  - Decompose high-level goals                         │  │
│  │  - Create task networks                               │  │
│  │  - Validate dependencies                              │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Task Allocator (Contract Net)                        │  │
│  │  - Announce tasks                                     │  │
│  │  - Collect bids                                       │  │
│  │  - Award tasks                                        │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Progress Monitor                                     │  │
│  │  - Track worker status                                │  │
│  │  - Detect failures                                    │  │
│  │  - Rebalance workload                                 │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Human Oversight Interface                            │  │
│  │  - Request approvals                                  │  │
│  │  - Display progress                                   │  │
│  │  - Enable interventions                               │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ A2A Protocol
                            │ EventBus
                            │ Blackboard
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼────────┐  ┌───────▼────────┐  ┌───────▼────────┐
│ Miner Worker   │  │ Builder Worker │  │ Guard Worker   │
│                │  │                │  │                │
│ - Mine ores    │  │ - Place blocks │  │ - Patrol area  │
│ - Gather       │  │ - Construct    │  │ - Defend       │
│ - Smelt        │  │ - Repair       │  │ - Alert        │
└────────────────┘  └────────────────┘  └────────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
                    ┌───────▼────────┐
                    │ Blackboard     │
                    │                │
                    │ - World state  │
                    │ - Inventory    │
                    │ - Progress     │
                    │ - Coordinates  │
                    └────────────────┘
```

### 4.3 State Machine Integration

**Existing MineWright State Machine (Already Implemented):**

```
IDLE → PLANNING → EXECUTING → COMPLETED → IDLE
                  ↓
                FAILED → IDLE
                  ↓
                PAUSED → EXECUTING / IDLE
```

**Foreman MineWright Extended States:**

```
IDLE → DECOMPOSING → ALLOCATING → MONITORING → COORDINATING → COMPLETED → IDLE
                      ↓            ↓             ↓
                    FAILED       REBALANCING   INTERVENTION
```

**Worker MineWright States:**

```
IDLE → BIDDING → ASSIGNED → EXECUTING → REPORTING → IDLE
                      ↓         ↓
                    FAILED    WAITING
```

### 4.4 Task Decomposition Hierarchy

**Example: "Build a castle"**

```
Level 0: Goal
  └─ Build a castle

Level 1: Compound Tasks
  ├─ Gather resources
  ├─ Prepare building site
  ├─ Construct structure
  └─ Add finishing touches

Level 2: Subtasks (for Gather resources)
  ├─ Mine stone (quantity: 1000)
  ├─ Mine wood (quantity: 500)
  ├─ Smelt iron (quantity: 64)
  └─ Craft bricks (quantity: 200)

Level 3: Primitive Tasks (assignable to workers)
  ├─ Miner_MineWright: Mine stone at [x,y,z]
  ├─ Miner_MineWright: Mine wood at [x,y,z]
  ├─ Smelter_MineWright: Smelt iron ore
  └─ Crafter_MineWright: Craft bricks

Level 4: Spatial Partitioning (for Mine stone)
  ├─ Miner_01: Quadrant NW (250 blocks)
  ├─ Miner_02: Quadrant NE (250 blocks)
  ├─ Miner_03: Quadrant SW (250 blocks)
  └─ Miner_04: Quadrant SE (250 blocks)
```

### 4.5 Failure Handling & Rebalancing

**Failure Detection:**
```java
public class WorkerMonitor {
    private Map<String, WorkerHealth> healthStatus = new ConcurrentHashMap<>();

    public void checkHeartbeat(String workerId) {
        WorkerHealth health = healthStatus.get(workerId);

        if (health.getLastHeartbeat() < deadline) {
            handleFailure(workerId, "timeout");
        }

        if (health.getFailureRate() > threshold) {
            handleFailure(workerId, "high_failure_rate");
        }
    }

    private void handleFailure(String workerId, String reason) {
        // 1. Preserve task state
        Task currentTask = getWorkerTask(workerId);

        // 2. Find replacement worker
        String replacement = findBestReplacement(currentTask);

        // 3. Migrate task
        if (replacement != null) {
            migrateTask(workerId, replacement, currentTask);
        } else {
            queueForRetry(currentTask);
        }

        // 4. Notify foreman
        eventBus.publish(new WorkerFailedEvent(workerId, reason));
    }
}
```

**Rebalancing Strategies:**
1. **Task Migration**: Move incomplete tasks to healthy workers
2. **Dynamic Scaling**: Spawn new workers if needed
3. **Priority Reprioritization**: Reschedule based on criticality
4. **Spatial Reassignment**: Redraw partition boundaries

### 4.6 Human-in-the-Loop Integration

**Approval Workflow:**
```java
public class OversightChannel {
    private Queue<ApprovalRequest> pendingApprovals = new ConcurrentLinkedQueue<>();

    public CompletableFuture<Approval> requestApproval(String workerId, String action) {
        ApprovalRequest request = new ApprovalRequest(workerId, action);
        pendingApprovals.add(request);

        // Notify player
        notifyPlayer(workerId + " requests approval to: " + action);

        // Wait for response (non-blocking)
        return request.getFuture();
    }

    public void approve(String requestId, boolean approved) {
        ApprovalRequest request = findRequest(requestId);

        if (approved) {
            request.complete(Approval.APPROVED);
        } else {
            request.complete(Approval.DENIED);
        }

        pendingApprovals.remove(request);
    }
}
```

**Critical Actions Requiring Approval:**
- Structure demolition
- Large-scale terrain modification
- PvP engagement
- Resource consumption above threshold
- Commands affecting other players

---

## 5. Implementation Plan

### 5.1 Phase 1: Foundation (Week 1-2)

**Tasks:**
1. Create `ForemanMineWright` entity extending `MineWrightEntity`
2. Implement `AgentCard` and A2A protocol basics
3. Extend `AgentStateMachine` for foreman states
4. Create `WorkerMineWright` entity with specialized roles

**Deliverables:**
- `ForemanMineWright.java` - Coordinator entity
- `WorkerMineWright.java` - Specialized worker entity
- `AgentToAgentProtocol.java` - Communication interface
- `AgentCard.java` - Agent discovery mechanism

### 5.2 Phase 2: HTN Planner (Week 3-4)

**Tasks:**
1. Design HTN domain for Minecraft tasks
2. Implement task decomposition logic
3. Create task network validator
4. Integrate with existing `TaskPlanner`

**Deliverables:**
- `HTNPlanner.java` - Hierarchical decomposition engine
- `TaskNetwork.java` - Task dependency graph
- `HTNDomain.java` - Minecraft task definitions
- `TaskDecomposer.java` - Recursive decomposition

### 5.3 Phase 3: Contract Net Allocator (Week 5-6)

**Tasks:**
1. Implement task announcement system
2. Create bid evaluation logic
3. Build task award mechanism
4. Add timeout handling

**Deliverables:**
- `ContractNetAllocator.java` - CNP implementation
- `TaskAnnouncement.java` - Task broadcast
- `Bid.java` - Worker bid structure
- `BidEvaluator.java` - Scoring algorithm

### 5.4 Phase 4: Blackboard & Shared State (Week 7)

**Tasks:**
1. Implement `MinecraftBlackboard`
2. Define shared state schema
3. Add change notifications
4. Integrate with existing `WorldKnowledge`

**Deliverables:**
- `MinecraftBlackboard.java` - Shared workspace
- `SharedStateSchema.java` - State definitions
- `StateChangeListener.java` - Observer pattern

### 5.5 Phase 5: Failure Handling & Rebalancing (Week 8)

**Tasks:**
1. Implement health monitoring
2. Create failure detection
3. Build task migration
4. Add rebalancing logic

**Deliverables:**
- `WorkerMonitor.java` - Health tracking
- `FailureHandler.java` - Recovery logic
- `TaskMigrator.java` - Task transfer
- `RebalancingStrategy.java` - Load balancing

### 5.6 Phase 6: Human Oversight (Week 9)

**Tasks:**
1. Design approval GUI
2. Implement oversight channel
3. Add intervention controls
4. Create progress dashboard

**Deliverables:**
- `OversightChannel.java` - Human-in-the-loop
- `OversightGUI.java` - Player interface
- `ApprovalRequest.java` - Approval workflow
- `ProgressDashboard.java` - Status display

### 5.7 Phase 7: Testing & Optimization (Week 10)

**Tasks:**
1. Unit tests for all components
2. Integration tests with multiple workers
3. Performance profiling
4. Stress testing (10+ workers)

**Deliverables:**
- Comprehensive test suite
- Performance benchmarks
- Documentation updates

### 5.8 Integration with Existing Code

**Leveraging Existing Components:**

| Existing Component | New Usage |
|-------------------|-----------|
| `ActionRegistry` | Register specialized worker actions |
| `EventBus` | Multi-agent coordination events |
| `AgentStateMachine` | Extended state management |
| `CollaborativeBuildManager` | Spatial partitioning foundation |
| `TaskPlanner` | Integrated with HTN planner |
| `MineWrightMemory` | Extended for team coordination |

---

## 6. Code Examples

### 6.1 Foreman MineWright Implementation

```java
package com.minewright.ai.entity;

import com.minewright.ai.action.Task;
import com.minewright.ai.execution.AgentState;
import com.minewright.ai.orchestration.*;
import java.util.*;

/**
 * Foreman MineWright - Coordinates multiple worker MineWrights
 *
 * Responsibilities:
 * - Decompose high-level goals using HTN
 * - Allocate tasks via Contract Net Protocol
 * - Monitor worker progress
 * - Handle failures and rebalancing
 * - Request human oversight for critical actions
 */
public class ForemanMineWright extends MineWrightEntity {

    private final HTNPlanner htnPlanner;
    private final ContractNetAllocator taskAllocator;
    private final WorkerMonitor workerMonitor;
    private final MinecraftBlackboard blackboard;
    private final OversightChannel oversight;

    private final Map<String, WorkerMineWright> workers = new ConcurrentHashMap<>();
    private final Queue<Task> pendingTasks = new ConcurrentLinkedQueue<>();
    private TaskNetwork currentPlan;

    public ForemanMineWright(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);

        this.htnPlanner = new HTNPlanner();
        this.taskAllocator = new ContractNetAllocator(this);
        this.workerMonitor = new WorkerMonitor(this);
        this.blackboard = new MinecraftBlackboard();
        this.oversight = new OversightChannel(this);

        getActionExecutor().getStateMachine().transitionTo(AgentState.IDLE);
        MineWrightMod.LOGGER.info("Foreman MineWright initialized");
    }

    /**
     * Process high-level command from player
     */
    @Override
    public void processNaturalLanguageCommand(String command) {
        MineWrightMod.LOGGER.info("Foreman MineWright processing: {}", command);

        // Transition to DECOMPOSING state
        getActionExecutor().getStateMachine().transitionTo(AgentState.PLANNING);

        // HTN decomposition
        CompletableFuture<TaskNetwork> decomposition = htnPlanner.decomposeAsync(command, this);

        decomposition.thenAccept(plan -> {
            this.currentPlan = plan;
            getActionExecutor().getStateMachine().transitionTo(AgentState.EXECUTING);

            // Begin allocation
            allocateTasks(plan.getTasks());
        }).exceptionally(ex -> {
            MineWrightMod.LOGGER.error("HTN decomposition failed", ex);
            getActionExecutor().getStateMachine().transitionTo(AgentState.FAILED);
            return null;
        });
    }

    /**
     * Allocate tasks to workers using Contract Net Protocol
     */
    private void allocateTasks(List<Task> tasks) {
        for (Task task : tasks) {
            if (task.needsApproval()) {
                // Request human approval for critical tasks
                requestApprovalAndAllocate(task);
            } else {
                // Direct allocation
                taskAllocator.announceTask(task);
            }
        }
    }

    /**
     * Request human approval before allocating task
     */
    private void requestApprovalAndAllocate(Task task) {
        oversight.requestApproval(getMineWrightName(), task.toString())
            .thenCompose(approval -> {
                if (approval.isApproved()) {
                    return taskAllocator.announceTask(task);
                } else {
                    MineWrightMod.LOGGER.info("Task denied by human: {}", task);
                    return CompletableFuture.completedFuture(null);
                }
            });
    }

    /**
     * Register a worker MineWright
     */
    public void registerWorker(WorkerMineWright worker) {
        String workerId = worker.getMineWrightName();
        workers.put(workerId, worker);
        workerMonitor.monitor(worker);

        MineWrightMod.LOGGER.info("Registered worker: {}", workerId);
    }

    /**
     * Handle task completion from worker
     */
    public void onTaskCompleted(String workerId, Task task, ActionResult result) {
        MineWrightMod.LOGGER.info("Task completed by {}: {}", workerId, task);

        // Update shared state
        blackboard.put("task." + task.getId(), result);

        // Check if plan is complete
        if (currentPlan != null && currentPlan.isComplete()) {
            getActionExecutor().getStateMachine().transitionTo(AgentState.COMPLETED);
            sendChatMessage("All tasks completed successfully!");
        }

        // Rebalance if workers are idle
        workerMonitor.rebalanceIfNeeded();
    }

    /**
     * Handle worker failure
     */
    public void onWorkerFailed(String workerId, Exception error) {
        MineWrightMod.LOGGER.error("Worker failed: {}", workerId, error);

        WorkerMineWright failedWorker = workers.get(workerId);
        Task currentTask = failedWorker.getCurrentTask();

        // Migrate task to another worker
        Task migratedTask = workerMonitor.migrateTask(workerId, currentTask);

        if (migratedTask != null) {
            taskAllocator.announceTask(migratedTask);
        } else {
            // Queue for retry
            pendingTasks.add(currentTask);
        }
    }

    /**
     * Get workers matching capability
     */
    public List<WorkerMineWright> getWorkersByCapability(String capability) {
        return workers.values().stream()
            .filter(w -> w.hasCapability(capability))
            .toList();
    }

    /**
     * Get shared blackboard
     */
    public MinecraftBlackboard getBlackboard() {
        return blackboard;
    }

    /**
     * Get oversight channel
     */
    public OversightChannel getOversight() {
        return oversight;
    }

    @Override
    public void tick() {
        super.tick();

        // Monitor workers
        workerMonitor.tick();

        // Process pending tasks
        if (!pendingTasks.isEmpty()) {
            Task task = pendingTasks.poll();
            taskAllocator.announceTask(task);
        }
    }
}
```

### 6.2 Worker MineWright Implementation

```java
package com.minewright.ai.entity;

import com.minewright.ai.action.Task;
import com.minewright.ai.action.ActionResult;
import com.minewright.ai.execution.AgentState;
import com.minewright.ai.orchestration.*;
import java.util.*;

/**
 * Worker MineWright - Specialized worker that executes tasks
 *
 * Specializations:
 * - Miner: Mines ores, gathers resources
 * - Builder: Places blocks, constructs structures
 * - Guard: Patrols, defends area
 * - Crafter: Crafts items, smelts ores
 * - Farmer: Plants, harvests crops
 */
public class WorkerMineWright extends MineWrightEntity {

    private final WorkerRole role;
    private final AgentCard agentCard;
    private final BidEvaluator bidEvaluator;

    private Task currentTask;
    private String foremanId;

    public WorkerMineWright(EntityType<? extends PathfinderMob> type, Level level, WorkerRole role) {
        super(type, level);
        this.role = role;
        this.agentCard = createAgentCard(role);
        this.bidEvaluator = new BidEvaluator(this);

        // Set name based on role
        String roleName = role.name().toLowerCase();
        setMineWrightName(roleName + "_" + UUID.randomUUID().toString().substring(0, 8));

        MineWrightMod.LOGGER.info("Worker MineWright created: {} with role {}", getMineWrightName(), role);
    }

    private AgentCard createAgentCard(WorkerRole role) {
        return AgentCard.builder()
            .id(getMineWrightName())
            .name(getMineWrightName())
            .role(role.name())
            .capabilities(role.getCapabilities())
            .status(AgentStatus.IDLE)
            .load(0.0)
            .position(blockPosition())
            .build();
    }

    /**
     * Receive task announcement from foreman
     */
    public Bid onTaskAnnounced(TaskAnnouncement announcement) {
        Task task = announcement.getTask();

        // Check if capable
        if (!role.canHandle(task)) {
            return null; // Don't bid
        }

        // Calculate bid score
        double capabilityScore = bidEvaluator.evaluateCapability(task);
        double availabilityScore = bidEvaluator.evaluateAvailability(task);
        double distanceScore = bidEvaluator.evaluateDistance(task.getLocation());

        double totalScore = (capabilityScore * 0.5) +
                           (availabilityScore * 0.3) +
                           (distanceScore * 0.2);

        long estimatedTime = bidEvaluator.estimateTime(task);

        return Bid.builder()
            .workerId(getMineWrightName())
            .taskId(task.getId())
            .score(totalScore)
            .estimatedTime(estimatedTime)
            .capability(capabilityScore)
            .availability(availabilityScore)
            .build();
    }

    /**
     * Receive task award
     */
    public void onTaskAwarded(Task task) {
        MineWrightMod.LOGGER.info("{} awarded task: {}", getMineWrightName(), task);

        this.currentTask = task;
        agentCard.setStatus(AgentStatus.BUSY);
        agentCard.setLoad(1.0);

        getActionExecutor().getStateMachine().transitionTo(AgentState.EXECUTING);

        // Execute task
        executeTask(task);
    }

    /**
     * Execute assigned task
     */
    private void executeTask(Task task) {
        // Delegate to existing action executor
        getActionExecutor().executeTask(task);

        // Monitor completion
        CompletableFuture.runAsync(() -> {
            while (!task.isComplete()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Report completion
            reportCompletion(task);
        });
    }

    /**
     * Report task completion to foreman
     */
    private void reportCompletion(Task task) {
        ActionResult result = getActionExecutor().getLastResult();

        agentCard.setStatus(AgentStatus.IDLE);
        agentCard.setLoad(0.0);

        getActionExecutor().getStateMachine().transitionTo(AgentState.COMPLETED);

        // Notify foreman
        if (foremanId != null) {
            ForemanMineWright foreman = (ForemanMineWright) MineWrightManager.getMineWright(foremanId);
            if (foreman != null) {
                foreman.onTaskCompleted(getMineWrightName(), task, result);
            }
        }

        MineWrightMod.LOGGER.info("{} completed task: {}", getMineWrightName(), task);
    }

    /**
     * Check if worker has capability
     */
    public boolean hasCapability(String capability) {
        return role.getCapabilities().contains(capability);
    }

    /**
     * Get current task
     */
    public Task getCurrentTask() {
        return currentTask;
    }

    /**
     * Set foreman
     */
    public void setForeman(String foremanId) {
        this.foremanId = foremanId;
    }

    /**
     * Get agent card
     */
    public AgentCard getAgentCard() {
        return agentCard;
    }

    /**
     * Update agent status
     */
    public void updateStatus(AgentStatus status) {
        agentCard.setStatus(status);
    }
}

/**
 * Worker roles with capabilities
 */
public enum WorkerRole {
    MINER("mine", "gather", "smelt"),
    BUILDER("place", "construct", "repair"),
    GUARD("patrol", "defend", "alert"),
    CRAFTER("craft", "smelt", "enchant"),
    FARMER("plant", "harvest", "breed"),
    GENERAL("pathfind", "follow", "idle");

    private final Set<String> capabilities;

    WorkerRole(String... capabilities) {
        this.capabilities = Set.of(capabilities);
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    public boolean canHandle(Task task) {
        return capabilities.contains(task.getAction());
    }
}
```

### 6.3 HTN Planner Implementation

```java
package com.minewright.ai.orchestration;

import com.minewright.ai.action.Task;
import com.minewright.ai.entity.MineWrightEntity;
import com.minewright.ai.memory.WorldKnowledge;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Hierarchical Task Network Planner
 *
 * Decomposes high-level goals into executable task networks
 */
public class HTNPlanner {

    private final HTNDomain domain;

    public HTNPlanner() {
        this.domain = new MinecraftHTNDomain();
    }

    /**
     * Decompose high-level goal into task network
     */
    public CompletableFuture<TaskNetwork> decomposeAsync(String goal, MineWrightEntity context) {
        return CompletableFuture.supplyAsync(() -> decompose(goal, context));
    }

    /**
     * Synchronous decomposition
     */
    public TaskNetwork decompose(String goal, MineWrightEntity context) {
        WorldKnowledge world = new WorldKnowledge(context);

        // Find method for root task
        CompoundTask rootTask = new CompoundTask(goal);
        Method method = domain.findMethod(rootTask, world);

        if (method == null) {
            throw new HTNException("No method found for goal: " + goal);
        }

        // Decompose recursively
        TaskNetwork network = new TaskNetwork();
        List<Task> tasks = method.decompose(rootTask, world);

        for (Task task : tasks) {
            if (task instanceof CompoundTask) {
                // Recursively decompose
                network.addAll(decompose(task.getName(), context));
            } else {
                // Primitive task - add to network
                network.add(task);
            }
        }

        return network;
    }

    /**
     * Validate task network
     */
    public boolean validate(TaskNetwork network) {
        // Check for circular dependencies
        if (hasCycles(network)) {
            return false;
        }

        // Check all constraints are satisfiable
        for (Constraint constraint : network.getConstraints()) {
            if (!constraint.isSatisfied(network)) {
                return false;
            }
        }

        return true;
    }

    private boolean hasCycles(TaskNetwork network) {
        // Topological sort to detect cycles
        // Implementation omitted for brevity
        return false;
    }
}

/**
 * HTN Domain for Minecraft tasks
 */
class MinecraftHTNDomain implements HTNDomain {

    private final Map<String, List<Method>> methods = new HashMap<>();

    public MinecraftHTNDomain() {
        initializeMethods();
    }

    private void initializeMethods() {
        // Build structure methods
        methods.put("build_structure", List.of(
            new BuildStructureMethod()
        ));

        // Gather resource methods
        methods.put("gather_resource", List.of(
            new MineResourceMethod(),
            new CraftResourceMethod()
        ));

        // Defense methods
        methods.put("defend_area", List.of(
            new PatrolMethod(),
            new BuildWallsMethod(),
            new EquipGuardMethod()
        ));
    }

    @Override
    public Method findMethod(CompoundTask task, WorldKnowledge world) {
        List<Method> applicableMethods = methods.get(task.getName());

        if (applicableMethods == null) {
            return null;
        }

        // Find first method whose preconditions are satisfied
        for (Method method : applicableMethods) {
            if (method.isApplicable(task, world)) {
                return method;
            }
        }

        return null;
    }
}

/**
 * Method for building structures
 */
class BuildStructureMethod implements Method {

    @Override
    public boolean isApplicable(CompoundTask task, WorldKnowledge world) {
        // Check if we have basic resources
        return world.hasResource("wood", 100) || world.hasResource("stone", 100);
    }

    @Override
    public List<Task> decompose(CompoundTask task, WorldKnowledge world) {
        List<Task> subtasks = new ArrayList<>();

        // 1. Gather resources
        subtasks.add(Task.builder()
            .action("gather")
            .parameter("resource", "wood")
            .parameter("quantity", 500)
            .build());

        // 2. Prepare site
        subtasks.add(Task.builder()
            .action("clear_area")
            .parameter("radius", 20)
            .build());

        // 3. Construct structure (decomposed further)
        subtasks.add(Task.builder()
            .action("construct")
            .parameter("structure", task.getParameter("structure"))
            .parameter("location", task.getParameter("location"))
            .build());

        return subtasks;
    }
}

/**
 * Task Network - represents decomposed plan
 */
class TaskNetwork {
    private final List<Task> tasks = new ArrayList<>();
    private final List<Constraint> constraints = new ArrayList<>();

    public void add(Task task) {
        tasks.add(task);
    }

    public void addAll(List<Task> tasks) {
        this.tasks.addAll(tasks);
    }

    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public void addConstraint(Constraint constraint) {
        constraints.add(constraint);
    }

    public List<Constraint> getConstraints() {
        return Collections.unmodifiableList(constraints);
    }

    public boolean isComplete() {
        return tasks.stream().allMatch(Task::isComplete);
    }

    public int getProgressPercentage() {
        if (tasks.isEmpty()) return 100;

        int completed = (int) tasks.stream().filter(Task::isComplete).count();
        return (completed * 100) / tasks.size();
    }
}

/**
 * HTN Domain interface
 */
interface HTNDomain {
    Method findMethod(CompoundTask task, WorldKnowledge world);
}

/**
 * Method interface
 */
interface Method {
    boolean isApplicable(CompoundTask task, WorldKnowledge world);
    List<Task> decompose(CompoundTask task, WorldKnowledge world);
}

/**
 * Constraint for task ordering
 */
interface Constraint {
    boolean isSatisfied(TaskNetwork network);
}

/**
 * Compound task - can be decomposed
 */
class CompoundTask extends Task {
    public CompoundTask(String name) {
        super(name);
    }
}

/**
 * HTN Exception
 */
class HTNException extends RuntimeException {
    public HTNException(String message) {
        super(message);
    }
}
```

### 6.4 Contract Net Allocator Implementation

```java
package com.minewright.ai.orchestration;

import com.minewright.ai.MineWrightMod;
import com.minewright.ai.action.Task;
import com.minewright.ai.entity.ForemanMineWright;
import com.minewright.ai.entity.WorkerMineWright;
import java.util.*;
import java.util.concurrent.*;

/**
 * Contract Net Protocol Allocator
 *
 * Implements task announcement, bidding, and award
 */
public class ContractNetAllocator {

    private final ForemanMineWright foreman;
    private final Map<String, TaskAnnouncement> announcements = new ConcurrentHashMap<>();
    private final Map<String, List<Bid>> pendingBids = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Configuration
    private static final long BID_TIMEOUT_MS = 5000;  // 5 seconds to bid
    private static final int MIN_BIDS_REQUIRED = 1;

    public ContractNetAllocator(ForemanMineWright foreman) {
        this.foreman = foreman;
    }

    /**
     * Announce task to all workers
     */
    public CompletableFuture<Void> announceTask(Task task) {
        String taskId = task.getId();
        TaskAnnouncement announcement = new TaskAnnouncement(taskId, task);

        announcements.put(taskId, announcement);
        pendingBids.put(taskId, new CopyOnWriteArrayList<>());

        MineWrightMod.LOGGER.info("Announcing task {}: {}", taskId, task);

        // Broadcast to all workers
        List<WorkerMineWright> workers = getEligibleWorkers(task);

        for (WorkerMineWright worker : workers) {
            try {
                Bid bid = worker.onTaskAnnounced(announcement);

                if (bid != null) {
                    submitBid(taskId, bid);
                }
            } catch (Exception e) {
                MineWrightMod.LOGGER.error("Error getting bid from worker", e);
            }
        }

        // Wait for bids then award
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(BID_TIMEOUT_MS);
                awardTask(taskId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, scheduler);
    }

    /**
     * Submit bid for task
     */
    public void submitBid(String taskId, Bid bid) {
        List<Bid> bids = pendingBids.get(taskId);

        if (bids != null && !isDeadlinePassed(taskId)) {
            bids.add(bid);
            MineWrightMod.LOGGER.debug("Received bid from {} for task {}: score={}",
                bid.getWorkerId(), taskId, bid.getScore());
        } else {
            MineWrightMod.LOGGER.warn("Rejected late bid from {} for task {}",
                bid.getWorkerId(), taskId);
        }
    }

    /**
     * Award task to best bidder
     */
    private void awardTask(String taskId) {
        List<Bid> bids = pendingBids.get(taskId);

        if (bids == null || bids.size() < MIN_BIDS_REQUIRED) {
            MineWrightMod.LOGGER.warn("Insufficient bids for task {}: {}/{}",
                taskId, bids != null ? bids.size() : 0, MIN_BIDS_REQUIRED);

            // Re-announce or queue for retry
            handleNoBids(taskId);
            return;
        }

        // Sort by score (highest first)
        bids.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // Select best bid
        Bid bestBid = bids.get(0);
        String workerId = bestBid.getWorkerId();

        MineWrightMod.LOGGER.info("Awarding task {} to {} (score: {})",
            taskId, workerId, bestBid.getScore());

        // Award task to worker
        WorkerMineWright worker = (WorkerMineWright) MineWrightManager.getMineWright(workerId);
        if (worker != null) {
            Task task = announcements.get(taskId).getTask();
            worker.onTaskAwarded(task);
        } else {
            MineWrightMod.LOGGER.error("Worker not found: {}", workerId);
        }

        // Cleanup
        announcements.remove(taskId);
        pendingBids.remove(taskId);
    }

    /**
     * Get eligible workers for task
     */
    private List<WorkerMineWright> getEligibleWorkers(Task task) {
        String action = task.getAction();

        return foreman.getWorkersByCapability(action);
    }

    /**
     * Check if bid deadline passed
     */
    private boolean isDeadlinePassed(String taskId) {
        TaskAnnouncement announcement = announcements.get(taskId);
        return announcement != null && announcement.getDeadline() < System.currentTimeMillis();
    }

    /**
     * Handle case with no valid bids
     */
    private void handleNoBids(String taskId) {
        TaskAnnouncement announcement = announcements.remove(taskId);
        pendingBids.remove(taskId);

        if (announcement != null) {
            // Re-announce with higher priority or queue
            MineWrightMod.LOGGER.info("Re-announcing task {} due to no bids", taskId);
            announceTask(announcement.getTask());
        }
    }

    /**
     * Shutdown allocator
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Task announcement
 */
class TaskAnnouncement {
    private final String taskId;
    private final Task task;
    private final long timestamp;
    private final long deadline;

    public TaskAnnouncement(String taskId, Task task) {
        this.taskId = taskId;
        this.task = task;
        this.timestamp = System.currentTimeMillis();
        this.deadline = timestamp + ContractNetAllocator.BID_TIMEOUT_MS;
    }

    public String getTaskId() { return taskId; }
    public Task getTask() { return task; }
    public long getTimestamp() { return timestamp; }
    public long getDeadline() { return deadline; }
}

/**
 * Bid from worker
 */
class Bid {
    private final String workerId;
    private final String taskId;
    private final double score;
    private final long estimatedTime;
    private final double capability;
    private final double availability;

    private Bid(Builder builder) {
        this.workerId = builder.workerId;
        this.taskId = builder.taskId;
        this.score = builder.score;
        this.estimatedTime = builder.estimatedTime;
        this.capability = builder.capability;
        this.availability = builder.availability;
    }

    public String getWorkerId() { return workerId; }
    public String getTaskId() { return taskId; }
    public double getScore() { return score; }
    public long getEstimatedTime() { return estimatedTime; }
    public double getCapability() { return capability; }
    public double getAvailability() { return availability; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String workerId;
        private String taskId;
        private double score;
        private long estimatedTime;
        private double capability;
        private double availability;

        public Builder workerId(String workerId) { this.workerId = workerId; return this; }
        public Builder taskId(String taskId) { this.taskId = taskId; return this; }
        public Builder score(double score) { this.score = score; return this; }
        public Builder estimatedTime(long estimatedTime) { this.estimatedTime = estimatedTime; return this; }
        public Builder capability(double capability) { this.capability = capability; return this; }
        public Builder availability(double availability) { this.availability = availability; return this; }

        public Bid build() {
            return new Bid(this);
        }
    }
}
```

### 6.5 Worker Monitor Implementation

```java
package com.minewright.ai.orchestration;

import com.minewright.ai.MineWrightMod;
import com.minewright.ai.action.Task;
import com.minewright.ai.entity.WorkerMineWright;
import java.util.*;
import java.util.concurrent.*;

/**
 * Monitors worker health and handles failures
 */
public class WorkerMonitor {

    private final ForemanMineWright foreman;
    private final Map<String, WorkerHealth> healthStatus = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Configuration
    private static final long HEARTBEAT_INTERVAL_MS = 1000;  // Check every second
    private static final long HEARTBEAT_TIMEOUT_MS = 5000;   // 5 seconds timeout
    private static final double MAX_FAILURE_RATE = 0.3;      // 30% failure threshold

    public WorkerMonitor(ForemanMineWright foreman) {
        this.foreman = foreman;

        // Start heartbeat checker
        scheduler.scheduleAtFixedRate(this::checkHeartbeats,
            HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Start monitoring a worker
     */
    public void monitor(WorkerMineWright worker) {
        String workerId = worker.getMineWrightName();

        healthStatus.put(workerId, new WorkerHealth(workerId));

        MineWrightMod.LOGGER.info("Started monitoring worker: {}", workerId);
    }

    /**
     * Update worker heartbeat
     */
    public void updateHeartbeat(String workerId) {
        WorkerHealth health = healthStatus.get(workerId);

        if (health != null) {
            health.updateHeartbeat();
        }
    }

    /**
     * Record task success
     */
    public void recordSuccess(String workerId) {
        WorkerHealth health = healthStatus.get(workerId);

        if (health != null) {
            health.recordSuccess();
        }
    }

    /**
     * Record task failure
     */
    public void recordFailure(String workerId, Exception error) {
        WorkerHealth health = healthStatus.get(workerId);

        if (health != null) {
            health.recordFailure();
        }

        // Check if failure rate is too high
        if (health != null && health.getFailureRate() > MAX_FAILURE_RATE) {
            handleUnhealthyWorker(workerId, "high_failure_rate");
        }
    }

    /**
     * Check all worker heartbeats
     */
    private void checkHeartbeats() {
        long now = System.currentTimeMillis();

        for (Map.Entry<String, WorkerHealth> entry : healthStatus.entrySet()) {
            String workerId = entry.getKey();
            WorkerHealth health = entry.getValue();

            if (now - health.getLastHeartbeat() > HEARTBEAT_TIMEOUT_MS) {
                handleUnhealthyWorker(workerId, "heartbeat_timeout");
            }
        }
    }

    /**
     * Handle unhealthy worker
     */
    private void handleUnhealthyWorker(String workerId, String reason) {
        MineWrightMod.LOGGER.warn("Worker {} is unhealthy: {}", workerId, reason);

        WorkerMineWright worker = (WorkerMineWright) MineWrightManager.getMineWright(workerId);
        if (worker == null) {
            return;
        }

        // Get current task
        Task currentTask = worker.getCurrentTask();

        // Migrate task to another worker
        Task migratedTask = migrateTask(workerId, currentTask);

        if (migratedTask != null) {
            MineWrightMod.LOGGER.info("Migrated task from {} to new worker", workerId);

            // Award task to new worker
            ContractNetAllocator allocator = new ContractNetAllocator(foreman);
            allocator.announceTask(migratedTask);
        } else {
            MineWrightMod.LOGGER.warn("Could not migrate task from {}", workerId);
        }

        // Remove worker from monitoring
        healthStatus.remove(workerId);
    }

    /**
     * Migrate task to another worker
     */
    public Task migrateTask(String fromWorkerId, Task task) {
        if (task == null) {
            return null;
        }

        // Find eligible workers
        List<WorkerMineWright> eligibleWorkers = foreman.getWorkersByCapability(task.getAction());

        // Filter out the failed worker
        eligibleWorkers.removeIf(w -> w.getMineWrightName().equals(fromWorkerId));

        if (eligibleWorkers.isEmpty()) {
            return null;
        }

        // Select best worker (lowest load)
        WorkerMineWright bestWorker = eligibleWorkers.stream()
            .min(Comparator.comparingDouble(w -> w.getAgentCard().getLoad()))
            .orElse(null);

        if (bestWorker != null) {
            MineWrightMod.LOGGER.info("Migrating task to {}: {}", bestWorker.getMineWrightName(), task);
            return task;
        }

        return null;
    }

    /**
     * Rebalance workload if needed
     */
    public void rebalanceIfNeeded() {
        // Find overloaded workers
        List<WorkerHealth> overloaded = healthStatus.values().stream()
            .filter(h -> h.getWorker() != null)
            .filter(h -> h.getWorker().getAgentCard().getLoad() > 0.8)
            .toList();

        // Find underloaded workers
        List<WorkerHealth> underloaded = healthStatus.values().stream()
            .filter(h -> h.getWorker() != null)
            .filter(h -> h.getWorker().getAgentCard().getLoad() < 0.3)
            .toList();

        // Migrate tasks from overloaded to underloaded
        for (WorkerHealth overloadedHealth : overloaded) {
            for (WorkerHealth underloadedHealth : underloaded) {
                WorkerMineWright overloadedWorker = overloadedHealth.getWorker();
                WorkerMineWright underloadedWorker = underloadedHealth.getWorker();

                if (overloadedWorker.getCurrentTask() != null) {
                    Task task = migrateTask(overloadedWorker.getMineWrightName(),
                                           overloadedWorker.getCurrentTask());

                    if (task != null) {
                        underloadedWorker.onTaskAwarded(task);
                        overloadedWorker.cancelCurrentTask();

                        MineWrightMod.LOGGER.info("Rebalanced: {} -> {}",
                            overloadedWorker.getMineWrightName(),
                            underloadedWorker.getMineWrightName());

                        break;
                    }
                }
            }
        }
    }

    /**
     * Tick method called every game tick
     */
    public void tick() {
        // Update worker heartbeats
        for (WorkerHealth health : healthStatus.values()) {
            if (health.getWorker() != null) {
                updateHeartbeat(health.getWorker().getMineWrightName());
            }
        }
    }

    /**
     * Shutdown monitor
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Worker health tracking
 */
class WorkerHealth {
    private final String workerId;
    private long lastHeartbeat;
    private int successCount = 0;
    private int failureCount = 0;

    public WorkerHealth(String workerId) {
        this.workerId = workerId;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public void recordSuccess() {
        successCount++;
    }

    public void recordFailure() {
        failureCount++;
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public double getFailureRate() {
        int total = successCount + failureCount;
        return total > 0 ? (double) failureCount / total : 0.0;
    }

    public WorkerMineWright getWorker() {
        return (WorkerMineWright) MineWrightManager.getMineWright(workerId);
    }
}
```

---

## 7. Sources

### Framework Documentation
- [Microsoft AutoGen Framework](https://github.com/microsoft/autogen) - Multi-agent conversation framework
- [CrewAI Documentation](https://docs.crewai.com/) - Role-playing agent framework
- [LangGraph Guide](https://langchain-ai.github.io/langgraph/) - Stateful agent workflows
- [CAMEL-AI Official Site](https://www.camel-ai.org) - Communicative agents framework
- [MetaGPT GitHub](https://github.com/geekan/MetaGPT) - Software company simulation
- [OpenAI Agents SDK](https://github.com/openai/openai-agents-python) - Production orchestration

### Research Papers
- [Agentic AI Frameworks: Architectures, Protocols](https://arxiv.org/abs/2508.10146) - Comprehensive framework analysis
- [HiBerNAC: Hierarchical Neural Agent Collective](https://arxiv.org/html/2506.08296v3) - Brain-inspired hierarchy
- [HTN-enhanced Multi-Agent Reinforcement Learning](https://www.sciencedirect.com/science/article/abs/pii/S0893608025001339) - HTN + MARL integration
- [Coral Protocol](https://arxiv.org/html/2505.00749v2) - Multi-agent infrastructure
- [SHIELDA: Exception Handling](https://arxiv.org/html/2508.07935v1) - Failure attribution
- [Agentic Services Computing](https://arxiv.org/html/2509.24380v2) - Resilience patterns

### Protocol Standards
- [A2A Protocol (AWS Blog)](https://aws.amazon.com/blogs/opensource/open-protocols-for-agent-interoperability-part-4-inter-agent-communication-on-a2a/) - Agent-to-agent communication
- [A2A with Semantic Kernel](https://devblogs.microsoft.com/semantic-kernel/guest-blog-building-multi-agent-solutions-with-semantic-kernel-and-a2a-protocol/) - Microsoft implementation
- [MCP Protocol](https://modelcontextprotocol.io/) - Model Context Protocol

### Coordination Patterns
- [Orchestrating Multi-Agent Systems](https://www.linkedin.com/pulse/orchestrating-multi-agent-systems-technical-patterns-complex-kiran-b8o2f) - Technical patterns
- [Event-Driven Multi-Agent Systems](https://www.confluent.io/blog/event-driven-multi-agent-systems/) - Event patterns
- [Multi-Agent Fault Tolerance](https://m.blog.csdn.net/weixin_52908342/article/details/156909180) - Failure handling
- [Spatial Partitioning Algorithms](https://m.zhangqiaokeyan.com/academic-conference-foreign_7th-world-multiconference-systemics-cybernetics-informatics_thesis_020513505635.html) - Parallel processing
- [Human-in-the-Loop Oversight](https://m.blog.csdn.net/warm3snow/article/details/158348995) - Governance patterns

### Minecraft Multi-Agent Projects
- [MineWright AI - GitHub](https://github.com/YuvDwi/MineWright) - Autonomous Minecraft agents
- [Mindcraft Platform](https://gitcode.com/GitCode/Trending/mi/mindcraft) - Research platform (1000 agents!)
- [STEVE Series Research](https://hub.baai.ac.cn/paper/de9ad466-90a1-4a01-8347-4efd099d135d) - 2.5-7.3x efficiency gains
- [Anemoi: Semi-Centralized System](https://arxiv.org/html/2508.17068v3) - Planner + workers model

### Chinese Resources (Machine Translated)
- [AutoGen Guide (CSDN)](https://m.blog.csdn.net/youmaob/article/details/157971799) - Framework comparison
- [CrewAI Tutorial](https://m.blog.csdn.net/gitblog_00233/article/details/157236986) - Complete guide
- [LangGraph Tutorial](https://m.blog.csdn.net/Android_xue/article/details/158385169) - Quick start
- [HTN Planning](https://m.blog.csdn.net/asd343442/article/details/146603808) - Hierarchical planning
- [Contract Net Protocol](https://m.blog.csdn.net/qq_38998213/article/details/151226166) - Task allocation

---

## Appendix: Quick Reference

### Key Patterns Summary
- **HTN**: Goal → Compound Tasks → Primitive Tasks
- **CNP**: Announce → Bid → Award → Execute
- **Blackboard**: Shared state, observers notified
- **Master-Worker**: Coordinator delegates, workers execute

### Communication Flow
```
Player → Foreman MineWright
         ↓
    HTN Decompose
         ↓
    CNP Allocate
         ↓
Worker MineWrights ←→ Blackboard
         ↓
    Report Results
         ↓
Player (via Oversight)
```

### State Transitions
```
Foreman: IDLE → DECOMPOSING → ALLOCATING → MONITORING → COMPLETE
Worker: IDLE → BIDDING → ASSIGNED → EXECUTING → REPORTING
```

### Failure Handling
1. Detect (heartbeat timeout, high failure rate)
2. Preserve (save task state)
3. Migrate (find replacement worker)
4. Resume (continue execution)
5. Rebalance (redistribute load)

---

**Document Version:** 1.0
**Last Updated:** 2026-02-26
**Next Review:** After Phase 1 implementation
