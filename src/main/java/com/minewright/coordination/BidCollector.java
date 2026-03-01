package com.minewright.coordination;

import com.minewright.testutil.TestLogger;

import org.slf4j.Logger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Collects and manages bids for task announcements with timeout handling.
 *
 * <p><b>Bid Collection Flow:</b></p>
 * <pre>
 * 1. Manager starts collection for a task announcement
 * 2. Agents submit bids until deadline or early termination
 * 3. Collector aggregates and validates bids
 * 4. Manager retrieves collected bids for evaluation
 * </pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>ConcurrentHashMap for thread-safe bid storage</li>
 *   <li>ReentrantLock for collection state changes</li>
 *   <li>AtomicBoolean for bid acceptance state</li>
 * </ul>
 *
 * @see TaskBid
 * @see AwardSelector
 * @since 1.3.0
 */
public class BidCollector {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(BidCollector.class);

    /**
     * State of a bid collection process.
     */
    public enum CollectionState {
        /** Collection is open and accepting bids */
        OPEN,
        /** Collection closed, bids being evaluated */
        CLOSED,
        /** Collection completed, winner selected */
        COMPLETED,
        /** Collection expired (timeout with no bids) */
        EXPIRED,
        /** Collection cancelled */
        CANCELLED
    }

    /**
     * Tracks the collection state for a single task announcement.
     */
    public static class BidCollection {
        private final String announcementId;
        private final Map<UUID, TaskBid> bids;
        private final CollectionState state;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final Duration timeout;
        private final UUID winnerId;
        private final String cancellationReason;

        public BidCollection(String announcementId, Duration timeout) {
            this(announcementId, new ConcurrentHashMap<>(), CollectionState.OPEN,
                 LocalDateTime.now(), null, timeout, null, null);
        }

        private BidCollection(
            String announcementId,
            Map<UUID, TaskBid> bids,
            CollectionState state,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Duration timeout,
            UUID winnerId,
            String cancellationReason
        ) {
            this.announcementId = announcementId;
            this.bids = bids;
            this.state = state;
            this.startTime = startTime;
            this.endTime = endTime;
            this.timeout = timeout;
            this.winnerId = winnerId;
            this.cancellationReason = cancellationReason;
        }

        public String getAnnouncementId() {
            return announcementId;
        }

        public Map<UUID, TaskBid> getBids() {
            return Collections.unmodifiableMap(bids);
        }

        public CollectionState getState() {
            return state;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public UUID getWinnerId() {
            return winnerId;
        }

        public String getCancellationReason() {
            return cancellationReason;
        }

        public int getBidCount() {
            return bids.size();
        }

        public boolean isOpen() {
            return state == CollectionState.OPEN;
        }

        public boolean isComplete() {
            return state == CollectionState.COMPLETED || state == CollectionState.EXPIRED || state == CollectionState.CANCELLED;
        }

        public Duration getElapsed() {
            LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
            return Duration.between(startTime, end);
        }

        public boolean isExpired() {
            if (endTime != null) {
                return true;
            }
            return getElapsed().compareTo(timeout) >= 0;
        }

        public BidCollection withBid(TaskBid bid) {
            Map<UUID, TaskBid> newBids = new ConcurrentHashMap<>(bids);
            newBids.put(bid.bidderId(), bid);
            return new BidCollection(announcementId, newBids, state, startTime, endTime, timeout, winnerId, cancellationReason);
        }

        public BidCollection withState(CollectionState newState) {
            return new BidCollection(announcementId, bids, newState, startTime,
                newState != CollectionState.OPEN ? LocalDateTime.now() : endTime,
                timeout, winnerId, cancellationReason);
        }

        public BidCollection withWinner(UUID winnerId) {
            return new BidCollection(announcementId, bids, CollectionState.COMPLETED, startTime,
                LocalDateTime.now(), timeout, winnerId, cancellationReason);
        }

        public BidCollection cancel(String reason) {
            return new BidCollection(announcementId, bids, CollectionState.CANCELLED, startTime,
                LocalDateTime.now(), timeout, winnerId, reason);
        }
    }

    /**
     * Listener for bid collection events.
     */
    public interface BidCollectionListener {
        /**
         * Called when a bid is received.
         */
        default void onBidReceived(String announcementId, TaskBid bid) {}

        /**
         * Called when collection closes.
         */
        default void onCollectionClosed(String announcementId, List<TaskBid> bids) {}

        /**
         * Called when collection times out.
         */
        default void onCollectionTimeout(String announcementId) {}

        /**
         * Called when collection is cancelled.
         */
        default void onCollectionCancelled(String announcementId, String reason) {}
    }

    private final Map<String, BidCollection> activeCollections;
    private final Map<String, ScheduledFuture<?>> timeoutTasks;
    private final List<BidCollectionListener> listeners;
    private final ReentrantLock collectionLock;
    private final ScheduledExecutorService scheduler;
    private final AtomicInteger totalCollections;
    private final AtomicInteger totalBids;
    private final AtomicInteger totalTimeouts;

    /**
     * Creates a new bid collector.
     */
    public BidCollector() {
        this.activeCollections = new ConcurrentHashMap<>();
        this.timeoutTasks = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.collectionLock = new ReentrantLock();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BidCollector-Timeout");
            t.setDaemon(true);
            return t;
        });
        this.totalCollections = new AtomicInteger(0);
        this.totalBids = new AtomicInteger(0);
        this.totalTimeouts = new AtomicInteger(0);
    }

    /**
     * Adds a listener for bid collection events.
     *
     * @param listener The listener to add
     */
    public void addListener(BidCollectionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a bid collection listener.
     *
     * @param listener The listener to remove
     */
    public void removeListener(BidCollectionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Starts bid collection for a task announcement.
     *
     * @param announcementId The announcement ID
     * @param timeout Duration before collection times out
     * @return true if collection started successfully
     */
    public boolean startCollection(String announcementId, Duration timeout) {
        if (announcementId == null || announcementId.isBlank()) {
            throw new IllegalArgumentException("Announcement ID cannot be null or blank");
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Timeout must be positive");
        }

        collectionLock.lock();
        try {
            if (activeCollections.containsKey(announcementId)) {
                LOGGER.warn("Collection already active for: {}", announcementId);
                return false;
            }

            BidCollection collection = new BidCollection(announcementId, timeout);
            activeCollections.put(announcementId, collection);
            totalCollections.incrementAndGet();

            LOGGER.info("Started bid collection for {} (timeout: {}s)",
                announcementId, timeout.toSeconds());

            // Schedule timeout task
            ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
                handleTimeout(announcementId);
            }, timeout.toMillis(), TimeUnit.MILLISECONDS);

            timeoutTasks.put(announcementId, timeoutTask);

            return true;
        } finally {
            collectionLock.unlock();
        }
    }

    /**
     * Receives and records a bid for an announcement.
     *
     * @param bid The bid to receive
     * @return true if the bid was accepted
     */
    public boolean receiveBid(TaskBid bid) {
        if (bid == null) {
            return false;
        }

        String announcementId = bid.announcementId();
        BidCollection collection = activeCollections.get(announcementId);

        if (collection == null) {
            LOGGER.warn("Cannot receive bid: no active collection for {}", announcementId);
            return false;
        }

        if (!collection.isOpen()) {
            LOGGER.warn("Cannot receive bid: collection closed for {}", announcementId);
            return false;
        }

        // Check for duplicate bids
        if (collection.getBids().containsKey(bid.bidderId())) {
            LOGGER.warn("Bid rejected: duplicate from agent {} for {}",
                bid.bidderId().toString().substring(0, 8), announcementId);
            return false;
        }

        collectionLock.lock();
        try {
            BidCollection current = activeCollections.get(announcementId);
            if (current != null && current.isOpen()) {
                BidCollection updated = current.withBid(bid);
                activeCollections.put(announcementId, updated);
                totalBids.incrementAndGet();

                LOGGER.info("Bid received for {}: score={:.2f}, time={}ms, bidder={}",
                    announcementId, bid.score(), bid.estimatedTime(),
                    bid.bidderId().toString().substring(0, 8));

                // Notify listeners
                listeners.forEach(listener -> {
                    try {
                        listener.onBidReceived(announcementId, bid);
                    } catch (Exception e) {
                        LOGGER.warn("Listener error in onBidReceived", e);
                    }
                });

                return true;
            }
        } finally {
            collectionLock.unlock();
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
        BidCollection collection = activeCollections.get(announcementId);
        if (collection == null) {
            return List.of();
        }
        return List.copyOf(collection.getBids().values());
    }

    /**
     * Gets the bid collection for an announcement.
     *
     * @param announcementId The announcement ID
     * @return The collection, or null if not found
     */
    public BidCollection getCollection(String announcementId) {
        return activeCollections.get(announcementId);
    }

    /**
     * Checks if collection is complete (closed, expired, or cancelled).
     *
     * @param announcementId The announcement ID
     * @return true if collection is complete
     */
    public boolean isCollectionComplete(String announcementId) {
        BidCollection collection = activeCollections.get(announcementId);
        return collection == null || collection.isComplete();
    }

    /**
     * Closes bid collection for an announcement.
     *
     * <p>No further bids will be accepted after closing.</p>
     *
     * @param announcementId The announcement ID
     * @return true if closed successfully
     */
    public boolean closeCollection(String announcementId) {
        collectionLock.lock();
        try {
            BidCollection collection = activeCollections.get(announcementId);
            if (collection == null || !collection.isOpen()) {
                return false;
            }

            BidCollection closed = collection.withState(CollectionState.CLOSED);
            activeCollections.put(announcementId, closed);

            // Cancel timeout task
            ScheduledFuture<?> timeoutTask = timeoutTasks.remove(announcementId);
            if (timeoutTask != null) {
                timeoutTask.cancel(false);
            }

            LOGGER.info("Closed bid collection for {} (received {} bids)",
                announcementId, collection.getBidCount());

            // Notify listeners
            List<TaskBid> bids = getBids(announcementId);
            listeners.forEach(listener -> {
                try {
                    listener.onCollectionClosed(announcementId, bids);
                } catch (Exception e) {
                    LOGGER.warn("Listener error in onCollectionClosed", e);
                }
            });

            return true;
        } finally {
            collectionLock.unlock();
        }
    }

    /**
     * Marks collection as completed with a winner.
     *
     * @param announcementId The announcement ID
     * @param winnerId The winning bidder ID
     * @return true if marked successfully
     */
    public boolean markCompleted(String announcementId, UUID winnerId) {
        collectionLock.lock();
        try {
            BidCollection collection = activeCollections.get(announcementId);
            if (collection == null) {
                return false;
            }

            BidCollection completed = collection.withWinner(winnerId);
            activeCollections.put(announcementId, completed);

            // Cancel timeout task
            ScheduledFuture<?> timeoutTask = timeoutTasks.remove(announcementId);
            if (timeoutTask != null) {
                timeoutTask.cancel(false);
            }

            LOGGER.info("Completed bid collection for {} (winner: {})",
                announcementId, winnerId.toString().substring(0, 8));

            return true;
        } finally {
            collectionLock.unlock();
        }
    }

    /**
     * Cancels bid collection for an announcement.
     *
     * @param announcementId The announcement ID
     * @param reason Cancellation reason
     * @return true if cancelled successfully
     */
    public boolean cancelCollection(String announcementId, String reason) {
        collectionLock.lock();
        try {
            BidCollection collection = activeCollections.get(announcementId);
            if (collection == null || collection.isComplete()) {
                return false;
            }

            BidCollection cancelled = collection.cancel(reason);
            activeCollections.put(announcementId, cancelled);

            // Cancel timeout task
            ScheduledFuture<?> timeoutTask = timeoutTasks.remove(announcementId);
            if (timeoutTask != null) {
                timeoutTask.cancel(false);
            }

            LOGGER.info("Cancelled bid collection for {}: {}",
                announcementId, reason);

            // Notify listeners
            listeners.forEach(listener -> {
                try {
                    listener.onCollectionCancelled(announcementId, reason);
                } catch (Exception e) {
                    LOGGER.warn("Listener error in onCollectionCancelled", e);
                }
            });

            return true;
        } finally {
            collectionLock.unlock();
        }
    }

    /**
     * Handles timeout for a bid collection.
     */
    private void handleTimeout(String announcementId) {
        collectionLock.lock();
        try {
            BidCollection collection = activeCollections.get(announcementId);
            if (collection == null || !collection.isOpen()) {
                return;
            }

            BidCollection expired = collection.withState(CollectionState.EXPIRED);
            activeCollections.put(announcementId, expired);
            timeoutTasks.remove(announcementId);

            totalTimeouts.incrementAndGet();

            LOGGER.info("Bid collection timed out for {} (received {} bids)",
                announcementId, collection.getBidCount());

            // Notify listeners
            listeners.forEach(listener -> {
                try {
                    listener.onCollectionTimeout(announcementId);
                } catch (Exception e) {
                    LOGGER.warn("Listener error in onCollectionTimeout", e);
                }
            });
        } finally {
            collectionLock.unlock();
        }
    }

    /**
     * Gets all active collections.
     *
     * @return Unmodifiable map of announcement ID to collection
     */
    public Map<String, BidCollection> getActiveCollections() {
        return Collections.unmodifiableMap(activeCollections);
    }

    /**
     * Gets the number of active collections.
     *
     * @return Count of active collections
     */
    public int getActiveCount() {
        return (int) activeCollections.values().stream()
            .filter(BidCollection::isOpen)
            .count();
    }

    /**
     * Gets statistics about bid collection.
     *
     * @return Map of statistic name to value
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalCollections", totalCollections.get());
        stats.put("totalBids", totalBids.get());
        stats.put("totalTimeouts", totalTimeouts.get());
        stats.put("activeCount", getActiveCount());
        return stats;
    }

    /**
     * Cleans up completed collections older than the specified duration.
     *
     * @param maxAge Maximum age to keep
     * @return Number of collections cleaned up
     */
    public int cleanup(Duration maxAge) {
        LocalDateTime cutoff = LocalDateTime.now().minus(maxAge);
        int cleaned = 0;

        Iterator<Map.Entry<String, BidCollection>> it = activeCollections.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, BidCollection> entry = it.next();
            BidCollection collection = entry.getValue();

            if (collection.isComplete() && collection.getEndTime().isBefore(cutoff)) {
                it.remove();

                // Cancel timeout task if still present
                ScheduledFuture<?> timeoutTask = timeoutTasks.remove(entry.getKey());
                if (timeoutTask != null) {
                    timeoutTask.cancel(false);
                }

                cleaned++;
            }
        }

        if (cleaned > 0) {
            LOGGER.debug("Cleaned up {} bid collections", cleaned);
        }

        return cleaned;
    }

    /**
     * Shuts down the bid collector.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Cancel all timeout tasks
        timeoutTasks.values().forEach(task -> task.cancel(false));
        timeoutTasks.clear();
    }

    @Override
    public String toString() {
        Map<String, Integer> stats = getStatistics();
        return String.format("BidCollector[active=%d, total=%d, bids=%d, timeouts=%d]",
            stats.get("activeCount"), stats.get("totalCollections"),
            stats.get("totalBids"), stats.get("totalTimeouts"));
    }
}
