package com.minewright.script;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Manual verification test for ScriptCache core functionality.
 * This test verifies the implementation works as expected.
 */
class ScriptCacheManualTest {

    @Test
    void testBasicFunctionality() {
        // Create cache
        ScriptCache cache = new ScriptCache();

        // Create test script
        Script testScript = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test-script-1")
                .name("Build House")
                .description("Builds a simple house")
                .build())
            .scriptNode(ScriptNode.builder()
                .type(ScriptNode.NodeType.SEQUENCE)
                .build())
            .build();

        // Store script
        cache.store("build a wooden house", testScript);
        assertEquals(1, cache.size(), "Cache should contain 1 script");

        // Find exact match
        Optional<Script> found = cache.findSimilar("build a wooden house");
        assertTrue(found.isPresent(), "Should find exact match");
        assertEquals("test-script-1", found.get().getId());

        // Find similar match
        Optional<Script> similar = cache.findSimilar("construct a wooden house");
        assertTrue(similar.isPresent(), "Should find semantically similar command");

        // Find different command (should not match)
        Optional<Script> different = cache.findSimilar("attack all enemies");
        assertFalse(different.isPresent(), "Should not match different command");

        // Record success
        cache.recordSuccess("test-script-1");
        cache.recordSuccess("test-script-1");
        cache.recordFailure("test-script-1");

        ScriptCache.CacheStats stats = cache.getStats();
        assertEquals(3, stats.getTotalExecutions());
        assertEquals(2, stats.getTotalSuccesses());

        // Cleanup
        cache.clear();
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
    }

    @Test
    void testSemanticSimilarity() {
        ScriptCache cache = new ScriptCache();

        Script testScript = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("mine-script-1")
                .name("Mine Ores")
                .description("Mines ores in a pattern")
                .build())
            .scriptNode(ScriptNode.builder()
                .type(ScriptNode.NodeType.SEQUENCE)
                .build())
            .build();

        cache.store("mine for diamonds", testScript);

        // Similar commands should match
        assertTrue(cache.findSimilar("mine diamonds").isPresent());
        assertTrue(cache.findSimilar("dig for diamonds").isPresent());
        assertTrue(cache.findSimilar("extract diamond ore").isPresent());

        // Different commands should not match
        assertFalse(cache.findSimilar("build a house").isPresent());
        assertFalse(cache.findSimilar("craft a sword").isPresent());
    }

    @Test
    void testCacheStats() {
        ScriptCache cache = new ScriptCache();

        Script testScript = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("stats-test-1")
                .name("Test")
                .build())
            .scriptNode(ScriptNode.builder()
                .type(ScriptNode.NodeType.SEQUENCE)
                .build())
            .build();

        cache.store("test command", testScript);

        // Generate some hits and misses
        cache.findSimilar("test command");      // Hit
        cache.findSimilar("test commands");     // Hit
        cache.findSimilar("testing command");   // Hit
        cache.findSimilar("different command"); // Miss

        double hitRate = cache.getHitRate();
        assertTrue(hitRate > 0.5, "Hit rate should be > 0.5");
        assertTrue(hitRate < 1.0, "Hit rate should be < 1.0");

        ScriptCache.CacheStats stats = cache.getStats();
        assertEquals(1, stats.getCurrentSize());
        assertTrue(stats.getTotalHits() > 0);
        assertTrue(stats.getTotalMisses() > 0);
    }

    @Test
    void testEviction() {
        ScriptCache smallCache = new ScriptCache(3, 0.75, 60000, 3, 0.4);

        for (int i = 0; i < 5; i++) {
            Script script = Script.builder()
                .metadata(Script.ScriptMetadata.builder()
                    .id("script-" + i)
                    .name("Script " + i)
                    .build())
                .scriptNode(ScriptNode.builder()
                    .type(ScriptNode.NodeType.SEQUENCE)
                    .build())
                .build();
            smallCache.store("command " + i, script);
        }

        // Should have evicted to max size
        assertEquals(3, smallCache.size());
        assertEquals(2, smallCache.getStats().getTotalEvictions());
    }
}
