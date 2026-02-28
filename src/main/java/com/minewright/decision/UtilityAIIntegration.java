package com.minewright.decision;

import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Integration utilities for using Utility AI with MineWright's existing systems.
 *
 * <p><b>Purpose:</b></p>
 * <p>This class provides helper methods to integrate the Utility AI system
 * with existing components like TaskPlanner and ActionExecutor.</p>
 *
 * <p><b>Integration Points:</b></p>
 * <ul>
 *   <li><b>TaskPlanner:</b> Prioritize parsed tasks before queuing</li>
 *   <li><b>ActionExecutor:</b> Runtime decision making during execution</li>
 *   <li><b>Orchestration:</b> Task assignment based on agent capabilities</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class UtilityAIIntegration {
    private static final Logger LOGGER = TestLogger.getLogger(UtilityAIIntegration.class);

    private UtilityAIIntegration() {
        // Prevent instantiation
    }

    /**
     * Creates a default prioritizer configured for MineWright.
     *
     * @return A configured TaskPrioritizer
     */
    public static TaskPrioritizer createDefaultPrioritizer() {
        return TaskPrioritizer.withDefaults();
    }

    /**
     * Prioritizes tasks for a foreman entity.
     *
     * <p>This is the main integration point for TaskPlanner. After parsing
     * tasks from the LLM response, prioritize them before queuing.</p>
     *
     * @param foreman The foreman entity
     * @param tasks   The tasks to prioritize
     * @return Prioritized list of tasks
     */
    public static List<Task> prioritizeTasks(ForemanEntity foreman, Collection<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }

        try {
            DecisionContext context = DecisionContext.of(foreman, tasks);
            TaskPrioritizer prioritizer = createDefaultPrioritizer();
            return prioritizer.prioritize(List.copyOf(tasks), context);

        } catch (Exception e) {
            LOGGER.error("Error prioritizing tasks for foreman '{}': {}",
                foreman.getEntityName(), e.getMessage());
            // Fall back to original order
            return List.copyOf(tasks);
        }
    }

    /**
     * Asynchronously prioritizes tasks.
     *
     * <p>Useful for non-blocking integration with async LLM planning.</p>
     *
     * @param foreman The foreman entity
     * @param tasks   The tasks to prioritize
     * @return CompletableFuture with prioritized tasks
     */
    public static CompletableFuture<List<Task>> prioritizeTasksAsync(ForemanEntity foreman,
                                                                       Collection<Task> tasks) {
        return CompletableFuture.supplyAsync(() -> prioritizeTasks(foreman, tasks));
    }

    /**
     * Creates an action selector for runtime decision making.
     *
     * <p>ActionSelector can be used during execution to choose between
     * multiple ways to complete a goal.</p>
     *
     * @return A configured ActionSelector
     */
    public static ActionSelector createActionSelector() {
        TaskPrioritizer prioritizer = createDefaultPrioritizer();
        ActionSelector selector = new ActionSelector(prioritizer);

        // Use softmax for variety in actions
        selector.setStrategy(ActionSelector.Strategy.SOFTMAX);
        selector.setRandomness(0.2);

        return selector;
    }

    /**
     * Generates a decision explanation for debugging and player feedback.
     *
     * @param selected   The task that was selected
     * @param tasks      All tasks that were considered
     * @param foreman    The foreman entity
     * @return A decision explanation
     */
    public static DecisionExplanation explainDecision(Task selected, Collection<Task> tasks,
                                                       ForemanEntity foreman) {
        DecisionContext context = DecisionContext.of(foreman, tasks);
        TaskPrioritizer prioritizer = createDefaultPrioritizer();
        return DecisionExplanation.explain(selected, List.copyOf(tasks), context, prioritizer);
    }

    /**
     * Logs a decision explanation for debugging.
     *
     * @param explanation The explanation to log
     */
    public static void logDecision(DecisionExplanation explanation) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("\n{}", explanation.toDetailedString());
        }
    }
}
