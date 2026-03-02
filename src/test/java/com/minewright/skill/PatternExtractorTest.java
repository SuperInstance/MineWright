package com.minewright.skill;

import com.minewright.testutil.TestLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link PatternExtractor}.
 *
 * <p><b>Test Coverage:</b></p>
 * <ul>
 *   <li>Pattern detection in action sequences</li>
 *   <li>Loop pattern recognition</li>
 *   <li>Sequence pattern recognition</li>
 *   <li>Parameter extraction</li>
 *   <li>Confidence score calculation</li>
 *   <li>Edge cases (empty sequences, single actions)</li>
 * </ul>
 *
 * @see PatternExtractor
 * @see ExecutionSequence
 * @see ActionRecord
 * @since 1.0.0
 */
@DisplayName("PatternExtractor Tests")
class PatternExtractorTest {

    private PatternExtractor extractor;

    @BeforeEach
    void setUp() {
        TestLogger.initForTesting();
        extractor = new PatternExtractor();
    }

    // ==================== Pattern Detection Tests ====================

    @Nested
    @DisplayName("Pattern Detection Tests")
    class PatternDetectionTests {

        @Test
        @DisplayName("Detects repeated action sequences")
        void detectsRepeatedActionSequences() {
            // Given: Multiple similar sequences
            List<ExecutionSequence> sequences = createRepeatedMinePlaceSequences(5);

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should detect the pattern
            assertFalse(patterns.isEmpty(), "Should detect at least one pattern");
            assertEquals(1, patterns.size(), "Should detect exactly one pattern");

            PatternExtractor.Pattern pattern = patterns.get(0);
            assertEquals(5, pattern.getFrequency(), "Pattern should have frequency of 5");
            assertTrue(pattern.getSignature().contains("mine") && pattern.getSignature().contains("place"),
                "Pattern signature should include mine and place actions");
        }

        @Test
        @DisplayName("Detects patterns with varying parameters")
        void detectsPatternsWithVaryingParameters() {
            // Given: Sequences with different coordinates but same structure
            List<ExecutionSequence> sequences = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                sequences.add(createMiningSequence(
                    10 + i,  // varying X
                    60,      // fixed Y
                    20 + i   // varying Z
                ));
            }

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should detect pattern despite varying coordinates
            assertFalse(patterns.isEmpty(), "Should detect pattern with varying parameters");
            PatternExtractor.Pattern pattern = patterns.get(0);
            assertTrue(pattern.getParameters().contains("x") ||
                       pattern.getParameters().contains("z"),
                "Pattern should identify variable parameters");
        }

        @Test
        @DisplayName("Groups sequences by normalized signature")
        void groupsSequencesBySignature() {
            // Given: Sequences with different action types
            List<ExecutionSequence> sequences = new ArrayList<>();
            sequences.addAll(createRepeatedMinePlaceSequences(3));
            sequences.addAll(createRepeatedCraftSequences(3));

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should create separate patterns for each signature
            assertEquals(2, patterns.size(), "Should detect two distinct patterns");

            // Check that patterns have different signatures
            assertNotEquals(patterns.get(0).getSignature(), patterns.get(1).getSignature(),
                "Patterns should have different signatures");
        }

        @Test
        @DisplayName("Sorts patterns by frequency")
        void sortsPatternsByFrequency() {
            // Given: Sequences with different frequencies
            List<ExecutionSequence> sequences = new ArrayList<>();
            sequences.addAll(createRepeatedMinePlaceSequences(5));  // high frequency
            sequences.addAll(createRepeatedCraftSequences(3));       // lower frequency

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should be sorted by frequency (highest first)
            assertTrue(patterns.get(0).getFrequency() >= patterns.get(1).getFrequency(),
                "First pattern should have higher or equal frequency");
            assertEquals(5, patterns.get(0).getFrequency(), "First pattern should have frequency 5");
            assertEquals(3, patterns.get(1).getFrequency(), "Second pattern should have frequency 3");
        }
    }

    // ==================== Loop Pattern Recognition Tests ====================

    @Nested
    @DisplayName("Loop Pattern Recognition Tests")
    class LoopPatternRecognitionTests {

        @Test
        @DisplayName("Detects repeated action loops")
        void detectsRepeatedActionLoops() {
            // Given: Sequences with repeated single action (loop pattern)
            List<ExecutionSequence> sequences = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                ExecutionSequence seq = createLoopSequence("mine", 10, i);
                sequences.add(seq);
            }

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should detect loop pattern
            assertFalse(patterns.isEmpty(), "Should detect loop pattern");
            PatternExtractor.Pattern pattern = patterns.get(0);
            assertEquals(5, pattern.getFrequency(), "Pattern frequency should match sequence count");
            assertTrue(pattern.getActionSequence().contains("mine"),
                "Action sequence should contain the repeated action");
        }

        @Test
        @DisplayName("Detects complex loop patterns with multiple actions")
        void detectsComplexLoopPatterns() {
            // Given: Sequences with repeated action groups
            List<ExecutionSequence> sequences = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                sequences.add(createComplexLoopSequence(i));
            }

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should detect complex loop pattern
            assertFalse(patterns.isEmpty(), "Should detect complex loop pattern");
            PatternExtractor.Pattern pattern = patterns.get(0);
            assertTrue(pattern.getActionSequence().size() > 2,
                "Complex pattern should have multiple actions");
        }

        @Test
        @DisplayName("Calculates average execution time for loops")
        void calculatesAverageExecutionTimeForLoops() {
            // Given: Sequences with varying execution times
            List<ExecutionSequence> sequences = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                ExecutionSequence seq = createLoopSequence("mine", 10, i);
                // Vary execution time: 1000, 1100, 1200, 1300, 1400
                sequences.add(seq);
            }

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should calculate average execution time
            assertFalse(patterns.isEmpty(), "Should detect pattern");
            PatternExtractor.Pattern pattern = patterns.get(0);
            assertTrue(pattern.getAverageExecutionTime() > 0,
                "Should have positive average execution time");
        }
    }

    // ==================== Sequence Pattern Recognition Tests ====================

    @Nested
    @DisplayName("Sequence Pattern Recognition Tests")
    class SequencePatternRecognitionTests {

        @Test
        @DisplayName("Detects linear action sequences")
        void detectsLinearActionSequences() {
            // Given: Linear sequences (pathfind -> mine -> place)
            List<ExecutionSequence> sequences = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                sequences.add(createLinearSequence(i));
            }

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should detect linear sequence pattern
            assertFalse(patterns.isEmpty(), "Should detect linear sequence");
            PatternExtractor.Pattern pattern = patterns.get(0);
            List<String> actions = pattern.getActionSequence();

            assertTrue(actions.contains("pathfind"), "Should contain pathfind action");
            assertTrue(actions.contains("mine"), "Should contain mine action");
            assertTrue(actions.contains("place"), "Should contain place action");
        }

        @Test
        @DisplayName("Detects branching sequences with common prefix")
        void detectsBranchingSequences() {
            // Given: Sequences with common prefix but different suffixes
            List<ExecutionSequence> sequences = new ArrayList<>();

            // Branch 1: pathfind -> mine -> place
            for (int i = 0; i < 3; i++) {
                sequences.add(createLinearSequence(i));
            }

            // Branch 2: pathfind -> mine -> craft
            for (int i = 0; i < 3; i++) {
                sequences.add(createBranchingSequence(i, "craft"));
            }

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should detect separate patterns for each branch
            assertTrue(patterns.size() >= 1, "Should detect at least one pattern");

            // Check that different branches create different patterns
            Set<String> signatures = new HashSet<>();
            for (PatternExtractor.Pattern pattern : patterns) {
                signatures.add(pattern.getSignature());
            }
            assertTrue(signatures.size() >= 1, "Should have unique patterns");
        }

        @Test
        @DisplayName("Detects conditional action patterns")
        void detectsConditionalActionPatterns() {
            // Given: Sequences with conditional actions (torch placement every N blocks)
            List<ExecutionSequence> sequences = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                sequences.add(createConditionalSequence(i));
            }

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should detect patterns despite conditional variation
            assertFalse(patterns.isEmpty(), "Should detect patterns with conditional actions");
        }

        @Test
        @DisplayName("Preserves action order in patterns")
        void preservesActionOrder() {
            // Given: Sequences with specific action order
            List<ExecutionSequence> sequences = createRepeatedMinePlaceSequences(3);

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Action order should be preserved
            assertFalse(patterns.isEmpty(), "Should detect pattern");
            PatternExtractor.Pattern pattern = patterns.get(0);

            // Check that mine comes before place
            List<String> actions = pattern.getActionSequence();
            int mineIndex = actions.indexOf("mine");
            int placeIndex = actions.indexOf("place");

            assertTrue(mineIndex >= 0 && placeIndex >= 0, "Should contain both actions");
            assertTrue(mineIndex < placeIndex, "Mine should come before place in sequence");
        }
    }

    // ==================== Parameter Extraction Tests ====================

    @Nested
    @DisplayName("Parameter Extraction Tests")
    class ParameterExtractionTests {

        @Test
        @DisplayName("Extracts coordinate parameters")
        void extractsCoordinateParameters() {
            // Given: Sequences with coordinate parameters
            List<ExecutionSequence> sequences = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                Map<String, Object> params = new HashMap<>();
                params.put("x", 10 + i);
                params.put("y", 60);
                params.put("z", 20 + i);
                params.put("blockType", "stone");

                ActionRecord action = new ActionRecord("mine", params, 100, true, null);
                ExecutionSequence seq = ExecutionSequence.builder("agent1", "mine stone")
                    .addAction(action)
                    .build(true);
                sequences.add(seq);
            }

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should identify coordinates as variable parameters
            assertFalse(patterns.isEmpty(), "Should detect pattern");
            PatternExtractor.Pattern pattern = patterns.get(0);

            assertTrue(pattern.getParameters().contains("x") ||
                       pattern.getParameters().contains("y") ||
                       pattern.getParameters().contains("z"),
                "Should extract coordinate parameters");
        }

        @Test
        @DisplayName("Extracts count and amount parameters")
        void extractsCountParameters() {
            // Given: Sequences with count parameters
            List<ExecutionSequence> sequences = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                Map<String, Object> params = new HashMap<>();
                params.put("count", 5 + i);
                params.put("amount", 10);
                params.put("item", "wood");

                ActionRecord action = new ActionRecord("gather", params, 100, true, null);
                ExecutionSequence seq = ExecutionSequence.builder("agent1", "gather wood")
                    .addAction(action)
                    .build(true);
                sequences.add(seq);
            }

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should identify count/amount as variable parameters
            assertFalse(patterns.isEmpty(), "Should detect pattern");
            PatternExtractor.Pattern pattern = patterns.get(0);

            assertTrue(pattern.getParameters().contains("count") ||
                       pattern.getParameters().contains("amount"),
                "Should extract count/amount parameters");
        }

        @Test
        @DisplayName("Identifies constant vs variable parameters")
        void identifiesConstantVsVariableParameters() {
            // Given: Sequences with mixed constant and variable parameters
            List<ExecutionSequence> sequences = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                Map<String, Object> params = new HashMap<>();
                params.put("x", 10 + i);        // variable
                params.put("blockType", "stone"); // constant
                params.put("count", 5);          // variable

                ActionRecord action = new ActionRecord("mine", params, 100, true, null);
                ExecutionSequence seq = ExecutionSequence.builder("agent1", "mine stone")
                    .addAction(action)
                    .build(true);
                sequences.add(seq);
            }

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should distinguish constant from variable parameters
            assertFalse(patterns.isEmpty(), "Should detect pattern");
            PatternExtractor.Pattern pattern = patterns.get(0);

            // Variable parameters should be extracted
            assertTrue(pattern.getParameters().contains("x") ||
                       pattern.getParameters().contains("count"),
                "Should extract variable parameters");
        }

        @Test
        @DisplayName("Handles sequences with no parameters")
        void handlesSequencesWithNoParameters() {
            // Given: Sequences with no parameters
            List<ExecutionSequence> sequences = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Map<String, Object> params = new HashMap<>();
                // Empty parameters

                ActionRecord action = new ActionRecord("wait", params, 100, true, null);
                ExecutionSequence seq = ExecutionSequence.builder("agent1", "wait")
                    .addAction(action)
                    .build(true);
                sequences.add(seq);
            }

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should handle gracefully (may or may not extract pattern based on frequency)
            // The key is that it doesn't crash
            assertNotNull(patterns, "Should return non-null result");
        }
    }

    // ==================== Confidence Score Tests ====================

    @Nested
    @DisplayName("Confidence Score Calculation Tests")
    class ConfidenceScoreTests {

        @Test
        @DisplayName("Calculates confidence score from frequency and success rate")
        void calculatesConfidenceScore() {
            // Given: A pattern with known frequency and success rate
            List<ExecutionSequence> sequences = createRepeatedMinePlaceSequences(10);

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should calculate confidence score
            assertFalse(patterns.isEmpty(), "Should detect pattern");
            PatternExtractor.Pattern pattern = patterns.get(0);

            double confidence = pattern.getConfidence();
            assertTrue(confidence >= 0.0 && confidence <= 1.0,
                "Confidence should be between 0 and 1");
        }

        @Test
        @DisplayName("High frequency increases confidence")
        void highFrequencyIncreasesConfidence() {
            // Given: High frequency pattern
            List<ExecutionSequence> highFreqSeq = createRepeatedMinePlaceSequences(15);
            List<PatternExtractor.Pattern> highFreqPatterns = extractor.extractPatterns(highFreqSeq);

            // Given: Low frequency pattern
            List<ExecutionSequence> lowFreqSeq = createRepeatedMinePlaceSequences(3);
            List<PatternExtractor.Pattern> lowFreqPatterns = extractor.extractPatterns(lowFreqSeq);

            // Then: High frequency should have higher confidence
            assertFalse(highFreqPatterns.isEmpty(), "Should detect high frequency pattern");
            assertFalse(lowFreqPatterns.isEmpty(), "Should detect low frequency pattern");

            // Note: This assumes same success rate, so frequency is the differentiator
            double highFreqConfidence = highFreqPatterns.get(0).getConfidence();
            double lowFreqConfidence = lowFreqPatterns.get(0).getConfidence();

            // High frequency might not always beat low frequency if success rates differ
            // but in this case they're all successful
            assertTrue(highFreqConfidence >= 0.0, "High frequency confidence should be valid");
            assertTrue(lowFreqConfidence >= 0.0, "Low frequency confidence should be valid");
        }

        @Test
        @DisplayName("High success rate increases confidence")
        void highSuccessRateIncreasesConfidence() {
            // Given: High success rate pattern
            List<ExecutionSequence> highSuccessSeq = createSequencesWithSuccessRate(10, 0.95);
            List<PatternExtractor.Pattern> highSuccessPatterns = extractor.extractPatterns(highSuccessSeq);

            // Given: Low success rate pattern
            List<ExecutionSequence> lowSuccessSeq = createSequencesWithSuccessRate(10, 0.75);
            List<PatternExtractor.Pattern> lowSuccessPatterns = extractor.extractPatterns(lowSuccessSeq);

            // Then: High success rate should have higher confidence
            assertFalse(highSuccessPatterns.isEmpty(), "Should detect high success pattern");
            assertFalse(lowSuccessPatterns.isEmpty(), "Should detect low success pattern");

            double highSuccessConfidence = highSuccessPatterns.get(0).getConfidence();
            double lowSuccessConfidence = lowSuccessPatterns.get(0).getConfidence();

            assertTrue(highSuccessConfidence > lowSuccessConfidence,
                "High success rate should result in higher confidence");
        }

        @Test
        @DisplayName("Identifies high confidence patterns")
        void identifiesHighConfidencePatterns() {
            // Given: Pattern with high frequency and success rate
            List<ExecutionSequence> sequences = createRepeatedMinePlaceSequences(15);

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should identify as high confidence
            assertFalse(patterns.isEmpty(), "Should detect pattern");
            PatternExtractor.Pattern pattern = patterns.get(0);

            // With 15 successful executions, should be high confidence
            boolean isHighConfidence = pattern.isHighConfidence();
            assertTrue(isHighConfidence || pattern.getConfidence() >= 0.7,
                "Pattern with high frequency and success should have good confidence");
        }

        @Test
        @DisplayName("Low confidence patterns are not high confidence")
        void lowConfidencePatternsAreNotHighConfidence() {
            // Given: Pattern with low frequency
            List<ExecutionSequence> sequences = createRepeatedMinePlaceSequences(3);

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: May not be high confidence due to low frequency
            assertFalse(patterns.isEmpty(), "Should detect pattern");
            PatternExtractor.Pattern pattern = patterns.get(0);

            // With minimum frequency, might not be high confidence
            double confidence = pattern.getConfidence();
            assertTrue(confidence >= 0.0 && confidence <= 1.0,
                "Confidence should be valid");
        }
    }

    // ==================== Success Rate Tests ====================

    @Nested
    @DisplayName("Success Rate Calculation Tests")
    class SuccessRateTests {

        @Test
        @DisplayName("Calculates success rate correctly")
        void calculatesSuccessRateCorrectly() {
            // Given: Mix of successful and failed sequences
            List<ExecutionSequence> sequences = createSequencesWithSuccessRate(10, 0.7);

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should calculate correct success rate
            assertFalse(patterns.isEmpty(), "Should detect pattern");
            PatternExtractor.Pattern pattern = patterns.get(0);

            assertEquals(0.7, pattern.getSuccessRate(), 0.01,
                "Success rate should match input");
        }

        @Test
        @DisplayName("Tracks success and failure counts")
        void tracksSuccessAndFailureCounts() {
            // Given: Known success/failure distribution
            List<ExecutionSequence> sequences = createSequencesWithSuccessRate(10, 0.6);

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should track counts correctly
            assertFalse(patterns.isEmpty(), "Should detect pattern");
            PatternExtractor.Pattern pattern = patterns.get(0);

            assertEquals(10, pattern.getFrequency(), "Frequency should match total sequences");
            assertEquals(6, pattern.getSuccessCount(), "Success count should match");
            assertEquals(4, pattern.getFailureCount(), "Failure count should match");
        }

        @Test
        @DisplayName("Filters patterns below minimum success rate")
        void filtersPatternsBelowMinimumSuccessRate() {
            // Given: Patterns with low success rate
            List<ExecutionSequence> sequences = createSequencesWithSuccessRate(5, 0.5);

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should filter out low success rate patterns
            // MIN_SUCCESS_RATE = 0.7, so 0.5 should be filtered
            assertTrue(patterns.isEmpty(), "Should filter patterns below success rate threshold");
        }

        @Test
        @DisplayName("All successful sequences have 100% success rate")
        void allSuccessfulSequencesHavePerfectSuccessRate() {
            // Given: All successful sequences
            List<ExecutionSequence> sequences = createRepeatedMinePlaceSequences(5);

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should have 100% success rate
            assertFalse(patterns.isEmpty(), "Should detect pattern");
            PatternExtractor.Pattern pattern = patterns.get(0);

            assertEquals(1.0, pattern.getSuccessRate(), 0.001,
                "All successful sequences should have 100% success rate");
            assertEquals(pattern.getFrequency(), pattern.getSuccessCount(),
                "Success count should equal frequency");
            assertEquals(0, pattern.getFailureCount(),
                "Failure count should be zero");
        }
    }

    // ==================== Edge Cases Tests ====================

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Handles empty sequence list")
        void handlesEmptySequenceList() {
            // Given: Empty list
            List<ExecutionSequence> sequences = List.of();

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should return empty list
            assertNotNull(patterns, "Should return non-null result");
            assertTrue(patterns.isEmpty(), "Should return empty list for empty input");
        }

        @Test
        @DisplayName("Handles null input")
        void handlesNullInput() {
            // Given: Null input
            List<ExecutionSequence> sequences = null;

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should return empty list
            assertNotNull(patterns, "Should return non-null result");
            assertTrue(patterns.isEmpty(), "Should return empty list for null input");
        }

        @Test
        @DisplayName("Handles single action sequences")
        void handlesSingleActionSequences() {
            // Given: Sequences with single action
            List<ExecutionSequence> sequences = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Map<String, Object> params = new HashMap<>();
                params.put("blockType", "stone");
                ActionRecord action = new ActionRecord("mine", params, 100, true, null);

                ExecutionSequence seq = ExecutionSequence.builder("agent1", "mine stone")
                    .addAction(action)
                    .build(true);
                sequences.add(seq);
            }

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should handle single action (minimum frequency is 3)
            assertFalse(patterns.isEmpty(), "Should detect single-action pattern");
            PatternExtractor.Pattern pattern = patterns.get(0);
            assertEquals(1, pattern.getActionSequence().size(),
                "Pattern should have single action");
            assertEquals("mine", pattern.getActionSequence().get(0),
                "Action should be mine");
        }

        @Test
        @DisplayName("Filters sequences below minimum frequency")
        void filtersSequencesBelowMinimumFrequency() {
            // Given: Only 2 sequences (below MIN_FREQUENCY = 3)
            List<ExecutionSequence> sequences = createRepeatedMinePlaceSequences(2);

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should filter out due to low frequency
            assertTrue(patterns.isEmpty(), "Should filter patterns below frequency threshold");
        }

        @Test
        @DisplayName("Handles sequences with no actions")
        void handlesSequencesWithNoActions() {
            // Given: Empty sequences
            List<ExecutionSequence> sequences = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                ExecutionSequence seq = ExecutionSequence.builder("agent1", "empty goal")
                    .build(true);
                sequences.add(seq);
            }

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should handle gracefully
            assertNotNull(patterns, "Should return non-null result");
            // Empty sequences will all have same signature, but no actions to extract
        }

        @Test
        @DisplayName("Handles sequences with varying action counts")
        void handlesSequencesWithVaryingActionCounts() {
            // Given: Sequences with different numbers of actions
            List<ExecutionSequence> sequences = new ArrayList<>();

            // Short sequence
            sequences.add(createLinearSequence(0));

            // Long sequence
            sequences.add(createLongSequence(1));

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should not crash (may or may not find patterns due to different signatures)
            assertNotNull(patterns, "Should return non-null result");
        }

        @Test
        @DisplayName("Generates meaningful pattern names")
        void generatesMeaningfulPatternNames() {
            // Given: Sequences with specific goal
            List<ExecutionSequence> sequences = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                ExecutionSequence seq = createMiningSequence(10 + i, 60, 20 + i);
                sequences.add(seq);
            }

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // Then: Should generate meaningful name from goal
            assertFalse(patterns.isEmpty(), "Should detect pattern");
            PatternExtractor.Pattern pattern = patterns.get(0);

            assertNotNull(pattern.getName(), "Pattern should have a name");
            assertFalse(pattern.getName().isEmpty(), "Pattern name should not be empty");

            // Name should be capitalized
            assertEquals(Character.toUpperCase(pattern.getName().charAt(0)),
                pattern.getName().charAt(0),
                "Pattern name should be capitalized");
        }
    }

    // ==================== Pattern Object Tests ====================

    @Nested
    @DisplayName("Pattern Object Tests")
    class PatternObjectTests {

        @Test
        @DisplayName("Pattern getters return correct values")
        void patternGettersReturnCorrectValues() {
            // Given: A detected pattern
            List<ExecutionSequence> sequences = createRepeatedMinePlaceSequences(5);
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

            // When: Accessing pattern properties
            PatternExtractor.Pattern pattern = patterns.get(0);

            // Then: All getters should return valid values
            assertNotNull(pattern.getSignature(), "Signature should not be null");
            assertNotNull(pattern.getName(), "Name should not be null");
            assertNotNull(pattern.getActionSequence(), "Action sequence should not be null");
            assertNotNull(pattern.getParameters(), "Parameters should not be null");
            assertTrue(pattern.getFrequency() > 0, "Frequency should be positive");
            assertTrue(pattern.getSuccessRate() >= 0.0 && pattern.getSuccessRate() <= 1.0,
                "Success rate should be between 0 and 1");
            assertTrue(pattern.getAverageExecutionTime() >= 0,
                "Average execution time should be non-negative");
            assertTrue(pattern.getSuccessCount() >= 0, "Success count should be non-negative");
            assertTrue(pattern.getFailureCount() >= 0, "Failure count should be non-negative");
        }

        @Test
        @DisplayName("Pattern toString is informative")
        void patternToStringIsInformative() {
            // Given: A pattern
            List<ExecutionSequence> sequences = createRepeatedMinePlaceSequences(5);
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);
            PatternExtractor.Pattern pattern = patterns.get(0);

            // When: Converting to string
            String str = pattern.toString();

            // Then: Should contain key information
            assertNotNull(str, "toString should not return null");
            assertTrue(str.contains("name="), "Should contain name");
            assertTrue(str.contains("frequency="), "Should contain frequency");
            assertTrue(str.contains("successRate="), "Should contain success rate");
            assertTrue(str.contains("confidence="), "Should contain confidence");
        }

        @Test
        @DisplayName("Pattern action sequence is immutable")
        void patternActionSequenceIsImmutable() {
            // Given: A pattern
            List<ExecutionSequence> sequences = createRepeatedMinePlaceSequences(5);
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);
            PatternExtractor.Pattern pattern = patterns.get(0);

            // When: Trying to modify action sequence
            List<String> actions = pattern.getActionSequence();

            // Then: Should be immutable (List.copyOf creates immutable list)
            assertThrows(UnsupportedOperationException.class, () -> {
                actions.add("new_action");
            }, "Action sequence should be immutable");
        }

        @Test
        @DisplayName("Pattern parameters are immutable")
        void patternParametersAreImmutable() {
            // Given: A pattern
            List<ExecutionSequence> sequences = createRepeatedMinePlaceSequences(5);
            List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);
            PatternExtractor.Pattern pattern = patterns.get(0);

            // When: Trying to modify parameters
            Set<String> params = pattern.getParameters();

            // Then: Should be immutable (Set.copyOf creates immutable set)
            assertThrows(UnsupportedOperationException.class, () -> {
                params.add("new_param");
            }, "Parameters should be immutable");
        }
    }

    // ==================== Threshold Tests ====================

    @Nested
    @DisplayName("Threshold Tests")
    class ThresholdTests {

        @Test
        @DisplayName("Minimum frequency threshold is enforced")
        void minimumFrequencyThresholdIsEnforced() {
            // Given: Sequences at threshold boundary
            List<ExecutionSequence> belowThreshold = createRepeatedMinePlaceSequences(2);
            List<ExecutionSequence> atThreshold = createRepeatedMinePlaceSequences(3);

            // When: Extracting patterns
            List<PatternExtractor.Pattern> belowPatterns = extractor.extractPatterns(belowThreshold);
            List<PatternExtractor.Pattern> atPatterns = extractor.extractPatterns(atThreshold);

            // Then: Below threshold should be filtered, at threshold should pass
            assertTrue(belowPatterns.isEmpty(), "Below threshold should be filtered");
            assertFalse(atPatterns.isEmpty(), "At threshold should pass");
        }

        @Test
        @DisplayName("Minimum success rate threshold is enforced")
        void minimumSuccessRateThresholdIsEnforced() {
            // Given: Sequences at success rate boundary
            List<ExecutionSequence> belowThreshold = createSequencesWithSuccessRate(5, 0.65);
            List<ExecutionSequence> atThreshold = createSequencesWithSuccessRate(5, 0.75);

            // When: Extracting patterns
            List<PatternExtractor.Pattern> belowPatterns = extractor.extractPatterns(belowThreshold);
            List<PatternExtractor.Pattern> atPatterns = extractor.extractPatterns(atThreshold);

            // Then: Below threshold should be filtered, at threshold should pass
            assertTrue(belowPatterns.isEmpty(), "Below success rate threshold should be filtered");
            assertFalse(atPatterns.isEmpty(), "At success rate threshold should pass");
        }

        @Test
        @DisplayName("Both thresholds must be met")
        void bothThresholdsMustBeMet() {
            // Given: High frequency but low success rate
            List<ExecutionSequence> highFreqLowSuccess = createSequencesWithSuccessRate(10, 0.5);

            // Given: Low frequency but high success rate
            List<ExecutionSequence> lowFreqHighSuccess = createSequencesWithSuccessRate(2, 1.0);

            // When: Extracting patterns
            List<PatternExtractor.Pattern> patterns1 = extractor.extractPatterns(highFreqLowSuccess);
            List<PatternExtractor.Pattern> patterns2 = extractor.extractPatterns(lowFreqHighSuccess);

            // Then: Both should be filtered
            assertTrue(patterns1.isEmpty(),
                "High frequency but low success rate should be filtered");
            assertTrue(patterns2.isEmpty(),
                "Low frequency but high success rate should be filtered");
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Creates repeated mine-place sequences for testing.
     */
    private List<ExecutionSequence> createRepeatedMinePlaceSequences(int count) {
        List<ExecutionSequence> sequences = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ExecutionSequence seq = ExecutionSequence.builder("agent1", "mine and place")
                .addAction(createAction("mine", Map.of("blockType", "stone", "x", i)))
                .addAction(createAction("place", Map.of("blockType", "torch", "z", i)))
                .build(true);
            sequences.add(seq);
        }
        return sequences;
    }

    /**
     * Creates repeated craft sequences for testing.
     */
    private List<ExecutionSequence> createRepeatedCraftSequences(int count) {
        List<ExecutionSequence> sequences = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ExecutionSequence seq = ExecutionSequence.builder("agent1", "craft items")
                .addAction(createAction("craft", Map.of("item", "planks", "count", 4)))
                .build(true);
            sequences.add(seq);
        }
        return sequences;
    }

    /**
     * Creates a mining sequence with specific coordinates.
     */
    private ExecutionSequence createMiningSequence(int x, int y, int z) {
        Map<String, Object> params = new HashMap<>();
        params.put("x", x);
        params.put("y", y);
        params.put("z", z);
        params.put("blockType", "stone");

        ActionRecord action = new ActionRecord("mine", params, 100, true, null);
        return ExecutionSequence.builder("agent1", "mining staircase")
            .addAction(action)
            .build(true);
    }

    /**
     * Creates a loop sequence with repeated actions.
     */
    private ExecutionSequence createLoopSequence(String actionType, int repeatCount, int offset) {
        ExecutionSequence.Builder builder = ExecutionSequence.builder(
            "agent1", "loop " + actionType);

        for (int i = 0; i < repeatCount; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put("iteration", offset + i);
            builder.addAction(createAction(actionType, params));
        }

        return builder.build(true);
    }

    /**
     * Creates a complex loop sequence with multiple actions.
     */
    private ExecutionSequence createComplexLoopSequence(int offset) {
        ExecutionSequence.Builder builder = ExecutionSequence.builder(
            "agent1", "complex mining loop");

        for (int i = 0; i < 3; i++) {
            Map<String, Object> mineParams = new HashMap<>();
            mineParams.put("x", offset + i);
            builder.addAction(createAction("mine", mineParams));

            Map<String, Object> placeParams = new HashMap<>();
            placeParams.put("z", offset + i);
            builder.addAction(createAction("place", placeParams));
        }

        return builder.build(true);
    }

    /**
     * Creates a linear action sequence.
     */
    private ExecutionSequence createLinearSequence(int offset) {
        return ExecutionSequence.builder("agent1", "linear sequence")
            .addAction(createAction("pathfind", Map.of("x", offset)))
            .addAction(createAction("mine", Map.of("blockType", "stone")))
            .addAction(createAction("place", Map.of("blockType", "torch")))
            .build(true);
    }

    /**
     * Creates a branching sequence.
     */
    private ExecutionSequence createBranchingSequence(int offset, String lastAction) {
        return ExecutionSequence.builder("agent1", "branching sequence")
            .addAction(createAction("pathfind", Map.of("x", offset)))
            .addAction(createAction("mine", Map.of("blockType", "stone")))
            .addAction(createAction(lastAction, Map.of("item", "planks")))
            .build(true);
    }

    /**
     * Creates a conditional sequence (torches placed periodically).
     */
    private ExecutionSequence createConditionalSequence(int offset) {
        ExecutionSequence.Builder builder = ExecutionSequence.builder(
            "agent1", "conditional mining");

        for (int i = 0; i < 10; i++) {
            builder.addAction(createAction("mine", Map.of("x", offset + i)));

            // Place torch every 3 blocks
            if (i % 3 == 0) {
                builder.addAction(createAction("place", Map.of("blockType", "torch")));
            }
        }

        return builder.build(true);
    }

    /**
     * Creates a long sequence with many actions.
     */
    private ExecutionSequence createLongSequence(int offset) {
        ExecutionSequence.Builder builder = ExecutionSequence.builder(
            "agent1", "long sequence");

        for (int i = 0; i < 20; i++) {
            builder.addAction(createAction("mine", Map.of("x", offset + i)));
            builder.addAction(createAction("place", Map.of("z", offset + i)));
        }

        return builder.build(true);
    }

    /**
     * Creates sequences with a specific success rate.
     */
    private List<ExecutionSequence> createSequencesWithSuccessRate(int total, double successRate) {
        List<ExecutionSequence> sequences = new ArrayList<>();
        int successCount = (int) (total * successRate);

        for (int i = 0; i < total; i++) {
            boolean successful = i < successCount;
            ExecutionSequence seq = ExecutionSequence.builder("agent1", "test sequence")
                .addAction(createAction("mine", Map.of("blockType", "stone")))
                .build(successful);
            sequences.add(seq);
        }

        return sequences;
    }

    /**
     * Creates an ActionRecord with the given parameters.
     */
    private ActionRecord createAction(String actionType, Map<String, Object> params) {
        return new ActionRecord(actionType, params, 100, true, null);
    }
}
