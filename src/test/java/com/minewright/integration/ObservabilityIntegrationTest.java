package com.minewright.integration;

import com.minewright.evaluation.EvaluationMetrics;
import com.minewright.entity.ForemanEntity;
import com.minewright.execution.AgentState;
import com.minewright.llm.PromptMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for observability features including metrics collection,
 * tracing, and monitoring.
 *
 * <p><b>Test Coverage:</b></p>
 * <ul>
 *   <li>Metrics collection and aggregation</li>
 *   <li>Task execution tracking</li>
 *   <li>LLM call monitoring</li>
 *   <li>Performance metrics</li>
 *   <li>Cost tracking</li>
 *   <li>Metrics export to JSON</li>
 *   <li>Concurrent metrics recording</li>
 *   <li>Benchmark run tracking</li>
 * </ul>
 *
 * @see EvaluationMetrics
 * @see PromptMetrics
 * @since 1.0.0
 */
@DisplayName("Observability Integration Tests")
class ObservabilityIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Metrics are recorded for task execution")
    void testTaskExecutionMetrics() {
        String agentName = "Steve-1";
        String command = "mine 50 stone";

        // Record task lifecycle
        EvaluationMetrics.recordTaskStart(agentName, command);
        EvaluationMetrics.recordPlanningStart(agentName, System.currentTimeMillis());
        EvaluationMetrics.recordPlanningComplete(agentName, 1500, 3);
        EvaluationMetrics.recordExecutionStart(agentName);
        EvaluationMetrics.recordTaskComplete(agentName, true, 1.0, Map.of("blocks_mined", 50));

        // Verify metrics were recorded
        Map<String, Object> summary = EvaluationMetrics.getSummary();
        assertTrue((Integer) summary.get("totalTasks") > 0, "Should record tasks");
        assertTrue((Integer) summary.get("successfulTasks") > 0, "Should record successful tasks");
    }

    @Test
    @DisplayName("LLM call metrics are tracked")
    void testLLMMetrics() {
        // Record LLM calls
        EvaluationMetrics.recordLLMCall(
            "openai",
            "gpt-4",
            1250,
            180,
            0.045,
            2300,
            false
        );

        EvaluationMetrics.recordLLMCall(
            "groq",
            "llama3-70b",
            890,
            120,
            0.0,
            1500,
            false
        );

        Map<String, Object> summary = EvaluationMetrics.getSummary();

        assertTrue((Integer) summary.get("totalLLMCalls") >= 2,
            "Should track LLM calls");

        assertTrue((Double) summary.get("totalCostUSD") > 0,
            "Should track cost");
    }

    @Test
    @DisplayName("Planning latency is measured")
    void testPlanningLatency() {
        String agentName = "Steve-2";

        EvaluationMetrics.recordTaskStart(agentName, "test command");
        EvaluationMetrics.recordPlanningStart(agentName, System.currentTimeMillis());

        // Simulate planning work
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long latency = System.currentTimeMillis() - EvaluationMetrics.getTaskStart(agentName);
        EvaluationMetrics.recordPlanningComplete(agentName, latency, 1);

        Map<String, Object> summary = EvaluationMetrics.getSummary();
        assertTrue((Integer) summary.get("totalTasks") > 0,
            "Should record task");
    }

    @Test
    @DisplayName("Execution time is tracked")
    void testExecutionTime() {
        String agentName = "Steve-3";

        EvaluationMetrics.recordTaskStart(agentName, "build house");
        EvaluationMetrics.recordPlanningStart(agentName, System.currentTimeMillis());
        EvaluationMetrics.recordPlanningComplete(agentName, 500, 5);
        EvaluationMetrics.recordExecutionStart(agentName);

        // Simulate execution
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        EvaluationMetrics.recordTaskComplete(agentName, true, 1.0,
            Map.of("structures_built", 1));

        Map<String, Object> summary = EvaluationMetrics.getSummary();
        assertTrue((Integer) summary.get("totalTasks") > 0,
            "Should record execution");
    }

    @Test
    @DisplayName("Failed tasks are tracked separately")
    void testFailedTaskTracking() {
        String agentName = "Steve-4";

        EvaluationMetrics.recordTaskStart(agentName, "impossible task");
        EvaluationMetrics.recordPlanningStart(agentName, System.currentTimeMillis());
        EvaluationMetrics.recordPlanningComplete(agentName, 300, 1);
        EvaluationMetrics.recordExecutionStart(agentName);
        EvaluationMetrics.recordTaskComplete(agentName, false, 0.0,
            Map.of("error", "Task impossible"));

        Map<String, Object> summary = EvaluationMetrics.getSummary();
        assertTrue((Integer) summary.get("failedTasks") > 0,
            "Should track failed tasks");

        double successRate = (Double) summary.get("successRate");
        assertTrue(successRate < 1.0, "Success rate should be < 100%");
    }

    @Test
    @DisplayName("Metrics can be exported to JSON")
    void testMetricsExport() throws Exception {
        // Record some metrics
        EvaluationMetrics.recordTaskStart("TestAgent", "test");
        EvaluationMetrics.recordPlanningStart("TestAgent", System.currentTimeMillis());
        EvaluationMetrics.recordPlanningComplete("TestAgent", 100, 1);
        EvaluationMetrics.recordExecutionStart("TestAgent");
        EvaluationMetrics.recordTaskComplete("TestAgent", true, 1.0, Map.of());

        // Export to temp file
        Path tempFile = Files.createTempFile("metrics", ".json");
        String filePath = tempFile.toString();

        EvaluationMetrics.exportToJson(filePath);

        // Verify file exists and is valid JSON
        assertTrue(Files.exists(tempFile), "Export file should exist");

        String content = Files.readString(tempFile);
        assertTrue(content.contains("\"benchmarkRunId\""), "Should contain benchmark ID");
        assertTrue(content.contains("\"tasks\""), "Should contain tasks");

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    @Test
    @DisplayName("Concurrent metrics recording is thread-safe")
    void testConcurrentMetricsRecording() throws InterruptedException {
        int numThreads = 10;
        int operationsPerThread = 50;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String agentName = "Agent-" + threadId + "-" + j;

                        EvaluationMetrics.recordTaskStart(agentName, "test command");
                        EvaluationMetrics.recordPlanningStart(agentName, System.currentTimeMillis());
                        EvaluationMetrics.recordPlanningComplete(agentName, 100, 1);
                        EvaluationMetrics.recordExecutionStart(agentName);
                        EvaluationMetrics.recordTaskComplete(agentName,
                            j % 5 != 0, // Mix of success/failure
                            1.0,
                            Map.of("iteration", j)
                        );

                        // Also record some LLM calls
                        EvaluationMetrics.recordLLMCall(
                            "test-provider",
                            "test-model",
                            100 + j,
                            50,
                            0.001 * j,
                            200 + j,
                            j % 10 == 0
                        );
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Map<String, Object> summary = EvaluationMetrics.getSummary();

        assertTrue((Integer) summary.get("totalTasks") >= numThreads * operationsPerThread,
            "Should record all tasks");
        assertTrue((Integer) summary.get("totalLLMCalls") >= numThreads * operationsPerThread,
            "Should record all LLM calls");
    }

    @Test
    @DisplayName("Token usage is tracked accurately")
    void testTokenTracking() {
        // Record calls with different token counts
        EvaluationMetrics.recordLLMCall("provider1", "model1", 1000, 200, 0.01, 1200, false);
        EvaluationMetrics.recordLLMCall("provider2", "model2", 1500, 300, 0.02, 1800, false);
        EvaluationMetrics.recordLLMCall("provider3", "model3", 800, 150, 0.008, 950, false);

        Map<String, Object> summary = EvaluationMetrics.getSummary();

        assertTrue((Integer) summary.get("totalPromptTokens") > 0,
            "Should track prompt tokens");
        assertTrue((Integer) summary.get("totalCompletionTokens") > 0,
            "Should track completion tokens");
        assertTrue((Integer) summary.get("totalTokens") > 0,
            "Should track total tokens");
    }

    @Test
    @DisplayName("Cost tracking is accurate")
    void testCostTracking() {
        // Record calls with known costs
        EvaluationMetrics.recordLLMCall("provider1", "model1", 1000, 200, 0.015, 1200, false);
        EvaluationMetrics.recordLLMCall("provider2", "model2", 2000, 400, 0.030, 2400, false);

        Map<String, Object> summary = EvaluationMetrics.getSummary();

        double totalCost = (Double) summary.get("totalCostUSD");
        assertTrue(totalCost > 0.045, "Should accumulate costs");
    }

    @Test
    @DisplayName("Cache hits are tracked")
    void testCacheHitTracking() {
        // Record cached responses
        EvaluationMetrics.recordCacheHit("provider1", "model1", 1000);
        EvaluationMetrics.recordCacheHit("provider2", "model2", 1500);
        EvaluationMetrics.recordCacheHit("provider1", "model1", 1200);

        Map<String, Object> summary = EvaluationMetrics.getSummary();

        assertTrue((Integer) summary.get("cacheHits") >= 3,
            "Should track cache hits");

        long tokensSaved = (Long) summary.get("tokensSavedByCache");
        assertTrue(tokensSaved > 0, "Should calculate tokens saved");
    }

    @Test
    @DisplayName("Skill usage metrics are tracked")
    void testSkillUsageMetrics() {
        EvaluationMetrics.recordSkillUsage("mineBlock", 10);
        EvaluationMetrics.recordSkillUsage("craftItem", 5);
        EvaluationMetrics.recordSkillUsage("mineBlock", 15);
        EvaluationMetrics.recordSkillUsage("buildStructure", 3);

        Map<String, Object> summary = EvaluationMetrics.getSummary();

        assertTrue((Integer) summary.get("totalSkillExecutions") >= 4,
            "Should track skill executions");

        @SuppressWarnings("unchecked")
        Map<String, Integer> topSkills = (Map<String, Integer>) summary.get("topSkills");
        assertNotNull(topSkills, "Should have top skills");
        assertTrue(topSkills.containsKey("mineBlock"), "Should track mineBlock usage");
    }

    @Test
    @DisplayName("Metrics can be reset")
    void testMetricsReset() {
        // Record some metrics
        EvaluationMetrics.recordTaskStart("Agent1", "test");
        EvaluationMetrics.recordPlanningStart("Agent1", System.currentTimeMillis());
        EvaluationMetrics.recordPlanningComplete("Agent1", 100, 1);
        EvaluationMetrics.recordExecutionStart("Agent1");
        EvaluationMetrics.recordTaskComplete("Agent1", true, 1.0, Map.of());

        Map<String, Object> beforeReset = EvaluationMetrics.getSummary();
        assertTrue((Integer) beforeReset.get("totalTasks") > 0,
            "Should have tasks before reset");

        // Reset
        EvaluationMetrics.reset();

        Map<String, Object> afterReset = EvaluationMetrics.getSummary();
        assertEquals(0, afterReset.get("totalTasks"),
            "Should have no tasks after reset");
    }

    @Test
    @DisplayName("Benchmark metadata is tracked")
    void testBenchmarkMetadata() {
        Map<String, Object> summary = EvaluationMetrics.getSummary();

        assertTrue(summary.containsKey("benchmarkRunId"),
            "Should have benchmark run ID");
        assertTrue(summary.containsKey("benchmarkVersion"),
            "Should have benchmark version");
        assertTrue(summary.containsKey("benchmarkStartTime"),
            "Should have benchmark start time");
    }

    @Test
    @DisplayName("Performance percentiles are calculated")
    void testPerformancePercentiles() {
        // Record tasks with varying execution times
        for (int i = 0; i < 20; i++) {
            String agentName = "Agent-" + i;
            EvaluationMetrics.recordTaskStart(agentName, "test");
            EvaluationMetrics.recordPlanningStart(agentName, System.currentTimeMillis());
            EvaluationMetrics.recordPlanningComplete(agentName, 100 + i * 10, 1);
            EvaluationMetrics.recordExecutionStart(agentName);
            EvaluationMetrics.recordTaskComplete(agentName, true, 1.0, Map.of());
        }

        Map<String, Object> summary = EvaluationMetrics.getSummary();

        assertTrue(summary.containsKey("p50Latency"),
            "Should calculate p50 latency");
        assertTrue(summary.containsKey("p95Latency"),
            "Should calculate p95 latency");
        assertTrue(summary.containsKey("p99Latency"),
            "Should calculate p99 latency");
    }

    @Test
    @DisplayName("Agent-specific metrics can be retrieved")
    void testAgentSpecificMetrics() {
        String agent1 = "Agent-A";
        String agent2 = "Agent-B";

        // Agent A completes tasks
        EvaluationMetrics.recordTaskStart(agent1, "task1");
        EvaluationMetrics.recordPlanningStart(agent1, System.currentTimeMillis());
        EvaluationMetrics.recordPlanningComplete(agent1, 200, 2);
        EvaluationMetrics.recordExecutionStart(agent1);
        EvaluationMetrics.recordTaskComplete(agent1, true, 1.0, Map.of());

        // Agent B fails tasks
        EvaluationMetrics.recordTaskStart(agent2, "task1");
        EvaluationMetrics.recordPlanningStart(agent2, System.currentTimeMillis());
        EvaluationMetrics.recordPlanningComplete(agent2, 150, 1);
        EvaluationMetrics.recordExecutionStart(agent2);
        EvaluationMetrics.recordTaskComplete(agent2, false, 0.0, Map.of("error", "failed"));

        // Get summary
        Map<String, Object> summary = EvaluationMetrics.getSummary();
        assertTrue((Integer) summary.get("totalTasks") >= 2,
            "Should track both agents");
    }

    @Test
    @DisplayName("Prompt metrics are tracked")
    void testPromptMetrics() {
        PromptMetrics metrics = new PromptMetrics();

        metrics.recordPrompt("test-prompt-1", 1000, 200);
        metrics.recordPrompt("test-prompt-2", 1500, 300);

        Map<String, Object> summary = metrics.getSummary();

        assertTrue((Integer) summary.get("totalPrompts") >= 2,
            "Should track total prompts");
        assertTrue((Integer) summary.get("totalTokens") > 0,
            "Should track total tokens");
    }

    @Test
    @DisplayName("Metrics export includes all required fields")
    void testMetricsExportCompleteness() throws Exception {
        // Record comprehensive metrics
        EvaluationMetrics.recordTaskStart("TestAgent", "test");
        EvaluationMetrics.recordPlanningStart("TestAgent", System.currentTimeMillis());
        EvaluationMetrics.recordPlanningComplete("TestAgent", 100, 1);
        EvaluationMetrics.recordExecutionStart("TestAgent");
        EvaluationMetrics.recordTaskComplete("TestAgent", true, 1.0, Map.of("result", "success"));
        EvaluationMetrics.recordLLMCall("provider", "model", 1000, 200, 0.01, 1200, false);
        EvaluationMetrics.recordCacheHit("provider", "model", 1000);
        EvaluationMetrics.recordSkillUsage("testSkill", 5);

        // Export
        Path tempFile = Files.createTempFile("complete-metrics", ".json");
        EvaluationMetrics.exportToJson(tempFile.toString());

        String content = Files.readString(tempFile);

        // Verify all major sections are present
        assertTrue(content.contains("\"benchmarkRunId\""), "Should have run ID");
        assertTrue(content.contains("\"tasks\""), "Should have tasks");
        assertTrue(content.contains("\"llmCalls\""), "Should have LLM calls");
        assertTrue(content.contains("\"summary\""), "Should have summary");
        assertTrue(content.contains("\"metadata\""), "Should have metadata");

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    @Test
    @DisplayName("Multiple benchmark runs can be differentiated")
    void testMultipleBenchmarkRuns() {
        // First run
        String runId1 = EvaluationMetrics.getCurrentBenchmarkRunId();
        EvaluationMetrics.recordTaskStart("Agent1", "test1");
        EvaluationMetrics.recordTaskComplete("Agent1", true, 1.0, Map.of());

        // Reset for second run
        EvaluationMetrics.reset();

        // Second run
        String runId2 = EvaluationMetrics.getCurrentBenchmarkRunId();
        EvaluationMetrics.recordTaskStart("Agent2", "test2");
        EvaluationMetrics.recordTaskComplete("Agent2", true, 1.0, Map.of());

        // Run IDs should be different
        assertNotEquals(runId1, runId2, "Benchmark run IDs should be unique");
    }

    @Test
    @DisplayName("Error metrics are tracked")
    void testErrorMetrics() {
        // Record errors
        EvaluationMetrics.recordError("LLM_TIMEOUT", "LLM request timed out");
        EvaluationMetrics.recordError("PATHFINDING_FAILED", "Could not find path");
        EvaluationMetrics.recordError("LLM_TIMEOUT", "Another timeout");

        Map<String, Object> summary = EvaluationMetrics.getSummary();

        @SuppressWarnings("unchecked")
        Map<String, Integer> errorCounts = (Map<String, Integer>) summary.get("errorCounts");

        assertNotNull(errorCounts, "Should track error counts");
        assertTrue(errorCounts.containsKey("LLM_TIMEOUT"), "Should track LLM_TIMEOUT");
        assertEquals(2, errorCounts.get("LLM_TIMEOUT"), "Should count LLM_TIMEOUT occurrences");
    }

    @Test
    @DisplayName("Metrics are collected during integration scenario")
    void testMetricsDuringScenario() {
        ForemanEntity foreman = createForeman("Steve");

        TestScenarioBuilder scenario = createScenario("Metrics Collection Test")
            .withEntity("foreman", foreman)
            .withCommand("mine 10 stone")
            .expectSuccess(true);

        // Record metrics during scenario
        EvaluationMetrics.recordTaskStart("Steve", "mine 10 stone");
        EvaluationMetrics.recordPlanningStart("Steve", System.currentTimeMillis());
        EvaluationMetrics.recordPlanningComplete("Steve", 500, 1);
        EvaluationMetrics.recordExecutionStart("Steve");

        TestScenarioBuilder.TestResult result = executeScenario(scenario);

        EvaluationMetrics.recordTaskComplete("Steve", result.isSuccess(),
            1.0, Map.of("blocks_mined", 10));

        assertSuccess(result);

        Map<String, Object> summary = EvaluationMetrics.getSummary();
        assertTrue((Integer) summary.get("totalTasks") > 0,
            "Should collect metrics during scenario");
    }
}
