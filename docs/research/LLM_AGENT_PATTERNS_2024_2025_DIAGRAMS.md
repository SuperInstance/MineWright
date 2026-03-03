# LLM Agent Patterns Visual Diagrams

**Companion to:** LLM_AGENT_PATTERNS_2024_2025.md

---

## 1. OpenAI Agents SDK Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Agent Kit (UI Layer)                        │
│         Visual Builder, Quick Interfaces, Dashboard             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   Agents SDK (Framework)                        │
│                                                                 │
│   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐           │
│   │   Agents    │  │  Handoffs   │  │ Guardrails  │           │
│   │ (LLM+Tools) │  │ (Control)   │  │ (Safety)    │           │
│   └─────────────┘  └─────────────┘  └─────────────┘           │
│                                                                 │
│   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐           │
│   │  Sessions   │  │  Tracing    │  │  Evals API  │           │
│   │ (History)   │  │ (Debugging) │  │ (Testing)   │           │
│   └─────────────┘  └─────────────┘  └─────────────┘           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                Primitives (Low-Level API)                       │
│         Direct Model Calls, Polling, Raw Responses              │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. LangGraph State Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                          StateGraph                             │
│                                                                 │
│   ┌─────────┐      ┌─────────┐      ┌─────────┐               │
│   │ Analyze │ ───▶ │ Search  │ ───▶ │ Generate│               │
│   │   Node  │      │   Node  │      │   Node  │               │
│   └─────────┘      └─────────┘      └────┬────┘               │
│                                          │                     │
│                                          ▼                     │
│                                    ┌─────────┐                │
│                                    │  Review │                │
│                                    │   Node  │                │
│                                    └────┬────┘                │
│                                         │                     │
│                         ┌───────────────┴───────────────┐     │
│                         │                               │     │
│                         ▼                               ▼     │
│                    score > 0.8                     score ≤ 0.8  │
│                         │                               │     │
│                         ▼                               ▼     │
│                    ┌─────────┐                    ┌─────────┐ │
│                    │   END   │                    │ Generate│ │
│                    │ (Done)  │                    │ (Revise)│ │
│                    └─────────┘                    └─────────┘ │
└─────────────────────────────────────────────────────────────────┘

State: { messages: List, current_step: String, retry_count: int }
Checkpoint: PostgreSQL/Redis (resume from any node)
```

---

## 3. Multi-Agent Patterns Comparison

```
┌─────────────────────────────────────────────────────────────────┐
│                    SUPERVISOR PATTERN                           │
│                                                                 │
│                        ┌─────────────┐                         │
│                        │ Supervisor  │                         │
│                        │   Agent     │                         │
│                        │ (Controller)│                         │
│                        └──────┬──────┘                         │
│                               │                                 │
│          ┌────────────────────┼────────────────────┐           │
│          ▼                    ▼                    ▼           │
│   ┌──────────┐         ┌──────────┐         ┌──────────┐     │
│   │ Worker 1 │         │ Worker 2 │         │ Worker 3 │     │
│   │ (Mining) │         │(Building)│         │(Combat)  │     │
│   └──────────┘         └──────────┘         └──────────┘     │
│                                                                 │
│   Characteristics: Centralized, predictable, controlled         │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      SWARM PATTERN                              │
│                                                                 │
│   ┌──────────┐    ┌──────────┐    ┌──────────┐                │
│   │  Agent 1 │◄──►│  Agent 2 │◄──►│  Agent 3 │                │
│   │ (Triage) │    │(Weather) │    │ (Sales)  │                │
│   └────┬─────┘    └─────┬────┘    └─────┬────┘                │
│        │ handoff         │ handoff         │                   │
│        ▼                 ▼                 ▼                   │
│   ┌──────────┐    ┌──────────┐    ┌──────────┐                │
│   │  Agent 4 │    │  Agent 5 │    │  Agent 6 │                │
│   │(Refund)  │    │ (Human)  │    │(Support) │                │
│   └──────────┘    └──────────┘    └──────────┘                │
│                                                                 │
│   Characteristics: Decentralized, autonomous, flexible          │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   LANGGRAPH TOOL ROUTING                        │
│                                                                 │
│   ┌──────────┐      ┌──────────┐      ┌──────────┐            │
│   │  Agent   │ ──▶ │transfer  │ ──▶ │conditional│            │
│   │   A      │      │_to_B()   │      │  edge    │            │
│   └──────────┘      └──────────┘      └─────┬────┘            │
│                                            │                   │
│                                            ▼                   │
│                                    ┌──────────┐               │
│                                    │  Agent   │               │
│                                    │    B     │               │
│                                    └──────────┘               │
│                                                                 │
│   Characteristics: Tool-based routing, conditional edges       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. MCP (Model Context Protocol) Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     LLM Agent Layer                             │
│                                                                 │
│   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐           │
│   │    GPT-4    │  │   Claude    │  │   Gemini    │           │
│   │    Agent    │  │    Agent    │  │    Agent    │           │
│   └──────┬──────┘  └──────┬──────┘  └──────┬──────┘           │
│          │                │                │                   │
└──────────┼────────────────┼────────────────┼───────────────────┘
           │                │                │
           └────────────────┼────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MCP Client Layer                             │
│                  (Universal Interface)                          │
└─────────────────────────────────────────────────────────────────┘
                            │
           ┌────────────────┼────────────────┐
           ▼                ▼                ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  MCP Server 1   │ │  MCP Server 2   │ │  MCP Server 3   │
│   (GitHub)      │ │  (Weather)      │ │  (Database)     │
└────────┬────────┘ └────────┬────────┘ └────────┬────────┘
         │                   │                   │
         ▼                   ▼                   ▼
    GitHub API          Weather API          PostgreSQL
```

**Key Benefit:** Build once, use across multiple AI models

---

## 5. Heterogeneous Agent Architecture (SLM-First)

```
┌─────────────────────────────────────────────────────────────────┐
│                   Task Classification                           │
│                                                                 │
│   Incoming Task ──▶ Complexity Analyzer ──▶ Route Decision      │
└─────────────────────────────────────────────────────────────────┘
                            │
         ┌──────────────────┼──────────────────┐
         │                  │                  │
         ▼                  ▼                  ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  Simple Task    │ │  Complex Task   │ │  Novel Task     │
│  (Repetitive)   │ │  (Multi-step)   │ │  (Uncertain)    │
└────────┬────────┘ └────────┬────────┘ └────────┬────────┘
         │                  │                  │
         ▼                  ▼                  ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   Small Model   │ │   Medium Model  │ │   Large Model   │
│   (3B-7B)       │ │   (13B-34B)     │ │   (70B+)        │
│                 │ │                 │ │                 │
│   • Fast        │ │   • Capable     │ │   • Powerful    │
│   • Cheap       │ │   • Balanced    │ │   • Smart       │
│   • Local       │ │   • Scalable    │ │   • Expensive   │
└─────────────────┘ └─────────────────┘ └─────────────────┘
    40-70% of calls     20-30% of calls     10-20% of calls

**Expected Savings:** 40-60% reduction in token costs
```

---

## 6. GraphRAG Memory Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Multi-Tier Memory                            │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐  │
│   │  Current Context (Cache)                                │  │
│   │  Recent conversation turns - Last 10 messages           │  │
│   │  Storage: In-memory / Sliding Window                    │  │
│   └─────────────────────────────────────────────────────────┘  │
│                           ↓                                    │
│   ┌─────────────────────────────────────────────────────────┐  │
│   │  Recent Thread (Summaries)                              │  │
│   │  Compressed conversation flow - Key points extracted    │  │
│   │  Storage: Redis / Fast KV Store                         │  │
│   └─────────────────────────────────────────────────────────┘  │
│                           ↓                                    │
│   ┌─────────────────────────────────────────────────────────┐  │
│   │  Long-term Facts (Structured)                            │  │
│   │  User profiles, preferences, configurations             │  │
│   │  Storage: PostgreSQL / MySQL                            │  │
│   └─────────────────────────────────────────────────────────┘  │
│                           ↓                                    │
│   ┌─────────────────────────────────────────────────────────┐  │
│   │  Historical Cases (GraphRAG)                             │  │
│   │  Semantic relationships, multi-hop reasoning             │  │
│   │  Storage: Neo4j / Graph Database                        │  │
│   │                                                         │  │
│   │   ┌─────┐    uses    ┌─────┐                           │  │
│   │   │Mining│──────────▶│Iron │                           │  │
│   │   └─────┘            └─────┘                           │  │
│   │      │                  │                              │  │
│   │  related to        requires                           │  │
│   │      ▼                  ▼                              │  │
│   │   ┌─────┐            ┌─────┐                          │  │
│   │   │Cave │◀───────────│Pickaxe│                         │  │
│   │   └─────┘  contains  └─────┘                          │  │
│   └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. ReAct Pattern (Reasoning + Acting)

```
┌─────────────────────────────────────────────────────────────────┐
│                    ReAct Loop (TAO Cycle)                       │
│                                                                 │
│   ┌───────────┐                                                │
│   │   THOUGHT │   "I need to find the nearest iron ore"        │
│   └─────┬─────┘                                                │
│         │                                                      │
│         ▼                                                      │
│   ┌───────────┐                                                │
│   │   ACTION  │   tool_call: search_nearby_blocks("iron_ore")  │
│   └─────┬─────┘                                                │
│         │                                                      │
│         ▼                                                      │
│   ┌───────────┐                                                │
│   │OBSERVATION│   "Found iron_ore at (10, 64, -25)"            │
│   └─────┬─────┘                                                │
│         │                                                      │
│         ▼                                                      │
│   ┌───────────┐                                                │
│   │   THOUGHT │   "Iron ore is 15 blocks away, need to path"   │
│   └─────┬─────┘                                                │
│         │                                                      │
│         ▼                                                      │
│   ┌───────────┐                                                │
│   │   ACTION  │   tool_call: navigate_to(10, 64, -25)          │
│   └─────┬─────┘                                                │
│         │                                                      │
│         ▼                                                      │
│   ┌───────────┐                                                │
│   │OBSERVATION│   "Arrived at target location"                 │
│   └─────┬─────┘                                                │
│         │                                                      │
│         ▼                                                      │
│   ┌───────────┐                                                │
│   │   ACTION  │   tool_call: mine_block()                      │
│   └─────┬─────┘                                                │
│         │                                                      │
│         ▼                                                      │
│   ┌───────────┐                                                │
│   │FINAL ANSW │   "Successfully mined iron ore"                 │
│   └───────────┘                                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 8. Agent Evaluation Pipeline

```
┌─────────────────────────────────────────────────────────────────┐
│                   Agent Evaluation Framework                    │
│                                                                 │
│   ┌─────────────┐      ┌─────────────┐      ┌─────────────┐   │
│   │   Agent     │ ───▶ │  Scenario   │ ───▶ │  Execution  │   │
│   │ (System)    │      │  (Task)     │      │  (Actions)  │   │
│   └─────────────┘      └─────────────┘      └──────┬──────┘   │
│                                                      │          │
│                                                      ▼          │
│   ┌─────────────┐      ┌─────────────┐      ┌─────────────┐   │
│   │   Results   │ ◀──▶ │   Metrics   │ ───▶ │   Report    │   │
│   │ (Outcome)   │      │ (Scoring)   │      │ (Analysis)  │   │
│   └─────────────┘      └─────────────┘      └─────────────┘   │
│                                                                 │
│   Metrics Collected:                                           │
│   • Success Rate: % of tasks completed                         │
│   • Duration: Time to completion                               │
│   • Token Usage: Total tokens consumed                         │
│   • Trajectory: Sequence of actions taken                      │
│   • Final State: Environmental outcome                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 9. Handoff Flow (Swarm Pattern)

```
┌─────────────────────────────────────────────────────────────────┐
│                    Handoff Execution Flow                       │
│                                                                 │
│   User: "I need to build a house and get iron for tools"       │
│         │                                                        │
│         ▼                                                        │
│   ┌─────────────┐                                               │
│   │ Triage Agent│ "This involves building and mining. I'll     │
│   │ (Initial)   │  handle the building, then transfer for       │
│   └──────┬──────┘  mining."                                     │
│          │                                                        │
│          │ transfer_to_building_agent(reason="House construction")│
│          ▼                                                        │
│   ┌─────────────┐                                               │
│   │Building Ag. │ "Building house foundation..."                │
│   │(Specialist) │ "Foundation complete. Now need iron."          │
│   └──────┬──────┘                                                │
│          │                                                        │
│          │ transfer_to_mining_agent(reason="Need iron for tools") │
│          ▼                                                        │
│   ┌─────────────┐                                               │
│   │ Mining Agent│ "Searching for iron ore..."                   │
│   │(Specialist) │ "Found and mined iron ore."                   │
│   └──────┬──────┘                                                │
│          │                                                        │
│          │ transfer_to_triage_agent(reason="Task complete")      │
│          ▼                                                        │
│   ┌─────────────┐                                               │
│   │ Triage Agent│ "House built and tools ready!"                │
│   │  (Return)   │                                               │
│   └─────────────┘                                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 10. Steve AI: Recommended Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│              Steve AI - Multi-Agent System                      │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐  │
│   │                    MCP Layer                             │  │
│   │  (Standardized Tool Interface)                          │  │
│   │                                                         │  │
│   │  mine_block │ build_structure │ navigate │ combat │    │  │
│   └───────────────────────┬─────────────────────────────────┘  │
│                           │                                     │
│   ┌───────────────────────┴─────────────────────────────────┐  │
│   │                 Handoff Layer                            │  │
│   │           (Agent Transfer Control)                       │  │
│   │                                                         │  │
│   │   transfer_to_mining │ transfer_to_building │ ...      │  │
│   └───────────────────────┬─────────────────────────────────┘  │
│                           │                                     │
│   ┌───────────────────────┴─────────────────────────────────┐  │
│   │                  Agent Layer                             │  │
│   │                                                         │  │
│   │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │  │
│   │  │ Foreman  │  │  Miner   │  │ Builder  │  │ Warrior│ │  │
│   │  │ (Orchestr)│  │(Mining)  │  │(Building)│  │(Combat)│ │  │
│   │  └──────────┘  └──────────┘  └──────────┘  └─────────┘ │  │
│   └───────────────────────┬─────────────────────────────────┘  │
│                           │                                     │
│   ┌───────────────────────┴─────────────────────────────────┐  │
│   │            Heterogeneous Router (Cascade)                │  │
│   │                                                         │  │
│   │   Simple Tasks ──▶ SLM (3B-7B)   │ 40-70% of calls      │  │
│   │   Complex Tasks ──▶ Medium (13B-34B) │ 20-30% of calls  │  │
│   │   Novel Tasks ───▶ LLM (70B+)    │ 10-20% of calls      │  │
│   └───────────────────────┬─────────────────────────────────┘  │
│                           │                                     │
│   ┌───────────────────────┴─────────────────────────────────┐  │
│   │                GraphRAG Memory Layer                     │  │
│   │                                                         │  │
│   │  Skill Library │ Experience Graph │ User Preferences   │  │
│   │  (Neo4j)       │ (Temporal KG)    │ (Profile)          │  │
│   └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Summary of Key Patterns

| Pattern | Use Case | Complexity |
|---------|----------|------------|
| **MCP** | Tool integration | Low |
| **Handoff** | Multi-agent coordination | Medium |
| **GraphRAG** | Memory & skill learning | High |
| **Heterogeneous** | Cost optimization | Medium |
| **ReAct** | Tool reasoning | Low |
| **Swarm** | Decentralized agents | High |
| **Supervisor** | Centralized control | Medium |

**Recommended for Steve AI:** All patterns (phased implementation)
