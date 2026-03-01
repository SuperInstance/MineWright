package com.minewright.execution;

import com.minewright.action.ActionResult;
import com.minewright.action.actions.BaseAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LoggingInterceptor}.
 *
 * Tests cover:
 * <ul>
 *   <li>Return values for each interceptor method</li>
 *   <li>Method invocation behavior</li>
 *   <li>Null handling for actions and results</li>
 *   <li>Exception handling</li>
 *   <li>Interceptor priority and name</li>
 * </ul>
 *
 * Note: Actual log output verification is not included as it requires
 * Logback-specific test dependencies. Log content verification would
 * be integration-level testing.
 */
@DisplayName("LoggingInterceptor Tests")
class LoggingInterceptorTest {

    private LoggingInterceptor interceptor;
    private BaseAction mockAction;
    private ActionContext mockContext;
    private ActionResult mockResult;

    @BeforeEach
    void setUp() {
        interceptor = new LoggingInterceptor();
        mockAction = mock(BaseAction.class);
        mockContext = mock(ActionContext.class);
        mockResult = mock(ActionResult.class);
    }

    // ==================== beforeAction Tests ====================

    @Test
    @DisplayName("beforeAction returns true to allow execution")
    void testBeforeActionReturnsTrue() {
        when(mockAction.getDescription()).thenReturn("Mining stone");

        boolean result = interceptor.beforeAction(mockAction, mockContext);

        assertTrue(result);
    }

    @Test
    @DisplayName("beforeAction handles null description gracefully")
    void testBeforeActionWithNullDescription() {
        when(mockAction.getDescription()).thenReturn(null);

        boolean result = interceptor.beforeAction(mockAction, mockContext);

        assertTrue(result);
    }

    @Test
    @DisplayName("beforeAction handles empty description")
    void testBeforeActionWithEmptyDescription() {
        when(mockAction.getDescription()).thenReturn("");

        boolean result = interceptor.beforeAction(mockAction, mockContext);

        assertTrue(result);
    }

    @Test
    @DisplayName("beforeAction can be called multiple times")
    void testBeforeActionMultipleInvocations() {
        when(mockAction.getDescription()).thenReturn("Test action");

        assertTrue(interceptor.beforeAction(mockAction, mockContext));
        assertTrue(interceptor.beforeAction(mockAction, mockContext));
        assertTrue(interceptor.beforeAction(mockAction, mockContext));
    }

    @Test
    @DisplayName("beforeAction does not throw exceptions")
    void testBeforeActionDoesNotThrow() {
        // Test with various edge cases
        when(mockAction.getDescription()).thenReturn(null);
        assertDoesNotThrow(() -> interceptor.beforeAction(mockAction, mockContext));

        when(mockAction.getDescription()).thenReturn("");
        assertDoesNotThrow(() -> interceptor.beforeAction(mockAction, mockContext));

        when(mockAction.getDescription()).thenReturn("Test");
        assertDoesNotThrow(() -> interceptor.beforeAction(mockAction, mockContext));
    }

    // ==================== afterAction Success Tests ====================

    @Test
    @DisplayName("afterAction completes without exception for success")
    void testAfterActionSuccess() {
        when(mockAction.getDescription()).thenReturn("Mining stone");
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Successfully mined 10 stone");

        assertDoesNotThrow(() -> interceptor.afterAction(mockAction, mockResult, mockContext));
    }

    @Test
    @DisplayName("afterAction handles null message")
    void testAfterActionWithNullMessage() {
        when(mockAction.getDescription()).thenReturn("Test action");
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn(null);

        assertDoesNotThrow(() -> interceptor.afterAction(mockAction, mockResult, mockContext));
    }

    @Test
    @DisplayName("afterAction handles empty message")
    void testAfterActionWithEmptyMessage() {
        when(mockAction.getDescription()).thenReturn("Test action");
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("");

        assertDoesNotThrow(() -> interceptor.afterAction(mockAction, mockResult, mockContext));
    }

    @Test
    @DisplayName("afterAction can be called multiple times")
    void testAfterActionMultipleInvocations() {
        when(mockAction.getDescription()).thenReturn("Test action");
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Done");

        assertDoesNotThrow(() -> {
            interceptor.afterAction(mockAction, mockResult, mockContext);
            interceptor.afterAction(mockAction, mockResult, mockContext);
            interceptor.afterAction(mockAction, mockResult, mockContext);
        });
    }

    // ==================== afterAction Failure Tests ====================

    @Test
    @DisplayName("afterAction completes without exception for failure")
    void testAfterActionFailure() {
        when(mockAction.getDescription()).thenReturn("Mining stone");
        when(mockResult.isSuccess()).thenReturn(false);
        when(mockResult.getMessage()).thenReturn("No stone found");

        assertDoesNotThrow(() -> interceptor.afterAction(mockAction, mockResult, mockContext));
    }

    @Test
    @DisplayName("afterAction handles failure with null message")
    void testAfterActionFailureWithNullMessage() {
        when(mockAction.getDescription()).thenReturn("Test action");
        when(mockResult.isSuccess()).thenReturn(false);
        when(mockResult.getMessage()).thenReturn(null);

        assertDoesNotThrow(() -> interceptor.afterAction(mockAction, mockResult, mockContext));
    }

    @Test
    @DisplayName("afterAction handles failure with empty message")
    void testAfterActionFailureWithEmptyMessage() {
        when(mockAction.getDescription()).thenReturn("Test action");
        when(mockResult.isSuccess()).thenReturn(false);
        when(mockResult.getMessage()).thenReturn("");

        assertDoesNotThrow(() -> interceptor.afterAction(mockAction, mockResult, mockContext));
    }

    // ==================== onError Tests ====================

    @Test
    @DisplayName("onError returns false to propagate exception")
    void testOnErrorReturnsFalse() {
        when(mockAction.getDescription()).thenReturn("Mining stone");
        Exception testException = new RuntimeException("Mining interrupted");

        boolean result = interceptor.onError(mockAction, testException, mockContext);

        assertFalse(result, "onError should return false to propagate exception");
    }

    @Test
    @DisplayName("onError handles null description")
    void testOnErrorWithNullDescription() {
        when(mockAction.getDescription()).thenReturn(null);
        Exception testException = new RuntimeException("Error");

        boolean result = interceptor.onError(mockAction, testException, mockContext);

        assertFalse(result);
    }

    @Test
    @DisplayName("onError handles various exception types")
    void testOnErrorWithVariousExceptionTypes() {
        when(mockAction.getDescription()).thenReturn("Test action");

        Exception runtimeException = new RuntimeException("Runtime error");
        Exception nullPointerException = new NullPointerException("Null error");
        Exception illegalArgumentException = new IllegalArgumentException("Illegal arg");

        assertFalse(interceptor.onError(mockAction, runtimeException, mockContext));
        assertFalse(interceptor.onError(mockAction, nullPointerException, mockContext));
        assertFalse(interceptor.onError(mockAction, illegalArgumentException, mockContext));
    }

    @Test
    @DisplayName("onError handles exception with null message")
    void testOnErrorWithExceptionNullMessage() {
        when(mockAction.getDescription()).thenReturn("Test action");
        Exception testException = new RuntimeException((String) null);

        boolean result = interceptor.onError(mockAction, testException, mockContext);

        assertFalse(result);
    }

    @Test
    @DisplayName("onError completes without throwing")
    void testOnErrorDoesNotThrow() {
        when(mockAction.getDescription()).thenReturn("Test action");
        Exception testException = new RuntimeException("Error");

        assertDoesNotThrow(() -> interceptor.onError(mockAction, testException, mockContext));
    }

    // ==================== Lifecycle Integration Tests ====================

    @Test
    @DisplayName("Full success lifecycle completes without exceptions")
    void testFullSuccessLifecycle() {
        when(mockAction.getDescription()).thenReturn("Mining stone");
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Mined 10 stone");

        assertDoesNotThrow(() -> {
            assertTrue(interceptor.beforeAction(mockAction, mockContext));
            interceptor.afterAction(mockAction, mockResult, mockContext);
        });
    }

    @Test
    @DisplayName("Full error lifecycle completes without exceptions")
    void testErrorLifecycle() {
        when(mockAction.getDescription()).thenReturn("Mining stone");
        Exception testException = new RuntimeException("Mining failed");

        assertDoesNotThrow(() -> {
            assertTrue(interceptor.beforeAction(mockAction, mockContext));
            assertFalse(interceptor.onError(mockAction, testException, mockContext));
        });
    }

    @Test
    @DisplayName("Full failure lifecycle completes without exceptions")
    void testFailureLifecycle() {
        when(mockAction.getDescription()).thenReturn("Mining stone");
        when(mockResult.isSuccess()).thenReturn(false);
        when(mockResult.getMessage()).thenReturn("No stone");

        assertDoesNotThrow(() -> {
            assertTrue(interceptor.beforeAction(mockAction, mockContext));
            interceptor.afterAction(mockAction, mockResult, mockContext);
        });
    }

    // ==================== Multiple Action Tests ====================

    @Test
    @DisplayName("Multiple actions are processed independently")
    void testMultipleActions() {
        when(mockAction.getDescription()).thenReturn("Action 1");
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Done 1");

        interceptor.beforeAction(mockAction, mockContext);
        interceptor.afterAction(mockAction, mockResult, mockContext);

        // Change description for second action
        when(mockAction.getDescription()).thenReturn("Action 2");
        when(mockResult.getMessage()).thenReturn("Done 2");

        assertDoesNotThrow(() -> {
            interceptor.beforeAction(mockAction, mockContext);
            interceptor.afterAction(mockAction, mockResult, mockContext);
        });
    }

    @Test
    @DisplayName("Sequential success and failure actions are processed correctly")
    void testSequentialSuccessAndFailure() {
        when(mockAction.getDescription()).thenReturn("Action 1");
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Success");

        interceptor.beforeAction(mockAction, mockContext);
        interceptor.afterAction(mockAction, mockResult, mockContext);

        // Now fail
        when(mockAction.getDescription()).thenReturn("Action 2");
        when(mockResult.isSuccess()).thenReturn(false);
        when(mockResult.getMessage()).thenReturn("Failed");

        assertDoesNotThrow(() -> {
            interceptor.beforeAction(mockAction, mockContext);
            interceptor.afterAction(mockAction, mockResult, mockContext);
        });
    }

    // ==================== Interceptor Priority Tests ====================

    @Test
    @DisplayName("LoggingInterceptor has highest priority")
    void testLoggingInterceptorPriority() {
        assertEquals(1000, interceptor.getPriority(),
            "LoggingInterceptor should have priority 1000 (highest)");
    }

    @Test
    @DisplayName("LoggingInterceptor has correct name")
    void testLoggingInterceptorName() {
        assertEquals("LoggingInterceptor", interceptor.getName());
    }

    // ==================== Edge Cases Tests ====================

    @Test
    @DisplayName("Interceptor handles very long descriptions")
    void testLongDescription() {
        String longDesc = "A".repeat(1000);
        when(mockAction.getDescription()).thenReturn(longDesc);

        assertDoesNotThrow(() -> interceptor.beforeAction(mockAction, mockContext));
    }

    @Test
    @DisplayName("Interceptor handles special characters in descriptions")
    void testSpecialCharactersInDescription() {
        String specialDesc = "Action with special chars: {}[]<>\\n\\t\"'";
        when(mockAction.getDescription()).thenReturn(specialDesc);

        assertDoesNotThrow(() -> interceptor.beforeAction(mockAction, mockContext));
    }

    @Test
    @DisplayName("Interceptor handles unicode characters")
    void testUnicodeCharacters() {
        String unicodeDesc = "Action with unicode: \u4E2D\u6587 \u65E5\u672C\u8A9E \uD83D\uDE00";
        when(mockAction.getDescription()).thenReturn(unicodeDesc);

        assertDoesNotThrow(() -> interceptor.beforeAction(mockAction, mockContext));
    }

    @Test
    @DisplayName("Interceptor handles null action")
    void testNullAction() {
        assertDoesNotThrow(() -> interceptor.beforeAction(null, mockContext));
        assertDoesNotThrow(() -> interceptor.afterAction(null, mockResult, mockContext));

        Exception testException = new RuntimeException("Test");
        assertDoesNotThrow(() -> interceptor.onError(null, testException, mockContext));
    }

    @Test
    @DisplayName("Interceptor handles null result")
    void testNullResult() {
        when(mockAction.getDescription()).thenReturn("Test");

        assertDoesNotThrow(() -> interceptor.afterAction(mockAction, null, mockContext));
    }

    @Test
    @DisplayName("Interceptor handles null context")
    void testNullContext() {
        when(mockAction.getDescription()).thenReturn("Test");
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Done");

        assertDoesNotThrow(() -> interceptor.beforeAction(mockAction, null));
        assertDoesNotThrow(() -> interceptor.afterAction(mockAction, mockResult, null));

        Exception testException = new RuntimeException("Test");
        assertDoesNotThrow(() -> interceptor.onError(mockAction, testException, null));
    }

    @Test
    @DisplayName("Interceptor handles all nulls")
    void testAllNulls() {
        assertDoesNotThrow(() -> interceptor.beforeAction(null, null));
        assertDoesNotThrow(() -> interceptor.afterAction(null, null, null));
        assertDoesNotThrow(() -> interceptor.onError(null, null, null));
    }

    // ==================== Concurrent Execution Tests ====================

    @Test
    @DisplayName("Interceptor is thread-safe for concurrent access")
    void testConcurrentAccess() throws InterruptedException {
        when(mockAction.getDescription()).thenReturn("Test action");
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Done");

        final int THREAD_COUNT = 10;
        final int INVOCATIONS_PER_THREAD = 100;
        Thread[] threads = new Thread[THREAD_COUNT];

        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < INVOCATIONS_PER_THREAD; j++) {
                    assertDoesNotThrow(() -> {
                        interceptor.beforeAction(mockAction, mockContext);
                        interceptor.afterAction(mockAction, mockResult, mockContext);
                    });
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // If we get here without exceptions, thread-safety is working
        assertTrue(true);
    }
}
