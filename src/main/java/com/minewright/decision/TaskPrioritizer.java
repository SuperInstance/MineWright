package com.minewright.decision;

import com.minewright.action.Task;
import com.minewright.testutil.TestLogger;

import org.slf4j.Logger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Prioritizes tasks using utility-based AI scoring with weighted factors.
 *
 * <p><b>Purpose:</b></p>
 * <p>TaskPrioritizer implements a utility AI system that scores tasks based on
 * multiple weighted factors. Tasks with higher utility scores are prioritized
 * for execution. This enables intelligent, context-aware decision making.</p>
 *
 * <p><b>How It Works:</b></p>
 * <ol>
 *   <li>Register utility factors with optional weights</li>
 *   <li>For each task, calculate factor values (0.0 to 1.0)</li>
 *   <li>Combine weighted factors to produce final score</li>
 *   <li>Sort tasks by final score (highest first)</li>
 * </ol>
 *
 * <p><b>Scoring Formula:</b></p>
 * <pre>
 * score = baseValue + sum(factor[i] * weight[i]) / totalWeight
 * </pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is thread-safe. Factors can be added/removed from any thread.
 * Task prioritization uses synchronized collections for concurrent access.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * TaskPrioritizer prioritizer = new TaskPrioritizer();
 * prioritizer.addFactor(UtilityFactors.URGENCY);
 * prioritizer.addFactor(UtilityFactors.SAFETY, 2.0); // High weight
 * prioritizer.addFactor(UtilityFactors.RESOURCE_PROXIMITY, 1.5);
 *
 * List<Task> tasks = ...;
 * DecisionContext context = DecisionContext.of(foreman, tasks);
 * List<Task> prioritized = prioritizer.prioritize(tasks, context);
 * }</pre>
 *
 * @see UtilityFactor
 * @see UtilityScore
 * @see DecisionContext
 * @see UtilityFactors
 * @since 1.0.0
 */
public class TaskPrioritizer {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(TaskPrioritizer.class);

    /**
     * Registered factors with their weights.
     * Thread-safe for concurrent modifications.
     */
    private final Map<UtilityFactor, Double> factors;

    /**
     * Default weight for factors without explicit weights.
     */
    private static final double DEFAULT_WEIGHT = 1.0;

    /**
     * Creates a new prioritizer with no registered factors.
     *
     * <p>Use {@link #addFactor(UtilityFactor)} methods to register factors.
     * Consider using {@link #withDefaults()} to add standard factors.</p>
     */
    public TaskPrioritizer() {
        this.factors = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new prioritizer and adds all default factors.
     *
     * <p>This is a convenience method for quick setup with common factors.</p>
     *
     * @return A new TaskPrioritizer with default factors registered
     */
    public static TaskPrioritizer withDefaults() {
        TaskPrioritizer prioritizer = new TaskPrioritizer();
        prioritizer.addDefaultFactors();
        return prioritizer;
    }

    /**
     * Adds a utility factor with its default weight.
     *
     * @param factor The factor to add
     * @return this prioritizer for chaining
     * @throws IllegalArgumentException if factor is null
     */
    public TaskPrioritizer addFactor(UtilityFactor factor) {
        if (factor == null) {
            throw new IllegalArgumentException("Factor cannot be null");
        }
        double weight = factor.getDefaultWeight();
        factors.put(factor, weight);
        try {
            LOGGER.debug("Added utility factor '{}' with weight {}", factor.getName(), weight);
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Logger not available in test environment - silently ignore
        }
        return this;
    }

    /**
     * Adds a utility factor with a custom weight.
     *
     * <p>Higher weights give the factor more influence over the final score.
     * Standard range is 0.0 to 2.0:</p>
     * <ul>
     *   <li>0.0 = Factor disabled</li>
     *   <li>0.5 = Low importance</li>
     *   <li>1.0 = Standard importance</li>
     *   <li>1.5 = High importance</li>
     *   <li>2.0 = Critical importance</li>
     * </ul>
     *
     * @param factor The factor to add
     * @param weight The weight for this factor (must be non-negative)
     * @return this prioritizer for chaining
     * @throws IllegalArgumentException if factor is null or weight is negative
     */
    public TaskPrioritizer addFactor(UtilityFactor factor, double weight) {
        if (factor == null) {
            throw new IllegalArgumentException("Factor cannot be null");
        }
        if (weight < 0) {
            throw new IllegalArgumentException("Weight must be non-negative, got " + weight);
        }
        factors.put(factor, weight);
        try {
            LOGGER.debug("Added utility factor '{}' with weight {}", factor.getName(), weight);
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Logger not available in test environment - silently ignore
        }
        return this;
    }

    /**
     * Removes a factor from this prioritizer.
     *
     * @param factor The factor to remove
     * @return true if the factor was present and removed
     */
    public boolean removeFactor(UtilityFactor factor) {
        boolean removed = factors.remove(factor) != null;
        if (removed) {
            try {
                LOGGER.debug("Removed utility factor '{}'", factor.getName());
            } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
                // Logger not available in test environment - silently ignore
            }
        }
        return removed;
    }

    /**
     * Removes all factors from this prioritizer.
     */
    public void clearFactors() {
        int count = factors.size();
        factors.clear();
        try {
            LOGGER.debug("Cleared {} utility factors", count);
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Logger not available in test environment - silently ignore
        }
    }

    /**
     * Adds the default set of utility factors.
     *
     * <p>This includes all standard factors from {@link UtilityFactors}
     * with appropriate weights for general-purpose use.</p>
     */
    public void addDefaultFactors() {
        // Critical factors
        addFactor(UtilityFactors.SAFETY, 2.0);
        addFactor(UtilityFactors.URGENCY, 1.8);

        // Important factors
        addFactor(UtilityFactors.RESOURCE_PROXIMITY, 1.5);
        addFactor(UtilityFactors.EFFICIENCY, 1.2);
        addFactor(UtilityFactors.SKILL_MATCH, 1.0);

        // Standard factors
        addFactor(UtilityFactors.PLAYER_PREFERENCE, 1.0);
        addFactor(UtilityFactors.TOOL_READINESS, 0.8);

        // Situational factors
        addFactor(UtilityFactors.HEALTH_STATUS, 0.8);
        addFactor(UtilityFactors.HUNGER_STATUS, 0.7);
        addFactor(UtilityFactors.TIME_OF_DAY, 0.5);
        addFactor(UtilityFactors.WEATHER_CONDITIONS, 0.3);

        try {
            LOGGER.info("Added {} default utility factors", factors.size());
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Logger not available in test environment - silently ignore
        }
    }

    /**
     * Prioritizes a list of tasks based on utility scores.
     *
     * <p>Tasks are scored and sorted in descending order (highest score first).
     * The original list is not modified.</p>
     *
     * @param tasks   The tasks to prioritize
     * @param context The decision context for scoring
     * @return A new list with tasks sorted by priority (highest first)
     * @throws IllegalArgumentException if tasks or context is null
     */
    public List<Task> prioritize(List<Task> tasks, DecisionContext context) {
        if (tasks == null) {
            throw new IllegalArgumentException("Tasks list cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Decision context cannot be null");
        }

        if (tasks.isEmpty()) {
            return List.of();
        }

        // Score all tasks
        List<ScoredTask> scoredTasks = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            UtilityScore score = score(task, context);
            scoredTasks.add(new ScoredTask(task, score.finalScore()));
        }

        // Sort by score (highest first)
        scoredTasks.sort(Comparator.comparingDouble(ScoredTask::score).reversed());

        // Extract sorted tasks
        List<Task> result = scoredTasks.stream()
            .map(ScoredTask::task)
            .collect(Collectors.toList());

        // Log the prioritization
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Prioritized {} tasks:", result.size());
                for (int i = 0; i < Math.min(3, result.size()); i++) {
                    ScoredTask st = scoredTasks.get(i);
                    LOGGER.debug("  {}. {} - score: {}",
                        i + 1, st.task().getAction(), String.format("%.2f", st.score()));
                }
            }
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Logger not available in test environment - silently ignore
        }

        return result;
    }

    /**
     * Calculates the utility score for a single task.
     *
     * <p>This method applies all registered factors that are applicable to
     * the task, combining their weighted contributions into a final score.</p>
     *
     * @param task    The task to score
     * @param context The decision context for scoring
     * @return A UtilityScore with the calculated values
     * @throws IllegalArgumentException if task or context is null
     */
    public UtilityScore score(Task task, DecisionContext context) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Decision context cannot be null");
        }

        // Start with neutral base value
        double baseValue = 0.5;

        // Calculate factor contributions
        Map<String, Double> factorValues = new TreeMap<>();
        double totalWeight = 0.0;
        double weightedSum = 0.0;

        for (Map.Entry<UtilityFactor, Double> entry : factors.entrySet()) {
            UtilityFactor factor = entry.getKey();
            double weight = entry.getValue();

            // Skip factors with zero weight
            if (weight <= 0.0) {
                continue;
            }

            // Skip factors that don't apply to this task
            if (!factor.appliesTo(task)) {
                continue;
            }

            try {
                // Calculate factor value
                double value = factor.calculate(task, context);

                // Clamp to valid range
                value = Math.max(0.0, Math.min(1.0, value));

                // Store the factor value
                factorValues.put(factor.getName(), value);

                // Accumulate weighted contribution
                weightedSum += value * weight;
                totalWeight += weight;

            } catch (Exception e) {
                try {
                    LOGGER.warn(
                        "Error calculating factor '{}' for task '{}': {}",
                        factor.getName(), task.getAction(), e.getMessage());
                } catch (ExceptionInInitializerError | NoClassDefFoundError e2) {
                    // Logger not available in test environment - silently ignore
                }
            }
        }

        // Calculate final score
        double finalScore;
        if (totalWeight > 0) {
            // Average the weighted factor values
            double factorAverage = weightedSum / totalWeight;
            // Blend with base value (base value has 20% influence)
            finalScore = (baseValue * 0.2) + (factorAverage * 0.8);
        } else {
            // No factors applied, use base value
            finalScore = baseValue;
        }

        // Clamp to valid range
        finalScore = Math.max(0.0, Math.min(1.0, finalScore));

        return new UtilityScore(baseValue, factorValues, finalScore);
    }

    /**
     * Returns the number of registered factors.
     *
     * @return The factor count
     */
    public int getFactorCount() {
        return factors.size();
    }

    /**
     * Returns an unmodifiable view of the registered factors and their weights.
     *
     * @return Map of factors to weights
     */
    public Map<UtilityFactor, Double> getFactors() {
        return Map.copyOf(factors);
    }

    /**
     * Returns the weight for a specific factor, or empty if not registered.
     *
     * @param factor The factor to look up
     * @return Optional containing the weight, or empty
     */
    public Optional<Double> getFactorWeight(UtilityFactor factor) {
        return Optional.ofNullable(factors.get(factor));
    }

    /**
     * Checks if a factor is registered with this prioritizer.
     *
     * @param factor The factor to check
     * @return true if the factor is registered
     */
    public boolean hasFactor(UtilityFactor factor) {
        return factors.containsKey(factor);
    }

    /**
     * Updates the weight for an already-registered factor.
     *
     * @param factor The factor to update
     * @param weight The new weight
     * @return true if the factor was found and updated
     * @throws IllegalArgumentException if factor is not registered or weight is negative
     */
    public boolean updateFactorWeight(UtilityFactor factor, double weight) {
        if (!factors.containsKey(factor)) {
            throw new IllegalArgumentException("Factor '" + factor.getName() + "' is not registered");
        }
        if (weight < 0) {
            throw new IllegalArgumentException("Weight must be non-negative, got " + weight);
        }
        factors.put(factor, weight);
        try {
            LOGGER.debug("Updated factor '{}' weight to {}", factor.getName(), weight);
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Logger not available in test environment - silently ignore
        }
        return true;
    }

    /**
     * Internal record for tracking scored tasks during sorting.
     */
    private record ScoredTask(Task task, double score) {
    }
}
