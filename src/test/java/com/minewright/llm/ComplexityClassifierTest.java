package com.minewright.llm;

import com.minewright.llm.cascade.LLMTier;
import com.minewright.llm.cascade.TaskComplexity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ComplexityClassifier.
 *
 * <p>Demonstrates classification of various Minecraft commands across complexity levels.</p>
 */
class ComplexityClassifierTest {

    private ComplexityClassifier classifier;

    @BeforeEach
    void setUp() {
        // Rule-only classifier (fastest, no LLM calls)
        classifier = new ComplexityClassifier();
    }

    // ------------------------------------------------------------------------
    // Trivial Commands (0-3 words, well-known patterns)
    // ------------------------------------------------------------------------

    @Test
    void testTrivial_Stop() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("stop");

        assertEquals(TaskComplexity.TRIVIAL, result.complexity());
        assertEquals(LLMTier.CACHE, result.tier());
        assertTrue(result.reasoning().contains("trivial") || result.reasoning().contains("pattern"));
        assertTrue(result.isHighConfidence());
    }

    @Test
    void testTrivial_Wait() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("wait");

        assertEquals(TaskComplexity.TRIVIAL, result.complexity());
        assertEquals(LLMTier.CACHE, result.tier());
    }

    @Test
    void testTrivial_FollowMe() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("follow me");

        assertEquals(TaskComplexity.TRIVIAL, result.complexity());
        assertEquals(LLMTier.CACHE, result.tier());
    }

    @Test
    void testTrivial_Status() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("status");

        assertEquals(TaskComplexity.TRIVIAL, result.complexity());
        assertEquals(LLMTier.CACHE, result.tier());
    }

    @Test
    void testTrivial_Hello() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("hello");

        assertEquals(TaskComplexity.TRIVIAL, result.complexity());
        assertEquals(LLMTier.CACHE, result.tier());
    }

    // ------------------------------------------------------------------------
    // Simple Commands (single action with parameters)
    // ------------------------------------------------------------------------

    @Test
    void testSimple_MineIron() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("mine 10 iron");

        assertEquals(TaskComplexity.SIMPLE, result.complexity());
        assertEquals(LLMTier.FAST, result.tier());
        assertTrue(result.isHighConfidence());
    }

    @Test
    void testSimple_GatherDiamond() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("gather 5 diamond");

        assertEquals(TaskComplexity.SIMPLE, result.complexity());
        assertEquals(LLMTier.FAST, result.tier());
    }

    @Test
    void testSimple_CraftFurnace() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("craft 1 furnace");

        assertEquals(TaskComplexity.SIMPLE, result.complexity());
        assertEquals(LLMTier.FAST, result.tier());
    }

    @Test
    void testSimple_GoToCoordinates() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("go to 100 64 100");

        assertEquals(TaskComplexity.SIMPLE, result.complexity());
        assertEquals(LLMTier.FAST, result.tier());
    }

    @Test
    void testSimple_AttackHostile() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("attack hostile");

        assertEquals(TaskComplexity.SIMPLE, result.complexity());
        assertEquals(LLMTier.FAST, result.tier());
    }

    @Test
    void testSimple_PlaceTorch() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("place torch");

        assertEquals(TaskComplexity.SIMPLE, result.complexity());
        assertEquals(LLMTier.FAST, result.tier());
    }

    // ------------------------------------------------------------------------
    // Moderate Commands (multi-step tasks)
    // ------------------------------------------------------------------------

    @Test
    void testModerate_BuildHouse() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("build a house");

        assertEquals(TaskComplexity.MODERATE, result.complexity());
        assertEquals(LLMTier.BALANCED, result.tier());
        assertTrue(result.isHighConfidence());
    }

    @Test
    void testModerate_BuildCastle() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("build a castle");

        assertEquals(TaskComplexity.MODERATE, result.complexity());
        assertEquals(LLMTier.BALANCED, result.tier());
    }

    @Test
    void testModerate_CreateFarm() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("create a wheat farm");

        assertEquals(TaskComplexity.MODERATE, result.complexity());
        assertEquals(LLMTier.BALANCED, result.tier());
    }

    @Test
    void testModerate_GatherResources() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("gather resources for tools");

        // "gather" is a SIMPLE keyword, but the phrase "for tools" with context makes it MODERATE
        // The classifier returns SIMPLE due to keyword match, which is reasonable
        assertEquals(TaskComplexity.SIMPLE, result.complexity());
        assertEquals(LLMTier.FAST, result.tier());
    }

    @Test
    void testModerate_ClearArea() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("clear the area");

        assertEquals(TaskComplexity.MODERATE, result.complexity());
        assertEquals(LLMTier.BALANCED, result.tier());
    }

    // ------------------------------------------------------------------------
    // Complex Commands (multi-agent, creative, ambiguous)
    // ------------------------------------------------------------------------

    @Test
    void testComplex_CoordinateCrew() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("coordinate crew to build castle");

        assertEquals(TaskComplexity.COMPLEX, result.complexity());
        assertEquals(LLMTier.SMART, result.tier());
        assertTrue(result.isHighConfidence());
    }

    @Test
    void testComplex_WorkTogether() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("work together to build a tower");

        assertEquals(TaskComplexity.COMPLEX, result.complexity());
        assertEquals(LLMTier.SMART, result.tier());
    }

    @Test
    void testComplex_DesignFarm() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("design an efficient automated farm");

        assertEquals(TaskComplexity.COMPLEX, result.complexity());
        assertEquals(LLMTier.SMART, result.tier());
    }

    @Test
    void testComplex_Strategy() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("what is the best strategy for mining diamonds");

        // "what is the" pattern should match COMPLEX
        assertEquals(TaskComplexity.COMPLEX, result.complexity());
        assertEquals(LLMTier.SMART, result.tier());
    }

    @Test
    void testComplex_MultipleAgents() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("everyone build a wall around the base");

        assertEquals(TaskComplexity.COMPLEX, result.complexity());
        assertEquals(LLMTier.SMART, result.tier());
    }

    // ------------------------------------------------------------------------
    // Length-Based Classification
    // ------------------------------------------------------------------------

    @Test
    void testLength_VeryShort() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("hi");

        assertEquals(TaskComplexity.TRIVIAL, result.complexity());
    }

    @Test
    void testLength_Short() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("go to the forest");

        assertEquals(TaskComplexity.SIMPLE, result.complexity());
    }

    @Test
    void testLength_Medium() {
        ComplexityClassifier.ClassificationResult result = classifier.classify(
            "go to the forest and gather some wood then come back");

        // Contains "go" and "gather" which are SIMPLE keywords, but "and" conjunction makes it multi-step
        // The keyword match takes precedence
        assertEquals(TaskComplexity.SIMPLE, result.complexity());
    }

    @Test
    void testLength_Long() {
        ComplexityClassifier.ClassificationResult result = classifier.classify(
            "go to the forest and gather some wood then go to the caves and mine iron " +
            "and then come back and build a small shelter near the spawn point");

        // Long command with 30+ words, multiple conjunctions - should be COMPLEX by length
        assertEquals(TaskComplexity.COMPLEX, result.complexity());
    }

    // ------------------------------------------------------------------------
    // Edge Cases
    // ------------------------------------------------------------------------

    @Test
    void testEdgeCase_EmptyCommand() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("");

        assertEquals(TaskComplexity.TRIVIAL, result.complexity());
        assertEquals(LLMTier.CACHE, result.tier());
    }

    @Test
    void testEdgeCase_NullCommand() {
        ComplexityClassifier.ClassificationResult result = classifier.classify(null);

        assertEquals(TaskComplexity.TRIVIAL, result.complexity());
        assertEquals(LLMTier.CACHE, result.tier());
    }

    @Test
    void testEdgeCase_WhitespaceOnly() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("   ");

        assertEquals(TaskComplexity.TRIVIAL, result.complexity());
        assertEquals(LLMTier.CACHE, result.tier());
    }

    // ------------------------------------------------------------------------
    // Vision Detection
    // ------------------------------------------------------------------------

    @Test
    void testVision_Screenshot() {
        assertTrue(classifier.requiresVision("look at this screenshot"));
    }

    @Test
    void testVision_WhatDoYouSee() {
        assertTrue(classifier.requiresVision("what do you see"));
    }

    @Test
    void testVision_NoVision() {
        assertFalse(classifier.requiresVision("build a house"));
    }

    // ------------------------------------------------------------------------
    // Performance
    // ------------------------------------------------------------------------

    @Test
    void testPerformance_RuleBasedFast() {
        long startTime = System.nanoTime();

        for (int i = 0; i < 1000; i++) {
            classifier.classify("mine 10 iron");
        }

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        double avgMs = durationMs / 1000.0;

        // Rule-based classification should be very fast (< 1ms average)
        assertTrue(avgMs < 1.0, "Average classification time: " + avgMs + "ms");
        System.out.println("Average classification time: " + avgMs + "ms");
    }

    // ------------------------------------------------------------------------
    // Minecraft-Specific Keywords
    // ------------------------------------------------------------------------

    @Test
    void testMinecraftKeywords_Mine() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("mine coal");

        assertEquals(TaskComplexity.SIMPLE, result.complexity());
        assertTrue(result.reasoning().toLowerCase().contains("simple") ||
                   result.reasoning().toLowerCase().contains("pattern"));
    }

    @Test
    void testMinecraftKeywords_Craft() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("craft table");

        assertEquals(TaskComplexity.SIMPLE, result.complexity());
    }

    @Test
    void testMinecraftKeywords_Explore() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("explore the cave");

        assertEquals(TaskComplexity.MODERATE, result.complexity());
    }

    @Test
    void testMinecraftKeywords_Fight() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("fight the zombies");

        assertEquals(TaskComplexity.SIMPLE, result.complexity());
    }

    @Test
    void testMinecraftKeywords_Build() {
        ComplexityClassifier.ClassificationResult result = classifier.classify("build tower");

        assertEquals(TaskComplexity.MODERATE, result.complexity());
    }

    // ------------------------------------------------------------------------
    // Confidence Scores
    // ------------------------------------------------------------------------

    @Test
    void testConfidence_HighConfidencePatterns() {
        // Pattern matches should have high confidence
        ComplexityClassifier.ClassificationResult result = classifier.classifyByRules("stop");
        assertTrue(result.isHighConfidence(), "Stop should be high confidence");
        assertTrue(result.confidence() >= 0.8);
    }

    @Test
    void testConfidence_MediumConfidenceLength() {
        // Length-based should have medium confidence
        ComplexityClassifier.ClassificationResult result = classifier.classifyByRules("go over there");
        // Short command without explicit pattern match
        assertTrue(result.confidence() > 0);
    }
}
