package com.minewright.llm.cascade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LLMTier}.
 *
 * Tests cover:
 * <ul>
 *   <li>Tier property accessors (cost, latency, parameters)</li>
 *   <li>Tier availability checks</li>
 *   <li>API call requirements</li>
 *   <li>Cost estimation calculations</li>
 *   <li>Tier navigation (higher/lower)</li>
 *   <li>Tier parsing from strings</li>
 *   <li>String representation</li>
 * </ul>
 *
 * @since 1.6.0
 */
@DisplayName("LLM Tier Tests")
class LLMTierTest {

    // ------------------------------------------------------------------------
    // Tier Property Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("CACHE tier has correct properties")
    void cacheTierProperties() {
        LLMTier tier = LLMTier.CACHE;

        assertEquals("cache", tier.getTierId(), "Tier ID should be 'cache'");
        assertEquals(0.0, tier.getCostPer1kTokens(), 0.001, "Cost should be 0.0");
        assertEquals(1, tier.getExpectedLatencyMs(), "Latency should be ~1ms");
        assertEquals(0, tier.getParameterCountBillions(), "Parameters should be 0");
        assertEquals("cached response", tier.getDescription(), "Description should match");
        assertEquals("no LLM call", tier.getProvider(), "Provider should be 'no LLM call'");
    }

    @Test
    @DisplayName("LOCAL tier has correct properties")
    void localTierProperties() {
        LLMTier tier = LLMTier.LOCAL;

        assertEquals("local", tier.getTierId());
        assertEquals(0.0, tier.getCostPer1kTokens(), 0.001);
        assertEquals(100, tier.getExpectedLatencyMs());
        assertEquals(1, tier.getParameterCountBillions());
        assertEquals("local model", tier.getDescription());
        assertEquals("future: Ollama", tier.getProvider());
    }

    @Test
    @DisplayName("FAST tier has correct properties")
    void fastTierProperties() {
        LLMTier tier = LLMTier.FAST;

        assertEquals("fast", tier.getTierId());
        assertEquals(0.00001, tier.getCostPer1kTokens(), 0.000001);
        assertEquals(200, tier.getExpectedLatencyMs());
        assertEquals(8, tier.getParameterCountBillions());
        assertEquals("llama-3.1-8b-instant", tier.getDescription());
        assertEquals("Groq", tier.getProvider());
    }

    @Test
    @DisplayName("BALANCED tier has correct properties")
    void balancedTierProperties() {
        LLMTier tier = LLMTier.BALANCED;

        assertEquals("balanced", tier.getTierId());
        assertEquals(0.00020, tier.getCostPer1kTokens(), 0.00001);
        assertEquals(500, tier.getExpectedLatencyMs());
        assertEquals(70, tier.getParameterCountBillions());
        assertEquals("llama-3.3-70b/gpt-3.5", tier.getDescription());
        assertEquals("Groq/OpenAI", tier.getProvider());
    }

    @Test
    @DisplayName("SMART tier has correct properties")
    void smartTierProperties() {
        LLMTier tier = LLMTier.SMART;

        assertEquals("smart", tier.getTierId());
        assertEquals(0.01000, tier.getCostPer1kTokens(), 0.0001);
        assertEquals(2000, tier.getExpectedLatencyMs());
        assertEquals(1000, tier.getParameterCountBillions());
        assertEquals("gpt-4/claude-3", tier.getDescription());
        assertEquals("OpenAI/Anthropic", tier.getProvider());
    }

    // ------------------------------------------------------------------------
    // API Call Requirement Tests
    // ------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(value = LLMTier.class, names = {"CACHE", "LOCAL"})
    @DisplayName("CACHE and LOCAL tiers do not require API call")
    void nonApiTiers(LLMTier tier) {
        assertFalse(tier.requiresApiCall(),
            tier.name() + " tier should not require API call");
    }

    @ParameterizedTest
    @EnumSource(value = LLMTier.class, names = {"FAST", "BALANCED", "SMART"})
    @DisplayName("FAST, BALANCED, SMART tiers require API call")
    void apiTiers(LLMTier tier) {
        assertTrue(tier.requiresApiCall(),
            tier.name() + " tier should require API call");
    }

    // ------------------------------------------------------------------------
    // Availability Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("LOCAL tier is not available")
    void localTierNotAvailable() {
        assertFalse(LLMTier.LOCAL.isAvailable(),
            "LOCAL tier should not be available until Ollama integration");
    }

    @ParameterizedTest
    @EnumSource(value = LLMTier.class, names = {"CACHE", "FAST", "BALANCED", "SMART"})
    @DisplayName("All tiers except LOCAL are available")
    void availableTiers(LLMTier tier) {
        assertTrue(tier.isAvailable(),
            tier.name() + " tier should be available");
    }

    // ------------------------------------------------------------------------
    // Cost Estimation Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Cost estimation: CACHE tier is always free")
    void cacheTierCostEstimation() {
        LLMTier tier = LLMTier.CACHE;

        assertEquals(0.0, tier.estimateCost(0), 0.001);
        assertEquals(0.0, tier.estimateCost(100), 0.001);
        assertEquals(0.0, tier.estimateCost(1000), 0.001);
        assertEquals(0.0, tier.estimateCost(10000), 0.001);
    }

    @Test
    @DisplayName("Cost estimation: LOCAL tier is always free")
    void localTierCostEstimation() {
        LLMTier tier = LLMTier.LOCAL;

        assertEquals(0.0, tier.estimateCost(0), 0.001);
        assertEquals(0.0, tier.estimateCost(1000), 0.001);
    }

    @Test
    @DisplayName("Cost estimation: FAST tier calculation")
    void fastTierCostEstimation() {
        LLMTier tier = LLMTier.FAST;
        double costPer1k = 0.00001;

        assertEquals(0.0, tier.estimateCost(0), 0.000001);
        assertEquals(costPer1k, tier.estimateCost(1000), 0.000001);
        assertEquals(costPer1k * 10, tier.estimateCost(10000), 0.000001);
        assertEquals(costPer1k * 0.5, tier.estimateCost(500), 0.000001);
    }

    @Test
    @DisplayName("Cost estimation: BALANCED tier calculation")
    void balancedTierCostEstimation() {
        LLMTier tier = LLMTier.BALANCED;
        double costPer1k = 0.00020;

        assertEquals(0.0, tier.estimateCost(0), 0.00001);
        assertEquals(costPer1k, tier.estimateCost(1000), 0.00001);
        assertEquals(costPer1k * 5, tier.estimateCost(5000), 0.00001);
    }

    @Test
    @DisplayName("Cost estimation: SMART tier calculation")
    void smartTierCostEstimation() {
        LLMTier tier = LLMTier.SMART;
        double costPer1k = 0.01000;

        assertEquals(0.0, tier.estimateCost(0), 0.0001);
        assertEquals(costPer1k, tier.estimateCost(1000), 0.0001);
        assertEquals(costPer1k * 2, tier.estimateCost(2000), 0.0001);
        assertEquals(costPer1k * 10, tier.estimateCost(10000), 0.0001);
    }

    // ------------------------------------------------------------------------
    // Tier Navigation Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Next higher tier: CACHE -> FAST")
    void nextHigherTierCacheToFast() {
        assertEquals(LLMTier.FAST, LLMTier.CACHE.nextHigherTier());
    }

    @Test
    @DisplayName("Next higher tier: LOCAL -> FAST")
    void nextHigherTierLocalToFast() {
        assertEquals(LLMTier.FAST, LLMTier.LOCAL.nextHigherTier());
    }

    @Test
    @DisplayName("Next higher tier: FAST -> BALANCED")
    void nextHigherTierFastToBalanced() {
        assertEquals(LLMTier.BALANCED, LLMTier.FAST.nextHigherTier());
    }

    @Test
    @DisplayName("Next higher tier: BALANCED -> SMART")
    void nextHigherTierBalancedToSmart() {
        assertEquals(LLMTier.SMART, LLMTier.BALANCED.nextHigherTier());
    }

    @Test
    @DisplayName("Next higher tier: SMART -> SMART (max tier)")
    void nextHigherTierSmartToSmart() {
        assertEquals(LLMTier.SMART, LLMTier.SMART.nextHigherTier(),
            "SMART tier should return itself as next higher");
    }

    @Test
    @DisplayName("Next lower tier: CACHE -> CACHE (min tier)")
    void nextLowerTierCacheToCache() {
        assertEquals(LLMTier.CACHE, LLMTier.CACHE.nextLowerTier(),
            "CACHE tier should return itself as next lower");
    }

    @Test
    @DisplayName("Next lower tier: LOCAL -> CACHE")
    void nextLowerTierLocalToCache() {
        assertEquals(LLMTier.CACHE, LLMTier.LOCAL.nextLowerTier());
    }

    @Test
    @DisplayName("Next lower tier: FAST -> CACHE")
    void nextLowerTierFastToCache() {
        assertEquals(LLMTier.CACHE, LLMTier.FAST.nextLowerTier());
    }

    @Test
    @DisplayName("Next lower tier: BALANCED -> FAST")
    void nextLowerTierBalancedToFast() {
        assertEquals(LLMTier.FAST, LLMTier.BALANCED.nextLowerTier());
    }

    @Test
    @DisplayName("Next lower tier: SMART -> BALANCED")
    void nextLowerTierSmartToBalanced() {
        assertEquals(LLMTier.BALANCED, LLMTier.SMART.nextLowerTier());
    }

    // ------------------------------------------------------------------------
    // Tier Parsing Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("fromId: valid tier IDs are parsed correctly")
    void fromIdValidTierIds() {
        assertEquals(LLMTier.CACHE, LLMTier.fromId("cache"));
        assertEquals(LLMTier.CACHE, LLMTier.fromId("CACHE"));
        assertEquals(LLMTier.LOCAL, LLMTier.fromId("local"));
        assertEquals(LLMTier.FAST, LLMTier.fromId("fast"));
        assertEquals(LLMTier.BALANCED, LLMTier.fromId("balanced"));
        assertEquals(LLMTier.SMART, LLMTier.fromId("smart"));
    }

    @Test
    @DisplayName("fromId: invalid tier ID returns BALANCED as default")
    void fromIdInvalidTierId() {
        assertEquals(LLMTier.BALANCED, LLMTier.fromId("invalid"));
        assertEquals(LLMTier.BALANCED, LLMTier.fromId("unknown"));
        assertEquals(LLMTier.BALANCED, LLMTier.fromId("tier1"));
    }

    @Test
    @DisplayName("fromId: null returns BALANCED as default")
    void fromIdNullReturnsDefault() {
        assertEquals(LLMTier.BALANCED, LLMTier.fromId(null));
    }

    @Test
    @DisplayName("fromId: empty string returns BALANCED as default")
    void fromIdEmptyReturnsDefault() {
        assertEquals(LLMTier.BALANCED, LLMTier.fromId(""));
        assertEquals(LLMTier.BALANCED, LLMTier.fromId("   "));
    }

    @Test
    @DisplayName("fromId: case insensitive parsing")
    void fromIdCaseInsensitive() {
        assertEquals(LLMTier.FAST, LLMTier.fromId("fast"));
        assertEquals(LLMTier.FAST, LLMTier.fromId("FAST"));
        assertEquals(LLMTier.FAST, LLMTier.fromId("Fast"));
        assertEquals(LLMTier.FAST, LLMTier.fromId("fAsT"));
    }

    // ------------------------------------------------------------------------
    // String Representation Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("toString: contains all relevant information")
    void toStringContainsAllInfo() {
        LLMTier tier = LLMTier.FAST;
        String str = tier.toString();

        assertTrue(str.contains("FAST"), "Should contain tier name");
        assertTrue(str.contains("llama-3.1-8b-instant"), "Should contain description");
        assertTrue(str.contains("200"), "Should contain latency");
        assertTrue(str.contains("8"), "Should contain parameter count");
    }

    // ------------------------------------------------------------------------
    // Cost Comparison Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Cost progression: higher tiers cost more")
    void costProgression() {
        assertTrue(LLMTier.CACHE.getCostPer1kTokens() < LLMTier.FAST.getCostPer1kTokens());
        assertTrue(LLMTier.FAST.getCostPer1kTokens() < LLMTier.BALANCED.getCostPer1kTokens());
        assertTrue(LLMTier.BALANCED.getCostPer1kTokens() < LLMTier.SMART.getCostPer1kTokens());
    }

    @Test
    @DisplayName("Latency progression: higher tiers have higher latency")
    void latencyProgression() {
        assertTrue(LLMTier.CACHE.getExpectedLatencyMs() < LLMTier.FAST.getExpectedLatencyMs());
        assertTrue(LLMTier.FAST.getExpectedLatencyMs() < LLMTier.BALANCED.getExpectedLatencyMs());
        assertTrue(LLMTier.BALANCED.getExpectedLatencyMs() < LLMTier.SMART.getExpectedLatencyMs());
    }

    @Test
    @DisplayName("Parameter progression: higher tiers have more parameters")
    void parameterProgression() {
        assertTrue(LLMTier.CACHE.getParameterCountBillions() < LLMTier.FAST.getParameterCountBillions());
        assertTrue(LLMTier.FAST.getParameterCountBillions() < LLMTier.BALANCED.getParameterCountBillions());
        assertTrue(LLMTier.BALANCED.getParameterCountBillions() < LLMTier.SMART.getParameterCountBillions());
    }

    // ------------------------------------------------------------------------
    // Edge Case Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Cost estimation: handles large token counts")
    void costEstimationLargeTokenCounts() {
        LLMTier tier = LLMTier.SMART;
        int largeTokenCount = 1_000_000; // 1M tokens

        double cost = tier.estimateCost(largeTokenCount);
        assertTrue(cost > 0, "Cost should be positive for large token counts");
        assertTrue(cost < 100, "Cost should be reasonable (< $100 for 1M tokens)");
    }

    @Test
    @DisplayName("Cost estimation: handles fractional token counts")
    void costEstimationFractionalTokens() {
        LLMTier tier = LLMTier.BALANCED;

        // Small token counts should produce fractional costs
        double cost500 = tier.estimateCost(500);
        double cost100 = tier.estimateCost(100);

        assertTrue(cost500 > 0 && cost500 < tier.estimateCost(1000));
        assertTrue(cost100 > 0 && cost100 < tier.estimateCost(1000));
    }

    @Test
    @DisplayName("All enum values are present")
    void allEnumValuesPresent() {
        LLMTier[] values = LLMTier.values();

        assertEquals(5, values.length, "Should have 5 tier levels");
        assertTrue(java.util.Arrays.asList(values).contains(LLMTier.CACHE));
        assertTrue(java.util.Arrays.asList(values).contains(LLMTier.LOCAL));
        assertTrue(java.util.Arrays.asList(values).contains(LLMTier.FAST));
        assertTrue(java.util.Arrays.asList(values).contains(LLMTier.BALANCED));
        assertTrue(java.util.Arrays.asList(values).contains(LLMTier.SMART));
    }

    @Test
    @DisplayName("valueOf: works for all enum names")
    void valueOfAllEnumNames() {
        assertEquals(LLMTier.CACHE, LLMTier.valueOf("CACHE"));
        assertEquals(LLMTier.LOCAL, LLMTier.valueOf("LOCAL"));
        assertEquals(LLMTier.FAST, LLMTier.valueOf("FAST"));
        assertEquals(LLMTier.BALANCED, LLMTier.valueOf("BALANCED"));
        assertEquals(LLMTier.SMART, LLMTier.valueOf("SMART"));
    }
}
