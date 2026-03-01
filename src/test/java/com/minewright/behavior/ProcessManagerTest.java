package com.minewright.behavior;

import com.minewright.behavior.processes.FollowProcess;
import com.minewright.behavior.processes.IdleProcess;
import com.minewright.behavior.processes.SurvivalProcess;
import com.minewright.behavior.processes.TaskExecutionProcess;
import com.minewright.entity.ForemanEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProcessManager behavior arbitration.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Priority ordering (highest priority process selected)</li>
 *   <li>Process transitions (activate/deactivate)</li>
 *   <li>Survival process interrupts (preempts lower priority)</li>
 *   <li>Idle fallback (no other process can run)</li>
 * </ul>
 *
 * @since 1.2.0
 */
class ProcessManagerTest {

    @Mock
    private ForemanEntity mockForeman;

    private ProcessManager processManager;

    // Mock processes for testing
    private MockBehaviorProcess survivalProcess;
    private MockBehaviorProcess taskExecutionProcess;
    private MockBehaviorProcess followProcess;
    private MockBehaviorProcess idleProcess;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup mock foreman
        when(mockForeman.getEntityName()).thenReturn("TestForeman");
        when(mockForeman.isAlive()).thenReturn(true);
        when(mockForeman.getHealth()).thenReturn(20.0f);
        when(mockForeman.getMaxHealth()).thenReturn(20.0f);

        // Create process manager
        processManager = new ProcessManager(mockForeman);

        // Create mock processes with different priorities
        survivalProcess = new MockBehaviorProcess("Survival", 100);
        taskExecutionProcess = new MockBehaviorProcess("TaskExecution", 50);
        followProcess = new MockBehaviorProcess("Follow", 25);
        idleProcess = new MockBehaviorProcess("Idle", 10);

        // Register processes in priority order
        processManager.registerProcess(survivalProcess);
        processManager.registerProcess(taskExecutionProcess);
        processManager.registerProcess(followProcess);
        processManager.registerProcess(idleProcess);
    }

    @Test
    void testProcessRegistration() {
        List<BehaviorProcess> processes = processManager.getProcesses();

        assertEquals(4, processes.size());
        assertEquals("Survival", processes.get(0).getName());
        assertEquals("TaskExecution", processes.get(1).getName());
        assertEquals("Follow", processes.get(2).getName());
        assertEquals("Idle", processes.get(3).getName());
    }

    @Test
    void testCannotRegisterDuplicateProcess() {
        assertThrows(IllegalArgumentException.class, () -> {
            processManager.registerProcess(survivalProcess);
        });
    }

    @Test
    void testCannotRegisterNullProcess() {
        assertThrows(IllegalArgumentException.class, () -> {
            processManager.registerProcess(null);
        });
    }

    @Test
    void testPriorityOrdering_HighestPrioritySelected() {
        // Setup: Only TaskExecution and Idle can run
        taskExecutionProcess.setCanRun(true);
        idleProcess.setCanRun(true);

        // When: tick() is called
        processManager.tick();

        // Then: TaskExecution should be selected (higher priority than Idle)
        assertEquals("TaskExecution", processManager.getActiveProcessName());
        assertTrue(taskExecutionProcess.isActive());
        assertFalse(idleProcess.isActive());
    }

    @Test
    void testSurvivalProcessPreemptsLowerPriority() {
        // Setup: TaskExecution is active
        taskExecutionProcess.setCanRun(true);
        processManager.tick();
        assertEquals("TaskExecution", processManager.getActiveProcessName());

        // When: Survival process can run (emergency!)
        survivalProcess.setCanRun(true);
        processManager.tick();

        // Then: Survival should preempt TaskExecution
        assertEquals("Survival", processManager.getActiveProcessName());
        assertTrue(survivalProcess.isActive());
        assertFalse(taskExecutionProcess.isActive());
        assertTrue(taskExecutionProcess.wasDeactivated());
    }

    @Test
    void testProcessActivation() {
        // Setup: Only TaskExecution can run
        taskExecutionProcess.setCanRun(true);

        // When: tick() is called
        processManager.tick();

        // Then: TaskExecution should be activated
        assertTrue(taskExecutionProcess.wasActivated());
        assertTrue(taskExecutionProcess.isActive());
    }

    @Test
    void testProcessDeactivation() {
        // Setup: TaskExecution is active
        taskExecutionProcess.setCanRun(true);
        processManager.tick();
        assertTrue(taskExecutionProcess.isActive());

        // When: TaskExecution can no longer run
        taskExecutionProcess.setCanRun(false);
        processManager.tick();

        // Then: TaskExecution should be deactivated
        assertTrue(taskExecutionProcess.wasDeactivated());
        assertFalse(taskExecutionProcess.isActive());
    }

    @Test
    void testIdleFallback_NoOtherProcessCanRun() {
        // Setup: Only Idle can run (no tasks, no danger, etc.)
        idleProcess.setCanRun(true);

        // When: tick() is called
        processManager.tick();

        // Then: Idle should be selected
        assertEquals("Idle", processManager.getActiveProcessName());
        assertTrue(idleProcess.isActive());
    }

    @Test
    void testTransitionBetweenProcesses() {
        // Setup: TaskExecution is active
        taskExecutionProcess.setCanRun(true);
        processManager.tick();
        assertEquals("TaskExecution", processManager.getActiveProcessName());

        // Reset activation flags
        taskExecutionProcess.resetFlags();
        followProcess.resetFlags();

        // When: TaskExecution can't run, but Follow can
        taskExecutionProcess.setCanRun(false);
        followProcess.setCanRun(true);
        processManager.tick();

        // Then: Should transition from TaskExecution to Follow
        assertEquals("Follow", processManager.getActiveProcessName());
        assertTrue(taskExecutionProcess.wasDeactivated());
        assertTrue(followProcess.wasActivated());
        assertTrue(followProcess.isActive());
    }

    @Test
    void testForceDeactivate() {
        // Setup: TaskExecution is active
        taskExecutionProcess.setCanRun(true);
        processManager.tick();
        assertEquals("TaskExecution", processManager.getActiveProcessName());

        // Reset activation flags
        taskExecutionProcess.resetFlags();

        // When: forceDeactivate() is called
        processManager.forceDeactivate();

        // Then: Process should be deactivated
        assertEquals("IDLE", processManager.getActiveProcessName());
        assertTrue(taskExecutionProcess.wasDeactivated());
        assertFalse(taskExecutionProcess.isActive());
    }

    @Test
    void testIsProcessActive() {
        // Setup: TaskExecution is active
        taskExecutionProcess.setCanRun(true);
        processManager.tick();

        // Then: isProcessActive() should return true
        assertTrue(processManager.isProcessActive("TaskExecution"));
        assertFalse(processManager.isProcessActive("Survival"));
    }

    @Test
    void testClearProcesses() {
        // Setup: Multiple processes registered
        assertEquals(4, processManager.getProcessCount());

        // When: clearProcesses() is called
        processManager.clearProcesses();

        // Then: All processes should be removed
        assertEquals(0, processManager.getProcessCount());
        assertEquals("IDLE", processManager.getActiveProcessName());
    }

    @Test
    void testProcessTickCalled() {
        // Setup: TaskExecution can run
        taskExecutionProcess.setCanRun(true);

        // When: tick() is called
        processManager.tick();

        // Then: Process tick() should be called
        assertEquals(1, taskExecutionProcess.getTickCount());
    }

    @Test
    void testProcessTickCalledMultipleTimes() {
        // Setup: TaskExecution can run
        taskExecutionProcess.setCanRun(true);

        // When: tick() is called multiple times
        processManager.tick();
        processManager.tick();
        processManager.tick();

        // Then: Process tick() should be called each time
        assertEquals(3, taskExecutionProcess.getTickCount());
    }

    // === Mock Process Class for Testing ===

    private static class MockBehaviorProcess implements BehaviorProcess {
        private final String name;
        private final int priority;
        private boolean canRun = false;
        private boolean active = false;
        private boolean wasActivated = false;
        private boolean wasDeactivated = false;
        private int tickCount = 0;

        public MockBehaviorProcess(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        public void setCanRun(boolean canRun) {
            this.canRun = canRun;
        }

        public boolean wasActivated() {
            return wasActivated;
        }

        public boolean wasDeactivated() {
            return wasDeactivated;
        }

        public int getTickCount() {
            return tickCount;
        }

        public void resetFlags() {
            wasActivated = false;
            wasDeactivated = false;
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
            if (!active) {
                throw new IllegalStateException("tick() called but process is not active");
            }
            tickCount++;
        }

        @Override
        public void onActivate() {
            active = true;
            wasActivated = true;
        }

        @Override
        public void onDeactivate() {
            active = false;
            wasDeactivated = true;
        }
    }
}
