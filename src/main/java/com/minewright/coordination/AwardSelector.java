package com.minewright.coordination;

import com.minewright.testutil.TestLogger;

import org.slf4j.Logger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Selects the best agent for a task from submitted bids.
 *
 * <p><b>Scoring Algorithm:</b></p>
 * <p>The winner is selected based on a weighted score considering:</p>
 * <ul>
 *   <li>Capability match (40% weight) - Skill proficiency vs requirements</li>
 *   <li>Bid price (20% weight) - Resource cost (lower is better)</li>
 *   <li>Estimated time (20% weight) - Completion time (lower is better)</li>
 *   <li>Agent availability (10% weight) - Current load factor</li>
 *   <li>Past performance (10% weight) - Historical success rate</li>
 * </ul>
 *
 * <p><b>Final Score Formula:</b></p>
 * <pre>
 * score = (capability * 0.40) +
 *         ((1.0 - price/maxPrice) * 0.20) +
 *         ((1.0 - time/maxTime) * 0.20) +
 *         ((1.0 - load) * 0.10) +
 *         (performance * 0.10)
 * </pre>
 *
 * @see TaskBid
 * @see ConflictResolver
 * @since 1.3.0
 */
public class AwardSelector {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(AwardSelector.class);

    /**
     * Configuration for bid scoring weights.
     */
    public static class ScoringWeights {
        private double capabilityWeight = 0.40;
        private double priceWeight = 0.20;
        private double timeWeight = 0.20;
        private double availabilityWeight = 0.10;
        private double performanceWeight = 0.10;

        public ScoringWeights() {}

        public ScoringWeights capabilityWeight(double weight) {
            this.capabilityWeight = weight;
            return this;
        }

        public ScoringWeights priceWeight(double weight) {
            this.priceWeight = weight;
            return this;
        }

        public ScoringWeights timeWeight(double weight) {
            this.timeWeight = weight;
            return this;
        }

        public ScoringWeights availabilityWeight(double weight) {
            this.availabilityWeight = weight;
            return this;
        }

        public ScoringWeights performanceWeight(double weight) {
            this.performanceWeight = weight;
            return this;
        }

        public double getCapabilityWeight() { return capabilityWeight; }
        public double getPriceWeight() { return priceWeight; }
        public double getTimeWeight() { return timeWeight; }
        public double getAvailabilityWeight() { return availabilityWeight; }
        public double getPerformanceWeight() { return performanceWeight; }

        public double getTotalWeight() {
            return capabilityWeight + priceWeight + timeWeight +
                   availabilityWeight + performanceWeight;
        }

        public void validate() {
            double total = getTotalWeight();
            if (Math.abs(total - 1.0) > 0.01) {
                throw new IllegalStateException(
                    "Scoring weights must sum to 1.0, got: " + total);
            }
        }
    }

    /**
     * Result of bid selection with detailed scoring.
     */
    public static class SelectionResult {
        private final TaskBid selectedBid;
        private final List<ScoredBid> scoredBids;
        private final String announcementId;
        private final int totalBids;
        private final long selectionTime;

        public SelectionResult(TaskBid selectedBid, List<ScoredBid> scoredBids, String announcementId) {
            this.selectedBid = selectedBid;
            this.scoredBids = scoredBids;
            this.announcementId = announcementId;
            this.totalBids = scoredBids.size();
            this.selectionTime = System.currentTimeMillis();
        }

        public TaskBid getSelectedBid() {
            return selectedBid;
        }

        public List<ScoredBid> getScoredBids() {
            return Collections.unmodifiableList(scoredBids);
        }

        public String getAnnouncementId() {
            return announcementId;
        }

        public int getTotalBids() {
            return totalBids;
        }

        public long getSelectionTime() {
            return selectionTime;
        }

        public boolean hasWinner() {
            return selectedBid != null;
        }

        public Optional<ScoredBid> getWinningScore() {
            if (selectedBid == null) {
                return Optional.empty();
            }
            return scoredBids.stream()
                .filter(sb -> sb.bid().equals(selectedBid))
                .findFirst();
        }
    }

    /**
     * A bid with its calculated score.
     */
    public static class ScoredBid {
        private final TaskBid bid;
        private final double totalScore;
        private final Map<String, Double> componentScores;

        public ScoredBid(TaskBid bid, double totalScore, Map<String, Double> componentScores) {
            this.bid = bid;
            this.totalScore = totalScore;
            this.componentScores = Map.copyOf(componentScores);
        }

        public TaskBid bid() {
            return bid;
        }

        public double totalScore() {
            return totalScore;
        }

        public Map<String, Double> componentScores() {
            return componentScores;
        }

        public double getCapabilityScore() {
            return componentScores.getOrDefault("capability", 0.0);
        }

        public double getPriceScore() {
            return componentScores.getOrDefault("price", 0.0);
        }

        public double getTimeScore() {
            return componentScores.getOrDefault("time", 0.0);
        }

        public double getAvailabilityScore() {
            return componentScores.getOrDefault("availability", 0.0);
        }

        public double getPerformanceScore() {
            return componentScores.getOrDefault("performance", 0.0);
        }

        @Override
        public String toString() {
            return String.format("ScoredBid[bid=%s, total=%.4f, components=%s]",
                bid.bidderId().toString().substring(0, 8), totalScore, componentScores);
        }
    }

    /**
     * Performance history for an agent.
     */
    public static class AgentPerformance {
        private final UUID agentId;
        private final int tasksCompleted;
        private final int tasksFailed;
        private final long averageCompletionTime;
        private final double successRate;

        public AgentPerformance(UUID agentId, int completed, int failed, long avgTime) {
            this.agentId = agentId;
            this.tasksCompleted = completed;
            this.tasksFailed = failed;
            this.averageCompletionTime = avgTime;
            int total = completed + failed;
            this.successRate = total > 0 ? (double) completed / total : 0.5;
        }

        public UUID getAgentId() {
            return agentId;
        }

        public int getTasksCompleted() {
            return tasksCompleted;
        }

        public int getTasksFailed() {
            return tasksFailed;
        }

        public long getAverageCompletionTime() {
            return averageCompletionTime;
        }

        public double getSuccessRate() {
            return successRate;
        }
    }

    private final ScoringWeights weights;
    private final ConflictResolver conflictResolver;
    private final AtomicInteger selectionsMade;
    private final AtomicInteger conflictsResolved;
    private final AtomicLong totalSelectionTime;

    /**
     * Creates an award selector with default weights.
     */
    public AwardSelector() {
        this(new ScoringWeights());
    }

    /**
     * Creates an award selector with custom weights.
     *
     * @param weights Scoring weights (must sum to 1.0)
     */
    public AwardSelector(ScoringWeights weights) {
        this(weights, new ConflictResolver());
    }

    /**
     * Creates an award selector with custom weights and conflict resolver.
     *
     * @param weights Scoring weights
     * @param conflictResolver Strategy for resolving ties
     */
    public AwardSelector(ScoringWeights weights, ConflictResolver conflictResolver) {
        weights.validate();
        this.weights = weights;
        this.conflictResolver = conflictResolver;
        this.selectionsMade = new AtomicInteger(0);
        this.conflictsResolved = new AtomicInteger(0);
        this.totalSelectionTime = new AtomicLong(0);
    }

    /**
     * Selects the best bid from a list of bids.
     *
     * @param bids List of bids to evaluate
     * @return The selected bid, or null if no bids
     */
    public TaskBid selectBestBid(List<TaskBid> bids) {
        if (bids == null || bids.isEmpty()) {
            return null;
        }

        if (bids.size() == 1) {
            return bids.get(0);
        }

        long startTime = System.nanoTime();

        List<ScoredBid> scoredBids = new ArrayList<>();
        for (TaskBid bid : bids) {
            double score = calculateBidScore(bid, bids);
            Map<String, Double> components = calculateComponentScores(bid, bids);
            scoredBids.add(new ScoredBid(bid, score, components));
        }

        // Sort by score descending
        scoredBids.sort((a, b) -> Double.compare(b.totalScore(), a.totalScore()));

        // Check for ties
        ScoredBid best = scoredBids.get(0);
        ScoredBid second = scoredBids.get(1);

        TaskBid winner;
        if (Math.abs(best.totalScore() - second.totalScore()) < 0.001) {
            // Tie detected - use conflict resolver
            List<TaskBid> tiedBids = scoredBids.stream()
                .filter(sb -> Math.abs(sb.totalScore() - best.totalScore()) < 0.001)
                .map(ScoredBid::bid)
                .collect(Collectors.toList());

            winner = conflictResolver.resolveConflict(tiedBids);
            conflictsResolved.incrementAndGet();

            LOGGER.debug("Conflict resolved for {} tied bids - selected: {}",
                tiedBids.size(), winner.bidderId().toString().substring(0, 8));
        } else {
            winner = best.bid();
        }

        long elapsed = System.nanoTime() - startTime;
        selectionsMade.incrementAndGet();
        totalSelectionTime.addAndGet(elapsed);

        LOGGER.info("Selected best bid: {} (score={:.4f}, evaluated {} bids in {}μs)",
            winner.bidderId().toString().substring(0, 8),
            best.totalScore(), bids.size(), elapsed / 1000);

        return winner;
    }

    /**
     * Selects the best bid with detailed scoring results.
     *
     * @param bids List of bids to evaluate
     * @param announcementId The announcement ID
     * @return Selection result with detailed scoring
     */
    public SelectionResult selectBestBidDetailed(List<TaskBid> bids, String announcementId) {
        if (bids == null || bids.isEmpty()) {
            return new SelectionResult(null, List.of(), announcementId);
        }

        List<ScoredBid> scoredBids = new ArrayList<>();
        for (TaskBid bid : bids) {
            double score = calculateBidScore(bid, bids);
            Map<String, Double> components = calculateComponentScores(bid, bids);
            scoredBids.add(new ScoredBid(bid, score, components));
        }

        // Sort by score descending
        scoredBids.sort((a, b) -> Double.compare(b.totalScore(), a.totalScore()));

        TaskBid winner;
        if (scoredBids.size() > 1) {
            ScoredBid best = scoredBids.get(0);
            ScoredBid second = scoredBids.get(1);

            if (Math.abs(best.totalScore() - second.totalScore()) < 0.001) {
                List<TaskBid> tiedBids = scoredBids.stream()
                    .filter(sb -> Math.abs(sb.totalScore() - best.totalScore()) < 0.001)
                    .map(ScoredBid::bid)
                    .collect(Collectors.toList());

                winner = conflictResolver.resolveConflict(tiedBids);
                conflictsResolved.incrementAndGet();
            } else {
                winner = best.bid();
            }
        } else {
            winner = scoredBids.get(0).bid();
        }

        selectionsMade.incrementAndGet();

        return new SelectionResult(winner, scoredBids, announcementId);
    }

    /**
     * Calculates the overall score for a bid.
     *
     * @param bid The bid to score
     * @param allBids All bids for normalization
     * @return Score from 0.0 to 1.0
     */
    private double calculateBidScore(TaskBid bid, List<TaskBid> allBids) {
        Map<String, Double> components = calculateComponentScores(bid, allBids);

        return (components.get("capability") * weights.getCapabilityWeight()) +
               (components.get("price") * weights.getPriceWeight()) +
               (components.get("time") * weights.getTimeWeight()) +
               (components.get("availability") * weights.getAvailabilityWeight()) +
               (components.get("performance") * weights.getPerformanceWeight());
    }

    /**
     * Calculates component scores for a bid.
     *
     * @param bid The bid to score
     * @param allBids All bids for normalization
     * @return Map of component name to score
     */
    private Map<String, Double> calculateComponentScores(TaskBid bid, List<TaskBid> allBids) {
        Map<String, Double> scores = new HashMap<>();

        // Capability score (already 0-1)
        scores.put("capability", bid.score());

        // Price score (lower is better, normalize to 0-1)
        double maxPrice = allBids.stream()
            .mapToDouble(b -> b.capabilities().getOrDefault("price", 1.0).toString().equals("true") ? 1.0 : 0.0)
            .max()
            .orElse(1.0);
        double price = bid.capabilities().getOrDefault("price", 0.5).toString().equals("true") ? 1.0 : 0.5;
        scores.put("price", maxPrice > 0 ? 1.0 - (price / maxPrice) : 1.0);

        // Time score (lower is better, normalize to 0-1)
        long maxTime = allBids.stream()
            .mapToLong(TaskBid::estimatedTime)
            .max()
            .orElse(bid.estimatedTime());
        scores.put("time", maxTime > 0 ? 1.0 - ((double) bid.estimatedTime() / maxTime) : 1.0);

        // Availability score (lower load is better)
        double load = bid.getCurrentLoad();
        scores.put("availability", 1.0 - load);

        // Performance score (use confidence as proxy)
        scores.put("performance", bid.confidence());

        return scores;
    }

    /**
     * Selects the best bid considering agent performance history.
     *
     * @param bids List of bids to evaluate
     * @param performanceMap Agent performance history
     * @return The selected bid, or null if no bids
     */
    public TaskBid selectBestBidWithPerformance(
        List<TaskBid> bids,
        Map<UUID, AgentPerformance> performanceMap
    ) {
        if (bids == null || bids.isEmpty()) {
            return null;
        }

        if (bids.size() == 1) {
            return bids.get(0);
        }

        // Create temporary bids with adjusted confidence based on performance
        List<TaskBid> adjustedBids = bids.stream()
            .map(bid -> {
                AgentPerformance perf = performanceMap.get(bid.bidderId());
                if (perf == null) {
                    return bid;
                }

                // Adjust confidence based on success rate
                double adjustedConfidence = (bid.confidence() + perf.getSuccessRate()) / 2.0;

                return TaskBid.builder()
                    .announcementId(bid.announcementId())
                    .bidderId(bid.bidderId())
                    .score(bid.score())
                    .estimatedTime(bid.estimatedTime())
                    .confidence(adjustedConfidence)
                    .capabilities(bid.capabilities())
                    .build();
            })
            .collect(Collectors.toList());

        return selectBestBid(adjustedBids);
    }

    /**
     * Gets the scoring weights.
     *
     * @return Current scoring weights
     */
    public ScoringWeights getWeights() {
        return weights;
    }

    /**
     * Gets the conflict resolver.
     *
     * @return Current conflict resolver
     */
    public ConflictResolver getConflictResolver() {
        return conflictResolver;
    }

    /**
     * Gets selection statistics.
     *
     * @return Map of statistic name to value
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("selectionsMade", selectionsMade.get());
        stats.put("conflictsResolved", conflictsResolved.get());
        stats.put("averageSelectionTimeUs",
            selectionsMade.get() > 0
                ? totalSelectionTime.get() / selectionsMade.get() / 1000.0
                : 0.0);
        stats.put("conflictRate",
            selectionsMade.get() > 0
                ? (double) conflictsResolved.get() / selectionsMade.get()
                : 0.0);
        return stats;
    }

    /**
     * Resets statistics.
     */
    public void resetStatistics() {
        selectionsMade.set(0);
        conflictsResolved.set(0);
        totalSelectionTime.set(0);
    }

    @Override
    public String toString() {
        Map<String, Object> stats = getStatistics();
        return String.format("AwardSelector[selections=%d, conflicts=%d, conflictRate=%.2f]",
            stats.get("selectionsMade"), stats.get("conflictsResolved"), stats.get("conflictRate"));
    }
}
