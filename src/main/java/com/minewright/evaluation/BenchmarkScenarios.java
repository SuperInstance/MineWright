package com.minewright.evaluation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Predefined benchmark scenarios for academic evaluation.
 *
 * <p>This class defines the standardized test scenarios used in the dissertation
 * evaluation. Each scenario includes starting conditions, success criteria, and
 * expected metrics ranges.</p>
 *
 * <p><b>Scenarios:</b></p>
 * <ul>
 *   <li>SIMPLE_MINE_10_STONE: Basic resource gathering</li>
 *   <li>MEDIUM_BUILD_5x5_HOUSE: Small structure construction</li>
 *   <li>COMPLEX_AUTOMATED_FARM: Multi-step automation project</li>
 *   <li>MULTI_AGENT_VILLAGE: Coordinated multi-agent building</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class BenchmarkScenarios {

    /**
     * Scenario: Mine 10 cobblestone blocks.
     *
     * <p>Complexity: SIMPLE</p>
     * <p>Expected actions: 3-5</p>
     * <p>Expected time: 20-40 seconds</p>
     */
    public static final BenchmarkScenario SIMPLE_MINE_10_STONE = new BenchmarkScenario(
        "Simple: Mine 10 Stone",
        "Mine 10 stone",
        Complexity.SIMPLE,
        new StartingConditions(
            new BlockPos(0, 65, 0),
            List.of(
                new ItemStack(Items.DIAMOND_PICKAXE)
            )
        ),
        new SuccessCriteria(
            10,                      // targetBlockCount
            60,                      // maxExecutionTimeSeconds
            0.95,                    // minCorrectnessScore
            true                     // requireExactBlockCount
        ),
        new ExpectedMetrics(
            2000, 5000,             // planningLatencyMs: min, max
            20000, 40000,           // executionLatencyMs: min, max
            500, 1000,              // inputTokens: min, max
            100, 300,               // outputTokens: min, max
            0.001, 0.01,            // costUsd: min, max
            3, 5,                   // actions: min, max
            0.95                    // expectedSuccessRate
        )
    );

    /**
     * Scenario: Build a 5x5x3 house with door.
     *
     * <p>Complexity: MODERATE</p>
     * <p>Expected actions: 50-100</p>
     * <p>Expected time: 120-300 seconds</p>
     */
    public static final BenchmarkScenario MEDIUM_BUILD_5x5_HOUSE = new BenchmarkScenario(
        "Medium: Build 5x5 House",
        "Build a 5x5 wooden house with 3 block high walls, a roof, and a door",
        Complexity.MODERATE,
        new StartingConditions(
            new BlockPos(0, 65, 0),
            List.of(
                new ItemStack(Items.OAK_PLANKS, 150),
                new ItemStack(Items.OAK_DOOR, 1),
                new ItemStack(Items.TORCH, 1)
            )
        ),
        new SuccessCriteria(
            111,                     // targetBlockCount (25 floor + 60 walls + 25 roof + 1 door)
            300,                     // maxExecutionTimeSeconds
            0.90,                    // minCorrectnessScore (allow 10% error)
            false                    // requireExactBlockCount (allow tolerance)
        ),
        new ExpectedMetrics(
            3000, 8000,             // planningLatencyMs: min, max
            120000, 300000,         // executionLatencyMs: min, max
            1000, 2000,             // inputTokens: min, max
            200, 500,               // outputTokens: min, max
            0.005, 0.02,            // costUsd: min, max
            50, 100,                // actions: min, max
            0.85                    // expectedSuccessRate
        )
    );

    /**
     * Scenario: Build an automated wheat farm.
     *
     * <p>Complexity: COMPLEX</p>
     * <p>Expected actions: 200-500</p>
     * <p>Expected time: 300-600 seconds</p>
     */
    public static final BenchmarkScenario COMPLEX_AUTOMATED_FARM = new BenchmarkScenario(
        "Complex: Automated Wheat Farm",
        "Build an automated wheat farm with water for hydration, farmland, seeds, " +
        "and a hopper collection system with a chest",
        Complexity.COMPLEX,
        new StartingConditions(
            new BlockPos(0, 65, 0),
            List.of(
                new ItemStack(Items.WATER_BUCKET, 1),
                new ItemStack(Items.IRON_HOE, 1),
                new ItemStack(Items.HOPPER, 4),
                new ItemStack(Items.CHEST, 1),
                new ItemStack(Items.WHEAT_SEEDS, 32),
                new ItemStack(Items.DIRT, 64),
                new ItemStack(Items.COBBLESTONE, 64)
            )
        ),
        new SuccessCriteria(
            100,                     // targetBlockCount (minimum threshold)
            600,                     // maxExecutionTimeSeconds
            0.80,                    // minCorrectnessScore
            false                    // requireExactBlockCount
        ),
        new ExpectedMetrics(
            5000, 15000,            // planningLatencyMs: min, max (may require multiple LLM calls)
            300000, 600000,         // executionLatencyMs: min, max
            2000, 5000,             // inputTokens: min, max
            500, 1500,              // outputTokens: min, max
            0.01, 0.05,             // costUsd: min, max
            200, 500,               // actions: min, max
            0.70                    // expectedSuccessRate
        )
    );

    /**
     * Scenario: Build a village with 3 coordinated agents.
     *
     * <p>Complexity: COMPLEX + MULTI_AGENT</p>
     * <p>Expected actions: 500-1000 (across all agents)</p>
     * <p>Expected time: 600-900 seconds</p>
     */
    public static final BenchmarkScenario MULTI_AGENT_VILLAGE = new BenchmarkScenario(
        "Multi-Agent: Village with 3 Workers",
        "Coordinate 3 workers to build a small village with 3 houses, " +
        "a central plaza, and paths connecting them",
        Complexity.COMPLEX_MULTI_AGENT,
        new StartingConditions(
            new BlockPos(0, 65, 0),
            List.of(
                new ItemStack(Items.OAK_PLANKS, 500),  // Per agent
                new ItemStack(Items.COBBLESTONE, 500),
                new ItemStack(Items.OAK_DOOR, 3),
                new ItemStack(Items.TORCH, 10)
            ),
            3                        // numberOfAgents
        ),
        new SuccessCriteria(
            300,                     // targetBlockCount (minimum)
            900,                     // maxExecutionTimeSeconds
            0.75,                    // minCorrectnessScore
            false                    // requireExactBlockCount
        ),
        new ExpectedMetrics(
            10000, 30000,           // planningLatencyMs: min, max (per agent)
            600000, 900000,         // executionLatencyMs: min, max (total)
            5000, 15000,            // inputTokens: min, max (total across agents)
            1000, 3000,             // outputTokens: min, max (total)
            0.03, 0.10,             // costUsd: min, max (total)
            500, 1000,              // actions: min, max (total)
            0.80                    // expectedSuccessRate
        )
    );

    /**
     * Complexity levels for benchmark scenarios.
     */
    public enum Complexity {
        SIMPLE("Simple", "Single action type, clear goal, <10 steps"),
        MODERATE("Moderate", "Multiple action types, requires planning, 10-100 steps"),
        COMPLEX("Complex", "Multiple phases, replanning likely, 100-500 steps"),
        COMPLEX_MULTI_AGENT("Complex + Multi-Agent", "Coordination required, 500+ steps");

        private final String label;
        private final String description;

        Complexity(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Configuration for a benchmark scenario.
     */
    public static class BenchmarkScenario {
        private final String name;
        private final String naturalLanguageCommand;
        private final Complexity complexity;
        private final StartingConditions startingConditions;
        private final SuccessCriteria successCriteria;
        private final ExpectedMetrics expectedMetrics;

        public BenchmarkScenario(String name,
                                String naturalLanguageCommand,
                                Complexity complexity,
                                StartingConditions startingConditions,
                                SuccessCriteria successCriteria,
                                ExpectedMetrics expectedMetrics) {
            this.name = name;
            this.naturalLanguageCommand = naturalLanguageCommand;
            this.complexity = complexity;
            this.startingConditions = startingConditions;
            this.successCriteria = successCriteria;
            this.expectedMetrics = expectedMetrics;
        }

        public String getName() {
            return name;
        }

        public String getNaturalLanguageCommand() {
            return naturalLanguageCommand;
        }

        public Complexity getComplexity() {
            return complexity;
        }

        public StartingConditions getStartingConditions() {
            return startingConditions;
        }

        public SuccessCriteria getSuccessCriteria() {
            return successCriteria;
        }

        public ExpectedMetrics getExpectedMetrics() {
            return expectedMetrics;
        }
    }

    /**
     * Starting conditions for a benchmark trial.
     */
    public static class StartingConditions {
        private final BlockPos spawnPosition;
        private final List<ItemStack> inventoryItems;
        private final int numberOfAgents;

        public StartingConditions(BlockPos spawnPosition, List<ItemStack> inventoryItems) {
            this(spawnPosition, inventoryItems, 1);
        }

        public StartingConditions(BlockPos spawnPosition,
                                  List<ItemStack> inventoryItems,
                                  int numberOfAgents) {
            this.spawnPosition = spawnPosition;
            this.inventoryItems = inventoryItems;
            this.numberOfAgents = numberOfAgents;
        }

        public BlockPos getSpawnPosition() {
            return spawnPosition;
        }

        public List<ItemStack> getInventoryItems() {
            return inventoryItems;
        }

        public int getNumberOfAgents() {
            return numberOfAgents;
        }
    }

    /**
     * Success criteria for evaluating benchmark completion.
     */
    public static class SuccessCriteria {
        private final int targetBlockCount;
        private final int maxExecutionTimeSeconds;
        private final double minCorrectnessScore;
        private final boolean requireExactBlockCount;

        public SuccessCriteria(int targetBlockCount,
                              int maxExecutionTimeSeconds,
                              double minCorrectnessScore,
                              boolean requireExactBlockCount) {
            this.targetBlockCount = targetBlockCount;
            this.maxExecutionTimeSeconds = maxExecutionTimeSeconds;
            this.minCorrectnessScore = minCorrectnessScore;
            this.requireExactBlockCount = requireExactBlockCount;
        }

        public int getTargetBlockCount() {
            return targetBlockCount;
        }

        public int getMaxExecutionTimeSeconds() {
            return maxExecutionTimeSeconds;
        }

        public double getMinCorrectnessScore() {
            return minCorrectnessScore;
        }

        public boolean isRequireExactBlockCount() {
            return requireExactBlockCount;
        }
    }

    /**
     * Expected metrics ranges for validation.
     */
    public static class ExpectedMetrics {
        private final long minPlanningLatencyMs;
        private final long maxPlanningLatencyMs;
        private final long minExecutionLatencyMs;
        private final long maxExecutionLatencyMs;
        private final int minInputTokens;
        private final int maxInputTokens;
        private final int minOutputTokens;
        private final int maxOutputTokens;
        private final double minCostUsd;
        private final double maxCostUsd;
        private final int minActions;
        private final int maxActions;
        private final double expectedSuccessRate;

        public ExpectedMetrics(long minPlanningLatencyMs, long maxPlanningLatencyMs,
                              long minExecutionLatencyMs, long maxExecutionLatencyMs,
                              int minInputTokens, int maxInputTokens,
                              int minOutputTokens, int maxOutputTokens,
                              double minCostUsd, double maxCostUsd,
                              int minActions, int maxActions,
                              double expectedSuccessRate) {
            this.minPlanningLatencyMs = minPlanningLatencyMs;
            this.maxPlanningLatencyMs = maxPlanningLatencyMs;
            this.minExecutionLatencyMs = minExecutionLatencyMs;
            this.maxExecutionLatencyMs = maxExecutionLatencyMs;
            this.minInputTokens = minInputTokens;
            this.maxInputTokens = maxInputTokens;
            this.minOutputTokens = minOutputTokens;
            this.maxOutputTokens = maxOutputTokens;
            this.minCostUsd = minCostUsd;
            this.maxCostUsd = maxCostUsd;
            this.minActions = minActions;
            this.maxActions = maxActions;
            this.expectedSuccessRate = expectedSuccessRate;
        }

        /**
         * Check if actual metrics are within expected ranges.
         */
        public boolean validateMetrics(long planningLatencyMs,
                                      long executionLatencyMs,
                                      int inputTokens,
                                      int outputTokens,
                                      double costUsd,
                                      int actions) {
            return planningLatencyMs >= minPlanningLatencyMs &&
                   planningLatencyMs <= maxPlanningLatencyMs &&
                   executionLatencyMs >= minExecutionLatencyMs &&
                   executionLatencyMs <= maxExecutionLatencyMs &&
                   inputTokens >= minInputTokens &&
                   inputTokens <= maxInputTokens &&
                   outputTokens >= minOutputTokens &&
                   outputTokens <= maxOutputTokens &&
                   costUsd >= minCostUsd &&
                   costUsd <= maxCostUsd &&
                   actions >= minActions &&
                   actions <= maxActions;
        }

        public long getMinPlanningLatencyMs() {
            return minPlanningLatencyMs;
        }

        public long getMaxPlanningLatencyMs() {
            return maxPlanningLatencyMs;
        }

        public long getMinExecutionLatencyMs() {
            return minExecutionLatencyMs;
        }

        public long getMaxExecutionLatencyMs() {
            return maxExecutionLatencyMs;
        }

        public double getExpectedSuccessRate() {
            return expectedSuccessRate;
        }
    }

    /**
     * Get all benchmark scenarios.
     */
    public static List<BenchmarkScenario> getAllScenarios() {
        List<BenchmarkScenario> scenarios = new ArrayList<>();
        scenarios.add(SIMPLE_MINE_10_STONE);
        scenarios.add(MEDIUM_BUILD_5x5_HOUSE);
        scenarios.add(COMPLEX_AUTOMATED_FARM);
        scenarios.add(MULTI_AGENT_VILLAGE);
        return scenarios;
    }

    /**
     * Get scenario by name.
     */
    public static BenchmarkScenario getScenario(String name) {
        return getAllScenarios().stream()
            .filter(s -> s.getName().equals(name))
            .findFirst()
            .orElse(null);
    }
}
