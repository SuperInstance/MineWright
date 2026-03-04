package com.minewright.coordination;

import com.minewright.testutil.TestLogger;

import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Manages the Contract Net Protocol bidding process for multi-agent task allocation.
 *
 * <p><b>Contract Net Protocol Flow:</b></p>
 * <pre>
 * Manager                Agent
 *   |                      |
 *   |-- announceTask ----->|
 *   |                      |-- evaluate capability
 *   |                      |
 *   |<-- submitBid --------|
 *   |                      |
 *   |-- evaluate all bids  |
 *   |                      |
 *   |-- awardContract ---->| (winner only)
 *   |                      |-- execute task
 *   |<-- taskComplete -----|
 * </pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Uses ConcurrentHashMap for thread-safe negotiation storage</li>
 *   <li>Synchronized blocks for bid submission and contract awarding</li>
 *   <li>Safe for concurrent access from multiple agents</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * // Create manager
 * ContractNetManager manager = new ContractNetManager();
 *
 * // Add listener for events
 * manager.addListener(new ContractListener() {
 *     public void onAnnouncement(TaskAnnouncement announcement) {
 *         // Notify agents of new task
 *     }
 *
 *     public void onContractAwarded(String id, TaskBid winner) {
 *         // Winner can start executing
 *     }
 * });
 *
 * // Manager announces task
 * Task task = new Task("mine", Map.of("block", "iron_ore"));
 * String announcementId = manager.announceTask(task, managerUUID, 30000);
 *
 * // Agents submit bids
 * TaskBid bid1 = TaskBid.builder()
 *     .announcementId(announcementId)
 *     .bidderId(agent1UUID)
 *     .score(0.95)
 *     .estimatedTime(5000)
 *     .build();
 * manager.submitBid(bid1);
 *
 * // Manager awards to best bidder
 * Optional<TaskBid> winner = manager.awardToBestBidder(announcementId);
 * }</pre>
 *
 * @see TaskAnnouncement
 * @see TaskBid
 * @see ContractNegotiation
 * @see ContractListener
 * @see com.minewright.action.Task
 *
 * @since 1.3.0
 */
public class ContractNetManager {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(ContractNetManager.class);

    /**
     * State of a contract negotiation.
     */
    public enum ContractState {
        /** Announcement created, waiting for bids */
        ANNOUNCED,
        /** Bidding closed, evaluating winner */
        EVALUATING,
        /** Contract awarded, agent executing */
        AWARDED,
        /** Task completed successfully */
        COMPLETED,
        /** Task failed or cancelled */
        FAILED,
        /** Bidding deadline expired with no bids */
        EXPIRED
    }

    /**
     * Tracks the state of an active contract negotiation.
     */
    public static class ContractNegotiation {
        private final TaskAnnouncement announcement;
        private final List<TaskBid> bids;
        private final ContractState state;
        private final TaskBid winningBid;
        private final UUID awardedAgent;
        private final long createdTime;
        private final long closedTime;

        public ContractNegotiation(TaskAnnouncement announcement) {
            this(announcement, new ArrayList<>(), ContractState.ANNOUNCED, null, null,
                 System.currentTimeMillis(), 0);
        }

        private ContractNegotiation(
            TaskAnnouncement announcement,
            List<TaskBid> bids,
            ContractState state,
            TaskBid winningBid,
            UUID awardedAgent,
            long createdTime,
            long closedTime
        ) {
            this.announcement = announcement;
            this.bids = bids;
            this.state = state;
            this.winningBid = winningBid;
            this.awardedAgent = awardedAgent;
            this.createdTime = createdTime;
            this.closedTime = closedTime;
        }

        public TaskAnnouncement getAnnouncement() {
            return announcement;
        }

        public List<TaskBid> getBids() {
            return Collections.unmodifiableList(bids);
        }

        public ContractState getState() {
            return state;
        }

        public TaskBid getWinningBid() {
            return winningBid;
        }

        public UUID getAwardedAgent() {
            return awardedAgent;
        }

        public long getCreatedTime() {
            return createdTime;
        }

        public long getClosedTime() {
            return closedTime;
        }

        public int getBidCount() {
            return bids.size();
        }

        public boolean isClosed() {
            return state != ContractState.ANNOUNCED;
        }

        public ContractNegotiation withState(ContractState newState) {
            return new ContractNegotiation(announcement, bids, newState, winningBid,
                awardedAgent, createdTime,
                newState != ContractState.ANNOUNCED ? System.currentTimeMillis() : closedTime);
        }

        public ContractNegotiation withWinningBid(TaskBid winner) {
            return new ContractNegotiation(announcement, bids, state, winner,
                winner != null ? winner.bidderId() : awardedAgent, createdTime, closedTime);
        }

        public ContractNegotiation withBid(TaskBid bid) {
            List<TaskBid> newBids = new ArrayList<>(bids);
            newBids.add(bid);
            return new ContractNegotiation(announcement, newBids, state, winningBid,
                awardedAgent, createdTime, closedTime);
        }
    }

    /**
     * Listener for contract negotiation events.
     */
    public interface ContractListener {
        /**
         * Called when a new announcement is published.
         */
        default void onAnnouncement(TaskAnnouncement announcement) {}

        /**
         * Called when a bid is submitted.
         */
        default void onBidSubmitted(TaskBid bid) {}

        /**
         * Called when a contract is awarded.
         */
        default void onContractAwarded(String announcementId, TaskBid winner) {}

        /**
         * Called when a negotiation expires.
         */
        default void onNegotiationExpired(String announcementId) {}
    }

    private final Map<String, ContractNegotiation> negotiations;
    private final AtomicBoolean acceptingBids;
    private final List<ContractListener> listeners;
    private final Object negotiationLock;
    private final WorkloadTracker workloadTracker;
    private final AwardSelector awardSelector;

    /**
     * Flag indicating if the manager is shutdown.
     * Marked volatile for visibility across threads.
     */
    private volatile boolean isShutdown = false;

    /**
     * Creates a new Contract Net manager.
     */
    public ContractNetManager() {
        this.negotiations = new ConcurrentHashMap<>();
        this.acceptingBids = new AtomicBoolean(true);
        this.listeners = new ArrayList<>();
        this.negotiationLock = new Object();
        this.workloadTracker = new WorkloadTracker();
        this.awardSelector = new AwardSelector();
    }

    /**
     * Creates a new Contract Net manager with custom components.
     *
     * @param workloadTracker Custom workload tracker
     * @param awardSelector Custom award selector
     */
    public ContractNetManager(WorkloadTracker workloadTracker, AwardSelector awardSelector) {
        this.negotiations = new ConcurrentHashMap<>();
        this.acceptingBids = new AtomicBoolean(true);
        this.listeners = new ArrayList<>();
        this.negotiationLock = new Object();
        this.workloadTracker = workloadTracker != null ? workloadTracker : new WorkloadTracker();
        this.awardSelector = awardSelector != null ? awardSelector : new AwardSelector();
    }

    /**
     * Adds a listener for contract events.
     *
     * <p>Listeners are notified of announcements, bid submissions, contract awards,
     * and negotiation expirations. Listeners should be thread-safe as callbacks
     * may occur from multiple threads.</p>
     *
     * @param listener The listener to add (not null)
     */
    public void addListener(ContractListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a contract event listener.
     *
     * @param listener The listener to remove
     */
    public void removeListener(ContractListener listener) {
        listeners.remove(listener);
    }

    /**
     * Announces a task for bidding.
     *
     * <p>The announcement is stored and agents can submit bids until the deadline.
     * Bids are evaluated in order of submission.</p>
     *
     * @param task The task to be allocated
     * @param requesterId UUID of the agent requesting the task
     * @return The unique announcement ID for tracking bids
     */
    public String announceTask(com.minewright.action.Task task, UUID requesterId) {
        return announceTask(task, requesterId, 30000); // Default 30 second deadline
    }

    /**
     * Announces a task for bidding with a custom deadline.
     *
     * @param task The task to be allocated
     * @param requesterId UUID of the agent requesting the task
     * @param deadlineMs Duration until bidding deadline (milliseconds)
     * @return The unique announcement ID for tracking bids
     */
    public String announceTask(com.minewright.action.Task task, UUID requesterId, long deadlineMs) {
        long deadline = System.currentTimeMillis() + deadlineMs;

        TaskAnnouncement announcement = TaskAnnouncement.builder()
            .task(task)
            .requesterId(requesterId)
            .deadline(deadline)
            .build();

        negotiations.put(announcement.announcementId(), new ContractNegotiation(announcement));

        LOGGER.info("Task announced: {} (deadline in {}ms, action: {})",
            announcement.announcementId(), deadlineMs, task.getAction());

        // Notify listeners
        listeners.forEach(listener -> {
            try {
                listener.onAnnouncement(announcement);
            } catch (Exception e) {
                LOGGER.warn("Listener error in onAnnouncement", e);
            }
        });

        return announcement.announcementId();
    }

    /**
     * Submits a bid for an announced task.
     *
     * <p>Bids are only accepted if:
     * <ul>
     *   <li>The announcement exists and is still open</li>
     *   <li>The deadline has not passed</li>
     *   <li>The agent hasn't already bid on this announcement</li>
     * </ul>
     *
     * @param bid The bid to submit
     * @return true if the bid was accepted, false otherwise
     */
    public boolean submitBid(TaskBid bid) {
        if (bid == null) {
            return false;
        }

        String announcementId = bid.announcementId();
        ContractNegotiation negotiation = negotiations.get(announcementId);

        if (negotiation == null) {
            LOGGER.warn("Bid rejected: announcement not found: {}", announcementId);
            return false;
        }

        if (negotiation.isClosed()) {
            LOGGER.warn("Bid rejected: negotiation closed for: {}", announcementId);
            return false;
        }

        if (negotiation.getAnnouncement().isExpired()) {
            LOGGER.warn("Bid rejected: announcement expired for: {}", announcementId);
            closeNegotiation(announcementId, ContractState.EXPIRED);
            return false;
        }

        // Check for duplicate bids
        boolean alreadyBid = negotiation.getBids().stream()
            .anyMatch(b -> b.bidderId().equals(bid.bidderId()));

        if (alreadyBid) {
            LOGGER.warn("Bid rejected: agent {} already bid on {}",
                bid.bidderId(), announcementId);
            return false;
        }

        // Add the bid
        synchronized (negotiationLock) {
            ContractNegotiation current = negotiations.get(announcementId);
            if (current != null && !current.isClosed()) {
                negotiations.put(announcementId, current.withBid(bid));

                LOGGER.info("Bid received for {}: score={:.2f}, time={}ms, bidder={}",
                    announcementId, bid.score(), bid.estimatedTime(),
                    bid.bidderId().toString().substring(0, 8));

                // Notify listeners
                listeners.forEach(listener -> {
                    try {
                        listener.onBidSubmitted(bid);
                    } catch (Exception e) {
                        LOGGER.warn("Listener error in onBidSubmitted", e);
                    }
                });

                return true;
            }
        }

        return false;
    }

    /**
     * Gets all bids for an announcement.
     *
     * @param announcementId The announcement ID
     * @return Unmodifiable list of bids, or empty list if not found
     */
    public List<TaskBid> getBids(String announcementId) {
        ContractNegotiation negotiation = negotiations.get(announcementId);
        return negotiation != null ? negotiation.getBids() : List.of();
    }

    /**
     * Selects the winning bid for an announcement.
     *
     * <p>The winner is selected based on the bid value calculated in
     * {@link TaskBid#getBidValue()}. Higher values are better.</p>
     *
     * @param announcementId The announcement ID
     * @return Optional containing the best bid, or empty if no bids
     */
    public Optional<TaskBid> selectWinner(String announcementId) {
        ContractNegotiation negotiation = negotiations.get(announcementId);

        if (negotiation == null || negotiation.getBids().isEmpty()) {
            return Optional.empty();
        }

        // Sort bids by value (descending) and return the best
        return negotiation.getBids().stream()
            .max(TaskBid::compareTo);
    }

    /**
     * Awards the contract to the winning bidder.
     *
     * <p>This closes the negotiation to further bids and records the winner.
     * The winning agent can then begin executing the task.</p>
     *
     * @param announcementId The announcement ID
     * @param winner The winning bid
     * @return true if the contract was awarded successfully
     */
    public boolean awardContract(String announcementId, TaskBid winner) {
        ContractNegotiation negotiation = negotiations.get(announcementId);

        if (negotiation == null) {
            LOGGER.warn("Cannot award: announcement not found: {}", announcementId);
            return false;
        }

        if (negotiation.isClosed()) {
            LOGGER.warn("Cannot award: negotiation already closed for: {}", announcementId);
            return false;
        }

        // Verify the winner is actually in the bid list
        boolean validWinner = negotiation.getBids().stream()
            .anyMatch(b -> b.equals(winner));

        if (!validWinner) {
            LOGGER.warn("Cannot award: bid not in list for: {}", announcementId);
            return false;
        }

        synchronized (negotiationLock) {
            ContractNegotiation current = negotiations.get(announcementId);
            if (current != null && !current.isClosed()) {
                ContractNegotiation updated = current
                    .withWinningBid(winner)
                    .withState(ContractState.AWARDED);

                negotiations.put(announcementId, updated);

                LOGGER.info("Contract awarded: {} to agent {} (value={:.4f})",
                    announcementId, winner.bidderId().toString().substring(0, 8),
                    winner.getBidValue());

                // Notify listeners
                listeners.forEach(listener -> {
                    try {
                        listener.onContractAwarded(announcementId, winner);
                    } catch (Exception e) {
                        LOGGER.warn("Listener error in onContractAwarded", e);
                    }
                });

                return true;
            }
        }

        return false;
    }

    /**
     * Awards the contract to the best bidder automatically.
     *
     * <p>Convenience method that combines selectWinner and awardContract.</p>
     *
     * @param announcementId The announcement ID
     * @return Optional containing the awarded bid, or empty if no bids
     */
    public Optional<TaskBid> awardToBestBidder(String announcementId) {
        Optional<TaskBid> winner = selectWinner(announcementId);
        if (winner.isPresent() && awardContract(announcementId, winner.get())) {
            return winner;
        }
        return Optional.empty();
    }

    /**
     * Closes a negotiation with a specific state.
     *
     * @param announcementId The announcement ID
     * @param state The final state to set
     */
    public void closeNegotiation(String announcementId, ContractState state) {
        ContractNegotiation negotiation = negotiations.get(announcementId);

        if (negotiation != null && !negotiation.isClosed()) {
            synchronized (negotiationLock) {
                ContractNegotiation current = negotiations.get(announcementId);
                if (current != null && !current.isClosed()) {
                    negotiations.put(announcementId, current.withState(state));

                    if (state == ContractState.EXPIRED) {
                        listeners.forEach(listener -> {
                            try {
                                listener.onNegotiationExpired(announcementId);
                            } catch (Exception e) {
                                LOGGER.warn("Listener error in onNegotiationExpired", e);
                            }
                        });
                    }
                }
            }
        }
    }

    /**
     * Gets a negotiation by ID.
     *
     * @param announcementId The announcement ID
     * @return The negotiation, or null if not found
     */
    public ContractNegotiation getNegotiation(String announcementId) {
        return negotiations.get(announcementId);
    }

    /**
     * Cleans up expired and completed negotiations.
     *
     * @return Number of negotiations cleaned up
     */
    public int cleanup() {
        long now = System.currentTimeMillis();
        int cleaned = 0;

        Iterator<Map.Entry<String, ContractNegotiation>> it = negotiations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ContractNegotiation> entry = it.next();
            ContractNegotiation negotiation = entry.getValue();

            // Remove if completed/failed/expired for more than 5 minutes
            boolean shouldRemove = switch (negotiation.getState()) {
                case COMPLETED, FAILED, EXPIRED -> {
                    long age = now - negotiation.getClosedTime();
                    yield age > 300000; // 5 minutes
                }
                default -> {
                    // Also remove announced negotiations that are expired
                    yield negotiation.getAnnouncement().isExpired();
                }
            };

            if (shouldRemove) {
                it.remove();
                cleaned++;
            }
        }

        if (cleaned > 0) {
            LOGGER.debug("Cleaned up {} contract negotiations", cleaned);
        }

        return cleaned;
    }

    /**
     * Gets all active negotiations.
     *
     * @return Unmodifiable map of announcement ID to negotiation
     */
    public Map<String, ContractNegotiation> getAllNegotiations() {
        return Collections.unmodifiableMap(negotiations);
    }

    /**
     * Gets the number of active negotiations.
     *
     * @return Count of negotiations
     */
    public int getActiveCount() {
        return (int) negotiations.values().stream()
            .filter(n -> !n.isClosed() || n.getState() == ContractState.AWARDED)
            .count();
    }

    // ========== Workload Tracking Integration ==========

    /**
     * Gets the workload tracker for this manager.
     *
     * @return The workload tracker
     */
    public WorkloadTracker getWorkloadTracker() {
        return workloadTracker;
    }

    /**
     * Registers an agent for workload tracking.
     *
     * @param agentId Agent UUID
     * @param maxConcurrentTasks Maximum concurrent tasks for this agent
     * @return true if registered successfully
     */
    public boolean registerAgent(UUID agentId, int maxConcurrentTasks) {
        return workloadTracker.registerAgent(agentId, maxConcurrentTasks);
    }

    /**
     * Unregisters an agent from workload tracking.
     *
     * @param agentId Agent UUID
     * @return The removed workload, or null if not found
     */
    public WorkloadTracker.AgentWorkload unregisterAgent(UUID agentId) {
        return workloadTracker.unregisterAgent(agentId);
    }

    /**
     * Gets the current workload for an agent.
     *
     * @param agentId Agent UUID
     * @return Agent workload, or null if not registered
     */
    public WorkloadTracker.AgentWorkload getAgentWorkload(UUID agentId) {
        return workloadTracker.getWorkload(agentId);
    }

    /**
     * Gets the current load factor for an agent.
     *
     * @param agentId Agent UUID
     * @return Load factor (0.0-1.0), or 0.0 if not registered
     */
    public double getAgentLoad(UUID agentId) {
        return workloadTracker.getCurrentLoad(agentId);
    }

    /**
     * Checks if an agent is available for new tasks.
     *
     * @param agentId Agent UUID
     * @return true if available, false otherwise
     */
    public boolean isAgentAvailable(UUID agentId) {
        return workloadTracker.isAvailable(agentId);
    }

    /**
     * Assigns a task to an agent through the workload tracker.
     *
     * @param agentId Agent UUID
     * @param taskId Task identifier
     * @return true if task was assigned, false if at capacity or not registered
     */
    public boolean assignTaskToAgent(UUID agentId, String taskId) {
        return workloadTracker.assignTask(agentId, taskId);
    }

    /**
     * Marks a task as completed for an agent.
     *
     * @param agentId Agent UUID
     * @param taskId Task identifier
     * @param success Whether the task succeeded
     * @return true if task was marked completed, false if not found
     */
    public boolean completeAgentTask(UUID agentId, String taskId, boolean success) {
        return workloadTracker.completeTask(agentId, taskId, success);
    }

    // ========== Advanced Bid Selection ==========

    /**
     * Selects the best bid using workload-aware scoring.
     *
     * <p>This method uses the AwardSelector to evaluate bids with
     * workload-aware scoring, considering agent availability and
     * current load factors.</p>
     *
     * @param announcementId The announcement ID
     * @return Selection result with detailed scoring, or empty result if no bids
     */
    public Optional<AwardSelector.SelectionResult> selectWinnerDetailed(String announcementId) {
        ContractNegotiation negotiation = negotiations.get(announcementId);

        if (negotiation == null || negotiation.getBids().isEmpty()) {
            return Optional.empty();
        }

        // Enhance bids with current load information
        List<TaskBid> enhancedBids = negotiation.getBids().stream()
            .map(bid -> {
                double currentLoad = workloadTracker.getCurrentLoad(bid.bidderId());
                // Update the bid's current load if it differs
                if (Math.abs(bid.getCurrentLoad() - currentLoad) > 0.001) {
                    Map<String, Object> updatedCapabilities = new HashMap<>(bid.capabilities());
                    updatedCapabilities.put("currentLoad", currentLoad);
                    return TaskBid.builder()
                        .announcementId(bid.announcementId())
                        .bidderId(bid.bidderId())
                        .score(bid.score())
                        .estimatedTime(bid.estimatedTime())
                        .confidence(bid.confidence())
                        .capabilities(updatedCapabilities)
                        .build();
                }
                return bid;
            })
            .collect(Collectors.toList());

        AwardSelector.SelectionResult result = awardSelector.selectBestBidDetailed(enhancedBids, announcementId);

        return Optional.of(result);
    }

    /**
     * Awards the contract to the best bidder using detailed selection.
     *
     * <p>Convenience method that combines selectWinnerDetailed and awardContract.</p>
     *
     * @param announcementId The announcement ID
     * @return Optional containing the selection result, or empty if no bids
     */
    public Optional<AwardSelector.SelectionResult> awardToBestBidderDetailed(String announcementId) {
        Optional<AwardSelector.SelectionResult> resultOpt = selectWinnerDetailed(announcementId);

        if (resultOpt.isPresent() && resultOpt.get().hasWinner()) {
            AwardSelector.SelectionResult result = resultOpt.get();
            TaskBid winner = result.getSelectedBid();

            if (awardContract(announcementId, winner)) {
                return resultOpt;
            }
        }

        return Optional.empty();
    }

    /**
     * Gets the award selector used by this manager.
     *
     * @return The award selector
     */
    public AwardSelector getAwardSelector() {
        return awardSelector;
    }

    /**
     * Gets statistics about contract negotiations and workload.
     *
     * @return Map of statistic name to value
     */
    public Map<String, Object> getFullStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Negotiation stats
        stats.put("totalNegotiations", negotiations.size());
        stats.put("activeNegotiations", getActiveCount());

        // Award selector stats
        stats.putAll(awardSelector.getStatistics());

        // Workload stats
        stats.putAll(workloadTracker.getStatistics());

        return stats;
    }
}
