# Architecture Diagrams - Steve AI Dissertation

**Dissertation Supplemental Visualizations**
**Date:** 2026-02-28
**Project:** Steve AI - "Cursor for Minecraft"
**Purpose:** Clear text-based diagrams for architecture comparison and explanation

---

## Table of Contents

1. [Traditional LLM Agent Architecture (ReAct)](#1-traditional-llm-agent-architecture-react)
2. [Steve AI "One Abstraction Away" Architecture](#2-steve-ai-one-abstraction-away-architecture)
3. [Three-Layer Hybrid Architecture](#3-three-layer-hybrid-architecture)
4. [Cascade Router Flow](#4-cascade-router-flow)
5. [Plugin System Architecture](#5-plugin-system-architecture)
6. [Event Bus and Interceptor Chain](#6-event-bus-and-interceptor-chain)
7. [Multi-Agent Coordination Architecture](#7-multi-agent-coordination-architecture)
8. [State Machine Architecture](#8-state-machine-architecture)
9. [Performance Comparison Visualizations](#9-performance-comparison-visualizations)
10. [Architecture Decision Flowchart](#10-architecture-decision-flowchart)
11. [Contract Net Protocol](#11-contract-net-protocol)
12. [Script Generation and Refinement](#12-script-generation-and-refinement)
13. [Memory System Architecture](#13-memory-system-architecture)

---

## 1. Traditional LLM Agent Architecture (ReAct)

### 1.1 ReAct Loop Pattern

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           USER QUERY                                         │
│                      "Build a Minecraft house"                               │
└─────────────────────────────────────┬────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                        REACT LOOP                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  THOUGHT 1: "User wants a house. I should check inventory."        │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  ACTION 1: check_inventory()                                       │    │
│  │  → LLM Call #1                                                      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  OBSERVATION 1: "Inventory: Wood x64, Stone x32"                   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  THOUGHT 2: "Good materials. Now find a building spot."            │    │
│  │  → LLM Call #2                                                      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  ACTION 2: look_around()                                           │    │
│  │  → LLM Call #3                                                      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  OBSERVATION 2: "Found flat area at (100, 64, 100)"                │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  THOUGHT 3: "Perfect spot. Start building walls."                  │    │
│  │  → LLM Call #4                                                      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  ACTION 3: place_block("oak_planks", x=100, y=64, z=100)          │    │
│  │  → LLM Call #5                                                      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  OBSERVATION 3: "Block placed successfully"                        │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼ (Repeats for each block)              │
│                              [NEXT THOUGHT-ACTION CYCLE]                    │
│                              → LLM Call #N...                               │
└─────────────────────────────────────┬────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                      ENVIRONMENT (Minecraft Server)                         │
│                    Executes actions, returns observations                    │
└──────────────────────────────────────────────────────────────────────────────┘

PERFORMANCE METRICS:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  LLM Calls:     N calls for N actions (100 calls for 100 blocks)
  Total Latency: ~1-10 seconds per action = 100-1000 seconds for full house
  Cost:          ~$0.01-0.10 per LLM call = $1-10 for full house
  Determinism:   LOW (probabilistic, may vary each run)
  Cache Support: POOR (each action requires LLM reasoning)
```

### 1.2 ReAct Flow Diagram

```
                      ┌─────────────────┐
                      │   User Input    │
                      └────────┬────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │   Think (LLM Call)   │ ◄───────────────────┐
                    └──────────┬───────────┘                     │
                               │                                  │
                               ▼                                  │
                    ┌──────────────────────┐                     │
                    │    Act (Execute)     │                     │
                    └──────────┬───────────┘                     │
                               │                                  │
                               ▼                                  │
                    ┌──────────────────────┐                     │
                    │  Observe (Result)    │ ────────────────────┘
                    └──────────┬───────────┘
                               │
                               ▼
                         ┌──────────┐
                         │ Complete?│
                         └───┬───┬──┘
                             │   │
                    No       │   │ Yes
                             │   │
                             ▼   ▼
                    ┌─────────────────┐
                    │   Return Result │
                    └─────────────────┘

Issue: Every cycle requires an LLM call → Slow, expensive, non-deterministic
```

---

## 2. Steve AI "One Abstraction Away" Architecture

### 2.1 Full System Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           USER QUERY                                         │
│                      "Build a Minecraft house"                               │
└─────────────────────────────────────┬────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                      LLM PLANNING LAYER (One-Shot)                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  CASCADE ROUTER: Analyzes task complexity                           │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Input: "Build a Minecraft house"                            │   │    │
│  │  │ Analysis:                                                   │   │    │
│  │  │   - Single task? NO                                         │   │    │
│  │  │   - Well-known pattern? YES (60-80% cache hit rate)        │   │    │
│  │  │   - Requires coordination? NO                               │   │    │
│  │  │   - Novel approach? NO                                      │   │    │
│  │  │ Output: TaskComplexity.MODERATE                             │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  TIER SELECTION: BALANCED (Groq llama-3.3-70b)                     │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ BALANCED Tier Options:                                      │   │    │
│  │  │   • Groq llama-3.3-70b-versatile (fast, good quality)      │   │    │
│  │  │   • GPT-3.5-turbo (slower, good quality)                   │   │    │
│  │  │                                                             │   │    │
│  │  │ Selected: Groq llama-3.3-70b (cost: ~$0.0001/1K tokens)     │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  SINGLE LLM CALL: Generate complete task sequence                   │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Prompt: "You are Steve, an AI agent in Minecraft.           │   │    │
│  │  │         Generate a JSON plan to build a house."             │   │    │
│  │  │                                                             │   │    │
│  │  │ Available actions: mine, place, craft, navigate..."         │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  │                              │                                        │
│  │                              ▼                                        │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Response (JSON):                                            │   │    │
│  │  │ {                                                           │   │    │
│  │  │   "plan": "Build a cozy 5x5 oak house with peaked roof",    │   │    │
│  │  │   "tasks": [                                                │   │    │
│  │  │     {"action": "mine", "block": "oak_log", "count": 64},    │   │    │
│  │  │     {"action": "craft", "item": "oak_planks", "count": 192},│   │    │
│  │  │     {"action": "navigate", "x": 100, "y": 64, "z": 100},    │   │    │
│  │  │     {"action": "build", "structure": "walls", ...},         │   │    │
│  │  │     {"action": "build", "structure": "roof", ...}           │   │    │
│  │  │   ]                                                         │   │    │
│  │  │ }                                                           │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  │                              │                                        │
│  │                              ▼                                        │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ ResponseParser: Parse JSON into Task objects                │   │    │
│  │  │ Cache: Store response for future similar commands          │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────┬────────────────────────────────────────┘
                                      │
                                      ▼ (Generated tasks passed down)
┌──────────────────────────────────────────────────────────────────────────────┐
│                   SCRIPT LAYER (Traditional AI)                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  ACTION REGISTRY: Type-safe action factories                        │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Plugin System:                                             │   │    │
│  │  │ • CoreActionsPlugin (built-in actions)                     │   │    │
│  │  │   - mine → MineBlockAction                                 │   │    │
│  │  │   - place → PlaceBlockAction                               │   │    │
│  │  │   - craft → CraftItemAction                                │   │    │
│  │  │   - navigate → NavigateAction                              │   │    │
│  │  │   - build → BuildStructureAction                           │   │    │
│  │  │                                                             │   │    │
│  │  │ Registration:                                              │   │    │
│  │  │ registry.register("mine", (steve, task, ctx) ->            │   │    │
│  │  │     new MineBlockAction(steve, task));                     │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  AGENT STATE MACHINE                                               │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ States: IDLE → PLANNING → EXECUTING → WAITING → COMPLETED  │   │    │
│  │  │                                                             │   │    │
│  │  │ Current: EXECUTING                                          │   │    │
│  │  │ Tasks Remaining: 47/50                                      │   │    │
│  │  │ Current Task: build_walls                                   │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  INTERCEPTOR CHAIN (AOP Pattern)                                    │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Before executing action:                                    │   │    │
│  │  │ 1. LoggingInterceptor → Log action start                    │   │    │
│  │  │ 2. MetricsInterceptor → Record start time                   │   │    │
│  │  │ 3. EventPublishingInterceptor → Publish ActionStartEvent    │   │    │
│  │  │                                                             │   │    │
│  │  │ Execute Action                                              │   │    │
│  │  │                                                             │   │    │
│  │  │ After executing action:                                     │   │    │
│  │  │ 4. EventPublishingInterceptor → Publish ActionCompleteEvent│   │    │
│  │  │ 5. MetricsInterceptor → Record execution time              │   │    │
│  │  │ 6. LoggingInterceptor → Log action result                   │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  TICK-BASED EXECUTION (20 times per second)                         │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ while (!taskQueue.isEmpty()) {                              │   │    │
│  │  │     Task task = taskQueue.poll();                           │   │    │
│  │  │     BaseAction action = registry.create(task);              │   │    │
│  │  │                                                             │   │    │
│  │  │     // Execute action tick-by-tick                          │   │    │
│  │  │     while (!action.isComplete()) {                          │   │    │
│  │  │         action.tick();  // Deterministic, NO LLM call!      │   │    │
│  │  │     }                                                       │   │    │
│  │  │ }                                                           │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  │                                                                     │    │
│  │  Example: BuildStructureAction                                     │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ tick() {                                                    │   │    │
│  │  │     if (currentBlockIndex >= blocks.length) {               │   │    │
│  │  │         complete();                                          │   │    │
│  │  │         return;                                              │   │    │
│  │  │     }                                                       │   │    │
│  │  │                                                             │   │    │
│  │  │     BlockPos pos = blocks[currentBlockIndex];               │   │    │
│  │  │     placeBlock(pos);                                        │   │    │
│  │  │     currentBlockIndex++;                                    │   │    │
│  │  │ }                                                           │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────┬────────────────────────────────────────┘
                                      │
                                      ▼ (Executes at 60 FPS)
┌──────────────────────────────────────────────────────────────────────────────┐
│                   EXECUTION LAYER (Minecraft Server)                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Block Placement: 20 blocks/second                                   │    │
│  │  Movement: Pathfinding with collision avoidance                       │    │
│  │  Inventory: Type-safe item management                                │    │
│  │  Crafting: Recipe execution                                           │    │
│  │  All Operations: Deterministic, reproducible                          │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘

PERFORMANCE METRICS:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  LLM Calls:     1 call for entire task sequence
  Total Latency: ~1 second planning + ~5 seconds execution (100 blocks @ 20/sec)
  Cost:          ~$0.01-0.10 for entire house (70-99% cost reduction)
  Determinism:   HIGH (same plan produces same execution)
  Cache Support: EXCELLENT (60-80% hit rate for common tasks)
```

### 2.2 "One Abstraction Away" Concept

```
TRADITIONAL LLM AGENT:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  User → LLM → Action → LLM → Action → LLM → Action → ...
         └────────────────────────────────────────────────────────────────┘
         Every action requires LLM reasoning → Slow, expensive, unpredictable


STEVE AI "ONE ABSTRACTION AWAY":
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  User → LLM (Planning) → Task Sequence → Traditional AI → Execution
         └─────────────────┘                └─────────────────────┘
         One-time planning                   Deterministic execution
         (1-3 seconds)                      (Fast, predictable)

KEY INSIGHT: LLM is ONE abstraction away from execution
• LLM generates high-level plan once
• Traditional AI (deterministic) handles execution
• Result: Fast execution with intelligent planning
```

---

## 3. Three-Layer Hybrid Architecture

### 3.1 Layer Overview

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    THREE-LAYER HYBRID ARCHITECTURE                           │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  LAYER 1: DIALOGUE & UNDERSTANDING                                   │    │
│  │  ─────────────────────────────────────────────────────────────────  │    │
│  │  • Dialogue State Machine handles player input                      │    │
│  │  • Intent classification routes to appropriate handler               │    │
│  │  • Conversation memory for context tracking                          │    │
│  │  • LLM used for complex/ambiguous commands                           │    │
│  │                                                                       │    │
│  │  Input: "Build a house"                                              │    │
│  │  Output: Parsed intent + context                                     │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  LAYER 2: PLANNING & DECOMPOSITION                                   │    │
│  │  ─────────────────────────────────────────────────────────────────  │    │
│  │  • Cascade Router analyzes task complexity                           │    │
│  │  • Tier Selection chooses appropriate LLM/model                      │    │
│  │  • HTN for common patterns (build, mine, craft)                      │    │
│  │  • Behavior Tree for reactive task selection                         │    │
│  │  • LLM fallback for novel tasks                                      │    │
│  │  • Skill library for caching successful patterns                     │    │
│  │                                                                       │    │
│  │  Input: Intent + context                                             │    │
│  │  Output: Structured task sequence                                    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  LAYER 3: EXECUTION & COORDINATION                                  │    │
│  │  ─────────────────────────────────────────────────────────────────  │    │
│  │  • Action Registry creates actions from tasks                        │    │
│  │  • State Machine tracks agent state                                  │    │
│  │  • Interceptor Chain provides AOP capabilities                       │    │
│  │  • Tick-based execution (20/sec) for non-blocking                    │    │
│  │  • Event Bus for loose coupling                                      │    │
│  │  • Utility AI for multi-agent worker assignment                      │    │
│  │                                                                       │    │
│  │  Input: Task sequence                                                │    │
│  │  Output: Executed actions in Minecraft world                         │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Data Flow Between Layers

```
DIALOGUE LAYER → PLANNING LAYER → EXECUTION LAYER
       │                   │                    │
       ▼                   ▼                    ▼
  ┌─────────┐        ┌─────────┐         ┌─────────┐
  │ Natural │        │  Tasks  │         │ Actions │
  │Language │   →    │Sequence │    →    │Execute  │
  │         │        │         │         │         │
  └─────────┘        └─────────┘         └─────────┘
       │                   │                    │
       ▼                   ▼                    ▼
  ┌─────────┐        ┌─────────┐         ┌─────────┐
  │ Intent  │        │  JSON   │         │ Blocks  │
  │Context  │        │  Plan   │         │Placed   │
  └─────────┘        └─────────┘         └─────────┘

EXAMPLE FLOW:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  User: "Build a house"
       │
       ▼
  Layer 1: Parse intent (build_structure)
       │
       ▼
  Layer 2: Generate task sequence
       │
       │    [
       │      {"action": "mine", "block": "oak_log", "count": 64},
       │      {"action": "craft", "item": "oak_planks", "count": 192},
       │      {"action": "build", "structure": "house", ...}
       │    ]
       │
       ▼
  Layer 3: Execute actions tick-by-tick
       │
       ▼
  Result: House built in Minecraft world
```

---

## 4. Cascade Router Flow

### 4.1 Complete Cascade Flow

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         USER COMMAND                                        │
│                      "Build a Minecraft house"                               │
└─────────────────────────────────────┬────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                      COMPLEXITY ANALYZER                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  analyzeComplexity("Build a Minecraft house")                       │    │
│  │                                                                     │    │
│  │  Evaluation Metrics:                                                │    │
│  │  • Single atomic task?          false  (multiple steps)            │    │
│  │  • Well-known pattern?          true   (60-80% cache hit rate)     │    │
│  │  • Requires multi-agent?         false  (single agent sufficient)   │    │
│  │  • Novel approach?              false  (standard building pattern)  │    │
│  │  • Time-critical?               false  (no rush)                   │    │
│  │  • High precision required?     false  (building tolerates minor   │    │
│  │                                       variations)                  │    │
│  │                                                                     │    │
│  │  Complexity Score: 3.5/10 → TaskComplexity.MODERATE                │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────┬────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                      CACHE CHECK                                            │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Cache Query: Semantic similarity search                            │    │
│  │  Key: "build house" + context                                       │    │
│  │                                                                     │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Cache Contents:                                             │   │    │
│  │  │ • "build simple house"     → 0.92 similarity (HIT!)         │   │    │
│  │  │ • "build wooden house"     → 0.89 similarity                 │   │    │
│  │  │ • "construct shelter"      → 0.76 similarity                 │   │    │
│  │  │ • "build castle"           → 0.65 similarity                 │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  │                                                                     │    │
│  │  Result: CACHE HIT (0.92 > threshold)                              │    │
│  │  Cached tasks: 47 pre-planned tasks                                │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  CACHE HIT PATH: Skip LLM, use cached plan                          │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Retrieved Plan:                                             │   │    │
│  │  │ [                                                            │   │    │
│  │  │   {"action": "navigate", "x": 100, "y": 64, "z": 100},      │   │    │
│  │  │   {"action": "place_blocks", "structure": "walls"},         │   │    │
│  │  │   {"action": "place_blocks", "structure": "roof"},          │   │    │
│  │  │   {"action": "place_block", "block": "oak_door", ...},       │   │    │
│  │  │   ...                                                         │   │    │
│  │  │ ]                                                            │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  │                                                                     │    │
│  │  Benefits:                                                         │    │
│  │  • 0 LLM calls (100% cost savings)                                 │    │
│  │  • ~0ms latency (instant response)                                 │    │
│  │  • Deterministic execution                                         │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────┬────────────────────────────────────────┘
                                      │ (If cache miss)
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                      TIER SELECTION                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  TaskComplexity.MODERATE → LLMTier.BALANCED                        │    │
│  │                                                                     │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ BALANCED Tier Options:                                      │   │    │
│  │  │                                                             │   │    │
│  │  │ Option A: Groq llama-3.3-70b-versatile                      │   │    │
│  │  │   Speed:     ★★★★★ (1-3s latency)                          │   │    │
│  │  │   Quality:   ★★★★☆ (good reasoning)                        │   │    │
│  │  │   Cost:      ★★★★★ ($0.0001/1K tokens)                     │   │    │
│  │  │   Selected: YES (best value)                                │   │    │
│  │  │                                                             │   │    │
│  │  │ Option B: GPT-3.5-turbo                                     │   │    │
│  │  │   Speed:     ★★★☆☆ (3-5s latency)                          │   │    │
│  │  │   Quality:   ★★★★☆ (good reasoning)                        │   │    │
│  │  │   Cost:      ★★★☆☆ ($0.001/1K tokens)                      │   │    │
│  │  │   Selected: NO (slower, more expensive)                     │   │    │
│  │  │                                                             │   │    │
│  │  │ Option C: GPT-4o                                           │   │    │
│  │  │   Speed:     ★★☆☆☆ (5-10s latency)                         │   │    │
│  │  │   Quality:   ★★★★★ (excellent reasoning)                   │   │    │
│  │  │   Cost:      ★★☆☆☆ ($0.005/1K tokens)                      │   │    │
│  │  │   Selected: NO (overkill for moderate task)                 │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────┬────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                      LLM GENERATION                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Prompt Engineering:                                                │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ System Prompt:                                              │   │    │
│  │  │ "You are Steve, an AI agent in Minecraft. Your task is to    │   │    │
│  │  │  generate structured plans for building structures."         │   │    │
│  │  │                                                             │   │    │
│  │  │ Available Actions:                                          │   │    │
│  │  │ - mine(block_type, count): Mine blocks                      │   │    │
│  │  │ - craft(item, count): Craft items                           │   │    │
│  │  │ - navigate(x, y, z): Move to position                       │   │    │
│  │  │ - place_block(block, x, y, z): Place single block           │   │    │
│  │  │ - build_structure(structure, ...): Build structure           │   │    │
│  │  │                                                             │   │    │
│  │  │ Output Format: JSON array of tasks with required fields    │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  │                              │                                        │
│  │                              ▼                                        │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ User Prompt:                                                │   │    │
│  │  │ "Build a Minecraft house at position (100, 64, 100).        │   │    │
│  │  │  Use oak wood for materials. Make it 5x5 blocks with a     │   │    │
│  │  │  peaked roof. Include a door and windows."                  │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  LLM API Call:                                                       │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ POST https://api.groq.com/openai/v1/chat/completions       │   │    │
│  │  │ {                                                           │   │    │
│  │  │   "model": "llama-3.3-70b-versatile",                       │   │    │
│  │  │   "messages": [system_prompt, user_prompt],                 │   │    │
│  │  │   "temperature": 0.7,                                       │   │    │
│  │  │   "max_tokens": 2000                                        │   │    │
│  │  │ }                                                           │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼ (~1-3 seconds)                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  LLM Response:                                                       │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ {                                                           │   │    │
│  │  │   "plan": "Build a 5x5 oak house with peaked roof at       │   │    │
│  │  │          (100, 64, 100) with door and windows",             │   │    │
│  │  │   "tasks": [                                                │   │    │
│  │  │     {"action": "mine", "block": "oak_log", "count": 64},    │   │    │
│  │  │     {"action": "craft", "item": "oak_planks", "count": 192},│   │    │
│  │  │     {"action": "navigate", "x": 100, "y": 64, "z": 100},    │   │    │
│  │  │     {"action": "build",                                     │   │    │
│  │  │      "structure": "walls",                                  │   │    │
│  │  │      "material": "oak_planks",                              │   │    │
│  │  │      "dimensions": {"width": 5, "height": 3, "depth": 5},   │   │    │
│  │  │      "pattern": "solid"},                                   │   │    │
│  │  │     {"action": "build",                                     │   │    │
│  │  │      "structure": "roof",                                   │   │    │
│  │  │      "material": "oak_planks",                              │   │    │
│  │  │      "style": "peaked"},                                    │   │    │
│  │  │     {"action": "place_block",                               │   │    │
│  │  │      "block": "oak_door",                                   │   │    │
│  │  │      "x": 100, "y": 64, "z": 102,                           │   │    │
│  │  │      "facing": "south"}                                     │   │    │
│  │  │   ]                                                         │   │    │
│  │  │ }                                                           │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  ResponseParser: Parse JSON into Task objects                        │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Parsed Tasks:                                               │   │    │
│  │  │ • Task[0]: MineBlockAction(block=oak_log, count=64)        │   │    │
│  │  │ • Task[1]: CraftItemAction(item=oak_planks, count=192)     │   │    │
│  │  │ • Task[2]: NavigateAction(x=100, y=64, z=100)              │   │    │
│  │  │ • Task[3]: BuildStructureAction(structure=walls, ...)       │   │    │
│  │  │ • Task[4]: BuildStructureAction(structure=roof, ...)        │   │    │
│  │  │ • Task[5]: PlaceBlockAction(block=oak_door, ...)            │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Cache Storage: Store response for future similar commands           │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Key: "build house oak 5x5 peaked roof"                      │   │    │
│  │  │ Value: [Task[0], Task[1], Task[2], Task[3], Task[4], Task[5]]│   │    │
│  │  │ TTL: 24 hours                                              │   │    │
│  │  │ Hit Count: 0                                                │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────┬────────────────────────────────────────┘
                                      │
                                      ▼ (Queue tasks)
┌──────────────────────────────────────────────────────────────────────────────┐
│                    ACTION EXECUTOR                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Task Queue:                                                        │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ [MineBlockAction, CraftItemAction, NavigateAction,          │   │    │
│  │  │  BuildStructureAction, BuildStructureAction,                │   │    │
│  │  │  PlaceBlockAction]                                          │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  State Machine: PLANNING → EXECUTING                                 │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ IDLE ──▶ PLANNING ──▶ EXECUTING ──▶ WAITING ──▶ COMPLETED   │   │    │
│  │  │   ▲                            │                              │   │    │
│  │  │   └────────────────────────────┘                              │   │    │
│  │  │          (on error or new task)                               │   │    │
│  │  │                                                             │   │    │
│  │  │ Current State: EXECUTING                                     │   │    │
│  │  │ Tasks Complete: 2/6                                          │   │    │
│  │  │ Current Action: BuildStructureAction (walls)                 │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Tick Loop (20 times/second):                                        │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ while (!taskQueue.isEmpty()) {                              │   │    │
│  │  │     Task task = taskQueue.poll();                           │   │    │
│  │  │     BaseAction action = registry.create(task);              │   │    │
│  │  │                                                             │   │    │
│  │  │     while (!action.isComplete()) {                          │   │    │
│  │  │         action.tick();  // NO LLM CALL!                     │   │    │
│  │  │         Thread.sleep(50);  // 20 ticks/second               │   │    │
│  │  │     }                                                       │   │    │
│  │  │ }                                                           │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  State Machine: EXECUTING → COMPLETED                               │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘

COST SAVINGS:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Traditional ReAct: 100 LLM calls × $0.01 = $1.00
  Steve AI Cascade: 1 LLM call × $0.01 = $0.01 (99% savings)
  Cache Hit (Repeat): 0 LLM calls = $0.00 (100% savings)

  Time Comparison:
  Traditional: 100s (planning) + 5s (execution) = 105s total
  Steve AI:    1s (planning) + 5s (execution) = 6s total
  Cached:      0s (planning) + 5s (execution) = 5s total

  Speedup: 17-21x faster
```

### 4.2 Complexity to Tier Mapping

```
CASCADE ROUTER COMPLEXITY MATRIX:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌──────────────────────────────────────────────────────────────────────────────┐
│                     TaskComplexity.TRIVIAL (0-2/10)                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Examples:                                                          │    │
│  │  • "Place a block"                                                  │    │
│  │  • "Move to position"                                               │    │
│  │  • "Check inventory"                                                │    │
│  │                                                                     │    │
│  │  Tier Selection: LLMTier.FAST                                      │    │
│  │  Model: Small local model or rule-based (no LLM)                   │    │
│  │  Latency: <100ms                                                    │    │
│  │  Cost: ~$0.00001                                                    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                     TaskComplexity.SIMPLE (2-4/10)                           │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Examples:                                                          │    │
│  │  • "Mine 10 blocks"                                                 │    │
│  │  • "Craft a wooden sword"                                           │    │
│  │  • "Follow me"                                                      │    │
│  │                                                                     │    │
│  │  Tier Selection: LLMTier.FAST                                      │    │
│  │  Model: GPT-3.5-turbo or Groq llama-3.1-8b                        │    │
│  │  Latency: 500ms-1s                                                  │    │
│  │  Cost: ~$0.0001                                                    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                     TaskComplexity.MODERATE (4-6/10)                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Examples:                                                          │    │
│  │  • "Build a house"                                                  │    │
│  │  • "Explore the cave"                                               │    │
│  │  • "Gather resources for crafting"                                  │    │
│  │                                                                     │    │
│  │  Tier Selection: LLMTier.BALANCED                                  │    │
│  │  Model: Groq llama-3.3-70b or GPT-3.5-turbo                        │    │
│  │  Latency: 1-3s                                                      │    │
│  │  Cost: ~$0.001                                                      │    │
│  │  Cache Hit Rate: 60-80%                                             │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                     TaskComplexity.COMPLEX (6-8/10)                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Examples:                                                          │    │
│  │  • "Build a complex redstone machine"                               │    │
│  │  • "Coordinate multi-agent construction"                            │    │
│  │  • "Defend against hostile mobs while building"                     │    │
│  │                                                                     │    │
│  │  Tier Selection: LLMTier.PREMIUM                                   │    │
│  │  Model: GPT-4o or Claude 3.5 Sonnet                                │    │
│  │  Latency: 3-10s                                                     │    │
│  │  Cost: ~$0.01                                                       │    │
│  │  Cache Hit Rate: 30-50%                                             │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                     TaskComplexity.NOVEL (8-10/10)                           │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Examples:                                                          │    │
│  │  • "Invent a new farming technique"                                 │    │
│  │  • "Create an automated sorting system"                             │    │
│  │  • "Design a unique architectural style"                            │    │
│  │                                                                     │    │
│  │  Tier Selection: LLMTier.PREMIUM                                   │    │
│  │  Model: GPT-4o or Claude Opus with chain-of-thought                │    │
│  │  Latency: 10-30s                                                    │    │
│  │  Cost: ~$0.10                                                       │    │
│  │  Cache Hit Rate: 0-20% (novel tasks)                                │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Plugin System Architecture

### 5.1 Plugin System Overview

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      PLUGIN SYSTEM ARCHITECTURE                              │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  PLUGIN MANAGER (Core)                                              │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Responsibilities:                                            │   │    │
│  │  │ • Discover plugins via SPI (Service Provider Interface)      │   │    │
│  │  │ • Load plugins at startup                                    │   │    │
│  │  │ • Initialize plugin dependencies (DI container)              │   │    │
│  │  │ • Manage plugin lifecycle (init, start, stop, shutdown)      │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  ACTION REGISTRY (Core)                                            │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Map<String, ActionFactory> actionFactories;                 │   │    │
│  │  │                                                             │   │    │
│  │  │ register(String actionId, ActionFactory factory) {         │   │    │
│  │  │     actionFactories.put(actionId, factory);                │   │    │
│  │  │ }                                                           │   │    │
│  │  │                                                             │   │    │
│  │  │ create(SteveEntity steve, Task task, ActionContext ctx) {  │   │    │
│  │  │     ActionFactory factory = actionFactories.get(           │   │    │
│  │  │         task.getActionId());                                │   │    │
│  │  │     return factory.create(steve, task, ctx);                │   │    │
│  │  │ }                                                           │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  LOADED PLUGINS                                                     │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ CoreActionsPlugin (built-in)                                │   │    │
│  │  │ ┌─────────────────────────────────────────────────────┐     │   │    │
│  │  │ │ Registers:                                          │     │   │    │
│  │  │ │ • mine     → MineBlockAction                       │     │   │    │
│  │  │ │ • place    → PlaceBlockAction                      │     │   │    │
│  │  │ │ • craft    → CraftItemAction                       │     │   │    │
│  │  │ │ • navigate → NavigateAction                        │     │   │    │
│  │  │ │ • build    → BuildStructureAction                  │     │   │    │
│  │  │ │ • attack   → AttackAction                          │     │   │    │
│  │  │ │ • follow   → FollowAction                          │     │   │    │
│  │  │ │ • wait     → WaitAction                            │     │   │    │
│  │  │ └─────────────────────────────────────────────────────┘     │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  │                                                                      │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ RedstoneActionsPlugin (extensibility example)              │   │    │
│  │  │ ┌─────────────────────────────────────────────────────┐     │   │    │
│  │  │ │ Registers:                                          │     │   │    │
│  │  │ │ • place_redstone → PlaceRedstoneAction              │     │   │    │
│  │  │ │ • connect_wire  → ConnectRedstoneWireAction         │     │   │    │
│  │  │ │ • build_circuit  → BuildRedstoneCircuitAction       │     │   │    │
│  │  │ └─────────────────────────────────────────────────────┘     │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  │                                                                      │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ FarmingActionsPlugin (extensibility example)               │   │    │
│  │  │ ┌─────────────────────────────────────────────────────┐     │   │    │
│  │  │ │ Registers:                                          │     │   │    │
│  │  │ │ • plant_crop   → PlantCropAction                    │     │   │    │
│  │  │ │ • harvest      → HarvestCropAction                  │     │   │    │
│  │  │ │ • water_crop   → WaterCropAction                    │     │   │    │
│  │  │ └─────────────────────────────────────────────────────┘     │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Plugin Registration Flow

```
                    ┌─────────────────┐
                    │  Application    │
                    │    Startup      │
                    └────────┬────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │   Plugin Manager Init        │
              │   • Scan classpath           │
              │   • Load SPI providers       │
              └────────┬─────────────────────┘
                       │
                       ▼
              ┌──────────────────────────────┐
              │   For Each Plugin:           │
              │   • Create instance          │
              │   • Inject dependencies      │
              │   • Call plugin.init()       │
              └────────┬─────────────────────┘
                       │
                       ▼
              ┌──────────────────────────────┐
              │   Call plugin.register()     │
              │   to register actions        │
              └────────┬─────────────────────┘
                       │
                       ▼
      ┌──────────────────────────────────────────┐
      │  CoreActionsPlugin.register(registry)    │
      │  {                                       │
      │      registry.register("mine",           │
      │          (steve, task, ctx) ->           │
      │              new MineBlockAction(        │
      │                  steve, task));          │
      │                                          │
      │      registry.register("place",          │
      │          (steve, task, ctx) ->           │
      │              new PlaceBlockAction(       │
      │                  steve, task));          │
      │                                          │
      │      // ... more registrations          │
      │  }                                       │
      └───────────────────┬──────────────────────┘
                          │
                          ▼
              ┌──────────────────────────────┐
              │   Actions Registered in      │
              │   Action Registry            │
              │   • mine → MineBlockAction    │
              │   • place → PlaceBlockAction │
              └────────┬─────────────────────┘
                       │
                       ▼
              ┌──────────────────────────────┐
              │   Plugin Ready               │
              │   System operational         │
              └──────────────────────────────┘
```

### 5.3 Action Factory Pattern

```
ACTION FACTORY PATTERN:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  ┌──────────────────────────────────────────────────────────────────────┐
  │  ActionFactory Interface                                            │
  │  ┌────────────────────────────────────────────────────────────────┐ │
  │  │ @FunctionalInterface                                            │ │
  │  │ public interface ActionFactory {                                │ │
  │  │     BaseAction create(SteveEntity steve,                       │ │
  │  │                        Task task,                              │ │
  │  │                        ActionContext context);                 │ │
  │  │ }                                                               │ │
  │  └────────────────────────────────────────────────────────────────┘ │
  └──────────────────────────────────────────────────────────────────────┘

                              │
                              ▼ Used by

  ┌──────────────────────────────────────────────────────────────────────┐
  │  Action Registry                                                     │
  │  ┌────────────────────────────────────────────────────────────────┐ │
  │  │ public BaseAction createAction(SteveEntity steve,              │ │
  │  │                                 Task task,                     │ │
  │  │                                 ActionContext ctx) {           │ │
  │  │     ActionFactory factory = actionFactories.get(               │ │
  │  │         task.getActionId());                                   │ │
  │  │     if (factory == null) {                                     │ │
  │  │         throw new UnknownActionException(                      │ │
  │  │             task.getActionId());                               │ │
  │  │     }                                                          │ │ │
  │  │     return factory.create(steve, task, ctx);                   │ │
  │  │ }                                                               │ │
  │  └────────────────────────────────────────────────────────────────┘ │
  └──────────────────────────────────────────────────────────────────────┘

                              │
                              ▼ Creates

  ┌──────────────────────────────────────────────────────────────────────┐
  │  BaseAction (Abstract)                                               │
  │  ┌────────────────────────────────────────────────────────────────┐ │
  │  │ public abstract class BaseAction {                              │ │
  │  │     protected final SteveEntity steve;                          │ │
  │  │     protected final Task task;                                  │ │
  │  │     protected final ActionContext context;                      │ │
  │  │                                                                  │ │
  │  │     // Called every tick (20 times per second)                  │ │
  │  │     public abstract void tick();                                │ │
  │  │                                                                  │ │
  │  │     // Check if action is complete                              │ │
  │  │     public abstract boolean isComplete();                       │ │
  │  │                                                                  │ │
  │  │     // Cleanup when action is cancelled                         │ │
  │  │     public void onCancel() {}                                   │ │
  │  │ }                                                                │ │
  │  └────────────────────────────────────────────────────────────────┘ │
  └──────────────────────────────────────────────────────────────────────┘

                              │
                              ▼ Example Implementation

  ┌──────────────────────────────────────────────────────────────────────┐
  │  MineBlockAction (Concrete)                                          │
  │  ┌────────────────────────────────────────────────────────────────┐ │
  │  │ public class MineBlockAction extends BaseAction {               │ │
  │  │     private final String blockType;                             │ │
  │  │     private final int count;                                    │ │
  │  │     private int mined = 0;                                      │ │
  │  │                                                                  │ │
  │  │     public MineBlockAction(SteveEntity steve, Task task) {      │ │
  │  │         super(steve, task);                                     │ │
  │  │         this.blockType = task.getParam("block");                │ │
  │  │         this.count = task.getParam("count", int.class);         │ │
  │  │     }                                                            │ │
  │  │                                                                  │ │
  │  │     @Override                                                   │ │
  │  │     public void tick() {                                        │ │
  │  │         if (mined >= count) return;  // Complete                 │ │
  │  │                                                                  │ │
  │  │         BlockPos target = findNearestBlock(blockType);          │ │
  │  │         if (target == null) return;  // No blocks found          │ │
  │  │                                                                  │ │
  │  │         if (!steve.isInRange(target)) {                         │ │
  │  │             steve.moveTo(target);  // Pathfind                   │ │
  │  │             return;                                              │ │
  │  │         }                                                        │ │
  │  │                                                                  │ │
  │  │         steve.mineBlock(target);  // Break block                 │ │
  │  │         mined++;                                                 │ │
  │  │     }                                                            │ │
  │  │                                                                  │ │
  │  │     @Override                                                   │ │
  │  │     public boolean isComplete() {                                │ │
  │  │         return mined >= count;                                  │ │
  │  │     }                                                            │ │
  │  │ }                                                                │ │
  │  └────────────────────────────────────────────────────────────────┘ │
  └──────────────────────────────────────────────────────────────────────┘

BENEFITS:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  • Loose coupling: Actions registered by name, not class
  • Extensibility: Add new actions via plugins without modifying core
  • Type safety: Factory ensures correct action instantiation
  • Testability: Mock factories for unit testing
  • Dependency injection: Context provides dependencies to actions
```

---

## 6. Event Bus and Interceptor Chain

### 6.1 Event Bus Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      EVENT BUS ARCHITECTURE                                  │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  EVENT PUBLISHERS                                                    │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ • ActionExecutor publishes:                                 │   │    │
│  │  │   - ActionStartEvent(action, timestamp)                     │   │    │
│  │  │   - ActionCompleteEvent(action, duration, success)           │   │    │
│  │  │   - ActionErrorEvent(action, error)                         │   │    │
│  │  │                                                             │   │    │
│  │  │ • StateMachine publishes:                                   │   │    │
│  │  │   - StateChangeEvent(from, to, timestamp)                   │   │    │
│  │  │                                                             │   │    │
│  │  │ • TaskPlanner publishes:                                    │   │    │
│  │  │   - PlanningStartEvent(command)                            │   │    │
│  │  │   - PlanningCompleteEvent(tasks, duration)                  │   │    │
│  │  │                                                             │   │    │
│  │  │ • CollaborativeBuildManager publishes:                     │   │    │
│  │  │   - TaskClaimedEvent(worker, task)                         │   │    │
│  │  │   - TaskCompletedEvent(worker, task)                       │   │    │
│  │  │   - SectionClaimedEvent(worker, section)                   │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼ publish(event)                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  EVENT BUS                                                          │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ public class EventBus {                                     │   │    │
│  │  │     private Map<Class<?>, List<Consumer<?>>> subscribers;   │   │    │
│  │  │                                                             │   │    │
│  │  │     public <T> void publish(T event) {                      │   │    │
│  │  │         List<Consumer<?>> handlers =                        │   │    │
│  │  │             subscribers.get(event.getClass());              │   │    │
│  │  │         if (handlers != null) {                             │   │    │
│  │  │             for (Consumer<?> handler : handlers) {          │   │    │
│  │  │                 ((Consumer<T>) handler).accept(event);      │   │    │
│  │  │             }                                               │   │    │
│  │  │         }                                                   │   │    │
│  │  │     }                                                       │   │    │
│  │  │                                                             │   │    │
│  │  │     public <T> void subscribe(Class<T> eventType,           │   │    │
│  │  │                             Consumer<T> handler) {          │   │    │
│  │  │         subscribers.computeIfAbsent(eventType,             │   │    │
│  │  │             k -> new ArrayList<>()).add(handler);           │   │    │
│  │  │     }                                                       │   │    │
│  │  │ }                                                           │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼ dispatch to subscribers               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  EVENT SUBSCRIBERS                                                  │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ MetricsCollector (subscribes to all events):                 │   │    │
│  │  │ • ActionStartEvent → Record start time                       │   │    │
│  │  │ • ActionCompleteEvent → Calculate duration, update metrics   │   │    │
│  │  │ • StateChangeEvent → Track state transitions                 │   │    │
│  │  │                                                             │   │    │
│  │  │ Logger (subscribes to all events):                           │   │    │
│  │  │ • All events → Log at appropriate level                      │   │    │
│  │  │                                                             │   │    │
│  │  │ CollaborativeBuildManager (specific events):                │   │    │
│  │  │ • TaskClaimedEvent → Update worker assignments              │   │    │
│  │  │ • SectionClaimedEvent → Update spatial partition            │   │    │
│  │  │                                                             │   │    │
│  │  │ DebugOverlay (UI events):                                   │   │    │
│  │  │ • ActionStartEvent → Show on-screen indicator                │   │    │
│  │  │ • StateChangeEvent → Update state display                    │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Interceptor Chain Pattern

```
INTERCEPTOR CHAIN (AOP PATTERN):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌──────────────────────────────────────────────────────────────────────────────┐
│  ACTION EXECUTION WITH INTERCEPTORS                                          │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Client Code                                                         │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ ActionExecutor executor = new ActionExecutor();              │   │    │
│  │  │ executor.setInterceptors(Arrays.asList(                     │   │    │
│  │  │     new LoggingInterceptor(),                                │   │    │
│  │  │     new MetricsInterceptor(),                                │   │    │
│  │  │     new EventPublishingInterceptor(eventBus)                 │   │    │
│  │  │ ));                                                          │   │    │
│  │  │ executor.execute(action);  // Interceptors called here      │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  INTERCEPTOR 1: LoggingInterceptor                                 │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ beforeExecute(action) {                                     │   │    │
│  │  │     logger.info("Starting action: " + action.getName());    │   │    │
│  │  │ }                                                           │   │    │
│  │  │                                                             │   │    │
│  │  │ afterExecute(action, result) {                              │   │    │
│  │  │     logger.info("Completed action: " + action.getName() +  │   │    │
│  │  │         ", success=" + result.isSuccess());                 │   │    │
│  │  │ }                                                           │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  INTERCEPTOR 2: MetricsInterceptor                                  │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ private ThreadLocal<Long> startTime = new ThreadLocal<>();   │   │    │
│  │  │                                                             │   │    │
│  │  │ beforeExecute(action) {                                     │   │    │
│  │  │     startTime.set(System.nanoTime());                      │   │    │
│  │  │ }                                                           │   │    │
│  │  │                                                             │   │    │
│  │  │ afterExecute(action, result) {                              │   │    │
│  │  │     long duration = System.nanoTime() - startTime.get();   │   │    │
│  │  │     metrics.record(action.getName(), duration);             │   │    │
│  │  │ }                                                           │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  INTERCEPTOR 3: EventPublishingInterceptor                           │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ beforeExecute(action) {                                     │   │    │
│  │  │     eventBus.publish(new ActionStartEvent(action));        │   │    │
│  │  │ }                                                           │   │    │
│  │  │                                                             │   │    │
│  │  │ afterExecute(action, result) {                              │   │    │
│  │  │     eventBus.publish(new ActionCompleteEvent(              │   │    │
│  │  │         action, result.isSuccess()));                      │   │    │
│  │  │ }                                                           │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  ACTUAL ACTION EXECUTION                                            │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ action.tick()                                               │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼ (interceptors unwind in reverse)      │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  INTERCEPTOR 3: EventPublishingInterceptor (after)                    │    │
│  │  → Publish ActionCompleteEvent                                         │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  INTERCEPTOR 2: MetricsInterceptor (after)                            │    │
│  │  → Record execution time                                               │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  INTERCEPTOR 1: LoggingInterceptor (after)                             │    │
│  │  → Log completion                                                      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘

EXECUTION FLOW:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  1. LoggingInterceptor.beforeExecute()
  2. MetricsInterceptor.beforeExecute()
  3. EventPublishingInterceptor.beforeExecute()
  4. action.tick()  ← Actual action execution
  5. EventPublishingInterceptor.afterExecute()
  6. MetricsInterceptor.afterExecute()
  7. LoggingInterceptor.afterExecute()

BENEFITS:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  • Separation of concerns: Logging, metrics, events in separate interceptors
  • Reusability: Interceptors can be mixed and matched
  • Testability: Mock interceptors for testing
  • Flexibility: Add/remove interceptors without changing actions
  • Aspect-Oriented Programming: Cross-cutting concerns handled centrally
```

### 6.3 Event Flow Diagram

```
                           ┌─────────────────┐
                           │  Action Start   │
                           └────────┬────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Event Bus                                         │
│                                                                             │
│     ┌──────────────┐    ┌──────────────┐    ┌──────────────┐               │
│     │  Subscriber  │    │  Subscriber  │    │  Subscriber  │               │
│     │     #1       │    │     #2       │    │     #3       │               │
│     │  (Metrics)   │    │   (Logger)   │    │  (Overlay)   │               │
│     └──────┬───────┘    └──────┬───────┘    └──────┬───────┘               │
│            │                    │                    │                      │
│            ▼                    ▼                    ▼                      │
│     ┌──────────┐         ┌──────────┐         ┌──────────┐                   │
│     │ Record   │         │ Log      │         │ Update   │                   │
│     │ start    │         │ info     │         │ UI       │                   │
│     │ time     │         │ message  │         │          │                   │
│     └──────────┘         └──────────┘         └──────────┘                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                           ┌─────────────────┐
                           │ Execute Action  │
                           │   action.tick() │
                           └────────┬────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Event Bus                                         │
│                                                                             │
│     ┌──────────────┐    ┌──────────────┐    ┌──────────────┐               │
│     │  Subscriber  │    │  Subscriber  │    │  Subscriber  │               │
│     │     #1       │    │     #2       │    │     #3       │               │
│     │  (Metrics)   │    │   (Logger)   │    │  (Overlay)   │               │
│     └──────┬───────┘    └──────┬───────┘    └──────┬───────┘               │
│            │                    │                    │                      │
│            ▼                    ▼                    ▼                      │
│     ┌──────────┐         ┌──────────┐         ┌──────────┐                   │
│     │ Calculate│         │ Log      │         │ Update   │                   │
│     │ duration │         │ result   │         │ UI       │                   │
│     │          │         │          │         │          │                   │
│     └──────────┘         └──────────┘         └──────────┘                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                           ┌─────────────────┐
                           │  Action Complete│
                           └─────────────────┘

KEY FEATURES:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  • Asynchronous: Subscribers notified without blocking
  • Decoupled: Publishers don't know about subscribers
  • Extensible: Add subscribers without changing publishers
  • Type-safe: Generic event types prevent errors
```

---

## 7. Multi-Agent Coordination Architecture

### 7.1 Collaborative Building System

```
┌──────────────────────────────────────────────────────────────────────────────┐
│              COLLABORATIVE BUILDING ARCHITECTURE                              │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  FOREMAN (Coordinator)                                              │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Responsibilities:                                            │   │    │
│  │  │ • Receive build request from player                         │   │    │
│  │  │ • Analyze structure requirements                            │   │    │
│  │  │ • Partition structure into sections                         │   │    │
│  │  │ • Assign sections to available workers                      │   │    │
│  │  │ • Monitor progress and rebalance if needed                 │   │    │
│  │  │ • Report completion to player                               │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼ partitions                            │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  SPATIAL PARTITIONING                                               │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Structure: 10x10x10 House                                      │   │    │
│  │  │                                                             │   │    │
│  │  │   ┌─────┬─────┬─────┬─────┬─────┐                           │   │    │
│  │  │   │  1  │  2  │  3  │  4  │  5  │  Front View (5x5)        │   │    │
│  │  │   ├─────┼─────┼─────┼─────┼─────┤                           │   │    │
│  │  │   │  6  │  7  │  8  │  9  │ 10  │                           │   │    │
│  │  │   └─────┴─────┴─────┴─────┴─────┘                           │   │    │
│  │  │                                                             │   │    │
│  │  │ Partitioning:                                               │   │    │
│  │  │ • Section 1: (0,0,0) to (4,9,4)    → 5x10x5 = 250 blocks    │   │    │
│  │  │ • Section 2: (5,0,0) to (9,9,4)    → 5x10x5 = 250 blocks    │   │    │
│  │  │ • Section 3: (0,0,5) to (4,9,9)    → 5x10x5 = 250 blocks    │   │    │
│  │  │ • Section 4: (5,0,5) to (9,9,9)    → 5x10x5 = 250 blocks    │   │    │
│  │  │                                                             │   │    │
│  │  │ Total: 4 sections, 1000 blocks                             │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼ assigns                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  UTILITY AI WORKER ASSIGNMENT                                        │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Available Workers:                                           │   │    │
│  │  │ • Worker1: Idle, at (0, 64, 0)                               │   │    │
│  │  │ • Worker2: Idle, at (5, 64, 0)                               │   │    │
│  │  │ • Worker3: Idle, at (0, 64, 5)                               │   │    │
│  │  │ • Worker4: Busy, will be available in 5s                     │   │    │
│  │  │                                                             │   │    │
│  │  │ Scoring for Section 1 (0,0,0) to (4,9,4):                   │   │    │
│  │  │ • Worker1: distance=0, idle=1.0  → score=1.0 (ASSIGN)       │   │    │
│  │  │ • Worker2: distance=5, idle=1.0  → score=0.5                │   │    │
│  │  │ • Worker3: distance=5, idle=1.0  → score=0.5                │   │    │
│  │  │ • Worker4: distance=7, idle=0.0  → score=0.0                │   │    │
│  │  │                                                             │   │    │
│  │  │ Assignment:                                                 │   │    │
│  │  │ • Section 1 → Worker1 (250 blocks)                          │   │    │
│  │  │ • Section 2 → Worker2 (250 blocks)                          │   │    │
│  │  │ • Section 3 → Worker3 (250 blocks)                          │   │    │
│  │  │ • Section 4 → Worker4 (when available, 250 blocks)          │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼ coordinates                           │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  WORKERS (Execution)                                                │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Worker1: Building Section 1                                  │   │    │
│  │  │ • State: EXECUTING                                          │   │    │
│  │  │ • Progress: 47/250 blocks (19%)                              │   │    │
│  │  │ • Rate: 20 blocks/second                                    │   │    │
│  │  │ • ETA: 10 seconds                                           │   │    │
│  │  │                                                             │   │    │
│  │  │ Worker2: Building Section 2                                  │   │    │
│  │  │ • State: EXECUTING                                          │   │    │
│  │  │ • Progress: 52/250 blocks (21%)                              │   │    │
│  │  │ • Rate: 20 blocks/second                                    │   │    │
│  │  │ • ETA: 10 seconds                                           │   │    │
│  │  │                                                             │   │    │
│  │  │ Worker3: Building Section 3                                  │   │    │
│  │  │ • State: EXECUTING                                          │   │    │
│  │  │ • Progress: 48/250 blocks (19%)                              │   │    │
│  │  │ • Rate: 20 blocks/second                                    │   │    │
│  │  │ • ETA: 10 seconds                                           │   │    │
│  │  │                                                             │   │    │
│  │  │ Worker4: Waiting for assignment                              │   │    │
│  │  │ • State: IDLE                                               │   │    │
│  │  │ • Waiting for Section 4                                      │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                        │
│                                      ▼ reports                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  PROGRESS MONITORING                                                 │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Overall Progress:                                             │   │    │
│  │  │ • Total: 1000 blocks                                         │   │    │
│  │  │ • Complete: 147 blocks (14.7%)                                │   │    │
│  │  │ • Workers: 3 active, 1 idle                                   │   │    │
│  │  │ • Rate: 60 blocks/second (3 workers × 20/sec)                 │   │    │
│  │  │ • ETA: 14 seconds                                            │   │    │
│  │  │                                                             │   │    │
│  │  │ Rebalancing:                                                 │   │    │
│  │  │ • Worker3 completes early (9s)                               │   │    │
│  │  │ • Foreman assigns Section 4 to Worker3                       │   │    │
│  │  │ • Worker4 reassigned to other task                           │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘

SCALING:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  1 Worker:  20 blocks/sec → 1000 blocks in 50 seconds
  4 Workers: 80 blocks/sec → 1000 blocks in 12.5 seconds (4x speedup)
  10 Workers: 200 blocks/sec → 1000 blocks in 5 seconds (10x speedup)

LINEAR SCALING: Each worker adds 20 blocks/sec throughput
```

### 7.2 Task Claiming Protocol

```
ATOMIC TASK CLAIMING:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌──────────────────────────────────────────────────────────────────────────────┐
│  SHARED TASK QUEUE                                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  ConcurrentHashMap<BlockPos, TaskClaim> claimedTasks;             │    │
│  │                                                                     │    │
│  │  public class TaskClaim {                                          │    │
│  │      final String workerId;                                         │    │
│  │      final long claimTime;                                          │    │
│  │      final BlockPos position;                                       │    │
│  │  }                                                                  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
         │                                    │
         │ claim(task)                       │ claim(task)
         ▼                                    ▼
┌──────────────────┐              ┌──────────────────┐
│   WORKER 1       │              │   WORKER 2       │
│  claimTask(pos)  │              │  claimTask(pos)  │
│                  │              │                  │
│  ┌────────────┐  │              │  ┌────────────┐  │
│  │  ATTEMPT   │  │              │  │  ATTEMPT   │  │
│  │  CLAIM     │  │              │  │  CLAIM     │  │
│  └─────┬──────┘  │              │  └─────┬──────┘  │
└────────┼─────────┘              └────────┼─────────┘
         │                                  │
         │ claimedTasks.putIfAbsent(pos,   │ claimedTasks.putIfAbsent(pos,
         │   new TaskClaim(worker1, ...))  │   new TaskClaim(worker2, ...))
         │                                  │
         ▼                                  ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  RACE CONDITION HANDLING                                                   │
│                                                                             │
│  Scenario: Both workers try to claim same position simultaneously            │
│                                                                             │
│  Thread 1 (Worker1):                                                        │
│    old = claimedTasks.putIfAbsent(pos, claim1)                              │
│    if (old == null) {                                                       │
│        // Success! Worker1 gets the task                                   │
│    } else {                                                                 │
│        // Failed! Position already claimed                                  │
│        // Try next position                                                │
│    }                                                                        │
│                                                                             │
│  Thread 2 (Worker2):                                                        │
│    old = claimedTasks.putIfAbsent(pos, claim2)                              │
│    if (old == null) {                                                       │
│        // Success! (but won't happen if Worker1 won)                       │
│    } else {                                                                 │
│        // Failed! Worker1 claimed first                                    │
│        // Try next position                                                │
│    }                                                                        │
│                                                                             │
│  Result: Atomic operation ensures only one worker claims each position      │
└──────────────────────────────────────────────────────────────────────────────┘

         │                                    │
         ▼                                    ▼
┌──────────────────┐              ┌──────────────────┐
│   WORKER 1       │              │   WORKER 2       │
│  Claim SUCCESS   │              │  Claim FAILED    │
│  or FAILED       │              │  (try next pos)  │
│                  │              │                  │
│  ┌────────────┐  │              │  ┌────────────┐  │
│  │ EXECUTE    │  │              │  │  RETRY     │  │
│  │ TASK       │  │              │  │  NEXT POS  │  │
│  └────────────┘  │              │  └────────────┘  │
└──────────────────┘              └──────────────────┘

BENEFITS:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  • Thread-safe: No two workers claim same position
  • Lock-free: Uses ConcurrentHashMap CAS operations
  • Scalable: Workers can claim without coordination
  • Fault-tolerant: Claim timeout allows reassignment if worker fails
```

---

## 8. State Machine Architecture

### 8.1 Agent State Machine

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      AGENT STATE MACHINE                                     │
│                                                                              │
│                          ┌─────────────┐                                    │
│                          │    IDLE     │                                    │
│                          │  (Waiting)  │                                    │
│                          └──────┬──────┘                                    │
│                                 │ receive command                           │
│                                 ▼                                            │
│                    ┌────────────────────────────┐                           │
│                    │      PLANNING              │                           │
│                    │  • Analyze command         │                           │
│                    │  • Select planning tier    │                           │
│                    │  • Check cache             │                           │
│                    │  • Call LLM if needed      │                           │
│                    │  • Generate task sequence   │                           │
│                    └────────────┬───────────────┘                           │
│                                 │ plan ready                                │
│                                 ▼                                            │
│                    ┌────────────────────────────┐                           │
│                    │      EXECUTING             │                           │
│                    │  • Execute tasks tick-by-   │                           │
│                    │    tick                    │                           │
│                    │  • Handle interrupts       │                           │
│                    │  • Update progress         │                           │
│                    │  • Publish events          │                           │
│                    └────────────┬───────────────┘                           │
│                                 │                                            │
│                                 │                    ┌─────────────┐          │
│                                 │ all tasks complete  │  COMPLETED  │          │
│                                 └───────────────────▶│  (Success)  │          │
│                                 │                    └─────────────┘          │
│                                 │                                            │
│                                 │ needs input/interaction                     │
│                                 ▼                                            │
│                    ┌────────────────────────────┐                           │
│                    │      WAITING               │                           │
│                    │  • Awaiting user response   │                           │
│                    │  • Awaiting resource        │                           │
│                    │  • Paused by command        │                           │
│                    └────────────┬───────────────┘                           │
│                                 │ resume/error                               │
│                                 ▼                                            │
│                    ┌────────────────────────────┐                           │
│                    │      ERROR                 │                           │
│                    │  • Planning failed         │                           │
│                    │  • Execution error         │                           │
│                    │  • LLM timeout             │                           │
│                    │  • Invalid task            │                           │
│                    └────────────┬───────────────┘                           │
│                                 │ can recover?                              │
│                    ┌────────────┴───────────────┐                           │
│                    │                            │                           │
│                   Yes                          No                           │
│                    │                            │                           │
│                    ▼                            ▼                           │
│           ┌─────────────┐              ┌─────────────┐                     │
│           │  IDLE       │              │  TERMINATED │                     │
│           │  (Retry)   │              │  (Fatal)    │                     │
│           └─────────────┘              └─────────────┘                     │
└──────────────────────────────────────────────────────────────────────────────┘

VALID TRANSITIONS:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  IDLE      → PLANNING    : Receive command
  PLANNING  → EXECUTING   : Plan generated successfully
  PLANNING  → ERROR       : Planning failed (LLM error, parse error)
  EXECUTING → WAITING     : Need user input or resource
  EXECUTING → COMPLETED   : All tasks executed successfully
  EXECUTING → ERROR       : Execution error (action failed)
  WAITING   → EXECUTING   : Input received, resume execution
  WAITING   → ERROR       : Timeout or cancellation
  ERROR     → IDLE        : Recoverable error, ready for new command
  ERROR     → TERMINATED  : Fatal error, agent shutdown

STATE PROPERTIES:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  • Current state stored in AgentStateMachine
  • Transitions logged to event bus
  • State changes trigger appropriate interceptor chains
  • Timeout transitions prevent deadlocks (e.g., WAITING → ERROR after 30s)
```

### 8.2 State Transition Table

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      STATE TRANSITION TABLE                                  │
├──────────┬─────────────────┬───────────────┬────────────────────────────────┤
│ FROM     │ TO              │ TRIGGER       │ ACTION                         │
├──────────┼─────────────────┼───────────────┼────────────────────────────────┤
│ IDLE     │ PLANNING        │ command       │ Publish PlanningStartEvent     │
│          │                 │ received      │ Store command context          │
├──────────┼─────────────────┼───────────────┼────────────────────────────────┤
│ PLANNING │ EXECUTING       │ plan          │ Validate tasks                 │
│          │                 │ generated     │ Queue tasks                    │
│          │                 │               │ Publish PlanReadyEvent         │
├──────────┼─────────────────┼───────────────┼────────────────────────────────┤
│ PLANNING │ ERROR           │ LLM failed    │ Log error                      │
│          │                 │ parse failed  │ Publish PlanningFailedEvent    │
│          │                 │ timeout       │ Store error context            │
├──────────┼─────────────────┼───────────────┼────────────────────────────────┤
│ EXECUTING│ WAITING         │ user input    │ Pause execution                │
│          │                 │ needed        │ Publish PausedEvent            │
│          │                 │               │ Store resume state             │
├──────────┼─────────────────┼───────────────┼────────────────────────────────┤
│ EXECUTING│ COMPLETED       │ all tasks     │ Publish CompletionEvent        │
│          │                 │ done          │ Report results to user         │
├──────────┼─────────────────┼───────────────┼────────────────────────────────┤
│ EXECUTING│ ERROR           │ action failed │ Publish ExecutionErrorEvent    │
│          │                 │ interrupt     │ Log failed task                │
├──────────┼─────────────────┼───────────────┼────────────────────────────────┤
│ WAITING  │ EXECUTING       │ input         │ Resume execution               │
│          │                 │ received      │ Publish ResumedEvent           │
├──────────┼─────────────────┼───────────────┼────────────────────────────────┤
│ WAITING  │ ERROR           │ timeout       │ Publish TimeoutEvent           │
│          │                 │ (30s)         │ Cancel pending tasks            │
├──────────┼─────────────────┼───────────────┼────────────────────────────────┤
│ ERROR    │ IDLE            │ recoverable   │ Clear error state              │
│          │                 │ error         │ Ready for new command          │
├──────────┼─────────────────┼───────────────┼────────────────────────────────┤
│ ERROR    │ TERMINATED      │ fatal error   │ Cleanup resources              │
│          │                 │               │ Shutdown agent                 │
└──────────┴─────────────────┴───────────────┴────────────────────────────────┘

STATE METRICS:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  • Time in each state (for performance analysis)
  • Transition counts (state visit frequency)
  • Error rates per state
  • Average time from PLANNING to COMPLETED
```

---

## 9. Performance Comparison Visualizations

### 9.1 Execution Time Comparison

```
BUILDING A 100-BLOCK HOUSE: EXECUTION TIME
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Traditional ReAct (100 LLM calls):
┌──────────────────────────────────────────────────────────────────────────────┐
│  ████ 1s ████ 1s ████ 1s ████ 1s ████ 1s ████ 1s ████ 1s ████ 1s ████ 1s ████│
│  ↑      ↑      ↑      ↑      ↑      ↑      ↑      ↑      ↑      ↑      ↑   │
│  LLM    LLM    LLM    LLM    LLM    LLM    LLM    LLM    LLM    LLM    LLM│
│  Call   Call   Call   Call   Call   Call   Call   Call   Call   Call   Call│
│                                                                              │
│  Planning: 100 seconds (100 LLM calls × 1s average)                          │
│  Execution: 5 seconds (100 blocks @ 20 blocks/sec)                           │
│  ──────────────────────────────────────────────────────────────────────────  │
│  TOTAL: 105 seconds                                                          │
└──────────────────────────────────────────────────────────────────────────────┘

Steve AI (1 LLM call):
┌──────────────────────────────────────────────────────────────────────────────┐
│  ████ 1s ████                                                              │
│  ↑      ↑                                                                   │
│  LLM    Execution                                                           │
│  Call  (5s @ 20 blocks/sec)                                                 │
│                                                                              │
│  Planning: 1 second (1 LLM call)                                            │
│  Execution: 5 seconds (100 blocks @ 20 blocks/sec)                           │
│  ──────────────────────────────────────────────────────────────────────────  │
│  TOTAL: 6 seconds                                                            │
└──────────────────────────────────────────────────────────────────────────────┘

Steve AI (Cached):
┌──────────────────────────────────────────────────────────────────────────────┐
│  ████ 0s ████                                                              │
│  ↑      ↑                                                                   │
│  Cache  Execution                                                           │
│  Hit    (5s @ 20 blocks/sec)                                                │
│                                                                              │
│  Planning: 0 seconds (cache hit)                                            │
│  Execution: 5 seconds (100 blocks @ 20 blocks/sec)                           │
│  ──────────────────────────────────────────────────────────────────────────  │
│  TOTAL: 5 seconds                                                            │
└──────────────────────────────────────────────────────────────────────────────┘

SPEEDUP:
  ReAct:    105 seconds
  Steve AI:   6 seconds  → 17.5x faster
  Cached:    5 seconds  → 21x faster
```

### 9.2 Cost Comparison

```
BUILDING A 100-BLOCK HOUSE: COST
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Traditional ReAct:
  100 LLM calls × $0.01/call = $1.00

Steve AI:
  1 LLM call × $0.01/call = $0.01

Steve AI (Cached):
  0 LLM calls = $0.00

COST SAVINGS:
  ReAct:    $1.00
  Steve AI:  $0.01  → 99% savings
  Cached:    $0.00  → 100% savings

ANNUAL COST COMPARISON (1000 houses):
  ReAct:    $1,000
  Steve AI:  $10    (99% savings)
  Cached:    $0     (100% savings)
```

### 9.3 Scalability Comparison

```
SCALABILITY: BLOCKS PER SECOND
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│  Blocks/Sec (Log Scale)                                                      │
│       │                                                                      │
│   1K │                                                                      │
│       │                                                   █                  │
│   100 │                                      █                               │
│       │                    █                   █  Steve AI (Cached)           │
│    10 │                    █  Steve AI         █                             │
│       │        █           █                   █                             │
│     1 │  █     █  ReAct    █                   █                             │
│       │  █     █           █                   █                             │
│   100m │  █     █           █                   █                             │
│       └─────────────────────────────────────────────────────────────────────│
│         10     100        1,000              10,000                          │
│                         Blocks                                                     │
│                                                                              │
│  ReAct:        ~1 block/sec (LLM latency limits)                             │
│  Steve AI:     ~20 blocks/sec (execution layer)                              │
│  Steve AI:     ~∞ blocks/sec (cached, no LLM latency)                       │
│                (Limited only by game tick rate)                              │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘

SCALABILITY: CONCURRENT AGENTS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│  Total Throughput (Blocks/Sec)                                               │
│       │                                                                      │
│  2K  │                                                                      │
│       │                                                           █          │
│  1K   │                                              █           █          │
│       │                               █     █           █   █      █          │
│  500  │                     █     █     █  █  █     █     █   █      █       │
│       │          █    █     █  █  █     █  █  █     █     █   █      █       │
│  250  │    █     █  █ █     █  █  █     █  █  █     █     █   █      █       │
│       │    █  █  █  █ █  █  █  █  █  █  █  █  █  █  █  █  █  █  █  █        │
│  125  │ █  █  █  █  █ █  █  █  █  █  █  █  █  █  █  █  █  █  █  █  █        │
│       └─────────────────────────────────────────────────────────────────────│
│         1    5   10   20   50   100  200  500                                 │
│                         Concurrent Agents                                     │
│                                                                              │
│  Steve AI: Linear scaling (20 blocks/sec per agent)                          │
│    • 1 agent:   20 blocks/sec                                                │
│    • 10 agents: 200 blocks/sec                                               │
│    • 100 agents: 2,000 blocks/sec                                            │
│                                                                              │
│  ReAct: Limited by LLM API rate limits                                       │
│    • 1 agent:   ~1 block/sec                                                 │
│    • 10 agents: ~10 blocks/sec (rate limited)                                │
│    • 100 agents: ~10 blocks/sec (API throttling)                             │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 10. Architecture Decision Flowchart

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                  AI ARCHITECTURE DECISION FLOWCHART                          │
│                                                                              │
│                              ┌─────────────────┐                              │
│                              │   Need AI for   │                              │
│                              │  Game Agent?    │                              │
│                              └────────┬────────┘                              │
│                                       │                                       │
│                                       ▼                                       │
│                    ┌──────────────────────────────────┐                      │
│                    │   Need real-time performance?    │                      │
│                    │   (60 FPS, no blocking)          │                      │
│                    └──────────────┬───────────────────┘                      │
│                                   │                                           │
│                    ┌──────────────┴───────────────┐                           │
│                    │                              │                           │
│                   Yes                            No                          │
│                    │                              │                           │
│                    ▼                              ▼                           │
│     ┌─────────────────────────┐      ┌─────────────────────────┐             │
│     │  Tick-based execution   │      │  Can block for LLM?     │             │
│     │  required               │      └────────────┬────────────┘             │
│     └────────────┬────────────┘                   │                           │
│                  │                                │                           │
│                  ▼                    ┌───────────┴───────────┐                │
│     ┌─────────────────────────┐       │                       │               │
│     │  Need natural language? │      Yes                     No              │
│     └────────────┬────────────┘       │                       │               │
│                  │                    ▼                       ▼               │
│     ┌────────────┴───────────┐  ┌─────────────┐    ┌─────────────────┐      │
│     │                        │  │ ReAct LLM  │    │  Traditional    │      │
│    Yes                       No │   Agent     │    │  AI (BT/HTN)    │      │
│     │                        │  └─────────────┘    └─────────────────┘      │
│     ▼                        ▼                                                     │
│ ┌─────────────┐    ┌─────────────────────┐                                    │
│ │ Steve AI    │    │  Predictable        │                                    │
│ │ (LLM + BT)  │    │  behavior required? │                                    │
│ └─────────────┘    └─────────┬───────────┘                                    │
│                              │                                                │
│                 ┌────────────┴─────────────┐                                  │
│                 │                          │                                  │
│                Yes                        No                                 │
│                 │                          │                                  │
│                 ▼                          ▼                                  │
│     ┌─────────────────────┐    ┌─────────────────────┐                        │
│     │  Behavior Tree      │    │  Complex planning    │                        │
│     │  or FSM             │    │  required?          │                        │
│     └─────────────────────┘    └─────────┬───────────┘                        │
│                                          │                                     │
│                             ┌────────────┴─────────────┐                       │
│                             │                          │                       │
│                            Yes                        No                      │
│                             │                          │                       │
│                             ▼                          ▼                       │
│                 ┌───────────────────┐      ┌───────────────────┐               │
│                 │  HTN (structured) │      │  Utility AI       │               │
│                 │  GOAP (emergent)  │      │  (scoring)        │               │
│                 └───────────────────┘      └───────────────────┘               │
└──────────────────────────────────────────────────────────────────────────────┘

DECISION SUMMARY:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Real-time + Natural Language → Steve AI (LLM + BT/HTN)
  Real-time + No Language       → Behavior Tree or HTN
  No Real-time + Language       → ReAct LLM Agent
  No Real-time + No Language    → Traditional AI (GOAP, Utility, etc.)
```

---

## 11. Contract Net Protocol

### 11.1 Contract Net Protocol Flow

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    CONTRACT NET PROTOCOL (CNP)                                │
│                    Decentralized Task Coordination                            │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│  PHASE 1: TASK ANNOUNCEMENT                                                  │
│                                                                              │
│  Manager (Foreman)                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Task: Build stone wall at (100, 64, 100)                           │    │
│  │  Requirements:                                                       │    │
│  │  • Material: Cobblestone                                            │    │
│  │  • Dimensions: 10x5                                                 │    │
│  │  • Deadline: 60 seconds                                             │    │
│  │  • Reward: 10 reputation points                                     │    │
│  │                                                                     │    │
│  │  broadcast(CallForProposals(task)) → All workers                   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  PHASE 2: BIDDING (Workers evaluate and bid)                                 │
│                                                                              │
│  Worker 1 (Builder, at (95, 64, 98))                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Evaluation:                                                        │    │
│  │  • Distance: sqrt((100-95)² + (64-64)² + (100-98)²) = 5.4 blocks   │    │
│  │  • Capability: BUILD = 0.9 (high skill)                            │    │
│  │  • Availability: IDLE = 1.0 (ready now)                            │    │
│  │  • Inventory: Cobblestone x64 = 1.0 (sufficient)                   │    │
│  │                                                                     │    │
│  │  Score = 0.9 * 1.0 * 1.0 / 5.4 = 0.167                              │    │
│  │                                                                     │    │
│  │  Bid:                                                               │    │
│  │  • Worker ID: worker_1                                             │    │
│  │  • Estimated completion: 45 seconds                                │    │
│  │  • Confidence: 0.9                                                 │    │
│  │                                                                     │    │
│  │  sendBid(manager, bid)                                              │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  Worker 2 (Miner, at (120, 64, 120))                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Evaluation:                                                        │    │
│  │  • Distance: 28.3 blocks (far)                                     │    │
│  │  • Capability: BUILD = 0.3 (low skill)                             │    │
│  │  • Availability: IDLE = 1.0                                        │    │
│  │  • Inventory: Cobblestone x0 = 0.0 (need materials)                │    │
│  │                                                                     │    │
│  │  Score = 0.3 * 1.0 * 0.0 / 28.3 = 0.0                              │    │
│  │                                                                     │    │
│  │  Decision: NO BID (not suitable)                                   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  Worker 3 (Builder, at (105, 64, 102))                                       │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Evaluation:                                                        │    │
│  │  • Distance: 5.4 blocks                                            │    │
│  │  • Capability: BUILD = 0.95 (very high skill)                      │    │
│  │  • Availability: BUSY = 0.2 (finishing soon)                       │    │
│  │  • Inventory: Cobblestone x128 = 1.0                               │    │
│  │                                                                     │    │
│  │  Score = 0.95 * 0.2 * 1.0 / 5.4 = 0.035                            │    │
│  │                                                                     │    │
│  │  Bid:                                                               │    │
│  │  • Worker ID: worker_3                                             │    │
│  │  • Estimated completion: 60 seconds (includes current task)        │    │
│  │  • Confidence: 0.8                                                 │    │
│  │                                                                     │    │
│  │  sendBid(manager, bid)                                              │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  PHASE 3: AWARD (Manager evaluates bids)                                     │
│                                                                              │
│  Manager Evaluation                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Bids Received:                                                     │    │
│  │  • Worker 1: 0.167 score, 45s, 0.9 confidence                       │    │
│  │  • Worker 3: 0.035 score, 60s, 0.8 confidence                       │    │
│  │                                                                     │    │
│  │  Selection: Worker 1 (highest score)                                │    │
│  │                                                                     │    │
│  │  awardContract(worker_1, task)                                      │    │
│  │  rejectBid(worker_3)                                                │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  PHASE 4: EXECUTION                                                         │
│                                                                              │
│  Worker 1                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  State: ASSIGNED → EXECUTING                                        │    │
│  │  Action: Navigate to (100, 64, 100), build wall                     │    │
│  │  Progress: 0/50 blocks                                              │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  PHASE 5: COMPLETION AND TERMINATION                                        │
│                                                                              │
│  Worker 1                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  sendCompletion(manager, task, result)                              │    │
│  │  • Status: SUCCESS                                                 │    │
│  │  • Actual time: 43 seconds                                         │    │
│  │  • Blocks placed: 50                                               │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                      │                                                        │
│                      ▼                                                        │
│  Manager                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  verifyTaskCompletion(task, result)                                 │    │
│  │  • Award reputation: +10 to worker_1                                │    │
│  │  • Update worker history: successful build task                    │    │
│  │  • Notify player: "Wall completed by worker_1"                     │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘

BIDDING FORMULA:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Score = (Capability × Availability × Resources) / Distance

  Where:
  • Capability: Worker's skill for task type (0-1)
  • Availability: 1.0 if idle, lower if busy
  • Resources: Sufficient materials? (0-1)
  • Distance: Physical distance to task location

ADVANTAGES:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  • Decentralized: No central scheduler needed
  • Dynamic: Workers self-evaluate based on current state
  • Efficient: Closest, most capable worker wins
  • Scalable: Works with any number of workers
  • Fault-tolerant: Manager can reassign if worker fails
```

### 11.2 Contract Net vs. Centralized Assignment

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  CENTRALIZED ASSIGNMENT (Traditional)                                        │
│                                                                              │
│  Manager                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  • Maintains complete worker state                                  │    │
│  │  • Calculates scores for all workers                               │    │
│  │  • Makes assignment decision                                        │    │
│  │  • O(n²) complexity for n workers, m tasks                          │    │
│  │                                                                     │    │
│  │  assignTask(bestWorker)                                             │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  Issues:                                                                     │
│  • Manager bottleneck as workers/tasks scale                                │
│  • Stale worker state (async updates)                                       │
│  • Single point of failure                                                  │
└──────────────────────────────────────────────────────────────────────────────┘

                              VS

┌──────────────────────────────────────────────────────────────────────────────┐
│  CONTRACT NET PROTOCOL (Distributed)                                         │
│                                                                              │
│  Manager                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  • Broadcasts task announcement                                     │    │
│  │  • Receives bids from interested workers                           │    │
│  │  • Awards contract to best bid                                      │    │
│  │  • O(n) complexity for n workers, m tasks                           │    │
│  │                                                                     │    │
│  │  announceTask() → evaluateBids() → awardContract()                 │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  Workers (Self-Evaluation)                                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  • Evaluate own capability for task                                │    │
│  │  • Calculate bid score based on current state                      │    │
│  │  • Only bid if suitable                                            │    │
│  │  • Local state is always accurate                                  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  Advantages:                                                                 │
│  • Scalable: Manager only handles interested workers                         │
│  • Accurate: Workers know their own state                                   │
│  • Fault-tolerant: No single point of failure                               │
└──────────────────────────────────────────────────────────────────────────────┘

COMPARISON:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Aspect              │ Centralized          │ Contract Net
  ─────────────────────┼──────────────────────┼──────────────────────
  Complexity           │ O(n²)                │ O(n)
  Scalability          │ Poor (bottleneck)    │ Excellent
  State Accuracy       │ Stale (async)        │ Fresh (local)
  Fault Tolerance      │ Low (single point)   │ High (distributed)
  Communication        │ Manager → Worker     │ Manager ↔ Workers
  Decision Making      │ Manager              │ Workers (self-eval)
```

---

## 12. Script Generation and Refinement

### 12.1 LLM-to-Script Pipeline

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    SCRIPT GENERATION PIPELINE                                │
│                    "One Abstraction Away" in Action                          │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│  INPUT: User Command                                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  "Build a circular stone tower, 10 blocks high, with windows"      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  STEP 1: TASK ANALYSIS (LLM - High Complexity)                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Model: GPT-4 or GLM-5 (highest quality)                           │    │
│  │  Input: User command + context                                     │    │
│  │                                                                     │    │
│  │  Analysis:                                                          │    │
│  │  • Task type: Construction (complex pattern)                       │    │
│  │  • Structure: Circular tower (requires geometry calculation)        │    │
│  │  • Requirements: 10 high, windows, stone material                  │    │
│  │  • Preconditions: Need stone, flat area, clear space               │    │
│  │                                                                     │    │
│  │  Output: Task breakdown                                             │    │
│  │  1. Gather materials (cobblestone x320)                            │    │
│  │  2. Prepare building site (clear 15x15 area)                       │    │
│  │  3. Build circular base (radius 5, 20 blocks)                      │    │
│  │  4. Build layers 2-9 (19 blocks each, with windows)                │    │
│  │  5. Build roof/layer 10 (20 blocks, solid)                         │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  STEP 2: SCRIPT GENERATION (LLM - Medium Complexity)                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Model: GLM-4.7-Air (faster, cheaper)                              │    │
│  │  Input: Task breakdown + script template                           │    │
│  │                                                                     │    │
│  │  Generated Script (JavaScript-like DSL):                           │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ function buildCircularTower(centerX, centerY, centerZ) {   │   │    │
│  │  │   const radius = 5;                                         │   │    │
│  │  │   const height = 10;                                        │   │    │
│  │  │                                                             │   │    │
│  │  │   // Layer 1: Base (solid circle)                           │   │    │
│  │  │   for (let angle = 0; angle < 360; angle += 18) {           │   │    │
│  │  │     const x = centerX + Math.floor(radius * cos(angle));    │   │    │
│  │  │     const z = centerZ + Math.floor(radius * sin(angle));    │   │    │
│  │  │     placeBlock("cobblestone", x, centerY, z);               │   │    │
│  │  │   }                                                         │   │    │
│  │  │                                                             │   │    │
│  │  │   // Layers 2-9: With windows                               │   │    │
│  │  │   for (let y = 1; y < height - 1; y++) {                   │   │    │
│  │  │     for (let angle = 0; angle < 360; angle += 18) {         │   │    │
│  │  │       // Skip every 3rd block for window                    │   │    │
│  │  │       if (angle % 54 === 0 && y > 2 && y < 8) continue;     │   │    │
│  │  │                                                             │   │    │
│  │  │       const x = centerX + Math.floor(radius * cos(angle));  │   │    │
│  │  │       const z = centerZ + Math.floor(radius * sin(angle));  │   │    │
│  │  │       placeBlock("cobblestone", x, centerY + y, z);         │   │    │
│  │  │     }                                                       │   │    │
│  │  │   }                                                         │   │    │
│  │  │                                                             │   │    │
│  │  │   // Layer 10: Roof (solid)                                 │   │    │
│  │  │   for (let angle = 0; angle < 360; angle += 18) {           │   │    │
│  │  │     const x = centerX + Math.floor(radius * cos(angle));    │   │    │
│  │  │     const z = centerZ + Math.floor(radius * sin(angle));    │   │    │
│  │  │     placeBlock("cobblestone", x, centerY + 9, z);           │   │    │
│  │  │   }                                                         │   │    │
│  │  │ }                                                          │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  STEP 3: SCRIPT VALIDATION                                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Validation Checks:                                                │    │
│  │  • Syntax check (valid JavaScript/DSL)                             │    │
│  │  • API usage (only allowed functions called)                       │    │
│  │  • Resource estimation (blocks needed)                             │    │
│  │  • Safety check (no infinite loops)                                │    │
│  │  • Timeout estimation (should complete in <5 min)                  │    │
│  │                                                                     │    │
│  │  Result: VALID                                                      │    │
│  │  • Estimated blocks: 184                                           │    │
│  │  • Estimated time: 45 seconds                                      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  STEP 4: SCRIPT CACHING                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  • Compute semantic hash of task description                        │    │
│  │  • Store script in skill library with metadata:                     │    │
│  │    - Name: "circular_stone_tower_10"                               │    │
│  │    - Type: CONSTRUCTION                                            │    │
│  │    - Complexity: HIGH                                              │    │
│  │    - Success rate: Unknown (first use)                             │    │
│  │    - Created: 2026-03-01                                           │    │
│  │    - Used: 0 times                                                 │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  STEP 5: SCRIPT EXECUTION (Zero LLM Calls)                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Execution Engine (GraalVM JS Sandbox)                             │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Tick 1: Execute layer 1 (20 blocks)                         │   │    │
│  │  │   → placeBlock() calls at (100,64,100), (101,64,100)...     │   │    │
│  │  │   → Progress: 20/184 (11%)                                   │   │    │
│  │  │                                                             │   │    │
│  │  │ Tick 2-9: Execute layers 2-9 with windows (144 blocks)      │   │    │
│  │  │   → placeBlock() calls with window skips                    │   │    │
│  │  │   → Progress: 164/184 (89%)                                  │   │    │
│  │  │                                                             │   │    │
│  │  │ Tick 10: Execute roof (20 blocks)                           │   │    │
│  │  │   → placeBlock() calls for top layer                        │   │    │
│  │  │   → Progress: 184/184 (100%)                                 │   │    │
│  │  │                                                             │   │    │
│  │  │ Total execution: 10 ticks @ 20 TPS = 0.5 seconds            │   │    │
│  │  │ LLM calls: 0 (all local execution)                          │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  STEP 6: EXECUTION FEEDBACK                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Execution Metrics:                                                │    │
│  │  • Status: SUCCESS                                                │    │
│  │  • Blocks placed: 184                                              │    │
│  │  • Time taken: 9.2 seconds (agent movement included)               │    │
│  │  • Errors: 0                                                       │    │
│  │  • LLM calls: 0                                                    │    │
│  │  • Token cost: 0 (script cached)                                   │    │
│  │                                                                     │    │
│  │  Update script metadata:                                            │    │
│  │  • Success rate: 1/1 (100%)                                        │    │
│  │  • Used: 1 time                                                    │    │
│  │  • Avg time: 9.2 seconds                                          │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘

TOKEN SAVINGS:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Traditional ReAct (184 blocks × 1 LLM call each):
  • Tokens: ~184,000 input + ~184,000 output = 368,000 tokens
  • Cost: ~$3.68 (at $0.01/1K tokens)
  • Time: ~184 seconds (1 sec/LLM call)

  Script Generation (one-time):
  • Tokens: ~1,500 input + ~2,500 output = 4,000 tokens (generation)
  • Cost: ~$0.04 (one-time)
  • Time: ~3 seconds (generation)
  • Subsequent runs: $0, 0 tokens, 0.5 seconds

  SAVINGS: 99% cost reduction, 98% time reduction (after first run)
```

### 12.2 Script Refinement Loop

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    SCRIPT REFINEMENT LOOP                                    │
│                    Continuous Improvement System                            │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│  ITERATION 1: Initial Script                                                │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Task: "Build efficient mining shaft"                               │    │
│  │                                                                     │    │
│  │  Generated Script:                                                  │    │
│  │  function buildMiningShaft(x, y, z) {                              │    │
│  │    // Dig straight down                                           │    │
│  │    for (let i = 0; i < 50; i++) {                                  │    │
│  │      digBlock(x, y - i, z);                                        │    │
│  │    }                                                               │    │
│  │  }                                                                 │    │
│  │                                                                     │    │
│  │  Execution Result:                                                 │    │
│  │  • Status: PARTIAL SUCCESS                                         │    │
│  │  • Issues:                                                         │    │
│  │    - Fell into lava at y=35 (died)                                 │    │
│  │    - No torches placed (dark)                                      │    │
│  │    - No safety barriers (fell)                                     │    │
│  │  • Score: 0.3/1.0                                                  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  ANALYSIS: What went wrong?                                                │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Failure Analysis:                                                  │    │
│  │  1. No depth check (lava danger)                                    │    │
│  │  2. No lighting (mob spawning)                                      │    │
│  │  3. no safety (fall damage)                                        │    │
│  │                                                                     │    │
│  │  Extracted Patterns:                                                │    │
│  │  • Always check block below before digging                          │    │
│  │  • Place torches every 5 blocks                                    │    │
│  │  • Add ladders/water for safety                                     │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  ITERATION 2: Refined Script                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Refined Script:                                                   │    │
│  │  function buildMiningShaft(x, y, z) {                              │    │
│  │    for (let i = 0; i < 50; i++) {                                  │    │
│  │      const currentY = y - i;                                       │    │
│  │                                                                     │    │
│  │      // Check for lava/danger                                      │    │
│  │      if (isDangerous(x, currentY - 1, z)) {                       │    │
│  │        placeBlock("cobblestone", x, currentY - 1, z);             │    │
│  │        continue; // Skip this layer                                │    │
│  │      }                                                             │    │
│  │                                                                     │    │
│  │      // Dig block                                                  │    │
│  │      digBlock(x, currentY, z);                                     │    │
│  │                                                                     │    │
│  │      // Place torch every 5 blocks                                 │    │
│  │      if (i % 5 === 0) {                                            │    │
│  │        placeBlock("torch", x + 1, currentY, z);                   │    │
│  │      }                                                             │    │
│  │                                                                     │    │
│  │      // Place ladder for safety                                    │    │
│  │      placeBlock("ladder", x, currentY, z + 1);                    │    │
│  │    }                                                               │    │
│  │  }                                                                 │    │
│  │                                                                     │    │
│  │  Execution Result:                                                 │    │
│  │  • Status: SUCCESS                                                │    │
│  │  • Depth reached: 50 blocks                                        │    │
│  │  • Torches placed: 10                                              │    │
│  │  • Ladders placed: 50                                              │    │
│  │  • Deaths: 0                                                       │    │
│  │  • Score: 0.9/1.0                                                  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  KNOWLEDGE EXTRACTION                                                       │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Extracted Pattern: "Safe Mining Shaft"                            │    │
│  │  • Name: safe_vertical_shaft                                       │    │
│  │  • Category: MINING                                                │    │
│  │  • Preconditions: Pickaxe, torches, ladders, cobblestone           │    │
│  │  • Success rate: 90% (after refinement)                            │    │
│  │  • Code: [refined script above]                                    │    │
│  │                                                                     │    │
│  │  Stored in Skill Library for:                                      │    │
│  │  • Future reuse (same task)                                        │    │
│  │  • Similar tasks (horizontal shafts)                               │    │
│  │  • Pattern extraction (mining safety principles)                   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  ITERATION 3: Generalization                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Task: "Build horizontal mining tunnel"                            │    │
│  │                                                                     │    │
│  │  Applied Patterns from "Safe Mining Shaft":                        │    │
│  │  • Danger checking before digging                                  │    │
│  │  • Torches every 5 blocks                                         │    │
│  │  • Escape route (ladders back to surface)                          │    │
│  │                                                                     │    │
│  │  Result: Script worked on first try!                               │    │
│  │  • Success rate: 0.95/1.0                                          │    │
│  │  • Iterations saved: 2                                             │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘

LEARNING METRICS:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Iteration 1: 0.3/1.0 → 0.9/1.0 (300% improvement)
  Iteration 2: 0.9/1.0 → 0.95/1.0 (6% improvement)
  Iteration 3: 0.0/1.0 → 0.95/1.0 (infinite improvement via pattern reuse)

  Cumulative Savings:
  • Tokens: 90% reduction after 3 refinements
  • Time: 85% reduction after 3 refinements
  • Success: 95% success rate on similar tasks
```

### 12.3 Skill Library Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      SKILL LIBRARY STRUCTURE                                 │
│                      Voyager-Style Learning System                          │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│  SKILL METADATA                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  {                                                                 │    │
│  │    "id": "safe_vertical_shaft_v2",                                 │    │
│  │    "name": "Safe Vertical Mining Shaft",                           │    │
│  │    "category": "MINING",                                           │    │
│  │    "created": "2026-03-01T10:30:00Z",                              │    │
│  │    "modified": "2026-03-01T14:45:00Z",                             │    │
│  │    "version": 2,                                                   │    │
│  │    "author": "worker_3",                                           │    │
│  │                                                                     │    │
│  │    "execution": {                                                  │    │
│  │      "times_used": 47,                                             │    │
│  │      "success_count": 45,                                          │    │
│  │      "failure_count": 2,                                           │    │
│  │      "success_rate": 0.957,                                        │    │
│  │      "avg_duration": 12.3,                                         │    │
│  │      "last_used": "2026-03-01T18:20:00Z"                            │    │
│  │    },                                                              │    │
│  │                                                                     │    │
│  │    "dependencies": [                                               │    │
│  │      "check_danger_v1",                                            │    │
│  │      "place_torches_v1"                                            │    │
│  │    ],                                                              │    │
│  │                                                                     │    │
│  │    "tags": ["mining", "vertical", "safe", "shaft"],                │    │
│  │    "complexity": "MEDIUM",                                         │    │
│  │    "reliability": "HIGH"                                           │    │
│  │  }                                                                 │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  SKILL CODE (JavaScript DSL)                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  /**                                                               │    │
│  │   * Safe vertical mining shaft with lighting and safety            │    │
│  │   * @param {number} x - Starting X coordinate                     │    │
│  │   * @param {number} y - Starting Y coordinate                     │    │
│  │   * @param {number} z - Starting Z coordinate                     │    │
│  │   * @param {number} depth - Depth to dig (default 50)             │    │
│  │   */                                                               │    │
│  │  function execute(x, y, z, depth = 50) {                          │    │
│  │    for (let i = 0; i < depth; i++) {                              │    │
│  │      const currentY = y - i;                                       │    │
│  │                                                                     │    │
│  │      // Use dependency: check_danger_v1                            │    │
│  │      if (!checkDanger(x, currentY - 1, z)) {                      │    │
│  │        continue;                                                   │    │
│  │      }                                                             │    │
│  │                                                                     │    │
│  │      digBlock(x, currentY, z);                                     │    │
│  │                                                                     │    │
│  │      // Use dependency: place_torches_v1                           │    │
│  │      if (i % 5 === 0) {                                            │    │
│  │        placeTorch(x + 1, currentY, z);                             │    │
│  │      }                                                             │    │
│  │                                                                     │    │
│  │      placeBlock("ladder", x, currentY, z + 1);                    │    │
│  │    }                                                               │    │
│  │                                                                     │    │
│  │    return {                                                        │    │
│  │      status: "success",                                           │    │
│  │      blocksDug: depth,                                            │    │
│  │      torchesPlaced: Math.floor(depth / 5)                         │    │
│  │    };                                                              │    │
│  │  }                                                                 │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  SEMANTIC INDEXING (Vector Embeddings)                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Embedding: [0.234, -0.567, 0.891, ...] (384 dimensions)            │    │
│  │                                                                     │    │
│  │  Related Skills (by semantic similarity):                           │    │
│  │  1. horizontal_tunnel_v1 (similarity: 0.89)                        │    │
│  │  2. strip_mine_v2 (similarity: 0.76)                               │    │
│  │  3. cave_exploration_v1 (similarity: 0.68)                          │    │
│  │                                                                     │    │
│  │  Query: "How to dig deep safely?"                                  │    │
│  │  → Retrieves: safe_vertical_shaft_v2 (most relevant)               │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  SKILL RETRIEVAL FLOW                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  User Command: "Dig a safe hole to bedrock"                        │    │
│  │                     │                                                │    │
│  │                     ▼                                                │    │
│  │  1. Embed query: "dig safe hole bedrock" → [0.345, -0.678, ...]    │    │
│  │                     │                                                │    │
│  │                     ▼                                                │    │
│  │  2. Search skill library:                                           │    │
│  │    - safe_vertical_shaft_v2 (similarity: 0.94) ← MATCH              │    │
│  │    - branch_mining_v1 (similarity: 0.72)                            │    │
│  │    - cave_clearing_v1 (similarity: 0.65)                            │    │
│  │                     │                                                │    │
│  │                     ▼                                                │    │
│  │  3. Retrieve skill code:                                            │    │
│  │    - Return: safe_vertical_shaft_v2.execute(x, y, z, 60)           │    │
│  │    - Success rate: 95.7%                                            │    │
│  │                     │                                                │    │
│  │                     ▼                                                │    │
│  │  4. Execute directly (no LLM call needed):                          │    │
│  │    - Run script in sandbox                                          │    │
│  │    - Agent digs to bedrock safely                                   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘

SKILL EVOLUTION:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  v1: Basic shaft (0.3 success rate)
  v2: Added danger checking (0.7 success rate)
  v3: Added torches (0.9 success rate)
  v4: Added ladders (0.957 success rate) ← Current version

  Total LLM calls for v4: 0 (after initial refinement)
  Total executions: 47
  Cumulative time saved: ~8.5 minutes (vs. ReAct)
  Cumulative cost saved: ~$15.00 (vs. ReAct)
```

---

## 13. Memory System Architecture

### 13.1 Three-Tier Memory Hierarchy

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    THREE-TIER MEMORY HIERARCHY                              │
│                    Inspired by Human Cognitive Architecture                 │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│  TIER 1: WORKING MEMORY (Short-term, Fast Access)                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Capacity: ~7 ± 2 items (Miller's Law)                              │    │
│  │  Duration: Seconds to minutes                                       │    │
│  │  Access: O(1) - Constant time lookup                                │    │
│  │  Storage: In-memory (Java heap)                                     │    │
│  │                                                                     │    │
│  │  Contents (Current Conversation):                                   │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ [0] User: "Build a house here"                               │   │    │
│  │  │ [1] Foreman: "I'll coordinate the workers"                   │   │    │
│  │  │ [2] Worker1: "I'll gather materials"                         │   │    │
│  │  │ [3] Worker2: "I'll clear the area"                           │   │    │
│  │  │ [4] Worker3: "I'll start the foundation"                     │   │    │
│  │  │ [5] System: "Task assigned to 3 workers"                     │   │    │
│  │  │ [6] User: "Make it stone, not wood"                           │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  │                                                                     │    │
│  │  Implementation:                                                    │    │
│  │  • Circular buffer (max 10 items)                                  │    │
│  │  • Automatic eviction (FIFO)                                       │    │
│  │  • Tick-based updates (20 TPS)                                     │    │
│  │  • Shared across agents via event bus                              │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼ (consolidation trigger)
┌──────────────────────────────────────────────────────────────────────────────┐
│  TIER 2: EPISODIC MEMORY (Mid-term, Conversational)                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Capacity: Hundreds of conversations                                │    │
│  │  Duration: Session to weeks                                        │    │
│  │  Access: O(log n) - Indexed search                                 │    │
│  │  Storage: Persistent (JSON files + vector DB)                      │    │
│  │                                                                     │    │
│  │  Structure (Conversation Episodes):                                 │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ {                                                           │   │    │
│  │  │   "id": "conv_20260301_143022",                             │   │    │
│  │  │   "timestamp": "2026-03-01T14:30:22Z",                       │   │    │
│  │  │   "participants": ["player", "foreman_steve"],               │   │    │
│  │  │   "context": {                                               │   │    │
│  │  │     "location": "plains_biome",                              │   │    │
│  │  │     "task": "build_house",                                   │   │    │
│  │  │     "agents_present": 3                                      │   │    │
│  │  │   },                                                         │   │    │
│  │  │   "messages": [                                              │   │    │
│  │  │     {                                                       │   │    │
│  │  │       "speaker": "player",                                   │   │    │
│  │  │       "text": "Build a house here",                          │   │    │
│  │  │       "timestamp": "14:30:22"                                │   │    │
│  │  │     },                                                      │   │    │
│  │  │     {                                                       │   │    │
│  │  │       "speaker": "foreman_steve",                            │   │    │
│  │  │       "text": "I'll coordinate the workers. Stone or wood?", │   │    │
│  │  │       "timestamp": "14:30:23"                                │   │    │
│  │  │     },                                                      │   │    │
│  │  │     // ... more messages                                     │   │    │
│  │  │   ],                                                         │   │    │
│  │  │   "outcome": {                                               │   │    │
│  │  │     "success": true,                                        │   │    │
│  │  │     "duration_seconds": 127,                                 │   │    │
│  │  │     "blocks_placed": 487                                     │   │    │
│  │  │   },                                                         │   │    │
│  │  │   "embedding": [0.123, -0.456, ...],  // Semantic vector    │   │    │
│  │  │   "importance": 0.87  // Calculated from recency + impact   │   │    │
│  │  │ }                                                           │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  │                                                                     │    │
│  │  Consolidation Triggers:                                            │    │
│  │  • Conversation ends (agent returns to IDLE)                       │    │
│  │  • Working memory buffer full                                      │    │
│  │  • Significant event occurs (task completion, error)               │    │
│  │  • Time-based (every 5 minutes)                                    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼ (consolidation trigger)
┌──────────────────────────────────────────────────────────────────────────────┐
│  TIER 3: SEMANTIC MEMORY (Long-term, Knowledge)                            │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Capacity: Unlimited (bounded by storage)                          │    │
│  │  Duration: Permanent                                               │    │
│  │  Access: O(1) with semantic search                                 │    │
│  │  Storage: Persistent (Vector database)                             │    │
│  │                                                                     │    │
│  │  Knowledge Types:                                                   │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ 1. PROCEDURAL KNOWLEDGE (Skills)                            │   │    │
│  │  │    {                                                       │   │    │
│  │  │      "type": "skill",                                      │   │    │
│  │  │      "name": "build_stone_house",                          │   │    │
│  │  │      "learned_from": "conv_20260301_143022",                │   │    │
│  │  │      "success_rate": 0.95,                                  │   │    │
│  │  │      "usage_count": 47                                      │   │    │
│  │  │    }                                                       │   │    │
│  │  │                                                             │   │    │
│  │  │ 2. DECLARATIVE KNOWLEDGE (Facts)                            │   │    │
│  │  │    {                                                       │   │    │
│  │  │      "type": "fact",                                       │   │    │
│  │  │      "subject": "player",                                   │   │    │
│  │  │      "predicate": "prefers_stone",                          │   │    │
│  │  │      "confidence": 0.9,                                     │   │    │
│  │  │      "source": "conversation_evidence",                     │   │    │
│  │  │      "last_confirmed": "2026-03-01T14:30:25Z"               │   │    │
│  │  │    }                                                       │   │    │
│  │  │                                                             │   │    │
│  │  │ 3. RELATIONAL KNOWLEDGE (Relationships)                     │   │    │
│  │  │    {                                                       │   │    │
│  │  │      "type": "relationship",                               │   │    │
│  │  │      "entities": ["player", "foreman_steve"],               │   │    │
│  │  │      "relation_type": "trust",                              │   │    │
│  │  │      "strength": 0.87,                                     │   │    │
│  │  │      "history": [                                          │   │    │
│  │  │        "successful_collaboration",                          │   │    │
│  │  │        "shared_accomplishments",                            │   │    │
│  │  │        "positive_interactions"                              │   │    │
│  │  │      ]                                                      │   │    │
│  │  │    }                                                       │   │    │
│  │  │                                                             │   │    │
│  │  │ 4. SPATIAL KNOWLEDGE (Locations)                            │   │    │
│  │  │    {                                                       │   │    │
│  │  │      "type": "location",                                   │   │    │
│  │  │      "name": "base_main_house",                            │   │    │
│  │  │      "coordinates": {"x": 100, "y": 64, "z": 100},          │   │    │
│  │  │      "description": "Player's main base, stone house",      │   │    │
│  │  │      "importance": 0.95                                    │   │    │
│  │  │    }                                                       │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘

MEMORY FLOW:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  1. Experience → Working Memory (immediate)
  2. Working Memory → Episodic Memory (consolidation, ~5 min)
  3. Episodic Memory → Semantic Memory (pattern extraction, ~1 hour)
  4. Semantic Memory → Skill Library (proven patterns, ~24 hours)

  RETRIEVAL:
  • Working Memory: Direct access (current context)
  • Episodic Memory: Semantic search (recent conversations)
  • Semantic Memory: Vector similarity (knowledge, skills)
```

### 13.2 Memory Consolidation Process

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    MEMORY CONSOLIDATION ENGINE                               │
│                    From Episodes to Knowledge                                │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│  INPUT: Episodic Memory (Conversations)                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Recent Episodes (last hour):                                       │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ [1] conv_143022: "Build stone house" → SUCCESS (127s)       │   │    │
│  │  │ [2] conv_143215: "Gather wood" → SUCCESS (45s)               │   │    │
│  │  │ [3] conv_144500: "Build circular tower" → FAILED (lava)      │   │    │
│  │  │ [4] conv_144800: "Repair tower" → SUCCESS (30s)              │   │    │
│  │  │ [5] conv_150100: "Mining expedition" → PARTIAL (cave spider) │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  PHASE 1: PATTERN EXTRACTION (LLM)                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Model: GLM-4.7-Air (medium complexity)                             │    │
│  │  Task: Extract reusable patterns from episodes                      │    │
│  │                                                                     │    │
│  │  Prompt:                                                            │    │
│  │  "Analyze these conversations and extract:                         │    │
│  │   1. Player preferences (materials, locations, styles)              │    │
│  │   2. Successful task patterns (what worked?)                       │    │
│  │   3. Failure patterns (what didn't work?)                          │    │
│  │   4. Relationship updates (trust changes)                           │    │
│  │   5. Spatial knowledge (important locations)"                       │    │
│  │                                                                     │    │
│  │  Extracted Patterns:                                                │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ Player Preferences:                                         │   │    │
│  │  │ • Prefers stone over wood (100% stone requests)             │   │    │
│  │  │ • Builds in plains biome (5/5 recent builds)                │   │    │
│  │  │ • Likes circular structures (tower request)                  │   │    │
│  │  │                                                             │   │    │
│  │  │ Successful Patterns:                                        │   │    │
│  │  │ • Multi-worker coordination speeds up building (3-5x)       │   │    │
│  │  │ • Material gathering before building prevents delays        │   │    │
│  │  │                                                             │   │    │
│  │  │ Failure Patterns:                                           │   │    │
│  │  │ • Building at y<35 risks lava (1 failure)                   │   │    │
│  │  │ • Mining without torches attracts mobs (1 partial)          │   │    │
│  │  │                                                             │   │    │
│  │  │ Relationship Updates:                                        │   │    │
│  │  │ • Trust increased: +0.15 (4 successes, 1 failure)           │   │    │
│  │  │ • Reliability score: 0.82 (previous: 0.67)                  │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  PHASE 2: KNOWLEDGE INTEGRATION                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Update Semantic Memory:                                            │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ 1. Update Player Profile:                                    │   │    │
│  │  │    {                                                       │   │    │
│  │  │      "type": "fact",                                       │   │    │
│  │  │      "subject": "player",                                   │   │    │
│  │  │      "predicate": "prefers_stone",                          │   │    │
│  │  │      "confidence": 1.0,  // Increased from 0.5             │   │    │
│  │  │      "evidence_count": 5,                                   │   │    │
│  │  │      "last_observed": "2026-03-01T15:01:00Z"                │   │    │
│  │  │    }                                                       │   │    │
│  │  │                                                             │   │    │
│  │  │ 2. Update Relationship:                                      │   │    │
│  │  │    {                                                       │   │    │
│  │  │      "type": "relationship",                               │   │    │
│  │  │      "entities": ["player", "foreman_steve"],               │   │    │
│  │  │      "relation_type": "trust",                              │   │    │
│  │  │      "strength": 0.87,  // Increased from 0.72             │   │    │
│  │  │      "trend": "increasing",                                 │   │    │
│  │  │      "recent_interactions": 5                               │   │    │
│  │  │    }                                                       │   │    │
│  │  │                                                             │   │    │
│  │  │ 3. Add Spatial Knowledge:                                   │   │    │
│  │  │    {                                                       │   │    │
│  │  │      "type": "location",                                   │   │    │
│  │  │      "name": "base_stone_house",                           │   │    │
│  │  │      "coordinates": {"x": 100, "y": 64, "z": 100},          │   │    │
│  │  │      "importance": 0.95,                                   │   │    │
│  │  │      "visits": 7,                                          │   │    │
│  │  │      "description": "Main base, stone construction"         │   │    │
│  │  │    }                                                       │   │    │
│  │  │                                                             │   │    │
│  │  │ 4. Extract Skill:                                           │   │    │
│  │  │    {                                                       │   │    │
│  │  │      "type": "skill",                                      │   │    │
│  │  │      "name": "coordinated_building",                        │   │    │
│  │  │      "success_rate": 0.9,                                  │   │    │
│  │  │      "pattern": "Assign 3-5 workers to different sections" │   │    │
│  │  │    }                                                       │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  PHASE 3: FORGETTING (Memory Pruning)                                       │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Retention Policy:                                                  │    │
│  │  • Keep: High importance (>0.7)                                     │    │
│  │  • Keep: Recent (<1 week)                                           │    │
│  │  • Compress: Medium importance (0.3-0.7), older                    │    │
│  │  • Forget: Low importance (<0.3), old (>1 month)                   │    │
│  │                                                                     │    │
│  │  Episodic Memory After Consolidation:                               │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ [1] conv_143022: COMPRESSED → "Player built stone house"    │   │    │
│  │  │ [2] conv_143215: FORGOTTEN (low importance)                  │   │    │
│  │  │ [3] conv_144500: KEPT (failure = learning opportunity)       │   │    │
│  │  │ [4] conv_144800: FORGOTTEN (routine repair)                  │   │    │
│  │  │ [5] conv_150100: KEPT (dangerous = important)                │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  │                                                                     │    │
│  │  Compression Example:                                               │    │
│  │  BEFORE: 47 messages (full conversation)                           │    │
│  │  AFTER: 1 summary (player built stone house successfully)          │    │
│  │  Space saved: 98%                                                  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘

CONSOLIDATION SCHEDULE:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Real-time:     Working Memory (current tick)
  Every 5 min:   Consolidate to Episodic Memory
  Every hour:    Extract patterns to Semantic Memory
  Daily:         Prune old/low-importance memories
  Weekly:        Skill library optimization

  STORAGE:
  • Working Memory: ~100 KB (10 items × 10 KB)
  • Episodic Memory: ~10 MB (100 conversations × 100 KB)
  • Semantic Memory: ~50 MB (skills, facts, relationships)
  • Total: ~60 MB per agent
```

### 13.3 Vector Search for Semantic Retrieval

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    SEMANTIC VECTOR SEARCH                                    │
│                    Finding Relevant Memories by Meaning                     │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│  EMBEDDING GENERATION                                                       │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Input Text: "Player wants a stone house near the base"            │    │
│  │                     │                                                │    │
│  │                     ▼                                                │    │
│  │  Embedding Model: all-MiniLM-L6-v2 (384 dimensions)                │    │
│  │                     │                                                │    │
│  │                     ▼                                                │    │
│  │  Vector: [0.234, -0.567, 0.891, 0.123, -0.456, ..., 0.789]         │    │
│  │          (384 floating-point numbers)                               │    │
│  │                     │                                                │    │
│  │                     ▼                                                │    │
│  │  Normalization: Unit vector (length = 1.0)                          │    │
│  │  Length: sqrt(0.234² + 0.567² + ...) = 1.0                          │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  SIMILARITY CALCULATION (Cosine Similarity)                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Query Vector: q = [0.2, -0.5, 0.8, ...]                            │    │
│  │  Memory Vector: m = [0.3, -0.4, 0.7, ...]                            │    │
│  │                                                                     │    │
│  │  Cosine Similarity:                                                 │    │
│  │    similarity(q, m) = (q · m) / (||q|| × ||m||)                     │    │
│  │                     = dot_product(q, m)  (since both normalized)    │    │
│  │                                                                     │    │
│  │  Example:                                                           │    │
│  │    q · m = (0.2 × 0.3) + (-0.5 × -0.4) + (0.8 × 0.7) + ...         │    │
│  │         = 0.06 + 0.20 + 0.56 + ... = 0.82                          │    │
│  │                                                                     │    │
│  │  Similarity Score: 0.82 (82% match)                                 │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  VECTOR DATABASE (In-Memory)                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Memory Index:                                                      │    │
│  │  ┌────────────────┬──────────────────────────┬──────────────┐       │    │
│  │  │ Memory ID      │ Vector (384 dim)          │ Similarity   │       │    │
│  │  ├────────────────┼──────────────────────────┼──────────────┤       │    │
│  │  │ conv_143022    │ [0.3, -0.4, 0.7, ...]    │ 0.94 ← MATCH │       │    │
│  │  │ conv_130515    │ [0.1, -0.6, 0.9, ...]    │ 0.87         │       │    │
│  │  │ skill_house_1  │ [0.4, -0.3, 0.6, ...]    │ 0.81         │       │    │
│  │  │ conv_121000    │ [-0.2, 0.5, -0.3, ...]   │ 0.23         │       │    │
│  │  │ fact_base_loc  │ [0.9, 0.1, -0.2, ...]    │ 0.19         │       │    │
│  │  └────────────────┴──────────────────────────┴──────────────┘       │    │
│  │                                                                     │    │
│  │  Search Query: "Player wants stone house"                           │    │
│  │  Top Result: conv_143022 (similarity: 0.94)                          │    │
│  │    → "Player: Build a stone house here"                            │    │
│  │    → Context: House building task from earlier                     │    │
│  │                                                                     │    │
│  │  Retrieved Context:                                                 │    │
│  │  1. conv_143022: "Build stone house" (0.94) ← Direct match          │    │
│  │  2. conv_130515: "Building preferences" (0.87) ← Related            │    │
│  │  3. skill_house_1: "House building skill" (0.81) ← Skill            │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  RETRIEVAL STRATEGIES                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Strategy 1: SEMANTIC SIMILARITY (Default)                          │    │
│  │  • Use cosine similarity to find related memories                   │    │
│  │  • Returns: Top-k most similar memories                             │    │
│  │  • Use case: "What did we do about X before?"                       │    │
│  │                                                                     │    │
│  │  Strategy 2: TEMPORAL PROXIMITY                                      │    │
│  │  • Filter by time range (last hour, day, week)                      │    │
│  │  • Returns: Memories within time window                             │    │
│  │  • Use case: "What happened recently?"                              │    │
│  │                                                                     │    │
│  │  Strategy 3: HYBRID (Semantic + Temporal)                           │    │
│  │  • Semantic search within time window                               │    │
│  │  • Returns: Similar memories from recent period                     │    │
│  │  • Use case: "What did we do about X recently?"                     │    │
│  │                                                                     │    │
│  │  Strategy 4: IMPORTANCE WEIGHTED                                    │    │
│  │  • Semantic similarity × importance score                           │    │
│  │  • Returns: Important related memories                              │    │
│  │  • Use case: "What are the key things about X?"                     │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘

PERFORMANCE:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Embedding Generation: ~50ms (CPU), ~10ms (GPU)
  Vector Search (1K memories): ~5ms (brute force), ~1ms (HNSW index)
  Total Retrieval Time: ~15-60ms
  Accuracy: 85-95% (depending on query clarity)

  ADVANTAGES:
  • Finds related memories even with different wording
  • No need for exact keyword matching
  • Handles synonyms and paraphrases naturally
  • Scales to millions of memories with approximate nearest neighbor (ANN)
```

---

## Appendix: Diagram Conventions

### Visual Symbols

```
┌──────────────┐  Box/Container  → Components, layers, modules
│  Component   │
└──────────────┘

     ┌─────┐     Small Box     → Data elements, states
     │Data │
     └─────┘

────────────────  Horizontal Line → Section separator
━━━━━━━━━━━━━━━━━  Double Line    → Performance metrics

       ▼           Arrow         → Flow direction, data flow
       │

      ███         Bar            → Time duration, quantity

┌───┬───┬───┐   Row             → Sequential items
│ 1 │ 2 │ 3 │
└───┴───┴───┘

┌─────────────┐
│    Tree     │  Tree           → Hierarchical structure
│  ├─ Node A  │
│  └─ Node B  │
└─────────────┘
```

### Color Coding (Text-Based)

```
[Uppercase]     → Main component, system, layer
[lowercase]     → Sub-component, method, variable
"Quotes"        → User input, example data
'Code'          → Code snippets, class names
• Bullet        → List items, features
→ Arrow         → Transitions, flow
```

### Performance Metrics Format

```
METRICS:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Metric Name:     Value
  Metric Name 2:   Value
  Metric Name 3:   Value
```

---

## Document References

These diagrams support the following dissertation chapters:

- **Chapter 6:** AI Architecture Patterns for Game Agents
  - Traditional vs. Hybrid architectures
  - Behavior trees, HTN, GOAP, Utility AI

- **Chapter 8:** Architecture Comparison
  - ReAct vs. Steve AI performance
  - Cost and scalability analysis

- **Implementation:**
  - Plugin system design
  - Event-driven architecture
  - Multi-agent coordination

**Related Files:**
- `C:\Users\casey\steve\docs\research\DISSERTATION_CHAPTER_6_ARCHITECTURE_IMPROVED.md`
- `C:\Users\casey\steve\docs\research\CHAPTER_8_ARCHITECTURE_COMPARISON.md`

---

**Document Status:** Complete
**Last Updated:** 2026-03-01
**Version:** 1.1
**Author:** Steve AI Project
**Changes:**
- Added Section 11: Contract Net Protocol (decentralized task coordination)
- Added Section 12: Script Generation and Refinement (LLM-to-Script pipeline)
- Added Section 13: Memory System Architecture (three-tier hierarchy)
