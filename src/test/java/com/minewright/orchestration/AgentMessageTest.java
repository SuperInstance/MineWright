package com.minewright.orchestration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link AgentMessage}.
 *
 * <p>Tests cover the message format including:</p>
 * <ul>
 *   <li>Message construction with Builder</li>
 *   <li>Factory methods for common message types</li>
 *   <li>Message types and priorities</li>
 *   <li>Payload handling and type-safe access</li>
 *   <li>Broadcast and direct message detection</li>
 *   <li>Message properties and getters</li>
 *   <li>String representation</li>
 *   <li>Edge cases and validation</li>
 * </ul>
 *
 * @see AgentMessage
 */
@DisplayName("Agent Message Tests")
class AgentMessageTest {

    private static final String SENDER_ID = "foreman";
    private static final String SENDER_NAME = "Foreman";
    private static final String RECIPIENT_ID = "worker1";
    private static final String CONTENT = "Test message content";

    // ==================== Builder Tests ====================

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Build message with all fields")
        void buildWithAllFields() {
            Map<String, Object> payload = new HashMap<>();
            payload.put("task", "mining");
            payload.put("quantity", 64);

            AgentMessage message = new AgentMessage.Builder()
                .type(AgentMessage.Type.TASK_ASSIGNMENT)
                .sender(SENDER_ID, SENDER_NAME)
                .recipient(RECIPIENT_ID)
                .content(CONTENT)
                .payload(payload)
                .priority(AgentMessage.Priority.HIGH)
                .correlationId("corr-123")
                .build();

            assertEquals(AgentMessage.Type.TASK_ASSIGNMENT, message.getType());
            assertEquals(SENDER_ID, message.getSenderId());
            assertEquals(SENDER_NAME, message.getSenderName());
            assertEquals(RECIPIENT_ID, message.getRecipientId());
            assertEquals(CONTENT, message.getContent());
            assertEquals(payload, message.getPayload());
            assertEquals(AgentMessage.Priority.HIGH, message.getPriority());
            assertEquals("corr-123", message.getCorrelationId());
            assertNotNull(message.getMessageId());
            assertNotNull(message.getTimestamp());
        }

        @Test
        @DisplayName("Build message with required fields only")
        void buildWithRequiredFieldsOnly() {
            AgentMessage message = new AgentMessage.Builder()
                .senderId(SENDER_ID)
                .build();

            assertNotNull(message);
            assertEquals(SENDER_ID, message.getSenderId());
            assertEquals(AgentMessage.Type.COORDINATION, message.getType(),
                "Default type should be COORDINATION");
            assertEquals(AgentMessage.Priority.NORMAL, message.getPriority(),
                "Default priority should be NORMAL");
        }

        @Test
        @DisplayName("Build without senderId throws exception")
        void buildWithoutSenderIdThrows() {
            assertThrows(IllegalStateException.class, () -> {
                new AgentMessage.Builder()
                    .senderName("Sender")
                    .build();
            }, "Builder should require senderId");
        }

        @Test
        @DisplayName("Builder supports fluent API")
        void builderFluentAPI() {
            AgentMessage message = new AgentMessage.Builder()
                .type(AgentMessage.Type.BROADCAST)
                .sender(SENDER_ID, SENDER_NAME)
                .recipient("*")
                .content("Fluent test")
                .priority(AgentMessage.Priority.URGENT)
                .build();

            assertEquals(AgentMessage.Type.BROADCAST, message.getType());
            assertEquals(SENDER_ID, message.getSenderId());
        }

        @Test
        @DisplayName("Individual builder methods work independently")
        void individualBuilderMethods() {
            AgentMessage message = new AgentMessage.Builder()
                .type(AgentMessage.Type.HELP_REQUEST)
                .senderId("agent1")
                .senderName("Agent One")
                .recipientId("agent2")
                .content("Need help")
                .priority(AgentMessage.Priority.HIGH)
                .build();

            assertEquals("agent1", message.getSenderId());
            assertEquals("Agent One", message.getSenderName());
            assertEquals("agent2", message.getRecipientId());
        }

        @Test
        @DisplayName("Builder with payload entries")
        void builderWithPayloadEntries() {
            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .payload("key1", "value1")
                .payload("key2", 42)
                .payload("key3", true)
                .build();

            assertEquals("value1", message.<String>getPayloadValue("key1"));
            assertEquals(42, message.<Integer>getPayloadValue("key2"));
            assertEquals(true, message.<Boolean>getPayloadValue("key3"));
        }

        @Test
        @DisplayName("Builder with combined payload")
        void builderWithCombinedPayload() {
            Map<String, Object> initialPayload = new HashMap<>();
            initialPayload.put("initial", "value");

            Map<String, Object> additionalPayload = new HashMap<>();
            additionalPayload.put("additional", 123);

            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .payload(initialPayload)
                .payload(additionalPayload)
                .build();

            assertEquals("value", (String) message.getPayloadValue("initial"));
            assertEquals(Integer.valueOf(123), (Integer) message.getPayloadValue("additional"));
        }

        @Test
        @DisplayName("Multiple builds create unique message IDs")
        void multipleBuildsCreateUniqueIds() {
            AgentMessage message1 = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME).build();
            AgentMessage message2 = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME).build();

            assertNotEquals(message1.getMessageId(), message2.getMessageId(),
                "Each message should have unique ID");
        }

        @Test
        @DisplayName("Builder creates message with current timestamp")
        void builderCreatesCurrentTimestamp() {
            Instant before = Instant.now();

            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME).build();

            Instant after = Instant.now();

            assertTrue(!message.getTimestamp().isBefore(before),
                "Timestamp should be after start");
            assertTrue(!message.getTimestamp().isAfter(after),
                "Timestamp should be before end");
        }
    }

    // ==================== Factory Method Tests ====================

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("taskAssignment factory method")
        void taskAssignmentFactory() {
            Map<String, Object> params = Map.of("block", "stone", "quantity", 64);

            AgentMessage message = AgentMessage.taskAssignment(
                SENDER_ID, SENDER_NAME, RECIPIENT_ID,
                "Mine stone", params
            );

            assertEquals(AgentMessage.Type.TASK_ASSIGNMENT, message.getType());
            assertEquals(SENDER_ID, message.getSenderId());
            assertEquals(RECIPIENT_ID, message.getRecipientId());
            assertEquals("Mine stone", message.getContent());
            assertEquals(AgentMessage.Priority.NORMAL, message.getPriority());

            // Check payload
            assertEquals("Mine stone", (String) message.getPayloadValue("taskDescription"));
            assertEquals("stone", (String) message.getPayloadValue("block"));
            assertEquals(Integer.valueOf(64), (Integer) message.getPayloadValue("quantity"));
        }

        @Test
        @DisplayName("taskProgress factory method")
        void taskProgressFactory() {
            AgentMessage message = AgentMessage.taskProgress(
                RECIPIENT_ID, "Worker One", SENDER_ID,
                "task-123", 75, "Mining stone at location"
            );

            assertEquals(AgentMessage.Type.TASK_PROGRESS, message.getType());
            assertEquals(RECIPIENT_ID, message.getSenderId());
            assertEquals(SENDER_ID, message.getRecipientId());
            assertEquals("task-123", (String) message.getPayloadValue("taskId"));
            assertEquals(Integer.valueOf(75), (Integer) message.getPayloadValue("percentComplete"));
            assertEquals("Mining stone at location", (String) message.getPayloadValue("status"));
            assertEquals(AgentMessage.Priority.LOW, message.getPriority());
        }

        @Test
        @DisplayName("taskComplete factory method with success")
        void taskCompleteFactorySuccess() {
            AgentMessage message = AgentMessage.taskComplete(
                RECIPIENT_ID, "Worker One", SENDER_ID,
                "task-123", true, "Completed successfully"
            );

            assertEquals(AgentMessage.Type.TASK_COMPLETE, message.getType());
            assertEquals(true, message.getPayloadValue("success"));
            assertEquals("Completed successfully", message.getPayloadValue("result"));
            assertEquals(AgentMessage.Priority.NORMAL, message.getPriority());
        }

        @Test
        @DisplayName("taskComplete factory method with failure")
        void taskCompleteFactoryFailure() {
            AgentMessage message = AgentMessage.taskComplete(
                RECIPIENT_ID, "Worker One", SENDER_ID,
                "task-123", false, "Path blocked"
            );

            assertEquals(AgentMessage.Type.TASK_FAILED, message.getType());
            assertEquals(false, message.getPayloadValue("success"));
            assertEquals("Path blocked", message.getPayloadValue("result"));
            assertEquals(AgentMessage.Priority.HIGH, message.getPriority());
        }

        @Test
        @DisplayName("helpRequest factory method")
        void helpRequestFactory() {
            AgentMessage message = AgentMessage.helpRequest(
                RECIPIENT_ID, "Worker One", SENDER_ID,
                "Stuck and cannot reach target"
            );

            assertEquals(AgentMessage.Type.HELP_REQUEST, message.getType());
            assertEquals(RECIPIENT_ID, message.getSenderId());
            assertEquals(SENDER_ID, message.getRecipientId());
            assertEquals("Stuck and cannot reach target", message.getContent());
            assertEquals(AgentMessage.Priority.HIGH, message.getPriority());
        }

        @Test
        @DisplayName("broadcast factory method")
        void broadcastFactory() {
            AgentMessage message = AgentMessage.broadcast(
                SENDER_ID, SENDER_NAME,
                "Team meeting in 5 minutes",
                AgentMessage.Priority.NORMAL
            );

            assertEquals(AgentMessage.Type.BROADCAST, message.getType());
            assertEquals(SENDER_ID, message.getSenderId());
            assertEquals("*", message.getRecipientId());
            assertEquals("Team meeting in 5 minutes", message.getContent());
            assertTrue(message.isBroadcast(), "Should be broadcast message");
        }

        @Test
        @DisplayName("humanCommand factory method")
        void humanCommandFactory() {
            AgentMessage message = AgentMessage.humanCommand(
                SENDER_ID, SENDER_NAME,
                "Build a house",
                null
            );

            assertEquals(AgentMessage.Type.HUMAN_COMMAND, message.getType());
            assertEquals(SENDER_ID, message.getSenderId());
            assertEquals("*", message.getRecipientId(), "No target should broadcast");
            assertEquals("Build a house", message.getContent());
            assertEquals(AgentMessage.Priority.HIGH, message.getPriority());
        }

        @Test
        @DisplayName("humanCommand factory method with target")
        void humanCommandFactoryWithTarget() {
            AgentMessage message = AgentMessage.humanCommand(
                SENDER_ID, SENDER_NAME,
                "Mine stone",
                "worker1"
            );

            assertEquals("worker1", message.getRecipientId());
            assertFalse(message.isBroadcast(), "Targeted command should not be broadcast");
        }
    }

    // ==================== Message Type Tests ====================

    @Nested
    @DisplayName("Message Type Tests")
    class MessageTypeTests {

        @Test
        @DisplayName("All message types have correct codes")
        void allMessageTypeCodes() {
            assertEquals("task_assignment", AgentMessage.Type.TASK_ASSIGNMENT.getCode());
            assertEquals("task_progress", AgentMessage.Type.TASK_PROGRESS.getCode());
            assertEquals("task_complete", AgentMessage.Type.TASK_COMPLETE.getCode());
            assertEquals("task_failed", AgentMessage.Type.TASK_FAILED.getCode());
            assertEquals("help_request", AgentMessage.Type.HELP_REQUEST.getCode());
            assertEquals("status_query", AgentMessage.Type.STATUS_QUERY.getCode());
            assertEquals("status_report", AgentMessage.Type.STATUS_REPORT.getCode());
            assertEquals("coordination", AgentMessage.Type.COORDINATION.getCode());
            assertEquals("broadcast", AgentMessage.Type.BROADCAST.getCode());
            assertEquals("human_command", AgentMessage.Type.HUMAN_COMMAND.getCode());
            assertEquals("plan_announcement", AgentMessage.Type.PLAN_ANNOUNCEMENT.getCode());
        }

        @Test
        @DisplayName("All message types have authority flags")
        void messageTypeAuthorityFlags() {
            assertTrue(AgentMessage.Type.TASK_ASSIGNMENT.requiresAuthority());
            assertFalse(AgentMessage.Type.TASK_PROGRESS.requiresAuthority());
            assertFalse(AgentMessage.Type.TASK_COMPLETE.requiresAuthority());
            assertFalse(AgentMessage.Type.TASK_FAILED.requiresAuthority());
            assertFalse(AgentMessage.Type.HELP_REQUEST.requiresAuthority());
            assertTrue(AgentMessage.Type.STATUS_QUERY.requiresAuthority());
            assertFalse(AgentMessage.Type.STATUS_REPORT.requiresAuthority());
            assertFalse(AgentMessage.Type.COORDINATION.requiresAuthority());
            assertTrue(AgentMessage.Type.BROADCAST.requiresAuthority());
            assertTrue(AgentMessage.Type.HUMAN_COMMAND.requiresAuthority());
            assertTrue(AgentMessage.Type.PLAN_ANNOUNCEMENT.requiresAuthority());
        }

        @Test
        @DisplayName("Message type values are accessible")
        void messageTypeValues() {
            AgentMessage.Type[] types = AgentMessage.Type.values();

            assertEquals(11, types.length, "Should have 11 message types");
        }
    }

    // ==================== Priority Tests ====================

    @Nested
    @DisplayName("Priority Tests")
    class PriorityTests {

        @Test
        @DisplayName("All priorities have correct levels")
        void allPriorityLevels() {
            assertEquals(1, AgentMessage.Priority.LOW.getLevel());
            assertEquals(5, AgentMessage.Priority.NORMAL.getLevel());
            assertEquals(10, AgentMessage.Priority.HIGH.getLevel());
            assertEquals(20, AgentMessage.Priority.URGENT.getLevel());
            assertEquals(50, AgentMessage.Priority.CRITICAL.getLevel());
        }

        @Test
        @DisplayName("Priorities are ordered correctly")
        void prioritiesOrdered() {
            assertTrue(AgentMessage.Priority.LOW.getLevel() <
                       AgentMessage.Priority.NORMAL.getLevel());
            assertTrue(AgentMessage.Priority.NORMAL.getLevel() <
                       AgentMessage.Priority.HIGH.getLevel());
            assertTrue(AgentMessage.Priority.HIGH.getLevel() <
                       AgentMessage.Priority.URGENT.getLevel());
            assertTrue(AgentMessage.Priority.URGENT.getLevel() <
                       AgentMessage.Priority.CRITICAL.getLevel());
        }

        @Test
        @DisplayName("Priority values are accessible")
        void priorityValues() {
            AgentMessage.Priority[] priorities = AgentMessage.Priority.values();

            assertEquals(5, priorities.length, "Should have 5 priority levels");
        }
    }

    // ==================== Payload Tests ====================

    @Nested
    @DisplayName("Payload Tests")
    class PayloadTests {

        private AgentMessage message;
        private Map<String, Object> payload;

        @BeforeEach
        void setUp() {
            payload = new HashMap<>();
            payload.put("stringKey", "stringValue");
            payload.put("intKey", 42);
            payload.put("boolKey", true);
            payload.put("doubleKey", 3.14);
            payload.put("nullKey", null);

            message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .payload(payload)
                .build();
        }

        @Test
        @DisplayName("Get payload returns full map")
        void getPayload() {
            assertEquals(payload, message.getPayload());
        }

        @Test
        @DisplayName("Get payload value with correct type")
        void getPayloadValueCorrectType() {
            assertEquals("stringValue", message.getPayloadValue("stringKey"));
            assertEquals(Integer.valueOf(42), message.getPayloadValue("intKey"));
            assertEquals(Boolean.TRUE, message.getPayloadValue("boolKey"));
            assertEquals(3.14, message.getPayloadValue("doubleKey"));
        }

        @Test
        @DisplayName("Get payload value with default")
        void getPayloadValueWithDefault() {
            assertEquals("defaultValue", message.getPayloadValue("nonexistent", "defaultValue"));
            assertEquals(999, message.getPayloadValue("nonexistent", 999));
        }

        @Test
        @DisplayName("Get payload value returns existing value even with default")
        void getPayloadValueExistingWithDefault() {
            assertEquals("stringValue", message.getPayloadValue("stringKey", "defaultValue"));
        }

        @Test
        @DisplayName("Get payload value with null returns default")
        void getPayloadValueNullWithDefault() {
            assertEquals("default", message.getPayloadValue("nullKey", "default"));
        }

        @Test
        @DisplayName("Get payload value with type casting")
        void getPayloadValueTypeCasting() {
            String stringValue = message.<String>getPayloadValue("stringKey");
            Integer intValue = message.<Integer>getPayloadValue("intKey");
            Boolean boolValue = message.<Boolean>getPayloadValue("boolKey");

            assertEquals("stringValue", stringValue);
            assertEquals(42, intValue);
            assertEquals(true, boolValue);
        }

        @Test
        @DisplayName("Payload supports complex types")
        void payloadComplexTypes() {
            Map<String, Object> complexPayload = new HashMap<>();
            complexPayload.put("list", List.of("a", "b", "c"));
            complexPayload.put("nested", Map.of("key", "value"));

            AgentMessage complexMessage = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .payload(complexPayload)
                .build();

            assertEquals(List.of("a", "b", "c"), complexMessage.getPayloadValue("list"));
            assertEquals(Map.of("key", "value"), complexMessage.getPayloadValue("nested"));
        }

        @Test
        @DisplayName("Empty payload is handled")
        void emptyPayload() {
            AgentMessage emptyMessage = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .build();

            assertTrue(emptyMessage.getPayload().isEmpty(),
                "Default payload should be empty");
        }
    }

    // ==================== Broadcast Tests ====================

    @Nested
    @DisplayName("Broadcast Tests")
    class BroadcastTests {

        @Test
        @DisplayName("Is broadcast with asterisk recipient")
        void isBroadcastWithAsterisk() {
            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .recipient("*")
                .build();

            assertTrue(message.isBroadcast());
        }

        @Test
        @DisplayName("Is broadcast with null recipient")
        void isBroadcastWithNull() {
            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .recipientId(null)
                .build();

            assertTrue(message.isBroadcast());
        }

        @Test
        @DisplayName("Is not broadcast with specific recipient")
        void isNotBroadcastWithSpecificRecipient() {
            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .recipient("worker1")
                .build();

            assertFalse(message.isBroadcast());
        }

        @Test
        @DisplayName("Broadcast factory creates broadcast message")
        void broadcastFactoryCreatesBroadcast() {
            AgentMessage message = AgentMessage.broadcast(
                SENDER_ID, SENDER_NAME, "Test", AgentMessage.Priority.NORMAL
            );

            assertTrue(message.isBroadcast());
            assertEquals("*", message.getRecipientId());
        }
    }

    // ==================== Message ID Tests ====================

    @Nested
    @DisplayName("Message ID Tests")
    class MessageIdTests {

        @Test
        @DisplayName("Message ID is 8 characters")
        void messageIdLength() {
            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME).build();

            assertEquals(8, message.getMessageId().length(),
                "Message ID should be 8 characters");
        }

        @Test
        @DisplayName("Message IDs are unique")
        void messageIdsAreUnique() {
            Set<String> ids = new java.util.HashSet<>();

            for (int i = 0; i < 100; i++) {
                AgentMessage message = new AgentMessage.Builder()
                    .sender("sender" + i, "Sender " + i).build();
                ids.add(message.getMessageId());
            }

            assertEquals(100, ids.size(), "All IDs should be unique");
        }

        @Test
        @DisplayName("Message ID is hexadecimal")
        void messageIdIsHexadecimal() {
            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME).build();

            String id = message.getMessageId();
            assertTrue(id.matches("[0-9a-f-]+"),
                "Message ID should contain only hexadecimal characters");
        }
    }

    // ==================== Timestamp Tests ====================

    @Nested
    @DisplayName("Timestamp Tests")
    class TimestampTests {

        @Test
        @DisplayName("Timestamp is set on creation")
        void timestampSetOnCreation() {
            Instant before = Instant.now();

            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME).build();

            Instant after = Instant.now();

            assertTrue(!message.getTimestamp().isBefore(before));
            assertTrue(!message.getTimestamp().isAfter(after));
        }

        @Test
        @DisplayName("Timestamp is immutable")
        void timestampIsImmutable() {
            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME).build();

            Instant timestamp1 = message.getTimestamp();
            Instant timestamp2 = message.getTimestamp();

            assertEquals(timestamp1, timestamp2);
        }
    }

    // ==================== Correlation ID Tests ====================

    @Nested
    @DisplayName("Correlation ID Tests")
    class CorrelationIdTests {

        @Test
        @DisplayName("Correlation ID can be set")
        void correlationIdSet() {
            String correlationId = "request-123";

            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .correlationId(correlationId)
                .build();

            assertEquals(correlationId, message.getCorrelationId());
        }

        @Test
        @DisplayName("Correlation ID can be null")
        void correlationIdNull() {
            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .correlationId(null)
                .build();

            assertNull(message.getCorrelationId());
        }

        @Test
        @DisplayName("Default correlation ID is null")
        void defaultCorrelationId() {
            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .build();

            assertNull(message.getCorrelationId());
        }
    }

    // ==================== ToString Tests ====================

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("ToString contains message type")
        void toStringContainsType() {
            AgentMessage message = new AgentMessage.Builder()
                .type(AgentMessage.Type.TASK_ASSIGNMENT)
                .sender(SENDER_ID, SENDER_NAME)
                .build();

            String result = message.toString();
            assertTrue(result.contains("task_assignment"));
        }

        @Test
        @DisplayName("ToString contains sender and recipient")
        void toStringContainsSenderAndRecipient() {
            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .recipient(RECIPIENT_ID)
                .build();

            String result = message.toString();
            assertTrue(result.contains(SENDER_NAME));
            assertTrue(result.contains(RECIPIENT_ID));
        }

        @Test
        @DisplayName("ToString contains priority")
        void toStringContainsPriority() {
            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .priority(AgentMessage.Priority.URGENT)
                .build();

            String result = message.toString();
            assertTrue(result.contains("URGENT"));
        }

        @Test
        @DisplayName("ToString truncates long content")
        void toStringTruncatesLongContent() {
            String longContent = "A".repeat(100);

            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .content(longContent)
                .build();

            String result = message.toString();
            assertTrue(result.length() < 100,
                "ToString should truncate long content");
            assertTrue(result.contains("..."),
                "Truncated content should have ellipsis");
        }

        @Test
        @DisplayName("ToString handles null content")
        void toStringHandlesNullContent() {
            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .content(null)
                .build();

            assertDoesNotThrow(() -> message.toString(),
                "ToString should handle null content");
        }
    }

    // ==================== Edge Cases Tests ====================

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Message with empty strings")
        void messageWithEmptyStrings() {
            AgentMessage message = new AgentMessage.Builder()
                .senderId("")
                .senderName("")
                .recipient("")
                .content("")
                .build();

            assertEquals("", message.getSenderId());
            assertEquals("", message.getSenderName());
            assertEquals("", message.getRecipientId());
            assertEquals("", message.getContent());
        }

        @Test
        @DisplayName("Message with very long strings")
        void messageWithVeryLongStrings() {
            String longString = "A".repeat(10000);

            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .content(longString)
                .build();

            assertEquals(longString, message.getContent());
        }

        @Test
        @DisplayName("Message with special characters in content")
        void messageWithSpecialCharacters() {
            String specialContent = "Test\n\t\r\b\f\u0000<>\"'&\\\\";

            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .content(specialContent)
                .build();

            assertEquals(specialContent, message.getContent());
        }

        @Test
        @DisplayName("Message with unicode content")
        void messageWithUnicodeContent() {
            String unicodeContent = "Hello \u4e16\u754c \ud83d\ude00";

            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .content(unicodeContent)
                .build();

            assertEquals(unicodeContent, message.getContent());
        }

        @Test
        @DisplayName("Message with zero in payload")
        void messageWithZeroInPayload() {
            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .payload("zero", 0)
                .build();

            assertEquals(Integer.valueOf(0), message.getPayloadValue("zero"));
        }

        @Test
        @DisplayName("Message with empty string in payload")
        void messageWithEmptyStringInPayload() {
            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .payload("empty", "")
                .build();

            assertEquals("", message.getPayloadValue("empty"));
            assertEquals("default", message.getPayloadValue("empty", "default"));
        }

        @Test
        @DisplayName("Message with collection in payload")
        void messageWithCollectionInPayload() {
            List<String> items = List.of("item1", "item2", "item3");

            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .payload("items", items)
                .build();

            assertEquals(items, message.getPayloadValue("items"));
        }

        @Test
        @DisplayName("Message retains all payload entries")
        void messageRetainsAllPayloadEntries() {
            Map<String, Object> largePayload = new HashMap<>();
            for (int i = 0; i < 100; i++) {
                largePayload.put("key" + i, "value" + i);
            }

            AgentMessage message = new AgentMessage.Builder()
                .sender(SENDER_ID, SENDER_NAME)
                .payload(largePayload)
                .build();

            assertEquals(100, message.getPayload().size());
            for (int i = 0; i < 100; i++) {
                assertEquals("value" + i, message.getPayloadValue("key" + i));
            }
        }
    }

    // ==================== Integration Tests ====================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Create and use complete task assignment workflow")
        void completeTaskAssignmentWorkflow() {
            // Create task assignment
            Map<String, Object> taskParams = Map.of(
                "block", "stone",
                "quantity", 64,
                "location", List.of(100, 64, 200)
            );

            AgentMessage assignment = AgentMessage.taskAssignment(
                "foreman", "Foreman", "worker1",
                "Mine stone at location", taskParams
            );

            // Worker reports progress
            AgentMessage progress = AgentMessage.taskProgress(
                "worker1", "Worker One", "foreman",
                assignment.getMessageId(), 25, "Mining stone"
            );

            // Worker completes task
            AgentMessage completion = AgentMessage.taskComplete(
                "worker1", "Worker One", "foreman",
                assignment.getMessageId(), true, "Mined 64 stone"
            );

            // Verify all messages
            assertEquals(AgentMessage.Type.TASK_ASSIGNMENT, assignment.getType());
            assertEquals(AgentMessage.Type.TASK_PROGRESS, progress.getType());
            assertEquals(AgentMessage.Type.TASK_COMPLETE, completion.getType());

            assertEquals(assignment.getMessageId(), progress.getPayloadValue("taskId"));
            assertEquals(assignment.getMessageId(), completion.getPayloadValue("taskId"));
        }

        @Test
        @DisplayName("Broadcast announcement workflow")
        void broadcastAnnouncementWorkflow() {
            // Foreman announces plan
            AgentMessage announcement = new AgentMessage.Builder()
                .type(AgentMessage.Type.PLAN_ANNOUNCEMENT)
                .sender("foreman", "Foreman")
                .recipient("*")
                .content("New mining plan starting")
                .payload("planId", "plan-123")
                .payload("taskCount", 5)
                .priority(AgentMessage.Priority.NORMAL)
                .build();

            // Workers acknowledge
            AgentMessage ack1 = new AgentMessage.Builder()
                .type(AgentMessage.Type.STATUS_REPORT)
                .sender("worker1", "Worker One")
                .recipient("foreman")
                .content("Acknowledged plan")
                .payload("planId", "plan-123")
                .priority(AgentMessage.Priority.NORMAL)
                .build();

            assertTrue(announcement.isBroadcast());
            assertFalse(ack1.isBroadcast());
            assertEquals("plan-123", announcement.getPayloadValue("planId"));
            assertEquals("plan-123", ack1.getPayloadValue("planId"));
        }

        @Test
        @DisplayName("Help request and response workflow")
        void helpRequestWorkflow() {
            // Worker requests help
            AgentMessage helpRequest = AgentMessage.helpRequest(
                "worker1", "Worker One", "foreman",
                "Stuck at location, cannot find path"
            );

            // Foreman responds
            AgentMessage response = new AgentMessage.Builder()
                .type(AgentMessage.Type.COORDINATION)
                .sender("foreman", "Foreman")
                .recipient("worker1")
                .content("Try alternate route via coordinates")
                .correlationId(helpRequest.getMessageId())
                .priority(AgentMessage.Priority.HIGH)
                .build();

            assertEquals(AgentMessage.Type.HELP_REQUEST, helpRequest.getType());
            assertEquals(AgentMessage.Priority.HIGH, helpRequest.getPriority());
            assertEquals(helpRequest.getMessageId(), response.getCorrelationId());
        }
    }
}
