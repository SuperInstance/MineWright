package com.minewright.decision;

import com.minewright.action.Task;
import com.minewright.testutil.TaskBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UtilityScore}.
 *
 * Tests cover:
 * <ul>
 *   <li>Score calculation with multiple factors</li>
 *   <li>Score bounds validation (0.0 to 1.0)</li>
 *   <li>Base value handling</li>
 *   <li>Factor value retrieval and validation</li>
 *   <li>Priority classification methods</li>
 *   <li>String representation and comparison</li>
 * </ul>
 */
@DisplayName("Utility Score Tests")
class UtilityScoreTest {

    @Test
    @DisplayName("Score combines multiple factors correctly")
    void scoreCombinesFactors() {
        Task task = TaskBuilder.Presets.mineStone(64);
        DecisionContext context = createTestContext();

        TaskPrioritizer prioritizer = new TaskPrioritizer();
        prioritizer.addFactor(UtilityFactors.URGENCY, 1.0);
        prioritizer.addFactor(UtilityFactors.SAFETY, 1.0);
        prioritizer.addFactor(UtilityFactors.EFFICIENCY, 1.0);

        UtilityScore score = prioritizer.score(task, context);

        assertTrue(score.finalScore() >= 0.0 && score.finalScore() <= 1.0,
                "Final score should be within valid range");
        assertFalse(score.factors().isEmpty(),
                "Score should contain factor values");
        assertTrue(score.factors().containsKey("urgency"),
                "Score should contain urgency factor");
        assertTrue(score.factors().containsKey("safety"),
                "Score should contain safety factor");
        assertTrue(score.factors().containsKey("efficiency"),
                "Score should contain efficiency factor");
    }

    @Test
    @DisplayName("Score is bounded between 0.0 and 1.0")
    void scoreIsBounded() {
        Task task = TaskBuilder.Presets.mineStone(64);
        DecisionContext context = createTestContext();

        TaskPrioritizer prioritizer = new TaskPrioritizer();
        prioritizer.addFactor(UtilityFactors.URGENCY, 1.0);

        UtilityScore score = prioritizer.score(task, context);

        assertTrue(score.finalScore() >= UtilityScore.MIN_SCORE,
                "Score should not be below minimum");
        assertTrue(score.finalScore() <= UtilityScore.MAX_SCORE,
                "Score should not exceed maximum");
        assertEquals(0.0, UtilityScore.MIN_SCORE, "Minimum should be 0.0");
        assertEquals(1.0, UtilityScore.MAX_SCORE, "Maximum should be 1.0");
    }

    @Test
    @DisplayName("Zero factors gives base value")
    void zeroFactorsGivesBaseValue() {
        Task task = TaskBuilder.Presets.mineStone(64);
        DecisionContext context = createTestContext();

        TaskPrioritizer prioritizer = new TaskPrioritizer();
        // No factors added

        UtilityScore score = prioritizer.score(task, context);

        assertEquals(0.5, score.finalScore(), 0.001,
                "Score with no factors should equal base value");
        assertEquals(0.5, score.baseValue(), 0.001,
                "Base value should be 0.5");
        assertTrue(score.factors().isEmpty(),
                "No factors should be recorded");
    }

    @Test
    @DisplayName("Constructor validates base value range")
    void constructorValidatesBaseValue() {
        Map<String, Double> factors = Map.of("urgency", 0.8);

        assertThrows(IllegalArgumentException.class,
                () -> new UtilityScore(-0.1, factors, 0.5),
                "Should reject base value below 0.0");

        assertThrows(IllegalArgumentException.class,
                () -> new UtilityScore(1.1, factors, 0.5),
                "Should reject base value above 1.0");
    }

    @Test
    @DisplayName("Constructor validates final score range")
    void constructorValidatesFinalScore() {
        Map<String, Double> factors = Map.of("urgency", 0.8);

        assertThrows(IllegalArgumentException.class,
                () -> new UtilityScore(0.5, factors, -0.1),
                "Should reject final score below 0.0");

        assertThrows(IllegalArgumentException.class,
                () -> new UtilityScore(0.5, factors, 1.1),
                "Should reject final score above 1.0");
    }

    @Test
    @DisplayName("Constructor validates factor value ranges")
    void constructorValidatesFactorValues() {
        Map<String, Double> invalidFactors = new HashMap<>();
        invalidFactors.put("urgency", 0.8);
        invalidFactors.put("safety", -0.1); // Invalid

        assertThrows(IllegalArgumentException.class,
                () -> new UtilityScore(0.5, invalidFactors, 0.7),
                "Should reject factor value below 0.0");

        invalidFactors.put("safety", 1.5); // Invalid
        assertThrows(IllegalArgumentException.class,
                () -> new UtilityScore(0.5, invalidFactors, 0.7),
                "Should reject factor value above 1.0");
    }

    @Test
    @DisplayName("Constructor creates immutable factors map")
    void constructorCreatesImmutableFactorsMap() {
        Map<String, Double> originalFactors = new HashMap<>();
        originalFactors.put("urgency", 0.8);
        originalFactors.put("safety", 0.6);

        UtilityScore score = new UtilityScore(0.5, originalFactors, 0.7);

        // Try to modify the original map
        originalFactors.put("efficiency", 0.9);

        // The score's factors should not be affected
        assertEquals(2, score.factors().size(),
                "Factors map should be immutable");
        assertFalse(score.factors().containsKey("efficiency"),
                "Should not reflect modifications to original map");
    }

    @Test
    @DisplayName("isHighPriority returns true for scores >= 0.7")
    void isHighPriorityReturnsTrueForHighScores() {
        UtilityScore highScore = new UtilityScore(0.5, Map.of(), 0.7);
        assertTrue(highScore.isHighPriority(),
                "Score of 0.7 should be high priority");

        UtilityScore veryHighScore = new UtilityScore(0.5, Map.of(), 0.9);
        assertTrue(veryHighScore.isHighPriority(),
                "Score of 0.9 should be high priority");
    }

    @Test
    @DisplayName("isHighPriority returns false for scores < 0.7")
    void isHighPriorityReturnsFalseForLowerScores() {
        UtilityScore mediumScore = new UtilityScore(0.5, Map.of(), 0.69);
        assertFalse(mediumScore.isHighPriority(),
                "Score of 0.69 should not be high priority");

        UtilityScore lowScore = new UtilityScore(0.5, Map.of(), 0.3);
        assertFalse(lowScore.isHighPriority(),
                "Score of 0.3 should not be high priority");
    }

    @Test
    @DisplayName("isLowPriority returns true for scores <= 0.3")
    void isLowPriorityReturnsTrueForLowScores() {
        UtilityScore lowScore = new UtilityScore(0.5, Map.of(), 0.3);
        assertTrue(lowScore.isLowPriority(),
                "Score of 0.3 should be low priority");

        UtilityScore veryLowScore = new UtilityScore(0.5, Map.of(), 0.1);
        assertTrue(veryLowScore.isLowPriority(),
                "Score of 0.1 should be low priority");
    }

    @Test
    @DisplayName("isLowPriority returns false for scores > 0.3")
    void isLowPriorityReturnsFalseForHigherScores() {
        UtilityScore mediumScore = new UtilityScore(0.5, Map.of(), 0.31);
        assertFalse(mediumScore.isLowPriority(),
                "Score of 0.31 should not be low priority");

        UtilityScore highScore = new UtilityScore(0.5, Map.of(), 0.7);
        assertFalse(highScore.isLowPriority(),
                "Score of 0.7 should not be low priority");
    }

    @Test
    @DisplayName("getFactorValue returns value for existing factor")
    void getFactorValueReturnsValueForExistingFactor() {
        Map<String, Double> factors = new HashMap<>();
        factors.put("urgency", 0.8);
        factors.put("safety", 0.6);

        UtilityScore score = new UtilityScore(0.5, factors, 0.7);

        assertEquals(0.8, score.getFactorValue("urgency").orElse(0.0), 0.001,
                "Should return urgency factor value");
        assertEquals(0.6, score.getFactorValue("safety").orElse(0.0), 0.001,
                "Should return safety factor value");
    }

    @Test
    @DisplayName("getFactorValue returns empty for missing factor")
    void getFactorValueReturnsEmptyForMissingFactor() {
        Map<String, Double> factors = Map.of("urgency", 0.8);
        UtilityScore score = new UtilityScore(0.5, factors, 0.7);

        assertTrue(score.getFactorValue("nonexistent").isEmpty(),
                "Should return empty for missing factor");
    }

    @Test
    @DisplayName("toDetailedString shows all factor contributions")
    void toDetailedStringShowsAllFactorContributions() {
        Map<String, Double> factors = new TreeMap<>();
        factors.put("urgency", 0.8);
        factors.put("safety", 0.6);
        factors.put("efficiency", 0.9);

        UtilityScore score = new UtilityScore(0.5, factors, 0.75);
        String detailed = score.toDetailedString();

        assertTrue(detailed.contains("0.75"),
                "Should show final score");
        assertTrue(detailed.contains("urgency=0.80"),
                "Should show urgency factor");
        assertTrue(detailed.contains("safety=0.60"),
                "Should show safety factor");
        assertTrue(detailed.contains("efficiency=0.90"),
                "Should show efficiency factor");
    }

    @Test
    @DisplayName("toDetailedString handles empty factors")
    void toDetailedStringHandlesEmptyFactors() {
        UtilityScore score = new UtilityScore(0.5, Map.of(), 0.5);
        String detailed = score.toDetailedString();

        assertTrue(detailed.contains("0.50"),
                "Should show score");
        assertFalse(detailed.contains("{"),
                "Should not show empty braces for no factors");
    }

    @Test
    @DisplayName("compareTo compares scores correctly")
    void compareToComparesScoresCorrectly() {
        UtilityScore lowScore = new UtilityScore(0.5, Map.of(), 0.3);
        UtilityScore mediumScore = new UtilityScore(0.5, Map.of(), 0.5);
        UtilityScore highScore = new UtilityScore(0.5, Map.of(), 0.8);

        assertTrue(lowScore.compareTo(mediumScore) < 0,
                "Low score should be less than medium");
        assertTrue(mediumScore.compareTo(highScore) < 0,
                "Medium score should be less than high");
        assertTrue(highScore.compareTo(lowScore) > 0,
                "High score should be greater than low");

        UtilityScore equalScore = new UtilityScore(0.5, Map.of(), 0.5);
        assertEquals(0, mediumScore.compareTo(equalScore),
                "Equal scores should compare to zero");
    }

    @Test
    @DisplayName("calculate factory method throws on null task")
    void calculateThrowsOnNullTask() {
        DecisionContext context = createTestContext();

        assertThrows(IllegalArgumentException.class,
                () -> UtilityScore.calculate(null, context),
                "Should throw when task is null");
    }

    @Test
    @DisplayName("calculate factory method throws on null context")
    void calculateThrowsOnNullContext() {
        Task task = TaskBuilder.Presets.mineStone(64);

        assertThrows(IllegalArgumentException.class,
                () -> UtilityScore.calculate(task, null),
                "Should throw when context is null");
    }

    @Test
    @DisplayName("calculate returns neutral score when no prioritizer")
    void calculateReturnsNeutralScoreWhenNoPrioritizer() {
        Task task = TaskBuilder.Presets.mineStone(64);
        DecisionContext context = DecisionContext.builder()
                .agentPosition(net.minecraft.core.BlockPos.ZERO)
                .build();

        UtilityScore score = UtilityScore.calculate(task, context);

        assertEquals(0.5, score.finalScore(), 0.001,
                "Should return neutral score when no prioritizer");
        assertTrue(score.factors().isEmpty(),
                "Should have no factors");
    }

    @Test
    @DisplayName("Weighted factors influence final score")
    void weightedFactorsInfluenceFinalScore() {
        Task task = TaskBuilder.Presets.mineStone(64);
        DecisionContext context = createTestContext();

        TaskPrioritizer highUrgencyPrioritizer = new TaskPrioritizer();
        highUrgencyPrioritizer.addFactor(UtilityFactors.URGENCY, 2.0);
        highUrgencyPrioritizer.addFactor(UtilityFactors.EFFICIENCY, 0.5);

        TaskPrioritizer highEfficiencyPrioritizer = new TaskPrioritizer();
        highEfficiencyPrioritizer.addFactor(UtilityFactors.URGENCY, 0.5);
        highEfficiencyPrioritizer.addFactor(UtilityFactors.EFFICIENCY, 2.0);

        UtilityScore score1 = highUrgencyPrioritizer.score(task, context);
        UtilityScore score2 = highEfficiencyPrioritizer.score(task, context);

        // Scores should differ based on weight configuration
        assertNotEquals(score1.finalScore(), score2.finalScore(), 0.001,
                "Different weights should produce different scores");
    }

    @Test
    @DisplayName("Zero weight factors are ignored")
    void zeroWeightFactorsAreIgnored() {
        Task task = TaskBuilder.Presets.mineStone(64);
        DecisionContext context = createTestContext();

        TaskPrioritizer prioritizer = new TaskPrioritizer();
        prioritizer.addFactor(UtilityFactors.URGENCY, 0.0);
        prioritizer.addFactor(UtilityFactors.SAFETY, 1.0);

        UtilityScore score = prioritizer.score(task, context);

        assertFalse(score.factors().containsKey("urgency"),
                "Zero-weight factor should not be included");
        assertTrue(score.factors().containsKey("safety"),
                "Non-zero weight factor should be included");
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
