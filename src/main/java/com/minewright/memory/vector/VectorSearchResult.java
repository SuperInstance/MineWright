package com.minewright.memory.vector;

import java.util.Objects;

/**
 * Represents a search result from vector similarity search.
 *
 * @param <T> The type of the associated data (e.g., EpisodicMemory)
 */
public class VectorSearchResult<T> {
    private final T data;
    private final double similarity;
    private final int index;

    /**
     * Creates a new vector search result.
     *
     * @param data The associated data
     * @param similarity Cosine similarity score (0.0 to 1.0)
     * @param index Index in the vector store
     */
    public VectorSearchResult(T data, double similarity, int index) {
        this.data = data;
        this.similarity = similarity;
        this.index = index;
    }

    /**
     * Gets the associated data.
     */
    public T getData() {
        return data;
    }

    /**
     * Gets the similarity score.
     * Higher values indicate greater similarity (0.0 to 1.0).
     */
    public double getSimilarity() {
        return similarity;
    }

    /**
     * Gets the index in the vector store.
     */
    public int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VectorSearchResult<?> that = (VectorSearchResult<?>) o;
        return Double.compare(that.similarity, similarity) == 0 &&
                index == that.index &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, similarity, index);
    }

    @Override
    public String toString() {
        return String.format("VectorSearchResult{similarity=%.4f, index=%d, data=%s}",
                similarity, index, data);
    }
}
