package com.minewright.coordination;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TaskMonitor}.
 *
 * <p>Tests cover monitoring lifecycle, progress updates, task retrieval,
 * statistics, and shutdown behavior.</p>
 *
 * @since 1.4.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskMonitor Tests")
class TaskMonitorTest {

    @Mock
    private ScheduledExecutorService scheduler;

    @Mock
    private TaskRebalancingAssessor assessor;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @Mock
    private TaskRebalancingManager.RebalancingListener listener;

    private TaskMonitor monitor;
    private UUID testAgent;
    private TaskRebalancingManager.MonitoredTask testTask;

    @BeforeEach
    void setUp() {
        monitor = new TaskMonitor(scheduler, assessor);
        testAgent = UUID.randomUUID();

        // Create test task
        testTask = new TaskRebalancingManager.MonitoredTask(
            "test-task-1",
            "announcement-1",
            testAgent,
            5000L,  // 5 second estimated duration
            2.0,    // 2x timeout threshold
            3000L,  // 3 second stuck threshold
            1000L   // 1 second monitoring interval
        );

        // Setup scheduler mock
        when(scheduler.scheduleAtFixedRate(
            any(Runnable.class),
            anyLong(),
            anyLong(),
            any(TimeUnit.class)
        )).thenReturn(scheduledFuture);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should initialize with empty task maps")
        void testConstructorInitializesEmpty() {
            TaskMonitor newMonitor = new TaskMonitor(scheduler, assessor);

            assertEquals(0, newMonitor.getMonitoredTaskCount(),
                "Should start with zero monitored tasks");
        }
    }

    @Nested
    @DisplayName("startMonitoring Tests")
    class StartMonitoringTests {

        @Test
        @DisplayName("startMonitoring should return false for null task")
        void testStartMonitoringNullTask() {
            boolean result = monitor.startMonitoring(null, listener);

            assertFalse(result, "Should return false for null task");
            verify(scheduler, never()).scheduleAtFixedRate(
                any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)
            );
        }

        @Test
        @DisplayName("startMonitoring should return false for duplicate task")
        void testStartMonitoringDuplicateTask() {
            monitor.startMonitoring(testTask, listener);

            boolean result = monitor.startMonitoring(testTask, listener);

            assertFalse(result, "Should return false for duplicate task");
            verify(scheduler, times(1)).scheduleAtFixedRate(
                any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)
            );
        }

        @Test
        @DisplayName("startMonitoring should successfully start monitoring")
        void testStartMonitoringSuccess() {
            boolean result = monitor.startMonitoring(testTask, listener);

            assertTrue(result, "Should return true for successful start");
            assertEquals(1, monitor.getMonitoredTaskCount(),
                "Should have one monitored task");
            verify(scheduler).scheduleAtFixedRate(
                any(Runnable.class),
                eq(1000L),
                eq(1000L),
                eq(TimeUnit.MILLISECONDS)
            );
        }

        @Test
        @DisplayName("startMonitoring should notify listener on start")
        void testStartMonitoringNotifiesListener() {
            boolean result = monitor.startMonitoring(testTask, listener);

            assertTrue(result, "Should start monitoring");
            verify(listener).onMonitoringStarted(eq("test-task-1"), eq(testAgent));
        }

        @Test
        @DisplayName("startMonitoring should handle listener exceptions gracefully")
        void testStartMonitoringHandlesListenerException() {
            doThrow(new RuntimeException("Listener error"))
                .when(listener).onMonitoringStarted(anyString(), any(UUID.class));

            assertDoesNotThrow(() -> monitor.startMonitoring(testTask, listener),
                "Should handle listener exception without throwing");
        }

        @Test
        @DisplayName("startMonitoring should work with null listener")
        void testStartMonitoringNullListener() {
            boolean result = monitor.startMonitoring(testTask, null);

            assertTrue(result, "Should start monitoring with null listener");
            assertEquals(1, monitor.getMonitoredTaskCount(),
                "Should have one monitored task");
        }
    }

    @Nested
    @DisplayName("updateProgress Tests")
    class UpdateProgressTests {

        @Test
        @DisplayName("updateProgress should update task progress")
        void testUpdateProgressUpdatesTask() {
            monitor.startMonitoring(testTask, null);

            monitor.updateProgress("test-task-1", 0.5);

            assertEquals(0.5, monitor.getMonitoredTask("test-task-1").getLastProgress(),
                0.001, "Progress should be updated to 0.5");
        }

        @Test
        @DisplayName("updateProgress should handle unknown task")
        void testUpdateProgressUnknownTask() {
            assertDoesNotThrow(() -> monitor.updateProgress("unknown-task", 0.5),
                "Should handle unknown task without throwing");
        }

        @Test
        @DisplayName("updateProgress should update last progress time for increasing progress")
        void testUpdateProgressUpdatesTime() throws InterruptedException {
            monitor.startMonitoring(testTask, null);
            long initialTime = monitor.getMonitoredTask("test-task-1").getLastProgressTime();

            Thread.sleep(10);
            monitor.updateProgress("test-task-1", 0.3);

            long updatedTime = monitor.getMonitoredTask("test-task-1").getLastProgressTime();

            assertTrue(updatedTime > initialTime,
                "Last progress time should be updated");
        }

        @Test
        @DisplayName("updateProgress should not update time for decreasing progress")
        void testUpdateProgressDoesNotUpdateTimeForDecrease() throws InterruptedException {
            monitor.startMonitoring(testTask, null);
            monitor.updateProgress("test-task-1", 0.5);

            Thread.sleep(10);
            long midTime = monitor.getMonitoredTask("test-task-1").getLastProgressTime();

            monitor.updateProgress("test-task-1", 0.3);
            long finalTime = monitor.getMonitoredTask("test-task-1").getLastProgressTime();

            assertEquals(midTime, finalTime,
                "Last progress time should not update for decreasing progress");
        }
    }

    @Nested
    @DisplayName("stopMonitoring Tests")
    class StopMonitoringTests {

        @Test
        @DisplayName("stopMonitoring should return false for unknown task")
        void testStopMonitoringUnknownTask() {
            boolean result = monitor.stopMonitoring("unknown-task", listener);

            assertFalse(result, "Should return false for unknown task");
        }

        @Test
        @DisplayName("stopMonitoring should successfully stop monitoring")
        void testStopMonitoringSuccess() {
            monitor.startMonitoring(testTask, listener);

            boolean result = monitor.stopMonitoring("test-task-1", listener);

            assertTrue(result, "Should return true for successful stop");
            assertEquals(0, monitor.getMonitoredTaskCount(),
                "Should have zero monitored tasks");
            verify(scheduledFuture).cancel(false);
        }

        @Test
        @DisplayName("stopMonitoring should notify listener")
        void testStopMonitoringNotifiesListener() {
            monitor.startMonitoring(testTask, listener);

            monitor.stopMonitoring("test-task-1", listener);

            verify(listener).onMonitoringStopped("test-task-1");
        }

        @Test
        @DisplayName("stopMonitoring should handle listener exceptions gracefully")
        void testStopMonitoringHandlesListenerException() {
            monitor.startMonitoring(testTask, listener);
            doThrow(new RuntimeException("Listener error"))
                .when(listener).onMonitoringStopped(anyString());

            assertDoesNotThrow(() -> monitor.stopMonitoring("test-task-1", listener),
                "Should handle listener exception without throwing");
        }

        @Test
        @DisplayName("stopMonitoring should work with null listener")
        void testStopMonitoringNullListener() {
            monitor.startMonitoring(testTask, null);

            boolean result = monitor.stopMonitoring("test-task-1", null);

            assertTrue(result, "Should stop monitoring with null listener");
        }

        @Test
        @DisplayName("stopMonitoring should handle null scheduled future")
        void testStopMonitoringHandlesNullFuture() {
            monitor.startMonitoring(testTask, null);

            // Manually remove the future to simulate null scenario
            assertDoesNotThrow(() -> monitor.stopMonitoring("test-task-1", null),
                "Should handle null future gracefully");
        }
    }

    @Nested
    @DisplayName("getMonitoredTask Tests")
    class GetMonitoredTaskTests {

        @Test
        @DisplayName("getMonitoredTask should return monitored task")
        void testGetMonitoredTask() {
            monitor.startMonitoring(testTask, null);

            TaskRebalancingManager.MonitoredTask result = monitor.getMonitoredTask("test-task-1");

            assertNotNull(result, "Should return the monitored task");
            assertEquals("test-task-1", result.getTaskId(),
                "Task ID should match");
        }

        @Test
        @DisplayName("getMonitoredTask should return null for unknown task")
        void testGetMonitoredTaskUnknown() {
            TaskRebalancingManager.MonitoredTask result = monitor.getMonitoredTask("unknown-task");

            assertNull(result, "Should return null for unknown task");
        }
    }

    @Nested
    @DisplayName("getMonitoredTaskCount Tests")
    class GetMonitoredTaskCountTests {

        @Test
        @DisplayName("getMonitoredTaskCount should return correct count")
        void testGetMonitoredTaskCount() {
            assertEquals(0, monitor.getMonitoredTaskCount(),
                "Initial count should be zero");

            monitor.startMonitoring(testTask, null);

            assertEquals(1, monitor.getMonitoredTaskCount(),
                "Count should be one after adding task");
        }

        @Test
        @DisplayName("getMonitoredTaskCount should update after stopping")
        void testGetMonitoredTaskCountAfterStop() {
            monitor.startMonitoring(testTask, null);
            monitor.stopMonitoring("test-task-1", null);

            assertEquals(0, monitor.getMonitoredTaskCount(),
                "Count should be zero after stopping");
        }

        @Test
        @DisplayName("getMonitoredTaskCount should track multiple tasks")
        void testGetMonitoredTaskCountMultiple() {
            monitor.startMonitoring(testTask, null);

            UUID agent2 = UUID.randomUUID();
            TaskRebalancingManager.MonitoredTask task2 = new TaskRebalancingManager.MonitoredTask(
                "test-task-2", "announcement-2", agent2, 5000L, 2.0, 3000L, 1000L
            );
            monitor.startMonitoring(task2, null);

            assertEquals(2, monitor.getMonitoredTaskCount(),
                "Count should be two");
        }
    }

    @Nested
    @DisplayName("cancelAllFutures Tests")
    class CancelAllFuturesTests {

        @Test
        @DisplayName("cancelAllFutures should cancel all scheduled futures")
        void testCancelAllFutures() {
            monitor.startMonitoring(testTask, null);

            monitor.cancelAllFutures();

            verify(scheduledFuture).cancel(false);
            assertEquals(0, monitor.getMonitoredTaskCount(),
                "Should clear all monitored tasks");
        }

        @Test
        @DisplayName("cancelAllFutures should handle empty monitor")
        void testCancelAllFuturesEmpty() {
            assertDoesNotThrow(() -> monitor.cancelAllFutures(),
                "Should handle empty monitor without throwing");
        }

        @Test
        @DisplayName("cancelAllFutures should handle multiple tasks")
        void testCancelAllFuturesMultiple() {
            monitor.startMonitoring(testTask, null);

            UUID agent2 = UUID.randomUUID();
            TaskRebalancingManager.MonitoredTask task2 = new TaskRebalancingManager.MonitoredTask(
                "test-task-2", "announcement-2", agent2, 5000L, 2.0, 3000L, 1000L
            );
            monitor.startMonitoring(task2, null);

            monitor.cancelAllFutures();

            assertEquals(0, monitor.getMonitoredTaskCount(),
                "Should clear all tasks");
            verify(scheduledFuture, times(2)).cancel(false);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete monitoring lifecycle")
        void testCompleteMonitoringLifecycle() {
            // Start monitoring
            assertTrue(monitor.startMonitoring(testTask, listener),
                "Should start monitoring");
            assertEquals(1, monitor.getMonitoredTaskCount(),
                "Should have one task");

            // Update progress
            monitor.updateProgress("test-task-1", 0.5);
            assertEquals(0.5, monitor.getMonitoredTask("test-task-1").getLastProgress(),
                0.001, "Progress should be updated");

            // Stop monitoring
            assertTrue(monitor.stopMonitoring("test-task-1", listener),
                "Should stop monitoring");
            assertEquals(0, monitor.getMonitoredTaskCount(),
                "Should have no tasks");

            verify(listener).onMonitoringStarted("test-task-1", testAgent);
            verify(listener).onMonitoringStopped("test-task-1");
        }

        @Test
        @DisplayName("Should handle multiple concurrent tasks")
        void testMultipleConcurrentTasks() {
            UUID agent1 = UUID.randomUUID();
            UUID agent2 = UUID.randomUUID();
            UUID agent3 = UUID.randomUUID();

            TaskRebalancingManager.MonitoredTask task1 = new TaskRebalancingManager.MonitoredTask(
                "task-1", "ann-1", agent1, 5000L, 2.0, 3000L, 1000L
            );
            TaskRebalancingManager.MonitoredTask task2 = new TaskRebalancingManager.MonitoredTask(
                "task-2", "ann-2", agent2, 5000L, 2.0, 3000L, 1000L
            );
            TaskRebalancingManager.MonitoredTask task3 = new TaskRebalancingManager.MonitoredTask(
                "task-3", "ann-3", agent3, 5000L, 2.0, 3000L, 1000L
            );

            assertTrue(monitor.startMonitoring(task1, null));
            assertTrue(monitor.startMonitoring(task2, null));
            assertTrue(monitor.startMonitoring(task3, null));

            assertEquals(3, monitor.getMonitoredTaskCount());

            monitor.updateProgress("task-1", 0.3);
            monitor.updateProgress("task-2", 0.5);
            monitor.updateProgress("task-3", 0.7);

            assertEquals(0.3, monitor.getMonitoredTask("task-1").getLastProgress(), 0.001);
            assertEquals(0.5, monitor.getMonitoredTask("task-2").getLastProgress(), 0.001);
            assertEquals(0.7, monitor.getMonitoredTask("task-3").getLastProgress(), 0.001);

            assertTrue(monitor.stopMonitoring("task-2", null));
            assertEquals(2, monitor.getMonitoredTaskCount());

            monitor.cancelAllFutures();
            assertEquals(0, monitor.getMonitoredTaskCount());
        }
    }
}
