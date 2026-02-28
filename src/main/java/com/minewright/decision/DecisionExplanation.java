package com.minewright.decision;

import com.minewright.action.Task;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Explains why a particular decision was made by the utility AI system.
 *
 * <p><b>Purpose:</b></p>
 * <p>DecisionExplanation provides detailed, human-readable explanations of
 * why a task was prioritized over others, which factors contributed most to
 * the decision, and what the scoring breakdown looked like. This is useful for:</p>
 *
 * <ul>
 *   <li><b>Debugging:</b> Understanding why the AI made certain choices</li>
 *   <li><b>Player Feedback:</b> Showing players what their agents are thinking</li>
 *   <li><b>Tuning:</b> Identifying factors that need weight adjustments</li>
 *   <li><b>Learning:</b> Teaching new players how utility AI works</li>
 * </ul>
 *
 * <p><b>Explanation Content:</b></p>
 * <ul>
 *   <li>The selected task and its final score</li>
 *   <li>Top contributing factors and their values</li>
 *   <li>Comparison to alternative tasks</li>
 *   <li>Reasoning in natural language</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is immutable and thread-safe. Once created, explanations
 * cannot be modified.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // After prioritizing tasks
 * List<Task> prioritized = prioritizer.prioritize(tasks, context);
 * Task selected = prioritized.get(0);
 *
 * // Generate explanation
 * DecisionExplanation explanation = DecisionExplanation.explain(
 *     selected, tasks, context, prioritizer
 * );
 *
 * // Show to player
 * player.sendSystemMessage(explanation.toDetailedString());
 * }</pre>
 *
 * @see UtilityScore
 * @see TaskPrioritizer
 * @see DecisionContext
 * @since 1.0.0
 */
public class DecisionExplanation {

    /**
     * The task that was selected.
     */
    private final Task selectedTask;

    /**
     * The utility score of the selected task.
     */
    private final UtilityScore selectedScore;

    /**
     * All tasks that were considered.
     */
    private final List<Task> consideredTasks;

    /**
     * Scores for all considered tasks.
     */
    private final Map<Task, UtilityScore> allScores;

    /**
     * The decision context used for scoring.
     */
    private final DecisionContext context;

    /**
     * Top contributing factors for the selected task.
     */
    private final List<FactorContribution> topFactors;

    /**
     * Natural language explanation of the decision.
     */
    private final String explanation;

    /**
     * Private constructor - use factory methods.
     */
    private DecisionExplanation(Builder builder) {
        this.selectedTask = builder.selectedTask;
        this.selectedScore = builder.selectedScore;
        this.consideredTasks = List.copyOf(builder.consideredTasks);
        this.allScores = Map.copyOf(builder.allScores);
        this.context = builder.context;
        this.topFactors = List.copyOf(builder.topFactors);
        this.explanation = builder.explanation;
    }

    /**
     * Creates an explanation for why a task was selected over alternatives.
     *
     * @param selected   The task that was selected
     * @param tasks      All tasks that were considered
     * @param context    The decision context
     * @param prioritizer The prioritizer used for scoring
     * @return A new DecisionExplanation
     */
    public static DecisionExplanation explain(Task selected, List<Task> tasks,
                                              DecisionContext context,
                                              TaskPrioritizer prioritizer) {
        if (selected == null) {
            throw new IllegalArgumentException("Selected task cannot be null");
        }
        if (tasks == null || tasks.isEmpty()) {
            throw new IllegalArgumentException("Tasks list cannot be null or empty");
        }
        if (context == null) {
            throw new IllegalArgumentException("Decision context cannot be null");
        }
        if (prioritizer == null) {
            throw new IllegalArgumentException("Prioritizer cannot be null");
        }

        // Score all tasks
        Map<Task, UtilityScore> scores = new HashMap<>();
        for (Task task : tasks) {
            UtilityScore score = prioritizer.score(task, context);
            scores.put(task, score);
        }

        UtilityScore selectedScore = scores.get(selected);

        // Extract top factors
        List<FactorContribution> topFactors = extractTopFactors(selectedScore, 5);

        // Generate explanation
        String explanation = generateExplanation(selected, selectedScore, tasks, scores, context);

        return new Builder()
            .selectedTask(selected)
            .selectedScore(selectedScore)
            .consideredTasks(tasks)
            .allScores(scores)
            .context(context)
            .topFactors(topFactors)
            .explanation(explanation)
            .build();
    }

    /**
     * Extracts the top contributing factors from a utility score.
     */
    private static List<FactorContribution> extractTopFactors(UtilityScore score, int max) {
        return score.factors().entrySet().stream()
            .map(entry -> new FactorContribution(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparingDouble(FactorContribution::value).reversed())
            .limit(max)
            .collect(Collectors.toList());
    }

    /**
     * Generates a natural language explanation of the decision.
     */
    private static String generateExplanation(Task selected, UtilityScore selectedScore,
                                             List<Task> tasks, Map<Task, UtilityScore> scores,
                                             DecisionContext context) {
        StringBuilder sb = new StringBuilder();

        // Identify highest and lowest scoring factors
        String highestFactor = null;
        double highestValue = 0.0;
        String lowestFactor = null;
        double lowestValue = 1.0;

        for (Map.Entry<String, Double> entry : selectedScore.factors().entrySet()) {
            if (entry.getValue() > highestValue) {
                highestValue = entry.getValue();
                highestFactor = entry.getKey();
            }
            if (entry.getValue() < lowestValue) {
                lowestValue = entry.getValue();
                lowestFactor = entry.getKey();
            }
        }

        // Build explanation
        sb.append(String.format("Selected action '%s' with score %.2f. ",
            selected.getAction(), selectedScore.finalScore()));

        if (highestFactor != null) {
            sb.append(String.format("Highest priority was %s (%.2f). ",
                formatFactorName(highestFactor), highestValue));
        }

        // Check if score was high priority
        if (selectedScore.isHighPriority()) {
            sb.append("This task was high priority. ");
        } else if (selectedScore.isLowPriority()) {
            sb.append("This task was low priority. ");
        }

        // Mention alternatives if they were close
        List<Map.Entry<Task, UtilityScore>> sorted = scores.entrySet().stream()
            .sorted(Comparator.comparingDouble((Map.Entry<Task, UtilityScore> e) -> e.getValue().finalScore()))
            .collect(Collectors.toList());

        if (sorted.size() > 1) {
            Map.Entry<Task, UtilityScore> runnerUp = sorted.get(sorted.size() - 2);
            double scoreGap = selectedScore.finalScore() - runnerUp.getValue().finalScore();

            if (scoreGap < 0.1) {
                sb.append(String.format("It narrowly beat '%s' (%.2f). ",
                    runnerUp.getKey().getAction(), runnerUp.getValue().finalScore()));
            }
        }

        // Contextual notes
        if (context.getHealthLevel() < 0.3) {
            sb.append("Health is critical. ");
        }
        if (!context.isDaytime()) {
            sb.append("It is nighttime. ");
        }
        if (!context.getNearbyThreats().isEmpty()) {
            sb.append("Threats are nearby. ");
        }

        return sb.toString().trim();
    }

    /**
     * Formats a factor name for human reading.
     */
    private static String formatFactorName(String factorName) {
        return factorName.replace("_", " ");
    }

    /**
     * Returns a brief, one-line explanation.
     *
     * @return Brief explanation string
     */
    public String toBriefString() {
        return String.format("Selected '%s' (score: %.2f) because: %s",
            selectedTask.getAction(),
            selectedScore.finalScore(),
            explanation);
    }

    /**
     * Returns a detailed, multi-line explanation.
     *
     * @return Detailed explanation with full breakdown
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();

        sb.append("=== Decision Explanation ===\n");
        sb.append(String.format("Selected Action: %s\n", selectedTask.getAction()));
        sb.append(String.format("Final Score: %.2f\n\n", selectedScore.finalScore()));

        sb.append("Top Contributing Factors:\n");
        for (FactorContribution factor : topFactors) {
            String bar = generateBar(factor.value());
            sb.append(String.format("  %-20s [%s] %.2f\n",
                formatFactorName(factor.name()), bar, factor.value()));
        }

        sb.append("\nAll Scores:\n");
        List<Map.Entry<Task, UtilityScore>> sorted = allScores.entrySet().stream()
            .sorted(Comparator.comparingDouble((Map.Entry<Task, UtilityScore> e) -> e.getValue().finalScore()).reversed())
            .toList();

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<Task, UtilityScore> entry = sorted.get(i);
            String indicator = entry.getKey().equals(selectedTask) ? ">>>" : "   ";
            sb.append(String.format("%s %2d. %-15s %.2f\n",
                indicator,
                i + 1,
                entry.getKey().getAction(),
                entry.getValue().finalScore()));
        }

        sb.append("\nReasoning:\n");
        sb.append("  ").append(explanation).append("\n");

        return sb.toString();
    }

    /**
     * Generates a visual bar for a score value.
     */
    private String generateBar(double value) {
        int length = (int) Math.round(value * 20);
        return "=".repeat(Math.max(0, length)) + " ".repeat(Math.max(0, 20 - length));
    }

    /**
     * Returns an explanation suitable for chat messages.
     *
     * @return Chat-friendly explanation
     */
    public String toChatString() {
        return String.format("[%s] Score: %.2f - %s",
            selectedTask.getAction(),
            selectedScore.finalScore(),
            explanation);
    }

    // Getters

    public Task getSelectedTask() {
        return selectedTask;
    }

    public UtilityScore getSelectedScore() {
        return selectedScore;
    }

    public List<Task> getConsideredTasks() {
        return consideredTasks;
    }

    public Map<Task, UtilityScore> getAllScores() {
        return allScores;
    }

    public DecisionContext getContext() {
        return context;
    }

    public List<FactorContribution> getTopFactors() {
        return topFactors;
    }

    public String getExplanation() {
        return explanation;
    }

    /**
     * Record for tracking factor contributions.
     */
    public record FactorContribution(String name, double value) {
    }

    /**
     * Builder pattern for constructing DecisionExplanation.
     */
    private static class Builder {
        private Task selectedTask;
        private UtilityScore selectedScore;
        private List<Task> consideredTasks;
        private Map<Task, UtilityScore> allScores;
        private DecisionContext context;
        private List<FactorContribution> topFactors;
        private String explanation;

        Builder selectedTask(Task task) {
            this.selectedTask = task;
            return this;
        }

        Builder selectedScore(UtilityScore score) {
            this.selectedScore = score;
            return this;
        }

        Builder consideredTasks(List<Task> tasks) {
            this.consideredTasks = tasks;
            return this;
        }

        Builder allScores(Map<Task, UtilityScore> scores) {
            this.allScores = scores;
            return this;
        }

        Builder context(DecisionContext context) {
            this.context = context;
            return this;
        }

        Builder topFactors(List<FactorContribution> factors) {
            this.topFactors = factors;
            return this;
        }

        Builder explanation(String explanation) {
            this.explanation = explanation;
            return this;
        }

        DecisionExplanation build() {
            return new DecisionExplanation(this);
        }
    }
}
