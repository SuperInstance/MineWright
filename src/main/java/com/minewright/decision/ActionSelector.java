package com.minewright.decision;

import com.minewright.action.Task;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Selects the best action when multiple options are available for a goal.
 *
 * <p><b>Purpose:</b></p>
 * <p>ActionSelector extends utility-based decision making to action selection.
 * Given a goal, it can generate candidate actions, score each one, and select
 * the best option. It introduces controlled randomness to add variety and
 * prevent robotic behavior.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li><b>Candidate Generation:</b> Generate multiple ways to achieve a goal</li>
 *   <li><b>Utility Scoring:</b> Score each candidate using registered factors</li>
 *   <li><b>Randomness Injection:</b> Add controlled randomness for variety</li>
 *   <li><b>Softmax Selection:</b> Probabilistic selection based on scores</li>
 *   <li><b>Top-K Selection:</b> Choose from top N candidates for efficiency</li>
 * </ul>
 *
 * <p><b>Selection Strategies:</b></p>
 * <ul>
 *   <li><b>HIGHEST_SCORE:</b> Always pick the highest-scoring action</li>
 *   <li><b>SOFTMAX:</b> Probabilistic selection favoring high scores</li>
 *   <li><b>TOP_K_RANDOM:</b> Randomly pick from top K candidates</li>
 *   <li><b>WEIGHTED_RANDOM:</b> Random selection weighted by scores</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is thread-safe for read operations. Write operations
 * (adding candidates, changing strategies) use concurrent data structures.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * ActionSelector selector = new ActionSelector(prioritizer);
 * selector.setStrategy(Strategy.SOFTMAX);
 * selector.setRandomness(0.3); // 30% randomness
 *
 * // Generate and score candidates
 * List<Task> candidates = generateCandidatesForGoal("build house");
 * Task selected = selector.selectAction(candidates, context);
 * }</pre>
 *
 * @see TaskPrioritizer
 * @see UtilityScore
 * @see DecisionContext
 * @since 1.0.0
 */
public class ActionSelector {
    private static final Logger LOGGER = TestLogger.getLogger(ActionSelector.class);

    /**
     * Selection strategy for choosing among candidates.
     */
    public enum Strategy {
        /**
         * Always select the action with the highest utility score.
         * Deterministic and optimal, but may seem robotic.
         */
        HIGHEST_SCORE,

        /**
         * Probabilistic selection using softmax function.
         * Higher scores have higher probability but low scores still possible.
         * Good balance of optimality and variety.
         */
        SOFTMAX,

        /**
         * Select randomly from the top K candidates.
         * Adds variety while still choosing from good options.
         */
        TOP_K_RANDOM,

        /**
         * Random selection weighted by utility scores.
         * Similar to softmax but with linear probability scaling.
         */
        WEIGHTED_RANDOM
    }

    /**
     * The prioritizer to use for scoring candidates.
     */
    private final TaskPrioritizer prioritizer;

    /**
     * Current selection strategy.
     */
    private volatile Strategy strategy;

    /**
     * Amount of randomness to inject (0.0 to 1.0).
     * 0.0 = fully deterministic, 1.0 = fully random.
     */
    private volatile double randomness;

    /**
     * Number of top candidates to consider for TOP_K_RANDOM strategy.
     */
    private volatile int topK;

    /**
     * Temperature parameter for softmax (higher = more uniform distribution).
     */
    private volatile double softmaxTemperature;

    /**
     * Creates a new action selector with the given prioritizer.
     *
     * @param prioritizer The prioritizer for scoring candidates
     * @throws IllegalArgumentException if prioritizer is null
     */
    public ActionSelector(TaskPrioritizer prioritizer) {
        if (prioritizer == null) {
            throw new IllegalArgumentException("Prioritizer cannot be null");
        }
        this.prioritizer = prioritizer;
        this.strategy = Strategy.HIGHEST_SCORE;
        this.randomness = 0.0;
        this.topK = 3;
        this.softmaxTemperature = 1.0;
    }

    /**
     * Creates a new action selector with a default prioritizer.
     *
     * <p>The default prioritizer will use standard utility factors.</p>
     */
    public ActionSelector() {
        this(TaskPrioritizer.withDefaults());
    }

    /**
     * Selects the best action from a list of candidates.
     *
     * <p>Candidates are scored using the prioritizer, then selected according
     * to the current strategy. The list is not modified.</p>
     *
     * @param candidates The candidate actions to choose from
     * @param context    The decision context for scoring
     * @return The selected action, or null if candidates is empty
     * @throws IllegalArgumentException if candidates or context is null
     */
    public Task selectAction(List<Task> candidates, DecisionContext context) {
        if (candidates == null) {
            throw new IllegalArgumentException("Candidates list cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Decision context cannot be null");
        }

        if (candidates.isEmpty()) {
            return null;
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        // Score all candidates
        List<ScoredCandidate> scored = scoreCandidates(candidates, context);

        // Select based on strategy
        return switch (strategy) {
            case HIGHEST_SCORE -> selectHighestScore(scored);
            case SOFTMAX -> selectSoftmax(scored);
            case TOP_K_RANDOM -> selectTopKRandom(scored);
            case WEIGHTED_RANDOM -> selectWeightedRandom(scored);
        };
    }

    /**
     * Selects an action from candidates generated by a function.
     *
     * <p>This is useful for lazy candidate generation - candidates are only
     * created if needed. The generator function should produce a list of
     * candidate tasks.</p>
     *
     * @param generator Function to generate candidate tasks
     * @param context   The decision context for scoring
     * @return The selected action, or null if no candidates
     */
    public Task selectFromGenerator(Function<DecisionContext, List<Task>> generator,
                                    DecisionContext context) {
        List<Task> candidates = generator.apply(context);
        return selectAction(candidates, context);
    }

    /**
     * Scores a list of candidate tasks.
     */
    private List<ScoredCandidate> scoreCandidates(List<Task> candidates, DecisionContext context) {
        List<ScoredCandidate> scored = new ArrayList<>(candidates.size());

        for (Task candidate : candidates) {
            UtilityScore score = prioritizer.score(candidate, context);
            scored.add(new ScoredCandidate(candidate, score));
        }

        return scored;
    }

    /**
     * Selects the candidate with the highest score (with optional randomness).
     */
    private Task selectHighestScore(List<ScoredCandidate> scored) {
        // Sort by score descending
        scored.sort(Comparator.comparingDouble(s -> s.score().finalScore()));

        // Apply randomness if configured
        if (randomness > 0 && scored.size() > 1) {
            // Check if we should add randomness
            if (ThreadLocalRandom.current().nextDouble() < randomness) {
                // Pick from top 3 instead of always #1
                int topN = Math.min(3, scored.size());
                int index = ThreadLocalRandom.current().nextInt(topN);
                ScoredCandidate selected = scored.get(scored.size() - 1 - index);
                logSelection(selected, "highest_score_with_randomness");
                return selected.task();
            }
        }

        ScoredCandidate selected = scored.get(scored.size() - 1);
        logSelection(selected, "highest_score");
        return selected.task();
    }

    /**
     * Selects a candidate using softmax probabilistic selection.
     */
    private Task selectSoftmax(List<ScoredCandidate> scored) {
        // Calculate softmax probabilities
        double[] probabilities = calculateSoftmaxProbabilities(scored);

        // Weighted random selection based on probabilities
        double random = ThreadLocalRandom.current().nextDouble();
        double cumulative = 0.0;

        for (int i = 0; i < scored.size(); i++) {
            cumulative += probabilities[i];
            if (random <= cumulative) {
                ScoredCandidate selected = scored.get(i);
                logSelection(selected, "softmax");
                return selected.task();
            }
        }

        // Fallback to last element
        ScoredCandidate selected = scored.get(scored.size() - 1);
        logSelection(selected, "softmax_fallback");
        return selected.task();
    }

    /**
     * Calculates softmax probabilities for scored candidates.
     */
    private double[] calculateSoftmaxProbabilities(List<ScoredCandidate> scored) {
        double[] scores = scored.stream()
            .mapToDouble(s -> s.score().finalScore())
            .toArray();

        // Apply temperature (higher temperature = more uniform)
        double temperature = Math.max(0.1, softmaxTemperature);

        // Calculate exp(score / temperature) for each
        double[] expScores = new double[scores.length];
        double sumExp = 0.0;

        for (int i = 0; i < scores.length; i++) {
            expScores[i] = Math.exp(scores[i] / temperature);
            sumExp += expScores[i];
        }

        // Normalize to probabilities
        double[] probabilities = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            probabilities[i] = expScores[i] / sumExp;
        }

        return probabilities;
    }

    /**
     * Selects randomly from the top K candidates.
     */
    private Task selectTopKRandom(List<ScoredCandidate> scored) {
        // Sort by score descending
        scored.sort(Comparator.comparingDouble(s -> s.score().finalScore()));

        // Get top K
        int k = Math.min(topK, scored.size());
        int startIndex = scored.size() - k;
        int selectedIndex = startIndex + ThreadLocalRandom.current().nextInt(k);

        ScoredCandidate selected = scored.get(selectedIndex);
        logSelection(selected, "top_" + k + "_random");
        return selected.task();
    }

    /**
     * Selects a candidate using weighted random selection.
     */
    private Task selectWeightedRandom(List<ScoredCandidate> scored) {
        // Calculate total weight (sum of scores)
        double totalWeight = scored.stream()
            .mapToDouble(s -> s.score().finalScore())
            .sum();

        if (totalWeight <= 0) {
            // All scores are zero, pick uniformly
            int index = ThreadLocalRandom.current().nextInt(scored.size());
            ScoredCandidate selected = scored.get(index);
            logSelection(selected, "weighted_random_uniform");
            return selected.task();
        }

        // Weighted random selection
        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double cumulative = 0.0;

        for (ScoredCandidate candidate : scored) {
            cumulative += candidate.score().finalScore();
            if (random <= cumulative) {
                logSelection(candidate, "weighted_random");
                return candidate.task();
            }
        }

        // Fallback to last element
        ScoredCandidate selected = scored.get(scored.size() - 1);
        logSelection(selected, "weighted_random_fallback");
        return selected.task();
    }

    /**
     * Logs the selected action for debugging.
     */
    private void logSelection(ScoredCandidate selected, String method) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ActionSelector [{}] selected action '{}' with score {}",
                method,
                selected.task().getAction(),
                String.format("%.3f", selected.score().finalScore()));
        }
    }

    // Getters and Setters

    /**
     * Returns the current selection strategy.
     */
    public Strategy getStrategy() {
        return strategy;
    }

    /**
     * Sets the selection strategy.
     *
     * @param strategy The new strategy
     * @throws IllegalArgumentException if strategy is null
     */
    public void setStrategy(Strategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy cannot be null");
        }
        this.strategy = strategy;
        LOGGER.debug("ActionSelector strategy set to {}", strategy);
    }

    /**
     * Returns the amount of randomness (0.0 to 1.0).
     */
    public double getRandomness() {
        return randomness;
    }

    /**
     * Sets the amount of randomness to inject.
     *
     * <p>Randomness affects the HIGHEST_SCORE strategy, causing it to
     * occasionally pick from top candidates instead of always the best.</p>
     *
     * @param randomness Randomness level (0.0 to 1.0)
     * @throws IllegalArgumentException if randomness is out of range
     */
    public void setRandomness(double randomness) {
        if (randomness < 0.0 || randomness > 1.0) {
            throw new IllegalArgumentException(
                "Randomness must be between 0.0 and 1.0, got " + randomness);
        }
        this.randomness = randomness;
        LOGGER.debug("ActionSelector randomness set to {}", randomness);
    }

    /**
     * Returns the top K value for TOP_K_RANDOM strategy.
     */
    public int getTopK() {
        return topK;
    }

    /**
     * Sets the top K value for TOP_K_RANDOM strategy.
     *
     * @param topK Number of top candidates to consider (must be positive)
     * @throws IllegalArgumentException if topK is not positive
     */
    public void setTopK(int topK) {
        if (topK <= 0) {
            throw new IllegalArgumentException("Top K must be positive, got " + topK);
        }
        this.topK = topK;
        LOGGER.debug("ActionSelector top K set to {}", topK);
    }

    /**
     * Returns the softmax temperature.
     */
    public double getSoftmaxTemperature() {
        return softmaxTemperature;
    }

    /**
     * Sets the softmax temperature.
     *
     * <p>Higher temperature creates more uniform selection (more randomness).
     * Lower temperature emphasizes score differences (more deterministic).</p>
     *
     * @param temperature Temperature value (must be positive, typical range 0.1 to 5.0)
     * @throws IllegalArgumentException if temperature is not positive
     */
    public void setSoftmaxTemperature(double temperature) {
        if (temperature <= 0) {
            throw new IllegalArgumentException("Temperature must be positive, got " + temperature);
        }
        this.softmaxTemperature = temperature;
        LOGGER.debug("ActionSelector softmax temperature set to {}", temperature);
    }

    /**
     * Returns the prioritizer used for scoring.
     */
    public TaskPrioritizer getPrioritizer() {
        return prioritizer;
    }

    /**
     * Internal record for tracking scored candidates.
     */
    private record ScoredCandidate(Task task, UtilityScore score) {
    }
}
