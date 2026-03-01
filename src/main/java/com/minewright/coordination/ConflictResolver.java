package com.minewright.coordination;

import com.minewright.testutil.TestLogger;

import org.slf4j.Logger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Resolves conflicts when multiple agents submit equal bids.
 *
 * <p><b>Conflict Resolution Strategies:</b></p>
 * <ul>
 *   <li><b>RANDOM</b> - Randomly select from tied agents</li>
 *   <li><b>ROUND_ROBIN</b> - Select agents in rotation order</li>
 *   <li><b>LOAD_BALANCING</b> - Select agent with lowest current load</li>
 *   <li><b>PERFORMANCE_BASED</b> - Select agent with best historical performance</li>
 *   <li><b>DISTANCE_BASED</b> - Select closest agent to task location</li>
 * </ul>
 *
 * @see AwardSelector
 * @see TaskBid
 * @since 1.3.0
 */
public class ConflictResolver {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(ConflictResolver.class);

    /**
     * Strategy for resolving tied bids.
     */
    public enum ResolutionStrategy {
        /** Randomly select from tied bids */
        RANDOM,
        /** Rotate through agents in order */
        ROUND_ROBIN,
        /** Select agent with lowest load */
        LOAD_BALANCING,
        /** Select agent with best performance history */
        PERFORMANCE_BASED,
        /** Select closest agent to task */
        DISTANCE_BASED
    }

    /**
     * Result of conflict resolution.
     */
    public static class Resolution {
        private final TaskBid selectedBid;
        private final ResolutionStrategy strategy;
        private final List<TaskBid> tiedBids;
        private final long resolutionTime;
        private final String reasoning;

        public Resolution(TaskBid selectedBid, ResolutionStrategy strategy,
                         List<TaskBid> tiedBids, String reasoning) {
            this.selectedBid = selectedBid;
            this.strategy = strategy;
            this.tiedBids = tiedBids;
            this.resolutionTime = System.currentTimeMillis();
            this.reasoning = reasoning;
        }

        public TaskBid getSelectedBid() {
            return selectedBid;
        }

        public ResolutionStrategy getStrategy() {
            return strategy;
        }

        public List<TaskBid> getTiedBids() {
            return Collections.unmodifiableList(tiedBids);
        }

        public long getResolutionTime() {
            return resolutionTime;
        }

        public String getReasoning() {
            return reasoning;
        }

        @Override
        public String toString() {
            return String.format("Resolution[strategy=%s, winner=%s, tied=%d, reason='%s']",
                strategy, selectedBid.bidderId().toString().substring(0, 8),
                tiedBids.size(), reasoning);
        }
    }

    /**
     * Performance statistics for an agent.
     */
    public static class AgentStats {
        private final UUID agentId;
        private final int completedTasks;
        private final int failedTasks;
        private final long totalCompletionTime;
        private final double successRate;

        public AgentStats(UUID agentId, int completed, int failed, long totalTime) {
            this.agentId = agentId;
            this.completedTasks = completed;
            this.failedTasks = failed;
            this.totalCompletionTime = totalTime;
            int total = completed + failed;
            this.successRate = total > 0 ? (double) completed / total : 0.5;
        }

        public UUID getAgentId() {
            return agentId;
        }

        public int getCompletedTasks() {
            return completedTasks;
        }

        public int getFailedTasks() {
            return failedTasks;
        }

        public long getTotalCompletionTime() {
            return totalCompletionTime;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public double getAverageCompletionTime() {
            return completedTasks > 0 ? (double) totalCompletionTime / completedTasks : 0.0;
        }
    }

    private final ResolutionStrategy strategy;
    private final Map<UUID, AgentStats> performanceHistory;
    private final AtomicInteger roundRobinIndex;
    private final Random random;
    private final AtomicInteger resolutionsMade;
    private final AtomicLong totalResolutionTime;
    private final Map<ResolutionStrategy, AtomicInteger> strategyUsage;

    /**
     * Creates a conflict resolver with the default strategy (LOAD_BALANCING).
     */
    public ConflictResolver() {
        this(ResolutionStrategy.LOAD_BALANCING);
    }

    /**
     * Creates a conflict resolver with a specific strategy.
     *
     * @param strategy The resolution strategy to use
     */
    public ConflictResolver(ResolutionStrategy strategy) {
        this.strategy = strategy;
        this.performanceHistory = new ConcurrentHashMap<>();
        this.roundRobinIndex = new AtomicInteger(0);
        this.random = new Random();
        this.resolutionsMade = new AtomicInteger(0);
        this.totalResolutionTime = new AtomicLong(0);
        this.strategyUsage = new ConcurrentHashMap<>();

        // Initialize strategy usage counters
        for (ResolutionStrategy s : ResolutionStrategy.values()) {
            strategyUsage.put(s, new AtomicInteger(0));
        }
    }

    /**
     * Resolves a conflict between tied bids.
     *
     * @param tiedBids List of bids with equal scores
     * @return The selected bid
     */
    public TaskBid resolveConflict(List<TaskBid> tiedBids) {
        if (tiedBids == null || tiedBids.isEmpty()) {
            return null;
        }

        if (tiedBids.size() == 1) {
            return tiedBids.get(0);
        }

        long startTime = System.nanoTime();

        TaskBid winner = switch (strategy) {
            case RANDOM -> resolveRandom(tiedBids);
            case ROUND_ROBIN -> resolveRoundRobin(tiedBids);
            case LOAD_BALANCING -> resolveLoadBalancing(tiedBids);
            case PERFORMANCE_BASED -> resolvePerformanceBased(tiedBids);
            case DISTANCE_BASED -> resolveDistanceBased(tiedBids);
        };

        long elapsed = System.nanoTime() - startTime;
        resolutionsMade.incrementAndGet();
        totalResolutionTime.addAndGet(elapsed);
        strategyUsage.get(strategy).incrementAndGet();

        LOGGER.debug("Resolved conflict using {} - selected {} from {} tied bids",
            strategy, winner.bidderId().toString().substring(0, 8), tiedBids.size());

        return winner;
    }

    /**
     * Resolves conflict with detailed resolution result.
     *
     * @param tiedBids List of bids with equal scores
     * @return Resolution result with details
     */
    public Resolution resolveConflictDetailed(List<TaskBid> tiedBids) {
        if (tiedBids == null || tiedBids.isEmpty()) {
            return null;
        }

        if (tiedBids.size() == 1) {
            return new Resolution(tiedBids.get(0), strategy, tiedBids,
                "Only one bid, no conflict");
        }

        long startTime = System.nanoTime();

        TaskBid winner = switch (strategy) {
            case RANDOM -> resolveRandom(tiedBids);
            case ROUND_ROBIN -> resolveRoundRobin(tiedBids);
            case LOAD_BALANCING -> resolveLoadBalancing(tiedBids);
            case PERFORMANCE_BASED -> resolvePerformanceBased(tiedBids);
            case DISTANCE_BASED -> resolveDistanceBased(tiedBids);
        };

        long elapsed = System.nanoTime() - startTime;
        resolutionsMade.incrementAndGet();
        totalResolutionTime.addAndGet(elapsed);
        strategyUsage.get(strategy).incrementAndGet();

        String reasoning = generateReasoning(winner, tiedBids);

        return new Resolution(winner, strategy, tiedBids, reasoning);
    }

    /**
     * Random resolution strategy.
     */
    private TaskBid resolveRandom(List<TaskBid> tiedBids) {
        int index = random.nextInt(tiedBids.size());
        return tiedBids.get(index);
    }

    /**
     * Round-robin resolution strategy.
     */
    private TaskBid resolveRoundRobin(List<TaskBid> tiedBids) {
        int index = roundRobinIndex.getAndIncrement() % tiedBids.size();
        return tiedBids.get(index);
    }

    /**
     * Load-balancing resolution strategy.
     * Selects agent with lowest current load.
     */
    private TaskBid resolveLoadBalancing(List<TaskBid> tiedBids) {
        return tiedBids.stream()
            .min(Comparator.comparingDouble(TaskBid::getCurrentLoad))
            .orElse(tiedBids.get(0));
    }

    /**
     * Performance-based resolution strategy.
     * Selects agent with best historical success rate.
     */
    private TaskBid resolvePerformanceBased(List<TaskBid> tiedBids) {
        // If we have performance history, use it
        if (!performanceHistory.isEmpty()) {
            return tiedBids.stream()
                .max(Comparator.comparingDouble(bid -> {
                    AgentStats stats = performanceHistory.get(bid.bidderId());
                    return stats != null ? stats.getSuccessRate() : 0.5;
                }))
                .orElse(tiedBids.get(0));
        }

        // Fall back to confidence score
        return tiedBids.stream()
            .max(Comparator.comparingDouble(TaskBid::confidence))
            .orElse(tiedBids.get(0));
    }

    /**
     * Distance-based resolution strategy.
     * Selects agent closest to task location.
     */
    private TaskBid resolveDistanceBased(List<TaskBid> tiedBids) {
        return tiedBids.stream()
            .min(Comparator.comparingDouble(TaskBid::getDistance))
            .orElse(tiedBids.get(0));
    }

    /**
     * Generates reasoning text for the resolution.
     */
    private String generateReasoning(TaskBid winner, List<TaskBid> tiedBids) {
        StringBuilder sb = new StringBuilder();
        sb.append("Selected ").append(winner.bidderId().toString().substring(0, 8));

        return switch (strategy) {
            case RANDOM -> {
                sb.append(" by random selection from ").append(tiedBids.size()).append(" tied agents");
                yield sb.toString();
            }
            case ROUND_ROBIN -> {
                sb.append(" by round-robin (index ").append(roundRobinIndex.get() % tiedBids.size()).append(")");
                yield sb.toString();
            }
            case LOAD_BALANCING -> {
                sb.append(String.format(" with lowest load (%.2f%%)", winner.getCurrentLoad() * 100));
                yield sb.toString();
            }
            case PERFORMANCE_BASED -> {
                AgentStats stats = performanceHistory.get(winner.bidderId());
                if (stats != null) {
                    sb.append(String.format(" with best success rate (%.1f%%, %d completed)",
                        stats.getSuccessRate() * 100, stats.getCompletedTasks()));
                } else {
                    sb.append(" with highest confidence");
                }
                yield sb.toString();
            }
            case DISTANCE_BASED -> {
                double distance = winner.getDistance();
                sb.append(String.format(" as closest agent (%.1f blocks)", distance));
                yield sb.toString();
            }
        };
    }

    /**
     * Updates performance statistics for an agent.
     *
     * @param agentId Agent ID
     * @param completed Number of completed tasks
     * @param failed Number of failed tasks
     * @param totalCompletionTime Total time for completed tasks
     */
    public void updateAgentStats(UUID agentId, int completed, int failed, long totalCompletionTime) {
        AgentStats stats = new AgentStats(agentId, completed, failed, totalCompletionTime);
        performanceHistory.put(agentId, stats);

        LOGGER.debug("Updated stats for {}: completed={}, failed={}, success=%.1f%%",
            agentId.toString().substring(0, 8), completed, failed, stats.getSuccessRate() * 100);
    }

    /**
     * Records a task completion for an agent.
     *
     * @param agentId Agent ID
     * @param success Whether the task succeeded
     * @param completionTime Time taken to complete
     */
    public void recordTaskCompletion(UUID agentId, boolean success, long completionTime) {
        AgentStats current = performanceHistory.get(agentId);
        int completed = current != null ? current.getCompletedTasks() : 0;
        int failed = current != null ? current.getFailedTasks() : 0;
        long totalTime = current != null ? current.getTotalCompletionTime() : 0;

        if (success) {
            completed++;
            totalTime += completionTime;
        } else {
            failed++;
        }

        updateAgentStats(agentId, completed, failed, totalTime);
    }

    /**
     * Gets performance statistics for an agent.
     *
     * @param agentId Agent ID
     * @return Agent stats, or null if not found
     */
    public AgentStats getAgentStats(UUID agentId) {
        return performanceHistory.get(agentId);
    }

    /**
     * Gets all agent statistics.
     *
     * @return Unmodifiable map of agent ID to stats
     */
    public Map<UUID, AgentStats> getAllAgentStats() {
        return Collections.unmodifiableMap(performanceHistory);
    }

    /**
     * Gets the current resolution strategy.
     *
     * @return Current strategy
     */
    public ResolutionStrategy getStrategy() {
        return strategy;
    }

    /**
     * Gets statistics about conflict resolution.
     *
     * @return Map of statistic name to value
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("resolutionsMade", resolutionsMade.get());
        stats.put("averageResolutionTimeUs",
            resolutionsMade.get() > 0
                ? totalResolutionTime.get() / resolutionsMade.get() / 1000.0
                : 0.0);
        stats.put("currentStrategy", strategy);

        Map<String, Integer> usage = new HashMap<>();
        strategyUsage.forEach((s, counter) -> usage.put(s.name(), counter.get()));
        stats.put("strategyUsage", usage);

        return stats;
    }

    /**
     * Resets statistics.
     */
    public void resetStatistics() {
        resolutionsMade.set(0);
        totalResolutionTime.set(0);
        strategyUsage.values().forEach(counter -> counter.set(0));
    }

    /**
     * Clears performance history.
     */
    public void clearPerformanceHistory() {
        performanceHistory.clear();
        LOGGER.info("Cleared performance history for all agents");
    }

    /**
     * Sets the round-robin index.
     *
     * @param index New index value
     */
    public void setRoundRobinIndex(int index) {
        roundRobinIndex.set(index);
    }

    @Override
    public String toString() {
        return String.format("ConflictResolver[strategy=%s, resolutions=%d]",
            strategy, resolutionsMade.get());
    }
}
