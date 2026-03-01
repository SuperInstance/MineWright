package com.minewright.script;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Feedback from script execution for learning and refinement.
 *
 * <p>This class captures comprehensive information about script execution results,
 * enabling the ScriptRefiner to analyze and improve scripts iteratively.</p>
 *
 * <p><b>Feedback Categories:</b></p>
 * <ul>
 *   <li><b>Execution Status:</b> Success/failure, completion state</li>
 *   <li><b>Performance Metrics:</b> Execution time, resource usage</li>
 *   <li><b>Error Information:</b> Failure reasons, error messages</li>
 *   <li><b>User Feedback:</b> Satisfaction ratings, manual corrections</li>
 *   <li><b>Context Data:</b> Agent state, environment conditions</li>
 * </ul>
 *
 * @see ScriptRefiner
 * @see ScriptExecution
 * @since 1.3.0
 */
public class ExecutionFeedback {

    private final String scriptId;
    private final String scriptName;
    private final String agentId;
    private final Instant executedAt;
    private final Duration executionTime;
    private final boolean success;
    private final String failureReason;
    private final List<String> errorMessages;
    private final ResourceUsage resourceUsage;
    private final Double userSatisfaction;
    private final Map<String, Object> context;
    private final List<String> executionLog;
    private final String lastNodeExecuted;

    private ExecutionFeedback(Builder builder) {
        this.scriptId = builder.scriptId;
        this.scriptName = builder.scriptName;
        this.agentId = builder.agentId;
        this.executedAt = builder.executedAt != null ? builder.executedAt : Instant.now();
        this.executionTime = builder.executionTime;
        this.success = builder.success;
        this.failureReason = builder.failureReason;
        this.errorMessages = Collections.unmodifiableList(new ArrayList<>(builder.errorMessages));
        this.resourceUsage = builder.resourceUsage;
        this.userSatisfaction = builder.userSatisfaction;
        this.context = Collections.unmodifiableMap(new HashMap<>(builder.context));
        this.executionLog = Collections.unmodifiableList(new ArrayList<>(builder.executionLog));
        this.lastNodeExecuted = builder.lastNodeExecuted;
    }

    /**
     * Creates a new builder for constructing ExecutionFeedback.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates feedback from a ScriptExecutionResult.
     */
    public static ExecutionFeedback fromResult(ScriptExecution.ScriptExecutionResult result) {
        return builder()
            .scriptId(result.scriptId())
            .agentId(result.agentId())
            .success(result.isSuccess())
            .failureReason(result.failureReason())
            .executionTime(result.duration())
            .executionLog(result.executionLog())
            .lastNodeExecuted(result.lastNode() != null ? result.lastNode().toString() : null)
            .build();
    }

    /**
     * Creates successful execution feedback.
     */
    public static ExecutionFeedback success(String scriptId, Duration executionTime) {
        return builder()
            .scriptId(scriptId)
            .success(true)
            .executionTime(executionTime)
            .build();
    }

    /**
     * Creates failed execution feedback.
     */
    public static ExecutionFeedback failure(String scriptId, String failureReason, List<String> errors) {
        return builder()
            .scriptId(scriptId)
            .success(false)
            .failureReason(failureReason)
            .errorMessages(errors)
            .build();
    }

    /**
     * Converts the feedback to a prompt section for LLM.
     */
    public String toPromptSection() {
        StringBuilder sb = new StringBuilder(1024);

        sb.append("**Execution Status:** ").append(success ? "SUCCESS" : "FAILED").append("\n");

        if (executionTime != null) {
            sb.append("**Execution Time:** ").append(executionTime.toMillis()).append(" ms\n");
        }

        if (!success && failureReason != null) {
            sb.append("**Failure Reason:** ").append(failureReason).append("\n");
        }

        if (!errorMessages.isEmpty()) {
            sb.append("**Errors:**\n");
            for (String error : errorMessages) {
                sb.append("  - ").append(error).append("\n");
            }
        }

        if (resourceUsage != null) {
            sb.append("**Resource Usage:**\n");
            sb.append("  - Efficiency Score: ").append(String.format("%.2f", resourceUsage.getScore())).append("\n");
            if (resourceUsage.getItemsConsumed() != null) {
                sb.append("  - Items Consumed: ").append(resourceUsage.getItemsConsumed()).append("\n");
            }
            if (resourceUsage.getBlocksMined() != null) {
                sb.append("  - Blocks Mined: ").append(resourceUsage.getBlocksMined()).append("\n");
            }
        }

        if (userSatisfaction != null) {
            sb.append("**User Satisfaction:** ").append(String.format("%.2f", userSatisfaction)).append("\n");
        }

        if (!context.isEmpty()) {
            sb.append("**Context:**\n");
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                sb.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        return sb.toString();
    }

    // Getters

    public String getScriptId() { return scriptId; }
    public String getScriptName() { return scriptName; }
    public String getAgentId() { return agentId; }
    public Instant getExecutedAt() { return executedAt; }
    public Duration getExecutionTime() { return executionTime; }
    public boolean isSuccess() { return success; }
    public String getFailureReason() { return failureReason; }
    public List<String> getErrorMessages() { return errorMessages; }
    public ResourceUsage getResourceUsage() { return resourceUsage; }
    public Double getUserSatisfaction() { return userSatisfaction; }
    public Map<String, Object> getContext() { return context; }
    public List<String> getExecutionLog() { return executionLog; }
    public String getLastNodeExecuted() { return lastNodeExecuted; }

    /**
     * Builder for constructing ExecutionFeedback instances.
     */
    public static class Builder {
        private String scriptId;
        private String scriptName;
        private String agentId;
        private Instant executedAt;
        private Duration executionTime;
        private boolean success = true;
        private String failureReason;
        private List<String> errorMessages = new ArrayList<>();
        private ResourceUsage resourceUsage;
        private Double userSatisfaction;
        private Map<String, Object> context = new HashMap<>();
        private List<String> executionLog = new ArrayList<>();
        private String lastNodeExecuted;

        public Builder scriptId(String scriptId) {
            this.scriptId = scriptId;
            return this;
        }

        public Builder scriptName(String scriptName) {
            this.scriptName = scriptName;
            return this;
        }

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder executedAt(Instant executedAt) {
            this.executedAt = executedAt;
            return this;
        }

        public Builder executionTime(Duration executionTime) {
            this.executionTime = executionTime;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder errorMessages(List<String> errorMessages) {
            this.errorMessages = new ArrayList<>(errorMessages);
            return this;
        }

        public Builder addErrorMessage(String error) {
            this.errorMessages.add(error);
            return this;
        }

        public Builder resourceUsage(ResourceUsage resourceUsage) {
            this.resourceUsage = resourceUsage;
            return this;
        }

        public Builder userSatisfaction(Double userSatisfaction) {
            this.userSatisfaction = userSatisfaction;
            return this;
        }

        public Builder addContext(String key, Object value) {
            this.context.put(key, value);
            return this;
        }

        public Builder context(Map<String, Object> context) {
            this.context = new HashMap<>(context);
            return this;
        }

        public Builder executionLog(List<String> executionLog) {
            this.executionLog = new ArrayList<>(executionLog);
            return this;
        }

        public Builder addLogEntry(String entry) {
            this.executionLog.add(entry);
            return this;
        }

        public Builder lastNodeExecuted(String lastNodeExecuted) {
            this.lastNodeExecuted = lastNodeExecuted;
            return this;
        }

        public ExecutionFeedback build() {
            return new ExecutionFeedback(this);
        }
    }

    /**
     * Resource usage information from script execution.
     */
    public static class ResourceUsage {
        private final Double efficiencyScore;
        private final Integer itemsConsumed;
        private final Integer blocksMined;
        private final Integer blocksPlaced;
        private final Integer distanceTraveled;
        private final Map<String, Integer> resourceBreakdown;

        private ResourceUsage(Builder builder) {
            this.efficiencyScore = builder.efficiencyScore;
            this.itemsConsumed = builder.itemsConsumed;
            this.blocksMined = builder.blocksMined;
            this.blocksPlaced = builder.blocksPlaced;
            this.distanceTraveled = builder.distanceTraveled;
            this.resourceBreakdown = Collections.unmodifiableMap(new HashMap<>(builder.resourceBreakdown));
        }

        /**
         * Creates a new builder for ResourceUsage.
         */
        public static Builder builder() {
            return new Builder();
        }

        public Double getScore() { return efficiencyScore; }
        public Integer getItemsConsumed() { return itemsConsumed; }
        public Integer getBlocksMined() { return blocksMined; }
        public Integer getBlocksPlaced() { return blocksPlaced; }
        public Integer getDistanceTraveled() { return distanceTraveled; }
        public Map<String, Integer> getResourceBreakdown() { return resourceBreakdown; }

        /**
         * Builder for constructing ResourceUsage instances.
         */
        public static class Builder {
            private Double efficiencyScore;
            private Integer itemsConsumed;
            private Integer blocksMined;
            private Integer blocksPlaced;
            private Integer distanceTraveled;
            private Map<String, Integer> resourceBreakdown = new HashMap<>();

            public Builder efficiencyScore(Double efficiencyScore) {
                this.efficiencyScore = efficiencyScore;
                return this;
            }

            public Builder itemsConsumed(Integer itemsConsumed) {
                this.itemsConsumed = itemsConsumed;
                return this;
            }

            public Builder blocksMined(Integer blocksMined) {
                this.blocksMined = blocksMined;
                return this;
            }

            public Builder blocksPlaced(Integer blocksPlaced) {
                this.blocksPlaced = blocksPlaced;
                return this;
            }

            public Builder distanceTraveled(Integer distanceTraveled) {
                this.distanceTraveled = distanceTraveled;
                return this;
            }

            public Builder addResource(String resource, int amount) {
                this.resourceBreakdown.put(resource, amount);
                return this;
            }

            public Builder resourceBreakdown(Map<String, Integer> resourceBreakdown) {
                this.resourceBreakdown = new HashMap<>(resourceBreakdown);
                return this;
            }

            public ResourceUsage build() {
                return new ResourceUsage(this);
            }
        }
    }

    /**
     * Creates a summary of this feedback for logging.
     */
    public String getSummary() {
        return String.format(
            "ExecutionFeedback{scriptId='%s', success=%s, executionTime=%dms, errors=%d, satisfaction=%.2f}",
            scriptId, success, executionTime != null ? executionTime.toMillis() : -1,
            errorMessages.size(), userSatisfaction != null ? userSatisfaction : -1
        );
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
