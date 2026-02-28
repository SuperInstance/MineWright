package com.minewright.communication;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Standard message formats for common agent communication protocols.
 *
 * <p>This class provides factory methods for creating well-formed messages
 * following established communication patterns. These standard formats ensure
 * consistency across different agent implementations.</p>
 *
 * <p><b>Supported Protocols:</b></p>
 * <ul>
 *   <li>Task Request - Agent requests help or task assignment</li>
 *   <li>Task Response - Agent accepts/declines task request</li>
 *   <li>Status Broadcast - Agent broadcasts current status</li>
 *   <li>Alert - Agent broadcasts urgent information</li>
 *   <li>Resource Share - Agent shares resource location</li>
 *   <li>Position Update - Agent shares current position</li>
 *   <li>Coordination Request - Agent requests coordination</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * // Request help with a task
 * AgentMessage request = ProtocolMessages.taskRequest(
 *     senderId,
 *     "I need help building a house at (100, 64, 200)",
 *     Map.of(
 *         "taskType", "build",
 *         "location", new BlockPos(100, 64, 200),
 *         "priority", "high"
 *     )
 * );
 *
 * // Accept the task
 * AgentMessage response = ProtocolMessages.taskResponse(
 *     senderId,
 *     request.correlationId(),
 *     TaskResponse.ACCEPT,
 *     "I can help with that"
 * );
 *
 * // Share resource discovery
 * AgentMessage discovery = ProtocolMessages.resourceShare(
 *     senderId,
 *     "diamond_ore",
 *     new BlockPos(50, 12, -30),
 *     5
 * );
 * </pre>
 *
 * @since 1.3.0
 */
public final class ProtocolMessages {

    private ProtocolMessages() {
        // Utility class - prevent instantiation
    }

    // ========== Task Request/Response ==========

    /**
     * Creates a task request message.
     *
     * <p>Use this when an agent needs assistance with a task.</p>
     *
     * @param senderId Sender agent ID
     * @param requestDescription What the agent needs help with
     * @param taskDetails Additional task parameters (location, type, priority, etc.)
     * @return Task request message
     */
    public static AgentMessage taskRequest(UUID senderId, String requestDescription,
                                          Map<String, Object> taskDetails) {
        Map<String, Object> payload = new HashMap<>(taskDetails);
        payload.put("requestType", "task_help");

        return new AgentMessage.Builder()
            .sender(senderId)
            .type(AgentMessage.MessageType.TASK_REQUEST)
            .content(requestDescription)
            .payload(payload)
            .build();
    }

    /**
     * Response types for task requests.
     */
    public enum TaskResponse {
        /** Agent accepts the task */
        ACCEPT,

        /** Agent declines the task */
        DECLINE,

        /** Agent is busy but may help later */
        BUSY,

        /** Agent needs more information */
        CLARIFY
    }

    /**
     * Creates a task response message.
     *
     * <p>Use this to respond to task requests.</p>
     *
     * @param senderId Sender agent ID
     * @param recipientId Recipient agent ID
     * @param correlationId Correlation ID from the request
     * @param response Response type
     * @param reason Additional explanation
     * @return Task response message
     */
    public static AgentMessage taskResponse(UUID senderId, UUID recipientId,
                                           UUID correlationId, TaskResponse response,
                                           String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("response", response.name());
        payload.put("reason", reason);

        return new AgentMessage.Builder()
            .sender(senderId)
            .recipient(recipientId)
            .type(AgentMessage.MessageType.RESPONSE)
            .content(response.name() + ": " + reason)
            .payload(payload)
            .correlationId(correlationId)
            .build();
    }

    // ========== Status Broadcast ==========

    /**
     * Status types for status broadcasts.
     */
    public enum AgentStatus {
        /** Agent is idle and available */
        IDLE,

        /** Agent is working on a task */
        WORKING,

        /** Agent is moving to a location */
        MOVING,

        /** Agent is in combat */
        COMBAT,

        /** Agent is stuck or needs help */
        STUCK,

        /** Agent is low on resources */
        LOW_RESOURCES,

        /** Agent is ready for new tasks */
        READY
    }

    /**
     * Creates a status broadcast message.
     *
     * <p>Status broadcasts inform other agents about current state.</p>
     *
     * @param senderId Sender agent ID
     * @param status Current status
     * @param position Current position (can be null)
     * @param currentTask Current task description (can be null)
     * @return Status broadcast message
     */
    public static AgentMessage statusBroadcast(UUID senderId, AgentStatus status,
                                              BlockPos position, String currentTask) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", status.name());
        payload.put("timestamp", System.currentTimeMillis());

        if (position != null) {
            payload.put("position", position);
            payload.put("x", position.getX());
            payload.put("y", position.getY());
            payload.put("z", position.getZ());
        }

        if (currentTask != null) {
            payload.put("currentTask", currentTask);
        }

        return new AgentMessage.Builder()
            .sender(senderId)
            .type(AgentMessage.MessageType.STATUS_UPDATE)
            .content(status.name())
            .payload(payload)
            .build();
    }

    /**
     * Creates a position update message.
     *
     * <p>Position updates help agents track each other's locations.</p>
     *
     * @param senderId Sender agent ID
     * @param position Current position
     * @param movementTarget Target destination (can be null)
     * @return Position update message
     */
    public static AgentMessage positionUpdate(UUID senderId, BlockPos position,
                                             BlockPos movementTarget) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("position", position);
        payload.put("x", position.getX());
        payload.put("y", position.getY());
        payload.put("z", position.getZ());

        if (movementTarget != null) {
            payload.put("target", movementTarget);
            payload.put("targetX", movementTarget.getX());
            payload.put("targetY", movementTarget.getY());
            payload.put("targetZ", movementTarget.getZ());
        }

        return new AgentMessage.Builder()
            .sender(senderId)
            .type(AgentMessage.MessageType.STATUS_UPDATE)
            .content("At " + position.toShortString())
            .payload(payload)
            .build();
    }

    // ========== Alert Messages ==========

    /**
     * Alert types for urgent notifications.
     */
    public enum AlertType {
        /** Dangerous mob detected */
        DANGER,

        /** Valuable resource discovered */
        DISCOVERY,

        /** Structure completed */
        MILESTONE,

        /** Task failed */
        FAILURE,

        /** Agent needs immediate assistance */
        EMERGENCY,

        /** Environmental hazard (lava, cliff, etc.) */
        HAZARD,

        /** Enemy player detected */
        ENEMY
    }

    /**
     * Creates an alert message.
     *
     * <p>Alerts are urgent notifications that require immediate attention.</p>
     *
     * @param senderId Sender agent ID
     * @param alertType Type of alert
     * @param location Location of the alert (can be null)
     * @param description Alert description
     * @return Alert message
     */
    public static AgentMessage alert(UUID senderId, AlertType alertType,
                                     BlockPos location, String description) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("alertType", alertType.name());
        payload.put("urgency", getUrgencyForAlert(alertType));

        if (location != null) {
            payload.put("location", location);
            payload.put("x", location.getX());
            payload.put("y", location.getY());
            payload.put("z", location.getZ());
        }

        return new AgentMessage.Builder()
            .sender(senderId)
            .type(AgentMessage.MessageType.ALERT)
            .content("[" + alertType.name() + "] " + description)
            .payload(payload)
            .build();
    }

    private static String getUrgencyForAlert(AlertType alertType) {
        return switch (alertType) {
            case DANGER, EMERGENCY, HAZARD, ENEMY -> "critical";
            case FAILURE -> "high";
            case DISCOVERY, MILESTONE -> "normal";
        };
    }

    // ========== Resource Sharing ==========

    /**
     * Creates a resource share message.
     *
     * <p>Use this to share resource discoveries with other agents.</p>
     *
     * @param senderId Sender agent ID
     * @param resourceType Type of resource (e.g., "diamond_ore", "iron_ore")
     * @param location Resource location
     * @param quantity Estimated quantity
     * @return Resource share message
     */
    public static AgentMessage resourceShare(UUID senderId, String resourceType,
                                            BlockPos location, int quantity) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("resourceType", resourceType);
        payload.put("location", location);
        payload.put("x", location.getX());
        payload.put("y", location.getY());
        payload.put("z", location.getZ());
        payload.put("quantity", quantity);
        payload.put("timestamp", System.currentTimeMillis());

        return new AgentMessage.Builder()
            .sender(senderId)
            .type(AgentMessage.MessageType.COORDINATION)
            .content("Found " + quantity + " " + resourceType + " at " + location.toShortString())
            .payload(payload)
            .build();
    }

    // ========== Coordination Messages ==========

    /**
     * Creates a coordination request message.
     *
     * <p>Use this to coordinate actions with other agents.</p>
     *
     * @param senderId Sender agent ID
     * @param recipientId Recipient agent ID (null for broadcast)
     * @param coordinationType Type of coordination needed
     * @param details Coordination details
     * @return Coordination request message
     */
    public static AgentMessage coordinationRequest(UUID senderId, UUID recipientId,
                                                   String coordinationType,
                                                   Map<String, Object> details) {
        Map<String, Object> payload = new HashMap<>(details);
        payload.put("coordinationType", coordinationType);

        return new AgentMessage.Builder()
            .sender(senderId)
            .recipient(recipientId)
            .type(AgentMessage.MessageType.COORDINATION)
            .content(coordinationType)
            .payload(payload)
            .build();
    }

    /**
     * Creates a collaborative building request.
     *
     * <p>Use this to request help with building tasks.</p>
     *
     * @param senderId Sender agent ID
     * @param structureType Type of structure
     * @param location Build location
     * @param sections Number of sections needing help
     * @return Building coordination message
     */
    public static AgentMessage buildingCoordination(UUID senderId, String structureType,
                                                   BlockPos location, int sections) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("coordinationType", "collaborative_build");
        payload.put("structureType", structureType);
        payload.put("location", location);
        payload.put("x", location.getX());
        payload.put("y", location.getY());
        payload.put("z", location.getZ());
        payload.put("sections", sections);

        return new AgentMessage.Builder()
            .sender(senderId)
            .type(AgentMessage.MessageType.COORDINATION)
            .content("Building " + structureType + " at " + location.toShortString())
            .payload(payload)
            .build();
    }

    // ========== Query/Response ==========

    /**
     * Creates a query message.
     *
     * <p>Use this to request information from other agents.</p>
     *
     * @param senderId Sender agent ID
     * @param recipientId Recipient agent ID (null for broadcast)
     * @param queryType Type of query
     * @param queryParameters Query parameters
     * @return Query message
     */
    public static AgentMessage query(UUID senderId, UUID recipientId,
                                    String queryType, Map<String, Object> queryParameters) {
        Map<String, Object> payload = new HashMap<>(queryParameters);
        payload.put("queryType", queryType);

        return new AgentMessage.Builder()
            .sender(senderId)
            .recipient(recipientId)
            .type(AgentMessage.MessageType.QUERY)
            .content(queryType)
            .payload(payload)
            .build();
    }

    /**
     * Creates a query response message.
     *
     * <p>Use this to respond to queries.</p>
     *
     * @param senderId Sender agent ID
     * @param recipientId Recipient agent ID
     * @param correlationId Correlation ID from the query
     * @param responseData Response data
     * @return Query response message
     */
    public static AgentMessage queryResponse(UUID senderId, UUID recipientId,
                                            UUID correlationId,
                                            Map<String, Object> responseData) {
        return new AgentMessage.Builder()
            .sender(senderId)
            .recipient(recipientId)
            .type(AgentMessage.MessageType.RESPONSE)
            .content("Query response")
            .payload(responseData)
            .correlationId(correlationId)
            .build();
    }

    // ========== Task Result ==========

    /**
     * Result types for task completion.
     */
    public enum TaskResult {
        /** Task completed successfully */
        SUCCESS,

        /** Task failed */
        FAILURE,

        /** Task partially completed */
        PARTIAL,

        /** Task was cancelled */
        CANCELLED
    }

    /**
     * Creates a task result message.
     *
     * <p>Use this to report task completion results.</p>
     *
     * @param senderId Sender agent ID
     * @param recipientId Recipient agent ID (foreman or null for broadcast)
     * @param taskId Task identifier
     * @param result Task result
     * @param details Result details
     * @return Task result message
     */
    public static AgentMessage taskResult(UUID senderId, UUID recipientId,
                                         String taskId, TaskResult result,
                                         Map<String, Object> details) {
        Map<String, Object> payload = new HashMap<>(details);
        payload.put("taskId", taskId);
        payload.put("result", result.name());

        return new AgentMessage.Builder()
            .sender(senderId)
            .recipient(recipientId)
            .type(AgentMessage.MessageType.TASK_RESULT)
            .content("Task " + result.name() + ": " + taskId)
            .payload(payload)
            .build();
    }
}
