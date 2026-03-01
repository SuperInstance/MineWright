package com.minewright.memory.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Composite embedding model with automatic fallback support.
 *
 * <p>This implementation chains multiple embedding models together, automatically
 * falling back to secondary models if the primary fails. This is useful for:</p>
 *
 * <ul>
 *   <li><b>Local → Remote fallback:</b> Try local model first, fall back to API if unavailable</li>
 *   <li><b>Multi-Cloud fallback:</b> Try OpenAI, fall back to Cohere if down</li>
 *   <li><b>High availability:</b> Ensure embedding generation always succeeds</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * // Local model with remote fallback
 * LocalEmbeddingModel localModel = new MyLocalModel();
 * EmbeddingModel remoteModel = new OpenAIEmbeddingModel(apiKey);
 *
 * EmbeddingModel composite = new CompositeEmbeddingModel(localModel, remoteModel);
 *
 * // Tries local first, falls back to OpenAI if local fails
 * float[] embedding = composite.embed("Hello world");
 * </pre>
 *
 * <p><b>Fallback Behavior:</b></p>
 * <ul>
 *   <li>Primary model is tried first</li>
 *   <li>If primary throws exception or returns zero vector, secondary is tried</li>
 *   <li>If both fail, throws CompositeEmbeddingException</li>
 *   <li>Successful results from any model are cached per-model</li>
 * </ul>
 *
 * <p><b>Dimension Mismatch Handling:</b></p>
 * <ul>
 *   <li>All models should ideally have the same dimension</li>
 *   <li>If dimensions differ, a warning is logged</li>
 *   <li>The primary model's dimension is reported by getDimension()</li>
 *   <li>For production use, ensure all models have matching dimensions</li>
 * </ul>
 *
 * @since 1.2.0
 */
public class CompositeEmbeddingModel implements EmbeddingModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeEmbeddingModel.class);

    private final EmbeddingModel primary;
    private final EmbeddingModel fallback;
    private final String name;

    /**
     * Creates a composite embedding model with primary and fallback.
     *
     * @param primary Primary embedding model (tried first)
     * @param fallback Fallback embedding model (used if primary fails)
     * @throws IllegalArgumentException if either model is null
     */
    public CompositeEmbeddingModel(EmbeddingModel primary, EmbeddingModel fallback) {
        if (primary == null) {
            throw new IllegalArgumentException("Primary model cannot be null");
        }
        if (fallback == null) {
            throw new IllegalArgumentException("Fallback model cannot be null");
        }

        this.primary = primary;
        this.fallback = fallback;
        this.name = "composite(" + primary.getModelName() + " → " + fallback.getModelName() + ")";

        // Log dimension mismatch warning
        if (primary.getDimension() != fallback.getDimension()) {
            LOGGER.warn("Dimension mismatch between primary ({}) and fallback ({}) models. " +
                    "This may cause issues with vector similarity search.",
                    primary.getDimension(), fallback.getDimension());
        }

        LOGGER.info("CompositeEmbeddingModel initialized: primary={}, fallback={}, dimension={}",
                primary.getModelName(), fallback.getModelName(), primary.getDimension());
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) {
            LOGGER.debug("Empty text input, returning zero vector");
            return createZeroVector();
        }

        // Try primary model first
        try {
            LOGGER.debug("Attempting primary model: {}", primary.getModelName());
            float[] result = primary.embed(text);

            if (isValidEmbedding(result)) {
                LOGGER.debug("Primary model succeeded");
                return result;
            } else {
                LOGGER.warn("Primary model returned invalid embedding, trying fallback");
            }
        } catch (Exception e) {
            LOGGER.warn("Primary model failed: {}, trying fallback. Error: {}",
                    primary.getModelName(), e.getMessage());
        }

        // Try fallback model
        try {
            LOGGER.debug("Attempting fallback model: {}", fallback.getModelName());
            float[] result = fallback.embed(text);

            if (isValidEmbedding(result)) {
                LOGGER.debug("Fallback model succeeded");
                return result;
            } else {
                throw new CompositeEmbeddingException(
                        "Fallback model returned invalid embedding");
            }
        } catch (Exception e) {
            LOGGER.error("Fallback model also failed: {}", fallback.getModelName(), e);
            throw new CompositeEmbeddingException(
                    "All embedding models failed. Primary: " + primary.getModelName() +
                    ", Fallback: " + fallback.getModelName(), e);
        }
    }

    @Override
    public CompletableFuture<float[]> embedAsync(String text) {
        if (text == null || text.isEmpty()) {
            return CompletableFuture.completedFuture(createZeroVector());
        }

        return CompletableFuture.supplyAsync(() -> embed(text));
    }

    @Override
    public float[][] embedBatch(String[] texts) {
        if (texts == null || texts.length == 0) {
            return new float[0][];
        }

        LOGGER.debug("Batch embedding {} texts with composite model", texts.length);

        float[][] embeddings = new float[texts.length][];

        for (int i = 0; i < texts.length; i++) {
            embeddings[i] = embed(texts[i]);
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
    public int getDimension() {
        return primary.getDimension();
    }

    @Override
    public String getModelName() {
        return name;
    }

    @Override
    public boolean isAvailable() {
        // Composite is available if at least one model is available
        boolean primaryAvailable = primary.isAvailable();
        boolean fallbackAvailable = fallback.isAvailable();

        if (primaryAvailable) {
            LOGGER.debug("Primary model available: {}", primary.getModelName());
            return true;
        }

        if (fallbackAvailable) {
            LOGGER.debug("Primary unavailable, but fallback available: {}", fallback.getModelName());
            return true;
        }

        LOGGER.warn("Neither primary nor fallback model is available");
        return false;
    }

    /**
     * Returns the primary embedding model.
     *
     * @return Primary model
     */
    public EmbeddingModel getPrimary() {
        return primary;
    }

    /**
     * Returns the fallback embedding model.
     *
     * @return Fallback model
     */
    public EmbeddingModel getFallback() {
        return fallback;
    }

    /**
     * Checks if an embedding vector is valid (non-zero).
     *
     * @param embedding Embedding vector to check
     * @return true if valid, false if zero vector or null
     */
    private boolean isValidEmbedding(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return false;
        }

        // Check if all zeros (invalid embedding)
        for (float v : embedding) {
            if (v != 0.0f) {
                return true;
            }
        }

        return false;
    }

    /**
     * Creates a zero vector for empty input.
     *
     * @return Zero vector with primary model's dimension
     */
    private float[] createZeroVector() {
        return new float[primary.getDimension()];
    }

    /**
     * Exception thrown when all embedding models in the composite fail.
     */
    public static class CompositeEmbeddingException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public CompositeEmbeddingException(String message) {
            super(message);
        }

        public CompositeEmbeddingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Builder for creating composite models with multiple fallbacks.
     *
     * <p><b>Usage:</b></p>
     * <pre>
     * EmbeddingModel model = CompositeEmbeddingModel.builder()
     *     .primary(localModel)
     *     .fallback(openaiModel)
     *     .fallback(cohereModel)
     *     .build();
     * </pre>
     */
    public static class Builder {
        private EmbeddingModel primary;
        private EmbeddingModel lastFallback;

        /**
         * Sets the primary embedding model.
         *
         * @param model Primary model
         * @return This builder
         */
        public Builder primary(EmbeddingModel model) {
            this.primary = model;
            this.lastFallback = model;
            return this;
        }

        /**
         * Adds a fallback model.
         *
         * <p>Fallbacks are chained in order: primary → first fallback → second fallback → ...</p>
         *
         * @param model Fallback model
         * @return This builder
         */
        public Builder fallback(EmbeddingModel model) {
            if (lastFallback == null) {
                lastFallback = model;
            } else {
                // Chain the new fallback to the last one
                CompositeEmbeddingModel composite = new CompositeEmbeddingModel(lastFallback, model);
                lastFallback = composite;
            }
            return this;
        }

        /**
         * Builds the composite embedding model.
         *
         * @return Composite model with all fallbacks
         * @throws IllegalArgumentException if primary is not set
         */
        public EmbeddingModel build() {
            if (primary == null) {
                throw new IllegalArgumentException("Primary model must be set");
            }
            return lastFallback != null ? lastFallback : primary;
        }
    }

    /**
     * Creates a new builder for composite models.
     *
     * @return New builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
