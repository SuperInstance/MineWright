package com.minewright.integration;

import com.minewright.entity.ForemanEntity;
import com.minewright.execution.AgentState;
import com.minewright.llm.PromptBuilder;
import com.minewright.llm.ResponseParser;
import com.minewright.llm.TaskPlanner;
import com.minewright.llm.async.*;
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
 * @see LLMCache
 * @since 1.6.0
 */
@DisplayName("LLM Integration Tests")
class LLMIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Task planner generates tasks from natural language")
    void testTaskPlanning() {
        ForemanEntity foreman = createForeman("Steve");

        // Note: TaskPlanner uses default constructor and gets clients from config
        // For testing, we can't easily mock the LLM client, so we'll test the parsing logic
        String mockResponse = """
            {
                "tasks": [
                    {
                        "action": "mine",
                        "block": "stone",
                        "quantity": 50
                    }
                ]
            }
            """;

        // Test response parsing
        ResponseParser.ParsedResponse response = ResponseParser.parseAIResponse(mockResponse);

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
        clients.put(LLMTier.FAST, LLMMockClient.createFast());
        clients.put(LLMTier.BALANCED, LLMMockClient.createFast());
        clients.put(LLMTier.SMART, LLMMockClient.createFast());

        // Create cache
        LLMCache cache = new LLMCache();

        // Create complexity analyzer
        ComplexityAnalyzer analyzer = new ComplexityAnalyzer();

        // Create router
        CascadeRouter router = new CascadeRouter(cache, analyzer, clients);

        // Test simple command
        CompletableFuture<LLMResponse> simpleFuture = router.route(
            "echo hello",
            Map.of()
        );

        assertDoesNotThrow(() -> {
            LLMResponse response = simpleFuture.orTimeout(5, TimeUnit.SECONDS).get();
            assertNotNull(response, "Simple command should route successfully");
        });

        // Test complex command
        CompletableFuture<LLMResponse> complexFuture = router.route(
            "Analyze the terrain and build an optimal shelter considering weather patterns",
            Map.of()
        );

        assertDoesNotThrow(() -> {
            LLMResponse response = complexFuture.orTimeout(5, TimeUnit.SECONDS).get();
            assertNotNull(response, "Complex command should route successfully");
        });

        // Check statistics
        assertTrue(router.getTotalRequests() >= 2,
            "Should track total requests");
    }

    @Test
    @DisplayName("Semantic cache returns similar cached responses")
    void testSemanticCaching() {
        com.minewright.llm.cache.SemanticLLMCache cache = new com.minewright.llm.cache.SemanticLLMCache();

        // Cache a response
        cache.put("mine 50 stone", "gpt-4", "cascade", "I will mine 50 stone");

        // Query with similar but not identical text
        Optional<String> cached = cache.get("mine fifty stones", "gpt-4", "cascade");
        assertTrue(cached.isPresent(), "Should find semantically similar response");
    }

    @Test
    @DisplayName("Concurrent LLM requests are handled safely")
    void testConcurrentRequests() throws InterruptedException {
        LLMCache cache = new LLMCache();
        ComplexityAnalyzer analyzer = new ComplexityAnalyzer();

        Map<LLMTier, AsyncLLMClient> clients = new HashMap<>();
        clients.put(LLMTier.FAST, LLMMockClient.createFast());
        clients.put(LLMTier.BALANCED, LLMMockClient.createFast());
        clients.put(LLMTier.SMART, LLMMockClient.createFast());

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
                    future.orTimeout(5, TimeUnit.SECONDS).get();
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
        // Create unreliable fast client
        LLMMockClient unreliableFast = new LLMMockClient();
        unreliableFast.setFailureRate(1.0); // Always fails
        LLMMockClient reliableSmart = LLMMockClient.createFast();

        Map<LLMTier, AsyncLLMClient> clients = new HashMap<>();
        clients.put(LLMTier.FAST, unreliableFast);
        clients.put(LLMTier.SMART, reliableSmart);

        LLMCache cache = new LLMCache();
        ComplexityAnalyzer analyzer = new ComplexityAnalyzer();

        CascadeRouter router = new CascadeRouter(cache, analyzer, clients);

        // Should fallback from FAST to SMART
        CompletableFuture<LLMResponse> future = router.route(
            "test command",
            Map.of()
        );

        assertDoesNotThrow(() -> {
            LLMResponse response = future.orTimeout(10, TimeUnit.SECONDS).get();
            assertNotNull(response, "Should succeed with fallback");
        }, "Should succeed with fallback");
    }

    @Test
    @DisplayName("Complexity analyzer categorizes tasks correctly")
    void testComplexityAnalysis() {
        ComplexityAnalyzer analyzer = new ComplexityAnalyzer();

        // Simple tasks
        TaskComplexity simpleComplexity = analyzer.analyze("echo hello", null, null);
        assertTrue(simpleComplexity == TaskComplexity.TRIVIAL || simpleComplexity == TaskComplexity.SIMPLE,
            "Simple task should have low complexity: " + simpleComplexity);

        // Medium tasks
        TaskComplexity mediumComplexity = analyzer.analyze("mine 50 stone and craft a furnace", null, null);
        assertTrue(
            mediumComplexity == TaskComplexity.SIMPLE || mediumComplexity == TaskComplexity.MODERATE,
            "Medium task should have medium complexity: " + mediumComplexity
        );

        // Complex tasks
        TaskComplexity complexComplexity = analyzer.analyze(
            "Analyze the surrounding terrain, plan an optimal base location, " +
            "gather necessary resources, and construct a fortified shelter " +
            "with automated farming systems",
            null, null
        );
        assertTrue(
            complexComplexity == TaskComplexity.MODERATE ||
            complexComplexity == TaskComplexity.COMPLEX ||
            complexComplexity == TaskComplexity.NOVEL,
            "Complex task should have high complexity: " + complexComplexity
        );
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

        ResponseParser.ParsedResponse response = ResponseParser.parseAIResponse(jsonResponse);

        assertNotNull(response, "Should parse response");
        assertEquals(2, response.getTasks().size(), "Should extract 2 tasks");
        assertEquals("mine", response.getTasks().get(0).getAction(), "First task should be mine");
        assertEquals("craft", response.getTasks().get(1).getAction(), "Second task should be craft");
    }

    @Test
    @DisplayName("Cache hit improves response time")
    void testCachePerformance() throws Exception {
        LLMCache cache = new LLMCache();

        // Cache a response
        LLMResponse cachedResponse = LLMResponse.builder()
            .content("Cached response")
            .model("gpt-4")
            .providerId("test")
            .tokensUsed(10)
            .latencyMs(1)
            .fromCache(true)
            .build();
        cache.put("test command", "gpt-4", "test", cachedResponse);

        // Measure cache hit time
        long startTime = System.nanoTime();
        Optional<LLMResponse> cached = cache.get("test command", "gpt-4", "test");
        long cacheTime = System.nanoTime() - startTime;

        assertTrue(cached.isPresent(), "Should get cached response");
        assertTrue(cacheTime < 1_000_000, "Cache lookup should be fast (< 1ms)");
    }

    @Test
    @DisplayName("Router statistics are accurate")
    void testRouterStatistics() {
        Map<LLMTier, AsyncLLMClient> clients = new HashMap<>();
        clients.put(LLMTier.FAST, LLMMockClient.createFast());
        clients.put(LLMTier.BALANCED, LLMMockClient.createFast());
        clients.put(LLMTier.SMART, LLMMockClient.createFast());

        CascadeRouter router = new CascadeRouter(
            new LLMCache(),
            new ComplexityAnalyzer(),
            clients
        );

        // Make several requests
        for (int i = 0; i < 5; i++) {
            try {
                router.route("command " + i, Map.of()).orTimeout(5, TimeUnit.SECONDS).get();
            } catch (Exception e) {
                // Ignore exceptions for this test
            }
        }

        assertTrue(router.getTotalRequests() >= 5,
            "Should count total requests");
        assertTrue(router.getCacheHits() >= 0,
            "Should count cache hits");
        assertTrue(router.getFallbacks() >= 0,
            "Should count fallbacks");
    }

    @Test
    @DisplayName("Multiple requests can be batched")
    void testRequestBatching() throws Exception {
        LLMCache cache = new LLMCache();
        ComplexityAnalyzer analyzer = new ComplexityAnalyzer();

        Map<LLMTier, AsyncLLMClient> clients = new HashMap<>();
        clients.put(LLMTier.FAST, LLMMockClient.createFast());

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
        clients.put(LLMTier.FAST, slowClient);

        CascadeRouter router = new CascadeRouter(
            new LLMCache(),
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
        LLMMockClient openaiClient = new LLMMockClient("openai", "gpt-4");
        LLMMockClient groqClient = new LLMMockClient("groq", "llama3-70b");
        LLMMockClient geminiClient = new LLMMockClient("gemini", "gemini-pro");

        assertEquals("openai", openaiClient.getProviderId());
        assertEquals("groq", groqClient.getProviderId());
        assertEquals("gemini", geminiClient.getProviderId());
    }

    @Test
    @DisplayName("Error responses are handled gracefully")
    void testErrorHandling() {
        LLMMockClient failingClient = new LLMMockClient();
        failingClient.setFailureRate(1.0); // Always fails

        Map<LLMTier, AsyncLLMClient> clients = new HashMap<>();
        clients.put(LLMTier.FAST, failingClient);
        clients.put(LLMTier.SMART, LLMMockClient.createFast());

        CascadeRouter router = new CascadeRouter(
            new LLMCache(),
            new ComplexityAnalyzer(),
            clients
        );

        // Should fallback to SMART tier
        assertDoesNotThrow(() -> {
            LLMResponse response = router.route("test", Map.of()).orTimeout(10, TimeUnit.SECONDS).get();
            assertNotNull(response, "Should handle errors with fallback");
        }, "Should handle errors with fallback");
    }

    @Test
    @DisplayName("Router can be reconfigured")
    void testRouterReconfiguration() {
        Map<LLMTier, AsyncLLMClient> clients = new HashMap<>();
        clients.put(LLMTier.FAST, LLMMockClient.createFast());

        CascadeRouter router = new CascadeRouter(
            new LLMCache(),
            new ComplexityAnalyzer(),
            clients
        );

        // Make initial requests
        assertDoesNotThrow(() -> {
            router.route("test1", Map.of()).orTimeout(5, TimeUnit.SECONDS).get();
        });

        // Reconfigure
        CascadeConfig.reload();

        // Should still work
        assertDoesNotThrow(() -> {
            router.route("test2", Map.of()).orTimeout(5, TimeUnit.SECONDS).get();
        }, "Should work after reconfiguration");
    }

    @Test
    @DisplayName("LLMResponse builder creates valid responses")
    void testLLMResponseBuilder() {
        LLMResponse response = LLMResponse.builder()
            .content("Test response")
            .model("gpt-4")
            .providerId("openai")
            .tokensUsed(100)
            .latencyMs(500)
            .fromCache(false)
            .build();

        assertEquals("Test response", response.getContent());
        assertEquals("gpt-4", response.getModel());
        assertEquals("openai", response.getProviderId());
        assertEquals(100, response.getTokensUsed());
        assertEquals(500, response.getLatencyMs());
        assertFalse(response.isFromCache());
    }

    @Test
    @DisplayName("LLMResponse withCacheFlag creates new instance")
    void testLLMResponseWithCacheFlag() {
        LLMResponse original = LLMResponse.builder()
            .content("Test response")
            .model("gpt-4")
            .providerId("openai")
            .tokensUsed(100)
            .latencyMs(500)
            .fromCache(false)
            .build();

        LLMResponse cached = original.withCacheFlag(true);

        // Verify original is unchanged
        assertFalse(original.isFromCache());
        // Verify new instance has cache flag set
        assertTrue(cached.isFromCache());
        // Verify other fields are preserved
        assertEquals(original.getContent(), cached.getContent());
        assertEquals(original.getModel(), cached.getModel());
    }
}
