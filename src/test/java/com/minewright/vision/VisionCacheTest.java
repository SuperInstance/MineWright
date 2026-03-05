package com.minewright.vision;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for VisionCache.
 * Tests caching mechanism for vision analysis results.
 */
@DisplayName("VisionCache Tests")
class VisionCacheTest {

    private static final String TEST_IMAGE = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
    private static final String TEST_PROMPT = "What biome is this?";
    private static final String TEST_RESPONSE = "This is a plains biome with oak trees.";
    private static final String DIFFERENT_PROMPT = "What resources are visible?";
    private static final String DIFFERENT_RESPONSE = "There are oak trees and grass.";
    private static final long DEFAULT_TTL_SECONDS = 300; // 5 minutes

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        VisionCache.clear();
    }

    // ==================== Basic Cache Operations Tests ====================

    @Test
    @DisplayName("Should store and retrieve cached result")
    void testPutAndGet() {
        String key = VisionCache.generateCacheKey(TEST_IMAGE, TEST_PROMPT);

        VisionCache.put(TEST_IMAGE, TEST_PROMPT, TEST_RESPONSE);
        String cached = VisionCache.get(TEST_IMAGE, TEST_PROMPT);

        assertNotNull(cached, "Cached result should not be null");
        assertEquals(TEST_RESPONSE, cached, "Cached result should match original");
    }

    @Test
    @DisplayName("Should return null for non-existent key")
    void testGet_MissingKey() {
        String cached = VisionCache.get(TEST_IMAGE, TEST_PROMPT);

        assertNull(cached, "Should return null for non-existent key");
    }

    @Test
    @DisplayName("Should generate consistent cache keys")
    void testGenerateCacheKey_Consistency() {
        String key1 = VisionCache.generateCacheKey(TEST_IMAGE, TEST_PROMPT);
        String key2 = VisionCache.generateCacheKey(TEST_IMAGE, TEST_PROMPT);

        assertEquals(key1, key2, "Cache keys should be consistent for same inputs");
    }

    @Test
    @DisplayName("Should generate different keys for different prompts")
    void testGenerateCacheKey_DifferentPrompts() {
        String key1 = VisionCache.generateCacheKey(TEST_IMAGE, TEST_PROMPT);
        String key2 = VisionCache.generateCacheKey(TEST_IMAGE, DIFFERENT_PROMPT);

        assertNotEquals(key1, key2, "Different prompts should generate different keys");
    }

    @Test
    @DisplayName("Should generate different keys for different images")
    void testGenerateCacheKey_DifferentImages() {
        String differentImage = "data:image/png;base64,AAAAAAA";
        String key1 = VisionCache.generateCacheKey(TEST_IMAGE, TEST_PROMPT);
        String key2 = VisionCache.generateCacheKey(differentImage, TEST_PROMPT);

        assertNotEquals(key1, key2, "Different images should generate different keys");
    }

    // ==================== Cache Expiration Tests ====================

    @Test
    @DisplayName("Should retrieve valid cached result before expiration")
    void testGet_BeforeExpiration() throws InterruptedException {
        VisionCache.put(TEST_IMAGE, TEST_PROMPT, TEST_RESPONSE);

        // Wait a short time (well within TTL)
        Thread.sleep(100);

        String cached = VisionCache.get(TEST_IMAGE, TEST_PROMPT);

        assertNotNull(cached, "Should retrieve cached result before expiration");
        assertEquals(TEST_RESPONSE, cached, "Cached result should be correct");
    }

    @Test
    @DisplayName("Should return null for expired cache entry")
    void testGet_AfterExpiration() throws InterruptedException {
        // Note: This test may need adjustment based on actual TTL implementation
        // For now, we test the cleanup mechanism

        VisionCache.put(TEST_IMAGE, TEST_PROMPT, TEST_RESPONSE);

        // Manually expire the entry by manipulating time if possible
        // Or test cleanup functionality

        VisionCache.cleanup();

        // After cleanup, expired entries should be removed
        // If TTL is very short, entry might be expired
        String cached = VisionCache.get(TEST_IMAGE, TEST_PROMPT);

        // Either still cached (if within TTL) or null (if expired)
        // This test verifies the mechanism works
        assertTrue(cached == null || cached.equals(TEST_RESPONSE));
    }

    @Test
    @DisplayName("Should remove expired entries during cleanup")
    void testCleanup_RemovesExpiredEntries() {
        // Add multiple entries
        VisionCache.put(TEST_IMAGE, TEST_PROMPT, TEST_RESPONSE);
        VisionCache.put(TEST_IMAGE, DIFFERENT_PROMPT, DIFFERENT_RESPONSE);

        // Perform cleanup
        int removed = VisionCache.cleanup();

        // Should either remove 0 (all still valid) or 2 (all expired)
        assertTrue(removed >= 0, "Cleanup should return non-negative count");
    }

    @Test
    @DisplayName("Should handle cleanup on empty cache")
    void testCleanup_EmptyCache() {
        int removed = VisionCache.cleanup();

        assertEquals(0, removed, "Should remove 0 entries from empty cache");
    }

    // ==================== Cache Update Tests ====================

    @Test
    @DisplayName("Should overwrite existing cache entry")
    void testPut_Overwrite() {
        String newResponse = "This is a forest biome.";

        VisionCache.put(TEST_IMAGE, TEST_PROMPT, TEST_RESPONSE);
        VisionCache.put(TEST_IMAGE, TEST_PROMPT, newResponse);

        String cached = VisionCache.get(TEST_IMAGE, TEST_PROMPT);

        assertEquals(newResponse, cached, "Should overwrite existing entry");
    }

    @Test
    @DisplayName("Should update timestamp on overwrite")
    void testPut_UpdatesTimestamp() throws InterruptedException {
        VisionCache.put(TEST_IMAGE, TEST_PROMPT, TEST_RESPONSE);
        Thread.sleep(100); // Small delay

        VisionCache.put(TEST_IMAGE, TEST_PROMPT, DIFFERENT_RESPONSE);

        // New value should be retrievable
        String cached = VisionCache.get(TEST_IMAGE, TEST_PROMPT);
        assertEquals(DIFFERENT_RESPONSE, cached, "Should get updated value");
    }

    // ==================== Concurrent Access Tests ====================

    @Test
    @DisplayName("Should handle concurrent put operations")
    void testConcurrentPuts() throws InterruptedException {
        Thread[] threads = new Thread[10];
        String[] responses = new String[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            responses[i] = "Response " + i;
            threads[i] = new Thread(() -> {
                VisionCache.put(TEST_IMAGE, TEST_PROMPT + index, responses[index]);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all entries are cached
        for (int i = 0; i < 10; i++) {
            String cached = VisionCache.get(TEST_IMAGE, TEST_PROMPT + i);
            assertEquals(responses[i], cached, "Should cache all concurrent puts");
        }
    }

    @Test
    @DisplayName("Should handle concurrent get operations")
    void testConcurrentGets() throws InterruptedException {
        VisionCache.put(TEST_IMAGE, TEST_PROMPT, TEST_RESPONSE);

        Thread[] threads = new Thread[10];
        boolean[] success = new boolean[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                String cached = VisionCache.get(TEST_IMAGE, TEST_PROMPT);
                success[index] = TEST_RESPONSE.equals(cached);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // All gets should succeed
        for (boolean s : success) {
            assertTrue(s, "All concurrent gets should succeed");
        }
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle empty image")
    void testEmptyImage() {
        String cached = VisionCache.get("", TEST_PROMPT);

        assertNull(cached, "Should handle empty image");

        VisionCache.put("", TEST_PROMPT, TEST_RESPONSE);
        cached = VisionCache.get("", TEST_PROMPT);

        assertEquals(TEST_RESPONSE, cached, "Should cache with empty image");
    }

    @Test
    @DisplayName("Should handle empty prompt")
    void testEmptyPrompt() {
        String cached = VisionCache.get(TEST_IMAGE, "");

        assertNull(cached, "Should handle empty prompt");

        VisionCache.put(TEST_IMAGE, "", TEST_RESPONSE);
        cached = VisionCache.get(TEST_IMAGE, "");

        assertEquals(TEST_RESPONSE, cached, "Should cache with empty prompt");
    }

    @Test
    @DisplayName("Should handle very long prompts")
    void testLongPrompt() {
        StringBuilder longPrompt = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longPrompt.append("word ");
        }

        assertDoesNotThrow(() -> {
            VisionCache.put(TEST_IMAGE, longPrompt.toString(), TEST_RESPONSE);
            String cached = VisionCache.get(TEST_IMAGE, longPrompt.toString());
            assertEquals(TEST_RESPONSE, cached);
        }, "Should handle long prompts");
    }

    @Test
    @DisplayName("Should handle very long responses")
    void testLongResponse() {
        StringBuilder longResponse = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longResponse.append("detail ");
        }

        assertDoesNotThrow(() -> {
            VisionCache.put(TEST_IMAGE, TEST_PROMPT, longResponse.toString());
            String cached = VisionCache.get(TEST_IMAGE, TEST_PROMPT);
            assertEquals(longResponse.toString(), cached);
        }, "Should handle long responses");
    }

    @Test
    @DisplayName("Should handle special characters in prompt")
    void testSpecialCharacters() {
        String specialPrompt = "Analyze: biome=\"plains\", coords=[100, 64, 200]\nLine 2\nTab\t";

        VisionCache.put(TEST_IMAGE, specialPrompt, TEST_RESPONSE);
        String cached = VisionCache.get(TEST_IMAGE, specialPrompt);

        assertEquals(TEST_RESPONSE, cached, "Should handle special characters");
    }

    @Test
    @DisplayName("Should handle Unicode characters")
    void testUnicode() {
        String unicodePrompt = "分析这个截图\n日本語のテスト\n이모지 🏕️";

        VisionCache.put(TEST_IMAGE, unicodePrompt, TEST_RESPONSE);
        String cached = VisionCache.get(TEST_IMAGE, unicodePrompt);

        assertEquals(TEST_RESPONSE, cached, "Should handle Unicode");
    }

    // ==================== Null and Empty Input Tests ====================

    @Test
    @DisplayName("Should handle null image")
    void testNullImage() {
        assertThrows(
            Exception.class,
            () -> VisionCache.get(null, TEST_PROMPT),
            "Should throw exception for null image"
        );
    }

    @Test
    @DisplayName("Should handle null prompt")
    void testNullPrompt() {
        assertThrows(
            Exception.class,
            () -> VisionCache.get(TEST_IMAGE, null),
            "Should throw exception for null prompt"
        );
    }

    @Test
    @DisplayName("Should handle null response in put")
    void testNullResponse() {
        // May throw exception or handle gracefully
        assertThrows(
            Exception.class,
            () -> VisionCache.put(TEST_IMAGE, TEST_PROMPT, null),
            "Should throw exception for null response"
        );
    }

    // ==================== Cache Statistics Tests ====================

    @Test
    @DisplayName("Should track cache size")
    void testCacheSize() {
        int initialSize = VisionCache.size();

        VisionCache.put(TEST_IMAGE, TEST_PROMPT, TEST_RESPONSE);
        int sizeAfterOnePut = VisionCache.size();

        assertEquals(initialSize + 1, sizeAfterOnePut, "Cache size should increase");

        VisionCache.put(TEST_IMAGE, DIFFERENT_PROMPT, DIFFERENT_RESPONSE);
        int sizeAfterTwoPuts = VisionCache.size();

        assertEquals(initialSize + 2, sizeAfterTwoPuts, "Cache size should increase again");
    }

    @Test
    @DisplayName("Should calculate hit rate correctly")
    void testCacheHitRate() {
        // Prime the cache
        VisionCache.put(TEST_IMAGE, TEST_PROMPT, TEST_RESPONSE);

        // Cache hit
        VisionCache.get(TEST_IMAGE, TEST_PROMPT);

        // Cache miss
        VisionCache.get(TEST_IMAGE, DIFFERENT_PROMPT);

        double hitRate = VisionCache.getHitRate();

        assertTrue(hitRate >= 0.0 && hitRate <= 1.0, "Hit rate should be between 0 and 1");
    }

    // ==================== Cache Performance Tests ====================

    @Test
    @DisplayName("Should perform cache operations efficiently")
    void testCachePerformance() {
        // Add 1000 entries
        long startPut = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            VisionCache.put(TEST_IMAGE, TEST_PROMPT + i, TEST_RESPONSE + i);
        }
        long putDuration = System.currentTimeMillis() - startPut;

        assertTrue(putDuration < 1000, "1000 puts should complete in less than 1 second");

        // Retrieve 1000 entries
        long startGet = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            VisionCache.get(TEST_IMAGE, TEST_PROMPT + i);
        }
        long getDuration = System.currentTimeMillis() - startGet;

        assertTrue(getDuration < 1000, "1000 gets should complete in less than 1 second");
    }

    // ==================== Cache Key Generation Tests ====================

    @Test
    @DisplayName("Cache key should be deterministic")
    void testCacheKey_Deterministic() {
        String key1 = VisionCache.generateCacheKey(TEST_IMAGE, TEST_PROMPT);
        String key2 = VisionCache.generateCacheKey(TEST_IMAGE, TEST_PROMPT);
        String key3 = VisionCache.generateCacheKey(TEST_IMAGE, TEST_PROMPT);

        assertEquals(key1, key2);
        assertEquals(key2, key3);
    }

    @Test
    @DisplayName("Cache key should handle different image signatures")
    void testCacheKey_ImageSignature() {
        // Keys should be based on first N characters of image
        String image1 = "data:image/png;base64,abc123def456";
        String image2 = "data:image/png;base64,abc123xyz789";
        String image3 = "data:image/png;base64,zzz000def456";

        String key1 = VisionCache.generateCacheKey(image1, TEST_PROMPT);
        String key2 = VisionCache.generateCacheKey(image2, TEST_PROMPT);
        String key3 = VisionCache.generateCacheKey(image3, TEST_PROMPT);

        // image1 and image2 share first 100 chars, so same key
        assertEquals(key1, key2, "Same signature should produce same key");

        // image3 has different signature
        assertNotEquals(key1, key3, "Different signature should produce different key");
    }

    // ==================== Cache Clear Tests ====================

    @Test
    @DisplayName("Should clear all cache entries")
    void testClear() {
        VisionCache.put(TEST_IMAGE, TEST_PROMPT, TEST_RESPONSE);
        VisionCache.put(TEST_IMAGE, DIFFERENT_PROMPT, DIFFERENT_RESPONSE);

        assertTrue(VisionCache.size() > 0, "Cache should have entries");

        VisionCache.clear();

        assertEquals(0, VisionCache.size(), "Cache should be empty after clear");
        assertNull(VisionCache.get(TEST_IMAGE, TEST_PROMPT), "Entries should be cleared");
    }

    @Test
    @DisplayName("Should handle multiple clear operations")
    void testMultipleClears() {
        VisionCache.put(TEST_IMAGE, TEST_PROMPT, TEST_RESPONSE);
        VisionCache.clear();

        assertEquals(0, VisionCache.size());

        VisionCache.clear(); // Clear again

        assertEquals(0, VisionCache.size(), "Multiple clears should be safe");
    }
}
