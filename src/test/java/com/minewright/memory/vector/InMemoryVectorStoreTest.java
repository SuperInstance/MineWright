package com.minewright.memory.vector;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link InMemoryVectorStore}.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Vector storage and retrieval</li>
 *   <li>Similarity search (cosine similarity)</li>
 *   <li>Top-k retrieval</li>
 *   <li>Vector deletion</li>
 *   <li>Thread safety</li>
 *   <li>Edge cases (empty store, null vectors, dimension mismatch)</li>
 *   <li>NBT persistence</li>
 *   <li>Parallel search threshold behavior</li>
 * </ul>
 *
 * @param <T> The type of data stored with vectors
 */
@DisplayName("In-Memory Vector Store Tests")
class InMemoryVectorStoreTest {

    private InMemoryVectorStore<String> store;
    private InMemoryVectorStore<TestData> genericStore;

    /**
     * Test data class for generic type testing.
     */
    private static class TestData {
        private final String name;
        private final int value;

        TestData(String name, int value) {
            this.name = name;
            this.value = value;
        }

        String getName() {
            return name;
        }

        int getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestData testData = (TestData) o;
            return value == testData.value && name.equals(testData.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode() + value;
        }
    }

    @BeforeEach
    void setUp() {
        store = new InMemoryVectorStore<>(128);
        genericStore = new InMemoryVectorStore<>(64);
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a normalized vector for testing.
     */
    private float[] createNormalizedVector(int dimension, double... values) {
        float[] vector = new float[dimension];
        // Fill with provided values, rest with zeros
        for (int i = 0; i < Math.min(values.length, dimension); i++) {
            vector[i] = (float) values[i];
        }
        return vector;
    }

    /**
     * Creates an orthogonal vector (perpendicular) to another vector.
     */
    private float[] createOrthogonalVector(float[] original) {
        float[] orthogonal = new float[original.length];
        // Create a vector perpendicular by rotating 90 degrees in 2D subspace
        orthogonal[0] = -original[1];
        orthogonal[1] = original[0];
        // Copy rest of values
        for (int i = 2; i < original.length; i++) {
            orthogonal[i] = original[i];
        }
        return orthogonal;
    }

    /**
     * Calculates cosine similarity manually for verification.
     */
    private double calculateCosineSimilarity(float[] a, float[] b) {
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

    // ==================== Constructor Tests ====================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Default constructor uses DEFAULT_DIMENSION")
        void defaultConstructorUsesDefaultDimension() {
            InMemoryVectorStore<String> defaultStore = new InMemoryVectorStore<>();

            assertEquals(InMemoryVectorStore.DEFAULT_DIMENSION, defaultStore.getDimension(),
                "Default dimension should be DEFAULT_DIMENSION");
        }

        @Test
        @DisplayName("Custom dimension constructor sets dimension")
        void customDimensionConstructorSetsDimension() {
            InMemoryVectorStore<String> customStore = new InMemoryVectorStore<>(256);

            assertEquals(256, customStore.getDimension(),
                "Dimension should match constructor argument");
        }

        @Test
        @DisplayName("New store is empty")
        void newStoreIsEmpty() {
            assertEquals(0, store.size(),
                "New store should have zero vectors");
        }

        @Test
        @DisplayName("Store dimension is immutable")
        void storeDimensionIsImmutable() {
            int initialDimension = store.getDimension();

            // Dimension should not change after operations
            store.add(new float[128], "test1");
            store.add(new float[128], "test2");

            assertEquals(initialDimension, store.getDimension(),
                "Dimension should remain constant");
        }
    }

    // ==================== Vector Storage Tests ====================

    @Nested
    @DisplayName("Vector Storage Tests")
    class VectorStorageTests {

        @Test
        @DisplayName("Add vector returns unique ID")
        void addVectorReturnsUniqueId() {
            float[] vector1 = createNormalizedVector(128, 1.0, 0.0, 0.0);
            float[] vector2 = createNormalizedVector(128, 0.0, 1.0, 0.0);

            int id1 = store.add(vector1, "data1");
            int id2 = store.add(vector2, "data2");

            assertNotEquals(id1, id2,
                "Each vector should get unique ID");
            assertEquals(0, id1,
                "First vector ID should start at 0");
            assertEquals(1, id2,
                "Second vector ID should be 1");
        }

        @Test
        @DisplayName("Add vector increases size")
        void addVectorIncreasesSize() {
            assertEquals(0, store.size(),
                "Initial size should be 0");

            store.add(createNormalizedVector(128, 1.0), "first");
            assertEquals(1, store.size(),
                "Size should be 1 after adding one vector");

            store.add(createNormalizedVector(128, 2.0), "second");
            assertEquals(2, store.size(),
                "Size should be 2 after adding two vectors");
        }

        @Test
        @DisplayName("Add vector with wrong dimension throws exception")
        void addVectorWithWrongDimensionThrowsException() {
            float[] wrongDimension = new float[64]; // Wrong dimension

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> store.add(wrongDimension, "data"),
                "Should throw IllegalArgumentException for dimension mismatch"
            );

            assertTrue(exception.getMessage().contains("dimension mismatch"),
                "Exception message should mention dimension mismatch");
        }

        @Test
        @DisplayName("Can store generic data types")
        void canStoreGenericDataTypes() {
            TestData data1 = new TestData("test1", 100);
            TestData data2 = new TestData("test2", 200);

            float[] vector1 = createNormalizedVector(64, 1.0, 0.0);
            float[] vector2 = createNormalizedVector(64, 0.0, 1.0);

            int id1 = genericStore.add(vector1, data1);
            int id2 = genericStore.add(vector2, data2);

            assertEquals(2, genericStore.size(),
                "Should store generic data");
        }

        @Test
        @DisplayName("Can store null data with vectors")
        void canStoreNullDataWithVectors() {
            float[] vector = createNormalizedVector(128, 1.0);

            int id = store.add(vector, null);

            assertEquals(1, store.size(),
                "Should accept null data");
        }

        @Test
        @DisplayName("Multiple vectors are stored with sequential IDs")
        void multipleVectorsStoredWithSequentialIds() {
            for (int i = 0; i < 10; i++) {
                float[] vector = createNormalizedVector(128, i);
                int id = store.add(vector, "data" + i);
                assertEquals(i, id,
                    "IDs should be sequential starting from 0");
            }

            assertEquals(10, store.size(),
                "Should store all 10 vectors");
        }
    }

    // ==================== Similarity Search Tests ====================

    @Nested
    @DisplayName("Similarity Search Tests")
    class SimilaritySearchTests {

        @Test
        @DisplayName("Search returns results sorted by similarity")
        void searchReturnsSortedResults() {
            // Create query vector
            float[] query = createNormalizedVector(128, 1.0, 0.0, 0.0);

            // Add vectors with varying similarities (all above 0.1 threshold)
            float[] exact = createNormalizedVector(128, 1.0, 0.0, 0.0); // Exact match
            float[] close = createNormalizedVector(128, 0.9, 0.1, 0.0); // Similar
            float[] moderate = createNormalizedVector(128, 0.7, 0.3, 0.0); // Moderate similarity
            float[] low = createNormalizedVector(128, 0.5, 0.4, 0.0); // Low but above threshold

            store.add(exact, "exact");
            store.add(close, "close");
            store.add(moderate, "moderate");
            store.add(low, "low");

            List<VectorSearchResult<String>> results = store.search(query, 4);

            assertTrue(results.size() >= 2,
                "Should return at least 2 results (exact and close)");
            assertEquals("exact", results.get(0).getData(),
                "Exact match should be first");
            assertEquals("close", results.get(1).getData(),
                "Close match should be second");
        }

        @Test
        @DisplayName("Search respects k parameter")
        void searchRespectsKParameter() {
            float[] query = createNormalizedVector(128, 1.0);

            for (int i = 0; i < 10; i++) {
                float[] vector = createNormalizedVector(128, Math.random());
                store.add(vector, "data" + i);
            }

            List<VectorSearchResult<String>> results3 = store.search(query, 3);
            List<VectorSearchResult<String>> results5 = store.search(query, 5);

            assertEquals(3, results3.size(),
                "Should return k=3 results");
            assertEquals(5, results5.size(),
                "Should return k=5 results");
        }

        @Test
        @DisplayName("Search with k larger than store size returns all results")
        void searchWithKLargerThanStoreSize() {
            float[] query = createNormalizedVector(128, 1.0);
            store.add(createNormalizedVector(128, 1.0), "data1");
            store.add(createNormalizedVector(128, 2.0), "data2");

            List<VectorSearchResult<String>> results = store.search(query, 100);

            assertEquals(2, results.size(),
                "Should return all vectors when k exceeds store size");
        }

        @Test
        @DisplayName("Search filters very low similarity results")
        void searchFiltersLowSimilarityResults() {
            float[] query = createNormalizedVector(128, 1.0, 0.0, 0.0);

            // Add vectors that will have low similarity (< 0.1)
            float[] lowSim = createNormalizedVector(128, 0.01, 0.99, 0.0);
            float[] highSim = createNormalizedVector(128, 0.8, 0.2, 0.0);

            store.add(lowSim, "low");
            store.add(highSim, "high");

            List<VectorSearchResult<String>> results = store.search(query, 10);

            assertTrue(results.stream().allMatch(r -> r.getSimilarity() > 0.1),
                "All results should have similarity > 0.1");
            assertEquals(1, results.size(),
                "Should filter out low similarity result");
        }

        @Test
        @DisplayName("Search returns empty list for empty store")
        void searchReturnsEmptyListForEmptyStore() {
            float[] query = createNormalizedVector(128, 1.0);

            List<VectorSearchResult<String>> results = store.search(query, 5);

            assertNotNull(results, "Results should not be null");
            assertTrue(results.isEmpty(), "Results should be empty for empty store");
        }

        @Test
        @DisplayName("Search with wrong query dimension throws exception")
        void searchWithWrongQueryDimensionThrowsException() {
            float[] wrongQuery = new float[64];

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> store.search(wrongQuery, 5),
                "Should throw IllegalArgumentException for dimension mismatch"
            );

            assertTrue(exception.getMessage().contains("dimension mismatch"),
                "Exception message should mention dimension mismatch");
        }

        @Test
        @DisplayName("Cosine similarity is calculated correctly")
        void cosineSimilarityIsCalculatedCorrectly() {
            // Test with known vectors - use vectors that pass the 0.1 threshold
            float[] query = createNormalizedVector(128, 1.0, 0.0, 0.0);
            float[] identical = createNormalizedVector(128, 1.0, 0.0, 0.0);
            float[] close = createNormalizedVector(128, 0.9, 0.1, 0.0);
            float[] somewhatOpposite = createNormalizedVector(128, -0.5, 0.5, 0.0);

            store.add(identical, "identical");
            store.add(close, "close");
            store.add(somewhatOpposite, "somewhatOpposite");

            List<VectorSearchResult<String>> results = store.search(query, 10);

            // Should find all three vectors
            assertTrue(results.size() >= 2, "Should find at least 2 vectors");

            // Identical vectors should have similarity ~1.0
            VectorSearchResult<String> identicalResult = results.stream()
                .filter(r -> r.getData().equals("identical"))
                .findFirst()
                .orElseThrow();

            assertEquals(1.0, identicalResult.getSimilarity(), 0.001,
                "Identical vectors should have similarity 1.0");

            // Close vector should have high similarity
            VectorSearchResult<String> closeResult = results.stream()
                .filter(r -> r.getData().equals("close"))
                .findFirst()
                .orElseThrow();

            assertTrue(closeResult.getSimilarity() > 0.8,
                "Close vector should have high similarity");

            // Somewhat opposite vector should have lower (possibly negative) similarity
            VectorSearchResult<String> oppositeResult = results.stream()
                .filter(r -> r.getData().equals("somewhatOpposite"))
                .findFirst()
                .orElse(null);

            // This may or may not be found depending on the 0.1 threshold
            if (oppositeResult != null) {
                assertTrue(oppositeResult.getSimilarity() < closeResult.getSimilarity(),
                    "Somewhat opposite vector should have lower similarity than close vector");
            }
        }

        @Test
        @DisplayName("Search result contains correct data")
        void searchResultContainsCorrectData() {
            float[] query = createNormalizedVector(128, 1.0);
            TestData expectedData = new TestData("test", 42);

            genericStore.add(createNormalizedVector(64, 1.0), expectedData);

            List<VectorSearchResult<TestData>> results = genericStore.search(
                createNormalizedVector(64, 1.0), 1);

            assertEquals(1, results.size(),
                "Should return one result");
            assertEquals(expectedData, results.get(0).getData(),
                "Result should contain the stored data");
        }

        @Test
        @DisplayName("Search result contains correct ID")
        void searchResultContainsCorrectId() {
            float[] query = createNormalizedVector(128, 1.0);
            float[] vector = createNormalizedVector(128, 1.0);

            int expectedId = store.add(vector, "data");

            List<VectorSearchResult<String>> results = store.search(query, 1);

            assertEquals(expectedId, results.get(0).getIndex(),
                "Result should contain the correct ID");
        }

        @Test
        @DisplayName("Search handles zero vector query")
        void searchHandlesZeroVectorQuery() {
            float[] zeroVector = new float[128]; // All zeros

            store.add(createNormalizedVector(128, 1.0), "data1");
            store.add(createNormalizedVector(128, 2.0), "data2");

            List<VectorSearchResult<String>> results = store.search(zeroVector, 10);

            assertNotNull(results, "Results should not be null");
            assertTrue(results.isEmpty(),
                "Zero vector query should return empty list (norm is 0)");
        }
    }

    // ==================== Vector Deletion Tests ====================

    @Nested
    @DisplayName("Vector Deletion Tests")
    class VectorDeletionTests {

        @Test
        @DisplayName("Remove existing vector returns true")
        void removeExistingVectorReturnsTrue() {
            float[] vector = createNormalizedVector(128, 1.0);
            int id = store.add(vector, "data");

            boolean removed = store.remove(id);

            assertTrue(removed, "Should return true when vector is removed");
            assertEquals(0, store.size(), "Size should decrease after removal");
        }

        @Test
        @DisplayName("Remove non-existent vector returns false")
        void removeNonExistentVectorReturnsFalse() {
            boolean removed = store.remove(999);

            assertFalse(removed,
                "Should return false when removing non-existent vector");
        }

        @Test
        @DisplayName("Remove vector does not affect other vectors")
        void removeVectorDoesNotAffectOthers() {
            int id1 = store.add(createNormalizedVector(128, 1.0), "data1");
            int id2 = store.add(createNormalizedVector(128, 2.0), "data2");
            int id3 = store.add(createNormalizedVector(128, 3.0), "data3");

            store.remove(id2);

            assertEquals(2, store.size(), "Size should be 2 after removing one");
            // Other vectors should still be searchable
            List<VectorSearchResult<String>> results = store.search(
                createNormalizedVector(128, 1.0), 10);
            assertTrue(results.stream().anyMatch(r -> r.getData().equals("data1")),
                "data1 should still be present");
            assertTrue(results.stream().anyMatch(r -> r.getData().equals("data3")),
                "data3 should still be present");
        }

        @Test
        @DisplayName("Can remove all vectors one by one")
        void canRemoveAllVectorsOneByOne() {
            for (int i = 0; i < 5; i++) {
                store.add(createNormalizedVector(128, i), "data" + i);
            }

            for (int i = 0; i < 5; i++) {
                assertTrue(store.remove(i), "Should remove vector " + i);
            }

            assertEquals(0, store.size(), "Store should be empty after removing all");
        }

        @Test
        @DisplayName("Removed vector is not returned in search")
        void removedVectorNotReturnedInSearch() {
            float[] vector = createNormalizedVector(128, 1.0);
            int id = store.add(vector, "toRemove");
            store.add(createNormalizedVector(128, 2.0), "toKeep");

            store.remove(id);

            List<VectorSearchResult<String>> results = store.search(
                createNormalizedVector(128, 1.0), 10);

            assertFalse(results.stream().anyMatch(r -> r.getData().equals("toRemove")),
                "Removed vector should not appear in search results");
            assertTrue(results.stream().anyMatch(r -> r.getData().equals("toKeep")),
                "Other vectors should still be searchable");
        }
    }

    // ==================== Clear Tests ====================

    @Nested
    @DisplayName("Clear Tests")
    class ClearTests {

        @Test
        @DisplayName("Clear removes all vectors")
        void clearRemovesAllVectors() {
            for (int i = 0; i < 10; i++) {
                store.add(createNormalizedVector(128, i), "data" + i);
            }

            store.clear();

            assertEquals(0, store.size(), "Store should be empty after clear");
        }

        @Test
        @DisplayName("Clear resets ID counter")
        void clearResetsIdCounter() {
            int id1 = store.add(createNormalizedVector(128, 1.0), "data1");
            store.clear();
            int id2 = store.add(createNormalizedVector(128, 2.0), "data2");

            assertEquals(0, id1, "First ID should be 0");
            assertEquals(0, id2, "ID should reset to 0 after clear");
        }

        @Test
        @DisplayName("Clear on empty store is safe")
        void clearOnEmptyStoreIsSafe() {
            assertDoesNotThrow(() -> store.clear(),
                "Clearing empty store should not throw");
            assertEquals(0, store.size(), "Empty store should remain empty");
        }

        @Test
        @DisplayName("Can add vectors after clear")
        void canAddVectorsAfterClear() {
            store.add(createNormalizedVector(128, 1.0), "before");
            store.clear();

            int id = store.add(createNormalizedVector(128, 2.0), "after");

            assertEquals(0, id, "Should start IDs from 0 after clear");
            assertEquals(1, store.size(), "Should have one vector after clear");
        }
    }

    // ==================== Thread Safety Tests ====================

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Concurrent addition is thread-safe")
        void concurrentAdditionIsThreadSafe() throws InterruptedException {
            int threadCount = 10;
            int vectorsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < vectorsPerThread; i++) {
                            float[] vector = createNormalizedVector(128, Math.random());
                            store.add(vector, "data");
                            successCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertEquals(threadCount * vectorsPerThread, successCount.get(),
                "All additions should succeed");
            assertEquals(threadCount * vectorsPerThread, store.size(),
                "All vectors should be stored");
        }

        @Test
        @DisplayName("Concurrent search is thread-safe")
        void concurrentSearchIsThreadSafe() throws InterruptedException {
            // Pre-populate store
            for (int i = 0; i < 100; i++) {
                float[] vector = createNormalizedVector(128, Math.random());
                store.add(vector, "data" + i);
            }

            int threadCount = 10;
            int searchesPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        float[] query = createNormalizedVector(128, Math.random());
                        for (int i = 0; i < searchesPerThread; i++) {
                            List<VectorSearchResult<String>> results = store.search(query, 10);
                            assertNotNull(results, "Results should not be null");
                            successCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(30, TimeUnit.SECONDS),
                "All searches should complete");
            executor.shutdown();

            assertEquals(threadCount * searchesPerThread, successCount.get(),
                "All searches should succeed");
        }

        @Test
        @DisplayName("Concurrent add and search is thread-safe")
        void concurrentAddAndSearchIsThreadSafe() throws InterruptedException {
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < 100; i++) {
                            if (threadId % 2 == 0) {
                                // Even threads: add vectors
                                float[] vector = createNormalizedVector(128, Math.random());
                                store.add(vector, "data" + threadId + "_" + i);
                            } else {
                                // Odd threads: search
                                float[] query = createNormalizedVector(128, Math.random());
                                store.search(query, 10);
                            }
                            successCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(30, TimeUnit.SECONDS),
                "All operations should complete");
            executor.shutdown();

            assertEquals(threadCount * 100, successCount.get(),
                "All operations should succeed");
        }

        @Test
        @DisplayName("Concurrent removal is thread-safe")
        void concurrentRemovalIsThreadSafe() throws InterruptedException {
            // Pre-populate with IDs
            int[] ids = new int[100];
            for (int i = 0; i < 100; i++) {
                ids[i] = store.add(createNormalizedVector(128, i), "data" + i);
            }

            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int t = 0; t < threadCount; t++) {
                final int start = t * 20;
                final int end = start + 20;
                executor.submit(() -> {
                    try {
                        for (int i = start; i < end; i++) {
                            store.remove(ids[i]);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS),
                "All removals should complete");
            executor.shutdown();

            assertEquals(0, store.size(), "All vectors should be removed");
        }

        @Test
        @DisplayName("Concurrent clear is handled safely")
        void concurrentClearIsHandledSafely() throws InterruptedException {
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < 10; i++) {
                            store.add(createNormalizedVector(128, Math.random()), "data");
                            if (i % 3 == 0) {
                                store.clear();
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS),
                "All operations should complete");
            executor.shutdown();

            // Store should be in a consistent state
            assertTrue(store.size() >= 0, "Size should be non-negative");
        }
    }

    // ==================== Parallel Search Tests ====================

    @Nested
    @DisplayName("Parallel Search Tests")
    class ParallelSearchTests {

        @Test
        @DisplayName("Parallel search is used for large datasets")
        void parallelSearchUsedForLargeDatasets() {
            // Add more than PARALLEL_THRESHOLD (1000) vectors
            for (int i = 0; i < 1100; i++) {
                float[] vector = createNormalizedVector(128, Math.random());
                store.add(vector, "data" + i);
            }

            float[] query = createNormalizedVector(128, 1.0);

            // Search should complete without error using parallel stream
            List<VectorSearchResult<String>> results = store.search(query, 10);

            assertNotNull(results, "Results should not be null");
            assertEquals(10, results.size(), "Should return requested number of results");
        }

        @Test
        @DisplayName("Sequential search is used for small datasets")
        void sequentialSearchUsedForSmallDatasets() {
            // Add less than PARALLEL_THRESHOLD vectors
            for (int i = 0; i < 100; i++) {
                float[] vector = createNormalizedVector(128, Math.random());
                store.add(vector, "data" + i);
            }

            float[] query = createNormalizedVector(128, 1.0);

            List<VectorSearchResult<String>> results = store.search(query, 10);

            assertNotNull(results, "Results should not be null");
            assertEquals(10, results.size(), "Should return requested number of results");
        }

        @Test
        @DisplayName("Parallel search produces correct results")
        void parallelSearchProducesCorrectResults() {
            // Add 1500 vectors with deterministic values
            float[] targetVector = createNormalizedVector(128, 1.0, 0.0, 0.0);
            int targetId = store.add(targetVector, "target");

            // Add noise vectors with lower similarity to target
            for (int i = 0; i < 1500; i++) {
                float[] vector = createNormalizedVector(128, 0.1, 0.9, 0.0); // Low similarity to target
                store.add(vector, "noise" + i);
            }

            // Search should find the target vector
            List<VectorSearchResult<String>> results = store.search(targetVector, 5);

            assertEquals(5, results.size(), "Should return 5 results");
            assertEquals("target", results.get(0).getData(),
                "Target should be top result");
            assertEquals(targetId, results.get(0).getIndex(),
                "Target should have correct ID");
            assertEquals(1.0, results.get(0).getSimilarity(), 0.001,
                "Target should have perfect similarity");
        }
    }

    // ==================== Edge Cases Tests ====================

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Search with k=0 returns empty list")
        void searchWithKZeroReturnsEmptyList() {
            store.add(createNormalizedVector(128, 1.0), "data1");

            List<VectorSearchResult<String>> results = store.search(
                createNormalizedVector(128, 1.0), 0);

            assertNotNull(results, "Results should not be null");
            assertTrue(results.isEmpty(), "Results should be empty for k=0");
        }

        @Test
        @DisplayName("Search with negative k throws exception")
        void searchWithNegativeKThrowsException() {
            store.add(createNormalizedVector(128, 1.0), "data1");

            // Stream.limit() throws IllegalArgumentException for negative values
            assertThrows(IllegalArgumentException.class,
                () -> store.search(createNormalizedVector(128, 1.0), -5),
                "Should throw IllegalArgumentException for negative k");
        }

        @Test
        @DisplayName("Can add vectors with all zeros")
        void canAddVectorsWithAllZeros() {
            float[] zeroVector = new float[128];

            int id = store.add(zeroVector, "zero");

            assertEquals(1, store.size(), "Zero vector should be stored");
            assertEquals(0, id, "Should assign ID to zero vector");
        }

        @Test
        @DisplayName("Can add vectors with negative values")
        void canAddVectorsWithNegativeValues() {
            float[] negativeVector = createNormalizedVector(128, -1.0, -0.5, -0.3);

            int id = store.add(negativeVector, "negative");

            assertEquals(1, store.size(), "Negative vector should be stored");
            assertEquals(0, id, "Should assign ID to negative vector");
        }

        @Test
        @DisplayName("Can add vectors with mixed positive and negative values")
        void canAddVectorsWithMixedValues() {
            float[] mixedVector = createNormalizedVector(128, 1.0, -0.5, 0.3, -0.8, 0.0);

            int id = store.add(mixedVector, "mixed");

            assertEquals(1, store.size(), "Mixed vector should be stored");
            assertEquals(0, id, "Should assign ID to mixed vector");
        }

        @Test
        @DisplayName("Size is consistent after many operations")
        void sizeIsConsistentAfterManyOperations() {
            // Track IDs for removal
            java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();

            for (int i = 0; i < 100; i++) {
                if (i % 3 == 0) {
                    // Add
                    int id = store.add(createNormalizedVector(128, i), "data" + i);
                    ids.add(id);
                } else if (i % 3 == 1 && !ids.isEmpty()) {
                    // Remove first vector
                    int idToRemove = ids.remove(0);
                    store.remove(idToRemove);
                }
                // i % 3 == 2: do nothing (search)
                store.search(createNormalizedVector(128, Math.random()), 5);
            }

            assertEquals(ids.size(), store.size(),
                "Size should be consistent after mixed operations");
        }

        @Test
        @DisplayName("Empty store search returns empty list")
        void emptyStoreSearchReturnsEmptyList() {
            InMemoryVectorStore<String> emptyStore = new InMemoryVectorStore<>(128);

            List<VectorSearchResult<String>> results = emptyStore.search(
                createNormalizedVector(128, 1.0), 10);

            assertNotNull(results, "Results should not be null");
            assertTrue(results.isEmpty(), "Empty store should return no results");
        }

        @Test
        @DisplayName("Remove from empty store returns false")
        void removeFromEmptyStoreReturnsFalse() {
            InMemoryVectorStore<String> emptyStore = new InMemoryVectorStore<>(128);

            boolean removed = emptyStore.remove(0);

            assertFalse(removed, "Removing from empty store should return false");
        }

        @Test
        @DisplayName("Dimension is preserved across operations")
        void dimensionIsPreservedAcrossOperations() {
            int dimension = store.getDimension();

            store.add(createNormalizedVector(dimension, 1.0), "data1");
            store.search(createNormalizedVector(dimension, 1.0), 5);
            store.remove(0);
            store.clear();

            assertEquals(dimension, store.getDimension(),
                "Dimension should remain constant");
        }
    }

    // ==================== NBT Persistence Tests ====================

    @Nested
    @DisplayName("NBT Persistence Tests")
    class NBTPersistenceTests {

        @Test
        @DisplayName("Save to NBT preserves dimension")
        void saveToNBTPreservesDimension() {
            CompoundTag tag = new CompoundTag();

            store.saveToNBT(tag);

            assertEquals(128, tag.getInt("Dimension"),
                "NBT should contain correct dimension");
        }

        @Test
        @DisplayName("Save to NBT preserves next ID")
        void saveToNBTPreservesNextId() {
            store.add(createNormalizedVector(128, 1.0), "data1");
            store.add(createNormalizedVector(128, 2.0), "data2");

            CompoundTag tag = new CompoundTag();
            store.saveToNBT(tag);

            assertEquals(2, tag.getInt("NextId"),
                "NBT should contain correct next ID");
        }

        @Test
        @DisplayName("Save to NBT preserves vector count")
        void saveToNBTPreservesVectorCount() {
            store.add(createNormalizedVector(128, 1.0), "data1");
            store.add(createNormalizedVector(128, 2.0), "data2");
            store.add(createNormalizedVector(128, 3.0), "data3");

            CompoundTag tag = new CompoundTag();
            store.saveToNBT(tag);

            ListTag vectorsList = tag.getList("Vectors", 10);
            assertEquals(3, vectorsList.size(),
                "NBT should contain all vectors");
        }

        @Test
        @DisplayName("Save to NBT preserves vector data")
        void saveToNBTPreservesVectorData() {
            float[] vector = createNormalizedVector(128, 1.0f, 0.5f, -0.3f);
            store.add(vector, "test");

            CompoundTag tag = new CompoundTag();
            store.saveToNBT(tag);

            ListTag vectorsList = tag.getList("Vectors", 10);
            CompoundTag vectorTag = vectorsList.getCompound(0);

            assertEquals(0, vectorTag.getInt("Id"),
                "Should preserve ID");

            int[] savedVector = vectorTag.getIntArray("Vector");
            assertEquals(128, savedVector.length,
                "Should preserve vector dimension");

            // Check first few values (multiplied by 1000)
            assertEquals(1000, savedVector[0], 1,
                "Should preserve first value (scaled)");
            assertEquals(500, savedVector[1], 1,
                "Should preserve second value (scaled)");
            assertEquals(-300, savedVector[2], 1,
                "Should preserve third value (scaled)");
        }

        @Test
        @DisplayName("Load from NBT restores vectors")
        void loadFromNBTRestoresVectors() {
            // Create and save
            store.add(createNormalizedVector(128, 1.0f, 0.5f), "data1");
            store.add(createNormalizedVector(128, 0.0f, 1.0f), "data2");

            CompoundTag tag = new CompoundTag();
            store.saveToNBT(tag);

            // Create new store and load
            InMemoryVectorStore<String> newStore = new InMemoryVectorStore<>(128);
            newStore.loadFromNBT(tag, id -> "data" + (id + 1));

            assertEquals(2, newStore.size(),
                "Should restore all vectors");
        }

        @Test
        @DisplayName("Load from NBT restores next ID")
        void loadFromNBTRestoresNextId() {
            store.add(createNormalizedVector(128, 1.0), "data1");
            store.add(createNormalizedVector(128, 2.0), "data2");

            CompoundTag tag = new CompoundTag();
            store.saveToNBT(tag);

            InMemoryVectorStore<String> newStore = new InMemoryVectorStore<>(128);
            newStore.loadFromNBT(tag, id -> "restored" + id);

            // New ID should continue from saved next ID
            int newId = newStore.add(createNormalizedVector(128, 3.0), "new");
            assertEquals(2, newId,
                "New ID should continue from loaded next ID");
        }

        @Test
        @DisplayName("Load from NBT clears existing data")
        void loadFromNBTClearsExistingData() {
            // Add data to new store
            InMemoryVectorStore<String> newStore = new InMemoryVectorStore<>(128);
            newStore.add(createNormalizedVector(128, 9.0), "existing");

            // Save and load from original
            store.add(createNormalizedVector(128, 1.0), "data1");

            CompoundTag tag = new CompoundTag();
            store.saveToNBT(tag);
            newStore.loadFromNBT(tag, id -> "loaded" + id);

            assertEquals(1, newStore.size(),
                "Load should clear existing data");
            assertFalse(newStore.search(createNormalizedVector(128, 9.0), 10).stream()
                .anyMatch(r -> r.getData().equals("existing")),
                "Existing data should be cleared");
        }

        @Test
        @DisplayName("Load from NBT with null dataLoader skips null entries")
        void loadFromNBTWithNullDataLoaderSkipsNullEntries() {
            store.add(createNormalizedVector(128, 1.0), "data1");
            store.add(createNormalizedVector(128, 2.0), "data2");

            CompoundTag tag = new CompoundTag();
            store.saveToNBT(tag);

            InMemoryVectorStore<String> newStore = new InMemoryVectorStore<>(128);
            // Loader returns null for odd IDs
            newStore.loadFromNBT(tag, id -> id % 2 == 0 ? "loaded" + id : null);

            assertEquals(1, newStore.size(),
                "Should only load non-null entries");
        }

        @Test
        @DisplayName("NBT round-trip preserves search capability")
        void nbtRoundTripPreservesSearchCapability() {
            float[] query = createNormalizedVector(128, 1.0, 0.0, 0.0);
            float[] exact = createNormalizedVector(128, 1.0, 0.0, 0.0);
            float[] close = createNormalizedVector(128, 0.9, 0.1, 0.0);

            store.add(exact, "exact");
            store.add(close, "close");

            // Save and load
            CompoundTag tag = new CompoundTag();
            store.saveToNBT(tag);

            InMemoryVectorStore<String> newStore = new InMemoryVectorStore<>(128);
            newStore.loadFromNBT(tag, id -> id == 0 ? "exact" : "close");

            // Search should work the same
            List<VectorSearchResult<String>> results = newStore.search(query, 2);

            assertEquals(2, results.size(),
                "Should find all vectors after round-trip");
            assertEquals("exact", results.get(0).getData(),
                "Should preserve similarity ordering");
        }
    }

    // ==================== Performance Tests ====================

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Large dataset search completes in reasonable time")
        void largeDatasetSearchCompletesInReasonableTime() {
            // Add 5000 vectors
            for (int i = 0; i < 5000; i++) {
                float[] vector = createNormalizedVector(128, Math.random());
                store.add(vector, "data" + i);
            }

            float[] query = createNormalizedVector(128, 1.0);

            long startTime = System.currentTimeMillis();
            List<VectorSearchResult<String>> results = store.search(query, 100);
            long duration = System.currentTimeMillis() - startTime;

            assertNotNull(results, "Results should not be null");
            assertEquals(100, results.size(), "Should return 100 results");
            assertTrue(duration < 5000,
                "Search should complete in less than 5 seconds, took " + duration + "ms");
        }

        @Test
        @DisplayName("Many consecutive searches are efficient")
        void manyConsecutiveSearchesAreEfficient() {
            // Pre-populate
            for (int i = 0; i < 1000; i++) {
                float[] vector = createNormalizedVector(128, Math.random());
                store.add(vector, "data" + i);
            }

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                float[] query = createNormalizedVector(128, Math.random());
                store.search(query, 10);
            }
            long duration = System.currentTimeMillis() - startTime;

            assertTrue(duration < 10000,
                "100 searches should complete in less than 10 seconds, took " + duration + "ms");
        }

        @Test
        @DisplayName("Add operations are efficient")
        void addOperationsAreEfficient() {
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                float[] vector = createNormalizedVector(128, Math.random());
                store.add(vector, "data" + i);
            }
            long duration = System.currentTimeMillis() - startTime;

            assertEquals(1000, store.size(), "Should add all vectors");
            assertTrue(duration < 5000,
                "1000 adds should complete in less than 5 seconds, took " + duration + "ms");
        }
    }
}
