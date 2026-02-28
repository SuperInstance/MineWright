package com.minewright.communication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link CommunicationBus}.
 *
 * <p>Tests cover the core communication infrastructure including:</p>
 * <ul>
 *   <li>Message routing between agents</li>
 *   <li>Broadcast messaging</li>
 *   <li>Conversation tracking</li>
 *   <li>Protocol handshaking</li>
 *   <li>Request/response correlation</li>
 *   <li>Message statistics</li>
 *   <li>Thread safety</li>
 *   <li>Queue management</li>
 * </ul>
 *
 * @see CommunicationBus
 * @see AgentMessage
 * @see Conversation
 * @see CommunicationProtocol
 */
@DisplayName("Communication Bus Tests")
class CommunicationBusTest {

    private CommunicationBus bus;
    private UUID agent1;
    private UUID agent2;
    private UUID agent3;

    @BeforeEach
    void setUp() {
        bus = new CommunicationBus();
        agent1 = UUID.randomUUID();
        agent2 = UUID.randomUUID();
        agent3 = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Agent Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Register agent with handler")
        void registerAgent() {
            MessageHandler handler = message -> {};
            bus.register(agent1, handler);

            assertTrue(bus.isRegistered(agent1), "Agent should be registered");
            assertEquals(1, bus.getRegisteredAgents().size(), "Should have one agent");
        }

        @Test
        @DisplayName("Register multiple agents")
        void registerMultipleAgents() {
            MessageHandler handler = message -> {};
            bus.register(agent1, handler);
            bus.register(agent2, handler);
            bus.register(agent3, handler);

            assertEquals(3, bus.getRegisteredAgents().size(), "Should have three agents");
            assertTrue(bus.isRegistered(agent1));
            assertTrue(bus.isRegistered(agent2));
            assertTrue(bus.isRegistered(agent3));
        }

        @Test
        @DisplayName("Unregister agent")
        void unregisterAgent() {
            MessageHandler handler = message -> {};
            bus.register(agent1, handler);
            assertTrue(bus.isRegistered(agent1));

            bus.unregister(agent1);

            assertFalse(bus.isRegistered(agent1), "Agent should be unregistered");
            assertEquals(0, bus.getRegisteredAgents().size());
        }

        @Test
        @DisplayName("Unregister non-existent agent does not throw")
        void unregisterNonExistentAgent() {
            assertDoesNotThrow(() -> bus.unregister(agent1),
                    "Unregistering non-existent agent should not throw");
        }

        @Test
        @DisplayName("Register null agent ID throws exception")
        void registerNullAgentId() {
            MessageHandler handler = message -> {};
            assertThrows(NullPointerException.class, () -> bus.register(null, handler),
                    "Should throw NullPointerException for null agentId");
        }

        @Test
        @DisplayName("Register null handler throws exception")
        void registerNullHandler() {
            assertThrows(NullPointerException.class, () -> bus.register(agent1, null),
                    "Should throw NullPointerException for null handler");
        }
    }

    @Nested
    @DisplayName("Message Routing Tests")
    class MessageRoutingTests {

        private List<AgentMessage> receivedMessages;
        private MessageHandler capturingHandler;

        @BeforeEach
        void setUp() {
            receivedMessages = new ArrayList<>();
            capturingHandler = message -> receivedMessages.add(message);

            bus.register(agent1, capturingHandler);
            bus.register(agent2, capturingHandler);
            bus.register(agent3, capturingHandler);
        }

        @Test
        @DisplayName("Send direct message from agent1 to agent2")
        void sendDirectMessage() {
            AgentMessage message = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.STATUS_UPDATE)
                    .content("Hello from agent1")
                    .build();

            bus.send(message);
            bus.tick();

            assertEquals(1, receivedMessages.size(), "Agent2 should receive one message");
            assertEquals(agent1, receivedMessages.get(0).senderId());
            assertEquals("Hello from agent1", receivedMessages.get(0).content());
        }

        @Test
        @DisplayName("Send multiple messages to same recipient")
        void sendMultipleMessages() {
            AgentMessage msg1 = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.STATUS_UPDATE)
                    .content("Message 1")
                    .build();

            AgentMessage msg2 = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.COORDINATION)
                    .content("Message 2")
                    .build();

            bus.send(msg1);
            bus.send(msg2);
            bus.tick();

            assertEquals(2, receivedMessages.size(), "Agent2 should receive two messages");
            assertEquals("Message 1", receivedMessages.get(0).content());
            assertEquals("Message 2", receivedMessages.get(1).content());
        }

        @Test
        @DisplayName("Send messages between different agent pairs")
        void sendBetweenDifferentPairs() {
            AgentMessage msg1 = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.STATUS_UPDATE)
                    .content("1 to 2")
                    .build();

            AgentMessage msg2 = new AgentMessage.Builder()
                    .sender(agent2)
                    .recipient(agent3)
                    .type(AgentMessage.MessageType.STATUS_UPDATE)
                    .content("2 to 3")
                    .build();

            AgentMessage msg3 = new AgentMessage.Builder()
                    .sender(agent3)
                    .recipient(agent1)
                    .type(AgentMessage.MessageType.STATUS_UPDATE)
                    .content("3 to 1")
                    .build();

            bus.send(msg1);
            bus.send(msg2);
            bus.send(msg3);
            bus.tick();

            assertEquals(3, receivedMessages.size());
        }

        @Test
        @DisplayName("Send to unregistered recipient logs warning")
        void sendToUnregisteredRecipient() {
            UUID unregisteredAgent = UUID.randomUUID();

            AgentMessage message = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(unregisteredAgent)
                    .type(AgentMessage.MessageType.STATUS_UPDATE)
                    .content("To nowhere")
                    .build();

            bus.send(message);
            bus.tick();

            assertEquals(0, receivedMessages.size(), "No messages should be delivered");
            assertEquals(1, bus.getStats().getFailed(), "Failed count should increment");
        }

        @Test
        @DisplayName("Message with payload is routed correctly")
        void messageWithPayload() {
            Map<String, Object> payload = Map.of(
                    "key1", "value1",
                    "key2", 42,
                    "key3", true
            );

            AgentMessage message = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.COORDINATION)
                    .content("Payload message")
                    .payload(payload)
                    .build();

            bus.send(message);
            bus.tick();

            assertEquals(1, receivedMessages.size());
            assertEquals("value1", receivedMessages.get(0).getPayload("key1"));
            assertEquals(42, receivedMessages.get(0).<Integer>getPayload("key2"));
            assertEquals(true, receivedMessages.get(0).getPayload("key3"));
        }
    }

    @Nested
    @DisplayName("Broadcast Message Tests")
    class BroadcastTests {

        private List<AgentMessage> agent1Messages;
        private List<AgentMessage> agent2Messages;
        private List<AgentMessage> agent3Messages;

        @BeforeEach
        void setUp() {
            agent1Messages = new ArrayList<>();
            agent2Messages = new ArrayList<>();
            agent3Messages = new ArrayList<>();

            bus.register(agent1, msg -> agent1Messages.add(msg));
            bus.register(agent2, msg -> agent2Messages.add(msg));
            bus.register(agent3, msg -> agent3Messages.add(msg));
        }

        @Test
        @DisplayName("Broadcast from agent1 reaches all other agents")
        void broadcastFromAgent1() {
            bus.broadcast(agent1, AgentMessage.MessageType.ALERT, "Danger detected!");
            bus.tick();

            assertEquals(0, agent1Messages.size(), "Sender should not receive own broadcast");
            assertEquals(1, agent2Messages.size(), "Agent2 should receive broadcast");
            assertEquals(1, agent3Messages.size(), "Agent3 should receive broadcast");
        }

        @Test
        @DisplayName("Broadcast with payload")
        void broadcastWithPayload() {
            Map<String, Object> payload = Map.of(
                    "location", "base",
                    "severity", "high"
            );

            bus.broadcast(agent1, AgentMessage.MessageType.ALERT,
                    "Emergency!", payload);
            bus.tick();

            assertEquals(1, agent2Messages.size());
            assertEquals("Emergency!", agent2Messages.get(0).content());
            assertEquals("base", agent2Messages.get(0).getPayload("location"));
            assertEquals("high", agent2Messages.get(0).getPayload("severity"));
        }

        @Test
        @DisplayName("Broadcast from different agents")
        void broadcastFromDifferentAgents() {
            bus.broadcast(agent1, AgentMessage.MessageType.STATUS_UPDATE, "Agent1 status");
            bus.broadcast(agent2, AgentMessage.MessageType.STATUS_UPDATE, "Agent2 status");
            bus.tick();

            assertEquals(1, agent1Messages.size(), "Agent1 should receive from agent2");
            assertEquals(1, agent2Messages.size(), "Agent2 should receive from agent1");
            assertEquals(2, agent3Messages.size(), "Agent3 should receive from both");
        }

        @Test
        @DisplayName("Broadcast message is flagged correctly")
        void broadcastMessageFlag() {
            bus.broadcast(agent1, AgentMessage.MessageType.ALERT, "Alert!");
            bus.tick();

            assertTrue(agent2Messages.get(0).isBroadcast(),
                    "Broadcast message should have isBroadcast() = true");
            assertNull(agent2Messages.get(0).recipientId(),
                    "Broadcast message should have null recipient");
        }
    }

    @Nested
    @DisplayName("Request/Response Tests")
    class RequestResponseTests {

        @BeforeEach
        void setUp() {
            bus.register(agent1, message -> {
                if (message.content().equals("REQUEST")) {
                    AgentMessage response = new AgentMessage.Builder()
                            .sender(agent1)
                            .recipient(message.senderId())
                            .type(AgentMessage.MessageType.RESPONSE)
                            .content("RESPONSE")
                            .correlationId(message.correlationId())
                            .build();
                    bus.sendResponse(response);
                }
            });
            bus.register(agent2, message -> {
                // Handle responses for agent2
            });
        }

        @Test
        @DisplayName("Send request and receive response")
        void sendRequestReceiveResponse() throws Exception {
            // Create request message
            AgentMessage request = new AgentMessage.Builder()
                    .sender(agent2)
                    .recipient(agent1)
                    .type(AgentMessage.MessageType.QUERY)
                    .content("REQUEST")
                    .build();

            // Start request thread that will wait for response
            Thread requestThread = new Thread(() -> {
                try {
                    bus.sendRequest(request, 5000);
                } catch (Exception e) {
                    // Ignore for test
                }
            });
            requestThread.start();

            // Give the request thread time to start and send the message
            Thread.sleep(100);

            // Process the request (deliver to agent1's handler)
            bus.tick();

            // Agent1's handler sends response - process it
            bus.tick();

            // Wait for request thread to complete
            requestThread.join(2000);

            // If we get here without exception, the request/response worked
            assertFalse(requestThread.isAlive(), "Request thread should have completed");
        }

        @Test
        @DisplayName("Send request with timeout")
        void sendRequestWithTimeout() {
            AgentMessage request = new AgentMessage.Builder()
                    .sender(agent2)
                    .recipient(agent1)
                    .type(AgentMessage.MessageType.QUERY)
                    .content("NO_RESPONSE")
                    .build();

            assertThrows(TimeoutException.class, () -> {
                bus.sendRequest(request, 100);
            }, "Should timeout when no response");
        }

        @Test
        @DisplayName("Request/response flow completes successfully")
        void requestResponseFlow() throws Exception {
            // This test verifies that the complete request/response flow works
            AgentMessage request = new AgentMessage.Builder()
                    .sender(agent2)
                    .recipient(agent1)
                    .type(AgentMessage.MessageType.QUERY)
                    .content("REQUEST")
                    .build();

            // sendRequest creates a correlated message and waits for response
            Thread requestThread = new Thread(() -> {
                try {
                    bus.sendRequest(request, 5000);
                } catch (Exception e) {
                    // Ignore for test - we're just verifying the flow doesn't hang
                }
            });
            requestThread.start();

            // Give thread time to start
            Thread.sleep(100);

            // Process the request (delivers to agent1's handler)
            bus.tick();

            // Process the response (delivers back to complete the future)
            bus.tick();

            // Wait for completion
            requestThread.join(2000);

            // Verify the thread completed (didn't timeout)
            assertFalse(requestThread.isAlive(),
                    "Request/response flow should complete without timeout");
        }
    }

    @Nested
    @DisplayName("Conversation Tracking Tests")
    class ConversationTests {

        @Test
        @DisplayName("Create conversation with participants")
        void createConversation() {
            Conversation conv = new Conversation(
                    UUID.randomUUID(),
                    new java.util.HashSet<UUID>(java.util.Arrays.asList(agent1, agent2)),
                    "Test conversation"
            );

            assertEquals(2, conv.getParticipants().size());
            assertTrue(conv.hasParticipant(agent1));
            assertTrue(conv.hasParticipant(agent2));
            assertEquals("Test conversation", conv.getDescription());
            assertEquals(Conversation.ConversationState.INITIATED, conv.getState());
        }

        @Test
        @DisplayName("Add messages to conversation")
        void addMessagesToConversation() {
            Conversation conv = new Conversation(
                    UUID.randomUUID(),
                    new java.util.HashSet<UUID>(java.util.Arrays.asList(agent1, agent2)),
                    "Test conversation"
            );

            AgentMessage msg1 = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.QUERY)
                    .content("Question")
                    .build();

            AgentMessage msg2 = new AgentMessage.Builder()
                    .sender(agent2)
                    .recipient(agent1)
                    .type(AgentMessage.MessageType.RESPONSE)
                    .content("Answer")
                    .build();

            conv.addMessage(msg1);
            conv.addMessage(msg2);

            assertEquals(2, conv.getMessageCount());
            assertTrue(conv.getState() == Conversation.ConversationState.ACTIVE);
        }

        @Test
        @DisplayName("Add message from non-participant throws exception")
        void addMessageFromNonParticipant() {
            Conversation conv = new Conversation(
                    UUID.randomUUID(),
                    new java.util.HashSet<UUID>(java.util.Arrays.asList(agent1, agent2)),
                    "Test conversation"
            );

            AgentMessage msg = new AgentMessage.Builder()
                    .sender(agent3)
                    .recipient(agent1)
                    .type(AgentMessage.MessageType.QUERY)
                    .content("From outsider")
                    .build();

            assertThrows(IllegalArgumentException.class, () -> conv.addMessage(msg),
                    "Should throw when adding message from non-participant");
        }

        @Test
        @DisplayName("Get last message from specific agent")
        void getLastMessageFromAgent() {
            Conversation conv = new Conversation(
                    UUID.randomUUID(),
                    new java.util.HashSet<UUID>(java.util.Arrays.asList(agent1, agent2)),
                    "Test conversation"
            );

            AgentMessage msg1 = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.QUERY)
                    .content("First")
                    .build();

            AgentMessage msg2 = new AgentMessage.Builder()
                    .sender(agent2)
                    .recipient(agent1)
                    .type(AgentMessage.MessageType.RESPONSE)
                    .content("Response")
                    .build();

            AgentMessage msg3 = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.COORDINATION)
                    .content("Second from agent1")
                    .build();

            conv.addMessage(msg1);
            conv.addMessage(msg2);
            conv.addMessage(msg3);

            assertTrue(conv.getLastFrom(agent1).isPresent());
            assertEquals("Second from agent1", conv.getLastFrom(agent1).get().content());
        }

        @Test
        @DisplayName("Get messages by type")
        void getMessagesByType() {
            Conversation conv = new Conversation(
                    UUID.randomUUID(),
                    new java.util.HashSet<UUID>(java.util.Arrays.asList(agent1, agent2)),
                    "Test conversation"
            );

            AgentMessage msg1 = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.QUERY)
                    .content("Query 1")
                    .build();

            AgentMessage msg2 = new AgentMessage.Builder()
                    .sender(agent2)
                    .recipient(agent1)
                    .type(AgentMessage.MessageType.RESPONSE)
                    .content("Response")
                    .build();

            AgentMessage msg3 = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.QUERY)
                    .content("Query 2")
                    .build();

            conv.addMessage(msg1);
            conv.addMessage(msg2);
            conv.addMessage(msg3);

            List<AgentMessage> queries = conv.getMessagesOfType(AgentMessage.MessageType.QUERY);
            assertEquals(2, queries.size());

            List<AgentMessage> responses = conv.getMessagesOfType(AgentMessage.MessageType.RESPONSE);
            assertEquals(1, responses.size());
        }

        @Test
        @DisplayName("State transitions are validated")
        void stateTransitions() {
            Conversation conv = new Conversation(
                    UUID.randomUUID(),
                    new java.util.HashSet<UUID>(java.util.Arrays.asList(agent1, agent2)),
                    "Test conversation"
            );

            assertEquals(Conversation.ConversationState.INITIATED, conv.getState());

            conv.transitionTo(Conversation.ConversationState.ACTIVE);
            assertEquals(Conversation.ConversationState.ACTIVE, conv.getState());

            conv.transitionTo(Conversation.ConversationState.PAUSED);
            assertEquals(Conversation.ConversationState.PAUSED, conv.getState());

            conv.resume();
            assertEquals(Conversation.ConversationState.ACTIVE, conv.getState());

            conv.complete();
            assertEquals(Conversation.ConversationState.COMPLETED, conv.getState());
        }

        @Test
        @DisplayName("Invalid state transition throws exception")
        void invalidStateTransition() {
            Conversation conv = new Conversation(
                    UUID.randomUUID(),
                    new java.util.HashSet<UUID>(java.util.Arrays.asList(agent1, agent2)),
                    "Test conversation"
            );

            assertThrows(IllegalStateException.class, () ->
                    conv.transitionTo(Conversation.ConversationState.COMPLETED),
                    "Cannot transition from INITIATED to COMPLETED directly");
        }

        @Test
        @DisplayName("Conversation timeout")
        void conversationTimeout() throws InterruptedException {
            Conversation conv = new Conversation(
                    UUID.randomUUID(),
                    new java.util.HashSet<UUID>(java.util.Arrays.asList(agent1, agent2)),
                    "Test conversation",
                    Duration.ofMillis(100)
            );

            assertFalse(conv.isTimedOut(), "Should not be timed out initially");

            Thread.sleep(150);

            assertTrue(conv.isTimedOut(), "Should be timed out after timeout duration");
        }

        @Test
        @DisplayName("Conversation involves both agents")
        void conversationInvolvesBothAgents() {
            Conversation conv = new Conversation(
                    UUID.randomUUID(),
                    new java.util.HashSet<UUID>(java.util.Arrays.asList(agent1, agent2, agent3)),
                    "Test conversation"
            );

            assertTrue(conv.involves(agent1, agent2));
            assertTrue(conv.involves(agent2, agent3));
            assertFalse(conv.involves(agent1, UUID.randomUUID()));
        }
    }

    @Nested
    @DisplayName("Protocol Handshaking Tests")
    class HandshakeTests {

        private List<AgentMessage> agent1Received;
        private List<AgentMessage> agent2Received;

        @BeforeEach
        void setUp() {
            agent1Received = new ArrayList<>();
            agent2Received = new ArrayList<>();

            bus.register(agent1, msg -> agent1Received.add(msg));
            bus.register(agent2, msg -> agent2Received.add(msg));
        }

        @Test
        @DisplayName("Handshake init message is sent")
        void handshakeInitSent() {
            CommunicationProtocol.HandshakeProtocol handshake =
                    new CommunicationProtocol.HandshakeProtocol(bus);

            handshake.initiate(agent1, agent2, Map.of("version", "1.0"), result -> {});
            bus.tick();

            assertEquals(1, agent2Received.size());
            assertEquals("HANDSHAKE_INIT", agent2Received.get(0).content());
            assertEquals(AgentMessage.MessageType.QUERY, agent2Received.get(0).type());
        }

        @Test
        @DisplayName("Handshake includes handshake ID in payload")
        void handshakeIncludesId() {
            CommunicationProtocol.HandshakeProtocol handshake =
                    new CommunicationProtocol.HandshakeProtocol(bus);

            handshake.initiate(agent1, agent2, Map.of("version", "1.0"), result -> {});
            bus.tick();

            assertNotNull(agent2Received.get(0).getPayload("handshakeId"));
        }

        @Test
        @DisplayName("Handle handshake init sends ACK")
        void handleHandshakeInit() {
            CommunicationProtocol.HandshakeProtocol handshake =
                    new CommunicationProtocol.HandshakeProtocol(bus);

            AgentMessage initMsg = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.QUERY)
                    .content("HANDSHAKE_INIT")
                    .payload("handshakeId", UUID.randomUUID())
                    .build();

            bus.send(initMsg);
            bus.tick();

            // Verify agent2 received the HANDSHAKE_INIT
            assertEquals(1, agent2Received.size());
            assertEquals("HANDSHAKE_INIT", agent2Received.get(0).content());

            // Manually call handleHandshake to simulate agent2 processing the message
            handshake.handleHandshake(agent2Received.get(0));
            bus.tick();

            // Now agent1 should have received the HANDSHAKE_ACK
            assertEquals(1, agent1Received.size());
            assertEquals("HANDSHAKE_ACK", agent1Received.get(0).content());
            assertEquals(AgentMessage.MessageType.RESPONSE, agent1Received.get(0).type());
        }

        @Test
        @DisplayName("Handshake ACK includes capabilities")
        void handshakeAckIncludesCapabilities() {
            CommunicationProtocol.HandshakeProtocol handshake =
                    new CommunicationProtocol.HandshakeProtocol(bus);

            AgentMessage initMsg = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.QUERY)
                    .content("HANDSHAKE_INIT")
                    .payload("handshakeId", UUID.randomUUID())
                    .build();

            bus.send(initMsg);
            bus.tick();

            // Now agent2Received should have the HANDSHAKE_INIT
            assertEquals(1, agent2Received.size());
            assertEquals("HANDSHAKE_INIT", agent2Received.get(0).content());

            // Agent2's handler should process the HANDSHAKE_INIT and send HANDSHAKE_ACK
            // For this test, we manually call handleHandshake to simulate agent2 processing
            handshake.handleHandshake(agent2Received.get(0));
            bus.tick();

            // agent1Received should now have the HANDSHAKE_ACK
            assertFalse(agent1Received.isEmpty(), "Agent1 should receive HANDSHAKE_ACK");
            assertEquals("HANDSHAKE_ACK", agent1Received.get(0).content());

            assertNotNull(agent1Received.get(0).getPayload("capabilities"));
        }

        @Test
        @DisplayName("Handshake callback can be registered")
        void handshakeCallbackInvoked() {
            CommunicationProtocol.HandshakeProtocol handshake =
                    new CommunicationProtocol.HandshakeProtocol(bus);

            boolean[] callbackCalled = {false};

            handshake.initiate(agent1, agent2, Map.of("version", "1.0"), result -> {
                callbackCalled[0] = true;
                // Verify the callback receives a result
                assertNotNull(result);
            });

            bus.tick();

            // Verify HANDSHAKE_INIT was sent to agent2
            assertEquals(1, agent2Received.size());
            assertEquals("HANDSHAKE_INIT", agent2Received.get(0).content());

            // The callback itself is called when HANDSHAKE_ACK is processed
            // This test verifies the callback can be registered without error
            assertFalse(callbackCalled[0], "Callback should not be called until ACK is processed");
        }
    }

    @Nested
    @DisplayName("Message Statistics Tests")
    class StatisticsTests {

        @BeforeEach
        void setUp() {
            bus.register(agent1, MessageHandler.noop());
            bus.register(agent2, message -> {
                if (message.content().equals("error")) {
                    throw new RuntimeException("Test error");
                }
            });
        }

        @Test
        @DisplayName("Track sent messages")
        void trackSentMessages() {
            bus.broadcast(agent1, AgentMessage.MessageType.STATUS_UPDATE, "Status 1");
            bus.broadcast(agent1, AgentMessage.MessageType.ALERT, "Alert 1");

            CommunicationBus.MessageStats stats = bus.getStats();

            assertEquals(2, stats.getSent());
        }

        @Test
        @DisplayName("Track delivered messages")
        void trackDeliveredMessages() {
            bus.broadcast(agent1, AgentMessage.MessageType.STATUS_UPDATE, "Status");
            bus.tick();

            assertEquals(1, bus.getStats().getDelivered());
        }

        @Test
        @DisplayName("Track received messages")
        void trackReceivedMessages() {
            AgentMessage msg = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.STATUS_UPDATE)
                    .content("Hello")
                    .build();

            bus.send(msg);
            bus.tick();

            assertEquals(1, bus.getStats().getReceived());
        }

        @Test
        @DisplayName("Track failed messages")
        void trackFailedMessages() {
            AgentMessage errorMsg = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.STATUS_UPDATE)
                    .content("error")
                    .build();

            bus.send(errorMsg);
            bus.tick();

            assertEquals(1, bus.getStats().getFailed());
        }

        @Test
        @DisplayName("Track messages by type")
        void trackMessagesByType() {
            bus.broadcast(agent1, AgentMessage.MessageType.STATUS_UPDATE, "Status");
            bus.broadcast(agent1, AgentMessage.MessageType.ALERT, "Alert");
            bus.broadcast(agent1, AgentMessage.MessageType.STATUS_UPDATE, "Status 2");

            Map<AgentMessage.MessageType, Long> byType = bus.getStats().getSentByType();

            assertEquals(2, byType.get(AgentMessage.MessageType.STATUS_UPDATE));
            assertEquals(1, byType.get(AgentMessage.MessageType.ALERT));
        }

        @Test
        @DisplayName("Statistics are cumulative")
        void statisticsCumulative() {
            for (int i = 0; i < 5; i++) {
                bus.broadcast(agent1, AgentMessage.MessageType.STATUS_UPDATE, "Status " + i);
            }
            bus.tick();

            assertEquals(5, bus.getStats().getSent());
            assertEquals(5, bus.getStats().getDelivered());
            assertEquals(5, bus.getStats().getReceived());
        }
    }

    @Nested
    @DisplayName("Queue Management Tests")
    class QueueManagementTests {

        @Test
        @DisplayName("Get pending count for agent")
        void getPendingCount() {
            List<AgentMessage> received = new ArrayList<>();
            bus.register(agent1, msg -> received.add(msg));
            bus.register(agent2, MessageHandler.noop());

            // Send messages without ticking (don't process)
            for (int i = 0; i < 3; i++) {
                AgentMessage msg = new AgentMessage.Builder()
                        .sender(agent1)
                        .recipient(agent2)
                        .type(AgentMessage.MessageType.STATUS_UPDATE)
                        .content("Message " + i)
                        .build();
                bus.send(msg);
            }

            assertEquals(3, bus.getPendingCount(agent2));
        }

        @Test
        @DisplayName("Pending count decreases after processing")
        void pendingCountDecreases() {
            List<AgentMessage> received = new ArrayList<>();
            bus.register(agent1, msg -> received.add(msg));
            bus.register(agent2, msg -> received.add(msg));

            AgentMessage msg = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.STATUS_UPDATE)
                    .content("Message")
                    .build();

            bus.send(msg);
            assertEquals(1, bus.getPendingCount(agent2));

            bus.tick();
            assertEquals(0, bus.getPendingCount(agent2));
        }

        @Test
        @DisplayName("Clear all queues")
        void clearQueues() {
            List<AgentMessage> received = new ArrayList<>();
            bus.register(agent1, msg -> received.add(msg));
            bus.register(agent2, msg -> received.add(msg));

            bus.broadcast(agent1, AgentMessage.MessageType.STATUS_UPDATE, "Status");
            bus.broadcast(agent1, AgentMessage.MessageType.ALERT, "Alert");

            assertEquals(2, bus.getPendingCount(agent2));

            bus.clear();

            assertEquals(0, bus.getPendingCount(agent2));
        }
    }

    @Nested
    @DisplayName("Message History Tests")
    class HistoryTests {

        @Test
        @DisplayName("Messages are recorded in history")
        void messagesRecordedInHistory() {
            bus.register(agent1, MessageHandler.noop());
            bus.register(agent2, MessageHandler.noop());

            AgentMessage msg = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.STATUS_UPDATE)
                    .content("History test")
                    .build();

            bus.send(msg);

            List<AgentMessage> history = bus.getHistory(10);

            assertEquals(1, history.size());
            assertEquals("History test", history.get(0).content());
        }

        @Test
        @DisplayName("Get history respects count limit")
        void historyRespectsLimit() {
            bus.register(agent1, MessageHandler.noop());
            bus.register(agent2, MessageHandler.noop());

            for (int i = 0; i < 10; i++) {
                bus.broadcast(agent1, AgentMessage.MessageType.STATUS_UPDATE, "Msg " + i);
            }

            List<AgentMessage> history = bus.getHistory(5);

            assertEquals(5, history.size());
        }

        @Test
        @DisplayName("History returns most recent messages first")
        void historyMostRecentFirst() {
            bus.register(agent1, MessageHandler.noop());
            bus.register(agent2, MessageHandler.noop());

            bus.broadcast(agent1, AgentMessage.MessageType.STATUS_UPDATE, "First");
            bus.broadcast(agent1, AgentMessage.MessageType.ALERT, "Second");
            bus.broadcast(agent1, AgentMessage.MessageType.COORDINATION, "Third");

            List<AgentMessage> history = bus.getHistory(10);

            assertEquals("Third", history.get(0).content());
            assertEquals("Second", history.get(1).content());
            assertEquals("First", history.get(2).content());
        }
    }

    @Nested
    @DisplayName("Lifecycle Tests")
    class LifecycleTests {

        @Test
        @DisplayName("Bus is running initially")
        void busRunningInitially() {
            assertTrue(bus.isRunning());
        }

        @Test
        @DisplayName("Shutdown stops message processing")
        void shutdownStopsProcessing() {
            List<AgentMessage> received = new ArrayList<>();
            bus.register(agent1, msg -> received.add(msg));
            bus.register(agent2, msg -> received.add(msg));

            bus.shutdown();

            AgentMessage msg = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.STATUS_UPDATE)
                    .content("Should not be delivered")
                    .build();

            bus.send(msg);
            bus.tick();

            assertEquals(0, received.size(), "No messages should be delivered after shutdown");
        }

        @Test
        @DisplayName("Tick does nothing when not running")
        void tickWhenNotRunning() {
            List<AgentMessage> received = new ArrayList<>();
            bus.register(agent1, msg -> received.add(msg));
            bus.register(agent2, msg -> received.add(msg));

            AgentMessage msg = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.STATUS_UPDATE)
                    .content("Message")
                    .build();

            bus.send(msg);
            bus.shutdown();
            bus.tick();

            assertEquals(0, received.size());
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Concurrent message sending")
        void concurrentMessageSending() throws InterruptedException {
            List<AgentMessage> received = new CopyOnWriteArrayList<>();
            bus.register(agent1, msg -> received.add(msg));
            bus.register(agent2, msg -> received.add(msg));

            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                new Thread(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < 10; j++) {
                            AgentMessage msg = new AgentMessage.Builder()
                                    .sender(agent1)
                                    .recipient(agent2)
                                    .type(AgentMessage.MessageType.STATUS_UPDATE)
                                    .content("Thread " + index + " Message " + j)
                                    .build();
                            bus.send(msg);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            doneLatch.await();
            bus.tick();

            assertEquals(threadCount * 10, received.size());
        }

        @Test
        @DisplayName("Concurrent registration")
        void concurrentRegistration() throws InterruptedException {
            int threadCount = 20;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        UUID agentId = UUID.randomUUID();
                        bus.register(agentId, MessageHandler.noop());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            doneLatch.await();

            assertEquals(threadCount, bus.getRegisteredAgents().size());
        }
    }

    @Nested
    @DisplayName("AgentMessage Builder Tests")
    class AgentMessageTests {

        @Test
        @DisplayName("Builder creates valid message")
        void builderCreatesValidMessage() {
            AgentMessage message = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.STATUS_UPDATE)
                    .content("Test")
                    .build();

            assertEquals(agent1, message.senderId());
            assertEquals(agent2, message.recipientId());
            assertEquals(AgentMessage.MessageType.STATUS_UPDATE, message.type());
            assertEquals("Test", message.content());
        }

        @Test
        @DisplayName("Builder with payload")
        void builderWithPayload() {
            Map<String, Object> payload = Map.of("key", "value");

            AgentMessage message = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.COORDINATION)
                    .content("Test")
                    .payload(payload)
                    .build();

            assertEquals("value", message.getPayload("key"));
        }

        @Test
        @DisplayName("Builder with correlation ID")
        void builderWithCorrelationId() {
            UUID correlationId = UUID.randomUUID();

            AgentMessage message = new AgentMessage.Builder()
                    .sender(agent1)
                    .recipient(agent2)
                    .type(AgentMessage.MessageType.QUERY)
                    .content("Test")
                    .correlationId(correlationId)
                    .build();

            assertEquals(correlationId, message.correlationId());
            assertTrue(message.isResponse());
        }

        @Test
        @DisplayName("Broadcast message has null recipient")
        void broadcastMessageHasNullRecipient() {
            AgentMessage message = new AgentMessage.Builder()
                    .sender(agent1)
                    .type(AgentMessage.MessageType.ALERT)
                    .content("Broadcast")
                    .build();

            assertNull(message.recipientId());
            assertTrue(message.isBroadcast());
        }

        @Test
        @DisplayName("Builder without required fields throws exception")
        void builderWithoutRequiredFields() {
            assertThrows(NullPointerException.class, () ->
                    new AgentMessage.Builder()
                            .sender(agent1)
                            .type(AgentMessage.MessageType.STATUS_UPDATE)
                            // Missing content
                            .build()
            );
        }
    }
}
