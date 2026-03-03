package com.minewright.testutil;

import com.minewright.llm.async.AsyncLLMClient;
import com.minewright.llm.async.LLMException;
import com.minewright.llm.async.LLMResponse;
import com.minewright.llm.async.LLMExecutorService;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Mock LLM client for testing.
 *
 * <p>Provides predictable responses for testing without making actual API calls.</p>
 *
 * @since 1.6.0
 */
public class LLMMockClient implements AsyncLLMClient {

    private final String providerId;
    private final String model;
    private long responseDelayMs = 100;
    private double failureRate = 0.0;
    private String mockResponseContent = """
        {
            "tasks": [
                {
                    "action": "mine",
                    "parameters": {"block": "stone", "quantity": 10}
                }
            ]
        }
        """;

    /**
     * Creates a mock client with the specified provider and model.
     */
    public LLMMockClient(String providerId, String model) {
        this.providerId = providerId;
        this.model = model;
    }

    /**
     * Creates a mock client with default values.
     */
    public LLMMockClient() {
        this("mock-provider", "mock-model");
    }

    /**
     * Sets the delay before responding (in milliseconds).
     */
    public void setResponseDelay(long delayMs) {
        this.responseDelayMs = delayMs;
    }

    /**
     * Sets the rate of failures (0.0 to 1.0).
     */
    public void setFailureRate(double rate) {
        this.failureRate = Math.max(0.0, Math.min(1.0, rate));
    }

    /**
     * Sets the mock response content.
     */
    public void setMockResponse(String response) {
        this.mockResponseContent = response;
    }

    @Override
    public String getProviderId() {
        return providerId;
    }

    @Override
    public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(responseDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (Math.random() < failureRate) {
                throw new LLMException(
                    "Mock failure",
                    LLMException.ErrorType.SERVER_ERROR,
                    providerId,
                    true
                );
            }

            return LLMResponse.builder()
                .content(mockResponseContent)
                .model(model)
                .providerId(providerId)
                .tokensUsed(100 + mockResponseContent.length())
                .latencyMs(responseDelayMs)
                .fromCache(false)
                .build();
        }, LLMExecutorService.getInstance());
    }

    /**
     * Creates a simple mock client with default settings.
     */
    public static LLMMockClient createSimple() {
        return new LLMMockClient();
    }

    /**
     * Creates a mock client that always returns the specified response.
     */
    public static LLMMockClient withResponse(String response) {
        LLMMockClient client = new LLMMockClient();
        client.setMockResponse(response);
        return client;
    }

    /**
     * Creates a mock client that always fails.
     */
    public static LLMMockClient createFailing() {
        LLMMockClient client = new LLMMockClient();
        client.setFailureRate(1.0);
        return client;
    }

    /**
     * Creates a mock client with zero delay for fast tests.
     */
    public static LLMMockClient createFast() {
        LLMMockClient client = new LLMMockClient();
        client.setResponseDelay(0);
        return client;
    }
}
