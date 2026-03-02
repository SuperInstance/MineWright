package com.minewright.integration;

import com.minewright.behavior.BehaviorProcess;
import com.minewright.behavior.ProcessManager;
import com.minewright.behavior.processes.IdleProcess;
import com.minewright.behavior.processes.SurvivalProcess;
import com.minewright.behavior.processes.TaskExecutionProcess;
import com.minewright.entity.ForemanEntity;
import com.minewright.orchestration.AgentRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for process arbitration - Survival process interrupts task execution.
 *
 * <p><b>Test Scenarios:</b></p>
 * <ul>
 *   <li>Survival process interrupts task execution when agent is in danger</li>
 *   <li>Priority ordering works correctly (Survival > TaskExecution > Idle)</li>
 *   <li>Process transitions happen cleanly with onActivate/onDeactivate</li>
 *   <li>Multiple processes registered but only one active at a time</li>
 *   <li>Process selection based on canRun() conditions</li>
 *   <li>Forced deactivation for emergency stops</li>
 * </ul>
 *
 * @see ProcessManager
 * @see BehaviorProcess
 * @see SurvivalProcess
 * @see TaskExecutionProcess
 * @see IntegrationTestBase
 * @since 1.2.0
 */
@DisplayName("Process Arbitration Integration Tests")
class ProcessArbitrationIntegrationTest extends IntegrationTestBase {

    private ForemanEntity foreman;
    private ProcessManager processManager;
    private List<TestBehaviorProcess> testProcesses;

    @BeforeEach
    void setUpProcessArbitrationTest() {
        // Create foreman entity
        foreman = createForeman("TestSteve", AgentRole.WORKER);

        // Create process manager
        processManager = new ProcessManager(foreman);

        // Initialize list of test processes
        testProcesses = new ArrayList<>();
    }

    @AfterEach
    void tearDownProcessArbitrationTest() {
        if (processManager != null) {
            processManager.clearProcesses();
        }
        testProcesses.clear();
    }

    @Test
    @DisplayName("Survival process interrupts task execution when agent health is critical")
    void testSurvivalInterruptsTask() {
        // Create test processes with different priorities
        TestBehaviorProcess taskProcess = new TestBehaviorProcess(
            "TaskExecution", 50, false);
        TestBehaviorProcess survivalProcess = new TestBehaviorProcess(
            "Survival", 100, false);
        TestBehaviorProcess idleProcess = new TestBehaviorProcess(
            "Idle", 10, true); // Always can run

        // Register processes in order
        processManager.registerProcess(idleProcess);
        processManager.registerProcess(taskProcess);
        processManager.registerProcess(survivalProcess);

        // Initially, task process should be active
        taskProcess.setCanRun(true);
        tickProcessManager();

        assertEquals("TaskExecution", processManager.getActiveProcessName(),
            "Task process should be active initially");
        assertTrue(taskProcess.isActive(),
            "Task process should be marked as active");
        assertTrue(taskProcess.wasActivated(),
            "Task process should have received onActivate() call");

        // Simulate survival condition
        taskProcess.setCanRun(false);
        survivalProcess.setCanRun(true);

        // Tick manager to trigger arbitration
        tickProcessManager();

        // Survival process should now be active
        assertEquals("Survival", processManager.getActiveProcessName(),
            "Survival process should interrupt and become active");
        assertTrue(survivalProcess.isActive(),
            "Survival process should be marked as active");
        assertFalse(taskProcess.isActive(),
            "Task process should no longer be active");
        assertTrue(taskProcess.wasDeactivated(),
            "Task process should have received onDeactivate() call");
    }

    @Test
    @DisplayName("Priority ordering works correctly - higher priority wins")
    void testPriorityOrdering() {
        // Create processes with various priorities
        TestBehaviorProcess lowPriority = new TestBehaviorProcess("Low", 10, true);
        TestBehaviorProcess mediumPriority = new TestBehaviorProcess("Medium", 50, true);
        TestBehaviorProcess highPriority = new TestBehaviorProcess("High", 100, true);

        // Register in any order
        processManager.registerProcess(mediumPriority);
        processManager.registerProcess(lowPriority);
        processManager.registerProcess(highPriority);

        // Tick to trigger arbitration
        tickProcessManager();

        // Highest priority process should win
        assertEquals("High", processManager.getActiveProcessName(),
            "Highest priority process should be selected");
        assertTrue(highPriority.isActive(),
            "High priority process should be active");
        assertFalse(mediumPriority.isActive(),
            "Medium priority process should not be active");
        assertFalse(lowPriority.isActive(),
            "Low priority process should not be active");
    }

    @Test
    @DisplayName("Process transitions happen cleanly with proper lifecycle")
    void testProcessTransitions() {
        TestBehaviorProcess taskProcess = new TestBehaviorProcess("Task", 50, true);
        TestBehaviorProcess survivalProcess = new TestBehaviorProcess("Survival", 100, false);

        processManager.registerProcess(taskProcess);
        processManager.registerProcess(survivalProcess);

        // Initial tick - task should be active
        tickProcessManager();
        assertTrue(taskProcess.isActive(),
            "Task process should be active");
        assertTrue(taskProcess.wasActivated(),
            "Task process should be activated");
        assertFalse(taskProcess.wasDeactivated(),
            "Task process should not be deactivated yet");

        // Simulate survival condition
        taskProcess.setCanRun(false);
        survivalProcess.setCanRun(true);

        // Reset activation flags for clearer testing
        taskProcess.resetActivationFlags();
        survivalProcess.resetActivationFlags();

        // Tick to trigger transition
        tickProcessManager();

        // Verify clean transition
        assertTrue(taskProcess.wasDeactivated(),
            "Task process should be deactivated during transition");
        assertTrue(survivalProcess.wasActivated(),
            "Survival process should be activated during transition");
        assertFalse(taskProcess.isActive(),
            "Task process should no longer be active");
        assertTrue(survivalProcess.isActive(),
            "Survival process should now be active");
    }

    @Test
    @DisplayName("Only one process is active at a time")
    void testSingleActiveProcess() {
        // Create multiple processes that can all run
        TestBehaviorProcess process1 = new TestBehaviorProcess("P1", 10, true);
        TestBehaviorProcess process2 = new TestBehaviorProcess("P2", 20, true);
        TestBehaviorProcess process3 = new TestBehaviorProcess("P3", 30, true);

        processManager.registerProcess(process1);
        processManager.registerProcess(process2);
        processManager.registerProcess(process3);

        // Tick to trigger arbitration
        tickProcessManager();

        // Count active processes
        int activeCount = 0;
        if (process1.isActive()) activeCount++;
        if (process2.isActive()) activeCount++;
        if (process3.isActive()) activeCount++;

        assertEquals(1, activeCount,
            "Exactly one process should be active");
        assertEquals("P3", processManager.getActiveProcessName(),
            "Highest priority process should be the active one");
    }

    @Test
    @DisplayName("Process selection based on canRun() conditions")
    void testCanRunSelection() {
        TestBehaviorProcess survival = new TestBehaviorProcess("Survival", 100, false);
        TestBehaviorProcess task = new TestBehaviorProcess("Task", 50, false);
        TestBehaviorProcess idle = new TestBehaviorProcess("Idle", 10, true); // Always can run

        processManager.registerProcess(survival);
        processManager.registerProcess(task);
        processManager.registerProcess(idle);

        // Initially only idle can run
        tickProcessManager();
        assertEquals("Idle", processManager.getActiveProcessName(),
            "Idle process should be active when others can't run");

        // Enable task process
        task.setCanRun(true);
        tickProcessManager();
        assertEquals("Task", processManager.getActiveProcessName(),
            "Task process should become active when it can run");

        // Enable survival process (highest priority)
        survival.setCanRun(true);
        tickProcessManager();
        assertEquals("Survival", processManager.getActiveProcessName(),
            "Survival process should preempt when it can run");
    }

    @Test
    @DisplayName("Forced deactivation stops current process immediately")
    void testForcedDeactivation() {
        TestBehaviorProcess taskProcess = new TestBehaviorProcess("Task", 50, true);
        TestBehaviorProcess idleProcess = new TestBehaviorProcess("Idle", 10, true);

        processManager.registerProcess(taskProcess);
        processManager.registerProcess(idleProcess);

        // Start with task active
        tickProcessManager();
        assertEquals("Task", processManager.getActiveProcessName(),
            "Task should be active initially");

        // Force deactivate
        processManager.forceDeactivate();

        // Verify task was deactivated
        assertNull(processManager.getActiveProcess(),
            "No process should be active after forced deactivation");
        assertFalse(taskProcess.isActive(),
            "Task process should be deactivated");
        assertTrue(taskProcess.wasDeactivated(),
            "Task process should have received onDeactivate()");

        // Next tick should select a new process (idle, as it can run)
        tickProcessManager();
        assertEquals("Idle", processManager.getActiveProcessName(),
            "Idle process should become active after forced deactivation");
    }

    @Test
    @DisplayName("Process continues ticking while active")
    void testProcessContinuesTicking() {
        TestBehaviorProcess taskProcess = new TestBehaviorProcess("Task", 50, true);

        processManager.registerProcess(taskProcess);

        // Initial tick
        tickProcessManager();
        assertEquals(1, taskProcess.getTickCount(),
            "Process should have ticked once");

        // Continue ticking
        tickProcessManager();
        tickProcessManager();
        tickProcessManager();

        assertEquals(4, taskProcess.getTickCount(),
            "Process should have ticked 4 times total");
    }

    @Test
    @DisplayName("Process state persists across ticks when continuously selected")
    void testProcessStatePersistence() {
        TestBehaviorProcess taskProcess = new TestBehaviorProcess("Task", 50, true);

        processManager.registerProcess(taskProcess);

        // Tick multiple times
        for (int i = 0; i < 10; i++) {
            tickProcessManager();
        }

        // Process should still be active
        assertTrue(taskProcess.isActive(),
            "Process should remain active across ticks");
        assertEquals(1, taskProcess.getActivationCount(),
            "Process should only be activated once");
        assertEquals(0, taskProcess.getDeactivationCount(),
            "Process should not be deactivated while remaining selected");
        assertEquals(10, taskProcess.getTickCount(),
            "Process should tick 10 times");
    }

    @Test
    @DisplayName("Process manager handles process exceptions gracefully")
    void testProcessExceptionHandling() {
        TestBehaviorProcess failingProcess = new TestBehaviorProcess("Failing", 50, true) {
            @Override
            public void tick() {
                super.tick();
                if (getTickCount() == 3) {
                    throw new RuntimeException("Simulated process failure");
                }
            }
        };

        TestBehaviorProcess fallbackProcess = new TestBehaviorProcess("Fallback", 10, true);

        processManager.registerProcess(fallbackProcess);
        processManager.registerProcess(failingProcess);

        // Tick until failure occurs
        tickProcessManager(); // tick 1
        tickProcessManager(); // tick 2
        tickProcessManager(); // tick 3 - failure occurs here

        // After failure, manager should deactivate failing process
        // and fallback to next available
        assertEquals("Fallback", processManager.getActiveProcessName(),
            "Fallback process should become active after failure");
    }

    // ==================== Helper Methods ====================

    /**
     * Ticks the process manager once.
     */
    private void tickProcessManager() {
        if (processManager != null) {
            processManager.tick();
        }
    }

    /**
     * Test implementation of BehaviorProcess for testing.
     */
    private static class TestBehaviorProcess implements BehaviorProcess {
        private final String name;
        private final int priority;
        private boolean canRun;
        private boolean active;
        private int tickCount;
        private int activationCount;
        private int deactivationCount;
        private boolean wasActivated;
        private boolean wasDeactivated;

        public TestBehaviorProcess(String name, int priority, boolean canRun) {
            this.name = name;
            this.priority = priority;
            this.canRun = canRun;
            this.active = false;
            this.tickCount = 0;
            this.activationCount = 0;
            this.deactivationCount = 0;
            this.wasActivated = false;
            this.wasDeactivated = false;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public boolean canRun() {
            return canRun;
        }

        @Override
        public void tick() {
            if (active) {
                tickCount++;
            }
        }

        @Override
        public void onActivate() {
            active = true;
            activationCount++;
            wasActivated = true;
        }

        @Override
        public void onDeactivate() {
            active = false;
            deactivationCount++;
            wasDeactivated = true;
        }

        public void setCanRun(boolean canRun) {
            this.canRun = canRun;
        }

        public int getTickCount() {
            return tickCount;
        }

        public int getActivationCount() {
            return activationCount;
        }

        public int getDeactivationCount() {
            return deactivationCount;
        }

        public boolean wasActivated() {
            return wasActivated;
        }

        public boolean wasDeactivated() {
            return wasDeactivated;
        }

        public void resetActivationFlags() {
            wasActivated = false;
            wasDeactivated = false;
        }
    }

    // ==================== Assertion Helpers ====================

    private void assertNull(Object obj, String message) {
        if (obj != null) {
            throw new AssertionError(message);
        }
    }

    private void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(message + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }

    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }
}
