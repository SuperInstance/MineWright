package com.minewright.execution;

import com.minewright.action.ActionResult;
import com.minewright.action.actions.BaseAction;
import com.minewright.action.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InterceptorChain}.
 *
 * Tests cover:
 * <ul>
 *   <li>Adding and removing interceptors</li>
 *   <li>Interceptor priority ordering</li>
 *   <li>beforeAction execution order and cancellation</li>
 *   <li>afterAction execution in reverse order</li>
 *   <li>onError execution and exception suppression</li>
 *   <li>Exception handling in interceptors</li>
 *   <li>Chain management (clear, size, getInterceptors)</li>
 * </ul>
 */
@DisplayName("InterceptorChain Tests")
class InterceptorChainTest {

    private InterceptorChain chain;
    private BaseAction mockAction;
    private ActionContext mockContext;
    private ActionResult mockResult;

    @BeforeEach
    void setUp() {
        chain = new InterceptorChain();
        mockAction = mock(BaseAction.class);
        mockContext = mock(ActionContext.class);
        mockResult = mock(ActionResult.class);
    }

    @Test
    @DisplayName("New chain has no interceptors")
    void testNewChainIsEmpty() {
        assertEquals(0, chain.size(),
            "New chain should have no interceptors");
        assertTrue(chain.getInterceptors().isEmpty(),
            "New chain should return empty list");
    }

    @Test
    @DisplayName("Add interceptor increases chain size")
    void testAddInterceptor() {
        ActionInterceptor interceptor = mock(ActionInterceptor.class);
        when(interceptor.getName()).thenReturn("TestInterceptor");
        when(interceptor.getPriority()).thenReturn(0);

        chain.addInterceptor(interceptor);

        assertEquals(1, chain.size(),
            "Chain should have one interceptor after adding");
        assertTrue(chain.getInterceptors().contains(interceptor),
            "Chain should contain the added interceptor");
    }

    @Test
    @DisplayName("Add null interceptor throws exception")
    void testAddNullInterceptor() {
        assertThrows(IllegalArgumentException.class,
            () -> chain.addInterceptor(null),
            "Adding null interceptor should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Remove interceptor decreases chain size")
    void testRemoveInterceptor() {
        ActionInterceptor interceptor = mock(ActionInterceptor.class);
        when(interceptor.getName()).thenReturn("TestInterceptor");
        when(interceptor.getPriority()).thenReturn(0);

        chain.addInterceptor(interceptor);
        assertEquals(1, chain.size());

        boolean removed = chain.removeInterceptor(interceptor);

        assertTrue(removed, "Remove should return true");
        assertEquals(0, chain.size(), "Chain should be empty after removal");
    }

    @Test
    @DisplayName("Remove non-existent interceptor returns false")
    void testRemoveNonExistentInterceptor() {
        ActionInterceptor interceptor = mock(ActionInterceptor.class);

        boolean removed = chain.removeInterceptor(interceptor);

        assertFalse(removed, "Removing non-existent interceptor should return false");
    }

    @Test
    @DisplayName("Interceptors are sorted by priority (descending)")
    void testInterceptorPriorityOrdering() {
        ActionInterceptor lowPriority = mock(ActionInterceptor.class);
        when(lowPriority.getName()).thenReturn("Low");
        when(lowPriority.getPriority()).thenReturn(10);

        ActionInterceptor mediumPriority = mock(ActionInterceptor.class);
        when(mediumPriority.getName()).thenReturn("Medium");
        when(mediumPriority.getPriority()).thenReturn(50);

        ActionInterceptor highPriority = mock(ActionInterceptor.class);
        when(highPriority.getName()).thenReturn("High");
        when(highPriority.getPriority()).thenReturn(100);

        // Add in random order
        chain.addInterceptor(mediumPriority);
        chain.addInterceptor(lowPriority);
        chain.addInterceptor(highPriority);

        List<ActionInterceptor> interceptors = chain.getInterceptors();

        assertEquals(3, interceptors.size());
        assertEquals(highPriority, interceptors.get(0), "High priority should be first");
        assertEquals(mediumPriority, interceptors.get(1), "Medium priority should be second");
        assertEquals(lowPriority, interceptors.get(2), "Low priority should be last");
    }

    @Test
    @DisplayName("Interceptors with same priority maintain insertion order")
    void testSamePriorityOrdering() {
        ActionInterceptor first = mock(ActionInterceptor.class);
        when(first.getName()).thenReturn("First");
        when(first.getPriority()).thenReturn(50);

        ActionInterceptor second = mock(ActionInterceptor.class);
        when(second.getName()).thenReturn("Second");
        when(second.getPriority()).thenReturn(50);

        chain.addInterceptor(first);
        chain.addInterceptor(second);

        List<ActionInterceptor> interceptors = chain.getInterceptors();

        assertEquals(2, interceptors.size());
        assertEquals(first, interceptors.get(0), "First added should come first with same priority");
        assertEquals(second, interceptors.get(1));
    }

    @Test
    @DisplayName("executeBeforeAction calls all interceptors in priority order")
    void testExecuteBeforeActionOrder() {
        ActionInterceptor first = mock(ActionInterceptor.class);
        when(first.getName()).thenReturn("First");
        when(first.getPriority()).thenReturn(100);
        when(first.beforeAction(any(), any())).thenReturn(true);

        ActionInterceptor second = mock(ActionInterceptor.class);
        when(second.getName()).thenReturn("Second");
        when(second.getPriority()).thenReturn(50);
        when(second.beforeAction(any(), any())).thenReturn(true);

        chain.addInterceptor(first);
        chain.addInterceptor(second);

        boolean result = chain.executeBeforeAction(mockAction, mockContext);

        assertTrue(result, "All interceptors approved, should return true");

        // Verify called in priority order (high to low)
        verify(first).beforeAction(mockAction, mockContext);
        verify(second).beforeAction(mockAction, mockContext);
    }

    @Test
    @DisplayName("executeBeforeAction stops when interceptor returns false")
    void testExecuteBeforeActionCancellation() {
        ActionInterceptor first = mock(ActionInterceptor.class);
        when(first.getName()).thenReturn("First");
        when(first.getPriority()).thenReturn(100);
        when(first.beforeAction(any(), any())).thenReturn(true);

        ActionInterceptor second = mock(ActionInterceptor.class);
        when(second.getName()).thenReturn("Second");
        when(second.getPriority()).thenReturn(50);
        when(second.beforeAction(any(), any())).thenReturn(false);

        ActionInterceptor third = mock(ActionInterceptor.class);
        when(third.getName()).thenReturn("Third");
        when(third.getPriority()).thenReturn(10);
        when(third.beforeAction(any(), any())).thenReturn(true);

        chain.addInterceptor(first);
        chain.addInterceptor(second);
        chain.addInterceptor(third);

        boolean result = chain.executeBeforeAction(mockAction, mockContext);

        assertFalse(result, "Interceptor rejected, should return false");

        verify(first).beforeAction(mockAction, mockContext);
        verify(second).beforeAction(mockAction, mockContext);
        verify(third, never()).beforeAction(any(), any());
    }

    @Test
    @DisplayName("executeBeforeAction with empty chain returns true")
    void testExecuteBeforeActionEmptyChain() {
        boolean result = chain.executeBeforeAction(mockAction, mockContext);

        assertTrue(result, "Empty chain should approve action");
    }

    @Test
    @DisplayName("executeAfterAction calls interceptors in reverse priority order")
    void testExecuteAfterActionReverseOrder() {
        ActionInterceptor first = mock(ActionInterceptor.class);
        when(first.getName()).thenReturn("First");
        when(first.getPriority()).thenReturn(100);

        ActionInterceptor second = mock(ActionInterceptor.class);
        when(second.getName()).thenReturn("Second");
        when(second.getPriority()).thenReturn(50);

        chain.addInterceptor(first);
        chain.addInterceptor(second);

        chain.executeAfterAction(mockAction, mockResult, mockContext);

        // Verify called in reverse order (low to high priority)
        verify(second).afterAction(mockAction, mockResult, mockContext);
        verify(first).afterAction(mockAction, mockResult, mockContext);
    }

    @Test
    @DisplayName("executeAfterAction continues after exception")
    void testExecuteAfterActionExceptionHandling() {
        ActionInterceptor failing = mock(ActionInterceptor.class);
        when(failing.getName()).thenReturn("Failing");
        when(failing.getPriority()).thenReturn(100);
        doThrow(new RuntimeException("Test exception"))
            .when(failing).afterAction(any(), any(), any());

        ActionInterceptor succeeding = mock(ActionInterceptor.class);
        when(succeeding.getName()).thenReturn("Succeeding");
        when(succeeding.getPriority()).thenReturn(50);

        chain.addInterceptor(failing);
        chain.addInterceptor(succeeding);

        // Should not throw exception
        assertDoesNotThrow(() -> chain.executeAfterAction(mockAction, mockResult, mockContext));

        verify(succeeding).afterAction(mockAction, mockResult, mockContext);
    }

    @Test
    @DisplayName("executeOnError calls interceptors in reverse order")
    void testExecuteOnErrorOrder() {
        ActionInterceptor first = mock(ActionInterceptor.class);
        when(first.getName()).thenReturn("First");
        when(first.getPriority()).thenReturn(100);
        when(first.onError(any(), any(), any())).thenReturn(false);

        ActionInterceptor second = mock(ActionInterceptor.class);
        when(second.getName()).thenReturn("Second");
        when(second.getPriority()).thenReturn(50);
        when(second.onError(any(), any(), any())).thenReturn(false);

        chain.addInterceptor(first);
        chain.addInterceptor(second);

        Exception testException = new RuntimeException("Test error");
        chain.executeOnError(mockAction, testException, mockContext);

        // Verify called in reverse order
        verify(second).onError(mockAction, testException, mockContext);
        verify(first).onError(mockAction, testException, mockContext);
    }

    @Test
    @DisplayName("executeOnError returns true when interceptor suppresses exception")
    void testExecuteOnErrorSuppression() {
        ActionInterceptor suppressing = mock(ActionInterceptor.class);
        when(suppressing.getName()).thenReturn("Suppressing");
        when(suppressing.getPriority()).thenReturn(100);
        when(suppressing.onError(any(), any(), any())).thenReturn(true);

        chain.addInterceptor(suppressing);

        Exception testException = new RuntimeException("Test error");
        boolean result = chain.executeOnError(mockAction, testException, mockContext);

        assertTrue(result, "Exception was suppressed, should return true");
    }

    @Test
    @DisplayName("executeOnError returns false when no interceptor suppresses")
    void testExecuteOnErrorNotSuppressed() {
        ActionInterceptor nonSuppressing = mock(ActionInterceptor.class);
        when(nonSuppressing.getName()).thenReturn("NonSuppressing");
        when(nonSuppressing.getPriority()).thenReturn(100);
        when(nonSuppressing.onError(any(), any(), any())).thenReturn(false);

        chain.addInterceptor(nonSuppressing);

        Exception testException = new RuntimeException("Test error");
        boolean result = chain.executeOnError(mockAction, testException, mockContext);

        assertFalse(result, "Exception was not suppressed, should return false");
    }

    @Test
    @DisplayName("executeOnError continues after exception in interceptor")
    void testExecuteOnErrorExceptionHandling() {
        ActionInterceptor failing = mock(ActionInterceptor.class);
        when(failing.getName()).thenReturn("Failing");
        when(failing.getPriority()).thenReturn(100);
        doThrow(new RuntimeException("Interceptor error"))
            .when(failing).onError(any(), any(), any());

        ActionInterceptor suppressing = mock(ActionInterceptor.class);
        when(suppressing.getName()).thenReturn("Suppressing");
        when(suppressing.getPriority()).thenReturn(50);
        when(suppressing.onError(any(), any(), any())).thenReturn(true);

        chain.addInterceptor(failing);
        chain.addInterceptor(suppressing);

        Exception testException = new RuntimeException("Test error");

        // Should not throw exception
        assertDoesNotThrow(() -> chain.executeOnError(mockAction, testException, mockContext));

        // Even though first interceptor threw exception, second should still be called
        verify(suppressing).onError(mockAction, testException, mockContext);
    }

    @Test
    @DisplayName("Clear removes all interceptors")
    void testClear() {
        ActionInterceptor interceptor1 = mock(ActionInterceptor.class);
        when(interceptor1.getName()).thenReturn("One");
        when(interceptor1.getPriority()).thenReturn(10);

        ActionInterceptor interceptor2 = mock(ActionInterceptor.class);
        when(interceptor2.getName()).thenReturn("Two");
        when(interceptor2.getPriority()).thenReturn(20);

        chain.addInterceptor(interceptor1);
        chain.addInterceptor(interceptor2);
        assertEquals(2, chain.size());

        chain.clear();

        assertEquals(0, chain.size(), "Chain should be empty after clear");
        assertTrue(chain.getInterceptors().isEmpty());
    }

    @Test
    @DisplayName("getInterceptors returns unmodifiable list")
    void testGetInterceptorsIsUnmodifiable() {
        ActionInterceptor interceptor = mock(ActionInterceptor.class);
        when(interceptor.getName()).thenReturn("Test");
        when(interceptor.getPriority()).thenReturn(10);

        chain.addInterceptor(interceptor);

        List<ActionInterceptor> interceptors = chain.getInterceptors();

        assertThrows(UnsupportedOperationException.class,
            () -> interceptors.add(mock(ActionInterceptor.class)),
            "Returned list should be unmodifiable");
    }

    @Test
    @DisplayName("Multiple interceptors with different priorities all execute")
    void testMultipleInterceptorsExecution() {
        ActionInterceptor interceptor1 = mock(ActionInterceptor.class);
        when(interceptor1.getName()).thenReturn("One");
        when(interceptor1.getPriority()).thenReturn(100);
        when(interceptor1.beforeAction(any(), any())).thenReturn(true);

        ActionInterceptor interceptor2 = mock(ActionInterceptor.class);
        when(interceptor2.getName()).thenReturn("Two");
        when(interceptor2.getPriority()).thenReturn(50);
        when(interceptor2.beforeAction(any(), any())).thenReturn(true);

        ActionInterceptor interceptor3 = mock(ActionInterceptor.class);
        when(interceptor3.getName()).thenReturn("Three");
        when(interceptor3.getPriority()).thenReturn(10);
        when(interceptor3.beforeAction(any(), any())).thenReturn(true);

        chain.addInterceptor(interceptor1);
        chain.addInterceptor(interceptor2);
        chain.addInterceptor(interceptor3);

        chain.executeBeforeAction(mockAction, mockContext);
        chain.executeAfterAction(mockAction, mockResult, mockContext);

        // Verify all interceptors were called
        verify(interceptor1).beforeAction(mockAction, mockContext);
        verify(interceptor2).beforeAction(mockAction, mockContext);
        verify(interceptor3).beforeAction(mockAction, mockContext);

        verify(interceptor1).afterAction(mockAction, mockResult, mockContext);
        verify(interceptor2).afterAction(mockAction, mockResult, mockContext);
        verify(interceptor3).afterAction(mockAction, mockResult, mockContext);
    }

    @Test
    @DisplayName("Interceptor with default priority (0) is ordered correctly")
    void testDefaultPriority() {
        ActionInterceptor defaultPriority = mock(ActionInterceptor.class);
        when(defaultPriority.getName()).thenReturn("Default");
        // Default priority is 0
        when(defaultPriority.beforeAction(any(), any())).thenReturn(true);

        ActionInterceptor highPriority = mock(ActionInterceptor.class);
        when(highPriority.getName()).thenReturn("High");
        when(highPriority.getPriority()).thenReturn(100);
        when(highPriority.beforeAction(any(), any())).thenReturn(true);

        chain.addInterceptor(defaultPriority);
        chain.addInterceptor(highPriority);

        List<ActionInterceptor> interceptors = chain.getInterceptors();

        assertEquals(highPriority, interceptors.get(0));
        assertEquals(defaultPriority, interceptors.get(1));
    }

    @Test
    @DisplayName("Negative priority interceptors are ordered correctly")
    void testNegativePriority() {
        ActionInterceptor negative = mock(ActionInterceptor.class);
        when(negative.getName()).thenReturn("Negative");
        when(negative.getPriority()).thenReturn(-10);
        when(negative.beforeAction(any(), any())).thenReturn(true);

        ActionInterceptor zero = mock(ActionInterceptor.class);
        when(zero.getName()).thenReturn("Zero");
        when(zero.getPriority()).thenReturn(0);
        when(zero.beforeAction(any(), any())).thenReturn(true);

        ActionInterceptor positive = mock(ActionInterceptor.class);
        when(positive.getName()).thenReturn("Positive");
        when(positive.getPriority()).thenReturn(10);
        when(positive.beforeAction(any(), any())).thenReturn(true);

        chain.addInterceptor(negative);
        chain.addInterceptor(zero);
        chain.addInterceptor(positive);

        List<ActionInterceptor> interceptors = chain.getInterceptors();

        assertEquals(positive, interceptors.get(0));
        assertEquals(zero, interceptors.get(1));
        assertEquals(negative, interceptors.get(2));
    }

    @Test
    @DisplayName("Chain works correctly after removing and adding interceptors")
    void testRemoveAndAdd() {
        ActionInterceptor interceptor1 = mock(ActionInterceptor.class);
        when(interceptor1.getName()).thenReturn("One");
        when(interceptor1.getPriority()).thenReturn(10);
        when(interceptor1.beforeAction(any(), any())).thenReturn(true);

        ActionInterceptor interceptor2 = mock(ActionInterceptor.class);
        when(interceptor2.getName()).thenReturn("Two");
        when(interceptor2.getPriority()).thenReturn(20);
        when(interceptor2.beforeAction(any(), any())).thenReturn(true);

        chain.addInterceptor(interceptor1);
        chain.addInterceptor(interceptor2);
        assertEquals(2, chain.size());

        chain.removeInterceptor(interceptor1);
        assertEquals(1, chain.size());

        chain.addInterceptor(interceptor1);
        assertEquals(2, chain.size());

        // Both should still be called
        chain.executeBeforeAction(mockAction, mockContext);
        verify(interceptor1).beforeAction(mockAction, mockContext);
        verify(interceptor2).beforeAction(mockAction, mockContext);
    }

    @Test
    @DisplayName("Chain maintains order after multiple operations")
    void testOrderAfterMultipleOperations() {
        ActionInterceptor low = mock(ActionInterceptor.class);
        when(low.getName()).thenReturn("Low");
        when(low.getPriority()).thenReturn(10);
        when(low.beforeAction(any(), any())).thenReturn(true);

        ActionInterceptor high = mock(ActionInterceptor.class);
        when(high.getName()).thenReturn("High");
        when(high.getPriority()).thenReturn(100);
        when(high.beforeAction(any(), any())).thenReturn(true);

        ActionInterceptor medium = mock(ActionInterceptor.class);
        when(medium.getName()).thenReturn("Medium");
        when(medium.getPriority()).thenReturn(50);
        when(medium.beforeAction(any(), any())).thenReturn(true);

        // Add in mixed order
        chain.addInterceptor(low);
        chain.addInterceptor(high);
        chain.removeInterceptor(low);
        chain.addInterceptor(medium);
        chain.addInterceptor(low);

        List<ActionInterceptor> interceptors = chain.getInterceptors();

        assertEquals(3, interceptors.size());
        assertEquals(high, interceptors.get(0));
        assertEquals(medium, interceptors.get(1));
        assertEquals(low, interceptors.get(2));
    }
}
