# AI Debugging Implementation - Quick Reference Guide

**Project:** MineWright (MineWright AI)
**Date:** 2025-02-27
**Companion to:** RESEARCH_AI_DEBUGGING.md

---

## Quick Start: Debugging Features

### Phase 1: Enhanced Logging (Priority 1)

**File:** `src/main/java/com/minewright/debug/AgentTraceLogger.java`

```java
package com.minewright.debug;

import com.minewright.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.concurrent.ConcurrentHashMap;

/**
 * Comprehensive trace logger for agent debugging.
 * Captures all decisions, actions, and observations for replay.
 */
public class AgentTraceLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentTraceLogger.class);
    private static final AgentTraceLogger INSTANCE = new AgentTraceLogger();

    private final Map<String, List<TraceEvent>> agentTraces = new ConcurrentHashMap<>();

    public static AgentTraceLogger getInstance() {
        return INSTANCE;
    }

    public void logDecision(String agentId, String decision, Map<String, Object> context) {
        TraceEvent event = new TraceEvent(
            Instant.now(),
            agentId,
            "DECISION",
            decision,
            context
        );
        addEvent(agentId, event);
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
        addEvent(agentId, event);
    }

    public void logLLMCall(String agentId, String prompt, String response, long latencyMs) {
        Map<String, Object> context = new HashMap<>();
        context.put("prompt", prompt);
        context.put("response", response);
        context.put("latencyMs", latencyMs);
        context.put("promptLength", prompt.length());
        context.put("responseLength", response.length());

        TraceEvent event = new TraceEvent(
            Instant.now(),
            agentId,
            "LLM_CALL",
            "Task planning request",
            context
        );
        addEvent(agentId, event);
    }

    public void logAction(String agentId, String action, ActionResult result) {
        Map<String, Object> context = new HashMap<>();
        context.put("result", result.getMessage());
        context.put("success", result.isSuccess());
        context.put("requiresReplanning", result.requiresReplanning());

        TraceEvent event = new TraceEvent(
            Instant.now(),
            agentId,
            result.isSuccess() ? "ACTION_SUCCESS" : "ACTION_FAILURE",
            action,
            context
        );
        addEvent(agentId, event);
    }

    public void logStateTransition(String agentId, String from, String to, String reason) {
        Map<String, Object> context = new HashMap<>();
        context.put("from", from);
        context.put("to", to);
        context.put("reason", reason);

        TraceEvent event = new TraceEvent(
            Instant.now(),
            agentId,
            "STATE_TRANSITION",
            from + " -> " + to,
            context
        );
        addEvent(agentId, event);
    }

    public void logError(String agentId, String component, String error, Exception exception) {
        Map<String, Object> context = new HashMap<>();
        context.put("component", component);
        context.put("exceptionType", exception.getClass().getSimpleName());
        context.put("exceptionMessage", exception.getMessage());

        TraceEvent event = new TraceEvent(
            Instant.now(),
            agentId,
            "ERROR",
            error,
            context
        );
        addEvent(agentId, event);
    }

    private void addEvent(String agentId, TraceEvent event) {
        agentTraces.computeIfAbsent(agentId, id -> new ArrayList<>()).add(event);
    }

    public List<TraceEvent> getTrace(String agentId) {
        return Collections.unmodifiableList(agentTraces.getOrDefault(agentId, List.of()));
    }

    public void clearTrace(String agentId) {
        agentTraces.remove(agentId);
    }

    public void saveToFile(String agentId) {
        List<TraceEvent> trace = getTrace(agentId);
        String filename = String.format("debug_trace_%s_%d.json",
            agentId.replace(" ", "_"), Instant.now().toEpochMilli());

        // Simple JSON serialization
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        for (int i = 0; i < trace.size(); i++) {
            TraceEvent event = trace.get(i);
            json.append("  {\n");
            json.append("    \"timestamp\": \"").append(event.timestamp).append("\",\n");
            json.append("    \"agentId\": \"").append(event.agentId).append("\",\n");
            json.append("    \"type\": \"").append(event.type).append("\",\n");
            json.append("    \"description\": \"").append(event.description).append("\",\n");
            json.append("    \"context\": {}\n");
            json.append("  }");
            if (i < trace.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("]\n");

        // In production, write to file
        LOGGER.info("Trace saved to: {}", filename);
    }

    public record TraceEvent(
        Instant timestamp,
        String agentId,
        String type,
        String description,
        Map<String, Object> context
    ) {}
}
```

---

### Phase 2: Stuck Detection (Priority 2)

**File:** `src/main/java/com/minewright/debug/StuckDetectionSystem.java`

```java
package com.minewright.debug;

import com.minewright.MineWrightMod;
import com.minewright.action.ActionExecutor;
import com.minewright.entity.ForemanEntity;
import com.minewright.action.actions.BaseAction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Monitors agents for stuck conditions and triggers recovery.
 */
public class StuckDetectionSystem {
    private static final Logger LOGGER = MineWrightMod.LOGGER;
    private static final StuckDetectionSystem INSTANCE = new StuckDetectionSystem();

    private final Map<String, StuckState> agentStates = new ConcurrentHashMap<>();

    // Configuration
    private static final int POSITION_STUCK_TICKS = 100;  // ~5 seconds
    private static final int ACTION_REPEAT_THRESHOLD = 5;
    private static final int MAX_RECOVERY_ATTEMPTS = 3;
    private static final double POSITION_TOLERANCE = 4.0;  // blocks

    public static StuckDetectionSystem getInstance() {
        return INSTANCE;
    }

    public void tick(ForemanEntity agent) {
        String agentId = agent.getSteveName();
        StuckState state = agentStates.computeIfAbsent(agentId, id -> new StuckState());

        // Check position stagnation
        BlockPos currentPos = agent.blockPosition();
        if (state.lastPosition != null) {
            double distance = currentPos.distSqr(state.lastPosition);
            if (distance < POSITION_TOLERANCE) {
                state.positionStuckTicks++;
            } else {
                state.positionStuckTicks = 0;
            }
        }
        state.lastPosition = currentPos;

        // Check action repetition
        ActionExecutor executor = agent.getExecutor();
        BaseAction currentAction = getCurrentAction(executor);
        String currentActionDesc = currentAction != null ? currentAction.getDescription() : "idle";

        if (currentActionDesc.equals(state.lastAction)) {
            state.actionRepeatCount++;
        } else {
            state.actionRepeatCount = 0;
            state.lastAction = currentActionDesc;
        }

        // Check for stuck condition
        if (isStuck(state)) {
            handleStuckAgent(agent, state);
        }
    }

    private boolean isStuck(StuckState state) {
        return state.positionStuckTicks > POSITION_STUCK_TICKS ||
               state.actionRepeatCount > ACTION_REPEAT_THRESHOLD;
    }

    private void handleStuckAgent(ForemanEntity agent, StuckState state) {
        String agentId = agent.getSteveName();

        if (state.isHandlingStuck) {
            return;  // Already handling
        }

        state.isHandlingStuck = true;
        state.recoveryAttempts++;

        LOGGER.warn("[StuckDetection] Agent '{}' is stuck! " +
            "Position: {} ticks, Action: {} repeats, Recovery attempt: {}",
            agentId, state.positionStuckTicks, state.actionRepeatCount, state.recoveryAttempts);

        // Attempt recovery based on stuck reason
        if (state.actionRepeatCount > ACTION_REPEAT_THRESHOLD) {
            // Action loop - try skipping
            handleActionLoop(agent, state);
        } else if (state.positionStuckTicks > POSITION_STUCK_TICKS) {
            // Position stuck - repathfind
            handlePositionStuck(agent, state);
        }

        // If too many recovery attempts, abort
        if (state.recoveryAttempts >= MAX_RECOVERY_ATTEMPTS) {
            abortAgent(agent, state);
        }

        state.isHandlingStuck = false;
    }

    private void handleActionLoop(ForemanEntity agent, StuckState state) {
        String agentId = agent.getSteveName();

        agent.sendChatMessage(Component.literal(
            "I'm having trouble with this task. Let me try skipping it."
        ));

        // Log the stuck detection
        AgentTraceLogger.getInstance().logDecision(agentId,
            "STUCK_DETECTED: Action loop detected, skipping current action",
            Map.of(
                "repeatCount", state.actionRepeatCount,
                "action", state.lastAction
            )
        );

        // Skip current task
        agent.getExecutor().skipCurrentTask();
        state.reset();
    }

    private void handlePositionStuck(ForemanEntity agent, StuckState state) {
        String agentId = agent.getSteveName();

        agent.sendChatMessage(Component.literal(
            "I'm stuck and can't reach my target. Trying to find a new path..."
        ));

        // Log the stuck detection
        AgentTraceLogger.getInstance().logDecision(agentId,
            "STUCK_DETECTED: Position stuck, attempting repathfinding",
            Map.of(
                "stuckTicks", state.positionStuckTicks,
                "position", state.lastPosition.toString()
            )
        );

        // Trigger repathfinding (implementation depends on pathfinding system)
        agent.getExecutor().repathfindCurrentAction();
        state.reset();
    }

    private void abortAgent(ForemanEntity agent, StuckState state) {
        String agentId = agent.getSteveName();

        LOGGER.error("[StuckDetection] Agent '{}' exceeded recovery attempts. Aborting task.", agentId);

        agent.sendChatMessage(Component.literal(
            "I'm unable to complete this task. It might be unreachable or I don't have the right tools. " +
            "Please help me or give me a different task."
        ));

        // Log the abort
        AgentTraceLogger.getInstance().logError(agentId,
            "StuckDetection",
            "Agent stuck after " + state.recoveryAttempts + " recovery attempts",
            new RuntimeException("Stuck agent abort")
        );

        agent.getExecutor().stopCurrentAction();
        state.reset();
    }

    public void resetAgent(String agentId) {
        agentStates.remove(agentId);
    }

    private BaseAction getCurrentAction(ActionExecutor executor) {
        try {
            // Use reflection or add getter to ActionExecutor
            return null;  // Implement based on actual API
        } catch (Exception e) {
            return null;
        }
    }

    private static class StuckState {
        BlockPos lastPosition;
        int positionStuckTicks = 0;
        String lastAction = "";
        int actionRepeatCount = 0;
        int recoveryAttempts = 0;
        boolean isHandlingStuck = false;

        void reset() {
            positionStuckTicks = 0;
            actionRepeatCount = 0;
            // Don't reset recoveryAttempts to track total issues
        }
    }

    // Methods to add to ActionExecutor:
    // - public void skipCurrentTask()
    // - public void repathfindCurrentAction()
}
```

---

### Phase 3: Debug Overlay (Priority 3)

**File:** `src/main/java/com/minewright/client/AgentDebugOverlay.java`

```java
package com.minewright.client;

import com.minewright.entity.ForemanEntity;
import com.minewright.execution.AgentState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Debug overlay renderer for agent inspection.
 * Press F6 to toggle debug view for nearest agent.
 */
public class AgentDebugOverlay {
    private static final AgentDebugOverlay INSTANCE = new AgentDebugOverlay();
    private boolean enabled = false;
    private ForemanEntity targetAgent;

    public static AgentDebugOverlay getInstance() {
        return INSTANCE;
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) {
            findTargetAgent();
        }
    }

    public void cycleTarget() {
        if (!enabled) return;
        findNextAgent();
    }

    private void findTargetAgent() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 playerPos = mc.player.position();
        ForemanEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        List<ForemanEntity> agents = mc.level.getEntitiesOfClass(
            ForemanEntity.class,
            mc.player.getBoundingBox().inflate(50)
        );

        for (ForemanEntity agent : agents) {
            double dist = agent.position().distanceToSqr(playerPos);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = agent;
            }
        }

        targetAgent = nearest;
    }

    private void findNextAgent() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        List<ForemanEntity> agents = mc.level.getEntitiesOfClass(
            ForemanEntity.class,
            mc.player.getBoundingBox().inflate(50)
        );

        if (agents.isEmpty()) {
            targetAgent = null;
            return;
        }

        int currentIndex = targetAgent != null ? agents.indexOf(targetAgent) : -1;
        int nextIndex = (currentIndex + 1) % agents.size();
        targetAgent = agents.get(nextIndex);
    }

    public void render(PoseStack poseStack, float partialTick) {
        if (!enabled || targetAgent == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        // Update target if it was removed
        if (!targetAgent.isAlive()) {
            findTargetAgent();
            return;
        }

        int x = 10;
        int y = 10;
        int lineHeight = 12;

        // Background
        int boxWidth = 300;
        int boxHeight = 200;
        fillGradient(poseStack, x - 2, y - 2, x + boxWidth, y + boxHeight, 0xC0101010, 0xD0101010);

        // Title
        drawString(poseStack, font, "Agent Debug: " + targetAgent.getSteveName(),
            x, y, 0xFFFFFF);
        y += lineHeight + 4;

        // Basic Info
        drawString(poseStack, font, "State: " + getStateString(),
            x, y, getStateColor());
        y += lineHeight;

        String goal = targetAgent.getMemory().getCurrentGoal();
        drawString(poseStack, font, "Goal: " + (goal.isEmpty() ? "(none)" : goal),
            x, y, 0xAAAAAA);
        y += lineHeight + 4;

        // Current Action
        // Note: Need to expose current action through ActionExecutor
        String currentAction = getCurrentActionString();
        if (!currentAction.isEmpty()) {
            drawString(poseStack, font, "Action: " + currentAction,
                x, y, 0x00FF00);
            y += lineHeight;

            // Progress bar
            int progress = getCurrentActionProgress();
            drawProgressBar(poseStack, x, y, 200, 6, progress);
            y += 10;
        }

        // Task Queue
        int queueSize = getTaskQueueSize();
        drawString(poseStack, font, "Queue: " + queueSize + " tasks",
            x, y, 0xAAAAAA);
        y += lineHeight;

        // Show first few tasks
        List<String> taskList = getTaskList();
        for (int i = 0; i < Math.min(5, taskList.size()); i++) {
            drawString(poseStack, font, "  " + (i + 1) + ". " + taskList.get(i),
                x, y, 0x888888);
            y += 10;
        }
        if (taskList.size() > 5) {
            drawString(poseStack, font, "  ... and " + (taskList.size() - 5) + " more",
                x, y, 0x888888);
            y += 10;
        }

        // Instructions
        y += 8;
        drawString(poseStack, font, "F6: Toggle | F7: Cycle agent | F8: Export trace",
            x, y, 0x666666);
    }

    private String getStateString() {
        // Get state from state machine
        return "EXECUTING";  // Placeholder
    }

    private int getStateColor() {
        AgentState state = AgentState.EXECUTING;  // Get from actual state machine
        return switch (state) {
            case IDLE -> 0xAAAAAA;
            case PLANNING -> 0xFFFF00;
            case EXECUTING -> 0x00FF00;
            case PAUSED -> 0xFFAA00;
            case FAILED -> 0xFF0000;
            default -> 0xFFFFFF;
        };
    }

    private String getCurrentActionString() {
        // Get from ActionExecutor
        return "Mining oak log";  // Placeholder
    }

    private int getCurrentActionProgress() {
        // Get from ActionExecutor
        return 60;  // Placeholder
    }

    private int getTaskQueueSize() {
        // Get from ActionExecutor
        return 8;  // Placeholder
    }

    private List<String> getTaskList() {
        // Get from ActionExecutor
        return List.of(
            "Mine oak log",
            "Craft oak planks",
            "Place oak planks",
            "Pathfind to site",
            "Place cobblestone"
        );  // Placeholder
    }

    private void drawString(PoseStack poseStack, Font font, String text, int x, int y, int color) {
        font.drawInBatch(text, x, y, color, true, poseStack.last().pose(), false, 0, 15728880);
    }

    private void drawProgressBar(PoseStack poseStack, int x, int y, int width, int height, int percent) {
        // Background
        fillRect(poseStack, x, y, width, height, 0xFF333333);

        // Fill
        int fillWidth = (int) (width * (percent / 100.0));
        fillRect(poseStack, x, y, fillWidth, height, 0xFF00FF00);

        // Border
        fillRect(poseStack, x, y, width, 1, 0xFFFFFFFF);
        fillRect(poseStack, x, y + height - 1, width, 1, 0xFFFFFFFF);
        fillRect(poseStack, x, y, 1, height, 0xFFFFFFFF);
        fillRect(poseStack, x + width - 1, y, 1, height, 0xFFFFFFFF);
    }

    private void fillRect(PoseStack poseStack, int x, int y, int width, int height, int color) {
        // Implementation depends on Minecraft version
        // Use GUI component filling
    }

    private void fillGradient(PoseStack poseStack, int x1, int y1, int x2, int y2, int color1, int color2) {
        // Implementation depends on Minecraft version
    }
}
```

**Register key bindings in `KeyBindings.java`:**
```java
public static final KeyMapping DEBUG_TOGGLE = new KeyMapping(
    "key.minewright.debug_toggle",
    GLFW.GLFW_KEY_F6,
    "key.categories.minewright"
);

public static final KeyMapping DEBUG_CYCLE = new KeyMapping(
    "key.minewright.debug_cycle",
    GLFW.GLFW_KEY_F7,
    "key.categories.minewright"
);

public static final KeyMapping DEBUG_EXPORT = new KeyMapping(
    "key.minewright.debug_export",
    GLFW.GLFW_KEY_F8,
    "key.categories.minewright"
);
```

---

### Phase 4: Error Attribution (Priority 4)

**File:** `src/main/java/com/minewright/debug/ErrorAttributionSystem.java`

```java
package com.minewright.debug;

import com.minewright.MineWrightMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tracks failures and attributes them to specific components.
 */
public class ErrorAttributionSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorAttributionSystem.class);
    private static final ErrorAttributionSystem INSTANCE = new ErrorAttributionSystem();

    private final List<FailureEvent> failureLog = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, ComponentStats> componentStats = new ConcurrentHashMap<>();

    public static ErrorAttributionSystem getInstance() {
        return INSTANCE;
    }

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

        // Update component stats
        componentStats.compute(component, (k, stats) -> {
            if (stats == null) stats = new ComponentStats(k);
            stats.recordFailure(failureType);
            return stats;
        });

        LOGGER.error("[ErrorAttribution] [{}] [{}] Component '{}' failed: {}",
            agentId, failureType, component, description);
    }

    public String generateFailureReport(String agentId) {
        StringBuilder report = new StringBuilder();
        report.append("═════════════════════════════════════════\n");
        report.append("       Failure Report for ").append(agentId).append("\n");
        report.append("═════════════════════════════════════════\n\n");

        // Overall stats
        List<FailureEvent> agentFailures = failureLog.stream()
            .filter(e -> e.agentId.equals(agentId))
            .toList();

        report.append("Total Failures: ").append(agentFailures.size()).append("\n\n");

        if (agentFailures.isEmpty()) {
            report.append("No failures recorded for this agent.\n");
            return report.toString();
        }

        // Group by component
        Map<String, List<FailureEvent>> byComponent = agentFailures.stream()
            .collect(Collectors.groupingBy(e -> e.component));

        report.append("Failures by Component:\n");
        report.append("───────────────────────────────────────\n");

        for (Map.Entry<String, List<FailureEvent>> entry : byComponent.entrySet()) {
            String component = entry.getKey();
            List<FailureEvent> failures = entry.getValue();
            ComponentStats stats = componentStats.get(component);

            report.append(String.format("\n📦 %s\n", component));
            report.append(String.format("   Failures: %d\n", failures.size()));
            if (stats != null) {
                report.append(String.format("   Most Common: %s (%d)\n",
                    stats.mostCommonType, stats.mostCommonCount));
            }

            FailureEvent mostRecent = failures.get(0);
            report.append(String.format("   Latest: %s\n", mostRecent.description));
            report.append(String.format("   Time: %s\n", mostRecent.timestamp));
        }

        // Culprit analysis
        report.append("\n───────────────────────────────────────\n");
        report.append("🔍 Identified Culprits:\n");

        List<String> culprits = identifyCulprits(agentId);
        for (int i = 0; i < culprits.size(); i++) {
            report.append(String.format("   %d. %s\n", i + 1, culprits.get(i)));
        }

        report.append("\n═════════════════════════════════════════\n");

        return report.toString();
    }

    public List<String> identifyCulprits(String agentId) {
        return failureLog.stream()
            .filter(e -> e.agentId.equals(agentId))
            .collect(Collectors.groupingBy(e -> e.component, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(e -> String.format("%s (%d failures)", e.getKey(), e.getValue()))
            .toList();
    }

    public Map<String, Integer> getFailureCountsByType(String agentId) {
        return failureLog.stream()
            .filter(e -> e.agentId.equals(agentId))
            .collect(Collectors.groupingBy(e -> e.failureType, Collectors.summingInt(e -> 1)));
    }

    public void clearLog(String agentId) {
        failureLog.removeIf(e -> e.agentId.equals(agentId));
    }

    public void clearAll() {
        failureLog.clear();
        componentStats.clear();
    }

    private static class ComponentStats {
        final String componentName;
        final Map<String, Integer> failureCounts = new HashMap<>();
        String mostCommonType = "";
        int mostCommonCount = 0;
        int totalFailures = 0;

        ComponentStats(String name) {
            this.componentName = name;
        }

        void recordFailure(String type) {
            totalFailures++;
            failureCounts.merge(type, 1, Integer::sum);

            if (failureCounts.get(type) > mostCommonCount) {
                mostCommonType = type;
                mostCommonCount = failureCounts.get(type);
            }
        }
    }

    public record FailureEvent(
        Instant timestamp,
        String agentId,
        String failureType,
        String component,
        String description,
        Map<String, Object> context
    ) {}
}
```

**Usage in existing code:**
```java
// In ActionExecutor - when action fails
if (!result.isSuccess()) {
    ErrorAttributionSystem.getInstance().recordFailure(
        foreman.getSteveName(),
        "ACTION_FAILURE",
        currentAction.getClass().getSimpleName(),
        result.getMessage(),
        Map.of("task", task.toString())
    );
}

// In TaskPlanner - when LLM fails
if (response == null) {
    ErrorAttributionSystem.getInstance().recordFailure(
        foreman.getSteveName(),
        "LLM_FAILURE",
        "TaskPlanner",
        "Failed to get LLM response",
        Map.of("command", command, "provider", provider)
    );
}

// In pathfinding - when path fails
if (!pathFound) {
    ErrorAttributionSystem.getInstance().recordFailure(
        foreman.getSteveName(),
        "PATH_FAILURE",
        "Pathfinding",
        "No path to target",
        Map.of("from", from, "to", to)
    );
}
```

---

### Phase 5: Debug Commands

**File:** `src/main/java/com/minewright/command/DebugCommands.java`

```java
package com.minewright.command;

import com.minewright.MineWrightMod;
import com.minewright.debug.AgentTraceLogger;
import com.minewright.debug.ErrorAttributionSystem;
import com.minewright.debug.StuckDetectionSystem;
import com.minewright.entity.ForemanEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class DebugCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Toggle debug overlay
        dispatcher.register(
            Commands.literal("steve")
                .then(Commands.literal("debug")
                    .executes(DebugCommands::toggleDebug)
                )
        );

        // View agent explanation
        dispatcher.register(
            Commands.literal("steve")
                .then(Commands.literal("explain")
                    .then(Commands.argument("name", net.minecraft.commands.arguments.EntityArgument.entity())
                        .executes(DebugCommands::explainAgent)
                    )
                )
        );

        // Unstuck agent
        dispatcher.register(
            Commands.literal("steve")
                .then(Commands.literal("unstuck")
                    .then(Commands.argument("name", net.minecraft.commands.arguments.EntityArgument.entity())
                        .executes(DebugCommands::unstuckAgent)
                    )
                )
        );

        // Export trace
        dispatcher.register(
            Commands.literal("steve")
                .then(Commands.literal("export")
                    .then(Commands.argument("name", net.minecraft.commands.arguments.EntityArgument.entity())
                        .executes(DebugCommands::exportTrace)
                    )
                )
        );

        // Failure report
        dispatcher.register(
            Commands.literal("steve")
                .then(Commands.literal("report")
                    .then(Commands.argument("name", net.minecraft.commands.arguments.EntityArgument.entity())
                        .executes(DebugCommands::failureReport)
                    )
                )
        );
    }

    private static int toggleDebug(CommandContext<CommandSourceStack> context) {
        // Toggle debug overlay
        // Implementation depends on client-side integration
        context.getSource().sendSuccess(() ->
            Component.literal("Debug overlay toggled"), true);
        return 1;
    }

    private static int explainAgent(CommandContext<CommandSourceStack> context) {
        try {
            ForemanEntity agent = context.getArgument("name", ForemanEntity.class);
            String agentId = agent.getSteveName();

            StringBuilder explanation = new StringBuilder();
            explanation.append("§e=== Agent Status: ").append(agentId).append(" ===§r\n\n");

            // State
            explanation.append("§6State:§r ").append(getStateString(agent)).append("\n");

            // Goal
            String goal = agent.getMemory().getCurrentGoal();
            explanation.append("§6Goal:§r ").append(goal.isEmpty() ? "(none)" : goal).append("\n");

            // Current Action
            explanation.append("§6Current Action:§r ").append(getCurrentActionString(agent)).append("\n");

            // Queue Size
            explanation.append("§6Queue Size:§r ").append(getQueueSize(agent)).append(" tasks\n");

            // Inventory
            explanation.append("§6Inventory:§r ").append(getInventorySummary(agent)).append("\n");

            // Recent Failures
            int recentFailures = ErrorAttributionSystem.getInstance()
                .getFailureCountsByType(agentId).values().stream().mapToInt(Integer::intValue).sum();
            if (recentFailures > 0) {
                explanation.append("\n§cRecent Failures:§r ").append(recentFailures).append("\n");
            }

            context.getSource().sendSuccess(() ->
                Component.literal(explanation.toString()), true);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int unstuckAgent(CommandContext<CommandSourceStack> context) {
        try {
            ForemanEntity agent = context.getArgument("name", ForemanEntity.class);
            String agentId = agent.getSteveName();

            // Reset stuck detection
            StuckDetectionSystem.getInstance().resetAgent(agentId);

            // Force replan
            agent.getExecutor().stopCurrentAction();

            context.getSource().sendSuccess(() ->
                Component.literal("Reset agent " + agentId + " and cleared stuck state"), true);

            MineWrightMod.LOGGER.info("Manual unstuck triggered for agent '{}'", agentId);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int exportTrace(CommandContext<CommandSourceStack> context) {
        try {
            ForemanEntity agent = context.getArgument("name", ForemanEntity.class);
            String agentId = agent.getSteveName();

            AgentTraceLogger.getInstance().saveToFile(agentId);

            context.getSource().sendSuccess(() ->
                Component.literal("Exported trace for " + agentId + " to debug file"), true);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int failureReport(CommandContext<CommandSourceStack> context) {
        try {
            ForemanEntity agent = context.getArgument("name", ForemanEntity.class);
            String agentId = agent.getSteveName();

            String report = ErrorAttributionSystem.getInstance().generateFailureReport(agentId);

            context.getSource().sendSuccess(() ->
                Component.literal(report), true);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    // Helper methods (implement based on actual API)
    private static String getStateString(ForemanEntity agent) { return "EXECUTING"; }
    private static String getCurrentActionString(ForemanEntity agent) { return "Mining"; }
    private static int getQueueSize(ForemanEntity agent) { return 5; }
    private static String getInventorySummary(ForemanEntity agent) { return "Oak log x64"; }
}
```

---

## Integration Checklist

- [ ] Add `AgentTraceLogger` calls throughout codebase
  - [ ] TaskPlanner - log LLM calls
  - [ ] ActionExecutor - log actions and state transitions
  - [ ] All Action implementations - log results
- [ ] Implement `StuckDetectionSystem` and register tick handler
- [ ] Add `ErrorAttributionSystem` calls at failure points
- [ ] Implement `AgentDebugOverlay` and register with Minecraft render system
- [ ] Add key bindings to `KeyBindings.java`
- [ ] Register debug commands in `DebugCommands.java`
- [ ] Add methods to `ActionExecutor` for recovery (skip, repathfind)
- [ ] Test all debug features with crew members
- [ ] Document debug features for players

---

**Quick Reference Version:** 1.0
**Companion Document:** RESEARCH_AI_DEBUGGING.md
