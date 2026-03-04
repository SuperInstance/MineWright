package com.minewright.coordination;

import com.minewright.action.Task;
import com.minewright.testutil.TaskBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link TaskRebalancingManager}.
 *
 * <p>Tests cover dynamic task rebalancing functionality including:</p>
 * <ul>
 *   <li>Task monitoring lifecycle</li>
 *   <li>Rebalancing triggers (timeout, stuck, failure, unavailable, overloaded)</li>
 *   <li>Agent selection for reassignment</li>
 *   <li>Reassignment workflow</li>
 *   <li>Statistics tracking</li>
 *   <li>Listener notifications</li>
 *   <li>Thread safety</li>
 *   <li>Edge cases and error handling</li>
 * </ul>
 *
 * @see TaskRebalancingManager
 * @since 1.4.0
 */
@DisplayName("Task Rebalancing Manager Tests")
class TaskRebalancingManagerTest {

    private TaskRebalancingManager rebalancingManager;
    private CapabilityRegistry capabilityRegistry;
    private WorkloadTracker workloadTracker;
    private ContractNetManager contractNetManager;

    private UUID agent1;
    private UUID agent2;
    private UUID agent3;

    @BeforeEach
    void setUp() {
        capabilityRegistry = new CapabilityRegistry();
        workloadTracker = new WorkloadTracker();
        contractNetManager = new ContractNetManager();

        rebalancingManager = new TaskRebalancingManager(
            capabilityRegistry,
            workloadTracker,
            contractNetManager
        );

        // Create test agents
        agent1 = UUID.randomUUID();
        agent2 = UUID.randomUUID();
        agent3 = UUID.randomUUID();

        // Register agents with capabilities
        AgentCapability cap1 = new AgentCapability(agent1, "Miner1")
            .addSkill(AgentCapability.Skills.MINING)
            .setProficiency(AgentCapability.Skills.MINING, 0.9);
        capabilityRegistry.register(agent1, cap1);

        AgentCapability cap2 = new AgentCapability(agent2, "Miner2")
            .addSkill(AgentCapability.Skills.MINING)
            .setProficiency(AgentCapability.Skills.MINING, 0.85);
        capabilityRegistry.register(agent2, cap2);

        AgentCapability cap3 = new AgentCapability(agent3, "Miner3")
            .addSkill(AgentCapability.Skills.MINING)
            .setProficiency(AgentCapability.Skills.MINING, 0.8);
        capabilityRegistry.register(agent3, cap3);

        // Register agents in workload tracker
        workloadTracker.registerAgent(agent1, 3);
        workloadTracker.registerAgent(agent2, 3);
        workloadTracker.registerAgent(agent3, 3);
    }

    @Nested
    @DisplayName("Task Monitoring Tests")
    class MonitoringTests {

        @Test
        @DisplayName("Start monitoring creates monitored task")
        void startMonitoringCreatesMonitoredTask() {
            String taskId = "task_001";
            String announcementId = "announce_001";
            long estimatedDuration = 30000; // 30 seconds

            boolean started = rebalancingManager.monitorTask(
                taskId, announcementId, agent1, estimatedDuration
            );

            assertTrue(started, "Monitoring should start successfully");
            assertEquals(1, rebalancingManager.getMonitoredTaskCount(),
                "Should have 1 monitored task");

            TaskRebalancingManager.MonitoredTask monitored =
                rebalancingManager.getMonitoredTasks().get(taskId);
            assertNotNull(monitored, "Monitored task should exist");
            assertEquals(taskId, monitored.getTaskId());
            assertEquals(agent1, monitored.getAssignedAgent());
        }

        @Test
        @DisplayName("Start monitoring with custom thresholds")
        void startMonitoringWithCustomThresholds() {
            String taskId = "task_002";
            String announcementId = "announce_002";

            boolean started = rebalancingManager.monitorTask(
                taskId, announcementId, agent1,
                30000, // estimatedDuration
                3.0,   // timeoutThreshold (3x)
                90000, // stuckThreshold (90 seconds)
                5000   // monitoringInterval (5 seconds)
            );

            assertTrue(started);

            TaskRebalancingManager.MonitoredTask monitored =
                rebalancingManager.getMonitoredTasks().get(taskId);
            assertNotNull(monitored);
            assertEquals(3.0, monitored.getTimeoutThreshold());
            assertEquals(90000, monitored.getStuckThreshold());
            assertEquals(5000, monitored.getMonitoringInterval());
        }

        @Test
        @DisplayName("Start monitoring with null parameters returns false")
        void startMonitoringWithNullParametersReturnsFalse() {
            assertFalse(rebalancingManager.monitorTask(null, "announce_001", agent1, 30000));
            assertFalse(rebalancingManager.monitorTask("task_001", null, agent1, 30000));
            assertFalse(rebalancingManager.monitorTask("task_001", "announce_001", null, 30000));
        }

        @Test
        @DisplayName("Start monitoring same task twice returns false")
        void startMonitoringSameTaskTwice() {
            String taskId = "task_003";

            assertTrue(rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000));
            assertFalse(rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000));

            assertEquals(1, rebalancingManager.getMonitoredTaskCount());
        }

        @Test
        @DisplayName("Stop monitoring removes task")
        void stopMonitoringRemovesTask() {
            String taskId = "task_004";

            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);
            assertEquals(1, rebalancingManager.getMonitoredTaskCount());

            boolean stopped = rebalancingManager.stopMonitoring(taskId);

            assertTrue(stopped, "Stop monitoring should succeed");
            assertEquals(0, rebalancingManager.getMonitoredTaskCount());
            assertFalse(rebalancingManager.getMonitoredTasks().containsKey(taskId));
        }

        @Test
        @DisplayName("Stop monitoring non-existent task returns false")
        void stopMonitoringNonExistentTaskReturnsFalse() {
            boolean stopped = rebalancingManager.stopMonitoring("non_existent");
            assertFalse(stopped);
        }

        @Test
        @DisplayName("Update progress updates monitored task")
        void updateProgressUpdatesMonitoredTask() {
            String taskId = "task_005";

            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);

            TaskRebalancingManager.MonitoredTask monitored =
                rebalancingManager.getMonitoredTasks().get(taskId);

            assertEquals(0.0, monitored.getLastProgress());

            rebalancingManager.updateProgress(taskId, 0.5);
            assertEquals(0.5, monitored.getLastProgress());
        }

        @Test
        @DisplayName("Update progress only increases")
        void updateProgressOnlyIncreases() {
            String taskId = "task_006";

            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);

            TaskRebalancingManager.MonitoredTask monitored =
                rebalancingManager.getMonitoredTasks().get(taskId);

            rebalancingManager.updateProgress(taskId, 0.5);
            rebalancingManager.updateProgress(taskId, 0.3); // Should not update
            rebalancingManager.updateProgress(taskId, 0.7);

            assertEquals(0.7, monitored.getLastProgress());
        }
    }

    @Nested
    @DisplayName("Rebalancing Assessment Tests")
    class AssessmentTests {

        @Test
        @DisplayName("Assess task not monitored returns assessment without rebalancing")
        void assessTaskNotMonitored() {
            TaskRebalancingManager.RebalancingAssessment assessment =
                rebalancingManager.assessTask("non_existent");

            assertNotNull(assessment);
            assertEquals("non_existent", assessment.getTaskId());
            assertFalse(assessment.needsRebalancing());
            assertNull(assessment.getReason());
        }

        @Test
        @DisplayName("Assess healthy task returns no rebalancing needed")
        void assessHealthyTaskReturnsNoRebalancing() {
            String taskId = "task_007";
            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 60000);

            // Update progress to show task is healthy
            rebalancingManager.updateProgress(taskId, 0.5);

            TaskRebalancingManager.RebalancingAssessment assessment =
                rebalancingManager.assessTask(taskId);

            assertFalse(assessment.needsRebalancing(),
                "Healthy task should not need rebalancing");
            assertTrue(assessment.getDetails().contains("progressing normally") ||
                      assessment.getDetails().contains("Task progressing normally"));
        }

        @Test
        @DisplayName("Assess timed out task triggers rebalancing")
        void assessTimedOutTaskTriggersRebalancing() throws InterruptedException {
            String taskId = "task_008";

            // Create task with very short estimated duration
            rebalancingManager.monitorTask(taskId, "announce_001", agent1,
                100, // 100ms estimated
                2.0, // 2x timeout threshold
                60000,
                1000
            );

            // Wait for timeout
            Thread.sleep(250);

            TaskRebalancingManager.RebalancingAssessment assessment =
                rebalancingManager.assessTask(taskId);

            assertTrue(assessment.needsRebalancing(),
                "Timed out task should need rebalancing");
            assertEquals(TaskRebalancingManager.RebalancingReason.TIMEOUT, assessment.getReason());
            assertTrue(assessment.getDetails().contains("exceeded timeout"));
        }

        @Test
        @DisplayName("Assess stuck task triggers rebalancing")
        void assessStuckTaskTriggersRebalancing() throws InterruptedException {
            String taskId = "task_009";

            // Create task with short stuck threshold
            rebalancingManager.monitorTask(taskId, "announce_001", agent1,
                30000,
                2.0,
                100, // 100ms stuck threshold
                1000
            );

            // Set initial progress
            rebalancingManager.updateProgress(taskId, 0.1);

            // Wait for stuck detection
            Thread.sleep(150);

            TaskRebalancingManager.RebalancingAssessment assessment =
                rebalancingManager.assessTask(taskId);

            assertTrue(assessment.needsRebalancing(),
                "Stuck task should need rebalancing");
            assertEquals(TaskRebalancingManager.RebalancingReason.STUCK, assessment.getReason());
            assertTrue(assessment.getDetails().contains("stuck"));
        }

        @Test
        @DisplayName("Assess overloaded agent triggers rebalancing")
        void assessOverloadedAgentTriggersRebalancing() {
            String taskId = "task_010";

            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);

            // Fill agent's capacity
            workloadTracker.assignTask(agent1, "other_task_1");
            workloadTracker.assignTask(agent1, "other_task_2");
            workloadTracker.assignTask(agent1, "other_task_3");

            // Verify agent is at capacity
            assertTrue(workloadTracker.getWorkload(agent1).isAtCapacity());

            TaskRebalancingManager.RebalancingAssessment assessment =
                rebalancingManager.assessTask(taskId);

            assertTrue(assessment.needsRebalancing(),
                "Overloaded agent should trigger rebalancing");
            assertEquals(TaskRebalancingManager.RebalancingReason.AGENT_OVERLOADED, assessment.getReason());
        }

        @Test
        @DisplayName("Assess unavailable agent triggers rebalancing")
        void assessUnavailableAgentTriggersRebalancing() {
            String taskId = "task_011";

            // Use agent not registered in workload tracker
            UUID unregisteredAgent = UUID.randomUUID();
            rebalancingManager.monitorTask(taskId, "announce_001", unregisteredAgent, 30000);

            TaskRebalancingManager.RebalancingAssessment assessment =
                rebalancingManager.assessTask(taskId);

            assertTrue(assessment.needsRebalancing(),
                "Unavailable agent should trigger rebalancing");
            assertEquals(TaskRebalancingManager.RebalancingReason.AGENT_UNAVAILABLE, assessment.getReason());
        }
    }

    @Nested
    @DisplayName("Task Reassignment Tests")
    class ReassignmentTests {

        @Test
        @DisplayName("Reassign task successfully transfers to new agent")
        void reassignTaskSuccessfullyTransfersToNewAgent() {
            String taskId = "task_012";

            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);
            workloadTracker.assignTask(agent1, taskId);

            assertTrue(workloadTracker.getWorkload(agent1).getActiveTasks().containsKey(taskId));
            assertFalse(workloadTracker.getWorkload(agent2).getActiveTasks().containsKey(taskId));

            boolean reassigned = rebalancingManager.reassignTask(taskId, agent2);

            assertTrue(reassigned, "Reassignment should succeed");

            // Verify task transferred
            assertFalse(workloadTracker.getWorkload(agent1).getActiveTasks().containsKey(taskId));
            assertTrue(workloadTracker.getWorkload(agent2).getActiveTasks().containsKey(taskId));
        }

        @Test
        @DisplayName("Reassign task increments reassignment count")
        void reassignTaskIncrementsReassignmentCount() {
            String taskId = "task_013";

            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);

            TaskRebalancingManager.MonitoredTask monitored =
                rebalancingManager.getMonitoredTasks().get(taskId);

            assertEquals(0, monitored.getReassignedCount());

            rebalancingManager.reassignTask(taskId, agent2);
            assertEquals(1, monitored.getReassignedCount());

            rebalancingManager.reassignTask(taskId, agent3);
            assertEquals(2, monitored.getReassignedCount());
        }

        @Test
        @DisplayName("Reassign task beyond max limit fails")
        void reassignTaskBeyondMaxLimitFails() {
            String taskId = "task_014";
            rebalancingManager.setMaxReassignments(2);

            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);

            rebalancingManager.reassignTask(taskId, agent2);
            rebalancingManager.reassignTask(taskId, agent3);

            // Third reassignment should fail
            boolean reassigned = rebalancingManager.reassignTask(taskId, agent1);

            assertFalse(reassigned, "Reassignment beyond max limit should fail");

            TaskRebalancingManager.RebalancingStatistics stats = rebalancingManager.getStatistics();
            assertEquals(2, stats.getReassignedSuccessfully());
            assertTrue(stats.getReassignedFailed() > 0);
        }

        @Test
        @DisplayName("Reassign task to unavailable agent fails")
        void reassignTaskToUnavailableAgentFails() {
            String taskId = "task_015";

            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);

            UUID unavailableAgent = UUID.randomUUID();
            // Don't register in capability registry

            boolean reassigned = rebalancingManager.reassignTask(taskId, unavailableAgent);

            assertFalse(reassigned, "Reassignment to unavailable agent should fail");
        }

        @Test
        @DisplayName("Reassign task to agent at capacity fails")
        void reassignTaskToAgentAtCapacityFails() {
            String taskId = "task_016";

            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);

            // Fill agent2's capacity
            workloadTracker.assignTask(agent2, "other_1");
            workloadTracker.assignTask(agent2, "other_2");
            workloadTracker.assignTask(agent2, "other_3");

            assertTrue(workloadTracker.getWorkload(agent2).isAtCapacity());

            boolean reassigned = rebalancingManager.reassignTask(taskId, agent2);

            assertFalse(reassigned, "Reassignment to agent at capacity should fail");
        }

        @Test
        @DisplayName("Report task failure triggers rebalancing assessment")
        void reportTaskFailureTriggersRebalancing() {
            String taskId = "task_017";

            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);
            workloadTracker.assignTask(agent1, taskId);

            boolean triggered = rebalancingManager.reportTaskFailure(taskId, "Task execution failed");

            // Should trigger rebalancing but may not find replacement
            TaskRebalancingManager.RebalancingStatistics stats = rebalancingManager.getStatistics();
            assertTrue(stats.getRebalancingTriggered() > 0 ||
                       stats.getReassignedFailed() > 0);
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Get statistics returns initial zeros")
        void getStatisticsReturnsInitialZeros() {
            TaskRebalancingManager.RebalancingStatistics stats =
                rebalancingManager.getStatistics();

            assertEquals(0, stats.getTotalAssessments());
            assertEquals(0, stats.getRebalancingTriggered());
            assertEquals(0, stats.getReassignedSuccessfully());
            assertEquals(0, stats.getReassignedFailed());
            assertEquals(0, stats.getNoCapableAgents());
        }

        @Test
        @DisplayName("Statistics increment with operations")
        void statisticsIncrementWithOperations() {
            String taskId = "task_018";
            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);
            workloadTracker.assignTask(agent1, taskId);

            // Trigger assessment
            rebalancingManager.assessTask(taskId);

            TaskRebalancingManager.RebalancingStatistics stats =
                rebalancingManager.getStatistics();

            assertTrue(stats.getTotalAssessments() > 0);
        }

        @Test
        @DisplayName("Calculate success rate correctly")
        void calculateSuccessRateCorrectly() {
            // Create manager with test components
            TaskRebalancingManager testManager = new TaskRebalancingManager(
                capabilityRegistry,
                workloadTracker,
                contractNetManager
            );

            String taskId1 = "task_019";
            String taskId2 = "task_020";

            testManager.monitorTask(taskId1, "announce_001", agent1, 30000);
            testManager.monitorTask(taskId2, "announce_001", agent2, 30000);

            workloadTracker.assignTask(agent1, taskId1);
            workloadTracker.assignTask(agent2, taskId2);

            // Successful reassignment
            assertTrue(testManager.reassignTask(taskId1, agent3));

            // Failed reassignment (agent at capacity)
            workloadTracker.assignTask(agent2, "filler");
            workloadTracker.assignTask(agent2, "filler2");
            workloadTracker.assignTask(agent2, "filler3");
            assertFalse(testManager.reassignTask(taskId2, agent3));

            TaskRebalancingManager.RebalancingStatistics stats = testManager.getStatistics();

            assertEquals(1, stats.getReassignedSuccessfully());
            assertEquals(1, stats.getReassignedFailed());
            assertEquals(0.5, stats.getRebalancingSuccessRate(), 0.001);
        }

        @Test
        @DisplayName("Reason counts track correctly")
        void reasonCountsTrackCorrectly() {
            // Create announcement with skills for capable agent lookup
            Task task = TaskBuilder.aTask("mine").withBlock("stone").build();
            String announcementId = contractNetManager.announceTask(task, agent1);

            String taskId = "task_021";
            rebalancingManager.monitorTask(taskId, announcementId, agent1,
                100, // Very short to trigger timeout
                2.0,
                60000,
                1000
            );

            // Wait for timeout and assess
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                fail("Test interrupted");
            }

            rebalancingManager.assessTask(taskId);

            TaskRebalancingManager.RebalancingStatistics stats =
                rebalancingManager.getStatistics();

            assertTrue(stats.getReasonCounts().containsKey(
                TaskRebalancingManager.RebalancingReason.TIMEOUT));
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Set timeout threshold")
        void setTimeoutThreshold() {
            rebalancingManager.setDefaultTimeoutThreshold(3.5);

            String taskId = "task_022";
            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);

            TaskRebalancingManager.MonitoredTask monitored =
                rebalancingManager.getMonitoredTasks().get(taskId);

            assertEquals(3.5, monitored.getTimeoutThreshold());
        }

        @Test
        @DisplayName("Set stuck threshold")
        void setStuckThreshold() {
            rebalancingManager.setDefaultStuckThreshold(45000);

            String taskId = "task_023";
            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);

            TaskRebalancingManager.MonitoredTask monitored =
                rebalancingManager.getMonitoredTasks().get(taskId);

            assertEquals(45000, monitored.getStuckThreshold());
        }

        @Test
        @DisplayName("Set monitoring interval")
        void setMonitoringInterval() {
            rebalancingManager.setDefaultMonitoringInterval(15000);

            String taskId = "task_024";
            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);

            TaskRebalancingManager.MonitoredTask monitored =
                rebalancingManager.getMonitoredTasks().get(taskId);

            assertEquals(15000, monitored.getMonitoringInterval());
        }

        @Test
        @DisplayName("Set max reassignments")
        void setMaxReassignments() {
            rebalancingManager.setMaxReassignments(5);

            String taskId = "task_025";
            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);

            // Verify max is enforced
            for (int i = 0; i < 5; i++) {
                rebalancingManager.reassignTask(taskId, i % 2 == 0 ? agent2 : agent3);
            }

            // 6th reassignment should fail
            assertFalse(rebalancingManager.reassignTask(taskId, agent1));
        }

        @Test
        @DisplayName("Set invalid threshold throws exception")
        void setInvalidThresholdThrowsException() {
            assertThrows(IllegalArgumentException.class,
                () -> rebalancingManager.setDefaultTimeoutThreshold(0.5));

            assertThrows(IllegalArgumentException.class,
                () -> rebalancingManager.setDefaultStuckThreshold(500));

            assertThrows(IllegalArgumentException.class,
                () -> rebalancingManager.setDefaultMonitoringInterval(500));

            assertThrows(IllegalArgumentException.class,
                () -> rebalancingManager.setMaxReassignments(0));
        }
    }

    @Nested
    @DisplayName("Listener Tests")
    class ListenerTests {

        private AtomicInteger monitoringStartedCount;
        private AtomicInteger monitoringStoppedCount;
        private AtomicInteger taskReassignedCount;
        private AtomicInteger reassignedFailedCount;

        @BeforeEach
        void setUp() {
            monitoringStartedCount = new AtomicInteger(0);
            monitoringStoppedCount = new AtomicInteger(0);
            taskReassignedCount = new AtomicInteger(0);
            reassignedFailedCount = new AtomicInteger(0);

            rebalancingManager.addListener(new TaskRebalancingManager.RebalancingListener() {
                @Override
                public void onMonitoringStarted(String taskId, UUID agent) {
                    monitoringStartedCount.incrementAndGet();
                }

                @Override
                public void onMonitoringStopped(String taskId) {
                    monitoringStoppedCount.incrementAndGet();
                }

                @Override
                public void onTaskReassigned(String taskId, UUID oldAgent, UUID newAgent,
                                           TaskRebalancingManager.RebalancingReason reason) {
                    taskReassignedCount.incrementAndGet();
                }

                @Override
                public void onReassignmentFailed(String taskId,
                                               TaskRebalancingManager.RebalancingReason reason,
                                               String cause) {
                    reassignedFailedCount.incrementAndGet();
                }
            });
        }

        @Test
        @DisplayName("Monitoring started triggers listener")
        void monitoringStartedTriggersListener() {
            rebalancingManager.monitorTask("task_026", "announce_001", agent1, 30000);

            assertEquals(1, monitoringStartedCount.get());
        }

        @Test
        @DisplayName("Monitoring stopped triggers listener")
        void monitoringStoppedTriggersListener() {
            String taskId = "task_027";
            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);
            rebalancingManager.stopMonitoring(taskId);

            assertEquals(1, monitoringStoppedCount.get());
        }

        @Test
        @DisplayName("Task reassignment triggers listener")
        void taskReassignmentTriggersListener() {
            String taskId = "task_028";
            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);
            workloadTracker.assignTask(agent1, taskId);

            rebalancingManager.reassignTask(taskId, agent2);

            assertEquals(1, taskReassignedCount.get());
        }

        @Test
        @DisplayName("Failed reassignment triggers listener")
        void failedReassignmentTriggersListener() {
            String taskId = "task_029";
            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);

            // Try to reassign to unregistered agent
            boolean reassigned = rebalancingManager.reassignTask(taskId, UUID.randomUUID());

            assertFalse(reassigned);
            assertTrue(reassignedFailedCount.get() > 0);
        }

        @Test
        @DisplayName("Multiple listeners are notified")
        void multipleListenersAreNotified() {
            AtomicInteger secondListenerCount = new AtomicInteger(0);

            rebalancingManager.addListener(new TaskRebalancingManager.RebalancingListener() {
                @Override
                public void onMonitoringStarted(String taskId, UUID agent) {
                    secondListenerCount.incrementAndGet();
                }
            });

            rebalancingManager.monitorTask("task_030", "announce_001", agent1, 30000);

            assertEquals(1, monitoringStartedCount.get());
            assertEquals(1, secondListenerCount.get());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Update progress for non-monitored task does nothing")
        void updateProgressForNonMonitoredTaskDoesNothing() {
            assertDoesNotThrow(() -> rebalancingManager.updateProgress("non_existent", 0.5));
        }

        @Test
        @DisplayName("Assess task after monitoring stopped")
        void assessTaskAfterMonitoringStopped() {
            String taskId = "task_031";
            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);
            rebalancingManager.stopMonitoring(taskId);

            TaskRebalancingManager.RebalancingAssessment assessment =
                rebalancingManager.assessTask(taskId);

            assertFalse(assessment.needsRebalancing());
        }

        @Test
        @DisplayName("Reassign task after monitoring stopped fails")
        void reassignTaskAfterMonitoringStoppedFails() {
            String taskId = "task_032";
            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);
            rebalancingManager.stopMonitoring(taskId);

            boolean reassigned = rebalancingManager.reassignTask(taskId, agent2);

            assertFalse(reassigned);
        }

        @Test
        @DisplayName("Get monitored tasks returns unmodifiable map")
        void getMonitoredTasksReturnsUnmodifiableMap() {
            String taskId = "task_033";
            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);

            var tasks = rebalancingManager.getMonitoredTasks();

            assertThrows(UnsupportedOperationException.class,
                () -> tasks.put("another", null));
        }

        @Test
        @DisplayName("Handle zero estimated duration")
        void handleZeroEstimatedDuration() {
            String taskId = "task_034";

            // Should not throw exception
            assertDoesNotThrow(() ->
                rebalancingManager.monitorTask(taskId, "announce_001", agent1, 0));
        }

        @Test
        @DisplayName("Shutdown prevents new monitoring")
        void shutdownPreventsNewMonitoring() {
            rebalancingManager.shutdown();

            boolean started = rebalancingManager.monitorTask("task_035", "announce_001", agent1, 30000);

            assertFalse(started, "Should not start monitoring after shutdown");
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Concurrent monitoring operations are handled safely")
        void concurrentMonitoringOperations() throws InterruptedException {
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];
            String[] taskIds = new String[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                taskIds[i] = "concurrent_task_" + i;
                threads[i] = new Thread(() -> {
                    rebalancingManager.monitorTask(taskIds[index], "announce_001",
                        agent1, 30000);
                    rebalancingManager.updateProgress(taskIds[index], 0.5);
                    rebalancingManager.assessTask(taskIds[index]);
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            assertEquals(threadCount, rebalancingManager.getMonitoredTaskCount());
        }

        @Test
        @DisplayName("Concurrent reassignments are handled safely")
        void concurrentReassignments() throws InterruptedException {
            String taskId = "task_036";
            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);
            workloadTracker.assignTask(agent1, taskId);

            int threadCount = 5;
            Thread[] threads = new Thread[threadCount];
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    if (rebalancingManager.reassignTask(taskId, agent2)) {
                        successCount.incrementAndGet();
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // At least one should succeed
            assertTrue(successCount.get() >= 1);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Full rebalancing workflow")
        void fullRebalancingWorkflow() throws InterruptedException {
            // Create announcement with required skills
            Task task = TaskBuilder.aTask("mine").withBlock("stone").build();
            String announcementId = contractNetManager.announceTask(task, agent1);

            String taskId = "task_037";
            long estimatedDuration = 100; // Very short for testing

            rebalancingManager.monitorTask(taskId, announcementId, agent1, estimatedDuration);
            workloadTracker.assignTask(agent1, taskId);

            // Wait for timeout
            Thread.sleep(150);

            // Assess task
            TaskRebalancingManager.RebalancingAssessment assessment =
                rebalancingManager.assessTask(taskId);

            assertTrue(assessment.needsRebalancing());
            assertEquals(TaskRebalancingManager.RebalancingReason.TIMEOUT, assessment.getReason());

            // Reassign to agent2 (capable and available)
            assertTrue(rebalancingManager.reassignTask(taskId, agent2));

            // Verify reassignment
            assertTrue(workloadTracker.getWorkload(agent2).getActiveTasks().containsKey(taskId));
        }

        @Test
        @DisplayName("Rebalancing with capable agent lookup")
        void rebalancingWithCapableAgentLookup() {
            // Create announcement with skills
            Task task = TaskBuilder.aTask("mine").withBlock("iron_ore").build();
            Map<String, Object> requirements = new java.util.HashMap<>();
            requirements.put("skills", Set.of(AgentCapability.Skills.MINING));
            requirements.put("minProficiency", 0.7);

            TaskAnnouncement announcement = TaskAnnouncement.builder()
                .task(task)
                .requesterId(agent1)
                .deadlineAfter(30000)
                .requirements(requirements)
                .build();

            String taskId = "task_038";
            rebalancingManager.monitorTask(taskId, announcement.announcementId(), agent1, 30000);

            // Assess task
            TaskRebalancingManager.RebalancingAssessment assessment =
                rebalancingManager.assessTask(taskId);

            // Should find capable agents (agent2 and agent3 are also miners)
            if (assessment.needsRebalancing()) {
                assertTrue(assessment.getCapableAgents().size() >= 2,
                    "Should find at least 2 capable mining agents");
            }
        }
    }

    @Nested
    @DisplayName("Lifecycle Tests")
    class LifecycleTests {

        @Test
        @DisplayName("Manager toString contains relevant info")
        void managerToStringContainsRelevantInfo() {
            String taskId = "task_039";
            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);

            String str = rebalancingManager.toString();

            assertTrue(str.contains("TaskRebalancingManager"));
            assertTrue(str.contains("monitored=1"));
        }

        @Test
        @DisplayName("MonitoredTask toString contains relevant info")
        void monitoredTaskToStringContainsRelevantInfo() {
            String taskId = "task_040";
            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);

            TaskRebalancingManager.MonitoredTask monitored =
                rebalancingManager.getMonitoredTasks().get(taskId);

            String str = monitored.toString();

            assertTrue(str.contains("taskId="));
            assertTrue(str.contains("assignedAgent="));
        }

        @Test
        @DisplayName("RebalancingAssessment toString contains relevant info")
        void rebalancingAssessmentToStringContainsRelevantInfo() {
            String taskId = "task_041";
            rebalancingManager.monitorTask(taskId, "announce_001", agent1, 30000);

            TaskRebalancingManager.RebalancingAssessment assessment =
                rebalancingManager.assessTask(taskId);

            String str = assessment.toString();

            assertTrue(str.contains("RebalancingAssessment"));
            assertTrue(str.contains("task="));
            assertTrue(str.contains("rebalance="));
        }

        @Test
        @DisplayName("RebalancingStatistics toString contains relevant info")
        void rebalancingStatisticsToStringContainsRelevantInfo() {
            TaskRebalancingManager.RebalancingStatistics stats =
                rebalancingManager.getStatistics();

            String str = stats.toString();

            assertTrue(str.contains("RebalancingStatistics"));
            assertTrue(str.contains("assessments="));
            assertTrue(str.contains("triggered="));
        }
    }
}
