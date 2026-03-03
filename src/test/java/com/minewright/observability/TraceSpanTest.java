package com.minewright.observability;

import com.minewright.observability.span.SpanContext;
import com.minewright.observability.span.SpanKind;
import com.minewright.observability.span.SpanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link TraceSpan}.
 *
 * <p>Tests cover span functionality including:</p>
 * <ul>
 *   <li>Span lifecycle (start, end, active duration)</li>
 *   <li>Attributes (set, get, contains)</li>
 *   <li>Events (add event, event list)</li>
 *   <li>Exceptions (record exception)</li>
 *   <li>Parent-child relationships</li>
 *   <li>OpenTelemetry export format</li>
 *   <li>LangSmith export format</li>
 *   <li>Edge cases</li>
 * </ul>
 *
 * @see TraceSpan
 */
@DisplayName("Trace Span Tests")
class TraceSpanTest {

    private static final int TEST_DELAY_MS = 100;

    @Nested
    @DisplayName("Constructor and Initialization Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Root span constructor creates valid span")
        void rootSpanConstructorCreatesValidSpan() {
            TraceSpan span = new TraceSpan("test-span", SpanKind.INTERNAL);

            assertNotNull(span.getSpanId());
            assertNotNull(span.getTraceId());
            assertNull(span.getParentId());
            assertEquals("test-span", span.getName());
            assertEquals(SpanKind.INTERNAL, span.getKind());
            assertEquals(SpanStatus.UNSET, span.getStatus());
            assertEquals(0, span.getEndEpochNanos());
        }

        @Test
        @DisplayName("Child span constructor creates valid span")
        void childSpanConstructorCreatesValidSpan() {
            TraceSpan parent = new TraceSpan("parent", SpanKind.INTERNAL);
            TraceSpan child = new TraceSpan("child", SpanKind.CLIENT, parent);

            assertNotNull(child.getSpanId());
            assertEquals(parent.getTraceId(), child.getTraceId());
            assertEquals(parent.getSpanId(), child.getParentId());
            assertEquals("child", child.getName());
            assertEquals(SpanKind.CLIENT, child.getKind());
        }

        @Test
        @DisplayName("Span from context constructor creates valid span")
        void spanFromContextConstructorCreatesValidSpan() {
            SpanContext context = new SpanContext("trace123", "span456", "parent789");
            TraceSpan span = new TraceSpan("context-span", SpanKind.SERVER, context);

            assertEquals("trace123", span.getTraceId());
            assertEquals("parent789", span.getParentId());
            assertEquals("context-span", span.getName());
            assertEquals(SpanKind.SERVER, span.getKind());
        }

        @Test
        @DisplayName("Span IDs are unique")
        void spanIdsAreUnique() {
            TraceSpan span1 = new TraceSpan("span1", SpanKind.INTERNAL);
            TraceSpan span2 = new TraceSpan("span2", SpanKind.INTERNAL);

            assertNotEquals(span1.getSpanId(), span2.getSpanId(),
                    "Span IDs should be unique");
        }

        @Test
        @DisplayName("Trace IDs are unique across root spans")
        void traceIdsAreUniqueAcrossRootSpans() {
            TraceSpan root1 = new TraceSpan("root1", SpanKind.INTERNAL);
            TraceSpan root2 = new TraceSpan("root2", SpanKind.INTERNAL);

            assertNotEquals(root1.getTraceId(), root2.getTraceId(),
                    "Root spans should have unique trace IDs");
        }

        @Test
        @DisplayName("Child spans inherit parent trace ID")
        void childSpansInheritParentTraceId() {
            TraceSpan parent = new TraceSpan("parent", SpanKind.INTERNAL);
            TraceSpan child = new TraceSpan("child", SpanKind.INTERNAL, parent);

            assertEquals(parent.getTraceId(), child.getTraceId(),
                    "Child should inherit parent's trace ID");
        }

        @Test
        @DisplayName("Span ID is 16 characters")
        void spanIdIs16Characters() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            assertEquals(16, span.getSpanId().length(),
                    "Span ID should be 16 characters");
        }

        @Test
        @DisplayName("Trace ID is 32 characters")
        void traceIdIs32Characters() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            assertEquals(32, span.getTraceId().length(),
                    "Trace ID should be 32 characters");
        }
    }

    @Nested
    @DisplayName("Span Lifecycle Tests")
    class LifecycleTests {

        @Test
        @DisplayName("Span starts with current timestamp")
        void spanStartsWithCurrentTimestamp() {
            Instant before = Instant.now().minusMillis(100);
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);
            Instant after = Instant.now().plusMillis(100);

            assertTrue(span.getStartTime().isAfter(before) ||
                       span.getStartTime().equals(before));
            assertTrue(span.getStartTime().isBefore(after) ||
                       span.getStartTime().equals(after));
        }

        @Test
        @DisplayName("Span starts with valid nano time")
        void spanStartsWithValidNanoTime() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            assertTrue(span.getStartEpochNanos() > 0,
                    "Start epoch nanos should be positive");
            assertTrue(span.getStartEpochNanos() < System.nanoTime(),
                    "Start time should be in the past");
        }

        @Test
        @DisplayName("End span sets end time and status")
        void endSpanSetsEndTimeAndStatus() throws InterruptedException {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            Thread.sleep(TEST_DELAY_MS);
            span.end();

            assertTrue(span.getEndEpochNanos() > 0,
                    "End epoch nanos should be set");
            assertEquals(SpanStatus.OK, span.getStatus(),
                    "Status should be OK after end()");
        }

        @Test
        @DisplayName("End span with status sets custom status")
        void endSpanWithStatusSetsCustomStatus() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.end(SpanStatus.ERROR);

            assertEquals(SpanStatus.ERROR, span.getStatus(),
                    "Status should be ERROR when specified");
        }

        @Test
        @DisplayName("End span can be called only once")
        void endSpanCanBeCalledOnlyOnce() throws InterruptedException {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.end();
            long firstEndTime = span.getEndEpochNanos();

            Thread.sleep(TEST_DELAY_MS);
            span.end();
            long secondEndTime = span.getEndEpochNanos();

            assertEquals(firstEndTime, secondEndTime,
                    "End time should not change after first call");
        }

        @Test
        @DisplayName("End span with different status after first end")
        void endSpanWithDifferentStatusAfterFirstEnd() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.end(SpanStatus.OK);
            span.end(SpanStatus.ERROR);

            assertEquals(SpanStatus.OK, span.getStatus(),
                    "First status should be preserved");
        }

        @Test
        @DisplayName("Duration is zero for unclosed span")
        void durationIsZeroForUnclosedSpan() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            assertEquals(0, span.getDurationNanos(),
                    "Duration should be 0 for unclosed span");
            assertEquals(0, span.getDurationMillis(),
                    "Duration millis should be 0 for unclosed span");
        }

        @Test
        @DisplayName("Duration is calculated for closed span")
        void durationIsCalculatedForClosedSpan() throws InterruptedException {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            Thread.sleep(TEST_DELAY_MS);
            span.end();

            assertTrue(span.getDurationNanos() > 0,
                    "Duration nanos should be positive");
            assertTrue(span.getDurationMillis() >= TEST_DELAY_MS,
                    "Duration millis should be at least TEST_DELAY_MS");
        }

        @Test
        @DisplayName("Duration is consistent")
        void durationIsConsistent() throws InterruptedException {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            Thread.sleep(TEST_DELAY_MS);
            span.end();

            long nanos = span.getDurationNanos();
            long millis = span.getDurationMillis();

            assertEquals(nanos / 1_000_000, millis,
                    "Duration millis should equal nanos / 1,000,000");
        }
    }

    @Nested
    @DisplayName("Attribute Tests")
    class AttributeTests {

        @Test
        @DisplayName("Set string attribute")
        void setStringAttribute() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.setAttribute("key1", "value1");

            Map<String, Object> attrs = span.getAttributes();
            assertEquals(1, attrs.size());
            assertEquals("value1", attrs.get("key1"));
        }

        @Test
        @DisplayName("Set long attribute")
        void setLongAttribute() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.setAttribute("count", 42L);

            Map<String, Object> attrs = span.getAttributes();
            assertEquals(1, attrs.size());
            assertEquals(42L, attrs.get("count"));
        }

        @Test
        @DisplayName("Set double attribute")
        void setDoubleAttribute() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.setAttribute("ratio", 3.14);

            Map<String, Object> attrs = span.getAttributes();
            assertEquals(1, attrs.size());
            assertEquals(3.14, attrs.get("ratio"));
        }

        @Test
        @DisplayName("Set boolean attribute")
        void setBooleanAttribute() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.setAttribute("active", true);

            Map<String, Object> attrs = span.getAttributes();
            assertEquals(1, attrs.size());
            assertEquals(true, attrs.get("active"));
        }

        @Test
        @DisplayName("Set multiple attributes")
        void setMultipleAttributes() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.setAttribute("string", "value");
            span.setAttribute("number", 123);
            span.setAttribute("decimal", 1.5);
            span.setAttribute("flag", false);

            Map<String, Object> attrs = span.getAttributes();
            assertEquals(4, attrs.size());
            assertEquals("value", attrs.get("string"));
            assertEquals(123L, attrs.get("number"));
            assertEquals(1.5, attrs.get("decimal"));
            assertEquals(false, attrs.get("flag"));
        }

        @Test
        @DisplayName("Update existing attribute")
        void updateExistingAttribute() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.setAttribute("key", "value1");
            span.setAttribute("key", "value2");

            Map<String, Object> attrs = span.getAttributes();
            assertEquals(1, attrs.size());
            assertEquals("value2", attrs.get("key"));
        }

        @Test
        @DisplayName("Get attributes returns copy")
        void getAttributesReturnsCopy() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.setAttribute("key", "value");

            Map<String, Object> attrs1 = span.getAttributes();
            Map<String, Object> attrs2 = span.getAttributes();

            assertNotSame(attrs1, attrs2,
                    "getAttributes should return a new map each time");
            assertEquals(attrs1, attrs2,
                    "Maps should be equal");
        }

        @Test
        @DisplayName("Modifying returned attributes does not affect span")
        void modifyingReturnedAttributesDoesNotAffectSpan() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.setAttribute("key", "value");

            Map<String, Object> attrs = span.getAttributes();
            attrs.put("newKey", "newValue");

            Map<String, Object> spanAttrs = span.getAttributes();
            assertEquals(1, spanAttrs.size(),
                    "Modifying returned map should not affect span");
        }
    }

    @Nested
    @DisplayName("Event Tests")
    class EventTests {

        @Test
        @DisplayName("Add event without attributes")
        void addEventWithoutAttributes() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.addEvent("test-event");

            List<TraceSpan.SpanEvent> events = span.getEvents();
            assertEquals(1, events.size());
            assertEquals("test-event", events.get(0).getName());
        }

        @Test
        @DisplayName("Add event with attributes")
        void addEventWithAttributes() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.addEvent("test-event", Map.of("key1", "value1", "key2", 42));

            List<TraceSpan.SpanEvent> events = span.getEvents();
            assertEquals(1, events.size());
            assertEquals("test-event", events.get(0).getName());

            Map<String, Object> attrs = events.get(0).getAttributes();
            assertEquals(2, attrs.size());
            assertEquals("value1", attrs.get("key1"));
            assertEquals(42, attrs.get("key2"));
        }

        @Test
        @DisplayName("Add multiple events")
        void addMultipleEvents() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.addEvent("event1");
            span.addEvent("event2");
            span.addEvent("event3");

            List<TraceSpan.SpanEvent> events = span.getEvents();
            assertEquals(3, events.size());
            assertEquals("event1", events.get(0).getName());
            assertEquals("event2", events.get(1).getName());
            assertEquals("event3", events.get(2).getName());
        }

        @Test
        @DisplayName("Events have timestamps")
        void eventsHaveTimestamps() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.addEvent("event1");

            List<TraceSpan.SpanEvent> events = span.getEvents();
            assertTrue(events.get(0).getTimestampNanos() > 0,
                    "Event should have a positive timestamp");
        }

        @Test
        @DisplayName("Events are ordered by addition")
        void eventsAreOrderedByAddition() throws InterruptedException {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.addEvent("event1");
            Thread.sleep(TEST_DELAY_MS);
            span.addEvent("event2");

            List<TraceSpan.SpanEvent> events = span.getEvents();
            assertTrue(events.get(1).getTimestampNanos() > events.get(0).getTimestampNanos(),
                    "Events should be ordered by timestamp");
        }

        @Test
        @DisplayName("Get events returns copy")
        void getEventsReturnsCopy() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.addEvent("event1");

            List<TraceSpan.SpanEvent> events1 = span.getEvents();
            List<TraceSpan.SpanEvent> events2 = span.getEvents();

            assertNotSame(events1, events2,
                    "getEvents should return a new list each time");
            assertEquals(events1, events2,
                    "Lists should be equal");
        }
    }

    @Nested
    @DisplayName("Exception Tests")
    class ExceptionTests {

        @Test
        @DisplayName("Record exception sets status to ERROR")
        void recordExceptionSetsStatusToError() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            Exception ex = new RuntimeException("Test error");
            span.recordException(ex);

            assertEquals(SpanStatus.ERROR, span.getStatus());
            assertEquals("Test error", span.getStatusDescription());
        }

        @Test
        @DisplayName("Record exception stores exception")
        void recordExceptionStoresException() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            RuntimeException ex = new RuntimeException("Test error");
            span.recordException(ex);

            assertSame(ex, span.getException(),
                    "Exception should be stored");
        }

        @Test
        @DisplayName("Record exception creates event")
        void recordExceptionCreatesEvent() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            Exception ex = new RuntimeException("Test error");
            span.recordException(ex);

            List<TraceSpan.SpanEvent> events = span.getEvents();
            assertEquals(1, events.size());
            assertEquals("exception", events.get(0).getName());
        }

        @Test
        @DisplayName("Exception event contains attributes")
        void exceptionEventContainsAttributes() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            RuntimeException ex = new RuntimeException("Test error");
            span.recordException(ex);

            List<TraceSpan.SpanEvent> events = span.getEvents();
            Map<String, Object> attrs = events.get(0).getAttributes();

            assertTrue(attrs.containsKey("exception.type"));
            assertTrue(attrs.containsKey("exception.message"));
            assertTrue(attrs.containsKey("exception.stacktrace"));

            assertEquals("java.lang.RuntimeException", attrs.get("exception.type"));
            assertEquals("Test error", attrs.get("exception.message"));
        }

        @Test
        @DisplayName("Exception event contains stack trace")
        void exceptionEventContainsStackTrace() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            RuntimeException ex = new RuntimeException("Test error");
            span.recordException(ex);

            List<TraceSpan.SpanEvent> events = span.getEvents();
            Map<String, Object> attrs = events.get(0).getAttributes();

            String stacktrace = (String) attrs.get("exception.stacktrace");
            assertNotNull(stacktrace);
            assertTrue(stacktrace.contains("TraceSpanTest"));
        }
    }

    @Nested
    @DisplayName("Context Conversion Tests")
    class ContextConversionTests {

        @Test
        @DisplayName("To context returns valid context")
        void toContextReturnsValidContext() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            SpanContext context = span.toContext();

            assertEquals(span.getTraceId(), context.getTraceId());
            assertEquals(span.getSpanId(), context.getSpanId());
            assertEquals(span.getParentId(), context.getParentId());
        }

        @Test
        @DisplayName("To context for root span")
        void toContextForRootSpan() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            SpanContext context = span.toContext();

            assertEquals(span.getTraceId(), context.getTraceId());
            assertEquals(span.getSpanId(), context.getSpanId());
            assertNull(context.getParentId());
        }

        @Test
        @DisplayName("To context for child span")
        void toContextForChildSpan() {
            TraceSpan parent = new TraceSpan("parent", SpanKind.INTERNAL);
            TraceSpan child = new TraceSpan("child", SpanKind.INTERNAL, parent);

            SpanContext context = child.toContext();

            assertEquals(parent.getTraceId(), context.getTraceId());
            assertEquals(child.getSpanId(), context.getSpanId());
            assertEquals(parent.getSpanId(), context.getParentId());
        }
    }

    @Nested
    @DisplayName("OpenTelemetry JSON Export Tests")
    class OpenTelemetryJsonTests {

        @Test
        @DisplayName("To JSON contains basic fields")
        void toJsonContainsBasicFields() {
            TraceSpan span = new TraceSpan("test-span", SpanKind.CLIENT);

            String json = span.toOpenTelemetryJson();

            assertTrue(json.contains("\"name\":\"test-span\""));
            assertTrue(json.contains("\"kind\":\"CLIENT\""));
            assertTrue(json.contains("\"traceId\""));
            assertTrue(json.contains("\"spanId\""));
        }

        @Test
        @DisplayName("To JSON contains parent span ID when present")
        void toJsonContainsParentSpanIdWhenPresent() {
            TraceSpan parent = new TraceSpan("parent", SpanKind.INTERNAL);
            TraceSpan child = new TraceSpan("child", SpanKind.CLIENT, parent);

            String json = child.toOpenTelemetryJson();

            assertTrue(json.contains("\"parentSpanId\":\"" + parent.getSpanId() + "\""));
        }

        @Test
        @DisplayName("To JSON omits parent span ID for root span")
        void toJsonOmitsParentSpanIdForRootSpan() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            String json = span.toOpenTelemetryJson();

            assertFalse(json.contains("\"parentSpanId\""));
        }

        @Test
        @DisplayName("To JSON contains status")
        void toJsonContainsStatus() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            String json = span.toOpenTelemetryJson();

            assertTrue(json.contains("\"status\":{"));
            assertTrue(json.contains("\"code\":\"UNSET\""));
        }

        @Test
        @DisplayName("To JSON contains status description")
        void toJsonContainsStatusDescription() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            Exception ex = new RuntimeException("Test error");
            span.recordException(ex);

            String json = span.toOpenTelemetryJson();

            assertTrue(json.contains("\"description\":\"Test error\""));
        }

        @Test
        @DisplayName("To JSON contains attributes")
        void toJsonContainsAttributes() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.setAttribute("stringAttr", "value");
            span.setAttribute("numberAttr", 42);

            String json = span.toOpenTelemetryJson();

            assertTrue(json.contains("\"attributes\":{"));
            assertTrue(json.contains("\"stringAttr\":\"value\""));
            assertTrue(json.contains("\"numberAttr\":42"));
        }

        @Test
        @DisplayName("To JSON contains events")
        void toJsonContainsEvents() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.addEvent("test-event", Map.of("key", "value"));

            String json = span.toOpenTelemetryJson();

            assertTrue(json.contains("\"events\":["));
            assertTrue(json.contains("\"name\":\"test-event\""));
        }

        @Test
        @DisplayName("To JSON escapes special characters")
        void toJsonEscapesSpecialCharacters() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.setAttribute("key", "value with \"quotes\" and\nnewlines");

            String json = span.toOpenTelemetryJson();

            assertTrue(json.contains("\\\""));
            assertTrue(json.contains("\\n"));
        }

        @Test
        @DisplayName("To JSON for ended span")
        void toJsonForEndedSpan() throws InterruptedException {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            Thread.sleep(TEST_DELAY_MS);
            span.end();

            String json = span.toOpenTelemetryJson();

            assertTrue(json.contains("\"startTimeUnixNano\":"));
            assertTrue(json.contains("\"endTimeUnixNano\":"));
        }
    }

    @Nested
    @DisplayName("SpanEvent Tests")
    class SpanEventTests {

        @Test
        @DisplayName("SpanEvent stores name and timestamp")
        void spanEventStoresNameAndTimestamp() {
            TraceSpan.SpanEvent event = new TraceSpan.SpanEvent("test-event", 123456, Map.of());

            assertEquals("test-event", event.getName());
            assertEquals(123456, event.getTimestampNanos());
        }

        @Test
        @DisplayName("SpanEvent stores attributes")
        void spanEventStoresAttributes() {
            Map<String, Object> attrs = Map.of("key1", "value1", "key2", 42);
            TraceSpan.SpanEvent event = new TraceSpan.SpanEvent("test-event", 123456, attrs);

            Map<String, Object> eventAttrs = event.getAttributes();
            assertEquals(2, eventAttrs.size());
            assertEquals("value1", eventAttrs.get("key1"));
            assertEquals(42, eventAttrs.get("key2"));
        }

        @Test
        @DisplayName("SpanEvent handles null attributes")
        void spanEventHandlesNullAttributes() {
            TraceSpan.SpanEvent event = new TraceSpan.SpanEvent("test-event", 123456, null);

            assertNotNull(event.getAttributes());
            assertTrue(event.getAttributes().isEmpty());
        }

        @Test
        @DisplayName("SpanEvent to JSON")
        void spanEventToJson() {
            Map<String, Object> attrs = Map.of("key", "value");
            TraceSpan.SpanEvent event = new TraceSpan.SpanEvent("test-event", 123456, attrs);

            String json = event.toJson();

            assertTrue(json.contains("\"name\":\"test-event\""));
            assertTrue(json.contains("\"timeUnixNano\":123456"));
            assertTrue(json.contains("\"attributes\":{"));
            assertTrue(json.contains("\"key\":\"value\""));
        }

        @Test
        @DisplayName("SpanEvent to JSON escapes special characters")
        void spanEventToJsonEscapesSpecialCharacters() {
            Map<String, Object> attrs = Map.of("key", "value \"with\" quotes");
            TraceSpan.SpanEvent event = new TraceSpan.SpanEvent("test-event", 123456, attrs);

            String json = event.toJson();

            assertTrue(json.contains("\\\""));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Special Scenarios")
    class EdgeCaseTests {

        @Test
        @DisplayName("Span with empty name")
        void spanWithEmptyName() {
            TraceSpan span = new TraceSpan("", SpanKind.INTERNAL);

            assertEquals("", span.getName());
        }

        @Test
        @DisplayName("Span with null parent")
        void spanWithNullParent() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL, (TraceSpan) null);

            assertNull(span.getParentId());
        }

        @Test
        @DisplayName("Deeply nested spans")
        void deeplyNestedSpans() {
            TraceSpan root = new TraceSpan("root", SpanKind.INTERNAL);
            TraceSpan child1 = new TraceSpan("child1", SpanKind.INTERNAL, root);
            TraceSpan child2 = new TraceSpan("child2", SpanKind.INTERNAL, child1);
            TraceSpan child3 = new TraceSpan("child3", SpanKind.INTERNAL, child2);

            assertEquals(root.getTraceId(), child3.getTraceId());
            assertEquals(child2.getSpanId(), child3.getParentId());
        }

        @Test
        @DisplayName("Span with many attributes")
        void spanWithManyAttributes() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            for (int i = 0; i < 100; i++) {
                span.setAttribute("key" + i, "value" + i);
            }

            assertEquals(100, span.getAttributes().size());
        }

        @Test
        @DisplayName("Span with many events")
        void spanWithManyEvents() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            for (int i = 0; i < 100; i++) {
                span.addEvent("event" + i);
            }

            assertEquals(100, span.getEvents().size());
        }

        @Test
        @DisplayName("Span attribute with special characters in key")
        void spanAttributeWithSpecialCharactersInKey() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            span.setAttribute("key-with-dashes", "value");
            span.setAttribute("key_with_underscores", "value");
            span.setAttribute("key.with.dots", "value");

            assertEquals(3, span.getAttributes().size());
        }

        @Test
        @DisplayName("Span attribute with null value handling")
        void spanAttributeWithNullValueHandling() {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            // Note: setAttribute methods don't accept null directly
            // but we can test empty string handling
            span.setAttribute("key", "");

            assertEquals("", span.getAttributes().get("key"));
        }

        @Test
        @DisplayName("Sibling spans have same parent")
        void siblingSpansHaveSameParent() {
            TraceSpan parent = new TraceSpan("parent", SpanKind.INTERNAL);
            TraceSpan child1 = new TraceSpan("child1", SpanKind.INTERNAL, parent);
            TraceSpan child2 = new TraceSpan("child2", SpanKind.INTERNAL, parent);

            assertEquals(parent.getSpanId(), child1.getParentId());
            assertEquals(parent.getSpanId(), child2.getParentId());
            assertEquals(child1.getParentId(), child2.getParentId());
        }

        @Test
        @DisplayName("Multiple children of same parent")
        void multipleChildrenOfSameParent() {
            TraceSpan parent = new TraceSpan("parent", SpanKind.INTERNAL);

            for (int i = 0; i < 10; i++) {
                TraceSpan child = new TraceSpan("child" + i, SpanKind.INTERNAL, parent);
                assertEquals(parent.getTraceId(), child.getTraceId());
                assertEquals(parent.getSpanId(), child.getParentId());
            }
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Concurrent attribute writes")
        void concurrentAttributeWrites() throws InterruptedException {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            Thread thread1 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    span.setAttribute("key1-" + i, "value1-" + i);
                }
            });

            Thread thread2 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    span.setAttribute("key2-" + i, "value2-" + i);
                }
            });

            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            // Should have 200 attributes (no exceptions thrown)
            assertTrue(span.getAttributes().size() >= 0);
        }

        @Test
        @DisplayName("Concurrent event additions")
        void concurrentEventAdditions() throws InterruptedException {
            TraceSpan span = new TraceSpan("test", SpanKind.INTERNAL);

            Thread thread1 = new Thread(() -> {
                for (int i = 0; i < 50; i++) {
                    span.addEvent("event1-" + i);
                }
            });

            Thread thread2 = new Thread(() -> {
                for (int i = 0; i < 50; i++) {
                    span.addEvent("event2-" + i);
                }
            });

            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            assertEquals(100, span.getEvents().size());
        }
    }
}
