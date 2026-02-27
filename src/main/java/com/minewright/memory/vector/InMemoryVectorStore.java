package com.minewright.memory.vector;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * In-memory vector store supporting cosine similarity search.
 * Thread-safe implementation with persistence to NBT.
 *
 * <p>This store maintains vectors alongside their associated metadata,
 * enabling efficient semantic search through vector similarity.</p>
 *
 * @param <T> The type of data to store with each vector
 */
public class InMemoryVectorStore<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryVectorStore.class);

    /**
     * Default embedding dimension for placeholder vectors.
     */
    public static final int DEFAULT_DIMENSION = 384;

    /**
     * Stored vectors with their associated data.
     */
    private final ConcurrentHashMap<Integer, VectorEntry<T>> vectors;

    /**
     * Counter for generating unique IDs.
     */
    private final AtomicInteger nextId;

    /**
     * The dimension of vectors in this store.
     */
    private final int dimension;

    /**
     * Creates a new vector store with default dimensions.
     */
    public InMemoryVectorStore() {
        this(DEFAULT_DIMENSION);
    }

    /**
     * Creates a new vector store with specified dimensions.
     *
     * @param dimension The dimension of vectors to store
     */
    public InMemoryVectorStore(int dimension) {
        this.dimension = dimension;
        this.vectors = new ConcurrentHashMap<>();
        this.nextId = new AtomicInteger(0);
        LOGGER.info("InMemoryVectorStore initialized with dimension {}", dimension);
    }

    /**
     * Adds a vector with associated data to the store.
     *
     * @param vector The embedding vector
     * @param data The associated data
     * @return The ID assigned to this entry
     * @throws IllegalArgumentException if vector dimension doesn't match
     */
    public int add(float[] vector, T data) {
        if (vector.length != dimension) {
            throw new IllegalArgumentException(
                    String.format("Vector dimension mismatch: expected %d, got %d",
                            dimension, vector.length));
        }

        int id = nextId.getAndIncrement();
        vectors.put(id, new VectorEntry<>(vector, data, id));
        LOGGER.debug("Added vector with ID {}, total vectors: {}", id, vectors.size());
        return id;
    }

    /**
     * Finds the k most similar vectors to the query vector.
     * Uses cosine similarity for ranking.
     *
     * @param queryVector The query vector
     * @param k Number of results to return
     * @return List of search results, sorted by similarity (descending)
     * @throws IllegalArgumentException if vector dimension doesn't match
     */
    public List<VectorSearchResult<T>> search(float[] queryVector, int k) {
        if (queryVector.length != dimension) {
            throw new IllegalArgumentException(
                    String.format("Query vector dimension mismatch: expected %d, got %d",
                            dimension, queryVector.length));
        }

        if (vectors.isEmpty()) {
            return Collections.emptyList();
        }

        // Compute similarity for all vectors
        List<VectorSearchResult<T>> results = vectors.values().stream()
                .map(entry -> {
                    double similarity = cosineSimilarity(queryVector, entry.vector);
                    return new VectorSearchResult<>(entry.data, similarity, entry.id);
                })
                .filter(result -> result.getSimilarity() > 0.0) // Only include results with some similarity
                .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                .limit(k)
                .collect(Collectors.toList());

        LOGGER.debug("Search returned {} results (k={})", results.size(), k);
        return results;
    }

    /**
     * Computes cosine similarity between two vectors.
     *
     * @param a First vector
     * @param b Second vector
     * @return Cosine similarity (-1.0 to 1.0)
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0.0) {
            return 0.0;
        }

        return dotProduct / denominator;
    }

    /**
     * Removes a vector by ID.
     *
     * @param id The ID of the vector to remove
     * @return true if the vector was removed, false if it didn't exist
     */
    public boolean remove(int id) {
        VectorEntry<T> removed = vectors.remove(id);
        if (removed != null) {
            LOGGER.debug("Removed vector with ID {}", id);
            return true;
        }
        return false;
    }

    /**
     * Clears all vectors from the store.
     */
    public void clear() {
        int count = vectors.size();
        vectors.clear();
        nextId.set(0);
        LOGGER.info("Cleared {} vectors from store", count);
    }

    /**
     * Gets the number of vectors in the store.
     */
    public int size() {
        return vectors.size();
    }

    /**
     * Gets the dimension of vectors in this store.
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * Saves the vector store to NBT.
     * Note: Vectors are persisted but generic data must be handled separately.
     *
     * @param tag The compound tag to save to
     */
    public void saveToNBT(CompoundTag tag) {
        tag.putInt("Dimension", dimension);
        tag.putInt("NextId", nextId.get());

        ListTag vectorsList = new ListTag();
        for (VectorEntry<T> entry : vectors.values()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt("Id", entry.id);

            // Save vector as float array
            float[] vector = entry.vector;
            int[] vectorInt = new int[vector.length];
            for (int i = 0; i < vector.length; i++) {
                // Convert float to int for NBT storage (multiply by 1000 for precision)
                vectorInt[i] = Math.round(vector[i] * 1000.0f);
            }
            entryTag.putIntArray("Vector", vectorInt);

            vectorsList.add(entryTag);
        }
        tag.put("Vectors", vectorsList);

        LOGGER.debug("Saved {} vectors to NBT", vectors.size());
    }

    /**
     * Loads the vector store from NBT.
     * Note: This only loads vectors - data must be reconstructed separately.
     *
     * @param tag The compound tag to load from
     * @param dataLoader Function to reconstruct data from ID
     */
    public void loadFromNBT(CompoundTag tag, java.util.function.IntFunction<T> dataLoader) {
        int loadedDimension = tag.getInt("Dimension");
        if (loadedDimension != dimension) {
            LOGGER.warn("Dimension mismatch in NBT: expected {}, got {}",
                    dimension, loadedDimension);
        }

        nextId.set(tag.getInt("NextId"));

        ListTag vectorsList = tag.getList("Vectors", 10); // 10 = CompoundTag type
        vectors.clear();

        for (int i = 0; i < vectorsList.size(); i++) {
            CompoundTag entryTag = vectorsList.getCompound(i);
            int id = entryTag.getInt("Id");

            int[] vectorInt = entryTag.getIntArray("Vector");
            float[] vector = new float[vectorInt.length];
            for (int j = 0; j < vectorInt.length; j++) {
                vector[j] = vectorInt[j] / 1000.0f;
            }

            T data = dataLoader.apply(id);
            if (data != null) {
                vectors.put(id, new VectorEntry<>(vector, data, id));
            }
        }

        LOGGER.info("Loaded {} vectors from NBT", vectors.size());
    }

    /**
     * Internal class representing a stored vector entry.
     */
    private static class VectorEntry<T> {
        final float[] vector;
        final T data;
        final int id;

        VectorEntry(float[] vector, T data, int id) {
            this.vector = vector;
            this.data = data;
            this.id = id;
        }
    }
}
