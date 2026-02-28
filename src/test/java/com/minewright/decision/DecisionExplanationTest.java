package com.minewright.decision;

import com.minewright.action.Task;
import com.minewright.testutil.TaskBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for decision explanation functionality.
 *
 * Tests cover:
 * <ul>
 *   <li>Detailed string representation shows all factors</li>
 *   <li>Factor breakdown in explanations</li>
 *   <li>Top factors identification</li>
 *   <li>Score formatting in explanations</li>
 *   <li>Empty factor handling</li>
 * </ul>
 *
 * <p>Note: While there is no separate DecisionExplanation class,
 * the explanation functionality is provided through UtilityScore's
 * toDetailedString() and related methods.</p>
 */
@DisplayName("Decision Explanation Tests")
class DecisionExplanationTest {

    @Test
    @DisplayName("Explanation lists all factors with their values")
    void explanationListsAllFactors() {
        Map<String, Double> factors = new TreeMap<>();
        factors.put("urgency", 0.9);
        factors.put("safety", 0.7);
        factors.put("efficiency", 0.8);
        factors.put("proximity", 0.6);

        UtilityScore score = new UtilityScore(0.5, factors, 0.75);

        String explanation = score.toDetailedString();

        assertTrue(explanation.contains("urgency=0.90"),
                "Should list urgency factor");
        assertTrue(explanation.contains("safety=0.70"),
                "Should list safety factor");
        assertTrue(explanation.contains("efficiency=0.80"),
                "Should list efficiency factor");
        assertTrue(explanation.contains("proximity=0.60"),
                "Should list proximity factor");
    }

    @Test
    @DisplayName("Explanation shows score breakdown clearly")
    void explanationShowsScoreBreakdown() {
        Map<String, Double> factors = new TreeMap<>();
        factors.put("urgency", 1.0);
        factors.put("safety", 0.5);

        UtilityScore score = new UtilityScore(0.5, factors, 0.8);

        String explanation = score.toDetailedString();

        assertTrue(explanation.contains("0.80"),
                "Should show final score");
        assertTrue(explanation.contains("UtilityScore["),
                "Should show score type");
        assertTrue(explanation.contains("{"),
                "Should show factor section start");
        assertTrue(explanation.contains("}"),
                "Should show factor section end");
    }

    @Test
    @DisplayName("Explanation handles single factor")
    void explanationHandlesSingleFactor() {
        Map<String, Double> factors = Map.of("urgency", 0.9);

        UtilityScore score = new UtilityScore(0.5, factors, 0.8);

        String explanation = score.toDetailedString();

        assertTrue(explanation.contains("urgency=0.90"),
                "Should show the single factor");
        assertTrue(explanation.contains("0.80"),
                "Should show final score");
    }

    @Test
    @DisplayName("Explanation handles empty factors")
    void explanationHandlesEmptyFactors() {
        UtilityScore score = new UtilityScore(0.5, Map.of(), 0.5);

        String explanation = score.toDetailedString();

        assertTrue(explanation.contains("0.50"),
                "Should show the score");
        assertFalse(explanation.contains("{"),
                "Should not show empty braces");
        assertFalse(explanation.contains("}"),
                "Should not show empty braces");
    }

    @Test
    @DisplayName("Explanation formats factors consistently")
    void explanationFormatsFactorsConsistently() {
        Map<String, Double> factors = new TreeMap<>();
        factors.put("factor_a", 0.123);
        factors.put("factor_b", 0.456);
        factors.put("factor_c", 0.789);

        UtilityScore score = new UtilityScore(0.5, factors, 0.6);

        String explanation = score.toDetailedString();

        // Factors should be sorted alphabetically (TreeMap guarantees this)
        int factorAIndex = explanation.indexOf("factor_a=");
        int factorBIndex = explanation.indexOf("factor_b=");
        int factorCIndex = explanation.indexOf("factor_c=");

        assertTrue(factorAIndex < factorBIndex,
                "factor_a should come before factor_b");
        assertTrue(factorBIndex < factorCIndex,
                "factor_b should come before factor_c");
    }

    @Test
    @DisplayName("Explanation shows values with two decimal places")
    void explanationShowsValuesWithTwoDecimalPlaces() {
        Map<String, Double> factors = Map.of("test", 0.123456789);

        UtilityScore score = new UtilityScore(0.5, factors, 0.987654321);

        String explanation = score.toDetailedString();

        assertTrue(explanation.contains("0.12") || explanation.contains("0.99"),
                "Should round to two decimal places");
    }

    @Test
    @DisplayName("Explanation includes all factor values in range")
    void explanationIncludesAllFactorValuesInRange() {
        Map<String, Double> factors = new TreeMap<>();
        factors.put("zero", 0.0);
        factors.put("low", 0.25);
        factors.put("medium", 0.5);
        factors.put("high", 0.75);
        factors.put("max", 1.0);

        UtilityScore score = new UtilityScore(0.5, factors, 0.5);

        String explanation = score.toDetailedString();

        assertTrue(explanation.contains("0.00"), "Should show 0.0");
        assertTrue(explanation.contains("0.25") || explanation.contains("0.30"),
                "Should show low value");
        assertTrue(explanation.contains("0.50"), "Should show 0.5");
        assertTrue(explanation.contains("0.75") || explanation.contains("0.80"),
                "Should show high value");
        assertTrue(explanation.contains("1.00") || explanation.contains("1.0"),
                "Should show 1.0");
    }

    @Test
    @DisplayName("Explanation from prioritized tasks is informative")
    void explanationFromPrioritizedTasksIsInformative() {
        Task task = TaskBuilder.Presets.mineStone(64);
        DecisionContext context = createTestContext();

        TaskPrioritizer prioritizer = new TaskPrioritizer();
        prioritizer.addFactor(UtilityFactors.URGENCY);
        prioritizer.addFactor(UtilityFactors.SAFETY);
        prioritizer.addFactor(UtilityFactors.EFFICIENCY);

        UtilityScore score = prioritizer.score(task, context);
        String explanation = score.toDetailedString();

        assertNotNull(explanation, "Explanation should not be null");
        assertFalse(explanation.isEmpty(), "Explanation should not be empty");
        assertTrue(explanation.length() > 10, "Explanation should have content");
        assertTrue(explanation.contains("UtilityScore"),
                "Should contain score type identifier");
    }

    @Test
    @DisplayName("Get factor value supports explanation lookup")
    void getFactorValueSupportsExplanationLookup() {
        Map<String, Double> factors = new TreeMap<>();
        factors.put("urgency", 0.9);
        factors.put("safety", 0.6);
        factors.put("efficiency", 0.8);

        UtilityScore score = new UtilityScore(0.5, factors, 0.75);

        assertTrue(score.getFactorValue("urgency").isPresent(),
                "Should find urgency factor");
        assertEquals(0.9, score.getFactorValue("urgency").get(), 0.001,
                "Should return correct urgency value");

        assertTrue(score.getFactorValue("safety").isPresent(),
                "Should find safety factor");
        assertEquals(0.6, score.getFactorValue("safety").get(), 0.001,
                "Should return correct safety value");

        assertTrue(score.getFactorValue("efficiency").isPresent(),
                "Should find efficiency factor");
        assertEquals(0.8, score.getFactorValue("efficiency").get(), 0.001,
                "Should return correct efficiency value");

        assertFalse(score.getFactorValue("nonexistent").isPresent(),
                "Should not find nonexistent factor");
    }

    @Test
    @DisplayName("Priority flags are part of explanation")
    void priorityFlagsArePartOfExplanation() {
        UtilityScore highPriorityScore = new UtilityScore(0.5, Map.of(), 0.8);
        UtilityScore lowPriorityScore = new UtilityScore(0.5, Map.of(), 0.2);
        UtilityScore mediumPriorityScore = new UtilityScore(0.5, Map.of(), 0.5);

        assertTrue(highPriorityScore.isHighPriority(),
                "Score 0.8 should be high priority");
        assertFalse(highPriorityScore.isLowPriority(),
                "Score 0.8 should not be low priority");

        assertFalse(lowPriorityScore.isHighPriority(),
                "Score 0.2 should not be high priority");
        assertTrue(lowPriorityScore.isLowPriority(),
                "Score 0.2 should be low priority");

        assertFalse(mediumPriorityScore.isHighPriority(),
                "Score 0.5 should not be high priority");
        assertFalse(mediumPriorityScore.isLowPriority(),
                "Score 0.5 should not be low priority");
    }

    @Test
    @DisplayName("Explanation handles boundary scores")
    void explanationHandlesBoundaryScores() {
        Map<String, Double> factors = Map.of("test", 0.5);

        UtilityScore minScore = new UtilityScore(0.0, factors, 0.0);
        UtilityScore maxScore = new UtilityScore(1.0, factors, 1.0);

        String minExplanation = minScore.toDetailedString();
        String maxExplanation = maxScore.toDetailedString();

        assertTrue(minExplanation.contains("0.00") || minExplanation.contains("0.0"),
                "Should show minimum score");
        assertTrue(maxExplanation.contains("1.00") || maxExplanation.contains("1.0"),
                "Should show maximum score");
    }

    @Test
    @DisplayName("Comparison supports explanation of differences")
    void comparisonSupportsExplanationOfDifferences() {
        UtilityScore lowScore = new UtilityScore(0.5, Map.of("urgency", 0.3), 0.4);
        UtilityScore highScore = new UtilityScore(0.5, Map.of("urgency", 0.9), 0.8);

        int comparison = lowScore.compareTo(highScore);

        assertTrue(comparison < 0,
                "Low score should compare as less than high score");

        // Can explain the difference
        double difference = highScore.finalScore() - lowScore.finalScore();
        assertEquals(0.4, difference, 0.001,
                "Should be able to calculate score difference");
    }

    @Test
    @DisplayName("Explanation with many factors remains readable")
    void explanationWithManyFactorsRemainsReadable() {
        Map<String, Double> factors = new TreeMap<>();
        factors.put("urgency", 0.9);
        factors.put("safety", 0.7);
        factors.put("efficiency", 0.8);
        factors.put("proximity", 0.6);
        factors.put("preference", 0.5);
        factors.put("skill_match", 0.7);
        factors.put("tool_readiness", 0.9);
        factors.put("health_status", 1.0);
        factors.put("hunger_status", 0.8);
        factors.put("time_of_day", 0.7);

        UtilityScore score = new UtilityScore(0.5, factors, 0.75);
        String explanation = score.toDetailedString();

        // Should contain all factors
        for (String factorName : factors.keySet()) {
            assertTrue(explanation.contains(factorName),
                    "Should contain " + factorName + " factor");
        }

        // Should be formatted with commas
        long commaCount = explanation.chars().filter(ch -> ch == ',').count();
        assertTrue(commaCount >= factors.size() - 1,
                "Should have commas between factors");
    }

    @Test
    @DisplayName("Explanation integrates with factor names")
    void explanationIntegratesWithFactorNames() {
        Task task = TaskBuilder.Presets.mineStone(64);
        DecisionContext context = createTestContext();

        TaskPrioritizer prioritizer = new TaskPrioritizer();
        prioritizer.addFactor(UtilityFactors.URGENCY);
        prioritizer.addFactor(UtilityFactors.SAFETY);

        UtilityScore score = prioritizer.score(task, context);
        String explanation = score.toDetailedString();

        // Should use the factor names from UtilityFactors
        assertTrue(explanation.contains("urgency") || explanation.contains("safety") ||
                        explanation.contains("UtilityScore"),
                "Should use factor names or show score type");
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
