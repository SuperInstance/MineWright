package com.minewright.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Central message routing system for inter-agent communication.
 *
 * <p>Provides asynchronous, thread-safe message passing between agents with support for:</p>
 * <ul>
 *   <li>Direct messaging (sender → recipient)</li>
 *   <li>Broadcast messaging (sender → all)</li>
 *   <li>Message handlers per agent</li>
 *   <li>Message filtering and priority</li>
 *   <li>Request/response correlation</li>
 *   <li>Message history for debugging</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> All operations are thread-safe using concurrent collections.</p>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * CommunicationBus bus = new CommunicationBus();
 *
 * // Register agent handlers
 * bus.register(agentId, message -> {
 *     System.out.println("Received: " + message.content());
 * });
 *
 * // Send direct message
 * bus.send(new AgentMessage.Builder()
 *     .sender(senderId)
 *     .recipient(recipientId)
 *     .type(MessageType.STATUS_UPDATE)
 *     .content("Status update")
 *     .build());
 *
 * // Broadcast to all
 * bus.broadcast(senderId, MessageType.ALERT, "Danger detected!");
 *
 * // Process messages (call each tick)
 * bus.tick();
 * </pre>
 *
 * @since 1.3.0
 * @see AgentMessage
 * @see MessageHandler
 */
public class CommunicationBus {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommunicationBus.class);

    /**
     * Maximum messages per agent queue before oldest are dropped.
     */
    private static final int MAX_QUEUE_SIZE = 100;

    /**
     * Message queues for each agent.
     */
    private final Map<UUID, BlockingQueue<AgentMessage>> agentQueues;

    /**
     * Message handlers for each agent.
     */
    private final Map<UUID, MessageHandler> handlers;

    /**
     * Global message queue for processing.
     */
    private final Queue<AgentMessage> messageQueue;

    /**
     * Message history for debugging (last 1000 messages).
     */
    private final ConcurrentLinkedDeque<AgentMessage> messageHistory;

    /**
     * Correlation ID tracker for request/response.
     */
    private final Map<UUID, CompletableFuture<AgentMessage>> pendingResponses;

    /**
     * Message statistics.
     */
    private final MessageStats stats;

    /**
     * Whether the bus is running.
     */
    private volatile boolean running = true;

    /**
     * Creates a new communication bus.
     */
    public CommunicationBus() {
        this.agentQueues = new ConcurrentHashMap<>();
        this.handlers = new ConcurrentHashMap<>();
        this.messageQueue = new ConcurrentLinkedQueue<>();
        this.messageHistory = new ConcurrentLinkedDeque<>();
        this.pendingResponses = new ConcurrentHashMap<>();
        this.stats = new MessageStats();

        LOGGER.info("CommunicationBus initialized");
    }

    /**
     * Registers an agent with the communication bus.
     *
     * @param agentId Unique agent identifier
     * @param handler Message handler for this agent
     */
    public void register(UUID agentId, MessageHandler handler) {
        Objects.requireNonNull(agentId, "agentId cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");

        agentQueues.computeIfAbsent(agentId,
            k -> new LinkedBlockingQueue<>(MAX_QUEUE_SIZE));
        handlers.put(agentId, handler);

        LOGGER.debug("Registered agent: {}", agentId);
    }

    /**
     * Unregisters an agent from the communication bus.
     *
     * @param agentId Agent identifier
     */
    public void unregister(UUID agentId) {
        Objects.requireNonNull(agentId, "agentId cannot be null");

        agentQueues.remove(agentId);
        handlers.remove(agentId);

        LOGGER.debug("Unregistered agent: {}", agentId);
    }

    /**
     * Checks if an agent is registered.
     *
     * @param agentId Agent identifier
     * @return true if registered
     */
    public boolean isRegistered(UUID agentId) {
        return handlers.containsKey(agentId);
    }

    /**
     * Sends a message to its recipient.
     *
     * @param message Message to send
     */
    public void send(AgentMessage message) {
        Objects.requireNonNull(message, "message cannot be null");

        if (!running) {
            LOGGER.warn("Cannot send message: bus is not running");
            return;
        }

        // Record in history
        addToHistory(message);
        stats.recordSent(message);

        if (message.isBroadcast()) {
            // Deliver to all agents except sender
            for (UUID agentId : handlers.keySet()) {
                if (!agentId.equals(message.senderId())) {
                    deliverTo(agentId, message);
                }
            }
        } else {
            // Direct message
            if (!handlers.containsKey(message.recipientId())) {
                LOGGER.warn("Recipient not registered: {}", message.recipientId());
                stats.recordFailed();
                return;
            }
            deliverTo(message.recipientId(), message);
        }
    }

    /**
     * Delivers a message to a specific agent.
     */
    private void deliverTo(UUID agentId, AgentMessage message) {
        BlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
        if (queue != null) {
            if (!queue.offer(message)) {
                LOGGER.warn("Queue full for agent {}, dropping message", agentId);
                stats.recordDropped();
            } else {
                stats.recordDelivered();
            }
        }
    }

    /**
     * Broadcasts a message to all registered agents.
     *
     * @param senderId Sender agent ID
     * @param type Message type
     * @param content Message content
     */
    public void broadcast(UUID senderId, AgentMessage.MessageType type, String content) {
        AgentMessage message = new AgentMessage.Builder()
            .sender(senderId)
            .type(type)
            .content(content)
            .build();

        send(message);
    }

    /**
     * Broadcasts a message with payload.
     *
     * @param senderId Sender agent ID
     * @param type Message type
     * @param content Message content
     * @param payload Message payload
     */
    public void broadcast(UUID senderId, AgentMessage.MessageType type,
                         String content, Map<String, Object> payload) {
        AgentMessage message = new AgentMessage.Builder()
            .sender(senderId)
            .type(type)
            .content(content)
            .payload(payload)
            .build();

        send(message);
    }

    /**
     * Sends a request and waits for a response.
     *
     * @param request Request message
     * @param timeout Timeout in milliseconds
     * @return Response message
     * @throws InterruptedException if interrupted while waiting
     * @throws TimeoutException if timeout expires
     */
    public AgentMessage sendRequest(AgentMessage request, long timeout)
            throws InterruptedException, TimeoutException {
        Objects.requireNonNull(request, "request cannot be null");

        // Generate correlation ID
        UUID correlationId = UUID.randomUUID();

        // Create response future
        CompletableFuture<AgentMessage> responseFuture = new CompletableFuture<>();
        pendingResponses.put(correlationId, responseFuture);

        // Add correlation ID to request
        AgentMessage correlatedRequest = new AgentMessage(
            request.senderId(),
            request.recipientId(),
            request.type(),
            request.content(),
            request.payload(),
            System.currentTimeMillis(),
            correlationId
        );

        // Send request
        send(correlatedRequest);

        try {
            // Wait for response
            return responseFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            pendingResponses.remove(correlationId);
            throw new TimeoutException("No response received within " + timeout + "ms");
        } catch (ExecutionException e) {
            pendingResponses.remove(correlationId);
            throw new RuntimeException("Error while waiting for response", e.getCause());
        }
    }

    /**
     * Sends a response to a previous request.
     *
     * @param response Response message
     */
    public void sendResponse(AgentMessage response) {
        Objects.requireNonNull(response, "response cannot be null");
        Objects.requireNonNull(response.correlationId(), "correlationId is required for responses");

        CompletableFuture<AgentMessage> future = pendingResponses.remove(response.correlationId());
        if (future != null) {
            send(response);
            future.complete(response);
        } else {
            LOGGER.warn("No pending request for correlationId: {}", response.correlationId());
        }
    }

    /**
     * Processes queued messages (call each tick).
     *
     * <p>This method delivers queued messages to their handlers.
     * Should be called once per game tick.</p>
     */
    public void tick() {
        if (!running) {
            return;
        }

        // Process messages for each agent
        for (Map.Entry<UUID, BlockingQueue<AgentMessage>> entry : agentQueues.entrySet()) {
            UUID agentId = entry.getKey();
            BlockingQueue<AgentMessage> queue = entry.getValue();
            MessageHandler handler = handlers.get(agentId);

            if (handler == null) {
                // Clear queue if no handler
                queue.clear();
                continue;
            }

            // Process messages for this agent
            AgentMessage message;
            while ((message = queue.poll()) != null) {
                try {
                    handler.handle(message);
                    stats.recordReceived();
                } catch (Exception e) {
                    LOGGER.error("Error handling message for agent {}: {}",
                        agentId, e.getMessage(), e);
                    stats.recordFailed();
                }

                // Complete response future if applicable
                if (message.isResponse() && message.correlationId() != null) {
                    CompletableFuture<AgentMessage> future =
                        pendingResponses.remove(message.correlationId());
                    if (future != null) {
                        future.complete(message);
                    }
                }
            }
        }
    }

    /**
     * Gets the number of pending messages for an agent.
     *
     * @param agentId Agent identifier
     * @return Number of pending messages
     */
    public int getPendingCount(UUID agentId) {
        BlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
        return queue != null ? queue.size() : 0;
    }

    /**
     * Gets message history.
     *
     * @param count Maximum number of messages
     * @return List of recent messages
     */
    public List<AgentMessage> getHistory(int count) {
        List<AgentMessage> result = new ArrayList<>();
        Iterator<AgentMessage> iterator = messageHistory.descendingIterator();
        while (iterator.hasNext() && result.size() < count) {
            result.add(iterator.next());
        }
        return result;
    }

    /**
     * Gets message statistics.
     *
     * @return Message statistics
     */
    public MessageStats getStats() {
        return stats;
    }

    /**
     * Clears all message queues.
     */
    public void clear() {
        agentQueues.values().forEach(BlockingQueue::clear);
        messageHistory.clear();
        LOGGER.debug("CommunicationBus cleared");
    }

    /**
     * Shuts down the communication bus.
     */
    public void shutdown() {
        running = false;
        clear();
        agentQueues.clear();
        handlers.clear();
        pendingResponses.clear();
        LOGGER.info("CommunicationBus shut down");
    }

    /**
     * Checks if the bus is running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Gets all registered agent IDs.
     *
     * @return Set of agent IDs
     */
    public Set<UUID> getRegisteredAgents() {
        return Collections.unmodifiableSet(handlers.keySet());
    }

    private void addToHistory(AgentMessage message) {
        messageHistory.addLast(message);
        while (messageHistory.size() > 1000) {
            messageHistory.removeFirst();
        }
    }

    /**
     * Message statistics tracker.
     */
    public static class MessageStats {
        private final AtomicLong sent = new AtomicLong(0);
        private final AtomicLong delivered = new AtomicLong(0);
        private final AtomicLong received = new AtomicLong(0);
        private final AtomicLong dropped = new AtomicLong(0);
        private final AtomicLong failed = new AtomicLong(0);
        private final Map<AgentMessage.MessageType, AtomicLong> sentByType =
            new ConcurrentHashMap<>();

        public void recordSent(AgentMessage message) {
            sent.incrementAndGet();
            sentByType.computeIfAbsent(message.type(), k -> new AtomicLong(0))
                .incrementAndGet();
        }

        public void recordDelivered() {
            delivered.incrementAndGet();
        }

        public void recordReceived() {
            received.incrementAndGet();
        }

        public void recordDropped() {
            dropped.incrementAndGet();
        }

        public void recordFailed() {
            failed.incrementAndGet();
        }

        public long getSent() {
            return sent.get();
        }

        public long getDelivered() {
            return delivered.get();
        }

        public long getReceived() {
            return received.get();
        }

        public long getDropped() {
            return dropped.get();
        }

        public long getFailed() {
            return failed.get();
        }

        public Map<AgentMessage.MessageType, Long> getSentByType() {
            Map<AgentMessage.MessageType, Long> result = new HashMap<>();
            sentByType.forEach((k, v) -> result.put(k, v.get()));
            return result;
        }

        public void logSummary() {
            LOGGER.info("Message Stats - Sent: {}, Delivered: {}, Received: {}, Dropped: {}, Failed: {}",
                sent.get(), delivered.get(), received.get(), dropped.get(), failed.get());
        }
    }
}
