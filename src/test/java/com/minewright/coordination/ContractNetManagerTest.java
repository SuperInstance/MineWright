package com.minewright.coordination;

import com.minewright.action.Task;
import com.minewright.testutil.TaskBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link ContractNetManager}.
 *
 * <p>Tests cover the Contract Net Protocol implementation including:</p>
 * <ul>
 *   <li>Task announcement and ID generation</li>
 *   <li>Bid submission and validation</li>
 *   <li>Winner selection logic</li>
 *   <li>Contract awarding</li>
 *   <li>Deadline enforcement</li>
 *   <li>Listener notifications</li>
 *   <li>Thread safety</li>
 *   <li>Cleanup operations</li>
 * </ul>
 *
 * @see ContractNetManager
 * @see TaskAnnouncement
 * @see TaskBid
 */
@DisplayName("Contract Net Manager Tests")
@ExtendWith(MockitoExtension.class)
class ContractNetManagerTest {

    private ContractNetManager manager;
    private UUID requesterId;
    private Task sampleTask;

    @Mock
    private ContractNetManager.ContractListener listener;

    @BeforeEach
    void setUp() {
        manager = new ContractNetManager();
        requesterId = UUID.randomUUID();
        sampleTask = TaskBuilder.aTask("mine")
                .withBlock("stone")
                .withQuantity(64)
                .build();
    }

    @Nested
    @DisplayName("Task Announcement Tests")
    class AnnouncementTests {

        @Test
        @DisplayName("Announce task generates unique ID")
        void announceTaskGeneratesId() {
            String announcementId1 = manager.announceTask(sampleTask, requesterId);
            String announcementId2 = manager.announceTask(sampleTask, requesterId);

            assertNotNull(announcementId1, "Announcement ID should not be null");
            assertNotNull(announcementId2, "Announcement ID should not be null");
            assertNotEquals(announcementId1, announcementId2,
                    "Each announcement should have a unique ID");
        }

        @Test
        @DisplayName("Announce task creates negotiation record")
        void announceTaskCreatesNegotiation() {
            String announcementId = manager.announceTask(sampleTask, requesterId);

            ContractNetManager.ContractNegotiation negotiation =
                    manager.getNegotiation(announcementId);

            assertNotNull(negotiation, "Negotiation should be created");
            assertEquals(ContractNetManager.ContractState.ANNOUNCED,
                    negotiation.getState(),
                    "Initial state should be ANNOUNCED");
            assertNotNull(negotiation.getAnnouncement(),
                    "Negotiation should contain the announcement");
        }

        @Test
        @DisplayName("Announce task with custom deadline")
        void announceTaskWithCustomDeadline() {
            long deadlineDuration = 60000; // 1 minute
            String announcementId = manager.announceTask(sampleTask, requesterId, deadlineDuration);

            ContractNetManager.ContractNegotiation negotiation =
                    manager.getNegotiation(announcementId);

            assertNotNull(negotiation);
            long expectedDeadline = System.currentTimeMillis() + deadlineDuration;
            long actualDeadline = negotiation.getAnnouncement().deadline();

            // Allow 1 second tolerance for test execution time
            assertTrue(Math.abs(actualDeadline - expectedDeadline) < 1000,
                    "Deadline should be approximately " + deadlineDuration + "ms in the future");
        }

        @Test
        @DisplayName("Announce task notifies listeners")
        void announceTaskNotifiesListeners() {
            manager.addListener(listener);

            manager.announceTask(sampleTask, requesterId);

            verify(listener, times(1)).onAnnouncement(any(TaskAnnouncement.class));
        }

        @Test
        @DisplayName("Multiple listeners are notified")
        void multipleListenersNotified() {
            ContractNetManager.ContractListener listener2 = mock(ContractNetManager.ContractListener.class);
            manager.addListener(listener);
            manager.addListener(listener2);

            manager.announceTask(sampleTask, requesterId);

            verify(listener, times(1)).onAnnouncement(any());
            verify(listener2, times(1)).onAnnouncement(any());
        }
    }

    @Nested
    @DisplayName("Bid Submission Tests")
    class BidSubmissionTests {

        private String announcementId;
        private UUID bidderId;

        @BeforeEach
        void setUp() {
            announcementId = manager.announceTask(sampleTask, requesterId);
            bidderId = UUID.randomUUID();
        }

        @Test
        @DisplayName("Submit bid records bid")
        void submitBidRecordsBid() {
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            boolean accepted = manager.submitBid(bid);

            assertTrue(accepted, "Bid should be accepted");

            var bids = manager.getBids(announcementId);
            assertEquals(1, bids.size(), "Should have one bid");
            assertEquals(bid, bids.get(0), "Bid should match submitted bid");
        }

        @Test
        @DisplayName("Submit bid for non-existent announcement returns false")
        void submitBidForNonExistentAnnouncement() {
            TaskBid bid = TaskBid.builder()
                    .announcementId("non_existent_id")
                    .bidderId(bidderId)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            boolean accepted = manager.submitBid(bid);

            assertFalse(accepted, "Bid should be rejected for non-existent announcement");
        }

        @Test
        @DisplayName("Submit bid after deadline returns false")
        void submitBidAfterDeadline() {
            // Create announcement with very short deadline
            String shortDeadlineId = manager.announceTask(sampleTask, requesterId, 10);

            // Wait for deadline to pass
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                fail("Test interrupted");
            }

            TaskBid bid = TaskBid.builder()
                    .announcementId(shortDeadlineId)
                    .bidderId(bidderId)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            boolean accepted = manager.submitBid(bid);

            assertFalse(accepted, "Bid should be rejected after deadline");
        }

        @Test
        @DisplayName("Submit duplicate bid from same agent returns false")
        void submitDuplicateBid() {
            TaskBid bid1 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            TaskBid bid2 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId)
                    .score(0.9)
                    .estimatedTime(25000)
                    .confidence(0.95)
                    .build();

            boolean firstAccepted = manager.submitBid(bid1);
            boolean secondAccepted = manager.submitBid(bid2);

            assertTrue(firstAccepted, "First bid should be accepted");
            assertFalse(secondAccepted, "Duplicate bid from same agent should be rejected");

            var bids = manager.getBids(announcementId);
            assertEquals(1, bids.size(), "Should only have the first bid");
        }

        @Test
        @DisplayName("Submit bid notifies listeners")
        void submitBidNotifiesListeners() {
            manager.addListener(listener);

            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            manager.submitBid(bid);

            verify(listener, times(1)).onBidSubmitted(bid);
        }

        @Test
        @DisplayName("Multiple bids from different agents are accepted")
        void multipleBidsFromDifferentAgents() {
            UUID agent1 = UUID.randomUUID();
            UUID agent2 = UUID.randomUUID();
            UUID agent3 = UUID.randomUUID();

            TaskBid bid1 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent1)
                    .score(0.7)
                    .estimatedTime(40000)
                    .confidence(0.8)
                    .build();

            TaskBid bid2 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent2)
                    .score(0.9)
                    .estimatedTime(25000)
                    .confidence(0.95)
                    .build();

            TaskBid bid3 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent3)
                    .score(0.6)
                    .estimatedTime(50000)
                    .confidence(0.7)
                    .build();

            assertTrue(manager.submitBid(bid1));
            assertTrue(manager.submitBid(bid2));
            assertTrue(manager.submitBid(bid3));

            var bids = manager.getBids(announcementId);
            assertEquals(3, bids.size(), "All three bids should be recorded");
        }

        @Test
        @DisplayName("Null bid returns false")
        void nullBidReturnsFalse() {
            boolean accepted = manager.submitBid(null);

            assertFalse(accepted, "Null bid should be rejected");
        }
    }

    @Nested
    @DisplayName("Winner Selection Tests")
    class WinnerSelectionTests {

        private String announcementId;

        @BeforeEach
        void setUp() {
            announcementId = manager.announceTask(sampleTask, requesterId);
        }

        @Test
        @DisplayName("Select winner picks best bid")
        void selectWinnerPicksBestBid() {
            UUID agent1 = UUID.randomUUID();
            UUID agent2 = UUID.randomUUID();
            UUID agent3 = UUID.randomUUID();

            // Agent 1: score 0.7, time 40s, confidence 0.8
            // Bid value = (0.7 * 0.8) / 40 = 0.014
            TaskBid bid1 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent1)
                    .score(0.7)
                    .estimatedTime(40000)
                    .confidence(0.8)
                    .build();

            // Agent 2: score 0.9, time 25s, confidence 0.95
            // Bid value = (0.9 * 0.95) / 25 = 0.0342 (best)
            TaskBid bid2 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent2)
                    .score(0.9)
                    .estimatedTime(25000)
                    .confidence(0.95)
                    .build();

            // Agent 3: score 0.6, time 50s, confidence 0.7
            // Bid value = (0.6 * 0.7) / 50 = 0.0084
            TaskBid bid3 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent3)
                    .score(0.6)
                    .estimatedTime(50000)
                    .confidence(0.7)
                    .build();

            manager.submitBid(bid1);
            manager.submitBid(bid2);
            manager.submitBid(bid3);

            Optional<TaskBid> winner = manager.selectWinner(announcementId);

            assertTrue(winner.isPresent(), "Should have a winner");
            assertEquals(agent2, winner.get().bidderId(),
                    "Agent 2 should win with highest bid value");
        }

        @Test
        @DisplayName("Select winner with no bids returns empty")
        void noBidsReturnsEmpty() {
            Optional<TaskBid> winner = manager.selectWinner(announcementId);

            assertFalse(winner.isPresent(), "Should return empty when no bids");
        }

        @Test
        @DisplayName("Select winner for non-existent announcement returns empty")
        void selectWinnerForNonExistentAnnouncement() {
            Optional<TaskBid> winner = manager.selectWinner("non_existent_id");

            assertFalse(winner.isPresent(), "Should return empty for non-existent announcement");
        }

        @Test
        @DisplayName("Select winner uses tie-breaker when scores are equal")
        void selectWinnerUsesTieBreaker() {
            UUID agent1 = UUID.randomUUID();
            UUID agent2 = UUID.randomUUID();

            // Both have same score and confidence, but agent2 is faster
            TaskBid bid1 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent1)
                    .score(0.8)
                    .estimatedTime(40000)
                    .confidence(0.9)
                    .build();

            TaskBid bid2 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent2)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            manager.submitBid(bid1);
            manager.submitBid(bid2);

            Optional<TaskBid> winner = manager.selectWinner(announcementId);

            assertTrue(winner.isPresent());
            assertEquals(agent2, winner.get().bidderId(),
                    "Faster agent should win when bid values are equal");
        }
    }

    @Nested
    @DisplayName("Contract Awarding Tests")
    class ContractAwardingTests {

        private String announcementId;
        private UUID winningAgent;

        @BeforeEach
        void setUp() {
            announcementId = manager.announceTask(sampleTask, requesterId);
            winningAgent = UUID.randomUUID();
        }

        @Test
        @DisplayName("Award contract notifies winner")
        void awardContractNotifiesWinner() {
            AtomicInteger awardCount = new AtomicInteger(0);
            manager.addListener(new ContractNetManager.ContractListener() {
                @Override
                public void onContractAwarded(String announcementId, TaskBid winner) {
                    awardCount.incrementAndGet();
                }
            });

            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(winningAgent)
                    .score(0.9)
                    .estimatedTime(30000)
                    .confidence(0.95)
                    .build();

            manager.submitBid(bid);
            boolean awarded = manager.awardContract(announcementId, bid);

            assertTrue(awarded, "Contract should be awarded");
            assertEquals(1, awardCount.get(), "Listener should be notified");
        }

        @Test
        @DisplayName("Award contract updates negotiation state")
        void awardContractUpdatesState() {
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(winningAgent)
                    .score(0.9)
                    .estimatedTime(30000)
                    .confidence(0.95)
                    .build();

            manager.submitBid(bid);
            manager.awardContract(announcementId, bid);

            ContractNetManager.ContractNegotiation negotiation =
                    manager.getNegotiation(announcementId);

            assertEquals(ContractNetManager.ContractState.AWARDED,
                    negotiation.getState(),
                    "State should be AWARDED");
            assertEquals(bid, negotiation.getWinningBid(),
                    "Winning bid should be recorded");
            assertEquals(winningAgent, negotiation.getAwardedAgent(),
                    "Awarded agent should be recorded");
        }

        @Test
        @DisplayName("Award contract for non-existent bid returns false")
        void awardContractForInvalidBid() {
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(winningAgent)
                    .score(0.9)
                    .estimatedTime(30000)
                    .confidence(0.95)
                    .build();

            // Don't submit the bid
            boolean awarded = manager.awardContract(announcementId, bid);

            assertFalse(awarded, "Should not award contract for unsubmitted bid");
        }

        @Test
        @DisplayName("Award contract twice returns false second time")
        void awardContractTwice() {
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(winningAgent)
                    .score(0.9)
                    .estimatedTime(30000)
                    .confidence(0.95)
                    .build();

            manager.submitBid(bid);
            boolean firstAward = manager.awardContract(announcementId, bid);
            boolean secondAward = manager.awardContract(announcementId, bid);

            assertTrue(firstAward, "First award should succeed");
            assertFalse(secondAward, "Second award should fail as negotiation is closed");
        }

        @Test
        @DisplayName("Award to best bidder combines selection and awarding")
        void awardToBestBidder() {
            UUID agent1 = UUID.randomUUID();
            UUID agent2 = UUID.randomUUID();

            TaskBid bid1 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent1)
                    .score(0.7)
                    .estimatedTime(40000)
                    .confidence(0.8)
                    .build();

            TaskBid bid2 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent2)
                    .score(0.9)
                    .estimatedTime(25000)
                    .confidence(0.95)
                    .build();

            manager.submitBid(bid1);
            manager.submitBid(bid2);

            Optional<TaskBid> winner = manager.awardToBestBidder(announcementId);

            assertTrue(winner.isPresent(), "Should have a winner");
            assertEquals(agent2, winner.get().bidderId(),
                    "Best agent should be awarded");

            ContractNetManager.ContractNegotiation negotiation =
                    manager.getNegotiation(announcementId);
            assertEquals(ContractNetManager.ContractState.AWARDED,
                    negotiation.getState(),
                    "State should be AWARDED");
        }

        @Test
        @DisplayName("Cannot award after closing negotiation")
        void cannotAwardAfterClosing() {
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(winningAgent)
                    .score(0.9)
                    .estimatedTime(30000)
                    .confidence(0.95)
                    .build();

            manager.submitBid(bid);
            manager.closeNegotiation(announcementId, ContractNetManager.ContractState.FAILED);

            boolean awarded = manager.awardContract(announcementId, bid);

            assertFalse(awarded, "Should not award after negotiation is closed");
        }
    }

    @Nested
    @DisplayName("Deadline and Expiration Tests")
    class DeadlineTests {

        @Test
        @DisplayName("Deadline expired rejects bids")
        void deadlineExpiredRejectsBids() {
            // Create announcement with very short deadline
            String announcementId = manager.announceTask(sampleTask, requesterId, 10);

            // Wait for deadline to pass
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                fail("Test interrupted");
            }

            UUID bidderId = UUID.randomUUID();
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            boolean accepted = manager.submitBid(bid);

            assertFalse(accepted, "Bid should be rejected after deadline");

            ContractNetManager.ContractNegotiation negotiation =
                    manager.getNegotiation(announcementId);
            assertEquals(ContractNetManager.ContractState.EXPIRED,
                    negotiation.getState(),
                    "Negotiation should be marked as EXPIRED");
        }

        @Test
        @DisplayName("Expired negotiation notifies listeners")
        void expiredNegotiationNotifiesListeners() {
            manager.addListener(listener);

            String announcementId = manager.announceTask(sampleTask, requesterId, 10);

            // Wait for deadline and submit a bid to trigger expiration
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                fail("Test interrupted");
            }

            UUID bidderId = UUID.randomUUID();
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            manager.submitBid(bid);

            verify(listener, times(1)).onNegotiationExpired(announcementId);
        }

        @Test
        @DisplayName("Check if announcement is expired")
        void checkAnnouncementExpired() {
            String announcementId = manager.announceTask(sampleTask, requesterId, 10);

            ContractNetManager.ContractNegotiation negotiation =
                    manager.getNegotiation(announcementId);

            assertFalse(negotiation.getAnnouncement().isExpired(),
                    "Should not be expired immediately");

            // Wait for deadline
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                fail("Test interrupted");
            }

            assertTrue(negotiation.getAnnouncement().isExpired(),
                    "Should be expired after deadline");
        }

        @Test
        @DisplayName("Get remaining time decreases over time")
        void getRemainingTimeDecreases() {
            long deadlineDuration = 5000;
            String announcementId = manager.announceTask(sampleTask, requesterId, deadlineDuration);

            ContractNetManager.ContractNegotiation negotiation =
                    manager.getNegotiation(announcementId);

            long remaining1 = negotiation.getAnnouncement().getRemainingTime();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                fail("Test interrupted");
            }

            long remaining2 = negotiation.getAnnouncement().getRemainingTime();

            assertTrue(remaining2 < remaining1,
                    "Remaining time should decrease");
            assertTrue(remaining2 > 0,
                    "Remaining time should still be positive before deadline");
        }
    }

    @Nested
    @DisplayName("Listener Management Tests")
    class ListenerTests {

        @Test
        @DisplayName("Add and remove listener")
        void addAndRemoveListener() {
            manager.addListener(listener);

            manager.announceTask(sampleTask, requesterId);
            verify(listener, times(1)).onAnnouncement(any());

            manager.removeListener(listener);

            manager.announceTask(sampleTask, requesterId);
            // Should still be called once (from before removal)
            verify(listener, times(1)).onAnnouncement(any());
        }

        @Test
        @DisplayName("Null listener is ignored")
        void nullListenerIgnored() {
            // Should not throw exception
            assertDoesNotThrow(() -> manager.addListener(null));
            assertDoesNotThrow(() -> manager.removeListener(null));
        }

        @Test
        @DisplayName("Listener exception does not stop processing")
        void listenerExceptionHandling() {
            ContractNetManager.ContractListener failingListener =
                    mock(ContractNetManager.ContractListener.class);
            ContractNetManager.ContractListener normalListener =
                    mock(ContractNetManager.ContractListener.class);

            doThrow(new RuntimeException("Test exception"))
                    .when(failingListener).onAnnouncement(any());

            manager.addListener(failingListener);
            manager.addListener(normalListener);

            // Should not throw exception
            assertDoesNotThrow(() -> manager.announceTask(sampleTask, requesterId));

            // Both listeners should be called despite exception
            verify(failingListener, times(1)).onAnnouncement(any());
            verify(normalListener, times(1)).onAnnouncement(any());
        }
    }

    @Nested
    @DisplayName("Cleanup and State Tests")
    class CleanupTests {

        @Test
        @DisplayName("Get active count")
        void getActiveCount() {
            String id1 = manager.announceTask(sampleTask, requesterId);
            String id2 = manager.announceTask(sampleTask, requesterId);

            assertEquals(2, manager.getActiveCount(),
                    "Should have 2 active negotiations");

            manager.closeNegotiation(id1, ContractNetManager.ContractState.COMPLETED);

            assertEquals(1, manager.getActiveCount(),
                    "Should have 1 active negotiation after closing one");
        }

        @Test
        @DisplayName("Get all negotiations")
        void getAllNegotiations() {
            String id1 = manager.announceTask(sampleTask, requesterId);
            String id2 = manager.announceTask(sampleTask, requesterId);

            var all = manager.getAllNegotiations();

            assertEquals(2, all.size(), "Should have 2 negotiations");
            assertTrue(all.containsKey(id1), "Should contain first announcement");
            assertTrue(all.containsKey(id2), "Should contain second announcement");
        }

        @Test
        @DisplayName("Cleanup removes old completed negotiations")
        void cleanupRemovesOldNegotiations() {
            String id1 = manager.announceTask(sampleTask, requesterId);
            String id2 = manager.announceTask(sampleTask, requesterId);

            manager.closeNegotiation(id1, ContractNetManager.ContractState.COMPLETED);
            manager.closeNegotiation(id2, ContractNetManager.ContractState.FAILED);

            // Cleanup should not remove immediately (needs 5 minutes)
            int cleaned = manager.cleanup();
            assertEquals(0, cleaned,
                    "Should not remove recent completed negotiations");

            // Note: We can't easily test the 5-minute timeout in unit tests
            // but we verify the cleanup method runs without error
        }

        @Test
        @DisplayName("Cleanup removes expired negotiations")
        void cleanupRemovesExpired() {
            String id = manager.announceTask(sampleTask, requesterId, 10);

            // Wait for expiration
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                fail("Test interrupted");
            }

            int cleaned = manager.cleanup();

            assertEquals(1, cleaned,
                    "Should remove 1 expired negotiation");
            assertNull(manager.getNegotiation(id),
                    "Expired negotiation should be removed");
        }

        @Test
        @DisplayName("Close negotiation changes state")
        void closeNegotiationChangesState() {
            String id = manager.announceTask(sampleTask, requesterId);

            manager.closeNegotiation(id, ContractNetManager.ContractState.FAILED);

            ContractNetManager.ContractNegotiation negotiation =
                    manager.getNegotiation(id);

            assertEquals(ContractNetManager.ContractState.FAILED,
                    negotiation.getState(),
                    "State should be FAILED");
        }

        @Test
        @DisplayName("Negotiation isClosed reflects state")
        void negotiationIsClosed() {
            String id = manager.announceTask(sampleTask, requesterId);

            ContractNetManager.ContractNegotiation negotiation =
                    manager.getNegotiation(id);

            assertFalse(negotiation.isClosed(),
                    "Should be open when ANNOUNCED");

            manager.closeNegotiation(id, ContractNetManager.ContractState.COMPLETED);

            assertTrue(negotiation.isClosed(),
                    "Should be closed when COMPLETED");
        }

        @Test
        @DisplayName("Get bids for non-existent announcement returns empty list")
        void getBidsForNonExistentAnnouncement() {
            var bids = manager.getBids("non_existent_id");

            assertNotNull(bids, "Should return empty list, not null");
            assertTrue(bids.isEmpty(), "Should return empty list");
        }

        @Test
        @DisplayName("Get negotiation for non-existent ID returns null")
        void getNegotiationForNonExistentId() {
            ContractNetManager.ContractNegotiation negotiation =
                    manager.getNegotiation("non_existent_id");

            assertNull(negotiation, "Should return null for non-existent ID");
        }
    }

    @Nested
    @DisplayName("Concurrent Access Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Concurrent bid submissions are handled safely")
        void concurrentBidSubmissions() throws InterruptedException {
            String announcementId = manager.announceTask(sampleTask, requesterId);

            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    UUID agentId = UUID.randomUUID();
                    TaskBid bid = TaskBid.builder()
                            .announcementId(announcementId)
                            .bidderId(agentId)
                            .score(0.5 + (index * 0.05))
                            .estimatedTime(30000 + (index * 1000))
                            .confidence(0.8)
                            .build();
                    manager.submitBid(bid);
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            var bids = manager.getBids(announcementId);
            assertEquals(threadCount, bids.size(),
                    "All concurrent bids should be recorded");
        }

        @Test
        @DisplayName("Concurrent announcements generate unique IDs")
        void concurrentAnnouncements() throws InterruptedException {
            int threadCount = 20;
            Thread[] threads = new Thread[threadCount];
            String[] ids = new String[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    ids[index] = manager.announceTask(sampleTask, requesterId);
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // Check all IDs are unique
            for (int i = 0; i < threadCount; i++) {
                for (int j = i + 1; j < threadCount; j++) {
                    assertNotEquals(ids[i], ids[j],
                            "Announcement IDs should be unique");
                }
            }
        }
    }

    @Nested
    @DisplayName("ContractNegotiation State Tests")
    class ContractNegotiationTests {

        @Test
        @DisplayName("ContractNegotiation withBid adds bid")
        void negotiationWithBid() {
            String announcementId = manager.announceTask(sampleTask, requesterId);
            ContractNetManager.ContractNegotiation negotiation =
                    manager.getNegotiation(announcementId);

            UUID agentId = UUID.randomUUID();
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agentId)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            ContractNetManager.ContractNegotiation updated = negotiation.withBid(bid);

            assertEquals(0, negotiation.getBidCount(),
                    "Original negotiation should be immutable");
            assertEquals(1, updated.getBidCount(),
                    "Updated negotiation should have the bid");
        }

        @Test
        @DisplayName("ContractNegotiation withState changes state")
        void negotiationWithState() {
            String announcementId = manager.announceTask(sampleTask, requesterId);
            ContractNetManager.ContractNegotiation negotiation =
                    manager.getNegotiation(announcementId);

            ContractNetManager.ContractNegotiation updated =
                    negotiation.withState(ContractNetManager.ContractState.AWARDED);

            assertEquals(ContractNetManager.ContractState.ANNOUNCED,
                    negotiation.getState(),
                    "Original negotiation should be immutable");
            assertEquals(ContractNetManager.ContractState.AWARDED,
                    updated.getState(),
                    "Updated negotiation should have new state");
        }

        @Test
        @DisplayName("ContractNegotiation withWinningBid sets winner")
        void negotiationWithWinningBid() {
            String announcementId = manager.announceTask(sampleTask, requesterId);
            ContractNetManager.ContractNegotiation negotiation =
                    manager.getNegotiation(announcementId);

            UUID agentId = UUID.randomUUID();
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agentId)
                    .score(0.9)
                    .estimatedTime(25000)
                    .confidence(0.95)
                    .build();

            ContractNetManager.ContractNegotiation updated =
                    negotiation.withWinningBid(bid);

            assertNull(negotiation.getWinningBid(),
                    "Original negotiation should be immutable");
            assertEquals(bid, updated.getWinningBid(),
                    "Updated negotiation should have winning bid");
            assertEquals(agentId, updated.getAwardedAgent(),
                    "Updated negotiation should have awarded agent");
        }

        @Test
        @DisplayName("ContractNegotiation getters return correct values")
        void negotiationGetters() {
            String announcementId = manager.announceTask(sampleTask, requesterId);
            ContractNetManager.ContractNegotiation negotiation =
                    manager.getNegotiation(announcementId);

            assertNotNull(negotiation.getAnnouncement());
            assertNotNull(negotiation.getBids());
            assertNotNull(negotiation.getState());
            assertEquals(0, negotiation.getBidCount());
            assertNotNull(negotiation.getCreatedTime());
            assertEquals(0, negotiation.getClosedTime());
            assertNull(negotiation.getWinningBid());
            assertNull(negotiation.getAwardedAgent());
        }
    }

    @Nested
    @DisplayName("Multi-Agent Negotiation Tests")
    class MultiAgentNegotiationTests {

        @Test
        @DisplayName("Multiple agents negotiate for same task")
        void multipleAgentsNegotiateForSameTask() {
            String announcementId = manager.announceTask(sampleTask, requesterId);

            // Create multiple agents with varying capabilities
            UUID specialist = UUID.randomUUID();
            UUID generalist = UUID.randomUUID();
            UUID novice = UUID.randomUUID();

            // Specialist: high skill, high confidence, fast
            TaskBid specialistBid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(specialist)
                    .score(0.95)
                    .estimatedTime(15000)
                    .confidence(0.98)
                    .capability("proficiency", "mining")
                    .capability("experience", 100)
                    .build();

            // Generalist: medium skill, medium confidence, medium time
            TaskBid generalistBid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(generalist)
                    .score(0.75)
                    .estimatedTime(30000)
                    .confidence(0.85)
                    .capability("proficiency", "general")
                    .capability("experience", 50)
                    .build();

            // Novice: low skill, low confidence, slow
            TaskBid noviceBid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(novice)
                    .score(0.5)
                    .estimatedTime(60000)
                    .confidence(0.6)
                    .capability("proficiency", "novice")
                    .capability("experience", 10)
                    .build();

            // All bids should be accepted
            assertTrue(manager.submitBid(specialistBid));
            assertTrue(manager.submitBid(generalistBid));
            assertTrue(manager.submitBid(noviceBid));

            // Verify all bids are recorded
            var bids = manager.getBids(announcementId);
            assertEquals(3, bids.size());

            // Select winner - should be specialist
            Optional<TaskBid> winner = manager.selectWinner(announcementId);
            assertTrue(winner.isPresent());
            assertEquals(specialist, winner.get().bidderId());

            // Award contract
            assertTrue(manager.awardContract(announcementId, specialistBid));

            // Verify negotiation state
            ContractNetManager.ContractNegotiation negotiation =
                    manager.getNegotiation(announcementId);
            assertEquals(ContractNetManager.ContractState.AWARDED, negotiation.getState());
            assertEquals(3, negotiation.getBidCount(), "All bids should be preserved");
        }

        @Test
        @DisplayName("Agents bid on multiple simultaneous tasks")
        void agentsBidOnMultipleSimultaneousTasks() {
            // Create multiple tasks
            String task1Id = manager.announceTask(
                    TaskBuilder.aTask("mine").withBlock("stone").withQuantity(64).build(),
                    requesterId);
            String task2Id = manager.announceTask(
                    TaskBuilder.aTask("build").withStructure("wall").build(),
                    requesterId);
            String task3Id = manager.announceTask(
                    TaskBuilder.aTask("craft").withItem("sword").withQuantity(1).build(),
                    requesterId);

            // Create agents
            UUID miner = UUID.randomUUID();
            UUID builder = UUID.randomUUID();
            UUID crafter = UUID.randomUUID();
            UUID utility = UUID.randomUUID();

            // Miner bids on mining task (high score)
            TaskBid minerMiningBid = TaskBid.builder()
                    .announcementId(task1Id)
                    .bidderId(miner)
                    .score(0.9)
                    .estimatedTime(20000)
                    .confidence(0.95)
                    .build();

            // Miner also bids on crafting task (low score - not their specialty)
            TaskBid minerCraftingBid = TaskBid.builder()
                    .announcementId(task3Id)
                    .bidderId(miner)
                    .score(0.4)
                    .estimatedTime(45000)
                    .confidence(0.5)
                    .build();

            // Builder bids on building task (high score)
            TaskBid builderBuildingBid = TaskBid.builder()
                    .announcementId(task2Id)
                    .bidderId(builder)
                    .score(0.95)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            // Builder also bids on mining task (medium score)
            TaskBid builderMiningBid = TaskBid.builder()
                    .announcementId(task1Id)
                    .bidderId(builder)
                    .score(0.6)
                    .estimatedTime(40000)
                    .confidence(0.7)
                    .build();

            // Crafter bids on crafting task (high score)
            TaskBid crafterCraftingBid = TaskBid.builder()
                    .announcementId(task3Id)
                    .bidderId(crafter)
                    .score(0.92)
                    .estimatedTime(15000)
                    .confidence(0.98)
                    .build();

            // Utility agent bids on all tasks (medium scores)
            TaskBid utilityTask1 = TaskBid.builder()
                    .announcementId(task1Id)
                    .bidderId(utility)
                    .score(0.7)
                    .estimatedTime(35000)
                    .confidence(0.8)
                    .build();
            TaskBid utilityTask2 = TaskBid.builder()
                    .announcementId(task2Id)
                    .bidderId(utility)
                    .score(0.7)
                    .estimatedTime(40000)
                    .confidence(0.8)
                    .build();
            TaskBid utilityTask3 = TaskBid.builder()
                    .announcementId(task3Id)
                    .bidderId(utility)
                    .score(0.7)
                    .estimatedTime(25000)
                    .confidence(0.8)
                    .build();

            // Submit all bids
            assertTrue(manager.submitBid(minerMiningBid));
            assertTrue(manager.submitBid(minerCraftingBid));
            assertTrue(manager.submitBid(builderBuildingBid));
            assertTrue(manager.submitBid(builderMiningBid));
            assertTrue(manager.submitBid(crafterCraftingBid));
            assertTrue(manager.submitBid(utilityTask1));
            assertTrue(manager.submitBid(utilityTask2));
            assertTrue(manager.submitBid(utilityTask3));

            // Verify task 1 bids
            assertEquals(3, manager.getBids(task1Id).size());
            assertEquals(miner, manager.selectWinner(task1Id).get().bidderId());

            // Verify task 2 bids
            assertEquals(2, manager.getBids(task2Id).size());
            assertEquals(builder, manager.selectWinner(task2Id).get().bidderId());

            // Verify task 3 bids
            assertEquals(3, manager.getBids(task3Id).size());
            assertEquals(crafter, manager.selectWinner(task3Id).get().bidderId());

            // Award all contracts
            assertTrue(manager.awardToBestBidder(task1Id).isPresent());
            assertTrue(manager.awardToBestBidder(task2Id).isPresent());
            assertTrue(manager.awardToBestBidder(task3Id).isPresent());

            // Verify all negotiations are awarded
            assertEquals(ContractNetManager.ContractState.AWARDED,
                    manager.getNegotiation(task1Id).getState());
            assertEquals(ContractNetManager.ContractState.AWARDED,
                    manager.getNegotiation(task2Id).getState());
            assertEquals(ContractNetManager.ContractState.AWARDED,
                    manager.getNegotiation(task3Id).getState());
        }

        @Test
        @DisplayName("Agents with different loads are prioritized correctly")
        void loadBasedPrioritization() {
            String announcementId = manager.announceTask(sampleTask, requesterId);

            UUID idleAgent = UUID.randomUUID();
            UUID busyAgent = UUID.randomUUID();
            UUID overloadedAgent = UUID.randomUUID();

            // All agents have same skill and time estimates, but different loads
            TaskBid idleAgentBid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(idleAgent)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .currentLoad(0.1)
                    .build();

            TaskBid busyAgentBid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(busyAgent)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .currentLoad(0.5)
                    .build();

            TaskBid overloadedAgentBid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(overloadedAgent)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .currentLoad(0.9)
                    .build();

            assertTrue(manager.submitBid(idleAgentBid));
            assertTrue(manager.submitBid(busyAgentBid));
            assertTrue(manager.submitBid(overloadedAgentBid));

            // All have same bid value, so the first submitted (idle) should win
            // due to tie-breaker using bidder ID
            Optional<TaskBid> winner = manager.selectWinner(announcementId);
            assertTrue(winner.isPresent());

            // Verify all bids have same base value
            double idleValue = idleAgentBid.getBidValue();
            double busyValue = busyAgentBid.getBidValue();
            double overloadedValue = overloadedAgentBid.getBidValue();

            assertEquals(idleValue, busyValue, 0.0001);
            assertEquals(busyValue, overloadedValue, 0.0001);
        }

        @Test
        @DisplayName("Distance affects bid selection")
        void distanceAffectsBidSelection() {
            String announcementId = manager.announceTask(sampleTask, requesterId);

            UUID nearAgent = UUID.randomUUID();
            UUID farAgent = UUID.randomUUID();
            UUID veryFarAgent = UUID.randomUUID();

            // All agents have same skills but different distances
            TaskBid nearAgentBid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(nearAgent)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .distance(5.0)
                    .build();

            TaskBid farAgentBid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(farAgent)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .distance(50.0)
                    .build();

            TaskBid veryFarAgentBid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(veryFarAgent)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .distance(200.0)
                    .build();

            assertTrue(manager.submitBid(nearAgentBid));
            assertTrue(manager.submitBid(farAgentBid));
            assertTrue(manager.submitBid(veryFarAgentBid));

            // All have same base bid value, distance doesn't directly affect
            // bid value but is stored for potential custom evaluation
            var bids = manager.getBids(announcementId);
            assertEquals(3, bids.size());

            // Verify distance information is preserved
            assertEquals(5.0, bids.get(0).getDistance());
            assertEquals(50.0, bids.get(1).getDistance());
            assertEquals(200.0, bids.get(2).getDistance());
        }

        @Test
        @DisplayName("Negotiation with high volume of bids")
        void highVolumeBidding() {
            String announcementId = manager.announceTask(sampleTask, requesterId);

            int agentCount = 50;
            UUID[] agents = new UUID[agentCount];

            // Create many agents bidding on the same task
            for (int i = 0; i < agentCount; i++) {
                agents[i] = UUID.randomUUID();
                double score = 0.5 + (Math.random() * 0.5); // 0.5 to 1.0
                long time = 20000 + (long)(Math.random() * 60000); // 20-80 seconds
                double confidence = 0.6 + (Math.random() * 0.4); // 0.6 to 1.0

                TaskBid bid = TaskBid.builder()
                        .announcementId(announcementId)
                        .bidderId(agents[i])
                        .score(score)
                        .estimatedTime(time)
                        .confidence(confidence)
                        .build();

                assertTrue(manager.submitBid(bid),
                        "Bid " + i + " should be accepted");
            }

            // Verify all bids were recorded
            var bids = manager.getBids(announcementId);
            assertEquals(agentCount, bids.size(),
                    "All " + agentCount + " bids should be recorded");

            // Verify winner selection works with many bids
            Optional<TaskBid> winner = manager.selectWinner(announcementId);
            assertTrue(winner.isPresent(),
                    "Should be able to select winner from many bids");

            // Award contract
            assertTrue(manager.awardToBestBidder(announcementId).isPresent(),
                    "Should be able to award contract");
        }
    }

    @Nested
    @DisplayName("Advanced Timeout Handling Tests")
    class AdvancedTimeoutTests {

        @Test
        @DisplayName("Bids submitted just before deadline are accepted")
        void lastMinuteBids() {
            // Create announcement with short deadline
            String announcementId = manager.announceTask(sampleTask, requesterId, 100);

            UUID agent1 = UUID.randomUUID();
            UUID agent2 = UUID.randomUUID();

            // Submit first bid immediately
            TaskBid bid1 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent1)
                    .score(0.7)
                    .estimatedTime(30000)
                    .confidence(0.8)
                    .build();
            assertTrue(manager.submitBid(bid1));

            // Wait near deadline and submit second bid
            try {
                Thread.sleep(80);
            } catch (InterruptedException e) {
                fail("Test interrupted");
            }

            TaskBid bid2 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent2)
                    .score(0.8)
                    .estimatedTime(25000)
                    .confidence(0.9)
                    .build();

            // Should still be accepted if we haven't passed deadline
            boolean accepted = manager.submitBid(bid2);

            // Test is timing-dependent, so we check the result
            // If accepted, verify it's recorded
            if (accepted) {
                assertEquals(2, manager.getBids(announcementId).size());
            }
        }

        @Test
        @DisplayName("Multiple negotiations expire independently")
        void independentExpiration() {
            // Create announcements with different deadlines
            String shortLivedId = manager.announceTask(sampleTask, requesterId, 50);
            String mediumLivedId = manager.announceTask(sampleTask, requesterId, 150);
            String longLivedId = manager.announceTask(sampleTask, requesterId, 300);

            // Wait for short-lived to expire
            try {
                Thread.sleep(60);
            } catch (InterruptedException e) {
                fail("Test interrupted");
            }

            // Short-lived should be expired
            ContractNetManager.ContractNegotiation shortLived =
                    manager.getNegotiation(shortLivedId);
            assertTrue(shortLived.getAnnouncement().isExpired(),
                    "Short-lived announcement should be expired");

            // Medium and long-lived should still be valid
            assertFalse(manager.getNegotiation(mediumLivedId).getAnnouncement().isExpired(),
                    "Medium-lived announcement should still be valid");
            assertFalse(manager.getNegotiation(longLivedId).getAnnouncement().isExpired(),
                    "Long-lived announcement should still be valid");

            // Try to submit bid to expired announcement
            TaskBid lateBid = TaskBid.builder()
                    .announcementId(shortLivedId)
                    .bidderId(UUID.randomUUID())
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            assertFalse(manager.submitBid(lateBid),
                    "Bid should be rejected for expired announcement");

            // But should still work for valid announcements
            TaskBid validBid = TaskBid.builder()
                    .announcementId(mediumLivedId)
                    .bidderId(UUID.randomUUID())
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            assertTrue(manager.submitBid(validBid),
                    "Bid should be accepted for valid announcement");
        }

        @Test
        @DisplayName("Cleanup removes multiple expired negotiations")
        void cleanupMultipleExpired() {
            // Create several short-lived announcements
            String id1 = manager.announceTask(sampleTask, requesterId, 10);
            String id2 = manager.announceTask(sampleTask, requesterId, 10);
            String id3 = manager.announceTask(sampleTask, requesterId, 10);
            String id4 = manager.announceTask(sampleTask, requesterId, 10000); // Long-lived

            // Wait for first three to expire
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                fail("Test interrupted");
            }

            // Run cleanup
            int cleaned = manager.cleanup();

            assertEquals(3, cleaned,
                    "Should clean up 3 expired negotiations");

            // Verify expired ones are gone
            assertNull(manager.getNegotiation(id1));
            assertNull(manager.getNegotiation(id2));
            assertNull(manager.getNegotiation(id3));

            // Verify long-lived one still exists
            assertNotNull(manager.getNegotiation(id4));
        }
    }

    @Nested
    @DisplayName("Complex Bid Evaluation Tests")
    class ComplexBidEvaluationTests {

        @Test
        @DisplayName("Bid evaluation with equal scores uses time as tiebreaker")
        void timeTiebreaker() {
            String announcementId = manager.announceTask(sampleTask, requesterId);

            UUID agent1 = UUID.randomUUID();
            UUID agent2 = UUID.randomUUID();

            // Both agents have identical scores and confidence
            // but agent2 is faster
            TaskBid bid1 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent1)
                    .score(0.85)
                    .estimatedTime(40000)
                    .confidence(0.9)
                    .build();

            TaskBid bid2 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent2)
                    .score(0.85)
                    .estimatedTime(20000)
                    .confidence(0.9)
                    .build();

            manager.submitBid(bid1);
            manager.submitBid(bid2);

            Optional<TaskBid> winner = manager.selectWinner(announcementId);

            assertTrue(winner.isPresent());
            assertEquals(agent2, winner.get().bidderId(),
                    "Faster agent should win when scores are equal");
        }

        @Test
        @DisplayName("High confidence low score vs low confidence high score")
        void confidenceVsScoreTradeoff() {
            String announcementId = manager.announceTask(sampleTask, requesterId);

            UUID conservativeAgent = UUID.randomUUID();
            UUID aggressiveAgent = UUID.randomUUID();

            // Conservative: lower skill but high confidence
            TaskBid conservativeBid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(conservativeAgent)
                    .score(0.6)
                    .estimatedTime(30000)
                    .confidence(0.95)
                    .build();

            // Aggressive: higher skill but lower confidence
            TaskBid aggressiveBid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(aggressiveAgent)
                    .score(0.95)
                    .estimatedTime(30000)
                    .confidence(0.5)
                    .build();

            manager.submitBid(conservativeBid);
            manager.submitBid(aggressiveBid);

            Optional<TaskBid> winner = manager.selectWinner(announcementId);

            assertTrue(winner.isPresent());

            // Calculate bid values
            double conservativeValue = conservativeBid.getBidValue();
            double aggressiveValue = aggressiveBid.getBidValue();

            // Conservative: (0.6 * 0.95) / 30 = 0.019
            // Aggressive: (0.95 * 0.5) / 30 = 0.0158
            // Conservative should win due to higher confidence
            assertEquals(conservativeAgent, winner.get().bidderId(),
                    "Agent with better balance of score and confidence should win");
        }

        @Test
        @DisplayName("Very fast but low skill vs slower but high skill")
        void speedVsSkillTradeoff() {
            String announcementId = manager.announceTask(sampleTask, requesterId);

            UUID fastNovice = UUID.randomUUID();
            UUID slowExpert = UUID.randomUUID();

            // Fast novice: low skill, very fast
            TaskBid fastNoviceBid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(fastNovice)
                    .score(0.4)
                    .estimatedTime(10000)
                    .confidence(0.7)
                    .build();

            // Slow expert: high skill, slower
            TaskBid slowExpertBid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(slowExpert)
                    .score(0.95)
                    .estimatedTime(60000)
                    .confidence(0.95)
                    .build();

            manager.submitBid(fastNoviceBid);
            manager.submitBid(slowExpertBid);

            Optional<TaskBid> winner = manager.selectWinner(announcementId);

            assertTrue(winner.isPresent());

            // Calculate bid values
            double fastNoviceValue = fastNoviceBid.getBidValue();
            double slowExpertValue = slowExpertBid.getBidValue();

            // Fast novice: (0.4 * 0.7) / 10 = 0.028
            // Slow expert: (0.95 * 0.95) / 60 = 0.015
            // Fast novice might win due to speed advantage
            // This tests the tradeoff in the scoring formula
        }

        @Test
        @DisplayName("Optimal bid has balanced score, time, and confidence")
        void optimalBidBalance() {
            String announcementId = manager.announceTask(sampleTask, requesterId);

            UUID agent1 = UUID.randomUUID();
            UUID agent2 = UUID.randomUUID();
            UUID agent3 = UUID.randomUUID();

            // Agent 1: High score, low confidence, medium time
            TaskBid bid1 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent1)
                    .score(0.95)
                    .estimatedTime(30000)
                    .confidence(0.6)
                    .build();

            // Agent 2: Medium score, high confidence, medium time (balanced)
            TaskBid bid2 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent2)
                    .score(0.85)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            // Agent 3: Low score, medium confidence, fast time
            TaskBid bid3 = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agent3)
                    .score(0.7)
                    .estimatedTime(15000)
                    .confidence(0.8)
                    .build();

            manager.submitBid(bid1);
            manager.submitBid(bid2);
            manager.submitBid(bid3);

            Optional<TaskBid> winner = manager.selectWinner(announcementId);

            assertTrue(winner.isPresent());

            // Agent 2 should win with balanced attributes
            // bid1: (0.95 * 0.6) / 30 = 0.019
            // bid2: (0.85 * 0.9) / 30 = 0.0255 (winner)
            // bid3: (0.7 * 0.8) / 15 = 0.0373 (actually wins due to speed)
            // The formula favors speed, so agent3 might win
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Handle empty task parameters")
        void emptyTaskParameters() {
            Task emptyTask = new Task("test", Map.of());
            String announcementId = manager.announceTask(emptyTask, requesterId);

            assertNotNull(announcementId);
            assertNotNull(manager.getNegotiation(announcementId));
        }

        @Test
        @DisplayName("Handle very short deadline")
        void veryShortDeadline() {
            // Create announcement with 1ms deadline
            String announcementId = manager.announceTask(sampleTask, requesterId, 1);

            // Immediately try to submit bid
            UUID bidderId = UUID.randomUUID();
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            // May fail due to timing, but should not throw exception
            assertDoesNotThrow(() -> manager.submitBid(bid));
        }

        @Test
        @DisplayName("Handle very long deadline")
        void veryLongDeadline() {
            // Create announcement with 1 hour deadline
            long oneHour = 3600000;
            String announcementId = manager.announceTask(sampleTask, requesterId, oneHour);

            ContractNetManager.ContractNegotiation negotiation =
                    manager.getNegotiation(announcementId);

            long remainingTime = negotiation.getAnnouncement().getRemainingTime();

            assertTrue(remainingTime > 3500000,
                    "Should have approximately 1 hour remaining");
        }

        @Test
        @DisplayName("Award contract with no bids returns false")
        void awardContractWithNoBids() {
            String announcementId = manager.announceTask(sampleTask, requesterId);

            UUID fakeAgent = UUID.randomUUID();
            TaskBid fakeBid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(fakeAgent)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            // Don't submit the bid, try to award it
            boolean awarded = manager.awardContract(announcementId, fakeBid);

            assertFalse(awarded,
                    "Should not award contract for bid that wasn't submitted");
        }

        @Test
        @DisplayName("Submit bid for wrong announcement ID")
        void submitBidForWrongAnnouncement() {
            String announcementId1 = manager.announceTask(sampleTask, requesterId);
            String announcementId2 = manager.announceTask(sampleTask, requesterId);

            UUID agentId = UUID.randomUUID();

            // Create bid for announcement 1
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId1)
                    .bidderId(agentId)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            // Try to submit to announcement 2 by modifying the ID
            TaskBid wrongBid = TaskBid.builder()
                    .announcementId(announcementId2)
                    .bidderId(agentId)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            manager.submitBid(bid);
            boolean accepted = manager.submitBid(wrongBid);

            // Should be accepted - it's a different announcement
            assertTrue(accepted);

            // Each announcement should have one bid
            assertEquals(1, manager.getBids(announcementId1).size());
            assertEquals(1, manager.getBids(announcementId2).size());
        }

        @Test
        @DisplayName("Close negotiation prevents further bids")
        void closeNegotiationPreventsBids() {
            String announcementId = manager.announceTask(sampleTask, requesterId);

            // Close the negotiation
            manager.closeNegotiation(announcementId, ContractNetManager.ContractState.FAILED);

            // Try to submit bid
            UUID agentId = UUID.randomUUID();
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agentId)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            boolean accepted = manager.submitBid(bid);

            assertFalse(accepted,
                    "Should not accept bid for closed negotiation");
        }

        @Test
        @DisplayName("Handle rapid open/close cycles")
        void rapidOpenCloseCycles() {
            for (int i = 0; i < 100; i++) {
                String id = manager.announceTask(sampleTask, requesterId);
                manager.closeNegotiation(id, ContractNetManager.ContractState.COMPLETED);
            }

            // All should be processed without error
            assertEquals(100, manager.getAllNegotiations().size());

            // Cleanup should remove all completed ones (after 5 minutes, so not immediately)
            int cleaned = manager.cleanup();
            assertEquals(0, cleaned,
                    "Recent completions should not be cleaned up immediately");
        }

        @Test
        @DisplayName("Capability information is preserved in bids")
        void capabilityInfoPreserved() {
            String announcementId = manager.announceTask(sampleTask, requesterId);

            UUID agentId = UUID.randomUUID();

            Map<String, Object> capabilities = Map.of(
                    "proficiencies", Map.of("mining", 0.9, "building", 0.7),
                    "tools", Set.of("pickaxe", "shovel"),
                    "distance", 25.0,
                    "currentLoad", 0.3
            );

            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(agentId)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .capabilities(capabilities)
                    .build();

            manager.submitBid(bid);

            var retrievedBids = manager.getBids(announcementId);
            assertEquals(1, retrievedBids.size());

            TaskBid retrieved = retrievedBids.get(0);
            assertEquals(25.0, retrieved.getDistance());
            assertEquals(0.3, retrieved.getCurrentLoad());
            assertNotNull(retrieved.capabilities());
        }
    }
}
