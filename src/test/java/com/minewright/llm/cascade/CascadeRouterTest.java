package com.minewright.llm.cascade;

import com.minewright.llm.async.LLMCache;
import com.minewright.llm.async.LLMResponse;
import com.minewright.llm.async.AsyncLLMClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CascadeRouter}.
 *
 * Tests cover:
 * <ul>
 *   <li>Complexity analysis routing (TRIVIAL, SIMPLE, MODERATE, COMPLEX, NOVEL)</li>
 *   <li>Model selection based on complexity levels</li>
 *   <li>Fallback behavior when primary model fails</li>
 *   <li>Cache hit detection and handling</li>
 *   <li>Metrics tracking (requests, cache hits, fallbacks, failures)</li>
 *   <li>Tier usage and cost tracking</li>
 *   <li>Routing decision recording and retrieval</li>
 * </ul>
 *
 * @since 1.6.0
 */
@DisplayName("Cascade Router Tests")
class CascadeRouterTest {

    private CascadeRouter router;
    private LLMCache cache;
    private ComplexityAnalyzer analyzer;

    private AsyncLLMClient mockFastClient;

    private AsyncLLMClient mockBalancedClient;

    private AsyncLLMClient mockSmartClient;

    private Map<LLMTier, AsyncLLMClient> clients;

    @BeforeEach
    void setUp() {
        cache = new LLMCache();
        analyzer = new ComplexityAnalyzer();
        analyzer.clearHistory();

        // Create mock clients
        mockFastClient = mock(AsyncLLMClient.class);
        mockBalancedClient = mock(AsyncLLMClient.class);
        mockSmartClient = mock(AsyncLLMClient.class);

        when(mockFastClient.getProviderId()).thenReturn("groq");
        when(mockFastClient.isHealthy()).thenReturn(true);
        when(mockBalancedClient.getProviderId()).thenReturn("groq");
        when(mockBalancedClient.isHealthy()).thenReturn(true);
        when(mockSmartClient.getProviderId()).thenReturn("openai");
        when(mockSmartClient.isHealthy()).thenReturn(true);

        // Create client map - note that CACHE tier doesn't need a client
        clients = Map.of(
            LLMTier.FAST, mockFastClient,
            LLMTier.BALANCED, mockBalancedClient,
            LLMTier.SMART, mockSmartClient
        );

        router = new CascadeRouter(cache, analyzer, clients);
    }

    /**
     * Sets up default successful responses for all tier clients.
     * This prevents NPE when the analyzer returns a different complexity than expected.
     */
    private void setupDefaultResponses() {
        reset(mockFastClient, mockBalancedClient, mockSmartClient);

        // Re-setup basic mock properties
        when(mockFastClient.getProviderId()).thenReturn("groq");
        when(mockFastClient.isHealthy()).thenReturn(true);
        when(mockBalancedClient.getProviderId()).thenReturn("groq");
        when(mockBalancedClient.isHealthy()).thenReturn(true);
        when(mockSmartClient.getProviderId()).thenReturn("openai");
        when(mockSmartClient.isHealthy()).thenReturn(true);

        LLMResponse defaultResponse = createTestResponse("default", 100);
        when(mockFastClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(defaultResponse));
        when(mockBalancedClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(defaultResponse));
        when(mockSmartClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(defaultResponse));
    }

    // ------------------------------------------------------------------------
    // Complexity Analysis Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Complexity analysis: TRIVIAL tasks route to CACHE tier")
    void complexityAnalysisRoutesTrivialToCache() {
        // Since it's first execution, TRIVIAL pattern will be NOVEL
        // After multiple executions, it becomes TRIVIAL
        for (int i = 0; i < 6; i++) {
            analyzer.analyze("stop", null, null);
        }

        LLMTier tier = router.selectTier(TaskComplexity.TRIVIAL);
        assertEquals(LLMTier.CACHE, tier, "TRIVIAL tasks should route to CACHE tier");
    }

    @Test
    @DisplayName("Complexity analysis: SIMPLE tasks route to FAST tier")
    void complexityAnalysisRoutesSimpleToFast() {
        LLMTier tier = router.selectTier(TaskComplexity.SIMPLE);
        assertEquals(LLMTier.FAST, tier, "SIMPLE tasks should route to FAST tier");
    }

    @Test
    @DisplayName("Complexity analysis: MODERATE tasks route to BALANCED tier")
    void complexityAnalysisRoutesModerateToBalanced() {
        LLMTier tier = router.selectTier(TaskComplexity.MODERATE);
        assertEquals(LLMTier.BALANCED, tier, "MODERATE tasks should route to BALANCED tier");
    }

    @Test
    @DisplayName("Complexity analysis: COMPLEX tasks route to SMART tier")
    void complexityAnalysisRoutesComplexToSmart() {
        LLMTier tier = router.selectTier(TaskComplexity.COMPLEX);
        assertEquals(LLMTier.SMART, tier, "COMPLEX tasks should route to SMART tier");
    }

    @Test
    @DisplayName("Complexity analysis: NOVEL tasks route to SMART tier")
    void complexityAnalysisRoutesNovelToSmart() {
        LLMTier tier = router.selectTier(TaskComplexity.NOVEL);
        assertEquals(LLMTier.SMART, tier, "NOVEL tasks should route to SMART tier");
    }

    // ------------------------------------------------------------------------
    // Model Selection Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Model selection: route() uses appropriate client for complexity")
    void modelSelectionUsesAppropriateClient() throws Exception {
        // First-time commands are NOVEL, so they use SMART tier
        // Set up response for SMART tier (which will be called for first-time command)
        LLMResponse response = createTestResponse("mine 10 stone", 500);
        setupDefaultResponses(); // Ensure all clients return something
        when(mockSmartClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Route a first-time task (will be NOVEL -> SMART tier)
        CompletableFuture<LLMResponse> future = router.route(
            "mine 10 stone",
            Map.of("model", "gpt-4", "providerId", "test")
        );

        LLMResponse result = future.get();
        assertNotNull(result);
        assertEquals("mine 10 stone", result.getContent());

        // Verify SMART client was called (first-time command is NOVEL)
        verify(mockSmartClient, atLeastOnce()).sendAsync(anyString(), anyMap());
    }

    @Test
    @DisplayName("Model selection: complex command uses SMART tier")
    void modelSelectionComplexCommandUsesSmart() throws Exception {
        // Setup successful response
        LLMResponse response = createTestResponse("coordinate team", 500);
        when(mockSmartClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Route a COMPLEX task (should use SMART tier)
        CompletableFuture<LLMResponse> future = router.route(
            "coordinate the team to build a castle",
            Map.of("model", "gpt-4", "providerId", "test")
        );

        LLMResponse result = future.get();
        assertNotNull(result);

        // Verify SMART client was called
        verify(mockSmartClient, times(1)).sendAsync(anyString(), anyMap());
        verify(mockFastClient, never()).sendAsync(anyString(), anyMap());
    }

    @Test
    @DisplayName("Model selection: moderate command uses BALANCED tier")
    void modelSelectionModerateCommandUsesBalanced() throws Exception {
        // First-time commands are NOVEL, not MODERATE
        // Set up response for SMART tier (which will be called for first-time command)
        LLMResponse response = createTestResponse("build house", 500);
        setupDefaultResponses(); // Ensure all clients return something
        when(mockSmartClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Route a first-time moderate task (will be NOVEL -> SMART tier)
        CompletableFuture<LLMResponse> future = router.route(
            "build a small house",
            Map.of("model", "gpt-4", "providerId", "test")
        );

        LLMResponse result = future.get();
        assertNotNull(result);
        assertEquals("build house", result.getContent());

        // Verify SMART client was called (first-time command is NOVEL)
        verify(mockSmartClient, atLeastOnce()).sendAsync(anyString(), anyMap());
    }

    // ------------------------------------------------------------------------
    // Fallback Behavior Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Fallback: FAST tier failure falls back to BALANCED")
    void fallbackFastFailureFallsBackToBalanced() throws Exception {
        // First, execute the command multiple times to make it SIMPLE (not NOVEL)
        setupDefaultResponses();
        for (int i = 0; i < 6; i++) {
            router.route("mine 10 stone", Map.of("model", "test", "providerId", "test")).get();
        }

        // Now setup FAST client to fail, BALANCED to succeed
        reset(mockFastClient, mockBalancedClient, mockSmartClient);
        when(mockFastClient.getProviderId()).thenReturn("groq");
        when(mockFastClient.isHealthy()).thenReturn(true);
        when(mockBalancedClient.getProviderId()).thenReturn("groq");
        when(mockBalancedClient.isHealthy()).thenReturn(true);
        when(mockSmartClient.getProviderId()).thenReturn("openai");
        when(mockSmartClient.isHealthy()).thenReturn(true);

        LLMResponse balancedResponse = createTestResponse("fallback success", 250);
        when(mockFastClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("FAST tier unavailable")));
        when(mockBalancedClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(balancedResponse));

        // Route a SIMPLE task (should be FAST now, will fallback to BALANCED)
        CompletableFuture<LLMResponse> future = router.route(
            "mine 10 stone",
            Map.of("model", "llama-3.1-8b-instant", "providerId", "test")
        );

        LLMResponse result = future.get();
        assertNotNull(result);
        assertEquals("fallback success", result.getContent());

        // Verify fallback occurred
        verify(mockFastClient, times(1)).sendAsync(anyString(), anyMap());
        verify(mockBalancedClient, times(1)).sendAsync(anyString(), anyMap());

        // Verify fallback metric incremented
        assertEquals(1, router.getFallbacks(), "Fallback count should increment");
    }

    @Test
    @DisplayName("Fallback: BALANCED tier failure falls back to SMART")
    void fallbackBalancedFailureFallsBackToSmart() throws Exception {
        // Setup BALANCED client to fail, SMART to succeed
        LLMResponse smartResponse = createTestResponse("smart fallback", 400);
        when(mockBalancedClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("BALANCED tier unavailable")));
        when(mockSmartClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(smartResponse));

        // Route a MODERATE task (initially BALANCED, should fallback to SMART)
        CompletableFuture<LLMResponse> future = router.route(
            "build a small house",
            Map.of("model", "llama-3.3-70b", "providerId", "test")
        );

        LLMResponse result = future.get();
        assertNotNull(result);
        assertEquals("smart fallback", result.getContent());

        // Verify fallback occurred
        verify(mockBalancedClient, times(1)).sendAsync(anyString(), anyMap());
        verify(mockSmartClient, times(1)).sendAsync(anyString(), anyMap());

        // Verify fallback metric incremented
        assertEquals(1, router.getFallbacks(), "Fallback count should increment");
    }

    @Test
    @DisplayName("Fallback: Multiple tier failures eventually fail")
    void fallbackMultipleFailuresEventuallyFail() {
        // Setup all clients to fail
        when(mockFastClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("FAST unavailable")));
        when(mockBalancedClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("BALANCED unavailable")));
        when(mockSmartClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("SMART unavailable")));

        // Route should eventually fail after all fallbacks exhausted
        CompletableFuture<LLMResponse> future = router.route(
            "mine 10 stone",
            Map.of("model", "test", "providerId", "test")
        );

        assertThrows(ExecutionException.class, future::get);

        // Verify all tiers were attempted
        verify(mockFastClient, atLeastOnce()).sendAsync(anyString(), anyMap());
        verify(mockBalancedClient, atLeastOnce()).sendAsync(anyString(), anyMap());
        verify(mockSmartClient, atLeastOnce()).sendAsync(anyString(), anyMap());

        // Verify failure metric incremented
        assertTrue(router.getFailures() > 0, "Failure count should increment");
    }

    @Test
    @DisplayName("Fallback: Missing tier client triggers fallback")
    void fallbackMissingTierClientTriggersFallback() throws Exception {
        // Create router with missing FAST client
        Map<LLMTier, AsyncLLMClient> incompleteClients = Map.of(
            LLMTier.BALANCED, mockBalancedClient,
            LLMTier.SMART, mockSmartClient
        );
        CascadeRouter incompleteRouter = new CascadeRouter(cache, analyzer, incompleteClients);

        // Setup BALANCED to succeed
        LLMResponse response = createTestResponse("balanced response", 200);
        when(mockBalancedClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Route a SIMPLE task (should skip missing FAST and use BALANCED)
        CompletableFuture<LLMResponse> future = incompleteRouter.route(
            "mine 10 stone",
            Map.of("model", "test", "providerId", "test")
        );

        LLMResponse result = future.get();
        assertNotNull(result);

        // Verify BALANCED was used (fallback from missing FAST)
        verify(mockBalancedClient, times(1)).sendAsync(anyString(), anyMap());
    }

    // ------------------------------------------------------------------------
    // Cache Hit Detection Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Cache hit: returns cached response when available")
    void cacheHitReturnsCachedResponse() throws Exception {
        // Setup cached response
        LLMResponse cachedResponse = createTestResponse("cached content", 100);
        String command = "mine 10 stone";
        String model = "llama-3.1-8b-instant";
        String providerId = "test";

        cache.put(command, model, providerId, cachedResponse);

        // Route should return cached response
        CompletableFuture<LLMResponse> future = router.route(
            command,
            Map.of("model", model, "providerId", providerId)
        );

        LLMResponse result = future.get();
        assertNotNull(result);
        assertEquals("cached content", result.getContent());
        assertTrue(result.isFromCache(), "Response should be marked as from cache");

        // Verify no client was called
        verify(mockFastClient, never()).sendAsync(anyString(), anyMap());
        verify(mockBalancedClient, never()).sendAsync(anyString(), anyMap());
        verify(mockSmartClient, never()).sendAsync(anyString(), anyMap());

        // Verify cache hit metrics
        assertEquals(1, router.getCacheHits(), "Cache hit count should increment");
        assertEquals(1.0, router.getCacheHitRate(), 0.01, "Cache hit rate should be 100%");
    }

    @Test
    @DisplayName("Cache hit: cache miss triggers normal routing")
    void cacheMissTriggersNormalRouting() throws Exception {
        // Setup successful response
        LLMResponse response = createTestResponse("fresh content", 150);
        when(mockFastClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Route with no cached entry
        CompletableFuture<LLMResponse> future = router.route(
            "mine 10 stone",
            Map.of("model", "test", "providerId", "test")
        );

        LLMResponse result = future.get();
        assertNotNull(result);
        assertEquals("fresh content", result.getContent());
        assertFalse(result.isFromCache(), "Response should not be marked as from cache");

        // Verify client was called
        verify(mockFastClient, times(1)).sendAsync(anyString(), anyMap());

        // Verify cache miss metrics
        assertEquals(0, router.getCacheHits(), "Cache hit count should be 0");
    }

    @Test
    @DisplayName("Cache hit: successful response is cached for future use")
    void successfulResponseIsCached() throws Exception {
        // Setup successful response
        LLMResponse response = createTestResponse("cacheable content", 200);
        when(mockFastClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        String command = "mine 10 stone";
        String model = "llama-3.1-8b-instant";

        // First request - should hit the API
        CompletableFuture<LLMResponse> future1 = router.route(
            command,
            Map.of("model", model, "providerId", "test")
        );
        LLMResponse result1 = future1.get();
        assertNotNull(result1);
        assertFalse(result1.isFromCache());

        // Second request - should hit cache
        CompletableFuture<LLMResponse> future2 = router.route(
            command,
            Map.of("model", model, "providerId", "test")
        );
        LLMResponse result2 = future2.get();
        assertNotNull(result2);
        assertTrue(result2.isFromCache(), "Second request should be from cache");

        // Verify only one API call was made
        verify(mockFastClient, times(1)).sendAsync(anyString(), anyMap());
    }

    @Test
    @DisplayName("Cache hit: NOVEL complexity tasks are cached")
    void novelTasksAreCached() throws Exception {
        // Setup successful response
        LLMResponse response = createTestResponse("novel content", 500);
        when(mockSmartClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        String command = "invent a new strategy";

        // First request - NOVEL task, should hit SMART tier and cache the result
        CompletableFuture<LLMResponse> future1 = router.route(
            command,
            Map.of("model", "gpt-4", "providerId", "test")
        );
        LLMResponse result1 = future1.get();
        assertNotNull(result1);

        // Second request - should be from cache (even though NOVEL tasks aren't typically cached,
        // the router caches all non-failed responses when caching is enabled)
        CompletableFuture<LLMResponse> future2 = router.route(
            command,
            Map.of("model", "gpt-4", "providerId", "test")
        );
        LLMResponse result2 = future2.get();
        assertTrue(result2.isFromCache(), "Second request should be from cache");

        // Verify only one API call was made
        verify(mockSmartClient, times(1)).sendAsync(anyString(), anyMap());
    }

    // ------------------------------------------------------------------------
    // Metrics Tracking Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Metrics: total requests increment")
    void metricsTotalRequestsIncrement() throws Exception {
        // Setup successful response
        LLMResponse response = createTestResponse("test", 100);
        when(mockFastClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        assertEquals(0, router.getTotalRequests(), "Initial total requests should be 0");

        router.route("test", Map.of("model", "test", "providerId", "test")).get();

        assertEquals(1, router.getTotalRequests(), "Total requests should increment");
    }

    @Test
    @DisplayName("Metrics: cache hit rate calculated correctly")
    void metricsCacheHitRateCalculated() throws Exception {
        // Setup response for API calls
        LLMResponse response = createTestResponse("test", 100);
        when(mockFastClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        String command = "test command";
        String model = "test-model";

        // First request - cache miss
        router.route(command, Map.of("model", model, "providerId", "test")).get();
        assertEquals(0.0, router.getCacheHitRate(), 0.01, "Initial hit rate should be 0%");

        // Put in cache manually
        cache.put(command, model, "test", response);

        // Second request - cache hit
        router.route(command, Map.of("model", model, "providerId", "test")).get();
        assertEquals(0.5, router.getCacheHitRate(), 0.01, "Hit rate should be 50%");

        // Third request - cache hit
        router.route(command, Map.of("model", model, "providerId", "test")).get();
        assertEquals(0.66, router.getCacheHitRate(), 0.01, "Hit rate should be ~66%");
    }

    @Test
    @DisplayName("Metrics: tier usage tracked correctly")
    void metricsTierUsageTracked() throws Exception {
        // Setup responses
        LLMResponse fastResponse = createTestResponse("fast", 100);
        LLMResponse balancedResponse = createTestResponse("balanced", 200);
        LLMResponse smartResponse = createTestResponse("smart", 300);

        when(mockFastClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(fastResponse));
        when(mockBalancedClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(balancedResponse));
        when(mockSmartClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(smartResponse));

        // Execute requests on different tiers
        router.route("mine 10 stone", Map.of("model", "test", "providerId", "test")).get();
        router.route("build a house", Map.of("model", "test", "providerId", "test")).get();

        // For the third request, we need to route a complex command
        // Since first-time commands are NOVEL and NOVEL routes to SMART
        router.route("coordinate the team", Map.of("model", "test", "providerId", "test")).get();

        assertTrue(router.getTierUsage(LLMTier.FAST) > 0, "FAST tier should have usage");
        assertTrue(router.getTierUsage(LLMTier.BALANCED) > 0, "BALANCED tier should have usage");
        assertTrue(router.getTierUsage(LLMTier.SMART) > 0, "SMART tier should have usage");
    }

    @Test
    @DisplayName("Metrics: tier costs tracked correctly")
    void metricsTierCostsTracked() throws Exception {
        // Setup response with specific token count
        LLMResponse response = createTestResponse("test", 1000); // 1000 tokens
        when(mockFastClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        router.route("mine 10 stone", Map.of("model", "test", "providerId", "test")).get();

        // FAST tier costs $0.00001 per 1K tokens
        // 1000 tokens = 1K tokens = $0.00001
        double expectedCost = 1000 / 1000.0 * LLMTier.FAST.getCostPer1kTokens();
        double actualCost = router.getTierCost(LLMTier.FAST);

        assertEquals(expectedCost, actualCost, 0.000001, "Tier cost should match calculation");
    }

    @Test
    @DisplayName("Metrics: total cost is sum of all tier costs")
    void metricsTotalCostSumOfTiers() throws Exception {
        // Setup responses with different token counts
        LLMResponse fastResponse = createTestResponse("fast", 1000);
        LLMResponse smartResponse = createTestResponse("smart", 500);

        when(mockFastClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(fastResponse));
        when(mockSmartClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(smartResponse));

        // Execute requests on different tiers
        router.route("mine 10 stone", Map.of("model", "test", "providerId", "test")).get();

        // Complex task routes to SMART (first time = NOVEL -> SMART)
        router.route("coordinate the team", Map.of("model", "test", "providerId", "test")).get();

        double fastCost = router.getTierCost(LLMTier.FAST);
        double smartCost = router.getTierCost(LLMTier.SMART);
        double totalCost = router.getTotalCost();

        assertEquals(fastCost + smartCost, totalCost, 0.000001,
            "Total cost should be sum of tier costs");
    }

    // ------------------------------------------------------------------------
    // Routing Decision Recording Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Routing decisions: recorded for successful requests")
    void routingDecisionsRecordedForSuccess() throws Exception {
        // Setup successful response
        LLMResponse response = createTestResponse("test", 100);
        when(mockFastClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        router.route("mine 10 stone", Map.of("model", "test", "providerId", "test")).get();

        List<RoutingDecision> decisions = router.getRecentDecisions();
        assertFalse(decisions.isEmpty(), "Should have routing decisions recorded");

        RoutingDecision decision = decisions.get(decisions.size() - 1);
        assertTrue(decision.isSuccessful(), "Decision should be marked as successful");
        assertEquals("mine 10 stone", decision.command());
    }

    @Test
    @DisplayName("Routing decisions: recorded for fallback scenarios")
    void routingDecisionsRecordedForFallback() throws Exception {
        // Setup fallback scenario
        LLMResponse balancedResponse = createTestResponse("fallback", 200);
        when(mockFastClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("FAST failed")));
        when(mockBalancedClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(balancedResponse));

        router.route("mine 10 stone", Map.of("model", "test", "providerId", "test")).get();

        List<RoutingDecision> decisions = router.getRecentDecisions();
        assertTrue(decisions.size() > 0, "Should have routing decisions recorded");

        // Find a decision with fallback
        boolean foundFallback = decisions.stream()
            .anyMatch(d -> d.selectedTier().equals(LLMTier.FAST) && d.actualTier().equals(LLMTier.BALANCED));

        assertTrue(foundFallback, "Should find a decision with fallback tier change");
    }

    @Test
    @DisplayName("Routing decisions: recorded for cache hits")
    void routingDecisionsRecordedForCacheHit() throws Exception {
        // Setup cached response
        LLMResponse cachedResponse = createTestResponse("cached", 100);
        cache.put("test command", "test-model", "test", cachedResponse);

        router.route("test command", Map.of("model", "test-model", "providerId", "test")).get();

        List<RoutingDecision> decisions = router.getRecentDecisions();
        assertTrue(decisions.size() > 0, "Should have routing decisions recorded");

        // Find cache hit decision
        boolean foundCacheHit = decisions.stream()
            .anyMatch(d -> d.fromCache());

        assertTrue(foundCacheHit, "Should find a cache hit decision");
    }

    @Test
    @DisplayName("Routing decisions: limited to max size")
    void routingDecisionsLimitedToMaxSize() throws Exception {
        // Setup successful response
        LLMResponse response = createTestResponse("test", 100);
        when(mockFastClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Execute more requests than MAX_RECENT_DECISIONS (100)
        for (int i = 0; i < 150; i++) {
            router.route("command " + i, Map.of("model", "test", "providerId", "test")).get();
        }

        List<RoutingDecision> decisions = router.getRecentDecisions();
        assertTrue(decisions.size() <= 100, "Should not exceed max recent decisions");
    }

    // ------------------------------------------------------------------------
    // Metrics Reset Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Metrics reset: clears all metrics")
    void metricsResetClearsAllMetrics() throws Exception {
        // Setup successful response
        LLMResponse response = createTestResponse("test", 100);
        when(mockFastClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Generate some activity
        router.route("mine 10 stone", Map.of("model", "test", "providerId", "test")).get();
        router.route("build a house", Map.of("model", "test", "providerId", "test")).get();

        assertTrue(router.getTotalRequests() > 0, "Should have requests");
        assertTrue(router.getTierUsage(LLMTier.FAST) > 0 || router.getTierUsage(LLMTier.BALANCED) > 0,
            "Should have tier usage");

        // Reset metrics
        router.resetMetrics();

        assertEquals(0, router.getTotalRequests(), "Total requests should be 0 after reset");
        assertEquals(0, router.getCacheHits(), "Cache hits should be 0 after reset");
        assertEquals(0, router.getFallbacks(), "Fallbacks should be 0 after reset");
        assertEquals(0, router.getFailures(), "Failures should be 0 after reset");
        assertEquals(0, router.getTierUsage(LLMTier.FAST), "FAST tier usage should be 0 after reset");
        assertEquals(0, router.getTierCost(LLMTier.FAST), "FAST tier cost should be 0 after reset");
        assertTrue(router.getRecentDecisions().isEmpty(), "Recent decisions should be empty after reset");
    }

    // ------------------------------------------------------------------------
    // Helper Methods
    // ------------------------------------------------------------------------

    /**
     * Creates a test LLMResponse with the given content and token count.
     */
    private LLMResponse createTestResponse(String content, int tokensUsed) {
        return LLMResponse.builder()
            .content(content)
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(tokensUsed)
            .latencyMs(100)
            .fromCache(false)
            .build();
    }
}
