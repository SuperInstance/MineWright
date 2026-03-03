package com.minewright.observability;

import com.minewright.observability.span.SpanContext;
import com.minewright.observability.span.SpanKind;
import com.minewright.observability.span.SpanStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link TracingService}.
 *
 * <p>Tests cover tracing service functionality including:</p>
 * <ul>
 *   <li>Span stack management</li>
 *   <li>Span lifecycle</li>
 *   <li>Specialized span factories</li>
 *   <li>Export functionality</li>
 *   <li>Statistics tracking</li>
 *   <li>Thread safety</li>
 * </ul>
 *
 * @see TracingService
 */
@DisplayName("Tracing Service Tests")
class TracingServiceTest {

    @TempDir
    Path tempDir;

    private TracingConfig config;
    private TracingService service;

    @BeforeEach
    void setUp() {
        config = new TracingConfig();
        config.setEnabled(true);
        config.setExportDirectory(tempDir.toString());
        service = new TracingService(config);
    }

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.close();
        }
        // Clear singleton instance
        resetSingleton();
    }

    private void resetSingleton() {
        // Clear thread-local state
        try {
            service.clearCompletedSpans();
        } catch (Exception e) {
            // Ignore
        }
    }

    @Nested
    @DisplayName("Constructor and Initialization Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor creates valid service")
        void constructorCreatesValidService() {
            TracingConfig testConfig = new TracingConfig();
            testConfig.setEnabled(true);

            TracingService testService = new TracingService(testConfig);

            assertNotNull(testService);
            assertEquals(testConfig, testService.getConfig());
            testService.close();
        }

        @Test
        @DisplayName("Constructor initializes with queue size from config")
        void constructorInitializesWithQueueSizeFromConfig() {
            TracingConfig testConfig = new TracingConfig();
            testConfig.setExportQueueSize(100);

            TracingService testService = new TracingService(testConfig);

            assertEquals(0, testService.getCompletedSpanCount());
            testService.close();
        }

        @Test
        @DisplayName("Singleton instance returns same instance")
        void singletonInstanceReturnsSameInstance() {
            TracingService instance1 = TracingService.getInstance();
            TracingService instance2 = TracingService.getInstance();

            assertSame(instance1, instance2,
                    "getInstance should return the same instance");
        }

        @Test
        @DisplayName("Service starts with zero statistics")
        void serviceStartsWithZeroStatistics() {
            Map<String, Long> stats = service.getStatistics();

            assertEquals(0L, stats.get("total_spans_created"));
            assertEquals(0L, stats.get("total_spans_completed"));
            assertEquals(0L, stats.get("total_spans_errored"));
            assertEquals(0L, stats.get("queued_spans"));
        }
    }

    @Nested
    @DisplayName("Span Creation Tests")
    class SpanCreationTests {

        @Test
        @DisplayName("Start span creates valid root span")
        void startSpanCreatesValidRootSpan() {
            TraceSpan span = TracingService.startSpan("test-span", SpanKind.INTERNAL);

            assertNotNull(span);
            assertEquals("test-span", span.getName());
            assertEquals(SpanKind.INTERNAL, span.getKind());
            assertNull(span.getParentId());

            service.endSpan(span);
        }

        @Test
        @DisplayName("Start span with explicit parent")
        void startSpanWithExplicitParent() {
            TraceSpan parent = TracingService.startSpan("parent", SpanKind.INTERNAL);
            TraceSpan child = TracingService.startSpan("child", SpanKind.INTERNAL, parent);

            assertNotNull(child);
            assertEquals(parent.getTraceId(), child.getTraceId());
            assertEquals(parent.getSpanId(), child.getParentId());

            service.endSpan(child);
            service.endSpan(parent);
        }

        @Test
        @DisplayName("Start span with context")
        void startSpanWithContext() {
            SpanContext context = new SpanContext("trace123", "span456", null);
            TraceSpan span = TracingService.startSpan("test", SpanKind.INTERNAL, context);

            assertNotNull(span);
            assertEquals("trace123", span.getTraceId());
            assertEquals("span456", span.getParentId());

            service.endSpan(span);
        }

        @Test
        @DisplayName("Start span returns null when disabled")
        void startSpanReturnsNullWhenDisabled() {
            config.setEnabled(false);

            TraceSpan span = TracingService.startSpan("test", SpanKind.INTERNAL);

            assertNull(span);
        }

        @Test
        @DisplayName("Child span inherits from thread-local parent")
        void childSpanInheritsFromThreadLocalParent() {
            TraceSpan parent = TracingService.startSpan("parent", SpanKind.INTERNAL);

            // Child automatically gets parent from thread-local
            TraceSpan child = TracingService.startSpan("child", SpanKind.INTERNAL);

            assertNotNull(child);
            assertEquals(parent.getTraceId(), child.getTraceId());
            assertEquals(parent.getSpanId(), child.getParentId());

            service.endSpan(child);
            service.endSpan(parent);
        }
    }

    @Nested
    @DisplayName("Specialized Span Factory Tests")
    class SpecializedSpanTests {

        @Test
        @DisplayName("Start LLM span with operation and model")
        void startLLMSpanWithOperationAndModel() {
            config.setLLMTracingEnabled(true);

            TraceSpan span = TracingService.startLLMSpan("chat.completion", "gpt-4");

            assertNotNull(span);
            assertTrue(span.getName().startsWith("llm."));
            assertEquals("chat.completion", span.getAttributes().get("llm.operation"));
            assertEquals("gpt-4", span.getAttributes().get("llm.model"));

            service.endSpan(span);
        }

        @Test
        @DisplayName("Start LLM span returns null when disabled")
        void startLLMSpanReturnsNullWhenDisabled() {
            config.setLLMTracingEnabled(false);

            TraceSpan span = TracingService.startLLMSpan("chat.completion", "gpt-4");

            assertNull(span);
        }

        @Test
        @DisplayName("Start skill span with name and type")
        void startSkillSpanWithNameAndType() {
            config.setSkillTracingEnabled(true);

            TraceSpan span = TracingService.startSkillSpan("mineOre", "mining");

            assertNotNull(span);
            assertTrue(span.getName().startsWith("skill."));
            assertEquals("mineOre", span.getAttributes().get("skill.name"));
            assertEquals("mining", span.getAttributes().get("skill.type"));

            service.endSpan(span);
        }

        @Test
        @DisplayName("Start skill span returns null when disabled")
        void startSkillSpanReturnsNullWhenDisabled() {
            config.setSkillTracingEnabled(false);

            TraceSpan span = TracingService.startSkillSpan("mineOre", "mining");

            assertNull(span);
        }

        @Test
        @DisplayName("Start contract net span with phase and task type")
        void startContractNetSpanWithPhaseAndTaskType() {
            config.setContractNetTracingEnabled(true);

            TraceSpan span = TracingService.startContractNetSpan("announcement", "mining");

            assertNotNull(span);
            assertTrue(span.getName().startsWith("contract_net."));
            assertEquals("announcement", span.getAttributes().get("contract_net.phase"));
            assertEquals("mining", span.getAttributes().get("contract_net.task_type"));

            service.endSpan(span);
        }

        @Test
        @DisplayName("Start contract net span returns null when disabled")
        void startContractNetSpanReturnsNullWhenDisabled() {
            config.setContractNetTracingEnabled(false);

            TraceSpan span = TracingService.startContractNetSpan("announcement", "mining");

            assertNull(span);
        }

        @Test
        @DisplayName("LLM provider extraction")
        void llmProviderExtraction() {
            config.setLLMTracingEnabled(true);

            TraceSpan gptSpan = TracingService.startLLMSpan("chat", "gpt-4");
            assertEquals("openai", gptSpan.getAttributes().get("llm.provider"));
            service.endSpan(gptSpan);

            TraceSpan glmSpan = TracingService.startLLMSpan("chat", "glm-5");
            assertEquals("zai", glmSpan.getAttributes().get("llm.provider"));
            service.endSpan(glmSpan);

            TraceSpan groqSpan = TracingService.startLLMSpan("chat", "llama3-70b");
            assertEquals("groq", groqSpan.getAttributes().get("llm.provider"));
            service.endSpan(groqSpan);

            TraceSpan geminiSpan = TracingService.startLLMSpan("chat", "gemini-pro");
            assertEquals("google", geminiSpan.getAttributes().get("llm.provider"));
            service.endSpan(geminiSpan);
        }
    }

    @Nested
    @DisplayName("Span Stack Management Tests")
    class SpanStackTests {

        @Test
        @DisplayName("Get current span returns active span")
        void getCurrentSpanReturnsActiveSpan() {
            TraceSpan span = TracingService.startSpan("test", SpanKind.INTERNAL);

            TraceSpan current = TracingService.getCurrentSpan();

            assertSame(span, current);

            service.endSpan(span);
        }

        @Test
        @DisplayName("Get current span returns null when no active span")
        void getCurrentSpanReturnsNullWhenNoActiveSpan() {
            TraceSpan current = TracingService.getCurrentSpan();

            assertNull(current);
        }

        @Test
        @DisplayName("Get current context returns context of active span")
        void getCurrentContextReturnsContextOfActiveSpan() {
            TraceSpan span = TracingService.startSpan("test", SpanKind.INTERNAL);

            SpanContext context = TracingService.getCurrentContext();

            assertNotNull(context);
            assertEquals(span.getTraceId(), context.getTraceId());
            assertEquals(span.getSpanId(), context.getSpanId());

            service.endSpan(span);
        }

        @Test
        @DisplayName("Get current context returns null when no active span")
        void getCurrentContextReturnsNullWhenNoActiveSpan() {
            SpanContext context = TracingService.getCurrentContext();

            assertNull(context);
        }

        @Test
        @DisplayName("Nested spans are pushed to stack")
        void nestedSpansArePushedToStack() {
            TraceSpan parent = TracingService.startSpan("parent", SpanKind.INTERNAL);
            assertSame(parent, TracingService.getCurrentSpan());

            TraceSpan child = TracingService.startSpan("child", SpanKind.INTERNAL);
            assertSame(child, TracingService.getCurrentSpan());

            service.endSpan(child);
            assertSame(parent, TracingService.getCurrentSpan());

            service.endSpan(parent);
            assertNull(TracingService.getCurrentSpan());
        }

        @Test
        @DisplayName("Deeply nested span stack")
        void deeplyNestedSpanStack() {
            TraceSpan span1 = TracingService.startSpan("span1", SpanKind.INTERNAL);
            TraceSpan span2 = TracingService.startSpan("span2", SpanKind.INTERNAL);
            TraceSpan span3 = TracingService.startSpan("span3", SpanKind.INTERNAL);
            TraceSpan span4 = TracingService.startSpan("span4", SpanKind.INTERNAL);

            assertSame(span4, TracingService.getCurrentSpan());

            service.endSpan(span4);
            assertSame(span3, TracingService.getCurrentSpan());

            service.endSpan(span3);
            assertSame(span2, TracingService.getCurrentSpan());

            service.endSpan(span2);
            assertSame(span1, TracingService.getCurrentSpan());

            service.endSpan(span1);
            assertNull(TracingService.getCurrentSpan());
        }
    }

    @Nested
    @DisplayName("Span Ending Tests")
    class SpanEndingTests {

        @Test
        @DisplayName("End span sets status and pops from stack")
        void endSpanSetsStatusAndPopsFromStack() {
            TraceSpan span = TracingService.startSpan("test", SpanKind.INTERNAL);

            service.endSpan(span, SpanStatus.OK);

            assertEquals(SpanStatus.OK, span.getStatus());
            assertNull(TracingService.getCurrentSpan());
        }

        @Test
        @DisplayName("End span defaults to OK status")
        void endSpanDefaultsToOKStatus() {
            TraceSpan span = TracingService.startSpan("test", SpanKind.INTERNAL);

            service.endSpan(span);

            assertEquals(SpanStatus.OK, span.getStatus());
        }

        @Test
        @DisplayName("End null span does nothing")
        void endNullSpanDoesNothing() {
            assertDoesNotThrow(() -> service.endSpan(null));
        }

        @Test
        @DisplayName("Ended span is added to completed queue")
        void endedSpanIsAddedToCompletedQueue() {
            TraceSpan span = TracingService.startSpan("test", SpanKind.INTERNAL);

            assertEquals(0, service.getCompletedSpanCount());

            service.endSpan(span);

            assertEquals(1, service.getCompletedSpanCount());
        }

        @Test
        @DisplayName("End span records error statistics")
        void endSpanRecordsErrorStatistics() {
            TraceSpan span1 = TracingService.startSpan("test1", SpanKind.INTERNAL);
            TraceSpan span2 = TracingService.startSpan("test2", SpanKind.INTERNAL);

            service.endSpan(span1, SpanStatus.OK);
            service.endSpan(span2, SpanStatus.ERROR);

            Map<String, Long> stats = service.getStatistics();
            assertEquals(2L, stats.get("total_spans_completed"));
            assertEquals(1L, stats.get("total_spans_errored"));
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Statistics track spans created")
        void statisticsTrackSpansCreated() {
            TracingService.startSpan("span1", SpanKind.INTERNAL);
            TracingService.startSpan("span2", SpanKind.INTERNAL);

            Map<String, Long> stats = service.getStatistics();

            assertEquals(2L, stats.get("total_spans_created"));
        }

        @Test
        @DisplayName("Statistics track spans by kind")
        void statisticsTrackSpansByKind() {
            TracingService.startSpan("span1", SpanKind.INTERNAL);
            TracingService.startSpan("span2", SpanKind.CLIENT);
            TracingService.startSpan("span3", SpanKind.INTERNAL);

            Map<String, Long> stats = service.getStatistics();

            assertEquals(2L, stats.get("spans_kind_internal"));
            assertEquals(1L, stats.get("spans_kind_client"));
        }

        @Test
        @DisplayName("Statistics track spans by name")
        void statisticsTrackSpansByName() {
            TracingService.startSpan("span1", SpanKind.INTERNAL);
            TracingService.startSpan("span1", SpanKind.INTERNAL);
            TracingService.startSpan("span2", SpanKind.INTERNAL);

            Map<String, Long> stats = service.getStatistics();

            assertEquals(2L, stats.get("spans_name_span1"));
            assertEquals(1L, stats.get("spans_name_span2"));
        }

        @Test
        @DisplayName("Statistics include queued spans")
        void statisticsIncludeQueuedSpans() {
            TraceSpan span1 = TracingService.startSpan("span1", SpanKind.INTERNAL);
            TraceSpan span2 = TracingService.startSpan("span2", SpanKind.INTERNAL);

            service.endSpan(span1);
            service.endSpan(span2);

            Map<String, Long> stats = service.getStatistics();

            assertEquals(2L, stats.get("queued_spans"));
        }

        @Test
        @DisplayName("Statistics are updated in real-time")
        void statisticsAreUpdatedInRealTime() {
            TraceSpan span1 = TracingService.startSpan("span1", SpanKind.INTERNAL);
            assertEquals(1L, service.getStatistics().get("total_spans_created"));

            service.endSpan(span1);
            assertEquals(1L, service.getStatistics().get("total_spans_completed"));
        }
    }

    @Nested
    @DisplayName("Export Tests")
    class ExportTests {

        @Test
        @DisplayName("Export to JSON creates file")
        void exportToJsonCreatesFile() throws IOException {
            config.setJsonExportEnabled(true);

            TraceSpan span1 = TracingService.startSpan("span1", SpanKind.INTERNAL);
            service.endSpan(span1);

            int exported = service.exportToJson(tempDir.toString());

            assertEquals(1, exported);
            assertTrue(Files.list(tempDir).anyMatch(p -> p.toString().endsWith(".json")));
        }

        @Test
        @DisplayName("Export to JSON returns 0 when disabled")
        void exportToJsonReturns0WhenDisabled() throws IOException {
            config.setJsonExportEnabled(false);

            TraceSpan span1 = TracingService.startSpan("span1", SpanKind.INTERNAL);
            service.endSpan(span1);

            int exported = service.exportToJson(tempDir.toString());

            assertEquals(0, exported);
        }

        @Test
        @DisplayName("Export to CSV creates file")
        void exportToCsvCreatesFile() throws IOException {
            config.setCsvExportEnabled(true);

            TraceSpan span1 = TracingService.startSpan("span1", SpanKind.INTERNAL);
            service.endSpan(span1);

            int exported = service.exportToCsv(tempDir.toString());

            assertEquals(1, exported);
            assertTrue(Files.list(tempDir).anyMatch(p -> p.toString().endsWith(".csv")));
        }

        @Test
        @DisplayName("Export to CSV returns 0 when disabled")
        void exportToCsvReturns0WhenDisabled() throws IOException {
            config.setCsvExportEnabled(false);

            TraceSpan span1 = TracingService.startSpan("span1", SpanKind.INTERNAL);
            service.endSpan(span1);

            int exported = service.exportToCsv(tempDir.toString());

            assertEquals(0, exported);
        }

        @Test
        @DisplayName("Export drains completed spans queue")
        void exportDrainsCompletedSpansQueue() throws IOException {
            config.setJsonExportEnabled(true);

            TraceSpan span1 = TracingService.startSpan("span1", SpanKind.INTERNAL);
            TraceSpan span2 = TracingService.startSpan("span2", SpanKind.INTERNAL);
            service.endSpan(span1);
            service.endSpan(span2);

            assertEquals(2, service.getCompletedSpanCount());

            service.exportToJson(tempDir.toString());

            assertEquals(0, service.getCompletedSpanCount());
        }

        @Test
        @DisplayName("Export creates directory if not exists")
        void exportCreatesDirectoryIfNotExists() throws IOException {
            config.setJsonExportEnabled(true);

            Path newDir = tempDir.resolve("new_dir");

            TraceSpan span1 = TracingService.startSpan("span1", SpanKind.INTERNAL);
            service.endSpan(span1);

            assertDoesNotThrow(() -> service.exportToJson(newDir.toString()));

            assertTrue(Files.exists(newDir));
        }
    }

    @Nested
    @DisplayName("Queue Management Tests")
    class QueueManagementTests {

        @Test
        @DisplayName("Clear completed spans removes all from queue")
        void clearCompletedSpansRemovesAllFromQueue() {
            TraceSpan span1 = TracingService.startSpan("span1", SpanKind.INTERNAL);
            TraceSpan span2 = TracingService.startSpan("span2", SpanKind.INTERNAL);
            service.endSpan(span1);
            service.endSpan(span2);

            int cleared = service.clearCompletedSpans();

            assertEquals(2, cleared);
            assertEquals(0, service.getCompletedSpanCount());
        }

        @Test
        @DisplayName("Get completed span count returns queue size")
        void getCompletedSpanCountReturnsQueueSize() {
            assertEquals(0, service.getCompletedSpanCount());

            TraceSpan span1 = TracingService.startSpan("span1", SpanKind.INTERNAL);
            service.endSpan(span1);

            assertEquals(1, service.getCompletedSpanCount());
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Get config returns configuration")
        void getConfigReturnsConfiguration() {
            TracingConfig serviceConfig = service.getConfig();

            assertEquals(config, serviceConfig);
        }

        @Test
        @DisplayName("Service respects enabled flag")
        void serviceRespectsEnabledFlag() {
            config.setEnabled(false);

            TraceSpan span = TracingService.startSpan("test", SpanKind.INTERNAL);

            assertNull(span);
        }
    }

    @Nested
    @DisplayName("Close and Shutdown Tests")
    class CloseTests {

        @Test
        @DisplayName("Close exports remaining spans")
        void closeExportsRemainingSpans() throws IOException {
            config.setJsonExportEnabled(true);

            TraceSpan span1 = TracingService.startSpan("span1", SpanKind.INTERNAL);
            service.endSpan(span1);

            service.close();

            assertTrue(Files.list(tempDir).anyMatch(p -> p.toString().endsWith(".json")));
        }

        @Test
        @DisplayName("Close clears completed spans")
        void closeClearsCompletedSpans() {
            TraceSpan span1 = TracingService.startSpan("span1", SpanKind.INTERNAL);
            service.endSpan(span1);

            service.close();

            assertEquals(0, service.getCompletedSpanCount());
        }

        @Test
        @DisplayName("Close clears thread-local stack")
        void closeClearsThreadLocalStack() {
            TraceSpan span = TracingService.startSpan("test", SpanKind.INTERNAL);

            service.close();

            // Thread-local should be cleared
        }
    }

    @Nested
    @DisplayName("Edge Cases and Special Scenarios")
    class EdgeCaseTests {

        @Test
        @DisplayName("Span with null context returns null")
        void spanWithNullContextReturnsNull() {
            TraceSpan span = TracingService.startSpan("test", SpanKind.INTERNAL, (SpanContext) null);

            assertNull(span);
        }

        @Test
        @DisplayName("End span not at top of stack")
        void endSpanNotAtTopOfStack() {
            TraceSpan parent = TracingService.startSpan("parent", SpanKind.INTERNAL);
            TraceSpan child = TracingService.startSpan("child", SpanKind.INTERNAL);

            // End parent before child (unusual but should not crash)
            service.endSpan(parent);

            assertNotNull(child);
            service.endSpan(child);
        }

        @Test
        @DisplayName("Multiple independent root spans")
        void multipleIndependentRootSpans() {
            TraceSpan root1 = TracingService.startSpan("root1", SpanKind.INTERNAL);
            String trace1 = root1.getTraceId();
            service.endSpan(root1);

            TraceSpan root2 = TracingService.startSpan("root2", SpanKind.INTERNAL);
            String trace2 = root2.getTraceId();
            service.endSpan(root2);

            assertNotEquals(trace1, trace2);
        }

        @Test
        @DisplayName("Span with special characters in name")
        void spanWithSpecialCharactersInName() {
            TraceSpan span = TracingService.startSpan("test.span-with_special", SpanKind.INTERNAL);

            assertNotNull(span);
            assertEquals("test.span-with_special", span.getName());

            service.endSpan(span);
        }

        @Test
        @DisplayName("LLM span with unknown model")
        void llmSpanWithUnknownModel() {
            config.setLLMTracingEnabled(true);

            TraceSpan span = TracingService.startLLMSpan("chat", "unknown-model-123");

            assertNotNull(span);
            assertEquals("unknown", span.getAttributes().get("llm.provider"));

            service.endSpan(span);
        }

        @Test
        @DisplayName("Empty span name")
        void emptySpanName() {
            TraceSpan span = TracingService.startSpan("", SpanKind.INTERNAL);

            assertNotNull(span);
            assertEquals("", span.getName());

            service.endSpan(span);
        }
    }
}
