package com.minewright.orchestration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Message bus for inter-agent communication.
 *
 * <p>Provides publish-subscribe messaging between agents with support for:</p>
 * <ul>
 *   <li>Direct messages (sender → specific recipient)</li>
 *   <li>Broadcast messages (sender → all agents)</li>
 *   <li>Priority-based message ordering</li>
 *   <li>Message filtering by type and sender</li>
 *   <li>Asynchronous message delivery</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> All operations are thread-safe using concurrent collections.</p>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * AgentCommunicationBus bus = new AgentCommunicationBus();
 *
 * // Subscribe to messages
 * bus.subscribe("MineWright1", message -> {
 *     System.out.println("Received: " + message.getContent());
 * });
 *
 * // Send direct message
 * bus.publish(AgentMessage.taskAssignment(
 *     "Foreman", "Foreman", "MineWright1", "Mine stone", Map.of("quantity", 10)
 * ));
 *
 * // Broadcast to all
 * bus.publish(AgentMessage.broadcast("Foreman", "Foreman", "Team meeting!", Priority.NORMAL));
 * </pre>
 *
 * @since 1.2.0
 */
public class AgentCommunicationBus {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentCommunicationBus.class);

    /**
     * Maximum messages per agent queue before oldest are dropped.
     */
    private static final int MAX_QUEUE_SIZE = 100;

    /**
     * Message queues for each agent, ordered by priority (high to low).
     */
    private final Map<String, PriorityBlockingQueue<AgentMessage>> agentQueues;

    /**
     * Message handlers for each agent.
     */
    private final Map<String, List<Consumer<AgentMessage>>> subscribers;

    /**
     * Message history for debugging (last 1000 messages).
     */
    private final ConcurrentLinkedDeque<AgentMessage> messageHistory;

    /**
     * Message filters per agent (optional).
     */
    private final Map<String, List<MessageFilter>> filters;

    /**
     * Statistics tracking.
     */
    private final MessageStats stats;

    public AgentCommunicationBus() {
        this.agentQueues = new ConcurrentHashMap<>();
        this.subscribers = new ConcurrentHashMap<>();
        this.messageHistory = new ConcurrentLinkedDeque<>();
        this.filters = new ConcurrentHashMap<>();
        this.stats = new MessageStats();

        LOGGER.info("AgentCommunicationBus initialized");
    }

    /**
     * Registers an agent with the communication bus.
     *
     * @param agentId   Agent's unique identifier
     * @param agentName Agent's display name
     */
    public void registerAgent(String agentId, String agentName) {
        agentQueues.computeIfAbsent(agentId,
            k -> new PriorityBlockingQueue<>(11,
                Comparator.comparingInt((AgentMessage m) -> -m.getPriority().getLevel())
                    .thenComparing(AgentMessage::getTimestamp)));

        subscribers.computeIfAbsent(agentId, k -> new CopyOnWriteArrayList<>());
        filters.computeIfAbsent(agentId, k -> new CopyOnWriteArrayList<>());

        LOGGER.info("Agent registered: {} ({})", agentName, agentId);
    }

    /**
     * Unregisters an agent from the communication bus.
     *
     * @param agentId Agent's unique identifier
     */
    public void unregisterAgent(String agentId) {
        agentQueues.remove(agentId);
        subscribers.remove(agentId);
        filters.remove(agentId);

        LOGGER.info("Agent unregistered: {}", agentId);
    }

    /**
     * Subscribes a handler to receive messages for an agent.
     *
     * @param agentId Agent to subscribe for
     * @param handler Message handler
     */
    public void subscribe(String agentId, Consumer<AgentMessage> handler) {
        List<Consumer<AgentMessage>> handlers = subscribers.get(agentId);
        if (handlers != null) {
            handlers.add(handler);
            LOGGER.debug("Subscribed handler for agent: {}", agentId);
        } else {
            LOGGER.warn("Cannot subscribe - agent not registered: {}", agentId);
        }
    }

    /**
     * Unsubscribes a handler from an agent's messages.
     *
     * @param agentId Agent to unsubscribe from
     * @param handler Handler to remove
     */
    public void unsubscribe(String agentId, Consumer<AgentMessage> handler) {
        List<Consumer<AgentMessage>> handlers = subscribers.get(agentId);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }

    /**
     * Adds a message filter for an agent.
     *
     * @param agentId Agent to filter messages for
     * @param filter  Filter to apply
     */
    public void addFilter(String agentId, MessageFilter filter) {
        List<MessageFilter> agentFilters = filters.get(agentId);
        if (agentFilters != null) {
            agentFilters.add(filter);
        }
    }

    /**
     * Publishes a message to its intended recipient(s).
     *
     * <p>If the message is a broadcast (recipientId is null or "*"),
     * it will be delivered to all registered agents.</p>
     *
     * @param message Message to publish
     */
    public void publish(AgentMessage message) {
        // Record in history
        addToHistory(message);
        stats.recordSent(message);

        if (message.isBroadcast()) {
            // Deliver to all agents except sender
            for (String agentId : agentQueues.keySet()) {
                if (!agentId.equals(message.getSenderId())) {
                    deliverToAgent(agentId, message);
                }
            }
        } else {
            // Direct message
            deliverToAgent(message.getRecipientId(), message);
        }
    }

    /**
     * Delivers a message to a specific agent.
     */
    private void deliverToAgent(String agentId, AgentMessage message) {
        // Check filters
        List<MessageFilter> agentFilters = filters.get(agentId);
        if (agentFilters != null) {
            for (MessageFilter filter : agentFilters) {
                if (!filter.accept(message)) {
                    LOGGER.debug("Message filtered for {}: {}", agentId, message);
                    stats.recordFiltered();
                    return;
                }
            }
        }

        // Add to queue
        PriorityBlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
        if (queue != null) {
            // Enforce max queue size
            while (queue.size() >= MAX_QUEUE_SIZE) {
                AgentMessage dropped = queue.poll();
                if (dropped != null) {
                    LOGGER.warn("Dropped message for {} (queue full): {}", agentId, dropped);
                    stats.recordDropped();
                }
            }
            queue.offer(message);
            stats.recordDelivered();

            // Notify handlers
            notifyHandlers(agentId);
        } else {
            LOGGER.warn("Cannot deliver - agent not registered: {}", agentId);
        }
    }

    /**
     * Notifies handlers that a new message is available.
     */
    private void notifyHandlers(String agentId) {
        List<Consumer<AgentMessage>> handlers = subscribers.get(agentId);
        PriorityBlockingQueue<AgentMessage> queue = agentQueues.get(agentId);

        if (handlers != null && queue != null) {
            AgentMessage message = queue.poll();
            if (message != null) {
                for (Consumer<AgentMessage> handler : handlers) {
                    try {
                        handler.accept(message);
                    } catch (Exception e) {
                        LOGGER.error("Error in message handler for {}: {}", agentId, e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Polls for the next message for an agent (non-blocking).
     *
     * @param agentId Agent to poll for
     * @return Next message, or null if queue is empty
     */
    public AgentMessage poll(String agentId) {
        PriorityBlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
        if (queue != null) {
            AgentMessage message = queue.poll();
            if (message != null) {
                stats.recordReceived();
            }
            return message;
        }
        return null;
    }

    /**
     * Polls for the next message for an agent (blocking with timeout).
     *
     * @param agentId Agent to poll for
     * @param timeout Maximum time to wait
     * @param unit    Time unit
     * @return Next message, or null if timeout elapsed
     */
    public AgentMessage poll(String agentId, long timeout, TimeUnit unit) throws InterruptedException {
        PriorityBlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
        if (queue != null) {
            // PriorityBlockingQueue doesn't support timed poll directly
            // So we use a simple approach with sleep
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            while (System.nanoTime() < deadline) {
                AgentMessage message = queue.poll();
                if (message != null) {
                    stats.recordReceived();
                    return message;
                }
                Thread.sleep(10);
            }
        }
        return null;
    }

    /**
     * Peeks at the next message without removing it.
     *
     * @param agentId Agent to peek for
     * @return Next message, or null if queue is empty
     */
    public AgentMessage peek(String agentId) {
        PriorityBlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
        return queue != null ? queue.peek() : null;
    }

    /**
     * Returns the number of pending messages for an agent.
     *
     * @param agentId Agent to check
     * @return Number of pending messages
     */
    public int getPendingCount(String agentId) {
        PriorityBlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
        return queue != null ? queue.size() : 0;
    }

    /**
     * Clears all pending messages for an agent.
     *
     * @param agentId Agent to clear
     */
    public void clearQueue(String agentId) {
        PriorityBlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
        if (queue != null) {
            int cleared = queue.size();
            queue.clear();
            LOGGER.info("Cleared {} messages for agent: {}", cleared, agentId);
        }
    }

    /**
     * Returns recent message history.
     *
     * @param count Maximum number of messages to return
     * @return List of recent messages (newest first)
     */
    public List<AgentMessage> getRecentHistory(int count) {
        List<AgentMessage> recent = new ArrayList<>();
        Iterator<AgentMessage> iterator = messageHistory.descendingIterator();
        while (iterator.hasNext() && recent.size() < count) {
            recent.add(iterator.next());
        }
        return recent;
    }

    /**
     * Returns message statistics.
     */
    public MessageStats getStats() {
        return stats;
    }

    /**
     * Returns list of registered agent IDs.
     */
    public Set<String> getRegisteredAgents() {
        return Collections.unmodifiableSet(agentQueues.keySet());
    }

    private void addToHistory(AgentMessage message) {
        messageHistory.addLast(message);
        // Trim history if too large
        while (messageHistory.size() > 1000) {
            messageHistory.removeFirst();
        }
    }

    /**
     * Shuts down the communication bus.
     */
    public void shutdown() {
        agentQueues.clear();
        subscribers.clear();
        filters.clear();
        LOGGER.info("AgentCommunicationBus shut down");
    }

    /**
     * Functional interface for message filtering.
     */
    @FunctionalInterface
    public interface MessageFilter {
        /**
         * Returns true if the message should be accepted.
         */
        boolean accept(AgentMessage message);
    }

    /**
     * Message statistics.
     */
    public static class MessageStats {
        private final ConcurrentHashMap<String, AtomicLong> sentByType = new ConcurrentHashMap<>();
        private final AtomicLong totalDelivered = new AtomicLong();
        private final AtomicLong totalReceived = new AtomicLong();
        private final AtomicLong totalFiltered = new AtomicLong();
        private final AtomicLong totalDropped = new AtomicLong();

        public void recordSent(AgentMessage message) {
            sentByType.computeIfAbsent(message.getType().name(), k -> new AtomicLong())
                .incrementAndGet();
        }

        public void recordDelivered() {
            totalDelivered.incrementAndGet();
        }

        public void recordReceived() {
            totalReceived.incrementAndGet();
        }

        public void recordFiltered() {
            totalFiltered.incrementAndGet();
        }

        public void recordDropped() {
            totalDropped.incrementAndGet();
        }

        public long getTotalDelivered() {
            return totalDelivered.get();
        }

        public long getTotalReceived() {
            return totalReceived.get();
        }

        public long getTotalFiltered() {
            return totalFiltered.get();
        }

        public long getTotalDropped() {
            return totalDropped.get();
        }

        public Map<String, Long> getSentByType() {
            Map<String, Long> result = new HashMap<>();
            sentByType.forEach((k, v) -> result.put(k, v.get()));
            return result;
        }

        public void logSummary() {
            LOGGER.info("Message Stats - Delivered: {}, Received: {}, Filtered: {}, Dropped: {}",
                totalDelivered.get(), totalReceived.get(), totalFiltered.get(), totalDropped.get());
        }
    }
}
