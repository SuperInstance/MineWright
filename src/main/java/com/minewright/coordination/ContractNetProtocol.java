package com.minewright.coordination;

import com.minewright.action.Task;
import com.minewright.testutil.TestLogger;
import com.minewright.event.EventBus;
import com.minewright.communication.AgentMessage;
import com.minewright.communication.AgentMessage.MessageType;

import org.slf4j.Logger;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Facade for the Contract Net Protocol implementation.
 *
 * <p><b>Contract Net Protocol Flow:</b></p>
 * <pre>
 * Manager                Agent
 *   |                      |
 *   |-- announceTask ----->| (1) Broadcast task announcement
 *   |                      |-- evaluate capability
 *   |                      |
 *   |<-- submitBid --------| (2) Agents submit bids
 *   |                      |
 *   |-- evaluate bids      | (3) Select best agent
 *   |-- awardContract ---->| (4) Notify winner
 *   |                      |-- execute task
 *   |<-- progressUpdate ---| (5) Track progress
 *   |<-- taskComplete -----| (6) Final report
 * </pre>
 *
 * <p><b>Integration Points:</b></p>
 * <ul>
 *   <li>Uses {@link EventBus} for task announcements</li>
 *   <li>Uses {@link AgentMessage} for inter-agent communication</li>
 *   <li>Uses {@link BidCollector} for bid management</li>
 *   <li>Uses {@link AwardSelector} for winner selection</li>
 *   <li>Uses {@link TaskProgress} for progress tracking</li>
 * </ul>
 *
 * @since 1.3.0
 */
public class ContractNetProtocol {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(ContractNetProtocol.class);

    /**
     * Listener for protocol events.
     */
    public interface ProtocolListener {
        /**
         * Called when a task is announced.
         */
        default void onTaskAnnounced(String announcementId, Task task) {}

        /**
         * Called when a bid is submitted.
         */
        default void onBidSubmitted(TaskBid bid) {}

        /**
         * Called when a contract is awarded.
         */
        default void onContractAwarded(String announcementId, UUID winnerId) {}

        /**
         * Called when progress updates.
         */
        default void onProgressUpdate(String taskId, double progress, String step) {}

        /**
         * Called when a task completes.
         */
        default void onTaskCompleted(String taskId, boolean success, String result) {}
    }

    private final ContractNetManager contractNetManager;
    private final BidCollector bidCollector;
    private final AwardSelector awardSelector;
    private final EventBus eventBus;
    private final Map<String, TaskProgress> activeProgress;
    private final List<ProtocolListener> listeners;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running;
    private final AtomicInteger totalAnnouncements;
    private final AtomicInteger totalAwards;
    private final AtomicInteger totalCompletions;

    /**
     * Creates a new Contract Net Protocol instance with default configuration.
     */
    public ContractNetProtocol() {
        this(null);
    }

    /**
     * Creates a new Contract Net Protocol instance with event bus integration.
     *
     * @param eventBus Event bus for broadcasting announcements
     */
    public ContractNetProtocol(EventBus eventBus) {
        this.contractNetManager = new ContractNetManager();
        this.bidCollector = new BidCollector();
        this.awardSelector = new AwardSelector();
        this.eventBus = eventBus;
        this.activeProgress = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ContractNetProtocol");
            t.setDaemon(true);
            return t;
        });
        this.running = new AtomicBoolean(true);
        this.totalAnnouncements = new AtomicInteger(0);
        this.totalAwards = new AtomicInteger(0);
        this.totalCompletions = new AtomicInteger(0);

        // Setup listeners
        setupListeners();

        // Start periodic tasks
        startPeriodicTasks();

        LOGGER.info("Contract Net Protocol initialized");
    }

    /**
     * Sets up internal listeners for bid collection and contract events.
     */
    private void setupListeners() {
        // Contract Net manager listeners
        contractNetManager.addListener(new ContractNetManager.ContractListener() {
            @Override
            public void onAnnouncement(TaskAnnouncement announcement) {
                // Notify protocol listeners
                listeners.forEach(l -> {
                    try {
                        l.onTaskAnnounced(announcement.announcementId(), announcement.task());
                    } catch (Exception e) {
                        LOGGER.warn("Listener error in onTaskAnnounced", e);
                    }
                });
            }

            @Override
            public void onBidSubmitted(TaskBid bid) {
                // Notify protocol listeners
                listeners.forEach(l -> {
                    try {
                        l.onBidSubmitted(bid);
                    } catch (Exception e) {
                        LOGGER.warn("Listener error in onBidSubmitted", e);
                    }
                });
            }

            @Override
            public void onContractAwarded(String announcementId, TaskBid winner) {
                // Create progress tracker
                String taskId = "task_" + announcementId;
                TaskProgress progress = new TaskProgress(taskId, announcementId, winner.bidderId());
                activeProgress.put(taskId, progress);

                totalAwards.incrementAndGet();

                // Notify protocol listeners
                listeners.forEach(l -> {
                    try {
                        l.onContractAwarded(announcementId, winner.bidderId());
                    } catch (Exception e) {
                        LOGGER.warn("Listener error in onContractAwarded", e);
                    }
                });
            }
        });

        // Bid collector listeners
        bidCollector.addListener(new BidCollector.BidCollectionListener() {
            @Override
            public void onCollectionClosed(String announcementId, List<TaskBid> bids) {
                LOGGER.debug("Bid collection closed for {}: {} bids", announcementId, bids.size());
            }

            @Override
            public void onCollectionTimeout(String announcementId) {
                LOGGER.warn("Bid collection timed out for {}", announcementId);
            }
        });
    }

    /**
     * Starts periodic maintenance tasks.
     */
    private void startPeriodicTasks() {
        // Cleanup old progress entries
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupOldProgress();
            } catch (Exception e) {
                LOGGER.warn("Error during progress cleanup", e);
            }
        }, 60, 60, TimeUnit.SECONDS);

        // Cleanup contract negotiations
        scheduler.scheduleAtFixedRate(() -> {
            try {
                contractNetManager.cleanup();
            } catch (Exception e) {
                LOGGER.warn("Error during contract cleanup", e);
            }
        }, 30, 30, TimeUnit.SECONDS);

        // Cleanup bid collections
        scheduler.scheduleAtFixedRate(() -> {
            try {
                bidCollector.cleanup(Duration.ofMinutes(5));
            } catch (Exception e) {
                LOGGER.warn("Error during bid collection cleanup", e);
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Announces a task for bidding.
     *
     * @param task The task to be allocated
     * @param requesterId UUID of the agent requesting the task
     * @return The announcement ID for tracking
     */
    public String announceTask(Task task, UUID requesterId) {
        return announceTask(task, requesterId, Duration.ofSeconds(30));
    }

    /**
     * Announces a task for bidding with custom deadline.
     *
     * @param task The task to be allocated
     * @param requesterId UUID of the agent requesting the task
     * @param deadline Duration before bidding closes
     * @return The announcement ID for tracking
     */
    public String announceTask(Task task, UUID requesterId, Duration deadline) {
        if (!running.get()) {
            throw new IllegalStateException("Protocol is not running");
        }

        // Announce via Contract Net Manager
        String announcementId = contractNetManager.announceTask(task, requesterId, deadline.toMillis());

        // Start bid collection
        bidCollector.startCollection(announcementId, deadline);

        // Publish to event bus if available
        if (eventBus != null) {
            eventBus.publish(new TaskAnnouncementEvent(announcementId, task, requesterId));
        }

        totalAnnouncements.incrementAndGet();

        LOGGER.info("Task announced: {} for action {} (deadline: {}s)",
            announcementId, task.getAction(), deadline.toSeconds());

        return announcementId;
    }

    /**
     * Submits a bid for an announced task.
     *
     * @param bid The bid to submit
     * @return true if the bid was accepted
     */
    public boolean submitBid(TaskBid bid) {
        if (!running.get()) {
            return false;
        }

        // Submit to Contract Net Manager
        boolean accepted = contractNetManager.submitBid(bid);

        if (accepted) {
            // Add to bid collector
            bidCollector.receiveBid(bid);

            LOGGER.debug("Bid accepted for {}: agent {}",
                bid.announcementId(), bid.bidderId().toString().substring(0, 8));
        }

        return accepted;
    }

    /**
     * Selects the winning agent for a task.
     *
     * @param announcementId The announcement ID
     * @return ID of the winning agent, or null if no bids
     */
    public UUID selectWinner(String announcementId) {
        if (!running.get()) {
            return null;
        }

        // Get all bids
        List<TaskBid> bids = contractNetManager.getBids(announcementId);

        if (bids.isEmpty()) {
            LOGGER.warn("No bids received for {}", announcementId);
            return null;
        }

        // Close bid collection
        bidCollector.closeCollection(announcementId);

        // Select winner
        TaskBid winner = awardSelector.selectBestBid(bids);

        if (winner != null) {
            // Award contract
            contractNetManager.awardContract(announcementId, winner);

            LOGGER.info("Winner selected for {}: agent {}",
                announcementId, winner.bidderId().toString().substring(0, 8));

            return winner.bidderId();
        }

        return null;
    }

    /**
     * Tracks progress of an active task.
     *
     * @param taskId The task ID
     * @return Progress tracker, or null if not found
     */
    public TaskProgress trackProgress(String taskId) {
        return activeProgress.get(taskId);
    }

    /**
     * Gets or creates progress tracking for a task.
     *
     * @param announcementId The announcement ID
     * @return Progress tracker
     */
    public TaskProgress getOrCreateProgress(String announcementId) {
        String taskId = "task_" + announcementId;
        return activeProgress.computeIfAbsent(taskId, id -> {
            // Get winner from contract negotiation
            ContractNetManager.ContractNegotiation negotiation = contractNetManager.getNegotiation(announcementId);
            UUID agentId = negotiation != null && negotiation.getWinningBid() != null
                ? negotiation.getWinningBid().bidderId()
                : UUID.randomUUID();

            return new TaskProgress(taskId, announcementId, agentId);
        });
    }

    /**
     * Handles a conflict resolution.
     *
     * @param tiedBids List of tied bids
     * @return The selected bid
     */
    public TaskBid handleConflict(List<TaskBid> tiedBids) {
        if (tiedBids == null || tiedBids.isEmpty()) {
            return null;
        }

        ConflictResolver.Resolution resolution = awardSelector.getConflictResolver().resolveConflictDetailed(tiedBids);

        LOGGER.info("Conflict resolved: {}", resolution.getReasoning());

        return resolution.getSelectedBid();
    }

    /**
     * Records a task completion.
     *
     * @param taskId The task ID
     * @param success Whether the task succeeded
     * @param result Completion result message
     * @return true if recorded successfully
     */
    public boolean recordCompletion(String taskId, boolean success, String result) {
        TaskProgress progress = activeProgress.get(taskId);

        if (progress == null) {
            LOGGER.warn("No progress tracker found for {}", taskId);
            return false;
        }

        if (success) {
            progress.markCompleted(result);
        } else {
            progress.markFailed(result);
        }

        totalCompletions.incrementAndGet();

        // Notify listeners
        listeners.forEach(l -> {
            try {
                l.onTaskCompleted(taskId, success, result);
            } catch (Exception e) {
                LOGGER.warn("Listener error in onTaskCompleted", e);
            }
        });

        LOGGER.info("Task {} {}: {}", taskId, success ? "completed" : "failed", result);

        return true;
    }

    /**
     * Updates progress for a task.
     *
     * @param taskId The task ID
     * @param progress Progress percentage (0-100)
     * @param step Current step description
     * @return true if updated successfully
     */
    public boolean updateProgress(String taskId, double progress, String step) {
        TaskProgress tracker = activeProgress.get(taskId);

        if (tracker == null) {
            return false;
        }

        tracker.setCompletionPercentage(progress);
        if (step != null) {
            tracker.setCurrentStep(step);
        }

        // Notify listeners
        listeners.forEach(l -> {
            try {
                l.onProgressUpdate(taskId, progress, step);
            } catch (Exception e) {
                LOGGER.warn("Listener error in onProgressUpdate", e);
            }
        });

        return true;
    }

    /**
     * Adds a listener for protocol events.
     *
     * @param listener The listener to add
     */
    public void addListener(ProtocolListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a protocol event listener.
     *
     * @param listener The listener to remove
     */
    public void removeListener(ProtocolListener listener) {
        listeners.remove(listener);
    }

    /**
     * Gets the Contract Net Manager.
     *
     * @return The manager instance
     */
    public ContractNetManager getContractNetManager() {
        return contractNetManager;
    }

    /**
     * Gets the Bid Collector.
     *
     * @return The collector instance
     */
    public BidCollector getBidCollector() {
        return bidCollector;
    }

    /**
     * Gets the Award Selector.
     *
     * @return The selector instance
     */
    public AwardSelector getAwardSelector() {
        return awardSelector;
    }

    /**
     * Gets all active progress trackers.
     *
     * @return Unmodifiable map of task ID to progress
     */
    public Map<String, TaskProgress> getActiveProgress() {
        return Collections.unmodifiableMap(activeProgress);
    }

    /**
     * Gets protocol statistics.
     *
     * @return Map of statistic name to value
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("running", running.get());
        stats.put("totalAnnouncements", totalAnnouncements.get());
        stats.put("totalAwards", totalAwards.get());
        stats.put("totalCompletions", totalCompletions.get());
        stats.put("activeProgress", activeProgress.size());
        stats.put("activeNegotiations", contractNetManager.getActiveCount());
        stats.put("bidCollections", bidCollector.getActiveCount());
        stats.putAll(awardSelector.getStatistics());
        return stats;
    }

    /**
     * Cleans up old progress entries.
     */
    private void cleanupOldProgress() {
        int cleaned = 0;
        Iterator<Map.Entry<String, TaskProgress>> it = activeProgress.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, TaskProgress> entry = it.next();
            TaskProgress progress = entry.getValue();

            // Remove terminated tasks older than 5 minutes
            if (progress.isTerminated()) {
                long elapsed = progress.getElapsedTime();
                if (elapsed > 300000) { // 5 minutes
                    it.remove();
                    cleaned++;
                }
            }
        }

        if (cleaned > 0) {
            LOGGER.debug("Cleaned up {} old progress entries", cleaned);
        }
    }

    /**
     * Shuts down the protocol.
     */
    public void shutdown() {
        running.set(false);

        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Shutdown bid collector
        bidCollector.shutdown();

        LOGGER.info("Contract Net Protocol shut down");
    }

    @Override
    public String toString() {
        Map<String, Object> stats = getStatistics();
        return String.format("ContractNetProtocol[running=%s, announcements=%d, awards=%d, completions=%d, active=%d]",
            stats.get("running"), stats.get("totalAnnouncements"),
            stats.get("totalAwards"), stats.get("totalCompletions"),
            stats.get("activeProgress"));
    }

    /**
     * Event for task announcements.
     */
    public static class TaskAnnouncementEvent {
        private final String announcementId;
        private final Task task;
        private final UUID requesterId;
        private final long timestamp;

        public TaskAnnouncementEvent(String announcementId, Task task, UUID requesterId) {
            this.announcementId = announcementId;
            this.task = task;
            this.requesterId = requesterId;
            this.timestamp = System.currentTimeMillis();
        }

        public String getAnnouncementId() {
            return announcementId;
        }

        public Task getTask() {
            return task;
        }

        public UUID getRequesterId() {
            return requesterId;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
