package com.minewright.communication;

import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Per-agent communication interface for sending and receiving messages.
 *
 * <p>The AgentRadio provides a high-level API for agents to communicate
 * with each other. It wraps the CommunicationBus and provides convenience
 * methods for common communication patterns.</p>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Status broadcasting</li>
 *   <li>Help/task requests</li>
 *   <li>Alert broadcasting</li>
 *   <li>Resource sharing</li>
 *   <li>Query/response handling</li>
 *   <li>Request/response correlation</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * // Create radio for agent
 * AgentRadio radio = new AgentRadio(agentId, "Agent1", bus);
 * radio.register();
 *
 * // Broadcast status
 * radio.broadcastStatus("Working on building house");
 *
 * // Request help
 * radio.requestHelp("I need help mining at " + position);
 *
 * // Send alert
 * radio.alert("Found diamonds!", new BlockPos(100, 12, 50));
 *
 * // Process incoming messages (call each tick)
 * radio.tick();
 * </pre>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe. Methods can be called
 * from any thread.</p>
 *
 * @since 1.3.0
 */
public class AgentRadio {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentRadio.class);

    /**
     * Default timeout for request/response (10 seconds).
     */
    private static final long DEFAULT_TIMEOUT = 10000;

    private final UUID agentId;
    private final String agentName;
    private final CommunicationBus bus;

    /**
     * Pending requests waiting for responses.
     */
    private final Map<UUID, PendingRequest> pendingRequests = new HashMap<>();

    /**
     * Current status of this agent.
     */
    private volatile String currentStatus = "Idle";

    /**
     * Current position of this agent.
     */
    private volatile BlockPos currentPosition;

    /**
     * Whether the radio is enabled.
     */
    private volatile boolean enabled = true;

    /**
     * Creates a new agent radio.
     *
     * @param agentId Agent's unique ID
     * @param agentName Agent's display name
     * @param bus Communication bus
     */
    public AgentRadio(UUID agentId, String agentName, CommunicationBus bus) {
        this.agentId = Objects.requireNonNull(agentId, "agentId cannot be null");
        this.agentName = Objects.requireNonNull(agentName, "agentName cannot be null");
        this.bus = Objects.requireNonNull(bus, "bus cannot be null");
    }

    /**
     * Registers this radio with the communication bus.
     */
    public void register() {
        bus.register(agentId, this::handleMessage);
        LOGGER.debug("Registered radio for agent: {}", agentName);
    }

    /**
     * Unregisters this radio from the communication bus.
     */
    public void unregister() {
        bus.unregister(agentId);
        LOGGER.debug("Unregistered radio for agent: {}", agentName);
    }

    /**
     * Processes messages (call each tick).
     *
     * <p>This method checks for timed out requests and performs any
     * periodic maintenance.</p>
     */
    public void tick() {
        if (!enabled) {
            return;
        }

        // Check for timed out requests
        long now = System.currentTimeMillis();
        pendingRequests.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired(now)) {
                LOGGER.debug("Request timed out: {}", entry.getKey());
                entry.getValue().complete(null); // Complete with null = timeout
                return true;
            }
            return false;
        });
    }

    /**
     * Gets the agent ID.
     *
     * @return Agent ID
     */
    public UUID getAgentId() {
        return agentId;
    }

    /**
     * Gets the agent name.
     *
     * @return Agent name
     */
    public String getAgentName() {
        return agentName;
    }

    /**
     * Checks if the radio is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether the radio is enabled.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the current status.
     *
     * @return Current status
     */
    public String getCurrentStatus() {
        return currentStatus;
    }

    /**
     * Sets the current status.
     *
     * @param status Current status
     */
    public void setCurrentStatus(String status) {
        this.currentStatus = status;
    }

    /**
     * Gets the current position.
     *
     * @return Current position
     */
    public BlockPos getCurrentPosition() {
        return currentPosition;
    }

    /**
     * Sets the current position.
     *
     * @param position Current position
     */
    public void setCurrentPosition(BlockPos position) {
        this.currentPosition = position;
    }

    // ========== Broadcasting ==========

    /**
     * Broadcasts a status update.
     *
     * @param status Status message
     */
    public void broadcastStatus(String status) {
        if (!enabled) return;

        this.currentStatus = status;

        Map<String, Object> payload = new HashMap<>();
        payload.put("status", status);
        if (currentPosition != null) {
            payload.put("position", currentPosition);
        }

        bus.broadcast(agentId, AgentMessage.MessageType.STATUS_UPDATE,
            status, payload);

        LOGGER.debug("[{}] Broadcast status: {}", agentName, status);
    }

    /**
     * Broadcasts a status update with protocol messages.
     *
     * @param status Status enum
     * @param currentTask Current task (can be null)
     */
    public void broadcastStatus(ProtocolMessages.AgentStatus status, String currentTask) {
        if (!enabled) return;

        AgentMessage message = ProtocolMessages.statusBroadcast(
            agentId, status, currentPosition, currentTask);
        bus.send(message);

        LOGGER.debug("[{}] Broadcast status: {}", agentName, status);
    }

    /**
     * Broadcasts an alert.
     *
     * @param alert Alert message
     */
    public void alert(String alert) {
        if (!enabled) return;

        bus.broadcast(agentId, AgentMessage.MessageType.ALERT, alert);

        LOGGER.debug("[{}] Broadcast alert: {}", agentName, alert);
    }

    /**
     * Broadcasts an alert with type and location.
     *
     * @param alertType Type of alert
     * @param location Location of the alert
     * @param description Alert description
     */
    public void alert(ProtocolMessages.AlertType alertType,
                     BlockPos location, String description) {
        if (!enabled) return;

        AgentMessage message = ProtocolMessages.alert(
            agentId, alertType, location, description);
        bus.send(message);

        LOGGER.debug("[{}] Broadcast alert: {} - {}",
            agentName, alertType, description);
    }

    // ========== Help/Task Requests ==========

    /**
     * Requests help from other agents.
     *
     * @param request Help request description
     */
    public void requestHelp(String request) {
        if (!enabled) return;

        Map<String, Object> details = new HashMap<>();
        details.put("requester", agentName);
        if (currentPosition != null) {
            details.put("location", currentPosition);
        }

        AgentMessage message = ProtocolMessages.taskRequest(
            agentId, request, details);
        bus.send(message);

        LOGGER.debug("[{}] Requested help: {}", agentName, request);
    }

    /**
     * Requests help with specific details.
     *
     * @param requestDescription Request description
     * @param taskDetails Task details
     */
    public void requestHelp(String requestDescription, Map<String, Object> taskDetails) {
        if (!enabled) return;

        AgentMessage message = ProtocolMessages.taskRequest(
            agentId, requestDescription, taskDetails);
        bus.send(message);

        LOGGER.debug("[{}] Requested help: {}", agentName, requestDescription);
    }

    /**
     * Responds to a task request.
     *
     * @param recipientId Requesting agent
     * @param correlationId Request correlation ID
     * @param response Response type
     * @param reason Response reason
     */
    public void respondTo(UUID recipientId, UUID correlationId,
                         ProtocolMessages.TaskResponse response, String reason) {
        if (!enabled) return;

        AgentMessage message = ProtocolMessages.taskResponse(
            agentId, recipientId, correlationId, response, reason);
        bus.send(message);

        LOGGER.debug("[{}] Responded to {} request: {}",
            agentName, response, reason);
    }

    // ========== Resource Sharing ==========

    /**
     * Shares a resource discovery.
     *
     * @param resourceType Type of resource
     * @param location Resource location
     * @param quantity Estimated quantity
     */
    public void shareResource(String resourceType, BlockPos location, int quantity) {
        if (!enabled) return;

        AgentMessage message = ProtocolMessages.resourceShare(
            agentId, resourceType, location, quantity);
        bus.send(message);

        LOGGER.debug("[{}] Shared resource: {} at {} (qty: {})",
            agentName, resourceType, location, quantity);
    }

    // ========== Query/Response ==========

    /**
     * Sends a query and waits for response.
     *
     * @param recipientId Recipient agent
     * @param queryType Type of query
     * @param parameters Query parameters
     * @return Response data
     */
    public Map<String, Object> query(UUID recipientId, String queryType,
                                     Map<String, Object> parameters) {
        if (!enabled) return Collections.emptyMap();

        try {
            AgentMessage request = ProtocolMessages.query(
                agentId, recipientId, queryType, parameters);

            AgentMessage response = bus.sendRequest(request, DEFAULT_TIMEOUT);

            if (response != null) {
                return response.payload();
            }

            return Collections.emptyMap();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("[{}] Query interrupted", agentName);
            return Collections.emptyMap();
        } catch (Exception e) {
            LOGGER.warn("[{}] Query failed: {}", agentName, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Sends a query asynchronously.
     *
     * @param recipientId Recipient agent
     * @param queryType Type of query
     * @param parameters Query parameters
     * @param callback Response callback
     */
    public void queryAsync(UUID recipientId, String queryType,
                          Map<String, Object> parameters,
                          java.util.function.Consumer<Map<String, Object>> callback) {
        if (!enabled) return;

        // Create async request
        new Thread(() -> {
            Map<String, Object> response = query(recipientId, queryType, parameters);
            callback.accept(response);
        }, "AgentRadio-Query-" + agentName).start();
    }

    /**
     * Sends a direct message to another agent.
     *
     * @param recipientId Recipient agent
     * @param type Message type
     * @param content Message content
     */
    public void sendMessage(UUID recipientId, AgentMessage.MessageType type,
                           String content) {
        if (!enabled) return;

        AgentMessage message = new AgentMessage.Builder()
            .sender(agentId)
            .recipient(recipientId)
            .type(type)
            .content(content)
            .build();

        bus.send(message);

        LOGGER.debug("[{}] Sent {} message to {}: {}",
            agentName, type, recipientId, content);
    }

    /**
     * Sends a message with payload.
     *
     * @param recipientId Recipient agent
     * @param type Message type
     * @param content Message content
     * @param payload Message payload
     */
    public void sendMessage(UUID recipientId, AgentMessage.MessageType type,
                           String content, Map<String, Object> payload) {
        if (!enabled) return;

        AgentMessage message = new AgentMessage.Builder()
            .sender(agentId)
            .recipient(recipientId)
            .type(type)
            .content(content)
            .payload(payload)
            .build();

        bus.send(message);
    }

    // ========== Internal Message Handling ==========

    /**
     * Handles an incoming message.
     *
     * @param message Incoming message
     */
    private void handleMessage(AgentMessage message) {
        if (!enabled) return;

        LOGGER.debug("[{}] Received message: {} from {}",
            agentName, message.type(),
            message.senderId().toString().substring(0, 8));

        // Check if this is a response to a pending request
        if (message.isResponse() && message.correlationId() != null) {
            PendingRequest pending = pendingRequests.remove(message.correlationId());
            if (pending != null) {
                pending.complete(message);
                return;
            }
        }

        // Handle different message types
        switch (message.type()) {
            case STATUS_UPDATE -> handleStatusUpdate(message);
            case TASK_REQUEST -> handleTaskRequest(message);
            case ALERT -> handleAlert(message);
            case QUERY -> handleQuery(message);
            case COORDINATION -> handleCoordination(message);
            default -> {
                // Other types are handled by specific protocols
            }
        }
    }

    private void handleStatusUpdate(AgentMessage message) {
        // Status updates are logged but not specifically handled
        // Subclasses can override to track peer status
        LOGGER.debug("[{}] Status update from {}: {}",
            agentName,
            message.senderId().toString().substring(0, 8),
            message.content());
    }

    private void handleTaskRequest(AgentMessage message) {
        // Task requests are logged - specific handling in subclasses
        LOGGER.debug("[{}] Task request from {}: {}",
            agentName,
            message.senderId().toString().substring(0, 8),
            message.content());
    }

    private void handleAlert(AgentMessage message) {
        // Alerts are important - log prominently
        LOGGER.info("[{}] ALERT from {}: {}",
            agentName,
            message.senderId().toString().substring(0, 8),
            message.content());
    }

    private void handleQuery(AgentMessage message) {
        // Queries require responses - subclasses should override
        LOGGER.debug("[{}] Query from {}: {}",
            agentName,
            message.senderId().toString().substring(0, 8),
            message.content());
    }

    private void handleCoordination(AgentMessage message) {
        LOGGER.debug("[{}] Coordination from {}: {}",
            agentName,
            message.senderId().toString().substring(0, 8),
            message.content());
    }

    /**
     * Represents a pending request waiting for response.
     */
    private static class PendingRequest {
        private final long expirationTime;
        private final java.util.function.Consumer<AgentMessage> callback;

        PendingRequest(long timeout, java.util.function.Consumer<AgentMessage> callback) {
            this.expirationTime = System.currentTimeMillis() + timeout;
            this.callback = callback;
        }

        boolean isExpired(long now) {
            return now >= expirationTime;
        }

        void complete(AgentMessage response) {
            if (callback != null) {
                callback.accept(response);
            }
        }
    }

    /**
     * Gets the number of pending requests.
     *
     * @return Pending request count
     */
    public int getPendingRequestCount() {
        return pendingRequests.size();
    }

    /**
     * Gets communication statistics.
     *
     * @return Communication statistics
     */
    public CommunicationBus.MessageStats getStats() {
        return bus.getStats();
    }
}
