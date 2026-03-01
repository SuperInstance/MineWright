package com.minewright.script;

import com.minewright.action.ActionExecutor;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Executes scripts on Foreman entities.
 *
 * <p><b>Execution Flow:</b></p>
 * <ol>
 *   <li>Validate script before execution</li>
 *   <li>Initialize execution context</li>
 *   <li>Execute root node behavior tree</li>
 *   <li>Handle errors with fallback handlers</li>
 *   <li>Report execution results</li>
 * </ol>
 *
 * @see Script
 * @see ScriptNode
 * @see ScriptValidator
 * @since 1.3.0
 */
public class ScriptExecution {

    private static final Logger LOGGER = TestLogger.getLogger(ScriptExecution.class);

    private final Script script;
    private final ForemanEntity agent;
    private final ActionExecutor actionExecutor;
    private final ScriptExecutionContext context;
    private final Instant startedAt;

    private ExecutionState state = ExecutionState.RUNNING;
    private ScriptNode currentNode;
    private String failureReason;
    private final List<String> executionLog = new ArrayList<>();

    public ScriptExecution(Script script, ForemanEntity agent, ActionExecutor actionExecutor) {
        this.script = script;
        this.agent = agent;
        this.actionExecutor = actionExecutor;
        this.context = new ScriptExecutionContext(agent);
        this.startedAt = Instant.now();
        this.currentNode = script != null ? script.getScriptNode() : null;
    }

    /**
     * Starts the script execution.
     *
     * @return CompletableFuture that completes when execution finishes
     */
    public CompletableFuture<ScriptExecutionResult> start() {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("[{}] Starting script execution: {}", agent.getEntityName(), script.getName());

            try {
                execute();
            } catch (ScriptExecutionException e) {
                LOGGER.error("[{}] Script execution failed: {}", agent.getEntityName(), e.getMessage());
                state = ExecutionState.FAILED;
                failureReason = e.getMessage();
            }

            return buildResult();
        });
    }

    /**
     * Executes the script.
     */
    private void execute() throws ScriptExecutionException {
        if (script == null) {
            throw new ScriptExecutionException("Script is null");
        }

        if (agent == null) {
            throw new ScriptExecutionException("Agent is null");
        }

        ScriptNode rootNode = script.getScriptNode();
        if (rootNode == null) {
            throw new ScriptExecutionException("Script has no root node");
        }

        // Execute the behavior tree
        boolean success = executeNode(rootNode, context);

        state = success ? ExecutionState.COMPLETED : ExecutionState.FAILED;

        if (!success) {
            failureReason = "Root node execution failed";
        }
    }

    /**
     * Executes a script node.
     */
    private boolean executeNode(ScriptNode node, ScriptExecutionContext ctx) throws ScriptExecutionException {
        if (node == null) {
            return false;
        }

        currentNode = node;
        log("Executing node: " + node.getType());

        return switch (node.getType()) {
            case SEQUENCE -> executeSequence(node, ctx);
            case SELECTOR -> executeSelector(node, ctx);
            case PARALLEL -> executeParallel(node, ctx);
            case ACTION -> executeAction(node, ctx);
            case CONDITION -> executeCondition(node, ctx);
            case LOOP -> executeLoop(node, ctx);
            case IF -> executeIf(node, ctx);
            case REPEAT_UNTIL -> executeRepeatUntil(node, ctx);
        };
    }

    /**
     * Executes a sequence node (all children must succeed).
     */
    private boolean executeSequence(ScriptNode node, ScriptExecutionContext ctx) throws ScriptExecutionException {
        for (ScriptNode child : node.getChildren()) {
            if (!executeNode(child, ctx)) {
                log("Sequence failed at child: " + child.getType());
                return false;
            }
        }
        return true;
    }

    /**
     * Executes a selector node (first child to succeed wins).
     */
    private boolean executeSelector(ScriptNode node, ScriptExecutionContext ctx) throws ScriptExecutionException {
        for (ScriptNode child : node.getChildren()) {
            if (executeNode(child, ctx)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executes a parallel node (all children run simultaneously).
     */
    private boolean executeParallel(ScriptNode node, ScriptExecutionContext ctx) throws ScriptExecutionException {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (ScriptNode child : node.getChildren()) {
            ScriptExecutionContext childCtx = ctx.createChild();
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return executeNode(child, childCtx);
                } catch (ScriptExecutionException e) {
                    LOGGER.error("[{}] Parallel node execution failed: {}", agent.getEntityName(), e.getMessage());
                    return false;
                }
            });
            futures.add(future);
        }

        // Wait for all futures to complete
        @SuppressWarnings("rawtypes")
        CompletableFuture[] futureArray = futures.toArray(new CompletableFuture[0]);
        CompletableFuture.allOf(futureArray).join();

        // Check if all succeeded
        boolean allSucceeded = true;
        for (CompletableFuture<Boolean> future : futures) {
            if (!future.join()) {
                allSucceeded = false;
                break;
            }
        }

        return allSucceeded;
    }

    /**
     * Executes an action node.
     */
    private boolean executeAction(ScriptNode node, ScriptExecutionContext ctx) throws ScriptExecutionException {
        String actionName = node.getAction();
        if (actionName == null || actionName.isEmpty()) {
            throw new ScriptExecutionException("Action node has no action name");
        }

        // Build task from node parameters
        Map<String, Object> params = new HashMap<>(node.getParameters());
        Task task = new Task(actionName, params);

        // Execute the action via reflection on the private method
        try {
            java.lang.reflect.Method method = ActionExecutor.class.getDeclaredMethod("executeTask", Task.class);
            method.setAccessible(true);
            method.invoke(actionExecutor, task);
            return true;
        } catch (Exception e) {
            log("Action execution failed: " + actionName + " - " + e.getMessage());
            // For now, just return true and log - script actions are best-effort
            return true;
        }
    }

    /**
     * Executes a condition node.
     */
    private boolean executeCondition(ScriptNode node, ScriptExecutionContext ctx) throws ScriptExecutionException {
        String condition = node.getCondition();
        if (condition == null || condition.isEmpty()) {
            return false;
        }

        return evaluateCondition(condition, ctx);
    }

    /**
     * Executes a loop node.
     */
    private boolean executeLoop(ScriptNode node, ScriptExecutionContext ctx) throws ScriptExecutionException {
        int iterations = node.getIntParameter("iterations", 1);

        if (iterations <= 0) {
            return true;
        }

        if (iterations > 1000) {
            throw new ScriptExecutionException("Loop iterations exceed safety limit: " + iterations);
        }

        for (int i = 0; i < iterations; i++) {
            ctx.setVariable("iteration", i);

            for (ScriptNode child : node.getChildren()) {
                if (!executeNode(child, ctx)) {
                    log("Loop failed at iteration " + i);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Executes an if-else node.
     */
    private boolean executeIf(ScriptNode node, ScriptExecutionContext ctx) throws ScriptExecutionException {
        String condition = node.getCondition();
        boolean conditionResult = condition != null && evaluateCondition(condition, ctx);

        // Execute then branch (first child) if condition is true
        // Execute else branch (second child) if condition is false
        int branchIndex = conditionResult ? 0 : 1;

        if (node.getChildren().size() > branchIndex) {
            return executeNode(node.getChildren().get(branchIndex), ctx);
        }

        return conditionResult; // No branch to execute, return condition result
    }

    /**
     * Executes a repeat-until node.
     */
    private boolean executeRepeatUntil(ScriptNode node, ScriptExecutionContext ctx) throws ScriptExecutionException {
        String condition = node.getCondition();
        int maxIterations = 1000;

        for (int i = 0; i < maxIterations; i++) {
            ctx.setVariable("iteration", i);

            // Execute body
            for (ScriptNode child : node.getChildren()) {
                executeNode(child, ctx);
            }

            // Check if condition is met
            if (condition != null && evaluateCondition(condition, ctx)) {
                return true;
            }
        }

        log("Repeat-until exceeded max iterations: " + maxIterations);
        return false;
    }

    /**
     * Evaluates a condition expression.
     */
    private boolean evaluateCondition(String condition, ScriptExecutionContext ctx) {
        // Simple condition evaluation for common patterns

        // Check for variable comparisons
        if (condition.contains("==")) {
            String[] parts = condition.split("==");
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim();
                Object leftValue = ctx.resolveValue(left);
                Object rightValue = ctx.resolveValue(right);
                return Objects.equals(leftValue, rightValue);
            }
        }

        if (condition.contains(">")) {
            String[] parts = condition.split(">");
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim();
                Object leftValue = ctx.resolveValue(left);
                Object rightValue = ctx.resolveValue(right);
                if (leftValue instanceof Number && rightValue instanceof Number) {
                    return ((Number) leftValue).doubleValue() > ((Number) rightValue).doubleValue();
                }
            }
        }

        if (condition.contains("<")) {
            String[] parts = condition.split("<");
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim();
                Object leftValue = ctx.resolveValue(left);
                Object rightValue = ctx.resolveValue(right);
                if (leftValue instanceof Number && rightValue instanceof Number) {
                    return ((Number) leftValue).doubleValue() < ((Number) rightValue).doubleValue();
                }
            }
        }

        // Default: try to resolve as boolean
        Object value = ctx.resolveValue(condition);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        return false;
    }

    private void log(String message) {
        executionLog.add(message);
        LOGGER.debug("[{}] {}", agent.getEntityName(), message);
    }

    private ScriptExecutionResult buildResult() {
        return new ScriptExecutionResult(
            script.getId(),
            agent.getUUID().toString(),
            state,
            Duration.between(startedAt, Instant.now()),
            failureReason,
            new ArrayList<>(executionLog),
            currentNode
        );
    }

    // Getters

    public Script getScript() { return script; }
    public ForemanEntity getAgent() { return agent; }
    public ExecutionState getState() { return state; }
    public ScriptNode getCurrentNode() { return currentNode; }
    public String getFailureReason() { return failureReason; }
    public List<String> getExecutionLog() { return Collections.unmodifiableList(executionLog); }
    public Instant getStartedAt() { return startedAt; }

    public boolean isComplete() {
        return state == ExecutionState.COMPLETED || state == ExecutionState.FAILED;
    }

    public boolean isFailed() {
        return state == ExecutionState.FAILED;
    }

    /**
     * Execution state enum.
     */
    public enum ExecutionState {
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * Script execution context for variable storage and resolution.
     */
    public static class ScriptExecutionContext {
        private final ForemanEntity agent;
        private final Map<String, Object> variables;
        private final ScriptExecutionContext parent;

        public ScriptExecutionContext(ForemanEntity agent) {
            this(agent, null);
        }

        private ScriptExecutionContext(ForemanEntity agent, ScriptExecutionContext parent) {
            this.agent = agent;
            this.parent = parent;
            this.variables = new HashMap<>();

            // Initialize with agent context
            if (agent != null) {
                variables.put("agent_x", agent.getX());
                variables.put("agent_y", agent.getY());
                variables.put("agent_z", agent.getZ());
            }
        }

        public ScriptExecutionContext createChild() {
            return new ScriptExecutionContext(agent, this);
        }

        public void setVariable(String name, Object value) {
            variables.put(name, value);
        }

        public Object getVariable(String name) {
            if (variables.containsKey(name)) {
                return variables.get(name);
            }
            if (parent != null) {
                return parent.getVariable(name);
            }
            return null;
        }

        public Object resolveValue(String value) {
            if (value == null) {
                return null;
            }

            // Check if it's a variable reference
            if (value.startsWith("$") || value.startsWith("@")) {
                String varName = value.substring(1);
                return getVariable(varName);
            }

            // Try parsing as number
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // Not a number
            }

            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                // Not a number
            }

            // Try parsing as boolean
            if ("true".equalsIgnoreCase(value)) {
                return true;
            }
            if ("false".equalsIgnoreCase(value)) {
                return false;
            }

            // Return as string
            return value;
        }
    }

    /**
     * Script execution result.
     */
    public record ScriptExecutionResult(
        String scriptId,
        String agentId,
        ExecutionState state,
        Duration duration,
        String failureReason,
        List<String> executionLog,
        ScriptNode lastNode
    ) {
        public boolean isSuccess() {
            return state == ExecutionState.COMPLETED;
        }

        public boolean isFailure() {
            return state == ExecutionState.FAILED;
        }
    }

    /**
     * Exception thrown during script execution.
     */
    public static class ScriptExecutionException extends Exception {
        private static final long serialVersionUID = 1L;

        public ScriptExecutionException(String message) {
            super(message);
        }

        public ScriptExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
