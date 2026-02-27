package com.minewright.orchestration;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Message format for inter-agent communication.
 *
 * <p>Supports various message types for coordination between agents
 * in the hierarchical orchestration system.</p>
 *
 * <p><b>Message Types:</b></p>
 * <ul>
 *   <li>TASK_ASSIGNMENT - Foreman assigns task to worker</li>
 *   <li>TASK_PROGRESS - Worker reports progress to foreman</li>
 *   <li>TASK_COMPLETE - Worker reports task completion</li>
 *   <li>TASK_FAILED - Worker reports task failure</li>
 *   <li>HELP_REQUEST - Worker requests assistance</li>
 *   <li>STATUS_QUERY - Foreman asks for status</li>
 *   <li>STATUS_REPORT - Agent reports current status</li>
 *   <li>COORDINATION - Inter-worker coordination</li>
 *   <li>BROADCAST - Message to all agents</li>
 * </ul>
 *
 * @since 1.2.0
 */
public class AgentMessage {

    /**
     * Types of agent messages.
     */
    public enum Type {
        /** Foreman assigns a task to a worker */
        TASK_ASSIGNMENT("task_assignment", true),

        /** Worker reports progress on assigned task */
        TASK_PROGRESS("task_progress", false),

        /** Worker reports task successfully completed */
        TASK_COMPLETE("task_complete", false),

        /** Worker reports task failed */
        TASK_FAILED("task_failed", false),

        /** Worker requests help or clarification */
        HELP_REQUEST("help_request", false),

        /** Foreman queries agent status */
        STATUS_QUERY("status_query", true),

        /** Agent responds with current status */
        STATUS_REPORT("status_report", false),

        /** Coordination message between workers */
        COORDINATION("coordination", false),

        /** Broadcast message to all agents */
        BROADCAST("broadcast", true),

        /** Human player command relayed through foreman */
        HUMAN_COMMAND("human_command", true),

        /** Foreman announces plan to team */
        PLAN_ANNOUNCEMENT("plan_announcement", true);

        private final String code;
        private final boolean requiresAuthority;

        Type(String code, boolean requiresAuthority) {
            this.code = code;
            this.requiresAuthority = requiresAuthority;
        }

        public String getCode() {
            return code;
        }

        public boolean requiresAuthority() {
            return requiresAuthority;
        }
    }

    /**
     * Priority levels for messages.
     */
    public enum Priority {
        LOW(1),
        NORMAL(5),
        HIGH(10),
        URGENT(20),
        CRITICAL(50);

        private final int level;

        Priority(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    private final String messageId;
    private final Type type;
    private final String senderId;
    private final String senderName;
    private final String recipientId; // null for broadcast
    private final String content;
    private final Map<String, Object> payload;
    private final Priority priority;
    private final Instant timestamp;
    private final String correlationId; // For request-response pairing

    private AgentMessage(Builder builder) {
        this.messageId = UUID.randomUUID().toString().substring(0, 8);
        this.type = builder.type;
        this.senderId = builder.senderId;
        this.senderName = builder.senderName;
        this.recipientId = builder.recipientId;
        this.content = builder.content;
        this.payload = builder.payload;
        this.priority = builder.priority;
        this.timestamp = Instant.now();
        this.correlationId = builder.correlationId;
    }

    // Getters

    public String getMessageId() {
        return messageId;
    }

    public Type getType() {
        return type;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public Priority getPriority() {
        return priority;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public boolean isBroadcast() {
        return recipientId == null || recipientId.equals("*");
    }

    // Convenience methods for payload access

    @SuppressWarnings("unchecked")
    public <T> T getPayloadValue(String key) {
        return (T) payload.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getPayloadValue(String key, T defaultValue) {
        Object value = payload.get(key);
        return value != null ? (T) value : defaultValue;
    }

    // Factory methods for common message types

    /**
     * Creates a task assignment message from foreman to worker.
     */
    public static AgentMessage taskAssignment(String foremanId, String foremanName,
                                              String workerId, String taskDescription,
                                              Map<String, Object> taskParams) {
        Map<String, Object> payload = new ConcurrentHashMap<>(taskParams);
        payload.put("taskDescription", taskDescription);

        return new Builder()
            .type(Type.TASK_ASSIGNMENT)
            .senderId(foremanId)
            .senderName(foremanName)
            .recipientId(workerId)
            .content(taskDescription)
            .payload(payload)
            .priority(Priority.NORMAL)
            .build();
    }

    /**
     * Creates a task progress report from worker to foreman.
     */
    public static AgentMessage taskProgress(String workerId, String workerName,
                                           String foremanId, String taskId,
                                           int percentComplete, String status) {
        Map<String, Object> payload = new ConcurrentHashMap<>();
        payload.put("taskId", taskId);
        payload.put("percentComplete", percentComplete);
        payload.put("status", status);

        return new Builder()
            .type(Type.TASK_PROGRESS)
            .senderId(workerId)
            .senderName(workerName)
            .recipientId(foremanId)
            .content(status)
            .payload(payload)
            .priority(Priority.LOW)
            .build();
    }

    /**
     * Creates a task completion message.
     */
    public static AgentMessage taskComplete(String workerId, String workerName,
                                           String foremanId, String taskId,
                                           boolean success, String result) {
        Map<String, Object> payload = new ConcurrentHashMap<>();
        payload.put("taskId", taskId);
        payload.put("success", success);
        payload.put("result", result);

        return new Builder()
            .type(success ? Type.TASK_COMPLETE : Type.TASK_FAILED)
            .senderId(workerId)
            .senderName(workerName)
            .recipientId(foremanId)
            .content(result)
            .payload(payload)
            .priority(success ? Priority.NORMAL : Priority.HIGH)
            .build();
    }

    /**
     * Creates a help request message.
     */
    public static AgentMessage helpRequest(String workerId, String workerName,
                                          String foremanId, String issue) {
        return new Builder()
            .type(Type.HELP_REQUEST)
            .senderId(workerId)
            .senderName(workerName)
            .recipientId(foremanId)
            .content(issue)
            .priority(Priority.HIGH)
            .build();
    }

    /**
     * Creates a broadcast message to all agents.
     */
    public static AgentMessage broadcast(String senderId, String senderName,
                                        String content, Priority priority) {
        return new Builder()
            .type(Type.BROADCAST)
            .senderId(senderId)
            .senderName(senderName)
            .recipientId("*")
            .content(content)
            .priority(priority)
            .build();
    }

    /**
     * Creates a human command relay message.
     */
    public static AgentMessage humanCommand(String foremanId, String foremanName,
                                           String command, String targetAgentId) {
        return new Builder()
            .type(Type.HUMAN_COMMAND)
            .senderId(foremanId)
            .senderName(foremanName)
            .recipientId(targetAgentId != null ? targetAgentId : "*")
            .content(command)
            .priority(Priority.HIGH)
            .build();
    }

    @Override
    public String toString() {
        return String.format("AgentMessage[%s %s->%s: %s (pri=%s)]",
            type.getCode(), senderName,
            recipientId != null ? recipientId : "ALL",
            content != null ? (content.length() > 30 ? content.substring(0, 30) + "..." : content) : "",
            priority);
    }

    /**
     * Builder for creating AgentMessage instances.
     */
    public static class Builder {
        private Type type = Type.COORDINATION;
        private String senderId;
        private String senderName;
        private String recipientId;
        private String content;
        private Map<String, Object> payload = new ConcurrentHashMap<>();
        private Priority priority = Priority.NORMAL;
        private String correlationId;

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder sender(String senderId, String senderName) {
            this.senderId = senderId;
            this.senderName = senderName;
            return this;
        }

        public Builder senderId(String senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder senderName(String senderName) {
            this.senderName = senderName;
            return this;
        }

        public Builder recipient(String recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        public Builder recipientId(String recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder payload(String key, Object value) {
            this.payload.put(key, value);
            return this;
        }

        public Builder payload(Map<String, Object> payload) {
            this.payload.putAll(payload);
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public AgentMessage build() {
            if (senderId == null) {
                throw new IllegalStateException("senderId is required");
            }
            return new AgentMessage(this);
        }
    }
}
