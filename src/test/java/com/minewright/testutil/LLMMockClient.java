package com.minewright.testutil;

import com.minewright.llm.async.*;

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

    private final String model;
    private final LLMProvider provider;
    private long responseDelayMs = 100;
    private double failureRate = 0.0;
    private String mockResponse = """
        {
            "tasks": [
                {
                    "action": "mine",
                    "parameters": {"block": "stone", "quantity": 10}
                }
            ]
        }
        """;

    public LLMMockClient(String model, LLMProvider provider) {
        this.model = model;
        this.provider = provider;
    }

    public LLMMockClient() {
        this("mock-model", LLMProvider.OPENAI);
    }

    public void setResponseDelay(long delayMs) {
        this.responseDelayMs = delayMs;
    }

    public void setFailureRate(double rate) {
        this.failureRate = Math.max(0.0, Math.min(1.0, rate));
    }

    public void setMockResponse(String response) {
        this.mockResponse = response;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public LLMProvider getProvider() {
        return provider;
    }

    @Override
    public CompletableFuture<LLMResponse> chatAsync(LLMRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(responseDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (Math.random() < failureRate) {
                throw new LLMException("Mock failure");
            }

            return new LLMResponse(
                mockResponse,
                request.getMessages().get(request.getMessages().size() - 1).content(),
                Map.of("model", model, "provider", provider.name()),
                100 + mockResponse.length()
            );
        }, LLMExecutorService.getInstance());
    }

    @Override
    public CompletableFuture<LLMResponse> chatAsync(LLMRequest request, LLMCache cache) {
        return chatAsync(request);
    }

    @Override
    public void shutdown() {
        // No-op for mock
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    public static LLMMockClient createSimple() {
        return new LLMMockClient("gpt-3.5-turbo", LLMProvider.OPENAI);
    }

    public static LLMMockClient createComplex() {
        return new LLMMockClient("gpt-4", LLMProvider.OPENAI);
    }

    public static LLMMockClient createFast() {
        LLMMockClient client = new LLMMockClient("fast-model", LLMProvider.GROQ);
        client.setResponseDelay(50);
        return client;
    }

    public static LLMMockClient createUnreliable(double failureRate) {
        LLMMockClient client = new LLMMockClient("unreliable-model", LLMProvider.OPENAI);
        client.setFailureRate(failureRate);
        return client;
    }
}
