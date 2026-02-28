package com.minewright.llm.cache;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable vector representation for text embeddings.
 *
 * <p>This class provides a simple float array wrapper with similarity metrics
 * for semantic text comparison. Used by the semantic cache to compare prompts
 * without external dependencies like word2vec or BERT.</p>
 *
 * <p><b>Similarity Metrics:</b></p>
 * <ul>
 *   <li><b>Cosine Similarity:</b> Measures angle between vectors (0 to 1, higher is more similar)</li>
 *   <li><b>Euclidean Distance:</b> Measures straight-line distance (lower is more similar)</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is immutable and thread-safe.</p>
 *
 * @since 1.6.0
 */
public class EmbeddingVector {

    private final float[] values;

    /**
     * Creates a new embedding vector from the given values.
     *
     * @param values The vector dimensions (will be copied for immutability)
     * @throws IllegalArgumentException if values is null or empty
     */
    public EmbeddingVector(float[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Embedding values cannot be null or empty");
        }
        this.values = Arrays.copyOf(values, values.length);
    }

    /**
     * Returns the dimensionality of this vector.
     *
     * @return The number of dimensions
     */
    public int dimensions() {
        return values.length;
    }

    /**
     * Returns the value at the specified dimension.
     *
     * @param index The dimension index
     * @return The value at that dimension
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public float get(int index) {
        return values[index];
    }

    /**
     * Calculates the cosine similarity between this vector and another.
     *
     * <p>Cosine similarity measures the cosine of the angle between two vectors.
     * Range: [-1, 1], where 1 means identical direction, 0 means orthogonal,
     * and -1 means opposite direction.</p>
     *
     * <p>For normalized text embeddings, values typically range [0, 1] where
     * higher values indicate greater semantic similarity.</p>
     *
     * @param other The other vector to compare with
     * @return Cosine similarity in range [-1, 1]
     * @throws IllegalArgumentException if vectors have different dimensions
     */
    public double cosineSimilarity(EmbeddingVector other) {
        if (other == null) {
            throw new IllegalArgumentException("Other vector cannot be null");
        }
        if (values.length != other.values.length) {
            throw new IllegalArgumentException(
                "Vector dimension mismatch: " + values.length + " vs " + other.values.length);
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < values.length; i++) {
            dotProduct += values[i] * other.values[i];
            norm1 += values[i] * values[i];
            norm2 += other.values[i] * other.values[i];
        }

        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        if (denominator == 0.0) {
            return 0.0;
        }

        return dotProduct / denominator;
    }

    /**
     * Calculates the Euclidean distance between this vector and another.
     *
     * <p>Euclidean distance is the straight-line distance between two points
     * in vector space. Lower values indicate greater similarity.</p>
     *
     * @param other The other vector to compare with
     * @return Euclidean distance (non-negative)
     * @throws IllegalArgumentException if vectors have different dimensions
     */
    public double euclideanDistance(EmbeddingVector other) {
        if (other == null) {
            throw new IllegalArgumentException("Other vector cannot be null");
        }
        if (values.length != other.values.length) {
            throw new IllegalArgumentException(
                "Vector dimension mismatch: " + values.length + " vs " + other.values.length);
        }

        double sum = 0.0;
        for (int i = 0; i < values.length; i++) {
            double diff = values[i] - other.values[i];
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }

    /**
     * Returns the L2 norm (magnitude) of this vector.
     *
     * @return The magnitude of the vector
     */
    public double norm() {
        double sum = 0.0;
        for (float value : values) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }

    /**
     * Returns a normalized version of this vector (unit length).
     *
     * <p>Normalized vectors are useful for cosine similarity calculations
     * as they simplify the computation to a simple dot product.</p>
     *
     * @return A new normalized vector
     */
    public EmbeddingVector normalize() {
        double norm = norm();
        if (norm == 0.0) {
            return this;
        }

        float[] normalized = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            normalized[i] = (float) (values[i] / norm);
        }
        return new EmbeddingVector(normalized);
    }

    /**
     * Creates a zero vector of the specified dimensionality.
     *
     * @param dimensions The number of dimensions
     * @return A zero vector
     */
    public static EmbeddingVector zero(int dimensions) {
        return new EmbeddingVector(new float[dimensions]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmbeddingVector that = (EmbeddingVector) o;
        return Arrays.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("EmbeddingVector[");
        sb.append("dim=").append(values.length);
        sb.append(", norm=").append(String.format("%.4f", norm()));
        if (values.length <= 5) {
            sb.append(", values=").append(Arrays.toString(values));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Returns a copy of the underlying values array.
     *
     * @return A copy of the vector values
     */
    public float[] toArray() {
        return Arrays.copyOf(values, values.length);
    }
}
