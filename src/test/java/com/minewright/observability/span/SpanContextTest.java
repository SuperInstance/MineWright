package com.minewright.observability.span;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link SpanContext}.
 *
 * <p>Tests cover span context functionality including:</p>
 * <ul>
 *   <li>Context construction and validation</li>
 *   <li>ID accessors (trace ID, span ID, parent ID)</li>
 *   <li>Trace state handling</li>
 *   <li>Validity checking</li>
 *   <li>Equality and hashCode</li>
 *   <li>String representation</li>
 *   <li>Edge cases and null handling</li>
 * </ul>
 *
 * @see SpanContext
 */
@DisplayName("Span Context Tests")
class SpanContextTest {

    private String traceId;
    private String spanId;
    private String parentId;
    private String traceState;

    @BeforeEach
    void setUp() {
        traceId = "0af7651916cd43dd8448eb211c80319c";
        spanId = "b7ad6b7169203331";
        parentId = "b7ad6b7169203330";
        traceState = "vendor=key1,value1";
    }

    @Nested
    @DisplayName("Constructor and Validation Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor creates valid context with all parameters")
        void constructorCreatesValidContextWithAllParameters() {
            SpanContext context = new SpanContext(traceId, spanId, parentId, traceState);

            assertEquals(traceId, context.getTraceId());
            assertEquals(spanId, context.getSpanId());
            assertEquals(parentId, context.getParentId());
            assertEquals(traceState, context.getTraceState());
        }

        @Test
        @DisplayName("Constructor creates valid context without trace state")
        void constructorCreatesValidContextWithoutTraceState() {
            SpanContext context = new SpanContext(traceId, spanId, parentId);

            assertEquals(traceId, context.getTraceId());
            assertEquals(spanId, context.getSpanId());
            assertEquals(parentId, context.getParentId());
            assertNull(context.getTraceState());
        }

        @Test
        @DisplayName("Constructor throws on null trace ID")
        void constructorThrowsOnNullTraceId() {
            assertThrows(NullPointerException.class,
                    () -> new SpanContext(null, spanId, parentId),
                    "Should throw NullPointerException for null trace ID");
        }

        @Test
        @DisplayName("Constructor throws on null span ID")
        void constructorThrowsOnNullSpanId() {
            assertThrows(NullPointerException.class,
                    () -> new SpanContext(traceId, null, parentId),
                    "Should throw NullPointerException for null span ID");
        }

        @Test
        @DisplayName("Constructor allows null parent ID")
        void constructorAllowsNullParentId() {
            assertDoesNotThrow(() -> new SpanContext(traceId, spanId, null),
                    "Should allow null parent ID for root spans");
        }

        @Test
        @DisplayName("Constructor allows null trace state")
        void constructorAllowsNullTraceState() {
            assertDoesNotThrow(() -> new SpanContext(traceId, spanId, parentId, null),
                    "Should allow null trace state");
        }

        @Test
        @DisplayName("Constructor allows empty trace ID")
        void constructorAllowsEmptyTraceId() {
            SpanContext context = new SpanContext("", spanId, parentId);

            assertEquals("", context.getTraceId());
            assertFalse(context.isValid());
        }

        @Test
        @DisplayName("Constructor allows empty span ID")
        void constructorAllowsEmptySpanId() {
            SpanContext context = new SpanContext(traceId, "", parentId);

            assertEquals("", context.getSpanId());
            assertFalse(context.isValid());
        }
    }

    @Nested
    @DisplayName("ID Accessor Tests")
    class AccessorTests {

        @Test
        @DisplayName("Get trace ID returns correct value")
        void getTraceIdReturnsCorrectValue() {
            SpanContext context = new SpanContext(traceId, spanId, parentId);

            assertEquals(traceId, context.getTraceId(),
                    "Trace ID should match the value passed to constructor");
        }

        @Test
        @DisplayName("Get span ID returns correct value")
        void getSpanIdReturnsCorrectValue() {
            SpanContext context = new SpanContext(traceId, spanId, parentId);

            assertEquals(spanId, context.getSpanId(),
                    "Span ID should match the value passed to constructor");
        }

        @Test
        @DisplayName("Get parent ID returns correct value")
        void getParentIdReturnsCorrectValue() {
            SpanContext context = new SpanContext(traceId, spanId, parentId);

            assertEquals(parentId, context.getParentId(),
                    "Parent ID should match the value passed to constructor");
        }

        @Test
        @DisplayName("Get parent ID returns null for root span")
        void getParentIdReturnsNullForRootSpan() {
            SpanContext context = new SpanContext(traceId, spanId, null);

            assertNull(context.getParentId(),
                    "Parent ID should be null for root spans");
        }

        @Test
        @DisplayName("Get trace state returns correct value")
        void getTraceStateReturnsCorrectValue() {
            SpanContext context = new SpanContext(traceId, spanId, parentId, traceState);

            assertEquals(traceState, context.getTraceState(),
                    "Trace state should match the value passed to constructor");
        }

        @Test
        @DisplayName("Get trace state returns null when not set")
        void getTraceStateReturnsNullWhenNotSet() {
            SpanContext context = new SpanContext(traceId, spanId, parentId);

            assertNull(context.getTraceState(),
                    "Trace state should be null when not provided");
        }
    }

    @Nested
    @DisplayName("Validity Tests")
    class ValidityTests {

        @Test
        @DisplayName("Context with non-empty IDs is valid")
        void contextWithNonEmptyIdsIsValid() {
            SpanContext context = new SpanContext(traceId, spanId, parentId);

            assertTrue(context.isValid(),
                    "Context with non-empty trace and span IDs should be valid");
        }

        @Test
        @DisplayName("Context with empty trace ID is invalid")
        void contextWithEmptyTraceIdIsInvalid() {
            SpanContext context = new SpanContext("", spanId, parentId);

            assertFalse(context.isValid(),
                    "Context with empty trace ID should be invalid");
        }

        @Test
        @DisplayName("Context with empty span ID is invalid")
        void contextWithEmptySpanIdIsInvalid() {
            SpanContext context = new SpanContext(traceId, "", parentId);

            assertFalse(context.isValid(),
                    "Context with empty span ID should be invalid");
        }

        @Test
        @DisplayName("Context with null trace ID is invalid")
        void contextWithNullTraceIdIsInvalid() {
            // Note: This would throw NullPointerException in constructor
            // so we can't test this case directly
            // Instead, we verify that the constructor enforces non-null
            assertThrows(NullPointerException.class,
                    () -> new SpanContext(null, spanId, parentId));
        }

        @Test
        @DisplayName("Context with both empty IDs is invalid")
        void contextWithBothEmptyIdsIsInvalid() {
            SpanContext context = new SpanContext("", "", null);

            assertFalse(context.isValid(),
                    "Context with both empty IDs should be invalid");
        }

        @Test
        @DisplayName("Root span context is valid")
        void rootSpanContextIsValid() {
            SpanContext context = new SpanContext(traceId, spanId, null);

            assertTrue(context.isValid(),
                    "Root span context (null parent ID) should be valid");
        }

        @Test
        @DisplayName("Context with whitespace IDs is invalid")
        void contextWithWhitespaceIdsIsInvalid() {
            SpanContext context = new SpanContext("   ", "   ", null);

            assertFalse(context.isValid(),
                    "Context with whitespace IDs should be invalid");
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualityTests {

        @Test
        @DisplayName("Contexts with same trace and span IDs are equal")
        void contextsWithSameTraceAndSpanIdsAreEqual() {
            SpanContext context1 = new SpanContext(traceId, spanId, "parent1");
            SpanContext context2 = new SpanContext(traceId, spanId, "parent2");

            assertEquals(context1, context2,
                    "Contexts with same trace and span IDs should be equal");
            assertEquals(context1.hashCode(), context2.hashCode(),
                    "Equal contexts should have same hashCode");
        }

        @Test
        @DisplayName("Contexts with different trace IDs are not equal")
        void contextsWithDifferentTraceIdsAreNotEqual() {
            SpanContext context1 = new SpanContext(traceId, spanId, parentId);
            SpanContext context2 = new SpanContext("differenttraceid", spanId, parentId);

            assertNotEquals(context1, context2,
                    "Contexts with different trace IDs should not be equal");
        }

        @Test
        @DisplayName("Contexts with different span IDs are not equal")
        void contextsWithDifferentSpanIdsAreNotEqual() {
            SpanContext context1 = new SpanContext(traceId, spanId, parentId);
            SpanContext context2 = new SpanContext(traceId, "differentspanid", parentId);

            assertNotEquals(context1, context2,
                    "Contexts with different span IDs should not be equal");
        }

        @Test
        @DisplayName("Parent ID does not affect equality")
        void parentIdDoesNotAffectEquality() {
            SpanContext context1 = new SpanContext(traceId, spanId, "parent1");
            SpanContext context2 = new SpanContext(traceId, spanId, "parent2");

            assertEquals(context1, context2,
                    "Parent ID should not affect equality");
        }

        @Test
        @DisplayName("Trace state does not affect equality")
        void traceStateDoesNotAffectEquality() {
            SpanContext context1 = new SpanContext(traceId, spanId, parentId, "state1");
            SpanContext context2 = new SpanContext(traceId, spanId, parentId, "state2");

            assertEquals(context1, context2,
                    "Trace state should not affect equality");
        }

        @Test
        @DisplayName("Context equals same instance")
        void contextEqualsSameInstance() {
            SpanContext context = new SpanContext(traceId, spanId, parentId);

            assertEquals(context, context,
                    "Context should equal itself");
        }

        @Test
        @DisplayName("Context not equal to null")
        void contextNotEqualToNull() {
            SpanContext context = new SpanContext(traceId, spanId, parentId);

            assertNotEquals(context, null,
                    "Context should not equal null");
        }

        @Test
        @DisplayName("Context not equal to different type")
        void contextNotEqualToDifferentType() {
            SpanContext context = new SpanContext(traceId, spanId, parentId);

            assertNotEquals(context, "not a context",
                    "Context should not equal a string");
        }

        @Test
        @DisplayName("HashCode is consistent")
        void hashCodeIsConsistent() {
            SpanContext context = new SpanContext(traceId, spanId, parentId);

            int hash1 = context.hashCode();
            int hash2 = context.hashCode();

            assertEquals(hash1, hash2,
                    "HashCode should be consistent across multiple calls");
        }

        @Test
        @DisplayName("Equal contexts have same hash code")
        void equalContextsHaveSameHashCode() {
            SpanContext context1 = new SpanContext(traceId, spanId, "parent1");
            SpanContext context2 = new SpanContext(traceId, spanId, "parent2");

            assertEquals(context1.hashCode(), context2.hashCode(),
                    "Equal contexts should have same hashCode");
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("ToString contains trace ID")
        void toStringContainsTraceId() {
            SpanContext context = new SpanContext(traceId, spanId, parentId);

            String str = context.toString();

            assertTrue(str.contains("SpanContext"),
                    "toString should contain class name");
            assertTrue(str.contains(traceId),
                    "toString should contain trace ID");
        }

        @Test
        @DisplayName("ToString contains span ID")
        void toStringContainsSpanId() {
            SpanContext context = new SpanContext(traceId, spanId, parentId);

            String str = context.toString();

            assertTrue(str.contains(spanId),
                    "toString should contain span ID");
        }

        @Test
        @DisplayName("ToString contains parent ID when present")
        void toStringContainsParentIdWhenPresent() {
            SpanContext context = new SpanContext(traceId, spanId, parentId);

            String str = context.toString();

            assertTrue(str.contains(parentId),
                    "toString should contain parent ID when present");
        }

        @Test
        @DisplayName("ToString handles null parent ID")
        void toStringHandlesNullParentId() {
            SpanContext context = new SpanContext(traceId, spanId, null);

            String str = context.toString();

            assertTrue(str.contains("SpanContext"),
                    "toString should handle null parent ID gracefully");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Special Scenarios")
    class EdgeCaseTests {

        @Test
        @DisplayName("Context with very long IDs")
        void contextWithVeryLongIds() {
            String longTraceId = "a".repeat(100);
            String longSpanId = "b".repeat(100);

            SpanContext context = new SpanContext(longTraceId, longSpanId, parentId);

            assertEquals(longTraceId, context.getTraceId());
            assertEquals(longSpanId, context.getSpanId());
            assertTrue(context.isValid());
        }

        @Test
        @DisplayName("Context with special characters in IDs")
        void contextWithSpecialCharactersInIds() {
            String specialTraceId = "trace-id_123.test";
            String specialSpanId = "span-id_456.test";

            SpanContext context = new SpanContext(specialTraceId, specialSpanId, parentId);

            assertEquals(specialTraceId, context.getTraceId());
            assertEquals(specialSpanId, context.getSpanId());
            assertTrue(context.isValid());
        }

        @Test
        @DisplayName("Context with unicode characters in IDs")
        void contextWithUnicodeCharactersInIds() {
            String unicodeTraceId = "trace\u1234\u5678";
            String unicodeSpanId = "span\uabcd\uef01";

            SpanContext context = new SpanContext(unicodeTraceId, unicodeSpanId, parentId);

            assertEquals(unicodeTraceId, context.getTraceId());
            assertEquals(unicodeSpanId, context.getSpanId());
            assertTrue(context.isValid());
        }

        @Test
        @DisplayName("Context with empty trace state")
        void contextWithEmptyTraceState() {
            SpanContext context = new SpanContext(traceId, spanId, parentId, "");

            assertEquals("", context.getTraceState());
            assertTrue(context.isValid());
        }

        @Test
        @DisplayName("Multiple contexts can be compared")
        void multipleContextsCanBeCompared() {
            SpanContext context1 = new SpanContext("trace1", "span1", null);
            SpanContext context2 = new SpanContext("trace2", "span2", null);
            SpanContext context3 = new SpanContext("trace1", "span1", null);

            assertEquals(context1, context3);
            assertNotEquals(context1, context2);
            assertNotEquals(context2, context3);
        }

        @Test
        @DisplayName("Context can be used in collections")
        void contextCanBeUsedInCollections() {
            SpanContext context1 = new SpanContext(traceId, spanId, "parent1");
            SpanContext context2 = new SpanContext(traceId, spanId, "parent2");
            SpanContext context3 = new SpanContext("different", spanId, parentId);

            java.util.Set<SpanContext> set = new java.util.HashSet<>();
            set.add(context1);
            set.add(context2);
            set.add(context3);

            assertEquals(2, set.size(),
                    "Set should contain 2 unique contexts (context1 and context2 are equal)");
        }
    }
}
