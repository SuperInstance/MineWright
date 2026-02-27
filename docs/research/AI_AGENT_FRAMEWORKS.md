# AI Agent Frameworks Research - Architecture Patterns for MineWright

**Research Date:** 2026-02-27
**Target Project:** MineWright (Steve AI - Minecraft Agent Framework)
**Goal:** Identify architecture patterns from popular AI agent frameworks for adoption

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [LangChain Agent Architecture](#langchain-agent-architecture)
3. [AutoGPT Architecture Patterns](#autogpt-architecture-patterns)
4. [CrewAI Multi-Agent Coordination](#crewai-multi-agent-coordination)
5. [Microsoft Semantic Kernel](#microsoft-semantic-kernel)
6. [OpenAI Swarm Agents](#openai-swarm-agents)
7. [Cross-Framework Analysis](#cross-framework-analysis)
8. [Adoptable Patterns for MineWright](#adoptable-patterns-for-minewright)
9. [Recommended Implementation Roadmap](#recommended-implementation-roadmap)

---

## Executive Summary

This research analyzes five leading AI agent frameworks to identify architectural patterns that can enhance MineWright's capabilities. The frameworks studied demonstrate mature patterns in:

- **Task Decomposition**: Breaking complex goals into executable subtasks
- **Memory Management**: Short-term context and long-term knowledge persistence
- **Tool/Function Calling**: Extensible action registries with type safety
- **Multi-Agent Communication**: Coordination protocols and handoff mechanisms
- **Error Recovery**: Self-reflection, retry strategies, and graceful degradation
- **State Management**: Durable execution with checkpointing and streaming

Key insight: All frameworks converge on a **ReAct (Reason + Act)** pattern where agents iteratively reason about tasks, take actions, observe results, and adjust strategies.

---

## LangChain Agent Architecture

### Core Architecture Components

| Component | Description | MineWright Equivalent |
|-----------|-------------|----------------------|
| **Models (LLMs)** | Language model interface for reasoning and planning | `TaskPlanner` with OpenAI/Groq/Gemini clients |
| **Chains** | Organize model calls in sequences | `ActionExecutor` task queue |
| **Agents** | Intelligent decision-making systems | `SteveEntity` with planning capability |
| **Tools** | External interface encapsulation | `ActionRegistry` with `ActionFactory` |
| **Memory** | Conversation context and long-term storage | `SteveMemory` system |
| **Prompts** | Template management | `PromptBuilder` |

### Key Agent Capabilities

1. **Planning** - Task decomposition, reflection, self-evaluation
2. **Memory** - Short-term and long-term with fast retrieval
3. **Tools** - Execution units for external APIs
4. **Action** - Actual execution of decisions
5. **Collaboration** - Interaction with other agents

### LangGraph: Low-Level Orchestration

LangChain v1.0 introduced **LangGraph**, a durable execution framework providing:

```python
# Conceptual LangGraph flow
state = {
    "messages": [],
    "current_agent": None,
    "context": {}
}

# Graph nodes represent agent states
# Edges represent transitions with conditions
# Provides persistence, streaming, human-in-the-loop
```

**Key Features:**
- **Durable Execution**: State persists across executions
- **Streaming**: Real-time progress updates
- **Human-in-the-Loop**: Pause and request human input
- **Persistence Management**: Checkpoint and recovery

### Agent Workflow Pattern

```
1. LLM receives input -> Decides actions
2. Agent selects tools and determines inputs
3. Tools execute and return results
4. Results feed back -> Agent decides if more actions needed
5. Task completes or continues iterating
```

### Architectural Principles

- **Composability**: Independent components that work combined
- **Layered Abstraction**: Low-level LLM calls to high-level agent behavior
- **Plugin Architecture**: Tools and integrations as plugins
- **State Management**: Memory components for conversation state

### Applicable Patterns for MineWright

1. **LangGraph-style State Machine**: Implement a graph-based state machine with checkpointing
2. **Streaming Updates**: Provide real-time progress feedback to players
3. **Prompt Templates**: More sophisticated prompt building with few-shot examples
4. **Tool Registration**: Standardized tool metadata (schema, descriptions, examples)

---

## AutoGPT Architecture Patterns

### Five-Layer Architecture

```
┌─────────────────────────────────────┐
│      User Interface Layer           │  <- Natural language goal input
└─────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│   Goals & Memory Management         │  <- Task list, action history, memory index
└─────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│   Reasoning & Planning Engine       │  <- LLM-driven TAO loop core
└─────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│      Tool Execution Layer           │  <- Search engine, file system, code interpreter
└─────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│   Observation & Feedback Collection │  <- Capture results, inject into next reasoning
└─────────────────────────────────────┘
```

### Four Core Components

| Component | Description | MineWright Equivalent |
|-----------|-------------|----------------------|
| **LLM Brain** | GPT-4/3.5 for thinking and decision-making | `TaskPlanner` |
| **Memory System** | Short-term context + long-term vector DB | `SteveMemory` (needs enhancement) |
| **Tool Set** | Web browsing, files, code execution | `ActionRegistry` |
| **Execution Engine** | Planning -> Execution -> Check -> Iteration | `ActionExecutor` |

### Core Execution Loop: TAO Cycle

AutoGPT follows a **Think-Act-Observe** closed loop:

1. **Task Definition**: Prompt-based with scenario, user, role, goals
2. **Task Understanding**: LLM semantic comprehension (Thinking)
3. **Solution Generation**: Detailed step-by-step plans (Planning)
4. **Instruction Generation**: Selecting priority steps (Criticism)
5. **Instruction Execution**: Accessing external resources
6. **Self-Monitoring**: Feedback collection and strategy adjustment

### Classic vs Forge Architecture

**Classic (Legacy):**
- Single-agent loop
- One LLM playing multiple roles (thinking, reasoning, criticizing, planning)

**Forge (New):**
- Multi-agent collaboration
- Specialized agents for research, analysis, content generation
- Better separation of concerns

### Memory Management

- **Short-term Memory**: Current session context
- **Long-term Memory**: Vector database for cross-session persistence
- **Retrieval**: Relevant historical information for better decisions

### Applicable Patterns for MineWright

1. **TAO Loop**: Explicit Think-Act-Observe cycle in agent behavior
2. **Vector Memory**: Add vector embeddings for fast context retrieval
3. **Self-Criticism**: Agent self-evaluation before action execution
4. **Multi-Agent Specialization**: Different agent types (builder, miner, explorer)

---

## CrewAI Multi-Agent Coordination

### Core Concept

CrewAI coordinates **role-playing autonomous AI agents** that work as a cohesive "team." Agents collaborate, delegate tasks, and communicate like a real-world team.

### Three Core Abstractions

```python
# CrewAI Structure
Agent(
    role="Research Analyst",
    goal="Find relevant information",
    backstory="Expert in data analysis",
    tools=[search_tool, database_tool]
)

Task(
    description="Research Minecraft building techniques",
    expected_output="Detailed report on building patterns"
)

Crew(
    agents=[researcher, analyst, writer],
    process="sequential",  # or "hierarchical"
    tasks=[task1, task2, task3]
)
```

### Core Components

| Component | Description | MineWright Equivalent |
|-----------|-------------|----------------------|
| **Agent** | Role, goal, backstory, tools | `SteveEntity` (needs role system) |
| **Task** | Description and expected output | Current `Task` object (needs enhancement) |
| **Crew** | Execution flow (sequential/hierarchical) | `CollaborativeBuildManager` |
| **Process** | Sequential or Hierarchical execution | `AgentStateMachine` |

### Execution Modes

1. **Sequential**: Tasks executed in order
2. **Hierarchical**: Manager agent coordinates worker agents

### Role-Based Specialization

Instead of one general-purpose agent, CrewAI uses specialists:
- **Researcher**: Gather information
- **Analyst**: Process and analyze
- **Writer**: Create content
- **Reviewer**: Quality check

### Comparison: Single Agent vs CrewAI

| Aspect | Single Agent | CrewAI |
|--------|--------------|--------|
| **Code Required** | More complex prompts | 30% less code |
| **Task Handling** | General-purpose | Specialized roles |
| **Explainability** | Black box | Clear role responsibilities |
| **Robustness** | Single point of failure | Distributed fault tolerance |

### Best Use Cases

- **Long-chain workflows** (research reports, competitive analysis)
- **Content pipelines** (design-code-test)
- **Customer support** (tiered escalation)
- **Not recommended**: Simple single-LLM tasks, real-time scenarios

### Enterprise Adoption

- Nearly 50% of Fortune 500 companies use CrewAI
- Scales to 30+ agents with structured coordination
- Proven in production environments

### Applicable Patterns for MineWright

1. **Role-Based Agents**: Define agent roles (Builder, Miner, Explorer, Farmer)
2. **Task Dependencies**: Explicit task prerequisites and ordering
3. **Hierarchical Coordination**: Lead agent coordinates specialist agents
4. **Shared Context**: Crew-level context accessible to all agents

---

## Microsoft Semantic Kernel

### Overview

**Semantic Kernel (SK)** is Microsoft's lightweight, open-source SDK for building AI agents. It powers Microsoft 365 Copilot and Bing Copilot.

### Key Features

1. **Unified Abstraction Layer**
   - Consistent interface across AI models (OpenAI, Azure, Hugging Face)
   - Central coordinator managing plugins, memory, execution

2. **Agent Framework**
   - `ChatCompletionAgent`: Standard chat-based interactions
   - `OpenAIAssistantAgent`: OpenAI Assistant integration
   - Multi-agent collaboration support

3. **Core Components**
   - **Kernel**: Central coordinator
   - **Plugins**: AI-callable skills
   - **Memory**: Short-term and long-term
   - **Process Framework**: Automated workflows

### Agent Types

```csharp
// ChatCompletionAgent example
var agent = new ChatCompletionAgent()
{
    Name = "BuilderAgent",
    Instructions = "You are a Minecraft building expert",
    Kernel = kernel
};

// Kernel with plugins
var kernel = Kernel.CreateBuilder()
    .AddOpenAIChatCompletion(modelId, apiKey)
    .AddPlugin(new MinecraftPlugin())
    .Build();
```

### Microsoft Agent Framework Integration

Combines:
- **Semantic Kernel** (stability, enterprise features)
- **AutoGen** (experimental multi-agent orchestration)

### Enterprise-Ready Features

- Production-ready with stability commitments
- Built-in observability and security filters
- Enterprise system connectors
- Content moderation and telemetry
- OpenAPI specification support

### Applicable Patterns for MineWright

1. **Plugin Architecture**: Kernel-based plugin loading (similar to current SPI approach)
2. **Process Framework**: Define reusable workflows (e.g., "build house", "mine tunnel")
3. **Observability**: Built-in telemetry and logging
4. **Type Safety**: Strongly typed plugin interfaces

---

## OpenAI Swarm Agents

### Overview

**Swarm** is an experimental framework for **lightweight multi-agent orchestration**. It has been superseded by the **OpenAI Agents SDK** (production-ready).

### Core Concepts

| Concept | Description | MineWright Equivalent |
|-----------|-------------|----------------------|
| **Agent** | Instructions and tools | `SteveEntity` |
| **Handoff** | Transfer tasks between agents | `CollaborativeBuildManager` coordination |
| **Context Variables** | Shared state across agents | `SteveMemory` (needs enhancement) |

### Handoff Mechanism

```python
# Swarm handoff pattern
def transfer_to_agent_b():
    return agent_b

agent_a = Agent(
    name="Builder Agent",
    instructions="You build structures",
    functions=[transfer_to_agent_b]
)

agent_b = Agent(
    name="Miner Agent",
    instructions="You mine resources"
)

# Seamless handoff during execution
```

### OpenAI Agents SDK (Production)

Enhanced features:
- Native async support (async/await)
- Deep MCP (Model Context Protocol) integration
- Unified context management (shared backpack)
- Security guardrails

### Key Features

- **Lightweight & Flexible**: Minimal overhead
- **Decentralized Collaboration**: Autonomous handoff without central control
- **Function Calling**: External tool integration
- **Multi-round Conversations**: Complex dialogue handling

### Applicable Patterns for MineWright

1. **Handoff Protocol**: Agent-to-agent task transfer mechanism
2. **Context Backpack**: Shared context passed between agents
3. **Async Native**: Non-blocking agent coordination
4. **Lightweight Coordination**: Minimal overhead for multi-agent scenarios

---

## Cross-Framework Analysis

### Common Patterns

All five frameworks converge on these patterns:

| Pattern | Description | Frameworks Using It |
|---------|-------------|---------------------|
| **ReAct Loop** | Reason -> Act -> Observe cycle | All |
| **Tool Registry** | Extensible function calling | LangChain, SK, Swarm |
| **Memory Layers** | Short-term + Long-term storage | AutoGPT, LangChain, SK |
| **State Persistence** | Durable execution with checkpoints | LangGraph, SK |
| **Multi-Agent Coord** | Specialist agents collaboration | CrewAI, Swarm, AutoGPT Forge |
| **Streaming Responses** | Real-time progress updates | LangGraph, Agents SDK |
| **Error Recovery** | Self-reflection and retry | AutoGPT, CrewAI |

### Unique Strengths

| Framework | Unique Strength |
|-----------|-----------------|
| **LangChain** | LangGraph state machines, extensive tool ecosystem |
| **AutoGPT** | Self-reflection (TAO), vector memory |
| **CrewAI** | Role-based specialization, hierarchical coordination |
| **Semantic Kernel** | Enterprise-ready, type-safe plugins |
| **Swarm** | Lightweight handoffs, educational clarity |

### Architecture Maturity

```
Production-Ready       Swarm
                        ↓
Enterprise             CrewAI, Semantic Kernel
                        ↓
Mature                 LangChain (LangGraph)
                        ↓
Experimental           AutoGPT (Forge)
```

---

## Adoptable Patterns for MineWright

### 1. Enhanced Task Decomposition

**Current State:**
- `TaskPlanner.planTasksAsync()` returns basic task list
- Linear task execution

**Adopted Pattern:**
```java
// LangChain-style task decomposition with dependencies
class TaskDecomposition {
    String taskId;
    String description;
    List<String> dependencies;  // Prerequisites
    TaskPriority priority;
    TaskStatus status;
    Map<String, Object> context;
}

// Recursive decomposition
interface TaskDecomposer {
    List<TaskDecomposition> decompose(
        String userGoal,
        int maxDepth
    );
}
```

**Benefits:**
- Parallel task execution when dependencies allow
- Better task prioritization
- Clearer task relationships

### 2. TAO (Think-Act-Observe) Loop

**Current State:**
- Direct action execution
- No explicit reasoning phase

**Adopted Pattern:**
```java
// AutoGPT-style TAO cycle
class TAOLoop {
    ThinkResult think(Task task, Context context) {
        // LLM reasoning about task
        // Consider available tools
        // Plan approach
    }

    ActionResult act(ThinkResult plan) {
        // Execute planned action
        // Handle tool calling
    }

    void observe(ActionResult result, Context context) {
        // Update context with results
        // Evaluate success/failure
        // Decide if iteration needed
    }
}
```

**Benefits:**
- More intelligent decision-making
- Self-correction capability
- Better explainability

### 3. Vector Memory System

**Current State:**
- `SteveMemory` with conversation history
- Simple context retrieval

**Adopted Pattern:**
```java
// Vector-based memory (AutoGPT, LangChain)
interface VectorMemory {
    void store(String content, Map<String, Object> metadata);

    List<MemoryResult> retrieve(
        String query,
        int topK,
        double similarityThreshold
    );

    void summarize();  // Condense old memories
}

class MemoryResult {
    String content;
    double similarity;
    Map<String, Object> metadata;
    LocalDateTime timestamp;
}
```

**Benefits:**
- Semantic search across memories
- Better context relevance
- Scalable memory storage

### 4. Role-Based Agent System

**Current State:**
- Generic `SteveEntity`
- All agents have same capabilities

**Adopted Pattern:**
```java
// CrewAI-style role system
enum AgentRole {
    BUILDER("Builds structures", BuildingActions.class),
    MINER("Mines resources", MiningActions.class),
    EXPLORER("Explores terrain", ExplorationActions.class),
    FARMER("Manages farms", FarmingActions.class),
    COORDINATOR("Manages other agents", CoordinationActions.class);
}

class RoleBasedAgent extends SteveEntity {
    private AgentRole primaryRole;
    private List<AgentRole> secondaryRoles;
    private String backstory;  // Role-specific context

    void setRole(AgentRole role) {
        this.primaryRole = role;
        this.capabilities = role.getActions();
        this.systemPrompt = buildRolePrompt(role);
    }
}
```

**Benefits:**
- Specialized behavior per role
- Clearer agent responsibilities
- Better multi-agent collaboration

### 5. Handoff Protocol

**Current State:**
- `CollaborativeBuildManager` handles spatial coordination
- No explicit handoff mechanism

**Adopted Pattern:**
```java
// Swarm-style handoff
interface HandoffProtocol {
    boolean canHandle(Task task);
    HandoffResult handoff(Task task, AgentContext context);
}

class HandoffResult {
    Agent targetAgent;
    Task transferredTask;
    Map<String, Object> contextBackpack;
    HandoffStatus status;
}

class BuilderAgent implements HandoffProtocol {
    public HandoffResult handoff(Task task, AgentContext ctx) {
        if (task.requiresMining()) {
            return HandoffResult.to(minerAgent, task, ctx);
        }
        return HandoffResult.keep(task);
    }
}
```

**Benefits:**
- Clear task transfer logic
- Context preservation during handoff
- Dynamic agent specialization

### 6. State Machine with Checkpoints

**Current State:**
- `AgentStateMachine` with basic states
- No persistence

**Adopted Pattern:**
```java
// LangGraph-style state machine
@StateMachine
class AgentStateMachine {
    @State
    private PlanningState planning;

    @State
    private ExecutingState executing;

    @Transition
    void planToExecute(PlanningState from, ExecutingState to) {
        checkpoint(from);  // Save state
    }

    @Checkpoint
    void checkpoint(AgentState state) {
        persistence.save(state);
    }

    void restore(String checkpointId) {
        AgentState state = persistence.load(checkpointId);
        setState(state);
    }
}
```

**Benefits:**
- Recover from crashes
- Pause and resume
- Debug complex workflows

### 7. Streaming Progress Updates

**Current State:**
- Tick-based execution
- No progress streaming

**Adopted Pattern:**
```java
// LangGraph-style streaming
interface ProgressStream {
    void emit(ProgressUpdate update);
}

class ProgressUpdate {
    String agentId;
    String taskId;
    double progress;  // 0.0 to 1.0
    String message;
    Object data;

    void toPlayer(ServerPlayer player) {
        // Display in GUI or chat
    }
}

// In ActionExecutor
public void tick() {
    for (Action action : activeActions) {
        ProgressUpdate update = action.getProgress();
        stream.emit(update);
    }
}
```

**Benefits:**
- Better user feedback
- Real-time monitoring
- Improved UX

### 8. Tool/Action Registry with Metadata

**Current State:**
- `ActionRegistry` with factory pattern
- Basic registration

**Adopted Pattern:**
```java
// Enhanced action registry (Semantic Kernel style)
interface ActionRegistry {
    void register(ActionMetadata metadata, ActionFactory factory);

    ActionMetadata getMetadata(String actionId);
    List<ActionMetadata> findActions(String capability);
}

class ActionMetadata {
    String id;
    String name;
    String description;

    // Schema for LLM understanding
    String parameterSchema;  // JSON Schema
    String resultSchema;

    // Examples for few-shot prompting
    List<ActionExample> examples;

    // Capabilities for discovery
    List<String> capabilities;

    // Execution metadata
    int estimatedTicks;
    boolean isBlocking;
    List<String> prerequisites;
}
```

**Benefits:**
- Better LLM tool selection
- Self-documenting actions
- Easier action discovery

### 9. Self-Reflection and Criticism

**Current State:**
- No self-evaluation
- Direct action execution

**Adopted Pattern:**
```java
// AutoGPT-style criticism
class SelfReflection {
    CritiqueResult critique(
        Task task,
        Action plannedAction,
        Context context
    ) {
        // Ask LLM: "Will this action succeed?"
        // Consider: resources, obstacles, alternatives
    }

    Action suggestAlternative(
        Task task,
        Action failedAction,
        Context context
    ) {
        // Ask LLM: "What should we do instead?"
    }
}

class CritiqueResult {
    boolean willSucceed;
    double confidence;
    List<String> concerns;
    Optional<Action> alternative;
}
```

**Benefits:**
- Fewer failed actions
- Better resource usage
- More intelligent behavior

### 10. Hierarchical Coordination

**Current State:**
- Flat agent coordination
- Spatial partitioning only

**Adopted Pattern:**
```java
// CrewAI-style hierarchy
class CoordinatorAgent extends SteveEntity {
    private List<WorkerAgent> workers;

    void coordinate(Task project) {
        // Decompose into subtasks
        List<SubTask> subtasks = decompose(project);

        // Assign to workers based on role
        for (SubTask task : subtasks) {
            WorkerAgent best = selectBestWorker(task);
            best.assign(task);
        }

        // Monitor and rebalance
        monitorProgress();
    }

    private WorkerAgent selectBestWorker(SubTask task) {
        // Consider: role, location, current load
        return workers.stream()
            .filter(w -> w.canHandle(task))
            .min(comparing(w -> w.getLoad()))
            .orElse(null);
    }
}
```

**Benefits:**
- Better task allocation
- Dynamic load balancing
- Scalable to many agents

---

## Recommended Implementation Roadmap

### Phase 1: Foundation (Immediate)

**Priority: High**

1. **TAO Loop Integration**
   - Add explicit Think phase before action execution
   - Implement Observe phase with result evaluation
   - Add self-reflection before critical actions

2. **Enhanced Task Decomposition**
   - Add task dependencies
   - Support parallel task execution
   - Implement task prioritization

3. **Action Metadata**
   - Add JSON Schema for parameters
   - Include examples in action registry
   - Auto-generate tool descriptions for LLM

### Phase 2: Memory & Learning (Short-term)

**Priority: High**

4. **Vector Memory System**
   - Integrate vector database (Redis/PGVector)
   - Implement semantic search
   - Add memory summarization

5. **State Persistence**
   - Implement checkpointing
   - Add crash recovery
   - Support pause/resume

### Phase 3: Multi-Agent (Medium-term)

**Priority: Medium**

6. **Role-Based Agents**
   - Define agent roles
   - Implement role-specific capabilities
   - Add role assignment logic

7. **Handoff Protocol**
   - Implement agent-to-agent handoff
   - Add context backpack
   - Support dynamic role switching

8. **Hierarchical Coordination**
   - Implement coordinator agent
   - Add task allocation logic
   - Support load balancing

### Phase 4: Experience (Long-term)

**Priority: Medium**

9. **Streaming Progress**
   - Add progress streaming
   - Implement GUI updates
   - Support player notifications

10. **Advanced Reflection**
    - Implement post-action analysis
    - Add learning from failures
    - Support strategy adaptation

### Phase 5: Enterprise (Optional)

**Priority: Low**

11. **Observability**
    - Add telemetry collection
    - Implement distributed tracing
    - Support performance monitoring

12. **Security**
    - Add action validation
    - Implement rate limiting
    - Support permission checks

---

## Summary

The five AI agent frameworks studied (LangChain, AutoGPT, CrewAI, Semantic Kernel, Swarm) demonstrate convergent patterns that can significantly enhance MineWright:

**Key Takeaways:**

1. **ReAct Loop**: All frameworks use Reason-Act-Observe cycles
2. **Memory**: Vector-based retrieval is standard for long-term memory
3. **Specialization**: Role-based agents outperform general-purpose agents
4. **Coordination**: Handoff mechanisms enable seamless agent collaboration
5. **Persistence**: Checkpointing enables recovery from failures
6. **Streaming**: Real-time updates improve user experience

**Recommended Priority:**

1. **Implement TAO Loop** - Biggest impact on intelligence
2. **Add Vector Memory** - Enables learning and context retrieval
3. **Define Agent Roles** - Foundation for multi-agent scenarios
4. **Enhance Tool Metadata** - Better LLM understanding
5. **Add Checkpointing** - Production reliability

**Next Steps:**

1. Review existing codebase for integration points
2. Prototype TAO Loop in `TaskPlanner`
3. Evaluate vector database options
4. Design agent role system
5. Plan phased implementation

---

## Sources

### LangChain
- [LangChain Agent Architecture Overview](https://m.blog.csdn.net/ttyy1112/article/details/156028277)
- [LangChain Agents Ultimate Guide](https://blog.csdn.net/gitblog_00156/article/details/154157035)
- [LangGraph and LangChain 1.0 Alpha](https://news.miracleplus.com/share_link/87110)

### AutoGPT
- [AutoGPT Five-Layer Architecture](https://m.toutiao.com/a7610454297817465354/)
- [LangChain Full Stack Tutorial with RAG+Agents](https://blog.csdn.net/m0_59163425/article/details/158042806)
- [Self-Improvement Agent Training System](https://www.51cto.com/aigc/8708.html)

### CrewAI
- [CrewAI Multi-Agent Framework](https://www.ibm.com/cn-zh/think/topics/crew-ai)
- [CrewAI Collaborative Multi-Agent System](https://cloud.tencent.com/developer/article/2625388)
- [CrewAI Complete Guide](https://m.blog.csdn.net/gitblog_00222/article/details/157194037)
- [CrewAI Enterprise Applications](https://m.weibo.cn/status/Qg5eX0yDv)

### Microsoft Semantic Kernel
- [Semantic Kernel Overview](https://m.blog.csdn.net/l01011_/article/details/155381463)
- [Microsoft Agent Framework Integration](https://m.blog.csdn.net/csdn_430422/article/details/157259474)

### OpenAI Swarm
- [Swarm GitHub Repository](https://github.com/openai/swarm/tree/main)
- [CrewAI Getting Started Tutorial](https://m.runoob.com/ai-agent/crewai-agent.html)

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Author:** Claude Code (Orchestrator Agent)
**Project:** MineWright (Steve AI)
