package com.minewright.llm.cache;

/**
 * Interface for generating text embeddings for semantic similarity comparison.
 *
 * <p>Text embedders convert text strings into vector representations that capture
 * semantic meaning. These embeddings enable the cache to find similar prompts
 * even when they're not identical matches.</p>
 *
 * <p><b>Implementation Options:</b></p>
 * <ul>
 *   <li><b>SimpleTextEmbedder:</b> TF-IDF based, no external dependencies</li>
 *   <li><b>Future:</b> Integration with word2vec, BERT, or sentence transformers</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Implementations must be thread-safe.</p>
 *
 * @since 1.6.0
 */
public interface TextEmbedder {

    /**
     * Generates an embedding vector for the given text.
     *
     * <p>The embedding captures semantic meaning of the text, allowing
     * similarity comparison between different but semantically related prompts.</p>
     *
     * @param text The text to embed (must not be null)
     * @return An embedding vector representing the text
     * @throws IllegalArgumentException if text is null or empty
     */
    EmbeddingVector embed(String text);

    /**
     * Calculates the similarity between two text strings.
     *
     * <p>This is a convenience method that embeds both texts and
     * computes their cosine similarity.</p>
     *
     * @param text1 The first text
     * @param text2 The second text
     * @return Similarity score in range [0, 1], where 1.0 is identical
     * @throws IllegalArgumentException if either text is null or empty
     */
    default double getSimilarity(String text1, String text2) {
        if (text1 == null || text1.isEmpty()) {
            throw new IllegalArgumentException("text1 cannot be null or empty");
        }
        if (text2 == null || text2.isEmpty()) {
            throw new IllegalArgumentException("text2 cannot be null or empty");
        }

        EmbeddingVector vec1 = embed(text1);
        EmbeddingVector vec2 = embed(text2);
        return vec1.cosineSimilarity(vec2);
    }

    /**
     * Returns the dimensionality of embeddings produced by this embedder.
     *
     * <p>All embeddings from this embedder will have the same number
     * of dimensions, allowing vector comparison.</p>
     *
     * @return The number of dimensions in embedding vectors
     */
    int getDimensions();

    /**
     * Returns a human-readable name for this embedder implementation.
     *
     * @return The embedder name (e.g., "TF-IDF", "word2vec", "BERT")
     */
    String getName();
}
