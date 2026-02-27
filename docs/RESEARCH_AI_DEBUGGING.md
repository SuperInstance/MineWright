# Research Report: AI Debugging and Explainability for Game Agents

**Project:** MineWright (MineWright AI - Minecraft Agents)
**Date:** 2025-02-27
**Author:** Claude (Orchestrator Agent)
**Version:** 1.0

---

## Executive Summary

This report investigates AI debugging and explainability techniques for autonomous game agents, with a focus on making agent behavior transparent, debuggable, and recoverable. The research covers state-of-the-art approaches from LLM observability platforms, game AI debugging tools, and explainable AI (XAI) research, with specific applications to the MineWright Minecraft agent system.

**Key Findings:**
- **LangSmith** leads the market for LLM agent tracing with full-chain visualization
- **Game engines** (Unity/Unreal) have mature debugging tooling for behavior trees
- **XAI techniques** like LIME/SHAP are primarily for ML models, less applicable to LLM agents
- **Stuck detection** and **timeout handling** are critical production concerns
- **Multi-agent error attribution** is an emerging research area (ICML 2025)

---

## Table of Contents

1. [Explainable AI (XAI) for Game Agents](#1-explainable-ai-xai-for-game-agents)
2. [Debugging Tools and Platforms](#2-debugging-tools-and-platforms)
3. [Visualization Approaches](#3-visualization-approaches)
4. [Error Attribution and Root Cause Analysis](#4-error-attribution-and-root-cause-analysis)
5. [Stuck Detection and Recovery](#5-stuck-detection-and-recovery)
6. [MineWright Applications](#6-minewright-applications)
7. [Implementation Recommendations](#7-implementation-recommendations)
8. [UI/UX for Debugging](#8-uiux-for-debugging)

---

## 1. Explainable AI (XAI) for Game Agents

### 1.1 Overview

Explainable AI aims to make AI decisions transparent and understandable to humans. For game agents, this means showing players and developers **why** an agent made a specific decision.

### 1.2 Traditional XAI Techniques

| Technique | Description | Applicability to Game Agents |
|-----------|-------------|------------------------------|
| **LIME** | Local interpretable model-agnostic explanations via perturbation | Low - Designed for classification models |
| **SHAP** | Shapley Additive exPlanations using game theory | Low - Focuses on feature importance |
| **Decision Trees** | Highly interpretable branching logic | **High** - Already used in game AI |
| **Utility Systems** | Weighted scoring for action selection | **High** - Explainable by design |
| **Behavior Trees** | Hierarchical decision structures | **High** - Visual and debuggable |

### 1.3 LLM-Specific Explainability

For LLM-powered agents like MineWright, traditional XAI techniques are less relevant. Instead, explainability comes from:

1. **Decision Tracing** - Showing the reasoning chain (Chain of Thought)
2. **Tool Call Inspection** - Displaying which tools/actions were called and why
3. **Context Visibility** - Showing what information the agent considered
4. **Plan Visualization** - Displaying the multi-step plan before execution

**Research Insight:** [ReasonGraph](https://github.com/ZongqianLi/ReasonGraph) is a cutting-edge tool for visualizing LLM reasoning paths in real-time, supporting both sequential (CoT) and tree-based (ToT) reasoning methods.

### 1.4 Game AI Explainability Best Practices

Based on research into game AI development:

> "90% of player frustration with NPCs comes from unclear intent expression. Players don't mind if NPCs make mistakes, but they need to understand *why*."

**Key Principles:**
- **Intent Signaling** - NPCs should communicate their goals before acting
- **Failure Explanation** - When actions fail, explain what went wrong
- **Progress Visualization** - Show progress toward goals
- **Decision Transparency** - Make the decision process inspectable

---

## 2. Debugging Tools and Platforms

### 2.1 LLM Agent Observability Platforms

#### LangSmith (Industry Leader)

LangSmith is the premier platform for debugging LLM applications, created by the LangChain team.

**Core Capabilities:**
- **Full-Chain Tracing** - Records every LLM call, tool invocation, and intermediate step
- **Visual Dashboard** - Flowchart-style visualization of decision paths
- **Performance Metrics** - Latency, token usage, cost tracking per step
- **Evaluation Framework** - Golden datasets, automatic evaluators, regression testing
- **Production Monitoring** - Real-time error tracking and anomaly detection

**Setup:**
```bash
export LANGCHAIN_TRACING_V2=true
export LANGCHAIN_API_KEY=your_key
```

**Key Benefit:** Acts as an "X-ray machine" for AI agents, reducing debugging time from hours to minutes.

**Sources:**
- [LangSmith Agent Monitoring Guide](https://m.toutiao.com/a7609642582695576079/)
- [LangChain Debugging Documentation](https://www.langchain.com.cn/docs/how_to/debugging/)

#### Other Platforms

| Platform | Focus | Key Features |
|----------|-------|--------------|
| **SWE-agent** | Code-editing agents | Trajectory files (.traj), Inspector tool, replay |
| **VoltAgent** | Production agents | OpenTelemetry tracing, session replay, cost tracking |
| **Shannon** | Multi-agent workflows | Time-travel debugging, temporal workflows |
| **debug-gym** (Microsoft) | Interactive debugging | Breakpoints, codebase navigation, runtime inspection |

### 2.2 Game AI Debugging Tools

#### Unity Behavior Tree Visualizer

A powerful tool for drawing real-time graph representations of behavior trees.

**Features:**
- Real-time node status visualization (running/success/failed)
- Execution path highlighting
- Debug gizmos for 3D visualization
- Integration with Unity editor

#### Unreal Engine AI Debugging

Unreal provides comprehensive AI debugging capabilities:

**Built-in Tools:**
- **Behavior Tree Debugger** - Real-time task execution status
- **Unreal Insights** - AI task execution timing
- **EQS Visualization** - Environment Query System debugging
- **Blackboard Inspection** - Monitor AI memory/variables in real-time

**Console Commands:**
```
ai_debug_behavior_tree 1
ai_debug_equipment 1
ShowDebug AI
```

**Best Practices from Unreal AI:**
1. **Visual Debugging** - Make every node's runtime status visible via gizmos
2. **Logging** - Use UE_LOG for state transitions
3. **Blackboard Monitoring** - Watch AI decision variables
4. **Performance Profiling** - Identify expensive nodes

**Sources:**
- [Unity Behavior Tree Visualizer](https://gitcode.com/gh_mirrors/un/UnityBehaviorTreeVisualizer)
- [Unreal AI Practical Guide](https://www.cnblogs.com/apachecn/p/19167931)
- [Behavior Tree Debugging Guide](https://m.blog.csdn.net/LiteProceed/article/details/155931429)

### 2.3 Minecraft AI Debugging Tools

#### MineStudio

A streamlined toolkit for Minecraft AI agent development with modular design.

**Features:**
- Offline/online training optimization
- Extensive documentation
- Modular component design
- Event tracking and logging

#### Steve (YuvDwi)

An autonomous AI agent for Minecraft survival gameplay using Cursor model/LangChain.

**Relevance:**
- Similar architecture to MineWright
- LLM-based planning system
- Cursor model integration

---

## 3. Visualization Approaches

### 3.1 Decision Visualization

#### ReasonGraph: LLM Reasoning Visualization

[ReasonGraph](https://github.com/ZongqianLi/ReasonGraph) is an open-source tool that visualizes LLM reasoning processes.

**Key Features:**
- Real-time rendering of reasoning paths
- Support for sequential (CoT) and tree-based (ToT) methods
- Interactive analysis - click to explore branches
- SVG export for documentation
- 50+ LLM provider support

**Use Cases:**
- Academic research on reasoning methods
- Model debugging - identify inefficiencies
- Education - show AI thinking to students
- Application development - select optimal methods

#### Decision Flow Visualization for Game AI

**Recommended Visualizations for MineWright:**

1. **Plan Tree** - Hierarchical view of tasks and subtasks
   ```
   Build House
   ├── Gather Materials
   │   ├── Mine Wood (x64)
   │   └── Mine Stone (x32)
   ├── Prepare Foundation
   └── Construct Walls
   ```

2. **State Transition Diagram** - Real-time state machine visualization
   ```
   [IDLE] → [PLANNING] → [EXECUTING] → [COMPLETED]
              ↓            ↓
           [FAILED]     [PAUSED]
   ```

3. **Action Queue Preview** - Upcoming actions with priority
   ```
   Queue: [1] Pathfind → [2] Mine → [3] Craft → [4] Place
   ```

4. **Goal Progress Bar** - Visual progress toward current goal
   ```
   ████████░░░░ 60% (6/10 blocks placed)
   ```

### 3.2 In-Game Visualization

#### NPC Intent Expression

Research shows players are more forgiving of AI mistakes when intent is clear.

**Visual Indicators:**
- **Goal Icon** - Above agent head showing current task
- **Progress Ring** - Circular progress around agent
- **Thought Bubble** - Brief text explanation of current action
- **Path Line** - Visualized movement path
- **Target Highlight** - What the agent is interacting with

**Example Minecraft Implementation:**
```java
// Render floating text above agent
renderTextAbove(agent, currentGoal);
// Draw path line to target
drawPathLine(agent.position, targetPosition);
// Highlight target block
highlightBlock(targetBlock, Color.YELLOW);
```

#### Li Auto Autonomous Driving Example

Li Auto's L3 autonomous driving uses AI reasoning visualization technology to intuitively present the thinking process of end-to-end + VLM (Vision-Language Models).

**Key Takeaway:** Production systems are successfully using reasoning visualization to build user trust.

### 3.3 Replay and Time-Travel Debugging

#### Shannon Framework

[Shannon](https://github.com/Kocoro-lab/Shannon) is a multi-agent framework with temporal workflows and time-travel debugging.

**Capabilities:**
- State snapshots at key decision points
- Rollback to any previous state
- Replay execution with different parameters
- What-if scenario analysis

#### Replay Benefits

**Without Replay:**
- Failures are "black boxes"
- Cannot reproduce issues
- No audit trail for multi-agent collaboration
- Work is lost on timeout/failure

**With Replay:**
- Full visibility into every decision step
- Checkpoint recovery for long-running tasks
- Clear handoff points between agents
- Complete audit trail for compliance

---

## 4. Error Attribution and Root Cause Analysis

### 4.1 Multi-Agent Error Attribution (ICML 2025)

**Who&When Dataset** - ICML 2025 Spotlight paper introducing automated failure attribution for multi-agent systems.

**Key Contributions:**
1. **New Problem Definition** - Formalizes "automated failure attribution" as a research task
2. **Dataset** - 127 LLM multi-agent systems with fine-grained annotations:
   - **Who**: Which agent is the "culprit"
   - **When**: At which interaction step the error occurred
   - **Why**: Natural language explanation of the failure cause

**Relevance to MineWright:**
- When a build task fails, identify which crew member caused it
- Track cascading failures across coordinated agents
- Provide natural language explanations for players

### 4.2 Production Error Analysis Approaches

#### Meta-Agent Analysis (2026)

**Concept:** Use another LLM (a "meta Agent") to analyze failed agent logs and find root causes.

**Feedback Loop Process:**
1. Establish baseline agent version
2. Deploy multiple instances and collect execution traces
3. Feed failed logs to LLM with larger context window (e.g., Gemini 1.5 Pro)
4. Improve baseline agent's prompts, tools, or context based on insights

**Debugging Checklist:**
- [ ] Check if tools are missing
- [ ] Verify if prompts are clear enough
- [ ] Ensure API keys/configurations are correct

#### Observability Platform RCA (观测云 2026)

AI Agent acting like an experienced SRE engineer:

**Multi-Path Investigation:**
- Plan A (High confidence): Database timeout
- Plan B (Low confidence): Upstream dependency slowdown
- Plan C (Low confidence): Network gateway failure

**Features:**
- Interactive mind maps showing logical action flow
- Recursive diagnosis with automatic node expansion
- Root cause lock when evidence is found

#### NVIDIA ITelligence

NVIDIA's internal AI agent for IT ticket analysis using Nemotron models.

**Features:**
- Root Cause Analysis (RCA) jobs
- Contextual enrichment
- Real-time executive summaries
- Proactive alerting

### 4.3 Error Attribution in Multi-Agent Systems

Research from [arxiv:2509.23735](https://arxiv.org/html/2509.23735v1) on platform-orchestrated systems:

**Key Findings:**
- Platform-orchestrated systems make root cause localization inherently difficult
- Cascading failures amplify from single root-cause errors
- Multi-agent systems require attribution at the interaction level

**Failure Modes:**
1. **Single Agent Failure** - One agent fails independently
2. **Cascading Failure** - One agent's failure propagates
3. **Coordination Failure** - Agents conflict or deadlock
4. **Platform Failure** - Infrastructure/communication failure

### 4.4 Multi-Agent Patcher Architecture

**Repair Agent Division:**
```python
class MultiAgentPatcher:
    - analysis_agent: AnalysisAgent()
    - root_cause_agent: RootCauseAgent()
    - patch_generation_agent: PatchGenerationAgent()
```

**Three-Phase Process:**
1. **Problem Analysis** - What went wrong?
2. **Root Cause Localization** - Where did it originate?
3. **Patch Generation** - How to fix it?

---

## 5. Stuck Detection and Recovery

### 5.1 Stuck Detection Patterns

#### Duplicate Message Detection

From OpenManus implementation:
```python
def is_stuck(self) -> bool:
    """Check if agent is stuck by detecting duplicate content"""
    # Counts duplicate assistant messages
    # Returns True if duplicates exceed threshold
```

**Handling:** Add prompts to encourage strategy changes

#### Loop Detection

**Solutions:**
1. **Iteration limits** in agent executors (e.g., 20-30 steps max)
2. **Timeout mechanisms** using signal handlers
3. **State tracking** with short-circuit logic
4. **Loop detectors** for cyclic behavior

### 5.2 Timeout Handling Best Practices

#### Production Parameters (Dify Agent)

| Parameter | Recommended Value | Description |
|-----------|------------------|-------------|
| Timeout | 5s | Balance response speed and waiting cost |
| Max Retries | 3 | Avoid infinite loops |
| Strategy | Exponential backoff | Reduce server pressure |

#### Google Agent Design Patterns

**Three-Layer Defense Philosophy:**
1. **Prevention First** - Design to avoid errors
2. **Control Second** - Detect errors early
3. **Recovery After** - Graceful recovery

**Handling Strategies:**
- **Logging** - Record all errors
- **Retry** - Exponential backoff
- **Fallback** - Alternative approaches
- **Graceful Degradation** - Continue with reduced capability
- **Notification** - Alert human operators

**Recovery Strategies:**
- **Rollback** - Return to previous safe state
- **Diagnosis** - Analyze what went wrong
- **Self-Correction** - Fix without intervention
- **Escalation** - Request human help

### 5.3 Stuck Detection for MineWright

**Recommended Implementation:**

```java
public class StuckDetector {
    private static final int STUCK_THRESHOLD = 100; // ticks
    private static final int POSITION_TOLERANCE = 2; // blocks

    private int stuckTicks = 0;
    private BlockPos lastPosition;
    private int loopCount = 0;
    private String lastAction;

    public boolean isStuck(ForemanEntity agent) {
        // Check if position hasn't changed
        BlockPos currentPos = agent.blockPosition();
        if (lastPosition != null &&
            currentPos.distSqr(lastPosition) < POSITION_TOLERANCE) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastPosition = currentPos;
        }

        // Check for action loops
        String currentAction = agent.getCurrentAction();
        if (currentAction != null && currentAction.equals(lastAction)) {
            loopCount++;
        } else {
            loopCount = 0;
            lastAction = currentAction;
        }

        return stuckTicks > STUCK_THRESHOLD || loopCount > 5;
    }
}
```

### 5.4 Recovery Strategies

**When Agent is Stuck:**

1. **Repathfinding** - Calculate new path with obstacle avoidance
2. **Alternative Action** - Try different approach to goal
3. **Request Help** - Ask player or another agent for assistance
4. **Abort Task** - Give up and report failure with explanation
5. **Timeout Escalation** - After N retries, involve human

**Example Recovery Logic:**
```java
if (stuckDetector.isStuck(agent)) {
    if (recoveryAttempts < MAX_RECOVERY) {
        // Try recovery
        agent.repathfind();
        recoveryAttempts++;
    } else {
        // Give up and explain
        agent.sendChatMessage("I'm stuck and can't reach the target. " +
            "The path is blocked or unreachable.");
        agent.abortCurrentTask();
    }
}
```

---

## 6. MineWright Applications

### 6.1 Current Debugging Infrastructure

MineWright already has several debugging components:

**Existing Infrastructure:**
- **AgentStateMachine** - State tracking with event publishing
- **InterceptorChain** - Logging, metrics, event publishing
- **EventBus** - Event-driven architecture for observability
- **ForemanMemory** - Action history and goal tracking
- **AsyncLLMClient** - Non-blocking LLM calls with latency tracking

**LoggingInterceptor** currently logs:
- Action start: `[ACTION START] {description}`
- Action complete: `[ACTION COMPLETE] {description} - Success: {message}`
- Action failed: `[ACTION FAILED] {description} - Reason: {message}`
- Action error: `[ACTION ERROR] {description} - Exception: {exception}`

### 6.2 Debugging Crew Members

#### Common Failure Scenarios

| Scenario | Symptoms | Root Cause | Debugging Approach |
|----------|----------|------------|-------------------|
| **Agent Standing Still** | No movement, idle animation | Pathfinding failure, unreachable target | Check pathfinding result, obstacle map |
| **Agent Repeating Action** | Same action in loop | Task validation failure, precondition not met | Inspect task parameters, world state |
| **Agent Ignoring Command** | No response to command | LLM parse failure, planning error | Check LLM response, prompt context |
| **Agent Wrong Block** | Mining/placing wrong block | Block identification error, context confusion | Inspect world knowledge, block registry |
| **Agent Falling** | Repeated fall damage | Pathfinding without safety checks | Review pathfinding safety logic |
| **Build Failure** | Incomplete structure | Block placement failure, resource shortage | Check inventory, placement validation |

#### Diagnostic Questions

When a crew member fails, these questions help identify the issue:

1. **What was the command?** - Check user input parsing
2. **What was the plan?** - Inspect LLM-generated task list
3. **What is the current state?** - AgentStateMachine state
4. **What action is executing?** - Current action type and parameters
5. **What was the result?** - ActionResult success/failure message
6. **What is in the inventory?** - Resource availability
7. **What is nearby?** - World knowledge, obstacles
8. **How long has it been running?** - Check for timeouts

### 6.3 What is the Crew Member Thinking?

#### LLM Thought Process Visualization

To make agent reasoning transparent:

**Option 1: Chain of Thought Logging**
```java
public class ThoughtLogger {
    public void logThought(String agentId, String thought, String reason) {
        // Log to console
        LOGGER.info("[THOUGHT] {}: {} (reason: {})", agentId, thought, reason);

        // Store in memory for inspection
        memory.addThought(new Thought(thought, reason, Instant.now()));
    }
}
```

**Option 2: Plan Visualization GUI**
```
┌─────────────────────────────────────┐
│ Steve's Current Plan                │
├─────────────────────────────────────┤
│ Goal: Build a small wooden house    │
│                                     │
│ Steps:                              │
│ ✓ 1. Gather wood (x64)             │
│ ✓ 2. Craft planks (x256)           │
│ → 3. Place foundation (10x10)       │
│   4. Build walls (3 high)           │
│   5. Add roof                       │
│   6. Add door and windows           │
└─────────────────────────────────────┘
```

**Option 3: In-Game Floating Text**
```java
// Render above agent's head
String statusText = String.format("%s: %s (%d%%)",
    currentGoal, currentAction, progressPercent());
renderFloatingText(agent, statusText);
```

### 6.4 How to Fix Stuck Agents

#### Recovery Workflow

```
1. Detect Stuck State
   ├─ Position unchanged for N ticks
   ├─ Same action repeating
   └─ Action timeout

2. Diagnose Cause
   ├─ Check pathfinding result
   ├─ Verify target block exists
   ├─ Inspect inventory
   └─ Validate world state

3. Attempt Recovery
   ├─ Repathfind with new algorithm
   ├─ Try alternative approach
   ├─ Request player intervention
   └─ Abort and explain failure

4. Learn from Failure
   ├─ Log failure pattern
   ├─ Update world knowledge
   └─ Avoid similar situations
```

#### Recovery Command

Add a debug command for manual intervention:

```
/minewright unstuck <name> - Force agent to replan
/minewright retry <name> - Retry current action
/minewright skip <name> - Skip current task
/minewright explain <name> - Get detailed state explanation
```

---

## 7. Implementation Recommendations

### 7.1 Priority 1: Enhanced Logging Infrastructure

**Goal:** Make all agent decisions observable and replayable.

**Implementation:**

```java
/**
 * Comprehensive trace logger for agent debugging.
 * Captures all decisions, actions, and observations for replay.
 */
public class AgentTraceLogger {
    private final List<TraceEvent> trace = new ArrayList<>();

    public void logDecision(String agentId, String decision, Map<String, Object> context) {
        TraceEvent event = new TraceEvent(
            Instant.now(),
            agentId,
            "DECISION",
            decision,
            context
        );
        trace.add(event);
        LOGGER.debug("[TRACE] [{}] DECISION: {}", agentId, decision);
    }

    public void logObservation(String agentId, String observation) {
        TraceEvent event = new TraceEvent(
            Instant.now(),
            agentId,
            "OBSERVATION",
            observation,
            Map.of()
        );
        trace.add(event);
    }

    public void logAction(String agentId, String action, ActionResult result) {
        TraceEvent event = new TraceEvent(
            Instant.now(),
            agentId,
            result.isSuccess() ? "ACTION_SUCCESS" : "ACTION_FAILURE",
            action,
            Map.of("result", result.getMessage())
        );
        trace.add(event);
    }

    public List<TraceEvent> getTrace() {
        return Collections.unmodifiableList(trace);
    }

    public void saveToFile(String agentId) {
        String filename = String.format("debug_trace_%s_%d.json",
            agentId, Instant.now().toEpochMilli());
        // Write trace to file for offline analysis
    }
}
```

**Integration Points:**
- Wrap all LLM calls with trace logging
- Log state transitions in AgentStateMachine
- Log all action starts and completions
- Log world observations

### 7.2 Priority 2: In-Game Debugging HUD

**Goal:** Real-time visualization of agent state.

**Implementation:**

```java
/**
 * Debug overlay renderer for agent inspection.
 * Press F6 to toggle debug view for nearest agent.
 */
public class AgentDebugOverlay {
    public void render(PoseStack poseStack, ForemanEntity agent) {
        int x = 10;
        int y = 10;

        // Agent Info
        drawString(poseStack, "Agent: " + agent.getSteveName(), x, y, 0xFFFFFF);
        y += 12;
        drawString(poseStack, "State: " + getStateMachine().getCurrentState(), x, y, getStateColor());
        y += 12;

        // Current Goal
        drawString(poseStack, "Goal: " + getCurrentGoal(), x, y, 0xAAAAAA);
        y += 12;

        // Current Action
        if (currentAction != null) {
            drawString(poseStack, "Action: " + currentAction.getDescription(), x, y, 0x00FF00);
            y += 12;

            // Action Progress
            int progress = getCurrentActionProgress();
            drawProgressBar(poseStack, x, y, progress);
            y += 14;
        }

        // Task Queue
        drawString(poseStack, "Queue: " + taskQueue.size() + " tasks", x, y, 0xAAAAAA);
        y += 12;
        for (Task task : taskQueue) {
            drawString(poseStack, "  - " + task.getAction(), x, y, 0x888888);
            y += 10;
        }

        // Inventory Summary
        drawString(poseStack, "Inventory:", x, y, 0xAAAAAA);
        y += 12;
        // Draw key items...

        // Stuck Warning
        if (stuckDetector.isStuck(agent)) {
            drawString(poseStack, "⚠ STUCK DETECTED", x, y + 20, 0xFF0000);
        }
    }
}
```

**Key Controls:**
- **F6** - Toggle debug overlay
- **F7** - Cycle through agents
- **F8 + Click** - Inspect specific agent
- **Ctrl+F6** - Export trace to file

### 7.3 Priority 3: Stuck Detection and Recovery

**Goal:** Automatically detect and recover from stuck states.

**Implementation:**

```java
/**
 * Monitors agent for stuck conditions and triggers recovery.
 */
public class StuckDetectionSystem {
    private final Map<String, StuckState> agentStates = new ConcurrentHashMap<>();

    public void tick(ForemanEntity agent) {
        String agentId = agent.getSteveName();
        StuckState state = agentStates.computeIfAbsent(agentId, id -> new StuckState());

        // Check position stagnation
        BlockPos currentPos = agent.blockPosition();
        if (currentPos.equals(state.lastPosition)) {
            state.positionStuckTicks++;
        } else {
            state.positionStuckTicks = 0;
            state.lastPosition = currentPos;
        }

        // Check action repetition
        String currentAction = getCurrentActionDescription(agent);
        if (currentAction.equals(state.lastAction)) {
            state.actionRepeatCount++;
        } else {
            state.actionRepeatCount = 0;
            state.lastAction = currentAction;
        }

        // Check for stuck condition
        if (isStuck(state)) {
            handleStuckAgent(agent, state);
        }
    }

    private boolean isStuck(StuckState state) {
        return state.positionStuckTicks > 100 ||
               state.actionRepeatCount > 5;
    }

    private void handleStuckAgent(ForemanEntity agent, StuckState state) {
        String agentId = agent.getSteveName();

        LOGGER.warn("[StuckDetection] Agent '{}' is stuck! " +
            "Position: {} ticks, Action: {} repeats",
            agentId, state.positionStuckTicks, state.actionRepeatCount);

        // Attempt recovery based on stuck reason
        if (state.actionRepeatCount > 5) {
            // Action loop - try alternative
            agent.sendChatMessage("I'm having trouble with this task. " +
                "Let me try a different approach.");
            agent.getExecutor().skipCurrentTask();
        } else if (state.positionStuckTicks > 100) {
            // Position stuck - repathfind
            agent.sendChatMessage("I can't reach my target. " +
                "Trying to find a new path...");
            agent.getExecutor().repathfindCurrentAction();
        }

        // Increment recovery counter
        state.recoveryAttempts++;

        // If too many recovery attempts, abort
        if (state.recoveryAttempts > 3) {
            LOGGER.error("[StuckDetection] Agent '{}' exceeded recovery attempts. Aborting.", agentId);
            agent.sendChatMessage("I'm unable to complete this task. " +
                "It might be unreachable or I don't have the right tools.");
            agent.getExecutor().stopCurrentAction();
            state.reset();
        }
    }

    private static class StuckState {
        BlockPos lastPosition;
        int positionStuckTicks = 0;
        String lastAction;
        int actionRepeatCount = 0;
        int recoveryAttempts = 0;

        void reset() {
            positionStuckTicks = 0;
            actionRepeatCount = 0;
            recoveryAttempts = 0;
        }
    }
}
```

### 7.4 Priority 4: Error Attribution System

**Goal:** Identify which component caused a failure.

**Implementation:**

```java
/**
 * Tracks failures and attributes them to specific components.
 */
public class ErrorAttributionSystem {
    private final List<FailureEvent> failureLog = new ArrayList<>();

    public void recordFailure(String agentId, String failureType,
                             String component, String description,
                             Map<String, Object> context) {
        FailureEvent event = new FailureEvent(
            Instant.now(),
            agentId,
            failureType,
            component,
            description,
            context
        );
        failureLog.add(event);

        LOGGER.error("[ErrorAttribution] [{}] [{}] Component '{}' failed: {}",
            agentId, failureType, component, description);
    }

    public String generateFailureReport(String agentId) {
        StringBuilder report = new StringBuilder();
        report.append("Failure Report for ").append(agentId).append(":\n\n");

        // Group by component
        Map<String, List<FailureEvent>> byComponent = failureLog.stream()
            .filter(e -> e.agentId.equals(agentId))
            .collect(Collectors.groupingBy(e -> e.component));

        for (Map.Entry<String, List<FailureEvent>> entry : byComponent.entrySet()) {
            String component = entry.getKey();
            List<FailureEvent> failures = entry.getValue();

            report.append("Component: ").append(component).append("\n");
            report.append("  Failures: ").append(failures.size()).append("\n");
            report.append("  Most Recent: ").append(failures.get(0).description).append("\n\n");
        }

        return report.toString();
    }

    public List<String> identifyCulprits(String agentId) {
        // Find components with most failures
        return failureLog.stream()
            .filter(e -> e.agentId.equals(agentId))
            .collect(Collectors.groupingBy(e -> e.component, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .limit(3)
            .toList();
    }
}
```

**Usage:**
```java
// When LLM parsing fails
errorAttribution.recordFailure(agent.getSteveName(),
    "PARSE_FAILURE", "LLM", "Failed to parse response", Map.of("raw", response));

// When pathfinding fails
errorAttribution.recordFailure(agent.getSteveName(),
    "PATH_FAILURE", "PATHFINDING", "No path found", Map.of("target", targetPos));

// When action fails
errorAttribution.recordFailure(agent.getSteveName(),
    "ACTION_FAILURE", "BLOCK_PLACEMENT", "Block obstructed", Map.of("pos", pos));
```

### 7.5 Priority 5: Plan Visualization GUI

**Goal:** Show agent's plan and reasoning to player.

**Implementation:**

```java
/**
 * GUI screen displaying agent's current plan and reasoning.
 */
public class AgentPlanScreen extends Screen {
    private final ForemanEntity agent;

    public AgentPlanScreen(ForemanEntity agent) {
        super(Component.literal("Agent Plan"));
        this.agent = agent;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);

        // Title
        drawCenteredString(poseStack, font, agent.getSteveName() + "'s Plan",
            width / 2, 20, 0xFFFFFF);

        int y = 50;

        // Current Goal
        drawString(poseStack, font, "Goal:", 50, y, 0xAAFFFF);
        y += 15;
        drawString(poseStack, font, agent.getCurrentGoal(), 70, y, 0xFFFFFF);
        y += 30;

        // Reasoning (if available from LLM)
        String reasoning = agent.getMemory().getLastReasoning();
        if (reasoning != null) {
            drawString(poseStack, font, "Reasoning:", 50, y, 0xAAFFFF);
            y += 15;
            drawWrappedString(poseStack, font, reasoning, 70, y, width - 100, 0xCCCCCC);
            y += 60;
        }

        // Task List
        drawString(poseStack, font, "Tasks:", 50, y, 0xAAFFFF);
        y += 15;

        Queue<Task> tasks = agent.getExecutor().getTaskQueue();
        int index = 0;
        for (Task task : tasks) {
            String icon = index == 0 ? "→" : " ";
            String taskText = String.format("%s %s: %s", icon, task.getAction(), task.getParams());
            int color = index == 0 ? 0x00FF00 : 0x888888;
            drawString(poseStack, font, taskText, 70, y, color);
            y += 12;
            index++;
            if (index > 10) {
                drawString(poseStack, font, "... and " + (tasks.size() - 10) + " more",
                    70, y, 0x888888);
                break;
            }
        }

        // Action Progress
        if (agent.getExecutor().getCurrentAction() != null) {
            y += 20;
            drawString(poseStack, font, "Current Action:", 50, y, 0xAAFFFF);
            y += 15;
            String actionDesc = agent.getExecutor().getCurrentAction().getDescription();
            drawString(poseStack, font, actionDesc, 70, y, 0xFFFFFF);
            y += 20;

            // Progress bar
            int progress = agent.getExecutor().getCurrentActionProgress();
            drawProgressBar(poseStack, 70, y, 200, 10, progress);
        }
    }
}
```

**Trigger:** Press K while looking at agent, or click "View Plan" button in Foreman Office GUI.

---

## 8. UI/UX for Debugging

### 8.1 Debug Interface Design

#### Foreman Office GUI Enhancements

Add debugging section to existing GUI:

```
┌─────────────────────────────────────────────────────────┐
│ MineWright Foreman Office                    [Minimize] │
├─────────────────────────────────────────────────────────┤
│ Crew Members                                            │
│ ┌─────────────────────────────────────────────────┐   │
│ │ Steve                                              │   │
│ │ State: Executing | Goal: Build house              │   │
│ │ Progress: ████████░░ 80%                         │   │
│ │ [View Plan] [Explain] [Pause] [Cancel]            │   │
│ └─────────────────────────────────────────────────┘   │
│ ┌─────────────────────────────────────────────────┐   │
│ │ Alex                                               │   │
│ │ State: Idle | Last task: Completed                │   │
│ │ [Assign Task]                                      │   │
│ └─────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│ Debug Console                                           │
│ ┌─────────────────────────────────────────────────┐   │
│ │ [14:32:01] Steve: Starting action: Mine oak log │   │
│ │ [14:32:15] Steve: Action complete: +1 oak log   │   │
│ │ [14:32:20] Steve: Starting action: Pathfind...  │   │
│ │ [14:32:45] ⚠ Steve: Pathfind failed (no path)  │   │
│ │ > [Retry] [Skip] [Explain]                       │   │
│ └─────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│ [View Traces] [Export Logs] [Debug Settings]           │
└─────────────────────────────────────────────────────────┘
```

### 8.2 In-World Visualization

#### Agent Status Indicators

Render above agent head:

```
        ┌─────────────────────┐
        │ Building House (60%)│ ← Status text
        │ ████████░░░░        │ ← Mini progress bar
        │ [→]                │ ← Current action icon
        Steve                │ ← Agent name
        ══════════════════════
```

**Color Coding:**
- **Green** - Executing normally
- **Yellow** - Planning/thinking
- **Red** - Failed/stuck
- **Blue** - Idle/following

#### Path Visualization

Draw agent's planned path:
- **Solid line** - Current path
- **Dotted line** - Alternative path being considered
- **X mark** - Target destination
- **Red circle** - Obstacle or blocked area

#### Target Highlighting

Highlight what agent is interacting with:
- **Yellow outline** - Current target block
- **Blue outline** - Path waypoints
- **Green outline** - Successfully completed

### 8.3 Explanation Tooltips

When player hovers over agent or GUI elements, show helpful explanations:

**Tooltip Examples:**
- **State: PLANNING** - "Agent is waiting for AI to generate a plan. This usually takes 5-10 seconds."
- **State: EXECUTING** - "Agent is working on the current task. See task list for details."
- **State: STUCK** - "Agent cannot reach target or complete action. Try moving obstacles or giving new command."

### 8.4 Error Messages

**Principles for Good Error Messages:**

1. **What happened?** - Clear description of failure
2. **Why did it happen?** - Root cause explanation
3. **What can I do?** - Suggested recovery actions

**Bad Example:**
```
Action failed
```

**Good Example:**
```
Steve couldn't place the block because:
→ The target location is obstructed by leaves
→ Steve needs a path to the block placement location

Try:
- Clear the leaves in the way
- Move Steve closer to the target
- Use a different building plan
```

### 8.5 Debug Commands

Add player commands for debugging:

```
/minewright debug <name> - Toggle debug overlay for agent
/minewright trace <name> - Enable trace logging
/minewright explain <name> - Get detailed state explanation
/minewright unstuck <name> - Force recovery from stuck state
/minewright inspect - Show block info agent is targeting
/minewright export <name> - Export debug trace to file
```

---

## 9. Conclusion and Next Steps

### 9.1 Summary

This research identified key techniques and tools for debugging and explaining AI game agent behavior:

**Key Technologies:**
- **LangSmith** - Gold standard for LLM agent tracing
- **ReasonGraph** - State-of-the-art reasoning visualization
- **Unity/Unreal Debuggers** - Mature game AI debugging tools
- **Who&When Dataset** - Emerging multi-agent error attribution research

**Critical Capabilities for MineWright:**
1. **Trace Logging** - Record all decisions and actions
2. **State Visualization** - Real-time debugging HUD
3. **Stuck Detection** - Automatic recovery from failures
4. **Error Attribution** - Identify failing components
5. **Plan Display** - Show agent reasoning to player

### 9.2 Recommended Implementation Order

**Phase 1: Foundation** (1-2 weeks)
- [ ] Implement AgentTraceLogger for comprehensive logging
- [ ] Add debug overlay renderer (F6 toggle)
- [ ] Export traces to JSON for offline analysis

**Phase 2: Intelligence** (2-3 weeks)
- [ ] Implement StuckDetectionSystem with recovery
- [ ] Add ErrorAttributionSystem for failure tracking
- [ ] Create failure report generation

**Phase 3: User Experience** (2-3 weeks)
- [ ] Build plan visualization GUI
- [ ] Add in-world status indicators
- [ ] Implement explanation tooltips
- [ ] Add debug commands

**Phase 4: Advanced Features** (3-4 weeks)
- [ ] Implement replay/time-travel debugging
- [ ] Add multi-agent coordination visualization
- [ ] Integrate with external observability platform
- [ ] Build analytics dashboard

### 9.3 Success Metrics

**Developer Experience:**
- Mean time to diagnose issue: < 2 minutes (down from ~10 minutes)
- Bug reproduction rate: 95%+ with trace replay
- Failure attribution accuracy: 80%+

**Player Experience:**
- Agent failure understanding: 90%+ of failures explained clearly
- Stuck agent recovery: 70%+ automatic recovery
- Player trust: Qualitative feedback on transparency

### 9.4 Open Questions

1. **Observability Integration** - Should we integrate with LangSmith or build custom?
2. **Performance Impact** - How much overhead from tracing is acceptable?
3. **Privacy** - Should players be able to export traces?
4. **Multi-Agent Attribution** - How to handle cascading failures?
5. **LLM Transparency** - Should we expose raw LLM reasoning or summarize?

---

## References and Sources

### Academic Papers
- [Diagnosing Failure Root Causes in Platform-Orchestrated Systems](https://arxiv.org/html/2509.23735v1) (arXiv, Sep 2025)
- [Who&When Dataset - ICML 2025 Spotlight](https://m.163.com/money/article/K12HJL5P0511ABV6.html) - Multi-agent failure attribution
- [Where LLM Agents Fail and How They Can Learn](https://www.researchgate.net/publication/396048725_Where_LLM_Agents_Fail_and_How_They_can_Learn_From_Failures) (ResearchGate, Sep 2025)

### Tools and Platforms
- [LangSmith Agent Tracing](https://m.toutiao.com/a7609642582695576079/) - LLM observability platform
- [ReasonGraph - LLM Reasoning Visualization](https://github.com/ZongqianLi/ReasonGraph) - Reasoning path visualization
- [Unity Behavior Tree Visualizer](https://gitcode.com/gh_mirrors/un/UnityBehaviorTreeVisualizer) - Game AI debugging
- [Unreal AI Practical Guide](https://www.cnblogs.com/apachecn/p/19167931) - Game AI debugging tools
- [SWE-agent - Code Editing Agent](https://blog.csdn.net/gitblog_00824/article/details/151233522) - Replay and inspection
- [VoltAgent Platform](https://github.com/VoltAgent/ai-agent-platform) - Production observability
- [Shannon Multi-Agent Framework](https://github.com/Kocoro-lab/Shannon) - Time-travel debugging

### Game AI Resources
- [Behavior Tree Debugging Guide](https://m.blog.csdn.net/LiteProceed/article/details/155931429) - Game AI debugging patterns
- [MineStudio - Minecraft AI Toolkit](https://blog.csdn.net/gitblog_00007/article/details/147293137) - Minecraft AI development
- [MineWright AI Agent](https://github.com/YuvDwi/Steve) - Similar Minecraft agent project

### Articles and Blog Posts
- [AI Agent状态管理与State Replay深度指南](https://www.toutiao.com/a7589460483040608809/) - State management and replay
- [拆解 AI Agent 黑盒：从日志到异常定位的全链路调试技巧](https://cloud.tencent.com/developer/article/2606862) - Full-stack debugging
- [Why Your AI Agent Keeps Making Mistakes](https://zhidao.baidu.com/question/1652666912209234380.html) - Production debugging principles
- [AI Agent停止策略详解](https://m.blog.csdn.net/2401_85343303/article/details/154444674) - Stop strategies and stuck detection
- [Observability 2026 Roadmap](https://hea.china.com/articles/20260109/202601091794416.html) - AI-powered RCA

### Additional Reading
- [AIX360 Toolkit](https://m.blog.csdn.net/gitblog_00296/article/details/145071874) - IBM AI explainability
- [Debug-Gym - Microsoft Research](https://www.x-mol.com/paper/1905780342484348928) - Interactive debugging environment
- [AI与游戏深度融合引热议](https://post.g.smzdm.com/p/awm25zmp/) - Game AI industry trends

---

**Document Version:** 1.0
**Last Updated:** 2025-02-27
**Next Review:** 2025-03-31

---

*This research report was generated by Claude (Orchestrator Agent) based on comprehensive web research and analysis of the MineWright codebase.*
