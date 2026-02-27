package com.minewright.memory.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Placeholder embedding model for testing and development.
 *
 * <p>This implementation generates deterministic pseudo-random vectors
 * based on text content. While not semantically meaningful, it provides
 * a working implementation for testing the vector search infrastructure.</p>
 *
 * <p><b>IMPORTANT:</b> This is NOT suitable for production use.
 * Replace with a real embedding model (e.g., Sentence-BERT, OpenAI embeddings)
 * for actual semantic search functionality.</p>
 *
 * <p>Future real embedding model options for Java:</p>
 * <ul>
 *   <li>Deep Java Library (DJL) - with BERT/Sentence-BERT models</li>
 *   <li>ONNX Runtime - with pre-trained embedding models</li>
 *   <li>Apache OpenNLP - with sentence embeddings</li>
 *   <li>Remote APIs (OpenAI, Cohere) via HTTP client</li>
 * </ul>
 */
public class PlaceholderEmbeddingModel implements EmbeddingModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceholderEmbeddingModel.class);

    private final int dimension;
    private final String modelName;
    private final ConcurrentHashMap<String, float[]> cache;

    /**
     * Random number generator for deterministic pseudo-random vectors.
     */
    private final Random random;

    /**
     * Creates a placeholder embedding model with default dimensions (384).
     */
    public PlaceholderEmbeddingModel() {
        this(384, "placeholder-embedding-v1");
    }

    /**
     * Creates a placeholder embedding model with specified dimensions.
     *
     * @param dimension The embedding dimension
     * @param modelName The model name for identification
     */
    public PlaceholderEmbeddingModel(int dimension, String modelName) {
        this.dimension = dimension;
        this.modelName = modelName;
        this.cache = new ConcurrentHashMap<>();
        this.random = new Random(42); // Fixed seed for deterministic behavior

        LOGGER.warn("Initialized PlaceholderEmbeddingModel - NOT suitable for production use");
        LOGGER.info("Replace with real embedding model for actual semantic search");
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) {
            return createZeroVector();
        }

        // Check cache for consistent embeddings
        return cache.computeIfAbsent(text, this::generateEmbedding);
    }

    /**
     * Generates a deterministic pseudo-random embedding from text.
     * Uses hash codes to seed the random generator for consistency.
     *
     * @param text The input text
     * @return A pseudo-random vector
     */
    private float[] generateEmbedding(String text) {
        float[] vector = new float[dimension];

        // Use text hash as seed for deterministic results
        int seed = text.hashCode();
        Random textRandom = new Random(seed);

        // Generate random values between -1 and 1
        for (int i = 0; i < dimension; i++) {
            vector[i] = (textRandom.nextFloat() * 2.0f) - 1.0f;
        }

        // Normalize the vector
        normalize(vector);

        return vector;
    }

    /**
     * Normalizes a vector to unit length (L2 normalization).
     *
     * @param vector The vector to normalize (modified in place)
     */
    private void normalize(float[] vector) {
        float norm = 0.0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        if (norm > 0.0f) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }

    /**
     * Creates a zero vector.
     *
     * @return A vector of all zeros
     */
    private float[] createZeroVector() {
        return new float[dimension];
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * Clears the embedding cache.
     */
    public void clearCache() {
        cache.clear();
        LOGGER.debug("Embedding cache cleared");
    }

    /**
     * Gets the cache size for monitoring.
     *
     * @return Number of cached embeddings
     */
    public int getCacheSize() {
        return cache.size();
    }
}
