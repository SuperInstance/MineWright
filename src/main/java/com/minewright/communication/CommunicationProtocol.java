package com.minewright.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * High-level communication protocols for agent coordination.
 *
 * <p>This class implements standard multi-step protocols for common
 * coordination scenarios, building on the basic messaging infrastructure.
 * Each protocol handles the message exchange pattern and state management.</p>
 *
 * <p><b>Supported Protocols:</b></p>
 * <ul>
 *   <li>Handshake Protocol - Establish communication channel</li>
 *   <li>Task Negotiation - Contract Net pattern for task allocation</li>
 *   <li>Conflict Resolution - Resolve disputes between agents</li>
 *   <li>Status Sync - Periodic status synchronization</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * CommunicationProtocol protocols = new CommunicationProtocol(bus);
 *
 * // Initiate handshake
 * protocols.getHandshakeProtocol().initiate(myId, otherAgent, response -> {
 *     if (response.isSuccess()) {
 *         System.out.println("Handshake successful!");
 *     }
 * });
 *
 * // Initiate task negotiation
 * protocols.getTaskNegotiationProtocol().announceTask(
 *     taskId,
 *     "Build house",
 *     taskParams,
 *     bids -> {
 *         // Select best bid
 *         Bid best = selectBestBid(bids);
 *         protocols.getTaskNegotiationProtocol().awardTask(best);
 *     }
 * );
 * </pre>
 *
 * @since 1.3.0
 */
public class CommunicationProtocol {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommunicationProtocol.class);

    private final CommunicationBus bus;
    private final HandshakeProtocol handshakeProtocol;
    private final TaskNegotiationProtocol taskNegotiationProtocol;
    private final ConflictResolutionProtocol conflictResolutionProtocol;
    private final StatusSyncProtocol statusSyncProtocol;

    /**
     * Creates a new protocol handler.
     *
     * @param bus Communication bus for message transport
     */
    public CommunicationProtocol(CommunicationBus bus) {
        this.bus = Objects.requireNonNull(bus, "bus cannot be null");
        this.handshakeProtocol = new HandshakeProtocol(bus);
        this.taskNegotiationProtocol = new TaskNegotiationProtocol(bus);
        this.conflictResolutionProtocol = new ConflictResolutionProtocol(bus);
        this.statusSyncProtocol = new StatusSyncProtocol(bus);

        LOGGER.info("CommunicationProtocol initialized");
    }

    /**
     * Gets the handshake protocol.
     *
     * @return Handshake protocol instance
     */
    public HandshakeProtocol getHandshakeProtocol() {
        return handshakeProtocol;
    }

    /**
     * Gets the task negotiation protocol.
     *
     * @return Task negotiation protocol instance
     */
    public TaskNegotiationProtocol getTaskNegotiationProtocol() {
        return taskNegotiationProtocol;
    }

    /**
     * Gets the conflict resolution protocol.
     *
     * @return Conflict resolution protocol instance
     */
    public ConflictResolutionProtocol getConflictResolutionProtocol() {
        return conflictResolutionProtocol;
    }

    /**
     * Gets the status sync protocol.
     *
     * @return Status sync protocol instance
     */
    public StatusSyncProtocol getStatusSyncProtocol() {
        return statusSyncProtocol;
    }

    // ========== Handshake Protocol ==========

    /**
     * Protocol for establishing communication between agents.
     *
     * <p>Handshake flow:</p>
     * <ol>
     *   <li>Initiator sends HANDSHAKE_INIT</li>
     *   <li>Responder sends HANDSHAKE_ACK with capabilities</li>
     *   <li>Initiator sends HANDSHAKE_CONFIRM</li>
     * </ol>
     */
    public static class HandshakeProtocol {
        private static final Logger LOGGER = LoggerFactory.getLogger(HandshakeProtocol.class);
        private static final String PROTOCOL_NAME = "handshake";

        private final CommunicationBus bus;
        private final Map<UUID, HandshakeState> pendingHandshakes = new ConcurrentHashMap<>();

        public HandshakeProtocol(CommunicationBus bus) {
            this.bus = bus;
        }

        /**
         * Initiates a handshake with another agent.
         *
         * @param initiatorId Initiator agent ID
         * @param responderId Responder agent ID
         * @param capabilities Initiator capabilities
         * @param callback Callback when handshake completes
         */
        public void initiate(UUID initiatorId, UUID responderId,
                            Map<String, Object> capabilities,
                            Consumer<HandshakeResult> callback) {
            UUID handshakeId = UUID.randomUUID();

            // Store handshake state
            pendingHandshakes.put(handshakeId, new HandshakeState(
                handshakeId, initiatorId, responderId,
                HandshakePhase.AWAITING_ACK, callback));

            // Send handshake init
            Map<String, Object> payload = new HashMap<>(capabilities);
            payload.put("handshakeId", handshakeId);
            payload.put("timestamp", System.currentTimeMillis());

            AgentMessage init = new AgentMessage.Builder()
                .sender(initiatorId)
                .recipient(responderId)
                .type(AgentMessage.MessageType.QUERY)
                .content("HANDSHAKE_INIT")
                .payload(payload)
                .build();

            bus.send(init);

            LOGGER.debug("Initiated handshake {} from {} to {}", handshakeId, initiatorId, responderId);
        }

        /**
         * Handles an incoming handshake message.
         *
         * @param message Handshake message
         */
        public void handleHandshake(AgentMessage message) {
            String content = message.content();
            UUID handshakeId = message.getPayload("handshakeId");

            if ("HANDSHAKE_INIT".equals(content)) {
                handleInit(message, handshakeId);
            } else if ("HANDSHAKE_ACK".equals(content)) {
                handleAck(message, handshakeId);
            } else if ("HANDSHAKE_CONFIRM".equals(content)) {
                handleConfirm(message, handshakeId);
            }
        }

        private void handleInit(AgentMessage message, UUID handshakeId) {
            // Send ACK
            Map<String, Object> capabilities = new HashMap<>();
            capabilities.put("version", "1.0");
            capabilities.put("supportedMessageTypes", Arrays.asList(
                AgentMessage.MessageType.values()));

            Map<String, Object> payload = new HashMap<>();
            payload.put("handshakeId", handshakeId);
            payload.put("capabilities", capabilities);

            AgentMessage ack = new AgentMessage.Builder()
                .sender(message.recipientId())
                .recipient(message.senderId())
                .type(AgentMessage.MessageType.RESPONSE)
                .content("HANDSHAKE_ACK")
                .payload(payload)
                .correlationId(handshakeId)
                .build();

            bus.send(ack);

            LOGGER.debug("Sent HANDSHAKE_ACK for handshake {}", handshakeId);
        }

        private void handleAck(AgentMessage message, UUID handshakeId) {
            // Send confirm
            Map<String, Object> payload = new HashMap<>();
            payload.put("handshakeId", handshakeId);
            payload.put("accepted", true);

            AgentMessage confirm = new AgentMessage.Builder()
                .sender(message.recipientId())
                .recipient(message.senderId())
                .type(AgentMessage.MessageType.RESPONSE)
                .content("HANDSHAKE_CONFIRM")
                .payload(payload)
                .correlationId(handshakeId)
                .build();

            bus.send(confirm);

            // Complete handshake
            HandshakeState state = pendingHandshakes.remove(handshakeId);
            if (state != null && state.callback != null) {
                Map<String, Object> responderCaps = message.getPayload("capabilities", new HashMap<>());
                state.callback.accept(new HandshakeResult(true, responderCaps));
            }

            LOGGER.debug("Completed handshake {}", handshakeId);
        }

        private void handleConfirm(AgentMessage message, UUID handshakeId) {
            LOGGER.debug("Handshake {} confirmed by {}", handshakeId, message.senderId());
        }

        private record HandshakeState(
            UUID handshakeId,
            UUID initiatorId,
            UUID responderId,
            HandshakePhase phase,
            Consumer<HandshakeResult> callback
        ) {}

        private enum HandshakePhase {
            AWAITING_ACK, AWAITING_CONFIRM, COMPLETED
        }

        /**
         * Result of a handshake.
         */
        public record HandshakeResult(
            boolean success,
            Map<String, Object> peerCapabilities
        ) {
            public boolean isSuccess() {
                return success;
            }
        }
    }

    // ========== Task Negotiation Protocol ==========

    /**
     * Contract Net protocol for task negotiation.
     *
     * <p>Negotiation flow:</p>
     * <ol>
     *   <li>Manager announces task (call for proposals)</li>
     *   <li>Contractors submit bids</li>
     *   <li>Manager evaluates bids and awards task</li>
     *   <li>Selected contractor confirms or declines</li>
     * </ol>
     */
    public static class TaskNegotiationProtocol {
        private static final Logger LOGGER = LoggerFactory.getLogger(TaskNegotiationProtocol.class);

        private final CommunicationBus bus;
        private final Map<String, TaskAnnouncement> activeAnnouncements = new ConcurrentHashMap<>();

        public TaskNegotiationProtocol(CommunicationBus bus) {
            this.bus = bus;
        }

        /**
         * Announces a task for bidding (Contract Net call for proposals).
         *
         * @param managerId Manager (foreman) agent ID
         * @param taskId Task identifier
         * @param taskDescription Task description
         * @param taskParams Task parameters
         * @param timeoutMs Bid timeout in milliseconds
         * @param callback Callback when bidding completes
         */
        public void announceTask(UUID managerId, String taskId,
                                String taskDescription, Map<String, Object> taskParams,
                                long timeoutMs,
                                Consumer<List<Bid>> callback) {
            TaskAnnouncement announcement = new TaskAnnouncement(
                taskId, managerId, taskDescription, taskParams,
                System.currentTimeMillis() + timeoutMs, callback);

            activeAnnouncements.put(taskId, announcement);

            Map<String, Object> payload = new HashMap<>(taskParams);
            payload.put("taskId", taskId);
            payload.put("deadline", announcement.deadline);
            payload.put("taskDescription", taskDescription);

            AgentMessage cfp = new AgentMessage.Builder()
                .sender(managerId)
                .type(AgentMessage.MessageType.TASK_REQUEST)
                .content("CFP: " + taskDescription)
                .payload(payload)
                .build();

            bus.broadcast(managerId, AgentMessage.MessageType.TASK_REQUEST,
                "CFP: " + taskDescription, payload);

            LOGGER.debug("Announced task {} for bidding (deadline: {})",
                taskId, announcement.deadline);
        }

        /**
         * Submits a bid for a task.
         *
         * @param contractorId Contractor agent ID
         * @param taskId Task ID
         * @param estimatedTime Estimated completion time (ticks)
         * @param confidence Confidence level (0.0 to 1.0)
         * @param reasoning Bid reasoning
         */
        public void submitBid(UUID contractorId, String taskId,
                             long estimatedTime, double confidence, String reasoning) {
            TaskAnnouncement announcement = activeAnnouncements.get(taskId);
            if (announcement == null) {
                LOGGER.warn("Cannot submit bid: task {} not found", taskId);
                return;
            }

            if (System.currentTimeMillis() > announcement.deadline) {
                LOGGER.warn("Cannot submit bid: task {} bidding closed", taskId);
                return;
            }

            Bid bid = new Bid(contractorId, taskId, estimatedTime, confidence, reasoning);
            announcement.addBid(bid);

            Map<String, Object> payload = new HashMap<>();
            payload.put("taskId", taskId);
            payload.put("estimatedTime", estimatedTime);
            payload.put("confidence", confidence);
            payload.put("reasoning", reasoning);

            AgentMessage bidMsg = new AgentMessage.Builder()
                .sender(contractorId)
                .recipient(announcement.managerId)
                .type(AgentMessage.MessageType.TASK_RESULT)
                .content("BID: " + taskId)
                .payload(payload)
                .build();

            bus.send(bidMsg);

            LOGGER.debug("Submitted bid for task {} from {} (confidence: {})",
                taskId, contractorId, confidence);
        }

        /**
         * Awards task to the best bidder.
         *
         * @param taskId Task ID
         * @param selectedBid Winning bid
         */
        public void awardTask(String taskId, Bid selectedBid) {
            TaskAnnouncement announcement = activeAnnouncements.get(taskId);
            if (announcement == null) {
                LOGGER.warn("Cannot award task: {} not found", taskId);
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("taskId", taskId);
            payload.put("awarded", true);
            payload.put("taskParams", announcement.taskParams);

            AgentMessage award = new AgentMessage.Builder()
                .sender(announcement.managerId)
                .recipient(selectedBid.contractorId())
                .type(AgentMessage.MessageType.TASK_REQUEST)
                .content("TASK_AWARD: " + taskId)
                .payload(payload)
                .build();

            bus.send(award);

            // Notify other bidders they were not selected
            for (Bid bid : announcement.bids) {
                if (!bid.contractorId().equals(selectedBid.contractorId())) {
                    AgentMessage reject = new AgentMessage.Builder()
                        .sender(announcement.managerId)
                        .recipient(bid.contractorId())
                        .type(AgentMessage.MessageType.RESPONSE)
                        .content("TASK_REJECTED: " + taskId)
                        .payload(Map.of("taskId", taskId, "awarded", false))
                        .build();

                    bus.send(reject);
                }
            }

            LOGGER.debug("Awarded task {} to {}", taskId, selectedBid.contractorId());
        }

        /**
         * Checks if bidding deadline has passed.
         *
         * @param taskId Task ID
         * @return true if deadline passed
         */
        public boolean isDeadlinePassed(String taskId) {
            TaskAnnouncement announcement = activeAnnouncements.get(taskId);
            return announcement != null && System.currentTimeMillis() > announcement.deadline;
        }

        /**
         * Gets bids for a task.
         *
         * @param taskId Task ID
         * @return List of bids
         */
        public List<Bid> getBids(String taskId) {
            TaskAnnouncement announcement = activeAnnouncements.get(taskId);
            return announcement != null ?
                new ArrayList<>(announcement.bids) : Collections.emptyList();
        }

        /**
         * Closes bidding and invokes callback.
         *
         * @param taskId Task ID
         */
        public void closeBidding(String taskId) {
            TaskAnnouncement announcement = activeAnnouncements.remove(taskId);
            if (announcement != null && announcement.callback != null) {
                announcement.callback.accept(new ArrayList<>(announcement.bids));
            }
        }

        private static class TaskAnnouncement {
            final String taskId;
            final UUID managerId;
            final String taskDescription;
            final Map<String, Object> taskParams;
            final long deadline;
            final Consumer<List<Bid>> callback;
            final List<Bid> bids = new ArrayList<>();

            TaskAnnouncement(String taskId, UUID managerId, String taskDescription,
                           Map<String, Object> taskParams, long deadline,
                           Consumer<List<Bid>> callback) {
                this.taskId = taskId;
                this.managerId = managerId;
                this.taskDescription = taskDescription;
                this.taskParams = taskParams;
                this.deadline = deadline;
                this.callback = callback;
            }

            synchronized void addBid(Bid bid) {
                bids.add(bid);
            }
        }

        /**
         * Bid for a task.
         */
        public record Bid(
            UUID contractorId,
            String taskId,
            long estimatedTime,
            double confidence,
            String reasoning
        ) {
            /**
             * Gets the bid score (higher is better).
             */
            public double getScore() {
                // Simple scoring: confidence / time
                return confidence / (estimatedTime / 1000.0 + 1.0);
            }
        }
    }

    // ========== Conflict Resolution Protocol ==========

    /**
     * Protocol for resolving conflicts between agents.
     *
     * <p>Handles disputes over resources, tasks, or locations.</p>
     */
    public static class ConflictResolutionProtocol {
        private static final Logger LOGGER = LoggerFactory.getLogger(ConflictResolutionProtocol.class);

        private final CommunicationBus bus;

        public ConflictResolutionProtocol(CommunicationBus bus) {
            this.bus = bus;
        }

        /**
         * Initiates conflict resolution.
         *
         * @param initiatorId Initiating agent
         * @param otherId Other party in conflict
         * @param conflictType Type of conflict
         * @param conflictDetails Conflict details
         * @param proposedResolution Proposed resolution
         */
        public void initiateResolution(UUID initiatorId, UUID otherId,
                                       String conflictType,
                                       Map<String, Object> conflictDetails,
                                       String proposedResolution) {
            Map<String, Object> payload = new HashMap<>(conflictDetails);
            payload.put("conflictType", conflictType);
            payload.put("proposedResolution", proposedResolution);
            payload.put("initiator", initiatorId.toString());

            AgentMessage message = new AgentMessage.Builder()
                .sender(initiatorId)
                .recipient(otherId)
                .type(AgentMessage.MessageType.COORDINATION)
                .content("CONFLICT_RESOLVE: " + conflictType)
                .payload(payload)
                .build();

            bus.send(message);

            LOGGER.debug("Initiated conflict resolution for {} between {} and {}",
                conflictType, initiatorId, otherId);
        }

        /**
         * Responds to a conflict resolution proposal.
         *
         * @param responderId Responding agent
         * @param initiatorId Initiating agent
         * @param accept Whether to accept the proposal
         * @param counterProposal Alternative proposal (if not accepting)
         */
        public void respond(UUID responderId, UUID initiatorId,
                           boolean accept, String counterProposal) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("accepted", accept);
            if (counterProposal != null) {
                payload.put("counterProposal", counterProposal);
            }

            AgentMessage response = new AgentMessage.Builder()
                .sender(responderId)
                .recipient(initiatorId)
                .type(AgentMessage.MessageType.RESPONSE)
                .content(accept ? "CONFLICT_ACCEPTED" : "CONFLICT_COUNTER")
                .payload(payload)
                .build();

            bus.send(response);

            LOGGER.debug("Responded to conflict resolution: {}", accept ? "accept" : "counter");
        }
    }

    // ========== Status Sync Protocol ==========

    /**
     * Protocol for periodic status synchronization.
     *
     * <p>Agents periodically broadcast their status to keep everyone informed.</p>
     */
    public static class StatusSyncProtocol {
        private static final Logger LOGGER = LoggerFactory.getLogger(StatusSyncProtocol.class);

        private final CommunicationBus bus;
        private final Map<UUID, Long> lastSyncTimes = new ConcurrentHashMap<>();

        // Default sync interval: 30 seconds
        private static final long DEFAULT_SYNC_INTERVAL = 30000;

        public StatusSyncProtocol(CommunicationBus bus) {
            this.bus = bus;
        }

        /**
         * Broadcasts status update.
         *
         * @param agentId Agent ID
         * @param status Status information
         */
        public void broadcastStatus(UUID agentId, Map<String, Object> status) {
            Map<String, Object> payload = new HashMap<>(status);
            payload.put("timestamp", System.currentTimeMillis());

            bus.broadcast(agentId, AgentMessage.MessageType.STATUS_UPDATE,
                "STATUS_SYNC", payload);

            lastSyncTimes.put(agentId, System.currentTimeMillis());

            LOGGER.debug("Broadcast status for {}", agentId);
        }

        /**
         * Checks if an agent should sync (based on time since last sync).
         *
         * @param agentId Agent ID
         * @param intervalMs Sync interval
         * @return true if should sync
         */
        public boolean shouldSync(UUID agentId, long intervalMs) {
            Long lastSync = lastSyncTimes.get(agentId);
            if (lastSync == null) {
                return true;
            }
            return System.currentTimeMillis() - lastSync > intervalMs;
        }

        /**
         * Checks if an agent should sync (using default interval).
         *
         * @param agentId Agent ID
         * @return true if should sync
         */
        public boolean shouldSync(UUID agentId) {
            return shouldSync(agentId, DEFAULT_SYNC_INTERVAL);
        }

        /**
         * Requests status update from specific agent.
         *
         * @param requesterId Requesting agent
         * @param targetId Target agent
         */
        public void requestStatus(UUID requesterId, UUID targetId) {
            AgentMessage request = new AgentMessage.Builder()
                .sender(requesterId)
                .recipient(targetId)
                .type(AgentMessage.MessageType.QUERY)
                .content("STATUS_REQUEST")
                .payload(Map.of("timestamp", System.currentTimeMillis()))
                .build();

            bus.send(request);

            LOGGER.debug("Requested status from {} by {}", targetId, requesterId);
        }

        /**
         * Responds to a status request.
         *
         * @param agentId Responding agent
         * @param requesterId Requesting agent
         * @param status Status information
         */
        public void sendStatus(UUID agentId, UUID requesterId,
                              Map<String, Object> status) {
            Map<String, Object> payload = new HashMap<>(status);
            payload.put("timestamp", System.currentTimeMillis());

            AgentMessage response = new AgentMessage.Builder()
                .sender(agentId)
                .recipient(requesterId)
                .type(AgentMessage.MessageType.RESPONSE)
                .content("STATUS_RESPONSE")
                .payload(payload)
                .build();

            bus.send(response);

            LOGGER.debug("Sent status from {} to {}", agentId, requesterId);
        }
    }
}
