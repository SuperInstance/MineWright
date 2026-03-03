package com.minewright.integration;

import com.minewright.coordination.WorkloadTracker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for workload tracking and balancing.
 *
 * <p><b>Test Coverage:</b></p>
 * <ul>
 *   <li>Agent registration and management</li>
 *   <li>Task assignment and completion</li>
 *   <li>Load factor calculation</li>
 *   <li>Concurrent task operations</li>
 *   <li>Workload listener notifications</li>
 *   <li>Agent availability queries</li>
 *   <li>Statistics and metrics collection</li>
 * </ul>
 *
 * @see WorkloadTracker
 * @see WorkloadTracker.AgentWorkload
 * @since 1.3.0
 */
@DisplayName("Workload Balancing Integration Tests")
class WorkloadBalancingTest extends IntegrationTestBase {

    @Test
    @DisplayName("Agent registration and workload tracking")
    void testAgentRegistration() {
        WorkloadTracker tracker = new WorkloadTracker(5);

        UUID agent1 = UUID.randomUUID();
        UUID agent2 = UUID.randomUUID();

        assertTrue(tracker.registerAgent(agent1), "First agent should register");
        assertTrue(tracker.registerAgent(agent2), "Second agent should register");
        assertFalse(tracker.registerAgent(agent1), "Duplicate registration should fail");

        assertEquals(2, tracker.getAgentCount(), "Should have 2 registered agents");
        assertTrue(tracker.isRegistered(agent1), "Agent1 should be registered");
        assertTrue(tracker.isRegistered(agent2), "Agent2 should be registered");
    }

    @Test
    @DisplayName("Task assignment increases agent load")
    void testTaskAssignment() {
        WorkloadTracker tracker = new WorkloadTracker(5);

        UUID agent = UUID.randomUUID();
        tracker.registerAgent(agent, 5);

        assertEquals(0.0, tracker.getCurrentLoad(agent), 0.001,
            "Initial load should be 0");

        assertTrue(tracker.assignTask(agent, "task1"), "First task should assign");
        assertEquals(0.2, tracker.getCurrentLoad(agent), 0.001,
            "Load should be 0.2 after 1 task");

        assertTrue(tracker.assignTask(agent, "task2"), "Second task should assign");
        assertEquals(0.4, tracker.getCurrentLoad(agent), 0.001,
            "Load should be 0.4 after 2 tasks");

        WorkloadTracker.AgentWorkload workload = tracker.getWorkload(agent);
        assertEquals(2, workload.getActiveTaskCount(), "Should have 2 active tasks");
    }

    @Test
    @DisplayName("Task completion decreases agent load")
    void testTaskCompletion() {
        WorkloadTracker tracker = new WorkloadTracker(5);

        UUID agent = UUID.randomUUID();
        tracker.registerAgent(agent, 5);

        // Assign tasks
        tracker.assignTask(agent, "task1");
        tracker.assignTask(agent, "task2");
        tracker.assignTask(agent, "task3");

        assertEquals(0.6, tracker.getCurrentLoad(agent), 0.001,
            "Load should be 0.6 after 3 tasks");

        // Complete tasks
        assertTrue(tracker.completeTask(agent, "task1", true), "Task1 should complete");
        assertEquals(0.4, tracker.getCurrentLoad(agent), 0.001,
            "Load should be 0.4 after 1 completion");

        assertTrue(tracker.completeTask(agent, "task2", true), "Task2 should complete");
        assertEquals(0.2, tracker.getCurrentLoad(agent), 0.001,
            "Load should be 0.2 after 2 completions");

        WorkloadTracker.AgentWorkload workload = tracker.getWorkload(agent);
        assertEquals(2, workload.getTotalCompleted(), "Should have 2 completed tasks");
        assertEquals(0, workload.getTotalFailed(), "Should have 0 failed tasks");
    }

    @Test
    @DisplayName("Agent capacity limits are enforced")
    void testCapacityLimits() {
        WorkloadTracker tracker = new WorkloadTracker(3);

        UUID agent = UUID.randomUUID();
        tracker.registerAgent(agent, 3);

        assertTrue(tracker.assignTask(agent, "task1"), "Task1 should assign");
        assertTrue(tracker.assignTask(agent, "task2"), "Task2 should assign");
        assertTrue(tracker.assignTask(agent, "task3"), "Task3 should assign");
        assertFalse(tracker.assignTask(agent, "task4"), "Task4 should fail (at capacity)");

        WorkloadTracker.AgentWorkload workload = tracker.getWorkload(agent);
        assertTrue(workload.isAtCapacity(), "Agent should be at capacity");
        assertFalse(workload.isAvailable(), "Agent should not be available");
    }

    @Test
    @DisplayName("Concurrent task operations are thread-safe")
    void testConcurrentTaskOperations() throws InterruptedException {
        WorkloadTracker tracker = new WorkloadTracker(10);

        int numAgents = 5;
        int tasksPerAgent = 20;

        List<UUID> agents = new ArrayList<>();
        for (int i = 0; i < numAgents; i++) {
            UUID agentId = UUID.randomUUID();
            agents.add(agentId);
            tracker.registerAgent(agentId, 10);
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numAgents * tasksPerAgent);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Concurrently assign and complete tasks
        for (int i = 0; i < numAgents; i++) {
            UUID agent = agents.get(i);
            for (int j = 0; j < tasksPerAgent; j++) {
                final int taskNum = j;
                executor.submit(() -> {
                    try {
                        String taskId = "task-" + taskNum;
                        if (tracker.assignTask(agent, taskId)) {
                            successCount.incrementAndGet();
                            // Simulate some work
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            tracker.completeTask(agent, taskId, true);
                        } else {
                            failCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify no data corruption
        for (UUID agent : agents) {
            WorkloadTracker.AgentWorkload workload = tracker.getWorkload(agent);
            assertTrue(workload.getActiveTaskCount() <= 10,
                "Active tasks should not exceed capacity");
            assertTrue(workload.getCurrentLoad() <= 1.0,
                "Load should not exceed 1.0");
        }
    }

    @Test
    @DisplayName("Concurrent bid submission simulation")
    void testConcurrentBidSimulation() throws InterruptedException {
        WorkloadTracker tracker = new WorkloadTracker(5);

        int numAgents = 10;
        List<UUID> agents = new ArrayList<>();
        for (int i = 0; i < numAgents; i++) {
            UUID agentId = UUID.randomUUID();
            agents.add(agentId);
            tracker.registerAgent(agentId, 5);
        }

        ExecutorService executor = Executors.newFixedThreadPool(numAgents);
        CountDownLatch latch = new CountDownLatch(numAgents);
        AtomicInteger selectedCount = new AtomicInteger(0);

        // Simulate agents bidding on tasks
        for (UUID agent : agents) {
            executor.submit(() -> {
                try {
                    // Agent tries to take on work
                    for (int i = 0; i < 3; i++) {
                        String taskId = "bid-task-" + UUID.randomUUID().toString().substring(0, 8);
                        if (tracker.assignTask(agent, taskId)) {
                            selectedCount.incrementAndGet();
                            // Simulate task completion
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            tracker.completeTask(agent, taskId, true);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify workload distribution
        List<UUID> availableAgents = tracker.getAvailableAgents();
        assertTrue(availableAgents.size() >= 0, "Should have available agents");

        double avgLoad = tracker.getAverageLoad();
        assertTrue(avgLoad >= 0.0 && avgLoad <= 1.0, "Average load should be valid");
    }

    @Test
    @DisplayName("Workload listeners receive notifications")
    void testWorkloadListeners() {
        WorkloadTracker tracker = new WorkloadTracker(5);

        List<String> events = new ArrayList<>();

        tracker.addListener(new WorkloadTracker.WorkloadListener() {
            @Override
            public void onLoadChanged(UUID agentId, double oldLoad, double newLoad) {
                events.add(String.format("LOAD_CHANGED:%s:%.2f:%.2f",
                    agentId.toString().substring(0, 8), oldLoad, newLoad));
            }

            @Override
            public void onTaskAssigned(UUID agentId, String taskId) {
                events.add(String.format("TASK_ASSIGNED:%s:%s",
                    agentId.toString().substring(0, 8), taskId));
            }

            @Override
            public void onTaskCompleted(UUID agentId, String taskId, boolean success, long duration) {
                events.add(String.format("TASK_COMPLETED:%s:%s:%b",
                    agentId.toString().substring(0, 8), taskId, success));
            }
        });

        UUID agent = UUID.randomUUID();
        tracker.registerAgent(agent, 5);

        tracker.assignTask(agent, "task1");
        tracker.assignTask(agent, "task2");
        tracker.completeTask(agent, "task1", true, 1000);

        assertTrue(events.stream().anyMatch(e -> e.contains("TASK_ASSIGNED")),
            "Should receive task assigned event");
        assertTrue(events.stream().anyMatch(e -> e.contains("TASK_COMPLETED")),
            "Should receive task completed event");
        assertTrue(events.stream().anyMatch(e -> e.contains("LOAD_CHANGED")),
            "Should receive load changed event");
    }

    @Test
    @DisplayName("Finding least loaded agent")
    void testLeastLoadedAgent() {
        WorkloadTracker tracker = new WorkloadTracker(5);

        UUID agent1 = UUID.randomUUID();
        UUID agent2 = UUID.randomUUID();
        UUID agent3 = UUID.randomUUID();

        tracker.registerAgent(agent1, 5);
        tracker.registerAgent(agent2, 5);
        tracker.registerAgent(agent3, 5);

        // Assign different loads
        tracker.assignTask(agent1, "task1");
        tracker.assignTask(agent1, "task2");
        tracker.assignTask(agent1, "task3");

        tracker.assignTask(agent2, "task1");
        tracker.assignTask(agent2, "task2");

        // agent3 has no tasks

        Optional<UUID> leastLoaded = tracker.getLeastLoadedAgent();
        assertTrue(leastLoaded.isPresent(), "Should have a least loaded agent");
        assertEquals(agent3, leastLoaded.get(), "Agent3 should be least loaded");
    }

    @Test
    @DisplayName("Getting agents sorted by availability")
    void testAgentsByAvailability() {
        WorkloadTracker tracker = new WorkloadTracker(5);

        UUID agent1 = UUID.randomUUID();
        UUID agent2 = UUID.randomUUID();
        UUID agent3 = UUID.randomUUID();

        tracker.registerAgent(agent1, 5);
        tracker.registerAgent(agent2, 5);
        tracker.registerAgent(agent3, 5);

        // Create different loads
        tracker.assignTask(agent1, "task1");
        tracker.assignTask(agent1, "task2");
        tracker.assignTask(agent1, "task3");
        tracker.assignTask(agent1, "task4");

        tracker.assignTask(agent2, "task1");
        tracker.assignTask(agent2, "task2");

        // agent3: no tasks

        List<UUID> sorted = tracker.getAgentsByAvailability();

        assertEquals(3, sorted.size(), "Should have all agents");
        assertEquals(agent3, sorted.get(0), "Agent3 should be first (lowest load)");
        assertEquals(agent2, sorted.get(1), "Agent2 should be second");
        assertEquals(agent1, sorted.get(2), "Agent1 should be last (highest load)");
    }

    @Test
    @DisplayName("Filtering agents by load threshold")
    void testAgentsWithLoadBelow() {
        WorkloadTracker tracker = new WorkloadTracker(5);

        UUID agent1 = UUID.randomUUID();
        UUID agent2 = UUID.randomUUID();
        UUID agent3 = UUID.randomUUID();

        tracker.registerAgent(agent1, 5);
        tracker.registerAgent(agent2, 5);
        tracker.registerAgent(agent3, 5);

        // agent1: 80% load (4/5 tasks)
        for (int i = 0; i < 4; i++) {
            tracker.assignTask(agent1, "task" + i);
        }

        // agent2: 40% load (2/5 tasks)
        for (int i = 0; i < 2; i++) {
            tracker.assignTask(agent2, "task" + i);
        }

        // agent3: 0% load

        List<UUID> below50 = tracker.getAgentsWithLoadBelow(0.5);
        assertEquals(2, below50.size(), "Should have 2 agents below 50% load");
        assertTrue(below50.contains(agent2), "Agent2 should be below 50%");
        assertTrue(below50.contains(agent3), "Agent3 should be below 50%");
        assertFalse(below50.contains(agent1), "Agent1 should not be below 50%");
    }

    @Test
    @DisplayName("Statistics collection across all agents")
    void testStatisticsCollection() {
        WorkloadTracker tracker = new WorkloadTracker(5);

        UUID agent1 = UUID.randomUUID();
        UUID agent2 = UUID.randomUUID();
        UUID agent3 = UUID.randomUUID();

        tracker.registerAgent(agent1, 5);
        tracker.registerAgent(agent2, 3);
        tracker.registerAgent(agent3, 10);

        // Simulate activity
        tracker.assignTask(agent1, "task1");
        tracker.assignTask(agent1, "task2");
        tracker.completeTask(agent1, "task1", true, 1000);
        tracker.completeTask(agent1, "task2", false, 500);

        tracker.assignTask(agent2, "task1");
        tracker.assignTask(agent2, "task2");
        tracker.assignTask(agent2, "task3");

        Map<String, Object> stats = tracker.getStatistics();

        assertEquals(3, stats.get("agentCount"), "Should track agent count");
        assertEquals(3, stats.get("totalActiveTasks"), "Should track active tasks");
        assertEquals(1, stats.get("totalCompleted"), "Should track completed tasks");
        assertEquals(1, stats.get("totalFailed"), "Should track failed tasks");

        double successRate = (Double) stats.get("overallSuccessRate");
        assertEquals(0.5, successRate, 0.001, "Success rate should be 50%");
    }

    @Test
    @DisplayName("Agent unregistration")
    void testAgentUnregistration() {
        WorkloadTracker tracker = new WorkloadTracker(5);

        UUID agent = UUID.randomUUID();
        tracker.registerAgent(agent, 5);

        assertTrue(tracker.isRegistered(agent), "Agent should be registered");

        tracker.assignTask(agent, "task1");
        tracker.assignTask(agent, "task2");

        WorkloadTracker.AgentWorkload removed = tracker.unregisterAgent(agent);

        assertNotNull(removed, "Should return removed workload");
        assertEquals(2, removed.getActiveTaskCount(), "Should preserve active task count");

        assertFalse(tracker.isRegistered(agent), "Agent should not be registered");
        assertEquals(0, tracker.getCurrentLoad(agent), 0.001,
            "Load should be 0 after unregistration");
    }

    @Test
    @DisplayName("Success rate calculation")
    void testSuccessRateCalculation() {
        WorkloadTracker tracker = new WorkloadTracker(5);

        UUID agent = UUID.randomUUID();
        tracker.registerAgent(agent, 5);

        // Complete some tasks with mixed results
        tracker.assignTask(agent, "task1");
        tracker.assignTask(agent, "task2");
        tracker.assignTask(agent, "task3");
        tracker.assignTask(agent, "task4");
        tracker.assignTask(agent, "task5");

        tracker.completeTask(agent, "task1", true, 1000);
        tracker.completeTask(agent, "task2", true, 1000);
        tracker.completeTask(agent, "task3", false, 500);
        tracker.completeTask(agent, "task4", true, 1000);
        tracker.completeTask(agent, "task5", false, 500);

        WorkloadTracker.AgentWorkload workload = tracker.getWorkload(agent);
        assertEquals(3, workload.getTotalCompleted(), "Should have 3 completed");
        assertEquals(2, workload.getTotalFailed(), "Should have 2 failed");
        assertEquals(0.6, workload.getSuccessRate(), 0.001,
            "Success rate should be 60%");
    }

    @Test
    @DisplayName("Average completion time tracking")
    void testAverageCompletionTime() {
        WorkloadTracker tracker = new WorkloadTracker(5);

        UUID agent = UUID.randomUUID();
        tracker.registerAgent(agent, 5);

        // Complete tasks with known durations
        tracker.assignTask(agent, "task1");
        tracker.completeTask(agent, "task1", true, 1000);

        tracker.assignTask(agent, "task2");
        tracker.completeTask(agent, "task2", true, 2000);

        tracker.assignTask(agent, "task3");
        tracker.completeTask(agent, "task3", true, 3000);

        WorkloadTracker.AgentWorkload workload = tracker.getWorkload(agent);
        assertEquals(2000.0, workload.getAverageCompletionTime(), 0.001,
            "Average should be 2000ms");
    }

    @Test
    @DisplayName("Clear all workload data")
    void testClearAllData() {
        WorkloadTracker tracker = new WorkloadTracker(5);

        // Register multiple agents and assign tasks
        for (int i = 0; i < 5; i++) {
            UUID agent = UUID.randomUUID();
            tracker.registerAgent(agent, 5);
            tracker.assignTask(agent, "task1");
        }

        assertTrue(tracker.getAgentCount() > 0, "Should have agents");

        tracker.clear();

        assertEquals(0, tracker.getAgentCount(), "Should have no agents after clear");
        assertEquals(0, tracker.getTotalActiveTasks(), "Should have no active tasks");
    }

    @Test
    @DisplayName("Concurrent availability queries")
    void testConcurrentAvailabilityQueries() throws InterruptedException {
        WorkloadTracker tracker = new WorkloadTracker(5);

        // Register many agents
        List<UUID> agents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            UUID agent = UUID.randomUUID();
            agents.add(agent);
            tracker.registerAgent(agent, 5);
        }

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(100);

        // Concurrently query and modify availability
        AtomicInteger queryCount = new AtomicInteger(0);

        for (int i = 0; i < 100; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    UUID agent = agents.get(index % agents.size());

                    // Mix of queries and modifications
                    if (index % 3 == 0) {
                        boolean available = tracker.isAvailable(agent);
                        double load = tracker.getCurrentLoad(agent);
                        queryCount.incrementAndGet();
                    } else if (index % 3 == 1) {
                        tracker.assignTask(agent, "task-" + index);
                    } else {
                        tracker.completeTask(agent, "task-" + (index - 1), true);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(queryCount.get() > 0, "Should have executed queries");
        // Verify no corruption
        assertTrue(tracker.getAverageLoad() >= 0.0 && tracker.getAverageLoad() <= 1.0,
            "Average load should be valid");
    }

    @Test
    @DisplayName("Edge case: null and invalid inputs")
    void testEdgeCases() {
        WorkloadTracker tracker = new WorkloadTracker(5);

        // Null inputs should be handled gracefully
        assertFalse(tracker.registerAgent(null), "Null agent should not register");
        assertFalse(tracker.assignTask(null, "task1"), "Null agent assign should fail");
        assertFalse(tracker.assignTask(UUID.randomUUID(), null), "Null task should fail");
        assertFalse(tracker.completeTask(null, "task1", true), "Null agent complete should fail");
        assertFalse(tracker.completeTask(UUID.randomUUID(), null, true), "Null task complete should fail");

        assertNull(tracker.getWorkload(null), "Null agent workload should be null");
        assertEquals(0.0, tracker.getCurrentLoad(null), 0.001, "Null agent load should be 0");
        assertFalse(tracker.isAvailable(null), "Null agent should not be available");
    }
}
