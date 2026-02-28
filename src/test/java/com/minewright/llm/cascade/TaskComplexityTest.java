package com.minewright.llm.cascade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TaskComplexity}.
 *
 * Tests cover:
 * <ul>
 *   <li>Complexity category and description properties</li>
 *   <li>Cache hit rate expectations</li>
 *   <li>Cacheability checks</li>
 *   <li>Latency requirements</li>
 *   <li>Reasoning requirements</li>
 *   <li>String representation</li>
 * </ul>
 *
 * @since 1.6.0
 */
@DisplayName("Task Complexity Tests")
class TaskComplexityTest {

    // ------------------------------------------------------------------------
    // Property Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("TRIVIAL complexity properties")
    void trivialComplexityProperties() {
        TaskComplexity complexity = TaskComplexity.TRIVIAL;

        assertEquals("single-action", complexity.getCategory());
        assertEquals("well-known pattern", complexity.getDescription());
        assertEquals(0.7, complexity.getExpectedCacheHitRate(), 0.01);
    }

    @Test
    @DisplayName("SIMPLE complexity properties")
    void simpleComplexityProperties() {
        TaskComplexity complexity = TaskComplexity.SIMPLE;

        assertEquals("few-actions", complexity.getCategory());
        assertEquals("straightforward", complexity.getDescription());
        assertEquals(0.5, complexity.getExpectedCacheHitRate(), 0.01);
    }

    @Test
    @DisplayName("MODERATE complexity properties")
    void moderateComplexityProperties() {
        TaskComplexity complexity = TaskComplexity.MODERATE;

        assertEquals("multi-step", complexity.getCategory());
        assertEquals("reasoning-needed", complexity.getDescription());
        assertEquals(0.2, complexity.getExpectedCacheHitRate(), 0.01);
    }

    @Test
    @DisplayName("COMPLEX complexity properties")
    void complexComplexityProperties() {
        TaskComplexity complexity = TaskComplexity.COMPLEX;

        assertEquals("coordinated", complexity.getCategory());
        assertEquals("multi-agent", complexity.getDescription());
        assertEquals(0.05, complexity.getExpectedCacheHitRate(), 0.01);
    }

    @Test
    @DisplayName("NOVEL complexity properties")
    void novelComplexityProperties() {
        TaskComplexity complexity = TaskComplexity.NOVEL;

        assertEquals("unknown", complexity.getCategory());
        assertEquals("first-seen", complexity.getDescription());
        assertEquals(0.0, complexity.getExpectedCacheHitRate(), 0.01);
    }

    // ------------------------------------------------------------------------
    // Cacheability Tests
    // ------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(value = TaskComplexity.class, names = {"TRIVIAL", "SIMPLE", "MODERATE", "COMPLEX"})
    @DisplayName("Most complexity levels are cacheable")
    void cacheableComplexities(TaskComplexity complexity) {
        assertTrue(complexity.isCacheable(),
            complexity.name() + " should be cacheable");
    }

    @Test
    @DisplayName("NOVEL complexity is not cacheable")
    void novelNotCacheable() {
        assertFalse(TaskComplexity.NOVEL.isCacheable(),
            "NOVEL complexity should not be cacheable");
    }

    // ------------------------------------------------------------------------
    // Latency Requirement Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("TRIVIAL complexity requires low latency")
    void trivialRequiresLowLatency() {
        assertTrue(TaskComplexity.TRIVIAL.requiresLowLatency(),
            "TRIVIAL should require low latency");
    }

    @Test
    @DisplayName("SIMPLE complexity requires low latency")
    void simpleRequiresLowLatency() {
        assertTrue(TaskComplexity.SIMPLE.requiresLowLatency(),
            "SIMPLE should require low latency");
    }

    @ParameterizedTest
    @EnumSource(value = TaskComplexity.class, names = {"MODERATE", "COMPLEX", "NOVEL"})
    @DisplayName("Higher complexity levels do not require low latency")
    void higherComplexitiesNoLowLatency(TaskComplexity complexity) {
        assertFalse(complexity.requiresLowLatency(),
            complexity.name() + " should not require low latency");
    }

    // ------------------------------------------------------------------------
    // Reasoning Requirement Tests
    // ------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(value = TaskComplexity.class, names = {"COMPLEX", "NOVEL"})
    @DisplayName("COMPLEX and NOVEL require full reasoning")
    void requiresFullReasoning(TaskComplexity complexity) {
        assertTrue(complexity.requiresFullReasoning(),
            complexity.name() + " should require full reasoning");
    }

    @ParameterizedTest
    @EnumSource(value = TaskComplexity.class, names = {"TRIVIAL", "SIMPLE", "MODERATE"})
    @DisplayName("Lower complexity levels do not require full reasoning")
    void noFullReasoning(TaskComplexity complexity) {
        assertFalse(complexity.requiresFullReasoning(),
            complexity.name() + " should not require full reasoning");
    }

    // ------------------------------------------------------------------------
    // Cache Hit Rate Progression Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Cache hit rate decreases with complexity")
    void cacheHitRateDecreasesWithComplexity() {
        assertTrue(TaskComplexity.TRIVIAL.getExpectedCacheHitRate() >
                   TaskComplexity.SIMPLE.getExpectedCacheHitRate());
        assertTrue(TaskComplexity.SIMPLE.getExpectedCacheHitRate() >
                   TaskComplexity.MODERATE.getExpectedCacheHitRate());
        assertTrue(TaskComplexity.MODERATE.getExpectedCacheHitRate() >
                   TaskComplexity.COMPLEX.getExpectedCacheHitRate());
        assertTrue(TaskComplexity.COMPLEX.getExpectedCacheHitRate() >
                   TaskComplexity.NOVEL.getExpectedCacheHitRate());
    }

    @Test
    @DisplayName("Cache hit rate: TRIVIAL is 70%")
    void trivialCacheHitRate() {
        assertEquals(0.7, TaskComplexity.TRIVIAL.getExpectedCacheHitRate(), 0.001);
    }

    @Test
    @DisplayName("Cache hit rate: SIMPLE is 50%")
    void simpleCacheHitRate() {
        assertEquals(0.5, TaskComplexity.SIMPLE.getExpectedCacheHitRate(), 0.001);
    }

    @Test
    @DisplayName("Cache hit rate: MODERATE is 20%")
    void moderateCacheHitRate() {
        assertEquals(0.2, TaskComplexity.MODERATE.getExpectedCacheHitRate(), 0.001);
    }

    @Test
    @DisplayName("Cache hit rate: COMPLEX is 5%")
    void complexCacheHitRate() {
        assertEquals(0.05, TaskComplexity.COMPLEX.getExpectedCacheHitRate(), 0.001);
    }

    @Test
    @DisplayName("Cache hit rate: NOVEL is 0%")
    void novelCacheHitRate() {
        assertEquals(0.0, TaskComplexity.NOVEL.getExpectedCacheHitRate(), 0.001);
    }

    // ------------------------------------------------------------------------
    // String Representation Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("toString: contains complexity name, description, and cache hit rate")
    void toStringFormat() {
        String trivialStr = TaskComplexity.TRIVIAL.toString();
        String simpleStr = TaskComplexity.SIMPLE.toString();

        assertTrue(trivialStr.contains("TRIVIAL"));
        assertTrue(trivialStr.contains("well-known pattern"));
        assertTrue(trivialStr.contains("70%"));

        assertTrue(simpleStr.contains("SIMPLE"));
        assertTrue(simpleStr.contains("straightforward"));
        assertTrue(simpleStr.contains("50%"));
    }

    // ------------------------------------------------------------------------
    // Enum Completeness Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("All enum values are present")
    void allEnumValuesPresent() {
        TaskComplexity[] values = TaskComplexity.values();

        assertEquals(5, values.length, "Should have 5 complexity levels");
        assertTrue(java.util.Arrays.asList(values).contains(TaskComplexity.TRIVIAL));
        assertTrue(java.util.Arrays.asList(values).contains(TaskComplexity.SIMPLE));
        assertTrue(java.util.Arrays.asList(values).contains(TaskComplexity.MODERATE));
        assertTrue(java.util.Arrays.asList(values).contains(TaskComplexity.COMPLEX));
        assertTrue(java.util.Arrays.asList(values).contains(TaskComplexity.NOVEL));
    }

    @Test
    @DisplayName("valueOf: works for all enum names")
    void valueOfAllEnumNames() {
        assertEquals(TaskComplexity.TRIVIAL, TaskComplexity.valueOf("TRIVIAL"));
        assertEquals(TaskComplexity.SIMPLE, TaskComplexity.valueOf("SIMPLE"));
        assertEquals(TaskComplexity.MODERATE, TaskComplexity.valueOf("MODERATE"));
        assertEquals(TaskComplexity.COMPLEX, TaskComplexity.valueOf("COMPLEX"));
        assertEquals(TaskComplexity.NOVEL, TaskComplexity.valueOf("NOVEL"));
    }

    // ------------------------------------------------------------------------
    // Use Case Validation Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Use case: TRIVIAL examples match expected characteristics")
    void trivialUseCaseValidation() {
        TaskComplexity complexity = TaskComplexity.TRIVIAL;

        assertTrue(complexity.isCacheable());
        assertTrue(complexity.requiresLowLatency());
        assertFalse(complexity.requiresFullReasoning());
        assertTrue(complexity.getExpectedCacheHitRate() > 0.5);
    }

    @Test
    @DisplayName("Use case: SIMPLE examples match expected characteristics")
    void simpleUseCaseValidation() {
        TaskComplexity complexity = TaskComplexity.SIMPLE;

        assertTrue(complexity.isCacheable());
        assertTrue(complexity.requiresLowLatency());
        assertFalse(complexity.requiresFullReasoning());
        assertTrue(complexity.getExpectedCacheHitRate() >= 0.3);
    }

    @Test
    @DisplayName("Use case: MODERATE examples match expected characteristics")
    void moderateUseCaseValidation() {
        TaskComplexity complexity = TaskComplexity.MODERATE;

        assertTrue(complexity.isCacheable());
        assertFalse(complexity.requiresLowLatency());
        assertFalse(complexity.requiresFullReasoning());
        assertTrue(complexity.getExpectedCacheHitRate() >= 0.1);
    }

    @Test
    @DisplayName("Use case: COMPLEX examples match expected characteristics")
    void complexUseCaseValidation() {
        TaskComplexity complexity = TaskComplexity.COMPLEX;

        assertTrue(complexity.isCacheable());
        assertFalse(complexity.requiresLowLatency());
        assertTrue(complexity.requiresFullReasoning());
        assertTrue(complexity.getExpectedCacheHitRate() < 0.1);
    }

    @Test
    @DisplayName("Use case: NOVEL examples match expected characteristics")
    void novelUseCaseValidation() {
        TaskComplexity complexity = TaskComplexity.NOVEL;

        assertFalse(complexity.isCacheable());
        assertFalse(complexity.requiresLowLatency());
        assertTrue(complexity.requiresFullReasoning());
        assertEquals(0.0, complexity.getExpectedCacheHitRate());
    }
}
