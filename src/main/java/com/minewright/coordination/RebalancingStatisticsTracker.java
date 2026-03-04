package com.minewright.coordination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.minewright.coordination.TaskRebalancingManager.RebalancingReason;

/**
 * Tracks statistics for task rebalancing operations.
 * <p>
 * Responsible for:
 * <ul>
 *   <li>Recording assessment counts</li>
 *   <li>Tracking rebalancing triggers by reason</li>
 *   <li>Calculating success rates</li>
 *   <li>Providing statistics reports</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class RebalancingStatisticsTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RebalancingStatisticsTracker.class);

    // Statistics
    private final AtomicInteger totalAssessments = new AtomicInteger(0);
    private final AtomicInteger rebalancingTriggered = new AtomicInteger(0);
    private final AtomicInteger noCapableAgents = new AtomicInteger(0);
    private final Map<RebalancingReason, AtomicInteger> reasonCounts;

    public RebalancingStatisticsTracker() {
        this.reasonCounts = new ConcurrentHashMap<>();

        // Initialize reason counters
        for (RebalancingReason reason : RebalancingReason.values()) {
            reasonCounts.put(reason, new AtomicInteger(0));
        }
    }

    /**
     * Records a task assessment.
     */
    public void recordAssessment() {
        totalAssessments.incrementAndGet();
    }

    /**
     * Records that rebalancing was triggered.
     *
     * @param reason The reason for rebalancing
     */
    public void recordRebalancingTriggered(RebalancingReason reason) {
        rebalancingTriggered.incrementAndGet();
        if (reason != null) {
            reasonCounts.get(reason).incrementAndGet();
        }
    }

    /**
     * Records that no capable agents were found.
     */
    public void recordNoCapableAgents() {
        noCapableAgents.incrementAndGet();
    }

    /**
     * Gets the total number of assessments.
     */
    public int getTotalAssessments() {
        return totalAssessments.get();
    }

    /**
     * Gets the number of times rebalancing was triggered.
     */
    public int getRebalancingTriggered() {
        return rebalancingTriggered.get();
    }

    /**
     * Gets the number of times no capable agents were found.
     */
    public int getNoCapableAgents() {
        return noCapableAgents.get();
    }

    /**
     * Gets the reason counts map.
     */
    public Map<RebalancingReason, AtomicInteger> getReasonCounts() {
        return Collections.unmodifiableMap(reasonCounts);
    }

    /**
     * Creates a statistics snapshot.
     */
    public TaskRebalancingManager.RebalancingStatistics createStatistics(
            int reassignedSuccessfully, int reassignedFailed, double averageRebalancingTime) {
        return new TaskRebalancingManager.RebalancingStatistics(
            totalAssessments.get(),
            rebalancingTriggered.get(),
            reassignedSuccessfully,
            reassignedFailed,
            noCapableAgents.get(),
            new ConcurrentHashMap<>(reasonCounts),
            averageRebalancingTime
        );
    }
}
