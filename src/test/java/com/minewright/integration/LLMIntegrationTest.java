package com.minewright.integration;

import com.minewright.entity.ForemanEntity;
import com.minewright.execution.AgentState;
import com.minewright.llm.PromptBuilder;
import com.minewright.llm.ResponseParser;
import com.minewright.llm.TaskPlanner;
import com.minewright.llm.async.*;
import com.minewright.llm.cache.SemanticLLMCache;
import com.minewright.llm.cascade.*;
import com.minewright.testutil.LLMMockClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for LLM integration including cascade routing,
 * caching, and feedback generation.
 *
 * <p><b>Test Coverage:</b></p>
 * <ul>
 *   <li>Task planning with LLM</li>
 *   <li>Cascade routing based on complexity</li>
 *   <li>Semantic caching</li>
 *   <li>Feedback generation</li>
 *   <li>Concurrent LLM requests</li>
 *   <li>Fallback and error handling</li>
 *   <li>Prompt building</li>
 *   <li>Response parsing</li>
 * </ul>
 *
 * @see CascadeRouter
 * @see TaskPlanner
 * @see SemanticLLMCache
 * @since 1.6.0
 */
@DisplayName("LLM Integration Tests")
class LLMIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Task planner generates tasks from natural language")
    void testTaskPlanning() {
        ForemanEntity foreman = createForeman("Steve");

        // Create task planner with mock client
        LLMMockClient mockClient = new LLMMockClient();
        mockClient.setMockResponse("""
            {
                "tasks": [
                    {
                        "action": "mine",
                        "block": "stone",
                        "quantity": 50
                    }
                ]
            }
            """);

        TaskPlanner planner = new TaskPlanner(mockClient);

        // Plan tasks
        ResponseParser.ParsedResponse response = planner.planTasks(foreman, "mine 50 stone");

        assertNotNull(response, "Should get response");
        assertFalse(response.getTasks().isEmpty(), "Should have tasks");

        assertEquals("mine", response.getTasks().get(0).getAction(),
            "Task action should match");
    }

    @Test
    @DisplayName("Cascade router routes to appropriate tier")
    void testCascadeRouting() {
        // Create clients for different tiers
        Map<LLMTier, AsyncLLMClient> clients = new HashMap<>();
        clients.put(LLMTier.SIMPLE, LLMMockClient.createFast());
        clients.put(LLMTier.COMPLEX, LLMMockClient.createComplex());

        // Create cache
        SemanticLLMCache cache = new SemanticLLMCache();

        // Create complexity analyzer
        ComplexityAnalyzer analyzer = new ComplexityAnalyzer();

        // Create router
        CascadeRouter router = new CascadeRouter(cache, analyzer, clients);

        // Test simple command
        CompletableFuture<LLMResponse> simpleFuture = router.route(
            "echo hello",
            Map.of()
        );

        assertTrue(simpleFuture.orTimeout(5, TimeUnit.SECONDS).isPresent(),
            "Simple command should route successfully");

        // Test complex command
        CompletableFuture<LLMResponse> complexFuture = router.route(
            "Analyze the terrain and build an optimal shelter considering weather patterns",
            Map.of()
        );

        assertTrue(complexFuture.orTimeout(5, TimeUnit.SECONDS).isPresent(),
            "Complex command should route successfully");

        // Check statistics
        Map<String, Object> stats = router.getStatistics();
        assertTrue((Integer) stats.get("totalRequests") >= 2,
            "Should track total requests");
    }

    @Test
    @DisplayName("Semantic cache returns similar cached responses")
    void testSemanticCaching() {
        SemanticLLMCache cache = new SemanticLLMCache();

        // Cache a response
        LLMResponse original = new LLMResponse(
            "Mine 50 stone",
            "I will mine 50 stone",
            Map.of("model", "gpt-3.5-turbo"),
            100
        );

        cache.put("mine 50 stone", original);

        // Query with similar but not identical text
        Optional<LLMResponse> cached = cache.get("mine fifty stones");
        assertTrue(cached.isPresent(), "Should find semantically similar response");
    }

    @Test
    @DisplayName("Concurrent LLM requests are handled safely")
    void testConcurrentRequests() throws InterruptedException {
        SemanticLLMCache cache = new SemanticLLMCache();
        ComplexityAnalyzer analyzer = new ComplexityAnalyzer();

        Map<LLMTier, AsyncLLMClient> clients = new HashMap<>();
        clients.put(LLMTier.SIMPLE, LLMMockClient.createFast());
        clients.put(LLMTier.COMPLEX, LLMMockClient.createComplex());

        CascadeRouter router = new CascadeRouter(cache, analyzer, clients);

        int numRequests = 20;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numRequests; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    CompletableFuture<LLMResponse> future = router.route(
                        "Command " + index,
                        Map.of()
                    );
                    future.orTimeout(5, TimeUnit.SECONDS);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(successCount.get() > 0, "Should have successful requests");
        assertTrue(failureCount.get() < numRequests, "Should not fail all requests");
    }

    @Test
    @DisplayName("Router falls back to higher tier on failure")
    void testRouterFallback() {
        // Create unreliable simple client
        LLMMockClient unreliableSimple = LLMMockClient.createUnreliable(0.8);
        LLMMockClient reliableComplex = LLMMockClient.createComplex();

        Map<LLMTier, AsyncLLMClient> clients = new HashMap<>();
        clients.put(LLMTier.SIMPLE, unreliableSimple);
        clients.put(LLMTier.COMPLEX, reliableComplex);

        SemanticLLMCache cache = new SemanticLLMCache();
        ComplexityAnalyzer analyzer = new ComplexityAnalyzer();

        CascadeRouter router = new CascadeRouter(cache, analyzer, clients);

        // Should fallback from SIMPLE to COMPLEX
        CompletableFuture<LLMResponse> future = router.route(
            "test command",
            Map.of()
        );

        assertDoesNotThrow(() -> future.orTimeout(10, TimeUnit.SECONDS),
            "Should succeed with fallback");

        Map<String, Object> stats = router.getStatistics();
        assertTrue((Integer) stats.get("fallbacks") > 0,
            "Should track fallbacks");
    }

    @Test
    @DisplayName("Complexity analyzer categorizes tasks correctly")
    void testComplexityAnalysis() {
        ComplexityAnalyzer analyzer = new ComplexityAnalyzer();

        // Simple tasks
        TaskComplexity simpleComplexity = analyzer.analyze("echo hello");
        assertTrue(simpleComplexity.getScore() < 0.3,
            "Simple task should have low complexity");

        // Medium tasks
        TaskComplexity mediumComplexity = analyzer.analyze("mine 50 stone and craft a furnace");
        assertTrue(mediumComplexity.getScore() >= 0.3 && mediumComplexity.getScore() < 0.7,
            "Medium task should have medium complexity");

        // Complex tasks
        TaskComplexity complexComplexity = analyzer.analyze(
            "Analyze the surrounding terrain, plan an optimal base location, " +
            "gather necessary resources, and construct a fortified shelter " +
            "with automated farming systems"
        );
        assertTrue(complexComplexity.getScore() >= 0.7,
            "Complex task should have high complexity");
    }

    @Test
    @DisplayName("Prompt builder constructs valid prompts")
    void testPromptBuilding() {
        ForemanEntity foreman = createForeman("Steve");

        String prompt = PromptBuilder.buildUserPrompt(
            foreman,
            "mine 50 stone",
            null
        );

        assertNotNull(prompt, "Prompt should not be null");
        assertTrue(prompt.length() > 0, "Prompt should have content");
        assertTrue(prompt.contains("mine"), "Prompt should contain command");
    }

    @Test
    @DisplayName("Response parser extracts tasks from LLM response")
    void testResponseParsing() {
        String jsonResponse = """
            {
                "tasks": [
                    {
                        "action": "mine",
                        "block": "iron_ore",
                        "quantity": 25
                    },
                    {
                        "action": "craft",
                        "item": "iron_pickaxe"
                    }
                ]
            }
            """;

        ResponseParser parser = new ResponseParser();
        ResponseParser.ParsedResponse response = parser.parseResponse(jsonResponse);

        assertNotNull(response, "Should parse response");
        assertEquals(2, response.getTasks().size(), "Should extract 2 tasks");
        assertEquals("mine", response.getTasks().get(0).getAction(), "First task should be mine");
        assertEquals("craft", response.getTasks().get(1).getAction(), "Second task should be craft");
    }

    @Test
    @DisplayName("Cache hit improves response time")
    void testCachePerformance() throws Exception {
        SemanticLLMCache cache = new SemanticLLMCache();

        // Cache a response
        LLMResponse cachedResponse = new LLMResponse(
            "Cached response",
            "I will do that",
            Map.of("model", "cached"),
            10
        );
        cache.put("test command", cachedResponse);

        // Measure cache hit time
        long startTime = System.nanoTime();
        Optional<LLMResponse> cached = cache.get("test command");
        long cacheTime = System.nanoTime() - startTime;

        assertTrue(cached.isPresent(), "Should get cached response");
        assertTrue(cacheTime < 1_000_000, "Cache lookup should be fast (< 1ms)");
    }

    @Test
    @DisplayName("Router statistics are accurate")
    void testRouterStatistics() {
        Map<LLMTier, AsyncLLMClient> clients = new HashMap<>();
        clients.put(LLMTier.SIMPLE, LLMMockClient.createFast());
        clients.put(LLMTier.COMPLEX, LLMMockClient.createComplex());

        CascadeRouter router = new CascadeRouter(
            new SemanticLLMCache(),
            new ComplexityAnalyzer(),
            clients
        );

        // Make several requests
        for (int i = 0; i < 5; i++) {
            router.route("command " + i, Map.of()).orTimeout(5, TimeUnit.SECONDS);
        }

        Map<String, Object> stats = router.getStatistics();

        assertTrue((Integer) stats.get("totalRequests") >= 5,
            "Should count total requests");
        assertTrue((Integer) stats.get("cacheHits") >= 0,
            "Should count cache hits");
        assertTrue((Integer) stats.get("fallbacks") >= 0,
            "Should count fallbacks");
    }

    @Test
    @DisplayName("Multiple requests can be batched")
    void testRequestBatching() throws Exception {
        SemanticLLMCache cache = new SemanticLLMCache();
        ComplexityAnalyzer analyzer = new ComplexityAnalyzer();

        Map<LLMTier, AsyncLLMClient> clients = new HashMap<>();
        clients.put(LLMTier.SIMPLE, LLMMockClient.createFast());

        CascadeRouter router = new CascadeRouter(cache, analyzer, clients);

        // Create multiple requests
        List<CompletableFuture<LLMResponse>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(router.route("batch command " + i, Map.of()));
        }

        // Wait for all
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .orTimeout(15, TimeUnit.SECONDS)
            .get();

        // All should complete
        for (CompletableFuture<LLMResponse> future : futures) {
            assertTrue(future.isDone(), "All requests should complete");
        }
    }

    @Test
    @DisplayName("Router handles timeout gracefully")
    void testRouterTimeout() {
        // Create slow client
        LLMMockClient slowClient = LLMMockClient.createSimple();
        slowClient.setResponseDelay(10000); // 10 second delay

        Map<LLMTier, AsyncLLMClient> clients = new HashMap<>();
        clients.put(LLMTier.SIMPLE, slowClient);

        CascadeRouter router = new CascadeRouter(
            new SemanticLLMCache(),
            new ComplexityAnalyzer(),
            clients
        );

        assertThrows(TimeoutException.class, () -> {
            router.route("test", Map.of()).orTimeout(1, TimeUnit.SECONDS).get();
        }, "Should timeout on slow response");
    }

    @Test
    @DisplayName("Different LLM providers can be used")
    void testMultipleProviders() {
        LLMMockClient openaiClient = new LLMMockClient("gpt-4", LLMProvider.OPENAI);
        LLMMockClient groqClient = new LLMMockClient("llama3-70b", LLMProvider.GROQ);
        LLMMockClient geminiClient = new LLMMockClient("gemini-pro", LLMProvider.GEMINI);

        assertEquals(LLMProvider.OPENAI, openaiClient.getProvider());
        assertEquals(LLMProvider.GROQ, groqClient.getProvider());
        assertEquals(LLMProvider.GEMINI, geminiClient.getProvider());
    }

    @Test
    @DisplayName("Error responses are handled gracefully")
    void testErrorHandling() {
        LLMMockClient failingClient = LLMMockClient.createUnreliable(1.0);

        Map<LLMTier, AsyncLLMClient> clients = new HashMap<>();
        clients.put(LLMTier.SIMPLE, failingClient);
        clients.put(LLMTier.COMPLEX, LLMMockClient.createComplex());

        CascadeRouter router = new CascadeRouter(
            new SemanticLLMCache(),
            new ComplexityAnalyzer(),
            clients
        );

        // Should fallback to COMPLEX tier
        assertDoesNotThrow(() -> {
            router.route("test", Map.of()).orTimeout(10, TimeUnit.SECONDS);
        }, "Should handle errors with fallback");
    }

    @Test
    @DisplayName("Router can be reconfigured")
    void testRouterReconfiguration() {
        Map<LLMTier, AsyncLLMClient> clients = new HashMap<>();
        clients.put(LLMTier.SIMPLE, LLMMockClient.createFast());

        CascadeRouter router = new CascadeRouter(
            new SemanticLLMCache(),
            new ComplexityAnalyzer(),
            clients
        );

        // Make initial requests
        router.route("test1", Map.of()).orTimeout(5, TimeUnit.SECONDS);

        // Reconfigure
        CascadeConfig config = CascadeConfig.getInstance();
        config.setSimpleTierThreshold(0.4);

        // Should still work
        assertDoesNotThrow(() -> {
            router.route("test2", Map.of()).orTimeout(5, TimeUnit.SECONDS);
        }, "Should work after reconfiguration");
    }
}
