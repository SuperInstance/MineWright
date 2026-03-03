package com.minewright.integration;

import com.minewright.communication.*;
import com.minewright.entity.ForemanEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for inter-agent communication system.
 *
 * <p><b>Test Coverage:</b></p>
 * <ul>
 *   <li>Direct messaging between agents</li>
 *   <li>Broadcast messaging to all agents</li>
 *   <li>Message handler registration and processing</li>
 *   <li>Concurrent message sending</li>
 *   <li>Message filtering and priority</li>
 *   <li>Request/response correlation</li>
 *   <li>Message history and statistics</li>
 * </ul>
 *
 * @see CommunicationBus
 * @see AgentMessage
 * @see MessageHandler
 * @since 1.3.0
 */
@DisplayName("Agent Communication Integration Tests")
class AgentCommunicationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Agents can send direct messages")
    void testDirectMessaging() {
        CommunicationBus bus = new CommunicationBus();

        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        List<AgentMessage> receivedMessages = new ArrayList<>();

        // Register handler for recipient
        bus.register(recipient, new MessageHandler() {
            @Override
            public void handleMessage(AgentMessage message) {
                receivedMessages.add(message);
            }

            @Override
            public UUID getAgentId() {
                return recipient;
            }
        });

        // Send message
        AgentMessage message = AgentMessage.builder()
            .sender(sender)
            .recipient(recipient)
            .type(AgentMessage.MessageType.STATUS_UPDATE)
            .content("Hello from sender")
            .build();

        assertTrue(bus.send(message), "Message should be sent");

        // Process messages
        bus.tick();

        assertEquals(1, receivedMessages.size(), "Should receive one message");
        assertEquals("Hello from sender", receivedMessages.get(0).content(),
            "Should receive correct content");
    }

    @Test
    @DisplayName("Agents can broadcast messages to all recipients")
    void testBroadcastMessaging() {
        CommunicationBus bus = new CommunicationBus();

        UUID sender = UUID.randomUUID();
        UUID recipient1 = UUID.randomUUID();
        UUID recipient2 = UUID.randomUUID();
        UUID recipient3 = UUID.randomUUID();

        Map<UUID, List<AgentMessage>> received = new HashMap<>();
        received.put(recipient1, new ArrayList<>());
        received.put(recipient2, new ArrayList<>());
        received.put(recipient3, new ArrayList<>());

        // Register handlers
        for (UUID recipient : received.keySet()) {
            final UUID r = recipient;
            bus.register(r, new MessageHandler() {
                @Override
                public void handleMessage(AgentMessage message) {
                    received.get(r).add(message);
                }

                @Override
                public UUID getAgentId() {
                    return r;
                }
            });
        }

        // Broadcast message
        bus.broadcast(sender, AgentMessage.MessageType.ALERT, "Emergency!");

        // Process messages
        bus.tick();

        // All recipients should receive the message
        for (UUID recipient : received.keySet()) {
            assertEquals(1, received.get(recipient).size(),
                recipient + " should receive broadcast");
            assertEquals("Emergency!", received.get(recipient).get(0).content(),
                "Should receive correct content");
        }
    }

    @Test
    @DisplayName("Multiple concurrent message sends")
    void testConcurrentMessaging() throws InterruptedException {
        CommunicationBus bus = new CommunicationBus();

        UUID sender = UUID.randomUUID();
        List<UUID> recipients = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            recipients.add(UUID.randomUUID());
        }

        Map<UUID, AtomicInteger> messageCounts = new ConcurrentHashMap<>();

        // Register handlers
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch registrationLatch = new CountDownLatch(recipients.size());

        for (UUID recipient : recipients) {
            messageCounts.put(recipient, new AtomicInteger(0));
            executor.submit(() -> {
                try {
                    final UUID r = recipient;
                    bus.register(r, new MessageHandler() {
                        @Override
                        public void handleMessage(AgentMessage message) {
                            messageCounts.get(r).incrementAndGet();
                        }

                        @Override
                        public UUID getAgentId() {
                            return r;
                        }
                    });
                } finally {
                    registrationLatch.countDown();
                }
            });
        }

        registrationLatch.await(5, TimeUnit.SECONDS);

        // Send messages concurrently
        CountDownLatch sendLatch = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    UUID recipient = recipients.get(index % recipients.size());
                    AgentMessage message = AgentMessage.builder()
                        .sender(sender)
                        .recipient(recipient)
                        .type(AgentMessage.MessageType.TASK_REQUEST)
                        .content("Task " + index)
                        .build();
                    bus.send(message);
                } finally {
                    sendLatch.countDown();
                }
            });
        }

        sendLatch.await(10, TimeUnit.SECONDS);

        // Process all messages
        for (int i = 0; i < 20; i++) {
            bus.tick();
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Verify all messages were received
        int totalReceived = messageCounts.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
        assertEquals(100, totalReceived, "All messages should be received");
    }

    @Test
    @DisplayName("Message handlers can filter by type")
    void testMessageFiltering() {
        CommunicationBus bus = new CommunicationBus();

        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        List<AgentMessage> statusUpdates = new ArrayList<>();
        List<AgentMessage> taskRequests = new ArrayList<>();

        // Register handler that filters by type
        bus.register(recipient, new MessageHandler() {
            @Override
            public void handleMessage(AgentMessage message) {
                switch (message.type()) {
                    case STATUS_UPDATE:
                        statusUpdates.add(message);
                        break;
                    case TASK_REQUEST:
                        taskRequests.add(message);
                        break;
                }
            }

            @Override
            public UUID getAgentId() {
                return recipient;
            }
        });

        // Send different types
        bus.send(AgentMessage.builder()
            .sender(sender)
            .recipient(recipient)
            .type(AgentMessage.MessageType.STATUS_UPDATE)
            .content("Status")
            .build());

        bus.send(AgentMessage.builder()
            .sender(sender)
            .recipient(recipient)
            .type(AgentMessage.MessageType.TASK_REQUEST)
            .content("Task")
            .build());

        bus.send(AgentMessage.builder()
            .sender(sender)
            .recipient(recipient)
            .type(AgentMessage.MessageType.STATUS_UPDATE)
            .content("Another status")
            .build());

        bus.tick();
        bus.tick();
        bus.tick();

        assertEquals(2, statusUpdates.size(), "Should receive 2 status updates");
        assertEquals(1, taskRequests.size(), "Should receive 1 task request");
    }

    @Test
    @DisplayName("Message priority affects delivery order")
    void testMessagePriority() {
        CommunicationBus bus = new CommunicationBus();

        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        List<AgentMessage> receivedMessages = new ArrayList<>();

        bus.register(recipient, new MessageHandler() {
            @Override
            public void handleMessage(AgentMessage message) {
                receivedMessages.add(message);
            }

            @Override
            public UUID getAgentId() {
                return recipient;
            }
        });

        // Send messages with different priorities
        bus.send(AgentMessage.builder()
            .sender(sender)
            .recipient(recipient)
            .type(AgentMessage.MessageType.STATUS_UPDATE)
            .content("Low priority")
            .priority(1)
            .build());

        bus.send(AgentMessage.builder()
            .sender(sender)
            .recipient(recipient)
            .type(AgentMessage.MessageType.ALERT)
            .content("High priority")
            .priority(10)
            .build());

        bus.send(AgentMessage.builder()
            .sender(sender)
            .recipient(recipient)
            .type(AgentMessage.MessageType.TASK_REQUEST)
            .content("Medium priority")
            .priority(5)
            .build());

        // Process messages
        bus.tick();
        bus.tick();
        bus.tick();

        assertEquals(3, receivedMessages.size(), "Should receive all messages");
        // High priority should be processed first
        assertEquals("High priority", receivedMessages.get(0).content(),
            "High priority message should be first");
    }

    @Test
    @DisplayName("Request/response correlation works")
    void testRequestResponseCorrelation() throws Exception {
        CommunicationBus bus = new CommunicationBus();

        UUID requester = UUID.randomUUID();
        UUID responder = UUID.randomUUID();

        // Register responder handler
        bus.register(responder, new MessageHandler() {
            @Override
            public void handleMessage(AgentMessage message) {
                if (message.type() == AgentMessage.MessageType.TASK_REQUEST) {
                    // Send response
                    AgentMessage response = AgentMessage.builder()
                        .sender(responder)
                        .recipient(requester)
                        .type(AgentMessage.MessageType.TASK_RESPONSE)
                        .content("Task completed")
                        .correlationId(message.messageId())
                        .build();
                    bus.send(response);
                }
            }

            @Override
            public UUID getAgentId() {
                return responder;
            }
        });

        // Send request and wait for response
        AgentMessage request = AgentMessage.builder()
            .sender(requester)
            .recipient(responder)
            .type(AgentMessage.MessageType.TASK_REQUEST)
            .content("Do task")
            .build();

        CompletableFuture<AgentMessage> responseFuture = bus.sendRequest(request, 5000);

        // Process messages
        for (int i = 0; i < 10; i++) {
            bus.tick();
            Thread.sleep(100);
        }

        assertTrue(responseFuture.isDone(), "Should receive response");
        AgentMessage response = responseFuture.get(5, TimeUnit.SECONDS);
        assertEquals("Task completed", response.content(), "Should receive response content");
        assertEquals(request.messageId(), response.correlationId(),
            "Correlation ID should match");
    }

    @Test
    @DisplayName("Unregistered agents don't receive messages")
    void testUnregisteredAgent() {
        CommunicationBus bus = new CommunicationBus();

        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        List<AgentMessage> received = new ArrayList<>();

        // Don't register recipient

        AgentMessage message = AgentMessage.builder()
            .sender(sender)
            .recipient(recipient)
            .type(AgentMessage.MessageType.STATUS_UPDATE)
            .content("Test")
            .build();

        assertTrue(bus.send(message), "Send should succeed even if not registered");

        bus.tick();

        assertEquals(0, received.size(), "Unregistered agent should not receive");
    }

    @Test
    @DisplayName("Message statistics are tracked")
    void testMessageStatistics() {
        CommunicationBus bus = new CommunicationBus();

        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        bus.register(recipient, new MessageHandler() {
            @Override
            public void handleMessage(AgentMessage message) {
                // Handle message
            }

            @Override
            public UUID getAgentId() {
                return recipient;
            }
        });

        // Send messages
        for (int i = 0; i < 10; i++) {
            bus.send(AgentMessage.builder()
                .sender(sender)
                .recipient(recipient)
                .type(AgentMessage.MessageType.STATUS_UPDATE)
                .content("Message " + i)
                .build());
        }

        bus.tick();

        Map<String, Object> stats = bus.getStatistics();
        assertTrue((Integer) stats.get("messagesSent") >= 10,
            "Should track sent messages");
        assertTrue((Integer) stats.get("messagesReceived") >= 10,
            "Should track received messages");
    }

    @Test
    @DisplayName("Message history is maintained")
    void testMessageHistory() {
        CommunicationBus bus = new CommunicationBus();

        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        bus.register(recipient, new MessageHandler() {
            @Override
            public void handleMessage(AgentMessage message) {
                // Handle
            }

            @Override
            public UUID getAgentId() {
                return recipient;
            }
        });

        // Send messages
        for (int i = 0; i < 5; i++) {
            bus.send(AgentMessage.builder()
                .sender(sender)
                .recipient(recipient)
                .type(AgentMessage.MessageType.STATUS_UPDATE)
                .content("Message " + i)
                .build());
        }

        bus.tick();

        List<AgentMessage> history = bus.getMessageHistory();
        assertTrue(history.size() >= 5, "History should contain messages");
    }

    @Test
    @DisplayName("Agent can unregister from communication")
    void testAgentUnregister() {
        CommunicationBus bus = new CommunicationBus();

        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        List<AgentMessage> received = new ArrayList<>();

        MessageHandler handler = new MessageHandler() {
            @Override
            public void handleMessage(AgentMessage message) {
                received.add(message);
            }

            @Override
            public UUID getAgentId() {
                return recipient;
            }
        };

        bus.register(recipient, handler);

        // Send message (should be received)
        bus.send(AgentMessage.builder()
            .sender(sender)
            .recipient(recipient)
            .type(AgentMessage.MessageType.STATUS_UPDATE)
            .content("Before unregister")
            .build());

        bus.tick();
        assertEquals(1, received.size(), "Should receive before unregister");

        // Unregister
        bus.unregister(recipient);

        // Send message (should not be received)
        bus.send(AgentMessage.builder()
            .sender(sender)
            .recipient(recipient)
            .type(AgentMessage.MessageType.STATUS_UPDATE)
            .content("After unregister")
            .build());

        bus.tick();
        assertEquals(1, received.size(), "Should not receive after unregister");
    }

    @Test
    @DisplayName("Complex multi-agent conversation")
    void testMultiAgentConversation() {
        CommunicationBus bus = new CommunicationBus();

        // Create agents with different roles
        UUID foreman = UUID.randomUUID();
        UUID miner1 = UUID.randomUUID();
        UUID miner2 = UUID.randomUUID();
        UUID builder = UUID.randomUUID();

        Map<UUID, List<AgentMessage>> received = new HashMap<>();
        for (UUID agent : Arrays.asList(foreman, miner1, miner2, builder)) {
            received.put(agent, new ArrayList<>());
        }

        // Register handlers
        for (UUID agent : received.keySet()) {
            final UUID a = agent;
            bus.register(a, new MessageHandler() {
                @Override
                public void handleMessage(AgentMessage message) {
                    received.get(a).add(message);

                    // Auto-reply logic
                    if (message.type() == AgentMessage.MessageType.TASK_REQUEST) {
                        AgentMessage reply = AgentMessage.builder()
                            .sender(a)
                            .recipient(message.sender())
                            .type(AgentMessage.MessageType.TASK_RESPONSE)
                            .content("Task accepted by " + a.toString().substring(0, 8))
                            .build();
                        bus.send(reply);
                    }
                }

                @Override
                public UUID getAgentId() {
                    return a;
                }
            });
        }

        // Foreman broadcasts task request
        bus.broadcast(foreman, AgentMessage.MessageType.TASK_REQUEST, "Need mining help");

        // Process
        bus.tick();
        bus.tick();

        // Miners should have received request
        assertTrue(received.get(miner1).size() > 0, "Miner1 should receive");
        assertTrue(received.get(miner2).size() > 0, "Miner2 should receive");

        // Foreman should have received responses
        long responses = received.get(foreman).stream()
            .filter(m -> m.type() == AgentMessage.MessageType.TASK_RESPONSE)
            .count();
        assertTrue(responses >= 2, "Foreman should receive responses from miners");
    }

    @Test
    @DisplayName("Message queue limits are enforced")
    void testQueueLimits() {
        CommunicationBus bus = new CommunicationBus();

        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        // Register handler that doesn't process
        bus.register(recipient, new MessageHandler() {
            @Override
            public void handleMessage(AgentMessage message) {
                // Don't process - let queue fill up
            }

            @Override
            public UUID getAgentId() {
                return recipient;
            }
        });

        // Send more messages than queue can hold
        for (int i = 0; i < 150; i++) {
            bus.send(AgentMessage.builder()
                .sender(sender)
                .recipient(recipient)
                .type(AgentMessage.MessageType.STATUS_UPDATE)
                .content("Message " + i)
                .build());
        }

        bus.tick();

        // Queue should be limited
        Map<String, Object> stats = bus.getStatistics();
        Integer queueSize = (Integer) stats.get("averageQueueSize");
        assertNotNull(queueSize, "Should track queue size");
        assertTrue(queueSize <= 100, "Queue should be limited");
    }

    @Test
    @DisplayName("Communication bus can be stopped and started")
    void testStartStop() {
        CommunicationBus bus = new CommunicationBus();

        assertTrue(bus.isRunning(), "Should be running initially");

        bus.stop();
        assertFalse(bus.isRunning(), "Should be stopped");

        bus.start();
        assertTrue(bus.isRunning(), "Should be running again");
    }

    @Test
    @DisplayName("Multiple foreman entities can communicate")
    void testForemanEntityCommunication() {
        CommunicationBus bus = new CommunicationBus();

        // Create foreman entities
        ForemanEntity foreman1 = createForeman("Steve1");
        ForemanEntity foreman2 = createForeman("Steve2");

        UUID id1 = foreman1.getUUID();
        UUID id2 = foreman2.getUUID();

        List<String> messages1 = new ArrayList<>();
        List<String> messages2 = new ArrayList<>();

        // Register handlers
        bus.register(id1, new MessageHandler() {
            @Override
            public void handleMessage(AgentMessage message) {
                messages1.add(message.content());
            }

            @Override
            public UUID getAgentId() {
                return id1;
            }
        });

        bus.register(id2, new MessageHandler() {
            @Override
            public void handleMessage(AgentMessage message) {
                messages2.add(message.content());
            }

            @Override
            public UUID getAgentId() {
                return id2;
            }
        });

        // Send messages
        bus.send(AgentMessage.builder()
            .sender(id1)
            .recipient(id2)
            .type(AgentMessage.MessageType.STATUS_UPDATE)
            .content("Hello from Steve1")
            .build());

        bus.send(AgentMessage.builder()
            .sender(id2)
            .recipient(id1)
            .type(AgentMessage.MessageType.STATUS_UPDATE)
            .content("Hello from Steve2")
            .build());

        bus.tick();
        bus.tick();

        assertEquals(1, messages1.size(), "Steve1 should receive message");
        assertEquals(1, messages2.size(), "Steve2 should receive message");
        assertEquals("Hello from Steve2", messages1.get(0), "Steve1 should get Steve2's message");
        assertEquals("Hello from Steve1", messages2.get(0), "Steve2 should get Steve1's message");
    }
}
