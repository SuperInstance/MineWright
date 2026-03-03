package com.minewright.observability.span;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SpanKind} enum.
 *
 * <p>Tests verify the span kind enumeration values and their properties:</p>
 * <ul>
 *   <li>Enum values existence</li>
 *   <li>Value properties and semantics</li>
 *   <li>Name representation</li>
 *   <li>Ordinal ordering</li>
 *   <li>Enum functionality</li>
 * </ul>
 *
 * @see SpanKind
 */
@DisplayName("Span Kind Tests")
class SpanKindTest {

    @Nested
    @DisplayName("Enum Values Tests")
    class EnumValuesTests {

        @Test
        @DisplayName("INTERNAL enum value exists")
        void internalEnumValueExists() {
            assertNotNull(SpanKind.INTERNAL,
                    "INTERNAL span kind should exist");
        }

        @Test
        @DisplayName("CLIENT enum value exists")
        void clientEnumValueExists() {
            assertNotNull(SpanKind.CLIENT,
                    "CLIENT span kind should exist");
        }

        @Test
        @DisplayName("SERVER enum value exists")
        void serverEnumValueExists() {
            assertNotNull(SpanKind.SERVER,
                    "SERVER span kind should exist");
        }

        @Test
        @DisplayName("PRODUCER enum value exists")
        void producerEnumValueExists() {
            assertNotNull(SpanKind.PRODUCER,
                    "PRODUCER span kind should exist");
        }

        @Test
        @DisplayName("CONSUMER enum value exists")
        void consumerEnumValueExists() {
            assertNotNull(SpanKind.CONSUMER,
                    "CONSUMER span kind should exist");
        }

        @Test
        @DisplayName("All enum values are distinct")
        void allEnumValuesAreDistinct() {
            assertNotEquals(SpanKind.INTERNAL, SpanKind.CLIENT);
            assertNotEquals(SpanKind.INTERNAL, SpanKind.SERVER);
            assertNotEquals(SpanKind.INTERNAL, SpanKind.PRODUCER);
            assertNotEquals(SpanKind.INTERNAL, SpanKind.CONSUMER);
            assertNotEquals(SpanKind.CLIENT, SpanKind.SERVER);
            assertNotEquals(SpanKind.CLIENT, SpanKind.PRODUCER);
            assertNotEquals(SpanKind.CLIENT, SpanKind.CONSUMER);
            assertNotEquals(SpanKind.SERVER, SpanKind.PRODUCER);
            assertNotEquals(SpanKind.SERVER, SpanKind.CONSUMER);
            assertNotEquals(SpanKind.PRODUCER, SpanKind.CONSUMER);
        }
    }

    @Nested
    @DisplayName("Enum Properties Tests")
    class EnumPropertiesTests {

        @Test
        @DisplayName("INTERNAL has correct name")
        void internalHasCorrectName() {
            assertEquals("INTERNAL", SpanKind.INTERNAL.name(),
                    "INTERNAL should have correct name");
        }

        @Test
        @DisplayName("CLIENT has correct name")
        void clientHasCorrectName() {
            assertEquals("CLIENT", SpanKind.CLIENT.name(),
                    "CLIENT should have correct name");
        }

        @Test
        @DisplayName("SERVER has correct name")
        void serverHasCorrectName() {
            assertEquals("SERVER", SpanKind.SERVER.name(),
                    "SERVER should have correct name");
        }

        @Test
        @DisplayName("PRODUCER has correct name")
        void producerHasCorrectName() {
            assertEquals("PRODUCER", SpanKind.PRODUCER.name(),
                    "PRODUCER should have correct name");
        }

        @Test
        @DisplayName("CONSUMER has correct name")
        void consumerHasCorrectName() {
            assertEquals("CONSUMER", SpanKind.CONSUMER.name(),
                    "CONSUMER should have correct name");
        }
    }

    @Nested
    @DisplayName("Enum Functionality Tests")
    class EnumFunctionalityTests {

        @Test
        @DisplayName("Values returns all enum constants")
        void valuesReturnsAllEnumConstants() {
            SpanKind[] values = SpanKind.values();

            assertEquals(5, values.length,
                    "Should have 5 span kind values");
            assertTrue(java.util.Arrays.asList(values).contains(SpanKind.INTERNAL));
            assertTrue(java.util.Arrays.asList(values).contains(SpanKind.CLIENT));
            assertTrue(java.util.Arrays.asList(values).contains(SpanKind.SERVER));
            assertTrue(java.util.Arrays.asList(values).contains(SpanKind.PRODUCER));
            assertTrue(java.util.Arrays.asList(values).contains(SpanKind.CONSUMER));
        }

        @Test
        @DisplayName("ValueOf returns correct enum constant")
        void valueOfReturnsCorrectEnumConstant() {
            assertEquals(SpanKind.INTERNAL, SpanKind.valueOf("INTERNAL"));
            assertEquals(SpanKind.CLIENT, SpanKind.valueOf("CLIENT"));
            assertEquals(SpanKind.SERVER, SpanKind.valueOf("SERVER"));
            assertEquals(SpanKind.PRODUCER, SpanKind.valueOf("PRODUCER"));
            assertEquals(SpanKind.CONSUMER, SpanKind.valueOf("CONSUMER"));
        }

        @Test
        @DisplayName("ValueOf throws on invalid name")
        void valueOfThrowsOnInvalidName() {
            assertThrows(IllegalArgumentException.class,
                    () -> SpanKind.valueOf("INVALID"),
                    "Should throw IllegalArgumentException for invalid name");
        }

        @Test
        @DisplayName("ValueOf throws on null name")
        void valueOfThrowsOnNullName() {
            assertThrows(NullPointerException.class,
                    () -> SpanKind.valueOf(null),
                    "Should throw NullPointerException for null name");
        }

        @Test
        @DisplayName("ValueOf is case sensitive")
        void valueOfIsCaseSensitive() {
            assertThrows(IllegalArgumentException.class,
                    () -> SpanKind.valueOf("internal"),
                    "Should throw for lowercase name");
            assertThrows(IllegalArgumentException.class,
                    () -> SpanKind.valueOf("Internal"),
                    "Should throw for mixed case name");
        }
    }

    @Nested
    @DisplayName("Semantic Meaning Tests")
    class SemanticMeaningTests {

        @Test
        @DisplayName("INTERNAL represents root or internal operations")
        void internalRepresentsRootOrInternalOperations() {
            // INTERNAL is used for spans that don't involve remote calls
            SpanKind kind = SpanKind.INTERNAL;
            assertEquals("INTERNAL", kind.name());
        }

        @Test
        @DisplayName("CLIENT represents outgoing remote calls")
        void clientRepresentsOutgoingRemoteCalls() {
            // CLIENT is used when the application makes a remote call
            SpanKind kind = SpanKind.CLIENT;
            assertEquals("CLIENT", kind.name());
        }

        @Test
        @DisplayName("SERVER represents incoming remote calls")
        void serverRepresentsIncomingRemoteCalls() {
            // SERVER is used when the application receives a remote call
            SpanKind kind = SpanKind.SERVER;
            assertEquals("SERVER", kind.name());
        }

        @Test
        @DisplayName("PRODUCER represents message sending")
        void producerRepresentsMessageSending() {
            // PRODUCER is used when sending messages to external systems
            SpanKind kind = SpanKind.PRODUCER;
            assertEquals("PRODUCER", kind.name());
        }

        @Test
        @DisplayName("CONSUMER represents message receiving")
        void consumerRepresentsMessageReceiving() {
            // CONSUMER is used when receiving messages from external systems
            SpanKind kind = SpanKind.CONSUMER;
            assertEquals("CONSUMER", kind.name());
        }
    }

    @Nested
    @DisplayName("Enum Comparison Tests")
    class EnumComparisonTests {

        @Test
        @DisplayName("Span kinds can be compared")
        void spanKindsCanBeCompared() {
            assertTrue(SpanKind.INTERNAL.compareTo(SpanKind.CLIENT) < 0);
            assertTrue(SpanKind.CLIENT.compareTo(SpanKind.SERVER) < 0);
            assertTrue(SpanKind.SERVER.compareTo(SpanKind.PRODUCER) < 0);
            assertTrue(SpanKind.PRODUCER.compareTo(SpanKind.CONSUMER) < 0);
        }

        @Test
        @DisplayName("Same span kind compares as equal")
        void sameSpanKindComparesAsEqual() {
            assertEquals(0, SpanKind.INTERNAL.compareTo(SpanKind.INTERNAL));
            assertEquals(0, SpanKind.CLIENT.compareTo(SpanKind.CLIENT));
            assertEquals(0, SpanKind.SERVER.compareTo(SpanKind.SERVER));
            assertEquals(0, SpanKind.PRODUCER.compareTo(SpanKind.PRODUCER));
            assertEquals(0, SpanKind.CONSUMER.compareTo(SpanKind.CONSUMER));
        }

        @Test
        @DisplayName("Ordinal values are sequential")
        void ordinalValuesAreSequential() {
            assertEquals(0, SpanKind.INTERNAL.ordinal());
            assertEquals(1, SpanKind.CLIENT.ordinal());
            assertEquals(2, SpanKind.SERVER.ordinal());
            assertEquals(3, SpanKind.PRODUCER.ordinal());
            assertEquals(4, SpanKind.CONSUMER.ordinal());
        }
    }

    @Nested
    @DisplayName("Usage Scenarios Tests")
    class UsageScenariosTests {

        @Test
        @DisplayName("Span kinds can be used in switch statements")
        void spanKindsCanBeUsedInSwitchStatements() {
            SpanKind kind = SpanKind.CLIENT;
            String description = "";

            switch (kind) {
                case INTERNAL:
                    description = "internal operation";
                    break;
                case CLIENT:
                    description = "outgoing call";
                    break;
                case SERVER:
                    description = "incoming call";
                    break;
                case PRODUCER:
                    description = "message producer";
                    break;
                case CONSUMER:
                    description = "message consumer";
                    break;
            }

            assertEquals("outgoing call", description);
        }

        @Test
        @DisplayName("Span kinds can be stored in collections")
        void spanKindsCanBeStoredInCollections() {
            java.util.List<SpanKind> kinds = new java.util.ArrayList<>();
            kinds.add(SpanKind.INTERNAL);
            kinds.add(SpanKind.CLIENT);
            kinds.add(SpanKind.SERVER);

            assertEquals(3, kinds.size());
            assertTrue(kinds.contains(SpanKind.CLIENT));
        }

        @Test
        @DisplayName("Span kinds can be used as map keys")
        void spanKindsCanBeUsedAsMapKeys() {
            java.util.Map<SpanKind, String> descriptions = new java.util.EnumMap<>(SpanKind.class);
            descriptions.put(SpanKind.INTERNAL, "Internal operation");
            descriptions.put(SpanKind.CLIENT, "Client call");

            assertEquals("Internal operation", descriptions.get(SpanKind.INTERNAL));
            assertEquals("Client call", descriptions.get(SpanKind.CLIENT));
        }

        @Test
        @DisplayName("Span kinds can be used in sets")
        void spanKindsCanBeUsedInSets() {
            java.util.Set<SpanKind> kinds = java.util.EnumSet.of(SpanKind.CLIENT, SpanKind.SERVER);

            assertTrue(kinds.contains(SpanKind.CLIENT));
            assertTrue(kinds.contains(SpanKind.SERVER));
            assertFalse(kinds.contains(SpanKind.INTERNAL));
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Enum constants are singletons")
        void enumConstantsAreSingletons() {
            SpanKind internal1 = SpanKind.INTERNAL;
            SpanKind internal2 = SpanKind.INTERNAL;

            assertSame(internal1, internal2,
                    "Enum constants should be singleton instances");
        }

        @Test
        @DisplayName("All span kinds have non-null names")
        void allSpanKindsHaveNonNullNames() {
            for (SpanKind kind : SpanKind.values()) {
                assertNotNull(kind.name());
                assertFalse(kind.name().isEmpty());
            }
        }

        @Test
        @DisplayName("All span kinds are comparable")
        void allSpanKindsAreComparable() {
            SpanKind[] kinds = SpanKind.values();

            for (int i = 0; i < kinds.length; i++) {
                for (int j = 0; j < kinds.length; j++) {
                    int result = kinds[i].compareTo(kinds[j]);
                    if (i == j) {
                        assertEquals(0, result);
                    } else if (i < j) {
                        assertTrue(result < 0);
                    } else {
                        assertTrue(result > 0);
                    }
                }
            }
        }
    }
}
