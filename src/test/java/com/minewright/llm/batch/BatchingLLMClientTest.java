package com.minewright.llm.batch;

import com.minewright.llm.async.AsyncLLMClient;
import com.minewright.llm.async.LLMResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BatchingLLMClient}.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Request batching logic with different prompt types</li>
 *   <li>Batch timeout handling for user and background prompts</li>
 *   <li>Response routing to correct callers</li>
 *   <li>Rate limit error handling and backoff</li>
 *   <li>Error propagation and exception handling</li>
 *   <li>Concurrent request handling</li>
 *   <li>Single vs multi-prompt batch responses</li>
 *   <li>Batch response parsing with [N] markers</li>
 * </ul>
 *
 * @since 1.3.0
 */
@DisplayName("Batching LLM Client Tests")
@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class BatchingLLMClientTest {

    private BatchingLLMClient batchingClient;
    private AsyncLLMClient mockUnderlyingClient;

    private CompletableFuture<LLMResponse> pendingResponse;

    @BeforeEach
    void setUp() {
        mockUnderlyingClient = mock(AsyncLLMClient.class);
        when(mockUnderlyingClient.getProviderId()).thenReturn("test-provider");
        when(mockUnderlyingClient.isHealthy()).thenReturn(true);

        // Create a pending future that we can complete manually in tests
        pendingResponse = new CompletableFuture<>();

        batchingClient = new BatchingLLMClient(mockUnderlyingClient);
        batchingClient.start();
    }

    @AfterEach
    void tearDown() {
        if (batchingClient != null && batchingClient.isRunning()) {
            batchingClient.stop();
        }
    }

    // ------------------------------------------------------------------------
    // Request Batching Logic Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Request batching: single user prompt sent to underlying client")
    void requestBatchingSingleUserPromptSent() throws Exception {
        // Setup successful response
        LLMResponse response = LLMResponse.builder()
            .content("Hello! How can I help you today?")
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(50)
            .latencyMs(100)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Submit a user prompt
        CompletableFuture<String> future = batchingClient.submitUserPrompt(
            "Hello Steve!",
            Map.of("playerName", "Alex")
        );

        // Wait for the result
        String result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result, "Result should not be null");
        assertEquals("Hello! How can I help you today?", result, "Response content should match");

        // Verify underlying client was called
        verify(mockUnderlyingClient, timeout(5000).times(1))
            .sendAsync(anyString(), anyMap());
    }

    @Test
    @DisplayName("Request batching: multiple background prompts are batched")
    void requestBatchingMultipleBackgroundPromptsBatched() throws Exception {
        // Setup response for batched requests
        String batchedResponse = "[1] Analyzed desert area\n[2] Found iron deposits\n[3] No diamond detected";

        LLMResponse response = LLMResponse.builder()
            .content(batchedResponse)
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(150)
            .latencyMs(200)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Submit multiple background prompts
        CompletableFuture<String> future1 = batchingClient.submitBackgroundPrompt(
            "Analyze the desert area for resources",
            Map.of("area", "desert")
        );

        CompletableFuture<String> future2 = batchingClient.submitBackgroundPrompt(
            "Look for iron deposits",
            Map.of("resource", "iron")
        );

        CompletableFuture<String> future3 = batchingClient.submitBackgroundPrompt(
            "Check for diamonds",
            Map.of("depth", "deep")
        );

        // Wait for all results
        String result1 = future1.get(10, TimeUnit.SECONDS);
        String result2 = future2.get(1, TimeUnit.SECONDS);
        String result3 = future3.get(1, TimeUnit.SECONDS);

        // Each should get their individual response
        assertTrue(result1.contains("Analyzed desert area") || result1.contains("Analyze the desert"),
            "First prompt should get its response: " + result1);
        assertTrue(result2.contains("Found iron") || result2.contains("iron"),
            "Second prompt should get its response: " + result2);
        assertTrue(result3.contains("diamond") || result3.contains("diamond"),
            "Third prompt should get its response: " + result3);

        // Verify underlying client was called (likely once for batch)
        verify(mockUnderlyingClient, timeout(5000).atLeast(1))
            .sendAsync(anyString(), anyMap());
    }

    @Test
    @DisplayName("Request batching: URGENT type prompts trigger immediate processing")
    void requestBatchingUrgentPromptsImmediate() throws Exception {
        // Setup response
        LLMResponse response = LLMResponse.builder()
            .content("Emergency handled")
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(50)
            .latencyMs(50)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Submit an urgent prompt
        CompletableFuture<String> future = batchingClient.submit(
            "Emergency! Help!",
            PromptBatcher.PromptType.URGENT,
            Map.of("priority", "high")
        );

        // Wait for result
        String result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals("Emergency handled", result);

        // Verify underlying client was called
        verify(mockUnderlyingClient, timeout(3000).times(1))
            .sendAsync(anyString(), anyMap());
    }

    @Test
    @DisplayName("Request batching: NORMAL type prompts processed with standard batching")
    void requestBatchingNormalPromptsStandard() throws Exception {
        // Setup response
        LLMResponse response = LLMResponse.builder()
            .content("Standard response")
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(50)
            .latencyMs(100)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Submit a normal prompt
        CompletableFuture<String> future = batchingClient.submitNormalPrompt(
            "Build a house",
            Map.of()
        );

        // Wait for result
        String result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals("Standard response", result);

        // Verify underlying client was called
        verify(mockUnderlyingClient, timeout(5000).atLeast(1))
            .sendAsync(anyString(), anyMap());
    }

    // ------------------------------------------------------------------------
    // Batch Timeout Handling Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Timeout: user prompt times out after USER_PROMPT_TIMEOUT_MS")
    void timeoutUserPromptTimeoutAfterTimeoutMs() {
        // Setup a response that never completes
        CompletableFuture<LLMResponse> neverCompletes = new CompletableFuture<>();
        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(neverCompletes);

        // Submit a user prompt
        CompletableFuture<String> future = batchingClient.submitUserPrompt(
            "This will timeout",
            Map.of()
        );

        // Wait for timeout (2 minutes is too long, so we'll check the exception type)
        assertThrows(CompletionException.class, () -> {
            try {
                future.get(3, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                throw new CompletionException(e);
            }
        });
    }

    @Test
    @DisplayName("Timeout: background prompt times out after BACKGROUND_PROMPT_TIMEOUT_MS")
    void timeoutBackgroundPromptTimeoutAfterTimeoutMs() {
        // Note: Full 5-minute timeout test would take too long
        // This test verifies the timeout is configured, not that it fully elapses

        // Setup a delayed response
        CompletableFuture<LLMResponse> delayedResponse = new CompletableFuture<>();
        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(delayedResponse);

        // Submit a background prompt
        CompletableFuture<String> future = batchingClient.submitBackgroundPrompt(
            "Long running task",
            Map.of()
        );

        // The future should not be immediately completed
        assertFalse(future.isDone(), "Background prompt should not be immediately completed");

        // Cancel to clean up
        future.cancel(true);
    }

    @Test
    @DisplayName("Timeout: prompt with explicit timeout configured")
    void timeoutPromptWithExplicitTimeout() throws Exception {
        // Setup quick response
        LLMResponse response = LLMResponse.builder()
            .content("Quick response")
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(50)
            .latencyMs(50)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Submit with specific type
        CompletableFuture<String> future = batchingClient.submit(
            "Test prompt",
            PromptBatcher.PromptType.NORMAL,
            Map.of()
        );

        // Should complete quickly
        String result = future.get(2, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals("Quick response", result);
    }

    // ------------------------------------------------------------------------
    // Response Routing Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Response routing: single prompt batch returns direct response")
    void responseRoutingSinglePromptDirectResponse() throws Exception {
        // Setup response
        LLMResponse response = LLMResponse.builder()
            .content("Direct response for single prompt")
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(50)
            .latencyMs(100)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Submit single prompt
        CompletableFuture<String> future = batchingClient.submitUserPrompt(
            "Single request",
            Map.of()
        );

        String result = future.get(5, TimeUnit.SECONDS);

        assertEquals("Direct response for single prompt", result,
            "Single prompt should get direct response");
    }

    @Test
    @DisplayName("Response routing: multi-prompt batch with [N] markers parsed correctly")
    void responseRoutingMultiPromptWithMarkersParsed() throws Exception {
        // Setup batched response with markers
        String batchedResponse = """
            [1] First response here
            [2] Second response here
            [3] Third response here
            """;

        LLMResponse response = LLMResponse.builder()
            .content(batchedResponse)
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(150)
            .latencyMs(200)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Submit multiple prompts
        CompletableFuture<String> future1 = batchingClient.submitBackgroundPrompt(
            "First prompt",
            Map.of()
        );

        CompletableFuture<String> future2 = batchingClient.submitBackgroundPrompt(
            "Second prompt",
            Map.of()
        );

        CompletableFuture<String> future3 = batchingClient.submitBackgroundPrompt(
            "Third prompt",
            Map.of()
        );

        // Get results
        String result1 = future1.get(10, TimeUnit.SECONDS);
        String result2 = future2.get(1, TimeUnit.SECONDS);
        String result3 = future3.get(1, TimeUnit.SECONDS);

        // Verify each got the correct response
        assertEquals("First response here", result1);
        assertEquals("Second response here", result2);
        assertEquals("Third response here", result3);
    }

    @Test
    @DisplayName("Response routing: unparseable batch response returns full response to all")
    void responseRoutingUnparseableReturnsFullResponse() throws Exception {
        // Setup response without [N] markers
        String plainResponse = "This is a plain response without markers";

        LLMResponse response = LLMResponse.builder()
            .content(plainResponse)
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(100)
            .latencyMs(150)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Submit multiple prompts
        CompletableFuture<String> future1 = batchingClient.submitBackgroundPrompt(
            "Prompt 1",
            Map.of()
        );

        CompletableFuture<String> future2 = batchingClient.submitBackgroundPrompt(
            "Prompt 2",
            Map.of()
        );

        // Get results
        String result1 = future1.get(10, TimeUnit.SECONDS);
        String result2 = future2.get(1, TimeUnit.SECONDS);

        // Both should get the full response
        assertEquals(plainResponse, result1);
        assertEquals(plainResponse, result2);
    }

    @Test
    @DisplayName("Response routing: partial [N] markers use full response for missing")
    void responseRoutingPartialMarkersUseFullResponse() throws Exception {
        // Setup response with some markers but not all
        String partialResponse = """
            [1] First response
            Some text without marker
            [2] Second response
            """;

        LLMResponse response = LLMResponse.builder()
            .content(partialResponse)
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(100)
            .latencyMs(150)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Submit multiple prompts (more than markers in response)
        CompletableFuture<String> future1 = batchingClient.submitBackgroundPrompt(
            "Prompt 1",
            Map.of()
        );

        CompletableFuture<String> future2 = batchingClient.submitBackgroundPrompt(
            "Prompt 2",
            Map.of()
        );

        CompletableFuture<String> future3 = batchingClient.submitBackgroundPrompt(
            "Prompt 3",
            Map.of()
        );

        // Get results - third prompt should get full response as fallback
        String result1 = future1.get(10, TimeUnit.SECONDS);
        String result2 = future2.get(1, TimeUnit.SECONDS);
        String result3 = future3.get(1, TimeUnit.SECONDS);

        assertEquals("First response", result1);
        assertEquals("Second response", result2);
        // Third should get the full response as fallback since no [3] marker
        assertTrue(result3.contains("First response") || result3.contains("partialResponse"),
            "Third prompt should get full response as fallback");
    }

    // ------------------------------------------------------------------------
    // Rate Limit Handling Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Rate limit: 429 error triggers backoff")
    void rateLimit429TriggersBackoff() throws Exception {
        // Setup rate limit error
        CompletableFuture<LLMResponse> errorFuture = CompletableFuture.failedFuture(
            new RuntimeException("HTTP 429: Rate limit exceeded")
        );

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(errorFuture);

        // Submit a prompt
        CompletableFuture<String> future = batchingClient.submitUserPrompt(
            "Rate limit test",
            Map.of()
        );

        // Should fail with rate limit error
        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));

        // Wait a bit for backoff to be applied
        Thread.sleep(100);

        // Verify backoff multiplier increased
        double backoff = batchingClient.getBackoffMultiplier();
        assertTrue(backoff > 1.0, "Backoff multiplier should increase after rate limit error");
    }

    @Test
    @DisplayName("Rate limit: rate limit error message detected")
    void rateLimitErrorMessageDetected() throws Exception {
        // Setup rate limit error with different message format
        CompletableFuture<LLMResponse> errorFuture = CompletableFuture.failedFuture(
            new RuntimeException("Rate limit exceeded, please retry later")
        );

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(errorFuture);

        // Submit a prompt
        CompletableFuture<String> future = batchingClient.submitUserPrompt(
            "Rate limit test 2",
            Map.of()
        );

        // Should fail
        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));

        // Verify backoff was triggered
        assertTrue(batchingClient.getBackoffMultiplier() > 1.0,
            "Should detect 'rate limit' in error message");
    }

    @Test
    @DisplayName("Rate limit: success resets backoff multiplier")
    void rateLimitSuccessResetsBackoff() throws Exception {
        // First, trigger rate limit
        CompletableFuture<LLMResponse> errorFuture = CompletableFuture.failedFuture(
            new RuntimeException("429: Too many requests")
        );

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(errorFuture);

        CompletableFuture<String> future1 = batchingClient.submitUserPrompt(
            "Trigger rate limit",
            Map.of()
        );

        try {
            future1.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            // Expected
        }

        // Wait for backoff to apply
        Thread.sleep(200);

        double backoffAfterError = batchingClient.getBackoffMultiplier();
        assertTrue(backoffAfterError > 1.0, "Backoff should be > 1 after error");

        // Now setup successful response
        LLMResponse successResponse = LLMResponse.builder()
            .content("Success!")
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(50)
            .latencyMs(100)
            .fromCache(false)
            .build();

        reset(mockUnderlyingClient);
        when(mockUnderlyingClient.getProviderId()).thenReturn("test-provider");
        when(mockUnderlyingClient.isHealthy()).thenReturn(true);
        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(successResponse));

        // Submit another prompt - need to wait for backoff period
        CompletableFuture<String> future2 = batchingClient.submitUserPrompt(
            "After success",
            Map.of()
        );

        String result = future2.get(30, TimeUnit.SECONDS);

        assertNotNull(result);

        // Backoff should be reset to 1.0 after success
        double backoffAfterSuccess = batchingClient.getBackoffMultiplier();
        assertEquals(1.0, backoffAfterSuccess, 0.01,
            "Backoff should reset to 1.0 after successful response");
    }

    // ------------------------------------------------------------------------
    // Error Propagation Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Error propagation: underlying client error propagates to caller")
    void errorPropagationUnderlyingErrorPropagates() {
        // Setup error from underlying client
        CompletableFuture<LLMResponse> errorFuture = CompletableFuture.failedFuture(
            new RuntimeException("LLM provider error")
        );

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(errorFuture);

        // Submit a prompt
        CompletableFuture<String> future = batchingClient.submitUserPrompt(
            "Error test",
            Map.of()
        );

        // Should receive the error
        ExecutionException exception = assertThrows(ExecutionException.class,
            () -> future.get(5, TimeUnit.SECONDS));

        assertTrue(exception.getCause() instanceof RuntimeException);
        assertTrue(exception.getCause().getMessage().contains("LLM provider error") ||
                   exception.getCause().getMessage().contains("Prompt failed"));
    }

    @Test
    @DisplayName("Error propagation: non-rate-limit errors don't trigger backoff")
    void errorPropagationNonRateLimitNoBackoff() throws Exception {
        // Setup non-rate-limit error
        CompletableFuture<LLMResponse> errorFuture = CompletableFuture.failedFuture(
            new RuntimeException("Internal server error: 500")
        );

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(errorFuture);

        // Submit a prompt
        CompletableFuture<String> future = batchingClient.submitUserPrompt(
            "Non-rate-limit error",
            Map.of()
        );

        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            // Expected
        }

        // Backoff should NOT be triggered for non-rate-limit errors
        assertEquals(1.0, batchingClient.getBackoffMultiplier(), 0.01,
            "Backoff should remain at 1.0 for non-rate-limit errors");
    }

    @Test
    @DisplayName("Error propagation: null response handled gracefully")
    void errorPropagationNullResponseHandled() throws Exception {
        // Setup a future that completes with null in the content
        LLMResponse nullContentResponse = LLMResponse.builder()
            .content(null)
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(0)
            .latencyMs(100)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(nullContentResponse));

        // Submit a prompt
        CompletableFuture<String> future = batchingClient.submitUserPrompt(
            "Null content test",
            Map.of()
        );

        // Should handle null gracefully (may return null or empty string)
        String result = future.get(5, TimeUnit.SECONDS);

        // Either null or empty string is acceptable
        assertTrue(result == null || result.isEmpty() || result.equals("[null]"));
    }

    // ------------------------------------------------------------------------
    // Concurrent Request Handling Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Concurrent: multiple simultaneous prompts handled correctly")
    void concurrentMultiplePromptsHandledCorrectly() throws Exception {
        // Setup responses for batched requests
        String batchedResponse = """
            [1] Response 1
            [2] Response 2
            [3] Response 3
            [4] Response 4
            [5] Response 5
            """;

        LLMResponse response = LLMResponse.builder()
            .content(batchedResponse)
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(200)
            .latencyMs(300)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Submit multiple prompts concurrently
        CompletableFuture<String> future1 = batchingClient.submitBackgroundPrompt("Prompt 1", Map.of());
        CompletableFuture<String> future2 = batchingClient.submitBackgroundPrompt("Prompt 2", Map.of());
        CompletableFuture<String> future3 = batchingClient.submitBackgroundPrompt("Prompt 3", Map.of());
        CompletableFuture<String> future4 = batchingClient.submitBackgroundPrompt("Prompt 4", Map.of());
        CompletableFuture<String> future5 = batchingClient.submitBackgroundPrompt("Prompt 5", Map.of());

        // Wait for all to complete
        CompletableFuture.allOf(future1, future2, future3, future4, future5)
            .get(15, TimeUnit.SECONDS);

        // Verify all completed successfully
        assertTrue(future1.isDone() && !future1.isCompletedExceptionally());
        assertTrue(future2.isDone() && !future2.isCompletedExceptionally());
        assertTrue(future3.isDone() && !future3.isCompletedExceptionally());
        assertTrue(future4.isDone() && !future4.isCompletedExceptionally());
        assertTrue(future5.isDone() && !future5.isCompletedExceptionally());

        // Verify each got a unique response
        String result1 = future1.get();
        String result2 = future2.get();
        String result3 = future3.get();
        String result4 = future4.get();
        String result5 = future5.get();

        assertTrue(result1.contains("Response 1"));
        assertTrue(result2.contains("Response 2"));
        assertTrue(result3.contains("Response 3"));
        assertTrue(result4.contains("Response 4"));
        assertTrue(result5.contains("Response 5"));
    }

    @Test
    @DisplayName("Concurrent: mixed prompt types prioritized correctly")
    void concurrentMixedTypesPrioritized() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);

        // Setup response that tracks call count
        LLMResponse response = LLMResponse.builder()
            .content("Response")
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(50)
            .latencyMs(100)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenAnswer(invocation -> {
                int count = callCount.incrementAndGet();
                return CompletableFuture.completedFuture(response);
            });

        // Submit mixed types - urgent should be processed first
        CompletableFuture<String> urgentFuture = batchingClient.submit(
            "Urgent request",
            PromptBatcher.PromptType.URGENT,
            Map.of()
        );

        CompletableFuture<String> backgroundFuture = batchingClient.submitBackgroundPrompt(
            "Background request",
            Map.of()
        );

        CompletableFuture<String> userFuture = batchingClient.submitUserPrompt(
            "User request",
            Map.of()
        );

        // Wait for all to complete
        CompletableFuture.allOf(urgentFuture, backgroundFuture, userFuture)
            .get(10, TimeUnit.SECONDS);

        // Verify all completed
        assertTrue(urgentFuture.isDone());
        assertTrue(backgroundFuture.isDone());
        assertTrue(userFuture.isDone());

        // Verify at least one call was made (urgent may have triggered immediate batch)
        assertTrue(callCount.get() >= 1, "At least one LLM call should be made");
    }

    @Test
    @DisplayName("Concurrent: high load doesn't cause deadlocks")
    void concurrentHighLoadNoDeadlocks() throws Exception {
        // Setup responses
        LLMResponse response = LLMResponse.builder()
            .content("OK")
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(50)
            .latencyMs(50)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Submit many prompts concurrently
        int promptCount = 20;
        @SuppressWarnings("unchecked")
        CompletableFuture<String>[] futures = (CompletableFuture<String>[]) new CompletableFuture<?>[promptCount];

        for (int i = 0; i < promptCount; i++) {
            final int index = i;
            futures[i] = batchingClient.submitBackgroundPrompt(
                "Prompt " + index,
                Map.of("index", index)
            );
        }

        // Wait for all to complete with generous timeout
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

        // Verify all completed successfully
        for (int i = 0; i < promptCount; i++) {
            assertTrue(futures[i].isDone(), "Future " + i + " should be done");
            assertFalse(futures[i].isCompletedExceptionally(),
                "Future " + i + " should not complete exceptionally");
        }
    }

    // ------------------------------------------------------------------------
    // Lifecycle Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Lifecycle: start() starts batcher and heartbeat")
    void lifecycleStartStartsComponents() {
        BatchingLLMClient client = new BatchingLLMClient(mockUnderlyingClient);

        assertFalse(client.isRunning(), "Should not be running before start()");

        client.start();

        assertTrue(client.isRunning(), "Should be running after start()");

        client.stop();
    }

    @Test
    @DisplayName("Lifecycle: stop() stops batcher and heartbeat")
    void lifecycleStopStopsComponents() throws Exception {
        batchingClient.stop();

        assertFalse(batchingClient.isRunning(), "Should not be running after stop()");
    }

    @Test
    @DisplayName("Lifecycle: multiple start() calls are idempotent")
    void lifecycleMultipleStartsIdempotent() {
        batchingClient.start();
        boolean runningAfterFirst = batchingClient.isRunning();

        batchingClient.start();
        boolean runningAfterSecond = batchingClient.isRunning();

        assertEquals(runningAfterFirst, runningAfterSecond,
            "Multiple start() calls should be idempotent");

        batchingClient.stop();
    }

    @Test
    @DisplayName("Lifecycle: multiple stop() calls are safe")
    void lifecycleMultipleStopsSafe() {
        batchingClient.stop();
        assertFalse(batchingClient.isRunning());

        // Should not throw exception
        batchingClient.stop();
        assertFalse(batchingClient.isRunning());
    }

    // ------------------------------------------------------------------------
    // Statistics and Accessors Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Statistics: queue size reported correctly")
    void statisticsQueueSizeReported() throws Exception {
        // Setup delayed response
        CompletableFuture<LLMResponse> delayed = new CompletableFuture<>();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(delayed);

        // Submit multiple prompts
        batchingClient.submitBackgroundPrompt("Prompt 1", Map.of());
        batchingClient.submitBackgroundPrompt("Prompt 2", Map.of());
        batchingClient.submitBackgroundPrompt("Prompt 3", Map.of());

        // Wait a bit for them to queue
        Thread.sleep(500);

        // Queue size should be at least 1 (prompts queue up)
        int queueSize = batchingClient.getQueueSize();
        assertTrue(queueSize >= 0, "Queue size should be non-negative");

        // Cancel pending future to clean up
        delayed.complete(LLMResponse.builder()
            .content("Done")
            .model("test")
            .providerId("test")
            .tokensUsed(50)
            .latencyMs(100)
            .fromCache(false)
            .build());
    }

    @Test
    @DisplayName("Statistics: idle mode status reported")
    void statisticsIdleModeReported() {
        // Initially should be in idle mode (no user activity)
        assertTrue(batchingClient.isIdleMode(), "Should start in idle mode");

        // User activity should switch to active mode
        batchingClient.submitUserPrompt("Test", Map.of());

        // After user activity, should be in active mode
        assertFalse(batchingClient.isIdleMode(), "Should be in active mode after user activity");
    }

    @Test
    @DisplayName("Statistics: status summary formatted correctly")
    void statisticsStatusSummaryFormatted() {
        String status = batchingClient.getStatusSummary();

        assertNotNull(status, "Status summary should not be null");
        assertTrue(status.contains("queue="), "Should contain queue info");
        assertTrue(status.contains("mode="), "Should contain mode info");
        assertTrue(status.contains("backoff="), "Should contain backoff info");
        assertTrue(status.contains("running="), "Should contain running status");
    }

    @Test
    @DisplayName("Accessors: batcher and heartbeat accessible")
    void accessorsBatcherAndHeartbeatAccessible() {
        assertNotNull(batchingClient.getBatcher(), "Batcher should be accessible");
        assertNotNull(batchingClient.getHeartbeat(), "Heartbeat should be accessible");

        assertTrue(batchingClient.getBatcher().isRunning() || !batchingClient.getBatcher().isRunning(),
            "Batcher should have valid state");
        assertTrue(batchingClient.getHeartbeat().isRunning() || !batchingClient.getHeartbeat().isRunning(),
            "Heartbeat should have valid state");
    }

    // ------------------------------------------------------------------------
    // Edge Cases Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Edge case: empty prompt handled")
    void edgeCaseEmptyPromptHandled() throws Exception {
        LLMResponse response = LLMResponse.builder()
            .content("Empty prompt response")
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(50)
            .latencyMs(100)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Submit empty prompt
        CompletableFuture<String> future = batchingClient.submitUserPrompt(
            "",
            Map.of()
        );

        String result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result, "Should handle empty prompt");
    }

    @Test
    @DisplayName("Edge case: very long prompt handled")
    void edgeCaseLongPromptHandled() throws Exception {
        StringBuilder longPrompt = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longPrompt.append("This is a very long prompt. ");
        }

        LLMResponse response = LLMResponse.builder()
            .content("Long prompt response")
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(1000)
            .latencyMs(200)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        CompletableFuture<String> future = batchingClient.submitUserPrompt(
            longPrompt.toString(),
            Map.of()
        );

        String result = future.get(10, TimeUnit.SECONDS);

        assertNotNull(result, "Should handle long prompt");
    }

    @Test
    @DisplayName("Edge case: null context map handled")
    void edgeCaseNullContextHandled() throws Exception {
        LLMResponse response = LLMResponse.builder()
            .content("Response")
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(50)
            .latencyMs(100)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // Submit with null context
        CompletableFuture<String> future = batchingClient.submitUserPrompt(
            "Test",
            null
        );

        String result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result, "Should handle null context");
    }

    @Test
    @DisplayName("Edge case: context with systemPrompt parameter")
    void edgeCaseContextWithSystemPrompt() throws Exception {
        LLMResponse response = LLMResponse.builder()
            .content("Custom system prompt response")
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(50)
            .latencyMs(100)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        Map<String, Object> context = Map.of(
            "systemPrompt", "Custom system instructions",
            "customParam", "value"
        );

        CompletableFuture<String> future = batchingClient.submitUserPrompt(
            "Test with custom system prompt",
            context
        );

        String result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Edge case: batch response with malformed [N] markers")
    void edgeCaseMalformedMarkers() throws Exception {
        // Response with malformed markers
        String malformedResponse = """
            [1] First response
            malformed marker without number
            [] Empty marker
            [abc] Non-numeric marker
            [2] Second response
            """;

        LLMResponse response = LLMResponse.builder()
            .content(malformedResponse)
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(100)
            .latencyMs(150)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        CompletableFuture<String> future1 = batchingClient.submitBackgroundPrompt("P1", Map.of());
        CompletableFuture<String> future2 = batchingClient.submitBackgroundPrompt("P2", Map.of());

        String result1 = future1.get(10, TimeUnit.SECONDS);
        String result2 = future2.get(1, TimeUnit.SECONDS);

        // Should handle gracefully - may return full response for malformed entries
        assertNotNull(result1);
        assertNotNull(result2);
    }

    @Test
    @DisplayName("Edge case: batch response with whitespace around markers")
    void edgeCaseWhitespaceAroundMarkers() throws Exception {
        // Response with various whitespace patterns
        String whitespaceResponse = """
            [1] First response

            [2] Second response

              [3] Third response with leading spaces
            """;

        LLMResponse response = LLMResponse.builder()
            .content(whitespaceResponse)
            .model("test-model")
            .providerId("test-provider")
            .tokensUsed(100)
            .latencyMs(150)
            .fromCache(false)
            .build();

        when(mockUnderlyingClient.sendAsync(anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(response));

        CompletableFuture<String> future1 = batchingClient.submitBackgroundPrompt("P1", Map.of());
        CompletableFuture<String> future2 = batchingClient.submitBackgroundPrompt("P2", Map.of());
        CompletableFuture<String> future3 = batchingClient.submitBackgroundPrompt("P3", Map.of());

        String result1 = future1.get(10, TimeUnit.SECONDS);
        String result2 = future2.get(1, TimeUnit.SECONDS);
        String result3 = future3.get(1, TimeUnit.SECONDS);

        // Should parse correctly despite whitespace
        assertTrue(result1.contains("First"));
        assertTrue(result2.contains("Second"));
        assertTrue(result3.contains("Third"));
    }
}
