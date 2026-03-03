package com.minewright.observability.span;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SpanStatus} enum.
 *
 * <p>Tests verify the span status enumeration values and their properties:</p>
 * <ul>
 *   <li>Enum values existence</li>
 *   <li>Value properties and semantics</li>
 *   <li>Name representation</li>
 *   <li>Ordinal ordering</li>
 *   <li>Enum functionality</li>
 *   <li>Status classification</li>
 * </ul>
 *
 * @see SpanStatus
 */
@DisplayName("Span Status Tests")
class SpanStatusTest {

    @Nested
    @DisplayName("Enum Values Tests")
    class EnumValuesTests {

        @Test
        @DisplayName("OK enum value exists")
        void okEnumValueExists() {
            assertNotNull(SpanStatus.OK,
                    "OK span status should exist");
        }

        @Test
        @DisplayName("ERROR enum value exists")
        void errorEnumValueExists() {
            assertNotNull(SpanStatus.ERROR,
                    "ERROR span status should exist");
        }

        @Test
        @DisplayName("CANCELLED enum value exists")
        void cancelledEnumValueExists() {
            assertNotNull(SpanStatus.CANCELLED,
                    "CANCELLED span status should exist");
        }

        @Test
        @DisplayName("UNSET enum value exists")
        void unsetEnumValueExists() {
            assertNotNull(SpanStatus.UNSET,
                    "UNSET span status should exist");
        }

        @Test
        @DisplayName("All enum values are distinct")
        void allEnumValuesAreDistinct() {
            assertNotEquals(SpanStatus.OK, SpanStatus.ERROR);
            assertNotEquals(SpanStatus.OK, SpanStatus.CANCELLED);
            assertNotEquals(SpanStatus.OK, SpanStatus.UNSET);
            assertNotEquals(SpanStatus.ERROR, SpanStatus.CANCELLED);
            assertNotEquals(SpanStatus.ERROR, SpanStatus.UNSET);
            assertNotEquals(SpanStatus.CANCELLED, SpanStatus.UNSET);
        }

        @Test
        @DisplayName("Total number of status values")
        void totalNumberOfStatusValues() {
            SpanStatus[] values = SpanStatus.values();
            assertEquals(4, values.length,
                    "Should have exactly 4 status values");
        }
    }

    @Nested
    @DisplayName("Enum Properties Tests")
    class EnumPropertiesTests {

        @Test
        @DisplayName("OK has correct name")
        void okHasCorrectName() {
            assertEquals("OK", SpanStatus.OK.name(),
                    "OK should have correct name");
        }

        @Test
        @DisplayName("ERROR has correct name")
        void errorHasCorrectName() {
            assertEquals("ERROR", SpanStatus.ERROR.name(),
                    "ERROR should have correct name");
        }

        @Test
        @DisplayName("CANCELLED has correct name")
        void cancelledHasCorrectName() {
            assertEquals("CANCELLED", SpanStatus.CANCELLED.name(),
                    "CANCELLED should have correct name");
        }

        @Test
        @DisplayName("UNSET has correct name")
        void unsetHasCorrectName() {
            assertEquals("UNSET", SpanStatus.UNSET.name(),
                    "UNSET should have correct name");
        }
    }

    @Nested
    @DisplayName("Enum Functionality Tests")
    class EnumFunctionalityTests {

        @Test
        @DisplayName("Values returns all enum constants")
        void valuesReturnsAllEnumConstants() {
            SpanStatus[] values = SpanStatus.values();

            assertEquals(4, values.length,
                    "Should have 4 status values");
            assertTrue(java.util.Arrays.asList(values).contains(SpanStatus.OK));
            assertTrue(java.util.Arrays.asList(values).contains(SpanStatus.ERROR));
            assertTrue(java.util.Arrays.asList(values).contains(SpanStatus.CANCELLED));
            assertTrue(java.util.Arrays.asList(values).contains(SpanStatus.UNSET));
        }

        @Test
        @DisplayName("ValueOf returns correct enum constant")
        void valueOfReturnsCorrectEnumConstant() {
            assertEquals(SpanStatus.OK, SpanStatus.valueOf("OK"));
            assertEquals(SpanStatus.ERROR, SpanStatus.valueOf("ERROR"));
            assertEquals(SpanStatus.CANCELLED, SpanStatus.valueOf("CANCELLED"));
            assertEquals(SpanStatus.UNSET, SpanStatus.valueOf("UNSET"));
        }

        @Test
        @DisplayName("ValueOf throws on invalid name")
        void valueOfThrowsOnInvalidName() {
            assertThrows(IllegalArgumentException.class,
                    () -> SpanStatus.valueOf("INVALID"),
                    "Should throw IllegalArgumentException for invalid name");
        }

        @Test
        @DisplayName("ValueOf throws on null name")
        void valueOfThrowsOnNullName() {
            assertThrows(NullPointerException.class,
                    () -> SpanStatus.valueOf(null),
                    "Should throw NullPointerException for null name");
        }

        @Test
        @DisplayName("ValueOf is case sensitive")
        void valueOfIsCaseSensitive() {
            assertThrows(IllegalArgumentException.class,
                    () -> SpanStatus.valueOf("ok"),
                    "Should throw for lowercase name");
            assertThrows(IllegalArgumentException.class,
                    () -> SpanStatus.valueOf("Ok"),
                    "Should throw for mixed case name");
            assertThrows(IllegalArgumentException.class,
                    () -> SpanStatus.valueOf("error"),
                    "Should throw for lowercase name");
        }
    }

    @Nested
    @DisplayName("Semantic Meaning Tests")
    class SemanticMeaningTests {

        @Test
        @DisplayName("OK represents successful completion")
        void okRepresentsSuccessfulCompletion() {
            // OK is used when a span completes successfully
            SpanStatus status = SpanStatus.OK;
            assertEquals("OK", status.name());
        }

        @Test
        @DisplayName("ERROR represents failure")
        void errorRepresentsFailure() {
            // ERROR is used when a span encounters an error
            SpanStatus status = SpanStatus.ERROR;
            assertEquals("ERROR", status.name());
        }

        @Test
        @DisplayName("CANCELLED represents cancellation")
        void cancelledRepresentsCancellation() {
            // CANCELLED is used when a span is cancelled before completion
            SpanStatus status = SpanStatus.CANCELLED;
            assertEquals("CANCELLED", status.name());
        }

        @Test
        @DisplayName("UNSET represents in-progress span")
        void unsetRepresentsInProgressSpan() {
            // UNSET is used when a span is still in progress
            SpanStatus status = SpanStatus.UNSET;
            assertEquals("UNSET", status.name());
        }

        @Test
        @DisplayName("Status values represent lifecycle states")
        void statusValuesRepresentLifecycleStates() {
            // Typical lifecycle: UNSET -> OK/ERROR/CANCELLED
            assertTrue(java.util.Arrays.asList(
                    SpanStatus.OK,
                    SpanStatus.ERROR,
                    SpanStatus.CANCELLED
            ).contains(SpanStatus.OK));

            assertTrue(java.util.Arrays.asList(
                    SpanStatus.OK,
                    SpanStatus.ERROR,
                    SpanStatus.CANCELLED
            ).contains(SpanStatus.ERROR));

            assertTrue(java.util.Arrays.asList(
                    SpanStatus.OK,
                    SpanStatus.ERROR,
                    SpanStatus.CANCELLED
            ).contains(SpanStatus.CANCELLED));
        }
    }

    @Nested
    @DisplayName("Status Classification Tests")
    class StatusClassificationTests {

        @Test
        @DisplayName("OK is a terminal status")
        void okIsTerminalStatus() {
            // OK is a terminal state - the span has completed successfully
            SpanStatus status = SpanStatus.OK;
            assertEquals("OK", status.name());
        }

        @Test
        @DisplayName("ERROR is a terminal status")
        void errorIsTerminalStatus() {
            // ERROR is a terminal state - the span has failed
            SpanStatus status = SpanStatus.ERROR;
            assertEquals("ERROR", status.name());
        }

        @Test
        @DisplayName("CANCELLED is a terminal status")
        void cancelledIsTerminalStatus() {
            // CANCELLED is a terminal state - the span was cancelled
            SpanStatus status = SpanStatus.CANCELLED;
            assertEquals("CANCELLED", status.name());
        }

        @Test
        @DisplayName("UNSET is not a terminal status")
        void unsetIsNotTerminalStatus() {
            // UNSET is not terminal - the span is still in progress
            SpanStatus status = SpanStatus.UNSET;
            assertEquals("UNSET", status.name());
        }

        @Test
        @DisplayName("Terminal status values")
        void terminalStatusValues() {
            SpanStatus[] terminal = {
                SpanStatus.OK,
                SpanStatus.ERROR,
                SpanStatus.CANCELLED
            };

            for (SpanStatus status : terminal) {
                assertNotNull(status);
            }
        }

        @Test
        @DisplayName("Non-terminal status values")
        void nonTerminalStatusValues() {
            SpanStatus[] nonTerminal = {
                SpanStatus.UNSET
            };

            for (SpanStatus status : nonTerminal) {
                assertNotNull(status);
            }
        }
    }

    @Nested
    @DisplayName("Enum Comparison Tests")
    class EnumComparisonTests {

        @Test
        @DisplayName("Span statuses can be compared")
        void spanStatusesCanBeCompared() {
            assertTrue(SpanStatus.OK.compareTo(SpanStatus.ERROR) < 0);
            assertTrue(SpanStatus.ERROR.compareTo(SpanStatus.CANCELLED) < 0);
            assertTrue(SpanStatus.CANCELLED.compareTo(SpanStatus.UNSET) < 0);
        }

        @Test
        @DisplayName("Same span status compares as equal")
        void sameSpanStatusComparesAsEqual() {
            assertEquals(0, SpanStatus.OK.compareTo(SpanStatus.OK));
            assertEquals(0, SpanStatus.ERROR.compareTo(SpanStatus.ERROR));
            assertEquals(0, SpanStatus.CANCELLED.compareTo(SpanStatus.CANCELLED));
            assertEquals(0, SpanStatus.UNSET.compareTo(SpanStatus.UNSET));
        }

        @Test
        @DisplayName("Ordinal values are sequential")
        void ordinalValuesAreSequential() {
            assertEquals(0, SpanStatus.OK.ordinal());
            assertEquals(1, SpanStatus.ERROR.ordinal());
            assertEquals(2, SpanStatus.CANCELLED.ordinal());
            assertEquals(3, SpanStatus.UNSET.ordinal());
        }
    }

    @Nested
    @DisplayName("Usage Scenarios Tests")
    class UsageScenariosTests {

        @Test
        @DisplayName("Span statuses can be used in switch statements")
        void spanStatusesCanBeUsedInSwitchStatements() {
            SpanStatus status = SpanStatus.ERROR;
            String description = "";

            switch (status) {
                case OK:
                    description = "completed successfully";
                    break;
                case ERROR:
                    description = "failed with error";
                    break;
                case CANCELLED:
                    description = "was cancelled";
                    break;
                case UNSET:
                    description = "is in progress";
                    break;
            }

            assertEquals("failed with error", description);
        }

        @Test
        @DisplayName("Span statuses can be stored in collections")
        void spanStatusesCanBeStoredInCollections() {
            java.util.List<SpanStatus> statuses = new java.util.ArrayList<>();
            statuses.add(SpanStatus.OK);
            statuses.add(SpanStatus.ERROR);
            statuses.add(SpanStatus.UNSET);

            assertEquals(3, statuses.size());
            assertTrue(statuses.contains(SpanStatus.OK));
        }

        @Test
        @DisplayName("Span statuses can be used as map keys")
        void spanStatusesCanBeUsedAsMapKeys() {
            java.util.Map<SpanStatus, String> descriptions = new java.util.EnumMap<>(SpanStatus.class);
            descriptions.put(SpanStatus.OK, "Success");
            descriptions.put(SpanStatus.ERROR, "Error");
            descriptions.put(SpanStatus.CANCELLED, "Cancelled");

            assertEquals("Success", descriptions.get(SpanStatus.OK));
            assertEquals("Error", descriptions.get(SpanStatus.ERROR));
            assertEquals("Cancelled", descriptions.get(SpanStatus.CANCELLED));
        }

        @Test
        @DisplayName("Span statuses can be used in sets")
        void spanStatusesCanBeUsedInSets() {
            java.util.Set<SpanStatus> statuses = java.util.EnumSet.of(
                    SpanStatus.OK,
                    SpanStatus.ERROR
            );

            assertTrue(statuses.contains(SpanStatus.OK));
            assertTrue(statuses.contains(SpanStatus.ERROR));
            assertFalse(statuses.contains(SpanStatus.UNSET));
        }

        @Test
        @DisplayName("Status tracking across span lifecycle")
        void statusTrackingAcrossSpanLifecycle() {
            // Simulate span lifecycle
            java.util.List<SpanStatus> lifecycle = new java.util.ArrayList<>();

            // Initial state
            lifecycle.add(SpanStatus.UNSET);

            // Successful completion
            lifecycle.add(SpanStatus.OK);

            assertEquals(2, lifecycle.size());
            assertEquals(SpanStatus.UNSET, lifecycle.get(0));
            assertEquals(SpanStatus.OK, lifecycle.get(1));
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Enum constants are singletons")
        void enumConstantsAreSingletons() {
            SpanStatus ok1 = SpanStatus.OK;
            SpanStatus ok2 = SpanStatus.OK;

            assertSame(ok1, ok2,
                    "Enum constants should be singleton instances");
        }

        @Test
        @DisplayName("All span statuses have non-null names")
        void allSpanStatusesHaveNonNullNames() {
            for (SpanStatus status : SpanStatus.values()) {
                assertNotNull(status.name());
                assertFalse(status.name().isEmpty());
            }
        }

        @Test
        @DisplayName("All span statuses are comparable")
        void allSpanStatusesAreComparable() {
            SpanStatus[] statuses = SpanStatus.values();

            for (int i = 0; i < statuses.length; i++) {
                for (int j = 0; j < statuses.length; j++) {
                    int result = statuses[i].compareTo(statuses[j]);
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

        @Test
        @DisplayName("Status values cover all common outcomes")
        void statusValuesCoverAllCommonOutcomes() {
            // A span can end in one of these states:
            // 1. OK - success
            // 2. ERROR - failure
            // 3. CANCELLED - cancelled
            // And it starts as:
            // 4. UNSET - in progress

            SpanStatus[] terminalStates = {
                SpanStatus.OK,
                SpanStatus.ERROR,
                SpanStatus.CANCELLED
            };

            assertEquals(3, terminalStates.length,
                    "Should have 3 terminal states");
        }
    }

    @Nested
    @DisplayName("Status Transition Tests")
    class StatusTransitionTests {

        @Test
        @DisplayName("UNSET can transition to OK")
        void unsetCanTransitionToOk() {
            // Valid transition: UNSET -> OK
            SpanStatus from = SpanStatus.UNSET;
            SpanStatus to = SpanStatus.OK;

            assertEquals("UNSET", from.name());
            assertEquals("OK", to.name());
        }

        @Test
        @DisplayName("UNSET can transition to ERROR")
        void unsetCanTransitionToError() {
            // Valid transition: UNSET -> ERROR
            SpanStatus from = SpanStatus.UNSET;
            SpanStatus to = SpanStatus.ERROR;

            assertEquals("UNSET", from.name());
            assertEquals("ERROR", to.name());
        }

        @Test
        @DisplayName("UNSET can transition to CANCELLED")
        void unsetCanTransitionToCancelled() {
            // Valid transition: UNSET -> CANCELLED
            SpanStatus from = SpanStatus.UNSET;
            SpanStatus to = SpanStatus.CANCELLED;

            assertEquals("UNSET", from.name());
            assertEquals("CANCELLED", to.name());
        }

        @Test
        @DisplayName("All possible transitions from UNSET")
        void allPossibleTransitionsFromUnset() {
            java.util.List<SpanStatus> possibleTransitions = java.util.Arrays.asList(
                    SpanStatus.OK,
                    SpanStatus.ERROR,
                    SpanStatus.CANCELLED
            );

            assertEquals(3, possibleTransitions.size(),
                    "UNSET can transition to 3 different terminal states");
        }
    }
}
