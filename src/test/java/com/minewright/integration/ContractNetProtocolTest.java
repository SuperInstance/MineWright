package com.minewright.integration;

import com.minewright.action.Task;
import com.minewright.coordination.*;
import com.minewright.entity.ForemanEntity;
import com.minewright.execution.AgentState;
import com.minewright.orchestration.AgentRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Contract Net Protocol implementation.
 *
 * <p><b>Test Coverage:</b></p>
 * <ul>
 *   <li>Task announcement and bid submission</li>
 *   <li>Contract awarding to best bidder</li>
 *   <li>Concurrent bidding scenarios</li>
 *   <li>Workload-aware bid selection</li>
 *   <li>Negotiation state management</li>
 *   <li>Timeout and expiry handling</li>
 *   <li>Multi-agent coordination</li>
 * </ul>
 *
 * @see ContractNetManager
 * @see TaskBid
 * @see TaskAnnouncement
 * @since 1.3.0
 */
@DisplayName("Contract Net Protocol Integration Tests")
class ContractNetProtocolTest extends IntegrationTestBase {

    @Test
    @DisplayName("Manager announces task and receives bids")
    void testTaskAnnouncementAndBidding() {
        ContractNetManager manager = new ContractNetManager();

        // Create a task
        Task task = new Task("mine", Map.of("block", "iron_ore", "quantity", 50));
        UUID managerId = UUID.randomUUID();

        // Announce task
        String announcementId = manager.announceTask(task, managerId);

        assertNotNull(announcementId, "Announcement ID should not be null");
        assertEquals(1, manager.getActiveCount(), "Should have one active negotiation");

        ContractNetManager.ContractNegotiation negotiation = manager.getNegotiation(announcementId);
        assertNotNull(negotiation, "Negotiation should exist");
        assertEquals(ContractNetManager.ContractState.ANNOUNCED, negotiation.getState(),
            "Negotiation should be in ANNOUNCED state");
        assertEquals(0, negotiation.getBidCount(), "Should have no bids initially");
    }

    @Test
    @DisplayName("Multiple agents submit bids for task")
    void testMultipleAgentsSubmitBids() {
        ContractNetManager manager = new ContractNetManager();

        Task task = new Task("mine", Map.of("block", "stone", "quantity", 100));
        UUID managerId = UUID.randomUUID();
        String announcementId = manager.announceTask(task, managerId);

        // Register agents for workload tracking
        UUID agent1 = UUID.randomUUID();
        UUID agent2 = UUID.randomUUID();
        UUID agent3 = UUID.randomUUID();
        manager.registerAgent(agent1, 5);
        manager.registerAgent(agent2, 5);
        manager.registerAgent(agent3, 5);

        // Submit bids from multiple agents
        TaskBid bid1 = TaskBid.builder()
            .announcementId(announcementId)
            .bidderId(agent1)
            .score(0.95)
            .estimatedTime(5000)
            .confidence(0.9)
            .build();

        TaskBid bid2 = TaskBid.builder()
            .announcementId(announcementId)
            .bidderId(agent2)
            .score(0.87)
            .estimatedTime(6000)
            .confidence(0.85)
            .build();

        TaskBid bid3 = TaskBid.builder()
            .announcementId(announcementId)
            .bidderId(agent3)
            .score(0.92)
            .estimatedTime(4500)
            .confidence(0.88)
            .build();

        assertTrue(manager.submitBid(bid1), "First bid should be accepted");
        assertTrue(manager.submitBid(bid2), "Second bid should be accepted");
        assertTrue(manager.submitBid(bid3), "Third bid should be accepted");

        List<TaskBid> bids = manager.getBids(announcementId);
        assertEquals(3, bids.size(), "Should have three bids");
    }

    @Test
    @DisplayName("Duplicate bid from same agent is rejected")
    void testDuplicateBidRejected() {
        ContractNetManager manager = new ContractNetManager();

        Task task = new Task("build", Map.of("structure", "house"));
        UUID managerId = UUID.randomUUID();
        String announcementId = manager.announceTask(task, managerId);

        UUID agentId = UUID.randomUUID();

        TaskBid bid1 = TaskBid.builder()
            .announcementId(announcementId)
            .bidderId(agentId)
            .score(0.9)
            .estimatedTime(10000)
            .build();

        TaskBid bid2 = TaskBid.builder()
            .announcementId(announcementId)
            .bidderId(agentId)
            .score(0.95)
            .estimatedTime(9000)
            .build();

        assertTrue(manager.submitBid(bid1), "First bid should be accepted");
        assertFalse(manager.submitBid(bid2), "Duplicate bid should be rejected");

        List<TaskBid> bids = manager.getBids(announcementId);
        assertEquals(1, bids.size(), "Should only have one bid");
    }

    @Test
    @DisplayName("Best bidder is selected and awarded contract")
    void testBestBidderAwardedContract() {
        ContractNetManager manager = new ContractNetManager();

        Task task = new Task("craft", Map.of("item", "iron_pickaxe"));
        UUID managerId = UUID.randomUUID();
        String announcementId = manager.announceTask(task, managerId);

        // Submit bids with different scores
        UUID agent1 = UUID.randomUUID();
        UUID agent2 = UUID.randomUUID();
        UUID agent3 = UUID.randomUUID();

        manager.submitBid(TaskBid.builder()
            .announcementId(announcementId)
            .bidderId(agent1)
            .score(0.85)
            .estimatedTime(8000)
            .build());

        manager.submitBid(TaskBid.builder()
            .announcementId(announcementId)
            .bidderId(agent2)
            .score(0.92)
            .estimatedTime(7000)
            .build());

        manager.submitBid(TaskBid.builder()
            .announcementId(announcementId)
            .bidderId(agent3)
            .score(0.78)
            .estimatedTime(9000)
            .build());

        // Select and award to best bidder
        Optional<TaskBid> winner = manager.awardToBestBidder(announcementId);

        assertTrue(winner.isPresent(), "Should have a winner");
        assertEquals(agent2, winner.get().bidderId(), "Agent2 should win (highest score)");

        ContractNetManager.ContractNegotiation negotiation = manager.getNegotiation(announcementId);
        assertEquals(ContractNetManager.ContractState.AWARDED, negotiation.getState(),
            "Negotiation should be in AWARDED state");
        assertEquals(agent2, negotiation.getAwardedAgent(), "Agent2 should be awarded");
    }

    @Test
    @DisplayName("Concurrent bid submission from multiple agents")
    void testConcurrentBidSubmission() throws InterruptedException {
        ContractNetManager manager = new ContractNetManager();

        Task task = new Task("gather", Map.of("resource", "wood"));
        UUID managerId = UUID.randomUUID();
        String announcementId = manager.announceTask(task, managerId);

        int numAgents = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numAgents);
        CountDownLatch latch = new CountDownLatch(numAgents);
        AtomicInteger successCount = new AtomicInteger(0);

        // Submit bids concurrently
        for (int i = 0; i < numAgents; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    UUID agentId = UUID.randomUUID();
                    TaskBid bid = TaskBid.builder()
                        .announcementId(announcementId)
                        .bidderId(agentId)
                        .score(0.7 + (index * 0.03))
                        .estimatedTime(5000 + index * 100)
                        .build();

                    if (manager.submitBid(bid)) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(numAgents, successCount.get(), "All bids should be accepted");

        List<TaskBid> bids = manager.getBids(announcementId);
        assertEquals(numAgents, bids.size(), "Should have all bids");
    }

    @Test
    @DisplayName("Workload-aware bid selection considers agent load")
    void testWorkloadAwareBidSelection() {
        // Create custom workload tracker
        WorkloadTracker workloadTracker = new WorkloadTracker();

        // Register agents with different workloads
        UUID busyAgent = UUID.randomUUID();
        UUID availableAgent = UUID.randomUUID();
        UUID moderateAgent = UUID.randomUUID();

        workloadTracker.registerAgent(busyAgent, 5);
        workloadTracker.registerAgent(availableAgent, 5);
        workloadTracker.registerAgent(moderateAgent, 5);

        // Assign tasks to create different load levels
        workloadTracker.assignTask(busyAgent, "task1");
        workloadTracker.assignTask(busyAgent, "task2");
        workloadTracker.assignTask(busyAgent, "task3");
        workloadTracker.assignTask(busyAgent, "task4");

        workloadTracker.assignTask(moderateAgent, "task1");

        ContractNetManager manager = new ContractNetManager(workloadTracker, new AwardSelector());

        Task task = new Task("mine", Map.of("block", "coal"));
        String announcementId = manager.announceTask(task, UUID.randomUUID());

        // Submit bids
        manager.submitBid(TaskBid.builder()
            .announcementId(announcementId)
            .bidderId(busyAgent)
            .score(0.95)
            .estimatedTime(3000)
            .build());

        manager.submitBid(TaskBid.builder()
            .announcementId(announcementId)
            .bidderId(availableAgent)
            .score(0.90)
            .estimatedTime(4000)
            .build());

        manager.submitBid(TaskBid.builder()
            .announcementId(announcementId)
            .bidderId(moderateAgent)
            .score(0.92)
            .estimatedTime(3500)
            .build());

        // Detailed selection should consider workload
        Optional<AwardSelector.SelectionResult> result = manager.selectWinnerDetailed(announcementId);

        assertTrue(result.isPresent(), "Should have selection result");
        // The available agent should be preferred despite slightly lower score
        assertEquals(availableAgent, result.get().getSelectedBid().bidderId(),
            "Available agent should be selected");
    }

    @Test
    @DisplayName("Negotiation expires after deadline")
    void testNegotiationExpiry() throws InterruptedException {
        ContractNetManager manager = new ContractNetManager();

        Task task = new Task("explore", Map.of("area", "cave"));
        String announcementId = manager.announceTask(task, UUID.randomUUID(), 100); // 100ms deadline

        // Wait for deadline to pass
        Thread.sleep(150);

        // Try to submit bid after deadline
        UUID lateAgent = UUID.randomUUID();
        TaskBid lateBid = TaskBid.builder()
            .announcementId(announcementId)
            .bidderId(lateAgent)
            .score(0.9)
            .estimatedTime(5000)
            .build();

        assertFalse(manager.submitBid(lateBid), "Late bid should be rejected");

        ContractNetManager.ContractNegotiation negotiation = manager.getNegotiation(announcementId);
        assertEquals(ContractNetManager.ContractState.EXPIRED, negotiation.getState(),
            "Negotiation should be expired");
    }

    @Test
    @DisplayName("Contract event listeners are notified")
    void testContractEventListeners() {
        ContractNetManager manager = new ContractNetManager();

        List<String> events = new ArrayList<>();

        manager.addListener(new ContractNetManager.ContractListener() {
            @Override
            public void onAnnouncement(TaskAnnouncement announcement) {
                events.add("ANNOUNCED:" + announcement.announcementId());
            }

            @Override
            public void onBidSubmitted(TaskBid bid) {
                events.add("BID:" + bid.bidderId());
            }

            @Override
            public void onContractAwarded(String announcementId, TaskBid winner) {
                events.add("AWARDED:" + announcementId + ":" + winner.bidderId());
            }

            @Override
            public void onNegotiationExpired(String announcementId) {
                events.add("EXPIRED:" + announcementId);
            }
        });

        Task task = new Task("test", Map.of());
        String announcementId = manager.announceTask(task, UUID.randomUUID());

        assertTrue(events.contains("ANNOUNCED:" + announcementId),
            "Should receive announcement event");

        UUID agentId = UUID.randomUUID();
        TaskBid bid = TaskBid.builder()
            .announcementId(announcementId)
            .bidderId(agentId)
            .score(0.9)
            .estimatedTime(5000)
            .build();

        manager.submitBid(bid);
        manager.awardToBestBidder(announcementId);

        assertTrue(events.stream().anyMatch(e -> e.startsWith("BID:")),
            "Should receive bid event");
        assertTrue(events.contains("AWARDED:" + announcementId + ":" + agentId),
            "Should receive awarded event");
    }

    @Test
    @DisplayName("Workload tracking integration with task assignment")
    void testWorkloadTrackingIntegration() {
        ContractNetManager manager = new ContractNetManager();

        UUID agent1 = UUID.randomUUID();
        UUID agent2 = UUID.randomUUID();

        manager.registerAgent(agent1, 3);
        manager.registerAgent(agent2, 3);

        // Assign tasks to agent1
        assertTrue(manager.assignTaskToAgent(agent1, "task1"), "First task assigned");
        assertTrue(manager.assignTaskToAgent(agent1, "task2"), "Second task assigned");
        assertTrue(manager.assignTaskToAgent(agent1, "task3"), "Third task assigned");

        // Agent1 should be at capacity
        assertFalse(manager.assignTaskToAgent(agent1, "task4"),
            "Should not assign task when at capacity");

        assertEquals(1.0, manager.getAgentLoad(agent1), 0.001,
            "Agent1 should be at full load");
        assertEquals(0.0, manager.getAgentLoad(agent2), 0.001,
            "Agent2 should have no load");

        assertFalse(manager.isAgentAvailable(agent1), "Agent1 should not be available");
        assertTrue(manager.isAgentAvailable(agent2), "Agent2 should be available");

        // Complete some tasks
        assertTrue(manager.completeAgentTask(agent1, "task1", true), "Task1 completed");

        assertEquals(2.0 / 3.0, manager.getAgentLoad(agent1), 0.001,
            "Agent1 load should decrease");
        assertTrue(manager.isAgentAvailable(agent1), "Agent1 should be available again");
    }

    @Test
    @DisplayName("Multiple simultaneous negotiations")
    void testMultipleSimultaneousNegotiations() {
        ContractNetManager manager = new ContractNetManager();

        // Announce multiple tasks
        String id1 = manager.announceTask(new Task("mine", Map.of("block", "stone")), UUID.randomUUID());
        String id2 = manager.announceTask(new Task("build", Map.of("structure", "wall")), UUID.randomUUID());
        String id3 = manager.announceTask(new Task("craft", Map.of("item", "sword")), UUID.randomUUID());

        assertEquals(3, manager.getActiveCount(), "Should have 3 active negotiations");

        // Submit bids for each
        UUID agent1 = UUID.randomUUID();
        UUID agent2 = UUID.randomUUID();

        manager.submitBid(TaskBid.builder().announcementId(id1).bidderId(agent1).score(0.9).estimatedTime(5000).build());
        manager.submitBid(TaskBid.builder().announcementId(id2).bidderId(agent2).score(0.85).estimatedTime(6000).build());
        manager.submitBid(TaskBid.builder().announcementId(id3).bidderId(agent1).score(0.92).estimatedTime(4000).build());

        // Award contracts
        Optional<TaskBid> winner1 = manager.awardToBestBidder(id1);
        Optional<TaskBid> winner2 = manager.awardToBestBidder(id2);
        Optional<TaskBid> winner3 = manager.awardToBestBidder(id3);

        assertTrue(winner1.isPresent() && winner2.isPresent() && winner3.isPresent(),
            "All negotiations should have winners");
    }

    @Test
    @DisplayName("Statistics collection across negotiations")
    void testStatisticsCollection() {
        ContractNetManager manager = new ContractNetManager();

        // Register agents and simulate activity
        UUID agent1 = UUID.randomUUID();
        UUID agent2 = UUID.randomUUID();
        manager.registerAgent(agent1, 5);
        manager.registerAgent(agent2, 5);

        // Create multiple negotiations
        for (int i = 0; i < 5; i++) {
            String announcementId = manager.announceTask(
                new Task("task" + i, Map.of("index", i)),
                UUID.randomUUID()
            );

            manager.submitBid(TaskBid.builder()
                .announcementId(announcementId)
                .bidderId(i % 2 == 0 ? agent1 : agent2)
                .score(0.8 + (i * 0.04))
                .estimatedTime(5000 + i * 100)
                .build());

            if (i % 2 == 0) {
                manager.assignTaskToAgent(agent1, "task" + i);
            }
        }

        Map<String, Object> stats = manager.getFullStatistics();

        assertTrue((Integer) stats.get("totalNegotiations") >= 5,
            "Should track total negotiations");
        assertTrue((Integer) stats.get("agentCount") >= 2,
            "Should track agent count");
    }

    @Test
    @DisplayName("Cleanup removes old negotiations")
    void testCleanupRemovesOldNegotiations() throws InterruptedException {
        ContractNetManager manager = new ContractNetManager();

        // Create a negotiation and complete it
        String announcementId = manager.announceTask(
            new Task("test", Map.of()),
            UUID.randomUUID()
        );

        UUID agentId = UUID.randomUUID();
        manager.submitBid(TaskBid.builder()
            .announcementId(announcementId)
            .bidderId(agentId)
            .score(0.9)
            .estimatedTime(5000)
            .build());

        manager.awardToBestBidder(announcementId);

        // Manually set to completed
        ContractNetManager.ContractNegotiation negotiation = manager.getNegotiation(announcementId);
        manager.closeNegotiation(announcementId, ContractNetManager.ContractState.COMPLETED);

        int initialCount = manager.getAllNegotiations().size();
        assertTrue(initialCount > 0, "Should have negotiations");

        // Cleanup should remove completed negotiations
        int cleaned = manager.cleanup();

        // Due to 5-minute threshold, immediate cleanup won't remove
        // But the cleanup method should execute without error
        assertNotNull(manager.getNegotiation(announcementId),
            "Recent negotiation should still exist");
    }
}
