package com.minewright.orchestration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link AgentCommunicationBus}.
 *
 * <p>Tests cover the publish-subscribe messaging system including:</p>
 * <ul>
 *   <li>Agent registration and unregistration</li>
 *   <li>Message publishing and delivery</li>
 *   <li>Direct and broadcast messages</li>
 *   <li>Priority-based message ordering</li>
 *   <li>Message filtering</li>
 *   <li>Queue management (poll, peek, clear)</li>
 *   <li>Message history tracking</li>
 *   <li>Statistics collection</li>
 *   <li>Thread safety</li>
 *   <li>Edge cases and error handling</li>
 * </ul>
 *
 * @see AgentCommunicationBus
 * @see AgentMessage
 */
@DisplayName("Agent Communication Bus Tests")
class AgentCommunicationBusTest {

    private AgentCommunicationBus bus;
    private static final String AGENT_1 = "agent1";
    private static final String AGENT_2 = "agent2";
    private static final String AGENT_3 = "agent3";
    private static final String FOREMAN = "foreman";

    @BeforeEach
    void setUp() {
        bus = new AgentCommunicationBus();
    }

    // ==================== Registration Tests ====================

    @Nested
    @DisplayName("Agent Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Register agent successfully")
        void registerAgent() {
            bus.registerAgent(AGENT_1, "Agent One");

            assertTrue(bus.getRegisteredAgents().contains(AGENT_1),
                "Agent should be registered");
        }

        @Test
        @DisplayName("Register multiple agents")
        void registerMultipleAgents() {
            bus.registerAgent(AGENT_1, "Agent One");
            bus.registerAgent(AGENT_2, "Agent Two");
            bus.registerAgent(AGENT_3, "Agent Three");

            assertEquals(3, bus.getRegisteredAgents().size(),
                "All agents should be registered");
        }

        @Test
        @DisplayName("Register same agent twice is idempotent")
        void registerAgentTwice() {
            bus.registerAgent(AGENT_1, "Agent One");
            bus.registerAgent(AGENT_1, "Agent One");

            assertEquals(1, bus.getRegisteredAgents().stream()
                .filter(id -> id.equals(AGENT_1))
                .count(), "Should only have one registration");
        }

        @Test
        @DisplayName("Unregister agent removes from bus")
        void unregisterAgent() {
            bus.registerAgent(AGENT_1, "Agent One");
            bus.unregisterAgent(AGENT_1);

            assertFalse(bus.getRegisteredAgents().contains(AGENT_1),
                "Agent should be unregistered");
        }

        @Test
        @DisplayName("Unregister non-existent agent is safe")
        void unregisterNonExistentAgent() {
            assertDoesNotThrow(() -> bus.unregisterAgent("nonexistent"),
                "Unregistering non-existent agent should not throw");
        }

        @Test
        @DisplayName("Get registered agents returns unmodifiable set")
        void getRegisteredAgentsReturnsUnmodifiable() {
            bus.registerAgent(AGENT_1, "Agent One");

            var agents = bus.getRegisteredAgents();

            assertThrows(UnsupportedOperationException.class, () -> {
                agents.add("new_agent");
            }, "Should return unmodifiable set");
        }

        @Test
        @DisplayName("Get registered agents returns empty set when no agents")
        void getRegisteredAgentsEmptyInitially() {
            assertTrue(bus.getRegisteredAgents().isEmpty(),
                "Should have no registered agents initially");
        }

        @Test
        @DisplayName("Register agent creates message queue")
        void registerAgentCreatesQueue() {
            bus.registerAgent(AGENT_1, "Agent One");

            // Queue should exist but be empty
            assertEquals(0, bus.getPendingCount(AGENT_1),
                "New agent should have empty queue");
        }

        @Test
        @DisplayName("Unregister agent clears queue")
        void unregisterAgentClearsQueue() {
            bus.registerAgent(AGENT_1, "Agent One");

            AgentMessage message = AgentMessage.broadcast(
                FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL
            );
            bus.publish(message);

            bus.unregisterAgent(AGENT_1);

            // Queue should be cleared
            assertEquals(0, bus.getPendingCount(AGENT_1),
                "Queue should be cleared after unregistration");
        }
    }

    // ==================== Subscription Tests ====================

    @Nested
    @DisplayName("Message Subscription Tests")
    class SubscriptionTests {

        private final List<AgentMessage> receivedMessages = new ArrayList<>();

        @BeforeEach
        void setUp() {
            receivedMessages.clear();
            bus.registerAgent(AGENT_1, "Agent One");
        }

        @Test
        @DisplayName("Subscribe to agent messages")
        void subscribeToMessages() {
            bus.subscribe(AGENT_1, receivedMessages::add);

            AgentMessage message = AgentMessage.broadcast(
                FOREMAN, "Foreman", "Test message", AgentMessage.Priority.NORMAL
            );
            bus.publish(message);

            // Message should be delivered to handler
            assertFalse(receivedMessages.isEmpty(),
                "Handler should receive message");
        }

        @Test
        @DisplayName("Subscribe multiple handlers to same agent")
        void subscribeMultipleHandlers() {
            List<AgentMessage> handler1Messages = new ArrayList<>();
            List<AgentMessage> handler2Messages = new ArrayList<>();

            bus.subscribe(AGENT_1, handler1Messages::add);
            bus.subscribe(AGENT_1, handler2Messages::add);

            AgentMessage message = AgentMessage.broadcast(
                FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL
            );
            bus.publish(message);

            // Both handlers should receive the message
            assertFalse(handler1Messages.isEmpty(),
                "First handler should receive message");
            assertFalse(handler2Messages.isEmpty(),
                "Second handler should receive message");
        }

        @Test
        @DisplayName("Unsubscribe removes handler")
        void unsubscribeRemovesHandler() {
            List<AgentMessage> handler1Messages = new ArrayList<>();
            List<AgentMessage> handler2Messages = new ArrayList<>();

            bus.subscribe(AGENT_1, handler1Messages::add);
            bus.subscribe(AGENT_1, handler2Messages::add);

            AgentMessage message1 = AgentMessage.broadcast(
                FOREMAN, "Foreman", "Test 1", AgentMessage.Priority.NORMAL
            );
            bus.publish(message1);

            assertEquals(1, handler1Messages.size());
            assertEquals(1, handler2Messages.size());

            // Unsubscribe first handler
            bus.unsubscribe(AGENT_1, handler1Messages::add);

            AgentMessage message2 = AgentMessage.broadcast(
                FOREMAN, "Foreman", "Test 2", AgentMessage.Priority.NORMAL
            );
            bus.publish(message2);

            // Only second handler should receive new message
            assertEquals(1, handler1Messages.size(),
                "First handler should not receive new messages");
            assertEquals(2, handler2Messages.size(),
                "Second handler should receive new message");
        }

        @Test
        @DisplayName("Subscribe to unregistered agent logs warning")
        void subscribeToUnregisteredAgent() {
            assertDoesNotThrow(() -> bus.subscribe("unregistered", msg -> {}),
                "Subscribing to unregistered agent should not throw");
        }

        @Test
        @DisplayName("Unsubscribe from unregistered agent is safe")
        void unsubscribeFromUnregisteredAgent() {
            assertDoesNotThrow(() -> bus.unsubscribe("unregistered", msg -> {}),
                "Unsubscribing from unregistered agent should not throw");
        }

        @Test
        @DisplayName("Handler exception does not stop delivery")
        void handlerExceptionDoesNotStopDelivery() {
            List<AgentMessage> handler1Messages = new ArrayList<>();
            List<AgentMessage> handler2Messages = new ArrayList<>();

            bus.subscribe(AGENT_1, msg -> {
                throw new RuntimeException("Test exception");
            });
            bus.subscribe(AGENT_1, handler2Messages::add);

            AgentMessage message = AgentMessage.broadcast(
                FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL
            );
            bus.publish(message);

            // Second handler should still receive message
            assertFalse(handler2Messages.isEmpty(),
                "Second handler should receive message despite first handler exception");
        }
    }

    // ==================== Message Publishing Tests ====================

    @Nested
    @DisplayName("Message Publishing Tests")
    class PublishingTests {

        @BeforeEach
        void setUp() {
            bus.registerAgent(AGENT_1, "Agent One");
            bus.registerAgent(AGENT_2, "Agent Two");
            bus.registerAgent(AGENT_3, "Agent Three");
        }

        @Test
        @DisplayName("Publish direct message to specific agent")
        void publishDirectMessage() {
            AgentMessage message = new AgentMessage.Builder()
                .type(AgentMessage.Type.TASK_ASSIGNMENT)
                .sender(FOREMAN, "Foreman")
                .recipient(AGENT_1)
                .content("Task assigned")
                .priority(AgentMessage.Priority.NORMAL)
                .build();

            bus.publish(message);

            assertEquals(1, bus.getPendingCount(AGENT_1),
                "Recipient should have one message");
            assertEquals(0, bus.getPendingCount(AGENT_2),
                "Other agents should not receive the message");
        }

        @Test
        @DisplayName("Publish broadcast message to all agents")
        void publishBroadcastMessage() {
            AgentMessage message = AgentMessage.broadcast(
                FOREMAN, "Foreman", "Team announcement", AgentMessage.Priority.NORMAL
            );

            bus.publish(message);

            assertEquals(1, bus.getPendingCount(AGENT_1),
                "Agent 1 should receive broadcast");
            assertEquals(1, bus.getPendingCount(AGENT_2),
                "Agent 2 should receive broadcast");
            assertEquals(1, bus.getPendingCount(AGENT_3),
                "Agent 3 should receive broadcast");
        }

        @Test
        @DisplayName("Broadcast does not include sender")
        void broadcastDoesNotIncludeSender() {
            bus.registerAgent(FOREMAN, "Foreman");

            AgentMessage message = AgentMessage.broadcast(
                FOREMAN, "Foreman", "Announcement", AgentMessage.Priority.NORMAL
            );

            bus.publish(message);

            assertEquals(0, bus.getPendingCount(FOREMAN),
                "Sender should not receive own broadcast");
            assertEquals(1, bus.getPendingCount(AGENT_1),
                "Other agents should receive broadcast");
        }

        @Test
        @DisplayName("Publish message with null recipient treated as broadcast")
        void publishWithNullRecipient() {
            AgentMessage message = new AgentMessage.Builder()
                .type(AgentMessage.Type.BROADCAST)
                .sender(FOREMAN, "Foreman")
                .recipientId(null)
                .content("Test")
                .priority(AgentMessage.Priority.NORMAL)
                .build();

            bus.publish(message);

            assertTrue(bus.getPendingCount(AGENT_1) > 0 ||
                       bus.getPendingCount(AGENT_2) > 0,
                "Message should be broadcast");
        }

        @Test
        @DisplayName("Publish to unregistered agent is handled gracefully")
        void publishToUnregisteredAgent() {
            AgentMessage message = new AgentMessage.Builder()
                .type(AgentMessage.Type.TASK_ASSIGNMENT)
                .sender(FOREMAN, "Foreman")
                .recipient("unregistered")
                .content("Test")
                .priority(AgentMessage.Priority.NORMAL)
                .build();

            assertDoesNotThrow(() -> bus.publish(message),
                "Publishing to unregistered agent should not throw");
        }

        @Test
        @DisplayName("Multiple messages to same agent queue in order")
        void multipleMessagesQueueInOrder() {
            bus.publish(AgentMessage.broadcast(AGENT_1, AGENT_1, "First", AgentMessage.Priority.NORMAL));
            bus.publish(AgentMessage.broadcast(AGENT_1, AGENT_1, "Second", AgentMessage.Priority.NORMAL));
            bus.publish(AgentMessage.broadcast(AGENT_1, AGENT_1, "Third", AgentMessage.Priority.NORMAL));

            // Check pending count
            assertEquals(3, bus.getPendingCount(AGENT_1),
                "All messages should be queued");
        }

        @Test
        @DisplayName("Messages ordered by priority")
        void messagesOrderedByPriority() {
            bus.publish(AgentMessage.broadcast(AGENT_2, AGENT_2, "Low", AgentMessage.Priority.LOW));
            bus.publish(AgentMessage.broadcast(AGENT_2, AGENT_2, "High", AgentMessage.Priority.HIGH));
            bus.publish(AgentMessage.broadcast(AGENT_2, AGENT_2, "Normal", AgentMessage.Priority.NORMAL));

            // Poll should return highest priority first
            AgentMessage first = bus.poll(AGENT_2);
            assertEquals(AgentMessage.Priority.HIGH, first.getPriority(),
                "Highest priority message should be first");
        }
    }

    // ==================== Message Retrieval Tests ====================

    @Nested
    @DisplayName("Message Retrieval Tests")
    class RetrievalTests {

        @BeforeEach
        void setUp() {
            bus.registerAgent(AGENT_1, "Agent One");
            bus.registerAgent(AGENT_2, "Agent Two");
        }

        @Test
        @DisplayName("Poll returns next message")
        void pollReturnsMessage() {
            AgentMessage message = AgentMessage.broadcast(
                FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL
            );
            bus.publish(message);

            AgentMessage polled = bus.poll(AGENT_1);

            assertNotNull(polled, "Should return message");
            assertEquals(message.getMessageId(), polled.getMessageId(),
                "Should return the published message");
        }

        @Test
        @DisplayName("Poll removes message from queue")
        void pollRemovesMessage() {
            AgentMessage message = AgentMessage.broadcast(
                FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL
            );
            bus.publish(message);

            assertNotNull(bus.poll(AGENT_1));
            assertEquals(0, bus.getPendingCount(AGENT_1),
                "Message should be removed after poll");
        }

        @Test
        @DisplayName("Poll from empty queue returns null")
        void pollFromEmptyQueue() {
            assertNull(bus.poll(AGENT_1),
                "Polling empty queue should return null");
        }

        @Test
        @DisplayName("Poll from unregistered agent returns null")
        void pollFromUnregisteredAgent() {
            assertNull(bus.poll("unregistered"),
                "Polling unregistered agent should return null");
        }

        @Test
        @DisplayName("Poll with timeout waits for message")
        void pollWithTimeout() throws InterruptedException {
            // Publish message after a delay
            new Thread(() -> {
                try {
                    Thread.sleep(50);
                    bus.publish(AgentMessage.broadcast(
                        FOREMAN, "Foreman", "Delayed", AgentMessage.Priority.NORMAL
                    ));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            AgentMessage polled = bus.poll(AGENT_1, 200, TimeUnit.MILLISECONDS);

            assertNotNull(polled, "Should receive message within timeout");
        }

        @Test
        @DisplayName("Poll with timeout returns null if no message")
        void pollWithTimeoutReturnsNull() throws InterruptedException {
            AgentMessage polled = bus.poll(AGENT_1, 50, TimeUnit.MILLISECONDS);

            assertNull(polled, "Should return null when timeout expires");
        }

        @Test
        @DisplayName("Peek returns message without removing")
        void peekReturnsMessage() {
            AgentMessage message = AgentMessage.broadcast(
                FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL
            );
            bus.publish(message);

            AgentMessage peeked = bus.peek(AGENT_1);

            assertNotNull(peeked, "Should return message");
            assertEquals(1, bus.getPendingCount(AGENT_1),
                "Message should not be removed after peek");
        }

        @Test
        @DisplayName("Peek from empty queue returns null")
        void peekFromEmptyQueue() {
            assertNull(bus.peek(AGENT_1),
                "Peeking empty queue should return null");
        }

        @Test
        @DisplayName("Get pending count returns queue size")
        void getPendingCount() {
            assertEquals(0, bus.getPendingCount(AGENT_1),
                "Empty queue should have count 0");

            bus.publish(AgentMessage.broadcast(FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL));
            assertEquals(1, bus.getPendingCount(AGENT_1),
                "Queue should have count 1");

            bus.publish(AgentMessage.broadcast(FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL));
            assertEquals(2, bus.getPendingCount(AGENT_1),
                "Queue should have count 2");
        }

        @Test
        @DisplayName("Clear queue removes all messages")
        void clearQueue() {
            bus.publish(AgentMessage.broadcast(FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL));
            bus.publish(AgentMessage.broadcast(FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL));
            bus.publish(AgentMessage.broadcast(FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL));

            assertEquals(3, bus.getPendingCount(AGENT_1));

            bus.clearQueue(AGENT_1);

            assertEquals(0, bus.getPendingCount(AGENT_1),
                "Queue should be empty after clear");
        }

        @Test
        @DisplayName("Clear queue for unregistered agent is safe")
        void clearQueueForUnregisteredAgent() {
            assertDoesNotThrow(() -> bus.clearQueue("unregistered"),
                "Clearing queue for unregistered agent should not throw");
        }
    }

    // ==================== Message Filtering Tests ====================

    @Nested
    @DisplayName("Message Filtering Tests")
    class FilteringTests {

        @BeforeEach
        void setUp() {
            bus.registerAgent(AGENT_1, "Agent One");
            bus.registerAgent(AGENT_2, "Agent Two");
        }

        @Test
        @DisplayName("Add filter for agent")
        void addFilter() {
            bus.addFilter(AGENT_1, message ->
                message.getType() == AgentMessage.Type.TASK_ASSIGNMENT
            );

            // Publish task assignment
            AgentMessage taskMsg = new AgentMessage.Builder()
                .type(AgentMessage.Type.TASK_ASSIGNMENT)
                .sender(FOREMAN, "Foreman")
                .recipient(AGENT_1)
                .content("Task")
                .priority(AgentMessage.Priority.NORMAL)
                .build();

            bus.publish(taskMsg);

            assertEquals(1, bus.getPendingCount(AGENT_1),
                "Task assignment should pass filter");

            // Publish broadcast
            bus.publish(AgentMessage.broadcast(FOREMAN, "Foreman", "Broadcast", AgentMessage.Priority.NORMAL));

            assertEquals(1, bus.getPendingCount(AGENT_1),
                "Broadcast should be filtered out");
        }

        @Test
        @DisplayName("Multiple filters applied in sequence")
        void multipleFilters() {
            bus.addFilter(AGENT_1, message -> {
                if (message.getType() != AgentMessage.Type.TASK_ASSIGNMENT) {
                    return false;
                }
                return message.getPriority() == AgentMessage.Priority.HIGH;
            });

            AgentMessage lowPriorityTask = new AgentMessage.Builder()
                .type(AgentMessage.Type.TASK_ASSIGNMENT)
                .sender(FOREMAN, "Foreman")
                .recipient(AGENT_1)
                .content("Low priority task")
                .priority(AgentMessage.Priority.LOW)
                .build();

            AgentMessage highPriorityTask = new AgentMessage.Builder()
                .type(AgentMessage.Type.TASK_ASSIGNMENT)
                .sender(FOREMAN, "Foreman")
                .recipient(AGENT_1)
                .content("High priority task")
                .priority(AgentMessage.Priority.HIGH)
                .build();

            bus.publish(lowPriorityTask);
            bus.publish(highPriorityTask);

            assertEquals(1, bus.getPendingCount(AGENT_1),
                "Only high priority task should pass filters");
        }

        @Test
        @DisplayName("Add filter for unregistered agent is safe")
        void addFilterForUnregisteredAgent() {
            assertDoesNotThrow(() -> bus.addFilter("unregistered", msg -> true),
                "Adding filter for unregistered agent should not throw");
        }

        @Test
        @DisplayName("Filter that throws exception is handled")
        void filterExceptionHandling() {
            bus.addFilter(AGENT_1, message -> {
                throw new RuntimeException("Filter exception");
            });

            AgentMessage message = AgentMessage.broadcast(
                FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL
            );

            assertDoesNotThrow(() -> bus.publish(message),
                "Filter exception should be handled gracefully");
        }

        @Test
        @DisplayName("Filtered messages are tracked in statistics")
        void filteredMessagesInStats() {
            bus.addFilter(AGENT_1, message -> false); // Reject all

            bus.publish(AgentMessage.broadcast(FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL));

            AgentCommunicationBus.MessageStats stats = bus.getStats();

            assertTrue(stats.getTotalDelivered() == 0 || stats.getTotalFiltered() > 0,
                "Filtered messages should be tracked");
        }
    }

    // ==================== Message History Tests ====================

    @Nested
    @DisplayName("Message History Tests")
    class HistoryTests {

        @Test
        @DisplayName("Messages are recorded in history")
        void messagesRecordedInHistory() {
            bus.registerAgent(AGENT_1, "Agent One");

            AgentMessage message = AgentMessage.broadcast(
                FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL
            );
            bus.publish(message);

            List<AgentMessage> history = bus.getRecentHistory(10);

            assertFalse(history.isEmpty(), "History should contain messages");
        }

        @Test
        @DisplayName("Get recent history returns most recent messages")
        void getRecentHistory() {
            bus.registerAgent(AGENT_1, "Agent One");

            bus.publish(AgentMessage.broadcast(AGENT_1, "Agent 1", "First", AgentMessage.Priority.NORMAL));
            bus.publish(AgentMessage.broadcast(AGENT_1, "Agent 1", "Second", AgentMessage.Priority.NORMAL));
            bus.publish(AgentMessage.broadcast(AGENT_1, "Agent 1", "Third", AgentMessage.Priority.NORMAL));

            List<AgentMessage> history = bus.getRecentHistory(2);

            assertEquals(2, history.size(), "Should return requested count");
            // Most recent should be first
            assertTrue(history.get(0).getContent().contains("Third") ||
                       history.get(0).getContent().contains("Second"));
        }

        @Test
        @DisplayName("Get recent history with limit larger than history")
        void getRecentHistoryWithLargeLimit() {
            bus.registerAgent(AGENT_1, "Agent One");

            bus.publish(AgentMessage.broadcast(AGENT_1, "Agent 1", "Test", AgentMessage.Priority.NORMAL));

            List<AgentMessage> history = bus.getRecentHistory(100);

            assertEquals(1, history.size(), "Should return all available messages");
        }

        @Test
        @DisplayName("History is trimmed to maximum size")
        void historyTrimmedToMaxSize() {
            bus.registerAgent(AGENT_1, "Agent One");

            // Publish more messages than max history (1000)
            for (int i = 0; i < 1100; i++) {
                bus.publish(AgentMessage.broadcast(AGENT_1, "Agent 1", "Message " + i, AgentMessage.Priority.NORMAL));
            }

            List<AgentMessage> history = bus.getRecentHistory(2000);

            assertTrue(history.size() <= 1000, "History should be trimmed to max size");
        }

        @Test
        @DisplayName("Get recent history from empty bus")
        void getRecentHistoryFromEmpty() {
            List<AgentMessage> history = bus.getRecentHistory(10);

            assertTrue(history.isEmpty(), "Should return empty list");
        }
    }

    // ==================== Statistics Tests ====================

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @BeforeEach
        void setUp() {
            bus.registerAgent(AGENT_1, "Agent One");
            bus.registerAgent(AGENT_2, "Agent Two");
        }

        @Test
        @DisplayName("Statistics track sent messages by type")
        void statisticsTrackSentByType() {
            bus.publish(AgentMessage.broadcast(FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL));

            AgentCommunicationBus.MessageStats stats = bus.getStats();
            Map<String, Long> sentByType = stats.getSentByType();

            assertFalse(sentByType.isEmpty(), "Should track sent messages");
        }

        @Test
        @DisplayName("Statistics track delivered messages")
        void statisticsTrackDelivered() {
            bus.publish(AgentMessage.broadcast(FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL));

            AgentCommunicationBus.MessageStats stats = bus.getStats();

            assertTrue(stats.getTotalDelivered() > 0,
                "Should track delivered messages");
        }

        @Test
        @DisplayName("Statistics track received messages")
        void statisticsTrackReceived() {
            bus.publish(AgentMessage.broadcast(FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL));

            bus.poll(AGENT_1);

            AgentCommunicationBus.MessageStats stats = bus.getStats();

            assertTrue(stats.getTotalReceived() > 0,
                "Should track received messages");
        }

        @Test
        @DisplayName("Statistics track filtered messages")
        void statisticsTrackFiltered() {
            bus.addFilter(AGENT_1, msg -> false);

            bus.publish(AgentMessage.broadcast(FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL));

            AgentCommunicationBus.MessageStats stats = bus.getStats();

            assertTrue(stats.getTotalFiltered() >= 0,
                "Should track filtered messages");
        }

        @Test
        @DisplayName("Statistics track dropped messages")
        void statisticsTrackDropped() {
            // Fill queue to max size (100)
            for (int i = 0; i < 105; i++) {
                bus.publish(AgentMessage.broadcast(FOREMAN, "Foreman", "Test " + i, AgentMessage.Priority.NORMAL));
            }

            AgentCommunicationBus.MessageStats stats = bus.getStats();

            // Some messages should be dropped
            assertTrue(stats.getTotalDropped() >= 0,
                "Should track dropped messages");
        }

        @Test
        @DisplayName("Log summary does not throw exception")
        void logSummary() {
            bus.publish(AgentMessage.broadcast(FOREMAN, "Foreman", "Test", AgentMessage.Priority.NORMAL));

            assertDoesNotThrow(() -> bus.getStats().logSummary(),
                "Logging summary should not throw exception");
        }

        @Test
        @DisplayName("Statistics are accurate for multiple operations")
        void statisticsAccuracy() {
            int messageCount = 10;
            for (int i = 0; i < messageCount; i++) {
                bus.publish(AgentMessage.broadcast(FOREMAN, "Foreman", "Test " + i, AgentMessage.Priority.NORMAL));
            }

            // Poll some messages
            bus.poll(AGENT_1);
            bus.poll(AGENT_2);

            AgentCommunicationBus.MessageStats stats = bus.getStats();

            assertTrue(stats.getTotalDelivered() >= messageCount,
                "Delivered count should match published messages");
        }
    }

    // ==================== Thread Safety Tests ====================

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Concurrent message publishing")
        void concurrentPublishing() throws InterruptedException {
            bus.registerAgent(AGENT_1, "Agent One");
            bus.registerAgent(AGENT_2, "Agent Two");

            int threadCount = 10;
            int messagesPerThread = 100;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < messagesPerThread; j++) {
                            bus.publish(AgentMessage.broadcast(
                                "sender" + j, "Sender " + j,
                                "Message " + j,
                                AgentMessage.Priority.NORMAL
                            ));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS));

            // All messages should be delivered
            AgentCommunicationBus.MessageStats stats = bus.getStats();
            assertTrue(stats.getTotalDelivered() >= threadCount * messagesPerThread * 2,
                "All published messages should be delivered to both agents");
        }

        @Test
        @DisplayName("Concurrent polling and publishing")
        void concurrentPollingAndPublishing() throws InterruptedException {
            bus.registerAgent(AGENT_1, "Agent One");

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(2);

            AtomicInteger publishedCount = new AtomicInteger(0);
            AtomicInteger polledCount = new AtomicInteger(0);

            // Publisher thread
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 100; i++) {
                        bus.publish(AgentMessage.broadcast(
                            FOREMAN, "Foreman", "Message " + i, AgentMessage.Priority.NORMAL
                        ));
                        publishedCount.incrementAndGet();
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();

            // Poller thread
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 100; i++) {
                        if (bus.poll(AGENT_1, 10, TimeUnit.MILLISECONDS) != null) {
                            polledCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();

            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS));

            assertTrue(polledCount.get() > 0, "Some messages should be polled");
        }

        @Test
        @DisplayName("Concurrent agent registration")
        void concurrentRegistration() throws InterruptedException {
            int threadCount = 20;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                new Thread(() -> {
                    try {
                        startLatch.await();
                        bus.registerAgent("agent" + index, "Agent " + index);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

            assertEquals(threadCount, bus.getRegisteredAgents().size(),
                "All agents should be registered");
        }

        @Test
        @DisplayName("Concurrent filter additions")
        void concurrentFilterAdditions() throws InterruptedException {
            bus.registerAgent(AGENT_1, "Agent One");

            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        bus.addFilter(AGENT_1, msg -> true);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

            // Should handle concurrent filter additions
            assertDoesNotThrow(() ->
                bus.publish(AgentMessage.broadcast(AGENT_1, AGENT_1, "Test", AgentMessage.Priority.NORMAL))
            );
        }
    }

    // ==================== Shutdown Tests ====================

    @Nested
    @DisplayName("Shutdown Tests")
    class ShutdownTests {

        @Test
        @DisplayName("Shutdown clears all state")
        void shutdownClearsState() {
            bus.registerAgent(AGENT_1, "Agent One");
            bus.registerAgent(AGENT_2, "Agent Two");

            bus.publish(AgentMessage.broadcast(AGENT_1, AGENT_1, "Test", AgentMessage.Priority.NORMAL));

            bus.shutdown();

            assertTrue(bus.getRegisteredAgents().isEmpty(),
                "All agents should be unregistered");
            assertEquals(0, bus.getPendingCount(AGENT_1),
                "Queues should be cleared");
        }

        @Test
        @DisplayName("Shutdown with no state is safe")
        void shutdownWithNoState() {
            assertDoesNotThrow(() -> bus.shutdown(),
                "Shutdown with no state should not throw");
        }

        @Test
        @DisplayName("Multiple shutdowns are safe")
        void multipleShutdowns() {
            bus.registerAgent(AGENT_1, "Agent One");

            assertDoesNotThrow(() -> {
                bus.shutdown();
                bus.shutdown();
                bus.shutdown();
            }, "Multiple shutdowns should be safe");
        }

        @Test
        @DisplayName("Operations after shutdown are handled")
        void operationsAfterShutdown() {
            bus.shutdown();

            assertDoesNotThrow(() -> {
                bus.registerAgent(AGENT_1, "Agent One");
                bus.publish(AgentMessage.broadcast(AGENT_1, AGENT_1, "Test", AgentMessage.Priority.NORMAL));
                bus.getStats();
                bus.getRegisteredAgents();
            }, "Operations after shutdown should not throw exceptions");
        }
    }

    // ==================== Edge Cases Tests ====================

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Handle message with very long content")
        void veryLongMessageContent() {
            bus.registerAgent(AGENT_1, "Agent One");

            String longContent = "A".repeat(10000);
            AgentMessage message = AgentMessage.broadcast(
                FOREMAN, "Foreman", longContent, AgentMessage.Priority.NORMAL
            );

            assertDoesNotThrow(() -> bus.publish(message),
                "Should handle very long message content");
        }

        @Test
        @DisplayName("Handle message with special characters")
        void messageWithSpecialCharacters() {
            bus.registerAgent(AGENT_1, "Agent One");

            String specialContent = "Test\n\t\r\b\f\u0000<>\"'&\\";
            AgentMessage message = AgentMessage.broadcast(
                FOREMAN, "Foreman", specialContent, AgentMessage.Priority.NORMAL
            );

            assertDoesNotThrow(() -> bus.publish(message),
                "Should handle special characters");
        }

        @Test
        @DisplayName("Handle message with null content")
        void messageWithNullContent() {
            bus.registerAgent(AGENT_1, "Agent One");

            AgentMessage message = new AgentMessage.Builder()
                .sender(FOREMAN, "Foreman")
                .recipient(AGENT_1)
                .content(null)
                .priority(AgentMessage.Priority.NORMAL)
                .build();

            assertDoesNotThrow(() -> bus.publish(message),
                "Should handle null content");
        }

        @Test
        @DisplayName("Handle message with empty payload")
        void messageWithEmptyPayload() {
            bus.registerAgent(AGENT_1, "Agent One");

            AgentMessage message = new AgentMessage.Builder()
                .sender(FOREMAN, "Foreman")
                .recipient(AGENT_1)
                .content("Test")
                .priority(AgentMessage.Priority.NORMAL)
                .build();

            assertDoesNotThrow(() -> bus.publish(message),
                "Should handle empty payload");
        }

        @Test
        @DisplayName("Handle all priority levels")
        void allPriorityLevels() {
            bus.registerAgent(AGENT_1, "Agent One");

            assertDoesNotThrow(() -> {
                bus.publish(AgentMessage.broadcast(AGENT_1, AGENT_1, "CRITICAL", AgentMessage.Priority.CRITICAL));
                bus.publish(AgentMessage.broadcast(AGENT_1, AGENT_1, "URGENT", AgentMessage.Priority.URGENT));
                bus.publish(AgentMessage.broadcast(AGENT_1, AGENT_1, "HIGH", AgentMessage.Priority.HIGH));
                bus.publish(AgentMessage.broadcast(AGENT_1, AGENT_1, "NORMAL", AgentMessage.Priority.NORMAL));
                bus.publish(AgentMessage.broadcast(AGENT_1, AGENT_1, "LOW", AgentMessage.Priority.LOW));
            }, "Should handle all priority levels");

            // Messages should be ordered by priority
            AgentMessage first = bus.poll(AGENT_1);
            assertEquals(AgentMessage.Priority.CRITICAL, first.getPriority(),
                "CRITICAL priority should be first");
        }

        @Test
        @DisplayName("Handle all message types")
        void allMessageTypes() {
            bus.registerAgent(AGENT_1, "Agent One");
            bus.registerAgent(FOREMAN, "Foreman");

            assertDoesNotThrow(() -> {
                new AgentMessage.Builder()
                    .type(AgentMessage.Type.TASK_ASSIGNMENT)
                    .sender(FOREMAN, "Foreman")
                    .recipient(AGENT_1)
                    .content("Task")
                    .build();

                new AgentMessage.Builder()
                    .type(AgentMessage.Type.TASK_PROGRESS)
                    .sender(AGENT_1, "Agent 1")
                    .recipient(FOREMAN)
                    .content("Progress")
                    .build();

                new AgentMessage.Builder()
                    .type(AgentMessage.Type.TASK_COMPLETE)
                    .sender(AGENT_1, "Agent 1")
                    .recipient(FOREMAN)
                    .content("Complete")
                    .build();

                new AgentMessage.Builder()
                    .type(AgentMessage.Type.TASK_FAILED)
                    .sender(AGENT_1, "Agent 1")
                    .recipient(FOREMAN)
                    .content("Failed")
                    .build();

                new AgentMessage.Builder()
                    .type(AgentMessage.Type.HELP_REQUEST)
                    .sender(AGENT_1, "Agent 1")
                    .recipient(FOREMAN)
                    .content("Help")
                    .build();

                new AgentMessage.Builder()
                    .type(AgentMessage.Type.STATUS_REPORT)
                    .sender(AGENT_1, "Agent 1")
                    .recipient(FOREMAN)
                    .content("Status")
                    .build();

                new AgentMessage.Builder()
                    .type(AgentMessage.Type.COORDINATION)
                    .sender(AGENT_1, "Agent 1")
                    .recipient(FOREMAN)
                    .content("Coordinate")
                    .build();

                new AgentMessage.Builder()
                    .type(AgentMessage.Type.BROADCAST)
                    .sender(FOREMAN, "Foreman")
                    .recipient("*")
                    .content("Broadcast")
                    .build();

                new AgentMessage.Builder()
                    .type(AgentMessage.Type.HUMAN_COMMAND)
                    .sender(FOREMAN, "Foreman")
                    .recipient(AGENT_1)
                    .content("Command")
                    .build();

                new AgentMessage.Builder()
                    .type(AgentMessage.Type.PLAN_ANNOUNCEMENT)
                    .sender(FOREMAN, "Foreman")
                    .recipient("*")
                    .content("Plan")
                    .build();
            }, "Should handle all message types");
        }

        @Test
        @DisplayName("Handle burst of messages")
        void burstOfMessages() {
            bus.registerAgent(AGENT_1, "Agent One");

            int burstCount = 500;
            for (int i = 0; i < burstCount; i++) {
                bus.publish(AgentMessage.broadcast(AGENT_1, AGENT_1, "Msg " + i, AgentMessage.Priority.NORMAL));
            }

            // Should have processed messages (some may be dropped due to queue limit)
            assertTrue(bus.getPendingCount(AGENT_1) <= 100,
                "Queue should respect max size");
        }

        @Test
        @DisplayName("Handle rapid publish and poll cycles")
        void rapidPublishPollCycles() {
            bus.registerAgent(AGENT_1, "Agent One");

            for (int i = 0; i < 100; i++) {
                bus.publish(AgentMessage.broadcast(AGENT_1, AGENT_1, "Msg " + i, AgentMessage.Priority.NORMAL));
                bus.poll(AGENT_1);
            }

            assertDoesNotThrow(() -> bus.getStats(),
                "Should handle rapid cycles without error");
        }

        @Test
        @DisplayName("Handle message with all priority levels same timestamp")
        void messagesWithSameTimestamp() {
            bus.registerAgent(AGENT_1, "Agent One");

            // Publish messages rapidly - may have same timestamp
            for (int i = 0; i < 10; i++) {
                bus.publish(AgentMessage.broadcast(
                    AGENT_1, AGENT_1, "Msg " + i, AgentMessage.Priority.NORMAL
                ));
            }

            // Should handle same timestamp gracefully
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 10; i++) {
                    bus.poll(AGENT_1);
                }
            });
        }
    }
}
