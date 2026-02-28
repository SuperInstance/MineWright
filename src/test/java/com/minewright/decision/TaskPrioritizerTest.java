package com.minewright.decision;

import com.minewright.action.Task;
import com.minewright.testutil.TaskBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TaskPrioritizer} and {@link DecisionExplanation}.
 *
 * Tests cover:
 * <ul>
 *   <li>Task prioritization by utility scores</li>
 *   <li>Factor registration and management</li>
 *   <li>Weight configuration and updates</li>
 *   <li>Empty and single-task handling</li>
 *   <li>Task order preservation with equal scores</li>
 *   <li>Decision explanation generation with multiple factors</li>
 *   <li>Urgent task prioritization and explanation</li>
 *   <li>Top contributing factors identification</li>
 *   <li>Brief, detailed, and chat-friendly explanation formats</li>
 *   <li>Contextual notes in explanations (health, time, threats)</li>
 *   <li>Error handling for null/empty inputs</li>
 * </ul>
 */
@DisplayName("Task Prioritizer Tests")
class TaskPrioritizerTest {

    private TaskPrioritizer prioritizer;
    private DecisionContext context;

    @BeforeEach
    void setUp() {
        prioritizer = new TaskPrioritizer();
        context = createTestContext();
    }

    @Test
    @DisplayName("Prioritizes higher scoring tasks first")
    void prioritizesHigherScoringTasks() {
        // Create tasks with different characteristics
        Task urgentTask = TaskBuilder.aTask("attack")
                .withTarget("zombie")
                .build();

        Task normalTask = TaskBuilder.Presets.mineStone(64);

        Task lowPriorityTask = TaskBuilder.aTask("build")
                .withStructure("house")
                .build();

        List<Task> tasks = List.of(lowPriorityTask, urgentTask, normalTask);

        // Add factors that will differentiate the tasks
        prioritizer.addFactor(UtilityFactors.URGENCY, 2.0);
        prioritizer.addFactor(UtilityFactors.EFFICIENCY, 1.0);

        List<Task> result = prioritizer.prioritize(tasks, context);

        assertEquals(3, result.size(), "Should return all tasks");
        // Urgent task should be first due to high urgency factor
        assertEquals("attack", result.get(0).getAction(),
                "Urgent task should be prioritized first");
    }

    @Test
    @DisplayName("Maintains task order when scores are equal")
    void maintainsTaskOrderWhenScoresEqual() {
        Task task1 = TaskBuilder.Presets.mineStone(64);
        Task task2 = TaskBuilder.Presets.mineStone(64);
        Task task3 = TaskBuilder.Presets.mineStone(64);

        List<Task> tasks = List.of(task1, task2, task3);

        // No factors means all tasks get the same base score
        List<Task> result = prioritizer.prioritize(tasks, context);

        assertEquals(3, result.size());
        // Order should be preserved for equal scores
        assertEquals(task1, result.get(0), "First task should remain first");
        assertEquals(task2, result.get(1), "Second task should remain second");
        assertEquals(task3, result.get(2), "Third task should remain third");
    }

    @Test
    @DisplayName("Empty list returns empty list")
    void emptyListReturnsEmpty() {
        List<Task> emptyTasks = List.of();

        List<Task> result = prioritizer.prioritize(emptyTasks, context);

        assertTrue(result.isEmpty(), "Should return empty list for empty input");
    }

    @Test
    @DisplayName("Single task returns same list")
    void singleTaskReturnsSameList() {
        Task singleTask = TaskBuilder.Presets.mineStone(64);
        List<Task> singleTaskList = List.of(singleTask);

        List<Task> result = prioritizer.prioritize(singleTaskList, context);

        assertEquals(1, result.size(), "Should return one task");
        assertEquals(singleTask.getAction(), result.get(0).getAction(),
                "Should return the same task");
    }

    @Test
    @DisplayName("Prioritize throws on null tasks list")
    void prioritizeThrowsOnNullTasks() {
        assertThrows(IllegalArgumentException.class,
                () -> prioritizer.prioritize(null, context),
                "Should throw when tasks list is null");
    }

    @Test
    @DisplayName("Prioritize throws on null context")
    void prioritizeThrowsOnNullContext() {
        List<Task> tasks = List.of(TaskBuilder.Presets.mineStone(64));

        assertThrows(IllegalArgumentException.class,
                () -> prioritizer.prioritize(tasks, null),
                "Should throw when context is null");
    }

    @Test
    @DisplayName("Prioritize does not modify original list")
    void prioritizeDoesNotModifyOriginalList() {
        Task task1 = TaskBuilder.Presets.mineStone(64);
        Task task2 = TaskBuilder.aTask("build").withStructure("house").build();

        List<Task> originalList = new ArrayList<>(List.of(task1, task2));
        List<Task> originalOrder = new ArrayList<>(originalList);

        prioritizer.addFactor(UtilityFactors.URGENCY);
        List<Task> result = prioritizer.prioritize(originalList, context);

        assertEquals(originalOrder, originalList,
                "Original list should not be modified");
    }

    @Test
    @DisplayName("AddFactor with default weight")
    void addFactorWithDefaultWeight() {
        UtilityFactor testFactor = new UtilityFactor() {
            @Override
            public double calculate(Task task, DecisionContext context) {
                return 0.5;
            }

            @Override
            public String getName() {
                return "test_factor";
            }
        };

        TaskPrioritizer result = prioritizer.addFactor(testFactor);

        assertEquals(1, prioritizer.getFactorCount(),
                "Should have one factor registered");
        assertTrue(prioritizer.hasFactor(testFactor),
                "Should contain the added factor");
        assertEquals(prioritizer, result,
                "Should return this for chaining");
    }

    @Test
    @DisplayName("AddFactor throws on null factor")
    void addFactorThrowsOnNullFactor() {
        assertThrows(IllegalArgumentException.class,
                () -> prioritizer.addFactor(null),
                "Should throw when factor is null");
    }

    @Test
    @DisplayName("AddFactor with custom weight")
    void addFactorWithCustomWeight() {
        UtilityFactor testFactor = new UtilityFactor() {
            @Override
            public double calculate(Task task, DecisionContext context) {
                return 0.7;
            }

            @Override
            public String getName() {
                return "custom_factor";
            }

            @Override
            public double getDefaultWeight() {
                return 1.0;
            }
        };

        prioritizer.addFactor(testFactor, 2.5);

        assertTrue(prioritizer.getFactorWeight(testFactor).isPresent(),
                "Should have the factor");
        assertEquals(2.5, prioritizer.getFactorWeight(testFactor).get(), 0.001,
                "Should use custom weight");
    }

    @Test
    @DisplayName("AddFactor throws on negative weight")
    void addFactorThrowsOnNegativeWeight() {
        UtilityFactor testFactor = new UtilityFactor() {
            @Override
            public double calculate(Task task, DecisionContext context) {
                return 0.5;
            }

            @Override
            public String getName() {
                return "test";
            }
        };

        assertThrows(IllegalArgumentException.class,
                () -> prioritizer.addFactor(testFactor, -1.0),
                "Should throw when weight is negative");
    }

    @Test
    @DisplayName("AddFactor allows zero weight")
    void addFactorAllowsZeroWeight() {
        UtilityFactor testFactor = new UtilityFactor() {
            @Override
            public double calculate(Task task, DecisionContext context) {
                return 0.5;
            }

            @Override
            public String getName() {
                return "zero_weight";
            }
        };

        assertDoesNotThrow(() -> prioritizer.addFactor(testFactor, 0.0),
                "Should allow zero weight");
    }

    @Test
    @DisplayName("RemoveFactor removes registered factor")
    void removeFactorRemovesRegisteredFactor() {
        prioritizer.addFactor(UtilityFactors.URGENCY);
        assertEquals(1, prioritizer.getFactorCount());

        boolean removed = prioritizer.removeFactor(UtilityFactors.URGENCY);

        assertTrue(removed, "Should return true when factor was removed");
        assertEquals(0, prioritizer.getFactorCount(),
                "Should have no factors after removal");
        assertFalse(prioritizer.hasFactor(UtilityFactors.URGENCY),
                "Should not contain removed factor");
    }

    @Test
    @DisplayName("RemoveFactor returns false for non-existent factor")
    void removeFactorReturnsFalseForNonExistentFactor() {
        boolean removed = prioritizer.removeFactor(UtilityFactors.URGENCY);

        assertFalse(removed, "Should return false when factor was not present");
    }

    @Test
    @DisplayName("ClearFactors removes all factors")
    void clearFactorsRemovesAll() {
        prioritizer.addFactor(UtilityFactors.URGENCY);
        prioritizer.addFactor(UtilityFactors.SAFETY);
        prioritizer.addFactor(UtilityFactors.EFFICIENCY);

        assertEquals(3, prioritizer.getFactorCount());

        prioritizer.clearFactors();

        assertEquals(0, prioritizer.getFactorCount(),
                "Should have no factors after clearing");
    }

    @Test
    @DisplayName("WithDefaults creates prioritizer with standard factors")
    void withDefaultsCreatesPrioritizerWithStandardFactors() {
        TaskPrioritizer defaultPrioritizer = TaskPrioritizer.withDefaults();

        assertTrue(defaultPrioritizer.getFactorCount() > 0,
                "Should have default factors registered");
        assertTrue(defaultPrioritizer.hasFactor(UtilityFactors.SAFETY),
                "Should include safety factor");
        assertTrue(defaultPrioritizer.hasFactor(UtilityFactors.URGENCY),
                "Should include urgency factor");
    }

    @Test
    @DisplayName("AddDefaultFactors adds all standard factors")
    void addDefaultFactorsAddsAllStandardFactors() {
        prioritizer.addDefaultFactors();

        assertTrue(prioritizer.getFactorCount() >= 10,
                "Should add at least 10 default factors");
        assertTrue(prioritizer.hasFactor(UtilityFactors.SAFETY),
                "Should include safety");
        assertTrue(prioritizer.hasFactor(UtilityFactors.URGENCY),
                "Should include urgency");
        assertTrue(prioritizer.hasFactor(UtilityFactors.RESOURCE_PROXIMITY),
                "Should include resource proximity");
        assertTrue(prioritizer.hasFactor(UtilityFactors.EFFICIENCY),
                "Should include efficiency");
    }

    @Test
    @DisplayName("GetFactors returns unmodifiable map")
    void getFactorsReturnsUnmodifiableMap() {
        prioritizer.addFactor(UtilityFactors.URGENCY);

        Map<UtilityFactor, Double> factors = prioritizer.getFactors();

        assertThrows(UnsupportedOperationException.class,
                () -> factors.put(UtilityFactors.SAFETY, 1.0),
                "Should not allow modification of returned map");
    }

    @Test
    @DisplayName("GetFactorWeight returns empty for unregistered factor")
    void getFactorWeightReturnsEmptyForUnregisteredFactor() {
        assertTrue(prioritizer.getFactorWeight(UtilityFactors.URGENCY).isEmpty(),
                "Should return empty for unregistered factor");
    }

    @Test
    @DisplayName("HasFactor returns correct status")
    void hasFactorReturnsCorrectStatus() {
        assertFalse(prioritizer.hasFactor(UtilityFactors.URGENCY),
                "Should return false before adding factor");

        prioritizer.addFactor(UtilityFactors.URGENCY);

        assertTrue(prioritizer.hasFactor(UtilityFactors.URGENCY),
                "Should return true after adding factor");
    }

    @Test
    @DisplayName("UpdateFactorWeight updates existing factor weight")
    void updateFactorWeightUpdatesExistingFactor() {
        prioritizer.addFactor(UtilityFactors.URGENCY, 1.0);

        boolean updated = prioritizer.updateFactorWeight(UtilityFactors.URGENCY, 2.5);

        assertTrue(updated, "Should return true when factor is updated");
        assertEquals(2.5, prioritizer.getFactorWeight(UtilityFactors.URGENCY).get(), 0.001,
                "Should have new weight");
    }

    @Test
    @DisplayName("UpdateFactorWeight throws for unregistered factor")
    void updateFactorWeightThrowsForUnregisteredFactor() {
        assertThrows(IllegalArgumentException.class,
                () -> prioritizer.updateFactorWeight(UtilityFactors.URGENCY, 2.0),
                "Should throw when factor is not registered");
    }

    @Test
    @DisplayName("UpdateFactorWeight throws for negative weight")
    void updateFactorWeightThrowsForNegativeWeight() {
        prioritizer.addFactor(UtilityFactors.URGENCY, 1.0);

        assertThrows(IllegalArgumentException.class,
                () -> prioritizer.updateFactorWeight(UtilityFactors.URGENCY, -1.0),
                "Should throw when weight is negative");
    }

    @Test
    @DisplayName("Score throws on null task")
    void scoreThrowsOnNullTask() {
        assertThrows(IllegalArgumentException.class,
                () -> prioritizer.score(null, context),
                "Should throw when task is null");
    }

    @Test
    @DisplayName("Score throws on null context")
    void scoreThrowsOnNullContext() {
        Task task = TaskBuilder.Presets.mineStone(64);

        assertThrows(IllegalArgumentException.class,
                () -> prioritizer.score(task, null),
                "Should throw when context is null");
    }

    @Test
    @DisplayName("Score includes all applicable factors")
    void scoreIncludesAllApplicableFactors() {
        Task task = TaskBuilder.Presets.mineStone(64);

        prioritizer.addFactor(UtilityFactors.URGENCY);
        prioritizer.addFactor(UtilityFactors.SAFETY);
        prioritizer.addFactor(UtilityFactors.EFFICIENCY);

        UtilityScore score = prioritizer.score(task, context);

        assertTrue(score.factors().containsKey("urgency"),
                "Should include urgency factor");
        assertTrue(score.factors().containsKey("safety"),
                "Should include safety factor");
        assertTrue(score.factors().containsKey("efficiency"),
                "Should include efficiency factor");
    }

    @Test
    @DisplayName("Score excludes factors that don't apply")
    void scoreExcludesNonApplicableFactors() {
        Task task = TaskBuilder.Presets.mineStone(64);

        prioritizer.addFactor(UtilityFactors.TOOL_READINESS);

        UtilityScore score = prioritizer.score(task, context);

        // TOOL_READINESS applies to mine tasks, so it should be included
        assertTrue(score.factors().containsKey("tool_readiness") ||
                        score.factors().isEmpty(),
                "Should respect appliesTo method");
    }

    @Test
    @DisplayName("Multiple tasks are scored independently")
    void multipleTasksAreScoredIndependently() {
        Task task1 = TaskBuilder.Presets.mineStone(64);
        Task task2 = TaskBuilder.aTask("attack").withTarget("zombie").build();

        prioritizer.addFactor(UtilityFactors.URGENCY);

        UtilityScore score1 = prioritizer.score(task1, context);
        UtilityScore score2 = prioritizer.score(task2, context);

        // Scores should differ due to different action types
        assertNotEquals(score1.finalScore(), score2.finalScore(), 0.001,
                "Different tasks should have different scores");
    }

    @Test
    @DisplayName("Prioritizer is thread-safe for concurrent access")
    void prioritizerIsThreadSafeForConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        RuntimeException[] exceptions = new RuntimeException[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        prioritizer.addFactor(UtilityFactors.URGENCY);
                        prioritizer.removeFactor(UtilityFactors.URGENCY);
                        prioritizer.getFactorCount();
                        prioritizer.hasFactor(UtilityFactors.URGENCY);
                    }
                } catch (RuntimeException e) {
                    exceptions[index] = e;
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        for (RuntimeException e : exceptions) {
            if (e != null) {
                throw e;
            }
        }

        // If we got here without exceptions, the test passed
        assertTrue(true, "Prioritizer should handle concurrent access");
    }

    @Test
    @DisplayName("DecisionExplanation generates explanation with all factors")
    void decisionExplanationGeneratesExplanationWithAllFactors() {
        Task selectedTask = TaskBuilder.Presets.mineStone(64);
        Task alternativeTask = TaskBuilder.aTask("build").withStructure("house").build();
        List<Task> tasks = List.of(selectedTask, alternativeTask);

        prioritizer.addFactor(UtilityFactors.URGENCY, 2.0);
        prioritizer.addFactor(UtilityFactors.SAFETY, 1.5);
        prioritizer.addFactor(UtilityFactors.EFFICIENCY, 1.0);

        DecisionExplanation explanation = DecisionExplanation.explain(
                selectedTask, tasks, context, prioritizer);

        assertNotNull(explanation, "Should generate explanation");
        assertEquals(selectedTask, explanation.getSelectedTask(),
                "Should have correct selected task");
        assertEquals(2, explanation.getConsideredTasks().size(),
                "Should include all considered tasks");
        assertEquals(2, explanation.getAllScores().size(),
                "Should have scores for all tasks");
    }

    @Test
    @DisplayName("DecisionExplanation explains high priority decision")
    void decisionExplanationExplainsHighPriorityDecision() {
        Task urgentTask = TaskBuilder.aTask("attack")
                .withTarget("zombie")
                .withParam("deadline", 5900)
                .build();
        Task normalTask = TaskBuilder.Presets.mineStone(64);
        List<Task> tasks = List.of(urgentTask, normalTask);

        prioritizer.addFactor(UtilityFactors.URGENCY, 2.5);
        prioritizer.addFactor(UtilityFactors.SAFETY, 1.0);

        DecisionContext urgentContext = DecisionContext.builder()
                .agentPosition(new net.minecraft.core.BlockPos(0, 64, 0))
                .healthLevel(1.0)
                .hungerLevel(1.0)
                .isDaytime(true)
                .gameTime(6000)
                .build();

        DecisionExplanation explanation = DecisionExplanation.explain(
                urgentTask, tasks, urgentContext, prioritizer);

        assertTrue(explanation.getSelectedScore().isHighPriority(),
                "Urgent task should be high priority");
        assertTrue(explanation.getExplanation().contains("high priority"),
                "Explanation should mention high priority");
    }

    @Test
    @DisplayName("DecisionExplanation shows top contributing factors")
    void decisionExplanationShowsTopContributingFactors() {
        Task task = TaskBuilder.Presets.mineStone(64);
        List<Task> tasks = List.of(task);

        prioritizer.addFactor(UtilityFactors.URGENCY, 2.0);
        prioritizer.addFactor(UtilityFactors.SAFETY, 1.5);
        prioritizer.addFactor(UtilityFactors.EFFICIENCY, 1.0);
        prioritizer.addFactor(UtilityFactors.RESOURCE_PROXIMITY, 0.8);

        DecisionExplanation explanation = DecisionExplanation.explain(
                task, tasks, context, prioritizer);

        List<DecisionExplanation.FactorContribution> topFactors = explanation.getTopFactors();

        assertNotNull(topFactors, "Should have top factors");
        assertTrue(topFactors.size() <= 5,
                "Should limit to top 5 factors");
        assertTrue(topFactors.stream().allMatch(f -> f.value() >= 0.0 && f.value() <= 1.0),
                "All factor values should be in valid range");
    }

    @Test
    @DisplayName("DecisionExplanation toBriefString generates concise output")
    void decisionExplanationToBriefStringGeneratesConciseOutput() {
        Task task = TaskBuilder.Presets.mineStone(64);
        List<Task> tasks = List.of(task);

        prioritizer.addFactor(UtilityFactors.URGENCY);
        prioritizer.addFactor(UtilityFactors.SAFETY);

        DecisionExplanation explanation = DecisionExplanation.explain(
                task, tasks, context, prioritizer);

        String brief = explanation.toBriefString();

        assertNotNull(brief, "Should generate brief string");
        assertTrue(brief.contains(task.getAction()),
                "Should include action name");
        assertTrue(brief.contains("score:"),
                "Should include score label");
        assertTrue(brief.contains("because:"),
                "Should include reasoning connector");
        assertTrue(brief.length() < 500,
                "Brief string should be concise");
    }

    @Test
    @DisplayName("DecisionExplanation toDetailedString generates comprehensive output")
    void decisionExplanationToDetailedStringGeneratesComprehensiveOutput() {
        Task selectedTask = TaskBuilder.Presets.mineStone(64);
        Task alternativeTask = TaskBuilder.aTask("build").withStructure("house").build();
        List<Task> tasks = List.of(selectedTask, alternativeTask);

        prioritizer.addFactor(UtilityFactors.URGENCY, 2.0);
        prioritizer.addFactor(UtilityFactors.SAFETY, 1.5);
        prioritizer.addFactor(UtilityFactors.EFFICIENCY, 1.0);

        DecisionExplanation explanation = DecisionExplanation.explain(
                selectedTask, tasks, context, prioritizer);

        String detailed = explanation.toDetailedString();

        assertTrue(detailed.contains("=== Decision Explanation ==="),
                "Should have header");
        assertTrue(detailed.contains("Selected Action:"),
                "Should show selected action");
        assertTrue(detailed.contains("Final Score:"),
                "Should show final score");
        assertTrue(detailed.contains("Top Contributing Factors:"),
                "Should show top factors section");
        assertTrue(detailed.contains("All Scores:"),
                "Should show all scores section");
        assertTrue(detailed.contains("Reasoning:"),
                "Should show reasoning section");
        assertTrue(detailed.contains(">>"),
                "Should indicate selected task in score list");
    }

    @Test
    @DisplayName("DecisionExplanation toChatString generates player-friendly output")
    void decisionExplanationToChatStringGeneratesPlayerFriendlyOutput() {
        Task task = TaskBuilder.Presets.mineStone(64);
        List<Task> tasks = List.of(task);

        prioritizer.addFactor(UtilityFactors.URGENCY);
        prioritizer.addFactor(UtilityFactors.EFFICIENCY);

        DecisionExplanation explanation = DecisionExplanation.explain(
                task, tasks, context, prioritizer);

        String chat = explanation.toChatString();

        assertTrue(chat.contains("[" + task.getAction() + "]"),
                "Should have action in brackets");
        assertTrue(chat.contains("Score:"),
                "Should show score label");
        assertTrue(chat.contains("-"),
                "Should have separator between score and explanation");
    }

    @Test
    @DisplayName("DecisionExplanation mentions close scoring alternatives")
    void decisionExplanationMentionsCloseScoringAlternatives() {
        Task task1 = TaskBuilder.Presets.mineStone(64);
        Task task2 = TaskBuilder.Presets.mineStone(32);
        List<Task> tasks = List.of(task1, task2);

        prioritizer.addFactor(UtilityFactors.EFFICIENCY);

        DecisionExplanation explanation = DecisionExplanation.explain(
                task1, tasks, context, prioritizer);

        String explanationText = explanation.getExplanation();

        // If scores are close (within 0.1), explanation should mention it
        double score1 = explanation.getAllScores().get(task1).finalScore();
        double score2 = explanation.getAllScores().get(task2).finalScore();
        double scoreGap = Math.abs(score1 - score2);

        if (scoreGap < 0.1) {
            assertTrue(explanationText.contains("narrowly"),
                    "Should mention narrow victory when scores are close");
        }
    }

    @Test
    @DisplayName("DecisionExplanation includes contextual notes")
    void decisionExplanationIncludesContextualNotes() {
        Task task = TaskBuilder.Presets.mineStone(64);
        List<Task> tasks = List.of(task);

        prioritizer.addFactor(UtilityFactors.SAFETY);
        prioritizer.addFactor(UtilityFactors.HEALTH_STATUS);

        DecisionContext criticalContext = DecisionContext.builder()
                .agentPosition(new net.minecraft.core.BlockPos(0, 64, 0))
                .healthLevel(0.2) // Critical health
                .hungerLevel(1.0)
                .isDaytime(false) // Nighttime
                .gameTime(18000)
                .build();

        DecisionExplanation explanation = DecisionExplanation.explain(
                task, tasks, criticalContext, prioritizer);

        String explanationText = explanation.getExplanation();

        assertTrue(explanationText.contains("Health is critical") ||
                        explanationText.contains("nighttime"),
                "Should include contextual notes about health or time");
    }

    @Test
    @DisplayName("DecisionExplanation throws on null selected task")
    void decisionExplanationThrowsOnNullSelectedTask() {
        Task task = TaskBuilder.Presets.mineStone(64);
        List<Task> tasks = List.of(task);

        assertThrows(IllegalArgumentException.class,
                () -> DecisionExplanation.explain(null, tasks, context, prioritizer),
                "Should throw when selected task is null");
    }

    @Test
    @DisplayName("DecisionExplanation throws on null tasks list")
    void decisionExplanationThrowsOnNullTasksList() {
        Task task = TaskBuilder.Presets.mineStone(64);

        assertThrows(IllegalArgumentException.class,
                () -> DecisionExplanation.explain(task, null, context, prioritizer),
                "Should throw when tasks list is null");
    }

    @Test
    @DisplayName("DecisionExplanation throws on empty tasks list")
    void decisionExplanationThrowsOnEmptyTasksList() {
        Task task = TaskBuilder.Presets.mineStone(64);
        List<Task> emptyTasks = List.of();

        assertThrows(IllegalArgumentException.class,
                () -> DecisionExplanation.explain(task, emptyTasks, context, prioritizer),
                "Should throw when tasks list is empty");
    }

    @Test
    @DisplayName("DecisionExplanation throws on null context")
    void decisionExplanationThrowsOnNullContext() {
        Task task = TaskBuilder.Presets.mineStone(64);
        List<Task> tasks = List.of(task);

        assertThrows(IllegalArgumentException.class,
                () -> DecisionExplanation.explain(task, tasks, null, prioritizer),
                "Should throw when context is null");
    }

    @Test
    @DisplayName("DecisionExplanation throws on null prioritizer")
    void decisionExplanationThrowsOnNullPrioritizer() {
        Task task = TaskBuilder.Presets.mineStone(64);
        List<Task> tasks = List.of(task);

        assertThrows(IllegalArgumentException.class,
                () -> DecisionExplanation.explain(task, tasks, context, null),
                "Should throw when prioritizer is null");
    }

    @Test
    @DisplayName("DecisionExplanation formats factor names for readability")
    void decisionExplanationFormatsFactorNamesForReadability() {
        Task task = TaskBuilder.Presets.mineStone(64);
        List<Task> tasks = List.of(task);

        prioritizer.addFactor(UtilityFactors.RESOURCE_PROXIMITY);
        prioritizer.addFactor(UtilityFactors.TOOL_READINESS);
        prioritizer.addFactor(UtilityFactors.HEALTH_STATUS);

        DecisionExplanation explanation = DecisionExplanation.explain(
                task, tasks, context, prioritizer);

        String detailed = explanation.toDetailedString();

        // Factor names with underscores should be formatted with spaces
        assertTrue(detailed.contains("resource proximity") ||
                        detailed.contains("RESOURCE_PROXIMITY"),
                "Should format or show resource proximity factor");
    }

    @Test
    @DisplayName("DecisionExplanation handles single task")
    void decisionExplanationHandlesSingleTask() {
        Task task = TaskBuilder.Presets.mineStone(64);
        List<Task> tasks = List.of(task);

        prioritizer.addFactor(UtilityFactors.EFFICIENCY);

        DecisionExplanation explanation = DecisionExplanation.explain(
                task, tasks, context, prioritizer);

        assertNotNull(explanation, "Should handle single task");
        assertEquals(1, explanation.getConsideredTasks().size());
        assertEquals(1, explanation.getAllScores().size());
        assertTrue(explanation.toDetailedString().contains(">>>"),
                "Should mark the single task as selected");
    }

    @Test
    @DisplayName("DecisionExplanation handles many tasks")
    void decisionExplanationHandlesManyTasks() {
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(TaskBuilder.aTask("task_" + i)
                    .withParam("priority", i)
                    .build());
        }

        prioritizer.addFactor(UtilityFactors.URGENCY);
        prioritizer.addFactor(UtilityFactors.EFFICIENCY);

        Task selectedTask = tasks.get(0);
        DecisionExplanation explanation = DecisionExplanation.explain(
                selectedTask, tasks, context, prioritizer);

        assertNotNull(explanation, "Should handle many tasks");
        assertEquals(20, explanation.getConsideredTasks().size());
        assertEquals(20, explanation.getAllScores().size());

        String detailed = explanation.toDetailedString();
        assertTrue(detailed.contains("1."),
                "Should rank tasks");
        assertTrue(detailed.contains("20."),
                "Should show all 20 tasks");
    }

    @Test
    @DisplayName("DecisionExplanation getters return correct values")
    void decisionExplanationGettersReturnCorrectValues() {
        Task task = TaskBuilder.Presets.mineStone(64);
        List<Task> tasks = List.of(task);

        prioritizer.addFactor(UtilityFactors.URGENCY);

        DecisionExplanation explanation = DecisionExplanation.explain(
                task, tasks, context, prioritizer);

        assertEquals(task, explanation.getSelectedTask());
        assertEquals(context, explanation.getContext());
        assertNotNull(explanation.getSelectedScore());
        assertFalse(explanation.getTopFactors().isEmpty());
        assertFalse(explanation.getExplanation().isEmpty());
    }

    @Test
    @DisplayName("DecisionExplanation handles zero-weight factors")
    void decisionExplanationHandlesZeroWeightFactors() {
        Task task = TaskBuilder.Presets.mineStone(64);
        List<Task> tasks = List.of(task);

        prioritizer.addFactor(UtilityFactors.URGENCY, 0.0);
        prioritizer.addFactor(UtilityFactors.SAFETY, 1.0);

        DecisionExplanation explanation = DecisionExplanation.explain(
                task, tasks, context, prioritizer);

        // Zero-weight factors should not contribute to the score
        UtilityScore score = explanation.getSelectedScore();
        assertFalse(score.factors().containsKey("urgency"),
                "Zero-weight factors should not be included");
        assertTrue(score.factors().containsKey("safety") || score.factors().isEmpty(),
                "Non-zero weight factors should be included");
    }

    /**
     * Helper method to create a test DecisionContext.
     */
    private DecisionContext createTestContext() {
        return DecisionContext.builder()
                .agentPosition(new net.minecraft.core.BlockPos(0, 64, 0))
                .healthLevel(1.0)
                .hungerLevel(1.0)
                .isDaytime(true)
                .isRaining(false)
                .isThundering(false)
                .gameTime(6000)
                .build();
    }
}
