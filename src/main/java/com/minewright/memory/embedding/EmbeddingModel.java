package com.minewright.memory.embedding;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for text embedding models.
 *
 * <p>Embedding models convert text into fixed-size vector representations
 * that capture semantic meaning. These vectors can be used for similarity
 * search, clustering, and other semantic operations.</p>
 *
 * <p>Implementations may include:</p>
 * <ul>
 *   <li>Remote API embeddings (OpenAI, Cohere, etc.)</li>
 *   <li>Local models (Sentence-BERT, etc.)</li>
 *   <li>Lightweight in-memory models</li>
 * </ul>
 */
public interface EmbeddingModel {

    /**
     * Generates an embedding vector for the given text.
     *
     * @param text The text to embed
     * @return A float array representing the text embedding
     */
    float[] embed(String text);

    /**
     * Asynchronously generates an embedding vector for the given text.
     *
     * @param text The text to embed
     * @return A CompletableFuture that completes with the embedding vector
     */
    default CompletableFuture<float[]> embedAsync(String text) {
        return CompletableFuture.supplyAsync(() -> embed(text));
    }

    /**
     * Gets the dimension of embeddings produced by this model.
     *
     * @return The embedding dimension
     */
    int getDimension();

    /**
     * Gets the name of this embedding model.
     *
     * @return The model name
     */
    String getModelName();

    /**
     * Checks if this model is available and ready to use.
     *
     * @return true if the model is available, false otherwise
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * Generates embeddings for multiple texts in batch.
     * Default implementation processes texts sequentially.
     *
     * @param texts The texts to embed
     * @return An array of embedding vectors
     */
    default float[][] embedBatch(String[] texts) {
        float[][] embeddings = new float[texts.length][];
        for (int i = 0; i < texts.length; i++) {
            embeddings[i] = embed(texts[i]);
        }
        return embeddings;
    }

    /**
     * Asynchronously generates embeddings for multiple texts in batch.
     *
     * @param texts The texts to embed
     * @return A CompletableFuture that completes with the embedding vectors
     */
    default CompletableFuture<float[][]> embedBatchAsync(String[] texts) {
        return CompletableFuture.supplyAsync(() -> embedBatch(texts));
    }
}
