package com.minewright.llm.cascade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RoutingDecision}.
 *
 * Tests cover:
 * <ul>
 *   <li>Record creation with required fields</li>
 *   <li>Factory methods (cacheHit, failure)</li>
 *   <li>Cost calculation</li>
 *   <li>Fallback detection</li>
 *   <li>Validation</li>
 *   <li>String representation</li>
 *   <li>Builder pattern</li>
 * </ul>
 *
 * @since 1.6.0
 */
@DisplayName("Routing Decision Tests")
class RoutingDecisionTest {

    // ------------------------------------------------------------------------
    // Constructor and Factory Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Constructor with required fields creates valid decision")
    void constructorWithRequiredFields() {
        RoutingDecision decision = new RoutingDecision(
            "mine 10 stone",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            250,
            150,
            true
        );

        assertEquals("mine 10 stone", decision.command());
        assertEquals(TaskComplexity.SIMPLE, decision.detectedComplexity());
        assertEquals(LLMTier.FAST, decision.selectedTier());
        assertEquals(LLMTier.FAST, decision.actualTier());
        assertEquals(250, decision.latencyMs());
        assertEquals(150, decision.tokensUsed());
        assertTrue(decision.success());
        assertNotNull(decision.timestamp());
        assertFalse(decision.fromCache());
        assertFalse(decision.hasFallback());
        assertNull(decision.errorMessage());
    }

    @Test
    @DisplayName("cacheHit factory creates CACHE tier decision")
    void cacheHitFactoryCreatesCacheDecision() {
        RoutingDecision decision = RoutingDecision.cacheHit(
            "follow me",
            TaskComplexity.TRIVIAL,
            5
        );

        assertEquals("follow me", decision.command());
        assertEquals(TaskComplexity.TRIVIAL, decision.detectedComplexity());
        assertEquals(LLMTier.CACHE, decision.selectedTier());
        assertEquals(LLMTier.CACHE, decision.actualTier());
        assertEquals(5, decision.latencyMs());
        assertEquals(0, decision.tokensUsed());
        assertTrue(decision.success());
        assertTrue(decision.fromCache());
        assertFalse(decision.hasFallback());
        assertNull(decision.fallbackReason());
        assertNull(decision.errorMessage());
    }

    @Test
    @DisplayName("failure factory creates failed decision")
    void failureFactoryCreatesFailedDecision() {
        RoutingDecision decision = RoutingDecision.failure(
            "complex command",
            TaskComplexity.COMPLEX,
            LLMTier.SMART,
            LLMTier.SMART,
            5000,
            "Rate limit exceeded"
        );

        assertEquals("complex command", decision.command());
        assertEquals(TaskComplexity.COMPLEX, decision.detectedComplexity());
        assertEquals(LLMTier.SMART, decision.selectedTier());
        assertEquals(LLMTier.SMART, decision.actualTier());
        assertEquals(5000, decision.latencyMs());
        assertEquals(0, decision.tokensUsed());
        assertFalse(decision.success());
        assertFalse(decision.fromCache());
        assertFalse(decision.hasFallback());
        assertEquals("Rate limit exceeded", decision.errorMessage());
    }

    // ------------------------------------------------------------------------
    // Cost Calculation Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("getEstimatedCost: CACHE tier is free")
    void estimatedCostCacheTier() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.TRIVIAL,
            LLMTier.CACHE,
            LLMTier.CACHE,
            1,
            1000,
            true
        );

        assertEquals(0.0, decision.getEstimatedCost(), 0.001);
    }

    @Test
    @DisplayName("getEstimatedCost: FAST tier calculation")
    void estimatedCostFastTier() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            200,
            1000,
            true
        );

        assertEquals(0.00001, decision.getEstimatedCost(), 0.000001);
    }

    @Test
    @DisplayName("getEstimatedCost: BALANCED tier calculation")
    void estimatedCostBalancedTier() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.MODERATE,
            LLMTier.BALANCED,
            LLMTier.BALANCED,
            500,
            2000,
            true
        );

        assertEquals(0.00040, decision.getEstimatedCost(), 0.00001);
    }

    @Test
    @DisplayName("getEstimatedCost: SMART tier calculation")
    void estimatedCostSmartTier() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.COMPLEX,
            LLMTier.SMART,
            LLMTier.SMART,
            2000,
            5000,
            true
        );

        assertEquals(0.05000, decision.getEstimatedCost(), 0.0001);
    }

    @Test
    @DisplayName("getEstimatedCost: uses actualTier not selectedTier")
    void estimatedCostUsesActualTier() {
        // Escalation scenario: selected FAST, actual BALANCED
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.BALANCED,
            600,
            1000,
            true
        );

        // Should use BALANCED tier cost, not FAST
        assertEquals(0.00020, decision.getEstimatedCost(), 0.00001);
    }

    // ------------------------------------------------------------------------
    // Success and Fallback Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("isSuccessful: returns success flag")
    void isSuccessfulReturnsFlag() {
        RoutingDecision success = new RoutingDecision(
            "command", TaskComplexity.SIMPLE, LLMTier.FAST, LLMTier.FAST,
            200, 100, true
        );
        RoutingDecision failure = new RoutingDecision(
            "command", TaskComplexity.SIMPLE, LLMTier.FAST, LLMTier.FAST,
            200, 0, false
        );

        assertTrue(success.isSuccessful());
        assertFalse(failure.isSuccessful());
    }

    @Test
    @DisplayName("hasFallback: true when actualTier differs from selectedTier")
    void hasFallbackTrueWhenTiersDiffer() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.BALANCED,
            500,
            100,
            true
        );

        assertTrue(decision.hasFallback());
        assertNotNull(decision.fallbackReason());
    }

    @Test
    @DisplayName("hasFallback: false when tiers match")
    void hasFallbackFalseWhenTiersMatch() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            200,
            100,
            true
        );

        assertFalse(decision.hasFallback());
        assertNull(decision.fallbackReason());
    }

    @Test
    @DisplayName("fallbackReason: escalation message")
    void fallbackReasonEscalation() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.BALANCED,
            500,
            100,
            true
        );

        assertTrue(decision.fallbackReason().contains("Escalated"));
        assertTrue(decision.fallbackReason().contains("FAST"));
        assertTrue(decision.fallbackReason().contains("BALANCED"));
    }

    @Test
    @DisplayName("fallbackReason: downgradation message")
    void fallbackReasonDowngradation() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.MODERATE,
            LLMTier.BALANCED,
            LLMTier.FAST,
            300,
            100,
            true
        );

        assertTrue(decision.fallbackReason().contains("Downgraded"));
        assertTrue(decision.fallbackReason().contains("BALANCED"));
        assertTrue(decision.fallbackReason().contains("FAST"));
    }

    // ------------------------------------------------------------------------
    // Validation Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("validate: valid decision returns empty Optional")
    void validateValidDecision() {
        RoutingDecision decision = new RoutingDecision(
            "valid command",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            200,
            100,
            true
        );

        Optional<String> error = decision.validate();
        assertTrue(error.isEmpty(), "Valid decision should have no validation errors");
    }

    @Test
    @DisplayName("validate: null command fails validation")
    void validateNullCommand() {
        RoutingDecision decision = new RoutingDecision(
            null,
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            200,
            100,
            true
        );

        Optional<String> error = decision.validate();
        assertTrue(error.isPresent());
        assertTrue(error.get().contains("command"));
    }

    @Test
    @DisplayName("validate: empty command fails validation")
    void validateEmptyCommand() {
        RoutingDecision decision = new RoutingDecision(
            "   ",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            200,
            100,
            true
        );

        Optional<String> error = decision.validate();
        assertTrue(error.isPresent());
        assertTrue(error.get().contains("command"));
    }

    @Test
    @DisplayName("validate: null complexity fails validation")
    void validateNullComplexity() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            null,
            LLMTier.FAST,
            LLMTier.FAST,
            200,
            100,
            true
        );

        Optional<String> error = decision.validate();
        assertTrue(error.isPresent());
        assertTrue(error.get().contains("complexity"));
    }

    @Test
    @DisplayName("validate: null selectedTier fails validation")
    void validateNullSelectedTier() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.SIMPLE,
            null,
            LLMTier.FAST,
            200,
            100,
            true
        );

        Optional<String> error = decision.validate();
        assertTrue(error.isPresent());
        assertTrue(error.get().contains("selectedTier"));
    }

    @Test
    @DisplayName("validate: null actualTier fails validation")
    void validateNullActualTier() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            null,
            200,
            100,
            true
        );

        Optional<String> error = decision.validate();
        assertTrue(error.isPresent());
        assertTrue(error.get().contains("actualTier"));
    }

    @Test
    @DisplayName("validate: negative latency fails validation")
    void validateNegativeLatency() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            -1,
            100,
            true
        );

        Optional<String> error = decision.validate();
        assertTrue(error.isPresent());
        assertTrue(error.get().contains("latency"));
    }

    @Test
    @DisplayName("validate: negative tokens fails validation")
    void validateNegativeTokens() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            200,
            -1,
            true
        );

        Optional<String> error = decision.validate();
        assertTrue(error.isPresent());
        assertTrue(error.get().contains("tokens"));
    }

    @Test
    @DisplayName("validate: null timestamp fails validation")
    void validateNullTimestamp() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            200,
            100,
            true,
            null,
            false,
            null,
            null
        );

        Optional<String> error = decision.validate();
        assertTrue(error.isPresent());
        assertTrue(error.get().contains("timestamp"));
    }

    // ------------------------------------------------------------------------
    // String Representation Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("getSummary: returns concise summary")
    void getSummaryReturnsConciseSummary() {
        RoutingDecision decision = new RoutingDecision(
            "mine 10 stone",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            250,
            150,
            true
        );

        String summary = decision.getSummary();

        assertTrue(summary.contains("fast"));
        assertTrue(summary.contains("SIMPLE"));
        assertTrue(summary.contains("250"));
        assertTrue(summary.contains("150"));
    }

    @Test
    @DisplayName("toString: contains all relevant fields")
    void toStringContainsAllFields() {
        RoutingDecision decision = new RoutingDecision(
            "mine 10 stone",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            250,
            150,
            true
        );

        String str = decision.toString();

        assertTrue(str.contains("mine 10 stone"));
        assertTrue(str.contains("SIMPLE"));
        assertTrue(str.contains("FAST"));
        assertTrue(str.contains("250"));
        assertTrue(str.contains("150"));
        assertTrue(str.contains("success=true"));
    }

    @Test
    @DisplayName("toString: truncates long commands")
    void toStringTruncatesLongCommands() {
        String longCommand = "a".repeat(100);

        RoutingDecision decision = new RoutingDecision(
            longCommand,
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            250,
            150,
            true
        );

        String str = decision.toString();
        assertTrue(str.length() < longCommand.length() + 50, "Long command should be truncated");
        assertTrue(str.contains("..."));
    }

    // ------------------------------------------------------------------------
    // Builder Pattern Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Builder: creates valid decision")
    void builderCreatesValidDecision() {
        Instant testTimestamp = Instant.now();

        RoutingDecision decision = RoutingDecision.builder()
            .command("test command")
            .detectedComplexity(TaskComplexity.MODERATE)
            .selectedTier(LLMTier.BALANCED)
            .actualTier(LLMTier.BALANCED)
            .latencyMs(500)
            .tokensUsed(1000)
            .success(true)
            .timestamp(testTimestamp)
            .fromCache(false)
            .fallbackReason(null)
            .errorMessage(null)
            .build();

        assertEquals("test command", decision.command());
        assertEquals(TaskComplexity.MODERATE, decision.detectedComplexity());
        assertEquals(LLMTier.BALANCED, decision.selectedTier());
        assertEquals(LLMTier.BALANCED, decision.actualTier());
        assertEquals(500, decision.latencyMs());
        assertEquals(1000, decision.tokensUsed());
        assertTrue(decision.success());
        assertEquals(testTimestamp, decision.timestamp());
        assertFalse(decision.fromCache());
    }

    @Test
    @DisplayName("Builder: has default timestamp")
    void builderHasDefaultTimestamp() {
        RoutingDecision decision = RoutingDecision.builder()
            .command("command")
            .detectedComplexity(TaskComplexity.SIMPLE)
            .selectedTier(LLMTier.FAST)
            .actualTier(LLMTier.FAST)
            .latencyMs(200)
            .tokensUsed(100)
            .success(true)
            .build();

        assertNotNull(decision.timestamp());
    }

    @Test
    @DisplayName("Builder: method chaining works")
    void builderMethodChaining() {
        RoutingDecision decision = RoutingDecision.builder()
            .command("command")
            .detectedComplexity(TaskComplexity.COMPLEX)
            .selectedTier(LLMTier.SMART)
            .actualTier(LLMTier.SMART)
            .latencyMs(2000)
            .tokensUsed(5000)
            .success(true)
            .build();

        assertNotNull(decision);
    }

    // ------------------------------------------------------------------------
    // Savings Calculation Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Savings: cache hit saves money compared to FAST")
    void savingsCacheHitVsFast() {
        RoutingDecision cacheDecision = RoutingDecision.cacheHit(
            "command",
            TaskComplexity.SIMPLE,
            1
        );

        RoutingDecision fastDecision = new RoutingDecision(
            "command",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            200,
            1000,
            true
        );

        assertTrue(cacheDecision.getEstimatedCost() < fastDecision.getEstimatedCost());
        assertEquals(0.0, cacheDecision.getEstimatedCost());
        assertEquals(0.00001, fastDecision.getEstimatedCost(), 0.000001);
    }

    @Test
    @DisplayName("Savings: downgrade from SMART to BALANCED saves money")
    void savingsDowngradeSmartToBalanced() {
        int tokensUsed = 5000;

        RoutingDecision smartDecision = new RoutingDecision(
            "command",
            TaskComplexity.COMPLEX,
            LLMTier.SMART,
            LLMTier.SMART,
            2000,
            tokensUsed,
            true
        );

        RoutingDecision balancedDecision = new RoutingDecision(
            "command",
            TaskComplexity.MODERATE,
            LLMTier.BALANCED,
            LLMTier.BALANCED,
            500,
            tokensUsed,
            true
        );

        double smartCost = smartDecision.getEstimatedCost();
        double balancedCost = balancedDecision.getEstimatedCost();

        assertTrue(balancedCost < smartCost);
        assertTrue(smartCost - balancedCost > 0.04); // Should save ~$0.04
    }

    @Test
    @DisplayName("Savings: downgrade from BALANCED to FAST saves money")
    void savingsDowngradeBalancedToFast() {
        int tokensUsed = 1000;

        RoutingDecision balancedDecision = new RoutingDecision(
            "command",
            TaskComplexity.MODERATE,
            LLMTier.BALANCED,
            LLMTier.BALANCED,
            500,
            tokensUsed,
            true
        );

        RoutingDecision fastDecision = new RoutingDecision(
            "command",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            200,
            tokensUsed,
            true
        );

        assertTrue(fastDecision.getEstimatedCost() < balancedDecision.getEstimatedCost());
    }

    // ------------------------------------------------------------------------
    // Record Equality Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Record equality: same values are equal")
    void recordEqualitySameValues() {
        RoutingDecision decision1 = new RoutingDecision(
            "command",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            200,
            100,
            true
        );

        RoutingDecision decision2 = new RoutingDecision(
            "command",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            200,
            100,
            true
        );

        assertEquals(decision1, decision2);
        assertEquals(decision1.hashCode(), decision2.hashCode());
    }

    @Test
    @DisplayName("Record inequality: different values are not equal")
    void recordInequalityDifferentValues() {
        RoutingDecision decision1 = new RoutingDecision(
            "command1",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            200,
            100,
            true
        );

        RoutingDecision decision2 = new RoutingDecision(
            "command2",
            TaskComplexity.SIMPLE,
            LLMTier.FAST,
            LLMTier.FAST,
            200,
            100,
            true
        );

        assertNotEquals(decision1, decision2);
    }

    // ------------------------------------------------------------------------
    // Edge Case Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Edge case: zero tokens is handled")
    void edgeCaseZeroTokens() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.TRIVIAL,
            LLMTier.CACHE,
            LLMTier.CACHE,
            1,
            0,
            true
        );

        assertEquals(0.0, decision.getEstimatedCost(), 0.001);
        assertEquals(0, decision.tokensUsed());
    }

    @Test
    @DisplayName("Edge case: very large token count is handled")
    void edgeCaseLargeTokenCount() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.NOVEL,
            LLMTier.SMART,
            LLMTier.SMART,
            3000,
            100000,
            true
        );

        assertTrue(decision.getEstimatedCost() > 0);
        assertTrue(decision.getEstimatedCost() < 10); // Should be reasonable
    }

    @Test
    @DisplayName("Edge case: zero latency is handled")
    void edgeCaseZeroLatency() {
        RoutingDecision decision = new RoutingDecision(
            "command",
            TaskComplexity.TRIVIAL,
            LLMTier.CACHE,
            LLMTier.CACHE,
            0,
            0,
            true
        );

        assertEquals(0, decision.latencyMs());
        assertTrue(decision.validate().isEmpty(), "Zero latency should be valid");
    }
}
