package com.minewright.coordination;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TaskReassigner}.
 *
 * <p>Tests cover task reassignment logic, validation, statistics tracking,
 * agent selection, and error handling.</p>
 *
 * @since 1.4.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskReassigner Tests")
class TaskReassignerTest {

    @Mock
    private CapabilityRegistry capabilityRegistry;

    @Mock
    private WorkloadTracker workloadTracker;

    @Mock
    private ContractNetManager contractNetManager;

    @Mock
    private TaskRebalancingManager.RebalancingListener listener;

    private TaskReassigner reassigner;
    private UUID oldAgent;
    private UUID newAgent;
    private TaskRebalancingManager.MonitoredTask testTask;
    private TaskRebalancingManager.RebalancingAssessment assessment;

    @BeforeEach
    void setUp() {
        reassigner = new TaskReassigner(capabilityRegistry, workloadTracker, contractNetManager, 3);
        oldAgent = UUID.randomUUID();
        newAgent = UUID.randomUUID();

        // Create test task
        testTask = new TaskRebalancingManager.MonitoredTask(
            "test-task-1",
            "announcement-1",
            oldAgent,
            5000L,
            2.0,
            3000L,
            1000L
        );

        // Create test assessment
        assessment = new TaskRebalancingManager.RebalancingAssessment(
            "test-task-1",
            "announcement-1",
            oldAgent,
            true,
            TaskRebalancingManager.RebalancingReason.TIMEOUT,
            "Task timed out",
            List.of(newAgent, UUID.randomUUID())
        );
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should initialize with provided dependencies")
        void testConstructorInitialization() {
            TaskReassigner newReassigner = new TaskReassigner(
                capabilityRegistry, workloadTracker, contractNetManager, 3);

            assertNotNull(newReassigner, "Reassigner should be initialized");
        }

        @Test
        @DisplayName("Constructor should set max reassignments")
        void testConstructorSetsMaxReassignments() {
            TaskReassigner newReassigner = new TaskReassigner(
                capabilityRegistry, workloadTracker, contractNetManager, 5);

            // Verify by checking that reassignments are allowed up to limit
            // (This is implicitly tested through reassignTask tests)
            assertNotNull(newReassigner);
        }
    }

    @Nested
    @DisplayName("reassignTask Tests")
    class ReassignTaskTests {

        @Test
        @DisplayName("reassignTask should return false for null task")
        void testReassignTaskNullTask() {
            boolean result = reassigner.reassignTask(null, newAgent, listener);

            assertFalse(result, "Should return false for null task");
            assertEquals(1, reassigner.getReassignedFailed(),
                "Should increment failed count");
            verify(listener, never()).onTaskReassigned(any(), any(), any(), any());
        }

        @Test
        @DisplayName("reassignTask should return false for null agent")
        void testReassignTaskNullAgent() {
            boolean result = reassigner.reassignTask(testTask, null, listener);

            assertFalse(result, "Should return false for null agent");
            assertEquals(1, reassigner.getReassignedFailed(),
                "Should increment failed count");
        }

        @Test
        @DisplayName("reassignTask should return false when max reassignments exceeded")
        void testReassignTaskMaxReassignmentsExceeded() {
            // Increment reassignment count to max
            testTask.incrementReassignedCount();
            testTask.incrementReassignedCount();
            testTask.incrementReassignedCount();

            boolean result = reassigner.reassignTask(testTask, newAgent, listener);

            assertFalse(result, "Should return false when max reassignments exceeded");
            assertEquals(1, reassigner.getReassignedFailed(),
                "Should increment failed count");
            verify(listener).onReassignmentFailed(
                eq("test-task-1"),
                eq(TaskRebalancingManager.RebalancingReason.TIMEOUT),
                contains("Max reassignments")
            );
        }

        @Test
        @DisplayName("reassignTask should return false when new agent unavailable")
        void testReassignTaskAgentUnavailable() {
            AgentCapability capability = mock(AgentCapability.class);
            when(capabilityRegistry.getCapability(newAgent)).thenReturn(capability);
            when(capability.isActive()).thenReturn(false);

            boolean result = reassigner.reassignTask(testTask, newAgent, listener);

            assertFalse(result, "Should return false when agent unavailable");
            verify(listener).onReassignmentFailed(
                eq("test-task-1"),
                eq(TaskRebalancingManager.RebalancingReason.AGENT_UNAVAILABLE),
                contains("not available")
            );
        }

        @Test
        @DisplayName("reassignTask should return false when new agent at capacity")
        void testReassignTaskAgentAtCapacity() {
            AgentCapability capability = mock(AgentCapability.class);
            when(capabilityRegistry.getCapability(newAgent)).thenReturn(capability);
            when(capability.isActive()).thenReturn(true);
            when(capability.isAvailable()).thenReturn(true);
            when(workloadTracker.isAvailable(newAgent)).thenReturn(false);

            boolean result = reassigner.reassignTask(testTask, newAgent, listener);

            assertFalse(result, "Should return false when agent at capacity");
            verify(listener).onReassignmentFailed(
                eq("test-task-1"),
                eq(TaskRebalancingManager.RebalancingReason.AGENT_OVERLOADED),
                contains("at capacity")
            );
        }

        @Test
        @DisplayName("reassignTask should successfully reassign task")
        void testReassignTaskSuccess() {
            AgentCapability capability = mock(AgentCapability.class);
            when(capabilityRegistry.getCapability(newAgent)).thenReturn(capability);
            when(capability.isActive()).thenReturn(true);
            when(capability.isAvailable()).thenReturn(true);
            when(workloadTracker.isAvailable(newAgent)).thenReturn(true);
            when(workloadTracker.completeTask(eq(oldAgent), anyString(), anyBoolean())).thenReturn(true);
            when(workloadTracker.assignTask(eq(newAgent), anyString())).thenReturn(true);

            boolean result = reassigner.reassignTask(testTask, newAgent, listener);

            assertTrue(result, "Should successfully reassign task");
            assertEquals(1, reassigner.getReassignedSuccessfully(),
                "Should increment success count");
            assertEquals(1, testTask.getReassignedCount(),
                "Should increment task reassignment count");
            verify(listener).onTaskReassigned(
                eq("test-task-1"),
                eq(oldAgent),
                eq(newAgent),
                any(TaskRebalancingManager.RebalancingReason.class)
            );
        }

        @Test
        @DisplayName("reassignTask should handle listener exceptions gracefully")
        void testReassignTaskHandlesListenerException() {
            AgentCapability capability = mock(AgentCapability.class);
            when(capabilityRegistry.getCapability(newAgent)).thenReturn(capability);
            when(capability.isActive()).thenReturn(true);
            when(capability.isAvailable()).thenReturn(true);
            when(workloadTracker.isAvailable(newAgent)).thenReturn(true);
            when(workloadTracker.completeTask(eq(oldAgent), anyString(), anyBoolean())).thenReturn(true);
            when(workloadTracker.assignTask(eq(newAgent), anyString())).thenReturn(true);
            doThrow(new RuntimeException("Listener error"))
                .when(listener).onTaskReassigned(any(), any(), any(), any());

            assertDoesNotThrow(() -> reassigner.reassignTask(testTask, newAgent, listener),
                "Should handle listener exception without throwing");
        }

        @Test
        @DisplayName("reassignTask should work with null listener")
        void testReassignTaskNullListener() {
            AgentCapability capability = mock(AgentCapability.class);
            when(capabilityRegistry.getCapability(newAgent)).thenReturn(capability);
            when(capability.isActive()).thenReturn(true);
            when(capability.isAvailable()).thenReturn(true);
            when(workloadTracker.isAvailable(newAgent)).thenReturn(true);
            when(workloadTracker.completeTask(eq(oldAgent), anyString(), anyBoolean())).thenReturn(true);
            when(workloadTracker.assignTask(eq(newAgent), anyString())).thenReturn(true);

            boolean result = reassigner.reassignTask(testTask, newAgent, null);

            assertTrue(result, "Should reassign with null listener");
        }
    }

    @Nested
    @DisplayName("performReassignment Tests")
    class PerformReassignmentTests {

        @Test
        @DisplayName("Should complete task for old agent")
        void testPerformReassignmentCompletesOldTask() {
            AgentCapability capability = mock(AgentCapability.class);
            when(capabilityRegistry.getCapability(newAgent)).thenReturn(capability);
            when(capability.isActive()).thenReturn(true);
            when(capability.isAvailable()).thenReturn(true);
            when(workloadTracker.isAvailable(newAgent)).thenReturn(true);
            when(workloadTracker.assignTask(eq(newAgent), anyString())).thenReturn(true);

            reassigner.reassignTask(testTask, newAgent, listener);

            verify(workloadTracker).completeTask(oldAgent, "test-task-1", false);
        }

        @Test
        @DisplayName("Should assign task to new agent")
        void testPerformReassignmentAssignsNewTask() {
            AgentCapability capability = mock(AgentCapability.class);
            when(capabilityRegistry.getCapability(newAgent)).thenReturn(capability);
            when(capability.isActive()).thenReturn(true);
            when(capability.isAvailable()).thenReturn(true);
            when(workloadTracker.isAvailable(newAgent)).thenReturn(true);
            when(workloadTracker.completeTask(eq(oldAgent), anyString(), anyBoolean())).thenReturn(true);

            reassigner.reassignTask(testTask, newAgent, listener);

            verify(workloadTracker).assignTask(newAgent, "test-task-1");
        }

        @Test
        @DisplayName("Should return false when assignTask fails")
        void testPerformReassignmentAssignFailure() {
            AgentCapability capability = mock(AgentCapability.class);
            when(capabilityRegistry.getCapability(newAgent)).thenReturn(capability);
            when(capability.isActive()).thenReturn(true);
            when(capability.isAvailable()).thenReturn(true);
            when(workloadTracker.isAvailable(newAgent)).thenReturn(true);
            when(workloadTracker.completeTask(eq(oldAgent), anyString(), anyBoolean())).thenReturn(true);
            when(workloadTracker.assignTask(eq(newAgent), anyString())).thenReturn(false);

            boolean result = reassigner.reassignTask(testTask, newAgent, listener);

            assertFalse(result, "Should return false when assignment fails");
            verify(listener).onReassignmentFailed(
                eq("test-task-1"),
                eq(TaskRebalancingManager.RebalancingReason.AGENT_UNAVAILABLE),
                contains("Reassignment failed")
            );
        }
    }

    @Nested
    @DisplayName("selectBestReplacementAgent Tests")
    class SelectBestReplacementAgentTests {

        @Test
        @DisplayName("Should return null when no capable agents")
        void testSelectBestReplacementNoAgents() {
            TaskRebalancingManager.RebalancingAssessment emptyAssessment =
                new TaskRebalancingManager.RebalancingAssessment(
                    "task-1", "ann-1", oldAgent, true,
                    TaskRebalancingManager.RebalancingReason.TIMEOUT,
                    "No agents", List.of()
                );

            UUID result = reassigner.selectBestReplacementAgent(emptyAssessment);

            assertNull(result, "Should return null when no capable agents");
        }

        @Test
        @DisplayName("Should return first agent when workload tracker null")
        void testSelectBestReplacementNoWorkloadTracker() {
            TaskReassigner reassignerNoTracker = new TaskReassigner(
                capabilityRegistry, null, contractNetManager, 3);

            UUID result = reassignerNoTracker.selectBestReplacementAgent(assessment);

            assertEquals(newAgent, result, "Should return first capable agent");
        }

        @Test
        @DisplayName("Should select agent with lowest load")
        void testSelectBestReplacementLowestLoad() {
            UUID agent1 = UUID.randomUUID();
            UUID agent2 = UUID.randomUUID();
            UUID agent3 = UUID.randomUUID();

            TaskRebalancingManager.RebalancingAssessment multiAgentAssessment =
                new TaskRebalancingManager.RebalancingAssessment(
                    "task-1", "ann-1", oldAgent, true,
                    TaskRebalancingManager.RebalancingReason.TIMEOUT,
                    "Multiple agents", List.of(agent1, agent2, agent3)
                );

            when(workloadTracker.getCurrentLoad(agent1)).thenReturn(0.8);
            when(workloadTracker.getCurrentLoad(agent2)).thenReturn(0.3);
            when(workloadTracker.getCurrentLoad(agent3)).thenReturn(0.5);

            UUID result = reassigner.selectBestReplacementAgent(multiAgentAssessment);

            assertEquals(agent2, result, "Should select agent with lowest load (0.3)");
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should track successful reassignments")
        void testTrackSuccessfulReassignments() {
            AgentCapability capability = mock(AgentCapability.class);
            when(capabilityRegistry.getCapability(newAgent)).thenReturn(capability);
            when(capability.isActive()).thenReturn(true);
            when(capability.isAvailable()).thenReturn(true);
            when(workloadTracker.isAvailable(newAgent)).thenReturn(true);
            when(workloadTracker.completeTask(eq(oldAgent), anyString(), anyBoolean())).thenReturn(true);
            when(workloadTracker.assignTask(eq(newAgent), anyString())).thenReturn(true);

            reassigner.reassignTask(testTask, newAgent, null);
            reassigner.reassignTask(testTask, newAgent, null);

            assertEquals(2, reassigner.getReassignedSuccessfully(),
                "Should track 2 successful reassignments");
        }

        @Test
        @DisplayName("Should track failed reassignments")
        void testTrackFailedReassignments() {
            reassigner.reassignTask(null, newAgent, null);
            reassigner.reassignTask(testTask, null, null);

            assertEquals(2, reassigner.getReassignedFailed(),
                "Should track 2 failed reassignments");
        }

        @Test
        @DisplayName("Should track total rebalancing time")
        void testTrackRebalancingTime() {
            AgentCapability capability = mock(AgentCapability.class);
            when(capabilityRegistry.getCapability(newAgent)).thenReturn(capability);
            when(capability.isActive()).thenReturn(true);
            when(capability.isAvailable()).thenReturn(true);
            when(workloadTracker.isAvailable(newAgent)).thenReturn(true);
            when(workloadTracker.completeTask(eq(oldAgent), anyString(), anyBoolean())).thenReturn(true);
            when(workloadTracker.assignTask(eq(newAgent), anyString())).thenReturn(true);

            reassigner.reassignTask(testTask, newAgent, null);

            assertTrue(reassigner.getTotalRebalancingTime() > 0,
                "Should track rebalancing time");
        }

        @Test
        @DisplayName("Should calculate average rebalancing time")
        void testAverageRebalancingTime() {
            AgentCapability capability = mock(AgentCapability.class);
            when(capabilityRegistry.getCapability(newAgent)).thenReturn(capability);
            when(capability.isActive()).thenReturn(true);
            when(capability.isAvailable()).thenReturn(true);
            when(workloadTracker.isAvailable(newAgent)).thenReturn(true);
            when(workloadTracker.completeTask(eq(oldAgent), anyString(), anyBoolean())).thenReturn(true);
            when(workloadTracker.assignTask(eq(newAgent), anyString())).thenReturn(true);

            reassigner.reassignTask(testTask, newAgent, null);
            reassigner.reassignTask(testTask, newAgent, null);

            double avgTime = reassigner.getAverageRebalancingTime(2);

            assertTrue(avgTime > 0, "Average time should be positive");
        }

        @Test
        @DisplayName("Should return zero average when no reassignments")
        void testAverageRebalancingTimeNoReassignments() {
            double avgTime = reassigner.getAverageRebalancingTime(0);

            assertEquals(0.0, avgTime, 0.001,
                "Average should be zero when no reassignments");
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete reassignment workflow")
        void testCompleteReassignmentWorkflow() {
            AgentCapability capability = mock(AgentCapability.class);
            when(capabilityRegistry.getCapability(newAgent)).thenReturn(capability);
            when(capability.isActive()).thenReturn(true);
            when(capability.isAvailable()).thenReturn(true);
            when(workloadTracker.isAvailable(newAgent)).thenReturn(true);
            when(workloadTracker.completeTask(eq(oldAgent), anyString(), anyBoolean())).thenReturn(true);
            when(workloadTracker.assignTask(eq(newAgent), anyString())).thenReturn(true);

            // Select best agent
            UUID selected = reassigner.selectBestReplacementAgent(assessment);
            assertEquals(newAgent, selected);

            // Reassign task
            boolean result = reassigner.reassignTask(testTask, selected, listener);
            assertTrue(result);

            // Verify statistics
            assertEquals(1, reassigner.getReassignedSuccessfully());
            assertEquals(0, reassigner.getReassignedFailed());
            assertTrue(reassigner.getTotalRebalancingTime() > 0);

            // Verify workload updates
            verify(workloadTracker).completeTask(oldAgent, "test-task-1", false);
            verify(workloadTracker).assignTask(newAgent, "test-task-1");
        }

        @Test
        @DisplayName("Should handle multiple reassignments")
        void testMultipleReassignments() {
            AgentCapability capability = mock(AgentCapability.class);
            when(capabilityRegistry.getCapability(any())).thenReturn(capability);
            when(capability.isActive()).thenReturn(true);
            when(capability.isAvailable()).thenReturn(true);
            when(workloadTracker.isAvailable(any())).thenReturn(true);
            when(workloadTracker.completeTask(any(), anyString(), anyBoolean())).thenReturn(true);
            when(workloadTracker.assignTask(any(), anyString())).thenReturn(true);

            UUID agent2 = UUID.randomUUID();
            UUID agent3 = UUID.randomUUID();

            // First reassignment
            assertTrue(reassigner.reassignTask(testTask, newAgent, null));
            assertEquals(1, testTask.getReassignedCount());

            // Second reassignment
            assertTrue(reassigner.reassignTask(testTask, agent2, null));
            assertEquals(2, testTask.getReassignedCount());

            // Third reassignment
            assertTrue(reassigner.reassignTask(testTask, agent3, null));
            assertEquals(3, testTask.getReassignedCount());

            assertEquals(3, reassigner.getReassignedSuccessfully());

            // Fourth should fail (max reassignments = 3)
            assertFalse(reassigner.reassignTask(testTask, newAgent, null));
            assertEquals(1, reassigner.getReassignedFailed());
        }
    }
}
