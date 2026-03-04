package com.minewright.evaluation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BenchmarkScenarios}.
 *
 * <p>Tests cover scenario definitions, complexity levels, starting conditions,
 * success criteria, expected metrics, and scenario retrieval.</p>
 *
 * @since 1.0.0
 */
@DisplayName("BenchmarkScenarios Tests")
class BenchmarkScenariosTest {

    // ========== Scenario Definition Tests ==========

    @Test
    @DisplayName("SIMPLE_MINE_10_STONE scenario should be properly configured")
    void testSimpleMine10StoneScenario() {
        BenchmarkScenarios.BenchmarkScenario scenario =
            BenchmarkScenarios.SIMPLE_MINE_10_STONE;

        assertEquals("Simple: Mine 10 Stone", scenario.getName());
        assertEquals("Mine 10 stone", scenario.getNaturalLanguageCommand());
        assertEquals(BenchmarkScenarios.Complexity.SIMPLE, scenario.getComplexity());
    }

    @Test
    @DisplayName("MEDIUM_BUILD_5x5_HOUSE scenario should be properly configured")
    void testMediumBuildHouseScenario() {
        BenchmarkScenarios.BenchmarkScenario scenario =
            BenchmarkScenarios.MEDIUM_BUILD_5x5_HOUSE;

        assertEquals("Medium: Build 5x5 House", scenario.getName());
        assertTrue(scenario.getNaturalLanguageCommand().contains("5x5"));
        assertEquals(BenchmarkScenarios.Complexity.MODERATE, scenario.getComplexity());
    }

    @Test
    @DisplayName("COMPLEX_AUTOMATED_FARM scenario should be properly configured")
    void testComplexAutomatedFarmScenario() {
        BenchmarkScenarios.BenchmarkScenario scenario =
            BenchmarkScenarios.COMPLEX_AUTOMATED_FARM;

        assertEquals("Complex: Automated Wheat Farm", scenario.getName());
        assertTrue(scenario.getNaturalLanguageCommand().contains("automated wheat farm"));
        assertEquals(BenchmarkScenarios.Complexity.COMPLEX, scenario.getComplexity());
    }

    @Test
    @DisplayName("MULTI_AGENT_VILLAGE scenario should be properly configured")
    void testMultiAgentVillageScenario() {
        BenchmarkScenarios.BenchmarkScenario scenario =
            BenchmarkScenarios.MULTI_AGENT_VILLAGE;

        assertEquals("Multi-Agent: Village with 3 Workers", scenario.getName());
        assertTrue(scenario.getNaturalLanguageCommand().contains("3 workers"));
        assertEquals(BenchmarkScenarios.Complexity.COMPLEX_MULTI_AGENT, scenario.getComplexity());
    }

    // ========== StartingConditions Tests ==========

    @Test
    @DisplayName("StartingConditions should store spawn position")
    void testStartingConditionsSpawnPosition() {
        BlockPos expectedPos = new BlockPos(100, 65, 200);
        BenchmarkScenarios.StartingConditions conditions =
            new BenchmarkScenarios.StartingConditions(expectedPos, List.of());

        assertEquals(expectedPos, conditions.getSpawnPosition());
    }

    @Test
    @DisplayName("StartingConditions should store inventory items")
    void testStartingConditionsInventory() {
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack torch = new ItemStack(Items.TORCH, 64);

        BenchmarkScenarios.StartingConditions conditions =
            new BenchmarkScenarios.StartingConditions(
                new BlockPos(0, 65, 0),
                List.of(pickaxe, torch)
            );

        List<ItemStack> inventory = conditions.getInventoryItems();
        assertEquals(2, inventory.size());
        assertTrue(inventory.contains(pickaxe));
        assertTrue(inventory.contains(torch));
    }

    @Test
    @DisplayName("StartingConditions should default to 1 agent")
    void testStartingConditionsDefaultAgents() {
        BenchmarkScenarios.StartingConditions conditions =
            new BenchmarkScenarios.StartingConditions(
                new BlockPos(0, 65, 0),
                List.of()
            );

        assertEquals(1, conditions.getNumberOfAgents());
    }

    @Test
    @DisplayName("StartingConditions should store number of agents")
    void testStartingConditionsMultipleAgents() {
        BenchmarkScenarios.StartingConditions conditions =
            new BenchmarkScenarios.StartingConditions(
                new BlockPos(0, 65, 0),
                List.of(),
                5
            );

        assertEquals(5, conditions.getNumberOfAgents());
    }

    // ========== SuccessCriteria Tests ==========

    @Test
    @DisplayName("SuccessCriteria should store all fields")
    void testSuccessCriteriaStoresFields() {
        BenchmarkScenarios.SuccessCriteria criteria =
            new BenchmarkScenarios.SuccessCriteria(
                100,     // targetBlockCount
                300,     // maxExecutionTimeSeconds
                0.85,    // minCorrectnessScore
                false    // requireExactBlockCount
            );

        assertEquals(100, criteria.getTargetBlockCount());
        assertEquals(300, criteria.getMaxExecutionTimeSeconds());
        assertEquals(0.85, criteria.getMinCorrectnessScore(), 0.001);
        assertFalse(criteria.isRequireExactBlockCount());
    }

    @Test
    @DisplayName("SuccessCriteria for simple scenario should require exact count")
    void testSuccessCriteriaSimpleScenario() {
        BenchmarkScenarios.SuccessCriteria criteria =
            BenchmarkScenarios.SIMPLE_MINE_10_STONE.getSuccessCriteria();

        assertEquals(10, criteria.getTargetBlockCount());
        assertTrue(criteria.isRequireExactBlockCount());
        assertEquals(0.95, criteria.getMinCorrectnessScore(), 0.001);
    }

    @Test
    @DisplayName("SuccessCriteria for complex scenario should allow tolerance")
    void testSuccessCriteriaComplexScenario() {
        BenchmarkScenarios.SuccessCriteria criteria =
            BenchmarkScenarios.COMPLEX_AUTOMATED_FARM.getSuccessCriteria();

        assertFalse(criteria.isRequireExactBlockCount());
        assertEquals(0.80, criteria.getMinCorrectnessScore(), 0.001,
            "Complex scenarios should allow lower correctness");
    }

    // ========== ExpectedMetrics Tests ==========

    @Test
    @DisplayName("ExpectedMetrics should store all ranges")
    void testExpectedMetricsStoresRanges() {
        BenchmarkScenarios.ExpectedMetrics metrics =
            new BenchmarkScenarios.ExpectedMetrics(
                1000, 5000,    // planning latency
                10000, 30000,  // execution latency
                100, 500,      // input tokens
                50, 200,       // output tokens
                0.001, 0.01,   // cost
                5, 15,         // actions
                0.90           // success rate
            );

        assertEquals(1000, metrics.getMinPlanningLatencyMs());
        assertEquals(5000, metrics.getMaxPlanningLatencyMs());
        assertEquals(10000, metrics.getMinExecutionLatencyMs());
        assertEquals(30000, metrics.getMaxExecutionLatencyMs());
    }

    @Test
    @DisplayName("ExpectedMetrics validateMetrics should accept in-range values")
    void testValidateMetricsInRange() {
        BenchmarkScenarios.ExpectedMetrics metrics =
            BenchmarkScenarios.SIMPLE_MINE_10_STONE.getExpectedMetrics();

        boolean valid = metrics.validateMetrics(
            3000,   // planningLatencyMs (within 2000-5000)
            25000,  // executionLatencyMs (within 20000-40000)
            750,    // inputTokens (within 500-1000)
            200,    // outputTokens (within 100-300)
            0.005,  // costUsd (within 0.001-0.01)
            4       // actions (within 3-5)
        );

        assertTrue(valid, "Should accept values within ranges");
    }

    @Test
    @DisplayName("ExpectedMetrics validateMetrics should reject out-of-range values")
    void testValidateMetricsOutOfRange() {
        BenchmarkScenarios.ExpectedMetrics metrics =
            BenchmarkScenarios.SIMPLE_MINE_10_STONE.getExpectedMetrics();

        // Planning latency too high
        boolean invalidHigh = metrics.validateMetrics(
            10000,  // Too high (max is 5000)
            25000, 750, 200, 0.005, 4
        );
        assertFalse(invalidHigh, "Should reject planning latency above max");

        // Actions too low
        boolean invalidLow = metrics.validateMetrics(
            3000, 25000, 750, 200, 0.005, 2  // Too low (min is 3)
        );
        assertFalse(invalidLow, "Should reject actions below min");
    }

    @Test
    @DisplayName("ExpectedMetrics validateMetrics should check all ranges")
    void testValidateMetricsChecksAllRanges() {
        BenchmarkScenarios.ExpectedMetrics metrics =
            BenchmarkScenarios.MEDIUM_BUILD_5x5_HOUSE.getExpectedMetrics();

        // All just at boundaries should pass
        boolean valid = metrics.validateMetrics(
            3000,   // minPlanningLatencyMs
            120000, // minExecutionLatencyMs
            1000,   // minInputTokens
            200,    // minOutputTokens
            0.005,  // minCostUsd
            50      // minActions
        );

        assertTrue(valid, "Values at boundaries should be valid");
    }

    // ========== Complexity Tests ==========

    @Test
    @DisplayName("Complexity enum should have correct labels")
    void testComplexityLabels() {
        assertEquals("Simple", BenchmarkScenarios.Complexity.SIMPLE.getLabel());
        assertEquals("Moderate", BenchmarkScenarios.Complexity.MODERATE.getLabel());
        assertEquals("Complex", BenchmarkScenarios.Complexity.COMPLEX.getLabel());
        assertEquals("Complex + Multi-Agent",
            BenchmarkScenarios.Complexity.COMPLEX_MULTI_AGENT.getLabel());
    }

    @Test
    @DisplayName("Complexity enum should have descriptions")
    void testComplexityDescriptions() {
        assertTrue(BenchmarkScenarios.Complexity.SIMPLE.getDescription().contains("Single action"));
        assertTrue(BenchmarkScenarios.Complexity.MODERATE.getDescription().contains("planning"));
        assertTrue(BenchmarkScenarios.Complexity.COMPLEX.getDescription().contains("replanning"));
        assertTrue(BenchmarkScenarios.Complexity.COMPLEX_MULTI_AGENT.getDescription().contains("Coordination"));
    }

    // ========== getAllScenarios Tests ==========

    @Test
    @DisplayName("getAllScenarios should return all predefined scenarios")
    void testGetAllScenarios() {
        List<BenchmarkScenarios.BenchmarkScenario> scenarios =
            BenchmarkScenarios.getAllScenarios();

        assertEquals(4, scenarios.size(),
            "Should have 4 predefined scenarios");

        assertTrue(scenarios.contains(BenchmarkScenarios.SIMPLE_MINE_10_STONE));
        assertTrue(scenarios.contains(BenchmarkScenarios.MEDIUM_BUILD_5x5_HOUSE));
        assertTrue(scenarios.contains(BenchmarkScenarios.COMPLEX_AUTOMATED_FARM));
        assertTrue(scenarios.contains(BenchmarkScenarios.MULTI_AGENT_VILLAGE));
    }

    // ========== getScenario Tests ==========

    @Test
    @DisplayName("getScenario should find scenario by name")
    void testGetScenarioByName() {
        BenchmarkScenarios.BenchmarkScenario scenario =
            BenchmarkScenarios.getScenario("Simple: Mine 10 Stone");

        assertNotNull(scenario);
        assertEquals("Simple: Mine 10 Stone", scenario.getName());
    }

    @Test
    @DisplayName("getScenario should return null for unknown name")
    void testGetScenarioUnknown() {
        BenchmarkScenarios.BenchmarkScenario scenario =
            BenchmarkScenarios.getScenario("Unknown Scenario");

        assertNull(scenario);
    }

    @Test
    @DisplayName("getScenario should be case sensitive")
    void testGetScenarioCaseSensitive() {
        BenchmarkScenarios.BenchmarkScenario scenario =
            BenchmarkScenarios.getScenario("simple: mine 10 stone");

        assertNull(scenario, "Should be case sensitive");
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Scenario complexity should increase with difficulty")
    void testScenarioComplexityProgression() {
        BenchmarkScenarios.BenchmarkScenario simple =
            BenchmarkScenarios.SIMPLE_MINE_10_STONE;
        BenchmarkScenarios.BenchmarkScenario medium =
            BenchmarkScenarios.MEDIUM_BUILD_5x5_HOUSE;
        BenchmarkScenarios.BenchmarkScenario complex =
            BenchmarkScenarios.COMPLEX_AUTOMATED_FARM;

        // Max execution time should increase with complexity
        assertTrue(medium.getExpectedMetrics().getMaxExecutionLatencyMs() >
                   simple.getExpectedMetrics().getMaxExecutionLatencyMs(),
            "Medium scenario should take longer than simple");
        assertTrue(complex.getExpectedMetrics().getMaxExecutionLatencyMs() >
                   medium.getExpectedMetrics().getMaxExecutionLatencyMs(),
            "Complex scenario should take longer than medium");
    }

    @Test
    @DisplayName("Multi-agent scenario should require multiple agents")
    void testMultiAgentScenarioRequiresMultipleAgents() {
        BenchmarkScenarios.BenchmarkScenario scenario =
            BenchmarkScenarios.MULTI_AGENT_VILLAGE;

        assertEquals(3, scenario.getStartingConditions().getNumberOfAgents(),
            "Multi-agent scenario should specify 3 agents");
    }

    @Test
    @DisplayName("Expected success rate should decrease with complexity")
    void testSuccessRateDecreasesWithComplexity() {
        double simpleSuccessRate =
            BenchmarkScenarios.SIMPLE_MINE_10_STONE.getExpectedMetrics().getExpectedSuccessRate();
        double complexSuccessRate =
            BenchmarkScenarios.COMPLEX_AUTOMATED_FARM.getExpectedMetrics().getExpectedSuccessRate();

        assertTrue(simpleSuccessRate > complexSuccessRate,
            "Simple scenarios should have higher expected success rate");
    }

    @Test
    @DisplayName("Scenarios should have distinct names")
    void testScenarioNamesAreDistinct() {
        List<BenchmarkScenarios.BenchmarkScenario> scenarios =
            BenchmarkScenarios.getAllScenarios();

        long uniqueNames = scenarios.stream()
            .map(BenchmarkScenarios.BenchmarkScenario::getName)
            .distinct()
            .count();

        assertEquals(scenarios.size(), uniqueNames,
            "All scenarios should have unique names");
    }

    @Test
    @DisplayName("All scenarios should have valid configuration")
    void testAllScenariosValid() {
        List<BenchmarkScenarios.BenchmarkScenario> scenarios =
            BenchmarkScenarios.getAllScenarios();

        for (BenchmarkScenarios.BenchmarkScenario scenario : scenarios) {
            assertNotNull(scenario.getName());
            assertNotNull(scenario.getNaturalLanguageCommand());
            assertNotNull(scenario.getComplexity());
            assertNotNull(scenario.getStartingConditions());
            assertNotNull(scenario.getSuccessCriteria());
            assertNotNull(scenario.getExpectedMetrics());

            // Verify reasonable ranges
            assertTrue(scenario.getSuccessCriteria().getTargetBlockCount() > 0);
            assertTrue(scenario.getSuccessCriteria().getMaxExecutionTimeSeconds() > 0);
            assertTrue(scenario.getExpectedMetrics().getExpectedSuccessRate() > 0);
            assertTrue(scenario.getExpectedMetrics().getExpectedSuccessRate() <= 1.0);
        }
    }

    // ========== Edge Case Tests ==========

    @Test
    @DisplayName("ExpectedMetrics should handle minimum values")
    void testExpectedMetricsMinimumValues() {
        BenchmarkScenarios.ExpectedMetrics metrics =
            new BenchmarkScenarios.ExpectedMetrics(
                0, 1,           // planning
                0, 1,           // execution
                0, 1,           // input tokens
                0, 1,           // output tokens
                0.0, 0.001,     // cost
                1, 1,           // actions
                0.0             // success rate
            );

        boolean valid = metrics.validateMetrics(0, 0, 0, 0, 0.0, 1);
        assertTrue(valid, "Should handle minimum values");
    }

    @Test
    @DisplayName("ExpectedMetrics should validate at exact boundaries")
    void testExpectedMetricsExactBoundaries() {
        BenchmarkScenarios.ExpectedMetrics metrics =
            new BenchmarkScenarios.ExpectedMetrics(
                1000, 5000,
                10000, 50000,
                100, 1000,
                50, 500,
                0.001, 0.1,
                5, 50,
                0.75
            );

        // Test all boundaries
        assertTrue(metrics.validateMetrics(1000, 10000, 100, 50, 0.001, 5));
        assertTrue(metrics.validateMetrics(5000, 50000, 1000, 500, 0.1, 50));
        assertFalse(metrics.validateMetrics(999, 10000, 100, 50, 0.001, 5));
        assertFalse(metrics.validateMetrics(1000, 10000, 100, 50, 0.001, 51));
    }
}
