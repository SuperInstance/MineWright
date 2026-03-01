package com.minewright.script;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ScriptCache.
 */
class ScriptCacheTest {

    private ScriptCache cache;
    private Script testScript;

    @BeforeEach
    void setUp() {
        cache = new ScriptCache(
            10,                    // maxSize
            0.75,                  // minSimilarity
            60000,                 // maxAgeMs (1 minute)
            3,                     // minExecutions
            0.4                    // minSuccessRate
        );

        // Create a test script
        testScript = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test-script-1")
                .name("Build House")
                .description("Builds a simple house")
                .build())
            .scriptNode(createTestNode())
            .build();
    }

    private ScriptNode createTestNode() {
        return ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .build();
    }

    @Test
    void testStoreAndFindSimilar() {
        // Store a script
        cache.store("build a small wooden house", testScript);

        // Find with exact command
        Optional<Script> found = cache.findSimilar("build a small wooden house");
        assertTrue(found.isPresent());
        assertEquals("test-script-1", found.get().getId());

        // Find with similar command
        Optional<Script> similar = cache.findSimilar("construct a wooden house");
        assertTrue(similar.isPresent());
    }

    @Test
    void testFindSimilar_NoMatch() {
        cache.store("build a house", testScript);

        // Different command should not match
        Optional<Script> found = cache.findSimilar("attack all enemies");
        assertFalse(found.isPresent());
    }

    @Test
    void testFindSimilar_BelowThreshold() {
        cache.store("build a house", testScript);

        // Very different command should not meet threshold
        Optional<Script> found = cache.findSimilar("build a house", 0.99);
        assertFalse(found.isPresent());
    }

    @Test
    void testStore_DuplicateScript() {
        cache.store("build a house", testScript);
        assertEquals(1, cache.size());

        // Storing same script again should not duplicate
        cache.store("build a house", testScript);
        assertEquals(1, cache.size());
    }

    @Test
    void testStore_NullCommand() {
        assertThrows(IllegalArgumentException.class, () -> {
            cache.store(null, testScript);
        });
    }

    @Test
    void testStore_EmptyCommand() {
        assertThrows(IllegalArgumentException.class, () -> {
            cache.store("  ", testScript);
        });
    }

    @Test
    void testStore_NullScript() {
        assertThrows(IllegalArgumentException.class, () -> {
            cache.store("build a house", null);
        });
    }

    @Test
    void testRecordSuccessAndFailure() {
        cache.store("build a house", testScript);

        cache.recordSuccess("test-script-1");
        cache.recordSuccess("test-script-1");
        cache.recordFailure("test-script-1");

        ScriptCache.CacheStats stats = cache.getStats();
        assertEquals(3, stats.getTotalExecutions());
        assertEquals(2, stats.getTotalSuccesses());
    }

    @Test
    void testRecordSuccess_UnknownScript() {
        // Should not throw, just log warning
        cache.recordSuccess("unknown-script");
        assertEquals(0, cache.getStats().getTotalExecutions());
    }

    @Test
    void testCleanup_RemovesStaleScripts() throws InterruptedException {
        cache.store("build a house", testScript);

        // Create cache with very short max age
        ScriptCache shortLivedCache = new ScriptCache(
            10, 0.75, 100, 3, 0.4
        );
        shortLivedCache.store("build a house", testScript);

        // Wait for script to become stale
        Thread.sleep(150);

        int removed = shortLivedCache.cleanup();
        assertEquals(1, removed);
        assertEquals(0, shortLivedCache.size());
    }

    @Test
    void testCleanup_RemovesLowPerformingScripts() {
        cache.store("build a house", testScript);

        // Record failures
        cache.recordFailure("test-script-1");
        cache.recordFailure("test-script-1");
        cache.recordFailure("test-script-1");
        cache.recordSuccess("test-script-1");

        // Should be removed: 1 success / 4 executions = 0.25 < 0.4 threshold
        int removed = cache.cleanup();
        assertEquals(1, removed);
        assertEquals(0, cache.size());
    }

    @Test
    void testCleanup_PreservesPerformantScripts() {
        cache.store("build a house", testScript);

        // Record successes
        cache.recordSuccess("test-script-1");
        cache.recordSuccess("test-script-1");
        cache.recordSuccess("test-script-1");
        cache.recordFailure("test-script-1");

        // Should be kept: 3 successes / 4 executions = 0.75 >= 0.4 threshold
        int removed = cache.cleanup();
        assertEquals(0, removed);
        assertEquals(1, cache.size());
    }

    @Test
    void testCleanup_NotEnoughExecutions() {
        cache.store("build a house", testScript);

        // Only 2 executions (below threshold of 3)
        cache.recordFailure("test-script-1");
        cache.recordFailure("test-script-1");

        // Should be kept: not enough executions to judge
        int removed = cache.cleanup();
        assertEquals(0, removed);
        assertEquals(1, cache.size());
    }

    @Test
    void testRemove() {
        cache.store("build a house", testScript);
        assertEquals(1, cache.size());

        boolean removed = cache.remove("test-script-1");
        assertTrue(removed);
        assertEquals(0, cache.size());
    }

    @Test
    void testRemove_NonExistent() {
        assertFalse(cache.remove("non-existent"));
    }

    @Test
    void testRemove_NullScriptId() {
        assertFalse(cache.remove(null));
    }

    @Test
    void testClear() {
        cache.store("build a house", testScript);

        Script script2 = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test-script-2")
                .name("Mine")
                .build())
            .scriptNode(createTestNode())
            .build();
        cache.store("mine ores", script2);

        assertEquals(2, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
    }

    @Test
    void testEviction_LRU() {
        // Fill cache to max size
        for (int i = 0; i < 10; i++) {
            Script script = Script.builder()
                .metadata(Script.ScriptMetadata.builder()
                    .id("script-" + i)
                    .name("Script " + i)
                    .build())
                .scriptNode(createTestNode())
                .build();
            cache.store("command " + i, script);
        }

        assertEquals(10, cache.size());

        // Add one more - should evict LRU
        Script script11 = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("script-10")
                .name("Script 10")
                .build())
            .scriptNode(createTestNode())
            .build();
        cache.store("command 10", script11);

        assertEquals(10, cache.size());
        assertEquals(1, cache.getStats().getTotalEvictions());
    }

    @Test
    void testGetStats() {
        cache.store("build a house", testScript);
        cache.recordSuccess("test-script-1");
        cache.recordSuccess("test-script-1");
        cache.recordFailure("test-script-1");

        ScriptCache.CacheStats stats = cache.getStats();
        assertEquals(1, stats.getCurrentSize());
        assertEquals(10, stats.getMaxSize());
        assertEquals(3, stats.getTotalExecutions());
        assertEquals(2, stats.getTotalSuccesses());
    }

    @Test
    void testGetHitRate() {
        cache.store("build a house", testScript);

        // Two hits
        cache.findSimilar("build a house");
        cache.findSimilar("construct a house");

        // One miss
        cache.findSimilar("attack enemies");

        double hitRate = cache.getHitRate();
        assertEquals(2.0 / 3.0, hitRate, 0.01);
    }

    @Test
    void testGetHitRate_EmptyCache() {
        assertEquals(0.0, cache.getHitRate());
    }

    @Test
    void testSemanticSimilarity() {
        cache.store("build a wooden house", testScript);

        // Semantically similar commands should match
        assertTrue(cache.findSimilar("construct a wooden house").isPresent());
        assertTrue(cache.findSimilar("create a wood house").isPresent());

        // Different commands should not match
        assertFalse(cache.findSimilar("mine for diamonds").isPresent());
        assertFalse(cache.findSimilar("attack zombies").isPresent());
    }

    @Test
    void testGetAllScripts() {
        cache.store("build a house", testScript);

        Script script2 = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test-script-2")
                .name("Mine")
                .build())
            .scriptNode(createTestNode())
            .build();
        cache.store("mine ores", script2);

        var scripts = cache.getAllScripts();
        assertEquals(2, scripts.size());
        assertTrue(scripts.stream().anyMatch(s -> s.getId().equals("test-script-1")));
        assertTrue(scripts.stream().anyMatch(s -> s.getId().equals("test-script-2")));
    }

    @Test
    void testConstructor_InvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ScriptCache(0, 0.75, 60000, 3, 0.4); // maxSize <= 0
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new ScriptCache(10, -0.1, 60000, 3, 0.4); // minSimilarity < 0
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new ScriptCache(10, 1.1, 60000, 3, 0.4); // minSimilarity > 1
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new ScriptCache(10, 0.75, 0, 3, 0.4); // maxAgeMs <= 0
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new ScriptCache(10, 0.75, 60000, -1, 0.4); // minExecutions < 0
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new ScriptCache(10, 0.75, 60000, 3, -0.1); // minSuccessRate < 0
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new ScriptCache(10, 0.75, 60000, 3, 1.1); // minSuccessRate > 1
        });
    }

    @Test
    void testFindSimilar_InvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> {
            cache.findSimilar(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            cache.findSimilar("  ");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            cache.findSimilar("build", -0.1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            cache.findSimilar("build", 1.1);
        });
    }

    @Test
    void testCleanup_ConcurrentSafety() throws InterruptedException {
        cache.store("build a house", testScript);

        // Start cleanup in background
        Thread cleanupThread = new Thread(() -> cache.cleanup());
        cleanupThread.start();

        // Try to start another cleanup - should be skipped
        int removed = cache.cleanup();

        cleanupThread.join();

        // Second cleanup should have been skipped
        assertEquals(0, removed);
    }

    @Test
    void testStatsToString() {
        cache.store("build a house", testScript);

        ScriptCache.CacheStats stats = cache.getStats();
        String statsString = stats.toString();

        assertTrue(statsString.contains("CacheStats"));
        assertTrue(statsString.contains("size="));
        assertTrue(statsString.contains("hits="));
        assertTrue(statsString.contains("hitRate="));
    }
}
