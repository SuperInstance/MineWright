package com.minewright.memory.embedding;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.core.IntervalFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OpenAI text embedding model implementation with caching and resilience patterns.
 *
 * <p>Uses OpenAI's text-embedding-3-small model to generate high-quality semantic
 * embeddings. This implementation includes:</p>
 *
 * <ul>
 *   <li><b>Local Caching:</b> LRU cache with TTL to reduce API calls</li>
 *   <li><b>Batch Support:</b> Process multiple texts in a single API call</li>
 *   <li><b>Resilience Patterns:</b> Circuit breaker, retry, and rate limiting</li>
 *   <li><b>Async Operations:</b> Non-blocking embedding generation</li>
 * </ul>
 *
 * <p><b>Model Details:</b></p>
 * <ul>
 *   <li>Model: text-embedding-3-small</li>
 *   <li>Dimension: 1536</li>
 *   <li>Max Input: 8191 tokens per text</li>
 *   <li>Batch Size: Up to 2048 texts per request</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * OpenAIEmbeddingModel model = new OpenAIEmbeddingModel(apiKey);
 *
 * // Synchronous embedding
 * float[] embedding = model.embed("Hello world");
 *
 * // Asynchronous embedding
 * model.embedAsync("Hello world")
 *     .thenApply(vec -> processVector(vec))
 *     .exceptionally(error -> handleError(error));
 *
 * // Batch embedding
 * float[][] embeddings = model.embedBatch(new String[]{"text1", "text2"});
 * </pre>
 *
 * @since 1.2.0
 */
public class OpenAIEmbeddingModel implements EmbeddingModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIEmbeddingModel.class);

    /**
     * Default OpenAI embedding API endpoint.
     */
    private static final String DEFAULT_API_URL = "https://api.openai.com/v1/embeddings";

    /**
     * Default model to use for embeddings.
     */
    private static final String DEFAULT_MODEL = "text-embedding-3-small";

    /**
     * Dimension for text-embedding-3-small.
     */
    private static final int EMBEDDING_DIMENSION = 1536;

    /**
     * Maximum batch size for OpenAI embeddings API.
     */
    private static final int MAX_BATCH_SIZE = 2048;

    /**
     * Maximum texts per batch (conservative limit).
     */
    private static final int DEFAULT_BATCH_SIZE = 100;

    /**
     * Cache TTL in milliseconds (1 hour).
     */
    private static final long CACHE_TTL_MS = 60 * 60 * 1000;

    /**
     * Maximum cache size.
     */
    private static final int MAX_CACHE_SIZE = 1000;

    /**
     * HTTP request timeout in seconds.
     */
    private static final int REQUEST_TIMEOUT_SECONDS = 30;

    /**
     * Maximum retry attempts.
     */
    private static final int MAX_RETRIES = 3;

    /**
     * Initial retry backoff in milliseconds.
     */
    private static final long INITIAL_BACKOFF_MS = 1000;

    private final HttpClient httpClient;
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final int batchSize;

    /**
     * LRU cache for embeddings.
     */
    private final ConcurrentHashMap<Integer, CacheEntry> cache;
    private final java.util.concurrent.ConcurrentLinkedDeque<Integer> accessOrder;
    private final Object cacheLock = new Object();

    /**
     * Resilience patterns.
     */
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    /**
     * Metrics.
     */
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong apiCalls = new AtomicLong(0);
    private final AtomicLong tokensUsed = new AtomicLong(0);

    /**
     * Creates an OpenAI embedding model with default configuration.
     *
     * @param apiKey OpenAI API key
     * @throws IllegalArgumentException if apiKey is null or empty
     */
    public OpenAIEmbeddingModel(String apiKey) {
        this(apiKey, DEFAULT_API_URL, DEFAULT_MODEL, DEFAULT_BATCH_SIZE);
    }

    /**
     * Creates an OpenAI embedding model with custom configuration.
     *
     * @param apiKey OpenAI API key
     * @param apiUrl API endpoint URL
     * @param model Model name (e.g., "text-embedding-3-small", "text-embedding-3-large")
     * @param batchSize Maximum batch size for batch embedding
     * @throws IllegalArgumentException if apiKey is null or empty
     */
    public OpenAIEmbeddingModel(String apiKey, String apiUrl, String model, int batchSize) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("OpenAI API key cannot be null or empty");
        }

        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.batchSize = Math.min(batchSize, MAX_BATCH_SIZE);

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.cache = new ConcurrentHashMap<>();
        this.accessOrder = new java.util.concurrent.ConcurrentLinkedDeque<>();

        // Initialize circuit breaker
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordExceptions(IOException.class, TimeoutException.class)
                .build();

        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.of(cbConfig);
        this.circuitBreaker = cbRegistry.circuitBreaker("openai-embeddings");

        // Initialize retry
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(MAX_RETRIES)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(INITIAL_BACKOFF_MS, 2))
                .retryOnException(e -> e instanceof IOException || e instanceof TimeoutException)
                .build();

        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        this.retry = retryRegistry.retry("openai-embeddings");

        // Register event listeners
        registerEventListeners();

        LOGGER.info("OpenAIEmbeddingModel initialized (model: {}, dimension: {}, batchSize: {})",
                this.model, EMBEDDING_DIMENSION, this.batchSize);
    }

    /**
     * Registers event listeners for circuit breaker and retry.
     */
    private void registerEventListeners() {
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> LOGGER.warn("Circuit breaker state: {} -> {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));

        circuitBreaker.getEventPublisher()
                .onError(event -> LOGGER.debug("Circuit breaker error: {} (duration: {}ms)",
                        event.getThrowable().getClass().getSimpleName(),
                        event.getElapsedDuration().toMillis()));

        retry.getEventPublisher()
                .onRetry(event -> LOGGER.warn("Retry attempt {} of {} after {}ms",
                        event.getNumberOfRetryAttempts(),
                        MAX_RETRIES,
                        event.getWaitInterval().toMillis()));
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) {
            return createZeroVector();
        }

        // Check cache
        int key = Objects.hash(text);
        CacheEntry entry = cache.get(key);

        if (entry != null && !entry.isExpired()) {
            cacheHits.incrementAndGet();
            updateAccessOrder(key);
            LOGGER.debug("Cache hit for text (hash: {})", key);
            return entry.embedding;
        }

        cacheMisses.incrementAndGet();

        // Generate embedding via API
        float[] embedding = fetchEmbedding(text);

        // Cache the result
        cacheEmbedding(key, embedding);

        return embedding;
    }

    @Override
    public CompletableFuture<float[]> embedAsync(String text) {
        if (text == null || text.isEmpty()) {
            return CompletableFuture.completedFuture(createZeroVector());
        }

        return CompletableFuture.supplyAsync(() -> embed(text));
    }

    @Override
    public int getDimension() {
        return EMBEDDING_DIMENSION;
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public float[][] embedBatch(String[] texts) {
        if (texts == null || texts.length == 0) {
            return new float[0][];
        }

        // Process in batches to respect API limits
        float[][] embeddings = new float[texts.length][];

        for (int i = 0; i < texts.length; i += batchSize) {
            int batchEnd = Math.min(i + batchSize, texts.length);
            int batchLength = batchEnd - i;
            String[] batch = new String[batchLength];
            System.arraycopy(texts, i, batch, 0, batchLength);

            float[][] batchEmbeddings = fetchBatchEmbeddings(batch);
            System.arraycopy(batchEmbeddings, 0, embeddings, i, batchLength);
        }

        return embeddings;
    }

    @Override
    public CompletableFuture<float[][]> embedBatchAsync(String[] texts) {
        if (texts == null || texts.length == 0) {
            return CompletableFuture.completedFuture(new float[0][]);
        }

        return CompletableFuture.supplyAsync(() -> embedBatch(texts));
    }

    @Override
    public boolean isAvailable() {
        return circuitBreaker.getState() != CircuitBreaker.State.OPEN;
    }

    /**
     * Fetches an embedding from the OpenAI API with retry and circuit breaker.
     *
     * @param text Input text
     * @return Embedding vector
     */
    private float[] fetchEmbedding(String text) {
        try {
            // Create supplier for the API call
            java.util.function.Supplier<float[]> apiSupplier = () -> {
                try {
                    return callEmbeddingAPI(text);
                } catch (IOException | InterruptedException e) {
                    throw new EmbeddingException("API call failed", e);
                }
            };

            // Apply circuit breaker
            java.util.function.Supplier<float[]> cbSupplier =
                    CircuitBreaker.decorateSupplier(circuitBreaker, apiSupplier);

            // Apply retry
            java.util.function.Supplier<float[]> retrySupplier =
                    Retry.decorateSupplier(retry, cbSupplier);

            return retrySupplier.get();
        } catch (Exception e) {
            LOGGER.error("Failed to fetch embedding after retries: {}", e.getMessage(), e);
            throw new EmbeddingException("Failed to fetch embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches batch embeddings from the OpenAI API.
     *
     * @param texts Input texts
     * @return Array of embedding vectors
     */
    private float[][] fetchBatchEmbeddings(String[] texts) {
        try {
            // Create supplier for the batch API call
            java.util.function.Supplier<float[][]> apiSupplier = () -> {
                try {
                    return callBatchEmbeddingAPI(texts);
                } catch (IOException | InterruptedException e) {
                    throw new EmbeddingException("Batch API call failed", e);
                }
            };

            // Apply circuit breaker
            java.util.function.Supplier<float[][]> cbSupplier =
                    CircuitBreaker.decorateSupplier(circuitBreaker, apiSupplier);

            // Apply retry
            java.util.function.Supplier<float[][]> retrySupplier =
                    Retry.decorateSupplier(retry, cbSupplier);

            return retrySupplier.get();
        } catch (Exception e) {
            LOGGER.error("Failed to fetch batch embeddings after retries: {}", e.getMessage(), e);
            throw new EmbeddingException("Failed to fetch batch embeddings: " + e.getMessage(), e);
        }
    }

    /**
     * Calls the OpenAI embeddings API for a single text.
     *
     * @param text Input text
     * @return Embedding vector
     * @throws IOException if the API call fails
     * @throws InterruptedException if the thread is interrupted
     */
    private float[] callEmbeddingAPI(String text) throws IOException, InterruptedException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("input", text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        apiCalls.incrementAndGet();

        if (response.statusCode() == 200) {
            return parseEmbeddingResponse(response.body());
        } else {
            throw new IOException("OpenAI API error: HTTP " + response.statusCode() +
                    " - " + response.body());
        }
    }

    /**
     * Calls the OpenAI embeddings API for multiple texts.
     *
     * @param texts Input texts
     * @return Array of embedding vectors
     * @throws IOException if the API call fails
     * @throws InterruptedException if the thread is interrupted
     */
    private float[][] callBatchEmbeddingAPI(String[] texts) throws IOException, InterruptedException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);

        JsonArray inputs = new JsonArray();
        for (String text : texts) {
            inputs.add(text);
        }
        requestBody.add("input", inputs);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        apiCalls.incrementAndGet();

        if (response.statusCode() == 200) {
            return parseBatchEmbeddingResponse(response.body(), texts.length);
        } else {
            throw new IOException("OpenAI API error: HTTP " + response.statusCode() +
                    " - " + response.body());
        }
    }

    /**
     * Parses the embedding response from OpenAI API.
     *
     * @param responseBody JSON response body
     * @return Embedding vector
     * @throws IOException if parsing fails
     */
    private float[] parseEmbeddingResponse(String responseBody) throws IOException {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (!json.has("data") || json.getAsJsonArray("data").isEmpty()) {
                throw new IOException("Invalid response: missing 'data' field");
            }

            JsonObject data = json.getAsJsonArray("data").get(0).getAsJsonObject();
            JsonArray embeddingArray = data.getAsJsonArray("embedding");

            float[] embedding = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = embeddingArray.get(i).getAsFloat();
            }

            // Track token usage if available
            if (json.has("usage")) {
                JsonObject usage = json.getAsJsonObject("usage");
                int totalTokens = usage.get("total_tokens").getAsInt();
                tokensUsed.addAndGet(totalTokens);
            }

            LOGGER.debug("Received embedding (dimension: {})", embedding.length);
            return embedding;

        } catch (Exception e) {
            throw new IOException("Failed to parse embedding response: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the batch embedding response from OpenAI API.
     *
     * @param responseBody JSON response body
     * @param expectedCount Expected number of embeddings
     * @return Array of embedding vectors
     * @throws IOException if parsing fails
     */
    private float[][] parseBatchEmbeddingResponse(String responseBody, int expectedCount)
            throws IOException {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (!json.has("data")) {
                throw new IOException("Invalid response: missing 'data' field");
            }

            JsonArray dataArray = json.getAsJsonArray("data");
            if (dataArray.size() != expectedCount) {
                LOGGER.warn("Expected {} embeddings, got {}", expectedCount, dataArray.size());
            }

            float[][] embeddings = new float[dataArray.size()][];

            for (int i = 0; i < dataArray.size(); i++) {
                JsonObject data = dataArray.get(i).getAsJsonObject();
                JsonArray embeddingArray = data.getAsJsonArray("embedding");

                embeddings[i] = new float[embeddingArray.size()];
                for (int j = 0; j < embeddingArray.size(); j++) {
                    embeddings[i][j] = embeddingArray.get(j).getAsFloat();
                }
            }

            // Track token usage if available
            if (json.has("usage")) {
                JsonObject usage = json.getAsJsonObject("usage");
                int totalTokens = usage.get("total_tokens").getAsInt();
                tokensUsed.addAndGet(totalTokens);
            }

            LOGGER.debug("Received {} embeddings (dimension: {})", embeddings.length,
                    embeddings.length > 0 ? embeddings[0].length : 0);

            return embeddings;

        } catch (Exception e) {
            throw new IOException("Failed to parse batch embedding response: " + e.getMessage(), e);
        }
    }

    /**
     * Caches an embedding with LRU eviction.
     *
     * @param key Cache key
     * @param embedding Embedding vector
     */
    private void cacheEmbedding(int key, float[] embedding) {
        synchronized (cacheLock) {
            // Evict oldest entries if cache is full
            while (cache.size() >= MAX_CACHE_SIZE) {
                Integer oldest = accessOrder.pollFirst();
                if (oldest != null) {
                    cache.remove(oldest);
                }
            }

            cache.put(key, new CacheEntry(embedding));
            accessOrder.addLast(key);
        }
    }

    /**
     * Updates access order for LRU cache.
     *
     * @param key Cache key
     */
    private void updateAccessOrder(int key) {
        synchronized (cacheLock) {
            accessOrder.remove(key);
            accessOrder.addLast(key);
        }
    }

    /**
     * Creates a zero vector for empty input.
     *
     * @return Zero vector
     */
    private float[] createZeroVector() {
        return new float[EMBEDDING_DIMENSION];
    }

    /**
     * Returns cache statistics.
     *
     * @return Cache statistics
     */
    public CacheStats getCacheStats() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total : 0.0;

        return new CacheStats(cache.size(), hits, misses, hitRate);
    }

    /**
     * Returns API usage statistics.
     *
     * @return API usage statistics
     */
    public ApiStats getApiStats() {
        return new ApiStats(apiCalls.get(), tokensUsed.get());
    }

    /**
     * Clears the embedding cache.
     */
    public void clearCache() {
        synchronized (cacheLock) {
            long sizeBefore = cache.size();
            cache.clear();
            accessOrder.clear();
            LOGGER.info("Cleared {} embeddings from cache", sizeBefore);
        }
    }

    /**
     * Resets the circuit breaker to CLOSED state.
     */
    public void resetCircuitBreaker() {
        circuitBreaker.reset();
        LOGGER.info("Circuit breaker reset to CLOSED");
    }

    /**
     * Internal cache entry with timestamp.
     */
    private static class CacheEntry {
        final float[] embedding;
        final long timestamp;

        CacheEntry(float[] embedding) {
            this.embedding = embedding;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    /**
     * Cache statistics.
     */
    public static class CacheStats {
        public final int size;
        public final long hits;
        public final long misses;
        public final double hitRate;

        public CacheStats(int size, long hits, long misses, double hitRate) {
            this.size = size;
            this.hits = hits;
            this.misses = misses;
            this.hitRate = hitRate;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{size=%d, hits=%d, misses=%d, hitRate=%.2f%%}",
                    size, hits, misses, hitRate * 100);
        }
    }

    /**
     * API usage statistics.
     */
    public static class ApiStats {
        public final long apiCalls;
        public final long tokensUsed;

        public ApiStats(long apiCalls, long tokensUsed) {
            this.apiCalls = apiCalls;
            this.tokensUsed = tokensUsed;
        }

        @Override
        public String toString() {
            return String.format("ApiStats{apiCalls=%d, tokensUsed=%d}", apiCalls, tokensUsed);
        }
    }

    /**
     * Exception thrown when embedding generation fails.
     */
    public static class EmbeddingException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public EmbeddingException(String message) {
            super(message);
        }

        public EmbeddingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
