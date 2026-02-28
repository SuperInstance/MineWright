package com.minewright.communication;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base message type for inter-agent communication.
 *
 * <p>Provides a unified message format for all coordination between agents,
 * supporting direct messages, broadcasts, and request/response patterns.</p>
 *
 * <p><b>Message Types:</b></p>
 * <ul>
 *   <li>STATUS_UPDATE - Agent reports current state</li>
 *   <li>TASK_REQUEST - Agent requests help or task assignment</li>
 *   <li>TASK_RESULT - Agent reports task completion</li>
 *   <li>COORDINATION - Multi-agent coordination messages</li>
 *   <li>ALERT - Urgent notifications (danger, discoveries)</li>
 *   <li>QUERY - Information requests</li>
 *   <li>RESPONSE - Responses to queries</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is immutable and thread-safe.</p>
 *
 * @since 1.3.0
 */
public record AgentMessage(
    UUID senderId,
    UUID recipientId,
    MessageType type,
    String content,
    Map<String, Object> payload,
    long timestamp,
    UUID correlationId
) {

    /**
     * Creates a new agent message with current timestamp.
     *
     * @param senderId     Unique ID of the sender
     * @param recipientId  Unique ID of recipient (null for broadcast)
     * @param type         Message type
     * @param content      Human-readable message content
     * @param payload      Structured data payload
     * @param correlationId Correlation ID for request/response pairing
     */
    public AgentMessage {
        Objects.requireNonNull(senderId, "senderId cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        // recipientId can be null for broadcasts
        Objects.requireNonNull(content, "content cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
        // correlationId can be null for one-way messages
    }

    /**
     * Creates a message with current timestamp.
     */
    public AgentMessage(UUID senderId, UUID recipientId, MessageType type,
                       String content, Map<String, Object> payload, UUID correlationId) {
        this(senderId, recipientId, type, content,
            payload != null ? new ConcurrentHashMap<>(payload) : new ConcurrentHashMap<>(),
            System.currentTimeMillis(), correlationId);
    }

    /**
     * Returns true if this is a broadcast message.
     *
     * @return true if recipientId is null
     */
    public boolean isBroadcast() {
        return recipientId == null;
    }

    /**
     * Returns true if this is a response to a previous message.
     *
     * @return true if correlationId is not null
     */
    public boolean isResponse() {
        return correlationId != null;
    }

    /**
     * Gets a payload value by key.
     *
     * @param key Payload key
     * @param <T> Expected type
     * @return Value or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getPayload(String key) {
        return (T) payload.get(key);
    }

    /**
     * Gets a payload value with default.
     *
     * @param key Payload key
     * @param defaultValue Default value if not found
     * @param <T> Expected type
     * @return Value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getPayload(String key, T defaultValue) {
        Object value = payload.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Types of messages for inter-agent communication.
     */
    public enum MessageType {
        /** Agent reports current status/state */
        STATUS_UPDATE,

        /** Agent requests task assignment or help */
        TASK_REQUEST,

        /** Agent reports task completion result */
        TASK_RESULT,

        /** General coordination between agents */
        COORDINATION,

        /** Urgent alert (danger, discovery, etc.) */
        ALERT,

        /** Information/query request */
        QUERY,

        /** Response to a query */
        RESPONSE
    }

    /**
     * Builder for creating AgentMessage instances.
     */
    public static class Builder {
        private UUID senderId;
        private UUID recipientId;
        private MessageType type;
        private String content;
        private final Map<String, Object> payload = new ConcurrentHashMap<>();
        private UUID correlationId;

        public Builder sender(UUID senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder recipient(UUID recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        public Builder type(MessageType type) {
            this.type = type;
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
            if (payload != null) {
                this.payload.putAll(payload);
            }
            return this;
        }

        public Builder correlationId(UUID correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = UUID.fromString(correlationId);
            return this;
        }

        /**
         * Builds the agent message.
         *
         * @return New AgentMessage instance
         * @throws IllegalStateException if required fields are missing
         */
        public AgentMessage build() {
            Objects.requireNonNull(senderId, "senderId is required");
            Objects.requireNonNull(type, "type is required");
            Objects.requireNonNull(content, "content is required");

            return new AgentMessage(senderId, recipientId, type, content,
                new ConcurrentHashMap<>(payload), correlationId);
        }
    }

    @Override
    public String toString() {
        return String.format("AgentMessage[%s -> %s: %s]",
            senderId.toString().substring(0, 8),
            isBroadcast() ? "ALL" : recipientId.toString().substring(0, 8),
            type);
    }
}
